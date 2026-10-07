"""
einde2 (the Guheinde expansion), slice deco: the decoration buildings of the Kristalkaasklippen (aarde) and of the
Knabbelkoorbos (vuur). The builder and the rules: einde2_deco_bouw.py.

    Kristalkaasklippen   deco_das_molguh_grot      a burrow under a mound of klipkaas, lit by crystals; someone is home
                         deco_aardrommel_ring      the ring of the Aardrommel with its stands, boulders and a dent
                         deco_koolkraam_wrak       a cabbage cart that met a piece of mail chute. MIJN KOLEN!
                         deco_geheime_tunnel       a tunnel through a ridge (through the mountains!) with a heart in the middle
    Knabbelkoorbos       deco_sintelstrand_huisje  a summer cottage with a veranda on a little red beach, and a pond
                         deco_zonnekrijger_altaar  a stepped altar with the eternal flame between two dragons
                         deco_theekraampje         a tea stall under a striped cloth; the owner went to look for his nephew
"""
import math

from features import guheinde_bouw as gb
from features.einde2_deco_bouw import AIR, G, KIST_THEE, Deco, bouwer, luik, plaat, trap
from features.einde2_deco_bouw_a import stam, zadeldak

KLK, KLS = "guhs:klipkaas", "guhs:klipkaas_stenen"
ERTS = "guhs:kristalkaas_erts"
KR = "guhs:kruimelrots"
KOOR, KK = "guhs:koorkorst", "guhs:kaaskorst"
KOOL = "guhs:einde2_deco_kool"
ROOD = "mangrove"                       # the dark red wood of the Vuurnatie
ROODSTAM = "stripped_mangrove_log"
BAMBOE = "stripped_bamboo_block"
KRISTAL_GLOEI = ("guhs:kristalkaas_kristal", {"facing": "up", "waterlogged": "false"}, (KLK,))


def kool(b, x, y, z, geplet=False):
    b.set(x, y, z, KOOL, {"geplet": "true" if geplet else "false"})


def rots(b):
    # (ore as sparse as in the klipkaas of the biome itself: a mound is no little mine, the loot stays modest)
    return b.kies([(KLK, 91), (ERTS, 2), (KR, 7)])


# =====================================================================================================================
# Kristalkaasklippen
# =====================================================================================================================
@bouwer("deco_das_molguh_grot")
def das_molguh_grot(h):
    """A round mound of klipkaas with a burrow inside: an arched way in on the south side, a nest of hay and brown
    carpet at the back, kristalkaas crystals on the floor and the ceiling, a small chest, and the das-molguh himself."""
    b = Deco(h, "deco_das_molguh_grot", (19, G + 9, 19))
    c = 9
    b.gloei, b.gloei_hoog = KRISTAL_GLOEI, (KLK, ERTS, KR)
    b.grond(b.blob(9.2, 9.2), lambda x, z: b.kies([(KLK, 92), (KR, 8)]), onder=KLK, vrij=8)
    buiten, binnen = (6.4, 5.7, 6.4), (4.4, 3.5, 4.4)

    def e(x, y, z, r):
        return ((x - c) / r[0]) ** 2 + ((y - G) / r[1]) ** 2 + ((z - c) / r[2]) ** 2
    for x in range(b.W):
        for z in range(b.D):
            for y in range(G + 1, b.H):
                if e(x, y, z, buiten) <= 1.0:
                    b.set(x, y, z, AIR if e(x, y, z, binnen) <= 1.0 else rots(b))
    for z in range(c + 3, c + 8):                                    # the way in: three wide, an arch
        for x in (c - 1, c, c + 1):
            for y in (G + 1, G + 2, G + 3):
                if (y < G + 3 or x == c) and b.get(x, y, z) not in (None, AIR):
                    b.set(x, y, z, AIR)
    zf = c + 6                                                       # the arch of bricks around the mouth
    for x in (c - 2, c + 2):
        for y in (G + 1, G + 2):
            if b.get(x, y, zf) not in (None, AIR):
                b.set(x, y, zf, KLS)
    for (x, y) in ((c - 1, G + 3), (c + 1, G + 3), (c, G + 4)):
        if b.get(x, y, zf) not in (None, AIR):
            b.set(x, y, zf, KLS)
    # inside: crystals, the nest, the chest
    for (x, z) in ((c - 3, c), (c + 3, c - 1), (c + 1, c + 2)):
        b.kristal(x, G + 1, z)
    for (x, z) in ((c - 1, c - 1), (c + 2, c + 1), (c - 2, c + 2)):
        y = G + 1
        while b.get(x, y + 1, z) == AIR:                               # (up to the ceiling of the burrow)
            y += 1
        b.kristal(x, y, z, "down")
    b.set(c - 1, G + 1, c - 3, "hay_block", stam("x"))
    for (x, z) in ((c, c - 3), (c + 1, c - 3), (c, c - 2)):
        b.set(x, G + 1, z, "brown_carpet")
    b.kist(c + 3, G + 1, c + 1, "west")
    b.bord(c - 3, G + 1, c + 1, "east", ["Hij ziet je niet", "maar hij voelt", "je knabbels wel.", ""], wall=False, id="bordje_binnen")
    b.dier("das_molguh", c, G + 1, c, "guhs:das_molguh", yaw=0.0, honk=4)
    b.prop("hol", c, G + 1, c + 1, wie="in het hol")
    # outside: heaps of dug-out crumb, and the sign
    for (x, y, z) in ((c - 3, 1, c + 8), (c - 4, 1, c + 8), (c - 3, 2, c + 8), (c + 4, 1, c + 7), (c + 4, 1, c + 8)):
        if (x, z) in b.veld and b.get(x, G + y, z) in (None, AIR):
            b.set(x, G + y, z, KR)
    b.bord(c + 2, G + 1, c + 8, "south", ["Das-molguh-hol.", "Blind maar lief.", "Zing iets, dan", "graaft hij mee."], wall=False)
    return b, (c, G + 1, c + 8)


