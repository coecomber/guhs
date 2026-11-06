"""Debug: print a slice of the LOCAL zone-5 world (after maak5; reads tools/vadsrijk/_cache/z5/wereld.npz).
    python tools/vadsrijk/zone5/kijk5.py z <z> <x1> <x2> <y1> <y2>     # vertical slice at local z (x across, y up)
    python tools/vadsrijk/zone5/kijk5.py x <x> <z1> <z2> <y1> <y2>     # vertical slice at local x (z across)
    python tools/vadsrijk/zone5/kijk5.py y <y> <x1> <x2> <z1> <z2>     # horizontal slice at local y
    add "spel" as the first argument to give GAME coordinates instead (x/z/y turned and lowered like the tiles)
Chars: . air  # solid  _ slab  s stair  | fence/wall  ~ water  H ladder  D door  ^ trapdoor  * glass  % leaves  L light
"""
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import plan5 as PL  # noqa: E402


def teken(naam):
    k = naam.split(":")[-1].split("[")[0]
    if k in ("air", "cave_air"):
        return "."
    if "water" in k:
        return "~"
    if k.endswith(("_slab", "_plaat")):
        return "_"
    if k.endswith(("_stairs", "_trap")):
        return "s"
    if k.endswith(("_fence", "_wall", "_muur", "iron_bars", "_pane", "_chain", "chain")):
        return "|"
    if "ladder" in k:
        return "H"
    if k.endswith(("_door", "_deur")):
        return "D"
    if "trapdoor" in k:
        return "^"
    if "glass" in k or "glas" in k:
        return "*"
    if "leaves" in k:
        return "%"
    if "lantern" in k or "candle" in k:
        return "L"
    if "bed" in k:
        return "b"
    return "#"


def main():
    a = sys.argv[1:]
    spel = a and a[0] == "spel"
    if spel:
        a = a[1:]
    d = np.load(os.path.join(HIER, "..", "_cache", "z5", "wereld.npz"))
    v, pal, b = d["v"], [str(s) for s in d["pal"]], d["b"]
    tk = np.array([teken(s) for s in pal])
    as_, c, u1, u2, y1, y2 = a[0], int(a[1]), int(a[2]), int(a[3]), int(a[4]), int(a[5])

    def get(x, y, z):
        if spel:
            x, y, z = PL.lokaal(x, y, z)
        i, j, k = int(x - b[0]), int(y - b[1]), int(z - b[2])
        if 0 <= i < v.shape[0] and 0 <= j < v.shape[1] and 0 <= k < v.shape[2]:
            return tk[v[i, j, k]]
        return " "
    if as_ in ("z", "x"):
        for y in range(y2, y1 - 1, -1):
            r = "".join(get(u, y, c) if as_ == "z" else get(c, y, u) for u in range(u1, u2 + 1))
            print(f"{y:4d} {r}")
        print("     " + "".join(str(abs(u) % 10) for u in range(u1, u2 + 1)))
    else:
        for z in range(y1, y2 + 1):
            print(f"{z:5d} " + "".join(get(x, c, z) for x in range(u1, u2 + 1)))
        print("      " + "".join(str(abs(u) % 10) for u in range(u1, u2 + 1)))


if __name__ == "__main__":
    main()
