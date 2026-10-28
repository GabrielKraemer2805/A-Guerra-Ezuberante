$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$jdk=(Get-ChildItem (Join-Path $root '.tools/jdk17') -Directory|Select-Object -First 1).FullName
$classes=Join-Path $root '.tools/bolsonaro-generator'
New-Item -ItemType Directory -Force $classes|Out-Null
Push-Location $root
try{
 & (Join-Path $jdk 'bin/javac.exe') -encoding UTF-8 -d $classes tools/GenerateGeckoAssets.java tools/GenerateBolsonaroAssets.java
 if($LASTEXITCODE -ne 0){throw 'Generator compilation failed.'}
 & (Join-Path $jdk 'bin/java.exe') -cp $classes GenerateBolsonaroAssets
 if($LASTEXITCODE -ne 0){throw 'Asset generation failed.'}
}finally{Pop-Location}
