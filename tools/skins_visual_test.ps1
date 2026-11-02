# 0.022.1: captures the refactored skins on the real model, in an isolated profile with master volume 0.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$profile = Join-Path $root '.tools/run-skins-client'
New-Item -ItemType Directory -Force $profile | Out-Null
$options = Join-Path $profile 'options.txt'
$text = if (Test-Path $options) { Get-Content $options -Raw } else { '' }
$text = $text -replace '(?m)^(renderDistance|simulationDistance|maxFps|enableVsync|soundCategory_master):[^\r\n]*\r?\n?', ''
[IO.File]::WriteAllText($options, $text.TrimEnd() + "`nrenderDistance:5`nsimulationDistance:5`nmaxFps:60`nenableVsync:false`nsoundCategory_master:0.0`n", (New-Object Text.UTF8Encoding($false)))
$result = Join-Path $profile 'skins-result.json'
if (Test-Path $result) { Remove-Item -LiteralPath $result }
$world = 'Skins-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
$destination = Join-Path $profile "saves/$world"
New-Item -ItemType Directory -Force (Split-Path -Parent $destination) | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'run-gametest/world') -Destination $destination -Recurse
& (Join-Path $PSScriptRoot 'gradle.ps1') runClient "-PclientWorkDir=$profile" '-PskinsTest' "-PquickPlayWorld=$world" --console=plain --max-workers=4
if (-not (Test-Path $result)) { throw 'Missing graphical report.' }
$report = Get-Content $result -Raw | ConvertFrom-Json
if (-not $report.passed) { throw ($report.errors -join '; ') }
Write-Output 'Skins visual test passed.'
