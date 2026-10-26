$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $projectRoot '.tools/run-renan-observer'
New-Item -ItemType Directory -Force -Path $profile | Out-Null
$resultPath=Join-Path $profile 'renan-observer-result.json'
if(Test-Path -LiteralPath $resultPath){Remove-Item -LiteralPath $resultPath}
$readyPath=Join-Path $projectRoot '.tools/run-renan-client/renan-ready.txt'
$deadline=(Get-Date).AddSeconds(90)
while(-not(Test-Path -LiteralPath $readyPath)) {if((Get-Date)-gt $deadline){throw 'Renan host not ready'};Start-Sleep -Milliseconds 500}
$env:JAVA_HOME=(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.gradle-user'
Push-Location $projectRoot
try {
    & .\gradlew.bat runClient '-PclientWorkDir=.tools/run-renan-observer' '-PquickPlayServer=127.0.0.1:25582' '-PdevUsername=Observer' '-PrenanTest' '-PrenanObserver' --console=plain --max-workers=4
    if($LASTEXITCODE -ne 0){throw 'Observer launch failed.'}
    if(-not(Test-Path -LiteralPath $resultPath)){throw 'Observer did not complete.'}
    $result=Get-Content -LiteralPath $resultPath -Raw|ConvertFrom-Json
    if(-not $result.passed){throw "Observer failed: $($result.errors -join '; ')"}
}finally{Pop-Location}
