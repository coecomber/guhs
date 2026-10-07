"""
bbq2 (guhrio-w2): world 2 of Super Guhrio, "de kelders" of the Kasteel van de Grote Nether-Mika. Java: feature/guhriow2.

  LEVELS      the two lanes, built with the engine's lane builder (guhrio_baan.Baanbouwer; guhrio_kasteel.py plugs them into
              the west and east cellar of the castle and writes data/guhs/guhrio_level/kasteel_2_1.json / kasteel_2_2.json):
                2-1  De buizenkelder: green pipes with Hapbloemen, a hidden coin cellar, a secret pipe, two platforms over
                     the kaassaus, a Schild-Mika whose shell must be kicked into a switch (the vadsmunt in the red cage),
                     falling blocks, a lift to a high ledge
                2-2  Het nest van Guhshi: the egg in the eierkamertje behind a door, the egg gate (it only opens for who
                     carries the egg), the broedgang where Guhshi hatches (a cutscene), a first ride over a wide gap, and a
                     pipe to the warp room (three warp pipes: to 3-1, to 3-2 and back to 1-1)
              Both follow one plan: rows 0..11 are the main lane (ground top row 3, a brick ceiling at row 11), rows 12..19
              hold the rooms (their own lanes: a floor at row 12, a ceiling at row 19).
  blocks      guhriow2_keldergrond, guhriow2_keldersteen (plain masonry), guhriow2_warppijp (kanaal 13 / 14 / 15 = where it
              leads, see GuhrioW2.WARP) on guhriow2_warpbuis (axis), guhriow2_eislot (kanaal = the gate's switch channel),
              guhriow2_nest, guhriow2_broedplek (invisible)
  texts       the blocks, the messages gui.guhs.guhriow2.*, the scene guhriow2_uit, two visible and three hidden advancements
  ftb(fq)     the quests of the section guhrio_w2
  test room   guhriow2_test_kamer (24 x 12 x 7, empty) for GuhrioW2GameTests

Every level is checked while it is built (guhrio_w2_check.py: can a careful player reach the flagpole, every coin, the
vadsmunten, the egg, the nest and the warp pipes; does nobody get stuck; does a big player fit). A level that fails stops
the generators. Textures: guhrio_w2_tex.py. Wiki texts: guhrio_w2_wiki.py.
"""
import os

from features import guhrio_baan as gb
from features import guhrio_w2_check
from features import guhrio_w2_tex

# the section of chapter guhs_guhrio this world's quests land in (CONTRACT_130 5.5 / 8)
FTB_SECTIES = [("guhrio_w2", "Wereld 2: de kelders", "guh:guhshi", None)]

KG, KS = "guhs:guhriow2_keldergrond", "guhs:guhriow2_keldersteen"
WARPPIJP, WARPBUIS, EISLOT, NEST, BROEDPLEK = ("guhs:guhriow2_warppijp", "guhs:guhriow2_warpbuis", "guhs:guhriow2_eislot",
                                              "guhs:guhriow2_nest", "guhs:guhriow2_broedplek")
BLOKKEN = ("guhriow2_keldergrond", "guhriow2_keldersteen", "guhriow2_warpbuis", "guhriow2_warppijp", "guhriow2_eislot", "guhriow2_nest",
           "guhriow2_broedplek")
TESTKAMER = "guhriow2_test_kamer"

G, Y = 3, 4                  # the ground's top row; the row you walk in
PLAFOND = 11                 # the brick ceiling of the main lane
K0, K1 = 12, 19              # a room's floor and ceiling
EI_KANAAL = 7                # the switch channel of the egg gate
WARP = {13: "3-1", 14: "3-2", 15: "1-1"}       # a warp pipe's channel -> where it leads (Java: GuhrioW2.WARP)
ZOEK = 26                    # how far round the hall's middle a warp pipe looks for a gate (Java: GuhrioW2.ZOEK)
STATS = {}                   # what the check counted per level (for the build log and the wiki texts)


