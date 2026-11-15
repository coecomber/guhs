"""1.10.4: walking check of the Ambachtseiland: from the Knuffelkamp campfire (the cook's spot) over the new path and the bridge
to every islet POI, WITHOUT swimming (water is a wall here), so a reached POI proves the way over land + bridge works.

    python tools/vadsrijk/kamp/ambacht_bereik.py          prints every POI (reached + walking distance) and the route to Meester Knutsel

Model (a simplified bereikcheck.py, with its block classes): a stand cell needs a floor below (full block, slab, stair, leaves;
not a fence/wall/lantern) and 2 passable cells (air, plants, signs, doors); a step to a 4-neighbour may go up 1 (with 3 cells of
head room at the start) or down at most 3. The map = the committed tiles (what kaartpak.py packs into the jar).
"""
import collections
import json
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
VADSRIJK = os.path.dirname(HIER)
sys.path.insert(0, VADSRIJK)
sys.path.insert(0, os.path.join(VADSRIJK, "audit"))
import bereikcheck as BC  # noqa: E402
import tegellezer as TL  # noqa: E402

X0, Z0, X1, Z1 = -720, 930, -360, 1120          # the region (world, inclusive-exclusive)
Y0, Y1 = 40, 140
START = (-380, 87, 1029)                         # NpcPlekken "kok" = kampvuur_kamp + (4, 0, 4)
BRUG = (-539, -476, 1021, 1027)                  # the bridge box x0, x1, z0, z1
LOOP = (BC.AIR, BC.PLANT, BC.DOOR, BC.SPOOK)
VLOER = (BC.SOLID, BC.SLAB, BC.STAIR)


def laad():
    nx, nz = X1 - X0, Z1 - Z0
    cls = np.zeros((nx, Y1 - Y0, nz), np.uint8)
    cache = {}
    for tx in range(X0 // 64, (X1 - 1) // 64 + 1):
        for tz in range(Z0 // 64, (Z1 - 1) // 64 + 1):
            pad = os.path.join(BC.TEGELS, f"t_{tx}_{tz}.nbt")
            if not os.path.exists(pad):
                continue
            pal, (xs, ys, zs, st) = TL.lees(pad)
            lut = np.array([cache.setdefault(s, BC.klasse(s)) for s in pal], np.uint8)
            wx, wz = xs + tx * 64 - X0, zs + tz * 64 - Z0
            ok = (wx >= 0) & (wx < nx) & (wz >= 0) & (wz < nz) & (ys >= Y0) & (ys < Y1)
            cls[wx[ok], ys[ok] - Y0, wz[ok]] = lut[st[ok]]
    return cls


def main():
    cls = laad()
    loop = np.isin(cls, LOOP)
    vloer = np.isin(cls, VLOER)
    staan = np.zeros_like(loop)
    staan[:, 1:-1, :] = vloer[:, :-2, :] & loop[:, 1:-1, :] & loop[:, 2:, :]
    sx, sy, sz = START[0] - X0, START[1] - Y0, START[2] - Z0
    assert staan[sx, sy, sz], "start is not a stand cell"
    vorige = {(sx, sy, sz): None}
    afst = {(sx, sy, sz): 0}
    rij = collections.deque([(sx, sy, sz)])
    NX, NY, NZ = staan.shape
    while rij:
        x, y, z = rij.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx_, nz_ = x + dx, z + dz
            if not (0 <= nx_ < NX and 0 <= nz_ < NZ):
                continue
            for dy in (0, 1, -1, -2, -3):
                ny = y + dy
                if not (1 <= ny < NY - 2) or not staan[nx_, ny, nz_]:
                    continue
                if dy == 1 and not loop[x, y + 2, z]:
                    break                                         # no head room to jump
                if dy < 0 and not all(loop[nx_, ny + k, nz_] for k in range(2, 2 - dy + 1)):
                    continue                                      # the drop column must be free
                k = (nx_, ny, nz_)
                if k not in vorige:
                    vorige[k] = (x, y, z)
                    afst[k] = afst[(x, y, z)] + 1
                    rij.append(k)
                break
    print(f"bereikt: {len(vorige)} stand-cellen (zonder zwemmen) vanaf de kok bij het kampvuur {START}")
    eiland = [k for k in vorige if k[0] + X0 <= -545]
    print(f"  waarvan op het eiland (x <= -545): {len(eiland)}")
    with open(os.path.join(HIER, "ambacht_plekken.json"), encoding="utf-8") as f:
        P = json.load(f)
    doelen = {}
    for k, v in P.items():
        if isinstance(v, list):
            doelen[k] = v
        else:
            for s, w in v.items():
                if s not in ("min", "max"):
                    doelen[f"{k}.{s}"] = w
    mis = 0
    for k, (x, y, z) in sorted(doelen.items()):
        beste = None
        for (ax, ay, az) in ((x, y, z), (x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y + 1, z), (x, y - 1, z)):
            c = (ax - X0, ay - Y0, az - Z0)
            if c in afst and (beste is None or afst[c] < beste):
                beste = afst[c]
        if beste is None:
            mis += 1
        print(f"  {'OK  ' if beste is not None else 'MIS '} {k:34s} {x}, {y}, {z}" + (f"  ({beste} stappen)" if beste is not None else ""))
    # the route to Meester Knutsel: does it cross the bridge?
    x, y, z = P["ambacht_knutsel"]
    c = (x - X0, y - Y0, z - Z0)
    over = False
    n = 0
    while c is not None and c in vorige:
        wx, wz = c[0] + X0, c[2] + Z0
        over |= BRUG[0] <= wx <= BRUG[1] and BRUG[2] <= wz <= BRUG[3]
        c = vorige[c]
        n += 1
    print(f"route kok -> Meester Knutsel: {n} stappen, over de brug: {over}")
    print(f"{len(doelen) - mis}/{len(doelen)} POI's bereikt")
    return 1 if mis or not over else 0


if __name__ == "__main__":
    sys.exit(main())
