# NOTITIES_NPC — Vadsrijk slice "npc" (Guhs 1.10.0, zone 1)

Branch `110-vr-npc`, worktree `guhs_vadsrijk/w-npc`, package `feature/vadsrijk/npc`, ids `vr_npc_*`, lang `vadsrijk.npc.*`,
generator `tools/features/vadsrijk_npc.py` (+ `tools/features/vadsrijk_npc_wiki.py`, not a feature module).

## What is in this slice

| part | where |
|---|---|
| NPC guhs (`vr_npc_guh` = a `GuhEntity` subclass, drawn by `GuhRenderer` with the existing guh clothes) | `RijkGuh`, `Npcs` (40 NPCs + 12 campfire ridder-guhs, names/lines in the generator), spots in `NpcPlekken` |
| Idle behaviour | look at players, wave when you come close, a random emote now and then; citizens wander 4 blocks, cook/angler/Sjors sit; escorts follow you |
| Spawning | code: every second, each NPC whose spot is loaded with a player within 96 blocks gets its one guh (never saved; floor search ±12) |
| Quest data | `data/guhs/vadsrijk/npc/quests.json` (generated), `Quest`, `Quests` (42 quests: 12 main, 22 side, 1 mount, 6 class proofs, 1 secret) |
| Quest state | `SpelerData.extra("npc")`: `q` {id: {s, v}}, `zone1_klaar`, `knuffels`, `aanwijzingen`, `schatten`, `wezens`, `adv_<klasse>`, `spion_ontmaskerd` (`Questlog`) |
| Dialogue | `QuestMotor`: click-to-continue through `feature/verhaal/Praat` (knuffeldal PraatScherm), speakers npc / jij / another NPC |
| Objectives | talk, opvrolijk (type / area / any), elite, collect (quest items drop for you while needed), reach (area or poi), campfire, boss, escort, secret (poi / counters), level, training dummy, class, meld (events of other slices) |
| Hooks | `NpcHaken`: `Haken.VERSLAGEN` (+ party within 48), `GEBIED_BETREDEN`, `KAMPVUUR`, `LEVEL_OP`, `FLAUW`, `BINNEN`; LeftClickBlock on target/hay in Knuffelkamp |
| Markers | `NpcMarkers` + client hook on `GuhRenderer`: gold "!" main, yellow "!" side, "?" hand in, pale "?" talk/escort (per player) |
| Questlog (J) | `client/QuestlogScherm`: tabs Quests / Geheimen / Wezens (the bestiary) / Gids (the Avonturiersgids, 15 pages) |
| Story | `Verhaal`: opening scene, the BAM (dragon + grey fluff + wilting flowers + Klodderreus), airship flights, cliffhanger -> `zone1_klaar` |
| Secrets | 10 knuffels, 5 Mika clues (start the secret quest), 5 treasures; glitter only you see, found at radius 3 |
| API for others | `NpcApi.meld(p, soort, waarde)`, `zone1Klaar(p)`, `ZONE1_KLAAR`, `QUEST_KLAAR`, `NpcRollen.koppel/koppelMet` |
| Dev command | `/guhs vadsrijk npc log | quest start|klaar|vergeet <id> | reset | opening | bam | cliffhanger | vlieg stad|kamp | npcs | tp <id>` |
| FTB | `ftb(fq)` in the generator: chapter `guhs_vadsrijk`, every quest = hidden adv `guhs:quest/vr_<id>` granted on hand-in |
| Gametests | `VadsrijkNpcGameTests` (3): quest data, progress via the kern hooks + clues + spy, hand-in through an NPC + `zone1_klaar` |

