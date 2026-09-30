"""
De Elf-Guhjestocht (2.10 rebuild) - the land of the polder between the villages: everything that makes it look like a
real, lived-in Frisian winter polder instead of a flat grid.

  sloten(s, ice, reserved, rng, info)      5-8 organic, wobbly ditches of ice that wander through the open polder (most
                                           of them run into the canal, like real sloten) with rows of knotwilgen along
                                           one bank and now and then a little guh-molentje
  knotwilg_groepjes(s, ice, reserved, rng, info)   small groups of pollard willows standing together in the fields
  riet(s, ice, reserved, rng, info)        reed clumps (kaasriet, tall grass, rijpsprietjes) on the banks of the canal and
                                           the ditches
  verspreid(...)                           spots spread evenly over the free polder (for the atmosphere pieces)
  reliëf(s, ice, info)                     a gentle snowy relief of at most ~2 blocks (snow blocks + snow layers) over the
                                           open polder: 0 at the ice banks and around everything that stands there (flat
                                           pads under objects, audience and trees), rising slowly away from them
  plantjes(s, ice, reserved, rng)          frosted grass tufts and ice flowers on the flat bits of polder

All of it stands on the template's ground layer (y = GY) and hangs together (the core's floating check).
"""
import math

from features import elftocht_bouw as B
from features import elftocht_route as R

GY, OY = R.GY, R.OY
SNOW = "minecraft:snow"
SNOWBLOCK = "minecraft:snow_block"
MAX_RELIEF = 2.0          # blocks


# =====================================================================================================================
# helpers
# =====================================================================================================================
def _vrij(s, x, z, ice, reserved, marge_plot=2, extra=()):
    return (2 < x < R.SIZE_X - 3 and 2 < z < R.SIZE_Z - 3 and (x, z) not in ice and (x, z) not in reserved
            and (x, z) not in extra and not R.in_plot(x, z, margin=marge_plot) and not B.near_start(x, z)
            and s.get(x, OY, z) is None and s.get(x, GY, z) == B.GRAS)


def afstand(bezet, size=(R.SIZE_X, R.SIZE_Z)):
    """A chamfer (1 / 1.4) distance field to the nearest occupied cell: {(x, z): distance} for the whole square."""
    sx, sz = size
    big = 1e9
    d = [[0.0 if (x, z) in bezet else big for z in range(sz)] for x in range(sx)]
    for x in range(sx):
        for z in range(sz):
            v = d[x][z]
            if v == 0.0:
                continue
            if x > 0:
                v = min(v, d[x - 1][z] + 1)
                if z > 0:
                    v = min(v, d[x - 1][z - 1] + 1.4)
                if z < sz - 1:
                    v = min(v, d[x - 1][z + 1] + 1.4)
            if z > 0:
                v = min(v, d[x][z - 1] + 1)
            d[x][z] = v
    for x in range(sx - 1, -1, -1):
        for z in range(sz - 1, -1, -1):
            v = d[x][z]
            if v == 0.0:
                continue
            if x < sx - 1:
                v = min(v, d[x + 1][z] + 1)
                if z > 0:
                    v = min(v, d[x + 1][z - 1] + 1.4)
                if z < sz - 1:
                    v = min(v, d[x + 1][z + 1] + 1.4)
            if z < sz - 1:
                v = min(v, d[x][z + 1] + 1)
            d[x][z] = v
    return d


def _ruis2(x, z, schaal, seed):
    """Smooth value noise (0..1) on a grid of `schaal` blocks (cosine interpolation, hashed corners)."""
    def h(i, j):
        n = (i * 374761393 + j * 668265263 + seed * 2147483647) & 0xFFFFFFFF
        n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
        return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0

    fx, fz = x / schaal, z / schaal
    i, j = math.floor(fx), math.floor(fz)
    tx, tz = fx - i, fz - j
    tx = (1 - math.cos(tx * math.pi)) / 2
    tz = (1 - math.cos(tz * math.pi)) / 2
    a = h(i, j) * (1 - tx) + h(i + 1, j) * tx
    b = h(i, j + 1) * (1 - tx) + h(i + 1, j + 1) * tx
    return a * (1 - tz) + b * tz


