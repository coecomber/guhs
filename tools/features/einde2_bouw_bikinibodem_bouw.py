"""
einde2, slice bouw_bikinibodem: the build of Bikinibodem onder de leegte, part 1: the ground it stands on and everything
that is not a house (called by einde2_bouw_bikinibodem.build; the houses are einde2_bouw_bikinibodem_stad.py).

Bottom to top (template coordinates; the build is 224 x 114 x 224 and lies at world y 4, build contract 6.11):

  - the dish: a round slab of kaaskorst with an irregular rim and a tapering underside; its top is G, you walk at G + 1
    (the level of the road to Schelpenstad);
  - the town on it: a ring road with two cross roads around the plein, the Schelpstraat with the three famous homes south of
    it, the boating course west of it, the Bellenkom north of it and the Kwallenvelden in the four corners;
  - the dome: a flattened soap bubble of guhs:zeepbel over all of it (closed: the ways in are the lift and the town gate);
  - bloemwolken high under the dome;
  - the lift: two columns of guhs:bubbelstroom in tubes of zeepbel, from the plein through the top of the dome up to
  - the little island above the dome, where whoever arrives flying lands (the top of the build);
  - the road east, through the gate in the dome wall to the seam with Schelpenstad (kit.WEG), and the pier next to it.

Stad is kit.Bouw with the things a town needs: digging (hol), furniture, street lamps, and a self-check that knows a
bubbelstroom carries you.
"""
import math
import random

import features.einde2_kit as kit
from features import guheinde_bouw as gb

NAAM = "bikinibodem"
PREFIX = "sign.guhs.einde2_bouw_bikinibodem"
W, H, D = 224, 114, 224
CX, CZ = 106, 112                 # the middle of the plein; the road east runs along z = CZ (the seam: z 108..116)
G = 15                            # the top block of the dish; you walk at G + 1 (kit.WEG["y"])
V = G + 1                         # the walking level
RK, HK = 100.0, 66.0              # the dome: its radius on the ground and its height (top at G + HK = 81)
TI = 107                          # the top block of the island above the dome (you walk at TI + 1)
RI = 15.0                         # ... and its radius
Y_WERELD = kit.LAAG_MIN           # world y of template y 0
ANKER = (104, 0, 104)

KORST, GEBARSTEN, ROTS = "guhs:kaaskorst", "guhs:gebarsten_korst", "guhs:kruimelrots"
STEEN, STEEN_TRAP, STEEN_PLAAT, STEEN_MUUR = "guhs:kruimelrots_stenen", "guhs:kruimelrots_stenen_trap", "guhs:kruimelrots_stenen_plaat", \
    "guhs:kruimelrots_stenen_muur"
KSTEEN, KSTEEN_TRAP, KSTEEN_PLAAT, KSTEEN_MUUR = "guhs:kaaskorst_stenen", "guhs:kaaskorst_stenen_trap", "guhs:kaaskorst_stenen_plaat", \
    "guhs:kaaskorst_stenen_muur"
BEL, STROOM, WOLK = "guhs:zeepbel", "guhs:bubbelstroom", "guhs:bloemwolk"
KORAAL = "guhs:einde2_bouw_bikinibodem_koraal"
PYLON = "guhs:einde2_bouw_bikinibodem_pylon"
PORTRET = "guhs:einde2_bouw_bikinibodem_zelfportret"
KLARINET = "guhs:einde2_bouw_bikinibodem_klarinet"
KLUIS = "guhs:einde2_bouw_bikinibodem_kluis"
KASSA = "guhs:einde2_bouw_bikinibodem_kassa"
CAMERA = "guhs:einde2_bouw_bikinibodem_camera"
ARCHIEF = "guhs:einde2_bouw_bikinibodem_archiefkast"
SAUS = "guhs:kaas_saus"
LAMP = "guhs:guh_kristal_lamp"
# light of our own blocks and of the Sponsguh blocks, for the self-check (kit.LICHT does not know them)
EIGEN_LICHT = {KORAAL: 14, STROOM: 10, WOLK: 10, "guhs:spooklantaarn": 12, "minecraft:redstone_lamp": 0}

# --- the street plan (x0, z0, x1, z1, inclusive) --------------------------------------------------------------------------
PLEIN = (88, 94, 124, 130)
WEGEN = {                                    # every road is 7 wide
    "hoofdstraat_west": (48, 109, 87, 115), "hoofdstraat_oost": (125, 109, 204, 115),
    "bellenlaan_noord": (103, 69, 109, 93), "bellenlaan_zuid": (103, 131, 109, 149),
    "schelpstraat": (58, 150, 160, 156), "noordstraat": (58, 70, 160, 76),
    "westring": (58, 77, 64, 149), "oostring": (154, 77, 160, 149),
}
BAAN = (20, 82, 54, 142)                     # the boating course: a loop of road, 7 wide, along the inside of this box
BRUG = (20, 107, 26, 117)                    # ... with a small bridge in its west leg
# flat ground: the town, the course, the stadium, the homes, the two places east of the ring, the way to the gate
VLAK = [(56, 68, 162, 158), (16, 78, 58, 146), (76, 36, 136, 70), (70, 156, 142, 182), (160, 76, 188, 150), (160, 104, 208, 120)]
VELDEN = {"kwallenveld_1": (44, 160, 68, 184), "kwallenveld_2": (144, 160, 168, 184), "kwallenveld_3": (140, 40, 164, 64),
          "kwallenveld_4": (46, 40, 70, 64)}


def ruis(x, z, schaal, zaad):
    """Smooth value noise in -1..1 (a lattice of hashed values, eased in between)."""
    def punt(i, j):
        n = (i * 374761393 + j * 668265263 + zaad * 1442695041) & 0xFFFFFFFF
        n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
        return ((n ^ (n >> 16)) & 0xFFFF) / 32767.5 - 1.0
    fx, fz = x / schaal, z / schaal
    i, j = math.floor(fx), math.floor(fz)
    u, v = fx - i, fz - j
    u, v = u * u * (3 - 2 * u), v * v * (3 - 2 * v)
    a, b_ = punt(i, j) + (punt(i + 1, j) - punt(i, j)) * u, punt(i, j + 1) + (punt(i + 1, j + 1) - punt(i, j + 1)) * u
    return a + (b_ - a) * v


def straal(x, z):
    return math.hypot(x - CX, z - CZ)


