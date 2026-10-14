"""
1.4.2 Het Knabbelpark, plekjes A - the templates of five random plekjes (the PLEKJES API of park_bouw.py: rigid, foundation
y = 0, ground y = G = 1, walk level y = 2, ONE entrance jigsaw guhs:park_plek in the middle of the south edge; ponds go down
into the foundation). The spots the Java needs come from feature/park/plek/PlekAPlekken.java (park_plek_a.plekken()).

  eendenvijver   a pond (two deep in the middle) with lily pads and reeds, a little spruce jetty with a bench and a lantern,
                 the berth of the roeibootje beside it; the guh-eendjes come from Bezetting (mama + rijtje: their links
                 can't live in a template)
  speeltuin      a slide from a little tower (ladder up, three slide pieces down onto sand), a swing frame with two swings,
                 a seesaw, a sandbox (park_zandbak: dig a kaasknabbel a day), benches for the parents
  picknickweide  checked picnic blankets on the grass with tea tables and cushions, the kraampje (striped awning, counter,
                 barrels) with the kraampjesguh behind the counter
  muziektuin     flower beds in stripes, a small glass kas with pots, the round muziektent on a raised floor with a pink
                 roof, the jukebox in the middle and three dancing guhs
  japanse_tuin   a brook from a mossy spring under a red arched bridge into a koi pond (two deep) with four Knabbelkoi's,
                 a zen corner of raked sand with a toro and bamboo, pink lampions, blossom trees
"""
import math

from features import park_bouw as pb
from features import wereld
from features.park_bouw import (AIR, BLOESEM_BLAD, G, GRAS, HEG, KLINKERS, LOOP, PAD, PAAL, ROZE_GRAS, bankje, bloem, boom,
                                gazon, lantaarnpaal, pad_cel, plantje, plekje_af, plekje_begin, prullenbak, struik, zet_gras)

WATER = "minecraft:water"
SPAR = "minecraft:spruce_planks"
KERS_STAM = "minecraft:stripped_cherry_log"
ROOD_HEK = "minecraft:mangrove_fence"
ROOD_PLAAT = "minecraft:mangrove_slab"
FACINGS = ("north", "east", "south", "west")


def water(s, x, y, z):
    s.set(x, y, z, WATER, {"level": "0"})


def plaat(soort, blok="minecraft:spruce_slab"):
    return blok, {"type": soort, "waterlogged": "false"}


def zet_plaat(s, x, y, z, soort, blok="minecraft:spruce_slab"):
    n, p = plaat(soort, blok)
    s.set(x, y, z, n, p)


def hekken(s, naam, ook=()):
    """The connections of every post of fence `naam` (to the same fence or `ook`)."""
    for (x, y, z), (n, props, nbt) in list(s.blocks.items()):
        if n == naam:
            out = {"waterlogged": "false"}
            for f, (dx, dz) in pb.DIRS.items():
                m = s.get(x + dx, y, z + dz)
                out[f] = "true" if m == naam or m in ook else "false"
            s.blocks[(x, y, z)] = (n, out, nbt)


def stam(s, x, y, z, hoog, naam=KERS_STAM):
    for dy in range(hoog):
        s.set(x, y + dy, z, naam, {"axis": "y"})


