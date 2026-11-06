# NOTITIES_U_Z6VERHAAL — Vadsrijk zone 6 "Het Zijdeland", the story

Branch `110-u-z6verhaal`, worktree `guhs_vadsrijk/u-z6verhaal`, from eaee06d08 (110-t-int). Package `feature/vadsrijk/zone6/verhaal`,
generator `tools/features/vadsrijk_u_z6verhaal.py` (in FEATURES, last). Cheat sheet: `SPIEK_U_Z6VERHAAL.md`.
Sources: `guhs1100-avontuur/ZONE6_PLAN.md` (+ the user decisions Z6-6 at its end, which win), `WERELDVERHAAL.md`, `u-z6wereld/POI_Z6.md`.

## What is built
- **Level cap 70** (`kern/Levels.MAX` + `MAX_GENERIEK`). The xp curve is a formula (`40 + 25 * l^1.6`), so it simply continues: lv 60->61
  costs ~17.6k, 69->70 ~20.6k, 60->70 ~190k total. Checked: every `Levels.MAX`/`MAX_GENERIEK` user clamps by the constant (Spelers,
  KernPayloads HUD, Erfstukken, AmbachtXp, VadsrijkCommando dev args, Z5 mobs), nothing has a hard 60. Skill points = level - 1, so 69 at
  cap (the class tree in THIS tree has fewer nodes than that: spare points just stay unspent; **u-specs** reworks the tree: it should give
  room for 69 points / a lv-65 or lv-70 step if wanted). Ambacht cap is NOT touched (u-ambacht2 owns `AmbachtXp.CAP`).
- **Same quest system as zones 1-5**: quests in `data/guhs/vadsrijk/npc/quests_z6.json` (`Quests.BESTAND_Z6`, read after zone 5), texts
  under `vadsrijk.npc.q|naam|praat|plek|gids|tip.z6_*`, own texts `vadsrijk.z6verhaal.*`. Every objective has an explicit `plek` and says
  WHO + WHERE + WHAT (generator asserts, gametest checks). Quest gates clamped to `LV_CAP = 70` (generator); xp = the z3-z5 formula.
- **The transition** (`Z6Verhaal.wolkwaltocht`): when `z5_hq10_stormrand` is handed in and the player is free (no scene/dialogue), the
  epic film "De grote zwemtocht" plays: Moeder Wolkwal brums, swims east over the Wolkenzee to the pink cliffs, blows "Tot hier".
  **Only a film**: Hoogvads stays in place in the world. At its end the flag **`z6_lijn_e_open`** is set and z6_hq01 starts.
- **Airship line E** (`Z6Lijn`): added at runtime to `Lijnen.ALLE` (`z5_z6`, letter E, Staarthaven `haven_z5_z6_vertrek` <-> Luchthaven
  Rozenkloof `haven_z6_z5_vertrek`, phase 3300, ~70 s), unless a line `z5_z6` exists already. Gate `Luchtschepen.SLOT["z5_z6"]`: out
  only with `z6_lijn_e_open` + lv 60, back always. `Luchtschepen.AANKOMST` is wrapped: a landing at Rozenkloof plays the arrival film
  the first time and counts `z6:aangekomen`. Travel map destination **`zijdeland`** (Reiskaart + QuestMotor + a marker on the map),
  Havenmeester Zandloper (role `luchtschip_z5`, own list) flies back to Hoogvads and the islands; "zijdeland" is appended to the other
  captains' lists (shown once line E is open).
