# NOTITIES_G_INT: Vadsrijk round 2 (second playtest), integration (branch 110-g-int, worktree guhs_vadsrijk/g-int)

Base: fba766b79 (110-fix-int). One merge commit per branch. Never gradle in f-int / w-poc (the user plays from f-int).
build/moddev copied from f-int (artifacts only).

## Merged
| branch | head | notes |
|---|---|---|
| 110-g-biomen | b8427f57a | clean |
| 110-g-ui | 8c48f6aa8 | lang union (scratchpad gint/langunie.py); Kern+Biomen 9/9 |
| 110-g-kaart | 786ec74da | clean; + loopcheck `_deur` passable (97cafc68b) |
| 110-g-balans | f3c03e9b7 | lang union; config unified (see log) |
| 110-g-ux | 79f8a7dda | Npcs: kaart staanPlek + ux standplek -> ONE `staanPlek` (exact; blocked -> nearest lit free floor same y within 2; down 1-4; up/down 12); lang 3-way per key (scratchpad gint/lang3.py) |
| 110-g-mounts | ec1a2aea7 | __init__ FEATURES both (biomen + rijdieren); lang 3-way; + boss Pluimguh eggs (BazenLoot.pluimEi) |
| 110-g-z2 | 25568a596 | Z2NpcPlekken: z2 volgKaart + dragon cams relative with balans' x1.5; ux GEODE/POORT_* -> r 66 gates; Z2WezensSpawners: z2's grid + ONE path rule (kern Spawners.padAfstand - PAD_VRIJ); Z2WezensPlekken.PADEN = generated Z2WereldPlekken.PADEN |
| 110-g-werver | b5f7af9cf | (contains ux+mounts) Z2NpcPlekken z2_wolfje + volgKaart; lang 3-way with base fb8601735 (werver's ux+mounts merge; `lang3.py --base=`); quests_z2/FTB/source_ftb = ours, then regenerated: `vadsrijk_npc.py` FIRST, then `vadsrijk_z2_npc.py` (npc's _direct deletes the z2 keys otherwise), then make_ftbquests |
| 110-g-klassen | 739674370 | vadsrijk_klassen.py: klassen's scherm.ordetempel text + ui's skilltip keys; lang union |

## Waiting for
g-pets. Lead: wait for its message before the final steps.

## Cross-cutting to do
- after g-z2: VadsrijkBiomen zone-2 boxes + `zone2Biome` id mapping -> new zone-2 areas.
- g-klassen + g-balans: one `config/guhs-vadsrijk-balans.toml`.
- level users (quests, gates, xp, tracker, mounts, z2 airship gate) -> generic vs class level.
- npc package: g-z2 still to come.
- after zone-1 regen: check Sergeant Pluizebol's spot (dark/enclosed in g-ux's intro shot).
- after g-z2: Kandijkom gates open (ux found walls at x=+-128 and a north step); update Z2NpcPlekken.POORT_* if gates moved.
- DONE (after klassen): `KernPayloads.Hud.knopen` = active class's bought nodes -> SkillInfo HUD hover; VERBETERINGEN unchanged (klassen did not touch nodes). Trainers: `QuestMotor.EXTRA` option per trainer -> `KlasseBalie.geefStartwapen(p, klasse, WAPEN_PRIJS)`; the "training" role dialog now shows metExtra.
- DONE: one balance file: kern.Balans = value holder, KlasseBalans reads/writes `[mobs] schade` (1.3) next to [algemeen] + classes.
- after g-z2 (balans): Z2WezensPlekken.PADEN copied from the OLD zone-2 plan.py -> refresh from the new layout (and the zone-2 spawner moves follow).
- dragon 3.75 scale + raised opening dip: user should judge the framing (TESTEN).
- g-ux Zwamtempel gate: generic lv 18 -> always open: keep.
- audit.py: MemoryError under load -> run when few agents are busy.
- gametests still owed: VadsrijkNpc (for g-kaart).

## Log
- Full run 1 `-Pgt=Vadsrijk,Z2Npc`: 82/83; reisKeuzes = placement (z -7M = zone 2) -> Reiskaart.hier '' outside the Vadsrijk (5cbcf4161). klodderreusVecht passed (flaky earlier).
- loopcheck whole map: the FROZEN KNUFFELMEER (11946 spots) had a 2-high shore all round (kaart2 oever = ijs+2) -> oever starts at ijs+1; zone 2 regenerated + rechecked.
- loopcheck zone 2 VALs that stay: tier-house hollows in Kandijkom (63/99 cells, unreachable, g-z2 notes), single cells.
- controleer_verticaal: Druipschacht 0 traps; Hoedenpaden 6671 = sea edge of its box (known artefact).
- Regen 1 (`maak_kaart --zone alle --geen-cache`): audit found a NEW pit at Zwamzoet 110 118 -90 = g-kaart's cap-house funderen filled hoed_noordo's spiral-stair exit -> funderen(niet_op_trap) (11c114a76); zone 1 regenerated again: same as before.
- FTB: CHAPTER_VERSION 60 (chapter texts changed by ux/z2).
- Gametest fails after ux merge (5 classes run): reisKeuzes 'lv 20: Kandijkom flies' (diagnostics added b51581a5f) and klodderreusVecht 'hurt the player standing still' (moved 10, closest 6, 0 hits). To check in the full run.
- Gametests after biomen/ui/kaart/klassen/balans: Klassen,Npc,Kern,Balans,Z2Systemen,Wezens,Z2Wezens 40/40.
- Bells (ux follow-up): all ship bells open `Reiskaart.open` (texts in vadsrijk_npc.py / vadsrijk_z2_npc.py + lang). Haven sneak-click = Kandijkom kept.
- Lang check tool: scratchpad gint/langcheck.py <branches> = every branch-changed nl key has the branch value in HEAD (all OK so far).
- AutoCheck `gui inventaris` was added by g-ui AND g-klassen (duplicate case): kept klassen's (optional hotbar slot).

## Resume point
Merging (see table).
