"""Ambachtseiland (ontwerp): a cosy craft islet west of the Knuffelkamp, joined to it by a wooden bridge.

bouw(S, ox, oz, kamp_H=None) builds the islet into Scene S whose column (0, 0) is WORLD (ox, oz); scene y + OY = world y
(same vertical frame as kamp.py: sea ZEE=10 -> y 62, plateau 34 -> y 86). All anchors below are WORLD x/z, so this can
later become the real stamp script. kamp_H (optional) = the Knuffelkamp height map in the same scene columns (-1 = sea);
the bridge then lands on the camp's west coast.

Layout (user decisions, 2026-11-15): round square with Meester Knutsel in the middle, 4 workshop huts around it
(Smederij N, Keuken W, Verzamelhuisje S, Viswinkel SE), Kiezelgroeve NW (ore veins as real blocks), Knuffelbosje NE,
Bloemenwei SW, Bijenhofje S, Visvijver SE, Vissteiger W, bridge E. Gatherables are real blocks with a little glitter
(small amethyst buds / crystal clusters) beside them.
"""
import math
import random

import numpy as np

import kamp as KP
import vrkit as K
from vrkit import stairs, slab, Rot

OY = 52
ZEE, VLOER = KP.ZEE, KP.VLOER

# ---- anchors (WORLD x, z) ------------------------------------------------------------------------------------------
MIDDEN = (-610, 1030)          # centre of the islet
HALF = (76, 64)                # half axes of the islet (x, z): ~152 x 128 blocks
PLEIN = (-607, 1030)           # the round square (Meester Knutsel)
PLEIN_R = 10
SMEDERIJ = (-607, 1015, "south")      # front-centre of the hut + facing (door side)
KEUKEN = (-622, 1030, "east")
VERZAMEL = (-607, 1045, "north")
VISWINKEL = (-590, 1044, "west")
GROEVE = (-652, 996)           # Kiezelgroeve (quarry hill)
BOSJE = (-586, 994)            # Knuffelbosje
WEI = (-652, 1054)             # Bloemenwei
BIJEN = (-626, 1077)           # Bijenhofje
VIJVER = (-584, 1072)          # Visvijver
VIJVER_R = (10, 6)
BRUG_Z = 1024                  # bridge centre line (runs east-west)
STEIGER_Z = 1032               # Vissteiger (west coast)

GRAS, AARDE, PAD, KLINKER = KP.GRAS, KP.AARDE, KP.PAD, KP.KLINKER
HART = "guhs:tegel_hartjes"
LAMP_O, LAMP_B = KP.LAMP_O, KP.LAMP_B
HEK = "minecraft:cherry_fence"
GLITTER = "minecraft:small_amethyst_bud"

rng = random.Random(1104)


class Ctx:
    """Scene + world offset + islet height map (a kamp.Eiland, so kamp helpers like lijn/top/boom work on it)."""

    def __init__(self, S, ox, oz, kamp_H):
        self.S, self.ox, self.oz = S, ox, oz
        self.I = KP.Eiland(S)
        self.kamp_H = kamp_H
        self.npcs = []          # (x, y, z, yaw, soort) in scene coords, rendered as GeckoLib NPC guhs
        self.labels = {}        # name -> (x, y, z) scene coords (for the map)
        self.zone = {}          # name -> (x1, z1, x2, z2) scene rect, for the map
        self.hutten = {}        # 1.10.4: hut name -> (voor, deur, binnen) scene (x, y, z): stand spot in the yard, door, inside

    def s(self, wx, wz):
        return int(wx - self.ox), int(wz - self.oz)

    def w(self, x, z):
        return int(x + self.ox), int(z + self.oz)


# ------------------------------------------------------------------------------------------------- terrain
def hoogtekaart(C):
    I = C.I
    X, Z = I.X, I.Z
    mx, mz = C.s(*MIDDEN)
    ax, az = HALF
    xs, zs = np.meshgrid(np.arange(X), np.arange(Z), indexing="ij")
    dx, dz = xs - mx, (zs - mz) * ax / az
    r = np.hypot(dx, dz)
    th = np.arctan2(dz, dx)
    Rr = ax + 4 * np.sin(3 * th + 0.4) + 3 * np.sin(5 * th + 2.1) + 1.5 * np.sin(11 * th + 1.0)
    d = Rr - r
    I.kust = d
    n = K.ruis(11)
    nv = np.vectorize(n)(xs, zs)
    golf = 1.6 * np.sin(xs / 13 + 0.5) * np.cos(zs / 11) + 1.2 * nv
    plateau = VLOER + golf
    gx, gz = C.s(*GROEVE)
    plateau += 12 * np.exp(-((xs - gx) ** 2 + (zs - gz) ** 2) / (2 * 13 ** 2))     # quarry hill
    helling = 2.9 + 1.8 * np.cos(th)        # steep cliffs in the east (bridge), a gentle beach slope in the west
    klif = ZEE + 1 + d * helling + 2 * nv
    h = np.minimum(plateau, klif)
    land = (d >= 0) & (xs < C.s(-528, 0)[0])
    I.H = np.where(land, np.maximum(h, ZEE + 1), -1).round().astype(int)
    # flat building ground: square + huts, pond, bees, bridge landing
    px, pz = C.s(*PLEIN)
    I.vlak(px - 24, pz - 24, px + 24, pz + 24, VLOER, 8)
    vx, vz = C.s(*VIJVER)
    I.vlak(vx - 14, vz - 10, vx + 14, vz + 10, VLOER, 5)
    bx, bz = C.s(*BIJEN)
    I.vlak(bx - 8, bz - 6, bx + 8, bz + 6, VLOER, 4)
    return I


