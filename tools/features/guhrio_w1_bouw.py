"""
Super Guhrio wereld 1 (bbq2, slice guhrio-w1) - the two levels of the binnentuin, as functions for the lane builder
(guhrio_baan.Baanbouwer; the castle calls them through guhrio_w1.LEVELS), and the check that both can really be played.

A level slot is 96 cells long and 20 rows high. Both levels fold it into two floors, so a level is about twice as long as
its hall:

  rows 0..9    the main lane: the garden itself. Ground top row 2 (you walk on row 3), ?-blocks on row 6.
  rows 10..19  two more lanes side by side: the second half of the level (you get there through the pipe / the door at
               the end of the garden, and the flagpole stands there) and the level's secret room.
               Their ground is rows 10..11 (you walk on row 12, ?-blocks on row 15); seen from the garden it is the sky's
               clouds and the crowns of the trees.

  1-1  "De binnentuin"   the lawn (walk, jump, ?-blocks, the first Guhmba's, a pit, stairs) -> the big green pipe ->
                         over the clouds and treetops to the flagpole. Secret: the mole's den under the lawn (a pipe).
  1-2  "De heggentuin"   hedges to climb, cheese ponds, a Schild-Mika with a row of Guhmba's, a vadsmunt in a brick box
                         -> the tower door -> along the top of the garden wall to the flagpole, where Pad-guh waits in
                         front of the wrong part of the castle. Secret: the gardener's coin greenhouse (hidden block + door).

Every height and distance stays inside what the engine's manual calls safe, and tighter where a level NEEDS it: a step
up is at most 2, a pit at most 3 wide. controleer() walks the level with those rules (small and big) and refuses to build
a level whose flagpole, flags, vadsmunten or secret cannot be reached.

The painted wall (d = -1) is painted here too, cell by cell (Doek): sky, hills with eyes, clouds with a snoet, trees whose
crowns are the blocks you walk on one floor up, the towers of the castle.
"""
import math

from features import guhrio_baan as gb

_P = "guhs:guhriow1_"
# what you walk on and bump into (the lane) ...
GRAS, AARDE, HEG, WOLK, BLOEM, TIP, GEHEIM = _P + "gras", _P + "aarde", _P + "heg", _P + "wolk", _P + "bloem", _P + "tip", _P + "geheim"
# ... and what is only painted on the wall behind it: paler and bluer, so it never looks like something to stand on
LUCHT, LOOF, WOLK_VER, WOLK_SNOET, HEUVEL, HEUVEL_OGEN = (_P + "lucht", _P + "loof", _P + "wolk_ver", _P + "wolk_snoet", _P + "heuvel",
                                                         _P + "heuvel_ogen")
STENEN, GEBARSTEN = "guhs:houtskoolsteen_stenen", "guhs:gebarsten_houtskoolsteen_stenen"
SIER = "guhs:guhrio_siersteen"
ROOD, GLOED = "minecraft:red_nether_bricks", "minecraft:shroomlight"
# the castle far away, on the painted wall: grey stone, a terracotta band and roof, a yellow window
VER_STEEN, VER_BAND, VER_DAK, VER_RAAM = "minecraft:stone_bricks", "minecraft:orange_terracotta", "minecraft:red_terracotta", "minecraft:yellow_concrete"
STAM = "minecraft:stripped_spruce_wood"
SAUS_VERF = "minecraft:yellow_concrete"                 # what a cheese pond looks like on the painted wall
DONKER = "minecraft:green_terracotta"                   # the shade inside a hedge

G, Y = 2, 3                   # the garden: the ground's top row, the row you walk on
D0, GB, U = 10, 11, 12        # one floor up: the first row of its lanes, its ground's top row, the row you walk on
H_KIJK = 5                    # the picture shows 5 rows above and below its middle: exactly one floor

# the tips (the block's property `tip`; the texts are gui.guhs.guhriow1.tip.<nr> in guhrio_w1.py)
TIP_LOPEN, TIP_VRAAG, TIP_GUHMBA, TIP_GAT, TIP_PIJP, TIP_SUPER, TIP_DEUR, TIP_SCHILD, TIP_RENNEN = range(9)

# the spots other code needs, in the coordinates of the whole castle: filled in while the castle builds the levels
KASTEEL = {}


# =====================================================================================================================
# little tools
# =====================================================================================================================
def _gazon(baan, gaten):
    """The lawn from end to end: grass on two rows of earth, except the pits [(s0, s1)], which end in the sauce."""
    for s in range(baan.L):
        if any(a <= s <= b for a, b in gaten):
            continue
        baan.zet(s, G, GRAS)
        baan.vul(s, 0, s, G - 1, AARDE)
    for a, b in gaten:
        baan.saus(a, b)


def _dek(baan, s0, s1, boven, onder=None, gaten=()):
    """The ground one floor up (rows 10 and 11) for the cells s0..s1."""
    for s in range(s0, s1 + 1):
        if any(a <= s <= b for a, b in gaten):
            continue
        baan.zet(s, GB, boven)
        baan.zet(s, D0, onder or boven)


def _muur(baan, s0, s1, y0, y1, banden=()):
    """A piece of the castle's own wall in the lane (the ends of a level, the tower between two rooms)."""
    for s in range(s0, s1 + 1):
        for y in range(y0, y1 + 1):
            baan.zet(s, y, SIER if y in banden else GEBARSTEN if baan.rng.random() < 0.07 else STENEN)


