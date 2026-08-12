[CmdletBinding()]
param(
    [int]$Port = 25599,
    [int]$StartupTimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
$serverRoot = Join-Path $workspaceRoot 'server'
$buildRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot '.build'))
$stageRoot = [System.IO.Path]::GetFullPath((Join-Path $buildRoot 'paper-26.2-smoke'))
if (-not $stageRoot.StartsWith($buildRoot + [System.IO.Path]::DirectorySeparatorChar,
        [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to recreate a smoke-test directory outside $buildRoot"
}
if (Test-Path -LiteralPath $stageRoot) {
    Remove-Item -LiteralPath $stageRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $stageRoot, (Join-Path $stageRoot 'plugins') | Out-Null

function Copy-RequiredFile {
    param([string]$Source, [string]$Destination)
    if (-not (Test-Path -LiteralPath $Source -PathType Leaf)) {
        throw "Required smoke-test input is missing: $Source"
    }
    $parent = Split-Path -Parent $Destination
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    Copy-Item -LiteralPath $Source -Destination $Destination -Force
}

$paper = Get-ChildItem -LiteralPath $serverRoot -Filter 'paper-26.2-*.jar' -File |
    Sort-Object Name -Descending | Select-Object -First 1
if ($null -eq $paper) {
    throw 'The Paper 26.2 runtime must be installed before the smoke test.'
}
Copy-RequiredFile $paper.FullName (Join-Path $stageRoot $paper.Name)
Copy-RequiredFile (Join-Path $projectRoot 'target\SniperPvp-1.0.0.jar') `
    (Join-Path $stageRoot 'plugins\SniperPvp-1.0.0.jar')

foreach ($pluginName in @(
    'DropboxAutoResourcePack-1.1.1.jar',
    'ViaVersion-5.11.0.jar',
    'ViaBackwards-5.11.0.jar',
    'NoChatReports-2.7.8.jar',
    'kakc.jar'
)) {
    Copy-RequiredFile (Join-Path $serverRoot "plugins\$pluginName") `
        (Join-Path $stageRoot "plugins\$pluginName")
}

Copy-RequiredFile (Join-Path $projectRoot 'server-config\bukkit.yml') (Join-Path $stageRoot 'bukkit.yml')
$properties = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'server-config\server.properties'))
$properties = [regex]::Replace($properties, '(?m)^server-port=.*$', "server-port=$Port")
$properties = [regex]::Replace($properties, '(?m)^online-mode=.*$', 'online-mode=false')
$properties = [regex]::Replace($properties, '(?m)^enforce-secure-profile=.*$', 'enforce-secure-profile=false')
$properties = [regex]::Replace($properties, '(?m)^network-compression-threshold=.*$', 'network-compression-threshold=-1')
[System.IO.File]::WriteAllText(
    (Join-Path $stageRoot 'server.properties'),
    $properties,
    [System.Text.UTF8Encoding]::new($false)
)
[System.IO.File]::WriteAllText(
    (Join-Path $stageRoot 'eula.txt'),
    "eula=true`r`n",
    [System.Text.UTF8Encoding]::new($false)
)

