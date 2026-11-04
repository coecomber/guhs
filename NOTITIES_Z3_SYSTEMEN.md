# NOTITIES_Z3_SYSTEMEN: zone 3 "De Schemerwortel", slice systemen (branch 110-z3-systemen)

## Kern: layered areas (EARLY COMMIT 206551a31, other slices can merge/cherry-pick it)
- `kern/Gebieden.Gebied` has an optional `Laag` (y-range and/or cylinder/ring around a centre). The old 8-argument constructor
  still works (no layer). Build with `Gebied.cilinder(id, cx, cz, rMin, rMax, minY, maxY, lo, hi, stad)` or
  `new Gebied(...).metY(minY, maxY).metRing(cx, cz, rMin, rMax)`.
- Lookups: `Gebieden.op(x, y, z)` / `Gebieden.op(BlockPos)` = full (layers). `Gebieden.op(x, z)` (2D) skips layered areas, so in
  zone 3 it returns the catch-all `z3_ring`. All existing callers that have a y now use the 3D lookup.
- `zone3/systemen/Z3Lagen.GEBIEDEN`: the zone-3 plan, copied from z3-wereld's POI_Z3.md v0 (same ids/order) + `z3_ring`.
  `Z3Lagen.kern(id, cilinder, a, b, c, d, minY, maxY, lo, hi, stad)` converts z3-wereld's `Gebied3` fields.
  `Z3Lagen.registreer()` puts them in front of zone 1/2 (called by Z3SystemenRegistry).
- Biomes: `VadsrijkBiomen.index(x, y, z)` (the biome source is 3D in zone 3): honingtak, kruinzee, takkenbanen, schorsstad
  (+ stamspiraal), wortelvelden, wortelduister, zeppelinwerf, hart, schemerwortel (rest of the box). "Het Vadsrijk - <naam>".
- `kern/Vaardigheden.NA_GEBRUIK` (player, skill) after a successful skill use.
- Merged 110-i-audio (fast-forward) so `StadsMuziek.stadOp(x, y, z)` exists and the client player uses it.

## Package `feature/vadsrijk/zone3/systemen` (registry `Z3SystemenRegistry`, client `client/Z3SystemenClient`)
Generator `tools/features/vadsrijk_z3_systemen.py` (one line in FEATURES). Ids `vr_z3_*`, lang `vadsrijk.z3.systemen.*`.
Dev (op 2): `/guhs vadsrijk z3systemen bord [premies|jacht|winkel] | winkel | penningen <n> | veren <n> | ruilveren |
premie <slot> (finish it) | dag | zegel | kristal <soort> <0-2>`.

## API for the other zone-3 slices
- **z3-npc**
  - Jagershut: Premiebord/Jachtbord/Jagersguh -> `Premiebord.open(p, Premiebord.PREMIES | JACHT | WINKEL)` (one screen, 3 tabs).
  - Juweliersguh Glinster -> `nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Juwelier.open(p)` (same juwelier, handles zone-3 gems
    and 3 sockets).
  - Drankjeswinkel -> `Winkels.open(p, Z3SystemenRegistry.HONINGKRAAM)` ("De Honingkraam": tier-3 potions + old tiers).
  - Nestkapel (4th advancement at class lv 50) -> `Advancement.kanDoen(p)` / `Advancement.doe(p)` (klassen); quest objective
    "class level 50". Chat + tip at lv 50 are already sent by `Advancement4`.
  - Stalmeester Veertje -> `GoudenVeren.kanRuilen(p)` / `GoudenVeren.ruil(p)` (25 Gouden Veren = gold Pluimguh, bound, server msg).
  - Quest xp: use `Z3Xp.quest(level, hoofd)` (main 45 % of a level, side 18 %).