def groeve_vorm(C):
    """Carves the quarry pit into the hill: a terraced bowl that opens to the south-east (towards the square)."""
    I = C.I
    gx, gz = C.s(*GROEVE)
    cx, cz = gx + 4, gz + 5
    C.groeve = set()
    for x in range(gx - 20, gx + 21):
        for z in range(gz - 20, gz + 21):
            if I.H[x, z] < 0:
                continue
            e = math.hypot((x - cx) / 1.15, z - cz)
            if e > 15:
                continue
            doel = VLOER - 1 + 3 * int(max(0.0, e - 5) // 2.5)
            if doel < I.H[x, z]:
                I.H[x, z] = doel
                C.groeve.add((x, z))
    C.zone["Kiezelgroeve"] = (gx - 16, gz - 16, gx + 16, gz + 16)


GROEVE_STEEN = ["guhs:knuffelsteen", "guhs:knuffelsteen", "guhs:roze_knuffelsteen", "minecraft:calcite",
                "guhs:knuffelsteen", "minecraft:andesite"]
ERTSEN = ["guhs:ijzererts_knuffel", "guhs:kopererts_knuffel", "guhs:kolenerts_knuffel", "guhs:ijzererts_knuffel",
          "guhs:kopererts_knuffel", "guhs:gouderts_knuffel"]


def zet_terrein(C):
    """Writes the islet shell (top + exposed sides) and the sea around it inside the islet's box."""
    S, I = C.S, C.I
    H = I.H
    Hp = np.pad(H, 1, constant_values=-1)
    buur = np.minimum.reduce([Hp[:-2, 1:-1], Hp[2:, 1:-1], Hp[1:-1, :-2], Hp[1:-1, 2:]])
    lagen = ["minecraft:stone", "minecraft:andesite", "minecraft:stone", "minecraft:tuff", "minecraft:stone",
             "minecraft:calcite", "minecraft:stone", "minecraft:diorite"]
    mx, mz = C.s(*MIDDEN)
    ax, az = HALF
    x1, x2 = max(0, mx - ax - 22), min(I.X, mx + ax + 22)
    z1, z2 = max(0, mz - az - 22), min(I.Z, mz + az + 22)
    for x in range(x1, x2):
        for z in range(z1, z2):
            h = H[x, z]
            kampland = C.kamp_H is not None and C.kamp_H[x, z] >= 0
            if h < 0:
                if kampland:
                    continue
                dk = -I.kust[x, z]
                if dk < 18:
                    b = "minecraft:sand" if dk < 5 else "minecraft:prismarine" if dk < 13 else \
                        "minecraft:light_blue_terracotta"
                    S.set(x, ZEE - 1, z, b)
                    S.set(x, ZEE, z, "minecraft:water")
                continue
            groeve = (x, z) in C.groeve
            lo = max(ZEE - 2, min(h, buur[x, z] if buur[x, z] >= 0 else ZEE - 1) - 1)
            for y in range(lo, h + 1):
                steil = h - max(buur[x, z], ZEE) >= 3
                if groeve:
                    if y == h:
                        r = rng.random()
                        b = "minecraft:gravel" if r < 0.3 else "minecraft:cobblestone" if r < 0.45 else \
                            "minecraft:coarse_dirt" if r < 0.6 else "guhs:knuffelsteen"
                    else:
                        b = GROEVE_STEEN[(y // 2 + (x + z) // 7) % len(GROEVE_STEEN)]
                elif y == h:
                    b = "minecraft:sand" if (h <= ZEE + 2 and I.kust[x, z] < 6) else GRAS
                elif y >= h - (0 if steil else 2) and not (steil and y < h):
                    b = AARDE
                elif y <= ZEE + 1:
                    b = "minecraft:mossy_cobblestone" if (x + z) % 3 else "minecraft:gravel"
                else:
                    k = (y + int(2 * math.sin(x / 7.0) + 2 * math.cos(z / 9.0))) // 3
                    b = lagen[k % len(lagen)]
                    if y >= h - 6 and rng.random() < 0.18:
                        b = "minecraft:moss_block"
                S.set(x, y, z, b)


def groeve_ertsen(C):
    """Ore veins on the exposed quarry walls (real blocks), each with a small amethyst bud of glitter beside it,
    an amethyst nook, a mine cart and an ore chest."""
    S, I = C.S, C.I
    dirs = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}
    ertsen = 0
    wanden = []
    for (x, z) in C.groeve:
        h = I.H[x, z]
        for f, (dx, dz) in dirs.items():
            nx, nz = x + dx, z + dz
            nh = I.H[nx, nz]
            for y in range(nh + 1, h + 1):
                wanden.append((x, y, z, f, nx, nz))
    # include the uncut hill rim facing into the pit
    for x in range(min(c[0] for c in C.groeve) - 2, max(c[0] for c in C.groeve) + 3):
        for z in range(min(c[1] for c in C.groeve) - 2, max(c[1] for c in C.groeve) + 3):
            if (x, z) in C.groeve or I.H[x, z] < 0:
                continue
            for f, (dx, dz) in dirs.items():
                if (x + dx, z + dz) in C.groeve:
                    for y in range(I.H[x + dx, z + dz] + 1, I.H[x, z]):
                        wanden.append((x, y, z, f, x + dx, z + dz))
    rng.shuffle(wanden)
    for (x, y, z, f, nx, nz) in wanden:
        if rng.random() < 0.16:
            ader = rng.choice(ERTSEN)
            S.set(x, y, z, ader)
            ertsen += 1
            if rng.random() < 0.55 and S.is_air(nx, y, nz):
                S.set(nx, y, nz, f"{GLITTER}[facing={f},waterlogged=false]")
            # a vein: sometimes a second block next to it
            if rng.random() < 0.4 and S.get(x, y - 1, z) not in ("minecraft:air",):
                S.set(x, y - 1, z, ader)
    # amethyst nook in the back wall + crystal glints on the pit floor
    gx, gz = C.s(*GROEVE)
    for (x, z) in sorted(C.groeve, key=lambda c: (c[0] - gx) + (c[1] - gz))[:6]:
        h = I.H[x, z]
        S.set(x, h, z, "minecraft:amethyst_block")
        S.set(x, h + 1, z, "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
    vloer = [c for c in C.groeve if I.H[c] == VLOER - 1]
    rng.shuffle(vloer)
    for (x, z) in vloer[:5]:
        S.set(x, VLOER, z, "guhs:guh_kristal_cluster[facing=up]")
    # props on the pit floor: mine cart, ore chest, pick-axe rack, lamp
    cx, cz = gx + 6, gz + 7
    y = VLOER - 1
    S.set(cx, y + 1, cz, "guhs:ertskarretje[facing=east]")
    S.set(cx + 1, y + 1, cz + 2, "guhs:ertsenkist[facing=south]")
    S.set(cx - 2, y + 1, cz + 3, "minecraft:barrel[facing=up,open=false]")
    S.set(cx + 3, y + 1, cz - 1, LAMP_O)
    S.set(cx + 3, y + 2, cz - 1, LAMP_B)
    C.npcs.append((cx + 2.5, y + 1, cz + 3.5, 315, "guh_npc_mijnwerkerguh"))
    C.labels["Kiezelgroeve"] = (gx, I.H[gx, gz] + 4, gz)
    return ertsen


# ------------------------------------------------------------------------------------------------- square + huts
def plein(C):
    S, I = C.S, C.I
    cx, cz = C.s(*PLEIN)
    y = VLOER
    I.claim(cx - PLEIN_R - 1, cz - PLEIN_R - 1, cx + PLEIN_R + 1, cz + PLEIN_R + 1, 0)
    for x in range(cx - PLEIN_R - 3, cx + PLEIN_R + 4):
        for z in range(cz - PLEIN_R - 3, cz + PLEIN_R + 4):
            d = math.hypot(x - cx, z - cz)
            if d <= 3.6:
                S.set(x, y, z, HART)
            elif d <= 4.6:
                S.set(x, y, z, "guhs:gladde_knuffelsteen")
            elif d <= PLEIN_R + 0.5 or (d <= PLEIN_R + 2.5 and rng.random() < 0.55):
                S.set(x, y, z, KLINKER if (d <= 8.5 or rng.random() < 0.5) else PAD)
            for k in (1, 2, 3):
                if d <= PLEIN_R + 0.5:
                    S.set(x, y + k, z, "minecraft:air")
    # centre: a little pink campfire ring, Meester Knutsel beside it at his work table
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            if (x, z) != (cx, cz):
                S.set(x, y + 1, z, slab("guhs:gladde_knuffelsteen_plaat"))
    S.set(cx, y + 1, cz, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
    # four lamp posts on the hearts ring, garlands (vlaggetjes) high across the square
    for (dx, dz) in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        S.set(cx + dx, y + 1, cz + dz, LAMP_O)
        S.set(cx + dx, y + 2, cz + dz, LAMP_B)
    # benches on the ring, facing in
    for (dx, dz, f) in ((0, -7, "south"), (-7, 0, "east"), (0, 7, "north")):
        for k in (-1, 0, 1):
            bx, bz = (cx + dx + k, cz + dz) if dz else (cx + dx, cz + dz + k)
            S.set(bx, y + 1, bz, f"guhs:guh_bankje_roze[facing={f}]")
    # Meester Knutsel: east side of the fire (the side you arrive from), with a work table + quest board
    kx, kz = cx + 3, cz - 1
    S.set(kx + 1, y + 1, kz - 1, "minecraft:crafting_table")
    S.set(kx + 1, y + 1, kz, "guhs:guh_tafel[facing=west]")
    S.set(kx + 2, y + 1, kz - 2, "guhs:infobord[facing=east]")
    S.set(kx + 1, y + 1, kz + 1, "minecraft:barrel[facing=up,open=false]")
    C.npcs.append((kx + 0.5, y + 1, kz + 0.5, 90, "guh_npc_knuffelmakerguh"))
    C.knutsel = (kx, y + 1, kz)
    C.labels["Meester Knutsel"] = (kx + 0.5, y + 3.2, kz + 0.5)
    C.zone["Plein"] = (cx - PLEIN_R, cz - PLEIN_R, cx + PLEIN_R, cz + PLEIN_R)
    # bunting across the square between the lamp posts (north-south and east-west)
    for i in range(-3, 4):
        S.set(cx + i, y + 5, cz - 4, "guhs:vlaggetjes[axis=x]")
        S.set(cx + i, y + 5, cz + 4, "guhs:vlaggetjes[axis=x]")


def hut(C, fx, fz, facing, dak, muur="guhs:pleister_roze", W=4, D=8, schoorsteen=False):
    """Cosy workshop hut: timber corners, plaster walls, heart windows, a gable roof whose gable faces the square.
    (fx, fz) = scene front centre; facing = the door side. Returns the Rot (local lz < 0 = the work yard in front)."""
    S, I = C.S, C.I
    y0 = VLOER
    T = Rot(S, fx, y0, fz, facing)
    trap = f"guhs:{dak}_trap"
    for lz in range(-1, D + 1):
        for lx in range(-W - 1, W + 2):
            for k in range(1, 12):
                T.set(lx, k, lz, "minecraft:air")
    for lz in range(D):
        for lx in range(-W, W + 1):
            T.set(lx, 0, lz, "minecraft:spruce_planks")
            for k in (-1, -2, -3):
                T.set(lx, k, lz, "guhs:roze_knuffelsteen")
            rand = abs(lx) == W or lz in (0, D - 1)
            for k in range(1, 5):
                if not rand:
                    continue
                hoek = abs(lx) == W and lz in (0, D - 1)
                b = "minecraft:stripped_cherry_log[axis=y]" if hoek else ("minecraft:cherry_planks" if k == 4 else muur)
                if not hoek and k in (2, 3) and ((abs(lx) == W and lz in (2, 3, D - 4, D - 3)) or
                                                 (lz == 0 and abs(lx) == 2) or (lz == D - 1 and abs(lx) == 1)):
                    b = "guhs:glasinlood_hartje"
                T.set(lx, k, lz, b)
            if 0 < lz < D - 1 and abs(lx) < W:
                T.set(lx, 1, lz, "minecraft:pink_carpet" if lx == 0 else "minecraft:air")
    T.set(0, 1, 0, "minecraft:cherry_door[facing=front,half=lower,hinge=left,open=false,powered=false]")
    T.set(0, 2, 0, "minecraft:cherry_door[facing=front,half=upper,hinge=left,open=false,powered=false]")
    # gable roof (ridge front-to-back), 1 overhang all round
    for k in range(W + 1):
        for lz in range(-1, D + 1):
            for side, f in ((1, "left"), (-1, "right")):
                T.set(side * (W + 1 - k), 4 + k, lz, stairs(trap, f), roof=True)
    for lz in range(-1, D + 1):
        T.set(0, 5 + W, lz, f"guhs:{dak}", roof=True)
    for lz in (0, D - 1):
        for k in range(1, W):
            for lx in range(-(W - k), W - k + 1):
                b = "guhs:glasinlood_hartje" if (lz == 0 and lx == 0 and k == 2) else muur
                T.set(lx, 4 + k, lz, b, roof=True)
    # flower strip along the side walls
    for lz in range(1, D - 1):
        for side in (-1, 1):
            if rng.random() < 0.7:
                T.set(side * (W + 1), 1, lz, rng.choice(["minecraft:pink_tulip", "minecraft:allium", "guhs:roze_guhbloem",
                                                         "minecraft:oxeye_daisy", "guhs:guhoortjes"]))
    if schoorsteen:
        for k in range(1, 11):
            T.set(W - 1, k, D - 2, "minecraft:bricks")
        T.set(W - 1, 11, D - 2, "guhs:guh_schoorsteen[rookt=true]")
    # work yard in front: path stones
    for lz in range(-4, 0):
        for lx in range(-W, W + 1):
            T.set(lx, 0, lz, KLINKER if rng.random() < 0.6 else PAD)
    # lantern posts at the front corners
    for lx in (-W - 1, W + 1):
        T.set(lx, 1, -1, LAMP_O)
        T.set(lx, 2, -1, LAMP_B)
    C.laatste_hut = (T.w(0, 1, -2), T.w(0, 1, 0), T.w(0, 1, D // 2))    # 1.10.4: POIs (yard stand spot, door, inside)
    cs = [T.w(lx, 0, lz) for lx in (-W - 1, W + 1) for lz in (-4, D)]
    I.claim(min(c[0] for c in cs), min(c[2] for c in cs), max(c[0] for c in cs), max(c[2] for c in cs), 1)
    return T


def npc(C, T, lx, lz, soort, yaw_rel=0):
    x, y, z = T.w(lx, 1, lz)
    base = {"south": 180, "north": 0, "east": 90, "west": 270}[K.NAMES[T.f]]
    C.npcs.append((x + 0.5, y, z + 0.5, (base + yaw_rel) % 360, soort))


def smederij(C):
    x, z = C.s(*SMEDERIJ[:2])
    T = hut(C, x, z, SMEDERIJ[2], "dakpan_kersrood", muur="guhs:pastelbaksteen_roze", schoorsteen=True)
    T.set(-3, 1, -2, "minecraft:anvil[facing=right]")
    T.set(-2, 1, -3, "guhs:vadsaambeeld[facing=front]")
    T.set(3, 1, -1, "minecraft:blast_furnace[facing=front,lit=true]")
    T.set(3, 2, -1, "minecraft:bricks")
    T.set(2, 1, -1, "minecraft:smithing_table")
    T.set(2, 1, -3, "minecraft:grindstone[face=floor,facing=right]")
    T.set(-1, 1, -1, "minecraft:water_cauldron[level=3]")
    T.set(-3, 1, -1, "guhs:infobord[facing=front]")
    npc(C, T, 1, -3, "guh_npc_smidguh", 20)
    C.labels["Smederij"] = T.w(0, 11, 3)
    C.hutten["Smederij"] = C.laatste_hut


def keuken(C):
    x, z = C.s(*KEUKEN[:2])
    T = hut(C, x, z, KEUKEN[2], "dakpan_perzik", schoorsteen=True)
    T.set(-3, 1, -1, "minecraft:smoker[facing=front,lit=true]")
    T.set(-2, 1, -1, "minecraft:water_cauldron[level=3]")
    T.set(2, 1, -1, "minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=true,has_bottle_2=false]")
    T.set(3, 1, -1, "minecraft:barrel[facing=up,open=false]")
    T.set(3, 2, -1, "minecraft:potted_red_tulip")
    T.set(-3, 1, -3, "guhs:infobord[facing=front]")
    # herb garden: two planters with berries and herbs
    for lx in (-3, -2, 2, 3):
        T.set(lx, 0, -4, "minecraft:farmland[moisture=7]")
        T.set(lx, 1, -4, rng.choice(["guhs:bestaand_mikakruid", "minecraft:sweet_berry_bush[age=3]",
                                     "minecraft:fern", "guhs:knabbelbessenstruik[age=3]"]))
    npc(C, T, 0, -2, "guh_npc_mika_kok", -15)
    C.labels["Keuken"] = T.w(0, 11, 3)
    C.hutten["Keuken"] = C.laatste_hut


def viswinkel(C):
    x, z = C.s(*VISWINKEL[:2])
    T = hut(C, x, z, VISWINKEL[2], "dakpan_hemelsblauw", muur="guhs:pleister_roze")
    T.set(-3, 1, -1, "minecraft:barrel[facing=up,open=false]")
    T.set(-3, 2, -1, "minecraft:barrel[facing=north,open=true]")
    T.set(-2, 1, -1, "minecraft:barrel[facing=up,open=false]")
    T.set(2, 1, -2, "guhs:kermiskraam_vissen[facing=front]")
    # drying rack with nets
    for lx in (2, 4):
        for k in (1, 2):
            T.set(lx, k, -4, "minecraft:spruce_fence")
    T.set(3, 2, -4, "minecraft:spruce_fence")
    T.set(3, 1, -4, "minecraft:cobweb")
    T.set(-3, 1, -3, "guhs:infobord[facing=front]")
    T.set(-1, 1, -3, "minecraft:dried_kelp_block")
    npc(C, T, 0, -2, "guh_npc_botenhuisje_visserguh", 10)
    C.labels["Viswinkel"] = T.w(0, 11, 3)
    C.hutten["Viswinkel"] = C.laatste_hut


def verzamelhuisje(C):
    x, z = C.s(*VERZAMEL[:2])
    T = hut(C, x, z, VERZAMEL[2], "dakpan_mint")
    T.set(-3, 1, -1, "guhs:ertsenkist[facing=front]")
    T.set(-3, 1, -2, "minecraft:barrel[facing=up,open=false]")
    T.set(-3, 2, -2, "minecraft:potted_allium")
    for lx in (2, 3):                       # log pile
        for k in (1, 2):
            T.set(lx, k, -1, "minecraft:oak_log[axis=x]" if lx == 2 else "guhs:guhbloesem_log[axis=x]")
    T.set(2, 3, -1, "minecraft:birch_log[axis=x]")
    T.set(3, 1, -3, "minecraft:hay_block[axis=y]")
    T.set(2, 1, -3, "minecraft:composter[level=7]")
    T.set(-1, 1, -3, "guhs:campingmarkt_weegschaal[facing=front,stand=midden]")
    T.set(-2, 1, -4, "guhs:infobord[facing=front]")
    npc(C, T, 1, -3, "guh_npc_houthakkerguh", -20)
    C.labels["Verzamelhuisje"] = T.w(0, 11, 3)
    C.hutten["Verzamelhuisje"] = C.laatste_hut


# ------------------------------------------------------------------------------------------------- gathering zones
def bosje(C):
    S, I = C.S, C.I
    bx, bz = C.s(*BOSJE)
    soorten = [KP.BOOM_ROZE, KP.BOOM_ROZE, KP.BOOM_BLOEI, KP.BOOM_GROEN]
    gezet, stronken = 0, 0
    for _ in range(400):
        a, rr = rng.uniform(0, 2 * math.pi), 16 * math.sqrt(rng.random())
        x, z = int(bx + rr * math.cos(a)), int(bz + rr * math.sin(a) * 0.85)
        if I.H[x, z] < VLOER - 3 or I.kust[x, z] < 8 or I.bezet[x, z]:
            continue
        h = I.h(x, z)
        if stronken < 5 and rng.random() < 0.3:
            S.set(x, h + 1, z, "guhs:campingmarkt_hakblok[stam=true]")
            S.set(x + 1, h + 1, z, f"{GLITTER}[facing=up,waterlogged=false]")
            S.set(x - 1, h + 1, z + 1, "minecraft:oak_log[axis=x]")
            I.claim(x - 1, z - 1, x + 1, z + 1, 0)
            stronken += 1
            continue
        if gezet >= 16:
            continue
        KP.boom(S, x, h, z, rng.choice(soorten), h=rng.randint(4, 6), r=rng.uniform(2.4, 3.3))
        S.set(x + 1, h + 1, z, f"{GLITTER}[facing=up,waterlogged=false]") if rng.random() < 0.35 else None
        I.claim(x - 2, z - 2, x + 2, z + 2, 0)
        gezet += 1
    # a chopping spot with an axe-guh
    h = I.h(bx, bz + 14)
    C.npcs.append((bx + 0.5, h + 1, bz + 14.5, 200, "guh_npc_timmerguh"))
    for (x, z) in ((bx - 13, bz + 2), (bx + 12, bz + 5)):
        if I.H[x, z] >= 0:
            S.set(x, I.h(x, z) + 1, z, "minecraft:firefly_bush")
    C.labels["Knuffelbosje"] = (bx, I.h(bx, bz) + 11, bz)
    C.zone["Knuffelbosje"] = (bx - 16, bz - 14, bx + 16, bz + 14)


def bloemenwei(C):
    S, I = C.S, C.I
    wx, wz = C.s(*WEI)
    R = 13
    hoog = [("minecraft:peony", 1), ("minecraft:rose_bush", 1), ("minecraft:lilac", 1)]
    laag = ["minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy", "guhs:roze_guhbloem",
            "minecraft:cornflower", "guhs:guhoortjes", "minecraft:white_tulip", "minecraft:azure_bluet",
            "minecraft:dandelion", "guhs:knabbelkoorbloem"]
    for x in range(wx - R - 1, wx + R + 2):
        for z in range(wz - R - 1, wz + R + 2):
            d = math.hypot(x - wx, z - wz) + 1.5 * math.sin(x * 0.5 + z * 0.3)
            if d > R or I.H[x, z] < 0 or I.bezet[x, z]:
                continue
            h = I.h(x, z)
            if S.get(x, h, z) != GRAS:
                continue
            r = rng.random()
            if r < 0.07:
                S.set(x, h + 1, z, rng.choice(["guhs:knabbelbessenstruik[age=3]", "minecraft:sweet_berry_bush[age=3]"]))
                if rng.random() < 0.4:
                    S.set(x, h + 2, z, f"{GLITTER}[facing=up,waterlogged=false]") if False else None
            elif r < 0.13:
                S.set(x, h + 1, z, rng.choice(["minecraft:flowering_azalea", "minecraft:flowering_azalea",
                                               "minecraft:azalea"]))
            elif r < 0.22:
                b = rng.choice(hoog)[0]
                S.set(x, h + 1, z, b + "[half=lower]")
                S.set(x, h + 2, z, b + "[half=upper]")
            elif r < 0.9:
                S.set(x, h + 1, z, rng.choice(laag))
    # picking baskets + a meadow guh
    h = I.h(wx + 9, wz - 9)
    S.set(wx + 9, h + 1, wz - 9, "minecraft:barrel[facing=up,open=false]")
    S.set(wx + 10, h + 1, wz - 9, "minecraft:composter[level=7]")
    C.npcs.append((wx + 8.5, h + 1, wz - 7.5, 220, "guh_npc_boerinneguh"))
    I.claim(wx - R, wz - R, wx + R, wz + R, 0)
    C.labels["Bloemenwei"] = (wx, I.h(wx, wz) + 4, wz)
    C.zone["Bloemenwei"] = (wx - R, wz - R, wx + R, wz + R)


def bijenhofje(C):
    S, I = C.S, C.I
    bx, bz = C.s(*BIJEN)
    y = VLOER
    for x in range(bx - 7, bx + 8):
        for z in range(bz - 5, bz + 6):
            rand = x in (bx - 7, bx + 7) or z in (bz - 5, bz + 5)
            if rand and not (z == bz - 5 and abs(x - bx) <= 1):
                S.set(x, y + 1, z, HEK)
            elif not rand:
                S.set(x, y + 1, z, rng.choice(["minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy",
                                                 "minecraft:cornflower", "minecraft:short_grass", "guhs:roze_guhbloem",
                                                 "minecraft:air"]))
    for (x, z) in ((bx - 7, bz - 5), (bx + 7, bz - 5)):
        S.set(x, y + 1, z, "minecraft:stripped_cherry_log[axis=y]")
        S.set(x, y + 2, z, LAMP_O)
        S.set(x, y + 3, z, LAMP_B)
    S.set(bx - 1, y + 1, bz - 5, "minecraft:cherry_fence_gate[facing=south,in_wall=false,open=true]")
    for (dx, dz, f) in ((-4, -1, "south"), (0, -1, "south"), (4, -1, "south"), (-4, 3, "north"), (0, 3, "north"),
                        (4, 3, "north")):
        S.set(bx + dx, y + 1, bz + dz, "minecraft:stripped_spruce_log[axis=y]")
        S.set(bx + dx, y + 2, bz + dz, f"minecraft:beehive[facing={f},honey_level=5]")
        S.set(bx + dx + 1, y + 1, bz + dz, f"{GLITTER}[facing=up,waterlogged=false]")
    S.set(bx + 7, y + 1, bz + 1, "minecraft:bee_nest[facing=west,honey_level=5]")
    S.set(bx - 6, y + 1, bz + 4, "minecraft:honey_block")
    S.set(bx - 5, y + 1, bz + 4, "minecraft:honeycomb_block")
    C.npcs.append((bx + 2.5, y + 1, bz + 1.5, 180, "guh_npc_verzorgerguh"))
    I.claim(bx - 8, bz - 6, bx + 8, bz + 6, 0)
    C.labels["Bijenhofje"] = (bx, y + 4, bz)
    C.zone["Bijenhofje"] = (bx - 7, bz - 5, bx + 7, bz + 5)


def visvijver(C):
    S, I = C.S, C.I
    vx, vz = C.s(*VIJVER)
    rx, rz = VIJVER_R
    y = VLOER
    for x in range(vx - rx - 2, vx + rx + 3):
        for z in range(vz - rz - 2, vz + rz + 3):
            e = ((x - vx) / rx) ** 2 + ((z - vz) / rz) ** 2 + 0.08 * math.sin(x * 0.7 + z * 0.4)
            if e <= 1:
                diep = 1 if e > 0.55 else 3
                for k in range(diep):
                    S.set(x, y - k, z, "minecraft:water")
                S.set(x, y - diep, z, "minecraft:clay" if diep > 1 else "minecraft:sand")
                for k in (1, 2):
                    S.set(x, y + k, z, "minecraft:air")
                if rng.random() < 0.07:
                    S.set(x, y + 1, z, "minecraft:lily_pad")
                elif rng.random() < 0.03:
                    S.set(x, y + 1, z, "guhs:guh_waterlelie")
            elif e <= 1.4:
                if z > vz and rng.random() < 0.35:
                    S.set(x, y + 1, z, "guhs:bloesemmeertje_riet[half=lower]")
                    S.set(x, y + 2, z, "guhs:bloesemmeertje_riet[half=upper]")
                elif rng.random() < 0.35:
                    S.set(x, y, z, "minecraft:sand")
    # little jetty from the north shore
    for z in range(vz - rz - 1, vz - 1):
        for x in (vx - 4, vx - 3):
            S.set(x, y + 1 if z < vz - rz + 1 else y, z, "minecraft:spruce_planks" if z < vz - rz + 1 else
                  slab("minecraft:spruce_slab", "top"))
            S.set(x, y + 1, z, "minecraft:air") if z >= vz - rz + 1 else None
    S.set(vx - 5, y + 1, vz - 2, "minecraft:spruce_fence")
    S.set(vx - 5, y + 2, vz - 2, "minecraft:lantern[hanging=false,waterlogged=false]")
    S.set(vx - 3, y + 1, vz - rz - 2, "minecraft:barrel[facing=up,open=false]")
    C.npcs.append((vx - 3.5, y + 0.5, vz - 2.5, 180, "guh_npc_veerguh"))
    I.claim(vx - rx - 2, vz - rz - 2, vx + rx + 2, vz + rz + 2, 0)
    C.labels["Visvijver"] = (vx, y + 3, vz)
    C.zone["Visvijver"] = (vx - rx, vz - rz, vx + rx, vz + rz)


def vissteiger(C):
    """Fishing jetty into the sea on the west (beach) side."""
    S, I = C.S, C.I
    _, z0 = C.s(0, STEIGER_Z)
    land = [x for x in range(I.X) if I.H[x, z0] >= 0]
    kx = min(land)
    # first column (from the west) whose ground is above the beach
    strand = next(x for x in range(kx, kx + 40) if I.H[x, z0] >= ZEE + 3)
    y = ZEE + 2
    x_eind = kx - 16
    for x in range(x_eind, strand + 1):
        for z in range(z0 - 1, z0 + 2):
            if I.H[x, z] < y:
                S.set(x, y, z, "minecraft:spruce_planks" if x % 4 else "minecraft:stripped_spruce_log[axis=z]")
                for k in (1, 2, 3):
                    S.set(x, y + k, z, "minecraft:air")
        if x <= kx and (x - x_eind) % 5 == 0:
            for z in (z0 - 2, z0 + 2):
                for yy in range(ZEE - 1, y + 1):
                    S.set(x, yy, z, "minecraft:spruce_log[axis=y]")
                S.set(x, y + 1, z, "minecraft:lantern[hanging=false,waterlogged=false]" if (x - x_eind) % 10 == 0
                      else "minecraft:spruce_fence")
    for z in range(z0 - 3, z0 + 4):                     # head of the jetty
        for x in range(x_eind - 3, x_eind + 1):
            S.set(x, y, z, "minecraft:spruce_planks")
    for z in range(z0 - 3, z0 + 4):
        S.set(x_eind - 4, y + 1, z, HEK)
    S.set(x_eind - 2, y + 1, z0 - 3, "minecraft:barrel[facing=up,open=false]")
    S.set(x_eind - 1, y + 1, z0 - 3, "minecraft:barrel[facing=up,open=false]")
    KP.vlag(S, x_eind - 3, y + 1, z0 + 3, (1, 0), h=3, w=3, paal_h=6)
    C.npcs.append((x_eind - 2.5, y + 1, z0 + 0.5, 270, "guh_npc_kapiteinguh"))
    C.steiger = (strand, z0, x_eind)
    C.labels["Vissteiger"] = (x_eind - 2, y + 4, z0)
    return strand, z0, x_eind


def brug(C):
    """Cosy wooden bridge to the Knuffelkamp: plank deck with a gentle arch, cherry railings, lantern arches
    with pink lampions every 8 blocks, spruce piles down into the sea."""
    S, I = C.S, C.I
    _, z0 = C.s(0, BRUG_Z)
    # islet side: east-most column that is well inland of the cliff
    xi = max(x for x in range(I.X) if I.H[x, z0] >= 0 and I.kust[x, z0] >= 7)
    # camp side
    if C.kamp_H is not None:
        kamp = [x for x in range(xi + 1, I.X) if C.kamp_H[x, z0] >= VLOER - 6]
        xk = kamp[0] + 3 if kamp else xi + 45
        yk = int(C.kamp_H[xk, z0])
    else:
        xk, yk = xi + 45, VLOER
    yd = yk                                  # deck top level (camp ground level)
    I.vlak(xi - 6, z0 - 4, xi, z0 + 4, yd, 6)
    C.brug_eind = (xi, xk, yd)
    return xi, xk, yd


def brug_bouw(C):
    S, I = C.S, C.I
    xi, xk, yd = C.brug_eind
    _, z0 = C.s(0, BRUG_Z)
    L = xk - xi
    for i, x in enumerate(range(xi - 2, xk + 1)):
        t = min(1.0, max(0.0, (x - xi) / max(1, L)))
        hh = yd + 1.6 * math.sin(math.pi * t)
        base = int(math.floor(hh))
        half = hh - base >= 0.5
        for z in range(z0 - 2, z0 + 3):
            S.set(x, base, z, "minecraft:spruce_planks" if z in (z0 - 2, z0 + 2) else
                  ("guhs:guhbloesem_planks" if (x // 2) % 2 else "minecraft:cherry_planks"))
            if half:
                S.set(x, base + 1, z, slab("minecraft:cherry_slab"))
            for k in range(2 if half else 1, 6):
                S.set(x, base + k, z, "minecraft:air")
            for k in range(1, 3):
                if S.get(x, base - k, z).startswith(("minecraft:grass", "minecraft:dirt")):
                    pass
        top = base + (1 if half else 0)
        for z in (z0 - 3, z0 + 3):
            S.set(x, base, z, "minecraft:stripped_cherry_log[axis=x]")
            S.set(x, base + 1, z, HEK)
        # piles + lantern arches every 8 blocks (over the sea only)
        over_zee = I.H[x, z0] < 0 and (C.kamp_H is None or C.kamp_H[x, z0] < 0)
        if (x - xi) % 8 == 4 and over_zee:
            for z in (z0 - 3, z0 + 3):
                for yy in range(ZEE - 1, base):
                    S.set(x, yy, z, "minecraft:spruce_log[axis=y]")
        if (x - xi) % 8 == 0 and 0 < x - xi < L:
            for z in (z0 - 3, z0 + 3):
                for k in range(1, 5):
                    S.set(x, top + k, z, "minecraft:stripped_cherry_log[axis=y]")
            for z in range(z0 - 3, z0 + 4):
                S.set(x, top + 5, z, "minecraft:stripped_cherry_log[axis=z]")
            S.set(x, top + 6, z0, "guhs:guh_dakpan_roze_plaat[type=bottom,waterlogged=false]")
            S.set(x, top + 4, z0, "guhs:lampion_roze[hanging=true]")
            S.set(x, top + 4, z0 - 2, "guhs:lampion_geel[hanging=true]")
            S.set(x, top + 4, z0 + 2, "guhs:lampion_geel[hanging=true]")
    # welcome arch on the islet end, with a sign
    x = xi - 3
    for z in (z0 - 3, z0 + 3):
        for k in range(1, 7):
            S.set(x, yd + k, z, "guhs:roze_knuffelsteen" if k % 3 else "guhs:gladde_knuffelsteen")
    for z in range(z0 - 4, z0 + 5):
        S.set(x, yd + 7, z, "guhs:dakpan_kersrood_trap[facing=east,half=bottom,shape=straight,waterlogged=false]"
              if False else "guhs:gladde_knuffelsteen")
        S.set(x, yd + 8, z, slab("guhs:guh_dakpan_roze_plaat"))
    K.oortjes(S, x, yd + 9, z0, (0, 1), gap=1, h=3, w=2)
    S.set(x - 1, yd + 1, z0 - 4, "guhs:infobord[facing=east]")
    C.labels["Brug naar het Knuffelkamp"] = ((xi + xk) / 2, yd + 8, z0)


# ------------------------------------------------------------------------------------------------- paths + greenery
def paden(C):
    I = C.I
    px, pz = C.s(*PLEIN)
    xi, xk, yd = C.brug_eind
    _, bz = C.s(0, BRUG_Z)
    gx, gz = C.s(*GROEVE)
    bsx, bsz = C.s(*BOSJE)
    wx, wz = C.s(*WEI)
    bix, biz = C.s(*BIJEN)
    vx, vz = C.s(*VIJVER)
    sx, sz, _ = C.steiger
    L = KP.lijn
    L(I, [(xi - 1, bz), (px + 24, bz + 1), (px + 10, pz - 2)], 1, PAD, KLINKER, 0.35)      # bridge -> square
    L(I, [(px - 9, pz - 4), (px - 22, pz - 16), (gx + 9, gz + 11)], 1, PAD, KLINKER, 0.15)    # -> quarry
    L(I, [(px + 7, pz - 7), (px + 14, pz - 20), (bsx - 2, bsz + 12)], 1, PAD, KLINKER, 0.15)  # -> wood
    L(I, [(px - 8, pz + 6), (px - 22, pz + 16), (wx + 10, wz - 6)], 1, PAD, KLINKER, 0.15)    # -> meadow
    L(I, [(px - 4, pz + 9), (px - 10, pz + 26), (bix + 1, biz - 7)], 1, PAD, KLINKER, 0.15)   # -> bees
    L(I, [(px + 7, pz + 7), (px + 14, pz + 24), (vx - 4, vz - VIJVER_R[1] - 3)], 1, PAD, KLINKER, 0.15)  # -> pond
    L(I, [(px - 10, pz + 1), (px - 30, pz + 2), (px - 50, pz + 3), (sx + 4, sz)], 1, PAD, KLINKER, 0.15)  # -> jetty
    # signposts at the forks (infobord)
    S = C.S
    for (x, z, f) in ((px + 13, pz - 4, "east"), (px - 13, pz - 3, "west"), (px - 3, pz + 13, "south"),
                      (px - 34, pz + 4, "west")):
        if I.H[x, z] >= 0:
            S.set(x, I.h(x, z) + 1, z, f"guhs:infobord[facing={f}]")
            I.claim(x, z, x, z, 1)


def groen(C, n_bomen=34):
    S, I = C.S, C.I
    px, pz = C.s(*PLEIN)
    gezet = 0
    for _ in range(4000):
        if gezet >= n_bomen:
            break
        x, z = rng.randrange(1, I.X - 1), rng.randrange(1, I.Z - 1)
        if I.H[x, z] < VLOER - 5 or I.kust[x, z] < 9 or I.bezet[x, z] or (x, z) in C.groeve:
            continue
        if math.hypot(x - px, z - pz) < 26:
            continue
        if any(S.get(x + dx, I.h(x + dx, z + dz), z + dz) in (PAD, KLINKER)
               for dx in range(-3, 4) for dz in range(-3, 4)):
            continue
        soort = rng.choice([KP.BOOM_ROZE, KP.BOOM_ROZE, KP.BOOM_BLOEI, KP.BOOM_GROEN])
        KP.boom(S, x, I.h(x, z), z, soort, h=rng.randint(4, 6), r=rng.uniform(2.4, 3.4))
        I.claim(x - 2, z - 2, x + 2, z + 2, 0)
        gezet += 1
    bloemen = ["minecraft:short_grass"] * 10 + ["minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy",
                                                 "guhs:guhoortjes", "guhs:roze_guhbloem", "minecraft:cornflower"] * 2
    for x in range(I.X):
        for z in range(I.Z):
            h = I.H[x, z]
            if h < 0 or I.kust[x, z] < 2 or (x, z) in C.groeve:
                continue
            if S.get(x, h, z) == GRAS and S.is_air(x, h + 1, z) and rng.random() < 0.13:
                S.set(x, h + 1, z, rng.choice(bloemen))


def bouw(S, ox, oz, kamp_H=None):
    """Builds the Ambachtseiland into S (scene column 0,0 = world ox,oz). Returns the Ctx (npcs, labels, zones)."""
    C = Ctx(S, ox, oz, kamp_H)
    rng.seed(1104)         # 1.10.4: a second build in the same process gives the same islet (the module rng starts at 1104)
    hoogtekaart(C)
    brug(C)
    groeve_vorm(C)
    zet_terrein(C)
    C.n_ertsen = groeve_ertsen(C)
    vissteiger(C)
    brug_bouw(C)
    plein(C)
    smederij(C)
    keuken(C)
    viswinkel(C)
    verzamelhuisje(C)
    paden(C)
    bijenhofje(C)
    visvijver(C)
    bloemenwei(C)
    bosje(C)
    KP.paden_verlichten(C.I)
    groen(C)
    return C
