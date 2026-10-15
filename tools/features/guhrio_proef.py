"""
Super Guhrio (bbq2) - the engine's own practice lanes: what stands in a level slot of the castle until its level slice
(guhrio_w1 .. guhrio_w3) exports the real level. They are short, fair and complete (start, flags, three vadsmunten, a
flagpole), and between them they use every piece of the engine once, so the castle can be played and every piece can be
looked at before a single real level exists. A level slice never touches this module: its own LEVELS replace these.

Numbers used (the engine's jump): you stand on row G + 1; a ?-block goes on row G + 4 (three rows of air under it); a
step up is at most 3; a gap without help is at most 3 wide.
"""
G = 3                 # the ground's top row; you walk on row 4


def _grond(baan, gaten=()):
    """Ground from end to end (4 thick, so the picture has no hole under it) except the gaps [(s0, s1)], which end in sauce."""
    for s in range(baan.L):
        if not any(a <= s <= b for a, b in gaten):
            baan.grond(s, s, G, dik=G + 1)
    for a, b in gaten:
        baan.saus(a, b)
    # a wall at both ends, so nobody wonders what is behind them
    for s in (0, baan.L - 1):
        baan.blok(s, G + 1, hoog=9)
    baan.start(2, G + 1)


def _einde(baan, s=88):
    for i, x in enumerate(range(s - 10, s - 6)):                 # the big stairs
        baan.blok(x, G + 1, hoog=i + 1)
    baan.mast(s, G + 1, hoog=7)


def proef_1_1(baan):
    """The basics: coins, ?-blocks and bricks (a Superknabbel), Guhmba's, a flag, stairs, one gap."""
    Y = G + 1
    _grond(baan, [(46, 48)])
    baan.munten(8, 10, Y + 1)
    baan.vraag(14, Y + 3)
    for s, wat in ((18, "steen"), (19, "superknabbel"), (20, "steen"), (21, "munt"), (22, "steen")):
        baan.steen(s, Y + 3) if wat == "steen" else baan.vraag(s, Y + 3, wat)
    baan.vadsmunt(20, Y + 5, 0)                                  # on top of the bricks: climb the step and jump
    baan.blok(24, Y)
    baan.guhmba(28, Y)
    baan.blok(33, Y)
    baan.vlag(36, Y)
    for i, s in enumerate((40, 41, 42)):
        baan.blok(s, Y, hoog=i + 1)
    for s in (46, 47, 48):                                       # an arc of coins over the gap
        baan.munt(s, Y + 2 + (s == 47))
    baan.vadsmunt(47, Y + 5, 1)                                  # from the stairs, with a run
    baan.blok(52, Y)
    baan.guhmba(55, Y)
    baan.guhmba(58, Y)
    baan.blok(62, Y)
    for s in range(64, 70):
        baan.steen(s, Y + 3) if s != 66 else baan.vraag(s, Y + 3)
        baan.munt(s, Y + 4)
    baan.vlag(72, Y)
    _einde(baan)
    baan.vadsmunt(82, Y + 5, 2)                                  # one step off the top of the big stairs


def proef_1_2(baan):
    """Hidden blocks, a Schild-Mika with a row of Guhmba's to bowl over, a switch that builds a bridge, a door to a bonus room."""
    Y = G + 1
    baan.hoofdbaan(0, 12)
    _grond(baan, [(50, 55)])
    baan.munten(7, 9, Y + 1)
    baan.onzichtbaar(12, Y + 3)
    baan.onzichtbaar(13, Y + 3, "superknabbel")
    baan.munt(12, Y + 1)                                         # a coin that says "jump here"
    baan.blok(16, Y)
    baan.schild_mika(19, Y)
    for s in (23, 25, 27):
        baan.guhmba(s, Y)
    baan.blok(30, Y)
    baan.deur(34, Y, 3)                                          # the bonus room above
    baan.vlag(38, Y)
    # the switch: its blue blocks bridge the gap (they are open until you hit it)
    baan.schakelaar(44, Y + 3, 0, "aan")
    for s in range(50, 56):
        baan.schakelblok(s, G, 0, aan=True)
    baan.munten(51, 54, Y + 1)
    baan.vadsmunt(53, Y + 3, 0)
    baan.vlag(60, Y)
    for s in (64, 65, 66):
        baan.steen(s, Y + 3)
    baan.vraag(65, Y + 3)
    baan.guhmba(70, Y)
    baan.blok(74, Y)
    _einde(baan)
    baan.vadsmunt(84, Y + 2, 1)
    # the bonus room: rows 13..19, a little vault of coins with the third vadsmunt
    B = 13
    baan.bijbaan("bonus", 28, 44, B, 19)
    baan.vul(28, B, 44, B, "guhs:guhrio_blok")
    baan.vul(28, 19, 44, 19, "guhs:guhrio_blok")
    for s in (28, 44):
        baan.vul(s, B, s, 19, "guhs:guhrio_blok")
    baan.deur(34, B + 1, 3)
    baan.munten(36, 42, B + 1)
    baan.munten(37, 41, B + 3)
    baan.vadsmunt(39, B + 4, 2)
    baan.decor_vul(28, B, 44, 19, "minecraft:yellow_concrete")