- **z3-wezens**
  - Mob xp: `Z3Xp.mob(level)` (1/55 of a level; elites x3 by kern), bosses `Z3Xp.baas(level)`.
  - Event bounties: call `Premies.activeer("ketel"|"sporen"|"honingpot"|"muisje"|"lantaarn")` once at registration and
    `Premies.meld(p, "<event>", n)` when it happens. Kill/elite/boss bounties count by themselves (`Haken.VERSLAGEN`, by the
    3D area at the foe).
  - Contracts: `Contracten` wires itself by reflection to `zone3.wezens.Z3Contracten` (spawnElite, VERSLAGEN, VERTROKKEN) when
    that class exists; no code needed on your side. Ids must match `Contracten.DOELWITTEN` (they do, copied from your file).
  - Penningwinkel wares (e.g. Premiejagerset pieces): `Premies.voegWaarToe(new Premies.Waar(id, prijs, () -> stack))` + a lang key
    `vadsrijk.z3.systemen.winkel.<id>`.
  - Weapons loot table `z3systemen:wapens`; crystals drop by themselves (lv 30+ foes in the zone box: 8 % ruw / elite 35 %
    geslepen / boss stralend); `Kristallen.stack("amberroze", q, n)` to drop one yourself.
- **z3-bazen**
  - Vahoeg altar: `Vahoeg.magVahoeg(p)` (lv 50), `Vahoeg.neemZegel(p)` (takes 1 Vahoegzegel or says why not).
  - After a Vahoeg boss: `GoudenVeren.geef(p, n)` and `GoudenVeren.rolGoudenPluimguh(p)` (2 %; do it on Baron Bleu or every boss,
    your call). Item `GoudenVeren.item(n)` for chests.
- **z3-wereld**: the wind columns are copied into `Z3Wind.KOLOMMEN` from POI_Z3 v0; the integrator can point it at
  `Z3WereldPlekken.WINDKOLOMMEN` (same fields). Areas: `Z3Lagen.GEBIEDEN` (see above).

## What's in it
1. **Layered areas + biomes** (see the early-commit section).
2. **Cap 50** (`kern/Levels` MAX/MAX_GENERIEK 50; the curve continues). Pacing in `Z3Xp` (30->50 = ~185k xp; main line ~7 levels,
   side ~1, ~1100 kills ~20, bounties/contracts the rest: comfortably 20 levels in 6-8 h). z2 tests adjusted (cap >= 35).
3. **Tree rows 6/7** (`Z3Boomrijen`), 4 points per node, row 6 at kl-lv 40/42/45 (needs a row-5 node), keystone row 7 at 48:
   - Ridder: Schorshuid, Afstijgstorm (Stormsprong +35 %, -1 s cd), Lantaarnschild (shield reflects 25 %), Drakenkussen (ult =
     party shield 20 %).
   - Jager: Zweefschot (+50 % from the air/mount, 8 s), Lichtpijl (Knuffelpijl marks +15 %), Sporenzoeker (+20 % vs status),
     Sterrenzwerm (3 after-lasers).
   - Magier: Lantaarnvlam (Vuurbal burns), Wortelband (Vadsbron -> Plakkerig), Blinkspoor (Blink heals party 8 %), Meteoorzwerm.
   - BoomScherm: DY 29 for 8 rows (not seen in a client).
4. **4th advancement at class lv 50** (`Advancement4`; klassen `MAX_STAP` 4, `nodigLevel(3)` 50): Nestwachter / Kruinvalk /
   Schemerwijze, hidden nodes `<x>_adv4`, golden-feather ceremony, Nestvorm ults (Plof +30 % and pulls adds in; Sterrenschot
   +40 % + 25 % true damage; Meteoorguh fire field 5 s).
5. **Weapons** (`Z3Wapens`): takkenknots (knots), schorshamer (hamer 2.4 s), liaanboog, raatkruisboog, bloesemstaf,
   lantaarnstaf (staf, 1.6 s). DPS rating 6.3 + 1.26/lv (~7 % over zone 2).
6. **Crystals** (`Z3Kristallen`): amberroze (Levensroof 1 %), honingkristal (Heal-kracht 4 %), wortelkristal (Verdediging 7),
   schemerkristal (Cooldownreductie 1.5 %), x1/1.7/2.6 by quality. Sockets lv 30+: 0-2, lv 45+: a rare 3rd (Guhddelijk 3).
   Kern `Rollen` now clamps at 3; JuwelierScherm height 244. Zone-2 `Kristallen.Soort` got an itemId component (4-arg ctor kept).
