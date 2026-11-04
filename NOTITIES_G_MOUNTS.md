# NOTITIES_G_MOUNTS (Vadsrijk fix round 2, agent G-MOUNTS, branch 110-g-mounts)

The mount system the user asked for, built on the economie slice's mounts (package `feature/vadsrijk/economie`).
Generator: `tools/features/vadsrijk_rijdieren.py` (new, appended to `tools/features/__init__.py` FEATURES after `vadsrijk_z2_bazen`).

## What is built
- **Keys** (Vadsrijk category, rebindable): `key.guhs.vr_rijdier` default **X** = mount/dismount, `key.guhs.vr_rijdieren` default **N**
  = the mount screen. In the dimension they win over Guhs keys on the same key (the snuffel boekje is also N), same trick as
  KernClient's skill keys. Client: `economie/client/RijdierClient.java`; payload `vr_rijdier_toets` (C->S).
- **HUD icon** right of the skill slots + Knuffelsprong (x = centre + 212, bottom row): the active mount's icon (saddle / Pluimguh egg
  in its colour), the key, and the state: ready (pink rim), riding (green, "rijdt"), cooldown (dark sweep + seconds), scared by a
  hit (red, "!" + "schrik!", 5 s), in a boss arena (grey with a cross). Payload `vr_rijdier_hud` (S->C, only on change, checked
  every 5 ticks).
- **Mount screen** `RijdierScherm` (replaces StalScherm; the EcoPayloads STAL screen with context "stal" = at the stalmeester):
  all 5 mounts in a list (yours in colour, others greyed + "?"), a turning preview (dark silhouette when not yours), the Pluimguh's
  colour swatches (gold hidden until owned), level / xp bar / speed + jump bars, the mount's trait, where to get it, "Hierop rijden",
  and at the stal "Kopen (prijs)" + a spare rijfluitje.
- **Ownership** in `SpelerData.extra("economie")` (same place as before, so existing Hoedhuppers stay): `rijdieren.<soort> =
  {level, xp, kleuren "roze,mint", kleur}`, `actief`, `starter`.
- **Starter (zone 3 team):** `Rijdieren.kiesStarter(p)` opens `StarterScherm` (3 Pluimguhs roze/mint/lavendel, turning previews,
  "Deze wil ik!"); returns false when already chosen. Hook: `Rijdieren.STARTER_GEKOZEN` (BiConsumer<ServerPlayer, PluimKleur>) fires
  once on the pick; `Rijdieren.heeftStarter(p)`. The other two starter colours: stal, 8z each (only after you have a Pluimguh).
- **Mounts** (`RijdierSoort`): PLUIMGUH (main, colours `PluimKleur`: roze, mint, lavendel, honing (stal 20z), wolkwit (quest, API
  `Rijdieren.geefKleur`), sterrennacht (boss loot: item `guhs:vr_item_pluimei_sterrennacht`), goud (hidden, later boss)),
  HOEDHUPPER (quest `geefEerste`, unchanged API), KLODDERROS (Koning Klodder saddle, unchanged), WOLKENSCHAAPJE (stal 12z; hovers:
  slow fall + double jump), SLAKKIE (stal 25z; slow luxury, seat on the shell). Items for rewards: `vr_item_pluimei_<kleur>` (7),
  `vr_item_zadel_wolkenschaapje`, `vr_item_zadel_slakkie`.
- **Behaviour:** pop-in (grows 0.2 -> 1.12 -> 1 in 9 ticks) + cloud/cherry/heart poof + chime; rider seat per mount
  (`RijdierSoort.zit`); animations idle / hop (walk) / ren (run, horizontal speed > 0.2 b/t) / sprong for every mount ("ren" added to
  the Hoedhupper/Klodderros animation files by my generator). Dismount: hit (+5 s "geschrokken" cooldown), boss arena (`Arenas.op`,
  checked every 5 ticks; summoning in an arena is refused), fainting (`Haken.FLAUW`), getting off, leaving the Vadsrijk, logout,
  restart. Towns: keep riding. Works in every zone (the whole dimension). 1 s anti-spam cooldown on the key.
- **Rijfluitje** optional: same as the key; `geefEerste` no longer hands one out; still for sale at the stal.
- **Dev:** `/guhs vadsrijk mount geef <soort> [kleur] | alle | reset | starter | scherm | stal | rijd <soort> [kleur]`.

## Pluimguh model (placeholder, swappable)
The model + 7 textures come from the concept renders (`renders-mounts/_bron/modellen/vr_pluimguh*`, made by the render agent);
`kopieer_pluimguh` copies them into `geckolib/models/entity/vr_eco_pluimguh.geo.json` + `textures/entity/vr_eco_pluimguh_<kleur>.png`
when that folder is found (or `GUHS_MOUNT_BRON`), otherwise the committed copy stays. Animations are mine (bone names hoofd, nek,
kuif, oor_l/r, leg_l/r, scheen_l/r, vleugel_l/r, staart, body). Seat height from the render table ZADEL (33.7 px -> zit 2.10).
To swap to the Knuffelhert/Guhwolf: copy its geo/textures in, write its animations (same 4 names), adjust `zit` and the entity size
in `EcoSpul` (PLUIMGUH 1.4 x 2.6).

