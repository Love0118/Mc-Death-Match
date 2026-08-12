# Sniper PvP server pack

Paper 1.21.8 deathmatch server pack. Java 21 or newer is required.

This is a standalone Paper server: Velocity, BungeeCord and PROXY protocol forwarding are disabled.

## Start

1. Run `Start-Server.bat`.
2. Complete Dropbox authorization from the server console:
   - `/darp dropbox auth`
   - `/darp dropbox finish <returned-code-or-full-redirect-url>`
   - `/darp status`
3. Join with Minecraft 1.21.8 and run `/sni start` as an operator or from the console.

The launcher accepts the EULA, creates a private Dropbox configuration from
`plugins/DropboxAutoResourcePack/config.example.yml`, and starts on port 25565 with a maximum of 50 players.

## Deliberately excluded

- `ops.json`, user cache, whitelist and ban lists
- world `playerdata`, statistics, advancements and scoreboard data
- Dropbox OAuth credentials and refresh tokens
- logs, backups, Paper libraries/download caches and remapped plugin caches

Paper downloads its required libraries on the first launch. The prebuilt arena, plugins and 1.21.8 resource pack
are included.
