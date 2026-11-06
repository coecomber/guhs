"""U-RUITERS: the routes of the ambient riding guhs (a guh on a Pluimguh), planned on the REAL map tiles.

Usage (project root):  python tools/vadsrijk/ruiters/routes.py .
Writes  src/main/resources/data/guhs/vadsrijk_ruiters/routes.txt  (read by feature/vadsrijk/ruiters/RuiterRoutes.java).

How a route is made:
  1. anchors: from the zone-3 generator's own geometry (the riding spiral rij_punt_lev, the inner stair trap_lev, the Honingtak
     ring, the ground ring path r 172, the Pluimstal training oval) or POI ids (POI_Z3.md / POI_VADSRIJK.md) or the caravan road;
  2. every anchor is snapped to the nearest real stand cell (floor below, 3 blocks of air: a Pluimguh + its rider fit);
  3. A* between consecutive anchors over stand cells: steps of at most 1 block up or down (half slabs count as half), no doors,
     no water, prefers path blocks and the middle of a path (cells next to a wall cost more), diagonals only when both sides are free;
  4. Douglas-Peucker in 3D (tolerance 0.35): the rider moves in straight lines between the kept points, so it never leaves the
     walkable cells by more than that; y between points is linear (half steps = a smooth ramp).
Modes: HEENWEER (back and forth), RONDJE (a closed loop), STAL (fixed spots: index 0 = rider idling, 1 = groomed + groomer).
Run again whenever the zone-1 / zone-3 map changes (integration regenerates the tiles); the gametest checks the file.
"""
import heapq
import math
import os
import re
import sys

ROOT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else ".")
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk", "wereld"))
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk", "zone3"))
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk", "zone3", "kit"))
import nbtsnel  # noqa: E402

TEG = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk_ruiters", "routes.txt")
CACHE = {}


def tegel(tx, tz):
    k = (tx, tz)
    if k not in CACHE:
        pad = os.path.join(TEG, f"t_{tx}_{tz}.nbt")
        d = {}
        if os.path.exists(pad):
            root = nbtsnel.lees(pad)
            pal = [p["Name"] + ("[" + ",".join(f"{a}={b}" for a, b in sorted(p.get("Properties", {}).items())) + "]" if p.get("Properties") else "")
                   for p in root["palette"]]
            for b in root["blocks"]:
                x, y, z = b["pos"]
                d[(x, y, z)] = pal[b["state"]]
        CACHE[k] = d
    return CACHE[k]


def blok(x, y, z):
    tx, tz = x // 64, z // 64
    return tegel(tx, tz).get((x - tx * 64, y, z - tz * 64), "minecraft:air")


DOOR = ("_door", "_trapdoor", "deur", "_gate", "luik")
LUCHTIG = ("air", "grass", "fern", "flower", "tulip", "poppy", "dandelion", "orchid", "allium", "bluet", "daisy", "torch", "carpet",
           "sapling", "mushroom", "button", "lantern", "sign", "rail", "pressure_plate", "vine", "snow[layers=1", "short_",
           "tall_grass", "bush", "petals", "lily", "dead_bush", "candle", "hanging_moss", "lampion")


def naam(b):
    return b.split(":", 1)[1].split("[")[0]


def vrij(b):
    """A rider can pass through it (air, plants, small lights). Never doors, gates, water, fences."""
    n = naam(b)
    if "water" in n or "lava" in n or any(d in n for d in DOOR) or "fence" in n or "_wall" in n:
        return False
    return any(w in n for w in LUCHTIG) and "mushroom_block" not in n


def vloer(b):
    n = naam(b)
    return not vrij(b) and "water" not in n and "lava" not in n and "fence" not in n and "_wall" not in n \
        and not any(d in n for d in DOOR) and "campfire" not in n


def onderslab(b):
    return "_slab" in b and "type=bottom" in b


HOOGTE = {}


def stand(x, y, z):
    """Stand height of cell (x, y, z) or None. A bottom slab IN the cell = stand y + 0.5."""
    k = (x, y, z)
    if k in HOOGTE:
        return HOOGTE[k]
    b0 = blok(x, y, z)
    h = None
    if onderslab(b0):
        if all(vrij(blok(x, y + d, z)) for d in (1, 2, 3)):
            h = y + 0.5
    elif vrij(b0) and vloer(blok(x, y - 1, z)) and vrij(blok(x, y + 1, z)) and vrij(blok(x, y + 2, z)):
        h = y
    HOOGTE[k] = h
    return h


