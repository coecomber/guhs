"""Figuren voor het Zijdeland: de bultguh (kameelguh), het vliegende tapijt en de pluimguh, met guhs erop.

  bultguh(variant)                      -> mountkit-Model (GeckoLib-geo + texture, net als de pluimguh)
  bultguh_quads(pos, yaw, variant, pose) -> extra-quads in wereldcoördinaten (zet ze met SK.quads_naar_voxel)
  tapijt_quads(pos, yaw, patroon, fase) -> een zwevend tapijt met golf en kwastjes
  zet_bultguh(W, x, y, z, yaw, variant, ruiter=True, lopend=False), zet_tapijt(W, ...), zet_pluimguh(W, ...)
  karavaan(W, punten, ...)              een stoet bultguhs + tapijten langs een lijn
  bevolk(W)                             zet figuren op de bekende plekken die in het venster liggen
Varianten van de bultguh: zandroze, saffraan, wolkwit, kaneel, sterrennacht (raid-beloning).
"""
import math
import numpy as np
import schemerkit as SK
import mountkit as K
from mountkit import Model, guh_gezicht, schubben, ellips, zacht, licht, mix

VARIANTEN = {
    "zandroze": dict(basis=(242, 196, 190), licht=(255, 230, 222), diep=(214, 160, 156), snuit=(255, 214, 200)),
    "saffraan": dict(basis=(246, 200, 128), licht=(255, 234, 184), diep=(222, 164, 90), snuit=(255, 222, 176)),
    "wolkwit": dict(basis=(246, 242, 240), licht=(255, 255, 255), diep=(214, 206, 214), snuit=(255, 226, 226)),
    "kaneel": dict(basis=(214, 156, 124), licht=(240, 200, 170), diep=(178, 120, 96), snuit=(246, 196, 170)),
    "sterrennacht": dict(basis=(70, 70, 140), licht=(120, 120, 196), diep=(44, 44, 104), snuit=(150, 150, 210)),
}
DEKENS = [((236, 84, 150), (255, 214, 96)), ((60, 186, 196), (255, 240, 220)), ((150, 96, 210), (255, 190, 220)),
          ((255, 150, 60), (255, 240, 180)), ((230, 60, 90), (60, 186, 196))]
_cache = {}


