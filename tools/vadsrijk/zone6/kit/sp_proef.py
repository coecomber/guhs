"""Saffraanpoort proof (U-Z6WERELD, Saffraanpoort fork): build the city window (land + Saffraanpoort), then check
  * every recorded plek (zijbouw.plek) of the Saffraanpoort block of plan6 is a stand spot,
  * they are all connected (BFS over stand cells, step <= 1, ladders/water as climb) to the paleisplein and to OUTSIDE all gates.
python sp_proef.py [--npz]     (--npz: save the window to tools/vadsrijk/_cache/z6/sp_venster.npz)
"""
import os, sys, time, math
from collections import deque
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(1, os.path.join(HERE, ".."))
sys.path.insert(2, os.path.join(HERE, "..", ".."))
import hoogkit  # noqa
import schemerkit as SK
import zijwereld as ZW
import saffraanpoort as SP

BOX = (3612, 40, -975, 3912, 175, -715)


def bouw():
    t = time.time()
    W = SK.Wereld(*BOX, 1.0)
    ZW.bouw_land(W)
    SP.bouw(W, figuren=True)
    print("venster", W.v.shape, "%.0f s" % (time.time() - t), "palet", len(W.pal), flush=True)
    return W


def klassen(W):
    import loopcheck as LC
    k, water = [], []
    for s in W.pal:
        naam, _, rest = s.partition("[")
        props = dict(kv.split("=", 1) for kv in rest.rstrip("]").split(",") if "=" in kv) if rest else {}
        k.append(LC.PASS if naam in ("minecraft:air", "minecraft:cave_air") else LC.klasse(naam, props))
        water.append(s.startswith("minecraft:water") or "waterlogged=true" in s)
    return LC, np.array(k, np.int8), np.array(water, bool)


def bfs(W, starts):
    LC, K, WAT = klassen(W)
    c = K[W.v]
    wat = WAT[W.v]
    vast = np.isin(c, (LC.SOLID, LC.HALF, LC.STAIR, LC.DIRT_PATH))
    vrij = (c == LC.PASS) | (c == LC.CLIMB)
    X, Y, Z = W.v.shape
    # stand: floor solid below (or climb/water cell), 2 free cells
    staan = np.zeros_like(vrij)
    staan[:, 1:-1, :] = (vast[:, :-2, :] | (c[:, :-2, :] == LC.CLIMB) | wat[:, :-2, :] | (c[:, 1:-1, :] == LC.CLIMB)) & vrij[:, 1:-1, :] & vrij[:, 2:, :]
    gezien = np.zeros_like(staan)
    q = deque()
    for (x, y, z) in starts:
        i, j, k = W.vx(x), W.vy(y), W.vz(z)
        if staan[i, j, k]:
            gezien[i, j, k] = True
            q.append((i, j, k))
    while q:
        i, j, k = q.popleft()
        for di, dk in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            a, cc = i + di, k + dk
            if not (0 <= a < X and 0 <= cc < Z):
                continue
            for dj in (0, 1, -1, -2, -3):
                b = j + dj
                if not (1 <= b < Y - 2):
                    continue
                if dj == 1 and not vrij[i, j + 2, k]:
                    continue
                if staan[a, b, cc] and not gezien[a, b, cc]:
                    gezien[a, b, cc] = True
                    q.append((a, b, cc))
                    break
                if staan[a, b, cc]:
                    break
        # climbing (ladder / water column)
        for dj in (1, -1):
            b = j + dj
            if 1 <= b < Y - 2 and staan[i, b, k] and not gezien[i, b, k] and (c[i, j, k] == LC.CLIMB or wat[i, j, k] or wat[i, j - 1, k]):
                gezien[i, b, k] = True
                q.append((i, b, k))
    return staan, gezien


def main():
    W = bouw()
    if "--npz" in sys.argv:
        d = os.path.join(HERE, "..", "..", "_cache", "z6")
        os.makedirs(d, exist_ok=True)
        np.savez_compressed(os.path.join(d, "sp_venster.npz"), v=W.v, pal=np.array(W.pal), b=np.array(W.b, float))
    import plan6
    P = plan6.plekken()
    sp_ids = [k for k in P if (3600 <= P[k][0] <= 3920 and -980 <= P[k][2] <= -730)]
    sp_ids = [k for k in sp_ids if not k.startswith(("palmbaken", "duinbaken", "zoutbaken", "geheim", "karavaan_weg", "uitkijk"))]
    rec = getattr(W, "plekken", {})
    mis = [k for k in sp_ids if k not in rec]
    print("plekken vastgelegd:", len(rec), "| plan6-ids in de stad:", len(sp_ids), "| NIET vastgelegd:", mis)
    starts = [rec["saffraanpoort_paleisplein"]]
    staan, gezien = bfs(W, starts)
    slecht, los = [], []
    for k, (x, y, z) in sorted(rec.items()):
        i, j, kk = W.vx(x), W.vy(y), W.vz(z)
        if not staan[i, j, kk]:
            slecht.append((k, (x, y, z), W.wget(x, y - 1, z), W.wget(x, y, z)))
        elif not gezien[i, j, kk]:
            los.append((k, (x, y, z)))
    print("geen stand-plek:", len(slecht))
    for s in slecht:
        print("   ", s)
    print("niet verbonden met het paleisplein:", len(los))
    for s in los:
        print("   ", s)
    for naam, ((gx, gz), kant) in SP.POORTEN.items():
        px, pz = gx + kant[0] * 10, gz + kant[1] * 10
        ok = any(gezien[W.vx(px), W.vy(y), W.vz(pz)] for y in range(68, 76))
        print(f"buiten de {naam}poort ({px}, {pz}) bereikbaar: {ok}")
    print("bereikbare stand-cellen:", int(gezien.sum()), "van", int(staan.sum()))
    for h in getattr(W, "huizen", []):
        x, y, z = h["binnen"]
        if not gezien[W.vx(x), W.vy(y), W.vz(z)]:
            print("   huis NIET bereikbaar:", h["id"], h["binnen"])
    print("ids:", " ".join(sorted(rec)))
    print("huizen met een rol:", [(h["id"], h["rol"]) for h in getattr(W, "huizen", [])])
    print("rijdieren:", len(getattr(W, "rijdieren", [])), "guhs:", len(W.guhs))
    return W, gezien


if __name__ == "__main__":
    main()
