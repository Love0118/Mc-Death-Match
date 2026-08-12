# AK-47 multiview review

The comparison uses complete connected subjects in muzzle, true-side, and top
order. It no longer crops the side barrel at the original equal-width panel
divider. The compiler top camera is rolled 90 degrees around its viewing axis;
geometry is not stretched or otherwise deformed.

| View | Silhouette IoU | Edge F1 | Area ratio |
| --- | ---: | ---: | ---: |
| Muzzle front orthographic | 0.771 | 0.367 | 1.203 |
| True side orthographic | 0.850 | 0.525 | 1.094 |
| Top orthographic | 0.772 | 0.460 | 0.894 |

The corrected true-side IoU is 0.850, compared with 0.524 from the invalid
fixed-third crop. The remaining side residuals are small voxel quantization and
depth-projection differences, not missing major parts. In particular, the
pistol grip now drops thirteen cells and the front assembly preserves the gas
tube, barrel, cleaning-rod gaps, compact muzzle, and front-sight tower.

Mechanical geometry remains independently verified: both barrel regions use
open X-axis 3x3 rings, the gas tube is an X-axis cross-section, the trigger
guard stays open, and the muzzle close-up proves a real hole rather than a dark
face color.
