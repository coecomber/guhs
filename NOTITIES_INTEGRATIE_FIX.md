# NOTITIES_INTEGRATIE_FIX: Vadsrijk fix round 1 + zone 2, integration (branch 110-fix-int, worktree guhs_vadsrijk/f-int)

Base: 6ec8724ca (zone-1 POC). One merge commit per branch. Lang conflicts = union of both sides (ours first, then the
other side's new keys; resolver script: scratchpad `langunie.py`, logic: drop conflict markers, append unseen keys, fix commas,
`json.loads` check).

## Merged
| branch | head | merge | notes |
|---|---|---|---|
| 110-fix-stad | 8a0599435 | (see git log) | clean |
| 110-fix-veilig | 82eaca630 | | clean |
| 110-fix-gevecht | 649cf3aee | a3210ac40 | lang conflict (union) |
| main v1.9.2 | 0282ff9c5 | 220882eff | lang conflict (union); Erfenis F9/F10 + ToetsVerhuizing from main |
| 110-fix-kamp | 608f13a78 | dc9eabbee | lang conflict (union); bouwsels.py auto-merged with stad |
| 110-fix-sfeer | 1d537afb3 | (git log) | clean |
| 110-fix-quests | efff644d4 | 874a45ad5 | lang union; vadsrijk_npc.py TIPS: elementen = gevecht text, quests = quests text; npc generator re-run = identical lang; Erfenis files identical to v1.9.2 |
| 110-fix-leven | a39fc19b2 | (git log) | clean |
| 110-fix-bazen | fe7f50f62 | f4cd80d88 | lang union; BazenLoot untouched by bazen (gevecht numbers stay); `baas.te_ver` added to vadsrijk_kern.py |
| 110-fix-beleving | ac0e6286e | bbe36d2d7 | KernRegistry: Veilig + Beleving lines both; lang union; phase names per boss in vadsrijk_beleving.py; VOOR_KLAP still before Getallen.toon |
| 110-fix-wild | 312e13a02 | db73717dc | bouwsels.py: kamp's zwemtrap + wild's naast_pad/_padafst both kept |
| 110-fix-verticaal | 944e39bdb | (git log) | clean |
| 110-z2-systemen | de029f699 | 499c32010 | FEATURES + lang; Rollen/RijkItem/klassen tests auto-merged with gevecht |
| 110-z2-wezens | dae5c34e5 | 2fa94fefd | hooks both kept; wezens Bevroren = facade over kern Status + systemen; wezens crystal drop off |
| 110-fix-momenten | 2ee0e9ca1 | fb5d67345 | hooks both kept |
| 110-z2-wereld | 92a270760 | 5cda03e4c | WereldClient: one tick listener (sfeer + flight skip) + Z2WereldClient.init; maak_kaart: --audit and --zone both |
| 110-z2-npc | 763c22876 | cea943bb7 | QuestMotor: hint/quiz (quests) + metExtra/EXTRA (z2-npc) |
| 110-z2-bazen | 550635ca0 | 3e098d28c | hooks + FEATURES + lang |

## Waiting for
NOTHING: all 14 branches + v1.9.2 are merged.
(old list:) beleving, verticaal, momenten.
zone 2: z2-wereld, z2-wezens, z2-bazen, z2-npc, z2-systemen.

## Rules to keep while merging
- bazen/BazenLoot named weapon numbers: keep gevecht's (6.5 + 1.3).
- Erfenis keys: keep main's (F9/F10) + ToetsVerhuizing over the quests branch.
- Generated tiles/WereldPlekken/POI: regenerate ONCE at the end (`python tools/vadsrijk/maak_kaart.py --audit`) and commit
  them + `data/guhs/vadsrijk/veilige_plekken.json`.
- build/moddev copied from w-poc (artifacts only); never gradle in w-poc.

## Log
- 653d39e99: kamp open items done by integration:
  (a) Zwamzoet water exits: `stad_wereld.zwemtrappen` (scene twin of `bouwsels.zwemtrap`): ladder at each jetty end, 2 per side
      of each jetty, and every 14 blocks up the fenced quay (fence/lantern above cleared); 17 ladders, checked in the scene npz.
  (b) Lighthouse: ladder inside the tower (lx, lz-2) on a pillar (lx, lz-3) through the gallery floor + doorway through the
      glass ring; POI `vuurtoren_top` moved onto the gallery (lx, ly+32, lz-5) (was inside the froglight core).
  Audit false positive: an open trapdoor above a ladder (vuurtorenkelder) counts as a pit.
- Gametests after stad/veilig/gevecht/main/kamp(/sfeer): `-Pgt=VadsrijkVeilig,VadsrijkKlassen,VadsrijkWereld,VadsrijkKern` 20/20.
- To check at the end: duplicate ambient particles (leven `wezens/client/SfeerClient` spores/fireflies in the Sporenwoud vs sfeer
  `wereld/sfeer` storm spores + wereld motes); WezensPlekken copies of kaart.py PADEN/VIJVERS/GROT_* vs f-wild's changes;
  quests loopcheck (`tools/vadsrijk/loopcheck_doelen.py` / `loopcheck_pad.py`) after the regen (south jetty stair, Druipgrotten).
