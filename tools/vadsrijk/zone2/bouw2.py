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
def huis(S, P, gid, cx, cz, w, d, deur, soort, naam):
    x1, z1 = cx - w // 2, cz - d // 2
    x2, z2 = x1 + w - 1, z1 + d - 1
    hw = {"huis": 5, "winkel": 6, "smid": 6, "cafe": 6, "tempel": 12}[soort]
    top = SY + hw
    tempel = soort == "tempel"
    wand = "minecraft:calcite" if tempel else CHOCO
    hoek = "guhs:guh_kristal_blok" if tempel else "minecraft:dark_oak_log[axis=y]"
    S.box(x1, FY - 4, z1, x2, FY, z2, "minecraft:stone_bricks")
    S.box(x1, SY, z1, x2, top, z2, wand)
    S.box(x1 + 1, SY, z1 + 1, x2 - 1, top - 1, z2 - 1, CARVE)
    for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
        S.box(x, SY, z, x, top, z, hoek)
    S.box(x1, SY, z1, x2, SY, z2, "minecraft:stone_bricks" if not tempel else "minecraft:polished_diorite")
    S.box(x1 + 1, SY, z1 + 1, x2 - 1, SY, z2 - 1, CARVE)
    # floor: planks + a carpet runner (shops/houses/cafe), pink/white marble (temple)
    if tempel:
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                S.set(x, FY, z, "minecraft:smooth_quartz" if (x + z) % 2 else "minecraft:pink_terracotta")
    else:
        S.box(x1 + 1, FY, z1 + 1, x2 - 1, FY, z2 - 1, "minecraft:spruce_planks")
    # windows
    glas = "minecraft:pink_stained_glass" if tempel else "minecraft:orange_stained_glass"
    for x in range(x1 + 2, x2 - 1, 3):
        for z in (z1, z2):
            S.box(x, SY + 1, z, x, SY + (6 if tempel else 2), z, glas)
    for z in range(z1 + 2, z2 - 1, 3):
        for x in (x1, x2):
            S.box(x, SY + 1, z, x, SY + (6 if tempel else 2), z, glas)
    # door (centre of the door side), a little step of path outside
    dx, dz = DIR[deur]
    if dz:
        zd = z1 if dz < 0 else z2
        xd = cx
        breed = 2 if soort in ("winkel", "cafe", "smid", "tempel") else 1
        xs = [xd] if breed == 1 else [xd - 1, xd]
        S.box(xs[0] - 1, SY, zd, xs[-1] + 1, SY + 3, zd, "minecraft:dark_oak_planks" if not tempel else "minecraft:polished_diorite")
        for i, x in enumerate(xs):
            hinge = "left" if i == 0 else "right"
            f = FACE[deur] if False else TEGEN[FACE[deur]]
            S.set(x, SY, zd, f"minecraft:spruce_door[facing={f},half=lower,hinge={hinge},open=false,powered=false]")
            S.set(x, SY + 1, zd, f"minecraft:spruce_door[facing={f},half=upper,hinge={hinge},open=false,powered=false]")
            S.set(x, FY, zd + dz, "guhs:knuffelklinkers")
            S.set(x, FY, zd - dz, "minecraft:spruce_planks" if not tempel else "minecraft:smooth_quartz")
        buiten = [xd, SY, zd + 2 * dz]
        runner = [(xd, z) for z in range(z1 + 1, z2)]
    else:
        xd = x1 if dx < 0 else x2
        zd = cz
        breed = 2 if soort in ("winkel", "cafe", "smid", "tempel") else 1
        zs = [zd] if breed == 1 else [zd - 1, zd]
        S.box(xd, SY, zs[0] - 1, xd, SY + 3, zs[-1] + 1, "minecraft:dark_oak_planks" if not tempel else "minecraft:polished_diorite")
        for i, z in enumerate(zs):
            hinge = "left" if i == 0 else "right"
            f = TEGEN[FACE[deur]]
            S.set(xd, SY, z, f"minecraft:spruce_door[facing={f},half=lower,hinge={hinge},open=false,powered=false]")
            S.set(xd, SY + 1, z, f"minecraft:spruce_door[facing={f},half=upper,hinge={hinge},open=false,powered=false]")
            S.set(xd + dx, FY, z, "guhs:knuffelklinkers")
            S.set(xd - dx, FY, z, "minecraft:spruce_planks" if not tempel else "minecraft:smooth_quartz")
        buiten = [xd + 2 * dx, SY, zd]
        runner = [(x, zd) for x in range(x1 + 1, x2)]
    if not tempel:
        for (x, z) in runner:
            S.set(x, SY, z, "minecraft:red_carpet" if soort != "cafe" else "minecraft:pink_carpet", keep=True)
    # lights: hanging lanterns from the ceiling
    for x in range(x1 + 2, x2 - 1, 4):
        for z in range(z1 + 2, z2 - 1, 4):
            S.set(x, top - 1, z, LANT_H)
    # roof: a thick snow cap over dark oak eaves (gable along the long side), icicles (end rods) under the eaves
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
                S.box(x1 - 1, y, u, x2 + 1, y, u, SNEEUW)
            else:
                S.box(u, y, z1 - 1, u, y, z2 + 1, SNEEUW)
        if langs_x:
            S.box(x1, y, lo + 1, x2, y, hi - 1, wand if not tempel else "minecraft:calcite")
            S.box(x1 + 1, y, lo + 1, x2 - 1, y, hi - 1, CARVE)
            S.box(x1 - 1, y + 1, lo, x2 + 1, y + 1, lo, laag(4), keep=True)
            S.box(x1 - 1, y + 1, hi, x2 + 1, y + 1, hi, laag(4), keep=True)
        else:
            S.box(lo + 1, y, z1, hi - 1, y, z2, wand if not tempel else "minecraft:calcite")
            S.box(lo + 1, y, z1 + 1, hi - 1, y, z2 - 1, CARVE)
            S.box(lo, y + 1, z1 - 1, lo, y + 1, z2 + 1, laag(4), keep=True)
            S.box(hi, y + 1, z1 - 1, hi, y + 1, z2 + 1, laag(4), keep=True)
    S.box(x1 + 1, top, z1 + 1, x2 - 1, top, z2 - 1, "minecraft:spruce_planks" if not tempel else "minecraft:calcite")
    if langs_x:
        for x in range(x1 - 1, x2 + 2):
            S.set(x, top, z1 - 1, trap("minecraft:dark_oak_stairs", "south"))
            S.set(x, top, z2 + 1, trap("minecraft:dark_oak_stairs", "north"))
            if x % 3 == 0:
                S.set(x, top - 1, z1 - 1, "minecraft:end_rod[facing=down]")
                S.set(x, top - 1, z2 + 1, "minecraft:end_rod[facing=down]")
    else:
        for z in range(z1 - 1, z2 + 2):
            S.set(x1 - 1, top, z, trap("minecraft:dark_oak_stairs", "east"))
            S.set(x2 + 1, top, z, trap("minecraft:dark_oak_stairs", "west"))
            if z % 3 == 0:
                S.set(x1 - 1, top - 1, z, "minecraft:end_rod[facing=down]")
                S.set(x2 + 1, top - 1, z, "minecraft:end_rod[facing=down]")
    # chimney with smoke (lit campfire on top, out of reach) + a fireplace glow inside
    if not tempel:
        cxh, czh = (x2 - 1, z1 + 1) if deur != "oost" else (x1 + 1, z1 + 1)
        if deur == "noord":
            cxh, czh = x2 - 1, z2 - 1
        S.box(cxh, SY, czh, cxh, top + half + 3, czh, "minecraft:bricks")
        S.set(cxh, top + half + 4, czh, ROOK)
        S.set(cxh, SY, czh, "minecraft:shroomlight")
    # furniture
    P_ = {"deur": buiten, "binnen": [cx, SY, cz], "doos": [[x1, FY, z1], [x2, top, z2]], "naam": naam}
    ox, oz = -dx, -dz            # towards the back
    bx, bz = cx + ox * (w // 2 - 2 if dx else 0), cz + oz * (d // 2 - 2 if dz else 0)
    if soort in ("winkel", "smid", "cafe"):
        # counter across the back third, the NPC behind it
        if dx:
            S.box(bx - ox, SY, z1 + 2, bx - ox, SY, z2 - 2, "minecraft:barrel[facing=up,open=false]")
            S.box(bx - ox, SY, cz, bx - ox, SY, cz, CARVE) if False else None
            S.box(bx - ox, SY + 1, z1 + 2, bx - ox, SY + 1, z2 - 2, plaat("minecraft:spruce_slab"))
            S.set(bx - ox, SY, z1 + 1, "minecraft:spruce_trapdoor[facing=east,half=bottom,open=true,powered=false,waterlogged=false]")
            S.box(bx + ox, SY, z1 + 1, bx + ox, SY + 2, z2 - 1, "minecraft:barrel[facing=up,open=false]" if soort != "smid" else "minecraft:stone_bricks")
        else:
            S.box(x1 + 2, SY, bz - oz, x2 - 2, SY, bz - oz, "minecraft:barrel[facing=up,open=false]")
            S.box(x1 + 2, SY + 1, bz - oz, x2 - 2, SY + 1, bz - oz, plaat("minecraft:spruce_slab"))
            S.box(x1 + 1, SY, bz + oz, x2 - 1, SY + 2, bz + oz, "minecraft:barrel[facing=up,open=false]")
        P_["npc"] = [bx, SY, bz]
        P_["toonbank"] = [bx - 2 * ox, SY, bz - 2 * oz]
        if soort == "smid":
            S.set(bx - 2 * ox + (1 if dz else 0), SY, bz - 2 * oz + (1 if dx else 0), "minecraft:anvil[facing=north]")
            S.set(x1 + 1, SY, z2 - 1, "minecraft:blast_furnace[facing=north,lit=true]")
            S.set(x2 - 1, SY, z2 - 1, "minecraft:smithing_table")
            S.set(x2 - 1, SY, z1 + 1, "minecraft:grindstone[face=floor,facing=north]")
        if soort == "cafe":
            # tables + chairs inside, a chocolate cauldron, cake on the counter
            for (tx, tz) in ((cx - 3, cz - 3), (cx - 3, cz + 3), (cx + 1, cz - 3), (cx + 1, cz + 3)):
                if x1 + 1 < tx < x2 - 1 and z1 + 1 < tz < z2 - 1:
                    S.set(tx, SY, tz, STAAF)
                    S.set(tx, SY + 1, tz, "minecraft:spruce_pressure_plate[powered=false]")
                    S.set(tx + 1, SY, tz, trap("minecraft:spruce_stairs", "east"))
                    S.set(tx - 1, SY, tz, trap("minecraft:spruce_stairs", "west"))
            S.set(bx, SY, z1 + 1, "minecraft:water_cauldron[level=3]")
            S.set(bx - ox, SY + 2, cz + 1, "minecraft:cake[bites=0]")
            P_["geheim_kelder"] = None
    elif soort == "huis":
        bedx, bedz = (x1 + 1, z1 + 1) if deur != "noord" else (x1 + 1, z2 - 2)
        S.set(bedx, SY, bedz, "minecraft:red_bed[facing=south,occupied=false,part=head]")
        S.set(bedx, SY, bedz + 1, "minecraft:red_bed[facing=south,occupied=false,part=foot]")
        S.set(x1 + 1, SY, z2 - 1 if deur != "noord" else z1 + 1, "minecraft:barrel[facing=up,open=false]")
        S.set(cx + 1, SY, cz, STAAF)
        S.set(cx + 1, SY + 1, cz, "minecraft:spruce_pressure_plate[powered=false]")
        S.set(cx + 2, SY, cz, trap("minecraft:spruce_stairs", "east"))
        S.set(x2 - 1, SY, z2 - 1 if deur != "zuid" else z1 + 1, "minecraft:potted_red_tulip")
        S.box(x1 + 1, SY, z1 + 1, x2 - 1, SY, z2 - 1, "minecraft:white_carpet", keep=True)
    elif tempel:
        # the class altar at the back, crystal pillars, free circle in front of it
        ax, az = cx, z1 + 2
        S.box(ax - 1, SY, az, ax + 1, SY, az, "minecraft:polished_diorite")
        S.set(ax, SY + 1, az, "minecraft:amethyst_block")
        S.set(ax, SY + 2, az, "guhs:guh_kristal_cluster[facing=up]")
        S.set(ax - 1, SY + 1, az, "minecraft:end_rod[facing=up]")
        S.set(ax + 1, SY + 1, az, "minecraft:end_rod[facing=up]")
        for x in (x1 + 2, x2 - 2):
            for z in range(z1 + 3, z2 - 1, 4):
                S.box(x, SY, z, x, top - 1, z, "guhs:guh_kristal_blok")
        S.box(x1 + 1, top, z1 + 1, x2 - 1, top, z2 - 1, "minecraft:calcite")
        for x in range(x1 + 3, x2 - 2, 4):
            S.set(x, top, cz, "guhs:guh_kristal_lamp")
        P_["npc"] = [cx, SY, az + 2]
        P_["altaar"] = [ax, SY, az]
        # crystal spire on the roof
        S.kegel(cx, cz, top + half + 1, 14, 2.6, 0.4, "minecraft:pink_stained_glass")
        S.kegel(cx, cz, top + half + 1, 10, 1.2, 0.2, "guhs:guh_kristal_blok")
    return P_


def lantaarnpaal(S, x, y, z):
    S.box(x, y, z, x, y + 2, z, STAAF)
    S.set(x, y + 3, z, LANT)


def stad(W, K, P):
    S = Vol(-140, FY - 6, -2540, 140, FY + 62, -2250)
    sx, sz = PL.STAD
    gb = {}
    # ---- streets (knuffelklinkers) and the plaza with the skating pond
    weg = "guhs:knuffelklinkers"
    S.box(-3, FY, sz + 26, 3, FY, -2251, weg)               # south: plaza -> dock road
    S.box(-3, FY, -2440, 3, FY, sz - 26, weg)               # north stub
    S.box(-24, FY, -2442, 24, FY, -2438, weg)               # cross street in front of the tree
    for xs in (-24, 20):
        S.box(xs, FY, -2524, xs + 4, FY, -2438, weg)        # around the kapel
    S.box(-24, FY, -2524, 24, FY, -2520, weg)
    S.box(-3, FY, -2540, 3, FY, -2520, weg)                  # north gate
    S.box(26, FY, sz - 3, 140, FY, sz + 3, weg)              # east gate
    S.box(-140, FY, sz - 3, -26, FY, sz + 3, weg)            # west gate
    S.box(-82, FY, -2444, -26, FY, -2440, weg)               # west quarter street
    S.box(26, FY, -2444, 86, FY, -2440, weg)
    S.box(-82, FY, -2348, -26, FY, -2344, weg)
    S.box(26, FY, -2348, 70, FY, -2344, weg)
    S.box(-58, FY, -2444, -54, FY, -2290, weg)
    S.box(54, FY, -2444, 58, FY, -2296, weg)
    S.box(-54, FY, -2302, 54, FY, -2298, weg)
    S.box(70, FY, -2480, 74, FY, -2444, weg)                 # to the gondola
    S.cyl(sx, sz, PL.PLEIN_R, FY, FY, "parelmoer_tegels")
    S.cyl(sx, sz, PL.PLEIN_R, FY, FY, "minecraft:polished_diorite", hollow=1.2)
    S.cyl(sx, sz, PL.VIJVER_R + 1.5, FY, FY, SNEEUW)
    S.cyl(sx, sz, PL.VIJVER_R, FY, FY, "minecraft:packed_ice")
    S.cyl(sx, sz, PL.VIJVER_R + 1.5, SY, SY, laag(2), hollow=1.4)
    for a in range(0, 360, 90):
        r = PL.VIJVER_R + 5
        x, z = round(sx + r * math.cos(math.radians(a + 45))), round(sz + r * math.sin(math.radians(a + 45)))
        f = "east" if math.cos(math.radians(a + 45)) > 0 else "west"
        S.set(x, SY, z, trap("minecraft:spruce_stairs", f))
        S.set(x, SY, z + 1, trap("minecraft:spruce_stairs", f))
    for a in range(22, 360, 45):
        r = PL.PLEIN_R - 2
        lantaarnpaal(S, round(sx + r * math.cos(math.radians(a))), SY, round(sz + r * math.sin(math.radians(a))))
    # snow-guh figures on the plaza edge
    for (x, z, f) in ((-18, -2385, "south"), (18, -2385, "south")):
        S.set(x, SY, z, f"guhs:sneeuwpopguh[facing={f},half=lower]")
        S.set(x, SY + 1, z, f"guhs:sneeuwpopguh[facing={f},half=upper]")
    # ---- the big snow spruce (north of the plaza, between the cross street and the kapel)
    tx, tz = PL.SNEEUWGUHSPAR[0], -2448
    S.box(tx, SY, tz, tx, SY + 22, tz, "minecraft:spruce_log[axis=y]")
    for k in range(20):
        r = max(0.8, 6.5 * (1 - k / 21))
        y = SY + 3 + k
        S.cyl(tx, tz, r, y, y, "minecraft:spruce_leaves", keep=True)
        if k % 3 == 0:
            S.cyl(tx, tz, r + 0.1, y + 1, y + 1, laag(1), keep=True, hollow=1)
    for k in range(18):
        a = k * 2.4
        r = max(1.0, 6.0 * (1 - (k * 1.1) / 21))
        S.set(round(tx + r * math.cos(a)), SY + 3 + int(k * 1.1), round(tz + r * math.sin(a)), "minecraft:shroomlight")
    S.set(tx, SY + 23, tz, "minecraft:pink_stained_glass")
    S.set(tx, SY + 24, tz, "guhs:guh_kristal_cluster[facing=up]")
    S.cyl(tx, tz, 7.5, FY, FY, SNEEUW)
    # ---- buildings
    for gid, (cx, cz, w, d, deur, soort, naam) in PL.GEBOUWEN.items():
        gb[gid] = huis(S, P, gid, cx, cz, w, d, deur, soort, naam)
    # cafe terrace (west of the cafe) + the hidden chocolate cellar under it
    cx, cz = PL.GEBOUWEN["cafe"][0], PL.GEBOUWEN["cafe"][1]
    for (tx_, tz_) in ((37, -2338), (37, -2351)):
        S.set(tx_, SY, tz_, STAAF)
        S.set(tx_, SY + 1, tz_, STAAF)
        S.set(tx_, SY + 2, tz_, "minecraft:pink_wool")
        S.box(tx_ - 1, SY + 2, tz_ - 1, tx_ + 1, SY + 2, tz_ + 1, "minecraft:pink_carpet", keep=True)
        S.set(tx_ + 1, SY, tz_, trap("minecraft:spruce_stairs", "east"))
        S.set(tx_ - 1, SY, tz_, trap("minecraft:spruce_stairs", "west"))
    kx1, kz1, kx2, kz2 = cx - 4, cz - 4, cx + 8, cz + 5
    S.box(kx1 - 1, FY - 7, kz1 - 1, kx2 + 1, FY - 1, kz2 + 1, "minecraft:bricks")
    S.box(kx1, FY - 5, kz1, kx2, FY - 2, kz2, CARVE)
    S.box(kx1, FY - 6, kz1, kx2, FY - 6, kz2, "minecraft:spruce_planks")
    S.box(kx1 + 1, FY - 5, kz1 + 1, kx2 - 3, FY - 5, kz2 - 1, "minecraft:brown_carpet")
    hx, hz = cx + 7, cz + 4
    S.box(hx, FY - 5, hz + 1, hx, FY, hz + 1, "minecraft:spruce_planks")
    for y in range(FY - 5, FY + 1):
        S.set(hx, y, hz, "minecraft:ladder[facing=north,waterlogged=false]")
    S.set(kx1, FY - 5, kz1, "minecraft:water_cauldron[level=3]")
    S.set(kx1 + 1, FY - 5, kz1, "minecraft:water_cauldron[level=3]")
    S.set(kx2, FY - 5, kz1, "minecraft:barrel[facing=up,open=false]")
    S.set(kx1, FY - 5, kz2, "minecraft:barrel[facing=up,open=false]")
    S.set(cx, FY - 2, cz, LANT_H)
    S.set(cx + 4, FY - 2, cz, LANT_H)
    P["geheim_chocokelder"] = [cx, FY - 5, cz]
    W.lo_extra_set = getattr(W, "lo_extra_set", [])
    # ---- the stable + paddock
    sx_, sz_ = PL.STAL
    S.box(sx_ - 5, FY, sz_ - 4, sx_ + 5, FY, sz_ + 4, "minecraft:spruce_planks")
    for (x, z) in ((sx_ - 5, sz_ - 4), (sx_ + 5, sz_ - 4), (sx_ - 5, sz_ + 4), (sx_ + 5, sz_ + 4), (sx_, sz_ - 4), (sx_, sz_ + 4)):
        S.box(x, SY, z, x, SY + 4, z, "minecraft:spruce_log[axis=y]")
    for k in range(5):
        S.box(sx_ - 6, SY + 5 + k, sz_ - 5 + k, sx_ + 6, SY + 5 + k, sz_ - 5 + k, SNEEUW)
        S.box(sx_ - 6, SY + 5 + k, sz_ + 5 - k, sx_ + 6, SY + 5 + k, sz_ + 5 - k, SNEEUW)
    S.box(sx_ - 5, SY + 5, sz_ - 4, sx_ + 5, SY + 5, sz_ + 4, "minecraft:spruce_planks")
    S.box(sx_ - 6, SY + 10, sz_, sx_ + 6, SY + 10, sz_, SNEEUW)
    for (x, z) in ((sx_ - 4, sz_ - 3), (sx_ - 3, sz_ - 3), (sx_ - 4, sz_ + 3)):
        S.set(x, SY, z, "minecraft:hay_block[axis=y]")
    S.set(sx_ + 3, SY, sz_ - 3, "minecraft:composter[level=0]")
    S.set(sx_, SY + 4, sz_, LANT_H)
    px0, pz0, px1, pz1 = sx_ - 8, sz_ + 6, sx_ + 8, sz_ + 16
    for x in range(px0, px1 + 1):
        for z in (pz0, pz1):
            S.set(x, SY, z, HEK_X)
    for z in range(pz0, pz1 + 1):
        for x in (px0, px1):
            S.set(x, SY, z, HEK_Z)
    S.set(px1, SY, (pz0 + pz1) // 2, "minecraft:spruce_fence_gate[facing=east,in_wall=false,open=false,powered=false]")
    S.box(px0 + 1, FY, pz0 + 1, px1 - 1, FY, pz1 - 1, "minecraft:grass_block[snowy=true]")
    gb["stal"] = {"deur": [sx_ + 7, SY, sz_], "binnen": [sx_, SY, sz_], "doos": [[sx_ - 5, FY, sz_ - 4], [sx_ + 5, SY + 10, sz_ + 4]],
                  "naam": "De Warme Stal", "npc": [sx_ + 6, SY, sz_ - 1], "wei": [sx_, SY, (pz0 + pz1) // 2]}
    # ---- quest board
    qx, qz = PL.QUESTBORD
    S.box(qx - 2, SY, qz, qx - 2, SY + 2, qz, "minecraft:spruce_log[axis=y]")
    S.box(qx + 2, SY, qz, qx + 2, SY + 2, qz, "minecraft:spruce_log[axis=y]")
    S.box(qx - 1, SY + 1, qz, qx + 1, SY + 2, qz, "minecraft:birch_planks")
    S.box(qx - 2, SY + 3, qz, qx + 2, SY + 3, qz, plaat("minecraft:dark_oak_slab"))
    S.set(qx, SY + 2, qz + 1, "minecraft:lantern[hanging=false,waterlogged=false]", keep=True) if False else None
    # ---- street lamps
    for z in range(-2372, -2252, 12):
        lantaarnpaal(S, 5, SY, z)
    for x in range(34, 130, 14):
        lantaarnpaal(S, x, SY, sz + 5)
        lantaarnpaal(S, -x, SY, sz + 5)
    # ---- the geode crystals: big pink shards leaning out of the inner wall, clusters at its foot
    rng = random.Random(4)
    R0 = PL.GEODE_R
    gates = [math.atan2(dz, dx) for (dx, dz) in PL.POORTEN.values()]
    for k in range(0, 360, 7):
        a = math.radians(k + rng.uniform(-2, 2))
        if any(abs(math.atan2(math.sin(a - g), math.cos(a - g))) < 0.2 for g in gates):
            continue
        r = R0 + 2.5
        bx, bz = sx + r * math.cos(a), sz + r * math.sin(a)
        y0 = SY + rng.randint(6, 30)
        h = rng.randint(9, 22)
        inw = -0.55 * h
        sch = (inw * math.cos(a), inw * math.sin(a))
        r0 = rng.uniform(1.6, 3.2)
        S.kegel(bx, bz, y0, h, r0, 0.3, "minecraft:pink_stained_glass", scheef=sch)
        S.kegel(bx, bz, y0, int(h * 0.8), r0 * 0.5, 0.2, "guhs:guh_kristal_blok", scheef=(sch[0] * 0.8, sch[1] * 0.8))
        if rng.random() < 0.6:
            rr = R0 - 0.5
            S.set(round(sx + rr * math.cos(a)), SY, round(sz + rr * math.sin(a)), "minecraft:amethyst_cluster[facing=up,waterlogged=false]", keep=True)
    # ---- the gondola valley station
    gondel_station(S, P, PL.GONDEL_DAL[0], PL.GONDEL_DAL[1], FY, "dal")
    # ---- NPC spots
    P["plein"] = [0, SY, -2412]
    P["schaatsvijver"] = [0, SY, -2400]
    P["questbord"] = [qx, SY, qz + 1]
    P["sneeuwguhspar"] = [tx, SY, tz + 8]
    P["npc_schaatsguh"] = [13, SY, -2400]
    P["npc_burgemeester"] = [0, SY, -2418]
    P["npc_stal"] = gb["stal"]["npc"]
    for gid, g in gb.items():
        if g.get("npc") and gid != "stal":
            P["npc_" + gid] = g["npc"]
    P["kristalkapel_altaar"] = gb["kristalkapel"]["altaar"]
    P["cafe_terras"] = [38, SY, -2344]
    P["gebouw"] = {k: {kk: vv for kk, vv in v.items() if kk not in ("naam",) and vv is not None} for k, v in gb.items()}
    W.stempel(S.stempel("kandijkom"))


def gondel_station(S, P, cx, cz, g, naam):
    """A gondola station: a platform with a roof on posts, the big wheel up top, a waiting cabin and the bell."""
    S.box(cx - 5, g, cz - 5, cx + 5, g, cz + 5, "minecraft:stone_bricks")
    S.box(cx - 5, g - 6, cz - 5, cx + 5, g - 1, cz + 5, "minecraft:stone_bricks")
    S.box(cx - 4, g, cz - 4, cx + 4, g, cz + 4, "minecraft:spruce_planks")
    for (x, z) in ((cx - 5, cz - 5), (cx + 5, cz - 5), (cx - 5, cz + 5), (cx + 5, cz + 5)):
        S.box(x, g + 1, z, x, g + 9, z, "minecraft:dark_oak_log[axis=y]")
    S.box(cx - 6, g + 10, cz - 6, cx + 6, g + 10, cz + 6, "minecraft:dark_oak_planks")
    S.box(cx - 6, g + 11, cz - 6, cx + 6, g + 11, cz + 6, SNEEUW)
    S.box(cx - 5, g + 12, cz - 5, cx + 5, g + 12, cz + 5, laag(3))
    # the bull-wheel (ring) under the roof
    for k in range(24):
        a = k * math.pi / 12
        S.set(round(cx + 3.5 * math.cos(a)), g + 9, round(cz + 3.5 * math.sin(a)), "minecraft:stripped_dark_oak_log[axis=y]")
    S.box(cx, g + 9, cz, cx, g + 9, cz, "minecraft:iron_block")
    # waiting cabin hanging at the platform edge (decoration)
    kx, kz = cx, cz - 2
    S.box(kx - 2, g + 2, kz - 1, kx + 2, g + 5, kz + 1, "minecraft:red_wool")
    S.box(kx - 1, g + 3, kz - 1, kx + 1, g + 4, kz - 1, "minecraft:glass")
    S.box(kx - 1, g + 3, kz + 1, kx + 1, g + 4, kz + 1, "minecraft:glass")
    S.box(kx - 2, g + 6, kz - 1, kx + 2, g + 6, kz + 1, SNEEUW)
    S.box(kx, g + 7, kz, kx, g + 8, kz, "minecraft:iron_chain[axis=y]")
    S.set(cx + 3, g + 1, cz + 3, "minecraft:bell[attachment=floor,facing=north,powered=false]")
    S.set(cx - 3, g + 1, cz + 3, LANT)
    P[f"gondel_{naam}"] = [cx, g + 1, cz + 2]
    P[f"gondel_{naam}_knop"] = [cx + 3, g + 1, cz + 3]
    P[f"gondel_{naam}_kabel"] = [cx, g + 9, cz]
    if naam == "dal":
        P["npc_gondel"] = [cx - 2, g + 1, cz + 3]


def gondel(W, K, P):
    (ax, az), (bx, bz) = PL.GONDEL_DAL, PL.GONDEL_BERG
    ya, yb = FY + 9, PL.GONDEL_BERG_TOP + 9
    S = Vol(min(ax, bx) - 12, min(ya, yb) - 80, min(az, bz) - 12, max(ax, bx) + 12, max(ya, yb) + 16, max(az, bz) + 12)
    gondel_station(S, P, bx, bz, PL.GONDEL_BERG_TOP, "berg")
    L = math.dist((ax, az), (bx, bz))
    ux, uz = (bx - ax) / L, (bz - az) / L
    px, pz = -uz, ux
    for kant in (-1.5, 1.5):
        a = (ax + px * kant, ya, az + pz * kant)
        b = (bx + px * kant, yb, bz + pz * kant)
        S.lijn(a, b, "minecraft:iron_chain[axis=x,waterlogged=false]")
    # pylons (lattice towers) at 1/3 and 2/3
    for t in (0.33, 0.66):
        x, z = ax + (bx - ax) * t, az + (bz - az) * t
        yk = ya + (yb - ya) * t
        g = hoogste(W, round(x), round(z))
        for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            S.box(round(x) + dx, g + 1, round(z) + dz, round(x) + dx, round(yk) - 1, round(z) + dz, "minecraft:iron_bars[east=false,north=false,south=false,waterlogged=false,west=false]")
        S.box(round(x) - 3, round(yk) - 1, round(z) - 3, round(x) + 3, round(yk) - 1, round(z) + 3, "minecraft:stripped_dark_oak_log[axis=x]")
        S.box(round(x) - 1, g, round(z) - 1, round(x) + 1, g, round(z) + 1, "minecraft:stone_bricks")
    # two cabins on the cable (one going up, one coming down)
    for t, kant in ((0.42, -1.5), (0.6, 1.5)):
        x, z = ax + (bx - ax) * t + px * kant, az + (bz - az) * t + pz * kant
        yk = round(ya + (yb - ya) * t)
        x, z = round(x), round(z)
        S.box(x, yk - 2, z, x, yk - 1, z, "minecraft:iron_chain[axis=y]")
        S.box(x - 2, yk - 6, z - 1, x + 2, yk - 3, z + 1, "minecraft:red_wool")
        S.box(x - 1, yk - 5, z - 1, x + 1, yk - 4, z + 1, "minecraft:glass")
        S.box(x - 2, yk - 3, z - 1, x + 2, yk - 3, z + 1, SNEEUW)
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
    W.stempel(S.stempel("dok"))


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
    P[f"uitkijk_{naam}"] = [x, top + 2, z]
    W.stempel(S.stempel("naald_" + naam))


def kristalnaald(S, x, z, g, h, r0, rng, roze=False):
    """A crystal needle: an icy foot, a blue-ice body and a glassy tip."""
    sch = (rng.uniform(-0.15, 0.15) * h, rng.uniform(-0.15, 0.15) * h)
    S.kegel(x, z, g - 2, int(h * 0.35) + 2, r0 + 0.6, r0 * 0.8, "minecraft:packed_ice", scheef=(sch[0] * 0.35, sch[1] * 0.35))
    S.kegel(x, z, g, int(h * 0.85), r0 * 0.85, r0 * 0.25, "minecraft:blue_ice", scheef=(sch[0] * 0.85, sch[1] * 0.85))
    S.kegel(x, z, g + int(h * 0.3), int(h * 0.7), r0 * 0.6, 0.2,
            "minecraft:pink_stained_glass" if roze else "minecraft:light_blue_stained_glass", scheef=(sch[0] * 0.7, sch[1] * 0.7))
    tx, tz = x + sch[0], z + sch[1]
    S.set(round(tx), g + h, round(tz), "minecraft:amethyst_block" if not roze else "guhs:guh_kristal_blok")


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
        kristalnaald(S, x, z, gg, rng.randint(10, 22), rng.uniform(1.6, 2.8), rng)
    # the Galmwolf's needle (north rim): tall, with a ledge on top
    nx, nz = cx, cz - r - 8
    gg = max(K.h(nx, nz), g)
    kristalnaald(S, nx, nz, gg, 34, 4.5, rng)
    P["galmwolf_naald"] = [nx, gg + 30, nz]
    P["arena_galmwolf"] = [cx, g + 1, cz]
    P["arena_galmwolf_deur"] = [cx - r - 6, g + 1, cz]
    P["arena_galmwolf_bord"] = [cx - r - 5, g + 1, cz + 4]
    W.stempel(S.stempel("galm_arena"))
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
            kristalnaald(S, x, z, K.h(x, z), rng.randint(10, 34), rng.uniform(1.5, 3.8), rng, roze=rng.random() < 0.12)
            n -= 1
        P[f"galm_naalden_{i + 1}"] = [fx, K.h(fx, fz) + 1, fz]
        W.stempel(S.stempel(f"naaldveld_{i}"))
    # loose needles all over the Galmhellingen
    S = Vol(150, 60, -2700, 600, 280, -1800)
    for _ in range(260):
        x, z = rng.randint(170, 590), rng.randint(-2690, -1810)
        if not vrij(K, x, z, 5) or op_pad(K, x, z) or math.hypot(x - cx, z - cz) < r + 14 or K.h(x, z) < 96:
            continue
        kristalnaald(S, x, z, K.h(x, z), rng.randint(7, 24), rng.uniform(1.2, 3.0), rng, roze=rng.random() < 0.1)
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
        P[f"galm_lawine_{i + 1}_voet"] = [ex, K.h(ex, ez) + 1, ez]
        W.stempel(S.stempel(f"lawine_{i}"), bezet=False)
    grotje(W, K, P, "geheim_galmgrot", PL.GALMGROT[0], PL.GALMGROT[1], "galm")


def grotje(W, K, P, naam, x, z, soort):
    """A small secret cave: an icy dome on the ground, the entrance behind a frozen waterfall (with a 1-wide gap)."""
    g = K.h(x, z)
    S = Vol(x - 12, g - 6, z - 12, x + 12, g + 16, z + 12)
    S.cyl(x, z, 9, g - 5, g, "minecraft:stone")
    S.ell(x, g + 1, z, 9, 8, 9, "minecraft:packed_ice" if soort == "galm" else SNEEUW, ymin=g + 1)
    S.ell(x, g + 1, z, 9.3, 8.3, 9.3, "minecraft:snow_block", hollow=0.6, ymin=g + 5)
    S.ell(x, g + 1, z, 6.5, 5.5, 6.5, CARVE, ymin=g + 1)
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
    P[naam] = [x, g + 1, z]
    P[naam + "_ingang"] = [x - 11, g + 1, z]
    W.stempel(S.stempel(naam))


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
    for k in range(7):
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
    for gx in range(-sr - 4, sr + 5, 6):
        for gz in range(-sr - 4, sr + 5, 6):
            x, z = sx + gx + rng.randint(-1, 1), sz + gz + rng.randint(-1, 1)
            i, j = K2.idx(x, z)
            if K.open_water[i, j]:
                S.cyl(x, z, rng.uniform(1.4, 2.6), ijs, ijs, "minecraft:packed_ice")
                if rng.random() < 0.4:
                    S.set(x, ijs + 1, z, laag(rng.randint(1, 3)))
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


def brug(W, K, P, i, bx, breed=1, dek="minecraft:spruce_slab", hout="minecraft:spruce"):
    zs, hs = kloofrand(K, bx, 1)
    zn, hn = kloofrand(K, bx, -1)
    for dx in range(-breed - 1, breed + 2):
        hs = max(hs, int(K.H[K2.idx(bx + dx, zs)]))
        hn = max(hn, int(K.H[K2.idx(bx + dx, zn)]))
    za, zb = zs + 2, zn - 2          # deck from 2 blocks onto the land on both sides
    S = Vol(bx - breed - 6, min(hs, hn) - 12, zb - 4, bx + breed + 6, max(hs, hn) + 12, za + 4)
    sa, sb = 2 * (hs + 1) + 1, 2 * (hn + 1) + 1
    n = za - zb
    for k, z in enumerate(range(za, zb - 1, -1)):
        t = k / max(1, n)
        s = sa + (sb - sa) * t - 4 * math.sin(math.pi * t)
        s = int(round(s))
        y, top = (s - 1) // 2, (s - 1) % 2 == 1
        typ = "top" if top else "bottom"
        S.box(bx - breed, y, z, bx + breed, y, z, f"{dek}[type={typ},waterlogged=false]")
        S.box(bx - breed, y + 1, z, bx + breed, y + 4, z, CARVE)
        for kant in (-1, 1):
            S.set(bx + kant * (breed + 1), y + 1, z, HEK_Z if hout == "minecraft:spruce" else "minecraft:dark_oak_fence[east=false,north=true,south=true,waterlogged=false,west=false]")
            S.set(bx + kant * (breed + 1), y, z, f"{hout}_planks")
        if k % 6 == 0:
            for kant in (-1, 1):
                S.set(bx + kant * (breed + 1), y - 1, z, "minecraft:iron_chain[axis=y]")
    for (z, h) in ((za + 1, hs), (zb - 1, hn)):
        for kant in (-1, 1):
            S.box(bx + kant * (breed + 2), h + 1, z, bx + kant * (breed + 2), h + 5, z, f"{hout}_log[axis=y]")
            S.set(bx + kant * (breed + 2), h + 6, z, LANT)
        S.box(bx - breed, h + 1, z, bx + breed, h + 4, z, CARVE)
    P[f"brug_{i + 1}_zuid"] = [bx, hs + 1, za + 2]
    P[f"brug_{i + 1}_noord"] = [bx, hn + 1, zb - 2]
    W.stempel(S.stempel(f"brug_{i}"))


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
        if bx == 0:
            brug(W, K, P, i, bx, breed=2, dek="minecraft:dark_oak_slab", hout="minecraft:dark_oak")
        else:
            brug(W, K, P, i, bx)
    for i, (x, z) in enumerate(PL.BOORTORENS):
        boortoren(W, K, P, i, x, z)
    for i, x0 in enumerate(PL.KLOOF_TRAPPEN):
        P[f"kloof_trap_{i + 1}"] = [x0 + 1, PL.KLOOF_BODEM + 2, int(round(float(K2.kloof_z(x0)))) + 12]
    # grey crystal outcrops and boulders on the ravine floor
    S = Vol(-700, 55, -2860, 700, 110, -2700)
    for _ in range(140):
        x = rng.randint(-600, 600)
        zc = float(K2.kloof_z(x))
        z = round(zc + rng.uniform(-14, 14))
        i, j = K2.idx(x, z)
        if not K.kloof[i, j] or K.kloof_ramp[i, j] or abs(z - zc) < 4:
            continue
        g = int(K.H[i, j])
        if rng.random() < 0.5:
            S.kegel(x, z, g, rng.randint(3, 9), rng.uniform(0.8, 1.8), 0.2, rng.choice(["minecraft:light_gray_stained_glass",
                                                                                      "minecraft:calcite", "minecraft:tinted_glass"]),
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
def spiegelburcht(W, K, P):
    T = PL.BURCHT_TOP
    Y = T + 1
    S = Vol(-62, 128, -2998, 62, 246, -2800)
    # ---- the grand stair from the bridge head up to the forecourt
    zn, hn = kloofrand(K, 0, -1)
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
    # forecourt
    S.box(-12, T - 6, z - 4, 12, T, z, "minecraft:polished_tuff")
    S.box(-12, Y, z - 4, 12, Y + 10, z, CARVE)
    for x in range(-12, 13, 2):
        S.set(x, T, z - 2, "minecraft:smooth_quartz")
    vz = z - 2
    P["burcht_trap_voet"] = [0, PL.BRUG_TOP + 1, zn - 4]
    P["burcht_voorplein"] = [0, Y, vz]
    # ---- the hull of the front rooms (solid calcite, rooms carved out), facade 4 thick
    D = PL.BURCHT_DEUR_Z
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
    for k in range(6):
        a = k * math.pi / 3 + math.pi / 6
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
    P["burcht_splinter"] = [sx, Y + 1, sz + 2]
    P["arena_spiegelzucht"] = [hx_, Y, hz_]
    P["arena_spiegelzucht_straal"] = [hr, 0, 0]
    P["arena_spiegelzucht_deur"] = [0, Y, -2937]
    P["arena_spiegelzucht_bord"] = [4, Y, -2936]
    W.stempel(S.stempel("spiegelburcht"))


# ================================================================================================ the edge: cloud sea, landmarks
def rand(W, K, P):
    rng = random.Random(8)
    pts = []
    for x in range(PL.X0 + 12, PL.X0 + PL.NX - 12, 44):
        pts += [(x, PL.Z0 + 16), (x, PL.Z0 + PL.NZ - 16)]
    for z in range(PL.Z0 + 12, PL.Z0 + PL.NZ - 12, 44):
        pts += [(PL.X0 + 16, z), (PL.X0 + PL.NX - 16, z)]
    for (x, z) in pts:
        y = rng.randint(64, 92)
        S = Vol(x - 24, y - 8, z - 24, x + 24, y + 10, z + 24)
        for _ in range(4):
            S.ell(x + rng.uniform(-10, 10), y + rng.uniform(-2, 3), z + rng.uniform(-10, 10), rng.uniform(7, 14),
                  rng.uniform(3, 6), rng.uniform(7, 14), rng.choice(["wolkenblok_wit", "wolkenblok_wit", "minecraft:white_wool"]))
        W.stempel(S.stempel("wolk"), bezet=False)
    # ice floes on the sea
    S = Vol(PL.X0, PL.ZEE - 1, PL.Z0, PL.X0 + PL.NX - 1, PL.ZEE + 2, PL.Z0 + PL.NZ - 1)
    for _ in range(260):
        x, z = rng.randint(PL.X0 + 4, PL.X0 + PL.NX - 5), rng.randint(PL.Z0 + 4, PL.Z0 + PL.NZ - 5)
        if K.H[K2.idx(x, z)] >= 0:
            continue
        S.cyl(x, z, rng.uniform(1.5, 4.5), PL.ZEE, PL.ZEE, "minecraft:packed_ice")
    W.stempel(S.stempel("schotsen"), bezet=False)
    # far crystal spires rising out of the sea (landmarks)
    for i, (x, z, h) in enumerate(((-668, -1790, 96), (664, -2940, 120), (-662, -2980, 84), (670, -1850, 70))):
        S = Vol(x - 30, 40, z - 30, x + 30, 62 + h + 4, z + 30)
        S.kegel(x, z, 50, h + 12, 10, 0.5, "minecraft:light_blue_stained_glass")
        S.kegel(x, z, 50, h, 5, 0.3, "minecraft:blue_ice")
        for k in range(5):
            a = k * 2 * math.pi / 5 + i
            S.kegel(x + 11 * math.cos(a), z + 11 * math.sin(a), 50, h // 3 + k * 4, 4, 0.3, "minecraft:white_stained_glass",
                    scheef=(6 * math.cos(a), 6 * math.sin(a)))
        P[f"landmark_spits_{i + 1}"] = [x, 62 + h, z]
        W.stempel(S.stempel(f"spits_{i}"), bezet=False)


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
    kans = np.clip(0.003 + 0.012 * K.n2, 0, 0.018) * np.where(K2.ZZ < -2690, 0.35, 1.0)
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
    m2 = (K2.XX > 160) & ~W.bezet & ~W.steil & (K.pad_m < 0.05) & (rng.random(H.shape) < 0.004) & (H > PL.ZEE + 3) & ~K.kabel
    for i, j in zip(*np.nonzero(m2)):
        W.punten.set(int(i) + PL.X0, int(H[i, j]) + 1, int(j) + PL.Z0, "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
    m3 = (W.top == i_gras) & ~W.bezet & (K.pad_m < 0.05) & (rng.random(H.shape) < 0.01)
    for i, j in zip(*np.nonzero(m3)):
        W.punten.set(int(i) + PL.X0, int(H[i, j]) + 1, int(j) + PL.Z0, "guhs:guh_ijsbloempje")
    P["aantal_bomen"] = [int(len(ii)), 0, 0]


# ================================================================================================ mob zones (boxes) + all
MOBZONES = {
    "mob_galm_voet": ((160, -2360), (360, -1850), (20, 22)),
    "mob_galm_naalden": ((360, -2200), (590, -1850), (22, 24)),
    "mob_galm_top": ((200, -2690), (590, -2370), (23, 25)),
    "mob_meer_oever": ((-590, -2420), (-160, -1850), (25, 27)),
    "mob_meer_ijs": ((-520, -2400), (-240, -2060), (27, 29)),
    "mob_meer_noord": ((-590, -2690), (-160, -2430), (28, 30)),
    "mob_kloof_zuid": ((-590, -2750), (590, -2700), (30, 31)),
    "mob_kloof_bodem": ((-590, -2810), (590, -2745), (31, 33)),
    "mob_kloof_noord": ((-590, -2990), (590, -2810), (32, 34)),
    "mob_burcht": ((-19, -2984), (19, -2856), (33, 35)),
}


def bouw_alles(W, K):
    P = {}
    stad(W, K, P)
    dok(W, K, P)
    gondel(W, K, P)
    galmhellingen(W, K, P)
    knuffelmeer(W, K, P)
    splinterkloof(W, K, P)
    spiegelburcht(W, K, P)
    mika_fort(W, K, P)
    for naam, (x, z) in PL.UITKIJK.items():
        ijsnaald(W, K, P, naam, x, z, {"galmtop": 46, "meerkijk": 52, "kloofkijk": 48}[naam])
    kampvuren(W, K, P)
    rand(W, K, P)
    decoratie(W, K, P)
    P["mobzones"] = {k: {"min": [a[0], 0, a[1]], "max": [b[0], 0, b[1]], "level": [lv[0], lv[1], 0]} for k, (a, b, lv) in MOBZONES.items()}
    return P
