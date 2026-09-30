"""
De Elf-Guhjestocht (2.10 rebuild) - the layout of the tour: one 256 x H x 256 template (the anchor in its middle, so the
whole tour stays within the +-8 chunk window around its start chunk), a frozen canal loop that meanders through the
polder like a real Frisian waterway, and the plots of the eleven villages spread evenly along it (every ~118 blocks).

Coordinates are template coordinates: x east, z south, y up; a cell (x, z) has its centre AT (x, z). The ground (and the
ice) is the layer y = GY; skaters and buildings stand on y = OY = GY + 1.

The loop is a chain of eleven village straights (axis aligned, 34 blocks long, the plot beside the ice) joined by smooth
Hermite splines through a few control points (LUS). A village plot is ROTATED onto its straight: the village modules
keep building in their own frame (the ice south of the plot, skaters from the west, see elftocht_dorp_api) and
plot_frame() maps that frame onto the world with a quarter-turn rotation. The plot lies on the left of the skaters
("L", like the villages were designed) or, where the polder has no room there, on their right ("R": the same village
turned the other way round; the skaters then pass it from its east).

The canal's half width varies naturally between 2 and 5 blocks per bank (breed()), but is exactly HALF = 3 along every
village straight, at the start and under the bridges. Everything is checked in check_route().
"""
import math

SIZE_X, SIZE_Z = 256, 256
GY = 4                  # the ground / ice layer
OY = GY + 1             # standing level (the villages' oy)
H = 40                  # template height (villages: PLOT_Y 32 from OY; nothing reaches H - 1)
ANCHOR = (128, GY, 128)
HALF = 3                # the canal's half width at the villages, the start and the bridges (7 blocks of ice)
MIN_HALF, MAX_HALF = 2.0, 5.0
PLOT_X, PLOT_Y, PLOT_Z = 28, 32, 24
STRAIGHT = 17           # a village straight reaches this far on both sides of the plot's middle (28 / 2 + 3)

# Guhwarden (village 1, the core's, never rotated): the plot north of the start line; the canal runs east along Z_E1
Z_E1 = 36
GW_X0 = 26
GUHWARDEN = (GW_X0, 4, GW_X0 + 48, 33)      # x0, z0, x1 (excl), z1 (excl): 48 x 29, the ice from z 33 on
START_X = GW_X0 + 26                        # the start / finish line (and the arch over it)
GW_STRAIGHT = (GW_X0 - 4, GW_X0 + 52)       # the straight ice in front of Guhwarden (x from, to)

NAMEN = {1: "Guhwarden", 2: "Snuh", 3: "IJlguh", 4: "Knabbelsloten", 5: "Vadsvoren", 6: "Guhdeloopen",
         7: "Vadskum", 8: "Knabbelsward", 9: "Guhlingen", 10: "Franeguh", 11: "Dokguh"}

RICHTING = {"E": (1, 0), "S": (0, 1), "W": (-1, 0), "N": (0, -1)}
KWART = {"E": 0, "S": 1, "W": 2, "N": 3}          # quarter turns (clockwise seen from above) from east

# The loop in route order, starting with Guhwarden's straight (skaters go east there):
#   ("V", index, (x, z), richting, kant)  a village straight: (x, z) = the centreline point at the plot's middle (the
#                                           along-axis coordinate on a half block, the cross one on a cell centre)
#   (x, z)                                a control point of the spline to the next straight
LUS = [
    ("V", 1, (GW_X0 + 23.5, Z_E1), "E", "L"),
    (102, 29), (128, 38),
    ("V", 2, (172.5, 36), "E", "L"),
    (216, 44),
    ("V", 3, (226, 122.5), "S", "L"),
    (225, 186), (219, 214),
    ("V", 4, (184.5, 226), "W", "L"),
    (150, 214), (118, 231),
    ("V", 5, (79.5, 226), "W", "L"),
    (34, 228), (21, 204),
    ("V", 6, (29, 154.5), "N", "L"),
    (34, 116), (44, 96), (62, 104),
    ("V", 7, (93.5, 109), "E", "L"),
    (124, 128), (114, 156), (120, 180),
    ("V", 8, (150.5, 187), "E", "L"),
    (200, 184), (198, 158),
    ("V", 9, (187, 119.5), "N", "L"),
    (190, 80), (172, 58), (160, 66),
    ("V", 10, (134.5, 69), "W", "L"),
    (102, 70), (84, 54),
    ("V", 11, (43.5, 70), "W", "R"),
    (14, 68), (6, 55), (9, 42),
]

