"""De wereldboom van zone 3 "De Schemerwortel": terrein, wortels, stam, Stamspiraal, kruin, takken, Wortelduister.

Alles in wereldcoördinaten t.o.v. de stamas T (spel: x + T_WERELD[0], z + T_WERELD[1]); y = echte spel-y.
Elke functie vult alleen wat in het venster valt, dus dezelfde code bouwt het overzicht (k=0.5) en elke
close-up (k=1). Maten volgen ZONE3_PLAN.md §2 (stam Ø~190, kruin r~330 y190-285, 8 wortels tot r~650,
Wortelduister y8-60 onder r<=450).

    bouw(W, delen=None)   -> bouwt alles (of alleen de genoemde delen) in venster W
"""
import math
import numpy as np
import schemerkit as SK
from schemerkit import vnoise, vnoise3, hash3

G = 64                       # maaiveld
KRUIN_R, KRUIN_Y0, KRUIN_TOP = 330, 190, 285
EILAND_R = 660
NEST = (0, None, -40)        # Kruintop (y volgt uit de kruinhoogte)
WORTEL_HOEKEN = [math.radians(22.5 + 45 * i) for i in range(8)]   # atan2(z, x): 0 = oost, 90 = zuid
WORTEL_EIND = [640, 600, 660, 610, 650, 690, 620, 600]
SPIRAAL = dict(y_top=200, y_bot=70, slagen=2.5, phi0=-math.pi / 2, breed=9)

SCHORS = ["minecraft:spruce_wood", "minecraft:dark_oak_wood", "minecraft:mangrove_wood", "minecraft:jungle_wood"]
SCHORS_W = [5, 3, 1.2, 0.6]
ROT = ["minecraft:light_blue_terracotta", "minecraft:pale_moss_block", "minecraft:light_gray_concrete_powder",
       "minecraft:cyan_terracotta", "guhs:bleekmos"]
ROT_W = [4, 3, 2, 1.5, 1.5]
BLAD = ["minecraft:oak_leaves", "minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves",
        "minecraft:jungle_leaves", "minecraft:acacia_leaves"]
BLAD_W = [4, 2, 2.5, 1.5, 1.5]
BLOESEM = ["minecraft:cherry_leaves", "guhs:guhbloesem_leaves", "minecraft:flowering_azalea_leaves"]
BLOESEM_W = [5, 3, 1]
GOUD = ["minecraft:honeycomb_block", "guhs:kristalkaas_blok", "minecraft:oak_leaves"]
GOUD_W = [3, 1, 3]
MOS = ["minecraft:moss_block", "guhs:vadsmos", "minecraft:moss_block", "guhs:glimmos"]
MOS_W = [6, 2, 2, 0.4]

# takken: (naam, controlepunten, stralen) — de bovenkant is het rijpad
TAKKEN = {
    "oost": ([(40, 204, 0), (190, 212, 3), (330, 220, 0), (450, 224, -3), (560, 226, 0), (628, 229, 2)],
             [38, 28, 21, 18, 15, 11]),
    "west": ([(-40, 200, 8), (-200, 206, 18), (-330, 203, 24), (-450, 190, 30), (-565, 171, 34), (-675, 156, 36)],
             [42, 32, 25, 21, 17, 11]),
    "noordwest": ([(-28, 200, -28), (-170, 207, -165), (-290, 203, -285), (-410, 186, -398), (-520, 166, -500),
                   (-590, 153, -560)], [40, 30, 23, 19, 15, 10]),
    "zuidwest": ([(-28, 200, 28), (-160, 205, 150), (-280, 202, 262), (-392, 186, 360), (-490, 167, 440),
                  (-552, 153, 492)], [40, 30, 23, 19, 15, 10]),
}
# korte takstompen binnen/net buiten de kruin (onregelmatig silhouet)
STOMPEN = [((0, 205, -40), (60, 228, -300), 18, 9), ((30, 205, 30), (140, 226, 300), 18, 9),
           ((25, 205, -25), (250, 224, -250), 18, 8), ((-30, 205, 0), (-260, 230, -120), 16, 8),
           ((10, 205, 30), (-60, 232, 330), 16, 8), ((30, 205, -10), (320, 236, -120), 14, 8)]
VALBRUG = ([(-470, 186, 40), (-455, 182, 160), (-430, 179, 300), (-420, 178, 350)], [7, 6.5, 6, 6])


def wrap(a):
    return (a + np.pi) % (2 * np.pi) - np.pi


def dal(theta):
    """Welk dal (0=O,1=ZO,2=Z,3=ZW,4=W,5=NW,6=N,7=NO) hoort bij hoek theta (atan2(z,x))."""
    return np.round(np.degrees(theta) / 45.0).astype(int) % 8


def rot_kracht(x, y, z):
    """0..1: hoe erg de Mika-schimmel hier is. Het ergst in het NW-dal en laag bij de stam; kruipt omhoog."""
    th = np.arctan2(z, x)
    r = np.hypot(x, z)
    nw = np.clip(np.cos(th - math.radians(225)), 0, 1) ** 2
    laag = np.clip((150 - np.asarray(y, float)) / 80.0, 0, 1)
    dichtbij = np.clip((520 - r) / 300.0, 0, 1)
    return np.clip(0.25 + 0.55 * nw, 0, 1) * (0.35 + 0.65 * laag) * (0.45 + 0.55 * dichtbij) + 0.35 * nw * laag


def rot_mask(W, x, y, z, extra=0.0):
    """Vlekkerige schimmel: True waar de rot zit."""
    n = vnoise(x + 0.3 * np.asarray(y, float), z, cell=22, seed=91, oct=3)
    return (n * 0.5 + 0.5) < rot_kracht(x, y, z) * 0.95 + extra


# ===================================================================================================== terrein
def eilandrand(theta):
    return EILAND_R + 34 * vnoise(np.cos(theta) * 300, np.sin(theta) * 300, cell=90, seed=7, oct=2)