def _deken(kl, rand):
    def f(arr, face=None):
        h, w = arr.shape[:2]
        k = max(2, min(h, w) // 7)
        arr[..., :3] = kl
        arr[:k, :, :3] = rand
        arr[-k:, :, :3] = rand
        arr[:, :k, :3] = rand
        arr[:, -k:, :3] = rand
        for x in range(k + 2, w - k - 1, 6):
            for y in range(k + 2, h - k - 1, 6):
                ellips(arr, x + 1, y + 1, 1.5, 1.5, rand)
    return f


def bultguh(variant="zandroze", deken=0):
    key = (variant, deken)
    if key in _cache:
        return _cache[key]
    P = VARIANTEN[variant]
    B, L, D, SN = P["basis"], P["licht"], P["diep"], P["snuit"]
    pluis = schubben(0.95, 7)
    m = Model("vr_bultguh_" + variant)
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 26, 0], "root")
    m.blob("body", (0, 28, 0), (14, 12, 27), B, r=3.5, verf=pluis)
    m.blob("body", (0, 23.5, 0), (10, 4, 20), L, r=1.5, verf=pluis)                # buikje
    m.blob("body", (0, 37, 1.5), (11, 9, 13), B, r=3.5, verf=pluis)               # de bult
    m.blob("body", (0, 41, 1.5), (7, 3, 8), L, r=1.2, verf=pluis)                 # bultpluis
    kl, rand = DEKENS[deken % len(DEKENS)]
    m.bone("zadel", [0, 42, 1.5], "body")
    m.box("zadel", (0, 42.6, 1.5), (12.5, 1.4, 14.5), kl, verf=_deken(kl, rand), fur=2, rand=0.04)
    for s in (-1, 1):
        m.box("zadel", (s * 6.6, 37.5, 1.5), (0.8, 9, 13), kl, verf=_deken(kl, rand), fur=2, rand=0.04)
        for k in range(5):                                                        # kwastjes
            m.box("zadel", (s * 7.0, 32.2, -4.5 + k * 3), (0.8, 1.6, 0.8), rand, fur=1, rand=0)
        m.box("zadel", (s * 8.4, 29.5, 1.5), (2.4, 6, 7), (196, 120, 90), fur=3, rand=0.12)   # zadeltassen
        m.box("zadel", (s * 9.7, 30.5, 1.5), (0.4, 2.2, 3), (255, 214, 96), fur=1, rand=0)
    m.box("zadel", (0, 44.2, 1.5), (9, 1.6, 9), (250, 120, 170), fur=3, rand=0.1)       # zitkussen
    for (sx, sz, n) in ((1, -1, "lv"), (-1, -1, "rv"), (1, 1, "la"), (-1, 1, "ra")):
        x, z = 4.8 * sx, 9.5 * sz
        m.bone("leg_" + n, [x, 23, z], "root")
        m.blob("leg_" + n, (x, 20, z), (5, 8, 6), B, r=1.6, verf=pluis)
        m.bone("scheen_" + n, [x, 15, z], "leg_" + n)
        m.box("scheen_" + n, (x, 9.5, z), (3.4, 12, 3.4), B, verf=pluis, fur=3, rand=0.08)
        m.box("scheen_" + n, (x, 3.2, z), (5, 4, 5), L, verf=pluis, fur=4, rand=0.1)       # pluissokje
        m.box("scheen_" + n, (x, 0.6, z - 0.3), (4.2, 1.2, 4.6), D, fur=2, rand=0.1)       # hoefje
    m.bone("nek", [0, 30, -11], "body")
    m.staaf("nek", (0, 29, -11), (0, 43, -19), 6.2, B, d=6.5, verf=pluis)
    m.blob("nek", (0, 33, -13.5), (7, 6, 7), L, r=2, verf=pluis)                     # halspluis
    m.bone("hoofd", [0, 45, -20], "nek")

    def gezicht(arr, face=None):
        guh_gezicht(arr, oog_y=0.36, afstand=0.25, r=arr.shape[1] * 0.15, mond=False,
                    iris=(80, 160, 235) if variant != "sterrennacht" else (190, 220, 255))
    m.blob("hoofd", (0, 48.5, -22.5), (12, 10, 11), B, r=2, voor=gezicht, verf=pluis)
    m.blob("hoofd", (0, 45.6, -28.2), (7.2, 5, 4.4), SN, r=1.2)                    # snuitje

    def mond(arr, face=None):
        h, w = arr.shape[:2]
        for xx in (w * 0.33, w * 0.67):
            ellips(arr, xx, h * 0.35, 1.0, 0.8, licht(SN, 0.6))
        for dx in range(-int(w * 0.2), int(w * 0.2) + 1):
            yy = int(h * 0.72 + abs(math.sin(dx / max(1, w * 0.2) * math.pi)) * 1.2)
            if 0 <= yy < h:
                arr[yy, int(w / 2 + dx), :3] = np.array(licht(SN, 0.55))
    m.box("hoofd", (0, 45.6, -30.45), (5.2, 3.6, 0.1), SN, verf=mond, fur=0, rand=0)
    for s, n in ((1, "l"), (-1, "r")):                                               # ronde guh-oortjes
        m.bone("oor_" + n, [5 * s, 53, -21], "hoofd")

        def binnen(arr, face=None, B=B):
            h, w = arr.shape[:2]
            ellips(arr, w / 2, h / 2, w * 0.34, h * 0.34, (255, 150, 192))
        for sz in ((7, 5.5, 1.2), (5.5, 7, 1.4)):
            m.box("oor_" + n, (7.6 * s, 56.0, -21), sz, B, verf={"north": binnen}, rotation=(0, 0, 22 * s),
                  pivot=(5 * s, 53, -21), fur=4, rand=0.12)
    m.bone("kuif", [0, 53.5, -22], "hoofd")
    m.blob("kuif", (0, 54.2, -22.5), (4, 2.2, 4), L, r=0.8, verf=pluis)
    m.bone("staart", [0, 30, 13], "body")
    m.staaf("staart", (0, 31, 13), (0, 22, 16.5), 1.8, D, d=1.8)
    m.blob("staart", (0, 20.8, 16.8), (3, 4, 3), L, r=1, verf=pluis)
    g = m.geo()["minecraft:geometry"][0]
    mod = {"g": g, "tex": np.clip(m.textuur(), 0, 255) / 255.0}
    _cache[key] = mod
    return mod


