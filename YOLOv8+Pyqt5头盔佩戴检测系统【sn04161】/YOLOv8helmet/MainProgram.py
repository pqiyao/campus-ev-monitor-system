# -*- coding: utf-8 -*-
import asyncio
import base64
import json
import multiprocessing
import os
import struct
import sys
import threading
import time
import httpx
from typing import Any, Dict, List, Optional, Set, Tuple

import cv2
import numpy as np
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, ImageDraw, ImageFont
from pydantic import BaseModel
import uvicorn

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
import Config
from ultralytics import YOLO

# ------------------------------------------------------------
# 违规上报配置
# ------------------------------------------------------------
SPRING_BOOT_BASE_URL = os.getenv("SPRING_BOOT_BASE_URL", "http://localhost:8083").rstrip("/")
AI_EVENT_URL = f"{SPRING_BOOT_BASE_URL}/api/ai/events"
AI_EVIDENCE_UPLOAD_URL = f"{SPRING_BOOT_BASE_URL}/api/ai/evidence/upload"
AI_INGEST_KEY = os.getenv("AI_INGEST_KEY", "campus-yolo-local")
VIOLATION_COOLDOWN_SEC = 30  # 载人等：同一摄像头、同一区域 30 秒内只记录一次
# 未戴头盔由 NoHelmetTrackTracker 按轨迹去重；以下为匹配与轨迹超时（可按场景调）
NO_HELMET_IOU_MATCH = 0.25       # 与上一帧同一目标的 IoU 高于此视为同一条轨迹
NO_HELMET_TRACK_TTL_SEC = float(os.getenv("NO_HELMET_TRACK_TTL_SEC", "3.0"))
NO_HELMET_CONFIRM_SEC = float(os.getenv("NO_HELMET_CONFIRM_SEC", "0.8"))
NO_HELMET_MIN_HITS = int(os.getenv("NO_HELMET_MIN_HITS", "4"))
NO_HELMET_MAX_WAIT_SEC = float(os.getenv("NO_HELMET_MAX_WAIT_SEC", "2.0"))
NO_HELMET_BUFFER_SIZE = int(os.getenv("NO_HELMET_BUFFER_SIZE", "3"))
AI_EVENT_QUEUE_SIZE = int(os.getenv("AI_EVENT_QUEUE_SIZE", "20"))
AI_EVENT_WORKERS = int(os.getenv("AI_EVENT_WORKERS", "2"))
AI_EVENT_RETRIES = int(os.getenv("AI_EVENT_RETRIES", "3"))

# 违规类型映射（与后端约定一致）
VIOLATION_TYPE_NO_HELMET = 8   # 未佩戴头盔（当前后端暂归“其他违规”）
VIOLATION_TYPE_OVERLOAD  = 6   # 违规载人
EVENT_TYPE_SMOKE = -101
EVENT_TYPE_FIRE = -102
FIRE_SMOKE_CONF = float(os.getenv("FIRE_SMOKE_CONF", "0.40"))
FIRE_SMOKE_CONFIRM_HITS = int(os.getenv("FIRE_SMOKE_CONFIRM_HITS", "2"))
FIRE_SMOKE_COOLDOWN_SEC = float(os.getenv("FIRE_SMOKE_COOLDOWN_SEC", "60"))
NETWORK_CAMERA_STREAMS = {
    1: os.getenv("NETWORK_CAMERA_1_URL", "http://192.168.1.101:8081/stream.mjpg"),
    2: os.getenv("NETWORK_CAMERA_2_URL", "http://192.168.1.101:8082/stream.mjpg"),
    3: os.getenv("NETWORK_CAMERA_3_URL", "http://192.168.1.101:8083/stream.mjpg"),
}

# 去重缓存：key = (camera_id, violation_type, region) -> 上次上报时间戳（用于载人等整幅类违规）
_violation_cooldown: Dict[Tuple[int, int, str], float] = {}
_fire_smoke_hits: Dict[Tuple[int, int, str], Tuple[int, float]] = {}
_fire_smoke_lock = threading.Lock()
_ai_event_queue: Optional[asyncio.Queue] = None
_ai_event_workers: List[asyncio.Task] = []

_BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# 服务端预览 WebSocket：推送 720p 框内缩放 JPEG + 二进制封包；推理仍用整帧
PREVIEW_MAX_W = 1280
PREVIEW_MAX_H = 720
PREVIEW_FPS = 15
PREVIEW_JPEG_QUALITY = 82
LOCAL_CAMERA_SCAN_LIMIT = 8
LOCAL_CAMERA_PREVIEW_SLOTS = 4
LABEL_FONT_PATH = os.path.join(_BASE_DIR, "Font", "platech.ttf")
LABEL_FONT_SIZE = 20
LABEL_BAR_H = 25
LABEL_LINE_W = 2
LABEL_COLORS_BGR = (
    (56, 56, 255), (151, 157, 255), (31, 112, 255), (29, 178, 255),
    (49, 210, 207), (10, 249, 72), (23, 204, 146), (134, 219, 61),
    (52, 147, 26), (187, 212, 0), (168, 153, 44), (255, 194, 0),
    (147, 69, 52), (255, 115, 100), (236, 24, 0), (255, 56, 132),
    (133, 0, 82), (255, 56, 203), (200, 149, 255), (199, 55, 255),
)


class LatestNetworkFrame:
    """Continuously drain a network stream and expose only its newest frame."""

    def __init__(self, stream_url: str):
        self.stream_url = stream_url
        self._condition = threading.Condition()
        self._frame: Optional[np.ndarray] = None
        self._sequence = 0
        self._stopped = False
        self._capture: Optional[cv2.VideoCapture] = None
        self._thread = threading.Thread(target=self._run, daemon=True)

    def start(self) -> "LatestNetworkFrame":
        self._thread.start()
        return self

    def _run(self) -> None:
        while not self._stopped:
            cap = cv2.VideoCapture(self.stream_url)
            self._capture = cap
            if not cap.isOpened():
                cap.release()
                time.sleep(1.0)
                continue
            cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
            while not self._stopped:
                ok, frame = cap.read()
                if not ok:
                    break
                with self._condition:
                    self._frame = frame
                    self._sequence += 1
                    self._condition.notify_all()
            cap.release()
            self._capture = None
            if not self._stopped:
                time.sleep(0.25)

    def read_after(self, sequence: int, timeout: float = 2.0) -> Tuple[int, Optional[np.ndarray]]:
        deadline = time.monotonic() + timeout
        with self._condition:
            while not self._stopped and self._sequence <= sequence:
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    return sequence, None
                self._condition.wait(remaining)
            if self._frame is None:
                return sequence, None
            return self._sequence, self._frame.copy()

    def stop(self) -> None:
        self._stopped = True
        with self._condition:
            self._condition.notify_all()
        if self._capture is not None:
            self._capture.release()
        self._thread.join(timeout=2.0)


