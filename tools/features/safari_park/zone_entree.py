"""
Zone module "entree": the lobby of Safaripark Vadse Bergen, after the approved lobby2 design (test round 4, design
rounds 47-56: safari-ontwerp/lobby2/ontwerp.md, the _v2 renders and lobby2_overzicht_legenda.png).

The lobby is built in its own local scene (240 x 56 x 224, ground top at local y 16 = world 64) and copied into the park
at OX, OZ. park.lobby = {"oorsprong": [OX, 64, OZ], "g": G, "draai": 0}: local (x, y, z) -> world (OX + x, 64 + y - G,
OZ + z). North is up, as in the design:
  * north-west: the vijver (the outdoor aquarium pond) and the lodges. The Kriebelhuis plot (zone_kriebelhuis builds
    there after this module): local x 0..182, z 0..96 stays flat grass, except the pond, the lodges and their paths;
    the aai-lagune (22..58 x 90..114) and the Kriebelhuis exit + shop (58..88 x 86..110) are left free for it too;
  * north-east: the haven (the old entree lake, joined to the river): the boat line's hoofdsteiger and the canoe rental
    (zone_s3_rit builds the jetty at park.boot_start) and the Botenhuis. The safaritruck road comes in from Australie
    at the north edge (local x 189), runs down past the Kriebelhuis and turns east along z 98 to the savanne; its stop
    (zone_s3_rit's open platform, hint park.truckperron) is on the haven side, over a zebra crossing;
  * middle: the dorpsplein with the elephant fountain; the giftshop west of it, the rangerstation (museum) east;
  * east: the oostpoort (on foot into the savanne), the aaihoek (stable + 4 pens), the expertenpaviljoen (9 counters);
  * west and south-west: the kampvuur, the big guh-speeltuin, the ijskraam;
  * south: the entreegebouw (welkomsthal) with the olifantenpoort and the busplein: the Guh-safaribus takes you back to
    the Guhmension (ring the bell at the bus stop, or click the bus: feature/safari/lobby/Bushalte).
Why the haven is in the north-east and not at the west pond (as in the render): the river comes in from the north-east
and the truck from the north, so a canal from the west pond to the river would have to cross the truck road.

What changes around the lobby (world):
  * the lobby's footprint becomes zone "entree", inside the park, with its own fence; what is left of the old entree
    zone goes to the neighbouring zone (Australie in the north, the savanne in the east); the zone fences there are
    drawn again;
  * the park route is spliced (park.route_lijn): Australie -> the north gate -> the lobby's truck road -> the east gate
    -> the savanne; the old village road and its path are gone;
  * a basin north of the haven joins it to the river (the boat line and the canoe start in the haven);
  * a ring around the lobby is blended to its height on the decor side.
Only blocks: no NPCs, no animals, no trucks or boats (the game brings them; the toys with a block entity that are
multi-blocks are placed by the game from beleving.json).
"""
import json
import math
import os
import random

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit

# ----------------------------------------------------------------------------------------------- placement (world)
OX, OZ = 20, 530             # local (0, 0) -> world
G = 16                       # local ground top (the grass you stand on is at G, feet at G + 1)
OY = 64 - G                  # local y 0 -> world 48, so the ground top G is world 64
X, Y, Z = 240, 56, 224
CX, CZ = 122, 126            # middle of the dorpsplein (the fountain)
SQ_RX, SQ_RZ = 26, 20        # the square's half axes
SQ_R = 20                    # (lobby.json: the visitor guhs stay within SQ_R + 6 of the fountain)
WEG_X, WEG_Z = 189, 98       # the truck road: north-south along x = WEG_X, then east along z = WEG_Z
HAVEN_X1 = 202               # the haven: x >= HAVEN_X1, z <= ~58
KRIEBEL = (0, 0, 182, 96)    # the Kriebelhuis plot (zone_kriebelhuis)
KRIEBEL_VRIJ = [(22, 90, 58, 114), (58, 86, 88, 110)]   # aai-lagune, exit + shop (zone_kriebelhuis)
PCX, PCZ, PRX, PRZ = 42, 54, 37, 29                     # the vijver (as the render)

WATER = "minecraft:water[level=0]"
BED = "minecraft:sand"
AIR = "minecraft:air"
HAY = "minecraft:hay_block[axis=y]"
LOG = "minecraft:stripped_spruce_log[axis=y]"
POST = "minecraft:dark_oak_log[axis=y]"
GRAY = "minecraft:light_gray_concrete"
GRASS = "minecraft:grass_block[snowy=false]"
DEUR = "guhs:roze_lakhout_deur"
AL = "minecraft:acacia_leaves[distance=1,persistent=true,waterlogged=false]"
OL = "minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]"
HEG = "minecraft:azalea_leaves[distance=1,persistent=true,waterlogged=false]"
HEGB = "minecraft:flowering_azalea_leaves[distance=1,persistent=true,waterlogged=false]"
PATHPAL = ["guhs:knuffelklinkers", "minecraft:smooth_sandstone", "guhs:knuffelklinkers", "minecraft:sandstone"]
LAMPION = ["lampion_geel[hanging=true]", "lampion_roze[hanging=true]", "lampion_mint[hanging=true]"]
KNUFFELS = ["knuffel_normal", "knuffel_mint", "knuffel_choco", "knuffel_snow", "knuffel_pinguh", "knuffel_wolk",
            "knuffel_pluisguh", "knuffel_glitter", "knuffel_rainbow", "knuffel_teckel", "knuffel_brontosaurus",
            "reisbureau_souvenir_knuffeldal", "knuffel_koning", "knuffel_zeemeerguh"]
# the aaihoek pens (x0, z0, animal): 8 x 9, the gate in the south side
PENS = [(194, 128, "geitje"), (203, 128, "alpaca"), (212, 128, "cavia"), (221, 128, "ezeltje")]
EXPERT_ZONES = ["savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie", "kriebelhuis"]


# ----------------------------------------------------------------------------------------------- noise
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
              "air", "dandelion", "poppy", "bluet", "snow", "composter", "cauldron", "palm_blad", "lampion", "knuffel",
              "bed", "kussen", "stoel", "tafel", "bankje", "campfire", "bell", "ladder", "tunnel", "bellenblaas",
              "luchtscooter", "potted", "tulip", "daisy", "torch", "chain")


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

    def set(self, x, y, z, s, keep=False):
        x, y, z = int(x), int(y), int(z)
        if not self.inb(x, y, z) or (keep and self.v[x, y, z] != 0):
            return
        self.v[x, y, z] = self.id(s)

    def get(self, x, y, z):
        return self.pal[self.v[x, y, z]] if self.inb(x, y, z) else AIR

    def is_air(self, x, y, z):
        return self.get(x, y, z) == AIR

    def fill(self, x1, y1, z1, x2, y2, z2, s, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, s, keep)

    def walls(self, x1, y1, z1, x2, y2, z2, s):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                if x in (x1, x2) or z in (z1, z2):
                    for y in range(min(y1, y2), max(y1, y2) + 1):
                        self.set(x, y, z, s)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, AIR)

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, s, keep=False):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1:
                        self.set(x, y, z, s, keep)

    def cylinder(self, cx, cz, r, y1, y2, s, keep=False):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                if math.hypot(x - cx, z - cz) <= r:
                    for y in range(y1, y2 + 1):
                        self.set(x, y, z, s, keep)

    def top_y(self, x, z):
        nz = np.nonzero(self.v[x, :, z])[0]
        return int(nz.max()) if len(nz) else -1

    def fix_connections(self):
        """Glass panes, iron bars and fences connect to their family and to solid blocks (like the game)."""
        def fam(n):
            n = n.split("[")[0]
            if n.endswith("_fence") or n.split(":")[-1].startswith("guh_hekje"):     # (the bouwhulp's guh fences)
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


# ----------------------------------------------------------------------------------------------- footprint
def _in_fp(x, z):
    """The lobby's footprint: a rounded rectangle (corner radius 14) inside the scene."""
    x0, x1, z0, z1, r = 2, 237, 2, 221, 14
    if not (x0 <= x <= x1 and z0 <= z <= z1):
        return False
    dx = max(x0 + r - x, 0, x - (x1 - r))
    dz = max(z0 + r - z, 0, z - (z1 - r))
    return math.hypot(dx, dz) <= r


FP = {(x, z) for x in range(X) for z in range(Z) if _in_fp(x, z)}
# the fence ring: every footprint cell with an outside cell among its 8 neighbours (a 4-connected ring)
EDGE = {(x, z) for (x, z) in FP if any((x + a, z + b) not in FP for a in (-1, 0, 1) for b in (-1, 0, 1) if a or b)}


def _catmull(pts, n=12):
    out = []
    for i in range(len(pts) - 1):
        p0 = pts[max(i - 1, 0)]; p1 = pts[i]; p2 = pts[i + 1]; p3 = pts[min(i + 2, len(pts) - 1)]
        for k in range(n):
            t = k / n; t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2
                                    + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in (0, 1)))
    out.append(pts[-1])
    return out


def _dicht(pts, stap=0.5):
    """A polyline resampled every `stap` blocks."""
    uit = [pts[0]]
    for (ax, az), (bx, bz) in zip(pts, pts[1:]):
        n = max(1, int(math.hypot(bx - ax, bz - az) / stap))
        for i in range(1, n + 1):
            uit.append((ax + (bx - ax) * i / n, az + (bz - az) * i / n))
    return uit


# the truck road (local): straight north-south at WEG_X, a bend, straight east at WEG_Z. Its middle lies on x = WEG_X
# (z = WEG_Z) exactly, so the 4-wide safaripoort of zone_s3_rit lands on cells WEG_X - 2 .. WEG_X + 1 with its posts
# on WEG_X - 3 and WEG_X + 2: the fence leaves those 6 cells free
WEG = _dicht([(WEG_X, -14.0)] + _catmull([(WEG_X, 76.0), (WEG_X, 84.0), (WEG_X + 3.0, 92.5), (WEG_X + 10.0, WEG_Z),
                                         (WEG_X + 18.0, WEG_Z)], n=16)[1:-1] + [(WEG_X + 18.0, WEG_Z), (X + 30.0, WEG_Z)])
WEG_HALF = 2.6


def _weg_cellen():
    cel = set()
    for (px, pz) in WEG:
        for x in range(int(px - WEG_HALF) - 1, int(px + WEG_HALF) + 2):
            for z in range(int(pz - WEG_HALF) - 1, int(pz + WEG_HALF) + 2):
                if math.hypot(x + 0.5 - px, z + 0.5 - pz) <= WEG_HALF:
                    cel.add((x, z))
    return cel


WEGCEL = _weg_cellen()
# INS: the "village" for zone_s3_rit (its dorpsweg search and the boat channel leave it alone): the footprint without
# the truck road (the route runs over the road, never inside INS, so zone_s3_rit keeps the road as it is)
INS = FP - WEGCEL


KANAAL_X = (220, 230)         # test round 5: the haven reaches the north edge only here (the boats' channel); elsewhere
                             # a strip of land with the fence keeps the lobby closed (the fence was missing over the water)


def _haven_cellen():
    cel = set()
    for x in range(HAVEN_X1, 234):
        for z in range(0, 60):
            rand = 54 + 4 * vnoise(x / 7.0, 3.3, 31)                      # an organic south shore
            hoek = math.hypot(max(HAVEN_X1 + 6 - x, 0), max(z - (rand - 6), 0))
            if z <= rand and hoek <= 6.5 and (z >= 5 or KANAAL_X[0] <= x <= KANAAL_X[1]):
                cel.add((x, z))
    return cel


HAVEN = _haven_cellen()


