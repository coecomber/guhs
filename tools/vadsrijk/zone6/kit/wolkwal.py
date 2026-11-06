"""Moeder Wolkwal: het lijf van de luchtwalvis (wereldcoördinaten; kop naar +x/oost, rug omhoog).

Vorm in één plek (profiel), zodat stad.py de rug kan opvragen:
  rug(x, z)  -> wereld-y van de bovenkant van de huid (nan buiten het lijf)
  breedte(x) -> halve breedte a(x);  as_y(x) -> y van de lichaamsas
Bouwfuncties: lijf, kop (ogen, blos, glimlach, oortjes, spuitgaten), vinnen, staart, spuitwolken.
"""
import math
import numpy as np
import palet
from palet import P

X_STAART, X_NEUS = -250.0, 262.0
L = X_NEUS - X_STAART
POW = 2.4
A_MAX, BT_MAX, BB_MAX = 120.0, 44.0, 48.0
AS_Y = 196.0  # lichaamsas; buik ~154, rug ~246 (spel-y, past in de 0..320 van het Vadsrijk)


def _u(x):
    return (np.asarray(x, float) - X_STAART) / L


def breedte(x):
    u = _u(x)
    voor = 16 + (A_MAX - 16) * np.sin(np.pi / 2 * np.clip(u / 0.62, 0, 1)) ** 1.25
    kop = A_MAX * np.sqrt(np.clip(1 - np.clip((u - 0.62) / 0.38, 0, 1) ** 2.6, 0, 1))
    return np.where(u <= 0.62, voor, kop) * (u >= 0) * (u <= 1)


def hoogte_boven(x):
    u = _u(x)
    voor = 14 + (BT_MAX - 14) * np.sin(np.pi / 2 * np.clip(u / 0.6, 0, 1)) ** 1.1
    kop = (BT_MAX + 5 * np.exp(-((u - 0.78) / 0.08) ** 2)) * np.sqrt(
        np.clip(1 - np.clip((u - 0.66) / 0.34, 0, 1) ** 2.4, 0, 1))
    return np.where(u <= 0.6, voor, np.maximum(kop, 0)) * (u >= 0) * (u <= 1)


def hoogte_onder(x):
    u = _u(x)
    voor = 12 + (BB_MAX - 12) * np.sin(np.pi / 2 * np.clip(u / 0.58, 0, 1)) ** 1.2
    kop = BB_MAX * np.sqrt(np.clip(1 - np.clip((u - 0.64) / 0.36, 0, 1) ** 2.2, 0, 1))
    return np.where(u <= 0.58, voor, kop) * (u >= 0) * (u <= 1)


def as_y(x):
    u = _u(x)
    staart = 26 * np.clip((0.22 - u) / 0.22, 0, 1) ** 1.6   # staart omhoog (zwemslag)
    kop = -4 * np.clip((u - 0.8) / 0.2, 0, 1)
    return AS_Y + staart * 0.8 + kop


def doorsnede_s(x, z):
    a = breedte(x)
    with np.errstate(divide="ignore", invalid="ignore"):
        q = np.abs(np.asarray(z, float)) / np.maximum(a, 1e-6)
    s = np.clip(1 - q ** POW, 0, 1) ** (1 / POW)
    return np.where(q < 1, s, np.nan)


def rug(x, z):
    return as_y(x) + hoogte_boven(x) * doorsnede_s(x, z)


def buik(x, z):
    return as_y(x) - hoogte_onder(x) * doorsnede_s(x, z)


