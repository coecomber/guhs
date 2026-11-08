"""U-DECO: floating-decoration check over the generated map tiles.

Lists every decoration block (flowers, plants, standing lanterns/lampions, lamp-post feet, carpets, pots, benches, bins, knuffel
blocks, standing signs) with AIR directly under it, i.e. something placed 1 block too high (a ground-height bug in a deco layer).
Hanging lanterns/lampions, wall signs/banners and vines are skipped (they hang). Fences/walls are NOT listed (railings and
lamp masts stand on purpose on edges; their own check is the connection pass).

    python tools/vadsrijk/zweefcheck.py [--zone 1|2|3|4|5|6|alle] [--max N]
Exit code 1 when anything floats (so it can gate a regen).
"""
import argparse
import os
import sys
from collections import defaultdict

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "audit"))
import tegellezer as TL  # noqa: E402

ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
ZONES = {"1": (-20, -16, 15, 20), "2": (6, -23, 15, -13), "3": (16, -16, 26, -3), "4": (27, -12, 39, -5), "5": (38, -12, 48, -5), "6": (51, -18, 66, -7)}   # V-INT: + zone 6 (u-z6wereld)

DECO_NAAM = ("_tulip", "allium", "oxeye_daisy", "cornflower", "dandelion", "poppy", "orchid", "azure_bluet", "lily_of_the_valley",
             "_flower", "guhbloem", "hibiscus", "guhoortjes", "short_grass", "fern", "_sapling", "_carpet", "flower_pot",
             "bloempot", "bankje", "prullenbak", "knuffel_", "lantaarnpaal[half=lower", "_sign[rotation", "torch",
             "lantern[hanging=false", "lampion_roze", "lampion_geel", "lampion_mint", "campfire", "candle", "theepotje",
             "sea_pickle", "pink_petals", "wildflowers", "leaf_litter", "bush")
NIET = ("hanging=true", "wall_", "potted_", "_leaves", "knuffelklinkers", "knuffelsteen", "knuffelblok", "knuffelgras",
        "knuffel_mager_blok", "vines", "_bed", "lily_pad", "pastelbaksteen", "pleister", "_carpet")
HANGERS = ("lantern[", "lampion_")      # a lantern/lampion with something above it should be hanging=true


def is_deco(s):
    return any(k in s for k in DECO_NAAM) and not any(k in s for k in NIET)


def check(zone, maxn):
    tx0, tz0, tx1, tz1 = ZONES[zone]
    fout = []
    for naam in sorted(os.listdir(TEGELS)):
        if not naam.startswith("t_") or not naam.endswith(".nbt"):
            continue
        tx, tz = map(int, naam[2:-4].split("_"))
        if not (tx0 <= tx <= tx1 and tz0 <= tz <= tz1):
            continue
        pal, (xs, ys, zs, st) = TL.lees(os.path.join(TEGELS, naam))
        deco = np.array([is_deco(s) for s in pal] + [False], bool)
        vol = np.full((TL.T, TL.HOOG, TL.T), -1, np.int32)
        vol[xs, ys, zs] = st
        m = deco[st]
        for x, y, z, s in zip(xs[m], ys[m], zs[m], st[m]):
            if y > 0 and vol[x, y - 1, z] < 0:
                soort = "zweeft"
                if any(h in pal[s] for h in HANGERS) and y + 1 < TL.HOOG and vol[x, y + 1, z] >= 0:
                    soort = "hangt (zet hanging=true)"
                fout.append((tx * 64 + int(x), int(y), tz * 64 + int(z), pal[s], soort))
    per = defaultdict(int)
    for f in fout:
        per[(f[4], f[3].split("[")[0])] += 1
    print(f"zone {zone}: {len(fout)} zwevende decoblokken")
    for k, n in sorted(per.items(), key=lambda kv: -kv[1]):
        print(f"  {n:5d} {k[0]:26s} {k[1]}")
    for f in [f for f in fout if f[4] == "zweeft"][:maxn]:
        print("   ", f[0], f[1], f[2], f[3])
    return sum(1 for f in fout if f[4] == "zweeft")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--zone", default="alle")
    ap.add_argument("--max", type=int, default=40)
    a = ap.parse_args()
    n = 0
    for z in (ZONES if a.zone == "alle" else [a.zone]):
        n += check(z, a.max)
    sys.exit(1 if n else 0)   # (n = floating only; the hanging ones are a warning)


if __name__ == "__main__":
    main()
