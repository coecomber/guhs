"""Zone 6 "Het Zijdeland": landschap in ECHTE spelcoördinaten (x oost, z zuid), als kaartgenerator bruikbaar.

Vak (tegelrekengrens, nergens zichtbaar): x 3280..4180, z -1060..-460 (900 x 600), tegels tx 51..65, tz -17..-8.
Alles is een functie van (x, z): hoogte(), materiaal, water. bouw(W) vult elk venster (schemerkit.Wereld) met
precies hetzelfde land, zodat een shot een uitsnede is en een tegel-export later dezelfde wereld geeft.

Plekken (POI) staan in PLEK; de lijnen (kloof, rivier, karavaanweg) in KLOOF, RIVIER, WEG.
"""
import math
import numpy as np
import schemerkit as SK
import zijbouw as ZB
from zpalet import Z, b

VAK = (3280, -1060, 4180, -460)
RAND = (3264, -1140, 4276, -396)   # [zone 6 kaart] outer coast lines (west x, north z, east x, south z): see velden()
Q_RICHEL = (0.46, 0.60, 0.72)   # kloofdoorsnede: grens bodem | richel 1 | richel 2 | richel 3 (als deel van de halve breedte)
DY_RICHEL = (16, 32, 50)        # hoogte van de richels boven de kloofbodem
ZEE = 63            # waterniveau zee/rivier (zoals de Eilandzee)
PLATEAU = 120       # hoogte van het westelijke zandsteenplateau
LAAG = 69           # het laagland (duinvoet, oase, stad)

# ------------------------------------------------------------------------------------------------ lijnen
# De Rozenkloof: van de zee (west) naar het plateau (oost). (x, z, halve breedte, bodem-y)
KLOOF = [(3236, -744, 46, 54), (3270, -742, 42, 60), (3300, -737, 40, 64), (3340, -724, 40, 66),
         (3380, -716, 42, 68), (3420, -710, 41, 70), (3455, -702, 36, 72), (3478, -698, 32, 74),
         (3500, -696, 28, 74)]
KLOOF_EIND = 3478   # sheer eindwand (de Schatkamer is erin uitgehouwen, kijkt naar het westen)
# De Siq: de smalle kloof van de kloofbodem schuin omhoog naar het plateau (noordoost). (x, z, halve breedte, bodem)
SIQ = [(3452, -716, 7, 72), (3470, -738, 6, 80), (3490, -758, 5, 92), (3512, -774, 5, 104), (3540, -786, 6, 116),
       (3566, -794, 8, 121)]
# De Saffraanrivier: bron in de Moederoase, door Saffraanpoort, door de Palmenkloven naar zee. (x, z, halve breedte)
RIVIER = [(4092, -772, 10), (4040, -796, 9), (3975, -826, 10), (3905, -848, 11), (3840, -850, 12),
          (3775, -842, 13), (3712, -856, 13), (3660, -888, 13), (3612, -924, 12), (3556, -952, 12),
          (3490, -970, 13), (3420, -984, 14), (3350, -996, 16), (3290, -1004, 20), (3230, -1008, 26)]
# De karavaanweg Rozenkloof -> Saffraanpoort -> (Zoutvlakte) -> Moederoase
WEG = [(3566, -794), (3596, -806), (3622, -826), (3640, -846)]
# De Duinroute: van het zuiden door de Roze Duinen naar de Zuidpoort van Saffraanpoort (de karavaanstoet in de render)
WEG_ZUID = [(3700, -470), (3708, -530), (3716, -580), (3726, -625), (3738, -672), (3750, -712), (3758, -744)]
WEG_OOST = [(3830, -820), (3880, -780), (3920, -720), (3945, -660), (3990, -640), (4050, -680), (4090, -715)]

SAFFRAAN = (3762, -846)          # midden van de stad (paleisplein)
STAD_R = (128, 104)              # stadsellips (x, z)
ZOUT = (3945, -598, 112, 118)    # Zoutvlakte-ellips
OASE = (4096, -768, 46)          # het meer van de Moederoase
TOREN = (4140, -688)             # De Sterrendovende Toren