LOOP = {"leg_lv": {"rot": [-22, 0, 0]}, "leg_ra": {"rot": [-22, 0, 0]}, "leg_rv": {"rot": [18, 0, 0]},
        "leg_la": {"rot": [18, 0, 0]}, "scheen_lv": {"rot": [14, 0, 0]}, "scheen_ra": {"rot": [14, 0, 0]},
        "nek": {"rot": [-6, 0, 0]}, "staart": {"rot": [10, 0, 14]}}
SCHAAL = 0.92
ZIT_Y = 45.6 / 16.0 * SCHAAL          # bovenkant zitkussen boven de voeten (blokken)


def bultguh_quads(pos, yaw, variant="zandroze", deken=0, lopend=False):
    m = bultguh(variant, deken)
    pose = LOOP if lopend else None
    return K.quads(m, K.base_mat(pos, yaw, SCHAAL), pose)


def zet_bultguh(W, x, y, z, yaw=0, variant="zandroze", deken=0, ruiter=True, lopend=False, skin="guh", schaduw=True):
    """Bultguh op wereldpositie (x, y = bovenkant grond, z). yaw 0 = kijkt naar het noorden (-z)."""
    if not W.binnen(x, y, z, marge=4):
        return
    rijdier(W, "bultguh", x, y, z, yaw, variant, ruiter, skin)   # [zone 6 kaart] the game spawns it (Z6Bewoners)
    q = bultguh_quads((x, y, z), yaw, variant, deken, lopend)
    if schaduw:
        q += K.schaduw_quad(x, y + 0.03, z, 1.2, 1.6)
    W.extra += SK.quads_naar_voxel(W, q)
    if ruiter:
        a = math.radians(yaw)
        # zitpunt: iets achter het midden van de bult
        zx = x + math.sin(a) * 0.08
        zz = z + math.cos(a) * 0.08
        W.wguh(zx, y + ZIT_Y + 0.05, zz, yaw % 360, skin=skin, sitting=True)   # W-LEVEN: the rider faces where its mount faces


# ============================================================================================ vliegend tapijt
def _tapijt_tex(kl, rand, accent, n=64):
    t = np.zeros((n * 2, n, 4), np.float32)
    t[..., 3] = 1
    t[..., :3] = np.array(kl) / 255
    yy, xx = np.mgrid[0:n * 2, 0:n]
    b = 6
    rm = (xx < b) | (xx >= n - b) | (yy < b) | (yy >= 2 * n - b)
    t[rm, :3] = np.array(rand) / 255
    rm2 = ((xx == b + 1) | (xx == n - b - 2) | (yy == b + 1) | (yy == 2 * n - b - 2)) & ~rm
    t[rm2, :3] = np.array(accent) / 255
    # medaillon + ruitjes
    cx, cy = n / 2, n
    d = np.abs(xx - cx) / (n * 0.3) + np.abs(yy - cy) / (n * 0.55)
    t[(d < 1) & (d > 0.82), :3] = np.array(accent) / 255
    t[d < 0.45, :3] = np.array(rand) / 255
    t[(d < 0.2), :3] = np.array(accent) / 255
    for (px, py) in ((n * 0.28, n * 0.4), (n * 0.72, n * 0.4), (n * 0.28, n * 1.6), (n * 0.72, n * 1.6)):
        dd = np.abs(xx - px) + np.abs(yy - py)
        t[dd < 4, :3] = np.array(accent) / 255
    return t


PATRONEN = [((236, 84, 150), (255, 214, 96), (60, 186, 196)), ((60, 150, 200), (255, 240, 220), (236, 84, 150)),
            ((150, 70, 170), (255, 190, 220), (255, 214, 96)), ((230, 120, 60), (255, 236, 200), (150, 70, 170))]