# ----------------------------------------------------------------------------------------------- the lobby scene
def _lobby():
    """Builds the lobby in a local scene. Returns (S, info)."""
    rnd = random.Random(16)
    S = Lokaal()
    PATHS = set()
    OCC = set()
    BORDJES = []
    LAMPEN = []

    def claim(x1, z1, x2, z2, m=2):
        for x in range(x1 - m, x2 + m + 1):
            for z in range(z1 - m, z2 + m + 1):
                OCC.add((x, z))

    def bordje(x, y, z, rot, regels, hout="birch"):
        S.set(x, y, z, f"minecraft:{hout}_sign[rotation={rot},waterlogged=false]")
        BORDJES.append(((x, y, z), list(regels)))

    def muurbord(x, y, z, facing, regels, hout="birch"):
        S.set(x, y, z, f"minecraft:{hout}_wall_sign[facing={facing},waterlogged=false]")
        BORDJES.append(((x, y, z), list(regels)))

    def deur(x, y, z, facing, hinge="left"):
        S.set(x, y, z, f"{DEUR}[facing={facing},half=lower,hinge={hinge},open=false,powered=false]")
        S.set(x, y + 1, z, f"{DEUR}[facing={facing},half=upper,hinge={hinge},open=false,powered=false]")

    def dubbeldeur(x, y, z, facing, as_):
        """Two guh doors side by side (as_ 'x': at x, x+1; 'z': at z, z+1), hinges on the outer sides."""
        a, b = ((x, z), (x + 1, z)) if as_ == "x" else ((x, z), (x, z + 1))
        deur(a[0], y, a[1], facing, "left")
        deur(b[0], y, b[1], facing, "right")

    def lantaarnpaal(x, z, y=G):
        S.fill(x, y + 1, z, x, y + 2, z, "minecraft:dark_oak_fence")
        S.set(x, y + 3, z, "park_lantaarn")
        LAMPEN.append((x, z))
        OCC.add((x, z))

    def hang(x, z, y0, blok=None, tot=10):
        """A hanging lantern under the first block above (x, y0, z)."""
        blok = blok or rnd.choice(LAMPION)
        for y in range(y0 + 1, y0 + tot):
            if not S.is_air(x, y, z):
                if y - 1 > y0 and S.is_air(x, y - 1, z):
                    S.set(x, y - 1, z, blok)
                return

    def tapijt(x1, z1, x2, z2, y, kleur, rand=None):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                if S.is_air(x, y, z) and not S.is_air(x, y - 1, z):
                    k = rand if rand and (x in (x1, x2) or z in (z1, z2)) else kleur
                    S.set(x, y, z, f"minecraft:{k}_carpet")

    def knuffel(x, y, z, f, i):
        S.set(x, y, z, f"{KNUFFELS[i % len(KNUFFELS)]}[facing={f}]")

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

    def dak(x1, z1, x2, z2, y, overhang=1, plafond="minecraft:birch_planks"):
        """A flat ceiling at y over the walls, the thatched roof on top of it (a hollow pyramid alone would float)."""
        S.fill(x1, y, z1, x2, y, z2, plafond)
        return thatch(x1, z1, x2, z2, y + 1, overhang)

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

    def ramen(x1, y, z1, x2, z2, every=3, vrij=()):
        """Glass panes in the walls, never within 2 of a door (vrij: the door cells)."""
        def ok(x, z):
            return all(abs(x - a) + abs(z - b) > 2 for (a, b) in vrij)
        for x in range(x1 + 2, x2 - 1, every):
            for z in (z1, z2):
                if ok(x, z):
                    S.set(x, y, z, "minecraft:glass_pane")
        for z in range(z1 + 2, z2 - 1, every):
            for x in (x1, x2):
                if ok(x, z):
                    S.set(x, y, z, "minecraft:glass_pane")

    def pad(x1, z1, x2, z2, pal=PATHPAL, y=G):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                if 0 <= x < X and 0 <= z < Z and (x, z) in FP and (x, z) not in POND and (x, z) not in HAVEN \
                        and (x, z) not in WEGCEL:
                    S.set(x, y, z, rnd.choice(pal))
                    S.air(x, y + 1, z, x, y + 5, z)
                    PATHS.add((x, z))
                    OCC.add((x, z))

    def pad_lijn(pts, w=3, pal=PATHPAL, lampen=0):
        """A path (w wide) along straight segments; lampen > 0: a lantern post beside it every `lampen` blocks."""
        n_ = 0
        for (ax, az), (bx, bz) in zip(pts, pts[1:]):
            n = int(max(abs(bx - ax), abs(bz - az)))
            for i in range(n + 1):
                t = i / max(n, 1)
                x, z = round(ax + (bx - ax) * t), round(az + (bz - az) * t)
                pad(x - w // 2, z - w // 2, x + (w - 1) // 2, z + (w - 1) // 2, pal)
                n_ += 1
                if lampen and n_ % lampen == 0:
                    L = math.hypot(bx - ax, bz - az) or 1
                    nx_, nz_ = -(bz - az) / L, (bx - ax) / L
                    for kant in (1, -1):
                        lx, lz = round(x + nx_ * (w // 2 + 2) * kant), round(z + nz_ * (w // 2 + 2) * kant)
                        if vrij_grond(lx, lz):
                            lantaarnpaal(lx, lz)
                            break

    def vrij_grond(x, z, r=0):
        for a in range(-r, r + 1):
            for b in range(-r, r + 1):
                c = (x + a, z + b)
                if c not in FP or c in EDGE or c in OCC or c in POND or c in HAVEN or c in WEGCEL or in_kriebel(*c):
                    return False
                if not S.is_air(c[0], G + 1, c[1]):
                    return False
        return True

    def in_kriebel(x, z):
        if KRIEBEL[0] <= x <= KRIEBEL[2] and KRIEBEL[1] <= z <= KRIEBEL[3]:
            return True
        return any(a <= x <= c and b <= z <= d for (a, b, c, d) in KRIEBEL_VRIJ)

    # ================================================================ ground
    for x in range(X):
        for z in range(Z):
            S.fill(x, 0, z, x, G - 4, z, "minecraft:stone")
            S.fill(x, G - 3, z, x, G - 1, z, "minecraft:dirt")
            S.set(x, G, z, GRASS)

    # ================================================================ VIJVER (north-west): the outdoor aquarium
    POND = set()
    for x in range(10, 82):
        for z in range(20, 88):
            r = math.hypot((x - PCX) / PRX, (z - PCZ) / PRZ) + (vnoise(x / 9.0, z / 9.0, 5) - 0.5) * 0.12
            if r < 1.0:
                POND.add((x, z))
    for (x, z) in POND:
        depth = 10 if math.hypot((x - PCX) / PRX, (z - PCZ) / PRZ) < 0.82 and x > 14 else 3
        S.set(x, G - depth, z, BED)
        S.air(x, G - depth + 1, z, x, G, z)
        S.fill(x, G - depth + 1, z, x, G, z, WATER)
    for x in range(6, 86):
        for z in range(16, 92):
            if (x, z) not in POND and any((x + a, z + b) in POND for a in (-1, 0, 1) for b in (-1, 0, 1)):
                S.set(x, G, z, "minecraft:sand")
                OCC.add((x, z))
    OCC.update(POND)
    for (x, z) in sorted(POND):
        if S.get(x, G - 10, z) != BED:
            if rnd.random() < 0.05:
                S.set(x, G + 1, z, "minecraft:lily_pad")
            continue
        if x >= 32:                                         # rif-lagune: coral, anemones, sea lanterns on the bed
            r = rnd.random()
            if r < 0.10:
                S.set(x, G - 9, z, rnd.choice(["minecraft:tube_coral_block", "minecraft:brain_coral_block",
                                               "minecraft:fire_coral_block", "minecraft:horn_coral_block",
                                               "minecraft:bubble_coral_block"]))
                if rnd.random() < 0.6:
                    S.set(x, G - 8, z, rnd.choice(["minecraft:tube_coral[waterlogged=true]",
                                                   "minecraft:brain_coral[waterlogged=true]",
                                                   "minecraft:fire_coral[waterlogged=true]",
                                                   "minecraft:horn_coral[waterlogged=true]"]))
            elif r < 0.14:
                S.set(x, G - 9, z, "minecraft:sea_lantern")
            elif r < 0.22:
                S.set(x, G - 9, z, "minecraft:seagrass")
        else:                                               # kelpbos: rocks and tall kelp
            r = rnd.random()
            if r < 0.22:
                hk = rnd.randint(4, 8)
                for k in range(hk):
                    S.set(x, G - 9 + k, z, "minecraft:kelp_plant")
                S.set(x, G - 9 + hk, z, "minecraft:kelp[age=20]")
            elif r < 0.27:
                S.set(x, G - 9, z, rnd.choice(["minecraft:andesite", "minecraft:tuff", "minecraft:mossy_cobblestone"]))

    # ================================================================ HAVEN (north-east): boats + canoes
    for (x, z) in HAVEN:
        S.fill(x, G - 9, z, x, G - 7, z, BED)
        S.air(x, G - 6, z, x, Y - 1, z)
        S.fill(x, G - 6, z, x, G - 2, z, WATER)                   # water top at G - 2 = world 62 (the river's level)
    for x in range(HAVEN_X1 - 2, 238):
        for z in range(0, 64):
            if (x, z) in HAVEN or (x, z) not in FP:
                continue
            if any((x + a, z + b) in HAVEN for a in (-1, 0, 1) for b in (-1, 0, 1)):
                for y in range(G - 6, G + 1):                    # the quay: stone brick sides, a smooth stone edge
                    S.set(x, y, z, rnd.choice(["minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                                               "minecraft:stone_bricks", "minecraft:cracked_stone_bricks"]))
                S.set(x, G, z, "minecraft:smooth_stone")
                OCC.add((x, z))
    OCC.update(HAVEN)

    # ================================================================ the truck road (zone_s3_rit drives it)
    for (x, z) in WEGCEL:
        if (x, z) not in FP:
            continue
        rand = min(math.hypot(x + 0.5 - px, z + 0.5 - pz) for (px, pz) in WEG[::2]) > WEG_HALF - 0.9
        S.set(x, G, z, "minecraft:coarse_dirt" if rand else "minecraft:dirt_path")
        S.air(x, G + 1, z, x, Y - 1, z)
        OCC.add((x, z))
    # the verges: a low hedge on the inner side of the bend and along the south of the east stretch (the truck stop of
    # zone_s3_rit then goes on the haven side), lantern posts along the north-south stretch
    for x in range(WEG_X + 12, 236):
        for z in (WEG_Z + 3, WEG_Z + 4):
            if (x, z) not in WEGCEL and not (206 <= x <= 211):
                S.set(x, G + 1, z, HEGB if (x + z) % 5 == 0 else HEG)
                OCC.add((x, z))
    for z in range(8, 84, 14):
        lantaarnpaal(WEG_X - 5, z)

    # ================================================================ DORPSPLEIN with the elephant fountain
    for x in range(CX - SQ_RX, CX + SQ_RX + 1):
        for z in range(CZ - SQ_RZ, CZ + SQ_RZ + 1):
            if math.hypot((x - CX) / SQ_RX, (z - CZ) / SQ_RZ) < 1:
                d = math.hypot(x - CX, z - CZ)
                S.set(x, G, z, "minecraft:cut_sandstone" if int(d) % 5 == 0 else rnd.choice(PATHPAL))
                S.air(x, G + 1, z, x, G + 6, z)
                PATHS.add((x, z))
                OCC.add((x, z))
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

    def olifant(x, z, y, facing=1, col=GRAY):
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

    olifant(CX - 1, CZ + 1, y=G + 3, facing=1)
    S.fill(CX, G + 6, CZ + 5, CX, G + 8, CZ + 5, GRAY)                    # the raised trunk
    for k in range(12):                                                     # lantern posts + benches round the square
        ang = k / 12 * 2 * math.pi + 0.26
        x, z = int(round(CX + math.cos(ang) * (SQ_RX - 4))), int(round(CZ + math.sin(ang) * (SQ_RZ - 4)))
        if k % 2 == 0:
            S.fill(x, G + 1, z, x, G + 2, z, "minecraft:dark_oak_fence")
            S.set(x, G + 3, z, "park_lantaarn")
            LAMPEN.append((x, z))
        else:
            f = "east" if math.cos(ang) < -0.4 else "west" if math.cos(ang) > 0.4 else ("south" if math.sin(ang) < 0 else "north")
            S.set(x, G + 1, z, f"park_bankje[facing={f}]")
    for (x, z) in ((CX - 12, CZ - 8), (CX + 12, CZ + 8), (CX - 12, CZ + 8), (CX + 12, CZ - 8)):
        S.fill(x - 1, G + 1, z - 1, x + 1, G + 1, z + 1, "minecraft:spruce_planks")
        S.set(x, G + 2, z, "minecraft:flowering_azalea")
        for (a, b) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            S.set(x + a, G + 2, z + b, rnd.choice(["minecraft:poppy", "guhs:roze_guhbloem", "minecraft:oxeye_daisy",
                                                   "guhs:guhoortjes"]))
        S.set(x, G + 2, z - 1, "minecraft:dandelion")

    # ================================================================ paths (before the buildings claim their ground)
    pad(CX - 3, 97, CX + 3, CZ - SQ_RZ + 1)                                  # square -> Kriebelhuis hall (z 96)
    pad(CX - 3, CZ + SQ_RZ - 1, CX + 3, 152)                                 # square -> entreegebouw
    pad(91, 124, CX - SQ_RX + 2, 129)                                        # square -> giftshop door (x 90)
    pad(CX + SQ_RX - 2, 123, 155, 128)                                       # square -> rangerstation door (x 156)
    pad_lijn([(87, 99), (89, 99), (95, 104), (103, 112), (108, 116)], lampen=9)   # Kriebelhuis exit + shop -> square
                                                                             # (round 5: right from the door, no grass row)
    pad_lijn([(141, 113), (150, 106), (170, 105), (236, 105)], lampen=12)    # square -> oostpoort (on foot)
    pad(207, 92, 212, 103, pal=["minecraft:smooth_stone", "minecraft:polished_andesite"])   # to the zebra crossing
    pad_lijn([(189, 106), (189, 172), (203, 172)], lampen=11)                # -> aaihoek, expertenpaviljoen
    pad(190, 138, 231, 140)                                                  # along the pens
    pad_lijn([(143, 140), (165, 158), (189, 168)], lampen=10)                # square -> paviljoen
    pad_lijn([(98, 134), (80, 146), (62, 151), (53, 151)], lampen=9)         # square -> ijskraam, speeltuin
    pad_lijn([(97, 119), (72, 115), (53, 117)], lampen=10)                   # square -> kampvuur
    pad_lijn([(53, 117), (40, 116), (6, 116), (6, 22)], lampen=11)           # kampvuur -> lodges (west path); round 5:
                                                                             # one path on from the square past the campfire
    pad_lijn([(6, 102), (23, 102)], lampen=3)                                # round 5: west path -> the aai-lagune boardwalk
    pad_lijn([(6, 24), (74, 24)], lampen=12)                                 # north shore, in front of the lodges
    pad_lijn([(31, 126), (36, 121), (40, 117)], w=3)                         # speeltuin north gate -> the west path

    # ================================================================ HUB at the haven: Botenhuis, quay, zebra crossing
    # the quay strip between the road and the water (x 192..201) and the south quay: planks (not a path: zone_s3_rit's
    # jetty lowers the ground at its root step by step)
    for x in range(WEG_X + 3, HAVEN_X1):
        for z in range(6, 60):
            if (x, z) in FP and (x, z) not in WEGCEL and (x, z) not in HAVEN and S.get(x, G, z) != "minecraft:smooth_stone":
                S.set(x, G, z, "minecraft:spruce_planks" if (x + z) % 7 else "minecraft:stripped_spruce_wood[axis=x]")
    for x in range(HAVEN_X1, 234):
        for z in range(56, 64):
            if (x, z) in FP and (x, z) not in HAVEN and S.get(x, G, z) == GRASS:
                S.set(x, G, z, "minecraft:spruce_planks")
                OCC.add((x, z))
    for z in range(10, 56, 11):
        S.set(WEG_X + 4, G + 1, z, "park_bankje[facing=east]")
        S.set(WEG_X + 4, G + 1, z + 2, "guh_bloempot")
        OCC.update({(WEG_X + 4, z), (WEG_X + 4, z + 2)})
    # the Botenhuis (boat house + canoe rental)
    BH = (214, 66, 232, 80)
    box(BH[0], G + 1, BH[1], BH[2], G + 6, BH[3], "minecraft:stripped_birch_log[axis=y]", floor="minecraft:spruce_planks")
    for x in range(BH[0], BH[2] + 1):
        for z in range(BH[1], BH[3] + 1):
            if S.get(x, G + 1, z) == "minecraft:stripped_birch_log[axis=y]":
                S.set(x, G + 1, z, "minecraft:light_blue_terracotta")
    ramen(BH[0], G + 3, BH[1], BH[2], BH[3], every=3, vrij=[(BH[0], 73), (BH[0], 74)])
    dubbeldeur(BH[0], G + 1, 73, "west", "z")
    dak(BH[0], BH[1], BH[2], BH[3], G + 7)
    for z in range(BH[1] + 1, BH[3]):                                       # canoes on racks along the east wall
        S.set(BH[2] - 1, G + 2, z, slab("minecraft:spruce_slab", "top"))
        if z % 3:
            S.set(BH[2] - 1, G + 3, z, "minecraft:orange_carpet")
    for x in range(BH[0] + 3, BH[2] - 2, 4):                                 # life rings on the north wall
        S.set(x, G + 4, BH[1] + 1, "minecraft:white_wool")
        S.set(x + 1, G + 4, BH[1] + 1, "minecraft:red_wool")
    for z in range(70, 77):                                                  # the rental counter
        S.set(BH[0] + 6, G + 1, z, "minecraft:spruce_planks")
    S.set(BH[0] + 6, G + 2, 70, "klusjes_guhlampje[facing=west,lit=true]")
    S.set(BH[0] + 6, G + 2, 76, "knuffel_zeemeerguh[facing=west]")
    S.set(BH[0] + 3, G + 1, BH[1] + 1, "guh_bloempot")
    S.set(BH[0] + 3, G + 1, BH[3] - 1, "guh_bloempot")
    tapijt(BH[0] + 1, 71, BH[0] + 5, 76, G + 1, "light_blue", rand="white")
    for (hx, hz) in ((BH[0] + 3, 73), (BH[0] + 9, 70), (BH[0] + 9, 77), (BH[0] + 14, 73)):
        hang(hx, hz, G + 1)
    S.set(BH[0] - 1, G + 5, 73, "knuffelsteen_gezicht[facing=west,stemming=1]")
    S.fill(BH[0] - 1, G + 3, 70, BH[0] - 1, G + 3, 72, "minecraft:light_blue_concrete")
    S.fill(BH[0] - 1, G + 3, 75, BH[0] - 1, G + 3, 77, "minecraft:light_blue_concrete")
    claim(BH[0], BH[1], BH[2], BH[3])
    bordje(BH[0] - 3, G + 1, 70, 4, ["Botenhuis", "Glasbodemboot", "+ kano huren", "~ de haven ~"])
    OCC.add((BH[0] - 3, 70))
    # benches and flowers on the hub, lanterns at the corners of the stop area
    for (x, z, f) in ((203, 66, "south"), (207, 66, "south"), (203, 82, "north"), (207, 82, "north")):
        S.set(x, G + 1, z, f"park_bankje[facing={f}]")
        OCC.add((x, z))
    for (x, z) in ((205, 74), (234, 62), (195, 86), (236, 84)):
        if vrij_grond(x, z):
            lantaarnpaal(x, z)
    # the zebra crossing over the east stretch (white stripes across the road)
    for x in range(206, 212):
        for z in range(WEG_Z - 2, WEG_Z + 3):
            if (x, z) in WEGCEL:
                S.set(x, G, z, "minecraft:white_concrete" if x % 2 == 0 else "minecraft:dirt_path")
    bordje(205, G + 1, 102, 8, ["Kijk uit!", "Safaritruck", "oversteken:", "eerst kijken"])
    OCC.add((205, 102))
    bordje(213, G + 1, 93, 0, ["Haven", "Glasbodemboot", "Kano huren", "Truckhalte ->"])
    OCC.add((213, 93))
    # the stop area (zone_s3_rit builds its open platform here, north of the road): kept free grass
    for x in range(212, 238):
        for z in range(84, WEG_Z - 2):
            OCC.add((x, z))

    # ================================================================ GIFTSHOP (west of the square)
    GX1, GZ1, GX2, GZ2 = 66, 116, 90, 138
    STR = ["minecraft:pink_terracotta", "minecraft:yellow_terracotta", "minecraft:light_blue_terracotta"]
    box(GX1, G + 1, GZ1, GX2, G + 7, GZ2, "minecraft:white_terracotta", floor="minecraft:birch_planks")
    for x in range(GX1, GX2 + 1):
        for z in range(GZ1, GZ2 + 1):
            for y in (G + 1, G + 2):
                if S.get(x, y, z) == "minecraft:white_terracotta":
                    S.set(x, y, z, STR[((x + z) // 2) % 3])
    for wy in (G + 3, G + 4):
        ramen(GX1, wy, GZ1, GX2, GZ2, every=3, vrij=[(GX2, 126), (GX2, 127)])
    dubbeldeur(GX2, G + 1, 126, "east", "z")
    dak(GX1, GZ1, GX2, GZ2, G + 8)
    for e in (-5, 5):                                                       # guh ears on the roof
        ty = S.top_y(78 + e, 127)
        S.fill(78 + e, ty + 1, 127, 78 + e, ty + 3, 127, "minecraft:pink_wool")
    for z in range(119, 136):                                               # striped awning over the front
        S.set(GX2 + 1, G + 5, z, "minecraft:orange_wool" if z % 2 else "minecraft:white_wool")
    S.fill(GX2 + 1, G + 6, 120, GX2 + 1, G + 7, 134, "minecraft:orange_concrete")
    S.set(GX2 + 1, G + 7, 127, "knuffelsteen_gezicht[facing=east,stemming=1]")
    muurbord(GX2 + 1, G + 3, 123, "east", ["Giftshop", "Knuffels, kleding", "en blokken!", "Vahoeg!"])
    for z in (118, 136):
        S.set(GX2 + 2, G + 1, z, "minecraft:barrel[facing=up,open=false]")
        S.set(GX2 + 2, G + 2, z, "minecraft:yellow_carpet")
    # inside: shelves full of plushies along the west wall
    i = 0
    for z in range(GZ1 + 1, GZ2):
        x = GX1 + 1
        if S.get(GX1, G + 3, z).startswith("minecraft:glass_pane"):
            S.set(x, G + 1, z, "guh_bloempot")
            continue
        S.set(x, G + 1, z, "minecraft:birch_planks")
        knuffel(x, G + 2, z, "east", i)
        S.set(x, G + 3, z, slab("minecraft:birch_slab", "top"))
        S.set(x, G + 4, z, f"{KNUFFELS[(i + 5) % len(KNUFFELS)]}[facing=east]")
        i += 1
    # the blocks corner (north wall): the buyable building blocks on display tables
    WAREN = ["guhs:rieten_dak", "guhs:safari_acaciaplanken", "guhs:bamboewand", "guhs:rieten_dak",
             "guhs:safari_acaciaplanken", "guhs:bamboewand"]
    for k, x in enumerate(range(GX1 + 4, GX2 - 3, 3)):
        S.set(x, G + 1, GZ1 + 1, "minecraft:spruce_planks")
        S.set(x, G + 2, GZ1 + 1, WAREN[k % len(WAREN)])
        S.set(x + 1, G + 1, GZ1 + 1, slab("minecraft:spruce_slab", "top"))
    S.set(GX1 + 2, G + 3, GZ1 + 1, "minecraft:birch_wall_sign[facing=south,waterlogged=false]")
    BORDJES.append(((GX1 + 2, G + 3, GZ1 + 1), ["Bouwblokken", "rieten dak,", "acaciaplanken,", "bamboewand"]))
    # the toys corner (south wall): the enrichment toys for the animals in a little pen
    for x in range(GX1 + 4, GX1 + 12):
        S.set(x, G + 1, GZ2 - 4, "minecraft:birch_fence")
    for (x, b) in ((GX1 + 5, "safari_krabboom"), (GX1 + 7, "safari_speelbal"), (GX1 + 9, "safari_schommelband"),
                   (GX1 + 11, "safari_glijbaantje")):
        S.set(x, G + 1, GZ2 - 2, "guhs:" + b)
    S.set(GX1 + 2, G + 1, GZ2 - 4, "minecraft:birch_sign[rotation=8,waterlogged=false]")
    BORDJES.append(((GX1 + 2, G + 1, GZ2 - 4), ["Dierenspeelgoed", "krabboom,", "speelbal,", "schommelband"]))
    # the clothes corner: a rack with ranger hats, verrekijkers, the guh mannequins
    S.fill(GX1 + 14, G + 1, GZ2 - 3, GX1 + 14, G + 2, GZ2 - 3, "minecraft:birch_fence")
    S.fill(GX1 + 19, G + 1, GZ2 - 3, GX1 + 19, G + 2, GZ2 - 3, "minecraft:birch_fence")
    S.fill(GX1 + 14, G + 3, GZ2 - 3, GX1 + 19, G + 3, GZ2 - 3, "minecraft:stripped_birch_log[axis=x]")
    for x in range(GX1 + 15, GX1 + 19):
        S.set(x, G + 4, GZ2 - 3, "minecraft:brown_carpet")
    for x in (GX1 + 15, GX1 + 18):
        S.set(x, G + 1, GZ2 - 1, "minecraft:birch_planks")
        S.set(x, G + 2, GZ2 - 1, "knuffel_normal[facing=north]")
    S.set(GX1 + 21, G + 1, GZ2 - 1, "minecraft:birch_sign[rotation=8,waterlogged=false]")
    BORDJES.append(((GX1 + 21, G + 1, GZ2 - 1), ["Safarikleding!", "hoedjes, vestjes", "en verrekijkers", "njeg, zo stoer"]))
    # the counter (the seller stands behind it at x 87, z 121)
    for z in range(118, 125):
        S.set(GX2 - 4, G + 1, z, "minecraft:pink_terracotta")
    S.set(GX2 - 4, G + 2, 118, "klusjes_guhlampje[facing=west,lit=true]")
    S.set(GX2 - 4, G + 2, 120, "knuffel_golden[facing=west]")
    S.set(GX2 - 4, G + 2, 123, "minecraft:decorated_pot[cracked=false,facing=west,waterlogged=false]")
    for (tx, tz) in ((GX1 + 8, 124), (GX1 + 8, 130), (GX1 + 13, 124), (GX1 + 13, 130)):
        S.set(tx, G + 1, tz, "guh_tafel[facing=north]")
        S.set(tx, G + 2, tz, f"{KNUFFELS[(tx + tz) % len(KNUFFELS)]}[facing=east]")
    tapijt(GX1 + 5, 121, GX2 - 6, 133, G + 1, "pink", rand="yellow")
    for (x, z) in ((78, 127), (72, 122), (84, 132)):                       # (test round 4) light in the middle
        S.set(x, G + 8, z, "glimtegel[fel=true]")
    for (hx, hz) in ((GX1 + 5, 120), (GX1 + 10, 127), (GX1 + 17, 120), (GX1 + 5, 134), (GX1 + 17, 134)):
        hang(hx, hz, G + 1)
    claim(GX1, GZ1, GX2 + 2, GZ2)

    # ================================================================ IJSKRAAM (between the giftshop and the speeltuin)
    IX, IZ = 60, 141                                    # the counter's west side; the kiosk runs to IX+3, IZ+4
    for (a, b) in ((0, 0), (0, 4), (3, 0), (3, 4)):
        S.fill(IX + a, G + 1, IZ + b, IX + a, G + 3, IZ + b, "minecraft:birch_fence")
    for b in (1, 2, 3):
        S.set(IX, G + 1, IZ + b, "minecraft:light_blue_concrete")
        S.set(IX + 3, G + 1, IZ + b, "minecraft:white_concrete")
        S.set(IX + 3, G + 2, IZ + b, "minecraft:light_blue_stained_glass")
    S.set(IX, G + 2, IZ + 1, "minecraft:cake[bites=0]")
    S.set(IX, G + 2, IZ + 3, "knuffel_snow[facing=west]")
    for a in range(-1, 5):
        for b in range(-1, 6):
            S.set(IX + a, G + 4, IZ + b, "minecraft:pink_wool" if (a + 10) % 2 else "minecraft:white_wool")
    bordje(IX - 1, G + 1, IZ + 5, 4, ["IJskraam", "Pandaijsje,", "Zebrastreep ...", "3 Safarimunten"])
    claim(IX - 1, IZ - 1, IX + 4, IZ + 5, m=0)
    ijs = dict(verkoper=(IX + 2, IZ + 2), kijk=(IX - 2, IZ + 2))

    # ================================================================ RANGERSTATION (museum) east of the square
    RX1, RZ1, RX2, RZ2 = 156, 112, 184, 138
    DZ = 125                                            # the doors (west wall, z 125..126)
    box(RX1, G + 1, RZ1, RX2, G + 8, RZ2, "minecraft:birch_planks", floor="minecraft:birch_planks")
    S.fill(RX1, G + 1, RZ1, RX2, G + 1, RZ2, "minecraft:stripped_birch_log[axis=x]")
    S.air(RX1 + 1, G + 1, RZ1 + 1, RX2 - 1, G + 1, RZ2 - 1)
    for (x, z) in ((RX1, RZ1), (RX2, RZ1), (RX1, RZ2), (RX2, RZ2)):
        S.fill(x, G + 1, z, x, G + 8, z, LOG)
    for wy in (G + 3, G + 4):
        ramen(RX1, wy, RZ1, RX2, RZ2, every=3, vrij=[(RX1, DZ), (RX1, DZ + 1)])
    dubbeldeur(RX1, G + 1, DZ, "west", "z")
    dak(RX1, RZ1, RX2, RZ2, G + 9)
    muurbord(RX1 - 1, G + 4, DZ - 2, "west", ["Rangerstation", "Guhmbo's mini-", "natuurmuseum", "welkom!"])
    for z in (DZ - 4, DZ + 5):                                               # flowers + guh lanterns at the front
        S.set(RX1 - 1, G + 1, z, "guh_bloempot")
        S.fill(RX1 - 2, G + 1, z + (1 if z > DZ else -1), RX1 - 2, G + 2, z + (1 if z > DZ else -1), "minecraft:dark_oak_fence")
        S.set(RX1 - 2, G + 3, z + (1 if z > DZ else -1), "park_lantaarn")
    # the lookout tower in the north-east corner (decor: a stack with a thatched cap)
    S.fill(RX2 - 2, G + 1, RZ1, RX2, G + 18, RZ1 + 2, "minecraft:spruce_planks")
    thatch(RX2 - 2, RZ1, RX2, RZ1 + 2, G + 19, overhang=1)
    S.set(RX2 - 1, G + 15, RZ1 - 1, "minecraft:spruce_wall_sign[facing=north,waterlogged=false]")
    BORDJES.append(((RX2 - 1, G + 15, RZ1 - 1), ["", "Uitkijk", "Vadse Bergen", ""]))

    def vitrine(cx, cz, obj):
        for a in (-1, 0, 1):
            for b in (-1, 0, 1):
                S.set(cx + a, G + 1, cz + b, slab("minecraft:birch_slab", "top") if (a or b) else "minecraft:polished_diorite")
                S.set(cx + a, G + 2, cz + b, "minecraft:glass_pane" if (a or b) else obj)
                S.set(cx + a, G + 3, cz + b, "minecraft:glass")
    for k, (vx, obj) in enumerate(((RX1 + 6, "fossielmijn_fossielbeeldje[facing=south]"), (RX1 + 11, "guhrio_guhshi_ei"),
                                   (RX1 + 16, "knuffel_brontosaurus[facing=south]"), (RX1 + 21, "knuffel_pinguh[facing=south]"))):
        vitrine(vx, RZ1 + 6, obj)
    for k, x in enumerate(range(RX1 + 5, RX2 - 3, 2)):                       # plushies on a low shelf (south wall)
        S.set(x, G + 1, RZ2 - 1, slab("minecraft:birch_slab", "top"))
        S.set(x, G + 2, RZ2 - 1, f"{KNUFFELS[(k + 3) % len(KNUFFELS)]}[facing=north]")
    S.fill(RX1 + 4, G + 1, RZ2 - 5, RX1 + 4, G + 1, RZ2 - 3, "minecraft:bone_block[axis=y]")   # a little skeleton
    S.set(RX1 + 4, G + 2, RZ2 - 4, "minecraft:bone_block[axis=x]")
    MAP = ["EEEEiiiBBR", "EAAAERRRDD", "AA~~~~~~~D"]                       # the park map on the north wall
    COL = {"i": "minecraft:light_gray_concrete", "B": "minecraft:lime_concrete", "E": "minecraft:green_concrete",
           "R": "minecraft:green_terracotta", "D": "minecraft:cyan_terracotta", "A": "minecraft:orange_terracotta",
           "~": "minecraft:light_blue_concrete"}
    for r, row in enumerate(MAP):
        for c, ch in enumerate(row):
            S.set(RX1 + 8 + c, G + 7 - r, RZ1 + 1, COL[ch])
    for z in range(RZ1 + 10, RZ1 + 17):                                     # the dex wall (east): little frames
        for y in (G + 3, G + 4, G + 5):
            S.set(RX2 - 1, y, z, rnd.choice(["minecraft:yellow_terracotta", "minecraft:orange_terracotta",
                                             "minecraft:pink_terracotta", "minecraft:light_blue_terracotta",
                                             "minecraft:lime_terracotta"]))
    # the hoofdranger's office corner (south-east): desk, chair, lamp; the ranger stands at (RX2 - 5, RZ2 - 6)
    S.set(RX2 - 3, G + 1, RZ2 - 6, "guhkantoor_bureautje[facing=west,bezet=false]")
    S.set(RX2 - 2, G + 1, RZ2 - 6, "guh_stoel[facing=west]")
    S.set(RX2 - 3, G + 1, RZ2 - 8, "klusjes_guhlampje[facing=west,lit=true]")
    for z in (RZ2 - 2, RZ2 - 3):                                             # coat rack with ranger hats
        S.fill(RX2 - 2, G + 1, z, RX2 - 2, G + 2, z, "minecraft:birch_fence")
        S.set(RX2 - 2, G + 3, z, "minecraft:brown_carpet")
    # the reading corner (south-west): bookshelves, guh armchairs, cushions
    for x in range(RX1 + 1, RX1 + 4):
        S.fill(x, G + 1, RZ2 - 1, x, G + 2, RZ2 - 1, "minecraft:bookshelf")
    S.set(RX1 + 2, G + 1, RZ2 - 4, "bieb_guhfauteuil[facing=south]")
    S.set(RX1 + 6, G + 1, RZ2 - 4, "bieb_guhfauteuil[facing=south]")
    S.set(RX1 + 4, G + 1, RZ2 - 3, "guh_tafel[facing=north]")
    S.set(RX1 + 3, G + 1, RZ2 - 2, "pink_kussen[facing=north]")
    tapijt(RX1 + 1, RZ2 - 6, RX1 + 7, RZ2 - 2, G + 1, "yellow", rand="orange")
    tapijt(RX1 + 9, RZ1 + 10, RX2 - 7, RZ2 - 4, G + 1, "light_blue", rand="white")
    for (x, z) in ((RX1 + 1, RZ1 + 1), (RX2 - 1, RZ1 + 1), (RX1 + 1, RZ1 + 9), (RX2 - 1, RZ2 - 10)):
        S.set(x, G + 1, z, "guh_bloempot")
    for (hx, hz) in ((RX1 + 6, RZ1 + 10), (RX1 + 14, RZ1 + 4), (RX1 + 20, RZ1 + 12), (RX1 + 14, RZ2 - 5),
                     (RX2 - 5, RZ2 - 4), (RX1 + 4, RZ2 - 5)):
        hang(hx, hz, G + 1)
    for (x, z) in ((RX1 + 14, RZ1 + 13), (RX1 + 8, RZ1 + 18)):
        S.set(x, G + 9, z, "glimtegel[fel=true]")
    claim(RX1 - 2, RZ1, RX2, RZ2)

    # ================================================================ AAIHOEK (east): stable + 4 pens
    BX1, BZ1, BX2, BZ2 = 196, 110, 220, 124
    box(BX1, G + 1, BZ1, BX2, G + 6, BZ2, "minecraft:red_terracotta", floor="minecraft:birch_planks")
    for x in range(BX1, BX2 + 1):
        S.set(x, G + 6, BZ1, "minecraft:white_terracotta"); S.set(x, G + 6, BZ2, "minecraft:white_terracotta")
    ramen(BX1, G + 3, BZ1, BX2, BZ2, every=4, vrij=[(BX1, 116), (BX1, 117)])
    dubbeldeur(BX1, G + 1, 116, "west", "z")
    dak(BX1, BZ1, BX2, BZ2, G + 7)
    S.set(BX1 - 1, G + 5, 117, "knuffelsteen_gezicht[facing=west,stemming=0]")
    muurbord(BX1 - 1, G + 3, 114, "west", ["Aaihoek", "Stal van", "Knuffelien", "aaien mag!"])
    for z in range(BZ1 + 1, BZ1 + 6):                                       # hay, brushes, a bench, straw, lanterns
        S.fill(BX2 - 1, G + 1, z, BX2 - 1, G + 2, z, HAY)
    S.set(BX2 - 1, G + 3, BZ1 + 2, "reisbureau_souvenir_knuffeldal[facing=west]")
    S.set(BX2 - 1, G + 1, BZ2 - 1, "minecraft:barrel[facing=up,open=false]")
    S.set(BX2 - 1, G + 1, BZ2 - 2, "minecraft:barrel[facing=up,open=false]")
    S.set(BX2 - 1, G + 1, BZ1 + 8, "guh_kast[facing=west,open=false]")
    S.set(BX1 + 3, G + 1, BZ1 + 1, "park_bankje[facing=south]")
    S.set(BX1 + 5, G + 1, BZ1 + 1, "park_bankje[facing=south]")
    S.set(BX1 + 9, G + 1, BZ1 + 1, "minecraft:composter[level=7]")
    for x in range(BX1 + 12, BX2 - 2):                                      # a straw corner with plushies to cuddle
        S.set(x, G + 1, BZ2 - 1, HAY)
    for x in range(BX1 + 12, BX2 - 2, 2):
        S.set(x, G + 2, BZ2 - 1, f"{KNUFFELS[x % len(KNUFFELS)]}[facing=north]")
    S.set(BX1 + 2, G + 1, BZ2 - 1, "minecraft:water_cauldron[level=3]")
    S.set(BX1 + 4, G + 1, BZ2 - 1, "guh_bloempot")
    tapijt(BX1 + 3, BZ1 + 4, BX2 - 4, BZ2 - 3, G + 1, "yellow", rand="orange")
    for (hx, hz) in ((BX1 + 5, BZ1 + 5), (BX1 + 12, BZ1 + 8), (BX1 + 19, BZ1 + 5), (BX1 + 8, BZ2 - 3)):
        hang(hx, hz, G + 1, "minecraft:lantern[hanging=true,waterlogged=false]")
    claim(BX1 - 1, BZ1, BX2, BZ2)
    for (x0, z0, fn) in PENS:
        for x in range(x0, x0 + 8):
            for z in range(z0, z0 + 9):
                S.set(x, G, z, "minecraft:coarse_dirt" if (x + z) % 3 else GRASS)
                S.air(x, G + 1, z, x, G + 3, z)
                if x in (x0, x0 + 7) or z in (z0, z0 + 8):
                    S.fill(x, G + 1, z, x, G + 2, z, "minecraft:oak_fence")
        S.set(x0 + 3, G + 1, z0 + 8, "minecraft:oak_fence_gate[facing=south,in_wall=false,open=false,powered=false]")
        S.set(x0 + 3, G + 2, z0 + 8, AIR)
        S.set(x0 + 6, G + 1, z0 + 1, HAY)
        S.set(x0 + 1, G + 1, z0 + 1, "minecraft:water_cauldron[level=3]")
        claim(x0, z0, x0 + 7, z0 + 8, m=0)
    bordje(192, G + 1, 136, 4, ["Aaihoek", "geitje, alpaca,", "cavia, ezeltje", "zachtjes aaien!"])
    OCC.add((192, 136))
    ALPACA = [(198, 145), (210, 143), (224, 145), (226, 149), (212, 151), (198, 149)]
    for (ax, az), (bx, bz) in zip(ALPACA, ALPACA[1:] + ALPACA[:1]):
        n = int(max(abs(bx - ax), abs(bz - az)))
        for k in range(n + 1):
            x, z = round(ax + (bx - ax) * k / n), round(az + (bz - az) * k / n)
            for (a, b) in ((0, 0), (0, 1)):
                if (x + a, z + b) not in PATHS:
                    S.set(x + a, G, z + b, "minecraft:dirt_path" if (x + z) % 3 else "minecraft:coarse_dirt")
                    S.air(x + a, G + 1, z + b, x + a, G + 4, z + b)
                    OCC.add((x + a, z + b))

    # ================================================================ EXPERTENPAVILJOEN (round, 9 counters)
    PVX, PVZ, PVR = 212, 172, 8.0
    EXPERTS = ["minecraft:yellow_wool", "minecraft:cyan_wool", "minecraft:green_wool", "minecraft:lime_wool",
               "minecraft:light_blue_wool", "minecraft:light_gray_wool", "minecraft:green_terracotta",
               "minecraft:orange_wool", "minecraft:pink_wool"]
    N_EXP = len(EXPERT_ZONES)
    for x in range(int(PVX - PVR) - 2, int(PVX + PVR) + 3):
        for z in range(int(PVZ - PVR) - 2, int(PVZ + PVR) + 3):
            d = math.hypot(x - PVX, z - PVZ)
            if d <= PVR + 0.5:
                S.set(x, G, z, "minecraft:spruce_planks" if d <= PVR - 0.5 else "minecraft:stripped_spruce_log[axis=x]")
                S.air(x, G + 1, z, x, G + 9, z)
    S.set(PVX, G, PVZ, "minecraft:chiseled_sandstone")
    pv_balies = []
    for k in range(N_EXP):
        a = k / N_EXP * 2 * math.pi + math.pi / N_EXP
        px, pz = int(round(PVX + math.cos(a) * PVR)), int(round(PVZ + math.sin(a) * PVR))
        if abs(px - (PVX - PVR)) < 1.5 and abs(pz - PVZ) < 2:
            continue                                                         # (no post in the west entrance)
        S.fill(px, G + 1, pz, px, G + 7, pz, LOG)
        S.set(px, G + 5, pz, EXPERTS[k % len(EXPERTS)])
    for k in range(N_EXP):
        b = k / N_EXP * 2 * math.pi
        bx, bz = int(round(PVX + math.cos(b) * (PVR - 1.0))), int(round(PVZ + math.sin(b) * (PVR - 1.0)))
        if bx < PVX - PVR + 2.5 and abs(bz - PVZ) <= 1:
            b += 0.35
            bx, bz = int(round(PVX + math.cos(b) * (PVR - 1.0))), int(round(PVZ + math.sin(b) * (PVR - 1.0)))
        S.set(bx, G + 1, bz, "minecraft:barrel[facing=up,open=false]")
        wool = EXPERTS[k % len(EXPERTS)]
        S.set(bx, G + 2, bz, wool.replace("_wool", "_carpet").replace("green_terracotta", "green_carpet"))
        pv_balies.append((bx, bz, b))
    for x in range(int(PVX - PVR) - 1, int(PVX + PVR) + 2):
        for z in range(int(PVZ - PVR) - 1, int(PVZ + PVR) + 2):
            d = math.hypot(x - PVX, z - PVZ)
            if PVR - 0.5 < d <= PVR + 0.5 and S.is_air(x, G + 8, z):
                S.set(x, G + 8, z, slab("minecraft:spruce_slab"))
    thatch_round(PVX, PVZ, PVR - 1.5, G + 9)
    for x in range(PVX - 3, PVX + 4):
        for z in range(PVZ - 3, PVZ + 4):
            d = math.hypot(x - PVX, z - PVZ)
            if d <= 2.4 and S.is_air(x, G + 1, z):
                S.set(x, G + 1, z, "minecraft:yellow_carpet" if d > 1.4 else "minecraft:white_carpet")
    S.set(PVX, G + 1, PVZ, "guh_bloempot")
    hang(PVX, PVZ, G + 1, "lampion_geel[hanging=true]", tot=14)
    bordje(int(PVX - PVR) - 2, G + 1, PVZ - 3, 4, ["Experten-", "paviljoen", "9 experts:", "vraag maar!"])
    claim(int(PVX - PVR) - 2, int(PVZ - PVR) - 1, int(PVX + PVR) + 1, int(PVZ + PVR) + 1, m=1)

    # ================================================================ LODGES on stilts at the north shore of the pond
    HUTS = [(12, 6), (24, 4), (36, 4), (48, 6), (60, 8)]
    bedden = []
    for (hx, hz) in HUTS:
        FY = G + 3
        for (a, b) in ((0, 0), (5, 0), (0, 5), (5, 5)):
            S.fill(hx + a, G + 1, hz + b, hx + a, FY - 1, hz + b, LOG)
        S.fill(hx - 1, FY, hz - 1, hx + 6, FY, hz + 6, "minecraft:birch_planks")
        box(hx, FY + 1, hz, hx + 5, FY + 3, hz + 5, "minecraft:stripped_birch_log[axis=y]", corner=LOG)
        for (a, b) in ((0, 2), (0, 3), (5, 2), (5, 3), (2, 0), (3, 0)):
            S.set(hx + a, FY + 2, hz + b, "minecraft:glass_pane")
        deur(hx + 2, FY + 1, hz + 5, "south")
        dak(hx, hz, hx + 5, hz + 5, FY + 4)
        for a in range(-1, 7):                                                # railing round the platform
            for b in range(-1, 7):
                if (a in (-1, 6) or b in (-1, 6)) and not (b == 6 and a in (1, 2, 3)):
                    S.set(hx + a, FY + 1, hz + b, "minecraft:spruce_fence")
        # two steps down from the platform to the shore path (stairs on solid planks)
        S.set(hx + 2, FY - 1, hz + 7, "minecraft:birch_planks")
        for xs in (hx + 1, hx + 2, hx + 3):
            S.set(xs, FY - 1, hz + 7, stairs("minecraft:birch_stairs", "north"))
            S.fill(xs, G + 1, hz + 7, xs, FY - 2, hz + 7, "minecraft:birch_planks")
            S.set(xs, FY - 2, hz + 8, stairs("minecraft:birch_stairs", "north"))
            S.air(xs, FY, hz + 7, xs, FY + 3, hz + 7)
            S.air(xs, FY - 1, hz + 8, xs, FY + 3, hz + 8)
        for z in range(hz + 9, 24):
            pad(hx + 1, z, hx + 3, z)
        S.set(hx + 6, FY + 2, hz + 6, "park_lantaarn")
        # inside: a bed (along the west wall, head north), a rug, a lamp, a cushion, a plant
        fy = FY + 1
        S.set(hx + 1, fy, hz + 1, "minecraft:yellow_bed[facing=north,occupied=false,part=head]")
        S.set(hx + 1, fy, hz + 2, "minecraft:yellow_bed[facing=north,occupied=false,part=foot]")
        bedden.append((hx + 1, fy, hz + 1))
        for x in range(hx + 2, hx + 5):
            for z in range(hz + 1, hz + 4):
                S.set(x, fy, z, "minecraft:light_blue_carpet")
        S.set(hx + 4, fy, hz + 4, "klusjes_guhlampje[facing=west,lit=true]")
        S.set(hx + 1, fy, hz + 4, "pink_kussen[facing=east]")
        S.set(hx + 4, fy, hz + 1, "guh_bloempot")
        claim(hx - 1, hz - 1, hx + 6, hz + 8, m=0)
    bordje(8, G + 1, 27, 0, ["Lodges", "slapen in het", "park: vroege", "vogels winnen!"])
    OCC.add((8, 27))

    # ================================================================ KAMPVUUR (between the aai-lagune and the speeltuin)
    kx, kz = 46, 121
    for a in range(-4, 5):
        for b in range(-4, 5):
            if math.hypot(a, b) <= 3.6:
                S.set(kx + a, G, kz + b, "minecraft:coarse_dirt" if math.hypot(a, b) > 1.5 else "minecraft:gravel")
                S.air(kx + a, G + 1, kz + b, kx + a, G + 4, kz + b)
    S.set(kx, G + 1, kz, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
    for b in (-1, 0, 1):
        S.set(kx + b, G + 1, kz - 3, "minecraft:stripped_oak_log[axis=x]")
        S.set(kx + b, G + 1, kz + 3, "minecraft:stripped_oak_log[axis=x]")
        S.set(kx - 3, G + 1, kz + b, "minecraft:stripped_oak_log[axis=z]")
    S.set(kx - 2, G + 1, kz + 2, "reisbureau_zeldzaam_camping[facing=north]")
    for (x, z) in ((kx + 4, kz - 4), (kx - 4, kz + 4)):
        S.fill(x, G + 1, z, x, G + 2, z, "minecraft:birch_fence")
        S.set(x, G + 3, z, "park_lantaarn")
    claim(kx - 4, kz - 4, kx + 4, kz + 4, m=0)
    kampvuur = dict(centrum=(kx, kz), verteller=(kx + 3, kz), kijk=(kx, kz))

    # ================================================================ GUH-SPEELTUIN (south-west, big)
    SX1, SZ1, SX2, SZ2 = 8, 128, 52, 176
    for x in range(SX1, SX2 + 1):
        for z in range(SZ1, SZ2 + 1):
            rand = x in (SX1, SX2) or z in (SZ1, SZ2)
            n = vnoise(x / 5.0, z / 5.0, 17)
            S.set(x, G, z, "minecraft:smooth_sandstone" if rand else ("minecraft:sand" if n > 0.42 else GRASS))
            S.air(x, G + 1, z, x, G + 6, z)
            if rand and not (SX2 - 0 == x and 149 <= z <= 152) and not (z == SZ1 and 29 <= x <= 33):
                S.set(x, G + 1, z, "minecraft:birch_fence")
    for (x, z) in ((SX1, SZ1), (SX2, SZ1), (SX1, SZ2), (SX2, SZ2), (SX2, 148), (SX2, 153), (28, SZ1), (34, SZ1)):
        S.fill(x, G + 1, z, x, G + 2, z, "minecraft:birch_fence")
        S.set(x, G + 3, z, "park_lantaarn")
    pad(SX2 - 1, 149, SX2 + 1, 152)
    pad(29, SZ1 - 1, 33, SZ1 + 1)
    bordje(SX2 + 1, G + 1, 147, 4, ["", "Guh-speeltuin", "voor guhs en", "vadsjes"])
    # the toys the game places (multi-blocks with a block entity, beleving.json) and their free ground
    speelgoed = [dict(soort="schommel", pos=(15, G + 1, 136), facing="south"),
                 dict(soort="glijbaantje", pos=(27, G + 1, 136), facing="south"),
                 dict(soort="schommel", pos=(41, G + 1, 136), facing="south"),
                 dict(soort="wip", pos=(16, G + 1, 166), facing="east"),
                 dict(soort="wip", pos=(30, G + 1, 168), facing="east"),
                 dict(soort="knabbelbal", pos=(30, G + 1, 152), facing="north"),
                 # round 5: more to play with (the guhs play there more often: SAFARI_CONTRACT "Testronde 5")
                 dict(soort="glijbaantje", pos=(37, G + 1, 162), facing="south"),
                 dict(soort="schommel", pos=(28, G + 1, 160), facing="south"),
                 dict(soort="wip", pos=(42, G + 1, 151), facing="east"),
                 dict(soort="knabbelbal", pos=(20, G + 1, 166), facing="north"),
                 dict(soort="knabbelbal", pos=(38, G + 1, 146), facing="north")]
    # the pluizige tunnel (in the tiles: plain blocks that join by their sides): an L through the middle
    TUN = [(x, 146) for x in range(14, 25)] + [(24, z) for z in range(147, 157)]
    tun = set(TUN)
    for (x, z) in TUN:
        kant = {}
        for d, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("west", (-1, 0)), ("east", (1, 0))):
            kant[d] = "open" if (x + dx, z + dz) in tun else "dicht"
        if (x, z) == TUN[0]:
            kant["west"] = "ingang"
        if (x, z) == TUN[-1]:
            kant["south"] = "ingang"
        as_ = "x" if kant["east"] == "open" or kant["west"] != "dicht" else "z"
        S.set(x, G + 1, z, f"pluizige_tunnel[axis={as_},east={kant['east']},north={kant['north']},"
                           f"south={kant['south']},west={kant['west']}]")
    # bubble blowers, wind scooters, the hapluikje, a sandbox with a little castle, benches
    for (x, z, f) in ((44, 146, "west"), (12, 174, "north")):
        S.set(x, G + 1, z, f"bellenblaas[facing={f}]")
    for (x, z) in ((44, 158), (38, 172)):
        S.set(x, G + 1, z, "luchtscooter")
    S.set(48, G + 1, 166, "hapluikje[facing=west,snoet=slaapt]")
    S.set(20, G + 1, 140, "hapluikje[facing=south,snoet=slaapt]")                  # round 5: a few more plain toys
    S.set(22, G + 1, 172, "bellenblaas[facing=north]")
    S.set(33, G + 1, 148, "luchtscooter")
    # round 5 (user): the safari animal toys too, now that the guhs play with them (FX-B Speeltuin): each on a free
    # 3x3 spot near its wish spot, so it never lands on another toy, a path or the tunnel
    bezet = {(x, z) for d in speelgoed for x in range(d["pos"][0] - 2, d["pos"][0] + 3) for z in range(d["pos"][2] - 2, d["pos"][2] + 3)}
    for naam, (wx, wz) in (("safari_speelbal", (46, 140)), ("safari_schommelband", (14, 132)),
                           ("safari_krabboom", (46, 170)), ("safari_glijbaantje", (20, 160))):
        for r in range(0, 7):
            plek = next(((x, z) for x in range(wx - r, wx + r + 1) for z in range(wz - r, wz + r + 1)
                         if SX1 + 2 <= x <= SX2 - 2 and SZ1 + 2 <= z <= SZ2 - 2
                         and all(S.is_air(x + dx, G + 1, z + dz) and (x + dx, z + dz) not in bezet
                                 for dx in (-1, 0, 1) for dz in (-1, 0, 1))), None)
            if plek:
                S.set(plek[0], G + 1, plek[1], naam)
                bezet |= {(plek[0] + dx, plek[1] + dz) for dx in range(-1, 2) for dz in range(-1, 2)}
                break
    for x in range(10, 16):
        for z in range(152, 158):
            S.set(x, G, z, "minecraft:sand")
            if x in (10, 15) or z in (152, 157):
                S.set(x, G + 1, z, slab("minecraft:spruce_slab"))
    S.set(12, G + 1, 154, "minecraft:sandstone")
    S.set(13, G + 1, 155, "minecraft:sandstone")
    S.set(12, G + 2, 154, "minecraft:sandstone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]")
    for z in range(156, 171, 5):
        S.set(SX2 - 1, G + 1, z, "park_bankje[facing=west]")
    for x in range(36, 48, 5):
        S.set(x, G + 1, SZ2 - 1, "park_bankje[facing=north]")
    for (x, z) in ((SX1 + 1, SZ1 + 1), (SX2 - 1, SZ1 + 1), (SX1 + 1, SZ2 - 1)):
        S.set(x, G + 1, z, "guh_bloempot")
    claim(SX1, SZ1, SX2, SZ2, m=1)
    speeltuin = dict(centrum=((SX1 + SX2) // 2, (SZ1 + SZ2) // 2), min=(SX1, SZ1), max=(SX2, SZ2), ingang=(SX2, 150),
                     speelgoed=speelgoed)

    def entree_binnen(EX1, EZ1, EX2, EZ2, MIDX):
        """Round 5: the hall was dark and bare. Lampions under the ceiling, glow tiles along the walls, stone guh
        lanterns, a cafe corner with guh chairs and tables, guh fences as a queue at the counter, cushions, plants."""
        for x in range(EX1 + 3, EX2 - 2, 5):
            for z in range(EZ1 + 3, EZ2 - 2, 5):
                if abs(x - MIDX) > 4 and S.is_air(x, G + 10, z) and not S.is_air(x, G + 11, z):
                    S.set(x, G + 10, z, LAMPION[(x // 5 + z // 5) % 3])
        for x in range(EX1 + 2, EX2 - 1, 4):                               # glow tiles in the floor along both walls
            for z in (EZ1 + 1, EZ2 - 1):
                if S.get(x, G, z) != AIR and S.is_air(x, G + 1, z):
                    S.set(x, G, z, "glimtegel[fel=true]")
        for z in range(EZ1 + 2, EZ2 - 1, 4):
            for x in (EX1 + 1, EX2 - 1):
                if S.get(x, G, z) != AIR and S.is_air(x, G + 1, z):
                    S.set(x, G, z, "glimtegel[fel=true]")
        for (x, z) in ((MIDX - 5, EZ1 + 2), (MIDX + 5, EZ1 + 2), (MIDX - 5, EZ2 - 2), (MIDX + 5, EZ2 - 2)):
            S.set(x, G + 1, z, "toro[lit=true]")
        # the cafe corner (south-west): guh tables with chairs round them, a cushion bench and plants
        for (tx, tz) in ((90, 163), (96, 163), (102, 163), (90, 168), (96, 168), (102, 168)):
            if not (S.is_air(tx, G + 1, tz) and S.is_air(tx, G + 1, tz - 1) and S.is_air(tx, G + 1, tz + 1)):
                continue
            S.set(tx, G + 1, tz, "guh_tafel[facing=north]")
            S.set(tx - 1, G + 1, tz, "guh_stoel[facing=east]")
            S.set(tx + 1, G + 1, tz, "guh_stoel[facing=west]")
            S.set(tx, G + 2, tz, "doolhof_lantaarn[lit=true]" if (tx + tz) % 2 else "guh_bloempot")
        for x in range(87, 106, 3):
            if S.is_air(x, G + 1, EZ2 - 1):
                S.set(x, G + 1, EZ2 - 1, "pink_kussen[facing=north]")
        # a queue of guh fences in front of the counter (x 92..110, z 156..157), open at both ends
        for x in range(93, 110):
            if S.is_air(x, G + 1, 160):
                S.set(x, G + 1, 160, "guh_hekje_mint")
        # plushies and plants on the counter, cloud lamps, more flower pots along the walls
        for (x, k) in ((93, "knuffel_normal"), (98, "knuffel_pinguh"), (104, "knuffel_wolk"), (109, "knuffel_mint")):
            if S.is_air(x, G + 3, 156):
                S.set(x, G + 3, 156, f"{k}[facing=south]")
        for (x, z) in ((EX1 + 2, EZ1 + 2), (EX2 - 2, EZ1 + 2), (EX1 + 2, EZ2 - 3), (EX2 - 2, EZ2 - 3)):
            if S.is_air(x, G + 1, z):
                S.set(x, G + 1, z, "wolkenlamp")
        for x in range(EX1 + 4, EX2 - 3, 6):
            for z in (EZ1 + 1, EZ2 - 1):
                if S.is_air(x, G + 1, z) and abs(x - MIDX) > 4:
                    S.set(x, G + 1, z, "guh_bloempot")

    # ================================================================ ENTREEGEBOUW (thatched lodge, welkomsthal)
    EX1, EZ1, EX2, EZ2 = 84, 152, 160, 178
    MIDX = CX
    box(EX1, G + 1, EZ1, EX2, G + 10, EZ2, "minecraft:stripped_birch_log[axis=y]", floor="minecraft:birch_planks")
    S.fill(EX1, G + 1, EZ1, EX2, G + 1, EZ2, "minecraft:sandstone")
    S.air(EX1 + 1, G + 1, EZ1 + 1, EX2 - 1, G + 1, EZ2 - 1)
    for z in (EZ1, EZ2):
        for x in range(EX1 + 2, EX2 - 1):
            if (x - EX1) % 5 and abs(x - MIDX) > 6:
                S.fill(x, G + 3, z, x, G + 8, z, "minecraft:glass_pane")
    for x in (EX1, EX2):
        for z in range(EZ1 + 3, EZ2 - 2, 4):
            S.fill(x, G + 3, z, x, G + 7, z, "minecraft:glass_pane")
    for x in range(EX1, EX2 + 1, 6):
        S.fill(x, G + 1, EZ1, x, G + 10, EZ1, LOG); S.fill(x, G + 1, EZ2, x, G + 10, EZ2, LOG)
    for z in (EZ1, EZ2):                                       # the passage: big open gate front + back (7 wide)
        S.air(MIDX - 3, G + 1, z, MIDX + 3, G + 7, z)
    top = dak(EX1, EZ1, EX2, EZ2, G + 11, overhang=2)
    for ex in (102, 140):
        S.fill(ex, top - 1, 163, ex + 2, top + 2, 167, "minecraft:pink_wool")
    pad(MIDX - 3, EZ1, MIDX + 3, EZ2, pal=["minecraft:polished_diorite", "minecraft:birch_planks"])
    S.fill(92, G + 1, 156, 110, G + 2, 157, "minecraft:spruce_planks")                    # info / ticket counter
    S.fill(92, G + 3, 156, 110, G + 3, 156, slab("minecraft:birch_slab"))
    for x in (95, 101, 107):
        S.set(x, G + 3, 157, "klusjes_guhlampje[facing=south,lit=true]")
    # round 5: the "park van boven" model is gone; a kaartwand with real maps of the park (item frames, filled by the
    # game from data/guhs/safari/kaartwand.json, written by safari_park/kaartwand.py from the finished park)
    KW = dict(x0=138, y0=G + 8, z=158, n=6)                                  # frames x 138..143, rows G+8 (north) .. G+3
    S.fill(135, G + 1, KW["z"], 146, G + 9, KW["z"], "minecraft:dark_oak_planks")
    S.fill(135, G + 1, KW["z"], 135, G + 9, KW["z"], "minecraft:stripped_dark_oak_log[axis=y]")
    S.fill(146, G + 1, KW["z"], 146, G + 9, KW["z"], "minecraft:stripped_dark_oak_log[axis=y]")
    S.fill(135, G + 10, KW["z"], 146, G + 10, KW["z"], "minecraft:stripped_dark_oak_log[axis=x]")
    S.fill(136, G + 2, KW["z"], 145, G + 9, KW["z"], "minecraft:pink_terracotta")           # the passe-partout
    S.fill(137, G + 3, KW["z"], 144, G + 8, KW["z"], "minecraft:spruce_planks")
    S.set(140, G + 9, KW["z"] + 1, "minecraft:oak_wall_sign[facing=south,waterlogged=false]")
    S.set(141, G + 9, KW["z"] + 1, "minecraft:oak_wall_sign[facing=south,waterlogged=false]")
    BORDJES.append(((140, G + 9, KW["z"] + 1), ["", "Safaripark", "Vadse Bergen", ""]))
    BORDJES.append(((141, G + 9, KW["z"] + 1), ["", "zo ziet het", "park eruit!", ""]))
    for x in range(KW["x0"], KW["x0"] + KW["n"]):                           # the column signs (filled in below)
        S.set(x, G + 2, KW["z"] + 1, "minecraft:birch_wall_sign[facing=south,waterlogged=false]")
    for (x, z) in ((134, 160), (147, 160)):
        S.set(x, G + 1, z, "toro[lit=true]")
    for x in range(134, 148):                                                # a little rail to look from
        if x not in (140, 141):
            S.set(x, G + 1, 163, "guh_hekje_roze")
    KAARTWAND.clear()
    KAARTWAND.update(KW)
    S.fill(88, G + 3, 177, 98, G + 6, 177, "minecraft:yellow_concrete")                    # the timetable board
    S.fill(89, G + 4, 177, 97, G + 5, 177, "minecraft:green_concrete")
    for x in range(146, 158, 3):                                                             # souvenir corner
        S.set(x, G + 1, 176, "guh_kast[facing=north,open=false]")
        S.set(x, G + 2, 176, "knuffel_choco[facing=north]")
    for x in range(90, 155, 9):
        if abs(x - MIDX) > 5 and S.is_air(x, G + 1, 170) and S.is_air(x + 2, G + 1, 170):
            S.set(x, G + 1, 170, "park_bankje[facing=north]")
            S.set(x + 2, G + 1, 170, "guh_bloempot")
    for x in range(90, 158, 8):
        S.set(x, G + 10, 165, "guh_kristal_lamp")
    for (hx, hz) in ((96, 162), (110, 168), (134, 156), (150, 168)):
        hang(hx, hz, G + 1)
    entree_binnen(EX1, EZ1, EX2, EZ2, MIDX)
    claim(EX1, EZ1, EX2, EZ2, m=1)
    # the olifantenpoort in front of it (two elephants, their trunks meet over the path)
    GZ = 181

    def olifant_zij(xc, zc, f):
        bx = xc - 3 * f
        for (a, b) in ((-2, -2), (-2, 1), (1, -2), (1, 1)):
            S.fill(bx + a, G + 1, zc + b, bx + a + 1, G + 3, zc + b + 1, GRAY)
        S.ellipsoid(bx, G + 5.5, zc + 0.5, 4.2, 3.0, 3.0, GRAY)
        hx = xc + 1.5 * f
        S.ellipsoid(hx, G + 8.5, zc + 0.5, 2.8, 2.8, 2.8, GRAY)
        for side in (-1, 1):
            ez = zc + 0.5 + side * 3.4
            S.fill(int(hx - f), G + 7, int(round(ez)), int(hx + f), G + 11, int(round(ez)), "minecraft:pink_wool")
        fx = int(round(hx + 2.8 * f))
        S.set(fx, G + 9, zc - 1, "minecraft:black_concrete"); S.set(fx, G + 9, zc + 2, "minecraft:black_concrete")
        x0, x1 = fx, MIDX + (0 if f > 0 else 1)
        n = abs(x1 - x0)
        for i in range(n + 1):
            t = i / max(n, 1)
            y = int(round(G + 8 + 6 * math.sin(t * math.pi / 2)))
            for zz in (zc, zc + 1):
                S.set(x0 + f * i, y, zz, GRAY); S.set(x0 + f * i, y + 1, zz, GRAY)

    olifant_zij(110, GZ, 1)
    olifant_zij(134, GZ, -1)
    S.fill(MIDX - 2, G + 15, GZ, MIDX + 3, G + 16, GZ, "minecraft:yellow_concrete")
    claim(100, GZ - 4, 144, GZ + 4, m=0)
    pad(MIDX - 3, EZ2, MIDX + 3, 186)

    # ================================================================ BUSPLEIN + the Guh-safaribus
    pad(70, 186, 174, 212, pal=["minecraft:smooth_stone", "minecraft:smooth_stone", "minecraft:polished_andesite"])
    for z in range(186, 222):
        for x in range(114, 131):
            if (x, z) in FP:
                S.set(x, G, z, "minecraft:gray_concrete_powder" if x in (114, 130) else "minecraft:smooth_stone")
                S.air(x, G + 1, z, x, G + 6, z)
                PATHS.add((x, z)); OCC.add((x, z))
    for x in range(78, 104, 6):                                              # the bus stop roof + benches
        S.fill(x, G + 1, 191, x, G + 6, 191, LOG); S.fill(x, G + 1, 198, x, G + 6, 198, LOG)
    dak(76, 190, 104, 199, G + 7, plafond="minecraft:spruce_planks")
    for x in range(80, 102, 4):
        S.set(x, G + 1, 194, "park_bankje[facing=east]")
        S.set(x + 1, G + 1, 196, "guh_bloempot")
    S.fill(106, G + 1, 193, 106, G + 3, 193, "minecraft:spruce_fence")
    S.fill(106, G + 4, 192, 106, G + 5, 194, "minecraft:yellow_concrete")              # the bus stop sign
    S.set(105, G + 4, 193, "minecraft:spruce_wall_sign[facing=west,waterlogged=false]")
    BORDJES.append(((105, G + 4, 193), ["Bushalte", "Guh-safaribus", "naar de", "Guhmension"]))
    BEL = (106, G + 1, 196)                                                  # the bell: ring it, the bus takes you home
    S.set(BEL[0], BEL[1], BEL[2], "minecraft:bell[attachment=floor,facing=east,powered=false]")
    S.set(105, G + 1, 197, "minecraft:birch_sign[rotation=4,waterlogged=false]")
    BORDJES.append(((105, G + 1, 197), ["Naar huis?", "Luid de bel,", "de bus brengt", "je terug! Vahoeg"]))
    BX_1, BX_2, BZ_1, BZ_2 = 117, 127, 190, 207                              # the Guh-safaribus (waiting)
    for z in (BZ_1 + 2, BZ_2 - 2):
        for x in (BX_1, BX_2):
            S.set(x, G + 1, z, "minecraft:black_concrete")
    S.fill(BX_1, G + 2, BZ_1, BX_2, G + 6, BZ_2, "minecraft:yellow_terracotta")
    S.fill(BX_1, G + 3, BZ_1, BX_2, G + 3, BZ_2, "minecraft:green_terracotta")
    S.fill(BX_1 + 1, G + 7, BZ_1 + 1, BX_2 - 1, G + 7, BZ_2 - 1, "minecraft:yellow_terracotta")
    for z in range(BZ_1 + 1, BZ_2):
        for x in (BX_1, BX_2):
            S.set(x, G + 5, z, "minecraft:light_blue_stained_glass" if z % 3 else "minecraft:yellow_terracotta")
    S.fill(BX_1 + 1, G + 4, BZ_1, BX_2 - 1, G + 5, BZ_1, "minecraft:light_blue_stained_glass")
    for x in (BX_1 + 1, BX_2 - 1):
        S.fill(x, G + 8, BZ_1 + 1, x, G + 9, BZ_1 + 2, "minecraft:pink_wool")
    S.set(BX_1 - 1, G + 4, BZ_1 + 6, "minecraft:spruce_wall_sign[facing=west,waterlogged=false]")
    BORDJES.append(((BX_1 - 1, G + 4, BZ_1 + 6), ["Guh-safaribus", "klik: terug", "naar de", "Guhmension"]))
    for x in range(150, 172, 7):                                             # planters on the bus square
        S.fill(x, G + 1, 190, x + 3, G + 1, 192, "minecraft:spruce_planks")
        S.fill(x, G + 2, 190, x + 3, G + 2, 192, "minecraft:flowering_azalea")
    claim(70, 186, 174, 212, m=0)
    bus = dict(min=(BX_1, G + 1, BZ_1), max=(BX_2, G + 9, BZ_2), bel=BEL)

    # ================================================================ the oostpoort (on foot into the savanne)
    POORT_Z = (104, 106)
    for z in (POORT_Z[0] - 1, POORT_Z[1] + 1):
        S.fill(236, G + 1, z, 237, G + 7, z, POST)
        S.set(236, G + 8, z, "park_lantaarn")
    S.fill(236, G + 7, POORT_Z[0], 237, G + 7, POORT_Z[1], "minecraft:dark_oak_log[axis=z]")
    S.fill(237, G + 6, POORT_Z[0], 237, G + 6, POORT_Z[1], "minecraft:yellow_concrete")
    bordje(234, G + 1, POORT_Z[0] - 2, 12, ["Het park in", "(te voet)", "Savanne ->", "Vadse Bergen"])
    OCC.add((234, POORT_Z[0] - 2))

    # ================================================================ fence round the footprint
    gaten = set()
    for (x, z) in EDGE:
        if (x, z) in WEGCEL or (x, z) in HAVEN:
            gaten.add((x, z))
        if x >= 236 and POORT_Z[0] - 1 <= z <= POORT_Z[1] + 1:
            gaten.add((x, z))
    # the north and east road crossings: 6 cells free for the safaripoort of zone_s3_rit (4 wide + its 2 posts)
    for (x, z) in EDGE:
        if z <= 3 and WEG_X - 3 <= x <= WEG_X + 2:
            gaten.add((x, z))
        if x >= 236 and WEG_Z - 3 <= z <= WEG_Z + 2:
            gaten.add((x, z))
    HEK = set()
    rand_ord = sorted(EDGE, key=lambda p: math.atan2(p[1] - Z / 2, p[0] - X / 2))
    for i, (x, z) in enumerate(rand_ord):
        if (x, z) in gaten:
            continue
        S.air(x, G + 1, z, x, G + 6, z)
        if (x * 7 + z * 3) % 4 == 0:
            S.fill(x, G + 1, z, x, G + 5, z, POST)
        else:
            S.set(x, G + 1, z, "minecraft:dark_oak_fence")
            S.fill(x, G + 2, z, x, G + 4, z, "minecraft:iron_bars")
        HEK.add((x, z))
        OCC.add((x, z))
    # the bus road leaves the park: a red and white barrier in the fence line
    for x in range(114, 131):
        for z in range(218, 222):
            if (x, z) in EDGE:
                S.set(x, G + 1, z, "minecraft:red_wool" if x % 2 else "minecraft:white_wool")
                S.fill(x, G + 2, z, x, G + 4, z, "minecraft:iron_bars")
    bordje(112, G + 1, 214, 0, ["", "Busweg", "naar de", "Guhmension"])

    # ================================================================ greenery + lanterns (never on paths, plots, fence)
    for (x, z) in ((118, 96), (126, 96), (118, 184), (126, 184), (150, 120), (92, 104), (66, 150), (100, 148),
                   (144, 148), (180, 150), (176, 110), (60, 112), (16, 120), (140, 190), (98, 214), (200, 194)):
        if vrij_grond(x, z):
            lantaarnpaal(x, z)

    def acacia(x, z, y=G):
        h = rnd.randint(4, 6)
        S.fill(x, y + 1, z, x, y + h, z, "minecraft:acacia_log[axis=y]")
        dx, dz = rnd.choice([(1, 0), (-1, 0), (0, 1), (0, -1)])
        S.set(x + dx, y + h, z + dz, "minecraft:acacia_log[axis=y]")
        cx_, cz_ = x + dx, z + dz
        for a in range(-3, 4):
            for b in range(-3, 4):
                if a * a + b * b <= 10:
                    S.set(cx_ + a, y + h + 1, cz_ + b, AL, keep=True)
                if a * a + b * b <= 3:
                    S.set(cx_ + a, y + h + 2, cz_ + b, AL, keep=True)

    def palm(x, z, y=G):
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

    def eik(x, z, y=G):
        h = rnd.randint(4, 6)
        S.fill(x, y + 1, z, x, y + h, z, "minecraft:oak_log[axis=y]")
        S.ellipsoid(x, y + h, z, 2.6, 2.0, 2.6, OL, keep=True)

    # ================================================================ (G1, design 1.5) a picknickplek beside a path
    def picknick():
        beste, bd = None, 1e9
        for (x, z) in sorted(FP):
            d = (x - CX) ** 2 + (z - CZ) ** 2
            if d < 24 * 24 or d >= bd or not vrij_grond(x, z, 4) or S.get(x, G, z) != GRASS:
                continue
            blok = [(x + a, z + b) for a in range(-4, 5) for b in range(-4, 5)]
            if any(c in PATHS for c in blok):
                continue
            if not any((x + a, z + b) in PATHS for a in range(-6, 7) for b in range(-6, 7)):
                continue
            beste, bd = (x, z), d
        if beste is None:
            return None
        x, z = beste
        for a in range(-1, 2):                                  # the checked blanket
            for b in range(-2, 3):
                S.set(x + a, G + 1, z + b, "minecraft:red_carpet" if (a + b) % 2 else "minecraft:white_carpet")
        S.set(x + 3, G + 1, z, "guh_tafel[facing=north]")
        S.set(x + 3, G + 1, z - 1, "guh_stoel[facing=south]")
        S.set(x + 3, G + 1, z + 1, "guh_stoel[facing=north]")
        S.set(x - 3, G + 1, z, "minecraft:decorated_pot[cracked=false,facing=east,waterlogged=false]")
        S.set(x - 3, G + 1, z - 2, "guh_bloempot")
        bordje(x - 3, G + 1, z + 2, 4, ["Picknickplek", "Eet smakelijk,", "vads!", ""])
        for a in range(-4, 5):
            for b in range(-4, 5):
                OCC.add((x + a, z + b))
        return beste

    PICKNICK = picknick()
    print(f"    lobby: picknickplek {PICKNICK}")

    cells = sorted(FP)
    rnd.shuffle(cells)
    for (x, z) in cells:
        if not vrij_grond(x, z) or S.get(x, G, z) != GRASS:
            continue
        r = rnd.random()
        if r < 0.012 and vrij_grond(x, z, 4):
            (acacia if z > 112 else palm if x > 150 else eik)(x, z)
            for a in range(-4, 5):
                for b in range(-4, 5):
                    OCC.add((x + a, z + b))
        elif r < 0.04:
            S.set(x, G + 1, z, "minecraft:bush")
        elif r < 0.16:
            S.set(x, G + 1, z, "minecraft:short_grass")
        elif r < 0.21:
            S.set(x, G + 1, z, rnd.choice(["guhs:roze_guhbloem", "minecraft:poppy", "minecraft:oxeye_daisy",
                                           "guhs:guhoortjes", "minecraft:dandelion", "minecraft:azure_bluet"]))
        elif r < 0.215:
            S.set(x, G + 1, z, "guh_bloempot")

    S.fix_connections()

    # ================================================================ the parade walk (local, feet at G + 1)
    heen = [(234, 105), (200, 105), (170, 105), (150, 107), (141, 113)]
    start = math.atan2(heen[-1][1] - CZ, heen[-1][0] - CX)
    rond = [(CX + math.cos(start + k * math.pi / 6) * 13.0, CZ + math.sin(start + k * math.pi / 6) * 10.0)
            for k in range(1, 13)]
    parade = heen + rond + list(reversed(heen))

    # where the characters stand (local x, z), for zone_s2_lobby
    npc = dict(verkoper=((87, 121), (84, 121)), hoofdranger=((RX2 - 5, RZ2 - 6), (RX1, DZ)),
               fotograaf=((CX - 9, CZ + 9), (CX, CZ)), verzorger=((BX1 - 2, 118), (189, 118)),
               lodgebeheerder=((10, 30), (6, 40)))
    pv = dict(x=PVX, z=PVZ, r=PVR, balies=pv_balies)
    info = {"POND": POND, "HAVEN": HAVEN, "PATHS": PATHS, "HEK": HEK, "BORDJES": BORDJES, "LAMPEN": LAMPEN,
            "J3": dict(ijskraam=ijs, speeltuin=speeltuin, kampvuur=kampvuur), "PARADE": parade, "NPC": npc,
            "PAVILJOEN": pv, "BEDDEN": bedden, "LODGE": ((HUTS[0][0] - 2, 2), (HUTS[-1][0] + 7, 18)),
            "ALPACA": ALPACA, "BUS": bus}
    return S, info


# ----------------------------------------------------------------------------------------------- into the park
def _naam(s):
    return s if ":" in s.split("[")[0] else "guhs:" + s


KAARTWAND = {}      # (round 5) the kaartwand in the entreegebouw (local): x0, y0 (top row), z (the wall), n
INFO = {}           # the last build's lobby info (local coordinates): zone_s2_lobby / zone_zz_j3 / plekken read it

# the park route through the lobby (world): Australie -> the north gate -> the truck road -> the east gate -> savanne
NOORD_IN = [(249.0, 480.0), (246.0, 490.0), (236.0, 503.0), (OX + WEG_X + 0.0, 516.0)]
OOST_UIT = [(OX + X - 2.0, OZ + WEG_Z + 0.0), (280.0, 627.0), (304.0, 613.0), (326.0, 594.0), (342.0, 584.0)]


def _route(park, log):
    """Splice park.route_lijn: the old way through the old village (from the savanne side at x < ~342 round to the
    Australie side below z 480) is replaced by the new road. Returns (new line, removed points)."""
    lijn = park.route_lijn
    n = len(lijn)
    # the old line starts and ends at the old village: find where it leaves it (to the savanne) and comes back
    i_b = next(i for i in range(n) if lijn[i][0] >= OOST_UIT[-1][0])                  # first kept point, savanne side
    i_a = max(i for i in range(n) if lijn[i][1] <= NOORD_IN[0][1] and i > n // 2)    # last kept point, Australie side
    weg = [(OX + x, OZ + z) for (x, z) in WEG if 0 <= OZ + z and x <= X - 2]
    weg = [p for p in weg if p[1] >= NOORD_IN[-1][1]]
    noord = _dicht(_catmull([lijn[i_a]] + NOORD_IN[1:], n=10))
    oost = _dicht(_catmull(OOST_UIT[:-1] + [lijn[i_b]], n=10))
    nieuw = _dicht(weg[:1] + weg[1:]) + oost[1:] + list(lijn[i_b + 1:i_a]) + noord[:-1]
    weg_oud = list(lijn[:i_b]) + list(lijn[i_a + 1:])
    park.route_lijn = nieuw
    log(f"    lobby: route gesplitst: {len(weg_oud)} oude punten weg, {len(nieuw)} punten (start bij de noordpoort)")
    return nieuw, weg_oud, noord + oost


def bouw(park, log=print):
    from . import terrein
    S, info = _lobby()
    INFO.clear()
    INFO.update(info)
    W = kit.W
    fp = np.zeros((W, W), bool)
    for (x, z) in FP:
        fp[OX + x, OZ + z] = True

    _bomen_weg(park, fp)
    nieuw, weg_oud, aansluit = _route(park, log)

    # copy the local columns
    lut = np.array([park.id(_naam(s)) if i else 0 for i, s in enumerate(S.pal)], np.uint16)
    stone = park.id("minecraft:stone")
    vloer = park.vloer
    for (x, z) in FP:
        wx, wz = OX + x, OZ + z
        kol = park.v[wx, :, wz]
        kol[OY:] = 0
        kol[OY:OY + Y] = lut[S.v[x, :, z]]
        for y in range(int(min(vloer[wx, wz], OY)), OY):
            if kol[y] == 0 or park.pal[kol[y]].startswith("minecraft:water"):
                kol[y] = stone
    # the zones: the footprint is the lobby (entree, inside the park); the rest of the old entree zone goes to the
    # nearest other zone
    ezi = terrein.ZONES.index("entree")
    oud_zone = park.zone.copy()
    oud_binnen = park.binnen.copy()
    rest = (park.zone_vol == ezi) & ~fp
    _, (ix, iz) = distance_transform_edt(park.zone_vol == ezi, return_indices=True)
    park.zone_vol[rest] = park.zone_vol[ix[rest], iz[rest]]
    park.zone[rest & park.binnen] = park.zone_vol[rest & park.binnen]
    pond, haven = info["POND"], info["HAVEN"]
    for (x, z) in FP:
        wx, wz = OX + x, OZ + z
        park.zone[wx, wz] = ezi
        park.zone_vol[wx, wz] = ezi
        park.binnen[wx, wz] = True
        park.water[wx, wz] = (x, z) in haven             # (the pond lies 2 higher, at the ground: not the river's water)
        if (x, z) in haven:
            park.hoogte[wx, wz] = OY + G - 9
        elif (x, z) in pond:
            park.hoogte[wx, wz] = OY + G - 3
        else:
            park.hoogte[wx, wz] = OY + G
        park.pad[wx, wz] = (x, z) in info["PATHS"] or (x, z) in WEGCEL
        park.vloer[wx, wz] = min(int(vloer[wx, wz]), OY)
    # the sign texts (the tile writer puts them in as block entities)
    if not hasattr(park, "bordjes"):
        park.bordjes = []
    for ((x, y, z), regels) in info["BORDJES"]:
        park.bordjes.append({"pos": [OX + x, OY + y, OZ + z], "tekst": list(regels)})

    _buiten(park, fp, oud_zone, weg_oud, aansluit, log)

    # the fences: zone borders drawn again; outside the footprint fences that are gone are taken down and new ones
    # built; the footprint keeps its own fence (its posts and mesh are in the scene)
    oud_hek = park.hek.copy()
    terrein._hek_lijnen(park)
    for (x, z) in FP:
        park.hek[OX + x, OZ + z] = False
    for (x, z) in info["HEK"]:
        park.hek[OX + x, OZ + z] = True
    for (x, z) in EDGE:                                     # the road gaps (zone_s3_rit's safaripoorten go here)
        if (z <= 3 and WEG_X - 3 <= x <= WEG_X + 2) or (x >= 236 and WEG_Z - 3 <= z <= WEG_Z + 2):
            park.hek[OX + x, OZ + z] = True
    _hekken_bij(park, oud_hek, fp, log)
    park.hek_afstand = distance_transform_edt(~(park.hek & ~park.water))
    _overgang(park, fp, log)
    # the blend can leave bits of decor trees hanging (the decor ground is a shell): gone now, before the holes are
    # filled (the park check removes them later too, but then the columns under them would show their underside)
    from .controle import zweven
    zweven(park, True)
    doos = (OX - 45, OZ - 45, OX + X + 45, min(OZ + Z + 45, W))
    _vul_gaten(park, doos, log)
    _vaste_bladeren(park, doos)

    park.plein = (OX + CX, OY + G, OZ + CZ)
    park.ingang = (OX + 122 + 0.5, OY + G + 1, OZ + 196 + 0.5, 180.0)      # off the bus, looking north
    park.lobby = {"oorsprong": [OX, OY + G, OZ], "g": G, "draai": 0}
    park.boot_start = (OX + 208, OZ + 48)
    park.truckperron = (OX + 224, OZ + WEG_Z)
    log(f"    lobby at world {OX}..{OX + X}, {OZ}..{OZ + Z}; ingang {park.ingang[:3]}, boot_start {park.boot_start}")


def _buiten(park, fp, oud_zone, weg_oud, aansluit, log):
    """Outside the footprint: the basin joining the haven to the river, the old lake and the old route gone, the new
    route links (Australie side, savanne side, the path from the oostpoort), the surfaces of the zones that took over."""
    from . import terrein
    W = kit.W
    wy = kit.WATER_Y
    rnd = random.Random(23)
    zand = park.id("minecraft:sand")
    water = park.id("minecraft:water", level="0")
    # the channel north of the haven to the river (test round 5: a winding channel from the boats' gap in the fence,
    # no longer a rectangular basin that left a dead square of water west of the gap)
    # (along the river line the canoe follows: terrein.RIVIER's last stretch runs diagonally to the village pond)
    lijn = _dicht([(OX + sum(KANAAL_X) / 2 + 0.5, OZ + 1.5), (OX + 230.0, OZ - 4.0), (OX + 236.0, OZ - 10.0),
                   (OX + 242.0, OZ - 13.5), (OX + 248.0, OZ - 14.0)])
    from scipy.spatial import cKDTree
    kanaal = cKDTree(np.array(lijn))
    in_kanaal = np.zeros((W, W), bool)
    for x in range(OX + HAVEN_X1, OX + 242):
        for z in range(508, OZ + 2):
            if fp[x, z]:
                continue
            d, _ = kanaal.query((x + 0.5, z + 0.5))
            if d > 3.6 + 0.8 * vnoise(x / 4.0, z / 4.0, 41):
                continue
            in_kanaal[x, z] = True
            for y in range(wy - 6, wy - 3):
                park.v[x, y, z] = zand
            for y in range(wy - 3, wy + 1):
                park.v[x, y, z] = water
            for y in range(wy + 1, min(wy + 14, kit.H)):
                park.v[x, y, z] = 0
            park.water[x, z] = True
            park.hoogte[x, z] = wy - 4
            park.pad[x, z] = False
    # the old entree lake west of and around the channel (outside the footprint): land
    ezi = terrein.ZONES.index("entree")
    for x in range(OX + 150, OX + 238):
        for z in range(500, OZ):
            if park.water[x, z] and oud_zone[x, z] == ezi and not in_kanaal[x, z]:
                _grond(park, x, z, 64, rnd)
                park.water[x, z] = False
    # the old route: its path cells that no new route point is near go back to the zone's ground
    from scipy.spatial import cKDTree
    oud = cKDTree(np.array(weg_oud)) if weg_oud else None
    nieuw = cKDTree(np.array(park.route_lijn))
    xs, zs = np.nonzero(park.pad & ~fp)
    pts = np.c_[xs + 0.5, zs + 0.5]
    if oud is not None and len(pts):
        d_oud, _ = oud.query(pts)
        d_nieuw, _ = nieuw.query(pts)
        n_ = 0
        for (x, z), a, b in zip(zip(xs, zs), d_oud, d_nieuw):
            if a <= 2.6 and b > 3.0 and not park.water[x, z]:
                park.pad[x, z] = False
                _grond(park, x, z, int(park.hoogte[x, z]), rnd)
                n_ += 1
        log(f"    lobby: {n_} oude routevakken weer grond")
    # the surfaces of the zones that took over the old entree ground (no flat entree grass in Australie / savanne)
    for x, z in zip(*np.nonzero((oud_zone == ezi) & (park.zone != ezi) & ~park.water & ~park.pad & ~fp)):
        _grond(park, x, z, int(park.hoogte[x, z]), rnd, alleen_top=True)
    # the new route links outside the footprint: a 3 wide path at the ground, the corridor around it flat and free
    lijnen = [aansluit, _dicht([(OX + 237.5, OZ + 105.5), (OX + 243.0, OZ + 104.0), (OX + 249.0, OZ + 99.5)])]
    padblok = park.id("minecraft:dirt_path")
    for lijn in lijnen:
        for (px, pz) in lijn:
            for x in range(int(px) - 3, int(px) + 4):
                for z in range(int(pz) - 3, int(pz) + 4):
                    if not (0 <= x < W and 0 <= z < W) or fp[x, z] or park.water[x, z] or park.hek[x, z]:
                        continue
                    d = math.hypot(x + 0.5 - px, z + 0.5 - pz)
                    if d > 3.2:
                        continue
                    g = int(park.hoogte[x, z])
                    doel = 64 if d <= 2.2 else int(round((g + 64) / 2))
                    if g != doel:
                        _grond(park, x, z, doel, rnd)
                    if d <= 1.6:
                        park.v[x, doel, z] = padblok
                        park.pad[x, z] = True
                    for y in range(doel + 1, min(doel + 6, kit.H)):
                        s = park.pal[park.v[x, y, z]]
                        if park.v[x, y, z] and not s.startswith(("minecraft:water",)):
                            park.v[x, y, z] = 0


def _grond(park, x, z, h, rnd, alleen_top=False):
    """Column x, z as natural ground of its zone with its top at h (the zone's surface blocks, plants above cleared)."""
    from . import terrein
    zi = int(park.zone_vol[x, z])
    k = terrein.ZONES[zi] if zi >= 0 else "entree"
    tops, sub, _ = terrein.OPP.get(k, terrein.OPP["entree"])
    keuze = [t for t, _ in tops if t != "powder_snow_off"]
    gew = np.array([w for t, w in tops if t != "powder_snow_off"], float)
    top = keuze[int(np.searchsorted(np.cumsum(gew / gew.sum()), rnd.random()))]
    top_s = "minecraft:grass_block[snowy=false]" if top == "grass_block" else "minecraft:" + top
    oud = int(park.hoogte[x, z])
    if alleen_top:
        if park.v[x, oud, z] and park.pal[park.v[x, oud, z]].split("[")[0] in ("minecraft:grass_block", "minecraft:dirt_path"):
            park.v[x, oud, z] = park.id(top_s)
            boven = park.pal[park.v[x, oud + 1, z]].split("[")[0]
            if top != "grass_block" and boven in ("minecraft:short_grass", "minecraft:tall_grass", "minecraft:poppy",
                                                  "minecraft:dandelion", "guhs:roze_guhbloem", "minecraft:bush",
                                                  "minecraft:oxeye_daisy", "guhs:guhoortjes", "minecraft:azure_bluet"):
                park.v[x, oud + 1, z] = 0
                if park.pal[park.v[x, oud + 2, z]].endswith("half=upper"):
                    park.v[x, oud + 2, z] = 0
        return
    subid = park.id("minecraft:" + sub)
    for y in range(min(oud, h) - 2, h):
        park.v[x, y, z] = subid
    park.v[x, h, z] = park.id(top_s)
    for y in range(h + 1, min(max(oud, h) + 6, kit.H)):
        s = park.pal[park.v[x, y, z]]
        if park.v[x, y, z] and not s.split("[")[0].endswith(("_log", "_wood")):
            park.v[x, y, z] = 0
    park.hoogte[x, z] = h


def _hekken_bij(park, oud_hek, fp, log):
    """Outside the footprint: fences that are no longer on a zone border come down, new border cells get the park
    fence (dark oak fence + iron bars, a log post every 4; the outer fence one higher), as terrein.hekken."""
    W = kit.W
    weg = 0
    nieuw = 0
    post = park.id("minecraft:dark_oak_log", axis="y")
    for x, z in zip(*np.nonzero(oud_hek & ~park.hek & ~fp)):
        g = int(park.hoogte[x, z])
        for y in range(g + 1, min(g + 8, kit.H)):
            s = park.pal[park.v[x, y, z]].split("[")[0]
            if s in ("minecraft:dark_oak_fence", "minecraft:iron_bars", "minecraft:dark_oak_log", kit.GATE,
                     "minecraft:dark_oak_fence_gate"):
                park.v[x, y, z] = 0
        weg += 1
    for x, z in zip(*np.nonzero(park.hek & ~oud_hek & ~fp)):
        if park.water[x, z]:
            continue
        g = int(park.hoogte[x, z])
        hoog = 4 if park.hek_buiten[x, z] else 3
        if park.pad[x, z]:
            continue                                   # a route crossing: zone_s3_rit puts its safaripoort here
        if (x * 7 + z * 3) % 4 == 0:
            for y in range(g + 1, g + hoog + 2):
                park.v[x, y, z] = post
        else:
            park.set(x, g + 1, z, "minecraft:dark_oak_fence")
            for y in range(g + 2, g + hoog + 1):
                park.set(x, y, z, "minecraft:iron_bars")
        nieuw += 1
    log(f"    lobby: hekken {weg} weg, {nieuw} nieuw")


HOUT = ("_leaves", "_log", "_wood", "vine", "palm", "mangrove_roots", "propagule")


def _bomen_weg(park, schrijf, beschermd=None):
    """Trees whose trunk is in the lobby area go, crown and all. A leaf or branch belongs to the NEAREST trunk, so
    the crowns of the trees that stay outside are left whole. Columns in `beschermd` (the lobby itself) are never
    touched: its buildings are wood too (test round 5: the two west lodges lost their walls to a decor tree's crown)."""
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
                if schrijf[x, z] or (beschermd is not None and beschermd[x, z]):
                    continue
                d = np.hypot(stammen[:, 0] - x, stammen[:, 1] - z)
                if int(np.argmin(d)) not in weg_idx:
                    continue
                g = int(park.hoogte[x, z])
                kol = park.v[x, g + 1:min(ttop + 5, kit.H), z]
                kol[np.isin(kol, hout_ids)] = 0
    park.bomen = [b for b in park.bomen if not schrijf[b[0], b[2]]]


def _overgang(park, schrijf, log):
    """Blend around the lobby to its height (9 blocks inside the park, 24 on the decor side): the 3 nearest flat (escape
    rule), then a ramp to the old ground. Not near fences, not over water. Trees standing where the ground changes go
    first, crown and all (the decor ground is a shell: a trunk column left standing would float with its tree)."""
    d = distance_transform_edt(~schrijf)
    doel = OY + G
    xs, zs = np.nonzero((d > 0) & (d <= 24))
    plan = []
    verandert = np.zeros(schrijf.shape, bool)
    for x, z in zip(xs, zs):
        if park.water[x, z] or park.hek[x, z] or (park.hek_afstand[x, z] <= 3 and d[x, z] > 4):
            continue                    # (other fences keep their flat strip; the lobby's own fence strip is made here)
        lim = 9 if park.binnen[x, z] else 24
        if d[x, z] > lim:
            continue
        oud = int(park.hoogte[x, z])
        t = min(max((d[x, z] - 3) / (lim - 3.0), 0.0), 1.0)
        nieuw = int(round(doel + (oud - doel) * t))
        if not park.binnen[x, z]:
            # the decor side: never steeper than ~1 in 2 away from the lobby (the lobby lies at the world's west and
            # south edge: the decor hills there stood as a cliff wall right behind the fence)
            ruimte = max(0.0, d[x, z] - 3) * 0.5
            nieuw = int(round(min(max(nieuw, doel - ruimte), doel + ruimte)))
        if nieuw == oud or park.v[x, oud, z] == 0:
            continue
        plan.append((x, z, oud, nieuw))
        verandert[x, z] = True
    _bomen_weg(park, verandert, beschermd=schrijf)
    padblok = park.id("minecraft:dirt_path")
    dirt = park.id("minecraft:dirt")
    for x, z, oud, nieuw in plan:
        top = park.v[x, oud, z]
        if top == 0 or park.pal[top].split("[")[0].endswith(HOUT):
            continue
        # (everything above the new ground goes: the decor hills hold more than their height map top)
        for y in range(min(oud, nieuw) + 1, kit.H if nieuw < oud else min(nieuw + 3, kit.H)):
            naam = park.get(x, y, z).split("[")[0]
            if naam.endswith(("_log", "_wood", "_leaves")) and y > nieuw + 2:
                continue                                # a crown of a tree outside the ring hanging over the column
            park.v[x, y, z] = 0
        for y in range(oud, nieuw):
            park.v[x, y, z] = dirt
        park.v[x, nieuw, z] = padblok if park.pad[x, z] else top
        park.hoogte[x, z] = nieuw
    log(f"    lobby: {len(plan)} columns blended around the lobby")


def _vul_gaten(park, doos, log):
    """Every column in the box reaches down to 2 below its lowest neighbour (no void under the ground)."""
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
    log(f"    lobby: {n} columns filled down (no void under the ground)")


def _vaste_bladeren(park, doos):
    """All leaves around the lobby are persistent (no decaying leaves dropping saplings and sticks)."""
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


def wereld(x, z, y=None):
    """Local (x, z) [and local y] -> world [x, y, z] (y: feet on the ground when not given)."""
    return [OX + int(round(x)), OY + (G + 1 if y is None else int(y)), OZ + int(round(z))]


def plekken(park, plekken_):
    """The four aaihoek animals live in their pens (1 adult + 1 young each); and (round 4) the lobby's connections to
    the rides, now that zone_s3_rit has built them: data/guhs/safari/lobby_aansluiting.json."""
    pens = {fn: (x0, z0) for (x0, z0, fn) in PENS}
    for p in plekken_:
        if p["soort"] in pens:
            x0, z0 = pens[p["soort"]]
            p["zone"] = "entree"
            p["centrum"] = [OX + x0 + 4, OY + G + 1, OZ + z0 + 4]
            p["straal"] = 2
            p["water"] = False
            p["boom"] = False
    s3 = getattr(park, "_s3", None) or {}
    hs = getattr(park, "_hoofdsteiger", None)
    route = s3.get("route") or {}
    halte = next((h for h in route.get("haltes", []) if h.get("zone") == "entree"), None)
    kano = s3.get("kano") or {}
    boot = ((s3.get("boot") or {}).get("routes") or [None])[0] or {}
    data = {
        "bus": wereld(122, 200),
        "bus_bel": wereld(*INFO["BUS"]["bel"][0::2], y=INFO["BUS"]["bel"][1]) if INFO else None,
        "hoofdsteiger": [round(c, 2) for c in hs["pos"]] if hs else None,
        "kano": [round(c, 2) for c in kano["start"]] if kano.get("start") else None,
        "truckperron": [round(c, 2) for c in route["punten"][halte["i"]]] if halte else None,
        "parkpoort_voet": wereld(236, 105),
        "boot_start": [[round(p[0], 2), round(p[1], 2), round(p[2], 2)] for p in (boot.get("punten") or [])[:1]],
        "lobby": getattr(park, "lobby", None),
        "uitleg": "round 4 (L1): where the lobby meets the rides. bus = the Guh-safaribus back to the Guhmension (ring "
                  "bus_bel), hoofdsteiger = the boat line's first jetty (in the haven, joined to the river), kano = "
                  "where the canoe is rented, truckperron = the truck's lobby stop, parkpoort_voet = the oostpoort into "
                  "the savanne on foot. lobby: local (x, y, z) -> world (oorsprong + (x, y - g, z)).",
    }
    pad = os.path.join(kit.OUT_DATA, "lobby_aansluiting.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
