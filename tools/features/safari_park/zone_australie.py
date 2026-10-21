"""
Zone module: Australië (design 1.4, zone table).

  eucalyptusbos       a dense grove of eucalyptus trees (koala's), a few fallen branches
  billabong           a quiet waterhole with an old windpomp (the kangoeroes drink here) and the schuilhut
  quokka_eiland       the island in the quokka pond: bushes, a little tree, stepping stones half under water
  outback             red outback with spinifex tussocks and the wombatholletjesheuvel (burrow holes)
  postkantoortje      a corrugated-iron outback post office with a red mailbox (geheimpje: the lost postcard)
  schaduwboom         a shade tree with a hammock
  prieel              the bower of the prieelvogel: an avenue of sticks with blue treasures
  uitkijktoren
Helpers come from zone_europa.py.
"""
import math

import numpy as np

from . import kit
from .zone_europa import (kap, vlak, zet_grond, grond_y, toren, kijkhut, zet_huizen, extra_huis, ver_van_hek, _naam,
                          hut_kijkplek, fav, knus, vuurvliegjes, lantaarnpaal)

W, WATER_Y = kit.W, kit.WATER_Y
R = kit.RESERVERINGEN

EUCALYPTUS = (R["eucalyptusbos"][0], R["eucalyptusbos"][1])      # 183, 433
BILLABONG = (302, 470)          # moved east of the reserved spot: the footpath runs through 278,478
QUOKKA = (R["quokka_eiland"][0], R["quokka_eiland"][1])           # 343, 428 (the pond is made by terrein.py)
WOMBAT = (205, 472)             # moved: the reserved spot sat on the south fence
POST = (296, 428)      # well away from the europa border fence
SCHADUW = (240, 445)
PRIEEL = (338, 482)
TOREN = (318, 448)
BILLA_HUT = (302, 481)          # round 2: front wall of the kijkhut south of the billabong, looks north over the water


def zacht_oever(park, cx, cz, r, top="sand"):
    """Round 3 (the user: 'kunnen de kangoeroes wel uit het meertje? het water zit overal 1 blok onder de zijkant'):
    every dry cell right next to the water of a pond within r of (cx, cz) gets its ground AT water level (an animal steps
    straight out of the water), the next ring one higher - so no bank stands a block above the water any more."""
    cellen = [(x, z) for x in range(cx - r, cx + r + 1) for z in range(cz - r, cz + r + 1) if park.water[x, z]]
    ring1, ring2 = set(), set()
    for (x, z) in cellen:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                c = (x + dx, z + dz)
                if not park.water[c] and not park.pad[c] and not park.hek[c] and park.hek_afstand[c] >= 3:
                    ring1.add(c)
    for (x, z) in ring1:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                c = (x + dx, z + dz)
                if c not in ring1 and not park.water[c] and not park.pad[c] and not park.hek[c] and park.hek_afstand[c] >= 3:
                    ring2.add(c)
    for (x, z) in ring1:
        if park.hoogte[x, z] != WATER_Y:
            zet_grond(park, x, z, WATER_Y, top, "sand" if top == "sand" else "dirt")
    for (x, z) in ring2:
        if park.hoogte[x, z] > WATER_Y + 1:
            zet_grond(park, x, z, WATER_Y + 1, top, "sand" if top == "sand" else "dirt")


