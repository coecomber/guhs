"""
bbq2 (guhrio-w3) - the lanes of world 3 of Super Guhrio, "de burcht", built with the engine's lane builder
(features/guhrio_baan.Baanbouwer; the manual: guhs_work130/reports/slice_guhrio-engine.md):

  bouw_3_1   level 3-1 "De Grillgang": you meet Guhshi, hop over the sauce from hub to hub of turning grill spits, bait two
             Plof-Mika's, and do the first Vuurpeper puzzle on foot (a knabbel through a slit flips the switch of a wall).
  bouw_3_2   level 3-2 "De Sauskelder": Guhshi's level: two gaps only his flutter jump crosses (or a timed bridge, on
             foot), falling blocks under fast spits, a platform under Plof-Mika's, and the Peperkamer behind a door.
  bouw_duel  the duel arena: a ledge, the grate bridge over the sauce, the far ledge with the lever, the pipe back and the
             door of the tower room; the Grote Nether-Mika's spot. DUEL_PLAN: where things are, for the boss
             (data/guhs/guhriow3/duel.json, written by guhrio_w3.build).

Every level can be finished without Guhshi (somebody may reach world 3 through a warp room, and Guhshi runs back to his
spot when something touches you): what only he reaches are shortcuts and vadsmunten. The numbers of the engine's jump: you
walk on the row above the ground's top row; a held jump rises 3.3 (a tap 1.5); a step up is at most 3; a gap is at most 3
wide on foot (4-5 with a run), about 12 on Guhshi; a ?-block goes 3 rows over your head's row.

This module only places blocks (ids as strings): the castle (guhrio_kasteel) calls its functions while guhrio.build runs.
"""
from features import guhrio_baan as gb

G = 3                         # the ground's top row
Y = G + 1                     # the row you walk on
TOP = 12                      # the main lane's last row in the two levels (the bonus rooms are rows 13..19)

ZWART, ZWART_BARST, ZWART_GLAD, VERGULD = ("minecraft:polished_blackstone_bricks", "minecraft:cracked_polished_blackstone_bricks",
                                           "minecraft:polished_blackstone", "minecraft:gilded_blackstone")
BLOK, SIER = "guhs:guhrio_blok", "guhs:guhrio_siersteen"
BRUG, HENDEL, BAAS, PAAL, STRUIK = ("guhs:guhriow3_brug", "guhs:guhriow3_hendel", "guhs:guhriow3_baas_plek", "guhs:guhriow3_parkeerpaal",
                                    "guhs:guhriow3_peperstruik")
GOUD, LICHT, KETTING = "minecraft:gold_block", "minecraft:shroomlight", "minecraft:chain"
SAUSKLEUR, NETHER, ROOD = "minecraft:orange_concrete", "minecraft:nether_bricks", "minecraft:red_nether_bricks"


# =====================================================================================================================
# shared bits
# =====================================================================================================================
def _steen(baan, s, y):
    """One block of the burcht's dark masonry: a cracked or a gilded one now and then."""
    k = baan.rng.random()
    baan.zet(s, y, ZWART_BARST if k < 0.1 else VERGULD if k < 0.14 else ZWART)


def _grond(baan, gaten):
    """Dark masonry from end to end (down to the slot's bottom, so the picture has no hole under it) except the gaps
    [(s0, s1)], which end in sauce; a wall at both ends; the sauce's glow painted behind every gap."""
    for s in range(baan.L):
        if not any(a <= s <= b for a, b in gaten):
            for y in range(0, G + 1):
                _steen(baan, s, y)
    for a, b in gaten:
        baan.saus(a, b)
        for s in range(a, b + 1):
            baan.decor(s, 0, SAUSKLEUR)
            baan.decor(s, 1, SAUSKLEUR if (s + a) % 3 else LICHT)
    for s in (0, baan.L - 1):
        for y in range(Y, TOP + 1):
            baan.zet(s, y, ZWART)


def _trap(baan, s0, hoog=4):
    """The big stairs before the flagpole: columns of 1 .. hoog blocks from s0 on."""
    for i in range(hoog):
        baan.blok(s0 + i, Y, hoog=i + 1)


def _spies(baan, s, y, plafond=TOP, **kw):
    """A grill spit whose hub hangs on a chain painted on the wall behind it."""
    baan.grillspies(s, y, **kw)
    for yy in range(y + 1, plafond + 1):
        baan.decor(s, yy, KETTING, {"axis": "y", "waterlogged": "false"})


def _kamer(baan, naam, s0, s1, wand, onder=13, boven=19):
    """A bonus room over the main lane: floor, ceiling, two walls, its own painted wall; a lane of its own."""
    baan.bijbaan(naam, s0, s1, onder, boven)
    baan.vul(s0, onder, s1, onder, BLOK)
    baan.vul(s0, boven, s1, boven, BLOK)
    for s in (s0, s1):
        baan.vul(s, onder, s, boven, BLOK)
    for s in range(s0, s1 + 1):
        for y in range(onder, boven + 1):
            baan.decor(s, y, wand(s, y))


