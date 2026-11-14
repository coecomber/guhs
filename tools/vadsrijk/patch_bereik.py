"""1.10.3: survival reachability of the Spiegelburcht's gated rooms with ALL doors closed, on the map tiles, optionally with a
world patch (data/guhs/vadsrijk/patch/<naam>.json, /guhs vadsrijk patch) laid over them.

Uses the bereikcheck.py player model (walk / step / drop / swim / climb / gap jumps). The front door (WereldDeuren
"spiegelburcht"), the four puzzle gates poort_1..4 and so the arena door are treated as CLOSED, the flood starts at the foot of
the grand stair and the forecourt. A room reached anyway has a hole. Run from the project root:

    python tools/vadsrijk/patch_bereik.py                       # tiles as they are (before 1.10.3: the Spiegelzaal leaks)
    python tools/vadsrijk/patch_bereik.py --patch spiegelzaal   # with the patch boxes as solid blocks
    python tools/vadsrijk/patch_bereik.py --pad                 # + the shortest way in (debug)

Exit code 1 when a gated room is reached.
"""
import argparse
import json
import os
import sys

import numpy as np
from scipy import sparse
from scipy.sparse import csgraph

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
sys.path.insert(0, HIER)
import bereikcheck as B  # noqa: E402

PATCHES = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk", "patch")
Y = 176                      # floor stand of the burcht
HAL = (854, -1397, 22)       # arena_spiegelzucht, radius (bouw2: PL.SPIEGELZAAL)
DX, DZ = 854, 1566           # bouw2 v1 castle coordinates -> world (Z2WereldPlekken.BURCHT_DX/DZ)
DICHT = [((-2 + DX, Y, z + DZ), (2 + DX, Y + 4, z + DZ)) for z in (-2880, -2900, -2918, -2938)] \
    + [((-3 + DX, Y, -2856 + DZ), (3 + DX, Y + 8, -2856 + DZ))]
KAMERS = {   # (x0, z0, x1, z1) v1, plan.BURCHT_KAMERS
    "voorhal": (-16, -2879, 16, -2857), "lichtkamer": (-14, -2899, 14, -2881),
    "spiegelgang": (-9, -2917, 9, -2901), "prismazaal": (-14, -2937, 14, -2919),
}
BRONNEN = [(854, 137, -1217), (854, 176, -1267)]   # burcht_trap_voet, burcht_voorplein
TEGELS = (11, 15, -25, -18)


def laad(extra):
    """The tiles around the burcht, with DICHT + extra boxes set solid (bedrock)."""
    orig = B.TL.lees

    def lees(pad):
        pal, (xs, ys, zs, st) = orig(pad)
        tx, tz = (int(v) for v in os.path.basename(pad)[2:-4].split("_"))
        X0, Z0 = tx * 64, tz * 64
        pal = list(pal) + ["minecraft:bedrock"]
        ax, ay, az, ast = [xs], [ys], [zs], [st]
        for mn, mx in DICHT + extra:
            x0, x1 = max(mn[0] - X0, 0), min(mx[0] - X0, 63)
            z0, z1 = max(mn[2] - Z0, 0), min(mx[2] - Z0, 63)
            if x0 > x1 or z0 > z1:
                continue
            g = np.mgrid[x0:x1 + 1, mn[1]:mx[1] + 1, z0:z1 + 1].reshape(3, -1)
            ax.append(g[0]); ay.append(g[1]); az.append(g[2]); ast.append(np.full(g.shape[1], len(pal) - 1))
        return pal, tuple(np.concatenate(a).astype(r.dtype) for a, r in zip((ax, ay, az, ast), (xs, ys, zs, st)))

    B.TL.lees = lees
    B.deur_dozen = lambda: []          # the moving front door stays as it is in the tile: closed
    try:
        return B.laad(lambda tx, tz: TEGELS[0] <= tx <= TEGELS[1] and TEGELS[2] <= tz <= TEGELS[3])
    finally:
        B.TL.lees = orig


def kamer_van(K, i):
    x, z, y, top = int(K.x[i]), int(K.z[i]), K.F[i] / 2, int(K.t[i])
    if y < Y:
        return None
    hx, hz, hr = HAL
    r = ((x - hx) ** 2 + (z - hz) ** 2) ** 0.5
    if r <= hr and (y <= Y + 14 or (r / hr) ** 2 + ((y - Y - 14) / (hr - 2)) ** 2 <= 1):
        return "spiegelzaal"
    for naam, (x0, z0, x1, z1) in KAMERS.items():
        if x0 + DX <= x <= x1 + DX and z0 + DZ <= z <= z1 + DZ and y <= Y + 16:
            return naam
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--patch", default=None, help="patch name (data/guhs/vadsrijk/patch/<naam>.json) or a json path")
    ap.add_argument("--pad", action="store_true")
    a = ap.parse_args()
    extra = []
    if a.patch:
        pad = a.patch if a.patch.endswith(".json") else os.path.join(PATCHES, a.patch + ".json")
        for d in json.load(open(pad, encoding="utf-8"))["dozen"]:
            mn = tuple(min(p, q) for p, q in zip(d["min"], d["max"]))
            mx = tuple(max(p, q) for p, q in zip(d["min"], d["max"]))
            extra.append((mn, mx))
        print(f"patch {pad}: {len(extra)} dozen")
    K = laad(extra)
    s1, d1 = B.randen(K)
    s2, d2 = B.sprongen(K)
    s, d = np.concatenate([s1, s2]), np.concatenate([d1, d2])
    G = sparse.csr_matrix((np.ones(len(s), np.int8), (s, d)), shape=(K.N, K.N))
    Z = B.Zoeker(K)
    bron = sorted(set(i for p in BRONNEN for i in Z.rond(*p, r=2)))
    N = K.N
    G2 = sparse.bmat([[G, None], [None, sparse.csr_matrix((1, 1), dtype=np.int8)]], format="csr") \
        + sparse.csr_matrix((np.ones(len(bron), np.int8), (np.full(len(bron), N), bron)), shape=(N + 1, N + 1))
    order, pred = csgraph.breadth_first_order(G2, N, directed=True, return_predecessors=True)
    R = np.zeros(N, bool)
    R[order[order < N]] = True
    lek = 0
    for naam in list(KAMERS) + ["spiegelzaal"]:
        alle = [i for i in np.nonzero(K.geldig)[0] if kamer_van(K, i) == naam]
        hit = [i for i in alle if R[i]]
        lek += len(hit)
        print(f"  {naam:12s} bereikt zonder deuren: {len(hit):5d} / {len(alle)} stand-cellen" + ("   LEK" if hit else ""))
        if hit and a.pad:
            t = hit[0]
            weg = []
            while 0 <= t < N:
                weg.append(t)
                t = pred[t]
            for i in reversed(weg[:40]):
                print(f"      {K.x[i]} {K.F[i] / 2} {K.z[i]}")
    print("DICHT" if lek == 0 else "LEK: een kamer is zonder deur te bereiken")
    sys.exit(1 if lek else 0)


if __name__ == "__main__":
    main()
