$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$jdk=(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$classes=Join-Path $projectRoot '.tools/lula-generator'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
Push-Location $projectRoot
try {
    & (Join-Path $jdk 'bin/javac.exe') -encoding UTF-8 -d $classes tools/GenerateGeckoAssets.java tools/GenerateLulaAssets.java
    if($LASTEXITCODE -ne 0){throw 'Lula generator compilation failed.'}
    & (Join-Path $jdk 'bin/java.exe') -cp $classes GenerateLulaAssets
    if($LASTEXITCODE -ne 0){throw 'Lula asset generation failed.'}
}finally{Pop-Location}
