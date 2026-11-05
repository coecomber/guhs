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
- `VadsrijkZones.nabij(zone, x, z, fade)`: 1 inside the tile box, fading to 0 outside (skies).
- `VadsrijkZones.kampMeng(x, y, z)`: line-0 camera/flight points bent west with the camp (full shift at z >= 860, none at <= 500).
- Generated POI classes (`WereldPlekken`, `Z2WereldPlekken`, `Z3WereldPlekken`) are in WORLD coordinates (after the shift).
- Harbour POI ids (shared with t-luchtschip): `haven_<van>_<naar>_vertrek` (+ `_bord`, `_mast`, some `_plein`/`_eind`):

| id | where | zone file |
|---|---|---|
| `haven_z1_z2_vertrek` | Noordklifhaven, north cliffs of zone 1 (~150, -650), line A | WereldPlekken |
| `haven_z2_z1_vertrek` | Kandijkom-dok (= `dok_landing`), line A | Z2WereldPlekken |
| `haven_z2_z3_vertrek` | Galmdok, SE cape of zone 2 (~990, -856), line B | Z2WereldPlekken |
| `haven_z3_z2_vertrek` | Weststeiger (= zone 3 `dok_landing`, Honingtak level), line B | Z3WereldPlekken |
| `haven_z3_z4_vertrek` | Oosttaksteiger (= `oosttaksteiger_landing`), line C | Z3WereldPlekken |
| `haven_z4_z3_vertrek` | Lampionhaven — **built by t-z4wereld** (planned 1800, 71, -470) | zone 4 |

  `IndelingRegistry.plek(id)` finds any of them (zone 1/2/3 POIs, else the planned spot). Line 0 (Kampsteiger <-> De Haven)
  keeps its old ids (`steiger_eind`, `luchtschip_bel`, `luchtschip_haven_*`).
- **Barrier exemption for the airship (t-luchtschip):** tag the player OR the vehicle with `guhs_luchtschip`
  (`Barrieres.TAG_VRIJ`), or add a predicate to `Barrieres.VRIJSTELLING`. Scenes (`RijkScenes.bezig`) are exempt already.

## What I built

