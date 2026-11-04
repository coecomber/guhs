# NOTITIES_FIX_BELEVING — Vadsrijk fix round 1, agent BELEVING (branch 110-fix-beleving)

Design: GUHS1100_AVONTUUR.md §9 round 14 + VOORSTEL_EPISCHER_ZONE1.md ideas 19-22, 25, 27, 28.
New package `feature/vadsrijk/kern/beleving` (server) + `kern/beleving/client` (client). Generator `tools/features/vadsrijk_beleving.py`.

## What it does
1. **Hit-stop + camera kick + screen shake** (`Beleving.naKlap`, `BelevingClient`): a player's hit on a VadsrijkMob that is a crit
   and/or heavy (>= 12 % of the mob's max Vads, or a basic hit with hamer/knots) freezes the target's animation 2/3/4 ticks
   (client: its `tickCount` is held, works for vanilla and GeckoLib models) for everyone within 40 blocks; the attacker's camera
   kicks up. Screen shake: boss hits on a player, boss landings (> 2.5 blocks fall, EntityTickEvent), player falls > 4 blocks
   (LivingFallEvent, e.g. Knuffelsprong), ults (detected by the ult charge dropping from 100), phase/victory/chest landing.
   Option slider: `config/guhs-vadsrijk-client.toml` `vadsrijkSchud` 0-100 (Guhs > Config in the mod list); 0 = shake, hit-stop,
   kick and camera swoops all off.
2. **Sound layer** (`Klankkleur`): per weapon kind (BasisType.soort: zwaard/knots/hamer/boog/kruisboog/katapult/staf/toverstok/boek,
   else fist) two layered pitched vanilla sounds; crit = bell + amethyst + twinkle "ding" (+ end-rod sparkle); element sound
   (Vuur crackle, Water squelch, Natuur spore puff, Roze chime; DoT ticks quieter). Max one weapon sound per player per tick,
   one element sound per target per tick. Plof note: client plays `guhs:klusjes.piep` + note bell, rising a major scale per streak step.
3. **Damage numbers** (`Getalstijl`, via kern `Getallen.toon`, API unchanged): size log-scales with damage (0.75-2.4), crit = gold
   + "★" + bold + bigger, element colour (white without element, red on players), " Zwak!" (x1.5) / " Taai" (x0.7), DoT ticks
   (skillId "status") on one target merge into one rising number "N ♨" (within 30 ticks).
4. **Vrolijke reeks** (`Reeks` + HUD right of centre): every opgevrolijkt mob +1 for each helper; breaks after 5 s (100 ticks).
   x10 / x25 / x50 / every 25 after: confetti plof (particles + firework sounds); xp bonus +10 % from x10, +15 % from x25,
   +20 % from x50 (my choice for "higher milestones"), via the new `Spelers.XP_FACTOR` hook. "Reeks voorbij: xN" when >= 5 breaks.
5. **Phase mini-moment** `Beleving.faseMoment(baas, fase)`: kern now calls it automatically for every boss when a phase starts
   (before `opFase`); calling it again for the same boss+phase does nothing (so bazen may call it too). Client: 22-tick camera
   swoop to the boss + FOV zoom + letterbox, boss frozen 12 ticks, everything around at half animation speed for 1 s; server: roar
   (ravager + dragon growl), red ring, shake; the boss bar area flashes and the phase name shows under it.
   Phase names: lang key `vadsrijk.kern.beleving.fase.<entity id path>.<fase>` (e.g. `...fase.vr_baas_koning_klodder.2`),
   fallback "Fase 2: Nu wordt het menens!" / "Fase 3: Alles of niks!". **Bazen: add your names under those keys.**
6. **Victory moment** (`Beleving.overwinning`, from `Haken.VERSLAGEN` when the mob is a VadsrijkBaas): slow-mo camera on the boss
   + white flash; +18 ticks a pink shockwave ring over the arena radius (petals stay on the floor ~2 s); guhs (GuhEntity, incl.
   RijkGuh NPCs) within radius+16 hop in place with notes (they don't walk); +34 ticks title card "Opgevrolijkt!" + boss name.
7. **Chest ceremony** (`Kistceremonie` = `BaasKist.CEREMONIE`): the chest falls 16 blocks (BlockDisplay, 14 ticks) in a light
   pillar (end rods + pink dust, 3 s), the real block appears on landing. After a boss defeat the fall starts 50 ticks after it.
   In the GameTestServer it is placed at once (VadsrijkBazenGameTests checks the chest right away). Server stop: everything in the
   air lands at once. Opening: 3 + rarity notes rising, then a burst in the best rarity's colour + rarity sound; Guhddelijk: pink
   screen flash, 3 s glitter rain, server-wide message "<speler> vond <item>!".

## Kern edits (for the integrator; all marked `BELEVING (fix round 1)`)
- `kern/Getallen.java`: `toon(...)` body now delegates to `beleving.Getalstijl.toon` (same signature); `tekst(...)` returns the
  display (was void; callers unaffected). Removed 2 unused imports.
- `kern/Spelers.java`: new `XP_FACTOR` list (ToDoubleFunction<ServerPlayer>), applied at the top of `geefXp`.
- `kern/baas/BaasKist.java`: `plaats` wraps the block placement in a Runnable and hands it to `CEREMONIE.val(...)` if set;
  `useWithoutItem` collects the loot copies and calls `CEREMONIE.open(...)`. New `Ceremonie` interface + static `CEREMONIE`.
- `kern/baas/VadsrijkBaas.java`: one line in `krijgSchade` (phase loop) calling `Beleving.faseMoment(this, fase)` before `opFase`.
- `kern/KernRegistry.java`: `Beleving.register()` at the end of `register`, `Beleving.payloads(r)` in `payloads`.
- `kern/client/KernClient.java`: `BelevingClient.init(modBus)` at the end of `init`.
- Shared: `tools/features/__init__.py` gets `'vadsrijk_beleving'` appended; lang en_us/nl_nl get 9 keys (re-run the generator
  after merging if the lang files conflict; all Java texts also have a Dutch fallback).
- Uses `Schade.VOOR_KLAP` (index 0, returns the damage unchanged, only remembers the current Klap for DoT detection) and
  `Schade.NA_KLAP`. If agent gevecht reworks Schade: keep VOOR_KLAP before `Getallen.toon` and the "status" skillId for DoTs.

## Dev commands (`/guhs vadsrijk beleving ...`, op)
`getallen`, `reeks <n>`, `klap` (nearest mob), `schud`, `fase` (nearest boss, forced), `overwinning` (effects only), `versla`
(really defeats the nearest boss: victory + real chests), `kist <zeldzaamheid> [pos]`.

## Tests
- Gametests `-Pgt=VadsrijkBeleving` (batch vadsrijk_beleving): `vrolijkeReeks`, `schadegetallen` (incl. DoT merge through the real pipeline).
- AutoCheck script `tools/autocheck/vadsrijk_beleving.txt`.

## Open / notes
- The hit-stop is client-visual only (the server mob keeps its AI); a real server freeze would touch AI (gevecht/bazen).
- Walk-leg animation of vanilla-model mobs is not held (no setter; would need a mixin); idle/GeckoLib animations are.
- The victory title replaces a level-up title card that arrives in the same second.

## Results (2026-11-04)
- Gametests `-Pgt=VadsrijkBeleving`: 3/3 passed (vrolijkeReeks, schadegetallen + the filter self-check).
- AutoCheck `vadsrijk_beleving.txt` (17 shots in run/screenshots/autocheck, not committed): numbers (sizes, gold ★ crits, element
  colours, Zwak!/Taai, merged DoT ♨), streak x4/x12 +10 % xp, falling chest with light pillar, Guhddelijk pink flash + glitter +
  "Dev vond Kussenzwaard!", Koning Klodder phase moment (swoop + letterbox + "Fase 2: Nu wordt het menens!"), real defeat ->
  shake + "Opgevrolijkt!" card. Not visible in shots (camera at the door, out of sight): the shockwave ring and the arena chests.
  After the run: the streak number no longer overlaps its title/bar (pop scale capped, centred).
- Resume point: done; only the integrator steps above. The ★ glyph renders as a chunky blob in the Guhs font at big sizes (ok).
