"""
einde2, slice bouw_vuur: the ground of the Vuurnatie-hoofdstad, a floating slab of scorched kaaskorst with two craters.

A height field first (numpy: the top and the underside of every column, what its surface is made of, where water stands),
then blocks: only a crust is written (the slab is hollow inside, the tiles stay small), solid rock only where a room is
carved into it (diep). einde2_bouw_vuur_bouw.py shapes the field and builds on it.

  Grond.top / bot      the y of the highest and the lowest block of a column (top -1: no land)
  Grond.soort          what the surface is (ROTS, WEG, ZAND ...): decides the blocks
  Grond.vast           cells somebody designed (a path, a building pad): the slope pass leaves them alone
  Grond.diep           the column is solid rock down to this y (a cave, a tunnel)
  Grond.nat            (level, fluid) of a column in a closed basin
"""
import math

import numpy as np

W = D = 256
H = 118
G = 44            # the ground outside the craters
F = 50            # the floor of the big crater (the city)

ROTS, WEG, ZAND, PLEIN, BODEM, RUINE, KORST, KLIF, GLOED = range(9)
KOOL = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
IJZER = "guhs:roosterijzer"
GLOEI = "guhs:gloeikool"

# what a surface is made of: (block, weight)
OPPERVLAK = {
    ROTS: [(KOOL, 62), (IJZER, 14), ("guhs:as_aarde", 10), ("minecraft:smooth_basalt", 8), ("minecraft:tuff", 6)],
    WEG: [(STENEN, 70), ("guhs:gebarsten_houtskoolsteen_stenen", 22), ("guhs:gepolijst_roosterijzer", 8)],
    ZAND: [("minecraft:sand", 68), ("guhs:as_aarde", 22), ("minecraft:gray_concrete_powder", 10)],
    PLEIN: [("minecraft:polished_blackstone_bricks", 62), ("minecraft:cracked_polished_blackstone_bricks", 14), ("minecraft:red_nether_bricks", 24)],
    BODEM: [(KOOL, 50), ("guhs:as_aarde", 22), ("minecraft:smooth_basalt", 20), ("minecraft:tuff", 8)],
    RUINE: [("minecraft:cut_sandstone", 40), ("minecraft:sandstone", 30), ("minecraft:smooth_sandstone", 18), ("minecraft:chiseled_sandstone", 4),
            (KOOL, 8)],
    KORST: [("guhs:kaaskorst", 78), ("guhs:gebarsten_korst", 14), (KOOL, 8)],
    KLIF: [("minecraft:basalt", 36), (IJZER, 30), (KOOL, 22), ("minecraft:smooth_basalt", 12)],
    GLOED: [(KOOL, 50), (GLOEI, 26), (IJZER, 24)],
}
ONDER = [("guhs:kaaskorst", 62), (KOOL, 26), ("guhs:gebarsten_korst", 12)]       # the underside: the crust the fire never reached


def ruis(rng, schaal, w=W, d=D):
    """Smooth noise in -1..1: random values on a grid `schaal` blocks apart, blended in between."""
    gx, gz = w // schaal + 3, d // schaal + 3
    grid = rng.uniform(-1.0, 1.0, (gx, gz))
    x = np.arange(w) / schaal
    z = np.arange(d) / schaal
    x0, z0 = x.astype(int), z.astype(int)
    fx, fz = x - x0, z - z0
    fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    a = grid[np.ix_(x0, z0)]
    b = grid[np.ix_(x0 + 1, z0)]
    c = grid[np.ix_(x0, z0 + 1)]
    e = grid[np.ix_(x0 + 1, z0 + 1)]
    fx, fz = fx[:, None], fz[None, :]
    return (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + e * fx) * fz


def afstand(buiten):
    """For every cell the distance (blocks, chamfer) to the nearest cell where `buiten` is true."""
    d = np.where(buiten, 0.0, 1e6)
    for _ in range(400):
        oud = d
        p = np.pad(d, 1, constant_values=1e6)
        d = np.minimum(d, np.minimum(np.minimum(p[:-2, 1:-1], p[2:, 1:-1]), np.minimum(p[1:-1, :-2], p[1:-1, 2:])) + 1.0)
        d = np.minimum(d, np.minimum(np.minimum(p[:-2, :-2], p[2:, 2:]), np.minimum(p[:-2, 2:], p[2:, :-2])) + 1.41421356)
        if np.array_equal(oud, d):
            break
    return d


def glad(t):
    t = np.clip(t, 0.0, 1.0)
    return t * t * (3 - 2 * t)


