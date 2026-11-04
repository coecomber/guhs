# NOTITIES_FIX_WILD — Vadsrijk fix round 1, agent WILD (branch 110-fix-wild)

Scope: the wilderness generators in `tools/vadsrijk/wereld/` (Klodderweide, Sporenwoud, Druipgrotten, de Kloof, Zwamtempel
mountain/exterior/rooms, needles, scars, trees/decoration). Only generator sources are committed; regenerate with
`python tools/vadsrijk/maak_kaart.py` (~2.5 min now) at integration.

## New tool: `tools/vadsrijk/loopcheck.py` (walkability checker, for the integrator)
Reads the generated tiles, builds the survival movement graph (walk, step 1/2, jump 1, fall, climb ladders/vines/water) per
4x4-tile window and reports:
- `VAL` trapped regions (reachable but no way out) with the cheapest way out (`kortste uitweg N blok omhoog bij ...`);
- `TRAP` / `TRAP-` stair tops/bottoms that lead nowhere or are blocked by a fence;
- `PAD` dirt-path blocks with a solid block in the 2 cells above (trees, caps, rock).
Options: `--box=x0,z0,x1,z1`, `--json f`, `--route=x,y,z` (how does a player get there), `--soepel="x,y,z;x,y,z"` (can you walk
A to B without a single jump or drop; used for the needle spirals). Whole map ~2 min. Exits = plain ground (or water)
columns on the window ring, so roofs/caps don't count as "outside".

Result: OLD map 1280 traps (Kloof floor 7100 + 5700 stand spots, Druipgrotten 4000 + both entrance tunnels, temple path
basin 900, hollow mushroom, crevices in the Kloof walls) -> NEW map: 4 single-cell spots, none in my areas
(37,-99 = Zwamzoet edge; 72,26 + 74,25 = inside Zwamzoet; 42,1215 = Knuffelkamp edge).

## What changed (user feedback -> fix)
1. Trees on paths: trees need 6+ blocks from any path surface and 5+ from buildings/needles (`padafstand`); giant Sporenwoud
   mushrooms keep stem+bend 4 blocks off paths and stay clear of the needles (cap radius + 23); campfire camps moved
   6-8 blocks BESIDE their path (`naast_pad`) on a levelled plaza (logs/tent were on the route). Paths that end at a needle
   now stop at the foot of its spiral (they ran under the rock).
2. Slime hill (Slaapheuvel): solid down to the LOCAL ground in every column (min with its centre ground), so the edge meets
   the terrain; the treasure tunnel gets mossy stairs down if the ground north of it is higher.
3. Bone path: the fossil bridge used to lie ALONG the Kloof (both ends on the gorge axis, one end hanging at ~y 52). It now
   crosses the Kloof square: ends `FOSSIEL = [(151,-181), (119,-219)]` on both rims, level landings at the deck height
   (`K.dek`) in `kaart.py`, the `brug` path ends on the south landing, the `berg` path starts on the north landing. Skull moved
   13 blocks beside the path (solid).
4. Zwamtempel: the mountain path is rerouted north of the Kloof (it crossed the gorge twice on earth dams that made a
   900-spot basin): bridge -> mountain flank -> foot of the temple stairs (ramp deck..TP-7, 9 wide trench). The front
   stairs were half outside the stamp box (row 0 + landing silently dropped) and one row short: box enlarged (z to -361),
   7 full-width rows TP-6..TP with solid under and air above, side walls, a landing at the foot. The arch of the main door
   no longer opens the voorhal roof (you could drop from the roof into the closed temple).
5. Needles (all 5): new spiral `naald()`: height = function of the angle, rises 0.42 x inner radius per radian = at most
   half a block per block -> top/bottom slabs, never a jump (`--soepel` check: zwamnaald and druipspits walk from the foot
   to the platform without jumping). Walkway 3.4 + 18% wide, cut into the rock (headroom carved), fence rail only on the
   outer rim (none near the ground or at the arrival), the spiral cuts its own way into the top platform, no fences within
   2 blocks of the arrival, fences on the platform only where it drops onto the lower ramp. Starts where the path arrives.
   POI `naaldtrap_<naam>` = the foot of the stair. Mushrooms keep away. Flag moved higher. Stable seed (was `hash()`,
   which made the tiles differ every run).
