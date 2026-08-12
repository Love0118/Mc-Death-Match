# Asset sources

- Rifle model and palette: user-supplied
  `walnut-longline-mk2-low-compact-scope-20260811-222044.zip`.
- AK-47 model and palette: hand-authored 72x32 ASCII blueprint under
  `voxel-models/ak47`, compiled with the `minecraft-reference-to-voxel` workflow.
  Its generated multiview sheet is a visual reference; the ASCII blueprint is
  the model source of truth.
- Kill streak audio: user-supplied `valorant-1-kill.mp3` through `valorant-5-kills.mp3`.
  Each source was converted without gain reduction, downmixed to mono, resampled to 48 kHz and encoded
  as OGG Vorbis for Minecraft playback.
- Bolt-action audio: user-supplied
  `freesound_community-mosin-nagant-bolt-fast-104031.mp3`, converted to OGG without gain changes.
- Match defeat source requested by the user:
  <https://youtu.be/jCQn-By6Jg8>
- Match victory source requested by the user:
  <https://youtu.be/nQDHLQts884>
- Bottom health-bar and digit atlases are copied from the user-designated `spear-vs-zombie-main` HUD.
  The digit atlas derives from Orbitron SemiBold and its OFL license is shipped beside the font definition.
- HUD panels, the crosshair-free scope mask, the kill-log rifle glyph and hidden vanilla HUD sprites are
  generated specifically for this pack by `tools/Generate-VisualAssets.ps1`.

Only OGG Vorbis files required by Minecraft are shipped in the resource pack. Temporary MP3 conversion
outputs are kept under the ignored `.build` directory.
