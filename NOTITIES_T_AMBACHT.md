# NOTITIES t-ambacht: the Ambachten (professions)

Branch `110-t-ambacht` (from dbf82899d). Java: `feature/vadsrijk/ambacht/` (+ `client/`). Generator: `tools/features/vadsrijk_t_ambacht.py`.
Design: `guhs1100-avontuur/VOORSTEL_PROFESSIONS.md` with decisions A1-A3. The user's cheat sheet is in `SPIEK_T_AMBACHT.md`.

## What is built
### Phase A
- **9 Ambachten** (A1): Mijnen, Houthakken, Plukken, Vissen and Imkeren (its own ambacht, A1), plus Smeden, Naaien, Koken and Brouwen.
  - Each has a level 1-50 (`AmbachtXp.CAP`, A1: +5 per zone later).
- **Xp curve** (`AmbachtXp`): the combat shape `40 + 25 lv^1.6`.
  - A gathering action gives a fixed amount per tier (T1 30 / T2 52 / T3 62). That gives:
    - 1 -> 20 in ~1.6 h on T1;
    - 20 -> 35 in ~3 h on T2;
    - 35 -> 50 in ~5 h on T3 (at ~8 actions a minute).
  - A tier more than 8 levels below you gives half.
  - Making: 0.7 x ingredient units x the tier amount.
  - **Generic xp:** each action gives 1/900 of a generic level (~20 % of what fighting gives per hour, A2). It never goes to the class level.
  - **Uitgerust:** offline builds up 1 s of double xp per 6 s away, max 2 h. Each action uses 7 s.
- **AmbachtData** = `SpelerData.extra("ambacht")`. The layout is in the class doc.
  - **t-karakter:** nothing keys on the UUID. When t-karakter makes the extra tags per character, the Ambachten follow automatically.
- **Ambachtenboek** (key **Y**, the Vadsrijk category; also a collection tab, order 40). Tabs:
  - Overzicht;
  - Recepten = the recipe book;
  - Bestellingen;
  - Ranglijst;
  - Plekken.
- **Gathering spots** (`Verzamelen`, `PlekWereld`, `AmbachtPlekken`):
  - Spots are per player. The client draws your OWN spots: block models, the bouwhulp ghost technique, `PlekTekenaar`. They are not real blocks.
  - After a harvest the spot is empty FOR YOU for 60-120 s. It comes back at another of its 4 positions.
  - Optional 2-4 s minigame (`client/Minigame`, the use key): tap (Mijnen 3x, Hout 2x), hold-release (Plukken, Imkeren), bite (Vissen).
    - The score 0..2 = the quality (gewoon / vadsig / extra vadsig).
    - Sneak-click skips it.
  - Rare spots: 5 % on refill. They glow golden and glitter, give 3x, and have a 35 % chance on the rare material.
  - Light anti-AFK: >10 min within 5 blocks halves the yield.
  - Mijnen "perfect" on T2/T3: 25 % chance on a raw crystal (the zone-2/3 kinds).
  - Vissen: night and rain fish.
  - Tier level gates: T2 needs 20, T3 needs 35.
- **Imkeren** (`Imkeren`):
  - hive stands in 3 zone-3 meadows, each with its own honey kind: bloesem, schemer, wortel;
  - click a stand to place your hive (max 1/2/3/4 at lv 1/15/30/45);
  - combs fill in real time, faster with more flowers (the flowers are scanned and stored); the client draws bees flying between the hive and the flowers;
  - harvest = the smoke minigame (score 0 = Plakkerig for 3 s, never damage);
  - per comb: honey + half a wax + a chance on Koninginnengelei.
