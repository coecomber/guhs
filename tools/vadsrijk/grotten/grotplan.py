"""U-GROTTEN: the five atmosphere caves of Het Vadsrijk (FEEDBACK_RONDE4 V2), one per zone, as data + theme + deco.

Every cave is written in its zone's WORLD-LOCAL frame and turned into world coordinates through indeling.py (the mirror of
VadsrijkZones.java): zone 1/4/5 local = world, zone 2 + (704, 1344), zone 3 relative to the trunk + (1404, -576).
POI ids (stand positions, also generated into GrotData.java by tools/features/vadsrijk_u_grotten.py):
    grot_<id>_ingang  outside, in front of the entrance       grot_<id>_binnen  the first room
    grot_<id>_hal     the big room                            grot_<id>_puzzel  puzzle anchor (y = floor - 4, see PuzzelStempel)
    grot_<id>_elite   the elite's den                          grot_<id>_schat   stand spot in front of the treasure chest
    grot_<id>_amb_1/2 Ambacht anchors (y = floor - 3: AmbachtPlekken scans 20 up / 30 down from here)
"""
import math
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))


def _indeling():
    """tools/vadsrijk/indeling.py under its own name (zone 4's kit has a different module called indeling)."""
    import importlib.util
    spec = importlib.util.spec_from_file_location("vadsrijk_indeling_grotten", os.path.join(HIER, "..", "indeling.py"))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


IND = _indeling()
from grot import Grot, ZET, LUCHT, STIJG  # noqa: E402

# ------------------------------------------------------------------------------------------------ the caves (meta data)
# id -> zone, NL/EN name, levels (normal mobs), elite level, ambacht spots: (key, ambacht, tier, count, radius, search)
META = {
    "zwamgrot": dict(zone=1, nl="De Zwamgrot", en="The Toadstool Grotto", lv=(11, 13), elite=13, biome="grot_zwamgrot",
                     amb=[("amb_1", "PLUKKEN", 1, 6, 11, "GROND"), ("amb_2", "MIJNEN", 1, 6, 11, "STEEN")],
                     onder=("Een gloeiende paddenstoelengrot onder het Sporenwoud", "A glowing mushroom grotto under the Sporenwoud")),
    "kristalgrot": dict(zone=2, nl="De Kristalgrot", en="The Crystal Grotto", lv=(22, 24), elite=24, biome="grot_kristalgrot",
                        amb=[("amb_1", "MIJNEN", 2, 6, 10, "STEEN"), ("amb_2", "VISSEN", 2, 4, 9, "WATER")],
                        onder=("Kristallen en een bevroren meer diep in de Galmtop", "Crystals and a frozen lake deep in the Galmtop")),
    "wortelgrot": dict(zone=3, nl="De Wortelgrot", en="The Root Grotto", lv=(44, 46), elite=47, biome="grot_wortelgrot",
                       amb=[("amb_1", "HOUTHAKKEN", 3, 6, 11, "BOOM"), ("amb_2", "MIJNEN", 3, 6, 11, "STEEN")],
                       onder=("Tussen de wortels van de Wereldboom", "Between the roots of the World Tree")),
    "koraalgrot": dict(zone=4, nl="De Koraalgrot", en="The Coral Grotto", lv=(53, 55), elite=55, biome="grot_koraalgrot",
                       amb=[("amb_1", "VISSEN", 3, 4, 7, "WATER"), ("amb_2", "MIJNEN", 3, 6, 9, "STEEN")],
                       onder=("Een zeegrot onder de Koraalbaai, alleen duikend te bereiken", "A sea cave under Coral Bay, reached by diving")),
    "wolkengrot": dict(zone=5, nl="De Wolkengrotten", en="The Cloud Caves", lv=(58, 60), elite=60, biome="grot_wolkengrot",
                       amb=[("amb_1", "MIJNEN", 3, 6, 10, "STEEN"), ("amb_2", "PLUKKEN", 3, 6, 10, "GROND")],
                       onder=("Wolkengrotten in de kop van Moeder Wolkwal", "Cloud caves in Mother Cloudwhale's head")),
}

SLAB = "[type=bottom,waterlogged=false]"
LANT = "minecraft:lantern[hanging=false,waterlogged=false]"
LANT_H = "minecraft:lantern[hanging=true,waterlogged=false]"
ZLANT = "minecraft:soul_lantern[hanging=false,waterlogged=false]"
KIST = "minecraft:chest[facing={f},type=single,waterlogged=false]"


def z3(rx, rz):
    """Zone 3 local (relative to the trunk) -> world x, z."""
    return rx + IND.Z3_DX, rz + IND.Z3_DZ


def kompas(dx, dz):
    """The facing (north/east/south/west) of a horizontal vector."""
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def trap_naar(g, cx, cz, S_top, S_bot, theta_top, opening=True):
    """A spiral whose top arrives at angle theta_top (the door side)."""
    th_bot = theta_top - 2 * math.pi * (S_top - S_bot) / STIJG
    g.trap(cx, cz, S_top, S_bot, th_bot, opening)


# ------------------------------------------------------------------------------------------------ deco helpers
def plafondlicht(g, rng, blok, kans, eigen=None):
    for (x, y, z) in g.plafondcellen(eigen):
        if rng.random() < kans and g.lees(x, y + 1, z)[0] in (4, 5):
            g.zet(x, y + 1, z, blok)


def hangers(g, rng, lijst, kans, lengte=1, eigen=None):
    """Things hanging from the ceiling. lijst: [(state, weight)], or a callable (g, x, y, z, rng)."""
    for (x, y, z) in g.plafondcellen(eigen):
        if g.bezet_vrij(x, z) or rng.random() >= kans:
            continue
        if callable(lijst):
            lijst(g, x, y, z, rng)
        else:
            g.zet(x, y, z, g._kies(lijst, x, z, y))


def plantjes(g, rng, lijst, kans, eigen=None):
    for (x, y, z) in g.vloercellen(eigen):
        if rng.random() < kans:
            g.zet(x, y, z, g._kies(lijst, x, z, y + 3))


