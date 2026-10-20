"""
Zone module savanne (design 1.4): de Trotse Rots, de Vadse Baobab bij de grote drinkpoel, termietenheuvels, de trekvlakte
met hoog droog gras (met het geheimpje), een acacia-uitkijktoren met verrekijker, een schuilhut met kijkgleuven aan de poel
en de truckhalte met overdekt uitkijkplatform bij de brug naar de delta (zoals de knooppunt-render).

Also the shared little helpers of the zone modules (zone_delta imports them): finding a free spot, levelling a column,
a lookout tower, a hide, a sign with text (kept on park.bordjes for the tile writer, see _bord).
Escape-proof rule (design 4.5c): anything that stands more than 1 block above the ground keeps >= 4 blocks from a fence.
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit
from .terrein import ZONES

W = kit.W
WATER_Y = kit.WATER_Y
PLANTEN = ("minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern", "minecraft:dead_bush",
           "minecraft:snow", "minecraft:poppy", "minecraft:dandelion", "minecraft:sugar_cane", "minecraft:sweet_berry_bush",
           "minecraft:azure_bluet", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:pink_petals")
HOUT = ("_log", "_leaves", "_wood", "minecraft:bamboo", "minecraft:vine", "minecraft:mangrove_roots", "propagule")


# ======================================================================================================================
# helpers (also used by zone_delta)
def zi(naam):
    return ZONES.index(naam)


def pad_afstand(park):
    if getattr(park, "_pad_d", None) is None or park._pad_d.shape != park.pad.shape:
        park._pad_d = distance_transform_edt(~park.pad)
    return park._pad_d


def is_plant(s):
    return s.startswith(PLANTEN)


def is_hout(s):
    return any(h in s for h in HOUT)


def vrij_van_hout(park, x0, z0, x1, z1, hoog=14):
    """No tree blocks (logs, leaves, bamboo, vines) in the box above the ground."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            g = int(park.hoogte[x, z])
            for y in range(g + 1, min(g + hoog, kit.H)):
                if park.v[x, y, z] and is_hout(park.get(x, y, z)):
                    return False
    return True


def ruim(park, x, z, van, tot):
    """Clear a column between y van..tot (inclusive)."""
    park.v[int(x), max(int(van), 0):min(int(tot) + 1, kit.H), int(z)] = 0


def vlak(park, x, z, y, top="grass_block", onder="dirt", vrij=8):
    """Make the ground at (x, z) exactly y: clear above (vrij blocks), fill below down to the column floor."""
    x, z, y = int(x), int(z), int(y)
    ruim(park, x, z, y + 1, y + vrij)
    for yy in range(int(min(park.vloer[x, z], y)), y):
        if park.v[x, yy, z] == 0 or park.get(x, yy, z).startswith("minecraft:water"):
            park.set(x, yy, z, onder)
    park.set(x, y, z, top, **({"snowy": "false"} if top == "grass_block" else {}))
    park.hoogte[x, z] = y
    park.water[x, z] = False


def onderbouw(park, x, z, y, blok="dirt"):
    """Solid from the column floor up to y-1 (nothing floating, no air pockets under a structure)."""
    x, z = int(x), int(z)
    for yy in range(int(min(park.vloer[x, z], park.hoogte[x, z])), int(y)):
        if park.v[x, yy, z] == 0:
            park.set(x, yy, z, blok)


