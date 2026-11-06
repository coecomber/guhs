"""U-Z6RAID: the inside of De Sterrendovende Toren (zone 6 raid, 2-10 players) as ONE structure template.

The tower OUTSIDE (the twisted grey shaft at the Moederoase, the iwan door, the floating rings, the Lege Lens) comes from
u-z6wereld's tiles. The raid floors do not fit in that shaft (r 8-16 is too small for 10 players and spread mechanics), so the
inside is a "binnentoren": a pocket in the rock UNDER the tower, entered through the tower door (Java: Z6Raid teleports you
in). It is stacked like the real tower (you climb it floor by floor on real stairs) and is self-contained (own 2-block shell),
so it works whatever the terrain is.

Layout (local coordinates; origin A = the feet spot in the middle of floor 1; +x east, +z south; Java: Z6RaidPlekken):
  floor k (0 = De Wachtzaal, 1 Zandwachter, 2 Spiegelvizier, 3 Gedoofde Sterrenbeelden, 4 Ringeling's roof) has its feet at
  y = 12 * (k - 1). Floors 0-3: 9 blocks of air, 3 blocks of slab. Floor 4 has a star dome (air up to +13).
  Each floor: an octagonal hall (walkable to r ~21, arena radius 21 in Java) with a ring of pillars (r 19.5), deep alcoves in
  the wall (r 21.5-24, iron-bar windows, lanterns on chains), a door east (x 22-23, z -1..1) to the landing (x 24-27,
  z -3..5 = the checkpoint), and a switchback stair in the annex (flight A x 28-29 going north, mid landing z -4..-6, flight B
  x 31-32 going south, centre wall x 30) up to the next landing. The Java "sterrenvlies" gate closes flight A at x 28-29, z 2,
  y +1..+3 until the floor's boss is cheered up.
  Floor 1: rose sandstone, two hourglass plates at (-12, 0) and (12, 0) (gold ring, Java checks r 2.5).
  Floor 2: pearl/mirror hall, mirror panels in the alcoves, a prism in the ceiling (the beam comes from there).
  Floor 3: a night-blue floor with star-line mosaics (the three constellations).
  Floor 4: grey roof with gold ring inlays, the Lege Lens hanging from the dome, unlit stars in the dome
           (minecraft:light_gray_stained_glass; Java swaps them to glowstone when Ringeling is cheered up).
  Floor 0 (Wachtzaal): benches, the start circle at (-6, -12, 0) r 3, the exit at the west wall (-19, -12, 0).

Output: src/main/resources/data/guhs/structure/vadsrijk/zone6/raid_binnentoren.nbt (template min corner = A + MIN),
run the u-hekken connection pass (verbind.verbind_vol) over the volume first. Deterministic.
Run from the project root:  python tools/vadsrijk/zone6/raid/binnentoren.py
"""
import math
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk"))
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk", "wereld"))
import nbtsnel  # noqa: E402

UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "zone6", "raid_binnentoren.nbt")

# ---- the numbers Java mirrors (Z6RaidPlekken) ------------------------------------------------------------------------
MIN = (-25, -14, -25)          # template min corner relative to A
MAX = (34, 52, 25)             # inclusive
VERDIEPING = 12                # feet of floor k = 12 * (k - 1)
LUCHT_H = 9                    # air above the feet on floors 0-3
DAK_H = 14                     # air above the feet on floor 4 (dome)
R_ARENA = 21.5                 # walkable octagon
R_ZUIL = 19.5                  # pillar ring
R_MUUR = 24.2                  # outer octagon wall
PLATEN = [(-12, 0), (12, 0)]   # floor 1 hourglass plates
START = (-6, 0)                # floor 0 start circle (r 3)
UITGANG = (-19, 0)             # floor 0 exit (west wall)

STEEN = "minecraft:polished_andesite"
POORT = "minecraft:purple_stained_glass"      # the gate block (Java Z6RaidBouw.POORT)
SCHIL = "minecraft:stone"


def feet(k):
    return VERDIEPING * (k - 1)


def okt(dx, dz, rot=0.0):
    """Octagon 'radius' (distance to the centre along the facet normal)."""
    a = math.atan2(dz, dx) + rot
    sec = math.pi / 8
    return math.hypot(dx, dz) * math.cos((a % (2 * sec)) - sec)


