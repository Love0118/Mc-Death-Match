# Sniper PvP server (Paper 1.21.8)

A small automatic-respawn deathmatch server built around a server-side hitscan rifle.

## Implemented gameplay

- Right click toggles the full-screen scope overlay and narrowed FOV. Right click again to leave the scope;
  firing, reloading or changing away from the rifle also drops it. The overlay has no custom crosshair, so
  only Minecraft's crosshair remains. The rifle does not enter vanilla item-use state, so an unmodified
  1.21.8 client still sends left-click attacks.
- Left click: instant server-side ray trace up to 350 blocks. Player hitboxes receive a 0.20-block targeting
  margin. The first concrete block or player hit wins, so cover cannot be penetrated and no projectile entity
  is created.
- Scoped shots are perfectly accurate. Hip-fire uses a uniform 5% tangent spread (at most about 5
  blocks of offset per 100 blocks of travel). `rifle.horizontal-aim-compensation-degrees` corrects
  horizontal client-side aim bias; positive values move hits right and `/sni reload` applies changes immediately.
- The hitscan trail uses the same white-to-gray dust transition, electric spark and mycelium combination
  as Below the Loop's sniper projectile, sampled every 0.75 blocks and force-sent for clear visibility.
- Hit height selects exact damage: legs 70, body 100 and head 150. Each life starts with 100 health plus
  50 absorption, so a headshot is lethal while a body shot leaves 50 effective health.
- The rifle has a 5-round magazine. Firing immediately drops the scope; the supplied Mosin-Nagant bolt
  sound starts after 14 ticks (0.7 seconds), while the next shot remains on the 30-tick (1.5-second) cycle.
  Pressing Q manually reloads any partially used magazine; after round five it automatically performs a
  90-tick (4.5-second) full reload.
  Fire audio uses Below the Loop's anti-material `mega_fire.ogg` at pitch 0.9; reload audio uses its
  generic firearm reload asset.
- Ammo and real-time reload progress are kept in the rifle item name only; no duplicate action-bar ammo or
  reload line is sent.
- Potion-based Speed and Jump Boost are not used. Arena players receive final movement-speed 0.25,
  final jump-strength 0.72, player scale 1.5 and Glowing.
- Arena players have 100 health, a non-regenerating 50-point absorption shield and unlimited full hunger.
  The bottom HUD displays combined effective health out of 150. Vanilla heart, armor and hunger sprites are
  hidden by the pack. After five seconds without firing or taking a hit, only normal health regenerates by 5
  every second.
- Join and match start restore the rifle, buffs, health, food and Adventure mode.
- Deathmatch respawn is automatic after exactly three seconds. A manually clicked early respawn is held
  in spectator mode until the same deadline, so it cannot bypass the delay.
- Respawns receive one second of invulnerability without Glowing; Glowing returns when protection ends.
- The first player to 40 kills wins. A ten-minute timeout awards first place. Matches remain stopped
  until an operator explicitly starts the next one.
- A Valorant-inspired top HUD places the timer in the center and ranked nickname/kill cards on both sides.
  The full score is also visible in TAB, with no display entities.
- The lower health HUD is the same stepped green/yellow/red bar and Orbitron number treatment used by
  `spear-vs-zombie-main`, ported to the 1.21.8 text shader.
- Each killer gets a private FIFO kill feed. It shows up to three victim-name banners; a fourth removes
  the oldest, and every banner expires after three seconds.
- Every participant also sees a five-entry global kill log in the upper-right HUD. Killer names are `&a`
  green, victim names are `&c` red, and a custom sniper-rifle font glyph separates them. Deaths are never
  written to chat.
- Winners alone hear the supplied victory theme and every non-winner privately hears the defeat theme.
  These use targeted player packets, so nearby players cannot hear somebody else's result sound.
- Match start is title-only and does not write to chat. At match end, the center title contains only aqua
  `VICTORY` or red `DEFEAT`; winner name, kill count and reason remain in the HUD. The original server tick
  rate is saved, changed to 2 TPS for seven real seconds, then restored even when the match is stopped early.
- Forty spawn candidates are distributed across three rings.
- Respawn is weighted toward low-population areas. Nearby players sharply reduce a point's probability,
  and a point used within the last five seconds receives an extra temporary penalty.
- Consecutive kills play the exact supplied, pre-scaled Valorant 1-5 kill sounds only to the killer. The
  standard OGG Vorbis files are preloaded and the private self-emitted cue follows the gunshot transient
  by four ticks. Kill six and above reuse sound 5, death resets to sound 1, and generic hit audio is
  suppressed on lethal shots.

## Arena

`sniper_arena` is generated as a 300x300 gray-concrete-only map. It contains a central raised citadel,
four quadrant plateaus, four cardinal towers, symmetric walls/low cover, edge bunkers and perimeter walls.
The layout is generated once and marked with arena build version 1.

`bukkit.yml` binds the default world to the plugin generator. Do not launch the server once without the
SniperPvp JAR installed, or Paper would create ordinary terrain under the same world name.

## Build

Requirements: Java 21 or newer, Maven, and internet access for the first dependency build.

```powershell
.\project\Build-Server.ps1
```

This command:

1. runs the SniperPvp tests and deploys `server/plugins/SniperPvp-1.0.0.jar`;
2. checks out the `resource-pack-only` implementation at pinned commit
   `e69c6f33023edeb0d704b64562b57c9611cb2b35`, runs its tests, and installs
   `server/plugins/DropboxAutoResourcePack-1.1.0.jar`;
3. builds the format-64 pack and copies it to
   `server/plugins/DropboxAutoResourcePack/resourcepacks/sniper-pvp-1.21.8.zip`.

The supplied `walnut-longline-mk2-low-compact-scope` rifle is already embedded as
`jm:walnut_longline_mk2`. An optional additional base pack can still be merged underneath:

```powershell
.\project\Build-Server.ps1 -BaseResourcePack 'C:\path\to\extra-pack.zip'
```

## Server folder, first start and Dropbox OAuth

The Maven project lives under `project/`; Paper runs only under the sibling `server/` directory.
Build artifacts are copied across by `project/Build-Server.ps1`, so worlds, logs and OAuth secrets never
mix with source files.

```text
Start-Server.bat
```

The user explicitly authorized EULA acceptance for this workspace. The batch launcher writes
`server/eula.txt` with `eula=true`, then starts `server/paper-1.21.8-60.jar` with 2-4 GB on port
25565 by default. The server accepts transfer packets and allows up to 50 simultaneous players.

In the server console or as an operator:

```text
/darp dropbox auth
/darp dropbox finish <returned-code-or-full-redirect-url>
/darp status
```

After authorization, the plugin uploads the local ZIP, downloads it back, verifies SHA-1, metadata and
archive safety, and only then sends it as a required pack to 1.21.8 clients. The refresh token lives in
the ignored runtime file `server/plugins/DropboxAutoResourcePack/config.yml`; never commit or share it.

## Commands

- `/sni start` - reset scores/streaks/timer, spread current arena players, and equip them
- `/sni stop` - stop combat and remove combat effects/rifles
- `/sni give [player]` - issue the rifle
- `/sni spawn` - move to a weighted spawn
- `/sni status` - show match/world/hitscan status
- `/sni reload` - validate and reload gameplay config

`/sniper` and `/snp` remain aliases, but the short operational command is `/sni`.

Most balance values are in `server/plugins/SniperPvp/config.yml` after first start, with source defaults in
`src/main/resources/config.yml`.