def eucalyptusbos(park):
    x, z = EUCALYPTUS
    rng = np.random.default_rng(kit.SEED + 81)
    r = R["eucalyptusbos"][2] + 6                       # round 3: a bigger, wilder grove
    gezet = []
    for _ in range(400):
        if len(gezet) >= 19:
            break
        a = rng.random() * 2 * math.pi
        d = math.sqrt(rng.random()) * r
        bx, bz = int(x + math.cos(a) * d), int(z + math.sin(a) * d)
        if any(math.hypot(bx - ox, bz - oz) < 4.5 for ox, oz in gezet):
            continue
        if park.water[bx, bz] or park.pad[bx, bz] or park.hek_afstand[bx, bz] < 6:
            continue
        g = int(park.hoogte[bx, bz])
        top = park.boom("eucalyptus", bx, g, bz, grootte=float(rng.uniform(0.8, 1.5)))
        park.bomen.append((bx, top, bz, "eucalyptus"))
        gezet.append((bx, bz))
    # fallen bark and twigs on the ground
    for _ in range(14):
        a = rng.random() * 2 * math.pi
        d = rng.random() * r
        bx, bz = int(x + math.cos(a) * d), int(z + math.sin(a) * d)
        g = int(park.hoogte[bx, bz])
        if park.v[bx, g + 1, bz] == 0 and not park.water[bx, bz]:
            park.set(bx, g + 1, bz, "brown_carpet")
    for _ in range(5):
        a = rng.random() * 2 * math.pi
        bx, bz = int(x + math.cos(a) * r * 0.7), int(z + math.sin(a) * r * 0.7)
        for t in range(4):
            g = int(park.hoogte[bx + t, bz])
            if park.v[bx + t, g + 1, bz] == 0 and not park.water[bx + t, bz] and not park.pad[bx + t, bz]:
                park.set(bx + t, g + 1, bz, "stripped_birch_log", axis="x")
    # undergrowth: low bushes (azalea / jungle leaves) and dry grass between the trunks
    for _ in range(60):
        a, d = rng.random() * 2 * math.pi, math.sqrt(rng.random()) * r
        bx, bz = int(x + math.cos(a) * d), int(z + math.sin(a) * d)
        if park.water[bx, bz] or park.pad[bx, bz] or park.hek_afstand[bx, bz] < 4:
            continue
        g = int(park.hoogte[bx, bz])
        if park.v[bx, g + 1, bz] != 0:
            continue
        k = rng.random()
        if k < 0.45:
            park.set(bx, g + 1, bz, ("azalea_leaves", "jungle_leaves", "birch_leaves")[int(rng.integers(0, 3))],
                     distance=1, persistent="true", waterlogged="false")
        elif k < 0.8:
            park.set(bx, g + 1, bz, "tall_grass", half="lower")
            park.set(bx, g + 2, bz, "tall_grass", half="upper")
        else:
            park.set(bx, g + 1, bz, "fern")


