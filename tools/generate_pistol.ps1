$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $jdkPath=(Get-ChildItem -LiteralPath .tools/jdk17 -Directory | Select-Object -First 1).FullName
    & (Join-Path $jdkPath 'bin/java.exe') tools/GeneratePistolAssets.java
    if($LASTEXITCODE -ne 0){throw 'Pistol asset generation failed.'}
    $ffmpegPath=Join-Path $projectRoot '.tools/ffmpeg/ffmpeg-9.0.2-essentials_build/bin/ffmpeg.exe'
    $destination=Join-Path $projectRoot 'src/main/resources/assets/guerra_ezuberante/sounds/pistol'
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    foreach($name in @('shot','reload','empty')) {
        & $ffmpegPath -hide_banner -loglevel error -y -i ".tools/pistol-audio/$name.wav" -c:a libvorbis -q:a 5 "$destination/$name.ogg"
        if($LASTEXITCODE -ne 0){throw 'Pistol sound conversion failed.'}
    }
} finally {Pop-Location}
