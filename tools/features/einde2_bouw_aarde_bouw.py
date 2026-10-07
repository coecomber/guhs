"""
einde2, slice bouw_aarde: the builder kit of Ba Sing Guh (used by einde2_bouw_aarde_stad.py and einde2_bouw_aarde_onder.py).

Ba Sing Guh is ONE build of 256 x 110 x 256 that floats in the high band of the Guheinde on a slab of its own: a round
island of klipkaas (the aged cliff cheese of the earth biome) with a tapering underside, three rings that each lie three
blocks higher than the one around it, and a lobe on the west side where you land. The island is a SHELL (a crust on top, a
skin underneath): everything underground is carved (Stad.hak) and gets its own lining (Stad.voering).

  Stad            kit.Bouw with the carved cells, the taken plots of the surface and a few fast paths for the self-check
  eiland          the slab: surface per ring, the crust, the underside with its belly under Meer Laogai and its drips
  ringmuur/poort  a ring wall and a gate through it in one of the four wind directions (the steps to the next ring are IN it)
  traptoren       a square spiral stair in a 7 x 7 tower (the monorail stations, the outer wall, the two ways down)
  huis            a small house you can walk into (three styles: lower, middle and upper ring)
  pagodedak       a hip roof with turned-up corners (theehuis, palace, stations, gates)
  vangnet         the last step: every floor a player can reach gets light (a glowing kristalkaas tile where a lamp is missing)

Coordinates are template coordinates of the whole build: x = east, z = south, the middle of the city is (CX, CZ).
"""
import math
from collections import deque

import features.einde2_kit as kit
from features import guheinde_bouw as gb
from features.guheinde_bouw import lamp, slab, stair

NAAM = "ba_sing_guh"
W, H, D = 256, 110, 256
CX, CZ = 134, 128
ONDER, MIDDEN, BOVEN = 45, 48, 51                 # the ground block of the lower, middle and upper ring (you stand one higher)
R_BOVEN, R_MUUR3, R_MIDDEN, R_MUUR2, R_ONDER, R_MUUR1 = 40, 44, 70, 74, 104, 110   # blocks from the middle: ring, wall, ring, wall, ring, wall
MUUR1_TOP, MUUR2_TOP, MUUR3_TOP = 65, 59, 61      # the walkway block of the outer wall, the caps of the two inner walls
SPOOR_Z, SPOOR_Y = 112, 65                        # the monorail: the deck of the viaduct (the beam lies on it)

# our own decorative blocks (E2BouwAardeFeature)
MUUR = "guhs:einde2_bouw_aarde_stadssteen"
MUNT = "guhs:einde2_bouw_aarde_aardemunt"
KOOL = "guhs:einde2_bouw_aarde_kool"
SPOOR = "guhs:einde2_bouw_aarde_spoor"
GROEN = "guhs:einde2_bouw_aarde_groen_kristal"
THEEKIST = "guhs:einde2_bouw_aarde_theekist"
# the earth biome's blocks (biomes slice) and the old cheese of the Guheinde
KLIP, KLIPS, KLIPT, KLIPP, KLIPM = "guhs:klipkaas", "guhs:klipkaas_stenen", "guhs:klipkaas_stenen_trap", "guhs:klipkaas_stenen_plaat", "guhs:klipkaas_stenen_muur"
GOUD, GOUDT, GOUDP, GOUDM = "guhs:kaaskorst_stenen", "guhs:kaaskorst_stenen_trap", "guhs:kaaskorst_stenen_plaat", "guhs:kaaskorst_stenen_muur"
KRISTAL, KRISTALBLOK = "guhs:kristalkaas_kristal", "guhs:kristalkaas_blok"
TEGEL, TEGELT, TEGELP = "minecraft:dark_prismarine", "minecraft:dark_prismarine_stairs", "minecraft:dark_prismarine_slab"   # the green roof tiles
AIR = gb.AIR
LICHTBLOK = "minecraft:light"

kit.LICHT.setdefault(GROEN, 12)
kit.LICHT.setdefault(LICHTBLOK, 15)

DIRS = gb.DIRS
OPP = gb.OPP
ZIJDEN = ("north", "east", "south", "west")