def tapijt_quads(pos, yaw, patroon=0, fase=0.0, L=3.0, B=2.0, golf=0.16):
    kl, rand, acc = PATRONEN[patroon % len(PATRONEN)]
    tex = _tapijt_tex(kl, rand, acc)
    onder = tex.copy()
    onder[..., :3] *= 0.7
    a = math.radians(yaw)
    f = np.array([-math.sin(a), 0, -math.cos(a)])      # voor
    r = np.array([math.cos(a), 0, -math.sin(a)])       # rechts
    p0 = np.asarray(pos, float)
    n = 10
    out = []
    pts = []
    for i in range(n + 1):
        t = i / n
        l = (t - 0.5) * L
        h = golf * math.sin(t * 2 * math.pi * 1.2 + fase) + 0.25 * (t - 0.5) ** 2
        pts.append(p0 + f * l + np.array([0, h, 0]))
    for i in range(n):
        a0, a1 = pts[i], pts[i + 1]
        u0, u1 = i / n, (i + 1) / n
        for kant, tx_ in ((1, tex), (-1, onder)):
            c0 = a0 - r * B / 2
            P = np.array([c0, a1 - r * B / 2, c0 + r * B]) if kant > 0 else np.array([c0 + r * B, a1 - r * B / 2 + r * B, c0])
            if kant > 0:
                cu = np.array([(0, u0), (0, u1), (1, u1), (1, u0)], float)
                P = np.array([c0, c0 + r * B, a1 - r * B / 2])
                cu = np.array([(0, u0), (1, u0), (1, u1), (0, u1)], float)
            else:
                P = np.array([c0 + r * B, c0, a1 - r * B / 2 + r * B])
                cu = np.array([(1, u0), (0, u0), (0, u1), (1, u1)], float)
            out.append((P, cu, tx_, np.ones(3), False))
    # kwastjes aan de korte kanten
    for i in (0, n):
        for k in range(5):
            q = pts[i] - r * B / 2 + r * B * (k + 0.5) / 5
            q2 = q + f * (0.25 if i == n else -0.25) + np.array([0, -0.08, 0])
            out += K.balk_quads(q, q2, 0.06, rand)
    return out


def zet_tapijt(W, x, y, z, yaw=0, patroon=0, fase=0.0, ruiter=True, skin="guh", hoogte=1.3, schaduw=True, grond=None):
    if not W.binnen(x, y, z, marge=4):
        return
    rijdier(W, "tapijt", x, y, z, yaw, str(patroon % 4), ruiter, skin)   # [zone 6 kaart]
    q = tapijt_quads((x, y + hoogte, z), yaw, patroon, fase)
    if schaduw:
        q += K.schaduw_quad(x, (grond if grond is not None else y) + 0.03, z, 1.1, 1.6, sterkte=0.35)
    W.extra += SK.quads_naar_voxel(W, q)
    if ruiter:
        W.wguh(x, y + hoogte + 0.1, z, yaw % 360, skin=skin, sitting=True)   # W-LEVEN: the rider faces where its carpet faces


def zet_pluimguh(W, x, y, z, yaw=0, variant="roze", ruiter=False):
    import pluimrit
    if not W.binnen(x, y, z, marge=4):
        return
    rijdier(W, "pluimguh", x, y, z, yaw, variant, ruiter, "guh")   # [zone 6 kaart]
    q = pluimrit.bereden((x, y, z), yaw, variant, galop=False, stof=False) if ruiter else pluimrit.los((x, y, z), yaw, variant)
    W.extra += SK.quads_naar_voxel(W, q)


# [zone 6 kaart] every mount figure is also recorded for the game: W.rijdieren = [(soort, x, y, z, yaw, variant, ruiter, skin)]
# (soort bultguh | tapijt | pluimguh; x/y/z = feet on the ground, game coordinates). Only the game exports it (Z6WereldPlekken.RIJDIEREN);
# a rider (ruiter=True) is ALSO a W.wguh figure (sitting), so it does not need to be spawned twice.
def rijdier(W, soort, x, y, z, yaw, variant, ruiter, skin):
    if not hasattr(W, "rijdieren"):
        W.rijdieren = []
    W.rijdieren.append((soort, float(x), float(y), float(z), float(yaw) % 360, str(variant), bool(ruiter), skin))