# =====================================================================================================================
# 3-1 De Grillgang
# =====================================================================================================================
def bouw_3_1(baan):
    """
    s 2 start, 9 Guhshi | 16 the first spit (over solid ground) | 22-24 a gap | 25 flag, 30 Guhshi | 32-45 the sauce with
    three spit hubs to hop over (vadsmunt 0 over the middle one) | 47 flag, 48 Guhshi | 51, 55 Plof-Mika's (vadsmunt 1 under
    the second) |
    58 Guhshi's hitching post, 60 the Vuurpeper bush | 63-65 two Guhmba's in a pen | 71-76 the block with the slit: a knabbel
    through it flips the switch that opens the red wall at 79; on top of it the pipe (with a Hapbloem) to the treasury in the
    sky (vadsmunt 2; its way out drops you at 78, still in front of the wall) | 80 flag, 81 Guhshi | 82-85 the big stairs, a
    last Plof-Mika, 92 the flagpole.
    """
    baan.hoofdbaan(0, TOP)
    _grond(baan, [(22, 24), (32, 45)])
    baan.start(2, Y)
    baan.munten(5, 7, Y + 1)
    baan.guhshi(9, Y)
    baan.vraag(12, Y + 3, "superknabbel")
    # the first spit: over solid ground, with a ring of coins that shows how far it reaches
    _spies(baan, 16, Y + 4, lengte=3)
    for s, y in ((13, Y), (14, Y + 2), (18, Y + 2), (19, Y)):
        baan.munt(s, y)
    for s in (22, 23, 24):
        baan.munt(s, Y + 2 + (s == 23))
    baan.vlag(25, Y)
    baan.blok(26, Y)
    baan.guhmba(28, Y)
    baan.guhshi(30, Y)                                           # (he runs back to a spot when something touches you)
    # the grill spits over the sauce: three hubs to stand on (for a moment), each with its skewer
    for i, s in enumerate((34, 38, 42)):
        _spies(baan, s, G, lengte=2, tegen_klok=i % 2 == 1, fase=i)
    for s in (36, 40):
        baan.munt(s, Y + 2)
    baan.vadsmunt(38, Y + 3, 0)
    baan.vlag(47, Y)
    baan.guhshi(48, Y)
    # two Plof-Mika's: walk under them and they drop
    baan.plof_mika(51, Y + 5)
    baan.munt(51, Y)
    baan.plof_mika(55, Y + 6)
    baan.vadsmunt(55, Y + 2, 1)
    # the Vuurpeper puzzle: Guhshi waits at the post, the bush gives the Vuurpeper (as often as you like)
    baan.zet(58, Y, PAAL)
    baan.zet(60, Y, STRUIK)
    baan.blok(62, Y)
    baan.guhmba(63, Y)
    baan.guhmba(65, Y)
    baan.blok(66, Y)
    # the block with the slit under it (cells 71..74 of row Y; only a knabbel fits) and the switch at the slit's closed end
    baan.vul(71, Y + 1, 76, Y + 2, BLOK)
    baan.schakelaar(75, Y, 0, "aan")
    baan.blok(76, Y)
    baan.vul(79, Y, 79, TOP, gb.SCHAKELBLOK, {"kanaal": "0", "aan": "false", "open": "false"})
    baan.pijp(74, Y + 4, 1, hoog=2, bloem=True)                  # on top of the block: down to the treasury (up, really)
    baan.vlag(80, Y)
    baan.guhshi(81, Y)
    _trap(baan, 82)
    baan.plof_mika(88, Y + 7)
    baan.munten(87, 89, Y + 1)
    baan.mast(92, Y, hoog=7)
    # the treasury in the sky: you drop out of the ceiling, the pipe at the far end brings you back
    _kamer(baan, "schatkamer", 66, 90, lambda s, y: GOUD if (s + y) % 5 == 0 else "minecraft:yellow_concrete")
    baan.pijp(70, 18, 1, hoog=1, richting="omlaag")
    baan.munten(72, 78, 14)
    baan.munten(73, 77, 16)
    baan.blok(80, 14, hoog=2)
    baan.vadsmunt(80, 17, 2)
    baan.munten(82, 85, 14)
    baan.pijp(88, 15, 2, hoog=2)
    baan.pijp(78, 11, 2, hoog=2, richting="omlaag", ingang=False)   # (back in front of the red wall: the treasury is no way round it)


