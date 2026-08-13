# Sniper PvP server (Paper 26.2, clients 1.21.8-26.2)

A small automatic-respawn deathmatch server built around a server-side hitscan rifle.

## Implemented gameplay

- Right click toggles the full-screen scope overlay and narrowed FOV. Right click again to leave the scope.
  Firing keeps the scope while rounds remain; an empty magazine, manual reload or changing away from the rifle
  drops it. The overlay has no custom crosshair, so
  only Minecraft's crosshair remains. The rifle does not enter vanilla item-use state, so an unmodified
  supported client still sends left-click attacks.
- Left click: instant server-side ray trace up to 350 blocks. Player hitboxes receive a 0.20-block targeting
  margin. The first concrete block or player hit wins, so cover cannot be penetrated and no projectile entity
  is created. The vanilla melee swing is not broadcast to other players, and the shooter's unavoidable local
  client prediction is shortened from six ticks to one tick with a hidden effect.
- Scoped shots are perfectly accurate. Hip-fire uses a uniform 5% tangent spread (at most about 5
  blocks of offset per 100 blocks of travel). `rifle.horizontal-aim-offset-blocks` shifts the parallel
  hitscan ray by a fixed amount; positive values move it right and `/sni reload` applies changes immediately.
  This correction affects hit detection only; the visible tracer always starts at the unshifted eye line.
- The hitscan trail uses the same white-to-gray dust transition, electric spark and mycelium combination
  as Below the Loop's sniper projectile, sampled every 0.75 blocks and force-sent for clear visibility.
- Hit height selects exact damage: legs 70, body 100 and head 150. Each life starts with 100 health plus
  50 absorption, so a headshot is lethal while a body shot leaves 50 effective health.
- The rifle has a 5-round magazine. The supplied Mosin-Nagant bolt
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
  `spear-vs-zombie-main`, ported to each supported client's text shader interface.
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
- Consecutive kills play the exact supplied, full-volume Valorant 1-5 kill sounds only to the killer. The
  standard OGG Vorbis files are preloaded and the private self-emitted cue follows the gunshot transient
  by four ticks. Kill six and above reuse sound 5, death resets to sound 1, and the private hit-confirmation
  cue plays only on headshots, including lethal headshots.

## Arena

`sniper_arena` is generated as a 300x300 gray-concrete map with slime-block jump pads. Layout version 3
removes the scattered stepped towers. Each side has one continuous Y=72 second-floor deck running nearly
the full map length, with a Y=80 third-floor deck over its middle section. Wide end and side supports leave
usable ground routes beneath the decks. The center uses a single low platform, broad stairs, long cover
walls and symmetric ground cover instead of a tall central citadel.

Eight 3x3 jump pads connect ground to the side decks and the side decks to their third floors. Ground pads
replace the Y=64 floor blocks instead of sitting one block above them. Pad activation only replaces vertical
velocity and preserves the player's current horizontal velocity and normal air control. Dead and spectator
players cannot activate pads. When an older arena build marker is found, startup restores the gray Y=64
floor, clears the old layout from Y=65 through Y=96 and constructs version 3. The rebuild is a one-time
operation on the first restart with the updated plugin.

`bukkit.yml` binds the default world to the plugin generator. Do not launch the server once without the
SniperPvp JAR installed, or Paper would create ordinary terrain under the same world name.

## Build

Requirements: Java 25 or newer, Maven, and internet access for the first dependency build.

```powershell
.\project\Build-Server.ps1
```

This command:

1. installs the pinned Paper 26.2 build, ViaVersion/ViaBackwards 5.11.0 and NoChatReports 2.7.8 after
   checking every SHA-256;
2. runs the SniperPvp tests and deploys `server/plugins/SniperPvp-1.0.0.jar`;
3. checks out the `resource-pack-only` implementation at pinned commit
   `172643a4483306b2af1fff237309e4d800dea0f2`, runs its normal and Paper 26.2 compatibility tests,
   and installs `server/plugins/DropboxAutoResourcePack-1.1.1.jar`;
4. builds five deterministic resource-pack ZIPs for the client protocol families below and copies them to
   `server/plugins/DropboxAutoResourcePack/resourcepacks/`.

| Client version | Protocol | Resource-pack format | Built ZIP |
| --- | ---: | ---: | --- |
| 1.21.8 | 772 | 64 | `sniper-pvp-1.21.8.zip` |
| 1.21.9-1.21.10 | 773 | 69 | `sniper-pvp-1.21.9-1.21.10.zip` |
| 1.21.11 | 774 | 75 | `sniper-pvp-1.21.11.zip` |
| 26.1, 26.1.1, 26.1.2 | 775 | 84 | `sniper-pvp-26.1.x.zip` |
| 26.2 | 776 | 88 | `sniper-pvp-26.2.zip` |

The 1.21.8 pack keeps the legacy text shader and metadata schema. Later packs use their matching
versioned metadata, and 26.2 uses the unified `text.vsh` entry point. A pack is never reused across an
incompatible shader or metadata boundary.

The hand-authored AK-47 is embedded as `jm:ak47`. The supplied
`walnut-longline-mk2-low-compact-scope` remains the gameplay default as `jm:walnut_longline_mk2`, and both
appear in the `/sni debug` model-selection GUI. An optional additional base pack can still be merged underneath:

```powershell
.\project\Build-Server.ps1 -BaseResourcePack 'C:\path\to\extra-pack.zip'
```

## Server folder, first start and Dropbox OAuth

The Maven project lives under `project/`; Paper runs only under the sibling `server/` directory.
Build artifacts are copied across by `project/Build-Server.ps1`, so worlds, logs and OAuth secrets never
mix with source files.

NoChatReports is upgraded from 2.7.7 to the official 2.7.8 release, whose 26.2 NMS provider is loaded during
the migration smoke test.

```text
Start-Server.bat
```

The user explicitly authorized EULA acceptance for this workspace. The batch launcher writes
`server/eula.txt` with `eula=true`, then starts `server/paper-26.2-112.jar` with 8 GB on port 25565 by
default. The server accepts transfer packets and allows up to 50 simultaneous players. The launcher makes a
one-time ZIP backup before Paper 26.2 opens an existing `sniper_arena`; do not downgrade that migrated world.

In the server console or as an operator:

```text
/darp dropbox auth
/darp dropbox finish <returned-code-or-full-redirect-url>
/darp status
```

After authorization, the plugin uploads all five local ZIPs, downloads them back, verifies SHA-1, metadata and
archive safety, and only then selects the required pack from the joining player's client protocol. The refresh token lives in
the ignored runtime file `server/plugins/DropboxAutoResourcePack/config.yml`; never commit or share it.

## Commands

- `/sni start` - reset scores/streaks/timer, spread current arena players, and equip them
- `/sni stop` - stop combat and remove combat effects/rifles
- `/sni give [player]` - issue the rifle
- `/sni debug` - open the paginated gun-model GUI and receive only the selected rifle
- `/sni spawn` - move to a weighted spawn
- `/sni status` - show match/world/hitscan status
- `/sni reload` - validate and reload gameplay config

`/sniper` and `/snp` remain aliases, but the short operational command is `/sni`.

`/sni debug` is catalog-driven and paginates up to 45 models per page. To add another selectable gun,
place its item definition, model JSON and palette PNG under one namespace in `resource-pack/assets`, then add
its `id`, `display-name`, `item-model` and `custom-model-data` under `debug.rifle-models` in `config.yml`.
The build tests reject catalog entries whose three pack assets are incomplete.

Most balance values are in `server/plugins/SniperPvp/config.yml` after first start, with source defaults in
`src/main/resources/config.yml`.
