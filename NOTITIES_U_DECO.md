# NOTITIES_U_DECO (round U, branch 110-u-deco from eaee06d08)

Feedback round 4 items: Knuffelkamp lighting, Honingtak (light, street furniture, bees), the Kruinroofvogel arena underside, the
nest viewpoint + the spiral stair floor indication, Schildpadeiland placement, mob spawns at the gondola/arrival spots.
Test steps: `SPIEK_U_DECO.md`. 110-u-hekken (central connection pass `tools/vadsrijk/verbind.py`) is merged in.

## What was built
### Map generators
- **Knuffelkamp** (`tools/vadsrijk/kamp/kamp.py`, `paden_verlichten`): greedy coverage over every path cell (dirt path /
  knuffelklinkers): a cell without a light within 7 blocks gets a guh lamp post on the nearest free grass cell 1-3 blocks beside
  the path (never on it, never against a wall/door). Runs before the trees, claims the cell. ~86 new posts.
- **Honingtak** (`zone3/bouw3.py`, `honingtak_straat`): lamp posts every ~10 blocks on the ring street (r 47.5), alternately a
  bench (`guhs:guh_bankje_hout`) and a bin (`guhs:park_prullenbak`) + a moss planter with a flower between them; posts on the
  outer promenade where it is >= 5.5 wide; a ring of posts + benches round the Honingplein fountain, posts + bins at the plaza
  corners. Every spot checks the deck block, 3 air above, at most 1 occupied neighbour, and keeps clear of every deck POI, the
  house door lines, market stalls, the fountain, lift shafts and wind-column landings (`_dek_vrij`).
- **Kruinroofvogel arena** (`zone3/bouw3.py`, `kale_onderkant`): the skirt keeps its stepped top, but each column gets an
  organic bottom: thickness 3 + 34*(1 - D/46)^1.5 + noise (a deep tapering bowl, ~y-39 in the middle, 3-5 at the rim), 9 pale-oak
  twigs converging to a tip under the middle, and hanging pale moss / hanging roots / glow-berry vines / honeycomb + honey drips.
  Honinglift B's shaft (r 5.5) is kept free.
- **Spiral stair** (`zone3/bouw3.py`, `trap_verdiepingen`): a concrete band on the trunk wall at stand+2 following the stair
  (colour of the floor you climb to), 2 rows on each landing, a wall banner of the floor colour at both landing ends, a lantern
  hanging from a bracket on each landing. Colours: brown, orange, pink, magenta, purple, white.
- **Nest / landings** (`zone3/kit/boom2.py`): the nest-slot fence + the 2 lanterns now stand on the nest floor (were at y0+2 =
  floating 1 above); the door-frame lanterns of the landings stand on a 2-high fence post (were floating beside the frame).
- **Lampion strings** (`zone3/kit/plekken2.lampionsnoer`, `bouw3.kruin_leven`): lampions under a chain are `hanging=true` now.
- **Zone 3 floating decoration** (`bouw3.zwevend_weg`, last step per tile): a standing lantern/lampion with air under it hangs
  (block above), gets a 1-2 block spruce post (floor 2-3 below) or goes; floating plants go. zweefcheck zone 3: 267 -> 0.
- **Schildpadeiland** (`zone4/kit/eilanden.py`, `schildpad`): flowers, lamp posts, the guh figure and the palm use the real shell
  height per cell (`boven`); they all used the dome's peak height. `plan4`: new POI `geheim_knuffel_6` on the shell (lok 10,-7).
- **Zone 4 in general** (`zone4/bouw4.py`): Opvangcentrum knuffels on the floor (were at y+1); `zwevende_plantjes_weg` removes
  every plant with air under it at the end of the game layer (2 strays: Vuurtoreneiland, Pierkop).

