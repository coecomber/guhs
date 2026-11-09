# NOTITIES W-BEREIK (ronde W, 2026-11-08): is every quest completable on foot in survival?

Branch `110-w-bereik` from 0f00119f2. Short answer: **yes for all main quests and all NPCs of zones 1-6** (100 %, checked by
a global flood over the real map tiles). Side quests 100 % except one puzzle-gated spot. The remaining secrets and their reasons
are listed at the bottom. Cheat sheet with teleports: `SPIEK_W_BEREIK.md`. Full report: `BEREIK_RAPPORT.md`.

## 1. The check: `tools/vadsrijk/bereikcheck.py`

- **Map input:** reads the tiles in `src/main/resources/data/guhs/structure/vadsrijk/tegels/` (1308 tiles, zones 1-6).
  - This is the working format every generator writes.
  - `kaartpak.py` packs these tiles 1:1 into the shipped `kaart.gpak` and verifies them tile by tile.
- **Graph:** one global run graph, ~12.9 M nodes, ~48 M edges.
  - A node is a vertical stretch of passable cells in a column.
  - An edge is a walk, step, drop, swim, climb or 1-2 block gap jump.
  - Extra edges: airship lines, lifts, the zone-3 wind columns, the zone-2 gondola, the zone-3 lifts, the zone-5 hatches/lifts
    (incl. the new Wolkwaldroom exit), the zone-6 hatches, the raid door, and the story teleport spuitgat -> Wolkwaldroom.
  - Runs in ~90 s and ~2.5 GB.
- **Player model** (half blocks):
  - 1.8 high: ceiling - feet >= 4.
  - Step +0.5 free; a +1 jump needs 2.8 blocks of air over the lower floor.
  - Drop at most 3 blocks, any depth into water. Landing on a ladder/vine/water column counts as holding on.
  - Off the high half of a stair, +1.5 is a jump.
  - Fences/walls/panes/bars count 1.5 high and blocking. Lanterns/chains/lampions are blocking.
  - Wooden doors/gates/trapdoors are open; a trapdoor on a ladder is climbed through. Iron doors are closed.
  - Leaves are solid. Carpets, plants and torches are passable. `gouden_kaarsenrand` and the other no-collision Z5 light blocks
    are passable (the report lists them on walking edges: 0).
- **Targets:** `tools/vadsrijk/bereik_doelen.json`, dumped from the running game code by the new gametest
  `nl.juiced.guhs.feature.vadsrijk.bereik.BereikDoelenGameTests`. It contains:
  - every public static BlockPos / Map / Collection of every `*Plekken` class and `QuestPlek`;
  - `Npcs.alle()`, `Arenas`, the spawners and areas (WikiExport), `Lijnen.ALLE` + line E, puzzles/secrets;
  - the teleport links.
  - Run: `gradle_slot.sh <tree> runGameTestServer -Pgt=BereikDoelenGameTests`.
  - The quest steps are resolved like `QlPlek.van`: giver, hand-in, every objective's npc/poi/gebied/baas.
  - "You are there" follows the game's rules: `NpcHaken.bij` with each zone's `POI_STRAAL`/`POI_HOOGTE`/`hoogteVan`. NPC = talk
    reach 3.5. Secrets = block reach 4.5. Open-air spawners = their radius at any height (mobs spawn on the surface).
- **Report** (`BEREIK_RAPPORT.md`), per zone:
  - reached/total;
  - every miss with coordinates and the nearest reached cell;
  - targets reached only with a gap jump;
  - arenas (cells without a way back, plus the Questlopen spot);
  - airship boarding (eye-to-hull distance);
  - links, one-way areas, doors, and deco on walking lines.
- **Options and wiring:**
  - `--zone N`, `--punt x,y,z` (debug), `--strikt` (exit 1 when a main-quest target or an NPC is unreached).
  - Wired into `maak_kaart.py --audit` (after the connection check).
  - Documented in `tools/vadsrijk/LEESMIJ_BOUWREGELS.md` §2.

## 2. The shared pass: `tools/vadsrijk/vrijpad.py` (in maak_kaart, right after verbind)
- **Removes:**
  - lanterns/chains/lampions hanging into the walking space (1 air under them over a walked floor). The chain above becomes a
    hanging lantern.
  - lanterns/chains standing on stair steps.
  - lanterns/chains on slab floors with 2+ walkable neighbours.
  - loose chains on floors.
  - lanterns in the open (4 walkable neighbours).
  - **floating fences/walls/bars/panes** (air under and over) 1-5 blocks over a walkable floor.
- **What it found:** the full run removed ~1 300 pieces (zone 1-6). Idempotent.
- **The cathedral "low walls/posts you walk through" (zone 5):**
  - They were `wolkmarmer_gepolijst_muur` walls **floating 4 blocks above the west stair** (x 2686, y 225, z -530..-510): a
    balustrade left in the air. They render normally and have collision, but you walk underneath them.
  - They are not display entities and not no-collision blocks. The floating-rail rule removes them.

