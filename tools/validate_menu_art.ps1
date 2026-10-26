$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$path=Join-Path $projectRoot 'src/main/resources/assets/guerra_ezuberante/textures/gui/title/guerra_ezuberante_menu_bg.png'
$bytes=[IO.File]::ReadAllBytes($path)
if($bytes.Length -lt 26 -or [BitConverter]::ToString($bytes,0,8) -ne '89-50-4E-47-0D-0A-1A-0A'){throw 'Invalid menu PNG.'}
$widthBytes=[byte[]]$bytes[16..19];$heightBytes=[byte[]]$bytes[20..23]
[Array]::Reverse($widthBytes);[Array]::Reverse($heightBytes)
if([BitConverter]::ToInt32($widthBytes,0) -ne 1920 -or [BitConverter]::ToInt32($heightBytes,0) -ne 1080){throw 'Menu artwork must be 1920x1080.'}
if($bytes[25] -ne 2){throw 'Expected opaque RGB PNG for the title background.'}
Write-Host 'PASS: original generated menu artwork, opaque RGB PNG, 1920x1080.'