class Bouw:
    def __init__(self):
        self.X, self.Y, self.Z = (MAX[i] - MIN[i] + 1 for i in range(3))
        self.pal = ["minecraft:air"]
        self.idx = {"minecraft:air": 0}
        self.vol = np.zeros((self.X, self.Y, self.Z), np.int32)
        self.vol[:] = self.id(SCHIL)

    def id(self, s):
        i = self.idx.get(s)
        if i is None:
            i = len(self.pal)
            self.pal.append(s)
            self.idx[s] = i
        return i

    def zet(self, x, y, z, s):
        ix, iy, iz = int(round(x)) - MIN[0], int(y) - MIN[1], int(round(z)) - MIN[2]
        if 0 <= ix < self.X and 0 <= iy < self.Y and 0 <= iz < self.Z:
            self.vol[ix, iy, iz] = self.id(s)

    def get(self, x, y, z):
        ix, iy, iz = x - MIN[0], y - MIN[1], z - MIN[2]
        if 0 <= ix < self.X and 0 <= iy < self.Y and 0 <= iz < self.Z:
            return self.pal[int(self.vol[ix, iy, iz])]
        return SCHIL

    def doos(self, x0, y0, z0, x1, y1, z1, s):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.zet(x, y, z, s)


def rng(x, y, z, s=0):
    h = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (s * 2654435761)
    return ((h & 0xFFFFFFFF) % 1000) / 1000.0


# ---- themes per floor --------------------------------------------------------------------------------------------------
THEMA = {
    0: dict(vloer=["minecraft:smooth_sandstone", "minecraft:cut_sandstone"], wand="minecraft:sandstone",
            accent="minecraft:chiseled_sandstone", zuil="minecraft:cut_red_sandstone", band="minecraft:pink_glazed_terracotta",
            plafond="minecraft:smooth_red_sandstone", trap="minecraft:smooth_sandstone_stairs"),
    1: dict(vloer=["minecraft:smooth_red_sandstone", "minecraft:cut_red_sandstone", "minecraft:red_sandstone"],
            wand="minecraft:red_sandstone", accent="minecraft:chiseled_red_sandstone", zuil="minecraft:cut_red_sandstone",
            band="minecraft:pink_glazed_terracotta", plafond="minecraft:smooth_red_sandstone", trap="minecraft:smooth_red_sandstone_stairs"),
    2: dict(vloer=["minecraft:white_concrete", "minecraft:pink_concrete", "minecraft:light_blue_concrete"],
            wand="minecraft:smooth_quartz", accent="minecraft:chiseled_quartz_block", zuil="minecraft:quartz_pillar",
            band="minecraft:light_blue_glazed_terracotta", plafond="minecraft:quartz_bricks", trap="minecraft:quartz_stairs"),
    3: dict(vloer=["minecraft:blue_concrete", "minecraft:blue_terracotta", "minecraft:black_concrete"],
            wand="minecraft:deepslate_bricks", accent="minecraft:chiseled_deepslate", zuil="minecraft:polished_deepslate",
            band="minecraft:purple_glazed_terracotta", plafond="minecraft:deepslate_tiles", trap="minecraft:deepslate_brick_stairs"),
    4: dict(vloer=["minecraft:polished_andesite", "minecraft:andesite", "minecraft:stone_bricks"],
            wand="minecraft:stone_bricks", accent="minecraft:chiseled_stone_bricks", zuil="minecraft:polished_andesite",
            band="minecraft:magenta_glazed_terracotta", plafond="minecraft:polished_blackstone_bricks", trap="minecraft:stone_brick_stairs"),
}


