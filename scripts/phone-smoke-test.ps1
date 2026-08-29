param(
    [string]$Serial,
    [string]$ApkPath,
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($adbCommand) {
    $adbExe = $adbCommand.Source
} else {
    $adbExe = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
}
if (-not (Test-Path -LiteralPath $adbExe)) {
    throw "Không tìm thấy adb. Hãy cài Android SDK Platform Tools hoặc thêm adb vào PATH."
}

$deviceRows = & $adbExe devices | Select-Object -Skip 1 | Where-Object { $_ -match '^\S+\s+\S+' }
$devices = foreach ($row in $deviceRows) {
    if ($row -match '^(\S+)\s+(\S+)') {
        [pscustomobject]@{ Serial = $Matches[1]; State = $Matches[2] }
    }
}

if ([string]::IsNullOrWhiteSpace($Serial)) {
    $physicalDevices = @($devices | Where-Object { $_.State -eq 'device' -and $_.Serial -notmatch '^emulator-' })
    if ($physicalDevices.Count -eq 0) {
        throw "Không có điện thoại Android đã authorize trong adb devices."
    }
    if ($physicalDevices.Count -gt 1) {
        throw "Có nhiều điện thoại. Chạy lại với -Serial <serial>."
    }
    $Serial = $physicalDevices[0].Serial
}

$selected = $devices | Where-Object Serial -eq $Serial | Select-Object -First 1
if (-not $selected) {
    throw "Không tìm thấy thiết bị '$Serial' trong adb devices."
}
if ($selected.State -ne 'device') {
    throw "Thiết bị '$Serial' đang ở trạng thái '$($selected.State)'. Hãy mở khóa và chấp nhận RSA."
}
if ($Serial -match '^emulator-') {
    throw "Script này dành cho điện thoại vật lý; serial '$Serial' là emulator."
}
if (-not $SkipInstall -and -not (Test-Path -LiteralPath $ApkPath)) {
    throw "Không tìm thấy APK: $ApkPath"
}

function Invoke-PhoneAdb {
    param([Parameter(Mandatory)][string[]]$Arguments)
    & $adbExe -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb thất bại: $($Arguments -join ' ')"
    }
}

$safeSerial = $Serial -replace '[^A-Za-z0-9._-]', '_'
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$evidenceDir = Join-Path $projectRoot ".jtmp\phone-smoke\$runId-$safeSerial"
New-Item -ItemType Directory -Path $evidenceDir -Force | Out-Null

function Save-PhoneDump {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string[]]$Arguments
    )
    $output = & $adbExe -s $Serial @Arguments 2>&1
    $output | Set-Content -LiteralPath (Join-Path $evidenceDir "$Name.txt") -Encoding utf8
    return ($output | Out-String)
}

Write-Host "Thiết bị: $Serial"
Write-Host "Thư mục bằng chứng: $evidenceDir"

if (-not $SkipInstall) {
    Write-Host "Cài APK: $ApkPath"
    Invoke-PhoneAdb -Arguments @('install', '-r', $ApkPath)
}

Invoke-PhoneAdb -Arguments @('shell', 'am', 'force-stop', 'com.risealarm')
Invoke-PhoneAdb -Arguments @('shell', 'am', 'start', '-W', '-n', 'com.risealarm/.MainActivity')

Write-Host "`nTrên điện thoại: xác nhận app vào thẳng Trang chủ."
Write-Host "Tạo báo thức một lần, cách hiện tại 2-3 phút; bật Rung và lưu."
Read-Host 'Nhấn Enter sau khi đã lưu báo thức'

$scheduled = Save-PhoneDump -Name '01-scheduled-alarm' -Arguments @('shell', 'dumpsys', 'alarm')
if ($scheduled -notmatch 'com\.risealarm\.action\.FIRE_ALARM') {
    throw "Không thấy exact alarm của com.risealarm trong dumpsys alarm."
}
Write-Host "PASS: hệ thống đã nhận lịch báo thức RISE."

Invoke-PhoneAdb -Arguments @('shell', 'input', 'keyevent', '3')
Invoke-PhoneAdb -Arguments @('shell', 'am', 'kill', 'com.risealarm')
Write-Host "`nKhóa màn hình điện thoại và chờ báo thức reo."
Read-Host 'Khi báo thức đang reo và màn hình alarm đã hiện, nhấn Enter'

