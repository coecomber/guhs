# NOTITIES_U_OVERGANG: natural zone transitions + epic viewpoints (round U)

Branch `110-u-overgang` from eaee06d08 (110-t-int). Feedback: `guhs1100-avontuur/FEEDBACK_RONDE4.md` ("zone-overgangen moeten
natuurlijker", "W3 uitkijkpunten liggen allemaal bij de ballonpunten", `uitkijk_nest`: rare naam, losse fences, zwevende
lantaarns). Merged: `110-u-hekken` (the central connection pass `verbind.py`).

## FOR OTHER AGENTS / THE INTEGRATOR (read first)

- **One open-sea model, two mirrors:** `tools/vadsrijk/zeenaad.py` (Python, the tiles) = `indeling/ZeeVulling.java` (Java, the
  tileless sea chunks). Same integer hashing, same double maths. **Change BOTH** (the gametest
  `IndelingGameTests.zeeVulling` holds reference values printed by zeenaad.py and fails when they drift apart).
- **The seam pass `tools/vadsrijk/overgang.py`** runs automatically at the end of every `maak_kaart.py` run (any `--zone`,
  before u-hekken's `verbind` pass). It is idempotent: running it again changes nothing. **If you run a zone maker directly**
  (`zone3/maak3.py`, `zone4/maak4.py`, `zone5/maak5.py`), run `python tools/vadsrijk/overgang.py` afterwards (~25 s without
  changes, ~3 min with).
- **New zone / moved zone:** add its tile box to `overgang.GROEPEN`, its land box to `zeenaad.LAND` / `ZeeVulling.LAND` and its
  tile box to `zeenaad.TEGELVAKKEN` / `ZeeVulling.TEGELVAKKEN` (rocks stay away from tiles), and its coast shape + anchor biome
  to `VadsrijkBiomen.KUSTEN` / `ANKER` (+ `vadsrijk_u_overgang.ANKERS`).
- Viewpoint ids removed: `uitkijk_noordkliftop`, `uitkijk_galmkam`, `uitkijk_kandijkom_balkon` (T-WERELD's harbour corners)
  and `uitkijk_westtak`, `uitkijk_oosttak` (the zone-3 piers). Their POIs are gone; the harbour POIs (`haven_*`) stay.
  `SPIEK_U_HEKKEN.md` still mentions `/guhs vadsrijk indeling plek uitkijk_westtak`: use `haven_z3_z2_vertrek` instead.

## 1. Natural zone transitions

### What was wrong (measured on the t-int tiles, renders in the scratchpad)
- **Water colour:** the biome came from area BOXES. Zone 2's ring box ended at x 1004, then a strip of "De Zee", then from
  x 1024 zone 3's `wortelvelden` (a cylinder r 600 round the trunk, y 0..320) with **teal** water (#58c8c0) over the whole
  zone-3 sea. Zone 1's ring box (±660) and the zone-4 box gave more straight colour lines. Sky/fog jumped at the same lines.
- **Sea floor:** every zone had its own floor (zone 1 light-blue terracotta at 49, zone 2 gravel 53..60, zone 3 gravel 50..60,
  zone 4 pink sand 34..55) and the procedural sea `ZeeVulling` another one (49, or 60 near zone 2). Under-water cliffs + a
  floor-colour edge exactly on the tile-box lines: x 1024 (613 columns with a jump of 4+), x 384, x 1728, z -768, -256, -1472,
  -704, x 2560 (`naden.py` in the scratchpad counts them).
- **Cloud sea:** zone 3's cloud banks (`barriere.zee_z3`) stopped dead at the zone-3 tile box: a straight cloud edge at x 1024
  (the Wolkenkloof, `indeling rand wolkenkloof_noord`), z -1024, z -129 and x 1727.

### What I built
- `tools/vadsrijk/zeenaad.py` + `ZeeVulling` (Java): the shared open sea.
  - Floor 47..51 (smooth value noise round 49), floor block light-blue terracotta / sand / gravel in patches.
  - The zone-3 cloud field moved here (`wolk`): the same field inside the zone-3 box, except that the outer edge of the
    Wolkenkloof's cloud cliffs now wobbles (24..56 blocks from the channel instead of a parallel line at 40); outside the box
    the threshold rises and the banks thin out over ~70 blocks, following the noise. So the Wolkenkloof's west bank
    now continues into zone 2's tiles and the open sea and frays out instead of ending on a line.
  - Scattered rock stacks (r 3..8, 1..8 blocks out of the water) in the open sea 30..260 blocks from a zone's land, never within
    r + 6 of a tile box. ~200 of them; they break up the empty sea round the coasts.
- `tools/vadsrijk/overgang.py`: the seam pass over the edge tiles of every tile group (zone 1 + camp grid, the Splinterwand row,
  zone 2, zone 3, zone 4). Only OPEN-SEA columns (water from the floor to y 62, only air/clouds above, no block entities):
  - floor height clamped to the shared floor, with an allowed deviation of 0.35 block per block from the box edge; the band
    width wobbles with noise (×0.55..1.45) so even zone 4's deep basin gets an organic rim instead of a rounded rectangle;
  - floor block dithered towards the shared one over the band (48 blocks);
  - the zone-3 cloud banks continued into the neighbouring tiles (zone 2 east edge, the Splinterwand row, zone 4 west edge);
  - exposed hollowed rock under a lowered floor is filled with stone.
  Result: the floor-jump lines at the tile boxes are gone (x 1024, 384, 1728, z -768, -256, -1472: 0 columns); what remains
  are a few short lines inside zone 4 (its own islands' terrain, e.g. a 3-block step at x 2304) and the spire feet in the
  Splinterwand row.
- **Biomes** (`VadsrijkBiomen`, U-OVERGANG block; generator `tools/features/vadsrijk_u_overgang.py`, one line in FEATURES):
  every zone has a coast shape (superellipse/circle, `KUSTEN`). Inside it the old area boxes decide; outside all of them (the
  open sea, also inside a zone's tile box) a **sea-band biome** between the two nearest zones: 7 steps per zone pair
  (`zeeband_<a>_<b>_<k>`, k/8 of zone b), colours (water, water fog, sky, fog) interpolated between the zones' anchor biomes
  (zone 1 `tussenland`, zone 2 `splinterpieken`, zone 3 `schemerwortel`, zone 4 `lampionkust`). A column inside a shape that
  no area claims gets its zone's anchor (was "De Zee"). 42 new biomes, all named "Het Vadsrijk - De Zee".
  Zone 3's sea is no longer teal; zone 3's island (r <= 205 from the trunk, the crown r <= 275 from y 100) keeps its layers.
  Zone 5 (above y 150) is unchanged (its lookup still runs first).

### Decisions made myself
- Tiles are pulled onto the shared sea (not the other way round): the sea is one function, every zone just needs its edge to
  meet it. No zone generator had to change (except the cloud function moving out of `barriere.zee_z3`).
- The floor is deep everywhere in the open sea (47..51; the old "shallow 60 round zone 2" is gone; zone 2's own shelf is in
  its tiles and now slopes down to it).
- One biome per band step instead of a continuous colour: vanilla blends biomes over a few blocks, 8 steps over a gap of
  200..500 blocks make the change invisible.
- Rocks are only in the procedural sea, never in tiles (no cut rock at a tile edge).

### Not done / open
- Zone 4 has internal floor steps of ~3 blocks between its own tiles (e.g. x 2304, z -560..-460): t-z4wereld's terrain, not a
  zone seam. Left alone.
- The Splinterwand row (tz -12) is only 64 high, so the seam band covers almost all of its sea; its spires stay as they were.
- Client-side sky tints (`Z2Lucht`, `Z3Lucht`) still fade by box distance (they already faded; untouched).

## 2. Viewpoints (W3)
- New: **De Reuzenhoed** (`uitkijk_hoedtop`) on the giant temple mushroom of Zwamzoet: from the temple forecourt (where the
  tempel lift's ramp arrives) a 3-wide railed plank walkway out over the cap rim to a round railed balcony on cherry-log
  struts, flush with the plateau, ~110 blocks above the town. (`stad_wereld.hoedbalkon`; zone-1 viewpoints now also come from
  the town's POIs, `maak_kaart.java_plekken`.)
- Fixed: **Het Kruinnest** (`uitkijk_nest`, the very top of the tree): it had no name text (the "rare naam" was the raw key);
  the railing and the two lanterns stood one block above the nest floor (floating, `boom2.nest` y0 + 2 -> y0 + 1); the fences
  are connected by u-hekken's pass.
- Names added for the zone-4 viewpoints that had none: **De Vuurtorengalerij** (the lighthouse gallery, `uitkijk_vuurtoren`,
  spiral stair inside), **De Lampionduin**, **De Pierkop**, **De Vliegerheuvel**.
- Kept as is (already epic, with a way up): Galmtop (the highest needle of the Splinterpieken, spiral stair), Kroon, Takpunt,
  Wortelgewelf (zone 3), the zone-1 needles, and Hoogvads' Ooggaarde-balkon / Geveltoren / Staartvin / Sterrenwacht.
- Removed (they were the "allemaal bij de ballonpunten" ones): Noordkliftop, Galmkam, Kandijkom-balkon, Westtak-uitkijk,
  Oosttak-uitkijk.
- Name check: every viewpoint id of zone 1-5 has an nl + en text now (script in this note's resume section).

## Shared-file changes (all marked U-OVERGANG)
- `VadsrijkBiomen.java` (sea band; `IDS` = base list + band ids; flat lookup ends in the zone anchor instead of De Zee),
  `VadsrijkBiomenGameTests.java` (expectations: two old "zee" spots, new band spots).
- `ZeeVulling.java` (rewritten: the shared model), `IndelingGameTests.zeeVulling` (reference values instead of "shallow 58+").
- `IndelingRegistry.java` (`plek` also finds zone 4/5 POIs; suggestions = the epic viewpoints).
- `tools/vadsrijk/maak_kaart.py` (overgang pass before verbind; town POIs `uitkijk_*` in zone 1's viewpoint list),
  `barriere.py` (cloud field from zeenaad), `wereld/bouwsels.py`, `zone2/bouw2.py`, `zone3/plan3.py` (harbour viewpoints
  removed), `zone3/kit/boom2.py` (nest railing/lanterns), `stad/stad_wereld.py` (Reuzenhoed balcony),
  `tools/features/__init__.py` (+ `vadsrijk_u_overgang`).

## Fixed in u-hekken's code (merged)
- `verbind.na()` passed `--alleen <list>`; a list starting with a negative tile ("-10,-1;...") is read by argparse as an
  option, so `maak_kaart --zone 1` crashed at the end. Now `--alleen=<list>` (marked U-OVERGANG).

## Regen + checks (2026-11-06)
- `maak_kaart.py --zone 1`, `--zone 2`, `--zone 3` (all 154 tiles), each followed by overgang + verbind; zone 4/5 tiles only
  got the seam pass (their makers did not change). Last pass: `overgang: 434 randtegels` (0 changes on a rerun = idempotent).
- Gametests `-Pgt=Indeling,VadsrijkBiomen,Z5Wereld,VadsrijkVoltooiing`: 11/11 pass; after the kloof-wobble change
  `-Pgt=Indeling,VadsrijkBiomen` again: 5/5 pass.
- `verbindcheck`: los 2, fout 0, as 0, gat 1 (u-hekken's baseline ~0).
- `audit.py` (zone 1 + camp): stap 5 (= baseline), kuil 7 (baseline 4: the 3 new ones are in the Sporenwoud / Hoedenpaden
  area, -374 103 82, -336 97 56, -331 97 57, far from anything this slice built; probably the regenerated Hoedenpaden network
  or the connection pass raising a post; NOT checked further), water 20 (= baseline), zee 1.
- `loopcheck.py --box=-60,-60,40,0` (Zwamzoet incl. the Reuzenhoed): no VAL; 2 TRAP at the town square (y 64), old.
- `loopcheck3.py`: no VAL/TRAP; PAD 462 (t-wereld's baseline 388..396; the extra ones are fence posts/blocks on the zone-3
  paths, likely from the connection pass; not inspected).
- Floor jumps of 4+ across a tile-box line (my `naden.py`): x 1024 613 -> 0, x 384 528 -> 0, x 1728 512 -> 0,
  z -768 1611 -> 0, z -1472 490 -> 0, x 2560 128 -> 0; what is left are short internal zone-4 steps and rock/spire feet.

## Resume point
- Head: see `git log` (branch 110-u-overgang). Generated files (tiles, *WereldPlekken.java, POI_*.md, AUDIT_*.md,
  veilige_plekken.json) are NOT committed; the worktree holds the regenerated ones. Integration: regenerate with
  `python tools/vadsrijk/maak_kaart.py --zone alle --geen-cache --audit` (overgang + verbind run at the end by themselves),
  then the FEATURES generators (incl. `vadsrijk_u_overgang`).
- Renders for my own checks: scratchpad `kijk/` (topo.py = top-down from the tiles + the procedural sea; naden.py = floor-jump
  counter; band.py = Python mirror of the sea band).