PLEK = {
    "rozenkloof_luchthaven": (3398, 124, -772),
    "rozenkloof_aankomst": (3398, 125, -760),
    "rozenkloof_plein": (3400, 69, -714),
    "rozenkloof_schatkamer": (3418, 69, -688),
    "saffraanpoort_paleis": (3762, 70, -872),
    "saffraanpoort_bazaar": (3742, 69, -812),
    "saffraanpoort_poort_west": (3640, 69, -846),
    "palmbaken": (3478, 121, -906),
    "duinbaken": (3655, 96, -560),
    "zoutbaken": (3930, 69, -612),
    "ruinebaken": (4045, 108, -1000),
    "oasebaken": (4062, 70, -842),
    "verzonken_sterrenwacht": (3985, 72, -962),
    "zoutgrot": (3874, 69, -505),
    "wormelien": (3700, 80, -600),
    "zoutkolos": (3995, 69, -560),
    "toren": (TOREN[0], 70, TOREN[1]),
    "moederoase": (OASE[0], 66, OASE[1]),
}
GEBIEDEN = [  # naam, middelpunt (x, z), lv
    ("De Roze Duinen", (3660, -640), "60-62"),
    ("De Palmenkloven", (3460, -960), "62-64"),
    ("De Zoutvlakte", (3945, -598), "64-66"),
    ("Het Ruïnedal", (3985, -975), "66-68"),
    ("De Moederoase", (4100, -780), "68-70"),
]


# ------------------------------------------------------------------------------------------------ wiskunde
def smooth(a, b_, x):
    t = np.clip((x - a) / (b_ - a), 0, 1)
    return t * t * (3 - 2 * t)


def lijn_afstand(X, Z_, pts):
    """Afstand tot een polylijn + interpolatieparameter (segment-index + t) en de geïnterpoleerde extra waarden."""
    P = np.asarray([p[:2] for p in pts], float)
    E = np.asarray([p[2:] for p in pts], float)
    best = np.full(X.shape, np.inf)
    val = np.zeros(X.shape + (E.shape[1],))
    tpar = np.zeros(X.shape)
    for i in range(len(P) - 1):
        a, c = P[i], P[i + 1]
        d = c - a
        L2 = float(d @ d)
        t = np.clip(((X - a[0]) * d[0] + (Z_ - a[1]) * d[1]) / L2, 0, 1)
        qx, qz = a[0] + t * d[0], a[1] + t * d[1]
        dist = np.hypot(X - qx, Z_ - qz)
        m = dist < best
        best = np.where(m, dist, best)
        v = E[i][None] * (1 - t[..., None]) + E[i + 1][None] * t[..., None] if E.shape[1] else val
        val = np.where(m[..., None], v, val) if E.shape[1] else val
        tpar = np.where(m, i + t, tpar)
    return best, val, tpar


def ruis(X, Z_, cel, seed, oct=3):
    return SK.vnoise(X, Z_, cell=cel, seed=seed, oct=oct)


