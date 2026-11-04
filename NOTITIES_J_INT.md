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

## Waiting for
- 110-i-lore (Keizer hints in zones 1+2, zone-2 cliffhanger east): the lead will message. After it: regen both zones again
  (`--geen-cache --audit`), npc generators (vadsrijk_npc.py FIRST, then vadsrijk_z2_npc.py), make_ftbquests (CHAPTER_VERSION =
  max(branches) + 1; lore at 60 -> keep 61, at 61 -> 62), gametests again.

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
