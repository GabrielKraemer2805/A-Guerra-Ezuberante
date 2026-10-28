$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Add-Type -AssemblyName System.Drawing
foreach($character in @('jair_bolsonaro','flavio_bolsonaro')){
 $original=Join-Path $root "assets-entrada/skins/${character}_skin_64x64.png"
 $skin=[Drawing.Bitmap]::new($original)
 try{
  foreach($side in @(@('direita',0),@('esquerda',16))){
   $patch=[Drawing.Bitmap]::new((Join-Path $root "assets-entrada/skins/correcoes/${character}_$($side[0])_8x8.png"))
   try{
    if($patch.Width -ne 8 -or $patch.Height -ne 8){throw 'Head side patch must be 8x8.'}
    for($y=0;$y -lt 8;$y++){for($x=0;$x -lt 8;$x++){$skin.SetPixel($x+[int]$side[1],$y+8,$patch.GetPixel($x,$y))}}
   }finally{$patch.Dispose()}
  }
  $corrected=Join-Path $root "assets-entrada/skins/${character}_skin_64x64_corrigida.png"
  $skin.Save($corrected,[Drawing.Imaging.ImageFormat]::Png)
  Copy-Item -LiteralPath $corrected -Destination (Join-Path $root "src/main/resources/assets/guerra_ezuberante/textures/entity/$character.png")
 }finally{$skin.Dispose()}
}
Write-Host 'Prepared corrected 64x64 skins from imagegen head patches; original atlases preserved.'
