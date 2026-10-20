"""
Zone module: Europees Woud & Beekdal (design 1.4, zone table).

Landmarks (world coordinates, see kit.RESERVERINGEN for the reserved spots of plattegrond C):
  beverdam      a side branch of the river into the hill: a dam of logs, sticks and mud with the pond behind it two
                blocks higher (y 64 instead of 62), and a beaver lodge (burcht) in the pond
  beek          a brook from the river to the east, under the footpath (stone bridge), past the otterglijbaan, ending in
                a pond with the watermolentje
  holle_eik     a hollow oak you can walk through, with a hedgehog nest at its foot and the old ranger chest (geheimpje)
  dassenheuvel  a mound with badger tunnels and glass kijkvenstertjes into them
  paddenstoelenkring, herfsthoekje (leaf piles), hertenweide (meadow + hay feeder), ooievaarsnest on a pole,
  uitkijktoren, schuilhut

The small helpers at the top (kap, vlak, grond_y, toren, schuilhut, ...) are shared with zone_australie.py.
"""
import math

import numpy as np

from . import kit
from .terrein import ZONES

W, WATER_Y = kit.W, kit.WATER_Y
LEAVES_ETC = ("_leaves", "_log", "_wood", "vine", "bamboo", "short_grass", "tall_grass", "fern", "dead_bush", "snow",
              "mushroom", "dandelion", "poppy", "cornflower", "daisy", "allium", "bluet", "tulip", "sugar_cane")


# ======================================================================================================================
# shared helpers
def _naam(park, x, y, z):
    return park.get(x, y, z).split("[")[0]


def kap(park, cx, cz, r):
    """Remove every tree whose trunk stands within r+3 of (cx, cz), whole (crown too), so nothing floats; then clear
    plants and loose stuff above the ground in the circle."""
    blijf = []
    for (bx, top, bz, soort) in park.bomen:
        if math.hypot(bx - cx, bz - cz) > r + 3:
            blijf.append((bx, top, bz, soort))
            continue
        g = int(park.hoogte[bx, bz])
        rr = 8
        for x in range(bx - rr, bx + rr + 2):
            for z in range(bz - rr, bz + rr + 2):
                if not (0 <= x < W and 0 <= z < W):
                    continue
                for y in range(g + 1, min(top + 3, kit.H)):
                    n = _naam(park, x, y, z)
                    if n.endswith(("_leaves", "_log", "_wood", "vine")) or n == "minecraft:mangrove_roots":
                        park.v[x, y, z] = 0
    park.bomen[:] = blijf
    for x in range(int(cx - r), int(cx + r) + 1):
        for z in range(int(cz - r), int(cz + r) + 1):
            if math.hypot(x - cx, z - cz) <= r and 0 <= x < W and 0 <= z < W:
                g = int(park.hoogte[x, z])
                for y in range(g + 1, min(g + 30, kit.H)):
                    if park.v[x, y, z] and _naam(park, x, y, z).endswith(LEAVES_ETC):
                        park.v[x, y, z] = 0


def zet_grond(park, x, z, y, top="grass_block", sub="dirt"):
    """Make column (x, z) solid up to y with `top` on top, air above (up to y+20 plants/leaves only removed)."""
    g = int(park.hoogte[x, z])
    if y < g:
        for yy in range(y + 1, g + 1):
            park.v[x, yy, z] = 0
    lo = min(g, y) - 3
    for yy in range(max(lo, 1), y):
        if park.v[x, yy, z] == 0 or yy > g - 3:
            park.set(x, yy, z, sub)
    park.set(x, y, z, top, **({"snowy": "false"} if top in ("grass_block", "podzol") else {}))
    for yy in range(y + 1, y + 6):
        if park.v[x, yy, z] and _naam(park, x, yy, z).endswith(LEAVES_ETC):
            park.v[x, yy, z] = 0
    park.hoogte[x, z] = y
    park.water[x, z] = False


def vlak(park, cx, cz, r, y, top="grass_block", sub="dirt"):
    """Flatten a disc to height y (cut or fill), ground block `top`."""
    for x in range(int(cx - r), int(cx + r) + 1):
        for z in range(int(cz - r), int(cz + r) + 1):
            if math.hypot(x - cx, z - cz) <= r and not park.hek[x, z] and not park.pad[x, z]:
                zet_grond(park, x, z, y, top, sub)


def grond_y(park, x, z):
    """First y above the ground at (x, z) where an animal can stand (plants, snow and water count as free)."""
    y = int(park.hoogte[x, z]) + 1
    while y < kit.H - 2 and park.v[x, y, z] != 0:
        n = _naam(park, x, y, z)
        if n.endswith(("short_grass", "tall_grass", "fern", "dead_bush", "snow", "water", "lily_pad", "carpet",
                       "seagrass", "mushroom", "cornflower", "blue_orchid", "dandelion", "poppy")):
            break
        y += 1
    return y


def ver_van_hek(park, cx, cz, r, minimaal=4):
    """Escape rule (design 4.5c): nothing raised within 2 blocks of a fence; we keep a margin."""
    xs, zs = np.mgrid[int(cx - r):int(cx + r) + 1, int(cz - r):int(cz + r) + 1]
    return float(park.hek_afstand[xs, zs].min()) >= minimaal


def toren(park, x, z, hout="spruce", dak="spruce", hoogte=8, dakblok=None):
    """Lookout tower 5x5 on 4 log legs, ladder on the south side, platform with railing, pyramid roof.
    x, z = centre. Returns the platform y."""
    g = int(max(park.hoogte[x + dx, z + dz] for dx in (-2, 2) for dz in (-2, 2)))
    log = kit.state(f"{hout}_log", axis="y")
    for dx in (-2, 2):
        for dz in (-2, 2):
            for y in range(int(park.hoogte[x + dx, z + dz]) + 1, g + hoogte + 4):
                park.set(x + dx, y, z + dz, log)
    p = g + hoogte
    park.fill(x - 2, p, z - 2, x + 2, p, z + 2, f"{hout}_planks")
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 or abs(dz) == 2:
                if not (abs(dx) == 2 and abs(dz) == 2):
                    park.set(x + dx, p + 1, z + dz, f"{hout}_fence")
    park.set(x, p + 1, z + 2, "air")                                # gap for the ladder
    for y in range(int(park.hoogte[x, z + 3]) + 1, p + 1):
        park.set(x, y, z + 3, "ladder", facing="south", waterlogged="false")
    park.set(x, p, z + 2, "air")
    # roof
    blok = dakblok or f"{dak}_planks"
    for i, rr in enumerate((3, 2, 1, 0)):
        yy = p + 4 + i
        for dx in range(-rr, rr + 1):
            for dz in range(-rr, rr + 1):
                if max(abs(dx), abs(dz)) == rr:
                    park.set(x + dx, yy, z + dz, blok)
    park.set(x, p + 1, z, "lantern", hanging="false", waterlogged="false")
    # a spyglass stand: a lectern-like post (the verrekijker block comes with the lobby slice); mark with a lightning rod
    park.set(x + 1, p + 1, z - 1, "lightning_rod", facing="up", powered="false", waterlogged="false")
    return p


