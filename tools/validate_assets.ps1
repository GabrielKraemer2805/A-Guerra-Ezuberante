$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $projectRoot 'src\main\resources\assets\guerra_ezuberante'
$pt = Get-Content -LiteralPath (Join-Path $assets 'lang\pt_br.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$en = Get-Content -LiteralPath (Join-Path $assets 'lang\en_us.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$difference = Compare-Object ($pt.PSObject.Properties.Name | Sort-Object) ($en.PSObject.Properties.Name | Sort-Object)
if ($difference) { throw 'As traduções pt_br e en_us possuem chaves diferentes.' }

foreach ($skin in @(
    @{ Texture = 'renan_santos'; Source = 'renan_santos_skin_64x64.png' },
    @{ Texture = 'arthur_do_val'; Source = 'arthur_do_val_skin_64x64_corrigida.png' },
    @{ Texture = 'combatente_ucraniana'; Source = 'skin-loira.png' },
    @{ Texture = 'haddad'; Source = 'haddad_skin_64x64.png' },
    @{ Texture = 'guilherme_boulos'; Source = 'guilherme_boulos_skin_64x64.png' },
    @{ Texture = 'petista\petista_0'; Source = 'petistas\petista_0.png' },
    @{ Texture = 'petista\petista_1'; Source = 'petistas\petista_1.png' },
    @{ Texture = 'petista\petista_2' },
    @{ Texture = 'petista\petista_3'; Source = 'petistas\petista_3.png' },
    @{ Texture = 'petista\petista_4'; Source = 'petistas\petista_4.png' },
    @{ Texture = 'petista\petista_5'; Source = 'petistas\petista_5.png' }
)) {
    $skinPath = Join-Path $assets ('textures\entity\' + $skin.Texture + '.png')
    $bytes = [IO.File]::ReadAllBytes($skinPath)
    if ($bytes.Length -lt 24 -or [BitConverter]::ToString($bytes, 0, 8) -ne '89-50-4E-47-0D-0A-1A-0A') {
        throw "PNG inválido: $skinPath"
    }
    $widthBytes = [byte[]]$bytes[16..19]
    $heightBytes = [byte[]]$bytes[20..23]
    [Array]::Reverse($widthBytes)
    [Array]::Reverse($heightBytes)
    if ([BitConverter]::ToInt32($widthBytes, 0) -ne 64 -or [BitConverter]::ToInt32($heightBytes, 0) -ne 64) {
        throw "A skin deve ter 64 x 64 pixels: $skinPath"
    }
    # petista_2 has no byte-identical source: it is the 64x32 legacy skin converted to 64x64.
    if (-not $skin.Source) { continue }
    $source = Join-Path $projectRoot ('assets-entrada\skins\' + $skin.Source)
    if ((Test-Path -LiteralPath $source) -and
        (Get-FileHash -LiteralPath $source).Hash -ne (Get-FileHash -LiteralPath $skinPath).Hash) {
        throw "Skin fonte e recurso diferentes: $skinPath"
    }
}
$swordPath = Join-Path $assets 'textures\item\espada_ucraniana.png'
$sword = [IO.File]::ReadAllBytes($swordPath)
if ([BitConverter]::ToString($sword, 0, 8) -ne '89-50-4E-47-0D-0A-1A-0A' -or
    [BitConverter]::ToString($sword, 16, 8) -ne '00-00-00-10-00-00-00-10') { throw 'A espada deve ser PNG 16 x 16.' }
$swordSource = Join-Path $projectRoot 'assets-entrada\texturas\espada_bandeira_ucrania_16x16.png'
if ((Get-FileHash -LiteralPath $swordSource).Hash -ne (Get-FileHash -LiteralPath $swordPath).Hash) {
    throw 'A textura da espada deve preservar o sprite original.'
}
$sounds = Get-Content -LiteralPath (Join-Path $assets 'sounds.json') -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($event in $sounds.PSObject.Properties) {
    if (-not $pt.PSObject.Properties[$event.Value.subtitle]) { throw "Legenda ausente: $($event.Value.subtitle)" }
    foreach ($sound in $event.Value.sounds) {
        $name = if ($sound -is [string]) { $sound } else { $sound.name }
        $relative = $name -replace '^guerra_ezuberante:', ''
        $file = Join-Path $assets ('sounds\' + $relative + '.ogg')
        if (-not (Test-Path -LiteralPath $file)) { throw "OGG ausente: $file" }
        $header = [IO.File]::ReadAllBytes($file)
        if ($header.Length -lt 4 -or [Text.Encoding]::ASCII.GetString($header, 0, 4) -ne 'OggS') {
            throw "Cabeçalho OGG inválido: $file"
        }
    }
}
Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\main\resources') -Filter '*.json' -Recurse |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8 | ConvertFrom-Json | Out-Null }
Write-Host 'OK: JSONs, traduções, legendas, OGGs e skin 64 x 64 preservada.'