# the bridges: fractions of the free stretch between village a and the next one (the spot is moved to where the canal
# runs along an axis); filled in by bruggen() below
BRUG_NA = [1, 2, 4, 6, 8, 10]


def _sub(a, b):
    return (a[0] - b[0], a[1] - b[1])


def _add(a, b):
    return (a[0] + b[0], a[1] + b[1])


def _mul(a, k):
    return (a[0] * k, a[1] * k)


def _len(a):
    return math.hypot(a[0], a[1])


def _unit(a):
    n = _len(a)
    return (a[0] / n, a[1] / n) if n else (1.0, 0.0)


def dorpen():
    """{index: (centre (x, z), richting, kant)} of all eleven village straights."""
    return {it[1]: (it[2], it[3], it[4]) for it in LUS if isinstance(it[0], str)}


def _straight(index):
    (cx, cz), r, _ = dorpen()[index]
    d = RICHTING[r]
    if index == 1:
        return (GW_STRAIGHT[0], Z_E1), (GW_STRAIGHT[1], Z_E1), d
    return (cx - d[0] * STRAIGHT, cz - d[1] * STRAIGHT), (cx + d[0] * STRAIGHT, cz + d[1] * STRAIGHT), d


def _hermite(p0, p1, m0, m1, t):
    t2, t3 = t * t, t * t * t
    h00, h10, h01, h11 = 2 * t3 - 3 * t2 + 1, t3 - 2 * t2 + t, -2 * t3 + 3 * t2, t3 - t2
    return (h00 * p0[0] + h10 * m0[0] + h01 * p1[0] + h11 * m1[0], h00 * p0[1] + h10 * m0[1] + h01 * p1[1] + h11 * m1[1])


def _spline(pts, d0, d1, step):
    """A smooth curve through pts (first/last with the fixed unit tangents d0/d1), sampled about every `step`."""
    n = len(pts)
    dirs = []
    for i in range(n):
        if i == 0:
            dirs.append(d0)
        elif i == n - 1:
            dirs.append(d1)
        else:
            dirs.append(_unit(_sub(pts[i + 1], pts[i - 1])))
    out = []
    for i in range(n - 1):
        a, b = pts[i], pts[i + 1]
        chord = _len(_sub(b, a))
        m0, m1 = _mul(dirs[i], chord), _mul(dirs[i + 1], chord)
        k = max(4, int(math.ceil(chord * 1.6 / step)))
        for j in range(k):
            out.append(_hermite(a, b, m0, m1, j / k))
    return out


