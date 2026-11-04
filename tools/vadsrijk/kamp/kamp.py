"""Knuffelkamp: rekrutenkamp van De Knuffelorde op een groen klif-eiland (Guhs 1.10.0, Het Vadsrijk).

bouw(S) zet het hele eiland (~250x250) in een Scene van KAMP_MAAT. Elk onderdeel is een eigen functie met
een ankerpunt, zodat dit later het echte stempelscript kan worden:
  eiland, paden, kampplein, tent, ordetent, trainingsveld, tempeltje, vijver, vuurtoren, steiger, luchtschip,
  guhballon, bomen_en_bloemen.
Assen: x oost, y omhoog, z zuid. Zeeniveau (bovenkant water) = ZEE.
"""
import math, random
import numpy as np
import vrkit as K
from vrkit import stairs, slab, Rot, pix, oor, oortjes, vlag

KAMP_MAAT = (340, 86, 310)
ZEE = 10
CX, CZ = 150, 155           # midden van het eiland
KAMP = (150, 160)           # kampvuur
VLOER = 34                  # bovenkant gras rond het kamp
TEMPEL = (98, 200)
VIJVER = (204, 204)
TRAINING = (212, 116)
ORDETENT = (150, 126)
LICHT = (66, 72)            # vuurtoren
STEIGER_Z = 150

GRAS, AARDE = "minecraft:grass_block", "minecraft:dirt"
PAD, KLINKER = "minecraft:dirt_path", "guhs:knuffelklinkers"
ROZESTEEN, GLAD, PASTEL = "guhs:roze_knuffelsteen", "guhs:gladde_knuffelsteen", "guhs:pastelbaksteen_roze"
WOL, BETON, WIT = "minecraft:pink_wool", "minecraft:pink_concrete", "minecraft:white_wool"
HEK = "minecraft:cherry_fence"
LAMP = "guhs:guh_lantaarnpaal"
LAMP_O, LAMP_B = LAMP + "[half=lower]", LAMP + "[half=upper]"

rng = random.Random(110)


class Eiland:
    """Hoogtekaart + bezette vakjes (zodat bomen niet in gebouwen groeien)."""

    def __init__(self, S):
        self.S = S
        X, _, Z = S.v.shape
        self.X, self.Z = X, Z
        self.H = np.full((X, Z), -1, int)
        self.bezet = np.zeros((X, Z), bool)
        self.kust = np.zeros((X, Z), float)

    def h(self, x, z):
        return int(self.H[x, z])

    def claim(self, x1, z1, x2, z2, marge=2):
        self.bezet[max(0, x1 - marge):x2 + marge + 1, max(0, z1 - marge):z2 + marge + 1] = True

    def vlak(self, x1, z1, x2, z2, y, rand=6):
        """Maak het terrein in de rechthoek precies y hoog, met een zachte overgang erbuiten."""
        for x in range(x1 - rand, x2 + rand + 1):
            for z in range(z1 - rand, z2 + rand + 1):
                if not (0 <= x < self.X and 0 <= z < self.Z) or self.H[x, z] < 0:
                    continue
                d = max(x1 - x, x - x2, z1 - z, z - z2, 0)
                t = min(1.0, d / max(1, rand))
                self.H[x, z] = int(round(y * (1 - t) + self.H[x, z] * t))


def _hoekverschil(a, b):
    return (a - b + math.pi) % (2 * math.pi) - math.pi


def eiland(S):
    """Hoogtekaart, zee en klif. Geeft een Eiland-object terug met H (bovenkant per kolom, -1 = zee)."""
    I = Eiland(S)
    X, Z = I.X, I.Z
    xs, zs = np.meshgrid(np.arange(X), np.arange(Z), indexing="ij")
    dx, dz = xs - CX, zs - CZ
    r = np.hypot(dx, dz)
    th = np.arctan2(dz, dx)
    Rr = 108 + 7 * np.sin(3 * th + 1) + 4 * np.sin(7 * th + 0.5) + 2.5 * np.sin(13 * th + 2)
    hv = np.vectorize(_hoekverschil)
    Rr += 17 * np.exp(-(hv(th, -3 * math.pi / 4) / 0.2) ** 2)    # kaap met de vuurtoren (NW)
    Rr += 7 * np.exp(-(hv(th, 0.0) / 0.25) ** 2)                 # oostkust voor de steiger
    d = Rr - r
    I.kust = d
    n = K.ruis(7)
    nv = np.vectorize(n)(xs, zs)
    camp_d = np.hypot(xs - KAMP[0], zs - KAMP[1])
    golf = (3.5 * np.sin(xs / 21 + 1) * np.cos(zs / 17) + 2.0 * nv) * np.clip((camp_d - 45) / 40, 0, 1)
    plateau = VLOER + golf
    plateau += 5 * np.exp(-((xs - TEMPEL[0]) ** 2 + (zs - TEMPEL[1]) ** 2) / (2 * 16 ** 2))
    plateau += 5 * np.exp(-((xs - LICHT[0]) ** 2 + (zs - LICHT[1]) ** 2) / (2 * 22 ** 2))
    klif = ZEE + 1 + d * 5.0 + 3 * nv
    h = np.minimum(plateau, klif)
    h = np.where(d < 0, -1, np.maximum(h, ZEE + 1)).round().astype(int)
    I.H = h
    return I


