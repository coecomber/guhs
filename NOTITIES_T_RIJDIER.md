# NOTITIES_T_RIJDIER (night round T, agent T-RIJDIER, branch 110-t-rijdier, from dbf82899d)

Roadmap §4.1 (more land mount variants) + §8 with decision R2 (pets: support/sniffing only, NO damage).
New package **`feature/vadsrijk/rijdier/`** (+ `client/`). Generator **`tools/features/vadsrijk_t_rijdier.py`** (one line in FEATURES,
after everything; standalone: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_rijdier.py`).

## For t-z4boot (and anyone adding a mount species): the species API
Adding a mount species is now ONE enum line + assets + texts (no more switch statements):
1. `economie/RijdierSoort.java`: add a value, e.g.
   `ZEILBOOTJE(0.30, 0.0, 0, 1.0, "quest", 2.0f, 1.2f, Eigenschap.X)`
   = speed (0.1 = walking), jump velocity, stal price in brons (0 = not for sale), seat height (blocks), source word, entity width,
   entity height, traits. The old 5-number form still compiles (size 1.3 x 2.0, no traits).
   Traits live in `RijdierSoort.Eigenschap` (`KLEUREN` = Pluimguh colour enum, `ZWEEFT` = slow fall + double jump, `STEVIG` = shrugs off
   the first hit). **A water mount: add your own value there (e.g. `VAART`) and branch on `soort.heeft(Eigenschap.VAART)`** in
   `RijdierEntity.travel` (that is the movement code; it is shared by every mount).
2. `EcoSpul` registers the entity `guhs:vr_eco_<id>` (size from the enum) and the saddle item `guhs:vr_item_zadel_<id>` in a loop
   (`EcoSpul.RIJDIEREN`, `EcoSpul.ZADELS`); the renderer (`EconomieClient.RijdierRenderer`) and attributes are per `values()` already.
   Assets: `geckolib/models/entity/vr_eco_<id>.geo.json`, `geckolib/animations/entity/vr_eco_<id>.animation.json` (idle, hop, ren,
   sprong), `textures/entity/vr_eco_<id>.png`, saddle icon + `items/vr_item_zadel_<id>.json` + model, lang
   `vadsrijk.economie.rijdier.<id>`, `...rijdier.eigen.<id>` (trait line), `...stal.nog_niet.<id>` (where to get it), `entity.guhs.vr_eco_<id>`.
3. Looks (optional): a species without patterns/tack (a boat): call `Uiterlijk.zonderGarderobe(RijdierSoort.X)` once (common code).
   Its own coats: `Uiterlijk.registreer(new Variant(As.VACHT, "id", RijdierSoort.X, 0xFFrrggbb, Bron.START/…))` + textures
   `vr_eco_<id>_<vacht>.png`. Without any variants the N screen simply shows no "Aankleden" tab.
4. Where it comes from: `Aankleden.SOORT_BRON.put(RijdierSoort.X, Bron.veren(20) / Bron.baas(type) / Bron.geheim(id) / Bron.prestatie(id))`
   (automatic sources are checked every 10 s), or just call `Rijdieren.geef(p, "id")` from a quest.
5. The gametest `VadsrijkRijdierenGameTests.rooster` checks the first 8 ids only (`subList(0, 8)`), so appending a 9th is fine.
   The N screen fits 9+ rows (rows of 20 px, screen 380x256).

## What is built
### Mounts: three new land species (common code in economie, assets in my generator)
- **Duinhupper** (springy beach hare): speed 290 %, the HIGHEST jump (0.98 = ~5.8 blocks), seat 1.15. Source: **secret**, find 10
  hidden knuffels (all zones together). Its walk/run is a hopping gait.
- **Wolkwalkalfje** (cloud-whale calf on its own little cloud): speed 340 %, `ZWEEFT` (slow fall + double jump, like the sheep but
  faster). Source: **20 Gouden Veren** (button in the N screen).
- **Zwamzwijntje** (piglet with a red mushroom-cap saddle): speed 330 %, `STEVIG`: the first hit doesn't scare it (the 2nd within
  10 s does; chat line). Source: **first kill of the Opperzwam** (zone 1 temple boss).
- Knuffelhert's trait text changed: "springt heel hoog" (the Duinhupper now jumps highest).
- Two new **Pluimguh colours**: **Kristal** (first kill of Spiegelzucht, zone 2) and **Lampion** (12 Gouden Veren; a zone-4 quest can
  also give it: `Rijdieren.geefKleur(p, PluimKleur.LAMPION)`). Textures recoloured from the pink one; eggs `vr_item_pluimei_kristal|lampion`.

### Mount looks (`rijdier/Uiterlijk`, `rijdier/Aankleden`, `rijdier/client/UiterlijkLaag`, `rijdier/client/AankleedPaneel`)
Looks never change stats (the Gouden Pluimguh's +5 % stays the only exception, untouched). Three axes:
- **Vacht** (coat) per species: 3-5 per mount (`Uiterlijk` static block). Textures: the Wolkenschaapje + the new mounts are
  re-painted from their models; Knuffelhert/Guhwolf/Slakkie(shell) are recoloured pixel-wise on their fur pixels.
- **Patroon** (all land mounts, account-wide): vlekjes, streepjes (stal 6 zilver each), hartjes (5 veren), sterretjes (4 pets),
  glitter (first kill of Vahoegnir; drawn full-bright). One extra model pass with `vr_rijpatroon_<soort>_<patroon>.png` (fur pixels
  only), tinted from the coat colour (vlekjes darker, streepjes lighter).
- **Tuig** (tack, all land mounts, account-wide): zadeldekje (start), bloemenkrans (stal 4z), belletjes (stal 5z), hoge hoed (secret:
  8 treasures), lampionnetjes (6 veren; full-bright = they glow at night), kroontje (achievement: any mount lv 10). Drawn as 3D item
  models (`vr_rijtuig_<id>`) on anchor bones (head / neck / back) per species: `UiterlijkLaag.ANKERS`. The items are also reward
  tokens: right-click = unlocked.
- Coat sources per species: stal (coins, only at the stal), Gouden Veren (anywhere in N), achievements (mount lv 10, pets, Verkenningsboek
  75 %), bosses (Spiegelzucht, Kruinroofvogel), secrets (knuffels / treasures / clues counted over zone 1-3 npc maps).
- Storage: `SpelerData.extra("economie")`: `garderobe` (csv of keys `vacht:<soort>:<id>`, `patroon:<id>`, `tuig:<id>`), per mount
  `rijdieren.<soort>.uiterlijk` = "vacht|patroon|tuig". Synced on the entity (`RijdierEntity.DATA_UITERLIJK`).
- Automatic sources (`Aankleden.inhalen`): every 10 s in the Vadsrijk + when N opens. Only after the first mount (zone-3 starter).
- **N screen** (RijdierScherm): now 380x256, rows of 20 px, preview 130 wide, Pluimguh swatches 12 px (9 colours). Info column has an
  **Info | Aankleden** switch: three swatch rows (Vacht / Patroon / Tuig, with "×" = take off), clicking tries it on in the turning
  preview, tooltip = name + "Van jou!" or where to get it (+ progress), button Aantrekken / Kopen (stal only) / Ruilen (n veren).
  Not-owned new mounts show their source, progress and (veren) a Ruilen button; same for the Lampion Pluimguh. Silhouettes as before.

### Pets (`rijdier/Maatjes`, `rijdier/Snuffelen`, `rijdier/Vahoegje`)
Built on G-PETS' `HuisdierEffect` (tick every second + a passive via `Spelers.EXTRA_STATS`). Pets never deal damage and stay invulnerable.
| pet | role | ability | passive |
|---|---|---|---|
| Mini Klodderreus | Steuner | Klodderschild: in combat below 50 % Vads -> KNUFFELSCHILD 8 % max Vads, 8 s (cd 30 s) | Elementweerstand +2 % |
| Mini Koning Klodder | Steuner | Koninklijk kussentje: out of combat +2 % max Vads per second | Genezen (Healkracht) +3 % |
| Mini Galmwolf | Steuner | Galmlied: in combat +8 % max energy (cd 10 s) | Loopsnelheid +2 % |
| Piepje | Snuffelaar | Kruimeltjes rapen: picks up YOUR loot within 6-9 blocks (vanilla pickup rules, personal drops stay personal) | Loopsnelheid +2 % |
| Kruinroofvogel | Snuffelaar | Scherp oog: every 5 s out of combat a golden trail to the nearest unfound secret within 24-34 blocks (only you see it), "ruikt iets!" once per spot | Loopsnelheid +2 % |
| Vahoegje | Steuner (special) | Drakenwarmte: out of combat +1 % Vads/s; below 35 % in combat a pink flame heals 15 % (cd 75 s). Grown (after zone 5): +1.5 %/s, heals 22 % + shield 8 % (cd 60 s) | Genezen +2 % (grown: Genezen +4 %, Loopsnelheid +2 %) |
| any other pet | Snuffelaar | Snuffeltje: picks up loot within 4 blocks | none |
- Snuffelaars also give a 5 % bonus-coin chance on a kill (mob level x 3 brons).
- **Levels**: 1-10, xp = kills while the pet walks along (boss = 10), `12 x level` per step (~540 kills to max). +5 % ability strength per level.
- "In combat" = hit or got hit in the last 5 s (`Schade.NA_KLAP`).
- **Snuffelen** spots: every Verkenningsboek entry that is `geheim` with a place and not done (all zones that registered a page), plus
  `Snuffelen.BRONNEN` (other slices, e.g. t-ambacht gathering spots / t-puzzels pieces: `(player, radius) -> List<BlockPos>`).
- **Vahoegje grows**: stage `KLEIN` / `MIDDEL`; MIDDEL when the quest flag **`zone5_klaar`** is set (**t-z5verhaal: please set
  `Questlog.zetVlag(p, "zone5_klaar", true)` at the end of zone 5**), or any `Vahoegje.GROEI` predicate, or the dev override. Model
  1.55x bigger (synced float on `HuisdierEntity`).
- **P screen** (HuisdierScherm, 360x248): name + "Level x / 10" + xp bar, a role badge (pink Steuner / yellow Snuffelaar), the ability
  name + text, "Altijd: <passive>", Vahoegje's stage, then flavour or "Zo krijg je hem". A green line under the grid: pets never fight.
  The "<pet> van <speler>" nametag is untouched.

## Decisions I made myself
- Variants and sources (which coat from where, prices: coats 12-20 zilver, patterns 6z, tack 4-5z; feathers 5-20) - all in
  `Uiterlijk` / `Aankleden` static blocks, easy to tune.
- Species sources: Duinhupper = secret (10 knuffels) instead of a zone-4 quest (zone 4 isn't there yet; a quest can still call
  `Rijdieren.geef(p,"duinhupper")`); Wolkwalkalfje = 20 Gouden Veren; Zwamzwijntje (new, my idea) = Opperzwam first kill.
- Feathers can be spent before the gold Pluimguh (player's choice); the trade works anywhere in the N screen (not only at Veertje).
- Patterns/tack are account-wide wardrobe (one unlock for every mount); coats belong to one species. Fits R1 (cosmetics account-wide).
- Pets: light level model (1-10, +5 %/level), passives only defensive/utility stats (no damage stats), the Snuffelaar bonus = coins.
- No "Meenemen" change and no HUD pet portrait (roadmap §8 mentioned one; skipped to keep RijdHud untouched).

## Shared-file changes (all marked `T-RIJDIER`)
- `VadsrijkFeature`: one register line.
- `economie/RijdierSoort`: 3 values, size fields, `Eigenschap` enum, `stevig()`, 2 PluimKleur values (KRISTAL, LAMPION appended).
- `economie/EcoSpul`: entity types + saddles registered in a loop (`RIJDIEREN`, `ZADELS`; old fields kept as aliases), `holder`/`icoon` via maps.
- `economie/RijdierEntity`: synced look string + `vangSchrikOp` (sturdy). `economie/Rijdieren`: look on summon, sturdy in `geraakt`.
- `economie/Stal`: `inhalen` on N, look data per mount, Gouden Veren in the data, unknown actions -> `Aankleden.doe`.
- `economie/client/EconomieClient`: coat texture + `UiterlijkLaag`. `economie/client/RijdierScherm`: size, rows, the Aankleden switch,
  progress + Ruilen for mounts/colours, public `paneel*` wrappers.
- `economie/VadsrijkRijdierenGameTests.rooster`: checks the first 8 ids.
- `huisdieren/HuisdierEntity` (+ synced size), `huisdieren/client/HuisdierClient` (size in render), `huisdieren/Huisdieren`
  (`Maatjes.schermData` in the screen data), `huisdieren/HuisdierEffect` (javadoc), `huisdieren/client/HuisdierScherm` (layout).
- `zone3/systemen/GoudenVeren`: `neem(p, n)` (spend feathers).
- `tools/features/__init__.py`: `vadsrijk_t_rijdier` appended. lang nl_nl/en_us via the generator.

## Tests / resume point
See the end of this file.