def centreline(step=0.25, glad=30):
    """Dense points of the canal's centreline in route order (closed loop): [(x, z, tx, tz, s)] with the unit tangent and
    the distance s from the start line (START_X on Guhwarden's straight). The splines between the village straights are
    smoothed a little more (glad passes of a moving average; the straights stay put)."""
    items = LUS
    raw = []            # (x, z, vast)
    n = len(items)
    vs = [i for i, it in enumerate(items) if isinstance(it[0], str)]
    for vi, i in enumerate(vs):
        a, b, d = _straight(items[i][1])
        length = _len(_sub(b, a))
        k = max(1, int(math.ceil(length / step)))
        for m in range(k):
            raw.append(_add(a, _mul(d, length * m / k)) + (True,))
        j = vs[(vi + 1) % len(vs)]
        ctrl = [items[q % n] for q in range(i + 1, j if j > i else j + n)]
        na, nb, nd = _straight(items[j][1])
        for p in _spline([b] + [tuple(map(float, c)) for c in ctrl] + [na], d, nd, step):
            raw.append(p + (False,))
    raw = _even(raw, step)
    # smoothing: free points only (a window of +-8 samples = 2 blocks)
    pts = [(p[0], p[1]) for p in raw]
    vast = [p[2] for p in raw]
    m = len(pts)
    w = 8
    # how far (in samples) every point is from a straight: the smoothing fades in over 60 samples (15 blocks)
    afstand = [0 if v else m for v in vast]
    for _ in range(2):
        for i in range(m):
            afstand[i] = min(afstand[i], afstand[i - 1] + 1)
        for i in range(m - 1, -1, -1):
            afstand[i] = min(afstand[i], afstand[(i + 1) % m] + 1)
    alpha = [min(1.0, a / 60.0) ** 2 for a in afstand]
    for _ in range(glad):
        sx = [0.0] * (m + 1)
        sz = [0.0] * (m + 1)
        ext = pts[-w:] + pts + pts[:w]
        cx = [0.0]
        cz = [0.0]
        for p in ext:
            cx.append(cx[-1] + p[0])
            cz.append(cz[-1] + p[1])
        new = []
        for i in range(m):
            if vast[i]:
                new.append(pts[i])
            else:
                a, b = i, i + 2 * w + 1
                mx, mz = (cx[b] - cx[a]) / (2 * w + 1), (cz[b] - cz[a]) / (2 * w + 1)
                new.append((pts[i][0] + (mx - pts[i][0]) * alpha[i], pts[i][1] + (mz - pts[i][1]) * alpha[i]))
        pts = new
    raw = _even([p + (v,) for p, v in zip(pts, vast)], step)
    cum = [0.0]
    for p, q in zip(raw, raw[1:] + raw[:1]):
        cum.append(cum[-1] + math.hypot(q[0] - p[0], q[1] - p[1]))
    total = cum[-1]
    # the start line
    i0 = min(range(len(raw)), key=lambda i: (raw[i][0] - START_X) ** 2 + (raw[i][1] - Z_E1) ** 2)
    s0 = cum[i0] + (START_X - raw[i0][0])
    out = []
    for i, p in enumerate(raw):
        q = raw[(i + 1) % len(raw)]
        tx, tz = _unit((q[0] - p[0], q[1] - p[1]))
        out.append((p[0], p[1], tx, tz, (cum[i] - s0) % total))
    k0 = min(range(len(out)), key=lambda i: out[i][4])
    out = out[k0:] + out[:k0]
    return out, total


def _even(raw, step):
    """Resamples a closed polyline [(x, z, vast)] evenly by arc length (about every `step`)."""
    cum = [0.0]
    ring = raw + raw[:1]
    for p, q in zip(ring, ring[1:]):
        cum.append(cum[-1] + math.hypot(q[0] - p[0], q[1] - p[1]))
    total = cum[-1]
    count = int(round(total / step))
    out = []
    j = 0
    for k in range(count):
        target = k * total / count
        while cum[j + 1] < target:
            j += 1
        p, q = ring[j], ring[j + 1]
        seg = cum[j + 1] - cum[j]
        t = (target - cum[j]) / seg if seg else 0.0
        out.append((p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, p[2] and q[2]))
    return out


def _ruis(s, seed, length, parts=((1, 1.0), (3, 0.55), (7, 0.3))):
    """A smooth closed-loop noise in -1..1 along the route."""
    v = 0.0
    tot = 0.0
    rnd = seed * 12.9898
    for k, (freq, amp) in enumerate(parts):
        f = freq + (seed * (k + 3)) % 3
        ph = (math.sin(rnd * (k + 1)) * 43758.5453) % (2 * math.pi)
        v += amp * math.sin(2 * math.pi * f * s / length + ph)
        tot += amp
    return v / tot


_CACHE = {}


def vaste_plekken():
    """Distances along the route where the canal is exactly HALF wide: [(s, reach)] (villages, start, bridges)."""
    if "vast" not in _CACHE:
        pts, length = centreline()
        out = []
        for index in range(1, 12):
            s = dorp_s(index)
            out.append((s, (GW_STRAIGHT[1] - GW_STRAIGHT[0]) / 2 + 4 if index == 1 else STRAIGHT + 3))
        for s in bruggen():
            out.append((s[0], 9))
        _CACHE["vast"] = out
    return _CACHE["vast"]


def vrijheid(s):
    """0 where the canal must be exactly HALF wide (villages, start, bridges), rising to 1 over 14 blocks."""
    _, length = centreline_cached()
    f = 1.0
    for (sv, reach) in vaste_plekken():
        ds = abs(s - sv)
        ds = min(ds, length - ds)
        if ds <= reach:
            return 0.0
        if ds < reach + 14:
            t = (ds - reach) / 14
            f = min(f, t * t * (3 - 2 * t))
    return f