def wortel_profiel(W, i, XX, ZZ):
    """Hoofdwortel i: geeft (bot, top, d_rel, s) per kolom (NaN buiten de wortel)."""
    r = np.hypot(XX, ZZ)
    th = np.arctan2(ZZ, XX)
    phi = WORTEL_HOEKEN[i] + 0.09 * np.sin(r / 105.0 + i * 2.1)
    dlt = wrap(th - phi)
    s = r * np.cos(dlt)
    d = r * np.sin(dlt)
    eind = WORTEL_EIND[i]
    rr = (44 + 6 * math.sin(i * 2.3)) * (1 - s / 800.0) + 7
    rr = rr * np.clip((eind - s) / 80.0, 0, 1) ** 0.6
    boog = 24 * np.clip(np.sin(s / 47.0 + i * 1.37), 0, 1) ** 3 * np.clip((s - 230) / 90.0, 0, 1)
    c = G + 0.12 * rr + boog + 26 * np.exp(-np.clip(s, 0, None) / 220.0) + \
        4 * vnoise(s, i * 100.0, cell=60, seed=3)
    c = c - 16 * np.clip((s - (eind - 170)) / 170.0, 0, 1) ** 1.5   # [zone 3 kaart] the root dives into the ground at its end
    q = 1 - (d / np.maximum(rr, 1e-3)) ** 2
    ok = (np.abs(dlt) < 1.2) & (q > 0) & (s > 40) & (rr > 1)
    h = np.sqrt(np.clip(q, 0, 1)) * rr
    top = np.where(ok, c + h * 0.85, np.nan)
    bot = np.where(ok, c - h * 0.8, np.nan)
    return bot, top, np.where(ok, np.abs(d) / np.maximum(rr, 1e-3), np.nan), s


def grondhoogte(XX, ZZ):
    r = np.hypot(XX, ZZ)
    th = np.arctan2(ZZ, XX)
    g = G + 3.5 * vnoise(XX, ZZ, cell=70, seed=11) + 1.5 * vnoise(XX, ZZ, cell=16, seed=12)
    # dalen tussen de wortels lopen iets af naar buiten, met heuveltjes
    g += 5 * np.clip(vnoise(XX, ZZ, cell=45, seed=13), 0, 1)
    g += 26 * np.exp(-r / 220.0)   # de wortelheuvel loopt op naar de stam
    g += 11 * np.clip(vnoise(XX, ZZ, cell=38, seed=14, oct=2), 0, 1) ** 1.3 * np.clip((r - 120) / 100.0, 0, 1)
    g -= 5 * np.clip(-vnoise(XX, ZZ, cell=52, seed=15), 0, 1)   # kommen en heuveltjes: nergens vlak
    rand = eilandrand(th)
    # [zone 3 kaart] a walkable shore instead of a sheer cliff: from the rim the ground slopes into the cloud sea (y 62), so
    # anyone who falls into the sea can wade out again (walkability standard); the roots still plunge into it
    g = np.where(r > rand, np.maximum(58.6, np.minimum(g, 63.5 - (r - rand) * 0.45)), g)
    return g


def terrein(W):
    if W.b[1] > 140:
        return
    XX, ZZ, RR, TH = W.XX, W.ZZ, W.RR, W.TH
    g = grondhoogte(XX, ZZ)
    dl = dal(TH)
    onder_kruin = RR < KRUIN_R * 0.95
    ylo = W.b[1]
    rand = eilandrand(TH)
    eiland = RR <= rand + 30
    bodem = np.full(XX.shape, float(ylo))
    # ondergrond
    gesteente = W.kies(["minecraft:stone", "minecraft:tuff", "minecraft:deepslate", "minecraft:andesite"],
                       [4, 2, 1, 1], XX, 0, ZZ, 5)
    wortelstreep = hash3(np.round(TH * 180), 0, 0, 6) < 0.16

    def klifmix(y, m):
        ids = W.kies(["minecraft:stone", "minecraft:tuff", "minecraft:andesite", "minecraft:moss_block",
                      "minecraft:mossy_cobblestone", "minecraft:calcite"], [4, 3, 2, 1.2, 1, 0.6],
                     XX, np.floor(y / 3.0) + XX * 0, ZZ, 5)
        return np.where(wortelstreep & (RR > rand - 8), W.kies(SCHORS, SCHORS_W, XX, y, ZZ, 8), ids)
    W.kolom(np.maximum(bodem, ylo), g - 4, None, mask=eiland, laagfn=klifmix)
    W.kolom(g - 3.5, g - 1, "minecraft:dirt", mask=eiland & (RR <= rand))
    W.kolom(g - 3.5, g - 1, "minecraft:stone", mask=eiland & (RR > rand))
    # bovenlaag per dal
    top_ids = np.zeros(XX.shape, np.uint16)
    gras = W.kies(["minecraft:grass_block", "minecraft:grass_block", "guhs:knuffelgras"], [5, 3, 1.2], XX, 1, ZZ, 21)
    schaduw = W.kies(["minecraft:moss_block", "minecraft:podzol", "minecraft:rooted_dirt", "guhs:vadsmos",
                      "minecraft:moss_block"], [4, 2, 1, 1.5, 1], XX, 1, ZZ, 22)
    top_ids[:] = gras
    top_ids[onder_kruin] = schaduw[onder_kruin]
    nrd = dl == 6
    top_ids[nrd] = W.kies(["minecraft:moss_block", "guhs:vadsmos", "minecraft:podzol", "guhs:lichtmosblok"],
                          [4, 3, 2, 0.25], XX, 1, ZZ, 23)[nrd]
    rot = rot_mask(W, XX, g, ZZ) & (RR <= rand)
    rotid = W.kies(ROT, ROT_W, XX, 2, ZZ, 24)
    top_ids[rot] = rotid[rot]
    top_ids[RR > rand] = W.kies(["minecraft:sand", "minecraft:sand", "minecraft:moss_block"], [4, 2, 1], XX, 1, ZZ, 25)[RR > rand]   # [zone 3 kaart] beach
    W.kolom(g, g, top_ids, mask=eiland)
    W._g = g
    W._dal = dl


