# NOTITIES_Z2_SYSTEMEN: zone 2 "De Splinterpieken", slice systemen (branch 110-z2-systemen)

Package `feature/vadsrijk/zone2/systemen` (registry `Z2SystemenRegistry`, client `client/Z2SystemenClient`), generator
`tools/features/vadsrijk_z2_systemen.py` (one line in `FEATURES`). Ids `vr_z2_*`, lang `vadsrijk.z2.systemen.*`.
Dev commands (op 2): `/guhs vadsrijk z2systemen juwelier | winkel | kristal <soort> <0-2> [n] | sokjes <0-2> | bevries`.

## API for the other zone-2 slices
- **z2-npc**:
  - juweliersguh (POI `npc_juwelier`) -> `Juwelier.open(p)`;
  - Kandijkraam (POI `npc_winkel_kandij`) -> `Winkels.open(p, Z2Winkel.KANDIJKRAAM)` (id `z2_kandijkraam`);
  - kristalkapel altar (POI `kristalkapel_altaar`) -> `Advancement.kanDoen(p)` / `Advancement.doe(p)` (klassen; the 3rd step at lv 30;
    the ordetempel desk in Zwamzoet also works).
- **z2-wezens / z2-bazen**:
  - Bevroren = `kern.Status.BEVROREN`. Give it with `Bevroren.geef(e, ticks, bron)` (respects the chocomelk immunity) or plain
    `Statussen.geef(e, Status.BEVROREN, ticks, 1, bron)`.
  - Effects (all here, on hooks): players -30 % Loopsnelheid / -15 % Aanvalssnelheid, mobs -45 % movement speed, heavy hits +30 %.
    Heavy = skills and boss attacks (any skill id except `basis*`/`mob`/`status`/`val`/`omgeving`), plus the basic hits of hamer/kruisboog.
  - Raw crystals drop by themselves (`Haken.VERSLAGEN`): a defeated VadsrijkMob of lv 20+ inside the zone-2 box drops per helper:
    - normal: 8 % ruw;
    - elite: 35 % geslepen;
    - VadsrijkBaas: always stralend.
  - To drop one yourself: `Kristallen.stack(soort, kwaliteit, n)`.
  - Weapons loot table `z2systemen:wapens` (the 6 zone-2 class weapons).

## Kern / other-slice edits (all marked `z2-systemen` in the code)
- `kern/Levels.java`: `MAX` 20 -> 35. The curve formula simply continues; skill points stay 1 per level (lv 35 = 34, +15).
  Class stat growth is linear in every class, so it continues.
- `kern/Status.java`: `BEVROREN` added at the end (no ordinals are saved).
- `kern/loot/Uitrusting.java`: new last field `List<String> sokjes` (optional in the codec, default empty).
  - The old 10-argument constructor is kept, so no caller breaks.
  - `metSokjes(...)`; all `met*` keep the sockets.
- `kern/loot/Rollen.java`:
  - hook `SOKJES` (socket count per fresh roll, default 0) used in `rol`;
  - hook `SOKJE_BONUS` (gem stats) added in `stats`.
- `kern/loot/RijkItem.java`: hook `TOOLTIP_EXTRA` (lines after the bonuses).
- `klassen/Advancement.java`:
  - `MAX_STAP = 3`, `nodigLevel(2) = 30`, `levelVan(s)`;
  - `stap()` reads `<x>_adv1..3`;
  - hook `CEREMONIE` (extra ceremony effects per step).
- `klassen/boom/Boom.java`: `MIN_LEVEL` (node -> class level) checked in `waaromNiet`, with the new reason `"level"`.
- `klassen/client/BoomScherm.java`:
  - height and row distance follow the number of rows (6 rows: DY 32, H 308);
  - the cost label comes from the row's nodes, plus a "Nv 20/26" label;
  - 3 advancement badges (step 3 on a second line);
  - the level texts come from `Advancement`.
- `klassen/VadsrijkKlassenGameTests.java`: `skillboomPuntenRekenen` skips rows >= 4 and the `_adv3` nodes (the zone-1 counts 12/14
  stay checked).
- `economie/Winkels.java`: public `registreer(Winkel)` + `itemWaar(...)` (it had no API to add a shop).
- `VadsrijkFeature` / `VadsrijkClient`: one line each (after economie).

## What's in it
1. **Cap 35.** See kern above. Test `levelCap35`.
2. **Tree rows 4/5** (`Boomrijen`):
   - 6 nodes per class, added to the klassen tree lists at registration.
   - Row 4 opens at lv 20 (2 points per node) and needs any capstone (row 3).
   - Row 5 opens at lv 26 (3 points per node) and needs the row-4 node above it.
   - Total 15 points: 22 + 15 = 37 > 34, so you still choose.
   - Each class gets 2 stat nodes and 4 effect nodes:
     - Ridder: IJsklap, IJspantser, Onverzettelijk, Galmstoot, Kristalhart, IJzeren Knuffel.
     - Jager: IJspijl, Poolvacht, Jachtinstinct, Splinterregen, Sterrenoog, Windloper.
     - Magier: IJsbollen, Kristalgeest, Heldere Bron, IJsnova, Sterrenwijsheid, Manastroom.
   - All effects run on kern hooks (`VOOR_KLAP`/`NA_KLAP`/`NA_HEAL`/`STATUS`/`VERSLAGEN`/`COOLDOWN_MOD`).
