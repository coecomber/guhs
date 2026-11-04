# NOTITIES_FIX_MOMENTEN — fix round 1 (2026-11-04), agent MOMENTEN

Branch `110-fix-momenten`, worktree `guhs_vadsrijk/f-momenten`, from 6ec8724ca. Ideas 18 (travelling caravan) and 30 (recolour
wave at `zone1_klaar`) of `guhs1100-avontuur/VOORSTEL_EPISCHER_ZONE1.md`.

## New package `feature/vadsrijk/momenten` (ids `vr_mom_*`, lang `vadsrijk.momenten.*`, payloads `guhs:vr_momenten_*`)
| class | what |
|---|---|
| `MomentenRegistry` | entities, item, listeners, payloads, dev commands `/guhs vadsrijk momenten ...` |
| `GolfBaan` | pure timing of the wave front (shared server/client) |
| `Golf` | server: `NpcApi.ZONE1_KLAAR` -> client keeps scars grey (`GolfKomt`), 275 ticks later (after the 260-tick cliffhanger) the scene + `GolfStart` |
| `client/GolfClient` | client: per-position scar tint via the wereld hook, re-meshes the sections the front crosses, pink particle wall near the camera, creatures/guhs cheer (hearts, notes, squeak) |
| `KaravaanRoute` | the road (corners from an A* over the real map tiles, see below) + stops haven / kustpad / zwamzoet / sporenwoud |
| `KaravaanStaat` | pure state machine STOP / RIJDEN / AANVAL (gametested) |
| `Karavaan` | server manager: logical position always travels; the cart + trader exist only when a player is within 112 blocks; shop rotation; attacks + rewards |
| `KaravaanBeloning` | pure reward split (gametested) |
| `KaravaanSlak` | the cart entity (plain Entity + GeckoLib, noSave, never hurt, click = shop) |
| `HandelGuh` | Kruimel, a `RijkGuh` subclass riding the cart (guh model via `GuhRenderer`), click = shop |
| `RegenboogDrankje` | the rare potion `vr_mom_regenboogdrankje` (full Vads/energy + 90 s Vahoeg, Snelpootjes, Knuffelschild) |
| `VadsrijkMomentenGameTests` | 7 focused tests (`-Pgt=VadsrijkMomenten`) |

Generator: `tools/features/vadsrijk_momenten.py` (uses `vadsrijk_wezens.Model/schilder`): `geckolib/.../vadsrijk/momenten/karavaan.*`,
`textures/vadsrijk/momenten/karavaan.png`, the potion icon + item model, 26 lang keys (nl = en). Run standalone:
`PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_momenten.py`.

## Hooks outside the package (all marked `MOMENTEN-HOOK`)
- `VadsrijkFeature.register/payloads` and `client/VadsrijkClient.init`: one line each.
- `wereld/client/WereldClient`: `public static volatile Predicate<BlockPos> golf` + `Tint.colorInWorld` uses it when set
  (null = the old flag behaviour). Owner wereld: please keep it when touching the tint.
