# ============================================================
# 校园电动车监测管理系统 - 一键启动脚本
# 用法:
#   .\start-all.ps1                  # 启动全部服务
#   .\start-all.ps1 -SkipBridge      # 不启动 PyBullet/F2C 桥接
#   .\start-all.ps1 -BackendOnly     # 只启动后端
#   .\start-all.ps1 -FrontendOnly    # 只启动前端
#   .\start-all.ps1 -Rebuild         # 启动前重新 mvn 编译后端
# ============================================================
[CmdletBinding()]
param(
    [switch]$SkipBridge,
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$Rebuild,
    [switch]$NoDsh
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$PROJECT_ROOT = 'E:\资源1\校园电动车监测管理系统'
$BACKEND_DIR  = Join-Path $PROJECT_ROOT '电动车管理系统springboot\ev-monitor-platform'
$FRONTEND_DIR = Join-Path $PROJECT_ROOT 'vue-admin-box-master\vue-admin-box-master'
$LOG_DIR      = Join-Path $PROJECT_ROOT 'logs'
if (-not (Test-Path $LOG_DIR)) { New-Item -ItemType Directory -Path $LOG_DIR | Out-Null }

# ---------- 工具函数 ----------
function Write-Step($msg)  { Write-Host "`n[步骤] $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "  [!]  $msg" -ForegroundColor Yellow }
function Write-Err($msg)   { Write-Host "  [X]  $msg" -ForegroundColor Red }

function Test-Port($port) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return [bool]$conn
}

function Stop-Port($port) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        $proc = Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Warn "端口 $port 被进程 $($proc.ProcessName)(PID $($proc.Id)) 占用，正在终止..."
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 1
        }
    }
}

function Wait-Port($port, $timeoutSec = 30, $name = '服务') {
    Write-Host "  等待 $name 监听端口 $port ..." -NoNewline
    $elapsed = 0
    while ($elapsed -lt $timeoutSec) {
        if (Test-Port $port) { Write-Host " 就绪" -ForegroundColor Green; return $true }
        Start-Sleep -Seconds 1
        $elapsed++
        Write-Host "." -NoNewline
    }
    Write-Host " 超时(${timeoutSec}s)" -ForegroundColor Red
    return $false
}

# ---------- 0. 环境检查 ----------
Write-Step "环境检查"
$javaOk = $null -ne (Get-Command java -ErrorAction SilentlyContinue)
$mvnOk  = $null -ne (Get-Command mvn -ErrorAction SilentlyContinue)
$nodeOk = $null -ne (Get-Command node -ErrorAction SilentlyContinue)
$wslOk  = $null -ne (Get-Command wsl -ErrorAction SilentlyContinue)
"  Java:  $(if($javaOk){'OK'}else{'缺失'})   Maven: $(if($mvnOk){'OK'}else{'缺失'})   Node: $(if($nodeOk){'OK'}else{'缺失'})   WSL: $(if($wslOk){'OK'}else{'缺失'})"

