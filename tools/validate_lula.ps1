$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$assets=Join-Path $root 'src/main/resources/assets/guerra_ezuberante'
function Require($condition,$message){if(-not $condition){throw $message}}
$skin=Join-Path $assets 'textures/entity/lula_skin_64x64.png'
$source=Join-Path $root 'assets-entrada/skins/lula_skin_64x64.png'
Require ((Get-FileHash $skin).Hash -eq (Get-FileHash $source).Hash) 'Lula original skin was altered.'
$bytes=[IO.File]::ReadAllBytes($skin)
Require ([BitConverter]::ToString($bytes,16,8) -eq '00-00-00-40-00-00-00-40') 'Lula skin must be 64x64.'
$geo=Get-Content (Join-Path $assets 'geo/lula.geo.json') -Raw -Encoding UTF8|ConvertFrom-Json
$model=$geo.'minecraft:geometry'[0]
$belly=$model.bones|Where-Object name -eq 'belly'
Require ($belly.parent -eq 'torso' -and $belly.cubes.Count -eq 3 -and $belly.cubes[0].origin[2] -lt -2) 'Missing protruding belly geometry.'
foreach($bone in $model.bones){foreach($cube in $bone.cubes){foreach($face in $cube.uv.PSObject.Properties){for($i=0;$i -lt 2;$i++){
 $start=$face.Value.uv[$i];$end=$start+$face.Value.uv_size[$i]
 Require ($start -ge 0 -and $start -le 64 -and $end -ge 0 -and $end -le 64) "UV outside skin: $($bone.name)"
}}}}
$animation=Get-Content (Join-Path $assets 'animations/lula.animation.json') -Raw -Encoding UTF8|ConvertFrom-Json
foreach($name in @('idle','walk','attack_bean','faz_o_l','hurt','death')){Require ($null -ne $animation.animations.PSObject.Properties["animation.guerra.lula_$name"]) "Missing clip: $name"}
Require ($animation.animations.'animation.guerra.lula_death'.animation_length -eq 3) 'Death animation must last 3 s.'
$bean=[IO.File]::ReadAllBytes((Join-Path $assets 'textures/item/semente_feijao.png'))
Require ([BitConverter]::ToString($bean,16,8) -eq '00-00-00-10-00-00-00-10') 'Bean sprite must be 16x16.'
Write-Host 'OK: Lula original skin, belly, UV, six animations, three-second death and bean sprite.'
