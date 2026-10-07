"""
einde2, slice bouw_aarde: Ba Sing Guh under the ground (called by einde2_bouw_aarde.stad after the city itself stands).

  basis           the secret base of the Dai Li-Mika's under Meer Laogai: from the tower under the boat shed a hall, the
                  corridor with the four cells and the alcove with the tank (the wink with Mewtwo-guh), the cross corridor
                  to the lamp room and the barracks, and behind bars the chamber where Appa-guh is held
  catacomben      the crystal catacombs of Old Ba Sing Guh: from the tower under the old gate in the palace garden a long
                  way down around the kristalkamer, with a den of das-molguhs, a ruin and a crystal cell on the way
  kristalkamer    the big cave under the palace where the city falls: green crystals, a pool, and the Koningsput above it
                  (the shaft of 9 x 7 up to the palace garden, with the waterfall from the pond)

The island is a shell: everything here is carved (Stad.hak) and lined (Stad.voering); asserts keep every floor above the
underside of the island.
"""
from features.guheinde_bouw import lamp, slab, stair
from features import guheinde_bouw as gb
from features.einde2_bouw_aarde_bouw import BOVEN, GOUD, GROEN, KLIP, KLIPS, KRISTAL, MUNT, ONDER, THEEKIST, _hash, bed, ruis
from features.einde2_bouw_aarde_stad import LAMPION, PUT, WATER

BASIS_Y = 28                      # feet level of the whole base
KAMER = (151, 17, 128, 16.0, 19.0, 15.0)      # the kristalkamer: middle (x, feet level, z) and its three radii
TANK = (226, 118)                 # the outer corner of the tank (5 x 5, five high)


def diep_genoeg(b, cellen, wat):
    """Every carved cell must lie at least two blocks above the underside of the island (else its lining pokes out)."""
    for (x, y, z) in cellen:
        k = b.kolom.get((x, z))
        assert k is not None and y - 2 >= k[1], f"{wat}: {(x, y, z)} lies too deep or outside the island (underside {k and k[1]})"


def gang(b, x0, z0, x1, z1, f0, f1=None, hoog=4, breed=1):
    """A corridor along the line (x0, z0) - (x1, z1) (one of the two must be equal), `breed` blocks to both sides of it,
    from feet level f0. With f1 it goes down (or up) one block per block from its start until it is at f1."""
    cellen = []
    lang = max(abs(x1 - x0), abs(z1 - z0))
    sx, sz = (x1 > x0) - (x1 < x0), (z1 > z0) - (z1 < z0)
    f = f0
    for i in range(lang + 1):
        x, z = x0 + sx * i, z0 + sz * i
        if f1 is not None and i > 0 and f != f1:
            f += 1 if f1 > f else -1
        for v in range(-breed, breed + 1):
            xx, zz = (x, z + v) if sx else (x + v, z)
            cellen += b.hak(xx, f, zz, xx, f + hoog - 1, zz)
    return cellen


# =====================================================================================================================
# the base under Meer Laogai
# =====================================================================================================================
def _dai_li_steen(c, kant):
    r = _hash(c[0] * 7 + c[1], c[2], 81)
    if kant == "vloer":
        return "minecraft:polished_deepslate"
    if kant == "plafond":
        return "minecraft:deepslate_tiles"
    return "minecraft:dark_prismarine" if c[1] == BASIS_Y + 1 and r < 0.5 else "minecraft:deepslate_bricks" if r < 0.85 else "minecraft:deepslate_tiles"


