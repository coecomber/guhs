"""
einde2 (the Guheinde expansion), slice deco: the decoration buildings of the two common biomes (Gatenkaasheuvels,
Korstwoestijn). The builder and the rules: einde2_deco_bouw.py.

    both                 deco_mika_wachtpost       a little watchtower of Mika-steen; the guard went to get knabbels
                         deco_vetschipwrak         a small vetschip, broken in two, nose in the crust
    Korstwoestijn        deco_karavaanresten       an oasis with what a trade caravan left: a sand sailer, a tent, a cold fire
                         deco_pai_sho_tafeltje     a little pavilion with a Pai Sho table and the white lotus tile
                         deco_appa_afdruk          two enormous three-toed footprints, white fur, and a lost poster
    Gatenkaasheuvels     deco_boemerangboom        a guh blossom tree with a boomerang stuck in it (it did not come back)

The watchtower and the wreck stand in both biomes, so they bring ground that fits either: paving, and churned-up kaaskorst
(the crust that lies under the cracked crust of the desert as well).
"""
import math

from features.einde2_deco_bouw import AIR, G, KIST_MIKA, Deco, bouwer, plaat, trap
from features.einde2_deco_bouw_a import stam

KK, KKS = "guhs:kaaskorst", "guhs:kaaskorst_stenen"
MS = "guhs:mika_steen"
GEB, ZAND = "guhs:gebarsten_korst", "guhs:kruimelzand"
JUNGLE = "stripped_jungle_log"


def woestijn(b):
    return b.kies([(GEB, 78), (ZAND, 22)])


def blad():
    return {"distance": "1", "persistent": "true", "waterlogged": "false"}


# =====================================================================================================================
# both common biomes: what the Mika's left behind
# =====================================================================================================================
@bouwer("deco_mika_wachtpost")
def mika_wachtpost(h):
    """A watchtower of Mika-steen on a paved yard: a room with the duty roster and a chest, a ladder up to a look-out with
    battlements, purple banners, and a fence with a gate that says nobody may pass. The gate is open."""
    b = Deco(h, "deco_mika_wachtpost", (15, G + 14, 17))
    c = 7

    def top(x, z):
        if math.hypot(x - c, z - 8) <= 5.2:
            return b.kies([(KKS, 70), ("guhs:gebarsten_kaaskorst_stenen", 22), (KK, 8)])
        return KK
    b.grond(b.blob(7.2, 8.2, ruw=0.07), top, onder=KK, vrij=13)
    x0, x1, z0, z1 = c - 2, c + 2, 4, 8
    dek = G + 6
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                hoek = x in (x0, x1) and z in (z0, z1)
                b.fill(x, G + 1, z, x, dek - 1, z, "guhs:mika_steen_pilaar" if hoek else MS, stam() if hoek else None)
            else:
                b.fill(x, G + 1, z, x, dek - 1, z, AIR)
            b.set(x, G, z, MS)
    b.fill(c, G + 1, z1, c, G + 2, z1, AIR)                          # the doorway
    for x in (c - 1, c + 1):
        b.set(x, G + 3, z1 + 1, "purple_wall_banner", {"facing": "south"})
    # the look-out: a deck one block wider than the tower, on brackets, with battlements
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            buiten = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1)
            b.set(x, dek, z, MS)
            if buiten:
                hoek = x in (x0 - 1, x1 + 1) and z in (z0 - 1, z1 + 1)
                if hoek or (x + z) % 2 == 0:
                    b.set(x, dek + 1, z, MS)
                else:
                    b.set(x, dek + 1, z, "guhs:mika_steen_plaat", plaat())
                if not hoek:
                    naar = "east" if x == x0 - 1 else "west" if x == x1 + 1 else "south" if z == z0 - 1 else "north"
                    b.set(x, dek - 1, z, "guhs:mika_steen_trap", trap(naar, "top"))
    for y in range(G + 1, dek + 1):                                  # the ladder, on the north wall, up through the deck
        b.set(c, y, z0 + 1, "ladder", {"facing": "south", "waterlogged": "false"})
    for (x, z) in ((x0 - 1, z0 - 1), (x1 + 1, z0 - 1), (x0 - 1, z1 + 1), (x1 + 1, z1 + 1)):
        b.lantaarn(x, dek + 2, z, ziel=True)
    b.fill(x0 - 1, dek + 2, z0 - 1, x0 - 1, dek + 2, z0 - 1, MS)     # the flagpole on the north-west corner
    b.fill(x0 - 1, dek + 3, z0 - 1, x0 - 1, dek + 5, z0 - 1, "crimson_fence")
    b.set(x0 - 1, dek + 6, z0 - 1, "purple_banner", {"rotation": "0"})
    # the room: the roster, the chest, a lantern, a stool
    b.kist(c - 1, G + 1, z0 + 1, "south", loot=KIST_MIKA)
    b.bord(c + 1, G + 2, z0 + 2, "west", ["Dienstrooster:", "ma: vadsen", "di: vadsen", "wo: ook vadsen"], id="bordje_rooster")
    b.lantaarn(c + 1, dek - 1, z1 - 1, hangend=True, ziel=True)
    b.set(c - 1, G + 1, z1 - 1, "crimson_stairs", trap("west"))
    b.prop("uitkijk", c + 1, dek + 1, z0 + 2, wie="boven op de uitkijk")
    # the yard: a fence with an open gate, the sign next to it, a barrel
    zh = 13
    for x in range(c - 4, c + 5):
        if (x, zh) in b.veld and x != c:
            b.set(x, G + 1, zh, "crimson_fence")
    b.set(c, G + 1, zh, "crimson_fence_gate", {"facing": "south", "open": "true", "in_wall": "false", "powered": "false"})
    b.bord(c + 1, G + 1, zh + 1, "south", ["Wachtpost 12.", "Mika is even", "knabbels halen.", "Niet doorlopen!"], wall=False)
    b.set(c + 3, G + 1, z1 + 1, "barrel", {"facing": "up", "open": "false"})
    b.set(c - 3, G + 1, z1 + 2, "purple_carpet")
    return b, (c - 1, G + 1, zh + 2)


