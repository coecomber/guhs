"""X-RAID: block-level structure check of De Sterrendovende Toren (zone 6, the raid tower OUTSIDE, u-z6wereld's tiles).

Reads the local zone-6 world that maak6.py saves (tools/vadsrijk/_cache/z6/wereld.npz: every voxel, before hollowing) and checks
the box around the tower (x 4100..4180, z -730..-646, from the rock up):
  LEK      air inside the voet (distance < 15 from the axis) or inside a shaft section (distance < r - 2) that is 6-connected
           to the open air outside: the tower can be walked/looked into (the round-W bug: a 150-block hollow tube behind the door).
           The niche of the door (x < tx-13) is outside air by design and does not count.
  ZWEEF    solid pieces (26-connected) that do not touch the ground: floating rings, a floating lens, loose blocks.
  NAAD     per section joint: wall cells of the section above that have NO solid block under them (a wall standing on air);
           the arched ceiling of the door niche is skipped (open below by design).
  DEUR     the door: the niche is open (2 air above the floor across its width), the walk in from the stair top is free
           (3 wide), the back wall is closed (no air cell in it).
Run from the project root (after `python tools/vadsrijk/zone6/maak6.py --alleen "64,-11;64,-12;65,-11;65,-12"`):
    python tools/vadsrijk/zone6/raid/torencheck.py [--npz other.npz]
Exit 1 when LEK, ZWEEF or NAAD finds anything, or DEUR fails.
"""
import argparse
import math
import os
import sys

import numpy as np
from scipy import ndimage

HIER = os.path.dirname(os.path.abspath(__file__))
NPZ = os.path.join(HIER, "..", "..", "_cache", "z6", "wereld.npz")
TX, TZ = 4140, -688
BOX = (4100, 4181, -730, -645)          # x0, x1, z0, z1 (exclusive end)
# the section table of landschap.sterrendovende_toren: (r, h); the shaft starts at y0 + 14
SECTIES = [(16, 34), (13, 32), (10.5, 30), (8, 24)]
NIET_VAST = ("glass_pane", "chain", "lantern", "torch", "carpet", "banner", "sapling", "tulip", "petals", "short_grass",
             "tall_grass", "fern", "dead_bush", "allium", "daisy", "peony", "azalea_bush", "short_dry_grass", "guhbloem")


def _vast(naam):
    n = naam.split("[")[0].split(":")[-1]
    return n not in ("air", "cave_air", "light") and not any(w in n for w in NIET_VAST)