PADBLOK = ("path", "plank", "slab", "stair", "terracotta", "klinker", "brick", "cobble", "gravel", "stone", "tegel", "packed_mud",
           "wood", "log", "concrete", "tuff", "deepslate", "polished", "honeycomb", "smooth")
NATUUR = ("grass_block", "moss", "dirt", "sand", "podzol", "mycelium", "mud", "clay", "snow", "vadsmos", "glimmos", "knuffelgras")


def kosten(x, y, z, h):
    b = blok(x, int(math.floor(h)) - (0 if h % 1 else 1), z)
    n = naam(b)
    c = 0.0
    if "leaves" in n:
        c += 2.5
    elif any(p in n for p in PADBLOK) and "mossy" not in n:
        c += 0.0
    elif any(p in n for p in NATUUR):
        c += 0.7
    else:
        c += 0.4
    hy = int(math.floor(h))
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):   # walls / railings next to the cell: keep to the middle
        if not vrij(blok(x + dx, hy + 1, z + dz)) or not vrij(blok(x + dx, hy, z + dz)):
            c += 1.1
    return c


def buren(x, y, z, h):
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
        for dy in (0, 1, -1, -2):
            ny = y + dy
            h2 = stand(x + dx, ny, z + dz)
            if h2 is None or abs(h2 - h) > 1.0:
                continue
            if dx and dz:   # diagonal: both sides walkable at about this height
                ok = True
                for sx, sz in ((x + dx, z), (x, z + dz)):
                    if not any(stand(sx, yy, sz) is not None and abs(stand(sx, yy, sz) - h) <= 1.0 for yy in (y - 1, y, y + 1)):
                        ok = False
                if not ok:
                    break
            yield (x + dx, ny, z + dz), h2, (1.4142 if dx and dz else 1.0)
            break


def snap(p, r=4, ydev=4):
    """The nearest stand cell to a point (x, y, z floats)."""
    px, py, pz = p
    beste, bd = None, 1e9
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            for dy in range(-ydev, ydev + 1):
                x, y, z = int(math.floor(px)) + dx, int(math.floor(py)) + dy, int(math.floor(pz)) + dz
                h = stand(x, y, z)
                if h is None:
                    continue
                d = (x + 0.5 - px) ** 2 + (z + 0.5 - pz) ** 2 + 2 * (h - py) ** 2 + kosten(x, y, z, h) * 0.5
                if d < bd:
                    beste, bd = (x, y, z), d
    return beste


def grondsnap(x, z, y0=110, y1=40):
    """Highest stand cell in a column (for anchors without a y)."""
    for y in range(y0, y1, -1):
        if stand(int(x), y, int(z)) is not None:
            return (int(x), y, int(z))
    return None


def astar(a, b, marge=24, ylo=None, yhi=None, max_n=400000):
    ha, hb = stand(*a), stand(*b)
    x0, x1 = min(a[0], b[0]) - marge, max(a[0], b[0]) + marge
    z0, z1 = min(a[2], b[2]) - marge, max(a[2], b[2]) + marge
    ylo = min(ha, hb) - 6 if ylo is None else ylo
    yhi = max(ha, hb) + 6 if yhi is None else yhi
    open_ = [(0.0, 0.0, a)]
    van = {a: None}
    g = {a: 0.0}
    n = 0
    while open_ and n < max_n:
        f, gc, c = heapq.heappop(open_)
        if c == b:
            pad = []
            while c is not None:
                pad.append(c)
                c = van[c]
            return pad[::-1]
        if gc > g.get(c, 1e18):
            continue
        n += 1
        h = stand(*c)
        for nb, h2, d in buren(c[0], c[1], c[2], h):
            if not (x0 <= nb[0] <= x1 and z0 <= nb[2] <= z1 and ylo <= h2 <= yhi):
                continue
            ng = gc + d + abs(h2 - h) * 0.6 + kosten(nb[0], nb[1], nb[2], h2)
            if ng < g.get(nb, 1e18):
                g[nb] = ng
                van[nb] = c
                hh = math.sqrt((nb[0] - b[0]) ** 2 + (nb[2] - b[2]) ** 2 + (h2 - stand(*b)) ** 2)
                heapq.heappush(open_, (ng + hh * 1.15, ng, nb))
    return None


