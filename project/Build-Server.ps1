[CmdletBinding()]
param(
    [string]$BaseResourcePack = '',
    [switch]$SkipDropboxBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
$serverRoot = Join-Path $workspaceRoot 'server'
$pluginsDirectory = Join-Path $serverRoot 'plugins'
$javaExecutable = & (Join-Path $projectRoot 'tools\Resolve-Java25.ps1')
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
$env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaExecutable)
$env:Path = (Split-Path -Parent $javaExecutable) + ';' + $env:Path
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'tools\Generate-VisualAssets.ps1')

    & (Join-Path $projectRoot 'tools\Install-ServerRuntime.ps1') -ServerRoot $serverRoot

    mvn clean verify
    if ($LASTEXITCODE -ne 0) {
        throw 'SniperPvp build or tests failed'
    }

    New-Item -ItemType Directory -Force -Path $pluginsDirectory | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'target\SniperPvp-1.0.0.jar') `
        -Destination (Join-Path $pluginsDirectory 'SniperPvp-1.0.0.jar') -Force

    foreach ($configName in @('server.properties', 'bukkit.yml', 'Start-Server.ps1')) {
        $runtimeConfig = Join-Path $serverRoot $configName
        if ($configName -eq 'Start-Server.ps1' -or -not (Test-Path -LiteralPath $runtimeConfig)) {
            Copy-Item -LiteralPath (Join-Path $projectRoot "server-config\$configName") `
                -Destination $runtimeConfig
        }
    }

    if (-not $SkipDropboxBuild) {
        & (Join-Path $projectRoot 'tools\Install-DropboxPlugin.ps1') -ServerRoot $serverRoot
    }

    $packArguments = @{}
    if (-not [string]::IsNullOrWhiteSpace($BaseResourcePack)) {
        $packArguments.BasePack = $BaseResourcePack
    }
    $packArguments.ServerRoot = $serverRoot
    & (Join-Path $projectRoot 'tools\Build-ResourcePack.ps1') @packArguments

    Write-Host "Server deployment complete: $serverRoot" -ForegroundColor Green
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
