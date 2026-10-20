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
        park.set(wx, g + 2, wz, "glass_pane")
    park.set(x + dx_, g + 1, z + dz_, "air")
    park.set(x + dx_, g + 2, z + dz_, "air")
    park.set(x, g + 1, z, "spruce_slab", type="bottom", waterlogged="false")          # a little bench
    park.set(x + (1 if not ox else 0), g + 1, z + (1 if ox else 0), "spruce_slab", type="bottom", waterlogged="false")


def zone_cel_ok(park, x, z, zone):
    """True when the 16x16 cell of (x, z) has `zone` as majority (that is what the game reads from zones.json)."""
    cx, cz = x // 16, z // 16
    blok = park.zone[cx * 16:(cx + 1) * 16, cz * 16:(cz + 1) * 16]
    binnen = blok[blok >= 0]
    if binnen.size < 64:
        return False
    return int(np.bincount(binnen).argmax()) == ZONES.index(zone)


def zet_huizen(park, plekken, soort, punten, zone):
    """Give the homes of `soort` the points given (cycled), standing height recomputed, cell zone checked."""
    eigen = [p for p in plekken if p["soort"] == soort]
    for i, p in enumerate(eigen):
        x, z = punten[i % len(punten)]
        if not zone_cel_ok(park, x, z, zone):
            raise SystemExit(f"{soort}: plek {x},{z} ligt in de zones.json-cel van een andere zone dan {zone}")
        y = grond_y(park, x, z) if not p.get("_water_y") else WATER_Y
        p["centrum"] = [int(x), int(y), int(z)]


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
HUT = (196, 306)              # looks south over the beaver pond


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
    # the pond: two blocks higher than the river (the dam holds it), banks at least y 65
    py = WATER_Y + 2
    for x in range(BEVER[0] - 12, BEVER[0] + 13):
        for z in range(BEVER[1] - 12, BEVER[1] + 13):
            d = math.hypot((x - BEVER[0]) / 1.25, z - BEVER[1])
            if x >= DAM_X:
                continue
            if d <= 7.5:
                zet_grond(park, x, z, py - 3 if d < 5 else py - 2, "mud" if d < 6 else "gravel", "dirt")
                for y in range(int(park.hoogte[x, z]) + 1, py + 1):
                    park.set(x, y, z, "water", level=0)
                park.water[x, z] = True
            elif d <= 10.5 and park.hoogte[x, z] < py + 1:
                zet_grond(park, x, z, py + 1)
    # the dam: logs across the channel, sticks (fences) and mud packed in, from the bed up to y 65
    for z in range(323 - 4, 323 + 5):
        bed = WATER_Y - 3
        for y in range(bed, py + 2):
            x = DAM_X + (1 if (z + y) % 3 == 0 else 0)
            blok = kit.state("spruce_log", axis="z") if y % 2 else kit.state("oak_log", axis="z")
            park.set(DAM_X, y, z, blok if y < py + 1 else "packed_mud")
            park.set(DAM_X - 1, y, z, "mud" if y <= py else "air")
        park.set(DAM_X, py + 2, z, "dark_oak_fence" if z % 2 else "air")    # sticking-out sticks
        park.water[DAM_X, z] = False
        park.hoogte[DAM_X, z] = py + 1
    # a little trickle over the dam
    park.set(DAM_X + 1, py, 323, "water", level=0)
    # the lodge (burcht): a mound of logs and mud in the pond, entrance under water
    bx, bz = BEVER[0] - 3, BEVER[1]
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(0, 4):
                d = math.sqrt(dx * dx + dz * dz + (dy * 1.3) ** 2)
                if d <= 3.4:
                    y = py - 2 + dy
                    binnen = d <= 2.0 and dy >= 1
                    if binnen:
                        park.set(bx + dx, y, bz + dz, "air")
                    else:
                        r = (dx * 7 + dz * 3 + dy) % 5
                        park.set(bx + dx, y, bz + dz,
                                 kit.state("spruce_log", axis="x") if r == 0 else
                                 kit.state("oak_log", axis="z") if r == 1 else
                                 "packed_mud" if r == 2 else "mud" if r == 3 else "dark_oak_fence")
    for y in range(py - 2, py + 3):
        park.set(bx + 1, y, bz + 4, "dark_oak_fence")
    # stumps (gnawed trees) on the banks
    for sx, sz in ((186, 312), (190, 334), (205, 333), (206, 312), (183, 324)):
        g = int(park.hoogte[sx, sz])
        if not park.water[sx, sz]:
            park.set(sx, g + 1, sz, "stripped_oak_log", axis="y")
            park.set(sx, g + 2, sz, "oak_button", face="floor", facing="north", powered="false")


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
                    # the stone bridge: deck at the path's height (at least water + 1), arch blocks under it
                    deck = max(int(park.hoogte[x, z]), WATER_Y + 1)
                    for y in range(WATER_Y - 2, deck):
                        park.set(x, y, z, "water", level=0)
                    park.set(x, deck, z, "stone_bricks")
                    park.hoogte[x, z] = deck
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
    # bridge railings: stone brick walls on the deck cells that border the brook on the path's sides
    for (x, z) in list(gezien):
        if park.pad[x, z]:
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, zz = x + dx, z + dz
                if not park.pad[xx, zz] and park.water[xx, zz]:
                    pass
    brug = [(x, z) for (x, z) in gezien if park.pad[x, z]]
    if brug:
        xs = [x for x, _ in brug]
        zs = [z for _, z in brug]
        # the path runs mainly north-south here: walls on the west and east edge of the deck
        for x, z in brug:
            for dx in (-1, 1):
                if not park.pad[x + dx, z]:
                    park.set(x + dx, int(park.hoogte[x, z]), z, "stone_bricks")
                    park.set(x + dx, int(park.hoogte[x, z]) + 1, z, "stone_brick_wall", up="true", north="low",
                             south="low", east="none", west="none", waterlogged="false")
                    park.water[x + dx, z] = False
        # arch stairs under the deck edges, both sides of the brook
        for x, z in brug:
            y = int(park.hoogte[x, z]) - 1
            if park.get(x, y, z).startswith("minecraft:water"):
                park.set(x, y, z, "stone_brick_stairs", facing="north", half="top", shape="straight",
                         waterlogged="true")
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
        for dz in (-1, 1):
            park.set(x + 3, y, z + dz, "glass_pane")
        park.set(x, y, z - 3, "glass_pane")
        park.set(x, y, z + 3, "glass_pane")
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
    x, z = DAS
    kap(park, x, z, 13)
    g = int(park.hoogte[x, z])
    r = 10
    for xx in range(x - r, x + r + 1):
        for zz in range(z - r, z + r + 1):
            d = math.hypot(xx - x, zz - z)
            if d > r or park.hek[xx, zz] or park.pad[xx, zz] or park.water[xx, zz]:
                continue
            top = g + int(round(5 * math.cos(d / r * math.pi / 2)))
            if top > park.hoogte[xx, zz]:
                zet_grond(park, xx, zz, top)
    # tunnels: an east-west and a north-south gang 1 above the old ground, with chambers, windows on the south side
    for t in range(-7, 8):
        for (ax, az) in ((x + t, z), (x, z + t)):
            for y in (g + 1, g + 2):
                park.set(ax, y, az, "air")
            park.set(ax, g, az, "rooted_dirt")
    for cx, cz in ((x - 4, z), (x + 4, z), (x, z - 4)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for y in (g + 1, g + 2):
                    park.set(cx + dx, y, cz + dz, "air")
                park.set(cx + dx, g, cz + dz, "rooted_dirt")
        park.set(cx, g + 1, cz, "brown_carpet")
    # entrances (holes in the slope) at the ends
    for (ex, ez) in ((x - 8, z), (x + 8, z), (x, z - 8)):
        for y in (g + 1, g + 2):
            park.set(ex, y, ez, "air")
    # kijkvenstertjes: glass in the south slope looking into the south tunnel
    for zz in range(z + 2, z + 8, 2):
        top = int(park.hoogte[x + 1, zz])
        for y in range(g + 1, top + 1):
            if y > g + 2:
                park.set(x + 1, y, zz, "glass")
        park.set(x + 1, g + 1, zz, "glass")
        park.set(x + 1, g + 2, zz, "glass")


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
                 "herfsthoekje", "hertenweide", "ooievaarsnest"):
        globals()[naam](park)
    for (x, z) in (TOREN,):
        kap(park, x, z, 6)
        vlak(park, x, z, 3.5, int(park.hoogte[x, z]))
        toren(park, x, z, hout="spruce", hoogte=9)
    kap(park, HUT[0], HUT[1], 5)
    schuilhut(park, HUT[0], HUT[1], kijk="south")
    for naam, (x, z, r) in {"toren": (TOREN[0], TOREN[1], 5), "das": (DAS[0], DAS[1], 11),
                             "bever": (BEVER[0], BEVER[1], 11)}.items():
        if not ver_van_hek(park, x, z, r):
            log(f"  LET OP europa: {naam} ligt dicht bij een hek")
    from . import preview
    for spot in (("park_iso_europa_bever", 196 - 128, 323 - 128), ("park_iso_europa_beek", 300 - 128, 335 - 128)):
        if spot not in preview.SPOTS:
            preview.SPOTS.append(spot)


