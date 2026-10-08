"""
biomes3 slice "bouw-meer": the two buildings as templates (not in FEATURES; bio_bouw_meer.py calls build()).

Both are guhs:bio_plek structures: the start jigsaw lands IN the top ground block of the spot and the template's north
side (low z) is turned to what the spot looks at. Only the blocks set here are placed; everything else of the box stays
as the landscape made it (water under the jetty, the island's own tree).

  botenhuisje (spot meer_oever: a dry shore block one above the water, open lake to the north)
      x 0..13, y 0..16, z 0..21.  The deck is at y JY = 8 (the spot's ground level), eight layers of posts under it.
      z 21      the landing: a row of planks in the shore, the jigsaw in its middle (8, JY, 21)
      x 3..6    the jetty, from the landing (z 20) out to its end (z 4..5, one wider to the west): 17 blocks
      x 7..9    the berth: open water under the roof (z 11..18), the lake's guaranteed water line is x 8
      x 5..12   the roof (z 10..21) on posts: open to the jetty and to the lake, a railing to the east, a wall to the south
      (6, JY, 15)   de meerpaal, the anchor: MEERPAAL. Java (MeerpaalBlock) knows the berth, the visser-guh and the
                    lantern as steps from it: LIGPLAATS, VISSER, LANTAARN below are the same numbers.

  picknickeilandje (spot meer_boom: three blocks from the big tree of a large island, the tree to the north)
      x 0..12, y 0..9, z 0..10.  The rug is IN the ground at y 0 (x 4..8, z 3..6, round the trunk), the jigsaw at
      (6, 0, 6); the tree's trunk stands at (6, 1.., 3). (7, 1, 5) is de picknickmand, the anchor: MAND. Java (MandBlock) knows the three
      seats as steps from it: ZITPLEKKEN. No tree and no lanterns in the template: the lanterns are hung in whatever tree
      the island has when a player first comes near (MandBlock.versier).

steiger() and picknick() build into any Structure at an offset, so the game test templates (a pond, an island) hold the
very same buildings.
"""
from features import bio_lib as lib

JY = 8                                   # botenhuisje: the deck / the spot's ground layer
HUIS_NOORD = 11                          # botenhuisje: the north row of the house (its south wall is nine further)
MEERPAAL = (6, JY, HUIS_NOORD + 4)
LIGPLAATS = (2, 0, 0)                    # steps from the meerpaal: east, up, south
VISSER = (-2.0, 1.0, -11.0)
LANTAARN = (-4, 2, -11)
STEIGER_MAAT = (14, JY + 9, HUIS_NOORD + 11)
STEIGER_ANKER = (8, JY, HUIS_NOORD + 10)

MAND = (7, 1, 5)
ZITPLEKKEN = [("bloesem", -2.0, 0.0, 1.0, 0, "hanami_bloesemguh"), ("guh", 0.0, 0.0, 1.0, 0, "hanami_guh"),
              ("slaper", -3.0, 0.0, -1.0, 1, "hanami_guh_slaapt")]
PICKNICK_MAAT = (13, 10, 11)
PICKNICK_ANKER = (6, 0, 6)
STAM = (6, 3)                            # where the island's tree stands (x, z): three north of the jigsaw
# (five wide: on the smallest island with the most ragged outline the flat ground is 4.7 blocks round the middle, and the
# rug's far corners lie 4.3 from it; two wider and a corner hung over the beach)
KLEED = [(x, z) for x in range(4, 9) for z in range(3, 7) if (x, z) != STAM]
BIJ_STAM = [(x, z) for x in range(5, 8) for z in range(2, 5)]

LUCHT = "minecraft:air"


def _blokken(h):
    b = lambda bid, standin: lib.blok(h, bid, standin)   # noqa: E731
    return {
        "plank": "guhs:bleekhout_planken", "plaat": "guhs:bleekhout_plaat", "stam": "guhs:bleekhout_stam", "paal": "guhs:bleekhout_gestript",
        "hek": "guhs:bleekhout_hek",
        "balk": "guhs:guhbloesem_planks",
        "dak": b("guh_dakpan_roze_trap", "minecraft:cherry_stairs"), "nok": b("guh_dakpan_roze_plaat", "minecraft:cherry_slab"),
        "shoji": b("shoji", "minecraft:white_stained_glass"),
    }


def _anker(s, x, y, z, naam, final):
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": f"guhs:{naam}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def _npc(h, s, x, y, z, kind, yaw, naam_zichtbaar=True):
    ms = h.ms
    nbt = {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(yaw, 0.0)}
    if not naam_zichtbaar:
        nbt["CustomNameVisible"] = ms.Byte(0)
    s.entity(x, float(y), z, nbt)