# ------------------------------------------------------------------ bouwen
def lijf(W):
    """Vult de huid per kolom. Bovenkant huid (met wolkpokjes), flank licht, buik crème."""
    X, Z = W.XX, W.ZZ
    s = doorsnede_s(X, Z)
    m = ~np.isnan(s)
    yc = as_y(X)
    top = np.where(m, yc + hoogte_boven(X) * np.nan_to_num(s), -1e9)
    bot = np.where(m, yc - hoogte_onder(X) * np.nan_to_num(s), 1e9)
    huid, licht, buik_, pok = W.id(P["huid"]), W.id(P["huid_licht"]), W.id(P["buik"]), W.id(P["huid_pok"])
    import schemerkit as SK
    h = SK.hash3(X, np.zeros_like(X), Z, 17)
    vlek = SK.vnoise(X, Z * 1.6, cell=22.0, seed=8, oct=2)

    def laag(y, mk):
        rel = (y - yc) / np.maximum(hoogte_boven(X), 1)
        out = np.full(X.shape, huid, np.uint16)
        out[rel < 0.12] = licht
        out[(rel < 0.30) & (rel >= 0.12) & (h > 0.5)] = licht
        # wolkvlekken op de flanken
        out[(rel > 0.15) & (rel < 0.75) & (vlek > 0.66)] = licht
        out[y < yc - 0.28 * hoogte_onder(X)] = buik_
        out[(rel > 0.2) & ((h > 0.985) | ((vlek > 0.72) & (h > 0.8)))] = pok
        return out
    W.kolom(bot, top, None, mask=m, laagfn=laag)


def _zijvlak_z(x, y, kant):
    """z van het huidoppervlak op (x, y) aan kant +1/-1 (nan als buiten)."""
    a = breedte(x)
    yc = as_y(x)
    b = hoogte_boven(x) if y >= yc else hoogte_onder(x)
    q = abs(y - yc) / max(b, 1e-6)
    if q >= 1:
        return float("nan")
    return kant * a * (1 - q ** POW) ** (1 / POW)


def schijf_op_huid(W, cx, cy, kant, r, kleurfn, diepte=2, bol=1.0):
    """Tekent een rond 'plaatje' op het zijvlak (oog, blos): kleurfn(dx, dy, d) -> blok of None."""
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy) / r
            if d > 1:
                continue
            b = kleurfn(dx / r, dy / r, d)
            if b is None:
                continue
            z = _zijvlak_z(x + 0.5, y + 0.5, kant)
            if math.isnan(z):
                continue
            uit = bol * (1 - d * d) ** 0.5
            for k in range(-diepte, int(round(uit)) + 1):
                W.wset(x, y, int(math.floor(z + kant * k)), b)


def oog(W, kant):
    cx = X_NEUS - 50
    cy = as_y(cx) + 2
    zwart, wit = "minecraft:black_concrete", "minecraft:white_concrete"
    iris, iris2 = "minecraft:light_blue_concrete", "minecraft:blue_concrete"

    def kl(u, v, d):
        if d > 0.86:
            return zwart
        if (u + 0.38) ** 2 + (v - 0.40) ** 2 < 0.05 or (u - 0.25) ** 2 + (v + 0.30) ** 2 < 0.012:
            return wit
        if d < 0.40:
            return zwart
        return iris2 if v < -0.25 else iris
    schijf_op_huid(W, cx, cy, kant, 11, kl, diepte=2, bol=2.5)
    # wimpers / ooglid
    for i, (dx, dy) in enumerate(((-9, 11), (-4, 13), (2, 13), (7, 11))):
        for k in range(3):
            x, y = cx + dx + (dx / 6.0) * k * 0.6, cy + dy + k
            z = _zijvlak_z(x, y, kant)
            if not math.isnan(z):
                W.wset(int(x), int(y), int(math.floor(z + kant * 0.5)), zwart)
    # blos
    bx, by = cx - 4, cy - 15

    def blos(u, v, d):
        return "minecraft:pink_concrete" if d < 0.75 else "minecraft:pink_wool"
    schijf_op_huid(W, bx, by, kant, 7.5, blos, diepte=1, bol=0.6)


def glimlach(W, kant):
    """Glimlach langs de kop: van de neus naar onder het oog, met een krul omhoog."""
    x0 = int(X_NEUS - 100)
    for x in range(x0, int(X_NEUS) + 1):
        t = (x - x0) / (X_NEUS - x0)
        y = as_y(x) - 18 + 10 * t ** 1.5 - 6 * math.sin(t * math.pi) * 0.6
        if x < x0 + 12:
            y += (x0 + 12 - x) * 0.55
        for dy in (0, 1):
            z = _zijvlak_z(x + 0.5, y + dy, kant)
            if not math.isnan(z):
                W.wset(x, int(y) + dy, int(math.floor(z)), "minecraft:purple_terracotta")
                W.wset(x, int(y) + dy, int(math.floor(z - kant)), "minecraft:purple_terracotta")
    # voorkant: dicht de mond aan de neus
    for z_ in range(-30, 31):
        x = int(X_NEUS - 2 - 0.004 * z_ * z_)
        y = as_y(X_NEUS) - 16 + 0.01 * z_ * z_
        W.wset(x, int(y), z_, "minecraft:purple_terracotta")


