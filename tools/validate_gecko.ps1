$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $projectRoot 'src/main/resources/assets/guerra_ezuberante'
function Require($condition, $message) { if (-not $condition) { throw $message } }
$geo = Get-Content -LiteralPath (Join-Path $assets 'geo/humanoid.geo.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$anim = Get-Content -LiteralPath (Join-Path $assets 'animations/humanoid.animation.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$model = $geo.'minecraft:geometry'[0]
Require ($model.description.texture_width -eq 64 -and $model.description.texture_height -eq 64) 'Original skin atlas must remain 64x64.'
$names = @($model.bones | ForEach-Object { $_.name })
Require (($names | Select-Object -Unique).Count -eq $names.Count) 'Duplicate bones.'
foreach ($bone in $model.bones) {
    if ($bone.parent) { Require ($names -contains $bone.parent) "Missing parent: $($bone.name)" }
    foreach ($cube in $bone.cubes) {
        foreach ($face in $cube.uv.PSObject.Properties) {
            $uv = $face.Value
            for ($axis = 0; $axis -lt 2; $axis++) {
                $start = $uv.uv[$axis]; $end = $start + $uv.uv_size[$axis]
                Require ($start -ge 0 -and $start -le 64 -and $end -ge 0 -and $end -le 64) "UV outside skin: $($bone.name).$($face.Name)"
            }
        }
    }
}
# Assert the external side faces are the original 12px limb, partitioned into two 6px strips.
foreach ($pair in @(@('right_arm','right_forearm'), @('left_arm','left_forearm'), @('right_leg','right_shin'), @('left_leg','left_shin'))) {
    $upper = $model.bones | Where-Object name -eq $pair[0]
    $lower = $model.bones | Where-Object name -eq $pair[1]
    Require ($lower.parent -eq $upper.name) 'Joint hierarchy is disconnected.'
    for ($layer=0;$layer -lt 2;$layer++) {
        foreach ($face in @('north','south','east','west')) {
            $a=$upper.cubes[$layer].uv.$face; $b=$lower.cubes[$layer].uv.$face
            Require ($a.uv[0] -eq $b.uv[0] -and $a.uv[1]+6 -eq $b.uv[1]) "Discontinuous skin UV: $($pair[0]) $face"
            Require ($a.uv_size[1] -eq 6 -and $b.uv_size[1] -eq 6 -and $a.uv_size[0] -eq 4 -and $b.uv_size[0] -eq 4) 'Limb texture stretched.'
        }
    }
}
foreach ($clip in $anim.animations.PSObject.Properties) {
    $duration = $clip.Value.animation_length
    Require ($duration -gt 0) "Invalid duration: $($clip.Name)"
    foreach ($bone in $clip.Value.bones.PSObject.Properties) {
        Require ($names -contains $bone.Name) "Animation uses missing bone: $($bone.Name)"
        foreach ($track in $bone.Value.PSObject.Properties) {
            foreach ($frame in $track.Value.PSObject.Properties) {
                $time = [double]::Parse($frame.Name, [Globalization.CultureInfo]::InvariantCulture)
                Require ($time -ge 0 -and $time -le $duration + 0.00001) "Keyframe outside clip: $($clip.Name) $time"
                Require ($frame.Value.Count -eq 3) 'Invalid keyframe vector.'
                foreach ($v in $frame.Value) { Require (-not [double]::IsNaN($v) -and -not [double]::IsInfinity($v)) 'Non-finite keyframe.' }
            }
        }
    }
}
foreach ($clip in @('idle','walk','punch_right','punch_left','sword_attack','hurt','cast','ukrainian_emerge')) {
    Require ($null -ne $anim.animations.('animation.guerra.'+$clip)) "Missing animation: $clip"
}
foreach ($clip in @('rifle_idle','rifle_prepare','rifle_aim','rifle_walk','rifle_fire','rifle_hurt')) {
    Require ($null -ne $anim.animations.('animation.guerra.'+$clip)) "Missing rifle animation: $clip"
}
Require ($anim.animations.'animation.guerra.ukrainian_emerge'.animation_length -eq 2.2) 'Emergence must last 44 ticks.'
foreach ($clip in @('punch_right','punch_left','sword_attack')) {
    $side = if ($clip -eq 'punch_left') { 'left_arm' } else { 'right_arm' }
    Require ($null -ne $anim.animations.('animation.guerra.'+$clip).bones.$side.rotation.'0.25') 'Missing tick-5 impact keyframe.'
}
foreach ($texture in @('renan_santos','arthur_do_val','combatente_ucraniana')) {
    Require (Test-Path -LiteralPath (Join-Path $assets "textures/entity/$texture.png")) "Missing texture: $texture"
}
$props = Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') -Raw
$gradle = Get-Content -LiteralPath (Join-Path $projectRoot 'build.gradle') -Raw
$toml = Get-Content -LiteralPath (Join-Path $projectRoot 'src/main/resources/META-INF/mods.toml') -Raw
Require ($props -match 'minecraft_version=1.20.1' -and $props -match 'geckolib_version=4.8.4') 'Incompatible pinned platform.'
Require ($gradle.Contains('geckolib-forge-${minecraft_version}:${geckolib_version}') -and $toml.Contains('modId="geckolib"')) 'GeckoLib is not registered as a required dependency.'
Write-Host "Gecko validated: $($names.Count) bones, 14 clips, 64x64 UV subdivision, impact tick 5, dependency 4.8.4."