@bouwer("deco_vetschipwrak")
def vetschipwrak(h):
    """A small vetschip that came down hard: the hull in two pieces with a gap between them, the bow a block deep in the
    crust, the mast snapped and lying over the side with a rag of sail, Mika's face still on the prow. In the stern a
    chest; around it barrels and lumps of set grease."""
    b = Deco(h, "deco_vetschipwrak", (24, G + 9, 15))
    cz = 7
    b.grond(b.blob(11.6, 7.2, ruw=0.06, cx=11.5), lambda x, z: b.kies([(KK, 86), ("honeycomb_block", 8), ("brown_terracotta", 6)]), onder=KK, vrij=8)

    def half(x):
        if x < 4 or x > 20:
            return None
        if x <= 8:
            return 0.6 + 2.0 * (x - 4) / 4
        if x <= 16:
            return 2.6
        return 2.6 - (x - 16) * 0.3

    def romp():
        return b.kies([("orange_terracotta", 40), ("yellow_terracotta", 30), ("brown_terracotta", 30)])
    for x in range(4, 21):
        if x in (12, 13):                                            # the gap where it broke
            continue
        yb = G if x < 12 else G + 1                                  # (the bow half lies a block deeper)
        hw = half(x)
        for z in range(cz - 3, cz + 4):
            u = abs(z - cz)
            if u > hw + 0.3:
                continue
            rand = u > hw - 0.7 or x in (4, 20)
            b.set(x, yb, z, "brown_terracotta" if rand else ("yellow_terracotta" if (x + z) % 2 else "orange_terracotta"))
            if rand:
                b.set(x, yb + 1, z, romp())
                b.set(x, yb + 2, z, "honeycomb_block" if x % 4 else "brown_terracotta")
            else:
                b.fill(x, yb + 1, z, x, yb + 4, z, AIR)
    for z in (cz - 1, cz, cz + 1):                                   # the broken ends: ragged, and open
        b.set(11, G + 1, z, AIR)
        b.set(11, G + 2, z, AIR)
        b.set(14, G + 2, z, AIR)
        b.set(14, G + 3, z, AIR)
    for (x, z, n) in ((12, cz - 2, "brown_terracotta"), (13, cz + 1, "orange_terracotta"), (12, cz + 3, "yellow_terracotta"), (13, cz - 4, "honey_block")):
        b.set(x, G + 1, z, n)
    # Mika's face on the prow (two angry black eyes, a white fang)
    for dz, boven, onder in ((-1, "black_wool", "pink_wool"), (0, "pink_wool", "white_wool"), (1, "black_wool", "pink_wool")):
        b.set(3, G + 2, cz + dz, boven)
        b.set(3, G + 1, cz + dz, onder)
    # the mast: a stump in the stern half, the rest over the side with a rag of sail
    b.fill(16, G + 2, cz, 16, G + 3, cz, "stripped_dark_oak_log", stam())
    for z in range(cz + 3, cz + 7):
        if (16, z) in b.veld:
            b.set(16, G + 1 if z > cz + 3 else G + 4, z, "stripped_dark_oak_log", stam("z"))
    b.set(16, G + 4, cz + 3, AIR)
    b.set(16, G + 3, cz + 3, "stripped_dark_oak_log", stam("z"))
    for (x, z) in ((15, cz + 5), (17, cz + 5), (17, cz + 6), (15, cz + 6)):
        if (x, z) in b.veld:
            b.set(x, G + 1, z, "brown_wool" if (x + z) % 2 else "orange_wool")
    # the hold of the stern half: the chest, barrels, grease, two lampions on the rail
    b.kist(18, G + 2, cz, "west", loot=KIST_MIKA)
    b.set(17, G + 2, cz - 1, "barrel", {"facing": "up", "open": "false"})
    b.set(15, G + 2, cz + 1, "honey_block")
    for z in (cz - 2, cz + 2):
        b.lampion(19, G + 4, z, "geel")
    b.set(9, G + 1, cz, "barrel", {"facing": "east", "open": "true"})
    b.lampion(7, G + 3, cz + 2, "geel")
    b.set(10, G + 1, cz + 5, "barrel", {"facing": "up", "open": "false"})
    b.bord(12, G + 1, cz + 5, "south", ["Vetschip Njegus", "Kapitein keek", "naar een knabbel", "i.p.v. vooruit."], wall=False)
    b.prop("ruim", 17, G + 2, cz, wie="in het ruim van het achterschip")
    return b, (12, G + 1, cz + 6)