def hal(B, k):
    T = THEMA[k]
    y0 = feet(k)
    hoog = DAK_H if k == 4 else LUCHT_H
    for x in range(MIN[0], MAX[0] + 1):
        for z in range(MIN[2], MAX[2] + 1):
            d = okt(x, z)
            if d > R_MUUR + 1.2:
                continue
            # floor + foundation under it (floor block y0-1)
            if d <= R_MUUR:
                vl = T["vloer"]
                s = vl[int(rng(x, k, z) * len(vl)) % len(vl)]
                if k == 1 and int(round(math.hypot(x, z))) in (7, 14):
                    s = "minecraft:chiseled_red_sandstone"
                if k == 2 and (abs(x) == abs(z) or x == 0 or z == 0) and d < R_ARENA:
                    s = "minecraft:pearlescent_froglight" if int(math.hypot(x, z)) % 6 == 0 else "minecraft:white_concrete"
                if k == 4 and abs(math.hypot(x, z) - 10) < 0.6:
                    s = "minecraft:gold_block"
                if k == 0 and math.hypot(x, z) < 3.5:
                    s = "minecraft:chiseled_sandstone"
                B.zet(x, y0 - 1, z, s)
            # air inside the walkable octagon
            if d <= R_ARENA:
                for y in range(y0, y0 + hoog):
                    B.zet(x, y, z, "minecraft:air")
            elif d <= R_MUUR:
                # the wall ring: wall + a decorative band and a cornice
                for y in range(y0, y0 + hoog):
                    s = T["wand"]
                    if y - y0 == 3:
                        s = T["band"]
                    elif y - y0 == hoog - 1:
                        s = T["accent"]
                    B.zet(x, y, z, s)
    # ceiling (floors 0-3): plafond with a coffered pattern
    if k < 4:
        for x in range(MIN[0], MAX[0] + 1):
            for z in range(MIN[2], MAX[2] + 1):
                d = okt(x, z)
                if d <= R_MUUR:
                    s = T["plafond"]
                    if d <= R_ARENA and (x % 6 == 0 or z % 6 == 0):
                        s = T["accent"]
                    B.zet(x, y0 + hoog, z, s)
                    # coffer recess: one block up between the beams (depth in the ceiling)
                    if d <= R_ARENA - 1 and x % 6 != 0 and z % 6 != 0:
                        B.zet(x, y0 + hoog, z, "minecraft:air")
                        B.zet(x, y0 + hoog + 1, z, T["plafond"])
    # pillars (2x2) on the ring + capitals, arches between them with upside-down stairs
    for i in range(12):
        a = i * 2 * math.pi / 12 + math.pi / 12
        cx, cz = math.cos(a) * R_ZUIL, math.sin(a) * R_ZUIL
        if abs(cz) < 3 and cx > 0:      # keep the east door free
            continue
        for dx in (0, 1):
            for dz in (0, 1):
                x, z = int(math.floor(cx)) + dx, int(math.floor(cz)) + dz
                for y in range(y0, y0 + hoog):
                    s = T["zuil"]
                    if y == y0:
                        s = T["accent"]
                    elif y == y0 + hoog - 1:
                        s = T["accent"]
                    B.zet(x, y, z, s)
    # alcoves between the pillars: 2 deep, 3 wide, 5 high, with iron-bar windows, a lantern on a chain
    for i in range(12):
        a = i * 2 * math.pi / 12
        if abs(math.sin(a)) < 0.3 and math.cos(a) > 0:   # east = the door
            continue
        ux, uz = math.cos(a), math.sin(a)
        for w in (-1, 0, 1):
            for diep in (0, 1):
                r = R_ARENA + 0.6 + diep
                x = int(round(ux * r - uz * w))
                z = int(round(uz * r + ux * w))
                for y in range(y0, y0 + 5):
                    B.zet(x, y, z, "minecraft:air")
                B.zet(x, y0 - 1, z, T["accent"])
            # back wall: bars in the middle (a grille window), mirror panels on floor 2
            r = R_ARENA + 2.6
            x = int(round(ux * r - uz * w))
            z = int(round(uz * r + ux * w))
            for y in range(y0 + 1, y0 + 4):
                if k == 2:
                    B.zet(x, y, z, "minecraft:white_stained_glass" if w else "minecraft:glass")
                else:
                    B.zet(x, y, z, "minecraft:iron_bars")
        # a lantern hanging in the alcove mouth
        x, z = int(round(ux * (R_ARENA + 0.6))), int(round(uz * (R_ARENA + 0.6)))
        B.zet(x, y0 + 4, z, "minecraft:iron_chain[axis=y]")
        B.zet(x, y0 + 3, z, "minecraft:lantern[hanging=true]")
    # benches: a ring of slabs in front of the wall (walkable: 1/2 step)
    for i in range(48):
        a = i * 2 * math.pi / 48
        if abs(math.sin(a)) < 0.25 and math.cos(a) > 0:
            continue
        x, z = int(round(math.cos(a) * (R_ARENA - 0.4))), int(round(math.sin(a) * (R_ARENA - 0.4)))
        if B.get(x, y0, z) == "minecraft:air":
            B.zet(x, y0, z, T["trap"].replace("_stairs", "_slab") + "[type=bottom]")
    # the east door + landing + stair annex
    deur(B, k, T)
    if k < 4:
        trap(B, k, T)
    # light: lanterns on the ceiling beams
    if k < 4:
        for x in range(-18, 19, 6):
            for z in range(-18, 19, 6):
                if okt(x, z) < R_ZUIL - 2 and (x, z) != (0, 0):
                    B.zet(x, y0 + LUCHT_H - 1, z, "minecraft:iron_chain[axis=y]")
                    B.zet(x, y0 + LUCHT_H - 2, z, "minecraft:lantern[hanging=true]" if k != 3 else "minecraft:soul_lantern[hanging=true]")


