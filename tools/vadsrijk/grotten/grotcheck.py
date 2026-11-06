"""U-GROTTEN: the cave check on the REAL written tiles (run after the map makers, from the project root):

    python tools/vadsrijk/grotten/grotcheck.py [cave id ...]

Per cave:
  * every stand POI (binnen, hal, elite, schat) is standable (floor below, 2 free above);
  * walking (steps of at most half a block = slabs, no jumping) reaches every POI from the entrance; for the sea cave the dry way
    in is the exit chimney ("uitgang"), the dive is checked by hand;
  * no pit: every reachable cell can walk back to the entrance (the walk graph is symmetric for steps <= 0.5 except falls, which
    are counted);
  * the cave is closed: no cave-air cell has the void below it within 6 blocks of its floor (the shell has no holes).
Exit code 1 when something fails.
"""
import math
import os
import sys
from collections import deque

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
sys.path.insert(0, os.path.join(HIER, "..", "audit"))
sys.path.insert(0, HIER)
import tegellezer as TL  # noqa: E402
import grot as GR  # noqa: E402

UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
_C = {}


def blok(x, y, z):
    t = (x // 64, z // 64)
    if t not in _C:
        p = os.path.join(UIT, f"t_{t[0]}_{t[1]}.nbt")
        d = {}
        if os.path.exists(p):
            pal, (xs, ys, zs, st) = TL.lees(p)
            for a, b, c, s in zip(xs.tolist(), ys.tolist(), zs.tolist(), st.tolist()):
                d[(a, b, c)] = pal[s]
        _C[t] = d
    return _C[t].get((x % 64, y, z % 64), "minecraft:air")


DOORLOOP = ("air", "carpet", "grass", "fern", "flower", "tulip", "mushroom[", "glimpaddenstoel", "schimmelpaddenstoel", "snow[layers=1",
            "candle", "lantern", "spore_blossom", "vines", "hanging_roots", "end_rod", "chain", "lily", "nepkoraal", "torch", "bush",
            "petals", "amethyst_cluster", "kristal_cluster", "_bud")


def lucht(b):
    if "water" in b and "waterlogged" not in b:
        return False
    if b in ("minecraft:red_mushroom", "minecraft:brown_mushroom"):
        return True
    return any(w in b for w in DOORLOOP) and "mushroom_block" not in b


def hoogte(x, y, z):
    """Stand height at stand cell y (None: not standable)."""
    o = blok(x, y - 1, z)
    if lucht(o) or "fence" in o or "_wall" in o or ("water" in o and "waterlogged" not in o):
        return None
    if not (lucht(blok(x, y, z)) and lucht(blok(x, y + 1, z))):
        return None
    if "slab" in o or "plaat" in o:
        return y - 0.5 if "type=bottom" in o else y
    if "stairs" in o or "_trap[" in o:
        return y - 0.5
    return float(y)


def stand_y(x, y, z):
    for dy in range(0, 4):
        for yy in (y + dy, y - dy):
            if hoogte(x, yy, z) is not None:
                return yy
    return None


_BINNEN = None


def binnen(c):
    """Is a stand cell part of the cave itself (its footprint or a spiral shaft)? Outside the cave natural terrain counts
    (1-block steps there are the world's, not ours: the strict half-block rule is for the cave)."""
    return _BINNEN(c) if _BINNEN else False


def stap_max(c, max_stap):
    return max_stap if binnen(c) else max(max_stap, 1.0)


def loop(start, doos, max_stap=0.5):
    """BFS over stand cells inside doos (x0, z0, x1, z1, y0, y1). Steps up <= max_stap (cave cells; 1 outside), down <= 3."""
    x0, z0, x1, z1, y0, y1 = doos
    seen = {start: hoogte(*start)}
    q = deque([start])
    vallen = 0
    while q:
        cx, cy, cz = q.popleft()
        h = seen[(cx, cy, cz)]
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = cx + dx, cz + dz
            if not (x0 <= nx <= x1 and z0 <= nz <= z1):
                continue
            for dy in (0, 1, -1, -2, -3):
                ny = cy + dy
                if not y0 <= ny <= y1:
                    continue
                nh = hoogte(nx, ny, nz)
                if nh is None:
                    continue
                if dy < 0 and not all(lucht(blok(nx, yy, nz)) for yy in range(ny, cy + 2)):
                    continue   # (a drop needs the column free from our height down: never through a wall)
                if nh - h > stap_max((nx, ny, nz), max_stap) + 1e-6:
                    break
                if dy > 0 and not lucht(blok(cx, cy + 2, cz)):
                    break
                if h - nh > 1.0:
                    vallen += 1
                if (nx, ny, nz) not in seen:
                    seen[(nx, ny, nz)] = nh
                    q.append((nx, ny, nz))
                break
    return seen, vallen


def terug_graaf(seen, doel):
    """The cells of `seen` from which the goal is reachable (walk steps <= 0.5 up, <= 3 down), by searching backwards."""
    ok = {doel}
    q = deque([doel])
    while q:
        cx, cy, cz = q.popleft()
        h = seen[(cx, cy, cz)]
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1, 2, 3, -2, -3):
                n = (cx + dx, cy + dy, cz + dz)
                if n not in seen or n in ok:
                    continue
                nh = seen[n]
                # n -> c must be a legal step: up at most 0.5, down at most 3
                if h - nh <= stap_max((cx, cy, cz), 0.5) + 1e-6 and nh - h <= 3.0:
                    ok.add(n)
                    q.append(n)
    return ok