def wortels(W):
    if W.b[1] > 160:
        return
    XX, ZZ = W.XX, W.ZZ
    for i in range(8):
        bot, top, drel, s = wortel_profiel(W, i, XX, ZZ)
        m = ~np.isnan(top)
        if not m.any():
            continue
        g = W._g if hasattr(W, "_g") else grondhoogte(XX, ZZ)
        bot = np.where(bot < g + 2.5, np.minimum(bot, g - 4), bot)  # aansluiten op de grond, tenzij een boog
        top_i = np.nan_to_num(top, nan=-1e9)
        bot_i = np.nan_to_num(bot, nan=1e9)

        def laag(y, mm, top_i=top_i, drel=drel):
            ids = W.kies(SCHORS, SCHORS_W, W.XX + 0.0 * y, y, W.ZZ, 31 + i)
            rotm = rot_mask(W, W.XX, y, W.ZZ, extra=0.16)
            ids = np.where(rotm, W.kies(ROT, ROT_W, W.XX, y, W.ZZ, 33), ids)
            boven = (y > top_i - 2.2 / W.k) & (np.nan_to_num(drel, nan=9) < 0.62)
            mos = W.kies(MOS, MOS_W, W.XX, y, W.ZZ, 34)
            ids = np.where(boven & ~rotm, mos, ids)
            return ids
        W.kolom(bot_i, top_i, None, mask=m, laagfn=laag)
    # vertakkende wortels: elke hoofdwortel splitst zich in twee ruggen die de dalen opdelen
    for i in range(8):
        for zij in (-1, 1):
            a0 = WORTEL_HOEKEN[i]
            pts, rads = [], []
            for t in np.linspace(0, 1, 7):
                s_ = 190 + 380 * t
                a = a0 + zij * (0.05 + 0.3 * t) + 0.04 * math.sin(t * 7 + i)
                boog = 16 * max(0.0, math.sin(t * 9 + i + zij)) ** 3
                pts.append((s_ * math.cos(a), G + 4 + boog - 8 * t, s_ * math.sin(a)))
                rads.append(20 - 13 * t)
            W.buis(pts, rads, None, mixfn=_wortelmix(W, 45 + i), plat=0.85)
    # kleine zijwortels tussen de grote
    for i in range(8):
        a = math.radians(45 * i + 2)
        for j, (r0, r1) in enumerate(((100, 260), (120, 330))):
            aa = a + (0.12 if j else -0.1)
            p0 = (r0 * math.cos(aa), G - 4, r0 * math.sin(aa))
            p1 = ((r0 + r1) / 2 * math.cos(aa + 0.05), G + 6, (r0 + r1) / 2 * math.sin(aa + 0.05))
            p2 = (r1 * math.cos(aa + 0.02), G - 6, r1 * math.sin(aa + 0.02))
            W.buis([p0, p1, p2], [11, 8, 4], None, mixfn=_schorsmix(W, 40 + i), plat=0.8)


def _wortelmix(W, seed):
    """Schors met mos bovenop (hoe hoger in de buis, hoe groener) en schimmelvlekken."""
    def f(x, y, z, m):
        ids = W.kies(SCHORS, SCHORS_W, x, y, z, seed)
        g = grondhoogte(x[:, 0, :], z[:, 0, :])[:, None, :]
        mos = (y > g + 6) & (hash3(x, y, z, seed + 7) < 0.75)
        ids = np.where(mos, W.kies(MOS, MOS_W, x, y, z, seed + 8), ids)
        rm = rot_mask(W, x, y, z, extra=0.12)
        return np.where(rm, W.kies(ROT, ROT_W, x, y, z, seed + 1), ids)
    return f


def _schorsmix(W, seed, rot=True):
    def f(x, y, z, m):
        ids = W.kies(SCHORS, SCHORS_W, x, y, z, seed)
        if rot:
            rm = rot_mask(W, x, y, z, extra=0.05)
            ids = np.where(rm, W.kies(ROT, ROT_W, x, y, z, seed + 1), ids)
        return ids
    return f


# ===================================================================================================== stam
def r_buiten(y, th):
    y = np.asarray(y, float)
    basis = np.where(y < 200, 100 - 12 * (y - G) / 136.0, 88 - 38 * np.clip((y - 200) / 70.0, 0, 1))
    voet = 52 * np.exp(-np.clip(y - G, 0, None) / 13.0) * (0.75 + 0.25 * np.cos(8 * (th - WORTEL_HOEKEN[0])))
    ribbel = 1 + 0.035 * np.sin(13 * th + y / 37.0) + 0.02 * np.sin(29 * th - y / 23.0)
    return (basis + voet) * ribbel


R_BINNEN = 68
HOL_Y = (75, 196)
KWASTGATEN = [(math.radians(a), y, r) for (a, y, r) in
              ((-35, 160, 9), (60, 120, 8), (150, 172, 10), (205, 104, 8), (280, 140, 9), (110, 90, 7),
               (330, 100, 7), (20, 186, 7))]


def spiraal_y(th):
    """Hoogte van de Stamspiraal-weg voor elke winding bij hoek th: lijst arrays (NaN = geen weg)."""
    p = SPIRAAL
    t0 = (th - p["phi0"]) % (2 * np.pi)
    out = []
    for n in range(3):
        a = t0 + 2 * np.pi * n
        y = p["y_top"] - (p["y_top"] - p["y_bot"]) * a / (2 * np.pi * p["slagen"])
        out.append(np.where(a <= 2 * np.pi * p["slagen"], y, np.nan))
    return out