def deur(B, k, T):
    y0 = feet(k)
    for x in range(20, 24):
        for z in range(-1, 2):
            for y in range(y0, y0 + 4):
                B.zet(x, y, z, "minecraft:air")
            B.zet(x, y0 - 1, z, T["accent"])
        B.zet(x, y0 + 4, -1, T["accent"])
        B.zet(x, y0 + 4, 1, T["accent"])
        B.zet(x, y0 + 4, 0, T["band"])
    # the landing room x 24-27, z -3..5 (+ x 28-32 z 3..5 = the top of the stair below)
    for x in range(24, 33):
        for z in range(-3, 6):
            if x >= 28 and z < 3:
                continue
            for y in range(y0, y0 + 4):
                B.zet(x, y, z, "minecraft:air")
            B.zet(x, y0 - 1, z, T["vloer"][0])
            B.zet(x, y0 + 4, z, T["plafond"])
    # a campfire-like brazier at the checkpoint (light) + a bench
    B.zet(26, y0, -3, "minecraft:lantern[hanging=false]")
    B.zet(24, y0, 5, T["trap"] + "[facing=north,half=bottom,shape=straight]")
    B.zet(25, y0, 5, T["trap"] + "[facing=north,half=bottom,shape=straight]")


def trap(B, k, T):
    """Switchback up to floor k+1: flight A (x 28-29) north, mid landing z -4..-6, flight B (x 31-32) south."""
    y0 = feet(k)
    trapblok = T["trap"]
    # carve the annex air
    for x in range(28, 33):
        for z in range(-6, 3):
            for y in range(y0, y0 + VERDIEPING + 4):
                B.zet(x, y, z, "minecraft:air")
    for z in range(-6, 6):
        for y in range(y0 - 1, y0 + VERDIEPING + 4):
            B.zet(33, y, z, T["wand"])
    for x in range(28, 34):
        for y in range(y0 - 1, y0 + VERDIEPING + 4):
            B.zet(x, y, -7, T["wand"])
    # flight A: step i at z = 2 - i, block y0 + i (fill below), facing north (you walk north going up)
    for i in range(6):
        z = 2 - i
        for x in (28, 29):
            for y in range(y0 - 1, y0 + i):
                B.zet(x, y, z, T["wand"])
            B.zet(x, y0 + i, z, f"{trapblok}[facing=north,half=bottom,shape=straight]")
    # mid landing z -4..-6, floor block y0 + 5 (feet y0 + 6)
    for x in range(28, 33):
        for z in range(-6, -3):
            for y in range(y0 - 1, y0 + 6):
                B.zet(x, y, z, T["wand"])
            B.zet(x, y0 + 5, z, T["vloer"][0])
    B.zet(30, y0 + 6, -6, "minecraft:lantern[hanging=false]")
    # flight B: step j at z = -3 + j, block y0 + 6 + j, facing south
    for j in range(6):
        z = -3 + j
        for x in (31, 32):
            # a thin underside (2 blocks): the flight of the floor below needs its headroom right under this one
            for y in range(max(y0 - 1, y0 + 4 + j), y0 + 6 + j):
                B.zet(x, y, z, T["wand"])
            B.zet(x, y0 + 6 + j, z, f"{trapblok}[facing=south,half=bottom,shape=straight]")
    # the centre wall x 30 (full height: no fall from flight B onto flight A) with a wall-lamp
    for z in range(-3, 3):
        for y in range(y0 - 1, y0 + VERDIEPING + 3):
            B.zet(30, y, z, T["wand"])
    # the top landing x 28-32, z 3..5 is floor k+1's landing (made by deur(k+1)); its floor block is y0 + 11
    # headroom over flight B into floor k+1: open the slab above the flight B top steps
    for z in range(-3, 3):
        for x in (31, 32):
            for y in range(y0 + 7 + (z + 3), y0 + VERDIEPING + 3):
                B.zet(x, y, z, "minecraft:air")
    # flight A must not stick up into the room above: the slab over flight A stays (only flight B opens upward)
    for z in range(-3, 3):
        for x in (28, 29):
            for y in range(y0 + 9, y0 + VERDIEPING):
                B.zet(x, y, z, T["wand"])
    # the railing between the top landing (z 3) and the open flight-B shaft is the wall at x 30 + this bar row
    for x in (28, 29):
        B.zet(x, y0 + VERDIEPING, 2, "minecraft:iron_bars")
    # the "sterrenvlies" gate (closed in the template; Java opens it when the floor's boss is cheered up)
    for x in (28, 29):
        for y in range(y0 + 1, y0 + 4):
            B.zet(x, y, 2, POORT)


