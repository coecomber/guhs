"""
einde2, slice bouw_schelpenstad - what the builders of the road trip share: the numbers of the build (where the road runs,
how high the ground lies, where the loopgraaf cuts through), the block names and a few small tools.

West to east along one road (template coordinates, x grows to the east; the seam with Bikinibodem is the west face, x 0):

    x   0 ..   9   the causeway out of Bikinibodem (the first 8 blocks are exactly the road of kit.WEG)
    x  10 ..  58   the plain of De Ruige Kroeg (the tugboat on the north side of the road, its yard and parking)
    x  59 ..  89   the road climbs six steps through the rocks to the west rim
    x  91 .. 127   the loopgraaf: a trench right across the slab, 18 deep, with a path down and a path up
    x 129 .. 161   the east rim and the road down the hill
    x 162 .. 232   Schelpenstad on its beach of kruimelzand; beyond the beach the void is the sea (the pier, David Hasselguh)

The other modules: _land (the slab, the road, the loopgraaf), _kroeg (De Ruige Kroeg), _stad (Schelpenstad), _bouw (puts it
together, the markers, the light, the self-check).
"""
import math

from features import einde2_kit as kit
from features import guheinde_bouw as gb

NAAM = "schelpenstad"
W, H, D = 256, 48, 96
Y = kit.LAAG_MIN                 # the world y of template y 0
ANKER = (120, 0, 40)
SEED = 21401750
PREFIX = "sign.guhs.einde2_bouw_schelpenstad"
EIGEN = "guhs:einde2_bouw_schelpenstad_"

# --- blocks ------------------------------------------------------------------------------------------------------------
KORST, BARST, ROTS, ZAND = "guhs:kaaskorst", "guhs:gebarsten_korst", "guhs:kruimelrots", "guhs:kruimelzand"
STENEN, TRAP, PLAAT, MUUR = "guhs:kruimelrots_stenen", "guhs:kruimelrots_stenen_trap", "guhs:kruimelrots_stenen_plaat", "guhs:kruimelrots_stenen_muur"
KORSTSTENEN, BELEGEN, KOOL, KOOLSTENEN = "guhs:kaaskorst_stenen", "guhs:belegen_kaas_stenen", "guhs:houtskoolsteen", "guhs:houtskoolsteen_stenen"
SCHIMMEL, ZWAM = "guhs:schimmelkaas", "guhs:schimmelpaddenstoel"
PAREL, PARELTEGEL, KLINKER = "guhs:parelmoer", "guhs:parelmoer_tegels", "guhs:knuffelklinkers"
LAMP, STAAFJE = "guhs:guh_kristal_lamp", "guhs:lichtstaafje"
PALM, PALMSTAM = "guhs:guhwaii_palm_planken", "guhs:guhwaii_palm_stam"
SCHELPSTEEN, DROOGLAMP, SOUVENIR, KUSSEN = EIGEN + "schelpsteen", EIGEN + "drooglamp", EIGEN + "souvenir", EIGEN + "kroonkussen"
STOPCONTACT, SPROEIER, NEON, KAARTENREK = EIGEN + "stopcontact", EIGEN + "sproeier", EIGEN + "neon", EIGEN + "kaartenrek"
EIGEN_BLOKKEN = (SCHELPSTEEN, DROOGLAMP, SOUVENIR, KUSSEN, STOPCONTACT, SPROEIER, NEON, KAARTENREK)

# light the kit's table does not know: our own two lamps and the lights of older blocks this build uses
EIGEN_LICHT = {DROOGLAMP: 15, NEON: 10, "guhs:tiki_fakkel": 15, "guhs:tiki_schelpjeslampion": 13, "guhs:schimmelhoed": 10,
               "guhs:spooklantaarn": 12, "guhs:sterrenlantaarn": 15}

# --- the road ----------------------------------------------------------------------------------------------------------
WEG_W = [(0, 48), (10, 48), (24, 52), (54, 52), (78, 46), (89, 46)]      # (x, the z of the stripe), west of the loopgraaf
WEG_O = [(129, 50), (138, 50), (160, 42), (175, 42)]                     # east of it, into town
X_WEST_EIND, X_OOST_BEGIN, X_OOST_EIND = 89, 129, 175
STAP_OP = (60, 65, 70, 75, 80, 85)                    # from this x on the road lies one higher
STAP_AF = (134, 138, 142, 146, 150, 154, 158, 162)    # from this x on one lower
WEG_Y = kit.WEG["y"]                                  # 15: the deck at the seam (you walk at 16)
RAND_Y = WEG_Y + len(STAP_OP)                         # 21: the ground at the rims of the loopgraaf
STAD_Y = RAND_Y - len(STAP_AF)                        # 13: the ground of Schelpenstad

# --- the loopgraaf -----------------------------------------------------------------------------------------------------
KLOOF_W, KLOOF_O = 91, 127       # its faces at rim height
KLOOF_Z0, KLOOF_Z1 = 9, 86       # its closed ends
BODEM = 3                        # the floor block (you walk at 4)
HELLING = 0.42                   # how much a wall leans in per block of depth