def oeverruis(x, z):
    """A small smooth 2D noise (-1..1) that makes the banks a bit ragged."""
    return 0.6 * math.sin(x * 0.61 + z * 0.23 + 1.7) * math.sin(z * 0.47 - x * 0.19 + 0.4) + 0.4 * math.sin(x * 1.37 + 2.1) * math.sin(z * 1.21 + 0.9)


def breed(s, kant):
    """The half width of the canal on one bank (kant +1 = left of the skaters, -1 = right) at distance s."""
    _, length = centreline_cached()
    w = 3.4 + 1.25 * _ruis(s, 7 if kant > 0 else 11, length) + 0.35 * _ruis(s, 23 if kant > 0 else 31, length, ((17, 1.0), (29, 0.6)))
    w = max(MIN_HALF, min(MAX_HALF, w))
    # exactly HALF near the villages, the start and the bridges, blending in over 14 blocks
    for (sv, reach) in vaste_plekken():
        ds = abs(s - sv)
        ds = min(ds, length - ds)
        if ds <= reach:
            return float(HALF)
        if ds < reach + 14:
            t = (ds - reach) / 14
            t = t * t * (3 - 2 * t)
            w = HALF + (w - HALF) * t
    return w


def centreline_cached():
    if "c" not in _CACHE:
        _CACHE["c"] = centreline()
    return _CACHE["c"]


def route():
    """(centreline points, loop length, ice cells {(x, z): distance along the route})."""
    if "r" not in _CACHE:
        pts, length = centreline_cached()
        best = {}
        widths = {}
        for i in range(0, len(pts), 2):
            x, z, tx, tz, s = pts[i]
            key = int(s)
            if key not in widths:
                widths[key] = (breed(key, 1), breed(key, -1), vrijheid(key))
            wl, wr, vrij = widths[key]
            reach = int(max(wl, wr)) + 2
            for cx in range(int(round(x)) - reach, int(round(x)) + reach + 1):
                for cz in range(int(round(z)) - reach, int(round(z)) + reach + 1):
                    dx, dz = cx - x, cz - z
                    along = dx * tx + dz * tz
                    if abs(along) > 0.6:
                        continue
                    side = dx * tz - dz * tx         # > 0: left of the skaters (left of (tx, tz) is (tz, -tx))
                    w = wl if side > 0 else wr
                    d = abs(side)
                    if d <= w + 0.5 + (0.45 * vrij * oeverruis(cx, cz) if vrij > 0 else 0.0):
                        old = best.get((cx, cz))
                        if old is None or d < old[0]:
                            best[(cx, cz)] = (d, s)
        ice = {c: v[1] for c, v in best.items()}
        # no single-cell notches or lonely cells on the banks
        for _ in range(2):
            add = []
            for (cx, cz) in list(ice):
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    n = (cx + dx, cz + dz)
                    if n not in ice and sum((n[0] + a, n[1] + b) in ice for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))) >= 3:
                        add.append((n, ice[(cx, cz)]))
            for n, s in add:
                ice[n] = s
        _CACHE["r"] = (pts, length, ice)
    return _CACHE["r"]


def plot_frame(index):
    """(to_world(lx, lz) -> (x, z) for a cell of the village's own frame (lx 0..27 along the ice, lz 0..23 from the back of
    the plot to the canal edge, lz 24..30 the ice), quarter turns k, yaw offset in degrees).
    Local +x (the skaters' way in the village's frame) maps to e1, local +z (towards the canal) to e2."""
    (cx, cz), r, kant = dorpen()[index]
    d = RICHTING[r]
    if kant == "L":
        e1 = d
    else:
        e1 = (-d[0], -d[1])
    e2 = (-e1[1], e1[0])            # e1 turned a quarter clockwise (east -> south)
    k = {(1, 0): 0, (0, 1): 1, (-1, 0): 2, (0, -1): 3}[e1]

    def to_world(lx, lz):
        u, v = lx - 13.5, lz - 27
        return (int(round(cx + u * e1[0] + v * e2[0])), int(round(cz + u * e1[1] + v * e2[1])))

    return to_world, k, 90.0 * k


