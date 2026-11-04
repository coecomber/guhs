# NOTITIES_G_Z2 - fix round 2: zone 2 "De Splinterpieken" rebuilt small, compact and more epic

Branch `110-g-z2` (worktree `guhs_vadsrijk/g-z2`, from fba766b79). Brief: `../G_BRIEF.md` + the coordinator's g-z2 task (user's
playtest: Kandijkom pretty but flat/fake, the zone too big and stretched, too white). The level/quest part (lv 20-30, 5 side
quests) is in `NOTITIES_G_Z2_LEVELS.md`.

## Layout (tools/vadsrijk/zone2/plan.py)
- Zone box **x -250..250, z -2775..-2200** (~500 x 575; was 1200 x 1200). The tile grid is unchanged (tx -11..10, tz -48..-28):
  everything outside the island is a shallow cloud sea (1-10 water layers, cloud banks round the coast), so no stale tiles stay
  behind and the airship arrival from the south is the same. Tiles: 29.5 MB (was 25 MB).
- **Kandijkom** stays at (0, -2400) (floor 90): geode inner face r 66 (was 124), 4 gates; the dock (0, -2236) is unchanged.
- **Galmhellingen** E (x 85..250), **Knuffelmeer** W (lake (-172, -2378)), **Splinterkloof** N (ravine z ~-2545, floor 64,
  rims 122/132, x -236..236), **Spiegelburcht** NE on its peak (centre (150, -2693)), dock S.