3. **3rd job advancement at lv 30** (`Advancement3`): hidden nodes `<x>_adv3` (stats), a crystal ceremony on top of the klassen one,
   and a real unlock per class:
   - Knuffelridder -> **Kristalkampioen**, Kristalwal: shield up = attackers freeze 3 s; Kussenklap +40 % on frozen enemies.
   - Pluisjager -> **Poolstervalk**, IJsster: every 5th basic hit fires an ice star (1x, freeze 2 s); Sterrenschot freezes 4 s.
   - Vadsmagier -> **Kristalorakel**, Spiegelecho: Roze Vuurbal freezes 1.5 s; hitting an already frozen enemy gives a 2nd explosion
     (1.2x, r 3).
   - A chat message + tip at lv 30.
4. **Sockets + crystals + juwelier** (`Kristallen`, `Juwelier`, `client/JuwelierScherm`):
   - Rolls:
     - zone 1 (lv < 20): 1 socket only at Legendarisch+;
     - zone 2: 0-2 sockets, rarer = more (Guhddelijk always 2).
   - 6 crystals `vr_z2_kristal_<soort>`, with the quality in the component `guhs:vr_z2_kwaliteit` (0 ruw / 1 geslepen / 2 stralend,
     x1 / x1.7 / x2.6):
     - rozekwarts: Vads +35;
     - zonnesteen: Kritiek +2 %;
     - galmkristal: Aanvalssnelheid +3 %;
     - spiegelkwarts: Verdediging +4;
     - vonkkristal: Elementschade +4 %;
     - ijsglans: a 6 % chance to freeze on hit (all worn ijsglans add up, max 35 %).
   - Juwelier: insert is free, remove costs 20/60/150 brons by quality. A gem is never lost: with a full bag it goes to kern's found
     items via `EcoUtil.geefOfBewaar`.
   - The screen uses the economie `EcoScherm` style, with its own payloads `vr_z2_systemen_juwelier(_doe)`.
5. **Bevroren** (`Bevroren`): see the API above. The warme chocomelk thaws you and keeps you unfreezable for 3 min.
6. **Weapons** (`Z2Wapens`):
   - Ridder: kristalkling (zwaard), galmhamer (hamer).
   - Jager: ijspeesboog (boog), splinterkruisboog (kruisboog).
   - Magier: kristalstaf (staf), poollichtboek (boek).
   - Numbers are in f-gevecht's **DPS-rating** model (~5.9 + 1.18/lv, ~7 % above zone 1). The interval comes from the weapon kind in
     f-gevecht's `kern.Aanvalstempo.SOORT`, so there is nothing to register.
   - Before the merge with f-gevecht (the old per-hit model) these weapons hit too softly; that is expected.
   - TODO integration: add `Aanvalstempo.TYPE` entries if a weapon should have its own interval (e.g. galmhamer 3.0 s).
7. **Economy** (`Z2Winkel`):
   - Tier-2 potions:
     - Grote Vadsdrank: 70 %;
     - Grote Energiedrank: full;
     - Warme Chocomelk: thaw + 3 min warm + 15 % Vads.
   - Shop `z2_kandijkraam` "De Kandijkraam": the 3 new drinks + Vahoeg/Snelpootjes drankje + verbetersteentje.

## Open / for the integrator
- Merge conflicts to expect, with f-gevecht:
  - `kern/loot/Rollen.java` (they added `klapSchade`; I touched `rol`/`stats` and added hooks at the top);
  - `kern/loot/RijkItem.java` (they changed the damage line; my hook is after the bonuses);
  - `klassen/VadsrijkKlassenGameTests.java`.
- `Advancement` row-3 badge position assumes the 6-row trees (always true once this slice is registered).
- Mob commands in wezens still take `level 1..20` (`WezensCommando`), and klassen's dev `punten` too. They are not mine. The kern
  `level` dev command already goes to 35.
- Crystal drop chances and socket chances are first guesses; tune after a playtest.
- Generated assets are committed (item json/models/textures + lang). No map tiles were touched.

## Tests
`-Pgt=VadsrijkZ2Systemen`:
- `levelCap35`;
- `sokjesNooitKwijt` (insert/remove/fee/never lost/codec/roll rules);
- `advancementStap3`;
- `boomRijenOpenOpLevel`;
- `bevrorenZwareKlap`.

Plus `-Pgt=VadsrijkKlassen` (the adjusted tree test).

Results (2026-11-04):
- VadsrijkZ2Systemen 5/5;
- VadsrijkKlassen 4/4;
- VadsrijkKern 7/7;
- VadsrijkEconomie 5/5.

No AutoCheck was run (the juwelier screen and the 6-row tree screen were not seen in a client yet).
