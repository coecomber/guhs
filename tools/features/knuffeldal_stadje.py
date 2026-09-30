"""
Het Knuffeldal (2.8) - the town knuffeldal_stadje: its templates (made with make_structures.Structure) and their
geometry self-checks.

  plein (49 x 24 x 49, ground top at y = G = 4; the anchor jigsaw guhs:knuffeldal_midden in the middle under the
  fountain): a promenade all around, four streets to the fountain in the middle (a big guh head with a face on every
  side, spouting into two basins), the Burgemeester's raadhuisbordes (NW), the grijpmachine arcade (NE), Opa Guh's
  kampvuurkring (SE: benches round a real campfire), the feestbuffet under a pergola (SW), lawns with pluizenbomen,
  bladerhoopjes and lamp posts, seizoensbloembakken and seizoensslingers everywhere (they follow the season), and nine
  jigsaws: the four building slots (bakkerij N, theehuis E, kapper S, creche W; 31 x H x 31 pieces of the other 2.8
  features, feature.knus.PleinSlot), the grijpmachine (under the arcade) and the four corners.
  hoek_noordoost / hoek_noordwest / hoek_zuidoost / hoek_zuidwest (40 x 30 x 31): a street with two guh houses each
  (one corner has Cocotje's little house, one a playground). The houses are big guh heads: a face on the front (the
  eyes are windows, the door is the mouth), round ears on the fluffy roof, a guh face on the back and on both sides
  (eye windows), flower boxes, a curly tail at the back, a chimney with a campfire; inside two floors with furniture
  and their resident guh. Front gardens with hedges and letterboxes, pluizenbomen behind, a picnic spot by the street.
  hoek_noordoost's street runs on to the town's east edge and ends in the town's ONE free street jigsaw
  (guhs:knuffeldal_straat_vrij -> pool guhs:knuffeldal_stadje/vrij: since 2.9 the Beroepenstraat, see VRIJ_STUKKEN).
  3.0 (timmerguh): hoek_noordwest's street runs on to the town's west edge and ends in the jigsaw of the Timmerguh's
  bouwplaats (guhs:knuffeldal_bouwplaats -> pool guhs:knuffeldal_stadje/bouwplaats; the template comes from
  tools/features/timmerguh_bouw.py, see BOUWPLAATS_*).

Every template goes through check(): everything is reachable on foot from the gate(s), NPCs and residents stand on a
floor, doors/ladders/beds are whole, nothing floats, lamps hang or stand on something, and the jigsaws are right.
build_all(h) writes the templates and pools (the slot templates come from the slot owners' generators).
"""
import math
import random
from collections import deque

G = 4
PLEIN = (49, 24, 49)
HOEK = (40, 30, 31)
ANCHOR = "guhs:knuffeldal_midden"
C = 24                       # the plein's middle (x and z)

KS = "guhs:knuffelsteen"
KS_TRAP = "guhs:knuffelsteen_trap"
KS_PLAAT = "guhs:knuffelsteen_plaat"
KS_MUUR = "guhs:knuffelsteen_muur"
KS_FACE = "guhs:knuffelsteen_gezicht"
DAK = "guhs:pluisdak"
DAK_TRAP = "guhs:pluisdak_trap"
DAK_PLAAT = "guhs:pluisdak_plaat"
KLINK = "guhs:knuffelklinkers"
GRAS = "guhs:knuffelgras"
PLUIS = "guhs:pluisgras"
BAK = "guhs:seizoensbloembak"
SLINGER = "guhs:seizoensslinger"
HOOP = "guhs:bladerhoopje"
TAFEL = "guhs:feestbuffettafel"
STAM = "guhs:pluizenboom_stam"
BLAD = "guhs:pluizenboom_bladeren"
PADDO = "guhs:guhpaddenstoel"
FLOWERS = ["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:guhoortjes", "minecraft:pink_tulip", "minecraft:allium"]
LAMP = "minecraft:lantern"
DOOR = "minecraft:cherry_door"
LADDER = "minecraft:ladder"

WALL_NAMES = ("_wall", "fence", "pane", "iron_bars", "chain")
THIN = ("lantern", "carpet", "sign", "_door", "ladder", "torch", "button", "pressure_plate", "rail", "flower_pot", "potted_", "candle",
        "seizoensslinger", "bladerhoopje", "pluisgras", "guhpaddenstoel", "vlaggetjes", "lampion")
PLANTS = set(FLOWERS) | {PLUIS, PADDO, "guhs:roze_gras", "guhs:kaasbloem"}
FACING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}


def solid(b):
    """Can you stand on it (a full block, stairs, slabs, a table...)?"""
    if b is None or b == "minecraft:air" or b == "minecraft:water" or b == "minecraft:jigsaw":
        return False
    if any(t in b for t in THIN) or b in PLANTS:
        return False
    return True


def passable(b):
    """Can your body be in it?"""
    return b is None or b in ("minecraft:air", "minecraft:jigsaw") or b in PLANTS or any(t in b for t in (
        "carpet", "_door", "ladder", "seizoensslinger", "bladerhoopje", "lantern", "flower_pot", "potted_", "sign", "lampion", "pressure_plate"))


class Bouw:
    """A template being built: blocks, entities, targets to reach, and what check() must verify."""

    def __init__(self, h, size, naam):
        self.h = h
        self.s = h.Structure(size)
        self.size = size
        self.naam = naam
        self.targets = {}          # name -> feet spot that must be reachable
        self.starts = []           # feet spots walking starts from (the gates)
        self.jigsaws = []          # (pos, name, target, pool, orientation)
        self.floors = []           # (x, y, z): npcs/guhs stand here
        self.rng = random.Random(sum(map(ord, naam)) * 97)
        self.gezichten = 0
        self.grote_gezichten = 0

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        self.s.fill(x0, y0, z0, x1, y1, z1, name, props)

    def air(self, x0, y0, z0, x1, y1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.s.blocks.pop((x, y, z), None)

    def jigsaw(self, x, y, z, orientation, name, target, pool, final, joint="rollable", sel=0):
        self.set(x, y, z, "minecraft:jigsaw", {"orientation": orientation},
                 {"id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool, "final_state": final, "joint": joint,
                  "selection_priority": sel, "placement_priority": 0})
        self.jigsaws.append(((x, y, z), name, target, pool, orientation))

    def npc(self, x, y, z, kind, facing):
        ms = self.h.ms
        self.s.entity(x + 0.5, float(y), z + 0.5, {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1),
                                                   "Rotation": ms.floats(YAW[facing], 0.0)})
        self.floors.append((x, y, z))
        self.targets[f"npc {kind}"] = (x, y, z)

    def bewoner(self, x, y, z, naam, variant="normal", scale=1.0, facing="south"):
        ms = self.h.ms
        nbt = ms.guh_nbt(scale, Variant=variant, Rotation=ms.floats(YAW[facing], 0.0),
                         NeoForgeData={"guhs_knus_bewoner": ms.Byte(1), "guhs_knus_bewoner_naam": naam, "guhs_knuffeldal_checked": ms.Byte(1)})
        self.s.entity(x + 0.5, float(y), z + 0.5, nbt)
        self.floors.append((x, y, z))
        self.targets[f"bewoner {naam}"] = (x, y, z)

    # --- small pieces ------------------------------------------------------------------------------------------------

    def lantaarnpaal(self, x, z, height=4):
        """A lamp post: knuffelsteen foot with a little face, a wall post, a lantern on top."""
        self.set(x, G + 1, z, KS_FACE, {"facing": self.rng.choice(list(FACING)), "stemming": str(self.rng.randrange(4))})
        self.gezichten += 1
        for y in range(G + 2, G + 1 + height):
            self.set(x, y, z, KS_MUUR, {"up": "true"})
        self.set(x, G + 1 + height, z, LAMP, {"hanging": "false", "waterlogged": "false"})
        return G + height

    def bloembak(self, x, z, y=None):
        self.set(x, (G + 1) if y is None else y, z, BAK, {"seizoen": "lente"})

    def slinger(self, x0, z0, x1, z1, y):
        """A garland from (x0, z0) to (x1, z1) (a straight line along x or z) at height y."""
        axis = "x" if z0 == z1 else "z"
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                if self.get(x, y, z) is None:
                    self.set(x, y, z, SLINGER, {"seizoen": "lente", "axis": axis})

    def boompje(self, x, z, height=4, r=2.6):
        """A little pluizenboom: a trunk and a round fluffy crown."""
        for y in range(G + 1, G + 1 + height):
            self.set(x, y, z, STAM, {"axis": "y"})
        cy = G + height + 1.5
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                for dy in range(-2, 3):
                    if (dx * dx + dz * dz) / (r * r) + (dy * dy) / (1.8 ** 2) <= 1.0 and self.get(x + dx, int(cy + dy), z + dz) is None:
                        self.set(x + dx, int(cy + dy), z + dz, BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})

    def bank(self, x, z, facing, y=None):
        self.set(x, G + 1 if y is None else y, z, "guhs:guh_bank", {"facing": facing})

    def grasveld(self, x0, z0, x1, z1, dichtheid=0.18):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if self.get(x, G, z) == GRAS and self.get(x, G + 1, z) is None and self.rng.random() < dichtheid:
                    r = self.rng.random()
                    self.set(x, G + 1, z, PLUIS if r < 0.55 else self.rng.choice(FLOWERS) if r < 0.93 else PADDO)


