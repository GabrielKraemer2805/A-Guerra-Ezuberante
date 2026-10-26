param([switch]$AcceptEula)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$serverProfile=Join-Path $projectRoot '.tools/run-arsenal-server'
$shooterProfile=Join-Path $projectRoot '.tools/run-arsenal-client'
$observerProfile=Join-Path $projectRoot '.tools/run-arsenal-observer'
foreach($profile in @($serverProfile,$shooterProfile,$observerProfile)){New-Item -ItemType Directory -Force -Path $profile|Out-Null}
$eula=Join-Path $serverProfile 'eula.txt'
if($AcceptEula){[IO.File]::WriteAllText($eula,"# Explicit author acceptance supplied for local testing.`neula=true`n")}
if(-not(Test-Path -LiteralPath $eula) -or (Get-Content -LiteralPath $eula -Raw) -notmatch '(?m)^eula=true\s*$'){throw 'Minecraft EULA requires explicit acceptance before this dedicated-server test.'}
$world='arsenal-test-'+(Get-Date -Format 'yyyyMMdd-HHmmss')
$properties="server-ip=127.0.0.1`nserver-port=25581`nonline-mode=false`nwhite-list=true`nenforce-whitelist=true`nenforce-secure-profile=false`nlevel-name=$world`nspawn-protection=0`nview-distance=4`nsimulation-distance=5`ngamemode=survival`ndifficulty=normal`nmax-players=2`nenable-rcon=false`nenable-query=false`ngenerate-structures=false`nlevel-type=minecraft\:flat`n"
[IO.File]::WriteAllText((Join-Path $serverProfile 'server.properties'),$properties)
$profiles=@('Shooter','Observer')|ForEach-Object {
    $md5=[Security.Cryptography.MD5]::Create();$hash=$md5.ComputeHash([Text.Encoding]::UTF8.GetBytes('OfflinePlayer:'+$_));$md5.Dispose()
    $hash[6]=($hash[6]-band 15)-bor 48;$hash[8]=($hash[8]-band 63)-bor 128
    $hex=([BitConverter]::ToString($hash)).Replace('-','').ToLowerInvariant()
    [pscustomobject]@{uuid=$hex.Substring(0,8)+'-'+$hex.Substring(8,4)+'-'+$hex.Substring(12,4)+'-'+$hex.Substring(16,4)+'-'+$hex.Substring(20);name=$_}
}
[IO.File]::WriteAllText((Join-Path $serverProfile 'whitelist.json'),($profiles|ConvertTo-Json))
foreach($entry in @(@($serverProfile,'arsenal-server-ready.txt'),@($serverProfile,'arsenal-server-result.json'),@($shooterProfile,'arsenal-client-result.json'),@($observerProfile,'arsenal-observer-result.json'))) {
    $file=Join-Path $entry[0] $entry[1];if(Test-Path -LiteralPath $file){Remove-Item -LiteralPath $file}
}
function Start-TestGradle([string]$name,[string]$role) {
    $stdout=Join-Path $projectRoot ('.tools/'+$name+'.log');$stderr=Join-Path $projectRoot ('.tools/'+$name+'-stderr.log')
    # Known fixed arguments only; process launches hidden, Minecraft creates its own game window.
    $process=Start-Process -FilePath powershell -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File','tools/arsenal_run.ps1','-Role',$role) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    return $process
}
$server=Start-TestGradle 'arsenal-dedicated' 'Server'
$ready=Join-Path $serverProfile 'arsenal-server-ready.txt'
$deadline=(Get-Date).AddSeconds(90)
while(-not(Test-Path -LiteralPath $ready)) {
    if($server.HasExited -or (Get-Date)-gt $deadline){throw 'Dedicated server did not become ready; inspect .tools/arsenal-dedicated.log.'}
    Start-Sleep -Milliseconds 500
}
$observer=Start-TestGradle 'arsenal-network-observer' 'Observer'
$observerLog=Join-Path $projectRoot '.tools/arsenal-network-observer.log'
$deadline=(Get-Date).AddSeconds(80)
while(-not((Test-Path -LiteralPath $observerLog) -and (Select-String -LiteralPath $observerLog -SimpleMatch 'Loaded 2 advancements' -Quiet))) {
    if($observer.HasExited -or (Get-Date)-gt $deadline){throw 'Observer did not finish login; inspect its log before starting Shooter.'}
    Start-Sleep -Milliseconds 500
}
# Avoid simultaneous cold client startup/registry handshakes. The observer is already in the world.
$shooter=Start-TestGradle 'arsenal-network-shooter' 'Shooter'
$deadline=(Get-Date).AddMinutes(5)
while(-not($server.HasExited -and $observer.HasExited -and $shooter.HasExited)) {
    if((Get-Date)-gt $deadline){throw 'Network test timed out; inspect logs. The isolated fixtures have their own shutdown timeout.'}
    Start-Sleep -Milliseconds 500
}
foreach($entry in @(@($serverProfile,'arsenal-server-result.json'),@($shooterProfile,'arsenal-client-result.json'),@($observerProfile,'arsenal-observer-result.json'))) {
    $file=Join-Path $entry[0] $entry[1]
    if(-not(Test-Path -LiteralPath $file)){throw "Network scenario did not produce $file"}
    $result=Get-Content -LiteralPath $file -Raw|ConvertFrom-Json
    if(-not $result.passed){throw "Network scenario failed: $file -- $($result.errors -join '; ')"}
}
Write-Host 'PASS: dedicated server, two real clients, native TaCZ firing/reload and server ammo conservation.'
