param([string]$Downloads = (Join-Path $env:USERPROFILE 'Downloads'))
# 0.020: imports the Bolsonarista villager skins, Enéias, Augusto Cury and the Lulista tower guard (traficante de armas).
# Originals are kept in assets-entrada/skins; skins with transparent pixels on visible base faces get a "_corrigida"
# copy where each empty pixel takes the colour of the nearest opaque pixel of the same face (the face's own colours).
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Add-Type -AssemblyName System.Drawing
$inputDir = Join-Path $root 'assets-entrada\skins'
$textures = Join-Path $root 'src\main\resources\assets\guerra_ezuberante\textures\entity'
$skins = @(
    @{ File = 'macaco-bolsonaro-mito-22.png'; Source = 'bolsonaristas\bolsonarista_0.png'; Texture = 'bolsonarista\bolsonarista_0.png' },
    @{ File = 'bolsonaro-ou-no.png'; Source = 'bolsonaristas\bolsonarista_1.png'; Texture = 'bolsonarista\bolsonarista_1.png' },
    @{ File = 'bolsonaro.png'; Source = 'bolsonaristas\bolsonarista_2.png'; Texture = 'bolsonarista\bolsonarista_2.png' },
    @{ File = 'bolsonaro (1).png'; Source = 'bolsonaristas\bolsonarista_3.png'; Texture = 'bolsonarista\bolsonarista_3.png' },
    @{ File = 'eleitor-do-bolsonaro.png'; Source = 'bolsonaristas\bolsonarista_4.png'; Texture = 'bolsonarista\bolsonarista_4.png' },
    @{ File = 'eneias-negresco.png'; Source = 'eneias_skin_64x64.png'; Texture = 'eneias.png' },
    @{ File = 'augusto-cury.png'; Source = 'augusto_cury_skin_64x64.png'; Texture = 'augusto_cury.png' },
    @{ File = 'traficante-de-armas.png'; Source = 'traficante_de_armas_skin_64x64.png'; Texture = 'traficante_de_armas.png' }
)
# Base layer boxes: u, v, width, depth, height (head, body, right arm, left arm, right leg, left leg).
$parts = @(@(0,0,8,8,8), @(16,16,8,4,12), @(40,16,4,4,12), @(32,48,4,4,12), @(0,16,4,4,12), @(16,48,4,4,12))

function Faces($p) {
    $u = $p[0]; $v = $p[1]; $w = $p[2]; $d = $p[3]; $h = $p[4]
    return @(@(($u + $d), $v, $w, $d), @(($u + $d + $w), $v, $w, $d), @($u, ($v + $d), $d, $h),
             @(($u + $d), ($v + $d), $w, $h), @(($u + $d + $w), ($v + $d), $d, $h), @(($u + 2 * $d + $w), ($v + $d), $w, $h))
}

foreach ($skin in $skins) {
    $original = Join-Path $Downloads $skin.File
    if (-not (Test-Path -LiteralPath $original)) { throw "Missing skin: $original" }
    $source = Join-Path $inputDir $skin.Source
    New-Item -ItemType Directory -Force (Split-Path -Parent $source) | Out-Null
    Copy-Item -LiteralPath $original -Destination $source -Force
    $bitmap = New-Object Drawing.Bitmap $source
    $fixed = New-Object Drawing.Bitmap 64, 64, ([Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($x = 0; $x -lt 64; $x++) { for ($y = 0; $y -lt 64; $y++) { $fixed.SetPixel($x, $y, $bitmap.GetPixel($x, $y)) } }
    $bitmap.Dispose()
    $changed = 0
    foreach ($part in $parts) {
        foreach ($face in (Faces $part)) {
            $opaque = @()
            for ($x = $face[0]; $x -lt $face[0] + $face[2]; $x++) { for ($y = $face[1]; $y -lt $face[1] + $face[3]; $y++) {
                if ($fixed.GetPixel($x, $y).A -eq 255) { $opaque += , @($x, $y) } } }
            if ($opaque.Count -eq 0) { continue }
            for ($x = $face[0]; $x -lt $face[0] + $face[2]; $x++) { for ($y = $face[1]; $y -lt $face[1] + $face[3]; $y++) {
                if ($fixed.GetPixel($x, $y).A -eq 255) { continue }
                $best = $opaque | Sort-Object { [Math]::Abs($_[0] - $x) + [Math]::Abs($_[1] - $y) } | Select-Object -First 1
                $fixed.SetPixel($x, $y, $fixed.GetPixel($best[0], $best[1])); $changed++ } }
        }
    }
    $target = Join-Path $textures $skin.Texture
    New-Item -ItemType Directory -Force (Split-Path -Parent $target) | Out-Null
    if ($changed -gt 0) {
        $corrected = $source -replace '\.png$', '_corrigida.png'
        $fixed.Save($corrected, [Drawing.Imaging.ImageFormat]::Png)
        Copy-Item -LiteralPath $corrected -Destination $target -Force
        Write-Host "$($skin.Texture): $changed base pixels filled"
    } else {
        Copy-Item -LiteralPath $source -Destination $target -Force
        Write-Host "$($skin.Texture): complete"
    }
    $fixed.Dispose()
}
# The Bolsonarista villager type reuses the petista biome-type overlay, like the comunista one.
foreach ($folder in 'villager\type', 'zombie_villager\type') {
    Copy-Item -LiteralPath (Join-Path $textures "$folder\petista.png") -Destination (Join-Path $textures "$folder\bolsonarista.png") -Force
}
