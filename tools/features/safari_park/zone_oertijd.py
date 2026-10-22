"""
Zone module Oerijstijd-vallei (ontwerp 1.4): the landmarks of the ice-age valley, built on the base terrain.

  ijsgrot            an ice cave in the hill: the fossiel on the floor (geheimpje spot) and grotschilderingen
                     (guhs riding a mammoth) on the back wall, softly lit by glow lichen
  bevroren waterval  a rock massif with a frozen waterfall; behind the ice the sabeltandtijger cave (side entrance)
  warme bronnen      bubbling pools (magma under the water) with steam vents (campfires in little stone chimneys)
  toendravlakte      tufts, berries and stones on the open snowfield
  ingevroren vondsten  things frozen in clear ice blocks: a guh plush, a little sled, a 'sabeltand-tandenborstel'
  mammoet-wolhoop    a heap of brown wool the mammoth calves roll in
  kampje van de oerguh  a stone ring, an unlit fire, a fur rug, a cooking pot, a stone axe
  uitkijktoren + schuilhut
plekken(park, plekken) moves the sabeltandtijgers into their cave and the mammoths to the springs / wool heap.
"""
import math

import numpy as np

from . import kit
from .terrein import ZONES
from .zone_ijskust import (blob, egaliseer, grond, kolom_top_vrij, mag_bouwen, ruim_kolom, schuilhut, toren,
                           zet_plek, zoek)

ZONE = "oertijd"


def _bekleed(park, punten_lucht, rng, keuze=(("minecraft:packed_ice", 6), ("minecraft:blue_ice", 2),
                                              ("minecraft:snow_block", 1), ("minecraft:stone", 1))):
    """Every solid block touching one of the carved air cells becomes ice (the cave lining)."""
    namen = [k for k, _ in keuze]
    w = np.array([g for _, g in keuze], float)
    w /= w.sum()
    lucht = set(punten_lucht)
    for (x, y, z) in punten_lucht:
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in lucht or not park.inb(*n):
                continue
            if park.v[n] != 0 or dy == 1:            # a missing roof block becomes ice too
                park.set(*n, namen[int(rng.choice(len(namen), p=w))])
                boven = (n[0], n[1] + 1, n[2])
                if dy == 1 and park.inb(*boven) and park.v[boven] == 0 and boven not in lucht:
                    park.set(*boven, "minecraft:snow_block")      # a roof that sticks out gets a snow cap


