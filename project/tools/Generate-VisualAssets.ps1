[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$fontDirectory = Join-Path $projectRoot 'resource-pack\assets\sniperpvp\textures\font'
$scopeDirectory = Join-Path $projectRoot 'resource-pack\assets\minecraft\textures\misc'
$customScopeDirectory = Join-Path $projectRoot 'resource-pack\assets\sniperpvp\textures\misc'
$bossBarDirectory = Join-Path $projectRoot 'resource-pack\assets\minecraft\textures\gui\sprites\boss_bar'
$vanillaHudDirectory = Join-Path $projectRoot 'resource-pack\assets\minecraft\textures\gui\sprites\hud'
$heartDirectory = Join-Path $vanillaHudDirectory 'heart'
New-Item -ItemType Directory -Force `
    -Path $fontDirectory, $scopeDirectory, $customScopeDirectory, $bossBarDirectory, `
    $vanillaHudDirectory, $heartDirectory | Out-Null

function Save-Png {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [string]$Path
    )
    try {
        $Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $Bitmap.Dispose()
    }
}

function New-HudPanel {
    param(
        [string]$Path,
        [int]$Width,
        [int]$Height,
        [System.Drawing.Color]$Accent,
        [ValidateSet('Left', 'Right', 'Center')]
        [string]$Direction
    )
    $bitmap = [System.Drawing.Bitmap]::new($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $dark = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(238, 24, 31, 38))
        $inner = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(225, 40, 49, 58))
        $accentBrush = [System.Drawing.SolidBrush]::new($Accent)
        try {
            if ($Direction -eq 'Left') {
                $points = [System.Drawing.Point[]]@(
                    [System.Drawing.Point]::new(3, 0),
                    [System.Drawing.Point]::new($Width - 1, 0),
                    [System.Drawing.Point]::new($Width - 1, $Height - 4),
                    [System.Drawing.Point]::new($Width - 4, $Height - 1),
                    [System.Drawing.Point]::new(0, $Height - 1),
                    [System.Drawing.Point]::new(0, 3)
                )
            } elseif ($Direction -eq 'Right') {
                $points = [System.Drawing.Point[]]@(
                    [System.Drawing.Point]::new(0, 0),
                    [System.Drawing.Point]::new($Width - 4, 0),
                    [System.Drawing.Point]::new($Width - 1, 3),
                    [System.Drawing.Point]::new($Width - 1, $Height - 1),
                    [System.Drawing.Point]::new(3, $Height - 1),
                    [System.Drawing.Point]::new(0, $Height - 4)
                )
            } else {
                $points = [System.Drawing.Point[]]@(
                    [System.Drawing.Point]::new(4, 0),
                    [System.Drawing.Point]::new($Width - 5, 0),
                    [System.Drawing.Point]::new($Width - 1, 4),
                    [System.Drawing.Point]::new($Width - 1, $Height - 5),
                    [System.Drawing.Point]::new($Width - 5, $Height - 1),
                    [System.Drawing.Point]::new(4, $Height - 1),
                    [System.Drawing.Point]::new(0, $Height - 5),
                    [System.Drawing.Point]::new(0, 4)
                )
            }
            $graphics.FillPolygon($dark, $points)
            $graphics.FillRectangle($inner, 3, 3, $Width - 6, $Height - 6)
            if ($Direction -eq 'Right') {
                $graphics.FillRectangle($accentBrush, $Width - 3, 2, 2, $Height - 4)
            } elseif ($Direction -eq 'Center') {
                $graphics.FillRectangle($accentBrush, 5, 1, $Width - 10, 2)
            } else {
                $graphics.FillRectangle($accentBrush, 1, 2, 2, $Height - 4)
            }
        } finally {
            $dark.Dispose()
            $inner.Dispose()
            $accentBrush.Dispose()
        }
    } finally {
        $graphics.Dispose()
    }
    Save-Png -Bitmap $bitmap -Path $Path
}

$teal = [System.Drawing.Color]::FromArgb(255, 82, 196, 181)
$coral = [System.Drawing.Color]::FromArgb(255, 223, 104, 111)
$silver = [System.Drawing.Color]::FromArgb(255, 218, 226, 230)
New-HudPanel -Path (Join-Path $fontDirectory 'hud_card_left.png') -Width 71 -Height 16 -Accent $teal -Direction Left
New-HudPanel -Path (Join-Path $fontDirectory 'hud_card_right.png') -Width 71 -Height 16 -Accent $coral -Direction Right
New-HudPanel -Path (Join-Path $fontDirectory 'hud_timer.png') -Width 47 -Height 16 -Accent $silver -Direction Center
New-HudPanel -Path (Join-Path $fontDirectory 'hud_kill_banner.png') -Width 127 -Height 18 -Accent $teal -Direction Center

