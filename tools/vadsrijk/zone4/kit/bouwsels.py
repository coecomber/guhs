"""Bouwsels voor de Lampionkust: huisjes op palen, steigers, lampionsnoeren, vuurtorens, bootjes, vliegers.

Alles werkt met een Plek: een lokale bouwplaats (1 voxel = 1 blok) met SPELcoördinaten, die je met plek.zet(W) in
elk venster stempelt (op elke schaal k). Kleine dingen die geen blok zijn (touwen, vliegers, zeilen) gaan als
extra-quads mee (alleen bij k >= 0.5), guh-figuurtjes en deeltjes alleen bij k = 1.
"""
import math
import numpy as np
import schemerkit as SK
import mountkit as MK
import kust as KU

ZEE = KU.ZEE
MUREN = ["pastelbaksteen_roze", "pastelbaksteen_perzik", "pastelbaksteen_aqua", "pastelbaksteen_vanille",
         "pastelbaksteen_lavendel", "pastelbaksteen_wolkje", "pastelbaksteen_pistache", "pleister_roze",
         "pastelbaksteen_boter", "pastelbaksteen_hemelsblauw"]
KOZIJN = {"pastelbaksteen_roze": "wit", "pastelbaksteen_perzik": "wit", "pastelbaksteen_aqua": "roze",
          "pastelbaksteen_vanille": "roze", "pastelbaksteen_lavendel": "wit", "pastelbaksteen_wolkje": "roze",
          "pastelbaksteen_pistache": "roze", "pleister_roze": "wit", "pastelbaksteen_boter": "hemelsblauw",
          "pastelbaksteen_hemelsblauw": "wit"}
DAKEN = ["guh_dakpan_roze", "guh_dakpan_roze", "guh_dakpan_wit", "dakpan_roze"]
LAMPIONS = ["guhs:lampion_roze[hanging=true]", "guhs:lampion_geel[hanging=true]", "guhs:lampion_mint[hanging=true]"]
DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


class Plek(SK.Lokaal):
    def __init__(self, x1, y1, z1, x2, y2, z2):
        super().__init__(int(x2 - x1), int(y2 - y1), int(z2 - z1), anker=(int(x1), int(y1), int(z1)))
        self.q = []          # extra-quads (spelcoördinaten)
        self.lab = []        # labels
        self.dl = []         # deeltjes
        self.ankers = []     # (x, z) bezette kolommen (voor begroeiing)

    # ---- blokken in spelcoördinaten
    def s(self, x, y, z, b, keep=False):
        ax, ay, az = self.anker
        self.set(int(math.floor(x)) - ax, int(math.floor(y)) - ay, int(math.floor(z)) - az, b, keep=keep)

    def g(self, x, y, z):
        ax, ay, az = self.anker
        return self.get(int(x) - ax, int(y) - ay, int(z) - az)

    def f(self, x1, y1, z1, x2, y2, z2, b, keep=False):
        ax, ay, az = self.anker
        X, Y, Z = self.v.shape
        a, b_ = sorted((int(x1) - ax, int(x2) - ax))
        c, d = sorted((int(y1) - ay, int(y2) - ay))
        e, f = sorted((int(z1) - az, int(z2) - az))
        a, c, e = max(a, 0), max(c, 0), max(e, 0)
        b_, d, f = min(b_, X - 1), min(d, Y - 1), min(f, Z - 1)
        if a > b_ or c > d or e > f:
            return
        i = self.id(b)
        sl = self.v[a:b_ + 1, c:d + 1, e:f + 1]
        if keep:
            sl[sl == 0] = i
        else:
            sl[...] = i

    def guhje(self, x, y, z, yaw=180, skin="guh", zit=False):
        ax, ay, az = self.anker
        self.guh(x - ax, y - ay, z - az, yaw, skin=skin, sitting=zit)

    def zet(self, W):
        SK.stempel(W, self, *self.anker)
        if W.k >= 0.5 and self.q:
            W.extra += SK.quads_naar_voxel(W, self.q)
        W.labels += self.lab
        if W.k >= 1:
            W.deeltjes += self.dl


# ============================================================================================= kleine hulpjes
def trap(b, facing, half="bottom"):
    return f"guhs:{b}_trap[facing={facing},half={half},shape=straight]" if ":" not in b else \
        f"{b}[facing={facing},half={half},shape=straight]"


def palen(P, x, z, y_top, blok="minecraft:stripped_cherry_log[axis=y]"):
    """Paal van y_top omlaag tot de bodem (zeebodem of maaiveld)."""
    yb, _ = KU.hoogte(x, z)
    for y in range(min(yb, y_top), y_top + 1):
        P.s(x, y, z, blok)