def thema_details(B):
    # floor 1: the two hourglass plates (gold ring + sand inside)
    y = feet(1) - 1
    for (px, pz) in PLATEN:
        for x in range(px - 3, px + 4):
            for z in range(pz - 3, pz + 4):
                d = math.hypot(x - px, z - pz)
                if d <= 2.6:
                    B.zet(x, y, z, "minecraft:sand" if d < 1.6 else "minecraft:smooth_sandstone")
                elif d <= 3.4:
                    B.zet(x, y, z, "minecraft:gold_block")
    # floor 2: the prism in the ceiling centre
    y2 = feet(2) + LUCHT_H
    B.doos(-1, y2, -1, 1, y2 + 1, 1, "minecraft:white_stained_glass")
    B.zet(0, y2, 0, "minecraft:sea_lantern")
    # floor 3: star lines (three constellations) inlaid in the floor
    y3 = feet(3) - 1
    sterren = [[(-14, -6), (-10, -9), (-6, -7), (-7, -3), (-11, -2)],
               [(4, -14), (9, -12), (13, -8), (10, -4), (15, -2)],
               [(-4, 9), (0, 12), (5, 10), (3, 6)]]
    for keten in sterren:
        for (ax, az), (bx, bz) in zip(keten, keten[1:]):
            n = int(max(abs(bx - ax), abs(bz - az)))
            for t in range(n + 1):
                x = int(round(ax + (bx - ax) * t / max(1, n)))
                z = int(round(az + (bz - az) * t / max(1, n)))
                B.zet(x, y3, z, "minecraft:light_blue_concrete")
        for (x, z) in keten:
            B.zet(x, y3, z, "minecraft:sea_lantern")
    # floor 4: the dome with unlit stars and the Lege Lens
    y4 = feet(4)
    for x in range(MIN[0], MAX[0] + 1):
        for z in range(MIN[2], MAX[2] + 1):
            d = okt(x, z)
            if d > R_MUUR + 1:
                continue
            r = math.hypot(x, z)
            # dome: air below the curve, shell above
            top = y4 + DAK_H + int(3 * max(0.0, 1 - (r / R_MUUR) ** 2))
            if d <= R_ARENA:
                for y in range(y4 + DAK_H, top):
                    B.zet(x, y, z, "minecraft:air")
            for y in (top, top + 1):
                s = "minecraft:black_concrete"
                if y == top and d <= R_ARENA and rng(x, y, z, 4) < 0.07:
                    s = "minecraft:light_gray_stained_glass"     # an unlit star (Java lights it)
                B.zet(x, y, z, s)
    # the Lege Lens: a tinted-glass ball under the dome with a purple core, a gold ring round it
    cy = y4 + DAK_H - 2
    for x in range(-4, 5):
        for y in range(-3, 4):
            for z in range(-4, 5):
                d = math.sqrt(x * x + (y * 1.3) ** 2 + z * z)
                if d <= 3.6:
                    B.zet(x, cy + y, z, "minecraft:tinted_glass" if d > 1.6 else "minecraft:purple_stained_glass")
    for i in range(40):
        a = i * 2 * math.pi / 40
        B.zet(math.cos(a) * 5.5, cy, math.sin(a) * 5.5, "minecraft:gold_block")
    for y in range(cy + 4, y4 + DAK_H + 3):
        B.zet(0, y, 0, "minecraft:iron_chain[axis=y]")
    # floor 0: the start circle (gold rim) and the exit arch at the west wall
    y0 = feet(0) - 1
    sx, sz = START
    for x in range(sx - 4, sx + 5):
        for z in range(sz - 4, sz + 5):
            d = math.hypot(x - sx, z - sz)
            if d <= 3.0:
                B.zet(x, y0, z, "minecraft:pink_glazed_terracotta")
            elif d <= 3.7:
                B.zet(x, y0, z, "minecraft:gold_block")
    ux, uz = UITGANG
    for z in range(uz - 1, uz + 2):
        for y in range(feet(0), feet(0) + 4):
            B.zet(ux - 1, y, z, "minecraft:air")
            B.zet(ux - 2, y, z, "minecraft:purple_stained_glass" if y < feet(0) + 3 else "minecraft:chiseled_sandstone")
            B.zet(ux - 3, y, z, "minecraft:obsidian")
    B.zet(ux - 1, feet(0) + 4, uz, "minecraft:chiseled_sandstone")