@bouwer("deco_aardrommel_ring")
def aardrommel_ring(h):
    """A square ring of klipkaas bricks, one block high, with a dent in it and boulders and stone discs lying about;
    stands on two sides, a lantern on a post at every corner, a green banner. The champion is a small blind guh."""
    b = Deco(h, "deco_aardrommel_ring", (21, G + 8, 21))
    c = 10
    b.gloei = KRISTAL_GLOEI
    b.grond(b.blob(10.2, 10.2, ruw=0.06), lambda x, z: b.kies([(KLK, 92), (KR, 8)]), onder=KLK, vrij=7)
    for x in range(c - 4, c + 5):
        for z in range(c - 4, c + 5):
            rand = max(abs(x - c), abs(z - c)) == 4
            b.set(x, G + 1, z, KLS if rand else b.kies([(KLS, 78), (KLK, 16), ("guhs:kruimelrots_stenen", 6)]))
    for (x, z) in ((c - 1, c + 1), (c, c + 1), (c - 1, c + 2)):      # the dent of a hard landing
        b.set(x, G + 1, z, AIR)
    for x in (c - 1, c, c + 1):                                      # the steps up, on the south side
        b.set(x, G + 1, c + 5, f"{KLS}_trap", trap("north"))
    for (x, z) in ((c - 5, c - 5), (c + 5, c - 5), (c - 5, c + 5), (c + 5, c + 5)):
        b.fill(x, G + 1, z, x, G + 2, z, f"{KLS}_muur")
        b.lantaarn(x, G + 3, z)
    # boulders and discs
    b.set(c - 2, G + 2, c - 2, KR)
    b.set(c + 2, G + 2, c + 1, KR)
    b.fill(c + 1, G + 2, c - 3, c + 1, G + 3, c - 3, KR)
    for (x, z, n) in ((c - 3, c + 2, 2), (c + 3, c - 1, 1)):
        b.set(x, G + 2, z, f"{KLS}_plaat", plaat("double" if n == 2 else "bottom"))
    b.set(c + 3, G + 2, c + 3, KLK)
    # the stands: two rows on the north side, two on the east side
    for x in range(c - 4, c + 5):
        b.set(x, G + 1, c - 6, f"{KLS}_trap", trap("north"))
        b.set(x, G + 1, c - 7, KLS)
        b.set(x, G + 2, c - 7, f"{KLS}_trap", trap("north"))
    for z in range(c - 4, c + 5):
        b.set(c + 6, G + 1, z, f"{KLS}_trap", trap("east"))
        b.set(c + 7, G + 1, z, KLS)
        b.set(c + 7, G + 2, z, f"{KLS}_trap", trap("east"))
    b.kist(c - 5, G + 1, c - 7, "south")
    b.fill(c - 7, G + 1, c, c - 7, G + 3, c, "spruce_fence")
    b.set(c - 7, G + 4, c, "green_banner", {"rotation": "12"})
    b.bord(c - 2, G + 1, c + 6, "south", ["Aardrommel VI", "Kampioen:", "De Blinde", "Bandiet (guh)"], wall=False)
    b.bord(c + 2, G + 2, c + 2, "south", ["De Kei-guh komt", "zo terug. Hij", "ligt nog even,", "njeg."])
    b.prop("ring", c, G + 2, c, wie="midden op de ring")
    return b, (c, G + 1, c + 8)


