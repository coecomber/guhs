"""
bbq2 (guhrio-beloning) - what stands on the forecourt and in the tower room of the Kasteel van de Grote Nether-Mika.

guhrio_kasteel.py builds the castle and calls the two functions below with a guhrio_baan.Plekbouwer (its own little frame:
x from left to right when you look INTO the place from its entrance, z from the entrance inwards, y = 0 the floor's top
block). They only place blocks and entities; nothing here is imported at build time by anything but guhrio_beloning.py.

  voorplein(plek)    39 x 12, open air, the deck in front of the gatehouse (z = 0 is the balustrade over the sauce sea,
                     the path x 17..21 stays free). Left of the path: Pad-guh's paddenstoelenkraam (a big toadstool with a
                     striped valance: he sits on its counter), a baby toadstool, a row "brick ? brick" in the air and one
                     green pipe. Right of the path: the highscore board, the other green pipe of the pair and the
                     flagpole. Azalea bushes in the back corners.
  torenkamer(plek)   21 x 13 x 7, behind the duel: Prinses Perzikguh's room. A pink runner to her and her cake table, a
                     peach in wool in a golden frame on the wall behind it, her four-poster bed, a tea corner, a piano,
                     tapestries, books, and Guhshi's nest.

PLEKKEN: where Pad-guh, the princess and Guhshi are, in the plek's own frame; guhrio_beloning.py turns them into the
coordinates of the whole castle and checks them against GuhrioBeloningFeature (Bezetting brings the three back there).
"""
STENEN, ZWART, ORANJE, GOUD, LICHT = ("guhs:houtskoolsteen_stenen", "minecraft:polished_blackstone_bricks", "guhs:guhrio_siersteen",
                                      "minecraft:gold_block", "minecraft:shroomlight")
HOED, STIP, STEEL = "minecraft:red_mushroom_block", "minecraft:white_concrete", "minecraft:mushroom_stem"
VRAAGBLOK, MAST, PIJP, BORD = ("guhs:guhriobeloning_vraagblok", "guhs:guhriobeloning_vlaggenmast", "guhs:guhriobeloning_pijp",
                               "guhs:guhriobeloning_scorebord")
AIR = "minecraft:air"

# (x, y, z) in the plek's own frame: the cell the three stand in (feet at y)
PLEKKEN = {"padguh": ("voorplein", (7, 2, 3)), "perzikguh": ("torenkamer", (10, 1, 7)), "guhshi": ("torenkamer", (15, 1, 5))}
IDS = {"padguh": "guhriobeloning_padguh", "perzikguh": "guhriobeloning_perzikguh", "guhshi": "guhriobeloning_guhshi"}
# the two green pipes of the forecourt (their bottom block, in the plek's frame) and the flagpole's foot
PIJPEN = ((1, 1, 2), (24, 1, 3))
VLAGGENMAST = (36, 2, 3)
SCOREBORD = (30, 1, 4)


# =====================================================================================================================
# little tools
# =====================================================================================================================
def _leeg(plek, x, y, z):
    return plek.haal(x, y, z) in (None, AIR)


def _zet(plek, x, y, z, blok, props=None, nbt=None):
    """Only where the shell left air (the gatehouse's turrets stand in the back corners of the forecourt)."""
    if 0 <= x < plek.B and 0 <= z < plek.D and _leeg(plek, x, y, z):
        plek.zet(x, y, z, blok, props, nbt)


def _lantaarn(plek, x, y, z, hangt=False):
    plek.zet(x, y, z, "minecraft:lantern", {"hanging": "true" if hangt else "false", "waterlogged": "false"})


def _ketting(plek, x, y, z):
    plek.zet(x, y, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})


def _hek(plek, x, y, z, blok="minecraft:birch_fence"):
    plek.zet(x, y, z, blok, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})


def _plaat(plek, x, y, z, blok, soort="top"):
    plek.zet(x, y, z, blok, {"type": soort, "waterlogged": "false"})


def _trap(plek, x, y, z, blok, kijkt, half="bottom"):
    plek.zet(x, y, z, blok, {"facing": plek.kant(kijkt), "half": half, "shape": "straight", "waterlogged": "false"})