# =====================================================================================================================
# building blocks of a cellar level
# =====================================================================================================================
def _kelder(baan, gaten=()):
    """The cellar itself: teal ground from end to end except the gaps [(s0, s1)] (they end in kaassaus), a brick ceiling
    with a keystone hanging from it over every pillar of the painted wall (they stand at s % 12 in (0, 1)), a wall at both
    ends. The main lane is rows 0..PLAFOND."""
    for s in range(baan.L):
        if not any(a <= s <= b for a, b in gaten):
            baan.grond(s, s, G, dik=G + 1, blok=KG)
        baan.zet(s, PLAFOND, KS)
        if s % 12 in (0, 1) and 1 < s < baan.L - 1:
            baan.zet(s, PLAFOND - 1, KS)
    for a, b in gaten:
        baan.saus(a, b)
    for s in (0, baan.L - 1):
        baan.vul(s, Y, s, PLAFOND - 1, KS)
    baan.hoofdbaan(0, PLAFOND)


def _kamer(baan, naam, s0, s1, wand):
    """A room above the ceiling: its own lane (rows K0..K1, cells s0..s1), a brick box, wand(s, y) -> the block of its painted wall."""
    baan.bijbaan(naam, s0, s1, K0, K1, hoogte=4)
    baan.vul(s0, K0, s1, K0, KS)
    baan.vul(s0, K1, s1, K1, KS)
    for s in (s0, s1):
        baan.vul(s, K0, s, K1, KS)
    for s in range(s0, s1 + 1):
        for y in range(K0, K1 + 1):
            baan.decor(s, y, wand(s, y))


def _trap(baan, s, hoog=4):
    """The big stairs before the flagpole: hoog columns, each one block higher."""
    for i in range(hoog):
        baan.blok(s + i, Y, hoog=i + 1)


def _schilder(baan, s, y, rijen, kleuren):
    """Pixel art on the painted wall: rijen from the top down, its top left cell at (s, y)."""
    for r, rij in enumerate(rijen):
        for c, ch in enumerate(rij):
            if ch in kleuren:
                baan.decor(s + c, y - r, kleuren[ch])


def _log(baan):
    """A log that shows its rings to the camera (the end of a barrel)."""
    return {"axis": "x" if baan.as_() == "z" else "z"}


def _vaten(baan, s, y=Y):
    """Three sauce barrels against the wall: two with one on top."""
    for ds, dy in ((0, 0), (2, 0), (1, 2)):
        for a in range(2):
            for b in range(2):
                baan.decor(s + ds + a, y + dy + b, "minecraft:stripped_spruce_log", _log(baan))


def _buis(baan, cellen):
    """A painted pipe on the wall: (s, y) cells; the joints (every fourth cell) are darker."""
    for i, (s, y) in enumerate(cellen):
        baan.decor(s, y, "minecraft:green_terracotta" if i % 4 == 0 else "minecraft:lime_terracotta")


def _gewelf(baan, behalve=()):
    """The cellar's vaults on the painted wall: a lamp on every pillar of the theme's backdrop (they stand at s % 12 in
    (0, 1)) and a corbel of tiles under the ceiling beside it. behalve: (s0, s1) stretches that have their own wall."""
    for s in range(baan.L):
        if any(a <= s <= b for a, b in behalve):
            continue
        k = s % 12
        if k in (0, 1):
            baan.decor(s, 8, "minecraft:ochre_froglight", {"axis": "y"})
        if k in (10, 11, 2, 3):
            baan.decor(s, PLAFOND - 1, "minecraft:deepslate_tiles")
        if k in (11, 2):
            baan.decor(s, PLAFOND - 2, "minecraft:deepslate_tiles")


CIJFERS = {"1": ["010", "110", "010", "010", "111"], "2": ["111", "001", "111", "100", "111"], "3": ["111", "001", "111", "001", "111"],
           "-": ["00", "00", "11", "00", "00"]}


def _getal(baan, s, y, tekst, blok):
    """A level's number ("3-1") painted on the wall, five rows high, its top left cell at (s, y); returns its width."""
    x = s
    for ch in tekst:
        for r, rij in enumerate(CIJFERS[ch]):
            for c, bit in enumerate(rij):
                if bit == "1":
                    baan.decor(x + c, y - r, blok)
        x += len(CIJFERS[ch][0]) + 1
    return x - 1 - s


EI = [".###.",
      "#####",
      "#g###",
      "###g#",
      "#g###",
      ".###."]
EI_KLEUR = {"#": "minecraft:white_concrete", "g": "minecraft:lime_concrete"}