# =====================================================================================================================
# 3-2 De Sauskelder
# =====================================================================================================================
def bouw_3_2(baan):
    """
    s 2 start, 5 Guhshi | 10-18 a pen of three Guhmba's (Guhshi's tongue) | 22-31 the first big gap: flutter over it, or
    the timed switch at 20 lays a bridge (vadsmunt 0 high over it: on Guhshi or big) | 33 flag, 35 Guhshi | 36-51 falling
    blocks over the sauce under two fast spits (vadsmunt 1 low between the first two: a small hop) | 53 flag, 55 Guhshi |
    57-68 a platform glides under two Plof-Mika's | 70 flag, 72 the hitching post, 73 the door to the Peperkamer (rows
    13..19: the bush, a knabbel through the slit opens the cage with vadsmunt 2) | 76-83 the second big gap, a spit swinging
    up through it from below (the timed switch at 75) | 85 flag | 86-89 the big stairs, 92 the flagpole.
    """
    baan.hoofdbaan(0, TOP)
    _grond(baan, [(22, 31), (36, 51), (57, 68), (76, 83)])
    baan.start(2, Y)
    baan.guhshi(5, Y)
    baan.munten(6, 8, Y + 1)
    baan.vraag(8, Y + 3, "superknabbel")
    baan.blok(10, Y)
    for s in (12, 14, 16):
        baan.guhmba(s, Y)
    baan.vraag(14, Y + 3)
    baan.blok(18, Y)
    # the first big gap
    baan.schakelaar(20, Y + 3, 2, "tijd")
    for s in range(22, 32):
        baan.schakelblok(s, G, 2, aan=True)
    for s in range(23, 31):
        baan.munt(s, Y + 2 + min(s - 23, 30 - s, 2))
    baan.vadsmunt(27, Y + 5, 0)
    baan.vlag(33, Y)
    baan.guhshi(35, Y)
    # falling blocks under two spits
    for s in (37, 42, 47):
        baan.valblok(s, G, breed=2)
    _spies(baan, 40, Y + 4, lengte=3, snel=True)
    _spies(baan, 45, Y + 4, lengte=3, snel=True, tegen_klok=True, fase=2)
    baan.vadsmunt(40, Y + 2, 1)
    baan.munt(45, Y + 1)
    baan.munt(50, Y + 2)
    baan.vlag(53, Y)
    baan.guhshi(55, Y)
    # a platform under two Plof-Mika's
    baan.platform(57, G, "langs", afstand=8, breed=3)
    baan.plof_mika(61, Y + 6)
    baan.plof_mika(65, Y + 6)
    baan.munten(60, 66, Y + 2)
    baan.vlag(70, Y)
    baan.zet(72, Y, PAAL)
    baan.deur(73, Y, 4)
    # the second big gap, with a spit in the middle of it
    baan.schakelaar(75, Y + 3, 5, "tijd")
    for s in range(76, 84):
        baan.schakelblok(s, G, 5, aan=True)
    # (its hub hangs down in the pit: the skewer swings up through the bridge - mind your feet - and never reaches whoever
    # flutters over the gap; a hub at head height would stop a big player on the bridge and bump a fluttering one)
    _spies(baan, 80, 1, lengte=3)
    for s in (77, 78, 82, 83):
        baan.munt(s, Y + 3)
    baan.vlag(85, Y)
    _trap(baan, 86)
    baan.mast(92, Y, hoog=7)
    # the Peperkamer: the bush, the slit with the switch at its end, the cage of red blocks with the third vadsmunt
    B = 13
    _kamer(baan, "peperkamer", 60, 86, lambda s, y: ROOD if y in (B, 19) or s % 6 == 0 else "minecraft:red_terracotta")
    baan.deur(73, B + 1, 4)
    baan.zet(71, B + 1, STRUIK)
    baan.vul(61, B + 2, 67, B + 3, BLOK)                         # the slit: cells 62..67 of the room's floor row
    baan.schakelaar(61, B + 1, 3, "aan")
    baan.blok(75, B + 1)
    baan.guhmba(76, B + 1)
    baan.guhmba(78, B + 1)
    baan.vul(80, B + 1, 80, B + 5, gb.SCHAKELBLOK, {"kanaal": "3", "aan": "false", "open": "false"})
    baan.munten(81, 85, B + 1)
    baan.vadsmunt(83, B + 2, 2)


# =====================================================================================================================
# the duel
# =====================================================================================================================
# where things are in the arena (cells of the lane); DUEL_PLAN gives them to the boss in the level's own frame
START = 2
BRUG_VAN, BRUG_TOT, BREUK = 9, 28, 19
HENDEL_S, PIJP_S, VRAAG_S, BAAS_S = 35, 37, 6, 18
DEUR_S = (32, 33)


