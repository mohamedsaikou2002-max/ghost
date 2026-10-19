$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$GradleVersion = '8.9'
$GradleDir = Join-Path $Root '.tooling\gradle-8.9'
$GradleExe = Join-Path $GradleDir 'bin\gradle.bat'

if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
    $Candidate = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $Candidate) { $env:ANDROID_HOME = $Candidate }
}
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
    throw 'Android SDK is not configured. Install Android Studio and SDK Platform 35 first.'
}
if (-not (Test-Path $GradleExe)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $GradleDir -Parent) | Out-Null
    $Zip = Join-Path $Root '.tooling\gradle-8.9-bin.zip'
    Write-Host 'Fetching official Gradle 8.9 distribution (first build only)...'
    Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $Zip
    Expand-Archive -LiteralPath $Zip -DestinationPath (Split-Path $GradleDir -Parent) -Force
    Remove-Item $Zip
}
Push-Location $Root
try {
    & $GradleExe ':app:assembleDebug' '--no-daemon'
    if ($LASTEXITCODE -ne 0) { throw "Gradle exited with code $LASTEXITCODE" }
    Write-Host "Built $Root\app\build\outputs\apk\debug\app-debug.apk"
} finally { Pop-Location }
