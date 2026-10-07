"""
einde2 (bouw_zij1): the Westelijke Luchttempel of the Kruimelwolken (a guhs:leegte_bouw; the build only: the luchtnon, the
strayed herd and the side quest are a later phase and stand as markers in the sidecar).

The temple hangs UPSIDE DOWN under its island. On top there is only grass, a fountain, the landing place and a little
pavilion over "the cellar stairs". Those wind down through the rock into the hanging hall; from the hall four wind
corridors (closed bridges on chains) lead to four pagodas that hang with their points down, and under the hall hangs
the big middle pagoda with the fifth wind corridor: the organ pipe, a shaft straight down through all its floors.
Inside the island, on the north side, the stable of the luchtbizon-guhs: five pens that open to the void.

Everything is laid out from the middle C; T = the top block layer of the island, HAL = the floor layer of the hanging hall.
"""
import math

import features.einde2_kit as kit
from features import guheinde_bouw as gb
from features import einde2_bouw_zij1_gereedschap as g

NAAM = "westelijke_luchttempel"
W, H, D = 112, 84, 112
Y = 150
C = 56
T = 70
HAL = 45
R_EILAND, R_HAL = 46.0, 15.5
ROTS, GROND, STENEN, TRAP, PLAAT, WAL = "guhs:kruimelrots", "guhs:kaaskorst", "guhs:kruimelrots_stenen", "guhs:kruimelrots_stenen_trap", \
    "guhs:kruimelrots_stenen_plaat", "guhs:kruimelrots_stenen_muur"
MUUR, DAKSTEEN, DAKTRAP, DAKPLAAT = "calcite", "prismarine_bricks", "prismarine_brick_stairs", "prismarine_brick_slab"
RAAM, GLAS = "cyan_stained_glass_pane", "cyan_stained_glass"
WOLK, DICHT = "guhs:kruimelwolk", "guhs:dichte_kruimelwolk"
PAGODE = 34                   # the four outer pagodas hang this far from the middle
STAL = (37, 75, 19, 25)       # the pens: x0, x1, z0, z1 (inside); the aisle lies south of them
STAL_Y = 62                   # the floor layer of the stable
RICHTING = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}


def vorm():
    rand, onder = g.ruis("luchttempel_rand", 12.0), g.ruis("luchttempel_onder", 6.0)

    def v(x, z):
        d = math.hypot(x - C, z - C) / (R_EILAND + rand(x, z) * 2.5)
        if d >= 1.0:
            return None
        return T, int(round(T - (6.0 + 10.0 * (1.0 - d * d) ** 0.5) - onder(x, z) * 1.8))
    return v


def onderkant(b, x, z, vanaf=HAL + 6):
    """The lowest block of the island in this column, looking up from `vanaf` (None: no island there)."""
    for y in range(vanaf, T + 1):
        if (x, y, z) in b.s.blocks:
            return y
    return None


def ketting(b, x, y, z):
    """A chain from the block (x, y, z) up to the rock of the island."""
    o = onderkant(b, x, z)
    if o is not None and all((x, q, z) not in b.s.blocks for q in range(y + 1, o)):
        for q in range(y + 1, o):
            b.set(x, q, z, "chain", {"axis": "y", "waterlogged": "false"})


