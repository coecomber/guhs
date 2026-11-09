"""The hand-made set pieces of zone 2 "De Splinterpieken" as stamps/points for the zone-2 tile writer.

bouw_alles(W, K) builds everything and returns the POI dict (-> _cache/plekken_z2.json, Z2WereldPlekken.java, POI_Z2.md).
Builders work on vol.Vol (numpy volumes in world coordinates).
"""
import math
import random

import numpy as np

import kaart2 as K2
import plan as PL
from vol import CARVE, TEGEN, Vol, plaat, trap

LANT = "minecraft:lantern[hanging=false,waterlogged=false]"
LANT_H = "minecraft:lantern[hanging=true,waterlogged=false]"
VUUR = "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]"
ROOK = "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]"
SNEEUW = "minecraft:snow_block"
CHOCO = "pastelbaksteen_mokka"
ROOM = "pastelbaksteen_vanille"
SY = PL.STAD_Y            # town stand y (91)
FY = PL.STAD_TOP          # town floor block (90)
STAAF = "minecraft:spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]"
HEK_X = "minecraft:spruce_fence[east=true,north=false,south=false,waterlogged=false,west=true]"
HEK_Z = "minecraft:spruce_fence[east=false,north=true,south=true,waterlogged=false,west=false]"
DIR = {"noord": (0, -1), "zuid": (0, 1), "oost": (1, 0), "west": (-1, 0)}
FACE = {"noord": "north", "zuid": "south", "oost": "east", "west": "west"}


def laag(n):
    return f"minecraft:snow[layers={n}]"


NIET_VAST = ("grass", "flower", "tulip", "allium", "petals", "fern", "carpet", "lantern", "snow[", "mushroom[", "leaves", "cluster",
             "dripstone", "end_rod", "torch", "rail", "button", "pressure_plate", "ijsbloempje", "cave_air")


def hoogste(W, x, z, tot=300):
    """Highest solid block in this column (terrain + stamps): stand y - 1."""
    K = W.K
    best = K.h(x, z)
    i, j = K2.idx(x, z)
    if K.meer[i, j]:
        best = PL.MEER_IJS
    for st in W.stempels:
        ox, oy, oz = st.off
        X, Y, Z = st.v.shape
        if not (ox <= x < ox + X and oz <= z < oz + Z):
            continue
        col = st.lut[st.v[x - ox, :, z - oz]]
        nz = np.nonzero(col > 1)[0]
        for k in nz[::-1]:
            y = oy + int(k)
            if y > tot:
                continue
            if y <= best:
                break
            naam = W.palet.lijst[col[k]]
            if not any(t in naam for t in NIET_VAST):
                best = y
                break
    return best


def vrij(K, x, z, r=0):
    """Column free for decoration (no path, pad, town, lake, ravine)."""
    i, j = K2.idx(x, z)
    if not (0 <= i < PL.NX and 0 <= j < PL.NZ):
        return False
    sl = (slice(max(0, i - r), i + r + 1), slice(max(0, j - r), j + r + 1))
    return not (K.pad_m[sl] > 0.2).any() and not K.stad_vloer[sl].any() and not K.meer[sl].any() and not K.kloof[sl].any() \
        and K.H[i, j] > PL.ZEE + 2 and not K.kabel[sl].any()


def op_pad(K, x, z):
    i, j = K2.idx(x, z)
    for (px, pz, pr, ptop, fall) in K.pads:
        if math.hypot(x - px, z - pz) < pr + 4:
            return True
    return False


# ================================================================================================ Kandijkom
def poortboog(S, sx, sz, ux, uz, s0, hout=False):
    """(i-z2kaart) A complete gate arch over a 7-wide street: two 2x2 pillars (|p| 5..6) on a foundation, a rounded arch
    (upside-down stairs at the springing), crystal finials, a big lampion under the keystone and lampions on the pillars.
    hout=True: a festive wooden welcome arch (spruce, snow on the beam) instead of the calcite/crystal one."""
    vx, vz = abs(uz), abs(ux)               # across the street

    def w(p, s):
        return sx + ux * s + p * vx, sz + uz * s + p * vz
    kant_f = {(1, 0): ("west", "east"), (0, 1): ("north", "south")}[(vx, vz)]   # facing towards -p / +p
    body = "minecraft:spruce_log[axis=y]" if hout else "minecraft:calcite"
    rand_ = "minecraft:stripped_spruce_log[axis=y]" if hout else "minecraft:polished_diorite"
    boog = "minecraft:spruce_planks" if hout else "minecraft:calcite"
    trapb = "minecraft:spruce_stairs" if hout else "minecraft:diorite_stairs"
    hp = 7 if hout else 9                  # pillar height above the floor
    for sgn in (-1, 1):
        for p in (5, 6):
            for ds in (0, 1):
                x, z = w(sgn * p, s0 + ds)
                S.box(x, FY - 4, z, x, FY, z, "minecraft:stone_bricks")
                S.box(x, SY, z, x, SY + hp, z, body if (p == 5 and ds == 0) or hout else rand_)
                if not hout and p == 6:
                    S.set(x, SY, z, "minecraft:polished_diorite")
        # finial on the pillar
        x, z = w(sgn * 5.5, s0 + 0.5)
        if hout:
            for ds in (0, 1):
                xx, zz = w(sgn * 5, s0 + ds)
                S.set(xx, SY + hp + 1, zz, LAMPION_S)
        else:
            S.kegel(x, z, SY + hp + 1, 5, 1.3, 0.3, "minecraft:pink_stained_glass")
            S.kegel(x, z, SY + hp + 1, 4, 0.7, 0.2, "guhs:guh_kristal_blok")
            for ds in (0, 1):
                xx, zz = w(sgn * 7, s0 + ds)
                S.set(xx, SY + 5, zz, LAMPION_S) if ds == 0 else None
    # the arch: a beam over the street, rounded underneath
    yb = SY + hp - 1
    for p in range(-6, 7):
        for ds in (0, 1):
            x, z = w(p, s0 + ds)
            S.box(x, yb, z, x, yb + 1, z, boog)
            if abs(p) == 4:
                S.set(x, yb - 1, z, trap(trapb, kant_f[1] if p > 0 else kant_f[0], half="top"))
    if hout:
        for p in range(-6, 7):
            for ds in (0, 1):
                x, z = w(p, s0 + ds)
                S.set(x, yb + 2, z, laag(3))
        # a pink sign board on the outer face of the beam (the guhs know what it says: "welkom")
        for p in range(-3, 4):
            x, z = w(p, s0 + 2)
            S.set(x, yb, z, "minecraft:pink_wool" if abs(p) < 3 else "minecraft:white_wool")
            S.set(x, yb + 1, z, "minecraft:white_wool" if abs(p) == 3 else "minecraft:pink_wool")
        x, z = w(0, s0 + 2)
        S.set(x, yb + 1, z, "guhs:guh_kristal_lamp")
    else:
        # keystone crystal + a crown of little shards along the top
        x, z = w(0, s0)
        S.set(x, yb + 1, z, "guhs:guh_kristal_lamp")
        for p in range(-6, 7, 3):
            x, z = w(p, s0 + 0.5)
            S.kegel(x, z, yb + 2, 2 + (3 if p == 0 else 0), 0.9, 0.2, "minecraft:pink_stained_glass" if p else "guhs:guh_kristal_blok")
    for ds in (0, 1):
        x, z = w(0, s0 + ds)
        S.set(x, yb - 1, z, LAMPION)
    # a garland across the street on both faces (stone arch: high enough)
    for ds in (() if hout else (-1, 2)):
        a, b = w(-5, s0 + ds), w(5, s0 + ds)
        slinger(S, (a[0], yb - 1, a[1]), (b[0], yb - 1, b[1]), zak=1.5, elk=3)


MUURKLEUR = ["pastelbaksteen_roze", "pastelbaksteen_lavendel", "pastelbaksteen_hemelsblauw", "pastelbaksteen_perzik",
             "pastelbaksteen_framboos", "pastelbaksteen_orchidee", "pastelbaksteen_korenbloem", "pastelbaksteen_mokka",
             "pastelbaksteen_pistache", "pastelbaksteen_aqua"]
DAKKLEUR = ["minecraft:magenta_terracotta", "minecraft:purple_terracotta", "minecraft:light_blue_terracotta", "minecraft:pink_terracotta",
            "minecraft:red_terracotta", "minecraft:purple_terracotta", "minecraft:blue_terracotta", "minecraft:brown_terracotta",
            "minecraft:cyan_terracotta", "minecraft:light_blue_terracotta"]
VLOER = {"huis": "minecraft:spruce_planks", "winkel": "minecraft:birch_planks", "smid": "minecraft:polished_andesite",
         "cafe": "minecraft:dark_oak_planks", "tempel": None}
LAMPION = "elftocht_lampion[hanging=true,lit=true]"
LAMPION_S = "elftocht_lampion[hanging=false,lit=true]"
VUURKORF = "elftocht_vuurkorf[lit=true]"


def kopjes(f):
    return f"elftocht_kopjes[facing={f},soort=chocovet]"


class Kader:
    """Local frame of a building: u across the front (door side), v depth from the front wall (0) to the back wall (D-1)."""

    def __init__(self, cx, cz, w, d, deur):
        self.dx, self.dz = DIR[deur]
        self.ux, self.uz = -self.dz, self.dx
        self.W = d if self.dx else w          # width across the front
        self.D = w if self.dx else d          # depth
        x1, z1 = cx - w // 2, cz - d // 2
        x2, z2 = x1 + w - 1, z1 + d - 1
        if self.dz:
            self.fx, self.fz = cx, (z1 if self.dz < 0 else z2)
        else:
            self.fx, self.fz = (x1 if self.dx < 0 else x2), cz
        self.voor = FACE[deur]                 # world facing towards the front
        self.achter = TEGEN[self.voor]
        self.hw = self.W // 2 - 1              # max |u| inside

    def p(self, u, v):
        return self.fx + u * self.ux - v * self.dx, self.fz + u * self.uz - v * self.dz

    def kant(self, teken):
        """World facing of +u (teken 1) / -u (teken -1)."""
        vx, vz = self.ux * teken, self.uz * teken
        return {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(vx, vz)]


