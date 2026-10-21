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
                           kies_kijkhut, paadje, bruggen_ophogen, GLAS, _hut_raster, drijvend_hout_weg, guhlamp,
                           bloempot, knuffel, kijkplek_hut, opaak_lut, zicht, is_plant, PARKLAMP, glooiing,
                           platen_breken, kies_favorieten)

RNG = np.random.default_rng(kit.SEED + 202)
SPOTS = {}
kit_stoel = kit.stoel


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
    # round 3 (user: 'groter, wilder'): ~1.25x, a lobed shore instead of an ellipse
    rx, rz = 15.0, 11.0
    lobben = [(0.0, 0.0, 1.0, 1.0), (rx * 0.5, -rz * 0.3, 0.55, 0.65), (-rx * 0.45, rz * 0.35, 0.6, 0.6)]
    fase = RNG.random(2) * 2 * math.pi

    def meer_d(x, z):
        a_ = math.atan2(z - cz, x - cx)
        return min(math.hypot((x - cx - lx) / (rx * sx), (z - cz - lz) / (rz * sz)) for lx, lz, sx, sz in lobben) \
            + 0.06 * math.sin(5 * a_ + fase[0]) + 0.04 * math.sin(9 * a_ + fase[1]) + (RNG.random() - 0.5) * 0.06
    for x in range(cx - 21, cx + 22):
        for z in range(cz - 16, cz + 17):
            if park.zone[x, z] != zi("delta") or park.hek_afstand[x, z] < 5 or park.pad[x, z]:
                continue
            d = meer_d(x, z)
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
    _dicht_rand(park, cx - 23, cz - 18, cx + 23, cz + 18, top="sand")
    # two little mud islands with a flamingo nest mound each (flamingos build mud nests)
    for ix, iz in ((cx + int(rx * 0.35), cz + 2), (cx - int(rx * 0.3), cz - 3)):
        if not park.water[ix, iz]:
            continue
        for x in range(ix - 2, ix + 3):
            for z in range(iz - 2, iz + 3):
                if park.water[x, z] and math.hypot(x - ix, z - iz) <= 1.9:
                    for y in range(int(park.hoogte[x, z]), WATER_Y + 1):
                        park.set(x, y, z, "minecraft:mud")
                    park.set(x, WATER_Y + 1, z, "minecraft:packed_mud" if (x, z) == (ix, iz) else "minecraft:mud")
                    park.hoogte[x, z] = WATER_Y + (1 if (x, z) == (ix, iz) else 0)
                    park.water[x, z] = False
    SPOTS["meer"] = (cx, WATER_Y - 1, cz)


