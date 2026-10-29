param([switch]$Observer)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $root $(if($Observer){'.tools/run-faction-observer'}else{'.tools/run-faction-host'})
New-Item -ItemType Directory -Force $profile|Out-Null
$options=Join-Path $profile 'options.txt'
$text=if(Test-Path $options){Get-Content $options -Raw}else{''}
$text=$text -replace '(?m)^(renderDistance|simulationDistance|maxFps|enableVsync|soundCategory_master):[^\r\n]*\r?\n?',''
[IO.File]::WriteAllText($options,$text.TrimEnd()+"`nrenderDistance:5`nsimulationDistance:5`nmaxFps:45`nenableVsync:false`nsoundCategory_master:0.0`n",(New-Object Text.UTF8Encoding($false)))
$sessionPointer=Join-Path $root '.tools/faction-test-session.txt'
$extra=@('-PfactionTest')
if($Observer){
 $session=(Get-Content $sessionPointer -Raw).Trim()
 if(-not(Test-Path (Join-Path $session 'ready.txt'))){throw 'Host listener not ready.'}
 $extra+=@('-PfactionObserver','-PquickPlayServer=127.0.0.1:25586','-PdevUsername=Observer')
}else{
 $stamp=Get-Date -Format 'yyyyMMdd-HHmmss'
 $session=Join-Path $root ".tools/faction-session-$stamp"
 New-Item -ItemType Directory -Force $session|Out-Null
 [IO.File]::WriteAllText($sessionPointer,$session)
 $world="Faction-$stamp";$destination=Join-Path $profile "saves/$world"
 New-Item -ItemType Directory -Force (Split-Path -Parent $destination)|Out-Null
 Copy-Item -LiteralPath (Join-Path $root 'run-gametest/world') -Destination $destination -Recurse
 $extra+="-PquickPlayWorld=$world"
}
$extra+="-PfactionTestSession=$session"
$env:JAVA_HOME=(Get-ChildItem (Join-Path $root '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $root '.gradle-user'
Push-Location $root
try{
 & .\gradlew.bat runClient "-PclientWorkDir=$profile" @extra --console=plain --max-workers=4
 if($LASTEXITCODE -ne 0){throw 'Faction client failed.'}
 $result=Join-Path $session $(if($Observer){'observer.json'}else{'host.json'})
 if(-not(Test-Path $result)){throw 'Missing faction report.'}
 $report=Get-Content $result -Raw|ConvertFrom-Json
 if(-not $report.passed){throw $report.error}
 if(-not $Observer){
  $remote=Get-Content (Join-Path $session 'observer.json') -Raw|ConvertFrom-Json
  $server=Get-Content (Join-Path $session 'server.json') -Raw|ConvertFrom-Json
  if(-not($remote.passed -and $server.passed)){throw 'Incomplete multiplayer reports.'}
  $expected=@{beanIds=3;meteorIds=[int]$server.meteors;bookIds=[int]$server.books;protesterIds=10}
  foreach($key in $expected.Keys){
   $a=@($report.$key|Sort-Object);$b=@($remote.$key|Sort-Object)
   if($a.Count -ne $expected[$key] -or (Compare-Object $a $b)){throw "Missing or different client entities: $key"}
  }
 }
}finally{Pop-Location}
