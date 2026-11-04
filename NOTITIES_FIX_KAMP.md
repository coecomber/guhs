# NOTITIES_FIX_KAMP — Vadsrijk fix round 1, Knuffelkamp + De Haven + the airship flight

Branch `110-fix-kamp` (worktree `guhs_vadsrijk/f-kamp`), from 6ec8724ca.

## What changed
### Map generators (commit only .py; integration regenerates)
- **Arrival stair** (`wereld/bouwsels.kamp_zuidsteiger`): the stair stopped at y 73 because it broke off as soon as the column
  under it was lower than the step (the cliff edge). Now it climbs one step per block until the ground ahead (5 wide, 3 deep)
  is no higher than the step, as a cutting into the cliff (stone-brick walls, solid roof filled up to the grass, hanging
  lanterns in the tunnel part, fences + lanterns where it is open), then a 3-deep dirt-path landing on the plateau. It ends at
  `aankomst_trap_top` (0, 87, 1068). Supports go down to the sea floor (no air pockets).
- **Ways out of the water**:
  - `bouwsels.zwemtrap(S, x, z, uit, y_top)`: a ladder from the sea floor to the deck level on a support column, with the
    railing above it opened. Used on the arrival jetty (8 ladders: both sides near the stair and halfway, the platform sides and the far end), the Haven
    quay (every 14 blocks), the Haven pier (west end + 4 along the sides). Under every ladder: sandstone (the sea floor is only a skin).
  - Knuffelkamp island (`kamp/kamp_wereld.uitgangen`): every ~14 blocks of coast (50 exits) a tiny sand beach at the waterline,
    a ladder up a stone-brick buttress against the cliff and a plank walk on top that ends on the plateau (side fences where it
    drops, a lantern post). Max swim to an exit is ~7 blocks; the south jetty and the airship jetty cover their own stretch.
  - The high airship jetty (`kamp_wereld.steiger_ladders`): ladders from the water up its posts at 2 spots on both sides and
    at the end platform (railing gap at each).
  - De Haven: two stone-brick stairs into the water from the quay (z 456, 528).
- **Lighthouse cellar** (`kamp_wereld`): one ladder shaft from the cellar floor up to the tower floor against a solid column, ONE
  spruce trapdoor at the top (same facing as the ladder, so the open trapdoor is climbable), air above it.
- **Interiors** (`kamp/kamp.py`): tents = spruce planks + carpet (pink runner, white sides); ordetent = planks + red runner +
  pink carpet; class temple cella = quartz/pink concrete checker (marble look); lighthouse keeper's house got a door (it had none),
  a plank floor and a path step outside. Haven houses = planks + coloured carpet runner; pakhuis = planks + runner off the
  Mika footprints.
- **De Haven** (`bouwsels.haven`, `mast`, `guhvlag`, `kraan`, `kratten`, `loper`, `kei`):
  - organic outline: `kaart.py` HAVEN_DORP flat area is now a noisy rounded shape (superellipse + noise) with a 26-block
    smoothstep rim (`K.haven_d`); `tegels.py` knuffelgras follows it with a dithered edge. HAVEN_DORP x1 125 -> 120 (inside the
    contract box). The zone did not grow.
  - quay follows the shore from z 438 to 550 and runs out into the beach (narrower ends, no hard stop); rocks/boulders on the
    beaches and in the shallows outside the quay, boulders + azalea shrubs on the soft rim.
  - grand airship tower: octagonal pink hall with 4 arched doorways and windows, 3x3 core with the ladder (one cherry trapdoor at
    the top), timber lattice with ring beams and hanging lanterns, railed top platform with lanterns, two guh flags, beacon and
    big guh ears. Moored airship kept.
  - arrival square (heart tiles, guh-face centre, 4 lamp posts) with the landing spot `luchtschip_haven_landing` (92, 65, 484),
    big arrival arch (zwamzoet.guhpoort, face towards the square) over the road at z 462, flags on it.
  - two cranes on the quay, crate/barrel stacks, lamp posts and guh flags along the land side of the quay (never on the walk).