# =====================================================================================================================
# Korstwoestijn
# =====================================================================================================================
@bouwer("deco_karavaanresten")
def karavaanresten(h):
    """A little oasis (clear water in a ring of fresh crust and korstgras, a palm) where a caravan camped: a striped
    tent with rugs, crates and a chest, a cold fire with two cushions, and the wreck of a sand sailer. They went looking
    for a library."""
    b = Deco(h, "deco_karavaanresten", (21, G + 9, 19))
    b.grond(b.blob(10.2, 9.2, ruw=0.07), lambda x, z: woestijn(b), onder=KK, vrij=8)
    px, pz = 6, 8
    for (x, z) in sorted(b.veld):                                    # the pond, in a ring of fresh crust
        d = math.hypot(x - px, z - pz)
        if d <= 2.3:
            b.set(x, G, z, "water", {"level": "0"})
            b.set(x, G - 1, z, ZAND)
        elif d <= 3.6:
            b.set(x, G, z, KK)
            if b.rng.random() < 0.45:
                b.set(x, G + 1, z, "guhs:korstgras")
    # the palm, leaning over the water
    b.fill(3, G + 1, 6, 3, G + 3, 6, "guhs:guhwaii_palm_stam", stam())
    b.fill(4, G + 3, 6, 4, G + 5, 6, "guhs:guhwaii_palm_stam", stam())
    tx, ty, tz = 4, G + 6, 6
    b.set(tx, ty, tz, "guhs:guhwaii_palm_blad", blad())
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        b.set(tx + dx, ty, tz + dz, "guhs:guhwaii_palm_blad", blad())
        b.set(tx + 2 * dx, ty, tz + 2 * dz, "guhs:guhwaii_palm_blad", blad())
        b.set(tx + 2 * dx, ty - 1, tz + 2 * dz, "guhs:guhwaii_palm_blad", blad())
    # the tent: four posts, a striped cloth, rugs, crates and the chest
    x0, x1, z0, z1 = 12, 16, 3, 6
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, G + 1, z, x, G + 2, z, "jungle_fence")
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G + 3, z, "orange_wool" if x % 2 == 0 else "white_wool")
    for (x, z, kleur) in ((13, 4, "orange"), (14, 4, "red"), (14, 5, "orange"), (13, 5, "red")):
        b.set(x, G + 1, z, f"{kleur}_carpet")
    b.kist(15, G + 1, 4, "west")
    b.set(15, G + 1, 5, "barrel", {"facing": "up", "open": "false"})
    b.set(13, G + 1, 3, "decorated_pot", {"facing": "south", "cracked": "false", "waterlogged": "false"})
    b.lantaarn(14, G + 2, 5, hangend=True)
    # the fire, long cold
    b.set(10, G + 1, 9, "campfire", {"facing": "north", "lit": "false", "signal_fire": "false", "waterlogged": "false"})
    b.set(11, G + 1, 10, "guhs:orange_kussen", {"facing": "west"})
    b.set(10, G + 1, 11, "guhs:yellow_kussen", {"facing": "north"})
    # the sand sailer: two skids, a deck between them, a mast with half a sail
    for z in (12, 15):
        b.fill(11, G + 1, z, 16, G + 1, z, JUNGLE, stam("x"))
    for x in range(12, 16):
        for z in (13, 14):
            b.set(x, G + 1, z, "jungle_slab", plaat("top"))
    b.set(15, G + 1, 14, AIR)
    b.fill(13, G + 2, 13, 13, G + 5, 13, "jungle_fence")
    for (y, z) in ((G + 5, 14), (G + 4, 14), (G + 4, 15), (G + 3, 14), (G + 5, 15)):
        b.set(13, y, z, "white_wool" if (y + z) % 2 else "yellow_wool")
    b.bord(8, G + 1, 13, "south", ["Karavaan-halte.", "Wij zijn even", "een bieb zoeken", "in het zand."], wall=False)
    b.strooi("guhs:dorre_korststruik", 0.04, op=(GEB,))
    b.prop("tent", 14, G + 1, 4, wie="onder de tent")
    return b, (10, G + 1, 16)


