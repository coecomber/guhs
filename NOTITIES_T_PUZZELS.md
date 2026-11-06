# NOTITIES T-PUZZELS (night round T, 2026-11-05/06)

Branch `110-t-puzzels` from dbf82899d. Brief: T_BRIEF.md + the agent prompt (VOORSTEL_ROADMAP §5.1).

## What I built
**The puzzle toolbox** (`feature/vadsrijk/puzzels/`), data-driven:
- `PuzzelDef`: one puzzle = `data/guhs/vadsrijk_puzzels/<id>.json`, written by `tools/features/vadsrijk_t_puzzels.py` from ASCII layouts.
  - The layout is in LOCAL coordinates; it is anchored to a POI id (`WereldPlekken`/`Z2WereldPlekken`/`Z3WereldPlekken.plek(id)`) plus an offset.
- `PuzzelBlok`: one block class with 19 kinds (`vr_pz_<soort>`). It has the properties `aan` and `stand` (0..7), is unbreakable, and has no loot.
  - drukplaat, klanksteen, voorspeelsteen, symboolsteen, probeerknop, herstelknop, spiegel, lichtbron, lichtdoel, schuifblok, doelvloer, startlantaarn, eindplaat, scheur, geheimdeur, hendel, fluistersteen, sterrensteen, sterrenkaart.
- `Puzzels`: the engine (8 kinds), with the rules:
  - **volgorde:** plates or singing stones, with a demo stone.
  - **licht:** mirrors and a beam; the beam is drawn with particles.
  - **symbool:** symbol stones plus a try button.
  - **patroon:** star stones, only at night, plus a try button.
  - **schuif:** push blocks plus a reset button.
  - **tijdrit:** per player, with a countdown on screen.
  - **wachtwoord:** per player; the chat answer is swallowed, so nobody else reads it.
  - **geheim:** a cracked wall or a hidden lever that opens a secret door for a while.
  - **Shared puzzle, reward once per player:** everyone within 8 blocks of the box when it is solved gets it.
  - **Reset:** after `reset` ticks (default 400; secrets 600); also after 2 min of being left half done with nobody near.
  - **Tips:** failed tries counted per player; tip 1 after `hintNa` (3), tip 2 after 6. After that the tip stays in the screen line.
  - **Screen line:** "puzzeltip constant in beeld" (round 16). An action-bar line every 2 s inside the box; secrets stay quiet.
- `PuzzelStempel`: puts the puzzle into the world, by code, the first time a player is within 72 blocks of the anchor.
  - It looks for the flattest free spot within 24 blocks of anchor + offset: height difference ≤2, no water, no logs/walls in the box (leaves and plants are fine).
  - It scans the ground from the anchor's own y, so the zone-3 tree canopy and overhangs don't confuse it.
  - The spot is remembered in `PuzzelData` (SavedData `guhs_vadsrijk_puzzels`). It stamps again (old box cleared first) when the layout or the anchor point changes.
- `PuzzelSpeler`: per player in `SpelerData.extra("puzzels")`: `opgelost`, `bladzijdes`, `ontgrendeld`.
- `Bladzijde`: items `vr_pz_bladzijde_1..9` ("Gescheurde bladzijde"). The legend of the Lege Schaal (WERELDVERHAAL §2.2) in 9 pieces, plus the Oog and the "eenzaam katje" note. Right-click reads it.
- `PuzzelBoek`: the Verkenningsboek category "Puzzels en geheimpjes" on zone 1, 2 and 3. It is counted in the percentage; secrets show "???" until found.
- `PuzzelDev`: solves a puzzle through the real input (dev command + gametests).
- `PuzzelRegistry`: blocks, items, events (server start = load data, tick, chat) and the commands `/guhs vadsrijk puzzel ...`.

**Content: 22 puzzles/secrets** (zone 1: 8, zone 2: 7, zone 3: 7). See SPIEK_T_PUZZELS.md for every id, place and solution.
- Rewards: coins on every puzzle, plus one of:
  - 9 lore pages about the Keizer;
  - item rewards (verbetersteentjes, herrolknabbels, Klodderreus-knuffel);
  - 6 cosmetic unlock ids (below).