### Java (`feature/vadsrijk/udeco/`)
- `Aankomstvrij`: no spawn within 24 (flat, 16 up/down) of arrival spots (gondel_dal/berg, harbour landing/bell/board/plein,
  boot_dok_*, *_landing, luchthaven arrival/piers, terugportaal_*), always; and of campfires + town edges, except for a spawner
  whose own centre is within 26 of that spot (dungeon spawners next to a campfire keep working; the kern's 12 still holds there).
  The spots are collected from WereldPlekken + Z2..Z5WereldPlekken by id pattern.
- `HoningtakBijen`: by day, round a player on the Honingtak deck (r 30-80 from the trunk, y 161-187): up to 8 bees (half vanilla
  `minecraft:bee`, half guhbijtjes `guhs:guh_bee`) + up to 4 zone-3 honey-bee critters, spawned 6-20 away, preferably near
  flowers. Bees: invulnerable, `stopBeingAngry` every 3 s, stay-out-of-hive countdown, their damage cancelled; discarded at night,
  in rain or with no player within 64; tagged with `Getallen.TAG` so they are never saved.
- `Verdiepingborden`: a text display (billboard, Dutch/English via lang) over every landing (trap_bordes_1..6): name, "Verdieping
  n van 6", up/down. Only while a player is within 48; `Getallen.TAG` so never saved.
- `UDeco`: registration + dev command `/guhs vadsrijk udeco vrij|bijen|borden`.

### Tools
- `tools/vadsrijk/zweefcheck.py [--zone 1|3|4|5|alle]`: lists decoration (flowers, plants, standing lanterns/lampions, lamp-post
  feet, knuffels, benches, bins) with air under it; lanterns/lampions with something above are reported as "hangt (zet
  hanging=true)". Exit 1 when anything floats.
- `tools/features/vadsrijk_u_deco.py` (FEATURES, before vadsrijk_t_vfx): the 13 board texts (nl + en_us; lang files updated).

## Decisions made myself
- Light radius 7 for the camp (posts every ~14 blocks along a path): 6 gave 115 posts (too busy), 8 left dark gaps.
- Name boards are text displays (Java), not signs in the tiles: sign text needs block-entity nbt, and u-hekken's pass skips
  every tile with block nbt (it cannot keep it), so signs would have cost the stair its connected fences.
- Benches are written as stairs-like `facing` = the backrest side.
- The arrival clearance is a new add-only kern hook (`Spawners.VERBODEN_VOOR`, knows the spawner) instead of raising
  `VEILIG_STRAAL` (that one also drives the zone-1 spawner PLAN and its gametests).
- Out of scope, noted only: zweefcheck still finds floating lanterns in Zwamzoet (zone 1, 112) and in the zone-5 part of tiles
  tx 38-39 (9); the hanging knuffels on the Knuffeltij washing lines are on purpose; the `uitkijk_nest` name/translation is
  another slice's item.

## Shared-file changes (all marked U-DECO)
- `kern/mob/Spawners.java`: `VERBODEN_VOOR` list + one check in `spawn()`.
- `VadsrijkFeature.java`: one register line.
- `zone4/verhaal/Z4VerhaalPlekken.java`: the knuffel-6 fallback coordinate (was Schelpie's spot).
- `tools/features/__init__.py`: one FEATURES line. Lang json: 13 keys added.
- Generators touched: kamp.py, zone3 bouw3.py / kit boom2.py / kit plekken2.py, zone4 bouw4.py / plan4.py / kit eilanden.py.

## Resume / open
- Generated tiles + plekken files are NOT committed: the integrator regenerates (zone 1, 3, 4 and then 5).
- Check results below.

## Checks (after regen of zone 1, 3, 4, 5 in this tree; baseline = the eaee06d08 tiles)
- `compileJava` OK. Gametest `-Pgt=UDeco` (UDecoGameTests: aankomstvrij, honingtakEnBorden): all passed.
- zweefcheck: zone 1 camp 0 (Zwamzoet 112, not mine), zone 3 267 -> 0, zone 4 39 -> 26 (16 = Knuffeltij washing-line knuffels,
  on purpose; 10 zone-5 lanterns in shared tiles).
- loopcheck camp box (-540,860 .. -190,1180): identical to baseline (PAD 39, TRAP 126, VAL 1: the old lighthouse cellar).
- loopcheck4: 0 findings; poicheck4: 0 of 372 not standable.
- loopcheck3: VAL 1 + 2 POI (masts) = baseline; PAD 396 -> 462: all 66 new ones are a spruce fence on a dirt_path with nothing
  above it = the **u-hekken** gap fill on the riding spiral rails (not this slice). Reported for u-hekken/integration.
- poicheck3: 1 of 430: `lift_kapel_boven` (1394,167,-642) gets a fence from the **u-hekken** diagonal gap fill (the fence ring
  round the Nestkapel lift shaft); baseline 0. Also for u-hekken: skip POI cells / path cells in the gap fill.