- **Main line "De Sterrenkaravaan" (10, `Z6Haken.HOOFD`)**: hq01 Land in zicht! (the swim, line E, Havenmeester Zandloper, Kloofmoeder
  Roosje in the Schatkamer) -> hq02 De karavaan van Oma Dadel (3 chores on the Rozenplein, **your bultguh at Stalmeester Hobbel**) ->
  hq03 Door de Roze Duinen (the Siq, the dune rest stop, Bobbeltje behind a dune, Koningin Wormelien in the distance, the Westpoort,
  Oma Dadel in the Karavanserai) -> hq04 Audiëntie (Grootvizier Kaneelstok, Sultane Rozijntje and the star map, the tutorial puzzle at
  Twinkel's telescope: Moeder Bron's first whisper; the Sterrenlens) -> hq05 Het Palmbaken (Palmguh Dadelpit, the waterfalls, the first
  beacon) -> hq06 Een tapijt van sterrenzijde (5 cocons, **Knoopje weaves the flying carpet**, test flight to the Saffraanbrug) ->
  hq07 De Zoutvlakte bij nacht (caravan camp, the Zoutbaken, De Zoutkolos wakes (dodge, a film), the Grauwe brief in the Mika camp) ->
  hq08 De Verzonken Sterrenwacht (Professor Stof, the dungeon -> the Sterrenkompas, the Ruïnebaken) -> hq09 De Spiegelzaal (the
  compass in the hall of mirrors, the unmasking of the Spiegelvizier, the rooftop chase, he flees to the tower) -> hq10 De Moederoase
  (the big caravan, the Duinbaken, the mirage, the Oasebaken, cheer up **Moeder Bron**, the foot of the tower: **Ringeling flees upward
  and seals the door**; title card + title "Ridder van het Zijdeland", flag `zone6_klaar` = the raid is open). The raid is NOT needed.
