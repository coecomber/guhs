"""
Super Guhrio (bbq2) - het Kasteel van de Grote Nether-Mika: the structure guhs:guhrio_kasteel (type guhs:burcht, saved as
32 x 32 tiles guhrio_kasteel/stuk_<i>_<j>, like the Spiesburcht; it rises out of the sauce sea of the Guhbarbecuether).

The castle is a shell with slots that the other Guhrio slices fill (see guhrio_baan.py and the manual
guhs_work130/reports/slice_guhrio-engine.md):

  two wings (west x 6..22, east x 104..120, z 9..109), three storeys each: six sealed level halls. A hall is the lane
      (96 cells), the painted wall behind it and 11 blocks of gallery in front of it for the side-view camera.
      cellar = world 2 (2-1 west, 2-2 east), ground floor = world 1 (1-1, 1-2), upper floor = world 3 (3-1, 3-2).
      West lanes run south with the camera on the west, east lanes run north with the camera on the east: the galleries
      are on the outside, behind the tall orange windows.
  the keep (x 39..87, z 34..82) between them: the level hall (the hub) on the ground floor with a gate (guhrio_poort) to
      every level and the big gate to the duel; above it the duel arena (a lane of 40) and the tower room behind it.
  four round corner towers, a gatehouse in the south wall, a forecourt on pillars in the sauce with a little dock.

You walk: dock -> forecourt -> gatehouse -> inner court -> the keep's door -> the level hall. A gate puts you in its level;
the flagpole puts you back in the hall (the duel: in the tower room, from where stairs lead down to the hall).

build(h) builds the shell, checks it (nothing floats, every walk works, the camera rooms are empty), lets every slot be
filled (LEVELS / DUEL / VOORPLEIN / TORENKAMER / HAL of guhrio_w1..w3 and guhrio_beloning, else the engine's own practice
lanes and decoration), writes the level files and the tiles, and declares the structure set + its guaranteed copy.
"""
import importlib
import math

from features import guhrio_baan as gb
from features import guhrio_proef
from features import spiesburcht_burcht as sb

NAAM = "guhrio_kasteel"
W, H, D = 127, 96, 127
G = 26                       # the top block of the ground you walk on outside (world y 33)
C = 63                       # the middle (x)
ANKER = (C, G, 58)

STENEN, TRAP, PLAAT, MUUR, HEK, GEBEITELD = sb.STENEN, sb.TRAP, sb.PLAAT, sb.MUUR, sb.HEK, sb.GEBEITELD
AIR = sb.AIR
ZWART, ZWART_TRAP = "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone_brick_stairs"
DAK, DAK_TRAP, DAK_PLAAT = "minecraft:red_nether_bricks", "minecraft:red_nether_brick_stairs", "minecraft:red_nether_brick_slab"
# the orange trim (bands, merlons, window frames, thousands of blocks): the brick's look on a PLAIN block. Never build with
# the brick piece guhs:guhrio_steen itself: its block entity draws it (only within 96 blocks) and nothing can hang on it
ORANJE = "guhs:guhrio_siersteen"
GOUD = "minecraft:gold_block"
GLAS = "minecraft:orange_stained_glass"
LICHT = "minecraft:shroomlight"
LAMP = ("minecraft:light", {"level": "15", "waterlogged": "false"})

# the wings: thema -> the first row of its slot; the lane's column and the gallery (the camera's room)
Y0 = {1: 27, 2: 4, 3: 50}
SLOT_H, LEVEL_L = 20, 96
WZ0, WZ1, LZ0, LZ1 = 9, 109, 11, 106
VLEUGEL = {"west": dict(x0=6, x1=22, buiten=(6, 7), baan=19, galerij=(8, 18), rug=(21, 22)),
           "oost": dict(x0=104, x1=120, buiten=(119, 120), baan=107, galerij=(108, 118), rug=(104, 105))}
LEVELS = [("1-1", 1, "west"), ("1-2", 1, "oost"), ("2-1", 2, "west"), ("2-2", 2, "oost"), ("3-1", 3, "west"), ("3-2", 3, "oost")]
# the keep
KX0, KX1, KZ0, KZ1 = 39, 87, 34, 82
HAL_Y, DUEL_Y0, DUEL_H, DUEL_L = G, 43, 18, 40
POORT_Z = {1: 68, 2: 58, 3: 48}                    # thema -> where its two gates are along the hall's side walls
HAL_UIT = (C, G + 1, 60)                           # where a flagpole puts you
TOREN_UIT = (C, 43, 68)                            # where the duel puts you
MODULES = ("guhrio_w1", "guhrio_w2", "guhrio_w3", "guhrio_beloning")
THEMA_RAND = {1: "minecraft:lime_concrete", 2: "minecraft:cyan_terracotta", 3: DAK}


def level_id(wereld):
    return "kasteel_" + wereld.replace("-", "_")


# =====================================================================================================================
# small tools
# =====================================================================================================================
def _steen(b, x0, y0, z0, x1, y1, z1):
    """A solid mass of charcoal bricks (a few cracked ones)."""
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                b.set(x, y, z, b.brick(0.06))


def _toren(b, cx, cz, r, y0, y1, dik=2, vol=False):
    """A round tower shell (dik thick); the blocks of each ring are bricks."""
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.3 and (vol or d > r - dik + 0.3):
                for y in range(y0, y1 + 1):
                    b.set(x, y, z, b.brick(0.06))


def _kegel(b, cx, cz, r, y0, top=GOUD):
    """A pointed red roof on a round tower, with a golden tip."""
    y = y0
    while r >= 0:
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                d = math.hypot(x - cx, z - cz)
                if d <= r + 0.5 and (d > r - 1.3 or r <= 1):
                    b.set(x, y, z, DAK)
        y += 1
        r -= 1
    b.set(cx, y, cz, top)
    b.set(cx, y + 1, cz, top)
    return y + 1


