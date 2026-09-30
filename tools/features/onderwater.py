"""
De Guhbubbel (onderwater feature, 2.5): the guh underwater world.

In the middle of every Diepe Guhzee (the Guhmension's big deep sea, tools/features/diepzee.py; GuhbubbelStructure.java
finds the deepest spot of each sea) lies this structure: a round piece of the sea (28 blocks deep) with on its bottom a
giant glass bubble dome (76 wide) shaped like a guh head (two ear bubbles, stained-glass eyes and nose, the entrance tunnel is its mouth). Inside: air,
a kaaskoraal garden with coral trees and ponds, reuzenschelpen (giant guh shells that now and then hold a pearl) and,
in the middle in front of a giant scallop, the Zeemeerguh with her little diving shop (the duikhelm for you and the
duikpakje for your guh, paid with pearls). Outside on the sea floor: a coral reef with more shells, kaaskoraal air
stations and, just north of the dome, a sunken guh ship with a treasure chest.

The way down needs nothing of your own: on a little island south of the dome stands the Duikpost (a guh head wearing
diving goggles and a snorkel, with a jetty to its mouth); walk in through its mouth and take the spiral staircase in a glass tube down to the sea floor, then the
glass tunnel into the dome. The bubble lift (a water column with bubbles) brings you back up; the dive doors of the
dome let you swim out (kaaskoraal gives you air, doors hold back the water).

Also here: the Zeemeerguh guh variant (fish tail, see BONES / variants), the duikpakje clothes (duikbril, snorkel,
zwemband), the blocks kaaskoraal / kaaskoraalblok / parelmoer / parelmoertegels / reuzenschelp and the duikhelm.

build(h) makes everything (h = make_v2); check(...) is the geometry self-check of the template.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw

# --- the template ------------------------------------------------------------------------------------------------------
W, H, D = 128, 58, 128
F = 8                   # the sea floor (and the dome's floor): blocks at y = F, you walk at F + 1
G = 36                  # the water surface: the top water block is at y = G (placed on the sea level of the Guhmension)
CX, CZ = 64, 64         # the middle of the template and the dome
RD, VD = 38, 20         # the dome: horizontal and vertical radius (outside of the glass)
EARS = ((CX - 19, F + 14, CZ - 4, 6.5), (CX + 19, F + 14, CZ - 4, 6.5))
FLAT = 48               # the reef's sea lanterns lie within this radius
SEA_R = 62.5            # the template is a round piece of the sea: floor and water up to this radius (outside: the real sea)
ISLAND_R = (13.0, 11.5)  # the Duikpost island around the stair tube: radius on the sea floor and at the surface
SEA_BIOME = "diepe_guhzee"  # the biome the bubble is in (tools/features/diepzee.py)
TX, TZ = CX, CZ + 50    # the stair tube (and the Duikpost head on top of it)
HEAD_R = (8.5, 10.0, 8.0)
LIFT = (TX + 5, TZ)     # the bubble lift (up) next to the stair tube
NPC = (CX, F + 1, CZ)   # the Zeemeerguh
WRECK = (CX - 12, CX + 10, CZ - 43)   # stern x, bow x, middle z of the sunken guh ship
SALT = 20250031         # 20250000 + feature 3 * 10 + 1

CLOTHES = ["duikbril", "snorkel", "zwemband"]
_H = [0, 6, -2]
BONES = {
    # --- the Zeemeerguh (variant): a fish tail over the back legs, a tail fin and a fin on its back ---
    "zeemeer_heup": ("body", [0, 6, 6], "zeemeer_schubben", [([-8.6, -0.3, 5.2], [17.2, 6.8, 7.4], 0.2)]),
    "zeemeer_staart": ("body", [0, 3, 12], "zeemeer_schubben", [([-5.6, 0.4, 12.4], [11.2, 5.2, 5.4], 0.1),
                                                                ([-3.6, 1.2, 17.6], [7.2, 3.6, 4.2], 0),
                                                                ([-2.1, 1.8, 21.6], [4.2, 2.4, 3], 0)]),
    "zeemeer_staartvin": ("zeemeer_staart", [0, 3, 12], "zeemeer_vin", [([-1.5, 2.2, 24.4], [3, 1.6, 1.4], 0),
                                                                         ([-8, 2.5, 25.2], [7, 0.8, 4.6], 0),
                                                                         ([1, 2.5, 25.2], [7, 0.8, 4.6], 0)]),
    "zeemeer_rugvin": ("body", [0, 11, 4], "zeemeer_vin", [([-0.5, 10.8, 1.5], [1, 2.6, 6], 0), ([-0.5, 13.2, 3.5], [1, 1.4, 3], 0)]),
    # --- the duikpakje (guh clothes): diving goggles (with a rubber strap), a snorkel and a swim ring ---
    "outfit_duikbril": ("head", _H, "duikbril", [([1, 7, -12.8], [6.2, 5.8, 0.8], 0), ([-7.2, 7, -12.8], [6.2, 5.8, 0.8], 0)]),
    "outfit_duikbril_band": ("head", _H, "duikbril_band", [([-1.25, 9.4, -12.7], [2.5, 1, 0.6], 0), ([7.1, 8.2, -12.2], [1.1, 1.2, 12.4], 0),
                                                           ([-8.2, 8.2, -12.2], [1.1, 1.2, 12.4], 0), ([-8.2, 8.2, 0.1], [16.4, 1.2, 0.6], 0)]),
    "outfit_snorkel": ("head", _H, "snorkel", [([-9.4, 5.5, -9.6], [1.8, 13, 1.8], 0), ([-9.4, 3.8, -12.6], [1.8, 1.8, 4.8], 0),
                                              ([-7.6, 3.8, -13.2], [4.2, 1.8, 1.2], 0)]),
    "outfit_snorkel_tip": ("head", _H, "snorkel_tip", [([-9.7, 18.5, -9.9], [2.4, 2.2, 2.4], 0)]),
    "outfit_zwemband": ("body", [0, 6, 6], "zwemband", [([6.4, 2.6, -2.2], [3.4, 4.8, 12.8], 0.2), ([-9.8, 2.6, -2.2], [3.4, 4.8, 12.8], 0.2),
                                                       ([-6.4, 2.6, 10.6], [12.8, 4.8, 3.2], 0.2)]),
}

# blocks you can stand in / walk through, and ones you can't stand on top of
THIN = (":lantern", ":soul_lantern", "sign", "carpet", "door", "torch", "button", "pressure_plate", "kaaskoraal[", "seagrass", "kelp", "coral_fan",
        "sea_pickle", "waterlelie", "chain")
FENCY = ("fence", "wall", "pane", "iron_bars")
LIGHT = {"sea_lantern": 15, "pearlescent_froglight": 15, "ochre_froglight": 15, "glowstone": 15, "lantern": 15, "shroomlight": 15,
         "kaaskoraal": 10}
AIRY = (None, "minecraft:air")
FLUID = ("minecraft:water", "minecraft:bubble_column")
WATERLOGGABLE = ("_stairs", "_slab", "_fence", "_wall", "_pane", ":lantern", ":soul_lantern", "_sign", "chest", "sea_pickle", "coral_fan",
                 "guhs:kaaskoraal", "reuzenschelp", "chain", "iron_bars")


def wets(n):
    """Can this block be waterlogged (so water next to it would soak into it)?"""
    return any(n.endswith(t) for t in WATERLOGGABLE)


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def pick(rng, table):
    total = sum(w for _, w in table)
    r = rng.uniform(0, total)
    for name, w in table:
        r -= w
        if r <= 0:
            return name
    return table[-1][0]


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


class Bubble:
    """The template being built, plus the bookkeeping the decorations and the self-check need."""

    def __init__(self, h):
        self.h = h
        self.s = h.Structure((W, H, D))
        self.rng = random.Random(SALT)
        self.air = {}              # (x, y, z) -> zone of the sealed air: dome, tube, tunnel, lift, head
        self.floor = {}            # (x, z) -> y of the floor (top solid block: sea floor or island) in the round piece of sea
        self.entities = []         # (x, y, z, kind) for the self-check
        self.shells = []           # (x, y, z, inside) the reuzenschelpen
        self.doors = []            # (inside cell, outside cell) of the dive doors
        self.targets = {}          # label -> cell to reach
        self.door_fronts = []

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def water(self, x, y, z):
        self.s.set(x, y, z, "minecraft:water", {"level": "0"})


def in_template(x, y, z):
    return 0 <= x < W and 0 <= y < H and 0 <= z < D


# =====================================================================================================================
# the sea floor (flat, the dome stands on it) and the Duikpost island; everything else up to the surface is water
# =====================================================================================================================
BANDS = ["pink_terracotta", "white_terracotta", "pink_terracotta", "calcite", "pink_terracotta", "magenta_terracotta",
         "white_terracotta", "pink_terracotta"]


def in_sea(x, z):
    """Is this column part of the template's round piece of sea (outside it, the real sea goes on)?"""
    return math.hypot(x - CX, z - CZ) <= SEA_R


def island_radius(y):
    """The Duikpost island (a round rock with steep sides) at height y."""
    t = (y - F) / (G - F)
    return ISLAND_R[0] + (ISLAND_R[1] - ISLAND_R[0]) * t