# ------------------------------------------------------------------------------------------------ het land
def velden(X, Z_):
    """Alle 2D-velden voor kolommen (x, z): hoogte H, water-y (nan = geen), materiaalcode, gebied-gewichten."""
    X = np.asarray(X, float)
    Z_ = np.asarray(Z_, float)
    n1 = ruis(X, Z_, 140, 1)
    n2 = ruis(X, Z_, 40, 2, oct=2)
    H = LAAG + 2.5 * n1 + 1.0 * n2
    wat = np.full(X.shape, np.nan)
    mat = np.zeros(X.shape, np.int16)          # 0 zand, 1 rots, 2 gras, 3 zout, 4 duin, 5 oasegras, 6 ruïnegrond
    # --- westelijk plateau (Rozenkloof + Palmenkloven), getrapte rand
    rand_x = 3602 + 34 * ruis(Z_, X * 0 + 7, 90, 3) + 12 * n2 - 30 * smooth(-760, -640, Z_)
    p = smooth(rand_x + 26, rand_x - 26, X)
    trede = np.floor(p * 4.0 + 0.15 * n2) / 4.0
    plat = PLATEAU + 5 * n1 + 2 * ruis(X, Z_, 18, 9, oct=1)
    Hp = H * (1 - trede) + plat * trede
    rotsig = trede > 0.05
    H = np.where(rotsig, Hp, H)
    mat = np.where(rotsig, 1, mat)
    # zandvlekken bovenop het plateau
    mat = np.where((trede > 0.99) & (ruis(X, Z_, 30, 12, oct=2) > 0.15), 0, mat)
    # --- westkust: kliffen naar zee
    kust = 3292 + 16 * ruis(Z_, X * 0, 70, 4) + 8 * n2
    zee = X < kust
    H = np.where(zee, np.minimum(H, ZEE - 6 - np.clip((kust - X) * 0.35, 0, 30)), H)
    wat = np.where(zee, ZEE, wat)
    # --- de Rozenkloof
    dk, vk, tk = lijn_afstand(X, Z_, KLOOF)
    hw, bodem = vk[..., 0], vk[..., 1]
    q = dk / np.maximum(hw, 1)
    qn = q + 0.05 * ruis(X, Z_, 22, 51, oct=2) + 0.03 * n2          # grillige richelranden
    ry = 1.8 * ruis(X, Z_, 30, 52, oct=2)                             # richels niet vlak
    mond = smooth(3330, 3300, X)                                      # bij de zeemond: alleen steile wanden
    kl = np.where(qn < Q_RICHEL[0] + 0.25 * mond, bodem,
                  np.where(qn < Q_RICHEL[1] + 0.2 * mond, bodem + DY_RICHEL[0] + ry,
                           np.where(qn < Q_RICHEL[2] + 0.12 * mond, bodem + DY_RICHEL[1] + ry,
                                    np.where(q < 1.0, bodem + DY_RICHEL[2] + ry, H))))
    in_kloof = (q < 1.0) & ~((X > KLOOF_EIND) & (q >= 0) )
    H = np.where(in_kloof, np.minimum(H, kl), H)
    # de Siq
    dq, vq, tq = lijn_afstand(X, Z_, SIQ)
    siq = dq < vq[..., 0] + 0.6 * n2
    H = np.where(siq, np.minimum(H, vq[..., 1]), H)
    beek = (dk < 2.4) & (tk < 6.5)
    H = np.where(beek, H - 2, H)
    wat = np.where(beek, H + 1, wat)
    wat = np.where(in_kloof & (bodem < ZEE + 1) & (q < Q_RICHEL[0]), ZEE, wat)
    # --- de Saffraanrivier (+ de Palmenkloven: de kloof in het plateau)
    dr, vr, tr = lijn_afstand(X, Z_, RIVIER)
    rb = vr[..., 0]
    kloofbreed = rb * 2.6 + 10 + 8 * n1
    in_pk = rotsig & (dr < kloofbreed)
    # in de Palmenkloven: brede kloofbodem met oevers (getrapt naar de rand)
    qq = dr / np.maximum(kloofbreed, 1)
    pk = np.where(qq < 0.55, LAAG - 2, np.where(qq < 0.8, LAAG + 9, LAAG + 24))
    H = np.where(in_pk, np.minimum(H, pk + 1.2 * n2), H)
    mat = np.where(in_pk & (qq < 0.8), 2, mat)
    # zijkloven met palmen
    for zk in ([(3470, -972, 0), (3492, -1010, 0), (3520, -1050, 0)], [(3420, -985, 0), (3400, -930, 0), (3380, -880, 0)],
               [(3560, -950, 0), (3570, -900, 0), (3548, -858, 0)]):
        dz_, _, _ = lijn_afstand(X, Z_, zk)
        m = rotsig & (dz_ < 13 + 4 * n2)
        H = np.where(m, np.minimum(H, LAAG + 4 + dz_ * 0.5), H)
        mat = np.where(m & (dz_ < 9), 2, mat)
    # de rivier zelf
    bed = dr < rb
    H = np.where(bed, np.minimum(H, ZEE - 3), H)
    wat = np.where(bed, ZEE + 1, wat)
    oever = (dr >= rb) & (dr < rb + 7)
    H = np.where(oever & ~rotsig, np.minimum(H, ZEE + 2 + (dr - rb) * 0.45), H)
    groen = (dr < rb + 30 + 10 * n1) & ~rotsig
    mat = np.where(groen & (mat == 0), 2, mat)
    # --- Saffraanpoort: vlak stadsplateau
    ds = np.hypot((X - SAFFRAAN[0]) / STAD_R[0], (Z_ - SAFFRAAN[1]) / STAD_R[1])
    stad = ds < 1.0
    H = np.where(stad & ~bed, np.maximum(np.minimum(H, LAAG + 1), LAAG + 1) * 0 + LAAG + 1, H)
    H = np.where((ds >= 1.0) & (ds < 1.25) & ~bed, H * smooth(1.0, 1.25, ds) + (LAAG + 1) * (1 - smooth(1.0, 1.25, ds)), H)
    mat = np.where(stad & ~bed, 7, mat)
    # --- de Zoutvlakte
    dzv = np.hypot((X - ZOUT[0]) / ZOUT[2], (Z_ - ZOUT[1]) / ZOUT[3]) + 0.08 * n1
    zout = dzv < 1.0
    H = np.where(zout, LAAG - 1, H)
    H = np.where((dzv >= 1) & (dzv < 1.3), H * smooth(1.0, 1.3, dzv) + (LAAG - 1) * (1 - smooth(1.0, 1.3, dzv)), H)
    mat = np.where(zout, 3, mat)
    # --- het Ruïnedal: mesa's rond een dalbodem
    rd = smooth(3850, 3890, X + 40 * n1) * smooth(-850, -900, Z_ + 30 * n1 + 12 * n2)
    mesa = (ruis(X, Z_, 70, 31, oct=2) > 0.12) & (rd > 0.5)
    dal, _, _ = lijn_afstand(X, Z_, [(3870, -930, 0), (3940, -960, 0), (4000, -975, 0), (4070, -1000, 0), (4120, -1040, 0)])
    mesa &= dal > 26
    mh = LAAG + 30 + 6 * n1 + np.floor(np.clip(ruis(X, Z_, 70, 31, oct=2) - 0.12, 0, 1) * 30) * 0.6
    H = np.where(mesa, mh, H)
    mat = np.where(mesa, 1, mat)
    mat = np.where((rd > 0.5) & ~mesa & (mat == 0), 6, mat)
    # --- de Roze Duinen (al het lage zand dat niet stad, rivier, zout, oase of ruïnedal is)
    dzv0 = np.hypot((X - ZOUT[0]) / ZOUT[2], (Z_ - ZOUT[1]) / ZOUT[3]) + 0.08 * n1
    do0 = np.hypot(X - OASE[0], Z_ - OASE[1]) / OASE[2]
    rd0 = smooth(3850, 3890, X + 40 * n1) * smooth(-850, -900, Z_ + 30 * n1 + 12 * n2)
    duin_m = smooth(1.15, 1.6, ds) * smooth(28, 70, dr) * smooth(1.15, 1.5, dzv0) * smooth(2.4, 3.4, do0)
    duin_m *= (1 - rd0) * smooth(rand_x + 10, rand_x + 60, X)
    duin_m *= 0.55 + 0.45 * smooth(3860, 3700, X)
    u = (X * 0.82 + Z_ * 0.57) / 34.0 + 1.6 * ruis(X, Z_, 90, 21, oct=2)
    fr = u - np.floor(u)
    kam = np.where(fr < 0.72, fr / 0.72, (1 - fr) / 0.28)          # flauwe loefzijde, steile lijzijde
    amp = 8 + 10 * (ruis(X, Z_, 160, 22, oct=1) * 0.5 + 0.5)
    duin = (kam ** 1.6) * amp + 3 * ruis(X, Z_, 60, 23, oct=2)
    dw, _, _ = lijn_afstand(X, Z_, [(x_, z_, 0) for (x_, z_) in WEG_ZUID])
    duin_m = duin_m * smooth(6, 16, dw)                              # de duinroute is uitgesleten
    H = np.where(~rotsig, H + duin * duin_m, H)
    mat = np.where((duin_m > 0.25) & ~rotsig & (mat == 0), 4, mat)
    # --- de Moederoase
    do = np.hypot(X - OASE[0], Z_ - OASE[1]) / (OASE[2] * (1 + 0.12 * n2))
    meer = do < 1.0
    H = np.where(meer, ZEE - 5 + 3 * do, H)
    wat = np.where(meer, ZEE + 1, wat)
    ring = (do >= 1) & (do < 2.1)
    H = np.where(ring, np.minimum(H, ZEE + 2 + (do - 1) * 3.5), H)
    mat = np.where(ring | ((do < 2.6) & (mat == 0)), 5, mat)
    # torenrots
    dt = np.hypot(X - TOREN[0], Z_ - TOREN[1])
    H = np.where(dt < 26, np.maximum(H, LAAG + 2 + np.clip(26 - dt, 0, 14) * 0.9), H)
    mat = np.where((dt < 24) & (mat != 3), 1, mat)
    # [zone 6 kaart] the outer coast: towards the north/south/east tile edges the land sinks into the shared open sea (zeenaad),
    # a beach on sand, steeper cliffs on rock; the west keeps its own sea cliffs. (zone 7 replaces the east later.)
    rr = np.minimum(np.minimum(Z_ - RAND[1], RAND[3] - Z_), RAND[2] - X) + 9 * n1 + 4 * n2
    cap = np.where(mat == 1, ZEE - 1 + (rr - 26) * 1.4, ZEE - 1 + (rr - 26) * 0.32)
    kust = H > cap
    H = np.where(kust, cap, H)
    nat = (H < ZEE) & np.isnan(wat)
    wat = np.where(nat, ZEE, wat)
    mat = np.where((rr < 44) & (mat != 1) & (mat != 3), 0, mat)
    return H, wat, mat


