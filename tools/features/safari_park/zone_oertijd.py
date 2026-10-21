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
    cx, cz, r, _ = kit.RESERVERINGEN["ijsgrot_fossiel"]
    # entrance on the lowest side of the hill
    beste = None
    for a in range(0, 360, 15):
        ex, ez = int(round(cx + math.cos(math.radians(a)) * 11)), int(round(cz + math.sin(math.radians(a)) * 11))
        if not mag_bouwen(park, ex, ez, 6) or park.water[ex, ez]:
            continue
        if beste is None or grond(park, ex, ez) < beste[0]:
            beste = (grond(park, ex, ez), ex, ez, a)
    if beste is None:
        return None, None
    g_in, ex, ez, a = beste
    vloer = g_in
    # make sure the hill is high enough over the chamber: add a snowy mound where it is not
    for x in range(cx - 9, cx + 10):
        for z in range(cz - 9, cz + 10):
            d = math.hypot(x - cx, z - cz)
            if d > 9 or not mag_bouwen(park, x, z, 5):
                continue
            nodig = vloer + 9 - int(d * 0.5)
            g = grond(park, x, z)
            if g < nodig:
                for y in range(g + 1, nodig + 1):
                    park.set(x, y, z, "minecraft:stone" if y < nodig else "minecraft:snow_block")
                ruim_kolom(park, x, z, nodig + 1, nodig + 14)
                park.hoogte[x, z] = nodig
    lucht = []
    # the chamber
    ccy = vloer + 3
    for x in range(cx - 7, cx + 8):
        for z in range(cz - 7, cz + 8):
            for y in range(vloer + 1, vloer + 7):
                if ((x - cx) / 6.2) ** 2 + ((y - ccy) / 3.4) ** 2 + ((z - cz) / 6.2) ** 2 <= 1:
                    lucht.append((x, y, z))
    # the corridor from the entrance, 3 wide, 3 high
    n = int(math.hypot(ex - cx, ez - cz))
    ux, uz = (cx - ex) / n, (cz - ez) / n
    for t in range(-1, n):
        px, pz = ex + ux * t, ez + uz * t
        for w in (-1, 0, 1):
            x, z = int(round(px - uz * w)), int(round(pz + ux * w))
            for y in range(vloer + 1, vloer + 4):
                lucht.append((x, y, z))
    lucht = list(dict.fromkeys(lucht))
    for p in lucht:
        park.set(*p, "minecraft:air")
    _bekleed(park, lucht, rng)
    # floor: packed snow, the fossiel (a little skeleton of bone blocks)
    lucht_set = set(lucht)
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            if (x, vloer + 1, z) in lucht_set:
                park.set(x, vloer, z, "minecraft:snow_block")
    for k in range(-3, 4):
        park.set(cx + k, vloer + 1, cz, "minecraft:bone_block[axis=x]")
        if k in (-2, 0, 2):
            park.set(cx + k, vloer + 1, cz - 1, "minecraft:bone_block[axis=z]")
            park.set(cx + k, vloer + 1, cz + 1, "minecraft:bone_block[axis=z]")
    park.set(cx + 4, vloer + 1, cz, "minecraft:bone_block[axis=y]")
    park.set(cx + 4, vloer + 2, cz, "minecraft:skeleton_skull[rotation=4,powered=false]")
    # ice teeth from the ceiling
    for k in range(10):
        x, z = cx + int(rng.integers(-5, 6)), cz + int(rng.integers(-5, 6))
        for y in range(vloer + 6, vloer + 1, -1):
            if park.v[x, y, z] != 0 and park.v[x, y - 1, z] == 0:
                for d in range(1, int(rng.integers(1, 3)) + 1):
                    if park.v[x, y - d, z] == 0 and y - d > vloer + 2:
                        park.set(x, y - d, z, "minecraft:ice")
                break
    # soft light: glow lichen on the floor along the walls (light 7, ice does not melt)
    for k in range(14):
        x, z = cx + int(rng.integers(-5, 6)), cz + int(rng.integers(-5, 6))
        if park.v[x, vloer + 1, z] == 0 and park.v[x, vloer, z] != 0:
            park.set(x, vloer + 1, z, "minecraft:glow_lichen[down=true,east=false,north=false,south=false,up=false,"
                                      "waterlogged=false,west=false]")
    # grotschilderingen on the back wall (opposite the entrance): calcite with terracotta pixels
    plaatje = [
        "....oo......",
        "...oooo.....",
        ".bbbbbbbb...",
        "bbbbbbbbbb..",
        "bbbbbbbbbbbw",
        "bbbbbbbbbb.w",
        ".bb.bb.bb...",
        ".bb.bb.bb...",
    ]
    kleur = {"b": "minecraft:brown_terracotta", "o": "minecraft:pink_terracotta", "w": "minecraft:white_terracotta"}
    bx, bz = -ux, -uz                                   # direction to the back wall
    hoogte = len(plaatje)
    for i, rij in enumerate(plaatje):
        y = vloer + 1 + (hoogte - 1 - i) // 2 + 1
        if i % 2:
            continue                                     # 2 picture rows per block row (it is a small wall)
        for j, c in enumerate(rij):
            if c == ".":
                continue
            off = (j - len(rij) / 2) * 0.8
            # walk from the centre toward the back wall to the first solid block
            for t in range(1, 12):
                x = int(round(cx + bx * t - bz * off))
                z = int(round(cz + bz * t + bx * off))
                if park.v[x, y, z] != 0:
                    park.set(x, y, z, kleur[c])
                    break
    return (cx, vloer + 1, cz), (ex, vloer + 1, ez)


