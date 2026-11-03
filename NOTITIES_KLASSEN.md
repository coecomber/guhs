# NOTITIES_KLASSEN (Guhs 1.10.0 "Het Vadsrijk", slice klassen)

Branch `110-vr-klassen`, worktree `guhs_vadsrijk/w-klassen`. Package `feature/vadsrijk/klassen`, generator `tools/features/vadsrijk_klassen.py`.

## What is there

| file | what |
|---|---|
| `KlassenRegistry` | items, class registration, tick/logout/stop events, `LEVEL_OP`/`BINNEN` hints, `/guhs vadsrijk klasse ...` |
| `jager/Pluisjager` | class + R Pluizenregen, V Rolsalto, G Knuffelpijl, Z Sterrenschot |
| `magier/Vadsmagier` | class + R Roze Vuurbal, V Blink, G Vadsbron, Z Meteoorguh |
| `RidderMetBoom` | **kern adapter**: the Knuffelridder with tree, titles and node effects (see below) |
| `Basisaanval` | ranged/magic basic attack (hold left mouse), shot kinds per weapon kind, `treffer()` helper (Schade + ult charge + forced crit) |
| `schot/Schoten` | server-simulated projectiles (travel time, block/mob sweep, pierce, homing, trails, ItemDisplay arrow that points along its flight) |
| `boom/Bomen`, `boom/Boom` | the 3 trees (12 nodes each + 2 hidden advancement nodes) and the rules (buy, either/or, "a\|b" requirements, respec) |
| `KlasseBalie` | API `open(p)`, `kiesEerste(p)`, `boom(p)`, `wissel(p, klasse)`, starter weapons, desk session, screen data |
| `Advancement` | API `kanDoen(p)`, `doe(p)`, titles, the ceremony (RijkScene + light beams), `VIA_BALIE` |
| `Wapens` | 9 base types + items `vr_kl_*`, loot table `klassen:wapens` (all 13 class weapons) |
| `client/` | key K, attack interception + hold-to-fire, `BoomScherm` (tree + ordetempel desk), `KeuzeScherm` (first class) |
| `Later`, `Fx`, `KlassenPlekken`, `KlassenPayloads` | planner, FX helpers, coordinates, network (`guhs:vr_klassen_*`) |

## Decisions

