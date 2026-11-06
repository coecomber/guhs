"""Knuffelkamp as a map stamp: builds kamp.bouw(S) and writes it (with its world offset + POIs) to an .npz for the tile writer.

python kamp_wereld.py <out.npz>
World = scene + (OX, OY, OZ): scene sea level ZEE=10 -> y 62, camp floor VLOER=34 -> y 86.
"""
import json
import math
import sys

import numpy as np

import kamp as KP
import vrkit as K
from vrkit import stairs, slab

ROZESTEEN = "guhs:roze_knuffelsteen"

sys.path.insert(0, __import__("os").path.join(__import__("os").path.dirname(__import__("os").path.abspath(__file__)), ".."))
import indeling as IND  # noqa: E402  (T-WERELD: the camp's world offset)

OX, OY, OZ = -150 + IND.KAMP_DX, 52, 865 + IND.KAMP_DZ


def w(x, y, z):
    return [int(x + OX), int(y + OY), int(z + OZ)]


def main(uit):
    S = K.Scene(*KP.KAMP_MAAT)
    I = KP.bouw(S)
    poi = {}
    # campfires: unlit (kern checkpoints)
    for i, s in enumerate(S.pal):
        if s.startswith("minecraft:campfire"):
            S.pal[i] = s.replace("lit=true", "lit=false")
            S.idx[S.pal[i]] = i
    cx, cz = KP.KAMP
    poi["kampvuur_kamp"] = w(cx, KP.VLOER + 1, cz)
    # lighthouse: a campfire at the keeper's house + the secret cellar under the tower
    lx, lz = KP.LICHT
    ly = I.h(lx, lz)
    hx, hz = lx + 9, lz + 2
    fx, fz = hx + 2, hz + 7
    S.set(fx, I.h(fx, fz) + 1, fz, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    poi["kampvuur_vuurtoren"] = w(fx, I.h(fx, fz) + 1, fz)
    # cellar: 5x4x5 room under the tower foot, reached by a trapdoor in the tower floor
    S.fill(lx - 4, ly - 7, lz - 4, lx + 3, ly - 2, lz + 3, "minecraft:mossy_stone_bricks")
    for x in range(lx - 3, lx + 3):
        for z in range(lz - 3, lz + 3):
            for y in range(ly - 6, ly - 2):
                S.set(x, y, z, "minecraft:cave_air")
            S.set(x, ly - 7, z, "minecraft:spruce_planks")
            S.set(x, ly - 2, z, "minecraft:spruce_planks")
    # one ladder shaft from the cellar floor up through the tower floor, against the wall block east of it (lx + 3), and ONE
    # trapdoor at the top (in the tower floor). An open trapdoor right above a ladder with the same facing is climbable.
    for y in range(ly - 6, ly + 1):
        S.set(lx + 3, y, lz + 1, "minecraft:mossy_stone_bricks" if y < ly - 1 else ROZESTEEN)
    for y in range(ly - 6, ly):
        S.set(lx + 2, y, lz + 1, "minecraft:ladder[facing=west,waterlogged=false]")
    S.set(lx + 2, ly, lz + 1, "minecraft:spruce_trapdoor[facing=west,half=top,open=false,powered=false,waterlogged=false]")
    for y in (ly + 1, ly + 2):
        S.set(lx + 2, y, lz + 1, "minecraft:air")
    S.set(lx - 2, ly - 6, lz - 2, "minecraft:chest[facing=south,type=single,waterlogged=false]")
    S.set(lx - 3, ly - 5, lz - 3, "minecraft:lantern[hanging=false,waterlogged=false]")
    poi["geheim_vuurtorenkelder"] = w(lx - 2, ly - 6, lz - 2)
    # integration (fix round 1): a ladder up the inside of the tower to the gallery (kamp.vuurtoren: tower floor ly, walls
    # k 1..30, gallery floor ly + 31 with the glass light room at d 2.2..3.3). Ladder at (lx, lz - 2) on a pillar at
    # (lx, lz - 3), through the gallery floor, then a doorway north through the glass ring onto the gallery.
    for y in range(ly + 1, ly + 31):
        S.set(lx, y, lz - 3, ROZESTEEN)
    for y in range(ly + 1, ly + 32):
        S.set(lx, y, lz - 2, "minecraft:ladder[facing=south,waterlogged=false]")
    for y in (ly + 32, ly + 33):
        S.set(lx, y, lz - 2, "minecraft:air")
        S.set(lx, y, lz - 3, "minecraft:air")
    poi["vuurtoren_top"] = w(lx, ly + 32, lz - 5)
    # dock + airship
    z0 = KP.STEIGER_Z
    kx = max(x for x in range(I.X) if I.H[x, z0] >= 0)
    x_eind = kx + 30
    poi["aankomst"] = w(kx - 4, KP.VLOER + 1, z0)
    poi["steiger_eind"] = w(x_eind - 2, KP.VLOER + 1, z0)
    S.set(x_eind - 4, KP.VLOER + 1, z0 - 7, "minecraft:bell[attachment=floor,facing=east,powered=false]")
    poi["luchtschip_bel"] = w(x_eind - 4, KP.VLOER + 1, z0 - 7)
    poi["luchtschip_kapitein"] = w(x_eind - 4, KP.VLOER + 1, z0 - 5)
    poi["ordetent"] = w(*(KP.ORDETENT[0], KP.VLOER + 1, KP.ORDETENT[1]))
    poi["training"] = w(KP.TRAINING[0], KP.VLOER + 1, KP.TRAINING[1])
    tx, tz = KP.TEMPEL
    poi["tempeltje"] = w(tx, I.h(tx, tz) + 3, tz)   # (integration: stand ON the plinth, not in its hollow)
    vx, vz = KP.VIJVER
    poi["vijver"] = w(vx, KP.VLOER + 1, vz)
    # sea: drop the scene's own water + sea floor (the global sea goes there); keep the dock posts
    zee = {S.idx.get(b) for b in ("minecraft:water", "minecraft:sand", "minecraft:prismarine", "minecraft:light_blue_terracotta")}
    zee.discard(None)
    zeekol = I.H < 0
    for y in range(0, KP.ZEE + 1):
        laag = S.v[:, y, :]
        m = zeekol & np.isin(laag, list(zee))
        laag[m] = 0
    uitgangen(S, I, poi)
    steiger_ladders(S, I)
    S.fix_connections()
    hoogte = np.where(I.H >= 0, I.H + OY, -1)
    np.savez_compressed(uit, v=S.v, pal=np.array(S.pal), off=np.array([OX, OY, OZ]), hoogte=hoogte,
                        guhs=np.array([[g[0] + OX, g[1] + OY, g[2] + OZ, g[3]] for g in S.guhs], float))
    with open(uit.replace(".npz", ".json"), "w", encoding="utf-8") as f:
        json.dump(poi, f, indent=1)
    print("kamp:", int((S.v > 0).sum()), "blokken", poi)


# ------------------------------------------------------------------------------------------------ ways out of the sea
RICHTING = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
LADDER = "minecraft:ladder[facing={f},waterlogged={w}]"
STEUN = ["minecraft:mossy_stone_bricks", "minecraft:stone_bricks", "minecraft:mossy_cobblestone", "minecraft:stone_bricks"]
VRIJ = ("grass", "fern", "flower", "tulip", "allium", "daisy", "cornflower", "bluet", "guhbloem", "guhoortjes", "_leaves",
        "petals", "carpet", "_log[axis=y]")


def ladder(S, x, z, uit, y0, y1):
    """A ladder column in (x, z) from y0 to y1 (inclusive), on the face of the block at (x, z) - uit; waterlogged in the sea."""
    f = RICHTING[uit]
    S.set(x, y0 - 1, z, "minecraft:sandstone")      # a floor under the ladder (the sea floor around is only a skin)
    for y in range(y0, y1 + 1):
        S.set(x, y, z, LADDER.format(f=f, w="true" if y <= KP.ZEE else "false"))


def vrij(S, x, y, z):
    b = S.get(x, y, z)
    return b in ("minecraft:air", "minecraft:cave_air") or any(t in b for t in VRIJ)


def uitgangen(S, I, poi):
    """Every ~14 blocks of coast a way out of the sea up to the plateau: a little sand beach at the waterline, a ladder up a
    stone-brick buttress against the cliff and a plank walk on top that ends on the plateau (no swim is longer than ~10)."""
    H = I.H
    land = H >= 0
    kust = []
    for x in range(1, I.X - 1):
        for z in range(1, I.Z - 1):
            if land[x, z] and not (land[x + 1, z] and land[x - 1, z] and land[x, z + 1] and land[x, z - 1]):
                kust.append((math.atan2(z - KP.CZ, x - KP.CX), x, z))
    kust.sort()
    jx = 150      # world x 0 = the south jetty (bouwsels.kamp_zuidsteiger)

    def verboden(x, z):
        if z > KP.CZ and abs(x - jx) < 12:
            return True                                   # the south jetty + its stair
        if x > KP.CX + 40 and abs(z - KP.STEIGER_Z) < 6:
            return True                                   # the airship jetty
        return False
    gemaakt = []
    afstand = 99.0
    vorige = None
    for (_, x, z) in kust:
        if vorige is not None:
            afstand += math.hypot(x - vorige[0], z - vorige[1])
        vorige = (x, z)
        if afstand < 14 or verboden(x, z):
            continue
        if any(math.hypot(x - a, z - b) < 10 for (a, b) in gemaakt):
            continue
        if uitgang(S, I, x, z):
            gemaakt.append((x, z))
            afstand = 0.0
    print("kamp: uitgangen uit zee:", len(gemaakt))


def uitgang(S, I, x, z):
    H = I.H
    ZEE = KP.ZEE
    dx, dz = KP.CX - x, KP.CZ - z
    for d in sorted(RICHTING, key=lambda d: -(d[0] * dx + d[1] * dz))[:2]:
        lx, lz = x - d[0], z - d[1]
        if not (0 <= lx < I.X and 0 <= lz < I.Z) or H[lx, lz] >= 0:
            continue
        p = (-d[1], d[0])
        prof = []
        for k in range(0, 18):
            xx, zz = x + k * d[0], z + k * d[1]
            if not (0 <= xx < I.X and 0 <= zz < I.Z) or H[xx, zz] < 0:
                prof = None
                break
            prof.append(int(H[xx, zz]))
        if not prof:
            continue
        kk = None
        for k in range(len(prof) - 3):
            if prof[k] >= ZEE + 6 and all(prof[k + i + 1] - prof[k + i] <= 1 for i in range(3)):
                kk = k
                break
        if kk is None or kk > 12:
            continue
        T = prof[kk]
        # nothing built in the way (trees/leaves and flowers are fine: they get cut)
        if not all(vrij(S, x + j * d[0] + q * p[0], y, z + j * d[1] + q * p[1])
                   for j in range(-1, kk + 2) for q in range(-2, 3) for y in range(T + 1, T + 4)):
            continue
        # the side columns of the beach must be sea too
        for j in range(0, kk + 1):
            for q in range(-2, 3):
                xx, zz = x + j * d[0] + q * p[0], z + j * d[1] + q * p[1]
                h = int(H[xx, zz]) if H[xx, zz] >= 0 else ZEE - 4
                for y in range(ZEE - 4, T):
                    if y > h or S.is_air(xx, y, zz):
                        S.set(xx, y, zz, STEUN[(xx * 3 + y + zz) % 4])
                for y in range(T + 1, max(h, T + 3) + 1):
                    S.set(xx, y, zz, "minecraft:air")
                if abs(q) <= 1:
                    S.set(xx, T, zz, "minecraft:spruce_planks")
                else:
                    S.set(xx, T, zz, "minecraft:stone_bricks")
                    bx, bz = xx + p[0] * (1 if q > 0 else -1), zz + p[1] * (1 if q > 0 else -1)
                    hb = H[bx, bz] if (0 <= bx < I.X and 0 <= bz < I.Z) else -1
                    if hb < T - 1 and 0 < j < kk:
                        S.set(xx, T + 1, zz, "minecraft:spruce_fence")
        # the ladder (sea side) + a lantern post beside it
        ladder(S, lx, lz, (-d[0], -d[1]), ZEE - 3, T)
        S.set(x + 2 * p[0], T + 1, z + 2 * p[1], "minecraft:spruce_fence")
        S.set(x + 2 * p[0], T + 2, z + 2 * p[1], "minecraft:lantern[hanging=false,waterlogged=false]")
        # a tiny sand beach on both sides of the ladder at the waterline
        for q in (-2, -1, 1, 2):
            bx, bz = lx + q * p[0], lz + q * p[1]
            if not (0 <= bx < I.X and 0 <= bz < I.Z) or H[bx, bz] >= 0:
                continue
            top = ZEE if abs(q) == 1 else ZEE - 1
            for y in range(ZEE - 4, top + 1):
                S.set(bx, y, bz, "minecraft:sand" if y == top else "minecraft:sandstone")
            bx2, bz2 = bx - d[0], bz - d[1]
            if abs(q) == 1 and 0 <= bx2 < I.X and 0 <= bz2 < I.Z and H[bx2, bz2] < 0:
                for y in range(ZEE - 4, ZEE):
                    S.set(bx2, y, bz2, "minecraft:sand" if y == ZEE - 1 else "minecraft:sandstone")
        return True
    return False


def steiger_ladders(S, I):
    """The high airship jetty (deck at VLOER, 24 above the sea): ladders from the water up to the deck, with a gap in the
    railing, at two spots along it and at the end platform."""
    z0 = KP.STEIGER_Z
    kx = max(x for x in range(I.X) if I.H[x, z0] >= 0)
    y = KP.VLOER
    x_eind = kx + 30
    for x in (kx + 6, kx + 21):
        for kant in (-1, 1):
            zr = z0 + 3 * kant
            for yy in range(KP.ZEE - 4, y):
                S.set(x, yy, zr, "minecraft:spruce_log[axis=y]")
            S.set(x, y + 1, zr, "minecraft:air")
            ladder(S, x, zr + kant, (0, kant), KP.ZEE - 3, y)
    # the end platform (x_eind - 6 .. x_eind + 2): a ladder on its east side
    xe, zl = x_eind + 2, z0 - 4
    for yy in range(KP.ZEE - 4, y):
        S.set(xe, yy, zl, "minecraft:spruce_log[axis=y]")
    S.set(xe, y + 1, zl, "minecraft:air")
    ladder(S, xe + 1, zl, (1, 0), KP.ZEE - 3, y)


if __name__ == "__main__":
    main(sys.argv[1])
