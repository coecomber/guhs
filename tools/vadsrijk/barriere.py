"""T-WERELD: the shared barriers between the zones (WERELDINDELING §3 + §6), in WORLD coordinates.

De Splinterwand (zone 1 -> 2): a row of sheer rock spires and crystal needles, 90-150 blocks out of the sea, with gaps to look
through. It is ONE deterministic list of spires; every map generator stamps the spires that overlap its own tile grid (zone 1
in world coordinates, zone 2 in its local frame: off - Z2 offset), and the tile row between the zone-1 and zone-2 grids
(tz -12, which no zone owns) is written here by maak_tegels() (sea + spires). So the wall runs over the seams.

Usage (map build, from maak_kaart.py):  barriere.maak_tegels()   -> t_<tx>_-12.nbt for tx -2..15
"""
import math
import os
import random
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
T, HOOG = 64, 320
ZEE = 62

# the wall line (west -> east, world): along the gap between the north coast of zone 1 (z ~ -615) and the south coast of
# zone 2 (z ~ -806), on to the sea south of zone 2's east cape
WAND_LIJN = [(-70, -690), (90, -712), (260, -700), (420, -728), (560, -742), (700, -760), (830, -745), (930, -728)]
GAT_TEGELS = [(tx, -12) for tx in range(-2, 16)]       # the unowned tile row the wall stands in

ROTS = ["minecraft:stone", "minecraft:andesite", "minecraft:tuff", "minecraft:stone", "minecraft:calcite", "minecraft:diorite",
        "minecraft:stone", "minecraft:cobbled_deepslate"]
SNEEUW = "minecraft:snow_block"
IJS = "minecraft:packed_ice"
KRISTAL = "minecraft:light_blue_stained_glass"
KRISTAL_KERN = "minecraft:blue_ice"