def sea_floor(b):
    rng = b.rng
    for x in range(W):
        for z in range(D):
            if not in_sea(x, z):
                continue
            d = math.hypot(x - TX, z - TZ)
            top = F
            for y in range(F - 3, G + 1):
                band = BANDS[(y + (x * 3 + z * 5) // 23) % len(BANDS)]
                if y >= F and d <= island_radius(y):
                    # the island: rock, a sandstone rim under the surface, sand on top
                    top = y
                    b.set(x, y, z, "sand" if y == G else "sandstone" if y >= G - 2 else band)
                elif y < F:
                    b.set(x, y, z, band)
                elif y > top:
                    b.water(x, y, z)
            b.floor[(x, z)] = top
            if top == F:
                b.set(x, F, z, pick(rng, [("sand", 70), ("pink_terracotta", 12), ("clay", 10), ("guhs:parelmoer", 2), ("gravel", 6)]))


# =====================================================================================================================
# the sealed air: the dome (with its ears), the stair tube, the tunnel, the lift passage, the Duikpost head
# =====================================================================================================================
def dome_value(x, y, z, shrink=0.0):
    """< 1 inside the dome (or an ear bubble); shrink makes it smaller (the air inside the glass)."""
    v = ((x - CX) / (RD - shrink)) ** 2 + ((z - CZ) / (RD - shrink)) ** 2 + ((y - F) / (VD - shrink)) ** 2
    for (ex, ey, ez, er) in EARS:
        v = min(v, ((x - ex) ** 2 + (y - ey) ** 2 + (z - ez) ** 2) / (er - shrink) ** 2)
    return v


def head_value(x, y, z, shrink=0.0):
    rx, ry, rz = HEAD_R
    return ((x - TX) / (rx - shrink)) ** 2 + ((y - G - 1) / (ry - shrink)) ** 2 + ((z - TZ) / (rz - shrink)) ** 2


def carve(b, cell, zone):
    b.air[cell] = zone
    b.s.set(*cell, "minecraft:air")


def air_regions(b):
    # the dome: the air inside the glass
    for x in range(CX - RD, CX + RD + 1):
        for z in range(CZ - RD, CZ + RD + 1):
            for y in range(F + 1, F + VD + 8):
                if dome_value(x, y, z, 1.0) < 1:
                    carve(b, (x, y, z), "dome")
    # the stair tube: its glass ring first (it goes through the rock of the steep side), then the air inside
    for y in range(F, G):
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if max(abs(dx), abs(dz)) == 3:
                    corner = abs(dx) == 3 and abs(dz) == 3
                    lamp = y % 6 == 3 and (dx == 0 or dz == 0)
                    b.set(TX + dx, y, TZ + dz, "guhs:parelmoer" if corner else "sea_lantern" if lamp else "glass")
    for y in range(F + 1, G + 1):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if (dx, dz) != (0, 0):
                    carve(b, (TX + dx, y, TZ + dz), "tube")
    # the tunnel from the tube to the dome (the dome's mouth)
    for z in range(CZ + 30, TZ - 2):
        for x in range(CX - 1, CX + 2):
            for y in range(F + 1, F + 4):
                if (x, y, z) not in b.air:
                    carve(b, (x, y, z), "tunnel")
    # the passage from the bottom of the tube to the bubble lift's door
    for y in (F + 1, F + 2):
        carve(b, (TX + 3, y, TZ), "tube")
    # the Duikpost head (above the water)
    rx, ry, rz = HEAD_R
    for x in range(int(TX - rx), int(TX + rx) + 1):
        for z in range(int(TZ - rz), int(TZ + rz) + 1):
            for y in range(G + 1, G + 2 + int(ry)):
                if head_value(x, y, z, 1.0) < 1:
                    carve(b, (x, y, z), "head")


def shell(b):
    """Glass on every spot next to the sealed air where water would otherwise be (so nothing can leak in)."""
    made = {}
    for (x, y, z), zone in list(b.air.items()):
        if zone == "head" or y > G:
            continue
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in b.air or not in_template(*n):
                continue
            here = b.get(*n)
            if here in (None, "minecraft:water"):
                made[n] = zone
    b.shell_cells = made
    for c, zone in made.items():
        b.set(*c, dome_glass(b, c) if zone == "dome" else "glass")


def dome_glass(b, c):
    """The dome's glass: clear, with pearly ribs, and a guh face on the south side (eyes, nose, blush, a smile)."""
    x, y, z = c
    u, v, w = x - CX, y - F, z - CZ
    if v <= 1:
        return "guhs:parelmoer"                                   # the pearly rim on the sea floor
    in_ellipsoid = (u / RD) ** 2 + (w / RD) ** 2 + (v / VD) ** 2 <= 1.05
    if not in_ellipsoid:                                          # an ear bubble
        for (ex, ey, ez, er) in EARS:
            if (x - ex) ** 2 + (y - ey) ** 2 + (z - ez) ** 2 <= (er + 0.8) ** 2:
                inner = z > ez + 1 and abs(x - ex) < er * 0.55 and abs(y - ey - 1) < er * 0.6
                return "magenta_stained_glass" if inner else "pink_stained_glass"
    if w > 10:                                                    # the face, looking south
        for s in (-1, 1):
            e = ((u - s * 11) / 4.2) ** 2 + ((v - 11) / 4.6) ** 2
            if e <= 1:
                shine = ((u - s * 11 + 1.6) / 1.4) ** 2 + ((v - 12.6) / 1.4) ** 2 <= 1
                return "white_stained_glass" if shine else "black_stained_glass"
            if ((u - s * 18) / 3.2) ** 2 + ((v - 5.5) / 2.0) ** 2 <= 1:
                return "pink_stained_glass"
        if (u / 2.4) ** 2 + ((v - 7) / 1.7) ** 2 <= 1:
            return "magenta_stained_glass"
        if 1 <= abs(u) <= 6 and abs(v - (4.2 - 0.35 * abs(abs(u) - 3.5))) < 0.75:
            return "magenta_stained_glass"
    ang = math.degrees(math.atan2(w, u)) % 30
    if (ang < 2.2 or ang > 27.8) and v < VD - 3:
        return "white_stained_glass"
    if v in (7, 14):
        return "white_stained_glass"
    return "glass"


# =====================================================================================================================
# the stair tube (a spiral staircase), the bubble lift and the tunnel
# =====================================================================================================================
def stair_facing(dx, dz):
    """Stairs go up counterclockwise (seen from above): the high side faces that way."""
    th = math.atan2(dx, -dz)
    cx, cz = -math.cos(th), -math.sin(th)
    if abs(cx) >= abs(cz):
        return "east" if cx > 0 else "west"
    return "south" if cz > 0 else "north"


def step_index(dx, dz):
    th = math.atan2(dx, -dz)                     # 0 = north, clockwise
    rel = (math.degrees(th) - 180) % 360         # the top step is on the south side (you come in from the mouth)
    return int(rel / 30)


def stair_tube(b):
    # the pillar in the middle: pearl with lamps
    for y in range(F, G + 1):
        b.set(TX, y, TZ, "sea_lantern" if y % 4 == 2 else "guhs:parelmoer")
        b.air.pop((TX, y, TZ), None)
    for y in range(G + 1, G + 12):                  # (on up through the Duikpost, as its middle post)
        if head_value(TX, y, TZ, 1.0) < 1:
            b.set(TX, y, TZ, "sea_lantern" if y % 4 == 2 else "guhs:parelmoer")
            b.air.pop((TX, y, TZ), None)
    # the steps: 12 per turn, two blocks thick, going down clockwise from the south side
    b.steps = {}
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if (dx, dz) == (0, 0):
                continue
            s = step_index(dx, dz)
            for k in range(0, 4):
                y = G - 12 * k - s
                if y <= F:
                    break
                cell = (TX + dx, y, TZ + dz)
                b.set(*cell, "prismarine_brick_stairs", {"facing": stair_facing(dx, dz), "half": "bottom", "shape": "straight",
                                                         "waterlogged": "false"})
                b.air.pop(cell, None)
                b.steps[cell] = s + 12 * k
                below = (TX + dx, y - 1, TZ + dz)
                if y - 1 > F:
                    b.set(*below, "prismarine_bricks")
                    b.air.pop(below, None)
    # the landing at the bottom
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            b.set(TX + dx, F, TZ + dz, "guhs:parelmoer_tegels" if (dx + dz) % 2 else "guhs:parelmoer")
    # the bubble lift: soul sand, a column of bubbles up to the surface, glass around it, a door at the bottom
    lx, lz = LIFT
    b.set(lx, F, lz, "soul_sand")
    for y in range(F + 1, G + 1):
        b.set(lx, y, lz, "bubble_column", {"drag": "false"})
    for y in range(F, G):
        for (x, z) in ((lx - 1, lz - 1), (lx, lz - 1), (lx + 1, lz - 1), (lx + 1, lz), (lx - 1, lz + 1), (lx, lz + 1), (lx + 1, lz + 1),
                       (lx - 1, lz)):
            if (x, z) == (lx - 1, lz) and y in (F + 1, F + 2):
                continue
            b.set(x, y, z, "sea_lantern" if y % 6 == 0 and (x, z) == (lx + 1, lz) else "light_blue_stained_glass")
    b.set(lx - 1, F + 1, lz, "warped_door", {"facing": "east", "half": "lower", "hinge": "left",
                                                                            "open": "false", "powered": "false"})
    b.set(lx - 1, F + 2, lz, "warped_door", {"facing": "east", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    sign(b, TX + 2, F + 1, TZ - 1, "east", ["onderwater.lift1", "onderwater.lift2", "onderwater.lift3"], wall=False)
    # the tunnel: pearly tiles with lamps along the sides
    for z in range(CZ + 30, TZ - 2):
        for x in range(CX - 1, CX + 2):
            if (x, F + 1, z) in b.air and b.air[(x, F + 1, z)] == "tunnel":
                lamp = x != CX and z % 4 == 0
                b.set(x, F, z, "sea_lantern" if lamp else "guhs:parelmoer_tegels")
    sign(b, CX - 1, F + 3, TZ - 6, "east", ["onderwater.tunnel1", "onderwater.tunnel2", "onderwater.tunnel3"], wall=True)


def sign(b, x, y, z, facing, keys, wall=True, wood="warped"):
    """A waxed sign with lang keys (so it's in the player's language). Wall signs hang on the block behind them."""
    msgs = [json.dumps({"translate": f"sign.guhs.{k}"}) if k else '""' for k in keys] + ['""'] * (4 - len(keys))
    text = {"messages": b.h.ms.NbtList(8, msgs), "color": "black", "has_glowing_text": b.h.Byte(1)}
    empty = {"messages": b.h.ms.NbtList(8, ['""'] * 4), "color": "black", "has_glowing_text": b.h.Byte(0)}
    nbt = {"id": "minecraft:sign", "is_waxed": b.h.Byte(1), "front_text": text, "back_text": empty}
    if wall:
        b.set(x, y, z, f"{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
        b.set(x, y, z, f"{wood}_sign", {"rotation": rot, "waterlogged": "false"}, nbt)
    # (a sign stays part of the sealed air around it: you walk through it)


# =====================================================================================================================
# the Duikpost: a guh head with diving goggles and a snorkel on its island south of the dome, the stairs start inside it
# =====================================================================================================================
def duikpost(b):
    rx, ry, rz = HEAD_R
    # the floor (on the island)
    for x in range(int(TX - rx) - 1, int(TX + rx) + 2):
        for z in range(int(TZ - rz) - 1, int(TZ + rz) + 2):
            if ((x - TX) / (rx + 0.6)) ** 2 + ((z - TZ) / (rz + 0.6)) ** 2 <= 1:
                if (x, G, z) in b.air or (x, z) == LIFT:
                    continue
                if (x, G, z) in getattr(b, "steps", {}) or (x, z) == (TX, TZ):
                    continue
                ring = max(abs(x - TX), abs(z - TZ))
                b.set(x, G, z, "guhs:parelmoer" if ring == 3 else "guhs:parelmoer_tegels" if (x + z) % 2 else "pink_concrete")
    # the shell of the head, with its face towards the jetty (south)
    for x in range(int(TX - rx) - 1, int(TX + rx) + 2):
        for z in range(int(TZ - rz) - 1, int(TZ + rz) + 2):
            for y in range(G + 1, G + 3 + int(ry)):
                v = head_value(x, y, z)
                if v > 1 or (x, y, z) in b.air:
                    continue
                b.set(x, y, z, head_block(x, y, z))
    # the mouth: the door in (air, two wide... three wide)
    for x in range(TX - 1, TX + 2):
        for y in range(G + 1, G + 4):
            for z in range(TZ + 5, TZ + 10):
                if (x, y, z) not in b.air and b.get(x, y, z) not in (None, "minecraft:air"):
                    carve(b, (x, y, z), "head")
    # the ears
    for s in (-1, 1):
        ex, ey, ez = TX + s * 5, G + 10, TZ - 1
        for x in range(ex - 3, ex + 4):
            for y in range(ey - 1, ey + 5):
                for z in range(ez - 1, ez + 1):
                    if ((x - ex) / 3.0) ** 2 + ((y - ey - 1.5) / 3.2) ** 2 <= 1 and (x, y, z) not in b.air:
                        inner = z == ez and ((x - ex) / 1.8) ** 2 + ((y - ey - 1.5) / 2.2) ** 2 <= 1
                        b.set(x, y, z, "magenta_wool" if inner and z == ez else "pink_wool")
    # the snorkel: up along the east side of the head, orange and white, with a mouthpiece into the cheek
    sx, sz = TX + 8, TZ + 3
    for y in range(G + 3, G + 16):
        b.set(sx, y, sz, "orange_concrete" if (y // 2) % 2 else "white_concrete")
    b.set(sx, G + 16, sz, "orange_concrete")
    b.set(sx, G + 17, sz, "orange_concrete")
    for z in range(sz + 1, sz + 4):
        b.set(sx, G + 3, z, "orange_concrete")
    # inside: lamps, signs, a railing around the stairwell and the lift
    for (x, z) in ((TX - 6, TZ), (TX, TZ - 6), (TX + 3, TZ + 5), (TX - 4, TZ + 5), (TX + 6, TZ - 3)):
        if b.get(x, G, z) not in (None, "minecraft:air") and (x, G + 1, z) in b.air:
            b.set(x, G, z, "sea_lantern")
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if max(abs(dx), abs(dz)) != 3:
                continue
            c = (TX + dx, G + 1, TZ + dz)
            # (open on the south side where the top step is: there you step onto the stairs)
            if dz == 3 and abs(dx) <= 2 and step_index(dx, 2) == 0:
                continue
            if c not in b.air:
                continue
            b.set(*c, "warped_fence")
            b.air.pop(c, None)
    # the well of the spiral right next to the top step: a railing, so nobody falls down it
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            c = (TX + dx, G + 1, TZ + dz)
            deep = (TX + dx, G, TZ + dz) not in b.steps and (TX + dx, G - 1, TZ + dz) not in b.steps
            if deep and (TX + dx, G, TZ + dz) in b.air and any(b.steps.get((TX + dx + ex, G, TZ + dz + ez)) == 0
                                                               for ex, ez in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                b.set(*c, "warped_fence")
                b.air.pop(c, None)
    lx, lz = LIFT
    for (x, z) in ((lx + 1, lz), (lx, lz - 1), (lx + 1, lz - 1), (lx + 1, lz + 1)):
        c = (x, G + 1, z)
        if c in b.air:
            b.set(*c, "warped_fence")
            b.air.pop(c, None)
    sign(b, TX - 3, G + 2, TZ + 3, "south", ["onderwater.post1", "onderwater.post2", "onderwater.post3"], wall=False)
    b.air.pop((TX - 3, G + 2, TZ + 3), None)
    b.set(TX - 3, G + 1, TZ + 3, "guhs:parelmoer")
    b.air.pop((TX - 3, G + 1, TZ + 3), None)
    sign(b, lx, G + 2, lz - 1, "south", ["onderwater.lift1", "onderwater.lift4", ""], wall=False)
    # a sign by the path to the mouth, and the path itself (at the south end of the island a little jetty, over the water)
    for z in range(TZ + 9, D - 1):
        for x in range(TX - 3, TX + 4):
            if abs(x - TX) <= 1:
                b.set(x, G, z, "guhs:parelmoer_tegels" if (x + z) % 2 else "pink_concrete")
            elif b.get(x, G, z) in ("minecraft:water", None):
                b.set(x, G, z, "warped_planks")
    sign(b, TX + 3, G + 1, TZ + 12, "south", ["onderwater.bord1", "onderwater.bord2", "onderwater.bord3"], wall=False)
    # lily pads on the sea around the island
    rng = random.Random(7)
    for _ in range(90):
        x, z = rng.randint(10, W - 11), rng.randint(10, D - 11)
        if b.get(x, G, z) == "minecraft:water" and b.get(x, G + 1, z) is None:
            b.set(x, G + 1, z, "guhs:guh_waterlelie")


def head_block(x, y, z):
    u, v, w = x - TX, y - G - 1, z - TZ
    if w > 3:
        for s in (-1, 1):
            e = math.hypot(u - s * 3.5, (v - 5) * 1.05)
            if e <= 1.7:
                return "light_blue_stained_glass"             # the goggle lens (a window)
            if e <= 2.8:
                return "yellow_concrete"                      # the goggle frame
        if abs(u) <= 1 and 3 <= v <= 4:
            return "magenta_wool"                             # the nose
        if abs(abs(u) - 6) <= 1 and 1.5 <= v <= 3:
            return "pink_terracotta"                          # blush
    if 4.6 <= v <= 5.4:
        return "black_wool"                                   # the goggle strap
    return "pink_wool"


# =====================================================================================================================
# inside the dome: tiles, paths, the kaaskoraal garden, the Zeemeerguh's scallop, shells, ponds, doors
# =====================================================================================================================
def guh_face(n):
    """A small guh face as an n x n grid (j = 0 is the top row): {(i, j): part}."""
    out = {}
    for i in range(n):
        for j in range(n):
            x = (i + 0.5) / n * 2 - 1
            y = 1 - (j + 0.5) / n * 2
            f = (x / 0.9) ** 2 + ((y + 0.1) / 0.8) ** 2
            ear = min(math.hypot((x - s * 0.66) / 0.9, (y - 0.74)) for s in (-1, 1))
            if f > 1 and ear > 0.26:
                continue
            part = "fur"
            if f > 1:
                part = "ear" if ear < 0.14 else "fur"
            elif f > 0.8:
                part = "edge"
            elif min(math.hypot((x - s * 0.38) / 0.12, (y - 0.08) / 0.18) for s in (-1, 1)) <= 1:
                part = "eye"
            elif min(math.hypot((x - s * 0.66) / 0.13, (y + 0.28) / 0.13) for s in (-1, 1)) <= 1:
                part = "blush"
            elif abs(x) < 0.1 and -0.24 < y < -0.06:
                part = "nose"
            elif -0.46 < y < -0.3 and abs(x) < 0.22:
                part = "mouth"
            out[(i, j)] = part
    return out


def interior(b):
    rng = random.Random(SALT + 5)
    b.path = set()
    # the floor: sand beds, pearly paths and plazas
    for x in range(CX - RD, CX + RD + 1):
        for z in range(CZ - RD, CZ + RD + 1):
            if (x, F + 1, z) not in b.air:
                continue
            u, w = x - CX, z - CZ
            r = math.hypot(u, w)
            path = abs(u) <= 1 or (abs(w) <= 1 and r > 10) or 19 <= r <= 21 or r <= 11
            if r <= 11:
                block = "guhs:parelmoer" if 8.5 <= r <= 9.5 else "guhs:parelmoer_tegels"
            elif path:
                block = "guhs:parelmoer_tegels" if (x + z) % 2 else "guhs:parelmoer"
            else:
                block = pick(rng, [("sand", 70), ("guhs:kaaskoraalblok", 12), ("pink_concrete", 8), ("sea_lantern", 4), ("clay", 6)])
            if path and abs(u) == 1 and z % 5 == 0 or path and abs(w) == 1 and abs(u) > 12 and x % 5 == 0:
                block = "sea_lantern"
            b.set(x, F, z, block)
            if path:
                b.path.add((x, z))
    # the guh face mosaic on the entrance plaza (between the mouth and the scallop)
    face = guh_face(13)
    pal = {"fur": "pink_concrete", "edge": "magenta_concrete", "ear": "magenta_concrete", "eye": "black_concrete", "blush": "red_concrete",
           "nose": "magenta_concrete", "mouth": "black_concrete"}
    for (i, j), part in face.items():
        x, z = CX - 6 + i, CZ + 14 + j
        if (x, F + 1, z) in b.air:
            b.set(x, F, z, pal[part])
            b.path.add((x, z))
    # the ponds with living coral
    for (px, pz) in ((CX - 14, CZ - 14), (CX + 14, CZ - 14), (CX - 26, CZ + 8), (CX + 26, CZ + 8)):
        for x in range(px - 4, px + 5):
            for z in range(pz - 4, pz + 5):
                d = math.hypot(x - px, (z - pz) * 1.1)
                if d <= 3.2 and (x, F + 1, z) in b.air and (x, z) not in b.path:
                    b.water(x, F, z)
                    b.set(x, F - 1, z, "sand")
                    roll = rng.random()
                    if roll < 0.25:
                        b.set(x, F, z, rng.choice(["brain_coral_fan", "bubble_coral_fan", "fire_coral_fan", "horn_coral_fan", "tube_coral_fan"]),
                              {"waterlogged": "true"})
                    elif roll < 0.4:
                        b.set(x, F, z, "sea_pickle", {"pickles": str(rng.randint(2, 4)), "waterlogged": "true"})
                    elif roll < 0.5:
                        b.set(x, F, z, "guhs:kaaskoraal", {"waterlogged": "true"})
                elif d <= 4.3 and (x, F + 1, z) in b.air and (x, z) not in b.path:
                    b.set(x, F, z, "guhs:parelmoer")
    # the Zeemeerguh's scallop: a giant fan shell standing behind her, with little guh ears on top
    for u in range(-9, 10):
        for v in range(0, 10):
            d = math.hypot(u, v)
            if d > 9.2:
                continue
            ang = math.degrees(math.atan2(v, u))
            rib = int(ang // 20) % 2
            if d > 8.4 and (int(ang) % 20) < 7:
                continue                                              # the scalloped edge
            block = "guhs:parelmoer" if d > 7.6 else ("pink_concrete" if rib else "white_concrete")
            for w, blk in ((-3, block), (-4, "guhs:parelmoer")):
                c = (CX + u, F + 1 + v, CZ + w)
                if c in b.air:
                    b.set(*c, blk)
                    b.air.pop(c, None)
    for s in (-1, 1):
        for (u, v) in ((0, 0), (1, 0), (0, 1), (1, 1), (0, 2), (-1, 0)):
            c = (CX + s * 5 + s * u, F + 10 + v, CZ - 3)
            if c in b.air or b.get(*c) is None:
                b.set(*c, "magenta_concrete" if v < 2 else "pink_concrete")
                b.air.pop(c, None)
    # the big pearls in open clams on both sides of the Zeemeerguh
    for s in (-1, 1):
        cx, cz = CX + s * 6, CZ - 1
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if abs(x - cx) + abs(z - cz) <= 3:
                    b.set(x, F + 1, z, "guhs:parelmoer")
                    b.air.pop((x, F + 1, z), None)
        for (x, y, z) in ((cx, F + 2, cz), (cx + 1, F + 2, cz), (cx, F + 2, cz + 1), (cx, F + 3, cz)):
            b.set(x, y, z, "white_concrete" if (x, y, z) != (cx, F + 3, cz) else "pearlescent_froglight")
            b.air.pop((x, y, z), None)
    # the Zeemeerguh herself (she sits on the plaza, facing the entrance)
    x, y, z = NPC
    b.s.entity(x + 0.5, float(y), z + 0.5, {"id": "guhs:guh_npc", "Kind": "zeemeerguh", "PersistenceRequired": b.h.Byte(1),
                                             "Rotation": b.h.floats(0.0, 0.0)})
    b.entities.append((x, y, z, "npc"))
    b.targets["Zeemeerguh (talk)"] = (x, y, z + 2)
    for s in (-1, 1):
        b.set(CX + s * 2, F + 1, CZ + 1, "guhs:kaaskoraalblok")
        b.air.pop((CX + s * 2, F + 1, CZ + 1), None)
        b.set(CX + s * 2, F + 2, CZ + 1, "guhs:kaaskoraal", {"waterlogged": "false"})
        b.air.pop((CX + s * 2, F + 2, CZ + 1), None)
    sign(b, CX - 3, F + 1, CZ + 2, "south", ["onderwater.winkel1", "onderwater.winkel2", "onderwater.winkel3"], wall=False)
    sign(b, CX + 3, F + 1, CZ + 2, "south", ["onderwater.winkel4", "onderwater.winkel5", "onderwater.winkel6"], wall=False)
    # the garden: kaaskoraal on the sand, coral trees, benches along the ring path
    for x in range(CX - RD, CX + RD + 1):
        for z in range(CZ - RD, CZ + RD + 1):
            c = (x, F + 1, z)
            if c not in b.air or (x, z) in b.path or b.get(x, F, z) not in ("minecraft:sand", "guhs:kaaskoraalblok", "minecraft:clay",
                                                                             "minecraft:pink_concrete"):
                continue
            if rng.random() < 0.12 and (x, F + 2, z) in b.air:
                b.set(*c, "guhs:kaaskoraal", {"waterlogged": "false"})
                b.air.pop(c, None)
    trees = [(CX - 16, CZ + 26), (CX + 17, CZ + 25), (CX - 28, CZ - 6), (CX + 28, CZ - 8), (CX - 8, CZ - 26), (CX + 9, CZ - 27),
             (CX - 25, CZ + 17), (CX + 24, CZ + 18), (CX - 4, CZ + 30), (CX + 30, CZ + 2)]
    for (tx, tz) in trees:
        coral_tree(b, rng, tx, tz)
    # the reuzenschelpen inside: on the sand beside the ring path, each with a little ring of kaaskoraal
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        x, z = int(round(CX + 24.5 * math.cos(a))), int(round(CZ + 24.5 * math.sin(a)))
        shell_block(b, x, F + 1, z, facing_to(x, z, CX, CZ), inside=True)
    # benches along the ring path
    for k in range(8):
        a = math.radians(45 * k + 8)
        x, z = int(round(CX + 17.5 * math.cos(a))), int(round(CZ + 17.5 * math.sin(a)))
        c = (x, F + 1, z)
        if c in b.air and (x, z) not in b.path:
            b.set(*c, "guhs:guh_bank", {"facing": facing_to(x, z, CX, CZ)})
            b.air.pop(c, None)
    # the dive doors (north, east, west): they hold back the water
    for (dx, dz, facing) in ((0, -1, "north"), (1, 0, "east"), (-1, 0, "west")):
        dive_door(b, dx, dz, facing)
    b.targets["tube bottom"] = (TX, F + 1, TZ - 2)
    b.targets["top of the dome floor (north)"] = (CX, F + 1, CZ - 30)


def facing_to(x, z, tx, tz):
    dx, dz = tx - x, tz - z
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def coral_tree(b, rng, tx, tz):
    """A tree of kaaskoraal: a knobbly trunk of coral blocks, branches, kaaskoraal on the tips and a pearly lamp."""
    if (tx, F + 1, tz) not in b.air or (tx, tz) in b.path:
        return
    height = rng.randint(4, 7)
    top = F + height
    for y in range(F + 1, top + 1):
        if (tx, y + 2, tz) not in b.air:
            top = y - 1
            break
        b.set(tx, y, tz, "guhs:kaaskoraalblok")
        b.air.pop((tx, y, tz), None)
    b.set(tx, top + 1, tz, "pearlescent_froglight")
    b.air.pop((tx, top + 1, tz), None)
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        by = rng.randint(F + 3, max(F + 3, top))
        for k in (1, 2):
            c = (tx + dx * k, by + (k - 1), tz + dz * k)
            if c in b.air and (c[0], c[2]) not in b.path and (c[0], c[1] + 1, c[2]) in b.air:
                b.set(*c, "guhs:kaaskoraalblok")
                b.air.pop(c, None)
                last = c
            else:
                break
        else:
            up = (last[0], last[1] + 1, last[2])
            if up in b.air:
                b.set(*up, "guhs:kaaskoraal", {"waterlogged": "false"})
                b.air.pop(up, None)
    up = (tx, top + 2, tz)
    if up in b.air:
        b.set(*up, "guhs:kaaskoraal", {"waterlogged": "false"})
        b.air.pop(up, None)


def shell_block(b, x, y, z, facing, inside):
    b.set(x, y, z, "guhs:reuzenschelp", {"facing": facing, "parel": "true", "waterlogged": "false" if inside else "true"})
    b.air.pop((x, y, z), None)
    b.set(x, y - 1, z, "guhs:parelmoer")
    b.shells.append((x, y, z, inside))


def dive_door(b, dx, dz, facing):
    """A double door in the dome wall at floor level, in a pearly frame. Outside: the reef (and kaaskoraal for air)."""
    # walk out from the middle until the first spot that isn't the dome's air
    k = 10
    while (CX + dx * k, F + 1, CZ + dz * k) in b.air:
        k += 1
    ox, oz = CX + dx * k, CZ + dz * k                 # the wall
    side = (-dz, dx)                                  # along the wall
    cells = [(ox, oz), (ox + side[0], oz + side[1])]
    for i, (x, z) in enumerate(cells):
        hinge = "left" if i == 0 else "right"
        b.set(x, F + 1, z, "warped_door", {"facing": facing, "half": "lower", "hinge": hinge, "open": "false", "powered": "false"})
        b.set(x, F + 2, z, "warped_door", {"facing": facing, "half": "upper", "hinge": hinge, "open": "false", "powered": "false"})
        b.set(x, F, z, "guhs:parelmoer")
        b.set(x, F + 3, z, "guhs:parelmoer")
        b.air.pop((x, F + 1, z), None)
        b.air.pop((x, F + 2, z), None)
    for (x, z) in ((ox - side[0], oz - side[1]), (ox + 2 * side[0], oz + 2 * side[1])):
        for y in range(F + 1, F + 4):
            if (x, y, z) not in b.air:
                b.set(x, y, z, "guhs:parelmoer")
    # the steps in front of and behind the door stay free
    inner = (ox - dx, F + 1, oz - dz)
    outer = (ox + dx, F + 1, oz + dz)
    for c in (inner, (inner[0] + side[0], F + 1, inner[2] + side[1])):
        for up in (0, 1):
            cc = (c[0], c[1] + up, c[2])
            b.s.set(*cc, "minecraft:air")
            b.air[cc] = "dome"
    b.set(ox - dx, F, oz - dz, "guhs:parelmoer_tegels")
    sign(b, ox - 2 * dx - side[0], F + 1, oz - 2 * dz - side[1], {"north": "south", "south": "north", "east": "west", "west": "east"}[facing],
         ["onderwater.deur1", "onderwater.deur2", "onderwater.deur3"], wall=False)
    b.doors.append((inner, outer))
    b.door_fronts.append((ox, oz, dx, dz, side))
    b.targets[f"dive door {facing} (inside)"] = inner


def door_fronts(b):
    """Clear the reef right outside every dive door and put an air station there."""
    for (ox, oz, dx, dz, side) in b.door_fronts:
        door_front(b, ox, oz, dx, dz, side)


def door_front(b, ox, oz, dx, dz, side):
    for s in (-2, -1, 0, 1, 2):
        for t in range(1, 3):
            x, z = ox + dx * t + side[0] * s, oz + dz * t + side[1] * s
            for y in range(F + 1, F + 4):
                if b.get(x, y, z) not in ("minecraft:water",) and (x, y, z) not in b.air and b.get(x, y, z) not in (None,) \
                        and "glass" not in (b.get(x, y, z) or "") and "parelmoer" not in (b.get(x, y, z) or "") and "door" not in (b.get(x, y, z) or ""):
                    b.water(x, y, z)
    for s in (-3, 3):
        x, z = ox + dx * 2 + side[0] * s, oz + dz * 2 + side[1] * s
        if b.get(x, F + 1, z) == "minecraft:water":
            b.set(x, F + 1, z, "guhs:kaaskoraal", {"waterlogged": "true"})


# =====================================================================================================================
# outside: the reef, shells, air stations, the sunken guh ship, two wild Zeemeerguhs
# =====================================================================================================================
def reef(b):
    rng = random.Random(SALT + 9)
    corals = ["brain_coral", "bubble_coral", "fire_coral", "horn_coral", "tube_coral"]
    for (x, z), f in b.floor.items():
        c = (x, f + 1, z)
        if b.get(*c) != "minecraft:water" or b.get(x, f + 2, z) != "minecraft:water":
            continue
        r = math.hypot(x - CX, z - CZ)
        if r < RD + 1.5:
            continue
        roll = rng.random()
        if roll < 0.035 and f == F:
            kind = rng.choice(corals)
            b.set(*c, f"{kind}_block")
            if b.get(x, f + 2, z) == "minecraft:water" and rng.random() < 0.6:
                b.set(x, f + 2, z, f"{kind}_fan", {"waterlogged": "true"})
        elif roll < 0.15:
            b.set(*c, "seagrass")
        elif roll < 0.175:
            b.set(*c, "sea_pickle", {"pickles": str(rng.randint(1, 4)), "waterlogged": "true"})
        elif roll < 0.19 and f < G - 6:
            height = rng.randint(3, min(14, G - f - 4))
            for k in range(height):
                y = f + 1 + k
                if b.get(x, y, z) != "minecraft:water" or b.get(x, y + 1, z) != "minecraft:water":
                    break
                b.set(x, y, z, "kelp_plant" if k < height - 1 else "kelp", {} if k < height - 1 else {"age": "20"})
        elif roll < 0.2 and f > F:
            b.set(*c, rng.choice(corals) + "_fan", {"waterlogged": "true"})
    # sea lanterns in the sea floor, so the reef isn't pitch black down there
    for k in range(40):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(RD + 2, FLAT)
        x, z = int(CX + r * math.cos(a)), int(CZ + r * math.sin(a))
        if b.floor.get((x, z)) == F and b.get(x, F, z) not in (None, "minecraft:water"):
            b.set(x, F, z, "sea_lantern")
    # air stations: little clumps of kaaskoraal all around the dome
    for k in range(16):
        a = math.radians(11.25 + 22.5 * k)
        for r in (42.5,):
            x, z = int(round(CX + r * math.cos(a))), int(round(CZ + r * math.sin(a)))
            f = b.floor.get((x, z))
            if f is None:
                continue
            for (dx, dz) in ((0, 0), (1, 0), (0, 1)):
                c = (x + dx, b.floor.get((x + dx, z + dz), f) + 1, z + dz)
                if b.get(*c) in ("minecraft:water", "minecraft:seagrass") and b.get(c[0], c[1] - 1, c[2]) not in FLUID:
                    b.set(*c, "guhs:kaaskoraal", {"waterlogged": "true"})
    # the reuzenschelpen on the reef: near the stations
    for k in range(8):
        a = math.radians(22.5 * 2 * k + 5)
        x, z = int(round(CX + 44.5 * math.cos(a))), int(round(CZ + 44.5 * math.sin(a)))
        if abs(x - TX) < 6 and z > CZ + 30 or WRECK[0] - 3 <= x <= WRECK[1] + 5 and abs(z - WRECK[2]) <= 6:
            continue                                              # (not in front of the tube, not in the wreck)
        f = b.floor.get((x, z))
        if f is None:
            continue
        shell_block(b, x, f + 1, z, facing_to(x, z, CX, CZ), inside=False)
        for (dx, dz) in ((1, 1), (-1, -1), (1, -1), (-1, 1)):
            ff = b.floor.get((x + dx, z + dz))
            if ff is None:
                continue
            c = (x + dx, ff + 1, z + dz)
            if b.get(*c) in ("minecraft:water", "minecraft:seagrass", "minecraft:sea_pickle") or (b.get(*c) or "").endswith("coral_fan"):
                b.set(*c, "guhs:kaaskoraal", {"waterlogged": "true"})
    # two wild Zeemeerguhs in the sea (they come and go like any wild guh)
    for (x, y, z) in ((CX + 44, G - 8, CZ - 6), (CX - 45, G - 10, CZ + 10)):
        if b.get(x, y, z) == "minecraft:water":
            b.s.entity(x + 0.5, float(y), z + 0.5, {"id": "guhs:guh", "Variant": "zeemeerguh", "attributes": b.h.ms.compounds(
                [{"id": "minecraft:generic.scale", "base": b.h.Double(1.45)}])})


def wreck(b):
    """The sunken guh ship, lying on the sea floor just north of the dome: stern (with the cabin and the treasure) to the west."""
    x0, x1, zc = WRECK
    rng = random.Random(SALT + 13)
    n = x1 - x0
    keel = {x: F - 1 + (2 * (x - x0)) // n for x in range(x0, x1 + 4)}
    b.wreck_cells = set()

    def put(x, y, z, name, props=None, nbt=None):
        if (x, y, z) in b.air:
            return
        b.set(x, y, z, name, props, nbt)
        b.wreck_cells.add((x, y, z))

    for x in range(x0, x1 + 1):
        taper = max(0, x - (x1 - 5))
        hw = 3 - (taper * 3) // 5
        yk = keel[x]
        for dz in range(-hw, hw + 1):
            z = zc + dz
            for dy in range(0, 6):
                y = yk + dy
                side = abs(dz) == hw
                if dy == 0 or (side and dy <= 4):
                    if side and 1 <= dy <= 2 and x0 + 9 <= x <= x0 + 12 and dz > 0:
                        b.water(x, y, z)                              # the hole in the hull (towards the dome)
                        continue
                    put(x, y, z, "stripped_dark_oak_log" if dy == 4 else "dark_oak_planks",
                        {"axis": "x"} if dy == 4 else None)
                elif dy == 3 and not side:
                    if rng.random() < 0.22 or (x0 + 6 <= x <= x0 + 7 and abs(dz) <= 1):
                        b.water(x, y, z)                              # holes and the hatch in the deck
                    else:
                        put(x, y, z, "spruce_planks")
                elif dy < 3:
                    b.water(x, y, z)
            # under the keel: sand heaped up (the bow rests a little higher)
            for y in range(F, yk):
                put(x, y, z, "sand")
    # the stern cabin with the treasure
    for x in range(x0, x0 + 5):
        for dz in range(-3, 4):
            z = zc + dz
            yk = keel[x]
            for dy in range(4, 8):
                wall = x in (x0, x0 + 4) or abs(dz) == 3 or dy == 7
                door = x == x0 + 4 and abs(dz) <= 1 and dy in (4, 5)
                window = abs(dz) == 3 and dy == 5 and x == x0 + 2
                if wall and not door:
                    put(x, yk + dy, z, "light_blue_stained_glass" if window else "dark_oak_planks")
                else:
                    b.water(x, yk + dy, z)
    yk = keel[x0 + 2]
    b.set(x0 + 2, yk + 4, zc, "chest", {"facing": "east", "type": "single", "waterlogged": "true"},
          {"id": "minecraft:chest", "LootTable": "guhs:chests/onderwater_wrak"})
    b.targets["the treasure chest (swim)"] = (x0 + 3, yk + 4, zc)
    b.chest = (x0 + 2, yk + 4, zc)
    b.set(x0 + 1, yk + 4, zc - 2, "guhs:kaaskoraal", {"waterlogged": "true"})
    b.set(x0 + 1, yk + 4, zc + 2, "guhs:kaaskoraal", {"waterlogged": "true"})
    b.set(x0 + 3, yk + 6, zc - 2, "sea_pickle", {"pickles": "4", "waterlogged": "true"})
    b.set(x0 + 3, yk + 5, zc - 2, "dark_oak_planks")
    # the masts: one still standing (with a torn pink sail), one broken off
    mx = x0 + 11
    top = keel[mx] + 4
    for y in range(top, min(G - 3, top + 12)):
        put(mx, y, zc, "spruce_log", {"axis": "y"})
    for y in range(top + 6, top + 10):
        for dz in range(-3, 4):
            if dz != 0 and rng.random() > 0.2:
                put(mx + 1, y, zc + dz, "pink_wool" if (y + dz) % 3 else "white_wool")
    for y in range(keel[x0 + 17] + 4, keel[x0 + 17] + 8):
        put(x0 + 17, y, zc, "spruce_log", {"axis": "y"})
    # the figurehead: a little guh head on the bow
    bx, by = x1 + 1, keel[x1] + 3
    for x in range(bx, bx + 3):
        for y in range(by, by + 3):
            for z in range(zc - 1, zc + 2):
                put(x, y, z, "pink_wool")
    put(bx + 2, by + 1, zc - 1, "black_wool")
    put(bx + 2, by + 1, zc + 1, "black_wool")
    put(bx + 2, by, zc, "magenta_wool")
    put(bx + 1, by + 3, zc - 1, "pink_wool")
    put(bx + 1, by + 3, zc + 1, "pink_wool")
    # kaaskoraal on the deck (air!) and a shell by the bow
    for (x, dz) in ((x0 + 8, -1), (x0 + 14, 1), (x0 + 16, -2)):
        y = keel[x] + 4
        if b.get(x, y - 1, zc + dz) == "minecraft:spruce_planks" and b.get(x, y, zc + dz) == "minecraft:water":
            b.set(x, y, zc + dz, "guhs:kaaskoraal", {"waterlogged": "true"})


# =====================================================================================================================
# the self-check
# =====================================================================================================================
def check(b):
    """Seals, ways and water: the dome, tube and tunnel are air-tight (no water next to their air except through doors),
    the dome really is on the sea floor under water, you can walk from the jetty down to the Zeemeerguh and back up
    (stairs down, bubble lift up), the dive doors lead out onto the reef and every outside shell and the wreck's chest are
    close to air (kaaskoraal), the Zeemeerguh and the shells sit on something solid, and nothing where you walk is dark.
    Outside the template's round piece of sea the real sea goes on: water from the floor up to G."""
    blocks = b.s.blocks
    problems = []

    def name(c):
        bl = blocks.get(c)
        if bl is None:
            if c[1] > G:
                return None
            if not in_sea(c[0], c[2]) and c[1] > F:
                return "minecraft:water"                   # the real sea around the template
            return "natural"
        return bl[0]

    def props(c):
        bl = blocks.get(c)
        return bl[1] if bl else {}

    def fluid(c):
        n = name(c)
        return n in FLUID or props(c).get("waterlogged") == "true" or n in ("minecraft:seagrass", "minecraft:kelp", "minecraft:kelp_plant")

    def passable(c):
        n = name(c)
        if n in AIRY or fluid(c):
            return True
        return n != "natural" and (n == "guhs:kaaskoraal" or any(t in n for t in THIN))

    def support(c):
        n = name(c)
        return n is not None and n != "minecraft:air" and not passable(c) and not any(t in n for t in FENCY)

    def inside(c):
        return in_template(*c)

    # --- the seal: water never touches the sealed air sideways or from above ---
    leaks = []
    for c, bl in blocks.items():
        if bl[0] not in FLUID and not (bl[1].get("waterlogged") == "true"):
            continue
        x, y, z = c
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if not inside(n) and name(n) != "minecraft:water":
                leaks.append((c, n, "the edge of the template"))
                continue
            nn = name(n)
            if nn in AIRY and not (y + dy > G):
                leaks.append((c, n, nn))
            elif nn is not None and nn != "natural" and wets(nn) and props(n).get("waterlogged") != "true" \
                    and n not in b.air and "door" not in nn:
                leaks.append((c, n, nn + " (waterloggable, would get wet)"))
    if leaks:
        problems.append(f"{len(leaks)} leaks, e.g. {leaks[:8]}")
    # --- really on the sea floor: the dome's rim lies on the sea floor, with water above the dome ---
    top = max(y for (x, y, z), zone in b.air.items() if zone == "dome" and (x, z) == (CX, CZ))
    over = [y for y in range(top + 2, G + 1) if name((CX, y, CZ)) != "minecraft:water"]
    if top + 1 >= G - 4 or over:
        problems.append(f"the dome isn't deep under water (top {top + 1}, surface {G}, not water at {over[:5]})")
    for (x, z) in ((CX - RD + 1, CZ), (CX + RD - 1, CZ), (CX, CZ - RD + 1)):
        if b.floor.get((x, z)) != F:
            problems.append(f"the dome's rim at {(x, z)} isn't on the flat sea floor")
    if name((CX, F, CZ)) in AIRY or fluid((CX, F, CZ)):
        problems.append("no floor under the middle of the dome")

    # --- walking (and swimming) like a player ---
    holes = set()

    def moves(c, swim):
        x, y, z = c
        out = []
        n = name(c)
        if n == "minecraft:bubble_column":
            up = (x, y + 1, z)
            if inside(up) and passable(up):
                out.append(up)
        elif fluid(c) and swim:
            for dy in (1, -1):
                v = (x, y + dy, z)
                if inside(v) and passable(v):
                    out.append(v)
        elif fluid(c) and y == G:
            pass
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            up = (nx, y + 1, nz)
            if inside(up) and passable(up) and passable((nx, y + 2, nz)) and passable((x, y + 2, z)) and support((nx, y, nz)):
                out.append(up)
            nb = (nx, y, nz)
            if not (inside(nb) and passable(nb) and passable((nx, y + 1, nz))):
                if inside(nb) and "door" in (name(nb) or ""):
                    out.append(nb)                         # (doors open)
                continue
            if fluid(nb) and not swim:
                continue
            if "door" in (name(c) or "") and fluid(nb) and swim:
                out.append(nb)
                continue
            yy = y
            while yy > 0 and not support((nx, yy - 1, nz)) and not fluid((nx, yy, nz)):
                yy -= 1
            fall = y - yy
            land = (nx, yy, nz)
            if fall > 3 and not fluid(land):
                holes.add((nb, fall))
                continue
            if fluid(land) and not swim:
                continue
            out.append(land)
        return out

    def bfs(start, swim=False, limit=None):
        seen = {start}
        todo = [start]
        while todo:
            c = todo.pop()
            for nb in moves(c, swim):
                if nb not in seen and (limit is None or limit(nb)):
                    seen.add(nb)
                    todo.append(nb)
        return seen

    start = (TX, G + 1, TZ + 12)
    if not support((start[0], start[1] - 1, start[2])):
        problems.append(f"the start {start} isn't on the ground")
    dry = bfs(start)
    for label, c in b.targets.items():
        if "swim" in label:
            continue
        if c not in dry:
            problems.append(f"can't walk to {label} at {c} from the Duikpost's jetty")
    for (x, y, z, inside_) in b.shells:
        if inside_ and not any((x + dx, y, z + dz) in dry for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            problems.append(f"can't walk up to the shell at {(x, y, z)}")
    # back up: from the Zeemeerguh through the tunnel into the lift and out of the head again
    wet = bfs(b.targets["Zeemeerguh (talk)"], swim=True, limit=lambda c: c in b.air or name(c) in FLUID + AIRY or "door" in (name(c) or "")
              or c[1] > G or fluid(c) and math.hypot(c[0] - LIFT[0], c[2] - LIFT[1]) < 2)
    if not any(abs(c[0] - start[0]) <= 1 and c[1] == start[1] and abs(c[2] - start[2]) <= 1 for c in wet):
        problems.append("can't get back up from the Zeemeerguh to the island (tunnel, stairs or bubble lift)")
    if (LIFT[0], G + 1, LIFT[1]) not in wet:
        problems.append("the bubble lift doesn't bring you up into the Duikpost")
    for (inner, outer) in b.doors:
        if inner not in dry:
            problems.append(f"can't walk to the dive door at {inner}")
        if not fluid(outer):
            problems.append(f"no water outside the dive door at {outer}")
    # swimming out of a dive door: the reef, the outside shells, the chest; and a swimmer can climb out onto the island
    breath = {}
    todo = []
    for c, bl in blocks.items():
        if bl[0] == "guhs:kaaskoraal" and bl[1].get("waterlogged") == "true":
            for dx, dy, dz in ((0, 0, 0), (1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, 0, 1), (0, 0, -1)):
                n = (c[0] + dx, c[1] + dy, c[2] + dz)
                if fluid(n):
                    breath[n] = 0
                    todo.append(n)
    for (inner, outer) in b.doors:
        breath[outer] = 0
        todo.append(outer)
    for x in range(W):
        for z in range(D):
            if name((x, G, z)) == "minecraft:water":
                breath[(x, G, z)] = 0
                todo.append((x, G, z))
    head = 0
    while head < len(todo):
        c = todo[head]
        head += 1
        d = breath[c]
        if d >= 30:
            continue
        x, y, z = c
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if inside(n) and fluid(n) and breath.get(n, 99) > d + 1:
                breath[n] = d + 1
                todo.append(n)
    for (x, y, z, inside_) in b.shells:
        if inside_:
            continue
        near = [breath.get((x + dx, y + dy, z + dz), 99) for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0))]
        if min(near) > 8:
            problems.append(f"the shell at {(x, y, z)} is {min(near)} swims from air")
    chest_spot = b.targets["the treasure chest (swim)"]
    if breath.get(chest_spot, 99) > 8:
        problems.append(f"the treasure chest is {breath.get(chest_spot, 99)} swims from air")
    north_out = b.doors[0][1]
    swim = bfs(north_out, swim=True, limit=lambda c: fluid(c) or name(c) in AIRY or c[1] > G - 1)
    if chest_spot not in swim:
        problems.append("can't swim from the north dive door to the treasure chest")
    for (x, y, z, inside_) in b.shells:
        if not inside_ and not any((x + dx, y + dy, z + dz) in swim for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0))):
            problems.append(f"can't swim to the shell at {(x, y, z)}")
    if not any(c[1] == G + 1 and support((c[0], G, c[2])) for c in swim):
        problems.append("a swimmer can't climb out of the sea onto the island")
    for (n, fall) in sorted(holes):
        if n in b.air:
            problems.append(f"hole: stepping into {n} drops {fall} blocks")

    # --- things that must stand on something ---
    for (x, y, z, kind) in b.entities:
        if not support((x, y - 1, z)) or not passable((x, y, z)):
            problems.append(f"{kind} at {(x, y, z)} doesn't sit on solid ground ({name((x, y - 1, z))}, {name((x, y, z))})")
    for (x, y, z, inside_) in b.shells:
        if not support((x, y - 1, z)):
            problems.append(f"the shell at {(x, y, z)} stands on nothing")
    for c, (n, pr, _) in blocks.items():
        below = (c[0], c[1] - 1, c[2])
        if n in ("minecraft:sand", "minecraft:gravel", "minecraft:pink_concrete_powder") and name(below) != "natural" and not support(below):
            problems.append(f"{n} at {c} would fall")
        if (n in ("guhs:kaaskoraal", "minecraft:seagrass", "minecraft:sea_pickle", "guhs:reuzenschelp") or n.endswith("coral_fan")) \
                and not support(below):
            problems.append(f"{n} at {c} has nothing under it")
        if n.endswith("_door") and pr.get("half") == "lower" and not support(below):
            problems.append(f"door at {c} has no floor")
        if n == "minecraft:kelp_plant" and name((c[0], c[1] + 1, c[2])) not in ("minecraft:kelp", "minecraft:kelp_plant"):
            problems.append(f"kelp at {c} has no top")
        if n == "minecraft:bubble_column" and name(below) not in ("minecraft:bubble_column", "minecraft:soul_sand"):
            problems.append(f"bubble column at {c} has no soul sand under it")

    # --- darkness: every place you can walk inside (dome, tube, tunnel, head) gets block light ---
    light = {}
    todo = []
    for c, bl in blocks.items():
        lvl = LIGHT.get(bl[0].split(":")[1], 0)
        if bl[0] == "minecraft:sea_pickle":
            lvl = 3 + 3 * int(bl[1].get("pickles", "1"))
        if lvl:
            light[c] = lvl
            todo.append(c)
    see_through = lambda c: passable(c) or any(t in (name(c) or "") for t in FENCY + ("glass", "stairs", "chest", "reuzenschelp", "bank"))
    head = 0
    while head < len(todo):
        c = todo[head]
        head += 1
        lv = light[c] - 1
        if lv <= 0:
            continue
        x, y, z = c
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if inside(n) and light.get(n, 0) < lv and see_through(n):
                light[n] = lv
                todo.append(n)
    dark = [c for c in dry if c in b.air and light.get(c, 0) < 5]
    if dark:
        problems.append(f"{len(dark)} dark spots where you can stand, e.g. {sorted(dark)[:30]}")
    b.stats = {"walkable": len(dry), "blocks": len(blocks), "water": sum(1 for v in blocks.values() if v[0] == "minecraft:water"),
               "shells": len(b.shells), "air": len(b.air)}
    return problems


# =====================================================================================================================
# build the template
# =====================================================================================================================
def build_structure(h):
    b = Bubble(h)
    sea_floor(b)
    air_regions(b)
    stair_tube(b)
    shell(b)
    duikpost(b)
    interior(b)
    reef(b)
    wreck(b)
    door_fronts(b)
    fp = [(x, z) for x in range(W) for z in range(D)]
    b.s.clear_above(fp, G + 1)
    # the anchor: the whole bubble is placed around it (GuhbubbelStructure puts it one block above the sea level). It
    # hangs in the air just above the middle of the sea, so the game checks the biome at the surface
    b.set(CX, G + 1, CZ, "jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": "guhs:onderwater_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": "minecraft:air", "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    problems = check(b)
    b.s.save("onderwater")
    print(f"onderwater: {b.stats}")
    b.problems = problems
    if problems:
        print("onderwater geometry check found problems:\n  " + "\n  ".join(problems[:40]))
    else:
        print("onderwater: geometry check ok")
    return b


# =====================================================================================================================
# textures
# =====================================================================================================================
CHEESE = (250, 200, 70)
CHEESE_DARK = (206, 146, 38)
CHEESE_LIGHT = (255, 232, 140)
CORAL_PINK = (246, 120, 170)


def nacre(size, seed, base=(236, 230, 244)):
    """Mother-of-pearl: pale, with soft rainbow swirls."""
    import colorsys
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    ph = [rng.uniform(0, 6.3) for _ in range(3)]
    for y in range(size):
        for x in range(size):
            t = math.sin(x * 0.55 + ph[0]) + math.sin(y * 0.43 + x * 0.21 + ph[1]) + math.sin((x - y) * 0.31 + ph[2])
            hue = (t / 6 + 0.5) % 1.0
            rgb = colorsys.hsv_to_rgb(hue, 0.18, 1.0)
            c = [int(base[i] * 0.6 + rgb[i] * 255 * 0.4) + rng.randint(-5, 5) for i in range(3)]
            img.putpixel((x, y), tuple(max(0, min(255, v)) for v in c) + (255,))
    return img


def kaaskoraal_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    branches = [((8, 16), (8, 9)), ((8, 10), (4, 5)), ((8, 10), (12, 4)), ((4, 6), (2, 2)), ((4, 6), (6, 1)), ((12, 5), (10, 1)),
                ((12, 5), (14, 2)), ((8, 12), (13, 9))]
    for a, b_ in branches:
        d.line([a, b_], fill=CHEESE + (255,), width=2)
    px = img.load()
    for (x, y) in ((8, 13), (5, 7), (11, 6), (8, 10)):
        px[x, y] = CHEESE_DARK + (255,)                        # cheese holes
    for (x, y) in ((2, 2), (6, 1), (10, 1), (14, 2), (13, 9), (2, 3), (14, 3)):
        px[x, y] = CORAL_PINK + (255,)                         # pink tips
    for (x, y) in ((7, 9), (4, 5), (12, 4)):
        px[x, y] = CHEESE_LIGHT + (255,)
    return img


def kaaskoraalblok_texture(h):
    img = h.noise_tex(CHEESE, 12, 31, spots=[(CHEESE_LIGHT, 0.12), (CORAL_PINK, 0.04)])
    px = img.load()
    rng = random.Random(32)
    for _ in range(7):
        cx, cy = rng.randint(1, 14), rng.randint(1, 14)
        for (x, y) in ((cx, cy), (cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1)):
            if x < 16 and y < 16:
                px[x, y] = CHEESE_DARK + (255,)
        px[cx, cy] = (150, 100, 30, 255)
    return img


def tiles_texture():
    img = nacre(16, 41)
    px = img.load()
    for i in range(16):
        for j in (0, 8):
            px[i, j] = (200, 186, 206, 255)
            px[j, i] = (200, 186, 206, 255)
    for (x, y) in ((4, 4), (12, 12), (4, 12), (12, 4)):
        px[x, y] = (255, 214, 232, 255)
    return img


def shell_textures(h):
    out = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            rib = (x // 2) % 2
            shade = 1.0 - y * 0.02
            base = (250, 196, 214) if rib else (255, 236, 242)
            out.putpixel((x, y), tuple(int(c * shade) for c in base) + (255,))
    for x in range(16):
        out.putpixel((x, 15), (214, 130, 160, 255))
    h.save(out, "block", "reuzenschelp_buiten.png")
    h.save(nacre(16, 44, base=(255, 214, 230)), "block", "reuzenschelp_binnen.png")
    parel = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = max(0, 1 - math.hypot(x - 5, y - 5) / 16)
            parel.putpixel((x, y), (int(228 + 27 * v), int(226 + 29 * v), int(236 + 19 * v), 255))
    parel.putpixel((4, 4), (255, 255, 255, 255))
    parel.putpixel((5, 4), (255, 255, 255, 255))
    h.save(parel, "block", "reuzenschelp_parel.png")


def item_icons(h):
    parel = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 8)
            if d <= 5.2:
                v = max(0.0, 1 - math.hypot(x - 5.5, y - 6) / 9)
                c = (int(210 + 45 * v), int(206 + 49 * v), int(224 + 31 * v))
                if d > 4.4:
                    c = (178, 166, 196)
                parel.putpixel((x, y), c + (255,))
    for (x, y) in ((5, 5), (6, 5), (5, 6)):
        parel.putpixel((x, y), (255, 255, 255, 255))
    parel.putpixel((10, 11), (255, 214, 232, 255))
    h.save(parel, "item", "parel.png")
    helm = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(helm)
    d.ellipse((1, 1, 14, 14), fill=(206, 150, 60, 255), outline=(120, 80, 30, 255))
    d.ellipse((4, 4, 11, 11), fill=(150, 220, 240, 255), outline=(90, 60, 25, 255))
    d.rectangle((2, 13, 13, 15), fill=(160, 110, 45, 255), outline=(110, 70, 25, 255))
    px = helm.load()
    for (x, y) in ((5, 6), (6, 5)):
        px[x, y] = (240, 255, 255, 255)
    for (x, y) in ((2, 7), (13, 7), (7, 1), (8, 1)):
        px[x, y] = (255, 226, 140, 255)                       # rivets
    for x in (4, 11):
        px[x, 0] = (246, 150, 190, 255)                       # little guh ears on top
        px[x, 1] = (246, 150, 190, 255)
    h.save(helm, "item", "duikhelm.png")


def armor_texture(h):
    """The duikhelm on the player (armor layer 1: only the head part), a brass diving helmet with a big round window."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    brass, dark, light = (206, 150, 60), (140, 96, 34), (240, 196, 110)
    faces = {"top": (8, 0), "bottom": (16, 0), "right": (0, 8), "front": (8, 8), "left": (16, 8), "back": (24, 8)}
    for face, (u, v) in faces.items():
        if face == "bottom":
            continue
        for y in range(8):
            for x in range(8):
                c = brass if (x + y) % 5 else light
                if x in (0, 7) or y in (0, 7):
                    c = dark
                px[u + x, v + y] = c + (255,)
        if face in ("front", "left", "right"):
            r = 2.6 if face == "front" else 1.6
            for y in range(8):
                for x in range(8):
                    d = math.hypot(x - 3.5, y - 3.6)
                    if d <= r - 0.9:
                        px[u + x, v + y] = (0, 0, 0, 0)          # see-through glass (your face shows)
                    elif d <= r:
                        px[u + x, v + y] = (150, 220, 240, 255)
                    elif d <= r + 0.5:
                        px[u + x, v + y] = dark + (255,)
        if face == "top":
            for (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4)):
                px[u + x, v + y] = light + (255,)
            for (x, y) in ((1, 1), (6, 1)):
                px[u + x, v + y] = (246, 150, 190, 255)        # guh ears painted on
    h.save(img, "entity", "equipment", "humanoid", "duikhelm.png")  # 26.1 equipment asset guhs:duikhelm
    h.save(Image.new("RGBA", (64, 32), (0, 0, 0, 0)), "entity", "equipment", "humanoid_leggings", "duikhelm.png")


# =====================================================================================================================
# build
# =====================================================================================================================
LANG = {
    # blocks and items
    "block.guhs.kaaskoraal": ("Cheese Coral", "Kaaskoraal"),
    "block.guhs.kaaskoraalblok": ("Block of Cheese Coral", "Kaaskoraalblok"),
    "block.guhs.parelmoer": ("Mother-of-Pearl", "Parelmoer"),
    "block.guhs.parelmoer_tegels": ("Mother-of-Pearl Tiles", "Parelmoertegels"),
    "block.guhs.reuzenschelp": ("Giant Guh Shell", "Reuzenschelp"),
    "item.guhs.parel": ("Pearl", "Parel"),
    "item.guhs.parel.lore": ("From the giant guh shells of the Guh Bubble. The Zeemeerguh takes them as money.",
                             "Uit de reuzenschelpen van de Guhbubbel. De Zeemeerguh neemt ze aan als geld."),
    "item.guhs.duikhelm": ("Guh Diving Helmet", "Duikhelm"),
    "item.guhs.duikhelm.lore": ("Breathe under water (and see a little better down there too).", "Adem onder water (en zie daar beneden ook wat beter)."),
    "block.guhs.kaaskoraal.lore": ("Blows air bubbles: under water, close to it you can breathe again.",
                                  "Blaast luchtbelletjes: onder water kun je er vlakbij weer ademen."),
    "item.guhs.duikbril": ("Guh Diving Goggles", "Duikbril"),
    "item.guhs.snorkel": ("Guh Snorkel", "Snorkel"),
    "item.guhs.zwemband": ("Guh Swim Ring", "Zwemband"),
    "entity.guhs.guh_npc.zeemeerguh": ("Zeemeerguh", "Zeemeerguh"),
    "entity.guhs.guh.zeemeerguh": ("Mer-Guh", "Zeemeerguh"),
    "structure.guhs.onderwater": ("Guh Bubble", "Guhbubbel"),
    "structure.guhs.onderwater.tooltip": ("In the middle of every Deep Guh Sea: a glass bubble on the sea floor, with the Zeemeerguh's shop, giant shells and a shipwreck",
                                          "In het midden van elke Diepe Guhzee: een glazen bubbel op de zeebodem, met de winkel van de Zeemeerguh, reuzenschelpen en een wrak"),
    # the Zeemeerguh
    "quest.guhs.onderwater.hello": ("Blub blub, VAHOEG! Welcome to the Guh Bubble, the only dry spot on the bottom of the guh sea! The giant shells around here sometimes hold a pearl: pearls are money down here. Need air outside? Swim to the cheese coral, it blows bubbles. And there's a sunken guh ship just north of my bubble... njeg, I never dared to look inside.",
                                    "Blub blub, VAHOEG! Welkom in de Guhbubbel, het enige droge plekje op de bodem van de guhzee! In de reuzenschelpen hier zit soms een parel: parels zijn hier beneden geld. Buiten lucht nodig? Zwem naar het kaaskoraal, dat blaast belletjes. En vlak ten noorden van mijn bubbel ligt een gezonken guhschip... njeg, ik durfde nooit naar binnen te kijken."),
    "quest.guhs.onderwater.tip0": ("A shell that's closed opens again by itself. Pearls grow slowly, just like a vadsige guh!",
                                   "Een dichte schelp gaat vanzelf weer open. Parels groeien langzaam, net als een vadsige guh!"),
    "quest.guhs.onderwater.tip1": ("With a diving helmet you can stay under water as long as you like. Only ten pearls, blub!",
                                   "Met een duikhelm blijf je zo lang onder water als je wilt. Maar tien parels, blub!"),
    "quest.guhs.onderwater.tip2": ("Cheese coral blows air bubbles. Swim close and breathe in: VAHOEG!",
                                   "Kaaskoraal blaast luchtbelletjes. Zwem er dichtbij en adem in: VAHOEG!"),
    "quest.guhs.onderwater.tip3": ("Out in the guh sea swim my wild cousins, the Zeemeerguhs. Kaasknabbels, a saddle, and off you go, under water!",
                                   "Buiten in de guhzee zwemmen mijn wilde neefjes, de Zeemeerguhs. Kaasknabbels, een zadel en hup, onder water rijden!"),
    "quest.guhs.onderwater.tip4": ("Goggles, a snorkel and a swim ring: the duikpakje for your guh. Only I sell it, njeg!",
                                   "Een duikbril, een snorkel en een zwemband: het duikpakje voor je guh. Alleen ik verkoop het, njeg!"),
    "quest.guhs.onderwater.shell_closed": ("This shell is closed... a new pearl is growing. Come back later!",
                                           "Deze schelp is dicht... er groeit een nieuwe parel. Kom straks terug!"),
    "quest.guhs.onderwater.shell_pearl": ("The shell opens: a pearl! VAHOEG!", "De schelp gaat open: een parel! VAHOEG!"),
    "quest.guhs.onderwater.no_build": ("Njeg! The Guh Bubble is glass and mother-of-pearl: hands off, or it fills up with water!",
                                       "Njeg! De Guhbubbel is van glas en parelmoer: afblijven, anders loopt hij vol water!"),
    "quest.guhs.onderwater.ride": ("Blub! On a Zeemeerguh: look where you want to go, jump = up, sneak = off.",
                                   "Blub! Op een Zeemeerguh: kijk waar je heen wilt, springen = omhoog, sluipen = afstappen."),
    "quest.guhs.onderwater.air": ("Blub... fresh air from the cheese coral!", "Blub... verse lucht uit het kaaskoraal!"),
    # signs
    "sign.guhs.onderwater.bord1": ("DUIKPOST", "DUIKPOST"), "sign.guhs.onderwater.bord2": ("to the Guh Bubble", "naar de Guhbubbel"),
    "sign.guhs.onderwater.bord3": ("in through the mouth!", "door de mond naar binnen!"),
    "sign.guhs.onderwater.post1": ("Spiral stairs", "Wenteltrap"), "sign.guhs.onderwater.post2": ("down to the", "naar de"),
    "sign.guhs.onderwater.post3": ("sea floor", "zeebodem"),
    "sign.guhs.onderwater.lift1": ("Bubble lift", "Bubbellift"), "sign.guhs.onderwater.lift2": ("up to the", "omhoog naar de"),
    "sign.guhs.onderwater.lift3": ("Duikpost", "Duikpost"), "sign.guhs.onderwater.lift4": ("(arrives here)", "(komt hier uit)"),
    "sign.guhs.onderwater.tunnel1": ("Guh Bubble", "Guhbubbel"), "sign.guhs.onderwater.tunnel2": ("this way", "deze kant op"),
    "sign.guhs.onderwater.tunnel3": ("mind the fish", "let op de vissen"),
    "sign.guhs.onderwater.winkel1": ("Zeemeerguh", "Zeemeerguh"), "sign.guhs.onderwater.winkel2": ("diving shop", "duikwinkeltje"),
    "sign.guhs.onderwater.winkel3": ("pay in pearls", "betalen met parels"),
    "sign.guhs.onderwater.winkel4": ("Pearls are in", "Parels zitten in"), "sign.guhs.onderwater.winkel5": ("the giant shells", "de reuzenschelpen"),
    "sign.guhs.onderwater.winkel6": ("(right-click)", "(rechtsklik)"),
    "sign.guhs.onderwater.deur1": ("Dive door", "Duikdeur"), "sign.guhs.onderwater.deur2": ("cheese coral", "kaaskoraal"),
    "sign.guhs.onderwater.deur3": ("gives air!", "geeft lucht!"),
    # advancements
    "advancements.guhs.guhmension.find_onderwater.title": ("Blub Blub Guh", "Blub blub guh"),
    "advancements.guhs.guhmension.find_onderwater.description": ("Find the Guh Bubble on the bottom of a Deep Guh Sea",
                                                                 "Vind de Guhbubbel op de bodem van een Diepe Guhzee"),
    "advancements.guhs.guhmension.onderwater_duikhelm.title": ("Deep-Sea Guh", "Diepzeeguh"),
    "advancements.guhs.guhmension.onderwater_duikhelm.description": ("Buy a diving helmet from the Zeemeerguh", "Koop een duikhelm bij de Zeemeerguh"),
    "advancements.guhs.guhmension.onderwater_zeemeer.title": ("Mer-Guh Rider", "Zeemeerguhruiter"),
    "advancements.guhs.guhmension.onderwater_zeemeer.description": ("Tame a wild Zeemeerguh", "Tem een wilde Zeemeerguh"),
}


def build(h):
    h.ms = __import__("make_structures")
    A, D_ = h.A, h.D

    # --- blocks ---
    h.save(kaaskoraal_texture(), "block", "kaaskoraal.png")
    h.save(kaaskoraalblok_texture(h), "block", "kaaskoraalblok.png")
    h.save(nacre(16, 40), "block", "parelmoer.png")
    h.save(tiles_texture(), "block", "parelmoer_tegels.png")
    shell_textures(h)
    for name in ("kaaskoraalblok", "parelmoer", "parelmoer_tegels"):
        h.simple_block(name)
        h.self_drop(name)
    h.w(f"{A}/models/block/kaaskoraal.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                "textures": {"cross": "guhs:block/kaaskoraal"}})
    h.w(f"{A}/blockstates/kaaskoraal.json", {"variants": {"": {"model": "guhs:block/kaaskoraal"}}})
    h.item_model("kaaskoraal", "guhs:block/kaaskoraal")
    h.self_drop("kaaskoraal")
    shell_models(h)
    h.self_drop("reuzenschelp")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:parelmoer", "guhs:parelmoer_tegels", "guhs:kaaskoraalblok", "guhs:reuzenschelp"])
    h.shaped("parelmoer_tegels", ["PP", "PP"], {"P": "guhs:parelmoer"}, "guhs:parelmoer_tegels", 4)
    h.shaped("parelmoer_van_parels", ["PP", "PP"], {"P": "guhs:parel"}, "guhs:parelmoer", 4)
    h.shaped("kaaskoraalblok", ["KK", "KK"], {"K": "guhs:kaaskoraal"}, "guhs:kaaskoraalblok")

    # --- items ---
    item_icons(h)
    h.item_model("parel")
    h.item_model("duikhelm")
    armor_texture(h)

    # --- the Zeemeerguh: an aqua sitting guh ---
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    npc_tex = h.recolour(src, hue=0.48, sat=1.5, val=0.97, only=h.pinkish)
    h.save(npc_tail(h, npc_tex), "entity", "npc_zeemeerguh.png")

    # --- the wreck's treasure ---
    h.w(f"{D_}/loot_table/chests/onderwater_wrak.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:parel", "functions": h.count_fn(4, 8)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            {"type": "minecraft:item", "name": "minecraft:gold_ingot", "weight": 5, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:prismarine_crystals", "weight": 4, "functions": h.count_fn(3, 9)},
            {"type": "minecraft:item", "name": "minecraft:nautilus_shell", "weight": 3, "functions": h.count_fn(1, 2)},
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": h.count_fn(8, 20)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 3, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "guhs:guh_vis", "weight": 3, "functions": h.count_fn(2, 5)},
            {"type": "minecraft:item", "name": "guhs:kaaskoraal", "weight": 3, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:emerald", "weight": 3, "functions": h.count_fn(2, 5)},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 1, "functions": h.count_fn(1, 2)},
            {"type": "minecraft:item", "name": "minecraft:heart_of_the_sea", "weight": 1},
            {"type": "minecraft:item", "name": "minecraft:saddle", "weight": 2}]}]})

    structure_json(h)
    if build_structure(h).problems:
        raise SystemExit("onderwater: fix the template (see the geometry check above)")

    # --- advancements: shown ones in the Guhmension tab, hidden ones for the quests ---
    for name, parent, icon, frame, crit in [
        ("find_onderwater", "enter_guhmension", "guhs:parel", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:onderwater"]}}}}),
        ("onderwater_duikhelm", "find_onderwater", "guhs:duikhelm", "goal",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:duikhelm"}]}}),
        ("onderwater_zeemeer", "find_onderwater", "guhs:guh_spawn_egg", "challenge",
         {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                          "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"zeemeerguh\"}"}}]}}),
    ]:
        h.w(f"{D_}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
    for name in ("onderwater_lucht", "onderwater_rit"):
        h.w(f"{D_}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    h.w(f"{D_}/advancement/quest/onderwater_wrak.json", {"criteria": {"done": {
        "trigger": "minecraft:player_generates_container_loot", "conditions": {"loot_table": "guhs:chests/onderwater_wrak"}}}})

    for key, (en, nl) in LANG.items():
        h.lang(key, en, nl)
    # the Zeemeerguh's fish tail swishes (GeckoLib animation, played by GuhEntity's "zeemeer" controller)
    anim_path = os.path.join(A, "geckolib", "animations", "entity", "guh.animation.json")
    anim = json.load(open(anim_path, encoding="utf-8"))
    anim["animations"]["animation.guh.zeemeer_swim"] = {"loop": True, "animation_length": 1.2, "bones": {
        "zeemeer_staart": {"rotation": {"0.0": [0, 14, 0], "0.3": [6, 0, 0], "0.6": [0, -14, 0], "0.9": [-6, 0, 0], "1.2": [0, 14, 0]}},
        "zeemeer_staartvin": {"rotation": {"0.0": [0, 10, 0], "0.3": [12, 0, 0], "0.6": [0, -10, 0], "0.9": [-12, 0, 0], "1.2": [0, 10, 0]}},
        "zeemeer_rugvin": {"rotation": {"0.0": [0, 0, -4], "0.6": [0, 0, 4], "1.2": [0, 0, -4]}}}}
    with open(anim_path, "w", encoding="utf-8") as f:
        json.dump(anim, f, indent=2)
    selfcheck_assets(h)


# the Zeemeerguh NPC: the sitting guh with a fish tail instead of its feet (own model: no other NPC gets the tail)
NPC_SCALES_UV, NPC_FIN_UV = (104, 104), (112, 104)      # free 8x8 spots on the guh_sitting texture (same spots as on guh.png)


def npc_tail(h, img):
    """Writes geo/entity/guh_npc_zeemeerguh.geo.json (guh_sitting minus feet, haunches and guh tail, plus the zeemeer
    hip, tail and tail fin of the Zeemeerguh variant, curled forward on the ground) and paints the scales and the fin on
    the NPC texture."""
    v = __import__("make_guh_variants")
    rng = np.random.default_rng(31)
    a = np.asarray(img.convert("RGBA")).copy()
    for (u, w_), paint in ((NPC_SCALES_UV, lambda: _scales(v, rng)), (NPC_FIN_UV, lambda: _fin(v, rng))):
        px = v.SWATCH * 4
        a[w_ * 4:w_ * 4 + px, u * 4:u * 4 + px, :3] = paint().astype(np.uint8)
        a[w_ * 4:w_ * 4 + px, u * 4:u * 4 + px, 3] = 255
    geo_path = os.path.join(h.A, "geckolib", "models", "entity", "guh_sitting.geo.json")
    geo = json.load(open(geo_path, encoding="utf-8"))
    model = geo["minecraft:geometry"][0]
    model["description"]["identifier"] = "geometry.guh_npc_zeemeerguh"
    model["description"]["visible_bounds_width"] = 4

    def cube(origin, size, uv, inflate=0.0):
        c = {"origin": origin, "size": size, "uv": {f: {"uv": list(uv), "uv_size": [8, 8]} for f in ("north", "south", "east", "west", "up", "down")}}
        if inflate:
            c["inflate"] = inflate
        return c

    bones = []
    for bone in model["bones"]:
        if bone["name"] in ("foot_left", "foot_right", "tail"):
            continue                                   # no paws and no guh tail: the fish tail takes their place
        if bone["name"] == "body":                     # (the two haunch cubes on the sides go too)
            bone["cubes"] = [c for c in bone["cubes"] if not (c["size"] == [2, 5, 6] and c["origin"][1] == 1)]
        bones.append(bone)
    bones += [
        {"name": "zeemeer_heup", "parent": "body", "pivot": [0, 2, 0], "cubes": [cube([-5.6, 0, -4.6], [11.2, 4.6, 9.2], NPC_SCALES_UV, 0.2)]},
        {"name": "zeemeer_staart", "parent": "body", "pivot": [0, 1, -4], "rotation": [0, -32, 0], "cubes": [
            cube([-4, 0, -9.6], [8, 3.8, 5.6], NPC_SCALES_UV, 0.1), cube([-3, 0.2, -13.6], [6, 3.2, 4.2], NPC_SCALES_UV),
            cube([-2, 0.4, -16.6], [4, 2.6, 3], NPC_SCALES_UV)]},
        {"name": "zeemeer_staartvin", "parent": "zeemeer_staart", "pivot": [0, 1.6, -16.6], "rotation": [-18, 0, 0], "cubes": [
            cube([-1.5, 0.9, -18], [3, 1.6, 1.4], NPC_FIN_UV), cube([-8, 1.2, -22.6], [7, 0.8, 4.6], NPC_FIN_UV),
            cube([1, 1.2, -22.6], [7, 0.8, 4.6], NPC_FIN_UV)]},
        {"name": "zeemeer_rugvin", "parent": "body", "pivot": [0, 8, 4], "cubes": [
            cube([-0.5, 5, 3.4], [1, 5, 2.4], NPC_FIN_UV), cube([-0.5, 7, 5.8], [1, 2.4, 1.2], NPC_FIN_UV)]},
    ]
    model["bones"] = bones
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "guh_npc_zeemeerguh.geo.json"), geo)
    return Image.fromarray(a)


def structure_json(h):
    """The structure: one in the deep middle of every Diepe Guhzee (guhs:guhbubbel, GuhbubbelStructure.java: the peak of
    the sea noise, the highest one around, with deep water all over the template). Its cells are as big as the
    random_spread spacing, so every cell has one start chunk. No monsters (and no land animals) inside."""
    from features import diepzee
    D_ = h.D
    none = {"bounding_box": "full", "spawns": []}
    h.w(f"{D_}/worldgen/structure/onderwater.json", {
        "type": "guhs:guhbubbel", "biomes": "#guhs:has_structure/onderwater", "step": "surface_structures",
        "spawn_overrides": {"creature": none, "monster": none, "ambient": none, "underground_water_creature": none, "axolotls": none},
        # no beard: the template brings its own flat sea floor, and the rest of the sea is already there
        "terrain_adaptation": "none",
        "start_pool": "guhs:onderwater/start", "start_jigsaw_name": "guhs:onderwater_midden",
        "sea_noise": f"guhs:{diepzee.SEA_NOISE}", "cell_chunks": diepzee.CELL_CHUNKS, "neighbourhood": diepzee.NEIGHBOURHOOD,
        "min_value": diepzee.PEAK_MIN, "sea_value": diepzee.WATER_FROM, "water_level": diepzee.WATER_LEVEL,
        "centre_depth": diepzee.BUBBLE_CENTRE_DEPTH, "min_depth": diepzee.BUBBLE_MIN_DEPTH, "check_radius": int(SEA_R)})
    h.w(f"{D_}/worldgen/template_pool/onderwater/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "guhs:onderwater",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    h.w(f"{D_}/worldgen/structure_set/onderwater.json", {
        "structures": [{"structure": "guhs:onderwater", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": diepzee.CELL_CHUNKS, "separation": 1, "salt": SALT}})
    h.w(f"{D_}/tags/worldgen/biome/has_structure/onderwater.json", {"values": [f"guhs:{SEA_BIOME}"]})


def shell_models(h):
    """The giant shell: a ribbed pink clam; open (top valve tilted up, a pearl inside) or closed."""
    A = h.A
    tex = {"out": "guhs:block/reuzenschelp_buiten", "in": "guhs:block/reuzenschelp_binnen", "parel": "guhs:block/reuzenschelp_parel",
           "particle": "guhs:block/reuzenschelp_buiten"}

    def el(frm, to, texture, top_tex=None, rot=None):
        faces = {d: {"texture": texture} for d in ("north", "south", "east", "west", "down")}
        faces["up"] = {"texture": top_tex or texture}
        e = {"from": frm, "to": to, "faces": faces}
        if rot:
            e["rotation"] = rot
        return e

    bottom = [el([1, 0, 1], [15, 2, 15], "#out", "#in"), el([0, 1, 2], [16, 3, 14], "#out", "#in"), el([6, 0, 14], [10, 4, 16], "#out")]
    closed_top = [el([1, 3, 1], [15, 5, 15], "#out"), el([2, 5, 2], [14, 6, 14], "#out")]
    hinge = {"origin": [8, 3, 15], "axis": "x", "angle": 45}
    open_top = [el([1, 3, 1], [15, 5, 15], "#out", rot=hinge), el([2, 5, 2], [14, 6, 14], "#out", rot=hinge)]
    pearl = [el([6, 2, 6], [10, 6, 10], "#parel")]
    h.w(f"{A}/models/block/reuzenschelp_open.json", {"parent": "minecraft:block/block", "textures": tex, "elements": bottom + open_top + pearl})
    h.w(f"{A}/models/block/reuzenschelp_leeg.json", {"parent": "minecraft:block/block", "textures": tex, "elements": bottom + closed_top})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    h.w(f"{A}/blockstates/reuzenschelp.json", {"variants": {
        f"facing={f},parel={p}": {"model": f"guhs:block/reuzenschelp_{'open' if p == 'true' else 'leeg'}", **({"y": r} if r else {})}
        for f, r in rot.items() for p in ("true", "false")}})
    h.w(f"{A}/models/item/reuzenschelp.json", {"parent": "guhs:block/reuzenschelp_open"})


def selfcheck_assets(h):
    """check_assets.py only knows the registry classes: this checks our own blocks and items."""
    A = h.A
    missing = []
    for bl in ("kaaskoraal", "kaaskoraalblok", "parelmoer", "parelmoer_tegels", "reuzenschelp"):
        for p in (f"{A}/blockstates/{bl}.json", f"{A}/models/item/{bl}.json"):
            if not os.path.exists(p):
                missing.append(p)
        if f"block.guhs.{bl}" not in h.NL:
            missing.append(f"lang block.guhs.{bl}")
    for i in ("parel", "duikhelm"):
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(f"{A}/textures/item/{i}.png"):
            missing.append(f"item {i}")
    if not os.path.exists(f"{A}/textures/entity/equipment/humanoid/duikhelm.png"):
        missing.append("armor texture")
    if missing:
        raise SystemExit(f"onderwater assets missing: {missing}")


# =====================================================================================================================
# the Zeemeerguh (guh variant) and the duikpakje (guh clothes)
# =====================================================================================================================
def _scales(v, rng, base=(64, 178, 170), light=(150, 232, 214), dark=(34, 120, 124)):
    px = v.SWATCH * 4
    a = v.fabric(base, rng, 8)
    for y in range(px):
        for x in range(px):
            cx = (x + (4 if (y // 4) % 2 else 0)) % 8
            d = math.hypot(cx - 4, (y % 4) - 0.5)
            if 3.2 < d < 4.3:
                a[y, x] = dark
            elif d < 1.6:
                a[y, x] = light
    return np.clip(a, 0, 255)


def _fin(v, rng):
    px = v.SWATCH * 4
    a = np.zeros((px, px, 3), np.float32)
    for y in range(px):
        for x in range(px):
            t = x / px
            a[y, x] = (240 - 60 * t, 150 + 40 * t, 210 + 20 * t)
            if x % 5 == 0:
                a[y, x] *= 0.82
    return np.clip(a + rng.normal(0, 4, a.shape), 0, 255)


def variants(rng, v):
    return {"zeemeerguh": ((236, 170, 214), {"zeemeer_schubben": lambda: _scales(v, rng), "zeemeer_vin": lambda: _fin(v, rng)})}


def _goggles(v, rng):
    px = v.SWATCH * 4
    a = np.zeros((px, px, 4), np.float32)
    for y in range(px):
        for x in range(px):
            edge = min(x, y, px - 1 - x, px - 1 - y)
            if edge < 5:
                a[y, x] = (250, 196, 40, 255)                      # the yellow rubber frame
            else:
                a[y, x] = (150, 222, 246, 255)                     # the lens
                if 8 <= x + y - 10 <= 11 and x < 20:
                    a[y, x] = (240, 252, 255, 255)                 # a glint
    return np.clip(a, 0, 255)


def clothes(rng, v):
    return {
        "duikbril": {"duikbril": lambda: _goggles(v, rng), "duikbril_band": lambda: v.fabric((34, 34, 40), rng, 6)},
        "snorkel": {"snorkel": lambda: v.band((250, 120, 30), (250, 250, 250), rng, (4, 14, 24)),
                    "snorkel_tip": lambda: v.fabric((250, 110, 20), rng, 6)},
        "zwemband": {"zwemband": lambda: np.ascontiguousarray(v.stripes((246, 110, 160), (252, 250, 250), rng, 4).transpose(1, 0, 2))},
    }


def icons(ic):
    snorkel = ic.icon(["................", "..........aa....", "..........ab....", "..........aa....", "..........ab....",
                       "..........aa....", "..........ab....", "..........aa....", "..........ab....", "..........aa....",
                       "..........ab....", "...cccccccaa....", "...caaaaaaaa....", "...cccc.........", "................",
                       "................"], {"a": (250, 120, 30), "b": (250, 250, 250), "c": (40, 40, 46)})
    ring = ic.icon(["................", ".....aaaaaa.....", "...aabbbbbbaa...", "..abbaaaaaabba..", ".abba......abba.",
                    ".aba........aba.", "abba........abba", "aaaa........aaaa", "abba........abba", ".aba........aba.",
                    ".abba......abba.", "..abbaaaaaabba..", "...aabbbbbbaa...", ".....aaaaaa.....", "................",
                    "................"], {"a": (246, 110, 160), "b": (252, 250, 250)})
    return {
        "duikbril": ic.shaped("glasses", (250, 196, 40), (150, 222, 246)),
        "snorkel": snorkel,
        "zwemband": ring,
    }


# =====================================================================================================================
# FTB quests (row y = 52)
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("onderwater_find", "Blub blub guh", "In het midden van elke &9Diepe Guhzee&r ligt op de zeebodem een reusachtige glazen bubbel in de vorm van een guhhoofd: de &bGuhbubbel&r! Zoek het eilandje met de Duikpost (een guhhoofd met een duikbril), loop hem binnen door zijn mond en neem de wenteltrap naar beneden. Het superkompas (Wonderen) wijst de weg.",
      "guhs:parel", [fq.structure("onderwater")], rewards=(("guhs:kaas_knabbels", 16),), x=-8, y=52, shape="hexagon", xp=100)
    q("onderwater_parel", "Parels uit de schelp", "Rechtsklik een open &dreuzenschelp&r: soms zit er een parel in! Een dichte schelp gaat na een tijdje vanzelf weer open. Parels zijn het geld van de Zeemeerguh.",
      "guhs:parel", [fq.item("guhs:parel", 5)], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-6, y=52)
    q("onderwater_lucht", "Belletjes happen", "Zwem door een duikdeur naar buiten. Bijna geen lucht meer? Zwem naar het gele &6kaaskoraal&r: dat blaast luchtbelletjes en je kunt weer ademen. VAHOEG!",
      "guhs:kaaskoraal", [fq.adv("onderwater_lucht")], x=-4, y=52)
    q("onderwater_wrak", "Het gezonken guhschip", "Vlak ten noorden van de bubbel ligt een gezonken guhschip met een guhhoofd op de boeg. In de kajuit achterop staat een schatkist... Njeg, spannend!",
      "minecraft:chest", [fq.adv("onderwater_wrak")], rewards=(("guhs:parel", 3),), x=-2, y=52, xp=150)
    q("onderwater_duikhelm", "Diepzeeguh", "Koop bij de &bZeemeerguh&r een duikhelm: daarmee adem je onder water zo lang als je wilt.",
      "guhs:duikhelm", [fq.item("guhs:duikhelm")], x=0, y=52)
    q("onderwater_temmen", "Een vis met pootjes", "In de guhzee en de Diepe Guhzee zwemt heel soms een wilde &dZeemeerguh&r: een guh met een vissenstaart! Tem hem met kaasknabbels.",
      "guhs:guh_spawn_egg", [fq.adv("tamed_zeemeerguh")], rewards=(("minecraft:saddle", 1),), x=2, y=52, xp=150)
    q("onderwater_rit", "Onderwaterrit", "Zadel je Zeemeerguh en rijd onder water: kijk waar je heen wilt, springen is omhoog. Hij zwemt supersnel en jij krijgt lucht van hem. Blub!",
      "minecraft:saddle", [fq.adv("onderwater_rit")], x=4, y=52, shape="rsquare")
    q("onderwater_pakje", "Klaar om te duiken", "Koop de duikbril, de snorkel en de zwemband bij de Zeemeerguh (alleen zij verkoopt ze) en trek ze je guh aan. VAHOEG, wat een waterguh!",
      "guhs:duikbril", [fq.item("guhs:duikbril"), fq.item("guhs:snorkel"), fq.item("guhs:zwemband")],
      rewards=(("guhs:parel", 5),), x=6, y=52, shape="gear", xp=150)
