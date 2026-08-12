[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$voxelRoot = Join-Path $projectRoot 'voxel-models'
$packRoot = Join-Path $projectRoot 'resource-pack'

if (-not (Test-Path -LiteralPath $voxelRoot -PathType Container)) {
    return
}

foreach ($modelProject in Get-ChildItem -LiteralPath $voxelRoot -Directory) {
    $configPath = Join-Path $modelProject.FullName 'config.json'
    $generatedRoot = Join-Path $modelProject.FullName 'generated'
    if (-not (Test-Path -LiteralPath $configPath -PathType Leaf)) {
        continue
    }
    if (-not (Test-Path -LiteralPath $generatedRoot -PathType Container)) {
        throw "Compile voxel model before building the resource pack: $($modelProject.Name)"
    }

    $configText = Get-Content -Raw -LiteralPath $configPath
    $namespaceMatch = [regex]::Match(
        $configText,
        '(?m)^\s*"namespace"\s*:\s*"([a-z0-9_.-]+)"\s*,?\s*$'
    )
    $modelIdMatch = [regex]::Match(
        $configText,
        '(?m)^\s*"model_id"\s*:\s*"([a-z0-9_.-]+)"\s*,?\s*$'
    )
    if (-not $namespaceMatch.Success -or -not $modelIdMatch.Success) {
        throw "Voxel model config needs lowercase namespace and model_id fields: $configPath"
    }
    $namespace = $namespaceMatch.Groups[1].Value
    $modelId = $modelIdMatch.Groups[1].Value
    foreach ($asset in @(
        [pscustomobject]@{
            Source = Join-Path $generatedRoot "assets\$namespace\items\$modelId.json"
            Destination = Join-Path $packRoot "assets\$namespace\items\$modelId.json"
        },
        [pscustomobject]@{
            Source = Join-Path $generatedRoot "assets\$namespace\models\item\$modelId.json"
            Destination = Join-Path $packRoot "assets\$namespace\models\item\$modelId.json"
        },
        [pscustomobject]@{
            Source = Join-Path $generatedRoot "assets\$namespace\textures\item\${modelId}_palette.png"
            Destination = Join-Path $packRoot "assets\$namespace\textures\item\${modelId}_palette.png"
        }
    )) {
        if (-not (Test-Path -LiteralPath $asset.Source -PathType Leaf)) {
            throw "Compiled voxel asset is missing: $($asset.Source)"
        }
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $asset.Destination) | Out-Null
        Copy-Item -LiteralPath $asset.Source -Destination $asset.Destination -Force
    }
}
