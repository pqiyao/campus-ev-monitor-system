#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
thermal-sim.py — 红外热成像帧仿真器（电池热失控物理模型）
飞巡安行 · 红外热源风险链路数据源

物理模型（锂离子电池热失控三阶段，竞赛答辩可讲）：
  阶段1 SEI 膜分解   90~120°C   缓慢放热，dT/dt ≈ 0.05~0.2 °C/s
  阶段2 隔膜熔化/内短路 120~200°C  加速自加热，dT/dt ≈ 0.5~3 °C/s
  阶段3 热失控 TR    >200°C     指数式飙升（Arrhenius 近似），峰值 600~800°C 后缓慢冷却

空间场：64x64 温度矩阵（模拟 FLIR Lepton 级分辨率）
  - 热源：高斯热斑（sigma 随热失控程度膨胀）
  - 扩散：3x3 核逐帧热扩散
  - 背景：环境温度 + 日晒渐变
  - 噪声：高斯 σ=0.4°C + 0.1°C 红外量化

用法：
  python thermal-sim.py --once                      # 单帧
  python thermal-sim.py --loop                      # 连续推流（默认1秒/帧，温度演化到热失控）
  python thermal-sim.py --loop --interval 2         # 2秒/帧
  python thermal-sim.py --once --core-temp 320      # 指定电芯温度（测试 HIGH 分级）
  python thermal-sim.py --target http://127.0.0.1:8083 --ingest-key campus-yolo-local