# ------------------------------------------------------------------------------------------------ in een venster
BANDEN = ["rots", "rots", "rots_rood", "rots", "rots_perzik", "rots", "rots_marmer", "rots", "rots_diep", "rots_rood",
          "rots", "rots_perzik", "rots", "rots_roze", "rots_rood", "rots", "rots_marmer", "rots", "rots_perzik", "rots"]


def vul_kuilen(H, wat):
    """[u-z6afronding] walkability: raise every dry terrain cell you could fall into but never walk out of (an enclosed
    terrace basin behind cliffs) to 1 below its lowest rim, so you can always step out (step up <= 1, falls are fine).
    Priority flood from the window edge + all water (you swim up and climb out 1). Returns the filled H (+ count)."""
    from collections import deque
    hk = np.floor(H)
    nat = ~np.isnan(wat)
    h = np.where(nat, np.floor(np.nan_to_num(wat)) - 1, hk)
    NX, NZ = h.shape
    # 1. who can walk out as is: BFS from the seeds, n joins via a neighbour c already out when h[c] <= h[n] + 1
    uit = np.zeros(h.shape, bool)
    uit[0, :] = uit[-1, :] = uit[:, 0] = uit[:, -1] = True
    uit |= nat
    q = deque(zip(*np.nonzero(uit)))
    while q:
        a, c = q.popleft()
        for da, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n1, n2 = a + da, c + dc
            if 0 <= n1 < NX and 0 <= n2 < NZ and not uit[n1, n2] and h[a, c] <= h[n1, n2] + 1:
                uit[n1, n2] = True
                q.append((n1, n2))
    lvl = h.copy()
    if not uit.all():
        # 2. the trapped cells: filled flat to 1 below their lowest rim (priority flood, grounded on the `uit` cells: from the
        #    flat you step up 1 onto the rim and walk out)
        import heapq
        lvl = np.where(uit, h, np.inf)
        q = []
        for a, c in zip(*np.nonzero(~uit)):
            for da, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n1, n2 = a + da, c + dc
                if 0 <= n1 < NX and 0 <= n2 < NZ and uit[n1, n2]:
                    k = max(h[a, c], h[n1, n2] - 1)
                    if k < lvl[a, c]:
                        lvl[a, c] = k
        q = [(float(lvl[a, c]), int(a), int(c)) for a, c in zip(*np.nonzero(~uit & np.isfinite(lvl)))]
        heapq.heapify(q)
        while q:
            L, a, c = heapq.heappop(q)
            if L > lvl[a, c]:
                continue
            for da, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n1, n2 = a + da, c + dc
                if 0 <= n1 < NX and 0 <= n2 < NZ and not uit[n1, n2]:
                    k = max(h[n1, n2], L)
                    if k < lvl[n1, n2]:
                        lvl[n1, n2] = k
                        heapq.heappush(q, (float(k), n1, n2))
    op = (~nat) & (lvl > hk)
    return np.where(op, lvl + 0.01, H), int(op.sum())


