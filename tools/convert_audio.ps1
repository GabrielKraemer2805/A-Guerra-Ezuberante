param(
    [string]$InputDirectory,
    [string]$FFmpegPath,
    [ValidateSet('renan', 'arthur')][string]$Character = 'renan'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $InputDirectory) {
    $InputDirectory = Join-Path $projectRoot ('assets-entrada\audio\' + $Character)
    if ($Character -eq 'renan' -and -not (Test-Path -LiteralPath $InputDirectory)) {
        $InputDirectory = Join-Path $projectRoot 'assets-entrada\audio'
    }
}
$outputDirectory = Join-Path $projectRoot ('src\main\resources\assets\guerra_ezuberante\sounds\' + $Character)

if (-not $FFmpegPath) {
    $installed = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($installed) { $FFmpegPath = $installed.Source }
    else {
        $local = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\ffmpeg') -Filter ffmpeg.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($local) { $FFmpegPath = $local.FullName }
    }
}
if (-not $FFmpegPath -or -not (Test-Path -LiteralPath $FFmpegPath)) {
    throw 'FFmpeg não encontrado. Instale-o no PATH ou passe -FFmpegPath com o caminho do executável.'
}
if (-not (Test-Path -LiteralPath $InputDirectory)) { throw "Pasta de entrada inexistente: $InputDirectory" }
$files = @(Get-ChildItem -LiteralPath $InputDirectory -File -Recurse | Where-Object { $_.Extension -ieq '.mp3' } | Sort-Object Name)
if ($files.Count -eq 0) { throw "Nenhum MP3 encontrado em $InputDirectory" }
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$outputs = @{}
$hashes = @{}
$jobs = foreach ($file in $files) {
    $hash = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
    if ($hashes.ContainsKey($hash)) {
        Write-Host "Duplicata idêntica preservada sem gerar outro OGG: $($file.Name) = $($hashes[$hash])"
        continue
    }
    $hashes[$hash] = $file.Name
    $decomposed = $file.BaseName.Normalize([Text.NormalizationForm]::FormD)
    $ascii = [regex]::Replace($decomposed, '\p{Mn}', '')
    $name = [regex]::Replace($ascii.ToLowerInvariant(), '[^a-z0-9]+', '_').Trim('_')
    if ($name -eq 'me_respeita_normal') { $name = 'me_respeita' }
    if ($name -eq 'renan_risadinha_safada') { $name = 'risadinha' }
    if ($name -eq 'me_respeita_creu_creu_phonk') { $name = 'phonk' }
    if (-not $name) { throw "Nome inválido: $($file.Name)" }
    if ($outputs.ContainsKey($name)) { throw "Dois MP3s produzem o mesmo nome '$name'. Renomeie as entradas." }
    $outputs[$name] = $true
    [pscustomobject]@{ Input = $file.FullName; Output = (Join-Path $outputDirectory ($name + '.ogg')) }
}
foreach ($job in $jobs) {
    Write-Host "Convertendo: $($job.Input)"
    & $FFmpegPath -hide_banner -loglevel error -y -i $job.Input -vn -ac 1 -ar 44100 -c:a libvorbis -q:a 4 $job.Output
    if ($LASTEXITCODE -ne 0) { throw "FFmpeg falhou ao converter $($job.Input) (código $LASTEXITCODE)." }
    Write-Host "OK: $($job.Output)"
}
Write-Host 'Conversão concluída. Originais preservados; nenhum corte foi aplicado.'
Write-Host 'Novas falas precisam também de registro em ModSounds, sounds.json e traduções.'
