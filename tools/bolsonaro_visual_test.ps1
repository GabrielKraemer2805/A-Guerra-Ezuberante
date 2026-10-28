param([switch]$Observer,[switch]$Network)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $root $(if($Observer){'.tools/run-bolsonaro-observer'}else{'.tools/run-bolsonaro-client'})
New-Item -ItemType Directory -Force $profile|Out-Null
$options=Join-Path $profile 'options.txt'
$text=if(Test-Path $options){Get-Content $options -Raw}else{''}
$text=$text -replace '(?m)^(renderDistance|simulationDistance|maxFps|enableVsync|soundCategory_master):[^\r\n]*\r?\n?',''
[IO.File]::WriteAllText($options,$text.TrimEnd()+"`nrenderDistance:5`nsimulationDistance:5`nmaxFps:60`nenableVsync:false`nsoundCategory_master:0.0`n",(New-Object Text.UTF8Encoding($false)))
$result=Join-Path $profile 'bolsonaro-result.json';if(Test-Path $result){Remove-Item -LiteralPath $result}
$extra=@('-PbolsonaroTest')
if($Observer){$extra+=@('-PbolsonaroObserver','-PquickPlayServer=127.0.0.1:25584','-PdevUsername=Observer')}
else{
 $world='Bolsonaro-'+(Get-Date -Format 'yyyyMMdd-HHmmss');$destination=Join-Path $profile "saves/$world"
 New-Item -ItemType Directory -Force (Split-Path -Parent $destination)|Out-Null
 Copy-Item -LiteralPath (Join-Path $root 'run-gametest/world') -Destination $destination -Recurse
 $extra+="-PquickPlayWorld=$world"
 if($Network){$extra+='-PbolsonaroNetwork';$ready=Join-Path $profile 'bolsonaro-ready.txt';if(Test-Path $ready){Remove-Item -LiteralPath $ready}}
}
$env:JAVA_HOME=(Get-ChildItem (Join-Path $root '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$env:GRADLE_USER_HOME=Join-Path $root '.gradle-user'
Push-Location $root
try{
 & .\gradlew.bat runClient "-PclientWorkDir=$profile" @extra --console=plain --max-workers=4
 if($LASTEXITCODE -ne 0){throw 'Bolsonaro test client failed.'}
 if(-not(Test-Path $result)){throw 'Missing graphical report.'}
 $report=Get-Content $result -Raw|ConvertFrom-Json
 if(-not $report.passed){throw ($report.errors -join '; ')}
}finally{Pop-Location}