def bouw_land(W):
    """Vult het venster met het landschap (kolommen, gevectoriseerd)."""
    H, wat, mat = velden(W.XX, W.ZZ)
    H, n_vul = vul_kuilen(H, wat)        # [u-z6afronding] no terrace basins you cannot leave
    print(f"  [land] kuilen gevuld: {n_vul} kolommen", flush=True)
    W.H, W.wat, W.mat = H, wat, mat
    x1, y1 = W.b[0], W.b[1]
    bodem = np.full(H.shape, float(y1))
    ids_band = W.ids([b(k) for k in BANDEN])
    nb = len(BANDEN)
    golf = 2.5 * SK.vnoise(W.XX, W.ZZ, cell=60, seed=41, oct=2)
    s_zand, s_zand2, s_rots = W.id(b("zand")), W.id(b("zand2")), W.id(b("rots"))
    s_gras, s_zout, s_zout2, s_mod = W.id(b("gras")), W.id(b("zout")), W.id(b("zout2")), W.id("minecraft:mud")
    s_ruine = W.id("minecraft:smooth_sandstone")
    s_stad = W.id(b("vloer_zand"))
    hk = np.floor(H)

    def laag(y, m):
        yi = math.floor(y)
        diep = hk - yi
        band = ids_band[((np.floor((yi + golf) / 3.0)).astype(int)) % nb]
        out = band.copy()
        top = diep < 1
        rotsboven = (mat == 1) & (diep < 1.5) & (SK.hash3(W.XX, 0, W.ZZ, 8) < 0.55 + 0.3 * golf / 2.5)
        out = np.where(rotsboven & top, s_zand, out)
        out = np.where((mat == 1) & (diep < 2.5) & ~(rotsboven & top), s_rots, out)
        zandig = (mat == 0) | (mat == 4)
        out = np.where(zandig & (diep < 4), np.where((mat == 4) & top & (SK.hash3(W.XX, yi, W.ZZ, 3) < 0.25), s_zand2,
                                                     s_zand), out)
        out = np.where(((mat == 2) | (mat == 5)) & top, s_gras, out)
        out = np.where(((mat == 2) | (mat == 5)) & ~top & (diep < 3), s_mod, out)
        zr = (mat == 3)
        lijn = (np.abs(((W.XX * 0.5 + W.ZZ * 0.866) / 7) % 1 - 0.5) < 0.07) | (np.abs((W.XX / 7) % 1 - 0.5) < 0.06)
        out = np.where(zr & top, np.where(lijn, s_zout2, s_zout), out)
        out = np.where(zr & ~top & (diep < 3), s_zout, out)
        out = np.where((mat == 6) & top, np.where(SK.hash3(W.XX, yi, W.ZZ, 5) < 0.3, s_ruine, s_zand), out)
        out = np.where((mat == 7) & top, s_stad, out)
        out = np.where((mat == 7) & ~top & (diep < 3), s_zand, out)
        return out
    W.kolom(bodem, hk, None, laagfn=laag)
    # water
    wm = ~np.isnan(wat)
    if wm.any():
        W.kolom(np.where(wm, hk + 1, 0), np.where(wm, wat - 1, -1), "minecraft:water[level=0]", mask=wm)
    return H