# =====================================================================================================================
# level 2-1: De buizenkelder
# =====================================================================================================================
def bouw_2_1(baan):
    """
    Pipes everywhere. From the start: coins, a pipe with a Hapbloem (down it: the hidden coin cellar), bricks with a
    Superknabbel, a flag; the pipe alley (a Schild-Mika and two Guhmba's between two tall pipes that are also each other's
    way round); the first sauce pit: two platforms and an island with a Hapbloem pipe that is a secret way on; the shell
    puzzle: kick the Schild-Mika's shell through the low tunnel into the switch and the red cage round the second vadsmunt
    opens; falling blocks over the second pit; the lift to the high ledge with the third vadsmunt; the stairs and the pole.
    """
    _kelder(baan, [(43, 49), (52, 55), (72, 77)])
    _gewelf(baan)
    baan.start(2, Y)
    baan.munten(5, 7, Y + 1)
    # the first pipe: its Hapbloem guards the way down to the coin cellar
    baan.pijp(10, Y + 1, 1, hoog=2, bloem=True)
    baan.guhmba(13, Y)
    baan.blok(17, Y)
    for s, wat in ((19, "steen"), (20, "superknabbel"), (21, "steen")):
        baan.steen(s, Y + 3) if wat == "steen" else baan.vraag(s, Y + 3, wat)
    baan.vlag(22, Y)
    # the pipe alley: over the first tall pipe (a step helps), through or round the alley, out over the second
    baan.blok(23, Y)
    baan.pijp(24, Y + 2, 3, hoog=3, bloem=True)
    baan.schild_mika(28, Y)
    for s in range(26, 37):                                      # the high road: a row of bricks from pipe to pipe, coins on it
        baan.steen(s, Y + 3) if s != 31 else baan.vraag(s, Y + 3)
    baan.munten(27, 35, Y + 4)
    baan.guhmba(32, Y)
    baan.guhmba(35, Y)
    baan.munten(29, 33, Y + 1)
    baan.blok(37, Y)
    baan.pijp(38, Y + 2, 3, hoog=3, ingang=False)                # (one way: nobody comes up under the Hapbloem of the first)
    baan.pijp(40, PLAFOND - 1, 2, hoog=2, richting="omlaag")     # where the coin cellar lets you out
    baan.vlag(41, Y)
    # the first pit: a platform, the island with its pipe, a second platform
    baan.platform(43, G, "langs", afstand=4, breed=3)
    baan.munten(45, 47, Y + 2)
    baan.pijp(50, Y + 1, 5, hoog=2, bloem=True)                  # a secret way on: it lets you out at s = 79
    baan.platform(52, G, "langs", afstand=2, breed=2)
    baan.vadsmunt(54, Y + 3, 0)                                  # jump for it from the second platform
    baan.munt(53, Y + 2)
    baan.munt(55, Y + 2)
    baan.vlag(57, Y)
    # the shell puzzle: the switch sits at the end of a tunnel only a shell fits through
    baan.blok(59, Y)
    baan.schild_mika(61, Y)
    baan.vul(64, Y + 1, 68, Y + 1, KS)
    baan.schakelaar(68, Y, 0, "aan")
    baan.munten(64, 67, Y + 2)
    for s, y in ((65, 9), (67, 9), (66, 9), (65, 10), (67, 10)):  # the cage hangs under the ceiling; red = solid until the switch
        baan.schakelblok(s, y, 0, aan=False)
    baan.vadsmunt(66, 10, 1)
    baan.vlag(70, Y)
    # the second pit: three falling blocks
    for s in (72, 74, 76):
        baan.valblok(s, G, breed=2)
    baan.munten(73, 76, Y + 2)
    baan.pijp(79, PLAFOND - 1, 5, hoog=2, richting="omlaag")     # where the secret pipe of the island lets you out
    # the lift and the high ledge (the vault is a row higher there, so a big player fits)
    baan.platform(80, Y, "omhoog", afstand=4, breed=2)
    baan.vul(82, 8, 86, 8, KS)
    baan.lucht(80, PLAFOND - 1, 87, PLAFOND)
    baan.vul(79, PLAFOND + 1, 88, PLAFOND + 1, KS)
    baan.munten(83, 85, 9)
    baan.vadsmunt(86, 9, 2)
    baan.munten(83, 85, Y)
    _trap(baan, 88)
    baan.mast(93, Y, hoog=6)
    # plumbing along the ceiling (real pipe bodies: plain blocks) and on the wall
    lang = {"axis": baan.as_()}
    for s0, s1 in ((14, 18), (58, 63)):
        baan.vul(s0, PLAFOND - 1, s1, PLAFOND - 1, gb.PIJP_LIJF, lang)
    for s0, s1 in ((4, 8), (45, 47)):                            # a lump of brick hanging from the ceiling
        baan.vul(s0, PLAFOND - 2, s1, PLAFOND - 1, KS)
    _buis(baan, [(s, 9) for s in range(14, 23)] + [(22, y) for y in range(8, 4, -1)])
    _buis(baan, [(44, y) for y in range(10, 6, -1)] + [(s, 7) for s in range(45, 50)])
    _buis(baan, [(s, 9) for s in range(71, 79)] + [(71, y) for y in range(8, 5, -1)])
    for s in (3, 26, 60):
        _vaten(baan, s)
    # the coin cellar: rows of coins, in through the ceiling, out through a sideways pipe
    _kamer(baan, "munten", 4, 28, lambda s, y: "minecraft:gold_block" if (s + 2 * y) % 6 == 0 and K0 < y < K1 else "minecraft:deepslate_tiles")
    baan.pijp(7, K1 - 1, 1, hoog=2, richting="omlaag")
    baan.munten(9, 22, K0 + 1)
    baan.munten(11, 20, K0 + 3)
    baan.munten(13, 18, K0 + 5)
    baan.pijp(25, K0 + 1, 2, hoog=3, richting="terug")
    STATS[baan.wereld] = guhrio_w2_check.controleer(baan, dict(ei=False, warp=0))