# =====================================================================================================================
# a guh face in blocks
# =====================================================================================================================
def face_role(u, v, R):
    """Where on a guh face of radius R (u right, v up from its middle): skin, eye, shine, ring, nose, mouth, cheek."""
    role = None
    if (u / (R + 0.4)) ** 2 + (v / (0.9 * R + 0.4)) ** 2 <= 1:
        role = "skin"
        for sx in (-1, 1):
            ex, ey = sx * 0.42 * R, 0.1 * R
            d = math.dist((u, v), (ex, ey))
            er = 0.27 * R + 0.35
            if d <= er:
                role = "ring" if (v < ey - 0.25 * er and d > 0.45 * er) else "eye"
                if round(u) == round(ex - 0.3 * er) and round(v) == round(ey + 0.35 * er):
                    role = "shine"
            if math.dist((u, v), (sx * 0.7 * R, -0.35 * R)) <= 0.13 * R + 0.25:
                role = "cheek"
        if math.dist((u, v), (0, -0.28 * R)) <= 0.08 * R + 0.3:
            role = "nose"
        y0 = round(-0.46 * R)
        if (round(u), round(v)) in {(-1, y0), (0, y0 - 1), (1, y0)} or (R >= 5 and (round(u), round(v)) in {(-2, y0 + 1), (2, y0 + 1)}):
            role = "mouth"
    return role


FACE_BLOCKS = {"skin": "minecraft:pink_wool", "eye": "minecraft:black_concrete", "shine": "minecraft:white_concrete",
               "ring": "minecraft:light_blue_concrete", "nose": "minecraft:magenta_concrete", "mouth": "minecraft:purple_concrete",
               "cheek": "minecraft:pink_concrete"}


def face_on_plane(b, cx, cy, cz, facing, R, blocks=None, only_features=False, skip=None):
    """Paints a guh face (radius R) onto the plane facing `facing` (the face looks that way), centred on (cx, cy, cz)."""
    blocks = blocks or FACE_BLOCKS
    dx, dz = FACING[facing]
    n = int(R + 1)
    for du in range(-n, n + 1):
        for dv in range(-n, n + 1):
            role = face_role(du, dv, R)
            if role is None or (only_features and role == "skin"):
                continue
            # u runs to the face's right: facing south -> +x is its left... (a face looking south: its right is west)
            rx, rz = (-dz, dx)
            x, y, z = cx + rx * du, cy + dv, cz + rz * du
            if skip and skip(x, y, z, role):
                continue
            b.set(x, y, z, blocks[role])
    b.grote_gezichten += 1


WINDOW_ROLES = ("eye", "ring", "shine")


def face_on_wall(b, cells, inner, facing, cu, cy, R, blocks, skip=None):
    """Paints a guh face (radius R) onto a curved house wall: every face column lands on the outermost wall block of its
    row (the wall's footprint `cells`, the room inside `inner`), so it follows the round wall. The eyes are windows: where
    the wall is two blocks thick the glass goes through both."""
    dx, dz = FACING[facing]
    rx, rz = (-dz, dx)
    n = int(R + 1)
    for du in range(-n, n + 1):
        if dx:
            z = cu + rz * du
            row = [x for (x, zz) in cells if zz == z]
            if not row:
                continue
            x = max(row) if dx > 0 else min(row)
        else:
            x = cu + rx * du
            col = [zz for (xx, zz) in cells if xx == x]
            if not col:
                continue
            z = max(col) if dz > 0 else min(col)
        for dv in range(-n, n + 1):
            role = face_role(du, dv, R)
            if role is None or role == "skin":
                continue
            y = cy + dv
            if skip and skip(x, y, z, role):
                continue
            b.set(x, y, z, blocks[role])
            if role in WINDOW_ROLES and (x - dx, z - dz) in cells and (x - dx, z - dz) not in inner:
                b.set(x - dx, y, z - dz, blocks[role])
    b.grote_gezichten += 1


# =====================================================================================================================
# the plein
# =====================================================================================================================
def plein(h):
    b = Bouw(h, PLEIN, "plein")
    W, _, D = PLEIN
    mc = h.mc
    # the ground: pink wool, knuffelgras on top
    for x in range(W):
        for z in range(D):
            for y in range(G):
                b.set(x, y, z, mc("pink_wool"))
            b.set(x, G, z, GRAS)
    # the promenade all round (inset 2..4), the four streets to the middle (x or z 22..26), the round plaza, the gates
    for x in range(W):
        for z in range(D):
            if weg(x, z):
                pattern = plaza(x, z) and (x + z) % 4 == 0 and math.dist((x, z), (C, C)) > 8.5
                b.set(x, G, z, KS if pattern else KLINK)
    fountain(b)
    raadhuis(b)
    arcade(b)
    kampvuur(b)
    buffet(b)
    tuinen(b)
    # 2.10.1: a Reisguh on the plaza south of the fountain, looking at it: the town's travel waypoint
    from features import reisguh_plek
    spot = reisguh_plek.zet(b.s, reisguh_plek.rondom(C + 3, G + 1, C + 10), "Knuffeldal", YAW["north"], h.ms.Byte, h.ms.floats,
                            "(Knuffeldal stadje)")
    b.floors.append(spot)
    b.targets["npc reisguh"] = spot
    # the jigsaws: the four slots (first), the grijpmachine, the four corners, the anchor
    f = KLINK
    b.jigsaw(24, G, 0, "north_up", "guhs:plein_bakkerij", "guhs:plein_ingang", "guhs:knuffeldal_stadje/bakkerij", f, sel=10)
    b.jigsaw(48, G, 24, "east_up", "guhs:plein_theehuis", "guhs:plein_ingang", "guhs:knuffeldal_stadje/theehuis", f, sel=10)
    b.jigsaw(24, G, 48, "south_up", "guhs:plein_kapper", "guhs:plein_ingang", "guhs:knuffeldal_stadje/kapper", f, sel=10)
    b.jigsaw(0, G, 24, "west_up", "guhs:plein_creche", "guhs:plein_ingang", "guhs:knuffeldal_stadje/creche", f, sel=10)
    b.jigsaw(38, G, 10, "up_south", "guhs:plein_grijpmachine", "guhs:grijpmachine_voet", "guhs:knuffeldal_stadje/grijpmachine", f,
             joint="aligned", sel=8)
    for (x, z, facing, hoeknaam) in ((44, 0, "north", "noordoost"), (4, 0, "north", "noordwest"), (44, 48, "south", "zuidoost"),
                                     (4, 48, "south", "zuidwest")):
        b.jigsaw(x, G, z, f"{facing}_up", "guhs:plein_hoek", "guhs:hoek_ingang", f"guhs:knuffeldal_stadje/hoek_{hoeknaam}", f, sel=5)
    b.jigsaw(C, G, C, "up_north", ANCHOR, "minecraft:empty", "minecraft:empty", KS)
    # walking starts at the four slot entrances and two corner gates
    b.starts = [(24, G + 1, 1), (47, G + 1, 24), (24, G + 1, 47), (1, G + 1, 24), (44, G + 1, 1), (4, G + 1, 47)]
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=PLEIN[1])
    return b


def plaza(x, z):
    return math.dist((x, z), (C, C)) <= 12.5


def weg(x, z):
    """Paved: the promenade (inset 2..4), the streets (22..26), the plaza, the corner gates (x 3..5 and 43..45)."""
    ring = (2 <= x <= 46 and 2 <= z <= 46) and (min(x, 48 - x) <= 4 or min(z, 48 - z) <= 4)
    street = 22 <= x <= 26 or 22 <= z <= 26
    gate = min(x, 48 - x) in (3, 4, 5) and (z <= 2 or z >= 46)
    return ring or street or plaza(x, z) or gate


