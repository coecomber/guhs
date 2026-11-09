# NOTITIES X-LABELS: no raw translation key may show

Branch `110-x-labels` from b9ef85f4c. Run-time ids → keys → text in nl_nl and en_us.

## The bug and why the checks missed it
- The user saw a guh called `vadsrijk.npc.naam.kk_poort_zwamzoet`. That is the Kwartiermeester of Zwamzoet.
  - `huizen/Poorten` adds a Kwartiermeester per hub town (`Poorten.HUBS`: zwamzoet, kandijkom, honingtak, lampionhaven, hoogvads) at run time, only when a player comes near.
  - Only zone 6's two (`kk_poort_rozenkloof`, `kk_poort_saffraanpoort`, made by u-z6verhaal) had a name.
- `taalcheck.py` only saw literal keys. `VadsrijkTaalGameTests` only read `Npcs.alle()` at start, and these NPCs are not in it yet at that point.
- The same blind spot existed for every NPC added late: the Bondenbord guhs, the ambacht board guhs, Mollie and the Werver. Those all had names; only the Kwartiermeesters did not.

## What I built
1. **`kern/VadsrijkAlleLabelsGameTests`** (batch `vadsrijk_alle_labels`, 7 tests). It enumerates the ids that really occur at run time and resolves every key built from them, in nl_nl AND en_us (read from the jar). It fails with a full list per family. Last run: about 4,100 keys, all present.
   - **`npcNamen` (363 NPCs):** `Npcs.alle()` plus every late source:
     - `Poorten.HUBS`, Mollie;
     - `BondNpcs` (klerk, poort, plein, `BORDEN` + vallei);
     - `AmbachtPlekken.WERKPLAATSEN`;
     - `Werver.ID`.
     For each NPC a real `RijkGuh` is created and dressed with `stelIn` (the code path the world uses). Its name tag is rendered with an nl-only and an en-only `Language`. It may not be blank, may not match `^[a-z0-9_]+(\.[a-z0-9_]+)+$` and may not contain "vadsrijk.". The test also asserts that all 5 hub Kwartiermeesters are enumerated.
   - **`npcRegels` (964 keys):** `praat.<id>.1..3` (or `..4` for the four-line NPCs) for every NPC whose click can reach the chatter line. Skipped: the Kwartiermeesters, `bond_*`, Mollie, the Werver and the ambacht guhs, because their own menu answers every click.
   - **`registers` (1,084 keys):** every entry of about 40 registries, enums and tables, read by reflection because many are package-private. Every public no-arg method that returns a String or Component holding a `vadsrijk.*` key is called.
     - The sources: Stat, Status, Element, Slot, Zeldzaamheid, Affix, BasisTypes, Klassen (+ skills, tree nodes), Spec, Huisdieren, RijdierSoort, PluimKleur, Winkels, HuisSoort, Ontwerp, HuizenData.Deur, Modus, Emote, Lijnen (+ ports), Steden, Rang, Ambacht, Recepten, Maatwerk.Eigenschap, Cosmetica (+ Soort, Kracht), Puzzels, BootVariant (+ As), BootZee placeholders, NepRol, Maatjes (+ Rol), BaasProfielen (attacks), DungeonType, Premies (pool + shop), Uiterlijk (+ As).
     - An empty source is itself a failure, so a renamed field cannot silently drop a family.
   - **`familiesPerId` (612 keys):** the families that code builds as `"prefix." + id`:
     - caves (7 keys each) and cave dungeons (4 each);
     - lookouts, world doors (titel, onder, dicht) and Wolkwal views (+ `.onder`);
     - guild hubs, travel map and room tips;
     - per class the choice-card role and titles; per skill the `kort` and `skilluitleg`; per node the `.uitleg`;
     - per spec the rol, kort and titles;
     - ambacht greetings (`.hallo`), plus `.uitleg` for the gathering masters only;
     - mode lines and buttons, town names, the Kwartiermeesters' "where" lines;
     - stable cards and plume colour sources, guild tasks (+ `.lang`) and castle levels 1..MAX (+ uitleg);
     - zone-1 and zone-2 secrets (knuffels, clues and treasures, with their questlog titles);
     - area leaders of zones 3, 5 and 6 (their name tags), z3 contract targets (both families), riders (names and lines);
     - puzzles (naam, aanwijzing, uitleg, soort, the whisper stone's vraag, torn pages 1..9, unlocks).
   - **`wezens` (269 keys):** every entity type `guhs:vr_*` is created. Its name (custom name or type name) is rendered in nl and en, and every `*Key()` String method of its class is called (boss titles, subtitles...). RijkGuh is covered by `npcNamen`.
   - **`gebiedenEnQuests` (844 keys):** every registered area, and every quest's title, text and objectives plus its giver's name.
   - **`terugval`:** the safety net (below) returns "Kwartiermeester" for an unknown `kk_poort_*`, and a readable non-key for nonsense.
   - A crash inside a test is reported with its own exception and line, not as "UnknownGameTestException".
2. **The safety net (code fallback):** `kern/Labels.terugval(key, lookup)` is used by the client language, `client/GuhsTaal.GuhsLanguage.getOrDefault`. For a MISSING `vadsrijk.*` key that has no explicit fallback, it returns, in order:
   - the generic sibling: drop `_suffix` parts of a segment, last segment first (`naam.kk_poort_zwamzoet` → `naam.kk_poort` "Kwartiermeester");
   - else line 1 of a numbered line;
   - else the id made readable ("Kk poort zwamzoet"). It never returns the key.
   - It is logged once per key: `Guhs: missing text <key>`. `Language.has()` is unchanged, so code that tests for a key (`I18n.exists`) behaves as before.
3. **`tools/vadsrijk/taalcheck.py` DYNAMISCH:** a table of (file, id regex, key templates) that reads the ids from the tables in the code and asks for every key built from them:
   - the Poorten hubs (naam + plek.npc), BondPaneel hubs (bond_bord naam + gilden.hub), ambacht workshops;
   - every `npc("…")` in `*Npcs.java`, the KAMPWACHT knights;
   - GrotData and KerkerData, Uitkijk/Deur in WereldPlekken, Wolkwal views, Steden;
   - z1 and z2 secrets, and the z3/z5/z6 LEIDERS.
   - A literal followed by `+` (e.g. `"z5_burger_" + i`) is skipped, because the runtime test covers those.
   - Result: 2,941 keys checked, 0 missing. Before my fixes it reported the 10 Kwartiermeester keys.

## Every key fixed (16 raw keys a player could meet + 4 for consistency)
| Key | nl_nl | en_us |
|---|---|---|
| vadsrijk.npc.naam.kk_poort_zwamzoet | Kwartiermeester Lamelletje | Quartermaster Gilly |
| vadsrijk.npc.naam.kk_poort_kandijkom | Kwartiermeester Klontje | Quartermaster Cubey |
| vadsrijk.npc.naam.kk_poort_honingtak | Kwartiermeester Wasje | Quartermaster Waxy |
| vadsrijk.npc.naam.kk_poort_lampionhaven | Kwartiermeester Lontje | Quartermaster Wicky |
| vadsrijk.npc.naam.kk_poort_hoogvads | Kwartiermeester Torentje | Quartermaster Turret |
| vadsrijk.npc.plek.npc.kk_poort_zwamzoet | op het plein van Zwamzoet, een paar stappen van het midden | on the square of Shroomsbury, a few steps from the middle |
| vadsrijk.npc.plek.npc.kk_poort_kandijkom | op het plein van Kandijkom, een paar stappen van het midden | on the square of Sugarbowl, a few steps from the middle |
| vadsrijk.npc.plek.npc.kk_poort_honingtak | op het plein van Honingtak, een paar stappen van het midden | on the square of Honeybough, a few steps from the middle |
| vadsrijk.npc.plek.npc.kk_poort_lampionhaven | op het havenplein van Lampionhaven, een paar stappen van het midden | on the harbor square of Lanternhaven, a few steps from the middle |
| vadsrijk.npc.plek.npc.kk_poort_hoogvads | bij de fontein op de markt van Hoogvads | by the fountain on the market of Highchonk |
| vadsrijk.loot.kussenzwaard | Kussenzwaard | Pillow Sword |
| vadsrijk.loot.knuffelknots | Knuffelknots | Snuggle Club |
| vadsrijk.loot.pluishamer | Pluishamer | Fluff Hammer |
| vadsrijk.loot.pluishelm | Pluishelm | Fluff Helmet |
| vadsrijk.loot.knuffelharnas | Knuffelharnas | Snuggle Harness |
| vadsrijk.loot.roze_ring | Roze Ring | Pink Ring |
| vadsrijk.loot.knuffelketting | Knuffelketting | Snuggle Necklace |
| vadsrijk.loot.pluisoorbel | Pluisoorbel | Fluff Earring |
| vadsrijk.z6raid.ster_bultguh.onder | Een gedoofd sterrenbeeld dat over een rode baan stormt | A snuffed constellation that charges down a red lane |
| vadsrijk.z6raid.ster_pluis.onder | Een gedoofd sterrenbeeldje dat de anderen heelt met een lied | A little snuffed constellation that heals the others with a song |

Notes on these keys:
- The loot names are the base names of kern's own 8 weapon, armour and jewellery bases. A crafted (Maatwerk) piece is named `bt.naamKey()`, and the set tooltip (`UitrustingTips`) uses them too, so those showed `vadsrijk.loot.kussenzwaard`. The other slices' bases already had names.
- The `plek.npc.kk_poort_*` "where" lines are only shown when a quest step points at that NPC (none does today). I added them so the family is complete, like zone 6's.
- The z6raid subtitles belong to the two side constellations (`titelKey() + ".onder"`, VadsrijkBaas.intro). In the raid only the Grote Knuffel is the arena boss, so they are probably never shown. Added anyway, because the bosses expose the key.

## Decisions
- The Kwartiermeesters keep the zone-6 naming style: "Kwartiermeester <cute town word>". Lamelletje = mushroom gills, Klontje = sugar lump, Wasje = beeswax, Lontje = lantern wick, Torentje = little tower. I checked that none of these names were already used.
- Not required, on purpose (they never reach the screen):
  - `praat.kk_poort_*`: `Menus.klik` handles every Kwartiermeester click;
  - the ambacht `.uitleg` for crafting masters: only gathering masters get the "where" option;
  - `klassen.kort.<class>`: the key is per SKILL; the test checks the skills.
- English goes through the overlay only: `tools/lang/en/c440_vr_x_labels.json` (12 keys, registered per key in chunks.json), plus the 8 loot names in `c409_vr_loot.json`. They are in c409 because c440's per-key prefixes would otherwise "claim" the loot flavour keys in check_en.
- `check_en`: c440 and c409 have 0 errors and 0 todo.
- I did NOT run `engelscheck.py --merk`, because `engels_bron.json` is shared with x-engels. `engelscheck.py` is OK (13,610 keys, all categories 0). Integration can run `--merk` once after merging.

## Generators (how the Dutch got in)
- Each key's Dutch is in its owning generator:
  - `tools/features/vadsrijk_u_huizen.py` TEKSTEN (the Kwartiermeesters);
  - `vadsrijk_kern.py` TEKSTEN (the loot names);
  - `vadsrijk_u_z6raid.py` TEKSTEN (the subtitles).
- **Warning for integration:** running `vadsrijk_kern.py` standalone is NOT safe. It is stale: it reverts later Dutch edits of `vadsrijk.kern.stat.*`, `kern.party.uitnodiging` and `kern.sieraad.*`, and rewrites 4 textures.
  - So I added only the new keys to nl_nl.json, with a tiny add-missing-keys script (in my scratchpad, not committed). u_huizen standalone was safe (only additions).
  - en_us.json was rebuilt with `apply_en.py --no-ftb`.

## Shared-file changes
- `src/main/java/nl/juiced/guhs/client/GuhsTaal.java`: the safety net in `GuhsLanguage.getOrDefault`, plus a small cache map, marked `X-LABELS`.
- `tools/vadsrijk/taalcheck.py`: the DYNAMISCH table, plus `VadsrijkAlleLabelsGameTests.java` in SKIP_FILES (its example keys).
- Generators: `tools/features/vadsrijk_u_huizen.py`, `vadsrijk_kern.py`, `vadsrijk_u_z6raid.py` (new TEKSTEN lines only, marked X-LABELS).
- Lang:
  - `nl_nl.json`: +20 keys;
  - `en_us.json`: rebuilt by apply_en;
  - `tools/lang/chunks.json`: the c440 chunk;
  - `tools/lang/en/c409_vr_loot.json`: +8 keys;
  - `tools/lang/en/c440_vr_x_labels.json`: new.
- New files: `kern/Labels.java`, `kern/VadsrijkAlleLabelsGameTests.java`.

## Tests run
- `runGameTestServer -Pgt=VadsrijkAlleLabelsGameTests,VadsrijkTaalGameTests`: **10/10 passed.**
- `taalcheck.py`: 2,941 keys, 0 missing. `engelscheck.py`: OK. `check_en.py`: c409/c440 have 0 errors.

## Open points
- Families the test does NOT enumerate:
  - **Random-line pools** (`stadsleven.<pool>.<n>`, `party.nep.zeg.<soort>.<n>`, `*.mika.weg.<n>`, `bazen.mika.praat.<n>`, karavaan lines): the count lives in code next to the random call.
  - **Client-only constant tables** (menu tabs, kleding tabs, smid tabs, the toetsen groups, questlog tabs), the boss phase cards (`*.fase<n>`), `vfx.aanval.ring_*`, and the `z2npc.vrij.<id>` / `scene.vlucht.<naar>` story keys.
  - The safety net covers all of these at run time (sibling, line 1 or a readable id), and the static taalcheck covers their literal parts.
- `Language.has()` still says false for a missing key, so code that does `I18n.exists(k) ? translatable(k) : literal(id)` keeps its own fallback.
- Not checked in a client (no client runs this round). The safety net is a few lines in GuhsLanguage, exercised by the `terugval` gametest through `Labels`, not through GuhsLanguage itself.
