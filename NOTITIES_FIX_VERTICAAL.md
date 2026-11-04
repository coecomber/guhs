# NOTITIES_FIX_VERTICAAL: Hoedenpaden + Druipschacht (fix round 1, 2026-11-04)

Branch `110-fix-verticaal` (worktree `guhs_vadsrijk/f-verticaal`). Ideas 6 and 7 of `VOORSTEL_EPISCHER_ZONE1.md`; the zone did not grow.

## What changed
Three new generator modules in `tools/vadsrijk/wereld/`, hooked in with two lines in `bouwsels.bouw_alles` (marked `# verticaal`,
right after `sporenwoud(W, K, P)`):

- `paadkit.py`: a small kit for walkable paths. Heights are counted in half blocks (bottom slab = half step, stair = full
  step), with rope bridges along a segment, square spirals (stairs or a slab ramp) with a flat top platform and a fenced
  stairwell, rails (fence/wall with real connections), occupancy of the world built so far, and a `Bouw` that writes one stamp.
- `hoedenpaden.py`: **De Hoedenpaden** in the Sporenwoud, a second layer 30-50 blocks up.
  - It **scans the real Sporenwoud stamp** for the giant mushrooms (stems + cap tops), so it follows the mushrooms if the
    `wild` agent changes the Sporenwoud. From the caps with a top 30-50 above the ground, it picks a connected network
    (~12-14 caps, bridges up to 30 long; a bridge rises at most 0.85 half-block per block and must not cut through other mushrooms).
  - Each cap gets a plank deck (stand = cap top + 2) with a fence rim. The rim is open only where a bridge lane touches it.
  - **Rope bridges**: 3 wide, slab half steps, spruce fence rails, lanterns on some posts, and spore lanterns (chain +
    pearlescent froglight) hanging under them.
  - **3 lookout huts**: 5x5, plank floor + carpet, glass windows, an open doorway (no door), and a red mushroom roof.
  - **Elite nest**: on the highest cap of the network, with a hay/log rim (open where bridges come in), moss, hay and sniffer eggs.
  - **Secret**: a treasure room (closed hut, chest + gold block) on a lone cap outside the network. Only a narrow (1-wide,
    fenced, unlit) plank walk leads there. If no natural cap fits, the generator grows a small red mushroom of its own.
  - **3 ways up** from the forest floor, placed next to the network on level ground (moss plinth + a soft half-step mound,
    away from paths/light beams/buildings):
    - *De Wenteltrapzwam*: dark-oak stairs (2 wide) spiralling round a square mushroom stem, with corner landings and a red parasol roof.
    - *Het Wortelpad*: a 3-wide mangrove slab ramp (1 up per 2) round a giant dark-oak root, with a mossy crown and a pink cap.
    - *De Holle Steel*: a hollow stem with a doorway at the foot, a ladder inside, and a doorway onto a platform at the top.
    Every tower has a flat bridge to its cap.
  - Falling off is allowed (you land in the forest). The rails make the paths safe; the cap domes outside the rims are open.
- `druipschacht.py`: **De Druipschacht** in the Druipgrotten, one vertical cave, 80 high (floor y 31 -> skylight y 111 in the
  current build, centre about (-498, -474)).
  - The place is **searched**: the highest hill top in the Druipgrotten whose rock column has no existing cave within
    radius 21 (so it keeps fitting if `wild` changes the caves). The shaft never breaks through the hill except at its
    skylight. Terrain columns are made solid down to the floor (`lo_extra`).
  - **Crystal skylight**: pink stained glass with guh-crystal ribs, flush with the hill top (you can walk over it; daylight
    falls in), and crystal clusters round it on the hill.
  - A lake (radius 8) on a crystal/calcite floor, and **3 waterfalls** dropping from the dome into the lake. Dripstone,
    amethyst and guh crystals on the ceiling and walls, plus invisible light blocks in the void.
  - **Spiral ledge path**: 3 wide, polished deepslate with slab half steps, and a deepslate wall rail on the void side. It
    climbs ~2 turns from the lake shore to a **balcony** under the dome, with 4 wider ledge niches on the way.
  - **Tunnel**: a short tunnel from the nearest cave node (N4 now) follows the real node floor. It is flat and 3 wide, and
    it joins the spiral at the same height (y 41).

## New spots (WereldPlekken ids after regeneration, current build)
For leven/npc/wezens integration (spawners, loot, secrets):

| id | what | now |
|---|---|---|
| `hoedenpad_elitenest` | elite nest centre (stand) | -348, 116, 236 |
| `hoedenpad_uitkijkhut_1..3` | inside each lookout hut (stand) | -325,112,185 / -353,113,146 / -324,112,239 |
| `geheim_hoedenschat` | inside the secret treasure room | -365, 113, 125 |
| `hoedenpad_schat_kist` | the chest in it (fill with loot) | -365, 113, 124 |
| `hoedenpad_wezen_1..8` | plain cap decks: creature spots | see POI_VADSRIJK.md |
| `hoedenpad_trap_wenteltrap / _wortelpad / _holle_steel` | foot of each way up (ground) | -372,71,174 / -309,66,129 / -291,74,220 |
| `hoedenpad_top_*` | top platform of each way up | |
| `hoedenpad_netwerk.hoeden.N` | every deck of the network (stand) | |
| `druipschacht_ingang` | where the tunnel leaves the cave node | -463, 41, -470 |
| `druipschacht_bodem` | lake shore at the bottom | -499, 32, -484 |
| `druipschacht_balkon` | balcony under the dome (top of the path) | -504, 85, -484 |
| `druipschacht_richel_1..4` | ledge niches on the spiral: creature spots | y 42 / 55 / 67 / 80 |
| `druipschacht_plafond_1..6` | air just under the ceiling: hanging/flying creatures | |
| `druipschacht_waterval_1..3` | foot of each waterfall | |
| `druipschacht_meer` | lake centre (water) | |
| `druipschacht_lichtschacht` | on the hill, on top of the skylight | -498, 115, -474 |

The coordinates shift if the Sporenwoud/caves change (the network and the shaft are searched), so use the ids. They land in
"Other" in POI_VADSRIJK.md (`geheim_hoedenschat` under secrets). I did not touch the SECTIES list in maak_kaart.py.

## Tests
- Walkability flood fill on the composed map (my dev script `loopbaar.py` in the scratchpad: stand states at half-block
  surfaces, steps up <= 1 block, ladders/water climb, real falling that never passes through floors):
  - Hoedenpaden: from the three tower feet, all 20 spots are reachable (nest, huts, secret + chest, all decks, tower tops),
    and **0 trap states** (every reachable spot can walk/climb/fall back to the forest floor).
  - Druipschacht: from `druipschacht_ingang`, the bottom, the balcony, all 4 ledges and the waterfalls are reachable, with **0 traps**.
  - Sanity: the checker does flag a 1x1 pit 3 deep and a stepped pit with 1.5-block steps.
- Map regenerated locally (690 tiles); tiles/WereldPlekken/POI are NOT committed (FIX_BRIEF).
- AutoCheck: `tools/autocheck/vadsrijk_verticaal.txt` (see below for the result).

## Notes / open points
- The network, towers and shaft are chosen from the world as it is built. After `wild` merges, integration should
  regenerate once and run the check again (the dev check script is described above; it is ~70 lines on top of
  `paadkit.bezetting` + the tile composer).
- The full build and the Sporenwoud-only build give different mushrooms (sporenwoud() depends on `W.bezet` of earlier
  stamps), so always look at a full `maak_kaart.py` run.
- The hut doorways are open (no door blocks), so nothing can get stuck closed.
