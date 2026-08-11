$ErrorActionPreference = 'Stop'
$dir = Join-Path $PSScriptRoot '..\static'
New-Item -ItemType Directory -Force -Path $dir | Out-Null
Add-Type -AssemblyName System.Drawing

function New-TabPng {
  param([string]$Path, [int[]]$BgRgb, [int[]]$FgRgb, [string]$Shape)
  $bmp = New-Object System.Drawing.Bitmap 81, 81
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.Clear([System.Drawing.Color]::FromArgb(255, $BgRgb[0], $BgRgb[1], $BgRgb[2]))
  $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, $FgRgb[0], $FgRgb[1], $FgRgb[2])), 4
  $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, $FgRgb[0], $FgRgb[1], $FgRgb[2]))
  if ($Shape -eq 'home') {
    $g.DrawEllipse($pen, 16, 16, 48, 48)
    $g.FillEllipse($brush, 28, 28, 24, 24)
  } else {
    $g.DrawRectangle($pen, 20, 22, 40, 32)
    $g.DrawLine($pen, 26, 30, 54, 30)
    $g.DrawLine($pen, 26, 40, 48, 40)
  }
  $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
  $g.Dispose()
  $bmp.Dispose()
}

New-TabPng (Join-Path $dir 'tab-home.png') @(245, 250, 249) @(122, 130, 138) 'home'
New-TabPng (Join-Path $dir 'tab-home-active.png') @(224, 242, 241) @(13, 148, 136) 'home'
New-TabPng (Join-Path $dir 'tab-svc.png') @(245, 250, 249) @(122, 130, 138) 'doc'
New-TabPng (Join-Path $dir 'tab-svc-active.png') @(224, 242, 241) @(13, 148, 136) 'doc'
Get-ChildItem $dir