HEK_LOS = {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"}


def steiger(h, s, ox=0, oy=0, oz=0, anker=True, visser=True):
    """The botenhuisje into s at this offset. Returns the cells (x, z) that hold a deck."""
    B = _blokken(h)
    top = {"type": "top", "waterlogged": "false"}
    N, Z, L = HUIS_NOORD, HUIS_NOORD + 9, HUIS_NOORD + 10      # the house's north and south row, the landing

    def zet(x, y, z, naam, props=None, nbt=None):
        s.set(ox + x, oy + y, oz + z, naam, props, nbt)

    dek = set()
    for z in range(6, L):                                      # the jetty
        for x in range(3, 7):
            dek.add((x, z))
    for z in (4, 5):                                           # its end, one wider
        for x in range(2, 7):
            dek.add((x, z))
    for z in range(N, L):                                      # under the roof: the east walk and the south deck
        dek.add((10, z))
        dek.add((11, z))
    for z in (Z - 1, Z):
        for x in range(7, 10):
            dek.add((x, z))
    berth = {(x, z) for x in range(7, 10) for z in range(N, Z - 1)}
    uitvaart = {(x, z) for x in range(7, 10) for z in range(1, N)}

    for (x, z) in sorted(dek):
        zet(x, JY, z, B["plaat"], top)
        for y in range(JY + 1, JY + 4):
            zet(x, y, z, LUCHT)
    for (x, z) in sorted(berth | uitvaart):                    # nothing floats in the boat's way (lily pads, petals)
        zet(x, JY, z, LUCHT)
    for (x, z) in sorted(berth):
        for y in range(JY + 1, JY + 3):
            zet(x, y, z, LUCHT)
    for x in range(3, 11):                                     # the landing: planks in the shore
        zet(x, JY, L, B["plank"])
        for y in range(JY + 1, JY + 4):
            zet(x, y, L, LUCHT)
    if anker:
        _anker(s, ox + STEIGER_ANKER[0], oy + STEIGER_ANKER[1], oz + STEIGER_ANKER[2], "botenhuisje", B["plank"])

    # the posts under the deck, down to the bottom of the lake
    for (x, z) in ((3, 8), (3, N + 2), (3, Z), (6, 8), (6, N), (6, MEERPAAL[2]), (6, Z), (2, 4), (6, 4), (11, N), (11, N + 3), (11, N + 6), (11, Z)):
        for y in range(0, JY):
            zet(x, y, z, B["stam"], {"axis": "y"})

    # the house: posts, blossom-wood beams, a railing to the east, a wall with two paper windows to the south
    for (x, z) in ((6, N), (6, Z), (11, N), (11, N + 3), (11, N + 6), (11, Z)):
        for y in (JY + 1, JY + 2):
            zet(x, y, z, B["paal"], {"axis": "y"})
    for z in range(N, Z + 1):
        zet(6, JY + 3, z, B["balk"])
        zet(11, JY + 3, z, B["balk"])
    for x in range(7, 11):
        zet(x, JY + 3, N, B["balk"])
        zet(x, JY + 3, Z, B["balk"])
    for z in (N + 1, N + 2, N + 4, N + 5, N + 7, N + 8):
        zet(11, JY + 1, z, B["hek"], HEK_LOS)
    for x in range(7, 11):
        zet(x, JY + 1, Z, B["plank"])
        if x in (8, 9) and "shoji" in B["shoji"]:
            zet(x, JY + 2, Z, B["shoji"], {"facing": "north", "hinge": "left" if x == 8 else "right", "open": "false"})
        elif x in (8, 9):
            zet(x, JY + 2, Z, B["shoji"])
        else:
            zet(x, JY + 2, Z, B["plank"])

    # the roof: pink tiles, the ridge along z; gables of planks
    RY = JY + 4
    helling = {5: (RY, "east"), 6: (RY + 1, "east"), 7: (RY + 2, "east"), 10: (RY + 2, "west"), 11: (RY + 1, "west"), 12: (RY, "west")}
    for z in range(N - 1, L + 1):
        for x, (y, kant) in helling.items():
            zet(x, y, z, B["dak"], {"facing": kant, "half": "bottom", "shape": "straight", "waterlogged": "false"})
        for x in (8, 9):
            zet(x, RY + 3, z, B["nok"], {"type": "bottom", "waterlogged": "false"})
    for z in (N, Z):
        for x in range(6, 12):
            zet(x, RY, z, B["plank"])
        for x in range(7, 11):
            zet(x, RY + 1, z, B["plank"])
        for x in (8, 9):
            zet(x, RY + 2, z, B["plank"])

    # inside: a paper lantern under the ridge, the fishing gear against the south wall
    zet(8, RY + 2, N + 4, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
    zet(7, JY + 1, Z - 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    zet(8, JY + 1, Z - 1, "guhs:botenhuisje_hengelstandaard", {"facing": "north"})
    zet(9, JY + 1, Z - 1, "guhs:botenhuisje_hengelstandaard", {"facing": "west"})
    zet(10, JY + 1, Z - 1, "minecraft:composter", {"level": "3"})

    # the anchor, the end of the jetty with its lantern, the windsock at the landing
    zet(MEERPAAL[0], MEERPAAL[1], MEERPAAL[2], "guhs:botenhuisje_meerpaal", {"facing": "north"})
    lx, ly, lz = MEERPAAL[0] + LANTAARN[0], MEERPAAL[1] + LANTAARN[1], MEERPAAL[2] + LANTAARN[2]
    zet(lx, ly - 1, lz, B["hek"], HEK_LOS)
    zet(lx, ly, lz, "guhs:botenhuisje_steigerlantaarn", {"lit": "false"})
    zet(3, JY + 1, 4, "guhs:botenhuisje_hengelstandaard", {"facing": "north"})
    for y in (JY + 1, JY + 2):
        zet(3, y, L, B["hek"], HEK_LOS)
    zet(3, JY + 3, L, "guhs:botenhuisje_koiwindzak", {"facing": "east"})
    if visser:
        _npc(h, s, ox + MEERPAAL[0] + 0.5 + VISSER[0], oy + MEERPAAL[1] + VISSER[1], oz + MEERPAAL[2] + 0.5 + VISSER[2], "botenhuisje_visserguh", 180.0)
    return dek


def picknick(h, s, ox=0, oy=0, oz=0, anker=True, guhs=True):
    """The picknick into s at this offset (the rug at y oy, in the ground). Nothing is cleared or put right beside the
    trunk: a thick-footed tree keeps its foot."""
    def zet(x, y, z, naam, props=None, nbt=None):
        s.set(ox + x, oy + y, oz + z, naam, props, nbt)

    for (x, z) in KLEED:
        zet(x, 0, z, "minecraft:light_blue_wool" if (x + z) % 2 == 0 else "minecraft:white_wool")
        if (x, z) not in BIJ_STAM:
            for y in (1, 2):
                zet(x, y, z, LUCHT)
    if anker:
        _anker(s, ox + PICKNICK_ANKER[0], oy + PICKNICK_ANKER[1], oz + PICKNICK_ANKER[2], "picknickeilandje",
               "minecraft:light_blue_wool" if (PICKNICK_ANKER[0] + PICKNICK_ANKER[2]) % 2 == 0 else "minecraft:white_wool")
    zet(MAND[0], MAND[1], MAND[2], "guhs:picknickeilandje_mand", {"facing": "north", "versierd": "false"})
    zet(6, 1, 5, "guhs:guh_taart", {"bites": "1"})
    zet(5, 1, 5, "guhs:theepotje", {"facing": "south"})
    zet(4, 1, 5, "guhs:roze_guh_koek", {"facing": "east", "koeken": "3"})
    zet(8, 1, 5, "guhs:roze_guh_koek", {"facing": "north", "koeken": "2"})
    zet(8, 1, 4, "guhs:pink_kussen", {"facing": "west"})
    zet(4, 1, 6, "guhs:white_kussen", {"facing": "north"})
    zet(8, 1, 6, "guhs:pink_kussen", {"facing": "north"})
    zet(8, 1, 3, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    zet(4, 1, 3, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    if guhs:
        for (_naam, oost, op, zuid, draai, kind) in ZITPLEKKEN:
            _npc(h, s, ox + MAND[0] + 0.5 + oost, oy + MAND[1] + op, oz + MAND[2] + 0.5 + zuid, kind, 180.0 + 90.0 * draai, naam_zichtbaar=False)


def boom(h, s, x, y, z):
    """A stand-in for the island's own big tree (the shape of the landscape's placeholder): a trunk of six, a round crown."""
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(-2, 3):
                if dx * dx + dz * dz + dy * dy * 2 <= 11:
                    s.set(x + dx, y + 6 + dy, z + dz, "guhs:guhbloesem_leaves", {"distance": "7", "persistent": "true", "waterlogged": "false"})
    for dy in range(6):
        s.set(x, y + dy, z, "guhs:guhbloesem_log", {"axis": "y"})


def test_meer(h):
    """botenhuisje_test_meer (20 x 18 x 25): a shore (z 21..24, grass at y JY) and a lake four deep north of it, with the
    botenhuisje at (3, 0, 0) exactly as the structure places it: the water's top block is one below the deck."""
    s = h.Structure((20, JY + 10, 25))
    for x in range(20):
        for z in range(25):
            land = z >= STEIGER_ANKER[2]
            for y in range(0, JY + 1):
                if land:
                    s.set(x, y, z, "minecraft:grass_block" if y == JY else "minecraft:dirt", {"snowy": "false"} if y == JY else {})
                elif y <= 3:
                    s.set(x, y, z, "minecraft:sand")
                elif y < JY:
                    s.set(x, y, z, "minecraft:water", {"level": "0"})
    steiger(h, s, 3, 0, 0, anker=False)
    s.save("botenhuisje_test_meer")


def test_eiland(h):
    """botenhuisje_test_eiland (34 x 8 x 30): water two deep (top block y 2) with a large island (13 x 11, grass at y 3),
    a small one (4 x 4) and a strip of mainland along the south side (z 26..29)."""
    s = h.Structure((34, 8, 30))
    for x in range(34):
        for z in range(30):
            groot = 5 <= x <= 17 and 6 <= z <= 16
            klein = 24 <= x <= 27 and 8 <= z <= 11
            land = z >= 26
            s.set(x, 0, z, "minecraft:sand")
            for y in (1, 2, 3):
                if groot or klein or land:
                    s.set(x, y, z, "minecraft:grass_block" if y == 3 else "minecraft:dirt", {"snowy": "false"} if y == 3 else {})
                elif y < 3:
                    s.set(x, y, z, "minecraft:water", {"level": "0"})
    s.save("botenhuisje_test_eiland")


def test_picknick(h):
    """picknickeilandje_test_eiland (15 x 14 x 13): flat grass (y 0) with the stand-in tree and the picnic one block
    in from the edge, exactly as the structure places it (the trunk three north of the jigsaw)."""
    s = h.Structure((15, 14, 13))
    for x in range(15):
        for z in range(13):
            s.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    boom(h, s, 1 + PICKNICK_ANKER[0], 1, 1 + PICKNICK_ANKER[2] - 3)
    picknick(h, s, 1, 0, 1, anker=False)
    s.save("picknickeilandje_test_eiland")


def build(h):
    s = h.Structure(STEIGER_MAAT)
    steiger(h, s)
    s.save("botenhuisje")
    s = h.Structure(PICKNICK_MAAT)
    picknick(h, s)
    s.save("picknickeilandje")
    test_meer(h)
    test_eiland(h)
    test_picknick(h)


def check(h):
    """The numbers Java repeats (MeerpaalBlock, MandBlock, BouwMeerEvents) are these."""
    import os
    import re
    problems = []
    J = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bio", "bouwmeer")

    def java(naam):
        return open(os.path.join(J, naam), encoding="utf-8").read()

    def getallen(tekst):
        return [float(v) for v in re.findall(r"-?\d+(?:\.\d+)?", tekst)]

    paal = java("MeerpaalBlock.java")
    for naam, wil in (("LIGPLAATS", LIGPLAATS), ("VISSER", VISSER), ("LANTAARN", LANTAARN)):
        m = re.search(naam + r" = \{([^}]*)\}", paal)
        if not m or getallen(m.group(1)) != [float(v) for v in wil]:
            problems.append(f"MeerpaalBlock.{naam} is not {wil}")
    events = java("BouwMeerEvents.java")
    for naam, wil in (("MEERPAAL_LOKAAL", MEERPAAL), ("MAND_LOKAAL", MAND)):
        m = re.search(naam + r" = new BlockPos\(([^)]*)\)", events)
        if not m or getallen(m.group(1)) != [float(v) for v in wil]:
            problems.append(f"BouwMeerEvents.{naam} is not {wil}")
    mand = java("MandBlock.java")
    for (naam, oost, op, zuid, draai, _kind) in ZITPLEKKEN:
        m = re.search(r'new Zitplek\("' + naam + r'", ([^,]*), ([^,]*), ([^,]*), (\d+),', mand)
        if not m or [float(g) for g in m.groups()] != [oost, op, zuid, float(draai)]:
            problems.append(f"MandBlock.ZITPLEKKEN {naam} is not {(oost, op, zuid, draai)}")
    return problems