@bouwer("deco_koolkraam_wrak")
def koolkraam_wrak(h):
    """A cabbage cart with half its awning left, a wheel off, and a broken piece of mail chute right through it. Cabbages
    everywhere, a few of them flat. The sign says what the merchant said."""
    b = Deco(h, "deco_koolkraam_wrak", (17, G + 7, 15))
    b.gloei = KRISTAL_GLOEI
    b.grond(b.blob(8.2, 7.2, ruw=0.08), lambda x, z: b.kies([(KLK, 90), (KR, 10)]), onder=KLK, vrij=6)
    # the cart: a floor on two axles, two posts with what is left of the cloth, one wheel still on
    for x in range(5, 9):
        for z in range(6, 9):
            b.set(x, G + 1, z, "spruce_slab", plaat("top"))
    for x in (5, 8):
        b.fill(x, G + 2, 6, x, G + 3, 6, "spruce_fence")
    b.fill(6, G + 3, 6, 7, G + 3, 6, "spruce_fence")
    for x, kleur in ((5, "green"), (6, "white"), (7, "green")):
        b.set(x, G + 4, 6, f"{kleur}_carpet")
    b.set(5, G + 1, 5, "spruce_trapdoor", luik("south", open_=True))        # the wheel that stayed on
    b.set(10, G + 1, 10, "spruce_trapdoor", luik("north"))                  # and the one that rolled off
    b.set(9, G + 1, 7, "barrel", {"facing": "east", "open": "true"})
    b.lantaarn(7, G + 2, 6, hangend=True)
    # the piece of mail chute that came down on it: ice between two low walls, one end still on its pier
    for x in range(9, 14):
        y = G + 1 if x < 13 else G + 2
        b.set(x, y, 4, "packed_ice")
        if x == 13:
            b.set(x, G + 1, 4, KLS)
        for z in (3, 5):
            b.set(x, y, z, f"{KLS}_plaat", plaat())
            if x == 13:
                b.set(x, G + 1, z, KLS)
    # cabbages: on the cart, all around it, some of them flat
    kool(b, 6, G + 2, 7)
    kool(b, 7, G + 2, 8)
    kool(b, 6, G + 2, 8)
    for (x, z, plat) in ((4, 9, False), (9, 9, True), (10, 7, False), (6, 11, False), (12, 9, False), (3, 6, True), (11, 6, True),
                         (7, 3, False), (13, 7, False), (8, 12, True)):
        if (x, z) in b.veld and b.get(x, G + 1, z) in (None, AIR):
            kool(b, x, G + 1, z, plat)
    b.bord(8, G + 1, 10, "south", ["MIJN KOLEN!", "Verse kool,", "nu extra", "geplet, njeg."], wall=False)
    return b, (8, G + 1, 13)