def ijsgrot(park, rng):
    """Round 3 (rebuilt): the ice cave with the big fossil as a closed, self-contained snowy dome hill (the user: an
    'iglo with a big bone' without walls, you could fall off the map / see the void). It stands only where the whole
    footprint is inside the zone, away from fences and paths; the shell is solid all round (snow outside, packed and
    blue ice inside), the floor solid down to the ground, one entrance tunnel toward the nearest path.
    Inside: a mammoth skeleton of bone blocks (one skeleton skull: the geheimpje spot), cave paintings on the back
    wall, ice teeth (packed ice), glow lichen and lanterns (only packed / blue ice near the lights: nothing melts)."""
    cx, cz, _, _ = kit.RESERVERINGEN["ijsgrot_fossiel"]
    c = zoek(park, ZONE, (cx, cz), r=12, max_helling=12, hek_marge=6, pad_marge=2, max_r=50)
    if c is None:
        c = zoek(park, ZONE, (cx, cz), r=10, max_helling=14, hek_marge=5, pad_marge=1, max_r=70)
    if c is None:
        return None, None
    cx, cz = c
    R, RY = 11.0, 9.0                      # outer half axes
    ri, riy = 7.6, 5.8                     # chamber
    hs = park.hoogte[cx - 12:cx + 13, cz - 12:cz + 13]
    vloer = int(np.percentile(hs, 30))
    # the entrance: the cardinal direction with the nearest path
    pd = _pad_afst(park)
    beste = None
    for d4 in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        x, z = cx + d4[0] * 14, cz + d4[1] * 14
        if 0 <= x < kit.W and 0 <= z < kit.W and (beste is None or pd[x, z] < beste[0]):
            beste = (pd[x, z], d4)
    ux, uz = beste[1]
    kies = [("minecraft:packed_ice", 6), ("minecraft:blue_ice", 2), ("minecraft:snow_block", 1)]
    namen = [k for k, _ in kies]
    w = np.array([g for _, g in kies], float)
    w /= w.sum()
    for x in range(cx - 12, cx + 13):
        for z in range(cz - 12, cz + 13):
            dxz = math.hypot(x - cx, z - cz)
            if dxz > R + 0.5:
                continue
            g = int(park.hoogte[x, z])
            top = vloer + int(RY * math.sqrt(max(0.0, 1 - (dxz / R) ** 2)))
            ruim_kolom(park, x, z, g + 1, max(top, g) + 14)
            for y in range(min(g, vloer) - 2, top + 1):
                d_in = ((x - cx) / ri) ** 2 + ((y - vloer - 1) / riy) ** 2 + ((z - cz) / ri) ** 2
                if y > vloer and d_in < 1:
                    park.set(x, y, z, "minecraft:air")
                elif y >= top - 1:
                    park.set(x, y, z, "minecraft:snow_block")
                elif y > vloer and d_in < 1.55:
                    park.set(x, y, z, namen[int(rng.choice(len(namen), p=w))])
                elif y > vloer:
                    park.set(x, y, z, "minecraft:snow_block" if y > top - 3 else "minecraft:stone")
                else:
                    park.set(x, y, z, "minecraft:snow_block" if (y == vloer and dxz <= ri) else "minecraft:stone")
            park.hoogte[x, z] = max(top, g)
    # the entrance tunnel 3 wide, 3 high, lined with packed ice, with an ice arch at the mouth
    for t in range(int(ri) - 1, int(R) + 3):
        for w_ in range(-2, 3):
            x, z = cx + ux * t - uz * w_, cz + uz * t + ux * w_
            g = int(park.hoogte[x, z])
            for y in range(vloer + 1, vloer + 5):
                if abs(w_) == 2 or y == vloer + 4:
                    if t > R:
                        park.set(x, y, z, "minecraft:packed_ice")
                    elif park.v[x, y, z] == 0:
                        park.set(x, y, z, "minecraft:packed_ice")
                else:
                    park.set(x, y, z, "minecraft:air")
            if abs(w_) < 2:
                park.set(x, vloer, z, "minecraft:snow_block")
                for y in range(min(g, vloer) - 2, vloer):
                    park.set(x, y, z, "minecraft:stone")
    # round 5: a snow staircase from the tunnel mouth down to the ground in front (the cave floor lay ~4 above it:
    # unreachable in survival); 3 wide, a low snow wall on both sides, its foot where it meets the ground
    naar = {(1, 0): "west", (-1, 0): "east", (0, 1): "north", (0, -1): "south"}[(ux, uz)]     # up = toward the cave
    t = int(R) + 3
    y = vloer
    while t < int(R) + 16:
        xs = [(cx + ux * t - uz * w_, cz + uz * t + ux * w_) for w_ in range(-2, 3)]
        g = max(int(park.hoogte[x, z]) for (x, z) in xs[1:4])
        if y <= g:
            break
        for k, (x, z) in enumerate(xs):
            g_ = int(park.hoogte[x, z])
            for yy in range(min(g_, y) - 1, y):
                park.set(x, yy, z, "minecraft:snow_block")
            if k in (0, 4):
                park.set(x, y, z, "minecraft:snow_block")
                ruim_kolom(park, x, z, y + 1, y + 6)
            else:
                park.set(x, y, z, f"minecraft:spruce_stairs[facing={naar},half=bottom,shape=straight,waterlogged=false]")
                ruim_kolom(park, x, z, y + 1, y + 6)
                park.hoogte[x, z] = max(int(park.hoogte[x, z]), y)
        t += 1
        y -= 1
    # a ramp from the stairs' foot to the nearest path
    mx, mz = cx + ux * t, cz + uz * t
    try:
        from .zone_savanne import paadje
        paadje(park, (mx + ux, mz + uz), max_r=40, blok="dirt_path")
    except Exception:
        pass
    # the skeleton: spine along the cave axis perpendicular to the entrance, ribs, legs, skull with tusks
    ax, az = -uz, ux
    for k in range(-4, 5):
        x, z = cx + ax * k, cz + az * k
        park.set(x, vloer + 3, z, "minecraft:bone_block[axis=" + ("x" if ax else "z") + "]")
        if k % 2 == 0 and abs(k) <= 3:
            for s in (-1, 1):
                for h in (1, 2):
                    park.set(x + ux * s * (1 if h == 2 else 2), vloer + 1 + h, z + uz * s * (1 if h == 2 else 2),
                             "minecraft:bone_block[axis=y]")
    for k in (-3, 3):
        for s in (-1, 1):
            for y in (vloer + 1, vloer + 2):
                park.set(cx + ax * k + ux * s, y, cz + az * k + uz * s, "minecraft:bone_block[axis=y]")
    kx, kz = cx + ax * 5, cz + az * 5
    park.set(kx, vloer + 3, kz, "minecraft:bone_block[axis=y]")
    rot = {(1, 0): 12, (-1, 0): 4, (0, 1): 0, (0, -1): 8}[(ax, az)]
    park.set(kx, vloer + 4, kz, f"minecraft:skeleton_skull[rotation={rot},powered=false]")
    for s in (-1, 1):                         # the tusks curling forward
        park.set(kx + ax + ux * s, vloer + 3, kz + az + uz * s, "minecraft:bone_block[axis=" + ("x" if ax else "z") + "]")
        park.set(kx + 2 * ax + ux * s, vloer + 4, kz + 2 * az + uz * s, "minecraft:bone_block[axis=y]")
    # ice teeth (packed ice) from the ceiling
    for _ in range(14):
        x, z = cx + int(rng.integers(-6, 7)), cz + int(rng.integers(-6, 7))
        for y in range(vloer + 9, vloer + 1, -1):
            if park.v[x, y, z] != 0 and park.v[x, y - 1, z] == 0:
                for d in range(1, int(rng.integers(1, 3)) + 1):
                    if park.v[x, y - d, z] == 0 and y - d > vloer + 4:
                        park.set(x, y - d, z, "minecraft:packed_ice")
                break
    # cave paintings on the back wall (opposite the entrance)
    plaatje = ["....oo......", "...oooo.....", ".bbbbbbbb...", "bbbbbbbbbb..", "bbbbbbbbbbbw", "bbbbbbbbbb.w",
               ".bb.bb.bb...", ".bb.bb.bb..."]
    kleur = {"b": "minecraft:brown_terracotta", "o": "minecraft:pink_terracotta", "w": "minecraft:white_terracotta"}
    for i, rij in enumerate(plaatje[::2]):
        y = vloer + 5 - i
        for j, ch in enumerate(rij):
            if ch == ".":
                continue
            off = (j - len(rij) / 2) * 0.8
            for t in range(1, 12):
                x = int(round(cx - ux * t + ax * off))
                z = int(round(cz - uz * t + az * off))
                if park.v[x, y, z] != 0:
                    park.set(x, y, z, kleur[ch])
                    break
    # light: glow lichen on the floor along the walls, two hanging lanterns, one at the mouth
    for _ in range(18):
        x, z = cx + int(rng.integers(-6, 7)), cz + int(rng.integers(-6, 7))
        if park.v[x, vloer + 1, z] == 0 and park.v[x, vloer, z] != 0 and math.hypot(x - cx, z - cz) > 4:
            park.set(x, vloer + 1, z, park.LAMP["glow_lichen"])
    for s in (-3, 3):
        x, z = cx + ux * s, cz + uz * s
        for y in range(vloer + 9, vloer + 4, -1):
            if park.v[x, y, z] != 0 and park.v[x, y - 1, z] == 0:
                park.lamp(x, y - 1, z, "lantaarn_hangend")
                break
    park.lamp(mx - ux, vloer + 1, mz - uz + (1 if ux else 0) - 0, "lantaarn")
    return (cx, vloer + 1, cz), (mx, vloer + 1, mz)