def zoek(park, zone, rond, r_zoek, half, minhek=5.0, pad_min=3.0, pad_max=None, helling=2, water_ok=False,
         weg_van=(), weg_r=0):
    """A spot (x, z, y) for a square footprint of half-width `half` in `zone`, as close to `rond` as possible:
    inside the park and zone, no water (unless water_ok), no path closer than pad_min (and the path within pad_max if
    given), at least minhek from any fence, ground slope <= helling, no tree blocks, and weg_r away from weg_van."""
    zone_i = zi(zone) if isinstance(zone, str) else zone
    pd = pad_afstand(park)
    rx, rz = int(rond[0]), int(rond[1])
    kand = []
    for x in range(rx - r_zoek, rx + r_zoek + 1):
        for z in range(rz - r_zoek, rz + r_zoek + 1):
            kand.append((math.hypot(x - rx, z - rz), x, z))
    kand.sort()
    for _, x, z in kand:
        x0, x1, z0, z1 = x - half, x + half, z - half, z + half
        if x0 < 1 or z0 < 1 or x1 >= W - 1 or z1 >= W - 1:
            continue
        zs = park.zone[x0:x1 + 1, z0:z1 + 1]
        if (zs != zone_i).any() or not park.binnen[x0:x1 + 1, z0:z1 + 1].all():
            continue
        if (park.hek_afstand[x0:x1 + 1, z0:z1 + 1] < minhek).any():
            continue
        if not water_ok and park.water[x0:x1 + 1, z0:z1 + 1].any():
            continue
        if (pd[x0:x1 + 1, z0:z1 + 1] < pad_min).any():
            continue
        if pad_max is not None and pd[x, z] > pad_max:
            continue
        hs = park.hoogte[x0:x1 + 1, z0:z1 + 1]
        if hs.max() - hs.min() > helling:
            continue
        if any(math.hypot(x - a, z - b) < weg_r for a, b in weg_van):
            continue
        if not vrij_van_hout(park, x0, z0, x1, z1):
            continue
        return x, z, int(np.median(hs))
    return None


def bord(park, x, y, z, regels, rotation=0, blok="minecraft:oak_sign"):
    """A standing sign. The tile writer does not write block entities (yet), so the text is kept on park.bordjes
    (x, y, z, [regels]) for when it does; the sign block itself is placed already."""
    park.set(x, y, z, blok, rotation=rotation, waterlogged="false")
    if not hasattr(park, "bordjes"):
        park.bordjes = []
    park.bordjes.append({"pos": [int(x), int(y), int(z)], "tekst": list(regels)})


def trap(naam, facing, half="bottom"):
    return kit.state(naam, facing=facing, half=half, shape="straight", waterlogged="false")


def slab(naam, type_="bottom"):
    return kit.state(naam, type=type_, waterlogged="false")


def kijk_richting(dx, dz):
    """Horizontal facing from a direction vector."""
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


TEGENOVER = {"north": "south", "south": "north", "east": "west", "west": "east"}
STAP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


def uitkijktoren(park, x, z, g, kijk_naar, hout="acacia", dak="hay_block", hoogte=8):
    """A 5x5 lookout tower on the ground at y g: four log posts, a ladder up the inside of one post side, a plank deck
    with fence railing at g+hoogte, a stepped roof, and the Guhs verrekijker on the deck facing kijk_naar (x, z)."""
    x, z, g = int(x), int(z), int(g)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            vlak(park, x + dx, z + dz, g, top="coarse_dirt" if max(abs(dx), abs(dz)) == 3 else "dirt_path", vrij=hoogte + 9)
    deck = g + hoogte
    log = kit.state(f"{hout}_log", axis="y")
    for px, pz in ((x - 2, z - 2), (x + 2, z - 2), (x - 2, z + 2), (x + 2, z + 2)):
        for y in range(g + 1, deck + 4):
            park.set(px, y, pz, log)
    # cross bracing half way (planks), deck
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            park.set(x + dx, deck, z + dz, f"minecraft:{hout}_planks")
    # ladder: a log column in the middle of the south side, ladder on its north face (inside)
    for y in range(g + 1, deck):
        park.set(x, y, z + 2, log)
        park.set(x, y, z + 1, "minecraft:ladder", facing="north", waterlogged="false")
    park.v[x, deck, z + 1] = 0                                   # the hatch: an opening in the deck above the ladder
    # railing round the deck (fences auto-connect; spruce = fence family)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if max(abs(dx), abs(dz)) == 2 and park.v[x + dx, deck + 1, z + dz] == 0:
                park.set(x + dx, deck + 1, z + dz, "minecraft:spruce_fence")
    # roof: stepped, 7x7 -> 1
    for lvl in range(4):
        y = deck + 4 + lvl
        a = 3 - lvl
        for dx in range(-a, a + 1):
            for dz in range(-a, a + 1):
                if lvl == 3 or max(abs(dx), abs(dz)) == a:
                    park.set(x + dx, y, z + dz, kit.state(dak, axis="y") if dak.endswith(("hay_block", "_log")) else dak)
    # verrekijker on the deck, on the side towards kijk_naar
    f = kijk_richting(kijk_naar[0] - x, kijk_naar[1] - z)
    sx, sz = STAP[f]
    park.set(x + sx, deck + 1, z + sz, "guhs:park_verrekijker", facing=f)
    return deck