@bouwer("deco_geheime_tunnel")
def geheime_tunnel(h):
    """A ridge of klipkaas with a tunnel right through it, a brick arch at both ends and crystals on the ceiling that
    glow in the dark. In the middle it widens: a pink heart on the wall, and a note block for whoever knows the song."""
    b = Deco(h, "deco_geheime_tunnel", (13, G + 9, 23))
    cx, cz = 6, 11
    b.gloei, b.gloei_hoog = KRISTAL_GLOEI, (KLK, ERTS, KR)
    b.grond(b.blob(6.2, 11.2, ruw=0.06), lambda x, z: b.kies([(KLK, 92), (KR, 8)]), onder=KLK, vrij=8)
    r = (5.7, 6.3, 8.7)
    for x in range(b.W):
        for z in range(b.D):
            for y in range(G + 1, b.H):
                if ((x - cx) / r[0]) ** 2 + ((y - G) / r[1]) ** 2 + ((z - cz) / r[2]) ** 2 <= 1.0:
                    b.set(x, y, z, rots(b))
    for z in range(b.D):                                             # the tunnel: three wide, an arch
        for x in (cx - 1, cx, cx + 1):
            for y in (G + 1, G + 2, G + 3):
                if (y < G + 3 or x == cx) and b.get(x, y, z) not in (None, AIR):
                    b.set(x, y, z, AIR)
    for z in range(cz - 2, cz + 3):                                  # the room in the middle
        for x in range(cx - 2, cx + 3):
            for y in range(G + 1, G + 5):
                if y < G + 4 or abs(x - cx) <= 1:
                    b.set(x, y, z, AIR)
    for zf in (cz - 7, cz + 7):                                      # an arch of bricks at both mouths
        for x in (cx - 2, cx + 2):
            for y in (G + 1, G + 2, G + 3):
                b.set(x, y, zf, KLS)
        for x in (cx - 1, cx + 1):
            b.set(x, G + 3, zf, f"{KLS}_trap", trap("east" if x < cx else "west", "top"))
        for x in range(cx - 2, cx + 3):
            b.set(x, G + 4, zf, "guhs:kristalkaas_blok" if x == cx else KLS)
    for z in (cz - 6, cz - 4, cz + 4, cz + 6):                       # crystals on the ceiling of the tunnel
        b.kristal(cx, G + 3, z, "down")
    for (x, z) in ((cx - 2, cz - 2), (cx + 2, cz + 2), (cx + 2, cz - 2)):
        b.kristal(x, G + 1, z)
    b.art([".X.X.", "XXXXX", ".XXX.", "..X.."], {"X": "pink_concrete"}, "x", cx - 3, cz - 2, G + 4)   # the heart on the west wall
    b.set(cx - 2, G + 1, cz + 2, "note_block")
    b.bord(cx + 2, G + 2, cz, "west", ["Liefde is het", "helderst in het", "donker. (Kusje", "= licht aan.)"], id="bordje_binnen")
    b.prop("kamer", cx, G + 1, cz, wie="de kamer in het midden")
    b.bord(cx + 3, G + 1, cz + 9, "south", ["Geheime tunnel!", "Geheime tunnel!", "Door de bergen!", "Geheime tunnel!"], wall=False)
    return b, (cx, G + 1, cz + 10)


