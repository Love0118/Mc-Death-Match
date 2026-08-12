[CmdletBinding()]
param(
    [int]$MinMemoryGB = 8,
    [int]$MaxMemoryGB = 8,
    [switch]$ValidateOnly
)

$ErrorActionPreference = 'Stop'
$serverRoot = $PSScriptRoot
if ($MinMemoryGB -le 0 -or $MaxMemoryGB -lt $MinMemoryGB) {
    throw 'Memory values must satisfy 0 < MinMemoryGB <= MaxMemoryGB'
}

function Get-JavaVersionText {
    param([string]$Executable)

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $Executable
    $startInfo.Arguments = '-version'
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    try {
        if (-not $process.Start()) {
            return ''
        }
        $standardOutput = $process.StandardOutput.ReadToEnd()
        $standardError = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        return $standardOutput + [Environment]::NewLine + $standardError
    } catch {
        return ''
    } finally {
        $process.Dispose()
    }
}

function Resolve-Java25 {
    $candidates = [System.Collections.Generic.List[string]]::new()
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidates.Add((Join-Path $env:JAVA_HOME 'bin\java.exe'))
    }
    foreach ($pattern in @(
        (Join-Path $env:LOCALAPPDATA 'Programs\Zulu\zulu25*\bin\java.exe'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium\jdk-25*\bin\java.exe'),
        (Join-Path $env:ProgramFiles 'Microsoft\jdk-25*\bin\java.exe'),
        (Join-Path $env:ProgramFiles 'Java\jdk-25*\bin\java.exe'),
        (Join-Path $env:ProgramFiles 'Zulu\zulu-25*\bin\java.exe')
    )) {
        Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending |
            ForEach-Object { $candidates.Add($_.FullName) }
    }
    $pathJava = Get-Command java -ErrorAction SilentlyContinue
    if ($null -ne $pathJava) {
        $candidates.Add($pathJava.Source)
    }
    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            continue
        }
        $versionText = Get-JavaVersionText $candidate
        if ($versionText -match '(?:openjdk|java) version "(?<major>\d+)' -and
            [int]$Matches.major -ge 25) {
            return [System.IO.Path]::GetFullPath($candidate)
        }
    }
    throw 'Paper 26.2 requires Java 25 or newer. Install Java 25 or set JAVA_HOME.'
}

$javaExecutable = Resolve-Java25
$paperJar = Get-ChildItem -LiteralPath $serverRoot -Filter 'paper-26.2-*.jar' -File |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $paperJar) {
    throw "paper-26.2-*.jar was not found under $serverRoot"
}

if ($ValidateOnly) {
    Write-Host "Java 25 validation passed: $javaExecutable" -ForegroundColor Green
    Write-Host "Paper runtime found: $($paperJar.Name)" -ForegroundColor Green
    return
}

[System.IO.File]::WriteAllText(
    (Join-Path $serverRoot 'eula.txt'),
    "eula=true`r`n",
    [System.Text.UTF8Encoding]::new($false)
)

$dropboxDirectory = Join-Path $serverRoot 'plugins\DropboxAutoResourcePack'
$dropboxConfig = Join-Path $dropboxDirectory 'config.yml'
$dropboxExample = Join-Path $dropboxDirectory 'config.example.yml'
if (-not (Test-Path -LiteralPath $dropboxConfig -PathType Leaf) -and
    (Test-Path -LiteralPath $dropboxExample -PathType Leaf)) {
    Copy-Item -LiteralPath $dropboxExample -Destination $dropboxConfig
    Write-Host 'Created private Dropbox config.yml from config.example.yml.' -ForegroundColor Yellow
}

$world = Join-Path $serverRoot 'sniper_arena'
$migrationMarker = Join-Path $serverRoot '.paper-26.2-migration-backup.done'
if ((Test-Path -LiteralPath $world -PathType Container) -and
    -not (Test-Path -LiteralPath $migrationMarker -PathType Leaf)) {
    $backupDirectory = Join-Path $serverRoot 'backups'
    New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $backup = Join-Path $backupDirectory "sniper_arena-before-26.2-$stamp.zip"
    Compress-Archive -LiteralPath $world -DestinationPath $backup -CompressionLevel Optimal
    [System.IO.File]::WriteAllText(
        $migrationMarker,
        "backup=$backup`r`npaper=$($paperJar.Name)`r`ncreated=$(Get-Date -Format o)`r`n",
        [System.Text.UTF8Encoding]::new($false)
    )
    Write-Host "Created mandatory pre-26.2 world backup: $backup" -ForegroundColor Yellow
}

Push-Location $serverRoot
try {
    & $javaExecutable "-Xms${MinMemoryGB}G" "-Xmx${MaxMemoryGB}G" -jar $paperJar.FullName --nogui
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