### 1. The shift (generators + code)
- Knuffelkamp: `kamp_wereld.py` OX += KAMP_DX; the camp pieces in `bouwsels.py` (south jetty + stair, return portal, 2 lost
  knuffels, Dobber's hat) are drawn in local camp coords and stamped shifted (`kamp_stempel`, `kamp_p`); the line-0 set pieces
  (anchored airship, cloud banks) bend with `kamp_meng`.
- Zone 2: `zone2/plan.py` stays LOCAL; `maak2.py` writes only the inner local tiles (`UIT_TX0..` = world tx 6..15, tz -23..-13)
  under their world names, shifts every POI (`indeling.schuif`, skips `level`), the areas, paths, zone box and `BURCHT_DX/DZ`
  (now incl. the zone offset, so all burcht code follows), and deletes the old-place tiles (world tx -11..10, tz -48..-28).
  The noise grid is unchanged (same terrain).
- Zone 3 was already at (1404, -576) in the base (verified: plan3 `TX_STAM, TZ_STAM = 1404, -576`, tiles tx 16..26, tz -16..-3).
  Fixed: `maak3` deleted every tile in the old zone-3 rasters (tx 25..46) = zone 4/5's tiles! Now skips other zones' boxes
  (`indeling.van_andere_zone`). `Z3WereldPlekken.inZone3` (poi3 template) = the tile box without the old 200 margin (it
  overlapped zone 2 and zone 4: biomes, Reiskaart, sky).
- Java: `Gebieden` (AANKOMST, knuffelkamp box, ring ±660), `NpcPlekken` (Dobber, camera paths via kampMeng), `WereldReizen`
  route + SCHIP, `WezensPlekken.KAMPKLODDER`, `QuestStap` (eiland/zone2 by box), `QuestMotor`, `Reiskaart.hier`, `Z3Haken`,
  `Z2Haken`, `Z2WereldRegistry.inZone2`, `Z2Lucht`/`Z3Lucht` sky strength (box distance), `Z2Commando proefvloer`,
  `Z2NpcPlekken` (p/w/v helpers = local zone-2 coords, raw Vec3 wrapped with `z2V`, fallback areas `+ DX/DZ`),
  `Z2BazenPlekken`/`Z2SystemenPlekken`/`Z2WezensPlekken` fallbacks, `Z2Reizen` fallback, biome + town-music gametests,
  `WereldLucht` horizon azimuths (Kandijkom NNE 33, Boom ENE 68, Hoogvads 86), `Z2Lucht` tree azimuth 124.
- AutoCheck scripts: every absolute `/tp` in the camp / zone-2 range shifted (50 lines in 12 scripts) + klodderreus summons.
- audit.py: only the zone-1 grid (tx -11..11, tz -11..18) + the camp box moved.

### 2. Natural edges + barriers
- Zone 1 (`wereld/kaart.py`): superellipse (p 4, 630 x 625) + low noise + 10 lobes (north cape for the Noordklifhaven,
  capes/bays all round, SW peninsula towards the camp); everything near the old coast is protected (`beschermd()`: campfires,
  viewpoints, paths, ponds, cave nodes, the Druipschacht, the temple). North foreland (x -60..620, z -520..coast): the land
  rises 34-56 blocks, rock/scree/snow materials, narrow beach under steep cliffs. Sea floor deeper (clip 12). `randwolken`
  (clouds along the RECTANGLE of the grid) removed; grid-edge barrier walls removed (zone 1 + zone 2).
- Zone 2 (`kaart2.py`): organic outline (superellipse + noise + 8 lobes incl. the SE Galmdok cape), crumbly east coast, steeper
  ice cliffs in the south (not at the dock); the cloud rim (`bouw2.rand`) now follows the real coast with gaps/clusters and sinks
  towards the east; the 4 far spires at the old grid corners are gone.
- De Splinterwand (`tools/vadsrijk/barriere.py`): one deterministic list of 55 rock spires / crystal needles (90-145 high, gaps
  to look through) along the gap zone 1 <-> 2; zone 1 and zone 2 stamp the ones in their grid, `barriere.maak_tegels()` writes
  the unowned tile row tz -12 (tx -2..15: sea + spires).
- Zone 3 (`barriere.zee_z3`, called from `maak3.Werker.tegel`): the sea (floor 50, rising to 60 at the zone-2 seam) round the
  island (columns >= 205 from the trunk, empty cells only) + the cloud sea on top: dense cloud cliffs along the meandering
  **Wolkenkloof** channel (x ~1066, the open water between zone 2 and 3), banks breaking apart towards the east (**Wolkenval**,
  towards zone 4). The old low cloud floor (y 14-40) under the new sea bed is removed.
- Procedural sea (`indeling/ZeeVulling`, hook in kern `Ondergrond.vul`): every tileless chunk of the island sea gets water to
  62 + a floor (49 deep, 60 round zone 2). Zone-5-only chunks get the sea first, then their tile (hook in `KaartFeature`).
- Soft push-back (`indeling/Barrieres`): free areas per zone (superellipses: camp, zone 1 + SW peninsula + turtle island, zone 2,
  zone 3 + werf, zone 4, zone 5 above y 150); outside them (inside the island sea) the player / his mount / boat / glider is
  pushed back towards the nearest zone (bubbles/clouds/smoke + an action-bar line per barrier kind: stroming, wand, kloof,
  wolkenval, mist, wind). Far into a gap (> 60 blocks) -> back to the last free spot. Never a visible wall.

### 3. Harbours + viewpoints (`tools/vadsrijk/havens.py`: pier, platform, mast + flag, departure board)
- Zone 1 Noordklifhaven: plaza on the cliff top (flattened in kaart.py), pier 26 north, platform; viewpoint `uitkijk_noordkliftop`
  (U1) at the platform's far corner; new path `noordklif` from the mountain path north of the fossil bridge.
- Zone 2 Galmdok: plaza on the SE cape (path `galmdok` from the hot-spring path), pier 24 east; viewpoint `uitkijk_galmkam` (U3).
  Kandijkom-dok: departure board added; `uitkijk_kandijkom_balkon` (U2) = the dock end.
- Zone 3: Weststeiger / Oosttaksteiger + `uitkijk_westtak/oosttak` already existed (p-wereld); I only added the shared ids.
- Texts: `tools/features/vadsrijk_t_wereld.py` (one line in FEATURES): viewpoint names, barrier messages, harbour names, dev text
  (nl + real English).

## Shared-file changes (all add-only, marked `T-WERELD`)
- `VadsrijkFeature.java`: +1 line `IndelingRegistry.register`.
- `kern/wereld/Ondergrond.java`: `ZeeVulling.vul` at the top of `vul`. `kern/wereld/KaartFeature.java`: sea under zone-5 tiles.
- `kern/Gebieden.java`: AANKOMST / knuffelkamp box via the camp offset; ring ±660.
- `tools/features/__init__.py`: + `vadsrijk_t_wereld`.
- Generators of other slices (zone 1/2/3 map tools), checkers (audit.py) — see above.

## Decisions I made myself
- Zone 2's plan stays in its old (local) frame; offset at output. Keeps all zone-2 numbers + noise identical.
- Barrier "walls" are free-area superellipses (simple, deterministic, tunable in one list) instead of per-barrier geometry.
- Zone 3 gets real water round the island (doc §6: "wolkenzee op y 62-64 met water eronder"), so all zone seams are water-water
  (no water spilling into void at the tile edges). The Wolkenkloof is an open channel between cloud cliffs (a 120-deep gorge is
  impossible with sea at 62).
- U1/U3 viewpoints are the harbour platforms' far corners (no extra paths/stairs to maintain); U2 = the Kandijkom dock end.
- Free keys: none used.

## Resume point / status
(filled in below after the regen)