## 3. Generator fixes (all verified by regenerating locally and rerunning the check)

| # | Zone | Problem | Fix | File |
|---|---|---|---|---|
| 1 | 5 | **De walvis / Staartpark:** the palace hill (y 232) ended in a 29-high cliff over the Staartpark (y 203). The park, the tail, the Staarthaven (airship line E) and therefore ALL of zone 6 could not be reached on foot. A 2-high parapet also closed the edge. | `staarttrap()`: a 3-wide switchback stair against the cliff, solid under the steps, with balustrades, through an opening in the parapet. | `zone5/bouw5.py` |
| 2 | 5 | **Kootje Zilverstaart:** the Wolkwaldroom has no way out; only the story teleports you in. | A ladder with an open hatch next to the door, plus a new hatch `wolkwaldroom_uit` (right-click = up to the middle blowhole). Lang keys added. | `zone5/bouw5.py` `wolkwaldroom()`, `Z5WezensPlekken.java`, nl_nl/en_us |
| 3 | 5 | **Gildemeester Duimdop:** the stair from the street ended with a step right at the door (feet +0.5), so the head hit the lintel. | `trap_af`: going UP from a door, the first cell is a flat landing at door level. This applies to every zone-5 door with an up-stair. | `zone5/bouw5.py` |
| 4 | 5 | 3 university courtyard doors opened onto a facade column (hoog3). | `hofdeuren_vrij()` after hoog3. | `zone5/bouw5.py` |
| 5 | 5 | Rose-window quest spots (z5_s11) lay behind the transept balustrade (7 > POI radius 5). | The POI moved 4 blocks into the transept. | `Z5VerhaalPlekken.java` |
| 6 | 6 | **33 Saffraanpoort doors** opened onto a wall. | A door without a room (`deur(open_=False)`) is an `iron_door` (locked, clearly fake). | `zone6/kit/koepelkit.py` |
| 7 | 6 | The 8 knuffels + 3 treasures of zone 6 looked up `geheim_z6_*` ids, but the map publishes `geheim_*`. They fell back to old literals: floating in the air / inside rock (mesa y 122, minaret y 120). | Lookup ids fixed. | `Z6VerhaalPlekken.java` |
| 8 | 4 | The Opstijgpier ring fence crossed the pier walkway: the hemelkapitein (main quest z4_hq07 / z5_hq01) was unreachable. | The ring is open where the pier comes in (west). The boarding side is w-schepen's. | `zone4/kit/eilanden.py` |
| 9 | 3 | lift_kapel's top stop was a fenced pocket (the deck railing ran through it): Wasja + 3 main quests unreachable. | The 3x3 stop is kept free; a rail only where it borders the void. | `zone3/bouw3.py` |
| 10 | 3 | The Kaaszeppelinwerf tower stair (Dok 3) had 2 blocks of head room over a +0.5 step (zeppelin hull/cheese rock): Roquefort's deck and cabin (z3_hq08) unreachable. | `werf2.trappen_vrij()` as the last zone-3 step: 3 air over every tread. | `zone3/kit/werf2.py`, `zone3/bouw3.py` |
| 11 | 2 | The Galmnaald POI sits inside the crystal spire; the nearest walkable cell is ~7 away (radius 6). | `Z2NpcPlekken.straalVan`: 9 for z2_galmnaald, used by `Z2Haken`. | `Z2NpcPlekken.java`, `Z2Haken.java` |
| 12 | 2 | The gondola guh (top) stood on the station ROOF; the east square guard stood on a 4-high wall. | The stand spot is inside the station; the guards stay on the square. | `zone2/bouw2.py` |

**Kootje, how you normally get there:**
- During z5_hq08 the story (Z5Finale -> `Z5Bazen.start("spion")`) teleports you from the middle blowhole to the Wolkwaldroom door.
- After the fight the dream starts, and you wake up at the blowhole.
- Questlopen, fainting (`Flauwvallen` wakes you at the arena door) or a skipped dream left you inside without an exit. Fixed by #2.

## 4. Numbers (final run, BEREIK_RAPPORT.md)

| zone | main quest | side quest | NPCs | secrets* | arenas |
|---|---|---|---|---|---|
| 1 | 24/24 | 40/40 | 52/52 | 55/61 | 1/1 |
| 2 | 28/28 | 13/13 | 62/62 | 56/63 | - |
| 3 | 43/43 | 11/11 | 61/61 | 78/83 | 8/8 |
| 4 | 24/24 | 26/26 | 48/48 | 119/121 | 1/1 |
| 5 | 23/23 | 28/28 | 50/50 | 36/36 | 3/3 |
| 6 | 37/37 | 37/38 | 53/53 | 45/47 | 2/2 |

\* "Secrets" counts every collectable/secret field entry. The same spot is often listed 2-3 times (NpcPlekken + ZnNpcPlekken +
the generated ALLE map), so the misses below are the unique ones.

