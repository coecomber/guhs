# NOTITIES_WERELD — Guhs 1.10.0 "Het Vadsrijk", slice wereld

Branch `110-vr-wereld` (worktree `guhs_vadsrijk/w-wereld`). Contract §2 + §10.

## What is here
- **The map**: `python tools/vadsrijk/maak_kaart.py` (from the project root, ~75 s) writes
  - 690 tiles `data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt` (tx -11..11, tz -11..18 = x -704..767, z -704..1215),
    **12.4 M blocks, 38 MB** (gzip, DataVersion 4790 = 26.1.2, so no DataFixer pass on load);
  - `data/guhs/structure/vadsrijk/wereld/portaaltempel.nbt` (the Guhmension ruin);
  - `WereldPlekken.java` (generated: every POI, the doors, viewpoints, light beams, campfires, tile range) and
    `POI_VADSRIJK.md` (generated; fixed text in `tools/vadsrijk/poi_vast.md`);
  - `tools/vadsrijk/_cache/kaart_bovenaanzicht(_klein).png`: a top-down check image of the real tiles (cache is git-ignored).
- Pipeline: `kamp/kamp_wereld.py` (Knuffelkamp from the render generator kamp.py) and `stad/stad_wereld.py` (Zwamzoet from
  zwamzoet.py, with shops + interiors, real wolkenlift columns, ordetempel interior + side door) run as subprocesses (the two kit
  folders share module names) -> `.npz`; `wereld/kaart.py` = the 2D plan (layout of zonekaart.py, world = map - 600);
  `wereld/bouwsels.py` = all set pieces; `wereld/tegels.py` = terrain columns + stamps + points, hollowed (fully enclosed opaque
  blocks are dropped; invisible because nobody can break blocks there), written by `wereld/nbtsnel.py` (numpy writer).
- Terrain is a **skin** (each column only goes down to its lowest neighbour - 1; caves get solid columns down to their floor).
  Nobody can dig in the dimension, so this is invisible and keeps the tiles small.
- Leaves are written `persistent=true`; sand never stands on air at the sea edge; an invisible barrier ring at the outermost
  blocks keeps the sea from flowing into the void.

## Code (feature/vadsrijk/wereld)
`WereldRegistry` (blocks vr_w_portaal / vr_w_poortsteen / vr_w_litteken_gras / vr_w_litteken_plant, item vr_w_vonk, events,
dev commands `/guhs vadsrijk wereld plek|deur|vlieg|littekens|uitkijk|vergeetportaal`), `Portalen`, `PortaalBlok`, `VonkItem`,
`WereldDeuren`, `WereldReizen`, `Uitkijken`, `Littekens`, `Later`, `TegelGeheugen`, `PlantBlok`, client `WereldClient`,
`WereldLucht` (sky), `WereldSfeer` (ambient). Resources: `tools/features/vadsrijk_wereld.py`.

### Hook API for the other slices
- `WereldDeuren.open(id, ServerPlayer)` (unlock + first-time ceremony), `open(ServerLevel, id)` / `sluit(ServerLevel, id)` (bazen,
  plain moves), `sluit(sl, id, true)` = keep shut, `ontgrendel/ontgrendeld`, `isOpen`. Ids `ordetempel`, `zwamtempel`.
- `WereldReizen.vlieg(p, Dok van, Dok naar)`; `WereldReizen.MAG_VLIEGEN` predicate (npc may lock the line until the camp story is done).
- `Littekens.SLEUTEL` = `"zone1_klaar"` in `SpelerData.extra("npc")` (boolean). `Littekens.zetKlaar(p, true)` sets it and re-syncs.
  The client re-tints the scar blocks (grey -> green grass / pink flowers) for that player only.
- `WereldPlekken.plek("<id>")` for every POI; `Portalen.BINNEN_GEWEEST` / `ONTWAAKT` gezien-flags.

