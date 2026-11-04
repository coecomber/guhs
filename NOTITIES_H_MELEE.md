# NOTITIES_H_MELEE: round 3 (2026-11-04), branch 110-h-melee (from 10bdbc540 = 110-g-int)

User feedback (playtest round 3, early tutorial): melee is much harder than ranged/magic. Four fixes.

## 1. The starter sword does double damage (`kern.loot.Rollen`)

- `Rollen.STARTZWAARD_FACTOR = 2.0`, `Rollen.isStartzwaard(u)` = a **bound, Gewoon, level-1 kussenzwaard**, which is exactly
  what `klassen.KlasseBalie.startwapen` hands out in Knuffelkamp. It's applied in `Rollen.wapenSchade`, so the basic hit,
  the skills (`Spelers.wapenSchade`) and the tooltip ("N schade per klap") all show it. Loot/shop kussenzwaarden (not bound) are unchanged.
- Numbers (damage model of NOTITIES_FIX_GEVECHT: rating = DPS, hit = rating x class factor x interval, then x (1 + 2 % Kracht)):

| | rating lv 1 | interval | per hit (before Kracht) | Proefklodder lv 1 (40 Vads) | lv 4 (82 Vads) |
|---|---|---|---|---|---|
| kussenzwaard before | 5.5 + 1.1 = 6.6 | 1.0 s | 6.6 | 5 hits (~5 s) | 10 hits |
| **starter now** | 13.2 | 1.0 s | **13.2** | **2-3 hits** (gametest: 2) | 5-6 hits |
| pluisboog starter (for comparison) | 6.6 x 0.68 | 1.1 s | 4.9 | | |
| rozenstaf starter (staf, splash) | 6.6 x 0.79 | 1.3 s | 6.8 | | |

  The gametest logs the real values (`[h-melee] starter sword: ...` in the gametest log): see "Tests".
- Open choice: from ~lv 5-7 a normal Gewoon kussenzwaard drop rates below the starter (13.2 DPS = a Gewoon lv-7 sword). If that
  feels odd, let the factor fade (e.g. 2.0 at class level <= 3 down to 1.0 at 8) or end it at the first boss.

## 2. Melee room in the Klodderreus pond arena

- Before (measured on the g-int tiles): radius **18** (code, NpcPlekken) / 20 (POI) around a 15 x 10 pond: on the long axis only
  ~2-3 blocks of shore, half of it reeds/flowers, slime-block decoration on the shore. **45 % dry, 45 % water, 10 % plants.** And a
  fighter standing just past 18 blocks on the shore was OUTSIDE the arena: the boss ignores hits from outside (the gold "te ver" message)
  = one more "no damage" cause.
- Now: `NpcPlekken.KAMPVIJVER_STRAAL = 26` (npc package: the brief asked for "the arena radius in code"), POI `arena_klodderreus` straal 26,
  door/bord moved to 30/31 blocks west of the centre (outside the disc). Generator `tools/vadsrijk/kamp/kamp.py`:
  - `ARENA_STRAAL = 26`, `VIJVER_R = (15, 10)`; the flattening is now the whole arena square (+2) instead of +-18/+-13;
  - reeds only as a thin fringe on the north shore (e <= 1.12), no reeds elsewhere; more sand on the beach;
  - the 16 decorative slime blocks stand in a ring 28-31 blocks out (outside the arena), the 2x2x2 moederklodder at 29 east;
  - `arena_vrij(x, z)`: no flowers/short grass inside the arena (they catch the crosshair and hide Klodders).
  After: **79 % dry flat ground, 21 % water, 0 % plants/obstacles** (`tools/vadsrijk/audit/arenaruimte.py`).
- Other boss arenas (`python tools/vadsrijk/audit/arenaruimte.py`, all OK, no change needed): Koning Klodder r32 100 % dry flat,
  Opperzwam r22 98 %, Galmwolf r32 98 % (a few rocks), Spiegelzucht r22 100 %, Boormeester r20 96 %.
- Walkability audit after the regen: identical except random shifts (camp rng): one new "tree on path" at (78,87,991) = a stripped-log
  fence post of the training field on a randomly chosen dirt_path block (false positive, not near the arena).

## 3. Hold to attack with melee (`klassen.client.NabijClient`, new)

