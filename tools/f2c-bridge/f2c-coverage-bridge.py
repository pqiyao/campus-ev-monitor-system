#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
f2c-coverage-bridge.py — Fields2Cover 全覆盖巡检航线规划桥接服务
飞巡安行 · 低空巡检「牛耕式全覆盖」航线（项目书 3.7 规划器升级）

对任意校园地块多边形，调用 Fields2Cover（C++ 覆盖路径规划库）真实计算
「地块内缩(headland) → 牛耕式条带(swaths) → 蛇形排序(snake) → Dubins 平滑」
的全覆盖巡检航线，输出无人机可执行的经纬度航点。所有几何均为实时计算，
不含任何写死坐标。规划失败时由后端回退到原有的风险网格贪心航线。

算法链：
  HG_Const_gen().generateHeadlands   地块边界内缩，保证航线不越界
  SG_BruteForce().generateSwaths     生成等宽牛耕式扫掠条带
  RP_Snake().genSortedSwaths         蛇形顺序串联条带（转弯次数最少）
  PP_PathPlanning + PP_DubinsCurves  生成满足最小转弯半径的平滑路径

HTTP API（默认 127.0.0.1:9031，仅后端 CoveragePlannerClient 访问）：
  GET  /health
  POST /coverage  {polygon:[[lng,lat]...], covWidth, robotWidth, minRadius,
                   headlandWidth?, sampleSpacingM?}
       -> {waypoints:[{lng,lat}], swathCount, turnCount, areaM2,
           pathLengthM, smoothPathLengthM, engine, version}

