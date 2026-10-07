"""
einde2, slice bouw_schelpenstad - De Ruige Kroeg: a rusty tugboat on the dry beside the road, with the bar in its hull.

Inside the hull (one hall, floor at K_VLOER): the stage at the stern, the bar along the north side, tables, the corner of
the sleeping Mika with the key, and in the bow the washroom where the bubbles are blown. A stair goes up to the deck; on
deck the barman's cabin, the wheelhouse on top of it and the funnel (it smokes). In front: the yard with the parking.
"""
from features import guheinde_bouw as gb
from features.einde2_bouw_schelpenstad_basis import *  # noqa: F401,F403

VL, DEK = K_VLOER, K_DEK          # 15: the floor block of the hall, 21: the deck (the hall is five high)
ROOD, ZWART, ROEST, WIT = "minecraft:red_terracotta", "minecraft:black_terracotta", "minecraft:brown_terracotta", "minecraft:white_terracotta"
PLANK, DONKER, BALK = "minecraft:spruce_planks", "minecraft:dark_oak_planks", "minecraft:stripped_dark_oak_log"
ROOSTER = "guhs:roosterijzer"


def halve(x):
    """Half the beam of the hull at x (-1: no hull there)."""
    if x < KX0 or x > KX1:
        return -1
    if x == KX0:
        return 6
    if x == KX0 + 1:
        return 7
    if x <= 40:
        return 8
    return int(round(8 * (1 - ((x - 40) / (KX1 - 40)) ** 1.7)))


def in_romp(x, z):
    return abs(z - KZC) <= halve(x)


