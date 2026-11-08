# NOTITIES W-LEVEN (round W, 2026-11-08, branch 110-w-leven from 0f00119f2)

Scope: town life, critters, figuranten (u-leven-stad / u-leven-ritme / u-geroezemoes + the Z5/Z6 townsfolk props).

## 1. Pet further from its owner (~1 block more)
- The only NPC owner+pet pair in the code is `stadsleven/Straat.Wandelaar` (a town guh walking a mouse, konijntje, blue blob or duckling). Checked the rest: DraakVriendjes (critters round the pink dragon, no owner), huizen/Figuranten (the player's pets roam a garden, no leash), G-PETS `HuisdierEntity` (the PLAYER's pet; it starts following beyond 2.6 blocks already, not changed).
- `Wandelaar.AFSTAND = 2.2` (was a literal 1.2): the pet trails 2.2 blocks behind along the guh's path.
- The pet now spawns 2.2 blocks beside its guh on walkable ground (`naast`), and the trail starts at the pet. Before, the pet snapped onto the guh's own spot at the first tick.
- `Wandelaar.MIN_AFSTAND = 1.2`: when the guh turns back along its own trail it used to walk right through its pet (the gametest caught 0.4 blocks); now the pet steps aside to 1.2 blocks.

## 2. Fewer guhs/critters on the Knuffelkamp pier (and every pier/jetty/alley)
- Cause: town life picks its spots round the PLAYER (`Plekken.rond`, radius 34-56). On a pier that sticks out over the sea, almost every standable cell in that circle is pier, so everything piled up on it.
- Fix in `Plekken.rond` for GROND/GRAS/ZIT spots (generic, so it covers De Haven, Lampionhaven, the Kandijkom dok, the Knuffelkamp jetty...):
  - **narrow spot** (`Plekken.smal`): of the 8 points 3 blocks round it, at most 3 are walkable ground at about the same height. That is true for the middle of a 3- or 5-wide pier, false for a square or a 7-wide street. A narrow spot is only used with 35 % chance AND when no other town-life entity is within 10 blocks.
  - **anywhere**: no new scene where 4+ town-life entities are already within 6 blocks.
  - at most 64 candidates are handed out per search (the scenes take the first one that fits).
- The town stays as lively: the caps did not change; the scenes simply go to the squares and lawns.
- Only stand-still NPCs on the pier are the luchtschip captain + bell (quest/travel), not touched.

## 3. Pieppiepmuisjes ~30 %, Kandijkom unchanged
- Per-town multiplier: `Steden.Stad.muizen` (record field). `Steden.MUIZEN_STANDAARD = 0.3`; `Steden.MUIZEN` map has `kandijkom -> 1.0`.
- The DIERTJE cap (9 at VOL) is split (`Beestjes.blobMax` / `muisMax` / `volgendeIsBlob`):
  - blobs keep their old share: round(9 x 0.3) = 3;
  - mice = round(9 x 0.7 x multiplier): 2 everywhere (was ~6), 6 in the Kandijkom (as before). Regular and pink alike (1 in 4 pink, as before).
  - ZACHT: blobs 2, mice 1 (Kandijkom 3).
- `StadsLeven.maak(DIERTJE)` now counts the town's mice and blobs (`StadsLeven.diertjes`) instead of a 30 % coin.
- Walkers' pets: a mouse pet is only kept with the town's multiplier (`Wandelaar.huisdier`), so mouse pets are also ~30 % as common outside the Kandijkom.
- Not changed: the pink mice round the pink dragon (DraakVriendjes, 3 of 32, only while he visits) and the zone-3 boss-scene mice.

## 4. Figuranten: fewer, and the facing bug
### Fewer
- `Z5Bewoners.blijft(b)`: plain townsfolk (soort 0/1) show at `DICHTHEID = 0.75`, inside the cathedral at `DICHTHEID_KATHEDRAAL = 0.4`. Knights (soort 2) and named-looking figures (soort 3) always stay (formations would get holes).
- The choice is a fixed hash of the position (`zone5/wereld/Dichtheid`): the same for everybody, no flicker, independent of the list order (a regen does not reshuffle).
- Cathedral box = from `kathedraal_apsis` - 6 to `kathedraal_poort` + 1, +-34 in z, y poort-4..+60 (POIs, so it follows a regen).
- Zone 6 (125 figures, Spiegelzaal mirror images) is NOT thinned: the mirror images would no longer match their originals, and the user's complaint was Hoogvads.
- Works at runtime, no regen needed.

### Facing (root cause, generator)
- The render kits draw a figure with yaw a facing (-sin a, -cos a): 0 = north, 90 = WEST (`mcextra.affine`, counter-clockwise from above). Minecraft yaw Y faces (-sin Y, cos Y).
- **Zone 5** is the kit turned 180 degrees (x -> GX0 - x, z -> GZ0 - z), so the game yaw must be Y = -a. `bouw5.bewoners` used a + 180: right for east/west figures, 180 degrees wrong for everybody facing north/south. Now `bouw5.spel_yaw` (also `kit/export.figuren`).
- **Zone 6** is in game coordinates (not turned): Y = 180 - a. `poi6.verzamel` used the raw a (same error). The mount figures (`rijdieren`) used a + 180 (wrong for east/west), now 180 - a too.
- On top of that some kit figures were authored with a compass yaw (90 = east), wrong in the concept render as well. Fixed at the source:
  - `zone5/kit/kathedraal.py`: churchgoers 90 -> 270 (face the altar), choir 270 -> 90 (faces the nave). This was the user's "180 graden verkeerd": the old export happened to keep their (wrong) east/west yaw;
  - `zone5/kit/wijken.py`: palace crowd kijk 270 -> 90 (faces the queen), kazerne duel yaws swapped (face each other);
  - `zone6/kit/saffraanpoort.py`: the golden guh on the throne 270 -> 90 (faces the hall);
  - `zone6/kit/zijfiguren.py`: bultguh/carpet riders used (180 - yaw) to compensate the raw export; now the rider's kit yaw = the mount's.
