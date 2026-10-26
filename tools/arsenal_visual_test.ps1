param([string]$WorldName=('Arsenal-'+(Get-Date -Format 'yyyyMMdd-HHmmss')))
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $projectRoot '.tools/run-arsenal-client'
$destination=Join-Path $profile ('saves/'+$WorldName)
if(Test-Path -LiteralPath $destination){throw 'Use a fresh isolated test world.'}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-gametest/world') -Destination $destination -Recurse
foreach($name in @('arsenal-client-result.json','arsenal-server-result.json')) {
    $file=Join-Path $profile $name
    if(Test-Path -LiteralPath $file){Remove-Item -LiteralPath $file}
}
$env:JAVA_HOME=(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.gradle-user'
Push-Location $projectRoot
try {
    & .\gradlew.bat runClient '-PclientWorkDir=.tools/run-arsenal-client' "-PquickPlayWorld=$WorldName" '-ParsenalTest' --console=plain --max-workers=4
    if($LASTEXITCODE -ne 0){throw 'Arsenal test client failed.'}
    foreach($name in @('arsenal-client-result.json','arsenal-server-result.json')) {
        $file=Join-Path $profile $name
        if(-not(Test-Path -LiteralPath $file)){throw "Missing real test result: $name"}
        $result=Get-Content -LiteralPath $file -Raw|ConvertFrom-Json
        if(-not $result.passed){throw "Scenario failed: $name -- $($result.errors -join '; ')"}
    }
} finally {Pop-Location}
