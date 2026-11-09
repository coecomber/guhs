# NOTITIES_X_DUNGEON: naamborden bij de dungeondeuren + w-grot restpunten + gloed stadsblubs (round X)

Branch `110-x-dungeon` from b9ef85f4c (110-w-int). Cheat sheet: `SPIEK_X_DUNGEON.md`.

## 1. Name boards over every dungeon door (user request)
New class `feature/vadsrijk/dungeon/DungeonBord.java`.
- Two `TextDisplay`s per door: the name (bold, theme colour, scale 1.7) and a smaller line under it (scale 0.95)
  `"Dungeon · %s · lv %s-%s"` (type label, `Dungeon.minLv()`-`maxLv()`, so "Dungeon · Grot · lv 11-14" for De Zwamgrot).
  Billboard `vertical`, full brightness (block/sky 15), dark background `0x99201028`, shadow, view range x2.
- **Where**: 1 block out of the door plane towards the stand spot, right over the door's top; if those 2 cells are not open:
  further out (2, 3 blocks), then lower (down to 1 above the door bottom). A hatch: 2.2 over its stand spot. The generator
  (`vadsrijk_w_grot.py`) mirrors this rule (`_bordplek`) and prints the spot per door; `--check` fails when a door has no spot.
  Spots now: zwam (-413.5, 72.05, 148.5), kristal (869.8, 123.05, -1103.7), wortel (1410.5, 72.05, -736.5), koraal
  (2231.0, 74.05, -596.0), koraal seal (2268.5, 52.05, -571.5, under water), wolk (2494.5, 207.2, -519.5), zout (3874.5, 52.05, -521.5).
- **Robust**: placed from `DungeonDeur.tick` (every 2 s for each door whose chunk is loaded, after the door itself).
  One per key through U-DUBBEL (`Dubbel.merk`/`zoek`, key `dungeonbord:<id>:<door index>:<line>`, waits for `Dubbel.klaar`).
  A copy loaded from disk is ALWAYS refused (`EntityJoinLevelEvent`, tag `guhs_vr_dungeonbord`), so an old board (an older text,
  a door that moved, e.g. zone 6 following its POI) never stays. A board not on its spot is discarded and remade. Text
  displays have no hitbox; a `/kill` comes back within 2 s.
- **Standing out (cheap)**: an invisible `minecraft:light` (level 11) in the board's cell when it is air, and a few `GLOW` particles
  on the door face (hatch: over the disc) every 2 s while a player is within 24 blocks.
- **Lanterns beside the door: NOT done (decision).** Each door sits in a different shape (round stem, tunnel, chimney, hatch, rock
  tube). Placed lanterns have collision and would need a per-door walkability check. The light block + glow + bright board
  give the same "it stands out" at night without touching the walking line.
- **Map/compass marker: none exists for dungeons/caves.** The Reiskaart only has airship places; the quest tracker
  (`VolgHud`) shows quest targets. Nothing to add a name to. Adding a new marker type would be a new feature (the brief says no).

## 2. w-grot leftovers
- **Koraalgrot sea-floor door -> a seal ("zegel").** The mouth is 11 blocks under water (sea level 62, door y51-53); a dry stand
  spot needs a map change. The chimney is the main door, so the second door is no longer a door: the same coral panels (no knocker),
  record field `DungeonDeurData.Deur.zegel` (a 9-arg constructor keeps old callers/tests working), a click gives the chat line
  "Deze ingang van De Koraalgrot is dichtgegroeid met koraal. De deur zit boven op het atol, bij de koraalschoorsteen." and NO
  window; its board says "De Koraalgrot" + "Dichtgegroeid · de deur is boven op het atol". The cave stays sealed there (leak check).
  Its `voor` remains only the outer-side reference for `binnenKant`/`GrotDungeon.bevat` (it was never a teleport target: exits use
  the main door).
