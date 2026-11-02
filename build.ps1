# 옛글 키보드 APK 빌드 (Windows PowerShell)
# 필요: JDK 17, Android SDK (Android Studio를 설치하면 함께 깔림)
# 사용: PowerShell에서  .\build.ps1          → APK 빌드
#                       .\build.ps1 -Install → 빌드 후 USB로 연결된 폰에 설치 (adb 필요)
param([switch]$Install)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not $env:ANDROID_HOME) {
    $guess = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $guess) { $env:ANDROID_HOME = $guess }
    else { throw 'Android SDK를 찾지 못했습니다. Android Studio를 설치하거나 ANDROID_HOME을 설정하세요.' }
}
"sdk.dir=$($env:ANDROID_HOME -replace '\\','\\')" | Set-Content -Encoding ASCII local.properties

& .\gradlew.bat --no-daemon assembleRelease
if ($LASTEXITCODE -ne 0) { throw '빌드 실패' }

$apk = 'app\build\outputs\apk\release\app-release.apk'
Copy-Item $apk .\yetgeul-keyboard.apk -Force
Write-Host "완료: $(Resolve-Path .\yetgeul-keyboard.apk)"

if ($Install) {
    $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
    & $adb install -r .\yetgeul-keyboard.apk
}
