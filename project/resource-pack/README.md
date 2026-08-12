# Sniper PvP resource-pack overlay

This directory contains the common server-owned assets. `tools/Build-ResourcePack.ps1` combines them with
the metadata and core-text-shader variants under `resource-pack-variants/`, then produces five version-aware
ZIPs and their runtime Dropbox copies.

`tools/Sync-VoxelModels.ps1` first copies every compiled model under `voxel-models/` into this common asset
tree. Edit the ASCII blueprint/config, re-run the voxel compiler, and build the packs; do not hand-edit the
generated Minecraft JSON.

## Already included

- `sniperpvp:kill.1` through `sniperpvp:kill.5`
- standard mono 48 kHz OGG Vorbis audio converted from the five supplied MP3 files and preloaded by Minecraft
- source waveform volume preserved during OGG encoding
- pack formats `64`, `69`, `75`, `84` and `88` for Minecraft 1.21.8 through 26.2
- hand-authored `jm:ak47` and supplied `jm:walnut_longline_mk2` item models,
  palettes and item definitions
- separate horizontal third-person rifle transforms without changing the established first-person view
- Valorant-inspired nickname/kill cards, centered timer, private three-entry banners and a global upper-right kill log
- transparent native white BossBar sprites so only the custom panels are visible
- the reference stepped bottom health bar and Orbitron digits, plus transparent vanilla health/hunger sprites
- a 512x288 (16:9) scope mask without a custom crosshair
- a custom sniper-rifle font glyph for the global kill log
- vanilla spyglass zoom-in/zoom-out cues for the custom toggle scope
- the user-supplied Mosin-Nagant bolt-action sound
- private match victory and defeat sound events

## Version matrix

| Clients | Protocol | Pack format | Text shader |
| --- | ---: | ---: | --- |
| 1.21.8 | 772 | 64.0 | `core/rendertype_text.vsh`, GLSL 150 |
| 1.21.9, 1.21.10 | 773 | 69.0 | `core/rendertype_text.vsh`, GLSL 330 |
| 1.21.11 | 774 | 75.0 | `core/rendertype_text.vsh`, GLSL 330 |
| 26.1, 26.1.1, 26.1.2 | 775 | 84.0 | `core/rendertype_text.vsh` with `sample_lightmap` |
| 26.2 | 776 | 88.0 | unified `core/text.vsh` with `IS_GUI` handling |

Versions sharing one handshake protocol must share one ZIP because the server cannot distinguish them at
login. The build matrix is `resource-pack-variants/matrix.json`.

The plugin clamps every streak sound tier with `min(streak, 5)`, so six kills and above keep playing
`sniperpvp:kill.5`.

## Rifle item contract

The rifles are based on `minecraft:paper` so holding the custom scope does not suppress vanilla left-click
attack packets. Paper sets the `minecraft:item_model` component to the selected catalog model; the gameplay
default remains `jm:walnut_longline_mk2`, while `/sni debug` can issue `jm:ak47`. Custom-model-data float
`1001` is retained as a fallback marker.

To merge an unrelated base pack:

```powershell
.\Build-Server.ps1 -BaseResourcePack 'C:\path\to\rifle-pack.zip'
```

The merge preserves a pre-existing `assets/sniperpvp/sounds.json` and overlays all server-owned events.
