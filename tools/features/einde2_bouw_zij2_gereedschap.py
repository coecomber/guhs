"""
einde2, slice bouw_zij2: the tools the five builders of this slice share (einde2_bouw_zij2_race / _haven / _kyoshi / _bieb /
_schildpad). Not a feature module.

  - Ruis: smooth value noise from a name (rims, dunes, undersides: never a straight line);
  - eiland: a floating mass with an irregular rim and a tapering underside, hollow inside (the tiles stay small);
  - Licht + verlicht: the block light of a build, kept up to date while lights are added, and the pass that puts a light on
    every walkable spot that is still too dark (the Guheinde has no skylight);
  - small furniture every place uses: a lamp post, a door, a bed, a pot plant, a banner, a vetschip of any size and heading;
  - voxel_guh_in: the guh model as blocks inside a Bouw (the statue of Kyoshi-guh, the head of the Leeuwschildpad-guh).
"""
import math
import random
from collections import deque

import features.einde2_kit as kit
from features import guheinde_bouw as gb

AIR = gb.AIR
PREFIX = "sign.guhs.einde2_bouw_zij2"
DIRS = gb.DIRS
OPP = gb.OPP
LINKS = {"north": "west", "west": "south", "south": "east", "east": "north"}     # (the left hand of someone facing that way)
RECHTS = {v: k for k, v in LINKS.items()}
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}


class Ruis:
    """Smooth value noise in two dimensions, 0..1, seeded from a name."""

    def __init__(self, naam, schaal=8.0):
        self.seed = kit.seed("ruis/" + naam)
        self.schaal = float(schaal)
        self.cache = {}

    def _punt(self, i, j):
        v = self.cache.get((i, j))
        if v is None:
            v = self.cache[(i, j)] = random.Random((self.seed * 73856093) ^ (i * 19349663) ^ (j * 83492791)).random()
        return v

    def __call__(self, x, z):
        x, z = x / self.schaal, z / self.schaal
        i, j = math.floor(x), math.floor(z)
        fx, fz = x - i, z - j
        fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
        a = self._punt(i, j) + (self._punt(i + 1, j) - self._punt(i, j)) * fx
        c = self._punt(i, j + 1) + (self._punt(i + 1, j + 1) - self._punt(i, j + 1)) * fx
        return a + (c - a) * fz


def eiland(b, cx, cz, rx, rz, top, dik, grond, steen, naam, rafel=0.2, hol=True, top_fn=None, onder=None, binnen=None):
    """A floating island: an ellipse (rx, rz) around (cx, cz) with a frayed rim, its surface at `top` (or top_fn(x, z, m),
    m = 0 at the rim .. 1 in the middle) and `dik` blocks thick in the middle, tapering to the rim. grond = the surface block,
    steen = what lies under it (a block, or a weighted table for gb.pick), onder = the block of the lowest layer (default:
    steen). hol: only a shell is written (the inside stays empty: nobody sees it and the tiles stay small).
    binnen(x, z) -> False leaves a column out (a bay, a notch). Returns {(x, z): the surface y}."""
    rand, bult = Ruis(naam + "/rand", 9.0), Ruis(naam + "/bult", 7.0)
    rng = random.Random(kit.seed(naam))
    toppen, bodems, maat = {}, {}, {}
    for x in range(int(cx - rx * (1 + rafel)) - 1, int(cx + rx * (1 + rafel)) + 2):
        for z in range(int(cz - rz * (1 + rafel)) - 1, int(cz + rz * (1 + rafel)) + 2):
            dx, dz = (x - cx) / rx, (z - cz) / rz
            d = math.hypot(dx, dz)
            hoek = math.atan2(dz, dx)
            e = 1 + rafel * (rand(40 + math.cos(hoek) * 22, 40 + math.sin(hoek) * 22) * 2 - 1)
            if d >= e or (binnen and not binnen(x, z)):
                continue
            m = 1 - d / e
            t = top_fn(x, z, m) if top_fn else top
            diepte = 1.5 + dik * (1 - (1 - m) ** 2.2) * (0.7 + 0.6 * bult(x, z))
            toppen[(x, z)], bodems[(x, z)], maat[(x, z)] = t, int(round(t - diepte)), m
    for (x, z), t in toppen.items():
        lo = bodems[(x, z)]
        buren = [(x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)]
        # the lowest neighbour bottom and the highest neighbour top: what of this column the outside can see
        open_onder = max([bodems[n] for n in buren if n in bodems] + [lo]) + 2
        rand_kolom = any(n not in toppen for n in buren)
        open_boven = min([toppen[n] for n in buren if n in toppen] + [t]) - 3
        for y in range(lo, t + 1):
            if hol and not rand_kolom and open_onder < y < open_boven:
                continue
            if y == t:
                blok = grond
            elif y == lo and onder:
                blok = onder
            else:
                blok = steen if isinstance(steen, str) else gb.pick(rng, steen)
            b.set(x, y, z, blok)
    return toppen