- **Sightseeing flight set pieces** (`bouwsels.rondvlucht`): an airship at anchor in the sky over the sea (40, 118, 760) with a
  railed balcony on its south side (POI `rondvlucht_schip`), and 5 cloud banks beside the route.
- `maak_kaart.py`: `HAVEN_LANDING` = the new `luchtschip_haven_landing` (one line).

### Java (feature/vadsrijk/wereld)
- `WereldReizen`: the flight is now a ~32 s sightseeing flight (640 ticks; camera keyframes every 2 ticks from a Catmull-Rom
  route, glances at the passing airship and the Zwamzoet skyline, cloud particles between z 560..720, ends looking at the arch /
  the camp). 4 waving RijkGuhs (Emote.ZWAAIEN, no AI, not saved) are spawned on the passing ship's balcony when you come near and
  removed 12 s later. The player's body is invisible and follows the camera (so chunks load around the camera).
  Safe landing: `veiligeLanding(sl, plek)` loads/generates the 3x3 chunks first, then searches a solid non-liquid floor with 2
  free blocks above (y-3..y+24, spiral 4 blocks), fallback = top solid block; never below the map. Re-checked 1.5 s after.
  Same code both ways. Skippable after the first time (`gezien` flag `vr_w_rondvlucht`): the client (WereldClient) reads the raw
  sneak key during a skippable flight and sends `Overslaan`; the server lands you and plays a 1 s arrival shot.
  Payloads `vr_wereld_vlucht` (S->C) and `vr_wereld_vlucht_overslaan` (C->S). `vlieg(p, van, naar, dev)`: the dev command
  `/guhs vadsrijk wereld vlieg haven|kamp` ignores MAG_VLIEGEN.
- Lang: `vadsrijk.wereld.luchtschip.overslaan` (tools/features/vadsrijk_wereld.py).

## Notes for integration / other owners
- npc: the Haven NPC "luchtschipper" stays at `luchtschip_haven_kapitein` (86, 65, 491); players now land at
  `luchtschip_haven_landing` (92, 65, 484). Haven stalls got a plank floor (the barrel ring was a 2-deep pit on the 63 shore).
- Camp lamp posts (kamp.py) now place both halves (`[half=lower]` + `[half=upper]`); trees keep 3 blocks off the paths. npc's fallback flight (NpcPlekken.VLUCHT_*) is unused when wereld is present.
- Zwamzoet's own quay/jetties (stad slice) also need ladders out of the water: `bouwsels.zwemtrap` can be reused.
- The lighthouse top (`vuurtoren_top`) has no way up inside the tower (no stairs); not in my brief, left as is.

## Tests
- Slice tool (scratchpad kampfix/kk.py: top-down crops + vertical slices of the real tiles): the stair reaches the plateau,
  the cellar ladder 87..92 + trapdoor 93 + air above, the tower ladder to the top trapdoor, exits all round.
- f-veilig's audit.py on the regenerated tiles: Knuffelkamp/Haven have no more under-the-map holes, no pits (except below),
  no water without an exit; tent grass floors gone.
  Left (false positives, noted for veilig): `vuurtorenkelder` "pit" (the audit does not know an open trapdoor above a ladder
  is climbable); "stairs that don't connect" = the A-tent roof stairs; "trees on paths" = bench logs/crane post/crate wood;
  "grass under a roof" = under eaves/overhangs outside the buildings, the temple porch eaves.
- AutoCheck `tools/autocheck/vadsrijk_fix_kamp.txt` (18 shots, run/screenshots/autocheck): stair tunnel up to the plateau,
  jetty ladders, island exit ladder + sand beach, cellar ladder shaft, Haven arrival (arch, cranes, lamp posts, tower hall),
  flight (passing airship, the landing on the square). Landing was safe (92, 65, 484). The skyline shot showed the city not
  yet loaded -> slow-down + body lead added afterwards (compiled, not re-run in a client: one AutoCheck run per agent).

## Open
- In-client check of the flight after the slow-down (does the city load in time? otherwise move the skyline pass closer).
- Sneak-skip not exercised in the autocheck (needs a second flight).