def _load_label_font() -> ImageFont.ImageFont:
    try:
        return ImageFont.truetype(LABEL_FONT_PATH, LABEL_FONT_SIZE, 0)
    except Exception:
        return ImageFont.load_default()


LABEL_FONT = _load_label_font()


def pack_ws_binary_frame(meta: Dict[str, Any], jpeg_bytes: bytes) -> bytes:
    """[4B BE uint32 json_len][json utf-8][raw jpeg]"""
    jb = json.dumps(meta, ensure_ascii=False).encode("utf-8")
    if len(jb) > 16_000_000:
        raise ValueError("meta json too large")
    return struct.pack("!I", len(jb)) + jb + jpeg_bytes


def _iou_xyxy_norm(a: Tuple[float, float, float, float], b: Tuple[float, float, float, float]) -> float:
    """归一化 xyxy 的 IoU。"""
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    ix1, iy1 = max(ax1, bx1), max(ay1, by1)
    ix2, iy2 = min(ax2, bx2), min(ay2, by2)
    iw, ih = max(0.0, ix2 - ix1), max(0.0, iy2 - iy1)
    inter = iw * ih
    if inter <= 0:
        return 0.0
    ar = max(0.0, ax2 - ax1) * max(0.0, ay2 - ay1)
    br = max(0.0, bx2 - bx1) * max(0.0, by2 - by1)
    union = ar + br - inter
    return inter / union if union > 0 else 0.0


def _evidence_quality(frame_bytes: bytes,
                      box: Tuple[float, float, float, float, float]) -> float:
    """Score a frame by detector confidence, target size and local sharpness."""
    frame = cv2.imdecode(np.frombuffer(frame_bytes, np.uint8), cv2.IMREAD_GRAYSCALE)
    if frame is None:
        return float(box[4])
    h, w = frame.shape[:2]
    x1, y1, x2, y2, conf = box
    bw = max(1.0, (x2 - x1) * w)
    bh = max(1.0, (y2 - y1) * h)
    left = max(0, int(x1 * w - bw * 2.0))
    right = min(w, int(x2 * w + bw * 2.0))
    top = max(0, int(y1 * h - bh))
    bottom = min(h, int(y2 * h + bh * 7.0))
    crop = frame[top:bottom, left:right]
    sharpness = float(cv2.Laplacian(crop, cv2.CV_64F).var()) if crop.size >= 64 else 0.0
    area = max(0.0, (x2 - x1) * (y2 - y1))
    return float(conf) * 4.0 + min(sharpness / 250.0, 3.0) + min(area * 20.0, 2.0)


class NoHelmetTrackTracker:
    """
    未戴头盔目标跨帧关联与短时证据缓冲。候选轨迹达到持续时间和命中帧数后，
    从有上限的清晰帧缓存中选择最佳证据，只触发一次上报。

    参数：
      iou_threshold  与已有轨迹匹配的最小 IoU
      track_ttl_sec    连续多少秒未匹配到检测则删除轨迹（短暂遮挡后可重新记为新目标）
    """

    def __init__(
        self,
        iou_threshold: float = NO_HELMET_IOU_MATCH,
        track_ttl_sec: float = NO_HELMET_TRACK_TTL_SEC,
    ) -> None:
        self.iou_threshold = iou_threshold
        self.track_ttl_sec = track_ttl_sec
        self._tracks: List[Dict[str, Any]] = []
        self._next_id = 0

    def _add_sample(self, track: Dict[str, Any],
                    box: Tuple[float, float, float, float, float],
                    frame_bytes: bytes, now: float) -> None:
        sample = {
            "score": _evidence_quality(frame_bytes, box),
            "frame_bytes": frame_bytes,
            "bbox": (box[0], box[1], box[2], box[3]),
            "conf": box[4],
            "captured_at": now,
        }
        samples = list(track.get("samples", []))
        samples.append(sample)
        samples.sort(key=lambda item: item["score"], reverse=True)
        track["samples"] = samples[:max(1, NO_HELMET_BUFFER_SIZE)]

    def _ready(self, track: Dict[str, Any], now: float) -> bool:
        age = now - track["first_seen"]
        return (track["hits"] >= NO_HELMET_MIN_HITS and age >= NO_HELMET_CONFIRM_SEC) \
            or (track["hits"] >= 2 and age >= NO_HELMET_MAX_WAIT_SEC)

    def _report(self, camera_id: int, track: Dict[str, Any]) -> Tuple[Any, ...]:
        samples = list(track.get("samples", []))
        best = samples[0]
        x1, y1, x2, y2 = best["bbox"]
        return (
            camera_id, VIOLATION_TYPE_NO_HELMET,
            x1, y1, x2, y2, best["conf"], best["frame_bytes"], samples,
        )

    def update(
        self,
        camera_id: int,
        boxes: List[Tuple[float, float, float, float, float]],
        now: float,
        frame_bytes: bytes,
    ) -> List[Tuple[Any, ...]]:
        """
        boxes: (x1,y1,x2,y2,conf) 均为相对整图 0~1 的归一化坐标。
        返回本帧需要上报的 pending 元组列表（与 report_violation_to_backend 参数一致）。
        """
        reports: List[Tuple[Any, ...]] = []
        self._tracks = [t for t in self._tracks if now - t["last_seen"] <= self.track_ttl_sec]

        used_box: Set[int] = set()
        used_track: Set[int] = set()
        pairs: List[Tuple[float, int, int]] = []
        for bi, b in enumerate(boxes):
            bb = (b[0], b[1], b[2], b[3])
            for j, t in enumerate(self._tracks):
                iou = _iou_xyxy_norm(bb, t["bbox"])
                if iou >= self.iou_threshold:
                    pairs.append((iou, bi, j))
        pairs.sort(key=lambda x: -x[0])

        for _iou, bi, j in pairs:
            if bi in used_box or j in used_track:
                continue
            used_box.add(bi)
            used_track.add(j)
            b = boxes[bi]
            tr = self._tracks[j]
            tr["bbox"] = (b[0], b[1], b[2], b[3])
            tr["last_seen"] = now
            tr["hits"] += 1
            self._add_sample(tr, b, frame_bytes, now)
            if not tr.get("reported_no_helmet") and self._ready(tr, now):
                reports.append(self._report(camera_id, tr))
                tr["reported_no_helmet"] = True

        for bi, b in enumerate(boxes):
            if bi in used_box:
                continue
            self._next_id += 1
            self._tracks.append(
                {
                    "id": self._next_id,
                    "bbox": (b[0], b[1], b[2], b[3]),
                    "first_seen": now,
                    "last_seen": now,
                    "hits": 1,
                    "samples": [],
                    "reported_no_helmet": False,
                }
            )
            self._add_sample(self._tracks[-1], b, frame_bytes, now)

        return reports

