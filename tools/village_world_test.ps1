param([long]$Seed=-42,[switch]$AcceptEula)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$profile=Join-Path $root '.tools/run-village-world'
New-Item -ItemType Directory -Force $profile|Out-Null
$eula=Join-Path $profile 'eula.txt'
if($AcceptEula){[IO.File]::WriteAllText($eula,"# Explicit local test-server acceptance.`neula=true`n")}
if(-not(Test-Path -LiteralPath $eula) -or (Get-Content -LiteralPath $eula -Raw) -notmatch '(?m)^eula=true\s*$'){
 throw 'Accept the Minecraft EULA before running this local server (then use -AcceptEula).'
}
$world='Village-'+$Seed+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss')
$properties=@('server-ip=127.0.0.1','server-port=25589','online-mode=false','white-list=true','enforce-whitelist=true','max-players=1',
 'view-distance=3','simulation-distance=3',"level-name=$world","level-seed=$Seed",'generate-structures=true','difficulty=peaceful',
 'gamemode=creative','enable-rcon=false','enable-query=false','spawn-protection=0')
[IO.File]::WriteAllLines((Join-Path $profile 'server.properties'),$properties)
$result=Join-Path $profile 'village-world-result.json'
if(Test-Path -LiteralPath $result){Remove-Item -LiteralPath $result}
Push-Location $root
try {
 & .\tools\gradle.ps1 runServer '-PserverWorkDir=.tools/run-village-world' '-PvillageWorldTest' --console=plain --max-workers=4
 if($LASTEXITCODE -ne 0){throw 'Village world server failed.'}
 $report=Get-Content -LiteralPath $result -Raw|ConvertFrom-Json
 if(-not $report.passed -or $report.villages.Count -ne 18){throw "Village generation failed: $($report.error)"}
 [IO.File]::WriteAllText((Join-Path $root '.tools/village-world-last-world.txt'),(Join-Path $profile $world))
} finally {Pop-Location}