def hal(b):
    """The hanging hall under the middle of the island: round, with a ring of windows and a door on each wind side."""
    for x in range(C - 18, C + 19):
        for z in range(C - 18, C + 19):
            r = math.hypot(x - C, z - C)
            if r <= R_HAL:
                b.set(x, HAL - 1, z, DAKSTEEN)
                b.set(x, HAL, z, "orange_terracotta" if int(r + 0.5) in (6, 11) else STENEN)
                o = onderkant(b, x, z) or T
                if r > R_HAL - 1.0:
                    hoek = math.degrees(math.atan2(z - C, x - C)) % 30
                    for y in range(HAL + 1, HAL + 8):
                        b.set(x, y, z, GLAS if 7 < hoek < 23 and HAL + 2 <= y <= HAL + 5 else MUUR)
                else:
                    for y in range(HAL + 1, HAL + 8):
                        b.set(x, y, z, gb.AIR)
                for y in range(HAL + 8, o):
                    b.set(x, y, z, MUUR if y == HAL + 8 else ROTS)
            elif r <= R_HAL + 2.0:
                b.set(x, HAL, z, DAKPLAAT, gb.slab("top"))               # (the eave: a hanging roof flares at the floor)
    for dx, dz in RICHTING.values():
        for k in (-1, 0, 1):
            x, z = C + dx * 15 + dz * k, C + dz * 15 + dx * k
            for y in range(HAL + 1, HAL + 5):
                b.set(x, y, z, gb.AIR)
            b.set(x, HAL + 5, z, STENEN)
    # hanging light rods, mats to sit on and a few shelves between the windows
    for a in range(0, 360, 45):
        x, z = int(round(C + 10 * math.cos(math.radians(a + 22)))), int(round(C + 10 * math.sin(math.radians(a + 22))))
        g.staaf(b, x, HAL + 7, z, "down")
        b.set(x, HAL + 1, z, "orange_carpet" if a % 90 else "yellow_carpet")
    for x, z in ((C - 10, C - 9), (C + 10, C - 9), (C - 10, C + 9), (C + 10, C + 9)):
        b.fill(x, HAL + 1, z, x, HAL + 2, z, "bookshelf")
        b.set(x, HAL + 3, z, "potted_azure_bluet")
    b.sign(C + 4, HAL + 1, C + 6, "south", ["Welkom in de", "Luchttempel.", "Niet omvallen:", "u hangt al"], wall=False)
    b.plek("luchtnon", C + 2, HAL + 1, C + 8, yaw=0.0, wie="de luchtnon-guh, bij de trap in de hangende hal", kind="LUCHTNON_GUH")
    b.wezen("lemur_1", C - 5, HAL + 1, C + 9, "guhs:vleermuis_lemurguh", yaw=-40.0, honk=8)


def keldertrap(b):
    """The winding stair from the pavilion on top down into the hall: a stone core, the steps around it, a wall around
    those. Every corner is a landing. It comes out on the south side of the pillar in the middle of the hall."""
    b.fill(C - 3, HAL + 1, C - 3, C + 3, T, C + 3, MUUR)
    b.fill(C - 2, HAL + 1, C - 2, C + 2, T - 1, C + 2, gb.AIR)      # (the top layer stays: a lid, but for the first steps)
    b.fill(C - 1, HAL + 1, C - 1, C + 1, T, C + 1, STENEN)
    for y in range(HAL + 3, T, 4):
        b.set(C - 1, y, C, "sea_lantern")
        b.set(C + 1, y, C, "sea_lantern")
        b.set(C, y, C - 1, "sea_lantern")
        b.set(C, y, C + 1, "sea_lantern")
    ring = [(C - 1, C + 2), (C - 2, C + 2)] + [(C - 2, z) for z in range(C + 1, C - 3, -1)] + [(x, C - 2) for x in range(C - 1, C + 3)] + \
        [(C + 2, z) for z in range(C - 1, C + 3)] + [(C + 1, C + 2), (C, C + 2)]
    stand, x, z = T + 1, C, C + 2
    b.set(x, T, z, STENEN)
    i = 0
    while stand > HAL + 1:
        px, pz = x, z
        x, z = ring[i % 16]
        i += 1
        hoek = abs(x - C) == 2 and abs(z - C) == 2
        if not hoek:
            stand -= 1
        if stand - 1 > HAL:
            if hoek:
                b.set(x, stand - 1, z, STENEN)
            else:
                naar = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(px - x, pz - z)]
                b.set(x, stand - 1, z, TRAP, gb.stair(naar))
        for y in range(stand, stand + 3):
            b.set(x, y, z, gb.AIR)
    b.fill(x, HAL + 1, C + 3, x, HAL + 3, C + 3, gb.AIR)              # (the way out into the hall)
    b.set(x, HAL + 4, C + 3, STENEN)
    b.sign(x + 2, HAL + 3, C + 4, "south", ["Naar boven:", "het gras, de", "fontein en de", "frisse leegte"], wall=True)
    # the pavilion over the stairs: four pillars, a roof with its point UP (the only thing here that stands straight)
    for px, pz in ((C - 3, C - 3), (C + 3, C - 3), (C - 3, C + 3), (C + 3, C + 3)):
        b.fill(px, T + 1, pz, px, T + 4, pz, STENEN)
    for i in range(C - 3, C + 4):
        for a, c in ((i, C - 3), (C - 3, i), (C + 3, i), (i, C + 3)):
            if (a, c) != (C, C + 3) and (a, T + 1, c) not in b.s.blocks:
                b.set(a, T + 1, c, WAL)
    b.fill(C - 3, T + 5, C - 3, C + 3, T + 5, C + 3, MUUR)
    tip = g.piramidedak(b, C - 4, C - 4, C + 4, C + 4, T + 5, DAKTRAP, DAKPLAAT, top=DAKSTEEN)
    g.staaf(b, C, tip + 1, C)
    g.staaf(b, C, T + 4, C, "down")
    b.sign(C + 1, T + 1, C + 4, "south", ["Luchttempel", "Ingang: omlaag.", "Ja, echt.", "Niet vragen"], wall=False)
    b.sign(C - 1, T + 1, C + 4, "south", ["Kelder =", "bovenverdieping", "Zolder = kelder", "Snap je? Njeg"], wall=False)