def _lijnpunten(lijn, stap):
    pts = []
    for (ax, az), (bx, bz) in zip(lijn, lijn[1:]):
        L = math.hypot(bx - ax, bz - az)
        n = max(1, int(L // stap))
        for i in range(n):
            t = i / n
            pts.append((ax + (bx - ax) * t, az + (bz - az) * t, (bx - ax) / L, (bz - az) / L))
    return pts


def spitsen():
    """The spires: (x, z, height above the sea, base radius, lean x, lean z, kind)."""
    rng = random.Random(2026_11_05)
    uit = []
    for (x, z, ux, uz) in _lijnpunten(WAND_LIJN, 21):
        if rng.random() < 0.12:
            continue                                   # a gap to look through
        nx, nz = -uz, ux
        off = rng.uniform(-22, 22)
        x, z = x + nx * off + rng.uniform(-5, 5), z + nz * off + rng.uniform(-5, 5)
        kristal = rng.random() < 0.22
        h = rng.uniform(70, 110) if kristal else rng.uniform(85, 145)
        r = rng.uniform(4.5, 7) if kristal else rng.uniform(7, 13)
        uit.append((round(x), round(z), round(h), r, rng.uniform(-0.12, 0.12) * h, rng.uniform(-0.12, 0.12) * h,
                    "kristal" if kristal else "rots"))
        if not kristal and rng.random() < 0.55:        # a smaller satellite spire at its foot
            a = rng.uniform(0, 2 * math.pi)
            uit.append((round(x + (r + 4) * math.cos(a)), round(z + (r + 4) * math.sin(a)), round(h * rng.uniform(0.35, 0.6)),
                        r * 0.55, 0.0, 0.0, "rots"))
    return uit


def _spits_vol(x, z, h, r0, lx, lz, soort, seed):
    """Voxel volume (uint8 palette ids, palette PAL) + world offset of one spire."""
    R = int(r0 + abs(lx) + abs(lz) + 4)
    y0, y1 = 36, ZEE + int(h) + 2
    X = Z = 2 * R + 1
    v = np.zeros((X, y1 - y0 + 1, Z), np.uint8)
    rng = np.random.default_rng(seed)
    fase = rng.uniform(0, 6.28, 3)
    xs = np.arange(X)[:, None] - R
    zs = np.arange(Z)[None, :] - R
    a = np.arctan2(zs, xs)
    for yi in range(v.shape[1]):
        y = y0 + yi
        t = max(0.0, (y - ZEE) / h)
        if t > 1:
            break
        r = r0 * (1 - t) ** 0.75 + 0.8
        r = r * (1 + 0.16 * np.sin(3 * a + fase[0] + y * 0.07) + 0.08 * np.sin(5 * a + fase[1]))
        if y < ZEE:
            r = r + (ZEE - y) * 0.35                    # a wider foot under water
        cx, cz = lx * t ** 1.5, lz * t ** 1.5
        d = np.hypot(xs - cx, zs - cz)
        m = d <= r
        if soort == "kristal":
            laag = np.where(d <= r * 0.55, PAL.index(KRISTAL_KERN), PAL.index(KRISTAL))
        else:
            band = ((y + int(3 * math.sin(y * 0.2 + fase[2]))) // 4) % len(ROTS)
            laag = np.full(m.shape, PAL.index(ROTS[band]))
            if t > 0.72:
                laag = np.where((d > r - 1.6) | (y % 3 == 0), PAL.index(SNEEUW), laag)
            elif t > 0.55:
                laag = np.where(np.sin(a * 4 + y) > 0.6, PAL.index(IJS), laag)
        v[:, yi, :] = np.where(m, laag, v[:, yi, :])
    return v, (x - R, y0, z - R)


PAL = ["minecraft:air"] + sorted(set(ROTS)) + [SNEEUW, IJS, KRISTAL, KRISTAL_KERN]


def stempels(stempel_cls, doos=None, dx=0, dz=0):
    """Every spire overlapping the world box doos = (x0, z0, x1, z1) as a stamp of the caller's kind (tegels.Stempel), its
    offset moved by (-dx, -dz) into the caller's local frame."""
    uit = []
    for i, (x, z, h, r, lx, lz, soort) in enumerate(spitsen()):
        R = r + abs(lx) + abs(lz) + 6
        if doos and (x + R < doos[0] or x - R > doos[2] or z + R < doos[1] or z - R > doos[3]):
            continue
        v, (ox, oy, oz) = _spits_vol(x, z, h, r, lx, lz, soort, i)
        uit.append(stempel_cls(f"splinterwand_{i}", v, PAL, (ox - dx, oy, oz - dz)))
    return uit


# ------------------------------------------------------------------------------------------------ the unowned tile row
def vloer_y(z):
    """Sea floor in the gap row: deep (49) at the zone-1 side, shallower towards zone 2's cloud sea (58)."""
    t = min(max((-705 - z) / 63.0, 0.0), 1.0)
    return int(round(49 + 9 * t))


def maak_tegels(alleen=None):
    sys.path.insert(0, os.path.join(HIER, "wereld"))
    import blokken as B
    import nbtsnel
    palet = B.Palet()
    ids = {s: palet.id(s) for s in PAL[1:] + ["minecraft:water", "minecraft:sand", "minecraft:gravel", "minecraft:stone",
                                              "minecraft:light_blue_terracotta"]}
    lut = np.array([0] + [ids[s] for s in PAL[1:]], np.uint16)
    alle = spitsen()
    vols = []
    for i, (x, z, h, r, lx, lz, soort) in enumerate(alle):
        v, off = _spits_vol(x, z, h, r, lx, lz, soort, i)
        vols.append((v, off))
    n = 0
    for (tx, tz) in GAT_TEGELS:
        if alleen and (tx, tz) not in alleen:
            continue
        x0, z0 = tx * T, tz * T
        vol = np.zeros((T, HOOG, T), np.uint16)
        for zi in range(T):
            fy = vloer_y(z0 + zi)
            vol[:, 40:fy - 1, zi] = ids["minecraft:stone"]
            vol[:, fy - 1, zi] = ids["minecraft:gravel"] if fy > 54 else ids["minecraft:sand"]
            vol[:, fy, zi] = ids["minecraft:gravel"] if fy > 54 else ids["minecraft:light_blue_terracotta"]
            vol[:, fy + 1:ZEE + 1, zi] = ids["minecraft:water"]
        for (v, (ox, oy, oz)) in vols:
            X, Y, Z = v.shape
            ax0, az0 = max(x0, ox), max(z0, oz)
            ax1, az1 = min(x0 + T, ox + X), min(z0 + T, oz + Z)
            if ax0 >= ax1 or az0 >= az1:
                continue
            blok = v[ax0 - ox:ax1 - ox, :, az0 - oz:az1 - oz]
            g = lut[blok]
            doel = vol[ax0 - x0:ax1 - x0, oy:oy + Y, az0 - z0:az1 - z0]
            m = g > 0
            doel[m] = g[m]
        # hollow: drop blocks enclosed on all 6 sides by opaque rock (smaller files)
        opaak = np.zeros(len(palet.lijst) + 1, bool)
        for s, i in ids.items():
            opaak[i] = not any(w in s for w in ("glass", "ice", "water"))
        occ = opaak[vol]
        inn = occ.copy()
        inn[0, :, :] = inn[-1, :, :] = False
        inn[:, 0, :] = inn[:, -1, :] = False
        inn[:, :, 0] = inn[:, :, -1] = False
        inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] & \
            occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
        vol[inn] = 0
        xs, ys, zs = np.nonzero(vol)
        idv = vol[xs, ys, zs]
        gebruikt, inv = np.unique(idv, return_inverse=True)
        palette = [B.splits(palet.lijst[i]) for i in gebruikt]
        nbtsnel.schrijf(os.path.join(UIT, f"t_{tx}_{tz}.nbt"), (T, HOOG, T), palette, xs.astype(np.int32), ys.astype(np.int32),
                        zs.astype(np.int32), inv.astype(np.int32), None)
        n += 1
    print(f"barriere: {n} tegels in de rij tz -12 (Splinterwand, {len(alle)} spitsen)")


if __name__ == "__main__":
    maak_tegels()


# ------------------------------------------------------------------------------------------------ zone 3: the sea + cloud sea
Z3_STAM = (1404, -576)
WOLK = ["minecraft:white_wool", "minecraft:white_wool", "guhs:pluiswolblok", "minecraft:white_concrete_powder"]


def _ruis2(X, Z, cel, seed):
    """Smooth 2D value noise in [0, 1) on world coordinates (deterministic; the same over every tile)."""
    gx, gz = np.floor(X / cel), np.floor(Z / cel)
    fx, fz = X / cel - gx, Z / cel - gz

    def h(a, b):
        v = (a.astype(np.int64) * 73856093) ^ (b.astype(np.int64) * 19349663) ^ (seed * 83492791)
        v = (v ^ (v >> 13)) * 1274126177
        return ((v ^ (v >> 16)) & 0xFFFF) / 65536.0
    sx, sz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    return (h(gx, gz) * (1 - sx) * (1 - sz) + h(gx + 1, gz) * sx * (1 - sz) + h(gx, gz + 1) * (1 - sx) * sz
            + h(gx + 1, gz + 1) * sx * sz)


def kloof_x(Z):
    """Centre line of the Wolkenkloof (the open channel between the cloud banks of zone 2 and zone 3), world x at world z."""
    return 1066 + 20 * np.sin(Z / 70.0) + 9 * np.sin(Z / 23.0 + 1.3)


def zee_z3(vol, tx, tz, palet_id):
    """T-WERELD: zone 3 gets the sea (y <= 62) round its floating island and the cloud sea on top: dense banks with the
    meandering Wolkenkloof channel in the west (towards zone 2), broken banks falling apart in the east (the Wolkenval,
    towards zone 4). Only columns 205+ blocks from the trunk (outside the island core), only empty cells. vol: (64, 320, 64)
    of global palette ids, modified in place."""
    X = (tx * T + np.arange(T))[:, None].astype(np.float64) + np.zeros((1, T))
    Z = (tz * T + np.arange(T))[None, :].astype(np.float64) + np.zeros((T, 1))
    R = np.hypot(X - Z3_STAM[0], Z - Z3_STAM[1])
    zee = R >= 205
    if not zee.any():
        return
    water, grind, steen, zand = (palet_id(s) for s in ("minecraft:water", "minecraft:gravel", "minecraft:stone", "minecraft:sand"))
    wolk = [palet_id(s) for s in WOLK]
    wolk_set = np.array(sorted(set(wolk)))
    # sea floor: 50, rising to 60 at the zone-2 seam (zone 2's shallow cloud sea)
    F = np.round(50 + 10 * np.clip((1090 - X) / 66, 0, 1)).astype(np.int32)
    for i, k in zip(*np.nonzero(zee)):
        f = F[i, k]
        kol = vol[i, :, k]
        if R[i, k] >= 245:
            onder = kol[:f]
            onder[np.isin(onder, wolk_set)] = 0          # the old low cloud floor (y 14-40) is under the sea bed now
        for y in range(f - 3, f):
            if kol[y] == 0:
                kol[y] = steen
        if kol[f] == 0:
            kol[f] = zand if R[i, k] < 260 else grind
        seg = kol[f + 1:ZEE + 1]
        seg[seg == 0] = water
    # cloud banks on the water (outside the island rim): U-OVERGANG moved the field into zeenaad.wolk (shared with the seam pass and
    # the Java sea filler, so the banks continue past the tile box and thin out instead of stopping at its edge)
    import zeenaad
    dik, wkeus = zeenaad.wolk(X, Z)
    for i, k in zip(*np.nonzero(zee & (R >= 262) & (dik >= 1))):
        kol = vol[i, :, k]
        if kol[ZEE - 1] != water or kol[ZEE] != water:
            continue                                                  # only over open water (not on a beach or a root)
        t = int(dik[i, k])
        for y in range(ZEE, ZEE + 1 + t):   # (U-OVERGANG: the same blocks as zeenaad.wolk / ZeeVulling)
            if kol[y] in (0, water):
                kol[y] = wolk[wkeus[i, k]]