def _tapijt(plek, x0, z0, x1, z1, blok):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if _leeg(plek, x, 1, z) or plek.haal(x, 1, z).endswith("_carpet"):
                plek.zet(x, 1, z, blok)


def _mast(plek, x, y, z, hoog, kijkt="uit"):
    """A flagpole of the building block: its foot, the pole, the ball with the flag."""
    for k in range(hoog):
        deel = "los" if hoog == 1 else "voet" if k == 0 else "top" if k == hoog - 1 else "paal"
        plek.zet(x, y + k, z, MAST, {"deel": deel, "facing": plek.kant(kijkt)})


def _pijp(plek, x, y, z, hoog=2, kleur="green"):
    """A green pipe of the building block (the one that really works): hoog blocks, the top one is the mouth."""
    for k in range(hoog):
        plek.zet(x, y + k, z, PIJP, {"kleur": kleur, "mond": "true" if k == hoog - 1 else "false"})


def _paddenstoel(plek, cx, cz, y, steel, lagen, stippen=(), licht=()):
    """A toadstool: a stem (cells) of `steel` blocks high and a red cap of elliptic layers [(rx, rz), ...] on it."""
    for (x, z) in steel[0]:
        for k in range(steel[1]):
            _zet(plek, x, y + k, z, STEEL)
    top = y + steel[1]
    for i, (rx, rz) in enumerate(lagen):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                if ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2 <= 1.0:
                    blok = STIP if (x, top + i, z) in stippen else LICHT if (x, top + i, z) in licht else HOED
                    _zet(plek, x, top + i, z, blok)


# =====================================================================================================================
# the forecourt
# =====================================================================================================================
def kraam(plek):
    """Pad-guh's paddenstoelenkraam: a toadstool as big as a market stall. Under the front of its cap hangs a striped
    valance over the counter he sits on; his wares stand next to him."""
    from features import wereld
    # the cap: four layers on a stem of five, white spots on top, lights in its underside over the counter
    stippen = {(3, 6, 4), (4, 6, 4), (11, 6, 6), (10, 6, 3), (7, 6, 7), (2, 6, 5), (5, 7, 3), (6, 7, 3), (9, 7, 6), (10, 7, 5), (4, 7, 6),
               (6, 8, 4), (9, 8, 5), (8, 8, 4), (7, 9, 5)}
    licht = {(5, 6, 4), (9, 6, 4), (7, 6, 4)}
    _paddenstoel(plek, 7, 5, 1, ([(x, z) for x in (6, 7, 8) for z in (5, 6)], 5), [(5.2, 2.9), (4.3, 2.4), (3.1, 1.7), (1.7, 0.9)], stippen, licht)
    # the valance: a red and white striped strip under the cap's front edge, on two posts
    for x in range(3, 12):
        plek.zet(x, 5, 3, "minecraft:red_wool" if x % 2 else "minecraft:white_wool")
    for x in (3, 11):
        for y in range(1, 5):
            _hek(plek, x, y, 3)
    # the counter between the posts: white and red blocks, the wares on it
    for x in range(4, 11):
        plek.zet(x, 1, 3, "minecraft:smooth_quartz" if x % 2 == 0 else "minecraft:red_terracotta")
    for x in (4, 5, 9, 10):                                       # his stock behind the counter
        plek.zet(x, 1, 5, "minecraft:barrel", {"facing": "up", "open": "false"})
    plek.zet(4, 2, 5, "minecraft:barrel", {"facing": plek.kant("uit"), "open": "false"})
    plek.zet(4, 2, 3, "minecraft:potted_red_mushroom")
    _mast(plek, 5, 2, 3, 1)                                       # a mini flagpole
    plek.zet(9, 2, 3, VRAAGBLOK)                                  # his free sample: one knabbel a day
    plek.zet(10, 2, 3, "minecraft:potted_brown_mushroom")
    for x in (5, 9):                                              # lanterns under the valance
        _lantaarn(plek, x, 4, 2, hangt=True)
        plek.zet(x, 5, 2, "minecraft:red_wool" if x % 2 else "minecraft:white_wool")
    x, y, z = PLEKKEN["padguh"][1]
    plek.entity(x, y, z, wereld.npc(plek.h, "padguh", IDS["padguh"], None, plek.yaw("uit")))
    plek.zet(7, 5, 2, "minecraft:red_wool")
    plek.bord(7, 5, 1, "uit", ["Pad-guh's kraam", "Outfits en", "bouwblokken", "voor munten, njeg"], "guhrio_beloning.kraam")
    # a baby toadstool next to it
    _paddenstoel(plek, 14, 6, 1, ([(14, 6)], 2), [(1.5, 1.5), (0.6, 0.6)], {(13, 3, 6), (14, 4, 6), (15, 3, 7)})


