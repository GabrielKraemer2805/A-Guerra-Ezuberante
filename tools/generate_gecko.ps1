param([string]$JavaPath)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $JavaPath) {
    $portable = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory | Select-Object -First 1
    $JavaPath = Join-Path $portable.FullName 'bin/java.exe'
}
Push-Location $projectRoot
try {
    & $JavaPath (Join-Path $PSScriptRoot 'GenerateGeckoAssets.java')
    if ($LASTEXITCODE -ne 0) { throw 'Gecko asset generation failed.' }
    & (Join-Path $PSScriptRoot 'validate_assets.ps1')
    & (Join-Path $PSScriptRoot 'validate_gecko.ps1')
} finally { Pop-Location }