def schuilhut(park, x, z, kijk="north", hout="spruce", dak="hay_block"):
    """A small hide 5 x 4 with a slit window (glass panes) facing `kijk`, door on the other side."""
    g = int(park.hoogte[x, z])
    vlak(park, x, z, 3.6, g)
    log = kit.state(f"{hout}_log", axis="y")
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            rand = abs(dx) == 2 or abs(dz) == 2
            for y in range(g + 1, g + 4):
                if rand:
                    park.set(x + dx, y, z + dz, log if (abs(dx) == 2 and abs(dz) == 2) else f"{hout}_planks")
                else:
                    park.set(x + dx, y, z + dz, "air")
            park.set(x + dx, g + 4, z + dz, dak)
    # slit window: the middle row at eye height on the kijk side
    off = {"north": (0, -2), "south": (0, 2), "east": (2, 0), "west": (-2, 0)}
    ox, oz = off[kijk]
    deur = {"north": "south", "south": "north", "east": "west", "west": "east"}[kijk]
    dx_, dz_ = off[deur]
    for t in (-1, 0, 1):
        wx, wz = (x + t, z + oz) if oz else (x + ox, z + t)
        park.set(wx, g + 2, wz, "glass")
    park.set(x + dx_, g + 1, z + dz_, "air")
    park.set(x + dx_, g + 2, z + dz_, "air")
    kit.stoel(park, x, g + 1, z, kijk)                                                # two guh chairs at the slit
    kit.stoel(park, x + (1 if not ox else 0), g + 1, z + (1 if ox else 0), kijk)


KIJKGLAS = "guhs:eenrichtingsglas"
_RICHTING = {"north": ((0, -1), (1, 0)), "south": ((0, 1), (-1, 0)), "east": ((1, 0), (0, 1)), "west": ((-1, 0), (0, -1))}
_TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}


