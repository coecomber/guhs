# NOTITIES_G_PETS (Vadsrijk fix round 2, agent G-PETS, branch 110-g-pets)

The pet system the user asked for. Built on top of `110-g-mounts` (merged first: its screen style `EcoScherm`, its key trick, its
per-player storage pattern). New package **`feature/vadsrijk/huisdieren`** (its own slice, not inside economie: pets are no economy).
Generator: `tools/features/vadsrijk_huisdieren.py` (new, appended to FEATURES after `vadsrijk_rijdieren`).

## What is built
- **Pets** (`Huisdier` record, registry `Huisdieren`): Mini Grauwe Klodderreus (`klodderreus`, boss `guhs:vr_klodderreus`), Mini
  Koning Klodder (`koning_klodder`, `guhs:vr_baas_koning_klodder`), Mini Galmwolf (`galmwolf`, `guhs:vr_z2_baas_galmwolf`).
- **Unlock:** `Haken.VERSLAGEN` -> every helper of the kill (kern contributors within 64 + the last hit) gets the pet of that boss
  type, once (owned = nothing happens). Catch-up: players whose kern first-kill key `baaskill:<type>` (SpelerData.gezien) is already
  set get the pet when they enter the Vadsrijk / open the screen / every 10 s in the dimension.
  The moment: a chat line (with the bound key) right away, then after 70 ticks (after the boss's own victory title) the title card
  "Nieuw huisdier!" + the pet's name, a pink poof (cloud/cherry/hearts) and a chime. Hook `Huisdieren.NIEUW` fires once per new pet.
  The very first pet starts walking along by itself (unless the player chose "none" before).
- **Future bosses:** one line `Huisdieren.registreer(new Huisdier(id, bossTypeId, geoModel, texturePath, animationId, scale, glowmaskOrNull))`
  + texts `vadsrijk.huisdier.<id>.naam|tekst|bron` + an animation file (idle/hop/sprong). Example for zone 3's Kruinroofvogel in
  the Huisdieren javadoc.
- **Screen** `client/HuisdierScherm` (extends the economie `EcoScherm`, own payloads `HuisdierPayloads`): "Verzameld x/y" in the
  title band, a 4-column grid of turning mini models (not owned = dark silhouette + "?", active = star), hover tooltip = name +
  flavour text + "Zo krijg je hem:" + source, click = select; right: big turning preview, name, state, flavour, source,
  "Meenemen" / "Thuislaten". Key `key.guhs.vr_huisdieren` default **P** (Vadsrijk category, rebindable). P was free in Guhs; vanilla
  binds P to Social Interactions, so in the dimension our key swallows other Guhs keys and the social-interactions key on the same
  key (same trick as the mount keys). Button "Huisdieren" in the mount screen's title band (not in stal mode).
- **Behaviour** (`HuisdierEntity`, one entity type `guhs:vr_huisdier`, look from the synced pet id): 0.4 x 0.45 hitbox, follows
  (walks beyond 2.6 blocks, faster beyond 6), teleports along with a poof beyond 12 blocks, when stuck (>3 s), in water far away or
  below the world; a happy hop now and then. Never pickable (arrows/spells/clicks go through), not attackable, invulnerable, no
  pushing/collision, not saved (`shouldBeSaved` false; respawned from the player data by the 10-tick manager).
  **Only in the Vadsrijk (default chosen):** leaving sends it home, coming back brings it back; logout removes it.
  Far teleports inside the dimension: the manager notices the pet is gone/unloaded and spawns a new one next to you.
- **No stats:** `HuisdierEffect` interface (tick + extraStats), field `Huisdier.effect`, unused.
- **Models:** the bosses' own geo + texture, the pet's own animation file with the chibi tweak as a constant bone scale:
  Klodderreus = the Koning's minion klodder model `vr_baas_kk_klodder` with a generated GREY texture
  (`textures/entity/vr_huisdier_klodderreus.png`, the blush stays pink; the boss itself is a vanilla slime model, no geo to reuse);
  Koning = crown 1.3x and wobbling; Galmwolf pup = head 1.55x, legs 0.62x (body lowered), crystals 0.55x, glowmask kept.
  Render scales 0.5 / 0.3 / 0.24 (about 0.4-0.5 blocks tall), pop-in on spawn.
- **Dev:** `/guhs vadsrijk huisdier geef <id> | alle | reset | scherm | kies <id|geen> | weg` (weg = puts the pet 20 blocks away to
  see the teleport).

## Files outside my package
- `VadsrijkFeature` + `client/VadsrijkClient`: one registration line each (G-PETS).
- `economie/client/RijdierScherm`: the "Huisdieren" button (3 lines).
- lang nl_nl/en_us (via the generator), `tools/features/__init__.py`.

## Open choices for the user
- Pets only in the Vadsrijk (default) vs everywhere.
- Default key P (vs vanilla social interactions outside the dimension).
- The Mini Klodderreus is grey (like the boss before it is cheered up); pink would be one texture swap.
- Sizes/animations: the user judges in the dev client.

## Tests / resume point
See the end of this file.
