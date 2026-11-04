# NOTITIES_Z2_NPC — Vadsrijk zone 2 "De Splinterpieken", slice npc

Branch `110-z2-npc`, worktree `guhs_vadsrijk/z2-npc`, package `feature/vadsrijk/zone2/npc`, generator
`tools/features/vadsrijk_z2_npc.py` (in FEATURES), wiki `tools/features/vadsrijk_z2_npc_wiki.py` (not a feature, like zone 1's).

## Approach: zone 1's quest SYSTEM, zone-2 DATA
No parallel system. Zone 2 adds data and NPCs to `feature/vadsrijk/npc`:
- quests: `data/guhs/vadsrijk/npc/quests_z2.json` (read by `Quests` after `quests.json`; ids `z2_*`), texts under the shared keys
  `vadsrijk.npc.q.z2_*`, `vadsrijk.npc.naam.z2_*`, `vadsrijk.npc.praat.z2_*`, `vadsrijk.npc.plek.*`, `vadsrijk.npc.gids.z2_*`;
  zone-2-own texts `vadsrijk.z2npc.*`.
- NPCs: `Npcs.voegToe(NpcDef)` (same keeper/spawning/markers/questlog/dialogue). 50 NPCs (see below).
- Every objective has an explicit target `plek` ("npc:x" / "poi:x" / "gebied:x" / "baas:x") + a "where" text, and every objective text
  says WHO + WHERE + WHAT. The generator asserts it; the gametest checks it.

### Small, additive changes in zone 1's npc package (integration: check against f-quests)
| file | change |
|---|---|
| `Quest.Doel` | 6th field `plek` (+ the old 5-arg constructor) |
| `Quests` | reads `quests.json` AND `quests_z2.json`; parses `plek` |
| `QuestPlek` (new) | resolves a `plek` to a BlockPos + its text key; maps POI/GEBIED/BAAS other zones fill |
| `Questlog` | GEHEIM "teller:<map>" counts that secrets map (zone 2: z2_knuffels, z2_aanwijzingen); `map`, `kijkLive` public |
| `QuestMotor` | hooks: `ACTIES` (unknown quest actions), `KLIK_VOOR` (asked first on a click), `EXTRA` (extra choices in a role dialogue; option ids 70+), `metExtra` on the `luchtschip_kamp` + default role cases; a few methods public |
| `Npcs` | `voegToe`, `vloer` public |
| `RijkGuh` | `stelIn` public |
| `NpcHaken` | `pastMob`, `bij` public |
| `client/QuestlogScherm` | 6 Gids ids `z2_*` appended to GIDS |
| `tools/features/vadsrijk_npc.py` | `_direct` no longer deletes zone-2 keys (".z2_"/":z2_") or `vr_z2_*` advancements |
| `VadsrijkFeature` | one line: `zone2.npc.Z2NpcRegistry.register` |

**f-quests (tracker + beam) merge:** `QuestStap.doel()` should prefer the explicit target when set:
```java
if (!d.plek().isEmpty()) { BlockPos b = QuestPlek.pos(sl, d.plek()); if (b != null) return new QuestStap(..., wat, Component.translatable(QuestPlek.tekstKey(d.plek())), b, extra); }
```
Also: `QuestStap.reis()` only knows the camp island <-> mainland hop; zone 2 needs "mainland <-> Splinterpieken via Stuurguh Zeppie /
Kapitein Poolster" (z < -1700 = zone 2; pilot ids `luchtschipper` / `z2_kapitein`; text key `vadsrijk.npc.plek.z2_luchtschip` exists).
`levelPlek()` is zone-1 only (lv > 20: use z2_galmhellingen / z2_knuffelmeer / z2_splinterkloof). `Z2Npcs.init` already fills
`NpcPlekken.GEBIED_DOEL/KAMPVUUR/BAAS` by reflection when those fields exist. MELD steps (dragon, edelsteen, boormeester) only point
somewhere through `plek`.

## Content
- **NPCs (50):** Kandijkom 25 (captain, cafe, mayor, Kapitein Kwarts of the Kristalwacht, Ordemeester Kristalijn in the Kristalkapel,
  juwelier, smid, wapenhandelaar, winkel, breister, veilingmeester, schaatsjuf, gondelier, 10 citizens), out in the zone 6 (gondola top,
  berggids, ijsvisser, ijsbeeldhouwer, kloofwachter, lichtwachter), Kristalwacht 17 (one per POI ridder_* campfire + 2 on the plaza),
  4 petrified guhs (KRISTALGUH variant, no AI). Clothes: existing jumpers/scarves/hats (STRIPED_SWEATER, CHRISTMAS_SWEATER, GOLF_TRUI,
  ELFTOCHT_TRUITJE, SAMEN_KNUFFELTRUITJE, WATERSTAM_PARKA, ...).
- **Main quest (12):** z2_hq01 (Haven pilot, after hq12_schaduw + lv 20: the flight = the opening) -> cafe -> mayor + Kwarts ->
  Galmhellingen (campfire, berggids, 12 kills) -> galmscherven + the burst needle -> the Galmwolf (BAAS z2_galmhellingen) -> free the
  4 petrified guhs (talk objectives with a crystal-shatter, never hurt) -> under the ice (kills, elite, crack, beeldhouwer) ->
  Splinterkloof (Touwtje, kills, boorbeitels, the east drilling tower) -> the dragon on the col (scene, click "pull the splinter",
  he looks at you, flies off) -> Spiegelburcht gate (Prisma) -> Vrouwe Spiegelzucht (BAAS z2_spiegelburcht) -> flashback (the pink
  dragon) -> the heart shard to Ordemeester Kristalijn -> cliffhanger (the giant tree in the far west, azimuth ~255) + `zone2_klaar`.
- **Side (23):** wanten, kandijkoekjes, 10 knuffels under the ice, schaatsles, echokoor (3 needles), post per slee, sneeuwpop, kas,
  winterslaap, poollicht, tandwieltjes, Kandijfeest, ijsbloemen, 3 Kristalwacht premies, woest model, lawinegevaar, ijsvis,
  wiebelbruggen, truien, je eerste edelsteen (MELD edelsteen:ingezet from z2-systemen), naar de top (gondola).
  + 3 class quests `z2_adv30_<klasse>` (lv 30, actie advancement:<k>:3) + secret `z2_geheim_boormeester`.
- **Secrets:** 10 knuffels (POI knuffel_ijs_1..10, map `z2_knuffels`), 5 boormeester clues (`z2_aanwijzingen`; the first starts
  the secret quest; all 5 call `NpcRollen.doe("z2_boormeester")` -> z2-bazen `Boormeester.ontgrendel(p)` by reflection), 5 treasures
  (`z2_schatten`). FTB advancements `vr_z2_knuffels_alle`, `vr_z2_aanwijzingen_alle`, `vr_z2_schat_<id>`.
- **Guides:** questlog Gids pages z2_splinterpieken/kristalwezens/bevroren/edelstenen/reizen/titel; FTB sections (chapter
  guhs_vadsrijk, nothing locked); wiki pages in vadsrijk_z2_npc_wiki.py (integration: add to the docs step next to zone 1's).

## Adapters to the other zone-2 slices (a binding always wins; reflection next; own fallback last)
| what | binding (NpcRollen) | reflection tried | fallback |
|---|---|---|---|
| juweliersguh socket screen | `juwelier` | `zone2.systemen.Juwelier#open`, `Sokjes#open`, `edelsteen.Juwelier#open` | "nog niet" line |
| airship Haven <-> Kandijkom | `z2_luchtlijn` (arg kandijkom/haven) | `zone2.wereld.Z2Reizen#vlieg`, `Luchtlijn#vlieg` | own scene + teleport (first flight = always our opening) |
| gondola | `z2_gondelbaan` (arg op/af) | `zone2.wereld.Z2Reizen#gondel`, `Gondel#rij` | own scene + teleport |
| boormeester unlock | `z2_boormeester` | `zone2.bazen.Boormeester#ontgrendel`, `boormeester.Boormeester`, `mika.Boormeester` | none (quest still says where) |
| Galmwolf in the opening | `z2_galmwolf_opening` (arg x,y,z) | — | grey dust silhouette + howl + sonic boom |
| resting dragon / flying off | `z2_draak_rust` (x,y,z,ticks), `z2_draak_weg` | — | wezens `DraakVlucht.vlieg` (a hover over ~0 blocks) |
| pink dragon (flashback) | `z2_draak_roze` (path|ticks) | — | the grey model wrapped in pink dust + hearts |
| 3rd job lv 30 | zone 1's `advancement` role (klassen `Advancement.doe`, extended by z2-systemen) | — | title card `vadsrijk.npc.adv.<k>.3` |
| quest events | `NpcApi.meld(p, "edelsteen", "ingezet")` (z2-systemen), `meld(p, "z2_boormeester", "verslagen")` (also seen via Haken.VERSLAGEN: entity id containing "boormeester"/"galmwolf"/"spiegelzucht") | | |
Shops use zone 1's roles: smid, winkel, wapenwinkel, kleermaker, markt, ordetempel (Kandijkom = same screens as Zwamzoet for now).
API for others: `Z2NpcApi.zone2Klaar/ZONE2_KLAAR`, `boormeesterMag(p)`, `nogVersteend(p, npc)`.

## Coordinates
`Z2NpcPlekken` = numbers copied from POI_Z2.md v1 (comment "POI:<id>" per line). Integration: replace by
`Z2WereldPlekken.plek(id)`. Requests (opening needle, hot spring, ice crack, treasure under the west bridge, npc_* ids) in
`../POI_VERZOEKEN_Z2.md`, section z2-npc. Area ids are z2-wereld's (`z2_kandijkom`, `z2_kandijkom_dok`, `z2_galmhellingen`,
`z2_knuffelmeer`, `z2_splinterkloof`, `z2_spiegelburcht`, `z2_ring`); we register them ourselves only when absent (same boxes).

## Decisions made alone (open for the user)
- All names/texts; the Kristalkapel is the order's branch; the dragon rests on z2-wereld's `draak_zadel` (north of the lake), not in a
  kloof ravine (the brief said ravine; the map has the col).
- The petrified guhs use the KRISTALGUH look for everyone; "freed" is per player (dialogue changes). A grey crystal look / per-player
  shell would be an art/wezens task (`Z2NpcApi.nogVersteend`).
- The opening teleports you to the dock at tick 0 (so the scene's chunks load); no ship entity: the camera is "on deck".
- Bestiary pages for guessed z2 mob ids (vr_z2_splinterling ...): align with z2-wezens/bazen ids at integration.
- `vadsrijk.npc.adv.<k>.3` titles: Splinterbreker / Poolsterschutter / Kristalziener (z2-systemen may own other names).

## Resume point (end of run, 2026-11-04)
- Compiles; gametests `-Pgt=Z2NpcGameTests` **3/3 passed** (elkeStapWieEnWaar: every z2 step has a target + where text, every NPC
  name/where/clothes; hoofdlijnViaApi: the whole main chain via the real hooks + NPC clicks up to zone2_klaar, plus the Haven gate;
  geheimenEnBeelden: knuffels under the ice count only for Vlokje, the 5 clues + boormeester, freeing a petrified guh).
- AutoCheck once (`tools/autocheck/vadsrijk_z2_npc.txt`, no zone-2 tiles in this tree: `/guhs vadsrijk z2npc proefvloer`): the
  opening plays (snowstorm particles, darkness, the grey dust wolf + howl, needle burst, pink geode, title "De Splinterpieken");
  all 50 NPCs render with names and jumpers (row + live on the plaza); the burgemeester's quest dialogue opens (1/4). Seen: the wolf
  silhouette is small from the camera (z2-bazen's `z2_galmwolf_opening` binding with a real Galmwolf would sell it better); the sky
  in the storm is still zone 1's pink (z2-wereld's sky). The script's questlog section did not open (the dialogue screen was still
  open; script timing only).
- Not done: a run on the real zone-2 map (tiles live in z2-wereld); the f-quests tracker hookup (see above, 3 lines in QuestStap).
