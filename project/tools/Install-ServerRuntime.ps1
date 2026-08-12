[CmdletBinding()]
param(
    [string]$ServerRoot = ''
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
if ([string]::IsNullOrWhiteSpace($ServerRoot)) {
    $ServerRoot = Join-Path $workspaceRoot 'server'
}
$ServerRoot = [System.IO.Path]::GetFullPath($ServerRoot)
$pluginsDirectory = Join-Path $ServerRoot 'plugins'
$downloadDirectory = Join-Path $projectRoot '.build\runtime-downloads'
New-Item -ItemType Directory -Force -Path $ServerRoot, $pluginsDirectory, $downloadDirectory | Out-Null

$artifacts = @(
    [pscustomobject]@{
        Name = 'paper-26.2-112.jar'
        Destination = Join-Path $ServerRoot 'paper-26.2-112.jar'
        Url = 'https://fill-data.papermc.io/v1/objects/bd3a58cf96874e5ea6643f5f6fe9b4f5bf9e34b795fa078c2f0ee8b98b2f907e/paper-26.2-112.jar'
        Sha256 = 'bd3a58cf96874e5ea6643f5f6fe9b4f5bf9e34b795fa078c2f0ee8b98b2f907e'
    },
    [pscustomobject]@{
        Name = 'ViaVersion-5.11.0.jar'
        Destination = Join-Path $pluginsDirectory 'ViaVersion-5.11.0.jar'
        Url = 'https://hangarcdn.papermc.io/plugins/ViaVersion/ViaVersion/versions/5.11.0/PAPER/ViaVersion-5.11.0.jar'
        Sha256 = '89db76c8e3e674238f5eee2bb7a9e9a2beeba0760bbd1b86494778e8a5a52f70'
    },
    [pscustomobject]@{
        Name = 'ViaBackwards-5.11.0.jar'
        Destination = Join-Path $pluginsDirectory 'ViaBackwards-5.11.0.jar'
        Url = 'https://hangarcdn.papermc.io/plugins/ViaVersion/ViaBackwards/versions/5.11.0/PAPER/ViaBackwards-5.11.0.jar'
        Sha256 = '41085a59d784c9a0d14917fe7487ef5e201a9da7825fd047f08d328ff33eecdc'
    },
    [pscustomobject]@{
        Name = 'NoChatReports-2.7.8.jar'
        Destination = Join-Path $pluginsDirectory 'NoChatReports-2.7.8.jar'
        Url = 'https://cdn.modrinth.com/data/XRJBgd3p/versions/ulQJdHkG/NoChatReports-2.7.8.jar'
        Sha256 = 'a0d898a7c6b02ef3cec06b60b824a8cd247bc8470112509a962d8ba96c7bfbe4'
    }
)

function Get-Sha256 {
    param([string]$Path)
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

foreach ($artifact in $artifacts) {
    $cached = Join-Path $downloadDirectory $artifact.Name
    if (-not (Test-Path -LiteralPath $cached) -or (Get-Sha256 $cached) -ne $artifact.Sha256) {
        $pending = "$cached.download"
        Remove-Item -LiteralPath $pending -Force -ErrorAction SilentlyContinue
        Invoke-WebRequest -Headers @{
            'User-Agent' = 'SniperPvpMigration/1.0 (Love0118/Mc-Death-Match)'
        } -Uri $artifact.Url -OutFile $pending
        $actual = Get-Sha256 $pending
        if ($actual -ne $artifact.Sha256) {
            Remove-Item -LiteralPath $pending -Force -ErrorAction SilentlyContinue
            throw "SHA-256 mismatch for $($artifact.Name): $actual"
        }
        Move-Item -LiteralPath $pending -Destination $cached -Force
    }
    $cachedHash = Get-Sha256 $cached
    if ($cachedHash -ne $artifact.Sha256) {
        throw "Cached SHA-256 mismatch for $($artifact.Name): $cachedHash"
    }
    Copy-Item -LiteralPath $cached -Destination $artifact.Destination -Force
    if ((Get-Sha256 $artifact.Destination) -ne $artifact.Sha256) {
        throw "Installed SHA-256 mismatch for $($artifact.Name)"
    }
}

$obsoletePatterns = @(
    [pscustomobject]@{ Directory = $ServerRoot; Pattern = 'paper-*.jar'; Keep = 'paper-26.2-112.jar' },
    [pscustomobject]@{ Directory = $pluginsDirectory; Pattern = 'ViaVersion-*.jar'; Keep = 'ViaVersion-5.11.0.jar' },
    [pscustomobject]@{ Directory = $pluginsDirectory; Pattern = 'ViaBackwards-*.jar'; Keep = 'ViaBackwards-5.11.0.jar' },
    [pscustomobject]@{ Directory = $pluginsDirectory; Pattern = 'NoChatReports-*.jar'; Keep = 'NoChatReports-2.7.8.jar' }
)
foreach ($rule in $obsoletePatterns) {
    $resolvedDirectory = [System.IO.Path]::GetFullPath($rule.Directory)
    if (-not $resolvedDirectory.StartsWith($ServerRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean artifacts outside the server root: $resolvedDirectory"
    }
    Get-ChildItem -LiteralPath $resolvedDirectory -Filter $rule.Pattern -File -ErrorAction SilentlyContinue |
        Where-Object Name -ne $rule.Keep |
        Remove-Item -Force
}

$viaConfigs = @(
    [pscustomobject]@{
        Directory = Join-Path $pluginsDirectory 'ViaVersion'
        Source = Join-Path $projectRoot 'server-config\ViaVersion.config.yml'
    },
    [pscustomobject]@{
        Directory = Join-Path $pluginsDirectory 'ViaBackwards'
        Source = Join-Path $projectRoot 'server-config\ViaBackwards.config.yml'
    }
)
foreach ($config in $viaConfigs) {
    New-Item -ItemType Directory -Force -Path $config.Directory | Out-Null
    $destination = Join-Path $config.Directory 'config.yml'
    Copy-Item -LiteralPath $config.Source -Destination $destination -Force
}

Write-Host 'Installed and SHA-256 verified:' -ForegroundColor Green
$artifacts | ForEach-Object {
    Write-Host "  $($_.Name)  $($_.Sha256)"
}
