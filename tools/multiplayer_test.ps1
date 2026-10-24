$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$profile = Join-Path $projectRoot '.tools/run-summon-observer'
New-Item -ItemType Directory -Force -Path $profile | Out-Null
$completeFile = Join-Path $profile 'observer-complete.txt'
if (Test-Path -LiteralPath $completeFile) { Remove-Item -LiteralPath $completeFile }
# Run tools/visual_test.ps1 -Multiplayer in another terminal first.
$ready = Join-Path $projectRoot '.tools/run-summon-client/multiplayer-ready.txt'
$deadline = (Get-Date).AddSeconds(55)
while (-not (Test-Path -LiteralPath $ready)) {
    if ((Get-Date) -gt $deadline) { throw 'Host was not ready within 55 seconds.' }
    Start-Sleep -Milliseconds 500
}
$env:JAVA_HOME = (Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory | Select-Object -First 1).FullName
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user'
Push-Location $projectRoot
try {
    & .\gradlew.bat runClient '-PclientWorkDir=.tools/run-summon-observer' '-PquickPlayServer=127.0.0.1:25579' '-PobserverTest' --console=plain --max-workers=4
    if ($LASTEXITCODE -ne 0) { throw 'Second Minecraft client failed; inspect the log.' }
    if (-not (Test-Path -LiteralPath $completeFile)) { throw 'Observer did not complete the network scenario; validation remains pending.' }
} finally { Pop-Location }