## Decisions I made myself
- **Placement by code (stamping), not in the map tiles.** t-wereld is rewriting the map generators tonight, so editing `maak_kaart.py` and its siblings would conflict.
  - The anchors are POI ids, so the shift moves the puzzles along: (-384, 0) for Knuffelkamp, (+704, +1344) for zone 2.
  - As soon as the regenerated `*WereldPlekken` hold the new coordinates, the puzzles appear at the new place. There is nothing to recompute.
- **Walkability is handled when stamping:**
  - a real floor under every cell, filled down to the ground;
  - the box and a 1-block ring cleared of plants/leaves;
  - stairs on each side (the walkable edge cell nearest the middle) out to the terrain when the difference is >1 (max 8 steps, up or down).
  - The parkour pillars are at most 3 high, so a fall never hurts and there is no pit. The secret rooms are 3x3 with an open door and a real floor.
- **The "password NPC" is a talking stone** ("Fluistersteen", with a face). This avoids touching the NPC slices, and a stone that whispers fits the guh style.
  - The answer is typed in chat within 1 minute after clicking, within ~12 blocks. Dutch and English answers are both accepted.
- **Zone-3 puzzles only at ground level** (around the Wortelweg). Stamping 9x9 boxes on the branches could cut paths.
- **The star puzzles use a "sterrenkaart" in chat** (★ ·), not a drawn screen.
- **Reward when solving, for everyone near.** There is no separate walk-in treasure. Doors and cracks open as a visual "it worked".
- **Knuffel reward:** `guhs:vr_eco_klodderreus_knuffel` (an existing Vadsrijk plush). Item rewards go into the Vadsrijk inventory like all loot.

## Shared-file changes (all add-only, marked `T-PUZZELS`)
- `feature/vadsrijk/VadsrijkFeature.java`: +1 line `PuzzelRegistry.register(modBus)`.
- `feature/vadsrijk/voltooiing/Voltooiing.java`: `categorieen()` now also adds `EXTRA` categories, plus 2 new methods:
  - `extraCategorie(zone, Supplier<Categorie>)`: a hook for any slice to add a book category to a zone page;
  - `herbouw(zone)`: rebuilds the page after a data reload.
- `tools/features/__init__.py`: +1 line `'vadsrijk_t_puzzels'` in FEATURES.
- `assets/guhs/lang/{nl_nl,en_us}.json`: +208 keys `vadsrijk.puzzels.*`, `block.guhs.vr_pz_*`, `item.guhs.vr_pz_bladzijde_*` (via `_direct()`).
- New assets: `blockstates/vr_pz_*.json`, `models/block/vr_pz_*.json`, `textures/block/vadsrijk/puzzels/*`, `items/vr_pz_bladzijde_*.json`, `models/item/vr_pz_bladzijde.json`, `textures/item/vadsrijk/puzzels/bladzijde.png`, `data/guhs/vadsrijk_puzzels/*.json`.

## For t-kleding (cosmetics)
- Unlock ids handed out by puzzles:
  - `pz_vleugels_sterrenstof` (z1_sterrenveld)
  - `pz_vleugels_poollicht` (z2_poollicht)
  - `pz_vleugels_lantaarnmot` (z3_lantaarnloop)
  - `pz_cape_sneeuwvlok` (z2_lantaarnsprint)
  - `pz_cape_honingraat` (z3_bijenlied)
  - `pz_cape_mos` (z3_mossymbolen)
- Ask: `PuzzelSpeler.ontgrendeld(ServerPlayer, id)`.
- Listen: `Puzzels.ONTGRENDEL_HAAK.add((player, id) -> ...)`. It is called once, at the moment of unlocking.
- Names are in lang: `vadsrijk.puzzels.ontgrendel.<id>`. Wing effects (R4: movement only) are yours to pick.