- Holding the attack button with a melee weapon (or the fist) in the dimension: every client tick, if the vanilla attack indicator
  is full (`getAttackStrengthScale(0) >= 1`, the attack speed attribute = the weapon interval, kern) and a Vadsrijk mob is the
  target, `gameMode.attack` + swing. It is the normal vanilla attack packet, so the server rhythm stays the judge.
- Also: **through plants**. Short grass, flowers and reeds have an outline, so the vanilla crosshair picked the plant in front of a
  small Klodder and the click "broke grass" (= nothing). When the crosshair is on a block (or nothing), a Vadsrijk mob on the
  same line within reach that is not behind a colliding block is hit instead (click and hold).
- `VadsrijkMob.getPickRadius()`: 0.3 for mobs < 1.2 wide (a Proefkloddertje is 0.52), else 0.1: easier to aim at small Klodders.

## 4. "Sometimes it does no damage": causes found and fixed (`kern.Schade.basisaanval`)

| candidate | verdict |
|---|---|
| vanilla attack-cooldown charge (>= 92 %) next to `VOLGENDE_KLAP` | **Main cause.** The server-side vanilla ticker is reset by EVERY click that reaches `Player.attack` (also early clicks, via Neo's `onAttack`), and the server/client tickers run a tick or two apart, so a click exactly on "indicator full" was often < 92 % on the server: swallowed whole. **Removed**: the server's own rhythm (`VOLGENDE_KLAP` = hit tick + interval) decides alone. |
| clicks before ready swallowed | **Fixed**: a click up to `Schade.BUFFER` = 6 ticks early is remembered (`Schade.WACHTEND`) and lands the moment the weapon is ready (`Schade.wachtendeKlap`, per tick from KernEvents; swing + PLAYER_ATTACK_STRONG sound). 1 tick of jitter (`SPELING`) counts as ready. Earlier clicks still do nothing (spam never beats the interval). |
| class switch in `Spelers.voorAanval` eats the first hit | Not a cause: `wisselKlasse` runs, `voorAanval` returns true and the hit lands (gametest asserts it). |
| reach/hitbox with slimes | Partly: tiny hitboxes + grass/flowers in front of them caught the crosshair. Fixed client side (pick radius, look through plants). |
| cheered-up (geplofd) mobs blocking | Not a cause: `isPickable()` is false once geplofd (synced data), the crosshair goes through them. |
| (found) hitting the boss from just outside the arena | Ignored with a gold "te ver" message: the pond arena was too small (fix 2). |
| (found, not changed) boss intro | `VadsrijkBaas.krijgSchade` ignores all damage during the intro while the floating number still shows. Owner: kern baas; could show "0"/nothing. |

- Feedback: the vanilla crosshair indicator is the ready indicator; a landing hit plays vanilla's strong-attack sound (and our sound for
  a buffered hit); an early click gives vanilla's soft "no damage" whoosh (our own extra whoosh is removed: it played twice).
- Ranged/magic are unchanged (`klassen.Basisaanval` already had hold + a 4-tick buffer).

## Tests

- `-Pgt=VadsrijkMelee` (new class `klassen.VadsrijkMeleeGameTests`):
  - `startzwaardDubbel`: the starter is the bound lv-1 kussenzwaard, 2x a loose one, a lv-1 Proefklodder goes down in <= 3 hits;
  - `nabijKlapElkeInterval`: a sword (20 ticks) with the vanilla ticker reset after every click: hit 1 with the class switch, a spam click
    halfway does nothing (not remembered), hits exactly on the interval, 1 tick early, a click 5 ticks early lands when ready (not before),
    then 3 more hits on the interval; every hit does damage;
  - `kampvijverArenaRuim`: radius >= 24, the registered arena uses it, the shore around the pond is inside.
- `VadsrijkKlassen.aanvalstempoHoudtInterval` adjusted: "without the vanilla cooldown charged: nothing" is now "ready by the server's rhythm: a hit".
- Result: `-Pgt=VadsrijkMelee,VadsrijkKlassen` **14/14 passed** (aanvalstempoHoudtInterval: its Proefklodder is now lv 10, a lv-1 one died after two hammer hits). Gametest log: `starter sword: per hit 13.2 (rating 13.2), Proefklodder lv1 40.0 Vads, down in 2 hits`.
- No AutoCheck client run (optional; skipped to save memory). Hold-to-attack and the look-through-plants pick are client-only: check in the dev client.

## Not committed (integration regenerates)
Tiles, WereldPlekken.java, POI_VADSRIJK.md, AUDIT_VADSRIJK.md, veilige_plekken.json (restored with git checkout).