def schuilhut(park, x, z, g, kijk_naar, hout="spruce", dak="hay_block"):
    """A 5x5 hide on the ground at g: plank walls, a door gap on the far side, a row of glass kijkgleuven at eye level
    on the side towards kijk_naar, a bench inside, a hay roof."""
    x, z, g = int(x), int(z), int(g)
    f = kijk_richting(kijk_naar[0] - x, kijk_naar[1] - z)
    achter = TEGENOVER[f]
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            vlak(park, x + dx, z + dz, g, top="coarse_dirt", vrij=9)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            rand = max(abs(dx), abs(dz)) == 2
            park.set(x + dx, g, z + dz, f"minecraft:{hout}_planks")
            if not rand:
                continue
            for y in range(g + 1, g + 4):
                park.set(x + dx, y, z + dz, f"minecraft:{hout}_planks" if (abs(dx), abs(dz)) != (2, 2)
                         else kit.state(f"{hout}_log", axis="y"))
    vx, vz = STAP[f]
    ax, az = STAP[achter]
    # kijkgleuven: the middle three of the front wall at y g+2
    for k in (-1, 0, 1):
        px = x + vx * 2 + (k if vx == 0 else 0)
        pz = z + vz * 2 + (k if vz == 0 else 0)
        park.set(px, g + 2, pz, "minecraft:glass")
    # door gap at the back
    for y in (g + 1, g + 2):
        park.v[x + ax * 2, y, z + az * 2] = 0
    # bench along the front wall inside
    for k in (-1, 0, 1):
        bx = x + vx * 1 + (k if vx == 0 else 0)
        bz = z + vz * 1 + (k if vz == 0 else 0)
        park.set(bx, g + 1, bz, trap(f"minecraft:{hout}_stairs", achter))
    # roof: hay, flat with a ridge
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            park.set(x + dx, g + 4, z + dz, kit.state(dak, axis="y"))
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if max(abs(dx), abs(dz)) <= 1:
                park.set(x + dx, g + 5, z + dz, kit.state(dak, axis="y"))
    return f


def geheimpje(park, x, y, z, facing="north"):
    """The geheimpje spot: a small chest (the game logic comes in a later slice); remembered on park.geheimpjes."""
    park.set(x, y, z, "minecraft:chest", facing=facing, type="single", waterlogged="false")
    if not hasattr(park, "geheimpjes"):
        park.geheimpjes = []
    park.geheimpjes.append([int(x), int(y), int(z)])


def zet_plek(plekken, soort, nr, centrum, straal=None):
    """Move the nr-th home of a species (0-based) to centrum (x, y, z)."""
    huizen = [p for p in plekken if p["soort"] == soort]
    if nr < len(huizen):
        huizen[nr]["centrum"] = [int(c) for c in centrum]
        if straal is not None:
            huizen[nr]["straal"] = int(straal)
        return huizen[nr]
    return None


# ======================================================================================================================
# savanne landmarks
RNG = np.random.default_rng(kit.SEED + 101)
SPOTS = {}          # naam -> (x, y, z), for plekken()


