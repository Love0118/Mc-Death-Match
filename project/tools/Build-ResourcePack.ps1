[CmdletBinding()]
param(
    [string]$BasePack = '',
    [string]$OutputDirectory = '',
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
$commonRoot = Join-Path $projectRoot 'resource-pack'
$variantRoot = Join-Path $projectRoot 'resource-pack-variants'
$matrixPath = Join-Path $variantRoot 'matrix.json'
$runtimeDirectory = Join-Path $ServerRoot 'plugins\DropboxAutoResourcePack\resourcepacks'
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $projectRoot 'dist'
}
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)

if (-not (Test-Path -LiteralPath (Join-Path $commonRoot 'assets') -PathType Container)) {
    throw 'resource-pack/assets is missing'
}
if (-not (Test-Path -LiteralPath $matrixPath -PathType Leaf)) {
    throw 'resource-pack-variants/matrix.json is missing'
}
if (-not [string]::IsNullOrWhiteSpace($BasePack)) {
    $BasePack = [System.IO.Path]::GetFullPath($BasePack)
    if (-not (Test-Path -LiteralPath $BasePack -PathType Leaf)) {
        throw "Base resource pack does not exist: $BasePack"
    }
}

$variants = @(Get-Content -Raw -LiteralPath $matrixPath | ConvertFrom-Json)
if ($variants.Count -eq 0) {
    throw 'Resource-pack matrix must contain at least one variant'
}
$protocols = @{}
$fileNames = @{}
foreach ($variant in $variants) {
    if ($variant.pack_format.Count -ne 2) {
        throw "Variant $($variant.id) requires a [major, minor] pack_format"
    }
    if ($variant.metadata -notin @('legacy', 'versioned')) {
        throw "Variant $($variant.id) has an unsupported metadata mode"
    }
    if ($protocols.ContainsKey([int]$variant.protocol)) {
        throw "Duplicate resource-pack protocol: $($variant.protocol)"
    }
    $protocols[[int]$variant.protocol] = $variant.id
    $fileKey = ([string]$variant.file).ToLowerInvariant()
    if ($fileNames.ContainsKey($fileKey) -or -not $fileKey.EndsWith('.zip')) {
        throw "Duplicate or invalid resource-pack output file: $($variant.file)"
    }
    $fileNames[$fileKey] = $variant.id
    $shaderSource = Join-Path $variantRoot ([string]$variant.shader_source -replace '/', '\')
    if (-not (Test-Path -LiteralPath $shaderSource -PathType Leaf)) {
        throw "Variant shader is missing: $shaderSource"
    }
}

function Write-Utf8File {
    param([string]$Path, [string]$Text)
    [System.IO.File]::WriteAllText($Path, $Text, [System.Text.UTF8Encoding]::new($false))
}

function Write-PackMetadata {
    param([object]$Variant, [string]$Path)
    $major = [int]$Variant.pack_format[0]
    $minor = [int]$Variant.pack_format[1]
    $versions = @($Variant.versions) -join ', '
    $description = "Sniper PvP $versions - rifle, scope, HUD and audio"
    if ($Variant.metadata -eq 'legacy') {
        $metadata = [ordered]@{
            pack = [ordered]@{
                pack_format = $major
                supported_formats = @($major, $major)
                description = $description
            }
        }
    } else {
        $metadata = [ordered]@{
            pack = [ordered]@{
                min_format = @($major, $minor)
                max_format = @($major, $minor)
                description = $description
            }
        }
    }
    Write-Utf8File -Path $Path -Text ($metadata | ConvertTo-Json -Depth 10)
}

function Merge-SoundDefinitions {
    param([object]$BaseSounds, [string]$WorkingDirectory)
    if ($null -eq $BaseSounds) {
        return
    }
    $soundsPath = Join-Path $WorkingDirectory 'assets\sniperpvp\sounds.json'
    $overlaySounds = Get-Content -Raw -LiteralPath $soundsPath | ConvertFrom-Json
    foreach ($entry in $overlaySounds.PSObject.Properties) {
        $BaseSounds | Add-Member -NotePropertyName $entry.Name -NotePropertyValue $entry.Value -Force
    }
    Write-Utf8File -Path $soundsPath -Text ($BaseSounds | ConvertTo-Json -Depth 20)
}

function Test-PackArchive {
    param([object]$Variant, [string]$ArchivePath)
    $archive = [System.IO.Compression.ZipFile]::OpenRead($ArchivePath)
    try {
        $entries = @($archive.Entries | ForEach-Object FullName)
        if ($entries -notcontains 'pack.mcmeta') {
            throw "$ArchivePath does not contain pack.mcmeta at the ZIP root"
        }
        $target = ([string]$Variant.shader_target).Replace('\', '/')
        if ($entries -notcontains $target) {
            throw "$ArchivePath does not contain its required text shader: $target"
        }
        $legacyShader = 'assets/minecraft/shaders/core/rendertype_text.vsh'
        $modernShader = 'assets/minecraft/shaders/core/text.vsh'
        if ($target -eq $modernShader -and $entries -contains $legacyShader) {
            throw "$ArchivePath mixes the removed 26.1 text shader into the 26.2 pack"
        }
        if ($target -eq $legacyShader -and $entries -contains $modernShader) {
            throw "$ArchivePath mixes the 26.2 text shader into an older pack"
        }

        $metadataEntry = $archive.GetEntry('pack.mcmeta')
        $reader = [System.IO.StreamReader]::new($metadataEntry.Open())
        try {
            $metadata = $reader.ReadToEnd() | ConvertFrom-Json
        } finally {
            $reader.Dispose()
        }
        $major = [int]$Variant.pack_format[0]
        $minor = [int]$Variant.pack_format[1]
        if ($Variant.metadata -eq 'legacy') {
            if ([int]$metadata.pack.pack_format -ne $major -or
                [int]$metadata.pack.supported_formats[0] -ne $major -or
                [int]$metadata.pack.supported_formats[1] -ne $major) {
                throw "$ArchivePath has invalid legacy pack metadata"
            }
        } else {
            if ($null -ne $metadata.pack.pack_format -or $null -ne $metadata.pack.supported_formats -or
                [int]$metadata.pack.min_format[0] -ne $major -or
                [int]$metadata.pack.min_format[1] -ne $minor -or
                [int]$metadata.pack.max_format[0] -ne $major -or
                [int]$metadata.pack.max_format[1] -ne $minor) {
                throw "$ArchivePath has invalid versioned pack metadata"
            }
        }
    } finally {
        $archive.Dispose()
    }
}

$tempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$buildRoot = Join-Path $tempRoot ("sniper-pvp-packs-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $buildRoot, $OutputDirectory, $runtimeDirectory | Out-Null
$results = @()

try {
    foreach ($variant in $variants) {
        $workingDirectory = Join-Path $buildRoot ([string]$variant.id)
        New-Item -ItemType Directory -Force -Path $workingDirectory | Out-Null
        $baseSounds = $null
        if (-not [string]::IsNullOrWhiteSpace($BasePack)) {
            [System.IO.Compression.ZipFile]::ExtractToDirectory($BasePack, $workingDirectory)
            if (-not (Test-Path -LiteralPath (Join-Path $workingDirectory 'pack.mcmeta'))) {
                throw 'The base pack must contain pack.mcmeta at the ZIP root'
            }
            $baseSoundsPath = Join-Path $workingDirectory 'assets\sniperpvp\sounds.json'
            if (Test-Path -LiteralPath $baseSoundsPath) {
                $baseSounds = Get-Content -Raw -LiteralPath $baseSoundsPath | ConvertFrom-Json
            }
        }

        Get-ChildItem -Force -LiteralPath $commonRoot |
            Copy-Item -Destination $workingDirectory -Recurse -Force
        Merge-SoundDefinitions -BaseSounds $baseSounds -WorkingDirectory $workingDirectory

        foreach ($relative in @(
            'assets\minecraft\shaders\core\rendertype_text.vsh',
            'assets\minecraft\shaders\core\text.vsh'
        )) {
            $candidate = Join-Path $workingDirectory $relative
            if (Test-Path -LiteralPath $candidate) {
                Remove-Item -LiteralPath $candidate -Force
            }
        }

        Write-PackMetadata -Variant $variant -Path (Join-Path $workingDirectory 'pack.mcmeta')
        $shaderSource = Join-Path $variantRoot ([string]$variant.shader_source -replace '/', '\')
        $shaderDestination = Join-Path $workingDirectory ([string]$variant.shader_target -replace '/', '\')
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $shaderDestination) | Out-Null
        Copy-Item -LiteralPath $shaderSource -Destination $shaderDestination -Force

        $stableTimestamp = [datetime]::SpecifyKind([datetime]'1980-01-01T00:00:00', 'Utc')
        Get-ChildItem -LiteralPath $workingDirectory -Force -Recurse | ForEach-Object {
            $_.LastWriteTimeUtc = $stableTimestamp
        }

        $output = Join-Path $OutputDirectory ([string]$variant.file)
        if (Test-Path -LiteralPath $output) {
            Remove-Item -LiteralPath $output -Force
        }
        [System.IO.Compression.ZipFile]::CreateFromDirectory(
            $workingDirectory,
            $output,
            [System.IO.Compression.CompressionLevel]::Optimal,
            $false
        )
        Test-PackArchive -Variant $variant -ArchivePath $output
        $runtimePack = Join-Path $runtimeDirectory ([string]$variant.file)
        Copy-Item -LiteralPath $output -Destination $runtimePack -Force
        $results += [pscustomobject]@{
            Versions = @($variant.versions) -join ', '
            Protocol = [int]$variant.protocol
            Format = "$([int]$variant.pack_format[0]).$([int]$variant.pack_format[1])"
            File = [string]$variant.file
            SHA1 = (Get-FileHash -Algorithm SHA1 -LiteralPath $output).Hash
            Bytes = (Get-Item -LiteralPath $output).Length
        }
    }
} finally {
    $resolvedBuildRoot = [System.IO.Path]::GetFullPath($buildRoot)
    if ($resolvedBuildRoot.StartsWith($tempRoot, [System.StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path -Leaf $resolvedBuildRoot).StartsWith('sniper-pvp-packs-')) {
        Remove-Item -LiteralPath $resolvedBuildRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}

$results | Format-Table -AutoSize
Write-Host "Built $($results.Count) version-specific resource packs." -ForegroundColor Green