- **Verified by computing yaw vs. look target** with the new checkers (they build the world without writing tiles):
  - `python tools/vadsrijk/zone5/kijkcheck5.py` (~30 s): churchgoers -> altar 74/74 (avg 18 deg; before 0/74, avg 162), choir -> nave 7/7 (before 0/7), palace knights -> aisle 8/8 (before 0/8), palace crowd -> queen 6/6 (before 0/6), owl students -> professor 12/13 (before 6/13), kazerne formation/drill master/queen unchanged (were right), duel 2/2.
  - `python tools/vadsrijk/zone6/kijkcheck6.py` (~2 min, ~1 GB): Spiegelzaal court -> aisle 10/10 (before all facing the walls), throne -> hall 1/1, riders = mount 27/27.
- **Generated outputs NOT committed** (Z5WereldPlekken.java / Z6WereldPlekken.java / POI_*.md / tiles). The yaw fix reaches the game only after integration regenerates zones 5 and 6 (`maak_kaart.py --zone 5` and `--zone 6`; blocks do not change, only the BEWONERS/RIJDIEREN packs). Run both kijkchecks after the regen.
- Other figure systems checked: Z3Figurant boss scenes (yaw computed in Java from targets), stadsleven guhs (Java), Z5 NPCs (t-z5verhaal, own data). Zone 1-4 maps export no figures.

## 5. The blue town critters (for w-modellen / integration)
- Entity: **`guhs:vr_sl_prop`** = `stadsleven/LevenProp` (a generic GeckoLib prop), renderer `stadsleven/client/StadslevenClient.PropRenderer`.
- Model/texture/anim: `Beestjes.BLOB_MODEL = "vadsrijk/wezens/klodder"` (the zone-1 Vadsrijk klodder geo + its animations), texture `Beestjes.BLOB_TEX = "textures/vadsrijk/stadsleven/blauwe_klodder.png"` (the klodder's happy face recoloured light blue, written by `tools/features` `vadsrijk_u_leven_stad`), scale `BLOB_SCHAAL = 0.5`; anims used: `idle`, `hop`, `blij`.
- Used in two places: `Beestjes.Diertje` (the free-roaming critters) and `Straat.Wandelaar.HUISDIEREN` (the blue blob pet, scale 0.42, anims idle/hop).
- So w-modellen must change `BLOB_MODEL` / `BLOB_TEX` (and maybe the anim names "hop"/"blij" + scale) in `Beestjes.java`, and check that the Wandelaar row picks it up (it reuses the constants). I did NOT touch the model, renderer or these constants. My edits in `Beestjes.java` are below the constants (new methods after the constructor), so a merge should only need a glance.

## Shared-file changes (all marked W-LEVEN)
- `stadsleven/Steden.java` (record field + constants), `Beestjes.java` (3 methods), `StadsLeven.java` (DIERTJE case, `diertjes`, `druk`), `Straat.java` (Wandelaar), `Plekken.java` (crowding filter), `StadslevenGameTests.java` (helpers private -> package, `testStad()`).
- `zone5/wereld/Z5Bewoners.java` (density), new `zone5/wereld/Dichtheid.java`, `Z5BewonersGameTests.java` (+1 test).
- tools: `zone5/bouw5.py`, `zone5/kit/{export,kathedraal,wijken}.py`, `zone6/poi6.py`, `zone6/kit/{saffraanpoort,zijfiguren}.py`, new `zone5/kijkcheck5.py`, `zone6/kijkcheck6.py`.

## Tests
- New `WLevenGameTests` (`-Pgt=WLeven`): `muizenFactor` (caps per town; 40 passes stay under the mouse/blob caps for a 0.3 and a 1.0 town), `huisdierAfstand` (pet ~2 blocks, never further than 2.5, never bumps into its guh), `smalleSteiger` (pier narrow / lawn not; with the player on the pier at most 5 entities on it).
- `Z5BewonersGameTests.dichtheid` (cathedral ~40 %, elsewhere ~75 %, knights/named always).
- Existing `-Pgt=Stadsleven` re-run (the crowding filter and the split cap must not break the budget test).
- Results: `-Pgt=WLeven,Stadsleven,Z5Bewoners`: all passed except `huisdierAfstand` (pet walked into, fixed with MIN_AFSTAND); rerun `-Pgt=WLeven,Stadsleven`: all 8 passed. `kijkcheck5.py` / `kijkcheck6.py`: OK.

## Open points
- The facing fix needs the zone 5 + 6 regen at integration (not verifiable in-game before that).
- Looks (pier density, mouse count, cathedral density) are for the user in the dev client; the knobs are `Plekken.SMAL_KANS/SMAL_VRIJ/DRUK_*`, `Steden.MUIZEN*`, `Z5Bewoners.DICHTHEID*`.
- Critters that already exist can still dash onto a pier for a moment (they wander 2-6 blocks per dash).
