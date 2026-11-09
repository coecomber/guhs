"""The hand-made set pieces of zone 1, in world coordinates, as stamps/points for the tile writer.

bouw_alles(W, K, kamp_d) builds everything and returns the POI dict (merged into _cache/plekken.json).
Every builder works on a WS (a vibekit Scene with a world offset), so the render helpers (fill, cylinder, ellipsoid,
zwamzoet.huis/gezicht/oortje/guhbeeld) work in world coordinates.
"""
import math
import random

import numpy as np

import kaart as KA
import stadkit as SK
import tegels as TG
import zwamzoet as Z
import os as _os
import sys as _sys
_sys.path.insert(0, _os.path.join(_os.path.dirname(_os.path.abspath(__file__)), ".."))
import indeling as IND  # noqa: E402  (T-WERELD: zone offsets)

KDX, KDZ = IND.KAMP_DX, IND.KAMP_DZ   # Knuffelkamp pieces are drawn in the old (local) camp coordinates, then shifted


def kamp_stempel(S, naam):
    """T-WERELD: a stamp drawn in local camp coordinates, moved to the camp's world place."""
    st = S.stempel(naam)
    st.off = (st.off[0] + KDX, st.off[1], st.off[2] + KDZ)
    return st


def kamp_p(v):
    return [v[0] + KDX, v[1], v[2] + KDZ]


def kamp_meng(x, z):
    """T-WERELD: line 0 (Knuffelkamp <-> De Haven) bends west with the camp: full camp shift at z >= 860, none at z <= 500.
    Same formula as VadsrijkZones.kampMeng (WereldReizen's route)."""
    m = min(max((z - 500) / 360.0, 0.0), 1.0)
    return int(round(x + KDX * m)), z

ST = SK.stairs
SLAB = SK.slab
LANT = "minecraft:lantern[hanging=false,waterlogged=false]"
LANT_H = "minecraft:lantern[hanging=true,waterlogged=false]"
VUUR = "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]"
ROZE, GLAD, PASTEL, KSTEEN, FRAMB = "roze_knuffelsteen", "gladde_knuffelsteen", "pastelbaksteen_roze", "knuffelsteen", \
    "pastelbaksteen_framboos"
HOED_ROZE = "guhpaddenstoel_hoed"
MUSH = SK.MUSH
ROOD, BRUIN, STEEL = SK.ROOD, SK.BRUIN, SK.STEEL
CARVE = "minecraft:cave_air"


class WS(SK.Scene):
    """A Scene whose coordinates are world coordinates (offset o)."""

    def __init__(self, x0, y0, z0, X, Y, Z_):
        super().__init__(X, Y, Z_)
        self.o = (int(x0), int(y0), int(z0))

    def _l(self, x, y, z):
        return math.floor(x) - self.o[0], math.floor(y) - self.o[1], math.floor(z) - self.o[2]

    def set(self, x, y, z, s, roof=False, keep=False):
        lx, ly, lz = self._l(x, y, z)
        super().set(lx, ly, lz, s, roof, keep)

    def get(self, x, y, z):
        lx, ly, lz = self._l(x, y, z)
        return super().get(lx, ly, lz)

    def is_air(self, x, y, z):
        return self.get(x, y, z) == "minecraft:air"

    def stempel(self, naam):
        return TG.Stempel(naam, self.v, self.pal, self.o)


def gezicht_vlak(S, x0, y0, z, w, h, richting="south"):
    """Guh face (zwamzoet.gezicht) on a vertical plane facing south/north (x0 = left as the viewer sees it)."""
    def put(u, v, b):
        x = x0 + u if richting == "south" else x0 - u
        S.set(x, y0 + v, z, b)
    Z.gezicht(put, w, h)


# ================================================================================================ Zwamtempel
TP = KA.TEMPEL_PLATEAU          # floor block y (stand = TP + 1)
TY = TP + 1


def zwamtempel(W, K, P):
    S = WS(262, TP - 16, -535, 150, 114, 175)      # z to -361: the front stairs + landing (fix round 1)
    # ---- terrace + front stairs (the path arrives at z -386)
    S.fill(300, TP, -394, 360, TP, -376, "tegel_hartjes")
    for i in range(7):
        # fix round 1: a real staircase (solid under it, free above it) so it never ends buried in the hill
        S.fill(316, TP - 14, -370 - i, 344, TP - 7 + i, -370 - i, KSTEEN)
        S.fill(318, TP - 6 + i, -370 - i, 342, TP - 6 + i, -370 - i, ST("guhs:knuffelsteen_trap", "north"))
        S.fill(318, TP - 5 + i, -370 - i, 342, TP + 1, -370 - i, CARVE)
        for x in (316, 317, 343, 344):      # side walls of the staircase
            S.fill(x, TP - 6 + i, -370 - i, x, TP - 5 + i, -370 - i, KSTEEN)
    S.fill(316, TP - 14, -369, 344, TP - 7, -365, KSTEEN)          # a landing at the foot of the stairs (path end)
    S.fill(316, TP - 7, -369, 344, TP - 7, -365, "tegel_hartjes")
    S.fill(316, TP - 6, -369, 344, TP - 2, -365, CARVE)
    # ---- the inside: a straight chain north (bazen slice's layout): voorhal -> Lichtzwamkamer -> Hendelkamer -> Ridderzaal
    #      -> the Opperzwam hall. The gates between them are open AIR (5 wide, 4 high); bazen fills them with its own gate blocks.
    def doos(x1, z1, x2, z2, h, wand=ROZE, dak=FRAMB):
        marmer(S, x1, z1, x2, z2, TP)
        S.walls(x1, TY, z1, x2, TY + h, z2, wand)
        for x in range(x1, x2 + 1, 4):
            S.fill(x, TY, z1, x, TY + h, z1, GLAD)
            S.fill(x, TY, z2, x, TY + h, z2, GLAD)
        for z in range(z1, z2 + 1, 4):
            S.fill(x1, TY, z, x1, TY + h, z, GLAD)
            S.fill(x2, TY, z, x2, TY + h, z, GLAD)
        S.fill(x1 - 1, TY + h + 1, z1 - 1, x2 + 1, TY + h + 1, z2 + 1, "minecraft:gold_block")
        S.fill(x1, TY + h + 1, z1, x2, TY + h + 1, z2, dak)
        S.fill(x1 + 1, TY, z1 + 1, x2 - 1, TY + h, z2 - 1, CARVE)
        rng = random.Random(x1 * 7 + z1)
        for _ in range(int((x2 - x1 + z2 - z1) * h * 0.12)):
            if rng.random() < 0.5:
                x, z = rng.choice((x1, x2)), rng.randint(z1, z2)
            else:
                x, z = rng.randint(x1, x2), rng.choice((z1, z2))
            S.set(x, TY + rng.randint(0, h), z, rng.choice(["minecraft:mossy_stone_bricks", "minecraft:moss_block", "minecraft:cracked_stone_bricks"]))
        # lanterns high on the walls, mushrooms in the corners
        for (x, z) in ((x1 + 1, z1 + 1), (x2 - 1, z1 + 1), (x1 + 1, z2 - 1), (x2 - 1, z2 - 1)):
            S.set(x, TY, z, "schimmelpaddenstoel")
        for z in range(z1 + 3, z2 - 1, 5):
            S.set(x1 + 1, TY + h - 2, z, LANT)
            S.set(x2 - 1, TY + h - 2, z, LANT)
        # light: glowing tiles in the floor (a 6-grid) and lanterns at eye height along the walls
        for x in range(x1 + 3, x2 - 1, 6):
            for z in range(z1 + 3, z2 - 1, 6):
                S.set(x, TP, z, "minecraft:pearlescent_froglight[axis=y]")
        for z in range(z1 + 2, z2 - 1, 4):
            S.set(x1 + 1, TY + 3, z, "minecraft:shroomlight")
            S.set(x2 - 1, TY + 3, z, "minecraft:shroomlight")

    def poort(z, breed=5, hoog=4):
        x0 = 330 - breed // 2
        S.fill(x0, TY, z, x0 + breed - 1, TY + hoog - 1, z, CARVE)
        S.fill(x0 - 1, TY + hoog, z, x0 + breed, TY + hoog, z, "minecraft:gold_block")
        return [x0, TY, z]
    doos(313, -405, 347, -393, 18)                 # voorhal (inside x 314..346, z -404..-394)
    doos(317, -426, 343, -405, 10)                 # room 1 Lichtzwamkamer (inside x 318..342, z -425..-406)
    doos(317, -447, 343, -426, 10)                 # room 2 Hendelkamer (inside z -446..-427)
    doos(313, -473, 347, -447, 12)                 # room 3 Ridderzaal (inside x 314..346, z -472..-448)
    poorten = [poort(-405), poort(-426), poort(-447)]
    for (x, z) in ((318, -400), (342, -400), (318, -398), (342, -398)):
        S.fill(x, TY, z, x, TY + 17, z, "pastelbaksteen_roze_muur")
        S.set(x, TY + 11, z, "minecraft:shroomlight")
    gezicht_vlak(S, 324, TY + 8, -404, 13, 9, "south")   # face mural over the first gate

    # ---- facade (z -393 .. -391) with the big door (wings x 324..336, y TY..TY+23, z -392)
    S.fill(300, TY, -393, 360, TY + 32, -391, ROZE)
    for x in range(300, 361, 5):
        S.fill(x, TY, -390, x, TY + 30, -390, GLAD)
    for i in range(8):  # stepped gable
        S.fill(306 + i * 3, TY + 33 + i, -393, 354 - i * 3, TY + 33 + i, -391, PASTEL if i % 2 else ROZE)
    S.fill(300, TY + 32, -390, 360, TY + 32, -390, "minecraft:gold_block")
    dw, dh = 13, 24
    for u in range(-2, dw + 2):
        for v in range(0, dh + 3):
            uu = u + 0.5 - dw / 2
            vb = dh - dw / 2
            binnen = (0 <= u < dw) if v < vb else math.hypot(uu, v + 0.5 - vb) <= dw / 2
            rand = (not binnen) and ((-2 <= u < dw + 2) if v < vb else math.hypot(uu, v + 0.5 - vb) <= dw / 2 + 2)
            x = 324 + u
            if binnen:
                # fix round 1: the inner slice (z -393) stays shut above the voorhal roof (y TY + 19), so nobody can
                # drop from the roof through the arch into the closed temple
                S.fill(x, TY + v, -393 if v < 19 else -392, x, TY + v, -391, CARVE)
                mid = u == dw // 2
                b = "minecraft:gold_block" if (mid or u in (0, dw - 1) or v == 0) else \
                    ("minecraft:pink_concrete" if v % 6 else "roze_lakhout_planken")
                S.set(x, TY + v, -392, b)
            elif rand:
                S.fill(x, TY + v, -390, x, TY + v, -390, "minecraft:gold_block" if (u + v) % 4 == 0 else FRAMB)

    def put(u, v, b):
        x = 324 + u
        y = TY + 7 + v
        if S.get(x, y, -392) not in ("minecraft:air", "minecraft:cave_air"):
            S.set(x, y, -392, b)
    Z.gezicht(put, dw, 12)
    P["deur_zwamtempel"] = dict(min=[324, TY, -392], max=[336, TY + dh - 1, -392], as_="x", voor=[330, TY, -388])
    # facade towers with mushroom caps
    for tx in (304, 356):
        S.cylinder(tx, -392, 6, TY, TY + 44, KSTEEN, hollow=1.5)
        S.cylinder(tx, -392, 4.5, TY, TY + 44, CARVE)
        for y in range(TY + 8, TY + 40, 9):
            for k in range(4):
                a = k * math.pi / 2 + 0.4
                S.set(tx + 5.7 * math.cos(a), y, -392 + 5.7 * math.sin(a), "glasinlood_guh")
        hoed(S, tx, -392, TY + 52, 10, HOED_ROZE)
    # big face + ears on the gable
    gezicht_vlak(S, 321, TY + 26, -390, 19, 11, "south")
    for kant in (-1, 1):
        Z.oortje(S, 330 + kant * 8, TY + 41, -392, "x", groot=2)
    # guardian statues + spore braziers on the terrace
    for kant in (-1, 1):
        S.fill(330 + kant * 13 - 1, TY, -386, 330 + kant * 13 + 1, TY + 2, -384, FRAMB)
        Z.guhbeeld(S, 330 + kant * 13, TY + 3, -385, 5)
        S.set(330 + kant * 9, TY, -384, "minecraft:gold_block")
        S.set(330 + kant * 9, TY + 1, -384, "minecraft:shroomlight")
    S.set(318, TY, -381, VUUR)
    P["kampvuur_zwamtempel_trap"] = [318, TY, -381]
    # ---- room 1: five free floor spots for bazen's light mushrooms; hidden room behind the west wall
    P["lichtzwamkamer"] = dict(centrum=[330, TY, -415], lichtzwammen=[[322, TY, -421], [326, TY, -424], [330, TY, -425], [334, TY, -424], [338, TY, -421]])
    marmer(S, 308, -421, 315, -411, TP)
    S.walls(307, TY, -422, 316, TY + 5, -410, KSTEEN)
    S.fill(308, TY, -421, 315, TY + 4, -411, CARVE)
    S.fill(307, TY + 6, -422, 316, TY + 6, -410, KSTEEN)
    S.fill(317, TY, -417, 317, TY + 2, -415, "minecraft:cracked_stone_bricks")
    S.set(309, TY, -416, "minecraft:chest[facing=east,type=single,waterlogged=false]")
    S.set(309, TY + 2, -413, LANT)
    P["geheim_tempelkamer"] = [311, TY, -416]
    P["geheim_tempelkamer_muur"] = [317, TY, -416]
    # ---- room 2: free floor for bazen's levers + plate, free north-wall spots for hint mushrooms
    P["hendelkamer"] = dict(centrum=[330, TY, -436], hendels=[[322, TY, -441], [326, TY, -441], [334, TY, -441], [338, TY, -441]],
                            plaat=[330, TY, -444], hints=[[338, TY + 5, -446], [334, TY + 5, -446], [326, TY + 5, -446], [322, TY + 5, -446]])
    # ---- room 3: the knights' hall (open floor)
    P["ridderzaal"] = dict(centrum=[330, TY, -460], spawns=[[320, TY, -469], [330, TY, -471], [340, TY, -469], [318, TY, -458], [342, TY, -458]])
    for x in (318, 342):
        for z in (-466, -454):
            S.fill(x, TY, z, x, TY + 11, z, "pastelbaksteen_roze_muur")
            S.set(x, TY + 12, z, "minecraft:shroomlight")
    P["poorten"] = poorten
    # ---- the Opperzwam hall: round, r 22 inside, a pink mushroom-cap dome (the breakable ceiling)
    hc = (330, -497)
    for x in range(hc[0] - 25, hc[0] + 26):
        for z in range(hc[1] - 25, hc[1] + 26):
            d = math.hypot(x - hc[0], z - hc[1])
            if d <= 25.5:
                ring = int(d) % 6
                S.set(x, TP, z, "minecraft:quartz_bricks" if ring == 0 else ("minecraft:pink_concrete" if ring % 2 else "minecraft:quartz_block"))
    S.cylinder(hc[0], hc[1], 25.5, TY, TY + 24, ROZE, hollow=3)
    S.cylinder(hc[0], hc[1], 22.5, TY, TY + 24, CARVE)
    S.fill(327, TY, -476, 333, TY + 4, -472, CARVE)                   # gate 4 (room 3 -> hall), open AIR
    S.fill(326, TY + 5, -476, 334, TY + 5, -472, "minecraft:gold_block")
    poorten.append([328, TY, -474])
    for x in range(hc[0] - 28, hc[0] + 29):
        for z in range(hc[1] - 28, hc[1] + 29):
            d = math.hypot(x - hc[0], z - hc[1])
            if d > 27:
                continue
            t = 1 - (d / 27) ** 2
            top = TY + 25 + int(16 * math.sqrt(max(0, t)))
            for y in range(top - 2, top + 1):
                b = HOED_ROZE
                if y == top and (int(x * 0.37 + z * 0.23) % 7 == 0) and d > 4:
                    b = "knuffelblok_vanille" if (x + z) % 2 else "minecraft:pearlescent_froglight[axis=y]"
                S.set(x, y, z, b)
            if d <= 23:
                for y in range(TY + 25, top - 2):
                    S.set(x, y, z, CARVE)
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        x, z = hc[0] + 20 * math.cos(a), hc[1] + 20 * math.sin(a)
        S.fill(int(x), TY, int(z), int(x), TY + 22, int(z), "pastelbaksteen_roze_muur")
        S.set(int(x), TY + 23, int(z), "minecraft:shroomlight")
        for yy in (TY + 5, TY + 12):
            S.set(int(x), yy, int(z), "minecraft:shroomlight")
    for k in range(24):     # glowing rings in the floor
        a = k * math.pi / 12
        for rr in (11.5, 20.5):
            S.set(hc[0] + rr * math.cos(a), TP, hc[1] + rr * math.sin(a), "minecraft:pearlescent_froglight[axis=y]")
    spots = [[hc[0] - 13, TY, hc[1] + 13], [hc[0] + 13, TY, hc[1] + 13], [hc[0] - 13, TY, hc[1] - 13], [hc[0] + 13, TY, hc[1] - 13]]
    S.fill(hc[0] - 6, TY, hc[1] - 22, hc[0] + 6, TY + 3, hc[1] - 20, "minecraft:mycelium[snowy=false]")
    P["arena_opperzwam"] = dict(centrum=[hc[0], TY, hc[1]], straal=22, deur=[330, TY, -470], bord=[334, TY, -469],
                                lichtzwammen=spots,
                                plafond=dict(centrum=[hc[0], TY + 25, hc[1]], straal=23, van_y=TY + 23, tot_y=TY + 42))
    P["zwamtempel_voorhal"] = [330, TY, -398]
    W.stempel(S.stempel("zwamtempel"))
    P["gebied_zwamtempel_binnen"] = [300, -525, 360, -390]