7. **Bounties** (`Premies`, `Premiebord`, `client/PremieScherm`): 3 a day per player from 23, reset 06:00 Europe/Amsterdam
   (`Z3Data.dag()`), 1 free swap, lv 40+. Reward: 5 % of a level xp, 150+8*lv brons, 10 Jachtpenningen (boss 15).
   Optional week goal: 15 bounties -> Premiekist (Episch/Legendarisch zone-3 weapon + geslepen crystal + 1 Gouden Veer).
   No lockouts, no leaderboards. Penningwinkel: Vahoegzegel 30, ruwe kristallen 12, geslepen verrassing 35, Herrolknabbel 15.
8. **Contracts** (`Contracten`): 2 of 8 targets a day (same for everyone), lv 45+, unlimited retakes; reward 25 penningen,
   10 % of a level xp, coins, 3 % Gouden Veer (instead of the plan's mount pattern).
9. **Vahoeg + Gouden Veren** (`Vahoeg`, `GoudenVeren`, items in `Z3Spul`).
10. **Wind columns + glide** (`Z3Wind` server, `client/Z3SystemenClient`): on foot Levitation (amp 9) + Slow Falling, set down
    towards the landing; on a mount: no gravity while inside (server) + the riding client pushes up 0.55 b/t and onto the landing.
    Pluimzweef: on a Pluimguh in the air hold jump = fall capped at 0.12 b/t, ~9 b/s forward; never up. Not gated by a quest flag.
    Riding blocks in zone 3 + columns feed the "rit"/"wind" bounties.
11. **Honingkraam** (`Z3SystemenRegistry.HONINGKRAAM`): Reuzen Vadsdrankje (1500 Vads, 9 zilver), Reuzen Energiedrankje (+200,
    150 brons), Bloesemthee (400 Vads + 60 energie + washes Plakkerig/Gesporen, 120 brons) + the zone-2 large ones.

## Kern / other-slice edits (marked z3-systemen)
- `kern/Gebieden.java`, the 3D callers (sfeer, stadsmuziek, npc, spawners, Karavaan, Z2Haken, Z2Sfeer), `kern/Levels.java`,
  `kern/Vaardigheden.java` (NA_GEBRUIK), `kern/loot/Rollen.java` (3 sockets), `klassen/Advancement.java` (step 4),
  `klassen/client/BoomScherm.java` (DY), `klassen/VadsrijkKlassenGameTests.java` (adv4 not counted),
  `zone2/systemen/Kristallen.java` (Soort itemId), `zone2/systemen/client/JuwelierScherm.java` (height),
  `zone2/systemen/VadsrijkZ2SystemenGameTests.java` (cap >= 35, rows 4-5 only), biomen (3D), VadsrijkFeature/VadsrijkClient.

## Open / for the integrator
- Not seen in a client: the board screen, the 8-row tree screen, the wind columns, the glide. No AutoCheck was run.
- The gold Pluimguh's "+5 % speed" is not done (economie owns the speed table; it is cosmetic + bound + server message now).
- Jachtpenningen are not in the HUD (only on the board screen).
- The plan's daily contract chest/lockout and the leaderboard are left out on purpose (ronde 18: no lockouts/leaderboards).
- When z3-wereld's generated `Z3WereldPlekken` lands: point `Z3Lagen.GEBIEDEN` (via `Z3Lagen.kern`) and `Z3Wind.KOLOMMEN` at it,
  or keep the copies (same numbers today).
- Mob commands in wezens still go to level 35? (`WezensCommando` is not mine.) Kern `level` dev command goes to 50.

## Tests (2026-11-04)
`-Pgt=VadsrijkZ3Systemen`: gelaagdeGebieden, levelCap50, advancementStap4, boomRij6, premiesResetEnRuil, kristallenZone3,
goudenVeren. Results: VadsrijkZ3Systemen 7/7, VadsrijkKlassen (incl. dpsTabelKlopt with the zone-3 weapons), VadsrijkZ2Systemen,
VadsrijkBiomen, VadsrijkKern: all green (31 + rerun 18 after fixing the schorshamer interval 2.8 -> 2.4 s and the respec test).
No AutoCheck (memory; screens, wind and glide not seen in a client).