坐标约定：经纬度(WGS84) <-> 以多边形质心为原点的局部米制等距圆柱投影。
"""
import argparse
import json
import math
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import fields2cover as f2c

METERS_PER_DEG_LAT = 110540.0
METERS_PER_DEG_LNG = 111320.0

# f2c 版本（自检与前端展示用）
try:
    F2C_VERSION = getattr(f2c, "__version__", "unknown")
except Exception:
    F2C_VERSION = "unknown"


# --------------------------------------------------------------------------
# 经纬度 <-> 局部米制（等距圆柱投影，绕多边形质心）
# --------------------------------------------------------------------------
def local_scales(ref_lat):
    return METERS_PER_DEG_LNG * math.cos(math.radians(ref_lat)), METERS_PER_DEG_LAT


def polygon_to_local(polygon, ref_lng, ref_lat):
    sx, sy = local_scales(ref_lat)
    return [((lng - ref_lng) * sx, (lat - ref_lat) * sy) for lng, lat in polygon]


def local_to_lnglat(x, y, ref_lng, ref_lat):
    sx, sy = local_scales(ref_lat)
    return ref_lng + x / sx, ref_lat + y / sy


def ring_area(points):
    """鞋带公式：局部米制多边形面积（平方米）。"""
    area = 0.0
    n = len(points)
    for i in range(n):
        x1, y1 = points[i]
        x2, y2 = points[(i + 1) % n]
        area += x1 * y2 - x2 * y1
    return abs(area) / 2.0


def polyline_length(points):
    length = 0.0
    for i in range(1, len(points)):
        length += math.hypot(points[i][0] - points[i - 1][0], points[i][1] - points[i - 1][1])
    return length


# --------------------------------------------------------------------------
# 几何兼容取点：不同 f2c 版本路径容器 API 略有差异，逐级兜底
# --------------------------------------------------------------------------
def _geometry_points(container):
    """从 LineString / MultiPoint 一类几何容器取点。"""
    pts = []
    size = container.size() if hasattr(container, "size") else len(container)
    for i in range(size):
        g = container.getGeometry(i)
        pts.append((g.getX(), g.getY()))
    return pts


def _path_points(path):
    """从 f2c.Path 取平滑后的路径点，覆盖 v2 常见 API。"""
    if hasattr(path, "getStates"):
        states = path.getStates()
        pts = []
        for st in states:
            p = getattr(st, "point", None) or st.getPoint()
            pts.append((p.getX(), p.getY()))
        return pts
    if hasattr(path, "getGeometry"):
        return _geometry_points(path.getGeometry(0) if hasattr(path.getGeometry(0), "size") else path)
    raise RuntimeError("无法从 f2c.Path 提取路径点，当前版本 API 不支持")


def _swath_points(swaths):
    """从 Swaths 逐条取原始条带端点（用于统计与兜底）。"""
    out = []
    size = swaths.size() if hasattr(swaths, "size") else len(swaths)
    for i in range(size):
        sw = swaths.getGeometry(i)
        line = sw.getPath()
        out.append(_geometry_points(line))
    return out


def _decimate(points, spacing):
    """按最小间距抽稀，保留折线几何（首尾必留）。"""
    if len(points) <= 2:
        return list(points)
    kept = [points[0]]
    for p in points[1:-1]:
        if math.hypot(p[0] - kept[-1][0], p[1] - kept[-1][1]) >= spacing:
            kept.append(p)
    if kept[-1] != points[-1]:
        kept.append(points[-1])
    return kept


# --------------------------------------------------------------------------
# 全覆盖规划主流程
# --------------------------------------------------------------------------
def compute_coverage(polygon, holes, cov_width, robot_width, min_radius,
                     headland_width, sample_spacing):
    if len(polygon) < 3:
        raise ValueError("地块多边形至少需要 3 个顶点")
    ref_lng = sum(p[0] for p in polygon) / len(polygon)
    ref_lat = sum(p[1] for p in polygon) / len(polygon)
    local = polygon_to_local(polygon, ref_lng, ref_lat)

    def build_ring(points):
        ring = f2c.LinearRing()
        for x, y in points:
            ring.addGeometry(f2c.Point(x, y))
        return ring

    cell = f2c.Cell()
    cell.addRing(build_ring(local))  # 首个 ring 为外边界，其余为禁飞区镂空
    for hole in holes or []:
        if len(hole) >= 3:
            cell.addRing(build_ring(polygon_to_local(hole, ref_lng, ref_lat)))
    cells = f2c.Cells()
    cells.addGeometry(cell)

    robot = f2c.Robot(robot_width, cov_width)
    robot.setMinTurningRadius(min_radius)
    # 头地宽度：默认 1 倍覆盖宽度，保证边界内缩足够，航线不越界
    hl_width = headland_width if headland_width and headland_width > 0 else 1.0 * cov_width

    no_hl = f2c.HG_Const_gen().generateHeadlands(cells, hl_width)
    swaths = f2c.SG_BruteForce().generateSwaths(math.pi, robot.getCovWidth(), no_hl.getGeometry(0))
    swath_count = swaths.size() if hasattr(swaths, "size") else len(swaths)

    sorted_swaths = f2c.RP_Snake().genSortedSwaths(swaths)
    path = f2c.PP_PathPlanning().planPath(robot, sorted_swaths, f2c.PP_DubinsCurves())

    raw_points = _path_points(path)
    if not raw_points:
        raise RuntimeError("Fields2Cover 未返回有效覆盖路径")

    decimated = _decimate(raw_points, max(0.5, sample_spacing))
    waypoints = []
    for x, y in decimated:
        lng, lat = local_to_lnglat(x, y, ref_lng, ref_lat)
        waypoints.append({"lng": round(lng, 7), "lat": round(lat, 7)})

    return {
        "waypoints": waypoints,
        "swathCount": int(swath_count),
        "rawPointCount": len(raw_points),
        "areaM2": round(ring_area(local), 1),
        "pathLengthM": round(polyline_length(raw_points), 1),
        "smoothPathLengthM": round(polyline_length(decimated), 1),
        "headlandWidthM": round(hl_width, 2),
        "engine": "fields2cover",
        "version": F2C_VERSION,
    }


class CoverageHandler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print(f"[f2c-bridge] {fmt % args}")

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
            self._send(200, {"status": "UP", "engine": "fields2cover", "version": F2C_VERSION,
                             "uptimeSec": round(time.time() - START_TIME, 1)})
            return
        self._send(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/coverage":
            self._send(404, {"error": "not found"})
            return
        data = self._body()
        try:
            polygon = data.get("polygon") or []
            polygon = [(float(p[0]), float(p[1])) for p in polygon]
            holes = [[(float(p[0]), float(p[1])) for p in ring] for ring in (data.get("holes") or [])]
            cov_width = float(data.get("covWidth") or 20.0)
            robot_width = float(data.get("robotWidth") or 1.0)
            min_radius = float(data.get("minRadius") or max(2.0, cov_width * 0.25))
            headland_width = float(data.get("headlandWidth") or 0.0)
            sample_spacing = float(data.get("sampleSpacingM") or 8.0)
            started = time.time()
            result = compute_coverage(polygon, holes, cov_width, robot_width, min_radius,
                                      headland_width, sample_spacing)
            result["holeCount"] = len([h for h in holes if len(h) >= 3])
            result["computeMs"] = round((time.time() - started) * 1000.0, 1)
            self._send(200, result)
        except Exception as ex:
            self._send(422, {"error": f"覆盖规划失败：{ex}"})


START_TIME = time.time()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Fields2Cover 全覆盖巡检航线桥接服务")
    parser.add_argument("--port", type=int, default=9031)
    parser.add_argument("--host", default="0.0.0.0", help="监听地址，WSL 内建议 0.0.0.0 便于 Windows 侧经 NAT 访问")
    args = parser.parse_args()
    server = ThreadingHTTPServer((args.host, args.port), CoverageHandler)
    print(f"[f2c-bridge] Fields2Cover 覆盖规划桥接服务已启动: http://{args.host}:{args.port} version={F2C_VERSION}")
    server.serve_forever()