def zet_terrein(I):
    """Schrijft de hoogtekaart als blokken (alleen de zichtbare schil, dat rendert veel sneller)."""
    S, H = I.S, I.H
    X, Z = I.X, I.Z
    sid = {b: S.id(b) for b in (GRAS, AARDE, "minecraft:stone", "minecraft:andesite", "minecraft:tuff",
                                 "minecraft:calcite", "minecraft:moss_block", "minecraft:mossy_cobblestone",
                                 "minecraft:sand", "minecraft:prismarine", "minecraft:dark_prismarine",
                                 "minecraft:gravel", "minecraft:water", "minecraft:diorite",
                                 "minecraft:light_blue_terracotta")}
    lagen = ["minecraft:stone", "minecraft:andesite", "minecraft:stone", "minecraft:tuff", "minecraft:stone",
             "minecraft:calcite", "minecraft:stone", "minecraft:diorite"]
    Hp = np.pad(H, 1, constant_values=-1)
    buur = np.minimum.reduce([Hp[:-2, 1:-1], Hp[2:, 1:-1], Hp[1:-1, :-2], Hp[1:-1, 2:]])
    for x in range(X):
        for z in range(Z):
            h = H[x, z]
            if h < 0:
                dk = -I.kust[x, z]
                b = "minecraft:sand" if dk < 5 else "minecraft:prismarine" if dk < 16 else "minecraft:light_blue_terracotta"
                S.v[x, ZEE - 1, z] = sid[b]
                S.v[x, ZEE, z] = sid["minecraft:water"]
                continue
            lo = max(ZEE - 2, min(h, (buur[x, z] if buur[x, z] >= 0 else ZEE - 1)) - 1)
            for y in range(lo, h + 1):
                steil = h - max(buur[x, z], ZEE) >= 3
                if y == h:
                    b = GRAS
                elif y >= h - (0 if steil else 2) and not (steil and y < h):
                    b = AARDE
                elif y <= ZEE + 1:
                    b = "minecraft:mossy_cobblestone" if (x + z) % 3 else "minecraft:gravel"
                else:
                    k = (y + int(2 * math.sin(x / 7.0) + 2 * math.cos(z / 9.0))) // 3
                    b = lagen[k % len(lagen)]
                    if y >= h - 6 and rng.random() < 0.18:
                        b = "minecraft:moss_block"
                S.v[x, y, z] = sid[b]


def top(I, x, z, b):
    h = I.h(x, z)
    if h >= 0:
        I.S.set(x, h, z, b)


def lijn(I, pts, w=1, blok=PAD, mix=None, kans=0.0):
    """Pad over het terrein langs een polylijn; mix = tweede blok met kans."""
    for (x1, z1), (x2, z2) in zip(pts, pts[1:]):
        n = int(max(abs(x2 - x1), abs(z2 - z1))) + 1
        for i in range(n):
            t = i / max(1, n - 1)
            x, z = x1 + (x2 - x1) * t, z1 + (z2 - z1) * t
            for ox in range(-w, w + 1):
                for oz in range(-w, w + 1):
                    if ox * ox + oz * oz <= w * w + 0.5:
                        xx, zz = int(round(x + ox)), int(round(z + oz))
                        if I.S.get(xx, I.h(xx, zz), zz) == GRAS:
                            top(I, xx, zz, mix if (mix and rng.random() < kans) else blok)