def dp(pts, tol=0.35):
    if len(pts) < 3:
        return pts
    a, b = pts[0], pts[-1]
    ab = [b[i] - a[i] for i in range(3)]
    l2 = sum(v * v for v in ab) or 1e-9
    best, bi = -1, 0
    for i in range(1, len(pts) - 1):
        p = pts[i]
        t = max(0, min(1, sum((p[k] - a[k]) * ab[k] for k in range(3)) / l2))
        d = math.sqrt(sum((p[k] - a[k] - t * ab[k]) ** 2 for k in range(3)))
        if d > best:
            best, bi = d, i
    if best <= tol:
        return [a, b]
    return dp(pts[:bi + 1], tol)[:-1] + dp(pts[bi:], tol)


def lees_poi(bestand):
    out = {}
    for line in open(os.path.join(ROOT, bestand), encoding="utf-8"):
        m = re.match(r"\| `([^`]+)` \| (-?\d+), (-?\d+), (-?\d+) \|", line)
        if m:
            out[m.group(1)] = (int(m.group(2)), int(m.group(3)), int(m.group(4)))
    return out


# ================================================================================================ the routes
def routes():
    import plan3 as P3
    import boom2 as B
    TX, TZ = P3.TX_STAM, P3.TZ_STAM
    Z3 = lees_poi("POI_Z3.md")
    Z1 = lees_poi("POI_VADSRIJK.md")
    R = []   # (id, zone, mode, riders, speed (blocks/tick on the flat), [anchors (x, y, z) or None-y], marge, ylo, yhi)

    def w(x, y, z):
        return (TX + x, y, TZ + z)

    # 1. Het Rijpad: the riding spiral round the outside, foot (82) -> Honingtak (166) -> kruin 1 (232)
    eind = (B.RIJ["eind"] - B.RIJ["y0"]) / B.RIJ["stijg"] - 0.03
    t, a = 0.04, []
    while t <= eind:
        th = B.RIJ["th0"] + 2 * math.pi * t
        x, y, z = P3.rij_punt_lev(int(math.floor(t)), th, 0.45)
        a.append(w(x, y, z))
        t += 0.0025   # ~0.7 blocks
    R.append(("z3_rijpad", 3, "HEENWEER", 3, 0.28, a[::40], 10, None, None, a))

    # 2. the inner spiral stair: Schorsstad floor (80) -> Honingtak (166) -> kruin 1 (232)
    import numpy as np
    kn = B._KNOPEN
    phi, eind, a = kn[0][0] + B.TRAP_WL / 2, kn[2][0] + B.TRAP_WL / 2, []
    while phi <= eind + 1e-6:
        lev = float(B.trap_lev(np.array([phi]))[0])
        th = B.TRAP_TH0 + phi
        a.append(w(20 * math.cos(th), float(B.stand(lev)), 20 * math.sin(th)))
        phi += 0.035   # ~0.7 blocks at r 20
    R.append(("z3_wenteltrap", 3, "HEENWEER", 2, 0.2, a[::40], 6, None, None, a))

    # 3. Honingtak: a loop on the deck round the trunk (the ring street r ~58)
    a = []
    for deg in range(0, 360, 8):
        th = math.radians(deg)
        a.append(w(P3.P.RING_R * math.cos(th), 167, P3.P.RING_R * math.sin(th)))
    a.append(a[0])
    R.append(("z3_honingtak", 3, "RONDJE", 2, 0.18, a, 10, 163, 172))

    # 4. branch paths from Honingtak to the beekeeper villages
    # (the Bijentak villages themselves are up a ladder: the rider goes as far as the Lianenbrug)
    R.append(("z3_tak_lianenbrug", 3, "HEENWEER", 1, 0.22, [Z3["plein"], Z3["lianenbrug"]], 60, 160, 215))
    R.append(("z3_tak_westtak", 3, "HEENWEER", 1, 0.22, [Z3["respawn"], Z3["pluimpost_westtak"], Z3["kampvuur_tak_west_62"]], 40, 160, 220))
    R.append(("z3_tak_molenhoek", 3, "HEENWEER", 1, 0.22, [Z3["plein"], Z3["molenhoek_plein"]], 50, 160, 215))

    # 5. the Pluimstal: two at the stable (one rider idling, one being groomed) + laps on the training oval
    R.append(("z3_pluimstal", 3, "STAL", 2, 0.0, [Z3["gebouw.pluimstal.deur"], Z3["pluimstal_keuze"]], 4, None, None))
    sx, sz = P3.PLUIMSTAL
    sa = math.atan2(sz, sx)
    ox, oz, tx_, tz_ = math.cos(sa), math.sin(sa), -math.sin(sa), math.cos(sa)
    ex, ez = sx + ox * 36 + tx_ * 20, sz + oz * 36 + tz_ * 20
    a = []
    for deg in range(0, 360, 12):   # (the east end of the oval is cut by a big root: the loop rounds it via a[3:28])
        b = math.radians(deg)
        x, z = ex + 13.5 * 1.6 * math.cos(b), ez + 13.5 * math.sin(b)
        a.append(w(x, P3.gstand(x, z), z))
    a.append(a[0])
    R.append(("z3_oefenbaan", 3, "RONDJE", 2, 0.3, a[3:28], 20, 60, 85))

    # 6. the ground under the tree: the ring path (r 172) + two roads through the Wortelvelden
    a = []
    for deg in range(0, 360, 5):
        th = math.radians(deg)
        x, z = P3.WORTELWEG_R * math.cos(th), P3.WORTELWEG_R * math.sin(th)
        a.append(w(x, P3.gstand(x, z), z))
    a.append(a[0])
    R.append(("z3_wortelweg", 3, "RONDJE", 3, 0.28, a, 14, 50, 100))
    R.append(("z3_stalweg", 3, "HEENWEER", 1, 0.26, [Z3["rijpad_start"], Z3["gebouw.pluimstal.deur"], Z3["kampvuur_wortelweg_5"]],
              60, 55, 100))
    R.append(("z3_schimmelpad", 3, "HEENWEER", 1, 0.26, [Z3["kampvuur_wortelweg_4"], (1241, None, -576, 76, 55), Z3["kampvuur_wortelweg_3"]],
              40, 50, 90))

    # zone 1: a courier on the main road Haven -> Zwamzoet, and a rider on the forest road Zwamzoet -> Sporenwoud (caravan road)
    hoeken = [(90, 65, 466), (93, 65, 434), (93, 65, 412), (95, 65, 410), (94, 66, 394), (97, 66, 380), (96, 64, 332), (99, 66, 302),
              (96, 64, 266), (93, 64, 262), (91, 64, 229), (93, 64, 216), (103, 64, 192), (103, 64, 162), (128, 64, 137), (131, 64, 131),
              (126, 64, 113), (123, 64, 109), (123, 64, 87), (109, 64, 84), (36, 64, 84), (19, 64, 67), (8, 64, 66), (1, 64, 70),
              (-3, 64, 70), (-17, 64, 56), (-17, 65, 53), (-24, 65, 46), (-32, 66, 46), (-58, 65, 21), (-141, 65, 20), (-190, 65, 31),
              (-198, 65, 35), (-220, 65, 14)]
    R.append(("z1_koerier", 1, "HEENWEER", 1, 0.28, hoeken[:24] + [Z1["plein"]], 16, 55, 80))
    R.append(("z1_hoofdweg", 1, "HEENWEER", 1, 0.26, hoeken[23:], 16, 55, 80))
    return R


