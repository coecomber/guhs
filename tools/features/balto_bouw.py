"""
Nomguh (3.0 Guhverhalen, slice balto) - the template (224 x H x 224, placed UNROTATED on the peak of a Sneeuwguhtoendra by
guhs:regio_jigsaw; its anchor jigsaw guhs:nomguh_midden is the middle of the town square) and the trek route file.

  The landscape (a heightfield, template y G = the ground at the anchor): the town on a flat of snow in the south, the frozen
  bay in the west with the old boat, the Nomguh peaks (pink Guhpieken rock, snowy tops) in the north-west, a long ridge
  across the north with the storm valley south of it, the steep Lawineberg, the deep Kloof (a gorge with a frozen stream)
  across the north strip, hills along the east and north edges.
  The route (ROUTE: a Catmull-Rom spline through the control points, the track 7 blocks wide of packed sled-track snow cut
  into / filled onto the land): from the sled stable east through the Stormdal (rest point 1), north round the bend (rest
  point 2), west along the north strip past the Wolvenrots (the dieptepunt of the way back), over the IJsbrug across the
  Kloof, under the Lawineberg (rest point 3 after it), and up the Bergkam (rest point 4) to the berghut on the Hutkop.
  Red-and-white route markers with glowing lamps every ROUTEPAAL_STAP blocks on both sides.
  The town (balto_gebouwen): the square with the big decorated sneeuwguhspar and the empty statue pedestal, eight cottages,
  the ziekenhuisje (Rosy, the sick babies, the nurse), the stable (the start line, Steele-Mika with plek "sledesprint"), the
  old boat (Baltoguh's story copy, Boris), Muk and Luk's igloo by the ice pond, a Reisguh, lamp posts, sneeuwpopguhs.

build(h) -> (Structure, info); check(s, info) raises SystemExit on problems; route_json(info) is what balto.py writes to
assets/guhs/nomguh/route.json (CONTRACT_30 §4.9; the y of every point is the TOP BLOCK of the track).
"""
import math
import random

import numpy as np

from features import balto_gebouwen as geb
from features import reisguh_plek

NAME = "nomguh"
SIZE = 224
G = 20                   # the template y of the ground at the anchor (the 20 layers under it are the skirt: no hollows under the town)
H = 76                   # template height (the peaks reach G + 44, their snow G + 46)
ANKER = (112, G, 112)    # the middle of the town square
REACH = 118              # how far the template reaches from its anchor (<= 136)
BREED = 3.0              # the track's half width: 7 blocks between the markers
ROUTEPAAL_STAP = 7       # a marker pair every this many blocks
PUNT_STAP = 3.0          # route.json: a point every this many blocks

SNEEUW = "minecraft:snow_block"
SPOOR = "guhs:nomguh_sneeuwspoor"
PAAL = "guhs:nomguh_routepaal"
AIR = "minecraft:air"

# the route's control points: (x, z, height above G) from the start line at the stable to the berghut
ROUTE = [
    (92.0, 77.0, 0), (104, 72, 0), (122, 64, 0), (148, 59, 0), (172, 56, 0), (192, 50, 0),
    (203, 38, 0), (203, 24, 0), (193, 14, 0), (176, 13, 0), (160, 15.5, 0), (140, 16.5, 0),
    (120, 17, 0), (100, 18, 1), (88, 21, 3), (78, 28, 6), (70, 37, 9), (64, 45, 12), (60, 52, 15), (56, 57, 17),
]
KLIM_VAN = (102, 18)        # the climb to the berghut starts here (the Lawineberg is behind you)
RUST = [("stormdal", (148, 59)), ("bocht", (203, 36)), ("lawine", (92, 20)), ("bergkam", (68, 41))]
DIEPTEPUNT = (184, 13)      # the Wolvenrots: on the way back the white wolf-guh appears here
LAWINE = ((136, 17), (101, 18))   # the Lawineberg's face beside the track (on the LEFT going to the berghut)

# the landscape ------------------------------------------------------------------------------------------------------------
# ridges: (height at a, height at b, a, b, full within, zero at)
RUGGEN = [
    (14, 12, (108, 36), (190, 34), 5, 14),          # the long ridge between the storm valley and the north strip
    (22, 22, (98, 33), (137, 33), 5, 11),           # the Lawineberg (steep north face over the track)
    (12, 12, (218, -8), (218, 120), 3, 13),         # the hills along the east edge
    (10, 10, (60, -6), (230, -6), 4, 13),           # the hills along the north edge
    (6, 6, (134, 77), (200, 71), 2, 9),             # the low wall between the valley and the town
    (4, 17, (88, 22), (55, 53), 4, 12),             # the Bergkam up to the Hutkop
    (17, 17, (45, 57), (45, 57), 9, 20),            # the Hutkop (the berghut's shoulder)
    (8, 8, (206, 150), (222, 210), 4, 16),          # the south-east hills
    (6, 6, (40, 226), (200, 228), 3, 12),           # the south edge
    (5, 7, (228, 118), (228, 150), 3, 12),
]
# mountains: (x, z, height, radius, power)
BERGEN = [(24, 24, 44, 46, 1.3), (62, 7, 26, 26, 1.2), (8, 60, 20, 22, 1.2)]
STAD = ((130, 138), (66, 62))               # the town: centre and radii (flattened)
# more flat ground (x0, z0, x1, z1): the stable and its start line, the shore by the boat, the igloo
VLAK = [(66, 66, 108, 98), (60, 90, 76, 124), (74, 150, 116, 184)]
BAAI = ((38, 128), (26, 42))                # the frozen bay: centre and radii (ice at G - 1)
VIJVER = ((102, 172), 8)                    # Muk and Luk's ice pond
KLOOF = (150.0, 3, 46)                      # the gorge: x (it wiggles a little), z from, z to
KLOOF_BODEM = G - 9


