$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$assets=Join-Path $root 'src/main/resources/assets/guerra_ezuberante'
function Require($condition,$message){if(-not $condition){throw $message}}
foreach($character in @('jair_bolsonaro','flavio_bolsonaro')){
 $skin=Join-Path $assets "textures/entity/$character.png";$source=Join-Path $root "assets-entrada/skins/${character}_skin_64x64_corrigida.png"
 Require ((Get-FileHash $skin).Hash -eq (Get-FileHash $source).Hash) "Corrected source and runtime skin differ: $character"
 $bytes=[IO.File]::ReadAllBytes($skin);Require ([BitConverter]::ToString($bytes,16,8) -eq '00-00-00-40-00-00-00-40') "Skin must be 64x64: $character"
 $geo=Get-Content (Join-Path $assets "geo/$character.geo.json") -Raw -Encoding UTF8|ConvertFrom-Json
 $bones=$geo.'minecraft:geometry'[0].bones;$names=@($bones|ForEach-Object{$_.name});Require ($names.Count -eq 14) "Wrong bone count: $character"
 foreach($bone in $bones){if($bone.parent){Require ($names -contains $bone.parent) 'Disconnected skeleton'};foreach($cube in $bone.cubes){foreach($face in $cube.uv.PSObject.Properties){for($i=0;$i -lt 2;$i++){
  $start=$face.Value.uv[$i];$end=$start+$face.Value.uv_size[$i];Require ($start -ge 0 -and $start -le 64 -and $end -ge 0 -and $end -le 64) 'UV outside original skin'
 }}}}
 $animation=Get-Content (Join-Path $assets "animations/$character.animation.json") -Raw -Encoding UTF8|ConvertFrom-Json
 $expected=if($character -eq 'jair_bolsonaro'){@('jair_idle','jair_walk','jair_armed_idle','jair_aim','jair_shoot','jair_hurt','jair_summon_flavio','jair_death_special')}else{@('flavio_idle','flavio_walk','flavio_attack','flavio_hurt','flavio_rachadinha_prepare','flavio_rachadinha_ground_hit','flavio_rachadinha_recover')}
 foreach($clip in $expected){Require ($null -ne $animation.animations.PSObject.Properties["animation.guerra.$clip"]) "Missing clip: $clip"}
 foreach($clip in $animation.animations.PSObject.Properties){foreach($bone in $clip.Value.bones.PSObject.Properties){Require ($names -contains $bone.Name) "Unknown animated bone: $($bone.Name)"}}
}
Write-Host 'OK: 2 corrected 64x64 skins, distinct models, articulated UVs and all 15 clips.'