$dropboxDirectory = Join-Path $stageRoot 'plugins\DropboxAutoResourcePack'
New-Item -ItemType Directory -Force -Path (Join-Path $dropboxDirectory 'resourcepacks') | Out-Null
$dropboxConfig = [System.IO.File]::ReadAllText(
    (Join-Path $projectRoot 'server-config\DropboxAutoResourcePack.config.yml')
)
$dropboxConfig = $dropboxConfig.Replace('publish-on-startup: true', 'publish-on-startup: false')
[System.IO.File]::WriteAllText(
    (Join-Path $dropboxDirectory 'config.yml'),
    $dropboxConfig,
    [System.Text.UTF8Encoding]::new($false)
)
Get-ChildItem -LiteralPath (Join-Path $serverRoot 'plugins\DropboxAutoResourcePack\resourcepacks') `
    -Filter 'sniper-pvp-*.zip' -File | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $dropboxDirectory 'resourcepacks') -Force
    }

foreach ($viaPlugin in @('ViaVersion', 'ViaBackwards')) {
    $destinationDirectory = Join-Path $stageRoot "plugins\$viaPlugin"
    New-Item -ItemType Directory -Force -Path $destinationDirectory | Out-Null
    Copy-RequiredFile (Join-Path $projectRoot "server-config\$viaPlugin.config.yml") `
        (Join-Path $destinationDirectory 'config.yml')
}
if (Test-Path -LiteralPath (Join-Path $serverRoot 'plugins\NoChatReports\config.yml')) {
    Copy-RequiredFile (Join-Path $serverRoot 'plugins\NoChatReports\config.yml') `
        (Join-Path $stageRoot 'plugins\NoChatReports\config.yml')
}
Copy-RequiredFile (Join-Path $projectRoot 'src\main\resources\config.yml') `
    (Join-Path $stageRoot 'plugins\SniperPvp\config.yml')
$sourceWorld = Join-Path $serverRoot 'sniper_arena'
if (-not (Test-Path -LiteralPath $sourceWorld -PathType Container)) {
    throw "Existing arena is missing: $sourceWorld"
}
Copy-Item -LiteralPath $sourceWorld -Destination (Join-Path $stageRoot 'sniper_arena') -Recurse

if ($null -eq ('SniperPvpSmoke.MinecraftProtocolProbe' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Net.Sockets;
using System.Text;

namespace SniperPvpSmoke {
    public static class MinecraftProtocolProbe {
        private static void WriteVarInt(Stream stream, int value) {
            uint remaining = unchecked((uint)value);
            do {
                byte current = (byte)(remaining & 0x7f);
                remaining >>= 7;
                if (remaining != 0) current |= 0x80;
                stream.WriteByte(current);
            } while (remaining != 0);
        }

        private static int ReadVarInt(Stream stream) {
            int value = 0;
            int position = 0;
            while (true) {
                int current = stream.ReadByte();
                if (current < 0) throw new EndOfStreamException("Connection closed while reading a VarInt.");
                value |= (current & 0x7f) << position;
                if ((current & 0x80) == 0) return value;
                position += 7;
                if (position >= 35) throw new InvalidDataException("Invalid Minecraft VarInt.");
            }
        }

        private static void WriteString(Stream stream, string value) {
            byte[] bytes = Encoding.UTF8.GetBytes(value);
            WriteVarInt(stream, bytes.Length);
            stream.Write(bytes, 0, bytes.Length);
        }

        private static byte[] ReadExact(Stream stream, int length) {
            byte[] bytes = new byte[length];
            int offset = 0;
            while (offset < length) {
                int read = stream.Read(bytes, offset, length - offset);
                if (read <= 0) throw new EndOfStreamException("Connection closed while reading a packet.");
                offset += read;
            }
            return bytes;
        }

        private static TcpClient Connect(int port) {
            var client = new TcpClient();
            var task = client.ConnectAsync("127.0.0.1", port);
            if (!task.Wait(2000)) {
                client.Dispose();
                throw new TimeoutException("Connection timed out.");
            }
            client.ReceiveTimeout = 3000;
            client.SendTimeout = 3000;
            return client;
        }

        private static void WriteHandshake(Stream stream, int port, int protocol, int nextState) {
            using var packet = new MemoryStream();
            WriteVarInt(packet, 0);
            WriteVarInt(packet, protocol);
            WriteString(packet, "localhost");
            packet.WriteByte((byte)((port >> 8) & 0xff));
            packet.WriteByte((byte)(port & 0xff));
            WriteVarInt(packet, nextState);
            WriteVarInt(stream, checked((int)packet.Length));
            packet.Position = 0;
            packet.CopyTo(stream);
            stream.Flush();
        }

        public static string QueryStatus(int port, int protocol) {
            using var client = Connect(port);
            Stream stream = client.GetStream();
            WriteHandshake(stream, port, protocol, 1);
            stream.WriteByte(1);
            stream.WriteByte(0);
            stream.Flush();
            ReadVarInt(stream);
            int packetId = ReadVarInt(stream);
            if (packetId != 0) throw new InvalidDataException("Unexpected status packet id " + packetId);
            int jsonLength = ReadVarInt(stream);
            return Encoding.UTF8.GetString(ReadExact(stream, jsonLength));
        }

        public static int ProbeLogin(int port, int protocol) {
            using var client = Connect(port);
            Stream stream = client.GetStream();
            WriteHandshake(stream, port, protocol, 2);
            using var packet = new MemoryStream();
            WriteVarInt(packet, 0);
            WriteString(packet, "Probe" + protocol);
            byte[] uuid = Guid.NewGuid().ToByteArray();
            packet.Write(uuid, 0, uuid.Length);
            WriteVarInt(stream, checked((int)packet.Length));
            packet.Position = 0;
            packet.CopyTo(stream);
            stream.Flush();
            // The minimal probe deliberately stops before the modern configuration phase. Paper's
            // authenticated UUID log marker is checked separately for every protocol.
            System.Threading.Thread.Sleep(250);
            return 1;
        }
    }
}
'@
}

function Get-MinecraftStatus {
    param([int]$Protocol)
    return [SniperPvpSmoke.MinecraftProtocolProbe]::QueryStatus($Port, $Protocol) | ConvertFrom-Json
}

$javaExecutable = & (Join-Path $PSScriptRoot 'Resolve-Java25.ps1')
$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $javaExecutable
$startInfo.WorkingDirectory = $stageRoot
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
foreach ($argument in @('-Xms1G', '-Xmx2G', '-jar', $paper.Name, '--nogui')) {
    $startInfo.ArgumentList.Add($argument)
}
$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$started = $process.Start()
if (-not $started) {
    throw 'Paper smoke-test process did not start.'
}
$stdoutTask = $process.StandardOutput.ReadToEndAsync()
$stderrTask = $process.StandardError.ReadToEndAsync()
$statuses = @{}
$loginPackets = @{}
$deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
try {
    while ([DateTime]::UtcNow -lt $deadline -and -not $process.HasExited) {
        foreach ($protocol in 772..776) {
            if ($statuses.ContainsKey($protocol)) {
                continue
            }
            try {
                $status = Get-MinecraftStatus $protocol
                $loginPacket = [SniperPvpSmoke.MinecraftProtocolProbe]::ProbeLogin($Port, $protocol)
                $statuses[$protocol] = $status
                $loginPackets[$protocol] = $loginPacket
            } catch {
                # Paper is still starting or Via has not registered yet.
            }
        }
        if ($statuses.Count -eq 5) {
            break
        }
        Start-Sleep -Milliseconds 500
    }
    if ($process.HasExited) {
        throw "Paper exited during startup with code $($process.ExitCode)."
    }
    if ($statuses.Count -ne 5) {
        throw "Only $($statuses.Count) of five supported protocols answered before timeout."
    }
    $process.StandardInput.WriteLine('plugins')
    $process.StandardInput.WriteLine('viaversion list')
    Start-Sleep -Seconds 1
    $process.StandardInput.WriteLine('stop')
    if (-not $process.WaitForExit(30000)) {
        throw 'Paper did not stop within 30 seconds.'
    }
} finally {
    if (-not $process.HasExited) {
        $process.Kill($true)
        $process.WaitForExit()
    }
}

$stdout = $stdoutTask.GetAwaiter().GetResult()
$stderr = $stderrTask.GetAwaiter().GetResult()
$combinedLog = $stdout + [Environment]::NewLine + $stderr
[System.IO.File]::WriteAllText(
    (Join-Path $stageRoot 'smoke-console.log'),
    $combinedLog,
    [System.Text.UTF8Encoding]::new($false)
)
foreach ($requiredPattern in @(
    'Done \(',
    'SniperPvp',
    'DropboxAutoResourcePack',
    'ViaVersion',
    'ViaBackwards',
    'NoChatReports'
)) {
    if ($combinedLog -notmatch $requiredPattern) {
        throw "Smoke log is missing required marker: $requiredPattern"
    }
}
foreach ($protocol in 772..776) {
    if ($combinedLog -notmatch "UUID of player Probe$protocol") {
        throw "Protocol $protocol did not reach the Paper login pipeline."
    }
}
if ($combinedLog -match '(?im)^.*(?:Could not load|Could not enable|Error occurred while enabling|UnsupportedClassVersionError).*$') {
    throw "A plugin compatibility failure was found in $stageRoot\smoke-console.log"
}

$results = foreach ($entry in $statuses.GetEnumerator() | Sort-Object Key) {
    [pscustomobject]@{
        Protocol = $entry.Key
        ServerProtocol = $entry.Value.version.protocol
        VersionName = $entry.Value.version.name
        LoginPacket = $loginPackets[$entry.Key]
        Motd = $entry.Value.description.text
    }
}
$results | Format-Table -AutoSize
Write-Host "Paper 26.2 migration smoke test passed and stopped cleanly: $stageRoot" -ForegroundColor Green