class Licht:
    """The block light of a build (kit.LICHT), kept up to date while lights are added."""

    def __init__(self, b):
        self.b = b
        self.light = {}
        todo = deque()
        for c, bl in b.s.blocks.items():
            if kit.LICHT.get(bl[0]):
                self.light[c] = kit.LICHT[bl[0]]
                todo.append(c)
        self._spreid(todo)

    def _dicht(self, c):
        bl = self.b.s.blocks.get(c)
        if bl is None:
            return False
        n = bl[0]
        return n != AIR and n not in kit.WATER and not self.b.is_dun(n) and not any(t in n for t in gb.TRANSPARENT_SUB) and n not in kit.LICHT

    def _spreid(self, todo):
        light, b = self.light, self.b
        while todo:
            c = todo.popleft()
            l = light[c] - 1
            if l <= 0:
                continue
            x, y, z = c
            for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if light.get(n, 0) < l and b.inside(*n) and not self._dicht(n):
                    light[n] = l
                    todo.append(n)

    def zet(self, c, sterkte):
        if self.light.get(c, 0) < sterkte:
            self.light[c] = sterkte
            self._spreid(deque([c]))

    def bij(self, c):
        return self.light.get(c, 0)


VOL = ("_trap", "_stairs", "_plaat", "_slab", "_fence", "_muur", "_wall", "chest", "barrel", "_door", "_bed", "bookshelf", "lectern",
       "furnace", "crafting", "table", "glass", "leaves", "water", "saus", "guhs:kruimelwolk", "ringenrace", "carpet", "_sign", "jukebox", "note_block",
       "loom", "cauldron", "composter", "smoker", "anvil", "grindstone", "kruimelzand", "_log", "_stam", "_wood", "honey", "wool", "hay",
       "target", "bell", "campfire", "ladder", "scaffolding")


def in_vloer(lamp):
    """A light for verlicht(): the floor block under the dark spot becomes this glowing block (only a plain full block is
    swapped: never a stair, a slab, a chest, a log, a rug...)."""
    def kies(b, c):
        onder = (c[0], c[1] - 1, c[2])
        bl = b.s.blocks.get(onder)
        if bl is None or bl[2] is not None or any(t in bl[0] for t in VOL) or kit.LICHT.get(bl[0]):
            return None
        b.set(*onder, lamp)
        return onder, kit.LICHT[gb.mc(lamp)]
    return kies


def in_wand(lamp):
    """A light for verlicht(): a plain full block of the wall right next to the dark spot (at foot or head height) becomes
    this glowing block. For stairs and narrow corridors, where nothing can stand on the floor."""
    def kies(b, c):
        for dy in (1, 0):
            for dx, dz in DIRS.values():
                n = (c[0] + dx, c[1] + dy, c[2] + dz)
                bl = b.s.blocks.get(n)
                if bl is None or bl[2] is not None or bl[0] == AIR or b.is_dun(bl[0]) or any(t in bl[0] for t in VOL) or kit.LICHT.get(bl[0]):
                    continue
                b.set(*n, lamp)
                return n, kit.LICHT[gb.mc(lamp)]
        return None
    return kies


def op_vloer(blok, props=None):
    """A light for verlicht(): this small glowing thing (a mushroom, a lichtstaafje, a lantern) stands on the dark spot."""
    def kies(b, c):
        onder = b.s.blocks.get((c[0], c[1] - 1, c[2]))
        hier = b.s.blocks.get(c)
        if onder is None or any(t in onder[0] for t in VOL[:10]) or (hier is not None and hier[0] != AIR):
            return None
        b.set(*c, blok, props)
        return c, kit.LICHT[gb.mc(blok)]
    return kies


