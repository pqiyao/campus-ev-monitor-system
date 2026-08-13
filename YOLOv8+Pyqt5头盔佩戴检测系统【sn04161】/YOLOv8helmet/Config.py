#coding:utf-8
import os
import sys


def _runtime_base_dir():
    if getattr(sys, "_MEIPASS", None):
        return sys._MEIPASS
    if getattr(sys, "frozen", False):
        return os.path.dirname(sys.executable)
    return os.path.dirname(os.path.abspath(__file__))


def resource_path(*parts):
    return os.path.join(_runtime_base_dir(), *parts)


save_path = resource_path("save_data")
video_test_file = resource_path("TestFiles", "video.mp4")
model_path = resource_path("models", "best.pt")
fire_smoke_model_path = resource_path("models", "fire_smoke_best.pt")

names = {0: "helmet", 1: "without", 2: "two_wheeler"}
CH_names = ["头盔", "未佩戴", "非机动车"]
