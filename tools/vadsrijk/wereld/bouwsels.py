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
    S = WS(262, TP - 12, -535, 150, 110, 165)
    # ---- terrace + front stairs (the path arrives at z -386)
    S.fill(300, TP, -394, 360, TP, -376, "tegel_hartjes")
    for i in range(6):
        S.fill(318 + i, TP - 6 + i, -376 + 6 - i, 342 - i, TP - 6 + i, -376 + 6 - i, ST("guhs:knuffelsteen_trap", "north"))
    # ---- the inside: a straight chain north (bazen slice's layout): voorhal -> Lichtzwamkamer -> Hendelkamer -> Ridderzaal
    #      -> the Opperzwam hall. The gates between them are open AIR (5 wide, 4 high); bazen fills them with its own gate blocks.
    def doos(x1, z1, x2, z2, h, wand=ROZE, dak=FRAMB):
        S.fill(x1, TP, z1, x2, TP, z2, "tegel_hartjes")
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
                S.fill(x, TY + v, -393, x, TY + v, -391, CARVE)
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
    S.fill(308, TP, -421, 315, TP, -411, "tegel_hartjes")
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
                S.set(x, TP, z, "pastelbaksteen_framboos" if ring == 0 else ("gladde_knuffelsteen" if ring % 2 else "tegel_hartjes"))
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
    S.cylinder(cx, cz, 5, g - 3, g, KSTEEN)
    S.cylinder(cx, cz, 5, g, g, "knuffelklinkers")
    for y in range(g + 1, top):
        for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
            S.set(cx + dx, y, cz + dz, "minecraft:stripped_cherry_log[axis=y]")
        if y % 8 == 0:
            S.walls(cx - 2, y, cz - 2, cx + 2, y, cz + 2, "minecraft:cherry_planks")
    for y in range(g + 1, top + 1):     # ladder inside
        S.set(cx, y, cz + 1, "minecraft:ladder[facing=north,waterlogged=false]")
        S.set(cx, y, cz + 2, "minecraft:cherry_planks")
    S.fill(cx - 4, top, cz - 4, cx + 4, top, cz + 4, "minecraft:cherry_planks")
    S.set(cx, top, cz + 1, "minecraft:cherry_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]")
    S.walls(cx - 4, top + 1, cz - 4, cx + 4, top + 1, cz + 4, "minecraft:cherry_fence")
    for (dx, dz) in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        S.set(cx + dx, top + 2, cz + dz, LANT)
    S.fill(cx, top + 1, cz - 3, cx, top + 8, cz - 3, "minecraft:cherry_log[axis=y]")
    S.set(cx, top + 9, cz - 3, "minecraft:shroomlight")
    Z.oortje(S, cx, top + 10, cz - 3, "z")


