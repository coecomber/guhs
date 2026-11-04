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

## Waiting for
fix: wild, bazen, quests, leven, beleving, verticaal, momenten.
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
