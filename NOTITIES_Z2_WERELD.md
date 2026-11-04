# NOTITIES_Z2_WERELD - Guhs 1.10 "Het Vadsrijk", zone 2 "De Splinterpieken", slice wereld

Branch `110-z2-wereld` (worktree `guhs_vadsrijk/z2-wereld`, from 6ec8724ca). Brief: `../Z2_BRIEF.md` (+ FIX_BRIEF rules).

## The map
- `python tools/vadsrijk/maak_kaart.py --zone 2` (from the project root, ~80 s) builds only zone 2; without `--zone` (= `alle`) zone 1 is built
  exactly as before and then zone 2. `--zone 2 --alleen "tx,tz;..."` rebuilds single zone-2 tiles. Hook = the lines marked `[zone 2]`
  in `maak_kaart.py` (an argument, an early return, one call at the end, a 4-line `zone2()`); zone-1 output is unchanged.
- Output: 462 tiles `t_<tx>_<tz>.nbt`, tx -11..10, tz -48..-28 (x -704..703, z -3072..-1729; ~25 MB, ~8 M blocks),
  `Z2WereldPlekken.java` (generated), `POI_Z2.md` (generated), `tools/vadsrijk/_cache/plekken_z2.json` + `kaart_z2(_klein).png` (top view).
- **Not committed:** the tiles (integration regenerates). **Committed:** generator sources, `Z2WereldPlekken.java` and `POI_Z2.md`
  (new files the code needs to compile; regenerated identically by integration).
- Modules in `tools/vadsrijk/zone2/`: `plan.py` (every fixed coordinate), `kaart2.py` (heightmap + masks: massif in a cloud sea,
  geode bowl, lake basin, ravine with ledge ramps, burcht peak, flattened pads, road profiles, gondola corridor), `tegels2.py`
  (zone-1 `tegels.Wereld` on the zone-2 grid with snow/ice/geode materials, the frozen lake, the cloud sea), `vol.py` (fast numpy
  build volume), `bouw2.py` (all set pieces), `maak2.py` (tiles, top view, Java + POI).

### What is in it
- **Kandijkom**: open pink-quartz geode (crystal inner face, calcite, basalt outside, snow on the rim) with 4 gates; streets of
  knuffelklinkers, a pearl-tile plaza with the skating pond (packed ice), benches, lamp posts, a huge snow spruce with lights,
  snow-guh figures; 15 hot-chocolate houses (mokka brick, dark-oak eaves with end-rod icicles, thick snow caps, smoking chimneys,
  plank floors + carpet runners, fireplace glow): Kristalkapel (ordetempel branch, marble floor, altar, crystal spire), juwelier,
  smid, 2 shops, bank, cafe (+ terrace + secret chocolate cellar via a trapdoor behind the counter), 8 houses; stable + paddock,
  quest board, gondola valley station, pink crystal shards leaning out of the geode wall.
- **Dock** (south): pier on posts over the cliff, the pink guh airship moored east of it, bell, campfire on the dock road.
- **Gondola**: valley station (NE in town) -> top station on the Galmtop shoulder (306,151,-2616); double chain cable, 2 pylons,
  2 cabins on the cable; a free corridor is cut in the terrain under the cable.
- **Galmhellingen**: rising snowy east with ridged peaks, the Galmwolf arena (flat bowl r 32 at y 120, 8 frozen pillars, needle
  ring, the Galmwolf's needle), 4 needle fields + loose needles (packed/blue ice, glassy tips), 3 avalanche lanes (bare snow,
  boulders and broken spruces at the foot), the secret crystal grotto behind a frozen waterfall.
- **Knuffelmeer**: frozen lake (clear ice at 84, blue ice at the rim, packed-ice cracks), 10 wool plushies under the ice (heads
  looking up), ice-fisher island hut, shallow open water with floes at the outflow, the dragon's saddle (flat r 30 at 125, grey
  scorched ground, grey crystal scales, pink shards), the secret ice cave with a snow-guh.