def bevroren_waterval(park, rng):
    cx, cz, _, _ = kit.RESERVERINGEN["bevroren_waterval"]
    g = int(np.median(park.hoogte[cx - 6:cx + 7, cz - 6:cz + 7]))
    top = g + 20
    # the massif: north of the centre (z < cz), rounded
    for x in range(cx - 9, cx + 10):
        for z in range(cz - 12, cz - 1):
            if not mag_bouwen(park, x, z, 5):
                continue
            rx = 9 - max(0, (cz - 6 - z) ** 2 / 9)
            if abs(x - cx) > rx:
                continue
            h = top - int(abs(x - cx) * 0.6) - max(0, z - (cz - 4)) * 2
            for y in range(grond(park, x, z) - 1, h + 1):
                park.set(x, y, z, "minecraft:stone" if rng.random() < 0.8 else "minecraft:andesite")
            park.set(x, h + 1, z, "minecraft:snow_block")
            park.hoogte[x, z] = h + 1
    # the cave behind the ice: x cx-3..cx+3, z cz-8..cz-3, 4 high
    lucht = []
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 8, cz - 2):
            for y in range(g + 1, g + 5):
                if ((x - cx) / 3.6) ** 2 + ((z - (cz - 5)) / 3.4) ** 2 <= 1.0 or z == cz - 3:
                    lucht.append((x, y, z))
    # side entrance from the east
    for x in range(cx + 3, cx + 10):
        for y in range(g + 1, g + 4):
            lucht.append((x, y, cz - 5))
            lucht.append((x, y, cz - 6))
    lucht = list(dict.fromkeys(lucht))
    for p in lucht:
        park.set(*p, "minecraft:air")
    _bekleed(park, lucht, rng, (("minecraft:packed_ice", 3), ("minecraft:stone", 3), ("minecraft:andesite", 1)))
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 8, cz - 2):
            if park.v[x, g + 1, z] == 0:
                park.set(x, g, z, "minecraft:snow_block")
    # the frozen fall in front of the mouth, from the top to the ground, a little wider than the mouth
    for x in range(cx - 4, cx + 5):
        for y in range(g + 1, top + 1):
            z = cz - 2
            breed = 4 if y < top - 3 else 3
            if abs(x - cx) <= breed:
                park.set(x, y, z, "minecraft:ice" if (x + y) % 3 else "minecraft:blue_ice")
                if abs(x - cx) <= breed - 2 and rng.random() < 0.4:
                    park.set(x, y, z + 1, "minecraft:packed_ice")
    # the frozen pool at the foot
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 1, cz + 5):
            if math.hypot((x - cx) / 1.3, z - (cz + 1)) <= 4 and mag_bouwen(park, x, z, 4):
                park.set(x, grond(park, x, z), z, "minecraft:ice")
                ruim_kolom(park, x, z, grond(park, x, z) + 1, grond(park, x, z) + 6)
    # a little glow inside
    park.set(cx, g + 1, cz - 6, "minecraft:glow_lichen[down=true,east=false,north=false,south=false,up=false,"
                                "waterlogged=false,west=false]")
    return (cx, g + 1, cz - 5)


