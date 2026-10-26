param([Parameter(Mandatory=$true)][ValidateSet('Server','Shooter','Observer')][string]$Role)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME=(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.gradle-user'
if($Role -eq 'Server') {
    $launchArgs=@('runServer','-PserverWorkDir=.tools/run-arsenal-server','-ParsenalTest')
} else {
    $profile=if($Role -eq 'Observer'){'.tools/run-arsenal-observer'}else{'.tools/run-arsenal-client'}
    $launchArgs=@('runClient',('-PclientWorkDir='+$profile),'-PquickPlayServer=127.0.0.1:25581',('-PdevUsername='+$Role),'-ParsenalTest')
    if($Role -eq 'Observer'){$launchArgs+='-ParsenalObserver'}
}
Push-Location $projectRoot
try {
    & .\gradlew.bat @launchArgs --console=plain --max-workers=4
    $result=$LASTEXITCODE
} finally {Pop-Location}
exit $result
