# Gera os recursos visuais do NEXCORE a partir do logotipo oficial (branding/nexcore-logo.png, 736x736).
# O desenho nao e alterado: so redimensionado, sempre na proporcao original, sobre o mesmo fundo preto.
#   powershell -ExecutionPolicy Bypass -File branding\gerar-recursos.ps1
# Saidas (versionadas, para o build nao depender deste script):
#   branding\nexcore.ico                         icone do executavel, atalhos e desinstalador (16 a 256 px)
#   branding\png\nexcore-<n>.png                 32, 64, 128, 192, 256 e 512 px
#   branding\instalador\wizard-<escala>.bmp      imagem lateral do instalador (Inno Setup, 100% a 200%)
#   branding\instalador\wizard-small-<n>.bmp     imagem do canto do instalador
#   frontend\public\brand\                       logo e favicon usados pela interface
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $PSScriptRoot 'nexcore-logo.png'
$original = [System.Drawing.Bitmap]::FromFile($src)
$black = [System.Drawing.Color]::Black

# O original tem uma margem preta larga; em 16-64 px o simbolo ficaria minusculo. Recorta so a margem: area do
# simbolo (pixels claros) + 18% de folga, em um quadrado centrado. O desenho em si nao muda.
$minX = $original.Width; $minY = $original.Height; $maxX = 0; $maxY = 0
for ($y = 0; $y -lt $original.Height; $y += 2) {
  for ($x = 0; $x -lt $original.Width; $x += 2) {
    if ($original.GetPixel($x, $y).GetBrightness() -gt 0.5) {
      if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
      if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
    }
  }
}
$side = [int]([Math]::Max($maxX - $minX, $maxY - $minY) * 1.18)
$cx = [int](($minX + $maxX) / 2); $cy = [int](($minY + $maxY) / 2)
$crop = New-Object System.Drawing.Rectangle ([Math]::Max(0, $cx - [int]($side / 2))), ([Math]::Max(0, $cy - [int]($side / 2))), $side, $side
$logo = $original.Clone($crop, $original.PixelFormat)
Write-Output "Simbolo em x=$minX..$maxX y=$minY..$maxY; recorte quadrado de $side px"

function New-Canvas([int]$w, [int]$h) {
  $bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.Clear($black)
  return @($bmp, $g)
}

# Logo quadrado de lado $side, centralizado em ($cx, $cy).
function Draw-Logo($g, [int]$side, [int]$cx, [int]$cy) {
  $g.DrawImage($logo, [int]($cx - $side / 2), [int]($cy - $side / 2), $side, $side)
}

function Save-Png([int]$size, [string]$path) {
  $bmp, $g = New-Canvas $size $size
  Draw-Logo $g $size ([int]($size / 2)) ([int]($size / 2))
  $g.Dispose(); $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
}

function Save-Bmp([int]$w, [int]$h, [int]$side, [int]$cy, [string]$path) {
  $bmp, $g = New-Canvas $w $h
  Draw-Logo $g $side ([int]($w / 2)) $cy
  $g.Dispose()
  # BMP de 24 bits (formato aceito pelo Inno Setup em todas as versoes).
  $rgb = $bmp.Clone((New-Object System.Drawing.Rectangle 0, 0, $w, $h), [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $rgb.Save($path, [System.Drawing.Imaging.ImageFormat]::Bmp); $rgb.Dispose(); $bmp.Dispose()
}

$png = Join-Path $PSScriptRoot 'png'
$wiz = Join-Path $PSScriptRoot 'instalador'
$web = Join-Path $root 'frontend\public\brand'
New-Item -ItemType Directory -Force $png, $wiz, $web | Out-Null

foreach ($s in 32, 64, 128, 192, 256, 512) { Save-Png $s (Join-Path $png "nexcore-$s.png") }
Copy-Item (Join-Path $png 'nexcore-64.png'), (Join-Path $png 'nexcore-128.png'), (Join-Path $png 'nexcore-32.png'), (Join-Path $png 'nexcore-192.png') $web

# ICO com imagens PNG embutidas (formato aceito desde o Windows Vista).
$sizes = 16, 24, 32, 48, 64, 128, 256
$images = foreach ($s in $sizes) {
  $bmp, $g = New-Canvas $s $s
  Draw-Logo $g $s ([int]($s / 2)) ([int]($s / 2))
  $g.Dispose(); $ms = New-Object System.IO.MemoryStream
  $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
  , $ms.ToArray()
}
$out = New-Object System.IO.MemoryStream
$w = New-Object System.IO.BinaryWriter $out
$w.Write([uint16]0); $w.Write([uint16]1); $w.Write([uint16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
  $s = $sizes[$i]; $len = $images[$i].Length
  $w.Write([byte]($(if ($s -ge 256) { 0 } else { $s }))); $w.Write([byte]($(if ($s -ge 256) { 0 } else { $s })))
  $w.Write([byte]0); $w.Write([byte]0); $w.Write([uint16]1); $w.Write([uint16]32)
  $w.Write([uint32]$len); $w.Write([uint32]$offset); $offset += $len
}
foreach ($img in $images) { $w.Write($img) }
$w.Flush(); [System.IO.File]::WriteAllBytes((Join-Path $PSScriptRoot 'nexcore.ico'), $out.ToArray()); $w.Dispose()

# Inno Setup: imagem lateral (164x314 em 100%) e do canto (55x55 em 100%), em varias escalas de tela.
$wizard = @{ '100' = @(164, 314); '125' = @(192, 386); '150' = @(246, 459); '175' = @(273, 556); '200' = @(328, 628) }
foreach ($k in $wizard.Keys) {
  $ww, $hh = $wizard[$k]
  Save-Bmp $ww $hh ([int]($ww * 0.86)) ([int]($hh * 0.42)) (Join-Path $wiz "wizard-$k.bmp")
}
foreach ($s in 55, 64, 83, 92, 110, 119, 138) { Save-Bmp $s $s $s ([int]($s / 2)) (Join-Path $wiz "wizard-small-$s.bmp") }

$logo.Dispose(); $original.Dispose()
Write-Output "Recursos gerados em $PSScriptRoot e $web"
