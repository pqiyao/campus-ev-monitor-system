#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""桥接服务联调测试：下发任务 -> 轮询遥测 -> 汇总事件。Windows 侧运行。"""
import json
import time
import urllib.request

BASE = "http://127.0.0.1:9030"


def req(method, path, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    r = urllib.request.Request(BASE + path, data=data, method=method,
                               headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(r, timeout=15) as resp:
        return json.loads(resp.read().decode("utf-8"))


mission = {
    "missionId": "test-001",
    "home": {"lng": 110.3239130, "lat": 25.2875179},
    "waypoints": [
        {"waypointId": "wp-1", "sequenceNo": 1, "lng": 110.3248, "lat": 25.2879, "altitudeM": 30},
        {"waypointId": "wp-2", "sequenceNo": 2, "lng": 110.3256, "lat": 25.2884, "altitudeM": 30},
    ],
}
print("POST /missions ->", req("POST", "/missions", mission))

events_all = []
for i in range(45):
    time.sleep(2)
    s = req("GET", "/missions/test-001/status")
    pos = s["position"]
    for e in s.get("events", []):
        events_all.append(e)
    print(f"t={i*2:3d}s state={pos['flightState']:9s} "
          f"lng={pos['lng']:.7f} lat={pos['lat']:.7f} alt={pos['altitudeM']:5.1f} "
          f"speed={pos['speedMps']:5.2f} bat={pos['battery']:5.1f} events+={len(s.get('events', []))}")
    if s.get("completed"):
        break

print("\n=== ALL EVENTS ===")
for e in events_all:
    print(e)
print("\nRESULT:", "PASS" if any(e["type"] == "ARRIVED" for e in events_all) else "NO-ARRIVED")