# ------------------------------------------------------------
# 1. 加载 YOLO 模型（全局单例，预热）
# ------------------------------------------------------------
model = YOLO(Config.model_path, task='detect')
model_lock = threading.Lock()
fire_smoke_model = YOLO(Config.fire_smoke_model_path, task='detect') if os.path.isfile(Config.fire_smoke_model_path) else None
fire_smoke_model_lock = threading.Lock()


def _warm_up_model() -> None:
    model(np.zeros((48, 48, 3), dtype=np.uint8))
    if fire_smoke_model is not None:
        fire_smoke_model(np.zeros((48, 48, 3), dtype=np.uint8))

# ------------------------------------------------------------
# 2. 本机 USB 摄像头发现
# ------------------------------------------------------------
def _open_local_camera(device_index: int) -> cv2.VideoCapture:
    if os.name == "nt":
        cap = cv2.VideoCapture(device_index, cv2.CAP_DSHOW)
        if cap.isOpened():
            return cap
        cap.release()
    return cv2.VideoCapture(device_index)


def _probe_local_camera(device_index: int) -> Optional[Dict[str, Any]]:
    cap = _open_local_camera(device_index)
    if not cap.isOpened():
        cap.release()
        return None

    try:
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
        ok, frame = cap.read()
        if not ok or frame is None:
            return None
        h, w = frame.shape[:2]
        fps = cap.get(cv2.CAP_PROP_FPS)
        if not fps or fps <= 1:
            fps = 30
        return {
            "id": device_index + 1,
            "deviceIndex": device_index,
            "name": f"USB Camera {device_index}",
            "location": f"Local USB Camera {device_index}",
            "status": "online",
            "rtspUrl": "",
            "type": "USB",
            "resolution": f"{w}x{h}",
            "frameRate": int(round(fps)),
            "lng": 112.6827629 + device_index * 0.0001,
            "lat": 26.8860959 + device_index * 0.0001,
        }
    finally:
        cap.release()


def discover_local_cameras(
    scan_limit: int = LOCAL_CAMERA_SCAN_LIMIT,
    slots: int = LOCAL_CAMERA_PREVIEW_SLOTS,
) -> List[Dict[str, Any]]:
    found: List[Dict[str, Any]] = []
    for idx in range(scan_limit):
        cam = _probe_local_camera(idx)
        if cam:
            cam["id"] = len(found) + 1
            found.append(cam)
        if len(found) >= slots:
            break

    for slot in range(len(found), slots):
        found.append({
            "id": slot + 1,
            "deviceIndex": None,
            "name": f"Empty Slot {slot + 1}",
            "location": "No local camera detected",
            "status": "offline",
            "rtspUrl": "",
            "type": "USB",
            "resolution": "640x480",
            "frameRate": 0,
            "lng": 112.6827629 + slot * 0.0001,
            "lat": 26.8860959 + slot * 0.0001,
        })
    return found


_camera_cache: List[Dict[str, Any]] = []


def refresh_camera_cache() -> List[Dict[str, Any]]:
    global _camera_cache
    _camera_cache = discover_local_cameras()
    return _camera_cache


def get_camera_cache() -> List[Dict[str, Any]]:
    return _camera_cache or refresh_camera_cache()

# ------------------------------------------------------------
# 3. FastAPI 应用与中间件
# ------------------------------------------------------------
app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

# ------------------------------------------------------------
# 4. 请求模型
# ------------------------------------------------------------
class SnapshotRequest(BaseModel):
    camera_id: int
    frame: str   # base64 编码

# ------------------------------------------------------------
# 5. 核心检测：从 Ultralytics Results 解析（与 VideoTest 共用统计逻辑）
# ------------------------------------------------------------
def parse_ultralytics_result(r: Any, camera_id: int) -> Dict[str, Any]:
    """单帧 Results → 与原先 detect_and_get_boxes 相同结构的字典（不含 success）。"""
    person_count = 0
    moto_count = 0
    helmet_violations = 0
    helmet_worn = 0
    load_violation = False
    boxes: List[Dict[str, Any]] = []
    pending_violations: List[Tuple[int, int, float, float, float, float, float]] = []
    no_helmet_boxes: List[Tuple[float, float, float, float, float]] = []

    h, w = int(r.orig_shape[0]), int(r.orig_shape[1])

    if r.boxes is not None and len(r.boxes):
        for box, conf, cls_id in zip(
            r.boxes.xyxy.cpu().numpy(),
            r.boxes.conf.cpu().numpy(),
            r.boxes.cls.cpu().numpy(),
        ):
            class_name = r.names[int(cls_id)]
            x1, y1, x2, y2 = box
            cn = str(class_name).lower()

            boxes.append({
                "cls_id": int(cls_id),
                "cls": class_name,
                "x1": round(float(x1) / w, 4),
                "y1": round(float(y1) / h, 4),
                "x2": round(float(x2) / w, 4),
                "y2": round(float(y2) / h, 4),
                "conf": round(float(conf), 2),
            })

            if cn == "person":
                person_count += 1
            elif cn in ("motorcycle", "bike", "two_wheeler", "moto"):
                moto_count += 1
            elif cn in ("no_helmet", "without"):
                helmet_violations += 1
                no_helmet_boxes.append(
                    (
                        float(x1) / w,
                        float(y1) / h,
                        float(x2) / w,
                        float(y2) / h,
                        float(conf),
                    )
                )
            elif cn == "helmet":
                helmet_worn += 1

        if moto_count > 0 and person_count > 1:
            load_violation = True
            pending_violations.append((
                camera_id,
                VIOLATION_TYPE_OVERLOAD,
                0.0, 0.0, 1.0, 1.0,
                0.9,
            ))

    return {
        "person_count": person_count,
        "moto_count": moto_count,
        "helmet_violations": helmet_violations,
        "helmet_worn": helmet_worn,
        "load_violation": load_violation,
        "boxes": boxes,
        "no_helmet_boxes": no_helmet_boxes,
        "pending_violations": pending_violations,
    }