- **Koken** (`Eten`): 9 dishes.
  - One buff at a time, 30 min of real time. It stays when you faint (it lives in AmbachtData, not in the status list).
  - Vadsigheid makes it up to 30 % stronger.
  - Hooks: `Spelers.EXTRA_STATS` (stats) and `Spelers.XP_FACTOR` (the Ridderontbijt's +5 % combat xp). The Verzamelaarshapje gives ambacht xp and double yield.
  - Feestmaal: the whole party within 10 blocks + "Gezellig" (Kritiek +1, Heal-kracht / Elementschade / Elementweerstand +2 %).
- **Brouwen** (`Brouwsels`):
  - 7 potions (the shop numbers x Vadsigheid), in the SAME shared 10 s potion cooldown (`Drinken.eigenschappen(true)`);
  - crystal cutting: 3 ruw -> 1 geslepen (lv 15), 3 geslepen + 1 glinstergruis -> stralend (lv 40). It uses z2-systemen's `Kristallen` items + quality.
- **Workshops in the hubs** (`AmbachtNpcs`, A3): one teacher guh per workshop.
  - Zwamzoet (plein): smidse, naaiatelier, keuken, brouwerij + the 4 gathering masters + a board guh.
  - Kandijkom (plein): 4 workshops + a board guh.
  - Honingtak (markt): 4 workshops + a board guh; the bijenstal (Imkerguh Zoemie) is at the Raatweide.
  - Placement: runtime, a ring of free standable floor round the hub's POI, once a player is within 96 blocks. They join `Npcs.voegToe`.
  - A click goes through `QuestMotor.KLIK_VOOR`. Choices: Werkplaats / Receptenboek / Vertel eens... / Dag. Gathering masters have "Waar?"; the board guh has Bestelbord / Ambachtsbord.
  - The workshop screen (`client/WerkplaatsScherm`) only works within 12 blocks of the guh you opened it at.
- **Market:** materials stack to 64 (one quality per stack: component `guhs:vr_amb_kwaliteit`), so the existing Markt lists whole stacks. Shops buy them via the new `Waarde.EXTRA` hook.
- **Verkenningsboek:** its own page `zone_ambachten` "Ambachten" (sorted after the zones, so it does NOT count for the zone %, as the proposal recommends).
  - Contents: spots found per zone, the 9 fish, the 3 honey kinds, the recipes made.
  - 100 % gives the title Ambachtskenner + 2 goud.

### Phase B
- **Maatwerk** (`Maatwerk`):
  - Smeden makes sword (ridder), bow (jager), staff (magier), helm and borstplaat; Naaien makes broek and schoentjes. Each at T1/T2/T3 (gear lv 15/30/45).
  - Episch, base stats = 0.9 x the zone's dropped set/weapons (per tier).
  - You CHOOSE 2 bonuses; the rest rolls. +1 guaranteed socket (max 3). No set bonus (its own base types `mw_t<t>_<soort>`). Tradeable.
  - Vadsigheid pushes the chosen bonuses up their range (100 %: top 20 %).
- **Meesterwerk jewellery** (A3: BiS): ring, ketting and oorbel per tier.
  - It needs a boss core (Zwam / Splinter / Wortelkern).
  - Legendarisch, 3 chosen bonuses + 1 property: Schade tegen bazen +8 %, tegen elites +10 %, eerste klap +25 %, minder schade van bazen -6 %, onder 30 % Vads +12 %. The properties work via `Schade.VOOR_KLAP`.
  - Bound. The T3 base stats are above the zone-3 drops (honingring/harsketting/sporenoorbel).
  - Boss cores: every `VadsrijkBaas` 15 % per helper (by mob level: <=20 zwam, <=35 splinter, else wortel), elites 3 % a Kernsplinter. 5 splinters = a core at the smidse.
- **Bestelbord** (`Bestelbord`):
  - 3 daily orders, 06:00 Amsterdam (the `Z3Data` day service), from the pool that fits your levels;
  - kinds: breng (hand in), maak, verzamel; one free swap a day;
  - reward: 8 % of a level of xp + munten;
  - 12 in a week = the Ambachtskist (2-3 kernsplinters, a rare material, 5 zilver).

### Phase C
- **Ambachtsbord** (ranking): `Ranglijst` SavedData. Top 10 per ambacht + total, ties by who was first. No chat announcements.
- **Website export:** JSON per list in `<server>/guhs-web/live/hiscores/ambacht_<id>.json` + `ambacht_totaal.json`.
  - Override with the system property `guhs.web.map`.
  - Format in the `Ranglijst` doc, following VOORSTEL_WEBSITE §5.2. s-site had no NOTITIES/JSON format yet: this follows the proposal.
  - Written atomically off-thread, every 5 min, at logout and on `export`. `export` also writes `ambachten_recepten.json` (the recipe data, §5.3).
- **Stories:** 5 steps per ambacht (lv 1/10/20/35/50), 3 pages each (teacher / you / teacher), with the lore from the proposal:
  - Smeden: the Aambeeld van de Orde;
  - Koken: Oma Truus' receptenboek, ending on the Keizer's cookie;
  - Brouwen: the ont-grauw-drankje;
  - Naaien: the quilt for Hoogvads;
  - Vissen: a Koraalduik teaser;
  - Imkeren: the Mika-imker.
  - Rewards: munten + 15 % of a level; the first gathering step gives 3 of each T1 material; the last step sends a keepsake home by Post.
- **Home, decoration only** (A2): Naaien/Smeden "post" recipes (clothes: GRILL_SCHORT, VISSERSHOEDJE, TUINHOEDJE, STRAW_HAT, KAASMIJN_HELM; deco: economie's 4 deco blocks) go by `economie.Post`.
- **Titles:** Leerling-<ambacht> (lv 25), Gezel-<ambacht> (lv 50) per ambacht, and Ambachtskenner (book 100 %). They show outside the dimension (the titles feature).
- NOT built (as asked): Helperguhs, Het Grote Werk.

## Decisions I made myself
- **Key Y** (free; H = einde2, O/M = erfenis).
- **No tools needed:** you gather by hand. The proposal's tool set (Knuffelhouweel, ...) is left for later.
- **Placement:**
  - Every place is a POI id + a terrain scan at runtime. There are no fixed coordinates, so the world shift moves everything along.
  - The scan is cached in SavedData `vr_ambacht_plekken` with the anchor it was made for; a moved anchor means a re-scan.
  - Strict rules first (natural floor, never paths/buildings/leaves; fish = still water with a shore), then a relaxed pass if too little was found.
  - The workshop guhs pick a free floor in a ring of r 7-16 round the square.
- The minigame is client-side (it trusts the score). Anti-macro: a random target every time, and the server allows 1 harvest per 15 ticks.
- **Rare spots:** the glitter is only visible to the owner (the spots are per player).
- **Recipes are known by level** (no recipe drops). The recipe book shows everything, with grey for "not yet".
- **Food buffs use real time** (they also run down while offline).
- **Generic xp** goes only to the generic level (`Spelers.geefXp(p, n, "ambacht", null)`).
- **Imkeren honey kinds:** one per zone-3 meadow (zone 4/5 kinds come with those zones).
- **Not built:** Meesterschort (lv 100 is beyond the cap); Knutselpenningen + ambacht shop; player orders.

## Shared-file changes (all add-only, marked T-AMBACHT)
- `VadsrijkFeature.java`: register + payloads line.
- `client/VadsrijkClient.java`: the init line.
- `economie/Waarde.java`: `EXTRA` list (sale value per item) + the loop in `verkoop`.
- `tools/features/__init__.py`: `'vadsrijk_t_ambacht'` in FEATURES.
- lang `nl_nl.json`/`en_us.json`: ~650 keys added. The generator also writes `vadsrijk.npc.naam/praat.amb_*` (my NPCs) and `gui.guhs.titels.naam.vr_amb_*`.
- Hooks used (no edits): `QuestMotor.KLIK_VOOR`, `Praat.luister("vr_amb")`, `Npcs.voegToe`, `Spelers.EXTRA_STATS`/`XP_FACTOR`, `Schade.VOOR_KLAP`, `Haken.VERSLAGEN`, `Voltooiing.registreer`, `Titels.registreer`, `Premies.meld(p, "ambacht", n)`, `CollectieTabs.registreer`.

## Interfaces for other slices
- **t-opslag (materiaaltas):** set `MateriaalTas.BRON` (interface `stop / tel / neem`).
  - Gathered materials then go into the bag. Recipes and orders count and take from the inventory first, then the bag.
  - Without it everything uses the inventory, with overflow to kern's verloren-gevonden chest.
- **s-site:** the hiscore JSON format above + `ambachten_recepten.json`.
- **t-wereld:** nothing needed. If a POI id I use is renamed, `dataKlopt` (gametest) fails and lists it.
  - The ids I use are in `AmbachtPlekken`: e.g. kampvuur_kamp, kampvuur_klodderweide_1..3, kampvuur_sporenwoud_1..3, kampvuur_druipgrotten_1..3, vijver, waterval, kampvuur_haven, druipschacht_meer, plein; z2 kampvuur_galmhellingen_1..3, kristalbos_1/5/9, kampvuur_splinterkloof_1/3/5, geheim_ijsgrot, kampvuur_knuffelmeer_1/3, warmwaterbron, plein; z3 lichtkeverbos, paddenbloesemdal, kampvuur_mosvoet, kampvuur_wortelweg_1, wortelgewelf, kampvuur_wortelgang_1/2, honingmoeras, mosvoet_waterrad, honingvijver_1, raatweide_plein, markt.

## Resume point / open
- See the end of this file for the test status.
- Not seen in a client yet:
  - how the spots look (model choice/scale in `PlekTekenaar.model`);
  - whether the scans find good spots in every group (`/guhs vadsrijk ambacht plekken` per area);
  - the workshop guhs' places.
- Possible tuning: `Verzamelen.LEEG_MIN/MAX`, `ZELDZAAM_KANS`, `AmbachtXp.VERZAMEL`, `Imkeren.RAAT_MINUTEN`.

## Test status (2026-11-06 01:02)
- `compileJava` clean; `runGameTestServer -Pgt=VadsrijkAmbacht`: **11/11 pass**.
  - Tests: xpCurve, dataKlopt (all POI anchors + recipes + base types), xpEnUitgerust, scanTerrein (grass/stone/water/hive rules + deterministic), oogstPerSpeler (per player, moves, quality, tier gate, rare 3x), imkeren, kokenEnBrouwen (Vadsigheid, buff, crystal cutting), maatwerkEnMeesterwerk (~90 % of the set, top 20 % at 100 %, BiS ring, no set), bestelbord, ranglijstEnWaarde.
- NOT run: an AutoCheck client run.
  - The 3 gradle slots were busy for most of the night, so I skipped it.
  - The client side (spot rendering, minigame HUD, screens) compiles but is unseen.
  - First thing to check in the dev client: SPIEK_T_AMBACHT.md §1 and §3.
