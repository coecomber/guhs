# NOTITIES_K_INT: Vadsrijk zone 3 "De Schemerwortel", integration (branch 110-k-int, worktree guhs_vadsrijk/k-int)

Base: 05196f3cc (110-j-int, the round-3 integration = the user's test client). One merge commit per branch. Never gradle in
j-int / h-melee / g-int / f-int / w-poc. build/moddev copied from j-int (artifacts only). Scratch: scratchpad/kint (logs,
lang3.py = 3-way lang merge, lang3m.py = the same with a synthetic base for criss-cross merges).

## Merged
| branch | head | notes |
|---|---|---|
| 110-z3-systemen | ca18781a8 | VadsrijkFeature payload lines: both; lang 3-way |
| 110-z3-wereld | d06c93935 | WereldLucht landmarks: z3-wereld's (tree 91, Hoogvads 116 behind it, volcano 148) instead of j-int's 75/105/140; i-lore's moon/finger constants kept |
| 110-z3-wezens | c9dffda70 | FEATURES: both; lang 3-way |
| 110-z3-npc | 53ca0312a | criss-cross (bases ca18781a8 + 5bbc7af29): lang via lang3m.py (base = ab6d952ac + systemen + voltooiing). Zone-2 cliffhanger: i-lore's version kept (Z2NpcPlekken comment, lang `z2_hq12_hartsplinter.klaar.*`), npc's generator change to vadsrijk_z2_npc.py was identical/absent |
| 110-l-fix | 569530eef | clean (generators + town beds + volumes) |
| 110-z3-bazen | 0d1cbd618 | Vahoegje animation add/add: bazen's (theirs). One pet registration (bazen's `PET_VAHOEGJE`; npc's `VAHOEGJE_PET` points at it, npc's texture `textures/vadsrijk/zone3/vahoegje.png` + its generator writes removed). Pet texts = bazen's. Rijdieren.RIJDEN_IN_ARENA merged cleanly with i-mounts |

## Cross-cutting decisions (commit 166214cb8, class `zone3/npc/Z3Koppelingen`)
- **Opening:** z3-wereld's film (`Z3Reizen.opening`) is THE opening (`Z3Reizen.EERSTE_AANKOMST = null`, set explicitly at
  server start). hq01's "aangekomen" comes from standing in the Honingtak area, so it works with the wereld film. npc's
  `Z3Verhaal.opening` only runs if Z3Reizen were absent (never now).
- **Zone-2 cliffhanger east:** i-lore's (in j-int) kept; z3-npc's duplicate dropped in the merge.
- **Travel:** `luchtschipper` (De Haven) already offers `schemerwortel` (z3-wereld did it). hq01 starts at Kapitein Poolster
  (`z2_kapitein`, Kandijkom).
- **Bazen <-> npc:** `Z3BazenApi.MOMENT` -> `NpcApi.meld`: `baas:<id>` -> `z3_<id>:verslagen` (nestwachter -> opgevrolijkt,
  vahoegnir via its own moment), `piepvel_weg` -> `z3:flash_f1`, `nestwachter_gered`, `vahoegnir_opgevrolijkt` ->
  `z3_vahoegnir:opgevrolijkt`, `troonzaal_klaar` -> the bloom (`zone3_klaar`, `z3:finale`). Binding `z3_vahoegnir` ->
  `Z3BazenApi.startVahoegnir(p, false)`. `HEEFT_SPLINTERS` = hq14 active/done (or creative).
- **Flashes / finale:** bazen's scenes are the real ones (F1 arm, Vahoegnir ending + hatching + beam, F2 throne room). npc
  waits for them: F1 is skipped when the player saw `z3:f1_arm`; the npc finale is marked seen on `vahoegnir_opgevrolijkt` and
  the bloom waits for `troonzaal_klaar` (max 90 s); hq15's cliffhanger skips its own F2 when `z3:f2_troonzaal` was seen. The
  npc versions remain as a fallback for a player who was not in the room (bazen shows its scenes to players within 40-45).
- **Bleu's egg:** a decoy (prop in his room); the npc egg scene now starts with "Het ei bij Baron Bleu? Nep! Een kaaswiel met
  roze verf..." (`vadsrijk.z3npc.scene.ei.nep`).