def _kantelen(b, x0, z0, x1, z1, y, blok=STENEN):
    """A parapet with merlons on the edge of a rectangle."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                b.set(x, y, z, blok)
                if (x + z) % 2 == 0:
                    b.set(x, y + 1, z, ORANJE)


def _vaandel(b, x, y, z, dx, dz, hoog=7):
    """A red banner with a golden bar and a golden diamond, flat against a wall (two wide along dx / dz)."""
    for k in range(2):
        for i in range(hoog):
            blok = GOUD if i == hoog - 1 else "minecraft:yellow_wool" if i == hoog - 4 else "minecraft:red_wool"
            if i == 0 and k == 1:
                continue                                         # a swallow tail
            b.set(x + dx * k, y + i, z + dz * k, blok)


def _gloeiraam(b, x, y, z, dx, dz, nx, nz, breed=3, hoog=8):
    """A tall glowing window in a thick wall: orange glass outside, light behind it, an orange frame (n: the way in)."""
    for k in range(-1, breed + 1):
        for i in range(-1, hoog + 1):
            px, pz = x + dx * k, z + dz * k
            rand = k in (-1, breed) or i in (-1, hoog)
            top = i == hoog - 1 and k in (0, breed - 1)           # a pointed top
            if rand or top:
                b.set(px, y + i, pz, ORANJE)
            else:
                b.set(px, y + i, pz, GLAS)
                b.set(px + nx, y + i, pz + nz, LICHT)


def _paal(b, x, y, z, hoog=3):
    """A lamp post."""
    for k in range(hoog):
        b.fence(x, y + k, z)
    b.lantern(x, y + hoog, z)


def _pijp(b, x, y, z, hoog):
    """A green pipe standing in the yard (decoration: its mouth has no partner)."""
    for k in range(hoog - 1):
        b.set(x, y + k, z, gb.PIJP_LIJF, {"axis": "y"})
    b.set(x, y + hoog - 1, z, gb.PIJP, {"kanaal": "15", "facing": "up", "ingang": "false", "boven": "false"})


# =====================================================================================================================
# the shell
# =====================================================================================================================
def binnenplaats(b):
    """The plate everything between the wings stands on, the air above it, the two curtain walls."""
    for x in range(23, 104):
        for z in range(WZ0, WZ1 + 1):
            b.set(x, G - 1, z, STENEN)
            b.set(x, G, z, sb.NYLIUM if b.rng.random() < 0.8 else sb.AS_AARDE)
    b.air(23, G + 1, 12, 103, G + 16, 106)
    for z0 in (WZ0, 107):                                       # north and south curtain wall, with a wall-walk
        _steen(b, 23, 0, z0, 103, G + 20, z0 + 2)
        for x in range(23, 104):
            for z in (z0, z0 + 2):                               # bands of orange brick, inside and outside
                b.set(x, G, z, ORANJE)
                b.set(x, G + 1, z, ORANJE)
                b.set(x, G + 13, z, ORANJE)
                b.set(x, G + 20, z, ORANJE)
        _kantelen(b, 23, z0, 103, z0 + 2, G + 21)
        b.air(24, G + 21, z0 + 1, 102, G + 22, z0 + 1)
    # the yard: paths of dark brick, lamp posts, a few green pipes and floating blocks for the mood
    for z in range(83, 107):
        for x in range(60, 67):
            b.set(x, G, z, ZWART if 61 <= x <= 65 else ORANJE)
    for x in list(range(26, 39)) + list(range(88, 101)):
        for z in (58, 59):
            b.set(x, G, z, ZWART)
    for x, z in ((57, 100), (69, 100), (57, 88), (69, 88), (30, 30), (96, 30), (30, 86), (96, 86), (30, 58), (96, 58)):
        _paal(b, x, G + 1, z)
    for x, z in ((27, 20), (33, 24), (99, 20), (93, 24), (27, 96), (99, 96), (50, 96), (76, 96)):
        _pijp(b, x, G + 1, z, 2 + (x + z) % 3)
    for x, z in ((48, 90), (78, 90)):                           # two little hills of bricks with a ?-block on top
        for i in range(3):
            b.fill(x - 2 + i, G + 1 + i, z - 2 + i, x + 2 - i, G + 1 + i, z + 2 - i, ORANJE)
        b.set(x, G + 5, z, gb.VRAAG, {"inhoud": "munt", "leeg": "false"}, {"id": "guhs:guhrio_stuk"})
    for x in range(23, 104):
        for z in range(12, 107):
            if b.get(x, G, z) == sb.mc(sb.NYLIUM) and b.get(x, G + 1, z) == AIR and b.rng.random() < 0.05:
                b.set(x, G + 1, z, sb.ZWAMMETJE if b.rng.random() < 0.4 else sb.SCHEUTJES)


def hoektorens(b):
    tops = []
    for cx in (14, 112):
        for cz in (11, 107):
            _toren(b, cx, cz, 10, 0, 76)
            _toren(b, cx, cz, 11, 73, 76, dik=3)                 # a ring that sticks out
            for x in range(cx - 11, cx + 12):
                for z in range(cz - 11, cz + 12):
                    d = math.hypot(x - cx, z - cz)
                    if 10.3 < d <= 11.3:
                        b.set(x, 73, z, ORANJE)
                        if (x + z) % 2 == 0:
                            b.set(x, 77, z, ORANJE)              # merlons
                    if d <= 10.3:
                        b.set(x, 76, z, STENEN)                  # the floor of the top
                    if 9.6 < d <= 10.3:
                        for y in (G, G + 1, 48, 49, 62):
                            b.set(x, y, z, ORANJE)
            for (dx, dz) in ((7, 7), (-7, 7), (7, -7), (-7, -7), (10, 0), (-10, 0), (0, 10), (0, -10)):
                for n, y0 in enumerate((G + 6, G + 16, G + 27, G + 38)):   # glowing slits, winding up
                    if (n + (dx != 0 and dz != 0)) % 2 == 0:
                        for k in range(5):
                            if 0 <= cx + dx < W and 0 <= cz + dz < D:
                                b.set(cx + dx, y0 + k, cz + dz, GLAS)
            tops.append(_kegel(b, cx, cz, 9, 77))
    return tops


def vleugel(b, kant):
    """One wing: the walls, the three floors and the roof. The halls are hollow; the levels are stamped in later."""
    v = VLEUGEL[kant]
    x0, x1 = v["x0"], v["x1"]
    g0, g1 = v["galerij"]
    hol = (min(g0, v["baan"]), max(g1, v["baan"]))               # gallery + lane: the hollow part
    for x in range(x0, x1 + 1):
        for z in range(WZ0, WZ1 + 1):
            binnen = hol[0] <= x <= hol[1] and LZ0 <= z <= LZ1
            for y in range(0, 72):
                vloer = y <= 3 or 24 <= y <= 26 or 47 <= y <= 49 or y >= 70
                if not binnen or vloer:
                    b.set(x, y, z, b.brick(0.06))
                else:
                    b.set(x, y, z, AIR)
    for thema, y0 in Y0.items():                                 # the trench under every lane (a pit ends in it)
        for z in range(LZ0, LZ1 + 1):
            b.set(v["baan"], y0 - 1, z, AIR)
    # the outside: orange bands, buttresses, tall glowing windows on the two floors above the ground
    buiten = v["buiten"][0] if kant == "west" else v["buiten"][1]
    binnen = v["buiten"][1] if kant == "west" else v["buiten"][0]
    stap = -1 if kant == "west" else 1
    for z in range(WZ0, WZ1 + 1):
        for y in (G, G + 1, 48, 49, 70, 71):
            b.set(buiten, y, z, ORANJE)
        if 14 <= z <= 104 and (z - 14) % 8 == 0:                 # a buttress
            for y in range(0, 62):
                b.set(buiten + stap, y, z, ZWART if y < G + 2 else STENEN)
            b.stair(buiten + stap, 62, z, "east" if kant == "west" else "west")
        if 18 <= z <= 100 and (z - 18) % 8 == 0:                 # a window
            for y0 in (Y0[1], Y0[3]):
                for y in range(y0 + 5, y0 + 13):
                    for dz in (0, 1):
                        b.set(buiten, y, z + dz, GLAS)
                        b.set(binnen, y, z + dz, AIR)
                for y in range(y0 + 4, y0 + 14):                  # an orange frame round the glass
                    for dz in (-1, 0, 1, 2):
                        if b.get(buiten, y, z + dz) != GLAS:
                            b.set(buiten, y, z + dz, ORANJE if dz in (-1, 2) or y in (y0 + 4, y0 + 13) else STENEN)
    # the roof: a red gable between the two corner towers
    mid = (x0 + x1) // 2
    dak0, dak1 = 21, 97
    for z in range(dak0, dak1 + 1):
        for i in range(0, mid - x0 + 2):
            y = 72 + i
            xl, xr = x0 - 1 + i, x1 + 1 - i
            if xl >= xr:
                b.set(mid, y, z, DAK)
                b.slab(mid, y + 1, z, block=DAK_PLAAT)
                break
            b.stair(xl, y, z, "east", block=DAK_TRAP)
            b.stair(xr, y, z, "west", block=DAK_TRAP)
            for x in range(xl + 1, xr):                           # (closed under the tiles: nothing floats, nothing leaks in)
                if z in (dak0, dak1) or x in (xl + 1, xr - 1):
                    b.set(x, y, z, DAK)


def donjon(b):
    """The keep: the level hall, the duel storey above it (solid: its rooms are cut out later), the roof and the spire."""
    for x in range(KX0, KX1 + 1):
        for z in range(KZ0, KZ1 + 1):
            rand = x < KX0 + 3 or x > KX1 - 3 or z < KZ0 + 3 or z > KZ1 - 3
            for y in range(0, 63):
                if rand or y in (G - 1, G) or 41 <= y <= 60 or y >= 61:
                    b.set(x, y, z, b.brick(0.06))
                elif y > G:
                    b.set(x, y, z, AIR)
    for x in range(KX0, KX1 + 1):
        for z in range(KZ0, KZ1 + 1):
            if x in (KX0, KX1) or z in (KZ0, KZ1):
                for y in (G, 41, 60):
                    b.set(x, y, z, ORANJE)
    _kantelen(b, KX0, KZ0, KX1, KZ1, 63)
    for x in (47, 77):                                           # tall glowing windows on the duel storey
        _gloeiraam(b, x, 45, KZ1, 1, 0, 0, -1)
    for x in (47, 62, 77):
        _gloeiraam(b, x, 45, KZ0, 1, 0, 0, 1)
    for z in (44, 57, 70):
        _gloeiraam(b, KX0, 45, z, 0, 1, 1, 0)
        _gloeiraam(b, KX1, 45, z, 0, 1, -1, 0)
    for x in (48, 77):                                           # real windows into the level hall, beside the door
        for dx in (0, 1):
            for y in range(G + 3, G + 8):
                b.set(x + dx, y, KZ1, GLAS)
                b.set(x + dx, y, KZ1 - 1, AIR)
                b.set(x + dx, y, KZ1 - 2, GLAS)
    for x in (52, 73):                                           # banners beside the big face
        _vaandel(b, x, 47, KZ1 + 1, 1, 0, hoog=9)
    for cx in (KX0, KX1):                                        # four turrets on the corners
        for cz in (KZ0, KZ1):
            _toren(b, cx, cz, 5, 0, 70, vol=False)
            _toren(b, cx, cz, 6, 68, 70, dik=3)
            for x in range(cx - 5, cx + 6):
                for z in range(cz - 5, cz + 6):
                    if math.hypot(x - cx, z - cz) <= 5.3:
                        b.set(x, 70, z, STENEN)
            _kegel(b, cx, cz, 5, 71)
    # the spire in the middle of the roof: a square tower with a pyramid of red tiles and the flag
    sx0, sx1, sz0, sz1 = C - 8, C + 8, 50, 66
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            if x in (sx0, sx1) or z in (sz0, sz1):
                for y in range(63, 81):
                    hoek = x in (sx0, sx1) and z in (sz0, sz1)
                    b.set(x, y, z, ORANJE if y in (63, 64, 79, 80) or (hoek and y % 2 == 0) else b.brick(0.06))
            b.set(x, 80, z, STENEN)
    for x, z in ((C, sz0), (C, sz1), (sx0, 58), (sx1, 58)):       # a tall glowing window in every face
        for y in range(68, 76):
            for k in (-1, 0, 1):
                if not (y == 75 and k != 0):
                    b.set(x + (k if z in (sz0, sz1) else 0), y, z + (k if x in (sx0, sx1) else 0), GLAS)
    for x, z in ((C, sz0 + 1), (C, sz1 - 1), (sx0 + 1, 58), (sx1 - 1, 58)):
        for y in (69, 72):
            b.set(x, y, z, LICHT)
    _kantelen(b, sx0 - 1, sz0 - 1, sx1 + 1, sz1 + 1, 80)
    for i in range(9):
        y = 81 + i
        for x in range(sx0 + i, sx1 + 1 - i):
            for z in range(sz0 + i, sz1 + 1 - i):
                if x <= sx0 + i + 1 or x >= sx1 - i - 1 or z <= sz0 + i + 1 or z >= sz1 - i - 1:
                    b.set(x, y, z, DAK)                           # (two thick, so every layer rests on the one below)
    for y in range(90, 95):
        b.set(C, y, 58, GOUD if y < 91 else "minecraft:oak_fence")
    for k in (1, 2, 3):
        b.set(C + k, 94, 58, "minecraft:red_wool")
        b.set(C + k, 93, 58, "minecraft:red_wool")
    gezicht(b)


# what hangs over the gates of a world: a hill under a blue sky, pipes in the dark, a castle over the sauce
SCHILDERIJ = {1: ["bbbbbbb", "bwwbbbb", "bbbbggb", "bgggggg", "ggggggg"],
              2: ["zzzzzzz", "zpzzzpz", "zpzcypz", "zpzzzpz", "ddddddd"],
              3: ["nnnnnnn", "nrnrnrn", "nrrrrrn", "nrrorrn", "ooooooo"]}
SCHILDER_KLEUR = {"b": "minecraft:light_blue_concrete", "w": "minecraft:white_concrete", "g": "minecraft:lime_concrete",
                  "z": "minecraft:black_concrete", "p": "minecraft:green_concrete", "c": "minecraft:cyan_concrete", "y": "minecraft:gold_block",
                  "d": "minecraft:deepslate_bricks", "n": "minecraft:nether_bricks", "r": "minecraft:red_nether_bricks", "o": LICHT}

GEZICHT = ["..BB.........BB..",
           ".BBB.........BBB.",
           ".BB..PP...PP..BB.",
           "....PPPP.PPPP....",
           "...PPPPPPPPPPP...",
           "..PPPPPPPPPPPPP..",
           "..PZZZPPPPPZZZP..",
           "..PPOOZPPPZOOPP..",
           "..PPOOPPPPPOOPP..",
           "..PPPPPPNPPPPPP..",
           "..PPPPPNNNPPPPP..",
           "...PZZZZZZZZZP...",
           "...PZWZWZWZWZP...",
           "....PPPPPPPPP...."]


def gezicht(b):
    """The Grote Nether-Mika himself, in bricks on the keep's south face: horns, glowing eyes, teeth."""
    kleuren = {"P": "minecraft:pink_terracotta", "Z": "minecraft:black_concrete", "O": LICHT, "N": "minecraft:pink_concrete",
               "W": "minecraft:quartz_block", "B": "minecraft:bone_block"}
    for r, rij in enumerate(GEZICHT):
        for c, ch in enumerate(rij):
            if ch in kleuren:
                b.set(C - 8 + c, 58 - r, KZ1, kleuren[ch])
                if ch == "B":
                    b.set(C - 8 + c, 58 - r, KZ1 + 1, kleuren[ch])   # the horns stick out