def controleer(g):
    global _BINNEN
    g.bouw()
    fout = []

    def _b(c):
        x, y, z = c
        i, j = x - g.x0, z - g.z0
        if 0 <= i < g.X and 0 <= j < g.Z and not math.isnan(g.F[i, j]) and y <= g.C[i, j] + 1:
            return True
        return any((x - t["cx"]) ** 2 + (z - t["cz"]) ** 2 <= 3.7 ** 2 and t["bot"] - 1 <= y <= t["top"] + 1 for t in g.trappen)
    _BINNEN = _b
    p = g.plekken
    doos = (g.x0, g.z0, g.x0 + g.X, g.z0 + g.Z, g.y0, g.y0 + g.Y + 20)
    for k in ("binnen", "hal", "elite", "schat"):
        x, y, z = p[k]
        if hoogte(x, y, z) is None:
            fout.append(f"{k} {p[k]}: niet staanbaar ({blok(x, y - 1, z)} / {blok(x, y, z)} / {blok(x, y + 1, z)})")
    start_k = "uitgang" if "uitgang" in p else "ingang"
    sx, sy, sz = p[start_k]
    sy2 = stand_y(sx, sy, sz)
    if sy2 is None:
        fout.append(f"{start_k} {p[start_k]}: geen vloer")
        return fout, 0
    seen, vallen = loop((sx, sy2, sz), doos)
    for k in ("binnen", "hal", "elite", "schat"):
        x, y, z = p[k]
        y2 = stand_y(x, y, z)
        if y2 is None or (x, y2, z) not in seen:
            seen2, _ = loop((sx, sy2, sz), doos, max_stap=1.0)
            extra = " (wel met springen)" if y2 is not None and (x, y2, z) in seen2 else ""
            fout.append(f"{start_k} -> {k}: niet lopend bereikbaar{extra}")
    import grotplan
    water = {k for (k, _a, _t, _n, _r, zoek) in grotplan.META[g.id]["amb"] if zoek == "WATER"}
    for k in ("puzzel", "amb_1", "amb_2"):
        x, y, z = p[k]
        if k in water:
            if not any("water" in blok(x, yy, z) for yy in range(y - 2, y + 8)):
                fout.append(f"{k}: geen water bij het visanker")
            continue
        if stand_y(x, y + (4 if k == "puzzel" else 3), z) is None:
            fout.append(f"{k}: geen vloer bij het anker")
    # back to the entrance from everywhere (no pit without a way out)
    # (one reverse search: every cell that can walk to the entrance = walk the reversed graph from the entrance)
    terug_ok = terug_graaf(seen, (sx, sy2, sz))
    vast_ = [c for c in seen if c not in terug_ok and binnen(c)]
    if vast_:
        fout.append(f"{len(vast_)} plekken komen niet lopend terug bij de {start_k} (bv. {sorted(vast_)[:6]})")
    # closed shell: no cave air above the void
    open_ = 0
    for i in range(0, g.X, 2):
        for j in range(0, g.Z, 2):
            S = g.F[i, j]
            if math.isnan(S):
                continue
            x, z = i + g.x0, j + g.z0
            a = int(math.ceil(S))
            if any((x - t["cx"]) ** 2 + (z - t["cz"]) ** 2 <= 2.3 for t in g.trappen):
                continue   # (the hollowed core of a spiral's 3x3 pillar)
            if all(blok(x, y, z) == "minecraft:air" for y in range(a - 6, a)) and g.Wt[i, j] < -900 and lucht(blok(x, a, z)):
                open_ += 1
    if open_:
        fout.append(f"{open_} kolommen zonder bodem onder de grotvloer")
    return fout, len(seen)


def main():
    ids = sys.argv[1:]
    slecht = 0
    for g in GR.grotten():
        if ids and g.id not in ids:
            continue
        fout, n = controleer(g)
        print(f"{g.id}: {n} loopbare plekken, {len(fout)} problemen")
        for f in fout:
            print("   ", f)
        slecht += len(fout)
    sys.exit(1 if slecht else 0)


if __name__ == "__main__":
    main()
