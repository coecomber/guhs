"""
1.8.0 Guhdromenland, island "herinnering" (dromen-ontwerp/BOUWPLAN_180.md part E; design ontwerp-guhs180-dromen.md, render
dromen-ontwerp/_bron/r04_herinneringseiland.py). Java: feature/herinnering (HerinneringFeature, HerinneringBellen,
HerinneringScenes).

  build    the island template data/guhs/structure/dromen/eiland_herinnering.nbt (the r04 render scene, block for block:
           the Guh-familiedag with the long party table and roze guh koeken, the guh-quiz, guhtje prik, Oma-guh on her
           bench, the mini Guhssenburg, Rommelguh's corner full of glas_restje, the bushes where Snoes hid) with its people
           as guh_npc entities, five herinneringsbelletjes (hollow zeepbelglas balls, see BELLEN: the same numbers are in
           HerinneringBellen.java), its island json data/guhs/dromen/eiland/herinnering.json and the texts of the island
           and its five retro films (Dutch in nl_nl, English in tools/lang/en/c212_herinnering.json)

Standalone (from the project root): PYTHONPATH=tools python tools/features/herinnering.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, "tools")
import make_structures as ms  # noqa: E402

X, Y, Z = 100, 52, 100
C, T = 50, 28                      # the island's centre spot (template block) and the height of its top layer
VERSIE = 1
TAG = "guhs_herinnering"           # people of the memory: clicking them gives a memory line (HerinneringFeature)

# the five herinneringsbelletjes: id -> (centre x, y, z in the template, radius, the little "film still" inside)
# KEEP IN SYNC with HerinneringBellen.BELLEN (relative there: minus (C, T, C))
BELLEN = {
    "koeken": ((44, T + 4, 42), 2.4, "guhs:roze_guh_koek[facing=south,koeken=6]"),
    "quiz": ((75, T + 4, 53), 2.4, "guhs:infobord[facing=north]"),
    "prik": ((25, T + 4, 59), 2.4, "guhs:speelkleed"),
    "oma": ((62, T + 4, 33), 2.4, "guhs:theepotje[facing=south]"),
    "snoes": ((36, T + 4, 74), 2.4, "minecraft:azalea_leaves[persistent=true]"),
}


def _parse(s):
    """'guhs:x[a=b,c=d]' -> ('guhs:x', {'a': 'b', 'c': 'd'}); no namespace = guhs."""
    naam, props = s, {}
    if "[" in s:
        naam, rest = s.split("[", 1)
        for kv in rest.rstrip("]").split(","):
            if kv:
                k, v = kv.split("=")
                props[k] = v
    if ":" not in naam:
        naam = "guhs:" + naam
    return naam, props


class Eiland(ms.Structure):
    """The few Scene calls of the render (dromen-ontwerp vibekit/droomkit) on top of a structure."""

    def __init__(self):
        super().__init__((X, Y, Z))
        self.namen = set()

    def zet(self, x, y, z, s, keep=False):
        x, y, z = int(x), int(y), int(z)
        if not self.inside(x, y, z) or (keep and (x, y, z) in self.blocks):
            return
        naam, props = _parse(s)
        if naam == "minecraft:air":
            self.blocks.pop((x, y, z), None)
            return
        self.namen.add(naam)
        self.set(x, y, z, naam, props)

    def is_air(self, x, y, z):
        return (int(x), int(y), int(z)) not in self.blocks

    def vul(self, x1, y1, z1, x2, y2, z2, s, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.zet(x, y, z, s, keep)

    def wanden(self, x1, y1, z1, x2, y2, z2, s):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                if x in (x1, x2) or z in (z1, z2):
                    for y in range(min(y1, y2), max(y1, y2) + 1):
                        self.zet(x, y, z, s)

    def lucht(self, x1, y1, z1, x2, y2, z2):
        self.vul(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def bol(self, cx, cy, cz, rx, ry, rz, s, hollow=0.0, keep=False):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 > 1:
                        continue
                    if hollow and ((x - cx) / (rx - hollow)) ** 2 + ((y - cy) / (ry - hollow)) ** 2 + ((z - cz) / (rz - hollow)) ** 2 <= 1:
                        continue
                    self.zet(x, y, z, s, keep)

    def cilinder(self, cx, cz, r, y1, y2, s, hollow=0.0):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                d = math.hypot(x - cx, z - cz)
                if d <= r and not (hollow and d <= r - hollow):
                    for y in range(y1, y2 + 1):
                        self.zet(x, y, z, s)

    def boom(self, x, y, z, h=4, r=2):
        for yy in range(y, y + h):
            self.zet(x, yy, z, "guhs:pluizenboom_stam[axis=y]")
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                for dy in range(-1, 2):
                    if abs(dx) + abs(dz) + abs(dy) * 1.5 <= r + 0.6 and (dx, dz) != (0, 0):
                        self.zet(x + dx, y + h + dy - 1, z + dz, "guhs:pluizenboom_bladeren", keep=True)
        self.zet(x, y + h, z, "guhs:pluizenboom_bladeren")
        self.zet(x, y + h + 1, z, "guhs:pluizenboom_bladeren", keep=True)

    def npc(self, x, y, z, render_yaw, kind, herinnering=True):
        """A person of the memory (render yaw 0 = looking north, as in the renders; Minecraft yaw 0 = south)."""
        nbt = {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1),
               "Rotation": ms.floats(float((render_yaw + 180) % 360), 0.0)}
        if herinnering:
            nbt["Tags"] = ms.NbtList(8, [TAG])
        self.entity(x, y, z, nbt)


def bouw():
    S = Eiland()
    rng = random.Random(216)

    # ------------------------------------------------------------------------------------------- the floating island
    r, diepte, fase = 31, 20, [rng.random() * 2 * math.pi for _ in range(4)]

    def rand(a):
        return r * (1 + 0.18 * (0.6 * math.sin(3 * a + fase[0]) + 0.4 * math.sin(5 * a + fase[1]) + 0.25 * math.sin(8 * a + fase[2])))

    def binnen(x, z):
        return math.hypot(x - C, z - C) <= rand(math.atan2(z - C, x - C))

    for x in range(int(C - r * 1.5) - 1, int(C + r * 1.5) + 2):
        for z in range(int(C - r * 1.5) - 1, int(C + r * 1.5) + 2):
            R0 = rand(math.atan2(z - C, x - C))
            d = math.hypot(x - C, z - C) / R0
            if d > 1:
                continue
            h = int(diepte * (1 - d) ** 0.7 + rng.random() * 1.5) + 1
            for k in range(h + 1):
                b = "guhs:knuffelgras" if k == 0 else "guhs:pluiswolblok" if k <= 2 else \
                    "guhs:droomzandblok" if k >= h - 1 and d < 0.3 else "guhs:knuffelsteen"
                S.zet(x, T - k, z, b)
            if d > 0.82 and rng.random() < 0.12:
                for k in range(1, rng.randint(2, 4)):
                    S.zet(x, T - h - k, z, "guhs:wolkenweide_rank", keep=True)

    def belletje(naam):
        (x, y, z), rr, binnenin = BELLEN[naam]
        S.bol(x, y, z, rr, rr, rr, "guhs:zeepbelglas", hollow=1)
        S.zet(x, y, z, binnenin)

    # ------------------------------------------------------------------------- little paths of mokka plaster (sepia)
    def pad(x1, z1, x2, z2, w=1):
        n = int(max(abs(x2 - x1), abs(z2 - z1)) * 2) + 1
        for i in range(n + 1):
            t = i / n
            x, z = x1 + (x2 - x1) * t, z1 + (z2 - z1) * t
            for a in range(-w, w + 1):
                for b in range(-w, w + 1):
                    xx, zz = int(round(x)) + a, int(round(z)) + b
                    if binnen(xx, zz):
                        S.zet(xx, T, zz, "guhs:pleister_mokka" if (xx + zz) % 3 else "guhs:tegel_pootjes")

    for (px, pz) in ((72, 28), (30, 34), (68, 58), (33, 63), (68, 40), (50, 74)):
        pad(C, C, px, pz, 1 if (px, pz) == (72, 28) else 0)

    # ----------------------------------------------------------------------- 1. the long party table in the middle
    TX1, TX2, TZ = C - 9, C + 9, C
    for x in range(TX1 - 2, TX2 + 3):
        for z in range(TZ - 4, TZ + 5):
            S.zet(x, T, z, "guhs:pleister_boter" if (x + z) % 2 else "guhs:pleister_mokka")
    for x in range(TX1, TX2 + 1):
        S.zet(x, T + 1, TZ, "guhs:feestbuffettafel[facing=south,gedekt=true]")
        if x % 3 == 0:
            S.zet(x, T + 2, TZ, f"guhs:roze_guh_koek[facing=south,koeken={1 + x % 6}]")
        elif x % 3 == 1:
            S.zet(x, T + 2, TZ, "guhs:theepotje[facing=south]" if x % 2 else "guhs:elftocht_kopjes[facing=south,soort=chocovet]")
    S.zet(C, T + 2, TZ, "guhs:guh_taart[bites=1]")
    S.zet(TX2, T + 2, TZ, "guhs:roze_guh_koek[facing=south,koeken=1]")          # the one koek nobody knew who it was for
    gasten = ["guhkindje", "opa_guh", "kraamguh", "theeguh", "bakkerguh", "juf_knuffel", "guhkindje", "boerinneguh"]
    for i, x in enumerate(range(TX1, TX2 + 1, 2)):
        S.zet(x, T + 1, TZ - 1, "guhs:guh_stoel[facing=south]")
        S.zet(x, T + 1, TZ + 1, "guhs:guh_stoel[facing=north]")
        if i % 2 == 0:
            S.npc(x + .5, T + 1.5, TZ - 1 + .5, 180, gasten[i % len(gasten)])
        if i % 3 == 1:
            S.npc(x + .5, T + 1.5, TZ + 1 + .5, 0, gasten[(i + 3) % len(gasten)])
    for x in (TX1 - 2, TX2 + 2):
        for z in (TZ - 4, TZ + 4):
            S.vul(x, T + 1, z, x, T + 5, z, "guhs:vadshout_hek")
            S.zet(x, T + 6, z, "guhs:lampion_roze[hanging=false]")
    for x in range(TX1 - 1, TX2 + 2):
        S.zet(x, T + 5, TZ - 4, "guhs:feestslingers[axis=x]")
        S.zet(x, T + 5, TZ + 4, "guhs:vlaggetjes[axis=x]")
    belletje("koeken")

    # ---------------------------------------------------------------------- 2. the guh-quiz stall (south-east)
    QX, QZ = 68, 60
    for x in range(QX - 3, QX + 4):
        for z in range(QZ - 2, QZ + 3):
            S.zet(x, T, z, "guhs:pleister_mokka")
    for x in (QX - 3, QX + 3):
        for z in (QZ - 2, QZ + 2):
            S.vul(x, T + 1, z, x, T + 3, z, "guhs:guh_hekje_wit")
    for x in range(QX - 3, QX + 4):
        for z in range(QZ - 2, QZ + 3):
            S.zet(x, T + 4, z, "guhs:campingmarkt_tentdoek_rood" if x % 2 else "guhs:campingmarkt_tentdoek_creme")
    for x in range(QX - 2, QX + 3):
        S.zet(x, T + 1, QZ - 2, "guhs:guh_tafel[facing=north]")
    S.zet(QX, T + 2, QZ - 2, "guhs:infobord[facing=north]")
    S.zet(QX, T + 5, QZ, "guhs:knuffelsteen_gezicht[facing=north,stemming=1]")
    S.npc(QX + .5, T + 1, QZ + .5, 0, "spelleiderguh")                       # the quizmaster
    for i, x in enumerate(range(QX - 3, QX + 4, 3)):
        S.npc(x + .5, T + 1, QZ - 5 + .5, 180, ["guhkindje", "opa_guh", "guhkindje"][i])
    belletje("quiz")

    # ---------------------------------------------------------------------- 3. guhtje prik (south-west)
    PX, PZ = 33, 64
    for x in range(PX - 3, PX + 4):
        for z in range(PZ - 3, PZ + 4):
            S.zet(x, T + 1, z, "guhs:speelkleed")
    S.vul(PX, T + 1, PZ + 4, PX, T + 3, PZ + 4, "guhs:pastelbaksteen_mokka")
    S.zet(PX, T + 4, PZ + 4, "guhs:knuffelsteen_gezicht[facing=north,stemming=3]")
    S.zet(PX - 1, T + 3, PZ + 4, "guhs:pastelbaksteen_mokka")
    S.zet(PX + 1, T + 3, PZ + 4, "guhs:pastelbaksteen_mokka")
    S.npc(PX + .5, T + 1.07, PZ + 1.5, 20, "guhkindje")                          # blindfolded, just a little crooked
    for (x, z) in ((PX - 3, PZ - 2), (PX + 3, PZ - 2)):
        S.npc(x + .5, T + 1.07, z + .5, 0, "guhkindje")
    belletje("prik")

    # ---------------------------------------------------------------------- 4. Oma-guh on her bench with a picnic (east)
    OX, OZ = 69, 39
    for x in range(OX - 3, OX + 3):
        for z in range(OZ - 1, OZ + 3):
            S.zet(x, T + 1, z, "guhs:park_picknickkleed")
    S.zet(OX, T + 1, OZ - 2, "guhs:guh_bankje_hout[facing=south]")
    S.zet(OX + 1, T + 1, OZ - 2, "guhs:guh_bankje_hout[facing=south]")
    S.npc(OX + 1, T + 1.5, OZ - 2 + .5, 180, "droom_omaguh", herinnering=False)
    S.zet(OX - 2, T + 2, OZ + 1, "guhs:picknickeilandje_mand[facing=south,versierd=true]")
    S.zet(OX, T + 2, OZ + 1, "guhs:roze_guh_koek[facing=south,koeken=4]")
    S.zet(OX - 1, T + 2, OZ, "guhs:theepotje[facing=west]")
    S.npc(OX - 1 + .5, T + 1.07, OZ + 2.5, 0, "guhkindje")                      # grandchild
    S.boom(OX + 4, T + 1, OZ - 4, h=6, r=3)
    belletje("oma")

    # ---------------------------------------------------------------------- 5. the mini Guhssenburg (north-west)
    BX, BZ, BW = 30, 31, 6
    for x in range(BX - BW, BX + BW + 1):
        for z in range(BZ - BW, BZ + BW + 1):
            if binnen(x, z):
                S.zet(x, T, z, "guhs:pleister_mokka")
    S.wanden(BX - BW + 1, T + 1, BZ - BW + 1, BX + BW - 1, T + 5, BZ + BW - 1, "guhs:pastelbaksteen_mokka")
    for x in range(BX - BW + 1, BX + BW, 2):
        S.zet(x, T + 6, BZ - BW + 1, "guhs:pastelbaksteen_mokka")
        S.zet(x, T + 6, BZ + BW - 1, "guhs:pastelbaksteen_mokka")
    for z in range(BZ - BW + 1, BZ + BW, 2):
        S.zet(BX - BW + 1, T + 6, z, "guhs:pastelbaksteen_mokka")
        S.zet(BX + BW - 1, T + 6, z, "guhs:pastelbaksteen_mokka")
    for (x, z) in ((BX - BW + 1, BZ - BW + 1), (BX + BW - 1, BZ - BW + 1), (BX - BW + 1, BZ + BW - 1), (BX + BW - 1, BZ + BW - 1)):
        S.cilinder(x, z, 1.6, T + 1, T + 9, "guhs:pleister_mokka")
        S.cilinder(x, z, 1.6, T + 10, T + 10, "guhs:pastelbaksteen_mokka", hollow=1)
        S.zet(x, T + 11, z, "guhs:guhrio_vlag")
    S.lucht(BX, T + 1, BZ + BW - 1, BX + 1, T + 3, BZ + BW - 1)            # gate to the south
    S.zet(BX, T + 5, BZ + BW - 1, "guhs:knuffelsteen_gezicht[facing=south,stemming=2]")
    S.zet(BX + 1, T + 5, BZ + BW - 1, "guhs:knuffelsteen_gezicht[facing=south,stemming=0]")
    S.zet(BX, T + 5, BZ - BW + 1, "guhs:knuffelsteen_gezicht[facing=north,stemming=1]")
    S.zet(BX - BW + 1, T + 4, BZ, "guhs:knuffelsteen_gezicht[facing=west,stemming=3]")
    S.zet(BX + BW - 1, T + 4, BZ, "guhs:knuffelsteen_gezicht[facing=east,stemming=1]")
    S.cilinder(BX, BZ, 2.2, T + 1, T + 12, "guhs:pastelbaksteen_mokka")
    S.cilinder(BX, BZ, 2.4, T + 13, T + 13, "guhs:pleister_mokka", hollow=1)
    S.zet(BX, T + 14, BZ, "guhs:guhrio_vlag")
    S.npc(BX + .5, T + 1, BZ + BW + 1.5, 0, "opa_guh")                       # the family photo is taken here

    # ------------------------------------------------------------ 6. Rommelguh's corner: glasses with a little left, everywhere
    RX, RZ = 60, 72
    for x in range(RX - 4, RX + 4):
        for z in range(RZ - 3, RZ + 3):
            S.zet(x, T, z, "guhs:tegel_dambord")
    S.wanden(RX + 4, T + 1, RZ - 3, RX + 4, T + 4, RZ + 2, "guhs:pleister_mokka")
    S.zet(RX + 4, T + 3, RZ - 1, "guhs:knuffelsteen_gezicht[facing=west,stemming=3]")
    drankjes, kanten = ["sap", "cola", "limo", "melk", "thee"], ["north", "east", "south", "west"]

    def glas():
        return f"guhs:glas_restje[drankje={rng.choice(drankjes)},facing={rng.choice(kanten)},glazen={rng.choice((1, 1, 2, 3))}]"

    for x in range(RX - 3, RX + 3):
        S.zet(x, T + 1, RZ, "guhs:guh_tafel[facing=north]")
        S.zet(x, T + 2, RZ, glas())
    geplaatst = 0
    while geplaatst < 22:                                                  # ... and everywhere round it (and a few further)
        x, z = RX + rng.randint(-9, 9), RZ + rng.randint(-8, 6)
        if binnen(x, z) and S.is_air(x, T + 1, z) and not S.is_air(x, T, z):
            S.zet(x, T + 1, z, glas())
            geplaatst += 1
    for (x, z) in ((C + 3, C - 6), (C - 6, C + 6), (66, 44), (38, 56)):    # Rommelguh was everywhere at the party
        if S.is_air(x, T + 1, z):
            S.zet(x, T + 1, z, glas())
    S.npc(RX + .5, T + 1, RZ + 2 + .5, 160, "rommelguh", herinnering=False)

    # ---------------------------------------------------------------- 7. the bushes where Snoes hid (south)
    SX, SZ = 45, 73
    for (x, z, rr) in ((SX - 3, SZ, 2.4), (SX + 2, SZ + 1, 2.8), (SX - 1, SZ + 3, 2.2), (SX + 5, SZ - 1, 1.8)):
        S.bol(x, T + 2, z, rr, 1.9, rr, "minecraft:azalea_leaves[persistent=true]", keep=True)
    for (x, z) in ((SX - 5, SZ - 2), (SX + 7, SZ + 1), (SX - 4, SZ + 2), (SX + 1, SZ - 3)):
        S.zet(x, T + 1, z, "guhs:knabbelbessenstruik[age=3]", keep=True)
    for x in range(SX - 6, SX + 8):
        S.zet(x, T + 1, SZ + 5, "guhs:doolhofheg", keep=True)
        S.zet(x, T + 2, SZ + 5, "guhs:doolhofheg", keep=True)
    S.zet(SX + 4, T + 2, SZ + 5, "guhs:doolhofheg_gezicht[facing=north]")
    S.lucht(SX, T + 1, SZ - 2, SX, T + 2, SZ - 1)
    S.npc(SX + .5, T + 1, SZ - 1.2, 0, "snoes")                               # Snoes, peeking between the leaves at the table
    belletje("snoes")

    # ---------------------------------------------------------------- the steiger to the plein (36 degrees, north-east)
    dx, dz = math.sin(math.radians(36)), -math.cos(math.radians(36))
    px, pz = -dz, dx
    for k in [i / 2 for i in range(46, 82)]:
        for w in (-1, 0, 1):
            x, z = int(round(C + dx * k + px * w)), int(round(C + dz * k + pz * w))
            if 0 <= x < X and 0 <= z < Z and not binnen(x, z):
                S.zet(x, T, z, "guhs:wolkenblok_wit_plaat[type=top,waterlogged=false]" if w == 0
                      else "guhs:wolkenblok_roze_plaat[type=top,waterlogged=false]", keep=True)
    for k in (25, 33):                                                     # lanterns along the steiger
        x, z = int(round(C + dx * k + px * 2)), int(round(C + dz * k + pz * 2))
        S.zet(x, T + 1, z, "guhs:lampion_roze[hanging=false]" if k == 25 else "guhs:lampion_geel[hanging=false]")

    # ---------------------------------------------------------------- welcome sign, white fence, flowers, trees, families
    S.zet(58, T + 1, 40, "guhs:dierentuinbordje[facing=south]")
    for x in range(X):
        for z in range(Z):
            if binnen(x, z) and not all(binnen(x + a, z + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                naar_steiger = x > C and z < C and abs((x - C) * dz - (z - C) * dx) < 3
                if S.is_air(x, T + 1, z) and not S.is_air(x, T, z) and not naar_steiger:
                    S.zet(x, T + 1, z, "guhs:guh_hekje_wit")
    bloemen = ("guhs:roze_guhbloem", "guhs:pluisgras", "guhs:maanbloemetje", "guhs:pluisgras")
    for x in range(X):
        for z in range(Z):
            if binnen(x, z) and S.is_air(x, T + 1, z) and not S.is_air(x, T, z) and rng.random() < 0.06:
                S.zet(x, T + 1, z, rng.choice(bloemen))
    for (x, z) in ((42, 24), (58, 24), (78, 50), (22, 52), (40, 80), (64, 30), (24, 40)):
        if binnen(x, z):
            S.boom(x, T + 1, z, h=5, r=2)
    for (x, z, yaw) in ((40, 40, 120), (58, 64, 300), (40, 58, 60), (62, 48, 250)):
        for a in range(-1, 2):
            for b in range(-1, 2):
                if S.is_air(x + a, T + 1, z + b):
                    S.zet(x + a, T + 1, z + b, "guhs:park_picknickkleed")
        S.npc(x + .5, T + 1.1, z + .5, yaw, gasten[x % len(gasten)])
        S.zet(x - 1, T + 2, z + 1, f"guhs:roze_guh_koek[facing=south,koeken={1 + z % 5}]")
    for (x, z) in ((44, 44), (56, 44), (44, 56), (56, 56), (40, 36), (62, 36), (40, 64), (60, 64)):
        if S.is_air(x, T + 1, z):
            S.vul(x, T + 1, z, x, T + 3, z, "guhs:vadshout_hek")
            S.zet(x, T + 4, z, "guhs:lampion_roze[hanging=false]" if (x + z) % 2 else "guhs:lampion_geel[hanging=false]")
    return S


def _bestaat(naam):
    ns, pad = naam.split(":")
    if ns == "minecraft":
        return True
    return os.path.exists(os.path.join("src", "main", "resources", "assets", ns, "blockstates", pad + ".json"))


def _steiger(k):
    dx, dz = math.sin(math.radians(36)), -math.cos(math.radians(36))
    return round(dx * k, 2) + 0.5, round(dz * k, 2) + 0.5


def build():
    S = bouw()
    mist = sorted(n for n in S.namen if not _bestaat(n))
    if mist:
        raise SystemExit(f"herinnering: unknown blocks {mist}")
    S.save("dromen/eiland_herinnering")
    sx, sz = _steiger(36)
    ax, az = _steiger(24)
    yaw = math.degrees(math.atan2(ax, -az))       # look back to the middle of the island (Minecraft yaw: 0 = +z)
    opzet = {"versie": VERSIE, "midden": [C, T, C], "aankomst": {"plek": [ax, 1, az], "yaw": round(yaw, 1)},
             "steiger": [sx, 1, sz]}
    pad = os.path.join("src", "main", "resources", "data", "guhs", "dromen", "eiland", "herinnering.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(opzet, f, indent=2)
        f.write("\n")
    return len(S.blocks), len(S.entities)


# ---------------------------------------------------------------------------------------------------------- texts
SCENE = "scene.guhs.herinnering_"
TEKSTEN = {  # key -> (Dutch, English)
    "gui.guhs.herinnering.bel": ("Een herinneringsbelletje... er zit een filmpje in.", "A memory bubble... there's a little film inside."),
    "gui.guhs.herinnering.bel_gezien": ("Dit filmpje ken je al. Klik op het belletje om het nog eens te zien.",
                                        "You know this film already. Click the bubble to watch it again."),
    "gui.guhs.herinnering.bel_dicht": ("De bosjes ritselen... maar dit belletje gaat pas open als je de andere herinneringen hebt gezien.",
                                       "The bushes rustle... but this bubble only opens once you've seen the other memories."),
    "gui.guhs.herinnering.persoon": ("Een herinnering. Hij kijkt dwars door je heen en lacht naar iemand die er niet meer is.",
                                     "A memory. They look right through you and smile at someone who isn't there any more."),
    "gui.guhs.herinnering.persoon_snoes": ("Een kleine Snoes, tussen de blaadjes. Hij ziet jou niet. Hij kijkt naar de feesttafel.",
                                           "A little Snoes, between the leaves. He doesn't see you. He's looking at the party table."),
    "gui.guhs.herinnering.stukje": ("Stukje gevonden: de herinnering. Nu snap je waarom Snoes nooit durfde te slapen.",
                                    "Piece found: the memory. Now you understand why Snoes never dared to sleep."),
    "gui.guhs.herinnering.voortgang": ("Herinneringen gezien: %s van 5", "Memories seen: %s of 5"),
    # 1. the quiz
    SCENE + "quiz.titel": ("Herinnering: de guh-quiz", "Memory: the guh quiz"),
    SCENE + "quiz.naam.master": ("Quizguh", "Quiz Guh"),
    SCENE + "quiz.naam.kind": ("Guhkindje", "Guh kid"),
    SCENE + "quiz.q1": ("Het is de Guh-familiedag. Iedereen is er. Echt iedereen.", "It's the Guh Family Day. Everyone is there. Really, everyone."),
    SCENE + "quiz.q2": ("Vraag drie! Hoeveel knabbels zitten er in de pot?", "Question three! How many nibbles are in the jar?"),
    SCENE + "quiz.q3": ("ALLEMAAL! Want ik heb ze nog niet opgegeten! ...Nog niet. Njeg!",
                        "ALL OF THEM! Because I haven't eaten them yet! ...Yet. Njeg!"),
    SCENE + "quiz.q4": ("Iedereen lacht. Achter de bosjes lacht iemand heel zachtjes mee.",
                        "Everyone laughs. Behind the bushes, someone laughs along, very quietly."),
    # 2. guhtje prik
    SCENE + "prik.titel": ("Herinnering: guhtje prik", "Memory: pin the tail on the guh"),
    SCENE + "prik.naam.kind": ("Guhkindje", "Guh kid"),
    SCENE + "prik.p1": ("Guhtje prik! Met een blinddoek om en een staartje in je pootje.",
                        "Pin the tail on the guh! With a blindfold on and a little tail in your paw."),
    SCENE + "prik.p2": ("Is het hier? Hier? ...O nee, dit is oma's knie.", "Is it here? Here? ...Oh no, this is Grandma's knee."),
    SCENE + "prik.p3": ("Het staartje kwam op de snoet. Iedereen vond dat ook goed.", "The tail ended up on the snout. Everyone thought that was fine too."),
    SCENE + "prik.p4": ("In de bosjes hield een guhtje zijn adem in. Hij had het precies goed geraden. Niemand hoorde het.",
                        "In the bushes, a little guh held his breath. He had guessed it exactly right. Nobody heard."),
    # 3. the roze guh koeken
    SCENE + "koeken.titel": ("Herinnering: de roze guh-koeken", "Memory: the pink guh cakes"),
    SCENE + "koeken.naam.opa": ("Opa-guh", "Grandpa Guh"),
    SCENE + "koeken.k1": ("Aan de lange tafel: roze guh-koeken. Voor elke guh eentje, met glazuur en grote oogjes.",
                          "At the long table: pink guh cakes. One for every guh, with icing and big eyes."),
    SCENE + "koeken.k2": ("Wie heeft er nog geen koek? Pootje omhoog!", "Who hasn't got a cake yet? Paw up!"),
    SCENE + "koeken.k3": ("Iedereen stak een pootje op. Iedereen kreeg er een.", "Everyone put a paw up. Everyone got one."),
    SCENE + "koeken.k4": ("Aan het eind bleef er eentje over op het schoteltje. Niemand wist voor wie.",
                          "In the end, one was left on the little plate. Nobody knew who it was for."),
    # 4. Oma and the Guhssenburg
    SCENE + "oma.titel": ("Herinnering: oma en de familiefoto", "Memory: Grandma and the family photo"),
    SCENE + "oma.naam.oma": ("Oma-guh", "Grandma Guh"),
    SCENE + "oma.o1": ("Kom eens hier, vadsjes. Allemaal. Iedereen hoort erbij.", "Come here, little ones. All of you. Everyone belongs."),
    SCENE + "oma.o2": ("Oma telde alle kleinkinderen. Ze kwam er telkens eentje te weinig uit.",
                       "Grandma counted all her grandchildren. Every time, she came up one short."),
    SCENE + "oma.o3": ("Ach. Ik tel ook altijd verkeerd. Nog een koekje, vadsje?", "Oh well. I always count wrong. Another cake, little one?"),
    SCENE + "oma.o4": ("Bij de Guhssenburg werd de familiefoto gemaakt. Op de foto, heel klein, een blaadje dat beweegt.",
                       "The family photo was taken at the Guhssenburg. In the photo, very small, a leaf that moves."),
    # 5. Snoes himself
    SCENE + "snoes.titel": ("Herinnering: Snoes", "Memory: Snoes"),
    SCENE + "snoes.naam.snoes": ("Snoes", "Snoes"),
    SCENE + "snoes.s1": ("En dit... is van Snoes zelf.", "And this one... is Snoes's own."),
    SCENE + "snoes.s2": ("Hij zat hier, tussen de blaadjes. De hele dag. Hij keek naar de quiz, de koeken en de foto.",
                         "He sat here, between the leaves. All day. He watched the quiz, the cakes and the photo."),
    SCENE + "snoes.s3": ("Ik heb geen mens. Dus ik hoor er niet bij... toch?", "I don't have a person. So I don't belong... do I?"),
    SCENE + "snoes.s4": ("Toen het donker werd, ging iedereen naar huis. Iedereen had een plekje. Behalve Snoes.",
                         "When it got dark, everyone went home. Everyone had a place. Except Snoes."),
    SCENE + "snoes.s5": ("Misschien... volgend jaar. Njeg.", "Maybe... next year. Njeg."),
    SCENE + "snoes.s6": ("Daarom durft hij niet te slapen. Wie slaapt, heeft een plekje nodig.",
                         "That's why he doesn't dare to sleep. If you sleep, you need a place."),
}


def _direct():
    blokken, mensen = build()
    en_pad = os.path.join("tools", "lang", "en", "c212_herinnering.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    bad = [k for k, (nl, en) in TEKSTEN.items() if nl.count("%") != en.count("%")]
    if bad:
        raise SystemExit(f"herinnering: % mismatch {bad}")
    print(f"herinnering: island ({blokken} blocks, {mensen} people), {len(BELLEN)} bellen, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
