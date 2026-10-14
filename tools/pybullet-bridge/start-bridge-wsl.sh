#!/usr/bin/env bash
# PyBullet 无人机桥接 WSL 启动脚本
# 1) 自动发现 E 盘上的 gym-pybullet-drones 与桥接脚本（WSL drvfs 对中文目录名的编码
#    转换不稳定，不能在脚本里写字面中文路径，必须按名字逐层探测）
# 2) 同步到 WSL 本地纯 ASCII 路径后启动
/home/yao/flysim-venv/bin/python - <<'PYEOF'
import os
import shutil

def nonascii_dirs(base):
    try:
        names = os.listdir(base)
    except Exception:
        return []
    return [os.path.join(base, n) for n in names
            if os.path.isdir(os.path.join(base, n)) and any(ord(c) > 127 for c in n)]

# /mnt/e 下乱码化的"资源1"：唯一以 '1' 结尾的非 ASCII 目录
e1 = [d for d in nonascii_dirs("/mnt/e") if os.path.basename(d).endswith("1")]
assert len(e1) == 1, "E-drive resource dir not unique"
# 项目根：其下唯一含 gym-pybullet-drones 的非 ASCII 目录
repo = None
proj_root = None
for cand in nonascii_dirs(e1[0]):
    for sub in os.listdir(cand):
        full = os.path.join(cand, sub)
        if os.path.isdir(full) and os.path.isdir(os.path.join(full, "gym-pybullet-drones")):
            repo = os.path.join(full, "gym-pybullet-drones")
            proj_root = cand
print("REPO:", repr(repo))
assert repo, "gym-pybullet-drones not found"
tools_dir = os.path.join(proj_root, "tools")
bridge_src = None
for sub in os.listdir(tools_dir):
    full = os.path.join(tools_dir, sub)
    if os.path.isdir(full):
        cand = os.path.join(full, "pybullet-drone-bridge.py")
        if os.path.exists(cand):
            bridge_src = cand
print("BRIDGE:", repr(bridge_src))
assert bridge_src, "bridge script not found"

link = "/home/yao/gym-pybullet-drones"
if os.path.islink(link):
    os.remove(link)
elif os.path.exists(link):
    shutil.rmtree(link)
os.symlink(repo, link)
os.makedirs("/home/yao/flysim", exist_ok=True)
shutil.copy(bridge_src, "/home/yao/flysim/bridge.py")
print("SYNC-OK")
PYEOF
export PYTHONPATH=/home/yao/gym-pybullet-drones:${PYTHONPATH:-}
exec /home/yao/flysim-venv/bin/python /home/yao/flysim/bridge.py --host 0.0.0.0 --port 9030