def nijlpaardenpoel(park):
    """A 4-deep pool off the path (round 3: bigger, a lobed shore with mud wallows), and beside it a sunken glass
    kijkhut: a 7-wide room on the pool bed whose pool-side wall is Eenrichtingsvadsglas (the water stays outside), a
    deep trench in front of the glass (you look into water, not at the bank), and a stairwell that runs ALONG the room
    (sideways) up to the ground, so nothing of it lies on the route (round 3: R3-A flagged mud_bricks on the path)."""
    rcx, rcz, r, _ = kit.RESERVERINGEN["nijlpaardenpoel"]
    pd = pad_afstand(park)
    dz_ = zi("delta")
    best = None
    for minhek in (14, 10, 7):
        for x in range(rcx - r - 6, rcx + r + 7):
            for z in range(rcz - r - 6, rcz + r + 7):
                if park.zone[x, z] != dz_ or park.hek_afstand[x, z] < minhek:
                    continue
                score = min(pd[x, z], 18) - math.hypot(x - rcx, z - rcz) * 0.3
                if best is None or score > best[0]:
                    best = (score, x, z)
        if best:
            break
    _, cx, cz = best
    rx, rz = 11.0, 8.5
    fase = RNG.random(2) * 2 * math.pi
    for x in range(cx - 15, cx + 16):
        for z in range(cz - 13, cz + 14):
            if park.zone[x, z] != dz_:
                continue
            a_ = math.atan2(z - cz, x - cx)
            d = math.hypot((x - cx) / rx, (z - cz) / rz) + 0.08 * math.sin(3 * a_ + fase[0]) \
                + 0.05 * math.sin(6 * a_ + fase[1]) + (RNG.random() - 0.5) * 0.05
            if d <= 1.0 and pd[x, z] >= 4 and park.hek_afstand[x, z] >= 5:
                diepte = 2 + int((1 - d) * 3)                # 2..4
                _water_kolom(park, x, z, WATER_Y - diepte, bodem="minecraft:mud" if RNG.random() < 0.5 else "minecraft:clay")
                if RNG.random() < 0.05:
                    park.set(x, WATER_Y + 1, z, "minecraft:lily_pad")
            elif d <= 1.22 and pd[x, z] >= 3 and not park.pad[x, z]:
                vlak(park, x, z, max(WATER_Y, min(int(park.hoogte[x, z]), WATER_Y + 1)), top="mud", vrij=4)
    _dicht_rand(park, cx - 17, cz - 15, cx + 17, cz + 15)
    SPOTS["poel"] = (cx, WATER_Y - 3, cz)
    # the room: on the pool rim, the side whose land behind it is widest and farthest from the path
    vloer = WATER_Y - 5
    beste = None
    for k8 in range(16):
        a_ = k8 * math.pi / 8
        ux, uz = math.cos(a_), math.sin(a_)
        ex = ez = None
        for t in range(1, 22):
            x, z = int(round(cx + ux * t)), int(round(cz + uz * t))
            if not park.water[x, z]:
                ex, ez = x, z
                break
        if ex is None:
            continue
        f = kijk_richting(-ux, -uz)
        ax, az = STAP[f]
        sx, sz = (1, 0) if ax == 0 else (0, 1)
        # room cells (k -3..3, dd 0..3) + a stairwell along the room on one side (k 4..11, dd 1..3)
        for zij in (1, -1):
            cellen = [(ex + sx * k - ax * dd, ez + sz * k - az * dd) for k in range(-3, 4) for dd in range(0, 4)]
            trap_c = [(ex + sx * k * zij - ax * dd, ez + sz * k * zij - az * dd) for k in range(4, 12) for dd in range(1, 4)]
            alles = cellen + trap_c
            if any(not (2 <= a < W - 2 and 2 <= b < W - 2) for a, b in alles):
                continue
            if any(park.zone[a, b] != dz_ or park.pad[a, b] or pd[a, b] < 3 or park.hek_afstand[a, b] < 5
                   for a, b in alles):
                continue
            if any(park.water[a, b] for a, b in trap_c) or sum(park.water[a, b] for a, b in cellen) > 4:
                continue
            score = min(min(pd[a, b] for a, b in alles), 10) + park.hek_afstand[ex, ez] * 0.05
            if beste is None or score > beste[0]:
                beste = (score, ex, ez, f, zij)
    if beste is None:
        return
    _, ex, ez, f, zij = beste
    ax, az = STAP[f]
    sx, sz = (1, 0) if ax == 0 else (0, 1)
    for k in range(-3, 4):
        for dd in range(0, 4):
            x = ex + sx * k - ax * dd
            z = ez + sz * k - az * dd
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
    # the doorway in the side wall (k = 3*zij, dd = 2), then the stairwell up along the room
    dx_, dz_w = ex + sx * 3 * zij - ax * 2, ez + sz * 3 * zij - az * 2
    for y in (vloer + 1, vloer + 2, vloer + 3):            # (round 4) 3 high: from the first stair (feet +0.5) your
        park.v[dx_, y, dz_w] = 0                           # head hit the lintel, you could not get in
    op = kijk_richting(sx * zij, sz * zij)                 # walking up = away from the room, along it
    for t in range(1, 9):
        k = 3 + t
        x, z = ex + sx * k * zij - ax * 2, ez + sz * k * zij - az * 2
        g = int(park.hoogte[x, z])
        y = vloer + t                                      # the stair block
        for dd in (1, 3):                                  # side walls of the stairwell up to the ground, a fence on top
            wx, wz = ex + sx * k * zij - ax * dd, ez + sz * k * zij - az * dd
            gw = max(int(park.hoogte[wx, wz]), y + 2)
            for yy in range(vloer, gw + 1):
                park.set(wx, yy, wz, "minecraft:mud_bricks")
            park.set(wx, gw + 1, wz, "minecraft:spruce_fence")
            park.hoogte[wx, wz] = gw
        for yy in range(int(park.vloer[x, z]), y):
            if park.v[x, yy, z] == 0 or yy >= vloer:
                park.set(x, yy, z, "minecraft:mud")
        park.set(x, y, z, trap("minecraft:spruce_stairs", op))
        for yy in range(y + 1, max(g, y) + 4):
            park.v[x, yy, z] = 0
        park.hoogte[x, z] = y
        if y >= g:
            # the top: one more landing on the ground beyond, then a little path to the route
            lx, lz = ex + sx * (k + 1) * zij - ax * 2, ez + sz * (k + 1) * zij - az * 2
            for yy in range(int(park.hoogte[lx, lz]) + 1, y + 1):
                park.set(lx, yy, lz, "minecraft:mud")
            park.set(lx, y, lz, "minecraft:dirt_path")
            ruim(park, lx, lz, y + 1, y + 4)
            park.hoogte[lx, lz] = y
            paadje(park, (lx + sx * zij, lz + sz * zij))
            break
    # a deep trench in front of the glass: water down to the room floor, so the window looks into water (and at the
    # hippos), not at the muddy pool bank
    for dd in range(1, 9):
        for k in range(-4 - dd // 3, 5 + dd // 3):
            x = ex + sx * k + ax * dd
            z = ez + sz * k + az * dd
            if park.zone[x, z] != dz_ or pd[x, z] < 3 or park.hek_afstand[x, z] < 5:
                continue
            bed = min(int(park.hoogte[x, z]), vloer)
            ruim(park, x, z, bed + 1, max(int(park.hoogte[x, z]), WATER_Y) + 3)
            for y in range(bed + 1, WATER_Y + 1):
                park.set(x, y, z, "minecraft:water", level=0)
            park.set(x, bed, z, "minecraft:mud")
            onderbouw(park, x, z, bed, "minecraft:mud")
            park.hoogte[x, z] = bed
            park.water[x, z] = True
    _dicht_rand(park, ex - 16, ez - 16, ex + 16, ez + 16)
    # inside: lanterns, guh chairs facing the glass, a lamp and a plush (round 3: cosy, the kijk-guh's seat)
    park.set(ex - ax * 2, WATER_Y + 1, ez - az * 2, "minecraft:lantern", hanging="true", waterlogged="false")
    park.lampen.append((ex - ax * 2, WATER_Y + 1, ez - az * 2, "lantaarn_hangend"))
    stoelen = []
    for k in (-2, -1, 1, 2):
        x, z = ex + sx * k - ax * 1, ez + sz * k - az * 1
        kit_stoel(park, x, vloer + 1, z, f)
        stoelen.append((x, vloer + 1, z))
    guhlamp(park, ex + sx * -2 - ax * 2, vloer + 1, ez + sz * -2 - az * 2, f)
    knuffel(park, ex + sx * 2 - ax * 2, vloer + 1, ez + sz * 2 - az * 2, f, nr=2)
    for k in range(-2, 3):
        x, z = ex + sx * k - ax * 2, ez + sz * k - az * 2
        if park.v[x, vloer + 1, z] == 0:
            park.set(x, vloer + 1, z, "minecraft:light_blue_carpet")
    SPOTS["kijkhut"] = (ex, vloer, ez)
    SPOTS["kijkhut_voor"] = (ex + ax * 4, vloer + 1, ez + az * 4)
    SPOTS["kijkhut_info"] = {"f": f, "oog": (ex - ax * 1 + 0.5, vloer + 2.62, ez - az * 1 + 0.5), "stoelen": stoelen,
                             "voor": (ex + ax, ez + az)}


def mangrovevlonders(park):
    """A muddy mangrove patch with shallow channels and a raised plank boardwalk maze (a grid with some links left out),
    mangrove trees in between; the geheimpje (zonnebril) in a chest at a dead end."""
    cx, cz, r, _ = kit.RESERVERINGEN["mangrovevlonders"]
    dz_ = zi("delta")
    R = r + 6                                              # round 3: bigger
    # never on (or right next to) the river: the boardwalk stays a marsh walk, the canoe keeps its water free
    rivier = distance_transform_edt(~SPOTS["rivier"]) if "rivier" in SPOTS else None
    ok = lambda x, z: park.zone[x, z] == dz_ and park.hek_afstand[x, z] >= 5 and not park.pad[x, z] \
        and (rivier is None or rivier[x, z] > 2)
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
    knopen = [(x, z) for x in range(cx - 15, cx + 16, 5) for z in range(cz - 15, cz + 16, 5)
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
    # under the boardwalk the marsh channels become mud (a boardwalk over mud on posts; no water under a 2-high deck)
    for (x, z) in gelegd:
        if ok(x, z) and park.water[x, z]:
            for y in range(int(park.hoogte[x, z]), WATER_Y + 1):
                park.set(x, y, z, "minecraft:mud")
            park.hoogte[x, z] = WATER_Y
            park.water[x, z] = False
    # mangrove trees between the boardwalks
    geplant = 0
    for _ in range(500):
        if geplant >= 16:
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
    # soft light in the marsh: pearlescent froglights set into the mud beside some boardwalk nodes
    for i, (x, z) in enumerate(knopen):
        if i % 3 or (x, z) not in gelegd:
            continue
        for dx, dz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            a, b = x + dx, z + dz
            if ok(a, b) and (a, b) not in gelegd and not park.water[a, b]:
                g = int(park.hoogte[a, b])
                park.lamp(a, g, b, "froglight")
                break
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
                if park.pad[x, z] or park.hek[x, z] or (rivier is not None and rivier[x, z] <= 0):
                    break
                g = int(park.hoogte[x, z])
                if park.water[x, z]:
                    for y in range(g, WATER_Y + 1):
                        park.set(x, y, z, "minecraft:mud")
                    park.hoogte[x, z] = WATER_Y
                    park.water[x, z] = False
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
    for x in range(bx - 9, bx + 10):
        for z in range(bz - 9, bz + 10):
            if park.zone[x, z] != dz_ or not park.water[x, z]:
                continue
            d = math.hypot((x - bx) / 8.0, (z - bz) / 4.5)
            if d <= 1.0 and w[x, z] <= 6:
                g = WATER_Y
                for y in range(int(park.hoogte[x, z]), g + 1):
                    park.set(x, y, z, "minecraft:sand")
                ruim(park, x, z, g + 1, g + 3)
                park.hoogte[x, z] = g
                park.water[x, z] = False
                bank.add((x, z))
                if RNG.random() < 0.08:
                    park.set(x, g + 1, z, "minecraft:dead_bush")
    _dicht_rand(park, bx - 11, bz - 11, bx + 11, bz + 11, top="sand")
    # driftwood: two bleached logs lying on the bank (the crocodiles bask against them)
    gelegd = 0
    for (x, z) in sorted(bank, key=lambda c: (c[0] * 7 + c[1] * 3) % 11):
        if gelegd >= 2:
            break
        for ax_, (ux_, uz_) in (("x", (1, 0)), ("z", (0, 1))):
            cel = [(x + ux_ * t, z + uz_ * t) for t in range(3)]
            if all(c in bank and park.v[c[0], WATER_Y + 1, c[1]] in (0, park.id("minecraft:dead_bush")) for c in cel):
                for c in cel:
                    park.set(c[0], WATER_Y + 1, c[1], kit.state("stripped_birch_log", axis=ax_))
                gelegd += 1
                break
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
    dd = math.hypot(px - mx, pz - mz) or 1.0
    mid = (int(mx + (px - mx) / dd * 20), int(mz + (pz - mz) / dd * 20))   # round 3: near the lake (in view range)
    t = zoek(park, "delta", mid, 30, 3, minhek=8, pad_min=3, pad_max=16, weg_van=[(mx, mz), (px, pz)], weg_r=14)
    if t:
        x, z, g = t
        uitkijktoren(park, x, z, g, (mx, mz), hout="mangrove", dak="hay_block", knus=True)
        SPOTS["toren"] = (x, g, z)
        SPOTS["toren_kp"] = park.kijkplekken[-1]
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
                       plantje="minecraft:potted_pink_tulip", knus=True, tapijt="minecraft:pink_carpet")
        paadje(park, _hut_raster(x, z, f, 5)(0, 8))
        SPOTS["hut"] = (x, g, z)
        SPOTS["hut_info"] = info
        SPOTS["hut_zicht"] = frac


def bouw(park, log=print):
    # (round 3: the river pipeline (terrein.py + kit.brug + controle.py) owns water and bridges: the route bridges are built at the right height already)
    SPOTS["rivier"] = park.water.copy()                   # the river as the terrain made it (the boardwalk keeps off)
    rv = kit.RESERVERINGEN
    glooiing(park, "delta", amp=1, schaal=3.5, seed=2,
             vlekken=[("minecraft:mud", 0.45), ("minecraft:moss_block", 1.4)], drempel=0.35,
             weg_van=[(rv[n][0], rv[n][1], rv[n][2] + 3) for n in ("mangrovevlonders",)],
             log=log)
    flamingomeer(park)
    nijlpaardenpoel(park)
    park._pad_d = None
    mangrovevlonders(park)
    park._pad_d = None
    krokodillenbank(park)
    toren_en_hut(park)
    drijvend_hout_weg(park, ["delta"], log)
    park._pad_d = None
    platen_breken(park, "delta", log=log)
    favorieten_delta(park, log)
    log(f"    delta: {', '.join(sorted(k for k in SPOTS if k not in ('rivier',) and not k.endswith('_kp')))}")


def _plek_bij(park, x, z, nat, r=5):
    """The nearest water (nat) or dry ground spot (x, y, z) within r of (x, z) in the delta."""
    best = None
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            a, b = int(x + dx), int(z + dz)
            if not (0 <= a < W and 0 <= b < W) or park.zone[a, b] != zi("delta") or park.hek_afstand[a, b] < 4:
                continue
            if bool(park.water[a, b]) != nat:
                continue
            g = int(park.hoogte[a, b])
            if not nat and park.v[a, g + 1, b] and not is_plant(park.get(a, g + 1, b)):
                continue
            d = math.hypot(dx, dz)
            if best is None or d < best[0]:
                best = (d, a, (WATER_Y if nat else g + 1), b)
    return best[1:] if best else None


def favorieten_delta(park, log=print):
    """Round 3: favourite spots in the view of the flamingo kijkhut, the underwater room and the tower (checked with
    the sightline from the viewer's eye)."""
    lut = opaak_lut(park)
    info = SPOTS.get("hut_info")
    if info:
        k = kijkplek_hut(park, "kijkhut_delta_flamingomeer", "delta", info, [])
        kies_favorieten(park, k, "delta", [["flamingo"], ["flamingo"]], n=2, dmin=13, dmax=28, nat=True, lut=lut)
        kies_favorieten(park, k, "delta", [["capibara", "flamingo"]], n=1, dmin=13, dmax=28, nat=False, lut=lut)
    kinfo = SPOTS.get("kijkhut_info")
    if kinfo:
        k = kijkplek_hut(park, "kijkhut_delta_nijlpaarden", "delta", kinfo, [])
        ex, _, ez = SPOTS["kijkhut"]
        ax, az = STAP[kinfo["f"]]
        sx, sz = (1, 0) if ax == 0 else (0, 1)
        for fn, dd, kk in (("voor_het_glas", 3, 0), ("schuin", 6, 3)):
            x, z = ex + ax * dd + sx * kk, ez + az * dd + sz * kk
            if park.water[x, z]:
                park.favoriet(f"{k['naam']}_{fn}", k, (x, int(park.hoogte[x, z]) + 1, z), ["nijlpaard", "zeekoe"], straal=3)
    kp = SPOTS.get("toren_kp")
    if kp:
        kies_favorieten(park, kp, "delta", [["flamingo"], ["nijlpaard", "zeekoe"]], n=2, dmin=10, dmax=40, nat=True,
                        uit_elkaar=10, lut=lut)
        kies_favorieten(park, kp, "delta", [["capibara", "krokodil"]], n=1, dmin=10, dmax=40, nat=False, lut=lut)
    n = sum(len(k["favorieten"]) for k in park.kijkplekken if k["zone"] == "delta")
    log(f"    delta: favorieten {n}")


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
