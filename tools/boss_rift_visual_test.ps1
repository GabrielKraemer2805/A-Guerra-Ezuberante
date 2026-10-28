$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $root '.tools/run-boss-rift-client'
New-Item -ItemType Directory -Force $profile|Out-Null
# Write silence before launching any automated Minecraft process. Normal run/options.txt is untouched.
$options=Join-Path $profile 'options.txt'
$text=if(Test-Path $options){Get-Content $options -Raw}else{''}
$text=$text -replace '(?m)^(renderDistance|simulationDistance|maxFps|enableVsync|soundCategory_master):[^\r\n]*\r?\n?',''
[IO.File]::WriteAllText($options,$text.TrimEnd()+"`nrenderDistance:5`nsimulationDistance:5`nmaxFps:60`nenableVsync:false`nsoundCategory_master:0.0`n",(New-Object Text.UTF8Encoding($false)))
$result=Join-Path $profile 'boss-rift-result.json'
if(Test-Path $result){Remove-Item -LiteralPath $result}
$world='BossRift-'+(Get-Date -Format 'yyyyMMdd-HHmmss')
$destination=Join-Path $profile "saves/$world"
New-Item -ItemType Directory -Force (Split-Path -Parent $destination)|Out-Null
Copy-Item -LiteralPath (Join-Path $root 'run-gametest/world') -Destination $destination -Recurse
$env:JAVA_HOME=(Get-ChildItem (Join-Path $root '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $root '.gradle-user'
Push-Location $root
try{
 & .\gradlew.bat runClient "-PclientWorkDir=$profile" '-PbossRiftTest' "-PquickPlayWorld=$world" --console=plain --max-workers=4
 if($LASTEXITCODE -ne 0){throw 'Boss test client failed.'}
 if(-not(Test-Path $result)){throw 'Missing graphical report.'}
 $report=Get-Content $result -Raw|ConvertFrom-Json
 if(-not $report.passed){throw $report.error}
}finally{Pop-Location}
