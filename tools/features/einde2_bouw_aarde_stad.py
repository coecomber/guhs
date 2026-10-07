"""
einde2, slice bouw_aarde: Ba Sing Guh above the ground (called by einde2_bouw_aarde.stad; the kit is
einde2_bouw_aarde_bouw.py, the two places under the ground are in einde2_bouw_aarde_onder.py).

From the outside in:
  plein           the landing lobe on the west side: the landing place, the team's camp, the road to the gate
  poortgebouw     the gate in the outer wall with the passport desk (a booth with a window on the passage), the waiting room,
                  the stair tower up to the wall and to station Buitenmuur
  monorail        the viaduct from the outer wall over the two inner walls to the upper ring, with its four stations
  aardrommel      the sunken arena of the lower ring and the blindfold course next to it
  meer            Meer Laogai with the boat shed that is the way down to the base
  theehuis        De Jasmijn Draak (tea room, kitchen, Oom Miroh's rooms upstairs)
  joo_dee, gasthuis, paleis (throne hall, Bosco's room, the garden with the Koningsput)
  wijken          the houses of the three rings, the stalls, the lamps, the trees

Every function also records its markers (build contract 6.7); NOTHING of the story is placed.
"""
import math
import random

import features.einde2_kit as kit
from features import guheinde_bouw as gb
from features.guheinde_bouw import lamp, slab, stair
from features.einde2_bouw_aarde_bouw import (
    BOVEN, CX, CZ, DIRS, GOUD, GOUDM, GOUDP, GOUDT, KLIP, KLIPM, KLIPP, KLIPS, KLIPT, KOOL, KRISTAL, MIDDEN, MUNT, MUUR,
    MUUR1_TOP, MUUR2_TOP, MUUR3_TOP, ONDER, OPP, R_BOVEN, R_MIDDEN, R_MUUR1, R_MUUR2, R_MUUR3, R_ONDER, SPOOR, SPOOR_Y, SPOOR_Z, TEGEL,
    TEGELP, TEGELT, THEEKIST, _hash, afstand, bed, boom, huis, kraam, lantaarn, muursteen, muurtoren, pagodedak, pol, poort, ringmuur,
    ruis, top, traptoren, zadeldak)

WATER = {"level": "0"}
BLAD = {"distance": "1", "persistent": "true", "waterlogged": "false"}
LAMPION = "guhs:lampion_mint"
LAMPION2 = "guhs:lampion_geel"
POORT_X0, POORT_X1, POORT_Z0, POORT_Z1 = 22, 32, 116, 140      # the gate building in the outer wall
ARENA = (90, 205)
BAAN = (110, 206, 128, 220)
MEER = (223, 128, 11.0, 17.0)                                   # middle, radius towards the middle, radius along the ring
SCHUUR = (219, 99)                                              # the boat shed: the tower down to the base
THEE = (73, 137, 87, 148)                                       # the walls of De Jasmijn Draak
PALEIS = (122, 114, 150, 142)                                   # the walls of the palace (its podium lies 3 around it)
PUT = (157, 124, 165, 130)                                      # the Koningsput: the shaft down to the kristalkamer
OUDE_POORT = (154, 102)                                         # the tower down to the catacombs
KRAMEN = [(44, 133), (84, 118), (98, 117)]                      # the three spots of the koolman's stall (5 x 4 x 4 free)
STATIONS = {"onder": 52, "midden": 80, "boven": 110}


# =====================================================================================================================
# roads
# =====================================================================================================================
def straatsteen(x, z, d):
    r = _hash(x, z, 21)
    if d >= R_MUUR2:
        return "minecraft:cobblestone" if r < 0.5 else "guhs:kruimelrots_stenen" if r < 0.86 else "minecraft:coarse_dirt"
    if d >= R_MUUR3:
        return KLIPS if r < 0.62 else "minecraft:stone_bricks"
    return GOUD if (x + z) % 4 == 0 else "minecraft:polished_andesite"


def wegen(b):
    """The avenue from the landing place to the palace (with a golden kerb), a ring road in every ring, three more roads
    from the palace gates to the lower ring and the alleys on the diagonals."""
    kol = b.kolom
    for x in range(5, 119):
        for z in range(125, 132):
            if (x, z) in kol:
                d = kol[(x, z)][2]
                b.plavei(x, z, GOUD if z in (125, 131) and x >= 33 else straatsteen(x, z, d))
    for (x, z), (t, bt, d) in kol.items():
        if 88 <= d < 91 or 47.5 <= d < 50.5 or 36.5 <= d < 39.5:
            b.plavei(x, z, straatsteen(x, z, d))
    for z in list(range(25, 111)) + list(range(146, 232)):
        for x in range(132, 137):
            if (x, z) in kol:
                b.plavei(x, z, straatsteen(x, z, kol[(x, z)][2]))
    for x in range(169, 212):
        for z in range(126, 131):
            b.plavei(x, z, straatsteen(x, z, kol[(x, z)][2]))
    for graden in (45, 135, 225, 315):
        c, s = math.cos(math.radians(graden)), math.sin(math.radians(graden))
        for (x, z), (t, bt, d) in kol.items():
            if (75 <= d < 104 or 45 <= d < 70) and (x - CX) * c + (z - CZ) * s > 0 and abs((x - CX) * s - (z - CZ) * c) <= (1.6 if d >= 74 else 1.1):
                b.plavei(x, z, straatsteen(x, z, d))


def muren(b):
    ringmuur(b, R_ONDER, R_MUUR1, MUUR1_TOP, buiten=True)
    ringmuur(b, R_MIDDEN, R_MUUR2, MUUR2_TOP)
    ringmuur(b, R_BOVEN, R_MUUR3, MUUR3_TOP)
    for graden in range(0, 360, 30):
        if abs(graden - 180) > 20:
            muurtoren(b, graden)
    for richting in ("west", "east", "north", "south"):
        poort(b, richting, R_MIDDEN, R_MUUR2, MUUR2_TOP, ONDER, MIDDEN)
        poort(b, richting, R_BOVEN, R_MUUR3, MUUR3_TOP, MIDDEN, BOVEN)


