"""
einde2 (the Guheinde expansion), slice deco: the decoration buildings of the Kruimelwolken (lucht) and of the
Schimmelkaasvlakte (water). The builder and the rules: einde2_deco_bouw.py.

    Kruimelwolken        deco_luchtbizonstal       an abandoned stable, big enough for one very large bison
                         deco_meditatiepaal        a pole to sit on, with a wind chime
                         deco_luchtbalveld_ruine   what is left of an airball court: poles, two goals, one ball
    Schimmelkaasvlakte   deco_geestenoase_vijver   the pond where a white and a black fish circle each other
                         deco_ijsvissershutje      an ice fisher's hut on a frozen pool
                         deco_waterstam_kano       a canoe of the Water Tribe, on trestles (the water is gone)
                         deco_bevroren_kaasbol     a cracked ball of ice with a hollow kaasbol inside: someone slept here
"""
import math

from features import guheinde_bouw as gb
from features.einde2_deco_bouw import AIR, G, Deco, bouwer, luik, plaat, trap

KR, KRS = "guhs:kruimelrots", "guhs:kruimelrots_stenen"
KK = "guhs:kaaskorst"
SK, SKS = "guhs:schimmelkaas", "guhs:schimmelkaas_stenen"
WOLK = "guhs:dichte_kruimelwolk"
KOPER = "waxed_cut_copper"            # the orange of the air nomads (waxed: it stays orange)
STAM = "stripped_spruce_log"
BERK = "stripped_birch_log"


def stam(as_="y"):
    return {"axis": as_}


def zadeldak(b, x0, x1, z_nok, breed, y_goot, blok, nok=None):
    """A saddle roof whose ridge runs along x at z = z_nok: stairs `breed` rows down on both sides (the lowest row at
    y_goot), a slab on the ridge. Returns the y of the ridge."""
    for dz in range(1, breed + 1):
        y = y_goot + breed - dz
        for x in range(x0, x1 + 1):
            b.set(x, y, z_nok - dz, f"{blok}_stairs", trap("south"))
            b.set(x, y, z_nok + dz, f"{blok}_stairs", trap("north"))
    for x in range(x0, x1 + 1):
        b.set(x, y_goot + breed, z_nok, nok or f"{blok}_slab", plaat())
    return y_goot + breed


def flauwdak(b, x0, x1, z_nok, breed, y_goot, blok):
    """A shallow roof of slabs (ridge along x at z = z_nok): every two rows it climbs one block, the way the roofs of the
    air temples do. Returns the y of the ridge."""
    for dz in range(breed, -1, -1):
        stap = breed - dz
        y, t = y_goot + stap // 2, "bottom" if stap % 2 == 0 else "top"
        for z in {z_nok - dz, z_nok + dz}:
            for x in range(x0, x1 + 1):
                b.set(x, y, z, f"{blok}_slab", plaat(t))
    return y_goot + breed // 2


