param([string]$WorldName=('Menu-'+(Get-Date -Format 'yyyyMMdd-HHmmss')))
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $projectRoot '.tools/run-title-client'
$destination=Join-Path $profile ('saves/'+$WorldName)
if(Test-Path -LiteralPath $destination){throw 'Use a fresh isolated test world.'}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-gametest/world') -Destination $destination -Recurse
$optionsPath=Join-Path $profile 'options.txt'
if(-not(Test-Path -LiteralPath $optionsPath)) {
    Copy-Item -LiteralPath (Join-Path $projectRoot 'run/options.txt') -Destination $optionsPath
}
$resultPath=Join-Path $profile 'title-result.json'
if(Test-Path -LiteralPath $resultPath){Remove-Item -LiteralPath $resultPath}
$env:JAVA_HOME=(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.gradle-user'
Push-Location $projectRoot
try {
    # No quickPlayWorld: test the real title screen and its vanilla navigation first.
    & .\gradlew.bat runClient '-PclientWorkDir=.tools/run-title-client' '-PtitleTest' --console=plain --max-workers=4
    if($LASTEXITCODE -ne 0){throw 'Title screen test client failed.'}
    if(-not(Test-Path -LiteralPath $resultPath)){throw 'No completed title-screen report.'}
    $result=Get-Content -LiteralPath $resultPath -Raw|ConvertFrom-Json
    if(-not $result.passed){throw "Title-screen scenario failed: $($result.errors -join '; ')"}
}finally{Pop-Location}
