# NOTITIES_G_UI (fix round 2, second playtest): jewellery, tooltips, skill icons, skill info

Branch `110-g-ui`, worktree `guhs_vadsrijk/g-ui`, from fba766b79.

## What changed

| Feedback | Fix | Files |
|---|---|---|
| Unclear how to wear jewellery | Jewellery tooltip line "Rechtsklik om te dragen (of Shift+klik in je inventaris)." The empty-slot text names shift-click too. | `client/UitrustingTips` (new), lang |
| Worn jewellery showed no stats on hover | **Cause:** the 3 slots were drawn in `ScreenEvent.Render.Post`, which fires *after* `extractDeferredElements`, so `setTooltipForNextFrame` was never drawn. They are now drawn in `ScreenEvent.Render.Foreground` (inside `AbstractContainerScreen.extractRenderState`, before the deferred tooltip). Hover = the full item tooltip + "Klik om af te doen." | `kern/client/SieradenScherm` |
| Shift-click did not equip jewellery | The client catches shift-click on a jewellery item in the player inventory (dimension only) and sends `KernPayloads.SieraadShift(index)`. The server `Sieraden.draagUitInventaris` -> `wisselMetInventaris` puts it in its slot; a worn piece swaps back into that inventory slot. The level gate applies. Clicking or shift-clicking the worn slot still takes it off (`af` -> `afZonderCheck`). | `kern/Sieraden`, `kern/KernPayloads`, `kern/client/SieradenScherm` |
| Skill icons | `tools/vadsrijk/maak_skill_iconen.py` draws 13 icons (32x32 pixel art: a class-coloured badge plus a symbol with a dark outline) in `textures/gui/vadsrijk/skill/<skill id>.png`: 3 classes x R/V/G/Z + knuffelsprong. HUD: an 18 px icon in the slots and 16 px in the sprong slot; a fallback to letters or the old drawing when there is no texture (`RijkHud.skillIcoon`, which handles new skills from g-klassen). Tree screen: the skill list shows the key badge, the icon and the name, and now also lists the Knuffelsprong (C). | `kern/client/RijkHud`, `klassen/client/BoomScherm` |
| Skill hover info | `klassen/client/SkillInfo.tip`: name + key, energy (or "volle ultieme lading") + cooldown, the base explanation, then "Verbeteringen" lists every node and advancement step that changes this skill: ✔ green + white text if learned, ✖ dark grey if not. Tree nodes get "Verbetert: <skill>". **HUD hover:** while the inventory is open, hovering a HUD skill slot shows the same tooltip (`RijkHud.skillVak/skillId` + a `Render.Foreground` listener). | `klassen/client/SkillInfo` (new), `BoomScherm`, `KlassenClient`, `RijkHud` |
| Kristalhelmpje not in the Kristalset | **Intended, not a bug.** The Kristalhelmpje is a zone-1 *loose piece* (tier 3, `WezensLoot`: "loose pieces"). The zone-2 Kristalset is kristalkroon/-borstplaat/-beenstukken/-schoentjes (`Z2Sets.KRISTALSET`, NOTITIES_Z2_WEZENS). Its tooltip now says "Los stuk: hoort bij geen set." | `client/UitrustingTips` |
| Set membership on all gear | Set pieces: "Set: Kristalset (2/4 gedragen)" plus a line listing all 4 pieces (worn ones green) above the bonus lines. Every armour piece outside a set: "Los stuk: hoort bij geen set." | `client/UitrustingTips`, one line each in `wezens/client/WezensClient` and `zone2/wezens/client/Z2WezensClient`, lang |

Lang: new or changed keys are in `tools/features/vadsrijk_kern.py`, `vadsrijk_klassen.py`, `vadsrijk_wezens.py` and `vadsrijk_z2_wezens.py`, and are also patched directly into nl_nl/en_us.json (the same Dutch text in both).

## Outside my own files (small, additive)
- `wezens/client/WezensClient`, `zone2/wezens/client/Z2WezensClient`: one line each (the set piece list).
- `dev/AutoCheck`: a new `gui inventaris` command (opens the inventory screen for hover shots).
- `client/VadsrijkClient`: registers `UitrustingTips`.

## For g-klassen (class rework)
- `SkillInfo.VERBETERINGEN` is a fixed table, skill id -> node ids. If skills or nodes are renamed or moved, update it there. Unknown nodes are filtered out via `Boom.knoop`.
- The HUD finds the icon from the skill id in the HUD lang key (`RijkHud.skillId`). A new skill only needs a `<id>.png` (add it to `maak_skill_iconen.py`).
- The advancement unlocks are not skills of their own (they change existing skills), so they get no icon. They show as improvement lines in the skill tooltips (e.g. Kussenkampioen under Kussenklap).

- **HUD hover and learned nodes:** `SpelerData` (`boom`) is NOT synced to the client, so `Spelers.heeftKnoop` on the client always returns false. The HUD tooltip uses the node list the tree screen last received (`SkillInfo.bekend`, set by `BoomScherm`). Before the tree has been opened in a session, it shows a hint "Open je skillboom (K)...". **Suggestion for g-klassen/kern:** put the class's node ids in `KernPayloads.Hud`; then `SkillInfo` can read them directly.

## Tests
- Gametest `VadsrijkKernGameTests.sieraadShiftKlik` (`-Pgt=VadsrijkKern`): **8/8 passed**.
- AutoCheck `tools/autocheck/vadsrijk_g_ui.txt` (11 shots, `run/screenshots/autocheck`). Checked:
  - worn ring hover shows the full tooltip + "Klik om af te doen";
  - the earring tip shows the how-to line;
  - Kristalhelmpje shows "Los stuk";
  - Kristalkroon shows "Set: Kristalset (1/4 gedragen)" with the borstplaat in green;
  - the HUD icons and the HUD R hover tooltip work;
  - tree: the skill list has icons, Kussenklap hover shows Vuurkussen and Kussenkampioen in green with ✔ and the rest grey, and the node tip says "Verbetert: Kussenklap".

  The only problems in the run were 3 script `/tp @e[type=item]` lines that found nothing (the loot had already been picked up).
  The HUD-hover shot was taken before the `bekend` fix (Kussenkampioen was still grey there); after the fix, the code was only compiled, with no new client run.
- Shift-click itself only ran in the gametest (server logic). The client intercept (shift + `getHoveredSlot`) has not been clicked by hand: **test in the dev client**.