# =====================================================================================================================
# the landing lobe and the gate
# =====================================================================================================================
def plein(b):
    G = ONDER
    for x in range(3, 22):
        for z in range(118, 139):
            if (x, z) in b.kolom and (4 <= x <= 18 and 120 <= z <= 136):
                rand = x in (4, 18) or z in (120, 136)
                b.plavei(x, z, GOUD if rand else "guhs:kruimelrots_stenen" if (x + z) % 2 else KLIPS)
    for x, z in ((3, 119), (3, 137), (19, 119), (19, 137)):
        lantaarn(b, x, z)
    b.sign(20, G + 1, 123, "west", ["Welkom in", "Ba Sing Guh", "Er is hier geen", "knabbeltekort."], wall=False)
    b.zone("aankomst", 5, G + 1, 121, 17, G + 14, 135, wie="de landingsplek op de westlob: vlak, vrij naar boven")
    b.prop("aankomst_speler", 11, G + 1, 128, wie="hier sta je na aankomst, met je neus naar de poort")
    b.prop("uitzicht", -30, 66, 128, wie="buiten de bouw, ten westen van de poort: het openingsshot op de buitenmuur")
    b.prop("rustpunt_1", 19, G + 1, 128, wie="voor de buitenpoort")
    # the camp of the team: a fire, logs to sit on, a little tent, room for Appa
    b.neem(5, 139, 21, 154)
    b.set(13, G + 1, 146, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for x, z, as_ in ((11, 145, "z"), (11, 147, "z"), (15, 145, "z"), (15, 147, "z"), (13, 143, "x"), (13, 149, "x")):
        b.set(x, G + 1, z, "minecraft:stripped_spruce_log", {"axis": as_})
    for z in range(141, 145):                                   # (a tent: two slopes of green cloth, closed at the back)
        for x, hoog in ((6, 1), (7, 2), (8, 3), (9, 2), (10, 1)):
            b.set(x, G + hoog, z, "minecraft:green_wool")
            if z == 141:
                for y in range(G + 1, G + hoog):
                    b.set(x, y, z, "minecraft:lime_wool")
    b.set(8, G + 1, 143, "minecraft:lantern", lamp(False))
    lantaarn(b, 6, 152, paal="minecraft:spruce_fence", hoog=2)
    lantaarn(b, 20, 141, paal="minecraft:spruce_fence", hoog=2)
    b.sign(10, G + 1, 139, "north", ["Kamp van Team", "Guhvatar", "Appa parkeert", "achteraan, njeg"], wall=False)
    b.zone("kamp", 5, G + 1, 139, 21, G + 8, 154, wie="het kamp van het team, naast de landingsplek")
    b.prop("appa_kamp", 17, G + 1, 151, wie="Appa-guh ligt hier: 5 x 5 vrij")
    for naam, x, z, yaw in (("aang", 12, 144, -20), ("momo", 14, 144, 20), ("katara", 10, 146, -90), ("sokka", 16, 146, 90),
                            ("toph", 12, 148, -160), ("zuka", 14, 148, 160)):
        b.plek(f"team_{naam}", x, G + 1, z, yaw=yaw, wie=f"{naam} bij het kampvuur")


def poortgebouw(b):
    """The gate building: a block of wall stone in the outer wall as high as its walkway. The passage is seven wide; on
    its north side the booth of the loketguh (a window with bars on the passage), on its south side the waiting room.
    Against its inside stands the stair tower to the wall; station Buitenmuur lies on the wall north of the gate."""
    G, T = ONDER, MUUR1_TOP
    x0, x1, z0, z1 = POORT_X0, POORT_X1, POORT_Z0, POORT_Z1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(G + 1, T + 3):
                b.weg(x, y, z)
            rand = x in (x0, x1) or z in (z0, z1)
            for y in range(G + 1, T + 1):
                if rand or y >= T - 1:
                    b.set(x, y, z, muursteen(y - G, T - y))
            b.bezet.add((x, z))
    cellen = b.hak(x0, G + 1, 125, x1, G + 6, 131) + b.hak(x0, G + 7, 126, x1, G + 7, 130) + b.hak(x0, G + 8, 127, x1, G + 8, 129)
    cellen += b.hak(24, G + 1, 118, 30, G + 4, 123) + b.hak(24, G + 1, 133, 30, G + 4, 138)
    for x in (x0 - 1, x1 + 1):
        b.laat_open(x, G + 1, 125, x, G + 8, 131)
    b.voering(cellen, lambda c, kant: KLIPS if kant == "vloer" else MUUR)
    # the parapets of its roof (the wall's walkway runs over it), open where the wall and the tower join
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x == x0:
                b.set(x, T + 1, z, KLIPS)
                if (z // 2) % 2 == 0:
                    b.set(x, T + 2, z, KLIPS)
            elif (x == x1 and not 118 <= z <= 120) or (z in (z0, z1) and not 25 <= x <= 29):
                b.set(x, T + 1, z, KLIPS)
    # the open gate leaves against the outside, the aardemunt on both faces
    for z in list(range(121, 125)) + list(range(132, 136)):
        for y in range(G + 1, G + 8):
            b.set(x0 - 1, y, z, "minecraft:dark_oak_planks" if (y - G) % 3 else "minecraft:stripped_dark_oak_log", None if (y - G) % 3 else {"axis": "z"})
    for x in (x0, x1):
        for z in (127, 128, 129):
            for y in (G + 11, G + 12, G + 13):
                b.set(x, y, z, MUNT)
    for x in (24, 30):
        b.set(x, G + 8, 128, "minecraft:lantern", lamp(True))
    # the booth of the loketguh
    gb.door(b, 24, G + 1, 124, "north", "spruce")
    b.set(27, G + 1, 124, "minecraft:polished_andesite")
    b.weg(27, G + 2, 124)
    b.set(26, G + 2, 124, "minecraft:iron_bars")
    b.set(28, G + 2, 124, "minecraft:iron_bars")
    for x in range(25, 30):
        b.set(x, G + 1, 118, "minecraft:bookshelf")
        b.set(x, G + 2, 118, "minecraft:bookshelf")
    b.set(30, G + 1, 118, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(29, G + 1, 123, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    b.set(27, G + 4, 121, "minecraft:lantern", lamp(True))
    b.sign(27, G + 3, 125, "south", ["PASPOORTLOKET", "Geen stempel,", "geen stad.", "Njeg."], glow=True)
    b.sign(29, G + 2, 125, "south", ["Loket 1: dicht", "Loket 2: dicht", "Loket 3: streng", ""])
    b.sign(26, G + 2, 119, "south", ["Paspoorten", "A t/m Guh", "", ""])
    b.plek("loketguh", 27, G + 1, 122, yaw=0.0, wie="de strenge loketguh, achter het raampje", kind="LOKETGUH")
    b.prop("loket", 27, G + 1, 124, wie="de balie van het paspoortloket (het raampje erboven is open)")
    b.prop("buitenpoort", x0, G + 1, 128, wie="de drempel van de buitenpoort (7 breed, 6 tot 8 hoog): fase B zet hem dicht of open")
    # the waiting room
    gb.door(b, 24, G + 1, 132, "south", "spruce")
    for x in range(25, 30):
        b.set(x, G + 1, 138, "minecraft:spruce_stairs", stair("south"))
    b.set(30, G + 1, 134, "minecraft:spruce_stairs", stair("east"))
    b.set(30, G + 1, 136, "minecraft:spruce_stairs", stair("east"))
    b.set(27, G + 4, 135, "minecraft:lantern", lamp(True))
    b.sign(27, G + 2, 133, "south", ["Wachtkamer", "Trek een nummer.", "De nummers", "zijn op, njeg."])
    b.sign(30, G + 3, 135, "west", ["Wachttijd:", "drie tot vijf", "werkdagen", "(of een stempel)"])
    # a pavilion over the gate
    for x, z in ((24, 122), (30, 122), (24, 134), (30, 134)):
        for y in range(T + 1, T + 6):
            b.set(x, y, z, KLIPM)
    pagodedak(b, 24, 122, 30, 134, T + 5, lagen=3)
    for z in (125, 131):
        b.set(27, T + 6, z, LAMPION, lamp(True))
    # the stair tower: its lower door on the avenue, its upper door on the roof of the gate
    traptoren(b, 33, 116, G + 1, T + 1, ("south", 0), ("west", 0), MUUR, KLIPT,
              dak=lambda b_, ax, az, bx, bz, y: pagodedak(b_, ax, az, bx, bz, y, lagen=3))
    b.neem(32, 115, 40, 124)
    b.sign(37, G + 2, 123, "south", ["Naar de muur", "en de monorail", "(20 treden,", "niet zeuren)"])
    # station Buitenmuur: the wall's walkway north of the gate, widened to the track
    for x in range(29, 37):
        for z in range(106, 116):
            for y in (T + 1, T + 2):
                if b.naam(x, y, z) in (KLIPS, "minecraft:lantern"):
                    b.weg(x, y, z)
            if b.naam(x, T, z) is None:
                b.set(x, T, z, "minecraft:polished_andesite")
    for z in range(106, 110):
        b.set(36, T + 1, z, KLIPM)
    for x in range(31, 37):
        b.set(x, T + 1, 106, KLIPM)
    for x in range(29, 37):
        b.set(x, T + 1, 115, KLIPM)
    for x, z in ((36, 106), (36, 115), (29, 106), (29, 115)):
        for y in range(G + 1 if x == 36 else T + 1, T + 7):
            if b.naam(x, y, z) in (None, KLIPM):
                b.set(x, y, z, MUUR if y <= T else "minecraft:dark_oak_fence")
    pagodedak(b, 29, 106, 36, 115, T + 6, lagen=2)
    b.set(32, T + 6, 108, LAMPION, lamp(True))
    b.sign(35, T + 1, 107, "west", ["Station", "Buitenmuur", "Volgende halte:", "Onderring"], wall=False)
    b.sign(30, T + 1, 107, "east", ["Monorail", "Zelf duwen.", "De duwer is", "met pauze."], wall=False)
    b.prop("station_buiten", 32, T + 1, 109, wie="het perron op de buitenmuur")
    b.prop("monorail_wagen", 34, T + 1, SPOOR_Z, wie="hier wacht de wagen (op de balk)")


# =====================================================================================================================
# the monorail
# =====================================================================================================================
PIJLERS = (38, 48, 58, 69, 79, 89, 100, 110)


def monorail(b):
    """The viaduct along z = SPOOR_Z from the outer wall to the upper ring: a deck of five with the beam in the middle, a
    low wall on both sides, piers every ten blocks, and under it a strip of garden. Three stations with a stair tower."""
    Y = SPOOR_Y
    for x in range(31, 115):
        for z in range(SPOOR_Z - 2, SPOOR_Z + 3):
            rand = abs(z - SPOOR_Z) == 2
            b.set(x, Y, z, KLIPS if rand else "minecraft:polished_andesite")
            if rand:
                b.set(x, Y + 1, z, KLIPM)
                if x % 8 == 3:
                    b.set(x, Y + 2, z, "minecraft:lantern", lamp(False))
        b.set(x, Y + 1, SPOOR_Z, SPOOR)
        if x % 10 == 4:
            b.set(x, Y - 1, SPOOR_Z, "minecraft:lantern", lamp(True))
    for z in range(SPOOR_Z - 2, SPOOR_Z + 3):
        b.set(115, Y, z, KLIPS)
        b.set(115, Y + 1, z, KLIPS)
    b.set(114, Y + 1, SPOOR_Z, MUUR)
    for px in PIJLERS:
        for x in range(px - 1, px + 2):
            for z in range(SPOOR_Z - 1, SPOOR_Z + 2):
                t = top(x, z)
                for y in range(t + 1, Y):
                    b.set(x, y, z, muursteen(y - t, Y - y))
        for z in range(SPOOR_Z - 1, SPOOR_Z + 2):
            b.set(px - 2, Y - 1, z, KLIPT, stair("east", "top"))
            b.set(px + 2, Y - 1, z, KLIPT, stair("west", "top"))
    # the garden strip under it
    for x in range(33, 114):
        for z in range(SPOOR_Z - 3, SPOOR_Z + 4):
            k = b.kolom.get((x, z))
            if k is None or (x, z) in b.bezet and b.naam(x, k[0] + 1, z) is not None:
                continue
            if (x, z) not in b.bezet:
                b.set(x, k[0], z, "minecraft:moss_block")
                r = _hash(x, z, 31)
                if abs(z - SPOOR_Z) <= 2 and b.naam(x, k[0] + 1, z) is None and r < 0.16:
                    b.set(x, k[0] + 1, z, ("minecraft:white_tulip", "minecraft:fern", "minecraft:short_grass", "minecraft:oxeye_daisy")[int(r * 25)])
            b.bezet.add((x, z))
    b.pad("monorail_route", [(32, Y + 2, SPOOR_Z), (STATIONS["onder"], Y + 2, SPOOR_Z), (STATIONS["midden"], Y + 2, SPOOR_Z),
                             (STATIONS["boven"], Y + 2, SPOOR_Z), (113, Y + 2, SPOOR_Z)],
          wie="de balk van de monorail, van de buitenmuur naar de bovenring (de wagen rijdt erboven)")
    station(b, "onder", "Onderring", "Middenring", ONDER, ("east", 0), ("south", 0))
    station(b, "midden", "Middenring", "Bovenring", MIDDEN, ("east", 1), ("south", -1))
    # the terminus: its tower stands south of the track (north of it the wall of the upper ring is in the way)
    x, G, Y1 = STATIONS["boven"], BOVEN, Y + 1
    tx, tz = x - 3, SPOOR_Z + 4
    traptoren(b, tx, tz, G + 1, Y1, ("east", -1), ("north", 1), MUUR, GOUDT,
              dak=lambda b_, ax, az, bx, bz, y: pagodedak(b_, ax, az, bx, bz, y, trap=GOUDT, plaat=GOUDP, blok=TEGEL, lagen=3))
    b.neem(tx - 1, tz - 1, tx + 7, tz + 7)
    for xx in range(x - 4, x + 5):
        b.set(xx, Y, SPOOR_Z + 3, "minecraft:polished_andesite")
        if b.naam(xx, Y + 1, SPOOR_Z + 2) == KLIPM and x - 2 <= xx <= x + 3:
            b.weg(xx, Y + 1, SPOOR_Z + 2)
            b.weg(xx, Y + 2, SPOOR_Z + 2)
    for xx in (x - 4, x + 4):
        b.set(xx, Y + 1, SPOOR_Z + 3, KLIPM)
    for xx, zz in ((x - 5, SPOOR_Z - 2), (x + 4, SPOOR_Z - 2), (x - 5, SPOOR_Z + 2), (x + 4, SPOOR_Z + 2)):
        for y in range(Y + 1, Y + 7):
            b.set(xx, y, zz, "minecraft:dark_oak_fence")
    pagodedak(b, x - 5, SPOOR_Z - 2, x + 4, SPOOR_Z + 2, Y + 6, trap=GOUDT, plaat=GOUDP, blok=TEGEL, lagen=2)
    b.set(x, Y + 6, SPOOR_Z + 1, LAMPION2, lamp(True))
    b.sign(x - 3, Y + 1, SPOOR_Z + 1, "east", ["Station", "Bovenring", "Eindpunt.", "Netjes lopen."], wall=False)
    b.prop("station_boven", x + 1, Y + 1, SPOOR_Z + 3, wie="het perron van de bovenring (de trap omlaag komt uit bij het paleisplein)")


def station(b, naam, titel, volgende, G, onder, boven):
    """A station north of the track: a platform of 11 x 5 under a roof, the stair tower behind it."""
    x, Y = STATIONS[naam], SPOOR_Y
    tx, tz = x - 3, SPOOR_Z - 14
    traptoren(b, tx, tz, G + 1, Y + 1, onder, boven, MUUR, KLIPT,
              dak=lambda b_, ax, az, bx, bz, y: pagodedak(b_, ax, az, bx, bz, y, lagen=3))
    b.neem(tx - 1, tz - 1, tx + 7, SPOOR_Z - 3)
    for xx in range(x - 5, x + 6):
        for zz in range(SPOOR_Z - 7, SPOOR_Z - 2):
            rand = xx in (x - 5, x + 5)
            b.set(xx, Y, zz, KLIPS if rand or zz == SPOOR_Z - 7 else "minecraft:polished_andesite")
            if (rand or zz == SPOOR_Z - 7) and b.naam(xx, Y + 1, zz) is None and not (zz == SPOOR_Z - 7 and tx <= xx <= tx + 6):
                b.set(xx, Y + 1, zz, KLIPM)
        if abs(xx - x) <= 4:
            b.weg(xx, Y + 1, SPOOR_Z - 2)
            b.weg(xx, Y + 2, SPOOR_Z - 2)
    for xx, zz in ((x - 5, SPOOR_Z - 7), (x + 5, SPOOR_Z - 7), (x - 5, SPOOR_Z + 2), (x + 5, SPOOR_Z + 2)):
        for y in range(Y + 1, Y + 7):
            b.set(xx, y, zz, "minecraft:dark_oak_fence")
        if zz < SPOOR_Z:
            t = top(xx, zz)
            for y in range(t + 1, Y):
                b.set(xx, y, zz, muursteen(y - t, Y - y))
            b.bezet.add((xx, zz))
    pagodedak(b, x - 5, SPOOR_Z - 7, x + 5, SPOOR_Z + 2, Y + 6, lagen=2)
    for xx in (x - 3, x + 3):
        b.set(xx, Y + 6, SPOOR_Z - 4, LAMPION, lamp(True))
    b.sign(x - 4, Y + 1, SPOOR_Z - 4, "east", ["Station", titel, "Volgende halte:", volgende], wall=False)
    b.prop(f"station_{naam}", x, Y + 1, SPOOR_Z - 4, wie=f"het perron van station {titel}")


# =====================================================================================================================
# the Aardrommel
# =====================================================================================================================
def aardrommel(b):
    """The arena: a bowl sunk five deep into the lower ring, tiers all around, the ring (a raised stone floor of 9 x 9
    with four lamp posts) in the middle, the stand of the omroeper with his bell on the north rim. East of it the walled
    blindfold course: posts and low walls to feel your way around, six aardemunt stones in the floor (the tril-doelen)."""
    G = ONDER
    ax, az = ARENA
    lampjes = set()
    for x in range(ax - 18, ax + 19):
        for z in range(az - 18, az + 19):
            r = math.hypot(x - ax, z - az)
            if r > 17.5 or (x, z) not in b.kolom:
                continue
            b.bezet.add((x, z))
            if r <= 13.5:
                trede = G - 5 + max(0, math.ceil(r - 8.5))
                for y in range(trede + 1, G + 1):
                    b.weg(x, y, z)
                for y in range(G - 8, trede):
                    b.set(x, y, z, KLIP)
                b.set(x, trede, z, "minecraft:packed_mud" if r <= 8.5 and _hash(x, z, 41) < 0.6 else KLIPS if r <= 8.5 else
                      ("minecraft:stone_bricks" if (trede + G) % 2 else KLIPS))
            elif r <= 14.5:
                gat = min(abs(x - ax), abs(z - az)) <= 1
                b.set(x, G, z, KLIPS)
                if not gat:
                    b.set(x, G + 1, z, MUUR)
                    hoek = int(math.degrees(math.atan2(z - az, x - ax)) % 360)
                    if hoek % 30 == 15 and (hoek // 30) not in lampjes:
                        lampjes.add(hoek // 30)
                        b.set(x, G + 2, z, "minecraft:lantern", lamp(False))
            else:
                b.set(x, G, z, "guhs:kruimelrots_stenen" if _hash(x, z, 42) < 0.7 else "minecraft:cobblestone")
    P = G - 4                                                   # the ring: its floor block
    for x in range(ax - 4, ax + 5):
        for z in range(az - 4, az + 5):
            rand = abs(x - ax) == 4 or abs(z - az) == 4
            b.set(x, P, z, MUUR if rand else "minecraft:smooth_stone")
    b.set(ax, P, az, MUNT)
    for dx, dz in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        b.set(ax + dx, P + 1, az + dz, KLIPM)
        b.set(ax + dx, P + 2, az + dz, KLIPM)
        b.set(ax + dx, P + 3, az + dz, "minecraft:lantern", lamp(False))
    for dx, dz in ((-7, 0), (7, 0), (0, 7), (0, -7)):
        b.set(ax + dx, P - 1, az + dz, "minecraft:ochre_froglight", {"axis": "y"})
    # the stand of the omroeper on the north rim, with the bell
    for x in range(ax - 1, ax + 2):
        for z in range(az - 17, az - 14):
            b.set(x, G + 1, z, MUUR)
    b.set(ax - 2, G + 1, az - 16, KLIPT, stair("east"))
    b.set(ax + 1, G + 2, az - 17, KLIPM)
    b.set(ax + 1, G + 3, az - 17, "minecraft:bell", {"attachment": "floor", "facing": "south", "powered": "false"})
    b.set(ax - 1, G + 2, az - 17, KLIPM)
    b.set(ax - 1, G + 3, az - 17, "minecraft:lantern", lamp(False))
    b.sign(ax + 3, G + 1, az - 16, "south", ["AARDROMMEL", "De Blinde", "Bandiet tegen", "De Kei-guh"], wall=False, glow=True)
    b.sign(ax - 4, G + 1, az - 16, "south", ["Kampioen:", "Blinde Bandiet", "Al 42 keer.", "Ze kijkt niet om"], wall=False)
    b.sign(ax - 16, G + 1, az - 3, "east", ["Eerste rij:", "je krijgt gruis", "in je knabbel.", "Hoort erbij."], wall=False)
    b.zone("aardrommel", ax - 15, G - 5, az - 17, ax + 15, G + 6, az + 15, wie="de arena van de Aardrommel")
    b.plek("toph", ax, P + 1, az - 3, yaw=0.0, wie="Toph-guh, De Blinde Bandiet", kind="TOPH_GUH")
    b.plek("kei", ax, P + 1, az + 3, yaw=180.0, wie="De Kei-guh", kind="KEI_GUH")
    b.plek("omroeper", ax, G + 2, az - 16, yaw=0.0, wie="de omroeper, op zijn verhoging bij de bel", kind="AARDROMMEL_OMROEPER")
    b.prop("arena_speler", ax - 3, P + 1, az, wie="hier staat de speler in de ring")
    b.prop("arena_tegenstander", ax + 3, P + 1, az, wie="hier staat de tegenstander")
    b.prop("rustpunt_2", ax - 12, G + 1, az - 12, wie="bij de ingang van de Aardrommel")
    # the blindfold course
    x0, z0, x1, z1 = BAAN
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            b.bezet.add((x, z))
            rand = x in (x0, x1) or z in (z0, z1)
            if x0 <= x <= x1 and z0 <= z <= z1:
                b.set(x, G, z, KLIPS if rand else "minecraft:smooth_sandstone" if (x * 3 + z) % 5 else "minecraft:sandstone")
                if rand and not (x == x0 and 211 <= z <= 213) and not (z == z0 and 118 <= x <= 120):
                    b.set(x, G + 1, z, MUUR)
                    b.set(x, G + 2, z, KLIPM if (x + z) % 2 else MUUR)
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1), (119, z1), (x1, 213)):
        b.set(x, G + 2, z, MUUR)
        b.set(x, G + 3, z, "minecraft:lantern", lamp(False))
    for x, z, hoog in ((114, 209, 2), (117, 215, 3), (121, 211, 2), (124, 216, 3), (125, 209, 2), (113, 217, 2), (120, 218, 1)):
        for y in range(G + 1, G + 1 + hoog):
            b.set(x, y, z, KLIPS)
    for x in range(115, 119):
        b.set(x, G + 1, 212, KLIPP, slab())
    for z in range(213, 217):
        b.set(122, G + 1, z, KLIPP, slab())
    doelen = ((112, 208), (116, 218), (119, 213), (123, 208), (126, 218), (126, 212))
    for i, (x, z) in enumerate(doelen, 1):
        b.set(x, G, z, MUNT)
        b.prop(f"tril_doel_{i}", x, G, z, wie="een aardemunt in de vloer: hier trilt het")
    b.sign(x0 - 1, G + 2, 210, "west", ["Blinddoekbaan", "Kijk met je", "voetjes, njeg.", ""])
    b.sign(121, G + 2, z0 - 1, "north", ["Oefenen?", "Ogen dicht.", "Stoten doet", "geen pijn. Soms."])
    b.zone("blinddoek_baan", x0 + 1, G + 1, z0 + 1, x1 - 1, G + 5, z1 - 1, wie="de ommuurde blinddoekbaan naast de arena")


# =====================================================================================================================
# Meer Laogai
# =====================================================================================================================
def in_meer(x, z, ruim=0.0):
    mx, mz, ra, rb = MEER
    return ((x - mx) / (ra + ruim)) ** 2 + ((z - mz) / (rb + ruim)) ** 2 <= 1.0 + 0.12 * ruis(x, z, 5, 51) * (0 if ruim else 1)


def meer(b):
    """The lake: four deep and full to the brim, in a bowl of klipkaas that closes it off from the hollow inside of the
    island (its water never runs anywhere), sea lanterns in its bed, reeds and lily pads, a jetty from the boat shed on its
    north shore."""
    G = ONDER
    mx, mz, ra, rb = MEER
    for x in range(int(mx - ra) - 5, int(mx + ra) + 6):
        for z in range(int(mz - rb) - 5, int(mz + rb) + 6):
            if (x, z) not in b.kolom or not in_meer(x, z, 3.0):
                continue
            b.bezet.add((x, z))
            nat = in_meer(x, z)
            for y in range(G - 8, G):
                b.set(x, y, z, KLIP)
            if nat:
                b.set(x, G - 4, z, "minecraft:sea_lantern" if (x % 5 == 3 and z % 5 == 3) else "minecraft:coarse_dirt" if _hash(x, z, 52) < 0.5 else KLIP)
                for y in range(G - 3, G + 1):
                    b.set(x, y, z, "minecraft:water", WATER)
            else:
                b.set(x, G, z, "minecraft:moss_block" if _hash(x, z, 53) < 0.7 else "guhs:kaaskorst")
    for x in range(int(mx - ra) - 2, int(mx + ra) + 3):
        for z in range(int(mz - rb) - 2, int(mz + rb) + 3):
            if b.naam(x, G - 1, z) != "minecraft:water":
                continue
            r = _hash(x, z, 54)
            for dx, dz in DIRS.values():                           # reeds on the shore, right at the water
                if b.naam(x + dx, G, z + dz) == "minecraft:moss_block" and _hash(x + dx, z + dz, 55) < 0.2:
                    for y in range(G + 1, G + 2 + int(_hash(x + dx, z + dz, 56) * 3)):
                        b.set(x + dx, y, z + dz, "minecraft:sugar_cane", {"age": "0"})
            if r < 0.05:
                b.set(x, G + 1, z, "minecraft:lily_pad")
    # the boat shed ("Botenverhuur") stands over the tower down; a jetty runs from its door into the lake
    sx, sz = SCHUUR
    for z in range(sz + 7, sz + 16):
        for x in (sx + 1, sx + 2, sx + 3):
            if b.naam(x, G - 1, z) == "minecraft:water" or z <= sz + 12:
                b.weg(x, G + 1, z)
                b.weg(x, G + 2, z)
                b.set(x, G, z, "minecraft:spruce_planks")
    b.set(sx + 1, G + 1, sz + 15, "minecraft:spruce_fence")
    b.set(sx + 1, G + 2, sz + 15, "minecraft:lantern", lamp(False))
    b.set(sx + 3, G + 1, sz + 15, "minecraft:barrel", {"facing": "up", "open": "false"})
    for x, z in ((209, 120), (210, 137), (236, 119), (236, 138), (223, 148)):
        lantaarn(b, x, z, paal="minecraft:spruce_fence", hoog=2)
    for x, z, f in ((208, 126, "east"), (208, 130, "east"), (238, 128, "west")):
        if b.naam(x, G + 1, z) is None:
            b.set(x, G + 1, z, "minecraft:spruce_stairs", stair(OPP[f]))
    b.sign(209, G + 1, 128, "west", ["Meer Laogai", "Hier is niets", "te zien. Zeker", "geen basis."], wall=False)
    b.zone("meer_laogai", int(mx - ra) - 2, G - 4, int(mz - rb) - 2, int(mx + ra) + 2, G + 6, int(mz + rb) + 2, wie="het meer in de onderring (4 diep)")


def botenschuur(b):
    """The top of the tower down to the base: a shed of spruce with a roof of green tiles and a sign that lies."""
    G = ONDER
    sx, sz = SCHUUR
    onder, boven = traptoren(b, sx, sz, 28, G + 1, ("south", -1), ("south", 1), "minecraft:deepslate_bricks", "minecraft:deepslate_brick_stairs",
                             kern="minecraft:polished_deepslate", ramen=False,
                             dak=lambda b_, ax, az, bx, bz, y: zadeldak(b_, ax, az, bx, bz, y, TEGELT, TEGELP, "minecraft:spruce_planks"))
    for x in range(sx, sx + 7):
        for z in range(sz, sz + 7):
            if x in (sx, sx + 6) or z in (sz, sz + 6):
                for y in range(G + 1, G + 5):
                    if b.naam(x, y, z) == "minecraft:deepslate_bricks":
                        hoek = x in (sx, sx + 6) and z in (sz, sz + 6)
                        b.set(x, y, z, "minecraft:stripped_spruce_log" if hoek else "minecraft:spruce_planks", {"axis": "y"} if hoek else None)
    b.neem(sx - 1, sz - 1, sx + 7, sz + 9)
    b.sign(sx + 3, G + 3, sz + 7, "south", ["Botenverhuur", "GESLOTEN", "Er is hier", "geen basis."], glow=True)
    b.sign(sx + 5, G + 2, sz + 7, "south", ["Boot huren?", "Fluit drie keer", "en wacht op", "de Dai Li. Eh."])
    b.prop("basis_ingang", sx + 2, G + 1, sz + 6, wie="de deur van de botenschuur: daarachter de trap omlaag naar de basis")
    return onder


# =====================================================================================================================
# De Jasmijn Draak
# =====================================================================================================================
def theehuis(b):
    """Oom Miroh's tea house on the avenue of the middle ring: a terrace with jasmine in front, the tea room (three tea
    tables, a counter with working teapots, the Pai Sho mat), the kitchen with the big kettle, a stair to Miroh's rooms
    upstairs, a skirt roof over the ground floor and a green hip roof on top. A dragon looks down over the door."""
    G = MIDDEN
    x0, z0, x1, z1 = THEE
    b.neem(x0 - 2, z0 - 5, x1 + 2, z1 + 2)
    hout, wand = "minecraft:dark_oak_planks", "minecraft:white_terracotta"
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            hoek = x in (x0, x1) and z in (z0, z1)
            b.set(x, G, z, "minecraft:stone_bricks" if rand else "minecraft:bamboo_planks")
            for y in range(G + 1, G + 10):
                if y in (G + 5, G + 9):
                    b.set(x, y, z, hout)
                elif hoek:
                    b.set(x, y, z, "minecraft:dark_oak_log", {"axis": "y"})
                elif rand:
                    b.set(x, y, z, "minecraft:stone_bricks" if y == G + 1 else wand)
    # windows, the door, the dragon
    for x in range(x0 + 2, x1 - 1, 2):
        for z in (z0, z1):
            if abs(x - 80) > 1 or z == z1:
                for y in (G + 2, G + 3):
                    b.set(x, y, z, "minecraft:lime_stained_glass_pane")
            b.set(x, G + 7, z, "minecraft:lime_stained_glass_pane")
    for z in range(z0 + 2, z1 - 1, 2):
        for x in (x0, x1):
            if not (x == x0 and z <= z0 + 6):
                b.set(x, G + 2, z, "minecraft:lime_stained_glass_pane")
            b.set(x, G + 7, z, "minecraft:lime_stained_glass_pane")
    gb.door(b, 80, G + 1, z0, "south", "dark_oak")
    b.set(80, G + 4, z0 - 1, "minecraft:dragon_wall_head", {"facing": "north", "powered": "false"})
    b.sign(80, G + 3, z0 - 1, "north", ["De Jasmijn Draak", "De beste thee", "van Ba Sing Guh", "(zegt Oom Miroh)"], glow=True)
    # the skirt roof over the ground floor and the roof on top
    pagodedak(b, x0, z0, x1, z1, G + 5, lagen=1, dicht=False)
    top_y = pagodedak(b, x0, z0, x1, z1, G + 9, uit=2)
    b.set(80, top_y + 1, (z0 + z1) // 2, GOUDM)
    for x, z in ((x0 - 1, z0 - 1), (x1 + 1, z0 - 1), (x0 - 1, z1 + 1), (x1 + 1, z1 + 1)):
        b.set(x, G + 4, z, LAMPION, lamp(True))
    for x, z in ((x0 - 2, z0 - 2), (x1 + 2, z0 - 2), (x0 - 2, z1 + 2), (x1 + 2, z1 + 2)):
        b.set(x, G + 8, z, LAMPION2, lamp(True))
    # the terrace: jasmine in planters, a low fence, two lampions
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 4, z0):
            b.plavei(x, z, "minecraft:bamboo_mosaic" if (x + z) % 2 else "minecraft:bamboo_planks")
    for x in list(range(x0 - 1, 78)) + list(range(83, x1 + 2)):
        b.set(x, G + 1, z0 - 4, "minecraft:dark_oak_fence")
    for x in (x0, x0 + 3, x1 - 3, x1):
        b.set(x, G + 1, z0 - 1, "minecraft:flowering_azalea_leaves", BLAD)
    for x in (78, 82):
        b.set(x, G + 1, z0 - 4, "minecraft:dark_oak_fence")
        b.set(x, G + 2, z0 - 4, "minecraft:dark_oak_fence")
        b.set(x, G + 3, z0 - 4, LAMPION, lamp(False))
    for x in (75, 85):
        b.set(x, G + 1, z0 - 3, "guhs:guh_tafel", {"facing": "north"})
        b.set(x - 1, G + 1, z0 - 3, "guhs:guh_stoel", {"facing": "east"})
        b.set(x + 1, G + 1, z0 - 3, "guhs:guh_stoel", {"facing": "west"})
    # inside, ground floor: tea room z0+1 .. z0+6, the counter at z0+7, the kitchen behind it
    kz = z0 + 7
    for x in range(x0 + 2, x1 - 3):
        b.set(x, G + 1, kz, hout)
    for x in (x0 + 3, x0 + 6, x0 + 9):
        b.set(x, G + 2, kz, "guhs:theepotje", {"facing": "north"})
    b.set(x1 - 3, G + 1, kz, "minecraft:dark_oak_fence_gate", {"facing": "north", "in_wall": "false", "open": "true", "powered": "false"})
    for x in (x1 - 2, x1 - 1):
        for y in range(G + 1, G + 5):
            b.set(x, y, kz, wand if y > G + 1 else "minecraft:stone_bricks")
    b.set(x0 + 1, G + 1, kz, hout)
    for tx, tz in ((x0 + 5, z0 + 2), (x1 - 3, z0 + 2), (x0 + 5, z0 + 5)):
        b.set(tx, G + 1, tz, "guhs:theetafel", {"gedekt": "true"})
        b.set(tx - 1, G + 1, tz, "guhs:guh_stoel", {"facing": "east"})
        b.set(tx + 1, G + 1, tz, "guhs:guh_stoel", {"facing": "west"})
    for x in range(x1 - 4, x1 - 1):
        for z in range(z0 + 4, z0 + 7):
            b.set(x, G, z, "minecraft:green_glazed_terracotta", {"facing": "north"})
    b.prop("pai_sho", x1 - 3, G + 1, z0 + 5, wie="hier komt de Pai Sho-tafel: 3 x 3 vrij op de groene mat")
    # the kitchen
    b.set(x0 + 3, G + 1, z1 - 1, "minecraft:furnace", {"facing": "north", "lit": "false"})
    b.set(x0 + 3, G + 2, z1 - 1, "minecraft:water_cauldron", {"level": "3"})
    b.prop("theeketel", x0 + 3, G + 2, z1 - 1, wie="de grote theeketel op het fornuis in de keuken")
    b.set(x0 + 4, G + 1, z1 - 1, "minecraft:smoker", {"facing": "north", "lit": "false"})
    b.set(x0 + 5, G + 1, z1 - 1, THEEKIST)
    b.set(x0 + 6, G + 1, z1 - 1, THEEKIST)
    b.set(x0 + 5, G + 2, z1 - 1, THEEKIST)
    b.set(x0 + 8, G + 1, z1 - 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.chest(x0 + 10, G + 1, z1 - 1, "north", "guhs:chests/einde2_bouw_aarde_theehuis")
    b.set(x1 - 1, G + 1, z1 - 1, "minecraft:cauldron")
    b.set(x1 - 1, G + 1, z1 - 2, THEEKIST)
    b.sign(x0 + 9, G + 3, z1 - 1, "north", ["Regel 1:", "thee trekt,", "de ober", "niet. Njeg."])
    b.sign(x0 + 7, G + 3, z0 + 1, "south", ["Jasmijnthee 2 kn", "Knabbelthee 1 kn", "Chagrijn: gratis", "(vraag de ober)"])
    for x, z in ((x0 + 4, z0 + 4), (x1 - 4, z0 + 2), (80, z1 - 2)):
        b.set(x, G + 4, z, LAMPION, lamp(True))
    b.zone("theehuis", x0 - 1, G + 1, z0 - 4, x1 + 1, G + 16, z1 + 1, wie="De Jasmijn Draak met zijn terras")
    b.zone("keuken", x0 + 1, G + 1, kz + 1, x1 - 1, G + 4, z1 - 1, wie="de keuken achter de toonbank")
    b.plek("miroh", x0 + 7, G + 1, kz + 1, yaw=180.0, wie="Oom Miroh, achter de toonbank", kind="OOM_MIROH")
    b.plek("zuka_ober", 79, G + 1, z0 + 4, yaw=180.0, wie="Zuka, de chagrijnige ober", kind="ZUKA")
    for i, (x, z, yaw) in enumerate(((x0 + 5, z0 + 3, 180.0), (x1 - 3, z0 + 3, 180.0), (x0 + 5, z0 + 4, 0.0)), 1):
        b.plek(f"klant_{i}", x, G + 1, z, yaw=yaw, wie="een klant bij een theetafel", kind="BA_SING_GUH_BURGER")
    # the stair in the south-west corner (behind the counter's end) to the upper floor
    for i in range(5):
        z = z0 + 2 + i
        if i < 4:
            b.weg(x0 + 1, G + 5, z)
        b.set(x0 + 1, G + 1 + i, z, "minecraft:dark_oak_stairs", stair("south"))
        for y in range(G + 1, G + 1 + i):
            b.set(x0 + 1, y, z, hout)
    for z in range(z0 + 1, z0 + 6):
        b.set(x0 + 2, G + 6, z, "minecraft:dark_oak_fence")
    b.set(x0 + 1, G + 6, z0 + 1, "minecraft:dark_oak_fence")
    # upstairs: Miroh's rooms
    U = G + 6
    bed(b, x1 - 2, U, z1 - 1, "east", "green")
    b.set(x1 - 1, U, z1 - 3, "minecraft:bookshelf")
    b.set(x1 - 1, U + 1, z1 - 3, "minecraft:potted_white_tulip")
    b.set(x1 - 1, U, z0 + 1, "guhs:guh_tafel", {"facing": "west"})
    b.set(x1 - 1, U + 1, z0 + 1, "guhs:theepotje", {"facing": "west"})
    b.set(x1 - 2, U, z0 + 1, "guhs:guh_stoel", {"facing": "east"})
    for x in range(x0 + 5, x0 + 8):
        for z in range(z0 + 4, z0 + 7):
            b.set(x, U, z, "minecraft:lime_carpet" if (x + z) % 2 else "minecraft:green_carpet")
    b.set(x0 + 4, U, z1 - 1, THEEKIST)
    b.sign(x1 - 1, U + 1, z1 - 2, "west", ["Thee is als", "een knabbel:", "deel hem, dan", "smaakt hij beter"])
    for x, z in ((x0 + 4, z0 + 5), (x1 - 4, z0 + 5)):
        b.set(x, G + 8, z, LAMPION2, lamp(True))


# =====================================================================================================================
# Joo Dee-guh's house, the guest house
# =====================================================================================================================
def joo_dee(b):
    """A house that is exactly the same on the left and on the right, with a smile on its sign."""
    G = MIDDEN
    x0, z0, x1, z1 = 139, 62, 149, 68
    b.neem(x0 - 2, z0 - 2, x1 + 2, z1 + 3)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, "minecraft:stone_bricks" if rand else "minecraft:birch_planks")
            if rand:
                for y in range(G + 1, G + 5):
                    hoek = x in (x0, x1) and z in (z0, z1)
                    b.set(x, y, z, "minecraft:quartz_pillar" if hoek else "minecraft:white_concrete", {"axis": "y"} if hoek else None)
            b.set(x, G + 5, z, "minecraft:birch_planks")
    xm = (x0 + x1) // 2
    gb.door(b, xm, G + 1, z1, "north", "birch")
    for dx in (2, 4):
        for x in (xm - dx, xm + dx):
            b.set(x, G + 2, z1, "minecraft:glass_pane")
            b.set(x, G + 2, z0, "minecraft:glass_pane")
    zadeldak(b, x0, z0, x1, z1, G + 5, TEGELT, TEGELP, "minecraft:white_concrete")
    for x in (xm - 2, xm + 2):
        b.set(x, G + 1, z1 + 1, "minecraft:potted_white_tulip")
        b.set(x, G + 4, z1 + 1, LAMPION, lamp(True))
    b.sign(xm, G + 3, z1 + 1, "south", ["Joo Dee-guh", "Uw blije gids.", "Er is geen", "knabbeltekort."], glow=True)
    for x in (xm - 3, xm + 3):
        bed(b, x, G + 1, z0 + 1, "south", "white")
        b.set(x, G + 4, z0 + 3, "minecraft:lantern", lamp(True))
    b.set(xm, G + 1, z0 + 1, "minecraft:bookshelf")
    b.sign(xm, G + 2, z0 + 1, "south", ["Lachoefening 7:", "mondhoeken op,", "ogen wijd.", "Nog wijder."])
    b.sign(x0 + 1, G + 2, z0 + 3, "east", ["Ik ben Joo Dee.", "Jij bent welkom.", "Alles is fijn.", "ALLES."])
    b.sign(x1 - 1, G + 2, z0 + 3, "west", ["Joo Dee 3 van 7", "(deze week)", "", ""])
    b.zone("joo_dee_huis", x0, G + 1, z0, x1, G + 9, z1 + 1, wie="het huis van Joo Dee-guh (links en rechts precies gelijk)")
    b.plek("joo_dee", xm, G + 1, z1 + 3, yaw=0.0, wie="Joo Dee-guh, voor haar deur", kind="JOO_DEE_GUH")


def gasthuis(b):
    """The house the city lends its guests, in the upper ring south of the palace: a room with a table, four beds."""
    G = BOVEN
    x0, z0, x1, z1 = 139, 149, 153, 159
    b.neem(x0 - 2, z0 - 2, x1 + 2, z1 + 2)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, "minecraft:polished_andesite" if rand else "minecraft:birch_planks")
            if rand:
                for y in range(G + 1, G + 5):
                    hoek = x in (x0, x1) and z in (z0, z1)
                    b.set(x, y, z, "minecraft:quartz_pillar" if hoek else "minecraft:smooth_sandstone", {"axis": "y"} if hoek else None)
            b.set(x, G + 5, z, "minecraft:birch_planks")
    zm = (z0 + z1) // 2
    gb.door(b, x0, G + 1, zm, "east", "birch")
    for z in (z0 + 2, z1 - 2):
        b.set(x0, G + 2, z, "minecraft:lime_stained_glass_pane")
        b.set(x1, G + 2, z, "minecraft:lime_stained_glass_pane")
    for x in range(x0 + 2, x1 - 1, 3):
        b.set(x, G + 2, z0, "minecraft:lime_stained_glass_pane")
        b.set(x, G + 2, z1, "minecraft:lime_stained_glass_pane")
    pagodedak(b, x0, z0, x1, z1, G + 5, trap=GOUDT, plaat=GOUDP, blok=TEGEL)
    for i, kleur in enumerate(("orange", "light_blue", "blue", "green")):
        bed(b, x1 - 1 - 3 * (i // 2) - 1, G + 1, z0 + 1 if i % 2 == 0 else z1 - 1, "east", kleur)
    b.set(x0 + 4, G + 1, zm, "guhs:theetafel", {"gedekt": "true"})
    for dx, f in ((-1, "east"), (1, "west")):
        b.set(x0 + 4 + dx, G + 1, zm, "guhs:guh_stoel", {"facing": f})
    b.chest(x0 + 1, G + 1, z0 + 1, "south", "guhs:chests/einde2_bouw_aarde_huis")
    for x in (x0 + 4, x1 - 4):
        b.set(x, G + 4, zm, LAMPION2, lamp(True))
    b.sign(x0 - 1, G + 2, zm - 1, "west", ["Gasthuis", "Voor gasten van", "de stad. Blijf", "gerust binnen."])
    b.sign(x1 - 1, G + 3, zm, "west", ["Bedden: 4", "Luchtbizons: 0", "(die past niet", "door de deur)"])
    for x, z in ((x0 - 2, zm - 2), (x0 - 2, zm + 2)):
        lantaarn(b, x, z, paal=GOUDM, hoog=2)
    b.zone("gasthuis", x0, G + 1, z0, x1, G + 9, z1, wie="het gasthuis van de bovenring")


# =====================================================================================================================
# the palace
# =====================================================================================================================
def paleis(b):
    """The palace of the Aardekoning-guh on a podium in the middle of the upper ring: the throne hall with two rows of
    green pillars, Long Feng-Mika's study and bear Bosco's room on its north side, the library and the royal bedroom on its
    south side, a golden roof in two tiers. Behind it the garden with a pond that pours into the Koningsput."""
    G = BOVEN
    P = G + 4                                                   # the podium's top block
    x0, z0, x1, z1 = PALEIS
    b.neem(113, z0 - 5, x1 + 8, z1 + 8)
    b.schil(x0 - 3, G + 1, z0 - 3, x1 + 3, P, z1 + 3, "minecraft:polished_andesite")
    for x in range(x0 - 3, x1 + 4):
        for z in range(z0 - 3, z1 + 4):
            rand = x in (x0 - 3, x1 + 3) or z in (z0 - 3, z1 + 3)
            b.set(x, P, z, GOUD if rand or (x + z) % 6 == 0 else "minecraft:polished_andesite")
    # the four stairs: the great one on the west (thirteen wide), small ones on the other sides
    def trap(cellen, facing, breed):
        for i, (x, z) in enumerate(cellen):
            dx, dz = (0, 1) if facing in ("east", "west") else (1, 0)
            for v in breed:
                xx, zz = x + dx * v, z + dz * v
                for y in range(G + 1, G + 1 + i):
                    b.set(xx, y, zz, "minecraft:polished_andesite")
                b.set(xx, G + 1 + i, zz, GOUDT, stair(facing))
                b.bezet.add((xx, zz))
    trap([(x0 - 7 + i, 128) for i in range(4)], "east", range(-6, 7))
    trap([(134, z0 - 7 + i) for i in range(4)], "south", range(-2, 3))
    trap([(134, z1 + 7 - i) for i in range(4)], "north", range(-2, 3))
    trap([(x1 + 7 - i, 136) for i in range(4)], "west", range(-2, 3))
    for z in (121, 135):
        for i in range(4):
            for y in range(G + 1, G + 2 + i):
                b.set(x0 - 7 + i, y, z, "minecraft:polished_andesite")
            b.set(x0 - 7 + i, G + 2 + i, z, GOUDM)
            b.bezet.add((x0 - 7 + i, z))
    # walls: the outer walls, the two long walls of the hall, the rooms
    zand, groen = "minecraft:smooth_sandstone", "minecraft:green_terracotta"
    def muur(x, z, y0, y1):
        for y in range(y0, y1 + 1):
            b.set(x, y, z, GOUD if y == y0 else zand)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            buiten = x in (x0, x1) or z in (z0, z1)
            hal = 121 <= z <= 135
            if buiten:
                if (x - x0) % 4 == 0 and (z - z0) % 4 == 0 or (x in (x0, x1) and z in (z0, z1)):
                    for y in range(P + 1, P + 9):
                        b.set(x, y, z, groen)
                else:
                    muur(x, z, P + 1, P + 8)
            elif z in (121, 135):
                muur(x, z, P + 1, P + 10)
            elif (x == 135 and z < 121) or (x == 137 and z > 135):
                muur(x, z, P + 1, P + 5)
            if hal and x in (x0, x1):
                muur(x, z, P + 1, P + 10)
            # ceilings: the side rooms at P + 6, the hall at P + 10
            if not buiten and not hal:
                b.set(x, P + 6, z, "minecraft:birch_planks")
            elif not buiten and 121 < z < 135:
                b.set(x, P + 10, z, "minecraft:dark_oak_planks")
    # windows in the outer walls, the doors
    for x in range(x0 + 2, x1 - 1):
        if (x - x0) % 4 == 2:
            for z in (z0, z1):
                for y in (P + 3, P + 4):
                    b.set(x, y, z, "minecraft:lime_stained_glass_pane")
    for z in list(range(z0 + 2, 120)) + list(range(137, z1 - 1)):
        if (z - z0) % 4 == 2:
            for x in (x0, x1):
                for y in (P + 3, P + 4):
                    b.set(x, y, z, "minecraft:lime_stained_glass_pane")
    for z in (127, 128, 129):
        for y in range(P + 1, P + 6):
            b.weg(x0, y, z)
    for x, z in ((128, 121), (129, 135), (143, 135)):
        gb.door(b, x, P + 1, z, "south" if z == 135 else "north", "dark_oak")
        b.weg(x, P + 3, z)
        b.set(x, P + 3, z, zand)
    for x in (142, 143):
        for y in (P + 1, P + 2, P + 3):
            b.weg(x, y, 121)
    # the hall: pillars with a froglight in them, the carpet, the dais and the throne
    for z in (124, 132):
        for x in range(126, 143, 4):
            for y in range(P + 1, P + 10):
                b.set(x, y, z, GOUD if y in (P + 1, P + 9) else "minecraft:ochre_froglight" if y == P + 5 else groen, {"axis": "y"} if y == P + 5 else None)
    for x in range(x0 + 1, 143):
        for z in (127, 128, 129):
            b.set(x, P + 1, z, "minecraft:green_carpet" if z == 128 else "minecraft:yellow_carpet")
    for x in range(144, 150):
        for z in range(124, 133):
            b.set(x, P + 1, z, GOUD)
    for z in range(124, 133):
        b.set(143, P + 1, z, GOUDT, stair("east"))
    b.set(147, P + 2, 128, GOUDT, stair("east"))
    for z in (127, 129):
        b.set(147, P + 2, z, GOUDP, slab())
    for z in range(125, 132):                                    # the panel behind the throne: the aardemunt in a glazed frame
        for y in range(P + 2, P + 8):
            if (y, z) == (P + 5, 128):
                b.set(149, y, z, MUNT)
            elif abs(z - 128) == 2 or y == P + 7:
                b.set(149, y, z, "minecraft:green_glazed_terracotta", {"facing": "west"})
            else:
                b.set(149, y, z, groen)
    for z in (125, 131):
        b.set(148, P + 2, z, LAMPION2, lamp(False))
    b.plek("aardekoning", 146, P + 2, 128, yaw=90.0, wie="de Aardekoning-guh, voor zijn troon", kind="AARDEKONING_GUH")
    b.plek("long_feng", 145, P + 2, 130, yaw=90.0, wie="Long Feng-Mika, vlak naast de troon (hij fluistert)", kind="LONG_FENG_MIKA")
    b.sign(x0 + 1, P + 3, 126, "east", ["Troonzaal", "Buigen mag.", "Knabbels aan", "de koning: moet."])
    # Long Feng-Mika's study
    for x in range(124, 134):
        for y in (P + 1, P + 2, P + 3):
            b.set(x, y, z0 + 1, "minecraft:bookshelf")
    b.set(126, P + 1, 118, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    b.set(131, P + 1, 118, "guhs:guh_tafel", {"facing": "south"})
    b.set(131, P + 2, 118, "guhs:theepotje", {"facing": "south"})
    b.sign(134, P + 2, 118, "west", ["Long Feng-Mika", "Cultuurminister", "Er is hier", "niks aan de hand"])
    b.sign(123, P + 2, 119, "east", ["Dossier Guhvatar", "(dik)", "Dossier Bosco", "(dikker)"])
    # bear Bosco's room: a nest of hay, his own little throne, honeycomb, a bath
    for x in range(145, 149):
        for z in range(115, 118):
            b.set(x, P + 1, z, "minecraft:hay_block", {"axis": "y"})
            b.set(x, P + 2, z, "minecraft:brown_carpet")
    b.set(137, P + 1, 115, GOUDT, stair("north"))
    b.set(138, P + 1, 115, "minecraft:honeycomb_block")
    b.set(139, P + 1, 115, "minecraft:honeycomb_block")
    b.set(138, P + 2, 115, "minecraft:honeycomb_block")
    b.set(137, P + 1, 119, "minecraft:water_cauldron", {"level": "3"})
    b.set(141, P + 1, 115, "minecraft:melon")
    b.sign(140, P + 2, 120, "north", ["Beer Bosco", "Gewoon een beer.", "Niks geks aan.", "Njeg."])
    b.sign(144, P + 2, 115, "south", ["Mandje van", "Bosco. Niet in", "gaan liggen.", "(Hij merkt het.)"])
    b.plek("bosco", 143, P + 1, 117, yaw=0.0, wie="beer Bosco, in zijn kamer", kind="BOSCO")
    b.zone("bosco_kamer", 136, P + 1, 115, 149, P + 5, 120, wie="de kamer van beer Bosco")
    # the library and the royal bedroom
    for x in range(124, 136, 2):
        for y in (P + 1, P + 2, P + 3):
            b.set(x, y, z1 - 1, "minecraft:bookshelf")
    b.set(126, P + 1, 138, "guhs:guh_tafel", {"facing": "north"})
    b.set(127, P + 1, 138, "guhs:guh_stoel", {"facing": "west"})
    b.sign(123, P + 2, 139, "east", ["Bibliotheek", "Boeken over de", "oorlog: geen.", "Oorlog? Welke?"])
    bed(b, 147, P + 1, 141, "east", "yellow")
    b.set(149, P + 1, 139, "minecraft:bookshelf")
    b.chest(139, P + 1, 141, "north", "guhs:chests/einde2_bouw_aarde_paleis")
    b.set(141, P + 1, 141, "minecraft:loom", {"facing": "north"})
    for x, z in ((128, 117), (140, 118), (146, 119), (128, 139), (134, 138), (144, 139)):
        b.set(x, P + 5, z, "minecraft:lantern", lamp(True))
    # the roof: a golden tier with green hips, a storey with windows, a second tier and a finial
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                for y in (P + 9, P + 10):
                    b.set(x, y, z, zand)
    y = pagodedak(b, x0, z0, x1, z1, P + 9, trap=GOUDT, plaat=GOUDP, blok=TEGEL, uit=2, lagen=5)
    for x in range(128, 145):
        for z in range(120, 137):
            rand = x in (128, 144) or z in (120, 136)
            if rand:
                for yy in range(y + 1, y + 8):
                    hoek = x in (128, 144) and z in (120, 136)
                    b.set(x, yy, z, groen if hoek else "minecraft:lime_stained_glass_pane" if yy in (y + 2, y + 3) and (x + z) % 2 == 0 else zand)
            elif (x, z) in ((131, 123), (141, 123), (131, 133), (141, 133)):
                b.set(x, y + 1, z, LAMPION2, lamp(False))
    y2 = pagodedak(b, 128, 120, 144, 136, y + 6, trap=GOUDT, plaat=GOUDP, blok=TEGEL, uit=2)
    b.set(136, y2 + 1, 128, GOUDM)
    b.set(136, y2 + 2, 128, GOUDM)
    b.set(136, y2 + 3, 128, "minecraft:lantern", lamp(False))
    # lamps on the terrace
    for x, z in ((x0 - 3, z0 - 3), (x1 + 3, z0 - 3), (x0 - 3, z1 + 3), (x1 + 3, z1 + 3), (x0 - 3, 121), (x0 - 3, 135), (134 - 3, z0 - 3), (134 + 3, z1 + 3),
                 (x1 + 3, 128), (134 + 3, z0 - 3), (134 - 3, z1 + 3)):
        b.set(x, P + 1, z, GOUDM)
        b.set(x, P + 2, z, "minecraft:lantern", lamp(False))
    b.zone("paleis", x0 - 3, G + 1, z0 - 3, x1 + 3, y2 + 3, z1 + 3, wie="het paleis van de Aardekoning-guh met zijn podium")
    b.plek("dai_li_6", x0 - 2, P + 1, 124, yaw=90.0, wie="een Dai Li-Mika op het paleisterras", kind="DAI_LI_MIKA")


def paleistuin(b):
    """The garden behind the palace: a pond whose water pours into the Koningsput (the shaft of 9 x 7 down to the
    kristalkamer, with a railing around it), trees and lamps."""
    G = BOVEN
    ax, az, bx, bz = PUT
    b.neem(155, 112, 168, 134)
    for x in range(155, 169):
        for z in range(112, 135):
            if afstand(x, z) < 36 and (x, z) in b.kolom:
                b.set(x, G, z, "minecraft:moss_block")
    # the shaft and its railing (carved down to the chamber by einde2_bouw_aarde_onder)
    for x in range(ax - 1, bx + 2):
        for z in range(az - 1, bz + 2):
            if not (ax <= x <= bx and az <= z <= bz):
                b.set(x, G, z, GOUD)
                b.set(x, G + 1, z, GOUDM)
    # the pond north of it, its outlet through the railing
    for x in range(ax + 1, bx):
        for z in range(az - 8, az - 2):
            b.set(x, G - 1, z, KLIP)
            b.set(x, G, z, "minecraft:water", WATER)
            if _hash(x, z, 61) < 0.12:
                b.set(x, G + 1, z, "minecraft:lily_pad")
    for x in range(ax, bx + 1):
        for z in range(az - 9, az - 1):
            if b.naam(x, G, z) != "minecraft:water":
                b.set(x, G, z, GOUD)
    xm = (ax + bx) // 2
    for z in (az - 2, az - 1):
        b.set(xm, G - 1, z, KLIP)
        b.set(xm, G, z, "minecraft:water", WATER)
    for x, z in ((ax - 2, az - 9), (bx + 2, az - 9), (ax - 2, bz + 2), (bx + 2, bz + 2)):
        if (x, z) in b.kolom:
            b.set(x, G + 1, z, GOUDM)
            b.set(x, G + 2, z, GOUDM)
            b.set(x, G + 3, z, "minecraft:lantern", lamp(False))
    boom(b, 167, 134)
    boom(b, 166, 113)
    for x, z in ((156, 119), (167, 120), (156, 133), (166, 133)):
        if b.naam(x, G + 1, z) is None:
            b.set(x, G + 1, z, "minecraft:flowering_azalea_leaves", BLAD)
    b.sign(ax - 2, G + 1, 127, "west", ["De Koningsput", "Niet in springen", "(ook geen", "luchtbizons)"], wall=False)
    return xm


def oude_poort_dak(b_, ax, az, bx, bz, y):
    pagodedak(b_, ax, az, bx, bz, y, trap="minecraft:mossy_stone_brick_stairs", plaat="minecraft:mossy_stone_brick_slab",
              blok="minecraft:mossy_stone_bricks", lagen=3)


def oude_poort(b):
    """The top of the tower down to the catacombs: an old, mossy gate house in the palace garden."""
    G = BOVEN
    x0, z0 = OUDE_POORT
    onder, boven = traptoren(b, x0, z0, 30, G + 1, ("east", -1), ("south", 1), "minecraft:mossy_stone_bricks", "minecraft:stone_brick_stairs",
                             kern="minecraft:stone_bricks", ramen=False, dak=oude_poort_dak)
    b.neem(x0 - 1, z0 - 1, x0 + 7, z0 + 9)
    b.sign(x0 + 3, G + 3, z0 + 7, "south", ["Oud Ba Sing Guh", "Verboden toegang", "Er is hier", "niets. Echt."])
    b.set(x0 + 4, G + 1, z0 + 7, KRISTAL, {"facing": "up", "waterlogged": "false"})
    b.prop("catacomben_ingang", x0 + 2, G + 1, z0 + 6, wie="de deur van de oude poort in de paleistuin: daarachter de trap omlaag naar de catacomben")
    return onder


# =====================================================================================================================
# the quarters: stalls, houses, lamps, trees, the people on the street
# =====================================================================================================================
def kramen(b):
    """The three spots of the koolman's stall (kept free: the stall itself is an overlay a later phase shows), each on a
    little paved square with a sign, and a few ordinary stalls on the market of the lower ring."""
    for i, (x, z) in enumerate(KRAMEN, 1):
        G = b.grond(x, z)
        for xx in range(x - 2, x + 8):
            for zz in range(z - 1, z + 6):
                if (xx, zz) in b.kolom and b.kolom[(xx, zz)][0] == G and b.naam(xx, G + 1, zz) is None:
                    b.plavei(xx, zz, GOUD if (xx in (x - 2, x + 7) or zz in (z - 1, z + 5)) else straatsteen(xx, zz, b.kolom[(xx, zz)][2]))
        b.prop(f"koolkraam_{i}", x, G + 1, z, wie="de hoek (laagste x, y, z) van de koolkraam: 5 x 4 x 4 vrij")
    x, z = KRAMEN[0]
    G = ONDER
    b.plek("koolman", x + 6, G + 1, z + 1, yaw=90.0, wie="de koolman, naast zijn kraam", kind="KOOLMAN")
    b.sign(x - 2, G + 1, z + 5, "north", ["Kool van", "de koolman", "Vers! Heel!", "(nog wel)"], wall=False)
    x, z = KRAMEN[1]
    b.sign(x + 6, MIDDEN + 1, z + 5, "north", ["Kool. Nu ook", "in de Middenring", "Kom niet te", "dichtbij, njeg."], wall=False)
    x, z = KRAMEN[2]
    b.sign(x - 1, BOVEN + 1, z + 5, "north", ["Koninklijke", "kool. Niet", "omver lopen,", "alsjeblieft."], wall=False)
    # the market of the lower ring, south of the avenue
    for (x, z), doek, waar in (((53, 134), ("minecraft:orange_wool", "minecraft:yellow_wool"), "guhs:block_of_kaasknabbels"),
                               ((53, 140), ("minecraft:red_wool", "minecraft:white_wool"), "minecraft:hay_block"),
                               ((53, 146), ("minecraft:brown_wool", "minecraft:yellow_wool"), "minecraft:melon")):
        if b.vrij(x - 1, z - 1, x + 5, z + 4):
            kraam(lambda xx, y, zz, n, p: b.set(xx, y, zz, n, p), x, ONDER + 1, z, doek=doek, waar=waar)
            b.neem(x - 1, z - 1, x + 5, z + 4)
    b.sign(59, ONDER + 1, 138, "west", ["Markt", "Knabbels genoeg.", "Echt. Vraag het", "maar aan Joo Dee"], wall=False)


BORDEN = {
    "onder": [["Kamer te huur", "Uitzicht op", "de muur. En op", "de andere muur."], ["Hier woont", "Guh Li. Niet", "de Dai Li.", "Ander iemand."],
              ["Knabbelkoek", "van oma-guh", "Op = op.", "(Het is op.)"], ["Aardestuurles", "Eerste steen", "gratis.", ""],
              ["Wasserette", "De Natte Guh", "Ook sokken", "van luchtbizons"], ["Pas op: vadsige", "guh. Bijt niet.", "Ligt wel", "in de weg."],
              ["Te koop:", "kool. Bijna", "niet onder een", "kar gekomen."], ["Boekwinkel", "Alles over de", "oorlog: nee.", "Kookboeken: ja."]],
    "midden": [["Universiteit", "van Ba Sing Guh", "Vak: vadsen.", "Niveau: hoog."], ["Badhuis", "Guhs links,", "Mika's niet.", ""],
               ["Kleermaker", "Groen. Of groen.", "Of anders:", "groen."]],
    "boven": [["Villa Vadszicht", "Alleen op", "afspraak (en", "met knabbels)."]],
}


def wijken(b):
    """The houses: the lower ring is packed with small mud houses, the middle ring has roomier white ones with green roofs,
    the upper ring a few villas with golden roofs. Now and then a house has a sign next to its door."""
    r = random.Random(kit.seed("bouw_aarde/wijken"))
    n = {"onder": 0, "midden": 0, "boven": 0}
    borden = {k: list(v) for k, v in BORDEN.items()}
    for stijl, d0, d1, pogingen, maten in (("onder", R_MUUR2 + 1.5, R_ONDER - 1.5, 30000, ((7, 6), (6, 7), (6, 6), (7, 5), (5, 7), (6, 5), (5, 6), (5, 5))),
                                           ("midden", R_MUUR3 + 6.5, R_MIDDEN - 1.5, 12000, ((9, 7), (7, 9), (8, 7), (7, 8), (7, 7), (8, 6), (6, 8), (7, 6), (6, 7), (6, 6))),
                                           ("boven", 4, R_BOVEN - 3.5, 6000, ((9, 7), (7, 9), (8, 7), (7, 8)))):
        for poging in range(pogingen):
            hoek, dd = r.uniform(0, 360), r.uniform(d0 + 3, d1 - 3)
            w, d = maten[min(len(maten) - 1, poging * len(maten) // pogingen)]      # (the big ones first, the small ones fill the gaps)
            cx, cz = CX + dd * math.cos(math.radians(hoek)), CZ + dd * math.sin(math.radians(hoek))
            x0, z0 = int(cx - w / 2), int(cz - d / 2)
            x1, z1 = x0 + w - 1, z0 + d - 1
            if not all(d0 <= afstand(x, z) <= d1 for x in (x0 - 1, x1 + 1) for z in (z0 - 1, z1 + 1)):
                continue
            if not b.vrij(x0 - 1, z0 - 1, x1 + 1, z1 + 1):
                continue
            # the door looks at the ring road of its ring
            weg_d = {"onder": 89.5, "midden": 49, "boven": 38}[stijl]
            naar = (1 if dd < weg_d else -1)
            c, s = math.cos(math.radians(hoek)) * naar, math.sin(math.radians(hoek)) * naar
            deur = ("east" if c > 0 else "west") if abs(c) >= abs(s) else ("south" if s > 0 else "north")
            om = {"onder": 7, "midden": 6, "boven": 2}[stijl]
            bord = borden[stijl].pop(0) if borden[stijl] and n[stijl] % om == om - 1 else None
            huis(b, x0, z0, w, d, stijl, deur, r, bord=bord)
            n[stijl] += 1
    b.huizen = n


def pleintjes(b):
    """Before the houses come: a few wells and fenced patches of kool in the lower ring (they keep their room)."""
    _klein(b, random.Random(kit.seed("bouw_aarde/pleintjes")), 900, ("kooltuin", "kooltuin", "put"), {"kooltuin": 7, "put": 4})


def rommel(b):
    """After the houses: stacks of barrels, crates and hay in the gaps of the lower ring, and lamps on posts in every ring
    wherever the ground is free, so the alleys and gardens are lit."""
    r = random.Random(kit.seed("bouw_aarde/rommel"))
    _klein(b, r, 2600, ("stapel",), {"stapel": 40})
    # lamps on a loose grid in every ring, wherever the ground is free
    for gx in range(24, 246, 8):
        for gz in range(18, 240, 8):
            x, z = gx + r.randint(-2, 2), gz + r.randint(-2, 2)
            k = b.kolom.get((x, z))
            if k is None or k[2] >= R_ONDER - 2 or (x, z) in b.bezet:
                continue
            if k[2] < R_BOVEN:
                lantaarn(b, x, z, paal=GOUDM, hoog=2, licht=LAMPION2)
            elif k[2] < R_MIDDEN:
                lantaarn(b, x, z, paal="minecraft:dark_oak_fence", hoog=2, licht=LAMPION)
            else:
                lantaarn(b, x, z, paal="minecraft:spruce_fence", hoog=2)
            b.rommel["lamp"] = b.rommel.get("lamp", 0) + 1


def _klein(b, r, pogingen, soorten, hoogstens):
    """The small things of the lower ring, thrown at random spots: a fenced patch of kool (5 x 4), a well with a little roof
    (3 x 3), a stack of barrels, crates and hay."""
    n = b.rommel
    for _ in range(pogingen):
        hoek, dd = r.uniform(0, 360), r.uniform(R_MUUR2 + 5, R_ONDER - 5)
        x, z = pol(dd, hoek)
        G = ONDER
        soort = r.choice(soorten)
        if n.get(soort, 0) >= hoogstens[soort]:
            continue
        if soort == "kooltuin" and b.vrij(x - 1, z - 1, x + 5, z + 4):
            for xx in range(x, x + 5):
                for zz in range(z, z + 4):
                    rand = xx in (x, x + 4) or zz in (z, z + 3)
                    b.set(xx, G, zz, "minecraft:coarse_dirt")
                    if rand and not (xx == x + 2 and zz == z):
                        b.set(xx, G + 1, zz, "minecraft:spruce_fence")
                    elif not rand and r.random() < 0.8:
                        b.set(xx, G + 1, zz, KOOL)
            b.set(x + 2, G + 1, z, "minecraft:spruce_fence_gate", {"facing": "north", "in_wall": "false", "open": "false", "powered": "false"})
            b.neem(x - 1, z - 1, x + 5, z + 4)
        elif soort == "put" and b.vrij(x - 1, z - 1, x + 3, z + 3):
            for xx in range(x, x + 3):
                for zz in range(z, z + 3):
                    midden = (xx, zz) == (x + 1, z + 1)
                    b.set(xx, G, zz, "minecraft:cobblestone")
                    b.set(xx, G + 1, zz, "minecraft:water_cauldron" if midden else "minecraft:cobblestone_wall" if (xx - x) % 2 == 0 and (zz - z) % 2 == 0 else "minecraft:cobblestone_slab",
                          {"level": "3"} if midden else None if (xx - x) % 2 == 0 and (zz - z) % 2 == 0 else slab())
            for xx, zz in ((x, z), (x + 2, z), (x, z + 2), (x + 2, z + 2)):
                b.set(xx, G + 2, zz, "minecraft:cobblestone_wall")
            for xx in range(x, x + 3):
                for zz in range(z, z + 3):
                    b.set(xx, G + 3, zz, "minecraft:spruce_slab", slab())
            b.set(x + 1, G + 2, z + 1, "minecraft:lantern", lamp(True))
            b.neem(x - 1, z - 1, x + 3, z + 3)
        elif soort == "stapel" and b.vrij(x - 1, z - 1, x + 2, z + 2):
            dingen = [("minecraft:barrel", {"facing": "up", "open": "false"}), ("minecraft:hay_block", {"axis": "y"}), (THEEKIST, None),
                      ("minecraft:composter", {"level": "0"})]
            for xx, zz, hoog in ((x, z, 2), (x + 1, z, 1), (x, z + 1, 1)):
                if r.random() < 0.85:
                    for y in range(G + 1, G + 1 + hoog):
                        ding, props = r.choice(dingen[:3] if y > G + 1 else dingen)
                        b.set(xx, y, zz, ding, props)
            b.neem(x, z, x + 1, z + 1)
        else:
            continue
        n[soort] = n.get(soort, 0) + 1


def lampen(b):
    """Street lamps: along the avenue and the three other roads, on the ring roads, along the inside and the outside of the
    outer wall. (What stays dark after this gets a glowing tile from the safety net.)"""
    for x in range(36, 114, 9):
        for z in (124, 132):
            if (x, z) in b.kolom and (x, z) not in b.bezet:
                lantaarn(b, x, z, paal=GOUDM if afstand(x, z) < R_BOVEN else KLIPM)
    for z in list(range(28, 108, 10)) + list(range(150, 230, 10)):
        for x in (131, 137):
            if (x, z) in b.kolom and (x, z) not in b.bezet:
                lantaarn(b, x, z)
    for x in range(172, 210, 10):
        for z in (125, 131):
            if (x, z) in b.kolom and (x, z) not in b.bezet:
                lantaarn(b, x, z)
    for d, stap in ((86.3, 9), (91.6, 9), (46.9, 20), (51.2, 15), (35.6, 30), (101.5, 6), (112.6, 4)):
        for graden in range(0, 360, stap):
            x, z = pol(d, graden + (stap // 2 if d in (91.6, 51.2) else 0))
            if (x, z) in b.kolom and (x, z) not in b.bezet:
                lantaarn(b, x, z, paal=GOUDM if d < R_BOVEN else KLIPM)
    for graden in (45, 135, 225, 315):
        for d in (50, 62, 80, 96):
            x, z = pol(d, graden + 2.2)
            if (x, z) in b.kolom and (x, z) not in b.bezet:
                lantaarn(b, x, z)


def groen(b):
    """Trees and flowers where nothing stands: a few in the middle ring, more in the upper ring; korstgras outside the wall."""
    r = random.Random(kit.seed("bouw_aarde/groen"))
    for _ in range(900):
        hoek, dd = r.uniform(0, 360), r.uniform(3, R_MIDDEN - 4)
        x, z = pol(dd, hoek)
        if dd > R_BOVEN - 3 and r.random() < 0.6:
            continue
        if b.vrij(x - 2, z - 2, x + 2, z + 2) and b.naam(x, b.grond(x, z), z) in ("minecraft:moss_block", "guhs:kaaskorst"):
            boom(b, x, z, stam=r.choice(("minecraft:birch_log", "minecraft:oak_log")),
                 blad=r.choice(("minecraft:flowering_azalea_leaves", "minecraft:azalea_leaves", "minecraft:oak_leaves")), hoog=r.choice((3, 4, 4)))
    for (x, z), (t, bt, d) in b.kolom.items():
        if (x, z) in b.bezet or b.naam(x, t + 1, z) is not None:
            continue
        v = _hash(x, z, 71)
        onder = b.naam(x, t, z)
        if d >= R_MUUR1 and v < 0.07 and onder in (KLIP, "guhs:kaaskorst"):
            b.set(x, t + 1, z, "guhs:korstgras")
        elif d < R_MIDDEN and onder == "minecraft:moss_block" and v < 0.06:
            b.set(x, t + 1, z, ("minecraft:white_tulip", "minecraft:oxeye_daisy", "minecraft:fern", "minecraft:short_grass", "minecraft:azure_bluet", "minecraft:dandelion")[int(v * 100)])
        elif R_MUUR2 <= d < R_ONDER and v < 0.02 and onder == "guhs:gebarsten_korst":
            b.set(x, t + 1, z, "guhs:dorre_korststruik")


def volk(b):
    """Where the people of the city stand (markers only): ten citizens on the roads and squares of the three rings, five
    more Dai Li-Mika's on corners from where they can follow you, and where Sjokkel creeps along a ring wall."""
    def vrije_plek(x, z):
        k = b.kolom[(x, z)]
        for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (2, 0), (0, 2), (-2, 0), (0, -2)):
            if b.naam(x + dx, k[0] + 1, z + dz) is None and b.naam(x + dx, k[0] + 2, z + dz) is None and (x + dx, z + dz) in b.kolom \
                    and b.kolom[(x + dx, z + dz)][0] == k[0] and b.naam(x + dx, k[0], z + dz) not in (None, "minecraft:water"):
                return x + dx, k[0] + 1, z + dz
        raise SystemExit(f"{b.name}: geen vrije plek bij {(x, z)}")

    burgers = [(89, 215), (89, 250), (89, 300), (89, 335), (89, 40), (89, 72), (49, 215), (49, 300), (49, 60), (49, 110), (38, 240), (38, 100)]
    for i, (d, graden) in enumerate(burgers, 1):
        x, y, z = vrije_plek(*pol(d, graden))
        b.plek(f"bewoner_{i}", x, y, z, yaw=(graden + 90) % 360 - 180, wie="een burger van Ba Sing Guh", kind="BA_SING_GUH_BURGER")
    dai_li = [(pol(89, 172), 0, "bij de hoofdweg van de onderring"), (pol(89, 14), 90, "bij Meer Laogai"), ((80, 123), 0, "tegenover De Jasmijn Draak"),
              ((141, 72), 0, "bij het huis van Joo Dee-guh"), ((136, 148), -90, "bij het gasthuis")]
    for i, ((px, pz), yaw, wie) in enumerate(dai_li, 1):
        x, y, z = vrije_plek(px, pz)
        b.plek(f"dai_li_{i}", x, y, z, yaw=yaw, wie=f"een Dai Li-Mika {wie}", kind="DAI_LI_MIKA")
    # the wink: Sjokkel on the cap of the middle wall, seen from station Middenring
    b.pad("knipoog_sjokkel", [(68, MUUR2_TOP + 2, 100), (71, MUUR2_TOP + 2, 93)], wie="Sjokkel kruipt over de kap van de muur tussen onder- en middenring")
    b.camera("knipoog_sjokkel", (76, SPOOR_Y + 3, 106), (69, MUUR2_TOP + 2, 97), wie="vanaf het perron van station Middenring")
    b.zone("ring_onder", CX - R_MUUR1, ONDER + 1, CZ - R_MUUR1, CX + R_MUUR1, ONDER + 26, CZ + R_MUUR1, wie="de onderring (alles binnen de buitenmuur, buiten de middenmuur)")
    b.zone("ring_midden", CX - R_MIDDEN, MIDDEN + 1, CZ - R_MIDDEN, CX + R_MIDDEN, MIDDEN + 24, CZ + R_MIDDEN, wie="de middenring")
    b.zone("ring_boven", CX - R_BOVEN, BOVEN + 1, CZ - R_BOVEN, CX + R_BOVEN, BOVEN + 40, CZ + R_BOVEN, wie="de bovenring met het paleis")
