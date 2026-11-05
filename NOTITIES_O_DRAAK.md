# NOTITIES_O_DRAAK: the Vahoegnir redesign, in game (branch 110-o-draak, from e5ab2935e)

The design was approved by the user on 2026-11-05: majestic and pink, with the guh-plush head and bigger round ears.
The renders are in `renders-draak/` (outside the repo).

## What changed
- **New generator `tools/features/vadsrijk_draak.py`** (it contains the mount kit's model kit). It writes every dragon asset:
  - `geckolib/models/entity/vadsrijk/wezens/draak.geo.json`: the same path as before, so every user gets the new model at once:
    the flyover/DraakVlucht, the city passes (Drakenpas), the zone-2 encounter (NpcRollen "draak" -> DraakVlucht) and the zone-3 boss.
  - `geckolib/animations/entity/vadsrijk/wezens/draak.animation.json`: vlieg, zweef, brul (the names Vahoegnir.java/DraakVlucht use).
  - `geckolib/animations/entity/vr_z3_baas_vahoegnir.animation.json`: idle, walk, vlieg + the trigger animations brul, adem, duik, land, buig
    (`Vahoegnir#actieAnimaties`).
  - `textures/vadsrijk/wezens/draak.png` + `draak_gloed.png`: the grey flyover and its purple glowing eyes (GloedLaag).
  - `textures/vadsrijk/z3bazen/vahoegnir_0..5.png` + **new** `vahoegnir_0..5_glowmask.png`: the boss turns from grey to pink per bone group
    (1 wing tips, 2 wings, 3 tail, 4 body/neck/legs, 5 head). The eyes glow purple until step 5.
  - Vahoegje: a new chibi model `geckolib/models/entity/vadsrijk/wezens/vahoegje.geo.json`, `vr_huisdier_vahoegje.animation.json`
    (idle, hop, sprong) and `z3bazen/vahoegje.png`.
- **`vadsrijk_z3_bazen_modellen.py`** calls `vadsrijk_draak.alles()`. The old `_draak_maskers`, `draak_texturen` and `vahoegnir_anims` are gone,
  and the Blockbench export now also writes `vr_huisdier_vahoegje.bbmodel`.
  **`vadsrijk_wezens.py`** no longer writes the dragon. `model_draak()` is still there but unused.
- **Java** (3 small changes):
  - `Z3BazenClient`: the boss renderer has its glow on (AutoGlowingGeoLayer -> `vahoegnir_<n>_glowmask`).
  - `Z3BazenRegistry.PET_VAHOEGJE`: model `vadsrijk/wezens/vahoegje`, scale 0.45 (was the big model at 0.12).
  - `Z3Scenes`: the hatching figurant uses the same model and scale.
- Entity ids and animation names are unchanged.

## Texture size
The model is painted at 4 px per model pixel, then halved alpha-aware to **2 px per model pixel** (`D_UIT`).
The textures are now 1024x1128 (were 2048x2192 in the design). The UVs are in model units, so the geo does not change.
The eyes still read well at 2 px; see `renders-draak/controle_ingame/kop_2px.png`.
Regions are packed on even pixels, so halving never mixes two regions.
Vahoegje stays at 4 px (small model, 1024 wide).

## Check without a client
`renders-draak/_bron/controle_ingame.py <worktree>` renders from the jar assets:
- steps 0..5;
- the head at 2 px;
- keyframes of vlieg, zweef, brul, walk, adem, duik and buig;
- Vahoegje.

## Regenerate
`PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_draak.py` (dragon only, ~10 s),
or the whole `vadsrijk_z3_bazen_modellen.py`. Both are deterministic.

## Open
- The ground pose of the boss (idle/walk/buig) keeps the wings **raised in a majestic V** instead of folded.
  A proper fold with these feather wings looked messy. Check in the client whether that works in the arena.
- The model is about 10 model px taller than the old one (feet at y=0, body at y 9-27). The boss hitbox is unchanged.
- The n-extra critters can reuse `vahoegje.geo.json` / `vahoegje.png` if they want a pink baby dragon.