def gang(b, naam, nr):
    """A wind corridor: a closed bridge from a door of the hall to the pagoda on that side, hung on chains."""
    dx, dz = RICHTING[naam]
    px, pz = dz, dx                                  # (across the corridor)
    for i in range(16, PAGODE - 6):
        for k in range(-2, 3):
            x, z = C + dx * i + px * k, C + dz * i + pz * k
            rand = abs(k) == 2
            b.set(x, HAL, z, MUUR if rand else ("orange_terracotta" if k == 0 else STENEN))
            b.set(x, HAL + 5, z, DAKSTEEN)
            for y in range(HAL + 1, HAL + 5):
                if rand:
                    b.set(x, y, z, RAAM if i % 3 == 0 and HAL + 2 <= y <= HAL + 3 else MUUR)
                else:
                    b.set(x, y, z, gb.AIR)
        for k, naar in ((-3, (px, pz)), (3, (-px, -pz))):                 # (an eave under each side, upside down)
            x, z = C + dx * i + px * k, C + dz * i + pz * k
            facing = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[naar]
            b.set(x, HAL, z, DAKTRAP, gb.stair(facing, "top"))
        if i % 4 == 1:
            ketting(b, C + dx * i + px * 2, HAL + 5, C + dz * i + pz * 2)
            ketting(b, C + dx * i - px * 2, HAL + 5, C + dz * i - pz * 2)
            g.staaf(b, C + dx * i, HAL + 4, C + dz * i, "down")
    a, e = 16, PAGODE + 8
    x0, x1 = sorted((C + dx * a - abs(px), C + dx * e + abs(px)))
    z0, z1 = sorted((C + dz * a - abs(pz), C + dz * e + abs(pz)))
    b.zone(f"windgang_{nr}", x0, HAL + 1, z0, x1, HAL + 4, z1, wie=f"windgang {nr}: van de hal naar de monding aan de {naam}kant")


def binnentrap(b, x, z, richting, y_laag, y_hoog):
    g.binnentrap(b, x, z, richting, y_laag, y_hoog, TRAP, WAL)


