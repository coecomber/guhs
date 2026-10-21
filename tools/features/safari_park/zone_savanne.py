"""
Zone module savanne (design 1.4): de Trotse Rots, de Vadse Baobab bij de grote drinkpoel, termietenheuvels, de trekvlakte
met hoog droog gras (met het geheimpje), een acacia-uitkijktoren met verrekijker, een schuilhut met kijkgleuven aan de poel
en de truckhalte met overdekt uitkijkplatform bij de brug naar de delta (zoals de knooppunt-render).

Also the shared little helpers of the zone modules (zone_delta imports them): finding a free spot, levelling a column,
a lookout tower, a hide, a sign with text (kept on park.bordjes for the tile writer, see _bord).
Escape-proof rule (design 4.5c): anything that stands more than 1 block above the ground keeps >= 4 blocks from a fence.

Round 2 (user test): the drinkpoel is bigger with gently sloping banks and at most 2 deep (the elephants wade, they do
not vanish under water); the hide is the new standard kijkhut (a raised hut with a wide band of Eenrichtingsvadsglas,
placed where its window sees the animals; see kijkhut / kies_kijkhut / zicht); the route bridges are raised to
BRUG_DEK (3 free blocks over the water for the canoe route, see bruggen_ophogen).
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
    kit.toren_kijkplek(park, x, z, deck)
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
        kit.stoel(park, bx, g + 1, bz, TEGENOVER[achter])          # guh chairs looking out of the slits
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
# round 2 (user feedback): sightlines, the standard kijkhut, raised bridges (also used by zone_delta; other zones may copy)
_DOORZICHTIG = ("minecraft:air", "minecraft:cave_air", "minecraft:water", "minecraft:glass", "guhs:eenrichtingsglas",
                "minecraft:glass_pane", "minecraft:light", "minecraft:lantern", "minecraft:lily_pad", "minecraft:seagrass",
                "minecraft:tall_seagrass", "minecraft:kelp", "minecraft:kelp_plant")
GLAS = "guhs:eenrichtingsglas"          # Eenrichtingsvadsglas: the standard viewing window of every kijkhut


def opaak_lut(park):
    """bool per palette id: True when the block blocks the view (air, water, glass, plants, corals do not)."""
    lut = np.ones(len(park.pal) + 1, bool)
    for i, s in enumerate(park.pal):
        n = s.split("[")[0]
        if n in _DOORZICHTIG or is_plant(s) or n.endswith(("_glass", "_glass_pane", "_carpet", "_coral", "_coral_fan")):
            lut[i] = False
    return lut


def zicht(park, oog, doelen, lut=None, stap=0.25):
    """Which targets can be seen from oog (x, y, z as floats, block coordinates): the straight line passes only
    see-through blocks. Returns (fraction visible, [bool per target])."""
    if lut is None:
        lut = opaak_lut(park)
    oog = np.asarray(oog, float)
    zie = []
    for d in doelen:
        d = np.asarray(d, float)
        n = max(int(np.linalg.norm(d - oog) / stap), 2)
        ts = np.linspace(0.0, 1.0, n)[1:-1]
        p = np.floor(oog[None] + (d - oog)[None] * ts[:, None]).astype(int)
        ok = (p[:, 0] >= 0) & (p[:, 0] < W) & (p[:, 1] >= 0) & (p[:, 1] < kit.H) & (p[:, 2] >= 0) & (p[:, 2] < W)
        p = p[ok]
        zie.append(not lut[park.v[p[:, 0], p[:, 1], p[:, 2]]].any())
    return (sum(zie) / len(zie) if zie else 0.0), zie


def _hut_raster(x, z, f, diep):
    """local (a across, b from the front wall backwards; b < 0 is in front of the hut) -> world (x, z)."""
    vx, vz = STAP[f]
    sx, sz = (1, 0) if vx == 0 else (0, 1)
    bc = (diep - 1) // 2
    return lambda a, b: (x + sx * a - vx * (b - bc), z + sz * a - vz * (b - bc))


def _hut_cellen(x, z, f, breed, diep, verhoogd):
    """All (x, z) the hut, its levelled margin and its entrance steps use."""
    w = _hut_raster(x, z, f, diep)
    hw = breed // 2
    return [w(a, b) for a in range(-hw - 1, hw + 2) for b in range(-1, diep + verhoogd + 1)]


def oog_van(x, z, g, f, diep=5, verhoogd=2):
    """The eye of a standing player at the window of a kijkhut (front row, beside the verrekijker)."""
    w = _hut_raster(x, z, f, diep)
    ex, ez = w(1, 1)
    return (ex + 0.5, g + verhoogd + 2.62, ez + 0.5)


def kies_kijkhut(park, zone, doelen, dmin, dmax, breed=7, diep=5, verhoogd=2, minhek=6, weg_van=(), weg_r=0,
                 ideaal=None, max_hoek=28):
    """The best spot (x, z, g, facing) for a kijkhut looking at doelen (list of (x, y, z) points where the animals
    are): on a ring dmin..dmax round the targets' middle, facing them (the axis within max_hoek degrees), the footprint
    dry, flat, free of trees and paths, in the zone, minhek from fences; scored on how many targets the window sees."""
    zone_i = zi(zone) if isinstance(zone, str) else zone
    tx = sum(d[0] for d in doelen) / len(doelen)
    tz = sum(d[2] for d in doelen) / len(doelen)
    pd = pad_afstand(park)
    lut = opaak_lut(park)
    ideaal = ideaal or (dmin + dmax) / 2
    best = None
    for r in range(int(dmin), int(dmax) + 1, 2):
        for k in range(36):
            a = 2 * math.pi * k / 36
            x, z = int(round(tx + math.cos(a) * r)), int(round(tz + math.sin(a) * r))
            f = kijk_richting(tx - x, tz - z)
            vx, vz = STAP[f]
            hoek = math.degrees(math.acos(max(-1.0, min(1.0, ((tx - x) * vx + (tz - z) * vz) / (math.hypot(tx - x, tz - z) or 1)))))
            if hoek > max_hoek:
                continue
            cellen = _hut_cellen(x, z, f, breed, diep, verhoogd)
            if any(not (2 <= cx_ < W - 2 and 2 <= cz_ < W - 2) for cx_, cz_ in cellen):
                continue
            xs = np.array([c[0] for c in cellen])
            zs = np.array([c[1] for c in cellen])
            if (park.zone[xs, zs] != zone_i).any() or not park.binnen[xs, zs].all():
                continue
            if (park.hek_afstand[xs, zs] < minhek).any() or park.water[xs, zs].any() or (pd[xs, zs] < 2).any():
                continue
            hs = park.hoogte[xs, zs]
            if hs.max() - hs.min() > 2:
                continue
            if any(math.hypot(x - p, z - q) < weg_r for p, q in weg_van):
                continue
            if not vrij_van_hout(park, int(xs.min()), int(zs.min()), int(xs.max()), int(zs.max()), hoog=10):
                continue
            g = int(np.median(hs))
            frac, _ = zicht(park, oog_van(x, z, g, f, diep, verhoogd), doelen, lut)
            score = frac * 10 - hoek * 0.03 - abs(r - ideaal) * 0.08 - min(pd[x, z], 30) * 0.03
            if best is None or score > best[0]:
                best = (score, x, z, g, f, frac)
    if best is None:
        return None
    return best[1:]


def kijkhut(park, x, z, g, kijk_naar=None, richting=None, hout="spruce", dak="hay_block", basis="minecraft:mud_bricks",
            breed=7, diep=5, verhoogd=2, titel=("Kijkhut", "", "Stil zijn,", "dan zie je meer!"), plantje=None):
    """The standard kijkhut (round 2): a raised hut (floor `verhoogd` above the ground at y g) whose front and the front
    halves of its sides are a band of Eenrichtingsvadsglas two high above a plank sill, so you look out wide at eye
    level (the animals do not see you, njeg). Inside: the verrekijker in the middle of the window, a bench behind it, a
    lantern, a sign with titel on the back wall; steps up to a door at the back. Faces `richting` or kijk_naar (x, z).
    The view in front is cleared of plants and low leaves. Returns dict(f, F, oog, voor) (voor = the cell in front)."""
    x, z, g = int(x), int(z), int(g)
    f = richting or kijk_richting(kijk_naar[0] - x, kijk_naar[1] - z)
    achter = TEGENOVER[f]
    w = _hut_raster(x, z, f, diep)
    hw = breed // 2
    F = g + verhoogd
    log = kit.state(f"{hout}_log", axis="y")
    planks = f"minecraft:{hout}_planks"
    # site: level the footprint, a margin and the entrance
    for a in range(-hw - 1, hw + 2):
        for b in range(-1, diep + verhoogd + 1):
            X, Z = w(a, b)
            vlak(park, X, Z, g, top="coarse_dirt" if b < diep else "dirt_path", vrij=verhoogd + 9)
    for a in range(-hw, hw + 1):
        for b in range(0, diep):
            X, Z = w(a, b)
            rand = abs(a) == hw or b in (0, diep - 1)
            for y in range(g + 1, F):
                park.set(X, y, Z, log if (abs(a) == hw and b in (0, diep - 1)) else (planks if rand else basis))
            park.set(X, F, Z, planks)
            if not rand:
                continue
            for y in range(F + 1, F + 4):
                hoek = abs(a) == hw and b in (0, diep - 1)
                if hoek:
                    blok = log
                elif b == 0 or (abs(a) == hw and b <= 1):            # the window band: front + front halves of the sides
                    blok = planks if y == F + 1 else GLAS
                elif b == diep - 1 and a == 0 and y <= F + 2:          # the door at the back
                    blok = None
                else:
                    blok = planks
                if blok is None:
                    park.v[X, y, Z] = 0
                else:
                    park.set(X, y, Z, blok)
    # roof: hay with an overhang, a second layer on top
    for a in range(-hw - 1, hw + 2):
        for b in range(-1, diep + 1):
            X, Z = w(a, b)
            park.set(X, F + 4, Z, kit.state(dak, axis="y") if dak.endswith(("hay_block", "_log")) else dak)
            if -hw < a < hw and 0 < b < diep - 1:
                park.set(X, F + 5, Z, kit.state(dak, axis="y") if dak.endswith(("hay_block", "_log")) else dak)
    # inside: verrekijker in the middle of the window row, bench behind it, lantern, sign, a plant
    X, Z = w(0, 1)
    park.set(X, F + 1, Z, "guhs:park_verrekijker", facing=f)
    for a in range(-hw + 1, hw):
        if a != 0:
            X, Z = w(a, 2)
            kit.stoel(park, X, F + 1, Z, TEGENOVER[achter])         # guh chairs looking through the glass
    X, Z = w(0, 2)
    park.set(X, F + 3, Z, "minecraft:lantern", hanging="true", waterlogged="false")
    X, Z = w(hw - 1, diep - 2)
    park.set(X, F + 2, Z, f"minecraft:{hout}_wall_sign", facing=f, waterlogged="false")
    if not hasattr(park, "bordjes"):
        park.bordjes = []
    park.bordjes.append({"pos": [int(X), int(F + 2), int(Z)], "tekst": list(titel)})
    if plantje:
        X, Z = w(-hw + 1, diep - 2)
        park.set(X, F + 1, Z, plantje)
    # entrance: a landing at floor height behind the door, then steps down to the ground
    for a in (-1, 0, 1):
        X, Z = w(a, diep)
        for y in range(g + 1, F):
            park.set(X, y, Z, basis)
        park.set(X, F, Z, planks)
        ruim(park, X, Z, F + 1, F + 4)
        for k in range(verhoogd):
            X, Z = w(a, diep + 1 + k)
            y = F - 1 - k
            for yy in range(g + 1, y):
                park.set(X, yy, Z, basis)
            park.set(X, y, Z, trap(f"minecraft:{hout}_stairs", f))
            ruim(park, X, Z, y + 1, y + 4)
    # the view: no plants / low leaves in a widening cone in front of the window (fences and trunks stay)
    for dist in range(1, 26):
        half = int(1 + dist * 0.75)
        for a in range(-hw - half, hw + half + 1):
            X, Z = w(a, -dist)
            if not (0 <= X < W and 0 <= Z < W) or park.hek[X, Z] or not park.binnen[X, Z]:
                continue
            gx = int(park.hoogte[X, Z])
            for y in range(gx + 1, min(F + 6, kit.H)):
                s = park.get(X, y, Z)
                if is_plant(s) or ("_leaves" in s and y <= F + 4):
                    park.v[X, y, Z] = 0
                    if s.endswith("half=lower]") or "half=lower" in s:
                        park.v[X, y + 1, Z] = 0
    return {"f": f, "F": F, "oog": oog_van(x, z, g, f, diep, verhoogd), "voor": w(0, -1)}


def paadje(park, van, max_r=30, blok="dirt_path"):
    """A trodden little path from van (x, z) to the nearest route path cell (stops at water and fences)."""
    x0, z0 = int(van[0]), int(van[1])
    pxs, pzs = np.nonzero(park.pad[max(x0 - max_r, 0):x0 + max_r + 1, max(z0 - max_r, 0):z0 + max_r + 1])
    if not len(pxs):
        return
    ox, oz = max(x0 - max_r, 0), max(z0 - max_r, 0)
    i = int(np.argmin(np.hypot(pxs + ox - x0, pzs + oz - z0)))
    tx, tz = int(pxs[i] + ox), int(pzs[i] + oz)
    n = int(math.hypot(tx - x0, tz - z0)) + 1
    for k in range(n + 1):
        x = int(round(x0 + (tx - x0) * k / n))
        z = int(round(z0 + (tz - z0) * k / n))
        if park.pad[x, z] or park.hek[x, z] or park.water[x, z]:
            break
        g = int(park.hoogte[x, z])
        if park.get(x, g, z).split("[")[0] in ("minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:dirt",
                                                "minecraft:sand", "minecraft:mud", "minecraft:podzol", "minecraft:red_sand"):
            park.set(x, g, z, blok)
            s = park.get(x, g + 1, z)
            if is_plant(s):
                park.v[x, g + 1, z] = 0
                if "half=lower" in s:
                    park.v[x, g + 2, z] = 0


def los_blad_weg(park, x0, x1, z0, z1):
    """Leaves with no log/wood within 3 blocks (crowns cut off by digging) go."""
    from scipy.ndimage import binary_dilation
    x0, z0 = max(x0, 0), max(z0, 0)
    x1, z1 = min(x1, W), min(z1, W)
    sub = park.v[x0:x1, :, z0:z1]
    blad = np.zeros(len(park.pal) + 1, bool)
    hout = np.zeros(len(park.pal) + 1, bool)
    for i, s in enumerate(park.pal):
        n = s.split("[")[0]
        blad[i] = n.endswith("_leaves")
        hout[i] = n.endswith(("_log", "_wood")) or n == "minecraft:mangrove_roots"
    houtm = binary_dilation(hout[sub], structure=np.ones((3, 3, 3), bool), iterations=3)
    weg = blad[sub] & ~houtm
    sub[weg] = 0


def drijvend_hout_weg(park, zones, log=print):
    """Trees cut off by digging (a crown or branch with nothing under it) go: every connected group of log/wood/leaf/
    vine blocks that mostly lies in `zones` and stands on nothing solid is removed."""
    from scipy.ndimage import label, maximum
    zset = [zi(n) for n in zones]
    zmask = np.isin(park.zone, zset)
    xs, zs = np.nonzero(zmask)
    if not len(xs):
        return 0
    x0, x1 = max(int(xs.min()) - 6, 0), min(int(xs.max()) + 7, W)
    z0, z1 = max(int(zs.min()) - 6, 0), min(int(zs.max()) + 7, W)
    y0 = max(WATER_Y - 12, 1)
    sub = park.v[x0:x1, y0:, z0:z1]
    hout = np.zeros(len(park.pal) + 1, bool)
    vast = np.zeros(len(park.pal) + 1, bool)
    for i, st in enumerate(park.pal):
        n = st.split("[")[0]
        hout[i] = n.endswith(("_log", "_wood", "_leaves")) or n in ("minecraft:vine", "minecraft:mangrove_roots",
                                                                    "minecraft:muddy_mangrove_roots")
        vast[i] = i != 0 and not hout[i] and n != "minecraft:water" and not is_plant(st)
    h = hout[sub]
    onder = np.zeros_like(h)
    onder[:, 1:, :] = vast[sub[:, :-1, :]]
    onder[:, 0, :] = vast[park.v[x0:x1, y0 - 1, z0:z1]]
    # a group is held when one of its blocks stands on something solid (a trunk on the ground, a post on the bed)
    steun = h & onder
    lab, n = label(h, structure=np.ones((3, 3, 3), bool))
    if n == 0:
        return 0
    gesteund = maximum(steun, lab, index=np.arange(1, n + 1)).astype(bool)
    zm = zmask[x0:x1, z0:z1][:, None, :] & np.ones_like(h)
    in_zone = maximum(zm & h, lab, index=np.arange(1, n + 1)).astype(bool)
    weg = np.zeros(n + 1, bool)
    weg[1:] = ~gesteund & in_zone
    m = weg[lab]
    sub[m] = 0
    log(f"    los hout weg ({', '.join(zones)}): {int(weg.sum())} groepjes, {int(m.sum())} blokken")
    return int(weg.sum())


BRUG_DEK = kit.BRUG_Y          # the deck block of a bridge (round 3: the one bridge builder's height): y 63..65 stay free above the water (kano route)


def bruggen_ophogen(park, zones, log=print):
    """Raise the route bridges whose water cells lie mostly in `zones`: the deck goes up to BRUG_DEK in the middle
    (3 free blocks above the water for the canoe route), with half-block steps (spruce slabs) up from both banks, the
    railing on top, no pillars in the river (posts only at the banks). Idempotent: a bridge already at that height
    (or higher) is left alone."""
    from scipy.ndimage import label, binary_dilation
    zset = {zi(n) for n in zones}
    lab, n = label(park.pad & park.water)
    surf_top = BRUG_DEK + 1
    plaat = kit.state("spruce_slab", type="bottom", waterlogged="false")
    planks = "minecraft:spruce_planks"
    gedaan = 0
    for i in range(1, n + 1):
        xs, zs = np.nonzero(lab == i)
        if len(xs) < 4:
            continue
        zv = park.zone[xs, zs]
        zv = zv[zv >= 0]
        if not len(zv) or int(np.bincount(zv).argmax()) not in zset:
            continue
        hoog = 0
        for x, z in zip(xs, zs):
            nz = np.nonzero(park.v[x, WATER_Y + 1:WATER_Y + 10, z])[0]
            if len(nz):
                hoog = max(hoog, WATER_Y + 1 + int(nz[-1]))
        if hoog >= BRUG_DEK:
            continue
        x0, z0 = max(int(xs.min()) - 6, 0), max(int(zs.min()) - 6, 0)
        x1, z1 = min(int(xs.max()) + 7, W), min(int(zs.max()) + 7, W)
        cel = lab[x0:x1, z0:z1] == i
        rand = binary_dilation(cel) & ~park.pad[x0:x1, z0:z1] & park.water[x0:x1, z0:z1]
        brug = [(int(x), int(z)) for x, z in zip(xs, zs)]
        rnd = [(x0 + int(a), z0 + int(b)) for a, b in zip(*np.nonzero(rand))]
        # away with the old low deck, its railing and the pillars in the river
        for x, z in brug + rnd:
            for y in range(WATER_Y + 1, WATER_Y + 4):
                park.v[x, y, z] = 0
            for y in range(int(park.hoogte[x, z]) + 1, WATER_Y + 1):
                park.set(x, y, z, "minecraft:water", level=0)
        cx, cz = xs.mean(), zs.mean()
        c = np.cov(np.vstack([xs - cx, zs - cz]))
        _, v = np.linalg.eigh(c)
        ax, az = v[:, 1]
        tt = lambda x, z: (x - cx) * ax + (z - cz) * az
        ts = np.array([tt(x, z) for x, z in brug])
        tmin, tmax = float(ts.min()), float(ts.max())
        # the walking surface of the path on both banks
        land = park.pad[x0:x1, z0:z1] & ~park.water[x0:x1, z0:z1] & binary_dilation(cel, iterations=3)
        sa, sb = [], []
        for a, b in zip(*np.nonzero(land)):
            x, z = x0 + int(a), z0 + int(b)
            t = tt(x, z)
            (sa if t < (tmin + tmax) / 2 else sb).append(int(park.hoogte[x, z]) + 1)
        sA = max(sa) if sa else WATER_Y + 2
        sB = max(sb) if sb else WATER_Y + 2

        # the ramps lie on the land approaches (half a block up per block), so the deck is at full height over the
        # whole river (the canoe passes anywhere under it)
        LA = int(math.ceil(2 * max(0.0, surf_top - sA)))
        LB = int(math.ceil(2 * max(0.0, surf_top - sB)))

        def surf(t):
            ua = sA + 0.5 * (1 + math.floor(t - (tmin - LA)))
            ub = sB + 0.5 * (1 + math.floor((tmax + LB) - t))
            return min(float(surf_top), ua, ub)

        # the approaches: path cells on land in line with the bridge, up to LA / LB beyond its ends
        bx0, bz0 = max(x0 - max(LA, LB) - 4, 0), max(z0 - max(LA, LB) - 4, 0)
        bx1, bz1 = min(x1 + max(LA, LB) + 4, W), min(z1 + max(LA, LB) + 4, W)
        for a_, b_ in zip(*np.nonzero(park.pad[bx0:bx1, bz0:bz1] & ~park.water[bx0:bx1, bz0:bz1])):
            x, z = bx0 + int(a_), bz0 + int(b_)
            t = tt(x, z)
            dwars = abs(-(x - cx) * az + (z - cz) * ax)
            if dwars > 3.5 or not ((tmin - LA - 0.5 <= t < tmin) or (tmax < t <= tmax + LB + 0.5)):
                continue
            g = int(park.hoogte[x, z])
            sn = surf(t)
            if sn <= g + 1:
                continue
            k = math.floor(sn)
            top_y = k - 1 if sn == k else k
            for y in range(g + 1, top_y):
                park.set(x, y, z, "minecraft:dirt")
            park.set(x, top_y, z, planks if sn == k else plaat)
            if sn != k:
                park.set(x, top_y - 1, z, "minecraft:dirt")
            ruim(park, x, z, top_y + 1, top_y + 4)
            park.hoogte[x, z] = top_y

        bodem = {}
        for (x, z) in brug + rnd:
            s = surf(tt(x, z))
            k = math.floor(s)
            if (x, z) in rnd:
                dy = math.ceil(s) - 1
                park.set(x, dy, z, planks)
                park.set(x, dy + 1, z, "minecraft:spruce_fence")
            elif s == k:
                dy = k - 1
                park.set(x, dy, z, planks)
            else:
                dy = k
                park.set(x, dy, z, plaat)
            ruim(park, x, z, max(dy + 1, WATER_Y + 1) + (1 if (x, z) in rnd else 0), dy + 4)
            bodem[(x, z)] = dy
        # posts only at the banks: deck cells with dry land next to them
        for (x, z), dy in bodem.items():
            if any(not park.water[x + dx, z + dz] and not park.pad[x + dx, z + dz]
                   for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                for y in range(int(park.hoogte[x, z]) + 1, dy):
                    park.set(x, y, z, kit.state("stripped_spruce_log", axis="y"))
        gedaan += 1
    log(f"    bruggen opgehoogd ({', '.join(zones)}): {gedaan}")
    return gedaan


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
    """The big drinkpoel (surface y 62, at most 2 deep, a wide 1-deep rim where the elephants wade) with gently sloping
    banks up to the plain, and the Vadse Baobab beside it: a fat round trunk, short thick branches, flat acacia crowns."""
    cx, cz, r, _ = kit.RESERVERINGEN["vadse_baobab_poel"]
    sav = zi("savanne")
    pd = pad_afstand(park)
    px, pz = cx - 6, cz - 2
    rx, rz = 14.0, 10.5
    ok = lambda x, z: (park.zone[x, z] == sav and park.binnen[x, z] and park.hek_afstand[x, z] >= 6 and pd[x, z] >= 3)
    X0, X1 = px - int(rx * 1.8) - 2, px + int(rx * 1.8) + 3
    Z0, Z1 = pz - int(rz * 1.8) - 2, pz + int(rz * 1.8) + 3
    ruis = {}
    for x in range(X0, X1):
        for z in range(Z0, Z1):
            ruis[x, z] = (RNG.random() - 0.5) * 0.08
    # trees over the poel and its banks go (trunk and crown); loose leaves of trees outside are cleaned up below
    for x in range(X0, X1):
        for z in range(Z0, Z1):
            if not ok(x, z) or math.hypot((x - px) / rx, (z - pz) / rz) > 1.75:
                continue
            g = int(park.hoogte[x, z])
            for y in range(g + 1, min(g + 30, kit.H)):
                if park.v[x, y, z] and (is_hout(park.get(x, y, z)) or is_plant(park.get(x, y, z))):
                    park.v[x, y, z] = 0
    for x in range(X0, X1):
        for z in range(Z0, Z1):
            if not ok(x, z):
                continue
            d = math.hypot((x - px) / rx, (z - pz) / rz) + ruis[x, z]
            if d <= 1.0:
                diepte = 2 if d < 0.45 else 1
                bed = WATER_Y - diepte
                ruim(park, x, z, bed + 1, park.hoogte[x, z] + 3)
                onderbouw(park, x, z, bed)
                park.set(x, bed, z, "minecraft:clay" if diepte == 2 else "minecraft:sand")
                for y in range(bed + 1, WATER_Y + 1):
                    park.set(x, y, z, "minecraft:water", level=0)
                park.hoogte[x, z] = bed
                park.water[x, z] = True
                if diepte == 1 and RNG.random() < 0.03:
                    park.set(x, WATER_Y + 1, z, "minecraft:lily_pad")
            elif d <= 1.7:
                g0 = int(park.hoogte[x, z])
                t = (d - 1.0) / 0.7
                y = WATER_Y + int(round(t * max(0, g0 - WATER_Y)))
                y = max(WATER_Y, min(y, g0))
                top = "mud" if d < 1.12 else ("coarse_dirt" if d < 1.3 else "grass_block")
                vlak(park, x, z, y, top=top, onder="dirt", vrij=5)
                if d > 1.12 and RNG.random() < 0.10:
                    park.set(x, park.hoogte[x, z] + 1, z, "minecraft:short_grass")
    # the rim round the water is never lower than the water (no leaks)
    for x in range(X0, X1):
        for z in range(Z0, Z1):
            if not park.water[x, z] and park.hoogte[x, z] < WATER_Y and park.water[x - 1:x + 2, z - 1:z + 2].any():
                vlak(park, x, z, WATER_Y, top="mud", vrij=3)
    los_blad_weg(park, X0 - 4, X1 + 4, Z0 - 4, Z1 + 4)
    SPOTS["poel"] = (px, WATER_Y, pz)
    SPOTS["poel_r"] = (rx, rz)
    # baobab: dry flat land east of the poel, clear of the water and the path
    bx, bz = px + int(rx) + 9, pz + 5
    for rr in range(0, 14):
        kand = [(x, z) for x in range(bx - rr, bx + rr + 1) for z in range(bz - rr, bz + rr + 1)
                if max(abs(x - bx), abs(z - bz)) == rr and ok(x, z) and pd[x, z] >= 6 and park.hek_afstand[x, z] >= 8
                and not park.water[x - 7:x + 8, z - 7:z + 8].any()]
        if kand:
            bx, bz = kand[0]
            break
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
        a_ = k * math.pi / 3 + 0.3
        for t in range(1, 6):
            x = int(round(bx + math.cos(a_) * (2 + t)))
            z = int(round(bz + math.sin(a_) * (2 + t)))
            y = top + (t // 2)
            park.set(x, y, z, kit.state("jungle_log", axis="x" if abs(math.cos(a_)) > abs(math.sin(a_)) else "z"))
        ex, ez = int(round(bx + math.cos(a_) * 7)), int(round(bz + math.sin(a_) * 7))
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


def _rand_punten(park, px, pz, rx, rz, d=1.08, n=24, water_ok=False):
    """Points on the rim of the poel (d times its radii), at animal head height (dry ones unless water_ok)."""
    uit = []
    for k in range(n):
        a = 2 * math.pi * k / n
        x, z = int(round(px + math.cos(a) * rx * d)), int(round(pz + math.sin(a) * rz * d))
        if 0 <= x < W and 0 <= z < W and park.zone[x, z] == zi("savanne") and (water_ok or not park.water[x, z]):
            uit.append((x + 0.5, park.hoogte[x, z] + 1.6, z + 0.5))
    return uit


def toren_en_hut(park):
    px, _, pz = SPOTS["poel"]
    rx, rz = SPOTS["poel_r"]
    tx, tz, _, _ = kit.RESERVERINGEN["trekvlakte"]
    bx, _, bz = SPOTS.get("baobab", (px, 0, pz))
    # tower between the poel and the trekvlakte, near the path (visitors walk to it), looking over the poel
    mid = ((px + tx) // 2, (pz + tz) // 2)
    t = zoek(park, "savanne", mid, 30, 3, minhek=8, pad_min=3, pad_max=14, weg_van=[(px, pz), (bx, bz)], weg_r=18)
    if t:
        x, z, g = t
        uitkijktoren(park, x, z, g, (px, pz), hout="acacia")
        SPOTS["toren"] = (x, g, z)
    # the kijkhut: looks over the water at the rim where the elephants wade (targets: the water and the rim)
    doelen = [(px + 0.5, WATER_Y + 1.2, pz + 0.5)] + _rand_punten(park, px, pz, rx, rz)
    k = kies_kijkhut(park, "savanne", doelen, int(max(rx, rz)) + 4, int(max(rx, rz)) + 14,
                     weg_van=[(bx, bz)] + ([SPOTS["toren"][::2]] if "toren" in SPOTS else []), weg_r=9)
    if k:
        x, z, g, f, frac = k
        info = kijkhut(park, x, z, g, richting=f, hout="acacia", dak="hay_block", basis="minecraft:packed_mud",
                       titel=("Kijkhut", "De Drinkpoel", "Olifanten, zebra's", "en giraffen"),
                       plantje="minecraft:potted_dead_bush")
        paadje(park, _hut_raster(x, z, f, 5)(0, 5 + 2 + 1))
        SPOTS["hut"] = (x, g, z)
        SPOTS["hut_info"] = info
        SPOTS["hut_zicht"] = frac


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
    DECK = BRUG_DEK
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
            kit.stoel(park, c[0], DECK + 1, c[1], f)                 # guh chairs looking at the bridge
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
    # (round 3: the river pipeline (terrein.py + kit.brug + controle.py) owns water and bridges)
    trotse_rots(park)
    baobab_en_poel(park)
    termietenheuvels(park)
    trekvlakte(park)
    toren_en_hut(park)
    brug_halte(park)
    drijvend_hout_weg(park, ["savanne"], log)
    park._pad_d = None
    log(f"    savanne: {', '.join(sorted(SPOTS))}")


def _in_beeld(park, info, afstand, opzij=0, water_ok=False):
    """A dry (or wet) ground spot `afstand` in front of the kijkhut window, `opzij` to the side: (x, y, z)."""
    f = info["f"]
    vx, vz = STAP[f]
    sx, sz = (1, 0) if vx == 0 else (0, 1)
    fx, fz = info["voor"]
    for extra in range(0, 10):
        for sgn in (1, -1):
            x = int(fx + vx * (afstand + extra) + sx * opzij * sgn)
            z = int(fz + vz * (afstand + extra) + sz * opzij * sgn)
            if 0 <= x < W and 0 <= z < W and park.zone[x, z] == zi("savanne") and park.hek_afstand[x, z] >= 4 \
                    and (water_ok or not park.water[x, z]):
                return (x, int(park.hoogte[x, z]) + 1, z)
    return None


def plekken(park, plekken):
    """Leeuwen on the Trotse Rots; olifanten, a giraffe and a zebra family round the drinkpoel, right where the kijkhut
    window looks (the elephants on the 1-deep rim they wade through)."""
    if "trotse_rots_top" in SPOTS:
        for i in range(2):
            zet_plek(plekken, "leeuw", i, SPOTS["trotse_rots_top"], straal=12)
    if "poel" not in SPOTS:
        return
    px, _, pz = SPOTS["poel"]
    rx, rz = SPOTS["poel_r"]
    info = SPOTS.get("hut_info")
    if info:
        # the near half of the poel seen from the hut: elephants at the rim (wading in the 1-deep edge)
        hx, hz = info["voor"]
        rand = sorted(((math.hypot(x - hx, z - hz), x, z) for x, _, z in _rand_punten(park, px, pz, rx, rz, d=1.12, n=48)),
                      key=lambda t: t[0])
        voor = [(int(x), int(z)) for _, x, z in rand[:12]]
        for i in range(2):
            if i < len(voor):
                x, z = voor[i * 5 % len(voor)]
                zet_plek(plekken, "olifant", i, (x, int(park.hoogte[x, z]) + 1, z), straal=9)
        p = _in_beeld(park, info, 9, opzij=6)
        if p:
            zet_plek(plekken, "zebra", 0, p, straal=10)
        p = _in_beeld(park, info, 12, opzij=-7)
        if p:
            zet_plek(plekken, "giraffe", 0, p, straal=10)
    else:
        for soort in ("giraffe", "zebra"):
            for d in range(int(rx) + 2, int(rx) + 14):
                x, z = px + d, pz
                if not park.water[x, z]:
                    zet_plek(plekken, soort, 0, (x, int(park.hoogte[x, z]) + 1, z), straal=16)
                    break
            pz += 6