def fountain(b):
    """The guh fountain: a round basin, a bowl on a pedestal, and on top a big guh head with a face on every side."""
    mc = b.h.mc
    for x in range(C - 8, C + 9):
        for z in range(C - 8, C + 9):
            d = math.dist((x, z), (C, C))
            if d <= 7.5:
                b.set(x, G, z, KS)
                if d > 6.5:
                    b.set(x, G + 1, z, KS)
                    b.set(x, G + 2, z, KS_PLAAT, {"type": "bottom"})
                elif d > 1.6:
                    b.set(x, G + 1, z, mc("water"))
    for y in range(G + 1, G + 4):                                 # the pedestal
        for x in range(C - 1, C + 2):
            for z in range(C - 1, C + 2):
                b.set(x, y, z, KS)
    for x in range(C - 3, C + 4):                                 # the bowl
        for z in range(C - 3, C + 4):
            d = math.dist((x, z), (C, C))
            if d <= 3.3:
                b.set(x, G + 4, z, KS)
                if d > 2.4:
                    b.set(x, G + 5, z, KS_PLAAT, {"type": "bottom"})
                elif d > 1.5:
                    b.set(x, G + 5, z, mc("water"))
    # the head: 7 x 7 x 6 of pink wool on a neck, a face on all four sides, two round ears, a knabbel on top
    y0 = G + 6
    for x in range(C - 1, C + 2):
        for z in range(C - 1, C + 2):
            b.set(x, G + 5, z, mc("pink_wool"))
    for x in range(C - 3, C + 4):
        for y in range(y0, y0 + 6):
            for z in range(C - 3, C + 4):
                if not (abs(x - C) == 3 and abs(z - C) == 3 and y in (y0, y0 + 5)):
                    b.set(x, y, z, mc("pink_wool"))
    for facing, (dx, dz) in FACING.items():
        face_on_plane(b, C + dx * 3, y0 + 3, C + dz * 3, facing, 3.4, only_features=True)
    for sx in (-1, 1):
        for du in range(-2, 3):
            for dv in range(-2, 3):
                r2 = du * du + dv * dv
                if r2 <= 5:
                    b.set(C + sx * 3 + du, y0 + 7 + dv, C, mc("magenta_terracotta") if r2 <= 1 else mc("pink_wool"))
    b.set(C, y0 + 6, C, "guhs:block_of_kaasknabbels")
    b.targets["de fontein"] = (C, G + 1, C + 9)


def raadhuis(b):
    """The raadhuisbordes (NW): a raised stage with steps, a backdrop with a big guh face, flags, the Burgemeester."""
    mc = b.h.mc
    x0, x1, z0, z1 = 5, 16, 5, 14
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, KS)
            b.set(x, G + 1, z, KS)
    for x in range(x0, x1 + 1):                                   # the steps down to the plaza
        b.set(x, G + 1, z1 + 1, KS_TRAP, {"facing": "north", "half": "bottom", "shape": "straight"})
    for z in range(z0, z1 + 1):
        b.set(x1 + 1, G + 1, z, KS_TRAP, {"facing": "west", "half": "bottom", "shape": "straight"})
    b.set(x1 + 1, G + 1, z1 + 1, KS_TRAP, {"facing": "north", "half": "bottom", "shape": "outer_left"})
    # the backdrop: a wall with a pluisdak top and a big guh face; pillars with faces and lanterns
    for x in range(x0, x1 + 1):
        for y in range(G + 2, G + 12):
            b.set(x, y, z0, KS)
        b.set(x, G + 12, z0, DAK_PLAAT, {"type": "bottom"})
    for x in (x0, x1):
        for z in range(z0, z0 + 3):
            for y in range(G + 2, G + 11):
                b.set(x, y, z, KS)
        b.set(x, G + 11, z0 + 1, KS_FACE, {"facing": "south", "stemming": "0"})
        b.gezichten += 1
        b.set(x, G + 12, z0 + 1, LAMP, {"hanging": "false", "waterlogged": "false"})
    face_on_plane(b, (x0 + x1) // 2, G + 7, z0, "south", 3.6)
    b.slinger(x0 + 1, z0 + 2, x1 - 1, z0 + 2, G + 10)
    for x in range(x0 + 1, x1):                                  # (the garland hangs from a beam between the pillars)
        b.set(x, G + 11, z0 + 2, KS_PLAAT, {"type": "top"})
    b.set(12, G + 2, 11, mc("lectern"), {"facing": "south", "has_book": "false", "powered": "false"})
    b.npc(10, G + 2, 11, "burgemeesterguh", "south")
    for x in (x0 + 1, x1 - 1):
        b.bloembak(x, z1, y=G + 2)


def arcade(b):
    """The grijpmachine arcade (NE): an open pavilion with a pluisdak roof; the machine (its own piece) stands in it."""
    x0, x1, z0, z1 = 33, 43, 5, 15
    pillars = ((x0, z0), (x1, z0), (x0, z1), (x1, z1), (x0, 10), (x1, 10))
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, KLINK)
    for (x, z) in pillars:
        for y in range(G + 1, G + 8):
            b.set(x, y, z, KS_MUUR, {"up": "true"})
        b.set(x, G + 8, z, KS_FACE, {"facing": "south" if z > 10 else "north" if z < 10 else ("west" if x == x0 else "east"),
                                     "stemming": str((x + z) % 4)})
        b.gezichten += 1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = min(x - x0, x1 - x, z - z0, z1 - z)
            b.set(x, G + 9 + min(edge, 4), z, DAK)
            if edge >= 1:
                b.set(x, G + 8 + min(edge, 4), z, DAK)            # (each step rests on the one below it)
            if edge == 0 and (x, z) not in pillars:
                b.set(x, G + 8, z, DAK_PLAAT, {"type": "top"})
    b.set(38, G + 14, 10, LAMP, {"hanging": "false", "waterlogged": "false"})
    for (x, z) in ((35, 5), (41, 5), (35, 15), (41, 15)):
        b.set(x, G + 7, z, LAMP, {"hanging": "true", "waterlogged": "false"})
    # (x 36..40, y 5..10, z 8..12 stays free for the grijpmachine piece)
    b.bank(34, 13, "east")
    b.bank(42, 13, "west")
    b.bloembak(x0 + 1, z1 + 1)
    b.bloembak(x1 - 1, z1 + 1)
    b.targets["de grijpmachine"] = (38, G + 1, 14)


