"""
einde2, slice bouw_lucht: the build of the Zuidelijke Luchttempel (DESIGN 5, chapter 1), a guhs:leegte_bouw of 6 x 6 tiles
that brings its own floating island. Called by einde2_bouw_lucht.build(h).

The island (192 x 116 x 192, template y 0 = world y 138): a hollow shell of kruimelrots with a top of kaaskorst. On it,
from south to north: the landing terrace with the team's camp (y 40), the stairs and the tempelpoort up to the plateau
(y 48), the forecourt, the Hal van de Guhvatars on its podium (y 52) with the great tower on its roof, the two towers
north of it (the monnikentoren and the windtoren, each with a bridge to the roof of the hall) and Appa's stable between
them. West: Guhatso's taartenterras above the luchtbalveld (a sunken court of poles in a bed of cloud). East: the
Werktuigguh's workshop behind its closed door, with the building site of the Vredesballon in a walled yard. The
luchtscooter-baan runs along the rim from the south-west round the north to the north-east; the zweefringen hang in a
wide loop around the towers.

No story logic and no characters: every spot a later phase needs is a marker (build contract 6.7), written to
data/guhs/einde2/plekken/zuidelijke_luchttempel.json. The five stages of the Vredesballon are overlay templates
(einde2_bouw_lucht_ballon.py).
"""
import math

from features import guheinde_bouw as gb
import features.einde2_kit as kit

NAAM = "zuidelijke_luchttempel"
W, H, D = 192, 116, 192
Y_WERELD = 138                 # the world y of template y 0 (the build then reaches y 253)
ANKER = (88, 0, 88)
A, G, HV = 40, 48, 52          # the floor blocks of the landing terrace, the plateau and the hall
MX, MZ = 96, 90                # the middle of the plateau
BAAN = (77, 73)                # the half axes of the luchtscooter-baan (an arc of an ellipse around the middle)
BAAN_VAN, BAAN_TOT = 140, 330  # degrees (0 = east, 90 = south)
P = "sign.guhs.einde2_bouw_lucht"
AIR = gb.AIR

KORST, ROTS, KLIP = "guhs:kaaskorst", "guhs:kruimelrots", "guhs:klipkaas"
RSTEEN, RTRAP, RPLAAT, RMUUR = "guhs:kruimelrots_stenen", "guhs:kruimelrots_stenen_trap", "guhs:kruimelrots_stenen_plaat", "guhs:kruimelrots_stenen_muur"
WOLK, KRISTAL, STAAFJE = "guhs:kruimelwolk", "guhs:kristalkaas_kristal", "guhs:lichtstaafje"
TEGEL, PIJL, TAART, TANDWIEL, BLAUWDRUK = ("guhs:einde2_bouw_lucht_tegel", "guhs:einde2_bouw_lucht_pijltegel", "guhs:einde2_bouw_lucht_taart",
                                           "guhs:einde2_bouw_lucht_tandwiel", "guhs:einde2_bouw_lucht_blauwdruk")
DEUR = "guhs:werkplaatsdeur"
KNABBELS = "guhs:block_of_kaasknabbels"
WIT = [("calcite", 82), ("diorite", 8), ("polished_diorite", 10)]
BAND, ZUIL = "quartz_bricks", "quartz_pillar"
DAK, DAKTRAP, DAKPLAAT = "oxidized_cut_copper", "oxidized_cut_copper_stairs", "oxidized_cut_copper_slab"
NOK, NOKPLAAT = "dark_prismarine", "dark_prismarine_slab"
PLANK, PAAL, HEK = "birch_planks", "stripped_birch_log", "birch_fence"
PLEIN = "smooth_stone"
PAD = [("smooth_stone", 70), ("polished_andesite", 16), ("stone", 14)]
GLAS = "light_blue_stained_glass"
RINGBLOK = "ochre_froglight"
NATUUR = (KORST, ROTS, KLIP)
CCW = {"north": "west", "west": "south", "south": "east", "east": "north"}
LINKS = {"south": (1, 0), "north": (-1, 0), "east": (0, -1), "west": (0, 1)}   # facing -> the statue's own left, as (dx, dz)


class Tempel(kit.Bouw):
    """The build in the making: kit.Bouw plus what this one temple needs everywhere."""

    def __init__(self, h):
        super().__init__(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=P)
        self.dun.add(WOLK)              # (everybody sinks through a kruimelwolk: so does the walker)
        self.ringen = set()             # the cells of the zweefringen (they hang in the air on purpose)
        self.top = None                 # top[x][z] = the y of the island's top block (-1: no island)
        self.binnen = []                # boxes the walker must NOT reach from the landing (behind the closed door)
        self.palen = []                 # the pole tops of the luchtbalveld
        self.auto_licht = {}            # kind of light -> how many the light pass added
        self.vrij = []                  # boxes that stay free of everything (a landing, where Appa lies): no crystal grows there

    def is_dun(self, n):
        # (the kit's list of thin blocks matches by substring and holds "minecraft:light", the light block: without this a
        # light blue window, bed or block of concrete would count as something you walk through)
        for kleur in ("minecraft:light_blue_", "minecraft:light_gray_"):
            if n.startswith(kleur):
                n = "minecraft:" + n[len(kleur):]
        return super().is_dun(n)

    def wit(self):
        return gb.pick(self.rng, WIT)

    def leeg(self, x0, y0, z0, x1, y1, z1):
        """Nothing here (unset = air): takes away what was set."""
        blocks = self.s.blocks
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    blocks.pop((x, y, z), None)

    def muur(self, x0, y0, z0, x1, y1, z1):
        """A white wall (the mix of the temple)."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, self.wit())

    def naam(self, x, y, z):
        bl = self.s.blocks.get((x, y, z))
        return bl[0] if bl else None

    def lantaarn(self, x, z, y=None, hoog=2):
        """A lamp post: a little wall of kruimelrotsstenen with a lantern on top."""
        y = self.top[x][z] + 1 if y is None else y
        for i in range(hoog):
            self.set(x, y + i, z, RMUUR)
        self.set(x, y + hoog, z, "lantern", gb.lamp(False))

    def hanglamp(self, x, y, z, ketting=1):
        """A lantern on a chain under the block at y + 1."""
        for i in range(ketting):
            self.set(x, y - i, z, "chain", {"axis": "y", "waterlogged": "false"})
        self.set(x, y - ketting, z, "lantern", gb.lamp(True))

    def bord(self, x, y, z, facing, lines, wall=True):
        self.sign(x, y, z, facing, lines, wall=wall)


# =====================================================================================================================
# the island
# =====================================================================================================================
def eiland(b):
    """The floating rock: the plateau (an ellipse with a wobbly rim), the lobe of the building site in the east and the
    lower tongue of the landing terrace in the south; a tapering underside with a few hanging lumps; hollow inside."""
    import numpy as np
    xs, zs = np.meshgrid(np.arange(W), np.arange(D), indexing="ij")
    ang = np.arctan2(zs - MZ, xs - MX)
    ruis = 1.5 * np.sin(3 * ang + 0.7) + 1.0 * np.sin(5 * ang + 2.1) + 0.5 * np.sin(9 * ang + 4.0)
    hoofd = ((xs - MX) / (84 + ruis)) ** 2 + ((zs - MZ) / (80 + ruis)) ** 2 <= 1
    klif = np.where((xs >= 84) & (xs <= 108), 156, 156 + np.round(1.3 * np.sin(xs / 5.0)).astype(int))
    plateau = hoofd & (zs <= klif)
    ang2 = np.arctan2(zs - 124, xs - 152)
    r2 = np.sin(4 * ang2 + 1.0)
    lob = ((xs - 152) / (28 + r2)) ** 2 + ((zs - 124) / (24 + r2)) ** 2 <= 1
    ang3 = np.arctan2(zs - 170, xs - 88)
    r3 = 1.2 * np.sin(4 * ang3 + 0.3) + 0.8 * np.sin(7 * ang3 + 1.9)
    tong = (((xs - 88) / (40 + r3)) ** 2 + ((zs - 170) / (18 + r3)) ** 2 <= 1) & (zs > klif)
    top = np.full((W, D), -1)
    top[tong] = A
    top[plateau | lob] = G
    foot = top >= 0
    foot[0, :] = foot[-1, :] = False
    foot[:, 0] = foot[:, -1] = False
    top[~foot] = -1

    def krimp(m, acht=False):
        e = m.copy()
        e[1:, :] &= m[:-1, :]
        e[:-1, :] &= m[1:, :]
        e[:, 1:] &= m[:, :-1]
        e[:, :-1] &= m[:, 1:]
        if acht:
            e[1:, 1:] &= m[:-1, :-1]
            e[1:, :-1] &= m[:-1, 1:]
            e[:-1, 1:] &= m[1:, :-1]
            e[:-1, :-1] &= m[1:, 1:]
        e[0, :] = e[-1, :] = False
        e[:, 0] = e[:, -1] = False
        return e

    d, cur = np.zeros((W, D)), foot.copy()
    while cur.any():
        d += cur
        cur = krimp(cur)
    diepte = 3 + 34 * (1 - np.exp(-d / 24.0)) + 2.5 * np.sin(xs / 6.0) * np.cos(zs / 7.0) + 1.5 * np.sin(xs / 3.1 + zs / 4.3)
    for px, pz, amp, sig in ((96, 92, 9, 14), (60, 72, 8, 9), (128, 106, 9, 10), (88, 170, 7, 8), (152, 126, 7, 9), (40, 110, 6, 7)):
        diepte += amp * np.exp(-((xs - px) ** 2 + (zs - pz) ** 2) / (2.0 * sig * sig))
    bot = np.maximum(top - np.round(np.clip(diepte, 3, None)).astype(int), 2)
    rng = b.rng
    for y in range(2, G + 1):
        vol = foot & (bot <= y) & (y <= top)
        if not vol.any():
            continue
        diep = krimp(krimp(vol, True), True) & (y <= top - 3) & (y >= bot + 2)
        for x, z in np.argwhere(vol & ~diep):
            x, z, t = int(x), int(z), int(top[x, z])
            if y == t:
                blok = KORST
            elif y >= t - 2:
                blok = ROTS if rng.random() < 0.75 else KORST
            elif (y + int(2 * math.sin(x / 9.0) + 2 * math.cos(z / 11.0))) % 9 in (0, 1):
                blok = KLIP
            else:
                blok = ROTS if rng.random() < 0.92 else KORST
            b.s.blocks[(x, y, z)] = (blok, {}, None)
    b.top = [[int(top[x, z]) for z in range(D)] for x in range(W)]
    b.bot = [[int(bot[x, z]) for z in range(D)] for x in range(W)]
    b.rand_afstand = [[int(d[x, z]) for z in range(D)] for x in range(W)]


def wolken(b):
    """Puffs of kruimelwolk that hang against the underside and the cliffs of the island (they touch the rock: nothing floats)."""
    rng = b.rng
    randen = [(x, z) for x in range(4, W - 4) for z in range(4, D - 4) if b.rand_afstand[x][z] == 1]
    for _ in range(26):
        x, z = rng.choice(randen)
        y = rng.randint(min(max(b.bot[x][z] + 1, b.top[x][z] - 16), b.top[x][z] - 4), b.top[x][z] - 4)
        dx, dz = x - MX, z - MZ
        n = math.hypot(dx, dz) or 1.0
        for i in range(rng.randint(3, 5)):
            cx, cz = x + dx / n * (1.5 + i * 1.6) + rng.uniform(-1, 1), z + dz / n * (1.5 + i * 1.6) + rng.uniform(-1, 1)
            rx, rz = rng.uniform(2.0, 3.6), rng.uniform(2.0, 3.6)
            for ax in range(int(cx - rx - 1), int(cx + rx + 2)):
                for az in range(int(cz - rz - 1), int(cz + rz + 2)):
                    for ay in (y, y + 1):
                        q = ((ax - cx) / rx) ** 2 + ((az - cz) / rz) ** 2 + (0.35 if ay > y else 0.0)
                        if q <= 1 and b.inside(ax, ay, az) and (ax, ay, az) not in b.s.blocks:
                            b.set(ax, ay, az, WOLK)
    # (a puff that ended up apart from the rock is taken away again in opruimen())


# =====================================================================================================================
# building blocks of the temple
# =====================================================================================================================
def plein(b, x0, z0, x1, z1, y, blok=PLEIN, rand=None):
    """A paved floor (with a border of another block)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            aan_rand = rand and (x in (x0, x1) or z in (z0, z1))
            b.set(x, y, z, rand if aan_rand else blok)
            b.leeg(x, y + 1, z, x, y + 3, z)


def pad(b, punten, breed=3, lampen=True):
    """A path over the natural ground of the plateau (only kaaskorst becomes path), with a lamp post now and then."""
    r = breed // 2
    stap = 0
    for (x0, z0), (x1, z1) in zip(punten, punten[1:]):
        n = max(abs(x1 - x0), abs(z1 - z0))
        for i in range(n + 1):
            x, z = round(x0 + (x1 - x0) * i / max(1, n)), round(z0 + (z1 - z0) * i / max(1, n))
            for ax in range(x - r, x + r + 1):
                for az in range(z - r, z + r + 1):
                    y = b.top[ax][az]
                    if y >= 0 and b.naam(ax, y, az) in NATUUR and b.naam(ax, y + 1, az) is None:
                        b.set(ax, y, az, gb.pick(b.rng, PAD))
            stap += 1
            if lampen and stap % 11 == 5:
                horizontaal = abs(x1 - x0) >= abs(z1 - z0)
                lx, lz = (x, z + r + 1) if horizontaal else (x + r + 1, z)
                y = b.top[lx][lz]
                if y >= 0 and b.naam(lx, y, lz) in NATUUR and b.naam(lx, y + 1, lz) is None:
                    b.lantaarn(lx, lz)


def schilddak(b, x0, z0, x1, z1, y, hoog=None):
    """A hipped roof of copper stairs over the box x0..x1, z0..z1 from height y (trapvormen() turns the corners);
    after `hoog` layers it ends in a flat top. Returns the y of its top."""
    i = 0
    while x0 + i <= x1 - i and z0 + i <= z1 - i:
        xa, xb, za, zb = x0 + i, x1 - i, z0 + i, z1 - i
        plat = hoog is not None and i >= hoog
        if xb - xa < 2 or zb - za < 2 or plat:
            for x in range(xa, xb + 1):
                for z in range(za, zb + 1):
                    b.set(x, y + i, z, NOK if plat else NOKPLAAT, None if plat else gb.slab())
            return y + i
        for x in range(xa, xb + 1):
            b.set(x, y + i, za, DAKTRAP, gb.stair("south"))
            b.set(x, y + i, zb, DAKTRAP, gb.stair("north"))
        for z in range(za + 1, zb):
            b.set(xa, y + i, z, DAKTRAP, gb.stair("east"))
            b.set(xb, y + i, z, DAKTRAP, gb.stair("west"))
        for x in range(xa + 1, xb):                    # (the block under the next layer: the roof is one piece)
            for z in range(za + 1, zb):
                if x in (xa + 1, xb - 1) or z in (za + 1, zb - 1):
                    b.set(x, y + i, z, DAK)
        i += 1
    return y + i