- **Wolkengrotten hatch**: the 6 "closed" cells were the spiral stair's treads (polished cloud marble) crossing the hatch disc. They
  were already solid, so the seal was complete; the generator now leaves solid cells out of the hatch (22 hatch blocks + 6 treads)
  and prints that; the leak check (door cells solid, inside not reachable from the stand spot) passes.
- **Door block item**: `guhs:vr_dungeondeur` (`DungeonDeur.ITEM`, class `DeurItem`, rarity epic), only in the Guhs creative tab
  (and so JEI); no recipe, no loot, the block is unbreakable: not obtainable in survival. Tooltip 2 lines
  (`block.guhs.vr_dungeondeur.tip/.tip2`). Item model `assets/guhs/items/vr_dungeondeur.json` = the zwam door with knocker
  (written by the generator). Placed by hand it does nothing (`DungeonDeur.op` finds no door).
- **Zone-6 seal is now checked by the generator.** Finding: the zone-6 tiles leave unbuilt cells out, and in the game those ARE air
  (`VadsrijkGenerator` = void + bedrock, `Ondergrond` fills nothing there): the stair is a closed tube (crystal walls x 3872/3876,
  calcite floor, terracotta ceiling y54). So the tiles are exactly the world and the movement-graph leak check is the real proof.
  The skip is removed; `--check`: sealed (and without the door it leaks, so the check is meaningful).

## 3. Town Blubs glow
`stadsleven/client/StadslevenClient.PropRenderer` gets `BlubGloed` (a GeckoLib render layer): for a prop on the `blubguh` model it
draws the same pass as `BlubClient.GloedLaag` (`GuhRenderTypes.eyes(textures/entity/blubguh/blauw_gloed.png)`, order 1); the hidden
bones stay hidden (same bone snapshots). Not seen in a client (no client runs this round).

## Shared-file changes (marked X-DUNGEON)
- `dungeon/DungeonDeur.java` (w-grot): tick calls `DungeonBord.zorg`; `verschoven` keeps `zegel`; item + creative tab.
- `dungeon/DungeonRij.java`: `klikDeur` seal message (4 lines). `dungeon/Dungeons.java`: `DungeonBord.register()`.
- `dungeon/DungeonDeurData.java`: GENERATED (record + `zegel`).
- `grotkerker/VadsrijkDungeonGameTests.java`: new test `naambord`.
- `stadsleven/client/StadslevenClient.java`: `BlubGloed` layer (1 line in the constructor + the class).
- `tools/features/vadsrijk_w_grot.py`: seal, hatch treads, zone-6 check, `_bordplek`, item model, 6 new lang keys.
- lang nl_nl/en_us: +6 keys (en = Dutch, for the EN pass): `block.guhs.vr_dungeondeur.tip/.tip2`, `vadsrijk.dungeon.bord.onder`,
  `.bord.onder.kort`, `.zegel.bord.koraalgrot`, `.zegel.msg.koraalgrot`.
- New: `assets/guhs/items/vr_dungeondeur.json`. No tiles/generated map files changed.

## Checks
- `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_w_grot.py --check`: **alles goed** (7 entries, all seals
  incl. zone 6, all stand spots, all board spots).
- `bereikcheck.py --strikt` with `--punt` on all 6 stand spots: all `bereikt=True terug=True` (wolkengrot: the tiles have no hatch,
  so the nearest cell is the tread under it at 203.5); strikt exit 0. BEREIK_RAPPORT.md restored (not committed).
- taalcheck 0 missing; engelscheck: the dungeon keys are Dutch in en_us (expected, EN pass).
- Gametests: see the final report.

## Open points
- Not seen in a client: the board size/readability at 16 blocks, the glow sparks, the town-Blub glow (the user judges).
- Zone-6 board hangs at head height 1 block in front of the door (the stair tube is only 4 high: nothing fits over the door).
- The Kristalgrot board is 2-3 blocks before the door under the tunnel ceiling (over the door is rock).
- Light blocks placed next to a door stay if a door ever moves (harmless, invisible).