def lichaam_vrij(x, h, z):
    """The rider's body (0.7 to each side, 1..2.6 above the feet) is free at (x, h, z); railings count at feet height too."""
    hy = int(math.floor(h))
    for ox, oz in ((0.6, 0.6), (0.6, -0.6), (-0.6, 0.6), (-0.6, -0.6)):
        cx, cz = int(math.floor(x + ox)), int(math.floor(z + oz))
        for yy in (hy + 1, hy + 2):
            if not vrij(blok(cx, yy, cz)):
                return False
        n = naam(blok(cx, hy, cz))
        if "fence" in n or "_wall" in n or "bars" in n or "chain" in n or any(d in n for d in DOOR):
            return False
    return True


def zicht(p, q, maxlen=18.0):
    """A straight ride from p to q stays on walkable cells, with y linear in between (within 0.6 of the real stand height)."""
    L = math.hypot(q[0] - p[0], q[2] - p[2])
    if L > maxlen:
        return False
    n = max(2, int(L / 0.3))
    for k in range(n + 1):
        t = k / n
        x, y, z = p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, p[2] + (q[2] - p[2]) * t
        cx, cz = int(math.floor(x)), int(math.floor(z))
        if not any((h := stand(cx, yy, cz)) is not None and abs(h - y) <= 0.6 for yy in range(int(math.floor(y)) - 1, int(math.floor(y)) + 2)):
            return False
        if not lichaam_vrij(x, y, z):
            return False
    return True


def trek(pts):
    """String pulling: keep a point only where the straight line has to bend."""
    if len(pts) < 3:
        return pts
    uit, i = [pts[0]], 0
    while i < len(pts) - 1:
        j = i + 1
        while j + 1 < len(pts) and zicht(pts[i], pts[j + 1]):
            j += 1
        uit.append(pts[j])
        i = j
    return uit


