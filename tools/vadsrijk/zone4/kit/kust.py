"""Zone 4 "De Lampionkust" (Guhs 1.10, Het Vadsrijk): landschapsgenerator.

Alles in ECHTE spelcoördinaten (x oost, z zuid, y absoluut), zodat dezelfde code later de echte kaart kan worden.
De plekken komen uit de wereldindeling (guhs1100-avontuur/wereldindeling/_bron/indeling.py); als die er niet is,
gebruiken we de kopie hieronder (stand 2026-11-05).

    W = SK.Wereld(x1, y1, z1, x2, y2, z2, k)      # venster in spelcoördinaten, k voxels per blok
    kust.bouw(W)                                   # zee + eilanden + begroeiing + verte (boom, walvis)
    hoogte(x, z) -> (y_boven, soort)               # maaiveld op elk punt (ook buiten een venster), voor bouwwerken

Lagen van bouw(): terrein -> water -> stroming/schuim -> begroeiing -> verte (wortel-eiland, wolkenzee, Moeder
Wolkwal met Hoogvads-silhouet). De bouwwerken (Lampionhaven, eilandthema's, bootjes) staan in haven.py en eilanden.py.
"""
import os, sys, math, functools
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schemerkit as SK  # noqa: E402

SK.R.WATER = (150, 178, 240)     # lila-blauw water; met de roze bodem eronder wordt de zee roze-lila
_tex_oud = SK.R.Res.texture


def _tex_helder(self, ref):
    """Helderder water: de alpha van de watertextuur omlaag, zodat de roze bodem en het koraal doorschemeren."""
    a = _tex_oud(self, ref)
    if "water" in ref:
        a = a.copy()
        a[..., 3] = np.minimum(1.0, a[..., 3] * 0.95)
    return a


SK.R.Res.texture = _tex_helder

# ------------------------------------------------------------------------------------------- wereldindeling
_IND = HERE   # [zone 4 kaart] indeling.py is copied next to this file (guhs1100-avontuur/wereldindeling/_bron)
try:
    sys.path.insert(0, _IND)
    import indeling as IND  # noqa: E402
    EILANDEN = list(IND.EILANDEN)
    STAM, EILAND_R, KRUIN_R = IND.STAM, IND.EILAND_R, IND.KRUIN_R
    WALVIS = dict(IND.WALVIS)
    HAVENS = dict(IND.HAVENS)
    SCHILDPAD_LUS = IND.SCHILDPAD_LUS
    WRAK = IND.WRAK
    LAND_Z4 = IND.ZONES["z4"]["land"]
except Exception:  # noqa: BLE001  (kopie, stand 2026-11-05)
    EILANDEN = [("Lampionhaven", 1850, -470, 70, 82, "hub"), ("Knuffeltij-strand", 1935, -345, 62, 36, "strand"),
                ("Strandfeesteiland", 1995, -605, 52, 36, "strand"), ("Bloemeneiland", 2085, -405, 48, 40, "bloem"),
                ("Koraalbaai", 2185, -565, 62, 60, "koraal"), ("Vliegereiland", 2300, -665, 44, 30, "vlieger"),
                ("Vuurtoreneiland", 2400, -500, 30, 26, "vuurtoren"), ("Pierkop", 2470, -565, 24, 20, "pier"),
                ("Mysterie-eiland", 2480, -330, 34, 28, "mysterie")]
    STAM, EILAND_R, KRUIN_R = (1404, -576), 238, 160
    WALVIS = dict(kop=2440, staart=3080, z=-520, breed=250, y=(170, 320))
    HAVENS = {"z4_haven": ("Lampionhaven (luchthaven)", 1800, -470, 70, "z4"),
              "z4_pier": ("De Opstijgpier", 2515, -565, 72, "z4"),
              "z3_oost": ("Oosttaksteiger", 1630, -500, 170, "z3")}
    SCHILDPAD_LUS = (2290, -420, 62, 44)
    WRAK = (2165, -700)
    LAND_Z4 = (1740, -716, 2540, -266)

ZEE = 62                    # bovenste waterblok; het wateroppervlak ligt op y 63
EIL = {e[0]: e for e in EILANDEN}
HUB = EIL["Lampionhaven"]
# eigen knipoog op de kaart: de haven-inham van Lampionhaven (oostkant, beschut) en een paar natuurlijke rotsjes
BAAI = (1890, -462, 22, 26)
ROTSJES = [("rots", 1965, -500, 9, 7, "rots"), ("rots", 2040, -520, 6, 5, "rots"), ("rots", 2250, -480, 8, 6, "rots"),
           ("rots", 2350, -585, 7, 6, "rots"), ("rots", 2120, -660, 6, 5, "rots"), ("rots", 2425, -400, 9, 7, "rots"),
           ("rots", 1790, -610, 10, 8, "rots"), ("rots", 1990, -280, 7, 6, "rots"), ("zandbank", 2030, -470, 14, 6,
                                                                                    "zandbank"),
           ("zandbank", 2240, -640, 12, 5, "zandbank"), ("rots", 2545, -470, 6, 5, "rots")]