def verlicht(b, start, kiezers, min_licht=5, grenzen=None):
    """Puts a light on every walkable spot that is darker than min_licht (the check of the kit wants 4). kiezers = the ways
    to do it, tried in order (in_vloer / op_vloer / a function (b, cell) -> (cell, level) or None); or a function
    cell -> kiezers, to light a floor of planks differently from the ground outside. Returns how many lights it placed and
    the spots it could not light."""
    reach = kit.Loper(b).bfs(tuple(start))
    licht = Licht(b)
    geplaatst, over = 0, []
    for c in sorted(reach):
        if licht.bij(c) >= min_licht or (grenzen and not grenzen(c)):
            continue
        for kies in (kiezers(c) if callable(kiezers) else kiezers):
            res = kies(b, c)
            if res:
                licht.zet(*res)
                geplaatst += 1
                break
        else:
            over.append(c)
    return geplaatst, over


# =====================================================================================================================
# small furniture
# =====================================================================================================================
def lantaarnpaal(b, x, y, z, hoogte=3, hek="spruce_fence", lamp="guhs:lampion_geel"):
    """A fence post with a lampion on top; y = the first block of the post."""
    for k in range(hoogte):
        b.set(x, y + k, z, hek)
    b.set(x, y + hoogte, z, lamp, gb.lamp(False))


