# NOTITIES_I_Z2KAART - fix round 2: zone-2 hanging bridges + the dock arrival / gates

Branch `110-i-z2kaart` (worktree `guhs_vadsrijk/i-z2kaart`, from ab6d952ac). Only generator sources changed
(`tools/vadsrijk/zone2/bouw2.py`, `kaart2.py`); tiles, Z2WereldPlekken.java and POI_Z2.md are NOT committed (integration regenerates).

## 1. Hanging bridges (Splinterkloof brug_1..4 + the Galmspleet brug_5)
- Bug: the deck ends were set from the MAX terrain height over the rim column (+ a half step), so at Kloofwachter Touwtje
  (brug_3, x 150) the deck sat 2.5 above the landing, at brug_1 3.5, at brug_2 1.5; the north end of brug_3 dropped 3.5.
- New `hangbrug()` (shared by `brug()` and `spleetbrug()`):
  - the deck runs from landing A to landing B. A landing is the first path cell beyond the rim (within 3..16 blocks), else 6 blocks
    out (`_landing`);
  - the end slabs sit flush with the ground there;
  - the profile sags over the ravine, and a slope limiter allows at most half a block per cell (slabs only: never a jump);
  - over land, the deck lies on a stone-brick foot, and terrain above it is cut away;
  - fences, chains and head posts sit only on the hanging part. A post never leaves a pocket: the cell outside it is raised.
- The main bridge (brug_3) lands on the Spiegelburcht's grand stair: end at zn-2, stand BRUG_TOP+1. A ridge of terrain that stuck out of
  the tuff landing (1-block bump, already in the baseline) is carved away. `brug_3_noord` is fixed at stand BRUG_TOP+1.
- Checked on the tiles (scratch profile along every axis, landing to landing): every step is <= 0.5 block on all 5 bridges.
  Touwtje (npcplek 153,129,-2528) stands on the walkway beside the deck.

## 2. Dock -> Kandijkom arrival
- The floating pink glass was the old "arch" (`S.box` at s = R0+2, y 100-101) hanging in the air over the gate gap. It is
  removed at all 4 gates. Replaced by `poortboog()`: a complete calcite/diorite gate arch on the inner wall face:
  - 2x2 pillars on a foundation;
  - a rounded beam with upside-down stairs, a crystal keystone lamp and a crown of shards;
  - crystal finials on the pillars, a big lampion under the arch, garlands on both faces, lampions on the pillars.
- Unnatural sides:
  - the old dock ledge cut a 29-wide slot (|x| <= 14) with 8-24 high vertical walls from inside the wall down to the pier;
  - the other gates were 9-wide slots with vertical walls.
  - New `Kaart2.poort_dal()`: across each gate axis the land may rise or fall at most ~1.3 per block from the street edge (plus a
    little rock noise; N/E/W slightly steeper). The geode wall now opens like a mountain pass, and the dock road lies in a soft
    cutting/embankment. The dock flat strip is narrowed to |x| <= 5. Geode crystal shards are only placed where the wall still
    stands (no shards floating over the notch).
- `aankomst()`, from the pier to the south gate:
  - a 7-wide knuffelklinkers road on a stone bed;
  - lampion posts every 8 blocks on both sides, plus tall posts every 16 with a light string (feestslingers + lampions) across the road;
  - fire baskets;
  - a bench by a fire basket (west shoulder);
  - a hot-chocolate kiosk on a little stone-walled terrace in the east bank near the pier;
  - a wooden welcome arch at the pier end: snow on the beam, a pink "welkom" board facing the pier, a crystal lamp and lampions;
  - banks dressed with snow drifts at their foot, snow-capped boulders, crystal clusters and small crystal spikes.
- New POIs (no code uses them yet): `dok_kiosk` (9,91,-2266, the vendor behind the counter), `dok_welkom` (3,91,-2256, a greeter
  spot just inside the welcome arch), `dok_bankje`. **For npc (not my ownership):** a greeter guh/kiosk guh could stand there.

## 3. Other gates (N/E/W)
- Same `poortboog` + `poort_dal` notch. `poort_overgangen()` adds drifts, boulders, crystals and a lampion post pair at each notch.

## Checks
- `maak_kaart.py --zone 2 --geen-cache`: OK (462 tiles, no unknown blocks).
- loopcheck `--box=-300,-2820,300,-2150` vs the baseline (h-melee tiles, same commit):
  - baseline: TRAP 42, VAL 27.
  - now: PAD 0, TRAP 46, VAL 28.
  - +4 TRAP = the new bench + kiosk seats (stairs used as furniture, like the existing chairs).
  - +1 VAL (-104,129,-2567, beside the brug_2 north head post) is a checker artefact. loopcheck counts snow_block as a half
    block; in game the floor there is a full snow block and the way out north is 1 block up (a normal jump).
- Renders (software renderer on the tiles, scratch `iz2/rend.py`):
  - dock arrival `dokr2.png` / `dok_aankomst.png`;
  - east gate `oostr.png`.
  - No AutoCheck client run (no gradle needed; the renders show the arrival).

## Resume point
Done. Open: the user's look judgement in the dev client (gate arch size, notch width, density of the bank dressing).