def rand_schotel(x, z):
    """The rim of the dish in this direction (always a little outside the dome)."""
    a = math.atan2(z - CZ, x - CX)
    return 103.3 + 1.2 * math.sin(3 * a + 0.7) + 0.8 * math.sin(7 * a + 2.1) + 0.5 * math.sin(13 * a + 4.0)


def koepel_hoogte(x, z):
    """The highest cell of the dome in this column (G: no dome here)."""
    r2 = ((x - CX) ** 2 + (z - CZ) ** 2) / (RK * RK)
    return G if r2 >= 1.0 else int(math.floor(G + HK * math.sqrt(1.0 - r2)))


def in_rect(x, z, r, marge=0):
    return r[0] - marge <= x <= r[2] + marge and r[1] - marge <= z <= r[3] + marge


class Stroomloper(kit.Loper):
    """The walker of the self-check for this town: a bubbelstroom carries you (up and down, like wading)."""

    def fluid(self, c):
        return self.name(c) == STROOM or super().fluid(c)


class Stad(kit.Bouw):
    """kit.Bouw for Bikinibodem. Extra state: `bebouwd` = the cells (x, z) of houses and other made floors (nothing grows
    there, the street lighting stays out), `wegcel` = the cells of the roads."""

    def __init__(self, h):
        super().__init__(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"] + 50, prefix=PREFIX)
        self.dun |= {STROOM, "guhs:vruchthuis_deur", PYLON, KLARINET, CAMERA, PORTRET}
        self.bebouwd = set()
        self.wegcel = set()
        self.grond = {}          # (x, z) -> the top block of the ground there

    def is_dun(self, n):
        if n.startswith("minecraft:light_"):     # (kit.DUN names "minecraft:light", the light block: that also matches light_blue_* ...)
            return n in self.dun or gb.is_thin(n)
        return super().is_dun(n)

    # --- small tools --------------------------------------------------------------------------------------------------------
    def hol(self, x0, y0, z0, x1, y1, z1):
        """Takes blocks away (the cells become air in the world)."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.s.blocks.pop((x, y, z), None)

    def naam(self, x, y, z):
        bl = self.s.blocks.get((x, y, z))
        return bl[0] if bl else None

    def bouwgrond(self, x0, z0, x1, z1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                self.bebouwd.add((x, z))

    def wanden(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                self.set(x, y, z0, name, props)
                self.set(x, y, z1, name, props)
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                self.set(x0, y, z, name, props)
                self.set(x1, y, z, name, props)

    def deur(self, x, y, z, facing, hout="spruce", hinge="left"):
        if ":" in hout:                      # (a door block of our own: guhs:vruchthuis_deur)
            for half, dy in (("lower", 0), ("upper", 1)):
                self.set(x, y + dy, z, hout, {"facing": facing, "half": half, "hinge": hinge, "open": "false", "powered": "false"})
        else:
            gb.door(self, x, y, z, facing, hout, hinge)

    def trap(self, x, y, z, facing, name="spruce_stairs", half="bottom", shape="straight"):
        self.set(x, y, z, name, {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"})

    def plaat(self, x, y, z, name="spruce_slab", t="bottom"):
        self.set(x, y, z, name, gb.slab(t))

    def stoel(self, x, y, z, kijkt, name="spruce_stairs"):
        """A seat: a stair whose back is behind whoever sits on it looking `kijkt`."""
        self.trap(x, y, z, gb.OPP[kijkt], name)

    def tafel(self, x, y, z, hout="spruce"):
        self.set(x, y, z, f"{hout}_fence")
        self.set(x, y + 1, z, f"{hout}_pressure_plate", {"powered": "false"})

    def lantaarn(self, x, y, z, hangt=True, name="minecraft:lantern"):
        self.set(x, y, z, name, gb.lamp(hangt))

    def luik(self, x, y, z, facing, name="spruce_trapdoor", half="bottom", open_="false"):
        self.set(x, y, z, name, {"facing": facing, "half": half, "open": open_, "powered": "false", "waterlogged": "false"})

    def tapijt(self, x, y, z, kleur):
        self.set(x, y, z, f"{kleur}_carpet")

    def ladder(self, x, y0, y1, z, facing):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, "ladder", {"facing": facing, "waterlogged": "false"})

    def bed(self, x, y, z, facing, kleur="yellow"):
        """A bed: the foot at (x, y, z), the head one block on in `facing`."""
        dx, dz = gb.DIRS[facing]
        self.set(x, y, z, f"{kleur}_bed", {"facing": facing, "part": "foot", "occupied": "false"})
        self.set(x + dx, y, z + dz, f"{kleur}_bed", {"facing": facing, "part": "head", "occupied": "false"})

    def kist(self, x, y, z, facing, loot):
        self.chest(x, y, z, facing, f"guhs:chests/einde2_bouw_bikinibodem_{loot}")

    def sier(self, x, y, z, name, facing=None, **props):
        p = dict(props)
        if facing:
            p["facing"] = facing
        self.set(x, y, z, name, p or None)

    def straatlamp(self, x, z, y=V):
        """A lamp post: a wall post with a lump of gloeikoraal on it."""
        self.set(x, y, z, STEEN_MUUR)
        self.set(x, y + 1, z, STEEN_MUUR)
        self.set(x, y + 2, z, KORAAL)
        self.bebouwd.add((x, z))

    def bol(self, cx, cy, cz, rx, ry, rz, name, props=None, hol=False, onder=None, boven=None):
        """An ellipsoid (hol: only its shell: the cells with a neighbour outside it, corners count, so the shell hangs
        together by faces); cells below `onder` or above `boven` are left out."""
        def binnen(x, y, z):
            return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1.0
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                if onder is not None and y < onder or boven is not None and y > boven:
                    continue
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    if not binnen(x, y, z):
                        continue
                    if hol and all(binnen(x + dx, y + dy, z + dz) for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1)):
                        continue
                    self.set(x, y, z, name, props)

    # --- the self-check: with a walker that knows the lift, the light of our own blocks, and a bubble you see through ---------
    def _eigen(self, f):
        """Runs f with the kit knowing this town: Stroomloper as its walker, the light of our own blocks (kit.LICHT does not
        have them) and zeepbel as see-through as glass (for light and for the cameras). Everything is put back afterwards."""
        oud = (kit.Loper, gb.TRANSPARENT_SUB)
        extra = {k: v for k, v in EIGEN_LICHT.items() if k not in kit.LICHT}
        kit.Loper, gb.TRANSPARENT_SUB = Stroomloper, tuple(gb.TRANSPARENT_SUB) + ("zeepbel",)
        kit.LICHT.update(extra)
        try:
            return f()
        finally:
            kit.Loper, gb.TRANSPARENT_SUB = oud
            for k in extra:
                kit.LICHT.pop(k, None)

    def check(self, start, verwacht=(), min_licht=4, zwevend_ok=None, standaard=True):
        return self._eigen(lambda: kit.Bouw.check(self, start, verwacht, min_licht, zwevend_ok, standaard))

    def licht(self):
        return self._eigen(lambda: kit.Bouw.licht(self))

    def bereik(self, start):
        """Every cell a player can walk to from `start` (the lift carries)."""
        return Stroomloper(self).bfs(tuple(start))


class Hoek:
    """A Stad seen from a corner: x and z are local to (ox, oz), y stays the y of the build. The houses are written this way."""

    def __init__(self, b, ox, oz):
        self.b, self.ox, self.oz = b, ox, oz
        self.rng = b.rng

    def w(self, x, y, z):
        return x + self.ox, y, z + self.oz

    def set(self, x, y, z, *a, **k):
        self.b.set(*self.w(x, y, z), *a, **k)

    def naam(self, x, y, z):
        return self.b.naam(*self.w(x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, *a, **k):
        self.b.fill(*self.w(x0, y0, z0), *self.w(x1, y1, z1), *a, **k)

    def hol(self, x0, y0, z0, x1, y1, z1):
        self.b.hol(*self.w(x0, y0, z0), *self.w(x1, y1, z1))

    def wanden(self, x0, y0, z0, x1, y1, z1, *a, **k):
        self.b.wanden(*self.w(x0, y0, z0), *self.w(x1, y1, z1), *a, **k)

    def bouwgrond(self, x0, z0, x1, z1):
        self.b.bouwgrond(x0 + self.ox, z0 + self.oz, x1 + self.ox, z1 + self.oz)

    def bol(self, cx, cy, cz, *a, **k):
        self.b.bol(cx + self.ox, cy, cz + self.oz, *a, **k)

    def ladder(self, x, y0, y1, z, facing):
        self.b.ladder(x + self.ox, y0, y1, z + self.oz, facing)

    def zone(self, id, x0, y0, z0, x1, y1, z1, wie=""):
        self.b.zone(id, *self.w(x0, y0, z0), *self.w(x1, y1, z1), wie=wie)

    def pad(self, id, punten, wie=""):
        self.b.pad(id, [self.w(*p) for p in punten], wie=wie)

    def camera(self, id, oog, kijk, zicht=(), wie=""):
        self.b.camera(id, self.w(*oog), self.w(*kijk), zicht=zicht, wie=wie)

    def __getattr__(self, name):
        """Everything else takes x, y, z first: deur, trap, plaat, stoel, tafel, lantaarn, luik, tapijt, bed, kist, sier,
        sign, plek, prop, straatlamp (x, z)."""
        f = getattr(self.b, name)
        if name == "straatlamp":
            return lambda x, z, *a, **k: f(x + self.ox, z + self.oz, *a, **k)
        if name in ("plek", "prop"):
            return lambda id, x, y, z, *a, **k: f(id, *self.w(x, y, z), *a, **k)
        return lambda x, y, z, *a, **k: f(*self.w(x, y, z), *a, **k)


# =====================================================================================================================
# the dish, the hills of the Kwallenvelden
# =====================================================================================================================
def is_vlak(x, z):
    return any(in_rect(x, z, r) for r in VLAK)


def grondhoogtes():
    """(x, z) -> the top block of the ground: G where the town is, gentle hills (up to G + 3) in the fields around it,
    back to G at the dome wall."""
    vlak = {(x, z) for x in range(W) for z in range(D) if is_vlak(x, z) or straal(x, z) > 95.5}
    # (the distance to the nearest flat cell, by steps: the hills rise slowly from the edge of town)
    afstand = {c: 0 for c in vlak}
    rand = list(vlak)
    while rand:
        volgende = []
        for (x, z) in rand:
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, z + dz)
                if 0 <= n[0] < W and 0 <= n[1] < D and n not in afstand:
                    afstand[n] = afstand[(x, z)] + 1
                    volgende.append(n)
        rand = volgende
    out = {}
    for x in range(W):
        for z in range(D):
            if straal(x, z) > rand_schotel(x, z):
                continue
            d = afstand[(x, z)]
            heuvel = min(1.0, d / 9.0) * (1.6 + 1.5 * ruis(x, z, 13.0, 77))
            out[(x, z)] = G + max(0, min(3, int(round(heuvel))))
    return out


def schotel(b):
    rng = random.Random(b.rng.random())
    b.grond = grondhoogtes()
    for (x, z), top in b.grond.items():
        r, rd = straal(x, z), rand_schotel(x, z)
        diepte = 2.0 + 11.2 * max(0.0, 1.0 - (r / rd) ** 2) ** 0.65 + 0.9 * ruis(x, z, 9.0, 31)
        onder = max(1, int(round(G - diepte)))
        for y in range(onder, top + 1):
            if y == top:
                naam = GEBARSTEN if ruis(x, z, 6.0, 5) > 0.55 else KORST
            elif y == onder and rng.random() < 0.3:
                naam = ROTS
            else:
                naam = KORST if y > onder + 1 or rng.random() < 0.8 else ROTS
            b.s.blocks[(x, y, z)] = (naam, {}, None)
        # the rim outside the dome: lumps of kruimelrots, so the edge is not a clean cut
        if r > RK + 1.2 and rng.random() < 0.22:
            b.s.blocks[(x, top + 1, z)] = (ROTS, {}, None)
    # drips under the dish: short stalactites of kruimelrots
    for _ in range(260):
        x, z = rng.randrange(W), rng.randrange(D)
        if (x, z) in b.grond and straal(x, z) < 96:
            y = min(yy for yy in range(0, G) if (x, yy, z) in b.s.blocks)
            for d in range(1, rng.randint(1, 3) + 1):
                if y - d >= 1:
                    b.s.blocks[(x, y - d, z)] = (ROTS, {}, None)


# =====================================================================================================================
# the dome and the bloemwolken
# =====================================================================================================================
def koepel(b):
    """One layer of zeepbel: a half ellipsoid on the dish. Closed (the gate and the lift are cut out later)."""
    hoog = {(x, z): koepel_hoogte(x, z) for x in range(-1, W + 1) for z in range(-1, D + 1)}
    n = 0
    for x in range(W):
        for z in range(D):
            h = hoog[(x, z)]
            if h <= G:
                continue
            laagste = min(hoog[(x + 1, z)], hoog[(x - 1, z)], hoog[(x, z + 1)], hoog[(x, z - 1)])
            top = b.grond.get((x, z), G)
            for y in range(max(top + 1, min(laagste, h)), h + 1):      # (one cell of overlap: the shell hangs together by faces)
                b.s.blocks[(x, y, z)] = (BEL, {}, None)
                n += 1
    b.zone("koepel", int(CX - RK), V, int(CZ - RK), int(CX + RK), int(G + HK), int(CZ + RK), wie="alles onder de zeepbel: hier hangt de leegtemist")
    return n


BLOEM = ["..##.##..",
         ".#######.",
         "####.####",
         "###...###",
         ".#.....#.",
         "###...###",
         "####.####",
         ".#######.",
         "..##.##.."]
BLOEM_KLEIN = [".#.#.",
               "#####",
               ".#.#.",
               "#####",
               ".#.#."]
# (centre x, y, z, the plane it lies in, colour, big): spread under the dome, a little under the shell
BLOEMEN = [(70, 62, 80, "y", 0, True), (140, 60, 84, "y", 1, True), (128, 64, 136, "y", 2, True), (76, 58, 146, "y", 3, True),
           (106, 71, 76, "y", 2, False), (96, 70, 132, "y", 1, False), (150, 50, 118, "y", 3, False), (56, 50, 112, "y", 0, False),
           (106, 54, 52, "z", 1, True), (106, 52, 172, "z", 0, True), (48, 46, 112, "x", 2, True), (164, 46, 112, "x", 3, True),
           (146, 40, 62, "y", 0, False), (62, 42, 158, "y", 2, False), (162, 44, 150, "y", 1, False), (60, 44, 64, "y", 3, False),
           (86, 66, 104, "y", 3, False), (124, 67, 100, "y", 0, False)]


def bloemwolken(b):
    """Flower shapes of guhs:bloemwolk high under the dome (the sky of the show): flat ones you see from below, and four
    standing ones over the ends of the two big roads."""
    cellen = set()
    for (cx, cy, cz, vlak, kleur, groot) in BLOEMEN:
        rijen = BLOEM if groot else BLOEM_KLEIN
        half = len(rijen) // 2
        for j, rij in enumerate(rijen):
            for i, ch in enumerate(rij):
                if ch != "#":
                    continue
                u, v = i - half, j - half
                c = {"y": (cx + u, cy, cz + v), "z": (cx + u, cy - v, cz), "x": (cx, cy - v, cz + u)}[vlak]
                if c[1] < koepel_hoogte(c[0], c[2]) - 2:
                    hart = abs(u) + abs(v) <= (2 if groot else 0)
                    b.set(*c, WOLK, {"kleur": str((kleur + 1) % 4 if hart else kleur)})
                    cellen.add(c)
        if groot and vlak == "y":      # (the heart of a big flower: a little cross of the next colour)
            for u, v in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                b.set(cx + u, cy, cz + v, WOLK, {"kleur": str((kleur + 1) % 4)})
                cellen.add((cx + u, cy, cz + v))
    return cellen


# =====================================================================================================================
# the island above the dome and the lift
# =====================================================================================================================
LIFT_OMLAAG = (CX, CZ - 6)        # the middle of the column going down (3 x 3)
LIFT_OMHOOG = (CX, CZ + 6)        # ... and of the one going back up


def eiland(b):
    """The little island above the dome: a lump of kaaskorst with a paved top, where you land (aankomst) and step into the
    bubbelstroom."""
    rng = random.Random(b.rng.random())
    for x in range(CX - 20, CX + 21):
        for z in range(CZ - 20, CZ + 21):
            a = math.atan2(z - CZ, x - CX)
            rr = RI + 1.3 * math.sin(3 * a + 1.0) + 0.8 * math.sin(5 * a + 0.3)
            r = math.hypot(x - CX, z - CZ)
            if r > rr:
                continue
            diepte = 2.0 + 8.5 * (1.0 - (r / rr) ** 2) ** 0.8 + 0.8 * ruis(x, z, 5.0, 91)
            for y in range(int(round(TI - diepte)), TI + 1):
                naam = KORST if y < TI else (KSTEEN if r < rr - 2.5 else KORST)
                if y < TI - 1 and rng.random() < 0.12:
                    naam = ROTS
                b.set(x, y, z, naam)
    # the landing place: a paved square in the middle, a mosaic of the two columns' colours around it
    for x in range(CX - 5, CX + 6):
        for z in range(CZ - 5, CZ + 6):
            b.set(x, TI, z, "guhs:parelmoer_tegels" if (abs(x - CX) == 5 or abs(z - CZ) == 5) else STEEN)
    for (x, z) in ((CX - 5, CZ - 5), (CX + 5, CZ - 5), (CX - 5, CZ + 5), (CX + 5, CZ + 5)):
        b.set(x, TI, z, KORAAL)
    b.set(CX, TI, CZ, KORAAL)
    for (x, z) in ((CX - 9, CZ), (CX + 9, CZ), (CX - 7, CZ - 9), (CX + 7, CZ - 9), (CX - 7, CZ + 9), (CX + 7, CZ + 9)):
        b.straatlamp(x, z, TI + 1)
    b.zone("aankomst", CX - 4, TI + 1, CZ - 4, CX + 4, TI + 6, CZ + 4, wie="het eilandje boven de zeepbel: hier land je")
    b.prop("aankomst_speler", CX, TI + 1, CZ, wie="hier sta je na aankomst")
    b.prop("uitzicht", 252, 74, 176, wie="schuin boven de weg naar Schelpenstad: de hele zeepbel met het eilandje erboven")
    b.sign(CX - 4, TI + 1, CZ - 5, "south", ["Bikinibodem", "onder de leegte", "Naar beneden:", "stap in de bel"], wall=False)
    b.sign(CX + 4, TI + 1, CZ - 5, "south", ["Adem inhouden", "hoeft niet.", "Het is mist,", "geen water, njeg"], wall=False)
    b.sign(CX + 4, TI + 1, CZ + 5, "north", ["Hier kom je", "weer boven.", "Tot bubbels!", ""], wall=False)


def buis(b, cx, cz, y0, y1, richting, open_onder, open_boven):
    """A column of bubbelstroom (3 x 3) in a tube of zeepbel, from y0 up to y1. open_onder / open_boven = the side
    ("north" ...) on which the tube is open at its foot (3 wide, 3 high) and at its top (the top two layers)."""
    for y in range(y0, y1 + 1):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 and abs(dz) == 2:
                    continue
                c = (cx + dx, y, cz + dz)
                if abs(dx) <= 1 and abs(dz) <= 1:
                    b.set(*c, STROOM, {"richting": richting})
                    continue
                kant = "north" if dz == -2 else "south" if dz == 2 else "west" if dx == -2 else "east"
                if kant == open_onder and y <= y0 + 2 or kant == open_boven and y >= y1 - 1:
                    b.s.blocks.pop(c, None)
                    continue
                if y > TI and kant != open_boven:      # (above the island's floor: a low rail of bubbles on the other sides)
                    if y == TI + 1:
                        b.set(*c, BEL)
                    else:
                        b.s.blocks.pop(c, None)
                    continue
                b.set(*c, BEL)


def lift(b):
    """The bubbelstroom lift between the island and the plein: one column down, one back up."""
    (ax, az), (ox, oz) = LIFT_OMLAAG, LIFT_OMHOOG
    # down: you step in on the island from the south (the landing place), and walk out at its foot to the south (the plein)
    buis(b, ax, az, V, TI + 2, "omlaag", "south", "south")
    # up: you step in on the plein from the north, and are puffed off at the top to the north (the landing place)
    buis(b, ox, oz, V, TI + 2, "omhoog", "north", "north")
    for (x, z) in ((ax, az), (ox, oz)):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                b.set(x + dx, G, z + dz, "guhs:parelmoer_tegels" if abs(dx) == 2 or abs(dz) == 2 else KORAAL if (dx, dz) == (0, 0) else "guhs:parelmoer")
                b.bebouwd.add((x + dx, z + dz))
    b.prop("bubbel_boven", ax, TI + 1, az + 2, wie="op het eilandje: hier stap je de bubbelstroom omlaag in")
    b.pad("bubbel_route", [(ax, TI + 1, az), (ax, 82, az), (ax, V, az)], wie="de kolom omlaag, van boven naar beneden")
    b.prop("bubbel_onder", ax, V, az + 3, wie="hier zet de bubbelstroom je neer, op het plein")
    b.prop("bubbel_omhoog", ox, V, oz - 3, wie="de voet van de kolom terug omhoog")
    b.prop("rustpunt_1", ax + 2, V, az + 5, wie="waar de lift je neerzet")
    b.sign(ax - 3, V, az + 3, "south", ["Omlaag", "(je bent er al)", "", "Welkom, njeg!"], wall=False)
    b.sign(ox - 3, V, oz - 3, "north", ["Omhoog", "naar het eiland", "", "Vadsig stijgen"], wall=False)


# =====================================================================================================================
# roads, the plein, the course
# =====================================================================================================================
def weg(b, r, streep="x"):
    """A stretch of road: the deck in the ground, a dashed stripe down its middle."""
    x0, z0, x1, z1 = r
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            midden = (z == (z0 + z1) // 2 and (x // 2) % 2 == 0) if streep == "x" else (x == (x0 + x1) // 2 and (z // 2) % 2 == 0)
            b.set(x, G, z, GEBARSTEN if midden else STEEN)
            b.wegcel.add((x, z))
            b.bebouwd.add((x, z))


def stoep(b):
    """A kerb of kaaskorststenen along every road, in the ground."""
    for (x, z) in list(b.wegcel):
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n not in b.wegcel and b.naam(n[0], G, n[1]) in (KORST, GEBARSTEN) and b.grond.get(n) == G:
                b.set(n[0], G, n[1], KSTEEN)
                b.bebouwd.add(n)


def wegen(b):
    for naam, r in WEGEN.items():
        weg(b, r, "x" if r[2] - r[0] > r[3] - r[1] else "z")
    # the plein: paved, a roundabout around the round middle with the lift
    x0, z0, x1, z1 = PLEIN
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            r = math.hypot(x - CX, z - CZ)
            if r < 3.5:
                naam = "guhs:parelmoer"
            elif r < 9.5:
                naam = "guhs:knuffelklinkers" if int(r) % 2 else "guhs:parelmoer_tegels"
            elif r < 16.5:
                naam = STEEN
            else:
                naam = KSTEEN
            b.set(x, G, z, naam)
            b.bebouwd.add((x, z))
            if 9.5 <= r < 16.5:
                b.wegcel.add((x, z))
    for (dx, dz) in ((0, 0), (-5, -5), (5, -5), (-5, 5), (5, 5), (-8, 0), (8, 0)):
        b.set(CX + dx, G, CZ + dz, KORAAL)
    b.zone("plein", x0, V, z0, x1, V + 8, z1, wie="het Bellenplein, met de lift in het midden")
    b.pad("weg_1", [(48, V, CZ), (88, V, CZ), (93, V, CZ), (97, V, CZ - 9), (CX, V, CZ - 13), (CX + 9, V, CZ - 9), (CX + 13, V, CZ), (124, V, CZ),
                    (204, V, CZ)], wie="de Hoofdstraat, van de vaarschool over het plein (om de lift heen) naar de stadspoort")
    b.pad("weg_2", [(CX, V, 69), (CX, V, 94), (CX, V, CZ - 13), (CX + 9, V, CZ - 9), (CX + 13, V, CZ), (CX + 9, V, CZ + 9), (CX, V, CZ + 13),
                    (CX, V, 130), (CX, V, 150)], wie="de Bellenlaan, van de Bellenkom over het plein naar de Schelpstraat")
    b.pad("weg_3", [(61, V, 73), (157, V, 73), (157, V, 153), (61, V, 153), (61, V, 73)], wie="de ringweg: Noordstraat, Oostring, Schelpstraat, Westring")
    # benches, flower tubs and the signpost in the round middle
    for (dx, dz, kijkt) in ((-6, -3, "east"), (-6, 3, "east"), (6, -3, "west"), (6, 3, "west")):
        b.stoel(CX + dx, V, CZ + dz, kijkt, "minecraft:birch_stairs")
    for (dx, dz) in ((-7, 0), (7, 0)):
        b.set(CX + dx, V, CZ + dz, "minecraft:moss_block")
        b.set(CX + dx, V + 1, CZ + dz, "minecraft:allium")
    b.set(CX, V, CZ, STEEN_MUUR)
    b.set(CX, V + 1, CZ, STEEN_MUUR)
    b.set(CX, V + 2, CZ, KORAAL)
    b.sign(CX, V + 1, CZ - 1, "north", ["Bellenkom", "recht vooruit", "", ""], wall=True)
    b.sign(CX, V + 1, CZ + 1, "south", ["Schelpstraat", "recht vooruit", "", ""], wall=True)
    b.sign(CX + 1, V + 1, CZ, "east", ["Krokante", "Knabbel", "en stadspoort", ""], wall=True)
    b.sign(CX - 1, V + 1, CZ, "west", ["IJssalon en", "vaarschool", "", ""], wall=True)
    # where koning Neptuguh puts his throne when he visits (7 x 7 x 7 free), and where the frozen Meneer Krabguh stands
    tx, tz = 91, 127
    for x in range(tx - 3, tx + 4):
        for z in range(tz - 3, tz + 4):
            rand = abs(x - tx) == 3 or abs(z - tz) == 3
            b.set(x, G, z, "minecraft:prismarine_bricks" if rand else "minecraft:dark_prismarine" if (x + z) % 2 else "guhs:parelmoer_tegels")
    b.set(tx, G, tz, KORAAL)
    b.prop("neptuguh_troon", tx, V, tz, wie="hier zet koning Neptuguh zijn troon neer als hij op bezoek komt (7 x 7 x 7 vrij)")
    b.plek("neptuguh", tx, V, tz, yaw=-135.0, wie="koning Neptuguh", wezen="guhs:neptuguh")
    b.prop("bevroren_krabguh", tx + 5, V, tz + 1, wie="hier staat Meneer Krabguh als ijsklomp (deel 2)")


def baan(b):
    """The boating course of juf Pufguh: a loop of road with pylons, a small bridge over a pond of kaassaus, start and finish."""
    x0, z0, x1, z1 = BAAN
    weg(b, (x0, z0, x1, z0 + 6), "x")
    weg(b, (x0, z1 - 6, x1, z1), "x")
    weg(b, (x0, z0, x0 + 6, z1), "z")
    weg(b, (x1 - 6, z0, x1, z1), "z")
    # the bridge in the west leg: three steps up, a deck, three steps down; under it a pond of kaassaus
    bx0, bz0, bx1, bz1 = BRUG
    for z in range(bz0, bz1 + 1):
        hoog = min(z - bz0, bz1 - z, 2)          # 0, 1, 2, 2, ... 2, 1, 0
        for x in range(bx0, bx1 + 1):
            if hoog:
                for y in range(V, V + hoog):
                    b.set(x, y, z, KSTEEN if y < V + hoog - 1 else STEEN)
            if x in (bx0, bx1) and 1 <= z - bz0 and z <= bz1 - 1:
                b.set(x, V + hoog, z, STEEN_MUUR)
    # (the pond: a closed basin next to and under the bridge, three deep in the dish)
    for z in range(110, 115):
        for x in range(bx1 + 1, bx1 + 8):
            b.set(x, G, z, SAUS, {"level": "0"})
            b.set(x, G - 1, z, SAUS, {"level": "0"})
            b.set(x, G - 2, z, KSTEEN)
            b.bebouwd.add((x, z))
    for z in range(109, 116):
        for x in range(bx1 + 1, bx1 + 9):
            if not (110 <= z <= 114 and x <= bx1 + 7):
                b.set(x, G, z, KSTEEN)
                b.set(x, G - 1, z, KSTEEN)
                b.bebouwd.add((x, z))
    for x in (bx0, bx1):
        b.set(x, V + 3, 112, KORAAL)
    b.zone("examen_brug", bx0, V, bz0, bx1, V + 7, bz1, wie="het bruggetje in het parcours")
    # start and finish on the east leg (next to the school), the route: a loop against the clock
    sx, sz = x1 - 3, 100
    for x in range(x1 - 6, x1 + 1):
        b.set(x, G, sz + 4, "minecraft:white_concrete" if x % 2 else "minecraft:black_concrete")
        b.set(x, G, sz - 5, "minecraft:white_concrete")
    b.prop("examen_start", sx, V, sz, wie="hier staat het vetbootje klaar (5 x 4 x 7 vrij)")
    b.prop("examen_finish", sx, V, sz + 4, wie="de finishlijn: een rondje verder")
    route = [(sx, V, sz), (sx, V, z0 + 3), (x0 + 10, V, z0 + 3), (x0 + 3, V, z0 + 3), (x0 + 3, V, 100), (x0 + 3, V + 2, 112), (x0 + 3, V, 124),
             (x0 + 3, V, z1 - 3), (x0 + 12, V, z1 - 3), (sx - 8, V, z1 - 3), (sx, V, z1 - 3), (sx, V, 120), (sx, V, sz + 4), (sx, V, sz)]
    b.pad("examen_route", route, wie="het pylonenparcours: een rondje tegen de klok in, over het bruggetje")
    # pylons: on the inside of the bends and as a slalom on the south leg
    pylonen = [(x1 - 7, z0 + 7), (x0 + 7, z0 + 7), (x0 + 7, z1 - 7), (x1 - 7, z1 - 7),
               (30, z1 - 5), (35, z1 - 1), (40, z1 - 5), (45, z1 - 1),
               (x0 + 1, 95), (x0 + 5, 101), (x0 + 1, 123), (x0 + 5, 129),
               (x1 - 5, 127), (x1 - 1, 121), (30, z0 + 1), (40, z0 + 5)]
    for i, (x, z) in enumerate(pylonen, 1):
        b.set(x, V, z, PYLON)
        b.prop(f"pylon_{i:02d}", x, V, z, wie="een pylon van het parcours")
    # wink 5: Guhdalf stands in the middle of the bridge; the way round him goes past the pond
    b.plek("knipoog_guhdalf_brug", bx0 + 3, V + 2, 112, yaw=180.0, wie="Guhdalf: gij zult niet passeren zonder rijbewijs", kind="GUHDALF")
    b.pad("knipoog_guhdalf_omweg", [(bx0 + 3, V, 103), (bx1 + 11, V, 103), (bx1 + 11, V, 121), (bx0 + 3, V, 121)],
          wie="om Guhdalf heen: van de baan af, om de sausvijver, en weer terug")
    for z in range(101, 124):                    # (the way round: a strip of road past the pond, 7 wide)
        for x in range(bx1 + 1, bx1 + 15):
            if (101 <= z <= 107 or 117 <= z <= 123 or x >= bx1 + 9) and b.naam(x, G, z) in (KORST, GEBARSTEN):
                b.set(x, G, z, KSTEEN)
                b.bebouwd.add((x, z))
    for (x, z) in ((x0 - 1, z0 - 1), (x1 + 1, z0 - 1), (x0 - 1, z1 + 1), (x1 + 1, z1 + 1), (x0 - 1, 112), (37, z0 - 1), (37, z1 + 1)):
        if straal(x, z) < 96:
            b.straatlamp(x, z)


def straatlampen(b):
    """Lamp posts along the roads of the town, on the kerb, and the bus stop."""
    for (x0, z0, x1, z1) in WEGEN.values():
        if x1 - x0 > z1 - z0:
            for x in range(x0 + 4, x1 - 2, 11):
                for z in (z0 - 1, z1 + 1):
                    if (x, z) not in b.wegcel and not in_rect(x, z, PLEIN) and b.naam(x, V, z) is None:
                        b.straatlamp(x, z)
        else:
            for z in range(z0 + 4, z1 - 2, 11):
                for x in (x0 - 1, x1 + 1):
                    if (x, z) not in b.wegcel and not in_rect(x, z, PLEIN) and b.naam(x, V, z) is None:
                        b.straatlamp(x, z)
    for (x, z) in ((88, 94), (124, 94), (124, 130), (96, 130)):
        if b.naam(x, V, z) is None:
            b.straatlamp(x, z)


# =====================================================================================================================
# the gate, the road east to the seam, the pier where David Hasselguh sets you down
# =====================================================================================================================
def poort(b):
    w = kit.WEG
    z0, z1 = w["z_bikini"]
    midden = (z0 + z1) // 2
    # the causeway from the dome to the seam: the profile of kit.WEG from the gate on
    for x in range(200, W):
        for z in range(z0, z1 + 1):
            for y in range(G + 1, G + 1 + w["vrij"] + 2):
                if x >= 207:
                    b.s.blocks.pop((x, y, z), None)
            if x < 205:
                continue
            b.set(x, G, z, w["streep"] if z == midden else w["dek"])
            for d in range(1, 5):
                dik = 4 if x < 216 else w["onder"] + (1 if abs(z - midden) < 3 else 0)
                if d <= dik:
                    b.set(x, G - d, z, w["grond"])
            if z in (z0, z1) and x >= 208:
                b.set(x, G + 1, z, w["rand"])
            b.bebouwd.add((x, z))
            b.wegcel.add((x, z))
    # the gate: an arch of kruimelrotsstenen in the dome wall (the opening 7 wide, 6 high), a shell on its top
    gx = 204
    for x in range(gx, gx + 4):
        for z in range(z0 - 2, z1 + 3):
            for y in range(V, V + 10):
                binnen = z0 + 1 <= z <= z1 - 1 and y <= V + 5
                boog = y <= V + 9 - max(0, abs(z - midden) - 3)
                if binnen:
                    b.s.blocks.pop((x, y, z), None)
                elif boog:
                    b.set(x, y, z, KSTEEN if (y == V + 6 or z in (z0, z1)) else STEEN)
    for z in (z0, z1):
        b.set(gx - 1, V + 3, z, KORAAL)
        b.set(gx + 4, V + 3, z, KORAAL)
    b.set(gx - 1, V + 7, midden, KORAAL)
    b.set(gx + 4, V + 7, midden, KORAAL)
    b.sign(gx - 1, V + 6, midden, "west", ["Tot bubbels!", "Schelpenstad:", "rechtdoor en", "niet omkijken"], wall=True)
    b.sign(gx + 4, V + 6, midden, "east", ["Welkom in", "Bikinibodem", "onder de leegte", "Njeg!"], wall=True)
    b.prop("stadspoort", gx + 1, V, midden, wie="de poort in de zeepbelwand, op de weg naar het oosten")
    b.prop("rustpunt_3", gx - 4, V, midden + 2, wie="bij de stadspoort")
    b.pad("weg_naar_schelpenstad", [(gx + 1, V, midden), (215, V, midden), (223, V, midden)], wie="van de poort naar de naad met Schelpenstad")
    # the pier south of the causeway: David Hasselguh sets you down here at the end of deel 2 (9 x 6 x 13 free)
    px0, pz0, px1, pz1 = 208, z1 + 1, 220, z1 + 10
    for x in range(px0, px1 + 1):
        for z in range(pz0, pz1 + 1):
            rand = x in (px0, px1) or z == pz1
            b.set(x, G, z, KSTEEN if rand else STEEN)
            for d in range(1, 4 if not rand else 3):
                b.set(x, G - d, z, KORST)
            for y in range(V, V + 7):                # (no lumps of the rim on the pier)
                b.s.blocks.pop((x, y, z), None)
            b.bebouwd.add((x, z))
    for x in range(px0, px1 + 1):               # (the kerb between the road and the pier opens here)
        if 211 <= x <= 215:
            b.s.blocks.pop((x, G + 1, z1), None)
    for (x, z) in ((px0, pz1), (px1, pz1)):
        b.set(x, V, z, STEEN_MUUR)
        b.set(x, V + 1, z, KORAAL)
    for (x, z) in ((211, z0 - 1), (217, z0 - 1), (222, z0 - 1), (222, z1 + 1)):      # light beside the causeway (nothing may stand on it)
        b.set(x, G, z, KORST)
        b.set(x, G - 1, z, KORST)
        b.set(x, V, z, KORAAL)
    b.prop("hasselguh_landing", 214, V, pz0 + 4, wie="de steiger naast de weg: hier zet David Hasselguh je af (9 x 6 x 13 vrij)")
    b.sign(px0, V, pz0 + 1, "west", ["Steiger", "Alleen voor", "reusachtige", "zwemmende guhs"], wall=False)


# =====================================================================================================================
# the Kwallenvelden and what grows on the dish
# =====================================================================================================================
def velden(b):
    for i, (naam, (x0, z0, x1, z1)) in enumerate(VELDEN.items()):
        b.zone(naam, x0, V, z0, x1, V + 12, z1, wie="een Kwallenveld: hier komen de Kruimelkwallen")
    # Patguh waits with the nets at the edge of the first field
    x0, z0, x1, z1 = VELDEN["kwallenveld_1"]
    kx, kz = x1 + 1, z0 - 1
    for x in range(kx - 2, kx + 3):
        for z in range(kz - 2, kz + 3):
            b.set(x, b.grond[(x, z)], z, KSTEEN)
            b.bebouwd.add((x, z))
    y = b.grond[(kx, kz)] + 1
    b.plek("kwallen_patguh", kx, y, kz, yaw=45.0, wie="Patguh met de netjes", wezen="guhs:patguh")
    b.prop("kwallen_start", kx - 1, y, kz + 1, wie="hier begint het kwallen vangen")
    b.set(kx + 2, y, kz - 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.sign(kx + 2, y + 1, kz - 2, "west", ["Kwallenvelden", "Vangen mag,", "aaien ook.", "Jam: later"], wall=False)
    b.straatlamp(kx - 2, kz - 2, y)
    # wink 7: a Rookguh floats among the kwallen and away; Patguh catches it, proud
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    gy = max(b.grond[(x, z)] for x in range(cx - 4, cx + 5) for z in range(cz - 4, cz + 5)) + 1
    b.pad("knipoog_rookguh_zweef", [(cx - 6, gy + 4, cz + 4), (cx, gy + 3, cz), (cx + 3, gy + 5, cz - 5), (cx + 8, gy + 10, cz - 10)],
          wie="een Rookguh zweeft tussen de kwallen door, en dan met net en al weg")
    px, pz = cx + 1, cz + 3
    b.plek("knipoog_rookguh_patguh", px, b.grond[(px, pz)] + 1, pz, yaw=160.0, wie="Patguh met zijn net: dit is de grootste!", wezen="guhs:patguh")
    b.camera("knipoog_rookguh", (cx + 9, gy + 4, cz + 9), (cx, gy + 3, cz), zicht=["knipoog_rookguh_patguh"])


def begroeiing(b):
    """Korstgras, glowing schimmelpaddenstoelen and dry bushes on the open ground (thick in the fields, thin elsewhere)."""
    rng = random.Random(b.rng.random())
    for (x, z), top in sorted(b.grond.items()):
        if (x, z) in b.bebouwd or straal(x, z) > RK - 2 or b.naam(x, top, z) not in (KORST, GEBARSTEN) or b.naam(x, top + 1, z) is not None:
            continue
        veld = not is_vlak(x, z)
        r = rng.random()
        if r < (0.20 if veld else 0.05):
            b.set(x, top + 1, z, "guhs:korstgras")
        elif r < (0.215 if veld else 0.056):
            b.set(x, top + 1, z, "guhs:schimmelpaddenstoel")
        elif r < (0.225 if veld else 0.06):
            b.set(x, top + 1, z, "guhs:dorre_korststruik")


def verlicht(b, start):
    """Street lighting for whatever open ground is still dark: lumps of gloeikoraal in the ground, as few as it takes.
    Returns how many it put down. (Inside the houses the builder lights the rooms himself.)"""
    reach = b.bereik(start)
    licht = b.licht()
    blocks = b.s.blocks

    def dicht(c):
        bl = blocks.get(c)
        if bl is None:
            return False
        n = bl[0]
        return n != gb.AIR and n not in kit.WATER and n != BEL and not b.is_dun(n) and not any(t in n for t in gb.TRANSPARENT_SUB) \
            and n not in kit.LICHT and n not in EIGEN_LICHT

    def gloei(bron, sterkte):
        licht[bron] = sterkte
        todo = [bron]
        while todo:
            volgende = []
            for c in todo:
                l = licht[c] - 1
                if l <= 0:
                    continue
                x, y, z = c
                for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                    n = (x + dx, y + dy, z + dz)
                    if b.inside(*n) and licht.get(n, 0) < l and not dicht(n):
                        licht[n] = l
                        volgende.append(n)
            todo = volgende

    def mag(c):                                  # may a lump lie in the floor under this cell?
        x, y, z = c
        onder = blocks.get((x, y - 1, z), ("",))[0]
        if y > 60:                               # (the island above the dome: all of it is open ground)
            return onder in (KORST, KSTEEN)
        if (x, z) in b.wegcel:
            return False
        if (x, z) in b.bebouwd:
            return onder in (KSTEEN, ROTS) and blocks.get((x, y, z)) is None
        return onder in (KORST, GEBARSTEN, KSTEEN, ROTS)

    def leg(c):
        x, y, z = c
        blocks.pop(c, None)                      # (a tuft of grass makes way)
        blocks[(x, y - 1, z)] = (KORAAL, {}, None)
        gloei((x, y - 1, z), EIGEN_LICHT[KORAAL])

    n = 0
    # (first a loose grid over the open ground, so the lumps lie evenly)
    for c in sorted(reach):
        if c[0] % 9 == 4 and c[2] % 9 == 4 and licht.get(c, 0) < 7 and mag(c):
            leg(c)
            n += 1
    # (then whatever is still dark: a lump in the nearest ground that may hold one, at most five steps away)
    for c in sorted(reach):
        if licht.get(c, 0) >= 5:
            continue
        x, y, z = c
        beste = None
        for dx in range(-5, 6):
            for dz in range(-5 + abs(dx), 6 - abs(dx)):
                for dy in (0, -1, 1):
                    k = (x + dx, y + dy, z + dz)
                    if k in reach and mag(k) and (beste is None or abs(dx) + abs(dz) < beste[0]):
                        beste = (abs(dx) + abs(dz), k)
        if beste is None and (x, z) in b.wegcel and x < 205 and blocks.get((x, y - 1, z), ("",))[0] in (STEEN, GEBARSTEN):
            beste = (0, c)                       # (a crossing, far from every kerb: a glowing stud in the road itself)
        if beste:
            leg(beste[1])
            n += 1
    return n