def kijkhut(park, x, z, kijk="north", hout="spruce", dak="spruce", breed=7, diep=5, verhoogd=2, zicht=18,
            naam="kijkhut", vrij_zicht=True):
    """A real viewing hide (round 2): x, z = middle of the FRONT wall, the hide stands behind it (away from `kijk`).
    Raised floor (verhoogd blocks above the ground, steps up to a door in the back wall), a wide band of one-way glass
    (guhs:eenrichtingsglas, 3 high from the floor) over the whole front, 2 high in the first block of both side walls, a
    bench facing the
    window, a lantern. With vrij_zicht the view cone in front is cleared: trees removed, ground and plants lowered to
    below the window so you look AT the animals, not at mud or leaves. Remembers the eye point on park.kijkpunten.
    Returns the floor y."""
    (fx, fz), (rx, rz) = _RICHTING[kijk]
    h = breed // 2
    def w2(u, w):
        return x + u * rx + w * fx, z + u * rz + w * fz
    cellen = [w2(u, w) for u in range(-h - 1, h + 2) for w in range(-diep - verhoogd - 1, 2)]
    g = int(round(np.median([park.hoogte[cx, cz] for cx, cz in cellen])))
    for cx, cz in cellen:
        if not park.hek[cx, cz] and not park.pad[cx, cz] and not park.water[cx, cz]:
            zet_grond(park, cx, cz, g)
    F = g + verhoogd
    log = kit.state(f"{hout}_log", axis="y")
    for u in range(-h, h + 1):
        for w in range(-diep + 1, 1):
            cx, cz = w2(u, w)
            rand_u, rand_w = abs(u) == h, w in (0, -diep + 1)
            hoek = rand_u and rand_w
            for y in range(g + 1, F):                                   # stilts under the corners, lattice between
                park.set(cx, y, cz, log if hoek else (f"{hout}_fence" if (rand_u or rand_w) else "air"))
            park.set(cx, F, cz, f"{hout}_planks")
            for y in range(F + 1, F + 5):
                if hoek:
                    park.set(cx, y, cz, log)
                elif rand_u or rand_w:
                    raam = (w == 0 and y in (F + 1, F + 2, F + 3)) or (rand_u and w == -1 and y in (F + 2, F + 3))
                    park.set(cx, y, cz, KIJKGLAS if raam else f"{hout}_planks")
                else:
                    park.set(cx, y, cz, "air")
    # roof: an overhanging flat roof with a slab rim, a raised middle ridge
    for u in range(-h - 1, h + 2):
        for w in range(-diep, 2):
            cx, cz = w2(u, w)
            rim = abs(u) == h + 1 or w in (-diep, 1)
            park.set(cx, F + 5, cz, f"{dak}_slab" if rim else f"{dak}_planks",
                     **({"type": "bottom", "waterlogged": "false"} if rim else {}))
        cx, cz = w2(u, -diep // 2)
        if abs(u) <= h:
            park.set(cx, F + 6, cz, f"{dak}_slab", type="bottom", waterlogged="false")
    # bench right behind the window (backrest away from the glass), gap in the middle for walking
    for u in range(-h + 1, h):
        if u != 0:
            cx, cz = w2(u, -1)
            kit.stoel(park, cx, F + 1, cz, kijk)                   # guh chairs looking through the glass
    cx, cz = w2(0, -2)
    park.set(cx, F + 4, cz, "lantern", hanging="true", waterlogged="false")
    # door in the back wall + steps down to the ground
    dx_, dz_ = w2(0, -diep + 1)
    park.set(dx_, F + 1, dz_, f"{hout}_door", facing=kijk, half="lower", hinge="left", open="false", powered="false")
    park.set(dx_, F + 2, dz_, f"{hout}_door", facing=kijk, half="upper", hinge="left", open="false", powered="false")
    for k in range(1, verhoogd + 1):
        sx, sz = w2(0, -diep + 1 - k)
        y = F - k + 1
        for yy in range(g + 1, y):
            park.set(sx, yy, sz, f"{hout}_planks")
        park.set(sx, y, sz, f"{hout}_stairs", facing=kijk, half="bottom", shape="straight", waterlogged="false")
        for yy in range(y + 1, y + 4):
            park.set(sx, yy, sz, "air")
    if vrij_zicht:
        stammen = {(bx, bz) for (bx, _, bz, _) in park.bomen}
        for w in range(1, zicht + 1):
            breedte = h + 1 + w // 2
            for u in range(-breedte, breedte + 1):
                cx, cz = w2(u, w)
                if not (0 <= cx < W and 0 <= cz < W) or park.hek[cx, cz] or park.pad[cx, cz]:
                    continue
                # trees: remove logs/leaves above the window sill in the cone (no floating crowns: kap whole trees)
                if (cx, cz) in stammen:
                    kap(park, cx, cz, 0)
                    stammen = {(bx, bz) for (bx, _, bz, _) in park.bomen}
                for y in range(F + 1, min(F + 14, kit.H)):
                    if park.v[cx, y, cz] and _naam(park, cx, y, cz).endswith(LEAVES_ETC + ("_wood", "mangrove_roots")):
                        park.v[cx, y, cz] = 0
                if not park.water[cx, cz] and park.hoogte[cx, cz] > F and park.hek_afstand[cx, cz] >= 3:
                    zet_grond(park, cx, cz, F)
    ox, oz = w2(0, -1)                                  # a visitor standing right behind the glass
    if not hasattr(park, "kijkpunten"):
        park.kijkpunten = []
    park.kijkpunten.append({"naam": naam, "oog": (ox + 0.5, F + 2.6, oz + 0.5), "kijk": kijk})
    return F


def dicht_water(park, zones, log=print):
    """Round 2: no water may flow when the park is stamped. Every water block in the given zones that has air beside
    it (same y) or below it gets that air filled with dirt (grass on top of a column). The terrain columns are only
    filled a few blocks deep, so a carved pond could otherwise run out under its banks. Repeats until nothing leaks."""
    zm = np.isin(park.zone, [ZONES.index(z) for z in zones])
    xs, zs = np.nonzero(zm)
    totaal = 0
    for _ in range(6):
        wid = np.array([s.split("[")[0] == "minecraft:water" for s in park.pal] + [False] * 64)
        gevuld = 0
        for x, z in zip(xs.tolist(), zs.tolist()):
            col = park.v[x, :, z]
            for y in np.nonzero(wid[col])[0].tolist():
                for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
                    nx, ny, nz = x + dx, y + dy, z + dz
                    if park.v[nx, ny, nz] == 0:
                        top = dy == 0 and park.hoogte[nx, nz] < ny
                        park.set(nx, ny, nz, "grass_block" if top else "dirt", **({"snowy": "false"} if top else {}))
                        if top:
                            park.hoogte[nx, nz] = ny
                        gevuld += 1
        totaal += gevuld
        if not gevuld:
            break
    if totaal:
        log(f"    water dichtgezet: {totaal} blokken in {', '.join(zones)}")


def zone_cel_ok(park, x, z, zone):
    """True when the 16x16 cell of (x, z) has `zone` as majority (that is what the game reads from zones.json)."""
    cx, cz = x // 16, z // 16
    blok = park.zone[cx * 16:(cx + 1) * 16, cz * 16:(cz + 1) * 16]
    binnen = blok[blok >= 0]
    if binnen.size < 64:
        return False
    return int(np.bincount(binnen).argmax()) == ZONES.index(zone)


def zet_huizen(park, plekken, soort, punten, zone, y=None, straal=None):
    """Give the homes of `soort` the points given (cycled), standing height recomputed (or y), cell zone checked."""
    eigen = [p for p in plekken if p["soort"] == soort]
    for i, p in enumerate(eigen):
        x, z = punten[i % len(punten)]
        if not zone_cel_ok(park, x, z, zone):
            raise SystemExit(f"{soort}: plek {x},{z} ligt in de zones.json-cel van een andere zone dan {zone}")
        yy = y if y is not None else (grond_y(park, x, z) if not p.get("_water_y") else WATER_Y)
        p["centrum"] = [int(x), int(yy), int(z)]
        if straal is not None:
            p["straal"] = straal


def extra_huis(park, plekken, soort, punt, zone, y=None, volwassenen=None, jongen=None, straal=None):
    """Round 2: an EXTRA home for `soort` at `punt`, so a viewing spot always has animals in front of it. The animals
    are taken from the fullest existing home of the soort (the species totals of plekken.controleer stay the same);
    the donor keeps at least one animal. Default: 1 adult (+ the young asked)."""
    eigen = sorted((p for p in plekken if p["soort"] == soort), key=lambda p: p["volwassenen"] + p["jongen"])
    if not eigen:
        return
    donor = eigen[-1]
    x, z = punt
    if not zone_cel_ok(park, x, z, zone):
        raise SystemExit(f"{soort}: extra plek {x},{z} ligt in de zones.json-cel van een andere zone dan {zone}")
    va = volwassenen if volwassenen is not None else 1
    ja = jongen if jongen is not None else 0
    va = min(va, donor["volwassenen"])
    ja = min(ja, donor["jongen"])
    while va + ja >= donor["volwassenen"] + donor["jongen"] and va + ja > 0:  # the donor keeps one
        if ja:
            ja -= 1
        else:
            va -= 1
    if va + ja == 0:
        return
    nieuw = dict(donor)
    donor["volwassenen"] -= va
    donor["jongen"] -= ja
    nieuw["volwassenen"], nieuw["jongen"] = va, ja
    nieuw["centrum"] = [int(x), int(y if y is not None else grond_y(park, x, z)), int(z)]
    if straal is not None:
        nieuw["straal"] = straal
    plekken.append(nieuw)


def lijn(a, b):
    n = max(1, int(math.hypot(b[0] - a[0], b[1] - a[1]) * 2))
    return [(a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n) for i in range(n + 1)]


def dichtste_water(park, x, z, r=25):
    best = None
    for xx in range(x - r, x + r + 1):
        for zz in range(z - r, z + r + 1):
            if park.water[xx, zz] and park.binnen[xx, zz]:
                d = math.hypot(xx - x, zz - z)
                if best is None or d < best[0]:
                    best = (d, xx, zz)
    return best[1], best[2]


# ======================================================================================================================
# europa
R = kit.RESERVERINGEN
BEVER = (196, 323)            # pond centre (behind the dam)
DAM_X = 211                   # the dam runs north-south at this x
BEEK_EIND = (333, 318)        # pond of the watermill
OTTER = (300, 336)            # otterglijbaan on the brook's south bank
HOLLE_EIK = (284, 352)
DAS = (160, 350)
KRING = (204, 296)
HERFST = (172, 300)
WEIDE = (318, 352)
OOIEVAAR = (252, 322)
TOREN = (300, 300)
HUT = (196, 309)              # round 2: kijkhut, front wall here, looks south over the beaver pond and the lodge
VIJVER = (370, 321)           # round 2, the east side: the kikkervijver with a jetty and the kijkhut
RUINE = (394, 303)            # round 2: an overgrown little castle ruin on a low hill, wild flowers around it
OOST_HUT = (368, 333)         # front wall of the east kijkhut, looks north over the pond to the ruin


def beverdam(park):
    kap(park, 200, 323, 18)
    # channel from the river's west bank into the hill
    rx, rz = dichtste_water(park, 222, 323)
    beek = [(x, 323) for x in range(DAM_X + 1, rx + 1)]
    for x, z in beek:
        for dz in (-1, 0, 1):
            zet_grond(park, x, z + dz, WATER_Y - 3, "gravel", "dirt")
            for y in range(WATER_Y - 2, WATER_Y + 1):
                park.set(x, y, z + dz, "water", level=0)
            park.water[x, z + dz] = True
        for dz in (-2, 2):                                # banks at least 1 above the water
            if park.hoogte[x, z + dz] < WATER_Y + 1:
                zet_grond(park, x, z + dz, WATER_Y + 1)
    oevers(park, [(x, z + dz) for x, z in beek for dz in (-1, 0, 1)], breed=3)
    # the pond: two blocks higher than the river (the dam holds it), bigger in round 2, banks at least y 65
    py = WATER_Y + 2
    for x in range(BEVER[0] - 15, BEVER[0] + 16):
        for z in range(BEVER[1] - 14, BEVER[1] + 15):
            d = math.hypot((x - BEVER[0]) / 1.25, z - BEVER[1])
            if x >= DAM_X - 1 or park.hek_afstand[x, z] < 4:
                continue
            if d <= 9.5:
                zet_grond(park, x, z, py - 4 if d < 6 else py - 2, "mud" if d < 7 else "gravel", "dirt")
                for y in range(int(park.hoogte[x, z]) + 1, py + 1):
                    park.set(x, y, z, "water", level=0)
                park.water[x, z] = True
            elif d <= 12.5 and park.hoogte[x, z] < py + 1:
                zet_grond(park, x, z, py + 1)
    # the dam (round 2: bigger and more characteristic): three rows thick, a curved crest of crossed logs, branches
    # (fences) and packed mud, from the bed up to py+2, wider than the channel and tied into both banks
    for z in range(323 - 7, 323 + 8):
        kern = abs(z - 323) <= 5
        bed = WATER_Y - 3
        top = py + 2 if kern else py + 1
        for dxd in (-1, 0, 1):
            x = DAM_X + dxd
            for y in range(bed, top + 1):
                r = (z * 3 + y * 5 + dxd * 7) % 6
                if dxd == -1:
                    blok = "mud" if y <= py else ("packed_mud" if r < 3 else kit.state("oak_log", axis="z"))
                elif dxd == 0:
                    blok = (kit.state("spruce_log", axis="z") if y % 2 else kit.state("oak_log", axis="z")) \
                        if y < top else ("packed_mud" if r < 2 else kit.state("stripped_oak_log", axis="x"))
                else:
                    blok = kit.state("oak_log", axis="x") if r == 0 else ("packed_mud" if r < 3 else "mud")
                park.set(x, y, z, blok)
            park.water[x, z] = False
            park.hoogte[x, z] = top
        # branches sticking out of the crest, every other one leaning (fences + a log end)
        if z % 2:
            park.set(DAM_X, top + 1, z, "dark_oak_fence")
            if z % 4 == 1:
                park.set(DAM_X + 1, top + 1, z, "dark_oak_fence")
    # the cascade on the downstream face: still pools stepping down (each source water in a closed basin, so nothing
    # flows when the park is stamped), rims of slabs so the water seems to tumble over them
    # pool 1 = a notch in the crest (x DAM_X-1..DAM_X, level py, open to the pond), rim slab at DAM_X+1;
    # pool 2 one lower at DAM_X+2 (on a mud step in the channel), rim slab at DAM_X+3 over the channel water
    for z in (322, 323, 324):
        for x in (DAM_X - 1, DAM_X):
            park.set(x, py, z, "water", level=0)
            park.set(x, py - 1, z, "packed_mud")
            park.set(x, py + 1, z, "air")
            park.set(x, py + 2, z, "air")
        park.set(DAM_X + 1, py, z, "mud_brick_slab", type="bottom", waterlogged="false")
        park.set(DAM_X + 1, py - 1, z, "packed_mud")
        park.set(DAM_X + 1, py + 1, z, "air")
        park.set(DAM_X + 2, py - 1, z, "water", level=0)
        park.set(DAM_X + 2, py - 2, z, "packed_mud")
        park.set(DAM_X + 2, py, z, "air")
        park.set(DAM_X + 3, py - 1, z, "mud_brick_slab", type="bottom", waterlogged="false")
        park.water[DAM_X + 2, z] = False
    for z in (321, 325):                                                # side walls of the notch and the pools
        for x in (DAM_X - 1, DAM_X, DAM_X + 1, DAM_X + 2):
            for y in range(py - 2, py + 1):
                park.set(x, y, z, kit.state("spruce_log", axis="x"))
    # the lodge (burcht), round 2: a big dome of logs, sticks and mud in the pond, a dry room inside, the entrance under
    # water on the dam side, a smoke-hole of sticks on top
    bx, bz = BEVER[0] - 2, BEVER[1]
    R_, RY = 5.0, 4.2
    for dx in range(-6, 7):
        for dz in range(-6, 7):
            for dy in range(-2, 6):
                d = math.sqrt((dx / R_) ** 2 + (dz / R_) ** 2 + (dy / RY) ** 2)
                if d > 1.0:
                    continue
                y = py + dy
                if d <= 0.58 and dy >= 1:
                    park.set(bx + dx, y, bz + dz, "air")
                else:
                    r = (dx * 7 + dz * 3 + dy * 11) % 7
                    park.set(bx + dx, y, bz + dz,
                             kit.state("spruce_log", axis="x") if r == 0 else
                             kit.state("oak_log", axis="z") if r == 1 else
                             kit.state("stripped_oak_log", axis="y") if r == 2 else
                             "packed_mud" if r == 3 else "mud" if r == 4 else
                             "dark_oak_wood" if r == 5 else "coarse_dirt")
            park.water[bx + dx, bz + dz] = park.water[bx + dx, bz + dz] and math.hypot(dx, dz) > R_
    park.set(bx, py + 1, bz, "hay_block", axis="y")                    # the beaver bed inside
    park.set(bx + 1, py + 1, bz, "brown_carpet")
    for dy in (5, 6):
        park.set(bx, py + dy, bz, "dark_oak_fence")
    park.set(bx - 1, py + 5, bz, "dark_oak_fence")
    for y in (py - 1, py):                                              # underwater entrance toward the dam
        for t in range(3, 6):
            park.set(bx + t, y, bz, "water", level=0)
    # gnawed trees on the banks: a stump with the typical waist (thin fence), a felled trunk with chips, a few stumps
    knaag = [(181, 311), (188, 336), (205, 336), (206, 309), (180, 330), (193, 308), (199, 338)]
    for i, (sx, sz) in enumerate(knaag):
        if park.water[sx, sz] or park.hek_afstand[sx, sz] < 4 or park.pad[sx, sz]:
            continue
        g = int(park.hoogte[sx, sz])
        park.set(sx, g + 1, sz, "stripped_oak_log", axis="y")
        if i % 3 == 0:                                                   # half-gnawed: waist + the rest still standing
            park.set(sx, g + 2, sz, "oak_fence")
            park.set(sx, g + 3, sz, "oak_log", axis="y")
            park.set(sx, g + 4, sz, "oak_log", axis="y")
            park.set(sx, g + 5, sz, "oak_leaves", distance=1, persistent="true", waterlogged="false")
        elif i % 3 == 1:                                                 # felled: the trunk lies next to the stump
            for t in range(1, 5):
                park.set(sx + t, int(park.hoogte[sx + t, sz]) + 1, sz, "oak_log", axis="x")
            park.set(sx, g + 2, sz, "oak_button", face="floor", facing="north", powered="false")
        else:
            park.set(sx, g + 2, sz, "oak_button", face="floor", facing="north", powered="false")
        for ddx, ddz in ((1, 1), (-1, 0), (0, -1)):                      # wood chips
            xx, zz = sx + ddx, sz + ddz
            gg = int(park.hoogte[xx, zz])
            if park.v[xx, gg + 1, zz] == 0 and not park.water[xx, zz]:
                park.set(xx, gg + 1, zz, "birch_button", face="floor", facing="east", powered="false")


def beek(park):
    """Brook east from the river to the watermill pond; stone bridge where the footpath crosses."""
    start = dichtste_water(park, 243, 352)
    punten = []
    for a, b in ((start, (262, 346)), ((262, 346), (290, 338)), ((290, 338), (318, 326)), ((318, 326), BEEK_EIND)):
        punten += lijn(a, b)
    gezien = set()
    for fx, fz in punten:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                x, z = int(round(fx)) + dx, int(round(fz)) + dz
                if (x, z) in gezien or park.hek[x, z]:
                    continue
                gezien.add((x, z))
                if park.pad[x, z]:
                    # the stone bridge (round 2): deck at least 3 above the water, open air under it (no water above
                    # the water level), ramps on the path both sides (see below)
                    deck = max(int(park.hoogte[x, z]), WATER_Y + 3)
                    park.set(x, WATER_Y - 3, z, "gravel")
                    for y in range(WATER_Y - 2, WATER_Y + 1):
                        park.set(x, y, z, "water", level=0)
                    for y in range(WATER_Y + 1, deck):
                        park.set(x, y, z, "air")
                    park.set(x, deck, z, "stone_bricks")
                    park.hoogte[x, z] = deck
                    park.water[x, z] = True
                    continue
                kap_blok = park.hoogte[x, z]
                zet_grond(park, x, z, WATER_Y - 3, "gravel", "dirt")
                for y in range(WATER_Y - 2, WATER_Y + 1):
                    park.set(x, y, z, "water", level=0)
                park.water[x, z] = True
                for y in range(WATER_Y + 1, int(kap_blok) + 3):
                    if park.v[x, y, z] and not _naam(park, x, y, z).endswith(("_log", "_wood")):
                        park.v[x, y, z] = 0
    oevers(park, gezien)
    # banks: every dry neighbour at least y 63
    for (x, z) in list(gezien):
        for dx in (-2, -1, 0, 1, 2):
            for dz in (-2, -1, 0, 1, 2):
                xx, zz = x + dx, z + dz
                if (xx, zz) in gezien or park.water[xx, zz] or park.pad[xx, zz] or park.hek[xx, zz]:
                    continue
                if park.hoogte[xx, zz] < WATER_Y + 1:
                    zet_grond(park, xx, zz, WATER_Y + 1)
    brug = [(x, z) for (x, z) in gezien if park.pad[x, z]]
    if brug:
        # ramps: path cells near the bridge climb to the deck, 1 per block (round 2: deck 3 above the water)
        deck = max(int(park.hoogte[x, z]) for x, z in brug)
        for (x, z) in brug:
            park.set(x, deck, z, "stone_bricks")
            park.hoogte[x, z] = deck
        for (bx, bz) in brug:
            for dz in range(-4, 5):
                for dx in range(-3, 4):
                    xx, zz = bx + dx, bz + dz
                    if not park.pad[xx, zz] or (xx, zz) in gezien or park.water[xx, zz]:
                        continue
                    d = max(abs(dz), abs(dx))
                    doel = deck - d
                    if park.hoogte[xx, zz] < doel:
                        top = _naam(park, xx, int(park.hoogte[xx, zz]), zz)
                        zet_grond(park, xx, zz, doel, top, "dirt")
        # parapets on the deck edges (the brook stays open underneath)
        for x, z in brug:
            for dx in (-1, 1):
                if not park.pad[x + dx, z]:
                    for y in range(WATER_Y + 1, deck):
                        park.set(x + dx, y, z, "air")                     # the brook runs on under the deck edges
                    park.set(x + dx, deck, z, "stone_bricks")
                    park.set(x + dx, deck - 1, z, "stone_brick_stairs", facing="west" if dx < 0 else "east",
                             half="top", shape="straight", waterlogged="false")
                    park.set(x + dx, deck + 1, z, "stone_brick_wall", up="true", north="low", south="low",
                             east="none", west="none", waterlogged="false")
    # the watermill pond + the mill
    ex, ez = BEEK_EIND
    kap(park, ex + 4, ez, 12)
    for x in range(ex - 5, ex + 6):
        for z in range(ez - 5, ez + 6):
            if math.hypot(x - ex, z - ez) <= 4.6 and not park.pad[x, z] and not park.hek[x, z]:
                zet_grond(park, x, z, WATER_Y - 3, "gravel", "dirt")
                for y in range(WATER_Y - 2, WATER_Y + 1):
                    park.set(x, y, z, "water", level=0)
                park.water[x, z] = True
    molen(park, ex + 8, ez)


def oevers(park, cellen, breed=4):
    """Slope the banks of a brook down to it (y 63 next to the water, +1 per block), so the water is seen, not a trench."""
    cellen = set(cellen)
    for (x, z) in list(cellen):
        for dx in range(-breed, breed + 1):
            for dz in range(-breed, breed + 1):
                xx, zz = x + dx, z + dz
                if (xx, zz) in cellen or park.water[xx, zz] or park.pad[xx, zz] or park.hek_afstand[xx, zz] < 3:
                    continue
                d = max(abs(dx), abs(dz))
                doel = WATER_Y + d
                if park.hoogte[xx, zz] > doel:
                    zet_grond(park, xx, zz, doel)


def molen(park, x, z):
    """Watermolentje east of its pond: cobblestone base, white walls, oak frame, a gable roof, and a wheel in the pond."""
    g = max(int(park.hoogte[x + dx, z + dz]) for dx in range(-3, 4) for dz in range(-3, 4) if not park.water[x + dx, z + dz])
    vlak(park, x, z, 4.5, g)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            rand = abs(dx) == 3 or abs(dz) == 3
            hoek = abs(dx) == 3 and abs(dz) == 3
            park.set(x + dx, g, z + dz, "cobblestone")
            for y in range(g + 1, g + 6):
                if rand:
                    blok = kit.state("oak_log", axis="y") if hoek else ("cobblestone" if y == g + 1 else "white_terracotta")
                    park.set(x + dx, y, z + dz, blok)
                else:
                    park.set(x + dx, y, z + dz, "air")
    for y in (g + 3,):
        for dz in (-1, 1):                          # round 2: full glass (loose panes did not connect, looked broken)
            park.set(x + 3, y, z + dz, "glass")
        park.set(x, y, z - 3, "glass")
        park.set(x, y, z + 3, "glass")
    park.set(x + 3, g + 1, z, "air")
    park.set(x + 3, g + 2, z, "air")
    # gable roof (ridge along x)
    for i in range(0, 5):
        y = g + 6 + i
        for dx in range(-4, 5):
            for side, facing in ((-1, "south"), (1, "north")):
                zz = z + side * (4 - i)
                park.set(x + dx, y, zz, "spruce_stairs", facing=facing, half="bottom", shape="straight",
                         waterlogged="false")
            if i < 4:
                for dz in range(-(3 - i), 4 - i):
                    if abs(dx) == 3:
                        park.set(x + dx, y, z + dz, "white_terracotta")
        if i == 4:
            for dx in range(-4, 5):
                park.set(x + dx, y, z, "spruce_slab", type="bottom", waterlogged="false")
    # the wheel: a ring of planks with spokes, standing in the pond on the west side, axle into the wall
    wx = x - 4
    for a in range(0, 360, 15):
        r = 3
        wy = g + 1 + round(math.sin(math.radians(a)) * r)
        wz = z + round(math.cos(math.radians(a)) * r)
        park.set(wx, wy, wz, "oak_planks")
    for t in range(-2, 3):
        park.set(wx, g + 1 + t, z, kit.state("stripped_oak_log", axis="y"))
        park.set(wx, g + 1, z + t, kit.state("stripped_oak_log", axis="z"))
    park.set(wx + 1, g + 1, z, kit.state("oak_log", axis="x"))


def otterglijbaan(park):
    """A mud slide from the brook's south bank into the water."""
    x, z = OTTER
    # find the brook north of here
    for k in range(1, 12):
        if park.water[x, z - k]:
            wz = z - k
            break
    else:
        return
    top = int(park.hoogte[x, wz + 1]) + 2
    for i, zz in enumerate(range(wz + 1, wz + 6)):
        y = top - i // 1
        for dx in (-1, 0, 1):
            g = int(park.hoogte[x + dx, zz])
            for yy in range(g + 1, max(y, g + 1) + 1):
                park.set(x + dx, yy, zz, "packed_mud")
            park.set(x + dx, max(y, g + 1), zz, "mud")
        for dx in (-2, 2):
            park.set(x + dx, max(y, int(park.hoogte[x + dx, zz])) + 1, zz, "oak_fence")
    for dx in (-1, 0, 1):
        park.set(x + dx, top, wz + 1, "mud_brick_slab", type="bottom", waterlogged="false")


def holle_eik(park):
    x, z = HOLLE_EIK
    kap(park, x, z, 9)
    g = int(park.hoogte[x, z])
    vlak(park, x, z, 6, g)
    # trunk 5x5 hollow ring, a passage east-west through it at ground level
    for y in range(g + 1, g + 10):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                rand = abs(dx) == 2 or abs(dz) == 2
                hoek = abs(dx) == 2 and abs(dz) == 2
                if rand and not hoek:
                    gang = dz == 0 and abs(dx) == 2 and y <= g + 3
                    gang |= dz in (-1, 1) and abs(dx) == 2 and y <= g + 2
                    park.set(x + dx, y, z + dz, "air" if gang else "oak_wood")
                elif not rand:
                    park.set(x + dx, y, z + dz, "air" if y <= g + 6 else "oak_wood")
    # roots
    for dx, dz in ((3, 3), (-3, 3), (3, -3), (-3, -3), (0, 3), (0, -3)):
        park.set(x + dx, g + 1, z + dz, "oak_wood")
    # branches + a big crown
    for dx, dz, dy in ((3, 0, 8), (-3, 0, 9), (0, 3, 8), (0, -3, 9)):
        park.set(x + dx, g + dy, z + dz, kit.state("oak_log", axis="x" if dx else "z"))
        park.set(x + 2 * dx // 3 + (1 if dx > 0 else -1 if dx < 0 else 0), g + dy + 1, z + dz, kit.state("oak_log", axis="y"))
    park.blob(x, g + 12, z, 7.5, 4.0, 7.5, park.leaves("oak"), gat=0.12)
    # inside: the old ranger chest (geheimpje) in a nook, the hedgehog nest outside at the foot (leaves + hay)
    park.set(x, g + 1, z - 1, "chest", facing="south", type="single", waterlogged="false")
    for dx, dz in ((3, 1), (4, 1), (3, 2)):
        park.set(x + dx, g + 1, z + dz, "oak_leaves", distance=1, persistent="true", waterlogged="false")
    park.set(x + 4, g + 1, z + 2, "hay_block", axis="y")
    for dx, dz, c in ((5, 0, "orange_carpet"), (5, 1, "brown_carpet"), (4, 3, "red_carpet")):
        if park.v[x + dx, g + 1, z + dz] == 0:
            park.set(x + dx, g + 1, z + dz, c)


def dassenheuvel(park):
    """Round 2: the badger hill with a REAL viewing room. Inside the hill a closed burrow chamber (9 x 5 x 3, a nest,
    roots from the ceiling, soft light) where the dassen live (their home is the chamber, so they are always in view);
    south of it, behind a wall of one-way glass 3 high, the visitors' kijkkamer with a bench, reached by a short tunnel
    from the south foot of the hill. The chamber is closed on every side (glass included), nothing can leave it."""
    x, z = DAS
    kap(park, x, z, 14)
    g = int(park.hoogte[x, z])
    vlak(park, x, z, 12, g)
    r = 11
    for xx in range(x - r, x + r + 1):
        for zz in range(z - r, z + r + 1):
            d = math.hypot(xx - x, zz - z)
            if d > r or park.hek[xx, zz] or park.pad[xx, zz] or park.water[xx, zz]:
                continue
            top = g + int(round(7 * math.cos(d / r * math.pi / 2)))
            if top > park.hoogte[xx, zz]:
                zet_grond(park, xx, zz, top)
    # everything inside the hill from g up to the surface is solid dirt first (no old caves)
    for xx in range(x - 6, x + 7):
        for zz in range(z - 5, z + 13):
            for y in range(g, int(park.hoogte[xx, zz])):
                park.set(xx, y, zz, "dirt")
    # the burrow chamber (z - 3 .. z + 1), floor g, air g+1..g+3, ceiling g+4 (dirt, a few shroomlights)
    for xx in range(x - 4, x + 5):
        for zz in range(z - 3, z + 2):
            park.set(xx, g, zz, "rooted_dirt")
            for y in range(g + 1, g + 4):
                park.set(xx, y, zz, "air")
            park.set(xx, g + 4, zz, "dirt")
    for (lx, lz) in ((x - 2, z - 2), (x + 3, z - 1)):
        park.set(lx, g + 4, lz, "shroomlight")
    for (hx, hz) in ((x - 4, z - 3), (x + 1, z - 3), (x + 4, z), (x - 1, z + 1)):
        park.set(hx, g + 3, hz, "hanging_roots", waterlogged="false")
    for (nx, nz) in ((x + 2, z - 2), (x + 3, z - 2), (x + 3, z - 3)):              # the nest: hay + leaf litter
        park.set(nx, g + 1, nz, "hay_block", axis="y")
    for (cx, cz, c) in ((x + 1, z - 2, "brown_carpet"), (x + 2, z - 1, "brown_carpet"), (x - 3, z, "moss_carpet")):
        park.set(cx, g + 1, cz, c)
    # the glass wall (z + 2): one-way glass 3 high over the whole chamber width
    for xx in range(x - 4, x + 5):
        park.set(xx, g, z + 2, "rooted_dirt")
        for y in range(g + 1, g + 4):
            park.set(xx, y, z + 2, KIJKGLAS)
        park.set(xx, g + 4, z + 2, "dirt")
    # the viewing room (z + 3 .. z + 5): planks floor, bench against the back wall, lanterns, a sign over the door
    for xx in range(x - 4, x + 5):
        for zz in range(z + 3, z + 6):
            park.set(xx, g, zz, "spruce_planks")
            for y in range(g + 1, g + 4):
                park.set(xx, y, zz, "air")
            park.set(xx, g + 4, zz, kit.state("spruce_log", axis="x"))
    for xx in range(x - 3, x + 4):
        if xx not in (x, x + 1):
            kit.stoel(park, xx, g + 1, z + 5, "north")              # guh chairs facing the burrow glass
    for lx in (x - 3, x + 3):
        park.set(lx, g + 3, z + 4, "lantern", hanging="true", waterlogged="false")
    # the entrance tunnel south, 2 wide, out of the hill; a little timber frame at the mouth
    zz = z + 6
    while zz < z + r + 3 and park.hoogte[x, zz] > g:
        for xx in (x, x + 1):
            park.set(xx, g, zz, "spruce_planks")
            for y in range(g + 1, g + 3):
                park.set(xx, y, zz, "air")
            park.set(xx, g + 3, zz, kit.state("spruce_log", axis="x"))
        for xx in (x - 1, x + 2):
            for y in range(g + 1, g + 3):
                park.set(xx, y, zz, kit.state("spruce_log", axis="y") if (zz - z) % 3 == 0 else "dirt")
        zz += 1
    for xx in (x, x + 1):                                                         # the path in front of the mouth
        for k in range(0, 3):
            if not park.pad[xx, zz + k] and not park.hek[xx, zz + k]:
                zet_grond(park, xx, zz + k, g, "dirt_path", "dirt")
    for xx in (x - 1, x + 2):
        park.set(xx, g + 1, zz, kit.state("spruce_log", axis="y"))
        park.set(xx, g + 2, zz, kit.state("spruce_log", axis="y"))
        park.set(xx, g + 3, zz, kit.state("spruce_log", axis="y"))
    for xx in (x - 1, x, x + 1, x + 2):
        park.set(xx, g + 3, zz, kit.state("spruce_log", axis="x"))
    if not hasattr(park, "bordjes"):
        park.bordjes = []
    park.set(x - 2, g + 1, zz, "spruce_sign", rotation=0, waterlogged="false")
    park.bordjes.append({"pos": [x - 2, g + 1, zz], "tekst": ["Dassenburcht", "kijkkamer", "sssst...", "ze slapen vaak"]})
    if not hasattr(park, "kijkpunten"):
        park.kijkpunten = []
    park.kijkpunten.append({"naam": "dassen-kijkkamer", "oog": (x + 0.5, g + 2.6, z + 4.5), "kijk": "north"})
    # a few fake burrow holes on the hill's flanks (decor only, closed underneath)
    for (ex, ez) in ((x - 8, z - 2), (x + 8, z - 3), (x - 2, z - 9)):
        top = int(park.hoogte[ex, ez])
        park.set(ex, top, ez, "coarse_dirt")
        park.set(ex, top + 1, ez, "brown_carpet")
    park.das_kamer = (x, g + 1, z - 1)


def paddenstoelenkring(park):
    x, z = KRING
    kap(park, x, z, 6)
    g = int(park.hoogte[x, z])
    vlak(park, x, z, 5, g, "podzol")
    for a in range(0, 360, 30):
        xx = x + round(math.cos(math.radians(a)) * 3.5)
        zz = z + round(math.sin(math.radians(a)) * 3.5)
        park.set(xx, g + 1, zz, "red_mushroom" if a % 60 == 0 else "brown_mushroom")
    for dx, dz in ((0, 0), (1, 0), (0, 1)):
        park.set(x + dx, g, z + dz, "moss_block")
    park.set(x, g + 1, z, "moss_carpet")
    # mossy stones for the vuursalamander
    for dx, dz in ((5, 2), (6, 3), (-5, 4)):
        park.set(x + dx, int(park.hoogte[x + dx, z + dz]) + 1, z + dz, "mossy_cobblestone")


def herfsthoekje(park):
    """Autumn corner: leaf piles (carpets) under the trees in coloured heaps, a few fallen logs."""
    x, z = HERFST
    rng = np.random.default_rng(kit.SEED + 71)
    kleuren = ["orange_carpet", "red_carpet", "yellow_carpet", "brown_carpet"]
    for xx in range(x - 10, x + 11):
        for zz in range(z - 10, z + 11):
            d = math.hypot(xx - x, zz - z)
            if d > 10 or park.water[xx, zz] or park.pad[xx, zz] or park.hek_afstand[xx, zz] < 3:
                continue
            if rng.random() < 0.55 * (1 - d / 12):
                g = int(park.hoogte[xx, zz])
                n = _naam(park, xx, g + 1, zz)
                if park.v[xx, g + 1, zz] == 0 or n.endswith(("short_grass", "fern")):
                    park.set(xx, g + 1, zz, kleuren[int(rng.integers(0, 4))])
    for (fx, fz, axis) in ((x - 3, z + 2, "x"), (x + 4, z - 3, "z")):
        g = int(park.hoogte[fx, fz])
        for t in range(3):
            xx, zz = (fx + t, fz) if axis == "x" else (fx, fz + t)
            park.set(xx, int(park.hoogte[xx, zz]) + 1, zz, "oak_log", axis=axis)


def hertenweide(park):
    x, z = WEIDE
    kap(park, x, z, 12)
    rng = np.random.default_rng(kit.SEED + 72)
    for xx in range(x - 12, x + 13):
        for zz in range(z - 12, z + 13):
            if math.hypot(xx - x, zz - z) <= 12 and not park.water[xx, zz] and not park.pad[xx, zz]:
                g = int(park.hoogte[xx, zz])
                if park.v[xx, g + 1, zz] == 0 and _naam(park, xx, g, zz) == "minecraft:grass_block":
                    r = rng.random()
                    if r < 0.35:
                        park.set(xx, g + 1, zz, "short_grass")
                    elif r < 0.42:
                        park.set(xx, g + 1, zz, ["oxeye_daisy", "dandelion", "cornflower", "poppy"][int(rng.integers(0, 4))])
    # hay feeder (voerruif)
    g = int(park.hoogte[x, z])
    for dx in (-1, 0, 1):
        park.set(x + dx, g + 1, z, "hay_block", axis="x")
        park.set(x + dx, g + 2, z, "spruce_fence")
        park.set(x + dx, g + 3, z, "spruce_slab", type="bottom", waterlogged="false")
    for dx in (-2, 2):
        for y in range(g + 1, g + 4):
            park.set(x + dx, y, z, kit.state("spruce_log", axis="y"))


def kikkervijver(park):
    """Round 2, east side: a frog pond with reeds, lily pads and a plank jetty, the ooievaars wade in it."""
    x, z = VIJVER
    kap(park, x, z, 14)
    rng = np.random.default_rng(kit.SEED + 75)
    for xx in range(x - 13, x + 14):
        for zz in range(z - 11, z + 12):
            d = math.hypot((xx - x) / 1.3, zz - z)
            if park.pad[xx, zz] or park.hek_afstand[xx, zz] < 4:
                continue
            if d <= 7:
                zet_grond(park, xx, zz, WATER_Y - 3 if d < 4.5 else WATER_Y - 1, "mud" if d < 5 else "clay", "dirt")
                for y in range(int(park.hoogte[xx, zz]) + 1, WATER_Y + 1):
                    park.set(xx, y, zz, "water", level=0)
                park.water[xx, zz] = True
            elif d <= 9.5:
                zet_grond(park, xx, zz, WATER_Y + 1, "grass_block", "dirt")
                if d <= 8.2 and rng.random() < 0.45 and park.v[xx, WATER_Y + 2, zz] == 0:
                    park.set(xx, WATER_Y + 2, zz, "sugar_cane", age=0)
                    if rng.random() < 0.6:
                        park.set(xx, WATER_Y + 3, zz, "sugar_cane", age=0)
    for _ in range(14):
        a, rr = rng.random() * 2 * math.pi, rng.random() * 6
        lx, lz = int(x + math.cos(a) * rr * 1.3), int(z + math.sin(a) * rr)
        if park.water[lx, lz] and park.v[lx, WATER_Y + 1, lz] == 0:
            park.set(lx, WATER_Y + 1, lz, "lily_pad")
    # the jetty from the south bank into the pond, toward the middle (rails of fences, a lantern post at the end)
    for zz in range(z + 9, z + 2, -1):
        for xx in (x - 1, x, x + 1):
            if park.water[xx, zz]:
                park.set(xx, WATER_Y - 2, zz, kit.state("spruce_log", axis="y")) if xx != x and zz % 2 else None
            park.set(xx, WATER_Y + 1, zz, "spruce_planks")
            if park.water[xx, zz]:
                for y in range(WATER_Y - 1, WATER_Y + 1):
                    if xx != x and zz % 2:
                        park.set(xx, y, zz, kit.state("spruce_log", axis="y"))
        for xx in (x - 2, x + 2):
            if park.water[xx, zz]:
                park.set(xx, WATER_Y + 2, zz, "spruce_fence")
    park.set(x + 1, WATER_Y + 2, z + 3, "spruce_fence")
    park.set(x + 1, WATER_Y + 3, z + 3, "lantern", hanging="false", waterlogged="false")


def kasteelruine(park):
    """Round 2, east side: a little overgrown castle on a low hill: a broken square keep with an arched gate, one corner
    tower still standing, mossy and cracked stone bricks, ivy (vines) hanging down, rubble, a flower meadow around."""
    x, z = RUINE
    kap(park, x, z, 13)
    g = int(park.hoogte[x, z])
    r = 11
    for xx in range(x - r, x + r + 1):
        for zz in range(z - r, z + r + 1):
            d = math.hypot(xx - x, zz - z)
            if d > r or park.hek[xx, zz] or park.pad[xx, zz] or park.water[xx, zz] or park.hek_afstand[xx, zz] < 4:
                continue
            top = g + int(round(3 * math.cos(d / r * math.pi / 2)))
            if top != park.hoogte[xx, zz]:
                zet_grond(park, xx, zz, top)
    top = g + 3
    rng = np.random.default_rng(kit.SEED + 76)
    stenen = ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks", "stone_bricks"]
    # the keep: 9 x 9 walls, broken off at different heights
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            rand = abs(dx) == 4 or abs(dz) == 4
            park.set(x + dx, top, z + dz, "mossy_cobblestone" if rng.random() < 0.4 else "cobblestone")
            if not rand:
                if rng.random() < 0.18:
                    park.set(x + dx, top + 1, z + dz, ["mossy_cobblestone", "cobblestone_slab", "moss_carpet"][
                        int(rng.integers(0, 3))], **({"type": "bottom", "waterlogged": "false"}
                                                       if False else {}))
                continue
            hoog = int(2 + 4 * (0.5 + 0.5 * math.sin(dx * 0.9 + dz * 1.7)) + rng.integers(0, 2))
            if dz == 4 and abs(dx) <= 1:                                   # the gate (south side, toward the pond)
                hoog = 0 if abs(dx) <= 0 else hoog
            for y in range(top + 1, top + 1 + hoog):
                if dz == 4 and abs(dx) <= 1 and y <= top + 3:
                    continue
                park.set(x + dx, y, z + dz, stenen[int(rng.integers(0, len(stenen)))])
    # the arch over the gate
    for dx in (-1, 0, 1):
        park.set(x + dx, top + 4, z + 4, "stone_brick_stairs" if dx else "chiseled_stone_bricks",
                 **({"facing": "east" if dx < 0 else "west", "half": "top", "shape": "straight",
                     "waterlogged": "false"} if dx else {}))
    # the corner tower (north-east), round-ish 5 x 5, 11 high with crenellations and an arrow slit window
    tx, tz = x + 4, z - 4
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 and abs(dz) == 2:
                continue
            rand = abs(dx) == 2 or abs(dz) == 2
            for y in range(top, top + 11):
                if rand:
                    park.set(tx + dx, y, tz + dz, stenen[int(rng.integers(0, len(stenen)))])
                elif y > top:
                    park.set(tx + dx, y, tz + dz, "air")
            if rand and (dx + dz) % 2 == 0:
                park.set(tx + dx, top + 11, tz + dz, "stone_brick_wall", up="true", north="none", south="none",
                         east="none", west="none", waterlogged="false")
    park.set(tx, top + 6, tz + 2, "iron_bars")                             # arrow slit toward the pond
    park.set(tx, top + 7, tz + 2, "iron_bars")
    # ivy: vines down the outer walls and the tower
    for _ in range(60):
        dx, dz = int(rng.integers(-5, 6)), int(rng.integers(-5, 6))
        vx, vz = x + dx, z + dz
        for kant, (ox, oz) in (("north", (0, 1)), ("south", (0, -1)), ("east", (-1, 0)), ("west", (1, 0))):
            nx, nz = vx + ox, vz + oz
            if park.v[vx, top + 2, vz] == 0 and park.v[nx, top + 2, nz] != 0 and \
                    _naam(park, nx, top + 2, nz).endswith(("bricks", "cobblestone")):
                lengte = int(rng.integers(1, 4))
                for y in range(top + 3, top + 3 - lengte, -1):
                    if park.v[vx, y, vz] == 0 and park.v[nx, y, nz] != 0:
                        park.set(vx, y, vz, "vine", **{kant: "true", "up": "false",
                                                      **{k: "false" for k in ("north", "south", "east", "west")
                                                         if k != kant}})
                break
    # rubble around and a flower meadow on the hill
    for _ in range(18):
        a, rr = rng.random() * 2 * math.pi, 5 + rng.random() * 4
        bx, bz = int(x + math.cos(a) * rr), int(z + math.sin(a) * rr)
        gb = int(park.hoogte[bx, bz])
        if park.v[bx, gb + 1, bz] == 0 and not park.water[bx, bz] and park.hek_afstand[bx, bz] >= 4:
            park.set(bx, gb + 1, bz, ["mossy_cobblestone", "cobblestone_wall", "stone_brick_slab"][
                int(rng.integers(0, 3))], **({} if True else {}))
    bloemen = ["oxeye_daisy", "cornflower", "poppy", "dandelion", "allium", "azure_bluet", "red_tulip", "white_tulip"]
    for xx in range(x - r - 4, x + r + 5):
        for zz in range(z - r - 4, z + r + 5):
            d = math.hypot(xx - x, zz - z)
            if d < 5.5 or d > r + 4 or park.water[xx, zz] or park.pad[xx, zz] or park.hek_afstand[xx, zz] < 3:
                continue
            gb = int(park.hoogte[xx, zz])
            if park.v[xx, gb + 1, zz] == 0 and _naam(park, xx, gb, zz) == "minecraft:grass_block" and rng.random() < 0.3:
                park.set(xx, gb + 1, zz, bloemen[int(rng.integers(0, len(bloemen)))])
    park.ruine_top = top


def ooievaarsnest(park):
    x, z = OOIEVAAR
    kap(park, x, z, 3)
    g = int(park.hoogte[x, z])
    for y in range(g + 1, g + 9):
        park.set(x, y, z, kit.state("spruce_log", axis="y"))
    top = g + 9
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            park.set(x + dx, top, z + dz, "hay_block", axis="y")
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2), (-1, -2), (1, 2), (2, -1), (-2, 1)):
        park.set(x + dx, top + 1, z + dz, "spruce_fence")
    park.set(x, top + 1, z, "brown_carpet")


