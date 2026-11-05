# NOTITIES_T_VFX (night round T, branch 110-t-vfx, from dbf82899d)

Roadmap 3.1 (bigger bosses, real animations) + 3.2 (cooler particles): the **VFX kit v1**, the **boss bouwdoos**, and every boss of
zones 1-3 upgraded with decal telegraphs, new particles, generic animations and at least one NEW mechanic.

## What was built
### VFX kit v1 (`feature/vadsrijk/vfx/` + `vfx/client/`)
- **One S2C payload** `guhs:vr_vfx` (kind + small tag, like beleving). Server API: `Vfx` (decal, weg, deeltjes, golf, straal, flits,
  spoor, zuil, pose, cast/castBreuk/castStop, schud, niveau). Nothing on the server spawns vanilla particles for the kit.
- **Ground DECALS** (`client/Decals`): flat textured quads that follow the ground (height per block column, looked up once per
  decal), 3 layers: faint fill, a progress fill that grows until the hit (circle/cone outward, donut from the safe edge out,
  strip/cross along its length, safe spot shrinks), a glowing rim (chevron tiles; hearts for safe spots), a flash in the last 15 %.
  Shapes: `Vorm` = cirkel, kegel, lijn, donut, veilig (everything EXCEPT the pink circle), kruis. Max 64 at once.
  - **`Telegraaf` now sends decals** (`Telegraaf.DECALS = true`; false = the old dust rings). So EVERY existing telegraph of every
    boss and mob (50+ call sites) is a decal now, one packet instead of dust every 3 ticks. New add-only `Telegraaf.kegel/donut/veilig`.
- **Own particles** (`client/Effecten`): sprite sheet `textures/vadsrijk/vfx/deeltjes.png` (13 kinds, `Deeltje` enum: ster, pluis,
  vonk, slijm, spoor, kristal, honing, confetti, hart, rook, veer, blad, gloed; animated frames, gravity/drag/spin/flutter).
- **Meshes**: shockwave ring (`golf`, flat ring + low wall), beam (`straal`, camera-facing strip), impact flash (`flits`), ribbon
  trail behind an entity (`spoor`), light pillar (`zuil`, with rising sparkles).
- **Screen shake / camera kick**: REUSED the existing beleving system (`Beleving.schudRond`, slider "schermschud" 0-100 % in
  guhs-vadsrijk-client.toml). `Vfx.schud` just calls it. No second slider.
- **Cast bar** (`client/CastBalk`, own gui layer above the hotbar layer): under the boss bar (`RijkHud.bovenVrij()`+2, or y 8 when there
  is no boss bar): "<baas> laadt: <aanval>", orange bar; interruptible casts are purple with a break meter + blinking "Onderbreek!";
  "ONDERBROKEN!" when stopped.
- **FPS budget + LOD + slider**: `VfxConfig` (config/guhs-vadsrijk-vfx.toml, VOL / ZACHT / UIT; also via
  `/guhs vadsrijk vfx niveau`). Budget: max 700 particles (VOL) / 250 (ZACHT, x0.4 counts, shorter lives) / 0 (UIT), oldest go
  first. LOD: particles only spawn within 48 blocks of the camera, meshes within 64, decals within 96 + their size. **Decals are
  gameplay and always draw** (at UIT without pulse/sparkle). Everything is drawn in 4-5 batches (one per texture) in
  `RenderLevelStageEvent.AfterTranslucentBlocks` with `RenderTypes.entityTranslucentEmissive(tex, false)` (no depth write, no cull).

### Boss bouwdoos (`Aanval`, `Bouwdoos`, `BaasVfx`, `BaasProfielen`)
- `Aanval` = data + fluent builder: steps (`Stap.op(na, telegraaf, Vorm, Anker)` + factor, duw, status, kleur, effect, schud,
  stapel, draai, aantal), cast bar on/off, `onderbreek(fractie)`, wind-up/hit animation names, `bijStart`/`nietOnderbroken` hooks.
  Anchors: BAAS, DOEL (tankbuster), ELKE_SPELER (spread), WILLEKEURIG (rain), MIDDEN.
- `Bouwdoos` runs one attack: telegraph -> wind-up (held) -> hit exactly the shown area (+ hit animation, particles per shape,
  shockwave, flash, shake). Stack = damage shared (alone 60 %). Safe spots = hit if in NONE of the pink circles. Interrupt = deal
  `fractie` x max Vads during the cast OR stun it (VERDOOFD) -> decals vanish, boss stunned 2.5 s, stars, "Onderbroken!".
