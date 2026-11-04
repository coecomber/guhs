# NOTITIES_J_INT: Vadsrijk round 3 (third playtest), integration (branch 110-j-int, worktree guhs_vadsrijk/j-int)

Base: ab6d952ac (110-h-melee, the user's current test client). One merge commit per branch. Never gradle in h-melee / g-int /
f-int / w-poc. build/moddev copied from h-melee (artifacts only). Scratch: scratchpad/jint (logs, langcheck.py with base ab6d952ac).

## Merged
| branch | head | notes |
|---|---|---|
| 110-i-ui | 6e5c813a7 | clean |
| 110-i-voltooiing | 5bbc7af29 | clean (already contained i-ui) |
| 110-i-mounts | 157c9c985 | lang 3-way (scratchpad gint/lang3.py). Book follow-up, see below |
| 110-i-z2kaart | 737b92773 | clean (generator sources only) |
| 110-i-audio | a115b99de | VadsrijkFeature / VadsrijkClient / tools/features/__init__.py: both lines (voltooiing + stadsmuziek); lang 3-way |
| 110-i-lore | 45b1a2dee | lang 3-way; vadsrijk_npc.py + vadsrijk_z2_npc.py rerun = identical lang; make_ftbquests rerun = no change (lore at 60, we stay 61). No map generator touched -> no map regen needed after it |

## Waiting for
NOTHING: all 6 branches merged (ui, voltooiing, mounts, z2kaart, audio, lore).

## Cross-cutting decisions
- **Verkenningsboek mounts** (i-voltooiing listed Hoedhupper + Klodderros): zone 1 = Wolkenschaapje + Knuffelhert, zone 2 = Guhwolf.
  Because mounts only exist from zone 3, the category is a **"later" category**: `Voltooiing.Categorie` got a `later` flag (old
  4-arg constructor = false). Later categories are shown ("Rijdieren (later)", greyed, hover tip) with their own x/y, but are
  skipped in `tel(p, zone)` and in the zone total of the payload, so they never block 100 % or the milestones. Hint texts:
  `hint.rijdier.wolkenschaapje` new, hoedhupper/klodderros removed (also from lang by hand: the generator only adds).
- **Keys** (all Vadsrijk keys, category Vadsrijk): B book, C sprong, G/R/V skills, Z ult, J questlog, K skilltree, N mounts, P pets,
  U losknuffelen, X ride, 2x unbound (VolgHud). No duplicates among them. Other Guhs keys on the same key: snuffel B (blaf), N
  (boekje), R/V/Z, maag G: in the dimension each Vadsrijk key swallows same-key Guhs-category clicks (book B, RijdierClient X/N,
  HuisdierClient P (+ vanilla social interactions), KernClient skills). Snuffel keys only act as a dog on Het Snuffeleiland
  (another dimension), and Vadsrijk keys do nothing outside the Vadsrijk. Vanilla: X/C = creative hotbar load/save activators
  (creative only; pre-existing, not changed). Verdict: no clash to fix.
- **Generator bug fixed:** `vadsrijk_z2_npc.py` `_mijn_sleutel` dropped `vadsrijk.npc.rijdierquest.teaser.z2_rijdier_guhwolf`
  (written by vadsrijk_npc.py since i-mounts) on a standalone run -> excluded the teaser prefix.
- **Giant-tree landmark** (lead's request): `WereldLucht.VERTE_PLEK` index 2 (lucht_verte_2 = reuzenboom = zone 3) from azimuth 255
  (west) to **105** (east). Not 95: the floating castle at 75 is +-14 deg wide and the tree +-15 deg, so 95 would overlap it; 105
  sits between the castle and the volcano (140, +-17 deg). WERELDVERHAAL.md §12 still says "~95" (not my file).
- Zone-2 cliffhanger east: taken from i-lore (Z2NpcPlekken.CLIFFHANGER mirrored). The zone-3 npc slice may do the same: keep one.
- FTB: CHAPTER_VERSION 61 (i-mounts changed the rijdier_hoedhupper quest into the Wolkenschaapje return quest).

## Log
- Compile after all merges: OK.
- Regen `maak_kaart.py --zone alle --geen-cache --audit`: zone 1 tiles unchanged, zone 2 95 tiles changed (bridges/dock/gates).
  Audit: kuil 6 / trap 14 / stap 5 / water 5 / deur 2 = the known set. New POIs dok_kiosk / dok_welkom / dok_bankje.
- loopcheck zone 2 box -300,-2820,300,-2150: TRAP 46, VAL 28 = exactly i-z2kaart's result (+4 TRAP = bench/kiosk seats,
  +1 VAL = snow_block artefact at brug_2 north).
- controleer_verticaal: Druipschacht 0 traps; Hoedenpaden 6671 = known sea-edge artefact.
- npc generators rerun: lang content identical (order only) after the teaser fix.
- make_ftbquests with 61: OK (every chapter file changes because of the version).

## Resume point
See the bottom of this file (updated at the end).
- After the lore merge: make_ftbquests again = no change.

## Gametests (`-Pgt=Vadsrijk,Z2Npc,StadsMuziek`, 97 tests)
- Run 1 (before lore): 2 failed: `VadsrijkVoltooiing.telling` "mounts per zone" (expected 3+1: the test still knew the
  Hoedhupper/Klodderros) -> test updated to 2 + 1 and checks that the later category is not in the zone total; and
  `VadsrijkWezens.grotSpawnerVindtDeVloer` "spawned".
- Run 2 (final head, after lore): 94/97. Failed: `opperzwamFasen`, `koningVecht`, `opperzwamVecht` (boss timing under machine load:
  zone-3 agents + gradle build-cache lock contention; the first run passed them) -> rerun `-Pgt=VadsrijkBazen,VadsrijkWezens`:
  every boss test passed (16 tests), only `grotSpawnerVindtDeVloer` failed again -> `-Pgt=VadsrijkWezens` alone: **7/7 passed**.
  So: grotSpawner is order-dependent (its spawn is vetoed by `WezensSpawners.npcInBuurt` = any GuhEntity/Kampklodder within 12,
  or a leftover from an earlier batch near its spot; my guess, not proven). Not caused by round 3 (no spawner/wezens code changed).
- Note: the gradle build cache (~/.gradle/caches/build-cache-1) can time out on its lock with many agents: use `--no-build-cache`.

## AutoCheck
- SKIPPED: the machine is under load (zone-3 agents running, ~9 GB free, build-cache lock contention). No walk ran this round.

## Run dir
- run/ copied from h-melee (config incl. the vadsrijk balans/client/sfeer toml, defaultconfigs, mods, resourcepacks, options.txt,
  usernamecache), saves EMPTY. The user starts `j-int/run-client.bat` and makes a NEW world.

## Resume point
Integration DONE. All 6 branches merged; map regenerated; checkers + gametests as above; TESTEN_VADSRIJK.md + SPIEKBRIEF.md
(renamed from SPIEKBRIEF_RONDE2.md, round 3 sections R1-R10 on top) updated; run/ ready. Open: the user's client checks
(SPIEKBRIEF R1-R10), mounts stepping over fences, the grotSpawner order flake.
