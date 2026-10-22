"""
Het Kriebelhuis (round 4, L2; design chapter 6 rounds 50-56, safari-ontwerp/lobby2/ontwerp.md + _bron/lobby2_v2.py).

One big building on the north side of the lobby square, one route through clearly separated sections:
  entreehal (gate with the magnifying-glass guh) -> 1 Terrarium (glass kas dome) -> 2 Kriebelzaal (winding path, small
  cages, ant farm, vlindertuin; leaf roofs + a big ladybird) -> 3 Nachtzaal (dark, blue light, star dome) -> 4a Diepzeezaal
  (closed indoor aquarium) -> stairs down -> 4b pink glass tube under the pond (rif-lagune + kelpbos, viewing cave) ->
  stairs up -> exit + gift shop -> 4c aai-lagune (boardwalk with a glass floor) -> the square.

Coordinates are lobby2's local scene coordinates (240 x 224, ground at G = 16), placed in the world with park.lobby
(set by zone_entree, L1): {"oorsprong": [wx, wy, wz], "g": 16, "draai": 0}: local (x, y, z) -> world (wx + x, wy + (y - g),
wz + z), draai = clockwise quarter turns. Without park.lobby a fallback origin is used (logged).

The homes of the Kriebelhuis species go into plekken.json (zone "entree", "afdeling" = the section); the existing guhs
critters (koi, knabbelkoi, guh_vis, kruimelkwal, guhxolotl, kikkerguh, vlinders, lieveheersbeestje, guh_bee) are not
safari species: their spots go into data/guhs/safari/kriebelhuis.json ("critters") for the spawner (L4), together with
the sections and the infobord list.
"""
import json
import math
import os
import random
from collections import deque

import numpy as np

from . import kit
from .terrein import ZONES

G = 16
X, Z = 240, 224
WATER = "minecraft:water[level=0]"
BED = "minecraft:sand"
LOG = "minecraft:stripped_spruce_log[axis=y]"
HAY = "minecraft:hay_block[axis=y]"
PINKG = "minecraft:pink_stained_glass"
TGLAS = "guhs:terrariumglas"
KASGLAS = "guhs:kasglas"
GLASVLOER = "guhs:glasvloer"
LANT = "guhs:park_lantaarn"
HEG = "minecraft:azalea_leaves[distance=1,persistent=true,waterlogged=false]"
HEGB = "minecraft:flowering_azalea_leaves[distance=1,persistent=true,waterlogged=false]"
MOS = "minecraft:moss_block"
FRAME = "minecraft:stripped_spruce_log[axis=y]"
KORAAL = ("roze", "geel", "blauw", "oranje", "paars")

# the sections (local x1, z1, x2, z2), also the plot L1 leaves free
HAL = (110, 70, 134, 92)
TERRARIUM = (134, 50, 176, 92)
KRIEBELZAAL = (128, 4, 182, 50)
NACHTZAAL = (86, 4, 128, 46)
DIEPZEE = (84, 48, 110, 70)
UITGANG = (60, 88, 86, 108)
AAI = (24, 92, 56, 112)
AQUARIUM_BUITEN = (2, 20, 82, 88)
MIDX = 122
TUBE = [(76, 59), (66, 57), (54, 50), (40, 44), (26, 46), (16, 56), (20, 68), (34, 76), (50, 80), (64, 80), (70, 84)]
PCX, PCZ, PRX, PRZ = 42, 54, 37, 29          # the pond (L1 digs it; we deepen it around the tube if needed)

_DRAAI_F = ["north", "east", "south", "west"]
# round 5 (test round 5): adults, young per water species; at least 3 of each, the small fish at least 8
AANTAL_VIS = {"kwal": (4, 0), "lantaarnvisje": (8, 0), "dumbo_octopus": (2, 1), "kogelvis": (2, 1), "babyhaai": (3, 0),
              "clownvis": (3, 1), "anemoon": (3, 0), "regenboogvis": (8, 0), "zeeschildpad": (2, 1), "zeepaardje": (3, 1),
              "pijlstaartrog": (2, 1), "zeester": (3, 0), "zee_egel": (3, 0)}


def L_K0(S):
    return S.k == 0


def _richting(S, dx, dz):
    """A local (dx, dz) direction in the world (draai)."""
    return [(dx, dz), (-dz, dx), (-dx, -dz), (dz, -dx)][S.k]


class Lokaal:
    """lobby2 local coordinates on top of the park volume."""

    def __init__(self, park, oorsprong, g=G, draai=0):
        self.park = park
        self.ox, self.wy, self.oz = (int(c) for c in oorsprong)
        self.g = int(g)
        self.k = int(draai) % 4

    def w(self, x, y, z):
        x, z = int(math.floor(x)), int(math.floor(z))
        a, b = [(x, z), (-z, x), (-x, -z), (z, -x)][self.k]
        return self.ox + a, self.wy + (int(math.floor(y)) - self.g), self.oz + b

    def wf(self, x, y, z):
        """Float world position (entity / eye spots)."""
        a, b = [(x, z), (-z, x), (-x, -z), (z, -x)][self.k]
        return [round(self.ox + a, 2), round(self.wy + (y - self.g), 2), round(self.oz + b, 2)]

    def draai_state(self, s):
        if self.k == 0 or "[" not in s:
            return s
        naam, props = kit.split_state(s)
        if "facing" in props and props["facing"] in _DRAAI_F:
            props["facing"] = _DRAAI_F[(_DRAAI_F.index(props["facing"]) + self.k) % 4]
        if self.k % 2 and props.get("axis") in ("x", "z"):
            props["axis"] = "z" if props["axis"] == "x" else "x"
        return naam + "[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]"

    def mag(self, x, z):
        """L1's plot rule: nothing at local x >= 183 (the truck road), nothing at z >= 97 except the aai-lagune, the
        exit + shop and the strip between them."""
        if x >= 183 or x < 0 or z < 0:
            return False
        if z < 97:
            return True
        return z <= 112 and (AAI[0] - 1 <= x <= UITGANG[2])

    def set(self, x, y, z, s):
        if not self.mag(math.floor(x), math.floor(z)):
            return
        self.park.set(*self.w(x, y, z), self.draai_state(s))

    def get(self, x, y, z):
        return self.park.get(*self.w(x, y, z))

    def leeg(self, x, y, z):
        return self.park.leeg(*self.w(x, y, z))

    def fill(self, x1, y1, z1, x2, y2, z2, s):
        hoeken = [(a, b) for a in (x1, x2) for b in (z1, z2)]
        if self.k == 0 and all(self.mag(a, b) for a, b in hoeken):
            a, b, c = self.w(min(x1, x2), min(y1, y2), min(z1, z2))
            d, e, f = self.w(max(x1, x2), max(y1, y2), max(z1, z2))
            self.park.fill(a, b, c, d, e, f, s)
            return
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for y in range(min(y1, y2), max(y1, y2) + 1):
                    self.set(x, y, z, s)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, s):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
                for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
                    if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1.0:
                        self.set(x, y, z, s)

    def walls(self, x1, y1, z1, x2, y2, z2, s):
        self.fill(x1, y1, z1, x2, y2, z1, s)
        self.fill(x1, y1, z2, x2, y2, z2, s)
        self.fill(x1, y1, z1, x1, y2, z2, s)
        self.fill(x2, y1, z1, x2, y2, z2, s)