def struik(plek, x0, x1, z0, z1):
    """A round bush of azalea leaves against the back wall (the green hills of the old games, as far as a deck allows)."""
    blad = {"distance": "7", "persistent": "true", "waterlogged": "false"}
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            hoog = 2 if x0 < x < x1 and z > z0 else 1
            for y in range(1, 1 + hoog):
                bloei = (x * 7 + z * 3 + y) % 4 == 0
                _zet(plek, x, y, z, "minecraft:flowering_azalea_leaves" if bloei else "minecraft:azalea_leaves", blad)


def proefblokken(plek):
    """The building blocks at work: "brick ? brick" in the air (jump against it), and the pair of green pipes."""
    for x, blok in ((13, ORANJE), (14, VRAAGBLOK), (15, ORANJE)):
        plek.zet(x, 4, 2, blok)
    # the pair: one in the far left corner by the balustrade, one right of the path (a step next to each)
    (ax, ay, az), (bx, by, bz) = PIJPEN
    _pijp(plek, ax, ay, az)
    plek.zet(ax, 1, az + 1, "guhs:guhrio_blok")
    _pijp(plek, bx, by, bz)
    plek.zet(bx + 1, 1, bz, "guhs:guhrio_blok")
    plek.bord(bx + 1, 1, bz - 1, "uit", ["Groene pijp", "Ga erop staan", "en sluip (Shift)", "Njeg!"], "guhrio_beloning.pijp")
    # the flagpole on a block, in the right corner
    mx, my, mz = VLAGGENMAST
    plek.zet(mx, my - 1, mz, "guhs:guhrio_blok")
    _mast(plek, mx, my, mz, 7)
    # the welcome sign on a post at the path
    for y in (1, 2):
        plek.zet(16, y, 3, ORANJE)
    plek.bord(16, 2, 2, "uit", ["Welkom bij het", "Kasteel van de", "Grote Nether-Mika", "Njeg njeg njeg!"], "guhrio_beloning.welkom")
    # bushes in the back corners and beside the gate
    struik(plek, 1, 5, 9, 11)
    struik(plek, 33, 37, 9, 11)
    struik(plek, 15, 16, 10, 11)
    struik(plek, 22, 23, 10, 11)


