# NOTITIES_FIX_BAZEN: fix round 1, the bosses really fight (2026-11-04)

Branch `110-fix-bazen`, worktree `guhs_vadsrijk/f-bazen`. Owner: kern.baas.* (+ Grauwe Klodderreus), the bazen package, the
Zwamtempel door gate.

## Root cause ("he just stands there")
Every boss state machine waited on `getTarget()`, filled only by vanilla `NearestAttackableTargetGoal`:
- **26.1's `Mob#setTarget` ignores creative players** (see `GuhMockPlayer`), so a creative player is never a target: the boss
  does nothing. The temple door also opened for `instabuild` players (wereld's `WereldDeuren.klik`), which fits the user
  playing in creative.
- A survival player is dropped as target past the follow range, and nothing re-targets a player hitting from range.
- The jumps were plain velocity: in the Klodderreus pond (arena centre in water) the water drag ate them, so even with a target
  he barely moved.
A diagnostic gametest on a flat floor with a SURVIVAL player at 14-16 blocks showed the old AI does work there (all three
bosses moved and hit) - so the in-game failure was targeting (creative / range) + the pond physics.

## What changed
### kern.baas.VadsrijkBaas (shared by all bosses)
- Own target choice every 5 ticks among `vechters()`: players inside the arena (or within the loose radius / 24 blocks),
  not fainted-protected, not in a scene. **Creative players only when nobody else is there** (the boss acts at them, Schade
  still never hurts them). `doel()`, `verste()`, `afstandsSpeler()` (stood >= 9 blocks away for 2.5 s), `volgendDoel()`
  (ranged player first, 2 of 3 times).
- `springNaar(doel, ticks, hoogte)`: kinematic arc jumps (ignore water/drag), `geland()` hook; never ends inside a block.
- `lob(...)`: a blob flies to where a player stands, red circle meanwhile, splash damage on landing (punishes range).
- `raakRond(...)` shared AoE helper on fighters only.
- **Anti-cheese**: a hit from a player outside the arena (outside the fight radius for loose bosses) does nothing ("Te ver
  weg! Kom de arena in om de baas te raken."). Leash: outside the arena for 1.5 s -> jumps back to the middle. Arena empty
  for 30 s while damaged -> boss removed, the arena spawns a fresh one (full reset).
- Telegraaf colours: `PAARS` (mechanic), `GEEL` (stack) added next to ROOD (damage) / ROZE (safe).
- `opFase` untouched in form (one call per phase): ready for a `Beleving.faseMoment(baas, fase)` hook from the beleving agent.

### Grauwe Klodderreus ("instructive but real", lv 3)
- HP `1000 + 200*(lv-1)` = 1400 at lv 3 (was 940): ~2-3 min for a lv-3 player. Hit `14 + 3*(lv-1)` = 20 (unchanged).
- P1: jumps at you (red shadow 1.5 s + 0.7 s flight), plet 2.0x (40 raw) r 3.6, 45 ticks rest. A ranged player gets jumped.
- P2 (50 %): 4 kloddertjes, +2 every 12 s (max 6); spit lob (1.1x, r 2.8, 1.3 s) at whoever keeps distance; jumps 24-tick warning.
- P3 (20 %): circle roll (1.2x), straight roll-charge at a player down a red lane (1.3 s warning), jumps 20-tick warning.
- A lv-3 Pluisjager (~118 Vads) faints after ~4 ignored hits; never a one-shot.

### Koning Klodder (lv 8, clearly harder)
- Jumps (r 4.6, 2.0x), shock wave only when someone is within 7.5 (r 8, 1.5x), klodderspat: 3 lobs around a ranged player
  (1.2x + Plakkerig). Fase 3: double jumps.
- **Crown**: flies off at 80 % (once), at fase 3, and 1.5 s after every Hemelsprong. It bounces through the arena (glowing,
  20 s). Walk into it = you carry it (15 s); right-click (item/air/block/him) or walk up to him = throw: he is dizzy 4 s.
  Not a real item (an un-pickable ItemEntity + a carrier flag), nothing enters an inventory.
- **Shield**: one Kroonklodder per split (the blue or the yellow) has a visible ring shield in its weak element's colour; only
  its weak element (Water klodder: Natuur, Vuur klodder: Water) breaks it at once; otherwise it cracks after 8 hits (so a solo
  player without that element can still do the 12 s rule). Kroonklodders now hop at the king's fighters themselves.
- Arena change: fase 2 turns half the pond surface to slime blocks, fase 3 all of it (Plakkerig while standing on it);
  `Herstel` key `kk_slijm`, restored 10 s after the fight, on reset/new spawn, and at server start.
- Split rule (12 s), Hemelsprong (3 safe spots, 55 % true damage) unchanged.

### De Opperzwam (lv 19)
- Uses `doel()`; the dark-phase pounce is a kinematic jump.
- Ranged players get a spore ball lob (1.4x + Gesporen).
- **Sporenlast** (yellow circle following one player, 3.5 s): split among everyone within 3.2: `basis * max(1, 3.2/n)`.
- **Spreidsporen** (fase 2+, purple circle on everyone, 3.5 s): `basis * (0.6 + 1.4 * neighbours within 3.2)`.
- Arena change: fase 2 cracks 5 root fields in the floor, fase 3 another 8 (ROOTED_DIRT, Plakkerig) - key `oz_vloer`.
- **Fase 3 root tower**: he snaps to the middle and stays rooted; a 7-high tower grows 4 blocks east of the centre (mushroom
  stem, 3x3 platform at +6, root steps of 1 block spiralling one ring further out). Ground hits do x0.5, hits from the
  tower top (y >= his y + 5.5) do x2 ("his cap is the weak spot"). Key `oz_toren`, restored with the rest.

### De Mika-spion (lv 15)
- He did act (blinks, powder, kloddertjes, melee); now the powder goes first to the player at range, he melees the kern target.
- **Decoys** (fase 2+, every 30-40 s): 3 `vr_baas_lok_mika` decoys (same model/texture/name/HP bar), the real one's moustache
  glitters (wax + spark particles). Hitting a decoy: it poofs, you get Gesporen 3 s. They live 16 s.
- **Cheese-wheel avalanche** (every 21-26 s, from fase 1): 2 (fase 3: 3) red lanes over the cap, 1.8 s warning, then wheels
  (honeycomb crumbs) roll through: 1.3x + knockback.

### Zwamtempel door gate
- `Zwamtempel.MAG_NAAR_BINNEN` = level 16+ AND quest `hq10_zwamtempel` active/done (or `hq11_opperzwam` active/done).
  Creative is no exception (dev: `/guhs vadsrijk bazen tempel open`).
- Our RightClickBlock listener (priority HIGH) handles every right-click around the door (door box +3 x, +-4 z): below the gate
  a friendly Dutch line and nothing opens (vanilla doors/trapdoors/gates in the doorway never open); allowed = the ceremony
  (+ unlock in WereldDeuren so a later click opens it). The walk-up trigger uses the same gate and messages.
- Messages: `tempel.te_laag` (level) and new `tempel.verhaal` (quest).
- Not covered (wereld's ownership): breaking the door blocks in survival, or another way in over the roof.

### Not done (by request)
- Music hooks: nothing.

## Tests
- Gametests `-Pgt=VadsrijkBazen` (old 4 + new `VadsrijkBazenGevechtTests`: Klodderreus/Koning/Opperzwam acquire a still
  player at 14-16 blocks, come at them (<6 blocks), hurt them, no one-shot; creative-only player is targeted and jumped at;
  a hit from 40 blocks away does nothing; temple gate level/quest/creative + door area). Result: see below.
- AutoCheck script `tools/autocheck/vadsrijk_bazen_fix.txt` (player ~14 blocks away in each arena).

## Results / resume point
- Gametests `-Pgt=VadsrijkBazen`: **10/10 passed** (2026-11-04 10:42). Numbers from the run (player standing still at range):
  - Klodderreus lv 3 vs a lv-3 player (128 Vads): came from 14 blocks to 0, 5 hits in 18 s, biggest hit 27 (plet now 1.7x).
  - Koning Klodder lv 8 vs lv 8 (198 Vads): from 16 blocks to 0, 5 hits, biggest 43.
  - Opperzwam lv 19 vs lv 18 (338 Vads): from 14 blocks to 1, 11 hits, biggest 88 (26 %).
  - Note: gametest mock players of parallel tests are all around: `VadsrijkBaas.testFilter` (tests only) limits the fighters.
- AutoCheck `tools/autocheck/vadsrijk_bazen_fix.txt` (one run, creative player): screenshots in `run/screenshots/autocheck/`:
  - Klodderreus: red landing circle around the player at range (k3), he left the pond and hops at you (k1-k4).
  - Koning Klodder: jumped right onto the player 14 blocks away (q2: camera inside him), split + Kroonklodders (q5).
    `bazen fase 3` during the split does nothing (he is invulnerable while split, by design), so q5-q7 show the split, not
    the crown/Hemelsprong.
  - Mika: decoys + cheese-wheel lanes (m3: "Vier Mika's?!" + "kaaswiel-lawine" and the red lanes on the cap).
  - Opperzwam: NOT captured - my script teleported with `/tp ~ ~1 ~14` before the temple tile was loaded, the player fell to
    y 1. Fix for a next run: `wait 80` + `waitrender` after `wereld plek arena_opperzwam.centrum` before the `/tp`.
    The Opperzwam behaviour is covered by the gametest (target at range, approach, hits).
- Head commit: see `git log` on `110-fix-bazen`.

## Open points / for integration
- Lang keys added directly to nl_nl/en_us (kern key `vadsrijk.kern.baas.te_ver` has no generator; the bazen keys are also in
  `tools/features/vadsrijk_bazen.py`).
- Not visually checked in a client: the crown bounce/throw, the shield ring, slime ponds, the root tower (walkability: steps
  of 1 block, 3x3 platform at +6; please check in the dev client), the sporenlast/spreid circles.
- Beleving hook: `opFase` is the single phase entry point in every boss; a `Beleving.faseMoment(this, fase)` call can go at its top.
- Balance (first guesses): Klodderreus 1400 HP at lv 3; plet 1.7x, spit 1.1x, roll 1.2x. KK crown stun 4 s, shield 8 hits.