def duel_plan(L=40):
    """The arena for feature/guhriow3/DuelPlan.java: the level's own frame (the start block is 0, 0, 0)."""
    return {"brug_van": BRUG_VAN - START, "brug_tot": BRUG_TOT - START, "breuk": BREUK - START, "muur_links": -START, "muur_rechts": L - 1 - START,
            "hendel": [HENDEL_S - START, 0, 0], "vraag": [VRAAG_S - START, 3, 0], "blok": BRUG}


TAARTSCHILDERIJ = ["..r..", ".www.", "ppppp", "yjyjy", "bbbbb"]
TAARTKLEUR = {"r": "minecraft:red_concrete", "w": "minecraft:white_concrete", "p": "minecraft:pink_concrete", "y": "minecraft:yellow_terracotta",
              "j": "minecraft:red_terracotta", "b": "minecraft:brown_terracotta"}


def bouw_duel(baan):
    """
    The arena (40 cells, 18 rows): the start ledge (s 0..8: the start, a flag, the ?-block that is empty until round 3, the
    pipe you drop out of), the grate bridge over the sauce (s 9..28; from 19 on it drops when the lever is pulled), the far
    ledge (s 29..39: the lever, the pipe back, and the door of the tower room painted on the wall, where Prinses Perzikguh
    comes out in the end scene). The Grote Nether-Mika's spot is on the bridge.
    """
    L = baan.L
    boven = baan.H - 1
    baan.hoofdbaan(0, boven, hoogte=7)
    for s in list(range(0, BRUG_VAN)) + list(range(BRUG_TOT + 1, L)):
        for y in range(0, G + 1):
            _steen(baan, s, y)
    for s in (BRUG_VAN - 1, BRUG_TOT + 1):                        # the edge stones of the two ledges
        baan.zet(s, G, SIER)
    for s in (0, L - 1):
        for y in range(Y, boven + 1):
            baan.zet(s, y, ZWART)
    for s in range(BRUG_VAN, BRUG_TOT + 1):
        baan.zet(s, G, BRUG)
    baan.saus(BRUG_VAN, BRUG_TOT)
    baan.start(START, Y)
    baan.vlag(4, Y)
    baan.vraag(VRAAG_S, Y + 3, "vuurpeper")
    baan.zet(BAAS_S, Y, BAAS)
    baan.zet(HENDEL_S, Y, HENDEL, {"getrokken": "false"})
    baan.pijp(PIJP_S, Y + 1, 5, hoog=2)
    baan.pijp(3, 13, 5, hoog=boven - 12, richting="omlaag", ingang=False)
    # --- the painted wall ---
    for s in range(BRUG_VAN, BRUG_TOT + 1):                       # the sauce glows up from under the bridge
        baan.decor(s, 0, SAUSKLEUR)
        baan.decor(s, 1, SAUSKLEUR if s % 4 else LICHT)
        baan.decor(s, 2, ROOD if s % 4 != 2 else "minecraft:magma_block")
    for s in (BRUG_VAN, 14, BREUK, 24, BRUG_TOT):                 # the chains the bridge hangs on
        for y in range(Y, boven + 1):
            baan.decor(s, y, KETTING, {"axis": "y", "waterlogged": "false"})
    # the door of the tower room: dark, in a golden frame, a pink heart over it
    for s in range(DEUR_S[0] - 1, DEUR_S[1] + 2):
        for y in range(Y, Y + 4):
            binnen = DEUR_S[0] <= s <= DEUR_S[1] and y <= Y + 2
            baan.decor(s, y, "minecraft:black_concrete" if binnen else GOUD)
    for s, y in ((DEUR_S[0], Y + 5), (DEUR_S[1], Y + 5), (DEUR_S[0], Y + 4), (DEUR_S[1], Y + 4)):
        baan.decor(s, y, "minecraft:pink_concrete" if y == Y + 5 else "minecraft:magenta_concrete")
    # what this is all about: a painting of a cake in a golden frame
    x0, y0 = 21, Y + 4
    for r, rij in enumerate(TAARTSCHILDERIJ):
        for c, ch in enumerate(rij):
            baan.decor(x0 + c, y0 + 4 - r, TAARTKLEUR.get(ch, "minecraft:light_blue_concrete"))
    for c in range(-1, 6):
        for y in (y0 - 1, y0 + 5):
            baan.decor(x0 + c, y, GOUD)
    for y in range(y0, y0 + 5):
        baan.decor(x0 - 1, y, GOUD)
        baan.decor(x0 + 5, y, GOUD)
    # two banners of the house: red with a golden bar
    for s0 in (11, 36):
        for k in (0, 1):
            for i in range(6):
                if i == 0 and k == 1:
                    continue
                baan.decor(s0 + k, boven - 7 + i, GOUD if i == 5 else "minecraft:yellow_wool" if i == 2 else "minecraft:red_wool")


LEVELS = {"3-1": bouw_3_1, "3-2": bouw_3_2}
DUEL = bouw_duel