# =====================================================================================================================
# Kruimelwolken
# =====================================================================================================================
@bouwer("deco_luchtbizonstal")
def luchtbizonstal(h):
    """An open-fronted stable of birch and kruimelrots under an orange copper roof: hay, a trough, the saddle blankets, tufts
    of white fur everywhere, a wind chime in the doorway. The bison went round the block a hundred years ago."""
    b = Deco(h, "deco_luchtbizonstal", (21, G + 12, 19))
    b.grond(b.blob(9.6, 8.6), lambda x, z: b.kies([(KK, 80), (KR, 20)]), onder=KK, vrij=9)
    x0, x1, z0, z1, zn = 4, 16, 4, 12, 8             # the walls, the open front (z1) and the ridge
    b.fill(x0 + 1, G, z0 + 1, x1 - 1, G, z1, KRS)    # the paved floor
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z == z0
            if rand:
                b.set(x, G + 1, z, KRS)
                b.fill(x, G + 2, z, x, G + 4, z, "birch_planks")
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, G + 1, z, x, G + 4, z, STAM, stam())
    b.fill(x0 + 1, G + 4, z1, x1 - 1, G + 4, z1, STAM, stam("x"))            # the beam over the doorway
    for x in (x0, x1):                                                           # a barred window in each side wall
        b.set(x, G + 3, 7, "spruce_fence")
        b.set(x, G + 3, 9, "spruce_fence")
    nok = flauwdak(b, x0 - 1, x1 + 1, zn, 5, G + 5, KOPER)
    for x in (x0, x1):                                                           # the gables, each with a round orange pane
        for z in range(z0 + 1, z1):
            b.fill(x, G + 5, z, x, G + 5 + (4 - abs(z - zn)) // 2, z, "birch_planks")
        b.set(x, G + 5, zn, "orange_stained_glass")
    b.fill(x0, G + 5, z0, x1, G + 5, z0, "birch_planks")                         # (the back wall up to the eave)
    b.fill(x0 + 1, nok - 1, zn, x1 - 1, nok - 1, zn, STAM, stam("x"))          # the ridge beam, with a gnawed gap
    b.set(11, nok - 1, zn, AIR)
    for x in (7, 14):
        b.staaf(x, nok - 2, zn, "down")
    # inside: hay, the trough, the saddle blankets, fur
    for (x, y, z, a) in ((5, 1, 5, "y"), (6, 1, 5, "x"), (5, 2, 5, "z"), (5, 1, 6, "y")):
        b.set(x, G + y, z, "hay_block", stam(a))
    b.set(9, G + 1, 5, "water_cauldron", {"level": "3"})
    b.set(10, G + 1, 5, "composter", {"level": "6"})
    b.set(11, G + 1, 5, "composter", {"level": "3"})
    b.kist(14, G + 1, 5, "south")
    for z, kleur in ((8, "brown"), (9, "orange"), (10, "brown")):
        b.set(15, G + 1, z, "spruce_slab", plaat("top"))
        b.set(15, G + 2, z, f"{kleur}_carpet")
    b.set(7, G + 1, 9, "guhs:pluiswolblok")
    for (x, z) in ((8, 9), (7, 10), (9, 7), (12, 10), (6, 12), (13, 7)):
        b.set(x, G + 1, z, "white_carpet")
    b.set(10, G + 3, z1, "guhs:einde2_deco_windgong")
    b.bord(x0, G + 2, z1 + 1, "south", ["Luchtbizon-stal", "Bizon is even", "een blokje om.", "(al 100 jaar)"])
    b.bord(8, G + 2, z0 + 1, "south", ["Niet voeren na", "middernacht.", "Hij gaat ervan", "njeggen."])
    b.dier("lemur", 11, G + 1, 8, "guhs:vleermuis_lemurguh", yaw=0.0, honk=6)
    # outside: two lantern posts, a hitching post, a puff of cloud that drifted in
    for x in (6, 14):
        b.fill(x, G + 1, 15, x, G + 2, 15, "spruce_fence")
        b.staaf(x, G + 3, 15)
    b.fill(18, G + 1, 10, 18, G + 1, 10, "spruce_fence")
    b.set(18, G + 2, 10, "white_carpet")
    for (x, y, z) in ((2, 1, 13), (2, 1, 14), (3, 1, 14), (2, 2, 14), (17, 1, 3), (18, 1, 4)):
        if (x, z) in b.veld:
            b.set(x, G + y, z, WOLK)
    b.strooi("guhs:korstgras", 0.07, op=(KK,))
    return b, (10, G + 1, 17)


@bouwer("deco_meditatiepaal")
def meditatiepaal(h):
    """A tall birch pole on a little round plaza, a ladder up its side, on top a platform with a cushion and a wind chime
    on an arm. Sit, breathe in, do not fall off."""
    b = Deco(h, "deco_meditatiepaal", (11, G + 16, 11))
    c = 5
    b.grond(b.blob(5.2, 5.2), lambda x, z: KRS if math.hypot(x - c, z - c) <= 2.7 else b.kies([(KK, 85), (KR, 15)]), onder=KK, vrij=15)
    top = G + 10
    b.fill(c, G + 1, c, c, top, c, BERK, stam())
    for y in range(G + 1, top):
        b.set(c, y, c + 1, "ladder", {"facing": "south", "waterlogged": "false"})
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if (dx, dz) not in ((0, 0), (0, 1)):                 # (the pole itself, and the hole the ladder comes up through)
                b.set(c + dx, top, c + dz, "birch_slab", plaat("top"))
    b.set(c, top + 1, c, "guhs:orange_kussen", {"facing": "south"})
    b.fill(c - 1, top + 1, c - 1, c - 1, top + 3, c - 1, "birch_fence")   # the arm of the wind chime
    b.set(c, top + 3, c - 1, "birch_fence")
    b.set(c, top + 2, c - 1, "guhs:einde2_deco_windgong")
    b.staaf(c + 1, top + 1, c - 1)
    b.set(c + 1, G + 7, c, "orange_wall_banner", {"facing": "east"})
    b.set(c - 1, G + 6, c, "light_blue_wall_banner", {"facing": "west"})
    for dx in (-3, 3):                                               # two little lights at the rim of the plaza
        b.set(c + dx, G + 1, c, f"{KRS}_muur")
        b.staaf(c + dx, G + 2, c)
    b.bord(c + 1, G + 1, c + 2, "south", ["Meditatiepaal.", "Ga zitten, adem", "in... en val er", "niet af, njeg."], wall=False)
    b.strooi("guhs:korstgras", 0.08, op=(KK,))
    return b, (c, G + 1, c + 4)


@bouwer("deco_luchtbalveld_ruine")
def luchtbalveld_ruine(h):
    """What is left of an airball court: a cracked floor, a forest of poles of every height (some snapped), a goal board
    with a ring at each end, and the ball, still stuck in the west ring. The score sign says it all."""
    b = Deco(h, "deco_luchtbalveld_ruine", (23, G + 9, 15))
    b.grond(b.blob(11.2, 7.2, ruw=0.07), lambda x, z: b.kies([(KK, 85), (KR, 15)]), onder=KK, vrij=8)
    for x in range(3, 20):
        for z in range(3, 12):
            if (x, z) in b.veld:
                b.set(x, G, z, b.kies([(KRS, 70), (KR, 14), (KK, 16)]))
    # the poles: three rows of five, each its own height; two are snapped and lie next to their stump
    hoogtes = {5: (3, 1, 4), 8: (2, 5, 1), 11: (4, 2, 3), 14: (1, 4, 2), 17: (3, 1, 5)}
    for x, rij in hoogtes.items():
        for z, hgt in zip((5, 7, 9), rij):
            b.fill(x, G + 1, z, x, G + hgt, z, BERK, stam())
    for (x, z, dx) in ((8, 7, 1), (14, 7, -1)):
        b.fill(x, G + 2, z, x, G + 5, z, AIR)
        for i in (1, 2):
            b.set(x + dx * i, G + 1, z + 1, BERK, stam("x"))
    # the goals: a board with a copper ring; the east one has lost its top corner
    for gx, kapot in ((2, False), (20, True)):
        for z in range(5, 10):
            for y in range(G + 2, G + 7):
                ring = max(abs(z - 7), abs(y - (G + 4)))
                if kapot and y - (G + 2) + (z - 5) >= 6:
                    continue
                if ring == 0:
                    continue
                b.set(gx, y, z, KOPER if ring == 1 else "birch_planks")
        for z in (5, 9):
            b.set(gx, G + 1, z, "birch_fence")
    b.set(2, G + 4, 7, "ochre_froglight", stam("x"))                 # the ball, stuck in the ring: a goal, a century ago
    b.set(19, G + 1, 10, "birch_planks")                             # (the corner that fell off the east goal)
    b.set(18, G + 1, 10, "birch_slab", plaat())
    for (x, z) in ((3, 3), (19, 3), (3, 11), (19, 11)):              # a light on a stub of wall at every corner
        b.set(x, G + 1, z, f"{KRS}_muur")
        b.staaf(x, G + 2, z)
    b.staaf(11, G + 3, 7)                                            # (and one on a low pole in the middle)
    b.bord(11, G + 1, 12, "south", ["Luchtbal-veld", "Aang-guh 7", "Guhatso 7", "Rust: taartpauze"], wall=False)
    b.bord(3, G + 2, 5, "east", ["Bal zit vast.", "Wie hem pakt,", "moet hem ook", "terugbrengen."])
    for (x, y, z) in ((16, 1, 12), (17, 1, 12), (17, 2, 12), (6, 1, 2), (5, 1, 2), (5, 1, 9)):
        if (x, z) in b.veld and b.get(x, G + y, z) in (None, AIR):
            b.set(x, G + y, z, WOLK)
    b.strooi("guhs:korstgras", 0.06, op=(KK,))
    return b, (11, G + 1, 13)


# =====================================================================================================================
# Schimmelkaasvlakte
# =====================================================================================================================
def vis(b, x, y, z, variant):
    """A tropical fish that stays (FromBucket: it never despawns)."""
    B = b.h.ms.Byte
    b.ent(x + 0.5, y + 0.2, z + 0.5, {"id": "minecraft:tropical_fish", "Variant": int(variant), "FromBucket": B(1), "PersistenceRequired": B(1)},
          "tropical_fish")


@bouwer("deco_geestenoase_vijver")
def geestenoase_vijver(h):
    """A round pond in a ring of moss, its floor a yin and yang of white and black, and in it a white fish and a black one
    that have been circling each other since for ever. A little gate, two soul lanterns, and a warning for admirals."""
    b = Deco(h, "deco_geestenoase_vijver", (17, G + 8, 17))
    c = 8

    def d(x, z):
        return math.hypot(x - c, z - c)
    b.grond(b.blob(8.2, 8.2, ruw=0.07), lambda x, z: "moss_block" if d(x, z) <= 6.3 else SK, onder=SK, vrij=7)
    for (x, z) in sorted(b.veld):
        if 1.6 < d(x, z) <= 4.5:
            wit = x < c
            if math.hypot(x - c, z - (c - 2.25)) < 2.2:
                wit = True
            if math.hypot(x - c, z - (c + 2.25)) < 2.2:
                wit = False
            b.set(x, G - 2, z, "white_concrete" if wit else "black_concrete")
            b.set(x, G - 1, z, "water", {"level": "0"})
            b.set(x, G, z, "water", {"level": "0"})
    b.set(c, G - 2, c - 3, "black_concrete")                         # the two eyes of the sign (the white one glows)
    b.set(c, G - 2, c + 3, "sea_lantern")
    vis(b, c - 3, G - 1, c, 1 | 15 << 24)                            # (a big white fish with black markings)
    vis(b, c + 3, G - 1, c, 1 | 15 << 16)                            # (and a black one with white markings)
    # the islet in the middle
    b.staaf(c, G + 1, c)
    b.set(c - 1, G + 1, c, "flowering_azalea")
    b.set(c, G + 1, c - 1, "moss_carpet")
    for (x, z) in ((c - 3, c - 2), (c + 2, c + 3), (c + 4, c - 1)):
        b.set(x, G + 1, z, "guhs:guh_waterlelie")
    # the gate on the south bank
    zg = c + 6
    for x in (c - 2, c + 2):
        b.fill(x, G + 1, zg, x, G + 4, zg, "stripped_warped_stem", stam())
    b.fill(c - 2, G + 4, zg, c + 2, G + 4, zg, "warped_planks")
    for x in range(c - 3, c + 4):
        b.set(x, G + 5, zg, "warped_slab", plaat())
    for x in (c - 3, c + 3):
        b.lantaarn(x, G + 4, zg, hangend=True, ziel=True)
    b.bord(c, G + 4, zg + 1, "south", ["Geestenoase.", "Vis NIET pakken.", "Ja, jij daar,", "Zhao-Mika."])
    # the banks: moss, mushrooms, a second light on the far side
    b.set(c - 5, G + 1, c - 3, f"{SKS}_muur")
    b.staaf(c - 5, G + 2, c - 3)
    b.set(c + 5, G + 1, c - 3, f"{SKS}_muur")
    b.staaf(c + 5, G + 2, c - 3)
    b.strooi("moss_carpet", 0.12, op=("minecraft:moss_block",))
    b.strooi("guhs:schimmelpaddenstoel", 0.12, op=(SK,))             # (they only grow on the blue cheese itself)
    return b, (c, G + 1, c + 7)


@bouwer("deco_ijsvissershutje")
def ijsvissershutje(h):
    """A tiny spruce hut on a frozen pool, snug and closed, with the fishing hole in the middle of its floor, a barrel for
    the catch and a lantern. Outside a sled and a heap of snow."""
    b = Deco(h, "deco_ijsvissershutje", (15, G + 9, 15))
    c = 7

    def top(x, z):
        if math.hypot(x - c, z - (c + 0.5)) <= 5.2:
            return b.kies([("packed_ice", 80), ("blue_ice", 20)])
        return b.kies([(SK, 85), ("snow_block", 15)])
    b.grond(b.blob(7.2, 7.2), top, onder=SK, vrij=7)
    x0, x1, z0, z1 = 5, 9, 4, 8
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                hoek = x in (x0, x1) and z in (z0, z1)
                b.fill(x, G + 1, z, x, G + 3, z, STAM if hoek else "spruce_planks", stam() if hoek else None)
            else:
                b.fill(x, G + 1, z, x, G + 5, z, AIR)
    nok = zadeldak(b, x0 - 1, x1 + 1, 6, 3, G + 3, "spruce")
    for x in (x0, x1):
        b.fill(x, G + 4, 5, x, G + 4, 7, "spruce_planks")
        b.set(x, G + 5, 6, "spruce_planks")
        b.set(x, G + 2, 6, "light_blue_stained_glass_pane")
    gb.door(b, c, G + 1, z1, "north", wood="spruce")
    # the hole in the ice (two deep, closed in by ice and blue cheese), the barrel, a stool, the lantern
    b.set(c, G, 6, "water", {"level": "0"})
    b.set(c, G - 1, 6, "water", {"level": "0"})
    b.kist(6, G + 1, 5, "up", ton=True)
    b.set(8, G + 1, 5, "spruce_stairs", trap("east"))
    b.set(8, G + 1, 7, "guhs:light_blue_kussen", {"facing": "north"})
    b.lantaarn(c, nok - 1, 6, hangend=True)
    b.bord(8, G + 2, z1 + 1, "south", ["IJsvissen.", "Al 3 jaar geen", "beet. Wel een", "koude snoet."])
    b.set(6, G + 2, z1 + 1, "light_blue_wall_banner", {"facing": "south"})
    # outside: the sled, snow, two lights, and someone who likes fish
    for x in (10, 11, 12):
        b.set(x, G + 1, 11, "spruce_slab", plaat())
    b.set(13, G + 1, 11, "spruce_stairs", trap("east"))
    b.set(11, G + 2, 11, "guhs:pluiswolblok")
    for (x, y, z) in ((3, 1, 5), (3, 1, 6), (2, 1, 6), (3, 2, 6), (11, 1, 3)):
        if (x, z) in b.veld:
            b.set(x, G + y, z, "snow_block")
    for (x, z) in ((4, 11), (11, 6)):
        b.set(x, G + 1, z, "spruce_fence")
        b.staaf(x, G + 2, z)
    b.dier("otter", 9, G + 1, 11, "guhs:otter_pinguinguh", yaw=180.0, honk=6)
    b.strooi("guhs:schimmelpaddenstoel", 0.05, op=(SK,))
    return b, (c, G + 1, 13)


@bouwer("deco_waterstam_kano")
def waterstam_kano(h):
    """A long spruce canoe on two trestles, far from any water: a net and a small chest still in it, a cold fire next to it,
    a blue banner at the stern. For sale."""
    b = Deco(h, "deco_waterstam_kano", (17, G + 7, 11))
    cz = 5
    b.grond(b.blob(8.2, 5.2, ruw=0.08), lambda x, z: b.kies([(SK, 80), ("snow_block", 12), ("packed_ice", 8)]), onder=SK, vrij=6)
    for x in (5, 8, 11):                                             # the trestles
        b.fill(x, G + 1, cz - 1, x, G + 1, cz + 1, "spruce_log", stam("z"))
    y = G + 2
    for x in range(4, 13):
        b.set(x, y, cz, "spruce_slab", plaat())
        b.set(x, y, cz - 1, "spruce_trapdoor", luik("north", open_=True))
        b.set(x, y, cz + 1, "spruce_trapdoor", luik("south", open_=True))
    b.set(3, y, cz, "spruce_stairs", trap("west"))                   # bow and stern, turned up
    b.set(13, y, cz, "spruce_stairs", trap("east"))
    b.set(3, y + 1, cz, "spruce_trapdoor", luik("east", open_=True))
    b.kist(8, y, cz, "south")
    b.set(10, y, cz, "cobweb")
    b.set(6, y, cz, "spruce_slab", plaat("double"))
    # next to it: a cold fire, a seat, the banner, a light on a pole at the bow
    b.set(11, G + 1, 8, "campfire", {"facing": "north", "lit": "false", "signal_fire": "false", "waterlogged": "false"})
    b.set(12, G + 1, 9, "spruce_stairs", trap("south"))
    b.set(14, G + 1, cz, "light_blue_banner", {"rotation": "4"})
    b.fill(2, G + 1, cz, 2, G + 3, cz, "spruce_fence")
    b.lantaarn(2, G + 4, cz)
    b.set(9, G + 1, 2, "spruce_fence")
    b.staaf(9, G + 2, 2)
    b.set(14, G + 1, 8, "spruce_fence")
    b.staaf(14, G + 2, 8)
    b.bord(5, G + 1, 8, "south", ["Kano te koop.", "Lek? Welnee.", "Het water is", "gewoon op."], wall=False)
    b.strooi("guhs:schimmelpaddenstoel", 0.05, op=(SK,))
    return b, (8, G + 1, 9)


@bouwer("deco_bevroren_kaasbol")
def bevroren_kaasbol(h):
    """A ball of ice, cracked open on one side, and inside it the hollow rind of a kaasbol that still glows a little: a
    carpet, a cushion, a tuft of white fur. Somebody lay here for a hundred years. It is for rent."""
    b = Deco(h, "deco_bevroren_kaasbol", (15, G + 11, 15))
    c, cy, r = 7, G + 4, 4.3

    def top(x, z):
        if math.hypot(x - c, z - c) <= 5.4:
            return b.kies([("packed_ice", 70), ("blue_ice", 15), ("snow_block", 15)])
        return b.kies([(SK, 85), ("snow_block", 15)])
    b.grond(b.blob(7.2, 7.2), top, onder=SK, vrij=10)
    for x in range(b.W):
        for z in range(b.D):
            for y in range(G + 1, b.H):
                d = math.dist((x, y, z), (c, cy, c))
                if d > r:
                    continue
                # the crack: a slanting bite out of the south side
                if (z - c) + 0.55 * (y - cy) > 1.7 and abs(x - c) <= 2.5 - 0.25 * max(0, y - cy):
                    b.set(x, y, z, AIR)
                elif d > 3.25:
                    b.set(x, y, z, b.kies([("packed_ice", 75), ("blue_ice", 25)]))
                elif d > 2.35:
                    b.set(x, y, z, b.kies([("yellow_concrete", 55), ("honeycomb_block", 35), ("ochre_froglight", 10)]))
                else:
                    b.set(x, y, z, AIR)
    b.set(c, G + 1, c, "ochre_froglight", stam())                    # the floor of the hollow glows
    b.set(c - 1, G + 2, c - 1, "orange_carpet")
    b.set(c, G + 2, c - 1, "orange_carpet")
    b.set(c + 1, G + 2, c - 1, "guhs:yellow_kussen", {"facing": "south"})
    b.set(c - 1, G + 2, c, "white_carpet")
    for (x, y, z) in ((3, 1, 11), (4, 1, 12), (11, 1, 11), (11, 2, 11), (12, 1, 10), (5, 1, 13)):   # shards of the shell
        if (x, z) in b.veld:
            b.set(x, G + y, z, b.kies([("packed_ice", 70), ("blue_ice", 30)]))
    b.set(10, G + 1, 12, "spruce_fence")
    b.staaf(10, G + 2, 12)
    b.bord(9, G + 1, 12, "south", ["Te huur:", "kaasbol, 100 jr", "bewoond geweest.", "Ruikt naar bizon"], wall=False)
    b.strooi("guhs:schimmelpaddenstoel", 0.05, op=(SK,))
    return b, (c, G + 1, 13)