# =====================================================================================================================
# level 2-2: Het nest van Guhshi
# =====================================================================================================================
def bouw_2_2(baan):
    """
    Guhshi. From the start: a platform over the first pit (the first vadsmunt over it), a Guhmba, a Vuurpeper, a flag, the
    door to the eierkamertje (the egg lies on a bed of hay there; a pipe drops you back in front of the gate); a pen with a
    Schild-Mika between two pipes; the egg gate (red blocks under the lock: only open with the egg); the broedgang: the
    hatching spot, the nest, Guhshi; a wide pit (a slow platform, or Guhshi's flutter jump along the high coins; the second
    vadsmunt); two Guhmba's for his tongue; a pipe whose Hapbloem hides the way to the warp room (the third vadsmunt and
    three warp pipes); the stairs and the pole.
    """
    _kelder(baan, [(8, 14), (64, 71)])
    _gewelf(baan, [(44, 59)])
    baan.start(2, Y)
    baan.munten(4, 6, Y + 1)
    baan.platform(8, G, "langs", afstand=4, breed=3)
    baan.munten(10, 12, Y + 2)
    baan.vadsmunt(11, Y + 4, 0)
    baan.guhmba(18, Y)
    for s, wat in ((18, "steen"), (19, "vuurpeper"), (20, "steen")):
        baan.steen(s, Y + 3) if wat == "steen" else baan.vraag(s, Y + 3, wat)
    baan.blok(22, Y)
    baan.vlag(24, Y)
    # the door to the eierkamertje, under a painted egg
    baan.deur(26, Y, 5)
    _schilder(baan, 24, 10, EI, EI_KLEUR)
    # a pen between two pipes (each other's way round)
    baan.pijp(31, Y + 1, 8, hoog=2, bloem=True)
    baan.schild_mika(34, Y)
    for s in range(33, 40):                                      # the high road over the pen
        baan.steen(s, Y + 3) if s != 36 else baan.vraag(s, Y + 3)
    baan.munten(33, 39, Y + 4)
    baan.guhmba(37, Y)
    baan.guhmba(39, Y)
    baan.munten(33, 39, Y + 1)
    baan.pijp(41, Y + 1, 8, hoog=2, ingang=False)
    baan.pijp(43, PLAFOND - 1, 6, hoog=2, richting="omlaag")     # where the eierkamertje lets you out
    # the egg gate and the broedgang behind it: a low passage (four rows) under a mass of brick
    baan.vul(45, 8, 58, PLAFOND - 1, KS)
    baan.zet(45, 8, EISLOT, {"kanaal": str(EI_KANAAL)})
    for y in range(Y, 8):
        baan.schakelblok(45, y, EI_KANAAL, aan=False)
        baan.zet(49, y, BROEDPLEK)
    baan.vlag(47, Y)
    baan.zet(52, Y, NEST)
    baan.guhshi(52, Y + 1)
    for s in range(44, 60):                                      # a warm wall: terracotta, hay along the floor, a lamp over the nest
        for y in range(Y, 8):
            baan.decor(s, y, "minecraft:hay_block" if y == Y and s > 45 else "minecraft:orange_terracotta" if (s + y) % 5 else "minecraft:yellow_terracotta",
                       {"axis": "y"} if y == Y and s > 45 else None)
    for s in (51, 53):
        baan.decor(s, 7, "minecraft:shroomlight")
    _schilder(baan, 38, 10, [".#.", "###", "#g#", ".#."], EI_KLEUR)   # a little egg before the lock: what the gate wants
    baan.vlag(60, Y)
    # the wide pit: a slow platform for who walks, high coins for who flutters
    baan.platform(64, G, "langs", afstand=6, breed=2)
    for s, y in ((65, 6), (66, 7), (67, 7), (69, 7), (70, 7), (71, 6)):
        baan.munt(s, y)
    baan.vadsmunt(68, Y + 4, 1)
    baan.guhmba(74, Y)
    baan.vraag(75, Y + 3, "superknabbel")
    baan.guhmba(77, Y)
    baan.munten(73, 78, Y + 1)
    # the pipe to the warp room (its Hapbloem must be down), and where the warp room lets you out
    baan.pijp(80, Y + 1, 9, hoog=2, bloem=True)
    baan.vlag(82, Y)
    baan.pijp(86, PLAFOND - 1, 10, hoog=2, richting="omlaag")
    _trap(baan, 88)
    baan.mast(93, Y, hoog=6)
    lang = {"axis": baan.as_()}
    for s0, s1 in ((16, 22), (74, 79)):
        baan.vul(s0, PLAFOND - 1, s1, PLAFOND - 1, gb.PIJP_LIJF, lang)
    for s0, s1 in ((3, 6), (9, 11)):
        baan.vul(s0, PLAFOND - 2, s1, PLAFOND - 1, KS)
    _buis(baan, [(s, 9) for s in range(27, 31)] + [(27, y) for y in range(8, 5, -1)])
    _buis(baan, [(s, 9) for s in range(60, 64)] + [(63, y) for y in range(8, 4, -1)])
    for s in (15, 85):
        _vaten(baan, s)
    # the eierkamertje: the egg on a bed of hay
    _kamer(baan, "ei", 16, 40, lambda s, y: "minecraft:hay_block" if y <= K0 + 1 else "minecraft:yellow_terracotta" if (s + y) % 4 == 0 else "minecraft:orange_terracotta")
    baan.deur(20, K0 + 1, 5)
    baan.munten(23, 27, K0 + 1)
    baan.zet(29, K0 + 1, KS)
    baan.vul(30, K0 + 1, 34, K0 + 1, KS)
    baan.vul(30, K0 + 2, 34, K0 + 2, "minecraft:hay_block", {"axis": "y"})
    baan.guhshi_ei(32, K0 + 3)
    baan.munt(30, K0 + 3)
    baan.munt(34, K0 + 3)
    for s in (22, 28, 36):
        baan.decor(s, K1 - 2, "minecraft:shroomlight")
    baan.pijp(37, K0 + 1, 6, hoog=3, richting="terug")
    # the warp room: black as the night, the three numbers in gold
    _kamer(baan, "warp", 50, 93, lambda s, y: "minecraft:purple_concrete" if (s * 7 + y * 13) % 31 == 0 else "minecraft:black_concrete")
    baan.pijp(52, K1 - 1, 9, hoog=2, richting="omlaag")
    baan.vadsmunt(65, K0 + 4, 2)
    for s, kanaal in ((58, 13), (71, 14), (84, 15)):
        baan.zet(s, K0, WARPBUIS, {"axis": "y"})
        baan.zet(s, K0 + 1, WARPPIJP, {"kanaal": str(kanaal), "facing": "up", "ingang": "true", "boven": "false"})
        _getal(baan, s - 4, K1 - 1, WARP[kanaal], "minecraft:gold_block")
    baan.munten(61, 68, K0 + 1)
    baan.munten(74, 81, K0 + 1)
    baan.pijp(90, K0 + 1, 10, hoog=3, richting="terug")
    STATS[baan.wereld] = guhrio_w2_check.controleer(baan, dict(ei=True, warp=3))