def oortjes(W):
    """Twee zachte guh-oortjes boven op de kop (buitenkant huid, binnenkant roze)."""
    for kant in (-1, 1):
        cx, cz = int(X_NEUS - 64), kant * 54
        y0 = rug(cx, cz) - 3
        for dy in range(0, 26):
            t = dy / 25.0
            hw = 13 * (1 - t ** 1.8) + 1.2
            dik = 4.5 * (1 - t) + 1.5
            for dx in range(-14, 15):
                for dz in range(-14, 15):
                    if abs(dz) > hw:
                        continue
                    # oortje kantelt licht naar buiten
                    zz = dz + kant * dy * 0.25
                    if abs(dx) > dik:
                        continue
                    blok = P["huid"]
                    if dx > dik - 1.6 and abs(dz) < hw - 2.5 and dy < 22:
                        blok = "minecraft:pink_wool"
                    W.wset(cx + dx, int(y0 + dy), int(cz + zz), blok)


def spuitgaten(W):
    """Twee spuitgaten met een opstaande rand vóór de stad."""
    for kant in (-1, 1):
        cx, cz = int(X_NEUS - 80), kant * 9
        y0 = int(rug(cx, cz))
        for dx in range(-7, 8):
            for dz in range(-5, 6):
                d = (dx / 6.5) ** 2 + (dz / 4.2) ** 2
                if d <= 1:
                    W.wset(cx + dx, y0 + 1, cz + dz, P["huid_licht"])
                    W.wset(cx + dx, y0 + 2, cz + dz, P["huid_licht"] if d > 0.45 else "minecraft:air")
                    if d < 0.45:
                        W.wset(cx + dx, y0 + 1, cz + dz, "minecraft:purple_concrete")
                        W.wset(cx + dx, y0, cz + dz, "minecraft:purple_concrete")


def spuitwolk(W, seed=3, hoogte=50):
    """Wolkenpluim uit de spuitgaten: twee kolommen die boven samenkomen en uitwaaieren."""
    rng = np.random.RandomState(seed)
    wolk = "guhs:wolkenblok_wit"
    xs_ = X_NEUS - 80
    y0 = rug(xs_, 0) + 2
    for kant in (-1, 1):
        for k in range(14):
            t = k / 13.0
            x = xs_ - 6 * t + rng.uniform(-1, 1)
            z = kant * (9 - 7 * t ** 0.7)
            y = y0 + hoogte * 0.55 * t
            r = 2.5 + 3 * t
            W.bol((x, y, z), r, wolk, ry=r * 0.9)
    # kop van de pluim
    for k in range(26):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(0, 1) ** 0.6 * 22
        x = xs_ - 6 + math.cos(a) * rr * 0.8
        z = math.sin(a) * rr
        y = y0 + hoogte * 0.58 + rng.uniform(0, 14) - rr * 0.25
        r = rng.uniform(5, 10) * (1 - rr / 34)
        W.bol((x, y, z), r, wolk, ry=r * 0.8)
    # druppels/sliertjes die naar achteren waaien
    for k in range(18):
        x = xs_ - 6 - rng.uniform(5, 60)
        z = rng.uniform(-20, 20)
        y = y0 + hoogte * 0.5 - (xs_ - 6 - x) * 0.35 + rng.uniform(-6, 6)
        W.bol((x, y, z), rng.uniform(1.5, 3.5), wolk)
        W.deeltjes.append((x, y + 4, z, (255, 255, 255), 0.8, 0.0, "stip"))