# ============================================================================================ stoet en bevolking
SKINS = ["guh", "guh_mint", "guh_wolk", "guh_bloesemguh", "guh_snow", "guh_golden", "guh_choco", "guh_konijntje_roze",
         "guh_pluisguh"]


def karavaan(W, punten, hoogte_fn, n_bult=7, n_tapijt=3, seed=0, start=0.0, stap=5.5):
    """Een stoet langs een polylijn [(x, z), ...]: bultguhs met ruiters en dekens, ertussen vliegende tapijten.
    hoogte_fn(x, z) -> y van de grond."""
    rng = np.random.RandomState(seed)
    P = np.asarray(punten, float)
    seg = np.r_[0, np.cumsum(np.hypot(*np.diff(P, axis=0).T))]

    def op(s):
        i = min(np.searchsorted(seg, s) - 1, len(P) - 2)
        i = max(i, 0)
        t = (s - seg[i]) / max(1e-6, seg[i + 1] - seg[i])
        p = P[i] * (1 - t) + P[i + 1] * t
        d = P[i + 1] - P[i]
        return p, d / np.linalg.norm(d)
    s = start
    tap = set(rng.choice(range(n_bult + n_tapijt), n_tapijt, replace=False))
    for k in range(n_bult + n_tapijt):
        p, d = op(s)
        yaw = math.degrees(math.atan2(-d[0], -d[1]))
        x, z = p[0] + rng.uniform(-0.6, 0.6), p[1] + rng.uniform(-0.6, 0.6)
        y = hoogte_fn(x, z)
        if k in tap:
            zet_tapijt(W, x, y, z, yaw, patroon=k, fase=k * 1.3, skin=SKINS[rng.randint(len(SKINS))], hoogte=1.6, grond=y)
        else:
            var = ["zandroze", "saffraan", "wolkwit", "kaneel", "zandroze"][k % 5]
            zet_bultguh(W, x, y, z, yaw, variant=var, deken=k, lopend=(k % 2 == 0), skin=SKINS[rng.randint(len(SKINS))])
        s += stap + rng.uniform(-0.8, 1.2)


def bevolk(W):
    """Zet figuren op de plekken die de bouwers in het venster hebben achtergelaten (W.stal, W.plein, ...)."""
    rng = np.random.RandomState(3)
    if getattr(W, "stal", None):
        x, y, z = W.stal
        for i, (dx, dz, yaw) in enumerate(((-3, -2, 90), (0, 1, 120), (3, -1, 250))):
            zet_bultguh(W, x + dx + 0.5, y, z + dz + 0.5, yaw, variant=["saffraan", "zandroze", "wolkwit"][i], deken=i,
                        ruiter=False)
        zet_pluimguh(W, x - 9.5, y, z + 2.5, 70, "honing")
        W.wguh(x + 1.5, y, z - 4.5, 200, skin="guh_choco")
    if getattr(W, "plein", None):
        x, y, z = W.plein
        for k in range(26):
            px, pz = x + rng.uniform(-20, 20), z + rng.uniform(-9, 9)
            if math.hypot(px - x, pz - z) < 7:
                continue
            W.wguh(px, y, pz, rng.uniform(0, 360), skin=SKINS[rng.randint(len(SKINS))])
        zet_tapijt(W, x + 9.5, y, z + 5.5, 250, patroon=1, fase=0.4, skin="guh_bloesemguh", hoogte=1.4)
        zet_tapijt(W, x - 12.5, y, z - 4.5, 80, patroon=2, fase=1.4, ruiter=False, hoogte=0.9)
        zet_bultguh(W, x + 16.5, y, z - 3.5, 280, variant="kaneel", deken=3, skin="guh_golden", lopend=True)
        zet_pluimguh(W, x - 17.5, y, z + 3.5, 100, "mint", ruiter=False)
    if getattr(W, "karavanserai", None):
        x, y, z = W.karavanserai
        for i in range(4):
            zet_bultguh(W, x - 8 + i * 5.5, y, z + 7.5, 90 + i * 20, variant=["zandroze", "saffraan", "kaneel", "wolkwit"][i],
                        deken=i, ruiter=(i % 2 == 0), skin=SKINS[i])
        zet_tapijt(W, x + 6.5, y, z - 7.5, 0, patroon=3, ruiter=False, hoogte=1.0)