def pagode(b, cx, cz, vloeren, helften, binnenkant=None, punt=True, trapkant="west"):
    """A pagoda that hangs with its point down: storeys that get smaller towards the bottom, an eave at every floor, and
    under the last one a point with a light rod. vloeren = the floor layers from the top one down, helften = the half
    sizes. binnenkant(k, x0, z0, x1, z1, y) furnishes storey k. Returns the y of the tip."""
    for k, (y, half) in enumerate(zip(vloeren, helften)):
        x0, x1, z0, z1 = cx - half, cx + half, cz - half, cz + half
        top = vloeren[k - 1] if k else y + 6
        b.fill(x0, y, z0, x1, y, z1, STENEN)
        for yy in range(y + 1, top):
            g.doos(b, x0, yy, z0, x1, yy, z1, MUUR)
            b.fill(x0 + 1, yy, z0 + 1, x1 - 1, yy, z1 - 1, gb.AIR)
            for px, pz in ((x0, z0), (x0, z1), (x1, z0), (x1, z1)):
                b.set(px, yy, pz, STENEN)
        if k == 0:
            b.fill(x0, top, z0, x1, top, z1, MUUR)
        for i in range(-1, 2):                       # (a window in the middle of every side)
            for yy in (y + 2, y + 3):
                b.set(cx + i, yy, z0, RAAM)
                b.set(cx + i, yy, z1, RAAM)
                b.set(x0, yy, cz + i, RAAM)
                b.set(x1, yy, cz + i, RAAM)
        for i in range(x0 - 1, x1 + 2):               # (the eave at the floor)
            b.set(i, y, z0 - 1, DAKTRAP, gb.stair("south", "top"))
            b.set(i, y, z1 + 1, DAKTRAP, gb.stair("north", "top"))
        for i in range(z0, z1 + 1):
            b.set(x0 - 1, y, i, DAKTRAP, gb.stair("east", "top"))
            b.set(x1 + 1, y, i, DAKTRAP, gb.stair("west", "top"))
        g.staaf(b, cx - 2, top - 1, cz - 2, "down")
        g.staaf(b, cx + 2, top - 1, cz + 2, "down")
        if k:                                        # (a stair up to the storey above, along one wall, turning back each storey)
            s = 1 if k % 2 else -1
            if trapkant == "west":
                binnentrap(b, cx - (half - 1), cz + s * (half - 1), "north" if s > 0 else "south", y, vloeren[k - 1])
            else:
                binnentrap(b, cx + s * (half - 1), cz - (half - 1), "west" if s > 0 else "east", y, vloeren[k - 1])
        if binnenkant:
            binnenkant(k, x0, z0, x1, z1, y)
    if not punt:
        return None
    y, half = vloeren[-1] - 1, helften[-1]
    tip = g.piramidedak(b, cx - half, cz - half, cx + half, cz + half, y, DAKTRAP, DAKPLAAT, top=DAKSTEEN, half="top")
    g.staaf(b, cx, tip - 1, cz, "down")
    return tip


def buitenpagodes(b):
    """The four pagodas at the ends of the wind corridors; each has the mouth of its corridor on the far side."""
    inhoud = {
        "north": (["De bieb", "Alle boeken", "liggen op het", "plafond"], "bookshelf"),
        "east": (["De bakkerij", "Taart gooien", "mag alleen op", "feestdagen"], "guhs:guh_taart"),
        "south": (["De slaapzaal", "Hangmatten.", "Wat dacht je", "dan? Njeg"], "white_wool"),
        "west": (["Stiltekamer", "Sssst.", "(de wind mag", "wel praten)"], "orange_carpet"),
    }
    for nr, naam in enumerate(("north", "east", "south", "west"), start=1):
        dx, dz = RICHTING[naam]
        px, pz = dz, dx
        cx, cz = C + dx * PAGODE, C + dz * PAGODE
        tekst, ding = inhoud[naam]

        def binnen(k, x0, z0, x1, z1, y, naam=naam, tekst=tekst, ding=ding, cx=cx, cz=cz, dx=dx, dz=dz, px=px, pz=pz):
            if k == 0:
                return
            for i, (ax, az) in enumerate(((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1))):
                vrij = (ax, y + 1, az) not in b.s.blocks or b.s.blocks[(ax, y + 1, az)][0] == gb.AIR
                if vrij and (i + k) % 2 == 0:
                    if ding == "orange_carpet":
                        b.set(ax, y + 1, az, ding)
                    elif ding == "guhs:guh_taart":
                        b.set(ax, y + 1, az, "guhs:guh_tafel", {"facing": "north"})
                        b.set(ax, y + 2, az, "cake", {"bites": str(i)})
                    else:
                        b.fill(ax, y + 1, az, ax, y + 1 + (i % 2), az, ding)
            if k == 1:
                b.sign(cx, y + 1, cz, "south" if dz <= 0 else "north", tekst, wall=False)

        pagode(b, cx, cz, (HAL, HAL - 6, HAL - 12), (6, 5, 4), binnen, trapkant="west" if dx == 0 else "north")
        # the stem that holds it to the island
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                o = onderkant(b, x, z, HAL + 7)
                if o is not None:
                    for y in range(HAL + 7, o):
                        b.set(x, y, z, ROTS if (abs(x - cx) < 4 and abs(z - cz) < 4) else STENEN)
        # the corridor comes in on the inner side; the mouth of the flute is a balcony on the outer side
        for k in (-1, 0, 1):
            for i, a in ((-6, gb.AIR), (6, gb.AIR)):
                x, z = cx + dx * i + px * k, cz + dz * i + pz * k
                for y in range(HAL + 1, HAL + 5):
                    b.set(x, y, z, a)
        for i in (7, 8, 9):
            for k in range(-2, 3):
                x, z = cx + dx * i + px * k, cz + dz * i + pz * k
                b.set(x, HAL, z, STENEN if abs(k) < 2 and i < 9 else DAKSTEEN)
                b.set(x, HAL - 1, z, DAKPLAAT, gb.slab("top"))
                if i == 9 or abs(k) == 2:
                    b.set(x, HAL + 1, z, WAL)
        for k, naar in ((-2, (px, pz)), (2, (-px, -pz))):                 # (the horn: stairs that open outwards)
            x, z = cx + dx * 6 + px * k, cz + dz * 6 + pz * k
            facing = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[naar]
            for y in (HAL + 1, HAL + 4):
                b.set(x, y, z, DAKTRAP, gb.stair(facing, "bottom" if y == HAL + 1 else "top"))
        b.set(cx + dx * 6, HAL + 5, cz + dz * 6, "orange_terracotta")
        b.prop(f"windfluit_{nr}", cx + dx * 7, HAL + 1, cz + dz * 7, wie=f"de monding van windgang {nr} (het balkon aan de {naam}kant)")
        b.sign(cx + dx * 5 + px * 3, HAL + 1, cz + dz * 5 + pz * 3, gb.OPP[naam], [f"Windfluit {nr}", "Fluit niet.", "Al heel lang", "niet. Zonde"],
               wall=False)
    b.wezen("lemur_2", C + 2, HAL - 11, C - PAGODE + 1, "guhs:vleermuis_lemurguh", yaw=180.0, honk=3)