@bouwer("deco_pai_sho_tafeltje")
def pai_sho_tafeltje(h):
    """A little pavilion on four posts with a stepped roof: under it a small table with the white lotus tile, a cushion on
    every side, a teapot within reach and a lantern above. Someone's uncle was here. He won."""
    b = Deco(h, "deco_pai_sho_tafeltje", (13, G + 8, 13))
    c = 6
    b.grond(b.blob(6.2, 6.2), lambda x, z: "smooth_sandstone" if max(abs(x - c), abs(z - c)) <= 2 else woestijn(b), onder=KK, vrij=7)
    for dx in (-2, 2):
        for dz in (-2, 2):
            b.fill(c + dx, G + 1, c + dz, c + dx, G + 3, c + dz, JUNGLE, stam())
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            r = max(abs(dx), abs(dz))
            if r == 3:
                b.set(c + dx, G + 4, c + dz, "jungle_slab", plaat())
            elif r == 2 or r == 1:
                b.set(c + dx, G + 4, c + dz, "jungle_slab", plaat("top"))
            if r == 1:
                b.set(c + dx, G + 5, c + dz, "jungle_slab", plaat())
            elif r == 0:
                b.set(c, G + 5, c, "jungle_planks")                    # (the top of the roof: the lantern hangs under it)
    b.lantaarn(c, G + 4, c, hangend=True)
    b.set(c, G + 1, c, "smooth_sandstone_slab", plaat("top"))
    b.set(c, G + 2, c, "guhs:einde2_deco_lotustegel")
    for dx, dz, naar in ((0, -1, "south"), (0, 1, "north"), (-1, 0, "east"), (1, 0, "west")):
        b.set(c + dx, G + 1, c + dz, f"guhs:{'white' if dx else 'blue'}_kussen", {"facing": naar})
    b.set(c + 1, G + 1, c - 1, "guhs:theepotje", {"facing": "south"})
    b.bord(c + 3, G + 1, c + 3, "south", ["Pai Sho.", "Wie de witte", "lotus legt, mag", "aanschuiven."], wall=False)
    b.bord(c - 2, G + 2, c + 3, "south", ["Oom Miroh was", "hier. Hij won.", "Hij wint altijd.", "(Beetje vads.)"])
    b.strooi("guhs:dorre_korststruik", 0.04, op=(GEB,))
    return b, (c, G + 1, c + 5)