def haven(W, K, P):
    g = 64
    S = WS(10, 40, 410, 130, 100, 170)
    # quay along the bay shore + a pier into the bay
    for z in range(425, 575):
        oever = None
        for x in range(30, 130):
            if K.h(x, z) >= 0:
                oever = x
                break
        if oever is None:
            continue
        for x in range(oever, oever + 4):
            for y in range(g - 4, g + 1):
                S.set(x, y, z, KSTEEN if y < g else "knuffelklinkers")
        S.set(oever, g + 1, z, "minecraft:cherry_fence" if z % 7 else LANT)
    for x in range(30, 72):
        for z in range(468, 473):
            S.set(x, g, z, "minecraft:cherry_planks")
        if x % 5 == 0:
            for z in (467, 473):
                S.fill(x, g - 4, z, x, g, z, "minecraft:cherry_log[axis=y]")
                S.set(x, g + 1, z, LANT if x % 10 == 0 else "minecraft:cherry_fence")
    for (bx, bz) in ((40, 478), (52, 462)):
        S.fill(bx - 3, g - 1, bz - 1, bx + 3, g - 1, bz + 1, "minecraft:cherry_planks")
        S.fill(bx - 3, g, bz - 1, bx + 3, g, bz - 1, "roze_lakhout_planken")
        S.fill(bx - 3, g, bz + 1, bx + 3, g, bz + 1, "roze_lakhout_planken")
        S.fill(bx, g, bz, bx, g + 6, bz, "minecraft:cherry_fence")
        for i in range(5):
            S.fill(bx + 1, g + 1 + i, bz, bx + 4 - i, g + 1 + i, bz, "minecraft:white_wool")
    # the airship mast + the moored airship + the bell
    mx, mz, mtop = 92, 500, g + 50
    mast(S, mx, mz, g, mtop)
    luchtschip(S, mx - 32, mz - 3, mtop + 2, mast_x=mx - 5, richting=1)
    S.set(mx - 6, g + 1, mz - 12, "minecraft:bell[attachment=floor,facing=west,powered=false]")
    S.fill(mx - 8, g, mz - 14, mx - 4, g, mz - 10, "tegel_hartjes")
    P["luchtschip_haven_bel"] = [mx - 6, g + 1, mz - 12]
    P["luchtschip_haven_kapitein"] = [mx - 6, g + 1, mz - 9]
    P["luchtschip_haven_mast_top"] = [mx, mtop + 1, mz]
    # warehouse (pakhuis) with Mika clue 2
    x0, z0 = 104, 515
    Z.huis(S, x0, g + 1, z0, 13, 11, 6, "west", Z.PALETTEN[2])
    for k in range(3):
        S.set(x0 + 9, g + 1, z0 + 2 + k * 3, "minecraft:barrel[facing=up,open=false]")
        S.set(x0 + 10, g + 1, z0 + 2 + k * 3, "minecraft:barrel[facing=up,open=false]")
    for (dx, dz) in ((3, 4), (4, 5), (5, 6)):
        S.set(x0 + dx, g, z0 + dz, "mika_steen")
    P["mika_aanwijzing_2"] = [x0 + 4, g + 1, z0 + 5]
    P["haven_pakhuis"] = [x0 - 1, g + 1, z0 + 5]
    # houses + stalls + lamp posts + campfire
    for i, (x, z, v) in enumerate(((100, 430, "west"), (112, 446, "west"), (100, 540, "west"), (114, 558, "north"))):
        Z.huis(S, x, g + 1, z, 7, 7, 5, v, Z.PALETTEN[i % 5])
    for i, z in enumerate((480, 488, 532)):
        Z.kraam.__globals__["G"] = g
        Z.kraam(S, 80, z, ["rood", "geel", "blauw"][i])
    Z.kraam.__globals__["G"] = 8
    for z in range(430, 570, 12):
        S.set(86, g + 1, z, "guh_lantaarnpaal[half=lower]")
        S.set(86, g + 2, z, "guh_lantaarnpaal[half=upper]")
    S.set(100, g + 1, 452, VUUR)
    P["kampvuur_haven"] = [100, g + 1, 452]
    P["haven_plein"] = [90, g + 1, 470]
    W.stempel(S.stempel("haven"))


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
    S = WS(cx - rx - 2, g - 3, cz - rz - 2, 2 * rx + 5, ry + 22, 2 * rz + 5)
    rng = random.Random(5)
    for x in range(cx - rx, cx + rx + 1):
        for z in range(cz - rz, cz + rz + 1):
            e = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if e > 1:
                continue
            top = g + int(ry * math.sqrt(1 - e) ** 1.15)
            for y in range(max(g - 1, top - 2), top + 1):
                b = "minecraft:lime_wool" if rng.random() < 0.75 else "minecraft:lime_concrete"
                if y == top and e < 0.25 and rng.random() < 0.7:
                    b = "minecraft:moss_block"
                S.set(x, y, z, b)
            # the inside goes down to the ground (sides)
            for y in range(g - 1, max(g - 1, top - 2)):
                if e > 0.85:
                    S.set(x, y, z, "minecraft:lime_wool")
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
    kz = cz - rz + 26
    S.ellipsoid(cx, g + 2, kz, 5, 4, 5, "minecraft:lime_terracotta")
    S.ellipsoid(cx, g + 2, kz, 4, 3, 4, CARVE)
    S.fill(cx - 4, g - 1, kz - 4, cx + 4, g - 1, kz + 4, "minecraft:lime_terracotta")
    S.set(cx, g, kz + 2, "minecraft:chest[facing=north,type=single,waterlogged=false]")
    S.set(cx + 2, g, kz, "minecraft:ochre_froglight[axis=y]")
    S.set(cx - 2, g, kz, "minecraft:ochre_froglight[axis=y]")
    P["slaapheuvel"] = [cx, g + ry + 1, cz]
    P["geheim_slaapheuvel"] = [cx, g, kz + 2]
    P["geheim_slaapheuvel_ingang"] = [cx, g, cz - rz - 2]
    W.stempel(S.stempel("slaapheuvel"))


