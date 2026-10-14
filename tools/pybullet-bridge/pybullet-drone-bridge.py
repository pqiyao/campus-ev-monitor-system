#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
pybullet-drone-bridge.py — PyBullet 物理仿真无人机桥接服务
飞巡安行 · 空地协同无人机链路（项目书3.7 无人机执行器升级）

基于 gym-pybullet-drones（utiasDSL, MIT）的 CtrlAviary + DSLPIDControl：
  - 真实六自由度刚体动力学（电机推力/力矩、气动阻尼）
  - DSL PID 位控器逐航点飞行
  - PyBullet GUI 可实时观看 3D 飞行画面（--gui）

HTTP API（本机 127.0.0.1:9030，仅后端 PyBulletDroneAdapter 访问）：
  GET  /health
  POST /missions                 下发任务 {missionId, home:{lng,lat}, waypoints:[...]}
  GET  /missions/{id}/status     拉取遥测与事件增量
  POST /missions/{id}/stop       停止任务
  POST /missions/{id}/fault      故障注入 {faultType, parameters}
                                 GPS_OFFSET / PACKET_LOSS / LOW_BATTERY / WIND(物理风扰)
  POST /missions/{id}/clear-fault

坐标约定：经纬度(WGS84) <-> 以 home 为原点的 NE 米制平面。
"""
import argparse
import json
import math
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import numpy as np

from gym_pybullet_drones.utils.enums import DroneModel, Physics
from gym_pybullet_drones.envs.CtrlAviary import CtrlAviary
from gym_pybullet_drones.control.DSLPIDControl import DSLPIDControl

METERS_PER_DEG_LAT = 110540.0
WIND_FAULT_FORCE = 6.0  # 牛顿，侧向随机风扰
# DSL PID 物理包线（cf2x：最大总推力 2.25mg≈0.595N，水平位置环 P=0.4）：
# 位置误差 e 的期望倾角 arctan(0.4e/mg)——4m 误差→80°倾角→所需升力1.5N超推力上限→螺旋坠机；
# 0.3m 误差→约24°倾角，留足余量。纯跟踪前置距离必须维持在此量级。
PURE_PURSUIT_LOOKAHEAD = 0.3
CRUISE_SPEED = 1.5  # 巡航限速(m/s)：经 target_vel 前馈限幅，到点前按 0.6*dist 收敛减速

MISSIONS = {}
LOCK = threading.Lock()


def lng_to_east(lng, home_lng, home_lat):
    return (lng - home_lng) * 111320.0 * math.cos(math.radians(home_lat))


def lat_to_north(lat, home_lat):
    return (lat - home_lat) * METERS_PER_DEG_LAT


def east_to_lng(east, home_lng, home_lat):
    return home_lng + east / (111320.0 * math.cos(math.radians(home_lat)))


def north_to_lat(north, home_lat):
    return home_lat + north / METERS_PER_DEG_LAT


class Mission:
    """单机 PyBullet 仿真任务：起飞 → 逐航点 → 返航 → 降落。"""

    def __init__(self, mission_id, home, waypoints, gui=False):
        self.mission_id = mission_id
        self.home = home  # {lng, lat}
        self.gui = gui
        # 航点转 NE 米制（飞行高度默认30m），并按 ≤8m 切分中继段（真实航点到达才发事件）
        raw = []
        for wp in waypoints:
            x = lng_to_east(wp["lng"], home["lng"], home["lat"])
            y = lat_to_north(wp["lat"], home["lat"])
            z = float(wp.get("altitudeM") or 30.0)
            raw.append({"id": wp.get("waypointId"), "seq": wp.get("sequenceNo", 0),
                        "ne": np.array([x, y, z]), "real": True})
        self.targets = raw  # 纯跟踪+限速可直接消化长航段，无需中继插值
        # NE 原点 = home：初始在地面 (0,0,0.1)
        self.env = CtrlAviary(
            drone_model=DroneModel("cf2x"), num_drones=1,
            initial_xyzs=np.array([[0.0, 0.0, 0.1]]),
            physics=Physics("pyb"), neighbourhood_radius=10,
            pyb_freq=240, ctrl_freq=48, gui=gui, record=False,
            obstacles=False, user_debug_gui=False)
        self.ctrl = DSLPIDControl(drone_model=DroneModel("cf2x"))
        self.action = np.zeros((1, 4))
        self.wp_index = 0
        self.phase = "TAKEOFF"  # TAKEOFF → CRUISE → RETURNING → LANDING → COMPLETED
        self.events = []        # 自上次 status 拉取累积
        self.battery = 100.0
        self.faults = {"gpsOffset": 0.0, "packetLoss": 0.0, "forceBattery": None, "wind": False}
        self._last_snapshot = None
        self._live = (0.0, 0.0, 0.0, 0.0)  # (east, north, alt, speed) 仿真线程每帧写入
        self._crash_steps = 0
        self._low_battery_warned = False
        self.running = True
        self.completed = False
        self.started_at = time.time()
        self.last_altitude = 0.0
        self.thread = threading.Thread(target=self.run, daemon=True)
        self.thread.start()

    def push_event(self, etype, waypoint_id=None, seq=-1, detail=""):
        self.events.append({"type": etype, "waypointId": waypoint_id, "sequenceNo": seq, "detail": detail})

    def _crash_check(self, pos):
        if pos[2] < 0.3:
            self._crash_steps += 1
            if self._crash_steps > 50:
                self.push_event("FAILED", detail="物理仿真检测到无人机坠落，任务失败")
                self.running = False
        else:
            self._crash_steps = 0

    def _pursuit(self, dest, pos):
        """纯跟踪目标点与限速前馈：PID 只处理 ≤lookahead 的小增量（防倾角超推力包线），
        target_vel 按 min(巡航速度, 0.6*dist) 限幅，到点自然减速。"""
        dirv = dest - pos
        dist = float(np.linalg.norm(dirv))
        if dist < 1e-6:
            return pos.copy(), np.zeros(3)
        u = dirv / dist
        return pos + u * min(PURE_PURSUIT_LOOKAHEAD, dist), u * min(CRUISE_SPEED, 0.6 * dist)

    def run(self):
        try:
            home_hover = np.array([0.0, 0.0, 30.0])
            while self.running and not self.completed:
                obs, _, _, _, _ = self.env.step(self.action)
                state = obs[0]
                pos = state[0:3]
                vel = state[10:13]
                # 电量：按飞行时长线性消耗（约17分钟航时）
                elapsed = time.time() - self.started_at
                self.battery = max(0.0, 100.0 - elapsed * 0.098)
                if self.faults["forceBattery"] is not None:
                    self.battery = min(self.battery, self.faults["forceBattery"])
                # 物理风扰：每控制帧施加随机侧向力（真实动力学扰动，非位置偏移）
                if self.faults["wind"]:
                    import pybullet as p
                    force = [np.random.uniform(-WIND_FAULT_FORCE, WIND_FAULT_FORCE),
                             np.random.uniform(-WIND_FAULT_FORCE, WIND_FAULT_FORCE), 0.0]
                    p.applyExternalForce(self.env.DRONE_IDS[0], -1,
                                         forceObj=force, posObj=[0, 0, 0],
                                         flags=p.LINK_FRAME, physicsClientId=self.env.CLIENT)
                # 阶段机
                tvel = np.zeros(3)
                if self.phase == "TAKEOFF":
                    target = home_hover
                    if np.linalg.norm(pos - target) < 0.6:
                        self.phase = "CRUISE"
                        self.push_event("TAKEOFF_DONE", detail="起飞完成，进入巡航")
                elif self.phase == "CRUISE":
                    if self.wp_index >= len(self.targets):
                        self.phase = "RETURNING"
                        self.push_event("RETURNING", detail="完成巡检航点，开始返航")
                        target = home_hover
                    else:
                        wp_ne = self.targets[self.wp_index]["ne"]
                        if np.linalg.norm(pos - wp_ne) < 0.8:
                            t = self.targets[self.wp_index]
                            if t["real"]:
                                self.push_event("ARRIVED", t["id"], t["seq"], "到达规划航点(PyBullet物理仿真)")
                            self.wp_index += 1
                            self._crash_steps = 0
                            target = wp_ne
                        else:
                            # 纯跟踪 + 限速前馈：见 _pursuit 注释（防倾角超推力包线）
                            target, tvel = self._pursuit(wp_ne, pos)
                elif self.phase == "RETURNING":
                    if self.battery <= 20.0 and not self._low_battery_warned:
                        self._low_battery_warned = True
                        self.push_event("LOW_BATTERY", detail="电量低于返航阈值(PyBullet)")
                    if np.linalg.norm(pos - home_hover) < 0.8:
                        self.phase = "LANDING"
                        self.push_event("AT_HOME", detail="已返航到达停靠点上空")
                        target = np.array([0.0, 0.0, 0.1])
                    else:
                        target, tvel = self._pursuit(home_hover, pos)
                else:  # LANDING：纯跟踪下压，避免 30m 垂直误差令推力指令归零自由落体
                    target, tvel = self._pursuit(np.array([0.0, 0.0, 0.1]), pos)
                    if pos[2] < 0.35:
                        self.completed = True
                        self.push_event("COMPLETED", detail="PyBullet仿真无人机已降落，任务完成")
                        break
                # PID 控制
                self.action[0, :], _, _ = self.ctrl.computeControlFromState(
                    control_timestep=self.env.CTRL_TIMESTEP, state=state,
                    target_pos=target, target_rpy=np.zeros(3), target_vel=tvel)
                # 坠落保护：巡航/返航中贴地持续约1秒仿真时间判定坠机
                if self.phase in ("CRUISE", "RETURNING"):
                    self._crash_check(pos)
                self.last_altitude = float(pos[2])
                # 仿真线程内缓存实时状态（pybullet 非线程安全，HTTP 线程只读此缓存）
                self._live = (float(pos[0]), float(pos[1]), float(pos[2]),
                              float(np.linalg.norm(vel)))
                if self.gui:
                    self.env.render()
                    from gym_pybullet_drones.utils.utils import sync
                    sync(int(elapsed * self.env.CTRL_FREQ), self.started_at, self.env.CTRL_TIMESTEP)
            self.env.close()
        except Exception as ex:  # 仿真线程异常上报
            self.push_event("FAILED", detail=f"PyBullet仿真异常: {ex}")
            self.running = False
            try:
                self.env.close()
            except Exception:
                pass

    def snapshot(self):
        """当前遥测快照（含 GPS_OFFSET 故障偏移与 PACKET_LOSS 通信中断模拟）。

        位置读仿真线程的 _live 缓存，不直接调 pybullet（非线程安全）。
        """
        if self.completed or not self.running:
            east, north, alt, speed = 0.0, 0.0, 0.0, 0.0
        else:
            east, north, alt, speed = self._live
        report_east = east + self.faults["gpsOffset"]
        report_north = north + self.faults["gpsOffset"]
        wp = next((t for t in self.targets[self.wp_index:] if t["real"]), None) if self.wp_index < len(self.targets) else None
        flight_state = {"TAKEOFF": "FLYING", "CRUISE": "FLYING", "RETURNING": "RETURNING",
                        "LANDING": "RETURNING", "COMPLETED": "RETURNING"}.get(self.phase, "FLYING")
        # PACKET_LOSS：随机返回缓存的上一帧（模拟通信中断）
        if self.faults["packetLoss"] > 0 and self._last_snapshot is not None \
                and np.random.random() < self.faults["packetLoss"]:
            return self._last_snapshot
        snapshot = {
            "running": self.running and not self.completed,
            "completed": self.completed,
            "position": {
                "lng": east_to_lng(report_east, self.home["lng"], self.home["lat"]),
                "lat": north_to_lat(report_north, self.home["lat"]),
                "altitudeM": round(alt, 2),
                "speedMps": round(speed, 2),
                "battery": round(self.battery, 2),
                "waypointId": wp["id"] if (wp and self.phase == "CRUISE") else None,
                "sequenceNo": (wp["seq"] if wp else -1) if self.phase == "CRUISE" else -1,
                "flightState": flight_state,
            },
            "events": [],
        }
        self._last_snapshot = snapshot
        return snapshot

    def drain_events(self):
        events = list(self.events)
        self.events = []
        return events

    def stop(self):
        self.running = False

    def apply_fault(self, fault_type, parameters):
        ftype = (fault_type or "").upper()
        if ftype == "GPS_OFFSET":
            self.faults["gpsOffset"] = float(parameters.get("meters", 8.0))
        elif ftype == "PACKET_LOSS":
            self.faults["packetLoss"] = min(1.0, max(0.0, float(parameters.get("rate", 0.3))))
        elif ftype == "LOW_BATTERY":
            self.faults["forceBattery"] = max(1.0, min(100.0, float(parameters.get("battery", 24.0))))
        elif ftype == "WIND":
            self.faults["wind"] = True
        else:
            raise ValueError(f"不支持的PyBullet故障类型：{fault_type}")
        self.push_event("FAULT_INJECTED", detail=f"PyBullet故障注入：{ftype}")

    def clear_fault(self):
        self.faults = {"gpsOffset": 0.0, "packetLoss": 0.0, "forceBattery": None, "wind": False}
        self.push_event("FAULT_CLEARED", detail="PyBullet故障已恢复")


class BridgeHandler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print(f"[bridge] {fmt % args}")

    def _send(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _body(self):
        length = int(self.headers.get("Content-Length") or 0)
        return json.loads(self.rfile.read(length).decode("utf-8")) if length > 0 else {}

    def do_GET(self):
        if self.path == "/health":
            self._send(200, {"status": "UP", "missions": len(MISSIONS)})
            return
        parts = self.path.strip("/").split("/")
        if len(parts) == 3 and parts[0] == "missions" and parts[2] == "status":
            mission = MISSIONS.get(parts[1])
            if mission is None:
                self._send(404, {"error": "任务不存在"})
                return
            snap = mission.snapshot()
            snap["events"] = mission.drain_events()
            self._send(200, snap)
            return
        self._send(404, {"error": "not found"})

    def do_POST(self):
        parts = self.path.strip("/").split("/")
        if self.path == "/missions" and len(parts) == 1:
            data = self._body()
            mission_id = str(data.get("missionId"))
            with LOCK:
                old = MISSIONS.get(mission_id)
                if old is not None:
                    old.stop()
                mission = Mission(mission_id, data.get("home") or {}, data.get("waypoints") or [],
                                  gui=BRIDGE_ARGS.gui)
                MISSIONS[mission_id] = mission
            self._send(202, {"accepted": True, "missionId": mission_id,
                             "waypoints": len(mission.targets)})
            return
        if len(parts) == 3 and parts[0] == "missions":
            mission = MISSIONS.get(parts[1])
            if mission is None:
                self._send(404, {"error": "任务不存在"})
                return
            action = parts[2]
            if action == "stop":
                mission.stop()
                self._send(200, {"stopped": True})
            elif action == "fault":
                data = self._body()
                try:
                    mission.apply_fault(data.get("faultType"), data.get("parameters") or {})
                    self._send(200, {"injected": True})
                except ValueError as ex:
                    self._send(422, {"error": str(ex)})
            elif action == "clear-fault":
                mission.clear_fault()
                self._send(200, {"cleared": True})
            else:
                self._send(404, {"error": "not found"})
            return
        self._send(404, {"error": "not found"})


BRIDGE_ARGS = argparse.Namespace(gui=False)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="PyBullet 无人机物理仿真桥接服务")
    parser.add_argument("--port", type=int, default=9030)
    parser.add_argument("--host", default="0.0.0.0", help="监听地址，WSL 内建议 0.0.0.0 便于 Windows 侧经 NAT 访问")
    parser.add_argument("--gui", action="store_true", help="显示 PyBullet 3D 仿真画面（路演可视化）")
    BRIDGE_ARGS = parser.parse_args()
    server = ThreadingHTTPServer((BRIDGE_ARGS.host, BRIDGE_ARGS.port), BridgeHandler)
    print(f"[bridge] PyBullet 无人机桥接服务已启动: http://{BRIDGE_ARGS.host}:{BRIDGE_ARGS.port} gui={BRIDGE_ARGS.gui}")
    server.serve_forever()