def warme_bronnen(park, rng):
    """Round 3 (bigger, wilder): a terraced field of steaming pools (calcite and dripstone rims stepping down a slope,
    magma under the deepest middles: bubbling), steam vents, pale rocks; light from a few lanterns on posts."""
    cx, cz, _, _ = kit.RESERVERINGEN["warme_bronnen"]
    # round 3: the field lies on dry ground well away from the river (it used to overflow into it)
    from .zone_ijskust import _water_afstand
    c = zoek(park, ZONE, (cx, cz), r=13, max_helling=5, hek_marge=5, pad_marge=3, max_r=50) or         zoek(park, ZONE, (cx, cz), r=10, max_helling=6, hek_marge=5, pad_marge=2, max_r=60)
    if c:
        cx, cz = c
    park._oertijd_bronnen_c = (cx, cz)
    from .zone_ijskust import _ruim_bomen
    _ruim_bomen(park, cx, cz, 17)                 # an open field: the kijkhut must see the springs and the mammoths
    wd = _water_afstand(park)
    poelen = []
    for (ox, oz, r) in ((0, 0, 3.6), (8, 4, 2.8), (-6, 7, 2.6), (3, 11, 2.2), (-10, -2, 2.4), (11, -5, 2.0)):
        x0, z0 = cx + ox, cz + oz
        if not mag_bouwen(park, x0, z0, 6) or wd[x0, z0] < r + 5:
            continue
        g = grond(park, x0, z0)
        for x in range(int(x0 - r) - 2, int(x0 + r) + 3):
            for z in range(int(z0 - r) - 2, int(z0 + r) + 3):
                d = math.hypot(x - x0, z - z0)
                if d > r + 1.5 or not mag_bouwen(park, x, z, 5) or wd[x, z] < 4:
                    continue
                ruim_kolom(park, x, z, g + 1, g + 8)
                for y in range(g - 4, g + 1):
                    if park.v[x, y, z] == 0:
                        park.set(x, y, z, "minecraft:stone")
                park.hoogte[x, z] = g
                if d <= r:
                    diep = 2 if d < r - 1 else 1
                    park.set(x, g - diep - 1, z, "minecraft:magma_block" if diep == 2 and d < r - 1.2
                             else "minecraft:calcite")
                    bubbel = diep == 2 and d < r - 1.2
                    for y in range(g - diep, g + 1):
                        park.set(x, y, z, "minecraft:bubble_column[drag=true]" if bubbel else "minecraft:water")
                else:
                    park.set(x, g, z, "minecraft:calcite" if rng.random() < 0.6 else (
                        "minecraft:dripstone_block" if rng.random() < 0.6 else "minecraft:smooth_basalt"))
                    if d > r + 0.6 and rng.random() < 0.18:
                        park.set(x, g + 1, z, "minecraft:calcite")
        poelen.append((x0, g, z0))
    for (ox, oz) in ((4, -4), (-5, 2), (12, 0), (-2, 14), (-12, 6)):
        x, z = cx + ox, cz + oz
        if not mag_bouwen(park, x, z, 6) or wd[x, z] < 4:
            continue
        g = grond(park, x, z)
        ruim_kolom(park, x, z, g + 1, g + 6)
        park.set(x, g, z, "minecraft:hay_block[axis=y]")
        park.set(x, g + 1, z, "minecraft:campfire[facing=north,lit=true,signal_fire=true,waterlogged=false]")
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if dx or dz:
                    for y in (g + 1, g + 2):
                        park.set(x + dx, y, z + dz, "minecraft:mossy_cobblestone" if rng.random() < 0.5
                                 else "minecraft:cobblestone")
    return poelen