def _seg(px, pz, a, b):
    ax, az = a
    bx, bz = b
    dx, dz = bx - ax, bz - az
    L2 = dx * dx + dz * dz
    if L2 == 0:
        return np.hypot(px - ax, pz - az), np.zeros_like(px)
    t = np.clip(((px - ax) * dx + (pz - az) * dz) / L2, 0, 1)
    return np.hypot(px - (ax + t * dx), pz - (az + t * dz)), t


def _plat(d, vol, nul):
    t = np.clip((nul - d) / max(1e-6, nul - vol), 0, 1)
    return t * t * (3 - 2 * t)


def _ruis(seed, cel):
    rng = np.random.default_rng(seed)
    n = SIZE // cel + 3
    grid = rng.random((n, n))
    zs, xs = np.mgrid[0:SIZE, 0:SIZE] / cel
    x0, z0 = xs.astype(int), zs.astype(int)
    fx, fz = xs - x0, zs - z0
    fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    a, b, c, d = grid[z0, x0], grid[z0, x0 + 1], grid[z0 + 1, x0], grid[z0 + 1, x0 + 1]
    return (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz


def kloof_x(z):
    return KLOOF[0] + 2.0 * math.sin(z / 9.0)


# =====================================================================================================================
# the route
# =====================================================================================================================
def _catmull(pts, stap=0.25):
    out = []
    P = [pts[0]] + list(pts) + [pts[-1]]
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = (np.array(P[j], float) for j in (i - 1, i, i + 1, i + 2))
        n = max(2, int(np.linalg.norm(p2[:2] - p1[:2]) / stap))
        for k in range(n):
            t = k / n
            t2, t3 = t * t, t * t * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    out.append(np.array(pts[-1], float))
    return np.array(out)


def route_lijn():
    """The centreline every 0.5 blocks: array (n, 4) of x, z, dy (height above G, smoothed), s (arc length)."""
    fijn = _catmull(ROUTE)
    seg = np.hypot(np.diff(fijn[:, 0]), np.diff(fijn[:, 1]))
    s = np.concatenate([[0], np.cumsum(seg)])
    L = s[-1]
    ss = np.arange(0, L, 0.5)
    x = np.interp(ss, s, fijn[:, 0])
    z = np.interp(ss, s, fijn[:, 1])
    # the height: flat until the end of the Lawineberg, then an even climb up the Bergkam to the berghut
    van = ss[int(np.hypot(x - KLIM_VAN[0], z - KLIM_VAN[1]).argmin())]
    dy = np.clip((ss - van) / max(1.0, L - 4 - van), 0, 1) * ROUTE[-1][2]
    k = 16                                            # smooth the height: no bumps for the sled
    pad = np.concatenate([np.full(k, dy[0]), dy, np.full(k, dy[-1])])
    dy = np.convolve(pad, np.ones(2 * k + 1) / (2 * k + 1), mode="same")[k:-k]
    return np.stack([x, z, dy, ss], axis=1)


# =====================================================================================================================
# the landscape
# =====================================================================================================================
def landschap(lijn):
    """Top block heights T[z, x] (template y), and the masks the builder needs."""
    zs, xs = np.mgrid[0:SIZE, 0:SIZE].astype(float)
    r1, r2, r3 = _ruis(20300721, 26), _ruis(20300722, 9), _ruis(20300723, 5)
    h = (r1 - 0.5) * 2.2 + (r2 - 0.5) * 0.9
    berg = np.zeros_like(h)
    for (h0, h1, a, b, vol, nul) in RUGGEN:
        d, t = _seg(xs, zs, a, b)
        hh = h0 + (h1 - h0) * t
        p = _plat(d, vol, nul)
        h = np.maximum(h, hh * p + (r2 - 0.5) * 1.6 * p + (r3 - 0.5) * 1.0 * p)
    for (cx, cz, hoog, R, macht) in BERGEN:
        r = np.hypot(xs - cx, zs - cz) / R
        b = hoog * np.clip(1 - r, 0, 1) ** macht * (0.9 + 0.2 * r1) + (r3 - 0.5) * 2.0 * (r < 1)
        berg = np.maximum(berg, b)
        h = np.maximum(h, b)
    # the town is a flat of snow (a hair of unevenness at its rim)
    (sx, sz), (srx, srz) = STAD
    e = np.hypot((xs - sx) / srx, (zs - sz) / srz)
    stad = _plat(e, 0.82, 1.0)
    h = h * (1 - stad)
    for (x0, z0, x1, z1) in VLAK:
        d = np.maximum(np.maximum(x0 - xs, xs - x1), np.maximum(z0 - zs, zs - z1))
        h = h * (1 - _plat(d, 0, 6))
    T = G + np.round(h).astype(int)
    # the frozen bay and the ice pond (ice at G - 1), with a gentle shore
    (bx, bz), (brx, brz) = BAAI
    eb = np.hypot((xs - bx) / brx, (zs - bz) / brz)
    baai = eb <= 1.0
    (vx, vz), vr = VIJVER
    vijver = np.hypot(xs - vx, zs - vz) <= vr
    ijs = baai | vijver
    oever = (eb > 1.0) & (eb < 1.18)
    T = np.where(oever, np.minimum(T, G), T)
    T = np.where(ijs, G - 1, T)
    # the Kloof: a gorge with near vertical walls down to a frozen stream
    kx = np.array([[kloof_x(z) for z in range(SIZE)]]).T * np.ones((1, SIZE))
    dxk = np.abs(xs - kx)
    zin = (zs >= KLOOF[1]) & (zs <= KLOOF[2])
    eind = np.minimum(zs - KLOOF[1], KLOOF[2] - zs)
    wand = KLOOF_BODEM + np.maximum(0, dxk - 3.2) * 3.4 + np.maximum(0, 3 - eind) * 5
    kloof = zin & (wand < T)
    T = np.where(kloof, np.round(np.minimum(T, wand)).astype(int), T)
    # the route: cut into the hills (a narrow shoulder: the walls stay steep) or filled up (a wide gentle bank)
    spoor_y = np.full(T.shape, -1)
    spoor = np.zeros(T.shape, bool)
    lat = np.full(T.shape, 99.0)
    idx = np.zeros(T.shape, int)
    pts = lijn[:, :2]
    for z in range(SIZE):
        row = np.stack([np.arange(SIZE, dtype=float), np.full(SIZE, float(z))], axis=1)
        d2 = ((row[:, None, :] - pts[None, :, :]) ** 2).sum(-1)
        j = d2.argmin(1)
        lat[z] = np.sqrt(d2[np.arange(SIZE), j])
        idx[z] = j
    ty = G + np.round(lijn[idx, 2]).astype(int)
    brug = kloof.copy()
    for z in range(SIZE):
        for x in range(SIZE):
            d = lat[z, x]
            if d > BREED + 7.5 or brug[z, x]:
                continue
            y = ty[z, x]
            if d <= BREED + 0.5:
                T[z, x] = y
                spoor[z, x] = True
                spoor_y[z, x] = y
                continue
            t0 = T[z, x]
            breed = 1.6 if t0 > y else 6.0
            k = (d - BREED - 0.5) / breed
            if k < 1:
                k = k * k * (3 - 2 * k)
                T[z, x] = int(round(y + (t0 - y) * k))
    return {"T": T, "berg": berg, "ijs": ijs, "baai": baai, "vijver": vijver, "kloof": kloof, "spoor": spoor, "lat": lat, "idx": idx,
            "stad": stad > 0.5, "r1": r1, "r2": r2}


def kolommen(b, L):
    """Fills every column of the template: the skirt (pink wool deep down like the Guhmension's own ground, dirt), the
    exposed sides of hills and gorges (snow, Guhpieken rock on the steep mountain faces, stone and ice in the gorge), the top
    block (snow, rock, track, ice), and air where the ground lies lower than the anchor's ground."""
    T, rng = L["T"], b.rng
    for z in range(SIZE):
        for x in range(SIZE):
            top = int(T[z, x])
            buren = [int(T[zz, xx]) for xx, zz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)) if 0 <= xx < SIZE and 0 <= zz < SIZE]
            laagste = min(buren + [top])
            steil = top - laagste
            berg = L["berg"][z, x] > 2.5 and steil >= 3
            kloof = L["kloof"][z, x]
            for y in range(0, top + 1):
                diep = top - y
                if y > laagste and diep > 0:            # an exposed side
                    if berg:
                        blok = "pink_terracotta" if (x * 3 + y * 5 + z) % 7 else "pink_wool"
                    elif kloof or (top - y > 3 and steil >= 4):
                        blok = "packed_ice" if (y + x) % 6 == 0 and kloof else ("andesite" if (x + y * 2 + z) % 5 == 0 else "stone")
                    else:
                        blok = "snow_block"
                elif diep == 0:
                    continue                            # (the top block below)
                elif diep <= 3:
                    blok = "dirt"
                else:
                    blok = "pink_wool"
                b.s.blocks[(x, y, z)] = (f"minecraft:{blok}", {}, None)
            # the top block
            if L["spoor"][z, x]:
                t = SPOOR
            elif L["ijs"][z, x]:
                t = "minecraft:packed_ice" if (x * 7 + z * 3) % 9 else "minecraft:blue_ice"
            elif kloof:
                t = "minecraft:packed_ice"
            elif berg:
                t = "minecraft:pink_terracotta" if (x + z) % 3 else "minecraft:pink_wool"
            elif top >= G + 36:
                t = "minecraft:white_wool"              # (like the Guhpieken's tops)
            else:
                t = SNEEUW
            b.s.blocks[(x, top, z)] = (t, {}, None)
            # air down to the anchor's ground where the ground lies lower (the bay, the gorge, dips)
            for y in range(top + 1, G + 1):
                b.s.blocks[(x, y, z)] = (AIR, {}, None)
            # a little air over the track and the town's flat (the world's own hills never poke through)
            if L["spoor"][z, x] or L["stad"][z, x]:
                for y in range(top + 1, top + 5):
                    if (x, y, z) not in b.s.blocks:
                        b.s.blocks[(x, y, z)] = (AIR, {}, None)