6. General terrain: (a) **Kloof ramps** (`Kaart.kloofpaden`): every ~68 blocks a 5-wide path ramp along the wall from the
   stream bank (y 34) to the rim, alternating sides, slope ~0.7 (7 ramps); (b) **pit fill** (`Kaart.ontsnap`): priority flood
   from the sea, every wilderness column must reach the coast with steps of at most 1 up (water at its surface); ~90 columns
   raised (Kloof-wall crevices). Not applied under Zwamzoet / De Haven / Knuffelkamp (z >= 700). The Kloof now starts at
   (75,-160), clear of the Zwamzoet box (it cut under its NE corner).
   Druipgrotten: every room has a flat floor at its POI height (`voet`), tunnels run floor to floor along a straight line
   (no bumps), entrances start 7 blocks outside the hill at ground level, the crystal crack is 3 high x 2 wide floor to floor,
   floor crystals in the crystal chamber 25% -> 8%.
   **Under-the-map holes** (audit of agent veilig: 48 in the Druipgrotten): where a stamp carves into terrain, the hollow
   space under the neighbouring terrain skin showed through the cave walls. `Wereld.onder_uitgraving` (tegels.py) now
   extends the skin of every carved column and its 2-column neighbourhood below the lowest carved cell (all stamps).
   **Path steps** (audit: 24, mostly Druipgrotten ridges): every path except the mountain ramp and the Haven road gets a
   smooth height profile along its length (slope <= 0.7, flat across) (`Kaart.padprofielen`). **Kloof stream**: dry banks
   at y 34 on both sides of the stream (a walk along the bottom to the ramps). **Big ponds** (Klodderweide): 3 small moss
   islands each, so land is within ~10 swims.
   Waterfall room (Sporenwoud): its egg-shaped cavity went 8 blocks below the floor (a pit all round the floor) -> filled
   to the floor; the pool sat in a pit -> a clearing at floor level in front of it + stairs up to the forest.
   Slime hill: the treasure chamber shell closed the end of the tunnel -> re-carved.
7. Interiors (user choice): temple rooms + hidden room + Opperzwam hall: pink/white marble (quartz block / pink concrete /
   quartz pillar; froglight tiles kept); hollow fallen mushroom: flat cobble/gravel floor through the stem + cobble stairs
   out at both ends; waterfall room: stone/cobble/gravel instead of moss. (Stal uses Z.huis = planks already.)
8. Life without mobs: Druipgrotten tunnels every ~19 blocks at the side: abandoned miner camps (barrel, bedroll, lantern),
   old rail ends with barrels, crystal clusters, glowing mushroom patches, lanterns on chains. Sporenwoud: 34 spots 4-11 blocks
   beside the paths: forest camps, glowing fairy rings (shroomlight centre), fallen logs with moss, spore lamp posts.

## POI changes (ids unchanged unless noted)
`fossielbrug.van/naar` (new ends), `kampvuur_zwamtempel_1/2` + `ridder_*` + all other campfires (moved beside their path),
`uitkijk_*` (same), new `naaldtrap_<naam>`, `grot_knopen` (same heights), `kristalkamer` (same).

## Audit (agent veilig's audit.py on my regenerated tiles)
Before (old map, their report) -> after: under-the-map 49 -> 0, path steps 24 -> 3 (2 left at a Druipgrotten ridge
-263,90,-301 and 319,68,-325 by the Kloof ramp), water without exit 12 -> 3 (1 column of the big pond; others in Zwamzoet),
Kloof / Zwamtempel / Druipgrotten pits gone; the 6 remaining pits are all in Zwamzoet/Knuffelkamp. Trees on paths left: only
Zwamzoet + Knuffelkamp (not mine). Grass under a roof in my areas = campfire tents (now carpet) and low mushroom caps.
POIs not on the network in my areas: `geheim_tempelkamer` (behind the cracked wall, bazen opens it: intended),
`geheim_schildpad` (island: swim/boat), `quest_tuba` (the POI is the gold block itself); waterfall + slime chamber fixed.

## Open / for others
- Kloof edges have no fences (the gorge can be walked out of everywhere now via the ramps); add rails at path rims if wanted.
- Single-cell spots at 37,-99 / 72,26 / 74,25 (Zwamzoet, agent stad) and 42,1215 (Knuffelkamp, agent kamp).

## AutoCheck
`tools/autocheck/vadsrijk_fix_wild.txt` (spectator mode, 12 shots in `run/screenshots/autocheck/fw_*.png`): needle spiral +
top platform, needle foot, slime hill edge on the ground, fossil bridge across the Kloof (dry stream banks below) + both
ends, a Kloof ramp, temple stairs (full width, landing), cave entrance + flat cave room, hollow mushroom. All rendered, no
missing textures. (First run was in creative: the camera fell to the ground; rerun in spectator.)

## Resume point
All 8 points built + checked with loopcheck. Commit = generator sources only.