## Kern adapters / notes for integration
1. **Template memory** (`TegelGeheugen`): vanilla's StructureTemplateManager keeps every loaded template. With 690 tiles this
   could grow past 1 GB on a server after everyone explored. Every 5 min the wereld slice forgets all map tiles (reload ~10 ms).
   Better in kern: forget a tile in `KaartFeature` once its 16 chunks are generated.
2. **AANKOMST**: kept at kern's `Gebieden.AANKOMST (0, 65, 1130)`: a low jetty on the south coast of Knuffelkamp with a wooden
   stair up the cliff; the camp plateau is at y 86. The balloon ship (airship dock, bell) is on the east dock at the cliff top.
3. **Sky**: drawn as an overlay after the vanilla sky (`RenderLevelStageEvent.AfterSky`), so kern's dimension type does not need
   `neoforge:custom_skybox`. The vanilla sun/moon stay; I add the glow, the second moon, guh constellations, far landmarks, the dragon.
4. Area for the dungeon: `wereld.gebied_zwamtempel_binnen` = box (300,-525)..(360,-390) if kern/bazen want a `voegVooraan` area.
5. The Guhmension structure uses the same biome list and jigsaw type (`guhs:flat_jigsaw`) as the kermis; spacing 34 / separation 14.

## Decisions (open for the user)
- Wild ground is **green grass** with pink meadow patches; towns use pink knuffelgras (LEESMIJ_STAD choice 1). Stems stay vanilla.
- Knuffelkamp kept the render's cliff (plateau 24 above the sea); kern's arrival point got its own little south jetty + stair.
- De Haven (airship mast) sits at the bay mouth (contract box), not at the city quay; the city keeps its quay/boats/lighthouse.
- The Zwamtempel inside follows the bazen slice's request (straight chain: voorhal, Lichtzwamkamer, Hendelkamer, Ridderzaal,
  hall; open gates that bazen fills). No inner moving door of mine.
- Return portals inside the Vadsrijk: Knuffelkamp south jetty, Zwamzoet (west of the plein), De Haven. `/guhs vadsrijk` also works.
- Viewpoint panorama = camera turning once round the needle top (10 s) + title card; no teleport (ronde 11).
- Ambient "seagulls" are pitched parrots (no vanilla gull); campfire crackle near checkpoints.

6. **`/time set` fails inside the dimension** ("There is no default clock in dimension guhs:vadsrijk", seen in the autocheck round).
   Kern's dimension type uses `timelines: #minecraft:universal` but no default clock; the day/night still runs on the overworld clock
   (my sky reads `GuhTime`). Kern may want to look at it (dev commands / night testing).
7. The gametest server has no Vadsrijk dimension, so my tests place a tile into the test area with a chunk-sized bounding box
   (the same call KaartFeature makes) instead of generating dimension chunks.

## Tests
- Gametests `-Pgt=VadsrijkWereld` (4): the arrival tile loads (64x320x64, jetty plank under AANKOMST, the ruin template has its
  portal), a tile placed with a small box puts exactly its own blocks, a door slides open/half/closed and is rebuilt exactly from
  its hidden copy, a self-built portal frame lights only when closed. All green.
- One autocheck client round (`tools/autocheck/vadsrijk_wereld.txt`, 24 screenshots in `run/screenshots/autocheck/`): the dimension
  generates the map (arrival jetty, camp, city, door ceremony + title card, Haven + airship flight, meadow, arena, scars grey and
  healed, fossil bridge, temple, Sporenwoud, caves, crystal chamber, viewpoint panorama with title card). No missing models or
  textures of this slice. Fixed after it: interiors (temple rooms, Opperzwam hall, ordetempel) were too dark -> glowing floor tiles
  and wall lights; the `plein` spot was inside the statue.

## Resume point
Everything in the brief is built. Possible next steps: more variety in the ring land (a few small hamlets), more cave branches,
proper sign texts on the shops (needs the 26.1 sign NBT format), a nicer far-dragon texture once the wezens model exists.