零第三方依赖（纯标准库），Windows/Linux 通用。
"""
import argparse
import json
import math
import random
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime

WIDTH, HEIGHT = 64, 64
QUANTIZE = 0.1  # 红外相机温度量化分辨率 °C

# 默认场景：东校区电动车充电棚（与系统围栏坐标系一致 WGS84）
DEFAULT_LNG, DEFAULT_LAT = 110.3239130, 25.2875179


class ThermalCore:
    """单颗电芯热失控演化状态机（三阶段分段 Arrhenius 近似）。"""

    SEI_START, SEI_END = 90.0, 120.0
    SHORT_START, TR_START = 120.0, 200.0
    PEAK_TEMP = 720.0

    def __init__(self, ambient, core_temp=None, severity=1.0):
        self.ambient = ambient
        self.temp = core_temp if core_temp is not None else ambient + 4.0
        self.severity = max(0.2, severity)  # 劣化程度倍率
        self.tr_started_at = None

    def step(self, dt):
        """推进 dt 秒，返回当前电芯温度。分段放热速率模型。"""
        t = self.temp
        rate = 0.0
        if t < self.SEI_START:
            # 正常运行：微小波动
            rate = 0.02 * self.severity + random.uniform(-0.01, 0.01)
        elif t < self.SEI_END:
            # 阶段1 SEI 分解
            rate = 0.12 * self.severity
        elif t < self.TR_START:
            # 阶段2 内短路，随温度加速
            progress = (t - self.SHORT_START) / (self.TR_START - self.SHORT_START)
            rate = 0.5 + 2.5 * progress ** 2
            rate *= self.severity
        else:
            # 阶段3 热失控：Arrhenius 型指数自加热，直至峰值
            if self.tr_started_at is None:
                self.tr_started_at = time.time()
            overshoot = max(0.0, self.PEAK_TEMP - t)
            rate = 8.0 + 0.12 * (t - self.TR_START) * (overshoot / self.PEAK_TEMP) * self.severity
        self.temp = min(self.PEAK_TEMP + 60.0, max(self.ambient, self.temp + rate * dt))
        # 热失控后趋于峰值后平台
        return self.temp

    @property
    def sigma(self):
        """热斑空间尺度：温度越高扩散越强。"""
        return 2.2 + 2.5 * min(1.0, max(0.0, (self.temp - 60.0) / 400.0))


def render_frame(core, frame_seq):
    """渲染一帧 64x64 温度矩阵：背景 + 高斯热斑 + 扩散 + 噪声 + 量化。"""
    ambient = core.ambient
    cx, cy = WIDTH * 0.62, HEIGHT * 0.55  # 热源位于画面中部偏右（模拟充电桩位）
    sigma = core.sigma
    peak_delta = max(0.0, core.temp - ambient)

    matrix = []
    for r in range(HEIGHT):
        row = []
        for c in range(WIDTH):
            # 背景：环境温度 + 轻微日晒渐变（左上偏热 1.5°C）
            value = ambient + 1.5 * (1.0 - (r + c) / (HEIGHT + WIDTH))
            # 高斯热斑
            d2 = (c - cx) ** 2 + (r - cy) ** 2
            value += peak_delta * math.exp(-d2 / (2 * sigma * sigma))
            # 热失控时二次热斑（相邻电芯受热）
            if core.temp > 250:
                d2b = (c - (cx - 9)) ** 2 + (r - (cy + 7)) ** 2
                value += (core.temp - 250) * 0.35 * math.exp(-d2b / (2 * (sigma * 1.4) ** 2))
            row.append(value)
        matrix.append(row)

    # 3x3 热扩散（一次迭代，边界不扩散）
    blurred = [row[:] for row in matrix]
    for r in range(1, HEIGHT - 1):
        for c in range(1, WIDTH - 1):
            blurred[r][c] = 0.6 * matrix[r][c] + 0.1 * (
                matrix[r - 1][c] + matrix[r + 1][c] + matrix[r][c - 1] + matrix[r][c + 1])
    # 噪声 + 量化
    out = []
    for r in range(HEIGHT):
        row = [round(QUANTIZE * round((v + random.gauss(0, 0.4)) / QUANTIZE), 1) for v in blurred[r]]
        out.append(row)
    return out


def post_json(url, payload, ingest_key, timeout=10):
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url, data=body, method="POST")
    request.add_header("Content-Type", "application/json")
    request.add_header("X-AI-Ingest-Key", ingest_key)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            data = json.loads(response.read().decode("utf-8"))
            return response.status, data
    except urllib.error.HTTPError as ex:
        return ex.code, ex.read().decode("utf-8", errors="replace")
    except Exception as ex:
        return -1, str(ex)


def main():
    parser = argparse.ArgumentParser(description="红外热成像帧仿真器（电池热失控物理模型）")
    parser.add_argument("--target", default="http://127.0.0.1:8083/api/thermal/ingest", help="后端接入URL")
    parser.add_argument("--ingest-key", default="campus-yolo-local", help="设备接入凭证")
    parser.add_argument("--device-id", default="flir-charging-01", help="红外设备ID")
    parser.add_argument("--source-type", default="CAMERA", choices=["CAMERA", "DRONE", "GROUND_ROBOT"])
    parser.add_argument("--scene", default="CHARGING_STATION",
                        choices=["CHARGING_STATION", "PARKING_LOT", "ROADSIDE", "OTHER"])
    parser.add_argument("--ambient", type=float, default=28.0, help="环境温度°C")
    parser.add_argument("--core-temp", type=float, default=None, help="起始电芯温度°C（默认环境+4）")
    parser.add_argument("--severity", type=float, default=1.0, help="热失控劣化倍率(>1更快)")
    parser.add_argument("--lng", type=float, default=DEFAULT_LNG)
    parser.add_argument("--lat", type=float, default=DEFAULT_LAT)
    parser.add_argument("--location", default="东校区充电棚A区")
    parser.add_argument("--pixel-meters", type=float, default=0.05, help="每像素地面尺寸m")
    parser.add_argument("--loop", action="store_true", help="连续推流模式")
    parser.add_argument("--interval", type=float, default=1.0, help="推流间隔秒")
    parser.add_argument("--once", action="store_true", help="只推一帧")
    parser.add_argument("--auto-analyze", action="store_true", help="HIGH时触发后端自动AI研判")
    parser.add_argument("--dry-run", action="store_true", help="只打印摘要不推送")
    args = parser.parse_args()

    core = ThermalCore(args.ambient, args.core_temp, args.severity)
    frame_seq = 0
    print(f"[thermal-sim] device={args.device_id} scene={args.scene} ambient={args.ambient}°C "
          f"target={args.target} loop={args.loop or args.once is False}")

    while True:
        core.step(args.interval if args.loop else 30.0)
        matrix = render_frame(core, frame_seq)
        peak = max(max(row) for row in matrix)
        payload = {
            "deviceId": args.device_id,
            "sourceType": args.source_type,
            "capturedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "longitude": args.lng,
            "latitude": args.lat,
            "locationName": args.location,
            "tempMatrix": matrix,
            "ambientTemp": args.ambient,
            "sceneType": args.scene,
            "frameSeq": frame_seq,
            "pixelMeters": args.pixel_meters,
            "autoAnalyze": args.auto_analyze,
        }
        if args.dry_run:
            print(f"[frame {frame_seq:04d}] core={core.temp:6.1f}°C peak={peak:6.1f}°C (dry-run)")
        else:
            status, resp = post_json(args.target, payload, args.ingest_key)
            summary = ""
            if isinstance(resp, dict) and resp.get("data"):
                data = resp["data"]
                summary = (f"risk={data.get('risk_level')} score={data.get('risk_score')} "
                           f"event={data.get('event_id') or '-'}")
            print(f"[frame {frame_seq:04d}] core={core.temp:6.1f}°C peak={peak:6.1f}°C "
                  f"http={status} {summary}")
            if status != 200:
                print(f"  response: {str(resp)[:300]}")

        frame_seq += 1
        if args.once or (not args.loop and frame_seq >= 1):
            break
        if frame_seq > 1 and core.temp >= core.PEAK_TEMP:
            # 到达峰值后继续推 10 帧观察冷却平台
            if frame_seq > 30:
                print("[thermal-sim] 已达热失控峰值平台，推流结束")
                break
        time.sleep(args.interval)


if __name__ == "__main__":
    sys.exit(main())