def basis(b, deur):
    """deur = the cell inside the lower door of the tower under the boat shed."""
    F = BASIS_Y
    dx, _, dz = deur
    cellen = b.hak(218, F, 106, 226, F + 4, 112)                                    # the hall
    cellen += gang(b, 222, 113, 222, 132, F)                                        # corridor A, south
    cellen += gang(b, 208, 134, 236, 134, F)                                        # corridor B, west - east
    for i in range(4):                                                              # the four cells west of A
        z0 = 114 + 4 * i
        cellen += b.hak(217, F, z0, 219, F + 2, z0 + 2)
    cellen += b.hak(224, F, 116, 230, F + 5, 124)                                   # the alcove of the tank
    cellen += b.hak(216, F, 138, 228, F + 7, 150)                                   # Appa's chamber (13 x 8 x 13)
    cellen += b.hak(219, F, 136, 225, F + 4, 137)                                   # its gate
    for x in range(198, 209):                                                       # the lamp room (round)
        for z in range(129, 140):
            if (x - 203) ** 2 + (z - 134) ** 2 <= 21.5:
                cellen += b.hak(x, F, z, x, F + 4, z)
    cellen += b.hak(232, F, 125, 238, F + 3, 131)                                   # the barracks
    cellen += b.hak(235, F, 132, 235, F + 1, 132)
    diep_genoeg(b, cellen, "basis")
    b.voering(cellen, _dai_li_steen)
    # light: green lampions under every ceiling
    for x, y, z in ([(222, F + 4, 109), (220, F + 4, 107), (224, F + 4, 111)] + [(222, F + 3, z) for z in range(115, 132, 5)] +
                    [(x, F + 3, 134) for x in range(210, 236, 5)] + [(227, F + 5, 117), (225, F + 5, 123)] +
                    [(x, F + 7, z) for x in (218, 226) for z in (140, 148)] + [(222, F + 7, 144), (203, F + 4, 134), (235, F + 3, 128)]):
        b.set(x, y, z, LAMPION, lamp(True))
    b.zone("basis", 196, F - 1, 97, 240, ONDER, 152, wie="de geheime basis van de Dai Li-Mika's onder Meer Laogai (met de trap vanuit de botenschuur)")
    # the hall
    for x in (218, 226):
        b.set(x, F, 106, THEEKIST)
        b.set(x, F + 1, 106, THEEKIST)
    b.sign(220, F + 2, 112, "north", ["Dai Li", "Je zag niets.", "Je was hier", "nooit. Njeg."])
    b.sign(224, F + 2, 112, "north", ["Thee voor de", "hersenspoeling:", "kist 4 t/m 12", ""])
    b.prop("rustpunt_3", 222, F, 108, wie="in de hal van de basis, voor het sluipstuk")
    b.prop("sluip_start", 222, F, 113, wie="het begin van de gang met de cellen")
    b.pad("sluip_route", [(222, F, 113), (222, F, 132), (222, F, 134), (222, F, 136)], wie="door de gang met de cellen naar de tralies van Appa's kamer")
    wachten = [(220, 110, -90, "in de hal"), (223, 119, 90, "bij de cellen"), (221, 127, -90, "halverwege de gang"), (213, 134, -90, "naar de lampkamer"),
               (231, 134, 90, "naar de slaapzaal"), (220, 136, -90, "bij de tralies van Appa")]
    for i, (x, z, yaw, wie) in enumerate(wachten, 1):
        b.plek(f"sluip_wacht_{i}", x, F, z, yaw=yaw, wie=f"een Dai Li-Mika op wacht {wie}", kind="DAI_LI_MIKA", los=True)
    for x, z in ((221, 116), (223, 123), (221, 130), (216, 133), (228, 135), (210, 135)):       # crates to hide behind
        b.set(x, F, z, THEEKIST)
        if (x + z) % 2:
            b.set(x, F + 1, z, THEEKIST)
    # the cells: bars and an iron door on the corridor
    for i in range(4):
        z0 = 114 + 4 * i
        for z in range(z0, z0 + 3):
            for y in range(F, F + 3):
                b.set(220, y, z, "minecraft:iron_bars")
            b.set(220, F - 1, z, "minecraft:polished_deepslate")
        gb.door(b, 220, F, z0 + 1, "west", "iron")
        b.set(217, F, z0, "minecraft:hay_block", {"axis": "x"})
        b.prop(f"cel_{i + 1}", 218, F, z0 + 1, wie="een cel (tralies en een ijzeren deur aan de gang)", los=True)
    b.sign(223, F + 2, 114, "west", ["Cellen 1-4", "Hier zit niemand", "Er is geen cel", "in Ba Sing Guh."])
    # the tank of the wink (DESIGN 5c, 3): glass, three by three inside, two deep of water, a hatch on top
    tx, tz = TANK
    for x in range(tx, tx + 5):
        for z in range(tz, tz + 5):
            rand = x in (tx, tx + 4) or z in (tz, tz + 4)
            for y in range(F, F + 5):
                if rand or y == F + 4:
                    b.set(x, y, z, "minecraft:glass")
                elif y <= F + 1:
                    b.set(x, y, z, "minecraft:water", WATER)
    b.set(tx + 2, F + 4, tz + 2, "minecraft:iron_trapdoor", {"facing": "north", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"})
    b.set(tx + 2, F - 1, tz + 2, "minecraft:sea_lantern")
    b.sign(tx - 1, F + 1, tz + 1, "west", ["Dubbele portie", "Zit vrijwillig.", "Niet storen,", "njeg."])
    b.prop("knipoog_mewtwo_tank", tx + 2, F + 1, tz + 2, wie="in de tank (3 x 3 binnen, 2 diep water, luik erboven): hier zit Mewtwo-guh", los=True)
    b.plek("knipoog_mewtwo_dai_li", tx - 1, F, tz + 3, yaw=-90.0, wie="een Dai Li-Mika naast de tank", kind="DAI_LI_MIKA")
    b.pad("knipoog_mewtwo_mew", [(224, F + 3, 117), (224, F + 3, 123)], wie="Mew-guh giechelt voorbij, door de lucht voor de tank langs")
    b.camera("knipoog_mewtwo", (221, F + 2, 126), (tx + 2, F + 2, tz + 2), zicht=["knipoog_mewtwo_tank", "knipoog_mewtwo_dai_li"],
             wie="vanuit de gang met de cellen, schuin op de tank")
    # Appa's chamber: bars over the whole gate, hay, a trough, the rings his chains hang on
    for x in range(219, 226):
        for y in range(F, F + 5):
            b.set(x, y, 137, "minecraft:iron_bars")
    for x, z in ((218, 140), (226, 140), (218, 148), (226, 148)):
        for y in range(F + 5, F + 8):
            b.set(x, y, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    for x in range(217, 221):
        b.set(x, F, 149, "minecraft:hay_block", {"axis": "x"})
    b.set(227, F, 149, "minecraft:water_cauldron", {"level": "3"})
    b.sign(227, F + 2, 135, "north", ["Luchtbizon-guh", "Niet aaien. Geen", "jip jip zeggen.", "NIET JIP JIP."])
    b.zone("appa_kamer", 216, F, 138, 228, F + 7, 150, wie="de kamer waar Appa-guh vastzit (13 x 8 x 13 vrij), achter tralies")
    b.prop("appa_gevangen", 222, F, 144, wie="hier ligt Appa-guh aan de ketting", los=True)
    b.prop("appa_poort", 222, F, 137, wie="het midden van de tralies (7 breed, 5 hoog): fase B haalt ze weg", los=True)
    # the lamp room: one lamp that goes round, a chair in the middle, the pupils' stools
    b.set(203, F, 134, "minecraft:dark_oak_stairs", stair("east"))
    b.set(206, F, 134, "minecraft:soul_lantern", lamp(False))
    for x, z in ((200, 132), (200, 134), (200, 136)):
        b.set(x, F, z, "minecraft:spruce_slab", slab())
    b.sign(203, F + 2, 130, "south", ["De lamp draait.", "Jij bent Joo Dee", "Er is geen", "knabbeltekort."])
    b.sign(199, F + 2, 134, "east", ["Les 1: lachen.", "Les 2: blijven", "lachen. Les 3:", "zie les 2."])
    b.zone("lampkamer", 198, F, 129, 208, F + 4, 139, wie="de ronde kamer met de draaiende lamp")
    # the barracks
    bed(b, 237, F, 126, "south", "green")
    bed(b, 233, F, 126, "south", "green")
    b.set(238, F, 130, "guhs:guh_tafel", {"facing": "west"})
    b.set(238, F + 1, 130, "guhs:theepotje", {"facing": "west"})
    b.chest(232, F, 130, "east", "guhs:chests/einde2_bouw_aarde_basis")
    b.sign(235, F + 2, 125, "south", ["Dai Li-kantine", "Vandaag:", "knabbelsoep.", "Morgen ook."])


# =====================================================================================================================
# the catacombs and the kristalkamer
# =====================================================================================================================
def _oude_steen(c, kant):
    r = _hash(c[0] * 5 + c[1] * 3, c[2], 82)
    if r < 0.075:
        return GROEN
    if kant == "vloer":
        return KLIPS if r < 0.5 else "minecraft:mossy_cobblestone" if r < 0.75 else "minecraft:cobbled_deepslate"
    return KLIP if r < 0.45 else "minecraft:tuff" if r < 0.7 else "minecraft:cobbled_deepslate" if r < 0.9 else "minecraft:mossy_cobblestone"


def _grot(b, cx, cy, cz, rx, ry, rz):
    """A cave: the upper half of an ellipsoid on a flat floor at feet level cy."""
    cellen = []
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            for y in range(cy, int(cy + ry) + 1):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1.0 + 0.1 * ruis(x + y, z - y, 4, 83):
                    b.hak_cel((x, y, z))
                    cellen.append((x, y, z))
    return cellen


def kristalpunt(b, x, y, z, hoog):
    """A spire of green crystal on the floor with a kristalkaas cluster on its tip."""
    for i in range(hoog):
        b.set(x, y + i, z, GROEN)
    b.set(x, y + hoog, z, KRISTAL, {"facing": "up", "waterlogged": "false"})


def catacomben(b, deur):
    """deur = the cell inside the lower door of the tower under the old gate (feet level 30)."""
    F = deur[1]
    cellen = gang(b, 161, 104, 176, 104, F)                                        # east from the tower
    cellen += gang(b, 176, 104, 176, 149, F, F - 5)                                 # south, five steps down
    cellen += gang(b, 176, 149, 128, 149, F - 5, F - 10)                            # west, five more
    cellen += gang(b, 128, 149, 128, 128, F - 10, F - 13)                           # north, three more: feet level 17
    cellen += gang(b, 128, 128, 137, 128, F - 13)                                   # east into the kristalkamer
    # the den of the das-molguhs east of the long corridor, the ruin of an old house south of it, the crystal cell
    hol_ = _grot(b, 183, F - 5, 126, 6.0, 5.0, 6.0)
    ruine = b.hak(158, F - 10, 151, 166, F - 7, 157)
    cel = _grot(b, 122, F - 13, 138, 4.0, 4.0, 4.0) + b.hak(125, F - 13, 138, 126, F - 12, 138)
    kx, kf, kz, rx, ry, rz = KAMER
    kamer = _grot(b, kx, kf, kz, rx, ry, rz)
    ax, az, bx, bz = PUT
    schacht = b.hak(ax, kf + 6, az, bx, BOVEN, bz)                                  # the Koningsput
    bassin = []                                                                     # the pool under it, two deep
    for x in range(ax - 2, bx + 3):
        for z in range(az - 2, bz + 3):
            if (x, kf, z) in b.hol:
                bassin += b.hak(x, kf - 2, z, x, kf - 1, z)
    alles = cellen + hol_ + ruine + cel + kamer + schacht + bassin
    b.laat_open(ax, BOVEN + 1, az, bx, BOVEN + 1, bz)
    diep_genoeg(b, alles, "catacomben")
    b.voering(alles, _oude_steen)
    for c in bassin:
        b.set(c[0], c[1], c[2], "minecraft:water", WATER)
    for x in range(ax - 2, bx + 3, 4):
        for z in range(az - 2, bz + 3, 4):
            if b.naam(x, kf - 2, z) == "minecraft:water":
                b.set(x, kf - 3, z, "minecraft:sea_lantern")
    # the waterfall: the pond's outlet runs over the rim and falls down the north side of the shaft into the pool
    xm = (ax + bx) // 2
    b.set(xm, BOVEN, az, "minecraft:water", {"level": "1"})
    for y in range(kf, BOVEN):
        b.set(xm, y, az, "minecraft:water", {"level": "8"})
    for y in range(kf - 2, BOVEN - 1):                                              # two ribs of rock and crystal beside the fall
        for x in (xm - 1, xm + 1):
            b.set(x, y, az - 1, GROEN if y % 3 == 0 else KLIP)
    for y in range(kf + 22, BOVEN - 2, 5):                                          # crystals up the shaft
        for x, z in ((ax - 1, az + 3), (bx + 1, az + 3), (xm - 2, bz + 1), (xm + 2, bz + 1)):
            b.set(x, y, z, GROEN)
    # the corridors: clusters on the walls, a sign now and then, lanterns the old city left behind
    for i, (x, y, z) in enumerate(cellen):
        if _hash(x, z, 84) < 0.05 and (x, y - 1, z) not in b.hol and b.naam(x, y - 1, z) is not None and b.naam(x, y, z) is None:
            # (only against a wall: the middle of a corridor stays free)
            if any(b.naam(x + dx, y, z + dz) is not None and (x + dx, y, z + dz) not in b.hol for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                b.set(x, y, z, KRISTAL, {"facing": "up", "waterlogged": "false"})
    b.sign(163, F + 1, 103, "south", ["Catacomben van", "Oud Ba Sing Guh", "Rechtdoor. Dan", "nog een eind."])
    b.sign(175, F - 3, 118, "east", ["Stil! Hier", "slapen", "das-molguhs.", "(Ze horen alles)"])
    b.sign(150, F - 8, 148, "south", ["Oude stad:", "ook hier was", "geen knabbel-", "tekort. Zegt men"])
    b.prop("rustpunt_4", 163, F, 104, wie="onderaan de trap van de oude poort, voor de catacomben")
    b.zone("catacomben", 118, kf - 1, 100, 190, F + 4, 158, wie="de kristalcatacomben van Oud Ba Sing Guh (gangen, het hol, de ruine, de cel)")
    # the den: two das-molguhs (they live here), a nest
    for x, z in ((185, 124), (186, 127), (184, 129)):
        b.set(x, F - 5, z, "minecraft:hay_block", {"axis": "y"})
    b.wezen("das_molguh_1", 183, F - 5, 125, "guhs:das_molguh", yaw=90.0, honk=6)
    b.wezen("das_molguh_2", 182, F - 5, 128, "guhs:das_molguh", yaw=120.0, honk=6)
    b.zone("das_molguh_hol", 177, F - 5, 120, 189, F, 132, wie="het hol van de das-molguhs naast de lange gang")
    # the ruin: what is left of a house of the old city
    for x in range(158, 167):
        for z in range(151, 158):
            rand = x in (158, 166) or z in (151, 157)
            hoogte = int(_hash(x, z, 85) * 3.4) if rand and not (z == 151 and 160 <= x <= 164) else 0
            for y in range(F - 10, F - 10 + hoogte):
                b.set(x, y, z, "minecraft:mossy_stone_bricks" if _hash(x, y + z, 86) < 0.5 else "minecraft:cracked_stone_bricks")
    b.set(162, F - 10, 155, MUNT)
    b.chest(164, F - 10, 156, "north", "guhs:chests/einde2_bouw_aarde_catacomben")
    b.sign(161, F - 10, 153, "north", ["Hier woonde", "iemand. Lang", "geleden. Hij", "had wel knabbels"], wall=False)
    # the crystal cell: where the Dai Li shuts people in (crystals all around)
    for x, z, h_ in ((120, 136, 2), (120, 140, 3), (123, 141, 2), (121, 138, 1), (123, 135, 3)):
        if (x, F - 13, z) in b.hol and b.naam(x, F - 14, z) is not None:
            kristalpunt(b, x, F - 13, z, h_)
    b.sign(127, F - 12, 140, "east", ["Kristalcel", "Hier zaten twee", "ruziemakers.", "Het werd knus."])
    b.prop("kristalcel", 122, F - 13, 138, wie="de kristalcel aan de gang naar de kristalkamer (hier zaten Zuka en Katara-guh)")
    # the kristalkamer
    for x in range(kx - 2, kx + 3):
        for z in range(kz - 2, kz + 3):
            b.set(x, kf - 1, z, MUNT if (x, z) == (kx, kz) else GOUD if abs(x - kx) == 2 or abs(z - kz) == 2 else KLIPS)
    for x, z, h_ in ((139, 121, 4), (142, 117, 6), (148, 115, 3), (140, 136, 5), (144, 140, 7), (150, 141, 4), (137, 124, 2), (153, 116, 5),
                     (146, 137, 2), (164, 119, 5), (166, 136, 4), (138, 132, 3)):
        if (x, kf, z) in b.hol and b.naam(x, kf - 1, z) not in (None, "minecraft:water"):
            kristalpunt(b, x, kf, z, h_)
    for x, y, z in ((kx - 6, kf + 17, kz - 3), (kx + 3, kf + 17, kz + 5), (kx - 2, kf + 18, kz + 1), (kx + 9, kf + 14, kz - 6), (kx - 9, kf + 14, kz + 6)):
        yy = y
        while (x, yy, z) in b.hol:
            yy += 1
        for i in range(1, 4):
            if (x, yy - i, z) in b.hol:
                b.set(x, yy - i, z, GROEN if i < 3 else KRISTAL, None if i < 3 else {"facing": "down", "waterlogged": "false"})
    # a half ring of crystal around the spot where Miroh is caught
    for x, z in ((144, 118), (145, 117), (147, 117), (148, 118), (148, 120)):
        if (x, kf, z) in b.hol:
            kristalpunt(b, x, kf, z, 2)
    b.sign(137, kf, 126, "east", ["Kristalkamer", "Wat hier valt,", "valt diep.", ""], wall=False)
    b.zone("kristalkamer", int(kx - rx), kf, int(kz - rz), int(kx + rx), kf + int(ry), int(kz + rz), wie="de grote kristalkamer onder het paleis")
    b.plek("azula", 141, kf, 131, yaw=-90.0, wie="Azula-Mika", kind="AZULA_MIKA")
    b.prop("bliksem_azula", 142, kf, 127, wie="van hieruit schiet Azula-Mika haar bliksem")
    b.prop("bliksem_speler", 149, kf, 127, wie="hier hangt de speler in de Guhvatar-staat als de bliksem inslaat")
    b.prop("zuka_keuze", 146, kf, 133, wie="hier kiest Zuka de verkeerde kant")
    b.prop("katara_redt", 153, kf, 126, wie="hier vangt Katara-guh je op, bij het water")
    b.prop("miroh_gevangen", 146, kf, 119, wie="hier wordt Oom Miroh in kristal gevangen (de halve ring staat er al)")
    b.prop("vlucht_gat", xm, kf + 12, (az + bz) // 2, los=True,
           wie=f"de Koningsput: een schacht van 9 x 7 (x {ax}-{bx}, z {az}-{bz}) van de kristalkamer omhoog naar de paleistuin, groot genoeg voor Appa-guh")
    b.zone("vlucht_schacht", ax, kf + 12, az, bx, BOVEN + 1, bz, wie="de schacht zelf (Appa vliegt hierdoor naar buiten)")