def hoogte(W, x, z):
    """Maaiveld (bovenste vaste blok + 1) op wereld (x, z) uit de velden van het venster (of berekend)."""
    i, j = W.vx(x), W.vz(z)
    if hasattr(W, "H") and 0 <= i < W.H.shape[0] and 0 <= j < W.H.shape[1]:
        return int(math.floor(W.H[i, j])) + 1
    H, _, _ = velden(np.array([[x + 0.5]]), np.array([[z + 0.5]]))
    return int(math.floor(H[0, 0])) + 1


def is_water(W, x, z):
    i, j = W.vx(x), W.vz(z)
    if hasattr(W, "wat") and 0 <= i < W.wat.shape[0] and 0 <= j < W.wat.shape[1]:
        return not np.isnan(W.wat[i, j])
    return False


def groen(W, dicht=1.0, seed=7, uitsluit=None):
    """Palmen, struiken en bloemen op groen (rivieroevers, oase, kloofbodems), droog spul op de duinen."""
    if not hasattr(W, "mat"):
        return
    x1, _, z1, x2, _, z2 = W.b
    stap = 5
    for gx in range(int(x1) // stap * stap, int(x2), stap):
        for gz in range(int(z1) // stap * stap, int(z2), stap):
            r = ZB.rng_van(gx, gz, seed)
            x = gx + r.randint(0, stap)
            z = gz + r.randint(0, stap)
            i, j = W.vx(x), W.vz(z)
            if not (0 <= i < W.mat.shape[0] and 0 <= j < W.mat.shape[1]):
                continue
            if uitsluit and uitsluit(x, z):
                continue
            m = W.mat[i, j]
            if not np.isnan(W.wat[i, j]):
                if r.rand() < 0.04 * dicht and W.wat[i, j] - W.H[i, j] < 3:
                    ZB.zet(W, x, int(W.wat[i, j]), z, "minecraft:lily_pad")
                continue
            y = int(math.floor(W.H[i, j])) + 1
            p = r.rand()
            if m in (2, 5):
                if p < 0.16 * dicht:
                    ZB.palm(W, x, y, z, seed=seed)
                elif p < 0.30 * dicht:
                    ZB.struik(W, x, y, z, seed=seed)
                elif p < 0.55 * dicht:
                    ZB.zet(W, x, y, z, ZB.BLOEMEN[r.randint(len(ZB.BLOEMEN))].replace("[half=lower]", ""))
                elif p < 0.75:
                    ZB.zet(W, x, y, z, "minecraft:short_grass")
            elif m == 4 or m == 0:
                if p < 0.025 * dicht:
                    ZB.zet(W, x, y, z, "minecraft:dead_bush")
                elif p < 0.05 * dicht:
                    ZB.zet(W, x, y, z, "minecraft:short_dry_grass")
                elif p < 0.056 * dicht and m == 0:
                    ZB.palm(W, x, y, z, seed=seed, h=7)
            elif m == 6:
                if p < 0.05:
                    ZB.zet(W, x, y, z, "minecraft:tall_dry_grass")
            elif m == 3:
                if p < 0.04:
                    ZB.zet(W, x, y, z, b("zoutkristalletjes"))
