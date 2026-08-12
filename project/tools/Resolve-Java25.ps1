[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

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
    if ($versionText -match '(?:openjdk|java) version "(?<major>\d+)') {
        if ([int]$Matches.major -ge 25) {
            return [System.IO.Path]::GetFullPath($candidate)
        }
    }
}

throw 'Paper 26.2 requires Java 25 or newer. Install Java 25 or set JAVA_HOME to a Java 25 JDK.'
