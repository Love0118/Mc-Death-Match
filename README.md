# Sniper PvP server pack

Paper 26.2 deathmatch server pack. Java 25 or newer is required. ViaVersion and ViaBackwards allow
Minecraft 1.21.8 through 26.2 clients; older and newer protocols are blocked explicitly.

This is a standalone Paper server: Velocity, BungeeCord and PROXY protocol forwarding are disabled.

## Start

1. Install Java 25 and run `Start-Server.bat`.
2. Complete Dropbox authorization from the server console:
   - `/darp dropbox auth`
   - `/darp dropbox finish <returned-code-or-full-redirect-url>`
   - `/darp status`
3. Join with any supported client and run `/sni start` as an operator or from the console.

The launcher accepts the EULA, creates a private Dropbox configuration from
`plugins/DropboxAutoResourcePack/config.example.yml`, and starts on port 25565 with a maximum of 50 players.
Before Paper 26.2 opens the bundled older arena for the first time, it creates a ZIP under `backups/` and
writes a one-time migration marker. Do not downgrade a world after Paper 26.2 has converted it.

## Client resource packs

DropboxAutoResourcePack 1.1.1 reads the player's actual protocol and sends one required pack:

| Client | Protocol | Pack format | ZIP |
| --- | ---: | ---: | --- |
| 1.21.8 | 772 | 64 | `sniper-pvp-1.21.8.zip` |
| 1.21.9-1.21.10 | 773 | 69 | `sniper-pvp-1.21.9-1.21.10.zip` |
| 1.21.11 | 774 | 75 | `sniper-pvp-1.21.11.zip` |
| 26.1-26.1.2 | 775 | 84 | `sniper-pvp-26.1.x.zip` |
| 26.2 | 776 | 88 | `sniper-pvp-26.2.zip` |

NoChatReports 2.7.8 is included because 2.7.7 does not load its NMS provider on Paper 26.2.

## Deliberately excluded

- `ops.json`, user cache, whitelist and ban lists
- world `playerdata`, statistics, advancements and scoreboard data
- Dropbox OAuth credentials and refresh tokens
- logs, backups, Paper libraries/download caches and remapped plugin caches

Paper downloads its required libraries on the first launch. The prebuilt arena, plugins and all five resource packs
are included. OAuth secrets, operators and per-player data are not included.