# =====================================================================================================================
# numbers: noise, the shape of the island
# =====================================================================================================================
def _hash(ix, iz, seed):
    n = (ix * 374761393 + iz * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def ruis(x, z, schaal, seed):
    """Smooth value noise in -1 .. 1 (a pure function of the place: no generator state)."""
    fx, fz = x / schaal, z / schaal
    ix, iz = math.floor(fx), math.floor(fz)
    tx, tz = fx - ix, fz - iz
    tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
    a, b, c, d = _hash(ix, iz, seed), _hash(ix + 1, iz, seed), _hash(ix, iz + 1, seed), _hash(ix + 1, iz + 1, seed)
    return (a + (b - a) * tx + (c - a) * tz + (a - b - c + d) * tx * tz) * 2 - 1


def afstand(x, z):
    return math.hypot(x - CX, z - CZ)


def pol(d, graden):
    """d blocks from the middle in this direction (0 = east, 90 = south, 180 = west, 270 = north)."""
    return int(round(CX + d * math.cos(math.radians(graden)))), int(round(CZ + d * math.sin(math.radians(graden))))


def rand_straal(x, z):
    d = afstand(x, z) or 1.0
    return 116.2 + 1.5 * ruis((x - CX) / d * 60, (z - CZ) / d * 60, 9, 11)


def lob(x, z):
    """Blocks inside the edge of the landing lobe on the west side (negative: outside it)."""
    return 7.0 + 0.8 * ruis(x, z, 6, 12) - math.hypot(max(10 - x, 0, x - 40), max(106 - z, 0, z - 150))


def top(x, z):
    """The ground block of this column: a wall stands on the LOWER of the two rings it parts."""
    d = afstand(x, z)
    return BOVEN if d < R_BOVEN else MIDDEN if d < R_MIDDEN else ONDER


def bodem(x, z, s):
    """The lowest block of this column; s = blocks inside the rim. A bowl, with a belly under Meer Laogai (the base)."""
    f = 1 - (1 - min(s, 116.0) / 116.0) ** 2
    diep = 3 + 34 * f ** 0.8
    diep += 15 * math.exp(-((x - 224) ** 2 + (z - 128) ** 2) / (2 * 26.0 ** 2))
    diep += 2.0 * ruis(x, z, 13, 13) + 0.8 * ruis(x, z, 5, 14)
    return max(1, int(round(42 - diep)))


class Kader:
    """One of the four wind directions seen from the middle: u = blocks from the middle, v = sideways (to the right when
    you look at the middle from outside... it only matters that it is the same everywhere)."""

    def __init__(self, richting):
        self.richting = richting
        self.ux, self.uz = {"west": (-1, 0), "east": (1, 0), "north": (0, -1), "south": (0, 1)}[richting]
        self.vx, self.vz = (0, 1) if self.ux else (1, 0)
        self.naar_binnen = OPP[richting]          # the way a stair faces that climbs towards the middle
        self.dwars = ("north", "south") if self.ux else ("west", "east")   # facing of a thing at v < 0 / v > 0 that looks at v = 0

    def p(self, u, v):
        return CX + self.ux * u + self.vx * v, CZ + self.uz * u + self.vz * v


# =====================================================================================================================
# the build
# =====================================================================================================================
class Stad(kit.Bouw):
    def __init__(self, h):
        super().__init__(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"] + 49, prefix="sign.guhs.einde2_bouw_aarde")
        self.hol = set()         # cells carved out of the island (they stay air: no lining ever fills them)
        self.bezet = set()       # (x, z) of the surface that is taken (roads, walls, plots)
        self.kolom = {}          # (x, z) -> (ground block y, lowest block y, blocks from the middle)
        self._dun = {}
        self.lantaarns = set()   # (x, z) of the street lamps
        self.rommel = {}         # the small things between the houses: kind -> count
        self.huizen = {}         # houses per ring
        self.vangnet = {}        # what the last step added: kind -> count

    # --- fast paths of the self-check (the walker asks these a few million times) ------------------------------------
    def is_dun(self, n):
        v = self._dun.get(n)
        if v is None:
            v = self._dun[n] = n == LICHTBLOK or super().is_dun(n)
        return v

    # --- blocks -------------------------------------------------------------------------------------------------------
    def zet(self, x, y, z, name, props=None, nbt=None):
        self.set(x, y, z, name, props, nbt)

    def naam(self, x, y, z):
        bl = self.s.blocks.get((x, y, z))
        return bl[0] if bl else None

    def weg(self, x, y, z):
        self.s.blocks.pop((x, y, z), None)

    def vul(self, x0, y0, z0, x1, y1, z1, name, props=None):
        self.fill(x0, y0, z0, x1, y1, z1, name, props)

    def schil(self, x0, y0, z0, x1, y1, z1, name):
        """Only the six faces of the box (a mass nobody looks into)."""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or y in (y0, y1) or z in (z0, z1):
                        self.set(x, y, z, name)

    def hak(self, x0, y0, z0, x1, y1, z1):
        """Carves the box to air and remembers the cells."""
        out = []
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    c = (x, y, z)
                    self.s.blocks.pop(c, None)
                    self.hol.add(c)
                    out.append(c)
        return out

    def hak_cel(self, c):
        self.s.blocks.pop(c, None)
        self.hol.add(c)

    def voering(self, cellen, kies):
        """The lining of carved cells: every neighbour that is not carved itself and holds nothing yet gets
        kies((x, y, z), kant) with kant "vloer", "plafond" or "wand". First the six faces, then the edges and corners (as
        "wand"): so the lining is one closed, connected skin."""
        blocks, hol = self.s.blocks, self.hol
        for (x, y, z) in cellen:
            for dx, dy, dz, kant in ((0, -1, 0, "vloer"), (0, 1, 0, "plafond"), (1, 0, 0, "wand"), (-1, 0, 0, "wand"), (0, 0, 1, "wand"), (0, 0, -1, "wand")):
                n = (x + dx, y + dy, z + dz)
                if n not in hol and n not in blocks:
                    self.set(n[0], n[1], n[2], kies(n, kant))
        for (x, y, z) in cellen:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        n = (x + dx, y + dy, z + dz)
                        if n not in hol and n not in blocks:
                            self.set(n[0], n[1], n[2], kies(n, "wand"))

    # --- the surface --------------------------------------------------------------------------------------------------
    def neem(self, x0, z0, x1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                self.bezet.add((x, z))

    def laat_open(self, x0, y0, z0, x1, y1, z1):
        """These cells are an opening of something carved (the mouth of a passage, the top of a shaft): no lining fills them."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.hol.add((x, y, z))

    def vrij(self, x0, z0, x1, z1):
        return all((x, z) not in self.bezet and (x, z) in self.kolom for x in range(x0, x1 + 1) for z in range(z0, z1 + 1))

    def grond(self, x, z):
        return self.kolom[(x, z)][0]

    def plavei(self, x, z, name):
        """Paves this column (its ground block) and takes it."""
        k = self.kolom.get((x, z))
        if k is not None:
            self.set(x, k[0], z, name)
            self.bezet.add((x, z))


# =====================================================================================================================
# 1. the island
# =====================================================================================================================
def _rots(x, y, z):
    n = ruis(x + y * 0.7, z - y * 0.4, 7, 15)
    return "guhs:kruimelrots" if n > 0.5 else "guhs:kaaskorst" if n < -0.55 else KLIP


def _grond(x, z, d):
    n = ruis(x, z, 9, 16)
    if d < R_BOVEN:
        return "minecraft:moss_block"
    if d < R_MIDDEN:
        return "guhs:kaaskorst" if n > 0.45 else "minecraft:moss_block"
    if d < R_ONDER:
        return "guhs:kaaskorst" if n > 0.4 else "minecraft:coarse_dirt" if n < -0.5 else "guhs:gebarsten_korst"
    return "guhs:kaaskorst" if n > 0.35 else KLIP


def eiland(b):
    """The slab: for every column its ground block, a crust of three and a skin underneath that closes against its
    neighbours (the inside stays hollow); the rim columns are solid. Drips of klipkaas hang under it."""
    kol = b.kolom
    for x in range(W):
        for z in range(D):
            d = afstand(x, z)
            s = max(rand_straal(x, z) - d, lob(x, z))
            if s >= 0:
                kol[(x, z)] = (top(x, z), bodem(x, z, s), d)
    blocks = b.s.blocks
    for (x, z), (t, bt, d) in kol.items():
        buren = [kol.get(n) for n in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))]
        rand = any(n is None for n in buren)
        y1 = t if rand else max(bt + 2, max(n[1] for n in buren) + 1)
        if rand or y1 >= t - 3:
            for y in range(bt, t):
                blocks[(x, y, z)] = (_rots(x, y, z), {}, None)
        else:
            for y in range(bt, y1 + 1):
                blocks[(x, y, z)] = (_rots(x, y, z), {}, None)
            blocks[(x, t - 2, z)] = blocks[(x, t - 1, z)] = (KLIP, {}, None)
        blocks[(x, t, z)] = (_grond(x, z, d), {}, None)
        # drips: now and then a spike of klipkaas under the skin, longer towards the middle
        if not rand and _hash(x, z, 17) < 0.035:
            lang = 1 + int(_hash(x, z, 18) * min(7, 2 + (116 - d) / 14))
            for y in range(max(1, bt - lang), bt):
                blocks[(x, y, z)] = (KLIP, {}, None)
            if bt - lang - 1 >= 1 and _hash(x, z, 19) < 0.3:
                blocks[(x, bt - lang - 1, z)] = (KRISTAL, {"facing": "down", "waterlogged": "false"}, None)


# =====================================================================================================================
# 2. walls and gates
# =====================================================================================================================
def muursteen(hoog, onder_top):
    """The course of a wall: klipkaas bricks at the foot and as a band, the big wall stone between."""
    return KLIPS if hoog <= 2 or onder_top == 3 else MUUR


def ringmuur(b, r0, r1, y_top, buiten=False):
    """A ring wall between r0 and r1 blocks from the middle, its top course at y_top. buiten: the outer wall (hollow, with a
    walkway, battlements on the outside, a low parapet on the inside and lanterns); else a wall with a green tiled cap."""
    gezien = set()
    for (x, z), (t, bt, d) in b.kolom.items():
        if r0 - 2 <= d < r1 + 2:
            b.bezet.add((x, z))
        if not (r0 <= d < r1):
            continue
        schil = d < r0 + 1.5 or d >= r1 - 1.5
        for y in range(t + 1, y_top + 1):
            if buiten and not schil and y < y_top - 1:
                continue
            b.set(x, y, z, muursteen(y - t, y_top - y))
        if not buiten:
            b.set(x, y_top, z, TEGEL)
            if not schil:
                b.set(x, y_top + 1, z, TEGELP, slab())
            continue
        boog = int(math.atan2(z - CZ, x - CX) * r1)
        if d >= r1 - 1.5:
            b.set(x, y_top + 1, z, KLIPS)
            if (boog // 2) % 2 == 0:
                b.set(x, y_top + 2, z, KLIPS)
        elif d < r0 + 1.5:
            b.set(x, y_top + 1, z, KLIPS)
            if boog % 11 == 0 and boog not in gezien:
                gezien.add(boog)
                b.set(x, y_top + 2, z, "minecraft:lantern", lamp(False))


def muurtoren(b, graden):
    """A round bastion on the outside of the outer wall: its top is one floor with the walkway."""
    y_top = MUUR1_TOP
    mx, mz = CX + (R_MUUR1 - 0.5) * math.cos(math.radians(graden)), CZ + (R_MUUR1 - 0.5) * math.sin(math.radians(graden))
    for x in range(int(mx) - 6, int(mx) + 7):
        for z in range(int(mz) - 6, int(mz) + 7):
            k = b.kolom.get((x, z))
            r = math.hypot(x - mx, z - mz)
            if k is None or r > 4.3 or k[2] < R_MUUR1 - 1.5:
                continue
            b.bezet.add((x, z))
            for y in (y_top + 1, y_top + 2):
                b.weg(x, y, z)
            if k[2] >= R_MUUR1:
                for y in range(k[0] + 1, y_top + 1):
                    if r > 2.8 or y >= y_top - 1:
                        b.set(x, y, z, muursteen(y - k[0], y_top - y))
            if r > 3.2 and k[2] >= R_MUUR1 - 0.3:
                b.set(x, y_top + 1, z, KLIPS)
                if int(math.atan2(z - mz, x - mx) * 4.3) % 2 == 0:
                    b.set(x, y_top + 2, z, KLIPS)
    cx, cz = int(round(mx + 2 * math.cos(math.radians(graden)))), int(round(mz + 2 * math.sin(math.radians(graden))))
    b.set(cx, y_top + 1, cz, KLIPM)
    b.set(cx, y_top + 2, cz, KLIPM)
    b.set(cx, y_top + 3, cz, "minecraft:lantern", lamp(False))


def poort(b, richting, r0, r1, y_top, laag, hoog):
    """A gate through an inner wall (between r0 and r1) in a wind direction: a block of wall stone a little higher than
    the wall, a passage five wide whose steps climb from the outer ring (ground `laag`) to the inner one (`hoog`), the
    aardemunt over the arch on both faces, lanterns in the vault and a green cap."""
    k = Kader(richting)
    stijg = hoog - laag
    for u in range(r0 - 1, r1 + 1):
        for v in range(-5, 6):
            x, z = k.p(u, v)
            t = top(x, z)
            for y in range(t + 1, y_top + 3):
                b.set(x, y, z, muursteen(y - laag, y_top + 2 - y))
            b.bezet.add((x, z))
    kap = y_top + 3
    for u in range(r0 - 2, r1 + 2):
        for v in range(-6, 7):
            x, z = k.p(u, v)
            rand = u in (r0 - 2, r1 + 1) or abs(v) == 6
            b.set(x, kap, z, TEGELP if rand else TEGEL, slab() if rand else None)
            if not rand and r0 <= u <= r1 - 1 and abs(v) <= 4:
                b.set(x, kap + 1, z, TEGELP, slab())
    for u in range(r0 - 1, r1 + 1):
        i = max(0, min(stijg, (r1 - 1) - u))
        for v in range(-2, 3):
            x, z = k.p(u, v)
            vloer = laag + i
            for y in range(laag + 1, vloer):
                b.set(x, y, z, KLIPS)
            if 0 < i and (r1 - 1) - u <= stijg:
                b.set(x, vloer, z, KLIPT, stair(k.naar_binnen))
            for y in range(vloer + 1, hoog + (5 if abs(v) == 2 else 6)):
                b.weg(x, y, z)
    for u in (r0 - 1, r1):
        for v in (-1, 0, 1):
            x, z = k.p(u, v)
            b.set(x, hoog + 7, z, MUNT)
    for u in (r0, r1 - 1):
        x, z = k.p(u, 0)
        b.set(x, hoog + 5, z, "minecraft:lantern", lamp(True))
    # the road runs on a few blocks on both sides, so no house stands in the mouth
    for u in list(range(r0 - 6, r0 - 1)) + list(range(r1 + 1, r1 + 6)):
        for v in range(-3, 4):
            b.bezet.add(k.p(u, v))


# =====================================================================================================================
# 3. a spiral stair in a tower
# =====================================================================================================================
def _ring(x0, z0):
    """The sixteen cells around the 3 x 3 core of a 7 x 7 tower, clockwise from the north-west corner."""
    out = [(x0 + 1 + i, z0 + 1) for i in range(4)] + [(x0 + 5, z0 + 1 + i) for i in range(4)]
    out += [(x0 + 5 - i, z0 + 5) for i in range(4)] + [(x0 + 1, z0 + 5 - i) for i in range(4)]
    return out


ZIJDE_MIDDEN = {"north": 2, "east": 6, "south": 10, "west": 14}
LOOP = ("east", "south", "west", "north")       # the way you walk along the cells 0-3, 4-7, 8-11, 12-15


def traptoren(b, x0, z0, y_onder, y_boven, onder, boven, wand, trap, kern=MUUR, dak=None, ramen=True, top_hoog=3):
    """A 7 x 7 tower at (x0, z0) with a square spiral stair around a solid core, from feet level y_onder to feet level
    y_boven. onder / boven = (side, offset): the door in that wall, offset -1, 0 or 1 from its middle (clockwise). The
    stair climbs clockwise, one block per cell; the two doors must fit it (a few flat cells at the foot are fine).
    Everything inside its box is carved first, so it also works as a way down through the island (its walls are the lining).
    Lamps sit in the core. Returns (the cell inside the lower door, the cell inside the upper door)."""
    n = y_boven - y_onder
    c = (ZIJDE_MIDDEN[onder[0]] + onder[1]) % 16
    t = (ZIJDE_MIDDEN[boven[0]] + boven[1]) % 16
    vlak = (t - n - c) % 16
    assert c % 4 and t % 4 and vlak <= 6, f"traptoren {(x0, z0)}: doors {onder} / {boven} do not fit a climb of {n} ({vlak} flat cells)"
    ring = _ring(x0, z0)
    b.hak(x0 + 1, y_onder, z0 + 1, x0 + 5, y_boven + top_hoog - 1, z0 + 5)
    for x in range(x0, x0 + 7):
        for z in range(z0, z0 + 7):
            rand = x in (x0, x0 + 6) or z in (z0, z0 + 6)
            for y in range(y_onder - 1, y_boven + top_hoog + 1):
                if rand or y in (y_onder - 1, y_boven + top_hoog):
                    b.hol.discard((x, y, z))
                    b.set(x, y, z, wand)
    for x in range(x0 + 2, x0 + 5):
        for z in range(z0 + 2, z0 + 5):
            for y in range(y_onder, y_boven + top_hoog):
                b.hol.discard((x, y, z))
                lampje = (x == x0 + 3) != (z == z0 + 3) and (y - y_onder) % 4 == 1
                b.set(x, y, z, "guhs:guh_kristal_lamp" if lampje else kern)
    # the steps: cell (c + vlak + i) holds step i (its block one under the feet level y_onder + i)
    for i in range(1, n + 1):
        j = (c + vlak + i) % 16
        x, z = ring[j]
        y = y_onder - 1 + i
        b.hol.discard((x, y, z))
        if j % 4 == 0 or i == n:
            b.set(x, y, z, kern)
        else:
            b.set(x, y, z, trap, stair(LOOP[(j - 1) // 4]))
        if y - 1 >= y_onder:
            b.hol.discard((x, y - 1, z))
            b.set(x, y - 1, z, kern)
    # the upper floor: everywhere but over the last three steps
    open_ = {(c + vlak + n - i) % 16 for i in (1, 2, 3)}
    for j, (x, z) in enumerate(ring):
        if j not in open_ and j != t:
            b.hol.discard((x, y_boven - 1, z))
            b.set(x, y_boven - 1, z, kern)
    # doors (the wall cell next to the ring cell) and slit windows
    def buiten(j):
        x, z = ring[j]
        zijde = ZIJDEN[j // 4]
        dx, dz = DIRS[zijde]
        return (x + dx, z + dz), zijde

    for j, y in ((c, y_onder), (t, y_boven)):
        (x, z), zijde = buiten(j)
        gb.door(b, x, y, z, OPP[zijde], "spruce")
    if ramen:
        for j in (2, 6, 10, 14):
            (x, z), zijde = buiten(j)
            for y in range(y_onder + 4, y_boven - 2, 5):
                if b.naam(x, y, z) == wand:
                    b.set(x, y, z, "minecraft:iron_bars")
    if dak:
        dak(b, x0, z0, x0 + 6, z0 + 6, y_boven + top_hoog)
    return (ring[c][0], y_onder, ring[c][1]), (ring[t][0], y_boven, ring[t][1])


# =====================================================================================================================
# 4. roofs, houses, small things
# =====================================================================================================================
def pagodedak(b, x0, z0, x1, z1, y, trap=TEGELT, plaat=TEGELP, blok=TEGEL, uit=1, lagen=99, punt=True, dicht=True, onder=None):
    """A hip roof over the box x0..x1, z0..z1 with its eaves `uit` blocks outside it at height y: rings of stairs that climb
    inwards, full blocks with a raised slab on the corners (the turned-up tips), at most `lagen` rings (then it is closed
    flat, unless dicht=False: a skirt roof around a storey). Every ring lies on a ring of `onder` (default: the roof's own
    block), so the roof is one piece and nothing of it hangs on a corner only. Returns the height of its top."""
    ax, az, bx, bz = x0 - uit, z0 - uit, x1 + uit, z1 + uit
    onder = onder or blok
    k = 0

    def steun(x, yy, z):
        if k >= 1 and b.naam(x, yy, z) is None:
            b.set(x, yy, z, onder)

    while ax <= bx and az <= bz and k < lagen:
        if bx - ax < 2 or bz - az < 2:
            for x in range(ax, bx + 1):
                for z in range(az, bz + 1):
                    b.set(x, y + k, z, plaat, slab())
                    steun(x, y + k - 1, z)
            return y + k
        for x in range(ax, bx + 1):
            hoek = x in (ax, bx)
            b.set(x, y + k, az, blok if hoek else trap, None if hoek else stair("south"))
            b.set(x, y + k, bz, blok if hoek else trap, None if hoek else stair("north"))
            steun(x, y + k - 1, az)
            steun(x, y + k - 1, bz)
        for z in range(az + 1, bz):
            b.set(ax, y + k, z, trap, stair("east"))
            b.set(bx, y + k, z, trap, stair("west"))
            steun(ax, y + k - 1, z)
            steun(bx, y + k - 1, z)
        if k == 0 and punt:
            for x, z in ((ax, az), (ax, bz), (bx, az), (bx, bz)):
                b.set(x, y + 1, z, plaat, slab())
        ax, az, bx, bz, k = ax + 1, az + 1, bx - 1, bz - 1, k + 1
    if dicht and ax <= bx and az <= bz:
        for x in range(ax, bx + 1):
            for z in range(az, bz + 1):
                b.set(x, y + k - 1, z, blok)
    return y + k - 1


def zadeldak(b, x0, z0, x1, z1, y, trap, plaat, gevel):
    """A gable roof whose ridge runs along the longer side; its eaves hang one block over at height y (the level of the
    ceiling). The two gable ends are closed with `gevel`."""
    if x1 - x0 >= z1 - z0:
        k = 0
        zn, zs = z0 - 1, z1 + 1
        while zn < zs:
            for x in range(x0 - 1, x1 + 2):
                b.set(x, y + k, zn, trap, stair("south"))
                b.set(x, y + k, zs, trap, stair("north"))
            if k >= 1:
                for z in range(zn + 1, zs):
                    b.set(x0, y + k, z, gevel)
                    b.set(x1, y + k, z, gevel)
            zn, zs, k = zn + 1, zs - 1, k + 1
        if zn == zs:
            for x in range(x0 - 1, x1 + 2):
                b.set(x, y + k, zn, plaat, slab())
    else:
        k = 0
        xw, xo = x0 - 1, x1 + 1
        while xw < xo:
            for z in range(z0 - 1, z1 + 2):
                b.set(xw, y + k, z, trap, stair("east"))
                b.set(xo, y + k, z, trap, stair("west"))
            if k >= 1:
                for x in range(xw + 1, xo):
                    b.set(x, y + k, z0, gevel)
                    b.set(x, y + k, z1, gevel)
            xw, xo, k = xw + 1, xo - 1, k + 1
        if xw == xo:
            for z in range(z0 - 1, z1 + 2):
                b.set(xw, y + k, z, plaat, slab())


def bed(b, x, y, z, facing, kleur="green"):
    """A whole bed: its foot at (x, y, z), its head one block further in `facing`."""
    dx, dz = DIRS[facing]
    b.set(x, y, z, f"minecraft:{kleur}_bed", {"facing": facing, "part": "foot", "occupied": "false"})
    b.set(x + dx, y, z + dz, f"minecraft:{kleur}_bed", {"facing": facing, "part": "head", "occupied": "false"})


def lantaarn(b, x, z, paal=KLIPM, hoog=3, licht="minecraft:lantern"):
    """A street lamp: a post with a lantern on top."""
    k = b.kolom.get((x, z))
    if k is None or (x, z) in b.lantaarns:
        return
    y = k[0]
    if b.naam(x, y + 1, z) is not None:
        return
    for i in range(1, hoog + 1):
        b.set(x, y + i, z, paal)
    b.set(x, y + hoog + 1, z, licht, lamp(False))
    b.bezet.add((x, z))
    b.lantaarns.add((x, z))


def boom(b, x, z, stam="minecraft:birch_log", blad="minecraft:flowering_azalea_leaves", hoog=4):
    """A small round tree."""
    k = b.kolom.get((x, z))
    if k is None:
        return
    y = k[0]
    for i in range(1, hoog + 1):
        b.set(x, y + i, z, stam, {"axis": "y"})
    for dx in range(-2, 3):
        for dy in range(-1, 3):
            for dz in range(-2, 3):
                if dx * dx + dy * dy * 1.6 + dz * dz <= 5.2 and b.naam(x + dx, y + hoog + dy, z + dz) is None:
                    b.set(x + dx, y + hoog + dy, z + dz, blad, {"distance": "1", "persistent": "true", "waterlogged": "false"})
    b.neem(x - 1, z - 1, x + 1, z + 1)


STIJL = {
    # the lower ring: mud and spruce, patched and low
    "onder": dict(muur=[("minecraft:mud_bricks", 5), ("minecraft:packed_mud", 3), ("guhs:kruimelrots_stenen", 3)], plint=None,
                  paal="minecraft:stripped_spruce_log", vloer="minecraft:spruce_planks", plafond="minecraft:spruce_planks",
                  trap="minecraft:spruce_stairs", plaat="minecraft:spruce_slab", raam="minecraft:glass_pane", deur="spruce", h=3,
                  bed=("brown", "gray", "light_gray"), dak2=("minecraft:dark_oak_stairs", "minecraft:dark_oak_slab")),
    # the middle ring: white plaster on a stone foot, dark beams, green tiles
    "midden": dict(muur=[("minecraft:white_terracotta", 6), ("minecraft:calcite", 2)], plint="minecraft:stone_bricks",
                   paal="minecraft:dark_oak_log", vloer="minecraft:birch_planks", plafond="minecraft:dark_oak_planks",
                   trap=TEGELT, plaat=TEGELP, raam="minecraft:glass_pane", deur="dark_oak", h=3, bed=("green", "lime", "white"), dak2=None),
    # the upper ring: pale stone, golden roofs
    "boven": dict(muur=[("minecraft:smooth_sandstone", 1)], plint="minecraft:polished_andesite",
                  paal="minecraft:quartz_pillar", vloer="minecraft:polished_andesite", plafond="minecraft:birch_planks",
                  trap=GOUDT, plaat=GOUDP, raam="minecraft:lime_stained_glass_pane", deur="birch", h=4, bed=("lime", "yellow", "green"), dak2=None),
}
MEUBELS = ["minecraft:crafting_table", "minecraft:bookshelf", "minecraft:barrel", "minecraft:composter", "minecraft:loom", "minecraft:hay_block",
           THEEKIST, "guhs:guh_tafel"]


def huis(b, x0, z0, w, d, stijl, deur, r, meubels=True, bord=None):
    """A small house of w x d at (x0, z0) with its door in the middle of the wall `deur`: floor, walls with corner posts
    and windows, a ceiling, a gable roof, a lantern inside and two under the eaves, and a few things inside (a bed against
    the back wall, something in a corner). bord = the lines of a sign next to its door. Returns the cell in front of its door."""
    S = STIJL[stijl]
    x1, z1 = x0 + w - 1, z0 + d - 1
    gy = b.grond(x0, z0)
    h = S["h"]
    muur = gb.pick(r, S["muur"])
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            hoek = x in (x0, x1) and z in (z0, z1)
            b.set(x, gy, z, (S["plint"] or muur) if rand else S["vloer"])
            if rand:
                for y in range(gy + 1, gy + h + 1):
                    if hoek:
                        b.set(x, y, z, S["paal"], {"axis": "y"})
                    else:
                        b.set(x, y, z, S["plint"] if (S["plint"] and y == gy + 1) else muur)
            b.set(x, gy + h + 1, z, S["plafond"])
    xm, zm = x0 + w // 2, z0 + d // 2
    px, pz = {"north": (xm, z0), "south": (xm, z1), "west": (x0, zm), "east": (x1, zm)}[deur]
    gb.door(b, px, gy + 1, pz, OPP[deur], S["deur"])
    b.set(px, gy + 3, pz, muur)
    for x in range(x0 + 2, x1 - 1, 2):
        for z in (z0, z1):
            if (x, z) != (px, pz) and abs(x - px) + abs(z - pz) > 1:
                b.set(x, gy + 2, z, S["raam"])
    for z in range(z0 + 2, z1 - 1, 2):
        for x in (x0, x1):
            if (x, z) != (px, pz) and abs(x - px) + abs(z - pz) > 1:
                b.set(x, gy + 2, z, S["raam"])
    dak = S["dak2"] if S["dak2"] and r.random() < 0.3 else (S["trap"], S["plaat"])
    zadeldak(b, x0, z0, x1, z1, gy + h + 1, dak[0], dak[1], muur)
    b.set(xm, gy + h, zm, "minecraft:lantern", lamp(True))
    for x, z in ((x0 - 1, z0 - 1), (x1 + 1, z1 + 1)):             # a lantern under the eaves on two corners: the alleys
        b.set(x, gy + h, z, "minecraft:lantern", lamp(True))
    dx, dz = DIRS[deur]
    if meubels:
        # the back wall (opposite the door): a bed along it, and one thing in the far corner
        if deur in ("north", "south"):
            zb = z1 - 1 if deur == "north" else z0 + 1
            bed(b, x0 + 1, gy + 1, zb, "east", r.choice(S["bed"]))
            ding = (x1 - 1, zb)
        else:
            xb = x1 - 1 if deur == "west" else x0 + 1
            bed(b, xb, gy + 1, z0 + 1, "south", r.choice(S["bed"]))
            ding = (xb, z1 - 1)
        m = r.choice(MEUBELS)
        props = {"facing": "up", "open": "false"} if m.endswith("barrel") else {"level": "0"} if m.endswith("composter") else \
            {"facing": OPP[deur]} if m.endswith(("loom", "guh_tafel")) else {"axis": "y"} if m.endswith("hay_block") else None
        if (ding[0], ding[1]) != (px - dx, pz - dz):
            b.set(ding[0], gy + 1, ding[1], m, props)
    if bord:
        sx, sz = (1, 0) if deur in ("north", "south") else (0, 1)
        b.sign(px + sx + dx, gy + 2, pz + sz + dz, deur, bord)
    b.neem(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
    return px + dx, gy + 1, pz + dz


def kraam(zet, x, y, z, heel=True, doek=("minecraft:green_wool", "minecraft:lime_wool"), waar=KOOL):
    """A market stall of 5 x 4 x 4 at (x, y, z) through zet(x, y, z, name, props): four posts, a striped cloth, a table full
    of `waar`. heel=False: what is left of it after the cart came by (the koolman's fate). Only blocks that draw
    themselves from their state: it is also the overlay a later phase shows to one player."""
    paal = {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"}
    if heel:
        for px, pz in ((0, 0), (4, 0), (0, 3), (4, 3)):
            for dy in range(3):
                zet(x + px, y + dy, z + pz, "minecraft:spruce_fence", paal)
        for dx in range(5):
            for dz in range(4):
                zet(x + dx, y + 3, z + dz, doek[dx % 2], None)
        for dx in (1, 2, 3):
            for dz in (1, 2):
                zet(x + dx, y, z + dz, "minecraft:spruce_planks", None)
                zet(x + dx, y + 1, z + dz, waar, None)
        zet(x + 2, y + 2, z + 1, waar, None)
    else:
        zet(x + 4, y, z, "minecraft:spruce_fence", paal)
        zet(x + 4, y + 1, z, "minecraft:spruce_fence", paal)
        for dx, dz in ((0, 0), (1, 0)):
            zet(x + dx, y, z + dz, "minecraft:stripped_spruce_log", {"axis": "x"})
        zet(x, y, z + 2, "minecraft:stripped_spruce_log", {"axis": "z"})
        for dx, dz in ((2, 1), (3, 2)):
            zet(x + dx, y, z + dz, "minecraft:spruce_slab", slab())
        for dx, dz in ((0, 3), (2, 2), (4, 1), (1, 2), (3, 0)):
            zet(x + dx, y, z + dz, waar, None)
        zet(x + 3, y + 1, z + 2, waar, None)
        for dx, dz, kleur in ((2, 3, 0), (3, 3, 1), (4, 3, 0), (4, 2, 1)):
            zet(x + dx, y, z + dz, doek[kleur].replace("_wool", "_carpet"), None)


# =====================================================================================================================
# 5. light for every floor
# =====================================================================================================================
VLOEREN = {KLIP, KLIPS, MUUR, GOUD, "guhs:kaaskorst", "guhs:gebarsten_korst", "guhs:kruimelrots", "guhs:kruimelrots_stenen",
           "minecraft:moss_block", "minecraft:coarse_dirt", "minecraft:cobblestone", "minecraft:stone_bricks", "minecraft:polished_andesite",
           "minecraft:spruce_planks", "minecraft:birch_planks", "minecraft:dark_oak_planks", "minecraft:oak_planks", "minecraft:bamboo_planks",
           "minecraft:smooth_sandstone", "minecraft:smooth_stone", "minecraft:packed_mud", "minecraft:mud_bricks", "minecraft:calcite",
           "minecraft:deepslate_bricks", "minecraft:deepslate_tiles", "minecraft:polished_deepslate", "minecraft:cobbled_deepslate",
           "minecraft:mossy_cobblestone", "minecraft:mossy_stone_bricks", "minecraft:tuff_bricks", "minecraft:polished_tuff", TEGEL,
           "minecraft:white_terracotta", "minecraft:green_terracotta", "minecraft:lime_terracotta", "minecraft:sandstone"}


def vangnet(b, start, min_licht=5):
    """Light for every cell a player can walk to that the lamps of the builders leave darker than min_licht: the floor
    block under it becomes a glowing tile (kristalkaas in a floor, a sea lantern under water); where the floor is no plain
    block (a stair, a slab) an unseen light block goes in the cell itself. Counts what it did in b.vangnet."""
    blocks = b.s.blocks
    doorzichtig = {}

    def dicht(c):
        bl = blocks.get(c)
        if bl is None:
            return False
        n = bl[0]
        v = doorzichtig.get(n)
        if v is None:
            v = doorzichtig[n] = not (n == AIR or n in kit.WATER or b.is_dun(n) or any(t in n for t in gb.TRANSPARENT_SUB) or n in kit.LICHT)
        return v

    light = b.licht()

    def bron(c, sterkte):
        light[c] = sterkte
        todo = deque([c])
        while todo:
            q = todo.popleft()
            l = light[q] - 1
            if l <= 0:
                continue
            x, y, z = q
            for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if light.get(n, 0) < l and b.inside(*n) and not dicht(n):
                    light[n] = l
                    todo.append(n)

    reach = kit.Loper(b).bfs(tuple(start))
    for c in sorted(reach, key=lambda c: (c[1], c[0], c[2])):
        if light.get(c, 0) >= min_licht:
            continue
        x, y, z = c
        nat = b.naam(x, y, z) in kit.WATER
        yy = y - 1
        while nat and b.naam(x, yy, z) in kit.WATER:
            yy -= 1
        onder = b.naam(x, yy, z)
        if onder in VLOEREN:
            blocks[(x, yy, z)] = ("minecraft:sea_lantern" if nat else KRISTALBLOK, {}, None)
            bron((x, yy, z), 15 if nat else 10)
            b.vangnet["zeelantaarn" if nat else "tegel"] = b.vangnet.get("zeelantaarn" if nat else "tegel", 0) + 1
        elif b.naam(x, y, z) is None:
            blocks[c] = (LICHTBLOK, {"level": "15", "waterlogged": "false"}, None)
            bron(c, 15)
            b.vangnet["lichtblok"] = b.vangnet.get("lichtblok", 0) + 1
        elif b.naam(x, y + 1, z) is None:
            blocks[(x, y + 1, z)] = (LICHTBLOK, {"level": "15", "waterlogged": "false"}, None)
            bron((x, y + 1, z), 15)
            b.vangnet["lichtblok"] = b.vangnet.get("lichtblok", 0) + 1
    return reach