# ================================================================================================ needles, bridge, turtle
def naald(W, K, P, naam, x, z, hoogte):
    g = K.h(x, z)
    top = g + hoogte
    S = WS(x - 12, g - 4, z - 12, 25, hoogte + 18, 25)
    lagen = ["minecraft:stone", "minecraft:andesite", "minecraft:calcite", "minecraft:stone", "minecraft:tuff", "minecraft:diorite"]
    rng = random.Random(hash(naam) & 0xFFFF)
    for y in range(g - 3, top + 1):
        t = (y - g) / hoogte
        r = 8.5 * (1 - t) ** 0.7 + 3.2
        for xx in range(x - 11, x + 12):
            for zz in range(z - 11, z + 12):
                a = math.atan2(zz - z, xx - x)
                rr = r * (1 + 0.12 * math.sin(3 * a + y * 0.15) + 0.06 * math.sin(7 * a))
                d = math.hypot(xx - x, zz - z)
                if d <= rr:
                    b = lagen[((y + int(2 * math.sin(a * 2))) // 3) % len(lagen)]
                    if rng.random() < 0.08:
                        b = "minecraft:moss_block"
                    S.set(xx, y, zz, b)
    # spiral stair around it
    a = 0.0
    for y in range(g + 1, top + 1):
        t = (y - g) / hoogte
        r = 8.5 * (1 - t) ** 0.7 + 3.2 + 1.6
        for k in range(3):
            aa = a + k * 0.5 / r
            for dr in (0.0, 0.9, 1.8):
                S.set(x + (r + dr) * math.cos(aa), y, z + (r + dr) * math.sin(aa), "minecraft:spruce_slab[type=top,waterlogged=false]", keep=True)
            S.set(x + (r + 2.7) * math.cos(aa), y + 1, z + (r + 2.7) * math.sin(aa), "minecraft:spruce_fence", keep=True)
        if (y - g) % 12 == 0:
            S.set(x + (r + 2.7) * math.cos(a), y + 2, z + (r + 2.7) * math.sin(a), LANT, keep=True)
        a += 1.3 / r
    # top platform
    S.cylinder(x, z, 6.5, top + 1, top + 1, "minecraft:spruce_planks")
    S.cylinder(x, z, 6.5, top + 2, top + 2, "minecraft:spruce_fence", hollow=1)
    S.fill(x, top + 2, z, x, top + 7, z, "minecraft:spruce_fence")
    S.set(x, top + 8, z, "minecraft:shroomlight")
    for i in range(4):
        S.fill(x + 1, top + 4 + i, z, x + 4 - i, top + 4 + i, z, "minecraft:pink_wool")
    S.set(x - 3, top + 2, z - 3, LANT)
    # opening in the railing where the stair arrives
    P[f"uitkijk_{naam}"] = [x, top + 2, z]
    W.stempel(S.stempel("naald_" + naam))


def fossielbrug(W, K, P):
    (ax, az), (bx, bz) = KA.FOSSIEL
    y = max(K.h(ax, az), K.h(bx, bz))
    L = math.hypot(bx - ax, bz - az)
    ux, uz = (bx - ax) / L, (bz - az) / L
    px, pz = -uz, ux
    S = WS(min(ax, bx) - 30, y - 30, min(az, bz) - 30, abs(bx - ax) + 61, 60, abs(bz - az) + 61)
    n = int(L * 2)
    for i in range(-10, n + 12):
        t = i / 2
        cx, cz = ax + ux * t, az + uz * t
        for o in np.arange(-2.5, 2.6, 0.5):
            S.set(cx + px * o, y, cz + pz * o, "minecraft:bone_block[axis=y]")
            S.set(cx + px * o, y - 1, cz + pz * o, "minecraft:bone_block[axis=y]")
        for o in (-3, 3):
            S.set(cx + px * o, y, cz + pz * o, "minecraft:calcite")
            S.set(cx + px * o, y + 1, cz + pz * o, "minecraft:diorite_wall")
        # vertebrae hanging below
        if i % 6 == 0:
            for k in range(1, 5):
                S.set(cx, y - 1 - k, cz, "minecraft:bone_block[axis=y]")
    # bone steps down to the ground at both ends (the deck lies at the higher bank)
    for (ex, ez, sx_, sz_) in ((ax - ux * 5, az - uz * 5, -ux, -uz), (bx + ux * 5.5, bz + uz * 5.5, ux, uz)):
        yy = y
        k = 0
        while yy > K.h(int(ex + sx_ * k), int(ez + sz_ * k)) and k < 20:
            k += 1
            yy -= 1
            cx, cz = ex + sx_ * k, ez + sz_ * k
            for o in np.arange(-2.5, 2.6, 0.5):
                S.set(cx + px * o, yy, cz + pz * o, "minecraft:bone_block[axis=y]")
                for d_ in range(1, 4):
                    S.set(cx + px * o, yy - d_, cz + pz * o, "minecraft:calcite")
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
    # the skull at the far end, beside the path, facing back along the bridge, with guh-ear horns
    sx, sz = bx + ux * 8 + px * 9, bz + uz * 8 + pz * 9
    S.ellipsoid(sx, y + 6, sz, 7, 6, 8, "minecraft:bone_block[axis=y]", hollow=1.5)
    S.ellipsoid(sx - ux * 7, y + 3, sz - uz * 7, 4, 3, 5, "minecraft:bone_block[axis=y]", hollow=1.2)
    for kant in (-1, 1):
        ex, ez = sx - ux * 4 + px * kant * 3.5, sz - uz * 4 + pz * kant * 3.5
        S.ellipsoid(ex, y + 8, ez, 1.8, 1.8, 1.8, CARVE)
        S.set(ex, y + 8, ez, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
        # guh-ear horns
        for k in range(10):
            S.set(sx + px * kant * (4 + k * 0.15), y + 12 + k, sz + pz * kant * (4 + k * 0.15), "minecraft:calcite")
            S.set(sx + px * kant * (5 + k * 0.15), y + 12 + k, sz + pz * kant * (5 + k * 0.15), "minecraft:pink_terracotta")
    P["fossielbrug"] = dict(van=[ax, y + 1, az], naar=[bx, y + 1, bz])
    W.stempel(S.stempel("fossielbrug"))


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
    g = 63
    S = WS(-165, g - 4, -18, 40, 30, 60)
    for x in range(-162, -128):
        for z in range(-14, 38):
            S.fill(x, g - 2, z, x, g - 1, z, "minecraft:dirt")
            S.set(x, g, z, "guhs:knuffelgras")
    x0, z0 = -158, -10
    Z.huis(S, x0, g + 1, z0, 15, 13, 7, "east", Z.PALETTEN[3])
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
            g = K.h(x, z)
            S = WS(x - 6, g - 2, z - 6, 13, 10, 13)
            S.fill(x - 3, g, z - 3, x + 3, g, z + 3, "minecraft:coarse_dirt")
            S.fill(x - 2, g, z - 2, x + 2, g, z + 2, "minecraft:dirt_path")
            S.fill(x - 4, g - 1, z - 4, x + 4, g - 1, z + 4, "minecraft:dirt")
            S.set(x, g + 1, z, VUUR)
            for (dx, dz, ax) in ((-3, 0, "z"), (3, 0, "z"), (0, 3, "x")):
                S.set(x + dx, g + 1, z + dz, f"minecraft:spruce_log[axis={ax}]")
                S.set(x + dx + (0 if ax == "z" else 1), g + 1, z + dz + (1 if ax == "z" else 0), f"minecraft:spruce_log[axis={ax}]")
            S.set(x + 3, g + 1, z - 3, "guh_lantaarnpaal[half=lower]")
            S.set(x + 3, g + 2, z - 3, "guh_lantaarnpaal[half=upper]")
            # a little order tent
            for k in range(3):
                for dz in range(-5, -2):
                    S.set(x - 5 + k, g + 1 + k, z + dz, "minecraft:pink_wool")
                    S.set(x - 1 - k, g + 1 + k, z + dz, "minecraft:white_wool")
            P[f"kampvuur_{gebied}_{i + 1}"] = [x, g + 1, z]
            P[f"ridder_{gebied}_{i + 1}"] = [x + 2, g + 1, z + 2]
            W.stempel(S.stempel(f"kampvuur_{gebied}_{i}"))


def kamp_zuidsteiger(W, K, P, kamp_d):
    """Kern's AANKOMST (0, 65, 1130): a low jetty on the south coast of Knuffelkamp + a wooden stair up the cliff."""
    hoog = kamp_d["hoogte"]
    ox, oy, oz = [int(v) for v in kamp_d["off"]]

    def kh(x, z):
        i, j = x - ox, z - oz
        if 0 <= i < hoog.shape[0] and 0 <= j < hoog.shape[1]:
            return int(hoog[i, j])
        return -1
    S = WS(-12, 50, 1080, 25, 50, 80)
    for z in range(1126, 1157):
        for x in range(-2, 3):
            S.set(x, 64, z, "minecraft:spruce_planks")
        if z % 4 == 0:
            for x in (-3, 3):
                S.fill(x, 58, z, x, 65, z, "minecraft:spruce_log[axis=y]")
            S.set(3, 66, z, LANT)
    S.fill(-5, 64, 1146, 5, 64, 1156, "minecraft:spruce_planks")
    S.set(-4, 65, 1147, "guhs:ballonsteiger")
    P["aankomst"] = [0, 65, 1130]
    # stair north up the cliff: one step per block until we meet the plateau
    y, z = 65, 1126
    while y < 88 and z > 1080:
        t = kh(0, z)
        for x in range(-1, 2):
            S.set(x, y, z, ST("minecraft:spruce_stairs", "north"))
            for yy in range(y + 1, y + 4):
                S.set(x, yy, z, CARVE)
            for yy in range(max(58, y - 12), y):
                if S.get(x, yy, z) == "minecraft:air":
                    S.set(x, yy, z, "minecraft:spruce_planks" if yy == y - 1 else "minecraft:stone")
        binnen = t > y + 1
        for x in (-2, 2):
            S.set(x, y + 1, z, "minecraft:stone" if binnen else "minecraft:spruce_fence")
            top = max(kh(x, z), y + 3)
            for yy in range(y + 2, top + 1):
                S.set(x, yy, z, "minecraft:stone")
            S.set(x, y, z, "minecraft:spruce_planks")
        if t > y + 4:
            for x in range(-2, 3):
                S.set(x, y + 4, z, "minecraft:stone")
            if z % 5 == 0:
                S.set(0, y + 3, z, LANT_H)
        if t >= 0 and t <= y - 1:
            break
        y += 1
        z -= 1
    P["aankomst_trap_top"] = [0, y + 1, z]
    W.stempel(S.stempel("kamp_zuidsteiger"))


def kamp_arena(P, kamp_poi):
    vx, vy, vz = kamp_poi["vijver"]
    P["arena_klodderreus"] = dict(centrum=[vx, vy, vz], straal=20, deur=[vx - 22, vy, vz - 8], bord=[vx - 23, vy + 1, vz - 10])


# ================================================================================================ caves (Druipgrotten)
GROT_KNOPEN = {"N1": (-320, 52, -300, 11), "N2": (-400, 48, -380, 12), "N3": (-260, 50, -420, 10), "N4": (-460, 46, -470, 9)}


def grotten(W, K, P):
    x0, z0, x1, z1 = -560, -560, -150, -180
    S = WS(x0, 30, z0, x1 - x0 + 1, 100, z1 - z0 + 1)
    rng = random.Random(12)
    gangen = []
    for (ex, ez), naar in zip(KA.GROT_INGANGEN, ("N1", "N2", "N3")):
        gangen.append(((ex, K.h(ex, ez) - 1, ez), GROT_KNOPEN[naar][:3], 3.6))
    for a, b in (("N1", "N2"), ("N2", "N3"), ("N2", "N4"), ("N1", "N3")):
        gangen.append((GROT_KNOPEN[a][:3], GROT_KNOPEN[b][:3], 3.2))
    holte = set()

    def bol(cx, cy, cz, r):
        for x in range(int(cx - r), int(cx + r) + 1):
            for y in range(int(cy - r * 0.7), int(cy + r * 0.8) + 1):
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
            bol(p[0], p[1], p[2], r + 0.8 * math.sin(i * 0.7))
    for k, (cx, cy, cz, r) in GROT_KNOPEN.items():
        bol(cx, cy, cz, r)
    # crystal chamber + the narrow crack from N4
    kx, ky, kz = KA.KRISTALKAMER
    bol(kx, ky, kz, 12)
    a, b = GROT_KNOPEN["N4"][:3], (kx, ky, kz)
    n = int(math.dist(a, b))
    for i in range(n + 1):
        t = i / n
        x, y, z = [a[k] + (b[k] - a[k]) * t for k in range(3)]
        for dy in (0, 1):
            holte.add((int(x), int(y) + dy, int(z)))
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
            if onder and r < 0.25:
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
    paden = K.pad > 0.1
    for _ in range(6000):
        x, z = rng.randint(x0 + 10, x1 - 10), rng.randint(z0 + 10, z1 - 10)
        i, j = KA.idx(x, z)
        if not woud[i, j] or paden[max(0, i - 6):i + 7, max(0, j - 6):j + 7].any() or W.bezet[i, j]:
            continue
        if any(math.hypot(x - a, z - b) < 18 for (a, b) in KA.LICHTSTRALEN):
            continue
        if any(math.hypot(x - a, z - b) < 24 for (a, b, _) in gekozen):
            continue
        r = rng.uniform(6, 15)
        gekozen.append((x, z, r))
    for (x, z, r) in gekozen:
        g = K.h(x, z)
        h = int(r * rng.uniform(2.2, 3.6))
        k = rng.randrange(4)
        steel = stelen[k]
        rs = max(1.5, r * 0.22)
        krom = rng.uniform(-3, 3)
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
    S.set(hx, g, hz, "minecraft:black_carpet")
    S.set(hx + 2, g, hz + 1, "minecraft:black_wool")
    P["mika_aanwijzing_3"] = [hx, g, hz]
    # waterfall rock with a room behind it
    wx, wz = -470, 0
    g = K.h(wx, wz)
    S.ellipsoid(wx, g + 6, wz, 13, 16, 9, "minecraft:mossy_cobblestone")
    S.ellipsoid(wx, g + 6, wz, 12, 15, 8, "minecraft:stone", keep=False)
    S.ellipsoid(wx, g + 6, wz, 11, 14, 7, CARVE)
    S.fill(wx - 4, g + 1, wz - 3, wx + 4, g + 5, wz + 3, CARVE)
    S.fill(wx - 5, g, wz - 4, wx + 5, g, wz + 4, "minecraft:moss_block")
    S.set(wx, g + 1, wz - 2, "minecraft:chest[facing=south,type=single,waterlogged=false]")
    S.set(wx - 3, g + 1, wz, "minecraft:ochre_froglight[axis=y]")
    S.set(wx + 3, g + 1, wz, "minecraft:ochre_froglight[axis=y]")
    S.fill(wx - 1, g + 1, wz + 8, wx + 1, g + 3, wz + 9, CARVE)       # entrance (south) behind the water
    for y in range(g + 1, g + 20):
        for x in range(wx - 1, wx + 2):
            S.set(x, y, wz + 10, "minecraft:water")
    for x in range(wx - 4, wx + 5):
        for z in range(wz + 11, wz + 17):
            S.set(x, g, z, "minecraft:water")
            S.set(x, g - 1, z, "minecraft:clay")
            if abs(x - wx) == 4 or z == wz + 16:
                S.set(x, g, z, "minecraft:mossy_cobblestone")
    P["geheim_waterval"] = [wx, g + 1, wz]
    P["waterval"] = [wx, g + 1, wz + 12]
    W.stempel(S.stempel("sporenwoud"), bezet=False)
    for (x, z, r) in gekozen:
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


# ================================================================================================ decoration
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
            elif q > 0.9985:
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
                elif q > 0.9975 and gb != "stad":
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
    # ponds: lilies + reed
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
    grotten(W, K, P)
    sporenwoud(W, K, P)
    __import__("hoedenpaden").bouw(W, K, P)  # verticaal: De Hoedenpaden (wereld/hoedenpaden.py)
    __import__("druipschacht").bouw(W, K, P)  # verticaal: De Druipschacht (wereld/druipschacht.py)
    randwolken(W, K, P)
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


def npc_wensen(W, K, P, kamp_d):
    """POI_VERZOEKEN (npc): the 10 lost knuffels, the quest decorations, Mopje's cave spot."""
    knuffels = ["knuffel_normal", "knuffel_mint", "knuffel_starry", "knuffel_pinguh", "knuffel_wolk", "knuffel_choco",
                "knuffel_rainbow", "knuffel_snow", "knuffel_glitter", "knuffel_teckel"]
    spots = [(-68, 952), (30, 1040), (-36, 33), (70, -70), (116, 466), (300, 250), (470, 185), (-400, -100), (-300, -226), (326, -374)]
    for i, ((x, z), k) in enumerate(zip(spots, knuffels)):
        y = hoogste(W, K, x, z) + 1
        W.punten.set(x, y, z, "guhs:" + k)
        P[f"knuffel_{i + 1}"] = [x, y, z]
    # Dobber's lost hat near the lighthouse: a straw hat on the grass
    x, z = -60, 955
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