def oortjes_dak(P, x, y, z, as_="x", kleur="guhs:knuffelblok_roze", binnen="minecraft:pink_wool", h=3, gap=2):
    """Twee kleine guh-oortjes (2 breed, h hoog) bovenop een nok; as_ = de richting waarlangs ze naast elkaar staan."""
    for sgn in (-1, 1):
        for i in range(h):
            for j in (0, 1):
                if i == h - 1 and j == (1 if sgn > 0 else 0):
                    continue
                o = sgn * (gap // 2 + j) + (0 if sgn > 0 else 0)
                b = binnen if (i == 1 and j == (0 if sgn > 0 else 1)) else kleur
                if as_ == "x":
                    P.s(x + o, y + i, z, b)
                else:
                    P.s(x, y + i, z + o, b)


def touw(P, p0, p1, zak=1.5, n=10, kleur=(250, 236, 240), w=0.06):
    """Doorhangend touw (extra-quads) van p0 naar p1; geeft de punten terug."""
    pts = []
    for i in range(n + 1):
        t = i / n
        pts.append((p0[0] + (p1[0] - p0[0]) * t, p0[1] + (p1[1] - p0[1]) * t - zak * 4 * t * (1 - t),
                    p0[2] + (p1[2] - p0[2]) * t))
    for a, b in zip(pts, pts[1:]):
        P.q += MK.balk_quads(a, b, w, kleur)
    return pts


def lampionsnoer(P, p0, p1, zak=1.6, stap=2.2, seed=0, slinger=False):
    """Lampionsnoer: touw + hangende lampions (blokken) om de ~2 blokken; slinger=True: ook vlaggetjes-quads."""
    L = math.dist(p0, p1)
    n = max(2, int(L / stap))
    pts = touw(P, p0, p1, zak, n=max(6, n * 2))
    touw_blokken(P, p0, p1, zak, "guhs:vlaggetjes" if slinger else "guhs:vadstouw")
    for i in range(1, n):
        t = i / n
        x = p0[0] + (p1[0] - p0[0]) * t
        z = p0[2] + (p1[2] - p0[2]) * t
        y = p0[1] + (p1[1] - p0[1]) * t - zak * 4 * t * (1 - t)
        P.s(x, y - 0.6, z, LAMPIONS[(i + seed) % 3])
    if slinger:
        kl = [(255, 120, 170), (255, 236, 140), (140, 230, 210), (190, 160, 250), (255, 255, 255)]
        for j, (a, b) in enumerate(zip(pts, pts[1:])):
            vlagje(P, a, b, kl[(j + seed) % len(kl)])


def touw_blokken(P, p0, p1, zak, blok):
    """[zone 4 kaart] the rope as real blocks: one block per step along the main axis, at the sagging curve (never over a
    lampion); vlaggetjes = a garland with little flags, vadstouw = a plain rope (chain)."""
    dx, dz = p1[0] - p0[0], p1[2] - p0[2]
    as_ = "x" if abs(dx) >= abs(dz) else "z"
    n = int(max(abs(dx), abs(dz))) + 1
    for i in range(n + 1):
        t = i / max(1, n)
        x = p0[0] + dx * t
        z = p0[2] + dz * t
        y = p0[1] + (p1[1] - p0[1]) * t - zak * 4 * t * (1 - t)
        P.s(x, y, z, f"{blok}[axis={as_}]", keep=True)


def vlagje(P, a, b, kleur):
    """Driehoekig vlaggetje dat aan een touwstuk hangt (twee kanten)."""
    a, b = np.asarray(a, float), np.asarray(b, float)
    m = (a + b) / 2 + np.array([0, -0.55, 0])
    tex = np.zeros((8, 8, 4), np.float32)
    for yy in range(8):
        for xx in range(8):
            if abs(xx - 3.5) <= 4 - yy * 0.5:
                tex[yy, xx, :3] = np.array(kleur) / 255.0
                tex[yy, xx, 3] = 1
    cu = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
    o = a
    u = b - a
    v = m - (a + b) / 2
    for P_ in (np.array([o, o + u, o + 2 * v]), np.array([o + u, o, o + u + 2 * v])):
        P.q.append((P_, cu, tex, np.ones(3), True))


def vlak_quad(o, u, v, tex, doorzichtig=True):
    """Dubbelzijdig vlak met textuur (RGBA float array). doorzichtig=False: uitknippen op alpha > 0.5 (schrijft
    diepte, zodat nevel en randvervaging er goed mee omgaan)."""
    o, u, v = (np.asarray(a, float) for a in (o, u, v))
    cu = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
    return [(np.array([o, o + u, o + v]), cu, tex, np.ones(3), doorzichtig),
            (np.array([o + u, o, o + u + v]), np.array([(1, 0), (0, 0), (0, 1), (1, 1)], float), tex, np.ones(3),
             doorzichtig)]


# ============================================================================================= huisjes
HUIZEN = []   # [zone 4 kaart] every house built (for POIs, interiors and the walkability checks): dicts, see huis()


def huis(P, x, y, z, w, d, h, facing="south", muur="pastelbaksteen_roze", dak="guh_dakpan_roze", oortjes=False,
         op_palen=False, balk="minecraft:stripped_cherry_log", luifel=None, bord=None, verdiep=1, naam=None):
    """Pastelhuisje. (x, z) = hoek met de kleinste coördinaten, y = vloer. w langs x, d langs z, h = muurhoogte.
    Zadeldak met de nok evenwijdig aan de voorgevel; op_palen = op palen boven water/duin (vloer + palen)."""
    x2, z2 = x + w - 1, z + d - 1
    m = "guhs:" + muur
    kz = KOZIJN.get(muur, "wit")
    # vloer + fundering / palen
    P.f(x, y - 1, z, x2, y - 1, z2, "guhs:roze_lakhout_planken")
    if op_palen:
        for px in sorted({x, x2, x + w // 2}):
            for pz in sorted({z, z2}):
                palen(P, px, pz, y - 2)
    else:
        for px in range(x, x2 + 1):
            for pz in range(z, z2 + 1):
                yb, _ = KU.hoogte(px, pz)
                if yb < y - 1:
                    P.f(px, max(yb, y - 8), pz, px, y - 2, pz, "minecraft:pink_terracotta")
    # muren met hoekbalken
    for yy in range(y, y + h):
        for px in range(x, x2 + 1):
            for pz in (z, z2):
                P.s(px, yy, pz, m)
        for pz in range(z, z2 + 1):
            for px in (x, x2):
                P.s(px, yy, pz, m)
    for (px, pz) in ((x, z), (x2, z), (x, z2), (x2, z2)):
        P.f(px, y, pz, px, y + h - 1, pz, balk + "[axis=y]")
    P.f(x + 1, y, z + 1, x2 - 1, y + h - 1, z2 - 1, "minecraft:air")
    # ramen (guh-kozijnen) op elke verdieping, deur aan de voorkant
    fx, fz = DIRS[facing]
    for yy in range(y + 1, y + h - 1, 3):
        for px in range(x + 2, x2 - 1, 3):
            for pz, fac in ((z, "north"), (z2, "south")):
                P.s(px, yy, pz, f"guhs:guh_kozijn_{kz}[decor=kaal,facing={fac}]")
        for pz in range(z + 2, z2 - 1, 3):
            for px, fac in ((x, "west"), (x2, "east")):
                P.s(px, yy, pz, f"guhs:guh_kozijn_{kz}[decor=kaal,facing={fac}]")
    if facing in ("south", "north"):
        dx_, dz_ = x + w // 2, (z2 if facing == "south" else z)
    else:
        dx_, dz_ = (x2 if facing == "east" else x), z + d // 2
    P.s(dx_, y, dz_, f"guhs:roze_lakhout_deur[facing={facing},half=lower,hinge=left,open=false]")
    P.s(dx_, y + 1, dz_, f"guhs:roze_lakhout_deur[facing={facing},half=upper,hinge=left,open=false]")
    if luifel:   # gestreepte luifel boven de deur/etalage
        for i in range(-2, 3):
            ox, oz = (dx_ + i, dz_ + fz) if facing in ("south", "north") else (dx_ + fx, dz_ + i)
            P.s(ox, y + 2, oz, ("minecraft:white_wool" if i % 2 else luifel))
    # zadeldak; nok evenwijdig aan de lange kant
    langs_x = w >= d
    dakb = "guhs:" + dak
    yt = y + h
    if langs_x:
        n = (d + 1) // 2
        for i in range(n + 1):
            for px in range(x - 1, x2 + 2):
                if z - 1 + i <= z2 + 1 - i:
                    P.s(px, yt + i, z - 1 + i, trap(dakb, "south"))
                    P.s(px, yt + i, z2 + 1 - i, trap(dakb, "north"))
            # gevel
            for pz in range(z + i, z2 - i + 1):
                for px in (x, x2):
                    P.s(px, yt + i, pz, m)
        if d % 2 == 1:
            for px in range(x - 1, x2 + 2):
                P.s(px, yt + n, z + d // 2, dakb + "_plaat[type=bottom]")
        nok = (yt + n, z + d // 2)
        if oortjes:
            oortjes_dak(P, x + w // 2, nok[0] + (1 if d % 2 else 0), nok[1], "x", gap=max(2, w // 3))
    else:
        n = (w + 1) // 2
        for i in range(n + 1):
            for pz in range(z - 1, z2 + 2):
                if x - 1 + i <= x2 + 1 - i:
                    P.s(x - 1 + i, yt + i, pz, trap(dakb, "east"))
                    P.s(x2 + 1 - i, yt + i, pz, trap(dakb, "west"))
            for px in range(x + i, x2 - i + 1):
                for pz in (z, z2):
                    P.s(px, yt + i, pz, m)
        if w % 2 == 1:
            for pz in range(z - 1, z2 + 2):
                P.s(x + w // 2, yt + n, pz, dakb + "_plaat[type=bottom]")
        if oortjes:
            oortjes_dak(P, x + w // 2, yt + n + (1 if w % 2 else 0), z + d // 2, "z", gap=max(2, d // 3))
    # schoorsteentje
    P.s(x + 1, yt + 1, z + 1, "guhs:guh_schoorsteen[rookt=false]", keep=True)
    for px in range(x - 1, x2 + 2):
        for pz in range(z - 1, z2 + 2):
            P.ankers.append((px, pz))
    # [zone 4 kaart] register: box, door (lower door block), stand spot outside the door, facing, stilts
    HUIZEN.append(dict(naam=naam, x=x, y=y, z=z, w=w, d=d, h=h, facing=facing, deur=(dx_, y, dz_),
                       voor=(dx_ + fx, y, dz_ + fz), op_palen=op_palen, muur=muur))
    return (dx_ + fx, y, dz_ + fz)


# ============================================================================================= steigers
def steiger(P, x1, z1, x2, z2, y, hek=True, planken="minecraft:cherry_planks", paal=4, lampen=True):
    """Houten steiger (dek op y) op palen, met een hekje langs de lange kanten en lantaarntjes."""
    P.f(x1, y, z1, x2, y, z2, planken)
    for px in range(x1, x2 + 1, paal):
        for pz in (z1, z2):
            palen(P, px, pz, y - 1)
    for pz in range(z1, z2 + 1, paal):
        for px in (x1, x2):
            palen(P, px, pz, y - 1)
    if hek:
        lang_x = (x2 - x1) >= (z2 - z1)
        if lang_x:
            for px in range(x1, x2 + 1):
                for pz in (z1, z2):
                    P.s(px, y + 1, pz, "minecraft:cherry_fence")
        else:
            for pz in range(z1, z2 + 1):
                for px in (x1, x2):
                    P.s(px, y + 1, pz, "minecraft:cherry_fence")
    if lampen:
        for (px, pz) in ((x2, z2), (x2, z1)):
            P.s(px, y + 2, pz, "minecraft:cherry_fence")
            P.s(px, y + 3, pz, LAMPIONS[(px + pz) % 3].replace("true", "false"))


# ============================================================================================= vuurtoren
def vuurtoren(P, x, y, z, h=26, r=4.5, oortjes=True, label=None):
    """Roze-witte vuurtoren met galerij, lichtkamer (vuurtorenlamp) en guh-oortjes op de kap."""
    for yy in range(y, y + h):
        rr = r - (yy - y) / h * 1.3
        b = "guhs:pastelbaksteen_roze" if ((yy - y) // 4) % 2 == 0 else "guhs:pastelbaksteen_wolkje"
        for dx in range(-int(r) - 1, int(r) + 2):
            for dz in range(-int(r) - 1, int(r) + 2):
                dd = math.hypot(dx, dz)
                if dd <= rr and dd > rr - 1.4:
                    P.s(x + dx, yy, z + dz, b)
    for dx, dz in ((0, int(r) - 1), (0, -int(r) + 1)):   # raampjes
        for yy in range(y + 5, y + h - 3, 6):
            P.s(x + dx, yy, z + dz, "minecraft:pink_stained_glass")
    yt = y + h
    rg = int(r)
    for dx in range(-rg - 1, rg + 2):        # galerij
        for dz in range(-rg - 1, rg + 2):
            dd = math.hypot(dx, dz)
            if dd <= rg + 1.2:
                P.s(x + dx, yt, z + dz, "guhs:pastelbaksteen_wolkje_plaat[type=bottom]" if dd > rg - 2 else
                    "guhs:pastelbaksteen_wolkje")
                if dd > rg + 0.2:
                    P.s(x + dx, yt + 1, z + dz, "guhs:guh_hekje_wit")
    for dx in range(-2, 3):                  # lichtkamer
        for dz in range(-2, 3):
            if abs(dx) == 2 or abs(dz) == 2:
                P.f(x + dx, yt + 1, z + dz, x + dx, yt + 3, z + dz, "minecraft:glass")
    P.f(x - 1, yt + 1, z - 1, x + 1, yt + 3, z + 1, "guhs:torenpeper_vuurtorenlamp[lit=true]")
    for i in range(4):                       # kap
        for dx in range(-3 + i, 4 - i):
            for dz in range(-3 + i, 4 - i):
                P.s(x + dx, yt + 4 + i, z + dz, "guhs:guh_dakpan_roze")
    if oortjes:
        for sgn in (-1, 1):
            for i in range(4):
                for j in range(2):
                    if i == 3 and j == 1:
                        continue
                    P.s(x + sgn * (2 + j) if sgn > 0 else x - 2 - j, yt + 6 + i, z,
                        "minecraft:pink_wool" if (i in (1, 2) and j == 0) else "guhs:knuffelblok_roze")
    for i in range(5):                       # lichtbundel (zacht, als deeltjes)
        P.dl.append((x + 0.5 + 3 + i * 2.5, yt + 2.5, z + 0.5, (255, 240, 190), 1.6 - i * 0.2, 1.0, "stip"))
    if label:
        P.lab.append((x, yt + 12, z, label))


# ============================================================================================= bootjes
def bootje(P, x, y, z, richting="east", romp="minecraft:cherry_planks", zeil=True, zeilkleur="minecraft:pink_wool",
           L=7, gezicht=True):
    """Klein guh-zeilbootje. (x, y, z) = midden van de boeg-achtersteven-as op waterhoogte (y = ZEE + 1).
    richting = waar de boeg naartoe wijst."""
    fx, fz = DIRS[richting]
    rx, rz = -fz, fx
    hw = [0, 1, 1, 1, 1, 1, 0]
    if L != 7:
        hw = [0] + [1] * (L - 2) + [0]
    for i in range(L):
        t = i - L // 2
        cx, cz = x + fx * t, z + fz * t
        b = hw[i]
        for j in range(-b, b + 1):
            P.s(cx + rx * j, y, cz + rz * j, romp)
            if abs(j) == b:
                P.s(cx + rx * j, y + 1, cz + rz * j, romp)
        P.s(cx, y, cz, "guhs:roze_lakhout_planken")
    # boeg: guh-gezichtje
    bx, bz = x + fx * (L // 2), z + fz * (L // 2)
    P.s(bx, y + 1, bz, f"guhs:wolkjeshout_gezicht[facing={richting}]" if gezicht else romp)
    if zeil:
        mx, mz = x - fx, z - fz
        for yy in range(y + 1, y + 9):
            P.s(mx, yy, mz, "minecraft:cherry_fence")
        # zeil (wol) naast de mast, met oortjes erop
        for i in range(6):
            for j in range(1, 5 - i // 2):
                P.s(mx + fx * j, y + 2 + i, mz + fz * j, zeilkleur if (i + j) % 5 else "minecraft:white_wool")
        P.s(mx + fx * 1, y + 8, mz + fz * 1, "guhs:knuffelblok_roze")
        P.s(mx + fx * 2, y + 8, mz + fz * 2, "guhs:knuffelblok_roze")


def zeilboot_groot(P, x, y, z, richting="east", kleur="minecraft:pink_wool", L=11):
    """Iets groter vissersbootje met kajuitje (voor de steigers)."""
    fx, fz = DIRS[richting]
    rx, rz = -fz, fx
    for i in range(L):
        t = i - L // 2
        b = 2 if 1 < i < L - 2 else 1
        if i == L - 1:
            b = 0
        cx, cz = x + fx * t, z + fz * t
        for j in range(-b, b + 1):
            P.s(cx + rx * j, y, cz + rz * j, "minecraft:cherry_planks")
            if abs(j) == b:
                P.s(cx + rx * j, y + 1, cz + rz * j, "guhs:pastelbaksteen_wolkje_plaat[type=bottom]")
    kx, kz = x - fx * 2, z - fz * 2
    for j in range(-1, 2):
        for k in range(0, 3):
            P.f(kx + rx * j - fx * k, y + 1, kz + rz * j - fz * k, kx + rx * j - fx * k, y + 2, kz + rz * j - fz * k,
                "guhs:pastelbaksteen_aqua")
            P.s(kx + rx * j - fx * k, y + 3, kz + rz * j - fz * k, "guhs:guh_dakpan_roze_plaat[type=bottom]")
    P.s(x + fx * (L // 2), y + 1, z + fz * (L // 2), f"guhs:wolkjeshout_gezicht[facing={richting}]")
    mx, mz = x + fx, z + fz
    for yy in range(y + 1, y + 11):
        P.s(mx, yy, mz, "minecraft:cherry_fence")
    for i in range(7):
        for j in range(1, 6 - i // 2):
            P.s(mx - fx * j, y + 3 + i, mz - fz * j, kleur if (i + j) % 4 else "minecraft:white_wool")


# ============================================================================================= vliegers
def _vlieger_tex(kleur, kleur2=(255, 255, 255), gezicht=True):
    n = 32
    tex = np.zeros((n, n, 4), np.float32)
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    ruit = (np.abs(xx - n / 2) / (n / 2) + np.abs(yy - n / 2) / (n / 2)) <= 1
    tex[ruit, :3] = np.array(kleur) / 255.0
    tex[ruit, 3] = 1
    band = ruit & ((np.abs(xx - n / 2) < 1.2) | (np.abs(yy - n / 2) < 1.2))
    tex[band, :3] = np.array(kleur2) / 255.0
    if gezicht:
        for ox in (-5, 5):
            oog = (xx - (n / 2 + ox)) ** 2 + (yy - (n / 2 - 1)) ** 2 < 6
            tex[oog, :3] = (0.12, 0.08, 0.12)
            gl = (xx - (n / 2 + ox - 0.8)) ** 2 + (yy - (n / 2 - 2)) ** 2 < 1.2
            tex[gl, :3] = 1.0
            blos = (xx - (n / 2 + ox * 1.5)) ** 2 + (yy - (n / 2 + 3)) ** 2 < 4
            tex[blos & ruit, :3] = (1.0, 0.55, 0.7)
        mond = (np.abs(xx - n / 2) < 2.2) & (np.abs(yy - (n / 2 + 4)) < 0.8)
        tex[mond, :3] = (0.12, 0.08, 0.12)
    return tex


def vlieger(P, anker, pos, grootte=3.0, kleur=(255, 130, 180), kleur2=(255, 255, 255), richting=(1, 0, 0),
            staart=True, oortjes=True, seed=0):
    """Guh-vlieger als quads: ruit met gezichtje, twee oortjes, een lijn naar het anker en een staart met strikjes.
    richting = de windrichting (de vlieger 'kijkt' tegen de wind in)."""
    pos = np.asarray(pos, float)
    wr = np.asarray(richting, float)
    wr /= np.linalg.norm(wr)
    if anker is not None:
        vlieger_blokken(P, anker, pos, grootte, kleur, kleur2, wr)
    u = np.cross([0, 1, 0], wr)
    u = u / (np.linalg.norm(u) + 1e-9) * grootte
    v = np.array([0, -1.0, 0]) * grootte * 1.15 + wr * grootte * 0.35
    o = pos - u / 2 - v / 2
    P.q += vlak_quad(o, u, v, _vlieger_tex(kleur, kleur2), doorzichtig=False)
    if oortjes:
        for s in (0.28, 0.52):
            tex = np.zeros((8, 8, 4), np.float32)
            yy, xx = np.mgrid[0:8, 0:8] + 0.5
            m = ((xx - 4) / 4) ** 2 + ((yy - 5) / 5) ** 2 <= 1
            tex[m, :3] = np.array(kleur) / 255.0
            tex[m, 3] = 1
            tex[m & (((xx - 4) / 2.2) ** 2 + ((yy - 5.5) / 3.2) ** 2 <= 1), :3] = (1.0, 0.75, 0.82)
            vn = v / np.linalg.norm(v)
            P.q += vlak_quad(o + u * s - vn * grootte * 0.18, u * 0.2, vn * grootte * 0.45, tex, doorzichtig=False)
    # lijn naar het anker
    if anker is not None:
        touw(P, tuple(pos + v * 0.5), tuple(anker), zak=0.6 + 0.02 * math.dist(pos, anker), n=12,
             kleur=(240, 240, 240), w=0.05)
    if staart:
        rng = np.random.default_rng(seed)
        p = pos + v * 0.5
        kl = [(255, 255, 255), tuple(kleur), (255, 236, 140)]
        for i in range(8):
            q = p + v / np.linalg.norm(v) * 0.9 + wr * -0.4 + np.array([0, 0, 0]) + u / np.linalg.norm(u) * \
                math.sin(i * 1.3 + seed) * 0.35
            P.q += MK.balk_quads(tuple(p), tuple(q), 0.06, (250, 240, 245))
            P.q += MK.balk_quads(tuple(q - u / np.linalg.norm(u) * 0.22), tuple(q + u / np.linalg.norm(u) * 0.22),
                                 0.16, kl[i % 3])
            p = q


VLIEGERS = []   # [zone 4 kaart] every flying kite: (anchor, kite centre, size, colour) for code (entities / the race)
# [T-Z4DIEREN] the kites are entities now (zone4/dieren GuhVlieger, positions via dieren4.py -> Z4DierenPlekken): the map only
# records them. Fallback to the old static wool kites + rope: environment GUHS_Z4_VLIEGERBLOKKEN=1 when running maak_kaart.
import os as _os
VLIEGER_BLOKKEN = _os.environ.get("GUHS_Z4_VLIEGERBLOKKEN", "") == "1"
_WOL = [((255, 120, 170), "pink"), ((140, 220, 250), "light_blue"), ((255, 230, 120), "yellow"), ((180, 150, 250), "purple"),
        ((130, 230, 170), "lime"), ((255, 170, 120), "orange"), ((255, 255, 255), "white"), ((250, 90, 140), "magenta")]


def _wol(kleur):
    return "minecraft:%s_wool" % min(_WOL, key=lambda w: sum((a - b) ** 2 for a, b in zip(w[0], kleur)))[1]


def vlieger_blokken(P, anker, pos, grootte, kleur, kleur2, wr):
    """[zone 4 kaart] a kite as blocks (the static decor version; code may replace it by entities): a wool diamond facing the
    wind with a white cross, two ear blocks, two eyes, and a rope (vadstouw) down to the guh holding it."""
    VLIEGERS.append((tuple(round(v, 1) for v in anker), tuple(round(float(v), 1) for v in pos), round(float(grootte), 1),
                     tuple(int(c) for c in kleur), tuple(int(c) for c in kleur2)))
    zet = P.s
    if not VLIEGER_BLOKKEN:
        # [T-Z4DIEREN] an entity flies here (see VLIEGERS): no blocks, but the columns stay "taken" exactly as before, so the
        # vegetation (trees, plants) around the kites is the same as with the block kites
        def zet(x, y, z, b, keep=False):
            ax, ay, az = P.anker
            X, Y, Z = P.v.shape
            i, j, k = int(math.floor(x)) - ax, int(math.floor(y)) - ay, int(math.floor(z)) - az
            if 0 <= i < X and 0 <= j < Y and 0 <= k < Z and math.floor(y) >= ZEE + 2:
                P.ankers.append((int(math.floor(x)), int(math.floor(z))))
    u = np.cross([0, 1, 0], wr)
    u = u / (np.linalg.norm(u) + 1e-9)
    hw = max(1, int(round(grootte / 2)))
    hv = max(1, int(round(grootte * 0.6)))
    w1, w2 = _wol(kleur), _wol(kleur2)
    for a in range(-hw, hw + 1):
        for b in range(-hv, hv + 1):
            if abs(a) / (hw + 0.5) + abs(b) / (hv + 0.5) > 1:
                continue
            q = pos + u * a + np.array([0, b, 0])
            blok = w2 if (a == 0 or b == 0) else w1
            if b == 1 and abs(a) == max(1, hw // 2):
                blok = "minecraft:black_wool"
            zet(q[0], q[1], q[2], blok)
    for sgn in (-1, 1):
        q = pos + u * sgn * max(1, hw // 2) + np.array([0, hv, 0])
        zet(q[0], q[1], q[2], "guhs:knuffelblok_roze")
    # the line: straight from the bottom of the kite to the anchor, one block per step along the longest axis
    a = np.asarray(anker, float) + np.array([0, 1.0, 0])
    b = pos + np.array([0, -hv - 1, 0])
    n = int(max(abs(b - a))) + 1
    for i in range(n + 1):
        q = a + (b - a) * i / n
        zet(q[0], q[1], q[2], "guhs:vadstouw[axis=y]", keep=True)


# ============================================================================================= feestspul
def parasol(P, x, y, z, kleur="minecraft:pink_wool", r=2):
    """Grote strandparasol (paal + gestreept wollen scherm)."""
    for yy in range(y, y + 4):
        P.s(x, yy, z, "minecraft:cherry_fence")
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if abs(dx) + abs(dz) <= r + 1:
                b = kleur if (abs(dx) + abs(dz)) % 2 == 0 else "minecraft:white_wool"
                P.s(x + dx, y + 4 - (1 if abs(dx) + abs(dz) > r else 0) + 1 * 0, z + dz, b.replace("wool", "carpet")
                    if abs(dx) + abs(dz) > r else b)
    P.s(x, y + 5, z, "guhs:knuffelblok_roze")


def kampvuur(P, x, y, z, zitjes=True):
    P.s(x, y, z, "minecraft:campfire[facing=north,lit=true]")
    for i in range(6):
        P.dl.append((x + 0.5 + math.sin(i) * 0.4, y + 1.4 + i * 0.7, z + 0.5 + math.cos(i * 1.7) * 0.4,
                     (255, 190, 90), 0.3, 1.2, "stip"))
    if zitjes:
        for (dx, dz, ax) in ((-3, 0, "z"), (3, 0, "z"), (0, -3, "x"), (0, 3, "x")):
            for j in (-1, 0, 1):
                P.s(x + dx + (j if ax == "x" else 0), y, z + dz + (j if ax == "z" else 0),
                    f"minecraft:stripped_cherry_log[axis={ax}]")


def lantaarnpaal(P, x, y, z, kleur=0):
    P.s(x, y, z, "minecraft:cherry_fence")
    P.s(x, y + 1, z, "minecraft:cherry_fence")
    P.s(x, y + 2, z, LAMPIONS[kleur % 3].replace("true", "false"))


# ============================================================================================= het guh-zeilbootje
def _zeil_tex(n=64, kleur=(255, 140, 186), rand=(255, 255, 255)):
    """Zeil met guhgezicht en twee oortjes bovenaan (alpha buiten de vorm)."""
    tex = np.zeros((n, n, 4), np.float32)
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    u, v = xx / n, yy / n          # u: van de mast (0) naar achteren (1); v: van boven (0) naar onder (1)
    bol = 0.08 * np.sin(np.pi * v)
    vorm = (v > 0.16) & (u < 0.94 - 0.25 * (1 - v) + bol) & (v < 0.97)
    for oc in (0.22, 0.62):        # oortjes
        oor = ((u - oc) / 0.13) ** 2 + ((v - 0.17) / 0.15) ** 2 <= 1
        vorm |= oor & (v < 0.2)
        binnen = ((u - oc) / 0.07) ** 2 + ((v - 0.15) / 0.09) ** 2 <= 1
        tex[binnen & (v < 0.2), :3] = (1.0, 0.78, 0.86)
    tex[vorm & ~(tex[..., 0] > 0), :3] = np.array(kleur) / 255.0
    randm = vorm & ((v > 0.93) | (u > 0.9 - 0.25 * (1 - v) + bol))
    tex[randm, :3] = np.array(rand) / 255.0
    for oc in (0.3, 0.56):          # ogen met glimmer
        oog = ((u - oc) / 0.06) ** 2 + ((v - 0.47) / 0.08) ** 2 <= 1
        tex[oog, :3] = (0.1, 0.07, 0.12)
        gl = ((u - oc + 0.02) / 0.022) ** 2 + ((v - 0.44) / 0.028) ** 2 <= 1
        tex[gl, :3] = 1.0
        blos = ((u - oc - 0.0) / 0.07) ** 2 + ((v - 0.6) / 0.04) ** 2 <= 1
        tex[blos & vorm, :3] = (1.0, 0.5, 0.66)
    mond = (np.abs(u - 0.43) < 0.05) & (np.abs(v - 0.6 - 0.03 * np.cos((u - 0.43) * 30)) < 0.012)
    tex[mond, :3] = (0.1, 0.07, 0.12)
    tex[vorm, 3] = 1.0
    return tex


def guh_zeilbootje(P, x, z, richting="east", romp="minecraft:cherry_planks", speler=True, matroos=True,
                   zeilkleur=(255, 140, 186), kielzog=True):
    """Het eigen guh-zeilbootje van de speler (mount op water): 7 lang, 3 breed, guhgezichtje op de boeg,
    stoffen zeil met oortjes en gezicht (quads), roer, de speler zittend achterin, een guh-matroos voorop."""
    fx, fz = DIRS[richting]
    rx, rz = -fz, fx
    y = ZEE
    for i in range(-3, 4):
        cx, cz = x + fx * i, z + fz * i
        b = 0 if abs(i) == 3 else 1
        for j in range(-b, b + 1):
            P.s(cx + rx * j, y, cz + rz * j, romp)
            if abs(j) == b:
                P.s(cx + rx * j, y + 1, cz + rz * j, romp)
            else:
                P.s(cx + rx * j, y + 1, cz + rz * j, "minecraft:air")
        P.s(cx, y, cz, "guhs:roze_lakhout_planken")
    P.s(x + fx * 3, y + 1, z + fz * 3, f"guhs:wolkjeshout_gezicht[facing={richting}]")
    P.s(x - fx * 3, y + 1, z - fz * 3, "guhs:roze_lakhout_planken")
    # mast + giek + zeil
    mx, mz = x + fx * 1 + 0.5, z + fz * 1 + 0.5
    top = y + 1 + 8.5
    P.q += MK.balk_quads((mx, y + 1, mz), (mx, top, mz), 0.22, (214, 160, 140))
    giek = (mx - fx * 4.2, y + 2.3, mz - fz * 4.2 + 0.6)
    P.q += MK.balk_quads((mx, y + 2.3, mz), giek, 0.16, (214, 160, 140))
    o = np.array([mx, top + 0.3, mz])
    u = np.array([-fx * 4.4, 0, -fz * 4.4 + 0.5])
    v = np.array([0, -(top + 0.3 - (y + 2.4)), 0])
    P.q += vlak_quad(o, u, v, _zeil_tex(kleur=zeilkleur), doorzichtig=False)
    P.q += MK.balk_quads((mx, top, mz), (mx + fx * 3.4, y + 1.6, mz + fz * 3.4), 0.04, (240, 240, 240))  # fok-lijn
    # wimpeltje
    P.q += MK.balk_quads((mx, top, mz), (mx - fx * 1.4, top + 0.2, mz - fz * 1.4 + 0.3), 0.18, (255, 236, 140))
    if speler:
        q = speler_zittend((x - fx * 2 + 0.5, y + 1.0, z - fz * 2 + 0.5), {"east": 270, "west": 90, "north": 0,
                                                                         "south": 180}[richting])
        P.q += q
        # roerpen
        P.q += MK.balk_quads((x - fx * 3 + 0.5, y + 1.6, z - fz * 3 + 0.5), (x - fx * 2.2 + 0.5, y + 1.75,
                                                                              z - fz * 2.2 + 0.9), 0.1, (190, 130, 110))
    if matroos:
        P.guhje(x + fx * 2 + 0.5, y + 1, z + fz * 2 + 0.5, {"east": 270, "west": 90, "north": 0, "south": 180}[richting],
                "guh_pinguh")
    if kielzog:
        for i in range(18):
            t = i + 1
            for sgn in (-1, 1):
                wx = x - fx * (3.5 + t * 1.3) + 0.5 + rx * sgn * (1.2 + t * 0.45)
                wz = z - fz * (3.5 + t * 1.3) + 0.5 + rz * sgn * (1.2 + t * 0.45)
                a = max(0.0, 0.85 - t * 0.045)
                P.q += vlak_quad((wx - 0.7, ZEE + 1.04, wz - 0.25), (1.4, 0, 0), (0, 0, 0.5 + 0.02 * t),
                                 _schuim_tex(a))
        for i in range(10):
            P.s(x - fx * (4 + i) + rx * ((i % 3) - 1), ZEE + 1, z - fz * (4 + i) + rz * ((i % 3) - 1),
                "minecraft:white_carpet")


def _schuim_tex(a):
    tex = np.zeros((8, 8, 4), np.float32)
    yy, xx = np.mgrid[0:8, 0:8] + 0.5
    m = ((xx - 4) / 4) ** 2 + ((yy - 4) / 4) ** 2 <= 1
    tex[m, :3] = 1.0
    tex[m, 3] = a
    return tex


_SPELER = None


def speler_zittend(pos, yaw):
    """De speler (mountkit-model) zittend, handen naar voren (roer/schoot)."""
    global _SPELER
    import os
    if _SPELER is None:
        _SPELER = MK.laad(os.path.join(MK.MODELS, "speler.geo.json"), os.path.join(MK.MODELS, "speler.png"))
    pose = {"leg_r": {"rot": [-82, -6, 4]}, "leg_l": {"rot": [-82, 6, -4]},
            "arm_r": {"rot": [-50, 0, 6]}, "arm_l": {"rot": [-40, 0, -6]}, "head": {"rot": [-6, -14, 0]}}
    base = MK.base_mat((pos[0], pos[1] - 0.66, pos[2]), yaw, MK.SPELER_SCHAAL)
    return MK.quads(_SPELER, base, pose)
