# NOTITIES_T_MODELS (night round T, branch 110-t-models, from dbf82899d)

Decision R5 = HYBRIDE: a better generator (model kit v2) + hand-shaped showcase models (Grauwe Klodderreus, Vahoegnir).

## Bones and animation names (for t-vfx and everyone else): read this first
- **No bone is renamed. No animation is renamed.** Existing animation files keep all their names and bones.
- **New bones (add-only):**
  - every zone-1 slime (klodder/kloddertje, bubbelklodder, stuiterklodder, moederklodder, druipslijm, kristalklodder, kampklodder):
    `wiebel` (child of `lijf`): the top cap, a lagging squash-and-stretch ("jiggle") in every animation.
- **Extra keyframes (add-only):** in looping animations, bones named oor*/staart*/manen*/pluim*/ear_* that the animation did not
  move get a small lagging wiggle (`vadsrijk_t_modelkit.naloop`; Vahoegnir: `_oren_wiebel` in the flyover anims vlieg/zweef only).
  Animations that already move a bone are untouched.
- **Vahoegnir boss animations (`vr_z3_baas_vahoegnir.animation.json`, `boss_anims()`): NOT touched** (t-vfx's area).
- **Grauwe Klodderreus: a NEW model** (was the vanilla slime model). Bones: `root > kern (roll axis, pivot 0,6.5,0) > lijf
  (squash from the ground) > wiebel > oor_l, oor_r`; `lijf > mond, wenkbrauw_l, wenkbrauw_r, druppels`.
  Animations: main controller `idle, sprong, land, rol, blij`; trigger controller `actie`: `spuug, brul, aanloop`.
  t-vfx: to add boss animations, add them to `anims_klodderreus()` in `tools/features/vadsrijk_t_models.py` and to
  `KlodderGeo.ACTIES` (+ a `KlodderGeo.speel(this, "<naam>")` call in Klodderreus.java).

## What I built
- **`tools/features/vadsrijk_t_modelkit.py`** (model kit v2, not a feature by itself):
  - `rond_blok`: a big cube becomes a rounded box (3 axis bars + 1 inner cube; edge radius >= 2.5 gets 3 steps = 7 cubes).
    Automatic for cubes >= 3 px thick; sharp roles (kristal, zwaard, schild, tand, snavel, ...) stay sharp. `rond=` overrides.
  - `taps`: long limbs (poot/been/broek/laars) in 2 parts, the lower one thinner.
  - v2 painting: light over the model height, contact shadow + bevel, recessed steps slightly darker, soft mottling,
    soft patterns (glans = specular blob + sparkle only on big faces, stippen with shadow, plekjes as blobs, facet with light
    edges, strepen soft), fur strokes on vacht/pluis/manen/geest/mos.
  - **Faces in model space** (`gezichten_wereld`): the face of a rounded box is drawn once over the whole front and copied
    onto every front step, so faces stay big on round bodies. Drawn 4x supersampled (anti-aliased) + an extra eye catchlight.
  - glowmasks: hot centre, softer edge (`gloed_v2`).
  - `naloop` (secondary motion), `wiebel_kap` (slime cap jiggle).
- **`vadsrijk_wezens.py`**: `Model(..., stijl=)` + `with stijl(2):` switch (default stays 1, so other users of Model such as
  rijdieren/momenten are unchanged); `cube(..., rond=)`; `schilder_v2`; the slime body (rounder, cap on `wiebel`).
  Zone 1 `assets()` builds with stijl 2.
- **`vadsrijk_z2_wezens.py` / `vadsrijk_z3_wezens.py`**: built with stijl 2 + naloop; the z2 painter got the v2 branch (z3 uses it).
- **`vadsrijk_draak.py`** (Vahoegnir, same design): `_licht_wereld` (light from above over the textures: more volume),
  `_oren_wiebel` in the flyover anims. Geometry, colours, ears, boss animations: unchanged.
- **`tools/features/vadsrijk_t_models.py`** (new, in FEATURES): the Grauwe Klodderreus model/anims/textures
  (`textures/vadsrijk/tmodels/grauwe_klodderreus{,_blij,_gloed}.png`) + the showroom texts (nl + en).
- **Java `feature/vadsrijk/tmodels/`** (new):
  - `KlodderGeo`: a `GeoReplacedEntity` so kern's slimes (plain VadsrijkMobs) get GeckoLib models without changing their
    classes. REUS = the Klodderreus (synced trigger anims), KLEIN = proefklodders + the kloddertjes he spits (zone-1 klodder model).
  - `client/KlodderGeoRenderer`: grey/blij texture, glowing eyes (not after the plof), plof translucency, client-side choice of
    sprong (in the air) / land (12 ticks after landing) / rol (fast on the ground) / idle.
  - `TModels`: `/guhs vadsrijk models toon|plof|weg|reus [vecht]|reus anim <actie>` (see SPIEK_T_MODELS.md).
  - `TModelsGameTests`: assets fit the code (bones/animations), the giant plays his trigger anims.
- **Offline previewer** `tools/vadsrijk/model_voorbeeld.py` (uses renders-mounts/_bron mountkit + mcrender, outside the repo).
  Before/after sheets in `voorbeelden/` (not committed, in .git/info/exclude).

## Shared-file changes (all marked // T-MODELS or with a comment)
- `VadsrijkFeature.register`: + `TModels.register(modBus)` (1 line).
- `kern/client/KernClient`: the 3 slime renderers -> `KlodderGeoRenderer::klein/::reus` (KlodderRenderer kept, unused).
- `kern/baas/klodderreus/Klodderreus.java`: + 4 one-line `KlodderGeo.speel(this, "...")` calls (brul in opFase, spuug in
  spuug()/spuugBal(), aanloop when he aims a jump).
- `tools/features/__init__.py`: + `'vadsrijk_t_models'` at the end of FEATURES.
- `vadsrijk_wezens.py`, `vadsrijk_z2_wezens.py`, `vadsrijk_z3_wezens.py`, `vadsrijk_draak.py` (see above).

## Decisions I made myself
- Kit v2 is opt-in (`stijl(2)`) per zone; zone 4 (t-z4wezens) can switch it on with `with vw.stijl(2):` around its builders
  plus `vw._kit().naloop(m, anims)`.
- Elites keep the old additive gold layer (made from the new texture).
- Emissive: no glow added to mobs that had none (grey ontvadst mobs + glowing eyes everywhere felt too menacing); the existing
  glowmasks got the soft gradient, and the Klodderreus got glowing purple eyes (like the grey dragon).
- The Klodderreus picks sprong/land/rol on the client from its movement (the server state is not synced); only the actions are
  server-triggered. Scale x3 (model 16 px wide = 3 blocks, hitbox 3.06).
- Proefklodders/kloddertjes use the zone-1 klodder model (size from grootte(): x0.68 per size step).
- Vahoegnir: only light/volume + ear secondary motion, no shape change ("nicer, not different").

## FPS: cubes per model (before -> after)
Max per regular mob 44 (Mika's with gear); bones unchanged except `wiebel` on the 7 slimes. Texture sizes: still 4 px per
model px, widths unchanged (64/96/128 units), heights grew (more regions; tallest 64x256 units = 256x1024 px, sporenpriester).
Klodderreus: 39 cubes, 128x? units. Vahoegnir: unchanged.

| mob | before | after |
|---|---|---|
| zone1/bubbelklodder | 8 | 17 |
| zone1/druipslijm | 11 | 20 |
| zone1/glimzwam | 9 | 15 |
| zone1/hoedjesbol | 9 | 15 |
| zone1/kampklodder | 10 | 19 |
| zone1/klodder | 6 | 15 |
| zone1/knaag | 15 | 21 |
| zone1/kristalklodder | 10 | 19 |
| zone1/moederklodder | 9 | 18 |
| zone1/mosgeest | 10 | 19 |
| zone1/sporenpriester | 15 | 33 |
| zone1/sporenpuffer | 9 | 12 |
| zone1/stuiterklodder | 9 | 18 |
| zone1/tempelridder | 16 | 34 |
| zone1/zwamwachter | 13 | 19 |
| zone2/galmvleermuis | 13 | 19 |
| zone2/ijspuffer | 15 | 24 |
| zone2/ijsvogel | 6 | 6 |
| zone2/ijzelgeest | 13 | 28 |
| zone2/kloofgier | 17 | 23 |
| zone2/kristalmot | 5 | 5 |
| zone2/kristalnaaldkruiper | 14 | 20 |
| zone2/kristalspin | 18 | 27 |
| zone2/mika_boorder | 24 | 39 |
| zone2/mika_mijnwerker | 22 | 37 |
| zone2/scherfgeest | 14 | 17 |
| zone2/schotsbijter | 20 | 29 |
| zone2/sneeuwhaas | 12 | 18 |
| zone2/sneeuwknabbelaar | 11 | 24 |
| zone2/spiegelwacht | 19 | 37 |
| zone2/splinterling | 15 | 33 |
| zone2/versteende_wacht | 16 | 34 |
| zone3/bijtbloesem | 11 | 11 |
| zone3/bladrover | 25 | 40 |
| zone3/dove_ziener | 19 | 37 |
| zone3/grauwe_rups | 21 | 39 |
| zone3/grauwe_wesp | 20 | 20 |
| zone3/harsgolem | 15 | 33 |
| zone3/kaasschutter | 26 | 44 |
| zone3/lantaarnkever | 6 | 6 |
| zone3/lantaarnloze | 16 | 28 |
| zone3/mika_schimmelboer | 26 | 44 |
| zone3/rotzwijn | 17 | 30 |
| zone3/schimmelbolletje | 2 | 5 |
| zone3/schorskruiper | 14 | 23 |
| zone3/schorsmarter | 11 | 17 |
| zone3/sporenmot | 5 | 5 |
| zone3/wortelmuis | 10 | 13 |
| zone3/wortelschim | 15 | 27 |
| zone3/wortelslang | 13 | 25 |
| **totaal (50 mobs)** | **685** | **1161** |

## Tests
(see the end of this file: filled in after the gradle run)

## Open / resume point
- The look is judged by the user in the dev client ("goed is goed"): I did not do extra tuning rounds from renders.
- Not done: hand-shaping of other bosses/iconic mobs (Pluimguh, Vahoegje...), zone-4 mobs (t-z4wezens can opt in), LOD for
  jiggle (GeckoLib animates all bones anyway; the extra keyframes cost little).
- Regenerate: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_{wezens,z2_wezens,z3_wezens,draak,t_models}.py`.