def detect_fire_smoke(image: np.ndarray, camera_id: int) -> Dict[str, Any]:
    """Run the optional fire/smoke model and return normalized detections."""
    if fire_smoke_model is None:
        return {"boxes": [], "pending_violations": []}
    with fire_smoke_model_lock:
        result = fire_smoke_model(image, imgsz=640, conf=FIRE_SMOKE_CONF, verbose=False)[0]
    h, w = int(result.orig_shape[0]), int(result.orig_shape[1])
    boxes: List[Dict[str, Any]] = []
    pending: List[Tuple[int, int, float, float, float, float, float]] = []
    for box, conf, cls_id in zip(
        result.boxes.xyxy.cpu().numpy(),
        result.boxes.conf.cpu().numpy(),
        result.boxes.cls.cpu().numpy(),
    ) if result.boxes is not None and len(result.boxes) else []:
        name = str(result.names[int(cls_id)]).lower()
        event_code = EVENT_TYPE_FIRE if name == "fire" else EVENT_TYPE_SMOKE if name == "smoke" else None
        if event_code is None:
            continue
        x1, y1, x2, y2 = [float(v) for v in box]
        confidence = float(conf)
        boxes.append({
            "cls_id": int(cls_id),
            "cls": name,
            "model": "fire-smoke",
            "x1": round(x1 / w, 4), "y1": round(y1 / h, 4),
            "x2": round(x2 / w, 4), "y2": round(y2 / h, 4),
            "conf": round(confidence, 2),
        })
        region = _region_id(x1 / w, y1 / h, x2 / w, y2 / h)
        key = (camera_id, event_code, region)
        now = time.time()
        with _fire_smoke_lock:
            previous_hits, previous_at = _fire_smoke_hits.get(key, (0, 0.0))
            hits = previous_hits + 1 if now - previous_at <= 2.0 else 1
            _fire_smoke_hits[key] = (hits, now)
            if hits >= max(1, FIRE_SMOKE_CONFIRM_HITS):
                pending.append((camera_id, event_code, x1 / w, y1 / h, x2 / w, y2 / h, confidence))
                _fire_smoke_hits[key] = (0, now)
    return {"boxes": boxes, "pending_violations": pending}


def detect_and_get_boxes(image: np.ndarray, camera_id: int) -> Dict[str, Any]:
    """浏览器推流：小图快检。"""
    with model_lock:
        r = model(image, imgsz=320, verbose=False)[0]
    out = parse_ultralytics_result(r, camera_id)
    safety = detect_fire_smoke(image, camera_id)
    out["boxes"].extend(safety["boxes"])
    out["pending_violations"].extend(safety["pending_violations"])
    out["fire_count"] = sum(1 for b in safety["boxes"] if b["cls"] == "fire")
    out["smoke_count"] = sum(1 for b in safety["boxes"] if b["cls"] == "smoke")
    out["success"] = True
    return out


def _display_label(cls_id: int, class_name: str, conf: float) -> str:
    ch_names = getattr(Config, "CH_names", None)
    if isinstance(ch_names, (list, tuple)) and 0 <= cls_id < len(ch_names):
        name = str(ch_names[cls_id])
    else:
        name = str(class_name)
    return f"{name} {float(conf):.2f}"


def _text_size(text: str) -> Tuple[int, int]:
    try:
        box = LABEL_FONT.getbbox(text)
        return box[2] - box[0], box[3] - box[1]
    except Exception:
        return LABEL_FONT.getsize(text)


def draw_project_style_detections(image: np.ndarray, r: Any) -> np.ndarray:
    """Use this project's PyQt-style boxes instead of the Ultralytics default plot."""
    annotated = image.copy()
    if r.boxes is None or not len(r.boxes):
        return annotated

    h, w = annotated.shape[:2]
    boxes = r.boxes.xyxy.cpu().numpy()
    confs = r.boxes.conf.cpu().numpy()
    cls_ids = r.boxes.cls.cpu().numpy()

    for box, conf, cls_id_raw in zip(boxes, confs, cls_ids):
        cls_id = int(cls_id_raw)
        x1, y1, x2, y2 = [int(round(v)) for v in box]
        x1 = max(0, min(w - 1, x1))
        y1 = max(0, min(h - 1, y1))
        x2 = max(0, min(w - 1, x2))
        y2 = max(0, min(h - 1, y2))
        if x2 <= x1 or y2 <= y1:
            continue

        color = LABEL_COLORS_BGR[cls_id % len(LABEL_COLORS_BGR)]
        class_name = r.names.get(cls_id, str(cls_id)) if hasattr(r.names, "get") else r.names[cls_id]
        label = _display_label(cls_id, class_name, float(conf))

        cv2.rectangle(annotated, (x1, y1), (x2, y2), color, LABEL_LINE_W)

        tw, _ = _text_size(label)
        bar_w = max(64, tw + 8)
        bar_x2 = min(w - 1, x1 + bar_w)
        bar_y1 = y1 - LABEL_BAR_H if y1 >= LABEL_BAR_H else y1
        bar_y2 = y1 if y1 >= LABEL_BAR_H else min(h - 1, y1 + LABEL_BAR_H)
        cv2.rectangle(
            annotated,
            (max(0, x1 - 1), max(0, bar_y1)),
            (bar_x2, max(0, bar_y2)),
            color,
            -1,
            cv2.LINE_AA,
        )

        pil_img = Image.fromarray(annotated)
        draw = ImageDraw.Draw(pil_img)
        text_y = max(0, bar_y1 - 2) if y1 >= LABEL_BAR_H else bar_y1 + 1
        draw.text((x1 + 2, text_y), label, (255, 255, 255), font=LABEL_FONT)
        annotated = np.array(pil_img)

    return annotated


def _video_preview_infer_and_encode(
    cap: cv2.VideoCapture,
    camera_id: int,
    annotate: bool = True,
) -> Optional[Tuple[bytes, Dict[str, Any]]]:
    """
    读帧 → 全帧推理 + plot → 全分辨率写入 save_path/live_full_last.jpg →
    缩放到不超过 1280×720 的预览图 JPEG。
    返回 (jpeg_bytes, parse_ultralytics_result 字典)。
    """
    ok, frame = cap.read()
    if not ok:
        cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
        ok, frame = cap.read()
    if not ok:
        return None

    with model_lock:
        r = model(frame, verbose=False)[0]
    annotated_full = draw_project_style_detections(frame, r) if annotate else frame
    det = parse_ultralytics_result(r, camera_id)
    safety = detect_fire_smoke(frame, camera_id)
    det["boxes"].extend(safety["boxes"])
    det["pending_violations"].extend(safety["pending_violations"])
    det["fire_count"] = sum(1 for b in safety["boxes"] if b["cls"] == "fire")
    det["smoke_count"] = sum(1 for b in safety["boxes"] if b["cls"] == "smoke")

    os.makedirs(Config.save_path, exist_ok=True)
    cv2.imwrite(
        os.path.join(Config.save_path, "live_full_last.jpg"),
        annotated_full,
    )

    h0, w0 = annotated_full.shape[:2]
    sc = min(PREVIEW_MAX_W / w0, PREVIEW_MAX_H / h0, 1.0)
    if sc < 1.0:
        small = cv2.resize(
            annotated_full,
            (int(w0 * sc), int(h0 * sc)),
            interpolation=cv2.INTER_AREA,
        )
    else:
        small = annotated_full

    enc_ok, buf = cv2.imencode(
        ".jpg",
        small,
        [int(cv2.IMWRITE_JPEG_QUALITY), PREVIEW_JPEG_QUALITY],
    )
    if not enc_ok:
        return None
    return buf.tobytes(), det


