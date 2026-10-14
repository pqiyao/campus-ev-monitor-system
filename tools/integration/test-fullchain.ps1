# Air-ground full-chain integration test:
# login -> register ground robot -> thermal frame ingest -> HIGH event -> dispatch -> review evidence
# Usage: powershell -ExecutionPolicy Bypass -File tools\integration\test-fullchain.ps1
$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:8083"

function Post($uri, $body, $headers) {
    Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" -Body $body -Headers $headers
}

function Build-Frame([string]$capturedAt, [double]$peak) {
    $matrix = @()
    for ($r = 0; $r -lt 12; $r++) {
        $row = @()
        for ($c = 0; $c -lt 12; $c++) {
            if ($r -ge 4 -and $r -le 7 -and $c -ge 4 -and $c -le 7) { $row += $peak } else { $row += 28.0 }
        }
        $matrix += ,$row
    }
    return @{
        deviceId = "ir-drone-01"; sourceType = "DRONE"
        capturedAt = $capturedAt
        longitude = 110.3250000; latitude = 25.2890000; locationName = "East-Charging-Shed"
        tempMatrix = $matrix; ambientTemp = 28.0; sceneType = "CHARGING_STATION"
        frameSeq = [int]((Get-Date).ToBinary() % 100000)
        idempotencyKey = "fullchain-" + (Get-Date -Format "yyyyMMddHHmmssfff")
        pixelMeters = 0.5
    } | ConvertTo-Json -Depth 5
}

# 1. login
$login = Post "$base/api/user/login" '{"username":"admin","password":"123456"}' @{}
$token = $login.data.token
if (-not $token) { $token = $login.data.accessToken }
if (-not $token) { $token = $login.token }
if (-not $token) { Write-Output "LOGIN-FAIL: $($login | ConvertTo-Json -Depth 4)"; exit 1 }
Write-Output "[1] login OK"
$H = @{ Authorization = "Bearer $token" }

# 2. register ground robot (upsert)
$robot = @{ robotCode = "bot-001"; name = "patrol-bot-01"; currentLng = 110.3239130; currentLat = 25.2875179; battery = 100; state = "IDLE" } | ConvertTo-Json
Post "$base/api/dispatch/robots" $robot $H | Out-Null
Write-Output "[2] robot registered"

# 3. thermal frames (12x12, center 4x4 hotspot): frame1 120C then frame2 320C (+10s)
#    -> rise rate 20C/s, risk score ~0.90 >= HIGH threshold 0.78, escalates to AI event
$ts = Get-Date
$t1 = $ts.ToString("yyyy-MM-dd HH:mm:ss")
$t2 = $ts.AddSeconds(10).ToString("yyyy-MM-dd HH:mm:ss")
$frame1 = Build-Frame $t1 120.0
$ingest = Post "$base/api/thermal/ingest" $frame1 @{"X-AI-Ingest-Key" = "campus-yolo-local"}
Write-Output "[3a] frame1: riskLevel=$($ingest.data.risk_level) score=$($ingest.data.risk_score)"
$frame2 = Build-Frame $t2 320.0
$ingest = Post "$base/api/thermal/ingest" $frame2 @{"X-AI-Ingest-Key" = "campus-yolo-local"}
$eventId = $ingest.data.event_id
if (-not $eventId) { $eventId = $ingest.data.eventId }
Write-Output "[3b] frame2: riskLevel=$($ingest.data.risk_level) score=$($ingest.data.risk_score) eventId=$eventId"
if (-not $eventId) { Write-Output "NO-HIGH-EVENT: $($ingest.data | ConvertTo-Json -Depth 3)"; exit 1 }

# 4. dispatch via DispatchEngine scoring
$disp = Post "$base/api/dispatch/events/$eventId/dispatch" "{}" $H
$orderId = $disp.data.order_id
if (-not $orderId) { Write-Output "DISPATCH-FAIL: $($disp | ConvertTo-Json -Depth 4)"; exit 1 }
Write-Output "[4] dispatched: orderId=$orderId robot=$($disp.data.robotCode) score=$($disp.data.score.total)"

# 5. poll order until terminal state
$final = $null
for ($i = 1; $i -le 40; $i++) {
    Start-Sleep -Seconds 5
    $o = Invoke-RestMethod -Uri "$base/api/dispatch/orders/$orderId" -Headers $H
    $state = $o.data.state
    Write-Output "  t+$($i*5)s state=$state last_event=$($o.data.last_event) telemetry=$($o.data.telemetry.Count)"
    if ($state -in @("COMPLETED", "FAILED", "BLOCKED")) { $final = $o.data; break }
}
if (-not $final) { Write-Output "FULLCHAIN-RESULT: TIMEOUT"; exit 1 }
Write-Output "[5] final state=$($final.state)"
Write-Output "reviewEvents: $($final.reviewEvents | ConvertTo-Json -Depth 4)"
if ($final.state -eq "COMPLETED" -and $final.reviewEvents.Count -gt 0) { Write-Output "FULLCHAIN-RESULT: PASS" } else { Write-Output "FULLCHAIN-RESULT: CHECK" }
