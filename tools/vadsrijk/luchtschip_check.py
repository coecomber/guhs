"""T-INT: checks every airship line (Lijnen.java) against the real map tiles: the sampled route between the two "out from the pier"
points must clear the terrain (highest non-air block in a 7x7 column around the path) by at least MIN blocks.

    python tools/vadsrijk/luchtschip_check.py            # report per line; exit code 1 when a line is too low somewhere

Replicates Dienstregeling.punten (dock, 26 out from the pier + 5 up, the via points, 26 out + 4 up, dock) with straight
segments (the game uses a smooth curve through the same points: close enough for a 10-block margin)."""
import math
import os
import re
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
sys.path.insert(0, os.path.join(HIER, "audit"))
import tegellezer as TL  # noqa: E402

T = 64
MIN = 10
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
GEEN = ("air", "barrier", "light", "structure_void")


def pois():
    alle = {}
    for f in ("wereld/WereldPlekken.java", "zone2/wereld/Z2WereldPlekken.java", "zone3/wereld/Z3WereldPlekken.java",
              "zone4/wereld/Z4WereldPlekken.java", "zone5/wereld/Z5WereldPlekken.java"):
        p = os.path.join(JAVA, f)
        if os.path.exists(p):
            src = open(p, encoding="utf-8").read()
            for m in re.finditer(r'Map\.entry\("([a-z0-9_]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)\)', src):
                alle.setdefault(m.group(1), (int(m.group(2)), int(m.group(3)), int(m.group(4))))
    return alle


def lijnen():
    src = open(os.path.join(JAVA, "luchtschip", "Lijnen.java"), encoding="utf-8").read()
    havens = {m.group(1): (m.group(2), (int(m.group(3)), int(m.group(4)), int(m.group(5))), float(m.group(6)))
              for m in re.finditer(r'Haven (\w+) = new Haven\("([a-z0-9_]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\), (-?[\d.]+)f', src)}
    uit = []
    for m in re.finditer(r'Lijn (L\w+) = new Lijn\("([a-z0-9_]+)", "(\w)", (\w+), (\w+), List\.of\((.*?)\), \d+\);', src, re.S):
        via = [tuple(float(v) for v in t) for t in re.findall(r"v\((-?[\d.]+), (-?[\d.]+), (-?[\d.]+)\)", m.group(6))]
        uit.append((m.group(3), m.group(2), havens[m.group(4)], havens[m.group(5)], via))
    return uit


def plek(h, alle):
    """Haven.plek(): the POI, the zone-4 derived spot (T-INT zone4Haven), or the reserve."""
    pid, reserve, richting = h
    u = (-math.sin(math.radians(richting)), math.cos(math.radians(richting)))
    if pid in alle:
        return alle[pid], u
    anker = {"haven_z4_z3_vertrek": "haven_3_4", "haven_z4_z5_vertrek": "haven_4_5"}.get(pid)
    if anker and anker + "_luchtschip" in alle and anker + "_landing" in alle:
        s, la = alle[anker + "_luchtschip"], alle[anker + "_landing"]
        return (math.floor(s[0] - u[0] * 9), la[1], math.floor(s[2] - u[1] * 9)), u
    return reserve, u


class Wereld:
    """Solid blocks per tile as a 3D bool array (y -64..319), read lazily."""

    def __init__(self):
        self.paden = {(tx, tz): pad for tx, tz, pad in TL.tegels(TEGELS)}
        self.cache = {}

    def tegel(self, tx, tz):
        if (tx, tz) not in self.cache:
            g = None
            pad = self.paden.get((tx, tz))
            if pad:
                pal, (xs, ys, zs, st) = TL.lees(pad)
                lucht = np.array([any(w in str(q) for w in GEEN) for q in pal])
                m = ~lucht[st]
                g = np.zeros((T, 384, T), bool)
                g[xs[m], ys[m] + 64, zs[m]] = True
            self.cache[(tx, tz)] = g
        return self.cache[(tx, tz)]

    def onder(self, x, y, z):
        """Free blocks below (x, y, z) down to the first solid one within 64 (64 = nothing)."""
        g = self.tegel(x // T, z // T)
        if g is None:
            return 64
        kol = g[x % T, :, z % T]
        for d in range(0, 64):
            yy = y - d + 64
            if 0 <= yy < 384 and kol[yy]:
                return d
        return 64

    def boven(self, x, y, z, h):
        """Is anything solid in (x, y..y+h, z)?"""
        g = self.tegel(x // T, z // T)
        if g is None:
            return False
        lo, hi = max(0, y + 64), min(384, y + h + 64 + 1)
        return bool(g[x % T, lo:hi, z % T].any())


def main():
    alle = pois()
    w = Wereld()
    slecht = 0
    for letter, lid, a, b, via in lijnen():
        (pa, ua), (pb, ub) = plek(a, alle), plek(b, alle)
        da = (pa[0] + 0.5 + ua[0] * 9, pa[1], pa[2] + 0.5 + ua[1] * 9)
        db = (pb[0] + 0.5 + ub[0] * 9, pb[1], pb[2] + 0.5 + ub[1] * 9)
        pts = [(da[0] + ua[0] * 26, da[1] + 5, da[2] + ua[1] * 26)] + via + [(db[0] + ub[0] * 26, db[1] + 4, db[2] + ub[1] * 26)]
        laagst, waar = 1e9, None
        for p, q in zip(pts, pts[1:]):
            n = max(1, int(math.dist(p, q) / 2))
            for i in range(n + 1):
                t = i / n
                x, y, z = (p[k] + (q[k] - p[k]) * t for k in range(3))
                xi, yi, zi = int(math.floor(x)), int(math.floor(y)), int(math.floor(z))
                vrij = min(w.onder(xi + dx, yi, zi + dz) for dx in range(-3, 4, 3) for dz in range(-3, 4, 3))
                if any(w.boven(xi + dx, yi, zi + dz, 8) for dx in range(-3, 4, 3) for dz in range(-3, 4, 3)):
                    vrij = -1   # (something in the ship's own box)
                if vrij < laagst:
                    laagst, waar = vrij, (round(x), round(y), round(z), yi - vrij)
        ok = laagst >= MIN
        slecht += not ok
        print(f"lijn {letter} ({lid}): {a[0]} {pa} -> {b[0]} {pb}: laagste vrije ruimte {laagst:.0f} blokken bij {waar[:3]} "
              f"(grond {waar[3]}) {'OK' if ok else 'TE LAAG'}")
    return 1 if slecht else 0


if __name__ == "__main__":
    sys.exit(main())
