"""
Op Safari! zone module: the Regenwoud landmarks (design 1.4 zone table), plus the helpers the bamboe module shares.

  waterval + grot      a mossy rock massif with a waterfall into a pond; behind the water a dark cave lit only by a
                       few soft glow lichens and spore blossoms (the glimwormpjes live here)
  reuzenboom           a 4x4 giant jungle tree with a spiral slab stair to a boomkronenpad ring at y 86, and two hanging
                       bridges to two satellite trees with their own rings (railings everywhere)
  tempelruine          an overgrown stepped temple; a chamber inside holds the geheimpje (a decorated pot)
  okapi-verstopdal     a sunken, very dense fern valley ringed by leaf walls and boulders, one narrow path in
  uitkijktoren         a wooden tower next to the truck route
  schuilhut            a little hide with a viewing slit near the river

The terrain (park.hoogte) is updated wherever a landmark raises or lowers the ground, so plekken.py (run after the zone
modules) puts animals ON things, not in them. plekken() moves the regenwoud homes onto their landmarks.

Round 3 (Z2): every spot bigger and wilder (a higher massif with boulders, jungle trees and firefly bushes round the
pond, glow-berry vines in the cave; a bigger canopy with hanging vines and glow berries under the ring and a third
satellite tree; a bigger ruin with broken pillars and fallen columns), cosy tower deck and hut (guh chairs, guh lamps,
cushions, a flower pot), lights registered with park.lamp, explicit viewpoints with favourite spots in their view, and
flat meadows ('platen') in the zone get a natural relief. Water and bridges stay with the river pipeline.
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit
from .kit import state
from .terrein import ZONES

WATER_Y = kit.WATER_Y
RNG = np.random.default_rng(kit.SEED + 31)

# --- shared helpers (also used by zone_bamboe) ---------------------------------------------------------------------------
_VEG = ("_leaves", "_log", "vine", "short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "bamboo", "dandelion",
        "poppy", "cornflower", "oxeye_daisy", "allium", "azure_bluet", "pink_tulip", "mushroom", "sugar_cane",
        "minecraft:snow[", "mangrove_roots", "lily_pad", "azalea", "jungle_wood")
_GROND = ("grass_block", "podzol", "moss_block", "coarse_dirt", "rooted_dirt", "dirt", "mud", "sand", "red_sand",
          "gravel", "stone", "granite", "andesite", "cobblestone", "mossy_cobblestone", "clay", "snow_block",
          "packed_ice", "muddy_mangrove_roots", "terracotta", "red_sandstone", "dirt_path")


def _lut(park, eind):
    lut = np.zeros(len(park.pal) + 256, bool)
    for i, s in enumerate(park.pal):
        if any(e in s for e in eind):
            lut[i] = True
    return lut


def ruim(park, cx, cz, r, extra=0):
    """Remove vegetation (trees, plants, bamboo, vines) in a cylinder above the ground, and the whole crowns of the
    trees whose trunk stands within r + extra, so nothing floats."""
    lut = _lut(park, _VEG)
    x0, x1 = max(int(cx - r - 1), 0), min(int(cx + r + 2), kit.W)
    z0, z1 = max(int(cz - r - 1), 0), min(int(cz + r + 2), kit.W)
    xx, zz = np.mgrid[x0:x1, z0:z1]
    cil = np.hypot(xx - cx, zz - cz) <= r
    box = park.v[x0:x1, :, z0:z1]
    veg = lut[box] & cil[:, None, :]
    box[veg] = 0
    for (bx, by, bz, soort) in park.bomen:
        if math.hypot(bx - cx, bz - cz) <= r + extra:
            rr = 8 if soort in ("reus", "jungle", "baobab") else 6
            a0, a1 = max(bx - rr, 0), min(bx + rr + 2, kit.W)
            b0, b1 = max(bz - rr, 0), min(bz + rr + 2, kit.W)
            sub = park.v[a0:a1, :, b0:b1]
            sub[lut[sub]] = 0


def zet_hoogte(park, x, z, nh, top="grass_block", sub="dirt", diep="stone"):
    """Make column (x, z) end exactly at nh with `top`; raising fills with sub/stone, lowering clears down."""
    h = int(park.hoogte[x, z])
    nh = int(nh)
    for y in range(nh + 1, max(h, nh) + 8):            # clear everything above (old ground + plants)
        if y < kit.H:
            park.v[x, y, z] = 0
    lo = min(int(park.vloer[x, z]), nh - 4)
    for y in range(max(lo, 1), nh):
        if park.v[x, y, z] == 0 or y > h - 3:
            park.set(x, y, z, sub if y >= nh - 2 else diep)
    if top in ("grass_block", "podzol"):
        top = state(top, snowy="false")
    park.set(x, nh, z, top)
    park.hoogte[x, z] = nh
    park.vloer[x, z] = min(int(park.vloer[x, z]), max(lo, 1))


def steun(park, x, z, y_bovenop, blok="stone"):
    """Solid column from the ground up to (not including) y_bovenop, so a raised build never floats."""
    for y in range(int(park.hoogte[x, z]) + 1, int(y_bovenop)):
        park.set(x, y, z, blok)


def zoek(park, zone, rond, r, hek=6, pad=(None, None), water=(None, None), zonder=(), dicht=None):
    """A spot in `zone` within r of rond (closest to rond first) that keeps distance hek from fences, pad distance in
    [pad[0], pad[1]], water distance in [water[0], water[1]], and stays `zonder` (x, z, r) free."""
    zi = ZONES.index(zone)
    m = (park.zone == zi) & ~park.water & (park.hek_afstand >= hek)
    if pad[0] is not None or pad[1] is not None:
        pd = getattr(park, "_pad_d", None)
        if pd is None:
            pd = park._pad_d = distance_transform_edt(~park.pad)
        if pad[0] is not None:
            m &= pd >= pad[0]
        if pad[1] is not None:
            m &= pd <= pad[1]
    if water[0] is not None or water[1] is not None:
        wd = getattr(park, "_water_d", None)
        if wd is None:
            wd = park._water_d = distance_transform_edt(~park.water)
        if water[0] is not None:
            m &= wd >= water[0]
        if water[1] is not None:
            m &= wd <= water[1]
    if dicht is not None:            # the whole disc of radius dicht must satisfy the mask (erode)
        from scipy.ndimage import binary_erosion
        k = int(dicht)
        yy, xx = np.mgrid[-k:k + 1, -k:k + 1]
        m = binary_erosion(m, structure=(np.hypot(yy, xx) <= k))
    xs, zs = np.nonzero(m)
    d = np.hypot(xs - rond[0], zs - rond[1])
    ok = d <= r
    for (ax, az, ar) in zonder:
        ok &= np.hypot(xs - ax, zs - az) > ar
    if not ok.any():
        return None
    i = int(np.argmin(np.where(ok, d, 1e9)))
    return int(xs[i]), int(zs[i])


def stairs(naam, facing, half="bottom", shape="straight"):
    return state(naam, facing=facing, half=half, shape=shape, waterlogged="false")


def slab(naam, typ="bottom"):
    return state(naam, type=typ, waterlogged="false")


def vine(**kant):
    p = {k: "false" for k in ("north", "east", "south", "west", "up")}
    p.update({k: "true" for k in kant})
    return state("vine", **p)


def lichen(**kant):
    p = {k: "false" for k in ("north", "east", "south", "west", "up", "down")}
    p.update({k: "true" for k in kant})
    return state("glow_lichen", waterlogged="false", **p)


# round 3 (Z2): cosy furniture (guhs blocks) and lights
GUHLAMP = "guhs:klusjes_guhlampje[facing=south,lit=true]"
OORLANTAARN = "guhs:doolhof_lantaarn[lit=true]"
KUSSENS = ("pink_kussen", "lime_kussen", "yellow_kussen", "light_blue_kussen", "white_kussen", "magenta_kussen")


def kussen(park, x, y, z, kleur=None, facing="south"):
    k = kleur or KUSSENS[int(RNG.integers(0, len(KUSSENS)))]
    park.set(x, y, z, f"guhs:{k}[facing={facing}]")


def bloempot(park, x, y, z, facing="south"):
    park.set(x, y, z, f"guhs:guh_bloempot[facing={facing}]")


def glowberry(park, x, y, z, lengte=2):
    """Glow-berry vines (cave_vines with berries, they shine) hanging DOWN from the solid block above (x, y + 1, z)."""
    lengte = max(1, int(lengte))
    for i in range(lengte):
        yy = y - i
        if park.v[x, yy, z] != 0:
            break
        laatste = i == lengte - 1 or park.v[x, yy - 1, z] != 0
        if laatste:
            park.set(x, yy, z, state("cave_vines", age=25, berries="true"))
            break
        park.set(x, yy, z, state("cave_vines_plant", berries="true"))
    park.lampen.append((int(x), int(y), int(z), "glowberry"))


def vuurvliegjes(park, cx, cz, r, n, zone=None, rng=None):
    """Firefly bushes (they glow and fireflies dance round them at night) on grass/moss/podzol within r of (cx, cz)."""
    rng = rng if rng is not None else RNG
    zi = ZONES.index(zone) if zone else None
    gezet = 0
    for _ in range(n * 8):
        if gezet >= n:
            break
        a, d = rng.random() * 2 * math.pi, rng.random() * r
        x, z = int(round(cx + math.cos(a) * d)), int(round(cz + math.sin(a) * d))
        if not (2 <= x < kit.W - 2 and 2 <= z < kit.W - 2) or park.water[x, z] or park.pad[x, z]:
            continue
        if zi is not None and park.zone[x, z] != zi:
            continue
        h = int(park.hoogte[x, z])
        if park.v[x, h + 1, z] != 0 or not any(g in park.get(x, h, z) for g in ("grass_block", "moss_block", "podzol",
                                                                                 "rooted_dirt", "coarse_dirt")):
            continue
        park.lamp(x, h + 1, z, "minecraft:firefly_bush")
        gezet += 1
    return gezet


def ontplaat(park, zone, vermijd=(), log=print):
    """Round 3 (Z2): flat meadows in the zone (controle.platen: one height + one top block over >= 300 columns) get a
    gentle natural relief (+0..2, never lower: banks stay above the water line) and a mixed top (grass, podzol, moss,
    coarse dirt) with ferns and bushes, away from water, paths, fences and the landmarks (vermijd: (x, z, r))."""
    from . import controle
    _, lijst = controle.platen(park)
    rng = np.random.default_rng(kit.SEED + 77 + ZONES.index(zone))
    relief = kit.ruis(rng, 6.0) if hasattr(kit, "ruis") else None
    klein = _lut(park, ("short_grass", "tall_grass", "fern", "dandelion", "poppy", "cornflower", "oxeye_daisy",
                        "azure_bluet", "allium", "pink_tulip", "dead_bush", "moss_carpet"))
    n_kol = 0
    for (px, pz, opp, blok, y, zn) in lijst:
        if zn != zone:
            continue
        r = int(math.sqrt(opp) * 0.9) + 6
        for x in range(max(px - r, 2), min(px + r, kit.W - 2)):
            for z in range(max(pz - r, 2), min(pz + r, kit.W - 2)):
                if park.zone[x, z] != ZONES.index(zone) or park.water[x, z] or park.pad[x, z]:
                    continue
                if int(park.hoogte[x, z]) != y or park.hek_afstand[x, z] < 4:
                    continue
                if any(math.hypot(x - ax, z - az) <= ar for ax, az, ar in vermijd):
                    continue
                if park.water[x - 2:x + 3, z - 2:z + 3].any():
                    continue                                       # banks: the river pipeline's
                kol = park.v[x, y + 1:y + 12, z]
                if kol.any() and not klein[kol[kol != 0]].all():
                    continue                                       # something built / growing here: leave it
                v = relief[x, z] if relief is not None else rng.normal()
                dh = 0 if v < -0.3 else (1 if v < 0.7 else 2)
                top = ("grass_block", "grass_block", "podzol", "moss_block", "coarse_dirt")[int(rng.integers(0, 5))]
                if dh:
                    zet_hoogte(park, x, z, y + dh, top=top, sub="dirt")
                else:
                    park.set(x, y, z, state(top, snowy="false") if top in ("grass_block", "podzol") else top)
                hh = int(park.hoogte[x, z])
                if park.v[x, hh + 1, z] == 0:
                    q = rng.random()
                    if q < 0.18:
                        park.set(x, hh + 1, z, "minecraft:fern")
                    elif q < 0.24:
                        park.set(x, hh + 1, z, state("large_fern", half="lower"))
                        park.set(x, hh + 2, z, state("large_fern", half="upper"))
                    elif q < 0.27:
                        park.set(x, hh + 1, z, park.leaves("jungle" if zone == "regenwoud" else "azalea"))
                n_kol += 1
    log(f"    {zone}: {n_kol} kolommen vlakte natuurlijker gemaakt")


def hek_ok(park, x0, z0, x1, z1, minimum=4):
    sub = park.hek_afstand[max(x0, 0):x1 + 1, max(z0, 0):z1 + 1]
    return sub.size == 0 or float(sub.min()) >= minimum


def toren(park, x, z, hout="spruce", hoogte=12, dak="spruce", log=print, gezellig=False):
    """A 5x5 wooden lookout tower (legs, a ladder up through a hatch, a railed deck with a roof) standing at (x, z).
    gezellig (round 3): two guh chairs looking out, a guh lamp and a cushion on the deck; the kijk-guh sits on a chair
    (not in the ladder hole)."""
    g = int(np.median(park.hoogte[x - 2:x + 3, z - 2:z + 3]))
    ruim(park, x, z, 5, extra=1)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            zet_hoogte(park, x + dx, z + dz, g, top="coarse_dirt")
    dek = g + hoogte
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for y in range(g + 1, dek + 4):
            park.set(x + dx, y, z + dz, state(f"{hout}_log", axis="y"))
    park.fill(x - 2, dek, z - 2, x + 2, dek, z + 2, state(f"{hout}_planks"))
    for y in range(g + 1, dek + 1):                            # a ladder up a central post, through a hole in the deck
        park.set(x, y, z + 1, state(f"{hout}_log", axis="y"))
        park.set(x, y, z, state("ladder", facing="north", waterlogged="false"))
    for i in range(-2, 3):                                     # railing
        for (ax, az) in ((x + i, z - 2), (x + i, z + 2), (x - 2, z + i), (x + 2, z + i)):
            if abs(ax - x) == 2 and abs(az - z) == 2:
                continue
            park.set(ax, dek + 1, az, "minecraft:spruce_fence")
    # cross braces on the legs
    for y in range(g + 3, dek, 4):
        for i in range(-1, 2):
            park.set(x + i, y, z - 2, state(f"{hout}_log", axis="x"))
            park.set(x + i, y, z + 2, state(f"{hout}_log", axis="x"))
    # roof: a pyramid of stairs
    for lvl in range(3):
        y = dek + 4 + lvl
        r = 3 - lvl
        for i in range(-r, r + 1):
            park.set(x + i, y, z - r, stairs(f"{dak}_stairs", "south"))
            park.set(x + i, y, z + r, stairs(f"{dak}_stairs", "north"))
            park.set(x - r, y, z + i, stairs(f"{dak}_stairs", "east"))
            park.set(x + r, y, z + i, stairs(f"{dak}_stairs", "west"))
        if r <= 1:
            park.fill(x - r + 1, y, z - r + 1, x + r - 1, y, z + r - 1, state(f"{dak}_planks"))
    park.set(x, dek + 7, z, state(f"{dak}_planks"))
    park.set(x, dek + 3, z, state("lantern", hanging="true", waterlogged="false"))
    kit.toren_kijkplek(park, x, z, dek)
    if gezellig:
        kit.stoel(park, x - 1, dek + 1, z - 1, "west")
        kit.stoel(park, x + 1, dek + 1, z - 1, "east")
        park.lamp(x - 1, dek + 1, z + 1, GUHLAMP)
        kussen(park, x + 1, dek + 1, z + 1)
        park.kijkplekken[-1]["stoel"] = [int(x - 1), int(dek + 1), int(z - 1)]
    return dek


def schuilhut(park, x, z, kijk="north", hout="spruce", log=print):
    """A 5x5 hide: low walls, a viewing slit on the `kijk` side, a slab roof with leaves on it, a door at the back."""
    g = int(np.median(park.hoogte[x - 2:x + 3, z - 2:z + 3]))
    ruim(park, x, z, 4, extra=0)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            zet_hoogte(park, x + dx, z + dz, g, top="coarse_dirt")
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            rand = abs(dx) == 2 or abs(dz) == 2
            for y in range(g + 1, g + 4):
                if rand:
                    park.set(x + dx, y, z + dz, state(f"{hout}_planks"))
            park.set(x + dx, g + 4, z + dz, slab(f"{hout}_slab"))
            if RNG.random() < 0.5:
                park.set(x + dx, g + 5, z + dz, park.leaves("oak" if hout != "bamboo" else "azalea"))
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for y in range(g + 1, g + 4):
            park.set(x + dx, y, z + dz, state(f"{hout}_log", axis="y"))
    vec = {"north": (0, -2), "south": (0, 2), "west": (-2, 0), "east": (2, 0)}
    kx, kz = vec[kijk]
    for i in (-1, 0, 1):
        sx, sz = (x + i, z + kz) if kz else (x + kx, z + i)
        park.set(sx, g + 2, sz, "minecraft:air")              # the slit
    bx, bz = (x - kx // 2 * 1, z - kz) if kz else (x - kx, z)
    tegen = {"north": "south", "south": "north", "west": "east", "east": "west"}[kijk]
    park.set(bx, g + 1, bz, state(f"{hout}_door", facing=tegen, half="lower", hinge="left", open="false",
                                  powered="false"))
    park.set(bx, g + 2, bz, state(f"{hout}_door", facing=tegen, half="upper", hinge="left", open="false",
                                  powered="false"))
    # (round 4) a way out of the back door: on a slope the door opened onto the hill; cut a 1-wide trail that climbs
    # one block per step until it meets the terrain (or a path / water)
    ux, uz = (-kx // 2, -kz // 2)
    for k in range(1, 10):
        tx, tz = bx + ux * k, bz + uz * k
        if not (0 <= tx < kit.W and 0 <= tz < kit.W) or park.water[tx, tz]:
            break
        if int(park.hoogte[tx, tz]) <= g + k - 1:
            break
        zet_hoogte(park, tx, tz, g + k - 1, top="coarse_dirt")
    kit.stoel(park, x, g + 1, z, kijk)                      # a guh chair looking out of the slit
    park.set(x, g + 3, z, state("lantern", hanging="true", waterlogged="false"))
    return g


# --- regenwoud landmarks ------------------------------------------------------------------------------------------------
PLAATS = {}       # filled by bouw(): name -> (x, y, z) for plekken()


def _massief_hoogte(dx, dz, rx, rz, top, g):
    d = (dx / rx) ** 2 + (dz / rz) ** 2
    if d > 1:
        return None
    return int(round(g + (top - g) * (1 - d ** 1.6) ** 0.5))


def waterval_grot(park, log):
    wx, wz, _, _ = kit.RESERVERINGEN["waterval_grot"]
    g = int(np.median(park.hoogte[wx - 6:wx + 7, wz - 6:wz + 7]))
    # pond on the north side, massif to the south of it
    px, pz = wx, wz - 7
    mx, mz = wx, wz + 6
    rx, rz, top = 14, 12, g + 24
    ruim(park, wx, wz, 20, extra=3)
    rocks = ("stone", "mossy_cobblestone", "andesite", "cobblestone", "stone")
    for dx in range(-rx, rx + 1):
        for dz in range(-rz, rz + 1):
            nh = _massief_hoogte(dx, dz, rx, rz, top, g)
            x, z = mx + dx, mz + dz
            if dz <= -rz + 2:
                continue                 # a vertical cliff face towards the pond (z > face_z)
            if nh is None or park.water[x, z] or nh <= park.hoogte[x, z]:
                continue
            top_blok = "moss_block" if nh > g + 3 else "mossy_cobblestone"
            zet_hoogte(park, x, z, nh, top=top_blok, sub=rocks[int(RNG.integers(0, 5))], diep="stone")
            for y in range(int(park.vloer[x, z]), nh):         # rock all the way (no dirt inside a cliff)
                if park.v[x, y, z] != 0:
                    park.v[x, y, z] = park.id(rocks[int(RNG.integers(0, 5))])
    # the cliff face towards the pond is the north edge of the massif: z = mz - rz .. find per x
    # pond
    for dx in range(-7, 8):
        for dz in range(-6, 7):
            x, z = px + dx, pz + dz
            d = math.hypot(dx / 7, dz / 6)
            if d > 1 or park.hoogte[x, z] > g + 4:
                continue
            bodem = WATER_Y - (3 if d < 0.6 else 1)
            zet_hoogte(park, x, z, bodem, top="clay" if d < 0.6 else "sand", sub="dirt")
            for y in range(bodem + 1, WATER_Y + 1):
                park.set(x, y, z, state("water", level=0))
            park.water[x, z] = True
            if RNG.random() < 0.06 and d > 0.5:
                park.set(x, WATER_Y + 1, z, "minecraft:lily_pad")
    # banks around the pond: ferns, moss
    for dx in range(-9, 10):
        for dz in range(-8, 9):
            x, z = px + dx, pz + dz
            d = math.hypot(dx / 9, dz / 8)
            if 0.8 < d <= 1 and not park.water[x, z] and park.v[x, int(park.hoogte[x, z]) + 1, z] == 0:
                park.set(x, int(park.hoogte[x, z]), z, "minecraft:moss_block")
                if RNG.random() < 0.4:
                    park.set(x, int(park.hoogte[x, z]) + 1, z, "minecraft:fern")
    # waterfall: a 4-wide curtain from the massif top down into the pond, in front of the cave mouth
    face_z = mz - rz + 2
    for x in range(wx - 2, wx + 2):
        top_y = int(park.hoogte[x, face_z + 1])
        for z in range(face_z + 1, face_z + 7):                 # the stream on top, cut 1 deep into the rock
            hh = int(park.hoogte[x, z])
            park.set(x, hh, z, state("water", level=0))
        for y in range(WATER_Y + 1, top_y + 1):
            park.set(x, y, face_z, state("water", level=8))      # falling water (static: structures don't tick it)
        for y in range(WATER_Y - 2, WATER_Y + 1):
            park.set(x, y, face_z, state("water", level=0))
    # the cave: a mouth right behind the curtain, a chamber deeper in
    vloer = WATER_Y + 2
    for x in range(wx - 2, wx + 2):
        for z in range(face_z + 1, face_z + 4):
            for y in range(vloer, vloer + 4):
                park.v[x, y, z] = 0
            park.set(x, vloer - 1, z, "minecraft:mossy_cobblestone")
    ccx, ccz, ccy = wx, face_z + 9, vloer + 2
    for dx in range(-7, 8):
        for dz in range(-6, 7):
            for dy in range(-3, 5):
                d = (dx / 6.5) ** 2 + (dz / 5.5) ** 2 + (dy / 4.2) ** 2
                if d <= 1 and ccy + dy >= vloer:
                    park.v[ccx + dx, ccy + dy, ccz + dz] = 0
    # a passage from the mouth to the chamber
    for z in range(face_z + 1, ccz):
        for x in range(wx - 2, wx + 2):
            for y in range(vloer, vloer + 4):
                park.v[x, y, z] = 0
    # floor + soft light: glow lichen on the ceiling, a few spore blossoms, moss carpet on the floor
    for dx in range(-7, 8):
        for dz in range(-6, 7):
            x, z = ccx + dx, ccz + dz
            if park.v[x, vloer, z] == 0 and park.v[x, vloer - 1, z] != 0:
                if RNG.random() < 0.35:
                    park.set(x, vloer, z, "minecraft:moss_carpet")
            # ceiling: first solid above the chamber floor
            for y in range(vloer + 1, ccy + 6):
                if park.v[x, y, z] == 0 and park.v[x, y + 1, z] != 0 and not park.get(x, y + 1, z).startswith(
                        "minecraft:water"):
                    r = RNG.random()
                    if r < 0.10:
                        park.set(x, y, z, lichen(up=True))
                    elif r < 0.13:
                        park.set(x, y, z, "minecraft:spore_blossom")
                    elif r < 0.20:
                        glowberry(park, x, y, z, lengte=1 + int(RNG.integers(0, 3)))
                    break
    park.set(ccx + 3, vloer, ccz + 2, state("decorated_pot", facing="north", cracked="true", waterlogged="false"))
    PLAATS["grot"] = (ccx, vloer, ccz)
    PLAATS["waterval_poel"] = (px, WATER_Y + 1, pz)
    # vines on the cliff face beside the waterfall
    for x in range(mx - rx, mx + rx + 1):
        if wx - 3 <= x <= wx + 2:
            continue
        for z in range(mz - rz, mz):
            hh = int(park.hoogte[x, z])
            if hh > g + 4 and park.v[x, hh, z - 1] == 0:
                for y in range(hh - 1, max(hh - 6, g), -1):
                    if park.v[x, y, z - 1] == 0 and park.v[x, y, z] != 0 and RNG.random() < 0.5:
                        park.set(x, y, z - 1, vine(south=True))
    # jungle bushes on top of the massif
    for _ in range(14):
        dx, dz = int(RNG.integers(-rx + 2, rx - 1)), int(RNG.integers(-rz + 3, rz - 1))
        x, z = mx + dx, mz + dz
        if abs(x - wx) <= 3 and z < face_z + 8:
            continue
        hh = int(park.hoogte[x, z])
        if hh > g + 4:
            park.blob(x, hh + 1, z, 1.4, 1.0, 1.4, park.leaves("jungle"))
    # round 3: a few small jungle trees on the massif (wilder skyline), mossy boulders and ferns round the pond, firefly
    # bushes on the banks
    for k in range(4):
        a = k * math.pi / 2 + 0.7
        x, z = int(round(mx + math.cos(a) * rx * 0.55)), int(round(mz + 2 + math.sin(a) * rz * 0.45))
        hh = int(park.hoogte[x, z])
        if hh > g + 6 and park.v[x, hh + 1, z] == 0 and not (abs(x - wx) <= 3 and z < face_z + 10):
            t = park.boom("jungle", x, hh, z, grootte=0.5)
            park.bomen.append((x, t, z, "jungle"))
    for k in range(7):
        a = k * 2 * math.pi / 7 + 0.3
        x, z = int(round(px + math.cos(a) * 9.5)), int(round(pz + math.sin(a) * 8.5))
        if park.water[x - 1:x + 2, z - 1:z + 2].any() or park.pad[x, z] or park.hek_afstand[x, z] < 5:
            continue
        hh = int(park.hoogte[x, z])
        park.blob(x, hh + 1, z, 1.3 + RNG.random(), 1.0, 1.3 + RNG.random(), "minecraft:mossy_cobblestone")
        park.set(x, hh + 3, z, "minecraft:moss_carpet", keep=True)
    vuurvliegjes(park, px, pz, 12, 8, zone="regenwoud")
    log(f"    waterval + grot bij ({wx},{wz}), top y{top}, grot y{vloer}")


def reuzenboom(park, log):
    rx, rz, _, _ = kit.RESERVERINGEN["reuzenboom"]
    g = int(np.median(park.hoogte[rx - 3:rx + 4, rz - 3:rz + 4]))
    ruim(park, rx, rz, 18, extra=4)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if math.hypot(dx, dz) <= 3.6:
                zet_hoogte(park, rx + dx, rz + dz, g, top="podzol")
    PAD_Y = g + 22
    trunk_top = PAD_Y + 18
    log_j = state("jungle_log", axis="y")
    # 4x4 trunk (rx-1 .. rx+2) with root flares
    for y in range(g - 2, trunk_top):
        for dx in range(-1, 3):
            for dz in range(-1, 3):
                park.set(rx + dx, y, rz + dz, log_j)
    for (dx, dz, ax) in ((-2, 0, "x"), (-2, 1, "x"), (3, 0, "x"), (3, 1, "x"), (0, -2, "z"), (1, -2, "z"), (0, 3, "z"),
                         (1, 3, "z")):
        for y in range(g + 1, g + 3 + int(RNG.integers(0, 3))):
            park.set(rx + dx, y, rz + dz, "minecraft:jungle_wood")
    # canopy
    cy = trunk_top
    park.blob(rx + 0.5, cy, rz + 0.5, 12, 4.5, 12, park.leaves("jungle"), gat=0.2)
    park.blob(rx + 5, cy + 3, rz - 3, 6, 3, 6, park.leaves("jungle"), gat=0.2)
    park.blob(rx - 4, cy + 2, rz + 5, 6, 3, 6, park.leaves("jungle"), gat=0.2)
    for k in range(6):          # branches into the canopy
        a = k * math.pi / 3 + 0.3
        for t in range(2, 9):
            park.set(int(round(rx + 0.5 + math.cos(a) * t)), cy - 3 + t // 3, int(round(rz + 0.5 + math.sin(a) * t)),
                     state("jungle_log", axis="x" if abs(math.cos(a)) > abs(math.sin(a)) else "z"))
    # make room for the ring and stair under the canopy
    for dx in range(-8, 10):
        for dz in range(-8, 10):
            for y in range(g + 1, PAD_Y + 4):
                s = park.get(rx + dx, y, rz + dz)
                if "leaves" in s or "vine" in s:
                    park.set(rx + dx, y, rz + dz, "minecraft:air")
    cxs, czs = rx + 0.5, rz + 0.5
    # ring deck at PAD_Y first (annulus 2.4 .. 6.6, railing ring at 6.6 .. 7.5); the stair cuts its way up through it
    for dx in range(-8, 10):
        for dz in range(-8, 10):
            d = math.hypot(rx + dx - cxs, rz + dz - czs)
            x, z = rx + dx, rz + dz
            if 2.4 < d <= 6.6:
                park.set(x, PAD_Y, z, state("spruce_planks"))
                for y in range(PAD_Y + 1, PAD_Y + 4):
                    park.set(x, y, z, "minecraft:air")
            elif 6.6 < d <= 7.5:
                park.set(x, PAD_Y, z, state("spruce_planks"))
                park.set(x, PAD_Y + 1, z, "minecraft:spruce_fence")
    # spiral stair around the trunk: 2 wide (radius 3.3 and 4.3), half a block up per step, railing at radius 5.4
    stappen = (PAD_Y - g) * 2
    treden = {}
    for i in range(stappen):
        a = i * (2 * math.pi / 28)
        y = g + 1 + i // 2
        typ = "bottom" if i % 2 == 0 else "top"
        for rr in (3.3, 4.3):
            x = int(round(cxs + math.cos(a) * rr))
            z = int(round(czs + math.sin(a) * rr))
            treden[(x, z)] = (y, typ)       # a later (higher) step on the same column wins
        ox = int(round(cxs + math.cos(a) * 5.4))
        oz = int(round(czs + math.sin(a) * 5.4))
        treden.setdefault(("hek", ox, oz), y)
        treden[("hek", ox, oz)] = y
    for key, val in treden.items():
        if key[0] == "hek":
            continue
        (x, z), (y, typ) = key, val
        park.set(x, y, z, slab("spruce_slab", typ))
        for hh in range(y + 1, y + 4):
            park.set(x, hh, z, "minecraft:air")
        for hh in range(int(park.hoogte[x, z]) + 1, y):            # nothing to fall into under the lowest turn
            if y - int(park.hoogte[x, z]) <= 2:
                park.set(x, hh, z, "minecraft:jungle_planks")
    for key, y in treden.items():
        if key[0] != "hek":
            continue
        _, x, z = key
        if (x, z) not in treden and y < PAD_Y:
            park.set(x, y + 1, z, "minecraft:spruce_fence")
            if park.v[x, y, z] == 0:
                park.set(x, y, z, slab("spruce_slab", "bottom"))
    # supports under the ring: four struts
    for a in (0.5, 2.1, 3.7, 5.3):
        for t in range(3, 7):
            park.set(int(round(cxs + math.cos(a) * t)), PAD_Y - 1 - (6 - t), int(round(czs + math.sin(a) * t)),
                     "minecraft:jungle_wood")
    for a in (0.0, 1.6, 3.2, 4.8):
        park.lamp(int(round(cxs + math.cos(a) * 6.8)), PAD_Y + 2, int(round(czs + math.sin(a) * 6.8)), "lantaarn")
    # round 3: glow berries and vines hanging under the ring deck (light at night, wilder), vines from the canopy
    for k in range(12):
        a = k * math.pi / 6 + 0.25
        x, z = int(round(cxs + math.cos(a) * 5.2)), int(round(czs + math.sin(a) * 5.2))
        if park.v[x, PAD_Y, z] != 0 and park.v[x, PAD_Y - 1, z] == 0:
            if k % 3 == 0:
                glowberry(park, x, PAD_Y - 1, z, lengte=2 + int(RNG.integers(0, 3)))
            else:
                park.set(x, PAD_Y - 1, z, vine(up=True))      # (an up-vine needs the block above: 1 long)
    for _ in range(40):
        a, d = RNG.random() * 2 * math.pi, 4 + RNG.random() * 8
        x, z = int(round(cxs + math.cos(a) * d)), int(round(czs + math.sin(a) * d))
        for y in range(cy - 6, cy + 2):
            if park.v[x, y, z] == 0 and "leaves" in park.get(x, y + 1, z):
                if y > PAD_Y + 3:
                    park.set(x, y, z, vine(up=True))
                break
    PLAATS["reuzenboom"] = (rx, PAD_Y + 1, rz)
    # two satellite trees with their own rings, hanging bridges to the main ring
    sats = []
    for k in range(18):
        if len(sats) >= 3:
            break
        hoek = k * math.pi / 9 + 0.4
        sx, sz = int(round(rx + math.cos(hoek) * 17)), int(round(rz + math.sin(hoek) * 17))
        if park.water[sx - 2:sx + 4, sz - 2:sz + 4].any() or park.hek_afstand[sx, sz] < 10 or                 park.zone[sx, sz] != ZONES.index("regenwoud") or park.pad[sx - 3:sx + 5, sz - 3:sz + 5].any():
            continue
        if any(math.hypot(sx + 0.5 - a, sz + 0.5 - b) < 14 for a, b in sats):
            continue
        sg = int(park.hoogte[sx, sz])
        ruim(park, sx, sz, 6, extra=2)
        top = PAD_Y + 9
        for y in range(sg - 1, top):
            for dx in range(0, 2):
                for dz in range(0, 2):
                    park.set(sx + dx, y, sz + dz, log_j)
        park.blob(sx + 0.5, top, sz + 0.5, 7, 3.5, 7, park.leaves("jungle"), gat=0.2)
        for dx in range(-5, 7):
            for dz in range(-5, 7):
                for y in range(PAD_Y + 1, PAD_Y + 4):
                    if "leaves" in park.get(sx + dx, y, sz + dz):
                        park.set(sx + dx, y, sz + dz, "minecraft:air")
                d = math.hypot(sx + dx - sx - 0.5, sz + dz - sz - 0.5)
                if 1.4 < d <= 3.6:
                    park.set(sx + dx, PAD_Y, sz + dz, state("spruce_planks"))
                elif 3.6 < d <= 4.5:
                    park.set(sx + dx, PAD_Y, sz + dz, state("spruce_planks"))
                    park.set(sx + dx, PAD_Y + 1, sz + dz, "minecraft:spruce_fence")
        sats.append((sx + 0.5, sz + 0.5))
    for (sx, sz) in sats:
        hangbrug(park, (cxs, czs, 6.9), (sx, sz, 4.2), PAD_Y + 1)
    log(f"    reuzenboom bij ({rx},{rz}), pad y{PAD_Y}, {len(sats)} hangbruggen")


def hangbrug(park, a, b, y, half=1, plank="spruce", reling="minecraft:spruce_fence"):
    """A walkable rope bridge between two rings/rims a=(x, z, r) and b=(x, z, r); y = the walk surface at both ends
    (the top of the deck blocks), sagging 1 in the middle, a railing on both sides; the ring railings (at y) open where
    it lands."""
    ax, az, ar = a
    bx, bz, br = b
    L = math.hypot(bx - ax, bz - az)
    ux, uz = (bx - ax) / L, (bz - az) / L
    nx, nz = -uz, ux
    t0, t1 = ar - 0.6, L - br + 0.6
    dek = {}
    for i in range(int((t1 - t0) * 3) + 1):
        t = t0 + i / 3.0
        f = (t - t0) / max(t1 - t0, 1e-6)
        yy = y - math.sin(f * math.pi) * 1.0          # walk surface height (y = top of a block at y-1... i.e. deck level)
        for k in range(-half * 3, half * 3 + 1):
            o = k / 3.0
            x = int(round(ax + ux * t + nx * o))
            z = int(round(az + uz * t + nz * o))
            dek[(x, z)] = min(dek.get((x, z), 99), yy)
    for (x, z), yy in dek.items():
        sv = round(yy * 2) / 2                       # walk surface on a half-block grid
        if sv == int(sv):
            park.set(x, int(sv) - 1, z, slab(f"{plank}_slab", "top"))
            vrij = int(sv)
        else:
            park.set(x, int(math.floor(sv)), z, slab(f"{plank}_slab", "bottom"))
            vrij = int(math.floor(sv)) + 1
        for hh in range(vrij, vrij + 3):
            if park.get(x, hh, z) != "minecraft:air" and "fence" not in park.get(x, hh, z):
                park.set(x, hh, z, "minecraft:air")
    rand = {}
    for i in range(int((t1 - t0) * 3) + 1):
        t = t0 + i / 3.0
        f = (t - t0) / max(t1 - t0, 1e-6)
        if f < 0.04 or f > 0.96:
            continue
        yy = y - math.sin(f * math.pi) * 1.0
        for o in (-half - 1.0, half + 1.0):
            x = int(round(ax + ux * t + nx * o))
            z = int(round(az + uz * t + nz * o))
            if (x, z) not in dek:
                rand[(x, z)] = min(rand.get((x, z), 99), yy)
    for (x, z), yy in rand.items():
        sv = round(yy * 2) / 2
        park.set(x, int(math.ceil(sv)), z, reling)
    # open the ring railings where the bridge lands
    for (cx, cz, r, s0) in ((ax, az, ar, 1), (bx, bz, br, -1)):
        for k in range(-4, 5):
            for rr in (r - 0.4, r, r + 0.4, r + 0.9):
                x = int(round(cx + ux * s0 * rr + nx * k * 0.35))
                z = int(round(cz + uz * s0 * rr + nz * k * 0.35))
                if "fence" in park.get(x, y, z) and (x, z) not in rand:
                    park.set(x, y, z, "minecraft:air")


def ruine(park, log):
    rx, rz, _, _ = kit.RESERVERINGEN["ruine"]
    g = int(np.median(park.hoogte[rx - 7:rx + 8, rz - 7:rz + 8]))
    ruim(park, rx, rz, 16, extra=3)
    blokken = ("stone_bricks", "mossy_stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "mossy_cobblestone")

    def steen():
        return "minecraft:" + blokken[int(RNG.integers(0, len(blokken)))]

    for dx in range(-9, 10):
        for dz in range(-9, 10):
            zet_hoogte(park, rx + dx, rz + dz, g, top="moss_block" if RNG.random() < 0.5 else "podzol")
    tiers = [(8, 2), (6, 2), (4, 2), (2, 1)]
    y = g + 1
    for (r, hgt) in tiers:
        for dy in range(hgt):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if RNG.random() < 0.04 and r > 2 and (abs(dx) == r or abs(dz) == r):
                        continue           # a few blocks missing (ruin)
                    park.set(rx + dx, y + dy, rz + dz, steen())
        y += hgt
    top_y = y
    # a wide stair on the north face up to the top
    for i, (r, hgt) in enumerate(tiers[:-1]):
        pass
    for step in range(top_y - g - 1):
        z = rz - 9 + step
        yy = g + 1 + step
        for dx in (-1, 0, 1):
            park.set(rx + dx, yy, z, stairs("stone_brick_stairs", "south"))
            for hh in range(yy + 1, yy + 4):
                if hh < top_y or z < rz - 2:
                    if park.v[rx + dx, hh, z] != 0 and hh > yy:
                        park.set(rx + dx, hh, z, "minecraft:air")
        if z >= rz - 2:
            break
    # shrine on top: chiseled pillars + a slab roof
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        for hh in range(top_y, top_y + 3):
            park.set(rx + dx, hh, rz + dz, "minecraft:chiseled_stone_bricks")
    park.fill(rx - 2, top_y + 3, rz - 2, rx + 2, top_y + 3, rz + 2, slab("stone_brick_slab", "bottom"))
    park.lamp(rx, top_y, rz, "lantaarn")
    # chamber inside the bottom tier with an entrance on the south side; the geheimpje in the middle
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for hh in range(g + 1, g + 4):
                park.set(rx + dx, hh, rz + dz, "minecraft:air")
    for hh in range(g + 1, g + 3):
        for dx in (-1, 0, 1):
            for dz in range(4, 8):
                park.set(rx + dx, hh, rz + dz, "minecraft:air")
    park.fill(rx - 3, g, rz - 3, rx + 3, g, rz + 3, "minecraft:mossy_stone_bricks")
    park.set(rx, g + 1, rz, state("decorated_pot", facing="south", cracked="true", waterlogged="false"))
    park.set(rx - 2, g + 1, rz - 2, "minecraft:moss_carpet")
    park.set(rx + 2, g + 3, rz + 2, lichen(up=True))
    # overgrowth: vines on the outer walls, moss carpet and bushes on the tiers
    # round 3: broken pillars and fallen columns round the temple (wilder ruin)
    for k in range(8):
        a = k * math.pi / 4 + 0.2
        x, z = int(round(rx + math.cos(a) * 12)), int(round(rz + math.sin(a) * 12))
        if park.water[x, z] or park.pad[x, z] or park.hek_afstand[x, z] < 5 or park.zone[x, z] != ZONES.index("regenwoud"):
            continue
        hh = int(park.hoogte[x, z])
        if k % 3 == 2:                                         # a fallen column lying in the moss
            ax = "x" if abs(math.cos(a)) < abs(math.sin(a)) else "z"
            for t in range(-2, 3):
                fx, fz = (x + t, z) if ax == "x" else (x, z + t)
                if park.v[fx, int(park.hoogte[fx, fz]) + 1, fz] == 0:
                    park.set(fx, int(park.hoogte[fx, fz]) + 1, fz, state("stone_bricks"))
        else:
            for y in range(hh + 1, hh + 2 + int(RNG.integers(1, 5))):
                park.set(x, y, z, "minecraft:mossy_stone_bricks" if RNG.random() < 0.5 else "minecraft:cracked_stone_bricks")
            park.set(x, y + 1, z, "minecraft:moss_carpet")
    for dx in range(-9, 10):
        for dz in range(-9, 10):
            x, z = rx + dx, rz + dz
            col = [yy for yy in range(g + 1, top_y + 4) if park.v[x, yy, z] != 0]
            if not col:
                continue
            tt = col[-1]
            if park.v[x, tt + 1, z] == 0 and "stone" in park.get(x, tt, z) and RNG.random() < 0.3:
                park.set(x, tt + 1, z, "minecraft:moss_carpet")
            elif park.v[x, tt + 1, z] == 0 and RNG.random() < 0.06:
                park.blob(x, tt + 1, z, 1.0, 0.8, 1.0, park.leaves("jungle"))
    for x in range(rx - 9, rx + 10):
        for z in range(rz - 9, rz + 10):
            for (ddx, ddz, kant) in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
                for hh in range(g + 1, top_y):
                    if park.v[x, hh, z] == 0 and "stone" in park.get(x + ddx, hh, z + ddz) and RNG.random() < 0.18:
                        park.set(x, hh, z, vine(**{kant: True}))
    PLAATS["ruine_top"] = (rx, top_y, rz)
    PLAATS["ruine_kamer"] = (rx, g + 1, rz)
    log(f"    tempelruine bij ({rx},{rz}), top y{top_y}")


def okapi_dal(park, log):
    ox, oz, _, _ = kit.RESERVERINGEN["okapi_dal"]
    plek = zoek(park, "regenwoud", (ox, oz), 70, hek=17, pad=(14, None), dicht=12)
    if plek is None:
        log("    okapi-dal: geen plek gevonden")
        return
    cx, cz = plek
    g = int(np.median(park.hoogte[cx - 8:cx + 9, cz - 8:cz + 9]))
    ruim(park, cx, cz, 13, extra=2)
    # the narrow way in: towards the nearest path
    pd = park._pad_d
    xs, zs = np.nonzero(park.pad[cx - 40:cx + 41, cz - 40:cz + 41])
    if len(xs):
        i = int(np.argmin(np.hypot(xs - 40, zs - 40)))
        tx, tz = cx - 40 + int(xs[i]), cz - 40 + int(zs[i])
    else:
        tx, tz = cx - 30, cz
    a = math.atan2(tz - cz, tx - cx)
    # bowl: 3 lower in the middle
    for dx in range(-9, 10):
        for dz in range(-9, 10):
            d = math.hypot(dx, dz) / 9
            if d > 1:
                continue
            nh = int(round(g - 3 * (1 - d * d)))
            zet_hoogte(park, cx + dx, cz + dz, nh, top="moss_block" if RNG.random() < 0.5 else "podzol",
                       sub="rooted_dirt")
    # the wall: leaves and mossy boulders on a ring 9.5..12.5, one 2-wide gap facing the path
    for dx in range(-13, 14):
        for dz in range(-13, 14):
            d = math.hypot(dx, dz)
            if not (9.5 <= d <= 12.5):
                continue
            hoek = math.atan2(dz, dx)
            if abs((hoek - a + math.pi) % (2 * math.pi) - math.pi) * d < 1.6:
                continue
            x, z = cx + dx, cz + dz
            hh = int(park.hoogte[x, z])
            if RNG.random() < 0.15:
                for y in range(hh + 1, hh + 3):
                    park.set(x, y, z, "minecraft:mossy_cobblestone")
                park.set(x, hh + 3, z, "minecraft:moss_block")
            else:
                for y in range(hh + 1, hh + 4 + int(RNG.integers(0, 2))):
                    park.set(x, y, z, park.leaves("jungle"))
    # narrow path from the gap to the main path
    for t in range(9, int(math.hypot(tx - cx, tz - cz)) + 1):
        x = int(round(cx + math.cos(a) * t))
        z = int(round(cz + math.sin(a) * t))
        if park.pad[x, z] or park.water[x, z]:
            break
        hh = int(park.hoogte[x, z])
        park.set(x, hh, z, "minecraft:rooted_dirt")
        for y in range(hh + 1, hh + 4):
            s = park.get(x, y, z)
            if "leaves" in s or "grass" in s or "fern" in s or "bamboo" in s:
                park.set(x, y, z, "minecraft:air")
        # dense on both sides
        for s in (-1, 1):
            xx = int(round(x - math.sin(a) * s * 1.5))
            zz = int(round(z + math.cos(a) * s * 1.5))
            if park.v[xx, int(park.hoogte[xx, zz]) + 1, zz] == 0:
                park.set(xx, int(park.hoogte[xx, zz]) + 1, zz, park.leaves("jungle"))
                park.set(xx, int(park.hoogte[xx, zz]) + 2, zz, park.leaves("jungle"))
    # very dense inside: large ferns, ferns, a few small jungle trees, a tiny pool
    for dx in range(-9, 10):
        for dz in range(-9, 10):
            if math.hypot(dx, dz) > 9:
                continue
            x, z = cx + dx, cz + dz
            hh = int(park.hoogte[x, z])
            if park.v[x, hh + 1, z] != 0:
                continue
            r = RNG.random()
            if r < 0.45:
                park.set(x, hh + 1, z, state("large_fern", half="lower"))
                park.set(x, hh + 2, z, state("large_fern", half="upper"))
            elif r < 0.75:
                park.set(x, hh + 1, z, "minecraft:fern")
            elif r < 0.82:
                park.set(x, hh + 1, z, "minecraft:moss_carpet")
    for k in range(4):
        aa = RNG.random() * 2 * math.pi
        x, z = int(cx + math.cos(aa) * 5), int(cz + math.sin(aa) * 5)
        top = park.boom("jungle", x, int(park.hoogte[x, z]), z, grootte=0.6)
        park.bomen.append((x, top, z, "jungle"))
    for dx in range(-1, 2):
        for dz in range(-1, 1):
            x, z = cx + dx + 2, cz + dz - 2
            hh = int(park.hoogte[x, z])
            for y in range(hh + 1, hh + 3):
                park.set(x, y, z, "minecraft:air")
            park.set(x, hh, z, state("water", level=0))
    PLAATS["okapi_dal"] = (cx, int(park.hoogte[cx, cz]) + 1, cz)
    log(f"    okapi-dal bij ({cx},{cz}) (reservering {ox},{oz}), toegang richting ({tx},{tz})")


def uitkijk_en_hut(park, log):
    vermijd = [(x, z, r + 8) for (x, z, r, zone) in kit.RESERVERINGEN.values()]
    t = zoek(park, "regenwoud", (520, 330), 140, hek=7, pad=(3, 7), zonder=vermijd, dicht=2)
    if t:
        dek = toren(park, t[0], t[1], hout="jungle", hoogte=13, dak="jungle", gezellig=True)
        PLAATS["uitkijktoren"] = (t[0], dek + 1, t[1])
        PLAATS["toren_kijkplek"] = park.kijkplekken[-1]
        log(f"    uitkijktoren bij {t}, dek y{dek}")
    # test round 2: a real viewing hut at the river (one-way glass, raised floor), looking over deeper clear water to
    # the far bank, where toekans and luiaards get trees of their own (the old hide looked at a mud bank)
    from .zone_ijskust import kijkhut
    h = zoek(park, "regenwoud", (470, 378), 140, hek=7, pad=(4, None), water=(3, 10), zonder=vermijd, dicht=2)
    if h:
        richtingen = {(0, -1): "north", (0, 1): "south", (-1, 0): "west", (1, 0): "east"}

        def score(k):                       # most water in a cone of 6..22 blocks, then land again (the far bank)
            dx, dz = k
            w = sum(park.water[h[0] + dx * t + q * -dz, h[1] + dz * t + q * dx]
                    for t in range(6, 23) for q in range(-3, 4))
            return w
        k = max(richtingen, key=score)
        F, oog, punten = kijkhut(park, h[0], h[1], kijk=k, hout="jungle", verhoog=1, kegel=24,
                                 bodem="minecraft:sand", oever_blok="minecraft:coarse_dirt", blad="jungle", log=log)
        PLAATS["schuilhut"] = (h[0], F + 1, h[1])
        PLAATS["kijkhut_kijk"] = (k, punten)
        # round 3: cosy inside (the back row behind the chairs): a guh lamp, cushions, a flower pot; the viewpoint
        kx, kz = k
        wx_, wz_ = -kz, kx

        def Wc(u, w):
            return h[0] + u * kx + w * wx_, h[1] + u * kz + w * wz_
        lx, lz = Wc(-1, -2)
        park.lamp(lx, F + 1, lz, GUHLAMP)
        for w in (-1, 1):
            cx_, cz_ = Wc(-1, w)
            kussen(park, cx_, F + 1, cz_)
        bx_, bz_ = Wc(-1, 2)
        bloempot(park, bx_, F + 1, bz_)
        sx, sz = Wc(0, -1)
        PLAATS["kijkhut_plek"] = park.kijkplek(f"kijkhut_regenwoud_{h[0]}_{h[1]}", "regenwoud",
                                               (Wc(1, 0)[0] + 0.5, F + 2.6, Wc(1, 0)[1] + 0.5), (kx, kz), (sx, F + 1, sz))
        # trees on the far bank, in the view: the first land beyond the water along the view axis
        vx, vz = h
        oever = None
        for t in range(8, 40):
            x, z = vx + k[0] * t, vz + k[1] * t
            if park.water[x - k[0], z - k[1]] and not park.water[x, z]:
                oever = t
                break
        bomen = []
        if oever:
            for (t, q) in ((oever + 3, -4), (oever + 4, 3), (oever + 6, 0)):
                x, z = vx + k[0] * t - k[1] * q, vz + k[1] * t + k[0] * q
                if (0 <= x < kit.W and 0 <= z < kit.W and not park.water[x, z]
                        and park.zone[x, z] == ZONES.index("regenwoud") and park.hek_afstand[x, z] > 5):
                    ruim(park, x, z, 2.5, extra=0)
                    top = park.boom("jungle", x, int(park.hoogte[x, z]), z, grootte=0.55)
                    park.bomen.append((x, top, z, "jungle"))
                    bomen.append((x, int(park.hoogte[x, z]) + 1, z))
        PLAATS["kijkhut_bomen"] = bomen
        kp = PLAATS["kijkhut_plek"]
        for i, (x, y, z) in enumerate(bomen):
            park.favoriet(f"kijkboom_{i}", kp, (x, y, z), soorten=("luiaard",) if i == 1 else ("toekan", "luiaard"),
                          straal=3)
        if oever:
            fx, fz = vx + k[0] * (oever + 1), vz + k[1] * (oever + 1)
            park.favoriet("overkant", kp, (fx, int(park.hoogte[fx, fz]) + 1, fz), soorten=("orang_oetan", "toekan"),
                          straal=4)
        log(f"    kijkhut bij {h}, kijkt {richtingen[k]}, overkant {oever}, {len(bomen)} kijkbomen")


def bouw(park, log=print):
    log("  regenwoud:")
    # (round 3: the river pipeline (terrein.py + kit.brug + controle.py) owns water and bridges)
    park._water_d = park._pad_d = None
    waterval_grot(park, log)
    reuzenboom(park, log)
    ruine(park, log)
    okapi_dal(park, log)
    uitkijk_en_hut(park, log)
    vermijd = [(x, z, r + 6) for (x, z, r, zone) in kit.RESERVERINGEN.values()]
    vermijd += [(v[0], v[2], 12) for k, v in PLAATS.items() if isinstance(v, tuple) and len(v) == 3]
    ontplaat(park, "regenwoud", vermijd, log)
    # favourites round the tower: the ruin top (gekko's), the reuzenboom ring (orang-oetans), a jungle clearing
    kp = PLAATS.get("toren_kijkplek")
    if kp is not None:
        tx, _, tz = PLAATS["uitkijktoren"]
        for naam, sleutel, soorten in (("ruine", "ruine_top", ("gekko",)), ("reuzenboom", "reuzenboom", ("orang_oetan",
                                                                                                          "toekan"))):
            if sleutel in PLAATS and math.hypot(PLAATS[sleutel][0] - tx, PLAATS[sleutel][2] - tz) <= 60:
                park.favoriet(naam, kp, PLAATS[sleutel], soorten=soorten, straal=5)
        # a jungle clearing right below the tower (14..24 out, away from the path): toekans and orang-oetans
        best = None
        for k in range(16):
            a = k * math.pi / 8
            for d in (18, 14, 22):
                x, z = int(round(tx + math.cos(a) * d)), int(round(tz + math.sin(a) * d))
                if not (4 <= x < kit.W - 4 and 4 <= z < kit.W - 4) or park.zone[x, z] != ZONES.index("regenwoud"):
                    continue
                if park.water[x - 2:x + 3, z - 2:z + 3].any() or park.pad[x - 2:x + 3, z - 2:z + 3].any() or                         park.hek_afstand[x, z] < 6:
                    continue
                best = (x, z)
                break
            if best:
                break
        if best:
            x, z = best
            ruim(park, x, z, 3, extra=0)
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    hh = int(park.hoogte[x + dx, z + dz])
                    if math.hypot(dx, dz) <= 3 and park.v[x + dx, hh + 1, z + dz] == 0 and RNG.random() < 0.35:
                        park.set(x + dx, hh + 1, z + dz, "minecraft:fern")
            PLAATS["toren_favoriet"] = (x, int(park.hoogte[x, z]) + 1, z)
            park.favoriet("open_plek", kp, PLAATS["toren_favoriet"], soorten=("toekan", "orang_oetan"), straal=4)
    vuurvliegjes(park, 470, 378, 30, 10, zone="regenwoud")
    if "reuzenboom" in PLAATS:
        vuurvliegjes(park, PLAATS["reuzenboom"][0], PLAATS["reuzenboom"][2], 20, 8, zone="regenwoud")


def _zet(p, xyz, straal=None):
    p["centrum"] = [int(xyz[0]), int(xyz[1]), int(xyz[2])]
    if straal is not None:
        p["straal"] = straal


def plekken(park, plekken):
    """Homes on the landmarks: glimwormpjes in the cave, orang-oetans at the reuzenboom, gekko's on the ruin, the
    okapi in its valley. (Gibbons live in the bamboe zone: zone_bamboe puts them at the hangbrug.)"""
    n = {}
    for p in plekken:
        s = p["soort"]
        i = n.get(s, 0)
        n[s] = i + 1
        if s == "glimworm" and "grot" in PLAATS:
            x, y, z = PLAATS["grot"]
            _zet(p, (x + (2 if i else -2), y, z + (1 if i else -1)), 4)
        elif s == "orang_oetan" and "reuzenboom" in PLAATS:
            x, _, z = PLAATS["reuzenboom"]
            gx, gz = x + (6 if i == 0 else -5), z + (2 if i == 0 else 5)
            _zet(p, (gx, int(park.hoogte[gx, gz]) + 1, gz), 12)
        elif s == "gekko" and "ruine_top" in PLAATS:
            x, y, z = PLAATS["ruine_top"]
            _zet(p, (x + (2 if i == 0 else -2), y, z), 7)
        elif s == "okapi" and "okapi_dal" in PLAATS:
            _zet(p, PLAATS["okapi_dal"], 7)
    if "toren_favoriet" in PLAATS:              # round 3: toekans in view of the tower
        x, y, z = PLAATS["toren_favoriet"]
        plekken.append({"soort": "toekan", "zone": "regenwoud", "centrum": [int(x), int(y), int(z)], "straal": 6,
                        "volwassenen": 2, "jongen": 1, "water": False, "boom": True})
    # test round 2: animals in the view of the viewing hut (toekans and a luiaard in the trees on the far bank)
    bomen = PLAATS.get("kijkhut_bomen") or []
    for i, (x, y, z) in enumerate(bomen):
        soort = "luiaard" if i == 1 else "toekan"
        plekken.append({"soort": soort, "zone": "regenwoud", "centrum": [int(x), int(y), int(z)],
                        "straal": 5 if soort == "luiaard" else 8, "volwassenen": 2, "jongen": 1,
                        "water": False, "boom": True})
