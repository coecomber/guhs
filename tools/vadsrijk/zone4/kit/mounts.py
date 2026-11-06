"""De drie mount-kandidaten voor Het Vadsrijk als GeckoLib-modellen (geo.json + texture), in de stijl van
vadsrijk_economie.py (bones root/body/leg_*/head/tail + zadel), maar met afgeronde knuffelvormen en 4 px per modelpixel.

  pluimguh(variant)  -> Model   (VARIANTEN: roze, mint, lavendel, honing, wolkwit, sterrennacht, goud)
  knuffelhert()      -> Model
  guhwolf()          -> Model
  ZADEL[naam]        -> (zadel-bone, zitpunt, teugelpunten [(bone, punt)], benen-spreiding)

Los draaien: python mounts.py  -> schrijft alles naar _bron/modellen/.
"""
import numpy as np
from mountkit import (Model, guh_gezicht, schubben, sterretjes, combi, ellips, zacht, licht, mix, MODELS)

# ======================================================================================================================
# Pluimguh
# ======================================================================================================================
VARIANTEN = {
    "roze":         dict(basis=(255, 172, 208), licht=(255, 216, 234), diep=(238, 132, 184), snavel=(255, 110, 160), poot=(255, 196, 176)),
    "mint":         dict(basis=(160, 230, 200), licht=(222, 251, 238), diep=(104, 198, 166), snavel=(255, 120, 168), poot=(255, 204, 180)),
    "lavendel":     dict(basis=(198, 176, 242), licht=(234, 224, 255), diep=(160, 132, 224), snavel=(255, 120, 172), poot=(255, 200, 186)),
    "honing":       dict(basis=(250, 198, 102), licht=(255, 234, 172), diep=(224, 156, 58), snavel=(255, 122, 150), poot=(255, 214, 170)),
    "wolkwit":      dict(basis=(244, 244, 252), licht=(255, 255, 255), diep=(206, 214, 238), snavel=(255, 128, 172), poot=(255, 206, 196)),
    "sterrennacht": dict(basis=(52, 62, 128), licht=(104, 114, 190), diep=(32, 38, 92), snavel=(255, 132, 184), poot=(150, 158, 222),
                         extra="sterren"),
    "goud":         dict(basis=(246, 196, 70), licht=(255, 236, 150), diep=(212, 150, 36), snavel=(255, 128, 160), poot=(255, 214, 140),
                         extra="glans"),
}
NAMEN = {"roze": "Roze", "mint": "Mint", "lavendel": "Lavendel", "honing": "Honing", "wolkwit": "Wolkwit",
         "sterrennacht": "Sterrennacht", "goud": "Goud"}

LEER, LEER_D, GOUDJE = (176, 96, 84), (130, 64, 60), (255, 214, 96)


def _glans(arr, face=None):
    h, w = arr.shape[:2]
    for x0 in range(-h, w, 22):
        for y in range(h):
            for k in range(3):
                x = x0 + y + k
                if 0 <= x < w:
                    arr[y, x, :3] = mix(arr[y, x, :3], (255, 250, 220), 0.35)


def _extra(P):
    e = P.get("extra")
    if e == "sterren":
        return sterretjes(10, (255, 246, 196))
    if e == "glans":
        return _glans
    return None