## For t-wereld (the world shift): what you need to know
1. **Keep these POI ids** (or tell integration the new name; the gametest `VadsrijkPuzzels data` fails with the missing id):
   - zone 1:
     - `kampvuur_klodderweide_1`, `kampvuur_klodderweide_3`
     - `kampvuur_sporenwoud_1`
     - `lichtstralen.1`
     - `kampvuur_druipgrotten_3`
     - `hoedhupperwei`
     - `kampvuur_vuurtoren` (Knuffelkamp, moves -384)
     - `kampvuur_zwamtempel_1`
   - zone 2:
     - `kampvuur_knuffelmeer_1`, `kampvuur_knuffelmeer_2`
     - `kampvuur_galmhellingen_1`, `kampvuur_galmhellingen_2`
     - `kampvuur_splinterkloof_1`, `kampvuur_splinterkloof_3`
     - `gehucht_meerkamp`
   - zone 3:
     - `kampvuur_wortelweg_1`..`_6`
     - `kampvuur_mosvoet`
2. **Nothing to compute:** the puzzles follow `plek(id)` + offset. If a POI moves to a spot without flat ground within 24 blocks, the stamp still works (raised floor + stairs), but it is nicer if there is a ~12x12 flat-ish meadow within ~20 blocks.
3. **Optional (nice to have):** if your generator wants to reserve flat "puzzle meadows", put a POI id `puzzel_<id>` there. I can switch the anchor to it in one line per puzzle (the generator file `vadsrijk_t_puzzels.py`, the `anker` tuple).
4. Already stamped puzzles of an old dev world are cleared and stamped again automatically when the anchor point changes (fingerprint in `PuzzelData`).

## Tests
- `VadsrijkPuzzelsGameTests` (`-Pgt=VadsrijkPuzzels`), 10 tests:
  - **data:** 22 puzzles, ≥5 per zone, anchors exist, blocks/items/pages exist, each page given once;
  - **alleOplosbaar:** every puzzle stamped + solved through the real input, starting unsolved, the beam missing at the start, the reward, doors open, all 9 pages, an unlock;
  - **foutenEnTips:** wrong order → reset + count → tip 1 → tip 2;
  - **herstelEnEenmaal:** reset after solving, doors close, a reward only once;
  - **samenPuzzelen:** 2 players are both rewarded;
  - **lichtstraal:** mirror rules + the target lights up;
  - **schuiven:** wall, reset button counts a try, the BFS solution, targets lit;
  - **raadsel:** only after clicking, a wrong answer is swallowed + counted, normalisation;
  - **sterrenEnTijd:** day/night, too late = a failed try, the end plate without a race;
  - **boek:** category per zone, counted.
- The generator validates every light puzzle (brute force: solvable, start state not solved) and push puzzle (BFS). The solution pushes are in the json.

## Resume point / status
- Done and green: `runGameTestServer -Pgt=VadsrijkPuzzels`, 10/10 (+ the framework's own), 2026-11-06 01:30. Also `-Pgt=VadsrijkVoltooiing` 3/3 green (the book hook).
- NOT tested in a client: the look of the 19 generated textures, the stair connections on real terrain, and whether every anchor has a decent flat spot.
  - First in-world check: `/guhs vadsrijk puzzel tp <id>` for each id in SPIEK_T_PUZZELS.md.
- After t-wereld's shift: nothing to do unless a POI id was renamed. The `data` gametest will say so.

## Open points
- No hand-made models: the blocks are 16x16 generated pixel art (cube/plate/small box models; lanterns reuse the vanilla lantern template).
- No wiki "blokuitleg" entries for the 19 blocks yet (the memory rule is for the main mod wiki; the Vadsrijk wiki page is not set up).
- "Volg het muisje", colour puzzles and "weggestopte guhs" (other §5.1 ideas) are not built. The toolbox can take a colour puzzle as `patroon` without the night flag (only data needed).
- The weekly "Snuffelschat" and the "Snuffellantaarn" glint (§5.1) are not built. Secrets glint with light level 3 instead.
- Stamping at runtime means a puzzle appears ~1 s after you arrive the first time. Pre-placing them in the map tiles is possible later: the generator can emit the same layout into the tiles.
