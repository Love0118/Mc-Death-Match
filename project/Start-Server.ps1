[CmdletBinding()]
param(
    [int]$MinMemoryGB = 2,
    [int]$MaxMemoryGB = 4,
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

$java = Get-Command java -ErrorAction Stop
$versionText = (& $java.Source -version 2>&1 | Out-String)
if ($versionText -notmatch 'version "(?<major>\d+)') {
    throw "Unable to determine Java version:`n$versionText"
}
if ([int]$Matches.major -lt 21) {
    throw "Paper 1.21.8 requires Java 21 or newer; detected Java $($Matches.major)"
}

$paperJar = Get-ChildItem -LiteralPath $serverRoot -Filter 'paper-1.21.8-*.jar' -File |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $paperJar) {
    throw "paper-1.21.8-*.jar was not found under $serverRoot"
}

[System.IO.File]::WriteAllText(
    (Join-Path $serverRoot 'eula.txt'),
    "eula=true`r`n",
    [System.Text.UTF8Encoding]::new($false)
)

Push-Location $serverRoot
try {
    & $java.Source "-Xms${MinMemoryGB}G" "-Xmx${MaxMemoryGB}G" -jar $paperJar.FullName --nogui
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