# ----------------------------------------------------------------------------------------------------- kamp
def kampplein(I):
    S = I.S
    cx, cz = KAMP
    y = VLOER
    I.claim(cx - 15, cz - 15, cx + 15, cz + 15, 0)
    for x in range(cx - 14, cx + 15):
        for z in range(cz - 14, cz + 15):
            d = math.hypot(x - cx, z - cz)
            if d <= 10.5 or (d <= 13.5 and rng.random() < 0.6):
                S.set(x, y, z, KLINKER if d <= 9 or rng.random() < 0.4 else PAD)
    # vuurkuil: ring van steen, 4 kampvuren
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            if max(abs(x - cx), abs(z - cz)) == 2 or (abs(x - cx) == 2 and abs(z - cz) == 2):
                S.set(x, y + 1, z, slab("minecraft:stone_brick_slab"))
    for (x, z) in ((cx - 1, cz - 1), (cx, cz - 1), (cx - 1, cz), (cx, cz)):
        S.set(x, y + 1, z, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
    # boomstambanken in een kring
    for a0 in (20, 110, 200, 290):
        for k in (-1, 0, 1):
            a = math.radians(a0 + k * 11)
            x, z = int(round(cx + 6.5 * math.cos(a))), int(round(cz + 6.5 * math.sin(a)))
            ax = "z" if abs(math.cos(a)) > abs(math.sin(a)) else "x"
            S.set(x, y + 1, z, f"minecraft:spruce_log[axis={ax}]")
    # guhs rond het vuur
    for a, sit in ((25, True), (115, True), (205, True), (295, False), (160, False), (330, False)):
        ar = math.radians(a)
        rr = 6.5 if sit else 4.6
        x, z = cx + rr * math.cos(ar), cz + rr * math.sin(ar)
        yaw = (math.degrees(math.atan2(cx - x, -(cz - z))) + 360) % 360
        S.guh(x + 0.5, y + (2 if sit else 1), z + 0.5, yaw, sitting=sit)
    for (x, z) in ((cx - 11, cz - 11), (cx + 11, cz - 11), (cx - 11, cz + 11), (cx + 11, cz + 11)):
        S.set(x, y + 1, z, LAMP_O)
        S.set(x, y + 2, z, LAMP_B)
    # vlaggen van de orde rond het plein
    for (x, z, u) in ((cx - 13, cz - 3, (0, 1)), (cx + 13, cz - 3, (0, 1)), (cx - 3, cz + 13, (1, 0))):
        vlag(S, x, y + 1, z, u, h=5, w=3, paal_h=9, embleem="guhs:mini_luchtballon")


def tent(I, x, z, facing, kleur="guhs:pleister_roze", h=4, diep=9):
    """A-tent, breedte 2h-1, gestreept roze/crème. (x,z) = midden van de voorkant."""
    S = I.S
    y = VLOER + 1
    T = Rot(S, x, y, z, facing)
    creme = "guhs:campingmarkt_tentdoek_creme"
    for lz in range(diep):
        streep = kleur if lz % 2 == 0 else creme
        for k in range(h):
            for side, f in ((1, "left"), (-1, "right")):
                T.set(side * (h - 1 - k), k, lz, stairs(streep + "_trap" if streep == creme else streep + "_trap", f))
        T.set(0, h - 1, lz, streep)
    for lz in (0, diep - 1):
        for k in range(h - 1):
            for lx in range(-(h - 2 - k), h - 1 - k):
                if lz == 0 and lx == 0 and k < 2:
                    continue
                T.set(lx, k, lz, creme)
    # interior: a plank floor with carpets (no grass or flowers inside the tent)
    for lz in range(0, diep):
        for lx in range(-(h - 2), h - 1):
            T.set(lx, -1, lz, "minecraft:spruce_planks")
            if 0 < lz < diep - 1:
                T.set(lx, 0, lz, "minecraft:pink_carpet" if lx == 0 else "minecraft:white_carpet")
    T.set(0, h, 0, "minecraft:cherry_fence")
    T.set(0, h + 1, 0, WOL)
    T.set(1, h + 1, 0, WOL)
    if rng.random() < 0.5:
        T.set(h, 0, -1, LAMP_O)
        T.set(h, 1, -1, LAMP_B)
    else:
        T.set(h, 0, -1, "minecraft:barrel[facing=up,open=false]")
    cs = [T.w(lx, 0, lz) for lx in (-h, h) for lz in (0, diep)]
    I.claim(min(c[0] for c in cs), min(c[2] for c in cs), max(c[0] for c in cs), max(c[2] for c in cs), 1)
    T.guh(-h + 1, 0, -2, 0)


def ordetent(I):
    """Grote commandotent van De Knuffelorde, met wollen wanden, guhgezicht en oortjes."""
    S = I.S
    x, z = ORDETENT
    y = VLOER + 1
    T = Rot(S, x, y, z, "south")
    W, D, WAND = 7, 15, 3
    for lz in range(D):
        for lx in range(-W, W + 1):
            if abs(lx) == W:
                for k in range(WAND):
                    T.set(lx, k, lz, WOL if lz % 3 else WIT)
        for k in range(W + 1):
            rij = WAND + k
            streep = "guhs:pleister_roze" if lz % 2 == 0 else "guhs:campingmarkt_tentdoek_creme"
            for side, f in ((1, "left"), (-1, "right")):
                T.set(side * (W - k), rij, lz, stairs(streep + "_trap", f))
        T.set(0, WAND + W, lz, "guhs:guh_dakpan_roze")
    for lz in (0, D - 1):
        for lx in range(-W + 1, W):
            for k in range(WAND + W - abs(lx)):
                if lz == 0 and abs(lx) <= 1 and k < 4:
                    continue
                T.set(lx, k, lz, "guhs:campingmarkt_tentdoek_creme" if k >= WAND else WOL)
    # interior: plank floor with a red carpet runner (no grass inside)
    for lz in range(0, D):
        for lx in range(-W + 1, W):
            T.set(lx, -1, lz, "minecraft:spruce_planks")
            if 0 < lz < D - 1:
                T.set(lx, 0, lz, "minecraft:red_carpet" if abs(lx) <= 1 else "minecraft:pink_carpet")
    # guhgezicht boven de ingang + oortjes op de nok
    pix(S, *T.w(-2, WAND + 6, -1), T.r, K.GEZICHT_5, K.GEZICHT_KEY)
    fx, fz = T.w(0, 0, 1)[0], T.w(0, 0, 1)[2]
    oortjes(S, fx, y + WAND + W - 4, fz, T.r, gap=2, h=8, w=3)
    for lx in (-4, 4):
        vlag(S, *T.w(lx, 0, -2), T.r if lx > 0 else (-T.r[0], -T.r[1]), h=6, w=3, paal_h=11,
             embleem="guhs:mini_luchtballon")
    T.guh(0, 0, -3, 0)
    T.guh(2, 0, -2, 20)
    I.claim(x - W - 3, z - 3, x + W + 3, z + D + 1)


def trainingsveld(I):
    """Omheind oefenveld met trainingspoppen (hooi + roos) en schietschijven."""
    S = I.S
    x0, z0 = TRAINING
    x1, z1, x2, z2 = x0 - 16, z0 - 10, x0 + 16, z0 + 10
    I.vlak(x1, z1, x2, z2, VLOER, 4)
    I.claim(x1, z1, x2, z2)
    y = VLOER
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            S.set(x, y, z, "minecraft:coarse_dirt" if rng.random() < 0.55 else
                  ("minecraft:sand" if rng.random() < 0.3 else PAD))
            if x in (x1, x2) or z in (z1, z2):
                if not (z in (z1, z2) and abs(x - x0) <= 1) and not (x == x1 and abs(z - z0) <= 1):
                    S.set(x, y + 1, z, HEK)
    for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
        S.set(x, y + 1, z, "minecraft:stripped_cherry_log[axis=y]")
        S.set(x, y + 2, z, LAMP_O)
        S.set(x, y + 3, z, LAMP_B)
    for i in range(4):
        for j in range(2):
            px, pz = x1 + 5 + i * 5, z0 - 4 + j * 8
            S.set(px, y + 1, pz, "minecraft:oak_fence")
            S.set(px, y + 2, pz, "minecraft:hay_block")
            S.set(px - 1, y + 2, pz, "minecraft:oak_fence")
            S.set(px + 1, y + 2, pz, "minecraft:oak_fence")
            S.set(px, y + 3, pz, "minecraft:target")
    for j in range(3):
        pz = z0 - 6 + j * 6
        S.set(x2 - 3, y + 1, pz, "minecraft:oak_fence")
        S.set(x2 - 3, y + 2, pz, "minecraft:target")
        S.set(x2 - 3, y + 3, pz, "minecraft:target")
    S.set(x1 + 3, y + 1, z0, "minecraft:barrel[facing=up,open=false]")
    S.set(x1 + 3, y + 1, z0 + 1, "minecraft:hay_block")
    S.guh(x1 + 7.5, y + 1, z0 + 0.5, 90)
    S.guh(x1 + 13.5, y + 1, z0 + 0.5, 90)
    S.guh(x2 - 9.5, y + 1, z0 - 2.5, 90)
    vlag(S, x0, y + 1, z1 - 2, (1, 0), kleur="minecraft:red_wool", h=4, w=3, paal_h=8)


def tempeltje(I, facing="east"):
    """Klasse-tempeltje: roze zuilen, zadeldak met guhgezicht en oortjes, drie klasse-sokkels ervoor."""
    S = I.S
    tx, tz = TEMPEL
    y0 = I.h(tx, tz)
    I.vlak(tx - 16, tz - 16, tx + 16, tz + 16, y0, 6)
    I.claim(tx - 16, tz - 16, tx + 16, tz + 16)
    T = Rot(S, tx, y0 + 1, tz, facing)
    W, D = 9, 22
    # sokkel + trap
    for lz in range(0, D + 1):
        for lx in range(-W, W + 1):
            for k in range(3):
                T.set(lx, k - 1, lz, GLAD if k == 2 else PASTEL)
    for s in range(3):
        for lx in range(-5, 6):
            T.set(lx, s - 1, -3 + s, stairs("guhs:gladde_knuffelsteen_trap", "back"))
            for k in range(s):
                T.set(lx, k - 1, -3 + s, PASTEL)
    # zuilen rondom
    zuilen = [(lx, 1) for lx in (-8, -4, 4, 8)] + [(s * 8, lz) for s in (-1, 1) for lz in (5, 9, 13, 17, 21)]
    for (lx, lz) in zuilen:
        T.set(lx, 2, lz, ROZESTEEN)
        for k in range(3, 9):
            T.set(lx, k, lz, "guhs:pastelbaksteen_roze_muur")
        T.set(lx, 9, lz, GLAD)
    # cella
    for lz in range(5, 21):
        for lx in range(-5, 6):
            if abs(lx) == 5 or lz in (5, 20):
                for k in range(2, 10):
                    if lz == 5 and abs(lx) <= 1 and k <= 5:
                        continue
                    b = ROZESTEEN
                    if abs(lx) == 5 and lz % 4 == 1 and 4 <= k <= 6:
                        b = "guhs:glasinlood_guh"
                    T.set(lx, k, lz, b)
    # cella floor: pink/white marble (quartz + pink concrete checker)
    for lz in range(6, 20):
        for lx in range(-4, 5):
            T.set(lx, 1, lz, "minecraft:quartz_block" if (lx + lz) % 2 else "minecraft:pink_concrete")
    for lz in range(4, 6):
        for lx in range(-1, 2):
            T.set(lx, 1, lz, "minecraft:quartz_block")
    # architraaf
    for lz in range(0, D + 1):
        for lx in range(-W, W + 1):
            if abs(lx) == W or lz in (0, D):
                T.set(lx, 10, lz, GLAD)
                T.set(lx, 11, lz, PASTEL)
            elif abs(lx) >= 5 or lz < 5:
                T.set(lx, 10, lz, GLAD, roof=True)
    # zadeldak
    for k in range(W + 1):
        for lz in range(-1, D + 2):
            for side, f in ((1, "left"), (-1, "right")):
                T.set(side * (W + 1 - k), 11 + k, lz, stairs("guhs:guh_dakpan_roze_trap", f), roof=True)
    for lz in range(-1, D + 2):
        T.set(0, 11 + W + 1, lz, "guhs:guh_dakpan_roze", roof=True)
    for lz in (0, D):
        for k in range(1, W + 1):
            for lx in range(-(W - k), W - k + 1):
                T.set(lx, 11 + k, lz, "guhs:pleister_roze", roof=True)
    pix(S, *T.w(-2, 18, -1), T.r, K.GEZICHT_5, K.GEZICHT_KEY)
    fx, _, fz = T.w(0, 0, 2)
    oortjes(S, fx, y0 + 1 + 11 + W - 3, fz, T.r, gap=2, h=8, w=3)
    # drie klasse-sokkels (ridder rood, jager groen, magiër paars)
    for lx, kl, ding in ((-6, "minecraft:red_wool", "minecraft:anvil[facing=north]"),
                         (0, "minecraft:lime_wool", "minecraft:target"),
                         (6, "minecraft:purple_wool", "minecraft:enchanting_table")):
        T.set(lx, 0, -8, ROZESTEEN)
        T.set(lx, 1, -8, GLAD)
        T.set(lx, 2, -8, ding)
        vlag(S, *T.w(lx + 2, 0, -9), T.r, kleur=kl, h=4, w=2, paal_h=7)
        T.set(lx - 2, 0, -7, LAMP_O)
        T.set(lx - 2, 1, -7, LAMP_B)
    for lz in range(-12, -3):
        for lx in range(-2, 3):
            T.set(lx, -1, lz, KLINKER)
    T.guh(0, 2, 3, 0)
    T.guh(-6, 0, -10, 0)
    T.guh(6, 0, -11, 10)
    return T


def vijver(I):
    """Vijver met waterlelies, riet, een vissteigertje en lieve slijmpjes (klodders) aan de oever."""
    S = I.S
    vx, vz = VIJVER
    rx, rz = 15, 10
    I.vlak(vx - rx - 2, vz - rz - 2, vx + rx + 2, vz + rz + 2, VLOER, 6)
    I.claim(vx - rx - 1, vz - rz - 1, vx + rx + 1, vz + rz + 1, 1)
    y = VLOER
    for x in range(vx - rx - 2, vx + rx + 3):
        for z in range(vz - rz - 2, vz + rz + 3):
            e = ((x - vx) / rx) ** 2 + ((z - vz) / rz) ** 2 + 0.08 * math.sin(x * 0.7 + z * 0.4)
            if e <= 1:
                diep = 1 if e > 0.55 else 3
                for k in range(diep):
                    S.set(x, y - k, z, "minecraft:water")
                S.set(x, y - diep, z, "minecraft:clay" if diep > 1 else "minecraft:sand")
                if rng.random() < 0.05:
                    S.set(x, y + 1, z, "minecraft:lily_pad")
                elif rng.random() < 0.025:
                    S.set(x, y + 1, z, "guhs:guh_waterlelie")
            elif e <= 1.35:
                if rng.random() < 0.25:
                    S.set(x, y + 1, z, "guhs:bloesemmeertje_riet[half=lower]")
                    S.set(x, y + 2, z, "guhs:bloesemmeertje_riet[half=upper]")
                elif rng.random() < 0.15:
                    S.set(x, y, z, "minecraft:sand")
    # vissteigertje
    for x in range(vx - rx + 2, vx - rx + 8):
        S.set(x, y + 1, vz + 1, slab("minecraft:spruce_slab"))
        S.set(x, y + 1, vz, slab("minecraft:spruce_slab"))
    S.set(vx - rx + 8, y, vz - 1, "minecraft:spruce_fence")
    S.guh(vx - rx + 6.5, y + 1.5, vz + 0.5, 90, sitting=True)
    # klodders: roze, groen, geel; een moederklodder van 2x2x2
    soorten = ["guhs:roze_slijmblok", "minecraft:slime_block", "minecraft:honey_block"]
    for i in range(16):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(1.25, 1.7)
        x, z = int(vx + rx * rr * math.cos(a)), int(vz + rz * rr * math.sin(a))
        if 0 <= x < I.X and 0 <= z < I.Z and S.is_air(x, y + 1, z):
            S.set(x, I.h(x, z) + 1, z, soorten[i % 3])
    mx, mz = vx + rx + 3, vz - 4
    for dx in (0, 1):
        for dy in (1, 2):
            for dz in (0, 1):
                S.set(mx + dx, y + dy, mz + dz, "guhs:roze_slijmblok")


def vuurtoren(S, x, y, z, ear_u=(1, 0)):
    """Vuurtoren met roze/witte banden, omloop, lichtkamer en een koepel met guh-oortjes.
    (x, y, z) = midden van de voet; y = bovenkant van de grond. Geeft de hoogte van de omloop terug."""
    for xx in range(x - 7, x + 8):
        for zz in range(z - 7, z + 8):
            d = math.hypot(xx - x, zz - z)
            if d <= 7.2:
                S.set(xx, y, zz, GLAD if d > 6.2 else ROZESTEEN)
                S.set(xx, y - 1, zz, ROZESTEEN)
                S.set(xx, y - 2, zz, ROZESTEEN)
    H = 30
    for k in range(1, H + 1):
        rr = 5.2 - 1.2 * k / H
        band = WIT if (k // 5) % 2 == 0 else BETON
        if k <= 2:
            band = ROZESTEEN
        for xx in range(x - 6, x + 7):
            for zz in range(z - 6, z + 7):
                d = math.hypot(xx - x, zz - z)
                if rr - 1.3 < d <= rr:
                    b = band
                    if k % 9 == 4 and abs(xx - x) <= 0 and d > rr - 1.3:
                        b = "guhs:glasinlood_guh"
                    S.set(xx, y + k, zz, b)
    # deur naar het zuidoosten
    for k in (1, 2, 3):
        S.set(x + 3, y + k, z + 3, "minecraft:air")
        S.set(x + 4, y + k, z + 3, "minecraft:air")
    G = y + H + 1
    for xx in range(x - 7, x + 8):
        for zz in range(z - 7, z + 8):
            d = math.hypot(xx - x, zz - z)
            if d <= 6.3:
                S.set(xx, G, zz, GLAD)
            if 5.4 < d <= 6.3:
                S.set(xx, G + 1, zz, "minecraft:white_stained_glass_pane" if False else HEK)
            if 2.2 < d <= 3.3:
                for k in range(1, 6):
                    S.set(xx, G + k, zz, "minecraft:white_stained_glass")
            if d <= 1.6:
                for k in range(1, 6):
                    S.set(xx, G + k, zz, "minecraft:pearlescent_froglight")
    S.ellipsoid(x, G + 6, z, 4.4, 3.6, 4.4, WOL, ymin=G + 6)
    S.set(x, G + 10, z, "minecraft:lightning_rod[facing=up,powered=false]")
    oortjes(S, x, G + 8, z, ear_u, gap=2, h=5, w=3)
    return G


def steiger(I):
    """Aanlegsteiger voor het luchtschip, op palen langs de klif, met een poort met guhgezicht."""
    S = I.S
    z0 = STEIGER_Z
    kx = max(x for x in range(I.X) if I.H[x, z0] >= 0)  # oostelijkste landkolom
    y = VLOER
    I.vlak(kx - 30, z0 - 6, kx, z0 + 6, VLOER, 6)
    I.claim(kx - 30, z0 - 6, kx + 40, z0 + 6, 0)
    x_eind = kx + 30
    for x in range(kx - 12, x_eind + 1):
        for z in range(z0 - 2, z0 + 3):
            S.set(x, y, z, "minecraft:spruce_planks" if x > kx - 4 else KLINKER)
        if x > kx - 2:
            for z in (z0 - 3, z0 + 3):
                if not (z == z0 + 3 and kx + 14 <= x <= kx + 18):
                    S.set(x, y + 1, z, HEK)
                S.set(x, y, z, "minecraft:stripped_cherry_log[axis=x]")
            if (x - kx) % 7 == 3:
                for z in (z0 - 3, z0 + 3):
                    for yy in range(ZEE - 1, y):
                        S.set(x, yy, z, "minecraft:spruce_log[axis=y]")
                    S.set(x, y + 1, z, "minecraft:stripped_cherry_log[axis=y]")
                    S.set(x, y + 2, z, "minecraft:lantern[hanging=false,waterlogged=false]")
    # aanlegplatform aan het eind
    for x in range(x_eind - 6, x_eind + 3):
        for z in range(z0 - 9, z0 + 3):
            S.set(x, y, z, "minecraft:spruce_planks")
            if x in (x_eind + 2,) or z == z0 - 9:
                S.set(x, y + 1, z, HEK)
    for x in range(kx + 14, kx + 19):
        S.set(x, y, z0 + 4, slab("minecraft:spruce_slab"))
    for z in (z0 - 9, z0 + 2):
        for yy in range(ZEE - 1, y):
            S.set(x_eind - 6, yy, z, "minecraft:spruce_log[axis=y]")
            S.set(x_eind + 2, yy, z, "minecraft:spruce_log[axis=y]")
    S.set(x_eind - 2, y + 1, z0, "guhs:ballonsteiger")
    vlag(S, x_eind + 2, y + 1, z0 - 9, (0, 1), h=5, w=3, paal_h=10, embleem="guhs:mini_luchtballon")
    # welkomstpoort op het land
    px = kx - 8
    for z in (z0 - 4, z0 + 3):
        for dz in (0, 1):
            for k in range(1, 10):
                S.set(px, y + k, z + dz, ROZESTEEN if k % 3 else GLAD)
                S.set(px - 1, y + k, z + dz, ROZESTEEN)
    for z in range(z0 - 5, z0 + 6):
        S.set(px, y + 10, z, GLAD)
        S.set(px - 1, y + 10, z, GLAD)
        S.set(px, y + 11, z, PASTEL)
        S.set(px - 1, y + 11, z, PASTEL)
    pix(S, px + 1, y + 16, z0 - 2, (0, 1), K.GEZICHT_5, K.GEZICHT_KEY)
    for k in range(12, 17):
        for z in range(z0 - 2, z0 + 3):
            S.set(px, y + k, z, WOL)
    oortjes(S, px, y + 17, z0, (0, 1), gap=1, h=4, w=3)
    for z in (z0 - 5, z0 + 5):
        S.set(px + 1, y + 1, z, LAMP_O)
        S.set(px + 1, y + 2, z, LAMP_B)
    # rekruten en een havenguh
    for (gx, gz, a) in ((kx + 20, z0 + 1, 270), (kx + 22, z0 - 1, 270), (kx + 8, z0, 270), (x_eind - 3, z0 + 3, 200)):
        S.guh(gx + 0.5, y + 1, gz + 0.5, a)
    return kx, x_eind


def luchtschip(S, x0, y, cz, L=42):
    """Ballonschip met de boeg naar het oosten. x0 = achtersteven, y = dek, cz = midden.
    Romp van kersenhout met witte band, roze dek, kajuit, propeller; ballon met guhgezicht en oortjes."""
    hw_max = 5.5
    ballon_x = x0 + L // 2
    for i in range(L):
        t = i / (L - 1)
        hw = hw_max * (math.sqrt(max(0.0, 1 - ((t - 0.42) / 0.58) ** 2)) if t > 0.42 else min(1.0, 0.55 + t * 1.1))
        if hw < 0.6:
            hw = 0.6
        diep = 6 * (0.55 + 0.45 * math.sin(math.pi * min(1, t * 1.05)))
        for c in range(-6, 7):
            if abs(c) > hw + 0.3:
                continue
            bodem = y - diep * math.sqrt(max(0.0, 1 - (c / (hw + 0.6)) ** 2))
            for yy in range(int(math.floor(bodem)), y + 1):
                b = "minecraft:cherry_planks"
                if yy == y - 1:
                    b = "minecraft:white_wool" if True else b
                if yy == y:
                    b = "guhs:guhbloesem_planks" if abs(c) < hw - 0.7 else "minecraft:stripped_cherry_log[axis=x]"
                if yy < y - 4:
                    b = "minecraft:pink_terracotta"
                S.set(x0 + i, yy, cz + c, b)
            if abs(c) >= hw - 0.7 and 1 < i < L - 2:
                S.set(x0 + i, y + 1, cz + c, HEK)
    # kajuit achterop
    for i in range(3, 13):
        for c in range(-3, 4):
            for k in range(1, 5):
                if i in (3, 12) or abs(c) == 3:
                    b = PASTEL
                    if k in (2, 3) and (i in (6, 9) or c == 0):
                        b = "guhs:glasinlood_guh"
                    if k <= 2 and i == 12 and c == 0:
                        b = "minecraft:air"
                    S.set(x0 + i, y + k, cz + c, b)
            S.set(x0 + i, y + 5, cz + c, slab("guhs:guh_dakpan_roze_plaat"))
    # propeller aan de achtersteven
    for k in range(-3, 4):
        S.set(x0 - 1, y - 2 + k, cz, "minecraft:spruce_planks")
        S.set(x0 - 1, y - 2, cz + k, "minecraft:spruce_planks")
    S.set(x0, y - 2, cz, "minecraft:stripped_cherry_log[axis=x]")
    # boegspriet + boegbeeldje
    for i in range(4):
        S.set(x0 + L + i - 1, y + 1 + i // 2, cz, "minecraft:stripped_cherry_log[axis=x]")
    S.set(x0 + L + 3, y + 3, cz, "guhs:mini_luchtballon")
    # ballon
    by = y + 17
    rx, ry, rz = 23, 10, 11
    S.ellipsoid(ballon_x, by, cz, rx, ry, rz, WOL, hollow=1.6)
    for (x, yy, z) in np.argwhere(S.v[ballon_x - rx - 1:ballon_x + rx + 2, by - ry - 1:by + ry + 2,
                                      cz - rz - 1:cz + rz + 2] == S.id(WOL)):
        X_, Y_, Z_ = x + ballon_x - rx - 1, yy + by - ry - 1, z + cz - rz - 1
        if abs(Y_ - by) <= 0 or (X_ - ballon_x) % 9 == 0:
            S.set(X_, Y_, Z_, WIT)
    # guhgezicht op de neus (2x geschaald), op de buitenste blokken in +x-richting
    rows = [r for r in K.GEZICHT_9 for _ in (0, 1)]
    rows = ["".join(ch * 2 for ch in r) for r in rows]
    h_, w_ = len(rows), len(rows[0])
    for i, row in enumerate(rows):
        for j, ch in enumerate(row):
            b = K.GEZICHT_KEY.get(ch)
            yy, zz = by + h_ // 2 - 1 - i, cz - w_ // 2 + j
            col = S.v[:, yy, zz]
            xs = [x for x in range(ballon_x, ballon_x + rx + 2) if col[x] != 0]
            if xs and b:
                S.set(max(xs), yy, zz, b)
    # oortjes bovenop, vlak bij de neus
    ox = ballon_x + rx - 9
    oortjes(S, ox, by + ry - 4, cz, (0, 1), gap=4, h=9, w=5, dik=(-1, 0))
    # staartvinnen
    for k in range(8):
        for i in range(8 - k):
            S.set(ballon_x - rx + 2 - i, by + k, cz, BETON)
            S.set(ballon_x - rx + 2 - i, by, cz + k, BETON)
            S.set(ballon_x - rx + 2 - i, by, cz - k, BETON)
    # touwen van romp naar ballon
    for i in (8, 16, 26, 34):
        for c in (-4, 4):
            x, z = x0 + i, cz + c
            for yy in range(y + 1, by):
                if S.is_air(x, yy, z):
                    S.set(x, yy, z, "minecraft:spruce_fence")
                else:
                    break
    for (gi, gc, a) in ((20, 0, 90), (24, 2, 90), (30, -2, 60)):
        S.guh(x0 + gi + 0.5, y + 1, cz + gc + 0.5, a)


def guhballon(S, x, y, z, r=6):
    """Losse guh-heteluchtballon: roze bol met gezicht en oortjes, mandje eronder."""
    S.ellipsoid(x, y, z, r, r + 1, r, WOL, hollow=1.5)
    for i, row in enumerate(K.GEZICHT_5):
        for j, ch in enumerate(row):
            b = K.GEZICHT_KEY.get(ch)
            yy, xx = y + 1 - i, x - 2 + j
            col = S.v[xx, yy, :]
            zs = [zz for zz in range(z, z + r + 2) if col[zz] != 0]
            if zs and b and b != WOL:
                S.set(xx, yy, max(zs), b)
    oortjes(S, x, y + r - 1, z, (1, 0), gap=2, h=4, w=3)
    for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for k in range(1, 5):
            S.set(x + dx, y - r - k + 1, z + dz, "minecraft:spruce_fence")
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            S.set(x + dx, y - r - 4, z + dz, "minecraft:spruce_planks")
            if max(abs(dx), abs(dz)) == 2:
                S.set(x + dx, y - r - 3, z + dz, "minecraft:spruce_planks")
    S.guh(x + 0.5, y - r - 3, z + 0.5, 180)


BOOM_ROZE = ("guhs:guhbloesem_log[axis=y]", "guhs:guhbloesem_leaves")
BOOM_GROEN = ("minecraft:oak_log[axis=y]", "minecraft:azalea_leaves")
BOOM_BLOEI = ("minecraft:oak_log[axis=y]", "minecraft:flowering_azalea_leaves")


def boom(S, x, y, z, soort, h=5, r=3.2):
    log, leaves = soort
    for k in range(1, h + 1):
        S.set(x, y + k, z, log)
    S.ellipsoid(x, y + h + 1, z, r, r * 0.75, r, leaves, keep=True)


def bomen_en_bloemen(I, n_bomen=170):
    S = I.S
    gezet = 0
    pogingen = 0
    while gezet < n_bomen and pogingen < 5000:
        pogingen += 1
        x, z = rng.randrange(8, I.X - 8), rng.randrange(8, I.Z - 8)
        if I.H[x, z] < VLOER - 4 or I.kust[x, z] < 9 or I.bezet[x, z]:
            continue
        if math.hypot(x - KAMP[0], z - KAMP[1]) < 22:
            continue
        if any(S.get(x + dx, I.h(x + dx, z + dz), z + dz) in (PAD, KLINKER, "minecraft:coarse_dirt")
               for dx in range(-3, 4) for dz in range(-3, 4) if 0 <= x + dx < I.X and 0 <= z + dz < I.Z):
            continue                      # (no trees on or overhanging the paths)
        soort = rng.choice([BOOM_ROZE, BOOM_ROZE, BOOM_GROEN, BOOM_BLOEI])
        boom(S, x, I.h(x, z), z, soort, h=rng.randint(4, 7), r=rng.uniform(2.6, 3.8))
        I.claim(x - 2, z - 2, x + 2, z + 2, 0)
        gezet += 1
    bloemen = ["minecraft:short_grass"] * 10 + ["minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy",
                                                 "guhs:guhoortjes", "guhs:roze_guhbloem", "minecraft:cornflower"] * 2
    for x in range(I.X):
        for z in range(I.Z):
            h = I.H[x, z]
            if h < 0 or I.kust[x, z] < 2:
                continue
            if S.get(x, h, z) == GRAS and S.is_air(x, h + 1, z) and rng.random() < 0.13:
                S.set(x, h + 1, z, rng.choice(bloemen))


def bouw(S):
    """Hele Knuffelkamp in Scene S (minstens KAMP_MAAT). Geeft het Eiland-object terug."""
    I = eiland(S)
    # gebouwplekken eerst vlak maken, dan pas de blokken zetten
    I.vlak(KAMP[0] - 34, KAMP[1] - 40, KAMP[0] + 34, KAMP[1] + 32, VLOER, 8)
    for (x, z) in (TEMPEL, VIJVER, TRAINING):
        pass
    vx, vz = VIJVER
    I.vlak(vx - 18, vz - 13, vx + 18, vz + 13, VLOER, 6)
    tx, tz = TRAINING
    I.vlak(tx - 17, tz - 11, tx + 17, tz + 11, VLOER, 5)
    I.vlak(LICHT[0] - 8, LICHT[1] - 8, LICHT[0] + 8, LICHT[1] + 8, I.h(*LICHT), 4)
    I.vlak(TEMPEL[0] - 16, TEMPEL[1] - 16, TEMPEL[0] + 16, TEMPEL[1] + 16, I.h(*TEMPEL), 6)
    kz = STEIGER_Z
    kx = max(x for x in range(I.X) if I.H[x, kz] >= 0)
    I.vlak(kx - 30, kz - 6, kx - 1, kz + 6, VLOER, 6)
    zet_terrein(I)
    # paden
    lijn(I, [(kx - 8, kz), (215, 150), (KAMP[0] + 12, KAMP[1] - 3)], 1, PAD, KLINKER, 0.3)
    lijn(I, [(KAMP[0] - 10, KAMP[1] + 8), (122, 184), (TEMPEL[0] + 24, TEMPEL[1])], 1, PAD, KLINKER, 0.3)
    lijn(I, [(KAMP[0] + 9, KAMP[1] + 9), (VIJVER[0] - 14, VIJVER[1] - 8)], 1, PAD)
    lijn(I, [(KAMP[0] + 8, KAMP[1] - 10), (TRAINING[0] - 2, TRAINING[1] + 11)], 1, PAD)
    lijn(I, [(KAMP[0] - 6, KAMP[1] - 12), (118, 112), (92, 96), (LICHT[0] + 7, LICHT[1] + 7)], 1, PAD, KLINKER, 0.15)
    kampplein(I)
    ordetent(I)
    cx, cz = KAMP
    for (a, rr) in ((20, 22), (68, 22), (104, 22), (168, 22), (198, 23), (300, 30), (238, 32)):
        ar = math.radians(a)
        x, z = int(cx + rr * math.cos(ar)), int(cz + rr * math.sin(ar))
        dx, dz = cx - x, cz - z
        fac = ("east" if dx > 0 else "west") if abs(dx) > abs(dz) else ("south" if dz > 0 else "north")
        tent(I, x, z, fac, kleur=rng.choice(["guhs:pleister_roze", "guhs:pleister_roze", "guhs:pastelbaksteen_roze"]))
    trainingsveld(I)
    tempeltje(I, "east")
    vijver(I)
    lx, lz = LICHT
    I.claim(lx - 10, lz - 10, lx + 10, lz + 10)
    vuurtoren(S, lx, I.h(lx, lz), lz)
    # vuurtorenwachtershuisje
    hx, hz, hy = lx + 9, lz + 2, I.h(lx + 9, lz + 2)
    for x in range(hx, hx + 6):
        for z in range(hz, hz + 5):
            for k in range(0, 4):
                if x in (hx, hx + 5) or z in (hz, hz + 4) or k == 0:
                    S.set(x, hy + k, z, PASTEL if k else ROZESTEEN)
    for k in range(3):
        for z in range(hz - 1, hz + 6):
            S.set(hx - 1 + k, hy + 4 + k, z, stairs("guhs:guh_dakpan_roze_trap", "east"))
            S.set(hx + 6 - k, hy + 4 + k, z, stairs("guhs:guh_dakpan_roze_trap", "west"))
    for z in range(hz - 1, hz + 6):
        S.set(hx + 2, hy + 7, z, slab("guhs:guh_dakpan_roze_plaat"))
        S.set(hx + 3, hy + 7, z, slab("guhs:guh_dakpan_roze_plaat"))
    # a door in the south wall + a step outside it, a plank floor inside
    for x in range(hx + 1, hx + 5):
        for z in range(hz + 1, hz + 4):
            S.set(x, hy, z, "minecraft:spruce_planks")
            for k in (1, 2, 3):
                S.set(x, hy + k, z, "minecraft:air")
    S.set(hx + 2, hy + 1, hz + 4, "minecraft:spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]")
    S.set(hx + 2, hy + 2, hz + 4, "minecraft:spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]")
    for dz in (5, 6):
        S.set(hx + 2, hy, hz + dz, PAD)
        for k in (1, 2, 3):
            S.set(hx + 2, hy + k, hz + dz, "minecraft:air")
    S.guh(lx + 5.5, I.h(lx + 5, lz + 6) + 1, lz + 6.5, 135)
    kx, xe = steiger(I)
    luchtschip(S, kx + 2, VLOER - 1, STEIGER_Z + 10)
    guhballon(S, 300, 64, 70)
    bomen_en_bloemen(I)
    return I


def sokkel(N, y_bodem):
    """Maakt de snijranden van een uitsnede netjes dicht (aarde/steen tot y_bodem), als een diorama-plaat."""
    X, Y, Z = N.v.shape
    boom = [i for i, p in enumerate(N.pal) if p.split("[")[0].endswith(("_leaves", "_log")) and "cherry" not in p]
    rand = np.zeros((X, Z), bool)
    rand[:6, :] = rand[-6:, :] = True
    rand[:, :6] = rand[:, -6:] = True
    for i in boom:
        m = (N.v == i) & rand[:, None, :]
        N.v[m] = 0
    for x in range(X):
        for z in range(Z):
            if x in (0, X - 1) or z in (0, Z - 1):
                h = N.top_y(x, z)
                while h > 0 and N.get(x, h, z) not in (GRAS, PAD, KLINKER, AARDE, "minecraft:coarse_dirt",
                                                       "minecraft:sand", "minecraft:water", "minecraft:clay"):
                    h -= 1
                for y in range(y_bodem, h):
                    if N.is_air(x, y, z) or y < h:
                        N.set(x, y, z, AARDE if y >= h - 3 else "minecraft:stone")
            N.v[x, :y_bodem, z] = 0


if __name__ == "__main__":
    import sys
    ss = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    wat = sys.argv[2] if len(sys.argv) > 2 else "alles"
    S = K.Scene(*KAMP_MAAT)
    bouw(S)
    print("blokken:", int((S.v > 0).sum()), flush=True)
    suf = "" if ss == 2 else "_schets"
    if wat in ("alles", "overzicht"):
        K.render(S, "knuffelkamp_overzicht" + suf, yaw=35, pitch=32, scale=4.6, ss=ss, wolken=0)
    if wat in ("alles", "midden"):
        N = K.uitsnede(S, 76, 98, 228, 222)
        sokkel(N, VLOER - 7)
        K.render(N, "knuffelkamp_kampmidden" + suf, yaw=30, pitch=34, scale=9.5, ss=ss)