def huis(S, P, gid, cx, cz, w, d, deur, soort, naam, fy, kleur=0):
    """A cosy Kandijkom house/shop on floor top fy: pastel walls, a coloured roof with a dusting of snow, a real interior."""
    sy = fy + 1
    x1, z1 = cx - w // 2, cz - d // 2
    x2, z2 = x1 + w - 1, z1 + d - 1
    hw = {"huis": 5, "winkel": 6, "smid": 6, "cafe": 6, "tempel": 10}[soort]
    top = sy + hw
    tempel = soort == "tempel"
    wand = "minecraft:calcite" if tempel else MUURKLEUR[kleur % len(MUURKLEUR)]
    dak = "minecraft:light_gray_terracotta" if tempel else DAKKLEUR[kleur % len(DAKKLEUR)]
    hoek = "guhs:guh_kristal_blok" if tempel else "minecraft:dark_oak_log[axis=y]"
    S.box(x1 - 1, fy - 5, z1 - 1, x2 + 1, fy, z2 + 1, "minecraft:stone_bricks")       # foundation (also on a tier edge)
    S.box(x1, sy, z1, x2, top, z2, wand)
    S.box(x1 + 1, sy, z1 + 1, x2 - 1, top - 1, z2 - 1, CARVE)
    for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
        S.box(x, sy, z, x, top, z, hoek)
    S.box(x1, sy, z1, x2, sy, z2, "minecraft:stone_bricks" if not tempel else "minecraft:polished_diorite")
    S.box(x1 + 1, sy, z1 + 1, x2 - 1, sy, z2 - 1, CARVE)
    if tempel:
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                S.set(x, fy, z, "minecraft:smooth_quartz" if (x + z) % 2 else "minecraft:pink_terracotta")
    elif soort == "cafe":
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                S.set(x, fy, z, "minecraft:dark_oak_planks" if (x + z) % 2 else "minecraft:spruce_planks")
    else:
        S.box(x1 + 1, fy, z1 + 1, x2 - 1, fy, z2 - 1, VLOER[soort])
    # windows (warm light)
    glas = "minecraft:pink_stained_glass" if tempel else "minecraft:orange_stained_glass"
    for x in range(x1 + 2, x2 - 1, 3):
        for z in (z1, z2):
            S.box(x, sy + 1, z, x, sy + (6 if tempel else 2), z, glas)
    for z in range(z1 + 2, z2 - 1, 3):
        for x in (x1, x2):
            S.box(x, sy + 1, z, x, sy + (6 if tempel else 2), z, glas)
    F = Kader(cx, cz, w, d, deur)
    dx, dz = F.dx, F.dz
    breed = 2 if soort in ("winkel", "cafe", "smid", "tempel") else 1
    us = [0] if breed == 1 else [-1, 0]
    # door frame + doors
    for u in range(us[0] - 1, us[-1] + 2):
        x, z = F.p(u, 0)
        S.box(x, sy, z, x, sy + 3, z, "minecraft:dark_oak_planks" if not tempel else "minecraft:polished_diorite")
    for i, u in enumerate(us):
        x, z = F.p(u, 0)
        hinge = "left" if i == 0 else "right"
        f = F.achter
        S.set(x, sy, z, f"minecraft:spruce_door[facing={f},half=lower,hinge={hinge},open=false,powered=false]")
        S.set(x, sy + 1, z, f"minecraft:spruce_door[facing={f},half=upper,hinge={hinge},open=false,powered=false]")
        S.set(x + dx, fy, z + dz, "guhs:knuffelklinkers")
        S.box(x + dx, sy, z + dz, x + dx, sy + 2, z + dz, CARVE)
        S.box(x + 2 * dx, sy, z + 2 * dz, x + 2 * dx, sy + 2, z + 2 * dz, CARVE)
        xi, zi = F.p(u, 1)
        S.set(xi, fy, zi, "minecraft:spruce_planks" if not tempel else "minecraft:smooth_quartz")
    # a lamp beside the door outside + a wreath above it
    xa, za = F.p(us[-1] + 2, -1)
    S.set(xa, sy + 2, za, LANT_H) if False else None
    xw, zw = F.p(us[0] - 1, -1)
    S.set(xw, sy + 2, zw, LANT, keep=True)
    buiten = list(F.p(0, -2))
    buiten = [buiten[0], sy, buiten[1]]
    # lights: hanging lanterns from the ceiling
    for x in range(x1 + 2, x2 - 1, 4):
        for z in range(z1 + 2, z2 - 1, 4):
            S.set(x, top - 1, z, LANT_H)
    # roof: coloured tiles, a thin dusting of snow, icicles (end rods) under the eaves
    langs_x = w >= d
    a0, a1 = (z1 - 1, z2 + 1) if langs_x else (x1 - 1, x2 + 1)
    half = (a1 - a0) // 2 + 1
    for k in range(half + 1):
        y = top + 1 + k
        lo, hi = a0 + k, a1 - k
        if lo > hi:
            break
        for u in (lo, hi):
            if langs_x:
                S.box(x1 - 1, y, u, x2 + 1, y, u, dak)
            else:
                S.box(u, y, z1 - 1, u, y, z2 + 1, dak)
        if langs_x:
            S.box(x1, y, lo + 1, x2, y, hi - 1, wand)
            S.box(x1 + 1, y, lo + 1, x2 - 1, y, hi - 1, CARVE)
        else:
            S.box(lo + 1, y, z1, hi - 1, y, z2, wand)
            S.box(lo + 1, y, z1 + 1, hi - 1, y, z2 - 1, CARVE)
    S.box(x1 + 1, top, z1 + 1, x2 - 1, top, z2 - 1, "minecraft:spruce_planks" if not tempel else "minecraft:calcite")
    if langs_x:
        for x in range(x1 - 1, x2 + 2):
            S.set(x, top, z1 - 1, trap("minecraft:dark_oak_stairs", "south"))
            S.set(x, top, z2 + 1, trap("minecraft:dark_oak_stairs", "north"))
            if x % 3 == 0:
                S.set(x, top - 1, z1 - 1, "minecraft:end_rod[facing=down]")
                S.set(x, top - 1, z2 + 1, "minecraft:end_rod[facing=down]")
            if x % 2 == 0:
                S.set(x, top - 1, z1 - 1, LAMPION, keep=True)
    else:
        for z in range(z1 - 1, z2 + 2):
            S.set(x1 - 1, top, z, trap("minecraft:dark_oak_stairs", "east"))
            S.set(x2 + 1, top, z, trap("minecraft:dark_oak_stairs", "west"))
            if z % 3 == 0:
                S.set(x1 - 1, top - 1, z, "minecraft:end_rod[facing=down]")
                S.set(x2 + 1, top - 1, z, "minecraft:end_rod[facing=down]")
    P_ = {"deur": buiten, "binnen": [cx, sy, cz], "doos": [[x1, fy, z1], [x2, top, z2]], "naam": naam}
    D, hwu = F.D, F.hw
    # ---- the fireplace (houses, cafe): in the back wall (or a side wall for shops), a chimney with smoke
    def haard(u, v, kant_naar):
        """Fireplace in the wall cell (u, v) opening towards kant_naar (world facing)."""
        x, z = F.p(u, v)
        S.box(x - 1, sy - 1, z - 1, x + 1, sy + 3, z + 1, "minecraft:bricks", keep=False) if False else None
        S.set(x, sy, z, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
        S.set(x, sy + 1, z, "minecraft:bricks")
        S.box(x, sy + 2, z, x, top + half + 3, z, "minecraft:bricks")
        S.set(x, top + half + 4, z, ROOK)
        S.set(x, fy, z, "minecraft:bricks")
        for du in (-1, 1):
            xx, zz = F.p(u + du, v) if v in (0, D - 1) else F.p(u, v + du)
            S.box(xx, sy, zz, xx, sy + 2, zz, "minecraft:bricks")
        return x, z
    if soort in ("huis", "cafe"):
        haard(0, D - 1, F.voor)
        # behind the fireplace: solid, so the fire never shows outside
        xb, zb = F.p(0, D)
        S.box(xb, sy, zb, xb, sy + 1, zb, "minecraft:bricks", keep=True)
        xr, zr = F.p(0, D - 2)
        S.set(xr, sy, zr, "minecraft:red_carpet")
    if soort in ("winkel", "smid", "cafe"):
        # counter across v = D-4, the NPC behind it at v = D-3, shelves along the back (v = D-2)
        vt, vn, vs = D - 4, D - 3, D - 2
        for u in range(-hwu + 1, hwu):
            x, z = F.p(u, vt)
            S.set(x, sy, z, "minecraft:barrel[facing=up,open=false]" if soort != "cafe" else "minecraft:dark_oak_planks")
            S.set(x, sy + 1, z, plaat("minecraft:spruce_slab"))
        x, z = F.p(-hwu, vt)
        S.set(x, sy, z, f"minecraft:spruce_trapdoor[facing={F.kant(-1)},half=bottom,open=true,powered=false,waterlogged=false]")
        for u in range(-hwu, hwu + 1):
            if soort == "cafe" and u == 0:
                continue
            x, z = F.p(u, vs)
            if soort == "smid":
                S.set(x, sy, z, "minecraft:stone_bricks")
            else:
                S.box(x, sy, z, x, sy + 1, z, "minecraft:bookshelf" if (u % 2 == 0 and soort == "winkel") else "minecraft:barrel[facing=up,open=false]")
                S.set(x, sy + 2, z, "minecraft:flower_pot" if u % 3 == 0 else "minecraft:candle[candles=3,lit=true,waterlogged=false]")
        xn, zn = F.p(0, vn)
        P_["npc"] = [xn, sy, zn]
        xt, zt = F.p(0, vt - 1)
        P_["toonbank"] = [xt, sy, zt]
        if soort == "smid":
            x, z = F.p(2, vt - 2)
            S.set(x, sy, z, "minecraft:anvil[facing=north]")
            x, z = F.p(-hwu, 1)
            S.set(x, sy, z, "minecraft:blast_furnace[facing=" + F.kant(1) + ",lit=true]")
            x, z = F.p(hwu, 1)
            S.set(x, sy, z, "minecraft:smithing_table")
            x, z = F.p(hwu, 2)
            S.set(x, sy, z, "minecraft:grindstone[face=floor,facing=north]")
            x, z = F.p(-hwu, 2)
            S.set(x, sy, z, "minecraft:furnace[facing=" + F.kant(1) + ",lit=true]")
            x, z = F.p(-hwu, 3)
            S.set(x, sy, z, "minecraft:lava_cauldron") if False else S.set(x, sy, z, "minecraft:water_cauldron[level=3]")
        elif soort == "winkel":
            # a warm stove in the corner, a little rug, a bench by the window
            x, z = F.p(-hwu, 1)
            S.set(x, sy, z, "minecraft:smoker[facing=" + F.kant(1) + ",lit=true]")
            x, z = F.p(-hwu, 2)
            S.set(x, sy, z, "minecraft:water_cauldron[level=3]")
            S.set(x, sy + 1, z, kopjes(F.voor)) if False else None
            for v in range(1, vt - 1):
                for u in range(-1, 2):
                    x, z = F.p(u, v)
                    S.set(x, sy, z, "minecraft:pink_carpet" if (u + v) % 2 else "minecraft:white_carpet", keep=True)
            x, z = F.p(hwu, 1)
            S.set(x, sy, z, trap("minecraft:spruce_stairs", F.kant(1)))
            x, z = F.p(hwu, 2)
            S.set(x, sy, z, trap("minecraft:spruce_stairs", F.kant(1)))
        else:
            # cafe: tables with chairs in the front half, chocolate kettles + cake on the counter
            for (u, v) in ((-hwu + 1, 2), (hwu - 2, 2)):
                if v >= vt - 1:
                    continue
                x, z = F.p(u, v)
                S.set(x, sy, z, STAAF)
                S.set(x, sy + 1, z, kopjes(F.voor))
                for du in (-1, 1):
                    xx, zz = F.p(u + du, v)
                    S.set(xx, sy, zz, trap("minecraft:spruce_stairs", F.kant(-du)))
            for u in (-hwu, hwu):
                x, z = F.p(u, vs)
                S.set(x, sy, z, "minecraft:water_cauldron[level=3]")
            x, z = F.p(1, vt)
            S.set(x, sy + 1, z, "minecraft:cake[bites=0]")
            x, z = F.p(-1, vt)
            S.set(x, sy + 1, z, kopjes(F.voor))
            P_["geheim_kelder"] = None
    elif soort == "huis":
        # bed with a blanket, bookcase, table with chocomelk, a kettle by the fire, a rug
        bu, bv = -hwu, D - 2
        x, z = F.p(bu, bv)
        x2_, z2_ = F.p(bu, bv - 1)
        S.set(x, sy, z, f"minecraft:pink_bed[facing={F.achter},occupied=false,part=head]")
        S.set(x2_, sy, z2_, f"minecraft:pink_bed[facing={F.achter},occupied=false,part=foot]")
        x, z = F.p(bu, bv - 2)
        S.set(x, sy, z, "minecraft:chest[facing=" + F.kant(1) + ",type=single,waterlogged=false]")
        for v in range(2, D - 2):
            x, z = F.p(hwu, v)
            S.box(x, sy, z, x, sy + 1, z, "minecraft:bookshelf")
        x, z = F.p(hwu, 1)
        S.set(x, sy, z, "minecraft:potted_red_tulip")
        tu, tv = 1, 2
        x, z = F.p(tu, tv)
        S.set(x, sy, z, STAAF)
        S.set(x, sy + 1, z, kopjes(F.voor))
        x, z = F.p(tu - 1, tv)
        S.set(x, sy, z, trap("minecraft:spruce_stairs", F.kant(-1)))
        x, z = F.p(1, D - 2)
        S.set(x, sy, z, "minecraft:water_cauldron[level=3]")
        S.set(x, sy + 1, z, "minecraft:candle[candles=2,lit=true,waterlogged=false]") if False else None
        x, z = F.p(-1, D - 2)
        S.set(x, sy, z, "minecraft:barrel[facing=up,open=false]")
        for v in range(1, D - 1):
            for u in range(-1, 1):
                x, z = F.p(u, v)
                S.set(x, sy, z, "minecraft:red_carpet" if (u + v) % 2 else "minecraft:white_carpet", keep=True)
    elif tempel:
        ax, az = F.p(0, D - 2)
        x0_, z0_ = F.p(-1, D - 2)
        x1_, z1_ = F.p(1, D - 2)
        S.set(x0_, sy, z0_, "minecraft:polished_diorite")
        S.set(x1_, sy, z1_, "minecraft:polished_diorite")
        S.set(ax, sy, az, "minecraft:polished_diorite")
        S.set(ax, sy + 1, az, "minecraft:amethyst_block")
        S.set(ax, sy + 2, az, "guhs:guh_kristal_cluster[facing=up]")
        S.set(x0_, sy + 1, z0_, "minecraft:end_rod[facing=up]")
        S.set(x1_, sy + 1, z1_, "minecraft:end_rod[facing=up]")
        for u in (-hwu, hwu):
            for v in range(2, D - 2, 3):
                x, z = F.p(u, v)
                S.box(x, sy, z, x, top - 1, z, "guhs:guh_kristal_blok")
        S.box(x1 + 1, top, z1 + 1, x2 - 1, top, z2 - 1, "minecraft:calcite")
        S.set(cx, top, cz, "guhs:guh_kristal_lamp")
        xn, zn = F.p(0, D - 4)
        P_["npc"] = [xn, sy, zn]
        P_["altaar"] = [ax, sy, az]
        S.kegel(cx, cz, top + half + 1, 18, 3.0, 0.4, "minecraft:pink_stained_glass")
        S.kegel(cx, cz, top + half + 1, 13, 1.4, 0.2, "guhs:guh_kristal_blok")
        S.set(cx, top + half + 15, cz, "guhs:guh_kristal_lamp")
    return P_


def lantaarnpaal(S, x, y, z, lampion=False):
    S.box(x, y, z, x, y + 2, z, STAAF)
    S.set(x, y + 3, z, LANT if not lampion else LAMPION_S)


def slinger(S, a, b, zak=3.0, elk=4):
    """A garland of lanterns sagging between two points (a, b = world (x, y, z))."""
    n = int(max(abs(b[0] - a[0]), abs(b[2] - a[2]))) + 1
    if n < 2:
        return
    as_ = "x" if abs(b[0] - a[0]) >= abs(b[2] - a[2]) else "z"
    for i in range(n + 1):
        t = i / n
        x = a[0] + (b[0] - a[0]) * t
        z = a[2] + (b[2] - a[2]) * t
        y = a[1] + (b[1] - a[1]) * t - zak * math.sin(math.pi * t)
        S.set(round(x), round(y), round(z), f"feestslingers[axis={as_}]", keep=True)
        if i % elk == 2:
            S.set(round(x), round(y) - 1, round(z), LAMPION, keep=True)


def kraam(S, x, z, f, fy, dak1="minecraft:red_wool", dak2="minecraft:white_wool", waar=None):
    """A Christmas-market stall (3 wide, 2 deep): counter facing f, a striped awning, a lamp, goods on the counter."""
    sy = fy + 1
    ddx, ddz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[f]
    ux, uz = -ddz, ddx
    for u in (-1, 0, 1):
        cxx, czz = x + u * ux, z + u * uz
        S.set(cxx, sy, czz, "minecraft:spruce_planks" if u else "minecraft:barrel[facing=up,open=false]")
        S.set(cxx, sy + 1, czz, plaat("minecraft:spruce_slab"))
        S.set(cxx - ddx, fy, czz - ddz, "minecraft:spruce_planks")
    for u in (-2, 2):
        for vv in (0, -2):
            px, pz = x + u * ux + vv * ddx, z + u * uz + vv * ddz
            S.box(px, sy, pz, px, sy + 2, pz, STAAF)
    for u in range(-2, 3):
        for vv in (1, 0, -1, -2):
            px, pz = x + u * ux + vv * ddx, z + u * uz + vv * ddz
            S.set(px, sy + 3 + (1 if vv <= -1 else 0), pz, dak1 if (u % 2 == 0) else dak2)
    S.set(x - ddx, sy + 2, z - ddz, LAMPION)
    goed = waar or [kopjes(f), "minecraft:cake[bites=0]", "minecraft:candle[candles=4,lit=true,waterlogged=false]"]
    for u, g in zip((-1, 0, 1), goed):
        S.set(x + u * ux, sy + 2, z + u * uz, g, keep=True)


def stad(W, K, P):
    sx, sz = PL.STAD
    R0 = PL.GEODE_R
    S = Vol(sx - R0 - 30, FY - 10, sz - R0 - 30, sx + R0 + 30, FY + 90, sz + R0 + 30)
    gb = {}
    weg = "guhs:knuffelklinkers"
    # ---- gate streets (through the wall) and the plaza
    for naam, (ux, uz) in PL.POORTEN.items():
        a = (sx + ux * PL.PLEIN_R - 3 * abs(uz), sz + uz * PL.PLEIN_R - 3 * abs(ux))
        b = (sx + ux * (R0 + PL.GEODE_DIK + 6) + 3 * abs(uz), sz + uz * (R0 + PL.GEODE_DIK + 6) + 3 * abs(ux))
        S.box(a[0], FY, a[1], b[0], FY, b[1], weg)
        # lamp posts with lampions along the street, both sides
        for s in range(PL.PLEIN_R + 4, R0 - 2, 8):
            for p in (-4, 4):
                x, z = sx + ux * s + p * abs(uz), sz + uz * s + p * abs(ux)
                lantaarnpaal(S, x, SY, z, lampion=True)
        # (i-z2kaart) a real gate arch on the inner face of the wall (was: a strip of pink glass floating over the gap)
        poortboog(S, sx, sz, ux, uz, R0 - 1)
    S.cyl(sx, sz, PL.PLEIN_R, FY, FY, "parelmoer_tegels")
    S.cyl(sx, sz, PL.PLEIN_R, FY, FY, "minecraft:polished_diorite", hollow=1.2)
    # ---- the skating pond, benches with blankets, snow-guhs
    vx, vz = PL.VIJVER
    S.cyl(vx, vz, PL.VIJVER_R + 1.5, FY, FY, SNEEUW)
    S.cyl(vx, vz, PL.VIJVER_R, FY, FY, "minecraft:packed_ice")
    for (dx, dz, f) in ((-10, 0, "east"), (10, 0, "west")):
        for k in (-1, 0, 1):
            S.set(vx + dx, SY, vz + dz + k, trap("minecraft:spruce_stairs", f))
    for (x, z, f) in ((vx - 9, vz + 6, "north"), (vx + 9, vz + 6, "north")):
        S.set(x, SY, z, f"guhs:sneeuwpopguh[facing={f},half=lower]")
        S.set(x, SY + 1, z, f"guhs:sneeuwpopguh[facing={f},half=upper]")
    # ---- the crystal clock tower in the middle (walk-through arches on all sides)
    tx, tz = PL.TOREN
    th = 34
    S.box(tx - 3, FY - 3, tz - 3, tx + 3, FY, tz + 3, "minecraft:stone_bricks")
    S.box(tx - 3, SY, tz - 3, tx + 3, SY + th, tz + 3, "pastelbaksteen_roze")
    S.box(tx - 2, SY, tz - 2, tx + 2, SY + 4, tz + 2, CARVE)
    S.box(tx - 1, SY, tz - 3, tx + 1, SY + 3, tz + 3, CARVE)
    S.box(tx - 3, SY, tz - 1, tx + 3, SY + 3, tz + 1, CARVE)
    S.box(tx - 2, FY, tz - 2, tx + 2, FY, tz + 2, "parelmoer_tegels")
    S.set(tx, SY + 4, tz, LAMPION)
    for (x, z) in ((tx - 3, tz - 3), (tx + 3, tz - 3), (tx - 3, tz + 3), (tx + 3, tz + 3)):
        S.box(x, SY, z, x, SY + th, z, "guhs:guh_kristal_blok")
    for y in range(SY + 8, SY + th - 6, 6):
        for (x, z) in ((tx, tz - 3), (tx, tz + 3), (tx - 3, tz), (tx + 3, tz)):
            S.box(x, y, z, x, y + 2, z, "minecraft:pink_stained_glass")
    # clock faces (4 sides) + a balcony with lampions
    yk = SY + th - 6
    for (f, x, z) in (("south", tx, tz + 4), ("north", tx, tz - 4), ("east", tx + 4, tz), ("west", tx - 4, tz)):
        S.set(x, yk, z, f"guhklok[facing={f},signaal=false,snoet=werkt]")
        ddx, ddz = (x - tx) // 4, (z - tz) // 4
        for k in (-1, 1):
            S.set(x + k * abs(ddz), yk, z + k * abs(ddx), "minecraft:smooth_quartz")
            S.set(x + k * abs(ddz), yk + 1, z + k * abs(ddx), "minecraft:smooth_quartz")
            S.set(x + k * abs(ddz), yk - 1, z + k * abs(ddx), "minecraft:smooth_quartz")
        S.set(x, yk + 1, z, "minecraft:smooth_quartz")
        S.set(x, yk - 1, z, "minecraft:smooth_quartz")
    S.box(tx - 4, SY + th + 1, tz - 4, tx + 4, SY + th + 1, tz + 4, "minecraft:dark_oak_planks")
    for k in range(-4, 5):
        for (x, z) in ((tx + k, tz - 4), (tx + k, tz + 4), (tx - 4, tz + k), (tx + 4, tz + k)):
            S.set(x, SY + th + 2, z, STAAF)
            if k % 2 == 0:
                S.set(x, SY + th, z, LAMPION, keep=True)
    S.kegel(tx, tz, SY + th + 2, 26, 3.6, 0.4, "minecraft:pink_stained_glass")
    S.kegel(tx, tz, SY + th + 2, 22, 2.0, 0.3, "guhs:guh_kristal_blok")
    S.set(tx, SY + th + 24, tz, "guhs:guh_kristal_lamp")
    S.set(tx, SY + th + 28, tz, "minecraft:amethyst_block")
    S.set(tx, SY + th + 29, tz, "guhs:guh_kristal_cluster[facing=up]")
    for k in range(4):     # little side crystals around the spire
        a = k * math.pi / 2 + math.pi / 4
        S.kegel(tx + 3.2 * math.cos(a), tz + 3.2 * math.sin(a), SY + th + 2, 9, 1.2, 0.2,
                ["minecraft:light_blue_stained_glass", "minecraft:purple_stained_glass"][k % 2], scheef=(1.5 * math.cos(a), 1.5 * math.sin(a)))
    toptoren = (tx, SY + th + 1, tz)
    # ---- the big decorated tree with presents
    bx, bz = PL.KERSTBOOM
    S.box(bx, SY, bz, bx, SY + 22, bz, "minecraft:spruce_log[axis=y]")
    rng = random.Random(4)
    versier = ["minecraft:shroomlight", "minecraft:pink_concrete", "minecraft:light_blue_concrete", "minecraft:gold_block",
               "minecraft:purple_concrete", "minecraft:sea_lantern", "minecraft:red_concrete"]
    for k in range(20):
        r = max(0.8, 6.2 * (1 - k / 21))
        y = SY + 3 + k
        S.cyl(bx, bz, r, y, y, "minecraft:spruce_leaves[distance=1,persistent=true,waterlogged=false]", keep=True)
    for k in range(40):
        a = k * 2.4
        kk = k * 0.5
        r = max(1.0, 6.4 * (1 - kk / 21))
        S.set(round(bx + r * math.cos(a)), SY + 3 + int(kk), round(bz + r * math.sin(a)), versier[k % len(versier)])
    S.set(bx, SY + 23, bz, "minecraft:gold_block")
    S.set(bx, SY + 24, bz, "guhs:guh_kristal_cluster[facing=up]")
    for k in range(7):
        a = k * 2 * math.pi / 7
        x, z = round(bx + 7.5 * math.cos(a)), round(bz + 7.5 * math.sin(a))
        S.set(x, SY, z, ["minecraft:red_wool", "minecraft:pink_wool", "minecraft:light_blue_wool", "minecraft:lime_wool"][k % 4])
        S.set(x, SY + 1, z, "minecraft:white_carpet")
    # ---- garlands of lampions from the tower balcony down to the plaza lamp posts
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        x, z = round(sx + (PL.PLEIN_R - 2) * math.cos(a)), round(sz + (PL.PLEIN_R - 2) * math.sin(a))
        lantaarnpaal(S, x, SY, z, lampion=True)
        slinger(S, (tx + round(4 * math.cos(a)), SY + th, tz + round(4 * math.sin(a))), (x, SY + 3, z), zak=2.5, elk=4)
    # ---- the Christmas market: stalls round the plaza, a hot-chocolate stand, fire baskets
    kramen = []
    for k, ag in enumerate((40, 70, 110, 140, 220, 250, 290, 320)):
        a = math.radians(ag)
        x, z = round(sx + 18 * math.cos(a)), round(sz + 18 * math.sin(a))
        if math.hypot(x - bx, z - bz) < 9 or math.hypot(x - vx, z - vz) < PL.VIJVER_R + 4 or math.hypot(x - tx, z - tz) < 7:
            continue
        ddx, ddz = sx - x, sz - z
        f = ("east" if ddx > 0 else "west") if abs(ddx) > abs(ddz) else ("south" if ddz > 0 else "north")
        kl = [("minecraft:red_wool", "minecraft:white_wool"), ("minecraft:pink_wool", "minecraft:white_wool"),
              ("minecraft:light_blue_wool", "minecraft:white_wool"), ("minecraft:purple_wool", "minecraft:pink_wool")][k % 4]
        kraam(S, x, z, f, FY, kl[0], kl[1])
        bdx, bdz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[f]
        kramen.append([x - bdx, SY, z - bdz])        # the vendor's spot behind the counter
    # the hot-chocolate stand by the pond (big kettle)
    cx_, cz_ = vx + 13, vz + 2
    kraam(S, cx_, cz_, "west", FY, "minecraft:brown_wool", "minecraft:pink_wool",
          [kopjes("west"), "minecraft:water_cauldron[level=3]", kopjes("west")])
    S.set(cx_ + 2, SY, cz_, "minecraft:water_cauldron[level=3]")
    S.set(cx_ + 2, FY, cz_, "minecraft:magma_block") if False else S.set(cx_ + 2, FY, cz_, "minecraft:bricks")
    P["chocomelkkraam"] = [cx_ - 2, SY, cz_]
    for a in range(0, 360, 60):
        x, z = round(sx + 23.5 * math.cos(math.radians(a + 15))), round(sz + 23.5 * math.sin(math.radians(a + 15)))
        if any(abs((x - sx) * uz_ - (z - sz) * ux_) <= 4 and (x - sx) * ux_ + (z - sz) * uz_ > 0 for (ux_, uz_) in PL.POORTEN.values()):
            continue
        S.set(x, SY, z, VUURKORF)
    # ---- buildings
    for n, (gid, (cx, cz, w, d, deur, soort, naam, fy)) in enumerate(PL.GEBOUWEN.items()):
        gb[gid] = huis(S, P, gid, cx, cz, w, d, deur, soort, naam, fy, kleur=n * 3)
    # ---- the hidden chocolate cellar under the cafe (ladder behind the counter)
    cx, cz, w, d, deur = PL.GEBOUWEN["cafe"][:5]
    F = Kader(cx, cz, w, d, deur)
    kx1, kz1, kx2, kz2 = cx - 5, cz - 3, cx + 5, cz + 3
    S.box(kx1 - 1, FY - 7, kz1 - 1, kx2 + 1, FY - 1, kz2 + 1, "minecraft:bricks")
    S.box(kx1, FY - 5, kz1, kx2, FY - 2, kz2, CARVE)
    S.box(kx1, FY - 6, kz1, kx2, FY - 6, kz2, "minecraft:spruce_planks")
    S.box(kx1 + 1, FY - 5, kz1 + 1, kx2 - 3, FY - 5, kz2 - 1, "minecraft:brown_carpet")
    hx, hz = F.p(F.hw - 1, F.D - 3)       # behind the counter (NPC row), near the side
    sup = F.p(F.hw - 1, F.D - 2)
    S.box(sup[0], FY - 5, sup[1], sup[0], FY, sup[1], "minecraft:spruce_planks")
    for y in range(FY - 5, FY + 1):
        S.set(hx, y, hz, f"minecraft:ladder[facing={F.voor},waterlogged=false]")
    S.set(kx1, FY - 5, kz1, "minecraft:water_cauldron[level=3]")
    S.set(kx1 + 1, FY - 5, kz1, "minecraft:water_cauldron[level=3]")
    S.set(kx2, FY - 5, kz1, "minecraft:barrel[facing=up,open=false]")
    S.set(kx1, FY - 5, kz2, "minecraft:barrel[facing=up,open=false]")
    S.set(cx, FY - 2, cz, LANT_H)
    S.set(cx + 3, FY - 2, cz, LANT_H)
    P["geheim_chocokelder"] = [cx - 2, FY - 5, cz]
    # the cafe terrace in front (on the plaza side), tables with parasols and blankets
    tz0 = cz - d // 2 - 4
    for x_ in (cx - 4, cx + 4):
        S.set(x_, SY, tz0, STAAF)
        S.set(x_, SY + 1, tz0, STAAF)
        S.set(x_, SY + 2, tz0, "minecraft:pink_wool")
        S.box(x_ - 1, SY + 2, tz0 - 1, x_ + 1, SY + 2, tz0 + 1, "minecraft:pink_carpet", keep=True)
        S.set(x_ + 1, SY, tz0, trap("minecraft:spruce_stairs", "east"))
        S.set(x_ - 1, SY, tz0, trap("minecraft:spruce_stairs", "west"))
    P["cafe_terras"] = [cx, SY, tz0]
    # ---- quest board
    qx, qz = PL.QUESTBORD
    S.box(qx - 2, SY, qz, qx - 2, SY + 2, qz, "minecraft:spruce_log[axis=y]")
    S.box(qx + 2, SY, qz, qx + 2, SY + 2, qz, "minecraft:spruce_log[axis=y]")
    S.box(qx - 1, SY + 1, qz, qx + 1, SY + 2, qz, "minecraft:birch_planks")
    S.box(qx - 2, SY + 3, qz, qx + 2, SY + 3, qz, plaat("minecraft:dark_oak_slab"))
    S.set(qx, SY + 3, qz, LAMPION_S) if False else None
    # ---- the tiers: stairs on the lanes, railings on the tier edges, bridges over the gate streets at tier B
    H = K.H
    i0, j0 = K2.idx(sx - R0, sz - R0)
    richt = {1: ("south", 0, 1), 2: ("east", 1, 0), 3: ("west", -1, 0), 4: ("north", 0, -1)}
    namen = list(PL.POORTEN.keys())
    for i in range(2 * R0 + 1):
        for j in range(2 * R0 + 1):
            ii, jj = i0 + i, j0 + j
            x, z = ii + PL.X0, jj + PL.Z0
            if not K.stad_vloer[ii, jj]:
                continue
            h = int(H[ii, jj])
            if K.trap_baan[ii, jj]:
                k = int(K.trap_richting[ii, jj])
                ux, uz = PL.POORTEN[namen[k - 1]]
                hp = int(H[ii - ux, jj - uz])
                f = FACE[namen[k - 1]]
                if hp == h - 1:
                    S.set(x, h, z, trap("minecraft:spruce_stairs", f))
                    S.box(x, h - 6, z, x, h - 1, z, "minecraft:stone_bricks")
                else:
                    S.set(x, h, z, "minecraft:spruce_planks")
                # railing on the street side of the lane
                for (nx, nz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    hn = int(H[ii + nx, jj + nz])
                    if K.straat[ii + nx, jj + nz] and h - hn >= 2 and not K.trap_baan[ii + nx, jj + nz]:
                        S.set(x + nx, h + 1, z + nz, CARVE) if False else None
                        S.set(x, h + 1, z, STAAF if hp != h - 1 else CARVE) if False else None
                continue
            if not K.tier[ii, jj]:
                continue
            # railing where the tier drops by >= 2 to a non-lane neighbour
            for (nx, nz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                hn = int(H[ii + nx, jj + nz])
                if h - hn >= 2 and not K.trap_baan[ii + nx, jj + nz]:
                    S.set(x, h + 1, z, STAAF, keep=True)
                    break
    # bridges over the 4 gate streets at tier B (deck at 100, headroom 9 over the street)
    yb = PL.TIER_B[2]
    for naam, (ux, uz) in PL.POORTEN.items():
        if naam == "zuid":
            continue
        for s in (57, 58, 59):
            for p in range(-4, 5):
                x, z = sx + ux * s + p * abs(uz), sz + uz * s + p * abs(ux)
                S.set(x, yb, z, "minecraft:spruce_planks")
                S.set(x, yb - 1, z, "minecraft:dark_oak_slab[type=top,waterlogged=false]")
        for p in range(-4, 5):
            for s in (56, 60):
                x, z = sx + ux * s + p * abs(uz), sz + uz * s + p * abs(ux)
                S.set(x, yb + 1, z, STAAF)
        x, z = sx + ux * 58 - 4 * abs(uz), sz + uz * 58 - 4 * abs(ux)
        slinger(S, (sx + ux * 56 - 4 * abs(uz), yb + 3, sz + uz * 56 - 4 * abs(ux)), (sx + ux * 56 + 4 * abs(uz), yb + 3, sz + uz * 56 + 4 * abs(ux)), zak=1, elk=3)
        for p in (-4, 4):
            lantaarnpaal(S, sx + ux * 60 + p * abs(uz), yb + 1, sz + uz * 60 + p * abs(ux), lampion=True)
    # ---- the geode crystals: big shards (pink, purple, blue) leaning out of the inner wall, glowing cores
    rng = random.Random(4)
    gates = [math.atan2(dz, dx) for (dx, dz) in PL.POORTEN.values()]
    kleuren = [("minecraft:pink_stained_glass", "guhs:guh_kristal_blok"), ("minecraft:purple_stained_glass", "minecraft:amethyst_block"),
               ("minecraft:light_blue_stained_glass", "minecraft:blue_ice"), ("minecraft:magenta_stained_glass", "guhs:guh_kristal_blok")]
    for k in range(0, 360, 9):
        a = math.radians(k + rng.uniform(-3, 3))
        if any(abs(math.atan2(math.sin(a - g), math.cos(a - g))) < 0.22 for g in gates):
            continue
        r = R0 + 2.5
        bx_, bz_ = sx + r * math.cos(a), sz + r * math.sin(a)
        y0 = PL.TIER_B[2] + rng.randint(8, 26)
        h = rng.randint(8, 18)
        inw = -0.55 * h
        sch = (inw * math.cos(a), inw * math.sin(a))
        r0 = rng.uniform(1.4, 2.8)
        g1, g2 = kleuren[rng.randrange(4)]
        # (i-z2kaart) the gates are notches now: a shard always grows out of the wall, never in the air above a lower wall
        hb = int(K.H[K2.idx(round(bx_), round(bz_))])
        if hb < SY + 10:
            continue
        y0 = min(y0, hb - 3)
        S.kegel(bx_, bz_, y0, h, r0, 0.3, g1, scheef=sch)
        S.kegel(bx_, bz_, y0, int(h * 0.8), r0 * 0.5, 0.2, g2, scheef=(sch[0] * 0.8, sch[1] * 0.8))
        if rng.random() < 0.35:
            S.set(round(bx_ + sch[0] * 0.3), y0 + int(h * 0.3), round(bz_ + sch[1] * 0.3), "minecraft:pearlescent_froglight[axis=y]")
    # garlands between the tier-A houses across the lanes, lampions along the tier edges
    for k in range(16):
        a = math.radians(11.25 + 22.5 * k)
        x, z = round(sx + 51.5 * math.cos(a)), round(sz + 51.5 * math.sin(a))
        i, j = K2.idx(x, z)
        if K.tier[i, j] and not K.trap_baan[i, j] and not K.straat[i, j]:
            lantaarnpaal(S, x, int(H[i, j]) + 1, z, lampion=True)
    # ---- the gondola valley station (tier B)
    gondel_station(S, P, PL.GONDEL_DAL[0], PL.GONDEL_DAL[1], PL.GONDEL_DAL_TOP, "dal")
    # ---- NPC spots
    P["plein"] = [0, SY, sz - 12]
    P["schaatsvijver"] = [vx, SY, vz]
    P["questbord"] = [qx, SY, qz + 1]
    P["sneeuwguhspar"] = [bx, SY, bz + 9]
    P["npc_schaatsguh"] = [vx + PL.VIJVER_R + 2, SY, vz]
    P["npc_burgemeester"] = [0, SY, tz + 6]
    P["klokkentoren"] = [tx, SY, tz]
    P["klokkentoren_top"] = list(toptoren)
    P["markt_kramen"] = {str(i + 1): v for i, v in enumerate(kramen)}
    for gid, g in gb.items():
        if g.get("npc"):
            P["npc_" + gid] = g["npc"]
    P["kristalkapel_altaar"] = gb["kristalkapel"]["altaar"]
    P["gebouw"] = {k: {kk: vv for kk, vv in v.items() if kk not in ("naam",) and vv is not None} for k, v in gb.items()}
    W.stempel(S.stempel("kandijkom"))


def stal(W, K, P):
    """The stable with a paddock, along the dock road just outside the south gate."""
    sx_, sz_ = PL.STAL
    S = Vol(sx_ - 12, FY - 6, sz_ - 10, sx_ + 12, FY + 14, sz_ + 22)
    S.box(sx_ - 6, FY - 5, sz_ - 5, sx_ + 6, FY, sz_ + 5, "minecraft:stone")
    S.box(sx_ - 6, SY, sz_ - 5, sx_ + 6, SY + 10, sz_ + 5, CARVE)
    S.box(sx_ - 5, FY, sz_ - 4, sx_ + 5, FY, sz_ + 4, "minecraft:spruce_planks")
    for (x, z) in ((sx_ - 5, sz_ - 4), (sx_ + 5, sz_ - 4), (sx_ - 5, sz_ + 4), (sx_ + 5, sz_ + 4), (sx_, sz_ - 4), (sx_, sz_ + 4)):
        S.box(x, SY, z, x, SY + 4, z, "minecraft:spruce_log[axis=y]")
    for k in range(5):
        S.box(sx_ - 6, SY + 5 + k, sz_ - 5 + k, sx_ + 6, SY + 5 + k, sz_ - 5 + k, "minecraft:red_terracotta")
        S.box(sx_ - 6, SY + 5 + k, sz_ + 5 - k, sx_ + 6, SY + 5 + k, sz_ + 5 - k, "minecraft:red_terracotta")
    S.box(sx_ - 5, SY + 5, sz_ - 4, sx_ + 5, SY + 5, sz_ + 4, "minecraft:spruce_planks")
    S.box(sx_ - 6, SY + 10, sz_, sx_ + 6, SY + 10, sz_, "minecraft:red_terracotta")
    S.box(sx_ - 6, SY + 11, sz_, sx_ + 6, SY + 11, sz_, laag(2))
    for (x, z) in ((sx_ - 4, sz_ - 3), (sx_ - 3, sz_ - 3), (sx_ - 4, sz_ + 3)):
        S.set(x, SY, z, "minecraft:hay_block[axis=y]")
    S.set(sx_ + 3, SY, sz_ - 3, "minecraft:composter[level=0]")
    S.set(sx_, SY + 4, sz_, LANT_H)
    px0, pz0, px1, pz1 = sx_ - 8, sz_ + 7, sx_ + 8, sz_ + 17
    S.box(px0, FY - 4, pz0, px1, FY, pz1, "minecraft:dirt")
    S.box(px0, SY, pz0, px1, SY + 4, pz1, CARVE)
    for x in range(px0, px1 + 1):
        for z in (pz0, pz1):
            S.set(x, SY, z, HEK_X)
    for z in range(pz0, pz1 + 1):
        for x in (px0, px1):
            S.set(x, SY, z, HEK_Z)
    S.set(px0, SY, (pz0 + pz1) // 2, "minecraft:spruce_fence_gate[facing=west,in_wall=false,open=false,powered=false]")
    gz = (pz0 + pz1) // 2
    S.box(px0 - 5, FY - 3, gz - 1, px0 - 1, FY, gz + 1, "minecraft:dirt")
    S.box(px0 - 5, FY, gz - 1, px0 - 1, FY, gz + 1, "minecraft:dirt_path")
    S.box(px0 - 5, SY, gz - 1, px0 - 1, SY + 3, gz + 1, CARVE)
    S.box(px0 + 1, FY, pz0 + 1, px1 - 1, FY, pz1 - 1, "minecraft:grass_block[snowy=true]")
    P["gebouw"]["stal"] = {"deur": [sx_ - 7, SY, sz_], "binnen": [sx_, SY, sz_], "doos": [[sx_ - 5, FY, sz_ - 4], [sx_ + 5, SY + 10, sz_ + 4]],
                           "npc": [sx_ - 6, SY, sz_ - 1], "wei": [sx_, SY, (pz0 + pz1) // 2]}
    P["npc_stal"] = [sx_ - 6, SY, sz_ - 1]
    W.stempel(S.stempel("stal"))


def gondel_station(S, P, cx, cz, g, naam):
    """A gondola station: a platform with a roof on posts, the big wheel up top, a waiting cabin and the bell."""
    S.box(cx - 5, g, cz - 5, cx + 5, g, cz + 5, "minecraft:stone_bricks")
    S.box(cx - 5, g - 6, cz - 5, cx + 5, g - 1, cz + 5, "minecraft:stone_bricks")
    S.box(cx - 4, g, cz - 4, cx + 4, g, cz + 4, "minecraft:spruce_planks")
    S.box(cx - 5, g + 1, cz - 5, cx + 5, g + 9, cz + 5, CARVE)
    for (x, z) in ((cx - 5, cz - 5), (cx + 5, cz - 5), (cx - 5, cz + 5), (cx + 5, cz + 5)):
        S.box(x, g + 1, z, x, g + 9, z, "minecraft:dark_oak_log[axis=y]")
    S.box(cx - 6, g + 10, cz - 6, cx + 6, g + 10, cz + 6, "minecraft:dark_oak_planks")
    S.box(cx - 6, g + 11, cz - 6, cx + 6, g + 11, cz + 6, "minecraft:magenta_terracotta")
    S.box(cx - 5, g + 12, cz - 5, cx + 5, g + 12, cz + 5, laag(2))
    for k in range(24):
        a = k * math.pi / 12
        S.set(round(cx + 3.5 * math.cos(a)), g + 9, round(cz + 3.5 * math.sin(a)), "minecraft:stripped_dark_oak_log[axis=y]")
    S.box(cx, g + 9, cz, cx, g + 9, cz, "minecraft:iron_block")
    kx, kz = cx, cz - 2
    S.box(kx - 2, g + 2, kz - 1, kx + 2, g + 5, kz + 1, "minecraft:red_wool")
    S.box(kx - 1, g + 3, kz - 1, kx + 1, g + 4, kz - 1, "minecraft:glass")
    S.box(kx - 1, g + 3, kz + 1, kx + 1, g + 4, kz + 1, "minecraft:glass")
    S.box(kx - 2, g + 6, kz - 1, kx + 2, g + 6, kz + 1, laag(3))
    S.box(kx, g + 7, kz, kx, g + 8, kz, "minecraft:iron_chain[axis=y]")
    S.set(cx + 3, g + 1, cz + 3, "minecraft:bell[attachment=floor,facing=north,powered=false]")
    S.set(cx - 3, g + 1, cz + 3, LANT)
    for (x, z) in ((cx - 5, cz), (cx + 5, cz), (cx, cz + 5)):
        S.set(x, g + 8, z, LAMPION)
    P[f"gondel_{naam}"] = [cx, g + 1, cz + 2]
    P[f"gondel_{naam}_knop"] = [cx + 3, g + 1, cz + 3]
    P[f"gondel_{naam}_kabel"] = [cx, g + 9, cz]
    if naam == "dal":
        P["npc_gondel"] = [cx - 2, g + 1, cz + 3]


def gondel(W, K, P):
    (ax, az), (bx, bz) = PL.GONDEL_DAL, PL.GONDEL_BERG
    ya, yb = PL.GONDEL_DAL_TOP + 9, PL.GONDEL_BERG_TOP + 9
    S = Vol(min(ax, bx) - 12, 50, min(az, bz) - 12, max(ax, bx) + 12, max(ya, yb) + 16, max(az, bz) + 12)
    gondel_station(S, P, bx, bz, PL.GONDEL_BERG_TOP, "berg")
    L = math.dist((ax, az), (bx, bz))
    ux, uz = (bx - ax) / L, (bz - az) / L
    px, pz = -uz, ux
    for kant in (-1.5, 1.5):
        a = (ax + px * kant, ya, az + pz * kant)
        b = (bx + px * kant, yb, bz + pz * kant)
        S.lijn(a, b, "minecraft:iron_chain[axis=x,waterlogged=false]")
    # one tall pylon on the far rim of the Galmspleet
    for t in (0.55,):
        x, z = ax + (bx - ax) * t, az + (bz - az) * t
        yk = ya + (yb - ya) * t
        g = hoogste(W, round(x), round(z))
        for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            S.box(round(x) + dx, g + 1, round(z) + dz, round(x) + dx, round(yk) - 1, round(z) + dz, "minecraft:iron_bars[east=false,north=false,south=false,waterlogged=false,west=false]")
        S.box(round(x) - 3, round(yk) - 1, round(z) - 3, round(x) + 3, round(yk) - 1, round(z) + 3, "minecraft:stripped_dark_oak_log[axis=x]")
        S.box(round(x) - 1, g, round(z) - 1, round(x) + 1, g, round(z) + 1, "minecraft:stone_bricks")
        S.set(round(x), round(yk) - 2, round(z), LAMPION)
    for t, kant in ((0.3, -1.5), (0.75, 1.5)):
        x, z = ax + (bx - ax) * t + px * kant, az + (bz - az) * t + pz * kant
        yk = round(ya + (yb - ya) * t)
        x, z = round(x), round(z)
        S.box(x, yk - 2, z, x, yk - 1, z, "minecraft:iron_chain[axis=y]")
        S.box(x - 2, yk - 6, z - 1, x + 2, yk - 3, z + 1, "minecraft:red_wool")
        S.box(x - 1, yk - 5, z - 1, x + 1, yk - 4, z + 1, "minecraft:glass")
        S.box(x - 2, yk - 3, z - 1, x + 2, yk - 3, z + 1, laag(2))
    P["gondel_kabel_dal"] = [ax, ya, az]
    P["gondel_kabel_berg"] = [bx, yb, bz]
    W.stempel(S.stempel("gondel"))


# ================================================================================================ dock
def dok(W, K, P):
    import bouwsels as BZ
    S = BZ.WS(-30, 60, -2265, 100, 70, 50)
    g = FY
    x0, x1, z0, z1 = -10, 10, -2251, -2224
    S.fill(x0, g, z0, x1, g, z1, "minecraft:spruce_planks")
    for x in range(x0, x1 + 1, 5):
        for z in range(z0, z1 + 1, 6):
            gy = hoogste(W, x, z)
            if gy < g - 1:
                S.fill(x, gy + 1, z, x, g - 1, z, "minecraft:spruce_log[axis=y]")
    for z in range(z0, z1 + 1):
        S.set(x0, g + 1, z, HEK_Z)
        if not (-2241 <= z <= -2235):
            S.set(x1, g + 1, z, HEK_Z)
    for x in range(x0, x1 + 1):
        if abs(x) > 3:   # W-SCHEPEN: the south tip in front of the line-A airship stays open (7 wide: its hull)
            S.set(x, g + 1, z1, HEK_X)
    for z in range(z0, z1 + 1, 7):
        S.set(x0, g + 2, z, LANT)
    # mooring post + the moored airship (east of the pier)
    S.fill(x1, g + 1, -2238, x1, g + 15, -2238, "minecraft:spruce_log[axis=y]")
    S.set(x1, g + 16, -2238, LANT)
    BZ.luchtschip(S, 36, -2238, g + 15, mast_x=x1 + 1, richting=-1)
    S.set(4, g + 1, -2232, "minecraft:bell[attachment=floor,facing=north,powered=false]")
    S.fill(2, g, -2234, 6, g, -2230, "tegel_hartjes")
    P["dok_bel"] = [4, g + 1, -2232]
    P["dok_landing"] = [0, g + 1, -2244]
    P["dok_kapitein"] = [-4, g + 1, -2240]
    P["dok_eind"] = [0, g + 1, -2226]
    P["dok_instap"] = [0, g + 1, z1]   # W-SCHEPEN: the open south tip, where the line-A airship lies flush (Lijnen.Haven.UIT)
    W.stempel(S.stempel("dok"))


# ================================================================================================ (i-z2kaart) the arrival
def _kei(S, x, g, z, r, rng):
    """A half-buried boulder with a snow cap."""
    steen = rng.choice(["minecraft:stone", "minecraft:calcite", "minecraft:tuff", "minecraft:cobblestone", "minecraft:andesite"])
    S.ell(x, g, z, r, r * 0.75, r * rng.uniform(0.8, 1.2), steen, ymin=g - 1)
    S.ell(x, g + 0.6, z, r * 0.8, r * 0.7, r * 0.8, SNEEUW, keep=True, ymin=g + round(r * 0.6))


def poort_overgang(S, K, ux, uz, s_a, s_b, rng, vrij_=lambda x, z: True, dicht=1.0):
    """Natural dressing of a gate notch / road cutting: snow drifts at the foot of the banks, boulders, crystal clusters and
    little crystal spikes on the slopes (both sides of the axis, never on the street |p| <= 5)."""
    sx, sz = PL.STAD
    vx, vz = abs(uz), abs(ux)
    for sgn in (-1, 1):
        for s_ in range(s_a, s_b + 1):
            # drifts: the foot of the bank (first cells next to the street that sit below the next cell out)
            for p in (6, 7, 8):
                x, z = sx + ux * s_ + sgn * p * vx, sz + uz * s_ + sgn * p * vz
                if not vrij_(x, z):
                    continue
                g, gu = K.h(x, z), K.h(x + sgn * vx, z + sgn * vz)
                if gu > g:
                    S.set(x, g + 1, z, laag(min(7, 2 + 2 * (p - 5) + rng.randint(0, 1))), keep=True)
        n = int((s_b - s_a) * 0.28 * dicht)
        for _ in range(n):
            s_ = rng.randint(s_a, s_b)
            p = rng.randint(7, 15)
            x, z = sx + ux * s_ + sgn * p * vx, sz + uz * s_ + sgn * p * vz
            if not vrij_(x, z):
                continue
            g = K.h(x, z)
            k = rng.random()
            if k < 0.35:
                _kei(S, x, g, z, rng.uniform(1.3, 2.6), rng)
            elif k < 0.55:
                kristalnaald(S, x, z, g + 1, rng.randint(4, 8), rng.uniform(0.9, 1.4), rng, roze=rng.choice(["roze", "paars", "blauw"]))
            elif k < 0.85:
                S.set(x, g + 1, z, rng.choice(["guhs:guh_kristal_cluster[facing=up]", "minecraft:amethyst_cluster[facing=up,waterlogged=false]",
                                               "minecraft:large_amethyst_bud[facing=up,waterlogged=false]"]), keep=True)
            else:
                S.set(x, g + 1, z, laag(rng.randint(3, 6)), keep=True)


def aankomst(W, K, P):
    """(i-z2kaart) The arrival from the airship pier to the south gate: a cobbled road in a soft cutting (kaart2.poort_dal),
    lampion posts, light strings across the road, fire baskets, a wooden welcome arch at the pier, a hot-chocolate kiosk with
    a greeter spot, a bench by a fire basket, and boulders/crystals/snow drifts on the banks."""
    rng = random.Random(77)
    za, zb = -2314, -2252                  # gate end -> pier end
    S = Vol(-40, FY - 8, za - 8, 40, FY + 30, zb + 2)
    kamp = PL.KAMPVUREN["kandijkom_dok"][0]
    stal = PL.STAL

    def vrij_(x, z):
        if math.hypot(x - kamp[0], z - kamp[1]) < 8:
            return False
        if stal[0] - 14 <= x <= stal[0] + 14 and stal[1] - 13 <= z <= stal[1] + 23:
            return False
        if 5 <= x <= 12 and -2272 <= z <= -2259:     # the kiosk
            return False
        return True
    # the road: knuffelklinkers (7 wide) on a stone bed, snow shoulders
    S.box(-3, FY - 3, za, 3, FY - 1, zb, "minecraft:stone")
    S.box(-3, FY, za, 3, FY, zb, "guhs:knuffelklinkers")
    S.box(-5, SY, za, 5, SY + 5, zb, CARVE)
    poort_overgang(S, K, 0, 1, za + 2400 - 6, zb + 2400 - 4, rng, vrij_, dicht=1.3)
    # lampion posts every 8 both sides; tall garland posts every 16 with a light string across
    for z in range(za + 2, zb - 1, 8):
        for x in (-5, 5):
            if not vrij_(x, z):
                continue
            if (z - za) % 16 == 2:
                S.box(x, SY, z, x, SY + 4, z, STAAF)
                S.set(x, SY + 5, z, LAMPION_S)
            else:
                lantaarnpaal(S, x, SY, z, lampion=True)
        if (z - za) % 16 == 2:
            slinger(S, (-5, SY + 5, z), (5, SY + 5, z), zak=1.0, elk=3)
    # fire baskets halfway between the posts (alternating sides)
    for k, z in enumerate(range(za + 6, zb - 1, 16)):
        x = 5 if k % 2 else -5
        if vrij_(x, z):
            S.set(x, SY, z, VUURKORF)
    # a bench by a fire basket (west shoulder)
    for z in (-2285, -2284, -2283):
        S.set(-5, SY, z, trap("minecraft:spruce_stairs", "west"))
    S.set(-5, SY, -2281, VUURKORF)
    # the hot-chocolate kiosk on a little terrace cut into the east bank, near the pier
    kx, kz = 8, -2266
    S.box(5, FY - 4, kz - 5, 12, FY - 1, kz + 5, "minecraft:stone")
    S.box(5, FY, kz - 5, 12, FY, kz + 5, "minecraft:spruce_planks")
    S.box(5, SY, kz - 5, 12, SY + 8, kz + 5, CARVE)
    S.box(13, FY - 2, kz - 6, 13, SY + 1, kz + 6, "minecraft:stone_bricks")      # a low retaining wall round it (open to the road)
    S.box(6, FY - 2, kz - 6, 12, SY + 1, kz - 6, "minecraft:stone_bricks")
    S.box(6, FY - 2, kz + 6, 12, SY + 1, kz + 6, "minecraft:stone_bricks")
    S.box(6, SY + 2, kz - 6, 13, SY + 2, kz - 6, laag(3))
    S.box(6, SY + 2, kz + 6, 13, SY + 2, kz + 6, laag(3))
    S.box(13, SY + 2, kz - 6, 13, SY + 2, kz + 6, laag(3))
    kraam(S, kx, kz, "west", FY, "minecraft:pink_wool", "minecraft:white_wool",
          [kopjes("west"), "minecraft:water_cauldron[level=3]", kopjes("west")])
    S.set(kx + 2, SY, kz - 3, VUURKORF)
    for z in (kz + 3, kz + 4):
        S.set(10, SY, z, trap("minecraft:spruce_stairs", "east"))
    S.set(11, SY, kz + 3, "minecraft:barrel[facing=up,open=false]")
    S.set(11, SY + 1, kz + 3, LAMPION_S)
    # the wooden welcome arch at the pier end
    poortboog(S, 0, PL.STAD[1], 0, 1, zb - PL.STAD[1] - 1, hout=True)
    P["dok_kiosk"] = [kx + 1, SY, kz]
    P["dok_welkom"] = [3, SY, zb - 4]
    P["dok_bankje"] = [-4, SY, -2284]
    W.stempel(S.stempel("aankomst"))


def poort_overgangen(W, K, P):
    """(i-z2kaart) The other three gates: boulders, crystals and drifts in their notches, a lampion post pair outside."""
    rng = random.Random(78)
    sx, sz = PL.STAD
    for naam, (ux, uz) in PL.POORTEN.items():
        if naam == "zuid":
            continue
        s_a, s_b = PL.GEODE_R + 1, PL.GEODE_R + PL.GEODE_DIK + 12
        xs = [sx + ux * s_a, sx + ux * s_b]
        zs = [sz + uz * s_a, sz + uz * s_b]
        S = Vol(min(xs) - 20, FY - 8, min(zs) - 20, max(xs) + 20, FY + 60, max(zs) + 20)
        poort_overgang(S, K, ux, uz, s_a, s_b, rng)
        vx, vz = abs(uz), abs(ux)
        for s_ in (s_a + 6, s_b - 2):
            for sgn in (-1, 1):
                x, z = sx + ux * s_ + sgn * 5 * vx, sz + uz * s_ + sgn * 5 * vz
                g = K.h(x, z)
                lantaarnpaal(S, x, g + 1, z, lampion=True)
        W.stempel(S.stempel(f"poort_{naam}"))


# ================================================================================================ campfires, needles
def kampvuur(W, K, P, gebied, i, x, z):
    g = hoogste(W, x, z)
    for dx in range(-6, 7):
        for dz in range(-6, 7):
            ii, jj = K2.idx(x + dx, z + dz)
            W.lo_extra[ii, jj] = min(W.lo_extra[ii, jj], g - 4)
    S = Vol(x - 7, g - 5, z - 7, x + 7, g + 8, z + 7)
    S.set = _padvrij(S, K, x, z)
    S.cyl(x, z, 4.6, g - 4, g - 1, "minecraft:stone")
    S.cyl(x, z, 4.6, g, g, SNEEUW)
    S.cyl(x, z, 3.2, g, g, "minecraft:coarse_dirt")
    S.cyl(x, z, 4.6, g + 1, g + 4, CARVE)
    S.set(x, g + 1, z, VUUR)
    for (dx, dz, ax) in ((-3, 0, "z"), (3, 0, "z"), (0, 3, "x")):
        S.set(x + dx, g + 1, z + dz, f"minecraft:spruce_log[axis={ax}]")
        S.set(x + dx + (0 if ax == "z" else 1), g + 1, z + dz + (1 if ax == "z" else 0), f"minecraft:spruce_log[axis={ax}]")
    S.set(x + 3, g + 1, z - 3, "guh_lantaarnpaal[half=lower]")
    S.set(x + 3, g + 2, z - 3, "guh_lantaarnpaal[half=upper]")
    for k in range(3):
        for dz in range(-5, -2):
            S.set(x - 5 + k, g + 1 + k, z + dz, "minecraft:pink_wool")
            S.set(x - 1 - k, g + 1 + k, z + dz, "minecraft:white_wool")
    P[f"kampvuur_{gebied}_{i + 1}"] = [x, g + 1, z]
    P[f"ridder_{gebied}_{i + 1}"] = [x + 2, g + 1, z + 2]
    W.stempel(S.stempel(f"kampvuur_{gebied}_{i}"))


def _padvrij(S, K, cx, cz):
    """Vol.set that leaves path columns alone (tent, logs, lamp) except the fire itself."""
    oud = Vol.set.__get__(S)

    def zet(x, y, z, s, keep=False):
        i, j = K2.idx(int(x), int(z))
        if (int(x), int(z)) != (cx, cz) and K.pad_m[i, j] > 0.5 and s != CARVE:
            return
        oud(x, y, z, s, keep)
    return zet


def kampvuren(W, K, P):
    for gebied, lijst in PL.KAMPVUREN.items():
        for i, (x, z) in enumerate(lijst):
            kampvuur(W, K, P, gebied, i, x, z)


def ijsnaald(W, K, P, naam, x, z, hoogte):
    """A viewpoint: an icy rock needle with a spiral stair and a platform on top."""
    g = K.h(x, z)
    top = g + hoogte
    S = Vol(x - 13, g - 4, z - 13, x + 13, top + 12, z + 13)
    lagen = ["minecraft:calcite", "minecraft:packed_ice", "minecraft:stone", "minecraft:calcite", "minecraft:diorite", "minecraft:snow_block"]
    xs = np.arange(x - 11, x + 12)[:, None]
    zs = np.arange(z - 11, z + 12)[None, :]
    a = np.arctan2(zs - z, xs - x)
    d = np.hypot(xs - x, zs - z)
    for y in range(g - 3, top + 1):
        t = (y - g) / hoogte
        r = 8.5 * (1 - t) ** 0.7 + 3.2
        rr = r * (1 + 0.12 * np.sin(3 * a + y * 0.15) + 0.06 * np.sin(7 * a))
        m = d <= rr
        for k, b in enumerate(lagen):
            mk = m & (((y + (2 * np.sin(a * 2)).astype(int)) // 3) % len(lagen) == k)
            S.masker(x - 11, y, z - 11, mk[:, None, :], b)
    ang = 0.0
    for y in range(g + 1, top + 1):
        t = (y - g) / hoogte
        r = 8.5 * (1 - t) ** 0.7 + 3.2 + 1.6
        for k in range(3):
            aa = ang + k * 0.5 / r
            for dr in (0.0, 0.9, 1.8):
                S.set(x + (r + dr) * math.cos(aa), y, z + (r + dr) * math.sin(aa), "minecraft:spruce_slab[type=top,waterlogged=false]", keep=True)
            S.set(x + (r + 2.7) * math.cos(aa), y + 1, z + (r + 2.7) * math.sin(aa), STAAF, keep=True)
        if (y - g) % 12 == 0:
            S.set(x + (r + 2.7) * math.cos(ang), y + 2, z + (r + 2.7) * math.sin(ang), LANT, keep=True)
        ang += 1.3 / r
    S.cyl(x, z, 6.5, top + 1, top + 1, "minecraft:spruce_planks")
    S.cyl(x, z, 6.5, top + 2, top + 2, STAAF, hollow=1)
    # opening in the railing where the stair arrives
    ea = ang
    for k in range(-2, 3):
        aa = ea + k * 0.18
        S.set(x + 6.2 * math.cos(aa), top + 2, z + 6.2 * math.sin(aa), CARVE)
    S.box(x, top + 2, z, x, top + 7, z, STAAF)
    S.set(x, top + 8, z, "minecraft:amethyst_block")
    S.set(x, top + 9, z, "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
    for i in range(4):
        S.box(x + 1, top + 4 + i, z, x + 4 - i, top + 4 + i, z, "minecraft:light_blue_wool")
    S.set(x - 3, top + 2, z - 3, LANT)
    P[f"uitkijk_{naam}"] = [x - 2, top + 2, z + 2]
    W.stempel(S.stempel("naald_" + naam))


NAALDKLEUR = {False: ("minecraft:light_blue_stained_glass", "minecraft:amethyst_block"), True: ("minecraft:pink_stained_glass", "guhs:guh_kristal_blok"),
              "roze": ("minecraft:pink_stained_glass", "guhs:guh_kristal_blok"), "paars": ("minecraft:purple_stained_glass", "minecraft:amethyst_block"),
              "blauw": ("minecraft:light_blue_stained_glass", "minecraft:pearlescent_froglight[axis=y]"),
              "magenta": ("minecraft:magenta_stained_glass", "guhs:guh_kristal_lamp")}


def kleur_kies(rng):
    return rng.choice(["roze", "roze", "paars", "paars", "blauw", "magenta", False])


def kristalnaald(S, x, z, g, h, r0, rng, roze=False):
    """A crystal needle: an icy foot, a blue-ice body and a coloured glassy tip (pink/purple/blue/magenta)."""
    sch = (rng.uniform(-0.15, 0.15) * h, rng.uniform(-0.15, 0.15) * h)
    S.kegel(x, z, g - 2, int(h * 0.35) + 2, r0 + 0.6, r0 * 0.8, "minecraft:packed_ice", scheef=(sch[0] * 0.35, sch[1] * 0.35))
    S.kegel(x, z, g, int(h * 0.85), r0 * 0.85, r0 * 0.25, "minecraft:blue_ice", scheef=(sch[0] * 0.85, sch[1] * 0.85))
    S.kegel(x, z, g + int(h * 0.3), int(h * 0.7), r0 * 0.6, 0.2,
            NAALDKLEUR[roze][0], scheef=(sch[0] * 0.7, sch[1] * 0.7))
    tx, tz = x + sch[0], z + sch[1]
    S.set(round(tx), g + h, round(tz), NAALDKLEUR[roze][1])


# ================================================================================================ De Galmhellingen
def galmhellingen(W, K, P):
    rng = random.Random(11)
    cx, cz, r = PL.GALM_ARENA
    g = PL.GALM_ARENA_TOP
    S = Vol(cx - 52, g - 8, cz - 52, cx + 52, g + 46, cz + 52)
    S.cyl(cx, cz, r + 1, g - 4, g, "minecraft:packed_ice")
    S.cyl(cx, cz, r + 1, g, g, SNEEUW)
    S.cyl(cx, cz, r + 1, g + 1, g + 40, CARVE)
    # frozen pillars (3x3, 9 high) on a ring r 19
    for k in range(PL.GALM_PILAREN):
        a = k * 2 * math.pi / PL.GALM_PILAREN + 0.2
        x, z = round(cx + 19 * math.cos(a)), round(cz + 19 * math.sin(a))
        S.box(x - 1, g + 1, z - 1, x + 1, g + 9, z + 1, "minecraft:packed_ice")
        S.box(x, g + 1, z, x, g + 9, z, "minecraft:blue_ice")
        S.box(x - 1, g + 10, z - 1, x + 1, g + 10, z + 1, SNEEUW)
        S.box(x - 1, g + 11, z - 1, x + 1, g + 11, z + 1, laag(2))
        P[f"galmwolf_pilaar_{k + 1}"] = [x, g + 1, z]
    # needles round the rim (not at the west entrance)
    for k in range(26):
        a = k * 2 * math.pi / 26 + rng.uniform(-0.08, 0.08)
        if abs(math.atan2(math.sin(a - math.pi), math.cos(a - math.pi))) < 0.45:
            continue
        rr = r + rng.uniform(5, 12)
        x, z = round(cx + rr * math.cos(a)), round(cz + rr * math.sin(a))
        gg = max(K.h(x, z), g)
        kristalnaald(S, x, z, gg, rng.randint(10, 22), rng.uniform(1.6, 2.8), rng, roze=kleur_kies(rng))
    # the Galmwolf's needle (north rim): tall, with a ledge on top
    nx, nz = cx, cz - r - 8
    gg = max(K.h(nx, nz), g)
    kristalnaald(S, nx, nz, gg, 34, 4.5, rng, roze="paars")
    P["galmwolf_naald"] = [nx, gg + 30, nz]
    P["arena_galmwolf"] = [cx, g + 1, cz]
    W.stempel(S.stempel("galm_arena"))
    P["arena_galmwolf_deur"] = [cx - r - 4, hoogste(W, cx - r - 4, cz) + 1, cz]
    P["arena_galmwolf_bord"] = [cx - r - 3, hoogste(W, cx - r - 3, cz + 4) + 1, cz + 4]
    # crystal needle fields
    for i, (fx, fz, fr) in enumerate(PL.NAALD_VELDEN):
        S = Vol(fx - fr - 12, 60, fz - fr - 12, fx + fr + 12, 260, fz + fr + 12)
        n = int(fr * fr / 110)
        for _ in range(n * 3):
            if n <= 0:
                break
            a, d = rng.uniform(0, 2 * math.pi), fr * math.sqrt(rng.random())
            x, z = round(fx + d * math.cos(a)), round(fz + d * math.sin(a))
            if not vrij(K, x, z, 4) or op_pad(K, x, z) or math.hypot(x - cx, z - cz) < r + 14:
                continue
            kristalnaald(S, x, z, K.h(x, z), rng.randint(10, 30), rng.uniform(1.5, 3.4), rng, roze=kleur_kies(rng))
            n -= 1
        P[f"galm_naalden_{i + 1}"] = [fx, K.h(fx, fz) + 1, fz]
        W.stempel(S.stempel(f"naaldveld_{i}"))
    # loose needles all over the Galmhellingen
    S = Vol(80, 60, -2510, 260, 280, -2190)
    for _ in range(110):
        x, z = rng.randint(90, 248), rng.randint(-2500, -2205)
        if not vrij(K, x, z, 5) or op_pad(K, x, z) or math.hypot(x - cx, z - cz) < r + 14 or K.h(x, z) < 96:
            continue
        kristalnaald(S, x, z, K.h(x, z), rng.randint(7, 22), rng.uniform(1.2, 2.8), rng, roze=kleur_kies(rng))
    W.stempel(S.stempel("galm_naalden_los"))
    # avalanche paths: bare snow lanes with snow boulders and broken spruces at the foot
    for i, lijn in enumerate(PL.LAWINES):
        dl, langs = K2.polyafstand(lijn)
        m = dl < 7 + 2 * K.n1
        W.top[m & (K.H > PL.ZEE + 2) & ~(K.pad_m > 0.5)] = W.palet.id(SNEEUW)
        W.bezet |= m
        (ex, ez) = lijn[-1]
        S = Vol(ex - 30, 60, ez - 30, ex + 30, 260, ez + 30)
        for _ in range(16):
            x, z = ex + rng.randint(-14, 14), ez + rng.randint(-14, 14)
            if not vrij(K, x, z, 2):
                continue
            gg = K.h(x, z)
            if rng.random() < 0.6:
                S.ell(x, gg + 1, z, rng.uniform(1.5, 2.8), rng.uniform(1.2, 2.0), rng.uniform(1.5, 2.8), SNEEUW)
            else:
                ax = rng.choice("xz")
                L = rng.randint(3, 6)
                for k in range(L):
                    S.set(x + (k if ax == "x" else 0), gg + 1, z + (k if ax == "z" else 0), f"minecraft:spruce_log[axis={ax}]")
        P[f"galm_lawine_{i + 1}"] = [lijn[0][0], K.h(*lijn[0]) + 1, lijn[0][1]]
        W.stempel(S.stempel(f"lawine_{i}"), bezet=False)
        P[f"galm_lawine_{i + 1}_voet"] = [ex, hoogste(W, ex, ez) + 1, ez]
    grotje(W, K, P, "geheim_galmgrot", PL.GALMGROT[0], PL.GALMGROT[1], "galm")


def grotje(W, K, P, naam, x, z, soort):
    """A small secret cave: an icy dome on the ground, the entrance behind a frozen waterfall (with a 1-wide gap)."""
    g = K.h(x, z)
    S = Vol(x - 24, g - 14, z - 12, x + 12, g + 16, z + 12)   # W-INT: west + down for the step ramp
    S.cyl(x, z, 9, g - 5, g, "minecraft:stone")
    S.ell(x, g + 1, z, 9, 8, 9, "minecraft:packed_ice" if soort == "galm" else SNEEUW, ymin=g + 1)
    S.ell(x, g + 1, z, 9.3, 8.3, 9.3, "minecraft:snow_block", hollow=0.6, ymin=g + 5)
    if soort == "galm":   # a geode: amethyst + pink crystal inside, glowing
        S.ell(x, g + 1, z, 7.4, 6.4, 7.4, "minecraft:amethyst_block", ymin=g + 1)
        S.ell(x, g + 1, z, 7.0, 6.0, 7.0, "guhs:guh_kristal_blok", hollow=0.6, ymin=g + 3)
    else:                 # an ice geode: blue ice + light blue glass with froglights
        S.ell(x, g + 1, z, 7.4, 6.4, 7.4, "minecraft:blue_ice", ymin=g + 1)
        S.ell(x, g + 1, z, 7.0, 6.0, 7.0, "minecraft:light_blue_stained_glass", hollow=0.6, ymin=g + 3)
    S.ell(x, g + 1, z, 6.5, 5.5, 6.5, CARVE, ymin=g + 1)
    for (ddx, ddz) in ((5, 0), (-3, 4), (0, -5), (-4, -3)):
        S.set(x + ddx, g + 3, z + ddz, "minecraft:pearlescent_froglight[axis=y]")
    S.cyl(x, z, 6.5, g, g, "minecraft:snow_block")
    # entrance to the west: a short tunnel, a waterfall curtain of ice with a gap
    S.box(x - 10, g + 1, z - 1, x - 5, g + 3, z + 1, CARVE)
    S.box(x - 10, g, z - 1, x - 5, g, z + 1, "minecraft:snow_block")
    S.box(x - 9, g + 1, z - 2, x - 9, g + 7, z + 2, "minecraft:blue_ice")
    S.box(x - 9, g + 1, z, x - 9, g + 2, z, CARVE)
    S.box(x - 10, g + 1, z, x - 10, g + 2, z, CARVE)
    for dz in (-1, 1):
        S.set(x - 8, g + 3, z + dz, "minecraft:pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]")
    if soort == "galm":
        for (dx, dz) in ((3, 2), (-2, 4), (4, -3), (1, -4), (-3, -3)):
            S.set(x + dx, g + 1, z + dz, "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
        S.set(x + 4, g + 1, z, "minecraft:amethyst_block")
        S.set(x + 4, g + 2, z, "guhs:guh_kristal_cluster[facing=up]")
    else:
        S.set(x + 3, g + 1, z, "guhs:sneeuwpopguh[facing=west,half=lower]")
        S.set(x + 3, g + 2, z, "guhs:sneeuwpopguh[facing=west,half=upper]")
        S.set(x + 2, g + 1, z + 2, "minecraft:white_carpet")
    S.set(x, g + 5, z, LANT_H)
    # W-INT: the tunnel floor (g) often sits 3-8 above the real ground west of the mouth (the dome stands on K.h of its centre):
    # a 3-wide snow step ramp down to the ground, 1 per block, with free head room (w-bereik: the cave was unreachable on foot)
    trede = 0
    for k in range(1, 14):
        xx = x - 10 - k
        top = g - k
        grond = max(hoogste(W, xx, zz) for zz in (z - 1, z, z + 1))
        if grond >= top:
            break
        S.box(xx, grond + 1, z - 1, xx, top, z + 1, "minecraft:snow_block")
        S.box(xx, top + 1, z - 1, xx, top + 4, z + 1, CARVE)
        trede = k
    P[naam] = [x, g + 1, z]
    W.stempel(S.stempel(naam))
    P[naam + "_ingang"] = [x - 11 - trede, hoogste(W, x - 11 - trede, z) + 1, z]


# ================================================================================================ Het Bevroren Knuffelmeer
KLEUREN = ["pink", "white", "light_blue", "yellow", "lime", "orange", "magenta", "pink", "white", "light_blue"]


def knuffelmeer(W, K, P):
    rng = random.Random(17)
    mx, mz, rx, rz = PL.MEER
    ijs = PL.MEER_IJS
    S = Vol(mx - rx - 10, 60, mz - rz - 10, mx + rx + 10, 120, mz + rz + 10)
    # plushies under the ice: big guh heads looking up (face on top), a body down to the bottom
    for i, (x, z) in enumerate(PL.KNUFFELS_ONDER_IJS):
        kl = KLEUREN[i % len(KLEUREN)]
        wol = f"minecraft:{kl}_wool"
        rr = rng.uniform(4.5, 6.5)
        bodem = K.h(x, z)
        S.cyl(x, z, rr * 0.7, bodem, ijs - 4, wol)
        S.ell(x, ijs - 3.5, z, rr, 2.6, rr * 0.9, wol)
        for kant in (-1, 1):
            S.ell(x + kant * rr * 0.6, ijs - 2, z - rr * 0.8, 1.6, 1.2, 1.4, wol)
        for kant in (-1, 1):
            S.set(round(x + kant * rr * 0.35), ijs - 1, round(z - 1), "minecraft:black_wool")
        for dx in (-1, 0, 1):
            S.set(x + dx, ijs - 1, z + 1 + (0 if dx == 0 else 1) - 1, "minecraft:magenta_wool" if kl != "magenta" else "minecraft:red_wool")
        S.cyl(x, z, rr + 1.5, ijs, ijs, "minecraft:ice")
        P[f"knuffel_ijs_{i + 1}"] = [x, ijs + 1, z]
    # cracks: zigzag lines of packed ice with a few short water gaps
    for k in range(4):
        a = rng.uniform(0, 2 * math.pi)
        x, z = mx + rng.uniform(-rx * 0.6, rx * 0.6), mz + rng.uniform(-rz * 0.6, rz * 0.6)
        for s in range(rng.randint(40, 80)):
            a += rng.uniform(-0.5, 0.5)
            x += math.cos(a)
            z += math.sin(a)
            xi, zi = round(x), round(z)
            i, j = K2.idx(xi, zi)
            if not (0 <= i < PL.NX and 0 <= j < PL.NZ) or not K.ijs[i, j]:
                break
            if any(math.hypot(xi - px, zi - pz) < 8 for (px, pz) in PL.KNUFFELS_ONDER_IJS):
                continue
            S.set(xi, ijs, zi, "minecraft:packed_ice" if (s % 9) else "minecraft:blue_ice")
    # ice floes in the open water at the outflow (spaced so you can always climb out)
    sx, sz, sr = PL.SCHOTSEN
    S.cyl(sx, sz, 2.2, ijs, ijs, "minecraft:packed_ice")
    for gx in range(-sr - 4, sr + 5, 6):
        for gz in range(-sr - 4, sr + 5, 6):
            x, z = sx + gx + rng.randint(-1, 1), sz + gz + rng.randint(-1, 1)
            i, j = K2.idx(x, z)
            if K.open_water[i, j]:
                S.cyl(x, z, rng.uniform(1.4, 2.6), ijs, ijs, "minecraft:packed_ice")
                if rng.random() < 0.4:
                    S.set(x, ijs + 1, z, laag(rng.randint(1, 3)))
    sx_, sz_ = PL.MEER_SCHEUR
    for k in range(-16, 17):
        x = sx_ + k
        z = sz_ + round(2.5 * math.sin(k / 3.0))
        i, j = K2.idx(x, z)
        if K.ijs[i, j]:
            S.set(x, ijs, z, "minecraft:blue_ice")
            S.set(x, ijs, z + 1, "minecraft:packed_ice" if k % 3 else "minecraft:light_blue_stained_glass")
    P["meer_scheur"] = [sx_, ijs + 1, sz_ - 2]
    P["meer_midden"] = [mx, ijs + 1, mz]
    P["meer_schotsen"] = [sx, ijs + 1, sz]
    W.stempel(S.stempel("knuffelmeer"))
    # the island with the ice fisher's hut
    ix, iz, ir = PL.MEER_EILAND
    g = K.h(ix, iz)
    S = Vol(ix - 12, g - 4, iz - 12, ix + 12, g + 12, iz + 12)
    x1, z1, x2, z2 = ix - 3, iz - 3, ix + 3, iz + 2
    S.box(x1, g, z1, x2, g, z2, "minecraft:spruce_planks")
    S.wanden(x1, g + 1, z1, x2, g + 4, z2, "minecraft:spruce_planks")
    for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
        S.box(x, g + 1, z, x, g + 4, z, "minecraft:spruce_log[axis=y]")
    S.box(x1 + 1, g + 1, z1 + 1, x2 - 1, g + 1, z2 - 1, "minecraft:light_blue_carpet")
    S.set(ix, g + 1, z2, "minecraft:spruce_door[facing=north,half=lower,hinge=left,open=false,powered=false]")
    S.set(ix, g + 2, z2, "minecraft:spruce_door[facing=north,half=upper,hinge=left,open=false,powered=false]")
    S.set(x1 + 1, g + 2, z1, "minecraft:glass")
    S.set(x2 - 1, g + 2, z1, "minecraft:glass")
    for k in range(3):
        S.box(x1 - 1 + k, g + 5 + k, z1 - 1, x1 - 1 + k, g + 5 + k, z2 + 1, SNEEUW)
        S.box(x2 + 1 - k, g + 5 + k, z1 - 1, x2 + 1 - k, g + 5 + k, z2 + 1, SNEEUW)
    S.box(x1 + 2, g + 5, z1, x2 - 2, g + 7, z2, "minecraft:spruce_planks")
    S.box(ix, g + 8, z1 - 1, ix, g + 8, z2 + 1, SNEEUW)
    S.set(x2 - 1, g + 1, z1 + 1, "minecraft:barrel[facing=up,open=false]")
    S.set(x1 + 1, g + 1, z1 + 1, "minecraft:white_carpet")
    S.set(ix, g + 4, iz, LANT_H)
    S.set(ix + 4, g + 1, iz + 5, STAAF)
    S.set(ix + 4, g + 2, iz + 5, STAAF)
    P["meer_eiland"] = [ix, g + 1, iz + 4]
    P["npc_ijsvisser"] = [ix + 2, g + 1, iz + 4]
    W.stempel(S.stempel("meereiland"))
    # the dragon's saddle: scorched grey ground, grey crystal scales, a few pink shards
    dx_, dz_, dr = PL.DRAKENZADEL
    g = PL.DRAKENZADEL_TOP
    S = Vol(dx_ - dr - 14, g - 6, dz_ - dr - 14, dx_ + dr + 14, g + 20, dz_ + dr + 14)
    xs = np.arange(dx_ - dr - 3, dx_ + dr + 4)
    zs = np.arange(dz_ - dr - 3, dz_ + dr + 4)
    for x in xs:
        for z in zs:
            d = math.hypot(x - dx_, z - dz_)
            if d > dr + 2:
                continue
            v = rng.random()
            b = "minecraft:gravel" if v < 0.3 else ("minecraft:tuff" if v < 0.55 else ("minecraft:light_gray_concrete_powder" if v < 0.7 else SNEEUW))
            if d < 12:
                b = "minecraft:light_gray_concrete_powder" if v < 0.5 else "minecraft:tuff"
            S.set(x, g, z, b)
    S.cyl(dx_, dz_, dr + 1, g + 1, g + 18, CARVE)
    for _ in range(26):
        a, d = rng.uniform(0, 2 * math.pi), rng.uniform(19, dr + 6)
        x, z = round(dx_ + d * math.cos(a)), round(dz_ + d * math.sin(a))
        gg = max(g, K.h(x, z))
        h = rng.randint(3, 7)
        sch = (rng.uniform(-2, 2), rng.uniform(-2, 2))
        S.kegel(x, z, gg, h, rng.uniform(0.9, 1.8), 0.2, rng.choice(["minecraft:light_gray_stained_glass", "minecraft:calcite", "minecraft:tuff"]), scheef=sch)
    for k in range(5):
        a = rng.uniform(0, 2 * math.pi)
        x, z = round(dx_ + 15 * math.cos(a)), round(dz_ + 15 * math.sin(a))
        S.set(x, g + 1, z, "guhs:guh_kristal_cluster[facing=up]")
    P["draak_zadel"] = [dx_, g + 1, dz_]
    P["draak_zadel_kijk"] = [dx_, g + 1, dz_ + dr - 2]
    W.stempel(S.stempel("drakenzadel"))
    grotje(W, K, P, "geheim_ijsgrot", PL.IJSGROT[0], PL.IJSGROT[1], "meer")


# ================================================================================================ De Splinterkloof
def kloofrand(K, x, richting):
    """The first column outside the ravine (richting +1 = south, -1 = north) at x: (z, top)."""
    zc = int(round(float(K2.kloof_z(x))))
    z = zc
    for _ in range(80):
        z += richting
        i, j = K2.idx(x, z)
        if not K.kloof[i, j]:
            return z, int(K.H[i, j])
    return z, int(K.H[K2.idx(x, z)])


def _in_kloof(K, x, z):
    i, j = K2.idx(x, z)
    return bool(K.kloof[i, j])


def _landing(K, x, z, ax, az, standaard=6):
    """(i-z2kaart) Where a bridge walkway meets the land: from the rim cell (x, z) outward along (ax, az) the first path cell
    within 3..16 blocks (so the walkway runs straight onto the road), else `standaard` blocks out. Returns (x, z, stand y)."""
    for k in range(3, 17):
        xx, zz = x + ax * k, z + az * k
        if K.pad_m[K2.idx(xx, zz)] > 0.5:
            return xx, zz, K.h(xx, zz) + 1
    xx, zz = x + ax * standaard, z + az * standaard
    return xx, zz, K.h(xx, zz) + 1


def hangbrug(K, S, cellen, dwars, breed, dek, hout, hek, sag=4.0, ketting=6, lampion=False, stand_b=None):
    """(i-z2kaart) A hanging bridge whose two ends are walkways ON the land, flush with the ground at both landings.

    cellen: (x, z) along the axis from landing A to landing B. dwars: unit (px, pz) across. The deck profile (in half blocks =
    stand height * 2) runs from the ground at A to the ground at B, sags over the ravine and changes at most half a block per
    cell (slabs only: always walkable, no jump). Over land the deck lies on a filled foot (stone bricks) and everything above
    it is cut away, so it never floats above or hides under the landing. Returns the deck profile [(x, z, slab y, stand y)]."""
    px, pz = dwars
    n = len(cellen) - 1
    (xa, za_), (xb, zb_) = cellen[0], cellen[-1]
    sa, sb = 2 * (K.h(xa, za_) + 1), 2 * (stand_b if stand_b is not None else K.h(xb, zb_) + 1)
    kl = [_in_kloof(K, x, z) for (x, z) in cellen]
    ia = kl.index(True) if True in kl else 0
    ib = n - kl[::-1].index(True) if True in kl else n
    prof = []
    for k in range(n + 1):
        t = k / max(1, n)
        s = sa + (sb - sa) * t
        if ia - 2 <= k <= ib + 2:
            s -= sag * math.sin(math.pi * (k - ia + 2) / max(1, ib - ia + 4))
        prof.append(int(round(s)))
    prof[0], prof[n] = sa, sb
    for _ in range(2):        # slope limiter: at most 1 half block per cell (fixed ends)
        for k in range(1, n + 1):
            prof[k] = min(max(prof[k], prof[k - 1] - 1), prof[k - 1] + 1)
        prof[n] = sb
        for k in range(n - 1, -1, -1):
            prof[k] = min(max(prof[k], prof[k + 1] - 1), prof[k + 1] + 1)
        prof[0] = sa
    uit = []
    for k, ((x, z), s) in enumerate(zip(cellen, prof)):
        y, top = (s - 1) // 2, (s - 1) % 2 == 1
        slab = f"{dek}_slab[type={'top' if top else 'bottom'},waterlogged=false]"
        rand_ = ia - 2 <= k <= ib + 2          # the hanging part (+2 cells onto the land each side)
        w = breed + 1
        for p in range(-w, w + 1):
            cx, cz = x + p * px, z + p * pz
            if k in (0, n) and p and K.h(cx, cz) + 1 < s / 2:
                continue        # the very end: a side column lower than the landing keeps its ground (a natural step)
            if abs(p) <= breed or not rand_:
                S.set(cx, y, cz, slab)
            else:
                S.set(cx, y, cz, f"{hout}_planks")
            S.box(cx, y + 1, cz, cx, max(y + 5, K.h(cx, cz) + 1), cz, CARVE)
            if not _in_kloof(K, cx, cz):
                g = K.h(cx, cz)
                if g < y - 1:
                    S.box(cx, g + 1, cz, cx, y - 1, cz, "minecraft:stone_bricks")
                elif g >= y:
                    S.set(cx, y - 1, cz, "minecraft:stone_bricks")
        if rand_:
            for kant in (-1, 1):
                S.set(x + kant * w * px, y + 1, z + kant * w * pz, hek)
            if k % ketting == 0 and kl[k]:
                for kant in (-1, 1):
                    S.set(x + kant * w * px, y - 1, z + kant * w * pz, "minecraft:iron_chain[axis=y]")
                if lampion:
                    S.set(x, y - 1, z, LAMPION)
        uit.append((x, z, y, s // 2))
    # head posts with a lantern on the first land cell beside the ravine (both ends)
    for k in (ia - 1, ib + 1):
        if 0 <= k <= n:
            x, z, y, _ = uit[k]
            for kant in (-1, 1):
                qx, qz = x + kant * (breed + 2) * px, z + kant * (breed + 2) * pz
                g = min(K.h(qx, qz), y)
                S.box(qx, g + 1, qz, qx, y + 5, qz, f"{hout}_log[axis=y]")
                S.set(qx, y + 6, qz, LAMPION_S if lampion else LANT)
                # no pocket between the post and the hillside: the cell outside the post is raised to the deck
                ox, oz = qx + kant * px, qz + kant * pz
                g2 = K.h(ox, oz)
                buren = [K.h(ox + ax, oz + az) for (ax, az) in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (ox + ax, oz + az) != (qx, qz)]
                doel = max(y, min(buren) - 1)
                if g2 < doel and not _in_kloof(K, ox, oz):
                    S.box(ox, g2 + 1, oz, ox, doel, oz, "minecraft:stone")
    return uit


def brug(W, K, P, i, bx, breed=1, dek="minecraft:spruce", hout="minecraft:spruce"):
    zs, _ = kloofrand(K, bx, 1)
    zn, _ = kloofrand(K, bx, -1)
    _, za, _ = _landing(K, bx, zs, 0, 1)
    _, zb, _ = _landing(K, bx, zn, 0, -1)
    stand_b = None
    if i == 2:      # the main bridge lands on the Spiegelburcht's grand stair (its tuff landing starts at zn - 2, stand BRUG_TOP + 1)
        zb, stand_b = zn - 2, PL.BRUG_TOP + 1
    S = Vol(bx - breed - 6, 40, zb - 8, bx + breed + 6, 200, za + 4)
    hek = HEK_Z if hout == "minecraft:spruce" else "minecraft:dark_oak_fence[east=false,north=true,south=true,waterlogged=false,west=false]"
    hangbrug(K, S, [(bx, z) for z in range(za, zb - 1, -1)], (1, 0), breed, dek, hout, hek, stand_b=stand_b)
    if i == 2:      # keep the grand stair's landing clear (a ridge of terrain stuck out of it: a 1-block bump)
        S.box(bx - 4, PL.BRUG_TOP + 1, zn - 7, bx + 4, PL.BRUG_TOP + 8, zn - 2, CARVE)
    W.stempel(S.stempel(f"brug_{i}"))
    P[f"brug_{i + 1}_zuid"] = [bx, hoogste(W, bx, zs + 3) + 1, zs + 3]
    P[f"brug_{i + 1}_noord"] = [bx, hoogste(W, bx, zn - 3) + 1, zn - 3] if i != 2 else [bx, PL.BRUG_TOP + 1, zn - 3]


def boortoren(W, K, P, i, x, z):
    g = hoogste(W, x, z)
    zc = float(K2.kloof_z(x))
    rz = 1 if zc > z else -1
    S = Vol(x - 8, g - 50, z - 26, x + 8, g + 26, z + 26)
    S.box(x - 4, g - 2, z - 4, x + 4, g, z + 4, "mika_steen")
    S.box(x - 4, g + 1, z - 4, x + 4, g + 21, z + 4, CARVE)
    for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        S.box(x + dx, g + 1, z + dz, x + dx, g + 20, z + dz, "minecraft:dark_oak_log[axis=y]")
    for yy in (g + 7, g + 14, g + 20):
        S.box(x - 3, yy, z - 3, x + 3, yy, z + 3, "minecraft:dark_oak_planks")
        S.box(x - 2, yy, z - rz * 2, x - 2, yy, z - rz * 2, CARVE)
    for yy in range(g + 1, g + 21):
        S.set(x - 2, yy, z - rz * 3, "minecraft:dark_oak_planks")
        S.set(x - 2, yy, z - rz * 2, f"minecraft:ladder[facing={'south' if rz > 0 else 'north'},waterlogged=false]")
    for yy in (g + 8, g + 15, g + 21):
        for k in range(-3, 4):
            for (ax, az) in ((x + k, z - 3), (x + k, z + 3), (x - 3, z + k), (x + 3, z + k)):
                S.set(ax, yy, az, STAAF, keep=True)
        S.set(x - 2, yy, z - rz * 3, CARVE)
    # the drill arm over the ravine edge, the drill hanging down along the wall
    for k in range(4, 13):
        S.set(x, g + 19, z + rz * k, "minecraft:iron_block")
    S.box(x, g + 1, z + rz * 12, x, g + 18, z + rz * 12, "minecraft:iron_chain[axis=y]")
    S.box(x, g - 14, z + rz * 12, x, g, z + rz * 12, "minecraft:iron_block", keep=True)
    S.box(x - 1, g - 12, z + rz * 12, x + 1, g - 8, z + rz * 12, "minecraft:polished_deepslate", keep=True)
    S.set(x, g - 15, z + rz * 12, "minecraft:lightning_rod[facing=down,powered=false,waterlogged=false]", keep=True)
    # boiler with smoke, crystal loot piles, a Mika flag
    S.set(x + 2, g + 1, z - rz * 2, "minecraft:blast_furnace[facing=north,lit=true]")
    S.set(x + 2, g + 21, z, ROOK)
    S.box(x + 2, g + 21, z - 1, x + 2, g + 21, z - 1, "minecraft:iron_block")
    for (dx, dz) in ((1, 1), (2, 1), (1, 2)):
        S.set(x + dx, g + 1, z + dz, "minecraft:light_gray_stained_glass")
    S.set(x + 1, g + 2, z + 1, "guhs:guh_kristal_cluster[facing=up]")
    S.box(x - 3, g + 22, z - 3, x - 3, g + 25, z - 3, "minecraft:dark_oak_fence")
    S.box(x - 2, g + 24, z - 3, x - 1, g + 25, z - 3, "minecraft:red_wool")
    P[f"boortoren_{i + 1}"] = [x, g + 1, z]
    P[f"boortoren_{i + 1}_top"] = [x, g + 21, z]
    W.stempel(S.stempel(f"boortoren_{i}"))


def splinterkloof(W, K, P):
    rng = random.Random(23)
    for i, bx in enumerate(PL.BRUGGEN):
        if i == 2:
            brug(W, K, P, i, bx, breed=2, dek="minecraft:dark_oak", hout="minecraft:dark_oak")
        else:
            brug(W, K, P, i, bx)
    spleetbrug(W, K, P)
    for i, (x, z) in enumerate(PL.BOORTORENS):
        boortoren(W, K, P, i, x, z)
    for i, t_ in enumerate(PL.KLOOF_TRAPPEN):
        if t_ is None:      # the Galmspleet: its floor is the ramp; the foot where it meets the ravine
            x0, zt = PL.GALMSPLEET[0][0], int(round(float(K2.kloof_z(PL.GALMSPLEET[0][0])))) + 8
        else:
            x0, rx_, kant = t_
            x0 = x0 + rx_ * 2
            zt = int(round(float(K2.kloof_z(x0)))) + kant * 12
        P[f"kloof_trap_{i + 1}"] = [x0, hoogste(W, x0, zt) + 1, zt]
    # spawn spots on the ravine floor (wezens)
    n = 0
    for x in range(-216, 217, 36):
        zc = int(round(float(K2.kloof_z(x))))
        i, j = K2.idx(x, zc)
        if K.kloof[i, j] and not K.kloof_ramp[i, j]:
            n += 1
            P[f"kloof_bodem_{n}"] = [x, int(K.H[i, j]) + 1, zc]
    # grey/purple crystal outcrops and boulders on the ravine floor
    S = Vol(-250, 55, -2600, 250, 110, -2480)
    for _ in range(90):
        x = rng.randint(-232, 232)
        zc = float(K2.kloof_z(x))
        z = round(zc + rng.uniform(-14, 14))
        i, j = K2.idx(x, z)
        if not K.kloof[i, j] or K.kloof_ramp[i, j] or abs(z - zc) < 4:
            continue
        g = int(K.H[i, j])
        if rng.random() < 0.5:
            S.kegel(x, z, g, rng.randint(3, 9), rng.uniform(0.8, 1.8), 0.2, rng.choice(["minecraft:purple_stained_glass", "minecraft:pink_stained_glass",
                                                                                      "minecraft:calcite", "minecraft:light_blue_stained_glass"]),
                    scheef=(rng.uniform(-2, 2), rng.uniform(-2, 2)))
        else:
            S.ell(x, g + 1, z, rng.uniform(1.2, 2.4), rng.uniform(1, 1.8), rng.uniform(1.2, 2.4), rng.choice(["minecraft:tuff", "minecraft:deepslate", SNEEUW]))
    W.stempel(S.stempel("kloofbodem"), bezet=False)
    # ---- the secret drill site of the Mika-boormeester: a cave in the north wall
    cx, cz, cr = PL.BOORMEESTER
    vloer = PL.KLOOF_BODEM + 1
    ix, iz = PL.BOORMEESTER_INGANG
    zr, _ = kloofrand(K, ix, -1)
    for x in range(cx - cr - 3, cx + cr + 4):
        for z in range(cz - cr - 3, zr + 3):
            i, j = K2.idx(x, z)
            W.lo_extra[i, j] = min(W.lo_extra[i, j], vloer - 4)
    S = Vol(cx - cr - 4, vloer - 4, cz - cr - 4, cx + cr + 4, vloer + 20, zr + 6)
    S.cyl(cx, cz, cr + 2, vloer - 3, vloer, "minecraft:deepslate")
    S.cyl(cx, cz, cr, vloer, vloer, "minecraft:deepslate_tiles")
    S.ell(cx, vloer, cz, cr, 14, cr, CARVE, ymin=vloer + 1)
    S.box(ix - 2, vloer - 2, cz + cr - 3, ix + 2, vloer, zr + 4, "minecraft:deepslate_tiles")
    S.box(ix - 2, vloer + 1, cz + cr - 3, ix + 2, vloer + 5, zr + 4, CARVE)
    for dx in (-2, -1, 1, 2):
        S.set(ix + dx, vloer + 5, zr, "minecraft:pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]")
    S.box(ix - 3, vloer + 1, zr + 1, ix - 3, vloer + 6, zr + 1, "minecraft:packed_ice")
    S.box(ix + 3, vloer + 1, zr + 1, ix + 3, vloer + 6, zr + 1, "minecraft:packed_ice")
    for k in range(3):
        a = math.radians(90 + k * 120)
        x, z = round(cx + 12 * math.cos(a)), round(cz + 12 * math.sin(a))
        S.box(x - 1, vloer, z - 1, x + 1, vloer, z + 1, "minecraft:iron_block")
        P[f"boormeester_kop_{k + 1}"] = [x, vloer + 1, z]
    for k in range(12):
        a = k * math.pi / 6
        x, z = round(cx + (cr - 2) * math.cos(a)), round(cz + (cr - 2) * math.sin(a))
        S.set(x, vloer + 1, z, rng.choice(["minecraft:barrel[facing=up,open=false]", "minecraft:iron_block", "mika_steen"]))
        S.set(x, vloer + 6, z, LANT) if False else S.set(x, vloer + 2, z, LANT)
    P["arena_boormeester"] = [cx, vloer + 1, cz]
    P["arena_boormeester_straal"] = [cr, 0, 0]
    P["arena_boormeester_deur"] = [ix, vloer + 1, zr + 2]
    P["geheim_boorplaats"] = [ix, vloer + 1, zr - 2]
    W.stempel(S.stempel("boormeester"))


def mika_fort(W, K, P):
    """The Mika fort on the north plateau: a palisade of Mika stone with a gate, a watchtower, tents and a smoking chimney."""
    x, z = PL.MIKA_FORT
    g = hoogste(W, x, z)
    S = Vol(x - 22, g - 8, z - 22, x + 22, g + 26, z + 22)
    S.box(x - 16, g - 6, z - 16, x + 16, g, z + 16, "minecraft:stone")
    S.box(x - 16, g, z - 16, x + 16, g, z + 16, "minecraft:gravel")
    S.box(x - 16, g + 1, z - 16, x + 16, g + 12, z + 16, CARVE)
    S.wanden(x - 16, g + 1, z - 16, x + 16, g + 5, z + 16, "mika_steen")
    for k in range(-16, 17, 2):
        for (ax, az) in ((x + k, z - 16), (x + k, z + 16), (x - 16, z + k), (x + 16, z + k)):
            S.set(ax, g + 6, az, "mika_steen")
    S.box(x - 2, g + 1, z + 16, x + 2, g + 4, z + 16, CARVE)          # gate (south)
    S.box(x - 3, g + 5, z + 16, x + 3, g + 5, z + 16, "mika_steen_pilaar")
    tx, tz = x + 10, z - 10
    S.box(tx - 2, g + 1, tz - 2, tx + 2, g + 14, tz + 2, "mika_steen")
    S.box(tx - 1, g + 1, tz - 1, tx + 1, g + 13, tz + 1, CARVE)
    for yy in range(g + 1, g + 15):
        S.set(tx, yy, tz - 1, "minecraft:ladder[facing=south,waterlogged=false]")
    S.set(tx, g + 1, tz + 2, CARVE)
    S.set(tx, g + 2, tz + 2, CARVE)
    S.box(tx - 3, g + 15, tz - 3, tx + 3, g + 15, tz + 3, "minecraft:dark_oak_planks")
    S.set(tx, g + 15, tz - 1, CARVE)
    for k in range(-3, 4):
        for (ax, az) in ((tx + k, tz - 3), (tx + k, tz + 3), (tx - 3, tz + k), (tx + 3, tz + k)):
            S.set(ax, g + 16, az, "minecraft:dark_oak_fence", keep=True)
    S.box(tx, g + 16, tz, tx, g + 21, tz, "minecraft:dark_oak_fence")
    S.box(tx + 1, g + 19, tz, tx + 3, g + 21, tz, "minecraft:red_wool")
    for (cx_, cz_) in ((x - 8, z - 6), (x - 8, z + 6), (x + 2, z + 6)):
        for k in range(3):
            S.box(cx_ - 3 + k, g + 1 + k, cz_ - 2, cx_ - 3 + k, g + 1 + k, cz_ + 2, "minecraft:gray_wool")
            S.box(cx_ + 3 - k, g + 1 + k, cz_ - 2, cx_ + 3 - k, g + 1 + k, cz_ + 2, "minecraft:red_wool")
    S.set(x, g + 1, z, "minecraft:blast_furnace[facing=south,lit=true]")
    S.set(x, g + 2, z, ROOK)
    for (dx, dz) in ((-3, 2), (-2, 3), (3, -3)):
        S.set(x + dx, g + 1, z + dz, "minecraft:barrel[facing=up,open=false]")
    P["mika_fort"] = [x, g + 1, z + 3]
    P["mika_fort_poort"] = [x, g + 1, z + 18]
    P["mika_fort_toren"] = [tx, g + 16, tz + 1]
    W.stempel(S.stempel("mika_fort"))


# ================================================================================================ De Spiegelburcht
def spiegelburcht(W, K, Pw):
    """The v1 castle, built in its v1 coordinates (x centred on 0, door plane z -2856) and moved as one piece by
    (BURCHT_DX, 0, BURCHT_DZ): bazen's puzzle rows/columns stay valid relative to it."""
    DX, DZ = PL.BURCHT_DX, PL.BURCHT_DZ
    P = {}
    T = PL.BURCHT_TOP
    Y = T + 1
    S = Vol(-62, 120, -2998, 62, 246, -2780)
    # ---- the grand stair from the bridge head up to the forecourt
    zn, hn = kloofrand(K, PL.BRUGGEN[2], -1)
    zn -= DZ
    z = zn - 6
    y = PL.BRUG_TOP
    S.box(-4, y - 12, z, 4, y, zn - 2, "minecraft:polished_tuff")
    trappen = []
    stap = 0
    while y < T:
        z -= 1
        if stap in (8, 17):
            for _ in range(3):
                S.box(-3, y - 14, z, 3, y, z, "minecraft:polished_tuff")
                S.box(-4, y - 14, z, -4, y + 1, z, "minecraft:tuff_bricks")
                S.box(4, y - 14, z, 4, y + 1, z, "minecraft:tuff_bricks")
                S.box(-3, y + 1, z, 3, y + 6, z, CARVE)
                z -= 1
            S.set(-4, y + 2, z + 2, LANT)
            S.set(4, y + 2, z + 2, LANT)
        y += 1
        stap += 1
        S.box(-3, y - 14, z, 3, y - 1, z, "minecraft:polished_tuff")
        S.box(-3, y, z, 3, y, z, trap("minecraft:polished_tuff_stairs", "north"))
        S.box(-4, y - 14, z, -4, y + 1, z, "minecraft:tuff_bricks")
        S.box(4, y - 14, z, 4, y + 1, z, "minecraft:tuff_bricks")
        S.box(-3, y + 1, z, 3, y + 6, z, CARVE)
    z -= 1
    # forecourt (always reaching the facade)
    D0 = PL.BURCHT_DEUR_Z - DZ
    zf = min(z - 4, D0 + 4)
    S.box(-12, T - 6, zf, 12, T, z, "minecraft:polished_tuff")
    S.box(-12, Y, zf, 12, Y + 10, z, CARVE)
    for x in range(-12, 13, 2):
        S.set(x, T, z - 2, "minecraft:smooth_quartz")
    vz = z - 2
    P["burcht_trap_voet"] = [0, PL.BRUG_TOP + 1, zn - 4]
    P["burcht_voorplein"] = [0, Y, vz]
    # ---- the hull of the front rooms (solid calcite, rooms carved out), facade 4 thick
    D = D0
    S.box(-19, T - 4, -2939, 19, T, D + 3, "minecraft:polished_tuff")
    S.box(-19, Y, -2939, 19, Y + 17, D + 3, "minecraft:calcite")
    S.box(-19, Y, -2939, 19, Y + 2, D + 3, "minecraft:tuff_bricks")
    S.box(-20, Y + 18, -2940, 20, Y + 18, D + 4, "minecraft:polished_tuff")
    for x in range(-20, 21, 2):
        S.set(x, Y + 19, D + 4, "minecraft:polished_tuff")
        S.set(x, Y + 19, -2940, "minecraft:polished_tuff")
    for zz in range(-2940, D + 5, 2):
        S.set(-20, Y + 19, zz, "minecraft:polished_tuff")
        S.set(20, Y + 19, zz, "minecraft:polished_tuff")
    # mirror windows outside (strips of grey glass)
    for zz in range(-2936, D, 4):
        for x in (-19, 19):
            S.box(x, Y + 5, zz, x, Y + 13, zz, "minecraft:light_gray_stained_glass")
    for name, (x0, z0, x1, z1, h) in PL.BURCHT_KAMERS.items():
        S.box(x0, T, z0, x1, T, z1, "minecraft:polished_tuff")
        for x in range(x0, x1 + 1):
            for zz in range(z0, z1 + 1):
                if (x + zz) % 2 == 0:
                    S.set(x, T, zz, "minecraft:smooth_quartz")
        for x in range(x0 + 3, x1 - 1, 6):
            for zz in range(z0 + 3, z1 - 1, 6):
                S.set(x, T, zz, "minecraft:pearlescent_froglight[axis=y]")
        S.box(x0, Y, z0, x1, Y + h - 1, z1, CARVE)
        # mirror panels on the side walls, end rods for light
        for zz in range(z0 + 1, z1, 3):
            S.box(x0 - 1, Y + 1, zz, x0 - 1, Y + h - 3, zz, "minecraft:white_stained_glass")
            S.box(x1 + 1, Y + 1, zz, x1 + 1, Y + h - 3, zz, "minecraft:white_stained_glass")
        for zz in range(z0 + 2, z1, 5):
            S.set(x0, Y + h - 3, zz, "minecraft:end_rod[facing=east]")
            S.set(x1, Y + h - 3, zz, "minecraft:end_rod[facing=west]")
        P[f"burcht_{name}"] = [0, Y, (z0 + z1) // 2]
    # gates between the rooms (open AIR, 5 wide, 5 high: bazen fills them)
    for i, zz in enumerate((-2880, -2900, -2918, -2938)):
        S.box(-2, Y, zz, 2, Y + 4, zz, CARVE)
        S.box(-3, Y + 5, zz, 3, Y + 5, zz, "minecraft:polished_tuff")
        P[f"poort_{i + 1}"] = [0, Y, zz]
    # puzzle spots (free floor, marked with chiseled quartz in the floor)
    spots = {
        "lichtkamer": [(-7, -2894), (7, -2894), (-7, -2885), (7, -2885), (0, -2890)],
        "spiegelgang": [(-5, -2904), (5, -2904), (-5, -2909), (5, -2909), (-5, -2914), (5, -2914)],
        "prismazaal": [(0, -2928), (-8, -2923), (8, -2923), (-8, -2933), (8, -2933)],
    }
    for kamer, lijst in spots.items():
        for n, (x, zz) in enumerate(lijst):
            S.set(x, T, zz, "minecraft:chiseled_quartz_block")
            P[f"puzzel_{kamer}_{n + 1}"] = [x, Y, zz]
    S.box(-15, Y + 2, -2891, -15, Y + 4, -2889, CARVE)
    S.set(-15, Y + 1, -2890, "minecraft:polished_tuff")
    P["puzzel_lichtkamer_bron"] = [-14, Y + 2, -2890]
    P["puzzel_lichtkamer_doel"] = [0, Y + 6, -2899]
    # ---- the front door (moving, WereldDeuren id "spiegelburcht"): door plane z D, x -3..3, y Y..Y+8
    S.box(-3, Y, D + 1, 3, Y + 8, D + 3, CARVE)
    S.box(-4, Y + 9, D, 4, Y + 10, D + 3, "minecraft:polished_tuff")
    for x in range(-3, 4):
        for yy in range(Y, Y + 9):
            rand = x in (-3, 3) or yy in (Y, Y + 8)
            b = "minecraft:polished_tuff" if rand else ("minecraft:light_gray_stained_glass" if (x + yy) % 2 else "minecraft:calcite")
            S.set(x, yy, D, b)
    for (x, yy) in ((-1, Y + 5), (1, Y + 5), (0, Y + 4), (-1, Y + 4), (1, Y + 4), (0, Y + 3)):
        S.set(x, yy, D, "minecraft:pink_stained_glass")
    # arch + two front towers
    for k in range(5):
        S.box(-5 - k, Y + 9 + k, D + 3, 5 + k, Y + 9 + k, D + 3, "minecraft:calcite", keep=False) if False else None
    for tx in (-15, 15):
        S.cyl(tx, D + 1, 4, Y, Y + 26, "minecraft:calcite")
        S.cyl(tx, D + 1, 4, Y, Y + 2, "minecraft:tuff_bricks")
        S.kegel(tx, D + 1, Y + 27, 22, 4.5, 0.4, "minecraft:light_gray_stained_glass")
        S.kegel(tx, D + 1, Y + 27, 16, 2, 0.2, "minecraft:calcite")
    for tx, tz in ((-19, -2938), (19, -2938)):
        S.kegel(tx, tz, Y + 19, 18, 3.5, 0.3, "minecraft:light_gray_stained_glass")
    # the central crystal spire over the entrance hall, with a faint pink heart at its top
    S.kegel(0, -2868, Y + 19, 44, 7, 0.4, "minecraft:light_gray_stained_glass")
    S.kegel(0, -2868, Y + 19, 36, 3.5, 0.3, "minecraft:calcite")
    S.set(0, Y + 58, -2868, "minecraft:pink_stained_glass")
    # secret: a ladder up the east wall to the roof, a Mika hat up there
    for yy in range(Y, Y + 19):
        S.set(20, yy, -2872, "minecraft:ladder[facing=east,waterlogged=false]")
    S.set(20, Y + 19, -2872, CARVE)
    S.box(19, Y + 19, -2873, 19, Y + 19, -2871, CARVE)
    hx, hz = 10, -2905
    S.set(hx, Y + 19, hz, "minecraft:red_wool")
    S.box(hx - 1, Y + 19, hz - 1, hx + 1, Y + 19, hz + 1, "minecraft:black_carpet", keep=True)
    S.set(hx, Y + 20, hz, "minecraft:black_carpet")
    P["geheim_burchtdak"] = [hx, Y + 19, hz + 2]
    P["deur_spiegelburcht"] = {"min": [-3, Y, D], "max": [3, Y + 8, D], "voor": [0, Y, D + 5], "as_": "x"}
    P["deur_spiegelburcht_voor"] = [0, Y, D + 5]
    # ---- the boss hall: round, a grey glass dome with calcite ribs
    hx_, hz_, hr = PL.SPIEGELZAAL
    S.cyl(hx_, hz_, hr + 3, T - 4, T, "minecraft:polished_tuff")
    S.cyl(hx_, hz_, hr + 3, Y, Y + 14, "minecraft:calcite")
    S.cyl(hx_, hz_, hr + 3, Y, Y + 2, "minecraft:tuff_bricks")
    S.ell(hx_, Y + 14, hz_, hr + 3, hr, hr + 3, "minecraft:light_gray_stained_glass", hollow=2, ymin=Y + 15)
    for k in range(12):
        a = k * math.pi / 6
        for t in np.linspace(0, math.pi / 2, 40):
            x = hx_ + (hr + 2) * math.cos(t) * math.cos(a)
            zz = hz_ + (hr + 2) * math.cos(t) * math.sin(a)
            S.set(round(x), round(Y + 14 + (hr - 1) * math.sin(t)), round(zz), "minecraft:calcite")
    S.cyl(hx_, hz_, hr, Y, Y + 14, CARVE)
    S.ell(hx_, Y + 14, hz_, hr, hr - 2, hr, CARVE, ymin=Y + 14)
    for x in range(hx_ - hr, hx_ + hr + 1):
        for zz in range(hz_ - hr, hz_ + hr + 1):
            d = math.hypot(x - hx_, zz - hz_)
            if d <= hr:
                S.set(x, T, zz, "minecraft:smooth_quartz" if int(d) % 4 < 2 else "minecraft:polished_tuff")
    S.set(hx_, T, hz_, "minecraft:pearlescent_froglight[axis=y]")
    S.box(-2, Y, hz_ + hr - 1, 2, Y + 4, -2938, CARVE)          # the gate from the Prismazaal (open AIR)
    S.box(-3, Y + 5, hz_ + hr, 3, Y + 5, -2938, "minecraft:polished_tuff")
    for k in range(6):
        a = k * math.pi / 3
        x, zz = round(hx_ + 17 * math.cos(a)), round(hz_ + 17 * math.sin(a))
        wx, wz = round(hx_ + (hr + 1) * math.cos(a)), round(hz_ + (hr + 1) * math.sin(a))
        S.box(wx - 1, Y, wz - 1, wx + 1, Y + 6, wz + 1, CARVE)
        S.box(round(hx_ + (hr + 2.4) * math.cos(a)), Y, round(hz_ + (hr + 2.4) * math.sin(a)),
              round(hx_ + (hr + 2.4) * math.cos(a)), Y + 6, round(hz_ + (hr + 2.4) * math.sin(a)), "minecraft:white_stained_glass")
        P[f"spiegelzucht_spiegel_{k + 1}"] = [x, Y, zz]
    for k in range(8):
        a = k * math.pi / 4
        S.set(round(hx_ + (hr - 0.5) * math.cos(a)), Y + 8, round(hz_ + (hr - 0.5) * math.sin(a)), "minecraft:sea_lantern")
    sx, sz = 0, hz_ - 17
    S.box(sx - 1, Y, sz - 1, sx + 1, Y, sz + 1, "minecraft:calcite")
    S.set(sx, Y + 1, sz, "minecraft:polished_tuff_slab[type=bottom,waterlogged=false]")
    P["burcht_splinter"] = [sx, Y, sz + 2]
    P["arena_spiegelzucht"] = [hx_, Y, hz_]
    P["arena_spiegelzucht_straal"] = [hr, 0, 0]
    P["arena_spiegelzucht_deur"] = [0, Y, -2937]
    P["arena_spiegelzucht_bord"] = [4, Y, -2936]
    S.o = (S.o[0] + DX, S.o[1], S.o[2] + DZ)
    W.stempel(S.stempel("spiegelburcht"))

    def schuif(o):
        if isinstance(o, list) and len(o) == 3 and all(isinstance(v, int) for v in o):
            return [o[0] + DX, o[1], o[2] + DZ]
        if isinstance(o, dict):
            return {k: (v if k == "as_" else schuif(v)) for k, v in o.items()}
        return o
    for k, v in P.items():
        Pw[k] = v if k.endswith("_straal") else schuif(v)


# ================================================================================================ the edge: cloud sea, landmarks
def rand(W, K, P):
    """T-WERELD: the cloud rim follows the real (organic) coast instead of the rectangle of the tile grid: irregular banks with
    gaps, clusters and noisy height; towards the east they become a low broken rim that 'falls' into the Wolkenkloof (zone 2 ->
    3). The rectangle of puffs along the grid edge and the far landmark spires at the old grid corners are gone; the
    Splinterwand (zone 1 -> 2) is stamped from the shared barrier list."""
    rng = random.Random(8)
    ux0, uz0 = PL.UIT_TX0 * 64, PL.UIT_TZ0 * 64
    ux1, uz1 = (PL.UIT_TX1 + 1) * 64 - 1, (PL.UIT_TZ1 + 1) * 64 - 1
    zd = K.zeed
    kand = np.argwhere((zd > 18) & (zd < 46))
    rng_np = np.random.default_rng(81)
    rng_np.shuffle(kand)
    gekozen = []
    for (i, j) in kand:
        x, z = int(i + PL.X0), int(j + PL.Z0)
        if not (ux0 + 20 <= x <= ux1 - 20 and uz0 + 20 <= z <= uz1 - 20):
            continue
        if any((x - a) ** 2 + (z - b) ** 2 < 34 ** 2 for (a, b) in gekozen):
            continue
        if rng.random() < 0.18:
            continue                                       # a gap
        gekozen.append((x, z))
        oost = min(max((x - 140) / 120, 0), 1)
        y = int(rng.randint(60, 74) - 6 * oost)
        n = 4 + int(3 * oost) + rng.randint(0, 2)
        S = Vol(x - 30, y - 12, z - 30, x + 30, y + 10, z + 30)
        for k in range(n):
            S.ell(x + rng.uniform(-15, 15), y + rng.uniform(-2, 3) - k * oost * 1.5, z + rng.uniform(-15, 15), rng.uniform(7, 14),
                  rng.uniform(2.5, 5.5), rng.uniform(7, 14), rng.choice(["wolkenblok_wit", "wolkenblok_wit", "minecraft:white_wool"]))
        W.stempel(S.stempel("wolk_kust"), bezet=False)
    print(f"  zone 2: {len(gekozen)} wolkenbanken langs de kust")
    # ice floes on the sea (only in the written part of the grid)
    S = Vol(ux0, PL.ZEE - 1, uz0, ux1, PL.ZEE + 2, uz1)
    for _ in range(140):
        x, z = rng.randint(ux0 + 4, ux1 - 4), rng.randint(uz0 + 4, uz1 - 4)
        if K.H[K2.idx(x, z)] >= 0:
            continue
        S.cyl(x, z, rng.uniform(1.5, 4.5), PL.ZEE, PL.ZEE, "minecraft:packed_ice")
    W.stempel(S.stempel("schotsen"), bezet=False)
    # the Splinterwand spires in this grid (world -> local frame)
    import os as _os
    import sys as _sys
    _sys.path.insert(0, _os.path.join(_os.path.dirname(_os.path.abspath(__file__)), ".."))
    import barriere
    import indeling as IND
    import tegels as TG
    for st in barriere.stempels(TG.Stempel, doos=(ux0 + IND.Z2_DX, uz0 + IND.Z2_DZ, ux1 + IND.Z2_DX, uz1 + IND.Z2_DZ),
                                dx=IND.Z2_DX, dz=IND.Z2_DZ):
        W.stempel(st, bezet=False)
    for i, (x, z, h) in enumerate(()):
        S = Vol(x - 30, 40, z - 30, x + 30, 62 + h + 4, z + 30)
        S.kegel(x, z, 50, h + 12, 10, 0.5, "minecraft:light_blue_stained_glass")
        S.kegel(x, z, 50, h, 5, 0.3, "minecraft:blue_ice")
        for k in range(5):
            a = k * 2 * math.pi / 5 + i
            S.kegel(x + 11 * math.cos(a), z + 11 * math.sin(a), 50, h // 3 + k * 4, 4, 0.3, "minecraft:white_stained_glass",
                    scheef=(6 * math.cos(a), 6 * math.sin(a)))
        P[f"landmark_spits_{i + 1}"] = [x, 62 + h, z]
        W.stempel(S.stempel(f"spits_{i}"), bezet=False)


# ================================================================================================ T-WERELD: harbours
def galmdok(W, K, P):
    """haven_z2_z3_vertrek: the Galmdok on the south-east cape (line B over the Wolkenkloof to the Weststeiger of zone 3); the
    platform's far corner is the viewpoint Galmkam (U3). Also the shared ids of the existing Kandijkom dock (line A, the other
    end of the Noordklifhaven) and its balcony viewpoint (U2)."""
    import bouwsels as BZ
    import havens
    cx, cz = PL.GALMDOK
    y = K.galmdok_y
    x0, z0, x1, z1 = havens.doos(cx + PL.GALMDOK_R - 2, cz, 1, 0, 24)
    S = BZ.WS(x0, 30, z0, x1 - x0 + 1, y - 30 + 20, z1 - z0 + 1)
    for a in range(0, 360, 36):
        x, z = cx + (PL.GALMDOK_R - 1) * math.cos(math.radians(a)), cz + (PL.GALMDOK_R - 1) * math.sin(math.radians(a))
        if a in (0, 36, 324):
            continue          # the pier leaves east
        S.set(x, y + 1, z, LANT if a % 72 == 0 else "minecraft:spruce_slab[type=bottom,waterlogged=false]")
    havens.pier(S, cx + PL.GALMDOK_R - 2, y, cz, 1, 0, 24, lambda x, z: hoogste(W, int(x), int(z)), "haven_z2_z3", P,
                bodem=40, wol="minecraft:light_blue_wool")
    P["haven_z2_z3_plein"] = [cx, y + 1, cz]
    e = P["haven_z2_z3_eind"]
    # U-OVERGANG: no viewpoint at the harbour (the Galmtop needle right behind it is the epic one)
    W.stempel(S.stempel("galmdok"))
    # the Kandijkom dock = the zone-2 end of line A (Noordklifhaven <-> Kandijkom-dok)
    P["haven_z2_z1_vertrek"] = list(P["dok_instap"])   # W-SCHEPEN: the tip (was dok_landing, 20 blocks inside the dock)
    S = BZ.WS(-12, FY - 2, -2240, 10, 12, 16)
    P["haven_z2_z1_bord"] = havens.bord(S, -9, FY, -2232, 0, 1, 1, 0)
    W.stempel(S.stempel("dok_bord"))
    # U-OVERGANG: the dock end is no viewpoint any more


# ================================================================================================ decoration
def decoratie(W, K, P):
    rng = np.random.default_rng(5)
    pal = W.palet
    H = K.H
    i_snow = pal.id(laag(1))
    i_gras = pal.id("minecraft:grass_block[snowy=true]")
    # snow layer on all snowy grass
    for i, j in zip(*np.nonzero((W.top == i_gras) & ~W.bezet & (K.pad_m < 0.2))):
        x, z = int(i) + PL.X0, int(j) + PL.Z0
        W.punten.set(x, int(H[i, j]) + 1, z, laag(1))
    # spruces: low/middle ground, not on paths, town, lake, ravine, pads, needles, steep rock
    kans = np.clip(0.002 + 0.008 * K.n2, 0, 0.011) * np.where(K2.ZZ < -2560, 0.35, 1.0)
    hoog = (H > PL.ZEE + 3) & (H < 140)
    m = hoog & ~W.bezet & ~W.steil & (K.pad_m < 0.05) & ~K.stad_vloer & ~K.meer & ~K.kloof & ~K.kabel & (K.meer_e > 1.12)
    from scipy import ndimage as _nd
    m &= ~_nd.binary_dilation(K.pad_m > 0.05, iterations=4)
    Hl = H.astype(np.int32)
    m &= (_nd.maximum_filter(Hl, size=7) - _nd.minimum_filter(Hl, size=7)) <= 3     # no trees on slopes (no pockets under leaves)
    m &= ~K.burcht_plat & (rng.random(H.shape) < kans) & (K.geode_r > PL.GEODE_R + 30)
    for (px, pz, pr, ptop, fall) in K.pads:
        m &= np.hypot(K2.XX - px, K2.ZZ - pz) > pr + 8
    ii, jj = np.nonzero(m)
    r2 = random.Random(6)
    blad = "minecraft:spruce_leaves"
    for i, j in zip(ii, jj):
        x, z, g = int(i) + PL.X0, int(j) + PL.Z0, int(H[i, j])
        h = r2.randint(6, 11)
        for y in range(g + 1, g + h + 1):
            W.punten.set(x, y, z, "minecraft:spruce_log[axis=y]")
        for k in range(h - 1):
            y = g + 3 + k
            r = max(0, int(round(3.2 * (1 - k / (h - 1)) + (0.6 if k % 2 == 0 else 0))))
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if (dx or dz) and abs(dx) + abs(dz) <= r + (1 if r > 1 else 0):
                        W.punten.set(x + dx, y, z + dz, blad)
        W.punten.set(x, g + h + 1, z, blad)
        W.punten.set(x, g + h + 2, z, laag(2))
    # crystal sprinkles on the Galm snow, ice flowers in the low south
    m2 = ~W.bezet & ~W.steil & (K.pad_d > 3) & (rng.random(H.shape) < 0.006) & (H > PL.ZEE + 3) & ~K.kabel & ~K.stad_vloer & ~K.meer
    kristal = ["minecraft:amethyst_cluster[facing=up,waterlogged=false]", "guhs:guh_kristal_cluster[facing=up]",
               "minecraft:large_amethyst_bud[facing=up,waterlogged=false]", "guhs:guh_kristal_cluster[facing=up]"]
    for n, (i, j) in enumerate(zip(*np.nonzero(m2))):
        W.punten.set(int(i) + PL.X0, int(H[i, j]) + 1, int(j) + PL.Z0, kristal[n % len(kristal)])
    m4 = (W.top == i_gras) & ~W.bezet & (K.pad_d > 2) & (rng.random(H.shape) < 0.05)
    for i, j in zip(*np.nonzero(m4)):
        W.punten.set(int(i) + PL.X0, int(H[i, j]) + 1, int(j) + PL.Z0, "minecraft:pink_petals[facing=north,flower_amount=3]")
    m3 = (W.top == i_gras) & ~W.bezet & (K.pad_m < 0.05) & (rng.random(H.shape) < 0.01)
    for i, j in zip(*np.nonzero(m3)):
        W.punten.set(int(i) + PL.X0, int(H[i, j]) + 1, int(j) + PL.Z0, "guhs:guh_ijsbloempje")
    P["aantal_bomen"] = [int(len(ii)), 0, 0]


# ================================================================================================ mob zones (boxes) + all
MOBZONES = {
    "mob_galm_voet": ((86, -2400), (250, -2200), (20, 21)),
    "mob_galm_top": ((86, -2504), (250, -2401), (22, 23)),
    "mob_meer_oever": ((-250, -2330), (-86, -2200), (23, 24)),
    "mob_meer_ijs": ((-226, -2448), (-118, -2331), (24, 25)),
    "mob_meer_noord": ((-250, -2504), (-86, -2449), (25, 26)),
    "mob_kloof_zuid": ((-236, -2528), (236, -2505), (26, 27)),
    "mob_kloof_bodem": ((-236, -2565), (236, -2529), (27, 28)),
    "mob_kloof_noord": ((-236, -2775), (109, -2566), (28, 29)),
    "mob_burcht": ((-19 + PL.BURCHT_DX, -2984 + PL.BURCHT_DZ), (19 + PL.BURCHT_DX, -2856 + PL.BURCHT_DZ), (28, 30)),
}


def stand(W, x, z):
    return [int(x), hoogste(W, int(x), int(z)) + 1, int(z)]


# ================================================================================================ fix round 2: drama + life
def spleetbrug(W, K, P):
    """brug_5: a hanging bridge E-W over the Galmspleet (the gondola gorge), on the rim path to the main bridge."""
    z = PL.SPLEETBRUG_Z
    x = PL.GALMSPLEET[1][0]
    xw, xe = x, x
    while K.spleet[K2.idx(xw, z)] or K.kloof[K2.idx(xw, z)]:
        xw -= 1
    while K.spleet[K2.idx(xe, z)] or K.kloof[K2.idx(xe, z)]:
        xe += 1
    xa, _, _ = _landing(K, xw, z, -1, 0)
    xb, _, _ = _landing(K, xe, z, 1, 0)
    S = Vol(xa - 4, 40, z - 6, xb + 4, 200, z + 6)
    hangbrug(K, S, [(x_, z) for x_ in range(xa, xb + 1)], (0, 1), 1, "minecraft:spruce", "minecraft:spruce", HEK_X,
             sag=3.0, ketting=5, lampion=True)
    W.stempel(S.stempel("brug_spleet"))
    P["brug_5_zuid"] = stand(W, xw - 3, z)        # (west end; ids kept: _zuid = the town side)
    P["brug_5_noord"] = stand(W, xe + 3, z)


def gehuchten(W, K, P):
    """Little guh hamlets / warm camps along the way: 2-3 cosy huts (real interiors), a stall, fire baskets, garlands."""
    rng = random.Random(41)
    for (gid, gx, gz, n) in PL.GEHUCHTEN:
        S = Vol(gx - 30, 40, gz - 30, gx + 30, 240, gz + 30)
        hutten = []
        pogingen = 0
        while len(hutten) < n and pogingen < 200:
            pogingen += 1
            a, d = rng.uniform(0, 2 * math.pi), rng.uniform(8, 20)
            x, z = round(gx + d * math.cos(a)), round(gz + d * math.sin(a))
            if any(math.hypot(x - hx, z - hz) < 11 for (hx, hz, _) in hutten):
                continue
            i, j = K2.idx(x, z)
            sl = (slice(i - 6, i + 7), slice(j - 6, j + 7))
            if (K.pad_d[sl] < 3.5).any() or K.kloof[sl].any() or K.meer[sl].any() or K.stad_vloer[sl].any() or K.kabel[sl].any():
                continue
            hs = K.H[sl].astype(int)
            if hs.max() - hs.min() > 5 or (W.bezet[sl]).any():
                continue
            # door towards the camp centre
            ddx, ddz = gx - x, gz - z
            deur = ("oost" if ddx > 0 else "west") if abs(ddx) > abs(ddz) else ("zuid" if ddz > 0 else "noord")
            F = Kader(x, z, 7, 7, deur)
            fx, fz = F.p(0, -1)
            fy = int(K.H[K2.idx(fx, fz)])
            S.box(x - 4, fy - 6, z - 4, x + 4, fy, z + 4, "minecraft:stone_bricks")
            S.box(x - 4, fy + 1, z - 4, x + 4, fy + 12, z + 4, CARVE)
            huis(S, P, gid, x, z, 7, 7, deur, "huis", "hutje", fy, kleur=rng.randrange(10))
            hutten.append((x, z, fy))
        if not hutten:
            continue
        # the camp square: a stall, fire baskets, garlands between the huts
        g = hoogste(W, gx, gz)
        i, j = K2.idx(gx, gz)
        if K.pad_d[i, j] > 4:
            S.set(gx, g + 1, gz, VUURKORF)
        for k, (hx, hz, fy) in enumerate(hutten):
            hn = hutten[(k + 1) % len(hutten)]
            if len(hutten) > 1:
                slinger(S, (hx, fy + 7, hz), (hn[0], hn[2] + 7, hn[1]), zak=2, elk=3)
        kx, kz = gx + 4, gz + 4
        i, j = K2.idx(kx, kz)
        if K.pad_d[i, j] > 5:
            kraam(S, kx, kz, "west", int(K.H[i, j]), "minecraft:pink_wool", "minecraft:white_wool")
        P[f"gehucht_{gid}"] = [gx, g + 1, gz]
        W.stempel(S.stempel(f"gehucht_{gid}"))


def kristalbossen(W, K, P):
    """Crystal forests: crystal 'trees' with an amethyst/calcite trunk, a glassy coloured crown and a glowing heart."""
    rng = random.Random(43)
    kleuren = {"roze": ["minecraft:pink_stained_glass", "minecraft:magenta_stained_glass"],
               "paars": ["minecraft:purple_stained_glass", "minecraft:magenta_stained_glass"],
               "blauw": ["minecraft:light_blue_stained_glass", "minecraft:cyan_stained_glass"]}
    for n, (bx, bz, br, kl) in enumerate(PL.KRISTALBOSSEN):
        S = Vol(bx - br - 10, 40, bz - br - 10, bx + br + 10, 260, bz + br + 10)
        geplant = 0
        for _ in range(int(br * br / 12)):
            a, d = rng.uniform(0, 2 * math.pi), br * math.sqrt(rng.random())
            x, z = round(bx + d * math.cos(a)), round(bz + d * math.sin(a))
            i, j = K2.idx(x, z)
            sl = (slice(i - 4, i + 5), slice(j - 4, j + 5))
            if (K.pad_d[sl] < 3).any() or K.kloof[sl].any() or K.meer[sl].any() or K.stad_vloer[sl].any() or W.bezet[i, j] or K.kabel[sl].any():
                continue
            if int(K.H[sl].max()) - int(K.H[sl].min()) > 3:
                continue
            g = int(K.H[i, j])
            h = rng.randint(5, 10)
            S.box(x, g + 1, z, x, g + h, z, rng.choice(["minecraft:amethyst_block", "minecraft:calcite", "guhs:guh_kristal_blok"]))
            glas = rng.choice(kleuren[kl])
            r = rng.uniform(2.2, 3.4)
            S.ell(x, g + h + 1, z, r, r * 1.2, r, glas, keep=True)
            S.set(x, g + h + 1, z, rng.choice(["minecraft:pearlescent_froglight[axis=y]", "guhs:guh_kristal_lamp", "minecraft:verdant_froglight[axis=y]"]))
            for _k in range(3):
                aa = rng.uniform(0, 2 * math.pi)
                S.set(round(x + (r + 0.6) * math.cos(aa)), g + h, round(z + (r + 0.6) * math.sin(aa)),
                      "minecraft:amethyst_cluster[facing=down,waterlogged=false]", keep=True)
            for _k in range(2):
                aa = rng.uniform(0, 2 * math.pi)
                S.set(round(x + 1.5 * math.cos(aa)), g + 1, round(z + 1.5 * math.sin(aa)), "minecraft:small_amethyst_bud[facing=up,waterlogged=false]", keep=True)
            geplant += 1
        P[f"kristalbos_{n + 1}"] = stand(W, bx, bz)
        W.stempel(S.stempel(f"kristalbos_{n}"))


def ijsvallen(W, K, P):
    """Frozen waterfalls down the ravine walls (packed/blue ice + glass, a glowing core) with a frozen pool at the foot."""
    for n, (x, z, kant) in enumerate(PL.IJSVALLEN):
        S = Vol(x - 8, 50, z - 12, x + 8, 200, z + 12)
        if kant == 0:
            continue
        for dx in range(-3, 4):
            zr, hr = kloofrand(K, x + dx, kant)
            zi = zr - kant                # the last column inside the ravine (against the wall)
            breed = 3 - abs(dx)
            bodem = PL.KLOOF_BODEM + 1
            for y in range(bodem, hr + 1):
                b = "minecraft:packed_ice" if (y + dx) % 4 else "minecraft:light_blue_stained_glass"
                if abs(dx) <= 1:
                    b = "minecraft:blue_ice" if (y % 3) else "minecraft:light_blue_stained_glass"
                S.set(x + dx, y, zi, b)
                if breed >= 2 and y < bodem + 8:
                    S.set(x + dx, y, zi - kant, "minecraft:packed_ice")
            if dx == 0:
                for y in range(bodem + 4, hr, 7):
                    S.set(x, y, zr, "minecraft:sea_lantern")
        S.cyl(x, round(float(K2.kloof_z(x)) + kant * (K.kloof_w[K2.idx(x, z)] - 5)), 4, PL.KLOOF_BODEM, PL.KLOOF_BODEM, "minecraft:packed_ice")
        P[f"ijsval_{n + 1}"] = [x, PL.KLOOF_BODEM + 2, z]
        W.stempel(S.stempel(f"ijsval_{n}"), bezet=False)


def lawinegolf(W, K, P):
    """The avalanche as a set piece: a giant wave of snow frozen mid-crash, curling over the path, boulders hanging in it."""
    lx, lz = PL.LAWINE_GOLF
    g = hoogste(W, lx, lz + 12)
    S = Vol(lx - 18, g - 4, lz - 20, lx + 18, g + 30, lz + 22)
    zc, yc, R = lz + 6, g + 10, 10.0
    rng = random.Random(51)
    for x in range(lx - 14, lx + 15):
        w = 1 - (abs(x - lx) / 15) ** 2
        Rx = R * (0.7 + 0.3 * w)
        for k in range(0, 72):
            a = math.radians(-35 + k * 3.2)        # -35 deg (lip, south, low) .. 195 deg (back on the slope)
            for dr in (0.0, 0.8, 1.6):
                rr = Rx - dr
                z = zc + rr * math.cos(math.radians(180) - a) * -1
                y = yc + rr * math.sin(a)
                if y < g + 1:
                    continue
                b = SNEEUW if dr < 1 else ("minecraft:packed_ice" if (k + x) % 5 else "minecraft:light_blue_stained_glass")
                S.set(x, y, z, b)
        # spray along the lip
        if x % 3 == 0:
            S.set(x, round(yc + Rx * math.sin(math.radians(-35))) - 1, round(zc + Rx * math.cos(math.radians(-35))), "minecraft:end_rod[facing=down]")
    # the path under it stays free (4 blocks of headroom over every path cell)
    for x in range(lx - 17, lx + 18):
        for z in range(lz - 8, lz + 22):
            i, j = K2.idx(x, z)
            if K.pad_d[i, j] < 4:
                S.box(x, int(K.H[i, j]) + 1, z, x, int(K.H[i, j]) + 5, z, CARVE)
    for _ in range(9):
        x, z = lx + rng.randint(-12, 12), zc + rng.randint(-8, 2)
        y = yc + rng.randint(2, 8)
        S.ell(x, y, z, rng.uniform(1.2, 2.2), rng.uniform(1.0, 1.8), rng.uniform(1.2, 2.2), SNEEUW)
    P["lawine_golf"] = stand(W, lx, lz + 14)
    W.stempel(S.stempel("lawinegolf"))


def warmwaterbron(W, K, P):
    """A little steaming hot spring (1 deep, you can always walk out) with warm stones and soul lanterns."""
    x, z = PL.WARMWATERBRON
    g = K.h(x, z)
    S = Vol(x - 7, g - 5, z - 7, x + 7, g + 6, z + 7)
    S.cyl(x, z, 5.5, g - 3, g, "minecraft:mossy_cobblestone")
    S.cyl(x, z, 5.5, g + 1, g + 5, CARVE)
    S.cyl(x, z, 3.5, g, g, "minecraft:water")
    S.cyl(x, z, 3.5, g - 1, g - 1, "minecraft:magma_block") if False else S.cyl(x, z, 3.5, g - 1, g - 1, "minecraft:gravel")
    for k in range(6):
        a = k * math.pi / 3
        S.set(round(x + 5 * math.cos(a)), g + 1, round(z + 5 * math.sin(a)), "minecraft:soul_lantern[hanging=false,waterlogged=false]" if k % 2 else "minecraft:mossy_cobblestone_slab[type=bottom,waterlogged=false]")
    P["warmwaterbron"] = [x, g + 1, z]
    W.stempel(S.stempel("warmwaterbron"))


def padlampen(W, K, P):
    """Lampion posts along every path (every ~20 blocks, alternating sides, never on the path)."""
    S = Vol(-300, 40, -2820, 300, 240, -2150)
    for naam, pts in PL.PADEN.items():
        kant = 1
        for (ax, az), (bx, bz) in zip(pts, pts[1:]):
            L = math.dist((ax, az), (bx, bz))
            ux, uz = (bx - ax) / L, (bz - az) / L
            t = 10.0
            while t < L - 4:
                x, z = round(ax + ux * t - uz * 4 * kant), round(az + uz * t + ux * 4 * kant)
                i, j = K2.idx(x, z)
                if K.pad_d[i, j] >= 3.2 and not K.kloof[i, j] and not K.stad_vloer[i, j] and not W.bezet[i, j] and not K.meer[i, j]:
                    g = int(K.H[i, j])
                    S.box(x, g + 1, z, x, g + 2, z, STAAF)
                    S.set(x, g + 3, z, LAMPION_S)
                kant = -kant
                t += 20
    W.stempel(S.stempel("padlampen"), bezet=False)


def opening_naald(W, K, P):
    """The crystal needle on the geode rim the Galmwolf howls on in the opening (visible from the dock)."""
    x, z = 46, -2344
    g = hoogste(W, x, z)
    S = Vol(x - 12, g - 4, z - 12, x + 12, g + 50, z + 12)
    kristalnaald(S, x, z, g, 30, 3.0, random.Random(3), roze="roze")
    P["opening_naald"] = [x, g + 31, z]
    W.stempel(S.stempel("opening_naald"))


def schat_onder_brug(W, K, P):
    """A ledge with a chest under the south head of brug_2, a ladder down the ravine wall from the rim."""
    bx = PL.BRUGGEN[1] + 4
    zr, hr = kloofrand(K, bx, 1)
    zi = zr - 1
    y = hr - 7
    S = Vol(bx - 4, y - 3, zi - 4, bx + 4, hr + 3, zr + 3)
    S.box(bx - 1, y - 1, zi - 2, bx + 1, y - 1, zi, "minecraft:spruce_planks")
    S.box(bx - 1, y, zi - 2, bx + 1, y + 2, zi, CARVE)
    for yy in range(y, hr + 1):
        S.set(bx, yy, zr, f"minecraft:ladder[facing=north,waterlogged=false]")
        S.set(bx, yy, zr + 1, "minecraft:stone") if yy < hr else None
    S.box(bx, hr + 1, zr, bx, hr + 2, zr, CARVE)
    S.set(bx - 1, y, zi - 2, "minecraft:chest[facing=south,type=single,waterlogged=false]")
    S.set(bx + 1, y, zi - 2, LANT)
    for k in (-1, 0, 1):
        S.set(bx + k, y - 1, zi - 3, STAAF) if False else S.set(bx + k, y, zi - 3, STAAF)
    P["schat_onder_de_brug"] = [bx, y, zi - 1]
    W.stempel(S.stempel("schat_brug"))


def npc_plekken(W, K, P):
    """Stand spots for every zone-2 NPC/POI that npc used to keep as literals (npcplek.<id>, poiplek.<id>, ...)."""
    def plus(id_, dx, dz):
        b = P[id_] if "." not in id_ else _pad(P, id_)
        return stand(W, b[0] + dx, b[2] + dz)

    def huisdeur(h, dx=0, dz=0):
        d = P["gebouw"][h]["deur"]
        return stand(W, d[0] + dx, d[2] + dz)
    N = {}
    N["z2_kwarts"] = plus("questbord", 3, 1)
    # W-BEREIK: the guards stand ON the square: the topmost-block stand put the east guard on a 4-high wall (out of reach)
    def op_plein(dxs):
        pl = P["plein"]
        for dx in dxs:
            st = stand(W, pl[0] + dx, pl[2])
            if st[1] <= pl[1] + 1:
                return st
        return stand(W, pl[0] + dxs[0], pl[2])
    N["z2_wacht_plein_west"] = op_plein(range(-14, -7))
    N["z2_wacht_plein_oost"] = op_plein(range(14, 7, -1))
    N["z2_wapenhandelaar"] = list(P["gebouw"]["smid"]["binnen"])
    N["z2_opa_sneeuwbal"] = plus("cafe_terras", 2, 1)
    N["z2_bakker"] = huisdeur("huis_4", 1)
    N["z2_vlokje"] = plus("schaatsvijver", -PL.VIJVER_R - 2, 2)
    N["z2_uitvinder"] = huisdeur("huis_8", 0, 1)
    N["z2_galmpje"] = plus("plein", -8, -10)
    N["z2_postguh"] = huisdeur("huis_5", 0, 1)
    N["z2_sneeuwpop"] = huisdeur("huis_6", 2)
    N["z2_kasguh"] = huisdeur("huis_3", 0, 1)
    N["z2_winterslaap"] = huisdeur("huis_2", 1)
    N["z2_poollicht"] = huisdeur("huis_1", 1)
    gb = P["gondel_berg"]
    N["z2_gondel_boven"] = [gb[0] - 2, gb[1], gb[2] + 1]   # W-BEREIK: inside the top station, not on its roof (y + 11)
    k1 = P["kampvuur_galmhellingen_1"]
    N["z2_berggids"] = stand(W, k1[0] - 3, k1[2] + 1)
    k3 = P["kampvuur_knuffelmeer_3"]
    N["z2_beeldhouwer"] = stand(W, k3[0] - 3, k3[2] + 1)
    N["z2_touwtje"] = plus("brug_3_zuid", 3, 0)
    N["z2_prisma"] = plus("burcht_voorplein", 4, 0)
    N["z2_wacht_dok"] = list(P["ridder_kandijkom_dok_1"])
    for gebied, kort in (("galmhellingen", "galm"), ("knuffelmeer", "meer"), ("splinterkloof", "kloof")):
        for i in range(len(PL.KAMPVUREN[gebied])):
            N[f"z2_wacht_{kort}_{i + 1}"] = list(P[f"ridder_{gebied}_{i + 1}"])
    N["z2_wacht_burcht"] = list(P["ridder_spiegelburcht_1"])
    N["z2_versteend_zwier"] = list(P["meer_midden"])
    N["z2_versteend_snoek"] = list(P["meer_schotsen"])
    mx, mz, rx, rz = PL.MEER
    N["z2_versteend_pluk"] = [round(mx - rx * 0.6), PL.MEER_IJS + 1, mz + 8]
    ij = P["npc_ijsvisser"]
    N["z2_versteend_ijsje"] = stand(W, ij[0] - 2, ij[2])
    # town life (fix round 2): skaters, snowball kids, penguin guhs, the sledge family, market folk
    vx, vz = PL.VIJVER
    N["z2_schaatser_1"] = [vx - 3, SY, vz - 2]
    N["z2_schaatser_2"] = [vx + 3, SY, vz + 2]
    N["z2_sneeuwbal_1"] = plus("plein", -10, 16)
    N["z2_sneeuwbal_2"] = plus("plein", -4, 20)
    N["z2_pinguguh_1"] = plus("sneeuwguhspar", 3, 0)
    N["z2_pinguguh_2"] = plus("schaatsvijver", 9, -6)
    N["z2_pinguguh_3"] = plus("plein", 10, 18)
    N["z2_slee_1"] = stand(W, PL.STAL[0] - 10, PL.STAL[1] - 8)
    N["z2_slee_2"] = stand(W, PL.STAL[0] - 12, PL.STAL[1] - 6)
    kr = P["markt_kramen"]
    for i, k in enumerate(list(kr.values())[:3]):
        N[f"z2_marktguh_{i + 1}"] = list(k)
    for k, v in N.items():
        P[f"npcplek.{k}"] = v
    # literals in npc's POI/secret maps
    Q = {"z2_lawinepad": list(P["galm_lawine_1_voet"]), "z2_warmwaterbron": list(P["warmwaterbron"]), "z2_meer_scheur": list(P["meer_scheur"]),
         "z2_brug_1": list(P["brug_3_zuid"]), "z2_brug_2": list(P["brug_2_zuid"]), "z2_brug_3": list(P["brug_1_zuid"]),
         "z2_uitkijk_poollicht": list(P["uitkijk_kloofkijk"])}
    for k, v in Q.items():
        P[f"poiplek.{k}"] = v
    A = {"olievlek": plus("boortoren_3", 4, 4), "bouwtekening": plus("boortoren_1", 4, 4), "boorgaten": plus("galm_lawine_1_voet", 6, 2),
         "boorschema": huisdeur("huis_8", 0, 3), "warme_spleet": list(P["geheim_boorplaats"])}
    for k, v in A.items():
        P[f"aanwijzing.{k}"] = v
    P["schat.onder_de_brug"] = list(P["schat_onder_de_brug"])


def _pad(P, id_):
    o = P
    for k in id_.split("."):
        o = o[k]
    return o


def bouw_alles(W, K):
    P = {}
    stad(W, K, P)
    stal(W, K, P)
    dok(W, K, P)
    aankomst(W, K, P)
    poort_overgangen(W, K, P)
    gondel(W, K, P)
    galmhellingen(W, K, P)
    knuffelmeer(W, K, P)
    splinterkloof(W, K, P)
    spiegelburcht(W, K, P)
    mika_fort(W, K, P)
    for naam, (x, z) in PL.UITKIJK.items():
        ijsnaald(W, K, P, naam, x, z, PL.UITKIJK_HOOG[naam])
    kampvuren(W, K, P)
    opening_naald(W, K, P)
    lawinegolf(W, K, P)
    warmwaterbron(W, K, P)
    schat_onder_brug(W, K, P)
    ijsvallen(W, K, P)
    gehuchten(W, K, P)
    kristalbossen(W, K, P)
    padlampen(W, K, P)
    rand(W, K, P)
    galmdok(W, K, P)      # T-WERELD
    decoratie(W, K, P)
    npc_plekken(W, K, P)
    P["mobzones"] = {k: {"min": [a[0], 0, a[1]], "max": [b[0], 0, b[1]], "level": [lv[0], lv[1], 0]} for k, (a, b, lv) in MOBZONES.items()}
    return P
