# NOTITIES_ECONOMIE (Guhs 1.10.0 "Het Vadsrijk", slice economie)

Branch `110-vr-economie`, package `feature/vadsrijk/economie`, generator `tools/features/vadsrijk_economie.py`.

## What is built
- **Vadsmunten**: items `vr_item_munt_brons/zilver/goud` (guh face; right-click = into the wallet, sneak = all coins). HUD: a coin
  pop-up above the hotbar ("+12b", 3 s) when the wallet changes (own payload `vr_economie_munten`; kern's HUD still shows the total).
- **Shops** `Winkels.open(p, id)`: ids `drankjes`, `uitrusting` (plain gear per level tier 1/5/10/15), `smidwaren`
  (verbetersteentje, pluisdraad, herrolknabbel, rijfluitje), `cosmetica` (clothes + deco by post, 2 titles), `algemeen`
  (drankjes + smidwaren). All except cosmetica buy loot (`Waarde`: (8+4*level) x rarity factor 1/2/4/9/20/50 x (1+0.15/star);
  shop items: half price). "Verkoop alle grijze" button.
- **Smid** `Smid.open(p)`: stars +0..+10 (`SterRegels`: chances 95..30 %, from +3 a fail may cost one star (40 %), from +6 a fail
  may make it verpluisd (6..12 %)); verpluisd keeps everything, gives no stats until repaired (munten + pluisdraad). NEVER
  permanent loss. Reroll with Herrolknabbels (keep one bonus = 2 knabbels + double munten). `Rollen.STER_BONUS`/`STER_SCHADE`
  are set (main stat per star by class; armour verdediging + Vads; +3 kritiek at +10; damage +4 %/star, +5 % at +5, +10 % at +10).
  Screen: hammer swing on the anvil, then GELUKT fanfare / pink plof card.
- **Market** `Markt.open(p)` (SavedData `guhs:vr_economie_markt`): listings with search + filters (slot, class, rarity, level), fee 2 %
  (min 5b, not refunded), max 12 per player, proceeds wait at the marktmeester (also offline; login message), withdraw own.
  Gebonden + coins can't be listed.
- **Trade window**: sneak + right-click a player (empty hand) in the Vadsrijk, or `/guhs vadsrijk eco handel <speler>`; the other
  clicks [Ja]. Both "Klaar", then both "Ruilen!"; any change resets both; at the final confirm everything is re-validated and swapped
  in one call. Logout / leaving the dimension cancels.
- **Mounts**: Hoedhupper (mushroom-pony) + Klodderros (slime horse), GeckoLib, generated model. `Rijdieren.geefEerste(p)` (npc quest
  lv ~8: mount + rijfluitje). Saddle items `guhs:vr_item_zadel_klodderros` (for Koning Klodder's loot, bazen slice) and
  `guhs:vr_item_zadel_hoedhupper`. Level 1..10 by riding (1 xp per block, 150 x level per level), speed +3 %/lv, jump +0.025/lv.
  Poofs when you get hit, get off, leave the Vadsrijk, log out, or after a restart. Stal `Stal.open(p)`: pick, stats, new fluitje.
- **Cosmetics back home**: 5 guh clothes (`GuhClothes.VR_ITEM_*`, existing bones), 4 deco blocks (`vr_eco_mini_koning_klodder`,
  `vr_eco_opperzwam_beeld`, `vr_eco_klodderreus_knuffel`, `vr_eco_vadsrijk_vaandel`), titles (bought: Ridder van Zwamzoet,
  Vadsmuntenbaron; earned: Sterrensmid (+10), Rijdiermeester (mount lv 10), Marktkoopman (10 market sales)).
  Bought items go to the **Post** (`SpelerData.extra("economie").post`) and arrive in the normal inventory outside the dimension
  (on leaving + every 5 s at home); never through the swap. Titles are flags in `extra("economie").titels`.
- **Extra base types** (`EcoBasisTypes`, items `guhs:vr_item_<id>`): klodderzwaard (ridder), pluisboog + klodderkatapult (jager,
  soort "boog"), vadsstaf + zwamstaf (magier, soort "staf"), pluisbroek/pluissloffen, Klodderset (helm/jas/broek/sloffen, +beh),
  Zwamset (hoed/gewaad/broek/sloffen, +wijs), klodderkroon (guhddelijk head piece for Koning Klodder). Loot tables
  `economie:klodder`, `economie:zwam`, `economie:alles` for wezens/bazen.

## For other slices (ids/APIs)
- npc: `Winkels.open(p, "drankjes"|"uitrusting"|"smidwaren"|"cosmetica"|"algemeen")`, `Smid.open(p)`, `Markt.open(p)`, `Stal.open(p)`,
  `Rijdieren.geefEerste(p)` (returns false when already owned).
- bazen: Koning Klodder loot: `guhs:vr_item_zadel_klodderros` (rare) and base type `klodderkroon` (guhddelijk).
- wezens: loot tables `economie:klodder` / `economie:zwam` / `economie:alles`.

## Shared-file edits (integration please check)
- `entity/GuhClothes.java`: one marker block `<vadsrijk_economie>` (5 constants) before the `;`. Items/models/textures for them are
  written by my generator; at integration `make_resources` also picks up `CLOTHES`, `clothes()`, `icons()` from my module.
- lang nl_nl/en_us: my keys merged by `_direct()` (Dutch in both).

## Kern adapters / wishes (kern should lift)
- `EcoUtil.geefOfBewaar`: writes overflow into `SpelerData.gevonden` itself (kern's `Inventaris.bewaar` is private). A public
  `Inventaris.bewaar(p, stack)` would be nicer.
- The contract mentions `Spelers.opgeslagen(p)` but it doesn't exist; not needed (the attachment is saved with the player).
- Mount hit detection uses `Schade.NA_KLAP` (doel = player, schade > 0).
- Contract §1 says textures under `textures/vadsrijk/<slice>/`; item/block textures must be in the atlas dirs, so they are in
  `textures/item/vadsrijk/economie/` and `textures/block/vadsrijk/economie/`; mount textures `textures/entity/vr_eco_*.png`
  (GeckoLib defaulted model).

## Open choices for the user
- Prices are first guesses (potions 15-60b, steentje 50b, herrolknabbel 6z, cosmetics 2z-25z, titles 10z / 1g).
- Star chances / risk numbers (SterRegels) - tune after playing.
- Herrolknabbels are buyable (6z) as well as "rare"; could be made loot-only.
- Kern's 5 reference items still use vanilla textures (not touched by me).
- No Blockbench .bbmodel for the mounts (generated geo only); the look is simple cuboids with a guh face.

## Exploit notes
- Market: item lives in exactly one place (bag / listing tag / buyer). A server crash between autosaves could in theory roll back one
  side only (player.dat vs. world SavedData) - same as every vanilla container; accepted.
- Trade: nothing moves until both confirm; full re-validation (slot contents with `ItemStack.matches`, munten, space, distance,
  gebonden) at the swap.
- Screens: actions are only accepted for the screen the server opened for that player (`EcoPayloads.OPEN`).

## Resume point
All features built, compiled, gametests `-Pgt=VadsrijkEconomie` (see TESTEN_ECONOMIE.md). Nothing pending except tuning.

## Test results (2026-11-04)
- `runGameTestServer -Pgt=VadsrijkEconomie`: all passed (stars/never-permanent-loss incl. 300 real smid attempts, market atomicity,
  trade two-sided confirm + tamper re-validation, mount xp).
- AutoCheck (`-PautocheckScript=tools/autocheck/vadsrijk_economie.txt`, once): winkel/cosmetica/uitrusting/smid/markt/stal screens
  render (pink, readable); modelcheck shows no missing model/texture for economie ids (the listed problems are other features':
  droombed, bliksemsmidse_onweerswolkje, guhmon_badgedoos). NOT verified visually: the mount models and deco blocks in the world
  (the autocheck player stood in the void, setblock "out of this world") - check them in the dev client.
- Summoned mounts without an owner (creative /summon) now stay as decoration; whistled ones still poof after a reload.