$rifleIcon = [System.Drawing.Bitmap]::new(22, 9)
$rifleGraphics = [System.Drawing.Graphics]::FromImage($rifleIcon)
try {
    $rifleGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $rifleGraphics.Clear([System.Drawing.Color]::Transparent)
    $shadow = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(245, 9, 16, 20))
    $metal = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 198, 226, 229))
    $accent = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 82, 196, 181))
    try {
        $rifleGraphics.FillRectangle($shadow, 0, 4, 20, 4)
        $rifleGraphics.FillRectangle($shadow, 6, 1, 8, 3)
        $rifleGraphics.FillRectangle($shadow, 8, 7, 4, 2)
        $rifleGraphics.FillRectangle($metal, 2, 4, 16, 2)
        $rifleGraphics.FillRectangle($metal, 7, 2, 6, 1)
        $rifleGraphics.FillRectangle($metal, 18, 3, 4, 2)
        $rifleGraphics.FillRectangle($accent, 5, 6, 8, 1)
        $rifleGraphics.FillRectangle($accent, 9, 7, 2, 2)
    } finally {
        $shadow.Dispose()
        $metal.Dispose()
        $accent.Dispose()
    }
} finally {
    $rifleGraphics.Dispose()
}
Save-Png -Bitmap $rifleIcon -Path (Join-Path $fontDirectory 'hud_rifle_icon.png')

$scopeWidth = 512
$scopeHeight = 288
$scopeCenterX = [int]($scopeWidth / 2)
$scopeCenterY = [int]($scopeHeight / 2)
$scopeRadius = 126
$scope = [System.Drawing.Bitmap]::new($scopeWidth, $scopeHeight)
$scopeGraphics = [System.Drawing.Graphics]::FromImage($scope)
try {
    $scopeGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $scopeGraphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $scopeGraphics.Clear([System.Drawing.Color]::FromArgb(255, 0, 0, 0))
    $transparent = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::Transparent)
    try {
        $scopeGraphics.FillEllipse(
            $transparent,
            $scopeCenterX - $scopeRadius,
            $scopeCenterY - $scopeRadius,
            $scopeRadius * 2,
            $scopeRadius * 2
        )
    } finally {
        $transparent.Dispose()
    }
    $scopeGraphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
    $ring = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(210, 10, 15, 18), 1.5)
    try {
        $scopeGraphics.DrawEllipse(
            $ring,
            $scopeCenterX - $scopeRadius,
            $scopeCenterY - $scopeRadius,
            $scopeRadius * 2,
            $scopeRadius * 2
        )
    } finally {
        $ring.Dispose()
    }
} finally {
    $scopeGraphics.Dispose()
}
Save-Png -Bitmap $scope -Path (Join-Path $scopeDirectory 'spyglass_scope.png')
Copy-Item -LiteralPath (Join-Path $scopeDirectory 'spyglass_scope.png') `
    -Destination (Join-Path $customScopeDirectory 'scope.png') -Force

foreach ($name in @('white_background.png', 'white_progress.png')) {
    $transparentBossBar = [System.Drawing.Bitmap]::new(182, 5)
    Save-Png -Bitmap $transparentBossBar -Path (Join-Path $bossBarDirectory $name)
}

$heartSprites = @(
    'container', 'container_blinking', 'container_hardcore', 'container_hardcore_blinking',
    'full', 'full_blinking', 'half', 'half_blinking',
    'hardcore_full', 'hardcore_full_blinking', 'hardcore_half', 'hardcore_half_blinking',
    'absorbing_full', 'absorbing_full_blinking', 'absorbing_half', 'absorbing_half_blinking',
    'absorbing_hardcore_full', 'absorbing_hardcore_full_blinking',
    'absorbing_hardcore_half', 'absorbing_hardcore_half_blinking',
    'frozen_full', 'frozen_full_blinking', 'frozen_half', 'frozen_half_blinking',
    'frozen_hardcore_full', 'frozen_hardcore_full_blinking',
    'frozen_hardcore_half', 'frozen_hardcore_half_blinking',
    'poisoned_full', 'poisoned_full_blinking', 'poisoned_half', 'poisoned_half_blinking',
    'poisoned_hardcore_full', 'poisoned_hardcore_full_blinking',
    'poisoned_hardcore_half', 'poisoned_hardcore_half_blinking',
    'withered_full', 'withered_full_blinking', 'withered_half', 'withered_half_blinking',
    'withered_hardcore_full', 'withered_hardcore_full_blinking',
    'withered_hardcore_half', 'withered_hardcore_half_blinking',
    'vehicle_container', 'vehicle_full', 'vehicle_half'
)
foreach ($name in $heartSprites) {
    $transparentSprite = [System.Drawing.Bitmap]::new(9, 9)
    Save-Png -Bitmap $transparentSprite -Path (Join-Path $heartDirectory "$name.png")
}

foreach ($name in @(
    'food_empty.png', 'food_full.png', 'food_half.png',
    'food_empty_hunger.png', 'food_full_hunger.png', 'food_half_hunger.png',
    'armor_empty.png', 'armor_full.png', 'armor_half.png'
)) {
    $transparentSprite = [System.Drawing.Bitmap]::new(9, 9)
    Save-Png -Bitmap $transparentSprite -Path (Join-Path $vanillaHudDirectory $name)
}

Write-Host 'Generated HUD panels/icons, hidden vanilla health/food sprites, and crosshair-free scope overlays.'