def verspreid(s, ice, reserved, n, afstand_ijs=(3, 40), onderling=24, rng=None, andere=None, rand=8):
    """n spots (x, z) on free polder, spread out: each time the free cell farthest from everything chosen so far (and
    from `andere`), keeping afstand_ijs[0] <= distance to the canal <= afstand_ijs[1]. A little random jitter so it
    doesn't look like a grid."""
    if andere is None:
        # away from the villages (the middles of their plots)
        andere = [((b[0] + b[2]) / 2, (b[1] + b[3]) / 2) for b in (R.plot_box(i) for i in range(1, 12))]
    kandidaten = []
    for x in range(rand, R.SIZE_X - rand, 3):
        for z in range(rand, R.SIZE_Z - rand, 3):
            if not _vrij(s, x, z, ice, reserved, marge_plot=4):
                continue
            if B.near_ice(x, z, ice, afstand_ijs[0]) or not B.near_ice(x, z, ice, afstand_ijs[1]):
                continue
            kandidaten.append((x + (rng.randrange(-1, 2) if rng else 0), z + (rng.randrange(-1, 2) if rng else 0)))
    gekozen = []
    punten = list(andere)
    if rng:
        rng.shuffle(kandidaten)
    for _ in range(n):
        best, bd = None, -1
        for (x, z) in kandidaten:
            if any(math.hypot(x - a, z - b) < onderling for a, b in gekozen):
                continue
            d = min((math.hypot(x - a, z - b) for a, b in punten), default=1e9)
            if d > bd:
                best, bd = (x, z), d
        if best is None:
            break
        gekozen.append(best)
        punten.append(best)
    return gekozen


# =====================================================================================================================
# the ditches
# =====================================================================================================================
def _stapjes(x0, z0, hoek, lengte, seed):
    """A wobbly line from (x0, z0) in direction `hoek`: a list of cells, 4-connected (no diagonal gaps in the ice)."""
    cellen = []
    x, z = float(x0), float(z0)
    last = (int(round(x)), int(round(z)))
    cellen.append(last)
    for i in range(1, lengte + 1):
        a = hoek + 0.55 * math.sin(i / 9.0 + seed) + 0.28 * math.sin(i / 3.7 + seed * 2.3)
        x += math.cos(a)
        z += math.sin(a)
        c = (int(round(x)), int(round(z)))
        if c == last:
            continue
        if c[0] != last[0] and c[1] != last[1]:
            cellen.append((c[0], last[1]))
        cellen.append(c)
        last = c
    return cellen