def plot_box(index):
    """(x0, z0, x1, z1) of a village plot (x1/z1 exclusive), Guhwarden's for index 1."""
    if index == 1:
        return GUHWARDEN
    if ("box", index) not in _CACHE:
        f, _, _ = plot_frame(index)
        a, b = f(0, 0), f(PLOT_X - 1, PLOT_Z - 1)
        _CACHE[("box", index)] = (min(a[0], b[0]), min(a[1], b[1]), max(a[0], b[0]) + 1, max(a[1], b[1]) + 1)
    return _CACHE[("box", index)]


def in_plot(x, z, margin=0):
    for i in range(1, 12):
        x0, z0, x1, z1 = plot_box(i)
        if x0 - margin <= x < x1 + margin and z0 - margin <= z < z1 + margin:
            return i
    return 0


def along(s):
    """The centreline point at distance s along the loop: (x, z, tx, tz)."""
    pts, length = centreline_cached()
    s %= length
    lo, hi = 0, len(pts) - 1
    while lo < hi:
        mid = (lo + hi + 1) // 2
        if pts[mid][4] <= s:
            lo = mid
        else:
            hi = mid - 1
    x, z, tx, tz, _ = pts[lo]
    return x, z, tx, tz


def distance_at(x, z):
    """How far along the route the centreline point nearest to (x, z) is."""
    pts, _ = centreline_cached()
    best = min(pts, key=lambda p: (p[0] - x) ** 2 + (p[1] - z) ** 2)
    return best[4]


def dorp_s(index):
    """The distance along the route of a village's middle (Guhwarden: 0, its plot's middle is right at the start)."""
    if index == 1:
        return 0.0
    (cx, cz), _, _ = dorpen()[index]
    return distance_at(cx, cz)


def gaten():
    """The distances between the villages along the route: [(a, b, gap)] including 11 -> 1."""
    _, length = centreline_cached()
    ss = [dorp_s(i) for i in range(1, 12)]
    out = []
    for i in range(11):
        a, b = ss[i], ss[(i + 1) % 11]
        out.append((i + 1, (i + 1) % 11 + 1, (b - a) % length))
    return out


def bruggen():
    """The bridges: [(s, x, z, along_x)] - a spot in the free stretch after the villages in BRUG_NA where the canal runs
    along an axis (the deck crosses it), the one nearest to the middle of the stretch."""
    if "bruggen" not in _CACHE:
        pts, length = centreline_cached()
        out = []
        for a in BRUG_NA:
            sa = dorp_s(a) + (STRAIGHT + 3 if a != 1 else 30)
            sb = dorp_s(a % 11 + 1) - (STRAIGHT + 3)
            if sb < sa:
                sb += length
            mid = (sa + sb) / 2
            best = None
            for p in pts:
                s = p[4] if p[4] >= sa - 1e-9 else p[4] + length
                if not (sa + 22 <= s <= sb - 22):
                    continue
                if max(abs(p[2]), abs(p[3])) < 0.97:
                    continue
                if not (16 <= p[0] <= SIZE_X - 17 and 16 <= p[1] <= SIZE_Z - 17):
                    continue
                score = abs(s - mid)
                if best is None or score < best[0]:
                    best = (score, p)
            if best:
                x, z, tx, tz, s = best[1]
                along_x = abs(tz) > abs(tx)       # the canal runs north-south: the deck along x
                out.append((s, int(round(x)), int(round(z)), along_x))
        _CACHE["bruggen"] = out
    return _CACHE["bruggen"]


def min_bocht():
    """The smallest turning radius of the centreline (over 6 blocks)."""
    pts, _ = centreline_cached()
    step = 24
    best = 1e9
    for i in range(0, len(pts), 4):
        a, b = pts[i], pts[(i + step) % len(pts)]
        ang = math.acos(max(-1.0, min(1.0, a[2] * b[2] + a[3] * b[3])))
        if ang > 1e-6:
            best = min(best, (step * 0.25) / ang)
    return best


