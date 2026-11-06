# NOTITIES_T_Z4VERHAAL: the story of zone 4 "De Lampionkust" (lv 50-56), night round T

- **Branch:** `110-t-z4verhaal` (from dbf82899d).
- **Worktree:** `guhs_vadsrijk/t-z4verhaal`.
- **Package:** `feature/vadsrijk/zone4/verhaal`.
- **Generator:** `tools/features/vadsrijk_t_z4verhaal.py` (in FEATURES).
- **Cheat sheet:** `SPIEK_T_Z4VERHAAL.md` (Dutch, the full walkthrough + commands).
- **POI requests:** `../POI_VERZOEKEN_Z4.md`.

## What is built
- **Approach: the shared quest system, with zone-4 data** (as zones 2 and 3):
  - The quests live in `data/guhs/vadsrijk/npc/quests_z4.json` (`Quests.BESTAND_Z4`, read after zone 3; ids `z4_*`).
  - Texts sit under the shared keys `vadsrijk.npc.q|naam|praat|plek|gids|tip.z4_*`. Zone-4-only texts use `vadsrijk.z4verhaal.*`.
  - Every objective has an explicit `plek` plus a "where" text and says who, where and what. The generator asserts this and the gametest checks it.
- **Main line "De Grote Knuffeltij"** (7 quests, `z4_hq01..07`):
  1. Wasja (zone 3) sends you to Lampionhaven. You fly there and meet Havenmeester Ankertje.
  2. At Botenguh Zeiltje you get the sailboat. You sail to the Knuffeltij-strand, talk to Wasguh Sopje and cheer up 6 grey knuffels. You hand in at Opvangmoeder Zachtje.
  3. You find 4 big knuffels on 4 islands (Strandfeest, Bloemeneiland, Koraalbaai ring, the turtle). Each one is a **soft flash** of the grey east: the poster city with the sucking pipe, the grey road with the bowl castle, the Grauwe Vloot with its ring-toed captain, and the throne room with "Meer.". Then you go to Opa Lichtstraal in the lighthouse.
  4. The grey gate on the Mysterie-eiland plays the mist flash. You talk to Verdwaalde Nevel, a refugee who tells of the Grauwe Vloot and the Ringvinger. Back at Zachtje you hear that knuffels were stolen, and step 5 starts automatically.
  5. The Wrak van de Vliegervanger: Kootje (meld `z4_vliegervanger:verslagen`), then the 2 stolen knuffels (11 and 12).
  6. You fetch lanterns, kites and lemonade, then the party scene on the beach: Grijsje finds her Muis, and Zachtje says "Welkom thuis". Then the **Moeder Wolkwal** scene: she sinks low, sings, and shows a tired eye and a grey wisp.
  7. At the Pierkop you meet Pierwachter Wolkje and then Kapitein Hemelhoog. The **cliffhanger**: the first airship rises along the whale's eye up to Hoogvads. Back on the pier, a white-and-gold cape drops a grey fluff (the zone-5 spy). "Wordt vervolgd...".
  - The cliffhanger sets `zone4_klaar` + `z5_luchtschip_open` first and calls `Z4VerhaalApi.ZONE4_KLAAR`.
  - The Knuffeltij is a **fixed 12** (6 + 4 + 2) and only exists inside the quests (maps z4_tijknuffels/z4_grote/z4_wrakknuffels). There is no collection system; the FTB entry "vr_z4_knuffeltij_alle" is only a reward check.
- **16 side quests:**
  - kite building (s01) and the kite contest (s02: 30 s at the pole with the kite in your bag);
  - fishing (s03: NeoForge ItemFishedEvent in zone 4 + `meld z4_vis:gevangen` for t-ambacht);
  - pearls in the lagoon (s04) and the Grauwe Zeeslurf (s05);
  - beach volleyball (s06: a click game; the wiggling ear shows the side);
  - shell lemonade (s07);
  - the lighthouse lens + lore (s08);
  - bees (s09) and flower wreaths (s10);
  - seaweed for turtle Ouwe Dobber (s11);
  - Nevel's mist letters (s12: his flight diary);
  - soup for 4 refugees (s13);
  - lanterns on hooks (s14);
  - the campfire dance (s15);
  - Grijsje's bottle to grandma in the Kringstroom (s16).