def stam(W):
    if not W.raakt(-170, 30, -170, 170, 270, 170):
        return
    X, Y, Z = W.v.shape
    sx = slice(max(0, W.vx(-170)), min(X, W.vx(170) + 1))
    sz = slice(max(0, W.vz(-170)), min(Z, W.vz(170) + 1))
    XX, ZZ = W.XX[sx, sz], W.ZZ[sx, sz]
    RR, TH = np.hypot(XX, ZZ), np.arctan2(ZZ, XX)
    sy_ = spiraal_y(TH)
    br = SPIRAAL["breed"]
    pad_id, hek_id, lamp_id = W.id("minecraft:dirt_path"), W.id("minecraft:spruce_fence"), W.id("guhs:lampion_geel")
    plank_id = W.id("minecraft:stripped_spruce_wood")
    kwast = [(math.cos(a) * 90, y, math.sin(a) * 90, r) for (a, y, r) in KWASTGATEN]
    for j in range(Y):
        y = W.ys[j]
        if y < G - 8 or y > 268:
            continue
        Ro = r_buiten(y, TH)
        m = RR <= Ro
        hol = HOL_Y[0] <= y <= HOL_Y[1]
        if hol:
            rin = R_BINNEN - 6 * np.clip((y - (HOL_Y[1] - 12)) / 12.0, 0, 1) ** 2 * 0
            dome = HOL_Y[1] - y
            rin = R_BINNEN * np.sqrt(np.clip(dome / 14.0, 0, 1)) if dome < 14 else R_BINNEN
            m &= RR > rin
        if hol:
            W.v[sx, j, sz][RR <= rin] = 0   # de holle stam is echt leeg (ook geen grond/wortels)
        if not m.any():
            continue
        ids = W.kies(SCHORS, SCHORS_W, XX, y, ZZ + 0.0 * np.round(TH * 40), 61)
        # verticale schorsstrepen
        streep = hash3(np.round(TH * 60), 0, 0, 62) < 0.3
        ids = np.where(streep, W.id("minecraft:dark_oak_wood"), ids)
        rm = rot_mask(W, XX, y, ZZ, extra=0.06) & (RR > Ro - 3)
        ids = np.where(rm, W.kies(ROT, ROT_W, XX, y, ZZ, 63), ids)
        # amber-sapdruppels en roze hars op de schors
        hars = (vnoise(TH * 200, y, cell=9, seed=64) > 0.62) & (RR > Ro - 2.5)
        ids = np.where(hars, W.kies(["minecraft:resin_block", "minecraft:resin_block", "guhs:weidekristal_roze"],
                                    [2, 1, 1], XX, y, ZZ, 65), ids)
        sl = W.v[sx, j, sz]
        sl[m] = ids[m]
        # binnenwand: lichter hout + amberroze
        if hol:
            wand = m & (RR <= (R_BINNEN if (HOL_Y[1] - y) >= 14 else 999) + 2.2)
            wids = W.kies(["minecraft:stripped_spruce_wood", "minecraft:stripped_oak_wood", "minecraft:spruce_wood",
                           "guhs:weidekristal_roze"], [5, 2, 2, 0.04], XX, y, ZZ, 66)
            sl[wand] = wids[wand]
        # Stamspiraal: een weg die in de schors is uitgehakt, met een rand
        for yr in sy_:
            dy = y - yr
            Rr = r_buiten(np.nan_to_num(yr, nan=0), TH)
            binnenrand = Rr - br * 0.6
            buitenrand = Rr + br * 0.45
            snij = (dy > 0.5) & (dy < 9) & (RR > binnenrand) & (RR < buitenrand + 1.5)
            sl[snij] = 0
            dek = (dy >= -0.5) & (dy <= 0.5) & (RR > binnenrand) & (RR < buitenrand)
            sl[dek] = pad_id
            rand_ = dek & (RR >= buitenrand - 1.6)
            sl[rand_] = plank_id
            draag = (dy < -0.5) & (dy > -4) & (RR >= Rr - 1) & (RR < buitenrand - (-dy) * 0.8)
            sl[draag] = plank_id
            hek = (dy > 0.5) & (dy < 1.5) & (RR >= buitenrand - 1.4) & (RR < buitenrand - 0.4)
            hk = hek & (hash3(np.round(TH * 90), 0, 0, 67) < 0.75)
            sl[hk] = hek_id
            lp = (dy > 1.5) & (dy < 2.5) & (RR >= buitenrand - 1.4) & (RR < buitenrand - 0.4) & \
                 (np.abs(((TH * 95 / (2 * np.pi)) % 12) - 6) < 0.5 * max(1, 1 / W.k))
            sl[lp] = lamp_id
    # kwastgaten (lichtgaten) door de wand
    for (x, y, z, r) in kwast:
        d = np.array([x, 0, z]) / 90.0
        W.buis([(d[0] * 55, y, d[2] * 55), (d[0] * 115, y + 4, d[2] * 115)], [r, r * 0.8], None)
    # 3 poorten van de spiraal naar binnen (y 175, 125, 75)
    for yp in (175, 125, 75):
        p = SPIRAAL
        a = (p["y_top"] - yp) / (p["y_top"] - p["y_bot"]) * 2 * math.pi * p["slagen"] + p["phi0"]
        d = (math.cos(a), math.sin(a))
        W.buis([(d[0] * 60, yp + 4, d[1] * 60), (d[0] * 98, yp + 4, d[1] * 98)], [4.5, 4.5], None, plat=1.3)
        W.buis([(d[0] * 60, yp, d[1] * 60), (d[0] * 98, yp, d[1] * 98)], [2.2, 2.2], "minecraft:dirt_path",
               plat=0.3)


# ===================================================================================================== kruin
def kruinrand(th):
    # [zone 3 kaart] more organic: lobes (low harmonics) + stronger noise, so the crown is not a round umbrella
    th = np.asarray(th, float)
    return KRUIN_R * (1 + 0.15 * vnoise(np.cos(th) * 260, np.sin(th) * 260, cell=55, seed=71, oct=2)
                      + 0.06 * np.sin(3 * th + 0.7) + 0.045 * np.sin(5 * th + 2.1))


def kruin_top(XX, ZZ):
    RR, TH = np.hypot(XX, ZZ), np.arctan2(ZZ, XX)
    q = np.clip(RR / kruinrand(TH), 0, 1.2)
    kus = vnoise(XX, ZZ, cell=62, seed=72, oct=2)
    # [zone 3 kaart] lower top (build limit 320: the nest arena needs headroom), lopsided domes (organic silhouette)
    t = 212 + 64 * np.clip(1 - q * q, 0, 1) ** 0.5 + 24 * kus * (1 - 0.4 * q) + \
        9 * np.sin(2 * TH + 0.4) * np.clip(q, 0, 1) + 3.5 * vnoise(XX, ZZ, cell=13, seed=73)
    return np.minimum(t, 291), q


VIJVERS = [(120, -90, 18), (-150, 60, 22), (60, 170, 16), (-60, -200, 14), (210, 90, 15)]


