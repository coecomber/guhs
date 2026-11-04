# NOTITIES_G_INT: Vadsrijk round 2 (second playtest), integration (branch 110-g-int, worktree guhs_vadsrijk/g-int)

Base: fba766b79 (110-fix-int). One merge commit per branch. Never gradle in f-int / w-poc (the user plays from f-int).
build/moddev copied from f-int (artifacts only).

## Merged
| branch | head | notes |
|---|---|---|
| 110-g-biomen | b8427f57a | clean |
| 110-g-ui | 8c48f6aa8 | lang union (scratchpad gint/langunie.py); Kern+Biomen 9/9 |
| 110-g-kaart | 786ec74da | clean; + loopcheck `_deur` passable (97cafc68b) |
| 110-g-klassen | 739674370 | vadsrijk_klassen.py: klassen's scherm.ordetempel text + ui's skilltip keys; lang union |

## Waiting for
g-ux, g-balans, g-z2, g-mounts (+ maybe zone 3).

## Cross-cutting to do
- after g-z2: VadsrijkBiomen zone-2 boxes + `zone2Biome` id mapping -> new zone-2 areas.
- g-klassen + g-balans: one `config/guhs-vadsrijk-balans.toml`.
- level users (quests, gates, xp, tracker, mounts, z2 airship gate) -> generic vs class level.
- npc package: g-ux + g-z2 (+ g-kaart's Npcs.staanPlek / NpcPlekken Bram).
- DONE (after klassen): `KernPayloads.Hud.knopen` = active class's bought nodes -> SkillInfo HUD hover; VERBETERINGEN unchanged (klassen did not touch nodes). Trainers: `QuestMotor.EXTRA` option per trainer -> `KlasseBalie.geefStartwapen(p, klasse, WAPEN_PRIJS)`; the "training" role dialog now shows metExtra.
- g-balans `mobs.schade` -> extra section in KlasseBalans's toml + loader.
- g-ux Zwamtempel gate: generic lv 18 -> always open: keep.
- audit.py: MemoryError under load -> run when few agents are busy.
- gametests still owed: VadsrijkNpc (for g-kaart).

## Log

## Resume point
Merging (see table).