def kegeldak(b, cx, cz, r, y, hoog, punt=True):
    """The roof of a round tower: an eave of slabs that sticks out, a hollow cone that curves inwards, a golden tip."""
    def straal(i):
        return (r + 1.6) * (1 - i / float(hoog)) ** 1.5 + 0.6
    for x in range(cx - r - 3, cx + r + 4):
        for z in range(cz - r - 3, cz + r + 4):
            d = math.hypot(x - cx, z - cz)
            if r + 0.5 < d <= r + 2.6:
                b.set(x, y, z, NOKPLAAT, gb.slab("top"))
            for i in range(hoog):
                if straal(i + 1) - 1.2 < d <= straal(i):
                    b.set(x, y + 1 + i, z, NOK if i % 5 == 4 else DAK)
    if punt:
        b.set(cx, y + hoog + 1, cz, NOK)
        b.set(cx, y + hoog + 2, cz, "gold_block")
        b.set(cx, y + hoog + 3, cz, "end_rod", {"facing": "up"})
    return y + hoog + 3


def boom(b, x, z, hoog=5, appels=True):
    """A little apple tree (the leaves stay: persistent)."""
    y = b.top[x][z] + 1
    for i in range(hoog):
        b.set(x, y + i, z, "oak_log", {"axis": "y"})
    blad = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for dy in range(hoog - 2, hoog + 2):
                rr = 2.6 if dy < hoog else 1.6
                if math.hypot(dx, dz) <= rr and (dx, dz) != (0, 0) or (dy >= hoog and (dx, dz) == (0, 0)):
                    if (x + dx, y + dy, z + dz) not in b.s.blocks:
                        rood = appels and b.rng.random() < 0.09 and dy < hoog
                        b.set(x + dx, y + dy, z + dz, "red_terracotta" if rood else "oak_leaves", None if rood else blad)


def rustpunt(b, nr, x, y, z, tekst=("Rustpunt", "Even zitten.", "Even vadsen.", "Dan weer door.")):
    """A rest point: a little round floor of temple tiles with a free middle (prop rustpunt_<nr>: where a later phase puts
    its rest fire), a bench and a lamp."""
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) + abs(dz) <= 3:
                b.set(x + dx, y - 1, z + dz, TEGEL if (dx, dz) != (0, 0) else "chiseled_quartz_block")
                b.leeg(x + dx, y, z + dz, x + dx, y + 3, z + dz)
    b.set(x - 2, y, z - 1, "birch_stairs", gb.stair("west"))
    b.set(x - 2, y, z, "birch_stairs", gb.stair("west"))
    b.set(x - 2, y, z + 1, "birch_stairs", gb.stair("west"))
    b.lantaarn(x + 2, z - 2, y=y)
    b.bord(x + 2, y, z + 2, "west", list(tekst), wall=False)
    b.prop(f"rustpunt_{nr}", x, y, z, wie="rustpunt: hier komt het rustvuur")


def trapvormen(b):
    """Every stair gets the shape the game would give it next to its neighbours (the tiles keep the block states of the
    template: build contract 2.6). The rule is the game's own (StairBlock.getStairsShape)."""
    blocks = b.s.blocks

    def trap_op(c):
        bl = blocks.get(c)
        return bl if bl and (bl[0].endswith("_stairs") or bl[0].endswith("_trap")) and "facing" in bl[1] else None

    def kan(c, st, kant):
        dx, dz = gb.DIRS[kant]
        o = trap_op((c[0] + dx, c[1], c[2] + dz))
        return o is None or o[1]["facing"] != st[1]["facing"] or o[1]["half"] != st[1]["half"]

    dwars = lambda f, g: (f in ("north", "south")) != (g in ("north", "south"))  # noqa: E731
    nieuw = {}
    for c, bl in blocks.items():
        if not trap_op(c):
            continue
        f = bl[1]["facing"]
        dx, dz = gb.DIRS[f]
        vorm = "straight"
        voor = trap_op((c[0] + dx, c[1], c[2] + dz))
        if voor and voor[1]["half"] == bl[1]["half"] and dwars(f, voor[1]["facing"]) and kan(c, bl, gb.OPP[voor[1]["facing"]]):
            vorm = "outer_left" if voor[1]["facing"] == CCW[f] else "outer_right"
        else:
            achter = trap_op((c[0] - dx, c[1], c[2] - dz))
            if achter and achter[1]["half"] == bl[1]["half"] and dwars(f, achter[1]["facing"]) and kan(c, bl, achter[1]["facing"]):
                vorm = "inner_left" if achter[1]["facing"] == CCW[f] else "inner_right"
        if bl[1].get("shape") != vorm:
            nieuw[c] = (bl[0], dict(bl[1], shape=vorm), bl[2])
    blocks.update(nieuw)


# =====================================================================================================================
# a round tower
# =====================================================================================================================
def toren(b, cx, cz, r, vloeren, dak_hoog, deuren, vensters=True, kragen=()):
    """A round white tower. vloeren = the y of every floor block (the first one is the ground floor), the wall goes on to
    one storey above the last floor; deuren = [(vloer index, 'north' | 'south' | 'east' | 'west')]. The stairs are straight
    flights of seven along the east and the west wall in turn, so the north and the south side stay free for doors.
    kragen = the floor indexes with a collar of roof around the tower (the tiers of a pagoda). Returns the y of the eave."""
    verdieping = vloeren[1] - vloeren[0]
    eave = vloeren[-1] + verdieping
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.hypot(x - cx, z - cz)
            if d > r + 0.5:
                continue
            wand = d > r - 0.5
            for y in range(vloeren[0], eave + 1):
                if wand:
                    band = y == vloeren[0] + 1 or (y in vloeren and y != vloeren[0]) or y == eave
                    b.set(x, y, z, RSTEEN if y == vloeren[0] else BAND if band else b.wit())
                elif y in vloeren:
                    b.set(x, y, z, PLANK)
                else:
                    b.s.blocks.pop((x, y, z), None)
    # the cornice under the roof
    for x in range(cx - r - 2, cx + r + 3):
        for z in range(cz - r - 2, cz + r + 3):
            if r + 0.5 < math.hypot(x - cx, z - cz) <= r + 1.5:
                b.set(x, eave, z, BAND)
    # the stairs: flight k (from floor k to k + 1) lies against the west wall (even k, rising to the south) or the east wall
    for k in range(len(vloeren) - 1):
        y0 = vloeren[k]
        oost = k % 2 == 1
        kolommen = (cx + r - 3, cx + r - 2) if oost else (cx - r + 2, cx - r + 3)
        for i in range(7):
            z = cz + 3 - i if oost else cz - 3 + i
            for x in kolommen:
                b.set(x, y0 + 1 + i, z, "birch_stairs", gb.stair("north" if oost else "south"))
                for yy in range(y0 + 1, y0 + 1 + i):
                    b.set(x, yy, z, PLANK)
                for yy in range(y0 + 2 + i, y0 + 5 + i):
                    if yy != y0 + 1 + i:
                        b.s.blocks.pop((x, yy, z), None)
        # a railing on the floor above, along the hole and at its low end
        binnen = cx + r - 4 if oost else cx - r + 4
        y1 = vloeren[k + 1] + 1
        for i in range(2, 6):
            b.set(binnen, y1, cz + 3 - i if oost else cz - 3 + i, HEK)
        for x in kolommen:
            b.set(x, y1, cz + 1 if oost else cz - 1, HEK)
    # doors (3 high, 1 wide, a frame of bands) and windows on the eight winds
    vrij = set()
    for k, kant in deuren:
        dx, dz = gb.DIRS[kant]
        x, z, y = cx + dx * r, cz + dz * r, vloeren[k] + 1
        b.leeg(x, y, z, x, y + 2, z)
        b.set(x, y + 3, z, "chiseled_quartz_block")
        for s in (-1, 1):
            for yy in range(y, y + 3):
                b.set(x + (s if dz else 0), yy, z + (s if dx else 0), ZUIL, {"axis": "y"})
        vrij.add((k, kant))
    if vensters:
        winden = {"east": 0, "south": 90, "west": 180, "north": 270}
        for k in range(len(vloeren)):
            for graden in range(0, 360, 45):
                kant = next((n for n, g in winden.items() if g == graden), None)
                if kant in ("east", "west") or (k, kant) in vrij:
                    continue                      # (the stairs lie against the east and west wall)
                x = cx + int(round(r * math.cos(math.radians(graden))))
                z = cz + int(round(r * math.sin(math.radians(graden))))
                for yy in range(vloeren[k] + 2, vloeren[k] + 5):
                    if b.naam(x, yy, z) not in (None, BAND, ZUIL):
                        b.set(x, yy, z, GLAS)
    # a lantern under every ceiling; under the roof (no ceiling there) four lanterns on posts
    for k in range(len(vloeren) - 1):
        boven = vloeren[k + 1]
        b.hanglamp(cx, boven - 1, cz)
        b.hanglamp(cx, boven - 1, cz + r - 3)
        b.hanglamp(cx, boven - 1, cz - r + 3)
        b.hanglamp(cx + r - 4, boven - 1, cz)
        b.hanglamp(cx - r + 4, boven - 1, cz)
    for dx, dz in ((-3, -r + 2), (3, -r + 2), (-3, r - 2), (3, r - 2)):
        b.set(cx + dx, vloeren[-1] + 1, cz + dz, HEK)
        b.set(cx + dx, vloeren[-1] + 2, cz + dz, "lantern", gb.lamp(False))
    for k in kragen:
        for x in range(cx - r - 3, cx + r + 4):
            for z in range(cz - r - 3, cz + r + 4):
                d = math.hypot(x - cx, z - cz)
                if r + 0.5 < d <= r + 1.6:
                    b.set(x, vloeren[k], z, DAK)
                elif r + 1.6 < d <= r + 2.7:
                    b.set(x, vloeren[k], z, DAKPLAAT, gb.slab())
    kegeldak(b, cx, cz, r, eave, dak_hoog)
    return eave


# =====================================================================================================================
# the statues of the Hal van de Guhvatars
# =====================================================================================================================
def beeld(b, x, y, z, kijkt, steen="smooth_stone", sjaal=None, ogen="blackstone", zonder=(), extra=(), hoog=0):
    """A sitting guh of stone on a pedestal of 7 x 7 (its middle at x, z; y = the pedestal block): body, head with the two
    ears, paws and a little tail; a scarf in the colour of its people. kijkt = the way it looks. zonder = cells left out
    (a bite), extra = [(u, hgt, v, block)] in the statue's own coordinates: u = to its left, v = forwards, hgt = up from
    the pedestal. hoog = extra body height (Kyoshi-guh was tall)."""
    fx, fz = gb.DIRS[kijkt]
    lx, lz = LINKS[kijkt]

    def zet(u, hgt, v, blok, props=None):
        if (u, hgt, v) not in zonder:
            b.set(x + lx * u + fx * v, y + hgt, z + lz * u + fz * v, blok, props)

    for u in range(-3, 4):
        for v in range(-3, 4):
            rand = abs(u) == 3 or abs(v) == 3
            b.set(x + lx * u + fx * v, y, z + lz * u + fz * v, BAND if rand else "smooth_quartz")
    top = 4 + hoog
    for u in range(-2, 3):
        for v in range(-2, 2):
            for hgt in range(1, top + 1):
                zet(u, hgt, v, sjaal if (sjaal and hgt == top and (abs(u) == 2 or v in (-2, 1))) else steen)
    for u in range(-2, 3):
        for v in range(-1, 3):
            for hgt in range(top + 1, top + 4):
                zet(u, hgt, v, steen)
    for s in (-1, 1):
        zet(s, top + 2, 2, ogen)
        zet(2 * s, top + 4, 0, steen)            # the ears: up and out
        zet(3 * s, top + 4, 0, steen)
        zet(3 * s, top + 3, 0, steen)
        zet(2 * s, 1, 2, steen)                  # the feet
        zet(2 * s, 3, 2, steen)                  # the paws
    zet(0, 2, -3, steen)                         # the tail
    for u, hgt, v, blok in extra:
        zet(u, hgt + (hoog if hgt > 4 else 0), v, blok)


def beelden(b):
    """The ten Guhvatars before you, oldest first, clockwise from the south-west corner of the hall; and their signs."""
    vy = HV + 1                                   # (the pedestal block: one above the floor)
    LB, OR, GR, RD, WT = "light_blue_concrete", "orange_terracotta", "green_terracotta", "red_terracotta", "white_wool"
    pijl = [(0, 7, v, LB) for v in range(-1, 3)] + [(0, 6, 2, LB), (-1, 7, 1, LB), (1, 7, 1, LB)]
    rij = [
        # id, x, z, kijkt, bord, arguments of beeld()
        ("beeld_01", 80, 110, "east", ["Guhvatar Wan-guh", "De allereerste.", "Vond het vadsen", "uit. Dank, Wan."],
         dict(steen="mossy_cobblestone", extra=[(3, 1, 3, "flower_pot")])),
        ("beeld_02", 80, 101, "east", ["Plons-guh", "Waterstam.", "Kon niet zwemmen", "Wel drijven."],
         dict(sjaal="blue_terracotta", extra=[(u, 8, v, "prismarine") for u in (-1, 0, 1) for v in (0, 1)] + [(0, 9, 0, "prismarine")])),
        ("beeld_03", 80, 92, "east", ["Kluit-guh", "Aarderijk.", "Zat liefst stil", "in de modder."],
         dict(sjaal=GR, extra=[(0, 8, 0, "coarse_dirt"), (0, 8, 1, "coarse_dirt"), (1, 8, 0, "moss_block"), (0, 9, 0, "moss_block")])),
        ("beeld_04", 81, 80, "south", ["Guhvatar ...guh", "Naam is", "opgeknabbeld.", "Wie deed dat?!"],
         dict(steen="cobblestone", sjaal=RD, zonder={(2, 8, 0), (3, 8, 0), (3, 7, 0), (2, 7, 2), (2, 7, 1), (2, 6, 2), (2, 5, 2), (1, 7, 2)})),
        ("beeld_05", 91, 80, "south", ["Szeto-guh", "Vuurnatie. Vulde", "formulieren in.", "Met vuur."],
         dict(sjaal=RD, extra=[(u, 8, v, RD) for u in (-1, 0, 1) for v in (0, 1)] + [(0, 9, 0, RD), (0, 9, 1, RD), (0, 10, 0, "gold_block")])),
        ("beeld_06", 101, 80, "south", ["Yangchen-guh", "Luchtnomade.", "Zo streng dat de", "wind ging liggen"],
         dict(sjaal=OR, extra=pijl + [(u, hgt, -2, "brown_terracotta") for u in (-1, 0, 1) for hgt in (5, 6)])),
        ("beeld_07", 111, 80, "south", ["Kuruk-guh", "Waterstam. Ging", "met de stroom", "mee. Liggend."],
         dict(sjaal="blue_terracotta", extra=[(u, 8, v, WT) for u in range(-2, 3) for v in (-1, 0, 1)] + [(-2, 9, -1, WT), (2, 9, -1, WT)]
              + [(u, hgt, -2, WT) for u in range(-2, 3) for hgt in (5, 6, 7)])),
        ("kyoshi", 112, 92, "west", ["Kyoshi-guh", "Aarderijk. 230", "jaar. Grootste", "voeten ooit."],
         dict(sjaal=GR, ogen="sea_lantern", hoog=1,
              extra=[(u, 8, 0, "gold_block") for u in range(-2, 3)] + [(u, 9, 0, "gold_block") for u in (-1, 0, 1)] + [(0, 10, 0, "gold_block")]
              + [(0, 5, 2, "calcite"), (0, 6, 2, "calcite"), (-2, 6, 2, RD), (2, 6, 2, RD), (-3, 3, 2, "gold_block"), (3, 3, 2, "gold_block"),
                 (-3, 1, 2, "smooth_stone"), (3, 1, 2, "smooth_stone")])),
        ("roku", 112, 101, "west", ["Roku-guh", "Vuurnatie. Had", "een draak.", "En een baard."],
         dict(sjaal=RD, ogen="sea_lantern",
              extra=[(0, 8, 0, RD), (0, 9, 0, RD), (0, 10, 0, "gold_block"), (-1, 10, 0, "gold_block"), (1, 10, 0, "gold_block"), (0, 11, 0, "gold_block"),
                     (0, 4, 2, WT), (0, 3, 2, WT), (-1, 4, 2, WT), (1, 4, 2, WT)])),
        ("beeld_08", 112, 110, "west", ["Aang-guh", "Luchtnomade.", "Ging even vadsen", "in een kaasbol."],
         dict(sjaal=OR, ogen="smooth_stone", extra=pijl + [(3, hgt, 2, HEK) for hgt in range(1, 7)] + [(3, 7, 2, "orange_wool")])),
    ]
    for id, x, z, kijkt, tekst, args in rij:
        beeld(b, x, vy, z, kijkt, **args)
        fx, fz = gb.DIRS[kijkt]
        b.bord(x + fx * 4, vy, z + fz * 4, kijkt, tekst)
        if id.startswith("beeld_"):
            b.prop(id, x + fx * 3, vy, z + fz * 3, wie=tekst[0] + " (de voorkant van het voetstuk; het beeld staat erachter)")
        else:
            kind = {"kyoshi": "KYOSHI_GUH", "roku": "ROKU_GUH"}[id]
            b.plek(f"{id}_beeld", x + fx * 6, vy, z + fz * 6, yaw={"west": 90.0, "east": -90.0, "south": 0.0, "north": 180.0}[kijkt],
                   wie=tekst[0] + " geeft raad (voor het beeld)", kind=kind)
    # the last pedestal, in the middle of the hall under the round window: empty
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) + abs(dz) <= 3 and (dx, dz) != (0, 0):
                b.set(96 + dx, HV, 96 + dz, TEGEL)
    b.set(96, HV, 96, "sea_lantern")
    b.set(96, vy, 96, "chiseled_quartz_block")
    b.prop("voetstuk_leeg", 96, vy, 96, wie="het laatste, lege voetstuk (hier komt het beeld van de speler)")
    b.bord(96, vy, 98, "south", ["Hier komt de", "volgende", "Guhvatar.", "Wie o wie, njeg?"], wall=False)


