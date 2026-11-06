"""Walk check of Rozenkloof (rozenkloof fork, U-Z6WERELD): builds the canyon window (land + rozenkloof, no figures) and
runs a BFS over stand cells (loopcheck movement classes): up 1, down up to 3, 4 directions, 2 free cells to stand.
Reports which recorded spots (zijbouw.plek) are not stand spots, not reachable from the Rozenplein, or cannot get back.
python rozenkloof_check.py
"""
import os, sys, time
from collections import deque
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(1, os.path.abspath(os.path.join(HERE, "..", "..")))
sys.path.insert(2, os.path.abspath(os.path.join(HERE, "..")))
import hoogkit  # noqa
import schemerkit as SK
import zijwereld as ZW
import rozenkloof as RK
import loopcheck as LC

GEEN_SNAP = ("haven_6_5_luchtschip", "haven_6_5_mast")


def main():
    t = time.time()
    W = SK.Wereld(3200, 40, -820, 3590, 210, -625, 1.0)
    ZW.bouw_land(W)
    RK.bouw(W, figuren=False)
    print("gebouwd %.0fs" % (time.time() - t), W.v.shape)
    kl = []
    for s in W.pal:
        naam, _, rest = s.partition("[")
        props = dict(kv.split("=", 1) for kv in rest.rstrip("]").split(",") if "=" in kv) if rest else {}
        if naam in ("minecraft:air", "minecraft:cave_air"):
            kl.append(LC.PASS)
        elif naam.endswith("_hek") or naam.endswith("_tralies"):
            kl.append(LC.FENCE)
        elif naam.startswith("minecraft:water"):
            kl.append(9)
        else:
            kl.append(LC.klasse(naam, props))
    kl = np.array(kl, np.int8)
    C = kl[W.v]
    vloer = np.isin(C, (LC.SOLID, LC.HALF, LC.STAIR, LC.DIRT_PATH))
    vrij = C == LC.PASS
    sta = np.zeros_like(vrij)
    sta[:, 1:-1, :] = vloer[:, :-2, :] & vrij[:, 1:-1, :] & vrij[:, 2:, :]
    x1, y1, z1 = (int(v) for v in W.b[:3])
    X, Y, Z = sta.shape

    def buren(i, j, k):
        for di, dk in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            a, c = i + di, k + dk
            if not (0 <= a < X and 0 <= c < Z):
                continue
            for dj in (1, 0, -1, -2, -3):
                b = j + dj
                if 1 <= b < Y - 1 and sta[a, b, c]:
                    if dj == 1 and not vrij[i, j + 2, k]:
                        continue
                    if dj < 0 and not (vrij[a, j:j + 2, c].all()):
                        continue
                    yield a, b, c
                    break

    def bfs(start, terug=False):
        seen = np.zeros_like(sta)
        q = deque([start])
        seen[start] = True
        while q:
            i, j, k = q.popleft()
            for n in buren(i, j, k):
                if terug:
                    pass
                if not seen[n]:
                    seen[n] = True
                    q.append(n)
        return seen

    P = W.plekken
    px, py, pz = P["rozenkloof_plein"]
    start = (px - x1, py - y1, pz - z1)
    print("plein sta:", bool(sta[start]))
    heen = bfs(start)
    print("bereikbaar %d cellen (%.0fs)" % (heen.sum(), time.time() - t))
    mis, geen_sta, niet_terug = [], [], []
    for pid, (x, y, z) in sorted(P.items()):
        if pid in GEEN_SNAP:
            continue
        if y is None:
            continue
        c = (x - x1, y - y1, z - z1)
        if not (0 <= c[0] < X and 0 <= c[1] < Y and 0 <= c[2] < Z):
            mis.append(pid + " (buiten venster)")
            continue
        if not sta[c]:
            # nearest stand cell within 2
            ok = None
            for dj in (0, 1, -1, 2, -2):
                for di in range(-2, 3):
                    for dk in range(-2, 3):
                        cc = (c[0] + di, c[1] + dj, c[2] + dk)
                        if 0 <= cc[1] < Y and sta[cc]:
                            ok = cc
                            break
                    if ok:
                        break
                if ok:
                    break
            geen_sta.append(pid + ("" if ok else " (niets in 2)"))
            if not ok:
                continue
            c = ok
        if not heen[c]:
            mis.append(pid)
            continue
        if not bfs(c)[start]:
            niet_terug.append(pid)
    import plan6
    want = [k for k in plan6.P if k.startswith(("rozenkloof", "haven_6_5", "haven_z6", "kampvuur_rozenkloof", "uitkijk_zeemond"))]
    print("plekken:", len(P), "ONTBREKEND uit plan6:", [k for k in want if k not in P])
    print("GEEN STAPLEK:", geen_sta)
    print("NIET BEREIKBAAR:", mis)
    print("NIET TERUG:", niet_terug)
    print("huizen:", len(getattr(W, "huizen", [])), [h["id"] for h in getattr(W, "huizen", [])])
    nh = 0
    for h in getattr(W, "huizen", []):
        x, y, z = h["binnen"]
        c = (x - x1, y - y1, z - z1)
        if not (0 <= c[1] < Y and heen[c]):
            nh += 1
            print("  huis niet bereikbaar binnen:", h["id"], h["binnen"], "sta" if sta[c] else "geen sta")
    print("rijdieren:", len(getattr(W, "rijdieren", [])), "klaar %.0fs" % (time.time() - t))
    np.save(os.path.join(HERE, "..", "..", "_cache", "rk_heen.npy"), np.packbits(heen)) if False else None


if __name__ == "__main__":
    main()
