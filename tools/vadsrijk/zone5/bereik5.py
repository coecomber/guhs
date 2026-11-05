"""Debug helper (T-Z5WERELD): which published spots can you WALK to from the airport station on the local zone-5 world, and for
the others: the nearest reachable stand cell and what blocks the way (height step). Simpler than loopcheck (4-neighbour walk,
step up <= 1, fall <= 4, stairs/slabs count as their top), but it says WHERE to fix.
    python tools/vadsrijk/zone5/bereik5.py [id ...]
Reads tools/vadsrijk/_cache/z5/wereld.npz + plekken_z5.json (game coordinates, turned back to local).
"""
import collections
import json
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
sys.path.insert(0, os.path.join(HIER, ".."))
import plan5 as PL  # noqa: E402
import loopcheck as LC  # noqa: E402


def laad():
    d = np.load(os.path.join(HIER, "..", "_cache", "z5", "wereld.npz"))
    v, pal, b = d["v"], [str(s) for s in d["pal"]], d["b"]
    kl = []
    for s in pal:
        naam, _, rest = s.partition("[")
        props = dict(kv.split("=", 1) for kv in rest.rstrip("]").split(",") if "=" in kv) if rest else {}
        kl.append(LC.PASS if naam in ("minecraft:air", "minecraft:cave_air") else LC.klasse(naam, props))
    kl = np.array(kl, np.int8)
    water = np.array([s.startswith("minecraft:water") for s in pal])
    return v, kl, water, b


def main():
    v, kl, water, b = laad()
    C = kl[v]
    vast = np.isin(C, (LC.SOLID, LC.HALF, LC.STAIR, LC.DIRT_PATH)) & ~water[v]
    vrij = (C == LC.PASS) | (C == LC.CLIMB)
    st = np.zeros_like(vast)
    st[:, 1:-1, :] = vast[:, :-2, :] & vrij[:, 1:-1, :] & vrij[:, 2:, :]
    half = np.isin(C, (LC.HALF, LC.STAIR))
    xs, ys, zs = np.nonzero(st)
    hoogte = ys - 1 + np.where(half[xs, ys - 1, zs], 0.5, 1.0)   # top of the floor
    idx = {}
    for n, (x, y, z) in enumerate(zip(xs, ys, zs)):
        idx.setdefault((x, z), []).append((y, hoogte[n]))
    with open(os.path.join(HIER, "..", "_cache", "plekken_z5.json"), encoding="utf-8") as f:
        alle = json.load(f)

    def lok(p):
        x, y, z = PL.lokaal(*p)
        return int(x - b[0]), int(y - b[1]), int(z - b[2])
    start = [lok(alle["luchthaven_station"]), lok(alle["kathedraal_plein"])]
    seen = set()
    dq = collections.deque()
    for (x, y, z) in start:
        for (yy, h) in idx.get((x, z), []):
            if abs(yy - y) <= 2:
                seen.add((x, yy, z))
                dq.append((x, yy, z, h))
    while dq:
        x, y, z, h = dq.popleft()
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for (yy, hh) in idx.get((x + dx, z + dz), []):
                if (x + dx, yy, z + dz) in seen:
                    continue
                if -4.0 <= hh - h <= 1.0:
                    seen.add((x + dx, yy, z + dz))
                    dq.append((x + dx, yy, z + dz, hh))
    print(f"bereikbaar: {len(seen)} stacellen van {len(xs)}")
    wil = sys.argv[1:] or sorted(alle)
    arr = np.array(list(seen))
    for k in wil:
        if k not in alle or k.startswith(("z5w_buikgangen", "z5w_schuilnest_m")):
            continue
        x, y, z = lok(alle[k])
        if (x, y, z) in seen:
            continue
        d = np.abs(arr[:, 0] - x) + np.abs(arr[:, 2] - z) + 2 * np.abs(arr[:, 1] - y)
        i = int(np.argmin(d))
        nx, ny, nz = arr[i]
        g = PL.spel(nx + b[0], ny + b[1], nz + b[2])
        print(f"{k:34s} NIET bereikbaar; dichtstbij {g} (lokaal {int(nx + b[0])},{int(ny + b[1])},{int(nz + b[2])}), "
              f"afstand {int(d[i])}, hoogte {int(ny - y):+d}")


if __name__ == "__main__":
    main()