# =====================================================================================================================
# the route's things: markers, the ice bridge, the Lawineberg, rest points, the Wolvenrots
# =====================================================================================================================
def ijsbrug(b, L, lijn):
    """The IJsbrug: a deck of packed ice with a blue-ice middle over the Kloof, an arch under it that is thick at the ends and
    thin in the middle, low ice railings with glowing ice crystals on the posts, icicles under the edges. Returns the route
    sample indexes on the deck."""
    T = L["T"]
    dek = []
    for i, (x, z, dy, s) in enumerate(lijn):
        if L["kloof"][int(round(z)), int(round(x))]:
            dek.append(i)
    if not dek:
        raise SystemExit("nomguh: the route doesn't cross the Kloof")
    i0, i1 = min(dek) - 4, max(dek) + 4
    xs = [lijn[i, 0] for i in range(i0, i1 + 1)]
    span = (min(xs), max(xs))
    for z in range(SIZE):
        for x in range(int(span[0]) - 1, int(span[1]) + 2):
            if not L["kloof"][z, x] or L["lat"][z, x] > BREED + 1.6:
                continue
            u = (x - span[0]) / max(1, span[1] - span[0])
            dik = 1 + int(round(5 * (1 - math.sin(math.pi * min(1, max(0, u))))))
            d = L["lat"][z, x]
            if d <= BREED + 0.5:
                b.set(x, G, z, "minecraft:blue_ice" if d <= 1.0 else "minecraft:packed_ice")
                for y in range(G - dik, G):
                    b.set(x, y, z, "minecraft:packed_ice")
                for y in range(G + 1, G + 4):
                    b.set(x, y, z, AIR)
                T[z, x] = G
                L["spoor"][z, x] = True
            else:
                # the railing: a low wall of ice, a post with a glowing crystal every third block
                b.set(x, G, z, "minecraft:packed_ice")
                b.set(x, G - 1, z, "minecraft:packed_ice")
                if (x % 3) == 0:
                    b.set(x, G + 1, z, "minecraft:blue_ice")
                    b.set(x, G + 2, z, "guhs:ijspegelguh_kristal", {"facing": "up", "waterlogged": "false"})
                else:
                    b.set(x, G + 1, z, "minecraft:packed_ice")
                if (x + z) % 2 == 0 and b.get(x, G - dik - 1, z) in (None, AIR):
                    b.set(x, G - dik - 1, z, "guhs:ijspegelguh_kristal", {"facing": "down", "waterlogged": "false"})
    return list(range(min(dek), max(dek) + 1))