- `economie/Winkels.registreerExtra(Winkel)`: public way to add/replace a shop (the caravan re-registers on every stock rotation).
- `tools/features/__init__.py`: `vadsrijk_momenten` appended to FEATURES.
- Lang key `vadsrijk.economie.winkel.karavaan` (shop title; economie's screen builds the key from the shop id) is written by my generator.

## The wave (idea 30)
- Flow: npc `Verhaal.cliffhanger` -> `NpcApi.zetZone1Klaar` -> `ZONE1_KLAAR` -> `Golf.plan`: `GolfKomt` (client: every scar grey,
  even when wereld's 2 s flag check already sent `Vlag(true)`), after 275 ticks `Golf.speel`: `RijkScene` "momenten:golf" (470 ticks,
  non-click, title "Het Vadsrijk kleurt weer" / "De littekens van de draak zijn geheeld" at tick 385) + `GolfStart`.
- Front (`GolfBaan`): from the Zwamtempel voorhal; 0-60 t fast to 120 blocks before the player, 60-200 t slow (240 blocks) past the
  player, 200-400 t on to r = 1450 (the far SW scar ends). Camera keyframes relative to the player (behind, looking at the temple;
  sees it come; turns via the side; follows it away and up); every keyframe is kept 6 blocks above the heightmap.
- Client cost: sections re-meshed only when the front crosses a chunk's near edge / middle / far edge (y sections 2..13).
- Ends healed: at the end the hook is cleared and the flag (true) applies. The dev command also sets the flag.
- Not saved: if the player logs out between the flag and the wave, there is no wave (the scars are simply healed next login).

## The caravan (idea 18)
- Road: A* over the real tiles (surface below y 100, steps <= 1, 3 wide, path blocks cheaper), simplified to 34 corners, ~800 blocks.
  Haven plein -> map path "haven" north -> **east shore** (the map path's last stretch (80,180)->(10,100) ends in the bay: water) ->
  the city quay (z 84) -> past the plein (stop by (1,64,70)) -> map path "woud" -> `kampvuur_sporenwoud_1`. Tools to redo it:
  the scratch scripts were `nbtlees.py` (NBT reader), `oppervlak.py`, `astar.py` (not committed; easy to redo if the map moves).
  **If integration moves the city/haven/paths, re-check the corners** (`KaravaanRoute.HOEKEN`). The cart follows the floor itself
  (nearest standing spot within 5 of the road y, smoothed 0.15/tick), so small terrain changes are fine.
- Speed 0.1 block/tick (~6.7 min one way), stops 60/30/75/60 s. State is static (not saved): after a restart it starts at the Haven.
- Shop `karavaan` (doesn't buy): always Regenboogdrankje (250) + Herrolknabbel (450, smid 600), 2 of the 5 Vadsrijk guh clothes
  (250, by post), 1 of {a deco piece by post (600), verbetersteentje (40)}; rotates every 20 min (seeded, the same for everyone).
- Attacks: first after 10-30 min, then every 20-30 min, only while driving, a player within 40 blocks, not in a town box and not
  within 24 of a stop. 4-8 creatures (3 + players nearby + 0..2): Klodder/Stuiterklodder/Bubbelklodder lv 2-5 east of x -60,
  Hoedjesbol/Sporenpuffer/Mosgeest lv 6-9 in the west. They walk to the cart when they have no target. Tag `vr_mom_karavaan`
  (leftovers from an old attack are refused when their chunk loads). Won (all cheered up) -> rewards; 2 min -> they poof away,
  helpers so far get their share. Nobody near any more -> the attack quietly ends.
- Rewards (`KaravaanBeloning`): helper = in kern's helpers of at least one defeated attacker; (60 + 15 x attackers) x share
  cheered up + 5 per own attacker; all cheered up: 25 % rare item (Regenboogdrankje / Herrolknabbel / 3 verbetersteentjes).
  Thank-you line (3 variants) + coin line. After 3 helped attacks: title **Karavaanvriend** (`vr_karavaanvriend`, titles feature).

## Tests
- Gametests `-Pgt=VadsrijkMomenten`: **7/7 passed** (route, round trip + stops, attack trigger/give-up/where, reward split,
  reward + title arrive on a mock player, wave timing, cart + trader can't be hurt / ride / not saved).
- AutoCheck round 1 `tools/autocheck/vadsrijk_momenten.txt` (caravan + first wave camera): cart + Kruimel at the Haven, on the
  shore road, at the Sporenwoud edge; the shop screen (rotating stock); a forced attack (attackers spawn, Kruimel's help line).
  Its `/damage` line failed (selector); now `execute as ... run damage` (not re-run). Rewards are covered by the gametests.
  The first wave camera (relative to the eye height) looked into the order temple / the sky -> rewritten.
- AutoCheck round 2 `tools/autocheck/vadsrijk_momenten_golf.txt` (wave only): at the fossil bridge the camera gives good
  overviews of the ravine and the scar strip; at the order temple the late frames still looked up past the caps -> look targets
  are now clamped to y <= 98 (compiled, not re-shot). **The user should judge the wave in the dev client**
  (`/guhs vadsrijk momenten golf grijs`, then `/guhs vadsrijk momenten golf`, or `golf plan` for the real 14 s delay).
- Known, not mine: economie's WinkelScherm overlaps the price with "Komt per post..." on POST rows (also in the cosmetica shop).

## Open / choices for the user
- Kruimel/Slakkie names, prices and the reward sizes are my proposal.
- No waypoint/marker on the map for the caravan (could come from the questtracker later).
- The wave's camera is relative to where the player stands when it starts (normally the Grootmeester in the ordetempel).

## Resume point
Done for this round (see the commit on `110-fix-momenten`). Next: the user's look at the wave camera and the snail in the dev
client; tune the camera keyframes in `Golf.scene` if needed. Integration: merge the four `MOMENTEN-HOOK`s and run
`vadsrijk_momenten.py` once (lang keys).
