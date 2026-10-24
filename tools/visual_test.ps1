param([string]$WorldName = ('Gecko4-' + (Get-Date -Format 'yyyyMMdd-HHmmss')), [switch]$Multiplayer)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $destination = Join-Path $projectRoot ('.tools/run-summon-client/saves/' + $WorldName)
    if (Test-Path -LiteralPath $destination) { throw 'Visual test destination already exists; use a fresh name.' }
    $source = Join-Path $projectRoot 'run-gametest/world'
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $source -Destination $destination -Recurse
    $env:JAVA_HOME = (Get-ChildItem -LiteralPath '.tools/jdk17' -Directory | Select-Object -First 1).FullName
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user'
    $extra = @()
    if ($Multiplayer) {
        $extra += '-PmultiplayerTest'
        $readyFile = Join-Path $projectRoot '.tools/run-summon-client/multiplayer-ready.txt'
        if (Test-Path -LiteralPath $readyFile) { Remove-Item -LiteralPath $readyFile }
        $completeFile = Join-Path $projectRoot '.tools/run-summon-client/multiplayer-complete.txt'
        if (Test-Path -LiteralPath $completeFile) { Remove-Item -LiteralPath $completeFile }
    }
    & .\gradlew.bat runClient '-PclientWorkDir=.tools/run-summon-client' "-PquickPlayWorld=$WorldName" '-PvisualTest' @extra --console=plain --max-workers=4
    if ($LASTEXITCODE -ne 0) { throw 'Visual client failed; inspect its log.' }
    if ($Multiplayer -and -not (Test-Path -LiteralPath $completeFile)) { throw 'Multiplayer scenario did not complete; do not report a passing network test.' }
} finally { Pop-Location }
