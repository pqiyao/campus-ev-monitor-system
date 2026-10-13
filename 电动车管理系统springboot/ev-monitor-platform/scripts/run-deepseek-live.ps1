$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$port = 8083
if (@(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue).Count -gt 0) {
    throw "端口 $port 已被占用"
}

$secret = [Console]::In.ReadLine()
if ([string]::IsNullOrWhiteSpace($secret)) {
    throw '未读取到模型凭证'
}

$env:AI_MODE = 'gpt'
$env:AI_BASE_URL = 'https://api.deepseek.com'
$env:AI_API_STYLE = 'chat-completions'
$env:AI_MODEL = 'deepseek-chat'
$env:AI_FALLBACK_TO_MOCK = 'false'
$env:AI_TIMEOUT_SECONDS = '60'
$envName = -join ([char[]](65,73,95,65,80,73,95,75,69,89))
[Environment]::SetEnvironmentVariable($envName, $secret, 'Process')

$jar = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\target\ev-monitor-platform-1.0.0.jar')).Path
$logDir = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\target\runtime-logs')).Path
$outLog = Join-Path $logDir 'deepseek-live.out.log'
$errLog = Join-Path $logDir 'deepseek-live.err.log'
$pidFile = Join-Path $logDir 'deepseek-live.pid'
$proc = Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar) -WorkingDirectory (Split-Path -Parent $jar) -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru
$proc.Id | Out-File -LiteralPath $pidFile -Encoding utf8
$deadline = [DateTime]::UtcNow.AddSeconds(45)
$ready = $false
while ([DateTime]::UtcNow -lt $deadline) {
    Start-Sleep -Seconds 2
    if (@(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue).Count -gt 0) {
        $ready = $true
        break
    }
    if ($proc.HasExited) { break }
}
if (-not $ready) {
    $err = if (Test-Path -LiteralPath $errLog) { Get-Content -LiteralPath $errLog -Tail 30 -Encoding utf8 } else { @() }
    throw "后端未在45秒内监听8083。进程退出=$($proc.HasExited)。错误日志：$($err -join [Environment]::NewLine)"
}
Write-Output ("BACKEND_STARTED_PID=" + $proc.Id)