def hanglamp(b, x, y, z, ketting=1, lamp="minecraft:lantern"):
    """A lantern on a chain under the block above (x, y, z); y = the highest chain link."""
    for k in range(ketting):
        b.set(x, y - k, z, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(x, y - ketting, z, lamp, gb.lamp(True))


def deur(b, x, y, z, facing, hout="spruce", scharnier="left", open_=False):
    for half, dy in (("lower", 0), ("upper", 1)):
        b.set(x, y + dy, z, f"{hout}_door", {"facing": facing, "half": half, "hinge": scharnier, "open": "true" if open_ else "false",
                                              "powered": "false"})


def bed(b, x, y, z, facing, kleur="pink"):
    """The foot at (x, y, z), the head one block further in `facing`."""
    dx, dz = DIRS[facing]
    b.set(x, y, z, f"{kleur}_bed", {"facing": facing, "part": "foot", "occupied": "false"})
    b.set(x + dx, y, z + dz, f"{kleur}_bed", {"facing": facing, "part": "head", "occupied": "false"})


def trap(b, x, y, z, blok, facing, half="bottom", vorm="straight"):
    b.set(x, y, z, blok, {"facing": facing, "half": half, "shape": vorm, "waterlogged": "false"})


def plaat(b, x, y, z, blok, t="bottom"):
    b.set(x, y, z, blok, {"type": t, "waterlogged": "false"})


def luik(b, x, y, z, facing, hout="spruce", half="bottom", open_=False):
    b.set(x, y, z, f"{hout}_trapdoor", {"facing": facing, "half": half, "open": "true" if open_ else "false", "powered": "false",
                                        "waterlogged": "false"})


def ton(b, x, y, z, facing="up"):
    b.set(x, y, z, "barrel", {"facing": facing, "open": "false"})


def stam(b, x, y, z, blok, as_="y"):
    b.set(x, y, z, blok, {"axis": as_})


def blad(b, x, y, z, blok="oak_leaves"):
    """A leaf block that never decays (vanilla leaves need persistent; our own leaf blocks have no states)."""
    if gb.mc(blok).startswith("minecraft:"):
        b.set(x, y, z, blok, {"distance": "7", "persistent": "true", "waterlogged": "false"})
    else:
        b.set(x, y, z, blok)


def vlag(b, x, y, z, kleur, facing=None, rotatie=0):
    """A plain banner: on the wall behind it (facing) or standing (rotatie 0-15)."""
    if facing:
        b.set(x, y, z, f"{kleur}_wall_banner", {"facing": facing})
    else:
        b.set(x, y, z, f"{kleur}_banner", {"rotation": str(rotatie)})


def schaakrand(b, cellen, y, a="white_concrete", z_="black_concrete"):
    """A chequered line (start, finish) on these (x, z) cells."""
    for x, z in cellen:
        b.set(x, y, z, a if (x + z) % 2 == 0 else z_)


def bol(b, cx, cy, cz, r, blok, alleen_leeg=False, plat=1.0):
    """A ball (plat < 1: flattened) of blocks; blok may be a function (x, y, z) -> block name or None."""
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for y in range(int(cy - r * plat - 1), int(cy + r * plat + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                if math.hypot(x - cx, (y - cy) / plat, z - cz) <= r + 0.2 and b.inside(x, y, z):
                    if alleen_leeg and (x, y, z) in b.s.blocks:
                        continue
                    n = blok(x, y, z) if callable(blok) else blok
                    if n:
                        b.set(x, y, z, n)


def wolkje(b, cx, cy, cz, r, naam, wolk="guhs:kruimelwolk"):
    """A loose puff of cloud: two or three flattened balls."""
    rng = random.Random(kit.seed(naam))
    for _ in range(rng.randint(2, 3)):
        bol(b, cx + rng.uniform(-r, r) * 0.7, cy + rng.uniform(-0.6, 0.6), cz + rng.uniform(-r, r) * 0.7, r * rng.uniform(0.6, 1.0), wolk,
            alleen_leeg=True, plat=0.45)


GUH_BOTTEN = ("body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right")
GUH_OOG = ("minecraft:black_concrete", "minecraft:white_concrete", "minecraft:light_blue_concrete", "minecraft:white_wool", "minecraft:black_wool",
           "minecraft:gray_concrete", "minecraft:gray_terracotta", "minecraft:light_blue_terracotta")


def guh_deel(xm, ym, zm):
    """Which part of the guh a point of the model is (model pixels: x across, y up, z from the nose at -12.75 to the tail tip
    at 18.5): "oor", "kop", "staart", "poot" or "lijf"."""
    if ym >= 10 and abs(xm) >= 4.5 and -6.3 <= zm <= -4.7 and not (abs(xm) < 8 and ym < 12.5 and zm < -5.5):
        return "oor"
    if zm < -2.4:
        return "kop"
    if zm > 12.05:
        return "staart"
    if ym < 2.5 and abs(xm) >= 4.25 and not (-6.5 <= xm <= 6.5 and ym >= 1.0 and not (-2.5 <= zm <= 2 or 7 <= zm <= 11.5)):
        return "poot"
    return "lijf"


def voxel_guh_in(b, ox, oy, oz, schaal, botten=None, naar_zuid=False, kleur=None):
    """The guh model as blocks (make_structures.voxel_guh) with the low corner of its box at (ox, oy, oz); it looks north
    (-z), or south with naar_zuid. kleur(deel, (xm, ym, zm), blok) -> another block name (None leaves the cell out), deel
    from guh_deel and the point in model pixels: a painter can dress the guh. Returns ((nx, ny, nz), naar_wereld), where
    naar_wereld(xm, ym, zm) gives the template cell of a model point (to put a crown on its head)."""
    import json
    import os
    ms = b.h.ms
    botten = tuple(botten or GUH_BOTTEN)
    geo = json.load(open(os.path.join(b.h.A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
    kubussen = [c for bot in geo["bones"] if bot["name"] in botten for c in bot.get("cubes", [])]
    lo = [min(c["origin"][i] for c in kubussen) for i in range(3)]
    s = ms.Structure((200, 140, 200))
    nx, ny, nz = ms.voxel_guh(s, 0, 0, 0, scale=schaal, face_south=naar_zuid, bones=botten)

    def model(x, y, z):
        bx, bz = (x, nz - 1 - z) if naar_zuid else (nx - 1 - x, z)
        return lo[0] + (bx + 0.5) / schaal, lo[1] + (y + 0.5) / schaal, lo[2] + (bz + 0.5) / schaal

    def naar_wereld(xm, ym, zm):
        bx, by, bz = (xm - lo[0]) * schaal, (ym - lo[1]) * schaal, (zm - lo[2]) * schaal
        x, z = (bx, nz - bz) if naar_zuid else (nx - bx, bz)
        return int(math.floor(ox + x)), int(math.floor(oy + by)), int(math.floor(oz + z))

    for (x, y, z), (naam, _p, _n) in sorted(s.blocks.items()):
        if kleur:
            pm = model(x, y, z)
            naam = kleur(guh_deel(*pm), pm, naam)
            if not naam:
                continue
        b.set(ox + x, oy + y, oz + z, naam)
    return (nx, ny, nz), naar_wereld


def op_grond(blok, gronden, props=None):
    """A light for verlicht(): like op_vloer, but only on these ground blocks (a glowing mushroom on blue cheese, not on a
    wooden floor)."""
    basis = op_vloer(blok, props)

    def kies(b, c):
        onder = b.s.blocks.get((c[0], c[1] - 1, c[2]))
        return basis(b, c) if onder is not None and onder[0] in gronden else None
    return kies


def zadeldak(b, x0, z0, x1, z1, y, as_, trapblok, vulblok, top=None, plat=None, oversteek=1):
    """A gable roof over the box (x0..x1, z0..z1), its ridge along `as_` ("x" or "z"), starting at height y on the
    overhang. The gable ends are filled with vulblok. top = how many rows it may rise before it is closed flat with
    `plat` (a slab block): a low, wide roof for a big hall. Returns the height of the ridge."""
    lo, hi = (z0 - oversteek, z1 + oversteek) if as_ == "x" else (x0 - oversteek, x1 + oversteek)
    a0, a1 = (x0 - oversteek, x1 + oversteek) if as_ == "x" else (z0 - oversteek, z1 + oversteek)
    g0, g1 = (x0, x1) if as_ == "x" else (z0, z1)
    k = 0
    while lo <= hi:
        yy = y + k
        klaar = top is not None and k >= top
        for a in range(a0, a1 + 1):
            if klaar or lo == hi:
                for c in range(lo, hi + 1):
                    x, z = (a, c) if as_ == "x" else (c, a)
                    if klaar:
                        b.set(x, yy, z, plat or vulblok, {"type": "bottom", "waterlogged": "false"} if plat else None)
                    else:
                        b.set(x, yy, z, vulblok)
            else:
                for c, f in ((lo, "south" if as_ == "x" else "east"), (hi, "north" if as_ == "x" else "west")):
                    x, z = (a, c) if as_ == "x" else (c, a)
                    trap(b, x, yy, z, trapblok, f)
        if klaar or lo == hi:
            return yy
        for a in (g0, g1):                                   # the gable ends, under the stairs of this row
            for c in range(lo + 1, hi):
                x, z = (a, c) if as_ == "x" else (c, a)
                if k and b.get(x, yy, z) is None:
                    b.set(x, yy, z, vulblok)
        lo, hi, k = lo + 1, hi - 1, k + 1
    return y + k - 1


# =====================================================================================================================
# a vetschip (the flying grease ships of the Mika's), any size, any heading
# =====================================================================================================================
SCHIP_ROMP = [("orange_terracotta", 40), ("yellow_terracotta", 30), ("brown_terracotta", 30)]
GUH_ZEIL = ["ppppppp",
            "pKpppKp",
            "pKpppKp",
            "ppphppp",
            "pHpppHp",
            "ppppppp"]


class Schip:
    """A vetschip in the making: local coordinates (u = across, 0 on the keel line; v = along, 0 at the stern, growing to
    the bow; y from the deck: 0 = the deck planks). boeg = the way the bow points. Everything is written through zet()."""

    def __init__(self, b, x, y, z, boeg, lengte=34, breedte=5, naam="schip"):
        self.b, self.x, self.y, self.z, self.boeg, self.L, self.B = b, x, y, z, boeg, lengte, breedte
        self.rng = random.Random(kit.seed("schip/" + naam))
        self.romp_cellen = set()

    def w(self, u, v):
        """Local (u, v) -> world (x, z). The bow points to `boeg`; u grows to starboard (the right hand)."""
        fx, fz = DIRS[self.boeg]
        rx, rz = DIRS[RECHTS[self.boeg]]
        return self.x + fx * v + rx * u, self.z + fz * v + rz * u

    def zet(self, u, y, v, blok, props=None, nbt=None):
        x, z = self.w(u, v)
        self.b.set(x, self.y + y, z, blok, props, nbt)

    def richting(self, lokaal):
        """A facing in ship terms ("boeg", "hek", "stuurboord", "bakboord") -> a world facing."""
        return {"boeg": self.boeg, "hek": OPP[self.boeg], "stuurboord": RECHTS[self.boeg], "bakboord": LINKS[self.boeg]}[lokaal]

    def half(self, v):
        """Half the beam at v (None: outside the hull)."""
        L, B = self.L, self.B
        if v < 0 or v > L:
            return None
        if v <= 5:
            return B - (5 - v) * 0.35
        if v <= L - 10:
            return float(B)
        return max(0.6, B * (L - v) / 10.0)

    def romp(self, diep=6):
        hull, L = set(), self.L
        for v in range(0, L + 1):
            hw = self.half(v)
            for u in range(-self.B - 1, self.B + 2):
                if abs(u) > hw + 0.3:
                    continue
                bodem = -diep + max(0, abs(u) - 2) + max(0, 4 - v) // 2 + max(0, v - (L - 9)) // 2
                for y in range(min(bodem, -1), 0):
                    hull.add((u, y, v))
        for (u, y, v) in sorted(hull):
            # (the shell: also a cell that only touches the outside by an edge, so the planking has no gaps)
            buiten = any((u + du, y + dy, v + dv) not in hull for du in (-1, 0, 1) for dy in (-1, 0) for dv in (-1, 0, 1))
            if not buiten:
                continue                                     # (the hold is hollow)
            if y == -1:
                self.zet(u, y, v, "honey_block")
            elif abs(u) <= 1 and (u, y - 1, v) not in hull:
                self.zet(u, y, v, "honeycomb_block")
            else:
                self.zet(u, y, v, gb.pick(self.rng, SCHIP_ROMP))
        self.romp_cellen = hull
        return hull

    def dek(self, gat=None, reling_open=()):
        """The deck planks and the bulwark; reling_open = [(side, v0, v1)]: where the gangway comes aboard (side -1 port, 1 starboard)."""
        cellen = []
        for v in range(0, self.L + 1):
            hw = self.half(v)
            for u in range(-self.B - 1, self.B + 2):
                if abs(u) > hw + 0.3:
                    continue
                rand = abs(u) > hw - 0.7 or v in (0, self.L)
                self.zet(u, 0, v, "brown_terracotta" if rand else ("yellow_terracotta" if u % 2 else "orange_terracotta"))
                if rand:
                    if not any((u < 0) == (side < 0) and v0 <= v <= v1 for side, v0, v1 in reling_open):
                        self.zet(u, 1, v, "honeycomb_block" if v % 5 else "brown_terracotta")
                else:
                    cellen.append((u, v))
        return cellen

    def mast(self, v, hoogte, zeil_b, zeil_h, rijen=None, palet=None):
        """A mast with a yard and a square sail across the ship; rijen = the picture (rows of letters), palet its blocks."""
        for y in range(1, hoogte + 1):
            self.zet(0, y, v, "stripped_dark_oak_log", {"axis": "y"})
        as_ = "x" if DIRS[self.boeg][0] == 0 else "z"
        for u in range(-zeil_b // 2 - 1, zeil_b // 2 + 2):
            if u:
                self.zet(u, hoogte - 1, v, "stripped_dark_oak_log", {"axis": as_})
        rijen = rijen or [("b" if j % 3 else "o") * zeil_b for j in range(zeil_h)]
        palet = palet or {}
        pal = {"b": "brown_wool", "o": "orange_wool", "p": "pink_wool", "K": "black_wool", "R": "red_wool", "W": "white_wool",
               "h": "magenta_wool", "H": "pink_concrete"}
        pal.update(palet)
        for j, rij in enumerate(rijen):
            for i, ch in enumerate(rij):
                u = i - zeil_b // 2
                if u:
                    self.zet(u, hoogte - 2 - j, v, pal[ch])

    def boegbeeld(self, guh=False):
        """A head at the bow: a Mika's (pink, angry) or a guh's (pink, sweet, with ears)."""
        L = self.L
        for u in (-1, 0, 1):
            for y in (-1, 0, 1):
                for v in (L + 1, L + 2, L + 3):
                    self.zet(u, y, v, "pink_wool")
        ogen = "black_wool"
        self.zet(-1, 1, L + 3, ogen)
        self.zet(1, 1, L + 3, ogen)
        if guh:
            self.zet(0, 0, L + 3, "magenta_wool")           # (the little nose)
            self.zet(-1, 2, L + 1, "pink_wool")
            self.zet(1, 2, L + 1, "pink_wool")
        else:
            self.zet(-1, 0, L + 3, "red_wool")
            self.zet(1, 0, L + 3, "red_wool")
            self.zet(0, -1, L + 3, "white_wool")

    def doos(self, hoog=16, diep=7):
        """The box of the whole ship in the world (two corners), for a zone."""
        pts = [self.w(u, v) for u in (-self.B - 1, self.B + 1) for v in (0, self.L + 3)]
        xs, zs = [p[0] for p in pts], [p[1] for p in pts]
        return min(xs), self.y - diep, min(zs), max(xs), self.y + hoog, max(zs)


def guh_zeil(b_, h_):
    """The sweet guh face (two eyes, a nose, two blushes) centred on a sail of b_ x h_."""
    rows = []
    fy, fx = (h_ - len(GUH_ZEIL)) // 2, (b_ - len(GUH_ZEIL[0])) // 2
    for j in range(h_):
        row = ""
        for i in range(b_):
            if 0 <= j - fy < len(GUH_ZEIL) and 0 <= i - fx < len(GUH_ZEIL[0]):
                row += GUH_ZEIL[j - fy][i - fx]
            else:
                row += "p"
        rows.append(row)
    return rows