def toendra_en_vondsten(park, rng, bij):
    """Tufts, berries and boulders around `bij`, plus three things frozen in clear ice."""
    zi = ZONES.index(ZONE)
    bx, bz = bij
    for k in range(260):
        x = bx + int(rng.integers(-28, 29))
        z = bz + int(rng.integers(-28, 29))
        if park.zone[x, z] != zi or not mag_bouwen(park, x, z, 3) or park.water[x, z]:
            continue
        g = grond(park, x, z)
        if park.v[x, g + 1, z] not in (0,) and "snow" not in park.get(x, g + 1, z):
            continue
        p = rng.random()
        if p < 0.45:
            park.set(x, g + 1, z, "minecraft:short_grass")
        elif p < 0.6:
            park.set(x, g + 1, z, "minecraft:sweet_berry_bush[age=3]")
        elif p < 0.75:
            park.set(x, g + 1, z, "minecraft:dead_bush")
        elif p < 0.82:
            park.set(x, g, z, "minecraft:moss_block")
        elif p < 0.86:
            park.set(x, g + 1, z, "minecraft:cobblestone")
    vondsten = []
    inhoud = [
        ["minecraft:pink_wool", "minecraft:pink_wool"],                        # een guh-knuffel (roze bolletje)
        ["minecraft:oak_slab[type=bottom,waterlogged=false]", "minecraft:spruce_fence"],   # een sleetje
        ["minecraft:bone_block[axis=y]", "minecraft:white_wool"],             # sabeltand-tandenborstel
    ]
    for i, ding in enumerate(inhoud):
        doel = (bx + (-10, 0, 10)[i], bz + (8, -12, 6)[i])
        c = zoek(park, ZONE, doel, r=2, max_helling=2, max_r=25)
        if c is None:
            continue
        x, z = c
        g = grond(park, x, z)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                ruim_kolom(park, x + dx, z + dz, g + 1, g + 6)
                for y in range(g + 1, g + 4):
                    park.set(x + dx, y, z + dz, "minecraft:ice")
        if i == 1:
            park.set(x - 1, g + 2, z, ding[0])
            park.set(x, g + 2, z, ding[0])
            park.set(x + 1, g + 2, z, ding[1])
        else:
            park.set(x, g + 2, z, ding[0])
            park.set(x, g + 3, z, ding[1])
        vondsten.append((x, z))
    return vondsten