def vin(W, kant):
    """Borstvin: lange vleugelvin, naar achteren en omlaag gezwaaid, met bultjes op de voorrand."""
    b = X_NEUS - 150
    pts = [(b, as_y(b) - 20, kant * 100), (b - 12, as_y(b) - 27, kant * 130), (b - 32, as_y(b) - 35, kant * 158),
           (b - 54, as_y(b) - 42, kant * 178), (b - 72, as_y(b) - 46, kant * 192)]
    rads = [26, 22, 17, 11, 5]
    W.buis(pts, rads, P["huid_licht"], plat=0.24)
    # onderkant buikkleur: de buis nog eens, iets lager en dunner
    pts2 = [(p[0], p[1] - 2.2, p[2]) for p in pts]
    W.buis(pts2, [r - 1 for r in rads], P["buik"], plat=0.18)
    # bultjes (wolkpokken) op de voorrand
    for i in range(len(pts) - 1):
        for t in np.linspace(0, 1, 4, endpoint=False):
            p = np.array(pts[i]) * (1 - t) + np.array(pts[i + 1]) * t
            r = rads[i] * (1 - t) + rads[i + 1] * t
            W.bol((p[0] + r * 0.9, p[1] + 1.5, p[2]), max(1.6, r * 0.14), P["huid_pok"])


def staart(W):
    """Horizontale staartvin (flukes) met inkeping, licht omhoog (opslag)."""
    yt = as_y(X_STAART)
    span = 128.0
    o = X_STAART + 300
    for x in range(int(-360 + o), int(-286 + o)):
        for z in range(-int(span), int(span) + 1):
            q = abs(z) / span
            voor = -292 + o - 22 * q - 34 * q ** 3
            achter = -318 + o + 16 * math.exp(-(z / 13.0) ** 2) - 6 * q - 30 * q ** 2.2
            if not (achter <= x <= voor) or q > 1:
                continue
            dik = 6.5 * (1 - q ** 2) * min(1, (voor - x) / 6 + 0.35) + 1
            yc = yt + 0.30 * (X_STAART - x) + 6 * q * q
            for y in range(int(yc - dik / 2), int(yc + dik / 2) + 1):
                b = P["huid"] if y >= yc else P["buik"]
                if abs(z) < 3 and x > voor - 3:
                    b = P["huid_licht"]
                W.wset(x, y, z, b)


def wolksliert(W, pts, r0, seed=0):
    """Een sliert wolkjes langs punten (achter vinnen en staart: 'ze zwemt')."""
    rng = np.random.RandomState(seed)
    for i, p in enumerate(pts):
        t = i / max(1, len(pts) - 1)
        r = r0 * (1 - 0.75 * t) * rng.uniform(0.8, 1.2)
        W.bol((p[0] + rng.uniform(-2, 2), p[1] + rng.uniform(-1, 1), p[2] + rng.uniform(-2, 2)), r,
              "guhs:wolkenblok_wit", ry=r * 0.55)


def zwemwolken(W):
    for k in (-1, 1):
        tip = (X_NEUS - 222, as_y(X_NEUS - 150) - 46, k * 192)
        wolksliert(W, [(tip[0] - 12 * i, tip[1] + 1.5 * i, tip[2] + k * 3 * i) for i in range(9)], 6, seed=k + 5)
        wolksliert(W, [(X_STAART - 80 - 12 * i, as_y(X_STAART) + 20 + 2 * i, k * (110 + 4 * i)) for i in range(7)], 7,
                   seed=k + 9)
    # wolkenkraag onder de buik waar ze de wolkentoppen raakt
    rng = np.random.RandomState(4)
    for i in range(46):
        x = rng.uniform(X_STAART + 60, X_NEUS - 40)
        z = rng.uniform(-60, 60)
        y = buik(x, z)
        if np.isnan(y):
            continue
        r = rng.uniform(7, 15)
        W.bol((x, y - r * 0.2, z), r, "guhs:wolkenblok_wit", ry=r * 0.5)


def bouw(W, delen=("lijf", "kop", "vinnen", "staart", "spuit", "zwemwolken")):
    if "lijf" in delen:
        lijf(W)
    if "kop" in delen:
        for k in (-1, 1):
            oog(W, k)
            glimlach(W, k)
        oortjes(W)
        spuitgaten(W)
    if "vinnen" in delen:
        vin(W, -1)
        vin(W, 1)
    if "staart" in delen:
        staart(W)
    if "spuit" in delen:
        spuitwolk(W)
    if "zwemwolken" in delen:
        zwemwolken(W)