- `BaasProfielen`: the bosses as data. **15 bosses, 30 attacks** (list in SPIEK_T_VFX.md). Mechanics used: 4 interrupts (Koning
  Klodder, Boormeester, Piepvel, Vahoegnir), 3 safe-spot attacks, 1 stack (Baron Bleu), in-out (Galmwolf) and out-in (Nestwachter),
  spreads, rains, tankbuster (Roquefort), cones, crosses.
- `BaasVfx`: hooks + per-boss state (WeakHashMap): `probeer` (at the boss's own "pick next move" point: 2/3 chance to do a
  ready profile attack, min 9 s between, own cooldown per attack, phase gates, first-use delay), `loopt` (own rotation/chase pause),
  `geraakt` (interrupt meter + rate-limited flinch), `fase` (rear-up anim, 2 shockwaves, light pillar, stars, shake), `verslagen`
  (cheer-up: `vfx_juich` + confetti + hearts + pink flash/pillar; zone 2 crystal shards instead), soft enrage after 6 min in one
  fight (`vfx_woede`, red smoke, attacks 2x as often), and one-liners `windup` / `cast` / `treffer` for a boss's OWN moves.

### Animations (GeckoLib)
- Generic set merged into EVERY boss animation file by `tools/features/vadsrijk_t_vfx.py` (28 files: `vr_baas_*`, `vr_z2_baas_*`,
  `vr_z3_baas_*`; Vahoegnir's file uses the draak geo bones): `vfx_windup` (crouch + lean + arms/wings back, HELD with
  thenPlayAndHold until the next anim), `vfx_treffer` (pop), `vfx_geraakt` (flinch), `vfx_fase` (rear up, shake, arms wide),
  `vfx_woede` (tremble), `vfx_juich` (sway, hops, right arm/front leg waves). Built only on bones the model has: `root` always,
  `body`, head (`head|kop|hoed|neck_1`), left/right "arm" (`arm_left|arm_l|leg_front_left|pootvl|vleugel_l|wing_l` and mirrors).
- A third GeckoLib controller `"vfx"` (`BaasAnims.controllers`) next to "beweeg"/"actie" in BazenBaas and Z2Baas (so all zone 1-3
  bosses). Our own one-shots go to "vfx"; attack anims named in a profile (`treffer("stamp")`) go to the boss's "actie".
- **Klodderreus has no GeckoLib** (slime renderer): it gets the same moments as poses (`Vfx.pose` -> `client/VfxPose.squish`) via
  one hook in `KlodderRenderer.extractRenderState`: crouch during the wind-up (trembling), stretch on the hit, wobble on hit/phase,
  bounce on the cheer-up.