def bouw(park, log=print):
    for naam in ("beverdam", "beek", "otterglijbaan", "holle_eik", "dassenheuvel", "paddenstoelenkring",
                 "herfsthoekje", "hertenweide", "ooievaarsnest", "kikkervijver", "kasteelruine"):
        globals()[naam](park)
    for (x, z) in (TOREN,):
        kap(park, x, z, 6)
        vlak(park, x, z, 3.5, int(park.hoogte[x, z]))
        toren(park, x, z, hout="spruce", hoogte=9)
    kap(park, HUT[0], HUT[1] - 4, 7)
    kijkhut(park, HUT[0], HUT[1], kijk="south", hout="spruce", dak="dark_oak", naam="bever-kijkhut", zicht=20)
    kap(park, OOST_HUT[0], OOST_HUT[1] + 4, 7)
    kijkhut(park, OOST_HUT[0], OOST_HUT[1], kijk="north", hout="oak", dak="spruce", naam="vijver-kijkhut", zicht=15)
    dicht_water(park, ["europa"], log)
    for naam, (x, z, r) in {"toren": (TOREN[0], TOREN[1], 5), "das": (DAS[0], DAS[1], 11),
                             "bever": (BEVER[0], BEVER[1], 11), "ruine": (RUINE[0], RUINE[1], 11),
                             "vijver": (VIJVER[0], VIJVER[1], 10)}.items():
        if not ver_van_hek(park, x, z, r):
            log(f"  LET OP europa: {naam} ligt dicht bij een hek")
    from . import preview
    for spot in (("park_iso_europa_bever", 196 - 128, 323 - 128), ("park_iso_europa_beek", 300 - 128, 335 - 128),
                 ("park_iso_europa_das", DAS[0] - 128, DAS[1] - 128),
                 ("park_iso_europa_oost", 382 - 128, 315 - 128)):
        if spot not in preview.SPOTS:
            preview.SPOTS.append(spot)