def lantaarnpalen(g, hek, lamp, stap=7.0):
    for (x, y, z, _) in g.langs_gangen(stap):
        if g.is_lucht(x, y, z) and g.is_lucht(x, y + 1, z) and g.is_lucht(x, y + 2, z) and not g.bezet_vrij(x, z):
            g.zet(x, y, z, hek)
            g.zet(x, y + 1, z, lamp)


def ring_palen(g, k, r, n, hek, lamp, hoog=2, start=0.3):
    """n lamp posts on a ring at normalised radius r of room k."""
    for i in range(n):
        a = start + 2 * math.pi * i / n
        x, z = round(k["cx"] + math.cos(a) * k["rx"] * r), round(k["cz"] + math.sin(a) * k["rz"] * r)
        y = g.stand(x, z)
        if y is None or g.is_halfje(x, z) or not all(g.is_lucht(x, y + d, z) for d in range(hoog + 2)):
            continue
        for d in range(hoog):
            g.zet(x, y + d, z, hek)
        g.zet(x, y + hoog, z, lamp)


def zuil(g, x, z, blokken, top=None, max_h=99):
    """A column from the floor up to the ceiling (or max_h), optional top block."""
    y = g.stand(x, z)
    if y is None or g.is_halfje(x, z):
        return
    h = 0
    while g.is_lucht(x, y + h, z) and h < max_h:
        g.zet(x, y + h, z, g._kies(blokken, x, z, y + h))
        h += 1
    if top and h:
        g.zet(x, y + h - 1, z, top)


def stalagmiet(g, rng, x, z, blokken, h, top=None):
    y = g.stand(x, z)
    if y is None or g.is_halfje(x, z):
        return
    for d in range(h):
        if not g.is_lucht(x, y + d + 1, z):
            return
        g.zet(x, y + d, z, g._kies(blokken, x, z, y + d))
    if top and g.is_lucht(x, y + h, z):
        g.zet(x, y + h, z, top)


def paddenstoel(g, x, z, h, r, kap, licht="minecraft:shroomlight"):
    """A small giant mushroom: stem h high, cap of radius r."""
    y = g.stand(x, z)
    if y is None or g.is_halfje(x, z):
        return
    for d in range(h):
        g.zet(x, y + d, z, "minecraft:mushroom_stem")
    ky = y + h
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if dx * dx + dz * dz <= r * r + 1 and g.is_lucht(x + dx, ky, z + dz):
                g.zet(x + dx, ky, z + dz, kap)
                if dx * dx + dz * dz <= (r - 1) ** 2 and g.is_lucht(x + dx, ky + 1, z + dz):
                    g.zet(x + dx, ky + 1, z + dz, kap)
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if g.is_lucht(x + dx, ky - 1, z + dz):
            g.zet(x + dx, ky - 1, z + dz, licht)


def schatnis(g, k, kist_x, kist_z, naar_x, naar_z, voet, sier, licht):
    """The treasure nook: a chest on a little plinth of `voet`, two lights, a carpet of `sier` in front."""
    y = g.stand(kist_x, kist_z)
    f = kompas(naar_x - kist_x, naar_z - kist_z)
    g.zet(kist_x, y - 1, kist_z, voet)
    g.zet(kist_x, y, kist_z, KIST.format(f=f))
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        xx, zz = kist_x + dx, kist_z + dz
        if (xx, zz) == (naar_x, naar_z) or not g.is_lucht(xx, y, zz):
            continue
        if abs(xx - naar_x) + abs(zz - naar_z) <= 1:
            continue
        g.zet(xx, y, zz, licht)
    if sier:
        g.zet(naar_x, y, naar_z, sier)
    g.vrij.append((kist_x, kist_z, 2.5))


def boog(g, cx, cz, richting, S, breed, hoog, blokken, diep=2):
    """An arch frame round a tunnel mouth: on `diep` planes along `richting` from (cx, cz), the cells round the opening
    (breed wide, hoog high, rounded top) get `blokken`; the opening itself and the cave air are never touched."""
    dx, dz = richting
    L = math.hypot(dx, dz)
    dx, dz = dx / L, dz / L
    nx, nz = -dz, dx
    a = int(math.ceil(S))
    half = breed / 2.0
    for d in range(diep):
        for s in range(-int(half) - 2, int(half) + 3):
            for y in range(a - 1, a + hoog + 2):
                x, z = round(cx + dx * d + nx * s), round(cz + dz * d + nz * s)
                hy = (y - a + 0.5) / hoog
                opening = y >= a and abs(s) <= half and (hy < 0.75 or (abs(s) / (half + 0.5)) ** 2 + ((hy - 0.75) / 0.6) ** 2 < 1)
                if opening or g.is_lucht(x, y, z) or g.lees(x, y, z)[1] in ("minecraft:water",):
                    continue
                g.zet(x, y, z, g._kies(blokken, x, z, y))


