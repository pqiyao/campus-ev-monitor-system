# ============================================================
# 校园电动车监测管理系统 - 停止脚本
# 关闭后端、前端、PyBullet 桥、F2C 桥、DSH 运行时
# ============================================================
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$ports = @(
    @{Port = 8083; Name = '后端 Spring Boot'},
    @{Port = 3002; Name = '前端 Vite'},
    @{Port = 9030; Name = 'PyBullet 桥'},
    @{Port = 9031; Name = 'F2C 覆盖规划桥'},
    @{Port = 8092; Name = 'DSH 运行时'}
)

Write-Host "`n正在停止服务..." -ForegroundColor Cyan

foreach ($p in $ports) {
    $conns = @(Get-NetTCPConnection -LocalPort $p.Port -State Listen -ErrorAction SilentlyContinue)
    if ($conns) {
        foreach ($c in $conns) {
            $proc = Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue
            if ($proc) {
                Write-Host "  停止 $($p.Name) (PID $($proc.Id): $($proc.ProcessName))" -ForegroundColor Yellow
                Stop-Process -Id $proc.Id -Force -ErrorAction Stop
            }
        }
    } else {
        Write-Host "  $($p.Name) 未运行" -ForegroundColor Gray
    }
}

# 清理 WSL 中的 python 桥接进程
Write-Host "`n清理 WSL 桥接进程..." -ForegroundColor Cyan
wsl -e bash -c "pkill -f '[f]lysim/bridge.py' 2>/dev/null; pkill -f 'pybullet-drone-bridge.py' 2>/dev/null; pkill -f 'f2c-coverage-bridge.py' 2>/dev/null; echo done"
if ($LASTEXITCODE -ne 0) { throw "WSL 桥接进程清理失败，退出码：$LASTEXITCODE" }

Write-Host "`n所有服务已停止。" -ForegroundColor Green
