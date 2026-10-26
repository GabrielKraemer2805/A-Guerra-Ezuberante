param([string]$WorldName=('Renan-'+(Get-Date -Format 'yyyyMMdd-HHmmss')), [switch]$Multiplayer)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $projectRoot '.tools/run-renan-client'
$destination=Join-Path $profile ('saves/'+$WorldName)
if(Test-Path -LiteralPath $destination){throw 'Use a fresh isolated test world.'}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-gametest/world') -Destination $destination -Recurse
$resultPath=Join-Path $profile 'renan-result.json'
if(Test-Path -LiteralPath $resultPath){Remove-Item -LiteralPath $resultPath}
if($Multiplayer) {
    $readyPath=Join-Path $profile 'renan-ready.txt';if(Test-Path -LiteralPath $readyPath){Remove-Item -LiteralPath $readyPath}
}
$env:JAVA_HOME=(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.gradle-user'
Push-Location $projectRoot
try {
    $extra=@();if($Multiplayer){$extra+='-PrenanNetwork'}
    & .\gradlew.bat runClient '-PclientWorkDir=.tools/run-renan-client' "-PquickPlayWorld=$WorldName" '-PrenanTest' @extra --console=plain --max-workers=4
    if($LASTEXITCODE -ne 0){throw 'Renan test client failed.'}
    if(-not(Test-Path -LiteralPath $resultPath)){throw 'No completed Renan graphical report.'}
    $result=Get-Content -LiteralPath $resultPath -Raw | ConvertFrom-Json
    if(-not $result.passed){throw "Renan scenario failed: $($result.errors -join '; ')"}
}finally{Pop-Location}