def marmer(S, x1, z1, x2, z2, y):
    """Temple floor (user choice, fix round 1): a pink/white marble look of quartz and pink concrete."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            k = (x // 2 + z // 2) % 2
            S.set(x, y, z, "minecraft:quartz_block" if k else ("minecraft:pink_concrete" if (x + z) % 5 else "minecraft:quartz_pillar[axis=y]"))


def hoed(S, cx, cz, top, r, blok, vlek="knuffelblok_vanille", lamellen=True):
    """A mushroom cap (dome) with spots and gills, centre top at y=top."""
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            d = math.hypot(x - cx, z - cz)
            if d > r + 0.3:
                continue
            t = d / r
            y1 = top - int(round(r * 0.55 * t ** 1.8))
            y0 = y1 - 2 - int((1 - t) * 1.5)
            for y in range(y0, y1 + 1):
                b = blok
                if y == y1 and int(x * 0.41 + z * 0.29 + y) % 5 == 0 and 2 < d < r - 1:
                    b = vlek
                if y == y0 and lamellen and d > 1.5:
                    b = "minecraft:pink_terracotta" if int(math.atan2(z - cz, x - cx) * r / 1.5) % 2 else STEEL
                S.set(x, y, z, b)
            if lamellen and d > r - 1 and int(math.atan2(z - cz, x - cx) * r) % 7 == 0:
                S.set(x, y0 - 1, z, LANT_H)


# ================================================================================================ De Haven
def luchtschip(S, cx, cz, cy, mast_x=None, richting=1):
    """Pink guh airship along x (copied from zwamzoet.luchtschip, rope to mast_x)."""
    rx, ry, rz = 22, 8, 8
    for x in range(cx - rx, cx + rx + 1):
        for y in range(cy - ry, cy + ry + 1):
            for z in range(cz - rz, cz + rz + 1):
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                if 0.72 < d <= 1:
                    S.set(x, y, z, "minecraft:white_wool" if (x - cx) % 9 == 0 else "minecraft:pink_wool")
    for kant in (-1, 1):
        Z.oortje(S, cx + 12 * richting, cy + ry - 1, cz + kant * 4, "x", groot=2)
    for v in range(0, 8):
        for u in range(0, 8 - v):
            bx = cx - (rx + 1 - u) * richting
            S.set(bx, cy + 3 + v, cz, "minecraft:pink_concrete")
            S.set(bx, cy, cz - 6 - v, "minecraft:pink_concrete")
            S.set(bx, cy, cz + 6 + v, "minecraft:pink_concrete")
    gy = cy - ry - 5
    S.fill(cx - 8, gy, cz - 2, cx + 8, gy + 3, cz + 2, "roze_lakhout_planken")
    S.fill(cx - 7, gy + 1, cz - 2, cx + 7, gy + 2, cz + 2, "glasinlood_guh")
    S.fill(cx - 8, gy + 4, cz - 2, cx + 8, gy + 4, cz + 2, "guh_dakpan_roze")
    for x in (cx - 6, cx + 6):
        for z in (cz - 2, cz + 2):
            S.fill(x, gy + 5, z, x, cy - ry + 1, z, "minecraft:iron_chain[axis=y]")
    px = cx - 10 * richting
    S.fill(px, gy + 1, cz - 2, px, gy + 3, cz + 2, "minecraft:white_wool")
    S.fill(px, gy + 2, cz - 3, px, gy + 2, cz + 3, "minecraft:white_wool")
    if mast_x is not None:
        a, b = sorted((cx + rx * richting, mast_x))
        for x in range(a, b + 1):
            S.set(x, cy, cz, "minecraft:cherry_fence")


def mast(S, cx, cz, g, top):
    """The grand airship tower of De Haven: an octagonal pink hall with four arches, a timber lattice with ring beams and a
    ladder in the middle (one trapdoor at the top), a railed top platform with lanterns, flags, the beacon and guh ears."""
    S.cylinder(cx, cz, 7.3, g - 3, g - 1, KSTEEN)
    S.cylinder(cx, cz, 7.3, g, g, "knuffelklinkers")
    S.cylinder(cx, cz, 5.4, g, g, "tegel_hartjes")
    # the hall: a ring wall with four arched doorways and windows
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            d = math.hypot(x - cx, z - cz)
            if 4.5 < d <= 5.6:
                for y in range(g + 1, g + 9):
                    b = PASTEL if (y - g) % 4 else FRAMB
                    if y in (g + 5, g + 6) and (abs(x - cx) == 2 or abs(z - cz) == 2) and abs(x - cx) + abs(z - cz) > 5:
                        b = "glasinlood_guh"
                    S.set(x, y, z, b)
    for (ux, uz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for w in (-1, 0, 1):
            for y in range(g + 1, g + 5):
                for r in (4, 5, 6):
                    S.set(cx + ux * r + uz * w, y, cz + uz * r + ux * w, CARVE)
        S.set(cx + ux * 5 + uz * 0, g + 5, cz + uz * 5, "glasinlood_hartje")
        for kant in (-2, 2):
            S.set(cx + ux * 6 + uz * kant, g + 1, cz + uz * 6 + ux * kant, LANT)
    S.cylinder(cx, cz, 6.2, g + 9, g + 9, GLAD)
    S.cylinder(cx, cz, 6.2, g + 10, g + 10, "guh_dakpan_roze")
    S.cylinder(cx, cz, 4.6, g + 10, g + 10, GLAD)
    # the core (3x3) with the ladder on its south face
    S.fill(cx - 1, g + 1, cz - 1, cx + 1, top - 1, cz + 1, KSTEEN)
    for y in range(g + 11, top):
        for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
            S.set(cx + dx, y, cz + dz, "minecraft:stripped_cherry_log[axis=y]")
        if (y - g) % 8 == 0:
            S.walls(cx - 3, y, cz - 3, cx + 3, y, cz + 3, "minecraft:cherry_planks")
            for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
                S.set(cx + dx + (1 if dx > 0 else -1), y, cz + dz + (1 if dz > 0 else -1), "minecraft:cherry_planks")
                S.set(cx + dx + (1 if dx > 0 else -1), y - 1, cz + dz + (1 if dz > 0 else -1), LANT_H)
        if (y - g) % 8 == 4:
            for (dx, dz) in ((-2, -3), (2, -3), (-2, 3), (2, 3), (-3, -2), (3, -2), (-3, 2), (3, 2)):
                S.set(cx + dx, y, cz + dz, "minecraft:cherry_fence")
    for y in range(g + 1, top):
        S.set(cx, y, cz + 2, "minecraft:ladder[facing=south,waterlogged=false]")
    for y in range(g + 1, g + 4):
        S.set(cx, y, cz + 3, CARVE)
    S.cylinder(cx, cz, 5.3, top, top, "minecraft:cherry_planks")
    S.cylinder(cx, cz, 5.3, top - 1, top - 1, "minecraft:stripped_cherry_log[axis=y]", hollow=1)
    S.set(cx, top, cz + 2, "minecraft:cherry_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]")
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            if 4.6 < math.hypot(x - cx, z - cz) <= 5.3:
                S.set(x, top + 1, z, "minecraft:cherry_fence")
    for (dx, dz) in ((-4, -2), (4, -2), (-4, 2), (4, 2), (-2, 4), (2, 4), (-2, -4), (2, -4)):
        S.set(cx + dx, top + 2, cz + dz, LANT)
    for (dx, dz) in ((-3, 3), (3, 3)):
        guhvlag(S, cx + dx, top + 1, cz + dz, "x", 7)
    S.fill(cx, top + 1, cz - 3, cx, top + 8, cz - 3, "minecraft:cherry_log[axis=y]")
    S.set(cx, top + 9, cz - 3, "minecraft:shroomlight")
    Z.oortje(S, cx, top + 10, cz - 3, "z", groot=2)


def guhvlag(S, x, y, z, as_="x", h=6):
    """A guh flag: a cherry pole with a pink cloth (white rim, two dark eyes and a little blush) at the top."""
    S.fill(x, y, z, x, y + h - 1, z, "minecraft:cherry_fence")
    for u in range(1, 5):
        for v in range(0, 3):
            b = "minecraft:pink_wool"
            if v == 2 or u == 4:
                b = "minecraft:white_wool"
            if v == 1 and u in (1, 3):
                b = "minecraft:black_wool"
            pos = (x + u, y + h - 1 - v, z) if as_ == "x" else (x, y + h - 1 - v, z + u)
            S.set(*pos, b)


def kraan(S, x, z, g, uit=-1):
    """A wooden harbour crane on the inner edge of the quay: post, boom out over the water, chain and a hanging crate."""
    S.fill(x - 1, g + 1, z - 1, x + 1, g + 1, z + 1, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
    S.fill(x, g + 1, z, x, g + 11, z, "minecraft:spruce_log[axis=y]")
    for i in range(-2, 9):
        S.set(x + uit * i, g + 12, z, "minecraft:stripped_spruce_log[axis=x]")
    for i in range(1, 4):
        S.set(x + uit * i, g + 12 - i, z, "minecraft:spruce_fence")
    S.set(x - uit * 2, g + 11, z, "minecraft:hay_block[axis=y]")
    kx = x + uit * 7
    S.fill(kx, g + 6, z, kx, g + 11, z, "minecraft:iron_chain[axis=y]")
    S.set(kx, g + 5, z, "minecraft:barrel[facing=up,open=false]")
    S.set(x + uit * 8, g + 13, z, LANT)


def kratten(S, x, z, g, rng):
    """A stack of crates and barrels (2x2, one or two on top)."""
    soorten = ["minecraft:barrel[facing=up,open=false]", "minecraft:spruce_planks", "minecraft:hay_block[axis=y]",
               "minecraft:stripped_spruce_wood[axis=y]"]
    for dx in (0, 1):
        for dz in (0, 1):
            S.set(x + dx, g + 1, z + dz, rng.choice(soorten))
    S.set(x, g + 2, z, rng.choice(soorten))
    if rng.random() < 0.5:
        S.set(x + 1, g + 2, z + 1, rng.choice(soorten))


def loper(S, x0, z0, w, d, voor, y, kleur="minecraft:pink_carpet"):
    """A carpet runner from the door of a zwamzoet.huis (planks floor) to the back wall."""
    if voor in ("west", "east"):
        mid = z0 + (d - 1) // 2
        for x in range(x0 + 1, x0 + w - 1):
            S.set(x, y, mid, kleur)
    else:
        mid = x0 + (w - 1) // 2
        for z in range(z0 + 1, z0 + d - 1):
            S.set(mid, y, z, kleur)


def kei(S, x, y, z, r, rng):
    """A mossy boulder."""
    blokken = ["minecraft:stone", "minecraft:andesite", "minecraft:mossy_cobblestone", "minecraft:cobblestone", "minecraft:tuff"]
    for xx in range(int(x - r - 1), int(x + r + 2)):
        for yy in range(int(y - r), int(y + r + 1)):
            for zz in range(int(z - r - 1), int(z + r + 2)):
                d = ((xx - x) / r) ** 2 + ((yy - y) / (r * 0.75)) ** 2 + ((zz - z) / r) ** 2
                if d <= 1 + rng.uniform(-0.15, 0.1):
                    S.set(xx, yy, zz, "minecraft:moss_block" if yy >= y + r * 0.5 and rng.random() < 0.5 else rng.choice(blokken))


def haven(W, K, P):
    """De Haven (contract box (-120,400)..(120,600)): a curved quay along the bay shore that ends in beaches and rocks, a pier,
    the grand airship tower with the moored airship, an arrival arch, cranes, crates, lanterns, guh flags, houses and the
    warehouse. Ways out of the water: ladders every ~14 blocks along the quay and the pier, two stone stairs into the water."""
    g = 64
    rng = random.Random(64)
    S = WS(0, 40, 395, 136, 110, 212)
    # quay along the bay shore, z 438..550 (the ends run out into the beach)
    oevers = {}
    for z in range(438, 551):
        for x in range(30, 130):
            if K.h(x, z) >= 0:
                oevers[z] = x
                break
    for z, oever in oevers.items():
        rand = z < 444 or z > 544
        for x in range(oever, oever + (3 if rand else 4)):
            for y in range(57, g + 1):
                S.set(x, y, z, KSTEEN if y < g else ("knuffelklinkers" if x > oever else "minecraft:stone_bricks"))
        S.set(oever, g + 1, z, "minecraft:cherry_fence" if z % 7 else LANT)
        # the land behind the quay: level with it
        for x in range(oever + 4, oever + 8):
            if K.h(x, z) < g:
                S.set(x, g, z, "knuffelklinkers" if x < oever + 6 else "knuffelgras")
    trappen = (456, 528)
    for z, oever in oevers.items():
        if z % 14 == 0 and not 462 <= z <= 478 and all(abs(z - t) > 4 for t in trappen) and 444 <= z <= 544:
            zwemtrap(S, oever - 1, z, (-1, 0), g)
    for t in trappen:
        oever = oevers[t]
        for w in (-1, 0, 1):
            z = t + w
            ox = oevers.get(z, oever)
            S.set(ox, g + 1, z, CARVE)
            for i in range(0, 5):
                x, y = ox - 1 - i, g - 1 - i
                wl = "true" if y <= KA.ZEE else "false"
                S.set(x, y, z, f"minecraft:stone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged={wl}]")
                for yy in range(56, y):
                    S.set(x, yy, z, "minecraft:stone_bricks")
                for yy in range(y + 1, g + 1):
                    if yy > KA.ZEE:
                        S.set(x, yy, z, CARVE)
            for zz in (t - 2, t + 2):
                for i in range(0, 5):
                    S.set(ox - 1 - i, g - 1 - i, zz, "minecraft:stone_bricks")
                    for yy in range(56, g - 1 - i):
                        S.set(ox - 1 - i, yy, zz, "minecraft:stone_bricks")
    # the pier into the bay
    for x in range(30, 72):
        for z in range(468, 473):
            S.set(x, g, z, "minecraft:cherry_planks")
        if x % 5 == 0:
            for z in (467, 473):
                S.fill(x, 57, z, x, g, z, "minecraft:cherry_log[axis=y]")
                S.set(x, g + 1, z, LANT if x % 10 == 0 else "minecraft:cherry_fence")
    # W-SCHEPEN: no swim ladder at the very tip any more (line 0's airship docks there; the ladders at x 37/42/52/62 stay)
    zwemtrap(S, 42, 467, (0, -1), g, steun="minecraft:cherry_log[axis=y]")
    for x in (37, 52, 62):
        zwemtrap(S, x, 473 if x != 52 else 467, (0, 1) if x != 52 else (0, -1), g, steun="minecraft:cherry_log[axis=y]")
    guhvlag(S, 30, g + 1, 473, "z", 7)             # (fix L) on the end post, not on the deck (was (31, 468): a pole on the pier)
    # W-SCHEPEN: line 0 (Kampsteiger <-> De Haven) docks at the open west tip of this pier, flush over the bay (Lijnen.Haven.UIT)
    P["haven_z1_kamp_vertrek"] = [30, g + 1, 470]
    for z in range(468, 473):                      # (fix L) the quay railing ran across the pier's start: keep it open
        if z in oevers:
            S.set(oevers[z], g + 1, z, CARVE)
    for (bx, bz) in ((40, 478), (52, 461)):
        S.fill(bx - 3, g - 1, bz - 1, bx + 3, g - 1, bz + 1, "minecraft:cherry_planks")
        S.fill(bx - 3, g, bz - 1, bx + 3, g, bz - 1, "roze_lakhout_planken")
        S.fill(bx - 3, g, bz + 1, bx + 3, g, bz + 1, "roze_lakhout_planken")
        S.fill(bx, g, bz, bx, g + 6, bz, "minecraft:cherry_fence")
        for i in range(5):
            S.fill(bx + 1, g + 1 + i, bz, bx + 4 - i, g + 1 + i, bz, "minecraft:white_wool")
    # the airship tower + the moored airship + the bell
    mx, mz, mtop = 92, 500, g + 50
    mast(S, mx, mz, g, mtop)
    luchtschip(S, mx - 32, mz - 3, mtop + 2, mast_x=mx - 5, richting=1)
    S.set(mx - 6, g + 1, mz - 12, "minecraft:bell[attachment=floor,facing=west,powered=false]")
    S.fill(mx - 8, g, mz - 14, mx - 4, g, mz - 10, "tegel_hartjes")
    P["luchtschip_haven_bel"] = [mx - 6, g + 1, mz - 12]
    P["luchtschip_haven_kapitein"] = [mx - 6, g + 1, mz - 9]
    P["luchtschip_haven_mast_top"] = [mx, mtop + 1, mz]
    # the arrival square between the tower and the arch: a ring of heart tiles, the landing spot in the middle
    for x in range(mx - 9, mx + 10):
        for z in range(468, 491):
            d = math.hypot((x - mx) / 9.5, (z - 479) / 11.5)
            if d <= 1:
                S.set(x, g, z, "tegel_hartjes" if d > 0.8 else ("knuffelklinkers" if (x + z) % 3 else "tegel_guhgezicht"))
    P["luchtschip_haven_landing"] = [mx, g + 1, 484]
    for (x, z) in ((mx - 9, 474), (mx + 9, 474), (mx - 9, 486), (mx + 9, 486)):
        S.set(x, g + 1, z, "guh_lantaarnpaal[half=lower]")
        S.set(x, g + 2, z, "guh_lantaarnpaal[half=upper]")
    # the big arrival arch over the road (its face looks at the landing)
    Z.guhpoort.__globals__["G"] = g
    Z.guhpoort(S, mx, 462, breed=9, hoog=16)
    Z.guhpoort.__globals__["G"] = 8
    for kant in (-1, 1):
        guhvlag(S, mx + kant * 7 - (4 if kant > 0 else 0), g + 22, 462, "x", 6)
        S.set(mx + kant * 7, g + 1, 466, LANT)
    # warehouse (pakhuis) with Mika clue 2
    x0, z0 = 104, 515
    Z.huis(S, x0, g + 1, z0, 13, 11, 6, "west", Z.PALETTEN[2])
    for k in range(3):
        S.set(x0 + 9, g + 1, z0 + 2 + k * 3, "minecraft:barrel[facing=up,open=false]")
        S.set(x0 + 10, g + 1, z0 + 2 + k * 3, "minecraft:barrel[facing=up,open=false]")
    for x in range(x0 + 1, x0 + 9):
        S.set(x, g + 1, z0 + 8, "minecraft:brown_carpet")
    for (dx, dz) in ((3, 4), (4, 5), (5, 6)):
        S.set(x0 + dx, g, z0 + dz, "mika_steen")
    P["mika_aanwijzing_2"] = [x0 + 4, g + 1, z0 + 5]
    P["haven_pakhuis"] = [x0 - 1, g + 1, z0 + 5]
    # houses (planks floor + a carpet runner), stalls, campfire
    for i, (x, z, v) in enumerate(((100, 430, "west"), (110, 444, "west"), (100, 540, "west"), (106, 553, "north"))):
        Z.huis(S, x, g + 1, z, 7, 7, 5, v, Z.PALETTEN[i % 5])
        # (fix L) the door sill is the floor (g); where the ground in front is lower, a slab step up to it
        vx, vz = (x - 1, z + 3) if v == "west" else (x + 3, z - 1)
        if K.h(vx, vz) < g:
            for yy in range(K.h(vx, vz) + 1, g):
                S.set(vx, yy, vz, KSTEEN)
            S.set(vx, g, vz, "minecraft:cherry_slab[type=bottom,waterlogged=false]")
        loper(S, x, z, 7, 7, v, g + 1, ["minecraft:pink_carpet", "minecraft:magenta_carpet", "minecraft:light_blue_carpet",
                                          "minecraft:lime_carpet"][i])
    for i, z in enumerate((480, 488, 532)):
        S.fill(77, g, z - 1, 82, g, z + 3, "minecraft:spruce_planks")       # a plank floor under the stall (no pit, no grass)
        for yy in range(58, g):
            S.fill(77, yy, z - 1, 82, yy, z + 3, KSTEEN)
        Z.kraam.__globals__["G"] = g
        Z.kraam(S, 78, z, ["rood", "geel", "blauw"][i])
        S.fill(79, g + 1, z + 1, 80, g + 1, z + 1, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
    Z.kraam.__globals__["G"] = 8
    S.set(100, g + 1, 452, VUUR)
    P["kampvuur_haven"] = [100, g + 1, 452]
    P["haven_plein"] = [90, g + 1, 470]
    # along the quay: cranes, crates, lanterns, flags (on the land side, never on the walkway)
    for (z, oever) in sorted(oevers.items()):
        binnen = oever + 4
        if z in (447, 541):
            kraan(S, oever + 3, z, g)
        elif z % 12 == 3 and 446 <= z <= 540 and not 460 <= z <= 492:
            S.set(binnen, g + 1, z, "guh_lantaarnpaal[half=lower]")
            S.set(binnen, g + 2, z, "guh_lantaarnpaal[half=upper]")
        elif z % 24 == 9 and 446 <= z <= 540 and not 460 <= z <= 492:
            guhvlag(S, binnen, g + 1, z, "z", 7)
        elif z % 17 == 0 and 446 <= z <= 540 and not 458 <= z <= 494:
            kratten(S, binnen + 1, z, g, rng)
    for (x, z) in ((98, 506), (99, 509), (84, 512), (110, 508), (73, 470)):
        kratten(S, x, z, g, rng)
    # the coast around the quay: rocks on the beach and in the shallows, sand coves
    for _ in range(140):
        x, z = rng.randint(20, 120), rng.randint(400, 598)
        if 440 <= z <= 548 and x < 120:
            if rng.random() < 0.85:
                continue
        i, j = KA.idx(x, z)
        h = int(K.H[i, j])
        kd = float(K.kustd[i, j])
        if K.pad[i, j] > 0.05 or W.bezet[i, j]:
            continue
        if h >= 0 and kd < 6:
            kei(S, x, h + 0.5, z, rng.uniform(1.0, 2.4), rng)
        elif h < 0 and float(K.zeed[i, j]) < 4:
            kei(S, x, KA.ZEE - 0.5, z, rng.uniform(1.0, 1.8), rng)
    # the soft rim of the village: a few boulders and shrubs where the flat ground turns into the meadow
    for _ in range(60):
        x, z = rng.randint(40, 120), rng.randint(405, 595)
        i, j = KA.idx(x, z)
        if not 4 < float(K.haven_d[i, j]) < 16 or K.pad[i, j] > 0.05 or K.H[i, j] < 63 or W.bezet[i, j]:
            continue
        h = int(K.H[i, j])
        if rng.random() < 0.5:
            kei(S, x, h + 0.3, z, rng.uniform(0.9, 1.7), rng)
        else:
            S.ellipsoid(x, h + 1.5, z, 1.6, 1.2, 1.6, "minecraft:flowering_azalea_leaves[distance=7,persistent=true,waterlogged=false]", keep=True)
    W.stempel(S.stempel("haven"))


def rondvlucht(W, K, P):
    """Set pieces for the sightseeing flight Knuffelkamp <-> De Haven (WereldReizen): an airship at anchor in the sky over the
    sea (the flight passes it; guhs wave from its balcony, code-spawned) and cloud banks on both sides of the route."""
    cx, cy, cz = 40, 118, 760          # = WereldReizen.SCHIP (T-WERELD: bent with the camp shift)
    cx, cz = kamp_meng(cx, cz)
    S = WS(cx - 30, cy - 20, cz - 14, 62, 40, 28)
    luchtschip(S, cx, cz, cy)
    gy = cy - 8 - 5
    S.fill(cx - 6, gy, cz + 3, cx + 6, gy, cz + 4, "minecraft:cherry_planks")
    S.fill(cx - 7, gy + 1, cz + 5, cx + 7, gy + 1, cz + 5, "minecraft:cherry_fence")
    for x in (cx - 7, cx + 7):
        S.fill(x, gy + 1, cz + 3, x, gy + 1, cz + 4, "minecraft:cherry_fence")
    for x in (cx - 7, cx + 7):
        S.set(x, gy + 2, cz + 5, LANT)
    P["rondvlucht_schip"] = [cx, gy + 1, cz + 3]
    W.stempel(S.stempel("rondvlucht_schip"), bezet=False)
    rng = random.Random(77)
    for (x, y, z) in ((68, 128, 690), (140, 134, 650), (70, 136, 610), (150, 140, 700), (58, 132, 650)):
        x, z = kamp_meng(x, z)
        S = WS(x - 24, y - 10, z - 24, 49, 22, 49)
        for _ in range(5):
            S.ellipsoid(x + rng.uniform(-10, 10), y + rng.uniform(-2, 3), z + rng.uniform(-10, 10), rng.uniform(6, 12),
                        rng.uniform(3, 5), rng.uniform(6, 12), rng.choice(["wolkenblok_wit", "wolkenblok_wit", "wolkenblok_roze"]))
        W.stempel(S.stempel("rondvlucht_wolk"), bezet=False)


# ================================================================================================ Klodderweide
def arena_klodder(W, K, P):
    ax, az, r = KA.ARENA
    g = K.h(ax, az)
    S = WS(ax - r - 8, g - 4, az - r - 8, 2 * r + 17, 30, 2 * r + 17)
    for x in range(ax - r - 4, ax + r + 5):
        for z in range(az - r - 4, az + r + 5):
            d = math.hypot(x - ax, z - az)
            if d <= r:
                S.set(x, g, z, "guhs:weide_roze_gras" if int(d) % 8 < 2 else "minecraft:grass_block[snowy=false]")
            elif d <= r + 2.5:
                ang = math.degrees(math.atan2(z - az, x - ax)) % 360
                if abs(ang - 180) < 7:      # the door (west)
                    S.set(x, g, z, "knuffelklinkers")
                    continue
                S.fill(x, g - 2, z, x, g, z, PASTEL)
                S.set(x, g + 1, z, "pastelbaksteen_roze_muur" if int(ang) % 12 else ROZE)
    for k in range(12):
        a = k * math.pi / 6
        x, z = ax + (r + 1.2) * math.cos(a), az + (r + 1.2) * math.sin(a)
        if abs(math.degrees(a) - 180) < 12:
            continue
        S.fill(int(x), g + 1, int(z), int(x), g + 6, int(z), GLAD)
        S.set(int(x), g + 7, int(z), ["guhs:roze_slijmblok", "minecraft:slime_block", "minecraft:honey_block"][k % 3])
        S.set(int(x), g + 8, int(z), "minecraft:shroomlight")
    # door arch (west) with a crown
    dx = ax - r - 1
    for kant in (-3, 3):
        S.fill(dx, g + 1, az + kant, dx, g + 8, az + kant, GLAD)
    S.fill(dx, g + 9, az - 3, dx, g + 9, az + 3, "minecraft:gold_block")
    for z in (az - 2, az, az + 2):
        S.set(dx, g + 10, z, "minecraft:gold_block")
    # 3 safe spots (the last phase): flower rings
    veilig = []
    for a in (90, 210, 330):
        x, z = ax + 20 * math.cos(math.radians(a)), az + 20 * math.sin(math.radians(a))
        for u in range(-2, 3):
            for v in range(-2, 3):
                if u * u + v * v <= 5:
                    S.set(x + u, g, z + v, "tegel_hartjes")
        veilig.append([int(x), g + 1, int(z)])
    S.set(dx - 2, g + 1, az - 5, "minecraft:spruce_sign[rotation=4,waterlogged=false]")
    P["arena_koning_klodder"] = dict(centrum=[ax, g + 1, az], straal=r, deur=[ax - r - 4, g + 1, az], bord=[dx - 2, g + 1, az - 5],
                                     veilige_plekken=veilig)
    W.stempel(S.stempel("arena_klodder"))


def slaapheuvel(W, K, P):
    cx, cz = KA.SLAAPHEUVEL
    g = K.h(cx, cz)
    rx, ry, rz = 46, 26, 32
    S = WS(cx - rx - 2, g - 12, cz - rz - 16, 2 * rx + 5, ry + 31, 2 * rz + 19)
    rng = random.Random(5)
    for x in range(cx - rx, cx + rx + 1):
        for z in range(cz - rz, cz + rz + 1):
            e = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if e > 1:
                continue
            top = g + int(ry * math.sqrt(1 - e) ** 1.15)
            # fix round 1: the slime is solid down to the LOCAL ground (min with the centre ground), so its edge meets
            # the terrain everywhere and nobody can crawl under it
            gl = min(K.h(x, z), g) - 1
            top = max(top, gl + 1)
            for y in range(gl, top + 1):
                b = "minecraft:lime_wool" if rng.random() < 0.75 else "minecraft:lime_concrete"
                if y == top and e < 0.25 and rng.random() < 0.7:
                    b = "minecraft:moss_block"
                S.set(x, y, z, b)
            if e < 0.3 and rng.random() < 0.08:
                S.set(x, top + 1, z, rng.choice(["minecraft:short_grass", "minecraft:pink_tulip", "guhs:roze_guhbloem"]))
    # shine
    for x in range(cx - 18, cx - 8):
        for z in range(cz - 14, cz - 6):
            e = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            top = g + int(ry * math.sqrt(max(0, 1 - e)) ** 1.15)
            if ((x - cx + 13) / 5) ** 2 + ((z - cz + 10) / 3) ** 2 < 1:
                S.set(x, top, z, "minecraft:white_concrete")
    # sleeping face on the south side: closed eye arcs + blush + mouth

    def oppervlak_z(x, y):
        e_xy = ((x - cx) / rx) ** 2 + ((y - g) / ry) ** 2
        if e_xy >= 1:
            return None
        return int(cz + rz * math.sqrt(1 - e_xy))
    for kant in (-1, 1):
        ox = cx + kant * 15
        for u in range(-6, 7):
            x, y = ox + u, g + 12 - int(round(0.12 * u * u))
            zz = oppervlak_z(x, y)
            if zz:
                S.set(x, y, zz, "minecraft:black_wool")
                S.set(x, y - 1, zz, "minecraft:black_wool")
        for u in range(-3, 4):
            for v in range(-1, 2):
                zz = oppervlak_z(ox + kant * 2 + u, g + 6 + v)
                if zz:
                    S.set(ox + kant * 2 + u, g + 6 + v, zz, "minecraft:pink_wool")
    for u in (-1, 0, 1):
        zz = oppervlak_z(cx + u, g + 5 - abs(u) + 1)
        if zz:
            S.set(cx + u, g + 5 - abs(u) + 1, zz, "minecraft:black_wool")
    # zzz floating above
    for i, (dx, dy, s) in enumerate(((18, 8, 3), (24, 15, 4), (31, 24, 5))):
        x0, y0 = cx + dx, g + ry + dy - 6
        for u in range(s):
            S.set(x0 + u, y0 + s - 1, cz, "minecraft:white_wool")
            S.set(x0 + u, y0, cz, "minecraft:white_wool")
            S.set(x0 + s - 1 - u, y0 + u, cz, "minecraft:white_wool")
    # treasure tunnel from the north side into a small chamber
    for t in range(0, 26):
        z = cz - rz - 1 + t
        for x in range(cx - 1, cx + 2):
            for y in range(g, g + 3):
                S.set(x, y, z, CARVE)
            S.set(x, g - 1, z, "minecraft:lime_terracotta")
            S.set(x, g + 3, z, "minecraft:lime_wool")
        S.set(cx - 2, g, z, "minecraft:lime_wool")
        S.set(cx - 2, g + 1, z, "minecraft:lime_wool")
        S.set(cx - 2, g + 2, z, "minecraft:lime_wool")
        S.set(cx + 2, g, z, "minecraft:lime_wool")
        S.set(cx + 2, g + 1, z, "minecraft:lime_wool")
        S.set(cx + 2, g + 2, z, "minecraft:lime_wool")
    # the way in: if the ground north of the tunnel is higher than its floor, stairs down into it (no pit)
    for k in range(1, 14):
        z = cz - rz - 1 - k
        hz = max(K.h(x, z) for x in range(cx - 1, cx + 2))
        ys = g - 1 + k
        if ys > hz:
            break
        for x in range(cx - 1, cx + 2):
            S.set(x, ys, z, ST("minecraft:mossy_cobblestone_stairs", "north"))
            for y in range(ys + 1, ys + 4):
                S.set(x, y, z, CARVE)
        for x in (cx - 2, cx + 2):
            for y in range(ys, ys + 3):
                S.set(x, y, z, "minecraft:lime_wool")
    kz = cz - rz + 26
    S.ellipsoid(cx, g + 2, kz, 5, 4, 5, "minecraft:lime_terracotta")
    S.ellipsoid(cx, g + 2, kz, 4, 3, 4, CARVE)
    S.fill(cx - 4, g - 1, kz - 4, cx + 4, g - 1, kz + 4, "minecraft:lime_terracotta")
    S.fill(cx - 1, g, cz - rz - 1, cx + 1, g + 2, kz - 2, CARVE)      # fix round 1: the chamber shell closed the tunnel
    S.set(cx, g, kz + 2, "minecraft:chest[facing=north,type=single,waterlogged=false]")
    S.set(cx + 2, g, kz, "minecraft:ochre_froglight[axis=y]")
    S.set(cx - 2, g, kz, "minecraft:ochre_froglight[axis=y]")
    P["slaapheuvel"] = [cx, g + ry + 1, cz]
    P["geheim_slaapheuvel"] = [cx, g, kz + 2]
    P["geheim_slaapheuvel_ingang"] = [cx, g, cz - rz - 2]
    W.stempel(S.stempel("slaapheuvel"))


# ================================================================================================ needles, bridge, turtle
def naald_straal(t):
    """Nominal rock radius of a needle at relative height t (0 = ground, 1 = top)."""
    return 8.5 * (1 - min(max(t, 0), 1)) ** 0.7 + 3.2


def naald(W, K, P, naam, x, z, hoogte):
    """Rock needle with a viewpoint on top (fix round 1): one continuous walkway spiral cut into the rock that rises
    half a block per step (slabs; no jumping), a fence rail on the outside only, headroom carved, ending level with the
    top platform where the platform railing is open. Height as a function of the angle, so it is radially even."""
    g = K.h(x, z)
    top = g + hoogte
    R = 19
    S = WS(x - R, g - 4, z - R, 2 * R + 1, hoogte + 18, 2 * R + 1)
    lagen = ["minecraft:stone", "minecraft:andesite", "minecraft:calcite", "minecraft:stone", "minecraft:tuff", "minecraft:diorite"]
    rng = random.Random(sum(map(ord, naam)) * 7919)
    for y in range(g - 3, top + 1):
        t = (y - g) / hoogte
        r = naald_straal(t)
        for xx in range(x - 13, x + 14):
            for zz in range(z - 13, z + 14):
                a = math.atan2(zz - z, xx - x)
                rr = r * (1 + 0.12 * math.sin(3 * a + y * 0.15) + 0.06 * math.sin(7 * a))
                d = math.hypot(xx - x, zz - z)
                if d <= rr:
                    b = lagen[((y + int(2 * math.sin(a * 2))) // 3) % len(lagen)]
                    if rng.random() < 0.08:
                        b = "minecraft:moss_block"
                    S.set(xx, y, zz, b)
    # ---- the spiral: feet height h(theta); dh/dtheta = 0.4 * inner radius (<= half a block per block at the inner edge)
    BREED = 3.4
    # start where the path arrives (the path that ends at this needle), else on the side facing the map centre
    a0 = math.atan2(-z, -x)
    for pts in KA.PADEN.values():
        if math.hypot(pts[-1][0] - x, pts[-1][1] - z) < 18:
            a0 = math.atan2(pts[-2][1] - z, pts[-2][0] - x)
    sx_, sz_ = x + (naald_straal(0) + 1.5) * math.cos(a0), z + (naald_straal(0) + 1.5) * math.sin(a0)
    th, h = a0, float(max(g, K.h(int(sx_), int(sz_))) + 1)
    h0 = int(h)
    stappen = [(th, h)]
    while h < top + 2:
        r_in = 0.82 * naald_straal((h - g) / hoogte)
        th += 0.01
        h = min(top + 2.0, h + 0.01 * 0.42 * r_in)
        stappen.append((th, h))
    th_eind = th
    ths = np.array([s_[0] for s_ in stappen])
    hs = np.array([s_[1] for s_ in stappen])

    def hoogte_bij(theta):
        return float(np.interp(theta, ths, hs))
    loop = {}           # (xx, zz) -> list of feet heights (one per loop)
    rand = {}
    for xx in range(x - R + 1, x + R):
        for zz in range(z - R + 1, z + R):
            d = math.hypot(xx - x, zz - z)
            a = math.atan2(zz - z, xx - x)
            k0 = math.floor((a0 - a) / (2 * math.pi))
            for k in range(k0, k0 + 12):
                theta = a + 2 * math.pi * k
                if theta < a0 - 0.05 or theta > th_eind:
                    continue
                hh = hoogte_bij(theta)
                rn = naald_straal((hh - g) / hoogte)
                r_in = 0.82 * rn
                r_uit = rn + BREED
                q = math.floor(hh * 2) / 2
                if r_in - 0.5 <= d <= r_uit:
                    loop.setdefault((xx, zz), []).append(q)
                elif r_uit < d <= r_uit + 1.2:
                    rand.setdefault((xx, zz), []).append(q)
    # the top platform first: the spiral cuts its own way through the last part of it
    S.cylinder(x, z, 6.5, top + 1, top + 1, "minecraft:spruce_planks")
    S.cylinder(x, z, 6.5, top + 2, top + 2, "minecraft:spruce_fence", hollow=1)
    S.fill(x, top + 2, z, x, top + 9, z, "minecraft:spruce_fence")
    S.set(x, top + 10, z, "minecraft:shroomlight")
    for i in range(4):
        S.fill(x + 1, top + 6 + i, z, x + 4 - i, top + 6 + i, z, "minecraft:pink_wool")
    S.set(x - 2, top + 2, z - 2, LANT)

    def vloer(xx, zz, q):
        y = int(math.floor(q))
        if q == y:      # whole block: a top slab under the feet
            S.set(xx, y - 1, zz, "minecraft:spruce_slab[type=top,waterlogged=false]")
        else:           # half: a bottom slab in the feet cell
            S.set(xx, y, zz, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
        return y
    for (xx, zz), qs in loop.items():
        for q in qs:
            y = int(math.floor(q))
            for yy in range(y, y + 3):
                S.set(xx, yy, zz, CARVE)
    for (xx, zz), qs in rand.items():
        for q in qs:
            y = int(math.floor(q))
            for yy in range(y, y + 3):
                if (xx, zz) not in loop:
                    S.set(xx, yy, zz, CARVE)
    for (xx, zz), qs in loop.items():
        for q in qs:
            vloer(xx, zz, q)
    n = 0
    for (xx, zz), qs in sorted(rand.items()):
        if (xx, zz) in loop:
            continue
        for q in qs:
            y = vloer(xx, zz, q)
            if q < g + 3 or q >= top + 1.5:
                continue        # no rail near the ground (step on from the side) nor at the arrival on the platform
            fy = y if q == y else y + 1
            S.set(xx, fy, zz, "minecraft:spruce_fence")
            n += 1
            if n % 23 == 0:
                S.set(xx, fy + 1, zz, LANT)
    # platform columns beside a lower part of the arriving ramp get a fence (no unexpected drop)
    for xx in range(x - 7, x + 8):
        for zz in range(z - 7, z + 8):
            if math.hypot(xx - x, zz - z) > 6.5 or (xx, zz) in loop:
                continue
            for (bx, bz) in ((xx + 1, zz), (xx - 1, zz), (xx, zz + 1), (xx, zz - 1)):
                laag = [q for q in loop.get((bx, bz), []) if top - 4 < q <= top + 0.5]
                if laag:
                    S.set(xx, top + 2, zz, "minecraft:spruce_fence")
    # the arrival: the last loop column (highest) is level with the platform; no fence in the 3 blocks around it
    aankomst = max(((q, xy) for xy, qs in loop.items() for q in qs), key=lambda t: t[0])[1]
    for xx in range(aankomst[0] - 2, aankomst[0] + 3):
        for zz in range(aankomst[1] - 2, aankomst[1] + 3):
            if S.get(xx, top + 2, zz) == "minecraft:spruce_fence" and not (xx == x and zz == z):
                S.set(xx, top + 2, zz, CARVE)
    start = (round(x + (naald_straal(0) + 1.5) * math.cos(a0)), round(z + (naald_straal(0) + 1.5) * math.sin(a0)))
    P[f"uitkijk_{naam}"] = [x, top + 2, z]
    P[f"naaldtrap_{naam}"] = [start[0], h0, start[1]]
    W.stempel(S.stempel("naald_" + naam))
    return R


def fossielbrug(W, K, P):
    """The dragon-fossil bridge square over the Kloof (fix round 1): a bone deck at the higher rim, rails, rib arches, and
    at both ends a 5-wide stone stair down to the ground (no deck ending in the air)."""
    (ax, az), (bx, bz) = KA.FOSSIEL
    y = K.dek
    L = math.hypot(bx - ax, bz - az)
    ux, uz = (bx - ax) / L, (bz - az) / L
    px, pz = -uz, ux
    S = WS(min(ax, bx) - 34, y - 45, min(az, bz) - 34, abs(bx - ax) + 69, 75, abs(bz - az) + 69)

    def vlak(t, o):
        return ax + ux * t + px * o, az + uz * t + pz * o
    n = int(L * 2)
    for i in range(-2, n + 3):
        t = i / 2
        cx, cz = ax + ux * t, az + uz * t
        for o in np.arange(-2.5, 2.6, 0.5):
            S.set(cx + px * o, y, cz + pz * o, "minecraft:bone_block[axis=y]")
            S.set(cx + px * o, y - 1, cz + pz * o, "minecraft:bone_block[axis=y]")
            for d_ in range(1, 4):
                S.set(cx + px * o, y + d_, cz + pz * o, CARVE)
        for o in (-3, 3):
            S.set(cx + px * o, y, cz + pz * o, "minecraft:calcite")
            S.set(cx + px * o, y + 1, cz + pz * o, "minecraft:diorite_wall")
        # vertebrae hanging below
        if i % 6 == 0:
            for k in range(1, 5):
                S.set(cx, y - 1 - k, cz, "minecraft:bone_block[axis=y]")

    # both ends: stairs (facing back to the deck) down to the local ground, the rail follows, headroom carved
    def trap_af(t0, richting):
        """richting -1 = before A (going away from B), +1 = after B."""
        for st in range(0, 30):
            t = t0 + richting * (st + 1)
            cx, cz = ax + ux * t, az + uz * t
            grond = max(K.h(int(round(cx + px * o)), int(round(cz + pz * o))) for o in (-2, 0, 2))
            yy = y - st
            if yy <= grond:
                # level with the ground (or ground higher): flatten a landing and stop
                for o in np.arange(-2.5, 2.6, 0.5):
                    for t2 in (t, t + richting * 0.5):
                        qx, qz = ax + ux * t2 + px * o, az + uz * t2 + pz * o
                        S.set(qx, grond, qz, "minecraft:bone_block[axis=y]")
                        for d_ in range(1, 4):
                            S.set(qx, grond + d_, qz, CARVE)
                return
            facing = richting_naar(-ux * richting, -uz * richting)
            for o in np.arange(-2.5, 2.6, 0.5):
                for t2 in (t - richting * 0.25, t + richting * 0.25):
                    qx, qz = ax + ux * t2 + px * o, az + uz * t2 + pz * o
                    S.set(qx, yy, qz, ST("minecraft:polished_diorite_stairs", facing))
                    for d_ in range(1, 4):
                        S.set(qx, yy - d_, qz, "minecraft:calcite")
                    for d_ in range(1, 4):
                        S.set(qx, yy + d_, qz, CARVE)
            for o in (-3, 3):
                S.set(cx + px * o, yy, cz + pz * o, "minecraft:calcite")
                S.set(cx + px * o, yy + 1, cz + pz * o, "minecraft:diorite_wall")
                for d_ in range(1, 4):
                    S.set(cx + px * o, yy - d_, cz + pz * o, "minecraft:calcite")
    trap_af(-1, -1)
    trap_af(L + 1.5, 1)
    # ribs: arches over the deck every 6 blocks
    for i in range(4, int(L) - 3, 6):
        cx, cz = ax + ux * i, az + uz * i
        for s in np.arange(0, math.pi + 0.01, 0.06):
            o = 6 * math.cos(s)
            hh = 10 * math.sin(s)
            S.set(cx + px * o, y + 1 + hh, cz + pz * o, "minecraft:bone_block[axis=y]")
        for kant in (-1, 1):
            for k in range(0, 8):   # rib tips curling down below the deck
                S.set(cx + px * kant * (6 + k * 0.2), y - k, cz + pz * kant * (6 + k * 0.2), "minecraft:bone_block[axis=y]")
    # the skull at the far end, well beside the path, facing back along the bridge, with guh-ear horns
    gs = K.h(int(bx + ux * 8 + px * 13), int(bz + uz * 8 + pz * 13))
    sx, sz = bx + ux * 8 + px * 13, bz + uz * 8 + pz * 13
    S.ellipsoid(sx, gs + 5, sz, 6, 6, 6, "minecraft:bone_block[axis=y]", hollow=1.5)
    S.ellipsoid(sx - ux * 6, gs + 3, sz - uz * 6, 4, 3, 4, "minecraft:bone_block[axis=y]", hollow=1.2)
    for kant in (-1, 1):
        ex, ez = sx - ux * 4 + px * kant * 3.5, sz - uz * 4 + pz * kant * 3.5
        S.ellipsoid(ex, gs + 7, ez, 1.8, 1.8, 1.8, CARVE)
        S.set(ex, gs + 7, ez, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
        for k in range(10):
            S.set(sx + px * kant * (4 + k * 0.15), gs + 11 + k, sz + pz * kant * (4 + k * 0.15), "minecraft:calcite")
            S.set(sx + px * kant * (5 + k * 0.15), gs + 11 + k, sz + pz * kant * (5 + k * 0.15), "minecraft:pink_terracotta")
    # the skull is solid: no room to fall into (its inside is hollowed by the tile writer only where fully enclosed)
    S.ellipsoid(sx, gs + 5, sz, 4.6, 4.6, 4.6, "minecraft:bone_block[axis=y]")
    P["fossielbrug"] = dict(van=[ax, y + 1, az], naar=[bx, y + 1, bz])
    W.stempel(S.stempel("fossielbrug"))


def trap_naar_buiten(S, K, x, z, dx, dz, yvloer, breed, blok="minecraft:cobblestone_stairs", vul="minecraft:cobblestone"):
    """From a floor (top block y = yvloer) at (x, z) walk outward along (dx, dz): while the ground there is higher, lay
    stairs up (facing outward), carve headroom, so a room/tunnel mouth below the terrain is never a pit."""
    fac = richting_naar(dx, dz)
    for k in range(0, 16):
        cx, cz = x + dx * k, z + dz * k
        cel = [(cx + dz * o, cz + dx * o) for o in range(-(breed // 2), breed // 2 + 1)]
        grond = max(K.h(a, b) for a, b in cel)
        ys = yvloer + k + 1
        if grond < ys:
            for (a, b) in cel:
                if k:       # a landing level with the top of the last step
                    for yy in range(K.h(a, b) + 1, ys):
                        S.set(a, yy, b, vul)
                for yy in range(ys if k else yvloer + 1, ys + 3):
                    S.set(a, yy, b, CARVE)
            return
        for (a, b) in cel:
            S.set(a, ys, b, ST(blok, fac))
            S.set(a, ys - 1, b, vul)
            for yy in range(ys + 1, ys + 4):
                S.set(a, yy, b, CARVE)


def richting_naar(dx, dz):
    """Compass facing of a (dx, dz) direction (stairs facing = the way you walk UP them)."""
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def schildpad(W, K, P):
    cx, cz = KA.SCHILDPAD
    g = KA.ZEE
    S = WS(cx - 45, g - 6, cz - 30, 90, 40, 60)
    rx, ry, rz = 30, 13, 21
    for x in range(cx - rx, cx + rx + 1):
        for z in range(cz - rz, cz + rz + 1):
            e = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if e > 1:
                continue
            top = g + int(ry * math.sqrt(1 - e))
            hexa = (int((x - cx) / 6 + ((z - cz) // 5) * 0.5) + (z - cz) // 5) % 3
            for y in range(max(g - 4, top - 2), top + 1):
                b = ["minecraft:green_terracotta", "minecraft:lime_terracotta", "minecraft:brown_terracotta"][hexa]
                if e > 0.88 and y > top - 2:
                    b = "minecraft:yellow_terracotta"
                if e < 0.3 and y == top:
                    b = "minecraft:grass_block[snowy=false]"
                S.set(x, y, z, b)
            for y in range(g - 4, max(g - 4, top - 2)):
                if e > 0.8:
                    S.set(x, y, z, "minecraft:green_terracotta")
    # head (east) with sleepy eyes
    S.ellipsoid(cx + rx + 6, g + 3, cz, 7, 5, 6, "minecraft:lime_terracotta")
    for kant in (-1, 1):
        S.set(cx + rx + 12, g + 5, cz + kant * 3, "minecraft:black_wool")
        S.set(cx + rx + 12, g + 5, cz + kant * 2, "minecraft:black_wool")
        S.set(cx + rx + 12, g + 3, cz + kant * 4, "minecraft:pink_wool")
    for (fx, fz) in ((cx + 18, cz - rz - 2), (cx + 18, cz + rz + 2), (cx - 20, cz - rz), (cx - 20, cz + rz)):
        S.ellipsoid(fx, g, fz, 6, 1.5, 3, "minecraft:lime_terracotta")
    # a palm-ish blossom tree, a chest
    S.fill(cx - 3, g + ry + 1, cz, cx - 3, g + ry + 6, cz, "guhbloesem_log[axis=y]")
    S.ellipsoid(cx - 3, g + ry + 7, cz, 4, 2, 4, "guhbloesem_leaves", keep=True)
    S.set(cx + 4, g + ry + 1, cz + 2, "minecraft:chest[facing=south,type=single,waterlogged=false]")
    P["geheim_schildpad"] = [cx + 4, g + ry + 1, cz + 2]
    P["schildpad_eiland"] = [cx, g + ry + 1, cz]
    W.stempel(S.stempel("schildpad"))


# ================================================================================================ stal, campfires, kamp south jetty
def stal(W, K, P):
    # (fix L) the ground here is y 64, not 63: with g = 63 the meadow grass stood right in front of the door (no way in
    # without /tp). Now the yard sits on the real ground level, the bumps above it are carved flat, and a paved path leads
    # from the door out east.
    x0, z0 = -158, -10
    g = max(63, K.h(x0 + 17, z0 + 6))
    S = WS(-165, g - 4, -18, 40, 30, 60)
    for x in range(-162, -128):
        for z in range(-14, 38):
            S.fill(x, g - 2, z, x, g - 1, z, "minecraft:dirt")
            S.set(x, g, z, "guhs:knuffelgras")
            S.fill(x, g + 1, z, x, g + 4, z, CARVE)
    for x in range(x0 + 15, x0 + 24):                     # the path from the door: 3 wide, head room free
        for z in range(z0 + 5, z0 + 8):
            S.set(x, g, z, "knuffelklinkers")
    Z.huis(S, x0, g + 1, z0, 15, 13, 7, "east", Z.PALETTEN[3], tapijt=None)
    S.fill(x0 + 1, g, z0 + 1, x0 + 8, g, z0 + 11, "minecraft:hay_block[axis=x]")      # stable floor: hay in the stalls
    for z in range(z0 + 1, z0 + 12):
        if z % 3 == 0:
            S.set(x0 + 2, g + 1, z, "minecraft:hay_block[axis=y]")
            S.set(x0 + 2, g + 2, z, "minecraft:hay_block[axis=y]")
        if z % 4 == 1:
            S.fill(x0 + 4, g + 1, z, x0 + 7, g + 1, z, "minecraft:spruce_fence")
    S.set(x0 + 12, g + 1, z0 + 2, "minecraft:barrel[facing=up,open=false]")
    S.set(x0 + 12, g + 1, z0 + 10, "minecraft:cauldron")
    # paddock
    for x in range(-158, -134):
        for z in (16, 34):
            S.set(x, g + 1, z, "minecraft:spruce_fence")
    for z in range(16, 35):
        for x in (-158, -134):
            if not (x == -134 and 24 <= z <= 26):
                S.set(x, g + 1, z, "minecraft:spruce_fence")
    S.set(-134, g + 1, 25, "minecraft:spruce_fence_gate[facing=east,in_wall=false,open=false,powered=false]")
    for (x, z) in ((-150, 20), (-142, 30), (-152, 28)):
        S.set(x, g + 1, z, "minecraft:hay_block[axis=y]")
    S.set(-146, g + 1, 24, "minecraft:composter[level=7]")
    P["stal"] = dict(deur=[x0 + 15, g + 1, z0 + 6], binnen=[x0 + 9, g + 1, z0 + 6], wei=[-146, g + 1, 25])
    W.stempel(S.stempel("stal"))


def kampvuren(W, K, P):
    for gebied, lijst in KA.KAMPVUREN.items():
        if gebied == "haven":
            continue
        for i, (x, z) in enumerate(lijst):
            x, z = naast_pad(K, x, z)
            g = K.h(x, z)
            S = WS(x - 6, g - 5, z - 6, 13, 13, 13)
            # fix round 1: the camp sits beside the path (logs and tent no longer on the walking route), on a level plaza
            S.fill(x - 4, g + 1, z - 4, x + 4, g + 5, z + 4, CARVE)
            S.fill(x - 4, g - 4, z - 4, x + 4, g - 1, z + 4, "minecraft:dirt")
            S.fill(x - 4, g, z - 4, x + 4, g, z + 4, "minecraft:grass_block[snowy=false]")
            S.fill(x - 3, g, z - 3, x + 3, g, z + 3, "minecraft:coarse_dirt")
            S.fill(x - 2, g, z - 2, x + 2, g, z + 2, "minecraft:dirt_path")
            S.set(x, g + 1, z, VUUR)
            for (dx, dz, ax) in ((-3, 0, "z"), (3, 0, "z"), (0, 3, "x")):
                S.set(x + dx, g + 1, z + dz, f"minecraft:spruce_log[axis={ax}]")
                S.set(x + dx + (0 if ax == "z" else 1), g + 1, z + dz + (1 if ax == "z" else 0), f"minecraft:spruce_log[axis={ax}]")
            S.set(x + 3, g + 1, z - 3, "guh_lantaarnpaal[half=lower]")
            S.set(x + 3, g + 2, z - 3, "guh_lantaarnpaal[half=upper]")
            # a little order tent (with a carpet floor inside, fix round 1)
            for k in range(3):
                for dz in range(-5, -2):
                    S.set(x - 5 + k, g + 1 + k, z + dz, "minecraft:pink_wool")
                    S.set(x - 1 - k, g + 1 + k, z + dz, "minecraft:white_wool")
            for dz in range(-5, -2):
                for dx in (-4, -3, -2):
                    S.set(x + dx, g, z + dz, "minecraft:coarse_dirt")
                    S.set(x + dx, g + 1, z + dz, "minecraft:red_carpet" if dx == -3 else "minecraft:white_carpet")
            P[f"kampvuur_{gebied}_{i + 1}"] = [x, g + 1, z]
            P[f"ridder_{gebied}_{i + 1}"] = [x + 2, g + 1, z + 2]
            W.stempel(S.stempel(f"kampvuur_{gebied}_{i}"))


def zwemtrap(S, x, z, uit, y_top, y_bodem=59, steun="minecraft:spruce_log[axis=y]"):
    """A ladder out of the sea: ladder blocks in column (x, z) from y_bodem up to y_top (the floor block of the deck/quay it
    leads onto), hanging on the support column (x, z) - uit, which is filled solid from y_bodem to y_top - 1."""
    f = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[uit]
    sx, sz = x - uit[0], z - uit[1]
    for y in range(y_bodem, y_top):
        if S.get(sx, y, sz) in ("minecraft:air", CARVE):
            S.set(sx, y, sz, steun)
    for y in range(y_bodem - 3, y_bodem):        # solid ground under the ladder and its post (the sea floor is only a skin)
        S.set(x, y, z, "minecraft:sandstone")
        S.set(sx, y, sz, "minecraft:sandstone")
    for y in range(y_bodem, y_top + 1):
        S.set(x, y, z, f"minecraft:ladder[facing={f},waterlogged={'true' if y <= KA.ZEE else 'false'}]")
    for y in (y_top + 1, y_top + 2):
        S.set(x, y, z, CARVE)
        S.set(sx, y, sz, CARVE)


def naast_pad(K, x, z):
    """A campfire spot 6-8 blocks beside the path near (x, z), on the flattest ground."""
    from scipy import ndimage  # noqa: F401
    i0, j0 = KA.idx(x, z)
    best, bs = (x, z), 1e9
    for dx in range(-12, 13):
        for dz in range(-12, 13):
            i, j = i0 + dx, j0 + dz
            d = float(_padafst(K)[i, j])
            if not 6 <= d <= 8.5:
                continue
            blok = K.H[i - 4:i + 5, j - 4:j + 5]
            if (blok < 64).any() or K.kloof[i - 4:i + 5, j - 4:j + 5].any():
                continue
            score = (int(blok.max()) - int(blok.min())) * 3 + math.hypot(dx, dz) * 0.2
            if score < bs:
                best, bs = (x + dx, z + dz), score
    return best


def _padafst(K):
    if "k" not in _PADAFSTAND:
        from scipy import ndimage
        _PADAFSTAND["k"] = ndimage.distance_transform_edt(~(K.pad > 0.5)).astype(np.float32)
    return _PADAFSTAND["k"]


def kamp_zuidsteiger(W, K, P, kamp_d):
    """Kern's AANKOMST (0, 65, 1130): a low jetty on the south coast of Knuffelkamp + a wooden stair up the cliff all the way to
    the camp plateau (a cutting into the cliff, walls of stone, lanterns), and ladders out of the water onto the jetty."""
    hoog = kamp_d["hoogte"]
    ox, oy, oz = [int(v) for v in kamp_d["off"]]

    def kh(x, z):
        i, j = x + KDX - ox, z + KDZ - oz
        if 0 <= i < hoog.shape[0] and 0 <= j < hoog.shape[1]:
            return int(hoog[i, j])
        return -1
    S = WS(-12, 50, 1060, 25, 60, 100)
    for z in range(1126, 1157):
        for x in range(-2, 3):
            S.set(x, 64, z, "minecraft:spruce_planks")
        if z % 4 == 0:
            for x in (-3, 3):
                S.fill(x, 58, z, x, 65, z, "minecraft:spruce_log[axis=y]")
            S.set(3, 66, z, LANT)
    S.fill(-5, 64, 1146, 5, 64, 1156, "minecraft:spruce_planks")
    S.set(-4, 65, 1147, "guhs:ballonsteiger")
    P["aankomst"] = kamp_p([0, 65, 1130])
    # ways out of the water onto the jetty: both sides halfway, both sides of the platform, the far end
    for (x, z, uit) in ((3, 1130, (1, 0)), (-3, 1130, (-1, 0)), (3, 1138, (1, 0)), (-3, 1138, (-1, 0)), (6, 1150, (1, 0)), (-6, 1150, (-1, 0)), (2, 1157, (0, 1)),
                        (-2, 1157, (0, 1))):
        zwemtrap(S, x, z, uit, 64)

    def hoogte_voor(z):
        return max(kh(x, z) for x in range(-2, 3))
    # the stair north up the cliff: one step per block, until the ground ahead is no higher than the step
    y, z = 65, 1126
    trap = []
    while z > 1066 and y < 100:
        trap.append((y, z))
        if hoogte_voor(z - 1) <= y and hoogte_voor(z - 2) <= y + 1 and hoogte_voor(z - 3) <= y + 1 and kh(0, z - 1) >= y - 1:
            break
        y += 1
        z -= 1
    for (y, z) in trap:
        t = hoogte_voor(z)
        for x in range(-1, 2):
            S.set(x, y, z, ST("minecraft:spruce_stairs", "north"))
            for yy in range(y + 1, y + 4):
                S.set(x, yy, z, CARVE)
            for yy in range(58, y):
                if S.get(x, yy, z) == "minecraft:air":
                    S.set(x, yy, z, "minecraft:spruce_planks" if yy == y - 1 else "minecraft:stone")
        binnen = t > y + 1
        for x in (-2, 2):
            S.set(x, y + 1, z, "minecraft:stone_bricks" if binnen else "minecraft:spruce_fence")
            top = max(kh(x, z), y + 3)
            for yy in range(y + 2, top + 1):
                S.set(x, yy, z, "minecraft:stone_bricks" if yy <= y + 4 else "minecraft:stone")
            for yy in range(58, y + 1):
                S.set(x, yy, z, "minecraft:spruce_planks" if yy == y else "minecraft:stone")
        if t > y + 4:
            for x in range(-2, 3):
                S.set(x, y + 4, z, "minecraft:stone_bricks")
                for yy in range(y + 5, kh(x, z)):
                    S.set(x, yy, z, "minecraft:stone")
            if z % 5 == 0:
                S.set(0, y + 3, z, LANT_H)
        elif z % 5 == 0:
            S.set(2, y + 2, z, LANT)
    # the top: a short landing on the plateau (the ground of the first columns is cut to the stair level)
    y, z = trap[-1]
    for zz in range(z - 1, z - 4, -1):
        for x in range(-2, 3):
            g = kh(x, zz)
            for yy in range(y + 1, max(g, y + 3) + 1):
                S.set(x, yy, zz, CARVE)
            for yy in range(max(58, min(g, y) - 3), y + 1):
                S.set(x, yy, zz, "minecraft:dirt_path" if yy == y else "minecraft:dirt")
    P["aankomst_trap_top"] = kamp_p([0, y + 1, z - 2])
    W.stempel(kamp_stempel(S, "kamp_zuidsteiger"))


def lijn0_kampsteiger(W, P, kamp_poi):
    """W-SCHEPEN: line 0's camp end. The north arm of the camp dock (kamp scene) ends 6 east of the bell in a railing: that
    railing goes (the 4 rails south of the flag pole; the airship lies flush against the tip) and the tip is the harbour POI
    haven_kamp_z1_vertrek. The pole's flag loses its top row (it reached into the docked ship's balloon)."""
    bx, by, bz = kamp_poi["luchtschip_bel"]
    tx, tz = bx + 6, bz
    S = WS(tx - 1, by - 1, tz - 3, 3, 12, 7)
    for z in range(tz - 1, tz + 3):
        S.set(tx, by, z, CARVE)
    for z in range(tz - 2, tz + 2):
        S.set(tx, by + 9, z, CARVE)
    S.set(tx + 1, by - 1, tz + 3, CARVE)   # (the top rung of the swim ladder beside the tip: in the docked ship's propeller)
    W.stempel(S.stempel("w_kampsteiger_tip"))
    P["haven_kamp_z1_vertrek"] = [tx, by, tz]


def kamp_arena(P, kamp_poi):
    vx, vy, vz = kamp_poi["vijver"]
    # h-melee: radius 26 (was 20 here / 18 in NpcPlekken) with a wide flat dry ring around the pond (kamp.vijver); door outside it
    P["arena_klodderreus"] = dict(centrum=[vx, vy, vz], straal=26, deur=[vx - 30, vy, vz - 8], bord=[vx - 31, vy + 1, vz - 10])


# ================================================================================================ caves (Druipgrotten)
GROT_KNOPEN = {"N1": (-320, 52, -300, 11), "N2": (-400, 48, -380, 12), "N3": (-260, 50, -420, 10), "N4": (-460, 46, -470, 9)}


def grotten(W, K, P):
    x0, z0, x1, z1 = -560, -560, -150, -180
    S = WS(x0, 30, z0, x1 - x0 + 1, 100, z1 - z0 + 1)
    rng = random.Random(12)
    # fix round 1: every room has a flat floor (stand y = voet, the POI height) and every tunnel runs floor to floor, so
    # no bowl or step is higher than 1 block; the entrances start outside the hill at ground level
    voet = {k: v[1] - int(v[3] * 0.7) + 1 for k, v in GROT_KNOPEN.items()}

    def knoop(k):
        x, _, z, _ = GROT_KNOPEN[k]
        return (x, voet[k] + 2, z)
    gangen = []
    for (ex, ez), naar in zip(KA.GROT_INGANGEN, ("N1", "N2", "N3")):
        nx_, _, nz_ = knoop(naar)
        L0 = math.hypot(ex - nx_, ez - nz_)
        bx, bz = ex + (ex - nx_) / L0 * 7, ez + (ez - nz_) / L0 * 7
        gangen.append(((bx, K.h(int(bx), int(bz)) + 2, bz), knoop(naar), 3.6))
    for a, b in (("N1", "N2"), ("N2", "N3"), ("N2", "N4"), ("N1", "N3")):
        gangen.append((knoop(a), knoop(b), 3.2))
    holte = set()
    vloer_min = {}

    def bol(cx, cy, cz, r, vlak=None):
        for x in range(int(cx - r), int(cx + r) + 1):
            for y in range(int(cy - r * 0.7), int(cy + r * 0.8) + 1):
                if vlak is not None and y < vlak:
                    continue
                for z in range(int(cz - r), int(cz + r) + 1):
                    if ((x - cx) / r) ** 2 + ((y - cy) / (r * 0.75)) ** 2 + ((z - cz) / r) ** 2 <= 1:
                        holte.add((x, y, z))
    for (a, b, r) in gangen:
        L = math.dist(a, b)
        n = int(L / 1.5)
        for i in range(n + 1):
            t = i / n
            w = math.sin(t * math.pi * 3) * 5
            p = [a[k] + (b[k] - a[k]) * t for k in range(3)]
            p[0] += w * (b[2] - a[2]) / max(1, L)
            p[2] -= w * (b[0] - a[0]) / max(1, L)
            # the floor of a tunnel follows the straight line between its ends (no bumps): carve from there up
            bol(p[0], p[1], p[2], r + 0.4 * math.sin(i * 0.7), vlak=int(round(p[1] - 2)))
    for k, (cx, cy, cz, r) in GROT_KNOPEN.items():
        bol(cx, cy, cz, r, vlak=voet[k])
    # crystal chamber (flat floor at the POI height) + the crack from N4, 3 high, floor to floor
    kx, ky, kz = KA.KRISTALKAMER
    bol(kx, ky, kz, 12, vlak=ky - 6)
    a, b = (GROT_KNOPEN["N4"][0], voet["N4"], GROT_KNOPEN["N4"][2]), (kx, ky - 6, kz)
    n = int(math.dist(a, b))
    for i in range(n + 1):
        t = i / n
        x, y, z = [a[k] + (b[k] - a[k]) * t for k in range(3)]
        for dy in (0, 1, 2):
            for dx in (0, 1):
                holte.add((int(x) + dx, int(round(y)) + dy, int(z)))
    # footprint: columns must be solid down to the cave floor
    for (x, y, z) in holte:
        i, j = KA.idx(x, z)
        W.lo_extra[i, j] = min(W.lo_extra[i, j], y - 3)
    vloer_ids = 0
    for (x, y, z) in holte:
        if y > K.h(x, z):
            continue
        S.set(x, y, z, CARVE)
    for (x, y, z) in holte:
        if y > K.h(x, z):
            continue
        onder = (x, y - 1, z) not in holte
        boven = (x, y + 1, z) not in holte and y + 1 <= K.h(x, z)
        in_kamer = math.dist((x, y, z), (kx, ky, kz)) < 13
        r = rng.random()
        if in_kamer:
            if onder and r < 0.08:
                S.set(x, y, z, rng.choice(["guh_kristal_cluster", "weidekristal_roze", "minecraft:amethyst_cluster[facing=up,waterlogged=false]"]))
            elif boven and r < 0.2:
                S.set(x, y, z, "minecraft:amethyst_cluster[facing=down,waterlogged=false]")
            continue
        if boven and r < 0.07:
            S.set(x, y, z, "minecraft:pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]")
        elif boven and r < 0.13:
            S.set(x, y + 1, z, "minecraft:shroomlight" if r < 0.11 else "minecraft:ochre_froglight[axis=y]")
        elif onder and r < 0.03:
            S.set(x, y, z, "glimpaddenstoel")
        elif onder and r < 0.05:
            S.set(x, y - 1, z, rng.choice(["minecraft:slime_block", "guhs:roze_slijmblok"]))
        elif onder and r < 0.07:
            S.set(x, y, z, "minecraft:pointed_dripstone[thickness=tip,vertical_direction=up,waterlogged=false]")
        elif onder and r < 0.09:
            S.set(x, y - 1, z, "minecraft:dripstone_block")
    # fix round 1: life along the cave paths (no mobs): little abandoned miner camps, rail ends with a cart's barrels,
    # crystal clusters, glowing mushroom patches and hanging lanterns, always at the side so the walkway stays free
    def vloer_bij(x, y0, z):
        for y in range(y0 - 3, y0 + 4):
            if (x, y, z) in holte and (x, y - 1, z) not in holte and (x, y + 1, z) in holte and y <= K.h(x, z) - 1:
                return y
        return None
    soort = 0
    for (a, b, r) in gangen:
        L = math.dist(a, b)
        dx, dz = (b[0] - a[0]) / max(1, L), (b[2] - a[2]) / max(1, L)
        for s_ in np.arange(14, L - 10, 19):
            t = s_ / L
            w = math.sin(t * math.pi * 3) * 5
            cx = a[0] + (b[0] - a[0]) * t + w * (b[2] - a[2]) / max(1, L)
            cz = a[2] + (b[2] - a[2]) * t - w * (b[0] - a[0]) / max(1, L)
            cy = a[1] + (b[1] - a[1]) * t
            kant = 1 if soort % 2 else -1
            x, z = int(round(cx - dz * 2.4 * kant)), int(round(cz + dx * 2.4 * kant))
            y = vloer_bij(x, int(round(cy - 2)), z)
            if y is None:
                continue
            soort += 1
            k = soort % 5
            if k == 0:      # abandoned camp: bedroll, barrel, lantern, an unlit candle
                S.set(x, y, z, "minecraft:barrel[facing=up,open=false]")
                S.set(x + int(dx * 1.5), y, z + int(dz * 1.5), "minecraft:brown_carpet")
                S.set(x + int(dx * 2.5), y, z + int(dz * 2.5), "minecraft:red_carpet")
                S.set(x - int(dx * 1.5), y, z - int(dz * 1.5), LANT)
            elif k == 1:    # the end of an old cart track: a few rails, a chest-less cart's load of barrels
                for u in range(-2, 2):
                    q = (x + int(round(dx * u)), z + int(round(dz * u)))
                    if vloer_bij(q[0], y, q[1]) == y:
                        S.set(q[0], y, q[1], "minecraft:rail[shape=north_south,waterlogged=false]" if abs(dz) > abs(dx)
                              else "minecraft:rail[shape=east_west,waterlogged=false]")
                q = (x + int(round(dx * 2)), z + int(round(dz * 2)))
                if vloer_bij(q[0], y, q[1]) == y:
                    S.set(q[0], y, q[1], "minecraft:barrel[facing=up,open=false]")
            elif k == 2:    # crystals
                S.set(x, y, z, "guh_kristal_cluster")
                S.set(x + int(round(dx)), y, z + int(round(dz)), "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
                S.set(x, y - 1, z, "guh_kristal_blok")
            elif k == 3:    # glowing mushrooms
                for (u, v) in ((0, 0), (1, 0), (0, 1), (-1, 0)):
                    if vloer_bij(x + u, y, z + v) == y:
                        S.set(x + u, y, z + v, "glimpaddenstoel")
            else:           # a lantern hanging from the ceiling on a chain over the walkway edge
                for yy in range(y + 2, y + 9):
                    if (x, yy + 1, z) not in holte:
                        S.set(x, yy, z, LANT_H)
                        for y2 in range(yy + 1, y + 10):
                            if (x, y2, z) in holte:
                                S.set(x, y2, z, "minecraft:iron_chain[axis=y]")
                        break
    # crystal chamber shell (amethyst + guh crystal) so it sparkles
    for (x, y, z) in holte:
        if math.dist((x, y, z), (kx, ky, kz)) < 13 and y <= K.h(x, z):
            for (dx, dy, dz) in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                q = (x + dx, y + dy, z + dz)
                if q not in holte and q[1] <= K.h(q[0], q[2]):
                    S.set(*q, rng.choice(["minecraft:amethyst_block", "minecraft:amethyst_block", "guh_kristal_blok",
                                          "minecraft:budding_amethyst" if False else "minecraft:calcite"]))
    S.set(kx + 3, ky - 6, kz, "minecraft:black_carpet")
    S.set(kx + 4, ky - 6, kz, "mika_steen")
    S.set(kx - 3, ky - 6, kz + 2, "minecraft:chest[facing=east,type=single,waterlogged=false]")
    S.fill(kx - 2, ky - 7, kz - 2, kx + 4, ky - 7, kz + 2, "minecraft:amethyst_block")
    P["kristalkamer"] = [kx, ky - 6, kz]
    P["mika_aanwijzing_4"] = [kx + 3, ky - 6, kz]
    P["grot_ingangen"] = [[ex, K.h(ex, ez) + 1, ez] for (ex, ez) in KA.GROT_INGANGEN]
    P["grot_knopen"] = {k: [v[0], v[1] - int(v[3] * 0.7) + 1, v[2]] for k, v in GROT_KNOPEN.items()}
    W.stempel(S.stempel("druipgrotten"), bezet=False)


# ================================================================================================ Sporenwoud
def sporenwoud(W, K, P):
    x0, z0, x1, z1 = -600, -170, -150, 420
    S = WS(x0, 60, z0, x1 - x0 + 1, 110, z1 - z0 + 1)
    rng = random.Random(3)
    woud = K.g["woud"] & (K.H >= 64)
    gekozen = []
    hoeden = [(ROOD, "minecraft:white_wool"), (BRUIN, "knuffelblok_roze"), (HOED_ROZE, "knuffelblok_vanille"),
              ("geestpaddenstoel_hoed", "minecraft:pearlescent_froglight[axis=y]")]
    stelen = [STEEL, "guhpaddenstoel_steel", "geestpaddenstoel_steel", STEEL]
    afst = padafstand(W, K)
    for _ in range(6000):
        x, z = rng.randint(x0 + 10, x1 - 10), rng.randint(z0 + 10, z1 - 10)
        i, j = KA.idx(x, z)
        r = rng.uniform(6, 15)
        krom = rng.uniform(-3, 3)
        rs = max(1.5, r * 0.22)
        # fix round 1: the stem (+ its bend) keeps 4 blocks off every path and building, the whole mushroom off the needles
        if not woud[i, j] or afst[i, j] < rs * 1.6 + abs(krom) + 4 or W.bezet[i, j]:
            continue
        if any(math.hypot(x - a, z - b) < 18 for (a, b) in KA.LICHTSTRALEN):
            continue
        if any(math.hypot(x - a, z - b) < 24 for (a, b, *_) in gekozen):
            continue
        if any(math.hypot(x - a, z - b) < 19 + r + 4 for (a, b) in KA.UITKIJK.values()):
            continue
        gekozen.append((x, z, r, krom))
    for (x, z, r, krom) in gekozen:
        g = K.h(x, z)
        h = int(r * rng.uniform(2.2, 3.6))
        k = rng.randrange(4)
        steel = stelen[k]
        rs = max(1.5, r * 0.22)
        for y in range(g - 1, g + h):
            t = (y - g) / h
            sx = x + krom * math.sin(math.pi * t)
            rr = rs * (1 + 0.6 * math.exp(-t * 8))
            for xx in range(int(sx - rr - 1), int(sx + rr + 2)):
                for zz in range(int(z - rr - 1), int(z + rr + 2)):
                    if math.hypot(xx - sx, zz - z) <= rr:
                        S.set(xx, y, zz, steel)
        hoed(S, x, z, g + h + int(r * 0.4), r, hoeden[k][0], hoeden[k][1])
        if rng.random() < 0.3:
            S.set(x + rs + 1, g + 1, z, "glimpaddenstoel")
    # fix round 1: more life beside the paths (no mobs): forest camps, glowing fairy rings, fallen logs, lamp posts
    n_leven = 0
    for _ in range(4000):
        if n_leven >= 34:
            break
        x, z = rng.randint(x0 + 10, x1 - 10), rng.randint(z0 + 10, z1 - 10)
        i, j = KA.idx(x, z)
        if not woud[i, j] or not 4 <= afst[i, j] <= 11 or W.bezet[i - 3:i + 4, j - 3:j + 4].any():
            continue
        if any(math.hypot(x - a, z - b) < rr * 0.35 + 6 for (a, b, rr, _) in gekozen):
            continue
        blok = K.H[i - 3:i + 4, j - 3:j + 4]
        if int(blok.max()) - int(blok.min()) > 1:
            continue
        g = K.h(x, z)
        k = n_leven % 4
        n_leven += 1
        if k == 0:      # a little forest camp
            S.set(x, g + 1, z, "minecraft:spruce_log[axis=y]")
            S.set(x, g + 2, z, LANT)
            S.set(x + 1, g + 1, z + 1, "minecraft:barrel[facing=up,open=false]")
            S.set(x - 1, g + 1, z + 1, "minecraft:barrel[facing=up,open=false]")
            for u in (-1, 0, 1):
                S.set(x + u, g + 1, z - 2, "minecraft:green_carpet" if u else "minecraft:white_carpet")
        elif k == 1:    # a glowing fairy ring
            for a in range(12):
                aa = a * math.pi / 6
                S.set(x + round(2.8 * math.cos(aa)), g + 1, z + round(2.8 * math.sin(aa)), "glimpaddenstoel")
            S.set(x, g, z, "minecraft:shroomlight")
        elif k == 2:    # a fallen log with moss and little mushrooms
            for u in range(-2, 3):
                S.set(x + u, g + 1, z, "minecraft:spruce_log[axis=x]")
                if u % 2:
                    S.set(x + u, g + 2, z, "minecraft:moss_carpet")
            S.set(x, g + 2, z, "minecraft:brown_mushroom")
            S.set(x + 2, g + 2, z, "glimpaddenstoel")
        else:           # a spore lamp post
            S.fill(x, g + 1, z, x, g + 3, z, "minecraft:spruce_fence")
            S.set(x, g + 4, z, "minecraft:shroomlight")
            S.set(x + 1, g + 1, z, "glimpaddenstoel")
    # light beams: a glowing clearing (the beam itself = client particles + light blocks)
    for (x, z) in KA.LICHTSTRALEN:
        g = K.h(x, z)
        for u in range(-5, 6):
            for v in range(-5, 6):
                if u * u + v * v <= 25 and rng.random() < 0.5:
                    S.set(x + u, K.h(x + u, z + v) + 1, z + v, rng.choice(["minecraft:pink_petals[facing=north,flower_amount=4]",
                                                                           "glimpaddenstoel", "minecraft:short_grass"]))
        for dy in (3, 9, 15):
            S.set(x, g + dy, z, "minecraft:light[level=13,waterlogged=false]")
    P["lichtstralen"] = [[x, K.h(x, z) + 1, z] for (x, z) in KA.LICHTSTRALEN]
    # the hollow fallen mushroom with Mika clue 3
    hx, hz = -380, 60
    g = K.h(hx, hz)
    for x in range(hx - 9, hx + 10):
        for y in range(g - 1, g + 8):
            for z in range(hz - 4, hz + 5):
                d = math.hypot(y - (g + 3), z - hz)
                if d <= 4.2:
                    S.set(x, y, z, STEEL if d > 3 else CARVE)
    hoed(S, hx + 13, hz, g + 6, 7, ROOD, "minecraft:white_wool")
    for x in range(hx - 9, hx + 10):      # fix round 1: a flat walkable floor (cobble/gravel) through the whole stem
        for z in range(hz - 2, hz + 3):
            S.set(x, g, z, ["minecraft:cobblestone", "minecraft:gravel", "minecraft:mossy_cobblestone"][(x * 3 + z) % 3])
            S.set(x, g - 1, z, STEEL)
            for y in range(g + 1, g + 4):
                S.set(x, y, z, CARVE)
    for (x_, d_) in ((hx - 10, -1), (hx + 10, 1)):
        trap_naar_buiten(S, K, x_, hz, d_, 0, g, 5)
    S.set(hx, g, hz, "minecraft:black_carpet")
    S.set(hx + 2, g, hz + 1, "minecraft:black_wool")
    P["mika_aanwijzing_3"] = [hx, g, hz]
    # waterfall rock with a room behind it
    wx, wz = -470, 0
    g = K.h(wx, wz)
    S.ellipsoid(wx, g + 6, wz, 13, 16, 9, "minecraft:mossy_cobblestone")
    S.ellipsoid(wx, g + 6, wz, 12, 15, 8, "minecraft:stone", keep=False)
    S.ellipsoid(wx, g + 6, wz, 11, 14, 7, CARVE)
    # fix round 1: the egg-shaped cavity went 8 blocks below the floor (a pit around the floor): fill it up to the floor
    for x in range(wx - 12, wx + 13):
        for z in range(wz - 8, wz + 9):
            for y in range(g - 9, g + 1):
                if S.get(x, y, z) == CARVE:
                    S.set(x, y, z, "minecraft:stone" if y < g else ["minecraft:stone", "minecraft:cobblestone", "minecraft:gravel", "minecraft:cobblestone"][(x + 2 * z) % 4])
    S.fill(wx - 4, g + 1, wz - 3, wx + 4, g + 5, wz + 3, CARVE)
    for x in range(wx - 5, wx + 6):       # fix round 1: a stone/cobble floor instead of moss
        for z in range(wz - 4, wz + 5):
            S.set(x, g, z, ["minecraft:stone", "minecraft:cobblestone", "minecraft:gravel", "minecraft:cobblestone"][(x + 2 * z) % 4])
    S.set(wx, g + 1, wz - 2, "minecraft:chest[facing=south,type=single,waterlogged=false]")
    S.set(wx - 3, g + 1, wz, "minecraft:ochre_froglight[axis=y]")
    S.set(wx + 3, g + 1, wz, "minecraft:ochre_froglight[axis=y]")
    # fix round 1: a clearing in front of the waterfall at the room's floor level, the floor runs on under the curtain
    # into the room, and stairs lead from the clearing up to the forest (it used to be a pool in a pit)
    for x in range(wx - 7, wx + 8):
        for z in range(wz + 10, wz + 24):
            for y in range(g + 1, g + 14):
                S.set(x, y, z, CARVE)
            for y in range(g - 3, g):
                S.set(x, y, z, "minecraft:stone")
            S.set(x, g, z, "minecraft:moss_block" if (x + z) % 3 else "minecraft:grass_block[snowy=false]")
    S.fill(wx - 1, g, wz + 7, wx + 1, g, wz + 10, "minecraft:cobblestone")
    S.fill(wx - 1, g + 1, wz + 7, wx + 1, g + 3, wz + 9, CARVE)       # entrance (south) behind the water
    for y in range(g + 1, g + 20):
        for x in range(wx - 1, wx + 2):
            S.set(x, y, wz + 10, "minecraft:water")
    for x in range(wx - 4, wx + 5):
        for z in range(wz + 11, wz + 17):
            S.set(x, g, z, "minecraft:water")
            S.set(x, g - 1, z, "minecraft:clay")
            if abs(x - wx) == 4 or z == wz + 16:
                S.set(x, g, z, "minecraft:mossy_cobblestone")
    trap_naar_buiten(S, K, wx, wz + 24, 0, 1, g, 5, "minecraft:mossy_cobblestone_stairs", "minecraft:mossy_cobblestone")
    P["geheim_waterval"] = [wx, g + 1, wz]
    P["waterval"] = [wx, g + 1, wz + 12]
    W.stempel(S.stempel("sporenwoud"), bezet=False)
    for (x, z, r, _) in gekozen:
        i, j = KA.idx(x, z)
        W.bezet[i - 2:i + 3, j - 2:j + 3] = True


# ================================================================================================ edge clouds
def randwolken(W, K, P):
    rng = random.Random(8)
    S = None
    stempels = []
    pts = []
    for x in range(KA.X0 + 10, KA.X0 + KA.NX - 10, 46):
        pts += [(x, KA.Z0 + 14), (x, KA.Z0 + KA.NZ - 14)]
    for z in range(KA.Z0 + 10, KA.Z0 + KA.NZ - 10, 46):
        pts += [(KA.X0 + 14, z), (KA.X0 + KA.NX - 14, z)]
    for (x, z) in pts:
        y = rng.randint(66, 90)
        S = WS(x - 22, y - 8, z - 22, 45, 20, 45)
        for _ in range(4):
            S.ellipsoid(x + rng.uniform(-10, 10), y + rng.uniform(-2, 3), z + rng.uniform(-10, 10), rng.uniform(7, 14),
                        rng.uniform(3, 6), rng.uniform(7, 14), rng.choice(["wolkenblok_wit", "wolkenblok_wit", "wolkenblok_roze"]))
        W.stempel(S.stempel("wolk"), bezet=False)


# ================================================================================================ T-WERELD: barriers + harbours
def splinterwand(W, K, P):
    """The rock spires of the Splinterwand that stand in the zone-1 grid (the same list as zone 2 and the gap row)."""
    import barriere
    n = 0
    for st in barriere.stempels(TG.Stempel, doos=(KA.X0, KA.Z0, KA.X0 + KA.NX - 1, KA.Z0 + KA.NZ - 1)):
        W.stempel(st, bezet=False)
        n += 1
    print(f"  splinterwand: {n} spitsen in zone 1")


def noordklifhaven(W, K, P):
    """haven_z1_z2_vertrek: the Noordklifhaven on the north cliffs (line A to Kandijkom-dok): a plaza (flattened in kaart.py),
    a pier north over the cliff with the departure platform, mast and board; the platform's far corner is the viewpoint
    Noordkliftop (U1) towards the Splinterwand and zone 2."""
    import havens
    cx, cz = KA.NOORDKLIF
    y = K.noordklif_y
    x0, z0, x1, z1 = havens.doos(cx, cz - KA.NOORDKLIF_R + 2, 0, -1, 34)
    S = WS(x0, 30, z0, x1 - x0 + 1, y - 30 + 20, z1 - z0 + 1)
    # the plaza: a ring of kerb stones, benches, lanterns
    for a in range(0, 360, 30):
        x, z = cx + (KA.NOORDKLIF_R - 1) * math.cos(math.radians(a)), cz + (KA.NOORDKLIF_R - 1) * math.sin(math.radians(a))
        if -120 < a - 270 < 120 and abs(a - 270) < 25:
            continue
        S.set(x, y + 1, z, LANT if a % 60 == 0 else "minecraft:spruce_slab[type=bottom,waterlogged=false]")

    def hoog(x, z):
        return hoogste(W, K, int(x), int(z))
    havens.pier(S, cx, y, cz - KA.NOORDKLIF_R + 2, 0, -1, 34, hoog, "haven_z1_z2", P, bodem=40)
    P["haven_z1_z2_plein"] = [cx, y + 1, cz]
    e = P["haven_z1_z2_eind"]
    # U-OVERGANG: no viewpoint at the harbour any more (feedback ronde 4: epic spots instead, see stad_wereld.hoedbalkon)
    W.stempel(S.stempel("noordklifhaven"))


# ================================================================================================ decoration
_PADAFSTAND = {}


def padafstand(W, K):
    """Distance (blocks) of every column to the nearest walking surface: map paths, the Kloof ramps and the dirt-path /
    klinker floors the stamps lay (campfire plazas)."""
    if "a" not in _PADAFSTAND:
        from scipy import ndimage
        pad = K.pad > 0.5
        _PADAFSTAND["a"] = ndimage.distance_transform_edt(~pad).astype(np.float32)
    return _PADAFSTAND["a"]


def decoratie(W, K, P):
    P_ = W.punten
    rng = np.random.default_rng(42)
    r = rng.random((KA.NX, KA.NZ))
    r2 = rng.random((KA.NX, KA.NZ))
    H = K.H
    top = W.top
    pal = W.palet
    groen = {pal.id(s) for s in ("minecraft:grass_block[snowy=false]", "guhs:weide_roze_gras", "guhs:knuffelgras")}
    mos = {pal.id(s) for s in ("minecraft:moss_block", "minecraft:podzol[snowy=false]", "minecraft:mycelium[snowy=false]")}
    lit = pal.id("guhs:vr_w_litteken_gras")
    ok = (H >= 63) & ~W.bezet & (K.pad < 0.1) & (H >= np.round(K.wl))
    # trees: trunk 6+ blocks from any path surface (the crown is up to 3.4 wide) and 5+ from buildings/needles
    from scipy import ndimage
    boom_ok = (padafstand(W, K) >= 6) & ~ndimage.binary_dilation(W.bezet, iterations=5)
    sx, sz = np.nonzero(ok)
    namen = K.namen
    bloemen_weide = ["minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy", "minecraft:cornflower",
                     "guhs:roze_guhbloem", "guhs:guhoortjes", "minecraft:azure_bluet", "minecraft:pink_petals[facing=north,flower_amount=4]"]
    bomen = []
    for i, j in zip(sx, sz):
        t = top[i, j]
        x, z = int(i + KA.X0), int(j + KA.Z0)
        y = int(H[i, j]) + 1
        gb = namen[K.gebied[i, j]]
        q = r[i, j]
        if t == lit:
            if q < 0.12:
                P_.set(x, y, z, "guhs:vr_w_litteken_plant")
            elif q > 0.9985 and boom_ok[i, j]:
                bomen.append((x, y, z, "dood"))
            continue
        if t in groen:
            if gb in ("weide", "zuid", "zuidoost", "zuidwest", "stad", "tempel"):
                if q < 0.22:
                    P_.set(x, y, z, "minecraft:short_grass")
                elif q < 0.27:
                    P_.set(x, y, z, bloemen_weide[int(r2[i, j] * len(bloemen_weide))])
                elif q < 0.29:
                    P_.set(x, y, z, "minecraft:tall_grass[half=lower]")
                    P_.set(x, y + 1, z, "minecraft:tall_grass[half=upper]")
                elif q > 0.9975 and gb != "stad" and boom_ok[i, j]:
                    bomen.append((x, y, z, "bloesem" if r2[i, j] < 0.5 else "azalea"))
                elif q > 0.9965 and gb == "weide":
                    P_.set(x, y, z, "guhs:roze_slijmblok" if r2[i, j] < 0.5 else "minecraft:slime_block")
            elif gb == "grotten":
                if q < 0.08:
                    P_.set(x, y, z, "minecraft:short_grass")
                elif q < 0.09:
                    P_.set(x, y, z, "minecraft:fern")
            else:
                if q < 0.15:
                    P_.set(x, y, z, "minecraft:short_grass")
        elif t in mos:
            if q < 0.06:
                P_.set(x, y, z, "minecraft:moss_carpet")
            elif q < 0.09:
                P_.set(x, y, z, "minecraft:fern")
            elif q < 0.105:
                P_.set(x, y, z, "minecraft:red_mushroom" if r2[i, j] < 0.5 else "minecraft:brown_mushroom")
            elif q < 0.112:
                P_.set(x, y, z, "glimpaddenstoel")
            elif q < 0.116:
                P_.set(x, y, z, "schimmelpaddenstoel")
            elif q < 0.118:
                P_.set(x, y, z, "minecraft:firefly_bush")
        elif gb == "grotten" and q < 0.006:
            P_.set(x, y, z, "minecraft:pointed_dripstone[thickness=tip,vertical_direction=up,waterlogged=false]")
    for (x, y, z, soort) in bomen:
        boom(P_, x, y, z, soort, random.Random(x * 31 + z))
    # ponds: lilies + reed; big ponds get little stepping islands so land is never more than ~10 swims away (fix round 1)
    for (px, pz, rx, rz) in KA.VIJVERS:
        if rx > 18:
            for k in (-1, 0, 1):
                ix, iz = int(px + k * rx * 0.5), int(pz)
                wl = int(round(K.wl[KA.idx(ix, iz)]))
                for u in range(-1, 2):
                    for v in range(-1, 2):
                        bodem = K.h(ix + u, iz + v)
                        for yy in range(bodem + 1, wl):
                            P_.set(ix + u, yy, iz + v, "minecraft:mossy_cobblestone")
                        P_.set(ix + u, wl, iz + v, "minecraft:moss_block")
                P_.set(ix, wl + 1, iz, "guhs:bloesemmeertje_riet[half=lower]")
                P_.set(ix, wl + 2, iz, "guhs:bloesemmeertje_riet[half=upper]")
    for (px, pz, rx, rz) in KA.VIJVERS:
        rr = random.Random(px)
        for _ in range(int(rx * rz * 0.15)):
            a, d = rr.uniform(0, 6.283), rr.uniform(0, 0.9)
            x, z = int(px + rx * d * math.cos(a)), int(pz + rz * d * math.sin(a))
            i, j = KA.idx(x, z)
            P_.set(x, int(round(K.wl[i, j])) + 1, z, "minecraft:lily_pad" if rr.random() < 0.7 else "guhs:guh_waterlelie")
        for _ in range(int(rx * 2)):
            a = rr.uniform(0, 6.283)
            x, z = int(px + rx * 1.1 * math.cos(a)), int(pz + rz * 1.1 * math.sin(a))
            y = K.h(x, z) + 1
            P_.set(x, y, z, "guhs:bloesemmeertje_riet[half=lower]")
            P_.set(x, y + 1, z, "guhs:bloesemmeertje_riet[half=upper]")


def boom(P_, x, y, z, soort, rng):
    if soort == "dood":
        h = rng.randint(4, 7)
        for k in range(h):
            P_.set(x, y + k, z, "minecraft:pale_oak_log[axis=y]")
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1)):
            if rng.random() < 0.6:
                P_.set(x + dx, y + h - 2, z + dz, "minecraft:pale_oak_log[axis=x]" if dx else "minecraft:pale_oak_log[axis=z]")
        P_.set(x, y + h - 3, z + 1, "minecraft:pale_hanging_moss[tip=true]")
        return
    log, blad = {"bloesem": ("guhs:guhbloesem_log[axis=y]", "guhs:guhbloesem_leaves"),
                 "azalea": ("minecraft:oak_log[axis=y]", "minecraft:flowering_azalea_leaves")}[soort]
    h = rng.randint(4, 6)
    for k in range(h):
        P_.set(x, y + k, z, log)
    rr = rng.uniform(2.3, 3.4)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(-1, 3):
                if dx * dx + dz * dz + (dy * 1.4) ** 2 <= rr * rr and not (dx == 0 and dz == 0 and dy < 1):
                    P_.set(x + dx, y + h - 1 + dy, z + dz, blad)


# ================================================================================================ all
def bouw_alles(W, K, kamp_d):
    P = {}
    import json
    zwamtempel(W, K, P)
    haven(W, K, P)
    arena_klodder(W, K, P)
    slaapheuvel(W, K, P)
    for naam, (x, z) in KA.UITKIJK.items():
        naald(W, K, P, naam, x, z, {"druipspits": 88, "klodderkijk": 74, "zwamnaald": 80, "haventand": 66, "slijmnaald": 70}[naam])
    fossielbrug(W, K, P)
    schildpad(W, K, P)
    stal(W, K, P)
    kampvuren(W, K, P)
    kamp_zuidsteiger(W, K, P, kamp_d)
    rondvlucht(W, K, P)
    grotten(W, K, P)
    sporenwoud(W, K, P)
    __import__("hoedenpaden").bouw(W, K, P)  # verticaal: De Hoedenpaden (wereld/hoedenpaden.py)
    __import__("druipschacht").bouw(W, K, P)  # verticaal: De Druipschacht (wereld/druipschacht.py)
    # T-WERELD: randwolken (the clouds along the RECTANGLE of the tile grid) are gone: the coast runs out naturally; the
    # Splinterwand (zone 1 -> 2) and the Noordklifhaven take their place
    splinterwand(W, K, P)
    noordklifhaven(W, K, P)
    terugportalen(W, K, P)
    decoratie(W, K, P)
    npc_wensen(W, K, P, kamp_d)
    return {"wereld": P}


def hoogste(W, K, x, z, kamp_d=None, tot=250):
    """The highest solid block in this column (stamps + terrain): stand y - 1."""
    for y in range(tot, 0, -1):
        i = W.blok_op(x, y, z)
        if i > 1:
            naam = W.palet.lijst[i]
            if not any(t in naam for t in ("grass", "flower", "tulip", "allium", "petals", "fern", "carpet", "lantern", "guhbloem",
                                            "guhoortjes", "daisy", "cornflower", "bluet", "mushroom[", "litteken_plant", "leaves")):
                return y
    return max(K.h(x, z), 0)


def grond_onder_hoed(W, K, x, z):
    """W-INT: the first free stand cell above the real ground in this column (under any mushroom cap / tree crown)."""
    g = max(K.h(x, z), 0)
    if W.blok_op(x, g + 1, z) <= 1 and W.blok_op(x, g + 2, z) <= 1:   # the terrain itself (no stamp on it)
        return g + 1
    for y in range(g - 3, g + 12):
        if W.blok_op(x, y, z) > 1 and W.blok_op(x, y + 1, z) <= 1 and W.blok_op(x, y + 2, z) <= 1:
            return y + 1
    return g + 1


def npc_wensen(W, K, P, kamp_d):
    """POI_VERZOEKEN (npc): the 10 lost knuffels, the quest decorations, Mopje's cave spot."""
    knuffels = ["knuffel_normal", "knuffel_mint", "knuffel_starry", "knuffel_pinguh", "knuffel_wolk", "knuffel_choco",
                "knuffel_rainbow", "knuffel_snow", "knuffel_glitter", "knuffel_teckel"]
    spots = [(-68 + KDX, 952 + KDZ), (30 + KDX, 1040 + KDZ), (-36, 33), (70, -70), (116, 466), (300, 250), (470, 185), (-400, -100), (-300, -226), (326, -374)]
    for i, ((x, z), k) in enumerate(zip(spots, knuffels)):
        y = hoogste(W, K, x, z) + 1
        if i == 7:   # W-INT: knuffel 8 ("Mosje") landed on a giant mushroom cap 36 up (no way to climb): on the forest floor under the cap
            y = grond_onder_hoed(W, K, x, z)
        W.punten.set(x, y, z, "guhs:" + k)
        P[f"knuffel_{i + 1}"] = [x, y, z]
    # Dobber's lost hat near the lighthouse: a straw hat on the grass
    x, z = -60 + KDX, 955 + KDZ
    y = hoogste(W, K, x, z) + 1
    W.punten.set(x, y, z, "minecraft:hay_block[axis=y]")
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        W.punten.set(x + dx, y, z + dz, "minecraft:yellow_carpet")
    W.punten.set(x, y + 1, z, "minecraft:red_carpet")
    P["quest_dobber_hoed"] = [x, y + 1, z]
    # Muzikant Mo's picnic spot with the tuba (gold + note block) in the SW Sporenwoud
    x, z = -470, 330
    y = hoogste(W, K, x, z) + 1
    for dx in range(-2, 3):
        for dz in range(-2, 2):
            W.punten.set(x + dx, y, z + dz, "minecraft:red_carpet" if (dx + dz) % 2 else "minecraft:white_carpet")
    W.punten.set(x + 3, y, z, "minecraft:note_block[instrument=harp,note=0,powered=false]")
    W.punten.set(x + 3, y + 1, z, "minecraft:gold_block")
    W.punten.set(x + 4, y, z, "minecraft:gold_block")
    W.punten.set(x + 3, y + 2, z, "minecraft:lightning_rod[facing=up,powered=false]")
    P["quest_tuba"] = [x + 3, y, z]
    # the Hoedhupperwei: a meadow with little mushroom deco
    rng = random.Random(52)
    for _ in range(40):
        x, z = 520 + rng.randint(-14, 14), 300 + rng.randint(-14, 14)
        y = hoogste(W, K, x, z) + 1
        W.punten.set(x, y, z, rng.choice(["minecraft:red_mushroom", "minecraft:brown_mushroom", "glimpaddenstoel", "schimmelpaddenstoel"]))
    P["hoedhupperwei"] = [520, hoogste(W, K, 520, 300) + 1, 300]
    # Mijnwerker Mopje in cave 2 (the Druipgrotten node N2, floor ~y 41)
    P["npc_mopje"] = P["grot_knopen"]["N2"]


