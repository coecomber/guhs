"""
Zone module delta (design 1.4): de mangrovevlonders (een vlonderdoolhofje tussen mangroves in een ondiep moerasje),
de nijlpaardenpoel met een glazen onderwaterkijkhut (ook de zeekoeien zwemmen hier), het roze flamingomeer, de
krokodillenzandbank met het bordje "Niet aaien (ze willen wel)", een uitkijktoren, een schuilhut en het geheimpje (de
verloren zonnebril, in een kistje tussen de mangroves). Helpers come from zone_savanne.

Round 2 (user test): the hide at the flamingo lake is the standard kijkhut (wide Eenrichtingsvadsglas band, placed and
scored on what its window sees, the flamingo homes in that view); the underwater kijkhut at the hippo pool has
Eenrichtingsvadsglas and a deep trench in front of the glass (water to look into instead of the mud bank); the route
bridge in the delta is raised (3 free blocks over the river for the canoe route).
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit
from .zone_savanne import (W, WATER_Y, zi, pad_afstand, vlak, ruim, onderbouw, zoek, bord, trap, kijk_richting, STAP,
                           TEGENOVER, uitkijktoren, schuilhut, geheimpje, zet_plek, vrij_van_hout, kijkhut,
                           kies_kijkhut, paadje, bruggen_ophogen, GLAS, _hut_raster, drijvend_hout_weg)

RNG = np.random.default_rng(kit.SEED + 202)
SPOTS = {}


def _water_kolom(park, x, z, bed, bodem="minecraft:sand", top=WATER_Y):
    ruim(park, x, z, bed + 1, max(int(park.hoogte[x, z]), top) + 4)
    onderbouw(park, x, z, bed)
    park.set(x, bed, z, bodem)
    for y in range(bed + 1, top + 1):
        park.set(x, y, z, "minecraft:water", level=0)
    park.hoogte[x, z] = bed
    park.water[x, z] = True


def _dicht_rand(park, x0, z0, x1, z1, top="mud"):
    """Every dry column next to water in the box gets at least water level (no leaking water over a lower bank)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not park.water[x, z] and park.hoogte[x, z] < WATER_Y and park.water[x - 1:x + 2, z - 1:z + 2].any():
                vlak(park, x, z, WATER_Y, top=top, vrij=3)


def flamingomeer(park):
    """A shallow lake (1-2 deep) with pink brain coral on the bed, lily pads, and pink petals / tulips / peonies on the
    banks; the flamingo homes stand in its shallow middle."""
    cx, cz, r, _ = kit.RESERVERINGEN["flamingomeer"]
    rx, rz = 12.0, 9.0
    for x in range(cx - 16, cx + 17):
        for z in range(cz - 13, cz + 14):
            if park.zone[x, z] != zi("delta") or park.hek_afstand[x, z] < 5:
                continue
            d = math.hypot((x - cx) / rx, (z - cz) / rz) + (RNG.random() - 0.5) * 0.1
            if d <= 1.0:
                diepte = 1 if d > 0.55 else 2
                bed = WATER_Y - diepte
                _water_kolom(park, x, z, bed, bodem="minecraft:sand" if RNG.random() < 0.6 else "minecraft:clay")
                if RNG.random() < 0.18:
                    park.set(x, bed, z, "minecraft:brain_coral_block")
                    park.set(x, bed + 1, z, "minecraft:brain_coral", waterlogged="true")
                elif RNG.random() < 0.10:
                    park.set(x, bed + 1, z, "minecraft:brain_coral_fan", waterlogged="true")
                if RNG.random() < 0.07:
                    park.set(x, WATER_Y + 1, z, "minecraft:lily_pad")
            elif d <= 1.45:
                vlak(park, x, z, WATER_Y if d < 1.2 else max(WATER_Y, int(park.hoogte[x, z])),
                     top="grass_block" if d > 1.12 else "sand", vrij=4)
                g = int(park.hoogte[x, z])
                k = RNG.random()
                if k < 0.30:
                    park.set(x, g + 1, z, "minecraft:pink_petals", facing=["north", "east", "south", "west"][int(RNG.integers(0, 4))],
                             flower_amount=int(RNG.integers(1, 5)))
                elif k < 0.38:
                    park.set(x, g + 1, z, "minecraft:pink_tulip")
                elif k < 0.42:
                    park.set(x, g + 1, z, "minecraft:peony", half="lower")
                    park.set(x, g + 2, z, "minecraft:peony", half="upper")
                elif k < 0.50:
                    park.set(x, g + 1, z, "minecraft:sugar_cane")
    _dicht_rand(park, cx - 18, cz - 15, cx + 18, cz + 15, top="sand")
    SPOTS["meer"] = (cx, WATER_Y - 1, cz)