$ringingActivity = Save-PhoneDump -Name '02-ringing-activity' -Arguments @('shell', 'dumpsys', 'activity', 'activities')
$ringingService = Save-PhoneDump -Name '03-ringing-service' -Arguments @('shell', 'dumpsys', 'activity', 'services', 'com.risealarm')
$null = Save-PhoneDump -Name '04-ringing-notification' -Arguments @('shell', 'dumpsys', 'notification', '--noredact')
$ringingAudio = Save-PhoneDump -Name '05-ringing-audio' -Arguments @('shell', 'dumpsys', 'audio')
$null = Save-PhoneDump -Name '06-ringing-vibrator' -Arguments @('shell', 'dumpsys', 'vibrator_manager')
$null = Save-PhoneDump -Name '07-ringing-power' -Arguments @('shell', 'dumpsys', 'power')
Invoke-PhoneAdb -Arguments @('shell', 'screencap', '-p', '/sdcard/rise-phone-smoke.png')
Invoke-PhoneAdb -Arguments @('pull', '/sdcard/rise-phone-smoke.png', (Join-Path $evidenceDir '08-ringing-screen.png'))

if ($ringingActivity -match 'com\.risealarm/\.AlarmActivity') {
    Write-Host 'PASS: AlarmActivity đang hiển thị.'
} else {
    Write-Warning 'Không thấy AlarmActivity ở foreground; kiểm tra quyền Full-screen alarm trong Cài đặt.'
}
if ($ringingService -match 'AlarmRingingService' -and $ringingService -match 'isForeground=true') {
    Write-Host 'PASS: foreground ringing service đang chạy.'
} else {
    Write-Warning 'Không thấy foreground ringing service.'
}
if ($ringingAudio -match 'com\.risealarm' -and $ringingAudio -match 'USAGE_ALARM') {
    Write-Host 'PASS: audio path dùng USAGE_ALARM.'
} else {
    Write-Warning 'Không xác nhận được USAGE_ALARM trong dumpsys audio.'
}

$heardAlarm = Read-Host 'Bạn có nghe rõ chuông từ loa điện thoại? (y/n)'
$feltVibration = Read-Host 'Bạn có cảm nhận điện thoại rung? (y/n)'
$lockScreenShown = Read-Host 'Màn hình alarm có xuất hiện khi máy đang khóa? (y/n)'

Write-Host "`nGiữ nút tắt khoảng 1 giây rồi thả; chuông phải tiếp tục."
Read-Host 'Nhấn Enter sau khi đã thử giữ ngắn'
$shortHoldService = Save-PhoneDump -Name '09-after-short-hold-service' -Arguments @('shell', 'dumpsys', 'activity', 'services', 'com.risealarm')
if ($shortHoldService -match 'AlarmRingingService' -and $shortHoldService -match 'isForeground=true') {
    Write-Host 'PASS: giữ ngắn không tắt báo thức.'
} else {
    Write-Warning 'Service đã dừng sau giữ ngắn; ghi nhận đây là lỗi.'
}

Write-Host "`nGiữ liên tục ít nhất 3 giây để tắt báo thức."
Read-Host 'Nhấn Enter sau khi báo thức đã dừng'
$postService = Save-PhoneDump -Name '10-after-dismiss-service' -Arguments @('shell', 'dumpsys', 'activity', 'services', 'com.risealarm')
$null = Save-PhoneDump -Name '11-after-dismiss-audio' -Arguments @('shell', 'dumpsys', 'audio')

$summary = @(
    "serial=$Serial"
    "apk=$ApkPath"
    "heard_alarm=$heardAlarm"
    "felt_vibration=$feltVibration"
    "lock_screen_shown=$lockScreenShown"
    "short_hold_kept_service=$($shortHoldService -match 'isForeground=true')"
    "dismiss_stopped_service=$($postService -notmatch 'AlarmRingingService')"
)
$summary | Set-Content -LiteralPath (Join-Path $evidenceDir 'summary.txt') -Encoding utf8

if ($postService -notmatch 'AlarmRingingService') {
    Write-Host 'PASS: giữ đủ 3 giây đã dừng service.'
} else {
    Write-Warning 'Service vẫn còn sau khi giữ đủ 3 giây.'
}
Write-Host "`nHoàn tất. Bằng chứng được lưu tại: $evidenceDir"