def bevroren_waterval(park, rng):
    cx, cz, _, _ = kit.RESERVERINGEN["bevroren_waterval"]
    g = int(np.median(park.hoogte[cx - 6:cx + 7, cz - 6:cz + 7]))
    top = g + 15
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
    cx, cz, _, _ = kit.RESERVERINGEN["warme_bronnen"]
    poelen = []
    for (ox, oz, r) in ((0, 0, 3.0), (7, 4, 2.3), (-5, 6, 2.2)):
        x0, z0 = cx + ox, cz + oz
        if not mag_bouwen(park, x0, z0, 6):
            continue
        g = grond(park, x0, z0)
        for x in range(int(x0 - r) - 2, int(x0 + r) + 3):
            for z in range(int(z0 - r) - 2, int(z0 + r) + 3):
                d = math.hypot(x - x0, z - z0)
                if d > r + 1.5 or not mag_bouwen(park, x, z, 5):
                    continue
                ruim_kolom(park, x, z, g + 1, g + 8)
                for y in range(g - 3, g + 1):
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
                    park.set(x, g, z, "minecraft:calcite" if rng.random() < 0.7 else "minecraft:dripstone_block")
        poelen.append((x0, g, z0))
    # steam vents: a smoky campfire on hay in a little stone chimney
    for (ox, oz) in ((4, -3), (-4, 2), (10, 0)):
        x, z = cx + ox, cz + oz
        if not mag_bouwen(park, x, z, 6) or park.water[x, z]:
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
    c = zoek(park, ZONE, doel, r=4, max_helling=3, max_r=30)
    if c is None:
        return None
    x, z = c
    g = grond(park, x, z)
    blob(park, x, g + 1, z, 3.4, 1.8, 2.8, [("minecraft:brown_wool", 6), ("minecraft:light_gray_wool", 1),
                                             ("minecraft:white_wool", 1)], rng, alleen_lucht=True)
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
    from .zone_ijskust import vul_laagtes, dicht_water
    rng = np.random.default_rng(kit.SEED + 601)
    park._oertijd = {}
    # (round 3: the river pipeline (terrein.py + kit.brug + controle.py) owns water and bridges: the old "raise every low column" made the coarse-dirt plate with buried trees)
    park._oertijd["grot"], ingang = ijsgrot(park, rng)
    park._oertijd["waterval"] = bevroren_waterval(park, rng)
    park._oertijd["bronnen"] = warme_bronnen(park, rng)
    bx, bz, _, _ = kit.RESERVERINGEN["warme_bronnen"]
    park._oertijd["wolhoop"] = wolhoop(park, rng, (bx + 18, bz - 6))
    park._oertijd["kampje"] = kampje(park, rng, (bx - 4, bz - 22))
    park._oertijd["vondsten"] = toendra_en_vondsten(park, rng, (bx + 30, bz + 14))
    gx, gz, _, _ = kit.RESERVERINGEN["ijsgrot_fossiel"]
    t = zoek(park, ZONE, (gx + 30, gz + 20), r=3, max_helling=4, max_r=40)
    if t:
        toren(park, *t, hout="spruce")
    h = zoek(park, ZONE, (bx - 12, bz + 4), r=3, max_helling=3, max_r=30)
    if h:
        schuilhut(park, *h, kijk=(1, 0) if bx > h[0] else (-1, 0))
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
