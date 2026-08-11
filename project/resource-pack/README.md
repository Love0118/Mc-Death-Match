# Sniper PvP resource-pack overlay

This directory is the complete server-owned 1.21.8 pack. `tools/Build-ResourcePack.ps1` can optionally
merge another pack underneath it, then produces `dist/sniper-pvp-1.21.8.zip` and the runtime Dropbox copy.

## Already included

- `sniperpvp:kill.1` through `sniperpvp:kill.5`
- standard mono 48 kHz OGG Vorbis audio converted from the five supplied MP3 files and preloaded by Minecraft
- waveform amplitude multiplied by exactly `0.4` before OGG encoding (about `-7.96 dB`)
- pack format `64`, restricted to Minecraft 1.21.8
- the supplied `jm:walnut_longline_mk2` item model, palette and item definition
- Valorant-inspired nickname/kill cards, centered timer, private three-entry banners and a global upper-right kill log
- transparent native white BossBar sprites so only the custom panels are visible
- the reference stepped bottom health bar and Orbitron digits, plus transparent vanilla health/hunger sprites
- a 512x288 (16:9) scope mask without a custom crosshair
- a custom sniper-rifle font glyph for the global kill log
- vanilla spyglass zoom-in/zoom-out cues for the custom hold scope
- the user-supplied Mosin-Nagant bolt-action sound
- private match victory and defeat sound events

The plugin clamps every streak sound tier with `min(streak, 5)`, so six kills and above keep playing
`sniperpvp:kill.5`.

## Rifle item contract

The rifle is based on `minecraft:paper` so holding the custom scope does not suppress vanilla left-click
attack packets. Paper sets the 1.21.8 `minecraft:item_model` component to `jm:walnut_longline_mk2`;
custom-model-data float `1001` is retained as a fallback marker.

To merge an unrelated base pack:

```powershell
.\Build-Server.ps1 -BaseResourcePack 'C:\path\to\rifle-pack.zip'
```

The merge preserves a pre-existing `assets/sniperpvp/sounds.json` and overlays all server-owned events.