# ================================================================================================ zone 1: De Zwamgrot
def zwamgrot():
    g = Grot("zwamgrot", 1, zaad=101)
    g.thema = dict(
        vloer=[("minecraft:moss_block", 4), ("minecraft:mycelium[snowy=false]", 2), ("minecraft:stone", 1.6), ("minecraft:tuff", 1),
               ("minecraft:rooted_dirt", 0.8)],
        plaat=[("minecraft:mossy_cobblestone_slab" + SLAB, 1), ("minecraft:tuff_slab" + SLAB, 1)],
        vul=[("minecraft:stone", 3), ("minecraft:tuff", 2), ("minecraft:deepslate", 1), ("minecraft:andesite", 1)],
        muur=[("minecraft:stone", 3), ("minecraft:tuff", 2), ("minecraft:mossy_cobblestone", 1.6), ("minecraft:moss_block", 1.4),
              ("guhs:glimmos", 0.5), ("minecraft:mushroom_stem", 0.3)],
        pilaar=[("minecraft:mushroom_stem", 3), ("minecraft:shroomlight", 1)],
        trap_blok="minecraft:mossy_cobblestone", trap_plaat="minecraft:mossy_cobblestone_slab" + SLAB,
        hek="minecraft:spruce_fence")
    SX, SZ, TOP, BOT = -419, 148, 69, 49
    trap_naar(g, SX, SZ, TOP, BOT, 0.0)
    sporen = g.kamer("sporenhal", -430, 147, 14, 10, 49, 8)
    g.gang([(-440, 145, 49), (-452, 140, 48.5), (-458, 137, 48), (-466, 134, 47.5), (-472, 132, 47)])
    glim = g.kamer("glimzaal", -482, 130, 16, 13, 47, 13)
    g.gang([(-490, 121, 47), (-497, 110, 47), (-503, 103, 47)])
    puzzel = g.kamer("puzzelhal", -506, 99, 9, 9, 47, 9, wobbel=0.06)
    g.gang([(-489, 138, 47), (-500, 150, 46.5), (-506, 157, 46), (-510, 161, 45.5), (-514, 164, 45)])
    elite = g.kamer("elitehol", -517, 167, 10, 9, 45, 10)
    g.gang([(-522, 173, 45), (-528, 178, 45)])
    nis = g.kamer("schatnis", -532, 181, 4.5, 4.5, 45, 5, hmin=3.5, wobbel=0.05)
    g.plekken = dict(ingang=(-412, TOP, SZ), binnen=(-437, 49, 147), hal=(-482, 47, 130), puzzel=(-506, 43, 99),
                     elite=(-517, 45, 167), schat=(-532, 45, 181), amb_1=(-476, 44, 124), amb_2=(-488, 44, 136))
    g.vrij += [(-506, 99, 7.5), (-517, 167, 2.5), (-437, 147, 1.5)]

    def stoel(g_):
        """The entrance: a giant hollow toadstool (the spiral inside), door to the east, a little path to the forest path."""
        y0 = TOP - 1
        for x in range(SX - 6, SX + 7):
            for z in range(SZ - 6, SZ + 7):
                r = math.hypot(x - SX, z - SZ)
                hoek = math.atan2(z - SZ, x - SX)
                deur = abs(math.atan2(math.sin(hoek), math.cos(hoek))) < 0.42
                if 3.6 < r <= 4.7:
                    for y in range(y0 - 2, TOP + 13):
                        if deur and TOP <= y <= TOP + 2:
                            g_.zet(x, y, z, None, op=LUCHT)
                        else:
                            g_.zet(x, y, z, "minecraft:mushroom_stem")
                elif r <= 3.6 and max(abs(x - SX), abs(z - SZ)) > 1:
                    for y in range(TOP + 6, TOP + 13):
                        g_.zet(x, y, z, None, op=LUCHT)
                elif r <= 3.6:
                    for y in range(TOP + 3, TOP + 13):
                        g_.zet(x, y, z, "minecraft:shroomlight" if y % 3 == 0 else "minecraft:mushroom_stem")
        # the cap
        ky = TOP + 13
        for x in range(SX - 11, SX + 12):
            for z in range(SZ - 11, SZ + 12):
                for y in range(ky - 2, ky + 8):
                    d = math.sqrt((x - SX) ** 2 + (z - SZ) ** 2 + ((y - ky + 2) * 1.6) ** 2)
                    if 8.6 <= d <= 10.4 and y >= ky - 1:
                        stip = (x * 7 + z * 13 + y * 3) % 11 == 0
                        g_.zet(x, y, z, "minecraft:mushroom_stem" if stip else "minecraft:red_mushroom_block")
                r = math.hypot(x - SX, z - SZ)
                if 4.7 < r <= 9.5:
                    g_.zet(x, ky - 1, z, "minecraft:red_mushroom_block")
                    if 6.5 < r <= 8.6 and (x + z) % 3 == 0:
                        g_.zet(x, ky - 2, z, "minecraft:shroomlight")
                elif r <= 3.6:
                    g_.zet(x, ky - 1, z, "minecraft:shroomlight" if (x + z) % 2 == 0 else "minecraft:red_mushroom_block")
        # the doorstep + two lamp posts + glowing toadstools round the foot
        g_.kolom += [(SX + 5, SZ - 2, 1, "minecraft:spruce_fence"), (SX + 5, SZ - 2, 2, LANT),
                     (SX + 5, SZ + 2, 1, "minecraft:spruce_fence"), (SX + 5, SZ + 2, 2, LANT)]
        for a in range(0, 360, 40):
            x, z = round(SX + math.cos(math.radians(a)) * 6), round(SZ + math.sin(math.radians(a)) * 6)
            if abs(a) > 40 and a < 320:
                g_.kolom.append((x, z, 1, "guhs:glimpaddenstoel"))
        # a little path from the door to the Sporenwoud path (towards (-386, 160))
        ax, az, bx, bz = SX + 5, SZ, -387, 161
        n = int(math.hypot(bx - ax, bz - az))
        for i in range(n + 1):
            t = i / n
            x, z = round(ax + (bx - ax) * t), round(az + (bz - az) * t)
            for (dx, dz) in ((0, 0), (0, 1)):
                g_.kolom.append((x + dx, z + dz, 0, "minecraft:dirt_path"))
            if i % 8 == 4:
                g_.kolom += [(x, z - 2, 1, "minecraft:spruce_fence"), (x, z - 2, 2, LANT)]
    g.bouwwerk.append(stoel)

    def deco(g_, rng):
        plafondlicht(g_, rng, "minecraft:shroomlight", 0.07)
        hangers(g_, rng, [("minecraft:spore_blossom", 1)], 0.03)
        plantjes(g_, rng, [("guhs:glimpaddenstoel", 4), ("minecraft:red_mushroom", 2), ("minecraft:brown_mushroom", 2),
                           ("minecraft:moss_carpet", 3), ("guhs:schimmelpaddenstoel", 1)], 0.14)
        # small giant mushrooms in the Glimzaal and the Sporenhal
        for k, n, kap in ((glim, 7, "minecraft:red_mushroom_block"), (sporen, 3, "minecraft:brown_mushroom_block"),
                          (elite, 5, "minecraft:red_mushroom_block")):
            for i in range(n):
                a = rng.uniform(0, 2 * math.pi)
                r = rng.uniform(0.5, 0.78)
                x, z = round(k["cx"] + math.cos(a) * k["rx"] * r), round(k["cz"] + math.sin(a) * k["rz"] * r)
                if not g_.bezet_vrij(x, z, 2) and not g_.pad[x - g_.x0, z - g_.z0]:
                    paddenstoel(g_, x, z, int(rng.integers(3, 6)), int(rng.integers(2, 4)), kap)
        ring_palen(g_, puzzel, 0.82, 6, "minecraft:mushroom_stem", "minecraft:shroomlight", hoog=3)
        ring_palen(g_, glim, 0.9, 8, "minecraft:spruce_fence", LANT)
        lantaarnpalen(g_, "minecraft:spruce_fence", LANT)
        schatnis(g_, nis, -534, 182, -532, 181, "minecraft:gold_block", "minecraft:red_carpet", "guhs:glimpaddenstoel")
    g.deco.append(deco)
    return g