def proef_2_1(baan):
    """The cellars: pipes (one with a Hapbloem, a sideways pair to a hidden cellar, a one-way exit), platforms over the sauce."""
    Y = G + 1
    baan.hoofdbaan(0, 12)
    _grond(baan, [(40, 49), (62, 65)])
    baan.munten(6, 8, Y + 1)
    baan.pijp(12, Y + 1, 1, hoog=2, bloem=True)                  # down this one (when the flower is in): the hidden cellar
    baan.guhmba(17, Y)
    baan.blok(21, Y)
    baan.pijp(26, Y + 2, 9, hoog=3, ingang=False)                # where you come back up (one-way)
    baan.vlag(30, Y)
    baan.vraag(34, Y + 3, "superknabbel")
    # a platform glides over the wide sauce
    baan.platform(40, G, "langs", afstand=7, breed=3)
    baan.munten(42, 47, Y + 2)
    baan.vadsmunt(45, Y + 4, 0)
    baan.vlag(52, Y)
    baan.schild_mika(56, Y)
    baan.blok(60, Y)
    baan.valblok(62, G, breed=2)                                 # two falling blocks over the second gap
    baan.valblok(64, G, breed=2)
    baan.vlag(68, Y)
    baan.platform(72, Y, "omhoog", afstand=4, breed=2)           # a lift to a ledge with the second vadsmunt
    baan.vul(75, Y + 4, 78, Y + 4, "guhs:guhrio_blok")
    baan.vadsmunt(77, Y + 6, 1)
    _einde(baan)
    # the hidden cellar: rows 13..19, reached by pipe 1, left by a sideways pipe (9)
    B = 13
    baan.bijbaan("kelder", 8, 30, B, 19)
    baan.vul(8, B, 30, B, "guhs:guhrio_blok")
    baan.vul(8, 19, 30, 19, "guhs:guhrio_blok")
    for s in (8, 30):
        baan.vul(s, B, s, 19, "guhs:guhrio_blok")
    baan.pijp(12, 18, 1, hoog=1, richting="omlaag")              # you drop out of the ceiling
    baan.munten(14, 24, B + 1)
    baan.vadsmunt(19, B + 3, 2)
    baan.pijp(28, B + 1, 9, hoog=2, richting="terug")            # walk into it: back up to the main lane
    baan.decor_vul(8, B, 30, 19, "minecraft:deepslate_tiles")


def proef_2_2(baan):
    """Guhshi's egg, a Vuurpeper to throw at a Hapbloem and a switch, lifts."""
    Y = G + 1
    _grond(baan, [(36, 38), (66, 68)])
    baan.munten(6, 9, Y + 1)
    baan.vraag(13, Y + 3, "vuurpeper")
    baan.pijp(18, Y + 1, 2, hoog=2, bloem=True)
    baan.pijp(24, Y + 1, 2, hoog=2)
    baan.guhmba(21, Y)
    baan.vlag(28, Y)
    baan.vadsmunt(37, Y + 3, 0)                                  # over the first gap
    baan.blok(42, Y)
    baan.schild_mika(45, Y)
    baan.guhmba(48, Y)
    baan.blok(51, Y)
    # the egg sits on a ledge behind a red wall: the switch (throw a knabbel at it, or bump it) opens the wall
    baan.schakelaar(55, Y + 3, 1, "aan")
    baan.vul(58, Y, 58, Y + 2, "guhs:guhrio_schakelblok", {"kanaal": "1", "aan": "false", "open": "false"})
    baan.guhshi_ei(61, Y)
    baan.vlag(63, Y)
    baan.vadsmunt(67, Y + 3, 1)
    baan.platform(72, Y, "omhoog", afstand=5, breed=2)
    baan.vul(75, Y + 5, 80, Y + 5, "guhs:guhrio_blok")
    baan.munten(76, 79, Y + 6)
    baan.vadsmunt(80, Y + 7, 2)
    _einde(baan, 90)


