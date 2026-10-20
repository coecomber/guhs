"""
Zone module "entree": the entreedorp (park lobby), after the approved render v2 (safari-ontwerp/_bron/
render_entreedorp_v2.py, user verdict "Perfect"; design 1.5).

The village is built in its own local scene (136 x 52 x 118, ground at local y 8 = world y 64) with the same code as the
render, then copied into the park inside its organic outline (INS) plus the bus forecourt in front of the park gate.
Only blocks: no NPCs, no animals, no block-built trucks (the truck slice brings real ones).

What changes around it (world):
  * the village outline becomes zone "entree" and inside the park (it pokes out over the old park edge to the south;
    the gate opens onto a fenced bus forecourt);
  * the old terrain, fences and the south part of the entree lake inside the outline are replaced;
  * a ring of 9 blocks around the outline is blended to the village height (the 3 nearest flat, the escape rule),
    away from existing fences; trees whose trunk was removed lose their crown too;
  * the park route crosses the village fence in two places: there the fence has a gap and a path leads to the
    village paths (the north one goes around Het Watergat, the north-east one to the truck station);
  * park.ingang / park.plein move to the square, the four aaihoek homes go into their pens (plekken hook).
Differences with the render: no water arc over the fountain (free water would flow; a spray can come as particles),
the creek stops inside the fence, the trucks are not built.
"""
import math
import random

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit

# ----------------------------------------------------------------------------------------------- placement (world)
OX, OZ = 137, 545            # local (0, 0) -> world
OY = 56                      # local y 0 -> world 56, so the village ground (local 8) is world 64
X, Y, Z = 136, 52, 118
G = 8
CX, CZ = 68, 60              # middle of the square
SQ_R = 15

WATER = "minecraft:water[level=0]"
BED = "minecraft:sand"
AIR = "minecraft:air"
HAY = "minecraft:hay_block[axis=y]"
LOG = "minecraft:stripped_spruce_log[axis=y]"
POST = "minecraft:dark_oak_log[axis=y]"
AL = "minecraft:acacia_leaves[distance=1,persistent=true,waterlogged=false]"
JL = "minecraft:jungle_leaves[distance=1,persistent=true,waterlogged=false]"
OL = "minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]"


# ----------------------------------------------------------------------------------------------- noise (as the render)
def _sstep(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def _h(ix, iy, seed):
    n = (ix * 374761393 + iy * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def vnoise(x, y, seed=0):
    ix, iy = math.floor(x), math.floor(y)
    fx, fy = _sstep(x - ix), _sstep(y - iy)
    a, b = _h(ix, iy, seed), _h(ix + 1, iy, seed)
    c, d = _h(ix, iy + 1, seed), _h(ix + 1, iy + 1, seed)
    return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy


def stairs(block, facing, half="bottom", shape="straight"):
    return f"{block}[facing={facing},half={half},shape={shape},waterlogged=false]"


def slab(block, typ="bottom"):
    return f"{block}[type={typ},waterlogged=false]"


# ----------------------------------------------------------------------------------------------- local scene
_NIET_VAST = ("slab", "stairs", "carpet", "leaves", "fence", "gate", "pane", "bars", "door", "sign", "grass", "fern",
              "bush", "vine", "lily", "cane", "lantaarn", "lantern", "bloempot", "guhoortjes", "guhbloem", "water",
              "air", "dandelion", "poppy", "bluet", "snow", "composter", "cauldron", "palm_blad")


class Lokaal:
    """A small block volume with the vibekit Scene API the render code uses."""

    def __init__(self):
        self.pal = [AIR]
        self.idx = {AIR: 0}
        self.v = np.zeros((X, Y, Z), np.uint16)

    def id(self, s):
        if ":" not in s.split("[")[0]:
            s = "guhs:" + s
        i = self.idx.get(s)
        if i is None:
            i = self.idx[s] = len(self.pal)
            self.pal.append(s)
        return i

    def inb(self, x, y, z):
        return 0 <= x < X and 0 <= y < Y and 0 <= z < Z

    def set(self, x, y, z, s, roof=False, keep=False):
        x, y, z = int(x), int(y), int(z)
        if not self.inb(x, y, z) or (keep and self.v[x, y, z] != 0):
            return
        self.v[x, y, z] = self.id(s)

    def get(self, x, y, z):
        return self.pal[self.v[x, y, z]] if self.inb(x, y, z) else AIR

    def is_air(self, x, y, z):
        return self.get(x, y, z) == AIR

    def fill(self, x1, y1, z1, x2, y2, z2, s, roof=False, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, s, roof, keep)

    def walls(self, x1, y1, z1, x2, y2, z2, s, roof=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                if x in (x1, x2) or z in (z1, z2):
                    for y in range(min(y1, y2), max(y1, y2) + 1):
                        self.set(x, y, z, s, roof)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, AIR)

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, s, roof=False, keep=False):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1:
                        self.set(x, y, z, s, roof, keep)

    def cylinder(self, cx, cz, r, y1, y2, s, roof=False, keep=False):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                if math.hypot(x - cx, z - cz) <= r:
                    for y in range(y1, y2 + 1):
                        self.set(x, y, z, s, roof, keep)

    def top_y(self, x, z):
        nz = np.nonzero(self.v[x, :, z])[0]
        return int(nz.max()) if len(nz) else -1

    def fix_connections(self):
        """Glass panes, iron bars and fences connect to their family and to solid blocks (like the game)."""
        def fam(n):
            n = n.split("[")[0]
            if n.endswith("_fence"):
                return "fence"
            if n.endswith(("glass_pane", "iron_bars")):
                return "pane"
            return None

        def vast(n):
            b = n.split("[")[0]
            return b != AIR and not any(k in b for k in _NIET_VAST)

        nieuw = []
        for (x, y, z) in np.argwhere(self.v > 0):
            s = self.pal[self.v[x, y, z]]
            f = fam(s)
            if not f:
                continue
            con = {}
            for d, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("west", (-1, 0)), ("east", (1, 0))):
                o = self.get(x + dx, y, z + dz)
                con[d] = vast(o) or fam(o) == f or (f == "fence" and o.split("[")[0].endswith("_gate"))
            props = ",".join(f"{d}={'true' if con[d] else 'false'}" for d in ("east", "north", "south", "west"))
            nieuw.append((x, y, z, f"{s.split('[')[0]}[{props},waterlogged=false]"))
        for (x, y, z, st) in nieuw:
            self.v[x, y, z] = self.id(st)


# ----------------------------------------------------------------------------------------------- outline
def rad(ang):
    return 1.0 + 0.10 * math.sin(3 * ang + 0.7) + 0.06 * math.sin(5 * ang + 2.1) + (vnoise(ang * 2.2, 0.5, 21) - 0.5) * 0.12


def inside(x, z):
    dx, dz = (x - CX) / 60.0, (z - CZ) / 49.0
    return math.hypot(dx, dz) < rad(math.atan2(dz, dx))


INS = {(x, z) for x in range(X) for z in range(Z) if inside(x, z)}
# the fence ring: every inside cell with an outside cell among its 8 neighbours. That ring is 4-connected (a ring of
# only 4-neighbour edge cells steps diagonally, so fences and bars stood as loose posts with gaps between them).
EDGE = [(x, z) for (x, z) in INS
        if any((x + a, z + b) not in INS for a in (-1, 0, 1) for b in (-1, 0, 1) if a or b)]
GZ = max(z for (x, z) in INS if x == CX)          # the park gate (south)


# ----------------------------------------------------------------------------------------------- the village
# the aaihoek pens (x0, z0, animal): 6 x 7, gate on the side facing the square paths
PENS = [(94, 52, "geitje"), (120, 50, "alpaca"), (90, 72, "cavia"), (106, 74, "ezeltje")]


