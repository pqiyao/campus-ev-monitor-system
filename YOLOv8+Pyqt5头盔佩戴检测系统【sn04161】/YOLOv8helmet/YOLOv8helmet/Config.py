#coding:utf-8
import os

# 图片及视频检测结果保存路径
save_path = 'save_data'

# 与 VideoTest.py 一致：OpenCV 读取的本地测试视频（绝对路径，避免工作目录不一致）
_SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
video_test_file = os.path.join(_SCRIPT_DIR, "TestFiles", "video.mp4")

# 使用的模型路径
model_path = 'models/best.pt'
names = {0: 'helmet',1:"without",2:"two_wheeler"}
CH_names = ['头盔','未佩戴','非机动车']