# =====================================================================================================================
# the south: landing terrace, camp, stairs, gate, forecourt
# =====================================================================================================================
def aankomst(b):
    ay = A + 1
    plein(b, 87, 164, 105, 180, A, PLEIN, rand=RSTEEN)
    for x in range(88, 105):
        for z in range(165, 180):
            if (x + z) % 4 == 0:
                b.set(x, A, z, TEGEL)
    for dx in range(-3, 4):                          # the landing mark: an arrow of arrow tiles that points at the stairs
        b.set(96 + dx, A, 172, PIJL, {"facing": "north"})
    for dz in range(-3, 4):
        b.set(96, A, 172 + dz, PIJL, {"facing": "north"})
    for x, z in ((87, 164), (105, 164), (87, 180), (105, 180), (87, 172), (105, 172)):
        b.lantaarn(x, z, y=ay)
    b.zone("aankomst", 88, ay, 165, 104, ay + 8, 179, wie="de landingsplek (17 x 15, vrij erboven)")
    b.prop("aankomst_speler", 96, ay, 172, wie="hier sta je na aankomst")
    b.vrij.append((86, ay, 163, 106, ay + 8, 181))
    b.prop("uitzicht", 96, 74, 226, wie="het openingsshot: de tempel van het zuiden af gezien")
    b.bord(103, ay, 163, "south", ["Zuidelijke", "Luchttempel", "Voeten vegen,", "of zweven. Njeg."], wall=False)
    rustpunt(b, 1, 110, ay, 168)


