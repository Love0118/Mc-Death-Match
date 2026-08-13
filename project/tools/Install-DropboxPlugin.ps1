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
$sourceDirectory = Join-Path $projectRoot '.build\dropbox-auto-resource-pack'
$pluginsDirectory = Join-Path $ServerRoot 'plugins'
$runtimeConfigDirectory = Join-Path $pluginsDirectory 'DropboxAutoResourcePack'
$repository = 'https://github.com/Love0118/spear-vs-zombie.git'
$branch = 'resource-pack-only'
$commit = '172643a4483306b2af1fff237309e4d800dea0f2'
$patch = Join-Path $projectRoot 'patches\dropbox-auto-resource-pack-via-client-protocol.patch'

if (-not (Test-Path -LiteralPath (Join-Path $sourceDirectory '.git'))) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $sourceDirectory) | Out-Null
    git clone --filter=blob:none --single-branch --branch $branch $repository $sourceDirectory
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to clone DropboxAutoResourcePack source'
    }
}

git -C $sourceDirectory fetch origin $branch
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to refresh DropboxAutoResourcePack source'
}
git -C $sourceDirectory checkout --detach $commit
if ($LASTEXITCODE -ne 0) {
    throw "Failed to check out pinned DropboxAutoResourcePack commit $commit"
}
$actualCommit = (git -C $sourceDirectory rev-parse HEAD).Trim()
if ($actualCommit -ne $commit) {
    throw "Unexpected DropboxAutoResourcePack commit: $actualCommit"
}
if (-not (Test-Path -LiteralPath $patch -PathType Leaf)) {
    throw "DropboxAutoResourcePack compatibility patch is missing: $patch"
}

$sourceStatus = @(git -C $sourceDirectory status --porcelain --untracked-files=all)
if ($LASTEXITCODE -ne 0 -or $sourceStatus.Count -ne 0) {
    throw 'DropboxAutoResourcePack build cache has unexpected local changes; remove it and retry'
}

$buildExitCode = 1
$patchApplied = $false
try {
    git -C $sourceDirectory apply --check $patch
    if ($LASTEXITCODE -ne 0) {
        throw 'DropboxAutoResourcePack compatibility patch no longer applies to the pinned source'
    }
    git -C $sourceDirectory apply --whitespace=nowarn $patch
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to apply DropboxAutoResourcePack ViaVersion compatibility patch'
    }
    $patchApplied = $true
    Write-Host 'Applied ViaVersion client-protocol compatibility patch.'

    git -C $sourceDirectory diff --check
    if ($LASTEXITCODE -ne 0) {
        throw 'DropboxAutoResourcePack patched source failed git diff --check'
    }

    Push-Location $sourceDirectory
    try {
        & '.\mvnw.cmd' verify
        $buildExitCode = $LASTEXITCODE
        if ($buildExitCode -eq 0) {
            & '.\mvnw.cmd' '-Ppaper-26.2-compat' test
            $buildExitCode = $LASTEXITCODE
        }
    } finally {
        Pop-Location
    }
} finally {
    if ($patchApplied) {
        git -C $sourceDirectory apply --reverse --whitespace=nowarn $patch
        if ($LASTEXITCODE -ne 0) {
            throw 'Failed to restore the clean DropboxAutoResourcePack build cache'
        }
    }
}
if ($buildExitCode -ne 0) {
    throw 'DropboxAutoResourcePack verification or Paper 26.2 compatibility check failed'
}

$builtJar = Join-Path $sourceDirectory 'target\DropboxAutoResourcePack-1.1.1.jar'
if (-not (Test-Path -LiteralPath $builtJar)) {
    throw "Built DropboxAutoResourcePack JAR is missing: $builtJar"
}
New-Item -ItemType Directory -Force -Path $pluginsDirectory | Out-Null
Get-ChildItem -LiteralPath $pluginsDirectory -Filter 'DropboxAutoResourcePack-*.jar' -File -ErrorAction SilentlyContinue |
    Where-Object Name -ne 'DropboxAutoResourcePack-1.1.1.jar' |
    Remove-Item -Force
Copy-Item -LiteralPath $builtJar -Destination (Join-Path $pluginsDirectory 'DropboxAutoResourcePack-1.1.1.jar') -Force

New-Item -ItemType Directory -Force -Path $runtimeConfigDirectory | Out-Null
$runtimeConfig = Join-Path $runtimeConfigDirectory 'config.yml'
if (-not (Test-Path -LiteralPath $runtimeConfig)) {
    Copy-Item -LiteralPath (Join-Path $projectRoot 'server-config\DropboxAutoResourcePack.config.yml') `
        -Destination $runtimeConfig
    Write-Host "Installed blank Dropbox configuration: $runtimeConfig"
} else {
    Write-Host 'Kept existing Dropbox runtime config (and any refresh token).'
}

Write-Host "DropboxAutoResourcePack 1.1.1 installed from $actualCommit"
