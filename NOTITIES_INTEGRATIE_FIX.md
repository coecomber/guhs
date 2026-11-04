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

## Waiting for
fix: momenten.
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