- **NPCs (48), via `Npcs.voegToe`:**
  - **Lampionhaven (21):** havenmeester, boat dock (role `z4_zeilboot`), Windvaan (role `luchtschip_z4` -> travel map), lantern maker, bankier (`bank`), werkplaats (`werkplaats`), winkel, smid, wapens, kleermaker, markt, kok (free fish soup), visser, duinwachter, and 6 citizens.
  - **Knuffeltij (7):** opvang, wasguh, soepguh, and 4 refugees. The refugees are `GuhVariant.MAGER` guhs.
  - **Islands (12):** limonade, volleybal, feestguh, vliegermaker, windmeester, duikmeester, Opa Lichtstraal, imker, Madelief, Schelpie (on the turtle), Nevel, pierwachter and Hemelhoog.
  - **Kustwacht (8):** one at each campfire.
- **Secrets:** 8 knuffels, 6 flessenpost (lore about the Keizer, Nooitgenoeg, the Ringvinger), 4 treasures (loot as in zone 3), 4 viewpoints.
- **Verkenningsboek page "zone4":**
  - title `vr_kenner_van_de_lampionkust`, reward 80000 bronze;
  - categories: main, side, treasures, flessenpost, knuffels, viewpoints, bosses (Zeeslurf/Vliegervanger), campfires, areas.
- **Travel map:**
  - The place `zone4` is renamed to **`lampionkust`**. It stays a teaser until zone 3 is done (or you have been there).
  - Its gate is `zone3_klaar` + lv 50.
  - Its flight is `z4_vlieg`: the binding `z4_luchtlijn` wins, otherwise our opening scene / short flight.
  - The airship NPCs `z3_kapitein`, `kapitein`, `luchtschipper` and `z2_kapitein` got `lampionkust` in `QuestMotor.REIZEN` (added at runtime from our registry).
  - `z4_luchthaven` flies back to the other places.
- **Tracker route:**
  - In zone 4 with a target outside it, the tracker points to Windvaan first.
  - In zone 3 with a target in zone 4, it points to Kapitein Zoemzeil.
- **Fallback areas:** flat boxes `z4_*` (+ the catch-all `z4_lampionkust`), registered only when t-z4wereld has not registered that id. Area name keys `vadsrijk.kern.gebied.z4_*` are written too (see open points).
- **Dev commands:** `/guhs vadsrijk z4verhaal klaarzetten | hoofdlijn <1-7> | skip | quest <id> start|klaar|af | tp <plek> | status | vergeet | opening|vlieg|mist|feest|wolkwal|cliffhanger|wrak|zeeslurf`.
- **Gametests (`Z4VerhaalGameTests`):**
  - `elkeStapWieEnWaar`;
  - `hoofdlijnViaApi`: the whole chain through the real hooks to zone4_klaar + the z5 flag + 12/12 + the boat;
  - `spelletjesEnReiskaart`: the teaser → open travel map, volleyball, the kite contest, fishing.