def scorebord(plek):
    """The highscore board: a black panel between two orange pillars under a golden beam; the block that shows the times
    stands in front of it (its board floats above the block, against the panel)."""
    z = 6
    for x in range(26, 35):
        plek.zet(x, 1, z, ZWART)
        plek.zet(x, 7, z, GOUD)
        for y in range(2, 7):
            plek.zet(x, y, z, ORANJE if x in (26, 34) else "minecraft:black_concrete")
    for x in (26, 30, 34):                                        # a crown on the beam: two merlons and a golden tip
        plek.zet(x, 8, z, GOUD if x == 30 else ORANJE)
    plek.zet(30, 9, z, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    sx, sy, sz = SCOREBORD
    plek.zet(sx, sy, sz, BORD, {"facing": plek.kant("uit")}, {"id": "guhs:guhriobeloning_scorebord"})
    for x in (sx - 2, sx + 2):                                    # a lantern on a low wall post left and right of the block
        plek.zet(x, 1, sz, "minecraft:polished_blackstone_brick_wall",
                 {"up": "true", "north": "none", "east": "none", "south": "none", "west": "none", "waterlogged": "false"})
        _lantaarn(plek, x, 2, sz)
    plek.bord(26, 3, 5, "uit", ["Highscorebord", "Klik op het", "gouden blok voor", "jouw eigen tijden"], "guhrio_beloning.bord")


def voorplein(plek):
    kraam(plek)
    proefblokken(plek)
    scorebord(plek)


# =====================================================================================================================
# the tower room
# =====================================================================================================================
# a peach (rows from the top; the canvas is 9 wide and 5 high)
PERZIK = ["...BGG...",
          "..PPPGO..",
          ".PWPPPOO.",
          ".PPPPPOO.",
          "..PPPOO.."]
PERZIK_KLEUR = {"P": "minecraft:pink_wool", "O": "minecraft:orange_wool", "W": "minecraft:white_wool", "G": "minecraft:lime_wool",
                "B": "minecraft:brown_wool"}


def _wandkleed(plek, x, z0, kleur):
    """A tapestry against a side wall: two wide, from above the plinth to under the ceiling, with a golden bar."""
    for z in (z0, z0 + 1):
        plek.zet(x, 7, z, GOUD)
        for y in range(3, 7):
            plek.zet(x, y, z, kleur if y != 4 else "minecraft:white_wool")
    plek.zet(x, 2, z0, kleur)                                     # a swallow tail


def torenkamer(plek):
    from features import wereld
    h = plek.h
    # the runner from the door to the princess, a rug with a border under the cake table
    _tapijt(plek, 9, 0, 11, 5, "minecraft:pink_carpet")
    _tapijt(plek, 7, 6, 13, 10, "minecraft:magenta_carpet")
    _tapijt(plek, 8, 6, 12, 9, "minecraft:pink_carpet")
    # the cake table behind her: a quartz table top with three cakes (somebody already had a piece)
    for x in range(8, 13):
        _plaat(plek, x, 1, 9, "minecraft:smooth_quartz_slab")
    plek.zet(9, 2, 9, "minecraft:cake", {"bites": "2"})
    plek.zet(10, 2, 9, "minecraft:pink_candle_cake", {"lit": "true"})
    plek.zet(11, 2, 9, "minecraft:cake", {"bites": "0"})
    plek.zet(8, 2, 9, "minecraft:potted_pink_tulip")
    plek.zet(12, 2, 9, "minecraft:potted_pink_tulip")
    # a peach in wool on the back wall: a white canvas in a golden frame on a quartz plinth, a pillar with a light at each side
    for x in range(5, 16):
        for y in range(1, 8):
            rand = x in (5, 15) or y in (2, 7)
            plek.zet(x, y, 12, "minecraft:quartz_bricks" if y == 1 else GOUD if rand else "minecraft:white_wool")
    for r, rij in enumerate(PERZIK):
        for c, ch in enumerate(rij):
            if ch in PERZIK_KLEUR:
                plek.zet(6 + c, 6 - r, 12, PERZIK_KLEUR[ch])
    for x in (4, 16):
        for y in range(1, 8):
            if y == 4:
                plek.zet(x, y, 12, LICHT)
            else:
                plek.zet(x, y, 12, "minecraft:quartz_pillar", {"axis": "y"})
    # the princess, and the sign at the door
    x, y, z = PLEKKEN["perzikguh"][1]
    plek.entity(x, y, z, wereld.npc(h, "perzikguh", IDS["perzikguh"], None, plek.yaw("uit")))
    for x in (7, 13):                                             # two pedestals with tulips beside the door
        plek.zet(x, 1, 1, "minecraft:quartz_pillar", {"axis": "y"})
        plek.zet(x, 2, 1, "minecraft:chiseled_quartz_block")
        plek.zet(x, 3, 1, "minecraft:potted_pink_tulip")
    plek.bord(13, 2, 0, "uit", ["Torenkamer van", "Prinses Perzikguh", "Taart: zelf", "opscheppen, njeg"], "guhrio_beloning.torenkamer")
    # chandeliers: under the middle of the ceiling, and one over each side of the room
    for x, z in ((10, 4), (4, 7), (16, 7)):
        _ketting(plek, x, 7, z)
        _lantaarn(plek, x, 6, z, hangt=True)
    # tapestries along both side walls
    for z0, kleur in ((2, "minecraft:pink_wool"), (6, "minecraft:magenta_wool")):
        _wandkleed(plek, 0, z0, kleur)
        _wandkleed(plek, 20, z0, kleur)
    # her four-poster bed in the back left corner: pink, with a canopy
    for part, z in (("foot", 10), ("head", 11)):
        plek.zet(2, 1, z, "minecraft:pink_bed", {"facing": plek.kant("in"), "part": part, "occupied": "false"}, {"id": "minecraft:bed"})
    for x in (1, 3):
        for z in (9, 12):
            for y in (1, 2, 3):
                _hek(plek, x, y, z)
    for x in range(1, 4):
        for z in range(9, 13):
            plek.zet(x, 4, z, "minecraft:white_wool" if (x + z) % 2 else "minecraft:pink_wool")
    plek.zet(0, 1, 11, "minecraft:bookshelf")
    _lantaarn(plek, 0, 2, 11)
    # the tea corner at the left wall: a little table and two quartz chairs on a white rug
    _tapijt(plek, 2, 3, 6, 5, "minecraft:white_carpet")
    _hek(plek, 4, 1, 4)
    plek.zet(4, 2, 4, "minecraft:pink_carpet")
    _trap(plek, 3, 1, 4, "minecraft:quartz_stairs", "links")
    _trap(plek, 5, 1, 4, "minecraft:quartz_stairs", "rechts")
    plek.zet(1, 1, 1, "minecraft:flowering_azalea")
    plek.zet(19, 1, 1, "minecraft:flowering_azalea")
    # her piano at the front right: black with white keys, a stool in front of it
    for x in (16, 17, 18):
        plek.zet(x, 1, 0, "minecraft:polished_blackstone")
        plek.zet(x, 2, 0, "minecraft:polished_blackstone")
        _plaat(plek, x, 1, 1, "minecraft:smooth_quartz_slab")
    plek.zet(17, 3, 0, "minecraft:note_block", {"instrument": "harp", "note": "12", "powered": "false"})
    _trap(plek, 17, 1, 3, "minecraft:quartz_stairs", "in")
    # books along the back of the right wall
    for z in range(9, 13):
        for y in (1, 2, 3):
            if (y + z) % 3:
                plek.zet(19, y, z, "minecraft:bookshelf")
            else:
                plek.zet(19, y, z, "minecraft:chiseled_bookshelf",
                         {"facing": plek.kant("links"), **{f"slot_{i}_occupied": "true" if i % 2 else "false" for i in range(6)}})
    _lantaarn(plek, 19, 4, 10)
    plek.zet(18, 1, 12, "minecraft:flowering_azalea")
    # Guhshi's nest right of the runner: a green rug, hay, a trough and a block of knabbels
    _tapijt(plek, 13, 3, 17, 7, "minecraft:green_carpet")
    _tapijt(plek, 14, 4, 16, 6, "minecraft:lime_carpet")
    plek.zet(18, 1, 4, "minecraft:hay_block", {"axis": "y"})
    plek.zet(18, 1, 5, "minecraft:hay_block", {"axis": "x"})
    plek.zet(18, 2, 4, "minecraft:hay_block", {"axis": "z"})
    plek.zet(18, 1, 6, "minecraft:composter", {"level": "6"})
    plek.zet(18, 1, 7, "guhs:block_of_kaasknabbels")
    x, y, z = PLEKKEN["guhshi"][1]
    plek.entity(x, y, z, {"id": "guhs:guh", "Variant": "guhshi", "PersistenceRequired": h.ms.Byte(1), "Invulnerable": h.ms.Byte(1),
                          "attributes": h.ms.compounds([{"id": "minecraft:scale", "base": h.ms.Double(1.25)}]),
                          "NeoForgeData": {"guhs_verhaal_guh": "guhshi", "guhs_bezetting": IDS["guhshi"]},
                          "Rotation": h.ms.floats(plek.yaw("uit"), 0.0)})