def bouw():
    B = Bouw()
    for k in range(5):
        hal(B, k)
    thema_details(B)
    return B


def verbind(B):
    try:
        import verbind as V
    except ImportError:
        print("binnentoren: no tools/vadsrijk/verbind.py (u-hekken): connection states left as written")
        return 0
    return V.verbind_vol(B.vol, B.pal, B.id, rand=0)


def schrijf(B):
    pal = []
    for s in B.pal:
        naam, _, rest = s.partition("[")
        props = {}
        if rest:
            for kv in rest.rstrip("]").split(","):
                k, v = kv.split("=")
                props[k] = v
        pal.append((naam, props))
    xs, ys, zs = np.nonzero(np.ones_like(B.vol, bool))
    st = B.vol[xs, ys, zs]
    os.makedirs(os.path.dirname(UIT), exist_ok=True)
    return nbtsnel.schrijf(UIT, (B.X, B.Y, B.Z), pal, xs.astype(np.int32), ys.astype(np.int32), zs.astype(np.int32), st.astype(np.int32))


def controleer(B):
    """Walkability: every stair step is a stair, every landing spot has 2 blocks of air, the gates' cells are air."""
    fout = []
    for k in range(4):
        y0 = feet(k)
        for i in range(6):
            if "stairs" not in B.get(28, y0 + i, 2 - i):
                fout.append(f"floor {k}: flight A step {i}")
            if "stairs" not in B.get(31, y0 + 6 + i, -3 + i):
                fout.append(f"floor {k}: flight B step {i}")
        for (x, z) in ((25, 1), (0, 0), (29, 3)):
            for dy in (0, 1):
                if B.get(x, y0 + dy, z) != "minecraft:air":
                    fout.append(f"floor {k}: no headroom at {x},{z}")
            if B.get(x, y0 - 1, z) in ("minecraft:air",):
                fout.append(f"floor {k}: no floor at {x},{z}")
        # the step from the top of flight B onto the top landing of k+1
        if B.get(31, y0 + VERDIEPING - 1, 3) == "minecraft:air":
            fout.append(f"floor {k}: no top landing floor")
        for dy in (0, 1):
            if B.get(31, y0 + VERDIEPING + dy, 3) != "minecraft:air":
                fout.append(f"floor {k}: top landing blocked")
    return fout


def main():
    B = bouw()
    n = verbind(B)
    fout = controleer(B)
    for f in fout:
        print("LOOPCHECK", f)
    blokken = schrijf(B)
    print(f"binnentoren: {B.X}x{B.Y}x{B.Z}, {blokken} blocks, {len(B.pal)} states, verbind changed {n}, loopcheck {len(fout)} -> {UIT}")
    return 1 if fout else 0


if __name__ == "__main__":
    sys.exit(main())
