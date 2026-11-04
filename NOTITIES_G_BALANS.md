# NOTITIES_G_BALANS: balance round 2 (second playtest, 2026-11-04)

Branch `110-g-balans` (from fba766b79). Agent G-BALANS. Klassen package and level code untouched.

## What changed

### 1. Verdediging, level-relative (kern `Schade`)
- `Schade.verdedigingFactor(def, attackerLevel)` = def / (def + 25 + 15 x level), capped at `MAX_VERDEDIGING` = 60 %;
  Elementweerstand comes after it (unchanged, capped at 75 %).
- `Schade.verdediging(n, stats, element, level)`; the old 3-argument form = level 1.
- The attacker level: `Schade.aanvallerLevel(bron, player)` = the mob's `mobLevel()`, else the defender's own
  `Spelers.level(p)` (**g-klassen**: if the level API is renamed, this one call follows).
- Armour: `BasisTypes.registreer` multiplies the Verdediging (base + per level) of every armour base type (any slice) by
  `HARNAS_VERDEDIGING` = 0.75. The Verdediging bonus roll: 1-3 (+0.3/lvl) -> 1-2 (+0.22/lvl).
- Tooltips/HUD: no place showed a defense % (checked RijkItem tooltip, SmidScherm, BoomScherm, HUD), so nothing to update.
  NOTITIES_POC.md still describes the old formula (historical).

### 2. Mob damage +30 % (config)
- `kern.Balans`: COMMON config `config/guhs-vadsrijk-balans.toml`, section `mobs`, key `schade` (default 1.3, range 0.1-5).
  Registered from `KernRegistry.register`.
- Applied in `Schade.doe` to every hit on a player whose source is a `VadsrijkMob` that is not a `VadsrijkBaas` (normal mobs,
  elites, boss adds; DoTs from mobs too), before Verdediging. Bosses unchanged.
- **Integration**: if g-klassen also creates `guhs-vadsrijk-balans.toml`, the two specs must be merged into ONE spec
  (NeoForge refuses two registrations of one file name); keep `mobs.schade`.

### 3. Potions: flat tiers, 2 s drink, 10 s shared cooldown
- `economie.Drinken`: amounts, `eigenschappen(gedeeld)` = Consumable component (2 s, DRINK animation, drink sound) +
  UseCooldown(10 s, group `guhs:vr_drankje`) so every potion of the group shows on the item cooldown overlay.
- Ids unchanged (no migration needed): `vr_item_vadsdrankje` = Vadsdrankje 300 Vads (40 brons), `vr_z2_grote_vadsdrank` =
  Groot Vadsdrankje 700 (300 brons = 3 zilver, Kandijkraam), `vr_item_energiedrankje` = Energiedrankje +50 (20 brons),
  `vr_z2_grote_energiedrank` = Groot Energiedrankje +120 (55), `vr_z2_warme_chocomelk` = 250 Vads + thaw (30).
  New: `vr_item_klein_vadsdrankje` = Klein Vadsdrankje 120 Vads (8 brons, zone-1 drankjes/algemeen shop; icon from
  tools/features/vadsrijk_economie.py `drankje_klein`).
- Kandijkraam now also sells the Vadsdrankje (40) and the Energiedrankje (20).
- Vahoeg/Snelpootjes: 2 s drink too, own 2 s cooldown (not in the shared group).
- Regenboogdrankje (caravan): 700 Vads + 120 energy (was full) + its buffs, 2 s drink, in the shared group.
- No Vadsrijk loot table contained potions; nothing to change there.
- Lang (nl = en): names/uitleg updated in nl_nl/en_us AND in the generator sources (vadsrijk_economie.py,
  vadsrijk_z2_systemen.py, vadsrijk_momenten.py); new key `vadsrijk.economie.drankje.wacht`.