def sloten(s, ice, reserved, rng, info):
    """5-8 ditches through the open polder. Each starts in the middle of an open field and wanders both ways until it
    reaches the canal (it joins it) or comes near something (it stops). Knotwilgen stand in a row along one bank, a
    guh-molentje at some of them. Returns the set of ditch cells (their ice)."""
    sloot = set()
    # where there is room: far from the canal, the plots and everything built
    bezet = set(reserved)
    for x in range(R.SIZE_X):
        for z in range(R.SIZE_Z):
            if (x, z) in ice:
                continue
            if R.in_plot(x, z, margin=3) or s.get(x, OY, z) is not None or B.near_start(x, z) or s.get(x, GY, z) != B.GRAS:
                bezet.add((x, z))
    veld = afstand(bezet)                       # room: away from everything built (the canal not counted)
    open_veld = afstand(bezet | set(ice))       # where to start: in the middle of an open field
    gedaan = []
    pogingen = 0
    while len(gedaan) < 8 and pogingen < 40:
        pogingen += 1
        # the most open spot not near an earlier ditch
        best, bd = None, 0
        for x in range(20, R.SIZE_X - 20, 2):
            for z in range(20, R.SIZE_Z - 20, 2):
                d = open_veld[x][z]
                if d < 9:
                    continue
                if any(math.hypot(x - a, z - b) < 38 for a, b in gedaan):
                    continue
                d += rng.random() * 3
                if d > bd:
                    best, bd = (x, z), d
        if best is None:
            break
        gedaan.append(best)
        cx, cz = best
        # the direction with the longest free run
        hoeken = [i * math.pi / 8 for i in range(8)]

        def vrije_run(h):
            n = 0
            for sgn in (1, -1):
                for k in range(1, 80):
                    x, z = int(round(cx + sgn * k * math.cos(h))), int(round(cz + sgn * k * math.sin(h)))
                    if not (3 < x < R.SIZE_X - 4 and 3 < z < R.SIZE_Z - 4) or veld[x][z] < 2:
                        break
                    n += 1
            return n

        hoek = max(hoeken, key=lambda h: vrije_run(h) + rng.random() * 8)
        seed = rng.random() * 10
        lijn = []
        naar_kanaal = 0
        for sgn in (1, -1):
            cellen = _stapjes(cx, cz, hoek if sgn > 0 else hoek + math.pi, 90, seed + (0 if sgn > 0 else 5.1))
            stuk = []
            for c in cellen[1:] if sgn < 0 else cellen:
                x, z = c
                if not (10 <= x < R.SIZE_X - 10 and 10 <= z < R.SIZE_Z - 10):
                    stuk = stuk[:-1]
                    break
                if c in ice:
                    naar_kanaal += 1
                    break
                if any((x + a, z + b) in ice for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    stuk.append(c)          # it runs into the canal: the last cell touches the ice
                    naar_kanaal += 1
                    break
                if veld[x][z] < 3 or c in sloot or any((x + a, z + b) in sloot for a in (-2, -1, 0, 1, 2) for b in (-2, -1, 0, 1, 2)):
                    stuk = stuk[:-2]
                    break
                stuk.append(c)
            lijn.extend(stuk if sgn > 0 else list(reversed(stuk)))
        if len(lijn) < 26:
            continue
        # a ditch is 1 wide, here and there 2
        breed = set(lijn)
        for i, (x, z) in enumerate(lijn):
            if math.sin(i / 7.0 + seed) > 0.55:
                nx, nz = (x + 1, z) if i % 2 else (x, z + 1)
                if veld[nx][nz] >= 3 and (nx, nz) not in ice:
                    breed.add((nx, nz))
        for (x, z) in breed:
            s.set(x, GY, z, B.ICE)
            s.set(x, GY - 1, z, B.ICE)
        sloot |= breed
        info.setdefault("sloten", []).append({"cellen": len(breed), "kanaal": naar_kanaal})
        # knotwilgen in a row along one bank, now and then a molentje
        kant = 1 if rng.random() < 0.5 else -1
        stap = rng.choice((6, 7, 8))
        for i in range(3, len(lijn) - 3, stap):
            (x0, z0), (x1, z1) = lijn[max(0, i - 3)], lijn[min(len(lijn) - 1, i + 3)]
            tx, tz = x1 - x0, z1 - z0
            n = math.hypot(tx, tz) or 1.0
            nx, nz = -tz / n * kant, tx / n * kant
            wx, wz = int(round(lijn[i][0] + nx * 3)), int(round(lijn[i][1] + nz * 3))
            if _boom_past(s, wx, wz, ice, reserved, sloot):
                B.knotwilg(s, wx, wz, rng)
                _reserveer(reserved, wx, wz, 2)
            elif rng.random() < 0.5:
                mx, mz = int(round(lijn[i][0] + nx * 2)), int(round(lijn[i][1] + nz * 2))
                if _vrij(s, mx, mz, ice, reserved, extra=sloot) and not B.near_ice(mx, mz, ice, 3):
                    s.set(mx, OY, mz, B.mc("spruce_fence"))
                    s.set(mx, OY + 1, mz, B.mc("spruce_fence"))
                    s.set(mx, OY + 2, mz, "guhs:guh_molentje")
                    reserved.add((mx, mz))
    info["sloot_cellen"] = sloot
    return sloot


def _boom_past(s, x, z, ice, reserved, extra=()):
    """Room for a knotwilg (a 5 x 5 head) here, and far enough from the canal that its leaves stay out of the skaters'
    headroom."""
    if B.near_ice(x, z, ice, 4):
        return False
    return all(_vrij(s, x + a, z + b, ice, reserved, extra=extra) for a in range(-2, 3) for b in range(-2, 3))


def _reserveer(reserved, x, z, r):
    for a in range(-r, r + 1):
        for b in range(-r, r + 1):
            reserved.add((x + a, z + b))


def knotwilg_groepjes(s, ice, reserved, rng, info, n=6):
    """Little groups (2-4) of knotwilgen in the fields, spread over the polder."""
    geplant = 0
    for (cx, cz) in verspreid(s, ice, reserved, n, afstand_ijs=(7, 60), onderling=30, rng=rng):
        k = 0
        for _ in range(12):
            if k >= rng.choice((2, 3, 4)):
                break
            x, z = cx + rng.randrange(-5, 6), cz + rng.randrange(-5, 6)
            if _boom_past(s, x, z, ice, reserved, info.get("sloot_cellen", ())):
                B.knotwilg(s, x, z, rng)
                _reserveer(reserved, x, z, 2)
                k += 1
        geplant += k
    info["knotwilgen_groep"] = geplant
    return geplant


# =====================================================================================================================
# reed along the banks
# =====================================================================================================================
def _riet_cel(s, x, z, rng):
    r = rng.random()
    if r < 0.5:
        s.set(x, OY, z, "guhs:kaasriet", {"half": "lower"})
        s.set(x, OY + 1, z, "guhs:kaasriet", {"half": "upper"})
    elif r < 0.75:
        s.set(x, OY, z, B.mc("tall_grass"), {"half": "lower"})
        s.set(x, OY + 1, z, B.mc("tall_grass"), {"half": "upper"})
    else:
        s.set(x, OY, z, "guhs:rijpsprietjes")


def riet(s, ice, reserved, rng, info, n=46):
    """Reed clumps on the banks: little blobs of 3-8 stalks right at the edge of the canal (never on it) and along the
    ditches, spread along the whole route."""
    sloot = info.get("sloot_cellen", set())
    water = set(ice) | set(sloot)
    oever = [(x, z) for x in range(2, R.SIZE_X - 2) for z in range(2, R.SIZE_Z - 2)
             if (x, z) not in water and any((x + a, z + b) in water for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    rng.shuffle(oever)
    centra = []
    for (x, z) in oever:
        if len(centra) >= n:
            break
        if not _vrij(s, x, z, ice, reserved, marge_plot=3, extra=sloot):
            continue
        if any(math.hypot(x - a, z - b) < 17 for a, b in centra):
            continue
        # not in front of a bridge landing or a stall: nothing built within 2
        if any(s.get(x + a, OY, z + b) not in (None,) for a in (-2, -1, 0, 1, 2) for b in (-2, -1, 0, 1, 2)):
            continue
        centra.append((x, z))
    stengels = 0
    for (cx, cz) in centra:
        grootte = rng.randrange(3, 9)
        blob = [(cx, cz)]
        for _ in range(grootte * 4):
            if len(blob) >= grootte:
                break
            bx, bz = rng.choice(blob)
            a, b = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
            c = (bx + a, bz + b)
            if c in blob or not _vrij(s, c[0], c[1], ice, reserved, marge_plot=3, extra=sloot):
                continue
            if not B.near_ice(c[0], c[1], water, 1) and not any((c[0] + p, c[1] + q) in water for p in (-2, 2) for q in (0,)):
                continue
            blob.append(c)
        for (x, z) in blob:
            _riet_cel(s, x, z, rng)
            reserved.add((x, z))
            stengels += 1
    info["riet"] = (len(centra), stengels)
    return stengels


# =====================================================================================================================
# the relief
# =====================================================================================================================
def relief(s, ice, info):
    """Gentle snowy hills of at most MAX_RELIEF blocks over the open polder: snow blocks and a snow layer on top (in
    eighths). Zero on (and right next to) the canal, the ditches, the plots, the start and everything that stands on the
    ground; from there it rises over ~7 blocks to a smooth noise height. Returns {(x, z): height in eighths}."""
    sloot = info.get("sloot_cellen", set())
    vast = set(ice) | set(sloot)
    for x in range(R.SIZE_X):
        for z in range(R.SIZE_Z):
            if (x, z) in vast:
                continue
            if (R.in_plot(x, z, margin=1) or B.near_start(x, z) or s.get(x, GY, z) != B.GRAS
                    or any(s.get(x, y, z) is not None for y in range(OY, OY + 6))):
                vast.add((x, z))
    for (ex, ey, ez, nbt) in s.entities:
        vast.add((int(math.floor(ex)), int(math.floor(ez))))
    vast.add((R.ANCHOR[0], R.ANCHOR[2]))
    for (x, z) in info.get("pads", ()):
        vast.add((x, z))
    veld = afstand(vast)
    hoogte = {}
    for x in range(1, R.SIZE_X - 1):
        for z in range(1, R.SIZE_Z - 1):
            if (x, z) in vast:
                continue
            d = veld[x][z]
            if d <= 1.0:
                continue
            t = min(1.0, (d - 1.0) / 7.0)
            ramp = t * t * (3 - 2 * t)
            n = 0.65 * _ruis2(x, z, 23.0, 7) + 0.35 * _ruis2(x, z, 9.0, 13)
            amp = max(0.0, min(1.0, (n - 0.36) / 0.5))
            h = MAX_RELIEF * amp * ramp
            # the edge of the template eases down too (the world's beard takes it from there)
            rand = min(x, z, R.SIZE_X - 1 - x, R.SIZE_Z - 1 - z)
            if rand < 8:
                h *= rand / 8.0
            achtsten = int(round(h * 8))
            if achtsten > 0:
                hoogte[(x, z)] = achtsten
    for (x, z), e in hoogte.items():
        vol, rest = divmod(e, 8)
        for k in range(vol):
            s.set(x, OY + k, z, SNOWBLOCK)
        if rest:
            s.set(x, OY + vol, z, SNOW, {"layers": str(rest)})
        if vol == 0:
            s.set(x, GY, z, B.GRAS, {"snowy": "true"})
    info["reliëf"] = hoogte
    return hoogte


# =====================================================================================================================
# plants on the flat polder
# =====================================================================================================================
def plantjes(s, ice, reserved, rng):
    """Frosted grass tufts, ice flowers and patches of thin snow on the free, flat polder (not on the relief: the plants
    need the frosty grass under them)."""
    n = 0
    for x in range(1, R.SIZE_X - 1):
        for z in range(1, R.SIZE_Z - 1):
            if (x, z) in ice or (x, z) in reserved or R.in_plot(x, z) or s.get(x, OY, z) is not None:
                continue
            if s.get(x, GY, z) != B.GRAS:
                continue
            r = rng.random()
            if B.near_ice(x, z, ice, 1):
                if r < 0.16:
                    s.set(x, OY, z, "guhs:rijpsprietjes")
                    n += 1
                continue
            if r < 0.07:
                s.set(x, OY, z, "guhs:rijpsprietjes")
                n += 1
            elif r < 0.085:
                s.set(x, OY, z, "guhs:guh_ijsbloempje")
                n += 1
            elif r < 0.13:
                s.set(x, OY, z, SNOW, {"layers": "1"})
    return n