def routepalen(b, L, lijn):
    """The marker poles: a pair every ROUTEPAAL_STAP blocks, just outside the track (not on the bridge or in the town)."""
    T = L["T"]
    palen = []
    stap = int(ROUTEPAAL_STAP / 0.5)
    for i in range(6, len(lijn) - 4, stap):
        x, z, dy, s = lijn[i]
        tx, tz = lijn[min(i + 1, len(lijn) - 1), :2] - lijn[max(i - 1, 0), :2]
        n = math.hypot(tx, tz) or 1
        nx, nz = -tz / n, tx / n
        for side in (-1, 1):
            px, pz = int(round(x + nx * side * (BREED + 1.6))), int(round(z + nz * side * (BREED + 1.6)))
            if not (0 < px < SIZE - 1 and 0 < pz < SIZE - 1) or L["kloof"][pz, px] or L["spoor"][pz, px]:
                continue
            y = int(T[pz, px]) + 1
            if b.get(px, y, pz) not in (None, AIR):
                continue
            b.set(px, y, pz, PAAL)
            palen.append((px, y, pz, i))
    return palen


def lawineberg(b, L, lijn, rng):
    """The Lawineberg's top: loose snow drifts on the crest (the avalanche's snow), big snow boulders on the slope, and a
    warning sign at both ends of its stretch."""
    T = L["T"]
    for x in range(98, 138):
        for z in range(24, 42):
            top = int(T[z, x])
            if top >= G + 17 and rng.random() < 0.55:
                b.set(x, top + 1, z, "minecraft:snow", {"layers": str(rng.randint(2, 6))})
            elif G + 6 < top < G + 16 and rng.random() < 0.05:
                b.set(x, top + 1, z, "minecraft:snow_block")
    (ax, az), (bx, bz) = LAWINE
    for (x, z, key) in ((ax + 3, az + 6, "la"), (bx - 3, bz + 6, "lb")):
        y = int(T[z, x]) + 1
        b.set(x, y, z, "minecraft:spruce_fence", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
        b.bord(x, y + 1, z, "north", [(f"{key}1", "Pas op!"), (f"{key}2", "Lawinegevaar"), (f"{key}3", "Doorrijden, njeg!")], muur=False,
               kleur="red")


def rustpunt(b, L, lijn, naam, bij, nummer, tekst):
    """A rest point beside the track: a flat lay-by with an open shelter (a snowy roof on four posts), a vuurkorf that burns
    at night, two benches, a lamp post and a sign. Returns (the lay-by's spot, the route index)."""
    T = L["T"]
    d = np.hypot(lijn[:, 0] - bij[0], lijn[:, 1] - bij[1])
    i = int(d.argmin())
    x, z, dy, s = lijn[i]
    y = G + int(round(dy))
    tx, tz = lijn[min(i + 2, len(lijn) - 1), :2] - lijn[max(i - 2, 0), :2]
    n = math.hypot(tx, tz) or 1
    nx, nz = -tz / n, tx / n
    # the side where the land is lowest (easier to flatten, a view)
    beste = None
    for side in (-1, 1):
        cx, cz = int(round(x + nx * side * (BREED + 5))), int(round(z + nz * side * (BREED + 5)))
        if 3 < cx < SIZE - 4 and 3 < cz < SIZE - 4 and not L["kloof"][cz, cx]:
            hoogte = int(T[cz, cx])
            if beste is None or abs(hoogte - y) < beste[0]:
                beste = (abs(hoogte - y), side, cx, cz)
    _, side, cx, cz = beste
    for px in range(cx - 3, cx + 4):
        for pz in range(cz - 3, cz + 4):
            if L["spoor"][pz, px]:
                continue
            T[pz, px] = y
            b.set(px, y, pz, "minecraft:spruce_planks" if abs(px - cx) <= 2 and abs(pz - cz) <= 2 else SNEEUW)
            for yy in range(y - 4, y):
                if b.get(px, yy, pz) in (None, AIR):
                    b.set(px, yy, pz, "minecraft:dirt")
            for yy in range(y + 1, y + 6):
                b.set(px, yy, pz, AIR)
    # the shelter: four posts, a slab roof, a vuurkorf in the middle, benches on two sides
    for (px, pz) in ((cx - 2, cz - 2), (cx + 2, cz - 2), (cx - 2, cz + 2), (cx + 2, cz + 2)):
        for yy in range(y + 1, y + 4):
            b.hek(px, yy, pz)
    for px in range(cx - 3, cx + 4):
        for pz in range(cz - 3, cz + 4):
            b.plaat(px, y + 4, pz, geb.DAK_PLAAT)
    b.set(cx, y + 1, cz, geb.VUURKORF, {"lit": "false"})
    b.set(cx - 1, y + 1, cz + 2, "guhs:guh_bank", {"facing": "north"})
    b.set(cx + 1, y + 1, cz - 2, "guhs:guh_bank", {"facing": "south"})
    b.lantaarn(cx, y + 3, cz - 2, hangend=True)
    b.bord(cx - 3, y + 1, cz, "west", [(f"rust{nummer}a", f"Rustpuntje {nummer}"), (f"rust{nummer}b", tekst), (f"rust{nummer}c", "Even opwarmen!")],
           muur=False)
    return (cx, y + 1, cz), i


def wolvenrots(b, L, rng):
    """The Wolvenrots at the dieptepunt: a snowy rock ledge on the north hill with a paw print carved on it."""
    T = L["T"]
    x0, z0 = DIEPTEPUNT
    for x in range(x0 - 3, x0 + 4):
        for z in range(z0 - 9, z0 - 4):
            d = math.hypot(x - x0, z - (z0 - 6.5))
            if d > 3.6:
                continue
            top = max(int(T[z, x]), G + 4 - int(d))
            for y in range(G - 2, top + 1):
                b.set(x, y, z, "minecraft:stone" if y < top else ("minecraft:snow_block" if d > 1.2 else "minecraft:chiseled_stone_bricks"))
            T[z, x] = top
    b.bord(x0 + 4, int(T[z0 - 5, x0 + 4]) + 1, z0 - 5, "south", [("wr1", "Wolvenrots"), ("wr2", "Wie hier huilt,"), ("wr3", "wordt gehoord.")],
           muur=False)


# =====================================================================================================================
# the town
# =====================================================================================================================
HUIZEN = [  # x0, z0, W, D, door, colour, name sign
    (121, 83, 11, 9, "south", 0, [("h1a", "Huize Knabbel"), ("h1b", "fam. Vadsema")]),
    (84, 104, 9, 11, "east", 1, [("h2a", "De Ijspegel"), ("h2b", "bakkerij-guh")]),
    (84, 121, 9, 10, "east", 2, [("h3a", "Villa Wantje"), ("h3b", "Oma Pluis")]),
    (103, 131, 11, 9, "north", 3, [("h4a", "Het Sneeuwnest")]),
    (120, 131, 11, 9, "north", 4, [("h5a", "Huize Snuf"), ("h5b", "de postguh")]),
    (163, 99, 9, 11, "west", 5, [("h6a", "De Warme Poot")]),
    (163, 115, 9, 11, "west", 0, [("h7a", "Huize Knus")]),
    (139, 132, 11, 9, "north", 1, [("h8a", "De Sledemaker")]),
]
ZIEKENHUIS = (134, 99)
STAL = (74, 80)
BOOT = (48, 96)                 # the boat's corner (7 x 15, bow north)
IGLO = (84, 164)
BERGHUT = (33, 51)              # the hut's corner (13 x 11); floor at G + 17


def straat(b, L, a, c, breed=1):
    """A snowy street (track snow) between two points (a line of breed on both sides of it)."""
    T = L["T"]
    ax, az = a
    cx, cz = c
    n = int(max(abs(cx - ax), abs(cz - az))) + 1
    for k in range(n + 1):
        t = k / n
        x, z = round(ax + (cx - ax) * t), round(az + (cz - az) * t)
        for dx in range(-breed, breed + 1):
            for dz in range(-breed, breed + 1):
                px, pz = x + dx, z + dz
                if 0 <= px < SIZE and 0 <= pz < SIZE and T[pz, px] == G and b.get(px, G, pz) in (SNEEUW, None, "minecraft:snow_block"):
                    b.set(px, G, pz, SPOOR)


def stad(b, L, rng, info):
    T = L["T"]
    ms = b.ms
    # a flat pad under every building (the town is flat already, but make sure)
    for (x0, z0, W, D, *_rest) in HUIZEN:
        for x in range(x0 - 2, x0 + W + 2):
            for z in range(z0 - 2, z0 + D + 2):
                T[z, x] = G
    # the square with the tree, the benches, the pedestal, the kiosk; the anchor jigsaw in its middle
    info["plein"] = geb.plein(b, ANKER[0], ANKER[2], G)
    # the streets: a ring round the square and streets to every building
    ax, az = ANKER[0], ANKER[2]
    for a in range(0, 360, 3):
        x, z = ax + round(15 * math.cos(math.radians(a))), az + round(15 * math.sin(math.radians(a)))
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if T[z + dz, x + dx] == G and b.get(x + dx, G, z + dz) in (SNEEUW, None):
                    b.set(x + dx, G, z + dz, SPOOR)
    straat(b, L, (ax - 12, az - 9), (90, 94))                 # to the stable
    straat(b, L, (ax + 15, az - 2), (ZIEKENHUIS[0] - 1, az - 2))   # to the hospital
    straat(b, L, (ax + 15, az + 2), (160, az + 2))            # to the east cottages
    straat(b, L, (ax - 15, az), (70, az))                     # to the bay shore (the boat)
    straat(b, L, (ax - 6, az + 14), (IGLO[0] + 8, IGLO[1] - 4))   # to the igloo and the pond
    straat(b, L, (ax + 6, az + 14), (ax + 6, 131))
    straat(b, L, (ax + 30, az + 14), (144, 131))
    # the cottages (a few with a guh inside)
    info["huizen"] = []
    for k, (x0, z0, W, D, deur, kleur, naam) in enumerate(HUIZEN):
        spot = geb.huisje(b, x0, z0, W, D, deur, G, kleur, naam=naam, bewoner=0.9 if k % 3 == 0 else None)
        info["huizen"].append(spot)
    # the hospital, the stable (the start line in front of it), the boat, the igloo and the pond
    info["ziekenhuis"], info["rosy"] = geb.ziekenhuisje(b, ZIEKENHUIS[0], ZIEKENHUIS[1], G)
    info["stal"] = geb.stal(b, STAL[0], STAL[1], G)
    startlijn(b, info)
    info["boot"], info["balto"] = geb.boot(b, BOOT[0], BOOT[1], G - 1)
    b.npc(*info["boot"], "boris", kijk="east")
    # Baltoguh's story copy, on the ice beside his boat (VerhaalGuhs marks him on join; his spot is where he stands)
    bx, by, bz = info["balto"]
    b.s.entity(bx + 0.5, float(by), bz + 0.5, ms.guh_nbt(1.25, Variant="baltoguh", Rotation=ms.floats(-90.0, 0.0),
                                                           NeoForgeData={"guhs_verhaal_guh": "baltoguh"}))
    geb.iglo(b, IGLO[0], IGLO[1], G + 1)
    geb.vijver(b, VIJVER[0][0], VIJVER[0][1], VIJVER[1], G - 1)
    b.npc(IGLO[0] + 8, G + 1, IGLO[1] + 2, "muk", kijk="east")
    b.npc(IGLO[0] + 9, G + 1, IGLO[1] + 5, "luk", kijk="east")
    b.bord(IGLO[0] + 7, G + 1, IGLO[1] + 7, "east", [("ig1", "Muk & Luk"), ("ig2", "IJsvissen"), ("ig3", "Knuffels gratis")], muur=False)
    # a Reisguh on the square
    info["reisguh"] = reisguh_plek.zet(b.s, reisguh_plek.rondom(ax - 5, G + 1, az + 9, 3), "Nomguh", 0, ms.Byte, ms.floats, "nomguh")
    # lamp posts along the streets, sneeuwpopguhs, a few guhs out for a walk, welcome signs
    for (x, z) in ((90, 97), (100, 88), (128, 112), (150, 114), (160, 124), (100, 150), (118, 150), (96, 160), (140, 128), (74, 114)):
        if T[z, x] == G and b.get(x, G + 1, z) in (None, AIR):
            geb.lamppaal(b, x, G + 1, z)
    for (x, z, f) in ((106, 150, "north"), (150, 96, "west"), (80, 140, "east")):
        if b.get(x, G + 1, z) in (None, AIR):
            b.set(x, G + 1, z, "guhs:sneeuwpopguh", {"facing": f, "half": "lower"})
            b.set(x, G + 2, z, "guhs:sneeuwpopguh", {"facing": f, "half": "upper"})
    for (x, z, v) in ((118, 120, "normal"), (104, 118, "snow"), (122, 104, "choco")):
        b.guh(x, G + 1, z, kijk="south", schaal=0.8 + 0.1 * rng.random(), Variant=v)
    b.bord(70, G + 1, 116, "west", [("w1", "Welkom in"), ("w2", "NOMGUH"), ("w3", "brr, njeg!")], muur=False)
    b.bord(158, G + 1, 80, "north", [("w4", "Nomguh"), ("w5", "↑ Stormdal"), ("w6", "↓ Stad")], muur=False)


def bomen(b, L, rng):
    """Groves of sneeuwguhsparren in the wild parts (never on the route, in the town, on the ice, in the gorge or against a
    building): a jittered grid, denser on the hills round the storm valley and at the town's edge. Returns how many."""
    T = L["T"]
    n = 0
    for gz in range(4, SIZE - 4, 8):
        for gx in range(4, SIZE - 4, 8):
            x, z = gx + rng.randint(-3, 3), gz + rng.randint(-3, 3)
            if not (5 <= x < SIZE - 5 and 5 <= z < SIZE - 5):
                continue
            if L["stad"][z, x] or L["ijs"][z, x] or L["lat"][z, x] < BREED + 8:
                continue
            top = int(T[z, x])
            if top < G - 1 or top > G + 32 or rng.random() < 0.35:
                continue
            vrij = True
            for dx in range(-4, 5):
                for dz in range(-4, 5):
                    xx, zz = x + dx, z + dz
                    if L["kloof"][zz, xx] or L["ijs"][zz, xx] or L["spoor"][zz, xx] or abs(int(T[zz, xx]) - top) > 3:
                        vrij = False
                        break
                    for y in range(int(T[zz, xx]) + 1, int(T[zz, xx]) + 14):
                        if b.get(xx, y, zz) not in (None, AIR):
                            vrij = False
                            break
                    if not vrij:
                        break
                if not vrij:
                    break
            if not vrij or top + 16 >= H - 1:
                continue
            geb.spar(b, x, top + 1, z, hoog=rng.choice((6, 7, 8, 9, 11)))
            n += 1
    return n


def startlijn(b, info):
    """The start line in front of the stable: a banner arch over the track, Steele-Mika (plek "sledesprint": balto-slee's
    race and his little shop) beside it, a stack of sleds and a bell post."""
    sx, sy, sz = info["stal"]
    x0, x1 = int(ROUTE[0][0]) - 5, int(ROUTE[0][0]) + 5
    z = int(ROUTE[0][1]) - 1
    for x in (x0, x1):
        for y in range(G + 1, G + 6):
            b.log(x, y, z)
    for x in range(x0, x1 + 1):
        b.log(x, G + 6, z, axis="x")
    for x in range(x0 + 1, x1, 2):
        b.banier(x, G + 5, z, "red" if x % 4 == 1 else "white", [("white", "stripe_middle")] if x % 4 == 1 else [("red", "stripe_middle")],
                 facing="south")
    b.lantaarn(x0, G + 7, z)
    b.lantaarn(x1, G + 7, z)
    b.bord(x1 + 1, G + 3, z, "east", [("sl1", "START"), ("sl2", "Naar de berghut"), ("sl3", "volg de paaltjes!")], muur=False)
    b.npc(x1 + 2, G + 1, z + 3, "steele_mika", kijk="west", roledata={"guhs_plek": "sledesprint"})
    info["steele"] = (x1 + 2, G + 1, z + 3)
    for y in range(G + 1, G + 3):
        b.hek(x0 - 2, y, z + 2)
    b.set(x0 - 2, G + 3, z + 2, "bell", {"attachment": "floor", "facing": "east", "powered": "false"})


# =====================================================================================================================
# build
# =====================================================================================================================
def build(h):
    rng = random.Random(20300701)
    s = h.Structure((SIZE, H, SIZE))
    b = geb.Bouwer(s, h, rng)
    lijn = route_lijn()
    L = landschap(lijn)
    info = {"lijn": lijn}
    kolommen(b, L)
    info["brug"] = ijsbrug(b, L, lijn)
    lawineberg(b, L, lijn, rng)
    wolvenrots(b, L, rng)
    info["rust"] = []
    for n, (naam, bij) in enumerate(RUST, start=1):
        tekst = {"stormdal": "Het Stormdal", "bocht": "De Grote Bocht", "lawine": "Na de Lawineberg", "bergkam": "De Bergkam"}[naam]
        spot, i = rustpunt(b, L, lijn, naam, bij, n, tekst)
        info["rust"].append((spot, i))
    stad(b, L, rng, info)
    # the berghut on the Hutkop (the route ends at its porch)
    hy = G + int(round(lijn[-1, 2]))
    for x in range(BERGHUT[0] - 3, BERGHUT[0] + 18):
        for z in range(BERGHUT[1] - 3, BERGHUT[1] + 14):
            if L["spoor"][z, x]:
                continue
            L["T"][z, x] = max(L["T"][z, x], hy)
            b.set(x, hy, z, SNEEUW)
            for y in range(hy - 6, hy):
                if b.get(x, y, z) in (None, AIR):
                    b.set(x, y, z, "minecraft:dirt")
            for y in range(hy + 1, hy + 12):
                b.set(x, y, z, AIR)
    info["berghut"], info["kist"] = geb.berghut(b, BERGHUT[0], BERGHUT[1], hy)
    info["palen"] = routepalen(b, L, lijn)
    info["bomen"] = bomen(b, L, rng)
    # the anchor: the jigsaw in the middle of the square (turns into cobbles)
    ax, ay, az = ANKER
    s.set(ax, ay, az, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": f"guhs:{NAME}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": geb.KLINKERS, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    info["L"] = L
    info["npcs"] = b.npcs
    return s, info


# =====================================================================================================================
# the route file (CONTRACT_30 §4.9)
# =====================================================================================================================
def route_json(info):
    lijn, L = info["lijn"], info["L"]
    # a point every PUNT_STAP blocks along the centreline; its y the top block of the track there
    stap = int(PUNT_STAP / 0.5)
    keuze = list(range(0, len(lijn), stap))
    if keuze[-1] != len(lijn) - 1:
        keuze.append(len(lijn) - 1)
    punten = []
    for i in keuze:
        x, z, dy, s = lijn[i]
        top = int(L["T"][int(round(z)), int(round(x))])
        punten.append([round(float(x), 2), top, round(float(z), 2), BREED])

    def punt(i):
        return min(range(len(keuze)), key=lambda k: abs(keuze[k] - i))
    rust = [punt(i) for _, i in info["rust"]]
    brug = [punt(min(info["brug"])), punt(max(info["brug"]))]
    d = np.hypot(lijn[:, 0] - LAWINE[0][0], lijn[:, 1] - LAWINE[0][1])
    e = np.hypot(lijn[:, 0] - LAWINE[1][0], lijn[:, 1] - LAWINE[1][1])
    lawine = [punt(int(d.argmin())), punt(int(e.argmin())), "links"]
    dp = np.hypot(lijn[:, 0] - DIEPTEPUNT[0], lijn[:, 1] - DIEPTEPUNT[1])
    lengte = float(lijn[-1, 3])
    return {"versie": 1, "anker": list(ANKER), "punten": punten, "terug": "zelfde",
            "stal": list(info["stal"]), "ziekenhuis": list(info["ziekenhuis"]), "berghut": list(info["berghut"]),
            "rust": rust, "ijsbrug": [brug], "lawine": [lawine], "dieptepunt": punt(int(dp.argmin())),
            "lengte": round(lengte, 1), "tijd_ticks": {"makkelijk": 4800, "medium": 3600, "lastig": 2800}}


# =====================================================================================================================
# the self-check
# =====================================================================================================================
def blokken_bestaan(s):
    """Every block of the template exists (vanilla: in the game's jar; ours: a blockstate file)."""
    import os
    import zipfile
    jar = os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")
    vanilla = None
    if os.path.exists(jar):
        vanilla = {n.split("/")[-1][:-5] for n in zipfile.ZipFile(jar).namelist() if n.startswith("assets/minecraft/blockstates/")}
    fout = []
    for name in sorted({v[0] for v in s.blocks.values()}):
        ns, _, path = name.partition(":")
        if name.count(":") != 1:
            fout.append(f"bad block id {name}")
        elif ns == "guhs" and not os.path.exists(os.path.join("src", "main", "resources", "assets", "guhs", "blockstates", path + ".json")):
            fout.append(f"unknown block {name}")
        elif ns == "minecraft" and vanilla is not None and path not in vanilla:
            fout.append(f"unknown block {name}")
    return fout


def check(s, info, route):
    """The route file on the template: every point on the track (a track block, 3 blocks of air above it for the sled),
    walkable (no step over 1 between neighbouring points), markers along it (on every 20 blocks at least one), the bridge and
    the special places; the NPCs stand on a floor; the anchor; nothing reaches the template's top."""
    problems = []
    P = route["punten"]
    for k, (x, y, z, br) in enumerate(P):
        bx, bz = int(round(x)), int(round(z))
        top = s.get(bx, y, bz)
        if top not in (SPOOR, "minecraft:blue_ice", "minecraft:packed_ice"):
            problems.append(f"point {k} ({x}, {y}, {z}): the top block is {top}")
        for dy in range(1, 4):
            if s.get(bx, y + dy, bz) not in (None, AIR, "minecraft:snow"):
                problems.append(f"point {k} ({x}, {y}, {z}): blocked at +{dy} by {s.get(bx, y + dy, bz)}")
                break
        if k and abs(P[k][1] - P[k - 1][1]) > 1:
            problems.append(f"point {k}: a step of {P[k][1] - P[k - 1][1]} from the point before")
        if k and math.hypot(P[k][0] - P[k - 1][0], P[k][2] - P[k - 1][2]) > 4.01:
            problems.append(f"point {k}: more than 4 blocks from the point before")
    palen = info["palen"]
    lijn = info["lijn"]
    L = float(lijn[-1, 3])
    for s0 in np.arange(10, L - 10, 20):
        i0, i1 = int(s0 / 0.5), int((s0 + 20) / 0.5)
        brug = set(info["brug"])
        if any(i in brug for i in range(i0, i1)):
            continue
        if not any(i0 <= p[3] <= i1 for p in palen):
            problems.append(f"no route marker between {s0:.0f} and {s0 + 20:.0f} blocks")
    if len(route["rust"]) < 3 or len(route["rust"]) > 5:
        problems.append("3-5 rest points")
    if s.get(*ANKER) != "minecraft:jigsaw":
        problems.append("no anchor jigsaw")
    for kind, (x, y, z) in info["npcs"]:
        if s.get(x, y - 1, z) in (None, AIR) or s.get(x, y, z) not in (None, AIR):
            problems.append(f"NPC {kind} at {(x, y, z)} doesn't stand on a floor ({s.get(x, y - 1, z)} / {s.get(x, y, z)})")
    kist = info["kist"]
    if s.get(*kist) != geb.KIST:
        problems.append("the medicine chest isn't on the berghut's table")
    top = max(y for (x, y, z) in s.blocks)
    if top >= H - 1:
        problems.append(f"blocks at the template's top ({top})")
    kinds = {k for k, _ in info["npcs"]}
    for k in ("boris", "steele_mika", "muk", "luk", "rosy"):
        if k not in kinds:
            problems.append(f"no {k}")
    if not any(e[3].get("NeoForgeData", {}).get("guhs_verhaal_guh") == "baltoguh" for e in s.entities):
        problems.append("no Baltoguh story copy")
    if reisguh_plek.aantal(s) != 1:
        problems.append("one Reisguh")
    problems += blokken_bestaan(s)
    if problems:
        raise SystemExit("nomguh check failed:\n  " + "\n  ".join(problems[:40]))
    return {"punten": len(P), "lengte": route["lengte"], "palen": len(palen), "rust": len(route["rust"]), "brug": route["ijsbrug"],
            "blokken": len(s.blocks), "bomen": info["bomen"]}