class Grond:
    def __init__(self, seed):
        self.rng = np.random.default_rng(seed)
        self.X, self.Z = np.meshgrid(np.arange(W), np.arange(D), indexing="ij")
        self.top = np.full((W, D), -1, dtype=np.int32)
        self.bot = np.zeros((W, D), dtype=np.int32)
        self.soort = np.zeros((W, D), dtype=np.int8)
        self.vast = np.zeros((W, D), dtype=bool)
        self.diep = np.full((W, D), 999, dtype=np.int32)
        self.nat = {}
        self.land = np.zeros((W, D), dtype=bool)
        self.rand = None           # the distance to the rim of the slab

    # --- the slab ---------------------------------------------------------------------------------------------------------
    def plaat(self, happen=()):
        """The outline (a rounded square with a ragged rim, minus the bites of `happen` = [(cx, cz, r)]), the flat ground
        with a little relief and the tapering underside."""
        dx, dz = np.abs(self.X - 127.5) / 124.0, np.abs(self.Z - 127.5) / 124.0
        f = dx ** 3 + dz ** 3
        rafel = ruis(self.rng, 14) * 0.07 + ruis(self.rng, 5) * 0.025
        land = f < 0.93 + rafel
        for cx, cz, r in happen:
            land &= np.hypot(self.X - cx, self.Z - cz) > r + ruis(self.rng, 9) * 3.0
        land[:2, :] = land[-2:, :] = False
        land[:, :2] = land[:, -2:] = False
        self.land = land
        self.rand = afstand(~land)
        relief = ruis(self.rng, 24) * 1.4 + ruis(self.rng, 9) * 0.6
        self.top = np.where(land, np.rint(G + relief).astype(np.int32), -1)
        self.onderkant()

    def onderkant(self):
        diepte = 34.0 * (1 - np.exp(-self.rand / 26.0))
        bobbel = ruis(self.rng, 18) * 3.5 + ruis(self.rng, 6) * 1.5
        pegel = np.where(ruis(self.rng, 4) > 0.72, -5.0 * self.rng.uniform(0.4, 1.0, (W, D)), 0.0)     # (stalactites)
        self.bot = np.rint(G - 5 - diepte + bobbel + pegel * np.clip(self.rand / 12.0, 0, 1)).astype(np.int32)
        self.bot = np.clip(self.bot, 2, G - 4)

    # --- shapes -----------------------------------------------------------------------------------------------------------
    def krater(self, cx, cz, r_vloer, r_kam, r_voet, vloer, kam, soort_binnen=PLEIN, golf=2.5):
        """A crater: a flat floor, a steep inner wall up to the crest and a flank down to the ground outside."""
        hoek = np.arctan2(self.Z - cz, self.X - cx)
        r = np.hypot(self.X - cx, self.Z - cz)
        r = r + golf * (np.sin(hoek * 3 + 0.7) * 0.5 + np.sin(hoek * 7 + 2.1) * 0.3 + ruis(self.rng, 11) * 0.6)
        kamruis = ruis(self.rng, 7) * 2.2
        binnen = vloer + (kam - vloer) * glad((r - r_vloer) / (r_kam - 2 - r_vloer)) ** 0.8
        buiten = G + (kam - G) * (1 - glad((r - r_kam) / (r_voet - r_kam))) ** 1.25
        h = np.where(r < r_kam - 1, binnen, buiten) + np.where((r > r_vloer + 3) & (r < r_voet - 3), kamruis, 0.0)
        h = np.rint(h).astype(np.int32)
        in_krater = (np.hypot(self.X - cx, self.Z - cz) < r_vloer - 1.5) & self.land
        doe = self.land & (r < r_voet)
        self.top = np.where(doe & (r < r_kam - 1), h, np.where(doe, np.maximum(self.top, h), self.top))
        self.top = np.where(in_krater, vloer, self.top)
        self.soort = np.where(in_krater, soort_binnen, self.soort)
        self.soort = np.where(doe & ~in_krater & (r > r_vloer - 1), KLIF, self.soort)
        self.soort = np.where(doe & (np.abs(r - r_kam) < 2.5), GLOED, self.soort)

    def berg(self, cx, cz, r, hoogte, steil=1.0):
        """A cone on the ground (a peak of the Zonnekrijger ridge)."""
        d = np.hypot(self.X - cx, self.Z - cz) + ruis(self.rng, 6) * 1.6
        h = np.rint(G + (hoogte - G) * (1 - np.clip(d / r, 0, 1)) ** steil).astype(np.int32)
        doe = self.land & (d < r) & (h > self.top)
        self.top = np.where(doe, h, self.top)
        self.soort = np.where(doe, KLIF, self.soort)

    def vlak(self, x0, z0, x1, z1, y, soort=None, vast=True):
        """A flat rectangle (a building pad, a square)."""
        s = (slice(min(x0, x1), max(x0, x1) + 1), slice(min(z0, z1), max(z0, z1) + 1))
        ok = self.land[s]
        self.top[s] = np.where(ok, y, self.top[s])
        if soort is not None:
            self.soort[s] = np.where(ok, soort, self.soort[s])
        if vast:
            self.vast[s] |= ok

    def schijf(self, cx, cz, r, y, soort=None, vast=True):
        doe = self.land & (np.hypot(self.X - cx, self.Z - cz) <= r)
        self.top = np.where(doe, y, self.top)
        if soort is not None:
            self.soort = np.where(doe, soort, self.soort)
        if vast:
            self.vast |= doe

    def weg(self, punten, breed=5, soort=WEG, maak_land=False):
        """A path along (x, z, y) points: `breed` wide, its height running evenly from point to point. With maak_land the
        path also lies where the slab ends (a pier, a ledge along a ragged rim). Returns its cells."""
        cellen = {}
        half = (breed - 1) / 2.0
        for (ax, az, ay), (bx, bz, by) in zip(punten, punten[1:]):
            n = int(max(abs(bx - ax), abs(bz - az)) * 2) + 1
            for k in range(n + 1):
                t = k / n
                px, pz, py = ax + (bx - ax) * t, az + (bz - az) * t, ay + (by - ay) * t
                for x in range(int(math.floor(px - half)), int(math.ceil(px + half)) + 1):
                    for z in range(int(math.floor(pz - half)), int(math.ceil(pz + half)) + 1):
                        if 2 <= x < W - 2 and 2 <= z < D - 2 and (self.land[x, z] or maak_land) and math.hypot(x - px, z - pz) <= half + 0.35:
                            d = abs(x - px) + abs(z - pz)
                            oud = cellen.get((x, z))
                            if oud is None or d < oud[1]:
                                cellen[(x, z)] = (int(round(py)), d)
        for (x, z), (y, _) in cellen.items():
            if not self.land[x, z]:
                self.land[x, z] = True
                self.bot[x, z] = max(2, y - 5)
                self.rand[x, z] = 1.0
            self.top[x, z] = y
            self.soort[x, z] = soort
            self.vast[x, z] = True
            self.nat.pop((x, z), None)
        return set(cellen)

    def zuil(self, cx, cz, r, y, soort=KLIF):
        """A column of rock standing in a basin (a pillar of the arena, the island of the prison)."""
        doe = np.hypot(self.X - cx, self.Z - cz) <= r
        self.land |= doe
        self.top = np.where(doe, y, self.top)
        self.soort = np.where(doe, soort, self.soort)
        self.vast |= doe
        for x, z in zip(*np.nonzero(doe)):
            self.nat.pop((int(x), int(z)), None)

    def streek(self, cellen, soort, y=None):
        """A patch of another surface (a beach, a meadow), flattened to y when given; what was designed stays."""
        doe = cellen & self.land & ~self.vast
        self.soort = np.where(doe, soort, self.soort)
        if y is not None:
            self.top = np.where(doe, y, self.top)

    def bekken(self, cellen, vloer, niveau, vloeistof="minecraft:water", soort=BODEM, rand_min=6):
        """A closed basin: the cells (a bool field) get a floor at `vloer` and stand full up to `niveau`; cells nearer than
        rand_min to the rim of the slab stay land (the lip), and what was designed (a path, a pad) stays too."""
        doe = cellen & self.land & (self.rand >= rand_min) & ~self.vast
        self.top = np.where(doe, np.minimum(self.top, vloer), self.top)
        self.soort = np.where(doe, soort, self.soort)
        self.vast |= doe
        for x, z in zip(*np.nonzero(doe)):
            self.nat[(int(x), int(z))] = (niveau, vloeistof)
        return doe

    def oever(self, hoog=1):
        """Whatever borders a basin is at least as high as its level (plus `hoog` - 1): nothing flows over a rim."""
        for (x, z), (niveau, _) in list(self.nat.items()):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    n = (x + dx, z + dz)
                    if n in self.nat or not (0 <= n[0] < W and 0 <= n[1] < D):
                        continue
                    if not self.land[n]:
                        self.land[n] = True
                        self.bot[n] = min(self.bot[x, z], niveau - 4)
                        self.soort[n] = KLIF
                    if self.top[n] < niveau + hoog - 1:
                        self.top[n] = niveau + hoog - 1

    def talud(self, afstand_=7, stap=2):
        """Around what was designed the ground rises or falls at most `stap` per block (no wall hangs over a path)."""
        grens = self.vast.copy()
        gedaan = self.vast.copy()
        for _ in range(afstand_):
            nieuw = np.zeros_like(grens)
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                src = np.roll(grens, (dx, dz), axis=(0, 1))
                h = np.roll(self.top, (dx, dz), axis=(0, 1))
                doe = src & ~gedaan & self.land
                if dx == 1:
                    doe[0, :] = False
                elif dx == -1:
                    doe[-1, :] = False
                elif dz == 1:
                    doe[:, 0] = False
                else:
                    doe[:, -1] = False
                self.top = np.where(doe, np.clip(self.top, h - stap, h + stap), self.top)
                nieuw |= doe
            gedaan |= nieuw
            grens = nieuw

    def koepel(self, cx, cz, r, y, steil=2.0):
        """At least a mound of rock here: y high within r of the middle, falling `steil` per block outside it (the roof
        over a cave that is carved out later). What was designed stays as it is."""
        d = np.hypot(self.X - cx, self.Z - cz)
        h = np.rint(y - steil * np.maximum(d - r, 0)).astype(np.int32)
        self.top = np.where(self.land & ~self.vast & (h > self.top), h, self.top)

    def massief(self, x0, z0, x1, z1, y):
        """These columns are solid rock down to y (something is carved into them)."""
        s = (slice(min(x0, x1), max(x0, x1) + 1), slice(min(z0, z1), max(z0, z1) + 1))
        self.diep[s] = np.minimum(self.diep[s], y)

    def hoogte(self, x, z):
        return int(self.top[x, z])

    # --- to blocks --------------------------------------------------------------------------------------------------------
    def zet(self, b, kies):
        """Writes the crust into the build b. kies(table) picks a block of a weighted table (the build's own rng)."""
        top, bot, land = self.top, self.bot, self.land
        self.top = np.where(land, top, -1)
        top = self.top
        buur_laag = top.copy()
        buur_hoog_bot = np.where(land, bot, -999)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            t = np.roll(top, (dx, dz), axis=(0, 1))
            l = np.roll(land, (dx, dz), axis=(0, 1))
            bb = np.roll(bot, (dx, dz), axis=(0, 1))
            buur_laag = np.minimum(buur_laag, np.where(l, t, 9999))
            # (a neighbour that is no land: the side of the slab shows, close it all the way down)
            buur_laag = np.where(land & ~l, -999, buur_laag)
            buur_hoog_bot = np.maximum(buur_hoog_bot, np.where(l, bb, -999))
        steil = np.zeros((W, D), dtype=np.int32)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            steil = np.maximum(steil, np.abs(top - np.roll(top, (dx, dz), axis=(0, 1))))
        for x in range(W):
            for z in range(D):
                if not land[x, z]:
                    continue
                t, bo = int(top[x, z]), int(bot[x, z])
                bo = min(bo, t - 3)
                lo = max(bo, min(t - 3, int(buur_laag[x, z]) - 1, int(self.diep[x, z])))
                soort = int(self.soort[x, z])
                if steil[x, z] >= 3 and soort in (ROTS, GLOED):
                    soort = KLIF
                tabel = OPPERVLAK[soort]
                open_ = int(buur_laag[x, z])          # (above this y a side of the column shows: a cliff, the rim of the slab)
                for y in range(lo, t + 1):
                    if y == t:
                        b.set(x, y, z, kies(tabel))
                    elif y >= t - 2 and soort in (KLIF, ROTS, GLOED, BODEM):
                        b.set(x, y, z, kies(OPPERVLAK[KLIF if steil[x, z] >= 3 else ROTS]))
                    elif y > open_:
                        # the deeper down the rim, the less the fire reached: scorched rock above, kaaskorst below
                        b.set(x, y, z, kies(ONDER) if (G - 8 - y) / 12.0 > self.rng.random() else kies(OPPERVLAK[KLIF]))
                    else:
                        b.set(x, y, z, KOOL)
                hi = min(lo - 1, max(bo + 2, int(buur_hoog_bot[x, z]) + 1))
                for y in range(bo, hi + 1):
                    b.set(x, y, z, kies(ONDER) if y <= bo + 3 else KOOL)
                nat = self.nat.get((x, z))
                if nat:
                    for y in range(t + 1, nat[0] + 1):
                        b.set(x, y, z, nat[1], {"level": "0"})

    def bekken_fouten(self):
        """Every basin is closed: next to a wet column stands a wet column or ground at least as high as the level."""
        p = []
        for (x, z), (niveau, _) in self.nat.items():
            if self.top[x, z] >= niveau:
                continue
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, z + dz)
                if n in self.nat and self.nat[n][0] >= niveau:
                    continue
                if not (0 <= n[0] < W and 0 <= n[1] < D) or not self.land[n] or self.top[n] < niveau:
                    p.append(f"bekken lekt bij {(x, z)} naar {n}")
        return p[:20]
