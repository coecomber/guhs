"""
Op Safari! step 2, slice S3 (vervoer & momenten): what the rides and the moments need in the park, built after every
zone module (NA_ZONES), plus the ride data the game reads (written by route_data.schrijf into assets/guhs/safari/, so the
server AND every client have the same paths):

  * the truck route: the route line (terrein.route) at standing height, a halte per zone (a sign, a bench, a lantern);
  * lantern posts along the paths (they light the park at night);
  * the canoe: a jetty at the village pond and a path up the river, back before the first low spot (the beaver dam);
  * the kabelbaan IJskust -> Bamboebergen: two stations (deck, ladder, roof), pylons and an iron-bar cable;
  * the struisvogelrace track on the savanna (an oval, a start arch, the sign of the scoreboard);
  * the schemabord at the village square (block guhs:safari_schemabord);
  * atmosphere spots (sfeer.json): the steam of the warme bronnen, the village fountain, the moment spots per zone.
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit
from .terrein import ZONES, RIVIER, MEREN, ROUTE, P
from .zone_savanne import zoek, vlak, onderbouw, bord, pad_afstand

W, H, WATER_Y = kit.W, kit.H, kit.WATER_Y
NA_ZONES = True       # safari_park.zone_modules runs this module after all zone modules

ZONE_NAAM = {"entree": "Entree", "savanne": "Savanne", "delta": "Rivierdelta", "regenwoud": "Regenwoud",
             "bamboe": "Bamboebergen", "ijskust": "IJskust", "oertijd": "Oerijstijd", "europa": "Europees Woud",
             "australie": "Australie"}

_VLOER = ("planks", "slab", "stairs", "path", "dirt", "sand", "gravel", "stone", "grass_block", "podzol", "mud", "ice",
          "terracotta", "brick", "block", "cobble", "andesite", "granite", "diorite", "clay", "calcite", "tuff",
          "deepslate", "concrete", "wool", "mosaic", "snow_block", "mycelium", "roots", "_wood", "prismarine")
_NIET = ("button", "_wall", "pressure", "fence", "gate", "_log", "lantern", "torch", "sign", "banner", "leaves",
         "carpet", "rail", "chain", "candle", "ladder", "bars", "pane", "glass", "trapdoor", "door", "chest")


def _vloer(s):
    n = s.split("[")[0]
    if any(w in n for w in _NIET):
        return False
    return any(w in n for w in _VLOER)


def staan(park, x, z):
    """Standing height on top of the walkable surface of column x,z (bridge decks, paths, slabs half a block)."""
    x, z = int(min(max(x, 0), W - 1)), int(min(max(z, 0), W - 1))
    h = int(park.hoogte[x, z])
    for y in range(min(H - 2, max(h, WATER_Y) + 9), max(h - 4, 1), -1):
        i = park.v[x, y, z]
        if not i:
            continue
        s = park.pal[i]
        if _vloer(s):
            return y + (0.5 if "_slab" in s and "type=bottom" in s else 1.0)
    return h + 1.0


def _herbemonster(punten, stap):
    """A polyline resampled every `stap` blocks (x, z)."""
    uit = [punten[0]]
    rest = 0.0
    for (ax, az), (bx, bz) in zip(punten, punten[1:]):
        d = math.hypot(bx - ax, bz - az)
        t = stap - rest
        while t <= d:
            uit.append((ax + (bx - ax) * t / d, az + (bz - az) * t / d))
            t += stap
        rest = d - (t - stap)
    return uit


def _glad(ys, r=2):
    ys = np.array(ys, float)
    n = len(ys)
    med = np.array([np.median(ys[max(0, i - r):i + r + 1]) for i in range(n)])
    return [float(np.mean(med[max(0, i - 1):i + 2])) for i in range(n)]


def _zone_op(park, x, z):
    zi = int(park.zone[int(x), int(z)])
    return ZONES[zi] if zi >= 0 else None


def _rotatie_naar(dx, dz):
    """Sign rotation (0..15, 0 = text faces south) so that its front faces direction (dx, dz)."""
    hoek = math.degrees(math.atan2(-dx, dz))   # 0 = south
    return int(round(((hoek % 360) / 22.5))) % 16


def _vrije_grond(park, x, z, pd, min_pad=2.0):
    x, z = int(x), int(z)
    if not (2 <= x < W - 2 and 2 <= z < W - 2) or not park.binnen[x, z] or park.water[x, z] or park.hek[x, z]:
        return False
    if pd[x, z] < min_pad or park.hek_afstand[x, z] < 3:
        return False
    g = int(park.hoogte[x, z])
    return all(park.v[x, y, z] == 0 for y in range(g + 1, g + 4))


# ======================================================================================================================
# the truck route + haltes + lanterns
def truckroute(park, log):
    lijn = _herbemonster(park.route_lijn, 1.5)
    ys = _glad([staan(park, x, z) for x, z in lijn])
    punten = [[round(x + 0.0, 2), round(y, 2), round(z + 0.0, 2)] for (x, z), y in zip(lijn, ys)]
    pd = pad_afstand(park)
    zcent = {}
    for zi, k in enumerate(ZONES):
        xs, zs = np.nonzero(park.zone == zi)
        if len(xs):
            zcent[k] = (xs.mean(), zs.mean())
    haltes = []
    gebruikt = set()
    for k in ROUTE[:-1]:
        doel = getattr(park, "plein", None) if k == "entree" else zcent.get(k)
        if doel is None:
            continue
        kand = [i for i, (x, z) in enumerate(lijn) if _zone_op(park, x, z) == k and i not in gebruikt]
        if not kand:
            continue
        i = min(kand, key=lambda j: (lijn[j][0] - doel[0]) ** 2 + (lijn[j][1] - doel[1]) ** 2)
        if k == "entree":
            i = min(kand, key=lambda j: j if j < len(lijn) // 2 else len(lijn))   # the start of the loop
        gebruikt.update(range(i - 30, i + 30))
        haltes.append({"zone": k, "naam": ZONE_NAAM[k], "i": i})
        _halte_bouw(park, lijn, i, k, pd)
    lantaarns = _lantaarns(park, lijn, pd)
    log(f"    s3: truckroute {len(punten)} punten ({len(punten) * 1.5:.0f} blokken), {len(haltes)} haltes, "
        f"{lantaarns} lantaarnpalen")
    return {"punten": punten, "stap": 1.5, "haltes": haltes}


def _richting(lijn, i):
    a, b = lijn[max(i - 3, 0)], lijn[min(i + 3, len(lijn) - 1)]
    d = math.hypot(b[0] - a[0], b[1] - a[1]) or 1
    return (b[0] - a[0]) / d, (b[1] - a[1]) / d


def _halte_bouw(park, lijn, i, zone, pd):
    """A halte beside the route: a sign facing the road, a bench and a lantern post."""
    x, z = lijn[i]
    rx, rz = _richting(lijn, i)
    for kant in (1, -1):
        nx, nz = -rz * kant, rx * kant           # perpendicular
        for afst in (3, 4, 5):
            sx, sz = int(round(x + nx * afst)), int(round(z + nz * afst))
            if not _vrije_grond(park, sx, sz, pd):
                continue
            # bench next to the sign, along the road
            bx, bz = int(round(sx + rx * 2)), int(round(sz + rz * 2))
            lx, lz = int(round(sx - rx * 2)), int(round(sz - rz * 2))
            if not (_vrije_grond(park, bx, bz, pd) and _vrije_grond(park, lx, lz, pd)):
                continue
            g = int(park.hoogte[sx, sz])
            bord(park, sx, g + 1, sz, ["", "Halte", ZONE_NAAM[zone], "~ safaritruck ~"],
                 rotation=_rotatie_naar(-nx, -nz))
            gb = int(park.hoogte[bx, bz])
            facing = _facing(-nx, -nz)
            park.set(bx, gb + 1, bz, "minecraft:spruce_stairs", facing=_tegen(facing), half="bottom", shape="straight",
                     waterlogged="false")
            gl = int(park.hoogte[lx, lz])
            park.set(lx, gl + 1, lz, "minecraft:dark_oak_fence")
            park.set(lx, gl + 2, lz, "minecraft:dark_oak_fence")
            park.set(lx, gl + 3, lz, "minecraft:lantern", hanging="false", waterlogged="false")
            return
    # nowhere free: the halte is only on the map


def _facing(dx, dz):
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def _tegen(f):
    return {"east": "west", "west": "east", "north": "south", "south": "north"}[f]


def _lantaarns(park, lijn, pd, elke=26):
    n = 0
    kant = 1
    for i in range(10, len(lijn) - 10, int(elke / 1.5)):
        x, z = lijn[i]
        rx, rz = _richting(lijn, i)
        kant = -kant
        for afst in (3, 4):
            sx, sz = int(round(x - rz * kant * afst)), int(round(z + rx * kant * afst))
            if not _vrije_grond(park, sx, sz, pd):
                continue
            if any(math.hypot(sx - a, sz - b) < r + 2 for a, b, r, _ in kit.RESERVERINGEN.values()):
                continue
            g = int(park.hoogte[sx, sz])
            park.set(sx, g + 1, sz, "minecraft:dark_oak_fence")
            park.set(sx, g + 2, sz, "minecraft:dark_oak_fence")
            park.set(sx, g + 3, sz, "minecraft:lantern", hanging="false", waterlogged="false")
            n += 1
            break
    return n


# ======================================================================================================================
# the canoe
_NAT = ("minecraft:water", "minecraft:seagrass", "minecraft:tall_seagrass", "minecraft:kelp")
_LUCHTIG = ("minecraft:lily_pad", "minecraft:sugar_cane")      # a canoe glides past a lily pad


def _water_ok(park, x, z):
    x, z = int(x), int(z)
    if not (1 <= x < W - 1 and 1 <= z < W - 1):
        return False
    if not park.pal[park.v[x, WATER_Y, z]].startswith(_NAT):
        return False
    return all(park.v[x, y, z] == 0 or park.pal[park.v[x, y, z]].startswith(_LUCHTIG) for y in (WATER_Y + 1, WATER_Y + 2))


def _snap(park, x, z, r=4):
    best = None
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if _water_ok(park, x + dx, z + dz):
                # prefer cells with water around (the middle of the stream)
                ruim = sum(_water_ok(park, x + dx + a, z + dz + b) for a, b in ((2, 0), (-2, 0), (0, 2), (0, -2)))
                k = (dx * dx + dz * dz) - ruim * 1.5
                if best is None or k < best[0]:
                    best = (k, x + dx + 0.5, z + dz + 0.5)
    return None if best is None else (best[1], best[2])


MAX_KANO = 420     # blocks upstream before the guided trip turns (a trip of a few minutes)


def _vrij_stuk(park, a, b):
    """Every cell between two path points is open water with headroom (no crossing a dam or a bank)."""
    n = int(math.hypot(b[0] - a[0], b[1] - a[1]) * 4) + 1
    return all(_water_ok(park, a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n) for i in range(n + 1))


def kano(park, log):
    meer = P(*MEREN[0][0])                         # the village pond (the river ends in it)
    plein = getattr(park, "plein", None) or (meer[0], 0, meer[1] + 20)
    # start: the pond water closest to the square
    best = None
    for dx in range(-30, 31):
        for dz in range(-30, 31):
            x, z = meer[0] + dx, meer[1] + dz
            if _water_ok(park, x, z) and _water_ok(park, x + 1, z) and _water_ok(park, x, z + 1):
                d = math.hypot(x - plein[0], z - plein[2])
                if best is None or d < best[0]:
                    best = (d, x, z)
    if best is None:
        log("    s3: kano: geen vijverwater gevonden")
        return None
    sx, sz = best[1] + 0.5, best[2] + 0.5
    rivier = [P(x, z) for x, z in reversed(RIVIER)]
    rivier = [meer] + rivier
    ruw = _herbemonster(rivier, 1.0)
    # from the start to the river: straight to the pond centre first
    pad = [(sx, sz)]
    for x, z in _herbemonster([(sx, sz), meer], 1.0)[1:]:
        s = _snap(park, x, z)
        if s:
            pad.append(s)
    geblokt = None
    lengte = 0.0
    for x, z in ruw:
        s = _snap(park, x, z)
        if s is None or math.hypot(s[0] - pad[-1][0], s[1] - pad[-1][1]) > 3.5 or not _vrij_stuk(park, pad[-1], s):
            geblokt = (int(x), int(z))
            break
        lengte += math.hypot(s[0] - pad[-1][0], s[1] - pad[-1][1])
        pad.append(s)
        if lengte > MAX_KANO:
            geblokt = "max"
            break
    # turn a little before the blocked spot, then back the same way
    pad = pad[:max(10, len(pad) - 12)]
    pad = _herbemonster(pad, 1.5)
    heen = [[round(x, 2), WATER_Y + 0.6, round(z, 2)] for x, z in pad]
    terug = list(reversed(heen))[1:]
    _steiger(park, sx, sz, plein)
    log(f"    s3: kano {len(heen)} punten heen ({len(heen) * 1.5:.0f} blokken), keert bij {geblokt}")
    return {"punten": heen + terug, "keer": len(heen) - 1, "start": heen[0], "stap": 1.5}


def _steiger(park, sx, sz, plein):
    """A small plank jetty from the shore to the canoe's mooring."""
    dx, dz = plein[0] - sx, plein[2] - sz
    d = math.hypot(dx, dz) or 1
    dx, dz = dx / d, dz / d
    x, z = sx, sz
    for _ in range(12):
        xi, zi = int(x), int(z)
        if not park.water[xi, zi]:
            break
        for a in (-1, 0, 1):
            cx, cz = int(round(x - dz * a)), int(round(z + dx * a))
            if park.water[cx, cz] and park.v[cx, WATER_Y + 1, cz] == 0:
                park.set(cx, WATER_Y + 1, cz, "minecraft:spruce_slab", type="bottom", waterlogged="false")
        x += dx
        z += dz