def proef_3_1(baan):
    """The burcht: turning grill spits over the sauce, Plof-Mika's, a Vuurpeper."""
    Y = G + 1
    _grond(baan, [(30, 32), (48, 50), (70, 72)])
    baan.munten(6, 8, Y + 1)
    baan.vraag(11, Y + 3, "superknabbel")
    baan.grillspies(17, Y + 3, lengte=3)
    baan.vadsmunt(17, Y + 1, 0)                                  # right under the hub: mind the skewer
    baan.plof_mika(23, Y + 5)
    baan.munt(23, Y)
    baan.vlag(27, Y)
    baan.munten(30, 32, Y + 2)
    baan.grillspies(38, Y + 2, lengte=2, tegen_klok=True, fase=1)
    baan.grillspies(43, Y + 2, lengte=2, fase=2)
    baan.vlag(46, Y)
    baan.vadsmunt(49, Y + 3, 1)
    baan.vraag(54, Y + 3, "vuurpeper")
    baan.guhmba(58, Y)
    baan.schild_mika(61, Y)
    baan.blok(64, Y)
    baan.vlag(66, Y)
    baan.plof_mika(71, Y + 6)                                    # over the last gap: wait for it, then jump
    baan.blok(75, Y)
    baan.vadsmunt(77, Y + 4, 2)
    baan.blok(77, Y, hoog=2)
    _einde(baan, 90)


def proef_3_2(baan):
    """Guhshi: a gap that only his flutter jump crosses (or the bridge of the timed switch), fast spits."""
    Y = G + 1
    _grond(baan, [(34, 42), (60, 62)])
    baan.munten(6, 8, Y + 1)
    baan.guhshi(12, Y)
    baan.guhmba(17, Y)
    baan.guhmba(20, Y)
    baan.blok(23, Y)
    baan.vlag(26, Y)
    # the wide gap: on Guhshi you flutter over it; on foot the timed switch lays a bridge for eight seconds
    baan.schakelaar(30, Y + 3, 2, "tijd")
    for s in range(34, 43):
        baan.schakelblok(s, G, 2, aan=True)
    baan.munten(35, 41, Y + 3)
    baan.vadsmunt(38, Y + 4, 0)
    baan.vlag(45, Y)
    baan.grillspies(50, Y + 3, lengte=3, snel=True)
    baan.plof_mika(55, Y + 5)
    baan.vadsmunt(61, Y + 3, 1)
    baan.vlag(64, Y)
    baan.pijp(68, Y + 1, 4, hoog=2, bloem=True)
    baan.pijp(74, Y + 1, 4, hoog=2)
    baan.schild_mika(71, Y)
    baan.vraag(77, Y + 3, "vuurpeper")
    _einde(baan, 90)
    baan.vadsmunt(84, Y + 5, 2)


def proef_duel(baan):
    """The duel arena as the engine leaves it: an empty hall over the sauce with a bridge and a flagpole at the far end."""
    Y = G + 1
    for s in range(baan.L):
        baan.grond(s, s, G, dik=G + 1, blok="minecraft:polished_blackstone_bricks" if 6 <= s <= baan.L - 8 else "guhs:guhrio_blok")
    for s in (0, baan.L - 1):
        baan.blok(s, Y, hoog=9)
    baan.start(2, Y)
    baan.vlag(5, Y)
    baan.mast(baan.L - 4, Y, hoog=7)


LEVELS = {"1-1": proef_1_1, "1-2": proef_1_2, "2-1": proef_2_1, "2-2": proef_2_2, "3-1": proef_3_1, "3-2": proef_3_2}
DUEL = proef_duel