def _fire_smoke_preview_infer_and_encode(
    frame: np.ndarray,
    camera_id: int,
) -> Optional[Tuple[bytes, Dict[str, Any]]]:
    """Run only the local fire/smoke model and keep preview pixels aligned with boxes."""
    safety = detect_fire_smoke(frame, camera_id)
    h0, w0 = frame.shape[:2]
    scale = min(PREVIEW_MAX_W / w0, PREVIEW_MAX_H / h0, 1.0)
    preview = cv2.resize(
        frame,
        (int(w0 * scale), int(h0 * scale)),
        interpolation=cv2.INTER_AREA,
    ) if scale < 1.0 else frame
    encoded, buffer = cv2.imencode(
        ".jpg",
        preview,
        [int(cv2.IMWRITE_JPEG_QUALITY), PREVIEW_JPEG_QUALITY],
    )
    if not encoded:
        return None
    boxes = safety["boxes"]
    return buffer.tobytes(), {
        "boxes": boxes,
        "pending_violations": safety["pending_violations"],
        "fire_count": sum(1 for box in boxes if box["cls"] == "fire"),
        "smoke_count": sum(1 for box in boxes if box["cls"] == "smoke"),
    }

# ------------------------------------------------------------
# 6. 违规上报与去重
# ------------------------------------------------------------
def _region_id(x1: float, y1: float, x2: float, y2: float, grid: int = 5) -> str:
    """将 bbox 中心点映射到 grid x grid 的格子，同一格子视为同一区域"""
    cx = int((x1 + x2) / 2 * grid)
    cy = int((y1 + y2) / 2 * grid)
    return f"{cx}_{grid - 1 - cy}"  # 使 y 方向从上到下递增


def _annotated_violation_frame(frame: np.ndarray, camera_id: int,
                               x1: float, y1: float, x2: float, y2: float,
                               confidence: float,
                               label: str = "AI EVENT") -> Optional[bytes]:
    """Create the human-review screenshot used as the formal violation evidence."""
    h, w = frame.shape[:2]
    left = max(0, min(w - 1, int(x1 * w)))
    top = max(0, min(h - 1, int(y1 * h)))
    right = max(left + 1, min(w, int(x2 * w)))
    bottom = max(top + 1, min(h, int(y2 * h)))
    annotated = frame.copy()
    color = (40, 40, 230)
    thickness = max(2, int(round(min(h, w) / 320)))
    cv2.rectangle(annotated, (left, top), (right, bottom), color, thickness)
    label = f"{label}  {confidence:.0%}"
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    font = cv2.FONT_HERSHEY_SIMPLEX
    scale = max(0.52, min(0.9, w / 1280.0))
    label_size, _ = cv2.getTextSize(label, font, scale, 2)
    label_top = max(0, top - label_size[1] - 16)
    cv2.rectangle(
        annotated,
        (left, label_top),
        (min(w, left + label_size[0] + 14), min(h, label_top + label_size[1] + 14)),
        color,
        -1,
    )
    cv2.putText(annotated, label, (left + 7, label_top + label_size[1] + 5),
                font, scale, (255, 255, 255), 2, cv2.LINE_AA)
    footer = f"Camera {camera_id}  {timestamp}"
    cv2.putText(annotated, footer, (12, max(24, h - 14)), font, 0.55,
                (255, 255, 255), 2, cv2.LINE_AA)
    ok, encoded = cv2.imencode(".jpg", annotated, [int(cv2.IMWRITE_JPEG_QUALITY), 94])
    return encoded.tobytes() if ok else None


def _build_evidence_images(frame_bytes: bytes, camera_id: int,
                           x1: float, y1: float, x2: float, y2: float,
                           confidence: float,
                           label: str = "AI EVENT") -> List[Tuple[str, bytes]]:
    """Return marked evidence first, followed by source and rider context images."""
    images: List[Tuple[str, bytes]] = []
    frame = cv2.imdecode(np.frombuffer(frame_bytes, np.uint8), cv2.IMREAD_COLOR)
    if frame is None:
        return [("source.jpg", frame_bytes)]
    marked = _annotated_violation_frame(
        frame, camera_id, x1, y1, x2, y2, confidence, label
    )
    if marked is not None:
        images.append(("violation.jpg", marked))
    images.append(("source.jpg", frame_bytes))
    h, w = frame.shape[:2]
    box_w = max(1.0, (x2 - x1) * w)
    box_h = max(1.0, (y2 - y1) * h)
    left = max(0, int(x1 * w - box_w * 2.0))
    right = min(w, int(x2 * w + box_w * 2.0))
    top = max(0, int(y1 * h - box_h * 1.0))
    bottom = min(h, int(y2 * h + box_h * 7.0))
    if right - left < 32 or bottom - top < 32:
        return images
    crop = frame[top:bottom, left:right]
    ok, encoded = cv2.imencode(".jpg", crop, [int(cv2.IMWRITE_JPEG_QUALITY), 92])
    if ok:
        images.append(("rider_context.jpg", encoded.tobytes()))
    return images

