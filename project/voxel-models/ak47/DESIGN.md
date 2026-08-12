# AK-47 voxel design contract

The generated three-panel sheet is a visual target. The hand-authored ASCII
blueprint remains the source of truth, with the true-side panel and the open
muzzle taking precedence over AI drift in the alternate views.

## Semantic parts

| Part | Grid extent | Structural contact or negative space |
| --- | --- | --- |
| Walnut fixed stock | x=6..22, y=17..24 | Four-neighbor contact with the rear receiver |
| Stamped receiver and dust cover | x=20..43, y=19..25 | Stable proportion anchor for all attached parts |
| Walnut pistol grip | x=20..26, y=11..18 | Touches the receiver; does not fill the trigger opening |
| Trigger and guard | x=27..33, y=15..18 | Hollow center remains visible |
| Curved magazine | x=34..54, y=0..18 | Locks into the receiver and sweeps forward at the lower tip |
| Walnut handguard | x=43..56, y=20..23 | Touches both receiver and exposed barrel |
| Gas tube | x=44..62, center y=25 | Explicit X-axis five-cell cross-section |
| Barrel | x=56..67, center y=21 | Explicit X-axis 3x3 ring with a through-bore |
| Muzzle device | x=68..71, center y=21 | Explicit faceted 5x5 ring with the same open bore |
| Front sight | x=61..66, y=23..27 | Bridges the barrel and gas system |
| Cleaning rod | x=54..67, y=19 | Thin steel depth tier beneath the barrel |

The main receiver spans 24 cells. The complete rifle spans 66 of 72 columns,
while the curved magazine drops 19 cells below its receiver contact. Wood,
body metal, thin steel, and raised rivets use separate depth roles.

## Generated reference prompt

Built-in image generation was used with a flat `#00FF00` background. The final
prompt required exactly three equal panels in this order: true left-side,
muzzle/front orthographic, and top orthographic. It preserved a classic AK-47
with blued steel, reddish walnut furniture, a strongly curved magazine, gas
tube, sights, trigger-guard opening, and an open muzzle; it prohibited scopes,
rails, slings, bayonets, suppressors, hands, text, shadows, micro-voxels, smooth
shading, and details smaller than a cell on the declared 72x28 side grid.

`reference_multiview_chroma.png` is the generated source and
`reference_multiview_alpha.png` is its locally validated chroma-key removal.
The generated concept declared a 72x28 grid; visual review then expanded the
authored source to 72x32 so the curved magazine could keep coarse one-cell
steps without compressing its silhouette. `reference_multiview_axis_order.png`
reorders the panels to muzzle, side, top. The renderer's top camera needed a
90-degree roll-only semantic correction in
`preview/ak47_comparison_turntable_semantic.png`; geometry was not stretched.

Recompile after editing the source:

```powershell
$skill = "$env:USERPROFILE\.codex\skills\minecraft-reference-to-voxel"
python "$skill\scripts\compile_voxel_item.py" `
  --project .\voxel-models\ak47
python "$skill\scripts\validate_axis_geometry.py" `
  --project .\voxel-models\ak47
```