def kamp(b):
    """The team's camp west of the landing: a ring of logs around a fire, three tents under the cliff, room for Appa."""
    ay = A + 1
    b.zone("kamp", 60, ay, 159, 85, ay + 6, 180, wie="het kamp van het team")
    pad(b, [(86, 172), (76, 172), (76, 168)], breed=3, lampen=False)
    fx, fz = 79, 166
    b.set(fx, A, fz, "cobblestone")
    b.set(fx, ay, fz, "campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for dx, dz, as_ in ((-2, -1, "z"), (-2, 0, "z"), (-2, 1, "z"), (2, -1, "z"), (2, 0, "z"), (2, 1, "z"), (-1, 2, "x"), (0, 2, "x"), (1, 2, "x")):
        b.set(fx + dx, ay, fz + dz, "stripped_spruce_log", {"axis": as_})
    # tents: a ridge of wool over two posts, against the cliff
    for tx, kleur in ((63, "orange_wool"), (70, "yellow_wool"), (82, "light_blue_wool")):
        for dz in range(4):
            z = 159 + dz
            b.set(tx - 2, ay, z, kleur)
            b.set(tx + 2, ay, z, kleur)
            b.set(tx - 1, ay + 1, z, kleur)
            b.set(tx + 1, ay + 1, z, kleur)
            b.set(tx, ay + 2, z, kleur)
        b.set(tx, ay, 159, "white_wool")
        b.set(tx, ay + 1, 159, "white_wool")
        b.set(tx, ay, 160, "orange_carpet")
        b.set(tx, ay, 161, "orange_carpet")
    for x, z in ((61, 168), (85, 160), (66, 178), (84, 178)):
        b.lantaarn(x, z, y=ay)
    b.prop("appa_kamp", 70, ay, 172, wie="hier ligt Appa in het kamp (5 x 5 vrij)")
    b.vrij.append((67, ay, 169, 73, ay + 5, 175))
    for id, dx, dz, yaw, wie in (("team_aang", -3, 0, -90.0, "Aang-guh"), ("team_momo", -1, -2, 0.0, "Momo-guh"), ("team_katara", 3, 0, 90.0, "Katara-guh"),
                                 ("team_sokka", 1, 3, 180.0, "Sokka-guh"), ("team_toph", -1, 3, 180.0, "Toph-guh"), ("team_zuka", 1, -2, 0.0, "Zuka")):
        b.plek(id, fx + dx, ay, fz + dz, yaw=yaw, wie=wie + " bij het kampvuur")
    b.bord(74, ay, 164, "south", ["Kampeerplek", "Vuurtje stoken", "mag. De tempel", "afbranden niet."], wall=False)


def trappen_en_poort(b):
    """The stairs through the cliff (y 40 to 48), the tempelpoort on top and the forecourt behind it."""
    for i in range(7):                                # steps at z 160 (y 41) .. 154 (y 47)
        z, y = 160 - i, A + 1 + i
        for x in range(90, 103):
            b.leeg(x, y + 1, z, x, G + 6, z)
            if x in (90, 102):
                for yy in range(y, y + 2):
                    b.set(x, yy, z, RSTEEN)
                b.set(x, y + 2, z, RMUUR)
            else:
                b.set(x, y, z, RTRAP, gb.stair("north"))
                for yy in range(y - 2, y):
                    b.set(x, yy, z, RSTEEN)
    for x in range(90, 103):
        b.leeg(x, A + 1, 161, x, A + 8, 163)
    for x in (90, 102):
        b.lantaarn(x, 161, y=A + 1, hoog=3)
    # the gate: two pillars, a double beam and a roof with turned-up ends
    gy = G + 1
    plein(b, 84, 149, 108, 153, G, PLEIN, rand=RSTEEN)
    for px in (88, 104):
        for x in range(px - 1, px + 2):
            for z in range(150, 153):
                b.set(x, gy, z, RSTEEN)
                for y in range(gy + 1, gy + 9):
                    b.set(x, y, z, BAND if y == gy + 8 else b.wit())
        b.set(px, gy + 3, 153, "orange_terracotta")
        b.set(px, gy + 4, 153, "orange_terracotta")
        b.set(px, gy + 5, 153, TEGEL)
    for x in range(85, 108):
        for z in range(150, 153):
            b.set(x, gy + 9, z, "quartz_bricks")
            b.set(x, gy + 11, z, "quartz_bricks")
        b.set(x, gy + 10, 151, ZUIL, {"axis": "x"})
    for x in range(83, 110):
        for z in range(149, 154):
            rand = z in (149, 153) or x in (83, 109)
            b.set(x, gy + 12, z, DAKPLAAT if rand else DAK, gb.slab("top") if rand else None)
    for x in range(86, 107):
        b.set(x, gy + 13, 151, NOKPLAAT, gb.slab())
    for x in (83, 109):                                # the turned-up ends
        for z in range(149, 154):
            b.set(x, gy + 13, z, DAKPLAAT, gb.slab())
    b.hanglamp(92, gy + 8, 151, ketting=2)
    b.hanglamp(100, gy + 8, 151, ketting=2)
    b.set(96, gy + 8, 153, TEGEL)
    b.set(96, gy + 9, 153, TEGEL)
    b.prop("poort", 96, gy, 151, wie="onder de tempelpoort")
    b.bord(89, gy + 1, 153, "south", ["Welkom, njeg!", "Hier woont de", "wind. En wij.", "- de monniken"])
    # the forecourt
    plein(b, 82, 127, 110, 148, G, PLEIN, rand=RSTEEN)
    for x in range(83, 110):
        for z in range(128, 148):
            d = math.hypot(x - 96, z - 138)
            if 5.5 < d <= 6.5 or d <= 1.2:
                b.set(x, G, z, TEGEL)
            elif 2.5 < d <= 3.5:
                b.set(x, G, z, "light_blue_terracotta")
    for x, z in ((82, 127), (110, 127), (82, 148), (110, 148), (82, 137), (110, 137)):
        b.set(x, gy, z, RSTEEN)
        b.lantaarn(x, z, y=gy + 1, hoog=1)
    b.plek("aang", 96, gy, 145, yaw=0.0, wie="Aang-guh wacht je op achter de poort", kind="AANG_GUH")
    b.prop("zweef_landing", 96, gy, 138, wie="het midden van het voorplein: hier land je na de zweefringen")
    rustpunt(b, 2, 116, gy, 140)


# =====================================================================================================================
# the Hal van de Guhvatars and the great tower
# =====================================================================================================================
def hal(b):
    vy = HV + 1                                       # where you walk in the hall
    dak = HV + 15                                     # the ceiling = the roof terrace (block y 67)
    # the podium, its balustrade and its four stairs
    for x in range(70, 123):
        for z in range(70, 123):
            for y in range(G + 1, HV + 1):
                rand = x in (70, 122) or z in (70, 122)
                if rand or y == HV:
                    b.set(x, y, z, RSTEEN if y < HV else (BAND if rand else PLEIN))
            b.leeg(x, HV + 1, z, x, HV + 4, z)
    for x in range(70, 123):
        for z in range(70, 123):
            if (x in (70, 122) or z in (70, 122)) and not (90 <= x <= 102 or 90 <= z <= 102):
                b.set(x, vy, z, "birch_fence" if (x + z) % 4 else BAND)
                if (x + z) % 4 == 0:
                    b.set(x, vy + 1, z, "lantern", gb.lamp(False))
    for i in range(3):                                # three steps (y 49..51) on each side, 13 wide
        for a in range(90, 103):
            for (x, z, kant) in ((a, 123 + i, "north"), (a, 69 - i, "south"), (123 + i, a, "west"), (69 - i, a, "east")):
                y = HV - 1 - i
                b.leeg(x, y + 1, z, x, y + 5, z)
                b.set(x, y, z, RTRAP, gb.stair(kant))
                for yy in range(G + 1, y):
                    b.set(x, yy, z, RSTEEN)
    # the walls: white, a plinth, pilasters, tall windows, a cornice
    for x in range(74, 119):
        for z in range(74, 119):
            wand = x in (74, 75, 117, 118) or z in (74, 75, 117, 118)
            for y in range(vy, dak + 1):
                if y == dak:
                    b.set(x, y, z, PLEIN if not wand else BAND)
                elif wand:
                    buiten = x in (74, 118) or z in (74, 118)
                    pilaster = buiten and ((x - 74) % 8 == 0 and z in (74, 118) or (z - 74) % 8 == 0 and x in (74, 118))
                    b.set(x, y, z, RSTEEN if y == vy and buiten else BAND if (pilaster or y in (dak - 1, vy + 8)) and buiten else b.wit())
                else:
                    b.s.blocks.pop((x, y, z), None)
            if not wand:
                b.set(x, HV, z, "polished_diorite" if (x + z) % 2 else "smooth_quartz")
    for a in range(78, 115, 8):                       # windows between the pilasters (not behind the statues' heads only: all)
        for da in range(0, 5, 2):
            for y in range(vy + 9, vy + 13):
                for (x, z) in ((a + da, 74), (a + da, 75), (74, a + da), (75, a + da), (117, a + da), (118, a + da)):
                    b.set(x, y, z, GLAS)
                for (x, z) in ((a + da, 117), (a + da, 118)):
                    if not 90 <= x <= 102:
                        b.set(x, y, z, GLAS)
    # the entrance (south, 7 wide and 8 high) with a porch roof, and a small door to the north court
    for x in range(93, 100):
        b.leeg(x, vy, 117, x, vy + 7, 118)
    for x in (92, 100):
        for y in range(vy, vy + 9):
            b.set(x, y, 118, ZUIL, {"axis": "y"})
            b.set(x, y, 119, ZUIL, {"axis": "y"})
    for x in range(91, 102):
        b.set(x, vy + 9, 119, "chiseled_quartz_block" if x in (92, 96, 100) else BAND)
        b.set(x, vy + 10, 119, DAKTRAP, gb.stair("north"))
        b.set(x, vy + 10, 120, DAKPLAAT, gb.slab())
    for x in range(95, 98):
        b.leeg(x, vy, 74, x, vy + 3, 75)
    b.bord(91, vy + 1, 119, "south", ["Hal van de", "Guhvatars", "Niet op de", "beelden klimmen"])
    b.zone("hal", 76, vy, 76, 116, dak - 1, 116, wie="de Hal van de Guhvatars")
    # four columns, the round window in the ceiling with its ring of light, chandeliers
    for cx in (86, 106):
        for cz in (86, 106):
            for dx in (0, 1):
                for dz in (0, 1):
                    for y in range(vy, dak):
                        b.set(cx - dx, y, cz - dz, "chiseled_quartz_block" if y in (vy, dak - 1) else ZUIL, None if y in (vy, dak - 1) else {"axis": "y"})
    for x in range(90, 103):
        for z in range(90, 103):
            d = math.hypot(x - 96, z - 96)
            if d <= 3.5:
                b.set(x, dak, z, GLAS)
            elif d <= 4.6:
                b.set(x, dak, z, "sea_lantern")
    for x in (86, 91, 96, 101, 106):
        for z in (86, 91, 96, 101, 106):
            if abs(x - 96) + abs(z - 96) > 6 and (x, z) not in ((86, 86), (106, 86), (86, 106), (106, 106)):
                b.hanglamp(x, dak - 1, z, ketting=6)
    for a in (77, 85, 96, 107, 115):                  # standing lights along the walls, between the statues
        for (x, z) in ((a, 77), (77, a), (115, a)):
            if (x, z) not in ((77, 96), (115, 96), (96, 77)):
                b.set(x, vy, z, BAND)
                b.set(x, vy + 1, z, "lantern", gb.lamp(False))
    for x in (89, 103):
        b.set(x, vy, 115, BAND)
        b.set(x, vy + 1, 115, "lantern", gb.lamp(False))
    beelden(b)
    # the roof terrace: a parapet, an eave of copper below it, a little pavilion on each corner
    ty = dak + 1
    for x in range(73, 120):
        for z in range(73, 120):
            ring = max(abs(x - 96), abs(z - 96))
            if ring == 23:
                langs_z = abs(z - 96) == 23 and abs(x - 96) < 23      # (the eave on the north and the south side)
                b.set(x, dak - 1, z, DAKTRAP, gb.stair(("south" if z < 96 else "north") if langs_z else ("east" if x < 96 else "west")))
            elif ring == 22:
                b.set(x, ty, z, HEK if (x + z) % 6 else BAND)
                if (x + z) % 6 == 0:
                    b.set(x, ty + 1, z, "lantern", gb.lamp(False))
            elif ring in (16, 20) and (x + z) % 2 == 0:
                b.set(x, dak, z, TEGEL)
    for cx in (77, 115):
        for cz in (77, 115):
            for dx in (-2, 2):
                for dz in (-2, 2):
                    for y in range(ty, ty + 4):
                        b.set(cx + dx, y, cz + dz, ZUIL, {"axis": "y"})
            schilddak(b, cx - 3, cz - 3, cx + 3, cz + 3, ty + 4)
            b.set(cx, ty, cz, "sea_lantern")
            b.leeg(cx, ty + 1, cz, cx, ty + 3, cz)
    return dak


def grote_toren(b, dak):
    """The great tower on the roof of the hall (r 10): the bell room, Aang-guh's old room, the tea room and the lookout
    with the jump balcony of the zweefringen."""
    cx, cz, r = 96, 96, 10
    vloeren = [dak, dak + 7, dak + 14, dak + 21, dak + 28]
    # the round window of the hall is the middle of the tower's floor: keep it
    glas = {c: bl for c, bl in b.s.blocks.items() if c[1] == dak and math.hypot(c[0] - cx, c[2] - cz) <= 4.6}
    eave = toren(b, cx, cz, r, vloeren, 10, [(0, "north"), (0, "south"), (4, "south"), (4, "north")], kragen=(2, 4))
    b.s.blocks.update(glas)
    y0 = dak + 1
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            if 4.6 < math.hypot(x - cx, z - cz) <= 5.6:
                b.set(x, y0, z, HEK)
    b.bord(cx + 2, y0, cz - 8, "south", ["Niet op het", "glas dansen.", "Eronder staan", "beelden. Njeg."], wall=False)
    # the bell room
    y1 = vloeren[1] + 1
    b.set(cx, y1 + 5, cz, "bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    for dx, dz in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        b.set(cx + dx, y1, cz + dz, "orange_carpet")
    b.bord(cx, y1, cz - 6, "south", ["Tempelbel", "Luiden bij:", "brand, bezoek", "of taart."], wall=False)
    # Aang-guh's old room
    y2 = vloeren[2] + 1
    b.set(cx - 2, y2, cz - 5, "orange_bed", {"facing": "west", "part": "foot", "occupied": "false"})
    b.set(cx - 3, y2, cz - 5, "orange_bed", {"facing": "west", "part": "head", "occupied": "false"})
    b.set(cx + 2, y2, cz - 6, "bookshelf")
    b.set(cx + 3, y2, cz - 6, "bookshelf")
    b.set(cx + 2, y2 + 1, cz - 6, "flower_pot")
    b.set(cx, y2, cz + 5, "yellow_carpet")
    b.set(cx + 1, y2, cz + 5, "orange_carpet")
    b.set(cx - 1, y2, cz + 5, "orange_carpet")
    b.set(cx + 4, y2, cz + 4, KNABBELS)
    b.bord(cx, y2, cz - 6, "south", ["Kamer van Aang", "Niet opgeruimd", "sinds honderd", "jaar. Njeg."], wall=False)
    # the tea room
    yt = vloeren[3] + 1
    for dx in (-1, 0, 1):
        b.set(cx + dx, yt, cz, "spruce_slab", gb.slab("top"))
    b.set(cx, yt + 1, cz, "flower_pot")
    b.set(cx - 1, yt + 1, cz, TAART)
    for dx, dz, kleur in ((-2, 0, "orange_carpet"), (2, 0, "orange_carpet"), (0, -2, "yellow_carpet"), (0, 2, "yellow_carpet")):
        b.set(cx + dx, yt, cz + dz, kleur)
    b.bord(cx, yt, cz - 7, "south", ["Theekamer", "Thee drinken is", "ook mediteren.", "Met een koekje."], wall=False)
    # the lookout: the south door opens onto the jump balcony, the north door onto a small one
    y3 = vloeren[4] + 1
    for s, n in ((1, 3), (-1, 2)):
        for i in range(1, n + 1):
            for dx in range(-2, 3):
                z = cz + s * (r + i)
                b.set(cx + dx, vloeren[4], z, BAND if abs(dx) == 2 or i == n else PLANK)
                b.leeg(cx + dx, y3, z, cx + dx, y3 + 3, z)
                if abs(dx) == 2 or (i == n and s == -1):
                    b.set(cx + dx, y3, z, HEK)
        for dx in (-2, 2):                              # brackets under the balcony
            b.set(cx + dx, vloeren[4] - 1, cz + s * (r + 1), "quartz_stairs", gb.stair("north" if s == 1 else "south", "top"))
    b.set(cx - 2, y3 + 1, cz + r + 3, "lantern", gb.lamp(False))
    b.set(cx + 2, y3 + 1, cz + r + 3, "lantern", gb.lamp(False))
    b.prop("zweef_start", cx, y3, cz + r + 3, wie="de sprongplank van de zweefringen (zuidbalkon van de grote toren)")
    b.bord(cx + 1, y3 + 1, cz + r + 1, "south", ["Zweefringen", "Spring, zweef,", "mis er geen.", "Kijk niet omlaag"])
    b.set(cx - 4, y3, cz, "orange_carpet")
    b.set(cx + 4, y3, cz, "orange_carpet")
    return vloeren, eave


# =====================================================================================================================
# the north: two towers, their bridges, Appa's stable
# =====================================================================================================================
def brug(b, x, dak):
    """A bridge from the south door of a north tower (floor y 69) to the roof of the hall (y 67): 5 wide, two steps down."""
    for z in range(59, 74):
        y = dak + 2 if z <= 69 else dak + 1 if z == 70 else dak
        if z in (70, 71):
            y = dak + 2 if z == 70 else dak + 1
        for dx in range(-2, 3):
            cx = x + dx
            b.leeg(cx, y + 1, z, cx, y + 4, z)
            if z in (70, 71) and abs(dx) < 2:
                b.set(cx, y, z, "quartz_stairs", gb.stair("north"))
            elif z >= 72:
                b.set(cx, dak, z, BAND if abs(dx) == 2 else PLEIN)
            else:
                b.set(cx, y, z, BAND if abs(dx) == 2 else "smooth_quartz")
            if abs(dx) == 2 and z < 73:
                b.set(cx, y + 1, z, HEK)
        if z in (60, 64, 68):
            b.set(x - 2, dak + 4, z, "lantern", gb.lamp(False))
            b.set(x + 2, dak + 4, z, "lantern", gb.lamp(False))
    # two piers with an arch between them
    for z in (62, 67):
        for dx in (-2, 2):
            for y in range(G + 1, dak + 2):
                b.set(x + dx, y, z, RSTEEN if y == G + 1 else b.wit())
        for dx in (-1, 1):
            b.set(x + dx, dak + 1, z, "quartz_stairs", gb.stair("west" if dx == -1 else "east", "top"))
    b.leeg(x, dak + 1, 73, x, dak + 3, 73)
    for dx in (-1, 1):
        b.leeg(x + dx, dak + 1, 73, x + dx, dak + 3, 73)


def monnikentoren(b):
    """The north-west tower: the monks' quarters (kitchen, two dormitories, the meditation room, the gong)."""
    cx, cz, r = 76, 50, 8
    vloeren = [G, G + 7, G + 14, G + 21, G + 28]
    eave = toren(b, cx, cz, r, vloeren, 15, [(0, "south"), (0, "north"), (3, "south")], kragen=(2, 4))
    b.zone("monnikenverblijf", cx - r, G + 1, cz - r, cx + r, eave, cz + r, wie="het monnikenverblijf (de noordwesttoren)")
    y = G + 1
    # kitchen and pantry: Momo's hiding spot is behind the barrels
    for dx in (-2, -1, 0):
        b.set(cx + dx, y, cz - 6, "smoker" if dx == -2 else "barrel", {"facing": "south", "lit": "false"} if dx == -2 else {"facing": "up", "open": "false"})
    b.set(cx + 1, y, cz - 6, "barrel", {"facing": "up", "open": "false"})
    b.set(cx + 1, y + 1, cz - 6, "barrel", {"facing": "south", "open": "false"})
    b.chest(cx + 3, y, cz - 5, "south", "guhs:chests/einde2_bouw_lucht_voorraad")
    for dx in (-1, 0, 1):
        b.set(cx + dx, y, cz + 1, "spruce_slab", gb.slab("top"))
    b.set(cx, y + 1, cz + 1, TAART)
    b.set(cx - 1, y, cz + 2, "birch_stairs", gb.stair("north"))
    b.set(cx + 1, y, cz + 2, "birch_stairs", gb.stair("north"))
    b.set(cx - 1, y, cz, "birch_stairs", gb.stair("south"))
    b.set(cx + 1, y, cz, "birch_stairs", gb.stair("south"))
    b.bord(cx + 2, y, cz + 5, "north", ["Keuken. Vandaag:", "taart. Morgen:", "taart. Daarna:", "njeg, taart."], wall=False)
    b.plek("momo", cx + 2, y, cz - 6, yaw=0.0, wie="Momo-guh's schuilplek achter de tonnen in de keuken", wezen="guhs:momo_guh")
    # two dormitories
    for k in (1, 2):
        y = vloeren[k] + 1
        for i, dx in enumerate((-3, -1, 1, 3)):
            if k == 2 and i == 3:
                kleur = "light_blue_bed"
            else:
                kleur = "orange_bed" if (i + k) % 2 else "yellow_bed"
            for s in ((-1,) if k == 1 else (-1, 1)):
                if kleur == "light_blue_bed" and s == 1:
                    kleur2 = "orange_bed"
                else:
                    kleur2 = kleur
                b.set(cx + dx, y, cz + s * 5, kleur2, {"facing": "north" if s == -1 else "south", "part": "foot", "occupied": "false"})
                b.set(cx + dx, y, cz + s * 6, kleur2, {"facing": "north" if s == -1 else "south", "part": "head", "occupied": "false"})
        b.set(cx, y, cz, "orange_carpet")
        if k == 1:
            b.bord(cx, y, cz + 5, "north", ["Slaapzaal", "Stil zijn.", "Hier wordt", "gevadst."], wall=False)
            b.set(cx - 2, y, cz + 6, "bookshelf")
            b.set(cx + 2, y, cz + 6, "bookshelf")
    b.prop("bed_speler", cx + 3, vloeren[2] + 1, cz - 6, wie="het logeerbed van de speler (het lichtblauwe, het hoofdeinde)")
    b.bord(cx + 2, vloeren[2] + 1, cz - 3, "south", ["Logeerbed", "voor de nieuwe", "Guhvatar.", "Snurken mag."], wall=False)
    # the meditation room on the bridge floor
    y = vloeren[3] + 1
    for graden in range(0, 360, 60):
        x, z = cx + round(3 * math.cos(math.radians(graden))), cz + round(3 * math.sin(math.radians(graden)))
        b.set(x, y, z, "orange_carpet" if graden % 120 else "yellow_carpet")
    b.set(cx, vloeren[3], cz, TEGEL)
    b.set(cx, y, cz, "candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    b.bord(cx, y, cz - 6, "south", ["Mediteren =", "vadsen met je", "ogen dicht en", "je rug recht."], wall=False)
    # the gong under the roof
    y = vloeren[4] + 1
    for dx in (-1, 1):
        for yy in range(y, y + 4):
            b.set(cx + dx * 2, yy, cz, "stripped_spruce_log", {"axis": "y"})
    for dx in range(-2, 3):
        b.set(cx + dx, y + 4, cz, "stripped_spruce_log", {"axis": "x"})
    b.set(cx, y + 3, cz, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(cx, y + 2, cz, "gold_block")
    b.set(cx, y + 1, cz, "gold_block")
    b.bord(cx, y, cz + 4, "north", ["Windgong", "Gaat vanzelf.", "Ook 's nachts.", "Sorry, njeg."], wall=False)
    return vloeren


def windtoren(b):
    """The north-east tower: the store of fruit, the library, the map room, the room of the wind flutes and, under the
    roof, Momo-guh's nest."""
    cx, cz, r = 116, 50, 8
    vloeren = [G, G + 7, G + 14, G + 21, G + 28]
    toren(b, cx, cz, r, vloeren, 13, [(0, "south"), (0, "north"), (3, "south")], kragen=(2, 4))
    y = G + 1
    for dx, dz, blok in ((-3, -5, "hay_block"), (-2, -6, "hay_block"), (-2, -5, "melon"), (2, -6, "pumpkin"), (3, -5, "hay_block"), (0, -6, "composter"),
                         (-3, 5, "melon"), (3, 5, "hay_block"), (2, 6, "hay_block")):
        b.set(cx + dx, y, cz + dz, blok, {"level": "0"} if blok == "composter" else {"axis": "y"} if blok == "hay_block" else None)
    b.bord(cx - 1, y, cz - 5, "south", ["Fruitkelder", "Voor taart.", "En voor Appa.", "Vooral Appa."], wall=False)
    # the library
    y = vloeren[1] + 1
    for graden in range(200, 341, 12):
        x, z = cx + round(6.6 * math.cos(math.radians(graden))), cz + round(6.6 * math.sin(math.radians(graden)))
        for yy in range(y, y + 3):
            if b.naam(x, yy, z) is None:
                b.set(x, yy, z, "bookshelf")
    for graden in range(20, 161, 12):
        x, z = cx + round(6.6 * math.cos(math.radians(graden))), cz + round(6.6 * math.sin(math.radians(graden)))
        for yy in range(y, y + 3):
            if b.naam(x, yy, z) is None:
                b.set(x, yy, z, "bookshelf")
    b.set(cx, y, cz, "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    b.bord(cx + 1, y, cz + 3, "north", ["Bibliotheek", "Boekrollen", "terugrollen na", "gebruik, njeg."], wall=False)
    # the map room
    y = vloeren[2] + 1
    b.set(cx, y, cz, "cartography_table")
    b.set(cx - 1, y, cz, "birch_stairs", gb.stair("east"))
    for dx in (-2, -1, 0, 1, 2):
        b.set(cx + dx, y + 2, cz - 7, BLAUWDRUK if dx == 0 else TEGEL, {"facing": "south"} if dx == 0 else None)
    b.bord(cx + 2, y, cz - 5, "south", ["Kaartenkamer", "Alle windstreken", "Wij wonen in", "het zuiden. Dus."], wall=False)
    # wind flutes on the bridge floor: rods of light in a row
    y = vloeren[3] + 1
    for i, dx in enumerate((-3, -2, -1, 1, 2, 3)):
        for yy in range(y, y + 1 + (i % 3) + 1):
            b.set(cx + dx, yy, cz - 6, STAAFJE, {"facing": "up"})
    b.bord(cx, y, cz - 5, "south", ["Windfluiten", "Fluiten vanzelf", "als het waait.", "Het waait altijd"], wall=False)
    # Momo's nest
    y = vloeren[4] + 1
    for dx, dz in ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)):
        b.set(cx + dx, y, cz + dz - 3, "hay_block", {"axis": "y"})
    b.set(cx + 2, y, cz - 3, KNABBELS)
    b.set(cx + 2, y + 1, cz - 3, KNABBELS)
    b.set(cx - 2, y, cz - 4, KNABBELS)
    b.set(cx, y, cz - 5, KNABBELS)
    b.set(cx, y + 1, cz - 5, "red_terracotta")
    b.prop("momo_eind", cx, y, cz - 3, wie="Momo-guh's nest onder het dak van de windtoren: het eind van de achtervolging")
    b.bord(cx, y, cz + 1, "south", ["Momo's buit.", "Niet van jou.", "(Wel van jou,", "maar toch.)"], wall=False)
    return vloeren


def stal(b):
    """Appa's stable between the two towers: open to the north (he flies out over the scooter track), hay, the broken
    saddle, the apples."""
    x0, x1, z0, z1 = 87, 105, 26, 44
    y = G + 1
    plein(b, x0, z0 - 4, x1, z1, G, "coarse_dirt")
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wand = x in (x0, x1) or z in (z0, z1)
            for yy in range(y, y + 9):
                if not wand:
                    b.s.blocks.pop((x, yy, z), None)
                elif z == z0 and x0 + 2 < x < x1 - 2 and yy < y + 7:
                    b.s.blocks.pop((x, yy, z), None)                      # the wide mouth of the stable
                elif z == z1 and 95 <= x <= 97 and yy < y + 3:
                    b.s.blocks.pop((x, yy, z), None)                      # the door to the plateau
                else:
                    hoek = (x in (x0, x1) and z in (z0, z1)) or (z == z0 and x in (x0 + 2, x1 - 2))
                    b.set(x, yy, z, PAAL if hoek else RSTEEN if yy == y else BAND if yy == y + 8 else b.wit(), {"axis": "y"} if hoek else None)
            if not wand and (x * 7 + z * 3) % 5 == 0:
                b.set(x, G, z, "hay_block", {"axis": "x"})
    for x in (x0, x1):                                # windows
        for z in range(z0 + 4, z1 - 3, 4):
            for yy in range(y + 3, y + 6):
                b.set(x, yy, z, GLAS)
    for x in range(x0 + 1, x1):                       # rafters
        if (x - x0) % 4 == 0:
            for z in range(z0 + 1, z1):
                b.set(x, y + 8, z, PAAL, {"axis": "z"})
            b.hanglamp(x, y + 7, 31)
            b.hanglamp(x, y + 7, 39)
    schilddak(b, x0 - 1, z0 - 1, x1 + 1, z1 + 1, y + 9, hoog=7)
    # hay along the walls, the trough
    for x, z, n in ((88, 43, 3), (89, 43, 2), (90, 43, 1), (88, 42, 2), (88, 41, 1), (104, 27, 2), (104, 28, 1), (103, 27, 1), (88, 27, 2), (88, 28, 1)):
        for i in range(n):
            b.set(x, y + i, z, "hay_block", {"axis": "y"})
    for x in range(101, 105):
        b.set(x, y, 36, "water_cauldron", {"level": "3"})
    # the broken saddle on its trestle: half of it has slid off
    for dx, dz, dy, blok, props in ((0, 0, 0, "spruce_fence", None), (2, 0, 0, "spruce_fence", None), (0, 0, 1, "brown_wool", None), (1, 0, 1, "brown_wool", None),
                                    (2, 0, 1, "spruce_stairs", gb.stair("west")), (0, 0, 2, "orange_carpet", None), (1, 0, 2, "orange_carpet", None),
                                    (1, 1, 0, "brown_wool", None), (2, 1, 0, "spruce_slab", gb.slab()), (1, 1, 1, "orange_carpet", None),
                                    (3, 1, 0, "spruce_trapdoor", {"facing": "east", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"})):
        b.set(100 + dx, y + dy, 40 + dz, blok, props)
    b.prop("zadel", 101, y, 41, wie="het kapotte zadel op zijn schraag")
    b.bord(103, y + 1, 43, "north", ["Zadel: stuk.", "Iemand ging er", "met vier guhs", "op zitten."])
    b.set(90, y, 39, "barrel", {"facing": "up", "open": "true"})
    b.set(91, y, 39, "barrel", {"facing": "up", "open": "false"})
    b.set(90, y + 1, 39, "red_terracotta")
    b.prop("appels", 90, y, 39, wie="de ton met appels voor Appa")
    b.bord(92, y, 39, "south", ["Appels voor", "Appa. Momo:", "AFBLIJVEN.", "Ja jij, njeg."], wall=False)
    b.prop("appa", 96, y, 34, wie="hier staat Appa in zijn stal (7 x 5 x 7 vrij)")
    b.zone("appa_stal", x0 + 1, y, z0 + 1, x1 - 1, y + 8, z1 - 1, wie="de stal van Appa")
    b.bord(98, y + 1, 45, "south", ["Stal van Appa", "Zes poten, een", "staart, veel te", "veel haar."])
    # the apron in front of the mouth
    for x in range(x0 + 2, x1 - 1):
        for z in range(z0 - 4, z0):
            b.set(x, G, z, PLEIN if (x + z) % 3 else TEGEL)
    for x in (x0 + 1, x1 - 1):
        b.lantaarn(x, z0 - 2, y=y)


# =====================================================================================================================
# the west: the taartenterras and the luchtbalveld
# =====================================================================================================================
def luchtbalveld(b):
    """A sunken court (floor y 43) full of poles in a bed of kruimelwolk: who falls sinks softly and walks out by the
    stairs. A gallery all around, a goal on a frame at each end, a start platform in front of each goal."""
    x0, x1, z0, z1 = 30, 50, 78, 106
    bodem = G - 5
    y = G + 1
    for x in range(x0 - 3, x1 + 4):
        for z in range(z0 - 3, z1 + 4):
            in_veld = x0 <= x <= x1 and z0 <= z <= z1
            if in_veld:
                b.leeg(x, bodem + 1, z, x, y + 12, z)
                b.set(x, bodem, z, RSTEEN if (x + z) % 5 else TEGEL)
                b.set(x, bodem + 1, z, WOLK)
                b.set(x, bodem + 2, z, WOLK)
            else:
                rand = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1)
                for yy in range(bodem, G + 1):
                    if rand or yy == G:
                        b.set(x, yy, z, RSTEEN if rand else PLEIN)
                b.leeg(x, y, z, x, y + 3, z)
                if rand:
                    b.set(x, y, z, HEK)
    # the stairs into the court: in the north-west and the south-east corner (5 steps along the wall)
    for hoek_x, hoek_z, richting in ((x0, z0, "south"), (x1, z1, "north")):
        dz = 1 if richting == "south" else -1
        b.leeg(hoek_x + (-1 if hoek_x == x0 else 1), y, hoek_z, hoek_x + (-1 if hoek_x == x0 else 1), y + 2, hoek_z)   # a gap in the railing
        for i in range(5):
            z = hoek_z + dz * i
            b.leeg(hoek_x, bodem + 1, z, hoek_x, y + 2, z)
            b.set(hoek_x, G - i, z, RTRAP, gb.stair("north" if richting == "south" else "south"))
            for yy in range(bodem + 1, G - i):
                b.set(hoek_x, yy, z, RSTEEN)
    # the poles: a grid of two apart, thinned out; every top can be reached from another with a jump over one block
    kolommen, rijen = range(10), range(14)
    staat = set()
    for i in kolommen:
        for j in rijen:
            if (i * 3 + j * 5) % 7 in (0, 3) and 0 < j < 13:
                continue
            if (i, j) in ((0, 0), (9, 13)):
                continue                               # (the stairs)
            staat.add((i, j))
    nr = 0
    for j in rijen:
        for i in kolommen:
            if (i, j) not in staat:
                continue
            x, z = 31 + 2 * i, 79 + 2 * j
            top = G + 2 + ((i * 7 + j * 3) % 4 == 0)
            for yy in range(bodem + 1, top + 1):
                b.set(x, yy, z, PAAL, {"axis": "y"})
            b.set(x, top, z, "stripped_spruce_log" if (i + j) % 2 else PAAL, {"axis": "y"})
            nr += 1
            b.prop(f"luchtbal_paal_{nr:02d}", x, top, z, wie="de top van een paal", los=True)
            b.palen.append((x, top, z))
    # the start platforms (3 x 2, at the height of the pole tops) with steps from the gallery, and the goals behind them
    for id, z_rand, s in (("speler", z1 + 1, 1), ("aang", z0 - 1, -1)):
        zp = z_rand - s                                # the row of the court next to the wall
        for x in range(39, 42):
            for yy in range(bodem + 1, G + 3):
                b.set(x, yy, zp, RSTEEN if yy < G + 2 else BAND)
            b.leeg(x, y, z_rand, x, y + 3, z_rand)     # a gap in the railing
            b.set(x, y, z_rand, "quartz_stairs", gb.stair("north" if s == 1 else "south"))
            b.leeg(x, G + 3, zp, x, G + 6, zp)
        b.prop(f"luchtbal_start_{id}", 40, G + 3, zp, wie="het startplatform van " + ("de speler" if id == "speler" else "Aang-guh"))
        zg = z_rand + 2 * s                            # the goal: a frame of 5 x 5 with a round hole, on the gallery's outer edge
        for dx in range(-3, 4):
            for dy in range(0, 7):
                rand = abs(dx) == 3 or dy in (0, 6)
                if rand:
                    b.set(40 + dx, G + 3 + dy, zg, "stripped_spruce_log", {"axis": "y" if abs(dx) == 3 else "x"})
                elif abs(dx) == 2 and dy in (1, 5):
                    b.set(40 + dx, G + 3 + dy, zg, "orange_wool")
        for dx in (-3, 3):
            for yy in range(y, G + 3):
                b.set(40 + dx, yy, zg, "stripped_spruce_log", {"axis": "y"})
        b.prop(f"luchtbal_doel_{id}", 40, G + 6, zg, wie="het gat van het doel van " + ("de speler" if id == "speler" else "Aang-guh"), los=True)
    b.zone("luchtbal_veld", x0, bodem, z0, x1, G + 12, z1, wie="het luchtbalveld")
    for x, z in ((x0 - 3, z0 - 3), (x1 + 3, z0 - 3), (x0 - 3, z1 + 3), (x1 + 3, z1 + 3), (x0 - 3, 92), (x1 + 3, 86), (x1 + 3, 98)):
        b.lantaarn(x, z, y=y)
    for x in (32, 40, 48):                             # lights down in the court, under the cloud
        for z in (80, 86, 92, 98, 104):
            b.set(x, bodem, z, "sea_lantern")
    b.bord(x1 + 3, y, 104, "east", ["LUCHTBALVELD", "Vallen mag.", "De wolk vangt", "je vads op."], wall=False)
    b.bord(x1 + 3, y, 80, "east", ["Stand:", "Monniken 7", "Zwaartekracht 0", "(tot nu toe)"], wall=False)


def taartenterras(b):
    """Guhatso's terrace between the hall and the court: a long bench full of taarten, aimed at the luchtbalveld."""
    y = G + 1
    plein(b, 54, 84, 66, 100, G, PLEIN, rand=RSTEEN)
    for x in range(55, 66):
        for z in range(85, 100):
            if (x + z) % 3 == 0:
                b.set(x, G, z, TEGEL)
    for z in range(89, 96):                            # the bench: a low wall of quartz with taarten on it
        b.set(57, y, z, "smooth_quartz_slab", gb.slab("top"))
        if z % 2:
            b.set(57, y + 1, z, TAART)
    b.prop("taartenbank", 57, y, 92, wie="de taartenbank van Guhatso")
    b.plek("guhatso", 60, y, 92, yaw=90.0, wie="de herinnering van monnik Guhatso (gooit taarten)", kind="GUHATSO")
    b.bord(57, y, 97, "east", ["Taarten van", "Guhatso. Niet", "opeten! Gooien", "mag wel, njeg."], wall=False)
    for x, z in ((54, 84), (66, 84), (54, 100), (66, 100)):
        b.lantaarn(x, z, y=y)
    rustpunt(b, 3, 62, y, 104, tekst=("Rustpunt", "Eerst zitten.", "Dan pas de", "palen op."))


# =====================================================================================================================
# the luchtscooter-baan along the rim
# =====================================================================================================================
def baanpunt(graden, uit=0.0):
    t = math.radians(graden)
    return MX + (BAAN[0] + uit) * math.cos(t), MZ + (BAAN[1] + uit) * math.sin(t)


def scooterbaan(b):
    """A smooth track of 5 wide along the rim, from the south-west (140 degrees) round the west and the north to the
    north-east (330 degrees): a kerb of slabs on the inside, a low wall on the outside, arrow tiles down the middle,
    an arch at the start and one at the finish."""
    y = G + 1
    dek, rand_in, rand_uit = {}, set(), set()
    stappen = int((BAAN_TOT - BAAN_VAN) * 8)
    for s in range(stappen + 1):
        graden = BAAN_VAN + (BAAN_TOT - BAAN_VAN) * s / stappen
        for uit10 in range(-34, 35, 4):
            uit = uit10 / 10.0
            px, pz = baanpunt(graden, uit)
            c = (int(round(px)), int(round(pz)))
            if abs(uit) <= 2.4:
                dek.setdefault(c, graden)
            elif uit < 0:
                rand_in.add(c)
            else:
                rand_uit.add(c)
    rand_in -= set(dek)
    rand_uit -= set(dek)
    for (x, z), graden in dek.items():
        assert b.top[x][z] == G, f"de baan ligt naast het plateau op {(x, z)}"
        b.leeg(x, y, z, x, y + 4, z)
        b.set(x, G, z, "smooth_stone")
    for (x, z) in rand_in:
        if b.top[x][z] == G and b.naam(x, y, z) is None:
            b.set(x, G, z, RSTEEN)
            b.set(x, y, z, RPLAAT, gb.slab())
    for (x, z) in rand_uit:
        if b.top[x][z] == G:
            b.leeg(x, y, z, x, y + 3, z)
            b.set(x, G, z, RSTEEN)
            b.set(x, y, z, RMUUR)
    # the middle line: an arrow tile every fourth block that points the way, orange in between
    lijn = []
    for s in range(stappen + 1):
        graden = BAAN_VAN + (BAAN_TOT - BAAN_VAN) * s / stappen
        px, pz = baanpunt(graden)
        c = (int(round(px)), int(round(pz)))
        if not lijn or lijn[-1][0] != c:
            lijn.append((c, graden))
    for i, ((x, z), graden) in enumerate(lijn):
        t = math.radians(graden + 90)                  # the way you ride: with the degrees
        rx, rz = math.cos(t), math.sin(t)
        if i % 4 == 0:
            kant = ("east" if rx > 0 else "west") if abs(rx) >= abs(rz) else ("south" if rz > 0 else "north")
            b.set(x, G, z, PIJL, {"facing": kant})
        elif i % 4 == 2:
            b.set(x, G, z, "orange_terracotta")
    # lamps on the outer wall
    for graden in range(BAAN_VAN + 6, BAAN_TOT, 9):
        px, pz = baanpunt(graden, 3.2)
        x, z = int(round(px)), int(round(pz))
        if (x, z) in rand_uit and b.naam(x, y, z) == RMUUR:
            b.set(x, y + 1, z, RMUUR)
            b.set(x, y + 2, z, "lantern", gb.lamp(False))
    # start and finish: an arch over the track, a line across it
    for id, graden, tekst in (("start", BAAN_VAN + 2, ["LUCHTSCOOTER", "Start. Niet", "duwen. Wel", "vahoeg gaan!"]),
                              ("finish", BAAN_TOT - 2, ["FINISH", "Baanrecord:", "Aang-guh,", "100 jaar terug"])):
        binnen, buiten = baanpunt(graden, -3.2), baanpunt(graden, 3.2)
        poten = [(int(round(binnen[0])), int(round(binnen[1]))), (int(round(buiten[0])), int(round(buiten[1])))]
        for (x, z) in poten:
            for yy in range(y, y + 5):
                b.set(x, yy, z, PAAL, {"axis": "y"})
        (xa, za), (xb, zb) = poten
        n = max(abs(xb - xa), abs(zb - za))
        vorige = None
        for i in range(n + 1):
            x, z = round(xa + (xb - xa) * i / n), round(za + (zb - za) * i / n)
            if vorige and vorige[0] != x and vorige[1] != z:
                b.set(x, y + 5, vorige[1], "orange_wool")      # (no diagonal steps: the beam is one piece)
            vorige = (x, z)
            b.set(x, y + 5, z, "orange_wool" if i % 2 else "yellow_wool")
            if 0 < i < n and b.naam(x, G, z) == "smooth_stone":
                b.set(x, G, z, "white_concrete" if i % 2 else "black_concrete")
        mx, mz = baanpunt(graden)
        b.prop(f"scooter_{id}", int(round(mx)), y, int(round(mz)), wie=f"de {id} van de luchtscooter-baan")
        sx, sz = baanpunt(graden + (-3 if id == "start" else 3), -4.6)
        sx, sz = int(round(sx)), int(round(sz))
        if b.naam(sx, y, sz) is None and b.top[sx][sz] == G:
            b.bord(sx, y, sz, "south" if id == "start" else "west", tekst, wall=False)
    b.pad("scooter_baan", [(int(round(baanpunt(g)[0])), y, int(round(baanpunt(g)[1]))) for g in range(BAAN_VAN + 2, BAAN_TOT - 1, 6)] +
          [(int(round(baanpunt(BAAN_TOT - 2)[0])), y, int(round(baanpunt(BAAN_TOT - 2)[1])))], wie="de middenlijn van de luchtscooter-baan, van start naar finish")
    # the wink: Sjokkel creeps along the inner kerb of the first stretch; the camera stands on the plateau and looks at him
    a, c = baanpunt(BAAN_VAN + 8, -3.2), baanpunt(BAAN_VAN + 15, -3.2)
    a, c = (int(round(a[0])), y + 1, int(round(a[1]))), (int(round(c[0])), y + 1, int(round(c[1])))
    b.pad("knipoog_sjokkel", [a, c], wie="Sjokkel kruipt over de binnenrand van het eerste stuk van de baan (van het eerste naar het tweede punt)")
    midden = baanpunt(BAAN_VAN + 11.5, -3.2)
    oog = baanpunt(BAAN_VAN + 11.5, -13.0)
    b.camera("knipoog_sjokkel", (int(round(oog[0])), y + 2, int(round(oog[1]))), (int(round(midden[0])), y + 1, int(round(midden[1]))),
             zicht=["scooter_start"], wie="de camera van de knipoog met Sjokkel op de luchtscooter-baan")


# =====================================================================================================================
# the east: the Werktuigguh's workshop and the building site of the Vredesballon
# =====================================================================================================================
WERK = (132, 166, 80, 106)                             # the outside of the workshop: x0, x1, z0, z1
TUIN = (137, 167, 107, 142)                            # the outside of the yard's wall (its north side is the workshop)
BALLON = (140, G + 1, 112)                             # vredesballon_anker: the origin of the five overlays
OEFEN = [(135, 82), (141, 82), (147, 82), (153, 82), (160, 89), (160, 95)]   # the north-west corner of each practice bay


def werkplaats(b):
    x0, x1, z0, z1 = WERK
    y, plafond = G + 1, G + 9
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wand = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, RSTEEN if wand else "smooth_stone")
            for yy in range(y, plafond + 1):
                if yy == plafond:
                    b.set(x, yy, z, "spruce_planks" if not wand else BAND)
                elif wand:
                    hoek = (x - x0) % 6 == 0 and z in (z0, z1) or (z - z0) % 6 == 2 and x in (x0, x1)
                    b.set(x, yy, z, RSTEEN if yy == y else "stripped_spruce_log" if hoek else b.wit(), {"axis": "y"} if hoek and yy != y else None)
                else:
                    b.s.blocks.pop((x, yy, z), None)
    for a in range(x0 + 3, x1 - 2, 6):                 # high windows
        for da in (0, 1):
            for yy in range(y + 5, y + 7):
                for z in (z0, z1):
                    if not (z == z1 and 148 <= a + da <= 156):
                        b.set(a + da, yy, z, GLAS)
    for z in range(z0 + 5, z1 - 3, 6):
        for dz in (0, 1):
            for yy in range(y + 5, y + 7):
                b.set(x1, yy, z + dz, GLAS)
    dak = schilddak(b, x0 - 1, z0 - 1, x1 + 1, z1 + 1, plafond + 1, hoog=6)
    for x in range(158, 161):                          # the chimney
        for z in range(86, 89):
            for yy in range(plafond + 1, dak + 5):
                b.set(x, yy, z, "bricks")
    b.set(159, dak + 5, 87, "campfire", {"facing": "north", "lit": "true", "signal_fire": "true", "waterlogged": "false"})
    # the closed door (3 x 3 of werkplaatsdeur blocks) in the west wall, a porch and the notice
    for z in range(92, 95):
        for yy in range(y, y + 3):
            b.set(x0, yy, z, DEUR)
        b.set(x0, y + 3, z, "chiseled_quartz_block")
    for z in (91, 95):
        for yy in range(y, y + 4):
            b.set(x0, yy, z, ZUIL, {"axis": "y"})
            b.set(x0 - 1, yy, z, "stripped_spruce_log", {"axis": "y"})
    for z in range(90, 97):
        b.set(x0 - 1, y + 4, z, DAKTRAP, gb.stair("east"))
        b.set(x0 - 2, y + 4, z, DAKPLAAT, gb.slab())
    plein(b, x0 - 4, 90, x0 - 1, 96, G, PLEIN, rand=None)
    for z in (91, 95):
        b.set(x0 - 1, y, z, "stripped_spruce_log", {"axis": "y"})
    b.prop("werkplaatsdeur", x0, y, 93, wie="het onderste middelste blok van de dichte deur van de Werktuigguh")
    b.bord(x0 - 1, y + 1, 91, "west", ["WERKPLAATS", "Nog niet af,", "njeg!", "- de Werktuigguh"])
    b.bord(x0 - 1, y + 1, 95, "west", ["Niet kloppen.", "Ik hoor het", "toch niet: te", "veel gehamer."])
    b.set(x0 - 1, y + 3, 93, "lantern", gb.lamp(True))
    b.zone("werkplaats", x0 + 1, y, z0 + 1, x1 - 1, plafond - 1, z1 - 1, wie="de werkplaats van de Werktuigguh (achter de dichte deur)")
    b.binnen.append((x0 + 1, y, z0 + 1, x1 - 1, plafond - 1, z1 - 1))
    # the six practice bays: a floor of plain smooth stone with a painted border, posts on the corners, a number
    for nr, (bx, bz) in enumerate(OEFEN, 1):
        for x in range(bx - 1, bx + 6):
            for z in range(bz - 1, bz + 6):
                rand = x in (bx - 1, bx + 5) or z in (bz - 1, bz + 5)
                if x0 < x < x1 and z0 < z < z1:
                    b.set(x, G, z, ("yellow_concrete" if (x + z) % 2 else "black_concrete") if rand else "smooth_stone")
        b.prop(f"oefen_{nr}", bx + 2, y, bz + 2, wie=f"oefenopstelling {nr}: het midden van een vrij vak van 5 x 4 x 5", los=True)
    for nr, (bx, bz) in enumerate(OEFEN, 1):           # a lamp above every bay and a number on the wall
        b.hanglamp(bx + 2, plafond - 1, bz + 2)
        if nr <= 4:
            b.bord(bx + 2, y + 5, z0 + 1, "south", [f"Proef {nr}", "", "", ""])
        else:
            b.bord(x1 - 1, y + 5, bz + 2, "west", [f"Proef {nr}", "", "", ""])
    # the workbench, the Werktuigguh's spot, tools and plans
    for x in range(138, 145):
        if x in (138, 144):
            b.set(x, y, 98, "spruce_planks")
        else:
            b.set(x, y, 98, "spruce_slab", gb.slab("top"))
    b.set(139, y + 1, 98, TANDWIEL)
    b.set(143, y + 1, 98, "flower_pot")
    b.set(141, y + 1, 98, "candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    b.prop("werkbank", 141, y, 98, wie="de werkbank van de Werktuigguh (achter de dichte deur)", los=True)
    b.plek("werktuigguh", 141, y, 96, yaw=0.0, wie="de Werktuigguh aan zijn werkbank (achter de dichte deur)", kind="WERKTUIGGUH", los=True)
    for z, blok in ((84, BLAUWDRUK), (86, TANDWIEL), (88, BLAUWDRUK), (98, BLAUWDRUK), (100, TANDWIEL), (102, BLAUWDRUK)):
        b.set(x0 + 1, y + 2, z, blok, {"facing": "east"} if blok == BLAUWDRUK else None)
    for x in (140, 142, 144, 146):
        b.set(x, y + 2, z1 - 1, BLAUWDRUK if x % 4 == 0 else TANDWIEL, {"facing": "north"} if x % 4 == 0 else None)
    for x, z, n in ((134, 104, 2), (135, 104, 1), (134, 103, 1), (163, 103, 2), (164, 103, 3), (164, 102, 1), (157, 103, 1)):
        for i in range(n):
            b.set(x, y + i, z, "barrel" if (x + i) % 2 else TANDWIEL, {"facing": "up", "open": "false"} if (x + i) % 2 else None)
    for x, z in ((147, 92), (148, 92), (147, 93)):      # an anvil corner and a furnace under the chimney
        b.set(x, y, z, "smithing_table" if (x, z) == (147, 92) else "anvil" if (x, z) == (148, 92) else "grindstone",
              {"facing": "north"} if (x, z) == (148, 92) else {"face": "floor", "facing": "north"} if (x, z) == (147, 93) else None)
    b.set(159, y, 88, "blast_furnace", {"facing": "south", "lit": "false"})
    b.set(158, y, 88, "bricks")
    b.set(160, y, 88, "bricks")
    # a glider of the Werktuigguh under the ceiling: a frame of fences with wings of wool
    for x in range(140, 153):
        b.set(x, plafond - 2, 93, "orange_wool" if abs(x - 146) > 1 else "birch_planks")
        if abs(x - 146) > 1 and abs(x - 146) < 6:
            b.set(x, plafond - 2, 92, "yellow_wool")
            b.set(x, plafond - 2, 94, "yellow_wool")
    b.set(146, plafond - 1, 93, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(141, plafond - 1, 93, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(151, plafond - 1, 93, "chain", {"axis": "y", "waterlogged": "false"})
    for x, z in ((137, 92), (137, 100), (146, 102), (155, 92), (155, 100), (150, 86), (138, 88), (146, 86)):
        b.hanglamp(x, plafond - 1, z)
    b.bord(136, y, 90, "west", ["Regel 1: niets", "aanraken. Regel", "2: echt niets.", "Ook jij, Momo."], wall=False)
    # a model of the balloon on a table, and a test machine with a snoet (every machine has one)
    b.set(136, y, 100, "spruce_planks")
    b.set(136, y + 1, 100, "spruce_fence")
    b.set(136, y + 2, 100, "orange_wool")
    b.set(136, y + 3, 100, "yellow_wool")
    for i, x in enumerate((157, 158, 159)):
        b.set(x, y, 101, "pink_terracotta" if i == 1 else "waxed_copper_block")
        b.set(x, y + 1, 101, "waxed_copper_block" if i == 1 else "black_concrete")
        if i != 1:
            b.set(x, y + 2, 101, "waxed_cut_copper_slab", gb.slab())
    b.bord(158, y, 99, "north", ["Proefmachine", "Doet nog niets.", "Kijkt wel al", "heel lief."], wall=False)
    # the archway to the yard
    for x in range(150, 155):
        b.leeg(x, y, z1, x, y + 4, z1)
    for x in (149, 155):
        for yy in range(y, y + 6):
            b.set(x, yy, z1, "stripped_spruce_log", {"axis": "y"})
    for x in range(149, 156):
        b.set(x, y + 5, z1, "stripped_spruce_log", {"axis": "x"})


def bouwplaats(b):
    """The walled yard south of the workshop, open to the sky: the building site of the Vredesballon. The balloon itself
    comes in five stages as overlay templates on vredesballon_anker; what stands here always is the yard's own gear."""
    x0, x1, z0, z1 = TUIN
    y = G + 1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wand = x in (x0, x1) or z == z1
            b.set(x, G, z, RSTEEN if wand else "coarse_dirt" if (x * 5 + z * 3) % 7 else "gravel")
            b.leeg(x, y, z, x, y + 6, z)
            if wand:
                paal = (x - x0) % 6 == 0 and z == z1 or (z - z0) % 7 == 0 and x in (x0, x1)
                for yy in range(y, y + 5):
                    b.set(x, yy, z, RSTEEN if yy == y else BAND if yy == y + 4 else b.wit())
                if paal:
                    b.set(x, y + 5, z, RMUUR)
                    b.set(x, y + 6, z, "lantern", gb.lamp(False))
    ax, ay, az = BALLON
    for x in range(ax, ax + 25):                       # the launch floor under the balloon: planks with a ring of tiles
        for z in range(az, az + 25):
            d = math.hypot(x - (ax + 12), z - (az + 12))
            if d <= 11.5:
                b.set(x, G, z, TEGEL if 9.5 < d <= 10.5 else "spruce_planks")
    b.set(ax + 12, G, az + 12, "sea_lantern")
    b.zone("vredesballon_bouwplaats", x0 + 1, y, z0, x1 - 1, y + 44, z1 - 1, wie="de bouwplaats van de Vredesballon (ommuurd, open naar de lucht)")
    b.binnen.append((x0 + 1, y, z0, x1 - 1, y + 44, z1 - 1))
    b.prop("vredesballon_anker", ax, ay, az, wie="de oorsprong van de lagen laag_vredesballon_1 .. _5 (25 x 44 x 25)", los=True)
    # the progress board on the workshop's south wall: a frame with a free face
    for x in range(141, 148):
        for yy in range(y + 1, y + 5):
            rand = x in (141, 147) or yy in (y + 1, y + 4)
            b.set(x, yy, WERK[3], "stripped_spruce_log" if rand else "smooth_stone", {"axis": "x" if yy in (y + 1, y + 4) else "y"} if rand else None)
    b.prop("vredesballon_teller", 144, y + 1, z0, wie="hier hangt het voortgangsbord van de Vredesballon (tegen de zuidmuur van de werkplaats)", los=True)
    b.bord(158, y + 1, z0, "south", ["BOUWPLAATS", "Vredesballon", "Helm op, oren", "naar binnen."])
    # the yard's own gear along the walls: crates, rolls of cloth, a ladder cart
    for x, z, n, blok in ((139, 139, 2, "barrel"), (140, 140, 1, "barrel"), (139, 140, 3, "barrel"), (165, 139, 2, "white_wool"), (165, 140, 1, "orange_wool"),
                          (164, 140, 1, "yellow_wool"), (165, 110, 2, "hay_block"), (165, 111, 1, "hay_block"), (139, 109, 1, TANDWIEL), (139, 110, 2, "spruce_planks")):
        for i in range(n):
            b.set(x, y + i, z, blok, {"facing": "up", "open": "false"} if blok == "barrel" else {"axis": "y"} if blok == "hay_block" else None)
    b.bord(141, y, 140, "north", ["Doek, mand,", "brander, voer en", "versiering. In", "die volgorde."], wall=False)


def lagen(b):
    """The five stages of the Vredesballon as overlay templates on vredesballon_anker (einde2_bouw_lucht_ballon.py). The
    room they take must be empty in the build itself."""
    from features import einde2_bouw_lucht_ballon as ballon
    stadia = ballon.maak(b.h)
    problems = ballon.controle(stadia)
    ax, ay, az = BALLON
    in_de_weg = [c for c in b.s.blocks if ax <= c[0] < ax + ballon.SX and ay <= c[1] < ay + ballon.SY and az <= c[2] < az + ballon.SZ]
    if in_de_weg:
        problems.append(f"{len(in_de_weg)} blokken van de bouw staan in de ruimte van de Vredesballon, bv. {sorted(in_de_weg)[:3]}")
    if problems:
        raise SystemExit(f"{NAAM}: the Vredesballon: {len(problems)} problem(s):" + "".join(chr(10) + "  " + q for q in problems[:20]))
    for n, s in enumerate(stadia, 1):
        b.laag(f"vredesballon_{n}", s, ax, ay, az)
    return stadia


# =====================================================================================================================
# the zweefringen
# =====================================================================================================================
RINGEN = [  # the middle of each ring and the axis it faces along, in flying order
    (96, 92, 121, "z"), (84, 89, 130, "x"), (68, 86, 125, "x"), (58, 84, 108, "z"), (58, 82, 88, "z"), (58, 80, 68, "z"), (57, 78, 46, "z"),
    (76, 76, 32, "x"), (96, 74, 27, "x"), (116, 72, 32, "x"), (132, 69, 48, "z"), (131, 66, 72, "z"), (128, 62, 112, "z"), (112, 57, 134, "x"),
]


def zweefringen(b):
    """Fourteen glowing rings (radius 4) in a wide loop around the towers, from the jump balcony down to the forecourt."""
    for nr, (x, y, z, as_) in enumerate(RINGEN, 1):
        for da in range(-5, 6):
            for dy in range(-5, 6):
                d = math.hypot(da, dy)
                if 4.4 < d <= 5.4:
                    c = (x, y + dy, z + da) if as_ == "x" else (x + da, y + dy, z)
                    assert c not in b.s.blocks, f"zweefring {nr} raakt {b.s.blocks[c][0]} op {c}"
                    b.set(*c, RINGBLOK if (da + dy) % 3 else "orange_concrete", {"axis": "y"} if (da + dy) % 3 else None)
                    b.ringen.add(c)
        b.ring(f"zweefring_{nr:02d}", x, y, z, as_=as_, straal=4)


# =====================================================================================================================
# garden, paths, creatures
# =====================================================================================================================
def tuin(b):
    """Paths between everything, apple trees, tufts of korstgras, the creatures that live here."""
    y = G + 1
    pad(b, [(96, 66), (96, 46)])                                   # hall north door -> stable
    pad(b, [(96, 60), (76, 60)])                                   # -> the monnikentoren (its south door)
    pad(b, [(96, 60), (116, 60)])                                  # -> the windtoren
    pad(b, [(76, 41), (76, 34), (62, 30)])                         # behind the monnikentoren to the track
    pad(b, [(116, 41), (116, 34), (130, 30)])
    pad(b, [(68, 96), (67, 96)])                                   # podium west stairs -> the taartenterras
    pad(b, [(126, 96), (130, 93)])                                 # podium east stairs -> the workshop's porch
    pad(b, [(81, 138), (60, 138), (48, 136), (40, 136)])           # forecourt -> the start of the track
    pad(b, [(60, 138), (60, 101)])                                 # -> the taartenterras
    pad(b, [(111, 138), (124, 138), (124, 100)])                   # forecourt -> the east
    pad(b, [(124, 80), (124, 62), (140, 58), (156, 56)])           # -> the finish of the track
    pad(b, [(124, 100), (124, 80)])
    pad(b, [(54, 92), (53, 92)], lampen=False)
    for x, z in ((70, 134), (66, 146), (122, 146), (130, 132), (64, 64), (60, 70), (130, 66), (132, 70), (84, 64), (108, 64),
                 (46, 122), (40, 64), (52, 44), (140, 44), (150, 70), (74, 120), (118, 122)):
        if b.top[x][z] == G and b.naam(x, y, z) is None and b.naam(x, G, z) in NATUUR:
            boom(b, x, z, hoog=4 + (x + z) % 2)
    rustpunt(b, 4, 108, y, 54, tekst=("Rustpunt", "Zitten. Appa", "aaien mag ook.", "Hij bijt niet."))
    # the creatures of the air: vleermuis-lemurguhs around the trees and the towers
    for nr, (x, z) in enumerate(((72, 132), (120, 142), (62, 66), (128, 68), (92, 62), (100, 130)), 1):
        x, z = next((ax, az) for d in range(6) for ax in range(x - d, x + d + 1) for az in range(z - d, z + d + 1)
                    if b.top[ax][az] == G and b.naam(ax, G, az) in NATUUR and b.naam(ax, y, az) is None and b.naam(ax, y + 1, az) is None)
        b.wezen(f"lemur_{nr}", x, y, z, "guhs:vleermuis_lemurguh", yaw=(nr * 67) % 360 - 180.0, honk=10)
    # korstgras on the open ground
    for x in range(2, W - 2):
        for z in range(2, D - 2):
            t = b.top[x][z]
            if t >= 0 and b.naam(x, t, z) == KORST and b.naam(x, t + 1, z) is None and b.rng.random() < 0.06:
                b.set(x, t + 1, z, "guhs:korstgras")


# =====================================================================================================================
# einde2 phase B (slice h1_lucht, CONTRACT_E2_B 4.1): the pier where Zuka's ship rams the temple, and the escape to Appa
# =====================================================================================================================
STEIGER_X0, STEIGER_X1 = 129, 166           # the deck from the east rim of the landing terrace to the end of the pier
STEIGER_Z0, STEIGER_Z1 = 169, 173           # 5 wide: a beam of logs on each side, planks between
KOP = (154, 165, 166, 177)                  # the head of the pier: x0, z0, x1, z1 (Zuka's ship lies against its east side)
SCHIP = (167, A - 12, 165)                  # zuka_schip_anker: the origin of the overlay laag_zuka_schip
WACHTEN = [(125, 174), (116, 167), (106, 175), (95, 168)]   # ontsnap_wacht_1..4: Zuka's men on the terrace (y A + 1)


def steiger(b):
    """The pier on the east side of the landing terrace (DESIGN 5 "Zuka's boog": his ship rams it in chapter 1). A plank
    deck on log beams with a railing, posts hanging under it, lamps, a rest point at its root (rustpunt_5: a player caught in
    the escape is put back here) and a wider head at the end where Zuka jumps off his ship. The ship itself is an overlay
    (zuka_schip_laag): only the player whose story it is sees it."""
    y = A
    plank, log, hek = "spruce_planks", "stripped_spruce_log", "spruce_fence"
    x0, z0, x1, z1 = KOP
    for x in range(STEIGER_X0, x1 + 1):
        kop = x >= x0
        for z in range(z0 if kop else STEIGER_Z0, (z1 if kop else STEIGER_Z1) + 1):
            rand = (z in (STEIGER_Z0, STEIGER_Z1) and not kop) or (kop and (z in (z0, z1) or x in (x0, x1)))
            if rand:
                b.set(x, y, z, log, {"axis": "x" if z in (STEIGER_Z0, STEIGER_Z1, z0, z1) else "z"})
            else:
                b.set(x, y, z, plank)
            b.leeg(x, y + 1, z, x, y + 5, z)
    # the railing: along both sides of the deck and round the head, open where the deck meets the head and on the east side
    # of the head (Zuka's ship lies there: no railing left there after the bump)
    for x in range(STEIGER_X0 + 1, x0):
        for z in (STEIGER_Z0, STEIGER_Z1):
            b.set(x, y + 1, z, hek)
    for x in range(x0, x1):
        for z in (z0, z1):
            b.set(x, y + 1, z, hek)
    for z in range(z0, z1 + 1):
        if not STEIGER_Z0 <= z <= STEIGER_Z1:
            b.set(x0, y + 1, z, hek)
    # posts under the deck (piles that hang down: the pier was built from the island outwards)
    for x in range(STEIGER_X0 + 3, x1 + 1, 7):
        for z in ((STEIGER_Z0, STEIGER_Z1) if x < x0 else (z0, z1)):
            for yy in range(y - 6, y):
                b.set(x, yy, z, log, {"axis": "y"})
    # lamps: posts on the railing of the deck, one on each corner of the head
    for x in range(STEIGER_X0 + 6, x0 - 1, 9):
        for z in (STEIGER_Z0, STEIGER_Z1):
            b.lantaarn(x, z, y=y + 1)
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.lantaarn(x, z, y=y + 1)
    # bollards on the head (where a ship would tie up... if it came politely)
    for x, z in ((x1 - 1, z0 + 1), (x1 - 1, z1 - 1)):
        b.set(x, y + 1, z, log, {"axis": "y"})
        b.set(x, y + 2, z, "spruce_trapdoor", {"facing": "north", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"})
    # the rest point at the root of the pier: a square of temple tiles, a lamp and a sign
    rx, rz = STEIGER_X0 + 2, (STEIGER_Z0 + STEIGER_Z1) // 2
    for x in range(rx - 1, rx + 2):
        for z in range(STEIGER_Z0 + 1, STEIGER_Z1):
            b.set(x, y, z, TEGEL if (x, z) != (rx, rz) else "chiseled_quartz_block")
    b.prop("rustpunt_5", rx, y + 1, rz, wie="rustpunt aan het begin van de steiger: wie bij de ontsnapping gepakt wordt, staat weer hier")
    b.bord(STEIGER_X0, y + 1, STEIGER_Z0, "north", ["Steiger", "Alleen voor", "vriendelijke", "schepen, njeg."], wall=False)
    b.bord(x0 + 1, y + 1, z0 + 1, "south", ["Niet rammen.", "Echt niet.", "Ook niet als je", "een prins bent."], wall=False)
    # the markers of the story (h1_lucht: the scene zuka_steiger, the escape)
    b.zone("steiger_zone", STEIGER_X0, y + 1, z0, x1, y + 5, z1, wie="de steiger met zijn kop")
    b.prop("steiger", x0 + 1, y + 1, (z0 + z1) // 2, wie="hier sta je als Zuka's schip de steiger ramt")
    b.plek("zuka_confrontatie", x1 - 3, y + 1, (z0 + z1) // 2, yaw=90.0, wie="Zuka springt hier van zijn schip op de steiger (kijkt naar het westen)")
    for nr, (x, z) in enumerate(WACHTEN, 1):
        b.leeg(x, y + 1, z, x, y + 3, z)
        b.plek(f"ontsnap_wacht_{nr}", x, y + 1, z, yaw=-90.0, wie="een Vuurnatie-Mika van Zuka op het landingsterras (de ontsnapping)")
    b.prop("zuka_schip_anker", *SCHIP, wie="de oorsprong van de laag laag_zuka_schip (Zuka's schip tegen de kop van de steiger)", los=True)


def zuka_schip_laag(b):
    """Zuka's ship as the overlay laag_zuka_schip at zuka_schip_anker: a small Vuurnatie warship (25 long) whose ram-bow
    lies against the east side of the pier's head, splinters of the railing on the deck. Blocks only, and only blocks that
    draw themselves from their block state (CONTRACT_E2 6.6): one player sees it (Schijn). Its cells only fill air."""
    import math as m
    sx, sy, sz = 25, 22, 13
    s = b.h.Structure((sx, sy, sz))
    mid = sz // 2
    romp, rand, dek, ijzer = "minecraft:polished_blackstone_bricks", "minecraft:red_nether_bricks", "minecraft:dark_oak_planks", "minecraft:iron_block"
    dek_y = 14                                                     # the deck (world template y A + 2): two above the pier
    bodem_y = 4

    def breedte(x, yy):
        """Half the width of the hull at length x (0 = the bow, against the pier) and height yy."""
        voor = min(1.0, (x + 0.5) / 7.0)                           # the bow narrows to a point
        achter = min(1.0, (sx - x + 0.5) / 3.0)
        hoog = min(1.0, (yy - bodem_y + 1.0) / 6.0)                # rounder towards the keel
        return (mid - 0.3) * m.sqrt(max(0.0, voor)) * achter ** 0.3 * (0.35 + 0.65 * hoog)

    for x in range(sx):
        for yy in range(bodem_y, dek_y + 1):
            w = breedte(x, yy)
            for z in range(sz):
                d = abs(z - mid)
                if d > w + 0.5:
                    continue
                schil = d > w - 0.7 or yy == bodem_y or yy == dek_y
                if not schil:
                    continue
                if yy == dek_y and d <= w - 0.7:
                    s.set(x, yy, z, dek)
                elif yy in (dek_y, dek_y - 1):
                    s.set(x, yy, z, rand)                        # the red band under the railing
                else:
                    s.set(x, yy, z, romp)
        # the railing of the deck
        w = breedte(x, dek_y)
        for z in range(sz):
            if abs(abs(z - mid) - w) < 0.6 and x > 1:
                s.set(x, dek_y + 1, z, "minecraft:polished_blackstone_brick_wall")
    # the ram: an iron point in front of the bow, just above the pier's deck
    for i, (yy, n) in enumerate(((dek_y - 2, 1), (dek_y - 3, 0))):
        for z in range(mid - n, mid + n + 1):
            s.set(0, yy, z, ijzer)
    # the bridge tower (at the stern), its windows, the flags and the chimney
    for x in range(15, 22):
        for z in range(mid - 3, mid + 4):
            for yy in range(dek_y + 1, dek_y + 5):
                rand_ = x in (15, 21) or z in (mid - 3, mid + 3)
                if not rand_:
                    continue
                raam = yy == dek_y + 3 and (x in (15, 21) and abs(z - mid) <= 1 or z in (mid - 3, mid + 3) and 17 <= x <= 19)
                s.set(x, yy, z, "minecraft:red_stained_glass" if raam else romp)
            s.set(x, dek_y + 5, z, rand)
    for yy in range(dek_y + 6, sy - 1):
        for x, z in ((18, mid), (19, mid)):
            s.set(x, yy, z, "minecraft:polished_blackstone")
    s.set(18, sy - 1, mid, "minecraft:magma_block")
    s.set(19, sy - 1, mid, "minecraft:magma_block")
    for x in (6, 11):                                              # two masts with red pennants (wool, no banners: no block entities)
        for yy in range(dek_y + 1, dek_y + 7):
            s.set(x, yy, mid, "minecraft:stripped_dark_oak_log", {"axis": "y"})
        for i, yy in enumerate(range(dek_y + 4, dek_y + 7)):
            for zz in range(mid + 1, mid + 4 - i):
                s.set(x, yy, zz, "minecraft:red_wool")
        s.set(x, dek_y + 7, mid, "minecraft:gold_block")
    # the Vuurnatie emblem on both sides of the bow (a flame: orange over red, on the hull's outer cells)
    for x, yy in ((5, 10), (5, 9), (4, 9), (6, 9), (5, 8)):
        for z in range(sz):
            c = s.blocks.get((x, yy, z))
            if c and c[0] == romp:
                s.set(x, yy, z, "minecraft:orange_concrete" if yy == 10 else "minecraft:red_concrete")
    # (the splinters of the railing on the head are particles of the scene: the overlay's box starts east of the head)
    # only air: whatever of the build stands in the box stays the build's
    ax, ay, az = SCHIP
    for c in list(s.blocks):
        if b.s.blocks.get((ax + c[0], ay + c[1], az + c[2]), (gb.AIR,))[0] != gb.AIR:
            del s.blocks[c]
    b.laag("zuka_schip", s, ax, ay, az)
    return s


def rotsen(b):
    """Pinnacles of kruimelrots on the open ground (the temple stands on a mountain top): only where nothing is built."""
    for x, z, r, hoog in ((42, 56, 4, 13), (36, 68, 3, 8), (50, 148, 4, 11), (58, 152, 3, 7), (152, 68, 4, 12), (160, 74, 3, 7), (130, 150, 3, 9),
                          (54, 44, 3, 9), (136, 46, 4, 12), (146, 48, 3, 7), (118, 172, 3, 8), (54, 172, 3, 7), (49, 122, 3, 9)):
        cellen = [(ax, az) for ax in range(x - r - 1, x + r + 2) for az in range(z - r - 1, z + r + 2) if math.hypot(ax - x, az - z) <= r + 0.5]
        if any(b.top[ax][az] < 0 or b.naam(ax, b.top[ax][az], az) not in NATUUR or b.naam(ax, b.top[ax][az] + 1, az) is not None for ax, az in cellen):
            raise SystemExit(f"{NAAM}: de rots op {(x, z)} staat op iets dat gebouwd is")
        for ax, az in cellen:
            d = math.hypot(ax - x, az - z)
            top = int(round(hoog * (1 - d / (r + 0.8)) ** 0.8 + b.rng.uniform(-0.6, 0.6)))
            y0 = b.top[ax][az]
            for i in range(1, max(0, top) + 1):
                b.set(ax, y0 + i, az, KLIP if (y0 + i) % 5 == 0 else KORST if i == top and b.rng.random() < 0.5 else ROTS)
        punt = max(y for (ax, y, az) in ((x, yy, z) for yy in range(b.top[x][z], b.top[x][z] + hoog + 3)) if b.naam(ax, y, az) is not None)
        b.set(x, punt + 1, z, KRISTAL, {"facing": "up"})       # (a glowing crystal on the tip)


def momo_route(b, monnik, wind, dak):
    """The chase after Momo-guh: from the kitchen up the monnikentoren, over its bridge, once around the roof of the hall,
    over the other bridge and up the windtoren to his nest."""
    punten = [(78, G + 1, 46), (76, monnik[1] + 1, 50), (76, monnik[2] + 1, 50), (76, monnik[3] + 1, 54), (76, dak + 3, 64),
              (78, dak + 1, 77), (78, dak + 1, 96), (80, dak + 1, 114), (96, dak + 1, 112), (112, dak + 1, 114), (114, dak + 1, 96),
              (114, dak + 1, 77), (116, dak + 3, 64), (116, wind[3] + 1, 54), (116, wind[4] + 1, 49)]
    b.pad("momo_route", punten, wie="de achtervolging van Momo-guh: keuken, monnikentoren, brug, het dak van de hal rond, brug, windtoren, nest")
    return punten


# =====================================================================================================================
# light, tidying, checks
# =====================================================================================================================
def verlicht(b, start, min_licht=5):
    """The Guheinde has no sky light. After the lamps that were built on purpose, this pass finds every walkable spot that
    is still too dark and lights it: a kristalkaaskristal on natural ground, a glowing tile in a paved floor."""
    blocks = b.s.blocks

    def dicht(c):
        bl = blocks.get(c)
        if bl is None:
            return False
        n = bl[0]
        return n != AIR and n not in kit.WATER and not b.is_dun(n) and not any(t in n for t in gb.TRANSPARENT_SUB) and n not in kit.LICHT

    licht = b.licht()

    def straal(c, sterkte):
        licht[c] = sterkte
        todo = [c]
        while todo:
            volgende = []
            for q in todo:
                l = licht[q] - 1
                if l <= 0:
                    continue
                x, y, z = q
                for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                    if b.inside(*n) and licht.get(n, 0) < l and not dicht(n):
                        licht[n] = l
                        volgende.append(n)
            todo = volgende

    bereik = kit.Loper(b).bfs(tuple(start))
    for binnen in b.binnen_start:
        bereik |= kit.Loper(b).bfs(binnen)
    for c in sorted(bereik, key=lambda c: (c[1], c[0] * 31 + c[2] * 17)):
        if licht.get(c, 0) >= min_licht:
            continue
        x, y, z = c
        onder = blocks.get((x, y - 1, z))
        hier = blocks.get(c)
        if onder is None or (hier is not None and hier[0] != AIR):
            continue
        vrij = any(x0 <= x <= x1 and z0 <= z <= z1 and y0 <= y <= y1 for (x0, y0, z0, x1, y1, z1) in b.vrij)
        if onder[0] in NATUUR and not vrij:
            b.set(x, y, z, KRISTAL, {"facing": "up"})
            straal(c, kit.LICHT[KRISTAL])
            soort = "kristal"
        elif onder[0] in NATUUR + ("minecraft:smooth_stone", "minecraft:polished_andesite", "minecraft:stone", TEGEL, "minecraft:coarse_dirt", "minecraft:gravel",
                          "minecraft:polished_diorite", "minecraft:smooth_quartz", RSTEEN, "minecraft:spruce_planks", "minecraft:birch_planks"):
            b.set(x, y - 1, z, "pearlescent_froglight", {"axis": "y"})
            straal((x, y - 1, z), 15)
            soort = "vloerlicht in " + onder[0].split(":")[1]
        else:
            continue
        b.auto_licht[soort] = b.auto_licht.get(soort, 0) + 1


def opruimen(b, start):
    """Takes away what hangs loose by accident: a puff of cloud that does not touch the rock, a tuft on a carved edge."""
    onder = (start[0], start[1] - 1, start[2])
    los = gb.floating(b, lambda c: c == onder)
    weg = 0
    for c in los:
        if c in b.ringen:
            continue
        n = b.s.blocks[c][0]
        if n in (WOLK, "guhs:korstgras"):
            del b.s.blocks[c]
            weg += 1
    for c, bl in list(b.s.blocks.items()):              # a plant whose ground was built over or carved away
        if bl[0] in ("guhs:korstgras", KRISTAL) and b.naam(c[0], c[1] - 1, c[2]) not in NATUUR + (PLEIN, RSTEEN, "minecraft:polished_andesite", "minecraft:stone"):
            del b.s.blocks[c]
    return weg


def eigen_controle(b, start, route):
    """What kit.check does not know about this temple."""
    p = []
    w = kit.Loper(b)
    bereik = w.bfs(tuple(start))
    # the workshop and the yard are shut: nobody walks in from outside, and inside everything connects
    for (x0, y0, z0, x1, y1, z1) in b.binnen:
        erin = [c for c in bereik if x0 <= c[0] <= x1 and y0 <= c[1] <= y1 and z0 <= c[2] <= z1]
        if erin:
            p.append(f"de werkplaats is niet dicht: {len(erin)} plekken bereikbaar van buiten, bv. {sorted(erin)[:3]}")
    binnen = w.bfs(b.binnen_start[0])
    for id in ["werktuigguh", "werkbank", "vredesballon_anker", "vredesballon_teller"] + [f"oefen_{i}" for i in range(1, 7)]:
        c = tuple(b.plekken[id]["pos"])
        if c not in binnen and not gb.near(binnen, c, dy=(-1, 0, 1)):
            p.append(f"plek {id} {c} is van binnen in de werkplaats niet te bereiken")
    if not w.standable(tuple(b.plekken["werktuigguh"]["pos"])):
        p.append("de plek van de Werktuigguh is niet vrij")
    # every practice bay: 5 x 4 x 5 free on a floor of plain blocks
    for nr, (bx, bz) in enumerate(OEFEN, 1):
        for x in range(bx, bx + 5):
            for z in range(bz, bz + 5):
                if b.naam(x, G, z) not in ("minecraft:smooth_stone", "minecraft:pearlescent_froglight"):
                    p.append(f"oefen_{nr}: de vloer op {(x, G, z)} is {b.naam(x, G, z)}")
                for yy in range(G + 1, G + 5):
                    if b.naam(x, yy, z) is not None:
                        p.append(f"oefen_{nr}: {b.naam(x, yy, z)} staat in het vak op {(x, yy, z)}")
    # the door: 3 x 3 of door blocks, and the marker is its lower middle block
    dx, dy, dz = b.plekken["werkplaatsdeur"]["pos"]
    for z in range(dz - 1, dz + 2):
        for yy in range(dy, dy + 3):
            if b.naam(dx, yy, z) != DEUR:
                p.append(f"de werkplaatsdeur mist een blok op {(dx, yy, z)}")
    # free boxes: Appa in his stable (7 x 5 x 7), Appa in the camp (5 x 5), above the empty pedestal (3), the landing
    for id, rx, hoog, rz in (("appa", 3, 5, 3), ("appa_kamp", 2, 4, 2), ("voetstuk_leeg", 0, 3, 0)):
        x, y, z = b.plekken[id]["pos"]
        for ax in range(x - rx, x + rx + 1):
            for az in range(z - rz, z + rz + 1):
                for ay in range(y + (1 if id == "voetstuk_leeg" else 0), y + hoog + (1 if id == "voetstuk_leeg" else 0)):
                    if b.naam(ax, ay, az) not in (None, "guhs:korstgras"):
                        p.append(f"{id}: {b.naam(ax, ay, az)} staat in de vrije ruimte op {(ax, ay, az)}")
    a = b.plekken["aankomst"]
    for x in range(a["pos"][0], a["tot"][0] + 1):
        for z in range(a["pos"][2], a["tot"][2] + 1):
            for y in range(a["pos"][1], a["tot"][1] + 1):
                if b.naam(x, y, z) is not None:
                    p.append(f"aankomst: {b.naam(x, y, z)} staat op de landingsplek {(x, y, z)}")
    # the poles: every top free above, and from every pole another one (or a start platform) within a jump over one block
    toppen = {(x, z): y for x, y, z in b.palen}
    for id in ("luchtbal_start_speler", "luchtbal_start_aang"):
        x, y, z = b.plekken[id]["pos"]
        for ax in (x - 1, x, x + 1):
            toppen[(ax, z)] = y - 1
        if tuple(b.plekken[id]["pos"]) not in bereik:
            p.append(f"{id} is niet te belopen vanaf de landingsplek")
    gezien, todo = set(), [(b.plekken["luchtbal_start_speler"]["pos"][0], b.plekken["luchtbal_start_speler"]["pos"][2])]
    while todo:
        c = todo.pop()
        if c in gezien:
            continue
        gezien.add(c)
        for ddx in range(-2, 3):
            for ddz in range(-2, 3):
                n = (c[0] + ddx, c[1] + ddz)
                if n in toppen and n not in gezien and abs(ddx) + abs(ddz) <= 2 and abs(toppen[n] - toppen[c]) <= 1:
                    todo.append(n)
    los = [c for c in toppen if c not in gezien]
    if los:
        p.append(f"{len(los)} palen van het luchtbalveld zijn niet te bespringen, bv. {sorted(los)[:4]}")
    for x, y, z in b.palen:
        if b.naam(x, y + 1, z) is not None or b.naam(x, y + 2, z) is not None:
            p.append(f"paal {(x, y, z)}: niet vrij erboven")
    if len(b.palen) < 12:
        p.append(f"maar {len(b.palen)} palen")
    # the chase route: every point a place to stand that you can walk to
    for q in route:
        if tuple(q) not in bereik:
            p.append(f"momo_route {tuple(q)}: daar kom je lopend niet")
    # the track: every point of its line on the deck, and the wink's two points at least six apart
    for q in b.plekken["scooter_baan"]["punten"]:
        if tuple(q) not in bereik:
            p.append(f"scooter_baan {tuple(q)}: niet te belopen")
    s1, s2 = b.plekken["knipoog_sjokkel"]["punten"]
    if math.dist(s1, s2) < 6:
        p.append(f"knipoog_sjokkel: de twee punten liggen maar {math.dist(s1, s2):.1f} uit elkaar")
    # the rings hang free and are the only things that do
    for nr, (x, y, z, as_) in enumerate(RINGEN, 1):
        for d in range(-3, 4):
            for e in range(-3, 4):
                c = (x, y + e, z + d) if as_ == "x" else (x + d, y + e, z)
                if math.hypot(d, e) <= 3.5 and c in b.s.blocks:
                    p.append(f"zweefring {nr}: {b.s.blocks[c][0]} in de opening op {c}")
    if tuple(b.plekken["zweef_start"]["pos"]) not in bereik:
        p.append("zweef_start is niet te belopen")
    return p


VERWACHT = ["aang", "poort", "hal", ("beeld", 8), "kyoshi_beeld", "roku_beeld", "voetstuk_leeg", "guhatso", "taartenbank", "luchtbal_veld",
            ("luchtbal_paal", 12), "luchtbal_doel_speler", "luchtbal_doel_aang", "luchtbal_start_speler", "luchtbal_start_aang", "scooter_start",
            "scooter_finish", "scooter_baan", "zweef_start", ("zweefring", 10), "appa_stal", "appa", "zadel", "appels", "momo", "momo_route",
            "momo_eind", "monnikenverblijf", "bed_speler", "werkplaatsdeur", "werkplaats", "werktuigguh", "werkbank", ("oefen", 6),
            "vredesballon_bouwplaats", "vredesballon_anker", "vredesballon_teller", "knipoog_sjokkel", "knipoog_sjokkel_camera",
            # einde2 phase B (h1_lucht): the pier, Zuka's ship and the escape
            "steiger", "steiger_zone", "zuka_confrontatie", "zuka_schip_anker", "rustpunt_5", ("ontsnap_wacht", 4)]


def maak(h):
    """The whole build, checked; returns the Tempel (not saved yet)."""
    b = Tempel(h)
    start = (96, A + 1, 172)
    eiland(b)
    wolken(b)
    aankomst(b)
    kamp(b)
    trappen_en_poort(b)
    dak = hal(b)
    monnik = monnikentoren(b)
    wind = windtoren(b)
    midden, _ = grote_toren(b, dak)
    brug(b, 76, dak)
    brug(b, 116, dak)
    stal(b)
    luchtbalveld(b)
    taartenterras(b)
    scooterbaan(b)
    werkplaats(b)
    bouwplaats(b)
    rotsen(b)
    tuin(b)
    steiger(b)
    zweefringen(b)
    route = momo_route(b, monnik, wind, dak)
    b.binnen_start = [(141, G + 1, 96)]
    weg = opruimen(b, start)
    trapvormen(b)
    b.connect()
    verlicht(b, start)
    b.connect()
    b.route = route
    b.start = start
    b.weg = weg
    return b


def controleer(b):
    problems = b.check(b.start, VERWACHT, min_licht=4, zwevend_ok=lambda c: c in b.ringen)
    problems += eigen_controle(b, b.start, b.route)
    return problems
