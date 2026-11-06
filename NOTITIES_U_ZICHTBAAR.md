# NOTITIES U-ZICHTBAAR (round U, 2026-11-06)

Branch `110-u-zichtbaar` (worktree `guhs_vadsrijk/u-zichtbaar`, from eaee06d08). Feedback: "the cape (z3npc klaarzetten) is on but
not visible; make as much as possible visible for yourself and others".
Code: `feature/vadsrijk/zichtbaar/` (+ `client/`). Generator: `tools/features/vadsrijk_u_zichtbaar.py` (last line in FEATURES).
Tests: `VadsrijkUZichtbaarGameTests` (`-Pgt=VadsrijkUZichtbaar`, 3 tests). Cheat sheet: `SPIEK_U_ZICHTBAAR.md`.

## What was built
- **Sync** (`Zichtbaar`, `ZichtbaarPayloads.Uitrusting(uuid, List<String>)`): per player the item ids of the 7 virtual slots in
  `Slot.SIERADEN` order ("" = empty; ALL "" outside the Vadsrijk).
  - Sent on change: every 10 ticks each player's list is compared with the last list sent; a difference goes to the player + all trackers
    (`sendToPlayersTrackingEntityAndSelf`). Polling instead of hooks: it catches every way the slots change (Sieraden, smid,
    t-karakter character swap, dev commands, dimension change) without touching kern.
  - Sent on start tracking (`PlayerEvent.StartTracking`): the last list sent, or the current one.
  - Login/logout forget the last list, so the next poll sends again.
- **Cape**: through the VANILLA cape layer (cloth physics, no extra geometry): the render-state modifier swaps the skin's cape texture for
  `textures/vadsrijk/zichtbaar/cape_<naam>.png` and sets `showCape`. T-KLEDING's rule: a cosmetic cape replaces it, wings hide it
  (`Looks.statCapeTonen`, reads `KledingCache.kaap/vleugel`). Elytra still win (vanilla). Same trick as t-kleding's cosmetic capes.
- **ZichtbaarLaag** (render layer on both player renderers, wide + slim): gloves on both lower arms (+cuff), armband on the left wrist,
  ring 1 right hand / ring 2 left hand (thin band + stone), necklace (V chain + pendant) on the chest, earrings on both sides of the head.
  Cuboid item models (same technique as t-kleding's wings/hats). Pushed outward when a chest piece / helmet is worn.
  Slim arms: moved to the 3-px arm centre and narrowed.
- **Looks**: 9 capes, 9 gloves, 7 armbands with their OWN look (all 17 t-items pieces + the zone-4/5 capes/gloves/armbands); every other
  piece of those slots gets a "standaard" look. Rings/necklaces/earrings: one generic gold model each, the gem coloured by dye tint from a
  keyword in the item id (`Looks.kleur`: kristal = cyan, honing = amber, ...; unknown = ruby).
- **Budget/LOD** (`Looks`): cape always (vanilla, ~free); gloves to 48 blocks, necklace/armband to 32, rings/earrings to 16; at most 12
  OTHER players per frame get the small parts (counter reset on `RenderFrameEvent.Pre`); you always do.
- **Checked, unchanged**: the 4 Vadsrijk armour pieces are real armour slots with equipment assets (`KernRegistry.harnasEigenschappen`),
  so vanilla draws them (and t-kleding's transmog changes them); the weapon is the main-hand item and draws normally.
- **Dev**: `/guhs vadsrijk zichtbaar aankleden [cape] [handschoenen] [armband]` | `uitkleden` | `status`.

## Decisions made alone
- Cape through the skin cape texture instead of an own model: free physics, one look rule shared with t-kleding, zero FPS cost.
- The player's own "cape" skin toggle is overridden in the Vadsrijk (t-kleding does the same for cosmetic capes).
- Rings ARE drawn (tiny band + stone, only within 16 blocks); earrings on both ears; one armband on the left wrist.
- No "hide my stat gear" toggle (not asked; the wardrobe's cosmetic cape/wings already cover the back). Possible later.
- First-person hands do NOT show gloves (layers aren't used for the first-person arm; would need a RenderArmEvent hook). F5/others only.

## Shared-file changes (add-only, marked U-ZICHTBAAR)
- `VadsrijkFeature.java`: `ZichtbaarRegistry.register(modBus)` + `ZichtbaarRegistry.payloads(registrar)`.
- `client/VadsrijkClient.java`: `ZichtbaarClient.init(modBus)`.
- `tools/features/__init__.py`: `'vadsrijk_u_zichtbaar'` at the end of FEATURES.
- New assets (generated, committed): `items/vr_u_zichtbaar_*.json`, `models/item/vr_u_zichtbaar/`, `textures/item/vr_u_zichtbaar/`,
  `textures/vadsrijk/zichtbaar/`. No lang keys (dev command text is literal Dutch).

## Open / not verified
- NOT looked at in a client (no AutoCheck run): positions of the small parts are computed from the player model boxes with the same
  transform t-kleding verified for its hats; the user judges the looks in the dev client (F5). Tune numbers in the generator
  (`handschoen_elementen` etc.) and the push-out factors in `ZichtbaarLaag.submit`.
- A future cape/glove/armband id gets the "standaard" look until it is added to the generator + `Looks` sets (the lookRegels test only
  forces the 17 t-items pieces).