def wolhoop(park, rng, doel):
    c = zoek(park, ZONE, doel, r=5, max_helling=3, max_r=30)
    if c is None:
        return None
    x, z = c
    g = grond(park, x, z)
    blob(park, x, g + 1, z, 4.6, 2.3, 3.6, [("minecraft:brown_wool", 6), ("minecraft:light_gray_wool", 1),
                                             ("minecraft:white_wool", 1), ("minecraft:hay_block[axis=y]", 1)], rng,
         alleen_lucht=True)
    for (ox, oz) in ((5, 3), (-5, -2), (2, -5)):            # loose tufts round it
        if mag_bouwen(park, x + ox, z + oz, 4):
            park.set(x + ox, grond(park, x + ox, z + oz) + 1, z + oz, "minecraft:brown_carpet")
    return x, z


def kampje(park, rng, doel):
    c = zoek(park, ZONE, doel, r=3, max_helling=2, max_r=30)
    if c is None:
        return None
    x, z = c
    g = grond(park, x, z)
    egaliseer(park, x, z, 3, g, top="minecraft:gravel")
    for a in range(0, 360, 45):
        rx, rz = int(round(x + math.cos(math.radians(a)) * 1.5)), int(round(z + math.sin(math.radians(a)) * 1.5))
        park.set(rx, g + 1, rz, "minecraft:cobblestone_slab[type=bottom,waterlogged=false]")
    park.set(x, g + 1, z, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    park.set(x + 3, g + 1, z, "minecraft:brown_carpet")           # het vachtje
    park.set(x + 3, g + 1, z + 1, "minecraft:brown_carpet")
    park.set(x - 3, g + 1, z, "minecraft:cauldron")               # het kookpotje
    park.set(x, g + 1, z - 3, "minecraft:stripped_spruce_log[axis=x]")      # zitboomstam
    park.set(x + 1, g + 1, z - 3, "minecraft:stripped_spruce_log[axis=x]")
    park.set(x - 2, g + 1, z + 2, "minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]")
    park.set(x - 2, g + 2, z + 2, "minecraft:stone_button[face=floor,facing=north,powered=false]")   # stenen bijl
    return x, z


def bouw(park, log=print):
    from .zone_ijskust import kijkhut, oostrand, pad_lampen, veilige_lamp
    rng = np.random.default_rng(kit.SEED + 601)
    park._oertijd = {}
    # (round 3: the river pipeline (terrein.py + kit.brug + controle.py) owns water and bridges)
    park._oertijd["grot"], ingang = ijsgrot(park, rng)
    park._oertijd["waterval"] = bevroren_waterval(park, rng)
    park._oertijd["bronnen"] = warme_bronnen(park, rng)
    bx, bz = park._oertijd_bronnen_c
    park._oertijd["wolhoop"] = wolhoop(park, rng, (bx + 18, bz - 6))
    park._oertijd["kampje"] = kampje(park, rng, (bx - 4, bz - 22))
    park._oertijd["vondsten"] = toendra_en_vondsten(park, rng, (bx + 30, bz + 14))
    gx, gz, _, _ = kit.RESERVERINGEN["ijsgrot_fossiel"]
    t = zoek(park, ZONE, (gx + 30, gz + 20), r=3, max_helling=4, max_r=40)
    if t:
        toren(park, *t, hout="spruce")
        veilige_lamp(park, t[0] + 3, grond(park, t[0] + 3, t[1]) + 1, t[1], "guhlamp")
    # round 3: a real viewing hut (one-way glass, raised floor, guh chairs) instead of the slit hide, on dry ground
    # west of the springs, looking at the springs and the wool heap
    doel_x = park._oertijd["wolhoop"][0] if park._oertijd["wolhoop"] else bx
    h = None
    def vrij(c):
        if c is None or math.hypot(c[0] - bx, c[1] - bz) < 18:
            return False
        for x in range(c[0] - 6, c[0] + 7):
            for z in range(c[1] - 6, c[1] + 7):
                g = int(park.hoogte[x, z])
                if any(w in park.get(x, y, z) for y in (g, g + 1) for w in ("water", "bubble", "calcite", "magma")):
                    return False
        return True
    h = None
    for rr in (20, 24, 28, 33):
        for hoek in range(0, 360, 20):
            doel = (int(bx + math.cos(math.radians(hoek)) * rr), int(bz + math.sin(math.radians(hoek)) * rr))
            if not (8 <= doel[0] < kit.W - 8 and 8 <= doel[1] < kit.W - 8):
                continue
            c = zoek(park, ZONE, doel, r=5, max_helling=4, hek_marge=6, pad_marge=2, max_r=5)
            if vrij(c):
                h = c
                break
        if h:
            break
    hut = None
    if h:
        from .zone_ijskust import _kardinaal
        kijk = _kardinaal(bx - h[0], bz - h[1])
        hut = kijkhut(park, *h, kijk=kijk, hout="spruce", verhoog=2, kegel=24, bodem="minecraft:gravel",
                      oever_blok="minecraft:snow_block", blad="spruce", diep_water=False, log=log)
        lx, lz = h[0] - kijk[0] * 5, h[1] + 2
        if mag_bouwen(park, lx, lz, 3) and not park.water[lx, lz]:
            veilige_lamp(park, lx, grond(park, lx, lz) + 1, lz, "guhlamp")
    park._oertijd["kijkhut"] = hut
    pad_lampen(park, ZONE, stap=24, soort="lantaarn", log=log)
    registreer_kijkplekken(park, hut, log)
    oostrand(park, ("ijskust", "oertijd"), log=log)
    log(f"    oertijd: grot {park._oertijd['grot']} ingang {ingang} waterval {park._oertijd['waterval']}"
        f" wolhoop {park._oertijd['wolhoop']} kampje {park._oertijd['kampje']} toren {t} hut {h}")


def plekken(park, plekken):
    info = getattr(park, "_oertijd", None)
    if not info:
        return
    x, y, z = info["waterval"]
    zet_plek(plekken, "sabeltandtijger", 0, x, y, z, 4)
    doel = info["wolhoop"] or ((info["bronnen"][0][0], info["bronnen"][0][2]) if info["bronnen"] else None)
    if doel:
        dx, dz = doel[0], doel[1]
        for r in range(4, 14):
            klaar = False
            for a in range(0, 360, 30):
                xx = int(round(dx + math.cos(math.radians(a)) * r))
                zz = int(round(dz + math.sin(math.radians(a)) * r))
                if park.zone[xx, zz] == ZONES.index(ZONE) and not park.water[xx, zz] and mag_bouwen(park, xx, zz, 5):
                    zet_plek(plekken, "mammoet", 0, xx, kolom_top_vrij(park, xx, zz), zz, 14)
                    klaar = True
                    break
            if klaar:
                break
def _pad_afst(park):
    from scipy.ndimage import distance_transform_edt
    if getattr(park, "_oer_pd", None) is None:
        park._oer_pd = distance_transform_edt(~park.pad)
    return park._oer_pd


def registreer_kijkplekken(park, hut, log=print):
    """Round 3: the oertijd kijkhut looks at the warm springs and the wool heap: favourites for the mammoths and the
    woolly rhinos there; the tower gets its favourites from the general first guess."""
    info = park._oertijd
    if not hut:
        return
    F, oog, punten = hut
    stoelen = [s for s in park.stoelen if abs(s[0] - oog[0]) <= 4 and abs(s[2] - oog[1]) <= 4 and s[1] == F + 1]
    kijk = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[stoelen[0][3]] if stoelen else None
    k = park.kijkplek("kijkhut_oertijd", ZONE, (oog[0] + 0.5, F + 2.6, oog[1] + 0.5), kijk,
                      stoelen[0][:3] if stoelen else None)
    for i, (x, z) in enumerate(punten):
        if 0 <= x < kit.W and 0 <= z < kit.W and park.binnen[x, z] and not park.water[x, z]:
            park.favoriet(f"kijkhut_oertijd_{i}", k, (x, int(park.hoogte[x, z]) + 1, z),
                          ["mammoet", "wolharige_neushoorn"], straal=3)