- RijkHud: a title card arriving < 1 s after another waits for it (level-up + "Opgevrolijkt!").
- npc SCHAT `hoedenschat` = `geheim_hoedenschat` (verticaal's secret hut): rolled Zeldzaam item + 2500 munten, advancement
  vr_schat_hoedenschat, text in vadsrijk_npc.py SCHATTEN. (The physical chest stays empty: the reward drops on discovery.)
- WezensPlekken: PADEN aligned to wild's kaart.py (berg rerouted, needle-foot ends of weide3/slijm/woud2/druip, slijm2),
  ramp BERG_VOET = fossielbrug.naar y - 1 .. TP-7; the 2 mountain elites follow the path (bergPunt).
- Shared scratchpad with other agents: my files are in scratchpad/fint/.
- Gametests so far: Veilig/Klassen/Wereld/Kern 20/20; Wezens/Npc/Kern 17/17; Bazen/Beleving/Wezens/Npc/Wereld 26/26 (on a trial
  regen); Z2Systemen/Klassen/Kern/Economie 22/22.
- Trial regen + audit (before zone 2): Zwamzoet quay ladders reached into the hollow under the sea (18 "onder de kaart") -> sandstone
  under them; ordetempel front stair -> one step per tier. Audit now reads zone-1 tiles only (tz > -20).
- controleer_verticaal: Druipschacht 0 traps; Hoedenpaden 6671 "traps" = all in the sea at the west edge of its check box (x -576,
  y 60: water states with no land target inside the box) = checker artefact; every hoedenpad POI reachable.
- Kern HUD: Bevroren line (Hud payload field `bevroren`). Shop post rows: post line cut before the price.
- Zone 2 wiring: NpcRollen binds z2_luchtlijn -> Z2Reizen.naarKandijkom/naarHaven, z2_gondelbaan -> Z2Gondel.rijd; the npc opening
  only runs without that binding (so ONE opening: z2-wereld's); QuestStap: explicit `plek` first, airship hop Haven<->Kandijkom
  (z < -1700), levelPlek lv 21+ = z2 areas, zone-2 NPC spots from NpcDef. Spots from Z2WereldPlekken: wezens (burcht rooms,
  mika_fort, arenas, every campfire as safe spot), systemen (juwelier/kandijkraam/altaar), npc (34 exact POI copies).
  Splinterkloof floor: 12 spawners at y 58..74 (`MAX_PER_GEBIED.splinterkloof_bodem`).

## Final steps (all branches in)
- z2-bazen wiring (6658ecc9e): kern `VadsrijkBaas.eigenGevecht()` (Z2Baas = own engine; kern keeps only lobs + kinematic jumps,
  so no double leash/reset/retarget), Spiegelgang gauntlet = vr_z2_mob_spiegelwacht/scherfgeest/splinterling, Z2BazenPlekken from
  Z2WereldPlekken (hall centre (0,176,-2963)), `z2_boormeester` -> `Boormeester.ontgrendel`, wiki blokuitleg `vr_z2_baas_spiegel`.
  Z2Baas.bevroren() already resolves to kern Status.BEVROREN. The npc opening's Galmwolf binding is unused (one opening =
  z2-wereld's, which has the howl but no wolf model): open point.
- Map: `maak_kaart.py` needs `--geen-cache` after a stad/kamp generator change (the scenes are cached in tools/vadsrijk/_cache).
  Final: zone 1 `--zone 1 --audit` + zone 2 (`maak_kaart.py --audit --geen-cache` once). Committed 9529804b0: tiles, WereldPlekken,
  Z2WereldPlekken, POI_VADSRIJK.md, POI_Z2.md, AUDIT_VADSRIJK.md, veilige_plekken.json (24 384).
- Audit after: onder de kaart 0 (was 18 from my first quay ladders), kuil 6 (vuurtorenkelder = trapdoor false positive; ordetempel
  "pocket" under the hall floor = a culled enclosed hollow in the tiles; 1-cell Zwamzoet; 4 cells Hoedenpaden; Druipschacht =
  audit vs controleer_verticaal disagree, the latter says 0), trap 14 (tent roofs), stap 5, water 5 (Zwamzoet bay far from a
  ladder, Druipschacht lake), deur 2 (decorative).
- loopcheck (wild) zone 1: VAL 5 (vuurtorenkelder; a stem-door hall in Zwamzoet = checker doesn't know roze_lakhout_deur; 3 single
  cells), TRAP 204 = roofs/benches, PAD 1. loopcheck_doelen (quests): all ok except the lift foot (POI on the lift block) after
  moving `litteken_tempel` to (145,88,-265) and updating its trainer table.
- controleer_verticaal: Druipschacht 0; Hoedenpaden: all POIs reachable, "traps" only in the sea at the edge of its box (artefact).
- Caravan road: checked on the new tiles (scratch karavaan.py): no water, small steps; one extra corner to the moved campfire.
- Gametests `-Pgt=Vadsrijk,Z2Npc` in one run: **69/69** (after: z2 boss weapons 7.0+1.4 DPS model, test fixes for parallel
  runs/zone-2 hand-over, a flaky interval test).
- run/: config/options/defaultconfigs copied from w-poc, `saves` empty.