def nijlpaardenpoel(park):
    """A 4-deep pool off the path, and beside it a sunken glass kijkhut: stairs down from the path side to a 5-wide room
    on the pool bed whose pool-side wall is glass (the water stays outside)."""
    rcx, rcz, r, _ = kit.RESERVERINGEN["nijlpaardenpoel"]
    pd = pad_afstand(park)
    dz_ = zi("delta")
    # the pool centre: in the spot, farthest from the path (at least 12 if possible), away from fences
    best = None
    for minhek in (14, 10, 7):
        for x in range(rcx - r - 6, rcx + r + 7):
            for z in range(rcz - r - 6, rcz + r + 7):
                if park.zone[x, z] != dz_ or park.hek_afstand[x, z] < minhek:
                    continue
                score = min(pd[x, z], 16) - math.hypot(x - rcx, z - rcz) * 0.3
                if best is None or score > best[0]:
                    best = (score, x, z)
        if best:
            break
    _, cx, cz = best
    rx, rz = 9.0, 7.0
    for x in range(cx - 12, cx + 13):
        for z in range(cz - 10, cz + 11):
            if park.zone[x, z] != dz_:
                continue
            d = math.hypot((x - cx) / rx, (z - cz) / rz) + (RNG.random() - 0.5) * 0.06
            if d <= 1.0 and pd[x, z] >= 3:
                diepte = 2 + int((1 - d) * 3)                # 2..4
                _water_kolom(park, x, z, WATER_Y - diepte, bodem="minecraft:mud" if RNG.random() < 0.5 else "minecraft:clay")
                if RNG.random() < 0.05:
                    park.set(x, WATER_Y + 1, z, "minecraft:lily_pad")
            elif d <= 1.35 and pd[x, z] >= 2:
                vlak(park, x, z, max(WATER_Y, min(int(park.hoogte[x, z]), WATER_Y + 1)), top="mud", vrij=4)
    _dicht_rand(park, cx - 14, cz - 12, cx + 14, cz + 12)
    SPOTS["poel"] = (cx, WATER_Y - 3, cz)
    # kijkhut on the side of the pool nearest to the path
    pxs, pzs = np.nonzero(park.pad[cx - 30:cx + 31, cz - 30:cz + 31])
    if not len(pxs):
        return
    i = int(np.argmin(np.hypot(pxs - 30, pzs - 30)))
    ux, uz = pxs[i] - 30, pzs[i] - 30
    n = math.hypot(ux, uz) or 1.0
    ux, uz = ux / n, uz / n
    f = kijk_richting(-ux, -uz)                  # the glass wall faces the pool (away from the path)
    ax, az = STAP[f]
    # the room: 5 wide (across), 3 deep, its glass wall on the pool edge; floor = pool bottom - 1
    # the pool edge along the direction: walk from the centre towards the path until no water
    ex, ez = cx, cz
    for t in range(1, 20):
        x, z = int(round(cx + ux * t)), int(round(cz + uz * t))
        if not park.water[x, z]:
            ex, ez = x, z
            break
    vloer = WATER_Y - 5
    sx, sz = (1, 0) if ax == 0 else (0, 1)        # across the room
    for k in range(-3, 4):
        for dd in range(-1, 4):                    # dd 0 = glass wall line, 1..3 into the land (room), -1 = pool side
            x = ex + sx * k - ax * dd
            z = ez + sz * k - az * dd
            if dd == -1:
                continue
            wand = abs(k) == 3 or dd == 3
            for y in range(vloer, WATER_Y + 3):
                if y == vloer:
                    park.set(x, y, z, "minecraft:spruce_planks")
                elif dd == 0 and abs(k) < 3 and y <= WATER_Y + 1:
                    park.set(x, y, z, GLAS)
                elif wand or y >= WATER_Y + 2:
                    park.set(x, y, z, "minecraft:mud_bricks" if y <= WATER_Y + 1 else "minecraft:spruce_planks")
                else:
                    park.v[x, y, z] = 0
            park.water[x, z] = False
            park.hoogte[x, z] = WATER_Y + 2
            onderbouw(park, x, z, vloer, "minecraft:mud")
    # stairs down from the land side: through the back wall (dd 3), 5 steps up to ground level
    for t in range(0, 7):
        y = vloer + 1 + t
        for k in (-1, 0, 1):
            x = ex + sx * k - ax * (3 + t)
            z = ez + sz * k - az * (3 + t)
            g = int(park.hoogte[x, z])
            if k == 0:
                for yy in range(y, max(g, y) + 4):
                    park.v[x, yy, z] = 0
                park.set(x, y - 1 if t else vloer, z, trap("minecraft:spruce_stairs", TEGENOVER[f]) if t else "minecraft:spruce_planks")
                for yy in range(int(park.vloer[x, z]), y - 1):
                    if park.v[x, yy, z] == 0:
                        park.set(x, yy, z, "minecraft:mud")
            else:
                for yy in range(min(y - 1, g), max(g, y + 3) + 1):
                    park.set(x, yy, z, "minecraft:mud_bricks")
        if y >= WATER_Y + 3 or y - 1 >= int(park.hoogte[ex - ax * (3 + t), ez - az * (3 + t)]):
            break
    # a deep trench in front of the glass: water down to the room floor, so the window looks into water (and at the
    # hippos), not at the muddy pool bank
    for dd in range(1, 9):
        for k in range(-4 - dd // 3, 5 + dd // 3):
            x = ex + sx * k + ax * dd
            z = ez + sz * k + az * dd
            if park.zone[x, z] != dz_ or pd[x, z] < 2 or park.hek_afstand[x, z] < 5:
                continue
            bed = min(int(park.hoogte[x, z]), vloer)
            ruim(park, x, z, bed + 1, max(int(park.hoogte[x, z]), WATER_Y) + 3)
            for y in range(bed + 1, WATER_Y + 1):
                park.set(x, y, z, "minecraft:water", level=0)
            park.set(x, bed, z, "minecraft:mud")
            onderbouw(park, x, z, bed, "minecraft:mud")
            park.hoogte[x, z] = bed
            park.water[x, z] = True
    _dicht_rand(park, ex - 14, ez - 14, ex + 14, ez + 14)
    # lanterns in the room
    park.set(ex - ax * 2, WATER_Y + 1, ez - az * 2, "minecraft:lantern", hanging="true", waterlogged="false")
    SPOTS["kijkhut"] = (ex, vloer, ez)
    SPOTS["kijkhut_voor"] = (ex + ax * 4, vloer + 1, ez + az * 4)


def mangrovevlonders(park):
    """A muddy mangrove patch with shallow channels and a raised plank boardwalk maze (a grid with some links left out),
    mangrove trees in between; the geheimpje (zonnebril) in a chest at a dead end."""
    cx, cz, r, _ = kit.RESERVERINGEN["mangrovevlonders"]
    dz_ = zi("delta")
    R = r + 2
    ok = lambda x, z: park.zone[x, z] == dz_ and park.hek_afstand[x, z] >= 5 and not park.pad[x, z]
    # ground: mud at y 63, channels (water 62 over a mud bed at 61)
    for x in range(cx - R, cx + R + 1):
        for z in range(cz - R, cz + R + 1):
            if not ok(x, z) or math.hypot(x - cx, z - cz) > R:
                continue
            kanaal = (math.sin(x * 0.45 + z * 0.12) + math.sin(z * 0.38 - x * 0.2)) > 0.9
            if kanaal and park.hek_afstand[x, z] >= 7:
                _water_kolom(park, x, z, WATER_Y - 1, bodem="minecraft:mud")
            else:
                vlak(park, x, z, WATER_Y + 1, top="mud", onder="mud", vrij=10)
    _dicht_rand(park, cx - R - 2, cz - R - 2, cx + R + 2, cz + R + 2)
    # boardwalk grid: nodes every 5, links mostly present (a maze: some left out), deck at y 64 on posts
    D = WATER_Y + 2
    knopen = [(x, z) for x in range(cx - 10, cx + 11, 5) for z in range(cz - 10, cz + 11, 5)
              if ok(x, z) and park.hek_afstand[x, z] >= 6]
    kset = set(knopen)
    gelegd = set()
    for (x, z) in knopen:
        for dx, dz in ((5, 0), (0, 5)):
            if (x + dx, z + dz) in kset and RNG.random() < 0.72:
                for t in range(0, 6):
                    gelegd.add((x + dx * t // 5, z + dz * t // 5))
    for (x, z) in gelegd:
        if not ok(x, z):
            continue
        ruim(park, x, z, D, D + 4)
        park.set(x, D, z, "minecraft:spruce_planks")
        if (x + z) % 3 == 0:
            for y in range(int(park.hoogte[x, z]) + 1, D):
                park.set(x, y, z, kit.state("stripped_spruce_log", axis="y"))
    # mangrove trees between the boardwalks
    geplant = 0
    for _ in range(300):
        if geplant >= 9:
            break
        x = int(cx + RNG.integers(-R + 2, R - 1))
        z = int(cz + RNG.integers(-R + 2, R - 1))
        if not ok(x, z) or park.hek_afstand[x, z] < 7 or park.water[x, z]:
            continue
        if any(abs(x - a) <= 2 and abs(z - b) <= 2 for a, b in gelegd):
            continue
        top = park.boom("mangrove", x, int(park.hoogte[x, z]), z, grootte=0.9)
        park.bomen.append((x, top, z, "mangrove"))
        geplant += 1
    # the geheimpje at a dead end of the boardwalk
    eindes = [(x, z) for (x, z) in gelegd if ok(x, z) and
              sum((x + dx, z + dz) in gelegd for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) == 1]
    if eindes:
        x, z = max(eindes, key=lambda c: math.hypot(c[0] - cx, c[1] - cz))
        geheimpje(park, x, D + 1, z, facing="north")
    # a connecting plank strip from the nearest boardwalk node to the path
    pd = pad_afstand(park)
    if knopen:
        nx, nz = min(knopen, key=lambda c: pd[c[0], c[1]])
        pxs, pzs = np.nonzero(park.pad[nx - 25:nx + 26, nz - 25:nz + 26])
        if len(pxs):
            i = int(np.argmin(np.hypot(pxs - 25, pzs - 25)))
            tx, tz = nx - 25 + int(pxs[i]), nz - 25 + int(pzs[i])
            n = int(math.hypot(tx - nx, tz - nz)) + 1
            for k in range(n + 1):
                x = int(round(nx + (tx - nx) * k / n))
                z = int(round(nz + (tz - nz) * k / n))
                if park.pad[x, z] or park.hek[x, z]:
                    break
                g = int(park.hoogte[x, z])
                if park.water[x, z]:
                    ruim(park, x, z, D, D + 4)
                    park.set(x, D, z, "minecraft:spruce_planks")
                else:
                    ruim(park, x, z, g + 1, g + 4)
                    park.set(x, g, z, "minecraft:spruce_planks" if g >= D - 1 else "minecraft:dirt_path")
    SPOTS["vlonders"] = (cx, D + 1, cz)


def krokodillenbank(park):
    """A sandbank in the river on the delta side, flush with the water (y 62), with the warning sign on its bank."""
    dz_ = zi("delta")
    pd = pad_afstand(park)
    land_d = distance_transform_edt(park.water)            # distance (in water) to dry land
    kand = []
    xs, zs = np.nonzero((park.zone == dz_) & park.water & (land_d >= 1) & (land_d <= 2))
    for x, z in zip(xs, zs):
        if pd[x, z] < 10 or park.hek_afstand[x, z] < 8:
            continue
        # want wide water (the river) in front of it
        breed = park.water[x - 6:x + 7, z - 6:z + 7].mean()
        kand.append((-breed, math.hypot(x - 600, z - 470) * 0.001, int(x), int(z)))
    if not kand:
        return
    kand.sort()
    _, _, bx, bz = kand[0]
    # the bank: an elongated blob of sand flush with the water surface, plus a little sloped gravel edge
    w = distance_transform_edt(park.water)
    bank = set()
    for x in range(bx - 7, bx + 8):
        for z in range(bz - 7, bz + 8):
            if park.zone[x, z] != dz_ or not park.water[x, z]:
                continue
            d = math.hypot((x - bx) / 6.0, (z - bz) / 3.5)
            if d <= 1.0 and w[x, z] <= 5:
                g = WATER_Y
                for y in range(int(park.hoogte[x, z]), g + 1):
                    park.set(x, y, z, "minecraft:sand")
                ruim(park, x, z, g + 1, g + 3)
                park.hoogte[x, z] = g
                park.water[x, z] = False
                bank.add((x, z))
                if RNG.random() < 0.08:
                    park.set(x, g + 1, z, "minecraft:dead_bush")
    _dicht_rand(park, bx - 9, bz - 9, bx + 9, bz + 9, top="sand")
    # the sign on the nearest dry land of the delta, facing the path
    best = None
    for x in range(bx - 14, bx + 15):
        for z in range(bz - 14, bz + 15):
            if (x, z) not in bank and park.zone[x, z] == dz_ and not park.water[x, z] and park.hek_afstand[x, z] >= 3 \
                    and pd[x, z] >= 2 \
                    and not park.water[x - 1:x + 2, z - 1:z + 2].any() and vrij_van_hout(park, x, z, x, z, 4):
                d = math.hypot(x - bx, z - bz) + pd[x, z] * 0.5
                if best is None or d < best[0]:
                    best = (d, x, z)
    if best:
        _, sx, sz = best
        g = int(park.hoogte[sx, sz])
        ruim(park, sx, sz, g + 1, g + 3)
        pxs, pzs = np.nonzero(park.pad[sx - 20:sx + 21, sz - 20:sz + 21])
        if len(pxs):
            i = int(np.argmin(np.hypot(pxs - 20, pzs - 20)))
            f = kijk_richting(pxs[i] - 20, pzs[i] - 20)
        else:
            f = kijk_richting(sx - bx, sz - bz)
        bord(park, sx, g + 1, sz, ["Niet aaien", "(ze willen wel)", "", "- de rangers"],
             rotation={"south": 0, "west": 4, "north": 8, "east": 12}[f])
    SPOTS["zandbank"] = (bx, WATER_Y + 1, bz)


def toren_en_hut(park):
    mx, _, mz = SPOTS["meer"]
    px, _, pz = SPOTS.get("poel", (mx, 0, mz))
    mid = ((mx + px) // 2, (mz + pz) // 2)
    t = zoek(park, "delta", mid, 30, 3, minhek=8, pad_min=3, pad_max=16, weg_van=[(mx, mz), (px, pz)], weg_r=14)
    if t:
        x, z, g = t
        uitkijktoren(park, x, z, g, (mx, mz), hout="mangrove", dak="hay_block")
        SPOTS["toren"] = (x, g, z)
    # the kijkhut at the flamingo lake: targets = the lake (flamingos stand in its shallow water)
    doelen = [(mx + 0.5, WATER_Y + 1.4, mz + 0.5)]
    for k in range(16):
        a_ = 2 * math.pi * k / 16
        doelen.append((mx + math.cos(a_) * 7 + 0.5, WATER_Y + 1.4, mz + math.sin(a_) * 5 + 0.5))
    weg = [(mx, mz)] + ([SPOTS["toren"][::2]] if "toren" in SPOTS else [])
    k = kies_kijkhut(park, "delta", doelen, 15, 26, weg_van=weg[1:], weg_r=8)
    if k:
        x, z, g, f, frac = k
        info = kijkhut(park, x, z, g, richting=f, hout="mangrove", dak="hay_block", basis="minecraft:mud_bricks",
                       titel=("Kijkhut", "Het Flamingomeer", "Roze, op een pootje", "(meestal)"),
                       plantje="minecraft:potted_pink_tulip")
        paadje(park, _hut_raster(x, z, f, 5)(0, 8))
        SPOTS["hut"] = (x, g, z)
        SPOTS["hut_info"] = info
        SPOTS["hut_zicht"] = frac


def bouw(park, log=print):
    # (round 3: the river pipeline (terrein.py + kit.brug + controle.py) owns water and bridges: the route bridges are built at the right height already)
    flamingomeer(park)
    nijlpaardenpoel(park)
    park._pad_d = None
    mangrovevlonders(park)
    park._pad_d = None
    krokodillenbank(park)
    toren_en_hut(park)
    drijvend_hout_weg(park, ["delta"], log)
    park._pad_d = None
    log(f"    delta: {', '.join(sorted(SPOTS))}")


def plekken(park, plekken):
    """Nijlpaarden and zeekoeien in the nijlpaardenpoel, flamingo's in the lake, krokodillen on the sandbank."""
    if "poel" in SPOTS:
        x, _, z = SPOTS["poel"]
        bed = int(park.hoogte[x, z])
        for i in range(3):
            zet_plek(plekken, "nijlpaard", i, (x, bed + 1, z), straal=8)
        zet_plek(plekken, "zeekoe", 0, (x, bed + 1, z), straal=7)
    if "kijkhut_voor" in SPOTS:
        # one hippo family right in front of the underwater window
        zet_plek(plekken, "nijlpaard", 0, SPOTS["kijkhut_voor"], straal=6)
    if "meer" in SPOTS:
        x, _, z = SPOTS["meer"]
        info = SPOTS.get("hut_info")
        if info:
            # the flamingos in the half of the lake that faces the kijkhut window
            hx, hz = info["voor"]
            d = math.hypot(x - hx, z - hz) or 1.0
            ux, uz = (x - hx) / d, (z - hz) / d
            for i in range(4):
                t = d - 5 + (i % 2) * 3
                zij = (i // 2) * 4 - 2
                fx = int(round(hx + ux * t - uz * zij))
                fz = int(round(hz + uz * t + ux * zij))
                if not park.water[fx, fz]:
                    fx, fz = x, z
                zet_plek(plekken, "flamingo", i, (fx, int(park.hoogte[fx, fz]) + 1, fz), straal=7)
        else:
            for i in range(4):
                zet_plek(plekken, "flamingo", i, (x + (i % 2) * 3 - 1, int(park.hoogte[x, z]) + 1, z + (i // 2) * 3 - 1),
                         straal=10)
    if "zandbank" in SPOTS:
        x, y, z = SPOTS["zandbank"]
        for i in range(2):
            zet_plek(plekken, "krokodil", i, (x, y, z), straal=8)