def trotse_rots(park):
    """A big granite rock with a tongue jutting out over the plain (Pride Rock parody); a gentle back ramp so the lions
    walk up; the top plateau is their spot."""
    cx, cz, r, _ = kit.RESERVERINGEN["trotse_rots"]
    pd = pad_afstand(park)
    # the tongue points towards the open plain: the direction (of 8) with the most open savanne and the path farther away
    best = None
    for k in range(8):
        a = k * math.pi / 4
        dx, dz = math.cos(a), math.sin(a)
        tx, tz = int(cx + dx * 22), int(cz + dz * 22)
        if not (0 <= tx < W and 0 <= tz < W) or park.zone[tx, tz] != zi("savanne"):
            continue
        score = pd[tx, tz] + park.hek_afstand[tx, tz] * 0.5
        if best is None or score > best[0]:
            best = (score, dx, dz)
    _, ux, uz = best
    g = int(np.median(park.hoogte[cx - 8:cx + 9, cz - 8:cz + 9]))
    blokken = ("minecraft:granite", "minecraft:granite", "minecraft:stone", "minecraft:andesite", "minecraft:polished_granite")
    top = g + 9
    # body: stacked ellipses, shrinking towards the top, leaning back (away from the tongue)
    for dy in range(0, 10):
        y = g + dy
        rr = 9.0 - dy * 0.45
        ox, oz = -ux * dy * 0.25, -uz * dy * 0.25
        for x in range(cx - 11, cx + 12):
            for z in range(cz - 11, cz + 12):
                d = math.hypot((x - cx - ox) / rr, (z - cz - oz) / (rr * 0.8))
                if d <= 1.0 + RNG.random() * 0.06:
                    park.set(x, y, z, blokken[int(RNG.integers(0, len(blokken)))])
    # the tongue: a slab 4 wide, 2 thick, from the top edge 10 blocks out, rising slightly, open underneath
    for t in range(0, 13):
        bx, bz = cx + ux * (4 + t), cz + uz * (4 + t)
        y = top - 1 + (1 if t > 7 else 0)
        w = 2.2 if t < 10 else 1.4
        for s in np.arange(-w, w + 0.01, 0.5):
            x, z = int(round(bx - uz * s)), int(round(bz + ux * s))
            for yy in (y - 1, y):
                park.set(x, yy, z, "minecraft:granite" if (x + z + yy) % 3 else "minecraft:polished_granite")
    # top plateau: smooth, a bit of dry grass
    for x in range(cx - 7, cx + 8):
        for z in range(cz - 7, cz + 8):
            col = np.nonzero(park.v[x, g:g + 14, z])[0]
            if len(col):
                ty = g + int(col[-1])
                if ty >= top - 2 and RNG.random() < 0.25 and park.v[x, ty + 1, z] == 0:
                    park.set(x, ty + 1, z, "minecraft:short_grass")
    # back ramp: steps of 1 from the ground to the top, 3 wide, on the side opposite the tongue
    bx0, bz0 = cx - ux * 7, cz - uz * 7
    for t in range(0, 14):
        y = g + min(t, top - g)
        mx, mz = bx0 - ux * (10 - t) * 0.9, bz0 - uz * (10 - t) * 0.9
        for s in (-1.5, -1, -0.5, 0, 0.5, 1, 1.5):
            x, z = int(round(mx - uz * s)), int(round(mz + ux * s))
            for yy in range(g, y + 1):
                park.set(x, yy, z, "minecraft:coarse_dirt" if yy == y else "minecraft:stone")
            ruim(park, x, z, y + 1, y + 3)
    # nothing floating: fill every rock column down to the ground
    for x in range(cx - 14, cx + 15):
        for z in range(cz - 14, cz + 15):
            col = np.nonzero(park.v[x, :, z])[0]
            if not len(col):
                continue
            # only the body (not the tongue): columns within the body ellipse
            if math.hypot(x - cx, z - cz) <= 9.5:
                for yy in range(int(park.vloer[x, z]), int(col[-1])):
                    if park.v[x, yy, z] == 0:
                        park.set(x, yy, z, "minecraft:stone")
    SPOTS["trotse_rots_top"] = (cx, top + 1, cz)
    SPOTS["trotse_rots_tong"] = (int(cx + ux * 12), top + 1, int(cz + uz * 12))
    SPOTS["trotse_rots_richting"] = (ux, uz)