async def report_violation_to_backend(camera_id: int, violation_type: int,
                                      x1: float, y1: float,
                                      x2: float, y2: float,
                                      conf: float,
                                      frame_bytes: Optional[bytes] = None,
                                      samples: Optional[List[Dict[str, Any]]] = None,
                                      event_token: Optional[str] = None) -> bool:
    """
    异步上报违规到 Spring Boot 后端。
    未戴头盔：去重已由 NoHelmetTrackTracker 按轨迹完成，此处不再按格子冷却。
    其他类型：仍按区域 + 时间冷却。
    """
    now = time.time()
    if violation_type != VIOLATION_TYPE_NO_HELMET:
        region = _region_id(x1, y1, x2, y2)
        key = (camera_id, violation_type, region)
        last = _violation_cooldown.get(key, 0)
        cooldown = FIRE_SMOKE_COOLDOWN_SEC if violation_type in (EVENT_TYPE_SMOKE, EVENT_TYPE_FIRE) else VIOLATION_COOLDOWN_SEC
        if now - last < cooldown:
            return
        _violation_cooldown[key] = now
        if len(_violation_cooldown) > 1000:
            expired = [k for k, v in _violation_cooldown.items()
                       if now - v > VIOLATION_COOLDOWN_SEC * 2]
            for k in expired:
                _violation_cooldown.pop(k, None)

    if frame_bytes is None:
        print(f"[AI事件] 缺少证据帧，跳过 camera={camera_id} type={violation_type}")
        return False

    event_type = {
        VIOLATION_TYPE_NO_HELMET: "NO_HELMET",
        VIOLATION_TYPE_OVERLOAD: "OVERLOAD",
        EVENT_TYPE_SMOKE: "SMOKE_DETECTED",
        EVENT_TYPE_FIRE: "FIRE_DETECTED",
    }.get(violation_type)
    if not event_type:
        print(f"[AI事件] 未知事件类型，跳过 camera={camera_id} type={violation_type}")
        return False
    evidence_label = {
        "SMOKE_DETECTED": "SMOKE",
        "FIRE_DETECTED": "FIRE",
        "NO_HELMET": "NO HELMET",
        "OVERLOAD": "OVERLOAD",
    }[event_type]
    headers = {"X-AI-Ingest-Key": AI_INGEST_KEY}
    try:
        async with httpx.AsyncClient(timeout=60) as client:
            evidence_urls: List[str] = []
            evidence_payloads: List[Tuple[str, bytes]] = []
            ranked_samples = (samples or [])[:max(1, NO_HELMET_BUFFER_SIZE)]
            if ranked_samples:
                best = ranked_samples[0]
                best_images = _build_evidence_images(
                    best["frame_bytes"], camera_id, *best["bbox"], best["conf"], evidence_label
                )
                evidence_payloads.extend(best_images[:3])
            if not evidence_payloads:
                evidence_payloads = _build_evidence_images(
                    frame_bytes, camera_id, x1, y1, x2, y2, conf, evidence_label
                )
            # 第一张是带框正式证据，后两张是原图与骑行目标上下文，GPT最多接收三张。
            seen_payloads: Set[bytes] = set()
            for suffix, image_bytes in evidence_payloads:
                if image_bytes in seen_payloads or len(evidence_urls) >= 3:
                    continue
                seen_payloads.add(image_bytes)
                upload = await client.post(
                    AI_EVIDENCE_UPLOAD_URL,
                    headers=headers,
                    files={"file": (f"camera_{camera_id}_{int(now * 1000)}_{suffix}", image_bytes, "image/jpeg")},
                )
                upload.raise_for_status()
                upload_body = upload.json()
                file_url = upload_body.get("data", {}).get("fileUrl")
                if upload_body.get("code") != 200 or not file_url:
                    raise RuntimeError(f"证据上传响应异常: {upload_body}")
                evidence_urls.append(file_url)
            payload = {
                "eventType": event_type,
                "sourceType": "CAMERA",
                "deviceId": f"camera-{camera_id}",
                "occurredAt": time.strftime("%Y-%m-%d %H:%M:%S"),
                "locationName": f"摄像头 {camera_id}",
                "detectionConfidence": round(float(conf), 4),
                "evidenceUrls": evidence_urls,
                "rawPayload": {
                    "cameraId": camera_id,
                    "bbox": {"x1": x1, "y1": y1, "x2": x2, "y2": y2},
                    "detector": "YOLOv8-fire-smoke" if event_type in ("FIRE_DETECTED", "SMOKE_DETECTED") else "YOLOv8-helmet",
                    "eventType": event_type,
                },
                "idempotencyKey": event_token or f"camera-{camera_id}-{event_type}-{int(now * 1000)}",
                "autoAnalyze": True,
            }
            resp = await client.post(AI_EVENT_URL, headers=headers, json=payload)
            resp.raise_for_status()
            body = resp.json()
            if body.get("code") == 200:
                event = body.get("data") or {}
                print(f"[AI事件] 成功 camera={camera_id} type={event_type} event={event.get('event_id') or event.get('eventId')}")
                return True
            else:
                print(f"[AI事件] 后端拒绝: {body}")
                return False
    except Exception as e:
        print(f"[AI事件] 上报失败 camera={camera_id}: {e}")
        raise


async def _ai_event_worker(worker_id: int) -> None:
    """Bounded uploader/AI-event worker; retries transient failures without frame storms."""
    assert _ai_event_queue is not None
    while True:
        args, frame_bytes, samples, event_token = await _ai_event_queue.get()
        try:
            for attempt in range(1, max(1, AI_EVENT_RETRIES) + 1):
                try:
                    ok = await report_violation_to_backend(
                        *args, frame_bytes=frame_bytes, samples=samples, event_token=event_token
                    )
                    if ok:
                        break
                    print(f"[AI队列] worker={worker_id} 后端未接受事件，不再重试")
                    break
                except Exception as ex:
                    if attempt >= max(1, AI_EVENT_RETRIES):
                        print(f"[AI队列] worker={worker_id} 最终失败: {ex}")
                    else:
                        await asyncio.sleep(min(2 ** (attempt - 1), 8))
        finally:
            _ai_event_queue.task_done()


async def _ensure_ai_event_workers() -> None:
    global _ai_event_queue, _ai_event_workers
    if _ai_event_queue is None:
        _ai_event_queue = asyncio.Queue(maxsize=max(1, AI_EVENT_QUEUE_SIZE))
    alive = [worker for worker in _ai_event_workers if not worker.done()]
    _ai_event_workers = alive
    while len(_ai_event_workers) < max(1, AI_EVENT_WORKERS):
        _ai_event_workers.append(asyncio.create_task(_ai_event_worker(len(_ai_event_workers) + 1)))


async def _enqueue_ai_events(pending: List[Tuple[Any, ...]], fallback_frame: bytes) -> None:
    await _ensure_ai_event_workers()
    assert _ai_event_queue is not None
    for vio in pending:
        if len(vio) >= 9:
            args = tuple(vio[:7])
            frame_bytes = vio[7] or fallback_frame
            samples = vio[8]
        else:
            args = tuple(vio)
            frame_bytes = fallback_frame
            samples = None
        event_token = f"camera-{args[0]}-{args[1]}-{time.time_ns()}"
        try:
            _ai_event_queue.put_nowait((args, frame_bytes, samples, event_token))
        except asyncio.QueueFull:
            print(f"[AI队列] 已满，丢弃低优先级事件 camera={args[0]} type={args[1]}")