# ======================================================================================================================
# the kabelbaan
def kabelbaan(park, log):
    a = zoek(park, "ijskust", P(205, 95), 40, 3, minhek=6, pad_min=4, helling=2)
    b = None
    for helling, weg_r in ((3, 14), (5, 10), (7, 8)):
        b = zoek(park, "bamboe", P(420, 90), 60, 3, minhek=5, pad_min=3, helling=helling,
                 weg_van=[(x, z) for x, z, r, k in kit.RESERVERINGEN.values() if k == "bamboe"], weg_r=weg_r)
        if b:
            break
    if not a or not b:
        log(f"    s3: kabelbaan: geen plek ({a}, {b})")
        return None
    (ax, az, ay), (bx, bz, by) = a, b
    lengte = math.hypot(bx - ax, bz - az)
    top = 0
    for t in np.linspace(0, 1, int(lengte) + 1):
        x, z = int(round(ax + (bx - ax) * t)), int(round(az + (bz - az) * t))
        for dx in (-2, -1, 0, 1, 2):
            for dz in (-2, -1, 0, 1, 2):
                kol = np.nonzero(park.v[x + dx, :, z + dz])[0]
                if len(kol):
                    top = max(top, int(kol.max()))
    kabel = min(H - 6, top + 7)
    dek = kabel - 5
    for (x, z, g) in (a, b):
        _station(park, x, z, g, dek, kabel, (bx - ax, bz - az) if (x, z) == (ax, az) else (ax - bx, az - bz),
                 "IJskust" if (x, z) == (ax, az) else "Bamboebergen")
    # pylons every ~40 blocks, the cable between
    n_pyl = max(1, int(lengte // 40))
    pylonen = []
    for k in range(1, n_pyl + 1):
        t = k / (n_pyl + 1)
        px, pz = int(round(ax + (bx - ax) * t)), int(round(az + (bz - az) * t))
        _pyloon(park, px, pz, kabel, (bx - ax, bz - az))
        pylonen.append([px, pz])
    for t in np.linspace(0, 1, int(lengte * 2) + 1):
        x, z = int(round(ax + (bx - ax) * t)), int(round(az + (bz - az) * t))
        if park.v[x, kabel + 1, z] == 0:
            park.set(x, kabel + 1, z, "minecraft:iron_bars")
    log(f"    s3: kabelbaan ({ax},{az}) -> ({bx},{bz}), {lengte:.0f} blokken, kabel y{kabel}, {len(pylonen)} masten")
    gondel_y = dek + 1.0
    return {"a": [ax + 0.5, gondel_y, az + 0.5], "b": [bx + 0.5, gondel_y, bz + 0.5], "kabel": kabel, "dek": dek,
            "pylonen": pylonen}


def _station(park, x, z, g, dek, kabel, naar, naam):
    """A station: a 7x7 deck at `dek` on four log legs, a ladder up, railings (open toward the cable), a roof."""
    ux, uz = naar
    d = math.hypot(ux, uz) or 1
    ux, uz = ux / d, uz / d
    open_kant = _facing(ux, uz)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            vlak(park, x + dx, z + dz, g, top=park.get(x + dx, g, z + dz) if park.v[x + dx, g, z + dz] else "dirt")
    for dx in (-3, 3):
        for dz in (-3, 3):
            for y in range(g + 1, kabel + 2):
                park.set(x + dx, y, z + dz, "minecraft:spruce_log", axis="y")
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if abs(dx) == 3 and abs(dz) == 3:
                continue
            park.set(x + dx, dek, z + dz, "minecraft:spruce_planks")
            park.set(x + dx, kabel + 2, z + dz, "minecraft:spruce_slab", type="bottom", waterlogged="false")
    # railings, open on the cable side (2 wide gap in the middle)
    for i in range(-2, 3):
        for kant, (fx, fz) in (("north", (i, -3)), ("south", (i, 3)), ("west", (-3, i)), ("east", (3, i))):
            if kant == open_kant and abs(i) <= 1:
                continue
            park.set(x + fx, dek + 1, z + fz, "minecraft:spruce_fence")
    # the ladder up the leg on the side away from the cable
    lx, lz = int(round(x - ux * 3)), int(round(z - uz * 3))
    lx, lz = max(min(lx, x + 2), x - 2), max(min(lz, z + 2), z - 2)
    achter = _tegen(open_kant)
    off = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}[achter]
    ox, oz = x + off[0] * 3, z + off[1] * 3
    sx, sz = x + off[0] * 4, z + off[1] * 4
    for y in range(g + 1, dek + 1):
        park.set(ox, y, oz, "minecraft:spruce_planks")
    for y in range(g + 1, dek + 2):
        park.set(sx, y, sz, "minecraft:ladder", facing=achter, waterlogged="false")
    park.set(ox, dek + 1, oz, "minecraft:air")
    onderbouw(park, sx, sz, g + 1)
    bord(park, x, dek + 1, z, ["Kabelbaan", "naar de", "Bamboebergen" if naam == "IJskust" else "IJskust", ""],
         rotation=_rotatie_naar(ux, uz))
    park.set(x - off[0], dek + 1, z - off[1], "minecraft:lantern", hanging="false", waterlogged="false")
    # a sign at the foot of the ladder
    vx, vz = x + off[0] * 5, z + off[1] * 5
    if park.v[vx, int(park.hoogte[vx, vz]) + 1, vz] == 0 and not park.water[vx, vz]:
        bord(park, vx, int(park.hoogte[vx, vz]) + 1, vz, ["", "Kabelbaan", naam, "klim omhoog!"],
             rotation=_rotatie_naar(off[0], off[1]))


def _pyloon(park, x, z, kabel, naar):
    g = int(park.hoogte[x, z])
    for dx in (0, 1):
        for dz in (0, 1):
            onderbouw(park, x + dx, z + dz, g + 1)
            for y in range(g + 1, kabel + 1):
                park.set(x + dx, y, z + dz, "minecraft:stripped_spruce_log", axis="y")
    ux, uz = naar
    langs_x = abs(ux) >= abs(uz)
    for i in range(-2, 4):
        if langs_x:
            park.set(x, kabel, z + i, "minecraft:spruce_planks")
        else:
            park.set(x + i, kabel, z, "minecraft:spruce_planks")


# ======================================================================================================================
# the struisvogelrace
def racebaan(park, log):
    plek = zoek(park, "savanne", P(380, 470), 70, 16, minhek=7, pad_min=5, helling=3,
                weg_van=[(x, z) for x, z, r, k in kit.RESERVERINGEN.values() if k == "savanne"], weg_r=26)
    if not plek:
        plek = zoek(park, "savanne", P(380, 470), 90, 13, minhek=6, pad_min=4, helling=4)
    if not plek:
        log("    s3: racebaan: geen plek")
        return None
    cx, cz, g = plek
    a, b = 13.0, 7.0
    # flat ground, track band (width 3) of coarse dirt
    for x in range(int(cx - a - 3), int(cx + a + 4)):
        for z in range(int(cz - b - 3), int(cz + b + 4)):
            e = math.hypot((x - cx) / (a + 2.5), (z - cz) / (b + 2.5))
            if e <= 1.0:
                f = math.hypot((x - cx) / a, (z - cz) / b)
                top = "minecraft:coarse_dirt" if 0.78 <= f <= 1.18 else "grass_block"
                vlak(park, x, z, g, top=top)
    # hay bales on the outside and the inside edge, every few blocks
    for k in range(48):
        t = k / 48 * 2 * math.pi
        for schaal, elk in ((1.30, 2), (0.66, 3)):
            if k % elk:
                continue
            x, z = int(round(cx + math.cos(t) * a * schaal)), int(round(cz + math.sin(t) * b * schaal))
            if park.v[x, g + 1, z] == 0:
                park.set(x, g + 1, z, "minecraft:hay_block", axis="y")
    # the centre line, starting at the start/finish (the east end), anticlockwise
    lijn = []
    for k in range(72):
        t = k / 72 * 2 * math.pi
        x, z = cx + math.cos(t) * a, cz - math.sin(t) * b
        lijn.append([round(x, 2), g + 1.0, round(z, 2)])
    sx, sz = int(round(cx + a)), int(round(cz))
    # start arch across the track (along z), checkered carpet line
    for dz in range(-2, 3):
        park.set(sx, g + 1, sz + dz, "minecraft:white_carpet" if (dz % 2 == 0) else "minecraft:black_carpet")
    for dz in (-3, 3):
        for y in range(g + 1, g + 6):
            park.set(sx, y, sz + dz, "minecraft:stripped_acacia_log", axis="y")
    for dz in range(-3, 4):
        park.set(sx, g + 6, sz + dz, "minecraft:acacia_planks")
    bord_x, bord_z = sx + 3, sz - 5
    if park.v[bord_x, g + 1, bord_z] == 0:
        bord(park, bord_x, g + 1, bord_z, ["Struisvogelrace", "2 rondjes", "klik op een", "struisvogel!"],
             rotation=_rotatie_naar(-1, 1))
    log(f"    s3: racebaan bij ({cx},{cz}) y{g}")
    return {"lijn": lijn, "start": [sx + 0.5, g + 1.0, sz + 0.5], "wacht": [sx + 2.5, g + 1.0, sz + 4.5],
            "bord": [bord_x + 0.5, g + 3.6, bord_z + 0.5], "rondes": 2}


# ======================================================================================================================
# schemabord, atmosphere and the moment spots
def schemabord(park, log):
    plein = getattr(park, "plein", None)
    if not plein:
        return None
    px, py, pz = plein
    pd = pad_afstand(park)
    for r in range(5, 16):
        for k in range(16):
            t = k / 16 * 2 * math.pi
            x, z = int(round(px + math.cos(t) * r)), int(round(pz + math.sin(t) * r))
            if park.water[x, z] or park.hek[x, z]:
                continue
            g = int(park.hoogte[x, z])
            if abs(g - py) > 1 or park.v[x, g + 1, z] or park.v[x, g + 2, z]:
                continue
            if not _vloer(park.get(x, g, z)):
                continue
            f = _facing(px - x, pz - z)
            park.set(x, g + 1, z, "guhs:safari_schemabord", facing=f)
            log(f"    s3: schemabord bij ({x},{g + 1},{z}) kijkt {f}")
            return [x, g + 1, z]
    return None


def sfeer(park, plekken_per_zone):
    uit = {}
    oert = getattr(park, "_oertijd", {}) or {}
    bronnen = []
    for b in oert.get("bronnen", []) or []:
        try:
            x, z = int(b[0]), int(b[1])
            bronnen.append([x + 0.5, WATER_Y + 1.0, z + 0.5] if len(b) < 3 else [x + 0.5, float(b[2]) + 1.0, z + 0.5])
        except (TypeError, ValueError, IndexError):
            pass
    if not bronnen:
        x, z, _, _ = kit.RESERVERINGEN["warme_bronnen"]
        bronnen = [[x + 0.5, float(park.hoogte[x, z]) + 1.0, z + 0.5]]
    uit["bronnen"] = bronnen
    plein = getattr(park, "plein", None)
    if plein:
        uit["fontein"] = [plein[0] + 0.5, plein[1] + 5.5, plein[2] + 0.5]
    uit["zones"] = plekken_per_zone
    return uit


def bouw(park, log=print):
    if getattr(park, "hek_afstand", None) is None:
        park.hek_afstand = distance_transform_edt(~(park.hek & ~park.water))
    s3 = {}
    s3["route"] = truckroute(park, log)
    s3["kano"] = kano(park, log)
    s3["kabelbaan"] = kabelbaan(park, log)
    s3["race"] = racebaan(park, log)
    s3["schemabord"] = schemabord(park, log)
    park._s3 = s3


def plekken(park, plekken):
    """No homes change here; this only remembers, per zone, where the feeding spots are (the first home of the zone's
    feeding species) for sfeer.json / the moments."""
    per_zone = {}
    for p in plekken:
        per_zone.setdefault(p["zone"], []).append({"soort": p["soort"], "centrum": p["centrum"]})
    park._s3["sfeer"] = sfeer(park, {k: v[:24] for k, v in per_zone.items()})