# =====================================================================================================================
# Knabbelkoorbos
# =====================================================================================================================
@bouwer("deco_sintelstrand_huisje")
def sintelstrand_huisje(h):
    """A small summer cottage of the Vuurnatie: cream walls between dark red posts under a red saddle roof, a veranda with
    lanterns, inside a laid tea table, cushions and a chest. In front of it a strip of red beach with a parasol and a
    pond where two schildpad-eendguhs paddle."""
    b = Deco(h, "deco_sintelstrand_huisje", (19, G + 11, 21))
    cx = 9

    def top(x, z):
        return "red_sand" if z >= 13 else KOOR
    b.grond(b.blob(9.2, 10.2, ruw=0.07), top, onder=KK, vrij=10)
    x0, x1, z0, z1, zn = 5, 13, 4, 10, 7
    vloer = G + 1
    b.fill(x0, vloer, z0, x1, vloer, z1 + 2, f"{ROOD}_planks")       # the floor, and the deck of the veranda
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                hoek = x in (x0, x1) and z in (z0, z1)
                b.fill(x, vloer + 1, z, x, vloer + 3, z, ROODSTAM if hoek else "white_terracotta", stam() if hoek else None)
            else:
                b.fill(x, vloer + 1, z, x, vloer + 6, z, AIR)
    nok = zadeldak(b, x0 - 1, x1 + 1, zn, 4, vloer + 3, ROOD)
    for x in (x0, x1):
        for dz in range(0, 3):
            for z in {zn - dz, zn + dz}:
                b.fill(x, vloer + 4, z, x, nok - 1 - dz, z, "white_terracotta")
    gb.door(b, cx, vloer + 1, z1, "north", wood=ROOD)
    for x in (cx - 2, cx + 2):
        b.set(x, vloer + 2, z1, "glass_pane")
    for x in (x0, x1):
        for z in (zn - 1, zn + 1):
            b.set(x, vloer + 2, z, "glass_pane")
    # the veranda: a rail, two posts with a lantern, steps down to the beach
    for x in range(x0, x1 + 1):
        if x not in (cx - 1, cx, cx + 1):
            b.set(x, vloer + 1, z1 + 2, f"{ROOD}_fence")
    for x in (x0, x1):
        b.set(x, vloer + 2, z1 + 2, f"{ROOD}_fence")
        b.lantaarn(x, vloer + 3, z1 + 2)
    for x in (cx - 1, cx, cx + 1):
        b.set(x, vloer, z1 + 3, f"{ROOD}_stairs", trap("north"))
    # inside
    b.set(cx - 2, vloer + 1, z0 + 2, "guhs:theetafel", {"gedekt": "true"})
    b.set(cx - 3, vloer + 1, z0 + 2, "guhs:red_kussen", {"facing": "east"})
    b.set(cx - 1, vloer + 1, z0 + 2, "guhs:red_kussen", {"facing": "west"})
    b.kist(x1 - 1, vloer + 1, z0 + 1, "south")
    for z in (z1 - 2, z1 - 1):
        b.set(x0 + 1, vloer + 1, z, "red_carpet")
    b.set(x0 + 1, vloer + 1, z1 - 3, "guhs:white_kussen", {"facing": "south"})
    b.set(cx, vloer + 3, z0 + 1, "red_wall_banner", {"facing": "south"})
    b.lantaarn(cx, nok - 1, zn, hangend=True)
    b.set(x1 - 1, vloer + 1, z1 - 1, "flower_pot")
    b.bord(cx + 1, vloer + 2, z1 + 1, "south", ["Zomerhuisje", "Sintelstrand.", "Vuurheer is er", "nooit. Vads maar"])
    b.prop("kamer", cx, vloer + 1, zn, wie="in het huisje")
    # the beach: a parasol with a chair, the pond, the poster of tonight's play
    px, pz = 4, 16
    b.fill(px, G + 1, pz, px, G + 3, pz, f"{ROOD}_fence")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            b.set(px + dx, G + 4, pz + dz, "red_wool" if (dx + dz) % 2 == 0 else "white_wool")
    b.set(px + 1, G + 1, pz + 1, f"{ROOD}_stairs", trap("west"))
    b.set(px + 2, G + 1, pz + 1, f"{ROOD}_slab", plaat())
    for x in range(10, 16):
        for z in range(13, 19):
            if (x, z) in b.veld and math.hypot(x - 12.5, z - 15.5) <= 2.0:
                b.set(x, G, z, "water", {"level": "0"})
    b.dier("eend_1", 12, G + 1, 15, "guhs:schildpad_eendguh", yaw=90.0, honk=5)
    b.dier("eend_2", 13, G + 1, 16, "guhs:schildpad_eendguh", yaw=-90.0, honk=5)
    b.bord(8, G + 1, 17, "south", ["Toneel vanavond:", "De jongen in de", "kaasbol. Slecht,", "maar grappig."], wall=False)
    return b, (cx, G + 1, 19)


@bouwer("deco_zonnekrijger_altaar")
def zonnekrijger_altaar(h):
    """A stepped altar of sandstone, three steps high: stairs up the south side, on top the eternal flame on its pedestal
    between a red and a blue dragon, a lantern at every corner, and on the north side the sun stone nobody should touch."""
    b = Deco(h, "deco_zonnekrijger_altaar", (19, G + 13, 19))
    c = 9
    b.grond(b.blob(9.2, 9.2, ruw=0.06), KOOR, onder=KK, vrij=12)

    def steen():
        return b.kies([("cut_sandstone", 55), ("sandstone", 30), ("smooth_sandstone", 15)])
    treden = ((6, G + 1), (4, G + 3), (2, G + 5))
    for half, y0 in treden:
        for x in range(c - half, c + half + 1):
            for z in range(c - half, c + half + 1):
                rand = max(abs(x - c), abs(z - c)) == half
                hoek = abs(x - c) == half and abs(z - c) == half
                b.set(x, y0, z, "cut_sandstone" if rand else steen())
                b.set(x, y0 + 1, z, "chiseled_sandstone" if hoek else ("cut_sandstone" if rand else steen()))
    for x in (c - 1, c, c + 1):                                      # the stairs: six steps up the south side
        for i in range(6):
            z = c + 7 - i
            b.set(x, G + 1 + i, z, "sandstone_stairs", trap("north"))
            b.fill(x, G + 2 + i, z, x, G + 9, z, AIR)
            if i:
                b.fill(x, G + 1, z, x, G + i, z, "sandstone")
    for (x, z) in ((c - 6, c - 6), (c + 6, c - 6), (c - 6, c + 6), (c + 6, c + 6)):
        b.set(x, G + 3, z, "sandstone_wall")
        b.lantaarn(x, G + 4, z)
    for (x, z) in ((c - 4, c - 4), (c + 4, c - 4), (c - 4, c + 4), (c + 4, c + 4)):
        b.staaf(x, G + 5, z)
    # on top: the flame, and the two dragons that guard it (a red one and a blue one, each with a snout and a horn)
    top = G + 7
    b.set(c, top, c, "chiseled_sandstone")
    b.set(c, top + 1, c, "guhs:einde2_deco_eeuwige_vlam")
    for x, lijf, kop, snuit in ((c - 2, "red_terracotta", "red_concrete", "red_nether_brick_stairs"),
                                (c + 2, "blue_terracotta", "blue_concrete", "warped_stairs")):
        b.fill(x, top, c - 1, x, top + 1, c - 1, lijf)
        b.set(x, top + 2, c - 1, kop)
        b.set(x, top + 2, c, snuit, trap("north"))
        b.set(x, top, c, lijf)
        b.set(x, top, c - 2, lijf)
        b.staaf(x, top + 3, c - 1)
    b.bord(c, top, c + 1, "south", ["Eeuwige vlam.", "Brandt al sinds", "de 1e draakguh.", "Niet blazen!"])
    # the sun stone, on the north side of the lowest step
    b.set(c, G + 3, c - 5, "chiseled_sandstone")
    b.set(c, G + 4, c - 5, "ochre_froglight", stam())
    b.bord(c, G + 3, c - 6, "north", ["Zonnesteen.", "Niet pakken.", "(De vorige plakt", "er nog aan.)"])
    b.prop("bovenop", c, top, c + 2, wie="boven op het altaar")
    return b, (c, G + 1, c + 8)


