# AK-47 voxel design contract

The generated multiview sheet is the visual target. The hand-authored ASCII
blueprint remains the source of truth, with the restored full true-side subject
and the open muzzle geometry taking precedence.

## Semantic parts

| Part | Grid extent | Structural contact or negative space |
| --- | --- | --- |
| Walnut fixed stock | x=3..19, y=13..25 | Four-neighbor contact with the rear receiver |
| Stamped receiver and dust cover | x=19..43, y=21..29 | Stable proportion anchor for all attached parts |
| Walnut pistol grip | x=19..26, y=9..21 | Thirteen-cell drop; touches receiver without filling trigger opening |
| Trigger and guard | x=27..31, y=18..20 | Hollow center remains visible |
| Curved magazine | x=30..44, y=2..21 | Locks into receiver and follows the restored side silhouette |
| Walnut handguard | x=42..52, y=21..28 | Touches receiver and front metalwork |
| Gas tube | x=53..59, center y=26 | Explicit X-axis five-cell cross-section |
| Barrel | x=53..66, center y=22 | Explicit X-axis 3x3 ring with a through-bore |
| Muzzle device | x=67..68, center y=22 | Compact 3x3 ring continuing the real bore |
| Front sight | x=64..66, y=24..29 | Matches the restored reference tower |
| Cleaning rod | x=54..67, y=19 | Thin steel tier below the barrel, fixed by two bands |

The complete rifle spans 66 of 72 columns. The restored semantic side subject
is mapped to 66x28 cells inside the 72x32 authored grid. Wood, body metal, thin
steel, and raised rivets use separate depth roles.

## Reference restoration and trace

The generated objects crossed the original equal-width panel dividers: the
side AK-47 barrel extended into the neighboring panel. The previous fixed-third
crop therefore cut the reference at the gas block. `rebuild_review_inputs.py`
now extracts the three connected alpha subjects first and places them on equal
semantic canvases in muzzle, side, top order.

`reference_multiview_alpha.png` remains the generated source.
`reference_multiview_axis_order.png` is the restored semantic comparison sheet.
`reference_multiview_axis_order_chroma.png` is its traceable chroma copy. The
retained `trace/color_trace_report.json` and `trace/color_cell_comparison.png`
record the 72x32 side extraction and reference palette. The authored blueprint
uses semantic material names while its colors come from those measured palette
clusters.

The renderer's top camera requires a 90-degree roll-only correction, applied by
the same review-input script. Geometry is never stretched or warped.

## Rebuild and validate

```powershell
$skill = "$env:USERPROFILE\.codex\skills\minecraft-reference-to-voxel"
python "$skill\scripts\compile_voxel_item.py" --project .\voxel-models\ak47
python "$skill\scripts\validate_axis_geometry.py" --project .\voxel-models\ak47
python .\voxel-models\ak47\rebuild_review_inputs.py
python "$skill\scripts\compare_multiview.py" `
  --reference .\voxel-models\ak47\reference_multiview_axis_order.png `
  --render .\voxel-models\ak47\preview\ak47_comparison_turntable_semantic.png `
  --out .\voxel-models\ak47\comparison --panels 3 `
  --labels "MUZZLE FRONT ORTHO,TRUE SIDE ORTHO,TOP ORTHO"
```
