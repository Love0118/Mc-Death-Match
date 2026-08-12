[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
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
    $versionText = (& $candidate -version 2>&1 | Out-String)
    if ($versionText -match '(?:openjdk|java) version "(?<major>\d+)') {
        if ([int]$Matches.major -ge 25) {
            return [System.IO.Path]::GetFullPath($candidate)
        }
    }
}

throw 'Paper 26.2 requires Java 25 or newer. Install Java 25 or set JAVA_HOME to a Java 25 JDK.'