- **Ranged basic attack**: no projectile entity class; the server simulates the projectile and shows an ItemDisplay
  (arrow / spectral arrow / cocoa bean / paper) or only particles (magic). This needs no renderer and nothing is saved. The client cancels
  the vanilla attack with a ranged weapon in hand (so you can't break/hit with it) and sends `Schiet` every 3 ticks while the button is
  down; the server keeps the weapon kind's interval (Aanvalssnelheid makes it faster) and buffers a press up to 4 ticks early.
- Weapon kinds: boog 6 t, kruisboog 13 t (pierce 1), katapult 9 t (lob + splash 1.6), toverstok 6 t (light homing), staf 10 t (splash 1.5),
  boek 12 t (strong homing). Splash = 50 % on the others.
- **Trees**: rows cost 1/2/2/3; the R-skill variants (fire vs slime etc.) exclude each other; capstones need their column (the R capstone
  needs either variant: "a|b"). Total 22 points vs 19 available at lv 20, so you always leave something out.
- **Advancement** = hidden nodes `r/j/m_adv1/2` (cost 0, row -1) with a stat bonus, which a respec keeps. Unlocks: see the class doc in
  `Advancement.java`. Titles: Knuffelridder → Kussenkampioen → Vahoege Paladijn; Pluisjager → **Sterrenschutter → Vahoege Hemelvalk**;
  Vadsmagiër → **Sterrenwever → Vahoege Aartsmagiër**.
- **Respec**: only at the ordetempel desk (session: `KlasseBalie.open` < 10 min ago, within 12 blocks), clears the current class only,
  1 hour real time (`System.currentTimeMillis`), an empty tree doesn't start the cooldown.
- **Class switch**: Vads keep their percentage, energy full, ult and cooldowns reset; first time in a class → starter weapon (bound, Gewoon,
  lv 1: kussenzwaard / pluisboog / rozenstaf). Armour is shared automatically (kern counts armour for every class).
- First class: `kiesEerste` opens the choice screen only when `extra("klassen").gekozen` is false. Switching also sets it.
- Until the npc slice's advancement quests exist, the desk has a "Ceremonie" button (`Advancement.VIA_BALIE = true`). Set it false at
  integration when the quests call `Advancement.doe(p)`.

## Balance (solo, lv 1-20, zone-1 numbers from kern)

Damage per hit = weapon `schade + perLevel x lv` x (1 + 2 % per main stat point). Weapon numbers (Gewoon, lv 1, before the main stat):

| class | weapon | per hit | interval | ≈ dps lv 1 (incl. main stat) |
|---|---|---|---|---|
| Ridder | kussenzwaard (kern) 8 + 1.6/lv | 9.6 | vanilla attack strength (5 t at attack speed 4!) | ~46 (if spam-clicking) |
| Jager | pluisboog 7 + 1.4/lv | 8.4 | 6 t | ~34 (+10 % crit) |
| Jager | snoepkruisboog 15 + 3.0/lv | 18 | 13 t | ~33 + pierce |
| Jager | eikelkatapult 10 + 2.0/lv | 12 | 9 t | ~32 + splash |
| Magier | rozenstaf 11 + 2.2/lv | 13.2 | 10 t | ~32 + splash |
| Magier | zwamstokje 6.5 + 1.3/lv | 7.8 | 6 t | ~32 + homing |
| Magier | slijmboek 13 + 2.6/lv | 15.6 | 12 t | ~32 + strong homing |

Ranged ≈ 70-75 % of the ridder's melee dps, paid back by range, safety and AoE. Base stats lv 1 → lv 20: Ridder Vads 100 → 366, Verd 8 → 36
(kern); Jager Vads 85 → 294, Beh 10 → 48, Verd 5 → 24, Krit 10 %; Magier Vads 80 → 270, Wijs 11 → 51, Verd 4 → 21, energy 130 → 187 (+8/s).
Skills: Kussenklap 1.6x cone / 3 s; Pluizenregen 3 x 0.6x AoE / 6 s; Knuffelpijl 1.8x or heal 12 % / 7 s; Sterrenschot 4x line;
Roze Vuurbal 2x AoE r3.5 / 5 s; Vadsbron 6 x (4 % + 0.25x) heal / 14 s; Meteoorguh 5x r6 + 6 x 0.1x.
Proeftuin klodder lv 1 = 40 Vads / hits 7: every class kills one in ~1-2 s at lv 1. Open: tune against the real wezens mobs at integration.

## Kern adapters (integration should lift these)

1. **`RidderMetBoom`** re-registers `knuffelridder` (Klassen.registreer replaces by id) around `Knuffelridder.INSTANCE`: it adds `boom()`,
   `titelKey()` and wraps the 4 kern skills. Lift: move `Bomen.ridder()` and the effects into Knuffelridder (or keep the wrapper).
   - The Doornenknuffel reflect assumes kern's block returns 20 % and reflects 50 % (`n / 0.2 * 0.5`). If kern changes those numbers, update.
2. Skill-specific cooldown/energy tweaks are done right after kern paid (`Later.na(0, ...)` changes `SpelerData.klaarOp` / `energie`).
   A kern hook like `Vaardigheid.cooldownTicks(ServerPlayer)` would be cleaner.
3. Forced crits (Rolsalto, Jachtroes) multiply the base damage and pass `kanKritiek=false`, so the floating number isn't gold. A
   `Klap.zekerKritiek` flag in kern would fix the colour.
4. **Kern melee ignores `AANVALSSNELHEID`** and the class weapons have no attack-speed attribute, so the ridder hits at full strength every
   5 ticks. Suggest kern maps Aanvalssnelheid/weapon kind onto the vanilla ATTACK_SPEED (or caps clicks) — otherwise the ridder outdamages.
5. Kern text `vadsrijk.kern.skill.ult_niet_vol` names the Vahoege Plof; with 3 classes it should be generic ("Je ultieme vaardigheid...").
6. The HUD shows `Klasse.naamKey()`; the advanced title is `Advancement.titelKey(p)` (kern's HUD could show it).
7. `SpelerData` has no "dirty" call (`Spelers.opgeslagen` from the contract doesn't exist); player attachments are saved with the player
   anyway, so nothing was needed.

## POI needs
See `POI_VERZOEKEN.md` (section klassen): ordetempel desk spot (+ ceremony circle) and the klasse-tempeltje in Knuffelkamp.
Placeholders in `KlassenPlekken` (not used by logic).

## Open choices for the user
- The two invented title pairs (Sterrenschutter / Vahoege Hemelvalk, Sterrenwever / Vahoege Aartsmagiër).
- Respec clears only the current class (one shared 1-hour cooldown for all classes).
- Should switching class be free everywhere in a town instead of only at the ordetempel? (Now: ordetempel only, per design.)
- Pixel-art of the 9 weapons is generated by code (`TEKENINGEN` in the generator); an art pass could replace them.

## Resume point
All 7 tasks built and compiled; gametests `-Pgt=VadsrijkKlassen` (3 tests). See TESTEN_KLASSEN.md for the dev-client checklist.
