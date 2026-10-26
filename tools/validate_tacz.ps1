param([Parameter(Mandatory=$true)][string]$TaczJar)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
function Require($condition,$message){if(-not $condition){throw $message}}
Require (Test-Path -LiteralPath $TaczJar) 'Missing official TaCZ dependency.'
$expectedHash='2CBB89F44333D2EB4E825D86D789FF119B272FF75184390B55BAFD4F0E7F10C2'
Require ((Get-FileHash -LiteralPath $TaczJar -Algorithm SHA256).Hash -eq $expectedHash) 'Unexpected TaCZ binary; expected official CurseForge file 9037989.'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::OpenRead($TaczJar)
try {
    function Read-ZipText([string]$name) {
        $entry=$zip.GetEntry($name)
        Require ($null -ne $entry) "Missing dependency resource: $name"
        $reader=[IO.StreamReader]::new($entry.Open())
        try{return $reader.ReadToEnd()}finally{$reader.Dispose()}
    }
    $manifest=Read-ZipText 'META-INF/MANIFEST.MF'
    Require ($manifest.Contains('Implementation-Version: 1.1.8-hotfix2')) 'Wrong TaCZ implementation version.'
    $weapons=[ordered]@{ak47='762x39';m4a1='556x45';glock_17='9mm';deagle='50ae';hk_mp5a5='9mm'}
    foreach($weapon in $weapons.GetEnumerator()) {
        $index=Read-ZipText ('assets/tacz/custom/tacz_default_gun/data/tacz/index/guns/'+$weapon.Key+'.json')
        $dataId=[regex]::Match($index,'"data"\s*:\s*"tacz:([a-z0-9_]+)"').Groups[1].Value
        Require ($dataId.Length -gt 0) "Missing gun data reference: $($weapon.Key)"
        $data=Read-ZipText ('assets/tacz/custom/tacz_default_gun/data/tacz/data/guns/'+$dataId+'.json')
        $ammo=[regex]::Match($data,'"ammo"\s*:\s*"([a-z0-9_:]+)"').Groups[1].Value
        Require ($ammo -eq ('tacz:'+$weapon.Value)) "Calibre mismatch for $($weapon.Key)"
        $null=Read-ZipText ('assets/tacz/custom/tacz_default_gun/data/tacz/index/ammo/'+$weapon.Value+'.json')
    }
} finally {$zip.Dispose()}
Require (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'src/main/resources/assets/tacz'))) 'Third-party assets must remain in the external mod.'
Require (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'src/main/java/com/tacz'))) 'Do not copy TaCZ implementation into this project.'
$props=Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') -Raw
$toml=Get-Content -LiteralPath (Join-Path $projectRoot 'src/main/resources/META-INF/mods.toml') -Raw
Require ($props.Contains('tacz_file_id=9037989') -and $props.Contains('tacz_version=1.1.8-hotfix2')) 'Dependency pin changed without verification.'
Require ($toml.Contains('modId="tacz"') -and $toml.Contains('versionRange="[${tacz_version}]"')) 'Missing exact required TaCZ dependency.'
Write-Host 'PASS: official TaCZ 1.1.8-hotfix2 hash, bundled gunpack, five gun IDs, four calibre IDs, external dependency.'