def hal(b):
    """The level hall: floor, pillars, the door, the carpet, the stairs to the tower room, the seven gates."""
    for x in range(42, 85):
        for z in range(37, 80):
            b.set(x, G, z, ZWART if (x // 2 + z // 2) % 2 == 0 else "minecraft:polished_andesite")
            b.set(x, 40, z, STENEN)
            if x in (42, 84) or z in (37, 79):                    # the walls: a dark plinth, two orange bands
                for y, blok in ((G + 1, ZWART), (G + 2, ZWART), (G + 3, ORANJE), (39, ORANJE)):
                    wx, wz = (41 if x == 42 else 85 if x == 84 else x), (36 if z == 37 else 80 if z == 79 else z)
                    b.set(wx, y, wz, blok)
    b.air(C - 2, G + 1, 80, C + 2, G + 5, KZ1)                    # the door (five wide, with a pointed top)
    b.air(C - 1, G + 6, 80, C + 1, G + 6, KZ1)
    for x in (C - 3, C + 3):
        for y in range(G + 1, G + 7):
            b.set(x, y, KZ1, ORANJE)
    for z in range(38, 83):
        for x in (C - 1, C, C + 1):
            b.set(x, G + 1, z, "minecraft:red_carpet")
    for px in (50, 76):                                           # pillars with a golden band
        for pz in (45, 58, 71):
            for y in range(G + 1, 40):
                for dx in (0, 1):
                    for dz in (0, 1):
                        b.set(px + dx - (1 if px > C else 0), y, pz + dz, GOUD if y == G + 4 else ZWART)
    for z in (45, 58, 71):                                        # chandeliers over the carpet
        b.hang_lantern(C, 40, z, length=3)
        b.set(C, 40, z, LICHT)
    for x in (46, 54, 72, 80):
        for z in (41, 52, 64, 75):
            b.set(x, 40, z, LICHT)
    # the stairs along the south wall up to the tower room's landing
    b.stairway((77, 79), 68, 1, G + 1, 16, "east", along_x=True)
    # the gates
    poorten = {}
    for wereld, thema, kant in LEVELS:
        z = POORT_Z[thema]
        x = 41 if kant == "west" else 85
        naar = 1 if kant == "west" else -1                        # towards the hall
        b.air(x, G + 1, z - 1, x, G + 3, z + 1)
        b.air(x, G + 4, z, x, G + 4, z)
        for dz in range(-2, 3):                                    # a frame in the world's colour round the niche
            for y in range(G + 1, G + 7):
                if b.get(x, y, z + dz) != AIR:
                    b.set(x, y, z + dz, THEMA_RAND[thema])
        b.set(x - naar, G + 4, z, LICHT)
        for r, rij in enumerate(SCHILDERIJ[thema]):               # a picture of its world over the gate
            for c, ch in enumerate(rij):
                b.set(x, G + 11 - r, z - 3 + c, SCHILDER_KLEUR[ch])
        for c in range(-1, 8):                                    # in a golden frame
            for r in (-1, len(SCHILDERIJ[thema])):
                b.set(x, G + 11 - r, z - 3 + c, GOUD)
        for r in range(len(SCHILDERIJ[thema])):
            b.set(x, G + 11 - r, z - 4, GOUD)
            b.set(x, G + 11 - r, z + 4, GOUD)
        b.sign(x + naar, G + 5, z, "east" if kant == "west" else "west", [f"guhrio.poort.{wereld}.1", f"guhrio.poort.{wereld}.2"])
        poorten[wereld] = ((x, G + 1, z), "west" if kant == "west" else "east", (x + naar * 3, G + 1, z))
    # the big gate to the duel, in the north wall, at the end of the carpet
    b.air(C - 1, G + 1, 36, C + 1, G + 4, 36)
    b.air(C, G + 5, 36, C, G + 5, 36)
    for dx in range(-3, 4):
        for y in range(G + 1, G + 8):
            if (abs(dx) >= 2 or y >= G + 6) and b.get(C + dx, y, 36) != AIR:
                b.set(C + dx, y, 36, GOUD if (abs(dx) == 3 or y == G + 7) else DAK)
    for dx in (-3, 3):                                            # two horns over it
        b.set(C + dx, G + 8, 37, "minecraft:bone_block")
        b.set(C + dx + (1 if dx > 0 else -1), G + 9, 37, "minecraft:bone_block")
    b.sign(C, G + 6, 37, "south", ["guhrio.poort.duel.1", "guhrio.poort.duel.2"])
    b.set(C, G + 5, 35, LICHT)
    poorten["duel"] = ((C, G + 1, 36), "north", (C, G + 1, 40))
    return poorten


def duelverdieping(b):
    """The rooms of the storey above the hall, cut out of the solid: the duel arena, the tower room, the corridor to the stairs."""
    b.air(42, DUEL_Y0, 39, 83, DUEL_Y0 + DUEL_H - 1, 50)          # the arena: lane z 39, gallery z 40..50
    for x in range(43, 43 + DUEL_L):
        b.set(x, DUEL_Y0 - 1, 39, AIR)                           # its trench
    # the tower room (21 x 13, 7 high) and its door
    b.air(53, 43, 58, 73, 49, 70)
    for x in range(53, 74):
        for z in range(58, 71):
            b.set(x, 42, z, "minecraft:dark_oak_planks" if (x + z) % 2 else "minecraft:spruce_planks")
            b.set(x, 50, z, STENEN)
    for x, z in ((57, 61), (69, 61), (57, 67), (69, 67), (63, 64)):
        b.set(x, 50, z, LICHT)
    b.air(C - 1, 43, 71, C + 1, 45, 72)
    # the corridor from the door to the top of the stairs (a wall between it and the stairwell)
    b.air(62, 43, 73, 84, 45, 75)
    b.air(84, 43, 76, 84, 45, 79)
    for x in range(62, 85, 5):
        b.set(x, 46, 74, LICHT)
    for z in range(77, 80):
        b.set(84, 42, z, STENEN)


def poortgebouw(b):
    """The gatehouse in the south wall and the two little turrets beside the gate."""
    _steen(b, 55, 0, 103, 71, G + 26, 111)
    b.air(C - 2, G + 1, 103, C + 2, G + 6, 111)
    b.air(C - 1, G + 7, 103, C + 1, G + 7, 111)
    for z in range(103, 112):
        for x in range(C - 2, C + 3):
            b.set(x, G, z, ZWART)
    for x in range(C - 2, C + 3):                                 # the portcullis, pulled up
        b.bars(x, G + 6, 110)
    for x in range(55, 72):
        for y in (G, G + 12, G + 25):
            b.set(x, y, 111, ORANJE)
    _kantelen(b, 55, 103, 71, 111, G + 27)
    for cx in (55, 71):
        _toren(b, cx, 111, 4, 0, G + 34, vol=True)
        _toren(b, cx, 111, 5, G + 32, G + 34, dik=2)
        for x in range(cx - 5, cx + 6):
            for z in range(106, 117):
                d = math.hypot(x - cx, z - 111)
                if 3.6 < d <= 4.3:
                    for y in (G, G + 1, G + 13, G + 26):
                        b.set(x, y, z, ORANJE)
                if 4.3 < d <= 5.3 and (x + z) % 2 == 0:
                    b.set(x, G + 35, z, ORANJE)
        for y in range(G + 16, G + 21):
            b.set(cx, y, 115, GLAS)
        _kegel(b, cx, 111, 4, G + 35)
    for x in range(C - 3, C + 4):                                 # the arch: orange stones round the opening
        for y in range(G + 1, G + 9):
            if b.get(x, y, 111) != AIR and (abs(x - C) == 3 or y == G + 8 or (abs(x - C) == 2 and y == G + 7)):
                b.set(x, y, 111, ORANJE)
    # a shield with the Mika's face over the gate, horns and all
    for r, rij in enumerate(["B.....B", "BPPPPPB", ".PZPZP.", ".PPNPP.", ".PWZWP.", "..PPP.."]):
        for c, ch in enumerate(rij):
            blok = {"P": "minecraft:pink_terracotta", "Z": "minecraft:black_concrete", "N": "minecraft:pink_concrete", "W": "minecraft:quartz_block",
                    "B": "minecraft:bone_block"}.get(ch)
            if blok:
                b.set(C - 3 + c, G + 19 - r, 111, blok)
    for x in (59, 66):                                            # a banner on each side of it
        _vaandel(b, x, G + 11, 112, 1, 0, hoog=8)
    b.sign(C - 3, G + 3, 112, "south", ["guhrio.kasteel.bord.1", "guhrio.kasteel.bord.2", "guhrio.kasteel.bord.3"])
    b.hang_lantern(C, G + 8, 107, length=1)


def voorplein(b):
    """The forecourt: a deck on pillars in the sauce in front of the gate, with a balustrade and steps down to the sea."""
    x0, x1, z0, z1 = 44, 82, 112, 123
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G - 1, z, STENEN)
            b.set(x, G, z, ZWART if (C - 2 <= x <= C + 2) else ORANJE if x in (x0, x1) or z == z1
                  else "minecraft:polished_andesite" if (x // 2 + z // 2) % 2 == 0 else STENEN)
    b.air(x0, G + 1, z0, x1, G + 14, 126)
    for x in range(x0, x1 + 1, 6):
        for z in (z0 + 2, z1):
            _steen(b, x, 0, z, x, G - 2, z)
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            b.wall(x, G + 1, z)
    for x in range(x0, x1 + 1):
        if not C - 2 <= x <= C + 2:
            b.wall(x, G + 1, z1)
    for x, z in ((x0, z1), (x1, z1), (x0, z0 + 3), (x1, z0 + 3), (C - 3, z1), (C + 3, z1)):
        b.set(x, G + 1, z, STENEN)
        _paal(b, x, G + 2, z, hoog=2)
    for i, z in enumerate((124, 125, 126)):                       # the dock: three steps down to the sauce
        for x in range(C - 2, C + 3):
            b.stair(x, G - 1 - i, z, "north")
            _steen(b, x, 0, z, x, G - i - 2, z)
    return (x0, G, z1), (x1 - x0 + 1, 12, z1 - z0 + 1)


# =====================================================================================================================
# the default decoration of the slots a reward slice may fill
# =====================================================================================================================
def proef_voorplein(plek):
    """The forecourt as the engine leaves it: a welcome sign, two pipes and a flagpole for the look."""
    plek.zet(13, 1, 2, ORANJE)
    plek.zet(13, 2, 2, ORANJE)
    plek.bord(13, 2, 1, "uit", ["Welkom bij het", "Kasteel van de", "Grote Nether-Mika", "Njeg njeg njeg!"], "guhrio.voorplein.welkom")
    for x in (8, 30):
        for k in range(2):
            plek.zet(x, 1 + k, 8, gb.PIJP_LIJF, {"axis": "y"})
        plek.zet(x, 3, 8, gb.PIJP, {"kanaal": "15", "facing": "up", "ingang": "false", "boven": "false"})
    plek.zet(25, 1, 2, "guhs:guhrio_blok")
    for k in range(5):
        plek.zet(25, 2 + k, 2, gb.MAST, {"top": "true" if k == 4 else "false"})


def proef_torenkamer(plek):
    """The tower room as the engine leaves it: a carpet, two lanterns and a sign that the princess is somewhere else."""
    for x in range(8, 13):
        for z in range(2, 11):
            plek.zet(x, 1, z, "minecraft:pink_carpet")
    for x in (3, 17):
        plek.zet(x, 1, 10, "minecraft:polished_blackstone")
        plek.zet(x, 2, 10, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    plek.zet(10, 1, 12, "minecraft:polished_blackstone")
    plek.zet(10, 2, 12, "minecraft:polished_blackstone")


# =====================================================================================================================
# the levels
# =====================================================================================================================
def achtergrond(thema, rng):
    """The theme's own painted wall: (s, y) -> (block, props) for the cells of d = -1 a level left alone."""
    wolken = [(rng.randrange(4, LEVEL_L - 4), rng.randrange(12, SLOT_H - 2)) for _ in range(10)]

    def tuin(s, y):
        heuvel = 5 + int(2.5 * math.sin(s / 7.0) + 1.8 * math.sin(s / 3.1 + 1))
        if y <= heuvel:
            return ("minecraft:green_concrete" if y <= heuvel - 3 else "minecraft:lime_concrete", None)
        for cx, cy in wolken:
            if abs(s - cx) <= 2 and (y == cy or (y == cy + 1 and abs(s - cx) <= 1)):
                return ("minecraft:white_concrete", None)
        return ("minecraft:light_blue_concrete", None)

    def kelder(s, y):
        if s % 12 in (0, 1):
            return ("minecraft:polished_deepslate", None)
        if y in (0, SLOT_H - 1):
            return ("minecraft:deepslate_tiles", None)
        k = (s * 7 + y * 13 + (s // 3) * (y // 2)) % 23
        return ("minecraft:cracked_deepslate_bricks" if k == 0 else "minecraft:mossy_cobblestone" if k == 5 and y < 4 else "minecraft:deepslate_bricks", None)

    def burcht(s, y):
        if s % 12 in (5, 6) and 8 <= y <= 11:
            return (LICHT, None)                                  # a glowing window
        if s % 12 in (4, 7) and 7 <= y <= 12 or s % 12 in (5, 6) and y in (7, 12):
            return ("minecraft:chiseled_nether_bricks", None)
        if y % 7 == 0:
            return (DAK, None)
        return ("minecraft:cracked_nether_bricks" if (s * 5 + y * 11) % 29 == 0 else "minecraft:nether_bricks", None)

    return {1: tuin, 2: kelder, 3: burcht}[thema]


def _slot(naam):
    """What the four slice modules export under this name, in FEATURES order."""
    uit = []
    for module in MODULES:
        try:
            m = importlib.import_module(f"features.{module}")
        except ModuleNotFoundError as e:
            if e.name != f"features.{module}":
                raise                                             # (the slice's module is there but broken: say so)
            continue
        if getattr(m, naam, None) is not None:
            uit.append(getattr(m, naam))
    return uit


def _verlicht(b, baan, lengte, hoogte):
    """Invisible lamps in the camera's room, so a level is evenly lit (they are air to everything else)."""
    for s in range(1, lengte, 4):
        for y in range(2, hoogte, 5):
            x, yy, z = baan.bouw(s, y, 2)
            if b.get(x, yy, z) == AIR:
                b.set(x, yy, z, LAMP[0], LAMP[1])


def levels(h, b, poorten):
    """Stamps every level into its slot and writes its lane file; returns the lane builders."""
    eigen = {}
    for fn in _slot("LEVELS"):
        eigen.update(fn)
    banen = {}
    vorige = None
    for nr, (wereld, thema, kant) in enumerate(LEVELS):
        v = VLEUGEL[kant]
        oorsprong, richting = ((v["baan"], Y0[thema], LZ0), (0, 1)) if kant == "west" else ((v["baan"], Y0[thema], LZ1), (0, -1))
        baan = gb.Baanbouwer(h, level_id(wereld), wereld, nr, thema, LEVEL_L, SLOT_H, oorsprong, richting)
        (eigen.get(wereld) or guhrio_proef.LEVELS[wereld])(baan)
        _stempel(h, b, baan, poorten[wereld], HAL_UIT, vorige, duel=False)
        _verlicht(b, baan, LEVEL_L, SLOT_H)
        banen[wereld] = baan
        vorige = baan.id
    duels = _slot("DUEL")
    baan = gb.Baanbouwer(h, "kasteel_duel", "DUEL", len(LEVELS), 3, DUEL_L, DUEL_H, (43, DUEL_Y0, 39), (1, 0))
    (duels[-1] if duels else guhrio_proef.DUEL)(baan)
    _stempel(h, b, baan, poorten["duel"], TOREN_UIT, vorige, duel=True)
    _verlicht(b, baan, DUEL_L, DUEL_H)
    banen["duel"] = baan
    return banen


def _stempel(h, b, baan, poort, uitgang, na, duel):
    baan.controleer(duel=duel)
    kleur = achtergrond(baan.thema, baan.rng)
    baan.stempel(b.set, b.s.entity, kleur)
    gb.level_json(h, baan.id, baan.banen(), baan.wereld, baan.naar_eigen(uitgang), baan.naar_eigen(poort[2]), na)
    # the gate in the hall: two blocks that know where the level's start block is, counted in the gate's own frame
    (px, py, pz), kijkt, _voor = poort
    fx, fz = {"west": (-1, 0), "east": (1, 0), "north": (0, -1), "south": (0, 1)}[kijkt]
    sx, sy, sz = baan.bouw(*baan._start)
    for dy in (0, 1):
        dx, dyy, dz = sx - px, sy - (py + dy), sz - pz
        b.set(px, py + dy, pz, gb.POORT, {"facing": kijkt}, {"id": "guhs:guhrio_start", "Level": baan.id, "NaarX": dx * fx + dz * fz,
                                                             "NaarY": dyy, "NaarZ": -dx * fz + dz * fx})


def verlicht_binnen(b):
    """Invisible lamps where you walk indoors (the hall, the stairs, the tower room and its corridor, the gate): no dark
    corners. Put in last: they are air to everything, but not to the checks of the shell."""
    def lamp(x, y, z):
        if b.get(x, y, z) == AIR:
            b.set(x, y, z, LAMP[0], LAMP[1])
    for x in range(44, 84, 6):
        for z in range(39, 80, 6):
            lamp(x, G + 5, z)
    for x in range(70, 84, 4):
        lamp(x, G + 4 + (x - 68), 78)
    for x in range(55, 73, 6):
        for z in range(60, 70, 4):
            lamp(x, 47, z)
    for x in range(64, 84, 5):
        lamp(x, 45, 74)
    for z in (104, 107, 110):
        lamp(C, G + 5, z)


def controleer_camera(b, banen):
    """The camera's room of every lane must be empty (air or an invisible lamp): a wall there hides the level."""
    fouten = []
    for baan in banen.values():
        for s in range(baan.L):
            for y in range(baan.H):
                for d in range(1, baan.AFSTAND + 1):
                    blok = b.get(*baan.bouw(s, y, d))
                    if blok not in (AIR, LAMP[0]):
                        fouten.append(f"{NAAM}: {blok} in the camera's room of {baan.wereld} at s={s} y={y} d={d}")
    return fouten[:20]


HANGT = ("wall_sign", "wall_hanging_sign", "wall_banner", "wall_torch", "ladder", "lever", "_button", "tripwire_hook", "cocoa", "_bed")
# the pieces a block entity draws (GuhrioBlocks.GetekendStuk: dynamicShape, so the game never calls them solid): whatever hangs
# on one of these falls off at the first block update (found on a dev server: the welcome sign of the forecourt was gone)
NIET_VAST = ("guhs:guhrio_steen", "guhs:guhrio_vraagblok", "guhs:guhrio_onzichtbaar", "guhs:guhrio_schakelaar", "guhs:guhrio_schakelblok")
STAP = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}


def controleer_naden(b):
    """
    A guhs:burcht is placed tile by tile and chunk by chunk, and a block whose support (or other half) lies in a bit that
    is placed later breaks off (found by the paleizen slice on real copies: ladders and bed halves gone). The seams are the
    tile edges and the chunk edges; the anchor stands on block 8 of its chunk and a turned copy shifts the edge by one. So
    nothing that hangs on a wall (signs, ladders, banners, torches, buttons...) and no bed may straddle such a pair of
    columns - in the shell, and in whatever a level or a room of another slice put into the castle.
    """
    def naad(c, anker):
        return (c - anker) % 16 in (7, 8) or c % sb.TILE == sb.TILE - 1

    fouten = []
    for (x, y, z), (naam, props, _nbt) in b.s.blocks.items():
        if not any(k in naam for k in HANGT) or "facing" not in props or props["facing"] not in STAP:
            continue
        dx, dz = STAP[props["facing"]]
        if "_bed" in naam:
            if props.get("part") != "foot":
                continue                                         # (the head lies one further the way the bed faces)
        else:
            dx, dz = -dx, -dz                                     # (its support is behind it)
        if "_bed" not in naam and (b.get(x + dx, y, z + dz) or "") in NIET_VAST:
            fouten.append(f"{NAAM}: {naam} at {(x, y, z)} hangs on {b.get(x + dx, y, z + dz)}, a drawn piece that is not solid for the game: "
                          f"put a plain block behind it (guhs:guhrio_siersteen has the brick's look)")
        c, anker = (min(x, x + dx), ANKER[0]) if dx else (min(z, z + dz), ANKER[2])
        if naad(c, anker):
            fouten.append(f"{NAAM}: {naam} at {(x, y, z)} hangs across a tile / chunk seam (its other block is at {(x + dx, y, z + dz)}): "
                          f"move it one or two blocks")
    return fouten[:20]


# vanilla plants that break off at the first block update unless they stand on earth (found with /guhs bouwcheck compleet
# at the merge of the level slices: the three flowering azaleas of the tower room stood on the room's floor and were gone
# in every real copy). A slice's own plant blocks (guhs:guhriow1_bloem) decide for themselves and are not looked at.
PLANT = ("minecraft:azalea", "minecraft:flowering_azalea", "minecraft:fern", "minecraft:large_fern", "minecraft:short_grass", "minecraft:tall_grass",
         "minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:oxeye_daisy",
         "minecraft:cornflower", "minecraft:lily_of_the_valley", "minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush", "minecraft:peony",
         "minecraft:sweet_berry_bush", "minecraft:pink_petals")
PLANT_EINDE = ("_sapling", "_tulip")
AARDE = ("minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:rooted_dirt", "minecraft:moss_block",
         "minecraft:mud", "minecraft:muddy_mangrove_roots", "minecraft:mycelium", "minecraft:farmland")


def controleer_planten(b):
    """A vanilla plant must stand on earth (an azalea also on clay), in the shell and in whatever a slice put into a slot."""
    fouten = []
    for (x, y, z), (naam, props, _nbt) in b.s.blocks.items():
        if not (naam in PLANT or (naam.startswith("minecraft:") and "potted_" not in naam and naam.endswith(PLANT_EINDE))):
            continue
        if props and props.get("half") == "upper":
            continue                                             # (the top half of a tall plant stands on its bottom half)
        onder = b.get(x, y - 1, z) or "minecraft:air"
        if onder not in AARDE and not ("azalea" in naam and onder == "minecraft:clay"):
            fouten.append(f"{NAAM}: {naam} at {(x, y, z)} stands on {onder} and will break off in a real copy: put earth or moss "
                          f"under it (minecraft:moss_block), or use a potted plant")
    return fouten[:20]


# =====================================================================================================================
def bouw(h):
    """Builds the whole castle; returns (Bouw, lane builders, problems)."""
    b = sb.Bouw(h, (W, H, D), 21302302)
    binnenplaats(b)
    hoektorens(b)
    for kant in VLEUGEL:
        vleugel(b, kant)
    donjon(b)
    poorten = hal(b)
    duelverdieping(b)
    hoek, maat = voorplein(b)
    poortgebouw(b)
    # the walks the shell must allow (checked before the levels go in: their coins float on purpose)
    b.must_reach = {"de hal": HAL_UIT, "de torenkamer": TOREN_UIT, "de duelpoort": poorten["duel"][2]}
    for wereld, (_p, _k, voor) in poorten.items():
        b.must_reach[f"poort {wereld}"] = voor
    b.connect()
    # (the two ?-blocks that hover over the little hills in the yard float on purpose)
    problems = [p for p in sb.check_common(b, NAAM) if "guhrio_vraagblok" not in p] + sb.check_paths(b, NAAM, (C, G + 1, 118))
    # the slots of the reward slice (and anybody's extras in the hall)
    plekken = {
        "VOORPLEIN": (gb.Plekbouwer(h, "voorplein", (39, 12, 12), hoek, (1, 0), b.set, b.s.entity, b.get, vrij=[(17, 0, 21, 11)]), proef_voorplein),
        "TORENKAMER": (gb.Plekbouwer(h, "torenkamer", (21, 7, 13), (53, 42, 70), (1, 0), b.set, b.s.entity, b.get, vrij=[(9, 0, 11, 2)]),
                       proef_torenkamer),
        "HAL": (gb.Plekbouwer(h, "hal", (43, 12, 43), (42, G, 79), (1, 0), b.set, b.s.entity, b.get,
                              vrij=[(19, 0, 23, 42), (0, 9, 3, 13), (0, 19, 3, 23), (0, 29, 3, 33), (39, 9, 42, 13), (39, 19, 42, 23),
                                    (39, 29, 42, 33), (26, 0, 42, 2)]), None),
    }
    for naam, (plek, standaard) in plekken.items():
        fns = _slot(naam)
        for fn in (fns if fns else ([standaard] if standaard else [])):
            fn(plek)
    banen = levels(h, b, poorten)
    verlicht_binnen(b)
    problems += controleer_camera(b, banen) + controleer_naden(b) + controleer_planten(b)
    return b, banen, problems


def build(h):
    from features import wereld
    b, banen, problems = bouw(h)
    if problems:
        print(f"{NAAM} geometry check found problems:\n  " + "\n  ".join(problems[:60]))
        raise SystemExit(f"guhrio: fix the castle ({len(problems)} problems, see above)")
    nx, nz, _saved = b.save_tiles(NAAM)
    wereld.bbq_structuur(h, NAAM, soort="burcht", titel="Kasteel van de Grote Nether-Mika",
                         tooltip="Super Guhrio: zes levels van opzij, een duel en een ontvoerde prinses (Guhbarbecuether)",
                         biomes=wereld.BBQ, salt=21302301, spacing=44, separation=18,
                         gegarandeerd=dict(sector=11, min=400, max=1100), voorrang=290,
                         burcht=dict(placement="paleis", tiles_x=nx, tiles_z=nz, tile_size=sb.TILE, anchor=ANKER, min_y=33, max_y=33, reach=48),
                         spawns=None)
    # nothing spawns inside the castle (its tiles): the levels bring their own creatures, and they only shove
    h.patch_json(f"{h.D}/worldgen/structure/{NAAM}.json", lambda d: d.update(spawn_overrides={
        "monster": {"bounding_box": "piece", "spawns": []}, "creature": {"bounding_box": "piece", "spawns": []}}))
    return b, banen