def billabong(park):
    x, z = BILLABONG
    kap(park, x, z, 12)
    g = int(park.hoogte[x, z])
    for xx in range(x - 10, x + 11):
        for zz in range(z - 10, z + 11):
            d = math.hypot((xx - x) / 1.3, zz - z)
            if park.pad[xx, zz] or park.hek[xx, zz]:
                continue
            if d <= 6:
                diep = WATER_Y - 3 if d < 3.5 else WATER_Y - 1
                zet_grond(park, xx, zz, diep, "mud" if d < 4 else "sand", "dirt")
                for y in range(diep + 1, WATER_Y + 1):
                    park.set(xx, y, zz, "water", level=0)
                park.water[xx, zz] = True
            elif d <= 11:                                   # round 3: a beach that slopes down INTO the water
                doel = WATER_Y + max(0, int(round((d - 6.5) * 0.8)))
                if park.hoogte[xx, zz] != doel and (park.hoogte[xx, zz] > doel or d <= 8):
                    zet_grond(park, xx, zz, doel, "sand", "sand")
    # reeds and lily pads
    for a in range(0, 360, 25):
        xx = x + round(math.cos(math.radians(a)) * 6.6 * 1.3)
        zz = z + round(math.sin(math.radians(a)) * 6.6)
        if not park.water[xx, zz] and park.hoogte[xx, zz] >= WATER_Y:
            g2 = int(park.hoogte[xx, zz])
            if park.v[xx, g2 + 1, zz] == 0 and any(park.water[xx + a, zz + b] for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                park.set(xx, g2 + 1, zz, "sugar_cane", age=0)
                park.set(xx, g2 + 2, zz, "sugar_cane", age=0)
    for dx, dz in ((1, 2), (-3, -1), (2, -2)):
        park.set(x + dx, WATER_Y + 1, z + dz, "lily_pad")
    zacht_oever(park, x, z, 12)
    # the windpomp: a 3x3 lattice tower of spruce fences, a platform, a wheel of trapdoors and a tail vane
    wx, wz = x + 12, z - 4
    g = int(park.hoogte[wx, wz])
    vlak(park, wx, wz, 2.5, g, "coarse_dirt", "dirt")
    hoog = 9
    for y in range(g + 1, g + hoog):
        for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            park.set(wx + dx, y, wz + dz, "spruce_fence")
        if (y - g) % 3 == 0:
            for dx, dz in ((0, -1), (0, 1), (-1, 0), (1, 0)):
                park.set(wx + dx, y, wz + dz, "spruce_fence")
    park.fill(wx - 1, g + hoog, wz - 1, wx + 1, g + hoog, wz + 1, "spruce_slab", type="bottom", waterlogged="false")
    hub_y = g + hoog + 3
    park.set(wx, g + hoog + 1, wz, kit.state("spruce_log", axis="y"))
    park.set(wx, g + hoog + 2, wz, kit.state("spruce_log", axis="y"))
    park.set(wx, hub_y, wz - 1, "iron_block")
    for a in range(0, 360, 30):
        for rr in (1, 2, 3):
            yy = hub_y + round(math.sin(math.radians(a)) * rr)
            xx = wx + round(math.cos(math.radians(a)) * rr)
            park.set(xx, yy, wz - 2, "iron_trapdoor" if rr == 3 else "iron_bars",
                     **({"facing": "north", "half": "bottom", "open": "true", "powered": "false",
                         "waterlogged": "false"} if rr == 3 else {}))
    for t in range(1, 4):
        park.set(wx, hub_y, wz + t, kit.state("spruce_log", axis="z"))
    park.set(wx, hub_y, wz + 4, "red_wool")
    park.set(wx, hub_y + 1, wz + 4, "red_wool")
    # a water trough at the foot
    for dx in (2, 3):
        park.set(wx + dx, g + 1, wz + 2, "cauldron")


def quokka_eiland(park):
    x, z = QUOKKA
    rng = np.random.default_rng(kit.SEED + 82)
    # round 3: grow the island (pond cells near its middle become land: a low dome, its edge a beach at water level)
    eiland0 = [(xx, zz) for xx in range(x - 6, x + 7) for zz in range(z - 6, z + 7)
               if not park.water[xx, zz] and math.hypot(xx - x, zz - z) <= 6]
    if eiland0:
        ix = sum(a for a, _ in eiland0) / len(eiland0)
        iz = sum(b for _, b in eiland0) / len(eiland0)
        for xx in range(int(ix) - 6, int(ix) + 7):
            for zz in range(int(iz) - 6, int(iz) + 7):
                d = math.hypot(xx - ix, zz - iz)
                if d <= 5.2 and park.water[xx, zz] and park.hek_afstand[xx, zz] >= 4:
                    zet_grond(park, xx, zz, WATER_Y + (1 if d <= 3.6 else 0), "grass_block" if d <= 3.6 else "sand", "dirt")
        x, z = int(round(ix)), int(round(iz))              # from here on: the island's own middle
    park.quokka_mid = (x, z)
    # the island = the dry cells near the pond centre
    eiland = [(xx, zz) for xx in range(x - 6, x + 7) for zz in range(z - 6, z + 7)
              if not park.water[xx, zz] and math.hypot(xx - x, zz - z) <= 6]
    for (xx, zz) in eiland:
        g = int(park.hoogte[xx, zz])
        for y in range(g + 1, g + 4):
            if park.v[xx, y, zz] and _naam(park, xx, y, zz).endswith(("dead_bush", "short_grass")):
                park.v[xx, y, zz] = 0
        park.set(xx, g, zz, "grass_block", snowy="false")
        if park.v[xx, g + 1, zz] == 0:
            r = rng.random()
            if r < 0.25:
                park.set(xx, g + 1, zz, "short_grass")
            elif r < 0.3:
                park.set(xx, g + 1, zz, "dandelion")
    if eiland:
        g = int(park.hoogte[x, z])
        top = park.boom("acacia", x, g, z, grootte=0.6)
        park.bomen.append((x, top, z, "acacia"))
        for dx, dz in ((2, 1), (-2, -1), (1, -2)):
            if (x + dx, z + dz) in eiland:
                park.blob(x + dx, int(park.hoogte[x + dx, z + dz]) + 1, z + dz, 1.2, 0.9, 1.2, park.leaves("acacia"))
    if eiland:
        bx, bz = x + 2, z - 2
        if (bx, bz) in eiland:
            top = park.boom("acacia", bx, int(park.hoogte[bx, bz]), bz, grootte=0.45)
            park.bomen.append((bx, top, bz, "acacia"))
    zacht_oever(park, x, z, 14, top="sand")              # beaches on the island AND on the pond's outer shore
    # stepping stones half under water from the west shore (visitors may look, quokka's stay)
    for k, dx in enumerate((-8, -10, -12)):
        sx, sz = x + dx, z + (k % 2)
        if park.water[sx, sz]:
            park.set(sx, WATER_Y, sz, "smooth_stone_slab", type="top", waterlogged="false")


def outback(park):
    """Spinifex tussocks over the open red outback (east half of the zone) and the wombat burrow hill."""
    rng = np.random.default_rng(kit.SEED + 83)
    au = 8
    xs, zs = np.nonzero((park.zone == au) & ~park.water & ~park.pad & (park.hek_afstand > 4))
    for _ in range(900):
        i = int(rng.integers(0, len(xs)))
        x, z = int(xs[i]), int(zs[i])
        if math.hypot(x - EUCALYPTUS[0], z - EUCALYPTUS[1]) < 18:
            continue
        g = int(park.hoogte[x, z])
        if park.v[x, g + 1, z] != 0 and not _naam(park, x, g + 1, z).endswith(("dead_bush", "short_grass")):
            continue
        if rng.random() < 0.5:
            park.set(x, g + 1, z, "tall_grass", half="lower")
            park.set(x, g + 2, z, "tall_grass", half="upper")
        else:
            park.set(x, g + 1, z, "short_grass")
    # round 3, wilder: red sandstone outcrops (boulder piles), a few dead trees, a dry creek bed of coarse dirt + gravel
    rotsen = 0
    for _ in range(400):
        if rotsen >= 6:
            break
        i = int(rng.integers(0, len(xs)))
        x, z = int(xs[i]), int(zs[i])
        if park.hek_afstand[x, z] < 9 or math.hypot(x - EUCALYPTUS[0], z - EUCALYPTUS[1]) < 24 or \
                math.hypot(x - BILLABONG[0], z - BILLABONG[1]) < 16 or math.hypot(x - QUOKKA[0], z - QUOKKA[1]) < 16:
            continue
        if any(park.pad[x + a, z + b] for a in range(-6, 7, 3) for b in range(-6, 7, 3)):
            continue
        g = int(park.hoogte[x, z])
        for k in range(int(rng.integers(3, 6))):
            ox, oz = int(rng.integers(-3, 4)), int(rng.integers(-3, 4))
            rr = float(rng.uniform(1.4, 2.8))
            park.blob(x + ox, g + rr * 0.6, z + oz, rr, rr * 0.9, rr, ("red_sandstone", "smooth_red_sandstone",
                      "terracotta", "red_terracotta")[int(rng.integers(0, 4))])
        rotsen += 1
    for _ in range(5):
        i = int(rng.integers(0, len(xs)))
        x, z = int(xs[i]), int(zs[i])
        if park.hek_afstand[x, z] < 6 or park.v[x, int(park.hoogte[x, z]) + 1, z] not in (0,):
            continue
        g = int(park.hoogte[x, z])
        for y in range(g + 1, g + 5 + int(rng.integers(0, 3))):
            park.set(x, y, z, kit.state("stripped_acacia_log", axis="y"))
        park.set(x + 1, g + 4, z, "acacia_fence")
        park.set(x - 1, g + 3, z, "acacia_fence")
    # wombat hill: a low round hill with burrow holes
    x, z = WOMBAT
    kap(park, x, z, 10)
    g = int(park.hoogte[x, z])
    r = 8
    for xx in range(x - r, x + r + 1):
        for zz in range(z - r, z + r + 1):
            d = math.hypot(xx - x, zz - z)
            if d <= r and not park.pad[xx, zz] and not park.hek[xx, zz] and not park.water[xx, zz]:
                top = g + int(round(3 * math.cos(d / r * math.pi / 2)))
                if top > park.hoogte[xx, zz]:
                    zet_grond(park, xx, zz, top, "red_sand", "red_sandstone")
    for a in (20, 140, 260):
        hx = x + round(math.cos(math.radians(a)) * 4)
        hz = z + round(math.sin(math.radians(a)) * 4)
        top = int(park.hoogte[hx, hz])
        park.set(hx, top, hz, "air")
        park.set(hx, top - 1, hz, "air")
        park.set(hx, top - 2, hz, "coarse_dirt")


def postkantoortje(park):
    x, z = POST
    kap(park, x, z, 7)
    g = int(park.hoogte[x, z])
    vlak(park, x, z, 5.5, g, "coarse_dirt", "dirt")
    for dx in range(-3, 4):
        for dz in range(-2, 3):
            rand = abs(dx) == 3 or abs(dz) == 2
            hoek = abs(dx) == 3 and abs(dz) == 2
            park.set(x + dx, g, z + dz, "spruce_planks")
            for y in range(g + 1, g + 4):
                if rand:
                    park.set(x + dx, y, z + dz, kit.state("stripped_spruce_log", axis="y") if hoek else
                             "light_gray_terracotta")
                else:
                    park.set(x + dx, y, z + dz, "air")
    # corrugated roof (iron) with an overhang over a veranda on the west (path) side
    for dx in range(-5, 4):
        for dz in range(-3, 4):
            park.set(x + dx, g + 4, z + dz, "iron_block" if (dz % 2 == 0) else "smooth_stone")
    for dz in (-3, 3):
        for y in range(g + 1, g + 4):
            park.set(x - 5, y, z + dz, "spruce_fence")
    # round 2: a proper door (the old opening with two loose panes looked broken) and two real windows beside it
    park.set(x - 3, g + 1, z, "spruce_door", facing="east", half="lower", hinge="left", open="false", powered="false")
    park.set(x - 3, g + 2, z, "spruce_door", facing="east", half="upper", hinge="left", open="false", powered="false")
    for dz in (-1, 1):
        park.set(x - 3, g + 1, z + dz, "light_gray_terracotta")
        park.set(x - 3, g + 2, z + dz, "glass")
    for dx in (-1, 1):
        park.set(x + dx, g + 2, z - 2, "glass")
        park.set(x + dx, g + 2, z + 2, "glass")
    park.set(x - 4, g, z, "spruce_slab", type="top", waterlogged="false")       # a step onto the veranda
    # counter + the lost postcard spot (geheimpje): a chest behind the counter
    park.fill(x, g + 1, z - 1, x, g + 1, z + 1, "spruce_planks")
    park.set(x + 2, g + 1, z + 1, "chest", facing="west", type="single", waterlogged="false")
    park.set(x + 2, g + 1, z - 1, "barrel", facing="up", open="false")
    # round 3: cosy inside (not bare): a rug, a flower pot and a little bell on the counter, a shelf of letters, a lamp
    for (cx, cz) in ((x - 2, z), (x - 1, z), (x - 2, z - 1), (x - 2, z + 1)):
        if park.v[cx, g + 1, cz] == 0:
            park.set(cx, g + 1, cz, "orange_carpet")
    park.set(x, g + 2, z - 1, "potted_dead_bush")
    park.set(x, g + 2, z + 1, "flower_pot")
    park.set(x + 2, g + 1, z, "chiseled_bookshelf", facing="west")
    park.lamp(x + 1, g + 3, z, "lantaarn_hangend")
    for dz in (-2, 2):
        park.lamp(x - 5, g + 3, z + dz, "lantaarn_hangend")
    # the red mailbox on a post in front
    mx, mz = x - 7, z + 2
    gm = int(park.hoogte[mx, mz])
    park.set(mx, gm + 1, mz, "spruce_fence")
    park.set(mx, gm + 2, mz, "red_terracotta")
    park.set(mx, gm + 3, mz, "red_carpet")
    # sign-like name board on the roof edge
    for dz in (-1, 0, 1):
        park.set(x - 4, g + 5, z + dz, "yellow_terracotta")


def kamp(park):
    """Round 3: an outback camp beside the shade tree: a campfire in a ring of stones, log benches, a lantern on a swag
    chest - the light of the outback at night."""
    x, z = SCHADUW[0], SCHADUW[1] + 7
    if park.hek_afstand[x, z] < 6 or park.pad[x, z]:
        return
    g = int(park.hoogte[x, z])
    vlak(park, x, z, 3.5, g, "coarse_dirt", "dirt")
    park.lamp(x, g + 1, z, "kampvuur")
    for a in range(0, 360, 45):
        sx, sz = x + round(math.cos(math.radians(a)) * 2), z + round(math.sin(math.radians(a)) * 2)
        park.set(sx, g, sz, "cobblestone")
    for (bx, bz, ax) in ((x - 3, z, "z"), (x + 3, z, "z"), (x, z + 3, "x")):
        park.set(bx, g + 1, bz, kit.state("stripped_acacia_log", axis=ax))
    park.set(x + 2, g + 1, z - 3, "chest", facing="south", type="single", waterlogged="false")
    park.lamp(x + 2, g + 2, z - 3, "lantaarn")


def schaduwboom(park):
    x, z = SCHADUW
    kap(park, x, z, 8)
    g = int(park.hoogte[x, z])
    a = (x - 3, z)
    b = (x + 4, z)
    for (tx, tz) in (a, b):
        top = park.boom("acacia", tx, int(park.hoogte[tx, tz]), tz, grootte=1.2)
        park.bomen.append((tx, top, tz, "acacia"))
    # hammock between the two trunks, sagging one block in the middle
    for xx in range(a[0] + 1, b[0]):
        mid = abs(xx - (a[0] + b[0]) / 2) < 2
        park.set(xx, g + (2 if mid else 3), z, "white_wool")
    for (tx, tz) in (a, b):
        park.set(tx, g + 3, tz, kit.state("acacia_log", axis="y"))


def prieel(park):
    x, z = PRIEEL
    kap(park, x, z, 6)
    g = int(park.hoogte[x, z])
    vlak(park, x, z, 4.5, g, "grass_block", "dirt")
    # the avenue: two walls of sticks (fences + dead bushes) leaning in, a court with blue treasures in front
    for dz in range(-2, 3):
        for side in (-1, 1):
            park.set(x + side, g + 1, z + dz, "spruce_fence")
            park.set(x + side, g + 2, z + dz, "dead_bush" if dz % 2 else "spruce_fence")
    rng = np.random.default_rng(kit.SEED + 84)
    blauw = ["cornflower", "blue_orchid", "cornflower"]
    for dx in range(-3, 4):
        for dz in range(3, 6):
            if rng.random() < 0.6 and park.v[x + dx, g + 1, z + dz] == 0:
                park.set(x + dx, g + 1, z + dz, blauw[int(rng.integers(0, 3))])
    park.set(x, g + 1, z + 4, "light_blue_carpet")
    park.set(x + 2, g + 1, z + 5, "blue_carpet")


def bouw(park, log=print):
    eucalyptusbos(park)
    billabong(park)
    quokka_eiland(park)
    outback(park)
    postkantoortje(park)
    schaduwboom(park)
    kamp(park)
    prieel(park)
    kap(park, TOREN[0], TOREN[1], 6)
    vlak(park, TOREN[0], TOREN[1], 3.5, int(park.hoogte[TOREN[0], TOREN[1]]), "coarse_dirt", "dirt")
    toren(park, TOREN[0], TOREN[1], hout="acacia", hoogte=8, dakblok="iron_block")
    # round 2: the billabong kijkhut (raised, one-way glass, the kangaroos drink on the north bank right in front)
    F = kijkhut(park, BILLA_HUT[0], BILLA_HUT[1], kijk="north", hout="acacia", dak="acacia", naam="billabong-kijkhut",
                zicht=16)
    knus(park, BILLA_HUT[0], BILLA_HUT[1], "north", F, kleed="orange_carpet")
    k = hut_kijkplek(park, "billabong-kijkhut", BILLA_HUT[0], BILLA_HUT[1], "north", F, zone="australie")
    fav(park, k, "drinkplek", BILLABONG[0] - 3, BILLABONG[1] - 7, ["kangoeroe"], 3)
    fav(park, k, "windpomp", BILLABONG[0] + 10, BILLABONG[1] - 2, ["kangoeroe", "wombat"], 3)
    # lights: lantern posts at the huts and the tower, fireflies in the eucalyptus at dusk
    for (lx, lz) in ((BILLA_HUT[0] + 5, BILLA_HUT[1] + 7), (TOREN[0] + 4, TOREN[1] + 4), (POST[0] - 8, POST[1] - 4),
                     (PRIEEL[0] - 4, PRIEEL[1] + 6)):
        lantaarnpaal(park, lx, lz, "acacia")
    vuurvliegjes(park, EUCALYPTUS[0], EUCALYPTUS[1], 16, 8, kit.SEED + 241,
                 op=("grass_block", "podzol", "coarse_dirt", "dirt", "red_sand", "sand"))
    # (round 3: the river pipeline (terrein.py + kit.brug + controle.py) owns water and bridges: leaks are sealed generically by controle.lekken)
    for naam, (x, z, r) in {"wombat": (WOMBAT[0], WOMBAT[1], 9), "billabong": (BILLABONG[0], BILLABONG[1], 10),
                             "post": (POST[0], POST[1], 6), "toren": (TOREN[0], TOREN[1], 5),
                             "kijkhut": (BILLA_HUT[0], BILLA_HUT[1] + 3, 6)}.items():
        if not ver_van_hek(park, x, z, r):
            log(f"  LET OP australie: {naam} ligt dicht bij een hek")
    from . import preview
    for spot in (("park_iso_australie", 290 - 128, 455 - 128),):
        if spot not in preview.SPOTS:
            preview.SPOTS.append(spot)


def plekken(park, plekken_):
    zet_huizen(park, plekken_, "koala", [(EUCALYPTUS[0] + 3, EUCALYPTUS[1] - 2), (EUCALYPTUS[0] - 5, EUCALYPTUS[1] + 4)],
               "australie")
    zet_huizen(park, plekken_, "kangoeroe", [(BILLABONG[0] - 3, BILLABONG[1] - 9)], "australie")
    extra_huis(park, plekken_, "kangoeroe", (BILLABONG[0] + 6, BILLABONG[1] - 8), "australie")      # round 2: in view
    qx, qz = getattr(park, "quokka_mid", QUOKKA)                  # on the island, beside its little tree
    zet_huizen(park, plekken_, "quokka", [(qx - 2, qz + 1)], "australie", straal=4)
    zet_huizen(park, plekken_, "wombat", [(WOMBAT[0] + 5, WOMBAT[1] + 1)], "australie")
    zet_huizen(park, plekken_, "prieelvogel", [(PRIEEL[0], PRIEEL[1] + 3)], "australie")