def keten(rid, cellen, marge, ylo, yhi, fout):
    """A* through the anchor cells. An anchor that can't be reached is skipped; when the chain gets stuck on an anchor (a dead
    end), it backs up one anchor and goes round it; an isolated first anchor is dropped."""
    pad, stapels, mis = [cellen[0]], [1], 0
    for c in cellen[1:]:
        stuk = astar(pad[-1], c, marge, ylo, yhi)
        if stuk is None and len(stapels) > 1:   # back up one anchor
            terug = pad[:stapels[-2]]
            stuk2 = astar(terug[-1], c, marge, ylo, yhi)
            if stuk2 is not None:
                fout.append(f"skipped dead-end anchor {pad[-1]}")
                pad, stapels = terug, stapels[:-1]
                stuk = stuk2
        if stuk is None:
            fout.append(f"{pad[-1]} -> {c}")
            mis += 1
            if len(pad) == 1 and mis >= 2:
                pad, stapels, mis = [c], [1], 0
            continue
        pad += stuk[1:]
        stapels.append(len(pad))
        mis = 0
    return pad


def analytisch(rid, punten, fout):
    """Dense analytic points (the spiral / the stair) -> the real stand cells under them."""
    cellen = []
    for (x, y, z) in punten:
        cx, cz = int(math.floor(x)), int(math.floor(z))
        beste = None
        for yy in range(int(math.floor(y)) - 2, int(math.floor(y)) + 3):
            h = stand(cx, yy, cz)
            if h is not None and (beste is None or abs(h - y) < abs(stand(*beste) - y)):
                beste = (cx, yy, cz)
        if beste is None:
            fout.append(f"no floor at {x:.1f},{y:.1f},{z:.1f}")
            continue
        if not cellen or beste != cellen[-1]:
            cellen.append(beste)
    return cellen


def plan(rid, mode, anchors, marge, ylo, yhi, dicht=None):
    fout = []
    if dicht is not None:
        pad = analytisch(rid, dicht, fout)
    else:
        cellen = []
        for p in anchors:
            c = grondsnap(p[0], p[2], *(p[3:] or (110, 40))) if p[1] is None else snap(p)
            if c is None:
                fout.append(f"no stand cell near {p}")
                continue
            if not cellen or c != cellen[-1]:
                cellen.append(c)
        if mode == "STAL":
            return [(c[0] + 0.5, stand(*c), c[2] + 0.5) for c in cellen], fout, mode
        pad = keten(rid, cellen, marge, ylo, yhi, fout)
        if mode == "RONDJE" and pad[-1] != pad[0]:
            stuk = astar(pad[-1], pad[0], marge, ylo, yhi)
            if stuk is None:
                fout.append("loop not closed: back and forth instead")
                mode = "HEENWEER"
            else:
                pad += stuk[1:]
    pts = [(c[0] + 0.5, stand(*c), c[2] + 0.5) for c in pad]
    return trek(pts), fout, mode


def main():
    regels = ["# U-RUITERS: routes of the ambient riding guhs. GENERATED by tools/vadsrijk/ruiters/routes.py from the map tiles.",
              "# route <id> <zone> <mode> <riders> <speed>, then one point per line: x y z (y = stand height, may be .5)"]
    for (rid, zone, mode, n, v, anchors, marge, ylo, yhi, *dicht) in routes():
        pts, fout, mode = plan(rid, mode, anchors, marge, ylo, yhi, dicht[0] if dicht else None)
        lengte = sum(math.dist(pts[i], pts[i + 1]) for i in range(len(pts) - 1))
        steil = max([abs(pts[i + 1][1] - pts[i][1]) / max(0.5, math.hypot(pts[i + 1][0] - pts[i][0], pts[i + 1][2] - pts[i][2]))
                     for i in range(len(pts) - 1)] or [0])
        print(f"{rid:18s} {mode:8s} {len(anchors):4d} anchors -> {len(pts):4d} points, {lengte:7.1f} blocks, steepest {steil:.2f}"
              + (f"  GAPS: {fout}" if fout else ""), flush=True)
        if len(pts) < (1 if mode == "STAL" else 2):
            continue
        regels.append(f"route {rid} {zone} {mode} {n} {v}")
        for p in pts:
            regels.append(f"{p[0]:g} {p[1]:g} {p[2]:g}")
    os.makedirs(os.path.dirname(UIT), exist_ok=True)
    with open(UIT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(regels) + "\n")
    print("written", UIT)


if __name__ == "__main__":
    main()