@bouwer("deco_theekraampje")
def theekraampje(h):
    """A tea stall of bamboo under a green and white cloth: the pot still on the counter, a barrel of tea and a stove
    behind it, a laid table with two cushions in front, lampions and a wind chime. Pour your own."""
    b = Deco(h, "deco_theekraampje", (15, G + 7, 13))
    b.grond(b.blob(7.2, 6.2, ruw=0.08), KOOR, onder=KK, vrij=6)
    x0, x1, z0, z1 = 4, 10, 3, 6
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, G + 1, z, x, G + 3, z, BAMBOE, stam())
    for x in range(x0 - 1, x1 + 2):                                  # the cloth
        for z in range(z0 - 1, z1 + 2):
            b.set(x, G + 4, z, "green_wool" if x % 2 == 0 else "white_wool")
    for x in range(x0 + 1, x1):                                      # the counter
        b.set(x, G + 1, z1, "bamboo_mosaic")
    b.set(x0 + 2, G + 2, z1, "guhs:theepotje", {"facing": "south"})
    b.set(x1 - 2, G + 2, z1, "flower_pot")
    b.kist(x0 + 1, G + 1, z0, "south", loot=KIST_THEE, ton=True)
    b.set(x0 + 3, G + 1, z0, "smoker", {"facing": "south", "lit": "false"})
    b.set(x1 - 1, G + 1, z0, "decorated_pot", {"facing": "south", "cracked": "false", "waterlogged": "false"})
    b.set(x0 + 2, G + 1, z0, "bamboo_mosaic_slab", plaat("top"))
    b.set(x0 + 4, G + 1, z0, "bamboo_mosaic_slab", plaat("top"))
    for x in (x0 - 1, x1 + 1):
        b.lampion(x, G + 3, z1 + 1, "geel", hangend=True)
    b.set(7, G + 3, z1 + 1, "guhs:einde2_deco_windgong")
    b.lantaarn(7, G + 3, z0 + 1, hangend=True)
    b.bord(x1 - 1, G + 1, z1 + 1, "south", ["Oom Miroh's thee", "Ben even weg:", "neef zoeken.", "Schenk zelf in."])
    # the guests' table
    b.set(7, G + 1, 9, "guhs:theetafel", {"gedekt": "true"})
    b.set(6, G + 1, 9, "guhs:green_kussen", {"facing": "east"})
    b.set(8, G + 1, 9, "guhs:green_kussen", {"facing": "west"})
    b.bord(11, G + 1, 9, "south", ["Thee delen met", "een vreemde is", "het fijnste dat", "er is, njeg."], wall=False)
    b.prop("toonbank", 7, G + 1, z1 - 1, wie="achter de toonbank")
    return b, (7, G + 1, 11)