LEVELS = {"2-1": bouw_2_1, "2-2": bouw_2_2}


# =====================================================================================================================
# models, blockstates, items, loot, tags
# =====================================================================================================================
def blocks_and_items(h):
    A = h.A
    b = lambda n: f"guhs:block/{n}"
    for name in ("guhriow2_keldergrond", "guhriow2_keldersteen", "guhriow2_eislot"):
        h.simple_block(name)
    # the warp pipe: the mouth opens the way it faces, the body lies along its axis (as the green pipe)
    h.w(f"{A}/models/block/guhriow2_warppijp.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": b("guhriow2_warppijp_boven"), "bottom": b("guhriow2_warppijp_boven"), "side": b("guhriow2_warppijp")}})
    h.w(f"{A}/models/block/guhriow2_warpbuis.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": b("guhriow2_warppijp_boven"), "bottom": b("guhriow2_warppijp_boven"), "side": b("guhriow2_warpbuis")}})
    draai = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "west": {"x": 90, "y": 270}, "east": {"x": 90, "y": 90}}
    h.w(f"{A}/blockstates/guhriow2_warppijp.json", {"variants": {f"facing={f}": {"model": b("guhriow2_warppijp"), **r} for f, r in draai.items()}})
    h.w(f"{A}/blockstates/guhriow2_warpbuis.json", {"variants": {"axis=y": {"model": b("guhriow2_warpbuis")},
                                                                  "axis=z": {"model": b("guhriow2_warpbuis"), "x": 90},
                                                                  "axis=x": {"model": b("guhriow2_warpbuis"), "x": 90, "y": 90}}})
    # the nest: the stove's sides, straw on top
    h.w(f"{A}/models/block/guhriow2_nest.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": b("guhriow2_nest_boven"), "bottom": b("guhriow2_keldersteen"), "side": b("guhriow2_nest_zij")}})
    h.w(f"{A}/blockstates/guhriow2_nest.json", {"variants": {"": {"model": b("guhriow2_nest")}}})
    for name in ("guhriow2_warppijp", "guhriow2_warpbuis", "guhriow2_nest"):
        h.w(f"{A}/models/item/{name}.json", {"parent": b(name)})
    # the hatching spot is not drawn: a model with only its particles, a flat picture as item
    h.w(f"{A}/models/block/guhriow2_broedplek.json", {"textures": {"particle": "guhs:item/guhriow2_broedplek"}})
    h.w(f"{A}/blockstates/guhriow2_broedplek.json", {"variants": {"": {"model": b("guhriow2_broedplek")}}})
    h.item_model("guhriow2_broedplek")
    for name in BLOKKEN:
        if name != "guhriow2_broedplek":
            h.self_drop(name)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in BLOKKEN if n != "guhriow2_broedplek"])


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {
    "block.guhs.guhriow2_keldergrond": "Keldergrond",
    "block.guhs.guhriow2_keldersteen": "Keldersteen",
    "block.guhs.guhriow2_warppijp": "Warppijp",
    "block.guhs.guhriow2_warpbuis": "Warppijp (onderstuk)",
    "block.guhs.guhriow2_eislot": "Eierslot",
    "block.guhs.guhriow2_nest": "Broednest van Guhshi",
    "block.guhs.guhriow2_broedplek": "Broedplekje",
    "gui.guhs.guhriow2.slot.dicht": "Het eierslot piept: zonder ei kom je er niet in, njeg! Zoek het ei van Guhshi (de deur onder het geschilderde ei).",
    "gui.guhs.guhriow2.slot.open": "Het eierslot ruikt het ei van Guhshi en klikt open. Vahoeg!",
    "gui.guhs.guhriow2.uit": "Guhshi is uit zijn ei gekropen! Spring op het nest en hij draagt je. Njeg!",
    "gui.guhs.guhriow2.warp": "Warp! De pijp spuugt je uit in level %s, en de poort van dat level in de hal blijft voortaan voor je open. Vahoeg!",
    "gui.guhs.guhriow2.warp.hal": "De warppijp heeft de poort van level %s voor je opengezet. Loop er maar doorheen, njeg.",
    "gui.guhs.guhriow2.warp.dicht": "Deze warppijp zit verstopt. Njeg?",
    "gui.guhs.guhriow2.vads": "Alle zes de grote vadsmunten van de kelders zijn van jou. Kelder-vads!",
}
SCENE = {
    "leg": "Je legt het ei in het warme nest.",
    "krak1": "Krak...",
    "krak2": "Krak... KRAK!",
    "njeg": "Njeg?",
    "mama": "Njeg! Mama!",
    "rug": "Guhshi denkt dat jij zijn mama bent. Spring maar op zijn rug, njeg!",
}