ALLE = EILANDEN + ROTSJES
SOORTEN = ["zee", "hub", "strand", "bloem", "koraal", "vlieger", "vuurtoren", "pier", "mysterie", "rots", "zandbank",
           "knuffeltij"]


def _soort(e):
    return "knuffeltij" if e[0].startswith("Knuffeltij") else e[5]


# ------------------------------------------------------------------------------------------- hoogteveld
def _smooth(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def velden(X, Z):
    """Voor arrays X, Z (spel): (h, s, soort-index, eiland-index).
    h = hoogte van het maaiveld t.o.v. ZEE (negatief = zeebodem), s = (ongeveer) afstand tot de kust in blokken."""
    X = np.asarray(X, float)
    Z = np.asarray(Z, float)
    best_s = np.full(X.shape, -1e9)
    best_h = np.full(X.shape, -99.0)
    best_k = np.zeros(X.shape, np.int16)
    best_i = np.full(X.shape, -1, np.int16)
    wn = SK.vnoise(X, Z, cell=46, seed=11, oct=3)
    wn2 = SK.vnoise(X, Z, cell=13, seed=12, oct=2)
    # domeinvervorming: kustlijnen met inhammen en kaapjes in plaats van ovalen
    Xw = X + 13 * SK.vnoise(X, Z, cell=55, seed=13, oct=2)
    Zw = Z + 13 * SK.vnoise(X, Z, cell=55, seed=14, oct=2)
    for i, e in enumerate(ALLE):
        naam, cx, cz, rx, rz = e[:5]
        soort = _soort(e)
        # snelle kandidaat-test: alleen punten binnen 2,2x de straal + zee-rand
        d = np.sqrt(((Xw - cx) / rx) ** 2 + ((Zw - cz) / rz) ** 2)
        if not (d < 3.2).any():
            continue
        rr = min(rx, rz)
        if soort not in ("koraal", "zandbank", "rots"):   # een tweede lob: elk eiland zijn eigen vorm
            a = SK.hash3(i, 3, 7, 77) * 6.283
            lx, lz = cx + math.cos(a) * rx * 0.62, cz + math.sin(a) * rz * 0.62
            d2 = np.sqrt(((Xw - lx) / (rx * 0.55)) ** 2 + ((Zw - lz) / (rz * 0.5)) ** 2)
            d = np.minimum(d, d2 * 1.02)
        ruw = 0.20 if soort not in ("zandbank",) else 0.08
        d = d * (1 + ruw * wn + 0.06 * wn2)
        s = (1 - d) * rr
        if soort == "hub":   # haven-inham aan de oostkant
            bx, bz, brx, brz = BAAI
            db = np.sqrt(((X - bx) / brx) ** 2 + ((Z - bz) / brz) ** 2) * (1 + 0.05 * wn2)
            # de inham is open naar het oosten
            open_ = np.clip((X - bx) / 30.0, 0, 1) * np.exp(-((Z - bz) / (brz * 0.55)) ** 2)
            sb = (db - 1) * brx - open_ * 40
            s = np.minimum(s, sb)
        t = np.clip(s / rr, 0, 1)
        strand = np.clip(s, 0, 9) * 0.24
        n1 = SK.vnoise(X, Z, cell=22, seed=20 + i, oct=2)
        if soort == "hub":
            # duinrug aan de noordwestkant (Lampionduin, y ~90), naar de haven toe laag en vlak
            rug = np.exp(-(((Z - (cz - 42)) / 24.0) ** 2 + ((X - (cx + 5)) / 46.0) ** 2))
            h = strand + 3.0 * t ** 0.6 + 24.0 * rug * _smooth(2, 18, s) + 1.6 * n1 * t
        elif soort == "strand":
            h = strand + 3.0 * t ** 0.8 + 1.2 * n1 * t
        elif soort == "knuffeltij":
            h = strand * 0.8 + 2.6 * t ** 0.9 + 0.8 * n1 * t
        elif soort == "bloem":
            h = strand + 8.0 * t ** 0.9 + 3.0 * n1 * t
        elif soort == "koraal":
            # atol: een ring met een smalle opening in het zuiden, binnenin de roze lagune
            ring = np.exp(-((t - 0.16) / 0.11) ** 2)
            hoek = np.arctan2(Z - cz, X - cx)
            gat = np.exp(-((hoek - math.pi / 2) / 0.22) ** 2)
            h = strand * 0.6 + 6.0 * ring * (1 + 0.5 * n1) * (1 - gat) - gat * 3 * (t > 0.05)
            lag = _smooth(0.28, 0.5, t)
            h = h * (1 - lag) + lag * (-3.0 - 9.0 * _smooth(0.45, 0.95, t) + 2.5 * n1)
        elif soort == "vlieger":
            h = strand + 27.0 * _smooth(0.0, 1.0, t) ** 1.1 + 2.0 * n1 * t
        elif soort == "vuurtoren":
            h = strand * 0.5 + 15.0 * _smooth(1.5, 6, s) + 9.0 * t ** 0.6 + 3.0 * n1
        elif soort == "pier":
            h = strand * 0.4 + 6.0 * _smooth(1, 5, s) + 4.0 * t + 1.5 * n1
        elif soort == "mysterie":
            h = strand * 0.6 + 7.0 * _smooth(1, 6, s) + 9.0 * t ** 0.8 + 4.0 * n1
        elif soort == "rots":
            h = 3.0 * _smooth(0, 3, s) + 6.0 * t ** 0.7 + 2.0 * n1
        else:  # zandbank
            h = 0.8 * _smooth(0, 4, s)
        # buiten de kust: zeebodem (ondiep bij de kust, dieper verderop)
        zee = -1.0 - 26.0 * (1 - np.exp(np.minimum(s, 0) / 30.0)) + 1.5 * wn2
        h = np.where(s > 0, np.maximum(h, 0.6 if soort != "koraal" else -20), zee)
        if soort == "koraal":
            h = np.where(s > 0, h, zee)
        m = s > best_s
        best_s = np.where(m, s, best_s)
        best_h = np.where(m, h, np.maximum(best_h, np.minimum(h, -1)))
        best_k = np.where(m, SOORTEN.index(soort), best_k)
        best_i = np.where(m, i, best_i)
    # open zee ver van alles
    ver = best_s < -60
    best_h = np.where(ver, -27 + 2 * wn, best_h)
    best_k = np.where(best_s < 0, 0, best_k)
    return best_h, best_s, best_k, best_i


@functools.lru_cache(maxsize=None)
def _hoogte(x, z):
    h, s, k, i = velden(np.array([[x + 0.5]]), np.array([[z + 0.5]]))
    y = int(math.floor(ZEE + h[0, 0] + 0.5))
    return y, SOORTEN[int(k[0, 0])] if y > ZEE else "zee"


def hoogte(x, z):
    """y van het bovenste maaiveldblok op blok (x, z) (int) en de soort ('zee' als het onder water ligt)."""
    return _hoogte(int(math.floor(x)), int(math.floor(z)))


def _hoogte_oud(x, z):
    h, s, k, i = velden(np.array([[x]]), np.array([[z]]))
    y = int(math.floor(ZEE + h[0, 0] + 0.5))
    return y, SOORTEN[int(k[0, 0])] if y > ZEE else "zee"


# ------------------------------------------------------------------------------------------- terrein
LAND = {  # soort -> (top, onder, rots)
    "hub": ("guhs:knuffelgras", "minecraft:pink_terracotta", "guhs:roze_knuffelsteen"),
    "strand": ("guhs:roze_zand", "guhs:roze_zand", "minecraft:pink_terracotta"),
    "knuffeltij": ("guhs:roze_zand", "guhs:roze_zand", "minecraft:pink_terracotta"),
    "bloem": ("guhs:knuffelgras", "minecraft:pink_terracotta", "guhs:roze_knuffelsteen"),
    "koraal": ("guhs:knuffelgras", "guhs:roze_zand", "minecraft:pink_terracotta"),
    "vlieger": ("guhs:knuffelgras", "minecraft:pink_terracotta", "minecraft:calcite"),
    "vuurtoren": ("guhs:knuffelgras", "minecraft:calcite", "minecraft:calcite"),
    "pier": ("guhs:knuffelgras", "minecraft:pink_terracotta", "minecraft:calcite"),
    "mysterie": ("minecraft:moss_block", "minecraft:light_gray_terracotta", "guhs:gladde_knuffelsteen"),
    "rots": ("minecraft:calcite", "minecraft:calcite", "minecraft:calcite"),
    "zandbank": ("guhs:roze_zand", "guhs:roze_zand", "guhs:roze_zand"),
}


def terrein(W, water=True):
    """Bodem, eilanden en water in venster W."""
    h, s, k, ii = velden(W.XX, W.ZZ)
    top = np.floor(ZEE + h + 0.5)
    W.top = top
    W.soort = k
    W.kustafst = s
    # helling (voor rotswanden)
    gx, gz = np.gradient(top, 1.0 / W.k)
    helling = np.hypot(gx, gz)
    rotsig = np.isin(k, [SOORTEN.index(n) for n in ("vuurtoren", "rots", "mysterie", "pier")])
    steil = np.where(rotsig, helling > 1.35, helling > 2.6)
    y_bot = W.b[1]
    ids_top = np.zeros(top.shape, np.uint16)
    ids_ond = np.zeros(top.shape, np.uint16)
    ids_rots = np.zeros(top.shape, np.uint16)
    zand, zand_d, bodem = W.id("guhs:roze_zand"), W.id("minecraft:pink_concrete_powder"), W.id("minecraft:pink_terracotta")
    for naam, (t_, o_, r_) in LAND.items():
        m = k == SOORTEN.index(naam)
        ids_top[m], ids_ond[m], ids_rots[m] = W.id(t_), W.id(o_), W.id(r_)
    # strandrand: alles tot 2,5 boven zee is zand
    laag = (top <= ZEE + 2) & (k > 0)
    ids_top[laag & (ids_top != W.id("minecraft:calcite"))] = zand
    ids_ond[laag] = zand
    ids_top[steil & (k > 0)] = ids_rots[steil & (k > 0)]
    # zeebodem: roze zand ondiep, poederroze dieper, zand in de diepte
    zm = top <= ZEE
    diep = ZEE - top
    ids_top[zm] = np.where(diep[zm] < 6, zand, np.where(diep[zm] < 15, zand_d, bodem))
    ids_ond[zm] = ids_top[zm]
    ids_rots[ids_rots == 0] = bodem    # [zone 4 kaart] the sea had no rock layer (air under a 4-block floor): solid to y 0
    mix = W.kies(["minecraft:pink_terracotta", "guhs:roze_knuffelsteen", "minecraft:calcite"], [5, 2, 2],
                 W.XX, 0, W.ZZ, seed=3)
    ids_rots = np.where(ids_rots == W.id("guhs:roze_knuffelsteen"), mix, ids_rots)

    def laagfn(y, m):
        d = top - y
        return np.where(d < 1, ids_top, np.where(d < 4, np.where(steil, ids_rots, ids_ond), ids_rots))
    W.kolom(np.full(top.shape, y_bot), top, None, laagfn=laagfn)
    if water:
        wm = top < ZEE
        W.kolom(top + 1, np.full(top.shape, ZEE), "minecraft:water", mask=wm)


def zy(W, x, z):
    """Maaiveld (y bovenste blok) uit het venster als het erin ligt, anders uit het hoogteveld."""
    i, j = W.vx(x), W.vz(z)
    if hasattr(W, "top") and 0 <= i < W.top.shape[0] and 0 <= j < W.top.shape[1]:
        return int(W.top[i, j])
    return hoogte(x, z)[0]


# ------------------------------------------------------------------------------------------- stroming / schuim
def schuim(W):
    """Witte schuimrandjes langs de kust (op het water) en de Kringstroom aan de rand van de zone."""
    if W.k < 0.5:
        sl = 1
    else:
        sl = 1
    s = W.kustafst
    top = W.top
    yv = W.vy(ZEE + 1)
    if not (0 <= yv < W.v.shape[1]):
        return
    n = SK.vnoise(W.XX, W.ZZ, cell=7, seed=33, oct=2)
    kust = (s < 0) & (s > -2.2 - 1.5 * n) & (top < ZEE) & (n > -0.35)
    # Kringstroom: een grote ellips om de zone (natuurlijke rand), met golvende strepen schuim
    cx, cz = 2150, -470
    d = np.sqrt(((W.XX - cx) / 470.0) ** 2 + ((W.ZZ - cz) / 255.0) ** 2)
    hoek = np.arctan2(W.ZZ - cz, W.XX - cx)
    streep = np.sin(hoek * 40 + d * 60 + 4 * SK.vnoise(W.XX, W.ZZ, cell=30, seed=34)) > 0.55
    ring = (np.abs(d - 1.0) < 0.035 + 0.02 * n) & streep & (top < ZEE - 2)
    # niet over de walvis-zee (oost) of de wolkenzee (west)
    ring &= (W.XX > 1700) & (W.XX < 2620)
    m = kust | ring
    W.v[:, yv, :][m & (W.v[:, yv, :] == 0)] = W.id("minecraft:white_carpet")


# ------------------------------------------------------------------------------------------- begroeiing
def palm(W, x, y, z, h=9, seed=0, roze=False):
    """Kromme palm (stam van guhwaii-palm, kruin van palmblad), werkt op elke schaal."""
    a = SK.hash3(x, 1, z, seed) * 6.28
    dx, dz = math.cos(a), math.sin(a)
    pts = [(x + 0.5 + dx * (t / h) ** 2 * 3, y + t, z + 0.5 + dz * (t / h) ** 2 * 3) for t in range(0, h + 1, 3)]
    W.buis(pts, [0.55] * len(pts), "guhs:guhwaii_palm_stam[axis=y]")
    tx, ty, tz = pts[-1]
    blad = "minecraft:cherry_leaves" if roze else "guhs:guhwaii_palm_blad"
    for b in range(6):
        bh = b * math.pi / 3 + a
        W.buis([(tx, ty + 0.5, tz), (tx + math.cos(bh) * 2.6, ty + 1.2, tz + math.sin(bh) * 2.6),
                (tx + math.cos(bh) * 4.6, ty - 0.6, tz + math.sin(bh) * 4.6)], [0.6, 0.6, 0.5], blad)
    W.bol((tx, ty + 0.6, tz), 1.2, "guhs:guhwaii_palm_blad")


def bloesemboom(W, x, y, z, h=7, r=4.0, seed=0, blad="minecraft:cherry_leaves"):
    """Roze bloesemboompje (kersenstam, bolle kruin), elke schaal."""
    W.cilinder(x + 0.5, z + 0.5, 0.6, y, y + h, "minecraft:cherry_log[axis=y]")
    for j in range(3):
        a = SK.hash3(x, j, z, seed + 5) * 6.28
        c = (x + 0.5 + math.cos(a) * r * 0.45, y + h + (j - 1) * 0.8, z + 0.5 + math.sin(a) * r * 0.45)
        W.bol(c, r * (0.75 + 0.2 * j / 2), blad, ry=r * 0.65,
              mixfn=lambda X, Y, Z, m: W.kies([blad, "guhs:gloeibladeren_roze", "guhs:wolkjeshout_bladeren_roze"],
                                              [6, 2, 1], X, Y, Z, seed=7))


PLANTJES = {
    "gras": (["guhs:weide_roze_gras", "guhs:roze_gras", "minecraft:short_grass", "guhs:roze_guhbloem",
              "minecraft:pink_tulip", "guhs:roze_hibiscus", "minecraft:allium", "minecraft:oxeye_daisy"],
             [10, 8, 3, 2, 2, 1, 1, 1]),
    "zand": (["guhs:schelpje", "minecraft:dead_bush", "guhs:roze_gras"], [3, 1, 2]),
    "grijs": (["minecraft:short_grass", "minecraft:fern", "minecraft:dead_bush"], [4, 2, 1]),
}


def plantjes(W, dicht=0.22, soortfilter=None):
    """Losse plantjes/bloemen bovenop het maaiveld (alleen bij k >= 1)."""
    if W.k < 1:
        return
    top = W.top
    k = W.soort
    X, Y, Z = W.v.shape
    gras = np.isin(W.v[np.arange(X)[:, None], np.clip(((top - W.b[1]) * W.k).astype(int), 0, Y - 1),
                       np.arange(Z)[None, :]], [W.id("guhs:knuffelgras")])
    zand = (top > ZEE) & ~gras & (k > 0)
    mos = k == SOORTEN.index("mysterie")
    hsh = SK.hash3(W.XX, 7, W.ZZ, 41)
    yv = np.clip(((top + 1 - W.b[1]) * W.k).astype(int), 0, Y - 1)
    ok = yv < Y - 1
    if soortfilter is not None:
        ok &= soortfilter
    for naam, m, p in (("gras", gras & ~mos, dicht), ("zand", zand, 0.012), ("grijs", mos & gras, dicht * 0.6)):
        bl, gw = PLANTJES[naam]
        sel = m & ok & (hsh < p)
        ids = W.kies(bl, gw, W.XX, 3, W.ZZ, seed=9)
        ii, jj = np.nonzero(sel)
        vrij = W.v[ii, yv[ii, jj], jj] == 0
        W.v[ii[vrij], yv[ii, jj][vrij], jj[vrij]] = ids[ii[vrij], jj[vrij]]


def begroeiing(W):
    """Palmen langs de stranden, bloesemboompjes op het gras; op een vast rooster met jitter (deterministisch)."""
    x1, _, z1, x2, _, z2 = W.b
    st = 9
    for gx in range(int(x1 // st) * st, int(x2) + st, st):
        for gz in range(int(z1 // st) * st, int(z2) + st, st):
            hsh = SK.hash3(gx, 0, gz, 5)
            x = gx + int(SK.hash3(gx, 1, gz, 6) * st)
            z = gz + int(SK.hash3(gx, 2, gz, 6) * st)
            if not W.binnen(x, W.b[1], z, marge=0):
                continue
            i, j = W.vx(x), W.vz(z)
            if not (0 <= i < W.top.shape[0] and 0 <= j < W.top.shape[1]):
                continue
            y = int(W.top[i, j])
            if y <= ZEE + 1:
                continue
            soort = SOORTEN[int(W.soort[i, j])]
            s = W.kustafst[i, j]
            if getattr(W, "vrij", None) is not None and not W.vrij(x, z):
                continue
            if soort in ("strand", "knuffeltij", "koraal", "hub", "pier") and s < 16 and hsh < (0.5 if soort == "strand" else 0.3):
                palm(W, x, y + 1, z, h=7 + int(hsh * 12), seed=int(hsh * 99), roze=hsh < 0.08)
            elif soort in ("bloem", "hub", "vlieger", "pier", "koraal") and s > 10 and hsh < 0.16:
                bloesemboom(W, x, y + 1, z, h=4 + int(hsh * 20), r=3.2 + hsh * 8, seed=int(hsh * 50))
            elif soort == "mysterie" and hsh < 0.3:
                W.cilinder(x + 0.5, z + 0.5, 0.5, y + 1, y + 6, "minecraft:pale_oak_log[axis=y]")
                W.bol((x + 0.5, y + 7, z + 0.5), 2.6, "minecraft:pale_oak_leaves", ry=2.0)


# ------------------------------------------------------------------------------------------- verte
def wortel_eiland(W):
    """De Schemerwortel (zone 3) als ECHTE structuur in de verte: wortel-eiland in de wolkenzee, smalle hoge stam
    tot y ~292, getrapte kruin (r 160/112/68) met roze bloesem, het nest bovenop. Vereenvoudigd (silhouet)."""
    sx, sz = STAM
    if not W.raakt(sx - 260, 0, sz - 260, sx + 260, 330, sz + 260):
        return
    # wortel-eiland (groen-goud, roze randjes) dat uit de wolkenzee steekt
    W.bol((sx, 40, sz), EILAND_R - 10, "guhs:knuffelgras", ry=45, ymin=40,
          mixfn=lambda X, Y, Z, m: W.kies(["minecraft:moss_block", "guhs:knuffelgras", "minecraft:pink_terracotta"],
                                          [5, 3, 2], X, Y, Z, seed=2))
    # wortels die over het eiland kruipen
    for a in range(12):
        h = a * math.pi / 6 + 0.2
        W.buis([(sx + math.cos(h) * 30, 120, sz + math.sin(h) * 30),
                (sx + math.cos(h) * 90, 82, sz + math.sin(h) * 90),
                (sx + math.cos(h) * 190, 70, sz + math.sin(h) * 190)], [14, 9, 5], "minecraft:spruce_log[axis=y]")
    # stam, smal en hoog
    W.cilinder(sx, sz, 36, 70, 250, "minecraft:spruce_log[axis=y]",
               mixfn=lambda X, Y, Z, m: W.kies(["minecraft:spruce_log[axis=y]", "minecraft:dark_oak_log[axis=y]"],
                                               [3, 1], X // 3, Y // 4, Z // 3, seed=4))
    # kruin in drie verdiepingen + nest
    blad = lambda X, Y, Z, m: W.kies(["minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves",  # noqa: E731
                                      "minecraft:cherry_leaves", "guhs:gloeibladeren_roze"], [4, 3, 4, 1],
                                     X // 4, Y // 4, Z // 4, seed=8)
    for (r, y0, dy) in ((KRUIN_R, 208, 20), (112, 240, 18), (68, 266, 16)):
        W.bol((sx, y0, sz), r, None, ry=dy, mixfn=blad)
        for b in range(10):
            h = b * 0.63 + y0
            W.bol((sx + math.cos(h) * r * 0.82, y0 - dy * 0.4, sz + math.sin(h) * r * 0.82), r * 0.3, None,
                  ry=dy * 0.9, mixfn=blad)
    W.cilinder(sx, sz, 22, 282, 292, "minecraft:stripped_spruce_log[axis=y]", hollow=6)
    W.label(sx, 300, sz, "De Schemerwortel (zone 3)")


def wolkenzee(W):
    """Wolkenzee rond het wortel-eiland, met de Wolkenval: wolken die over de rand in de roze zee storten."""
    x1, _, z1, x2, _, z2 = W.b
    if x1 > 1760:
        return
    X, Z = W.XX, W.ZZ
    d = np.hypot(X - STAM[0], Z - STAM[1])
    n = SK.vnoise(X, Z, cell=40, seed=60, oct=3)
    rand_x = 1690 + 30 * n   # oostrand van de wolkenzee
    # een ronde, pluizige wolkenbank om het wortel-eiland (geen vak), met de Wolkenval aan de oostrand
    wolk = (X < rand_x) & (d > EILAND_R - 30) & (d < 330 + 45 * n)
    top = 72 + 8 * (SK.vnoise(X, Z, cell=18, seed=61, oct=2) + 1) - np.clip((X - (rand_x - 40)) / 40, 0, 1) * 6
    bot = np.full(X.shape, 48.0)
    W.kolom(bot, top, None, mask=wolk, laagfn=lambda y, m: W.kies(["guhs:wolkenblok_wit", "guhs:wolkenblok_roze"],
                                                                  [6, 1], X, y, Z, seed=62))
    # de Wolkenval: een gordijn van wolkenpluimen langs de rand, van de wolkenzee naar beneden
    val = (np.abs(X - rand_x) < 7) & (d > EILAND_R) & (d < 330 + 45 * n)
    W.kolom(np.full(X.shape, float(ZEE - 2)), top - 2, "guhs:wolkenblok_wit", mask=val & (n > -0.4))
    W.label(1690, 90, -700, "de Wolkenval (wolkenzee -> roze zee)")


def walvis(W, kaal=False):
    """Moeder Wolkwal met Hoogvads op haar rug, als silhouet (de echte stad maakt een andere ontwerper).
    Kop in het westen (x 2440), staart in het oosten (x 3080), y ~170-320."""
    k0, k1, wz = WALVIS["kop"], WALVIS["staart"], WALVIS["z"]
    if not W.raakt(k0 - 60, 120, wz - 200, k1 + 120, 340, wz + 200):
        return
    yc = 215
    L = k1 - k0
    pts, rz, ry = [], [], []
    for i in range(15):
        t = i / 14
        x = k0 + t * L * 0.9
        prof = math.sin(math.pi * min(1.0, (t + 0.08) / 1.0)) ** 0.6
        pts.append((x, yc - 6 * math.sin(t * 3.1), wz))
        rz.append(max(6, WALVIS["breed"] / 2 * 0.52 * prof))
        ry.append(max(5, 52 * prof))
    huid = lambda X, Y, Z, m: np.where(Y < yc - 22 + 6 * np.sin(X / 23.0), W.id("guhs:wolkenblok_wit"),  # noqa: E731
                                       W.kies(["minecraft:light_blue_concrete", "guhs:pastelbaksteen_hemelsblauw"],
                                              [6, 1], X // 9, Y // 9, Z // 9, seed=70))
    for i in range(len(pts) - 1):
        p0, p1 = pts[i], pts[i + 1]
        r = (rz[i] + rz[i + 1]) / 2
        W.buis([p0, p1], [r, r], None, mixfn=huid, plat=(ry[i] + ry[i + 1]) / 2 / r)
    # staart met vinnen
    tx = k0 + L * 0.9
    W.buis([(tx, yc, wz), (k1 + 10, yc + 18, wz)], [14, 6], None, mixfn=huid)
    for sgn in (-1, 1):
        W.buis([(k1, yc + 20, wz), (k1 + 40, yc + 30, wz + sgn * 60)], [8, 3], None, mixfn=huid, plat=0.3)
        W.buis([(k0 + 120, yc - 25, wz + sgn * 70), (k0 + 160, yc - 40, wz + sgn * 120)], [12, 4], None, mixfn=huid,
               plat=0.3)
    # oog (lief, met glimmertje) en blos aan beide kanten
    for sgn in (-1, 1):
        ez = wz + sgn * (rz[2] * 0.93)
        W.bol((k0 + 70, yc + 6, ez), 6.5, "minecraft:black_concrete", ry=7.5, rz=2)
        W.bol((k0 + 68, yc + 9, ez + sgn * 1.5), 2.2, "minecraft:white_concrete", rz=1)
        W.bol((k0 + 82, yc - 10, ez - sgn * 2), 5, "guhs:knuffelblok_roze", ry=3, rz=2.5)
    W.label(k0 + 60, yc + 50, wz, "Moeder Wolkwal")
    if kaal:
        return
    # Hoogvads op de rug: een silhouet van roze torens, de kathedraal en wolkenweiden
    rug = yc + 48
    W.bol((k0 + L * 0.45, rug - 8, wz), L * 0.36, "guhs:wolkenweide_gras", ry=10, rz=WALVIS["breed"] * 0.3,
          ymin=rug - 4)
    rng = np.random.default_rng(14)
    kat = (2740, wz)
    for n in range(46):
        x = k0 + 110 + rng.uniform(0, L * 0.68)
        z = wz + rng.normal(0, WALVIS["breed"] * 0.12)
        if math.hypot(x - kat[0], z - kat[1]) < 30:
            continue
        r = rng.uniform(3, 7)
        h = rng.uniform(12, 40) * (1.25 - abs(z - wz) / WALVIS["breed"])
        kl = ["guhs:pastelbaksteen_roze", "guhs:pastelbaksteen_wolkje", "guhs:pastelbaksteen_lavendel",
              "guhs:pleister_roze"][n % 4]
        W.cilinder(x, z, r, rug, rug + h, kl)
        for j in range(int(r) + 2):  # spits
            W.cilinder(x, z, max(0.6, r + 1 - j * (r + 1) / (r + 2) * 1.0), rug + h + j * 2, rug + h + j * 2 + 1,
                       "guhs:guh_dakpan_roze")
    # de kathedraal: hoog schip + twee torens met oortjes
    W.wfill(kat[0] - 40, rug, kat[1] - 14, kat[0] + 20, rug + 46, kat[1] + 14, "guhs:pastelbaksteen_wolkje")
    for sgn in (-1, 1):
        tz = kat[1] + sgn * 16
        W.cilinder(kat[0] - 44, tz, 9, rug, rug + 82, "guhs:pastelbaksteen_roze")
        for j in range(12):
            W.cilinder(kat[0] - 44, tz, max(0.8, 10 - j * 0.85), rug + 82 + j * 3, rug + 85 + j * 3,
                       "guhs:guh_dakpan_roze")
    for sgn in (-1, 1):  # guh-oortjes op het dak van het schip
        W.bol((kat[0] - 5, rug + 54, kat[1] + sgn * 9), 7, "guhs:knuffelblok_roze", ry=12, rz=3)
    W.label(kat[0], rug + 120, kat[1], "Hoogvads (zone 5)")
    # wolkjes onder de buik
    for n in range(14):
        x = k0 + 40 + rng.uniform(0, L)
        W.bol((x, yc - 52 + rng.uniform(-8, 8), wz + rng.normal(0, 60)), rng.uniform(10, 22), "guhs:wolkenblok_wit",
              ry=rng.uniform(5, 9))


def bouw(W, delen=("terrein", "schuim", "begroeiing", "plantjes", "verte")):
    if "terrein" in delen:
        terrein(W)
    if "verte" in delen:
        wolkenzee(W)
        wortel_eiland(W)
        walvis(W)
    if "schuim" in delen:
        schuim(W)
    if "begroeiing" in delen:
        begroeiing(W)
    if "plantjes" in delen:
        plantjes(W)


# ------------------------------------------------------------------------------------------- zee tot de horizon
def zee_post(r_fade=None, horizon=(255, 226, 236), kleur=None):
    """Nabewerking: de zee loopt door buiten het venster (geen doos-randen). Pixels die leeg zijn of op de
    afgesneden zijkanten onder de waterlijn vallen, krijgen de zee die de kijkstraal op y = ZEE + 1 raakt; verder
    weg vervaagt die naar de horizon (en daarachter de lucht). De zeekleur wordt geproefd aan de rand van het venster."""
    def f(rgb, al, P):
        C = SK.LAST_CAM
        cam, W, sc, ox, oy = C["cam"], C["W"], C["sc"], C["ox"], C["oy"]
        x1, y1, z1, x2, y2, z2 = W.b
        k = W.k
        H, Wd = al.shape
        a = (np.arange(Wd, dtype=np.float32) + 0.5 - ox) / sc
        b = -(np.arange(H, dtype=np.float32) + 0.5 - oy) / sc
        rt, up, d = cam.right, cam.up, cam.d
        t = (((ZEE + 1) - y1) * k - b * up[1]) / d[1]
        hx = (a[None, :] * rt[0] + b[:, None] * up[0] + t[:, None] * d[0]) / k + x1
        hz = (a[None, :] * rt[2] + b[:, None] * up[2] + t[:, None] * d[2]) / k + z1
        binnen = (hx >= x1) & (hx < x2) & (hz >= z1) & (hz < z2)
        Px, Py, Pz = P[..., 0], P[..., 1], P[..., 2]
        ok = ~np.isnan(Px)
        e = 1.2 / k
        Pxn, Pyn, Pzn = (np.nan_to_num(v, nan=-1e9) for v in (Px, Py, Pz))
        rand = ok & (Pyn < ZEE + 1.4) & ((Pxn < x1 + e) | (Pxn > x2 - e) | (Pzn > z2 - e) | (Pzn < z1 + e))
        onder = ok & (Pyn < ZEE + 1.4)    # ook wat je door het doorzichtige water-snijvlak heen ziet
        vervang = ((al < 0.02) | rand | onder) & ~binnen & (d[1] > 0)
        if kleur is None:
            m = ok & (np.abs(Pyn - (ZEE + 1)) < 0.7) & ~rand & (
                (Pxn < x1 + 40) | (Pxn > x2 - 40) | (Pzn > z2 - 40) | (Pzn < z1 + 40))
            zk = np.median(rgb[m], 0) if m.sum() > 50 else np.array((0.56, 0.62, 0.86))
        else:
            zk = np.array(kleur, np.float32) / 255.0
        rf = r_fade or 0.9 * max(x2 - x1, z2 - z1)
        dd = np.hypot(np.maximum(np.maximum(x1 - hx, hx - x2), 0), np.maximum(np.maximum(z1 - hz, hz - z2), 0))
        tt = np.clip(dd / rf, 0, 1) ** 0.75
        hk = np.array(horizon, np.float32) / 255.0
        golf = SK.vnoise(hx, hz, cell=5, seed=95, oct=2) * 0.05 * (1 - tt)
        col = zk[None, None, :] * (1 - tt[..., None]) + hk * tt[..., None] + golf[..., None]
        alpha = 1 - np.clip((dd - rf) / (rf * 0.5), 0, 1)
        rgb = rgb.copy()
        al = al.copy()
        rgb[vervang] = col[vervang]
        al[vervang] = alpha[vervang]
        return rgb, al
    return f


def rand_post(breedte=40.0, horizon=(255, 226, 236)):
    """Nabewerking: het venster loopt aan alle kanten zacht uit in de nevel/lucht (geen doosranden).
    Alles binnen `breedte` blokken van de rand van het venster (x/z) vervaagt naar de horizonkleur en wordt
    doorzichtig, zodat de lucht erachter doorkomt: de zee lijkt tot in de verte door te lopen."""
    hk = np.array(horizon, np.float32) / 255.0

    def f(rgb, al, P):
        W = SK.LAST_CAM["W"]
        x1, y1, z1, x2, y2, z2 = W.b
        Px = P[..., 0].copy()
        Pz = P[..., 2].copy()
        leeg = np.isnan(Px)
        if leeg.any():
            # pixels zonder dieptewaarde (alleen doorzichtig water, de straal verlaat het venster): neem het punt
            # waar de kijkstraal het wateroppervlak raakt
            C = SK.LAST_CAM
            cam, sc, ox, oy, k = C["cam"], C["sc"], C["ox"], C["oy"], W.k
            H, Wd = Px.shape
            a = (np.arange(Wd, dtype=np.float32) + 0.5 - ox) / sc
            b = -(np.arange(H, dtype=np.float32) + 0.5 - oy) / sc
            rt, up, d = cam.right, cam.up, cam.d
            tt = (((ZEE + 1) - y1) * k - b * up[1]) / d[1]
            hx = (a[None, :] * rt[0] + b[:, None] * up[0] + tt[:, None] * d[0]) / k + x1
            hz = (a[None, :] * rt[2] + b[:, None] * up[2] + tt[:, None] * d[2]) / k + z1
            Px[leeg] = hx[leeg]
            Pz[leeg] = hz[leeg]
        # elliptisch: de zee loopt rondom gelijkmatig uit (geen ruit/vierkant als de camera schuin staat)
        cx, cz, rx, rz = (x1 + x2) / 2, (z1 + z2) / 2, (x2 - x1) / 2, (z2 - z1) / 2
        q = np.sqrt(((Px - cx) / rx) ** 2 + ((Pz - cz) / rz) ** 2)
        ex = (1 - q) * min(rx, rz)
        t = np.clip(ex / breedte, 0, 1)
        t = t * t * (3 - 2 * t)
        rgb = rgb * t[..., None] + hk * (1 - t[..., None])
        al = al * np.clip(t * 1.8 - 0.15, 0, 1)
        return rgb, al
    return f