Before the fixes:
- Zone 6 was 0 % everywhere (no way down to the tail).
- Zone 5 main 21/23.
- Zone 3 main 41/43.
- Zone 4 main 21/24.
- Zone 2 main 27/28.

## 5. What remains (with reasons)

- **Side quest z6_s12_zoutgrot** (`z6_grot_schat` 3848, 50, -528): behind the crystal wall that the cave puzzle opens at runtime.
  Expected: it is not a map defect.
- **Unique secrets not reached** (9). Each one needs a small generator fix; none blocks a quest step I could find (counters need
  fewer than all):
  - zone 1:
    - `holle_boom` / knuffel_8 (-400, 108, -100): inside a tree crown, no climb route;
    - `hoedenschat` (-318, 96, 59): hat path, the gap jumps are too far for the model.
  - zone 2: `boorgaten` (868, 131, -1081), `galmgrot` (940, 121, -1002), `ijsgrot` (464, 105, -1066). Each lies 7-8 blocks below
    the nearest walkable cell: a drop of more than 3 (findable with fall damage, but there is no way back up).
  - zone 3: `uitkijk_takpunt` + `kuiken_3` (1295, 213, -619): the end of the Werftak branch (the wind column's lower end) is not
    connected to the branch.
  - zone 4: `vliegerspul_1/_3` (2280, 78, -675 / 2295, 85, -680): on the kite island rocks, 6-8 above the path.
- **Airship boarding (for w-schepen, not touched):** eye-to-hull distance > 4 at Lampionhaven `haven_z4_z3_vertrek` (6.4) and the
  Opstijgpier `haven_z4_z5_vertrek` (8.4: the platform ends ~15 blocks from the docked ship). All other harbours 0-2.
- **Runtime-built areas, not in the tiles, so not checked:**
  - the guild valley (-12000, 12000);
  - De Verzonken Sterrenwacht rooms (Z6Bouw, flat by design, entered by the hatch link: reached);
  - the raid binnentoren (template placed by Z6RaidBouw; its door link is reached).
- **One-way areas:** 132 groups of ≥6 columns, mostly ledges/roofs you can drop onto (largest: a terrace at -388, 99, 189 in the
  camp hills, 1700 columns). No quest target lies in one. The Rozenkloof airport stair top in zone 6 has a 1-block step (a jump)
  instead of a last stair.
- **Deco on walking lines left in the report** (81, mostly zone 1/3 lanterns standing on slab/stair counters next to walls): the
  report rule is stricter than the pass; not every one blocks a walking line.
- **Doors:** 196 fine, 33 iron (fake), 0 into a wall, 0 with an unreachable interior (verified after fix #4).

## 6. Shared-file changes (all marked `W-BEREIK`)

- `tools/vadsrijk/maak_kaart.py`: calls `vrijpad.na_sinds` after verbind; `--audit` also runs bereikcheck.
- `tools/vadsrijk/LEESMIJ_BOUWREGELS.md`: §2.
- Generators:
  - `zone5/bouw5.py` (trap_af, wolkwaldroom, staarttrap, hofdeuren_vrij);
  - `zone4/kit/eilanden.py`;
  - `zone3/bouw3.py`, `zone3/kit/werf2.py`;
  - `zone6/kit/koepelkit.py`;
  - `zone2/bouw2.py`.
- Java:
  - `Z5WezensPlekken.java` (+ luik);
  - `Z5VerhaalPlekken.java` (rose windows);
  - `Z6VerhaalPlekken.java` (secret ids);
  - `Z2NpcPlekken.java` + `Z2Haken.java` (straalVan).
- Lang: `nl_nl.json` / `en_us.json`, 2 keys `vadsrijk.z5wezens.luik[.hint].wolkwaldroom_uit` (en_us = Dutch copy).
- New files: `tools/vadsrijk/bereikcheck.py`, `tools/vadsrijk/vrijpad.py`, `tools/vadsrijk/bereik_doelen.json`,
  `feature/vadsrijk/bereik/BereikDoelenGameTests.java`.

## 7. For integration

1. **Regenerate the map in the right order: zone 4 BEFORE zone 5.**
   - Zone 5 merges zone 4 below y 118 in the shared tiles tx 38..39. Zone 4 written after zone 5 wipes the head of the whale.
   - It bit me once in this round. It is not new, but easy to get wrong.
2. Run `maak_kaart` per zone (zones 2-6; zone 1 only needs vrijpad: `python tools/vadsrijk/vrijpad.py`).
3. Rerun the gametest `BereikDoelenGameTests` (it rewrites `bereik_doelen.json` from the merged code).
4. Run `python tools/vadsrijk/bereikcheck.py --strikt`. It must exit 0.
5. Generated outputs (tiles, POI_*.md, *WereldPlekken.java) are NOT committed on this branch. The committed BEREIK_RAPPORT.md is
   from my local regen with all fixes.
6. **Tests run:**
   - `-Pgt=BereikDoelenGameTests` (passes; also proves the Java changes compile);
   - the check, about 8 times.
   - No AutoCheck/client run (as briefed).