- **Splinterkloof**: ravine 40 wide, floor 64 with a frozen stream, vertical layered walls (deepslate/tuff/calcite/packed ice);
  4 ledge ramps up the south wall (exits), 5 hanging bridges (x 0 = wide dark-oak bridge to the burcht), 3 Mika drilling rigs
  (tower, ladder, platforms, drill arm down the wall, boiler smoke, crystal piles, flag), the secret Boormeester cave in the north
  wall (tunnel behind icicles), grey crystal outcrops, the Mika fort on the north plateau.
- **Spiegelburcht**: peak plateau at 175, grand stair from the bridge (landings, lanterns), forecourt, grey crystal palace (calcite
  hull, tuff base, mirror-glass strips, front towers with glass spires, central crystal spire with a pink heart), moving door
  (`spiegelburcht`, 7x9, hidden copy at y 8), voorhal + 3 puzzle rooms (checker floor, mirror panels, sea-lantern grid, puzzle spots
  marked with chiseled quartz), open AIR gates (5x5) for bazen, the round boss hall with a grey glass dome + calcite ribs and 6
  mirror alcoves, the shard pedestal; secret ladder to the roof with a Mika hat.
- **Edge**: cloud stamps around the coast, ice floes on the sea, 4 far crystal spires rising out of the sea (landmarks).
- Campfires: 1 dock + 4 Galm + 4 Meer + 5 Kloof + 1 burcht stair. Viewpoints: galmtop, meerkijk, kloofkijk (icy needles, spiral stair).
- Decoration: spruces with snow (sparse, not on paths/pads/town/lake/ravine), snow layers on snowy grass, amethyst sprinkles in
  the Galm, ice flowers in the low south.

## Code (`feature/vadsrijk/zone2/wereld`)
- `Z2WereldRegistry` (called from `wereld.WereldRegistry.register`, one marked line): adds `Z2WereldPlekken.GEBIEDEN` to
  `kern.Gebieden` with `voegVooraan` (no kern edit), adds the burcht door to `WereldDeuren.EXTRA`, zone-2 viewpoints (zone 1's
  `Uitkijken.panorama`), right-click hooks, dev commands `/guhs vadsrijk z2wereld plek|vlieg kandijkom|haven|opnieuw|gondel op|neer|uitkijk`.
- `Z2Reizen`: airship line Haven <-> Kandijkom (own class; zone-1 `WereldReizen` untouched). Kandijkom bell -> Haven; **sneak +
  right-click the Haven bell** -> Kandijkom (plain click still goes to Knuffelkamp). Gate `MAG_NAAR_KANDIJKOM` = `zone1_klaar` + lv 20
  (creative bypasses), refusal hook `WEIGER` (npc may set the captain's text), first-flight opening scene (storm, snow, ice knocks,
  lights out, howl, needle bursts, geode, chocomelk line) or the npc slice's `EERSTE_AANKOMST`.
- `Z2Gondel`: ride scene along the real cable (12 s, camera in a cabin), teleport to the other station; gate `MAG_RIJDEN`.
- Client (`client/`, called from `wereld.client.WereldClient.init`, one marked line): `Z2Lucht` (overlay after the sky: pale ice
  veil + horizon band, mother-of-pearl clouds drifting, green-pink aurora curtains at night, a big moon breaking through a crystal-
  spire silhouette, the giant tree), fog colour pale icy by day / blue-violet at night, `Z2Sfeer` (wind = Balto's wind sound,
  ice creaks, crystal chimes, cosy town; snowflakes, glints in the burcht, campfire crackle).
