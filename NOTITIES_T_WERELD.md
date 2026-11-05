# NOTITIES T-WERELD (wereldindeling W-3)

Branch `110-t-wereld` from dbf82899d. Source: `guhs1100-avontuur/wereldindeling/WERELDINDELING.md` + ZONE4_PLAN W-1..W-3.

## FOR OTHER AGENTS: zone offsets (one constant per zone) — read this first

`world = local + (DX, DZ)`, y never shifts. Java: `nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones`; Python (map
generators): `tools/vadsrijk/indeling.py`. Keep placing things in your zone's LOCAL coordinates and convert once:

| Zone | local frame | DX, DZ | Java helper |
|---|---|---|---|
| Knuffelkamp | old camp coords (arrival (0,65,1130)) | -384, 0 | `VadsrijkZones.kamp(x,y,z)` / `kampV` |
| Zone 1 Zwamzoet | world (did not move) | 0, 0 | — |
| Zone 2 Splinterpieken | old zone-2 coords (Kandijkom (0,-2400), `zone2/plan.py`) | +704, +1344 | `VadsrijkZones.z2(x,y,z)` / `z2V` |
| Zone 3 Schemerwortel | trunk-relative (`zone3/plan3.py` rel) | 1404, -576 | `VadsrijkZones.z3(x,y,z)` |
| Zone 4 Lampionkust | world | 0, 0 | box x 1740..2540, z -716..-266 (tiles tx 27..39, tz -12..-5) |
| Zone 5 Hoogvads | world | 0, 0 | box x 2440..3150, z -660..-380, y >= 150 (tiles tx 38..49) |

- `VadsrijkZones.zoneVan(x, z)` / `zoneVan(x, y, z)` -> `Zone.KAMP/Z1/Z2/Z3/Z4/Z5` (tile boxes; zone 5 wins above y 150).
- Generated POI classes (`WereldPlekken`, `Z2WereldPlekken`, `Z3WereldPlekken`) are in WORLD coordinates (after the shift).
- Harbour POI ids (shared with t-luchtschip): `haven_<van>_<naar>_vertrek`, e.g. `haven_z1_z2_vertrek` (Noordklifhaven),
  `haven_z2_z1_vertrek` (Kandijkom-dok), `haven_z2_z3_vertrek` (Galmdok), `haven_z3_z2_vertrek` (Weststeiger),
  `haven_z3_z4_vertrek` (Oosttaksteiger), `haven_z4_z3_vertrek` (Lampionhaven, built by t-z4wereld), plus the existing
  `kampsteiger` / `luchtschip_haven_*` for line 0. See `tools/vadsrijk/indeling.py` HAVENS for the planned positions.

## Status

(in progress)