def kruin(W):
    if W.b[4] < 180 or not W.raakt(-420, 180, -420, 420, 320, 420):
        return
    XX, ZZ, RR, TH = W.XX, W.ZZ, W.RR, W.TH
    top, q = kruin_top(XX, ZZ)
    rand = kruinrand(TH)
    binnen = RR <= rand
    hang = np.clip(vnoise(XX, ZZ, cell=40, seed=74), 0, 1)
    bot = 208 + 30 * (1 - q) - 20 * hang + 6 * np.clip(q - 0.85, 0, 1) / 0.15
    # honingvijvers in kommen
    vijver = np.zeros(XX.shape, bool)
    vniveau = np.zeros(XX.shape)
    for (vx, vz, vr) in VIJVERS:
        d = np.hypot(XX - vx, ZZ - vz)
        t0, _ = kruin_top(np.array([vx]), np.array([vz]))
        L = t0[0] - 3
        kom = d < vr + 6
        top = np.where(kom, np.minimum(top, L - 2 + np.clip(d - vr, 0, None) * 0.9), top)
        vm = d < vr
        vijver |= vm
        vniveau = np.where(vm, L, vniveau)
    # nest-kuil op de Kruintop
    dn = np.hypot(XX - NEST[0], ZZ - NEST[2])
    top_n, _ = kruin_top(np.array([NEST[0]]), np.array([NEST[2]]))
    W._nest_y = float(top_n[0])
    top = np.where(dn < 24, np.minimum(top, W._nest_y - 3), top)
    dik = 4.5
    vol = q > 0.9
    # bladkleur
    bloei = vnoise(XX, ZZ, cell=58, seed=75) > 0.12
    goud = vnoise(XX, ZZ, cell=36, seed=76) > 0.25
    grauw = (np.clip(np.cos(TH - math.radians(225)), 0, 1) ** 6 * 0.85 + 0.03 >
             (vnoise(XX, ZZ, cell=30, seed=77) * 0.5 + 0.5)) & (RR > 120)

    def laag(y, m):
        ids = W.kies(BLAD, BLAD_W, XX, y, ZZ, 78)
        ids = np.where(bloei, W.kies(BLOESEM, BLOESEM_W, XX, y, ZZ, 79), ids)
        ids = np.where(goud & ~bloei, W.kies(GOUD, GOUD_W, XX, y, ZZ, 80), ids)
        ids = np.where(grauw, W.kies(["minecraft:pale_oak_leaves", "minecraft:pale_oak_leaves",
                                      "minecraft:light_gray_concrete_powder", "minecraft:light_blue_terracotta"],
                                     [5, 2, 1, 1], XX, y, ZZ, 81), ids)
        # onderkant: donkerder blad met hangend mos
        onder = y < bot + dik
        ids = np.where(onder, W.kies(["minecraft:oak_leaves", "minecraft:azalea_leaves", "minecraft:jungle_leaves"],
                                     [3, 2, 1], XX, y, ZZ, 82), ids)
        return ids
    W.kolom(top - dik, top, None, mask=binnen, laagfn=laag)
    W.kolom(bot, bot + dik, None, mask=binnen & ~vol, laagfn=laag)
    W.kolom(bot, top, None, mask=binnen & vol, laagfn=laag)
    W.kolom(vniveau - 1, vniveau, "minecraft:honey_block", mask=vijver)
    W.kolom(vniveau - 3, vniveau - 2, "minecraft:honeycomb_block", mask=vijver)
    W._kruin_top = top
    # hangende lichtkevers/glimbessen onder het bladerdak
    lk = (hash3(XX, 0, ZZ, 83) < 0.012 * W.k ** -1) & binnen & (RR > 110)
    lengte = 2 + 10 * hash3(XX, 1, ZZ, 84)
    W.kolom(bot - lengte, bot - 1, W.kies(["minecraft:cave_vines_plant[berries=true]",
                                           "minecraft:cave_vines_plant[berries=false]",
                                           "minecraft:pale_hanging_moss[tip=false]"], [2, 3, 1], XX, 2, ZZ, 85),
            mask=lk)
    kruinkussens(W)
    kruintop_nest(W)
    bloesemwaterval(W)


def _bladmix(W, seed):
    def f(x, y, z, m):
        ids = W.kies(BLAD, BLAD_W, x, y, z, seed)
        bl = vnoise(x, z, cell=58, seed=75) > 0.12
        gd = vnoise(x, z, cell=36, seed=76) > 0.25
        ids = np.where(bl, W.kies(BLOESEM, BLOESEM_W, x, y, z, seed + 1), ids)
        ids = np.where(gd & ~bl, W.kies(GOUD, GOUD_W, x, y, z, seed + 2), ids)
        th = np.arctan2(z, x)
        gr = (np.clip(np.cos(th - math.radians(225)), 0, 1) ** 6 * 0.85 + 0.03 >
              (vnoise(x, z, cell=30, seed=77) * 0.5 + 0.5)) & (np.hypot(x, z) > 120)
        ids = np.where(gr, W.kies(["minecraft:pale_oak_leaves", "minecraft:pale_oak_leaves",
                                   "minecraft:light_blue_terracotta"], [5, 2, 1], x, y, z, seed + 3), ids)
        return ids
    return f


def kruinkussens(W):
    """Wolkachtige bladkussens langs de rand en op de kruin: een bol, onregelmatig silhouet."""
    rng = np.random.default_rng(171)
    for i in range(46):
        a = 2 * math.pi * i / 46 + rng.uniform(-0.05, 0.05)
        rr = float(kruinrand(np.array([a]))[0]) * rng.uniform(0.86, 1.0)
        R = rng.uniform(26, 44)
        ryf = rng.uniform(0.45, 0.6)
        c = (rr * math.cos(a), 216 + rng.uniform(-6, 14), rr * math.sin(a))
        if W.raakt(c[0] - R, c[1] - R, c[2] - R, c[0] + R, c[1] + R, c[2] + R):
            W.bol(c, R, None, ry=R * ryf, hollow=4, keep=True, mixfn=_bladmix(W, 160))
    for i in range(22):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(90, 250)
        R = rng.uniform(26, 42)
        x, z = rr * math.cos(a), rr * math.sin(a)
        if math.hypot(x - NEST[0], z - NEST[2]) < 70 or \
                any(math.hypot(x - vx, z - vz) < vr + 30 for (vx, vz, vr) in VIJVERS):
            continue
        t, _ = kruin_top(np.array([x]), np.array([z]))
        c = (x, t[0] - R * 0.15, z)
        if W.raakt(c[0] - R, c[1] - R, c[2] - R, c[0] + R, c[1] + R, c[2] + R):
            W.bol(c, R, None, ry=R * 0.55, hollow=4, keep=True, mixfn=_bladmix(W, 165), ymin=t[0] - 6)


def wolken(W):
    """Wolkenzee rond het eiland: zachte, platte wolkenbollen."""
    if W.b[1] > 60:
        return
    rng = np.random.default_rng(181)
    for i in range(160):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(EILAND_R + 5, 1000)
        R = rng.uniform(22, 55)
        y = rng.uniform(24, 46)
        x, z = rr * math.cos(a), rr * math.sin(a)
        if abs(x) > 760 or abs(z) > 760:
            continue
        c = (x, y, z)
        if W.raakt(c[0] - R, c[1] - R, c[2] - R, c[0] + R, c[1] + R, c[2] + R):
            W.bol(c, R, None, ry=R * 0.32, keep=True, mixfn=lambda x_, y_, z_, m: W.kies(
                ["minecraft:white_wool", "minecraft:white_wool", "guhs:pluiswolblok",
                 "minecraft:white_concrete_powder"], [5, 3, 2, 1], x_, y_, z_, 182))