- **Mika-imker:** NOT built as a boss (time). The secret quest ends in a lore scene: all 5 clues -> binding `z3_imker` (flag
  `z3_imker_open`) -> walk into `mika_imker_raatzaal` -> 6-line Praat scene (a Mika in a too-big beekeeper suit "tasting
  honey for the report", the smoke pot dies, he flees) -> `z3_imker:verslagen`. Book: imker counts by the quest.
- **Small bindings:** `z3_ring_aan` (a ring of flames round the trunk at the ring's height, per player), `z3_ketel_uit`
  (blossom puff), `z3_terugkleur` (a pink wave from the nest down the tree, ~6 s, per player).
- **Coordinates:** Z3WezensPlekken / Z3NpcPlekken / Z3BazenPlekken read `Z3WereldPlekken` directly (no reflection; the old
  numbers are only used for ids wereld lacks). All 43 bazen ids exist in wereld. `Z3Lagen.GEBIEDEN` is built from
  `Z3WereldPlekken.GEBIEDEN` (+ `z3_ring`), so areas follow the map. `Z3Wind.KOLOMMEN` was already pointed there by wereld.
- **Layered paths:** NOT registered in kern's 2D `Spawners.registreerPad` (wezens checks per height).
- **Book (zone-3 page):** lookouts (`Z3WereldPlekken.UITKIJKEN`) + campfires (`KAMPVUREN`) added; boss ids checked against
  bazen's registry (`vr_z3_baas_roquefort|kruinroofvogel|piepvel|bleu|vahoegnir` all exist; schimmelkolos dropped (a prop
  entity), imker = quest only). `ZoneBoeken.gebiedId` is 3D now (layered zone-3 areas), catch-all name `z3_ring` for zone 3.
- **Altars:** `bouw3` places `guhs:vr_z3_baas_altaar` at `werf_vahoeg_altaar` (on a 3x3 plank pad beside the werf entrance)
  and `hart_vahoeg_altaar` (was a chiseled stone placeholder).
- Generator fix: `vadsrijk_z3_npc.py` `_mijn_sleutel` dropped z3-wereld's `vadsrijk.npc.reis.kies.z3_kapitein` on a standalone
  run -> `vadsrijk.npc.reis.*` excluded.

- **Honingtak town music:** `StadsMuziek.STEDEN` + "honingtak" (`stadVan("z3_honingtak")`; a layer, so only the 3D lookup finds
  it), two honey waltzes `honingtak_1|2` (music box + pluck, pizzicato, honey-drip glockenspiel, a little bee hum; synth script
  `vadsrijk_stadsmuziek_geluid.py honingtak_1 honingtak_2` only makes those), bed = briesje, day sounds beehive/bee/birds/giggles.
  Gametest asserts the dock is Honingtak and the ground below is not. Not listened to.
- **Tile size:** the shared writer `nbtsnel.schrijf` now sorts the blocks y-major (y, z, x) and gzips at level 9: ~13 % smaller,
  the order is free (vanilla StructureTemplate sorts on load). Zone 3 71.8 -> **62.5 MB**, zone 1 35.9, zone 2 26.8, total
  **125.3 MB** (was ~140). What is left in zone 3: 20.5 M blocks (stone/tuff/andesite 29 %, water 11 % = the 3-deep cloud sea,
  leaves ~20 %). Further cuts need design changes (a 2-deep sea ~-4 %, a 1-thick leaf skin ~-10 % but see-through).
- **Gate 75 (walkability fix):** at y 75 the trunk foot is ~20 blocks wider (`boom.r_buiten` foot term), so the Stamspiraal road
  runs at r ~113-120 while the kit's gate tunnel ends at r 98: the Schorsstad floor (14,820 stand cells) + the lower spiral were
  a real trap. `bouw3.poort75_gang`: a 5-wide passage from the gate out to the road (floor y 74, a slab step, deck height 75),
  2 lanterns. Proven with the loopcheck (below).
- **Dev:** `/guhs vadsrijk z3npc klaarzetten` (zone-2 main quests done + `zone2_klaar` + level >= 30); `npc tp <id>` knows the
  zone-2/3 NPCs too.
- **Book page order:** `Voltooiing.zones()` sorted by id (z3-npc registers before VoltooiingRegistry, zone 3 was page 0).
- **Docs step:** `tools/wiki_bbq2/__init__.py` group bbq2_vadsrijk = vadsrijk_npc + vadsrijk_z2_npc + vadsrijk_z3_npc (zone 2 was
  in "Nog meer nieuws"). No English for the z2/z3 wiki pages yet (shown in Dutch on the English side, noted by the build).
- **FTB:** CHAPTER_VERSION **62**; make_ftbquests needs the vanilla resources jar at the old name
  `build/moddev/artifacts/neoforge-21.1.251-client-extra-aka-minecraft-resources.jar`: a copy of
  `minecraft-patched-26.1.2.112-merged.jar` (build/, not committed) for the new zone-3 header images.

## Log
- Compile after the 4 first merges: OK. After bazen + glue: OK.

- Final regen `maak_kaart.py --zone alle --geen-cache --audit` (after l-fix + glue): audit kuil 6 / poi 19 / trap 14 / boom 8 /
  gras 66 / stap 5 / zee 1 / water 5 / **deur 0** (l-fix). Then 2 tiles again for gate 75 (`--zone 3 --alleen "34,1;35,1"`).