def lees(pad):
    d = np.load(pad, allow_pickle=True)
    v, pal, b = d["v"], [str(s) for s in d["pal"]], d["b"]
    x1, y1, z1 = int(b[0]), int(b[1]), int(b[2])
    sub = v[BOX[0] - x1:BOX[1] - x1, :, BOX[2] - z1:BOX[3] - z1]
    vast_pal = np.array([_vast(n) for n in pal])
    lucht_pal = np.array([n.split("[")[0] in ("minecraft:air", "minecraft:cave_air") for n in pal])
    return sub, vast_pal[sub], lucht_pal[sub], y1, pal


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--npz", default=NPZ)
    a = ap.parse_args()
    sub, vast, lucht, ybase, pal = lees(a.npz)
    X, Y, Z = sub.shape
    xs = np.arange(BOX[0], BOX[1])[:, None]
    zs = np.arange(BOX[2], BOX[3])[None, :]
    hd = np.hypot(xs - TX, zs - TZ)
    # y0 = the voet floor: highest solid y at the axis below the shaft (first air going up from the bottom at the axis + 1)
    iz = TZ - BOX[2]
    # y0 = the voet floor: the solid layer under the first 2 air cells in the door niche (column tx-15)
    dx_door = TX - 15 - BOX[0]
    y0i = next(y for y in range(Y - 2) if vast[dx_door, y, iz] and lucht[dx_door, y + 1, iz] and lucht[dx_door, y + 2, iz]
               and y > 40 - ybase)
    y0 = y0i + ybase
    fout = 0
    print(f"toren: as {TX},{TZ}, voetvloer y0 = {y0}")

    # ---- LEK: interior air connected to the outside air
    open_ = ~vast
    lab, n = ndimage.label(open_)          # 6-connectivity
    buiten = set(np.unique(np.concatenate([lab[0].ravel(), lab[-1].ravel(), lab[:, :, 0].ravel(), lab[:, :, -1].ravel(),
                                          lab[:, -1, :].ravel()])))
    buiten.discard(0)
    binnen = np.zeros_like(open_)
    for y in range(y0i + 1, y0i + 13):                     # the voet (behind the door niche)
        binnen[:, y, :] = (hd < 15) & (xs + 0 * zs > TX - 13)
    y = y0i + 14
    for r, h in SECTIES:                                   # each section: inside its wall
        for yy in range(y, min(Y, y + h)):
            binnen[:, yy, :] = hd < r - 2.0
        y += h + 1
    lek = binnen & open_ & np.isin(lab, list(buiten))
    nl = int(lek.sum())
    if nl:
        fout += 1
        ys = np.nonzero(lek)[1] + ybase
        print(f"LEK   {nl} air cells inside the tower reach the outside (y {ys.min()}..{ys.max()})")
        ii = np.argwhere(lek)[:5]
        for (i, j, k) in ii:
            print(f"        e.g. {i + BOX[0]} {j + ybase} {k + BOX[2]}")
    else:
        print("LEK   0: the tower is closed (only the door niche is open)")

    # ---- ZWEEF: solid pieces not touching the ground (the layer at y0 - 4 and below counts as ground)
    lab2, n2 = ndimage.label(vast, structure=np.ones((3, 3, 3)))
    grond = set(np.unique(lab2[:, :max(1, y0i - 4), :]))
    grond.discard(0)
    toren = np.zeros_like(vast)
    toren[:, y0i - 3:, :] = (hd < 40)[:, None, :]
    los = [l for l in np.unique(lab2[toren & vast]) if l and l not in grond]
    if los:
        fout += 1
        print(f"ZWEEF {len(los)} floating pieces:")
        for l in los[:15]:
            c = np.argwhere(lab2 == l)
            lo, hi = c.min(0), c.max(0)
            print(f"        {len(c)} blocks, x {lo[0] + BOX[0]}..{hi[0] + BOX[0]} y {lo[1] + ybase}..{hi[1] + ybase} "
                  f"z {lo[2] + BOX[2]}..{hi[2] + BOX[2]} ({pal[sub[tuple(c[0])]]})")
    else:
        print("ZWEEF 0: every block of the tower hangs together with the ground")

    # ---- NAAD: the lowest wall layer of every section stands on solid blocks
    y = y0i + 14
    naad = 0
    for si, (r, h) in enumerate(SECTIES):
        nis = (xs + 0 * zs < TX - 12) & (np.abs(zs + 0 * xs - TZ) <= 5)     # the iwan's arched niche ceiling: open below by design
        laag = vast[:, y, :] & (hd >= r - 2.2) & (hd <= r * 1.09 + 0.5) & ~nis
        onder = vast[:, y - 1, :]
        mis = laag & ~onder
        k = int(mis.sum())
        print(f"NAAD  section {si + 1} (r {r}) at y {y + ybase}: {int(laag.sum())} wall cells, {k} on air")
        naad += k
        y += h + 1
    if naad:
        fout += 1

    # ---- DEUR: niche open across, back wall closed
    nis_open = all(lucht[TX - 15 - BOX[0], y0i + 1, TZ + dz - BOX[2]] and lucht[TX - 15 - BOX[0], y0i + 2, TZ + dz - BOX[2]]
                   for dz in range(-3, 4))
    achter = [(dz, yy) for dz in range(-5, 6) for yy in range(y0i + 1, y0i + 9) if open_[TX - 13 - BOX[0], yy, TZ + dz - BOX[2]]]
    # the walk in: from the stair top (tx-21) to the back of the niche (tx-14), 3 wide on the axis, 2 air above the floor
    dicht = [(x, dz) for x in range(TX - 21, TX - 13) for dz in (-1, 0, 1)
             if not (lucht[x - BOX[0], y0i + 1, TZ + dz - BOX[2]] and lucht[x - BOX[0], y0i + 2, TZ + dz - BOX[2]])]
    print(f"DEUR  niche open across 7 cells: {nis_open}; holes in the back wall (x {TX - 13}): {len(achter)}; "
          f"blocked cells on the way in (x {TX - 21}..{TX - 14}): {dicht}")
    if not nis_open or achter or dicht:
        fout += 1
    print("OK" if not fout else f"{fout} problem kind(s)")
    return 1 if fout else 0


if __name__ == "__main__":
    sys.exit(main())