def is_wand(x, z):
    return in_romp(x, z) and not all(in_romp(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def romp(b):
    rng = b.rng
    for x in range(12, 57):                                    # the yard in front of it is level and empty
        for z in range(KZC + 9, wegz(x) - 4):
            if b.naam(x, VL, z) is not None:
                b.wis(x, VL + 1, z, x, VL + 9, z)
    for x in range(KX0, KX1 + 1):
        for z in range(KZC - 8, KZC + 9):
            if not in_romp(x, z):
                continue
            b.wis(x, VL + 1, z, x, DEK + 14, z)
            b.set(x, VL, z, PLANK if (x + z) % 5 else DONKER)
            b.set(x, DEK, z, DONKER)
            b.vast.add((x, VL + 1, z))
            b.vast.add((x, DEK + 1, z))
            if is_wand(x, z):
                for y in range(VL + 1, DEK):
                    if y <= VL + 2:
                        blok = ROOD
                    else:
                        r = rng.random()
                        blok = ROEST if r < 0.10 else (ROOSTER if r < 0.24 else ZWART)
                    b.set(x, y, z, blok)
                b.set(x, DEK, z, "minecraft:white_concrete")                  # the white line of the gunwale
                b.set(x, DEK + 1, z, "minecraft:dark_oak_fence")              # the bulwark
    # portholes, and fenders (old tyres) on the outside
    for x in range(19, 41, 4):
        for z in (KZC - 8, KZC + 8):
            if not (29 <= x <= 34 and z == KZC + 8) and is_wand(x, z):
                b.set(x, VL + 3, z, "minecraft:glass")
    for x in (22, 28, 38):
        b.set(x, VL + 2, KZC - 9, "minecraft:black_wool")
    for x in (25, 39):
        b.set(x, VL + 2, KZC + 9, "minecraft:black_wool")
    # a Mika's face on the stern (the regulars painted it themselves)
    b.art(gb.MIKA_FACE, {k: "minecraft:" + v for k, v in gb.GRAFFITI.items()}, "z", KZC + 8, 17, DEK - 1)
    # the door: two swing gates in the south side, a lamp over it and the neon
    for x in (31, 32):
        b.wis(x, VL + 1, KZC + 8, x, VL + 3, KZC + 8)
        b.set(x, VL + 1, KZC + 8, "minecraft:spruce_fence_gate", {"facing": "south", "open": "false", "in_wall": "false", "powered": "false"})
        b.set(x, VL + 4, KZC + 8, ROOSTER)
        b.set(x, VL + 5, KZC + 9, NEON, {"facing": "south"})
    for x in (30, 33):
        for y in range(VL + 1, VL + 5):
            b.set(x, y, KZC + 8, BALK, {"axis": "y"})
    b.set(30, VL + 4, KZC + 9, "minecraft:lantern", gb.lamp(True))
    b.set(33, VL + 4, KZC + 9, "minecraft:lantern", gb.lamp(True))
    b.set(30, VL + 5, KZC + 9, ROOSTER)
    b.set(33, VL + 5, KZC + 9, ROOSTER)


def hal(b):
    """The hall in the hull."""
    y = VL + 1
    # hanging lanterns under the deck beams
    for x in range(19, 44, 6):
        for z in (KZC - 5, KZC, KZC + 5):
            if abs(z - KZC) < halve(x) - 1:
                b.set(x, DEK - 1, z, "minecraft:lantern", gb.lamp(True))
    # --- the stage at the stern -----------------------------------------------------------------------------------------
    for x in range(15, 21):
        for z in range(KZC - 5, KZC + 6):
            b.set(x, y, z, DONKER)
            b.vast.add((x, y + 1, z))
    for z in range(KZC - 6, KZC + 7):                          # the back curtain
        for yy in range(y + 1, DEK):
            if abs(z - KZC) < halve(15):
                b.set(15, yy, z, "minecraft:red_wool")
    for z in range(KZC - 5, KZC + 6):                          # the valance and the side drapes
        b.set(20, DEK - 1, z, "minecraft:red_wool")
    for z in (KZC - 5, KZC + 5):
        for yy in range(y + 1, DEK - 1):
            b.set(20, yy, z, "minecraft:red_wool")
    for z in (KZC - 1, KZC, KZC + 1):                          # the steps up
        b.set(21, y, z, "minecraft:dark_oak_stairs", gb.stair("west"))
    b.set(16, y + 1, KZC - 4, "minecraft:jukebox", {"has_record": "false"})
    b.set(16, y + 1, KZC + 4, "minecraft:note_block", {"instrument": "bass", "note": "3", "powered": "false"})
    b.set(17, y + 1, KZC + 4, "minecraft:barrel", {"facing": "up", "open": "false"})            # the drum
    b.set(19, y + 1, KZC, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})   # the microphone
    b.set(17, DEK - 1, KZC - 2, "guhs:lampion_roze", gb.lamp(True))
    b.set(17, DEK - 1, KZC + 2, "guhs:lampion_mint", gb.lamp(True))
    b.bord(21, y + 1, KZC + 5, "east", ["Vanavond live:", "De Brullende", "Mika's", "(niet meezingen)"])
    # --- the bar along the north side ---------------------------------------------------------------------------------
    zb = KZC - 7                                               # the row against the hull
    for x in range(26, 37):
        b.set(x, y, zb, "minecraft:barrel", {"facing": "south", "open": "false"})
        if x % 3:
            b.set(x, y + 1, zb, "minecraft:barrel", {"facing": "south", "open": "false"})
        b.set(x, y, zb + 2, BALK, {"axis": "x"})              # the counter
    b.set(25, y, zb + 2, BALK, {"axis": "x"})
    b.set(25, y, zb + 1, BALK, {"axis": "z"})
    b.set(25, y, zb, BALK, {"axis": "z"})
    for x, wat in ((27, "kaars"), (29, "taart"), (32, "kaars"), (34, "pot"), (36, "kaars")):
        if wat == "kaars":
            b.set(x, y + 1, zb + 2, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
        elif wat == "taart":
            b.set(x, y + 1, zb + 2, "guhs:guh_taart", {"bites": "2"})
        else:
            b.set(x, y + 1, zb + 2, "minecraft:flower_pot")
    for x in (26, 28, 33, 35):
        b.set(x, y, zb + 3, "guhs:tiki_kruk", {"facing": "north"})
    b.bord(31, y + 2, zb, "south", ["Stoer bier", "Stoere knabbels", "Geen bellen!"])
    # --- the tables ----------------------------------------------------------------------------------------------------
    for (tx, tz, stoelen) in ((24, KZC + 1, (((0, -1), "south"), ((0, 1), "north"))),
                              (29, KZC + 4, (((1, 0), "west"), ((0, 1), "north"))),
                              (36, KZC + 3, (((-1, 0), "east"), ((1, 0), "west"))),
                              (40, KZC - 1, (((0, -1), "south"), ((1, 0), "west")))):
        tafel(b, tx, y, tz)
        for (dx, dz), kijkt in stoelen:
            stoel(b, tx + dx, y, tz + dz, kijkt)
    b.set(24, y + 1, KZC + 1, "minecraft:candle", {"candles": "1", "lit": "true", "waterlogged": "false"})
    b.set(36, y + 1, KZC + 3, "minecraft:candle", {"candles": "1", "lit": "true", "waterlogged": "false"})
    b.set(43, y + 2, KZC + 4, "minecraft:target", {"power": "0"})                               # darts, on the bulkhead
    b.set(43, y + 1, KZC + 4, BALK, {"axis": "y"})
    b.set(43, y, KZC + 4, BALK, {"axis": "y"})
    # --- the bulkhead and the washroom in the bow ------------------------------------------------------------------------
    for z in range(KZC - 7, KZC + 8):
        if abs(z - KZC) < halve(44):
            for yy in range(y, DEK):
                b.set(44, yy, z, PLANK)
    b.wis(44, y, KZC, 44, y + 1, KZC)
    deur(b, 44, y, KZC, "east", "spruce")
    b.bord(43, y + 1, KZC - 1, "west", ["WC", "Handen wassen", "ZONDER bellen"], wood="spruce")
    for x, z in ((46, KZC - 4), (47, KZC - 3)):               # the sinks
        b.set(x, y, z, "minecraft:water_cauldron", {"level": "3"})
    b.set(45, y, KZC - 4, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(45, y + 1, KZC - 4, "guhs:bellenblaas", {"facing": "south"})                          # a pot of soap. Don't.
    b.set(47, y, KZC + 3, "minecraft:cauldron")                                                 # the throne
    b.set(48, y, KZC + 2, PLANK)
    b.set(48, y + 1, KZC + 2, PLANK)
    b.set(48, y + 2, KZC + 2, PLANK)
    b.set(46, y, KZC + 5, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(46, DEK - 1, KZC - 3, "minecraft:lantern", gb.lamp(True))
    b.set(46, DEK - 1, KZC + 3, "minecraft:lantern", gb.lamp(True))
    b.set(50, DEK - 1, KZC, "minecraft:lantern", gb.lamp(True))
    b.bord(45, y + 1, KZC + 1, "east", ["Wie hier een", "bel blaast is", "een baby'tje"], wood="spruce")
    # --- the stair up to the deck, along the north side -----------------------------------------------------------------
    zs = KZC - 7
    for i, x in enumerate((42, 41, 40, 39, 38)):
        b.wis(x, y + i, zs, x, DEK + 1, zs)
        b.set(x, y + i, zs, "minecraft:spruce_stairs", gb.stair("west"))
        for yy in range(y, y + i):
            b.set(x, yy, zs, PLANK)
    b.wis(43, y, zs, 43, y + 3, zs)


def dek(b):
    """On deck: the barman's cabin, the wheelhouse on top of it, the funnel, a mast with a lamp in the bow."""
    y = DEK + 1
    x0, x1, z0, z1 = 23, 37, KZC - 5, KZC + 5
    b.kader(x0, y, z0, x1, y + 3, z1, WIT)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, y + 4, z, DONKER)
            b.vast.add((x, y, z))
    for x in (x0, x1):
        for z in (z0, z1):
            for yy in range(y, y + 4):
                b.set(x, yy, z, BALK, {"axis": "y"})
    for x in range(x0 + 2, x1 - 1, 3):                         # windows
        for z in (z0, z1):
            b.set(x, y + 1, z, "minecraft:glass_pane")
            b.set(x, y + 2, z, "minecraft:glass_pane")
    b.wis(x0, y, KZC, x0, y + 1, KZC)
    deur(b, x0, y, KZC, "east", "spruce")
    b.wis(x1, y, KZC, x1, y + 1, KZC)
    deur(b, x1, y, KZC, "west", "spruce")
    # the barman's office
    tafel(b, 27, y, z0 + 2)
    stoel(b, 27, y, z0 + 3, "north")
    b.set(27, y + 1, z0 + 2, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    b.chest(25, y, z0 + 1, "south", "guhs:chests/einde2_bouw_schelpenstad_kroeg")
    b.set(24, y, z1 - 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(24, y + 1, z1 - 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(25, y, z1 - 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(30, y + 3, KZC, "minecraft:lantern", gb.lamp(True))
    b.set(31, y, z1 - 1, "guhs:black_zitzak", {"facing": "north"})
    b.bord(29, y + 1, z0 + 1, "south", ["Kantoor", "Klachten over", "bellen: buiten"], wood="spruce")
    # the ladder to the wheelhouse
    for yy in range(y, y + 6):
        b.wis(36, yy, z1 - 3)
        b.set(36, yy, z1 - 3, "minecraft:ladder", {"facing": "west", "waterlogged": "false"})
    hx0, hx1, hz0, hz1 = 31, 37, KZC - 3, KZC + 3
    b.kader(hx0, y + 5, hz0, hx1, y + 7, hz1, WIT)
    for x in range(hx0, hx1 + 1):
        for z in range(hz0, hz1 + 1):
            b.set(x, y + 8, z, ROOD)
            b.vast.add((x, y + 5, z))
    for x in range(hx0 + 1, hx1):
        for z in (hz0, hz1):
            b.set(x, y + 6, z, "minecraft:glass_pane")
    for z in range(hz0 + 1, hz1):
        for x in (hx0, hx1):
            b.set(x, y + 6, z, "minecraft:glass_pane")
    b.set(36, y + 5, z1 - 3, "minecraft:ladder", {"facing": "west", "waterlogged": "false"})
    b.set(34, y + 7, KZC, "minecraft:lantern", gb.lamp(True))
    b.set(32, y + 5, KZC, "minecraft:grindstone", {"face": "floor", "facing": "east"})          # the wheel (it is stuck)
    b.bord(32, y + 5, KZC - 2, "south", ["Koers: nergens", "Snelheid: nul", "Stemming: ruig"], wood="spruce")
    # the funnel: black with a red band, a fire in its top that smokes
    for yy in range(y + 5, y + 11):
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if (dx, dz) != (0, 0):
                    b.set(27 + dx, yy, KZC + dz, ROOD if yy == y + 9 else "minecraft:black_concrete")
    b.set(27, y + 8, KZC, "minecraft:hay_block", {"axis": "y"})
    b.set(27, y + 9, KZC, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "true", "waterlogged": "false"})
    for x in range(18, 47, 6):                                 # lanterns on the bulwark
        for zij in (-1, 1):
            b.set(x, y + 1, KZC + zij * halve(x), "minecraft:lantern", gb.lamp(False))
    # a mast in the bow, and a lamp at the stern
    for yy in range(y, y + 5):
        b.set(49, yy, KZC, "minecraft:dark_oak_fence")
    b.set(49, y + 5, KZC, "minecraft:lantern", gb.lamp(False))
    b.set(17, y, KZC, "minecraft:dark_oak_fence")
    b.set(17, y + 1, KZC, "minecraft:lantern", gb.lamp(False))
    b.set(42, y, KZC + 3, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(20, y, KZC - 4, "guhs:tiki_kruk", {"facing": "east"})
    b.set(20, y, KZC + 4, "minecraft:lantern", gb.lamp(False))
    b.set(43, y, KZC - 5, "minecraft:lantern", gb.lamp(False))


def erf(b):
    """The yard between the hull and the road: the door path, the parking, the road sign, an anchor."""
    y = VL + 1
    zr = KZC + 9                                               # the first row outside the hull
    for z in range(zr, wegz(31) - 3):                          # the door path
        for x in (31, 32):
            b.set(x, VL, z, STENEN)
            b.vast.add((x, y, z))
    for x in range(38, 51):                                    # three bays, white lines
        for z in range(38, 47):
            b.vast.add((x, y, z))
            b.set(x, VL, z, "minecraft:white_concrete" if x in (38, 44, 50) and z < 45 else KORSTSTENEN if (x + z) % 4 else STENEN)
    b.bord(41, y, 37, "south", ["Parkeren", "Alleen ruige", "karretjes"], wall=False, wood="spruce")
    b.set(41, VL, 37, STENEN)
    # the road sign at the door path, an anchor of grill iron beside it
    b.set(29, y, 46, MUUR)
    b.set(29, y + 1, 46, MUUR)
    b.bord(29, y + 2, 46, "south", ["De Ruige Kroeg", "Stoer bier", "Stoere Mika's", "Geen bellen!"], wall=False, wood="spruce")
    for (dx, dy) in ((0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (-1, 3), (1, 3), (-2, 1), (2, 1), (-1, 0), (1, 0), (-2, 0), (2, 0)):
        b.set(22 + dx, y + dy, 41, ROOSTER)
    b.set(22, y + 5, 41, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    b.set(22, y + 6, 41, ROOSTER)
    for (x, z, n) in ((15, 38, 2), (16, 38, 1), (15, 39, 1), (53, 37, 2), (54, 38, 1)):   # barrels
        for d in range(n):
            b.set(x, y + d, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    # the bench of the rest point, under a lantern
    b.set(25, y, 44, "guhs:guh_bank", {"facing": "south"})
    b.paal(27, y, 44, 2, MUUR)
    b.paal(36, y, 37, 2, MUUR)
    b.paal(52, y, 45, 2, MUUR)
    b.paal(18, y, 45, 2, MUUR)


def plekken(b):
    """The markers of De Ruige Kroeg (build contract 6.7)."""
    y = VL + 1
    mika = "guhs:mika"
    b.zone("ruige_kroeg", KX0, VL, KZC - 8, KX1, DEK + 12, KZC + 8, wie="De Ruige Kroeg: de sleepboot op het droge")
    b.prop("kroeg_parkeer", 41, y, 41, wie="het middelste vak: hier staat de Knabbelburgermobiel (5 x 4 x 7 vrij)")
    b.zone("kroeg_podium", 16, y + 1, KZC - 5, 20, DEK - 2, KZC + 5, wie="het podium in de achtersteven")
    b.prop("bellenblaas_plek", 45, y + 1, KZC - 4, wie="de pot zeepsop in de wc: hier worden de bellen geblazen")
    b.plek("barman", 31, y, KZC - 6, yaw=0.0, wie="de barman-Mika, achter de tap", wezen=mika)
    b.plek("uitsmijter", 34, y, KZC + 10, yaw=0.0, wie="de uitsmijter-Mika, naast de deur", wezen=mika)
    for i, (x, z, yaw) in enumerate(((30, KZC - 4, 180.0), (37, KZC - 3, 90.0), (23, KZC + 1, -90.0), (36, KZC + 4, 180.0),
                                     (36, KZC + 2, 0.0), (39, KZC - 1, -90.0)), 1):
        b.plek(f"stoere_mika_{i}", x, y, z, yaw=yaw, wie="een stoere Mika", wezen=mika)
    b.plek("slapende_mika", 28, y, KZC + 4, yaw=-90.0, wie="de slapende Mika met het sleuteltje, hoofd op tafel", wezen=mika)
    b.prop("sleutel", 29, y, KZC + 4, wie="de tafel van de slaper: hier ligt (hangt) het sleuteltje")
    b.pad("kroeg_sluip", [(31, y, KZC + 7), (32, y, KZC + 6), (32, y, KZC + 2), (30, y, KZC + 2), (28, y, KZC + 3), (28, y, KZC + 4)],
          wie="de sluipweg van de deur tussen de tafels door naar het sleuteltje")
    b.prop("rustpunt_1", 26, y, 43, wie="het rustpunt voor De Ruige Kroeg (bij het bankje)")
    for id in ("barman", "uitsmijter", "slapende_mika", "kroeg_parkeer", "rustpunt_1") + tuple(f"stoere_mika_{i}" for i in range(1, 7)):
        b.vast.add(tuple(b.plekken[id]["pos"]))


def kroeg(b):
    romp(b)
    hal(b)
    dek(b)
    erf(b)
    plekken(b)
