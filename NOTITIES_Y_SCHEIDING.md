# NOTITIES y-scheiding (round Y): survival <-> Vadsrijk truly separate

Branch `110-y-scheiding` (based on 65cd32467). The full matrix (14 channels, whether each leaked, the fix and the test) is in **SCHEIDING_RAPPORT.md**. The user's client cheat-sheet is in SPIEK_Y_SCHEIDING.md.

## What I did

I added the new package `feature/vadsrijk/scheiding/`:

- **`Scheiding`** registers everything.
  - `binnen(p)`: the player is in the dimension, or the Vadsrijk inventory is live.
  - `vrijgesteld(p)`: ops (permission 2) skip the border rules.
  - `geefThuis(p, stack)`: the API for new survival gifts.
- **`Lichaam`** swaps the vanilla body like kern swaps the inventory:
  - It covers xp (level, progress, total), health, food, saturation, absorption, fire, frost and potion effects.
  - Its state lives in `SpelerData.extra("account_scheiding")`. It is ACCOUNT-wide, so a Vadsrijk character switch never moves the body.
  - It has its own side flag, so it is idempotent. It runs from `Haken.BINNEN`, at login, and every second as a safety net.
- **`Grens`** guards the border:
  - No non-player entity crosses the Vadsrijk border, either way.
  - A non-op only enters through Guhs code (stack check). Leaving is always allowed.
  - A crossing player is taken off the mount, passengers get off, and leashes are dropped.
  - FTB Essentials teleport commands and `/enderchest` are refused inside (`GEWEIGERD`).
  - No respawn point can be set inside.
- **`Menus`**: a menu of the other side closes the moment it opens.
  - Inside: ender chest, Bank Guh, Leverkist.
  - Outside: OpslagMenu, MateriaalMenu, BondBank (creative players excepted: ops/dev, and the existing Opslag/BondBank gametests open them outside).
- **`Vangnet`**: during kern's "hands empty" step, drops are cancelled and go to "gevonden" of their own side.
- **`Thuispost`** together with **`mixin/InventoryScheidingMixin`**:
  - While the Vadsrijk inventory is live, `Inventory.add` / `placeItemBackInInventory` called by Guhs code OUTSIDE `feature/vadsrijk` go to the stored home inventory (`Inventaris.legThuis`).
  - This one generic rule closes the 10 survival reward paths that could fire inside (list in the report, channel 13).
- **`VadsrijkScheidingGameTests`**: 14 tests, one per channel.
- `feature/scheidingproef/ThuisGeverProef`: a test-only helper standing in for "the rest of the mod" (it must live outside feature/vadsrijk).

## Decisions

- **Ender chest: blocked, no own Vadsrijk ender inventory.** The block was already refused inside, so only remote opens were left. Closing the menu covers every way to open it.
- **The xp swap uses a pool, not a refusal.** It is the same idea as the inventory, and enchanting/anvil inside can only use what was gained inside (normally 0).
- **The body swap stores the home body and gives it back exactly.** Before, a trip in and out was a free full heal and meal (kern keeps health and food full inside).
- **The entry guard checks for "Guhs code on the stack", not only Vadsrijk code.** Erfenis `terug` and Guhpixel bring you back to where you stood, which can be inside; refusing those would strand the player. The guard targets other mods (FTB Essentials) and non-op commands.
- **Ops:** they skip the border rules (teleports, commands, respawn) for testing. The swaps, the entity border and the menus apply to them too.
- **Survival gifts:** one generic mixin instead of patching 10+ call sites. Its exceptions are in `Thuispost.RIJK_CODE` (feature/vadsrijk, KeepOnDeathHandler, the gametest helpers).

## Shared-file changes (all marked `Y-SCHEIDING`)

- `feature/vadsrijk/VadsrijkFeature.java`: +1 line, `Scheiding.register(modBus)` (after ZonePoortRegistry).
- `feature/vadsrijk/kern/Inventaris.java`: `handenLeeg` now runs its old body (`handenLeeg0`) inside `Vangnet.tijdens`.
- `feature/vadsrijk/zone3/systemen/Premiebord.java`: `doe` returns unless the player is inside.
- `feature/vadsrijk/gilden/BondTeleport.java`: `reis` (non-dev) is refused outside.
- `feature/vadsrijk/dungeon/DungeonRij.java`: the window buttons in `actie` (after the ready-check answers) only work inside.
- `feature/einde2/appa/Momo.java`: `spelerTick` does nothing while the Vadsrijk inventory is live.
- `mixin/InventoryScheidingMixin.java` (new) + `resources/guhs.mixins.json` (+1 entry).
- `feature/scheidingproef/ThuisGeverProef.java` (new, test helper).
- Lang:
  - 4 keys `vadsrijk.scheiding.*` in nl_nl and en_us; en_us uses the English from the new chunk.
  - New chunk `tools/lang/en/c455_scheiding.json`, registered in `tools/lang/chunks.json` (prefix `vadsrijk.scheiding.`).

## Tests run

**All 81 required tests passed** (log `guhs_vadsrijk/y-scheiding-gt3.log`): the 14 `VadsrijkScheidingGameTests` plus the regression set: VadsrijkBuitenstaanders, VadsrijkKern, VadsrijkKarakter, VadsrijkParty, VadsrijkDungeon(+Nep), VadsrijkGrotKerker, VadsrijkEconomie, VadsrijkTOpslag, VadsrijkUOpslagVip, VadsrijkUGilden, VadsrijkZ3Systemen, E2Appa.

## Open points

- **The gametest server has no `guhs:vadsrijk` dimension** (log: "dimension is absent"), so the real-teleport branches of tests 4, 9 and 10 were skipped. The border is verified through its decision functions only. A dev-server `test runall` or a client check (SPIEK 5 and 6) should confirm that FTB `/home` into the Vadsrijk is refused for real.

- **Vadsmunten -> survival items by design:**
  - Mollie sells vanilla building blocks.
  - The Post delivers Knutselwinkel/Werkplaats goods, a keepsake and heart glasses.
  - The user decides whether those stay. Not changed here.
- **FTB quest rewards claimed by hand inside** go into the live Vadsrijk inventory. That is y-ftb's job (it blocks claiming inside). FTB code is not caught by `Thuispost` (it isn't Guhs code).
- **`Thuispost` assumes Guhs code outside feature/vadsrijk never means to put an item into the live Vadsrijk inventory.** A future generic Guhs helper used by Vadsrijk code would need adding to `Thuispost.RIJK_CODE`.
- **Integration should do a quick client check** of SPIEK points 1 to 5: the effect restore and the xp bar resend are not visible in gametests.
- **The Vadsrijk maps contain vanilla beds.** Clicking one inside does nothing harmful (`can_set_spawn: never`). The walkability and looks question is not this slice's.