## Knuffelhert + Guhwolf (user decision, coordinator message)
- Both are collectible mounts now: `RijdierSoort.KNUFFELHERT` (seat 1.95, 1.3 x 2.4) and `GUHWOLF` (seat 1.86, 1.3 x 2.0), render
  models copied from `renders-mounts/_bron/modellen/vr_knuffelhert|vr_guhwolf` (single texture each), my quadruped animations
  (`anims_viervoeter`). Saddle items `guhs:vr_item_zadel_knuffelhert` / `guhs:vr_item_zadel_guhwolf` (right-click = unlocked).
  Mount screen: silhouette + "Quest: Het Knuffelhert (Sporenwoud)..." / "Quest: De verdwaalde Guhwolf (Kandijkom)...".
- API: `Rijdieren.geef(p, "knuffelhert")`, `Rijdieren.geef(p, "guhwolf")` (string ids; also "pluimguh:<kleur>");
  `Rijdieren.heeftEen(p)` = owns any mount (the gate for the two return quests).

### The two return quests (NOT built: npc-slice files + a Questlog gate are outside my brief) - for the quest makers / integrator
Gate: appear only once `Rijdieren.heeftEen(p)` (needs one line in `npc/Questlog.beschikbaar`, e.g. a `na` entry "rijdier" that
checks `nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren.heeftEen(p)`). Reward: the saddle item (simplest: `items=[("guhs:vr_item_zadel_guhwolf", 1)]`;
the player right-clicks it) or `actie_klaar` calling `Rijdieren.geef(p, "guhwolf")`.
1. zone 2, Kandijkom, "De verdwaalde Guhwolf" (zij, lv ~22, gever e.g. z2_berggids):
   - aanbod: "Hoor je dat? Er huilt iets in de Splinterkloof. Een sneeuwwolfje, zo klein als een kussentje, helemaal alleen!"
   - doelen: gebied Splinterkloof -> praat/zoek het wolfje (geheim teller) -> breng 3 `wolpluk` (warm nestje) -> terug.
   - klaar: "Kijk eens hoe groot hij is geworden! Hij wil met jou mee. Hier, zijn sjaaltje-zadel." -> zadel guhwolf.
2. zone 1, Sporenwoud, "Het Knuffelhert" (zij, lv ~10, gever: a Sporenwoud/Zwamzoet guh):
   - aanbod: "Als de lantaarns in het Sporenwoud branden, komt er soms een heel zacht hert kijken..."
   - doelen: steek 4 lantaarns aan (geheim teller "lantaarn", blocks to click) -> volg het hert (gebied: hidden glade) -> praat.
   - klaar: "Het hert snuffelt aan je hand. Het wil met je mee!" -> zadel knuffelhert.

## For other slices
- zone 3 (quest): `Rijdieren.kiesStarter(p)`, `Rijdieren.STARTER_GEKOZEN`, `Rijdieren.geefKleur(p, PluimKleur.WOLKWIT)` as a reward;
  boss loot item `guhs:vr_item_pluimei_sterrennacht` (later `_goud`).
- npc: `Stal.open(p)` (now with the shop), `Rijdieren.geefEerste(p)` unchanged.

## Open choices for the user
- Prices (Pluimguh colour 8z, honing 20z, Wolkenschaapje 12z, Slakkie 25z) and speeds (Pluimguh 0.32, Slakkie 0.17) are guesses.
- Final main mount (Pluimguh vs Knuffelhert/Guhwolf) still pending.
- Riding in towns is allowed (the brief said optional); a town dismount would be one line in `Rijdieren.afReden`.

## Resume point / tests
See the end of this file.

## Chocobo joke (user request)
- The starter screen shows `vadsrijk.economie.starter.grapje` under the question; the stal screen shows `vadsrijk.economie.stal.grapje`
  (the stalmeester) under the Pluimguh's swatches. For the zone-3 npc slice: put the same line in the quest-giver's dialogue when
  `Rijdieren.kiesStarter` is called (e.g. stalmeester: "Nee, het is geen choco-dinges uit een of ander Fantasy-spel. Dit is een
  Pluimguh. Kijk maar naar de oortjes, njeg!").

## Test results (2026-11-04)
- `runGameTestServer -Pgt=VadsrijkRijdieren,VadsrijkEconomie`: all 8 passed (ownership/selection/stal purchase, starter choice
  once + hook, dismount on hit/faint/arena, keep riding in town; economie rijdierXp updated: no rijfluitje from geefEerste).
- AutoCheck `tools/autocheck/vadsrijk_g_mounts.txt` (once, screenshots in run/screenshots/autocheck/m_*.png): the mount screen with
  silhouettes, the stal, the starter choice (3 turning Pluimguhs) and riding every mount render; HUD icon shows "rijdt". modelcheck:
  only the known foreign problems (badgedoos, onweerswolkje, droombed). Fixed after the run (compiled, not re-run): the starter
  texts were wider than the panel (now wrapped), the xp line overflowed the info column (now "x / y xp"), the screen-click
  coordinate in the script. The arena shot ended up just outside the Koning Klodder arena (the gametest covers the rule).
- Not judged by me (user judges looks in the dev client): mount sizes/seat heights (Slakkie's rider sits a bit high), animations.

## Resume point
All done and committed on 110-g-mounts. Open: the two return quests + the "owns a mount" gate (npc slice), the zone-3 starter quest
calling `Rijdieren.kiesStarter`, the Klodderros/Pluimguh eggs in boss loot tables (bazen), prices/speeds tuning.