def check_route():
    """Raises SystemExit on any problem with the layout; returns a small report dict."""
    pts, length, ice = route()
    problems = []
    for (x, z) in ice:
        if not (1 <= x < SIZE_X - 1 and 1 <= z < SIZE_Z - 1):
            problems.append(f"ice outside the template at {(x, z)}")
            break
    boxes = [plot_box(i) for i in range(1, 12)]
    for i, a in enumerate(boxes):
        if a[0] < 1 or a[1] < 1 or a[2] > SIZE_X - 1 or a[3] > SIZE_Z - 1:
            problems.append(f"plot {i + 1} outside the template: {a}")
        for j, b in enumerate(boxes[:i]):
            if a[0] - 2 < b[2] and b[0] - 2 < a[2] and a[1] - 2 < b[3] and b[1] - 2 < a[3]:
                problems.append(f"plots {j + 1} and {i + 1} overlap (or touch)")
        # no ice in the plot, and no other part of the canal within 2 of it
        for x in range(a[0] - 2, a[2] + 2):
            for z in range(a[1] - 2, a[3] + 2):
                if (x, z) in ice:
                    inside = a[0] <= x < a[2] and a[1] <= z < a[3]
                    if inside:
                        problems.append(f"ice inside plot {i + 1} at {(x, z)}")
                        break
                    if i > 0:
                        ds = abs(ice[(x, z)] - dorp_s(i + 1))
                        if min(ds, length - ds) > STRAIGHT + 8:
                            problems.append(f"another part of the canal touches plot {i + 1} at {(x, z)}")
                            break
    # every village: the ice exactly 7 wide along the whole plot front, straight, in the right direction
    for index in range(2, 12):
        f, k, _ = plot_frame(index)
        for lx in range(-2, PLOT_X + 2):
            for lz in range(PLOT_Z, PLOT_Z + 2 * HALF + 1):
                if f(lx, lz) not in ice:
                    problems.append(f"village {index}: no ice at {f(lx, lz)} in front of the plot")
                    break
            if f(lx, PLOT_Z + 2 * HALF + 1) in ice and 0 <= lx < PLOT_X:
                problems.append(f"village {index}: the canal is wider than 7 in front of the plot at {f(lx, PLOT_Z + 2 * HALF + 1)}")
            if f(lx, PLOT_Z - 1) in ice:
                problems.append(f"village {index}: ice on the plot's canal edge at {f(lx, PLOT_Z - 1)}")
    x0, z0, x1, z1 = GUHWARDEN
    for x in range(x0, x1):
        if (x, z1) not in ice or (x, z1 - 1) in ice:
            problems.append(f"Guhwarden: the ice doesn't start right at the plot at {(x, z1)}")
            break
    # the village order along the route and the gaps between them
    order = sorted(range(1, 12), key=dorp_s)
    if order != list(range(1, 12)):
        problems.append(f"the villages are not in stamp order along the route: {order}")
    for a, b, gap in gaten():
        if not 95 <= gap <= 145:
            problems.append(f"the gap between village {a} and {b} is {gap:.0f} (95..145)")
    if in_plot(ANCHOR[0], ANCHOR[2], margin=2) or (ANCHOR[0], ANCHOR[2]) in ice:
        problems.append("the anchor is not on free ground")
    # the loop doesn't come close to itself (except along the route)
    grid = {}
    for (x, z, _, _, s) in pts[::8]:
        grid.setdefault((int(x) // 16, int(z) // 16), []).append((x, z, s))
    close = None
    for (gx, gz), cell in grid.items():
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for (x2, z2, s2) in grid.get((gx + dx, gz + dz), []):
                    for (x, z, s) in cell:
                        ds = min(abs(s - s2), length - abs(s - s2))
                        if ds > 40 and math.hypot(x - x2, z - z2) < 2 * MAX_HALF + 6:
                            close = (round(x), round(z))
    if close:
        problems.append(f"the canal comes too close to itself near {close}")
    r = min_bocht()
    if r < 8:
        problems.append(f"a turn is too sharp (radius {r:.1f})")
    if not 900 <= length <= 1400:
        problems.append(f"route length {length:.0f} outside 900..1400")
    if len(bruggen()) < 4:
        problems.append(f"only {len(bruggen())} bridge spots")
    if problems:
        raise SystemExit("elftocht route: " + "; ".join(sorted(set(problems))[:14]))
    return {"length": length, "ice": len(ice), "gaps": gaten(), "radius": r}


if __name__ == "__main__":
    info = check_route()
    print(f"route ok: {info['length']:.0f} blocks, {info['ice']} ice cells, min radius {info['radius']:.1f}")