## The story (zone 1), in short
Knuffelkamp: Sergeant Pluizebol -> Kok Knapperkorst -> Zuster Zachtjes (class) -> Meester Kussensloop (dummies, first weapon) ->
the campfire -> **BAM**: Vahoegnir flies over (grey, crackling black fluff), fluff rains down, the camp's flowers wilt, the Grauwe
Klodderreus lands in the pond -> cheered up, he stays as the Kampklodder. Airship to Zwamzoet -> Grootmeester Vadsvoet tells about
the Vadsloosheid -> the scars (Klodderweide, a paw stamp on the fluff) -> Sporenwoud (Boswachter Mos saw small figures with sacks,
miauwing) -> Druipgrotten (escort Mijnwerker Mopje: "puntoortjes, een staart, 'het plan'") -> the Zwamtempel -> the Opperzwam
(a Mika pin falls out of his hat) -> the Grootmeester: **the Mika's** turned the dragon grey and harvest his vads; the dragon circles
over lands no-one has seen -> cliffhanger scene "Wordt vervolgd..." + `zone1_klaar`.
Secret line: 5 clues -> unmask "Akim de Kaasguh" (bazen's MikaSpion) -> rooftop fight -> back to the Grootmeester.

## Adapters to other slices (by reflection; this tree compiles without them) — integration: check they still match
| role / hook | calls | fallback here |
|---|---|---|
| class pick (Zuster Zachtjes) | `klassen.KlasseBalie.kiesEerste(p)`, else `.open(p)`; the pick is seen via `extra("klassen").gekozen` (polled) | own choice between `Klassen.alle()` |
| ordetempel (Grootmeester) | `klassen.KlasseBalie.open(p)` | "nog niet" line |
| class proofs lv 10/20 | `klassen.Advancement.doe(p)`; we set `Advancement.VIA_BALIE = false` on server start | own title card + `extra("npc").adv_<klasse>` |
| smid / kruidenier / uitrusting / kleermaker / markt / stal | `economie.Smid.open`, `Winkels.open(p, "drankjes"|"uitrusting"|"cosmetica")`, `Markt.open`, `Stal.open` | "nog niet" line |
| mount quest reward | `economie.Rijdieren.geefEerste(p)` | message |
| bank | (economie has no bank) | Bankier only chats |
| airship | `wereld.WereldReizen.vlieg(p, Dok.KAMP/HAVEN, ...)`; we set `WereldReizen.MAG_VLIEGEN` (haven only after hq05) | own flight scene + teleport |
| dragon | `wezens.draak.DraakVlucht.vlieg(level, path, 170, true)` (BAM), `.cirkel(...)` (cliffhanger) | own shadow/roar particles |
| Kampklodder | `wezens.Kampklodder.plaats(level, WezensPlekken.KAMPKLODDER)` when the Klodderreus is beaten | none |
| Mika-spion | we set `bazen.mika.MikaSpion.MAG_ONTMASKEREN` (5 clues -> right-click Akim unmasks; the predicate also reports it); the capture counts as any boss beaten in the Zwamzoet area; `MikaSpion.ontmasker(p)` when our own NPC asks | our own disguised "Akim" (only spawned without bazen) + title card |
| BAM boss | own arena `npc:kampvijver` (POI pond arena, kern's Klodderreus, spawns only while someone is in hq05 near the pond) | — |

Other slices may simply `NpcRollen.koppel(...)` instead; a binding always wins over the reflection.

## Kern adapters / things integration must lift
1. **`Gebieden.AANKOMST`** is still (0,65,1130); wereld wants (96,87,1015). The opening scene only checks "first time inside the
   knuffelkamp area", so it works either way; the scene camera is aimed at (96,87,1015).
2. **Klodderreus arena**: if kern/bazen/wereld also register a Knuffelkamp pond arena, bind `NpcRollen.koppel("bam_baas", ...)`
   (then we skip ours) or drop `Verhaal.registreerArena`. Ours hides its respawn board under the island on purpose.
3. **Guhdex pages**: the Guhdex is keyed on the shared `GuhVariant` enum, so new pages would edit a shared file. Instead the
   questlog's **Wezens** tab is the zone-1 bestiary (page texts `vadsrijk.npc.wezen.<entity path>`, filled on first opvrolijk).
   If the lead wants real Guhdex pages, add GuhVariants at integration and reuse those texts.
4. **FTB**: `CHAPTERS` in `tools/make_ftbquests.py` needs an entry, and `CHAPTER_VERSION` -> 57 (shared file, not edited here):
   ```python
   "guhs_vadsrijk": dict(
       title="&dHet Vadsrijk", sub="Word rekruut van De Knuffelorde", colour="pink", icon="guh:golden",
       banner=("guh:normal", "guh:golden"), welcome=(["guh:normal", "guh:golden", "guh:mint", "guh:snow"], None), links=["guhmension"],
       intro=["Vind de ingestorte ordetempel in de Guhmensie en stap door het portaal: Het Vadsrijk, een eigen avonturenwereld.",
              "Word rekruut van &dDe Knuffelorde&r, kies een klasse en vrolijk op wat grauw en knorrig is geworden. De quests vinken "
              "zichzelf af zodra je ze in het Vadsrijk inlevert. " + NIKS_OP_SLOT]),
   ```
   Until then the module's quests fall into "Minigames & bijzondere plekken" (make_ftbquests' default for unknown chapters).
5. **Wiki**: `tools/features/vadsrijk_npc_wiki.py` (WIKI dict like guhs190_wiki.py) must be added to the docs step.
6. **Tips** (kern `Tips.toon`): we trigger `vadsrijk.npc.tip.quests` (first quest), `.loot` (first quest gear), `.flauw`
   (Haken.FLAUW), `.elementen` (first element mob cheered up), `.skill` (after the fallback class pick). Texts written for the
   other slices to trigger: `.skillboom` (klassen), `.sterren` + `.markt` (economie), `.party` (kern), `.levelop` + `.kampvuur`
   (kern already has its own `vadsrijk.kern.tip.levelop/kampvuur`; use whichever reads better).
7. **Quest item looks**: the 9 `vr_npc_*` items use vanilla textures (gray dye, string, red mushroom...). An art pass may add own icons.
8. **Mob ids** used in the bestiary texts come from the wezens/bazen worktrees (vr_mob_klodder ... vr_mob_knaag, vr_mob_tempelridder,
   vr_baas_*). Quest objectives count by **area** (`gebied:<area>`), so they never depend on the exact mob ids.

## Decisions made alone (open for the user)
- NPC names and all quest texts (see the generator). The spy is bazen's "Akim de Kaasguh" (our early name "Marktguh Wim" is gone).
- The class proofs (lv 10/20) are Grootmeester quests: lv 10 = level + 15 cheer-ups in the Sporenwoud; lv 20 = level + the Opperzwam.
- Rewards are first guesses (xp 60..3000, coins 40 brons .. 50 zilver, gear rolls Ongewoon..Episch).
- The flowers wilt only for the player who sees the BAM (fake block updates), they bloom again after the Klodderreus.
- Escort NPCs rest 5 minutes at the destination (so you can hand in there), then walk home.
- Quest items only drop for you while you still need them (no inventory clutter).
- Hand-in rewards are given when the hand-in dialogue opens (nothing can be lost); the story action (BAM, cliffhanger) runs when
  you close/finish that dialogue.

## Resume point
Everything compiles. Gametests `-Pgt=VadsrijkNpc`: **3/3 passed** (questDataKlopt, voortgangViaHaken, inleverenEnZone1Klaar).
AutoCheck (once, `tools/autocheck/vadsrijk_npc.txt`, proeftuin): all 40 NPCs + a campfire ridder render with their clothes and
names, the gold "!" marker draws above the sergeant, the quest dialogue opens with pages (1/4, Verder/Doei). Found + fixed after
the run (not re-run, one run allowed): the questlog payload did not open while the chat screen was up (dev command); it now always
opens. The BAM screenshot shows the falling grey fluff but no camera move/title: kern's `SceneSpeler` (client scene player) was
still a stub in this tree, so all RijkScenes (opening, BAM, flights, cliffhanger) need kern's version to look right.
Not done: a run on the real map (the tiles were not in this tree).
