"""Shared contract between the Elf-Guhjestocht core (tools/features/elftocht.py) and the village modules.

Each village is a module `elftocht_dorp_<nn>_<slug>.py` (nn = 02..11; village 01 Guhwarden is built by the core) that
exposes:

    INDEX = 2                    # 1..11, the fixed stamp order
    NAAM = "Snuh"                # Dutch display name (lang key gui.guhs.elftocht.dorp.<INDEX>)
    def bouw(s, ox, oy, oz) -> dict
    def check(s, ox, oy, oz)     # raise SystemExit("...") on any geometry problem

Coordinates / orientation (the village's OWN frame; since 2.10 the core builds every village on a little building site in
this frame and then turns the whole plot onto its place along the route, so a module never needs to think about it):
- The village plot is the box x in [ox, ox+PLOT_X), y in [oy, oy+PLOT_Y), z in [oz, oz+PLOT_Z) of the Structure `s`
  (a make_structures.Structure: s.set(x,y,z,name,props=None,nbt=None), s.get(x,y,z), s.fill(x0,y0,z0,x1,y1,z1,name,props=None,
  only_empty=False), s.entity(x,y,z,nbt)). Stay strictly inside the plot (the core checks it).
- The frozen canal runs WEST→EAST directly SOUTH of the plot (z >= oz+PLOT_Z). Skaters come from the west (x < ox) and leave
  to the east. So the village's "front" faces south (+z) towards the ice.
- The ground: the core lays the polder ground so that y = oy-1 is the solid ground layer under the whole plot (rijpgras/
  snow); build from y = oy upward (you may dig at most 3 blocks down for cellars/stairs). The ice surface of the canal is at
  y = oy-1 as well (ice level = ground level), so a skater can step straight onto the bank.
- Everything must be walkable from the canal edge (z = oz+PLOT_Z-1 row) — e.g. a little steiger/jetty with the Stempelguh on
  it is perfect. The Stempelguh must stand on solid ground within 3 blocks of the canal edge, facing the ice (yaw 0 = south).

Blocks: vanilla + existing guhs blocks (knuffelsteen*, pluisdak*, knuffelklinkers, vadshout*, *_gezicht guh faces, kaas blocks
…) + the guhpolder blocks being built in parallel (ids fixed, fine to reference): guhs:rijpgras, guhs:rijpsprietjes,
guhs:guh_ijsbloempje, guhs:polderijs, guhs:knotwilg_stam, guhs:knotwilg_bladeren, guhs:ijspegelguh_kristal,
guhs:guh_molentje. Winter look: snow layers, snow on roofs, lanterns/lampions, vuurkorven (campfire), flags (banners/wool),
guh faces everywhere. Every village must be UNIQUE and CUTE (see DESIGN_29.md §6.2 for each village's theme).

bouw() returns a dict:
    {"stempelguh": (x, y, z, yaw),            # REQUIRED: absolute Structure coords (feet position) + yaw
     "publiek":   [(x, y, z, yaw), ...],        # cheering audience guhs (2..8), on solid ground, near the ice
     "boost":     [(x, y, z), ...],             # warm drink spots (chocovet/snert stall counters) — optional
     "lichten":   [(x, y, z), ...]}             # lamp/lampion spots that should glow at night — optional
Use the helpers below for NPC entities so the NBT is right.
"""

PLOT_X, PLOT_Y, PLOT_Z = 28, 32, 24


def _byte(v):
    from make_structures import Byte  # noqa: WPS433 (tools/ on sys.path when the generators run)
    return Byte(v)


def _floats(*v):
    from make_structures import floats
    return floats(*v)


def _tags(*values):
    from make_structures import NbtList   # a typed NBT string list (a plain list can't be saved)
    return NbtList(8, list(values))


def stempelguh(s, x, y, z, yaw, index):
    """The Stempelguh of village `index` (1..11). x/y/z = feet position (block coords, entity placed in the block centre)."""
    s.entity(x + 0.5, y, z + 0.5, {"id": "guhs:guh_npc", "Kind": "stempelguh", "PersistenceRequired": _byte(1),
                                   "Rotation": _floats(float(yaw), 0.0), "Tags": _tags(f"guhs_elftocht_dorp_{index}")})


def publiek(s, x, y, z, yaw):
    """A cheering audience guh (the core makes them wave/cheer when a skater passes)."""
    s.entity(x + 0.5, y, z + 0.5, {"id": "guhs:guh", "PersistenceRequired": _byte(1), "Rotation": _floats(float(yaw), 0.0),
                                   "Tags": _tags("guhs_elftocht_publiek")})


def inside(ox, oy, oz, x, y, z):
    return ox <= x < ox + PLOT_X and oy - 3 <= y < oy + PLOT_Y and oz <= z < oz + PLOT_Z