def _tip(baan, nr, s0, s1, y):
    """A tip the player walks through: two rows high, in every free cell of s0..s1."""
    for s in range(s0, s1 + 1):
        for yy in (y, y + 1):
            if baan.haal(s, yy) is None:
                baan.zet(s, yy, TIP, {"tip": str(nr)})


def _geheim(baan, s0, s1, y0, y1):
    for s in range(s0, s1 + 1):
        for y in range(y0, y1 + 1):
            if baan.haal(s, y) is None:
                baan.zet(s, y, GEHEIM)


def _boog(baan, s0, s1, y, top=1):
    """Coins in an arc over a pit."""
    n = s1 - s0
    for i, s in enumerate(range(s0, s1 + 1)):
        baan.munt(s, y + (top if 0 < i < n else 0))


def _bloemen(baan, s0, s1, y, kans=0.3):
    """Little plants on the free cells of a lawn (you walk right through them)."""
    for s in range(s0, s1 + 1):
        if baan.haal(s, y) is None and baan.haal(s, y - 1) in (GRAS, HEG) and baan.rng.random() < kans:
            baan.zet(s, y, BLOEM, {"soort": str(baan.rng.choice((0, 1, 2, 3, 3)))})


def _camera_yaw(baan):
    """The yaw of somebody who looks at the camera."""
    return {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[baan.kant("camera")]


# =====================================================================================================================
# the painted wall
# =====================================================================================================================
class Doek:
    """The painted wall of a level: paint in layers (later strokes cover earlier ones), then klaar() puts it on d = -1."""

    def __init__(self, baan):
        self.baan = baan
        self.c = {}

    def zet(self, s, y, blok):
        if 0 <= s < self.baan.L and 0 <= y < self.baan.H:
            self.c[(s, y)] = blok

    def vul(self, s0, y0, s1, y1, blok):
        for s in range(s0, s1 + 1):
            for y in range(y0, y1 + 1):
                self.zet(s, y, blok)

    def heuvel(self, midden, breed, hoog, y0, ogen=True):
        """A round hill standing on row y0; a hill of three or more has eyes."""
        for s in range(midden - breed // 2, midden + breed // 2 + 1):
            t = (s - midden) / (breed / 2 + 0.5)
            top = int(round(hoog * math.sqrt(max(0.0, 1 - t * t))))
            for y in range(y0, y0 + top):
                self.zet(s, y, HEUVEL)
        if ogen and hoog >= 3:
            self.zet(midden, y0 + hoog - 2, HEUVEL_OGEN)

    def wolk(self, s, y, breed=4, snoet=True):
        """A cloud far away: a row of `breed` and a shorter row on top; the snoet looks out of the lower row."""
        self.vul(s, y, s + breed - 1, y, WOLK_VER)
        self.vul(s + 1, y + 1, s + breed - 2, y + 1, WOLK_VER)
        if snoet:
            self.zet(s + breed // 2, y, WOLK_SNOET)

    def struik(self, s, y, breed=3):
        self.vul(s, y, s + breed - 1, y, LOOF)
        if breed >= 4:
            self.vul(s + 1, y + 1, s + breed - 2, y + 1, LOOF)

    def boom(self, s, y0, y1, kroon=2):
        """A trunk from y0 to y1 with leaves around its top (the crown itself is the ground of the floor above)."""
        self.vul(s, y0, s, y1, STAM)
        self.vul(s - kroon, y1, s + kroon, y1, LOOF)
        self.vul(s - kroon + 1, y1 - 1, s + kroon - 1, y1 - 1, LOOF)
        self.zet(s, y1 - 1, STAM)

    def toren(self, s, y0, hoog, breed=3, vlag=True):
        """A tower of the castle far away: grey stone, a terracotta band, a lit window, a pointed roof (and a pennant)."""
        for x in range(s, s + breed):
            for y in range(y0, y0 + hoog):
                self.zet(x, y, VER_BAND if y == y0 + hoog - 1 else VER_STEEN)
        self.zet(s + breed // 2, y0 + hoog - 3, VER_RAAM)
        for i in range((breed + 1) // 2 + 1):
            self.vul(s - 1 + i, y0 + hoog + i, s + breed - i, y0 + hoog + i, VER_DAK)
        if vlag:
            self.zet(s + breed // 2, y0 + hoog + (breed + 1) // 2 + 1, VER_RAAM)

    def heg(self, s0, s1, y0, hoog):
        """A tall clipped hedge far away, with round tops."""
        self.vul(s0, y0, s1, y0 + hoog - 1, LOOF)
        for s in range(s0 + 1, s1, 4):
            self.vul(s, y0 + hoog, s + 1, y0 + hoog, LOOF)

    def zon(self, s, y):
        self.vul(s, y, s + 2, y + 2, "minecraft:yellow_concrete")
        for ds, dy in ((1, 3), (1, -1), (-1, 1), (3, 1)):
            self.zet(s + ds, y + dy, "minecraft:orange_concrete")

    def klaar(self):
        for (s, y), blok in self.c.items():
            self.baan.decor(s, y, blok)


def _tuindoek(baan, gaten, bomen=(), heuvels=(), struiken=(), heggen=()):
    """The garden's own wall (rows 0..9): earth behind the lawn, cheese ponds behind the pits, sky, hedges, hills, trees."""
    doek = Doek(baan)
    doek.vul(0, 0, baan.L - 1, G, AARDE)
    doek.vul(0, Y, baan.L - 1, D0 + 1, LUCHT)
    for a, b in gaten:
        doek.vul(a, 0, b, G - 1, SAUS_VERF)
        doek.vul(a, G, b, G, LUCHT)
    for s0, s1, hoog in heggen:
        doek.heg(s0, s1, Y, hoog)
    for midden, breed, hoog in heuvels:
        doek.heuvel(midden, breed, hoog, Y)
    for s, breed in struiken:
        doek.struik(s, Y, breed)
    for s in bomen:
        doek.boom(s, Y, D0 - 1)
    return doek


# =====================================================================================================================
# level 1-1: de binnentuin
# =====================================================================================================================
def bouw_1_1(baan):
    KASTEEL.setdefault("1-1", baan)
    baan.achtergrond(False)
    baan.hoofdbaan(0, D0 - 1, hoogte=H_KIJK)
    baan.bijbaan("hol", 0, 19, D0, baan.H - 1, hoogte=H_KIJK)
    baan.bijbaan("boven", 24, baan.L - 1, D0, baan.H - 1, hoogte=H_KIJK)
    gaten = [(37, 38), (66, 67)]

    # ---------------------------------------------------------------------------------------------- the lawn (rows 0..9)
    _gazon(baan, gaten)
    _muur(baan, 0, 0, Y, D0 - 1, banden=(Y, D0 - 1))
    _muur(baan, 92, baan.L - 1, Y, D0 - 1, banden=(Y, D0 - 1))
    baan.start(2, Y)
    _tip(baan, TIP_LOPEN, 3, 4, Y)
    baan.munten(5, 7, Y + 1)
    # the first ?-block
    _tip(baan, TIP_VRAAG, 8, 9, Y)
    baan.vraag(10, Y + 3)
    # the first Guhmba, in a pen between two hedges: look at him from the hedge, then hop on
    baan.zet(14, Y, HEG)
    _tip(baan, TIP_GUHMBA, 13, 13, Y)
    _tip(baan, TIP_GUHMBA, 14, 14, Y + 1)
    baan.guhmba(18, Y)
    baan.zet(22, Y, HEG)
    # bricks and ?-blocks (the Superknabbel), the first big vadsmunt on top: climb the hedge behind it and jump back
    _tip(baan, TIP_SUPER, 23, 24, Y)
    for s, wat in ((25, "steen"), (26, "superknabbel"), (27, "steen"), (28, "munt"), (29, "steen")):
        baan.steen(s, Y + 3) if wat == "steen" else baan.vraag(s, Y + 3, wat)
    baan.vadsmunt(27, Y + 5, 0)
    baan.vul(31, Y, 31, Y + 1, HEG)
    baan.vlag(34, Y)
    # the first pit
    _tip(baan, TIP_GAT, 35, 36, Y)
    _boog(baan, 36, 39, Y + 2)
    # a Guhmba between a hedge and a pipe; the pipe is the way into the mole's den (the coin over it says so)
    baan.zet(41, Y, HEG)
    baan.guhmba(45, Y)
    baan.pijp(48, Y + 1, 1, hoog=2)
    baan.munt(48, Y + 4)
    # ?-blocks, bricks and a hidden block; a Guhmba under them; the pipe you come back out of
    baan.vraag(51, Y + 3)
    baan.steen(52, Y + 3)
    baan.steen(53, Y + 3)
    baan.onzichtbaar(54, Y + 3)
    baan.guhmba(53, Y)
    baan.zet(57, Y, HEG)
    baan.pijp(58, Y + 1, 3, hoog=2, ingang=False)
    baan.vlag(60, Y)
    # the stairs over the cheese pond
    for i, s in enumerate((64, 65)):
        baan.blok(s, Y, hoog=i + 1)
        baan.blok(69 - i, Y, hoog=i + 1)
    baan.munt(66, Y + 4)
    baan.munt(67, Y + 4)
    baan.vraag(73, Y + 3)
    # two Guhmba's in the last pen, coins between them and a ?-block over them
    baan.zet(76, Y, HEG)
    baan.guhmba(80, Y)
    baan.guhmba(83, Y)
    baan.munten(79, 82, Y + 1)
    baan.vraag(81, Y + 3)
    # the big pipe: the way up
    baan.vul(86, Y, 87, Y, HEG)
    _tip(baan, TIP_PIJP, 86, 87, Y + 1)
    baan.pijp(88, Y + 2, 2, hoog=3)
    baan.munt(88, Y + 4)
    baan.munt(88, Y + 5)
    baan.zet(89, Y, HEG)                                             # (whoever hops over the pipe can climb back)
    baan.munten(90, 91, Y)

    # ------------------------------------------------------------------------------- the mole's den (rows 10..19, s 0..19)
    _dek(baan, 0, 19, HEG)
    for s in (0, 19):
        baan.vul(s, U, s, baan.H - 1, AARDE)
    baan.vul(0, baan.H - 1, 19, baan.H - 1, AARDE)
    baan.pijp(3, baan.H - 2, 1, hoog=1, richting="omlaag")
    _geheim(baan, 2, 4, U, U + 2)
    baan.munten(5, 14, U + 1)
    for s in range(6, 14):
        if s != 10:
            baan.munt(s, U + 3)
    baan.vadsmunt(10, U + 3, 1)
    baan.pijp(17, U, 3, hoog=2, richting="terug")
    baan.vul(17, U + 2, 18, baan.H - 2, AARDE)

    # ----------------------------------------------------------------------- the tower between the den and the clouds
    _muur(baan, 20, 23, D0, baan.H - 1, banden=(D0, GB, baan.H - 1))

    # ------------------------------------------------------------------ over the clouds (rows 10..19, s 24..95)
    _dek(baan, 24, 40, WOLK)
    _dek(baan, 43, 53, HEG)
    _dek(baan, 56, 64, WOLK)                                         # (the gap behind it is the sky over the pond's stairs)
    _dek(baan, 68, 79, HEG)
    _dek(baan, 80, baan.L - 1, STENEN)
    for s in range(80, baan.L):
        baan.zet(s, GB, SIER if s % 2 == 0 else STENEN)
    baan.pijp(27, U + 1, 2, hoog=2, ingang=False)
    baan.vlag(30, U)
    baan.munten(32, 36, U + 1)
    baan.vraag(38, U + 3, "superknabbel")
    _boog(baan, 40, 43, U + 2)
    # a Guhmba in the first treetop, under bricks with a ?-block
    baan.zet(44, U, HEG)
    baan.guhmba(48, U)
    for s in range(46, 51):
        baan.steen(s, U + 3) if s != 48 else baan.vraag(s, U + 3)
    baan.zet(52, U, HEG)
    _boog(baan, 53, 56, U + 2)
    # the third big vadsmunt lies on a little cloud: up the treetop, onto the hidden block (the coin says "jump here")
    baan.vul(57, U, 57, U + 1, HEG)
    baan.munt(59, U + 2)
    baan.onzichtbaar(59, U + 3)
    baan.vul(61, U + 4, 63, U + 4, WOLK)
    baan.vadsmunt(62, U + 5, 2)
    baan.vlag(64, U)
    _boog(baan, 64, 68, U + 2)
    # two Guhmba's in the second treetop, coins like a rainbow over them
    baan.zet(70, U, HEG)
    baan.guhmba(73, U)
    baan.guhmba(76, U)
    for i, s in enumerate(range(71, 78)):
        baan.munt(s, U + 3 + (1 if 1 <= i <= 5 else 0) + (1 if 2 <= i <= 4 else 0))
    baan.zet(78, U, HEG)
    # the top of the tower: the big stairs and the flagpole
    for i, s in enumerate(range(81, 85)):
        baan.blok(s, U, hoog=i + 1)
    baan.mast(88, U, hoog=7)
    _muur(baan, 92, baan.L - 1, U, baan.H - 1, banden=(U, baan.H - 1))

    _bloemen(baan, 1, 91, Y)
    _bloemen(baan, 43, 79, U, kans=0.2)

    # ------------------------------------------------------------------------------------------------ the painted wall
    doek = _tuindoek(baan, gaten, bomen=(45, 51, 71, 77), heuvels=((7, 11, 3), (30, 15, 4), (58, 9, 3)), struiken=((17, 4), (39, 2), (61, 2)))
    doek.vul(80, Y, baan.L - 1, D0 + 1, STENEN)                    # the foot of the tower whose top you finish on
    doek.vul(80, 6, baan.L - 1, 6, SIER)
    for s in (83, 87):
        doek.vul(s, 7, s, 8, GLOED)
    # the den: earth with roots and two glow lights
    doek.vul(0, D0, 19, baan.H - 1, "minecraft:brown_terracotta")
    for s in range(1, 19):
        for y in range(D0, baan.H):
            k = (s * 7 + y * 13 + (s // 3) * (y // 2)) % 23
            if k in (0, 9):
                doek.zet(s, y, "minecraft:rooted_dirt")
            elif k == 4:
                doek.zet(s, y, "minecraft:coarse_dirt")
    for s, lang in ((5, 3), (9, 2), (13, 4), (16, 2)):
        doek.vul(s, baan.H - 1 - lang, s, baan.H - 2, "minecraft:stripped_dark_oak_wood")
    for s in (7, 14):
        doek.zet(s, U + 5, "minecraft:ochre_froglight")
    # the tower between them (its bricks are in the lane; behind it the same)
    doek.vul(20, D0, 23, baan.H - 1, STENEN)
    # the sky over the clouds: the sun, clouds with a snoet, the castle's towers far away
    doek.vul(24, D0, baan.L - 1, baan.H - 1, LUCHT)
    doek.zon(33, 15)
    doek.toren(45, U, 5)
    doek.toren(68, U, 3, vlag=False)                               # (a low one and a tall one, a cell of sky between their roofs)
    doek.toren(73, U, 5)
    for s, y, breed in ((26, 16, 4), (45, 17, 5), (53, 14, 4), (78, 17, 4), (84, 14, 4)):
        doek.wolk(s, y, breed)
    doek.vul(92, D0, baan.L - 1, baan.H - 1, STENEN)
    doek.klaar()


# =====================================================================================================================
# level 1-2: de heggentuin
# =====================================================================================================================
def bouw_1_2(baan):
    KASTEEL.setdefault("1-2", baan)
    baan.achtergrond(False)
    baan.hoofdbaan(0, D0 - 1, hoogte=H_KIJK)
    baan.bijbaan("muur", 0, 71, D0, baan.H - 1, hoogte=H_KIJK)
    baan.bijbaan("kas", 76, baan.L - 1, D0, baan.H - 1, hoogte=H_KIJK)
    gaten = [(21, 22), (25, 26), (45, 46), (78, 80)]

    # ------------------------------------------------------------------------------------------- the hedge garden (rows 0..9)
    _gazon(baan, gaten)
    _muur(baan, 0, 0, Y, D0 - 1, banden=(Y, D0 - 1))
    _muur(baan, 91, baan.L - 1, Y, D0 - 1, banden=(Y, D0 - 1))
    baan.start(2, Y)
    baan.munten(5, 8, Y + 1)
    # a hedge to climb, with a Guhmba walking on it and two ?-blocks over it (the first Superknabbel)
    baan.zet(10, Y, HEG)
    baan.vul(11, Y, 17, Y + 1, HEG)
    baan.guhmba(14, Y + 2)
    baan.vraag(13, Y + 5)
    baan.vraag(15, Y + 5, "superknabbel")
    # two cheese ponds with an island between them
    _boog(baan, 20, 23, Y + 2)
    _boog(baan, 24, 27, Y + 2)
    baan.vlag(29, Y)
    # a Schild-Mika and a row of Guhmba's: hop on him and kick his shell at them; it ends in the pond
    _tip(baan, TIP_SCHILD, 30, 30, Y)
    baan.zet(31, Y, HEG)
    _tip(baan, TIP_SCHILD, 31, 31, Y + 1)
    baan.schild_mika(34, Y)
    for s in (37, 39, 41):
        baan.guhmba(s, Y)
    _boog(baan, 44, 47, Y + 2)
    # a Superknabbel, and the first big vadsmunt in a box of bricks: only somebody big breaks in
    baan.vraag(49, Y + 3, "superknabbel")
    for s in range(51, 56):
        baan.steen(s, Y + 3)
    for s in (52, 54):
        baan.steen(s, Y + 4)
    for s in (52, 53, 54):
        baan.steen(s, Y + 5)
    baan.vadsmunt(53, Y + 4, 0)
    baan.vlag(58, Y)
    # the hedge arch. On top of it is the door of the greenhouse; the way up is a hidden block next to the little hedge
    baan.zet(63, Y, HEG)
    baan.onzichtbaar(64, Y + 2)
    baan.munt(64, Y + 3)
    baan.munt(64, Y + 4)
    baan.vul(66, Y + 3, 69, Y + 3, HEG)
    baan.deur(68, Y + 4, 4)
    baan.guhmba(67, Y)
    baan.zet(71, Y, HEG)                                             # (not right behind the arch: somebody big cannot hop under it)
    baan.vraag(73, Y + 3)
    baan.steen(74, Y + 3)
    baan.vraag(75, Y + 3)
    # the wide pond: take a run
    _tip(baan, TIP_RENNEN, 75, 76, Y)
    _boog(baan, 77, 81, Y + 2)
    baan.vlag(82, Y)
    baan.zet(84, Y, HEG)
    baan.guhmba(86, Y)
    baan.zet(88, Y, HEG)
    # the tower door: the way up
    _tip(baan, TIP_DEUR, 89, 90, Y)
    baan.deur(90, Y, 5)

    # ------------------------------------------------------------------- the top of the garden wall (rows 10..19, s 0..71)
    muurgaten = [(22, 23), (36, 38), (53, 54)]
    _dek(baan, 0, 71, STENEN, onder=HEG, gaten=muurgaten)
    for s in range(0, 72):
        if baan.haal(s, GB) and s % 2 == 0:
            baan.zet(s, GB, SIER)
    _muur(baan, 0, 0, U, baan.H - 1, banden=(U, baan.H - 1))
    baan.deur(3, U, 5)
    baan.vlag(5, U)
    baan.munten(7, 10, U + 1)
    # Guhmba's between the merlons, ?-blocks over them
    baan.zet(12, U, STENEN)
    baan.guhmba(16, U)
    baan.vraag(15, U + 3)
    baan.vraag(16, U + 3, "superknabbel")
    baan.vraag(17, U + 3)
    baan.zet(20, U, STENEN)
    _boog(baan, 21, 24, U + 2)
    baan.zet(26, U, STENEN)
    baan.guhmba(28, U)
    baan.guhmba(31, U)
    for s in range(28, 33):                                          # (not over the cell behind the merlon: somebody big must get off it)
        baan.steen(s, U + 3)
        if s % 2:
            baan.munt(s, U + 4)
    baan.vul(34, U, 34, U + 1, STENEN)                               # (two high: the way onto the bricks)
    _boog(baan, 35, 39, U + 2)
    baan.vlag(40, U)
    # up the wall: a step, a platform, a tower; the third big vadsmunt hangs high over the gap behind the tower
    baan.zet(42, U, STENEN)
    baan.vul(43, U, 47, U + 1, STENEN)
    baan.vraag(45, U + 5)
    baan.vul(50, U, 52, U + 2, STENEN)
    for s in (50, 52):
        baan.zet(s, U + 2, SIER)
    baan.munten(48, 49, U + 4)
    baan.vadsmunt(54, U + 6, 2)
    baan.vlag(56, U)
    # the big stairs, the flagpole, and the wrong part of the castle with Pad-guh in front of its door
    for i, s in enumerate(range(58, 62)):
        baan.blok(s, U, hoog=i + 1)
    baan.mast(64, U, hoog=7)
    yaw = _camera_yaw(baan)
    from features import wereld
    baan.entity(67, U, wereld.npc(baan.h, "padguh", "guhriow1_padguh", "guhriow1", yaw))
    _muur(baan, 71, 71, U, baan.H - 1, banden=(U, baan.H - 1))

    # ---------------------------------------------------------------------------- the tower between the wall and the greenhouse
    _muur(baan, 72, 75, D0, baan.H - 1, banden=(D0, GB, baan.H - 1))

    # ------------------------------------------------------------ the gardener's coin greenhouse (rows 10..19, s 76..95)
    WIT, TEGEL = "minecraft:white_concrete", "minecraft:polished_andesite"
    _dek(baan, 76, baan.L - 1, TEGEL, onder=HEG)
    for s in (76, baan.L - 1):
        baan.vul(s, U, s, baan.H - 1, WIT)
    baan.vul(76, baan.H - 1, baan.L - 1, baan.H - 1, WIT)
    baan.deur(78, U, 4)
    _geheim(baan, 79, 80, U, U + 1)
    baan.munten(81, 91, U + 1)
    for s in range(82, 91, 2):
        baan.munt(s, U + 3)
    baan.blok(93, U)
    baan.vadsmunt(93, U + 2, 1)

    _bloemen(baan, 1, 90, Y, kans=0.25)

    # ------------------------------------------------------------------------------------------------ the painted wall
    # (tall clipped hedges behind the garden; the one of the arch is the hedge you walk through)
    doek = _tuindoek(baan, gaten, heuvels=((5, 9, 3), (38, 13, 4), (85, 9, 3)), struiken=((28, 2), (57, 2)),
                     heggen=((9, 19, 4), (48, 56, 2), (61, 74, 6)))
    doek.vul(66, Y, 69, Y + 2, DONKER)                               # the way through the arch
    doek.vul(91, Y, baan.L - 1, D0 + 1, STENEN)
    doek.vul(89, Y, 91, Y + 3, STENEN)                               # the door's own frame
    doek.vul(89, Y + 3, 91, Y + 3, SIER)
    doek.vul(90, Y, 90, Y + 1, "minecraft:black_concrete")
    # the sky over the wall: the sun, clouds, towers, and at the end the wrong part of the castle
    doek.vul(0, D0, 71, baan.H - 1, LUCHT)
    doek.vul(0, D0, 0, baan.H - 1, STENEN)
    doek.vul(2, U, 4, U + 2, STENEN)                                 # the door you come out of
    doek.vul(2, U + 3, 4, U + 3, SIER)
    doek.vul(3, U, 3, U + 1, "minecraft:black_concrete")
    doek.zon(8, 16)
    doek.toren(28, U, 5)
    doek.toren(46, U + 2, 3, vlag=False)
    for s, y, breed in ((13, 17, 4), (27, 17, 5), (41, 16, 4), (57, 17, 4)):
        doek.wolk(s, y, breed)
    doek.vul(65, U, 70, U + 3, STENEN)                               # the castle part: a wall with a gate, merlons, a tower
    doek.vul(65, U + 3, 70, U + 3, SIER)
    for s in (65, 67, 69):
        doek.zet(s, U + 4, STENEN)
    doek.vul(66, U, 68, U + 1, "minecraft:black_concrete")
    doek.zet(67, U + 2, "minecraft:black_concrete")
    doek.vul(69, U + 4, 70, U + 5, STENEN)
    doek.vul(68, U + 6, 71, U + 6, ROOD)
    doek.vul(69, U + 7, 70, U + 7, ROOD)
    doek.zet(69, U + 5, GLOED)
    doek.vul(71, D0, 71, baan.H - 1, STENEN)
    # the tower between them
    doek.vul(72, D0, 75, baan.H - 1, STENEN)
    # the greenhouse: white frames, sky behind the glass, planters with a hedge in them
    doek.vul(76, D0, baan.L - 1, baan.H - 1, LUCHT)
    for s in range(76, baan.L, 4):
        doek.vul(s, D0, s, baan.H - 1, WIT)
    doek.vul(76, U + 4, baan.L - 1, U + 4, WIT)
    doek.vul(76, baan.H - 1, baan.L - 1, baan.H - 1, WIT)
    for s in range(77, baan.L - 1):
        if s % 4:
            doek.zet(s, U, "minecraft:spruce_planks")
            doek.zet(s, U + 1, LOOF)
    doek.klaar()


LEVELS = {"1-1": bouw_1_1, "1-2": bouw_1_2}


# =====================================================================================================================
# can it be played? (run for every level this module builds)
# =====================================================================================================================
LOOPT_DOOR = {gb.AIR, gb.MUNT, gb.VADSMUNT, gb.VLAG, gb.MAST, gb.DEUR, gb.GUHMBA, gb.SCHILD_MIKA, gb.PLOF_MIKA, gb.HAPBLOEM, gb.PLATFORM,
              gb.VALBLOK, gb.GUHSHI_EI, gb.GUHSHI, gb.START, gb.GRILLSPIES, TIP, GEHEIM, BLOEM}
# what a jump may do in this check: it rises 1, 2 or 3 rows and then goes at most this many cells along (less by one for
# every row it lands higher), and it never lands more than STAP_OP rows higher. These are the safe numbers of the engine's
# manual and no more (a step up of 2, a pit of 3): the jump was worked out, never measured in a client.
REIK = {1: 2, 2: 3, 3: 4}
STAP_OP = 2


class Loper:
    """
    Walks a built level with the rules above: the cells a player can stand in (staan), the cells a body passes through
    (geraakt), who climbs out of their lane (hoog) and who can never get to the flagpole again (vast_gelopen). Somebody
    big needs three rows instead of two and breaks the bricks over their head; somebody small finds the hidden blocks.
    """

    def __init__(self, baan, groot=False):
        self.baan, self.groot = baan, groot
        self.lijf = 3 if groot else 2
        hoofd = baan._hoofd
        self.banen = [dict(id="hoofd", s0=0, s1=baan.L - 1, onder=hoofd["onder"], boven=hoofd["boven"])] + [dict(b) for b in baan._bij]
        self.gevonden, self.gebroken = set(), set()       # hidden blocks that were bumped, bricks that were broken
        self.staan, self.geraakt, self.hoog, self.vast_gelopen = set(), set(), set(), set()
        self.bij_mast = False

    def blok(self, s, y):
        if not 0 <= s < self.baan.L or y >= self.baan.H:
            return STENEN
        if y < 0:
            return None
        cel = self.baan.cellen.get((s, y, 0))
        return None if cel is None else cel[0]

    def vast(self, s, y):
        blok = self.blok(s, y)
        if blok == gb.ONZICHTBAAR:
            return (s, y) in self.gevonden
        if blok == gb.STEEN and (s, y) in self.gebroken:
            return False
        return blok is not None and blok not in LOOPT_DOOR

    def vrij(self, s, y, rijen=None):
        return all(not self.vast(s, y + k) for k in range(self.lijf if rijen is None else rijen))

    def baan_van(self, s, y):
        for i, b in enumerate(self.banen):
            if b["s0"] <= s <= b["s1"] and b["onder"] <= y <= b["boven"]:
                return i
        return -1

    def val(self, s, y, lane):
        """Where somebody in cell (s, y) comes to stand: (s, y') or None (under the lane: fell, back to the flag)."""
        if not 0 <= s < self.baan.L or y < self.banen[lane]["onder"]:
            return None
        while not self.vast(s, y - 1):
            y -= 1
            if y < self.banen[lane]["onder"]:
                return None
        return (s, y)

    def _mond(self, s, y):
        cel = self.baan.cellen.get((s, y, 0))
        return cel[1] if cel is not None and cel[0] == gb.PIJP and cel[1].get("boven") != "true" else None

    def _andere(self, soort, kanaal, hier):
        """The partner of a pipe mouth / a door, the way the game finds it: the next one of the channel in lane order."""
        lijst = []
        for b in self.banen:
            for s in range(b["s0"], b["s1"] + 1):
                for y in range(b["onder"], b["boven"] + 1):
                    cel = self.baan.cellen.get((s, y, 0))
                    if cel is None or cel[0] != soort or cel[1].get("kanaal") != kanaal:
                        continue
                    if (soort == gb.PIJP and cel[1].get("boven") == "true") or (soort == gb.DEUR and cel[1].get("half") != "lower"):
                        continue
                    if (s, y) not in lijst:
                        lijst.append((s, y))
        return lijst[(lijst.index(hier) + 1) % len(lijst)] if hier in lijst and len(lijst) > 1 else None

    def _uit(self, mond):
        """The cell you are in once you are out of this mouth."""
        s, y = mond
        kant = self._mond(s, y)["facing"]
        if kant == "up":
            return (s, y + 1)
        if kant == "down":
            return (s, y - 2)
        return (s + (1 if kant == self.baan.kant("verder") else -1), y)

    def buren(self, s, y):
        """From standing in (s, y): [(cell you come out in, cells your body passes, True for a pipe or a door)]."""
        uit = []
        cel = self.baan.cellen.get((s, y, 0))
        if cel is not None and cel[0] == gb.DEUR and cel[1].get("half") == "lower":
            naar = self._andere(gb.DEUR, cel[1]["kanaal"], (s, y))
            if naar:
                uit.append((naar, [], True))
        onder = self._mond(s, y - 1)
        if onder and onder["facing"] == "up" and onder["ingang"] == "true":
            naar = self._andere(gb.PIJP, onder["kanaal"], (s, y - 1))
            if naar:
                uit.append((self._uit(naar), [], True))
        for teken in (-1, 1):
            zij = self._mond(s + teken, y)
            if zij and zij["ingang"] == "true" and zij["facing"] == self.baan.kant("terug" if teken > 0 else "verder"):
                naar = self._andere(gb.PIJP, zij["kanaal"], (s + teken, y))
                if naar:
                    uit.append((self._uit(naar), [], True))
            # a step (and a drop)
            if self.vrij(s + teken, y):
                uit.append(((s + teken, y), [(s + teken, y + k) for k in range(self.lijf)], False))
            # a jump: straight up `rijst` rows, along at that height, down again
            for rijst in (1, 2, 3):
                if not self.vrij(s, y + 1, rijst + self.lijf - 1):
                    break
                weg = [(s, y + k) for k in range(rijst + self.lijf)]
                for dx in range(1, REIK[rijst] + 1):
                    t = s + teken * dx
                    if not self.vrij(t, y + rijst):
                        break
                    weg = weg + [(t, y + rijst + k) for k in range(self.lijf)]
                    ty = y + rijst
                    while ty > -2 and not self.vast(t, ty - 1):
                        ty -= 1
                    stijgt = ty - y
                    if stijgt > STAP_OP or dx > REIK[rijst] - max(stijgt, 0):
                        continue
                    uit.append(((t, ty), weg + [(t, yy) for yy in range(max(ty, 0), y + rijst)], False))
        return uit

    def _rond(self):
        """One walk from the start with what is found / broken so far."""
        start = (self.baan._start[0], self.baan._start[1], 0)
        staan, geraakt, hoog, pijlen = set(), set(), set(), {}
        wachtrij = [start]
        while wachtrij:
            staat = wachtrij.pop()
            if staat in staan:
                continue
            s, y, lane = staat
            staan.add(staat)
            geraakt.update((s, y + k) for k in range(self.lijf))
            if y > self.banen[lane]["boven"]:
                hoog.add((s, y))
            for naar, weg, wissel in self.buren(s, y):
                geraakt.update(weg)
                doel = lane
                if wissel:
                    doel = self.baan_van(*naar)
                    if doel < 0:
                        continue
                plek = self.val(naar[0], naar[1], doel)
                if plek is None or not self.vrij(*plek):
                    continue
                volgende = (plek[0], plek[1], doel)
                pijlen.setdefault(volgende, set()).add(staat)
                wachtrij.append(volgende)
        return staan, geraakt, hoog, pijlen

    def loop(self):
        """Walks until nothing new is found or broken; then works out who is stuck."""
        while True:
            staan, geraakt, hoog, pijlen = self._rond()
            plekken = {(s, y) for s, y, _l in staan}
            nieuw = False
            for (cs, cy, cd), (naam, _p, _n) in self.baan.cellen.items():
                if cd != 0:
                    continue
                if naam == gb.ONZICHTBAAR and not self.groot and (cs, cy) not in self.gevonden:
                    # bumped by a head that comes up under it: somebody standing two to four rows lower, air in between
                    if any((cs, cy - dy) in plekken and all(not self.vast(cs, cy - k) for k in range(1, dy - 1)) for dy in (2, 3, 4)):
                        self.gevonden.add((cs, cy))
                        nieuw = True
                elif naam == gb.STEEN and self.groot and (cs, cy) not in self.gebroken:
                    # broken by a big head: somebody big standing three to six rows lower, air in between
                    if any((cs, cy - dy) in plekken and all(not self.vast(cs, cy - k) for k in range(1, dy - 2)) for dy in (3, 4, 5, 6)):
                        self.gebroken.add((cs, cy))
                        nieuw = True
            if not nieuw:
                break
        self.staan, self.geraakt, self.hoog = {(s, y) for s, y, _l in staan}, geraakt, hoog
        # who can still get to the flagpole: walk the arrows back from everybody whose body touches it
        masten = {(s, y) for (s, y, d), v in self.baan.cellen.items() if d == 0 and v[0] == gb.MAST}
        goed = {st for st in staan if any((st[0] + ds, st[1] + k) in masten for ds in (-1, 0, 1) for k in range(self.lijf))}
        wachtrij = list(goed)
        while wachtrij:
            for van in pijlen.get(wachtrij.pop(), ()):
                if van not in goed:
                    goed.add(van)
                    wachtrij.append(van)
        self.bij_mast = bool(goed)
        self.vast_gelopen = {(s, y) for s, y, _l in staan - goed}
        return self


def controleer(baan):
    """
    Refuses (SystemExit) a level that cannot be played with the safe jump. Somebody small AND somebody big must be able to
    reach the flagpole and every flag (a Superknabbel never locks you in or out), and from wherever they can get to they
    must still be able to reach the flagpole (no pocket you cannot climb out of); the three big vadsmunten, the secret,
    every tip and every coin must be within reach (small or big); nobody gets from the garden onto the floor above
    without the pipe or the door; and there is a flag at least every 36 cells. Returns a few numbers for the build log.
    """
    klein, groot = Loper(baan).loop(), Loper(baan, groot=True).loop()
    fouten = []
    soorten = {}
    for (s, y, d), (naam, props, _n) in baan.cellen.items():
        if d == 0:
            soorten.setdefault(naam, []).append((s, y, props or {}))
    for wie, loper in (("small", klein), ("big", groot)):
        if not loper.bij_mast:
            fouten.append(f"the flagpole is out of reach of somebody {wie}")
        for s, y, _p in soorten.get(gb.VLAG, []):
            if (s, y) not in loper.geraakt:
                fouten.append(f"the flag at ({s}, {y}) is out of reach of somebody {wie}")
        if loper.vast_gelopen:
            fouten.append(f"somebody {wie} is stuck (no way on to the flagpole) at {sorted(loper.vast_gelopen)[:6]}")
        if loper.hoog:
            fouten.append(f"somebody {wie} climbs out of their lane at {sorted(loper.hoog)[:4]}")
    beide = klein.geraakt | groot.geraakt
    for s, y, props in soorten.get(gb.VADSMUNT, []):
        if (s, y) not in beide:
            fouten.append(f"big vadsmunt {props.get('nummer')} at ({s}, {y}) is out of reach")
    geheim = soorten.get(GEHEIM, [])
    if not geheim:
        fouten.append("no secret (guhriow1_geheim)")
    elif not any((s, y) in klein.geraakt for s, y, _p in geheim):
        fouten.append("the secret is out of reach")
    los = [(s, y) for s, y, _p in soorten.get(gb.MUNT, []) if (s, y) not in beide]
    if los:
        fouten.append(f"coins out of reach: {sorted(los)}")
    for nr in sorted({p.get("tip") for _s, _y, p in soorten.get(TIP, [])}):
        if not any((s, y) in klein.geraakt for s, y, p in soorten[TIP] if p.get("tip") == nr):
            fouten.append(f"tip {nr} is never walked through")
    for b in klein.banen:
        vlaggen = sorted(s for s, y, _p in soorten.get(gb.VLAG, []) if b["s0"] <= s <= b["s1"] and b["onder"] <= y <= b["boven"])
        for a, c in zip(vlaggen, vlaggen[1:]):
            if c - a > 36:
                fouten.append(f"lane {b['id']}: {c - a} cells between the flags at s={a} and s={c}")
    if fouten:
        raise SystemExit(f"guhrio_w1 level {baan.wereld}: " + "; ".join(fouten))
    return dict(plekken=len(klein.staan), munten=len(soorten.get(gb.MUNT, [])), vlaggen=len(soorten.get(gb.VLAG, [])),
                wezens=sum(len(soorten.get(k, [])) for k in (gb.GUHMBA, gb.SCHILD_MIKA)),
                tips=len({p.get("tip") for _s, _y, p in soorten.get(TIP, [])}), verborgen=len(klein.gevonden))