@bouwer("deco_appa_afdruk")
def appa_afdruk(h):
    """Two footprints a block deep in the cracked crust, each as big as a small room, with three toes: something very
    large with six legs stood here. White fur in the prints, and a poster on a post: lost, one bison."""
    b = Deco(h, "deco_appa_afdruk", (21, G + 6, 15))
    cx, cz = 10, 7
    b.grond(b.blob(10.2, 7.2, ruw=0.06), lambda x, z: woestijn(b), onder=KK, vrij=5)
    for px, pz in ((5, 8), (15, 7)):
        cellen = set()
        for x in range(b.W):
            for z in range(b.D):
                if ((x - px) / 2.6) ** 2 + ((z - pz) / 2.9) ** 2 <= 1.0:
                    cellen.add((x, z))
        for dx, dz in ((-3, -3), (0, -4), (3, -3)):                  # the three toes
            cellen |= {(px + dx, pz + dz), (px + dx, pz + dz - 1)}
        for (x, z) in sorted(cellen):
            if (x, z) in b.veld and x != cx:
                b.set(x, G, z, AIR)
                b.set(x, G - 1, z, KK)
        for (dx, dz) in ((0, 0), (1, 1), (-1, 1), (0, -4)):
            if (px + dx, pz + dz) in cellen:
                b.set(px + dx, G, pz + dz, "white_carpet")
    b.set(cx - 1, G + 1, cz - 3, "guhs:pluiswolblok")
    b.set(cx, G + 1, cz - 3, "white_carpet")
    b.fill(cx, G + 1, cz + 4, cx, G + 2, cz + 4, JUNGLE, stam())
    b.lantaarn(cx, G + 3, cz + 4)
    b.bord(cx, G + 2, cz + 5, "south", ["Verloren: bizon", "Lief, pluizig,", "luistert naar", "jip jip."])
    b.bord(cx - 3, G + 1, cz + 5, "south", ["Hier stond iets", "heel groots met", "zes poten. Het", "zei: grrmmf."], wall=False)
    b.strooi("guhs:dorre_korststruik", 0.04, op=(GEB,))
    b.prop("afdruk", 5, G, 8, wie="midden in de linker afdruk")
    return b, (cx + 1, G + 1, cz + 6)


# =====================================================================================================================
# Gatenkaasheuvels
# =====================================================================================================================
@bouwer("deco_boemerangboom")
def boemerangboom(h):
    """A guh blossom tree on a hill of kaaskorst, and in its crown a big blue boomerang that did not come back. A bench
    under it, a lampion on a branch, blossom on the ground."""
    b = Deco(h, "deco_boemerangboom", (13, G + 13, 13))
    c = 6
    b.grond(b.blob(6.2, 6.2), KK, onder=KK, vrij=12)
    b.fill(c, G + 1, c, c, G + 6, c, "guhs:guhbloesem_log", stam())
    kroon = (c, G + 7, c)
    for x in range(b.W):
        for z in range(b.D):
            for y in range(G + 4, b.H):
                d = math.dist((x, y * 1.15, z), (kroon[0], kroon[1] * 1.15, kroon[2]))
                if d <= 3.3 and b.get(x, y, z) in (None, AIR) and (d <= 2.6 or b.rng.random() < 0.6):
                    b.set(x, y, z, "guhs:guhbloesem_leaves", blad())
    # the boomerang: an angle of blue with white tips, stuck in the south side of the crown
    zb = c + 3
    for x in range(c - 2, c + 2):
        b.set(x, G + 6, zb, "white_concrete" if x == c - 2 else "light_blue_terracotta")
    b.set(c + 1, G + 6, zb, "blue_terracotta")
    b.set(c + 1, G + 7, zb, "light_blue_terracotta")
    b.set(c + 1, G + 8, zb, "white_concrete")
    for (x, y) in ((c - 1, G + 6), (c, G + 6), (c + 1, G + 7)):     # (it sits in the leaves: the crown reaches it there)
        b.set(x, y, zb - 1, "guhs:guhbloesem_leaves", blad())
    # a branch with a lampion, a bench, fallen blossom
    b.set(c + 1, G + 4, c, "guhs:guhbloesem_log", stam("x"))
    b.set(c + 2, G + 4, c, "spruce_fence")
    b.lampion(c + 2, G + 3, c, "roze", hangend=True)
    b.set(c - 2, G + 1, c + 1, "spruce_stairs", trap("west"))
    b.set(c - 2, G + 1, c, "spruce_stairs", trap("west"))
    for (x, z) in ((c + 1, c + 2), (c - 1, c - 2), (c + 3, c + 1), (c - 3, c + 3), (c + 2, c - 3)):
        if (x, z) in b.veld:
            b.set(x, G + 1, z, "pink_carpet")
    b.bord(c + 1, G + 1, c + 4, "south", ["Boemerang!", "Je kwam NIET", "terug, njeg.", "- Sokka-guh"], wall=False)
    b.strooi("guhs:korstgras", 0.12, op=(KK,))
    return b, (c, G + 1, c + 5)