def plekken(park, plekken_):
    zet_huizen(park, plekken_, "bever", [(BEVER[0] + 4, BEVER[1] + 5), (BEVER[0] + 2, BEVER[1] - 5)], "europa")
    zet_huizen(park, plekken_, "otter", [(OTTER[0] - 6, OTTER[1] - 2), (OTTER[0] + 12, OTTER[1] - 8),
                                         (OTTER[0] - 22, OTTER[1] + 4)], "europa")
    zet_huizen(park, plekken_, "das", [(DAS[0] - 4, DAS[1] + 1), (DAS[0] + 4, DAS[1] + 1)], "europa")
    zet_huizen(park, plekken_, "egel", [(HOLLE_EIK[0] + 5, HOLLE_EIK[1] + 3), (HERFST[0] + 2, HERFST[1] + 1),
                                        (HERFST[0] - 4, HERFST[1] + 5)], "europa")
    zet_huizen(park, plekken_, "vuursalamander", [(KRING[0] + 4, KRING[1] + 3)], "europa")
    zet_huizen(park, plekken_, "edelhert", [(WEIDE[0] - 3, WEIDE[1] + 2), (WEIDE[0] + 4, WEIDE[1] - 3)], "europa")
    zet_huizen(park, plekken_, "ooievaar", [(OOIEVAAR[0] + 2, OOIEVAAR[1] + 2), (BEEK_EIND[0] - 6, BEEK_EIND[1] + 6)],
               "europa")
    zet_huizen(park, plekken_, "specht", [(HOLLE_EIK[0] - 3, HOLLE_EIK[1] - 4)], "europa")
