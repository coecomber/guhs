# NOTITIES_T_MODELS (night round T, branch 110-t-models, from dbf82899d)

Decision R5 = HYBRIDE: a better generator (model kit v2) + hand-refined showcase models (Grauwe Klodderreus, Vahoegnir).

## Bones and animation names (for t-vfx and everyone else) - read this first
- **No bone is renamed. No animation is renamed.** All existing animation files keep their names and bones.
- **New bones (add-only):**
  - every zone-1 slime (klodder/kloddertje, bubbelklodder, stuiterklodder, moederklodder, druipslijm, kristalklodder, kampklodder):
    `wiebel` (child of `lijf`): the top cap, a lagging squash-and-stretch ("jiggle") in every animation.
- **Extra keyframes (add-only):** in looping animations, bones named oor*/staart*/manen*/pluim*/... that the animation did not move
  get a small lagging wiggle (`vadsrijk_t_modelkit.naloop`). Animations that already move a bone are untouched.