def poortje(S, cx, y, z, breed=5, hoog=6, as_="x"):
    """A small portal gate (frame of Vadsrijk-poortsteen, an awake portal inside) standing on floor y-1."""
    for u in range(-breed // 2 - 1, breed // 2 + 2):
        for v in range(-1, hoog + 1):
            rand = u in (-breed // 2 - 1, breed // 2 + 1) or v in (-1, hoog)
            pos = (cx + u, y + v, z) if as_ == "x" else (z, y + v, cx + u)
            S.set(*pos, "vr_w_poortsteen" if rand else f"vr_w_portaal[axis={as_},wakker=true]")
    top = (cx, y + hoog + 1, z) if as_ == "x" else (z, y + hoog + 1, cx)
    S.set(*top, "minecraft:shroomlight")
    for kant in (-1, 1):
        Z.oortje(S, cx + kant * 2, y + hoog + 1, z, "x") if as_ == "x" else Z.oortje(S, z, y + hoog + 1, cx + kant * 2, "z")


def terugportalen(W, K, P):
    """The way home from inside: an awake portal on the Knuffelkamp south jetty, in Zwamzoet and in De Haven."""
    for naam, (cx, y, z) in (("kamp", (0, 65, 1153)), ("zwamzoet", (-43, 64, 28)), ("haven", (111, 65, 472))):
        S = WS(cx - 8, y - 3, z - 4, 17, 16, 9)
        S.fill(cx - 4, y - 1, z - 2, cx + 4, y - 1, z + 2, "minecraft:spruce_planks" if naam == "kamp" else "tegel_hartjes")
        poortje(S, cx, y, z)
        if naam == "kamp":
            P[f"terugportaal_{naam}"] = kamp_p([cx, y, z + 2])
            W.stempel(kamp_stempel(S, "terugportaal_" + naam))
            continue
        P[f"terugportaal_{naam}"] = [cx, y, z + 2]
        W.stempel(S.stempel("terugportaal_" + naam))


def deurkopie(W, deur, kopie_y=8):
    """The hidden copy of a closed door deep under the ground (WereldDeuren rebuilds the door from it)."""
    (x0, y0, z0), (x1, y1, z1) = deur["min"], deur["max"]
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                i = W.blok_op(x, y, z)
                if i > 1:
                    W.punten.per.setdefault((x // 64, z // 64), []).append((x, kopie_y + y - y0, z, i))
    deur["kopie_y"] = kopie_y
