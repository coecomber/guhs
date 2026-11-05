# NOTITIES_T_Z5WEZENS: zone 5 "Hoogvads" combat (lv 56-60)

Branch `110-t-z5wezens` (worktree `guhs_vadsrijk/t-z5wezens`, from dbf82899d + merge 110-r-int abb8a027c).
Package `feature/vadsrijk/zone5/wezens/`, ids `guhs:vr_z5_mob_*` / `guhs:vr_z5_baas_*`, lang `vadsrijk.z5wezens.*`,
generator `tools/features/vadsrijk_t_z5wezens.py`. (Work in progress; full notes follow at the end of the night.)

## For t-z5wereld: POI ids + volumes (EARLY, committed first)
All placeholders live in `zone5/wezens/Z5WezensPlekken.java` as `w("<poi id>", x, y, z)`. If `zone5.wereld.Z5WereldPlekken.plek(id)`
exists and knows the id, it wins (reflection). Coordinates come from the round-2 whale (NOT lowered): if the whale drops ~20, just
publish the ids and every spot follows.

### Volumes this slice builds itself (stand-ins, only into air: `/guhs vadsrijk z5wezens bouw ...`)
Keep these volumes free of city blocks (inside the hollow whale), or tell me and I move them:
| room | box (x, y, z) | why |
|---|---|---|
| De Buikgangen (dungeon) | x 2704..2808, y 184..196, z -530..-508 | maintenance tunnels: Ingangshal, Ribbengang, Kleppenzaal, Ketelkamer (round, centre 2792,186,-520, r 14) |
| Het Schuilnest (secret Mika hideout) | x 2588..2624, y 206..214, z -440..-414 | under the Luchthaven |
Both are reached by a hatch teleport (no shaft through the city): only a trapdoor ON the street.

### POI ids wanted from t-z5wereld (place, or just publish the coordinate)
| id | what | needed block |
|---|---|---|
| z5w_buik_luik | hatch in the cellar/yard of the Grote Soepkeuken (down to the Buikgangen) | a trapdoor on the floor (I place one into air if missing) |
| z5w_buik_uit | where you come back up | standable |
| z5w_schuilnest_luik | a loose plank behind the crates in the Luchthaven station (down to the Schuilnest) | trapdoor |
| z5w_schuilnest_uit | where you come back up | standable |
| z5w_proefarena | Proefarena van de Wolkwacht: an open trials ground beside the Academie oefenplein (r 11, flat, fenced with gaps) | floor |
| z5w_lift_zuidvin_boven / _onder | from the south city rim down to the south pectoral fin (stand-in lift; real stairs welcome) | standable both ends |
| z5w_lift_noordvin_boven / _onder | same, north fin | |
| z5w_lift_staart_boven / _onder | Staartpark down to the tail fluke | |
| z5w_zuidvin, z5w_noordvin | combat spots on top of the pectoral fins (flat-ish, ~r 12) | walkable fin top |
| z5w_staartvlakte_noord | combat spot, north lobe of the tail fluke | walkable |
| z5w_donderbuik_arena (+ _deur) | WORLD BOSS arena, south lobe of the tail fluke, r 20 flat | walkable, no holes |
| wolkwaldroom_arena (+ z5w_wolkwaldroom_deur) | t-z5verhaal's finale room: the spy fight (r 16) | |
| z5w_leider_strobaal / _mistmoeder / _meestervermommer | area-leader spots | standable |

Falling off the fins/tail: `Z5Vangnet` catches players in those areas and puts them back at the lift bottom.