- Areas (`GEBIEDEN`, levels from the coordinator's lv 20-30): galm 20-23, meer 23-26, kloof 26-29, burcht 28-30, town/ring 20-30.

## What is new
- **Kandijkom** (bouw2.stad):
  - tiers against the geode wall as terrain: tier A (r 34-50, floor 95), tier B (r 50-66, floor 100); stair lanes beside every
    gate street (spruce stairs, 1 block per step), railings on the tier edges, plank bridges over the N/E/W streets at tier B;
  - a 34-high **crystal clock tower** in the middle of the plaza (walk-through arches, 4 guhklok faces, a lampion balcony, a pink
    crystal spire), **garlands of lampions** (feestslingers + elftocht lampions) from the tower to 8 plaza posts;
  - **Christmas market**: up to 8 striped stalls round the plaza, a hot-chocolate stand by the pond (`chocomelkkraam`), fire
    baskets (elftocht_vuurkorf), the big **decorated tree** with ornaments, a star and presents, the skating pond with benches;
  - 19 buildings (ground: juwelier, smid, cafe, bank; tier A: 2 shops + huis_1..6; tier B: kristalkapel, huis_7..12) with
    pastel walls, coloured roofs, icicles + lampions under the eaves and **real interiors**: a fireplace in the back wall with a
    chimney, beds with blankets, bookcases, tables with chocomelk cups, kettles, rugs; floors per type (spruce = house, birch =
    shop, polished andesite = smithy, dark-oak/spruce checker = cafe, marble = kapel); the chocolate cellar under the cafe;
  - pink/purple/blue/magenta crystal shards with glowing cores in the geode wall; the stable moved outside the south gate.
- **Gates fixed**: every gate street runs flat at y 90 straight through the wall (`doorgang`), the paths start inside the town
  so their slope-limited profile ramps up outside (the old 7-block wall at the east side came from the town floor being reset
  after the path profile). Checked: east gate -> (96, 100, -2399) -> campfire galm_1 (Berggids Klimpoot) is walkable.
- **Drama**: the Galmspleet (a gorge under the gondola, floor rising south = an exit), brug_5 over it, 4 hanging bridges over the
  ravine (x 150 = the wide one to the Spiegelburcht), ledge ramps (2 south, 1 north, the Galmspleet), 4 frozen waterfalls down the
  ravine walls, the **frozen avalanche wave** curling over the Galmtop path (`lawine_golf`), the gondola over the gorge.
- **Colour + life**: 6 crystal forests (crystal trees with glowing hearts), crystal needles in pink/purple/blue/magenta, geode
  grottos (galmgrot = amethyst/pink, ijsgrot = blue ice), crystal-tinted patches in the snow, pink petals on the low grass,
  fewer spruces; 4 guh hamlets/camps (`gehucht_*`: 2-3 huts with interiors, a stall, fire basket, garlands), lampion posts along
  every path, a hot spring (`warmwaterbron`).
- **Town life (npc)**: 12 new ambient NPCs (`z2_schaatser_1/2`, `z2_sneeuwbal_1/2`, `z2_pinguguh_1..3` = GuhVariant.PINGUH,
  `z2_slee_1/2`, `z2_marktguh_1..3`) through the existing NPC keeper (Gedrag SLENTER/STAAN), names/lines/where in
  vadsrijk_z2_npc.py. Minimal footprint: Z2NpcPlekken (spots) + Z2Npcs (looks) + the generator; no new entity types. Ambient
  critters (snow hares, ice birds, moths) were already in wezens `Z2Critters`.

## POI ids (all v1 ids kept; new positions)
- Moved as one piece: the **Spiegelburcht** = v1 castle + (BURCHT_DX 150, 0, BURCHT_DZ 222) (`Z2WereldPlekken.BURCHT_DX/DZ`).
  bazen: `Z2BazenPlekken.BURCHT_X/DZ`, `k(x, z)` = x relative to the castle centre, z world; puzzles, gauntlet, gates,
  `inBurcht`, `Z2Bouw.burcht`, the puzzle gametest use the offset. Room geometry, beam rows, hall mirrors are unchanged relative.
- Galmwolf arena (182, -2318) r 32 (pillar offsets unchanged), Boormeester cave (-30, -2614) r 20 (entrance from the ravine floor).
- Campfires: same ids, same counts (1 + 4 + 4 + 5 + 1). brug_1..5 (brug_5 = the Galmspleet bridge, `_zuid` = west end).
  kloof_trap_1..4 (3 = the Galmspleet foot). Needles fields, avalanches, knuffel_ijs_1..10, secrets, viewpoints: same ids.
- New ids: `opening_naald` (the needle the Galmwolf howls on, SE geode rim), `klokkentoren(_top)`, `chocomelkkraam`,
  `markt_kramen.N`, `gebouw.huis_9..12`, `lawine_golf`, `warmwaterbron`, `meer_scheur`, `ijsval_N`, `kristalbos_N`, `gehucht_*`,
  `kloof_bodem_N` (ravine-floor spawn spots), `schat_onder_de_brug` (a ladder + ledge + chest under brug_2's south head),
  `npcplek.<npc>` / `poiplek.<poi>` / `aanwijzing.<id>` / `schat.<id>` (every literal of Z2NpcPlekken now comes from the map:
  `Z2NpcPlekken.volgKaart`), `PADEN`, `ZONE_*`.

## Code touched
- zone2/wereld: `Z2Reizen` (opening needle from `opening_naald`); `Z2WereldPlekken` generated (+ ZONE_*, BURCHT_DX/DZ, PADEN).
- zone2/bazen: `Z2BazenPlekken`, `Z2Bouw`, `spiegel/Spiegelburcht`, `VadsrijkZ2BazenGameTests` (offset only).
- zone2/wezens: `Z2WezensPlekken` (boxes from GEBIEDEN, `padAfstand`, Kandijkom/dock safe radii 92/48), `Z2WezensSpawners`
  (spacing per area so each holds <= 100 alive (MMO-dense), radius shrunk/dropped so nothing spawns within 5 of a path; ravine
  floor spawners from `kloof_bodem_N`).
- zone2/npc: `Z2NpcPlekken` (volgKaart overrides, scenes relative to their POIs: dragon, flashback (+ castle offset),
  cliffhanger (altar), gondola fallback), `Z2Npcs` (12 town-life NPCs). Where-texts updated for the new layout.
- zone2/systemen: `Z2SystemenPlekken` box from ZONE_*.
- tools/vadsrijk/zone2: plan, kaart2 (new terrain), tegels2 (stad material, colour patches, **stamp carves lower the terrain
  shell** so a carve never opens a hole into the void), bouw2 (rewrite of the town + new set pieces), maak2 (Java extras).

## Checks
- `python tools/vadsrijk/maak_kaart.py --zone 2 --geen-cache`: OK, no unknown blocks.
- loopcheck `--box=-300,-2820,300,-2150`: **LEEG 0, PAD 0**, TRAP 44 (all chairs/benches = stairs used as furniture), VAL 26:
  14 enclosed attics/culled hollows under the tier houses (no opening, unreachable), the rest single cells (1.5-2 up to get out).
- Every road walked block by block on the tiles (scratch `padloop.py`): no step > 1 and no blocker on any path (fixed after
  g-ux's note: roads now ramp out from fixed town-floor heights through the gates; the east/west/north exits were the bug).
- POI stand-spot check (feet + head free, a floor under it) over all 385 generated spots: OK (only markers left: the camps'
  fire baskets, ice-fall/forest markers). All NPC spots (incl. g-ux's 5 corrected ones: bakker, beeldhouwer, touwtje,
  wacht_burcht, versteend_pluk) now come from `npcplek.*` on the new tiles (volgKaart overrides the literals).
- Gametests `-Pgt=Vadsrijk,Z2Npc`: 67/69 on the first run (spawnersDichtheid: galm max 41 -> spacing from 18, min radius 6;
  grotSpawnerVindtDeVloer = zone-1 flake); rerun `-Pgt=VadsrijkWezens,VadsrijkZ2Wezens` 11/11.
- AutoCheck `tools/autocheck/vadsrijk_g_z2.txt` (27 shots, run/screenshots/autocheck/gz2_*.png, on the tiles before the road
  fix): opening (storm, needle), dock, plaza + clock-tower street with the tier stairs and the bridge, market stall, tree, cafe +
  house + kapel interiors, east gate out, Galm campfire, avalanche wave, Galmwolf arena, lake, dragon saddle, ravine rim/floor,
  Galmspleet bridge, crystal forest, main bridge with the Spiegelburcht on its peak, burcht door + hall, gondola ride. (The
  skyline shot fell to the ground: creative /tp without flying.) run/saves still holds the AutoCheck world (run/ is ignored).

## Open / for the user
- Looks are judged by the user in the dev client (goed is goed): roof colours, amount of snow vs crystal, density of the market.
- The where-texts of the 18 removed side-quest givers are kept (they are citizens now).

## Merge notes
- g-ux also edited Z2NpcPlekken (5 spots): keep my `volgKaart` lines; their numbers are only fallbacks now.
- Integration: regenerate zone 2 (`maak_kaart.py --zone 2 --geen-cache`); the tile grid is unchanged, so no tiles to delete.
- Z2WereldPlekken.java + POI_Z2.md are committed (the code needs ZONE_*, BURCHT_DX/DZ, PADEN to compile); tiles are not.

## Resume point
Done: map rebuilt, code aligned, checks + gametests + one AutoCheck walk. Open: the user's look judgement in the dev client.
