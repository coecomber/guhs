# NOTITIES_FIX_STAD: Vadsrijk fix round 1, Zwamzoet (branch 110-fix-stad)

Only generator sources changed: `tools/vadsrijk/stad/zwamzoet.py`, `tools/vadsrijk/stad/stad_wereld.py`, and a 2-line change in
`tools/vadsrijk/wereld/bouwsels.py` (the stal). Tiles / WereldPlekken / POI_VADSRIJK.md are NOT committed (integration regenerates).
AutoCheck script: `tools/autocheck/vadsrijk_fix_stad.txt`.

## Root causes and fixes
1. **Grass over the plaza, paths and floors.** The map terrain inside the city box is y 64-65 (kaart.py "inland ground stays
   >= 64"), but the Zwamzoet scene's ground is y 63. Stamp air never cleared terrain, so 1-2 layers of knuffelgras covered
   the plaza and paths and grew inside every ground house. Fix: `stad_wereld.terrein_weg()` turns every air cell in the 3
   layers above the scene's ground into `cave_air` (= the tile writer's CARVE). `zwamzoet.huis()` also fills its interior with
   cave_air, which fixes the stal and the Haven houses built through `Z.huis` in bouwsels too.
2. **Lamp posts show only the bottom half.** `guhs:guh_lantaarnpaal` is a two-block block (`half=lower/upper`). Zwamzoet and
   bouwsels place both halves (verified in the tiles). But **`tools/vadsrijk/kamp/kamp.py` places `LAMP = "guhs:guh_lantaarnpaal"`
   with no `half`** (lines ~189, 216, 275, 364, 524), so only the lower post is placed and the lamp top never exists. Tiles
   t_0_15, t_0_16, t_1_15, t_-1_15, t_-1_16 (Knuffelkamp) have 14 half-less lamp posts.
   -> **FOR THE INTEGRATOR / agent kamp:** in kamp.py, set `half=lower` at y and `guhs:guh_lantaarnpaal[half=upper]` at y+1
   (`T.set(...)` in the tent template too), or call a helper like `zwamzoet.lantaarnpaal(S, x, y, z)`. Also check that y+1 is free.
   - New helper `zwamzoet.lantaarnpaal(S, x, y, z)` (both halves). Other call sites, all already 2-half: bouwsels.py haven
     (x=86 row along the Haven road; agent kamp owns it: check it stands beside, not on, the road) and kampvuren (x+3, z-3).
   - Zwamzoet lamps moved: plaza lamps from the r=14 ring (several stood on the main N/S road) to the plaza rim (r=21), skipping
     every exit; temple lamps now flank the stair foot (the old loop had an empty range: there were none); new `padlampen()` puts
     posts one block past the edge tiles of every ground path, alternating sides, only on free grass.
3. **Wolkenlift top: stuck.** The station sat 3 blocks below the plateau floor with a plank path at T (a 3-high wall), and the
   rim fences/lanterns stood in the way. New `stad_wereld.lift()`: the station is flush with the cap, roofed, with posts only on
   its corners; a 3-wide cherry-stair ramp (1 block per step, headroom carved) climbs to the plateau floor. Hats' houses keep a
   1.0 rad gap at the lift direction (one oost house stood right in front of the ramp). Bottom: a paved square plus a path to the
   nearest town path. POI `lift_<naam>_top` is now on the station, 2 blocks in front of the column.
4. **Building next to the lift (the tempel stem door).** The stem's foot flares wider than the door position, so a stem block
   stood in front of the door, and behind the door the stem is solid. New `zwamzoet.steeldeur()`: the door sits at the outside of
   the flare, with a 5x5x3 hall behind it (planks, carpet, lights, benches, barrel). The spiral stairs now start just past the door,
   and `deuren_vrij()` keeps the door front free. Small lamp mushrooms have no door any more.
5. **Walkability.**
   - Spiral stairs (6 caps) ended 2-3 blocks UNDER the cap: a dead end. They now wind on through the cap (headroom carved) and
     come out on the plateau. The hats' houses avoid the exit angle (`WENTEL_UIT`).
   - Bridges: the deck goes in half-slab steps (top/bottom slabs, never more than 1 block between cells), with headroom free
     (no rim fence or lantern on the deck). tempel->noordw and tempel->noordo were steeper than 1.4 per block (unclimbable): they
     now drop at most 0.85 per block and end in a small **wolkenlift tower** (up + down column, roofed landing) on the low cap.
   - Sporenpaden (hoekw-west, noordw-hoekw) were loose stepping stones with gaps and 2.3-block rises: now a continuous 2-wide cloud
     path, at most 1 up per step, headroom free.
   - Checked with a BFS walker over the scene (step up <= 1, drop <= 3, 2 free cells, lifts as edges): before, all 9 cap tops,
     the ordetempel and the Mika lookout were unreachable from the plaza; now every POI, shop door and interior is reachable,
     **also without using any lift**. (Only `hoed_tempel` = the cap centre under the temple building, which is not a real spot.)
     Tool: scratchpad, not committed. If wanted, it can be added under tools/vadsrijk/stad/.
6. **Interiors** (lead's choice): shops and houses have plank floors (roze_lakhout_planken) with a pink carpet runner from the door
   (in `huis()`, so the Haven houses get it too); ordetempel: a 2x2 checker of quartz + pink concrete, aisle in smooth quartz with a
   pink concrete line, the glowing tiles kept; stal: hay floor in the stall half (bouwsels.stal, `tapijt=None`). No grass inside
   anywhere now (see 1).
7. **More life, same footprint:** goods on every market stall (by cloth colour: melons/pumpkins, hay/honey, potted flowers,
   fern/moss, wool), wool banners (`vaandel()`) at the two big plaza exits, lanterns hanging under the bridges, flower strips
   outside along the side/back walls of the ground buildings (`tuintjes()`), lamp posts along the paths.

## Open / not done
- The Kloof (wereld/kaart.py) runs under the NE corner of the city box (scene x 144-200, z 0-30): the scene's ground layer at
  y 63 spans it as a thin sheet. Not touched (kaart.py is wereld's); integrator may exclude the city box from the Kloof.
- At the city box edge the ground is 1 block lower than the terrain outside (63 vs 64): a 1-block step, walkable.
- The spiral stairwell leaves an open hole in the plateau (it leads down the stairs, so you can always get out).
- The step-by-step render pipeline (render_stad.py) still uses zwamzoet's own `lift()` (chain shaft): render only.

## Resume point
Test: local regen (114 s, no unknown blocks) + AutoCheck `vadsrijk_fix_stad.txt`: 15 shots, all OK: plaza paving visible from above (no grass), lamp post whole (post + lamp head), survival ride up the tempel lift ends on the ramp at the temple stair, stem door with a lit recess + hall, ramp at the lift top, smidse plank floor + carpet, ordetempel quartz/pink checker floor.