### 4. No wild spawns next to paths
- kern `Spawners`: `PADEN` (registered paths with half width), `dichtstbijPad/padAfstand`, `bijPad(sl, pos)`:
  `magHier` refuses (inside the Vadsrijk) any stand spot within `PAD_VRIJ` = 5 of a path EDGE at the path's height
  (|y - path surface| <= 4), so caves under a path and the hat layer above still spawn.
- Zone 1 paths: `WezensPlekken` (kaart.py PADEN; + brug/haven/zuid), half width 2.8 (berg 4.5).
  Zone 2 paths: `Z2WezensPlekken.PADEN` (plan.py PADEN), half width 2.8.
- Zone 1 (`WezensSpawners`): every open-air spawner whose circle reaches the strip is moved straight away from the path
  (`naastPad`: same side, other side, +4, +8; inside its area, clear of campfires/no-go circles). Same groups, same maxima,
  same RNG order -> the plan is unchanged. Patrols: the route is shifted with the centre (parallel, beside the path).
  The mountain path (grot spawners): radius 14 and floor search y-10..y+10, kern refuses the ramp itself -> they stand on
  the flanks.
- Zone 2 (`Z2WezensSpawners.gebied`): grid spawners moved off paths the same way (6/12 further).
- Baseline plan before (same map): zone 1 {klodderweide=189, sporenwoud=194, druipgrotten=233, ring=6, zwamtempel=20}
  (227 surface + 98 floor), zone 2 {galmhellingen=79, knuffelmeer=70, splinterkloof=67, splinterkloof_bodem=33,
  spiegelburcht=12} (94 spawners). After: exactly the same numbers. Zone 1: 111 of 227 open-air spawners moved off a
  path, 2 found no free spot (left in place; kern's spot check keeps their mobs off the path); zone 2: 9 moved, 0 left.

### 5. Vahoegnir 50 % bigger
- `Vahoegnir.SCHAAL` = 3.75 (was 2.5; wingspan ~31, length ~27), culling box 30/12/30, entity box 6x4.5, ground shadow
  x1.5 (26x26 cells).
- Clearance checked against the generated tiles (scratch script, top solid block within the wing circle):
  - opening/BAM flyover (NpcRollen + the reflective copy): the dips were -25/-20, now -14/-11: wing circle clears by 1.3
    (old size with the old dip: -6, i.e. it already brushed a pink wool crown near (152,124,935));
  - Zwamzoet pass (`Drakenpas.stadPad`): clears by 1.7 (was 5.5), unchanged;
  - random passes (`Drakenpas.pad`): now each point is at least 12 above the highest ground under its wings (loaded chunks);
  - zone 2 ravine rest (`Z2Verhaal`): the floor around DRAAK_RUST is flat (125) out to 18 blocks, fine; the cutscene
    cameras close to him were scaled x1.5 from the rest point (`Z2NpcPlekken.DRAAK_1/2`) so they are not inside the tail;
  - dev `overvlucht`: the low point +4.

### Extra (coordinator, user feedback): cheered-up mobs have no hitbox
- `VadsrijkMob`: after the plof `isPickable/canBeHitByProjectile/isPushable/isAttackable/canBeCollidedWith` = false (skills and
  AoE already skipped `geplofd()`). Renderers draw it translucent (alpha `PLOF_ALPHA` 0x8C): wezens `WezenRenderer`,
  zone-2 `WezenRenderer`, kern `KlodderRenderer`, zone-2 boss helpers (welp, Mika-mijnwerker: `PlofRenderer`).

## Tests
- New `kern/VadsrijkBalansGameTests` (`-Pgt=VadsrijkBalans`): defense formula + armour/bonus/config, potion amounts +
  drink time + shared cooldown + prices, path-free spawners + kern spot check.
- `VadsrijkKernGameTests.schadeEnLevels`: the old "50 def halves" became "50 def vs level 5 takes a third off".

## Open / for integration
- One config file shared with g-klassen (see 2).
- Map generated files untouched.