## Contract for the other T slices (javadoc of `Z4VerhaalApi`)
| Slice | Hook |
|---|---|
| **t-luchtschip** | Read `Z4VerhaalApi.luchtschipNaarHoogvads(p)` (flag `z5_luchtschip_open`) or listen to `Z4VerhaalApi.ZONE4_KLAAR` to open line D (Opstijgpier -> Walvisrughaven). Optional bindings: `z4_luchtlijn` (arg "lampionhaven": your real line C instead of our flight) and `z4_cliffhanger` (a real first flight instead of our camera scene; the flags are already set when it is called). |
| **t-z4boot** | Bind `z4_zeilboot_geef` (z4_hq02 step 1: unlock/give the boat) and `z4_zeilboot` (the dock NPC's service). Our fallback binding of `z4_zeilboot` is only set when the role is still free at ServerStarted LOW. |
| **t-ambacht** | `NpcApi.meld(p, "z4_vis", "gevangen")` per fish in zone 4. Vanilla rod fishing is counted here as well, so do not report the same catch twice; if your Vissen also fires ItemFishedEvent, nothing extra is needed. The roles `werkplaats` / `bank` are yours to bind. |
| **bosses** (whoever builds them) | `meld(p, "z4_zeeslurf", "opgevrolijkt")` and `meld(p, "z4_vliegervanger", "verslagen")`. Kills of `vr_z4_*zeeslurf*` / `*vliegervanger*` / `*kootje*` are seen too. Bind `z4_zeeslurf` / `z4_vliegervanger` to start your fight when the player arrives; without a binding a short dialogue does it. |
| **t-z4wereld** | `zone4.wereld.Z4WereldPlekken#plek(String)` is found by reflection. See POI_VERZOEKEN_Z4.md for the ids and the decoration requests (grey knuffels along the waterline, big knuffels). Optional bindings: `z4_wolkwal_zakt`, `z4_feest`. |

## Shared-file changes (all marked `T-Z4VERHAAL`, add-only)
| File | Change |
|---|---|
| `VadsrijkFeature` | One line: `zone4.verhaal.Z4VerhaalRegistry.register` |
| `npc/Quests` | `BESTAND_Z4` added + read |
| `npc/QuestMotor` | `reisDoelen`: lampionkust only after zone3_klaar; `reisSlot` case "lampionkust"; `vliegNaar` case "lampionkust" -> `z4_vlieg` |
| `npc/Reiskaart` | `PLEKKEN` "zone4" -> "lampionkust"; `hier()` knows zone 4; `plek()` lampionkust is a teaser until zone 3 is done |
| `npc/client/ReiskaartScherm` | Id "zone4" -> "lampionkust" (spot + route line) |
| `npc/client/QuestlogScherm` | 4 Gids ids `z4_*` appended |
| `npc/VadsrijkNpcGameTests` | Teaser assert uses "lampionkust" |
| `tools/features/__init__.py` | `vadsrijk_t_z4verhaal` appended to FEATURES |

## Decisions made alone (open for the user)
- **Names:** all NPC and knuffel names are mine (Ankertje, Zeiltje, Windvaan, Gloeiwiek, Zachtje, Sopje, Lepeltje, Grijsje, Opa Dun, Vaaltje, Mama Pluis, Bubbel, Smash, Confetti, Wimpel, Zwiep (+ the kite "Henk"), Parel, Opa Lichtstraal, Nectar, Madelief, Schelpie + turtle Ouwe Dobber, Verdwaalde Nevel, Wolkje, Kapitein Hemelhoog, Kootje de Vliegervanger).
- **The knuffels' origin:** they fall overboard from the **Grauwe Vloot** of the Ringvinger (zone 6 Vinger, WERELDVERHAAL §5) in storms. That ties zone 4 to zone 6.
- **The zone-5 spy hint** in the cliffhanger is a white-and-gold elite cape dropping a grey fluff. It is subtle and names nobody, so zone 5 is free to choose its spy.
- **The main line has 7 quests** instead of ~6, because the arrival is its own step (as in zone 3).
- **Visiting Lampionhaven needs `zone3_klaar` + lv 50.** The main quest starts at Wasja (zone 3's last NPC).
- **The kite contest / lens / dance** are "stand there N seconds" games. The volleyball game is a click game in the dialogue screen. These are light by design ("cooling down").
- **The lighthouse keeper's story (s08)** gives one Keizer-lore moment: a Mika pointing with one finger from a grey fleet.
- **Area name keys `vadsrijk.kern.gebied.z4_*`** are written by my generator as a fallback. t-z4wereld owns them; if both write them, the later generator wins at integration. Delete mine when theirs exist (`GEBIED_NAMEN` in the generator).

## Open points / not done
- **Real coordinates:** all spots are render fallbacks until POI_Z4.md / `Z4WereldPlekken` appear. Integration should check that `Z4WereldPlekken.plek(String)` exists with that signature, or adapt `Z4VerhaalPlekken.WERELD`.
- **The turtle moves** (t-z4wereld/t-z4wezens). Schelpie and the big knuffel `groot_4` stand on its resting spot (2239,-445). If the turtle becomes an entity, they should follow it.
- **No real knuffel blocks are placed by me.** The finds are particles plus a dialogue. The world slice should put grey knuffel decorations on the POIs.
- **No AutoCheck client run** (no real zone-4 map in this tree).
- **The Gids "z4_zeilboot" text** assumes the boat is called with the boat slice's own key. Adjust it at integration if the boat has a key/command.

## Resume point / tests
- `compileJava` is green.
- `runGameTestServer -Pgt=Z4VerhaalGameTests,VadsrijkNpcGameTests`: **all 10 required tests passed** (3 new + 7 zone-1 NPC tests, incl. the renamed travel-map teaser).
- **Level cap:** kern `Levels.MAX` is **50** in this tree. The quests_z4.json level requirement is therefore capped at 50 (`LV_CAP` in the generator). Xp still uses the design levels 50-55. Raise `LV_CAP` when the cap goes up (someone else's slice).