# ------------------------------------------------------------
# 7. REST API
# ------------------------------------------------------------
@app.get("/api/cameras")
async def get_cameras():
    return {"code": 200, "data": refresh_camera_cache()}


@app.get("/api/ai/status")
async def get_ai_status():
    return {
        "code": 200,
        "data": {
            "queueSize": _ai_event_queue.qsize() if _ai_event_queue is not None else 0,
            "queueCapacity": AI_EVENT_QUEUE_SIZE,
            "workers": len([task for task in _ai_event_workers if not task.done()]),
            "workerLimit": AI_EVENT_WORKERS,
            "retryLimit": AI_EVENT_RETRIES,
            "trackTtlSec": NO_HELMET_TRACK_TTL_SEC,
            "confirmSec": NO_HELMET_CONFIRM_SEC,
            "minHits": NO_HELMET_MIN_HITS,
            "maxWaitSec": NO_HELMET_MAX_WAIT_SEC,
            "bufferSize": NO_HELMET_BUFFER_SIZE,
        },
    }

@app.post("/api/snapshot")
async def save_snapshot(req: SnapshotRequest):
    try:
        img_data = base64.b64decode(req.frame)
        nparr = np.frombuffer(img_data, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        os.makedirs(Config.save_path, exist_ok=True)
        filename = f"snapshot_{req.camera_id}_{int(time.time())}.jpg"
        filepath = os.path.join(Config.save_path, filename)
        cv2.imwrite(filepath, img)
        return {"code": 200, "message": "Snapshot saved", "filename": filename}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ------------------------------------------------------------
# 8. WebSocket 实时检测
#    策略：只保留最新帧，旧帧直接丢弃，避免队列积压
# ------------------------------------------------------------
@app.websocket("/ws/detection/{camera_id}")
async def websocket_detection(websocket: WebSocket, camera_id: int):
    await websocket.accept()
    print(f"[WS] connected camera={camera_id}")

    loop = asyncio.get_event_loop()
    latest_frame: Optional[bytes] = None   # 待处理的最新帧
    processing = False
    helmet_tracker = NoHelmetTrackTracker()

    async def process_latest():
        nonlocal processing, latest_frame
        if processing or latest_frame is None:
            return
        processing = True
        data, latest_frame = latest_frame, None   # 取出并清空

        try:
            # 解码（线程池，避免阻塞事件循环）
            img = await loop.run_in_executor(
                None,
                lambda: cv2.imdecode(np.frombuffer(data, np.uint8), cv2.IMREAD_COLOR)
            )
            if img is None:
                return

            def _detect_and_track():
                r = detect_and_get_boxes(img, camera_id)
                nh = r.pop("no_helmet_boxes", [])
                pend = r.pop("pending_violations", [])
                r["pending_violations"] = helmet_tracker.update(
                    camera_id, nh, time.time(), data
                ) + pend
                return r

            result = await loop.run_in_executor(None, _detect_and_track)

            # 提取检测框和违规列表
            boxes = result.pop("boxes", [])
            pending = result.pop("pending_violations", [])
            # 本帧实际会触发后端上报的「未戴头盔」条数（经轨迹去重），供前端累计与数据库一致
            helmet_new_incidents = sum(
                1 for p in pending if p[1] == VIOLATION_TYPE_NO_HELMET
            )

            # 发送检测框 JSON 到前端（非常小）
            await websocket.send_text(json.dumps({
                "success": True,
                "person_count": result["person_count"],
                "moto_count": result["moto_count"],
                "helmet_violations": result["helmet_violations"],
                "helmet_new_incidents": helmet_new_incidents,
                "helmet_worn": result["helmet_worn"],
                "load_violation": result["load_violation"],
                "fire_count": result.get("fire_count", 0),
                "smoke_count": result.get("smoke_count", 0),
                "boxes": boxes,
            }, ensure_ascii=False))

            # 异步上报违规（不阻塞当前协程）
            await _enqueue_ai_events(pending, data)

        except Exception as e:
            print(f"[WS] process error: {e}")
        finally:
            processing = False
            # 处理完毕，若已有新帧等待则立即处理
            if latest_frame is not None:
                asyncio.create_task(process_latest())

    try:
        while True:
            data = await websocket.receive_bytes()
            latest_frame = data      # 永远只保留最新帧
            if not processing:
                asyncio.create_task(process_latest())
    except WebSocketDisconnect:
        print(f"[WS] disconnected camera={camera_id}")
    except Exception as e:
        print(f"[WS] error: {e}")


# ------------------------------------------------------------
# 8b. WebSocket：服务端预览（二进制 JPEG + JSON 元数据，720p 内缩放、15fps）
#    全分辨率标注图每帧写入 save_path/live_full_last.jpg
# ------------------------------------------------------------
@app.websocket("/ws/video_test/{camera_id}")
async def websocket_video_test(websocket: WebSocket, camera_id: int):
    await websocket.accept()
    vpath = getattr(Config, "video_test_file", os.path.join(_BASE_DIR, "TestFiles", "video.mp4"))
    if not os.path.isfile(vpath):
        await websocket.send_text(json.dumps({
            "success": False,
            "error": f"测试视频不存在: {vpath}（请将 mp4 放到 YOLOv8helmet/TestFiles/video.mp4）",
        }, ensure_ascii=False))
        await websocket.close(code=4000)
        print(f"[WS video_test] missing file: {vpath}")
        return

    cap = cv2.VideoCapture(vpath)
    if not cap.isOpened():
        await websocket.send_text(json.dumps({
            "success": False,
            "error": f"无法打开视频: {vpath}",
        }, ensure_ascii=False))
        await websocket.close(code=4001)
        return

    print(
        f"[WS video_test] camera={camera_id} file={vpath} "
        f"binary preview {PREVIEW_MAX_W}x{PREVIEW_MAX_H} cap @ {PREVIEW_FPS}fps"
    )
    loop = asyncio.get_event_loop()
    helmet_tracker = NoHelmetTrackTracker()
    last_tick = 0.0

    try:
        while True:
            now = time.monotonic()
            wait = 1.0 / PREVIEW_FPS - (now - last_tick)
            if wait > 0:
                await asyncio.sleep(wait)
            last_tick = time.monotonic()

            out = await loop.run_in_executor(
                None,
                lambda: _video_preview_infer_and_encode(cap, camera_id, annotate=False),
            )
            if out is None:
                await asyncio.sleep(0.02)
                continue

            jpeg_bytes, det = out
            d = dict(det)
            nh = d.pop("no_helmet_boxes", [])
            pend = d.pop("pending_violations", [])
            helmet_pend = helmet_tracker.update(camera_id, nh, time.time(), jpeg_bytes)
            pending = list(helmet_pend) + list(pend)

            await _enqueue_ai_events(pending, jpeg_bytes)

            helmet_new_incidents = sum(
                1 for p in pending if p[1] == VIOLATION_TYPE_NO_HELMET
            )

            meta = {
                "success": True,
                "mode": "video_preview",
                "person_count": d["person_count"],
                "moto_count": d["moto_count"],
                "helmet_violations": d["helmet_violations"],
                "helmet_new_incidents": helmet_new_incidents,
                "helmet_worn": d["helmet_worn"],
                "load_violation": d["load_violation"],
                "fire_count": d.get("fire_count", 0),
                "smoke_count": d.get("smoke_count", 0),
                "annotated": False,
                "boxes": d.get("boxes", []),
            }
            await websocket.send_bytes(pack_ws_binary_frame(meta, jpeg_bytes))
    except WebSocketDisconnect:
        print(f"[WS video_test] disconnected camera={camera_id}")
    except Exception as e:
        print(f"[WS video_test] error: {e}")
    finally:
        cap.release()


@app.websocket("/ws/local_camera/{camera_id}")
async def websocket_local_camera(websocket: WebSocket, camera_id: int):
    await websocket.accept()
    camera = next((c for c in get_camera_cache() if c["id"] == camera_id), None)
    device_index = camera.get("deviceIndex") if camera else None
    if device_index is None:
        await websocket.send_text(json.dumps({
            "success": False,
            "error": f"No local camera for slot {camera_id}",
        }, ensure_ascii=False))
        await websocket.close(code=4002)
        return

    cap = _open_local_camera(int(device_index))
    if not cap.isOpened():
        await websocket.send_text(json.dumps({
            "success": False,
            "error": f"Cannot open local camera index {device_index}",
        }, ensure_ascii=False))
        await websocket.close(code=4003)
        return

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
    print(
        f"[WS local_camera] camera={camera_id} device_index={device_index} "
        f"binary preview {PREVIEW_MAX_W}x{PREVIEW_MAX_H} cap @ {PREVIEW_FPS}fps"
    )
    loop = asyncio.get_event_loop()
    helmet_tracker = NoHelmetTrackTracker()
    last_tick = 0.0

    try:
        while True:
            now = time.monotonic()
            wait = 1.0 / PREVIEW_FPS - (now - last_tick)
            if wait > 0:
                await asyncio.sleep(wait)
            last_tick = time.monotonic()

            out = await loop.run_in_executor(
                None,
                lambda: _video_preview_infer_and_encode(cap, camera_id, annotate=False),
            )
            if out is None:
                await asyncio.sleep(0.02)
                continue

            jpeg_bytes, det = out
            d = dict(det)
            nh = d.pop("no_helmet_boxes", [])
            pend = d.pop("pending_violations", [])
            helmet_pend = helmet_tracker.update(camera_id, nh, time.time(), jpeg_bytes)
            pending = list(helmet_pend) + list(pend)

            await _enqueue_ai_events(pending, jpeg_bytes)

            helmet_new_incidents = sum(
                1 for p in pending if p[1] == VIOLATION_TYPE_NO_HELMET
            )

            meta = {
                "success": True,
                "mode": "local_camera_preview",
                "camera_id": camera_id,
                "person_count": d["person_count"],
                "moto_count": d["moto_count"],
                "helmet_violations": d["helmet_violations"],
                "helmet_new_incidents": helmet_new_incidents,
                "helmet_worn": d["helmet_worn"],
                "load_violation": d["load_violation"],
                "fire_count": d.get("fire_count", 0),
                "smoke_count": d.get("smoke_count", 0),
                "annotated": False,
                "boxes": d.get("boxes", []),
            }
            await websocket.send_bytes(pack_ws_binary_frame(meta, jpeg_bytes))
    except WebSocketDisconnect:
        print(f"[WS local_camera] disconnected camera={camera_id}")
    except Exception as e:
        print(f"[WS local_camera] error: {e}")
    finally:
        cap.release()


@app.websocket("/ws/network_camera/{camera_id}")
async def websocket_network_camera(websocket: WebSocket, camera_id: int):
    """Read a LAN MJPEG source and run the local helmet + fire/smoke YOLO models."""
    await websocket.accept()
    stream_url = NETWORK_CAMERA_STREAMS.get(camera_id)
    if not stream_url:
        await websocket.send_text(json.dumps({
            "success": False,
            "error": f"No network stream configured for camera {camera_id}",
        }, ensure_ascii=False))
        await websocket.close(code=4004)
        return

    print(f"[WS network_camera] camera={camera_id} source={stream_url} local YOLO @ {PREVIEW_FPS}fps")
    loop = asyncio.get_event_loop()
    reader = LatestNetworkFrame(stream_url).start()
    sequence = 0
    last_tick = 0.0

    try:
        while True:
            now = time.monotonic()
            wait = 1.0 / PREVIEW_FPS - (now - last_tick)
            if wait > 0:
                await asyncio.sleep(wait)
            last_tick = time.monotonic()

            sequence, frame = await loop.run_in_executor(None, reader.read_after, sequence, 2.0)
            if frame is None:
                await websocket.send_text(json.dumps({
                    "success": False,
                    "error": "Network video stream is unavailable",
                }, ensure_ascii=False))
                await asyncio.sleep(0.25)
                continue

            out = await loop.run_in_executor(None, _fire_smoke_preview_infer_and_encode, frame, camera_id)
            if out is None:
                continue

            jpeg_bytes, det = out
            pending = list(det.get("pending_violations", []))
            await _enqueue_ai_events(pending, jpeg_bytes)

            meta = {
                "success": True,
                "mode": "network_camera_preview",
                "camera_id": camera_id,
                "model": "local-fire-smoke",
                "fire_count": det.get("fire_count", 0),
                "smoke_count": det.get("smoke_count", 0),
                "annotated": False,
                "boxes": det.get("boxes", []),
            }
            await websocket.send_bytes(pack_ws_binary_frame(meta, jpeg_bytes))
    except WebSocketDisconnect:
        print(f"[WS network_camera] disconnected camera={camera_id}")
    except Exception as e:
        print(f"[WS network_camera] error: {e}")
    finally:
        reader.stop()


# ------------------------------------------------------------
# 9. 启动
# ------------------------------------------------------------
if __name__ == "__main__":
    multiprocessing.freeze_support()
    _warm_up_model()
    uvicorn.run(app, host="0.0.0.0", port=8000, workers=1)