def kampvuur(b):
    """Opa Guh's kampvuurkring (SE): a real campfire, benches round it, logs, a pluizenboom, Opa on his bench."""
    mc = b.h.mc
    cx, cz = 35, 35
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = math.dist((x, z), (cx, cz))
            if d <= 5.2:
                b.set(x, G, z, KLINK if d > 1.5 else KS)
    b.set(cx, G + 1, cz, mc("campfire"), {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for (x, z, facing) in ((cx, cz - 3, "south"), (cx - 3, cz, "east"), (cx + 3, cz, "west"), (cx - 2, cz - 2, "south"),
                           (cx + 2, cz - 2, "south")):
        b.bank(x, z, facing)
    for (x, z, axis) in ((cx + 2, cz + 2, "x"), (cx - 2, cz + 2, "x")):
        b.set(x, G + 1, z, STAM, {"axis": axis})
    # Opa's own bench: a long bench, Opa in its middle, looking at the fire
    b.bank(cx - 1, cz + 4, "north")
    b.bank(cx + 1, cz + 4, "north")
    b.npc(cx, G + 1, cz + 4, "opa_guh", "north")
    b.boompje(cx + 6, cz + 6, height=5, r=3.0)
    b.targets["het kampvuur"] = (cx, G + 1, cz - 4)


def buffet(b):
    """The feestbuffet (SW): a pergola with garlands, two long rows of feestbuffettafels with chairs."""
    x0, x1, z0, z1 = 5, 18, 32, 43
    zm = 37
    posts = ((x0, z0), (x1, z0), (x0, z1), (x1, z1), (x0, zm), (x1, zm))
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, KLINK)
    for (x, z) in posts:
        for y in range(G + 1, G + 7):
            b.set(x, y, z, STAM, {"axis": "y"})
        b.set(x, G + 7, z, KS_FACE, {"facing": "north" if z == z0 else "south" if z == z1 else ("west" if x == x0 else "east"),
                                     "stemming": str((x * 3 + z) % 4)})
        b.gezichten += 1
    for x in range(x0, x1 + 1):                                   # the beams
        for z in (z0, zm, z1):
            b.set(x, G + 8, z, DAK_PLAAT, {"type": "bottom"})
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            b.set(x, G + 8, z, DAK_PLAAT, {"type": "bottom"})
    for z in (z0, zm, z1):                                        # garlands under the beams
        for x in range(x0 + 1, x1):
            b.set(x, G + 7, z, SLINGER, {"seizoen": "lente", "axis": "x"})
    for x in range(8, 16):
        b.set(x, G + 1, 35, TAFEL, {"facing": "south", "gedekt": "false"})
        b.set(x, G + 1, 39, TAFEL, {"facing": "north", "gedekt": "false"})
        if x % 2:
            b.set(x, G + 1, 34, "guhs:guh_stoel", {"facing": "south"})
        else:
            b.set(x, G + 1, 40, "guhs:guh_stoel", {"facing": "north"})
    b.set(7, G + 1, 37, "guhs:guh_taart", {"bites": "0"})
    b.set(16, G + 1, 37, "guhs:guh_taart", {"bites": "0"})
    b.bloembak(x0 + 1, z0 + 1)
    b.bloembak(x1 - 1, z1 - 1)
    b.targets["het feestbuffet"] = (12, G + 1, 37)


def gras_plekken(b, n, spacing=4):
    """n free spots on the lawns (knuffelgras, nothing on it, not next to a road), spread out."""
    spots = []
    cells = [(x, z) for x in range(1, 48) for z in range(1, 48) if b.get(x, G, z) == GRAS and b.get(x, G + 1, z) is None
             and not any(weg(x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1))]
    b.rng.shuffle(cells)
    for c in cells:
        if all(math.dist(c, s) >= spacing for s in spots):
            spots.append(c)
            if len(spots) >= n:
                break
    return spots


def tuinen(b):
    """The lawns, trees, lamp posts, flower boxes, garlands over the streets, bladerhoopjes, benches round the plaza."""
    # lamp posts beside the four streets; garlands across the streets between them; lamp posts in the corners
    for d in (9, 39):
        for side in (21, 27):
            for (x, z) in ((side, d), (d, side)):
                if b.get(x, G + 1, z) is None:
                    b.lantaarnpaal(x, z)
        b.slinger(22, d, 26, d, G + 5)
        b.slinger(d, 22, d, 26, G + 5)
    for (x, z) in ((3, 3), (45, 3), (3, 45), (45, 45)):
        if b.get(x, G + 1, z) is None:
            b.lantaarnpaal(x, z)
    # flower boxes round the plaza and at the street ends (on the paving)
    for (x, z) in ((C - 6, C - 6), (C + 6, C - 6), (C - 6, C + 6), (C + 6, C + 6), (C - 3, 5), (C + 3, 5), (C - 3, 43), (C + 3, 43),
                   (5, C - 3), (5, C + 3), (43, C - 3), (43, C + 3)):
        if b.get(x, G + 1, z) is None:
            b.bloembak(x, z)
    # trees and leaf piles on the lawns
    for (x, z) in gras_plekken(b, 5, spacing=9):
        b.boompje(x, z, height=4, r=2.4)
    for (x, z) in gras_plekken(b, 10, spacing=5):
        b.set(x, G + 1, z, HOOP, {"vol": "false"})
    b.grasveld(0, 0, 48, 48)
    # benches round the plaza, looking at the fountain
    for (x, z, facing) in ((C - 10, C - 3, "east"), (C - 10, C + 3, "east"), (C + 10, C - 3, "west"), (C + 10, C + 3, "west"),
                           (C - 3, C + 10, "north"), (C + 3, C + 10, "north"), (C - 3, C - 10, "south"), (C + 3, C - 10, "south")):
        if b.get(x, G + 1, z) is None:
            b.bank(x, z, facing)


# =====================================================================================================================
# the houses
# =====================================================================================================================
HUIZEN = {
    # name: (bewoner, variant, size (w, d), wall, roof, ear, ear_in, eye ring glass, mood, scale)
    "pluisje": ("pluisje", "pluisguh", (15, 15), KS, DAK, "minecraft:pink_wool", "minecraft:magenta_terracotta",
                "minecraft:light_blue_stained_glass", 0, 1.0),
    "knabbeltje": ("knabbeltje", "normal", (15, 14), "minecraft:white_terracotta", "minecraft:yellow_wool", "minecraft:yellow_wool",
                   "minecraft:orange_terracotta", "minecraft:cyan_stained_glass", 3, 0.9),
    "dikkie": ("dikkie", "normal", (16, 15), KS, "minecraft:magenta_wool", "minecraft:pink_wool", "minecraft:purple_wool",
               "minecraft:light_blue_stained_glass", 0, 1.35),
    "mollie": ("mollie", "pluisguh", (15, 15), "minecraft:pink_terracotta", DAK, "minecraft:white_wool", "minecraft:pink_wool",
               "minecraft:lime_stained_glass", 1, 0.85),
    "sproetje": ("sproetje", "normal", (15, 14), KS, "minecraft:light_blue_wool", "minecraft:pink_wool", "minecraft:light_blue_terracotta",
                 "minecraft:light_blue_stained_glass", 2, 0.8),
    "bolletje": ("bolletje", "pluisguh", (16, 15), "minecraft:white_terracotta", DAK, "minecraft:pink_wool", "minecraft:magenta_terracotta",
                 "minecraft:cyan_stained_glass", 3, 1.2),
}


def pane(ns=True):
    return {"east": str(not ns).lower(), "west": str(not ns).lower(), "north": str(ns).lower(), "south": str(ns).lower(), "waterlogged": "false"}


def guhhuis(b, name, x0, z0):
    """A big guh head house with its front (the face, the door as its mouth) to +z; returns the feet spot outside its door."""
    mc = b.h.mc
    bewoner, variant, (w, d), wall, roof, ear, ear_in, ring, mood, scale = HUIZEN[name]
    x1, z1 = x0 + w - 1, z0 + d - 1
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    rx, rz = w / 2, d / 2
    y_wall_top = G + 10                        # two floors: G (ground floor) and G + 5 (upper floor)

    def inside(x, z, shrink=0.0):
        return ((x - cx) / (rx - shrink)) ** 4 + ((z - cz) / (rz - shrink)) ** 4 <= 1.0

    cells = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) if inside(x, z)]
    inner = {(x, z) for (x, z) in cells if inside(x, z, 1.0)}
    for (x, z) in cells:
        b.set(x, G, z, mc("cherry_planks") if (x, z) in inner else KS)
        for y in range(G + 1, y_wall_top + 1):
            if (x, z) in inner:
                b.air(x, y, z, x, y, z)
            else:
                b.set(x, y, z, wall)
        if (x, z) in inner:
            b.set(x, G + 5, z, mc("cherry_planks"))              # the upper floor
    # the roof: a fluffy dome like the top of a guh head
    top = y_wall_top + 1
    roof_top = {}
    for (x, z) in cells:
        dist = min(1.0, (((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2) ** 0.5)
        hgt = int(round(8.0 * math.sqrt(max(0.0, 1 - dist ** 2))))
        for y in range(top, top + hgt + 1):
            b.set(x, y, z, roof)
        roof_top[(x, z)] = top + hgt
    # the ears on the roof: two round discs looking to the front, their inside coloured
    for sx in (-1, 1):
        ex, ez, ey = int(round(cx + sx * (rx - 3.2))), int(round(cz)), top + 6
        for du in range(-3, 4):
            for dv in range(-3, 4):
                r2 = du * du + dv * dv
                if r2 <= 10:
                    b.set(ex + du, ey + dv, ez + 1, ear_in if r2 <= 4 else ear)
                    b.set(ex + du, ey + dv, ez, ear)
    # the face on the front wall (z1): the eyes are windows, blush and a nose; the door (the mouth) in the middle
    fx = int(round(cx))
    blocks = dict(FACE_BLOCKS)
    blocks.update({"eye": "minecraft:black_stained_glass", "ring": ring, "shine": "minecraft:white_stained_glass", "skin": wall})
    face_on_plane(b, fx, G + 6, z1, "south", 5.0 if w >= 15 else 4.4, blocks=blocks, only_features=True,
                  skip=lambda x, y, z, role: y <= G + 3 and abs(x - fx) <= 1)
    b.set(fx, G + 1, z1, DOOR, {"facing": "north", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    b.set(fx, G + 2, z1, DOOR, {"facing": "north", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    for x in (fx - 1, fx, fx + 1):                                # a little porch roof with a lantern
        b.set(x, G + 4, z1 + 1, DAK_PLAAT, {"type": "bottom"})
    b.set(fx, G + 3, z1 + 1, LAMP, {"hanging": "true", "waterlogged": "false"})
    b.set(fx - 1, G + 1, z1 + 1, mc("potted_pink_tulip"))
    b.set(fx + 1, G + 1, z1 + 1, mc("potted_allium"))
    # the side walls: a guh face upstairs (its eyes are the windows), a wide window downstairs with a flower box under
    # it, and little knuffelsteen faces - people walk all round the houses in the meadow
    wz = int(round(cz))
    for side, pick, sdx in (("west", min, -1), ("east", max, 1)):
        face_on_wall(b, cells, inner, side, wz, G + 7, 4.0, blocks)
        for dzz in (-1, 0, 1):
            row = [x for (x, z) in cells if z == wz + dzz]
            ex = pick(row)
            for y in (G + 2, G + 3):
                b.set(ex, y, wz + dzz, mc("pink_stained_glass_pane"), pane(True))
            b.bloembak(ex + sdx, wz + dzz)
        for zz in (z0 + 3, z1 - 3):
            row = [x for (x, z) in cells if z == zz]
            b.set(pick(row), G + 4, zz, KS_FACE, {"facing": side, "stemming": str((zz + mood) % 4)})
            b.gezichten += 1
    # the back: a sleepy guh face upstairs (round eye windows), a curly guh tail against the wall, flower boxes beside it
    face_on_wall(b, cells, inner, "north", fx, G + 7, 4.4, blocks)
    for (u, v) in ((0, 1), (0, 2), (1, 3), (2, 3), (3, 2), (3, 1), (2, 0)):
        b.set(fx + u, G + v, z0 - 1, mc("pink_wool"))
    for x in (fx - 3, fx - 2):
        col = [z for (xx, z) in cells if xx == x]
        b.bloembak(x, min(col) - 1)
    # the chimney with a campfire (a little smoke) at the back right
    chx, chz = int(round(cx + rx * 0.45)), int(round(cz - rz * 0.4))
    ctop = roof_top.get((chx, chz), top)
    for y in range(top, ctop + 3):
        b.set(chx, y, chz, mc("bricks"))
    b.set(chx, ctop + 3, chz, mc("campfire"), {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    # inside: the ground floor (table, chairs, cupboard, a rug, a lantern) and upstairs (bed, bean bag, a lantern)
    iz0, iz1 = min(z for x, z in inner), max(z for x, z in inner)
    ix0 = min(x for x, z in inner)
    ci, cj = int(cx), int(cz)
    b.set(ci - 2, G + 1, cj, "guhs:guh_tafel", {"facing": "south"})
    b.set(ci - 3, G + 1, cj, "guhs:guh_stoel", {"facing": "east"})
    b.set(ci - 2, G + 1, cj - 1, "guhs:guh_stoel", {"facing": "south"})
    b.set(ix0 + 1, G + 1, iz0 + 2, "guhs:guh_kast", {"facing": "east", "open": "false"})
    for x in range(ci + 1, ci + 4):
        for z in range(cj - 1, cj + 2):
            if (x, z) in inner and b.get(x, G + 1, z) is None:
                b.set(x, G + 1, z, mc("pink_carpet"))
    b.set(ci, G + 4, cj, LAMP, {"hanging": "true", "waterlogged": "false"})
    lx = ci - 3                                                   # the ladder up, on the back wall
    for y in range(G + 1, G + 6):
        b.set(lx, y, iz0, LADDER, {"facing": "south", "waterlogged": "false"})
    by, bx = G + 6, ci + 2
    b.set(bx, by, iz0, mc("pink_bed"), {"facing": "north", "part": "head", "occupied": "false"})
    b.set(bx, by, iz0 + 1, mc("pink_bed"), {"facing": "north", "part": "foot", "occupied": "false"})
    b.set(bx + 1, by, iz0, mc("flower_pot"))
    b.set(ci - 2, by, iz1 - 1, "guhs:pink_zitzak", {"facing": "north"})
    b.set(ci, G + 10, cj, LAMP, {"hanging": "true", "waterlogged": "false"})
    b.bewoner(ci + 1, G + 1, cj + 2, bewoner, variant, scale, "south")
    b.targets[f"boven bij {name}"] = (ci, by, cj + 1)
    return (fx, G + 1, z1 + 2)


def cocotje_huis(b, x0, z0):
    """Cocotje's little house: a small round guh head with long ears hanging down its sides, a bow on top, a garden."""
    mc = b.h.mc
    w = 11
    x1, z1 = x0 + w - 1, z0 + w - 1
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    r = w / 2

    def inside(x, z, shrink=0.0):
        return math.dist((x, z), (cx, cz)) <= r - shrink

    cells = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) if inside(x, z)]
    inner = {(x, z) for (x, z) in cells if inside(x, z, 1.1)}
    for (x, z) in cells:
        b.set(x, G, z, mc("cherry_planks") if (x, z) in inner else KS)
        for y in range(G + 1, G + 6):
            if (x, z) not in inner:
                b.set(x, y, z, "minecraft:pink_terracotta")
    top = G + 6
    for (x, z) in cells:
        dist = min(1.0, math.dist((x, z), (cx, cz)) / r)
        for y in range(top, top + int(round(4.5 * math.sqrt(max(0.0, 1 - dist ** 2)))) + 1):
            b.set(x, y, z, "minecraft:white_wool" if (x + y + z) % 5 else "minecraft:pink_wool")
    ci, cj = int(round(cx)), int(round(cz))
    # the hanging ears: from the top of the dome down along both sides, almost to the ground
    for sx in (-1, 1):
        ex = ci + sx * int(round(r + 0.4))
        for y in range(G + 1, top + 3):
            for zz in (cj - 1, cj, cj + 1):
                b.set(ex, y, zz, "minecraft:magenta_terracotta" if zz == cj and G + 3 <= y <= top else "minecraft:pink_wool")
        b.set(ex - sx, top + 2, cj, "minecraft:pink_wool")
    # the bow on top
    ty = top + 5
    for (u, v) in ((-2, 0), (-1, 0), (-2, 1), (1, 0), (2, 0), (2, 1), (0, 0)):
        b.set(ci + u, ty + v, cj, "minecraft:red_wool" if u else "minecraft:pink_concrete")
    # the face and the door
    blocks = dict(FACE_BLOCKS)
    blocks.update({"eye": "minecraft:black_stained_glass", "ring": "minecraft:light_blue_stained_glass",
                   "shine": "minecraft:white_stained_glass", "skin": "minecraft:pink_terracotta"})
    face_on_plane(b, ci, G + 4, z1, "south", 2.8, blocks=blocks, only_features=True, skip=lambda x, y, z, role: y <= G + 2 and x == ci)
    b.set(ci, G + 1, z1, DOOR, {"facing": "north", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    b.set(ci, G + 2, z1, DOOR, {"facing": "north", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    # the back: a little face too (round eye windows), with flower boxes
    face_on_wall(b, cells, inner, "north", ci, G + 3, 2.8, blocks)
    for x in (ci - 3, ci + 3):
        col = [z for (xx, z) in cells if xx == x]
        b.bloembak(x, min(col) - 1)
    # inside: Cocotje, a little table, a bean bag, a lantern
    b.npc(ci, G + 1, z1 - 3, "cocotje", "south")
    b.set(ci - 2, G + 1, cj - 1, "guhs:guh_tafel", {"facing": "south"})
    b.set(ci + 2, G + 1, cj - 1, "guhs:pink_zitzak", {"facing": "south"})
    b.set(ci, G + 5, cj, LAMP, {"hanging": "true", "waterlogged": "false"})
    # the garden: flowers, a bench, a flower box
    for x in range(x0, x1 + 1):
        if abs(x - ci) > 1 and b.get(x, G + 1, z1 + 2) is None:
            b.set(x, G + 1, z1 + 2, b.rng.choice(FLOWERS))
    b.bank(x0, z1 + 1, "east")
    b.bloembak(x1, z1 + 1)
    return (ci, G + 1, z1 + 2)


def speeltuin(b, x0, z0, x1, z1):
    """A playground: a guh climbing dome, a slide, swings, a sandbox with a sand castle, a bench, a tree."""
    mc = b.h.mc
    for x in range(x0 + 1, x0 + 7):                               # the sandbox
        for z in range(z0 + 1, z0 + 7):
            edge = x in (x0 + 1, x0 + 6) or z in (z0 + 1, z0 + 6)
            b.set(x, G, z, KS if edge else mc("sand"))
            if edge:
                b.set(x, G + 1, z, KS_PLAAT, {"type": "bottom"})
    for (x, z, y) in ((x0 + 3, z0 + 3, G + 1), (x0 + 4, z0 + 3, G + 1), (x0 + 3, z0 + 4, G + 1), (x0 + 4, z0 + 4, G + 1),
                      (x0 + 3, z0 + 3, G + 2), (x0 + 4, z0 + 4, G + 2)):
        b.set(x, y, z, mc("sandstone"))
    # the climbing dome: half a ball of pink wool with holes to climb through, a guh face on its front
    dx0, dz0, R = x0 + 12, z0 + 6, 4.6
    for x in range(int(dx0 - R) - 1, int(dx0 + R) + 2):
        for z in range(int(dz0 - R) - 1, int(dz0 + R) + 2):
            for y in range(G + 1, G + 6):
                dd = math.dist((x, y - G - 1, z), (dx0, 0, dz0))
                doorway = y < G + 3 and abs(z - dz0) <= 1 and abs(x - dx0) > R - 1.6       # (a way in on the east and west)
                if R - 1.0 < dd <= R and not doorway:
                    b.set(x, y, z, mc("pink_wool"))
    face_on_plane(b, int(dx0), G + 3, int(round(dz0 + R - 0.5)), "south", 2.3, only_features=True)
    # the slide: a little tower with a ladder, a slide of quartz stairs on a solid ramp
    sx, sz = x0 + 3, z0 + 11
    for y in range(G + 1, G + 4):
        for (x, z) in ((sx, sz), (sx + 1, sz), (sx, sz + 1), (sx + 1, sz + 1)):
            b.set(x, y, z, KS)
    for y in range(G + 1, G + 4):
        b.set(sx - 1, y, sz, LADDER, {"facing": "west", "waterlogged": "false"})
    for i in range(4):
        x, y = sx + 2 + i, G + 3 - i
        for z in (sz, sz + 1):
            for yy in range(G + 1, y):
                b.set(x, yy, z, mc("quartz_block"))
            b.set(x, y, z, mc("quartz_stairs"), {"facing": "west", "half": "bottom", "shape": "straight"})
    # the swings: two posts and a beam, chains with seats
    wx, wz = x0 + 11, z0 + 14
    for x in (wx, wx + 4):
        for y in range(G + 1, G + 5):
            b.set(x, y, wz, STAM, {"axis": "y"})
    for x in range(wx, wx + 5):
        b.set(x, G + 5, wz, STAM, {"axis": "x"})
    for x in (wx + 1, wx + 3):
        for y in (G + 3, G + 4):
            b.set(x, y, wz, mc("chain"), {"axis": "y", "waterlogged": "false"})
        b.set(x, G + 2, wz, mc("pink_carpet"))
    b.bank(x0 + 8, z0 + 10, "north")
    b.bloembak(x1 - 1, z0 + 1)
    b.set(x0 + 9, G + 1, z0 + 2, HOOP, {"vol": "false"})
    b.boompje(x1 - 2, z1 - 3, height=4, r=2.4)
    b.grasveld(x0, z0, x1, z1, 0.1)
    b.targets["de speeltuin"] = (x0 + 8, G + 1, z0 + 12)


def hoek(h, naam, gate_x, links, rechts):
    """A corner piece (40 x 30 x 31): a street along z 22..24 from the gate, a house (or Cocotje's house, or the
    playground) on each side of it."""
    b = Bouw(h, HOEK, f"hoek_{naam}")
    W, _, D = HOEK
    mc = h.mc
    for x in range(W):
        for z in range(D):
            for y in range(G):
                b.set(x, y, z, mc("pink_wool"))
            b.set(x, G, z, GRAS)
    vrij = VRIJ_STRAAT if naam == VRIJ_HOEK else None
    bouwplaats = BOUWPLAATS_STRAAT if naam == BOUWPLAATS_HOEK else None       # 3.0: the street on to the Timmerguh's bouwplaats
    for x in range(0 if bouwplaats else 1, W if vrij else W - 1):  # the street (up to the edge at the open street ends)
        for z in range(22, 25):
            b.set(x, G, z, KLINK)
    for z in range(22, D):                                        # the path to the gate
        for x in range(gate_x - 1, gate_x + 2):
            b.set(x, G, z, KLINK)
    doors = []
    for part, xs in ((links, 1), (rechts, 21)):
        if part == "cocotje":
            door = cocotje_huis(b, xs + 4, 5)
            hx0, hx1 = xs + 4, xs + 14
        elif part == "speeltuin":
            speeltuin(b, xs, 1, xs + 17, 20)
            door = None
            hx0, hx1 = xs + 1, xs + 16
        else:
            hx0 = xs + (1 if HUIZEN[part][2][0] < 16 else 0)
            hx1 = hx0 + HUIZEN[part][2][0] - 1
            door = guhhuis(b, part, hx0, 5)
        if door:
            doors.append(door)
            for z in range(door[2] - 1, 22):                      # the garden path from the street to the door
                for x in (door[0] - 1, door[0], door[0] + 1):
                    b.air(x, G + 1, z, x, G + 1, z)
                    b.set(x, G, z, KLINK)
        # the front garden: a low fluffy hedge along the street with a gap for the path, and a letterbox by the path
        gap = door[0] if door else xs + 8
        for x in range(hx0, hx1 + 1):
            if abs(x - gap) > 1 and b.get(x, G + 1, 21) is None:
                b.set(x, G + 1, 21, BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})
        if door:
            b.set(gap + 2, G + 1, 21, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false",
                                                             "waterlogged": "false"})
            b.set(gap + 2, G + 2, 21, mc("barrel"), {"facing": "south", "open": "false"})
        # little pluizenbomen in the meadow behind the houses (not in the playground)
        for tx in ((3, 19) if xs == 1 else (36,)):
            if part != "speeltuin" and b.get(tx, G + 1, 2) is None:
                b.boompje(tx, 2, height=4, r=2.2)
    if vrij:                                                      # the open street end: kerb lamps on both sides
        (vx, _, vz), _o = vrij
        for zz in (vz - 2, vz + 2):
            b.set(vx, G, zz, KS)
            b.lantaarnpaal(vx, zz, height=3)
    if bouwplaats:                                                # 3.0: the way to the bouwplaats: kerb lamps and a signpost
        (bx, _, bz), _o = bouwplaats
        for zz in (bz - 2, bz + 2):
            b.set(bx, G, zz, KS)
            b.lantaarnpaal(bx, zz, height=3)
        b.set(bx + 2, G + 1, bz + 2, mc("spruce_fence"), {"north": "false", "south": "false", "east": "false", "west": "false",
                                                           "waterlogged": "false"})
        b.set(bx + 2, G + 2, bz + 2, mc("spruce_fence"), {"north": "false", "south": "false", "east": "false", "west": "false",
                                                           "waterlogged": "false"})
        b.set(bx + 2, G + 3, bz + 2, mc("spruce_sign"), {"rotation": "12", "waterlogged": "false"}, bouwplaats_bordje(h))
    # a picnic spot and a tree in the meadow by the street
    b.set(27, G + 1, 28, "guhs:guh_tafel", {"facing": "south"})
    b.set(26, G + 1, 28, "guhs:guh_stoel", {"facing": "east"})
    b.set(28, G + 1, 28, "guhs:guh_stoel", {"facing": "west"})
    b.boompje(21, 28, height=4, r=2.2)
    for x in (3, 19, 36):                                         # lamp posts, flower boxes, benches, leaf piles
        if abs(x - gate_x) > 2:
            b.lantaarnpaal(x, 26)
    for x in (10, 28):
        b.bloembak(x, 26)
    for x in (14, 32):
        b.bank(x, 27, "north")
    for (x, z) in ((7, 28), (24, 29), (17, 29), (38, 28)):
        if b.get(x, G + 1, z) is None and abs(x - gate_x) > 2:
            b.set(x, G + 1, z, HOOP, {"vol": "false"})
    b.grasveld(0, 25, W - 1, D - 1, 0.2)
    b.grasveld(0, 0, W - 1, 21, 0.08)
    b.jigsaw(gate_x, G, D - 1, "south_up", "guhs:hoek_ingang", "minecraft:empty", "minecraft:empty", KLINK)
    if vrij:
        # the town's one free street connection: its pool holds the 2.9 street piece(s) (VRIJ_STUKKEN, the Beroepenstraat)
        (vx, vy, vz), orient = vrij
        b.jigsaw(vx, vy, vz, orient, VRIJ_NAAM, VRIJ_NAAM, VRIJ_POOL, KLINK)
        b.targets["het open straateinde"] = (vx - 1, G + 1, vz)
    if bouwplaats:
        # 3.0: the Timmerguh's bouwplaats hangs on the west end of this street (depth 2: plein -> hoek -> bouwplaats)
        (bx, by, bz), orient = bouwplaats
        b.jigsaw(bx, by, bz, orient, BOUWPLAATS_NAAM, BOUWPLAATS_DOEL, BOUWPLAATS_POOL, KLINK)
        b.targets["de weg naar de bouwplaats"] = (bx + 1, G + 1, bz)
    b.starts = [(gate_x, G + 1, D - 2)]
    for i, dd in enumerate(doors):
        b.targets[f"voordeur {i}"] = dd
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=HOEK[1])
    return b


# =====================================================================================================================
# the self-check
# =====================================================================================================================
def neighbours(p):
    x, y, z = p
    return ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1))


def walk(b, starts):
    s = b.s
    W, H, D = b.size
    get = s.get

    def standable(p):
        x, y, z = p
        if not (0 <= x < W and 0 <= z < D and 1 <= y < H - 1):
            return False
        if not passable(get(x, y, z)) or not passable(get(x, y + 1, z)):
            return False
        return solid(get(x, y - 1, z)) or get(x, y, z) == LADDER or get(x, y - 1, z) == LADDER

    seen = {p for p in starts if standable(p)}
    todo = deque(seen)
    while todo:
        x, y, z = todo.popleft()
        nxt = []
        for dx, dz in FACING.values():
            for dy in (1, 0, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if dy == 1 and not passable(get(x, y + 2, z)):
                    continue
                if dy < 0 and any(not passable(get(x + dx, y + k, z + dz)) for k in range(dy + 1, 2)):
                    continue
                if standable(n):
                    nxt.append(n)
                    break
        if get(x, y, z) == LADDER or get(x, y - 1, z) == LADDER:
            for n in ((x, y + 1, z), (x, y - 1, z)):
                if standable(n):
                    nxt.append(n)
        for n in nxt:
            if n not in seen:
                seen.add(n)
                todo.append(n)
    return seen


def check(b, extra=None):
    problems = []
    s = b.s
    get = s.get
    reach = walk(b, b.starts)
    b.walkable = len(reach)
    if not reach:
        problems.append(f"{b.naam}: nothing walkable from the starts {b.starts}")
    for name, p in b.targets.items():
        ok = p in reach or any((p[0] + dx, p[1] + dy, p[2] + dz) in reach for dx, dz in list(FACING.values()) + [(0, 0)] for dy in (-1, 0, 1))
        if not ok:
            problems.append(f"{b.naam}: {name} {p} can't be reached on foot")
    for (x, y, z) in b.floors:
        if not solid(get(x, y - 1, z)) or not passable(get(x, y, z)) or not passable(get(x, y + 1, z)):
            problems.append(f"{b.naam}: someone at {(x, y, z)} doesn't stand free on a floor ({get(x, y - 1, z)}, {get(x, y, z)}, {get(x, y + 1, z)})")
    for (x, y, z), (blk, props, _) in s.blocks.items():
        if blk == LADDER:
            dx, dz = FACING[props["facing"]]
            if not solid(get(x - dx, y, z - dz)):
                problems.append(f"{b.naam}: ladder at {(x, y, z)} hangs on {get(x - dx, y, z - dz)}")
        if blk in (DOOR,) and props.get("half") == "lower" and (get(x, y + 1, z) != DOOR or not solid(get(x, y - 1, z))):
            problems.append(f"{b.naam}: door at {(x, y, z)} is broken")
        if blk == "minecraft:pink_bed" and props.get("part") == "head":
            dx, dz = FACING[props["facing"]]
            if get(x - dx, y, z - dz) != "minecraft:pink_bed":
                problems.append(f"{b.naam}: bed at {(x, y, z)} has no foot")
        if blk == LAMP:
            support = get(x, y + 1, z) if props.get("hanging") == "true" else get(x, y - 1, z)
            if not solid(support) and not (support and ("_wall" in support or "chain" in support)):
                problems.append(f"{b.naam}: lantern at {(x, y, z)} hangs/stands on {support}")
        if blk in PLANTS or blk == HOOP or blk in (BAK, TAFEL, "guhs:guh_bank", "guhs:guh_stoel", "guhs:guh_tafel", "guhs:guh_taart"):
            if not solid(get(x, y - 1, z)):
                problems.append(f"{b.naam}: {blk} at {(x, y, z)} stands on {get(x, y - 1, z)}")
    # nothing floats: every group of blocks holds on to the ground (y <= G)
    real = {p for p, (blk, _, _) in s.blocks.items() if blk != "minecraft:air"}
    seen = set()
    for p in real:
        if p in seen:
            continue
        comp, todo, grounded = [], [p], False
        seen.add(p)
        while todo:
            q = todo.pop()
            comp.append(q)
            if q[1] <= G:
                grounded = True
            for n in neighbours(q):
                if n in real and n not in seen:
                    seen.add(n)
                    todo.append(n)
        if not grounded:
            first = sorted(comp)[0]
            problems.append(f"{b.naam}: {len(comp)} floating blocks from {first} ({get(*first)})")
    if extra:
        problems += extra(b)
    return problems


def check_plein(b):
    problems = []
    names = sorted(j[1] for j in b.jigsaws)
    want = sorted(["guhs:plein_bakkerij", "guhs:plein_theehuis", "guhs:plein_kapper", "guhs:plein_creche", "guhs:plein_grijpmachine",
                   "guhs:plein_hoek", "guhs:plein_hoek", "guhs:plein_hoek", "guhs:plein_hoek", ANCHOR])
    if names != want:
        problems.append(f"plein: jigsaws {names}")
    slots = {j[1]: j[0] for j in b.jigsaws}
    for name, pos in (("guhs:plein_bakkerij", (24, 4, 0)), ("guhs:plein_theehuis", (48, 4, 24)), ("guhs:plein_kapper", (24, 4, 48)),
                      ("guhs:plein_creche", (0, 4, 24)), ("guhs:plein_grijpmachine", (38, 4, 10)), (ANCHOR, (C, G, C))):
        if slots.get(name) != pos:
            problems.append(f"plein: {name} at {slots.get(name)}, should be {pos}")
    fires = [p for p, (blk, _, _) in b.s.blocks.items() if blk == "minecraft:campfire" and math.dist((p[0], p[2]), (C, C)) <= 20]
    if not fires:
        problems.append("plein: no campfire within 20 blocks of the anchor")
    counts = {}
    for (blk, props, _) in b.s.blocks.values():
        counts[blk] = counts.get(blk, 0) + 1
    for blk, least in ((TAFEL, 8), (BAK, 10), (SLINGER, 20), (HOOP, 6), (KS_FACE, 12)):
        if counts.get(blk, 0) < least:
            problems.append(f"plein: only {counts.get(blk, 0)} x {blk}")
    kinds = sorted(e[3]["Kind"] for e in b.s.entities if e[3]["id"] == "guhs:guh_npc")
    if kinds != ["burgemeesterguh", "opa_guh", "reisguh"]:
        problems.append(f"plein: NPCs {kinds}")
    # the grijpmachine's room under the arcade is free
    for x in range(36, 41):
        for y in range(G + 1, G + 7):
            for z in range(8, 13):
                if b.get(x, y, z) not in (None, "minecraft:air"):
                    problems.append(f"plein: {b.get(x, y, z)} at {(x, y, z)} is in the grijpmachine's room")
    # the edge where a slot building stands is clear above the ground (x 9..39 at z 0, etc.) - nothing sticks out
    return problems


def check_hoek(b):
    problems = []
    js = [j for j in b.jigsaws if j[1] not in (VRIJ_NAAM, BOUWPLAATS_NAAM)]
    if len(js) != 1 or js[0][1] != "guhs:hoek_ingang" or js[0][4] != "south_up" or js[0][0][2] != HOEK[2] - 1:
        problems.append(f"{b.naam}: jigsaw {js}")
    vrij = [j for j in b.jigsaws if j[1] == VRIJ_NAAM]
    if b.naam == f"hoek_{VRIJ_HOEK}":
        (vx, vy, vz), orient = VRIJ_STRAAT
        if len(vrij) != 1 or vrij[0][0] != (vx, vy, vz) or vrij[0][2:] != (VRIJ_NAAM, VRIJ_POOL, orient):
            problems.append(f"{b.naam}: the free street jigsaw is {vrij}")
        # the street runs up to it, and the doorway a later street piece hangs on (3 wide, 4 high) is open
        for z in (vz - 1, vz + 1):
            if b.get(vx, G, z) != KLINK:
                problems.append(f"{b.naam}: the street doesn't reach the free end at {(vx, G, z)} ({b.get(vx, G, z)})")
        for z in range(vz - 1, vz + 2):
            for y in range(G + 1, G + 5):
                if b.get(vx, y, z) not in (None, "minecraft:air"):
                    problems.append(f"{b.naam}: {b.get(vx, y, z)} at {(vx, y, z)} blocks the free street end")
    elif vrij:
        problems.append(f"{b.naam}: a free street jigsaw here too {vrij}")
    # 3.0: the Timmerguh's bouwplaats jigsaw, only at the west end of hoek_noordwest's street, with the doorway open
    bp = [j for j in b.jigsaws if j[1] == BOUWPLAATS_NAAM]
    if b.naam == f"hoek_{BOUWPLAATS_HOEK}":
        (bx, by, bz), orient = BOUWPLAATS_STRAAT
        if len(bp) != 1 or bp[0][0] != (bx, by, bz) or bp[0][2:] != (BOUWPLAATS_DOEL, BOUWPLAATS_POOL, orient):
            problems.append(f"{b.naam}: the bouwplaats jigsaw is {bp}")
        for z in (bz - 1, bz + 1):
            if b.get(bx, G, z) != KLINK:
                problems.append(f"{b.naam}: the street doesn't reach the bouwplaats at {(bx, G, z)} ({b.get(bx, G, z)})")
        for z in range(bz - 1, bz + 2):
            for y in range(G + 1, G + 5):
                if b.get(bx, y, z) not in (None, "minecraft:air"):
                    problems.append(f"{b.naam}: {b.get(bx, y, z)} at {(bx, y, z)} blocks the way to the bouwplaats")
    elif bp:
        problems.append(f"{b.naam}: a bouwplaats jigsaw here too {bp}")
    bewoners = [e for e in b.s.entities if e[3]["id"] == "guhs:guh"]
    if not bewoners and "speeltuin" not in b.naam:
        problems.append(f"{b.naam}: no residents")
    return problems


# the one free street jigsaw of the town (DESIGN_28 "Town extensibility"): at the east end of hoek_noordoost's street (world
# east edge of the town, x = 79 from the plein's corner), name = target = guhs:knuffeldal_straat_vrij, pool
# guhs:knuffeldal_stadje/vrij (only an empty element, fallback minecraft:empty). A later street piece needs a jigsaw named
# guhs:knuffeldal_straat_vrij on its west side and the structure's size/max_distance_from_center raised.
VRIJ_HOEK = "noordoost"
VRIJ_STRAAT = ((HOEK[0] - 1, G, 23), "east_up")
VRIJ_NAAM = "guhs:knuffeldal_straat_vrij"
VRIJ_POOL = "guhs:knuffeldal_stadje/vrij"
# 2.9 (beroepen): the street pieces in the vrij pool (their templates come from tools/features/beroepen.py), and their world
# boxes (x0, z0, x1, z1) relative to the plein's (0, 0): the Beroepenstraat runs north-south along the town's east edge
# (x 80..102), its jigsaw on its west side at z = -8 (the end of hoek_noordoost's street)
VRIJ_STUKKEN = ["knuffeldal_stadje/beroepenstraat"]
VRIJ_BOXES = {"beroepenstraat": (80, -48, 102, 84)}

# 3.0 (timmerguh): the Timmerguh's bouwplaats (DESIGN_30 par. 1) at the west end of hoek_noordwest's street (the town's west
# edge, x = -31 from the plein's corner): jigsaw guhs:knuffeldal_bouwplaats (target guhs:bouwplaats_ingang) -> pool
# guhs:knuffeldal_stadje/bouwplaats (one element: the template of tools/features/timmerguh_bouw.py, 22 x H x 33, its
# jigsaw guhs:bouwplaats_ingang at (21, G, 16) east_up). Its world box (x0, z0, x1, z1) relative to the plein's (0, 0).
BOUWPLAATS_HOEK = "noordwest"
BOUWPLAATS_STRAAT = ((0, G, 23), "west_up")
BOUWPLAATS_NAAM = "guhs:knuffeldal_bouwplaats"
BOUWPLAATS_DOEL = "guhs:bouwplaats_ingang"
BOUWPLAATS_POOL = "guhs:knuffeldal_stadje/bouwplaats"
BOUWPLAATS_STUK = "knuffeldal_stadje/bouwplaats"
BOUWPLAATS_BOXES = {"bouwplaats": (-53, -24, -32, 8)}


def bouwplaats_bordje(h):
    """The signpost at the street end towards the bouwplaats (a waxed sign, lang sign.guhs.timmerguh_wegwijzer1..3)."""
    import json as _json
    ms = h.ms
    blank = _json.dumps("")
    msgs = [_json.dumps({"translate": f"sign.guhs.timmerguh_wegwijzer{i}"}) for i in (1, 2, 3)] + [blank]
    return {"id": "minecraft:sign", "is_waxed": ms.Byte(1),
            "front_text": {"messages": ms.NbtList(8, msgs), "color": "brown", "has_glowing_text": ms.Byte(0)},
            "back_text": {"messages": ms.NbtList(8, [blank] * 4), "color": "black", "has_glowing_text": ms.Byte(0)}}

LAYOUT = {  # corner: (gate x, left, right)  - A corners (gate at x 4): noordoost, zuidwest; B (gate at x 35): noordwest, zuidoost
    "noordoost": (4, "pluisje", "knabbeltje"),
    "noordwest": (35, "dikkie", "cocotje"),
    "zuidoost": (35, "mollie", "sproetje"),
    "zuidwest": (4, "bolletje", "speeltuin"),
}


def build_all(h):
    """Makes, checks and saves the town's own templates and pools. Returns a summary (faces, residents...)."""
    problems = []
    summary = {}
    p = plein(h)
    problems += check(p, check_plein)
    p.s.save("knuffeldal_stadje/plein")
    summary["plein"] = (p.walkable, p.gezichten, p.grote_gezichten)
    for naam, (gate, links, rechts) in LAYOUT.items():
        b = hoek(h, naam, gate, links, rechts)
        problems += check(b, check_hoek)
        b.s.save(f"knuffeldal_stadje/hoek_{naam}")
        summary[naam] = (b.walkable, b.gezichten, b.grote_gezichten)
    if problems:
        raise SystemExit("knuffeldal_stadje geometry check failed:\n  " + "\n  ".join(problems[:80]))
    D = h.D
    pools = {"start": "plein", **{f"hoek_{n}": f"hoek_{n}" for n in LAYOUT}}
    for pool, tpl in pools.items():
        h.w(f"{D}/worldgen/template_pool/knuffeldal_stadje/{pool}.json", {"fallback": "minecraft:empty", "elements": [
            {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:knuffeldal_stadje/{tpl}",
                                      "projection": "rigid", "processors": "minecraft:empty"}}]})
    # the free street end's pool: the 2.9 street pieces (VRIJ_STUKKEN; an empty element if there are none)
    h.w(f"{D}/worldgen/template_pool/knuffeldal_stadje/vrij.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{tpl}",
                                  "projection": "rigid", "processors": "minecraft:empty"}} for tpl in VRIJ_STUKKEN] or [
        {"weight": 1, "element": {"element_type": "minecraft:empty_pool_element"}}]})
    # 3.0: the Timmerguh's bouwplaats (its template comes from tools/features/timmerguh_bouw.py)
    h.w(f"{D}/worldgen/template_pool/knuffeldal_stadje/bouwplaats.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{BOUWPLAATS_STUK}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    for slot in ("bakkerij", "theehuis", "kapper", "creche", "grijpmachine"):
        h.w(f"{D}/worldgen/template_pool/knuffeldal_stadje/{slot}.json", {"fallback": "minecraft:empty", "elements": [
            {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:knuffeldal_stadje/{slot}",
                                      "projection": "rigid", "processors": "minecraft:empty"}}]})
    print("knuffeldal_stadje: geometry ok", summary)
    return summary


def stukken():
    """The world boxes (x0, z0, x1, z1) of the town's pieces relative to the plein's (0, 0): for the overlap self-check."""
    return {"plein": (0, 0, 48, 48), "bakkerij": (9, -31, 39, -1), "kapper": (9, 49, 39, 79), "creche": (-31, 9, -1, 39),
            "theehuis": (49, 9, 79, 39), "hoek_noordoost": (40, -31, 79, -1), "hoek_noordwest": (-31, -31, 8, -1),
            "hoek_zuidoost": (40, 49, 79, 79), "hoek_zuidwest": (-31, 49, 8, 79), **VRIJ_BOXES, **BOUWPLAATS_BOXES}


def check_stukken():
    boxes = stukken()
    problems = []
    names = list(boxes)
    for i in range(len(names)):
        for j in range(i + 1, len(names)):
            a, b2 = boxes[names[i]], boxes[names[j]]
            if a[0] <= b2[2] and b2[0] <= a[2] and a[1] <= b2[3] and b2[1] <= a[3]:
                problems.append(f"{names[i]} and {names[j]} overlap")
    reach = max(max(abs(v - C) for v in (x0, x1, z0, z1)) for (x0, z0, x1, z1) in boxes.values())
    return problems, reach


if __name__ == "__main__":
    # the self-check on its own:  python tools/features/knuffeldal_stadje.py   (from the project root; saves nothing)
    import os
    import sys
    import types
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms
    stub = types.SimpleNamespace(mc=ms.mc, Structure=ms.Structure, ms=ms)
    found = []
    pb = plein(stub)
    found += check(pb, check_plein)
    print("plein:", len(pb.s.blocks), "blocks,", pb.walkable, "walkable,", pb.gezichten, "faces,", pb.grote_gezichten, "big faces")
    for hn, (gx, left, right) in LAYOUT.items():
        hb = hoek(stub, hn, gx, left, right)
        found += check(hb, check_hoek)
        print(f"hoek_{hn}:", len(hb.s.blocks), "blocks,", hb.walkable, "walkable,", hb.gezichten, "faces,", hb.grote_gezichten, "big faces")
    overlap, reach = check_stukken()
    found += overlap
    print("reach from the anchor:", reach)
    print("\n".join(found[:120]) if found else "geometry check ok")