def _deken(kleur, rand_k):
    def f(arr, face=None):
        h, w = arr.shape[:2]
        k = max(2, min(h, w) // 6)
        arr[:k, :, :3] = rand_k
        arr[-k:, :, :3] = rand_k
        arr[:, :k, :3] = rand_k
        arr[:, -k:, :3] = rand_k
        for x in range(k + 3, w - k - 2, 8):
            for y in range(k + 3, h - k - 2, 8):
                ellips(arr, x + 1, y + 1, 1.6, 1.6, (255, 255, 255))
    return f


def _zadel(m, bone, top, z0, z1, breed_top, breed_onder, y_knik, y_onder, deken=(120, 210, 200), rand=(255, 150, 190)):
    """Dekentje dat over de ronde rug valt (bovenstuk + getrapte flappen) en een leren zadeltje. Geeft het zitpunt."""
    zc, zl = (z0 + z1) / 2, z1 - z0
    dv = _deken(deken, rand)
    m.box(bone, (0, top + 0.4, zc), (breed_top * 2 - 2, 0.8, zl), deken, verf=dv, fur=3, rand=0.05)
    for s in (-1, 1):
        m.box(bone, (s * (breed_top + 0.3), (y_knik + top + 0.8) / 2, zc), (0.6, top + 0.8 - y_knik, zl - 1), deken,
              verf=dv, fur=3, rand=0.05)
        m.box(bone, (s * (breed_onder + 0.3), (y_onder + y_knik) / 2, zc), (0.6, y_knik - y_onder, zl - 2), deken,
              verf=dv, fur=3, rand=0.05)
        # gesp
        m.box(bone, (s * (breed_onder + 0.65), y_onder + 1.4, zc), (0.3, 1.2, 1.2), GOUDJE, fur=2, rand=0)
    zt = top + 0.8
    m.box(bone, (0, zt + 0.7, zc), (8, 1.4, 8), LEER, fur=3, rand=0.12)
    m.box(bone, (0, zt + 1.6, zc - 4.2), (3.6, 2.6, 1.6), LEER_D, fur=3, rand=0.1)  # zadelknop
    m.box(bone, (0, zt + 2.0, zc + 4.4), (8, 2.6, 1.6), LEER_D, fur=3, rand=0.1)  # achterboog
    m.box(bone, (0, zt + 3.4, zc + 4.4), (6, 0.6, 1.4), GOUDJE, fur=2, rand=0)
    return (0, zt + 1.4, zc + 0.6)


def pluimguh(variant="roze"):
    P = VARIANTEN[variant]
    B, L, Dp, SN, PO = P["basis"], P["licht"], P["diep"], P["snavel"], P["poot"]
    ex = _extra(P)
    veren = combi(schubben(0.88, 8), ex)
    pluis = combi(schubben(0.94, 6), ex)
    diepv = combi(schubben(0.86, 7), ex)
    m = Model("vr_pluimguh")
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 22, 0], "root")
    m.blob("body", (0, 24, 0), (16, 15, 21), B, r=3.5, verf=veren)
    m.blob("body", (0, 22.5, -8.6), (13, 12, 6), L, r=2.5, verf=pluis)          # borstpluis
    m.blob("body", (0, 18.3, 0.5), (11, 4, 14), L, r=1.5, verf=pluis)           # buikje
    m.blob("body", (0, 25.5, 9.2), (12, 9, 5), B, r=2, verf=veren)             # achterwerkje
    # zadel
    m.bone("zadel", [0, 31.5, 1], "body")
    deken = {"roze": (120, 210, 200), "mint": (255, 160, 200), "sterrennacht": (255, 210, 110), "goud": (190, 120, 220)}.get(variant, (120, 210, 200))
    _zadel(m, "zadel", 31.5, -4.5, 7.5, 6.7, 8.0, 28.0, 22.0, deken=deken)
    # vleugeltjes
    for s, n in ((1, "l"), (-1, "r")):
        m.bone(f"vleugel_{n}", [7.8 * s, 28, -4], "body")
        m.blob(f"vleugel_{n}", (8.3 * s, 25, 0.5), (2.4, 9, 12), Dp, r=1.0, verf=diepv)
        for (y, z, ln) in ((22.0, 6.4, 5.5), (24.2, 8.0, 5.5), (26.4, 8.6, 4.5)):
            m.box(f"vleugel_{n}", (8.3 * s, y, z), (2.0, 2.6, ln), L, verf=pluis, fur=4, rand=0.08)
    # poten (kinderen van root: de rug kan kantelen zonder dat de voeten van de grond gaan)
    for s, n in ((1, "l"), (-1, "r")):
        x = 4.3 * s
        m.bone(f"leg_{n}", [x, 19, 2], "root")
        m.blob(f"leg_{n}", (x, 17.2, 2.2), (6, 7.4, 7), B, r=1.8, verf=veren)       # dij-pluis
        m.bone(f"scheen_{n}", [x, 13.5, 2.6], f"leg_{n}")
        m.box(f"scheen_{n}", (x, 13.2, 2.6), (3.8, 1.8, 3.8), L, verf=pluis, fur=4)   # pluissokje
        m.box(f"scheen_{n}", (x, 7.6, 2.8), (2.6, 11.4, 2.6), PO, verf=schubben(0.9, 6), fur=3, rand=0.08)
        m.bone(f"voet_{n}", [x, 1.6, 2.8], f"scheen_{n}")
        m.box(f"voet_{n}", (x, 0.8, 0.6), (1.8, 1.6, 5.0), PO, fur=3, rand=0.1)
        for t in (-1, 1):
            m.box(f"voet_{n}", (x + t * 1.7, 0.75, 1.2), (1.6, 1.5, 4.0), PO, rotation=(0, 22 * t, 0),
                  pivot=(x, 0.75, 3.0), fur=3, rand=0.1)
        m.box(f"voet_{n}", (x, 0.65, 4.6), (1.4, 1.3, 2.4), PO, fur=3, rand=0.1)
        for t in (-1, 0, 1):  # teennageltjes
            m.box(f"voet_{n}", (x + t * 2.3, 0.5, -1.6 + abs(t) * 0.9), (1.0, 1.0, 0.9), licht(PO, 1.08), fur=2, rand=0)
    # nek + kop
    m.bone("nek", [0, 29, -7], "body")
    m.blob("nek", (0, 35, -10), (8, 14, 8), B, r=2.5, verf=veren)
    m.blob("nek", (0, 30.5, -8.6), (12, 5, 10), L, r=2, verf=pluis)          # kraagje
    m.bone("hoofd", [0, 40, -10], "nek")

    def gezicht(arr, face=None):
        if ex:
            ex(arr, face)
        guh_gezicht(arr, oog_y=0.42, afstand=0.24, r=arr.shape[1] * 0.165, mond=False,
                    iris=(80, 160, 235) if variant != "sterrennacht" else (150, 200, 255))
    m.blob("hoofd", (0, 45.5, -11.5), (15, 12.5, 12), B, r=1.8, voor=gezicht, verf=pluis)
    for s in (-1, 1):  # bolle wangetjes
        m.blob("hoofd", (s * 6.6, 42.6, -13.2), (3, 4, 5), L, r=1, verf=pluis)
    m.bone("snavel", [0, 43, -17], "hoofd")

    def neusgaatjes(arr, face=None):
        h, w = arr.shape[:2]
        for xx in (w * 0.3, w * 0.7):
            ellips(arr, xx, h * 0.75, 1.3, 1.0, licht(SN, 0.6))
    m.blob("snavel", (0, 43.2, -18.9), (4.8, 2.4, 3.2), SN, r=0.7, verf={"up": neusgaatjes})
    m.box("snavel", (0, 42.9, -20.8), (2.8, 1.5, 1.2), licht(SN, 1.04), fur=3, rand=0.1)
    m.box("snavel", (0, 41.6, -18.5), (3.6, 1.2, 2.2), licht(SN, 0.86), fur=3, rand=0.1)
    # guh-oren (ronde schijfjes, roze binnenkant)
    for s, n in ((1, "l"), (-1, "r")):
        m.bone(f"oor_{n}", [6 * s, 50.5, -11], "hoofd")

        def binnen(arr, face=None, B=B):
            h, w = arr.shape[:2]
            ellips(arr, w / 2, h / 2, w * 0.36, h * 0.36, (255, 150, 192))
        for sz in ((8, 6, 1.2), (6, 8, 1.4)):
            m.box(f"oor_{n}", (9.0 * s, 54.0, -11), sz, B, verf={"north": binnen}, rotation=(0, 0, 18 * s),
                  pivot=(6 * s, 50.5, -11), fur=4, rand=0.12)
    # kuifje: drie zachte veertjes
    m.bone("kuif", [0, 51, -12], "hoofd")
    for dx, top in ((-1.4, (-2.2, 55.0, -13.8)), (0, (0, 56.4, -13.2)), (1.4, (2.2, 55.0, -13.8))):
        m.staaf("kuif", (dx * 0.5, 50.8, -12.2), top, 1.3, L, d=1.0,
                verf=lambda a, f=None: a.__setitem__((slice(0, max(1, a.shape[0] // 3)), slice(None), slice(0, 3)),
                                                     np.array(licht(Dp, 1.0))))
    # staartpluim: waaier
    m.bone("staart", [0, 27, 10], "body")

    def tip(arr, face=None):
        h = arr.shape[0]
        k = max(1, int(h * 0.35))
        arr[:k, :, :3] = np.array(L) + np.random.default_rng(h).normal(0, 4, (k, arr.shape[1], 1))
        if ex:
            ex(arr, face)
    for (dx, dy, dz, w) in ((0, 34.5, 19.5, 3.4), (-3.4, 32.0, 18.6, 3.0), (3.4, 32.0, 18.6, 3.0),
                            (-5.6, 28.4, 17.2, 2.6), (5.6, 28.4, 17.2, 2.6)):
        m.staaf("staart", (dx * 0.25, 26.5, 10.5), (dx, dy, dz), w, Dp, verf=tip, d=1.6)
    return m


# ======================================================================================================================
# Knuffelhert
# ======================================================================================================================
def knuffelhert():
    V, LI, SN, HO = (232, 188, 148), (255, 242, 228), (255, 222, 214), (122, 86, 96)
    GW, BL1, BL2, LA, LF = (214, 176, 140), (255, 176, 210), (255, 238, 246), (255, 216, 110), (112, 72, 66)
    m = Model("vr_knuffelhert")

    def vlekjes(arr, face=None):
        if face in ("up", "east", "west"):
            h, w = arr.shape[:2]
            rng = np.random.default_rng(h * 7 + w)
            for _ in range(max(1, h * w // 300)):
                zacht(arr, rng.uniform(0, w), rng.uniform(0, h * (0.6 if face != "up" else 1)), 3.2, 2.8, LI, 0.9)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 22, 0], "root")
    m.blob("body", (0, 23, 0), (14, 12, 24), V, r=3, verf=vlekjes)
    m.blob("body", (0, 18.4, 0), (10, 3, 18), LI, r=1)
    m.blob("body", (0, 22.5, -11.0), (10, 9, 5), LI, r=2)       # borstpluis
    m.bone("tail", [0, 26, 12], "body")
    m.blob("tail", (0, 27, 13.2), (4.4, 4.4, 3.4), LI, r=1.1)
    m.bone("zadel", [0, 29, 1], "body")
    _zadel(m, "zadel", 29.0, -5.0, 7.0, 5.9, 7.0, 26.0, 20.5, deken=(126, 196, 120), rand=(255, 236, 160))
    for n, x, z, achter in (("leg_fl", 4, -8.5, False), ("leg_fr", -4, -8.5, False), ("leg_bl", 4, 8, True),
                            ("leg_br", -4, 8, True)):
        m.bone(n, [x, 18, z], "root")
        if achter:
            m.blob(n, (x, 17, z + 0.5), (5, 9, 7), V, r=1.5, verf=vlekjes)
        else:
            m.blob(n, (x, 15.5, z), (4.4, 8, 5), V, r=1.3)
        m.box(n, (x, 6.5, z), (3, 10, 3), V, fur=4, rand=0.1)
        m.box(n, (x, 2.8, z), (3.8, 1.4, 3.8), LI, fur=4, rand=0.08)
        m.box(n, (x, 1.0, z - 0.2), (3.4, 2.0, 3.6), HO, fur=3, rand=0.1)
    m.bone("nek", [0, 26, -9], "body")
    m.blob("nek", (0, 31, -11.5), (7, 12, 7), V, r=2)
    m.bone("hoofd", [0, 36, -12], "nek")

    def gezicht(arr, face=None):
        guh_gezicht(arr, oog_y=0.36, afstand=0.25, r=arr.shape[1] * 0.155, mond=False)
    m.blob("hoofd", (0, 39.2, -13), (12, 10, 10), V, r=2.0, voor=gezicht)

    def neus(arr, face=None):
        h, w = arr.shape[:2]
        ellips(arr, w / 2, h * 0.32, w * 0.16, h * 0.18, (150, 80, 100))
        ellips(arr, w / 2 - w * 0.05, h * 0.27, w * 0.04, h * 0.05, (255, 255, 255))
        for dx in range(-int(w * 0.12), int(w * 0.12) + 1):
            yy = int(h * 0.62 + abs(np.sin(dx / (w * 0.12) * np.pi)) * 1.5)
            arr[yy, int(w / 2 + dx), :3] *= 0.6
    m.blob("hoofd", (0, 36.4, -18.6), (6.4, 4.2, 3.6), SN, r=1.0, voor=neus)
    for s, n in ((1, "l"), (-1, "r")):
        m.bone(f"oor_{n}", [5.5 * s, 41, -12], "hoofd")

        def binnen(arr, face=None):
            h, w = arr.shape[:2]
            ellips(arr, w / 2, h / 2, w * 0.36, h * 0.36, (255, 160, 196))
        for sz in ((7, 5, 1.1), (5, 7, 1.3)):
            m.box(f"oor_{n}", (8.8 * s, 42.0, -12.5), sz, V, verf={"north": binnen}, rotation=(0, 0, -38 * s),
                  pivot=(5.5 * s, 41, -12), fur=4, rand=0.12)
    # gewei met bloesem en lampionnetjes
    m.bone("gewei", [0, 44, -13], "hoofd")

    def bloesem(c, k):
        m.box("gewei", c, (1.9, 1.9, 1.9), k, verf=lambda a, f=None: ellips(a, a.shape[1] / 2, a.shape[0] / 2, 1.6, 1.6, (255, 220, 120)),
              fur=3, rand=0.05)
    for s in (-1, 1):
        a0, a1, a2 = (2.6 * s, 43.6, -12.6), (5.0 * s, 49.5, -12.0), (6.6 * s, 55.0, -11.0)
        t1, t2, t3 = (9.6 * s, 51.6, -13.4), (4.6 * s, 58.8, -12.2), (10.0 * s, 57.6, -10.2)
        for p, q, w in ((a0, a1, 1.6), (a1, a2, 1.4), (a1, t1, 1.2), (a2, t2, 1.1), (a2, t3, 1.1)):
            m.staaf("gewei", p, q, w, GW, fur=3, rand=0.08)
        for c, k in ((t1, BL1), (t2, BL2), (t3, BL1), ((a2[0] + 0.8 * s, a2[1] + 0.8, a2[2] - 1), BL2),
                     ((a1[0] - 0.4 * s, a1[1] + 1.4, a1[2] - 1.2), BL1), ((t2[0] + 1.5 * s, t2[1] - 1.2, t2[2] + 0.5), BL1),
                     ((t3[0] - 1.6 * s, t3[1] + 0.6, t3[2] - 0.6), BL2)):
            bloesem(c, k)
        # blaadjes
        m.box("gewei", (t1[0] - 0.8 * s, t1[1] + 1.4, t1[2] + 0.9), (1.4, 0.6, 2.2), (130, 200, 120), fur=3, rand=0)
        m.box("gewei", (t3[0] + 0.4 * s, t3[1] + 1.4, t3[2] + 1.0), (1.4, 0.6, 2.2), (130, 200, 120), fur=3, rand=0)
        # lampionnetjes aan een touwtje
        for (hx, hy, hz), ln in (((t1[0] + 0.3 * s, t1[1] - 1.0, t1[2]), 2.2), ((t3[0], t3[1] - 1.0, t3[2] + 0.4), 2.8)):
            m.box("gewei", (hx, hy - ln / 2, hz), (0.35, ln, 0.35), (90, 70, 70), fur=1, rand=0)
            ly = hy - ln - 1.5

            def gloed(arr, face=None):
                h, w = arr.shape[:2]
                zacht(arr, w / 2, h / 2, w * 0.45, h * 0.45, (255, 250, 210), 0.9)
                arr[:, :max(1, w // 6), :3] *= 0.8
                arr[:, -max(1, w // 6):, :3] *= 0.8
            m.box("gewei", (hx, ly, hz), (2.4, 2.8, 2.4), LA, verf=gloed, fur=2, rand=0.0)
            m.box("gewei", (hx, ly + 1.65, hz), (1.8, 0.5, 1.8), LF, fur=2, rand=0)
            m.box("gewei", (hx, ly - 1.65, hz), (1.8, 0.5, 1.8), LF, fur=2, rand=0)
    return m


# ======================================================================================================================
# Guhwolf
# ======================================================================================================================
def guhwolf():
    V, RUG, BU, NE = (240, 230, 244), (178, 160, 210), (255, 250, 252), (70, 44, 66)
    SJ, SJ2 = (236, 84, 120), (255, 236, 242)
    m = Model("vr_guhwolf")

    def rugvacht(arr, face=None):
        h, w = arr.shape[:2]
        if face == "up":
            arr[..., :3] = arr[..., :3] * 0 + np.array(RUG) + np.random.default_rng(h).normal(0, 4, (h, w, 1))
        elif face in ("east", "west", "north", "south"):
            k = int(h * 0.42)
            for y in range(k):
                t = 1 - y / max(1, k)
                arr[y, :, :3] = arr[y, :, :3] * (1 - t) + np.array(RUG) * t
        # plukjes
        rng = np.random.default_rng(h * 3 + w)
        for _ in range(h * w // 40):
            x, y = int(rng.integers(0, w)), int(rng.integers(0, max(1, h - 3)))
            arr[y:y + 3, x, :3] *= 0.93

    def pluche(arr, face=None):
        h, w = arr.shape[:2]
        rng = np.random.default_rng(h * 5 + w)
        for _ in range(h * w // 30):
            x, y = int(rng.integers(0, w)), int(rng.integers(0, max(1, h - 3)))
            arr[y:y + 3, x, :3] *= 0.95
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 21, 0], "root")
    m.blob("body", (0, 21, 1), (15, 13, 22), V, r=3.5, verf=rugvacht)
    m.blob("body", (0, 20, -8.8), (12.5, 11, 7), BU, r=2.6, verf=pluche)     # borstkraag
    m.blob("body", (0, 15.6, 1), (10, 3, 15), BU, r=1)
    m.bone("zadel", [0, 27.5, 1.5], "body")
    _zadel(m, "zadel", 27.5, -4.0, 7.5, 6.3, 7.6, 24.5, 18.6, deken=(110, 170, 235), rand=(255, 236, 160))
    m.bone("tail", [0, 25, 11.5], "body")
    m.blob("tail", (0, 26.5, 14.5), (5.4, 5.4, 6), V, r=1.6, verf=rugvacht)
    m.blob("tail", (0, 31, 16.4), (5.6, 6, 5), V, r=1.6, verf=pluche)
    m.blob("tail", (0, 34.2, 15.4), (4.4, 3.6, 4.4), BU, r=1.2)
    for n, x, z, achter in (("leg_fl", 4.6, -6.5, False), ("leg_fr", -4.6, -6.5, False), ("leg_bl", 4.6, 7.8, True),
                            ("leg_br", -4.6, 7.8, True)):
        m.bone(n, [x, 15, z], "root")
        if achter:
            m.blob(n, (x, 14.5, z + 0.5), (6, 9, 8), V, r=1.8, verf=pluche)
        m.blob(n, (x, 7.5, z), (5, 12, 5), V, r=1.2, verf=pluche)

        def teentjes(arr, face=None):
            h, w = arr.shape[:2]
            for xx in (w * 0.35, w * 0.65):
                arr[int(h * 0.3):, int(xx), :3] *= 0.7
        m.blob(n, (x, 1.5, z - 0.7), (6, 3, 6.6), BU, r=1.0, voor=teentjes)
    m.bone("hoofd", [0, 28, -10], "body")

    def gezicht(arr, face=None):
        guh_gezicht(arr, oog_y=0.38, afstand=0.25, r=arr.shape[1] * 0.155, mond=False)
    m.blob("hoofd", (0, 33, -13.5), (15, 13, 12), V, r=2.6, voor=gezicht, verf={"up": rugvacht})
    for s in (-1, 1):
        m.blob("hoofd", (s * 7.0, 29.6, -14.0), (3, 4, 6), BU, r=1, verf=pluche)   # wangpluisjes

    def snuit(arr, face=None):
        h, w = arr.shape[:2]
        for dx in range(-int(w * 0.2), int(w * 0.2) + 1):
            yy = int(h * 0.62 + abs(np.sin(dx / (w * 0.2) * np.pi)) * 2)
            arr[yy:yy + 1, int(w / 2 + dx), :3] = (110, 70, 90)
        arr[int(h * 0.3):int(h * 0.62), int(w / 2), :3] = (110, 70, 90)
    m.blob("hoofd", (0, 29.4, -20.6), (7, 4.6, 4.2), BU, r=1.1, voor=snuit)

    def neusje(arr, face=None):
        h, w = arr.shape[:2]
        ellips(arr, w * 0.35, h * 0.3, w * 0.12, h * 0.18, (200, 170, 200))
    m.blob("hoofd", (0, 31.3, -22.8), (3.0, 1.9, 1.4), NE, r=0.4, voor=neusje)
    for s, n in ((1, "l"), (-1, "r")):
        m.bone(f"oor_{n}", [5.5 * s, 38.5, -13], "hoofd")

        def binnen(arr, face=None):
            h, w = arr.shape[:2]
            ellips(arr, w / 2, h / 2, w * 0.36, h * 0.36, (255, 160, 196))
        for sz in ((8.4, 6.4, 1.3), (6.4, 8.4, 1.5)):
            m.box(f"oor_{n}", (8.0 * s, 42.2, -13), sz, V, verf={"north": binnen, "south": rugvacht},
                  rotation=(0, 0, 14 * s), pivot=(5.5 * s, 38.5, -13), fur=4, rand=0.12)
    # sjaal
    m.bone("sjaal", [0, 27, -10], "body")

    def streep(arr, face=None):
        h, w = arr.shape[:2]
        for x in range(0, w, 12):
            arr[:, x:x + 5, :3] = np.array(SJ2) + np.random.default_rng(x).normal(0, 3, (h, min(5, w - x), 1))

    def streep_h(arr, face=None):
        h, w = arr.shape[:2]
        for y in range(4, h - 6, 12):
            arr[y:y + 5, :, :3] = SJ2
        for x in range(0, w, 3):
            arr[h - 4:, x, :3] = licht(SJ, 0.8)
            arr[h - 4:, x + 1:x + 3, 3] = 0
    m.blob("sjaal", (0, 27.6, -10.2), (13.5, 4.2, 11.5), SJ, r=1.3, verf=streep)
    m.box("sjaal", (6.9, 22.4, -13.2), (1.2, 9, 3.6), SJ, verf=streep_h, rotation=(8, 0, -10), pivot=(6.6, 27, -13.2),
          fur=3, rand=0.06)
    m.box("sjaal", (7.4, 23.8, -11.0), (1.2, 7, 3.4), SJ, verf=streep_h, rotation=(-6, 0, -16), pivot=(6.6, 27, -11),
          fur=3, rand=0.06)
    return m


# zadel-bone, zitpunt (GeckoLib), teugelpunten [(bone, punt)], benen-spreiding (graden)
ZADEL = {
    "pluimguh": ("zadel", (0, 33.7, 1.6), [("snavel", (2.4, 43.0, -17.6)), ("snavel", (-2.4, 43.0, -17.6))], 40),
    "knuffelhert": ("zadel", (0, 31.2, 1.6), [("hoofd", (3.2, 36.4, -18.0)), ("hoofd", (-3.2, 36.4, -18.0))], 30),
    "guhwolf": ("zadel", (0, 29.7, 2.3), [("sjaal", (5.6, 28.0, -14.0)), ("sjaal", (-5.6, 28.0, -14.0))], 40),
}


def schrijf_alles():
    paden = {}
    for i, v in enumerate(VARIANTEN):
        m = pluimguh(v)
        paden[f"pluimguh_{v}"] = (m.naam, m.schrijf(f"vr_pluimguh_{v}", geo=(i == 0)))
    for f in (knuffelhert, guhwolf):
        m = f()
        paden[m.naam] = (m.naam, m.schrijf())
    return paden


if __name__ == "__main__":
    for k, v in schrijf_alles().items():
        print(k, v)
    print("->", MODELS)
