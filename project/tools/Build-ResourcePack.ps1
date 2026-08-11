[CmdletBinding()]
param(
    [string]$BasePack = '',
    [string]$Output = '',
    [string]$ServerRoot = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
if ([string]::IsNullOrWhiteSpace($ServerRoot)) {
    $ServerRoot = Join-Path $workspaceRoot 'server'
}
$ServerRoot = [System.IO.Path]::GetFullPath($ServerRoot)
$overlayRoot = Join-Path $projectRoot 'resource-pack'
$runtimeDirectory = Join-Path $ServerRoot 'plugins\DropboxAutoResourcePack\resourcepacks'
if ([string]::IsNullOrWhiteSpace($Output)) {
    $Output = Join-Path $projectRoot 'dist\sniper-pvp-1.21.8.zip'
}
$Output = [System.IO.Path]::GetFullPath($Output)

if (-not (Test-Path -LiteralPath (Join-Path $overlayRoot 'pack.mcmeta'))) {
    throw "resource-pack/pack.mcmeta is missing"
}
if (-not [string]::IsNullOrWhiteSpace($BasePack)) {
    $BasePack = [System.IO.Path]::GetFullPath($BasePack)
    if (-not (Test-Path -LiteralPath $BasePack -PathType Leaf)) {
        throw "Base resource pack does not exist: $BasePack"
    }
}

$tempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$workingDirectory = Join-Path $tempRoot ("sniper-pvp-pack-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $workingDirectory | Out-Null

try {
    $baseSounds = $null
    if (-not [string]::IsNullOrWhiteSpace($BasePack)) {
        [System.IO.Compression.ZipFile]::ExtractToDirectory($BasePack, $workingDirectory)
        if (-not (Test-Path -LiteralPath (Join-Path $workingDirectory 'pack.mcmeta'))) {
            throw 'The base pack must contain pack.mcmeta at the ZIP root (not inside a wrapper folder)'
        }
        $baseSoundsPath = Join-Path $workingDirectory 'assets\sniperpvp\sounds.json'
        if (Test-Path -LiteralPath $baseSoundsPath) {
            $baseSounds = Get-Content -Raw -LiteralPath $baseSoundsPath | ConvertFrom-Json
        }
    }

    Get-ChildItem -Force -LiteralPath $overlayRoot | Copy-Item -Destination $workingDirectory -Recurse -Force

    if ($null -ne $baseSounds) {
        $overlaySoundsPath = Join-Path $workingDirectory 'assets\sniperpvp\sounds.json'
        $overlaySounds = Get-Content -Raw -LiteralPath $overlaySoundsPath | ConvertFrom-Json
        foreach ($entry in $overlaySounds.PSObject.Properties) {
            $baseSounds | Add-Member -NotePropertyName $entry.Name -NotePropertyValue $entry.Value -Force
        }
        $json = $baseSounds | ConvertTo-Json -Depth 20
        [System.IO.File]::WriteAllText($overlaySoundsPath, $json, [System.Text.UTF8Encoding]::new($false))
    }

    $outputDirectory = Split-Path -Parent $Output
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
    if (Test-Path -LiteralPath $Output) {
        Remove-Item -LiteralPath $Output -Force
    }
    [System.IO.Compression.ZipFile]::CreateFromDirectory(
        $workingDirectory,
        $Output,
        [System.IO.Compression.CompressionLevel]::Optimal,
        $false
    )
} finally {
    $resolvedWorking = [System.IO.Path]::GetFullPath($workingDirectory)
    if ($resolvedWorking.StartsWith($tempRoot, [System.StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path -Leaf $resolvedWorking).StartsWith('sniper-pvp-pack-')) {
        Remove-Item -LiteralPath $resolvedWorking -Recurse -Force -ErrorAction SilentlyContinue
    } else {
        Write-Warning "Refused to remove unexpected temporary path: $resolvedWorking"
    }
}

New-Item -ItemType Directory -Force -Path $runtimeDirectory | Out-Null
$runtimePack = Join-Path $runtimeDirectory 'sniper-pvp-1.21.8.zip'
Copy-Item -LiteralPath $Output -Destination $runtimePack -Force
$sha1 = [System.Security.Cryptography.SHA1]::Create()
try {
    $hashStream = [System.IO.File]::OpenRead($Output)
    try {
        $hashValue = $sha1.ComputeHash($hashStream)
    } finally {
        $hashStream.Dispose()
    }
} finally {
    $sha1.Dispose()
}
$hash = -join ($hashValue | ForEach-Object { $_.ToString('X2') })
Write-Host "Resource pack: $Output"
Write-Host "Runtime copy: $runtimePack"
Write-Host "SHA-1: $hash"
