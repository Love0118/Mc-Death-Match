[CmdletBinding()]
param(
    [int]$MinMemoryGB = 8,
    [int]$MaxMemoryGB = 8,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
$serverRoot = Join-Path $workspaceRoot 'server'

if ($MinMemoryGB -le 0 -or $MaxMemoryGB -lt $MinMemoryGB) {
    throw 'Memory values must satisfy 0 < MinMemoryGB <= MaxMemoryGB'
}

if (-not $SkipBuild) {
    & (Join-Path $projectRoot 'Build-Server.ps1')
}

& (Join-Path $serverRoot 'Start-Server.ps1') -MinMemoryGB $MinMemoryGB -MaxMemoryGB $MaxMemoryGB
exit $LASTEXITCODE