# --- De Ruige Kroeg (a tugboat on the dry) -------------------------------------------------------------------------------
KX0, KX1, KZC = 14, 54, 26       # stern, the tip of the bow, the centre line
K_VLOER, K_DEK = WEG_Y, WEG_Y + 6

# --- Schelpenstad --------------------------------------------------------------------------------------------------------
STAD_X = 162
PLEIN = (184, 43)                # the middle of the square (the aankomst)
STRAND_Z, STRAND_X = 57, 213     # from here on the ground is beach (south, east)
LAND_OOST = 232                  # the last x of land; beyond it the void is the sea


def lijn(punten, x):
    """Piecewise linear through (x, value) points, rounded."""
    if x <= punten[0][0]:
        return punten[0][1]
    for (x0, v0), (x1, v1) in zip(punten, punten[1:]):
        if x <= x1:
            return int(round(v0 + (v1 - v0) * (x - x0) / (x1 - x0)))
    return punten[-1][1]


def wegz(x):
    """The z of the road's stripe at x (across the loopgraaf: the straight line between its two ends)."""
    if x <= X_WEST_EIND:
        return lijn(WEG_W, x)
    if x >= X_OOST_BEGIN:
        return lijn(WEG_O, x)
    return lijn([(X_WEST_EIND, WEG_W[-1][1]), (X_OOST_BEGIN, WEG_O[0][1])], x)


def wegy(x):
    """The y of the road's deck at x (the ground along the road)."""
    return WEG_Y + sum(1 for s in STAP_OP if x >= s) - sum(1 for s in STAP_AF if x >= s)


def glad(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


class Ruis:
    """Smooth value noise in 2D, 0..1, from an integer hash (the same picture every run)."""

    def __init__(self, seed):
        self.seed = seed & 0xFFFFFFFF

    def _punt(self, ix, iz):
        n = (ix * 374761393 + iz * 668265263 + self.seed * 982451653) & 0xFFFFFFFF
        n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
        return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0

    def __call__(self, x, z, schaal):
        fx, fz = x / schaal, z / schaal
        ix, iz = math.floor(fx), math.floor(fz)
        tx, tz = glad(fx - ix), glad(fz - iz)
        a, b_ = self._punt(ix, iz), self._punt(ix + 1, iz)
        c, d = self._punt(ix, iz + 1), self._punt(ix + 1, iz + 1)
        return (a + (b_ - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz


class Stad(kit.Bouw):
    """kit.Bouw for this build: it also knows the light of our own lamps, can take a block away again (an unset cell is air
    in the deep band) and finds the ground of a column."""

    def __init__(self, h):
        super().__init__(h, NAAM, (W, H, D), SEED, prefix=PREFIX)
        self.vast = set()        # cells the light keeper may not use (roads, floors, the spots of the markers)

    def licht(self):
        oud = dict(kit.LICHT)
        kit.LICHT.update(EIGEN_LICHT)
        try:
            return super().licht()
        finally:
            kit.LICHT.clear()
            kit.LICHT.update(oud)

    def wis(self, x0, y0, z0, x1=None, y1=None, z1=None):
        """Takes blocks away (air)."""
        x1, y1, z1 = x0 if x1 is None else x1, y0 if y1 is None else y1, z0 if z1 is None else z1
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.s.blocks.pop((x, y, z), None)

    def naam(self, x, y, z):
        bl = self.s.blocks.get((x, y, z))
        return bl[0] if bl else None

    def grond(self, x, z, van=None):
        """The y of the highest block of this column that you can stand on (None: nothing there)."""
        for y in range(H - 1 if van is None else van, -1, -1):
            if self.solid_at((x, y, z)):
                return y
        return None

    def kader(self, x0, y0, z0, x1, y1, z1, blok, props=None):
        """The four walls of a box (no floor, no roof)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    for y in range(y0, y1 + 1):
                        self.set(x, y, z, blok, props)

    def schijf(self, cx, y, cz, r, blok, props=None, vanaf=0.0):
        """A disc (or a ring: vanaf = the inner radius) of blocks around the middle of cell (cx, cz)."""
        n = int(math.ceil(r))
        for x in range(cx - n, cx + n + 1):
            for z in range(cz - n, cz + n + 1):
                if vanaf < math.hypot(x - cx, z - cz) <= r or (vanaf == 0.0 and x == cx and z == cz):
                    self.set(x, y, z, blok, props)

    def paal(self, x, y, z, hoog, blok=MUUR, licht="minecraft:lantern"):
        """A post with a light on top (y = its lowest block)."""
        for d in range(hoog):
            self.set(x, y + d, z, blok)
        if "lantern" in licht or "lampion" in licht:
            self.set(x, y + hoog, z, licht, gb.lamp(False))
        else:
            self.set(x, y + hoog, z, licht)

    def bord(self, x, y, z, facing, regels, wall=True, wood="birch"):
        self.sign(x, y, z, facing, regels, wall=wall, wood=wood)


def stoel(b, x, y, z, kijkt):
    b.set(x, y, z, "guhs:guh_stoel", {"facing": kijkt})


def tafel(b, x, y, z):
    b.set(x, y, z, "guhs:guh_tafel", {"facing": "north"})


def deur(b, x, y, z, facing, wood="birch", hinge="left"):
    gb.door(b, x, y, z, facing, wood, hinge)
