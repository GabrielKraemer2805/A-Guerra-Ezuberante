param([string]$WorldName=('Pistol-'+(Get-Date -Format 'yyyyMMdd-HHmmss')), [switch]$WithTacz)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $projectRoot '.tools/run-pistol-client'
$destination=Join-Path $profile ('saves/'+$WorldName)
if(Test-Path -LiteralPath $destination){throw 'Use a fresh isolated test world.'}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-gametest/world') -Destination $destination -Recurse
$resultPath=Join-Path $profile 'pistol-client-result.json'
if(Test-Path -LiteralPath $resultPath){Remove-Item -LiteralPath $resultPath}
$env:JAVA_HOME=(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.gradle-user'
Push-Location $projectRoot
try {
    $runArguments=@('runClient','-PclientWorkDir=.tools/run-pistol-client',"-PquickPlayWorld=$WorldName",'-PpistolTest','--console=plain','--max-workers=4')
    if($WithTacz){$runArguments+='-PpistolWithTacz'}else{$runArguments+='-PwithoutTacz'}
    & .\gradlew.bat @runArguments
    if($LASTEXITCODE -ne 0){throw 'Original pistol test client failed.'}
    if(-not(Test-Path -LiteralPath $resultPath)){throw 'No completed pistol scenario report.'}
    $result=Get-Content -LiteralPath $resultPath -Raw | ConvertFrom-Json
    if(-not $result.passed){throw "Original pistol scenario failed: $($result.errors -join '; ')"}
} finally {Pop-Location}
