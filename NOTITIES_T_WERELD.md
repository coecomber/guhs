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

## Map regen + checkers (2026-11-06 night)
`python tools/vadsrijk/maak_kaart.py --zone alle --geen-cache --audit` ran clean (zone 1: 690 tiles 63 MB; gap row tz -12:
18 tiles, 55 spires; zone 2: 110 tiles 6.8 MB, 462 old-place tiles deleted; zone 3: 154 tiles 30.8 MB). Zone 1 and 2 were
regenerated again after fixes (`--zone 1 --audit`, `--zone 2`).

| checker | result |
|---|---|
| audit.py (zone 1 + camp) | stairs 0 (was 14: the camp tent ridges, now counted as roofs), path steps 5 (= baseline, all old: camp lighthouse, camp south, Druipgrotten, temple foot), doors 0, under-map 0, pits 4 (old: camp lighthouse cellar + Zwamzoet ordetempel roof), water 20 (new ones = open sea round the camp/turtle island + 1-column pools at the SW peninsula and under the north cliffs, all in the barrier zone) |
| controleer_verticaal.py | 0 errors (Hoedenpaden 73549 states, Druipschacht 2135, no traps) |
| loopcheck.py zone 2 box | VAL 30 / TRAP 46 vs baseline 28 / 46 at the old place (same set; +5 single-column coast pools at the new organic coast, -3 old ones) |
| loopcheck.py gap row | only 2 "LEEG" at the window edge under the sea bed (y 37, checker artefact of the hollowed zone-1 sea floor) |
| loopcheck3.py (zone 3) | no VAL / TRAP / POI; PAD 396 (baseline 388) |
| poicheck3.py | 0 of 430 (oosttaksteiger_luchtschip = an airship anchor in the air, now skipped like dok_luchtschip) |

Note: the Hoedenpaden network is generated from the Sporenwoud's giant mushrooms, and these shift a little with the new
coast; the layout changed slightly but controleer_verticaal proves it fully walkable again.

## Stairs report (trappencheck worldgen zone 1-3)
- Zone 1 + camp (audit.py, every bottom-half stair checked for a floor on both sides): **0 disconnected stairs**. The 14 it
  used to list (also in the baseline) are the ridges of the camp tents (`pleister_roze_trap` on `tentdoek`), roofs, not stairs.
- Zone 1 towers (Hoedenpaden) + Druipschacht (controleer_verticaal): every stair/ramp reachable and walkable, 0 traps.
- Zone 2 (loopcheck.py TRAP, 46, all also in the baseline): every one inspected by type = **furniture**: chairs (a stair at a
  table with cups: Kandijkom houses, Galmdorpje, cafe), benches round the Kandijkom plaza and the dock, a bench behind a fence
  armrest. No stair flight with a gap, no floating step, no blocked exit. Coordinates in `scratchpad`-free form: e.g. chairs at
  (578,99,-951), (748,129,-1261), (813,103,-1070); benches at (694,91,-1042..-1040), (699,91,-941).
- Zone 3 (loopcheck3, whole zone in one window incl. lifts/wind columns): 0 TRAP, 0 VAL.
- New pieces of this slice: Noordklifhaven path (max step 1 along the whole path, checked) and plaza flush with the pier;
  Galmdok plaza flush with its pier. Pier/platform decks are flush with their plazas (no steps).
- NOT fixed (old, not stairs): the 5 path steps of 2 blocks in zone 1 (camp lighthouse -457,92,948; camp south -386/-382,91,1103;
  Druipgrotten -263,90,-301; temple foot 319,68,-325) and Zwamzoet's 2 old roof pits.

## Resume point / status
- Head: see git log (last commit "t-wereld: ..."). Generated files (tiles, *WereldPlekken.java, POI_*.md, AUDIT, veilige_plekken)
  are NOT committed; integration regenerates with the command above.
- Gametests `-Pgt=Indeling,VadsrijkBiomen,StadsMuziek,VadsrijkNpc,VadsrijkZ2Systemen` (16 methods, with the regenerated
  map): all pass except `StadsMuziekGameTests.gebiedNaarDeuntje` "Honingtak dock: honingtak": the Weststeiger dock (1201,167,-601)
  lies 204 blocks from the trunk, outside the z3_honingtak cylinder (r <= 200). Identical data in the base (dbf82899d), so this is
  pre-existing (p-wereld moved the dock to the branch tip), not caused by the shift. Fix for z3: widen the cylinder or test the
  plaza instead.
- The worktree still holds the regenerated (uncommitted) tiles/POI files, so it can be used as a test client as is.

## Open points
- **Zone 4 (t-z4wereld):** zone 3 now has sea at y 62 to its tile edge (x 1727), so zone 4's tiles meet water. Zone 4 should not
  put invisible walls at its tile edges; outside its tiles the procedural sea fills (floor 49). Its free area for the barrier
  push-back is the superellipse in `Barrieres.VRIJ` ("z4": centre 2152,-491, 390 x 262); adjust there if islands end up outside.
- **Zone 5:** its tiles east of zone 4 (x >= 2560) get the procedural sea under them automatically (KaartFeature hook).
- **t-luchtschip:** exempt your riders/ship with the tag `guhs_luchtschip` or `Barrieres.VRIJSTELLING`; POIs above.
- POI_VERZOEKEN*.md (shared folder) still hold old zone-2 coordinates in old requests: add (704, 1344) when reading them.
- The far "verre schimmen" (client-side silhouettes at real positions) and the per-dimension view distance (W-3) are not built
  here; only the horizon azimuths were corrected.
- Departure boards are plain dark boards on posts; the timetable text/display is t-luchtschip's (`haven_*_bord`).
- Zone 3 Weststeiger/Oosttaksteiger have no physical board (bell spot = `_bord`).
- AutoCheck client run: not done (gradle slots were busy all night).