- poicheck3: 389/390 (`lianenbrug`: a planned spot, no bridge; known). controleer_verticaal: Druipschacht 0, Hoedenpaden 6671
  (known sea-edge artefact). loopcheck zone 2 box -300,-2820,300,-2150: TRAP 46, VAL 28 = j-int's numbers.
- **Zone-3 loopcheck** (scratch copy `loopcheck3.py` with Y 2..318 + a `PEIL` probe: per point "reachable from the window exits"
  and "can get back"):
  - whole zone, venster 11 (4 windows): VAL 20; the only big ones (spiral 2937 + ring 2572 cells) sit on the window seam at
    x 2304 = through the trunk. Trunk-centred venster 11 (box 1984,-320,2687,383): **no big trap left**: VAL 16 = one
    Paddenbloesemdal pond pit (2608,67,367; 21 cells, 3 up) + 15 pockets of 1-3 cells (2-4 up) at root forks / knot holes.
    TRAP 6 = 2 lone stair blocks on the flat spiral-foot floor (2297-2299, 71-72, 108/113; you walk round them). PAD 1 (2440,264,81).
  - **Exit 1 proven:** Hartslag-zaal (2299,13,30) and the Wortelpoort (2299,62,283) both "can get back" (window 7, box
    2176,-64,2431,383). The smooth-walk query says NO: the route needs ordinary 1-block jumps.
  - **Exit 2 fixed + proven:** before: floor/gate/passage `kan_weg=False`; after poort75_gang: Schorsstad floor (2279,76,30),
    gate (2265,75,49), passage, spiral (2278,95,57), spiral foot (2299,71,112) and the ramp (z 125..160) all `kan_weg=True`.
- Gametests `-Pgt=Vadsrijk,Z2Npc,Z3Npc,StadsMuziek` (116 tests, one run): **113/116**. Failed (integration effects, fixed):
  `hoofdlijnHeeftAltijdWieEnWaar` (counted the z3 main quests as zone 1), `dpsTabelKlopt` (bazen's named z3baas_ weapons at lv 1),
  `telling` (zone 3 was book page 0). Rerun `-Pgt=VadsrijkNpc,VadsrijkKlassen,VadsrijkVoltooiing`: **19/19**. No flakes this run
  (boss timing and grotSpawner passed).

- `-Pgt=VadsrijkZ3Bazen` after the empty-arena fix: 6/6.

## AutoCheck (one walk, ~20 GB free)
- Script `tools/autocheck/vadsrijk_k_int.txt` (z3-wereld's walk + gate 75, spiral foot, both altars, Piepvel arena, Vahoegje,
  the book). **Run 1 crashed the server** at the Kale Kruin: `BaronBleu.vecht` -> `l.get(nextInt(0))` (his arena ticked in
  loaded chunks with nobody in it; every zone-3 boss's vecht picks random fighters). Fixed in `Z3Baas.actieTick` (no vecht
  without fighters; `vechtZonderStrijders()` = true only for the Kruinroofvogel's chase flight). **Run 2: 32/32 shots, no
  crash.** Shots in `run/screenshots/autocheck/` (kint_*, z3_*). Log findings = pre-existing non-Vadsrijk model warnings
  (droombed, bliksemsmidse) + EntityIconLoader noise. The werf altar stands on its pad; the book shot missed the screen.
- Saw: at the gate-75 spot (feet y 74) the area/biome is "Het Hart van de Boom" (z3_hart y 40..74 under the Schorsstad floor y 75).
  Cosmetic (label/ambience for the gate step), not changed.

## Run dir
- run/ copied from j-int (config, defaultconfigs, mods, resourcepacks, options.txt, usernamecache), **saves EMPTY** (the AutoCheck
  world was removed). The user starts `k-int/run-client.bat` and makes a NEW world.

## Open / for the user
- Not seen by a person: all of zone 3 (map, models, scenes, screens, wind, glide, lifts, music). SPIEKBRIEF Z1-Z7 + TESTEN zone-3
  section say what to look at.
- Mika-imker is a lore scene, not a boss (ZONE3_PLAN §5 boss still to build if wanted).
- Small walkability leftovers in zone 3: the Paddenbloesemdal pond pit (2608,67,367) and ~15 single pockets (1-3 cells, 2-4 up),
  two lone stair blocks at the spiral foot; `lianenbrug` POI without a bridge.
- No English for the zone-2/3 wiki pages; the zone-3 FTB texts are Dutch in en_us too (as before).
- make_ftbquests needs the copied vanilla jar name (see above) whenever new header art is drawn.

## Resume point
Integration DONE (head: see `git log -1` on 110-k-int). All 5 zone-3 branches + 110-l-fix merged; glue in Z3Koppelingen; final regen
committed (tiles incl. zone 3); checkers + gametests + one AutoCheck as above; TESTEN_VADSRIJK.md + SPIEKBRIEF.md have zone 3; run/
ready with empty saves.