def bosjes(W):
    """Boompjes in de dalen (buiten de kruin meer, eronder minder): leven, geen lege vlaktes."""
    if W.b[1] > 130:
        return
    rng = np.random.default_rng(191)
    soorten = [("minecraft:oak_log[axis=y]", ["minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves"]),
               ("guhs:guhbloesem_log[axis=y]", ["minecraft:cherry_leaves", "guhs:guhbloesem_leaves"]),
               ("minecraft:birch_log[axis=y]", ["minecraft:oak_leaves", "minecraft:flowering_azalea_leaves"]),
               ("minecraft:spruce_log[axis=y]", ["minecraft:cherry_leaves", "minecraft:oak_leaves"])]
    for i in range(2600):
        a = rng.uniform(0, 2 * math.pi)
        rr = math.sqrt(rng.uniform(150 ** 2, (EILAND_R - 20) ** 2))
        groot = rng.random() < 0.16
        h = rng.uniform(14, 24) if groot else rng.uniform(6, 14)
        R = rng.uniform(7, 10) if groot else rng.uniform(3.5, 6.5)
        skip = rng.random() < 0.6
        x, z = rr * math.cos(a), rr * math.sin(a)
        if rr < KRUIN_R and skip:
            continue
        dl = (math.degrees(a) - 22.5) % 45
        if min(dl, 45 - dl) < 6 and rr < 520:
            continue
        if not W.raakt(x - 10, 50, z - 10, x + 10, 140, z + 10):
            continue
        g = float(grondhoogte(np.array([x]), np.array([z]))[0])
        log, bl = soorten[i % 4]
        W.wfill(x, g + 1, z, x, g + h, z, log)
        W.bol((x, g + h + 1, z), R, None, ry=R * 0.8, keep=True,
              mixfn=lambda x_, y, z_, m, bl=bl: W.kies(bl, [3, 2], x_, y, z_, 192))


def kruintop_nest(W):
    """Het oude nest: een ring van gevlochten takken (~60 Ø) met grauwe en roze veren en een lege eierkuil."""
    y0 = getattr(W, "_nest_y", None)
    if y0 is None:
        t, _ = kruin_top(np.array([NEST[0]]), np.array([NEST[2]]))
        y0 = float(t[0])
    cx, cz = NEST[0], NEST[2]
    if not W.raakt(cx - 45, y0 - 15, cz - 45, cx + 45, y0 + 25, cz + 45):
        return
    n = 46
    tw = ["minecraft:stripped_spruce_wood", "minecraft:spruce_wood", "minecraft:stripped_dark_oak_wood",
          "minecraft:dark_oak_wood", "minecraft:stripped_oak_wood"]

    def mix(x, y, z, m):
        return W.kies(tw, [3, 3, 2, 2, 1.5], x, y, z, 90)
    for k in range(n):
        a = 2 * math.pi * k / n
        for laag_ in range(3):
            r = 27 + laag_ * 2.5 + 2 * math.sin(a * 3 + laag_)
            b = a + 0.5 + laag_ * 0.3
            p0 = (cx + r * math.cos(a), y0 - 2 + laag_ * 4, cz + r * math.sin(a))
            p1 = (cx + (r + 2) * math.cos(b), y0 + 2 + laag_ * 4, cz + (r + 2) * math.sin(b))
            W.buis([p0, p1], [2.6 - laag_ * 0.3, 2.2], None, mixfn=mix)
    W.bol((cx, y0 - 3, cz), 26, None, ry=8, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:hay_block", "guhs:roze_hooibaal", "minecraft:light_gray_wool", "minecraft:pink_wool",
         "minecraft:white_wool", "guhs:pluiswolblok"], [3, 2, 2, 2, 1, 1], x, y, z, 91), ymax=y0 - 1)
    # lege eierkuil
    W.bol((cx, y0 + 1, cz), 7, None, ry=4)
    W.bol((cx, y0 - 1, cz), 7.5, "minecraft:light_gray_wool", ry=3, ymax=y0 - 1)
    # grote losse veren (roze en grauw) die uit het nest steken
    for k, (a, kl) in enumerate(((0.3, "minecraft:pink_wool"), (1.4, "minecraft:light_gray_wool"),
                                 (2.6, "minecraft:pink_concrete"), (3.5, "minecraft:light_blue_terracotta"),
                                 (4.6, "minecraft:pink_wool"), (5.5, "minecraft:white_wool"))):
        p0 = (cx + 30 * math.cos(a), y0 + 6, cz + 30 * math.sin(a))
        p1 = (cx + 40 * math.cos(a + 0.15), y0 + 15 + 3 * (k % 2), cz + 40 * math.sin(a + 0.15))
        W.buis([p0, p1], [1.6, 0.8], kl)
    W._nest = (cx, y0, cz)


def bloesemwaterval(W):
    """Een waterval met bloesem die over de kruinrand de diepte in valt (zuidzuidoost)."""
    a = math.radians(108)
    rr = float(kruinrand(np.array([a]))[0]) + 3
    x, z = rr * math.cos(a), rr * math.sin(a)
    if not W.raakt(x - 30, 60, z - 30, x + 30, 230, z + 30):
        return
    t = (-math.sin(a), math.cos(a))
    for k in range(-4, 5):
        xx, zz = x + t[0] * k, z + t[1] * k
        for y in range(70, 205):
            if hash3(xx, y, zz, 92) < 0.15:
                W.wset(xx, y, zz, "minecraft:cherry_leaves")
            else:
                W.wset(xx, y, zz, "minecraft:water")
    W.bol((x, 64, z), 16, None, ry=5)
    W.bol((x, 61, z), 16, "minecraft:water", ry=3, ymax=63)