def plekken(park, plekken_):
    # round 2: the beavers on both flanks of the lodge, in full view of the bever-kijkhut (north bank)
    zet_huizen(park, plekken_, "bever", [(BEVER[0] + 11, BEVER[1] - 5)], "europa")
    extra_huis(park, plekken_, "bever", (BEVER[0] - 12, BEVER[1] - 7), "europa")
    zet_huizen(park, plekken_, "otter", [(OTTER[0] - 6, OTTER[1] - 2), (OTTER[0] + 12, OTTER[1] - 8),
                                         (OTTER[0] - 22, OTTER[1] + 4)], "europa")
    kx, ky, kz = park.das_kamer
    zet_huizen(park, plekken_, "das", [(kx, kz)], "europa", y=ky, straal=3)       # in the burrow chamber
    zet_huizen(park, plekken_, "egel", [(HOLLE_EIK[0] + 5, HOLLE_EIK[1] + 3), (HERFST[0] + 2, HERFST[1] + 1),
                                        (HERFST[0] - 4, HERFST[1] + 5)], "europa")
    zet_huizen(park, plekken_, "vuursalamander", [(KRING[0] + 4, KRING[1] + 3)], "europa")
    zet_huizen(park, plekken_, "edelhert", [(WEIDE[0] - 3, WEIDE[1] + 2), (WEIDE[0] + 4, WEIDE[1] - 3)], "europa")
    zet_huizen(park, plekken_, "ooievaar", [(OOIEVAAR[0] + 2, OOIEVAAR[1] + 2), (VIJVER[0] - 3, VIJVER[1] - 8)],
               "europa")
    zet_huizen(park, plekken_, "specht", [(HOLLE_EIK[0] - 3, HOLLE_EIK[1] - 4), (RUINE[0] - 7, RUINE[1] + 7)], "europa")
    # round 2, the east side: animals in front of the vijver-kijkhut (ooievaars in the pond, deer and a hedgehog on the
    # meadow below the ruin, a woodpecker at the ruin)
    extra_huis(park, plekken_, "ooievaar", (VIJVER[0], VIJVER[1] - 5), "europa", y=WATER_Y, volwassenen=2, jongen=1,
               straal=6)
    extra_huis(park, plekken_, "edelhert", (RUINE[0] - 8, RUINE[1] + 12), "europa")
    extra_huis(park, plekken_, "egel", (RUINE[0] - 3, RUINE[1] + 8), "europa", volwassenen=2, jongen=2, straal=5)
    extra_huis(park, plekken_, "specht", (RUINE[0] - 6, RUINE[1] + 6), "europa")