# ---------- 1. 后端 Spring Boot (8083) ----------
if (-not $FrontendOnly) {
    Write-Step "启动后端 Spring Boot :8083"

    if (Test-Port 8083) {
        Write-Warn "8083 已被占用，先释放..."
        Stop-Port 8083
    }

    if ($Rebuild) {
        Write-Host "  重新编译后端 (mvn clean package -DskipTests)..."
        Push-Location $BACKEND_DIR
        & mvn clean package -DskipTests -q
        if ($LASTEXITCODE -ne 0) { Write-Err "后端编译失败"; Pop-Location; exit 1 }
        Pop-Location
        Write-Ok "后端编译完成"
    }

    $jar = Join-Path $BACKEND_DIR 'target\ev-monitor-platform-1.0.0.jar'
    if (-not (Test-Path $jar)) {
        Write-Err "找不到 jar: $jar，请加 -Rebuild 编译"
        exit 1
    }

    $logFile = Join-Path $LOG_DIR 'backend.log'
    Start-Process -FilePath java -ArgumentList @('-jar', $jar) `
        -WorkingDirectory $BACKEND_DIR `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError (Join-Path $LOG_DIR 'backend-err.log') `
        -WindowStyle Hidden
    Write-Ok "后端进程已启动，日志: $logFile"

    if (-not (Wait-Port 8083 -timeoutSec 40 -name '后端')) {
        Write-Err "后端启动超时，请检查 logs\backend.log"
        exit 1
    }
    Write-Ok "后端 http://localhost:8083 已就绪"
}

# ---------- 2. PyBullet 无人机仿真桥 (9030) ----------
if (-not ($SkipBridge -or $BackendOnly -or $FrontendOnly)) {
    Write-Step "启动 PyBullet 仿真桥 :9030 (WSL)"
    if (Test-Port 9030) { Stop-Port 9030; Start-Sleep 1 }

    $pybridge = '/mnt/e/资源1/校园电动车监测管理系统/tools/pybullet-bridge/pybullet-drone-bridge.py'
    $pyRepo   = '/mnt/e/资源1/校园电动车监测管理系统/开源参考/gym-pybullet-drones'
    $logFile = Join-Path $LOG_DIR 'pybullet-bridge.log'

    # 先停旧实例，再把最新源码部署到 ~/flysim/bridge.py，用 flysim-venv 后台运行
    Start-Process -FilePath wsl -ArgumentList @(
        '-e', 'bash', '-c',
        "pkill -f '[f]lysim/bridge.py' 2>/dev/null; sleep 1; mkdir -p ~/flysim; cp '$pybridge' ~/flysim/bridge.py; chmod +x ~/flysim/bridge.py; PYTHONPATH='$pyRepo' setsid nohup ~/flysim-venv/bin/python ~/flysim/bridge.py --host 0.0.0.0 --port 9030 --speed 1.5 --time-scale 1.0 > ~/flysim/bridge.log 2>&1 < /dev/null &"
    ) -WindowStyle Hidden
    Write-Ok "PyBullet 桥已启动（首次加载物理引擎约需 8 秒），WSL 日志: ~/flysim/bridge.log"

    if (Wait-Port 9030 -timeoutSec 25 -name 'PyBullet桥') {
        try { $h = Invoke-RestMethod -Uri 'http://localhost:9030/health' -TimeoutSec 3; Write-Ok "PyBullet 桥 http://localhost:9030 已就绪 (missions=$($h.missions))" }
        catch { Write-Warn "PyBullet 桥端口已监听但 health 异常" }
    } else {
        Write-Warn "PyBullet 桥未就绪，巡检仿真功能可能不可用（不影响大屏基础功能）"
    }
}

# ---------- 3. Fields2Cover 覆盖规划桥 (9031) ----------
if (-not ($SkipBridge -or $BackendOnly -or $FrontendOnly)) {
    Write-Step "启动 Fields2Cover 覆盖规划桥 :9031 (WSL)"
    if (Test-Port 9031) { Stop-Port 9031; Start-Sleep 1 }

    $f2cSh = '/mnt/e/资源1/校园电动车监测管理系统/tools/f2c-bridge/start-f2c-bridge-wsl.sh'
    $logFile = Join-Path $LOG_DIR 'f2c-bridge.log'

    Start-Process -FilePath wsl -ArgumentList @('-e', 'bash', $f2cSh) `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError (Join-Path $LOG_DIR 'f2c-bridge-err.log') `
        -WindowStyle Hidden
    Write-Ok "F2C 桥已启动，日志: $logFile"

    if (Wait-Port 9031 -timeoutSec 20 -name 'F2C覆盖规划桥') {
        Write-Ok "F2C 桥 http://localhost:9031 已就绪"
    } else {
        Write-Warn "F2C 桥未就绪，全覆盖航线规划功能可能不可用"
    }
}

# ---------- 4. 前端 Vite (3002) ----------
if (-not $BackendOnly) {
    Write-Step "启动前端 Vite :3002"
    if (Test-Port 3002) { Stop-Port 3002; Start-Sleep 1 }

    $logFile = Join-Path $LOG_DIR 'frontend.log'
    Start-Process -FilePath npm.cmd -ArgumentList @('run', 'dev') `
        -WorkingDirectory $FRONTEND_DIR `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError (Join-Path $LOG_DIR 'frontend-err.log') `
        -WindowStyle Hidden
    Write-Ok "前端进程已启动，日志: $logFile"

    if (Wait-Port 3002 -timeoutSec 30 -name '前端') {
        Write-Ok "前端 http://localhost:3002 已就绪"
    } else {
        Write-Warn "前端启动超时，请检查 logs\frontend.log"
    }
}

# ---------- 5. DSH 校园运行时 (8092，可选) ----------
if (-not ($NoDsh -or $BackendOnly -or $FrontendOnly)) {
    Write-Step "启动 DSH 校园运行时 :8092 (可选)"
    if (Test-Port 8092) {
        Write-Warn "8092 已被占用，跳过 DSH"
    } else {
        $dshScript = Join-Path $PROJECT_ROOT 'scripts\start-campus-runtime.ps1'
        if (Test-Path $dshScript) {
            Start-Process -FilePath pwsh -ArgumentList @('-NoLogo', '-NoProfile', '-File', $dshScript) `
                -RedirectStandardOutput (Join-Path $LOG_DIR 'dsh.log') `
                -WindowStyle Hidden
            Write-Ok "DSH 运行时已启动"
        } else {
            Write-Warn "未找到 start-campus-runtime.ps1，跳过 DSH"
        }
    }
}

# ---------- 完成 ----------
Write-Step "启动完成"
Write-Host ""
Write-Host "  大屏地址  : http://localhost:3002/#/dashboard" -ForegroundColor Green
Write-Host "  后端 API  : http://localhost:8083" -ForegroundColor Green
Write-Host "  API 文档  : http://localhost:8083/swagger-ui.html" -ForegroundColor Green
Write-Host "  登录账号  : admin / 123456" -ForegroundColor Green
Write-Host ""
Write-Host "  日志目录  : $LOG_DIR" -ForegroundColor Gray
Write-Host "  停止服务  : 关闭对应的 java/node/wsl 进程，或运行 stop-all.ps1" -ForegroundColor Gray
Write-Host ""
