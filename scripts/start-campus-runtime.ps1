$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$runtimeRoot = 'C:\Users\yao\Desktop\AiSillyTavern\JiuGAi\DeepSeekHarness'
$campusRoot = 'E:\资源1\校园电动车监测管理系统'
$env:DSH_HOME = Join-Path $campusRoot 'dsh-campus-home'
$env:APP_AGENT_DSH_RUNTIME_ROOT = $runtimeRoot
$env:DSH_HOST = '127.0.0.1'
$env:DSH_PORT = '8092'
Set-Location -LiteralPath $runtimeRoot
& pnpm.cmd exec dsh web --host 127.0.0.1 --port 8092 --no-open
if ($LASTEXITCODE -ne 0) { throw "校园智能运行时启动失败，退出码：$LASTEXITCODE" }