def texts(h):
    from features import verhaal_motor
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)
    verhaal_motor.scene(h, "guhriow2_uit", "Guhshi kruipt uit zijn ei", SCENE, namen={"guhshi": "Guhshi", "ei": "Het ei"})


def advancements(h):
    from features import bbq2
    for naam in ("guhrio_w2_uitgebroed", "guhrio_w2_warp", "guhrio_w2_vads"):
        bbq2.verborgen(h, naam)
    bbq2.zichtbaar(h, "guhrio", "guhrio_w2_uitgebroed", "root", "guhs:guhrio_guhshi_ei", "goal", "Njeg! Mama!",
                   "Vind het ei van Guhshi in de kelders en leg het in het warme nest")
    bbq2.zichtbaar(h, "guhrio", "guhrio_w2_warp", "guhrio_w2_uitgebroed", "guhs:guhriow2_warppijp", "task", "Binnendoor",
                   "Vind de warpkamer en laat je door een warppijp naar een ander level spugen", hidden=True)


# =====================================================================================================================
# FTB quests (section guhrio_w2 of chapter guhs_guhrio)
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("guhrio_w2_buizenkelder", "Level 2-1: De buizenkelder", "De poort van &6level 2-1&r gaat open zodra je de binnentuin (1-1 en 1-2) gehaald "
      "hebt. Beneden is het donker en staat het vol &agroene pijpen&r: ga er op staan en druk op &dS&r om erin te duiken. Kijk uit voor de "
      "&dHapbloemen&r: zolang zo'n bloem uit haar pijp steekt, krijg je een natte zoen en sta je weer bij je vlaggetje. Over de kaassaus "
      "glijden platforms heen en weer; wachten mag, njeg.", "guhs:guhrio_pijp", [adv("guhrio_stap_4")], rewards=(("guhs:kaas_knabbels", 12),),
      deps=["guhrio_binnen"], xp=50)
    q("guhrio_w2_vads", "Kelder-vads", "Vind alle zes de &6grote vadsmunten&r van de kelders. Twee tips. In level 2-1 hangt er een in een rode "
      "kooi: de &buitroeptekenschakelaar&r die de kooi opent zit aan het eind van een tunneltje waar jij niet in past. Het schild van een "
      "&dSchild-Mika&r wel: spring op hem en geef zijn schild een zetje. En in level 2-2 ligt er een op een plek waar je alleen door een pijp "
      "komt, njeg.", "guhs:guhrio_vadsmunt", [adv("guhrio_w2_vads")], rewards=(("guhs:kaas_knabbels", 16),), deps=["guhrio_w2_buizenkelder"],
      shape="diamond", xp=100)
    q("guhrio_w2_ei", "Een gespikkeld ei", "In &6level 2-2&r zit een hek met een &deierslot&r in de weg. Het gaat alleen open voor wie het &aei "
      "van Guhshi&r bij zich heeft. Zoek de deur onder het geschilderde ei (druk op &dW&r als je erin staat): in het eierkamertje ligt het op "
      "een bedje van hooi.", "guhs:guhrio_guhshi_ei", [adv("guhrio_ei")], rewards=(("guhs:kaas_knabbels", 8),), deps=["guhrio_w2_buizenkelder"])
    q("guhrio_w2_uitgebroed", "Krak... njeg!", "Loop met het ei door het eierslot naar het &6broednest&r, een warm kacheltje met stro erop. "
      "Daar kruipt &aGuhshi&r uit zijn ei. Hij denkt dat jij zijn mama bent. Spring op het nest en hij draagt je: houd &dspatie&r ingedrukt "
      "om te fladderen en klik om zijn tong uit te steken. Wie tegen je aan botst krijgt Guhshi te pakken in plaats van jou; hij wacht dan "
      "weer op zijn nest.", "guhs:guhriow2_nest", [adv("guhrio_w2_uitgebroed")], rewards=(("guhs:kaas_knabbels", 16),),
      deps=["guhrio_w2_ei"], shape="gear", xp=100)
    q("guhrio_w2_nest", "Level 2-2: Het nest van Guhshi", "Rijd op Guhshi naar de vlaggenmast van &6level 2-2&r. Het grote gat in de vloer haal je "
      "fladderend (of te voet, op het trage platform). Daarna gaat de poort van de burcht open: level 3-1.", "guhs:guhrio_mast",
      [adv("guhrio_stap_5")], rewards=(("guhs:kaas_knabbels", 12),), deps=["guhrio_w2_uitgebroed"], xp=50)
    q("guhrio_w2_warp", "Binnendoor", "Niet elke pijp komt uit waar je denkt. Ergens in level 2-2 woont een Hapbloem in een pijp die naar de "
      "&5warpkamer&r leidt: drie gouden pijpen met een getal erboven. Duik erin en je staat meteen in dat level, en de poort ervan in de "
      "hal blijft voortaan voor je open. Het level waar je uit vertrok telt dan niet als gehaald, njeg.", "guhs:guhriow2_warppijp",
      [adv("guhrio_w2_warp")], rewards=(("guhs:kaas_knabbels", 8),), deps=["guhrio_w2_uitgebroed"], shape="circle")