def middenpagode(b):
    """The big pagoda under the hall, with the fifth wind corridor: the organ pipe, a shaft through all its floors."""
    vloeren, helften = (HAL - 7, HAL - 13, HAL - 19, HAL - 25), (8, 7, 6, 5)
    teksten = [["Klaslokaal", "Les 1: zweven", "Les 2: landen", "(les 2 valt uit)"], ["Kleedkamer", "Pijltjes op je", "kop? Hier, met", "een stempel"],
               ["Gongkamer", "De gong is weg.", "Wie hem vindt:", "niet slaan"], ["De orgelpijp", "Kijk omlaag.", "Niet te lang.", "Njeg"]]

    def binnen(k, x0, z0, x1, z1, y):
        b.fill(C - 1, y, C - 1, C + 1, y, C + 1, gb.AIR)                 # (the pipe)
        for i in range(C - 2, C + 3):
            for a, c in ((i, C - 2), (i, C + 2), (C - 2, i), (C + 2, i)):
                b.set(a, y + 1, c, WAL)
        b.sign(C + 3, y + 1, C - 3, "south", teksten[k], wall=False)
        for ax, az in ((x0 + 1, z1 - 1), (x1 - 1, z0 + 1)):
            if (ax, y + 1, az) not in b.s.blocks or b.s.blocks[(ax, y + 1, az)][0] == gb.AIR:
                b.set(ax, y + 1, az, "bookshelf" if k % 2 == 0 else "orange_wool")
                b.set(ax, y + 2, az, "candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
        b.set(C, y + 1, z0 + 1, "yellow_carpet")
        b.set(C, y + 1, z1 - 1, "orange_carpet")

    pagode(b, C, C, vloeren, helften, binnen, punt=False)
    b.fill(C - 8, HAL - 1, C - 8, C + 8, HAL - 1, C + 8, DAKSTEEN)
    # the stair from the hall floor down into the first storey
    binnentrap(b, C + 7, C + 7, "north", vloeren[0], HAL)
    b.sign(C + 5, HAL + 1, C + 8, "south", ["Naar de zolder", "(die hangt", "onder de", "kelder)"], wall=False)
    # the point is a funnel: the pipe ends in an open mouth
    y, half = vloeren[-1] - 1, helften[-1]
    for k in range(4):
        h = half - k
        b.fill(C - h, y - k, C - h, C + h, y - k, C + h, DAKSTEEN)
        for i in range(C - h, C + h + 1):
            b.set(i, y - k, C - h, DAKTRAP, gb.stair("south", "top"))
            b.set(i, y - k, C + h, DAKTRAP, gb.stair("north", "top"))
            b.set(C - h, y - k, i, DAKTRAP, gb.stair("east", "top"))
            b.set(C + h, y - k, i, DAKTRAP, gb.stair("west", "top"))
    b.fill(C - 1, y - 3, C - 1, C + 1, y, C + 1, gb.AIR)
    for q in (-2, 2):
        g.staaf(b, C + q, y - 4, C, "down")
        g.staaf(b, C, y - 4, C + q, "down")
    b.zone("windgang_5", C - 1, y - 3, C - 1, C + 1, HAL - 2, C + 1, wie="windgang 5: de orgelpijp, recht omlaag door de middenpagode")
    b.prop("windfluit_5", C + 3, vloeren[-1] + 1, C, wie="de monding van de orgelpijp (de onderste verdieping)")
    b.wezen("lemur_3", C - 3, vloeren[1] + 1, C + 4, "guhs:vleermuis_lemurguh", yaw=90.0, honk=4)


def stal(b):
    """The stable of the luchtbizon-guhs inside the island: five pens that open to the void, an aisle behind them and a
    stair up to the grass."""
    x0, x1, z0, z1 = STAL
    y = STAL_Y
    b.fill(x0 - 1, y - 1, z0 - 1, x1 + 1, y, z1 + 6, STENEN)
    b.fill(x0 - 1, y + 1, z0 - 1, x1 + 1, y + 6, z1 + 6, ROTS)
    b.fill(x0, y + 1, z0, x1, y + 5, z1 + 5, gb.AIR)
    for k in range(5):
        px0 = x0 + k * 8
        if k:
            b.fill(px0 - 1, y + 1, z0, px0 - 1, y + 5, z1, STENEN)
        b.fill(px0, y, z0, px0 + 6, y, z1, "hay_block", {"axis": "y"})
        b.fill(px0 + 2, y, z0 + 2, px0 + 4, y, z0 + 4, STENEN)
        # the front: a fence with a gate that stands open
        for x in range(px0, px0 + 7):
            if x != px0 + 3:
                b.set(x, y + 1, z1 + 1, "spruce_fence")
        b.set(px0 + 3, y + 1, z1 + 1, "spruce_fence_gate", {"facing": "south", "open": "true", "in_wall": "false", "powered": "false"})
        b.set(px0 + 6, y + 1, z1, "guhs:guh_voerbak", {"facing": "south", "voer": str(k % 5)})
        # the flight opening: out through the rock to the north, as far as the rim
        for x in range(px0 + 1, px0 + 6):
            z = z0 - 1
            while z >= 0 and any((x, yy, z) in b.s.blocks for yy in range(y + 1, y + 5)):
                for yy in range(y + 1, y + 5):
                    b.set(x, yy, z, gb.AIR)
                if (x, y, z) not in b.s.blocks or b.s.blocks[(x, y, z)][0] == gb.AIR:
                    b.set(x, y, z, STENEN)
                z -= 1
        g.staaf(b, px0 + 3, y + 5, z0 + 3, "down")
        b.prop(f"bizon_{k + 1}", px0 + 3, y + 1, z0 + 3, wie=f"hok {k + 1} van de luchtbizon-guhs (leeg: de kudde is zoek)")
        naam = ["Appa", "Oogie", "Lola", "Pema", "Dikkie"][k]
        b.sign(px0 + 5, y + 2, z1 + 1, "south", [f"Hok {k + 1}", naam, "is even weg", "(al 100 jaar)" if k == 0 else ""], wall=False)
    for x in range(x0 + 3, x1, 8):
        g.staaf(b, x, y + 5, z1 + 3, "down")
    b.zone("bizonstal", x0, y + 1, z0, x1, y + 5, z1 + 5, wie="de stal van de luchtbizon-guhs")
    # the stair up to the grass, at the west end of the aisle
    for z in (z1 + 3, z1 + 4):
        b.fill(x0 - 8, y, z, x0 - 1, y, z, STENEN)
    b.fill(x0 - 1, y + 1, z1 + 3, x0 - 1, y + 3, z1 + 4, gb.AIR)
    g.trap(b, x0 - 2, y + 1, z1 + 4, "west", 8, TRAP, breed=2, vul=STENEN)
    for x in range(x0 - 10, x0 - 5):
        for z in (z1 + 2, z1 + 5):
            b.set(x, T + 1, z, WAL)
    b.set(x0 - 5, T + 1, z1 + 3, WAL)
    b.set(x0 - 5, T + 1, z1 + 4, WAL)
    b.sign(x0 - 11, T + 1, z1 + 5, "west", ["Bizonstal", "Trap af.", "Hooi meenemen", "mag, njeg"], wall=False)


def bovenop(b, top):
    """The top of the island: grass, a path from the landing place to the pavilion, the fountain, clouds."""
    rng = b.rng
    for z in range(C + 4, C + 42):
        for x in range(C - 1, C + 2):
            if (x, z) in top:
                b.set(x, T, z, STENEN)
    # the fountain: a closed basin with a light in the middle
    fx, fz = C, C + 16
    b.fill(fx - 3, T, fz - 3, fx + 3, T, fz + 3, STENEN)
    b.fill(fx - 2, T + 1, fz - 2, fx + 2, T + 1, fz + 2, STENEN)
    b.fill(fx - 1, T + 1, fz - 1, fx + 1, T + 1, fz + 1, "water", {"level": "0"})
    b.fill(fx, T + 1, fz, fx, T + 2, fz, MUUR)
    b.set(fx, T + 3, fz, "sea_lantern")
    b.prop("fontein", fx, T + 1, fz, wie="de fontein op het eiland")
    b.sign(fx + 3, T + 1, fz + 3, "south", ["Fontein", "Hoort onder te", "hangen. Liep", "steeds leeg"], wall=False)
    # a ring of mats to sit and think, west of the path
    for a in range(0, 360, 45):
        x, z = int(round(C - 18 + 4 * math.cos(math.radians(a)))), int(round(C + 20 + 4 * math.sin(math.radians(a))))
        b.set(x, T + 1, z, "orange_carpet" if a % 90 else "yellow_carpet")
    b.set(C - 18, T + 1, C + 20, STENEN)
    b.set(C - 18, T + 2, C + 20, "lantern", gb.lamp(False))
    for (x, z) in sorted(top):
        if (x, T + 1, z) in b.s.blocks or b.s.blocks[(x, T, z)][0] != gb.mc(GROND):
            continue
        r = rng.random()
        if r < 0.05:
            b.set(x, T + 1, z, "guhs:korstgras")
        elif r < 0.054 and math.hypot(x - C, z - C) > 12:
            b.fill(x, T + 1, z, x, T + 1 + rng.randrange(2), z, DICHT)
    # clouds around the rim and under the island (they hang loose: that is what clouds do)
    for i in range(14):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(R_EILAND + 3, R_EILAND + 8)
        cx, cy, cz = int(C + r * math.cos(a)), rng.randrange(T - 30, T + 6), int(C + r * math.sin(a))
        if abs(cx - C) < 12 and cz > C:
            continue                                 # (not in front of the landing place)
        for x in range(cx - 3, cx + 4):
            for z in range(cz - 2, cz + 3):
                for y in (cy, cy + 1):
                    if ((x - cx) / 3.4) ** 2 + ((z - cz) / 2.4) ** 2 + (y - cy) * 0.5 <= 1.0 and b.inside(x, y, z) and (x, y, z) not in b.s.blocks:
                        b.set(x, y, z, WOLK)
    b.zone("aankomst", C - 6, T + 1, C + 28, C + 6, T + 6, C + 40, wie="de landingsplek op het eiland")
    b.prop("aankomst_speler", C, T + 1, C + 33)
    b.prop("uitzicht", -30, HAL - 4, C + 30, wie="van hier zie je de tempel onder het eiland hangen")
    b.sign(C + 3, T + 1, C + 28, "south", ["Westelijke", "Luchttempel", "(u staat op", "het dak)"], wall=False, glow=True)


def kudde(b, top):
    """Where the strayed luchtbizon-guhs hang about: two ledges on the side of the island, two clouds and the far end
    of the grass. Nobody walks there (los)."""
    for nr, (dx, y) in enumerate(((-1, T - 3), (1, T - 4)), start=1):
        x = C + dx * (W // 2 - 1)
        while (x, y, C) not in b.s.blocks:           # (walk in from the edge of the template to the rim of the island)
            x -= dx
        for i in range(-2, 6):
            for z in range(C - 3, C + 4):
                if abs(z - C) + max(0, i - 2) < 5 and b.inside(x + dx * i, y, z):
                    b.set(x + dx * i, y, z, ROTS)
                    b.set(x + dx * i, y - 1, z, ROTS)
        b.prop(f"kudde_{nr}", x + dx * 3, y + 1, C, wie="een richel aan de zijkant van het eiland", los=True)
    wolken = [(C + 44, T - 22, C - 40), (C - 40, T - 34, C + 38)]
    for nr, (cx, cy, cz) in enumerate(wolken, start=3):
        cx, cz = max(5, min(W - 6, cx)), max(5, min(D - 6, cz))
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                if math.hypot(x - cx, z - cz) <= 4.3:
                    b.set(x, cy, z, DICHT)
                    if math.hypot(x - cx, z - cz) <= 3.0:
                        b.set(x, cy - 1, z, WOLK)
        b.prop(f"kudde_{nr}", cx, cy + 1, cz, wie="een wolk waar een luchtbizon-guh op ligt te dutten", los=True)
    b.prop("kudde_5", C - 38, T + 1, C - 8, wie="het verste puntje van het gras", los=True)


def bouw(h):
    b = kit.Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=g.PREFIX)
    top = g.eiland(b, vorm(), GROND, ROTS, kern=ROTS, laag=2)
    stal(b)
    hal(b)
    keldertrap(b)
    for nr, naam in enumerate(("north", "east", "south", "west"), start=1):
        gang(b, naam, nr)
    buitenpagodes(b)
    middenpagode(b)
    bovenop(b, top)
    kudde(b, top)
    return (b,)


def zwevend_ok(b):
    return lambda c: "kruimelwolk" in b.s.blocks[c][0]


def verlicht(b, start):
    return g.verlicht(b, start, "sea_lantern", {gb.mc(STENEN), gb.mc(ROTS), gb.mc(GROND), "minecraft:calcite", "minecraft:hay_block"},
                      min_licht=7, raster=7)


def check(b):
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    verwacht = ["luchtnon", ("windgang", 5), ("windfluit", 5), "bizonstal", ("bizon", 5), ("kudde", 5), "fontein", ("lemur", 3)]
    problems = g.eerlijk(b) + b.check(start, verwacht, min_licht=5, zwevend_ok=zwevend_ok(b))
    for k in range(1, 6):                            # (every pen: 7 x 5 x 7 free)
        x, y, z = b.plekken[f"bizon_{k}"]["pos"]
        vol = [c for c in ((x + i, y + j, z + q) for i in range(-3, 4) for j in range(5) for q in range(-3, 4))
               if c in b.s.blocks and b.s.blocks[c][0] not in (gb.AIR, "guhs:guh_voerbak", "guhs:lichtstaafje")]
        if vol:
            problems.append(f"hok {k} is niet 7 x 5 x 7 vrij: {vol[:3]}")
    return problems


def build(h):
    (b,) = bouw(h)
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    b.connect()
    g.trapvormen(b)
    lampen = verlicht(b, start)
    problems = check(b)
    if problems:
        raise SystemExit(f"{NAAM}: self-check found {len(problems)} problem(s):\n  " + "\n  ".join(problems[:40]))
    b.save_tiles()
    kit.leegte_structuur(h, b, titel="Westelijke Luchttempel", tooltip="Een tempel die ondersteboven onder zijn eiland hangt", y=Y)
    print(f"{NAAM}: self-check ok {b.stats}, {lampen} lampen gelegd")
    return b