- Mika spy skipped (shared guh animations; not in the brief's list).

### The bosses (one-liners in their own code, all marked `// T-VFX`)
- Klodderreus: probeer at RUST; wind-up + cast bar "Plettersprong" on the jump, trail through the air, landing = pop + slime wave +
  splats; charge = wind-up + cast "Rolaanval" + rolling trail.
- Koning Klodder: probeer at RUST (not in the sky phase); cast "Grote Plof" on the shock wave; wind-up on the jump; landing wave.
- Opperzwam: probeer at RUST (not in the dark); stamp = wind-up + cast "Sporenstamp", hit = pop + spore wave per spot.
- Zone 2 + 3 (Galmwolf, Spiegelzucht, Boormeester, Kruinroofvogel, Taleggio, Brokkel, Roquefort, Piepvel, Wortelwurger, Nestwachter,
  Baron Bleu, Vahoegnir): through the Z2Baas base (probeer at the rotation pick, wait while it runs). Their own telegraphs are decals
  via Telegraaf. Vahoegnir (showcase) also: cast bars "Grauwe Huil", "Vleugelwind" (+ wind-up, feather wave), "Grauw Vuur"
  (+ wind-up, a grey-fire beam), a golden trail on its dives. Its profile stays out of phase 4 (the scripted egg dives + ending).

## Decisions I made myself
- Telegraaf -> decals globally (also mobs): biggest win for looks + network; the dust path kept behind a flag.
- Screen shake: reuse beleving's slider (it already does shake/kick/hit-stop) instead of a second one.
- Decals ignore the VFX slider (gameplay), only lose their pulse at UIT.
- Bouwdoos attacks come ON TOP of the bosses' own rotations (2/3 chance at a free pick, >= 9 s apart), not replacing them: the
  existing fights were tuned and tested; this adds without breaking.
- Bouwdoos damage = boss base hit x factor (MAGIE), same scale as their own moves (factors 0.6-2.0). Telegraphs >= 1 s (test).
- No random bouwdoos attacks on the GAMETEST server (`BaasVfx.OOK_IN_TESTS`), so the zone gametests that count moves stay stable.
- Soft enrage = faster attacks + red smoke only (no damage boost: solo-friendly).
- Cast bar placed UNDER the boss bar (roadmap 3.1 says "onder de baasbalk"; the brief's "over" read as "on top of / with").

## Shared-file changes (all add-only, marked `// T-VFX`)
- `VadsrijkFeature`: VfxRegistry.register + payloads (2 lines). `client/VadsrijkClient`: VfxClient.init (1 line).
- `kern/baas/Telegraaf`: `DECALS` flag, cirkel/lijn early-return to decals, new kegel/donut/veilig, javadoc.
- `kern/baas/VadsrijkBaas`: tick -> BaasVfx.tick; krijgSchade -> BaasVfx.geraakt + BaasVfx.fase; beloon -> BaasVfx.verslagen;
  new `public double tVfxSlag()`.
- `bazen/BazenBaas`, `zone2/bazen/Z2Baas`: registerControllers -> BaasAnims.controllers. Z2Baas.tick: wait while loopt; probeer at
  the rotation pick (`laatsteAanval = "vfx"`).
- `kern/client/KlodderRenderer`: squish + VfxPose.squish for the Klodderreus.
- Boss files: Klodderreus, KoningKlodder, Opperzwam, Vahoegnir (one-liners listed above).
- `tools/features/__init__.py`: `'vadsrijk_t_vfx'` appended at the END of FEATURES (must stay after every boss/model slice).
- Generated (by my generator, committed): 28 boss animation files (+vfx_* keys only), lang nl_nl/en_us (+46 keys),
  `textures/vadsrijk/vfx/*.png` (6).

## For t-models (Klodderreus + Vahoegnir models) / integration
- Keep these bone names (my anims use them): Vahoegnir/draak: `root`, `body`, `head`, `wing_l`, `wing_r` (plus their existing anim
  bones). Any boss: `root` must exist; `body`, `head`, `arm_l/arm_r` (or the other names above) are used when present.
- If the Klodderreus becomes a GeckoLib model: name its animation file `vr_klodderreus.animation.json` (or
  `vadsrijk/klodderreus.animation.json`; the generator picks both up, geo with the same path) with at least `root` (+ `body`), make it
  implement GeoEntity and call `BaasAnims.controllers(controllers)` in its registerControllers; keep the KlodderRenderer pose hook
  idea (or drop it: BaasAnims then drives it).
- **If anyone regenerates boss animation files after this branch, re-run `vadsrijk_t_vfx.py`** (it is idempotent: overwrites only
  the vfx_* keys). FEATURES order handles it in a full build.

## Tests
- `VadsrijkVfxGameTests` (`-Pgt=VadsrijkVfx`): vormen (hit geometry of all 6 shapes), profielen (15 bosses covered, unique ids,
  telegraphs >= 20 ticks, mechanics used), telegraafIsDecal, bouwdoosRaakt (spread hits both fighters; a circle where nobody stands
  hits nobody), onderbreken (6 % damage interrupts + stuns + no heal; not interrupted -> the follow-up runs).
- Results: see the bottom of this file.

## Resume point / open
- NOT checked in a client (the user tests in the dev client): looks of decals/particles/cast bar, GeckoLib vfx_* poses per model
  (generic keyframes may look odd on some rigs: e.g. `vfx_fase` lifts root 2 px), shader packs (Iris) with the emissive pipeline.
- Own moves of the zone-2/3 bosses (other than Vahoegnir) did not get wind-up/cast one-liners: they get the decals, flinch, phase,
  cheer-up and their profile attacks. Adding `BaasVfx.windup/cast(this, ...)` next to their Telegraaf calls is a 1-line job each.
- Vahoeg mode does not shorten bouwdoos telegraphs (Z3Baas.tg) - could be added in `Bouwdoos.leg` for Z3Baas.
- Sound: bouwdoos uses vanilla sounds (ravager roar / evoker prepare / shield break); the t-geluid slice may want own stingers.