def baobab_en_poel(park):
    """The big drinkpoel (surface y 62) and the Vadse Baobab beside it: a fat 5x5 round trunk, short thick branches,
    flat acacia-leaf crowns."""
    cx, cz, r, _ = kit.RESERVERINGEN["vadse_baobab_poel"]
    # poel: west/north part of the spot, baobab east/south
    px, pz = cx - 5, cz - 3
    rx, rz = 9.5, 7.0
    for x in range(px - 12, px + 13):
        for z in range(pz - 10, pz + 11):
            d = math.hypot((x - px) / rx, (z - pz) / rz) + (RNG.random() - 0.5) * 0.08
            if d <= 1.0:
                diepte = 1 + int((1 - d) * 3.2)            # 1..4
                bed = WATER_Y - diepte
                ruim(park, x, z, bed + 1, park.hoogte[x, z] + 3)
                onderbouw(park, x, z, bed)
                park.set(x, bed, z, "minecraft:clay" if diepte > 2 else "minecraft:sand")
                for y in range(bed + 1, WATER_Y + 1):
                    park.set(x, y, z, "minecraft:water", level=0)
                park.hoogte[x, z] = bed
                park.water[x, z] = True
            elif d <= 1.35:
                # the muddy rim, flat at the water level + a few reeds
                vlak(park, x, z, WATER_Y if d < 1.15 else max(WATER_Y, int(park.hoogte[x, z])),
                     top="mud" if d < 1.15 else "coarse_dirt", onder="dirt", vrij=4)
                if RNG.random() < 0.12:
                    park.set(x, park.hoogte[x, z] + 1, z, "minecraft:short_grass")
    # make sure the rim around the water is at least water level (no leaks)
    for x in range(px - 13, px + 14):
        for z in range(pz - 11, pz + 12):
            if not park.water[x, z] and park.hoogte[x, z] < WATER_Y:
                nb = park.water[x - 1:x + 2, z - 1:z + 2].any()
                if nb:
                    vlak(park, x, z, WATER_Y, top="mud", vrij=3)
    SPOTS["poel"] = (px, WATER_Y, pz)
    # baobab
    bx, bz = cx + 8, cz + 6
    g = int(park.hoogte[bx, bz])
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            if math.hypot(dx, dz) <= 4.2:
                vlak(park, bx + dx, bz + dz, g, top="coarse_dirt", vrij=20)
    h = 11
    for y in range(g + 1, g + h + 1):
        bulk = 2.6 if y < g + h - 2 else 2.0                 # fat all the way up (vads!)
        if y <= g + 2:
            bulk = 3.0                                      # flared foot
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if math.hypot(dx, dz) <= bulk:
                    park.set(bx + dx, y, bz + dz, "minecraft:jungle_wood" if math.hypot(dx, dz) > bulk - 1 else "minecraft:stripped_jungle_wood",
                             axis="y")
    top = g + h
    for k in range(6):
        a = k * math.pi / 3 + 0.3
        for t in range(1, 6):
            x = int(round(bx + math.cos(a) * (2 + t)))
            z = int(round(bz + math.sin(a) * (2 + t)))
            y = top + (t // 2)
            park.set(x, y, z, kit.state("jungle_log", axis="x" if abs(math.cos(a)) > abs(math.sin(a)) else "z"))
        ex, ez = int(round(bx + math.cos(a) * 7)), int(round(bz + math.sin(a) * 7))
        park.blob(ex, top + 3, ez, 3.2, 1.3, 3.2, park.leaves("acacia"))
    park.blob(bx, top + 2, bz, 3.5, 1.4, 3.5, park.leaves("acacia"))
    park.bomen.append((bx, top + 4, bz, "baobab"))
    SPOTS["baobab"] = (bx, g + 1, bz)


def termietenheuvels(park):
    cx, cz, r, _ = kit.RESERVERINGEN["termietenheuvels"]
    blokken = ("minecraft:terracotta", "minecraft:orange_terracotta", "minecraft:red_sand", "minecraft:terracotta")
    for i in range(8):
        a = i * 2 * math.pi / 8 + RNG.random() * 0.6
        d = 3 + RNG.random() * (r - 5)
        x0, z0 = int(cx + math.cos(a) * d), int(cz + math.sin(a) * d)
        if park.hek_afstand[x0, z0] < 6 or park.pad[x0, z0]:
            continue
        g = int(park.hoogte[x0, z0])
        h = int(3 + RNG.integers(0, 5))                     # 3..7 high
        r0 = 1.6 + RNG.random() * 0.9
        for dy in range(h):
            rr = r0 * (1 - dy / (h + 0.5)) + 0.35
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    if math.hypot(dx, dz) <= rr:
                        park.set(x0 + dx, g + dy, z0 + dz, blokken[int(RNG.integers(0, 4))])
        # a few spires on top
        park.set(x0, g + h, z0, "minecraft:terracotta")
        if RNG.random() < 0.6:
            park.set(x0 + 1, g + h - 1, z0, "minecraft:orange_terracotta")
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if park.v[x0 + dx, g, z0 + dz]:
                    onderbouw(park, x0 + dx, z0 + dz, g, "minecraft:red_sand")
    SPOTS["termieten"] = (cx, int(park.hoogte[cx, cz]) + 1, cz)


def trekvlakte(park):
    """Tall dry grass all over the plain (biome tint makes it straw yellow), dead bushes; the geheimpje in the grass."""
    cx, cz, r, _ = kit.RESERVERINGEN["trekvlakte"]
    sav = zi("savanne")
    pd = pad_afstand(park)
    rr = r + 14
    kist = None
    for x in range(cx - rr, cx + rr + 1):
        for z in range(cz - rr, cz + rr + 1):
            d = math.hypot(x - cx, z - cz)
            if d > rr or park.zone[x, z] != sav or park.water[x, z] or pd[x, z] < 2 or park.hek_afstand[x, z] < 3:
                continue
            g = int(park.hoogte[x, z])
            boven = park.get(x, g + 1, z)
            if park.v[x, g + 1, z] and not is_plant(boven):
                continue
            if park.get(x, g, z).split("[")[0] not in ("minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:dirt"):
                continue
            kans = 0.75 if d < r else 0.75 * (1 - (d - r) / 14)
            ruim(park, x, z, g + 1, g + 2)
            if RNG.random() < kans:
                park.set(x, g + 1, z, "minecraft:tall_grass", half="lower")
                park.set(x, g + 2, z, "minecraft:tall_grass", half="upper")
            elif RNG.random() < 0.4:
                park.set(x, g + 1, z, "minecraft:short_grass")
            elif RNG.random() < 0.06:
                park.set(x, g + 1, z, "minecraft:dead_bush")
            if kist is None and 5 < d < 8 and park.hek_afstand[x, z] > 8 and pd[x, z] > 10:
                kist = (x, g + 1, z)
    if kist:
        x, y, z = kist
        ruim(park, x, z, y, y + 1)
        geheimpje(park, x, y, z, facing="south")
        # hidden: tall grass all around it
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if (dx or dz) and park.v[x + dx, y, z + dz] == 0:
                    park.set(x + dx, y, z + dz, "minecraft:tall_grass", half="lower")
                    park.set(x + dx, y + 1, z + dz, "minecraft:tall_grass", half="upper")
    SPOTS["trekvlakte"] = (cx, int(park.hoogte[cx, cz]) + 1, cz)


def toren_en_hut(park):
    px, _, pz = SPOTS["poel"]
    tx, tz, _, _ = kit.RESERVERINGEN["trekvlakte"]
    # tower between the poel and the trekvlakte, near the path (visitors walk to it), looking over the poel
    mid = ((px + tx) // 2, (pz + tz) // 2)
    t = zoek(park, "savanne", mid, 30, 3, minhek=8, pad_min=3, pad_max=14, weg_van=[(px, pz)], weg_r=14)
    if t:
        x, z, g = t
        uitkijktoren(park, x, z, g, (px, pz), hout="acacia")
        SPOTS["toren"] = (x, g, z)
    # hide at the poel: 12-16 from the poel centre, facing it
    h = zoek(park, "savanne", (px + 12, pz + 8), 14, 3, minhek=6, pad_min=3, weg_van=[(px, pz)], weg_r=12)
    if h:
        x, z, g = h
        schuilhut(park, x, z, g, (px, pz), hout="spruce")
        SPOTS["hut"] = (x, g, z)


def brug_halte(park):
    """The truck stop at the bridge over the river between savanne and delta: a covered 6x6 viewing platform beside the
    bridge deck at deck height, posts down to the river bed, a hay roof, a bench, a yellow 'Halte' board."""
    from scipy.ndimage import label
    deck = park.pad & park.water
    lab, n = label(deck)
    sav = zi("savanne")
    beste = None
    for i in range(1, n + 1):
        xs, zs = np.nonzero(lab == i)
        if len(xs) < 30:
            continue
        cx, cz = int(xs.mean()), int(zs.mean())
        # the savanne/delta crossing: the bridge whose surroundings are savanne on one side and delta/regenwoud on the other
        omg = park.zone[max(cx - 25, 0):cx + 26, max(cz - 25, 0):cz + 26]
        if (omg == sav).mean() < 0.2 or not ((omg == zi("delta")).any() or (omg == zi("regenwoud")).any()):
            continue
        if beste is None or len(xs) > beste[0]:
            beste = (len(xs), xs, zs)
    if beste is None:
        return
    _, xs, zs = beste
    cx, cz = xs.mean(), zs.mean()
    # bridge direction (principal axis) and its normal
    c = np.cov(np.vstack([xs - cx, zs - cz]))
    w, v = np.linalg.eigh(c)
    ax, az = v[:, 1]
    nx, nz = -az, ax
    DECK = WATER_Y + 1
    # try both sides; take the side with more water (over the river) and no path
    for zijde in (1, -1):
        ox, oz = cx + nx * zijde * 5.5, cz + nz * zijde * 5.5
        x0, z0 = int(round(ox)) - 3, int(round(oz)) - 3
        cells = [(x, z) for x in range(x0, x0 + 6) for z in range(z0, z0 + 6)]
        if any(park.pad[x, z] for x, z in cells):
            continue
        if sum(park.water[x, z] for x, z in cells) < 18:
            continue
        break
    else:
        return
    for x, z in cells:
        ruim(park, x, z, DECK, DECK + 9)
        park.set(x, DECK, z, "minecraft:spruce_planks")
    for x, z in cells:
        edge = x in (x0, x0 + 5) or z in (z0, z0 + 5)
        if edge:
            park.set(x, DECK + 1, z, "minecraft:spruce_fence")
    for px, pz in ((x0, z0), (x0 + 5, z0), (x0, z0 + 5), (x0 + 5, z0 + 5)):
        for y in range(int(park.hoogte[px, pz]) + 1, DECK + 4):
            park.set(px, y, pz, kit.state("stripped_spruce_log", axis="y"))
    # connect to the bridge: clear the railing between deck and platform (cells of the bridge rail next to the platform)
    for x in range(x0 - 2, x0 + 8):
        for z in range(z0 - 2, z0 + 8):
            if park.get(x, DECK + 1, z).startswith("minecraft:spruce_fence") and not (x0 <= x < x0 + 6 and z0 <= z < z0 + 6):
                # a rail cell between the deck and the platform: open the 4 nearest to the platform middle
                if math.hypot(x - (x0 + 2.5), z - (z0 + 2.5)) < 4.2:
                    park.v[x, DECK + 1, z] = 0
                    park.set(x, DECK, z, "minecraft:spruce_planks")
    # the opening in the platform's own railing on the bridge side
    for x, z in cells:
        if x in (x0, x0 + 5) or z in (z0, z0 + 5):
            near = any(park.pad[x + dx, z + dz] for dx in (-2, -1, 0, 1, 2) for dz in (-2, -1, 0, 1, 2))
            if near and (x, z) not in ((x0, z0), (x0 + 5, z0), (x0, z0 + 5), (x0 + 5, z0 + 5)):
                park.v[x, DECK + 1, z] = 0
    # roof
    for lvl in range(4):
        y = DECK + 4 + lvl
        a, b = x0 - 1 + lvl, x0 + 6 - lvl
        c_, d = z0 - 1 + lvl, z0 + 6 - lvl
        if a > b or c_ > d:
            break
        for x in range(a, b + 1):
            for z in range(c_, d + 1):
                if lvl == 3 or x in (a, b) or z in (c_, d):
                    park.set(x, y, z, kit.state("hay_block", axis="y"))
    # bench on the far side, facing the bridge
    binnen = [(x, z) for x, z in cells if not (x in (x0, x0 + 5) or z in (z0, z0 + 5))]
    proj = {c: (c[0] - cx) * nx * zijde + (c[1] - cz) * nz * zijde for c in binnen}
    verst = max(proj.values())
    f = kijk_richting(-nx * zijde, -nz * zijde)          # the bench faces the bridge
    for c, pr in proj.items():
        if pr > verst - 0.75:
            park.set(c[0], DECK + 1, c[1], trap("minecraft:spruce_stairs", TEGENOVER[f]))
    # 'Halte' board beside the path at the savanne bridgehead: two fence posts, a yellow board, the sign on top
    pd = pad_afstand(park)
    kand = [(math.hypot(x - cx, z - cz), x, z) for x in range(int(cx) - 30, int(cx) + 31)
            for z in range(int(cz) - 30, int(cz) + 31)
            if 0 <= x < W and 0 <= z < W and park.zone[x, z] == sav and not park.water[x, z] and 1.5 < pd[x, z] < 2.5
            and park.hek_afstand[x, z] > 2 and not park.water[max(x - 1, 0):x + 2, max(z - 1, 0):z + 2].any()]
    if kand:
        _, hx, hz = min(kand)
        g = int(park.hoogte[hx, hz])
        # the board runs along the path: the path direction here is the bridge axis
        lx, lz = (1, 0) if abs(ax) >= abs(az) else (0, 1)
        for k in (0, 1):
            x, z = hx + lx * k, hz + lz * k
            vlak(park, x, z, g, top="coarse_dirt", vrij=5)
            park.set(x, g + 1, z, "minecraft:spruce_fence")
            park.set(x, g + 2, z, "minecraft:spruce_fence")
            park.set(x, g + 3, z, "minecraft:yellow_concrete")
        # face the sign towards the path (the nearest path cell)
        px_, pz_ = min(((x, z) for x in range(hx - 3, hx + 4) for z in range(hz - 3, hz + 4) if park.pad[x, z]),
                       key=lambda c: math.hypot(c[0] - hx, c[1] - hz))
        f = kijk_richting(px_ - hx, pz_ - hz)
        bord(park, hx, g + 4, hz, ["", "Halte", "Brug Savanne", ""],
             rotation={"south": 0, "west": 4, "north": 8, "east": 12}[f])
    SPOTS["halte"] = (x0 + 2, DECK + 1, z0 + 2)


def bouw(park, log=print):
    trotse_rots(park)
    baobab_en_poel(park)
    termietenheuvels(park)
    trekvlakte(park)
    toren_en_hut(park)
    brug_halte(park)
    park._pad_d = None
    log(f"    savanne: {', '.join(sorted(SPOTS))}")


def plekken(park, plekken):
    """Leeuwen on the Trotse Rots, one giraffe and one zebra home at the drinkpoel (the olifanten are anchored there)."""
    if "trotse_rots_top" in SPOTS:
        for i in range(2):
            zet_plek(plekken, "leeuw", i, SPOTS["trotse_rots_top"], straal=12)
    if "poel" in SPOTS:
        px, _, pz = SPOTS["poel"]
        for soort in ("giraffe", "zebra"):
            # the nearest dry land 12 east of the poel
            for d in range(12, 24):
                x, z = px + d, pz
                if not park.water[x, z]:
                    zet_plek(plekken, soort, 0, (x, int(park.hoogte[x, z]) + 1, z), straal=16)
                    break
            pz += 6
        for i in range(2):
            p = zet_plek(plekken, "olifant", i, (px + 13, WATER_Y + 1, SPOTS["poel"][2] - 2), straal=16)
            if p:
                x, _, z = p["centrum"]
                p["centrum"][1] = int(park.hoogte[x, z]) + 1
