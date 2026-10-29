param([string]$WorldPath='.tools/run-village-world/VillageSeedMinus42')
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$source=Join-Path $root $WorldPath
if(-not(Test-Path -LiteralPath (Join-Path $source 'level.dat'))){throw 'Prepare a closed, fresh normal world with villageWorldTest first.'}
$profile=Join-Path $root '.tools/run-character-audio'
New-Item -ItemType Directory -Force $profile|Out-Null
$world='AudioVillage-'+(Get-Date -Format 'yyyyMMdd-HHmmss')
$destination=Join-Path $profile "saves/$world"
New-Item -ItemType Directory -Force (Split-Path -Parent $destination)|Out-Null
Copy-Item -LiteralPath $source -Destination $destination -Recurse
# Set silence before launching: never depend on post-launch volume changes for automated tests.
[IO.File]::WriteAllText((Join-Path $profile 'options.txt'),"soundCategory_master:0.0`nsoundCategory_records:1.0`nlang:pt_br`nrenderDistance:4`nsimulationDistance:4`nmaxFps:45`nenableVsync:false`npauseOnLostFocus:false`n",(New-Object Text.UTF8Encoding($false)))
$result=Join-Path $profile 'audio-village-result.json'
if(Test-Path -LiteralPath $result){Remove-Item -LiteralPath $result}
Push-Location $root
try {
 & .\tools\gradle.ps1 runClient "-PclientWorkDir=$profile" "-PquickPlayWorld=$world" '-PcharacterAudioTest' --console=plain --max-workers=4
 if($LASTEXITCODE -ne 0){throw 'Audio/village client failed.'}
 if(-not(Test-Path -LiteralPath $result)){throw 'Missing audio/village report.'}
 $report=Get-Content -LiteralPath $result -Raw|ConvertFrom-Json
 if(-not $report.passed){throw $report.error}
 if($report.teleportCount -ne 18){throw 'Not all village teleports were exercised.'}
} finally {Pop-Location}