class Bouwer:
    def __init__(self, park, L, log):
        self.park, self.S, self.log = park, L, log
        self.rnd = random.Random(kit.SEED + 52)
        self.doelen = []        # (naam, (x, y, z)) local stand spots: the reachability check
        self.kijk = []          # (naam, glass centre (x, y, z), stand spot (x, y, z)): the sight check
        self.huizen = []        # plekken.json homes (local centre)
        self.critters = []      # guhs critters (local pos)
        self.borden = []        # infobord (local pos, soort)
        self.paden = set()      # local (x, z) walkway cells (decor never lands there)
        self.startpunt = (MIDX, G + 1, 100)

    # --- small helpers (lobby2_v2.py) --------------------------------------------------------------------------------
    def doel(self, n, x, y, z):
        self.doelen.append((n, (int(x), int(y), int(z))))

    def door2(self, x, y, z, facing, mat="guhs:roze_lakhout_deur"):
        self.S.set(x, y, z, f"{mat}[facing={facing},half=lower,hinge=left,open=false,powered=false]")
        self.S.set(x, y + 1, z, f"{mat}[facing={facing},half=upper,hinge=left,open=false,powered=false]")

    def lantaarnpaal(self, x, z, y=G):
        self.S.fill(x, y + 1, z, x, y + 2, z, "minecraft:dark_oak_fence")
        self.S.set(x, y + 3, z, LANT)

    def infobord(self, x, y, z, facing, soort):
        self.S.set(x, y, z, f"guhs:infobord[facing={facing}]")
        self.borden.append(((x, y, z), soort))

    def warmtelamp(self, x, y, z):
        self.S.set(x, y, z, "guhs:warmtelamp")

    def box(self, x1, y1, z1, x2, y2, z2, wall, floor=None, corner=LOG):
        S = self.S
        S.walls(x1, y1, z1, x2, y2, z2, wall)
        for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
            S.fill(x, y1, z, x, y2, z, corner)
        if floor:
            S.fill(x1, y1 - 1, z1, x2, y1 - 1, z2, floor)
        S.air(x1 + 1, y1, z1 + 1, x2 - 1, y2, z2 - 1)

    def thatch(self, x1, z1, x2, z2, y, overhang=1):
        a, b, c, d = x1 - overhang, x2 + overhang, z1 - overhang, z2 + overhang
        lvl = 0
        while a <= b and c <= d:
            for x in range(a, b + 1):
                for z in range(c, d + 1):
                    if x in (a, b) or z in (c, d) or a + 1 >= b or c + 1 >= d:
                        self.S.set(x, y + lvl, z, HAY)
            a, b, c, d = a + 1, b - 1, c + 1, d - 1
            lvl += 1
        return y + lvl

    def windows(self, x1, y, z1, x2, z2, every=3, pane="minecraft:glass", skip=()):
        for x in range(x1 + 2, x2 - 1, every):
            for z in (z1, z2):
                if (x, z) not in skip:
                    self.S.fill(x, y, z, x, y + 1, z, pane)
        for z in range(z1 + 2, z2 - 1, every):
            for x in (x1, x2):
                if (x, z) not in skip:
                    self.S.fill(x, y, z, x, y + 1, z, pane)

    def pad(self, x1, z1, x2, z2, pal=("guhs:knuffelklinkers", "minecraft:smooth_sandstone"), y=G, hoog=5):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                self.S.set(x, y, z, self.rnd.choice(pal))
                self.S.air(x, y + 1, z, x, y + hoog, z)
                self.paden.add((x, z))

    def pad_lijn(self, pts, w=3, pal=("guhs:knuffelklinkers", "minecraft:smooth_sandstone")):
        for (ax, az), (bx, bz) in zip(pts, pts[1:]):
            n = int(max(abs(bx - ax), abs(bz - az)))
            for i in range(n + 1):
                t = i / max(n, 1)
                x, z = round(ax + (bx - ax) * t), round(az + (bz - az) * t)
                self.pad(x - w // 2, z - w // 2, x + (w - 1) // 2, z + (w - 1) // 2, pal)

    def huis(self, soort, afdeling, x, y, z, volw=1, jong=0, straal=2, water=False, boom=False):
        self.huizen.append(dict(soort=soort, afdeling=afdeling, pos=(x, y, z), volwassenen=volw, jongen=jong,
                                straal=straal, water=water, boom=boom))

    def critter(self, entity, variant, afdeling, x, y, z, straal=3, water=False):
        self.critters.append(dict(entity=entity, variant=variant, afdeling=afdeling, pos=(x, y, z), straal=straal,
                                  water=water))

    def terrarium(self, x1, z1, w, d, h, front, bodem, soort, yb=G + 1, dak=True):
        """Glass case (guhs:terrariumglas, log corners) on a 1 high plinth; front = the path side (n/s/e/w).
        Registers the sight check (front glass centre) and the stand spot (2 in front), and an infobord beside it.
        Returns the inside floor box (x1, y, z1, x2, z2)."""
        S = self.S
        x2, z2 = x1 + w - 1, z1 + d - 1
        S.fill(x1, yb, z1, x2, yb, z2, "minecraft:stripped_spruce_wood[axis=y]")
        S.fill(x1 + 1, yb, z1 + 1, x2 - 1, yb, z2 - 1, bodem)
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                if x in (x1, x2) or z in (z1, z2):
                    hoek = x in (x1, x2) and z in (z1, z2)
                    S.fill(x, yb + 1, z, x, yb + h, z, FRAME if hoek else TGLAS)
                else:
                    S.air(x, yb + 1, z, x, yb + h, z)
        if dak:
            S.fill(x1, yb + h + 1, z1, x2, yb + h + 1, z2, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
            S.fill(x1 + 1, yb + h + 1, z1 + 1, x2 - 1, yb + h + 1, z2 - 1, TGLAS)
        cx, cz = (x1 + x2) / 2, (z1 + z2) / 2
        if front == "s":
            g, st, ib, f = (cx, yb + 1 + h / 2, z2), (int(cx), yb, z2 + 2), (x1 - 1 if w > 4 else x2 + 1, yb, z2 + 1), "south"
        elif front == "n":
            g, st, ib, f = (cx, yb + 1 + h / 2, z1), (int(cx), yb, z1 - 2), (x1 - 1 if w > 4 else x2 + 1, yb, z1 - 1), "north"
        elif front == "e":
            g, st, ib, f = (x2, yb + 1 + h / 2, cz), (x2 + 2, yb, int(cz)), (x2 + 1, yb, z1 - 1 if d > 4 else z2 + 1), "east"
        else:
            g, st, ib, f = (x1, yb + 1 + h / 2, cz), (x1 - 2, yb, int(cz)), (x1 - 1, yb, z1 - 1 if d > 4 else z2 + 1), "west"
        if soort:
            self.infobord(ib[0], ib[1], ib[2], f, soort)
        self.kijk.append((soort or "terrarium", g, st))
        self.doelen.append((f"bij {soort}", st))
        return (x1 + 1, yb + 1, z1 + 1, x2 - 1, z2 - 1)

    def decor_binnen(self, x1, y, z1, x2, z2, stijl):
        """Real terrarium dressing inside a case: stones, branches, ferns, moss, bark plates, a water dish."""
        S = self.S
        w = x2 - x1
        if stijl == "woestijn":
            S.set(x1, y, z1, "minecraft:cobblestone")
            S.set(x2, y, z2, "minecraft:dead_bush")
            S.set(x1 + w // 2, y, z1, "minecraft:sandstone_slab[type=bottom,waterlogged=false]")
            S.set(x2, y, z1, "minecraft:dead_bush")
            S.set(x1 + 1, y, z2, "guhs:schorsplaat")
            S.set(x1, y, z2, "minecraft:smooth_sandstone_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
        elif stijl == "jungle":
            S.set(x1, y, z1, "minecraft:mossy_cobblestone")
            S.set(x2, y, z1, "minecraft:fern")
            S.set(x1, y, z2, "minecraft:large_fern[half=lower]")
            S.set(x1, y + 1, z2, "minecraft:large_fern[half=upper]")
            S.set(x1 + w // 2, y, z1, "minecraft:water_cauldron[level=3]")
            S.fill(x2, y, z2, x2, y + 2, z2, "minecraft:stripped_jungle_log[axis=y]")
            S.set(x2 - 1, y + 2, z2, "minecraft:stripped_jungle_log[axis=x]")
            S.set(x1 + 1, y, z2, "guhs:schorsplaat")
            S.set(x2 - 1, y, z1 + 1, "minecraft:moss_carpet")
        else:
            S.set(x1, y, z1, "minecraft:mossy_cobblestone")
            S.set(x2, y, z2, "minecraft:fern")
            S.set(x1, y, z2, "minecraft:stripped_oak_wood[axis=x]")
            S.set(x2, y, z1, "minecraft:moss_carpet")
            S.set(x1 + 1, y, z1, "guhs:schorsplaat")

    # ==================================================================================================================
    def entreehal(self):
        S = self.S
        HX1, HZ1, HX2, HZ2 = HAL
        S.fill(HX1 - 1, G - 2, HZ1 - 1, HX2 + 1, G - 1, HZ2 + 6, "minecraft:dirt")
        self.box(HX1, G + 1, HZ1, HX2, G + 9, HZ2, "minecraft:white_terracotta", floor="minecraft:birch_planks")
        for x in range(HX1 + 2, HX2 - 1, 5):                                            # windows (beside the gate)
            if abs(x + 1 - MIDX) > 4:
                S.fill(x, G + 4, HZ2, x + 2, G + 7, HZ2, "minecraft:glass")
        S.air(MIDX - 2, G + 1, HZ2, MIDX + 2, G + 6, HZ2)                                # the wide entrance
        S.fill(HX1 + 1, G + 10, HZ1 + 1, HX2 - 1, G + 10, HZ2 - 1, "minecraft:birch_planks")     # ceiling (the lamps hang on it)
        self.thatch(HX1, HZ1, HX2, HZ2, G + 10, overhang=1)
        for x in range(HX1 + 3, HX2 - 2, 5):
            S.set(x, G + 9, 81, "guhs:guh_kristal_lamp")
        S.fill(114, G + 1, 74, 124, G + 2, 75, "minecraft:spruce_planks")                 # ticket desk
        S.fill(114, G + 3, 75, 124, G + 3, 75, "minecraft:birch_slab[type=bottom,waterlogged=false]")
        for x in (115, 119, 123):
            S.set(x, G + 3, 74, "guhs:klusjes_guhlampje[facing=south,lit=true]")
        self.npc_balie = (119.5, G + 1, 77.5, 180.0)
        S.fill(112, G + 2, HZ1 + 1, 132, G + 7, HZ1 + 1, "minecraft:lime_terracotta")      # mural with a ladybird
        S.fill(118, G + 3, HZ1 + 1, 122, G + 6, HZ1 + 1, "minecraft:red_terracotta")
        S.set(119, G + 5, HZ1 + 1, "minecraft:black_terracotta")
        S.set(121, G + 4, HZ1 + 1, "minecraft:black_terracotta")
        S.fill(120, G + 3, HZ1 + 1, 120, G + 6, HZ1 + 1, "minecraft:black_terracotta")
        for x in (114, 126, 129):
            S.set(x, G + 4, HZ1 + 1, "minecraft:yellow_terracotta")                      # little flowers
            S.set(x, G + 3, HZ1 + 1, "minecraft:green_terracotta")
        for z in range(74, 91, 4):
            S.set(HX1 + 1, G + 1, z, "guhs:guh_bloempot")
            S.set(HX2 - 1, G + 1, z, "guhs:guh_bloempot")
        S.fill(HX1 + 2, G, 84, HX1 + 6, G, 88, "minecraft:pink_wool")                    # a carpet-island with benches
        S.set(HX1 + 2, G + 1, 86, "guhs:park_bankje[facing=east]")
        S.set(HX2 - 3, G + 1, 86, "guhs:park_bankje[facing=west]")
        # big map board of the house (the route)
        S.fill(HX2 - 6, G + 2, HZ1 + 1, HX2 - 2, G + 5, HZ1 + 1, "minecraft:yellow_concrete")
        for i, k in enumerate(("minecraft:light_blue_concrete", "minecraft:lime_concrete", "minecraft:blue_concrete",
                               "minecraft:pink_concrete")):
            S.set(HX2 - 6 + i + (1 if i else 0) - (1 if i == 3 else 0), G + 3 + (i % 2), HZ1 + 1, k)
        self.pad(MIDX - 3, HZ2 + 1, MIDX + 3, 96)                                       # gate (L1: on to the square)
        self.doel("kriebelhuis_entreehal", MIDX, G + 1, 84)
        self.hal_licht(HX1, HZ1, HX2, HZ2)
        # the gate: two pillars, an arch, the sign, a guh with a big magnifying glass on top
        for x in (MIDX - 6, MIDX + 6):
            S.fill(x, G + 1, 95, x + 1, G + 9, 96, "minecraft:spruce_planks")
            S.set(x, G + 10, 95, "minecraft:pink_wool")
            S.set(x + 1, G + 10, 96, "minecraft:pink_wool")
            S.set(x, G + 1, 97, LANT)
        S.fill(MIDX - 6, G + 9, 95, MIDX + 7, G + 9, 96, "minecraft:spruce_planks")
        S.fill(MIDX - 4, G + 7, 95, MIDX + 5, G + 8, 95, "minecraft:lime_concrete")
        S.fill(MIDX - 3, G + 7, 95, MIDX + 4, G + 7, 95, "minecraft:magenta_concrete")
        S.set(MIDX, G + 8, 94, "minecraft:oak_wall_sign[facing=south,waterlogged=false]")
        bx, by, bz = S.w(MIDX, G + 8, 94)
        self.park.bord_nbt(bx, by, bz, ["", "Het", "Kriebelhuis", ""])
        S.ellipsoid(MIDX - 2, G + 12, 95.5, 2.6, 2.4, 2.2, "minecraft:pink_wool")
        for side in (-1, 1):
            S.fill(MIDX - 2 + side * 2, G + 14, 95, MIDX - 2 + side * 3, G + 16, 96, "minecraft:pink_wool")
        S.set(MIDX - 3, G + 12, 97, "minecraft:black_concrete")
        S.set(MIDX - 1, G + 12, 97, "minecraft:black_concrete")
        for a in range(0, 360, 12):
            rx, ry = MIDX + 4 + 3.2 * math.cos(math.radians(a)), G + 13 + 3.2 * math.sin(math.radians(a))
            S.set(int(round(rx)), int(round(ry)), 96, "minecraft:gold_block")
        for xx in range(MIDX + 2, MIDX + 7):
            for yy in range(G + 11, G + 16):
                if math.hypot(xx - (MIDX + 4), yy - (G + 13)) < 2.5:
                    S.set(xx, yy, 96, "minecraft:light_blue_stained_glass")
        for k in range(4):
            S.set(MIDX + 1 - k, G + 10 - k + 1, 96, "minecraft:stripped_dark_oak_wood[axis=y]")

    def lampions(self, x1, z1, x2, z2, y, stap=4, vrij=()):
        """Hanging lampions (yellow, pink, mint) in a grid under a ceiling at y + 1."""
        kleuren = ("geel", "roze", "mint")
        for x in range(x1, x2 + 1, stap):
            for z in range(z1, z2 + 1, stap):
                if (x, z) in vrij or not self.S.leeg(x, y, z) or self.S.leeg(x, y + 1, z):
                    continue
                self.S.set(x, y, z, f"guhs:lampion_{kleuren[(x // stap + z // stap) % 3]}[hanging=true]")

    def hal_licht(self, HX1, HZ1, HX2, HZ2):
        """Test round 5: the entreehal was too empty and dark: lampions under the ceiling, glow tiles in a paw trail
        to the terrarium door, stone guh lanterns, and a cheerful corner with plushies and cushions."""
        S = self.S
        self.lampions(HX1 + 2, HZ1 + 2, HX2 - 2, HZ2 - 2, G + 9)
        for z in range(HZ1 + 3, HZ2 - 1, 4):                                          # glow tiles along the walk
            for x in (MIDX - 4, MIDX + 4):
                if S.get(x, G, z).startswith("minecraft:birch_planks"):
                    S.set(x, G, z, "guhs:glimtegel[fel=true]")
        for x in range(MIDX + 2, HX2, 3):                                             # a paw trail to the terrarium door
            S.set(x, G, 81, "guhs:tegel_pootjes")
        for (x, z) in ((HX1 + 1, HZ1 + 1), (HX2 - 1, HZ1 + 1), (HX1 + 1, HZ2 - 1), (HX2 - 1, HZ2 - 1)):
            S.set(x, G + 1, z, "guhs:toro[lit=true]")
        for (x, z, f) in ((HX1 + 2, 90, "east"), (HX1 + 3, 90, "east"), (HX2 - 3, 90, "west"), (HX2 - 2, 90, "west")):
            S.set(x, G + 1, z, f"guhs:pink_kussen[facing={f}]")
        for (x, k) in ((116, "knuffel_normal"), (119, "knuffel_mint"), (122, "knuffel_wolk")):
            S.set(x, G + 3, 75, f"guhs:{k}[facing=south]")
        S.set(HX1 + 4, G + 1, 88, "guhs:wolkenlamp")
        S.set(HX2 - 4, G + 1, 88, "guhs:wolkenlamp")

    # ==================================================================================================================
    def terrarium_zaal(self):
        S = self.S
        TX1, TZ1, TX2, TZ2 = TERRARIUM
        HX2 = HAL[2]
        S.fill(TX1, G - 2, TZ1, TX2, G - 1, TZ2, "minecraft:dirt")
        self.box(TX1, G + 1, TZ1, TX2, G + 8, TZ2, "minecraft:terracotta", floor="minecraft:smooth_sandstone")
        S.air(HX2, G + 1, 80, HX2, G + 3, 82)
        S.fill(HX2, G + 1, 80, HX2, G + 3, 82, "minecraft:white_terracotta")             # (round 5: no open blocks
        S.air(HX2, G + 1, 81, HX2, G + 2, 81)                                            # beside the door)
        self.door2(HX2, G + 1, 81, "east", "guhs:guhwaii_palm_deur")
        S.set(HX2 + 1, G + 1, 79, "guhs:toro[lit=true]")
        S.set(HX2 + 1, G + 1, 83, "guhs:toro[lit=true]")
        self.sectiebord(HX2 - 1, G + 3, 79, "west", ["", "1", "Terrarium", ""])
        for x in range(TX1 + 1, TX2):
            for z in range(64, 79):
                S.set(x, G, z, "minecraft:smooth_sandstone" if (x + z) % 4 else "minecraft:sandstone")
                self.paden.add((x, z))
        # (round 5: the north row leaves a 3 wide aisle at x 153..155 to the Kriebelzaal door, visible from the middle)
        T_N = [(136, "kameleon", "jungle", 1, 1), (145, "reuzenschildpad", "woestijn", 1, 1),
               (156, "slang", "jungle", 2, 0), (165, "hoornkikker", "jungle", 2, 0)]
        T_S = [(137, "kraaghagedis", "woestijn", 2, 0), (146, "gifkikker", "jungle", 3, 0),
               (155, "baardagaam", "woestijn", 1, 1), (164, None, "water", 0, 0)]
        for (x1, soort, stijl, v, j) in T_N:
            bodem = "minecraft:sand" if stijl == "woestijn" else MOS
            ix1, iy, iz1, ix2, iz2 = self.terrarium(x1, 53, 8, 8, 4, "s", bodem, soort)
            self.decor_binnen(ix1, iy, iz1, ix2, iz2, stijl)
            self.warmtelamp(x1 + 4, G + 5, 57)
            self.huis(soort, "terrarium", x1 + 4, iy, 56, v, j, straal=2)
        for (x1, soort, stijl, v, j) in T_S:
            bodem = "minecraft:sand" if stijl == "woestijn" else MOS
            ix1, iy, iz1, ix2, iz2 = self.terrarium(x1, 82, 8, 8, 4, "n", bodem, soort or "guhxolotl")
            if stijl == "water":                                                       # guhxolotl aquarium
                S.fill(ix1, iy - 1, iz1, ix2, iy - 1, iz2, "minecraft:sand")
                S.fill(ix1, iy, iz1, ix2, iy + 2, iz2, WATER)
                S.set(ix1, iy - 1, iz1, "minecraft:sea_lantern")
                S.set(ix2, iy, iz2, "guhs:nepkoraal[kleur=roze,waterlogged=true]")
                S.set(ix1 + 2, iy, iz2, "minecraft:seagrass")
                for v in ("guhxolotl_roze", "guhxolotl_roze"):
                    self.critter("guhs:guhxolotl", v, "terrarium", x1 + 4, iy + 0.5, 85.5, straal=2, water=True)
            else:
                self.decor_binnen(ix1, iy, iz1, ix2, iz2, stijl)
                self.huis(soort, "terrarium", x1 + 4, iy, 85, v, j, straal=2)
            self.warmtelamp(x1 + 4, G + 5, 86)
        S.set(158, G + 2, 86, "minecraft:magma_block")                                   # warm stone for the baardagaam
        # kikkerhoek: an open pond in the middle of the aisle with a path around it
        for x in range(147, 165):
            for z in range(67, 76):
                if math.hypot((x - 156) / 7.5, (z - 71.5) / 3.6) < 1:
                    S.set(x, G, z, WATER)
                    S.set(x, G - 1, z, BED)
                    S.set(x, G + 1, z, "minecraft:lily_pad" if (x * 3 + z) % 5 == 0 else "minecraft:air")
                    self.paden.discard((x, z))
        for (x, z, b) in ((146, 70, "minecraft:mossy_cobblestone"), (166, 72, "minecraft:fern"),
                          (166, 70, "minecraft:mossy_cobblestone"), (146, 73, "minecraft:fern")):
            S.set(x, G + 1, z, b)
        for v in ("kikkerguh_roze", "kikkerguh_mint", "kikkerguh_geel"):
            self.critter("guhs:kikkerguh", v, "terrarium", 156.5, G + 1, 71.5, straal=4)
        self.kijk.append(("kikkerhoek", (156, G + 0.5, 71.5), (156, G + 1, 78)))
        self.doel("kikkerhoek", 156, G + 1, 78)
        for x in range(TX1 + 4, TX2 - 2, 8):
            S.set(x, G + 1, 64, "guhs:guh_bloempot")
            S.set(x + 2, G + 1, 78, "minecraft:potted_fern")
        S.set(TX2 - 2, G + 1, 66, "guhs:park_bankje[facing=west]")
        S.set(TX1 + 2, G + 1, 76, "guhs:park_bankje[facing=east]")
        self.looproute()
        # the glass kas dome
        self.koepel(155, 71, 21.5, 21.5, G + 8, 13, TX1, TZ1, TX2, TZ2, KASGLAS, rib="minecraft:white_concrete")
        for x in range(TX1 + 8, TX2 - 2, 8):
            self.lantaarnpaal(x, 64)
            self.lantaarnpaal(x, 78)

    def looproute(self):
        """Round 5: the door to the Kriebelzaal was hidden behind the cases. A paw-tile route from the hall door round the
        kikkerhoek to the aisle between the north cases (x 153..155) and the door, with little lamps and a sign."""
        S = self.S
        TZ1 = TERRARIUM[1]
        route = [(x, 80) for x in range(135, 146)] + [(145, z) for z in range(79, 61, -1)] +                 [(x, 62) for x in range(146, 155)] + [(154, z) for z in range(61, TZ1, -1)]
        for (x, z) in route:
            S.set(x, G, z, "guhs:tegel_pootjes")
            self.paden.add((x, z))
        for z in range(TZ1 + 1, 62):                                                   # the aisle stays free and walkable
            for x in (153, 155):
                S.set(x, G, z, "minecraft:smooth_sandstone")
                S.air(x, G + 1, z, x, G + 4, z)
                self.paden.add((x, z))
            S.air(154, G + 1, z, 154, G + 4, z)
        for (x, z) in ((153, 52), (155, 52)):
            S.set(x, G + 1, z, "guhs:toro[lit=true]")
        S.set(144, G + 1, 63, "minecraft:birch_sign[rotation=0,waterlogged=false]")
        self.park.bord_nbt(*S.w(144, G + 1, 63), ["", "Kriebelzaal", "volg de", "pootjes ->"])

    def sectiebord(self, x, y, z, facing, regels):
        self.S.set(x, y, z, f"minecraft:oak_wall_sign[facing={facing},waterlogged=false]")
        self.park.bord_nbt(*self.S.w(x, y, z), regels)

    def koepel(self, cx, cz, rx, rz, yb, h, x1, z1, x2, z2, schil, rib=None, ster=None):
        """Half ellipsoid roof on the walls (yb = wall top); corners outside the ellipse: a flat roof at yb."""
        S = self.S
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                r = math.hypot((x - cx) / rx, (z - cz) / rz)
                if r >= 1:
                    S.set(x, yb, z, schil if not rib else rib)
                    continue
                y = yb + h * math.sqrt(max(0.0, 1 - r * r))
                ys = [int(round(y))]
                for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    rr = math.hypot((x + a - cx) / rx, (z + b - cz) / rz)
                    yn = yb + h * math.sqrt(max(0.0, 1 - rr * rr)) if rr < 1 else yb
                    for yy in range(int(round(yn)) + 1, ys[0]):
                        ys.append(yy)
                for yy in ys:
                    blok = schil
                    if rib and (x % 6 == 0 or z % 6 == 0):
                        blok = rib
                    if ster and self.rnd.random() < 0.05:
                        blok = ster
                    S.set(x, yy, z, blok)

    # ==================================================================================================================
    def kriebelzaal(self):
        S = self.S
        KX1, KZ1, KX2, KZ2 = KRIEBELZAAL
        TZ1 = TERRARIUM[1]
        S.fill(KX1, G - 2, KZ1, KX2, G - 1, KZ2, "minecraft:dirt")
        self.box(KX1, G + 1, KZ1, KX2, G + 8, KZ2, "minecraft:light_gray_terracotta", floor=MOS)
        S.air(153, G + 1, TZ1, 155, G + 3, TZ1)
        S.fill(153, G + 3, TZ1, 155, G + 3, TZ1, "minecraft:terracotta")
        S.set(153, G + 1, TZ1, "minecraft:terracotta"); S.set(153, G + 2, TZ1, "minecraft:terracotta")
        S.set(155, G + 1, TZ1, "minecraft:terracotta"); S.set(155, G + 2, TZ1, "minecraft:terracotta")
        self.door2(154, G + 1, TZ1, "north", "guhs:guhwaii_palm_deur")
        self.sectiebord(154, G + 4, TZ1 + 1, "south", ["", "2", "Kriebelzaal", ""])
        KRONKEL = [(154, 49), (153, 42), (166, 38), (173, 28), (170, 16), (156, 10), (142, 14), (136, 24), (129, 27)]
        kpad = set()
        for (ax, az), (bx, bz) in zip(KRONKEL, KRONKEL[1:]):
            n = int(max(abs(bx - ax), abs(bz - az)))
            for i in range(n + 1):
                t = i / max(n, 1)
                cx, cz = round(ax + (bx - ax) * t), round(az + (bz - az) * t)
                for a in range(-2, 3):
                    for b in range(-2, 3):
                        if KX1 < cx + a < KX2 and KZ1 < cz + b < KZ2:
                            kpad.add((cx + a, cz + b))
        for (x, z) in kpad:
            S.set(x, G, z, "minecraft:spruce_planks" if (x + 2 * z) % 7 else "minecraft:stripped_spruce_wood[axis=y]")
            S.air(x, G + 1, z, x, G + 7, z)
            self.paden.add((x, z))
        rand = set()
        for (x, z) in kpad:
            for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + a, z + b) not in kpad and KX1 < x + a < KX2 and KZ1 < z + b < KZ2:
                    rand.add((x + a, z + b))
        kooi_cellen = set()

        def kooitje(px, pz, nx, nz, soort, v=1, j=0, bodem=MOS, stijl="bos"):
            """A small cage (5 x 5, inside 3 x 3, 3 high glass) on a plinth, its glass front right at the path edge."""
            cx, cz = px + nx * 5, pz + nz * 5
            x1, z1 = cx - 2, cz - 2
            front = {(0, 1): "n", (0, -1): "s", (1, 0): "w", (-1, 0): "e"}[(nx, nz)]
            for x in range(x1 - 1, x1 + 6):
                for z in range(z1 - 1, z1 + 6):
                    kooi_cellen.add((x, z))
                    if (x, z) not in kpad:
                        S.air(x, G + 1, z, x, G + 6, z)
            ix1, iy, iz1, ix2, iz2 = self.terrarium(x1, z1, 5, 5, 3, front, bodem, soort)
            self.decor_binnen(ix1, iy, iz1, ix2, iz2, stijl)
            self.huis(soort, "kriebelzaal", cx, iy, cz, v, j, straal=1)

        kooitje(154, 44, 1, 0, "springspin")
        kooitje(162, 39, 0, 1, "wandelende_tak", stijl="jungle")
        kooitje(170, 32, -1, 0, "mestkever", bodem="minecraft:coarse_dirt", stijl="woestijn")
        kooitje(170, 20, -1, 0, "slak", stijl="jungle")
        kooitje(160, 11, 0, 1, "vogelspin", bodem="minecraft:coarse_dirt", stijl="woestijn")
        kooitje(145, 13, 0, 1, "hommel")
        kooitje(138, 22, -1, 0, "mier", v=3, bodem="minecraft:coarse_dirt", stijl="woestijn")
        for (x, z) in rand:
            if (x, z) in kooi_cellen:
                continue
            S.set(x, G + 1, z, HEGB if (x * 5 + z * 3) % 5 == 0 else HEG)
        # the ant farm on the east wall: a glass slab with tunnels, the path comes right up to it
        for z in range(22, 36):
            for y in range(G + 1, G + 7):
                S.set(KX2 - 1, y, z, "minecraft:coarse_dirt" if (z * 3 + y * 5) % 7 > 2 else "minecraft:brown_terracotta")
                S.set(KX2 - 2, y, z, "guhs:mierenboerderij")
        for z in range(24, 35):
            for x in range(174, KX2 - 2):
                S.set(x, G, z, "minecraft:spruce_planks")
                S.air(x, G + 1, z, x, G + 6, z)
                self.paden.add((x, z))
                kpad.add((x, z))
        self.kijk.append(("mierenboerderij", (KX2 - 2, G + 4, 29), (176, G + 1, 29)))
        self.doel("mierenboerderij", 176, G + 1, 29)
        self.infobord(KX2 - 3, G + 1, 23, "west", "mier")
        # vlindertuin: an open garden room south-west, its own loop from the path
        VX1, VZ1, VX2, VZ2 = 130, 32, 146, 48
        for x in range(VX1, VX2 + 1):
            for z in range(VZ1, VZ2 + 1):
                if (x, z) in kpad:
                    continue
                S.set(x, G, z, "minecraft:grass_block[snowy=false]")
                S.air(x, G + 1, z, x, G + 6, z)
                if (x * 7 + z * 3) % 4 == 0:
                    S.set(x, G + 1, z, self.rnd.choice(["minecraft:poppy", "minecraft:allium", "minecraft:oxeye_daisy",
                                                        "minecraft:cornflower", "guhs:roze_guhbloem"]))
        self.pad_lijn([(150, 44), (140, 44), (136, 38), (140, 34), (148, 36)], w=3,
                      pal=("minecraft:moss_block", "minecraft:rooted_dirt"))
        # a beehive behind glass (a case at the garden edge, its front toward the garden path)
        ix1, iy, iz1, ix2, iz2 = self.terrarium(131, 39, 3, 3, 3, "e", "minecraft:grass_block[snowy=false]", "")
        S.set(ix1, iy, iz1, "minecraft:bee_nest[facing=east,honey_level=3]")
        for v in ("knabbelvlindertje_roze", "knabbelvlindertje_mint", "knabbelvlindertje_lila"):
            self.critter("guhs:knabbelvlindertje", v, "vlindertuin", 140.5, G + 3, 40.5, straal=6)
        self.critter("guhs:lieveheersbeestje", "lieveheersbeestje", "vlindertuin", 139.5, G + 1, 46.5, straal=5)
        self.critter("guhs:lieveheersbeestje", "lieveheersbeestje", "vlindertuin", 143.5, G + 1, 35.5, straal=5)
        self.critter("guhs:guh_bee", "guh_bee", "vlindertuin", 134.5, G + 2, 42.5, straal=5)
        self.doel("vlindertuin", 140, G + 1, 40)
        for (px_, pz_) in KRONKEL[1:-1]:                                              # little lamps at the bends
            for (a, b) in ((3, 0), (-3, 0), (0, 3), (0, -3)):
                if (px_ + a, pz_ + b) not in kpad and (px_ + a, pz_ + b) not in kooi_cellen:
                    self.lantaarnpaal(px_ + a, pz_ + b)
                    break
        for (x, z) in ((150, 26), (160, 28), (146, 24)):                              # giant mushroom decor
            if (x, z) in kpad or (x, z) in kooi_cellen:
                continue
            S.fill(x, G + 1, z, x, G + 3, z, "minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=true,west=true]")
            S.ellipsoid(x, G + 4, z, 2.2, 0.9, 2.2,
                        "minecraft:red_mushroom_block[down=false,east=true,north=true,south=true,up=true,west=true]")
            S.set(x + 1, G + 1, z, "guhs:glimpaddenstoel")
        for x in range(KX1 + 4, KX2 - 2, 7):
            S.set(x, G + 8, 27, "minecraft:verdant_froglight[axis=y]")
            S.set(x, G + 8, 14, "minecraft:pearlescent_froglight[axis=y]")
        # round 5: more light, the vlindertuin most of all (lampions over the garden, guh lamps along its path, glow
        # mushrooms in the beds) and lampions over the whole winding path
        self.lampions(VX1 + 1, VZ1 + 1, VX2 - 1, VZ2 - 1, G + 8, stap=3)
        for i, (x, z) in enumerate(sorted(kpad)):
            if i % 9 == 0 and (x, z) not in kooi_cellen:
                self.lampions(x, z, x, z, G + 8)
        for (x, z) in ((133, 34), (144, 34), (133, 47), (144, 47), (138, 41)):
            if S.leeg(x, G + 1, z) and (x, z) not in kpad:
                S.set(x, G + 1, z, "guhs:glimpaddenstoel")
        for (x, z) in ((137, 44), (143, 38), (147, 45)):
            if S.leeg(x, G + 1, z) and (x, z) not in kpad:
                S.set(x, G + 1, z, "guhs:doolhof_lantaarn[lit=true]")
        for (x, z, f) in ((131, 33, "east"), (145, 47, "west")):
            if S.leeg(x, G + 1, z) and S.leeg(x, G + 2, z):
                S.set(x, G + 1, z, "minecraft:spruce_fence")
                S.set(x, G + 2, z, f"guhs:vogelhuisje[facing={f}]")
        # roof: flat green + leaf-shaped roofs + a big ladybird
        S.fill(KX1, G + 9, KZ1, KX2, G + 9, KZ2, "minecraft:green_terracotta")
        for (cx, cz, Ln, Wd, ang) in ((142, 15, 15, 8, 25), (168, 14, 16, 8, -30), (142, 38, 14, 8, -20),
                                      (169, 39, 15, 8, 35)):
            self.blad(cx, cz, Ln, Wd, G + 10, ang)
        LBX, LBZ, LBY = 156, 27, G + 12
        S.ellipsoid(LBX, LBY, LBZ, 5.2, 3.0, 6.2, "minecraft:red_wool")
        S.fill(LBX - 3, G + 10, LBZ - 4, LBX + 3, LBY - 1, LBZ + 4, "minecraft:red_wool")
        for (a, b) in ((-2, -3), (2, -3), (-3, 1), (3, 1), (0, 4), (0, -1)):
            S.set(LBX + a, LBY + 3, LBZ + b, "minecraft:black_wool")
        S.fill(LBX, LBY, LBZ - 6, LBX, LBY + 3, LBZ + 6, "minecraft:black_wool")
        S.ellipsoid(LBX, LBY + 0.5, LBZ + 7, 3.0, 2.4, 2.4, "minecraft:black_wool")
        S.set(LBX - 1, LBY + 1, LBZ + 9, "minecraft:white_concrete")
        S.set(LBX + 1, LBY + 1, LBZ + 9, "minecraft:white_concrete")
        S.set(LBX - 2, LBY, LBZ + 9, "minecraft:pink_wool")
        S.set(LBX + 2, LBY, LBZ + 9, "minecraft:pink_wool")
        for side in (-1, 1):
            S.fill(LBX + side * 2, LBY + 3, LBZ + 8, LBX + side * 2, LBY + 5, LBZ + 8, "minecraft:black_wool")
            S.set(LBX + side * 2, LBY + 6, LBZ + 8, "minecraft:pink_wool")

    def blad(self, cx, cz, L, W, yb, ang, col="minecraft:lime_terracotta", rib="minecraft:green_concrete"):
        ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        for x in range(int(cx - L - 2), int(cx + L + 3)):
            for z in range(int(cz - L - 2), int(cz + L + 3)):
                dx, dz = x - cx, z - cz
                u, v = dx * ca + dz * sa, -dx * sa + dz * ca
                if abs(u) >= L:
                    continue
                wmax = W * (1 - (u / L) ** 2)
                if abs(v) >= wmax:
                    continue
                y = yb + 3 * (1 - (v / max(wmax, 0.1)) ** 2) * (1 - 0.4 * (u / L) ** 2)
                yy = int(round(y))
                self.S.fill(x, yb, z, x, yy, z, col)
                if abs(v) < 0.7:
                    self.S.set(x, yy, z, rib)

    # ==================================================================================================================
    def nachtzaal(self):
        S = self.S
        NX1, NZ1, NX2, NZ2 = NACHTZAAL
        KX1 = KRIEBELZAAL[0]
        S.fill(NX1, G - 2, NZ1, NX2, G - 1, NZ2, "minecraft:dirt")
        self.box(NX1, G + 1, NZ1, NX2, G + 8, NZ2, "minecraft:black_terracotta", floor="minecraft:blue_terracotta")
        S.air(KX1, G + 1, 26, KX1, G + 3, 28)
        S.fill(KX1, G + 3, 26, KX1, G + 3, 28, "minecraft:light_gray_terracotta")
        S.set(KX1, G + 1, 26, "minecraft:light_gray_terracotta"); S.set(KX1, G + 2, 26, "minecraft:light_gray_terracotta")
        S.set(KX1, G + 1, 28, "minecraft:light_gray_terracotta"); S.set(KX1, G + 2, 28, "minecraft:light_gray_terracotta")
        self.door2(KX1, G + 1, 27, "west")
        self.sectiebord(KX1 - 1, G + 3, 25, "west", ["", "3", "Nachtzaal", ""])
        for x in range(NX1 + 1, NX2):
            for z in range(21, 32):
                S.set(x, G, z, "minecraft:blue_terracotta" if (x + z) % 6 else "minecraft:sea_lantern")
                self.paden.add((x, z))
        NV = [((88, 5, 106, 17), "n", [("spookdiertje", 1, 1, False), ("plompe_lori", 1, 1, True)]),
              ((108, 5, 126, 17), "n", [("fruitvleermuis", 3, 0, True), ("galago", 1, 1, True)]),
              ((88, 35, 96, 45), "s", [("kiwi", 1, 1, False), ("schubdier", 1, 1, False)]),
              ((104, 35, 126, 45), "s", [("suikereekhoorn", 2, 0, True), ("fennek", 1, 1, False)])]
        for (x1, z1, x2, z2), kant, dieren in NV:
            S.fill(x1, G + 1, z1, x2, G + 1, z2, MOS)
            zz = z2 + 1 if kant == "n" else z1 - 1
            wand_z = (NZ1 + 1, zz) if kant == "n" else (zz, NZ2 - 1)
            for x in (x1 - 1, x2 + 1):                                              # side walls up to the star ceiling
                S.fill(x, G + 1, wand_z[0], x, G + 8, wand_z[1], "minecraft:black_terracotta")
            for x in range(x1, x2 + 1):
                S.fill(x, G + 1, zz, x, G + 8, zz, TGLAS)
            S.fill(x1 - 1, G + 9, wand_z[0], x2 + 1, G + 9, wand_z[1], "guhs:sterrenplafond")
            for k in range(3):                                                        # branches
                tx = x1 + 3 + k * 6
                if tx > x2 - 2:
                    break
                tz = z1 + 3 if kant == "n" else z2 - 3
                S.fill(tx, G + 2, tz, tx, G + 6, tz, "minecraft:dark_oak_log[axis=y]")
                S.fill(tx - 2, G + 6, tz, tx + 2, G + 6, tz, "minecraft:dark_oak_log[axis=x]")
                S.set(tx + 2, G + 7, tz, "minecraft:moss_carpet")
            for k in range(7):                                                        # glow mushrooms + lichen + ferns
                gx, gz = self.rnd.randint(x1 + 1, x2 - 1), self.rnd.randint(z1 + 1, z2 - 1)
                if S.leeg(gx, G + 2, gz):
                    S.set(gx, G + 2, gz, "guhs:glimpaddenstoel" if k % 2 == 0 else "minecraft:fern")
            S.set((x1 + x2) // 2, G + 1, (z1 + z2) // 2, "minecraft:sea_lantern")       # moonlight from below the moss
            S.set(x1 + 2, G + 2, z1 + 1 if kant == "n" else z2 - 1, "minecraft:water_cauldron[level=3]")
            cx = (x1 + x2) / 2
            self.kijk.append(("nachtverblijf " + "/".join(d[0] for d in dieren), (cx, G + 3.5, zz), (int(cx), G + 1, 26)))
            self.infobord(int(cx) - 4, G + 1, zz + (1 if kant == "n" else -1), "south" if kant == "n" else "north",
                          dieren[0][0])
            self.infobord(int(cx) + 4, G + 1, zz + (1 if kant == "n" else -1), "south" if kant == "n" else "north",
                          dieren[1][0])
            for i, (soort, v, j, boom) in enumerate(dieren):
                hx = x1 + 4 + i * (x2 - x1 - 8)
                hz = (z1 + z2) // 2
                self.huis(soort, "nachtzaal", hx, G + 2, hz, v, j, straal=3, boom=boom)
        self.doel("nachtzaal", 107, G + 1, 26)
        self.koepel(107, 25, 22, 22, G + 8, 10, NX1, NZ1, NX2, NZ2, "minecraft:blue_concrete",
                    rib="minecraft:black_concrete", ster="guhs:sterrenplafond")
        for x in range(NX1 + 4, NX2 - 2, 8):                                          # dim blue light over the aisle
            S.set(x, G + 1, 21, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
            S.set(x + 4, G + 1, 31, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
        S.set(NX1 + 2, G + 1, 26, "guhs:park_bankje[facing=east]")
        for z in range(34, NZ2, 3):                                                    # round 5: a little light in the
            S.set(100, G, z, "minecraft:sea_lantern")                                 # corridor to the Diepzeezaal door
            self.paden.add((100, z))
        for (x, z) in ((98, 36), (102, 40), (98, 44)):
            if S.leeg(x, G + 1, z):
                S.set(x, G + 1, z, "minecraft:soul_lantern[hanging=false,waterlogged=false]")

    # ==================================================================================================================
    def diepzeezaal(self):
        S = self.S
        DX1, DZ1, DX2, DZ2 = DIEPZEE
        NZ2 = NACHTZAAL[3]
        S.fill(DX1, G - 2, DZ1, DX2, G - 1, DZ2, "minecraft:dirt")
        self.box(DX1, G + 1, DZ1, DX2, G + 8, DZ2, "minecraft:black_terracotta", floor="minecraft:black_concrete")
        S.air(99, G + 1, NZ2, 101, G + 3, DZ1)
        for x in (98, 102):                                                            # the short corridor
            S.fill(x, G + 1, NZ2 + 1, x, G + 4, DZ1 - 1, "minecraft:black_terracotta")
        S.fill(98, G + 4, NZ2, 102, G + 4, DZ1, "minecraft:black_terracotta")
        S.fill(99, G, NZ2, 101, G, DZ1, "minecraft:black_concrete")
        for z in range(NZ2, DZ1 + 1):
            self.paden.add((99, z)); self.paden.add((100, z)); self.paden.add((101, z))
        S.set(99, G + 1, NZ2, "minecraft:black_terracotta"); S.set(99, G + 2, NZ2, "minecraft:black_terracotta")
        S.set(101, G + 1, NZ2, "minecraft:black_terracotta"); S.set(101, G + 2, NZ2, "minecraft:black_terracotta")
        S.set(99, G + 3, NZ2, "minecraft:black_terracotta"); S.set(100, G + 3, NZ2, "minecraft:black_terracotta")
        S.set(101, G + 3, NZ2, "minecraft:black_terracotta")
        self.door2(100, G + 1, NZ2, "south")
        self.sectiebord(100, G + 3, NZ2 - 1, "north", ["", "4", "Diepzeezaal", ""])
        S.fill(DX1, G + 9, DZ1, DX2, G + 9, DZ2, "minecraft:dark_prismarine")
        tanks = (((85, 91), [("kwal", 52), ("lantaarnvisje", 55), ("dumbo_octopus", 66)]),
                 ((103, 109), [("kogelvis", 53), ("babyhaai", 63)]))
        for (ax1, ax2), dieren in tanks:
            S.fill(ax1, G, DZ1 + 1, ax2, G, DZ2 - 1, "minecraft:sand")
            S.fill(ax1, G + 1, DZ1 + 1, ax2, G + 7, DZ2 - 1, WATER)
            gx = ax2 if ax1 < 96 else ax1
            wx = ax1 if ax1 < 96 else ax2
            S.fill(gx, G + 1, DZ1 + 1, gx, G + 8, DZ2 - 1, "minecraft:blue_stained_glass")
            S.fill(ax1, G + 8, DZ1 + 1, ax2, G + 8, DZ2 - 1, "minecraft:dark_prismarine")
            for z in range(DZ1 + 2, DZ2 - 1, 4):
                S.set((ax1 + ax2) // 2, G, z, "minecraft:sea_lantern")
            for z in range(DZ1 + 1, DZ2):
                for y in range(G + 1, G + 8):
                    S.set(wx, y, z, "minecraft:sea_lantern" if self.rnd.random() < 0.05 else "minecraft:deepslate_tiles")
            for z in range(DZ1 + 1, DZ2):
                for x in range(min(ax1, ax2) + 1, max(ax1, ax2)):
                    if x == gx or x == wx:
                        continue
                    r = self.rnd.random()
                    if r < 0.10:
                        S.set(x, G + 1, z, "guhs:nepkoraal[kleur=%s,waterlogged=true]" % self.rnd.choice(KORAAL))
                    elif r < 0.17:
                        S.set(x, G + 1, z, "minecraft:deepslate")
                        if self.rnd.random() < 0.5:
                            S.set(x, G + 2, z, "minecraft:cobbled_deepslate")
                    elif r < 0.21:
                        S.set(x, G + 1, z, "minecraft:sea_pickle[pickles=4,waterlogged=true]")
                    elif r < 0.27:
                        S.set(x, G + 1, z, "minecraft:seagrass")
            kant = "west" if ax1 < 96 else "oost"
            stand_x = gx + (2 if ax1 < 96 else -2)
            self.kijk.append(("diepzeebak " + kant, (gx, G + 4, 53), (stand_x, G + 1, 53)))
            self.kijk.append(("diepzeebak " + kant + " zuid", (gx, G + 4, 65), (stand_x, G + 1, 65)))
            self.doel("diepzeebak " + kant, stand_x, G + 1, 53)
            cx = (ax1 + ax2) / 2
            for soort, hz in dieren:
                # round 5: at least 3 of every fish (small ones 8), their home in the middle of the water (G+1..G+7)
                volw, jong = AANTAL_VIS.get(soort, (3, 0))
                self.huis(soort, "diepzeezaal", int(cx), G + 4, int(hz), volw, jong, straal=3, water=True)
                self.infobord(gx + (1 if ax1 < 96 else -1), G + 1, int(hz), "east" if ax1 < 96 else "west", soort)
            if ax1 < 96:
                self.critter("guhs:kruimelkwal", "kruimelkwal", "diepzeezaal", cx, G + 5, 64.5, straal=3, water=True)
        # an aquarium corridor through the west tank to the stairs down (z 58..60, glass on both sides)
        S.fill(84, G + 1, 57, 91, G + 7, 57, "minecraft:blue_stained_glass")
        S.fill(84, G + 1, 61, 91, G + 7, 61, "minecraft:blue_stained_glass")
        S.fill(84, G + 4, 58, 91, G + 8, 60, "minecraft:dark_prismarine")
        S.fill(84, G, 58, 91, G, 60, "minecraft:black_concrete")
        S.air(84, G + 1, 58, 91, G + 3, 60)
        for x in range(84, 92):
            for z in (58, 59, 60):
                self.paden.add((x, z))
        for x in (86, 90):
            S.set(x, G + 3, 59, "minecraft:soul_lantern[hanging=true,waterlogged=false]")
        self.doel("aquariumgang", 88, G + 1, 59)
        S.fill(92, G, DZ1 + 1, 102, G, DZ2 - 1, "minecraft:black_concrete")
        for z in range(DZ1 + 1, DZ2):
            for x in range(92, 103):
                self.paden.add((x, z))
        for z in range(DZ1 + 1, DZ2, 3):
            S.set(97, G, z, "minecraft:sea_lantern")
        S.set(97, G + 1, 66, "guhs:park_bankje[facing=north]")
        self.doel("diepzeezaal", 97, G + 1, 59)

    # ==================================================================================================================
    def vijver_en_buis(self):
        """4b: the pink tube under the pond through the rif-lagune (east) and the kelpbos (west), with a viewing cave."""
        S = self.S
        DX1 = DIEPZEE[0]
        # the pond: L1 digs it; here only make sure it is deep enough where the tube runs (and exists at all)
        pond = set()
        for x in range(2, 82):
            for z in range(20, 88):
                if math.hypot((x - PCX) / PRX, (z - PCZ) / PRZ) < 1.0:
                    pond.add((x, z))
        wy_opp = None
        for (x, z) in sorted(pond):
            for y in range(G, G - 4, -1):
                if S.get(x, y, z).startswith("minecraft:water"):
                    wy_opp = y if wy_opp is None else max(wy_opp, y)
                    break
        if wy_opp is None:
            self.log("    kriebelhuis: no pond from zone_entree here: digging lobby2's pond")
            wy_opp = G
        self.vijver_y = wy_opp
        diep = 0
        verdiept = set()
        for (x, z) in pond:
            r = math.hypot((x - PCX) / PRX, (z - PCZ) / PRZ)
            if not S.get(x, wy_opp, z).startswith("minecraft:water") and r > 0.9:
                continue
            bodem = wy_opp - (10 if r < 0.82 and x > 6 else 3)
            for y in range(bodem + 1, wy_opp + 1):
                if not S.get(x, y, z).startswith("minecraft:water"):
                    S.set(x, y, z, WATER)
                    diep += 1
                    verdiept.add((x, z))
            S.set(x, bodem, z, BED)
        self.log(f"    kriebelhuis: pond surface local y {wy_opp} ({diep} water blocks added for depth)")
        TY = min(G, wy_opp) - 6
        # pond floor: coral reef east, kelp forest west
        for (x, z) in sorted(verdiept):
            y0 = None
            for y in range(wy_opp, wy_opp - 14, -1):
                if not S.get(x, y, z).startswith("minecraft:water"):
                    y0 = y
                    break
            if y0 is None or y0 > wy_opp - 4:
                continue
            r = self.rnd.random()
            if x >= 32:
                if r < 0.10:
                    S.set(x, y0, z, self.rnd.choice(["minecraft:tube_coral_block", "minecraft:brain_coral_block",
                                                     "minecraft:fire_coral_block", "minecraft:horn_coral_block"]))
                    if self.rnd.random() < 0.6:
                        S.set(x, y0 + 1, z, "guhs:nepkoraal[kleur=%s,waterlogged=true]" % self.rnd.choice(KORAAL))
                elif r < 0.14:
                    S.set(x, y0, z, "minecraft:sea_lantern")
                elif r < 0.24:
                    S.set(x, y0 + 1, z, "minecraft:seagrass")
            else:
                if r < 0.22:
                    hk = self.rnd.randint(3, 7)
                    for k in range(hk):
                        if S.get(x, y0 + 1 + k + 1, z).startswith("minecraft:water"):
                            S.set(x, y0 + 1 + k, z, "minecraft:kelp_plant")
                        else:
                            break
                    S.set(x, y0 + 1 + k, z, "minecraft:kelp[age=20]")
                elif r < 0.27:
                    S.set(x, y0, z, self.rnd.choice(["minecraft:tuff", "minecraft:mossy_cobblestone", "minecraft:andesite"]))
                    S.set(x, y0 + 1, z, self.rnd.choice(["minecraft:tuff", "minecraft:andesite"]))
        # the tube: a pink glass shell (r 3.9) hollowed out (r 3.0), quartz floor
        tubec = []
        for (ax, az), (bx, bz) in zip(TUBE, TUBE[1:]):
            n = int(max(abs(bx - ax), abs(bz - az)))
            for i in range(n + 1):
                t = i / max(n, 1)
                tubec.append((ax + (bx - ax) * t, az + (bz - az) * t))
        cellen = {}
        for (cx, cz) in tubec:
            for x in range(int(cx) - 4, int(cx) + 5):
                for z in range(int(cz) - 4, int(cz) + 5):
                    for y in range(TY - 3, TY + 7):
                        d = math.hypot(math.hypot(x - cx, z - cz), y - (TY + 1.5))
                        if d < 3.9:
                            k = (x, y, z)
                            cellen[k] = min(cellen.get(k, 9), d)
        for (x, y, z), d in cellen.items():
            S.set(x, y, z, PINKG if d >= 3.0 else "minecraft:smooth_quartz" if y <= TY else "minecraft:air")
        for (cx, cz) in tubec:
            for x in range(int(cx) - 1, int(cx) + 2):
                for z in range(int(cz) - 1, int(cz) + 2):
                    if S.leeg(x, TY, z) or S.get(x, TY - 1, z) == PINKG:
                        S.set(x, TY, z, "minecraft:smooth_quartz")
                        self.paden.add((x, z))
        for i, (cx, cz) in enumerate(tubec[::9]):                                    # soft light in the tube floor
            S.set(int(cx), TY, int(cz), "minecraft:pearlescent_froglight[axis=y]")
        # stairs down: diepzee hall west door -> tube, a closed pink glass corridor
        for i in range(G - TY + 1):
            x, yf = DX1 - 1 - i, G - i
            S.fill(x, yf - 1, 57, x, yf + 4, 61, PINKG)
            for z in (58, 59, 60):
                S.set(x, yf, z, "minecraft:quartz_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]"
                      if i < G - TY else "minecraft:smooth_quartz")
                S.air(x, yf + 1, z, x, yf + 3, z)
                self.paden.add((x, z))
        x_onder = DX1 - 1 - (G - TY)
        for x in range(x_onder - 2, x_onder + 1):                                      # landing to the tube's first point
            S.fill(x, TY - 1, 57, x, TY + 4, 61, PINKG)
            for z in (58, 59, 60):
                S.set(x, TY, z, "minecraft:smooth_quartz")
                S.air(x, TY + 1, z, x, TY + 3, z)
        for (x, y, z), d in cellen.items():                                           # re-hollow where they overlap
            if d < 3.0 and y > TY:
                S.set(x, y, z, "minecraft:air")
        self.buis_mantel(tubec, cellen, wy_opp, TY)
        # the viewing cave in the kelp forest: a stone shell on the east side, the glass stays toward the kelp (west)
        for (cx, cz) in tubec:
            if not (14 <= cx <= 28 and 46 <= cz <= 70):
                continue
            for x in range(int(cx), int(cx) + 6):
                for z in range(int(cz) - 5, int(cz) + 6):
                    for y in range(TY - 3, TY + 8):
                        d3 = math.hypot(math.hypot(x - cx, z - cz), y - (TY + 1.5))
                        if 3.9 <= d3 < 5.2 and x > cx + 1 and S.get(x, y, z).startswith(("minecraft:water", "minecraft:sand")):
                            S.set(x, y, z, self.rnd.choice(["minecraft:tuff", "minecraft:mossy_cobblestone", "minecraft:andesite"]))
        kit.stoel(self.park, *S.w(18, TY + 1, 58), "west" if L_K0(S) else _DRAAI_F[(3 + S.k) % 4])
        S.set(18, TY + 4, 58, "minecraft:lantern[hanging=true,waterlogged=false]")
        kp = self.park.kijkplek("kriebelhuis_kijkgrot", "entree", S.wf(18.5, TY + 2.6, 58.5), _richting(S, -1, 0),
                                S.w(18, TY + 1, 58))
        self.kijkgrot = kp
        self.doel("kijkgrot kelpbos", 16, TY + 1, 56)
        self.kijk.append(("kijkgrot kelpbos", (13.5, TY + 2, 56), (16, TY + 1, 56)))
        self.doel("roze buis", 40, TY + 1, 44)
        self.TY = TY
        # the animals around the tube: rif-lagune (east) and kelpbos (west)
        W = wy_opp
        # round 5: more animals round the tube: at least 3 of every species, the small fish at least 8
        A = AANTAL_VIS
        self.huis("clownvis", "rif-lagune", 54, W - 6, 42, *A["clownvis"], straal=4, water=True)
        self.huis("anemoon", "rif-lagune", 52, W - 9, 41, *A["anemoon"], straal=1, water=True)
        self.huis("regenboogvis", "rif-lagune", 46, W - 5, 66, *A["regenboogvis"], straal=6, water=True)
        self.huis("zeeschildpad", "rif-lagune", 60, W - 5, 70, *A["zeeschildpad"], straal=8, water=True)
        for (x, y, z) in ((48, W - 5, 58), (58, W - 4, 66), (38, W - 5, 52), (44, W - 6, 72), (62, W - 5, 56),
                          (34, W - 6, 64), (54, W - 5, 76), (40, W - 4, 40)):
            self.critter("guhs:guh_vis", "guh_vis", "rif-lagune", x + 0.5, y, z + 0.5, straal=6, water=True)
        self.huis("zeepaardje", "kelpbos", 12, W - 5, 56, *A["zeepaardje"], straal=4, water=True)
        self.huis("pijlstaartrog", "kelpbos", 28, W - 7, 64, *A["pijlstaartrog"], straal=6, water=True)
        self.huis("zeester", "kelpbos", 22, W - 9, 70, *A["zeester"], straal=3, water=True)
        self.huis("zee_egel", "kelpbos", 30, W - 9, 40, *A["zee_egel"], straal=3, water=True)
        self.huis("regenboogvis", "kelpbos", 20, W - 5, 46, *A["regenboogvis"], straal=6, water=True)
        for (x, y, z, v) in ((16, W - 4, 40, "koi_roodwit"), (30, W - 3, 34, "koi_goud"), (8, W - 4, 64, "koi_driekleur"),
                             (24, W - 4, 60, "koi_roodwit"), (10, W - 3, 48, "koi_goud"), (34, W - 4, 58, "koi_driekleur")):
            self.critter("guhs:koi", v, "kelpbos", x + 0.5, y, z + 0.5, straal=6, water=True)
        for soort, x, z in (("zeepaardje", 13, 54), ("pijlstaartrog", 14, 62), ("zeester", 15, 50)):
            self.park.favoriet(f"kijkgrot_{soort}", kp, S.w(x, W - 6, z), [soort], straal=2)
        # tube infoborden (on the tube floor, against the glass)
        for (x, z, f, soort) in ((52, 47, "south", "clownvis"), (44, 46, "south", "regenboogvis"),
                                 (24, 51, "east", "zeepaardje"), (40, 79, "north", "zeeschildpad"),
                                 (19, 64, "east", "pijlstaartrog")):
            if S.leeg(x, TY + 1, z) and S.get(x, TY, z) != "minecraft:air":
                self.infobord(x, TY + 1, z, f, soort)

    def buis_mantel(self, tubec, cellen, wy_opp, TY):
        """Round 5: past halfway the tube runs where the pond is shallow, so dirt and stone lay against the glass. Inside
        the pond the water now reaches down round the tube (a sand floor below it); where the tube runs under land, the
        ground against the glass is rock, like the viewing cave."""
        S = self.S
        bij = {}
        for (cx, cz) in tubec:
            for x in range(int(cx) - 6, int(cx) + 7):
                for z in range(int(cz) - 6, int(cz) + 7):
                    d = math.hypot(x - cx, z - cz)
                    if d < 5.6:
                        bij[(x, z)] = min(bij.get((x, z), 9), d)
        grond = ("minecraft:dirt", "minecraft:grass_block", "minecraft:stone", "minecraft:coarse_dirt",
                 "minecraft:rooted_dirt", "minecraft:gravel", "minecraft:clay", "minecraft:sand")
        n_water = n_rots = 0
        for (x, z), dh in bij.items():
            in_vijver = math.hypot((x - PCX) / PRX, (z - PCZ) / PRZ) < 0.97
            for y in range(TY - 4, wy_opp):                                       # (the shore at the surface stays)
                if (x, y, z) in cellen:
                    continue
                b = S.get(x, y, z)
                if not b.startswith(grond):
                    continue
                if in_vijver and y >= TY - 3:
                    S.set(x, y, z, WATER)
                    n_water += 1
                elif in_vijver:
                    S.set(x, y, z, BED)
                elif dh < 5.2:
                    S.set(x, y, z, self.rnd.choice(["minecraft:tuff", "minecraft:mossy_cobblestone", "minecraft:andesite"]))
                    n_rots += 1
        self.log(f"    kriebelhuis: water round the tube {n_water}, rock against the glass {n_rots}")

    # ==================================================================================================================
    def uitgang(self):
        S = self.S
        UX1, UZ1, UX2, UZ2 = UITGANG
        TY = self.TY
        S.fill(UX1, G - 2, UZ1, UX2, G - 1, UZ2, "minecraft:dirt")
        self.box(UX1, G + 1, UZ1, UX2, G + 7, UZ2, "minecraft:white_terracotta", floor="minecraft:birch_planks")
        self.windows(UX1, G + 3, UZ1, UX2, UZ2, every=3)
        for i in range(G - TY + 2):                                                    # stairs up out of the tube (south)
            z, yf = 84 + i, TY + i
            binnen = z > UZ1
            if not binnen:
                S.fill(68, yf - 1, z, 72, yf + 4, z, PINKG)
            for x in (69, 70, 71):
                if yf >= G:
                    S.set(x, G, z, "minecraft:birch_planks")
                    S.air(x, G + 1, z, x, G + 4, z)
                else:
                    S.set(x, yf, z, "minecraft:smooth_quartz" if i == 0 else
                          "minecraft:quartz_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
                    S.air(x, yf + 1, z, x, yf + 3, z)
                    if binnen:
                        S.air(x, yf + 1, z, x, G + 4, z)
                self.paden.add((x, z))
            if binnen and yf < G:
                for x in (68, 72):
                    S.fill(x, yf, z, x, G, z, "minecraft:birch_planks")
                    S.set(x, G + 1, z, "minecraft:spruce_fence")
        S.air(UX2, G + 1, 98, UX2, G + 2, 99)
        self.door2(UX2, G + 1, 98, "east")
        self.door2(UX2, G + 1, 99, "east")
        S.set(UX2, G + 2, 99, "guhs:roze_lakhout_deur[facing=east,half=upper,hinge=right,open=false,powered=false]")
        S.set(UX2, G + 1, 99, "guhs:roze_lakhout_deur[facing=east,half=lower,hinge=right,open=false,powered=false]")
        S.air(UX1, G + 1, 100, UX1, G + 2, 101)
        self.door2(UX1, G + 1, 100, "west")
        self.door2(UX1, G + 1, 101, "west")
        S.set(UX1, G + 2, 101, "guhs:roze_lakhout_deur[facing=west,half=upper,hinge=right,open=false,powered=false]")
        S.set(UX1, G + 1, 101, "guhs:roze_lakhout_deur[facing=west,half=lower,hinge=right,open=false,powered=false]")
        for x in range(63, 84, 4):                                                     # the gift shop
            if 67 <= x <= 73:
                continue
            S.set(x, G + 1, 106, "guhs:guh_kast[facing=north,open=false]")
            S.set(x, G + 2, 106, "guhs:knuffel_choco[facing=north]")
        for x, k in ((76, "guhs:knuffel_choco[facing=north]"),):
            S.set(x, G + 1, 104, "minecraft:spruce_planks")
            S.set(x, G + 2, 104, k)
        S.set(80, G + 1, 96, "guhs:guh_tafel[facing=north]")
        S.set(78, G + 1, 96, "guhs:guh_stoel[facing=east]")
        S.set(63, G + 1, 92, "guhs:guh_bloempot")
        S.set(83, G + 1, 92, "guhs:guh_bloempot")
        S.set(76, G + 7, 98, "guhs:glimtegel[fel=true]")
        S.set(64, G + 7, 100, "guhs:glimtegel[fel=true]")
        S.set(80, G + 7, 104, "guhs:glimtegel[fel=true]")
        self.npc_winkel = (78.5, G + 1, 102.5, 0.0)
        S.fill(UX1 + 1, G + 8, UZ1 + 1, UX2 - 1, G + 8, UZ2 - 1, "minecraft:birch_planks")     # ceiling
        self.thatch(UX1, UZ1, UX2, UZ2, G + 8, overhang=1)
        self.sectiebord(70, G + 3, UZ1 + 1, "south", ["", "Uitgang", "+ cadeautjes", ""])
        self.winkel_lief()

        self.doel("cadeauwinkeltje", 76, G + 1, 100)

    def winkel_lief(self):
        """Round 5: the gift shop was dull and dark. Heart tiles on the floor, lampions under the ceiling, plush shelves
        and plush blocks, a cushion corner, cloud lamps and a welcome mat at both doors."""
        S = self.S
        UX1, UZ1, UX2, UZ2 = UITGANG
        for x in range(UX1 + 1, UX2):
            for z in range(UZ1 + 1, UZ2):
                if S.get(x, G, z).startswith("minecraft:birch_planks") and S.leeg(x, G + 1, z):
                    S.set(x, G, z, "guhs:tegel_hartjes" if (x + z) % 2 else "guhs:pleister_wolkje")
        self.lampions(UX1 + 2, UZ1 + 2, UX2 - 2, UZ2 - 2, G + 7, stap=3)
        for (x, z) in ((UX1 + 1, UZ1 + 1), (UX2 - 1, UZ1 + 1), (UX1 + 1, UZ2 - 1), (UX2 - 1, UZ2 - 1)):
            if S.leeg(x, G + 1, z):
                S.set(x, G + 1, z, "guhs:wolkenlamp")
        knuffels = ["knuffel_normal", "knuffel_mint", "knuffel_snow", "knuffel_pinguh", "knuffel_wolk", "knuffel_choco"]
        for i, z in enumerate(range(UZ1 + 3, UZ2 - 2, 2)):                            # plush shelves along the west wall
            x = UX1 + 1
            if not (S.leeg(x, G + 1, z) and S.leeg(x, G + 2, z)) or 99 <= z <= 102:
                continue
            S.set(x, G + 1, z, "minecraft:birch_planks")
            S.set(x, G + 2, z, f"guhs:{knuffels[i % len(knuffels)]}[facing=east]")
        for (x, z, k) in ((74, 94, "knuffelblok_roze"), (75, 94, "knuffelblok_hemelsblauw"), (74, 95, "knuffelblok_boter"),
                          (82, 94, "knuffelblok_lavendel"), (82, 95, "knuffelblok_aqua")):
            if S.leeg(x, G + 1, z):
                S.set(x, G + 1, z, f"guhs:{k}")
        S.set(74, G + 2, 94, "guhs:knuffel_pinguh[facing=north]")
        for (x, z, f) in ((64, 96, "east"), (64, 97, "east"), (65, 95, "south")):
            if S.leeg(x, G + 1, z):
                S.set(x, G + 1, z, f"guhs:pink_kussen[facing={f}]")
        for x in range(76, 81):                                                        # the counter + lamps
            if S.leeg(x, G + 1, 101):
                S.set(x, G + 1, 101, "minecraft:spruce_planks")
        S.set(76, G + 2, 101, "guhs:doolhof_lantaarn[lit=true]")
        S.set(80, G + 2, 101, "guhs:knuffel_starry[facing=south]")
        if S.leeg(UX2 - 1, G + 1, 98):
            S.set(UX2 - 1, G + 1, 98, "guhs:deurmat_welkom[facing=east]")
        if S.leeg(UX1 + 1, G + 1, 100):
            S.set(UX1 + 1, G + 1, 100, "guhs:deurmat_welkom[facing=west]")

    def aai_lagune(self):
        S = self.S
        AGX1, AGZ1, AGX2, AGZ2 = AAI
        UX1 = UITGANG[0]
        lag = set()
        for x in range(AGX1, AGX2 + 1):
            for z in range(AGZ1, AGZ2 + 1):
                if math.hypot((x - 40) / 16.0, (z - 102) / 10.0) < 1:
                    lag.add((x, z))
                    S.set(x, G - 3, z, "minecraft:sand")
                    S.fill(x, G - 2, z, x, G - 1, z, WATER)
                    S.set(x, G, z, WATER)
                    S.air(x, G + 1, z, x, G + 3, z)
        for (x, z) in lag:
            for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + a, z + b) not in lag:
                    S.set(x + a, G, z + b, "minecraft:sand")
                    for y in (G - 1, G - 2, G - 3):
                        if S.leeg(x + a, y, z + b):
                            S.set(x + a, y, z + b, "minecraft:sand")
        for x in range(AGX1, UX1):                                                    # boardwalk (west -> east)
            for z in (101, 102, 103):
                S.set(x, G, z, GLASVLOER if (x // 3) % 2 == 0 and (x, z) in lag else "minecraft:spruce_planks")
                S.air(x, G + 1, z, x, G + 3, z)
                self.paden.add((x, z))
            for z in (100, 104):
                if (x, z) in lag and x % 2 == 0:
                    S.set(x, G, z, "minecraft:spruce_planks")
                    S.set(x, G + 1, z, "minecraft:spruce_fence")
        for (x, z, v) in ((32, 98, "knabbelkoi_roze"), (44, 106, "knabbelkoi_blauw"), (38, 99, "knabbelkoi_goud"),
                          (48, 98, "knabbelkoi_roodwit")):
            self.critter("guhs:knabbelkoi", v, "aai-lagune", x + 0.5, G - 1, z + 0.5, straal=5, water=True)
        for (x, z, v) in ((26, 104, "kikkerguh_roze"), (54, 98, "kikkerguh_mint"), (40, 111, "kikkerguh_geel")):
            S.set(x, G + 1, z, "minecraft:lily_pad")
            self.critter("guhs:kikkerguh", v, "aai-lagune", x + 0.5, G + 1, z + 0.5, straal=4)
        self.lantaarnpaal(UX1 - 2, 99)
        self.lantaarnpaal(UX1 - 2, 105)
        self.doel("aai-lagune vlonder", 40, G + 1, 102)
        self.kijk.append(("aai-lagune glasvloer", (36, G - 1, 102), (40, G + 1, 102)))

    # ==================================================================================================================
    def checks(self):
        """Reachability (walk from the gate, player 2 high, steps <= 1, doors open) to every stand spot, and a clear
        line of sight from every stand spot (eye height) to its glass."""
        S = self.S
        x0, x1 = 0, 186
        z0, z1 = 0, 116
        y0, y1 = self.TY - 4, G + 12
        nx, ny, nz = x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1
        P = np.zeros((nx, ny, nz), bool)
        St = np.zeros((nx, ny, nz), bool)
        cache = {}
        pas_k = ("short_grass", "fern", "poppy", "allium", "daisy", "cornflower", "guhbloem", "_door", "carpet",
                 "lantern", "lampion", "sign", "glow_lichen", "glimpaddenstoel", "dead_bush", "kelp", "seagrass",
                 "torch", "flower", "schorsplaat", "nepkoraal", "_deur")
        niet_staan = ("water", "fence", "wall", "pane", "leaves", "short_grass", "fern", "flower", "poppy", "allium",
                      "daisy", "cornflower", "guhbloem", "lilac", "_door", "_deur", "lantern", "lampion", "carpet", "glow_lichen",
                      "glimpaddenstoel", "dead_bush", "kelp", "seagrass", "sign", "lily_pad", "nepkoraal", "infobord")
        for x in range(nx):
            for z in range(nz):
                for y in range(ny):
                    n = S.get(x + x0, y + y0, z + z0)
                    if n not in cache:
                        b = n.split("[")[0]
                        lucht = b == "minecraft:air"
                        cache[n] = (lucht or (any(k in b for k in pas_k) and "leaves" not in b),
                                    not lucht and not any(k in b for k in niet_staan))
                    P[x, y, z], St[x, y, z] = cache[n]
        seen = np.zeros(P.shape, bool)
        sx, sy, sz = self.startpunt
        start = (sx - x0, sy - y0, sz - z0)
        seen[start] = True
        q = deque([start])
        while q:
            x, y, z = q.popleft()
            for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ax, az = x + a, z + b
                if not (0 <= ax < nx and 0 <= az < nz):
                    continue
                for dy in (0, 1, -1):
                    ay = y + dy
                    if not (1 <= ay < ny - 2) or seen[ax, ay, az]:
                        continue
                    if dy == 1 and not P[x, y + 2, z]:
                        continue
                    if dy == -1 and not P[ax, ay + 2, az]:
                        continue
                    if St[ax, ay - 1, az] and P[ax, ay, az] and P[ax, ay + 1, az]:
                        seen[ax, ay, az] = True
                        q.append((ax, ay, az))
                        break
        fout_b = []
        for (n, (x, y, z)) in self.doelen:
            ok = any(seen[x - x0 + a, y - y0 + dy, z - z0 + b] for a in (-1, 0, 1) for b in (-1, 0, 1) for dy in (0, 1, -1)
                     if 0 <= x - x0 + a < nx and 0 <= z - z0 + b < nz and 0 <= y - y0 + dy < ny)
            if not ok:
                fout_b.append(f"{n} {S.w(x, y, z)}")
        fout_z = []
        for (n, g, st) in self.kijk:
            ox, oy, oz = st[0] + 0.5, st[1] + 1.6, st[2] + 0.5
            tx, ty, tz = g
            m = int(max(abs(tx - ox), abs(ty - oy), abs(tz - oz)) * 4) + 1
            for i in range(1, m):
                t = i / m
                x, y, z = ox + (tx - ox) * t, oy + (ty - oy) * t, oz + (tz - oz) * t
                if math.hypot(x - tx, z - tz) < 0.9 and abs(y - ty) < 2.6:
                    break
                nm = S.get(math.floor(x), math.floor(y), math.floor(z))
                if nm == "minecraft:air" or "glass" in nm or "glas" in nm or any(
                        k in nm for k in ("lantern", "lampion", "short_grass", "fern", "flower", "fungus", "sign",
                                          "lily_pad", "water", "infobord", "glimpaddenstoel", "seagrass", "kelp")):
                    continue
                fout_z.append(f"{n}: {nm} op {S.w(math.floor(x), math.floor(y), math.floor(z))}")
                break
        return fout_b, fout_z


def oorsprong(park):
    lob = getattr(park, "lobby", None)
    if isinstance(lob, dict) and lob.get("oorsprong"):
        return lob["oorsprong"], lob.get("g", G), lob.get("draai", 0), True
    try:
        from . import zone_entree as E
        return (E.OX, E.OY + E.G, E.OZ), G, 0, False
    except Exception:
        return (20, 64, 530), G, 0, False


def bouw(park, log=print):
    o, g, draai, echt = oorsprong(park)
    if not echt:
        log(f"    kriebelhuis: no park.lobby from zone_entree, fallback origin {o}")
    L = Lokaal(park, o, g, draai)
    b = Bouwer(park, L, log)
    b.entreehal()
    b.terrarium_zaal()
    b.kriebelzaal()
    b.nachtzaal()
    b.diepzeezaal()
    b.vijver_en_buis()
    b.uitgang()
    b.aai_lagune()
    # the whole plot is the lobby (entree) zone for zones.json / the homes
    zi = ZONES.index("entree")
    for (x1, z1, x2, z2) in (HAL, TERRARIUM, KRIEBELZAAL, NACHTZAAL, DIEPZEE, UITGANG, AAI, AQUARIUM_BUITEN):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                wx, _, wz = L.w(x, G, z)
                if 0 <= wx < kit.W and 0 <= wz < kit.W:
                    park.zone[wx, wz] = zi
                    if getattr(park, "zone_vol", None) is not None:
                        park.zone_vol[wx, wz] = zi
                    if (x, z) in b.paden and park.pad is not None:
                        park.pad[wx, wz] = True
                    if (x1, z1) not in (AQUARIUM_BUITEN[:2], AAI[:2]) and park.water is not None:
                        park.water[wx, wz] = False          # indoor: the home fix-up never moves our animals "out of water"
    for (pos, soort) in b.borden:
        wx, wy, wz = L.w(*pos)
        if hasattr(park, "infobord_nbt"):
            park.infobord_nbt(wx, wy, wz, soort)
        else:
            if not hasattr(park, "block_nbt"):
                park.block_nbt = {}
            park.block_nbt[(wx, wy, wz)] = kit._str_tag("Soort", soort)
    npcs = getattr(park, "lobby_npcs_extra", [])
    for (id_, kind, p) in (("kriebelhuis_balie", "safari_kriebelhuis_balie", b.npc_balie),
                           ("kriebelhuis_winkel", "safari_kriebelhuis_winkel", b.npc_winkel)):
        npcs.append(dict(id=id_, kind=kind, pos=L.wf(p[0], p[1], p[2]), yaw=float((p[3] + 90 * L.k) % 360)))
    park.lobby_npcs_extra = npcs
    fout_b, fout_z = b.checks()
    log(f"    kriebelhuis: bereikbaar {len(b.doelen) - len(fout_b)}/{len(b.doelen)}, "
        f"zicht {len(b.kijk) - len(fout_z)}/{len(b.kijk)}; {len(b.huizen)} huizen, {len(b.critters)} critters, "
        f"{len(b.borden)} infoborden")
    for f in fout_b[:20]:
        log("      niet bereikbaar: " + f)
    for f in fout_z[:20]:
        log("      geen zicht: " + f)
    park.kriebelhuis = {"bouwer": b, "L": L, "fout": (fout_b, fout_z)}


def plekken(park, plekken_):
    """The Kriebelhuis homes (no clones: zone entree) + kriebelhuis.json (sections, critters, infoborden)."""
    kh = getattr(park, "kriebelhuis", None)
    if not kh:
        return
    b, L = kh["bouwer"], kh["L"]
    from . import plekken as plk
    plk.WATERDIER.update(h["soort"] for h in b.huizen if h["water"])     # aquarium homes stay in their water
    for h in b.huizen:
        x, y, z = L.w(*h["pos"])
        plekken_.append({"soort": h["soort"], "zone": "entree", "centrum": [x, y, z], "straal": h["straal"],
                         "volwassenen": h["volwassenen"], "jongen": h["jongen"], "water": h["water"], "boom": h["boom"],
                         "afdeling": h["afdeling"]})

    def doos(t, y1=G - 12, y2=G + 20):
        a, b_, c = L.w(t[0], y1, t[1])
        d, e, f = L.w(t[2], y2, t[3])
        return [min(a, d), b_, min(c, f), max(a, d), e, max(c, f)]
    data = {
        "uitleg": "Het Kriebelhuis (zone_kriebelhuis.py): secties (x1 y1 z1 x2 y2 z2), the guhs critters to spawn "
                  "(entity, variant, pos, straal, water) and the infoborden (pos, soort)",
        "secties": {"entreehal": doos(HAL), "terrarium": doos(TERRARIUM), "kriebelzaal": doos(KRIEBELZAAL),
                    "nachtzaal": doos(NACHTZAAL), "diepzeezaal": doos(DIEPZEE), "aquarium_buiten": doos(AQUARIUM_BUITEN),
                    "uitgang": doos(UITGANG), "aai_lagune": doos(AAI)},
        "ingang": L.w(MIDX, G + 1, 99),
        "critters": [dict(c, pos=L.wf(*c["pos"])) for c in b.critters],
        "infoborden": [{"pos": list(L.w(*p)), "soort": s} for (p, s) in b.borden],
    }
    pad = os.path.join(kit.OUT_DATA, "kriebelhuis.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
