$ErrorActionPreference = 'Stop'
# Pixel art for Dilma's content: the mandioca thrown in bursts (16x16 item) and the "Sem ar" effect icon (18x18).
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
$textures = Join-Path $projectRoot 'src\main\resources\assets\guerra_ezuberante\textures'

function New-Canvas([int]$size) {
    $bitmap = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($x = 0; $x -lt $size; $x++) { for ($y = 0; $y -lt $size; $y++) { $bitmap.SetPixel($x, $y, [System.Drawing.Color]::Transparent) } }
    return $bitmap
}
function Color([string]$hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }

# Mandioca: a tapered root lying on the diagonal, dark bark with a lighter ridge, cream cut end and a short stem.
$root = New-Canvas 16
$bark = Color '#5A3A1E'; $ridge = Color '#8A5A30'; $shadow = Color '#3E2814'; $flesh = Color '#F2E6C8'; $stem = Color '#6E8F3A'
# Each pixel is painted by its distance to the root's axis, from the thick cut end (bottom-left) to the thin tip.
$ax = 3.0; $ay = 12.5; $bx = 13.5; $by = 2.0
$lx = $bx - $ax; $ly = $by - $ay; $len2 = $lx * $lx + $ly * $ly
for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) {
    $px = $x + .5; $py = $y + .5
    $t = [Math]::Max(0.0, [Math]::Min(1.0, (($px - $ax) * $lx + ($py - $ay) * $ly) / $len2))
    $dx = $px - ($ax + $t * $lx); $dy = $py - ($ay + $t * $ly)
    $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)
    $radius = if ($t -lt .55) { 2.3 } else { 2.3 - ($t - .55) / .45 * 1.8 }
    if ($dist -gt $radius) { continue }
    # Positive side faces the top-left light: lighter ridge; the far edge gets the shadow.
    $side = $lx * $dy - $ly * $dx
    $color = if ($t -le .06) { $flesh } elseif ($dist -gt $radius - .9 -and $side -gt 0) { $shadow } elseif ($side -lt -.6) { $ridge } else { $bark }
    $root.SetPixel($x, $y, $color)
} }
$root.SetPixel(1, 14, $stem); $root.SetPixel(1, 15, $stem)
$root.Save((Join-Path $textures 'item\mandioca.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$root.Dispose()

# Sem ar: three air bubbles, the last one popped, on a transparent 18x18 icon like the vanilla effect icons.
$icon = New-Canvas 18
$rim = Color '#2F7FD8'; $fill = Color '#A9D8FF'; $shine = Color '#FFFFFF'; $pop = Color '#D8443A'
function Bubble($bitmap, [int]$cx, [int]$cy, [int]$r) {
    for ($x = $cx - $r; $x -le $cx + $r; $x++) { for ($y = $cy - $r; $y -le $cy + $r; $y++) {
        $d = [Math]::Sqrt(($x - $cx) * ($x - $cx) + ($y - $cy) * ($y - $cy))
        if ($d -le $r + .3) { $bitmap.SetPixel($x, $y, $(if ($d -ge $r - .7) { $rim } else { $fill })) }
    } }
    $bitmap.SetPixel($cx - [int]($r / 2), $cy - [int]($r / 2), $shine)
}
Bubble $icon 5 11 4
Bubble $icon 12 6 3
foreach ($p in @(@(12,13),@(14,15),@(16,13),@(14,11),@(13,14),@(15,14),@(15,12),@(13,12))) { $icon.SetPixel($p[0], $p[1], $pop) }
$icon.Save((Join-Path $textures 'mob_effect\sem_ar.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$icon.Dispose()
Write-Host 'Generated mandioca.png (16x16) and sem_ar.png (18x18).'