# ===================================================================================================== takken
def _spline(pts, n=10):
    P = np.asarray(pts, float)
    out = []
    for i in range(len(P) - 1):
        p0 = P[i - 1] if i > 0 else P[i]
        p1, p2 = P[i], P[i + 1]
        p3 = P[i + 2] if i + 2 < len(P) else P[i + 1]
        for t in np.linspace(0, 1, n, endpoint=False):
            t2, t3 = t * t, t * t * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 +
                              (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    out.append(P[-1])
    return np.array(out)


def tak_lijn(naam):
    pts, rads = TAKKEN[naam]
    L = _spline(pts, 12)
    rr = np.interp(np.linspace(0, 1, len(L)), np.linspace(0, 1, len(rads)), rads)
    if naam != "oost":   # [zone 3 kaart] varied thickness (knots and thin necks), Honingtak's branch stays as designed
        k = np.arange(len(L))
        rr = rr * (1 + 0.13 * np.sin(k * 0.21 + len(naam)) + 0.06 * np.sin(k * 0.53 + 2 * len(naam)))
    return L, rr


def tak_top(naam, x, z):
    """Hoogte van het rijpad bovenop tak 'naam' bij (x, z) (dichtstbijzijnde punt op de lijn)."""
    L, rr = tak_lijn(naam)
    d = np.hypot(L[:, 0] - x, L[:, 2] - z)
    i = int(np.argmin(d))
    return L[i, 1] + rr[i] * 0.72


def takken(W):
    if W.b[4] < 130:
        return
    for naam in TAKKEN:
        L, rr = tak_lijn(naam)
        lo, hi = L.min(0) - rr.max() - 30, L.max(0) + rr.max() + 30
        if not W.raakt(*lo, *hi):
            continue
        W.buis(L, rr, None, mixfn=_takmix(W, 100 + len(naam)), plat=0.72)
        tak_bovenlaag(W, L, rr, pad=(naam != "oost"))
        if naam != "oost":
            tak_leven(W, naam, L, rr)
    for (p0, p1, r0, r1) in STOMPEN:
        mid = ((p0[0] + p1[0]) / 2, max(p0[1], p1[1]) + 6, (p0[2] + p1[2]) / 2)
        W.buis(_spline([p0, mid, p1], 6), np.linspace(r0, r1, 13), None, mixfn=_takmix(W, 120), plat=0.8)
        W.bol(p1, r1 * 2.6, None, ry=r1 * 1.4, mixfn=lambda x, y, z, m: W.kies(BLOESEM + BLAD, [3, 2, 1, 3, 2, 2, 1, 1],
                                                                               x, y, z, 121), keep=True)
    L = _spline(*VALBRUG[:1], 8) if False else _spline(VALBRUG[0], 8)
    W.buis(L, np.interp(np.linspace(0, 1, len(L)), [0, 1], [7, 6]), None, mixfn=_takmix(W, 130), plat=0.8)
    tak_bovenlaag(W, L, np.full(len(L), 6.0), pad=True, padbreed=1.5)


def _takmix(W, seed):
    def f(x, y, z, m):
        ids = W.kies(SCHORS, SCHORS_W, x, y, z, seed)
        rm = rot_mask(W, x, y, z, extra=-0.15)
        return np.where(rm, W.kies(ROT, ROT_W, x, y, z, seed + 1), ids)
    return f


def tak_bovenlaag(W, L, rr, pad=True, padbreed=2.6):
    """Mos op de rug van een tak, met een aarden rijpad in het midden."""
    lo = L.min(0) - rr.max() - 2
    hi = L.max(0) + rr.max() + 2
    X, Y, Z = W.v.shape
    a, b = max(0, W.vx(lo[0])), min(X, W.vx(hi[0]) + 1)
    c, d = max(0, W.vy(lo[1])), min(Y, W.vy(hi[1]) + 1)
    e, f = max(0, W.vz(lo[2])), min(Z, W.vz(hi[2]) + 1)
    if a >= b or c >= d or e >= f:
        return
    sub = W.v[a:b, c:d, e:f]
    schors = np.isin(sub, W.ids(SCHORS))
    lucht_boven = np.zeros_like(schors)
    lucht_boven[:, :-1, :] = sub[:, 1:, :] == 0
    top = schors & lucht_boven
    xs, zs = W.xs[a:b], W.zs[e:f]
    XX, ZZ = np.meshgrid(xs, zs, indexing="ij")
    # afstand tot de lijn (in xz), en de lokale straal
    dmin = np.full(XX.shape, 1e9)
    rloc = np.zeros(XX.shape)
    for i in range(0, len(L), 2):
        dd = np.hypot(XX - L[i, 0], ZZ - L[i, 2])
        better = dd < dmin
        dmin = np.where(better, dd, dmin)
        rloc = np.where(better, rr[i], rloc)
    mos = dmin < rloc * 0.62
    padm = dmin < padbreed if pad else np.zeros_like(mos)
    mos_ids = W.kies(MOS, MOS_W, XX, 0, ZZ, 140)
    for j in range(sub.shape[1]):
        t = top[:, j, :]
        s = sub[:, j, :]
        m1 = t & mos
        s[m1] = mos_ids[m1]
        m2 = t & padm
        s[m2] = W.id("minecraft:dirt_path")


def tak_leven(W, naam, L, rr):
    """Lianen, glimbessen en sapdraden onder de tak; bladerpluimen ernaast."""
    rng = np.random.default_rng(len(naam) * 7)
    for i in range(6, len(L) - 2, 3):
        p = L[i]
        r = rr[i]
        if not W.raakt(p[0] - 40, p[1] - 60, p[2] - 40, p[0] + 40, p[1] + 40, p[2] + 40):
            continue
        for _ in range(int(5 * max(1, r / 10))):
            ox, oz = rng.uniform(-r * 0.7, r * 0.7, 2)
            yb = p[1] - r * 0.6
            n = int(rng.integers(5, 26))
            soort = "minecraft:cave_vines_plant[berries=true]" if rng.random() < 0.35 else \
                "minecraft:cave_vines_plant[berries=false]"
            for k in range(n):
                W.wset(p[0] + ox, yb - k, p[2] + oz, soort, keep=True)
        # zijtakje dat omhoog/opzij krult, met een bloesemkussen
        if i % 8 == 4:
            d = L[min(i + 1, len(L) - 1)] - L[i - 1]
            nrm = np.array([-d[2], 0, d[0]])
            nrm /= (np.linalg.norm(nrm) + 1e-9)
            zijde = -1 if (i // 8) % 2 else 1
            q0 = p + nrm * zijde * r * 0.5
            q1 = p + nrm * zijde * (r + 14) + np.array([0, 10, 0])
            q2 = p + nrm * zijde * (r + 26) + np.array([0, 24, 0]) + (d / (np.linalg.norm(d) + 1e-9)) * 8
            W.buis(_spline([q0, q1, q2], 6), np.linspace(5, 2, 13), None, mixfn=_takmix(W, 155 + i))
            W.bol(q2 + np.array([0, 4, 0]), 12, None, ry=7, keep=True,
                  mixfn=lambda x, y, z, m: W.kies(BLOESEM + BLAD, [4, 2, 1, 2, 1, 2, 1, 1], x, y, z, 156 + i))
        # bladerpluim opzij (niet op het pad)
        if i % 6 == 0:
            zijde = 1 if (i // 6) % 2 else -1
            d = L[min(i + 1, len(L) - 1)] - L[i - 1]
            nrm = np.array([-d[2], 0, d[0]])
            nrm /= (np.linalg.norm(nrm) + 1e-9)
            c = p + nrm * zijde * (r + 4) + np.array([0, r * 0.3, 0])
            W.bol(c, 11 + (i % 4) * 2, None, ry=7, keep=True,
                  mixfn=lambda x, y, z, m: W.kies(BLOESEM + BLAD, [3, 2, 1, 3, 2, 2, 1, 1], x, y, z, 150 + i))


# ===================================================================================================== onder: het Wortelduister
HARTSLAG = (0, 12, 0, 62, 34)    # midden x, vloer-y, z, straal, hoogte
WORTELGANG = [(0, 64, 268), (40, 58, 230), (90, 48, 170), (88, 36, 110), (50, 26, 66)]


def wortelduister(W):
    if W.b[1] > 64 or not W.raakt(-460, 0, -460, 460, 66, 460):
        return
    X, Y, Z = W.v.shape
    sx = slice(max(0, W.vx(-460)), min(X, W.vx(460) + 1))
    sz = slice(max(0, W.vz(-460)), min(Z, W.vz(460) + 1))
    XX, ZZ = W.XX[sx, sz], W.ZZ[sx, sz]
    RR = np.hypot(XX, ZZ)
    for j in range(Y):
        y = W.ys[j]
        if y < 6 or y > 60:
            continue
        sl = W.v[sx, j, sz]
        # grotten: 3D-ruis, groter naar het midden, binnen r<=450
        n = vnoise3(XX, y * 1.6, ZZ, cell=58, seed=201, oct=2)
        drempel = 0.06 + 0.25 * np.clip(RR / 450.0, 0, 1) ** 2 + 0.15 * np.clip((y - 50) / 10.0, 0, 1) + \
            0.2 * np.clip((14 - y) / 8.0, 0, 1)
        grot = (np.abs(n) > drempel) & (RR < 450)
        grot &= ~((n > 0.5) & (RR < 440))  # dikke pilaren laten staan
        sl[grot] = 0
    # Hartslag-zaal en de Grote Wortelgang
    cx, fy, cz, r, h = HARTSLAG
    W.bol((cx, fy + h * 0.45, cz), r, None, ry=h * 0.62)
    W.bol((cx, fy + h * 0.45, cz), r * 0.75, None, ry=h * 0.8)
    L = _spline(WORTELGANG, 10)
    W.buis(L, np.full(len(L), 9.0), None, plat=1.1)
    # wanden aankleden: rotsen, roze aders, mos, schimmel
    wortelduister_aankleden(W, sx, sz)


def wortelduister_aankleden(W, sx, sz):
    X, Y, Z = W.v.shape
    XX, ZZ = W.XX[sx, sz], W.ZZ[sx, sz]
    RR = np.hypot(XX, ZZ)
    steen = W.ids(["minecraft:stone", "minecraft:tuff", "minecraft:deepslate", "minecraft:andesite",
                   "minecraft:dirt"])
    ader = W.id("guhs:weidekristal_roze")
    ader2 = W.id("guhs:guh_kristal_blok")
    for j in range(1, Y - 1):
        y = W.ys[j]
        if y < 5 or y > 64:
            continue
        sl = W.v[sx, j, sz]
        boven = W.v[sx, j + 1, sz]
        onder = W.v[sx, j - 1, sz]
        vast = np.isin(sl, steen)
        if not vast.any():
            continue
        # gloeiende roze aders: dunne banden in de 3D-ruis, feller (dichter) naar het midden
        n = vnoise3(XX, y * 1.2, ZZ, cell=20, seed=210, oct=1)
        dicht = 0.035 + 0.05 * np.clip(1 - RR / 450.0, 0, 1)
        aders = vast & (np.abs(n) < dicht) & (RR < 470)
        # alleen zichtbare blokken (grenzen aan lucht boven/onder of opzij)
        rand_ = (boven == 0) | (onder == 0)
        rand_[1:, :] |= sl[:-1, :] == 0
        rand_[:-1, :] |= sl[1:, :] == 0
        rand_[:, 1:] |= sl[:, :-1] == 0
        rand_[:, :-1] |= sl[:, 1:] == 0
        sl[aders & rand_] = np.where(hash3(XX, y, ZZ, 211)[aders & rand_] < 0.7, ader, ader2)
        # vloer: mos, paarse zwammetjes, schimmel; plafond: schimmeldoek
        vloer = vast & (boven == 0) & ~(aders & rand_)
        vl = W.kies(["minecraft:moss_block", "guhs:vadsmos", "minecraft:tuff", "guhs:glimmos",
                     "minecraft:light_blue_terracotta", "minecraft:pale_moss_block"], [3, 2, 2, 0.6, 1.2, 1],
                    XX, y, ZZ, 212)
        sl[vloer] = vl[vloer]
        bovenop = vloer & (hash3(XX, y, ZZ, 213) < 0.05)
        boven[bovenop & (boven == 0)] = np.where(
            hash3(XX, y, ZZ, 214)[bovenop & (boven == 0)] < 0.6, W.id("guhs:glimpaddenstoel"),
            W.id("guhs:glimplant"))
        plafond = vast & (onder == 0)
        schimmel = plafond & (hash3(XX, y, ZZ, 215) < 0.08)
        sl[plafond & rot_mask(W, XX, y, ZZ, 0.15)] = W.id("minecraft:light_blue_terracotta")
        hang = schimmel & (onder == 0)
        onder[hang] = W.id("minecraft:pale_hanging_moss[tip=false]")
    # sapmeren op de laagste vloeren
    for j in range(Y):
        y = W.ys[j]
        if y < 8 or y > 10:   # [zone 3 kaart] shallow sap lakes (walkability: no deep pits of water)
            continue
        sl = W.v[sx, j, sz]
        sl[(sl == 0) & (RR < 450) & (RR > 70)] = W.id("minecraft:water")


# ===================================================================================================== alles
DELEN = ("terrein", "wortels", "wortelduister", "stam", "takken", "kruin", "wolken", "bosjes")


def bouw(W, delen=None):
    for d in (delen or DELEN):
        globals()[d](W)
    return W
