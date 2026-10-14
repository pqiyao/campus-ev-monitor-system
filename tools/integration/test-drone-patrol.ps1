# PyBullet drone patrol integration test:
# login -> create mission -> plan route -> start -> poll until COMPLETED
# Requires backend started with DRONE_EXECUTOR=pybullet and bridge on 127.0.0.1:9030
$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:8083"

function Post($uri, $body, $headers) {
    Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" -Body $body -Headers $headers
}

# 1. login
$login = Post "$base/api/user/login" '{"username":"admin","password":"123456"}' @{}
$token = $login.data.token
if (-not $token) { $token = $login.data.accessToken }
if (-not $token) { Write-Output "LOGIN-FAIL"; exit 1 }
$H = @{ Authorization = "Bearer $token" }
Write-Output "[1] login OK"

# 2. create mission (defaults: center = campus home point)
$m = Post "$base/api/patrol/missions" '{}' $H
$id = $m.data.mission_id
Write-Output "[2] mission created id=$id code=$($m.data.mission_code)"

# 3. plan route (risk grid + geofence constraints)
$p = Post "$base/api/patrol/missions/$id/plan" "{}" $H
$wpCount = $p.data.waypoints.Count
Write-Output "[3] planned: waypoints=$wpCount state=$($p.data.state)"
if ($wpCount -eq 0) { Write-Output "PLAN-FAIL: no waypoints"; exit 1 }

# 4. start (dispatches to drone executor -> PyBullet bridge)
$s = Post "$base/api/patrol/missions/$id/start" "{}" $H
Write-Output "[4] started: state=$($s.data.state)"

# 5. poll until terminal state
$final = $null
for ($i = 1; $i -le 66; $i++) {
    Start-Sleep -Seconds 5
    $o = Invoke-RestMethod -Uri "$base/api/patrol/missions/$id" -Headers $H
    $state = $o.data.state
    $arrived = ($o.data.waypoints | Where-Object { $_.state -eq "ARRIVED" }).Count
    $tele = $o.data.telemetry.Count
    Write-Output "  t+$($i*5)s state=$state arrivedWp=$arrived/$wpCount telemetry=$tele"
    if ($state -in @("COMPLETED", "FAILED", "ABORTED")) { $final = $o.data; break }
}
if (-not $final) { Write-Output "DRONE-RESULT: TIMEOUT"; exit 1 }
Write-Output "[5] final state=$($final.state) telemetry=$($final.telemetry.Count)"
if ($final.state -eq "COMPLETED" -and $final.telemetry.Count -gt 0) { Write-Output "DRONE-RESULT: PASS" } else { Write-Output "DRONE-RESULT: CHECK" }