# =====================================================================================================================
def selfcheck(h):
    A, D = h.A, h.D
    missing = [p for p in [f"{A}/blockstates/{n}.json" for n in BLOKKEN] + [f"{A}/models/item/{n}.json" for n in BLOKKEN]
               + [f"{h.TEX}/block/{n}.png" for n in ("guhriow2_keldergrond", "guhriow2_keldersteen", "guhriow2_warppijp", "guhriow2_warppijp_boven",
                                                     "guhriow2_warpbuis", "guhriow2_eislot", "guhriow2_nest_boven", "guhriow2_nest_zij")]
               + [f"{D}/guhrio_level/kasteel_2_{n}.json" for n in (1, 2)] + [f"{D}/structure/{TESTKAMER}.nbt"]
               + [f"{D}/advancement/quest/{n}.json" for n in ("guhrio_w2_uitgebroed", "guhrio_w2_warp", "guhrio_w2_vads")]
               + [f"{D}/advancement/guhrio/{n}.json" for n in ("guhrio_w2_uitgebroed", "guhrio_w2_warp")]
               if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL] + [f"scene.guhs.guhriow2_uit.{k}" for k in SCENE if f"scene.guhs.guhriow2_uit.{k}" not in h.NL]
    # the castle must have built OUR levels (not the engine's practice lanes), each with its rooms
    for wereld, kamers in (("2-1", 1), ("2-2", 2)):
        if wereld not in STATS:
            missing.append(f"level {wereld} was not built from this module")
    # a warp pipe finds the level it leads to through that level's gate in the hall, searching ZOEK blocks round the spot
    # where a flagpole puts you (Java: GuhrioW2.zoekStart): the gates must stand within that range
    from features import guhrio_kasteel as gk
    for wereld in WARP.values():
        thema, kant = next((t, k) for w, t, k in gk.LEVELS if w == wereld)
        poort = (41 if kant == "west" else 85, gk.G + 1, gk.POORT_Z[thema])
        if any(abs(poort[i] - gk.HAL_UIT[i]) > ZOEK for i in (0, 2)) or not 0 <= poort[1] - gk.HAL_UIT[1] <= 1:
            missing.append(f"the gate of level {wereld} at {poort} is further than {ZOEK} blocks from the hall's middle {gk.HAL_UIT}")
    if missing:
        raise SystemExit(f"guhrio_w2: missing {missing}")


def build(h):
    guhrio_w2_tex.save(h)
    blocks_and_items(h)
    texts(h)
    advancements(h)
    h.Structure((24, 12, 7)).save(TESTKAMER)
    selfcheck(h)