def _dorp(kruising_cellen):
    """Builds the village in a local scene. kruising_cellen: edge cells (local) where the park route crosses the
    outline. Returns (S, info)."""
    rnd = random.Random(11)
    S = Lokaal()

    # ---- helpers (from the render kit) ----
    def acacia(x, z, y=None):
        y = S.top_y(x, z) if y is None else y
        h = rnd.randint(4, 6)
        S.fill(x, y + 1, z, x, y + h, z, "minecraft:acacia_log[axis=y]")
        dx, dz = rnd.choice([(1, 0), (-1, 0), (0, 1), (0, -1)])
        S.set(x + dx, y + h, z + dz, "minecraft:acacia_log[axis=y]")
        cx, cz = x + dx, z + dz
        for a in range(-3, 4):
            for b in range(-3, 4):
                if a * a + b * b <= 10:
                    S.set(cx + a, y + h + 1, cz + b, AL, keep=True)
                if a * a + b * b <= 3:
                    S.set(cx + a, y + h + 2, cz + b, AL, keep=True)

    def palm(x, z, y=None):
        y = S.top_y(x, z) if y is None else y
        h = rnd.randint(5, 7)
        for i in range(1, h + 1):
            S.set(x + (1 if i > h - 2 else 0), y + i, z, "guhwaii_palm_stam[axis=y]")
        tx, t = x + 1, y + h
        S.set(tx, t + 1, z, "guhwaii_palm_blad")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            S.set(tx + dx, t + 1, z + dz, "guhwaii_palm_blad", keep=True)
            S.set(tx + 2 * dx, t, z + 2 * dz, "guhwaii_palm_blad", keep=True)
            S.set(tx + 3 * dx, t - 1, z + 3 * dz, "guhwaii_palm_blad", keep=True)
        for dx, dz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            S.set(tx + dx, t, z + dz, "guhwaii_palm_blad", keep=True)
            S.set(tx + 2 * dx, t - 1, z + 2 * dz, "guhwaii_palm_blad", keep=True)

    def thatch(x1, z1, x2, z2, y, overhang=1):
        a, b, c, d = x1 - overhang, x2 + overhang, z1 - overhang, z2 + overhang
        lvl = 0
        while a <= b and c <= d:
            for x in range(a, b + 1):
                for z in range(c, d + 1):
                    if x in (a, b) or z in (c, d) or a + 1 >= b or c + 1 >= d:
                        S.set(x, y + lvl, z, HAY)
            a, b, c, d = a + 1, b - 1, c + 1, d - 1
            lvl += 1
        return y + lvl

    def thatch_round(cx, cz, r, y):
        lvl = 0
        rr = r + 1.2
        while rr > 0.4:
            for x in range(int(cx - rr) - 1, int(cx + rr) + 2):
                for z in range(int(cz - rr) - 1, int(cz + rr) + 2):
                    d = math.hypot(x - cx, z - cz)
                    if d <= rr and (d > rr - 1.6 or rr < 1.6):
                        S.set(x, y + lvl, z, HAY)
            rr -= 0.7
            lvl += 1
        return y + lvl

    def box(x1, y1, z1, x2, y2, z2, wall, floor=None, corner=LOG):
        S.walls(x1, y1, z1, x2, y2, z2, wall)
        for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
            S.fill(x, y1, z, x, y2, z, corner)
        if floor:
            S.fill(x1, y1 - 1, z1, x2, y1 - 1, z2, floor)
        S.air(x1 + 1, y1, z1 + 1, x2 - 1, y2, z2 - 1)

    def windows(x1, y, z1, x2, z2, every=3, pane="minecraft:glass_pane"):
        for x in range(x1 + 2, x2 - 1, every):
            for z in (z1, z2):
                S.set(x, y, z, pane)
        for z in range(z1 + 2, z2 - 1, every):
            for x in (x1, x2):
                S.set(x, y, z, pane)

    def door(x, y, z, w=1):
        for i in range(w):
            S.air(x + i, y, z, x + i, y + 1, z)

    def olifant(x, z, y, facing=1, col="minecraft:light_gray_concrete"):
        f = facing
        for (a, b) in ((0, 0), (2, 0), (0, -3 * f), (2, -3 * f)):
            S.fill(x + a, y + 1, z + b, x + a, y + 2, z + b, col)
        S.ellipsoid(x + 1, y + 4, z - 1.5 * f, 2.2, 1.8, 2.8, col)
        S.ellipsoid(x + 1, y + 5.5, z + 1.8 * f, 1.9, 1.9, 1.6, col)
        for side in (-1, 1):
            S.fill(x + 1 + side * 3, y + 4, z + 1 * f, x + 1 + side * 3, y + 6, z + 2 * f, "minecraft:pink_wool")
        S.set(x, y + 6, z + 3 * f, "minecraft:black_concrete")
        S.set(x + 2, y + 6, z + 3 * f, "minecraft:black_concrete")
        S.set(x - 1, y + 5, z + 3 * f, "minecraft:pink_wool")
        S.set(x + 3, y + 5, z + 3 * f, "minecraft:pink_wool")
        S.fill(x + 1, y + 2, z + 4 * f, x + 1, y + 5, z + 4 * f, col)
        S.set(x + 1, y + 2, z + 5 * f, col)
        S.set(x, y + 4, z + 4 * f, "minecraft:white_concrete")
        S.set(x + 2, y + 4, z + 4 * f, "minecraft:white_concrete")

    # ---- ground ----
    HGT = {}
    for x in range(X):
        for z in range(Z):
            HGT[(x, z)] = G
            S.fill(x, 0, z, x, G - 3, z, "minecraft:stone")
            S.fill(x, G - 2, z, x, G - 1, z, "minecraft:dirt")
            n = vnoise(x / 6, z / 6, 4)
            S.set(x, G, z, "minecraft:grass_block[snowy=false]" if n > 0.14 else "minecraft:coarse_dirt")

    # ---- pond SW + creek (stops 2 inside the fence) ----
    POND = set()
    for x in range(0, 60):
        for z in range(66, Z):
            d = math.hypot((x - 34) / 17.0, (z - 90) / 12.5) + (vnoise(x / 4, z / 4, 7) - 0.5) * 0.22
            if d < 1.0:
                POND.add((x, z))
    for x in range(0, 22):
        zc = 92 + 4 * math.sin(x / 5.0)
        for z in range(int(zc) - 2, int(zc) + 3):
            POND.add((x, z))
    ins_arr = np.zeros((X, Z), bool)
    for (x, z) in INS:
        ins_arr[x, z] = True
    rand_d = distance_transform_edt(ins_arr)            # distance to the outline, inside
    POND = {(x, z) for (x, z) in POND if (x, z) in INS and rand_d[x, z] > 3}
    SAND = set()
    for (x, z) in list(POND):
        for a in (-1, 0, 1):
            for b in (-1, 0, 1):
                if (x + a, z + b) not in POND:
                    SAND.add((x + a, z + b))
    for (x, z) in POND:
        S.set(x, G - 2, z, BED)
        S.fill(x, G - 1, z, x, G, z, WATER)
    for (x, z) in SAND:
        if 0 <= x < X and 0 <= z < Z:
            S.set(x, G, z, "minecraft:sand")

    # ---- winding paths ----
    PATHB = ["minecraft:smooth_sandstone", "minecraft:sandstone", "minecraft:dirt_path", "minecraft:smooth_sandstone",
             "minecraft:dirt_path"]
    PATHS = set()

    def catmull(pts, n=12):
        out = []
        for i in range(len(pts) - 1):
            p0 = pts[max(i - 1, 0)]; p1 = pts[i]; p2 = pts[i + 1]; p3 = pts[min(i + 2, len(pts) - 1)]
            for k in range(n):
                t = k / n; t2, t3 = t * t, t * t * t
                out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2
                                        + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in (0, 1)))
        out.append(pts[-1])
        return out

    def path(pts, w=2.2, mats=PATHB):
        for (px, pz) in catmull(pts):
            r = w + (vnoise(px / 5, pz / 5, 9) - 0.5) * 0.8
            for x in range(int(px - r) - 1, int(px + r) + 2):
                for z in range(int(pz - r) - 1, int(pz + r) + 2):
                    if 0 <= x < X and 0 <= z < Z and math.hypot(x - px, z - pz) <= r and (x, z) not in POND:
                        S.set(x, HGT[(x, z)], z, rnd.choice(mats))
                        S.air(x, HGT[(x, z)] + 1, z, x, HGT[(x, z)] + 4, z)
                        PATHS.add((x, z))

    path([(CX, CZ + SQ_R), (CX - 3, CZ + 26), (CX + 2, CZ + 38), (CX, GZ + 7)], w=2.6)         # square -> gate
    # (test round 2: every building sits at least 4 inside the outline; the rangerstation, its tower, the truck station
    # and the aaihoek stood partly outside it, so they were cut off at the back)
    path([(CX - SQ_R, CZ + 2), (CX - 22, CZ + 6), (49, 61)])                                  # -> giftshop
    path([(CX - 11, CZ - 11), (52, 45), (48, 41), (46, 40)])                                 # -> rangerstation door
    path([(CX + 2, CZ - SQ_R), (CX + 5, CZ - 22), (74, 38)])                                 # -> watergat
    path([(CX + 11, CZ - 10), (82, 49), (88, 47)])                                           # -> truck station
    path([(CX + SQ_R, CZ + 1), (CX + 24, CZ + 4), (100, 65), (103, 66)])                     # -> aaihoek (barn door)
    path([(CX + 9, CZ + 12), (CX + 20, CZ + 22), (90, 84), (93, 86)])                        # -> picknick
    path([(CX - 9, CZ + 12), (CX - 18, CZ + 20), (46, 78), (44, 80)])                        # -> lodges (deck)
    ALPACA_LOOP = [(120, 60), (126, 59), (130, 64), (127, 72), (121, 74), (119, 67)]
    path(ALPACA_LOOP + [ALPACA_LOOP[0]], w=1.2, mats=["minecraft:dirt_path", "minecraft:coarse_dirt"])
    # where the park route crosses the outline: a clear park gate there, and the path runs from it to the truck
    # station (east of Het Watergat, never past its terrace), and from the station to the square
    for (kx, kz) in kruising_cellen:
        if kz < 20 and kx < 92:
            path([(kx, kz + 1), (kx + 1, kz + 4), (85, 13), (88, 19), (90, 27), (91, 34), (93, 39)], w=2.2)
        else:
            path([(kx, kz + 1), ((kx + 98) / 2, kz + 3), (98, 39)], w=2.2)
    # bus forecourt in front of the gate
    for x in range(CX - 6, CX + 7):
        for z in range(GZ - 2, GZ + 9):
            S.set(x, G, z, "minecraft:gray_concrete_powder" if x in (CX - 6, CX + 6) else "minecraft:smooth_stone")
            S.air(x, G + 1, z, x, G + 4, z)
            PATHS.add((x, z))

    # ---- square + elephant fountain ----
    for x in range(CX - SQ_R, CX + SQ_R + 1):
        for z in range(CZ - SQ_R, CZ + SQ_R + 1):
            d = math.hypot(x - CX, z - CZ) + (vnoise(x / 3, z / 3, 13) - 0.5) * 1.2
            if d <= SQ_R:
                S.set(x, G, z, "minecraft:cut_sandstone" if int(d) % 4 == 0 else "minecraft:smooth_sandstone")
                S.air(x, G + 1, z, x, G + 4, z)
                PATHS.add((x, z))
    for x in range(CX - 7, CX + 8):
        for z in range(CZ - 7, CZ + 8):
            d = math.hypot(x - CX, z - CZ)
            if d <= 6.6:
                S.set(x, G, z, BED)
                S.set(x, G + 1, z, WATER)
            if 5.6 < d <= 6.6:
                S.set(x, G + 1, z, "minecraft:smooth_sandstone")
                S.set(x, G + 2, z, slab("minecraft:smooth_sandstone_slab"))
    S.cylinder(CX, CZ, 1.8, G + 1, G + 3, "minecraft:chiseled_sandstone")
    olifant(CX - 1, CZ + 1, y=G + 3, facing=1)
    S.fill(CX, G + 6, CZ + 5, CX, G + 8, CZ + 5, "minecraft:light_gray_concrete")       # raised trunk
    for k in range(10):
        ang = k / 10 * 2 * math.pi + 0.3
        x, z = int(round(CX + math.cos(ang) * (SQ_R - 2))), int(round(CZ + math.sin(ang) * (SQ_R - 2)))
        if k % 2 == 0:
            S.fill(x, G + 1, z, x, G + 2, z, "minecraft:dark_oak_fence")
            S.set(x, G + 3, z, "park_lantaarn")
        else:
            f = "east" if math.cos(ang) < -0.4 else "west" if math.cos(ang) > 0.4 else ("south" if math.sin(ang) < 0 else "north")
            S.set(x, G + 1, z, stairs("minecraft:spruce_stairs", f))
    for (x, z) in ((CX - 10, CZ - 6), (CX + 9, CZ + 8), (CX - 8, CZ + 9), (CX + 10, CZ - 7)):
        S.set(x, G + 1, z, OL)
        S.set(x, G + 2, z, "guh_bloempot")

    OCC = set(PATHS)

    def claim(x1, z1, x2, z2, m=2):
        for x in range(x1 - m, x2 + m + 1):
            for z in range(z1 - m, z2 + m + 1):
                OCC.add((x, z))

    def gable(x1, z1, x2, z2, y, mat="minecraft:dark_oak_stairs", ridge_axis="x", gablewall="minecraft:spruce_planks", over=1):
        if ridge_axis == "x":
            a, b = z1 - over, z2 + over
            lvl = 0
            while a <= b:
                for x in range(x1 - over, x2 + over + 1):
                    if a == b:
                        S.set(x, y + lvl, a, slab(mat.replace("_stairs", "_slab")))
                    else:
                        S.set(x, y + lvl, a, stairs(mat, "south"))
                        S.set(x, y + lvl, b, stairs(mat, "north"))
                for x in (x1, x2):
                    for z in range(a + 1, b):
                        S.set(x, y + lvl, z, gablewall)
                a, b, lvl = a + 1, b - 1, lvl + 1
        else:
            a, b = x1 - over, x2 + over
            lvl = 0
            while a <= b:
                for z in range(z1 - over, z2 + over + 1):
                    if a == b:
                        S.set(a, y + lvl, z, slab(mat.replace("_stairs", "_slab")))
                    else:
                        S.set(a, y + lvl, z, stairs(mat, "east"))
                        S.set(b, y + lvl, z, stairs(mat, "west"))
                for z in (z1, z2):
                    for x in range(a + 1, b):
                        S.set(x, y + lvl, z, gablewall)
                a, b, lvl = a + 1, b - 1, lvl + 1

    def guh_ears(x, y, z, axis="x", gap=6):
        for e in (-gap // 2, gap // 2):
            ex, ez = (x + e, z) if axis == "x" else (x, z + e)
            S.fill(ex, y, ez, ex, y + 1, ez, "minecraft:pink_wool")
            S.set(ex, y + 2, ez, "minecraft:pink_wool")

    # ---- giftshop (W), L-shape ----
    GX1, GZ1, GX2, GZ2 = 26, 52, 44, 66
    STRIPES = ["minecraft:pink_terracotta", "minecraft:yellow_terracotta", "minecraft:light_blue_terracotta"]
    box(GX1, G + 1, GZ1, GX2, G + 6, GZ2, "minecraft:white_terracotta", floor="minecraft:oak_planks")
    box(GX1 + 4, G + 1, GZ2, GX1 + 12, G + 5, GZ2 + 6, "minecraft:white_terracotta", floor="minecraft:oak_planks")
    S.air(GX1 + 5, G + 1, GZ2, GX1 + 11, G + 5, GZ2)
    for x in range(GX1, GX2 + 1):
        for z in range(GZ1, GZ2 + 7):
            for y in range(G + 1, G + 3):
                if S.get(x, y, z) == "minecraft:white_terracotta":
                    S.set(x, y, z, STRIPES[((x + z) // 2) % 3])
    windows(GX1, G + 3, GZ1, GX2, GZ2, every=3)
    door(GX2, G + 1, 58, 3)
    top = thatch(GX1, GZ1, GX2, GZ2, G + 7, overhang=1)
    thatch(GX1 + 4, GZ2 + 1, GX1 + 12, GZ2 + 6, G + 6, overhang=1)
    guh_ears(GX1 + 9, top - 1, 59, axis="x", gap=8)
    for z in range(54, 65):
        S.set(GX2 + 1, G + 4, z, "minecraft:orange_wool" if z % 2 else "minecraft:white_wool")
    S.fill(GX2 + 1, G + 5, 55, GX2 + 1, G + 6, 63, "minecraft:orange_concrete")
    S.set(GX2 + 1, G + 6, 59, "knuffelsteen_gezicht[facing=east,stemming=1]")
    for z in (53, 65):
        S.set(GX2 + 2, G + 1, z, "minecraft:barrel[facing=up,open=false]")
        S.set(GX2 + 2, G + 2, z, "minecraft:yellow_carpet")
        S.fill(GX2 + 3, G + 1, z, GX2 + 3, G + 2, z, "minecraft:oak_fence")
        S.set(GX2 + 3, G + 3, z, "minecraft:pink_wool")
    claim(GX1, GZ1, GX2 + 3, GZ2 + 6)

    # ---- rangerstation (NW) + map + dex board + lookout tower ----
    RX1, RZ1, RX2, RZ2 = 26, 33, 44, 46
    DOOR_Z = RZ1 + 6                     # the door (east wall, 2 wide), the stairs run along the wall north of it
    box(RX1, G + 1, RZ1, RX2, G + 9, RZ2, "minecraft:spruce_planks", floor="minecraft:spruce_planks")
    S.fill(RX1, G + 5, RZ1, RX2, G + 5, RZ2, "minecraft:stripped_spruce_log[axis=x]")
    S.air(RX1 + 1, G + 5, RZ1 + 1, RX2 - 1, G + 5, RZ2 - 1)
    S.fill(RX1 + 1, G + 5, RZ1 + 1, RX2 - 1, G + 5, RZ2 - 1, "minecraft:spruce_planks")    # upper floor
    S.air(RX2 - 3, G + 5, RZ1 + 2, RX2 - 2, G + 5, RZ1 + 5)                                   # stairwell
    for i in range(4):
        for sx in (RX2 - 3, RX2 - 2):
            S.set(sx, G + 1 + i, RZ1 + 6 - i, stairs("minecraft:spruce_stairs", "north"))
    windows(RX1, G + 3, RZ1, RX2, RZ2, every=3)
    windows(RX1, G + 7, RZ1, RX2, RZ2, every=3)
    door(RX2, G + 1, DOOR_Z, 2)
    S.set(RX2 + 1, G, DOOR_Z, "minecraft:spruce_planks")
    thatch(RX1, RZ1, RX2, RZ2, G + 10, overhang=1)
    for z in range(RZ1 + 1, RZ2):
        S.set(RX2 + 1, G, z, "minecraft:spruce_planks"); S.set(RX2 + 2, G, z, "minecraft:spruce_planks")
    for z in (RZ1 + 1, RZ2 - 1):
        S.fill(RX2 + 2, G + 1, z, RX2 + 2, G + 4, z, LOG)
    S.fill(RX2 + 1, G + 5, RZ1 + 1, RX2 + 2, G + 5, RZ2 - 1, slab("minecraft:spruce_slab"))
    MAP = ["WWWiiiiBBB", "EEEEiiiBBR", "EE~~~~~~RR", "EAAAERRRDD", "AAAAARRR~D", "AA~~~~~~~D", "kSSSSSSSSS"]
    COL = {"W": "minecraft:white_concrete", "i": "minecraft:light_gray_concrete", "B": "minecraft:lime_concrete",
           "E": "minecraft:green_concrete", "R": "minecraft:green_terracotta", "D": "minecraft:cyan_terracotta",
           "A": "minecraft:orange_terracotta", "S": "minecraft:yellow_concrete", "~": "minecraft:light_blue_concrete",
           "k": "minecraft:pink_concrete"}
    MX0 = 28
    for r, row in enumerate(MAP):
        for c, ch in enumerate(row):
            S.set(MX0 + c, G + 8 - r, RZ2 + 1, COL[ch])
    S.fill(MX0 - 1, G + 1, RZ2 + 1, MX0 - 1, G + 9, RZ2 + 1, POST)
    S.fill(MX0 + 10, G + 1, RZ2 + 1, MX0 + 10, G + 9, RZ2 + 1, POST)
    S.fill(MX0 - 1, G + 9, RZ2 + 1, MX0 + 10, G + 9, RZ2 + 1, slab("minecraft:dark_oak_slab"))
    # the dex board: on the front beside the map (it hung on the back wall, facing the fence), in a frame of posts
    DX0 = MX0 + 12
    S.fill(DX0 - 1, G + 1, RZ2 + 1, DX0 - 1, G + 9, RZ2 + 1, POST)
    S.fill(DX0 + 5, G + 1, RZ2 + 1, DX0 + 5, G + 9, RZ2 + 1, POST)
    S.fill(DX0 - 1, G + 9, RZ2 + 1, DX0 + 5, G + 9, RZ2 + 1, slab("minecraft:dark_oak_slab"))
    S.fill(DX0, G + 3, RZ2 + 1, DX0 + 4, G + 8, RZ2 + 1, "minecraft:spruce_planks")
    for i in range(15):
        S.set(DX0 + (i % 5), G + 8 - (i // 5) * 2, RZ2 + 1,
              rnd.choice(["minecraft:yellow_terracotta", "minecraft:orange_terracotta", "minecraft:light_gray_concrete",
                          "minecraft:brown_concrete", "minecraft:pink_terracotta"]))
    TX, TZ = RX1 + 5, RZ1 - 6
    for (a, b) in ((0, 0), (4, 0), (0, 4), (4, 4)):
        S.fill(TX + a, G + 1, TZ + b, TX + a, G + 17, TZ + b, LOG)
    for yy in (G + 6, G + 12):
        for (a, b) in ((0, 2), (4, 2), (2, 0), (2, 4)):
            S.set(TX + a, yy, TZ + b, "minecraft:stripped_spruce_log[axis=y]")
    S.fill(TX + 2, G + 1, TZ + 2, TX + 2, G + 16, TZ + 2, "minecraft:ladder[facing=north,waterlogged=false]")
    S.fill(TX + 2, G + 1, TZ + 3, TX + 2, G + 16, TZ + 3, "minecraft:spruce_planks")              # ladder backing
    S.fill(TX, G + 17, TZ, TX + 4, G + 17, TZ + 4, "minecraft:spruce_planks")
    S.set(TX + 2, G + 17, TZ + 2, "minecraft:spruce_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]")
    for a in range(5):
        for b in range(5):
            if a in (0, 4) or b in (0, 4):
                S.set(TX + a, G + 18, TZ + b, "minecraft:spruce_fence")
    for (a, b) in ((0, 0), (4, 0), (0, 4), (4, 4)):
        S.fill(TX + a, G + 18, TZ + b, TX + a, G + 19, TZ + b, LOG)
    thatch(TX, TZ, TX + 4, TZ + 4, G + 20, overhang=1)
    claim(TX, TZ, RX2 + 2, RZ2 + 1)

    # ---- expertenpaviljoen (next to the rangerstation): octagon, 8 counters in the zone colours ----
    PVX, PVZ, PVR = 54, 37, 5.5
    EXPERTS = ["minecraft:yellow_wool", "minecraft:cyan_wool", "minecraft:green_wool", "minecraft:lime_wool",
               "minecraft:light_blue_wool", "minecraft:light_gray_wool", "minecraft:green_terracotta", "minecraft:orange_wool"]
    for x in range(int(PVX - PVR) - 2, int(PVX + PVR) + 3):
        for z in range(int(PVZ - PVR) - 2, int(PVZ + PVR) + 3):
            d = max(abs(x - PVX), abs(z - PVZ), (abs(x - PVX) + abs(z - PVZ)) / 1.42)
            if d <= PVR + 0.5:
                S.set(x, G, z, "minecraft:spruce_planks" if d <= PVR - 0.5 else "minecraft:stripped_spruce_log[axis=x]")
                S.air(x, G + 1, z, x, G + 8, z)
    S.set(PVX, G, PVZ, "minecraft:chiseled_sandstone")
    for k in range(8):
        a = k / 8 * 2 * math.pi + math.pi / 8
        px, pz = int(round(PVX + math.cos(a) * PVR)), int(round(PVZ + math.sin(a) * PVR))
        S.fill(px, G + 1, pz, px, G + 6, pz, LOG)
        wool = EXPERTS[k]
        S.set(px, G + 4, pz, wool)
        b = a + math.pi / 8
        bx, bz = int(round(PVX + math.cos(b) * (PVR - 0.6))), int(round(PVZ + math.sin(b) * (PVR - 0.6)))
        S.set(bx, G + 1, bz, "minecraft:barrel[facing=up,open=false]")
        S.set(bx, G + 2, bz, wool.replace("_wool", "_carpet").replace("green_terracotta", "green_carpet"))
    for x in range(int(PVX - PVR) - 1, int(PVX + PVR) + 2):
        for z in range(int(PVZ - PVR) - 1, int(PVZ + PVR) + 2):
            d = max(abs(x - PVX), abs(z - PVZ), (abs(x - PVX) + abs(z - PVZ)) / 1.42)
            if PVR - 0.5 < d <= PVR + 0.5 and S.is_air(x, G + 7, z):
                S.set(x, G + 7, z, slab("minecraft:spruce_slab"))
    thatch_round(PVX, PVZ, PVR - 2.2, G + 8)
    S.fill(PVX - 3, G + 1, PVZ + 8, PVX - 3, G + 3, PVZ + 8, "minecraft:spruce_fence")
    S.fill(PVX + 3, G + 1, PVZ + 8, PVX + 3, G + 3, PVZ + 8, "minecraft:spruce_fence")
    S.fill(PVX - 3, G + 4, PVZ + 8, PVX + 3, G + 5, PVZ + 8, "minecraft:yellow_concrete")
    S.fill(PVX - 2, G + 4, PVZ + 8, PVX + 2, G + 4, PVZ + 8, "minecraft:orange_concrete")
    S.set(PVX, G + 5, PVZ + 8, "knuffelsteen_gezicht[facing=south,stemming=1]")
    path([(60, 50), (56, 47), (PVX, PVZ + 7)], w=1.6)
    claim(int(PVX - PVR) - 1, int(PVZ - PVR) - 1, int(PVX + PVR) + 1, PVZ + 9, m=1)

    # ---- Het Watergat (N), terrace to the north over the savanne ----
    WX1, WZ1, WX2, WZ2 = 62, 24, 86, 36
    box(WX1, G + 1, WZ1, WX2, G + 6, WZ2, "minecraft:stripped_jungle_log[axis=y]", floor="minecraft:jungle_planks")
    for x in range(WX1 + 1, WX2):
        for z in (WZ1, WZ2):
            for y in range(G + 2, G + 5):
                S.set(x, y, z, "minecraft:glass_pane" if x % 3 else "minecraft:stripped_jungle_log[axis=y]")
    door(73, G + 1, WZ2, 3)
    door(70, G + 1, WZ1, 3)
    thatch(WX1, WZ1, WX2, WZ2, G + 7, overhang=2)
    S.fill(69, G + 7, WZ2 + 3, 77, G + 8, WZ2 + 3, "minecraft:light_blue_concrete")
    S.fill(70, G + 7, WZ2 + 3, 76, G + 7, WZ2 + 3, "minecraft:blue_concrete")
    S.fill(68, G + 1, WZ2 + 3, 68, G + 8, WZ2 + 3, LOG)
    S.fill(78, G + 1, WZ2 + 3, 78, G + 8, WZ2 + 3, LOG)
    S.set(73, G + 8, WZ2 + 3, "knuffelsteen_gezicht[facing=south,stemming=1]")
    TERR = []
    for x in range(WX1 - 2, 81):
        for z in range(15, WZ1):
            if (x, z) in INS:
                S.set(x, G + 1, z, "minecraft:spruce_planks")
                S.air(x, G + 2, z, x, G + 6, z)
                TERR.append((x, z))
    TS = set(TERR)
    for (x, z) in TERR:
        if any((x + a, z + b) not in TS for a in (-1, 0, 1) for b in (-1, 0, 1)) and z < WZ1 - 1:
            S.set(x, G + 2, z, "minecraft:spruce_fence")      # 8-neighbour edge: a closed, 4-connected railing
    PARA = ["minecraft:orange_wool", "minecraft:white_wool", "minecraft:red_wool"]
    for i, (x, z) in enumerate(((63, 18), (69, 17), (75, 18))):
        S.set(x, G + 2, z, "minecraft:spruce_fence")
        S.set(x, G + 3, z, "minecraft:white_carpet")
        for (a, b, f) in ((-1, 0, "east"), (1, 0, "west"), (0, -1, "south"), (0, 1, "north")):
            S.set(x + a, G + 2, z + b, stairs("minecraft:spruce_stairs", f))
        S.fill(x + 1, G + 3, z + 1, x + 1, G + 5, z + 1, "minecraft:spruce_fence")
        for a in range(-1, 4):
            for b in range(-1, 4):
                if (a - 1) ** 2 + (b - 1) ** 2 <= 4:
                    S.set(x + a, G + 6, z + b, PARA[(a + b + i) % 3])
    claim(WX1 - 2, 14, WX2, WZ2 + 3)

    # ---- truck station (NE): roof, platform, benches, sign (the trucks come later as entities) ----
    SX1, SZ1, SX2, SZ2 = 88, 38, 106, 49
    for x in range(SX1, SX2 + 1):
        for z in range(SZ1, SZ2 + 1):
            S.set(x, G, z, "minecraft:smooth_sandstone" if z > SZ2 - 3 else "minecraft:dirt_path")
            S.air(x, G + 1, z, x, G + 6, z)
            PATHS.add((x, z))
    for x in range(SX1, SX2 + 1, 6):
        for z in (SZ1, SZ2):
            S.fill(x, G + 1, z, x, G + 6, z, LOG)
    thatch(SX1, SZ1, SX2, SZ2, G + 7, overhang=1)
    S.fill(SX1 + 1, G + 1, SZ2, SX2 - 1, G + 1, SZ2, slab("minecraft:smooth_sandstone_slab", "top"))
    for x in range(SX1 + 2, SX2 - 1, 4):
        S.set(x, G + 1, SZ2 - 1, stairs("minecraft:spruce_stairs", "south"))
    S.fill(SX1 - 2, G + 1, SZ2 + 1, SX1 - 2, G + 3, SZ2 + 1, "minecraft:spruce_fence")
    S.fill(SX1 - 3, G + 4, SZ2 + 1, SX1 - 1, G + 4, SZ2 + 1, "minecraft:yellow_concrete")
    S.set(SX1 - 2, G + 5, SZ2 + 1, "minecraft:yellow_concrete")
    claim(SX1, SZ1, SX2, SZ2)

    # ---- aaihoek (E): red barn + four pens (fences 2 high: nothing inside can be climbed over) ----
    BX1, BZ1, BX2, BZ2 = 104, 58, 116, 70
    box(BX1, G + 1, BZ1, BX2, G + 5, BZ2, "minecraft:red_terracotta", floor="minecraft:spruce_planks")
    for x in range(BX1, BX2 + 1):
        S.set(x, G + 5, BZ1, "minecraft:white_terracotta"); S.set(x, G + 5, BZ2, "minecraft:white_terracotta")
    door(BX1, G + 1, 65, 3)
    windows(BX1, G + 3, BZ1, BX2, BZ2, every=4)
    gable(BX1, BZ1, BX2, BZ2, G + 6, mat="minecraft:dark_oak_stairs", ridge_axis="x", gablewall="minecraft:red_terracotta")
    S.set(BX1 - 1, G + 5, 66, "knuffelsteen_gezicht[facing=west,stemming=0]")
    for (x0, z0, fn) in PENS:
        for x in range(x0, x0 + 6):
            for z in range(z0, z0 + 7):
                S.set(x, G, z, "minecraft:coarse_dirt" if (x + z) % 3 else "minecraft:grass_block[snowy=false]")
                S.air(x, G + 1, z, x, G + 3, z)
                if x in (x0, x0 + 5) or z in (z0, z0 + 6):
                    S.fill(x, G + 1, z, x, G + 2, z, "minecraft:oak_fence")
        gz_ = z0 + 6 if z0 < 60 else z0
        facing = "south" if z0 < 60 else "north"
        S.set(x0 + 2, G + 1, gz_, f"minecraft:oak_fence_gate[facing={facing},in_wall=false,open=false,powered=false]")
        S.set(x0 + 2, G + 2, gz_, AIR)
        S.set(x0 + 4, G + 1, z0 + 1 if z0 < 60 else z0 + 5, "minecraft:hay_block[axis=y]")
        S.set(x0 + 4, G + 1, z0 + 4 if z0 < 60 else z0 + 2, "minecraft:cauldron")
        claim(x0, z0, x0 + 5, z0 + 6, m=1)
    S.set(BX1 - 2, G + 1, 62, "minecraft:composter[level=0]")
    claim(BX1, BZ1, BX2, BZ2)

    # ---- lodge huts on stilts in the pond (SW): gable roofs ----
    HUTS = [(20, 82, "x"), (30, 90, "z"), (20, 92, "x"), (42, 84, "z"), (40, 94, "x")]
    for (hx, hz, ax) in HUTS:
        FY = G + 3
        for (a, b) in ((0, 0), (5, 0), (0, 5), (5, 5)):
            S.fill(hx + a, G - 2, hz + b, hx + a, FY, hz + b, LOG)
        S.fill(hx - 1, FY, hz - 1, hx + 6, FY, hz + 6, "minecraft:spruce_planks")
        box(hx, FY + 1, hz, hx + 5, FY + 3, hz + 5, "minecraft:stripped_spruce_log[axis=y]", corner=LOG)
        for (a, b) in ((2, 0), (3, 0), (2, 5), (3, 5), (0, 2), (5, 3)):
            S.set(hx + a, FY + 2, hz + b, "minecraft:glass_pane")
        S.air(hx + 5, FY + 1, hz + 2, hx + 5, FY + 2, hz + 2)
        for a in range(-1, 7):
            for b in (-1, 6):
                S.set(hx + a, FY + 1, hz + b, "minecraft:spruce_fence")
        gable(hx, hz, hx + 5, hz + 5, FY + 4, mat="minecraft:spruce_stairs", ridge_axis=ax, gablewall="minecraft:spruce_planks")
        S.set(hx + 6, FY + 1, hz + 2, "park_lantaarn")
    DECK = slab("minecraft:spruce_slab")
    for (hx, hz, ax) in HUTS:
        for (px, pz) in catmull([(48, 79), ((48 + hx + 7) / 2, (79 + hz + 2) / 2 + 2), (hx + 7, hz + 2)], n=20):
            for (x, z) in ((int(px), int(pz)), (int(px) + 1, int(pz))):
                if S.is_air(x, G + 1, z) or (x, z) in POND:
                    if S.get(x, G + 3, z) != "minecraft:spruce_planks":
                        S.set(x, G + 1, z, DECK)
                        OCC.add((x, z))
    # the deck (standing at G+1.5) up to the hut platform (standing at G+4): half steps at each hut end
    for (hx, hz, ax) in HUTS:
        S.set(hx + 8, G + 1, hz + 2, slab("minecraft:spruce_slab", "top"))
        S.set(hx + 7, G + 1, hz + 2, "minecraft:spruce_planks")
        S.set(hx + 7, G + 2, hz + 2, slab("minecraft:spruce_slab", "top"))
        S.air(hx + 7, G + 3, hz + 2, hx + 8, G + 6, hz + 2)
        S.set(hx + 6, G + 4, hz + 2, AIR)
        S.set(hx + 6, G + 4, hz + 3, "park_lantaarn")
    for (x, z) in POND:
        if rnd.random() < 0.05 and S.is_air(x, G + 1, z):
            S.set(x, G + 1, z, "minecraft:lily_pad")
    for (x, z) in SAND:
        naast_water = any((x + a, z + b) in POND for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if 0 <= x < X and 0 <= z < Z and naast_water and rnd.random() < 0.18 and S.is_air(x, HGT[(x, z)] + 1, z) \
                and S.get(x, HGT[(x, z)], z) == "minecraft:sand":
            S.set(x, HGT[(x, z)] + 1, z, "minecraft:sugar_cane")
            S.set(x, HGT[(x, z)] + 2, z, "minecraft:sugar_cane")

    # ---- picnic spot (SE) ----
    PKX, PKZ = 92, 86
    for (dx, dz) in ((0, 0), (8, 3), (3, 8)):
        x, z = PKX + dx, PKZ + dz
        S.fill(x, G + 1, z, x + 2, G + 1, z, slab("minecraft:spruce_slab", "top"))
        S.fill(x, G + 1, z - 1, x + 2, G + 1, z - 1, stairs("minecraft:spruce_stairs", "south"))
        S.fill(x, G + 1, z + 1, x + 2, G + 1, z + 1, stairs("minecraft:spruce_stairs", "north"))
        S.set(x + 1, G + 2, z, "minecraft:red_carpet")
    claim(PKX - 1, PKZ - 2, PKX + 12, PKZ + 10, m=1)
    acacia(PKX + 7, PKZ - 3, y=G)
    claim(PKX + 4, PKZ - 6, PKX + 10, PKZ, m=0)

    # ---- park gate with ticket booths (S) ----
    for gx in (CX - 9, CX + 7):
        S.fill(gx, G + 1, GZ - 1, gx + 2, G + 10, GZ + 1, "minecraft:spruce_planks")
        for (a, b) in ((0, -1), (2, -1), (0, 1), (2, 1)):
            S.fill(gx + a, G + 1, GZ + b, gx + a, G + 10, GZ + b, POST)
        thatch(gx, GZ - 1, gx + 2, GZ + 1, G + 11, overhang=1)
    S.fill(CX - 7, G + 9, GZ, CX + 6, G + 9, GZ, "minecraft:dark_oak_log[axis=x]")
    S.fill(CX - 6, G + 7, GZ, CX + 5, G + 8, GZ, "minecraft:yellow_concrete")
    S.fill(CX - 5, G + 7, GZ, CX + 4, G + 7, GZ, "minecraft:orange_concrete")
    S.set(CX, G + 8, GZ, "knuffelsteen_gezicht[facing=south,stemming=1]")
    S.air(CX - 6, G + 1, GZ, CX + 5, G + 6, GZ)
    for kx in (CX - 15, CX + 11):
        box(kx, G + 1, GZ - 9, kx + 4, G + 3, GZ - 5, "minecraft:yellow_terracotta", floor="minecraft:spruce_planks")
        for zz in (GZ - 8, GZ - 6):
            S.set(kx + (4 if kx < CX else 0), G + 2, zz, "minecraft:glass_pane")
        door(kx + (4 if kx < CX else 0), G + 1, GZ - 7)
        thatch(kx, GZ - 9, kx + 4, GZ - 5, G + 4, overhang=1)
        claim(kx, GZ - 9, kx + 4, GZ - 5)
    claim(CX - 10, GZ - 2, CX + 10, GZ + 2)

    # ---- safari fence along the organic outline, gaps for the gate and the park route ----
    gaps = set()
    for (x, z) in EDGE:
        if abs(x - CX) <= 6 and z > CZ:
            gaps.add((x, z))
    for (kx, kz) in kruising_cellen:
        for (x, z) in EDGE:
            if math.hypot(x - kx, z - kz) <= 2.5:
                gaps.add((x, z))
    rand_ord = sorted(EDGE, key=lambda p: math.atan2(p[1] - CZ, p[0] - CX))
    poortpalen = set()
    for i, (x, z) in enumerate(rand_ord):
        if (x, z) in gaps or (x, z) in TS:
            continue
        if any((x + a, z + b) in gaps for a in (-1, 0, 1) for b in (-1, 0, 1)):
            poortpalen.add((x, z))
        S.air(x, G + 1, z, x, G + 5, z)
        if i % 3 == 0 or (x, z) in poortpalen:
            S.fill(x, G + 1, z, x, G + 4, z, POST)
            S.set(x, G + 5, z, "minecraft:dark_oak_fence")
            if (x, z) in poortpalen and abs(x - CX) > 9:
                S.fill(x, G + 5, z, x, G + 7, z, POST)                  # tall posts beside the route gaps
        else:
            S.fill(x, G + 1, z, x, G + 3, z, "minecraft:iron_bars")
            S.set(x, G + 4, z, "minecraft:dark_oak_fence")
        OCC.add((x, z))
    # the bus forecourt outside the gate: same fence around it
    EXTRA = set()
    for z in range(GZ + 1, GZ + 10):
        for x in range(CX - 7, CX + 8):
            EXTRA.add((x, z))
            if x in (CX - 7, CX + 7) or z == GZ + 9:
                S.fill(x, G + 1, z, x, G + 3, z, "minecraft:iron_bars")
                S.set(x, G + 4, z, "minecraft:dark_oak_fence")
                if (x + z) % 3 == 0:
                    S.fill(x, G + 1, z, x, G + 4, z, POST)
                    S.set(x, G + 5, z, "minecraft:dark_oak_fence")
                OCC.add((x, z))
    for gx in (CX - 9, CX + 7):                                       # the ticket booth posts stick out a bit
        for a in range(3):
            for b in (-1, 0, 1):
                EXTRA.add((gx + a, GZ + b))
    # a bus stop sign + bench on the forecourt
    S.fill(CX + 4, G + 1, GZ + 6, CX + 4, G + 3, GZ + 6, "minecraft:spruce_fence")
    S.set(CX + 4, G + 4, GZ + 6, "minecraft:yellow_concrete")
    S.fill(CX - 4, G + 1, GZ + 5, CX - 2, G + 1, GZ + 5, stairs("minecraft:spruce_stairs", "west"))

    # ---- the park gates where the route crosses the fence: an open arch (posts, beam, sign), no loose gap ----
    BORDJES = []
    for (kx, kz) in kruising_cellen:
        gat = [(x, z) for (x, z) in gaps if math.hypot(x - kx, z - kz) <= 3.5]
        palen = [p for p in poortpalen if math.hypot(p[0] - kx, p[1] - kz) <= 5.5]
        if len(palen) < 2 or not gat:
            continue
        a, b = max(((p, q) for p in palen for q in palen), key=lambda pq: math.hypot(pq[0][0] - pq[1][0], pq[0][1] - pq[1][1]))
        for (x, z) in (a, b):
            S.fill(x, G + 1, z, x, G + 8, z, POST)
            S.set(x, G + 9, z, "minecraft:dark_oak_fence")
            S.set(x, G + 10, z, "park_lantaarn")
        # the beam: a straight line of logs between the two posts at G+8
        n = max(abs(a[0] - b[0]), abs(a[1] - b[1]))
        as_ = "x" if abs(a[0] - b[0]) >= abs(a[1] - b[1]) else "z"
        for i in range(1, n):
            x = round(a[0] + (b[0] - a[0]) * i / n)
            z = round(a[1] + (b[1] - a[1]) * i / n)
            S.set(x, G + 8, z, f"minecraft:dark_oak_log[axis={as_}]")
            S.air(x, G + 1, z, x, G + 6, z)
        mx, mz = round((a[0] + b[0]) / 2), round((a[1] + b[1]) / 2)
        S.fill(mx - (1 if as_ == "x" else 0), G + 7, mz - (0 if as_ == "x" else 1),
               mx + (1 if as_ == "x" else 0), G + 7, mz + (0 if as_ == "x" else 1), "minecraft:yellow_concrete")
        # a sign beside the path on the village side
        kand = [(a[0] + dx, a[1] + dz) for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2), (2, 2), (-2, 2), (2, -2), (-2, -2))]
        kand = [(x, z) for (x, z) in kand if (x, z) in INS and (x, z) not in PATHS and S.is_air(x, G + 1, z)
                and S.get(x, G, z) != AIR]
        if kand:
            sx, sz = max(kand, key=lambda c: rand_d[c[0], c[1]])          # the village side of the post
            rot = 0 if as_ == "x" else 4
            S.set(sx, G + 1, sz, f"minecraft:spruce_sign[rotation={rot},waterlogged=false]")
            BORDJES.append(((sx, G + 1, sz), ["", "Het park in", "Safaripark", "Vadse Bergen"]))

    # ---- rock groups along the fence (inside, kept 3 away from it: escape rule) ----
    for (rx, rz, r) in ((16, 52, 2.6), (20, 42, 2.0), (112, 84, 2.4), (34, 16, 2.0), (56, 104, 1.6)):
        if (rx, rz) not in INS or rand_d[rx, rz] < r + 3:
            continue
        S.ellipsoid(rx, G, rz, r, r * 0.8, r * 0.9, "minecraft:granite" if rx % 2 else "minecraft:andesite")
        S.ellipsoid(rx + 1, G + 1, rz - 1, r * 0.6, r * 0.5, r * 0.6, "minecraft:mossy_cobblestone")
        claim(int(rx - r), int(rz - r), int(rx + r), int(rz + r), m=0)

    # ---- greenery inside the village ----
    def oaktree(x, z):
        y = HGT[(x, z)]
        h = rnd.randint(4, 6)
        S.fill(x, y + 1, z, x, y + h, z, "minecraft:oak_log[axis=y]")
        S.ellipsoid(x, y + h, z, 2.6, 2.0, 2.6, OL, keep=True)

    def free(x, z, r):
        return all((x + a, z + b) not in OCC and (x + a, z + b) not in POND for a in range(-r, r + 1) for b in range(-r, r + 1))

    def take(x, z, r):
        for a in range(-r, r + 1):
            for b in range(-r, r + 1):
                OCC.add((x + a, z + b))

    cells = sorted(INS)
    rnd.shuffle(cells)
    for (x, z) in cells:
        if (x, z) in OCC or (x, z) in POND or rand_d[x, z] < 3:
            continue
        y = HGT[(x, z)]
        if not S.is_air(x, y + 1, z):
            continue
        if S.get(x, y, z).split("[")[0] not in ("minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:dirt"):
            continue
        r = rnd.random()
        if r < 0.030 and free(x, z, 2) and rand_d[x, z] > 5:
            palm(x, z, y=y); take(x, z, 2)
        elif r < 0.050 and free(x, z, 2) and rand_d[x, z] > 5:
            oaktree(x, z); take(x, z, 2)
        elif r < 0.060 and free(x, z, 3) and rand_d[x, z] > 6:
            acacia(x, z, y=y); take(x, z, 3)
        elif r < 0.09:
            S.set(x, y + 1, z, "minecraft:bush")
        elif r < 0.22:
            S.set(x, y + 1, z, "minecraft:short_grass")
        elif r < 0.27:
            S.set(x, y + 1, z, rnd.choice(["roze_guhbloem", "minecraft:dandelion", "minecraft:poppy", "guhoortjes",
                                           "minecraft:azure_bluet"]))
        elif r < 0.31:
            S.set(x, y + 1, z, OL)
    for (x, z, f) in ((56, 44, "south"), (86, 52, "west"), (60, 82, "east"), (90, 78, "north"), (52, 64, "east")):
        if S.is_air(x, G + 1, z):
            S.fill(x, G + 1, z, x, G + 2, z, "minecraft:dark_oak_fence")
            S.set(x, G + 3, z, "park_lantaarn")
        if S.is_air(x + 1, G + 1, z + 1):
            S.set(x + 1, G + 1, z + 1, stairs("minecraft:spruce_stairs", f))

    S.fix_connections()
    info = {"POND": POND, "PATHS": PATHS, "EXTRA": EXTRA, "PENS": PENS, "TS": TS, "BORDJES": BORDJES}
    return S, info


# ----------------------------------------------------------------------------------------------- into the park
def _kruisingen(park):
    """Clusters of outline cells where the park route (park.pad) crosses; one representative cell per cluster."""
    cel = sorted((x, z) for (x, z) in EDGE if park.pad[OX + x, OZ + z])
    clusters = [[c] for c in cel]
    samen = True
    while samen:                                          # merge clusters closer than 4 (manhattan)
        samen = False
        for i in range(len(clusters)):
            for j in range(i + 1, len(clusters)):
                if any(abs(a - c) + abs(b - d) <= 4 for (a, b) in clusters[i] for (c, d) in clusters[j]):
                    clusters[i] += clusters.pop(j)
                    samen = True
                    break
            if samen:
                break
    uit = []
    for cl in clusters:
        mx = sum(a for a, _ in cl) / len(cl)
        mz = sum(b for _, b in cl) / len(cl)
        uit.append(min(cl, key=lambda p: math.hypot(p[0] - mx, p[1] - mz)))
    return uit


def _naam(s):
    return s if ":" in s.split("[")[0] else "guhs:" + s


def bouw(park, log=print):
    kruising = _kruisingen(park)
    S, info = _dorp(kruising)
    W = kit.W
    schrijf = np.zeros((W, W), bool)
    for (x, z) in INS | info["EXTRA"]:
        schrijf[OX + x, OZ + z] = True

    _bomen_weg(park, schrijf)

    # copy the local columns
    lut = np.array([park.id(_naam(s)) if i else 0 for i, s in enumerate(S.pal)], np.uint16)
    stone = park.id("minecraft:stone")
    vloer = park.vloer
    for (x, z) in INS | info["EXTRA"]:
        wx, wz = OX + x, OZ + z
        kol = park.v[wx, :, wz]
        kol[OY:] = 0
        kol[OY:OY + Y] = lut[S.v[x, :, z]]
        for y in range(int(min(vloer[wx, wz], OY)), OY):
            if kol[y] == 0 or park.pal[kol[y]].startswith("minecraft:water"):
                kol[y] = stone
    # park bookkeeping
    pond = info["POND"]
    for (x, z) in INS | info["EXTRA"]:
        wx, wz = OX + x, OZ + z
        park.zone[wx, wz] = 0                               # entree
        park.zone_vol[wx, wz] = 0
        park.binnen[wx, wz] = True
        park.water[wx, wz] = (x, z) in pond
        park.hoogte[wx, wz] = OY + G - 2 if (x, z) in pond else OY + G
        park.pad[wx, wz] = (x, z) in info["PATHS"]
        park.hek[wx, wz] = False
        park.vloer[wx, wz] = min(int(vloer[wx, wz]), OY)
    for (x, z) in EDGE:
        if S.get(x, G + 1, z).split("[")[0] in ("minecraft:iron_bars", "minecraft:dark_oak_log"):
            park.hek[OX + x, OZ + z] = True
    for (x, z) in info["EXTRA"]:
        if S.get(x, G + 1, z).split("[")[0] in ("minecraft:iron_bars", "minecraft:dark_oak_log"):
            park.hek[OX + x, OZ + z] = True
    # the sign texts (the tile writer puts them in as block entities)
    if not hasattr(park, "bordjes"):
        park.bordjes = []
    for ((x, y, z), regels) in info["BORDJES"]:
        park.bordjes.append({"pos": [OX + x, OY + y, OZ + z], "tekst": list(regels)})

    # blend around the village, but keep clear of the fences that remain outside it (their strip is flat already)
    park.hek_afstand = distance_transform_edt(~(park.hek & ~park.water & ~schrijf))
    _overgang(park, schrijf, log)
    park.hek_afstand = distance_transform_edt(~(park.hek & ~park.water))
    _bruggen(park, schrijf, log)
    _kade(park, schrijf)
    doos = (OX - 45, OZ - 45, OX + X + 45, OZ + Z + 45)
    _vul_gaten(park, doos, log)
    _vaste_bladeren(park, doos)
    park.plein = (OX + CX, OY + G, OZ + CZ)
    park.ingang = (OX + CX + 0.5, OY + G + 1, OZ + CZ + 11.5, 180.0)
    log(f"    entreedorp at world {OX}..{OX + X}, {OZ}..{OZ + Z}; route gaps at {[(OX + a, OZ + b) for a, b in kruising]}")


HOUT = ("_leaves", "_log", "_wood", "vine", "palm", "mangrove_roots", "propagule")


def _bomen_weg(park, schrijf):
    """Trees whose trunk is in the village area go, crown and all. A leaf or branch belongs to the NEAREST trunk, so
    the crowns of the trees that stay outside are left whole (cutting a radius around the trunk took bites out of
    the neighbouring trees: half crowns, leaves without a log that broke)."""
    W = kit.W
    weg = [b for b in park.bomen if schrijf[b[0], b[2]]]
    if not weg:
        return
    stammen = np.array([(b[0], b[2]) for b in park.bomen], float)
    weg_idx = {i for i, b in enumerate(park.bomen) if schrijf[b[0], b[2]]}
    hout_ids = np.array([i for i, s in enumerate(park.pal) if s.split("[")[0].endswith(HOUT)], np.int64)
    for i in sorted(weg_idx):
        tx, ttop, tz = park.bomen[i][0], park.bomen[i][1], park.bomen[i][2]
        x0, x1 = max(tx - 9, 0), min(tx + 10, W)
        z0, z1 = max(tz - 9, 0), min(tz + 10, W)
        for x in range(x0, x1):
            for z in range(z0, z1):
                if schrijf[x, z]:
                    continue                        # the village columns are replaced anyway
                d = np.hypot(stammen[:, 0] - x, stammen[:, 1] - z)
                if int(np.argmin(d)) not in weg_idx:
                    continue
                g = int(park.hoogte[x, z])
                kol = park.v[x, g + 1:min(ttop + 5, kit.H), z]
                kol[np.isin(kol, hout_ids)] = 0
    park.bomen = [b for b in park.bomen if not schrijf[b[0], b[2]]]


def _overgang(park, schrijf, log):
    """Blend 9 blocks around the village to its height (24 on the decor side outside the park): the 3 nearest flat
    (escape rule), then a ramp to the old ground. Not near existing fences (their strip is already flat at their own
    height), not over water and not under a tree trunk. Crowns hanging over a blended column stay (only the ground
    and the plants on it change)."""
    d = distance_transform_edt(~schrijf)
    doel = OY + G
    xs, zs = np.nonzero((d > 0) & (d <= 24))
    padblok = park.id("minecraft:dirt_path")
    dirt = park.id("minecraft:dirt")
    n = 0
    for x, z in zip(xs, zs):
        if park.water[x, z] or park.hek[x, z] or park.hek_afstand[x, z] <= 3:
            continue
        lim = 9 if park.binnen[x, z] else 24                # the decor outside the park gets a long, soft slope
        if d[x, z] > lim:
            continue
        oud = int(park.hoogte[x, z])
        t = min(max((d[x, z] - 3) / (lim - 3.0), 0.0), 1.0)
        nieuw = int(round(doel + (oud - doel) * t))
        if nieuw == oud:
            continue
        top = park.v[x, oud, z]
        if top == 0:
            continue
        if park.get(x, oud + 1, z).split("[")[0].endswith(("_log", "_wood")):
            continue                                    # a trunk stands here: leave its ground alone
        # the old surface and the plants on it go; leaves higher up (a crown over the column) stay
        for y in range(min(oud, nieuw) + 1, min(max(oud, nieuw) + 3, kit.H)):
            naam = park.get(x, y, z).split("[")[0]
            if naam.endswith(("_log", "_wood", "_leaves")) and y > nieuw:
                continue
            park.v[x, y, z] = 0
        for y in range(oud, nieuw):
            park.v[x, y, z] = dirt
        park.v[x, nieuw, z] = padblok if park.pad[x, z] else top
        park.hoogte[x, z] = nieuw
        n += 1
    log(f"    entreedorp: {n} columns blended around the village")


def _bruggen(park, schrijf, log):
    """The route bridges right by the village (test round 2: they lay flat on the water, 1 below the ground, and a
    canoe could not pass): a plank deck at water + 3 (two blocks of air above the water), railings on the water cells
    alongside, posts every few blocks, and steps up at both ends."""
    from scipy.ndimage import label
    W = kit.W
    wy = kit.WATER_Y
    dek_y = wy + 3
    brug = park.pad & park.water & ~schrijf
    lab, n = label(brug)
    if not n:
        return
    dicht = distance_transform_edt(~schrijf) <= 4
    water_id = park.id("minecraft:water", level="0")
    planks = park.id("minecraft:spruce_planks")
    hek = park.id("minecraft:spruce_fence", east="false", west="false", north="false", south="false",
                  waterlogged="false")
    paal = park.id("minecraft:stripped_spruce_log", axis="y")
    lantaarn = park.id("guhs:park_lantaarn")
    vast = ("sand", "dirt", "gravel", "clay", "stone", "mud", "grass_block", "coarse_dirt", "andesite", "granite",
            "diorite", "sandstone", "terracotta", "podzol", "rooted_dirt", "moss_block", "deepslate", "tuff")
    gedaan = 0
    for k in range(1, n + 1):
        cel = lab == k
        if not (cel & dicht).any():
            continue                                    # only the bridges at the village
        xs, zs = np.nonzero(cel)
        x0, x1, z0, z1 = max(xs.min() - 3, 0), min(xs.max() + 4, W), max(zs.min() - 3, 0), min(zs.max() + 4, W)
        dek = set(zip(xs.tolist(), zs.tolist()))
        # railings: water cells (not deck) that touch the deck, 8-neighbourhood (a closed, 4-connected railing)
        reling = set()
        for (x, z) in dek:
            for a in (-1, 0, 1):
                for b in (-1, 0, 1):
                    c = (x + a, z + b)
                    if c not in dek and 0 <= c[0] < W and 0 <= c[1] < W and park.water[c] and not schrijf[c]:
                        reling.add(c)
        for (x, z) in dek | reling:
            # the old deck goes, water back up to the water line, then the new deck
            for y in range(wy + 1, min(wy + 10, kit.H)):
                park.v[x, y, z] = 0
            y = wy
            while y > 0:
                naam = park.get(x, y, z).split("[")[0].split(":")[1]
                if park.v[x, y, z] != 0 and any(naam == v or naam.endswith(v) for v in vast):
                    break
                park.v[x, y, z] = water_id
                y -= 1
            park.v[x, dek_y, z] = planks
        for (x, z) in reling:
            park.v[x, dek_y + 1, z] = hek
            if (x * 7 + z * 3) % 4 == 0:                # a post down into the water now and then
                y = dek_y - 1
                while y > 0 and park.get(x, y, z).split("[")[0] in ("minecraft:water", "minecraft:air"):
                    park.v[x, y, z] = paal
                    y -= 1
                if (x + z) % 3 == 0:
                    park.v[x, dek_y + 2, z] = lantaarn
        # steps at both ends: the path cells beside the deck get a stair up to it, the next ring is raised to y-1
        trap = {}
        for (x, z) in dek:
            for (a, b, f) in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
                c = (x + a, z + b)
                if c in dek or c in reling or not (0 <= c[0] < W and 0 <= c[1] < W) or park.water[c]:
                    continue
                if park.pad[c] or schrijf[c]:
                    trap.setdefault(c, f)
        for (x, z), f in trap.items():
            g = int(park.hoogte[x, z])
            if g >= dek_y:
                continue
            for y in range(g + 1, dek_y):
                park.v[x, y, z] = planks
            park.v[x, dek_y, z] = park.id("minecraft:spruce_stairs", facing=f, half="bottom", shape="straight",
                                         waterlogged="false")
            for y in range(dek_y + 1, min(dek_y + 5, kit.H)):
                park.v[x, y, z] = 0
            for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                c = (x + a, z + b)
                if c in dek or c in trap or c in reling or not (0 <= c[0] < W and 0 <= c[1] < W) or park.water[c]:
                    continue
                if (park.pad[c] or schrijf[c]) and int(park.hoogte[c]) < dek_y - 1:
                    h = int(park.hoogte[c])
                    top = park.v[c[0], h, c[1]]
                    for y in range(h, dek_y - 1):
                        park.v[c[0], y, c[1]] = park.id("minecraft:dirt")
                    park.v[c[0], dek_y - 1, c[1]] = top if top else park.id("minecraft:dirt_path")
                    for y in range(dek_y, min(dek_y + 4, kit.H)):
                        if park.get(c[0], y, c[1]).split("[")[0] not in ("minecraft:dark_oak_log", "minecraft:iron_bars"):
                            park.v[c[0], y, c[1]] = 0
                    park.hoogte[c] = dek_y - 1
        gedaan += 1
    log(f"    entreedorp: {gedaan} bridge(s) by the village raised to y {dek_y}")


def _kade(park, schrijf):
    """Where the village edge meets open water outside it, its side is a stone quay instead of a dirt wall."""
    W = kit.W
    mix = [park.id("minecraft:stone_bricks"), park.id("minecraft:mossy_stone_bricks"), park.id("minecraft:stone_bricks"),
           park.id("minecraft:cracked_stone_bricks")]
    wy = kit.WATER_Y
    xs, zs = np.nonzero(schrijf)
    for x, z in zip(xs, zs):
        if park.water[x, z]:
            continue
        aan_water = False
        for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            c = (x + a, z + b)
            if 0 <= c[0] < W and 0 <= c[1] < W and not schrijf[c] and park.water[c]:
                aan_water = True
        if not aan_water:
            continue
        for y in range(wy - 2, OY + G):
            if park.v[x, y, z] != 0:
                park.v[x, y, z] = mix[(x * 3 + y * 5 + z) % len(mix)]


def _vul_gaten(park, doos, log):
    """Every column in the box reaches down to 2 below its lowest neighbour, so nowhere a side shows the void under
    the ground (the blended slopes and the decor hill in front of the gate had such holes)."""
    W, H = kit.W, kit.H
    x0, z0, x1, z1 = max(doos[0], 0), max(doos[1], 0), min(doos[2], W), min(doos[3], W)
    v = park.v[x0:x1, :, z0:z1]
    nz = v != 0
    heeft = nz.any(axis=1)
    bot = np.where(heeft, np.argmax(nz, axis=1), H)
    top = np.where(heeft, H - 1 - np.argmax(nz[:, ::-1, :], axis=1), -1)
    laag = top.copy()
    for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nb = np.full_like(top, H)
        sx = slice(max(a, 0), top.shape[0] + min(a, 0))
        tx = slice(max(-a, 0), top.shape[0] + min(-a, 0))
        sz = slice(max(b, 0), top.shape[1] + min(b, 0))
        tz = slice(max(-b, 0), top.shape[1] + min(-b, 0))
        nb[tx, tz] = top[sx, sz]
        nb[nb < 0] = H
        laag = np.minimum(laag, nb)
    stone = park.id("minecraft:stone")
    dirt = park.id("minecraft:dirt")
    n = 0
    for i, k in zip(*np.nonzero(heeft & (bot > laag - 2))):
        tot = max(int(laag[i, k]) - 2, 0)
        for y in range(tot, int(bot[i, k])):
            v[i, y, k] = dirt if y >= int(top[i, k]) - 3 else stone
        park.vloer[x0 + i, z0 + k] = min(int(park.vloer[x0 + i, z0 + k]), tot)
        n += 1
    log(f"    entreedorp: {n} columns filled down (no void under the ground)")


def _vaste_bladeren(park, doos):
    """All leaves around the village are persistent (no decaying leaves dropping saplings and sticks)."""
    W = kit.W
    x0, z0, x1, z1 = max(doos[0], 0), max(doos[1], 0), min(doos[2], W), min(doos[3], W)
    vervang = {}
    for i, s in enumerate(park.pal):
        if s.split("[")[0].endswith("_leaves") and "persistent=false" in s:
            vervang[i] = park.id(s.replace("persistent=false", "persistent=true"))
    if not vervang:
        return
    v = park.v[x0:x1, :, z0:z1]
    for oud, nieuw in vervang.items():
        v[v == oud] = nieuw


def plekken(park, plekken_):
    """The four aaihoek animals live in their pens (1 adult + 1 young each)."""
    pens = {fn: (x0, z0) for (x0, z0, fn) in PENS}
    for p in plekken_:
        if p["soort"] in pens:
            x0, z0 = pens[p["soort"]]
            p["zone"] = "entree"
            p["centrum"] = [OX + x0 + 2, OY + G + 1, OZ + z0 + 3]
            p["straal"] = 2
            p["water"] = False
            p["boom"] = False
