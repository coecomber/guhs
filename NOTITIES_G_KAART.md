# NOTITIES_G_KAART: Vadsrijk fix round 2 (second playtest), zone 1: map generators + NPC placement

Branch `110-g-kaart` (worktree `guhs_vadsrijk/g-kaart`), from fba766b79. Only the generator sources (.py) and Java are committed.
The tiles, WereldPlekken, POI and AUDIT/veilige_plekken stay as they were; integration regenerates them with `maak_kaart.py --zone 1 --geen-cache`.

## What changed
### 1. Camp stair top + arrival path (`tools/vadsrijk/kamp/kamp.py`)
- New `aankomstpad(I)` (called in `bouw` right after the other paths, BEFORE the trees): a 3-wide path from the top of the cliff
  stair (world 0,88,1102 = local 150,237) north to the campfire square (local 151,172), plus a branch east onto the pond path.
  The surface is dirt path with gravel and knuffelklinkers. It has stone-brick and mossy stone-brick edging flush in the grass, and guh lamp posts
  every ~11 blocks on alternating sides (none between the tents or on the plein). The path cells get 3 air above them.
- Every path and edge cell is claimed with a 3-block margin, so `bomen_en_bloemen` puts no tree on or over it. The tree that stood
  in the middle of the stair exit (audit: `-2 86 1109 boom op het pad`) is gone. The stair landing (bouwsels.kamp_zuidsteiger)
  is unchanged and flows straight into the new path.
- The tree RNG sequence shifts, so all camp trees move a little (same density, still 3 off the paths).
- Quick fixes: the pond's slime/honey blobs never land on a path block (one sat on the pond path), and the training field's
  barrel + hay stand on coarse dirt instead of a random dirt-path cell (the loopcheck reported them as "block on the path").

### 2. Bakker Bram + the NPC floor search (Java, npc slice)
- Cause: Bram's spot `p(22, 64, 62)` was the barrel counter of his bread stall (zwamzoet.kraam, the stall with the roze guh-koek).
  The floor search went up the column and found the stall's canvas roof (y 68).
- `NpcPlekken`: Bram -> `p(23, 64, 61)` (inside the stall, behind the koek). `Npcs`: Bram's yaw 270 -> 0 (he faces the customers on
  the south side).
- `Npcs.staanPlek(sl, pos)` (new, used by `zet` and the "back home" snap): the exact spot first. If that cell is blocked
  (counter, wall), it takes the nearest free floor beside it at the SAME y (ring 1, then ring 2: inside the building or stall)
  before any vertical search. `vloerOfNaN` now searches DOWN first (1..4, lit floors), then the old alternating search up to 12.
  So a blocked spot never goes onto the roof above it. `vloer()` keeps its signature (Verhaal, Z2Verhaal and NpcCommando use it).
  The "no floor -> hover" check now looks at the chosen stand cell.
- I checked all zone-1 NPC spots against the regenerated tiles (scratch emulation of the search, without light): all stand at dy 0 on
  a real floor, except tweeling_tap (-382, 68, 62 -> 65, inside the hollow mushroom, fine). Nobody else is on a roof, in a wall or
  underground.

### 3. Ordetempel (Kasteeltje of Grootmeester Vadsvoet) on the cap (`tools/vadsrijk/stad/zwamzoet.py`)
- Cause: the corner towers (centre cx+-19, cz+10, r 5) and the bordes corners stood beyond the flat plateau (radius 20.7), above the
  sloping cap, so their bottoms hung 3-11 blocks above the cap. The west and east rope bridges also leave the cap right there and
  carved the ground away under the towers.
- `zwam(..., voet=tempel_voet)`: under the temple's footprint (bordes + stair + both towers) the cap top is raised to the plateau
  level T, with 1 block of flat margin and then a 1:1 slope back to the normal cap profile.
- Towers moved to cz+12 (`TEMPEL_TOREN_DZ`), clear of the bridges and still within the cap radius. Each tower gets a solid
  knuffelsteen plinth from T-3 up to its floor and a roze-knuffelsteen base course (r 5.7) on the cap.
- `funderen(S, box, y_van, y_tot, y_min)`: every column of the temple's lower part (y0..yv) whose lowest block hangs over air gets
  a foundation down to the first solid block. This covered the facade ends, the vuurschaal pedestals and the gold/shroomlight posts
  in front of the stair.
- The same `funderen` runs under every cap house (hoedhuisjes), filled with the cap block. Three houses had floor corners over the
  sloping cap edge (hoed_noordo, hoekw, hoeko).
- Scene check (stad.npz before hollowing, scratch `zweef.py`): nothing of the temple base hangs over air any more. What is left in the
  list is legit: statue ears/faces, door rings, arch tops, the lift station roof on its posts.
  NB: in the TILES, solid masses are hollowed (fully enclosed blocks are removed), so column dumps of tiles show fake gaps.

## Checks (regenerated zone 1 locally, `--geen-cache`)
- loopcheck camp box (-130,900..130,1150): PAD 1 (training hay, fixed afterwards; not regenerated again), TRAP 126 = the A-tent
  roof stairs (known false positive), VAL 1 = the lighthouse cellar (known false positive: open trapdoor over a ladder).
- loopcheck Zwamzoet (-150,-130..150,130): only roof-stair TRAPs, plus 2 VAL that I did not cause:
  - `-78 64 37`: a stem house with a `roze_lakhout_deur`. The loopcheck only treats "door" as passable, not "deur", so this is a
    false positive of the checker (for the audit/loopcheck owner).
  - `83 95 27`: a 1-cell pocket beside a spiral stair. Open; not touched.
- audit.py: the tree on the stair exit is gone. New `78 87 991 boom op het pad` = the training-field corner post (cherry log + lamp)
  on the field's random dirt-path ground (same as the old `78 87 971`; false positive). The `0 177 -32` ordetempel "kuil" is the
  pre-existing one (the hollowed floor mass under the Knuffeldeur), not new.
- After the final regen (with the true-distance edging): loopcheck camp = PAD 0, TRAP 126 (tent roofs), VAL 1 (lighthouse cellar).
  The last audit.py rerun died of a MemoryError (other agents' RAM); the audit numbers above are from the run before the edging tweak.
- The `0 177 -32` / `-7 177 -33` ordetempel "kuil" grew from 122 to 129 columns. Those are the solid layers under the hall floor
  (y 174..175), which the tile writer hollows because they are fully enclosed. It is the same checker artifact, not a real hole.
- AutoCheck `tools/autocheck/vadsrijk_g_kaart.txt` (13 shots, 0 problems; run/screenshots/autocheck, not committed):
  - stair top: the exit is free, and a straight 3-wide path with lamp posts leads to the camp, no tree.
  - Bakker Bram stands INSIDE his stall behind the koek, facing the customers.
  - the ordetempel from below/the sides: the cap is solid under the bordes and the towers.
  - the grootmeester, the smid, oma Truus and the trainers stand on their floors.
  - After that run, only the path edging changed: it became one continuous row (it was a ragged diamond pattern) with fewer pink
    klinkers. I checked it in a tile top-down, not in a second client run (one AutoCheck per agent).
  - Extra: an offline render of the temple (scratch, stadkit.render2) shows both corner towers standing on the raised cap rim, with no gap.

## Open / for integration
- Regenerate once after merging (`--zone 1 --geen-cache`); the tree layout of the camp changes (RNG), which is intended.
- loopcheck/audit: teach them that `*_deur` is a door (false pits at stem-house doors).