- Zone-1 wereld files touched (marked `[zone 2]`): `WereldDeuren` (`EXTRA` list searched after `WereldPlekken.DEUREN`),
  `WereldRegistry` (1 line), `client/WereldClient` (1 line), `client/WereldLucht` (skip zone 1's horizon landmarks in zone 2).
- Resources: `tools/features/vadsrijk_z2_wereld.py` (in FEATURES, one line): sky textures `textures/vadsrijk/wereld/z2_*.png`,
  texts (area names `vadsrijk.kern.gebied.z2_*`, door, airship, opening, gondola, viewpoints).

## For the other slices
- All coordinates: `POI_Z2.md` / `Z2WereldPlekken`. Door id `spiegelburcht` (WereldDeuren API). Burcht gates are open AIR.
- npc: the captain at Haven can call `Z2Reizen.naarKandijkom(p)`; set `Z2Reizen.WEIGER` for the captain's refusal; set
  `Z2Reizen.EERSTE_AANKOMST` to replace the opening; the gondola guh may gate `Z2Gondel.MAG_RIJDEN`.

## Tests
- **Walkability** (`loopcheck.py` from f-wild, copied to a scratch dir with one fix: `snow_block` is a full block, not a snow
  layer; run with `--tegels <this worktree's tiles> --box=-704,-3072,703,-1729`): VAL 10, TRAP 28, PAD 0, LEEG 0.
  - The 10 VAL are single cells (1-2 blocks, 2 up to get out) inside tree canopies / needle feet / the kloofkijk spiral stair; no pits,
    rooms or water traps. The 28 TRAP are chairs (stairs used as furniture: cafe, houses, plaza benches), not real stairs.
  - Fixed on the way: Mika fort sat in a terrain hollow (now on a pad), the cafe cellar ladder (now up to the floor), single-cell
    terrain pits (filled), trees on slopes (pockets under leaves), the ravine's open ends at the coast (closed), the Spiegelzaal
    mirror alcove that blocked the gate from the Prismazaal, campfire tents on paths, the burcht campfire on the steep cone.
  - Also a POI check (every generated stand spot: feet+head free, floor under it): only the intended block spots are left
    (`galmwolf_pilaar_*` = pillar centre, `*_altaar`, `gondel_*_kabel`).
- **AutoCheck** (`tools/autocheck/vadsrijk_z2_wereld.txt`, 30 screenshots in `run/screenshots/autocheck/z2_*.png`) + a short
  re-check of the sky/arena/opening (`vadsrijk_z2_lucht.txt`, `z2b_*.png`): Kandijkom (plaza, tree, geode wall, cafe, kapel, cellar),
  gondola station + ride + top station, Galmhellingen needles, Galmwolf arena with pillars, frozen lake, dragon saddle, ravine
  + drilling rig + Boormeester cave, bridge, Spiegelburcht outside, door ceremony + open, rooms, boss hall, viewpoint panorama,
  day sky (pale veil, pearl clouds, crystal spires, giant tree) and night sky (green-pink aurora, the big moon behind the spires).
  The opening scene plays (snow, the chocomelk line). Found and fixed: the body could drop through not-yet-loaded chunks during a
  long flight scene -> `Z2Reizen.landOp` puts the player back on the landing spot during/after the scene (airship + gondola).
- No gametests added (world code only; the tile mechanism is zone 1's, covered by `VadsrijkWereld` tests).

## Open / for the user
- The plushies under the ice are visible but subtle from above (clear ice over dark water); maybe brighter wool or lights later.
- Walls of the houses are `pastelbaksteen_mokka` (reads more like light toffee brick than dark chocolate).
- Haven side of the airship: sneak + right-click the Haven bell (plain click = Knuffelkamp). The npc captain should offer it too.
- The Galmwolf howl in the opening is a low elder-guardian sound (no vanilla wolf howl in 26.1).

## Resume point
Everything in the brief is built and checked. Regenerate with `python tools/vadsrijk/maak_kaart.py --zone 2` after any generator
change (POI_Z2.md + Z2WereldPlekken.java come along). Integration: run without `--zone` (both zones) once after merging.