def bord(h, s, x, y, z, kijk, regels):
    """A standing cherry sign with four Dutch lines (translatable: sign.guhs.parkplek.*, chunk c142_park_a)."""
    import sign_text
    B = h.Byte
    voor = {"messages": sign_text.messages("sign.guhs.parkplek", regels), "color": "black", "has_glowing_text": B(0)}
    leeg = {"messages": sign_text.messages("sign.guhs.parkplek", ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}
    rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[kijk]
    s.set(x, y, z, "minecraft:cherry_sign", {"rotation": rot, "waterlogged": "false"},
          {"id": "minecraft:sign", "is_waxed": B(1), "front_text": voor, "back_text": leeg})


def oever(s, rng, sx, sz, nat):
    """The bank round a pond: the ground cells next to water become sand or gravel now and then, with reeds and pink grass."""
    for x in range(sx):
        for z in range(sz):
            if (x, z) in nat or s.get(x, G, z) not in (GRAS, ROZE_GRAS):
                continue
            if any((x + dx, z + dz) in nat for dx, dz in pb.DIRS.values()):
                if rng.random() < 0.45:
                    s.set(x, G, z, "minecraft:sand" if rng.random() < 0.7 else "minecraft:gravel")
                if s.get(x, LOOP, z) is None and rng.random() < 0.5:
                    s.set(x, LOOP, z, rng.choice(["guhs:roze_gras", "minecraft:short_grass", "guhs:pluisgras", "minecraft:fern"]))


# =====================================================================================================================
# de eendenvijver
# =====================================================================================================================
EENDENVIJVER = (17, 12, 15)


def eendenvijver(h, p):
    sx, sy, sz = EENDENVIJVER
    s, rng, mid = plekje_begin(h, "eendenvijver", sx, sy, sz, 1420301, roze=0.3)
    cx, cz, rx, rz = 8.5, 5.4, 6.6, 4.6
    nat, diep = set(), set()
    for x in range(sx):
        for z in range(sz):
            d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if d <= 1.0:
                nat.add((x, z))
                if d <= 0.5:
                    diep.add((x, z))
    for (x, z) in nat:
        water(s, x, G, z)
        if (x, z) in diep:
            water(s, x, 0, z)
            s.set(x, LOOP, z, AIR)
        else:
            s.set(x, 0, z, "minecraft:clay" if rng.random() < 0.4 else "minecraft:sand")
    # the jetty: two wide, spruce on spruce posts, from the bank into the pond; a bench and a lantern at its end
    for z in range(6, 12):
        for x in (8, 9):
            s.set(x, G, z, SPAR)
            s.set(x, LOOP, z, AIR)
            s.set(x, LOOP + 1, z, AIR)
    for (x, z) in ((8, 6), (9, 6), (8, 9), (9, 9)):
        if (x, z) in diep:
            s.set(x, 0, z, "minecraft:spruce_log", {"axis": "y"})
    for z in range(12, sz):
        for x in (8, 9):
            pad_cel(s, x, z)
    bankje(s, 9, LOOP, 6, "north")
    lantaarnpaal(s, 8, LOOP, 7)   # (on the jetty, beside the berth)
    bx, by, bz = p["LIGPLAATS"]
    assert (bx, bz) in nat and (bx - 1, bz) in nat and (bx, bz - 1) in nat and (bx, bz + 1) in nat, "the berth is in the pond"
    for dx in (-1, 0):
        for dz in (-1, 0, 1):
            s.set(bx + dx, LOOP, bz + dz, AIR)            # (no lily pad where the boat lies)
    # lily pads, the bank, benches facing the pond, a sign, trees
    for (x, z) in sorted(nat):
        if s.get(x, LOOP, z) is None and abs(x - bx) + abs(z - bz) > 2 and rng.random() < 0.12:
            s.set(x, LOOP, z, "minecraft:lily_pad")
    for (ex, ey, ez) in p["EENDEN"]:
        assert (ex, ez) in nat, "an eendje spot is in the pond"
        s.set(ex, LOOP, ez, AIR)
    oever(s, rng, sx, sz, nat)
    for (x, z, kijk) in ((3, 11, "north"), (13, 11, "north")):
        gazon(rng, s, x, G, z)
        bankje(s, x, LOOP, z, kijk)
    prullenbak(s, 11, LOOP, 12, "west")
    lantaarnpaal(s, 6, LOOP, 12)
    bord(h, s, 10, LOOP, 13, "south", ["De Eendenvijver", "Voer de eendjes", "brood of zaadjes.", "Roeien mag! Njeg."])
    boom(rng, s, 15, LOOP, 12, hoogte=4, r=2.2)
    boom(rng, s, 1, LOOP, 12, hoogte=3, r=1.8, soort="pluis")
    struik(rng, s, 15, LOOP, 1)
    struik(rng, s, 1, LOOP, 1, soort="pluis")
    return plekje_af(s, rng, sx, sz, heg=0.35)


# =====================================================================================================================
# de speeltuin
# =====================================================================================================================
SPEELTUIN = (17, 10, 15)
GLIJ_TOREN = (2, 3)          # x0, x1 of the tower (z 2..4); the slide goes east from x1 + 1 at z = 3
SCHOMMEL_REK = (10, 14, 8)   # x0, x1, z of the swing frame (bar at LOOP + 2)
WIP = (12, 12)
ZANDBAK = (2, 8, 6, 12)      # x0, z0, x1, z1 (border included)


def speeltuin(h, p):
    sx, sy, sz = SPEELTUIN
    s, rng, mid = plekje_begin(h, "speeltuin", sx, sy, sz, 1420302, roze=0.25)
    for z in range(5, sz):
        pad_cel(s, mid, z)
    for x in range(3, 10):
        pad_cel(s, x, 6)
    # the slide: a little tower (four posts, a planks back wall with the ladder, the platform, a railing), three slide
    # pieces down to the east, sand where you land
    x0, x1 = GLIJ_TOREN
    for (x, z) in ((x0, 2), (x1, 2), (x0, 4), (x1, 4)):
        stam(s, x, LOOP, z, 2)
    for y in (LOOP, LOOP + 1):
        s.set(x0, y, 3, SPAR)
        s.set(x1, y, 3, AIR)
    for x in (x0, x1):
        for z in (2, 3, 4):
            s.set(x, LOOP + 2, z, SPAR)
        for z in (2, 4):
            s.set(x, LOOP + 3, z, PAAL)
        s.set(x, LOOP + 3, 3, AIR)
        s.set(x, LOOP + 4, 3, AIR)
    for y in (LOOP, LOOP + 1, LOOP + 2):
        s.set(x0 - 1, y, 3, "minecraft:ladder", {"facing": "west", "waterlogged": "false"})
    s.set(x0 - 1, G, 3, PAD)
    for i in range(3):
        x, y = x1 + 1 + i, LOOP + 2 - i
        s.set(x, y, 3, "guhs:park_glijbaan", {"facing": "east"})
        for dy in (1, 2):
            s.set(x, y + dy, 3, AIR)
        for yy in range(LOOP, y):
            s.set(x, yy, 3, AIR)
    for x in range(x1 + 3, x1 + 8):
        for z in (2, 3, 4):
            s.set(x, G, z, "minecraft:sand")
            if (x, z) != (x1 + 3, 3):
                s.set(x, LOOP, z, AIR)
                s.set(x, LOOP + 1, z, AIR)
    # the swing frame: two posts, the bar, two swings (they swing north-south)
    a, b, zr = SCHOMMEL_REK
    for x in (a, b):
        stam(s, x, LOOP, zr, 2)
    for x in range(a, b + 1):
        s.set(x, LOOP + 2, zr, KERS_STAM, {"axis": "x"})
    for x in range(a + 1, b):
        for z in range(zr - 2, zr + 3):
            for y in (LOOP, LOOP + 1):
                if (x, z) != (x, zr) or y == LOOP + 1:
                    s.set(x, y, z, AIR)
            zet_gras(s, x, G, z, GRAS)
    for x in (a + 1, b - 1):
        s.set(x, LOOP, zr, "guhs:park_schommel", {"facing": "south", "stand": "3"})
    s.set(a + 2, LOOP, zr, AIR)
    # the seesaw (along x)
    wx, wz = WIP
    for x in range(wx - 2, wx + 3):
        for y in (LOOP, LOOP + 1):
            s.set(x, y, wz, AIR)
    s.set(wx, LOOP, wz, "guhs:park_wip", {"facing": "east", "kant": "vlak"})
    # the sandbox: a spruce slab rim, the sand inside (dig it: park_zandbak), a bucket and a little sand castle
    zx0, zz0, zx1, zz1 = ZANDBAK
    for x in range(zx0, zx1 + 1):
        for z in range(zz0, zz1 + 1):
            if x in (zx0, zx1) or z in (zz0, zz1):
                zet_plaat(s, x, LOOP, z, "bottom")
                s.set(x, G, z, "minecraft:dirt")
            else:
                s.set(x, G, z, "guhs:park_zandbak")
                s.set(x, LOOP, z, AIR)
                s.set(x, LOOP + 1, z, AIR)
    s.set(zx1 - 1, LOOP, zz0 + 1, "minecraft:sandstone_wall", {"east": "none", "north": "none", "south": "none", "west": "none",
                                                                "up": "true", "waterlogged": "false"})
    # benches for the parents, a lantern, a bin, a sign, trees and bushes
    bankje(s, mid + 1, LOOP, 10, "west")
    bankje(s, 15, LOOP, 4, "west")
    lantaarnpaal(s, mid + 1, LOOP, 12)
    prullenbak(s, mid - 1, LOOP, 13, "east")
    bord(h, s, mid + 1, LOOP, 13, "south", ["Speeltuin", "De Vrolijke Vads", "Glijden, schommelen,", "wippen. VAHOEG!"])
    boom(rng, s, 15, LOOP, 13, hoogte=3, r=1.6)
    boom(rng, s, 1, LOOP, 13, hoogte=3, r=1.6, soort="pluis")
    struik(rng, s, 15, LOOP, 1)
    return plekje_af(s, rng, sx, sz, heg=0.5)


# =====================================================================================================================
# de picknickweide
# =====================================================================================================================
PICKNICKWEIDE = (15, 10, 13)
KLEDEN = [(1, 6, 4, 8), (10, 6, 13, 8), (9, 10, 12, 11)]     # x0, z0, x1, z1 of the picnic blankets


def picknickweide(h, p):
    sx, sy, sz = PICKNICKWEIDE
    s, rng, mid = plekje_begin(h, "picknickweide", sx, sy, sz, 1420303, roze=0.4)
    for z in range(4, sz):
        pad_cel(s, mid, z)
    # the kraampje: posts, a striped awning, the counter, barrels and crates behind it, the kraampjesguh
    kx, ky, kz = p["KRAAM"]
    for x in range(kx - 3, kx + 4):
        for z in range(0, 4):
            s.set(x, G, z, SPAR)
            s.set(x, LOOP, z, AIR)
            s.set(x, LOOP + 1, z, AIR)
            s.set(x, LOOP + 2, z, AIR)
    for (x, z) in ((kx - 3, 0), (kx + 3, 0), (kx - 3, 3), (kx + 3, 3)):
        stam(s, x, LOOP, z, 3)
    for x in range(kx - 4, kx + 5):
        for z in range(0, 5):
            s.set(x, LOOP + 3, z, "minecraft:red_wool" if (x + (1 if z == 4 else 0)) % 2 else "minecraft:white_wool")
    for x in range(kx - 2, kx + 3):
        zet_plaat(s, x, LOOP, 3, "top")
    for x in (kx - 2, kx + 2):
        s.set(x, LOOP, 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    s.set(kx - 2, LOOP + 1, 1, "minecraft:flower_pot")
    s.set(kx + 2, LOOP + 1, 1, "minecraft:potted_red_tulip")
    s.set(kx - 1, LOOP + 1, 3, "minecraft:potted_azure_bluet")
    s.set(kx + 1, LOOP + 1, 3, "minecraft:decorated_pot", {"facing": "south", "cracked": "false", "waterlogged": "false"})
    s.set(kx, ky, kz, AIR)
    s.set(kx, ky + 1, kz, AIR)
    s.entity(kx + 0.5, float(ky), kz + 0.5, wereld.npc(h, "kraamguh", "parka_kraamguh", yaw=p["KRAAM_YAW"]))
    bord(h, s, kx + 4, LOOP, 4, "south", ["Het Kraampje", "Lekkers voor je", "picknick! Betalen", "met vads. Njeg!"])
    # the blankets, with tea and cushions on them
    for (x0, z0, x1, z1) in KLEDEN:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                zet_gras(s, x, G, z, GRAS)
                s.set(x, LOOP, z, "guhs:park_picknickkleed")
    s.set(2, LOOP, 7, "guhs:theetafel", {"gedekt": "true"})
    s.set(3, LOOP, 7, "guhs:pink_kussen", {"facing": "west"})
    s.set(11, LOOP, 7, "guhs:theetafel", {"gedekt": "true"})
    s.set(12, LOOP, 7, "guhs:red_kussen", {"facing": "west"})
    s.set(10, LOOP, 11, "guhs:yellow_kussen", {"facing": "east"})
    s.set(11, LOOP, 10, "minecraft:decorated_pot", {"facing": "north", "cracked": "false", "waterlogged": "false"})
    # shade, flowers, a bench, a bin, a lantern
    boom(rng, s, 2, LOOP, 10, hoogte=4, r=2.3)
    boom(rng, s, 13, LOOP, 4, hoogte=3, r=1.7, soort="pluis")
    bankje(s, mid + 1, LOOP, 5, "west")
    prullenbak(s, mid - 1, LOOP, 5, "east")
    lantaarnpaal(s, mid + 1, LOOP, 9)
    return plekje_af(s, rng, sx, sz, heg=0.3)


# =====================================================================================================================
# de muziektuin
# =====================================================================================================================
MUZIEKTUIN = (17, 13, 17)
TENT = (8, 9, 3.2)           # the muziektent: middle x, z and the radius of its floor
KAS = (11, 1, 15, 5)         # x0, z0, x1, z1 of the kas


def muziektuin(h, p):
    sx, sy, sz = MUZIEKTUIN
    s, rng, mid = plekje_begin(h, "muziektuin", sx, sy, sz, 1420304, roze=0.35)
    tx, tz, tr = TENT
    for z in range(13, sz):
        pad_cel(s, mid, z)
    # flower beds in stripes on the west side, a little path between each two
    soorten = ["minecraft:pink_tulip", "guhs:roze_guhbloem", "minecraft:allium", "minecraft:white_tulip", "guhs:knabbelroos",
               "guhs:kaasbloem", "minecraft:red_tulip", "guhs:guhoortjes"]
    rij = 0
    for z in range(1, 15):
        if z % 3 == 0:
            for x in range(1, 5):
                pad_cel(s, x, z)
            rij += 1
            continue
        for x in range(1, 5):
            zet_gras(s, x, G, z, ROZE_GRAS if rij % 2 else GRAS)
            s.set(x, LOOP, z, soorten[(rij * 2 + (z % 3)) % len(soorten)])
    for z in range(1, 15):
        pad_cel(s, 5, z)
    # the kas: glass walls and a pink glass roof, a door gap in the south, pots on tables, a composter, water
    x0, z0, x1, z1 = KAS
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, G, z, "minecraft:smooth_stone")
            rand = x in (x0, x1) or z in (z0, z1)
            for y in (LOOP, LOOP + 1):
                s.set(x, y, z, "minecraft:glass" if rand else AIR)
            s.set(x, LOOP + 2, z, "minecraft:pink_stained_glass")
    deur = (x0 + x1) // 2
    for y in (LOOP, LOOP + 1):
        s.set(deur, y, z1, AIR)
    s.set(deur, G, z1, PAD)
    for x in range(deur, deur + 1):
        for z in range(z1 + 1, tz):
            pad_cel(s, x, z)
    zet_plaat(s, x0 + 1, LOOP, z0 + 1, "top")
    zet_plaat(s, x1 - 1, LOOP, z0 + 1, "top")
    s.set(x0 + 1, LOOP + 1, z0 + 1, "minecraft:potted_allium")
    s.set(x1 - 1, LOOP + 1, z0 + 1, "minecraft:potted_pink_tulip")
    s.set(deur, LOOP, z0 + 1, "minecraft:potted_cherry_sapling")
    s.set(x0 + 1, LOOP, z1 - 1, "minecraft:composter", {"level": "3"})
    s.set(x1 - 1, LOOP, z1 - 1, "minecraft:water_cauldron", {"level": "3"})
    # the muziektent: a round raised floor, six slim posts, a pink roof in rings, the jukebox in the middle
    for x in range(sx):
        for z in range(sz):
            d = math.hypot(x - tx, z - tz)
            if d <= tr:
                s.set(x, G, z, "minecraft:stone_bricks")
                s.set(x, LOOP, z, "minecraft:smooth_quartz" if (x + z) % 2 else "minecraft:pink_terracotta")
                for y in range(LOOP + 1, LOOP + 4):
                    s.set(x, y, z, AIR)
            if d <= tr + 0.6:
                s.set(x, LOOP + 4, z, "minecraft:pink_wool")
            if d <= tr - 1.0:
                s.set(x, LOOP + 5, z, "minecraft:pink_wool")
            if d <= tr - 2.2:
                s.set(x, LOOP + 6, z, "minecraft:pink_terracotta")
    s.set(tx, LOOP + 7, tz, "guhs:park_lantaarn")
    for k in range(6):
        a = math.radians(60 * k)
        x, z = int(round(tx + 2.6 * math.cos(a))), int(round(tz + 2.6 * math.sin(a)))
        for y in range(LOOP + 1, LOOP + 4):
            s.set(x, y, z, PAAL)
    jx, jy, jz = p["JUKEBOX"]
    assert (jx, jz) == (tx, tz) and jy == LOOP + 1
    s.set(jx, jy, jz, "minecraft:jukebox", {"has_record": "false"})
    zet_plaat(s, tx, LOOP, tz + 4, "bottom", "minecraft:smooth_quartz_slab")     # (the step up, at the entrance side)
    s.set(tx, G, tz + 4, "minecraft:stone_bricks")
    s.set(tx, LOOP + 1, tz + 4, AIR)
    for n, (dx_, dy_, dz_) in enumerate(p["DANSERS"], start=1):
        assert math.hypot(dx_ - tx, dz_ - tz) <= tr and dy_ == LOOP + 1
        from features import park_plek_a
        s.entity(dx_ + 0.5, float(dy_), dz_ + 0.5, h.ms.guh_nbt(0.9, Variant=park_plek_a.DANSER_VACHT[n - 1], Behavior="PASSIVE",
                                                                Invulnerable=h.Byte(1), NeoForgeData={"guhs_bezetting": f"parka_danser_{n}"}))
    # flowers round the tent, a bench facing it, lanterns, a bin, the sign
    for x in range(sx):
        for z in range(sz):
            d = math.hypot(x - tx, z - tz)
            if tr < d <= tr + 1.6 and s.get(x, LOOP, z) is None and s.get(x, G, z) in (GRAS, ROZE_GRAS):
                s.set(x, LOOP, z, bloem(rng))
    bankje(s, 14, LOOP, 10, "west")
    bankje(s, 14, LOOP, 8, "west")
    lantaarnpaal(s, 14, LOOP, 13)
    lantaarnpaal(s, 6, LOOP, 15)
    prullenbak(s, mid - 1, LOOP, 14, "east")
    bord(h, s, mid + 1, LOOP, 15, "south", ["De Muziektuin", "Klik op de jukebox", "en de guhs gaan", "dansen! Vahoeg!"])
    boom(rng, s, 15, LOOP, 15, hoogte=3, r=1.6)
    struik(rng, s, 12, LOOP, 14)
    return plekje_af(s, rng, sx, sz, heg=0.4)


# =====================================================================================================================
# de japanse tuin
# =====================================================================================================================
JAPANSE_TUIN = (17, 12, 15)
KOIVIJVER = (12.0, 5.6, 3.6, 4.2)     # middle x, z, radius x, z
BEEK = (5, 6)                          # the z rows of the brook (from x 1 east into the pond)
BRUG = (2, 4)                          # x0, x1 of the bridge (it crosses the brook north-south at x0..x1, z 4..7)


def japanse_tuin(h, p):
    sx, sy, sz = JAPANSE_TUIN
    s, rng, mid = plekje_begin(h, "japanse_tuin", sx, sy, sz, 1420305, roze=0.3)
    cx, cz, rx, rz = KOIVIJVER
    nat, diep = set(), set()
    for x in range(sx):
        for z in range(sz):
            d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if d <= 1.0:
                nat.add((x, z))
                if d <= 0.6:
                    diep.add((x, z))
    beek = {(x, z) for x in range(1, int(cx)) for z in BEEK}
    for (x, z) in nat | beek:
        water(s, x, G, z)
        s.set(x, LOOP, z, AIR)
        if (x, z) in diep:
            water(s, x, 0, z)
        else:
            s.set(x, 0, z, "minecraft:gravel" if (x, z) in beek else "minecraft:sand")
    # the spring: mossy stones round the head of the brook
    for (x, y, z) in ((0, LOOP, 4), (0, LOOP, 5), (0, LOOP, 6), (0, LOOP, 7), (1, LOOP, 4), (1, LOOP, 7), (0, LOOP + 1, 5), (0, LOOP + 1, 6)):
        s.set(x, y, z, "minecraft:mossy_cobblestone")
    s.set(0, G, 5, "minecraft:mossy_cobblestone")
    s.set(0, G, 6, "minecraft:mossy_cobblestone")
    # the walks: from the entrance north, west along the pond, over the bridge to the zen corner
    bx0, bx1 = BRUG
    bm = (bx0 + bx1) // 2
    for z in range(8, sz):
        pad_cel(s, mid, z)
    for x in range(bm, mid + 1):
        for z in (9, 10):
            pad_cel(s, x, z)
    for z in (2, 3, 8):
        pad_cel(s, bm, z)
    # the red arched bridge: half up, up, up, half up (mangrove: the reddest wood), a rail on the high part, red posts
    # with pink lampions at the four corners
    for x in range(bx0, bx1 + 1):
        for z, soort in ((7, "bottom"), (6, "top"), (5, "top"), (4, "bottom")):
            zet_plaat(s, x, LOOP, z, soort, ROOD_PLAAT)
            s.set(x, LOOP + 1, z, AIR)
            s.set(x, LOOP + 2, z, AIR)
        for z in (4, 7):
            if s.get(x, G, z) not in (WATER,):
                s.set(x, G, z, "minecraft:dirt")
    for z in (5, 6):
        s.set(bx0, LOOP + 1, z, ROOD_HEK)
        s.set(bx1, LOOP + 1, z, ROOD_HEK)
    for (x, z) in ((bx0 - 1, 3), (bx1 + 1, 3), (bx0 - 1, 8), (bx1 + 1, 8)):
        s.set(x, G, z, "minecraft:stone_bricks")
        s.set(x, LOOP, z, "minecraft:red_concrete")
        s.set(x, LOOP + 1, z, "guhs:lampion_roze", {"hanging": "false"})
    # the zen corner: raked sand, a toro, bamboo, a bench to look at the brook
    for x in range(1, 7):
        for z in range(0, 3):
            if s.get(x, G, z) in (GRAS, ROZE_GRAS) and s.get(x, LOOP, z) is None and (x, z) != (bm, 2):
                s.set(x, G, z, "guhs:geharkt_zand", {"axis": "x"})
    s.set(1, LOOP, 1, "guhs:toro", {"lit": "true"})
    s.set(1, G, 1, "minecraft:stone_bricks")
    bankje(s, 4, LOOP, 1, "south")
    for (x, z, hoog) in ((6, 0, 4), (6, 1, 3), (5, 0, 3)):
        s.set(x, G, z, GRAS, {"snowy": "false"})
        for i in range(hoog):
            top = hoog - 1 - i
            s.set(x, LOOP + i, z, "guhs:guh_bamboe", {"leaves": "large" if top == 0 else "small" if top == 1 else "none", "stage": "0"})
    # round the koi pond: lily pads, stones, a toro, a bench to watch the Knabbelkoi's, the sign
    for (x, z) in sorted(nat):
        if (x, z) not in beek and s.get(x, LOOP, z) == AIR and rng.random() < 0.1:
            s.set(x, LOOP, z, "minecraft:lily_pad")
    for (kx, ky, kz) in p["KOIEN"]:
        assert (kx, kz) in diep and ky == 0, "a Knabbelkoi spot is deep in the pond"
        s.set(kx, LOOP, kz, AIR)
        s.entity(kx + 0.5, 0.3, kz + 0.5, {"id": "guhs:knabbelkoi", "PersistenceRequired": h.Byte(1), "Invulnerable": h.Byte(1),
                                           "NeoForgeData": {"guhs_bezetting": "parka_koi_" + str(p["KOIEN"].index((kx, ky, kz)) + 1)}})
    oever(s, rng, sx, sz, nat | beek)
    for (x, z) in ((16, 10), (8, 1)):
        if s.get(x, G, z) in (GRAS, ROZE_GRAS, "minecraft:sand", "minecraft:gravel"):
            s.set(x, LOOP, z, "guhs:toro", {"lit": "true"})
    for (x, z) in ((10, 11), (15, 1), (9, 2)):
        if s.get(x, LOOP, z) in (None, "guhs:roze_gras", "minecraft:short_grass", "guhs:pluisgras", "minecraft:fern"):
            s.set(x, LOOP, z, "minecraft:mossy_cobblestone_slab", {"type": "bottom", "waterlogged": "false"})
    bankje(s, mid - 1, LOOP, 8, "east")
    bord(h, s, mid + 1, LOOP, 13, "south", ["De Japanse Tuin", "Over het bruggetje", "naar de Knabbel-", "koi's. Niet vissen!"])
    boom(rng, s, 14, LOOP, 12, hoogte=4, r=2.4)
    boom(rng, s, 2, LOOP, 12, hoogte=3, r=1.9)
    lantaarnpaal(s, mid + 1, LOOP, 11)
    hekken(s, ROOD_HEK)
    return plekje_af(s, rng, sx, sz, heg=0.3)


# --- for park_bouw.templates ----------------------------------------------------------------------------------------------
BOUWERS = {"eendenvijver": eendenvijver, "speeltuin": speeltuin, "picknickweide": picknickweide, "muziektuin": muziektuin,
           "japanse_tuin": japanse_tuin}


def templates(h, p):
    return {naam: f(h, p) for naam, f in BOUWERS.items()}


def check(t):
    """The plekjes of this part: walkable from the entrance to their things (the park's own check does the conventions)."""
    problems = []
    for naam in BOUWERS:
        s = t[naam]
        cel = pb.bereikbaar(s, (s.size[0] // 2, s.size[2] - 1))
        if len(cel) < 20:
            problems.append(f"plekje {naam}: only {len(cel)} cells reachable from the entrance")
    return problems
