#!/usr/bin/env bash
# Fields2Cover 覆盖规划桥接 WSL 启动脚本
# 1) 自动发现 E 盘上的 f2c-bridge 脚本（WSL drvfs 对中文目录名的编码转换不稳定，
#    不能在脚本里写字面中文路径，必须按名字逐层探测）
# 2) 同步到 WSL 本地纯 ASCII 路径后，用 f2c conda 环境启动
/home/yao/miniconda3/envs/f2c/bin/python - <<'PYEOF'
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
# 项目根：其下唯一含 tools/f2c-bridge 的非 ASCII 目录
bridge_src = None
for cand in nonascii_dirs(e1[0]):
    cand_path = os.path.join(cand, "tools")
    if not os.path.isdir(cand_path):
        continue
    for sub in os.listdir(cand_path):
        full = os.path.join(cand_path, sub)
        if os.path.isdir(full) and os.path.exists(os.path.join(full, "f2c-coverage-bridge.py")):
            bridge_src = os.path.join(full, "f2c-coverage-bridge.py")
print("BRIDGE:", repr(bridge_src))
assert bridge_src, "f2c-coverage-bridge.py not found"

os.makedirs("/home/yao/f2c-bridge", exist_ok=True)
shutil.copy(bridge_src, "/home/yao/f2c-bridge/f2c-coverage-bridge.py")
print("SYNC-OK")
PYEOF
exec /home/yao/miniconda3/envs/f2c/bin/python /home/yao/f2c-bridge/f2c-coverage-bridge.py --host 0.0.0.0 --port 9031
