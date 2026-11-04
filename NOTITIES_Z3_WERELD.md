# NOTITIES_Z3_WERELD - Guhs 1.10 "Het Vadsrijk", zone 3 "De Schemerwortel", slice wereld

Branch `110-z3-wereld` (worktree `guhs_vadsrijk/z3-wereld`, from ab6d952ac). Brief: `../Z3_BRIEF.md` (+ G_BRIEF rules).

## Layout decision (read this first)
- Zone 3 is in the EAST: box x 1600..3000, z -700..700, trunk axis (2300, 0); tiles tx 25..46, tz -11..10 (484 tiles).
- The map is the approved render design (renders-schemerwortel/_bron) **MIRRORED in x**: world X = 2299 - xr (xr = render/kit x
  relative to the trunk), Z = zr. The renders were made for the old west box with Honingtak on the branch pointing to zone 1;
  mirrored, Honingtak still points WEST to zone 1/2 (airship mast at x ~1677) and the far east (Hoogvads) lies behind the tree.
  Block states are mirrored with it (facing east<->west, stair shapes, hinges, rotations: `maak3.spiegel`).
- Heights: crown 200-291 (nest floor 280; lowered from the renders' ~306 for headroom under the 320 limit), branches ~150-235,
  hollow trunk 75-196, ground ~64-95 (root hill to ~90 at the trunk), Wortelduister 6-60, Hart dungeon ring floor 50.

## The map (`python tools/vadsrijk/maak_kaart.py --zone 3`, ~2.5 min with 5 workers)
- Hook in `maak_kaart.py` (lines marked `[zone 3]`): `--zone 3` builds only zone 3, `alle` builds 1, 2 and then 3; `--werkers N`.
  Zone 3 runs as its own process (`tools/vadsrijk/zone3/maak3.py`), because its render kit shares module names with zone 1's kits.
  `--alleen "tx,tz;..."` works too. `--geen-cache` is accepted (zone 3 has no cache).
- Modules in `tools/vadsrijk/zone3/`:
  - `kit/` = copies of renders-schemerwortel/_bron (boom, honingtak, plekken, schemerkit + the shared kits). Changes are marked
    `[zone 3 kaart]`: organic crown (lobes + stronger noise, lopsided domes), lower crown top (cap 291), varied branch thickness
    (not Honingtak's branch), roots that dive into the ground at their ends, a walkable shore into the cloud sea (no sheer cliff),
    shallow sap lakes in the roots, no kit lifts/werf (bouw3 builds them), Mosvoet lift station flush with the ground.
  - `plan3.py`: every fixed coordinate (rel), the mirroring, areas with y-ranges, mob zones, paths, wind columns, `plekken()`.
  - `poi3.py`: writes `Z3WereldPlekken.java` + `POI_Z3.md` + `_cache/plekken_z3.json` (also standalone: `python poi3.py`).
  - `bouw3.py`: the game parts on top of the kit (see below); every spot is read back from `plan3.plekken()` so map and POI match.
  - `maak3.py`: per tile a k=1 window of the kit + bouw3, mirrored, hollowed (opaque blocks + leaves deeper than 2 inside a mass),
    written as nbt; top view `_cache/kaart_z3(_klein).png`; multiprocessing (spawn).
- What is in it:
  - the world tree (stam with ribs, resin, rot; the Stamspiraal with fences/lanterns, 3 gates in at ring floors 176/128 and the
    floor 75; knot holes), the crown (golden/pink leaf shell with cushions, honey ponds, the old nest, blossom waterfall), the
    4 giant branches with riding paths, lianas, plumes, the fallen branch bridge, 8 main roots + forks + side roots, the
    Wortelduister caves with pink veins, sap lakes (shallow), the Hartslag-zaal (now with a real floor), the Grote Wortelgang;
  - Honingtak (kit): deck + Bloesemplein + market + blossom tree + mast + moored airship, 16 korfhuisjes with stairs and rope
    bridges, 3 raattrossen (cafe interior from the kit), Nestkapel cell, Pluimstal with 8 boxes, oefenbaan, zweefplatform;
    **bouw3 adds**: interiors in the korven by function (smid stone floor + anvil/blast furnace/magma "honingvuur", juwelier
    vitrines, 3 shops with counter + barrels + jars, bank with gold counter + chests, jagershut with premiebord (lectern) and
    jachtbord (chiseled bookshelf), burgemeester + 8 woningen with bed/smoker/table), doors kept free + fence gaps where the
    stairs arrive; the Nestkapel interior (quartz/pink marble, aisle, egg pedestal, pews, Droomsteen, froglights); doors + bridges
    from the cafe / Nestkapel / Raattros bottom cells to their honey-lift landings and to the tros spiral stairs; the Mika-imker
    secret raatzaal under the Raattros (hatch + ladder);
  - Hangnest Dauwdrup, Mosvoet (bouw3: flat plank floor in the root knot, flush thresholds, tables), Schorsstad (rings, wall
    houses, Lantaarntoren, lifts), valleys (Lichtkeverbos, schimmelboerderij, Paddenbloesemdal, Honingmoeras), Wortelgewelf,
    Grote Wortelpoort, 4 Pluimposten, Kruinring, Mika-steiger (all kit);
  - bouw3: cloud sea (3-deep water on sand at y 58-62 round the island, white puffs, barrier at the grid edge), 6 wind columns
    (petal ring at the foot, free shaft, blossom rings on the way up, landing deck at the top), 3 honey lifts (chains, honeycomb
    landings, bridges), the big Honinglift (top station balcony, double chain cable with 3 buckets, Mosvoet station),
    Kale Kruin (raised dead mesa r 24 with a stepped skirt, dead branches, stick ring) + 2 perches, 3 grey spray patches,
    2 glide platforms, viewpoint tower De Kroon, the Pelszaal (round hall r 16 on top of the tower: cheese-rind throne, big cage,
    4 hanging cages, 6 blown-out lantern posts, stair from ring 7, root chains), the finger print on the trunk wall, 3 spiral
    balconies with glide-jump planks, the Wortelweg (ring road r ~470 on the ground, tunnels cut through the roots, lanterns),
    dungeon shells (see below), secrets (stand spots + relief panels for the Muurverhaal), campfires (unlit) at every
    `kampvuur_*` POI with a plank pad and log benches. All the kit's decorative campfires are dropped (any campfire is a kern
    checkpoint).
- Dungeons (shells; rooms are EMPTY for the bazen slice):
  - Kaaszeppelinwerf (rel WERF, world ~2899,-570): scaffold posts, level C y 110 = the flag zeppelin hull with a flat deck
    (Roquefort's arena) + the captain's cabin with a chest; level B y 125 = deck ring with the gieterij hall (magma "fondue"
    strips), the bewapeningsloods (4 levers), Brokkel's open deck with 2 cannon spots (dispensers on posts); level A y 140 = the
    rope-work deck: 5 plank islands joined by slab bridges, Taleggio's open rigging (chains); cranes; staircases branch -> A -> B -> C.
  - Hart van de Boom: a stair tunnel from the Hartslag-zaal north wall (`hart_poort`) up to a ring corridor (r 95, floor y 50)
    under the trunk, one way round: aderloop -> schimmelkwekerij (3 ketels) -> ringenzaal -> Wortelwurger -> Nestwachter
    (jaarring floor) -> Baron Bleu -> a walkway in to the Nestkamer (the egg from the kit, a floor ring round it).

## Code (`feature/vadsrijk/zone3/wereld`)
- `Z3WereldPlekken` (GENERATED): `ALLE`/`plek(id)`, `GEBIEDEN` (Gebied3 with y-range + box/cylinder) + `gebied(x, y, z)`,
  `MOBZONES` + `mobzone(x, y, z)`, `PADEN`, `WINDKOLOMMEN`, `LIFTEN`, `KAMPVUREN`, `UITKIJKEN`, `DOK_LANDING`, `DOK_BEL`, `RESPAWN`,
  `inZone3(x, z)`.
- `Z3WereldRegistry` (one marked line in `wereld.WereldRegistry`): flat kern areas for the 2D callers (`z3_honingtak` town box,
  `z3_zeppelinwerf`, `z3_ring` = the whole zone, lv 30-50) via `Gebieden.voegVooraan`; quest action `z3_vlieg`; server tick
  (wind columns, viewpoints, column particles); right-click (dock bell, lifts); dev commands
  `/guhs vadsrijk z3wereld plek <id> | gebied | vlieg honingtak|opnieuw | lift <id> op|neer | uitkijk <id>`.
- `Z3Reizen`: airship line to the Honingtak dock. Gate `MAG_NAAR_SCHEMERWORTEL` = `zone2_klaar` + lv 30 (creative bypass;
  once reached always open), `WEIGER`, `EERSTE_AANKOMST` (npc may replace the opening), `slot(p)` (lang key or null),
  `bereikt(p)`. Opening film (~26 s): bark wall, through the clouds to the golden crown, down the trunk into the roots, two
  grey-blue eyes + the voice "Hij leeft nog... maar niet lang meer.", then the dock. Later flights ~8 s. Dock bell = travel map.
- `Z3Liften`: right-click a block within ~3 of a stop -> ride scene along the shaft/cable, then you stand at the other stop.
  Honinglift gate `MAG_HONINGLIFT` (default: Questlog flag `z3_honinglift`; creative always).
- `Z3Wind`: inside a column: levitation 17 (short, refreshed) on the player and on his mount; above the landing y: pushed
  sideways onto the landing with slow falling. Petals/glow particles from the server.
- Client (`client/`, one marked line in `wereld.client.WereldClient`): `Z3Lucht` (honey band, Hoogvads floating city east with
  pink night windows, the volcano SE, the Grauwe Einder grey streak), fog colour per LAYER (`Z3WereldClient.laagKleur`),
  `Z3Sfeer` (bees/leaves/petals in the crown, wood creak on branches, drips + hoot in the trunk, fireflies on the ground at night,
  a heartbeat in the roots that grows towards the heart, campfire crackle).
- Edits outside my package (all marked `[zone 3]` / `[z3-wereld]`):
  - `wereld/WereldRegistry` + `wereld/client/WereldClient`: 1 line each;
  - `wereld/client/WereldLucht`: skip its far landmarks in zone 3; the giant-tree landmark moved to azimuth 91 (east) and the
    floating castle (Hoogvads) to 116 behind it (was 255 / 75);
  - `zone2/wereld/client/Z2Lucht`: the tree drawn at azimuth 136 (south-east of zone 2) instead of 326;
  - `npc/Reiskaart`: `hier` = "schemerwortel" in zone 3; "schemerwortel" is a teaser until zone2_klaar (or reached), then a real
    place (open / locked with the level reason);
  - `npc/QuestMotor`: REIZEN `kapitein` + `z2_kapitein` get "schemerwortel", new `z3_kapitein` (zwamzoet, kandijkom, knuffelkamp);
    `reisDoelen` shows schemerwortel only after zone2_klaar; `reisSlot("schemerwortel")` = `Z3Reizen.slot`; `vliegNaar` ->
    action `z3_vlieg`.
- Resources: `tools/features/vadsrijk_z3_wereld.py` (in FEATURES, one line): textures `z3_hoogvads(_nacht).png`, `z3_einder.png`;
  texts (area names, airship, opening, lifts, viewpoints, travel map/captain lines for schemerwortel).

## Kern areas with y-ranges (open, for z3-systemen / integration)
- kern `Gebieden.Gebied` is still flat. I did NOT edit kern. The real zone-3 areas are `Z3WereldPlekken.GEBIEDEN` (Gebied3:
  y-range + box/cylinder, first match wins) with `Z3WereldPlekken.gebied(x, y, z)`. Kern gets only 3 flat boxes (see above),
  so 2D callers see "Honingtak" in the town and "De Schemerwortel" (z3_ring, lv 30-50) elsewhere. When z3-systemen lands a
  y-aware kern API, register `GEBIEDEN` there (ids and order are ready) and drop the flat z3_ring.

## For the other slices
- All coordinates: `POI_Z3.md` / `Z3WereldPlekken.plek(id)`. Stand spots unless noted.
- npc: captains call `QuestMotor.vliegNaar(p, npc, "schemerwortel")` (or `Z3Reizen.naarHoningtak`); set `Z3Reizen.WEIGER`,
  `Z3Reizen.EERSTE_AANKOMST` (the hq01 film) as wanted; **set the Questlog flag `z3_honinglift` at z3_hq09** (or replace
  `Z3Liften.MAG_HONINGLIFT`); NPC id `z3_kapitein` is a travel NPC at the dock (`dok_kapitein`).
- bazen: arenas `arena_*`, dungeon rooms `werf_*`, `hart_kamer_*`, doors are open (no moving doors in zone 3); the Pelszaal
  props (throne, cages, lantern posts) are blocks at the `piepvel_*` spots; cave lanterns at `hart_grotlantaarn_*` are soul
  lanterns (switch to lanterns when lit).
- wezens: `MOBZONES` (with y-ranges) + `PADEN` (no spawns within ~5).

## Tests
- (see the end of this file, filled in after the checks)

## Resume point
- Regenerate with `python tools/vadsrijk/maak_kaart.py --zone 3` after any generator change; `python tools/vadsrijk/zone3/poi3.py`
  alone rewrites POI_Z3.md + Z3WereldPlekken.java.