- **The star beacons** (`Z6Puzzels`): at each beacon spot (u-z6wereld's `<baken>_puzzel`) a star puzzle opens on the talking screen:
  follow the stars of a constellation in the order of the hint (Twinkel / Moeder Bron / Oma Dadel / a temple wall). A wrong star = a
  gentle line + start again. The last star lights it: meld `z6_baken:<id>`, a star shard, a beam from `<baken>_ster`, then **Moeder
  Bron's vision** (`Z6Verhaal.visioen`, 6 levels: her voice grows from a drip to a fountain per beacon). "Night puzzles": in daylight
  the Sterrenlens shows the night sky (a short Darkness + stars) so nobody waits for night. Right-click the Sterrenlens = how many burn,
  which is next. Per player `extra("npc").z6_bakens`.
- **The same engine** does the haggling (side 5, 3 merchants, "lief afdingen") and the cave song (side 12, the karavaanlied on 5 crystals).
- **15 side quests**: Bobbeltjes verjaardag, Water voor de hangende tuinen, Post in de rots, De verloren tapijtkwastjes (-> u-tapijt
  look `Bron.api("z6_kwastjes")`), Afdingen! (title "Sjacheraar"), De norias zingen niet meer, Badeendjes in de Kanaalwijk, Sterrenkijkers-
  examen (4 constellations = the 3 raid "Gedoofde Sterrenbeelden" + Het Kroontje), De Dadelapenbende, Watervalkoerier, Zout op de staart,
  Het lied van de Zoutgrot (-> look `Bron.api("z6_eerste_karavaan")`), Koningin Wormelien (field boss), De tabletten van de Sterrenguhs
  (6 lore tablets), Vluchtelingen langs de Oostroute (Mager Mientje, after hq10).
- **53 NPCs** (`Z6Npcs`): both cities (havenmeester, kloofmoeder, caravan, stable, carpet shop, gardens, post, sultane, grand vizier,
  mirror polisher, astronomer, bazaar merchants with shop roles winkel/markt/juwelier/smid, bank x2, norias, canal kid, the veiled
  lady), 5 profession teachers (QuestMotor.EXTRA -> t-ambacht workshops/book), the land (Dadelpit, Spetter, Korrel, Rimpel, Stof,
  Moeder Bron, Mientje + 3 refugees), 6 citizens, 6 guards. **Bankers**: `Bank.bankier("z6_bankier_kloof" / "z6_bankier_poort")`.
  **Kwartiermeester / Bondenbord**: NPC ids `kk_poort_rozenkloof|saffraanpoort` and `bond_bord_rozenkloof|saffraanpoort`, i.e. the id
  conventions of u-huizen (`Poorten.PREFIX`) and u-gilden (`bond_bord_*`): once those slices are merged their own KLIK_VOOR handles them
  (the Kwartiermeester's way back = where you clicked). Without them (this tree) a "binnenkort" line.
- **Verkenningsboek** page `zone6` (`Voltooiing.registreer`, title `vr_kenner_van_het_zijdeland`, 120000 brons): main, side, secrets
  (3 treasures + the Grauwe brief + the 5 beacons), 8 knuffels, bosses (Wormelien, Zoutkolos, Verzonken Sterrenguh, Ringeling (secret)),
  4 lookouts, 7 campfires, 7 areas.
- **Gids pages** z6_zijdeland/bakens/rijdieren/steden/toren, tips z6_veilig/puzzel/lens, FTB sections (chapter guhs_vadsrijk), hidden
  advancements `vr_z6_*`, titles vr_ridder_van_het_zijdeland, vr_sjacheraar, vr_vriend_van_ringeling, vr_kenner_van_het_zijdeland.

## Decisions made alone (open for the user)
- **The Spiegelvizier = Meester Glans** (aligned with u-z6raid, lead message): a Mika illusionist and **Ringeling's Knokkel**, who for
  twenty years was the palace's mirror polisher (in every mirror a bit of him: that is why in the raid he hides in mirror images). The real Grootvizier
  Kaneelstok is innocent (running gag: "I saw myself walk down the hall"). Unmasked in hq09; after that his NPC is "a mirror shaped like
  a guh". `Z6VerhaalApi.SPIEGELVIZIER`. (Earlier draft name "Kootje Weerschijn" is gone everywhere.)
- **Ringeling after the raid** (user decision): `Z6VerhaalApi.ringelingOpgevrolijkt(p)` -> flag `z6_ringeling_vriend`, scene "Opgevrolijkt!"
  (her ten rings roll away, she turns pink, gives you the Keizer's ring item), title "Vriend van Ringeling", then **F4 adapted**: the
  throne room is not built, so a narrated camera drift east: Krabbel reads her letter ("Beste Keizer. Ik stop. Ik heb sterren gezien.
  Groetjes, Ringeling. ... Uw ring ligt bij de ridder." + a drawing of a tongue), the grey finger taps: "Dan... kijk... ik... zelf." and
  the Oog turns away from the Zijdeland towards Hoogvads (a hook for the next part). Then the epilogue (caravans return, the river rises).
  **She is an ally NPC in Saffraanpoort from the start**, as "Gesluierde Dame" in the Paradijstuin (no spoiler: NPCs are shared by all
  players): before your raid clear she says "Ken ik jou? Nog niet, schat."; after it she talks as Ringeling (4 lines). The scene waits for
  a quiet moment in the Vadsrijk (raid instance elsewhere = it plays when you are back).
- **The order of the story differs a little from ZONE6_PLAN 4.1** so every hand-in NPC can talk: hq02 is offered by Oma Dadel herself,
  hq05/hq08 go back to Twinkel, hq06 is offered by Knoopje (the weaving is a click on her: carpet given there, then the test flight),
  hq09 is offered by Twinkel. The Duinbaken is part of hq10 (ZONE6_PLAN had it twice), so side quest 13 is "Koningin Wormelien".
- **Zone 5's hq10 texts were changed** (vadsrijk_t_z5verhaal.py, marked U-Z6VERHAAL): "De Stormrand" -> "Sterren in het oosten" (quest id
  unchanged), the cliffhanger now shows the pink land with stars going out + the ten toe rings, the logbook/letter no longer mention the
  Stormrand / De Gouden Greep, and Windvaan's hand-in ends with "Hou je vast!" -> the great swim. (The cliffhanger Java scene still draws
  its grey/lightning particles: cosmetic, Z5Verhaal.cliffTik, left alone.)
- The puzzles are dialogue puzzles (no blocks to place): robust without the world; "night" is shown through the Sterrenlens.
- The dungeon (hq08): `Z6VerhaalApi.KERKER` starters / reflection `zone6.wezens.baas.Z6Bazen.start(p, "sterrenwacht")` (u-z6wezens); else a
  fallback scene gives the compass (the chain never blocks). End: `Z6VerhaalApi.kompasGevonden(p)` or a kill of `guhs:vr_z6_*sterrenguh*`.
- Rewards from slices that are not in this tree wait with a flag and are retried every 5 s: the bultguh (`RijdierSoort` id "bultguh",
  flag `z6_bultguh_verdiend`), the carpet (`tapijt.Tapijt.geef` by reflection, flag `z6_tapijt_verdiend`), the two carpet looks.
- Dutch only (en_us = the same Dutch, English comes at the end). Quest levels: hq01 60 ... hq10 69, side 60-69.

## For the other slices
- **u-z6wezens (DONE, a1114de75; wired by reflection, see POI_VERZOEKEN_Z6_WEZENS.md)**: hq02 Hobbel -> `zone6.bultguh.Bultguh.geef(p,
  "naturel")` (fallback `Rijdieren.geef(p, "bultguh")`, else a waiting flag); hq08 at the dungeon entrance -> `zone6.wezens.baas.Z6Bazen.
  start(p, "sterrenwacht")` (fallback scene); the compass objective is now `z6_sterrenwacht:kompas` = exactly the meld u-z6wezens sends
  (`Z6WezensApi.KOMPAS`); hq07 after the Zoutkolos film -> `Z6Bazen.start(p, "zoutkolos")` (dodging is fine, no objective); side 13 waits for
  `z6_baas:wormelien` (their meld) and calls `Z6Bazen.start(p, "wormelien")` once per visit near her dunes; pets via
  `Huisdieren.geef`: springpiepje (side 1), dadelaapje (side 9), bobbeltje (hq10), with a waiting flag when not in the tree. Their dungeon's
  second boss is "Kootje Glimmertje" (no clash with the story: hq08 only says "a Mika with a giant snuffer").
- **u-z6raid**: it calls `Z6Verhaal.ringelingOpgevrolijkt(p)` by reflection on Ringeling's defeat (public static, delegates to the API;
  once per player) plus its own `Z6RaidHaken.OPGEVROLIJKT`. Raid open = `Z6VerhaalApi.torenOpen(p)` (= `zone6_klaar`). At the end: `Z6VerhaalApi.ringelingOpgevrolijkt(p)` per player
  (or kill `guhs:vr_z6_*ringeling*`). Listeners: `Z6VerhaalApi.RINGELING`, `ZONE6_KLAAR`. Boss names: Zandwachter, **Meester Glans de
  Spiegelvizier**, the 3 Gedoofde Sterrenbeelden (De Grote Knuffel, De Vliegende Bultguh, De Kleine Pluis: the exam constellations),
  Hertogin Ringeling. Her last line in hq10: "Daar zijn er minstens twee voor nodig, ridder. Kom maar terug met vriendjes. Ik heb thee."
  Ringeling must be CHEERED UP (not flee in smoke): the raid's end text should match the scene "ringeling" (she drops the rings, giggles).
- **u-z6wezens**: add `RijdierSoort.BULTGUH` with id **"bultguh"** (bron "quest"): Hobbel's hq02 step calls `Rijdieren.geef(p, "bultguh")`
  and waiting players get it automatically once the species exists. **The second seat** (decision Z6-6) is u-z6wezens' (the mount entity);
  not done here. Field boss Wormelien: entity id containing "wormelien" (`guhs:vr_z6_baas_wormelien` in the book) or
  `meld(p, "z6_baas", "wormelien")`. The Zoutkolos `guhs:vr_z6_baas_zoutkolos` (book only). Mika-Lampendovers near the beacons:
  not required by the story (the puzzle is the gate).
- **u-tapijt**: we call `tapijt.Tapijt.geef(p)` (reflection) at Knoopje (hq06) and give every look with `Bron.api("z6_kwastjes")` (side 4)
  and `Bron.api("z6_eerste_karavaan")` (side 12, the cave chest).
- **u-z6wereld**: Z6VerhaalPlekken uses your POI_Z6 ids (Z6WereldPlekken.plek by reflection, same pattern as zone 5). Ids I use that are
  NOT in POI_Z6.md yet (my fallback numbers stand): `staarthaven` (Hoogvads, for line E: really zone 5's `haven_z5_z6_vertrek`),
  `saffraanpoort_poort_oost` (the roof chase's end), `dak_2`, `toren_top` (the tower's top, for the camera), `oostroute_luchtspiegeling`,
  `zoutvlakte_mikakamp` (the abandoned Mika camp with the letter), `examen_1..3` (exam roofs: Sterrenwacht roof, bridge gallery, palace
  minaret), `schatkamer_koepeltje`, `paleis_minaret` (lookouts), `palmenkloven_palmenbos`, `zoutvlakte`, `ruinedal`,
  `bazaar_smidse_leermeester`, `oase_tent_1..3`, `z6_burger_1..6`, `z6_wacht_1..6` (stand spots), `z6_taart_1..3`, `z6_dadels_1..6`,
  `z6_zoutkreeft_1..5`, `moerbei_4..5`, `geheim_z6_*` (you have them). Please also place: a lectern/letter in the Mika camp, campfire
  blocks, and keep `<baken>_puzzel` reachable. Areas: I use your ids (`z6_roze_duinen`!); my fallback boxes only register when yours are
  missing.
- **t-luchtschip / u-luchtschip**: line E is added at runtime (Z6Lijn). u-luchtschip's `Poorten` (gate explanations per line) needs an
  entry for E (quest `z5_hq10_stormrand`, lv 60) and its `dichtUitleg` test iterates all lines. I changed `VadsrijkLuchtschipGameTests.
  dienstregeling` to expect 6 lines.
- **u-specs**: the cap is 70 now; the third job step was at lv 58; a step/keystone room up to 69 points is theirs.

## Shared-file changes (all marked `// U-Z6VERHAAL`, add-only)
| file | change |
|---|---|
| `VadsrijkFeature` | one line `zone6.verhaal.Z6VerhaalRegistry.register` |
| `kern/Levels` | `MAX` + `MAX_GENERIEK` 60 -> 70 |
| `npc/Quests` | `BESTAND_Z6` + `leesNa(...)` after zone 5 |
| `npc/QuestMotor` | `reisDoelen` filter "zijdeland" (`Z6Verhaal.magZien`), `reisSlot` case "zijdeland", `vliegNaar` case "zijdeland" -> action `z6_vlieg` |
| `npc/Reiskaart` | "zijdeland" in PLEKKEN, `hier()` zone 6, teaser until `magZien` |
| `npc/client/ReiskaartScherm` | marker `zijdeland` {0.97, 0.30} + route hoogvads->zijdeland (no art yet) |
| `npc/client/QuestlogScherm` | 5 Gids ids `z6_*` appended |
| `luchtschip/VadsrijkLuchtschipGameTests` | dienstregeling: 6 lines (+ E) |
| `tools/features/__init__.py` | `vadsrijk_u_z6verhaal` appended to FEATURES |
| `tools/features/vadsrijk_t_z5verhaal.py` | zone 5 hq10 / cliffhanger / logbook / letter texts (Stormrand -> Zijdeland) |
| lang nl_nl/en_us | ~1160 keys (generator; zone 6 own keys + the hub NPC names) |
| Runtime only (no file change) | `Lijnen.ALLE += E`, `Luchtschepen.SLOT["z5_z6"]`, `Luchtschepen.AANKOMST` wrapped, `QuestMotor.REIZEN` (+ zijdeland), `Bank.bankier` x2, `QuestMotor.EXTRA` (teachers) |

## Resume point / tests
- Gametests `runGameTestServer -Pgt=Z6VerhaalGameTests`: elkeStapWieEnWaar, hoofdlijnViaApi, overgangEnLijnE, ringelingNaDeRaid,
  levelCap70: **5/5 passed** (2026-11-06 16:49); VadsrijkLuchtschipGameTests with line E: passed (16:19). Also run `-Pgt=VadsrijkLuchtschip` (the line count).
- Not done: real geometry (u-z6wereld), the fights (Wormelien, Zoutkolos, dungeon, raid), the bultguh species + second seat, the playable
  F4 throne room, travel-map art for the Zijdeland, English texts, the Mika-Lampendovers as a story step.
