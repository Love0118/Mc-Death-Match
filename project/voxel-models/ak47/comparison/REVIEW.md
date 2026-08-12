# AK-47 multiview review

The final comparison uses the reference panels in muzzle, true-side, and top
order. The compiler's top camera was rolled 90 degrees around its viewing axis
before comparison; alpha was preserved and the gun geometry was not stretched
or otherwise deformed.

| View | Silhouette IoU | Edge F1 | Area ratio |
| --- | ---: | ---: | ---: |
| Muzzle front orthographic | 0.771 | 0.323 | 1.163 |
| True side orthographic | 0.524 | 0.142 | 0.702 |
| Top orthographic | 0.733 | 0.362 | 0.857 |

The muzzle and top silhouettes agree after camera roll correction. The AI side
panel is vertically inflated relative to both its own top/muzzle panels and a
mechanically consistent coarse AK-47: its stock, receiver, pistol grip, and
magazine are all simultaneously thicker. The second authored pass still moved
toward that panel by shortening the stock and extending the curved magazine
from a 13-cell to a 19-cell drop. It does not copy the remaining contradictory
vertical inflation into every physical depth tier.

Mechanical geometry remains authoritative where the AI sheet is ambiguous:
the barrel and muzzle are hollow X-axis rings, the gas tube is an X-axis
cross-section, the trigger guard stays open, and the side colors do not fake a
bore or end face.