# ================================================================================================ zone 2: De Kristalgrot
def kristalgrot():
    g = Grot("kristalgrot", 2, zaad=202)
    g.thema = dict(
        vloer=[("minecraft:calcite", 3), ("minecraft:snow_block", 2), ("minecraft:packed_ice", 1.2), ("minecraft:stone", 1.4),
               ("minecraft:diorite", 0.8)],
        plaat=[("minecraft:polished_diorite_slab" + SLAB, 1)],
        vul=[("minecraft:stone", 2), ("minecraft:calcite", 1), ("minecraft:packed_ice", 1)],
        muur=[("minecraft:calcite", 3), ("minecraft:packed_ice", 2), ("minecraft:blue_ice", 1), ("minecraft:amethyst_block", 1.2),
              ("guhs:guh_kristal_blok", 0.5), ("minecraft:snow_block", 0.8)],
        pilaar=[("minecraft:packed_ice", 2), ("minecraft:sea_lantern", 1)],
        trap_blok="minecraft:polished_diorite", trap_plaat="minecraft:polished_diorite_slab" + SLAB, hek="minecraft:birch_fence")
    # the mouth on the west flank of the Galmtop, looking out over the Galmspleet
    g.gang([(859, -1104, 119), (868, -1105, 119), (874, -1106, 117.5), (880, -1108, 115), (884, -1109, 114), (890, -1110, 114)], breed=4.0, h=4.6)
    kristal = g.kamer("kristalhal", 894, -1111, 11, 10, 114, 11)
    g.gang([(900, -1116, 114), (904, -1119, 113), (907, -1122, 112), (916, -1129, 112)])
    meer = g.kamer("ijsmeer", 918, -1132, 15, 12, 112, 15)
    g.meer(921, -1134, 9, 6.5, 111, 108.5, ijs=0.6)
    g.gang([(901, -1104, 114), (909, -1097, 114), (912, -1094, 114)])
    puzzel = g.kamer("puzzelhal", 916, -1091, 9, 9, 114, 9, wobbel=0.06)
    g.gang([(927, -1139, 112), (934, -1145, 112)])
    elite = g.kamer("elitehol", 940, -1151, 9, 8, 112, 9)
    g.gang([(945, -1155, 112), (948, -1159, 112)])
    nis = g.kamer("schatnis", 950, -1161, 4.5, 4.5, 112, 5, hmin=3.5, wobbel=0.05)
    g.plekken = dict(ingang=(856, 119, -1104), binnen=(892, 114, -1108), hal=(906, 112, -1127), puzzel=(916, 110, -1091),
                     elite=(940, 112, -1151), schat=(950, 112, -1161), amb_1=(894, 111, -1111), amb_2=(921, 108, -1134))
    g.vrij += [(916, -1091, 7.5), (940, -1151, 2.5)]

    def poort(g_):
        boog(g_, 866, -1104, (1, 0), 119, 4, 4, [("minecraft:calcite", 3), ("minecraft:amethyst_block", 2), ("minecraft:packed_ice", 2),
                                                 ("guhs:guh_kristal_blok", 1)], diep=2)
        for (x, z) in ((864, -1107), (864, -1101)):
            g_.zet(x, 119, z, "minecraft:birch_fence")
            g_.zet(x, 120, z, ZLANT)
        for (x, z) in ((865, -1108), (865, -1100), (866, -1108), (866, -1100)):
            g_.zet(x, 121, z, "guhs:guh_kristal_cluster[facing=up]")
    g.bouwwerk.append(poort)

    def deco(g_, rng):
        plafondlicht(g_, rng, "minecraft:sea_lantern", 0.035)
        hangers(g_, rng, [("minecraft:amethyst_cluster[facing=down,waterlogged=false]", 3), ("guhs:guh_kristal_cluster[facing=down]", 2),
                          ("minecraft:ice", 1)], 0.09)
        plantjes(g_, rng, [("minecraft:amethyst_cluster[facing=up,waterlogged=false]", 2), ("guhs:guh_kristal_cluster[facing=up]", 2),
                           ("minecraft:snow[layers=1]", 3), ("minecraft:small_amethyst_bud[facing=up,waterlogged=false]", 1)], 0.12)
        for k, n in ((kristal, 9), (meer, 7), (elite, 6), (puzzel, 4)):
            for i in range(n):
                a = rng.uniform(0, 2 * math.pi)
                r = rng.uniform(0.62, 0.9) if k is not puzzel else 0.92
                x, z = round(k["cx"] + math.cos(a) * k["rx"] * r), round(k["cz"] + math.sin(a) * k["rz"] * r)
                if not g_.bezet_vrij(x, z, 1.5) and not g_.pad[x - g_.x0, z - g_.z0]:
                    stalagmiet(g_, rng, x, z, [("minecraft:amethyst_block", 2), ("minecraft:calcite", 1), ("minecraft:packed_ice", 1)],
                               int(rng.integers(2, 5)), "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
        ring_palen(g_, meer, 0.86, 7, "minecraft:birch_fence", ZLANT)
        ring_palen(g_, puzzel, 0.8, 6, "minecraft:packed_ice", "minecraft:sea_lantern", hoog=2)
        lantaarnpalen(g_, "minecraft:birch_fence", ZLANT)
        schatnis(g_, nis, 952, -1162, 950, -1161, "minecraft:diamond_block", "minecraft:light_blue_carpet",
                 "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
    g.deco.append(deco)
    return g


# ================================================================================================ zone 3: De Wortelgrot
def wortelgrot():
    g = Grot("wortelgrot", 3, zaad=303)
    g.bewaar_hout = True      # the World Tree's own roots (logs/wood/roots) stay where they cross the cave above head height
    g.thema = dict(
        vloer=[("minecraft:rooted_dirt", 3), ("minecraft:moss_block", 2), ("minecraft:dirt", 1), ("minecraft:stone", 1.5),
               ("minecraft:tuff", 1), ("guhs:vadsmos", 0.8)],
        plaat=[("minecraft:mud_brick_slab" + SLAB, 1)],
        vul=[("minecraft:dirt", 2), ("minecraft:rooted_dirt", 2), ("minecraft:stone", 1), ("minecraft:tuff", 1)],
        muur=[("minecraft:rooted_dirt", 3), ("minecraft:dirt", 1), ("minecraft:stone", 1), ("minecraft:tuff", 1), ("guhs:glimmos", 0.5),
              ("minecraft:dark_oak_wood[axis=y]", 0.7), ("minecraft:mangrove_roots[waterlogged=false]", 0.5)],
        pilaar=[("minecraft:dark_oak_wood[axis=y]", 3), ("minecraft:ochre_froglight[axis=y]", 1)],
        trap_blok="minecraft:stripped_dark_oak_wood[axis=y]", trap_plaat="minecraft:dark_oak_slab" + SLAB, hek="minecraft:dark_oak_fence")
    SX, SZ = z3(6, -155)
    TOP, BOT = 69, 45
    trap_naar(g, SX, SZ, TOP, BOT, -math.pi / 2)
    voet = g.kamer("wortelvoet", *z3(4, -154), 10, 8, 45, 8)
    g.gang([(*z3(-4, -151), 45), (*z3(-8, -149), 44.5), (*z3(-12, -146), 44), (*z3(-18, -143), 44)])
    hal = g.kamer("wortelhal", *z3(-24, -140), 13, 11, 44, 11)
    g.gang([(*z3(-33, -147), 44), (*z3(-40, -154), 44)])
    puzzel = g.kamer("puzzelkamer", *z3(-46, -160), 8, 8, 44, 8, wobbel=0.06)
    g.gang([(*z3(13, -150), 45), (*z3(16, -146), 43.5), (*z3(19, -142), 42), (*z3(21, -139), 41), (*z3(25, -135), 41)])
    elite = g.kamer("elitehol", *z3(26, -133), 9, 8, 41, 9)
    g.gang([(*z3(31, -129), 41), (*z3(35, -126), 41)])
    nis = g.kamer("schatnis", *z3(38, -124), 4.5, 4.5, 41, 5, hmin=3.5, wobbel=0.05)

    def w(rx, y, rz):
        x, z = z3(rx, rz)
        return (x, y, z)
    g.plekken = dict(ingang=w(6, TOP, -161), binnen=w(-2, 45, -152), hal=w(-24, 44, -140), puzzel=w(-46, 40, -160), elite=w(26, 41, -133),
                     schat=w(38, 41, -124), amb_1=w(-20, 41, -136), amb_2=w(-28, 41, -144))
    g.vrij += [(*z3(-46, -160), 7.0), (*z3(26, -133), 2.5)]

    def stomp(g_):
        """The entrance: a hollow root stump (the spiral inside) with the door towards the Wortelweg."""
        for x in range(SX - 8, SX + 9):
            for z in range(SZ - 8, SZ + 9):
                r = math.hypot(x - SX, z - SZ)
                hoek = math.atan2(z - SZ, x - SX)
                deur = abs(math.atan2(math.sin(hoek + math.pi / 2), math.cos(hoek + math.pi / 2))) < 0.42
                hoogte = 7 + int(2 * math.sin(hoek * 3) + 1.5 * math.cos(hoek * 5))
                if 3.6 < r <= 5.2:
                    for y in range(TOP - 3, TOP + hoogte):
                        if deur and TOP <= y <= TOP + 2:
                            g_.zet(x, y, z, None, op=LUCHT)
                        else:
                            g_.zet(x, y, z, "minecraft:dark_oak_wood[axis=y]" if (x + y + z) % 5 else "minecraft:mangrove_roots[waterlogged=false]")
                    if (x * 3 + z) % 4 == 0:
                        g_.zet(x, TOP + hoogte, z, "minecraft:moss_carpet")
                elif 5.2 < r <= 7.5 and (int(math.degrees(hoek)) // 30) % 3 == 0 and not deur:
                    # buttress roots flaring out over the ground
                    h = int(3 * (7.5 - r) / 2.3) + 1
                    for y in range(TOP - 2, TOP - 1 + h):
                        g_.zet(x, y, z, "minecraft:dark_oak_wood[axis=y]")
                elif r <= 3.6 and max(abs(x - SX), abs(z - SZ)) <= 1:
                    for y in range(TOP + 3, TOP + 8):
                        g_.zet(x, y, z, "minecraft:ochre_froglight[axis=y]" if y >= TOP + 6 else "minecraft:dark_oak_wood[axis=y]")
        # glowing lamp posts beside the door and a short path to the Wortelweg (north)
        g_.kolom += [(SX - 2, SZ - 6, 1, "minecraft:dark_oak_fence"), (SX - 2, SZ - 6, 2, LANT),
                     (SX + 2, SZ - 6, 1, "minecraft:dark_oak_fence"), (SX + 2, SZ - 6, 2, LANT)]
        for i in range(6, 18):
            for dx in (0, 1):
                g_.kolom.append((SX + dx, SZ - i, 0, "minecraft:dirt_path"))
    g.bouwwerk.append(stomp)

    def deco(g_, rng):
        plafondlicht(g_, rng, "minecraft:verdant_froglight[axis=y]", 0.025)
        plafondlicht(g_, rng, "minecraft:ochre_froglight[axis=y]", 0.02)

        def ranken(g2, x, y, z, rng2):
            n = int(rng2.integers(1, 5))
            for d in range(n):
                if not g2.is_lucht(x, y - d, z) or not g2.is_lucht(x, y - d - 2, z):
                    return
                tip = d == n - 1
                g2.zet(x, y - d, z, "minecraft:cave_vines[age=25,berries=true]" if tip else "minecraft:cave_vines_plant[berries=true]")
        hangers(g_, rng, ranken, 0.035)
        hangers(g_, rng, [("minecraft:hanging_roots[waterlogged=false]", 1)], 0.12)
        plantjes(g_, rng, [("minecraft:moss_carpet", 4), ("guhs:glimpaddenstoel", 2), ("minecraft:brown_mushroom", 1),
                           ("minecraft:fern", 1)], 0.12)
        # root pillars (logs floor to ceiling) for the Houthakken spots, and in the den
        for k, n in ((hal, 10), (voet, 3), (elite, 4)):
            for i in range(n):
                a = rng.uniform(0, 2 * math.pi)
                r = rng.uniform(0.35, 0.85)
                x, z = round(k["cx"] + math.cos(a) * k["rx"] * r), round(k["cz"] + math.sin(a) * k["rz"] * r)
                if not g_.bezet_vrij(x, z, 1.5) and not g_.pad[x - g_.x0, z - g_.z0]:
                    zuil(g_, x, z, [("minecraft:dark_oak_wood[axis=y]", 3), ("minecraft:mangrove_roots[waterlogged=false]", 1)])
        ring_palen(g_, hal, 0.9, 7, "minecraft:dark_oak_fence", LANT)
        ring_palen(g_, puzzel, 0.85, 5, "minecraft:dark_oak_fence", "minecraft:ochre_froglight[axis=y]")
        lantaarnpalen(g_, "minecraft:dark_oak_fence", LANT)
        kx, kz = z3(40, -123)
        sx, sz = z3(38, -124)
        schatnis(g_, nis, kx, kz, sx, sz, "minecraft:honeycomb_block", "minecraft:orange_carpet", "minecraft:ochre_froglight[axis=y]")
    g.deco.append(deco)
    return g


# ================================================================================================ zone 4: De Koraalgrot
def koraalgrot():
    g = Grot("koraalgrot", 4, zaad=404)
    g.thema = dict(
        vloer=[("minecraft:sand", 3), ("minecraft:gravel", 0.8), ("minecraft:prismarine", 1), ("minecraft:stone", 1.6),
               ("minecraft:tuff", 1)],
        plaat=[("minecraft:prismarine_slab" + SLAB, 1)],
        vul=[("minecraft:sandstone", 2), ("minecraft:stone", 2), ("minecraft:prismarine", 1)],
        muur=[("minecraft:prismarine", 2), ("minecraft:dark_prismarine", 1), ("minecraft:prismarine_bricks", 1), ("minecraft:sandstone", 1),
              ("guhs:kaaskoraalblok", 0.7), ("minecraft:stone", 0.8)],
        pilaar=[("minecraft:prismarine_bricks", 3), ("minecraft:sea_lantern", 1)],
        trap_blok="minecraft:prismarine_bricks", trap_plaat="minecraft:prismarine_brick_slab" + SLAB, hek="minecraft:prismarine_wall")
    # the flooded way in from the open sea east of the atoll (air pockets every ~8 blocks)
    g.gang([(2271, -572, 51), (2264, -572, 50), (2257, -574, 47.5), (2250, -577, 45), (2244, -580, 43), (2239, -583, 41.5)],
           breed=3.0, h=3.0, water=62)
    g.gang([(2239, -583, 41.5), (2236, -586, 40.5), (2233, -588, 40)], breed=3.0, h=2.4, water=62)
    for (x, z, S, L) in ((2257, -574, 47.5, 52), (2248, -578, 44.5, 49), (2240, -583, 41.5, 46)):
        g.kamer("bel", x, z, 2.6, 2.6, S, (L - math.ceil(S)) + 3.5, hmin=L - math.ceil(S) + 2, water=L, wobbel=0.0)
    hal = g.kamer("koraalhal", 2226, -593, 14, 12, 44, 9)
    g.meer(2233, -589, 5.5, 4.5, 43, 39.5)
    g.gang([(2215, -588, 44), (2213, -584, 44)])
    puzzel = g.kamer("puzzelkamer", 2212, -578, 8, 8, 44, 8, wobbel=0.06)
    g.gang([(2219, -601, 44), (2214, -606, 44)])
    elite = g.kamer("elitehol", 2212, -611, 8, 7, 44, 8)
    g.gang([(2207, -615, 44), (2204, -618, 44)])
    nis = g.kamer("schatnis", 2202, -620, 4.5, 4.5, 44, 5, hmin=3.5, wobbel=0.05)
    # the dry way out: a spiral up to a coral chimney on the atoll rim
    EX, EZ, ETOP = 2234, -600, 71
    trap_naar(g, EX, EZ, ETOP, 44, math.radians(135))
    g.monden.append((2273, 52, -572, 3.2))
    g.plekken = dict(ingang=(2269, 63, -568), binnen=(2222, 44, -597), hal=(2224, 44, -596), puzzel=(2212, 40, -578), elite=(2212, 44, -611),
                     schat=(2202, 44, -620), amb_1=(2233, 39, -589), amb_2=(2220, 41, -598), uitgang=(2230, ETOP, -596))
    g.vrij += [(2212, -578, 7.0), (2212, -611, 2.5)]

    def boei(g_):
        # the arch on the sea floor, coral and sea lanterns round it, and a little raft with a lantern right above
        boog(g_, 2268, -572, (-1, 0), 51, 3, 3, [("minecraft:prismarine_bricks", 2), ("minecraft:dark_prismarine", 1),
                                                 ("guhs:kaaskoraalblok", 1), ("minecraft:sea_lantern", 0.7)], diep=2)
        for (x, z) in ((2270, -575), (2270, -569), (2266, -576), (2266, -568)):
            for y in range(50, 52):
                g_.zet(x, y, z, "minecraft:prismarine_bricks")
            g_.zet(x, 52, z, "minecraft:sea_lantern")
        for y in range(50, 62):
            g_.zet(2270, y, -566, "minecraft:dark_prismarine" if y % 4 else "minecraft:sea_lantern")
        for x in range(2268, 2272):
            for z in range(-569, -565):
                g_.zet(x, 62, z, "minecraft:bamboo_block[axis=x]" if (x + z) % 2 else "minecraft:stripped_bamboo_block[axis=z]")
        g_.zet(2270, 63, -566, "minecraft:bamboo_fence")
        g_.zet(2270, 64, -566, LANT)
        g_.zet(2268, 63, -566, "guhs:nepkoraal[kleur=roze]")

    def schoorsteen(g_):
        for x in range(EX - 6, EX + 7):
            for z in range(EZ - 6, EZ + 7):
                r = math.hypot(x - EX, z - EZ)
                hoek = math.atan2(z - EZ, x - EX)
                deur = abs(math.atan2(math.sin(hoek - math.radians(135)), math.cos(hoek - math.radians(135)))) < 0.42
                if 3.6 < r <= 4.8:
                    for y in range(ETOP - 4, ETOP + 5):
                        if deur and ETOP <= y <= ETOP + 2:
                            g_.zet(x, y, z, None, op=LUCHT)
                        else:
                            g_.zet(x, y, z, "minecraft:sea_lantern" if (y - ETOP) == 3 and (x + z) % 3 == 0 else
                                   g_._kies([("minecraft:prismarine_bricks", 2), ("guhs:kaaskoraalblok", 1), ("minecraft:dark_prismarine", 1)], x, z, y))
                elif r <= 4.8:
                    g_.zet(x, ETOP + 5, z, "minecraft:dark_prismarine" if r > 1.5 else "minecraft:sea_lantern")
                    if max(abs(x - EX), abs(z - EZ)) <= 1:
                        for y in (ETOP + 3, ETOP + 4):
                            g_.zet(x, y, z, "minecraft:prismarine_bricks")
                if 4.8 < r <= 5.8 and (x + z) % 2 == 0 and not deur:
                    g_.kolom.append((x, z, 1, "guhs:nepkoraal[kleur=" + ("roze", "geel", "blauw", "oranje")[(x * 3 + z) % 4] + "]"))
    g.bouwwerk += [boei, schoorsteen]

    def deco(g_, rng):
        plafondlicht(g_, rng, "minecraft:sea_lantern", 0.05)
        plantjes(g_, rng, [("guhs:nepkoraal[kleur=roze]", 2), ("guhs:nepkoraal[kleur=geel]", 1), ("guhs:nepkoraal[kleur=blauw]", 1),
                           ("guhs:nepkoraal[kleur=oranje]", 1)], 0.16)
        # sea lanterns along the flooded tunnel walls + seagrass on its floor, so the way is easy to follow under water
        for gi, gg in enumerate(g_.gangen[:2]):
            P = gg["punten"]
            for (ax, az, aS), (bx, bz, bS) in zip(P[:-1], P[1:]):
                L = math.hypot(bx - ax, bz - az)
                for s_ in range(0, int(L), 3):
                    t = s_ / max(1, L)
                    x, z, S = ax + (bx - ax) * t, az + (bz - az) * t, aS + (bS - aS) * t
                    nx, nz = -(bz - az) / L, (bx - ax) / L
                    for kant in (-1, 1):
                        for d in range(2, 5):
                            xx, zz = round(x + nx * d * kant), round(z + nz * d * kant)
                            if g_.lees(xx, math.ceil(S) + 1, zz)[0] in (4, 5):
                                if s_ % 6 == 0:
                                    g_.zet(xx, math.ceil(S) + 1, zz, "minecraft:sea_lantern")
                                break
                    y = math.ceil(S)
                    if g_.lees(round(x), y, round(z))[1] == "minecraft:water" and s_ % 2 == 0:
                        g_.zet(round(x + nx), y, round(z + nz), "minecraft:seagrass")
        # sea pickles glowing on the pool bed
        for i in range(10):
            x, z = round(2233 + rng.uniform(-4, 4)), round(-589 + rng.uniform(-3, 3))
            for y in range(39, 43):
                if g_.lees(x, y, z)[1] == "minecraft:water" and g_.is_vast(x, y - 1, z):
                    g_.zet(x, y, z, "minecraft:sea_pickle[pickles=%d,waterlogged=true]" % int(rng.integers(2, 5)))
                    break
        for k, n in ((hal, 8), (elite, 4)):
            for i in range(n):
                a = rng.uniform(0, 2 * math.pi)
                r = rng.uniform(0.6, 0.88)
                x, z = round(k["cx"] + math.cos(a) * k["rx"] * r), round(k["cz"] + math.sin(a) * k["rz"] * r)
                if not g_.bezet_vrij(x, z, 1.5) and not g_.pad[x - g_.x0, z - g_.z0]:
                    stalagmiet(g_, rng, x, z, [("guhs:kaaskoraalblok", 2), ("minecraft:prismarine", 1)], int(rng.integers(2, 4)),
                               "guhs:nepkoraal[kleur=roze]")
        ring_palen(g_, hal, 0.9, 7, "minecraft:prismarine_wall", LANT, hoog=1)
        ring_palen(g_, puzzel, 0.85, 5, "minecraft:prismarine_wall", "minecraft:sea_lantern", hoog=1)
        lantaarnpalen(g_, "minecraft:prismarine_wall", LANT)
        schatnis(g_, nis, 2200, -622, 2202, -620, "minecraft:gold_block", "minecraft:cyan_carpet", "minecraft:sea_lantern")
    g.deco.append(deco)
    return g


# ================================================================================================ zone 5: De Wolkengrotten
def wolkengrot():
    g = Grot("wolkengrot", 5, zaad=505)
    g.thema = dict(
        vloer=[("guhs:wolkmarmer", 3), ("minecraft:snow_block", 2), ("minecraft:calcite", 1.5), ("guhs:wolkmarmer_gepolijst", 1)],
        plaat=[("guhs:wolkmarmer_gepolijst_plaat" + SLAB, 1)],
        vul=[("guhs:wolkenblok_wit", 2), ("minecraft:calcite", 1), ("minecraft:snow_block", 1)],
        muur=[("guhs:wolkenblok_wit", 4), ("minecraft:snow_block", 1.5), ("minecraft:calcite", 1), ("minecraft:white_wool", 1)],
        pilaar=[("guhs:wolkmarmer_zuil[axis=y]", 3), ("minecraft:pearlescent_froglight[axis=y]", 1)],
        trap_blok="guhs:wolkmarmer_gepolijst", trap_plaat="guhs:wolkmarmer_gepolijst_plaat" + SLAB, hek="minecraft:birch_fence")
    SX, SZ, TOP, BOT = 2492, -520, 209, 181
    trap_naar(g, SX, SZ, TOP + 0.5, BOT, 0.0)    # (arrives on a slab: half a step below the skin east of the hole)
    bodem = g.kamer("blaasgatbodem", 2493, -521, 7.5, 7.5, 181, 7)
    g.gang([(2497, -527, 181), (2503, -535, 181), (2506, -540, 181)])
    hal = g.kamer("wolkenhal", 2512, -548, 13, 11, 181, 12)
    g.gang([(2490, -514, 181), (2488, -504, 181), (2487, -499, 181)])
    puzzel = g.kamer("puzzelhal", 2486, -492, 8.5, 8.5, 181, 9, wobbel=0.06)
    g.gang([(2519, -542, 181), (2527, -533, 180.5), (2530, -525, 180), (2533, -517, 179.5), (2535, -512, 179)])
    elite = g.kamer("elitehol", 2537, -506, 9, 8, 179, 9)
    g.gang([(2543, -500, 179), (2547, -496, 179)])
    nis = g.kamer("schatnis", 2549, -494, 4.5, 4.5, 179, 5, hmin=3.5, wobbel=0.05)
    g.plekken = dict(ingang=(2498, TOP, -520), binnen=(2489, 181, -524), hal=(2512, 181, -548), puzzel=(2486, 177, -492),
                     elite=(2537, 179, -506), schat=(2549, 179, -494), amb_1=(2516, 178, -552), amb_2=(2506, 178, -544))
    g.vrij += [(2486, -492, 7.0), (2537, -506, 2.5)]

    def blaasgat(g_):
        """The blowhole on the whale's head: a fence ring with an opening to the east, cloud puffs round it."""
        for a in range(0, 360, 8):
            if a <= 30 or a >= 330:
                continue
            x, z = round(SX + math.cos(math.radians(a)) * 4.6), round(SZ + math.sin(math.radians(a)) * 4.6)
            g_.kolom.append((x, z, 1, "minecraft:birch_fence"))
        for a in range(10, 360, 45):
            x, z = round(SX + math.cos(math.radians(a)) * 6.5), round(SZ + math.sin(math.radians(a)) * 6.5)
            g_.kolom += [(x, z, 1, "guhs:wolkenblok_wit"), (x, z, 2, "guhs:wolkenblok_wit")]
        for (dx, dz) in ((6, -2), (6, 2)):
            g_.kolom += [(SX + dx, SZ + dz, 1, "minecraft:birch_fence"), (SX + dx, SZ + dz, 2, "minecraft:birch_fence"), (SX + dx, SZ + dz, 3, LANT)]
    g.bouwwerk.append(blaasgat)

    def deco(g_, rng):
        plafondlicht(g_, rng, "minecraft:pearlescent_froglight[axis=y]", 0.05)
        hangers(g_, rng, [("minecraft:end_rod[facing=down]", 1)], 0.03)
        plantjes(g_, rng, [("minecraft:white_carpet", 3), ("minecraft:snow[layers=1]", 2), ("minecraft:white_candle[candles=3,lit=true,waterlogged=false]", 0.5),
                           ("minecraft:lily_of_the_valley", 1)], 0.11)
        for k, n in ((hal, 10), (bodem, 3), (elite, 5)):
            for i in range(n):
                a = rng.uniform(0, 2 * math.pi)
                r = rng.uniform(0.55, 0.88)
                x, z = round(k["cx"] + math.cos(a) * k["rx"] * r), round(k["cz"] + math.sin(a) * k["rz"] * r)
                if not g_.bezet_vrij(x, z, 1.5) and not g_.pad[x - g_.x0, z - g_.z0]:
                    blok = [("guhs:wolkenblok_wit", 3), ("minecraft:white_wool", 1)] if k is not elite else \
                        [("minecraft:gray_wool", 2), ("minecraft:light_gray_wool", 1)]
                    stalagmiet(g_, rng, x, z, blok, int(rng.integers(1, 4)))
        # the den of the grey cloud devil: grey wisps on the walls
        for (x, y, z) in g_.plafondcellen(elite["i"]):
            if rng.random() < 0.3 and g_.lees(x, y + 1, z)[0] in (4, 5):
                g_.zet(x, y + 1, z, "minecraft:gray_wool")
        ring_palen(g_, hal, 0.9, 7, "minecraft:birch_fence", LANT)
        ring_palen(g_, puzzel, 0.82, 6, "guhs:wolkmarmer_zuil[axis=y]", "minecraft:pearlescent_froglight[axis=y]", hoog=2)
        lantaarnpalen(g_, "minecraft:birch_fence", LANT)
        schatnis(g_, nis, 2551, -493, 2549, -494, "guhs:wolkmarmer_gepolijst", "minecraft:light_blue_carpet",
                 "minecraft:pearlescent_froglight[axis=y]")
    g.deco.append(deco)
    return g


def alle():
    return [zwamgrot(), kristalgrot(), wortelgrot(), koraalgrot(), wolkengrot()]
