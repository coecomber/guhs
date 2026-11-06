"""Zone 3 "De Schemerwortel" — REVAMP (ronde 20): een smalle, hoge wereldboom in 4 lagen.

Coördinaten t.o.v. de stamas (spel: x + 2300, z + 0); y = echte spel-y. Zonevak: -400..400 x -400..400 (800 x 800).
Elke functie vult alleen wat in het venster valt; hetzelfde script bouwt overzicht (k=0.5) en close-ups (k=1).

De 4 lagen (y):
  WORTELS   4..78    het Wortelduister onder de stam + het Hart van de Boom (ingang: de Wortelpoort, oost)
  STAM     80..160   de Schorsstad BINNEN in de holle stam (Ø ~46 binnen, ~70 buiten), grote wenteltrap
  TAKKEN  160..216   Honingtak (ringstad rond de stam op y 166) + reuzentakken met imkerdorpjes + de Werftak
  KRUIN   216..312   3 getrapte verdiepingen (r 160 / 112 / 68) en het nest bovenop de stam (y ~292)

Verbindingen (elk sluit boven en onder aan; zie de constanten):
  TRAP     wenteltrap binnen in de stam, 18 blokken per slag, van de Schorsstadvloer (80) tot in het nest (290);
           bordessen + deur + laagbord op 80 / 166 / 236 / 262 / 286.
  RIJPAD   spiraal-rijpad om de buitenkant, 34 per slag, van het maaiveld (82) via Honingtak (166) tot op kruin 1 (236).
  LIFTEN   honingliften: A maaiveld -> Honingtak, B Honingtak -> kruin 1.
  WIND     bloesemwindkolommen: takken -> kruin 1 -> kruin 2 -> kruin 3.
  Werf     de Kaaszeppelinwerf is een los eiland (werf2.py), via een kabelbrug vanaf de Werftak.

    bouw(W, delen=None)
"""
import math
import numpy as np
import schemerkit as SK
from schemerkit import vnoise, vnoise3, hash3

# ===================================================================================================== maten
G = 66                 # basis-maaiveld
VOET = 80              # maaiveld bij de stam = vloer van de Schorsstad
EILAND_R = 222
R_IN = 23              # holle stam binnen
NEST_Y = 292
HONING_Y = 166         # dek van Honingtak
# kruinverdiepingen: straal, bovenkant bij de stam, dikte
# [zone 3 kaart] the user: a bit LESS WIDE (was 160/112/68) and more clearly stepped (more height between the storeys)
KRUIN = [dict(R=130, top=232, dik=18, seed=1), dict(R=95, top=260, dik=13, seed=2), dict(R=60, top=284, dik=10, seed=3)]
LAGEN = [("WORTELS", 4, 78, (150, 120, 230)), ("STAM", 80, 160, (170, 110, 60)),
         ("TAKKEN + Honingtak", 160, 216, (240, 170, 50)), ("KRUIN + nest", 216, 312, (250, 140, 190))]
TH_M = math.pi / 2     # zuid: hier komen trap, rijpad en Honingtak-dek samen (Honingplein)
TRAP = dict(y0=VOET, stijg=18.0, r1=16.0, top=float(NEST_Y))
TRAP["th0"] = TH_M - 2 * math.pi * (((HONING_Y - VOET) / TRAP["stijg"]) % 1.0)
BORDES = [(80, "STAM - Schorsstad"), (166, "TAKKEN - Honingtak"), (232, "KRUIN 1"), (260, "KRUIN 2"),
          (284, "KRUIN 3 - nest")]
RIJ = dict(y0=82.0, stijg=34.0, eind=232.0, breed=9.0)   # [zone 3 kaart] 9 wide, ends on kruin 1 (232)
RIJ["th0"] = TH_M - 2 * math.pi * (((HONING_Y - RIJ["y0"]) / RIJ["stijg"]) % 1.0)

SCHORS = ["minecraft:spruce_wood", "minecraft:dark_oak_wood", "minecraft:jungle_wood", "minecraft:oak_wood",
          "minecraft:mangrove_wood"]
SCHORS_W = [4, 1.5, 1.6, 1.4, 0.5]
MOS = ["minecraft:moss_block", "guhs:vadsmos", "minecraft:moss_block", "minecraft:mossy_cobblestone",
       "guhs:glimmos"]
MOS_W = [6, 2, 2, 0.6, 0.3]
BLAD = ["minecraft:oak_leaves", "minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves",
        "minecraft:jungle_leaves", "minecraft:birch_leaves"]
BLAD_W = [4, 2.5, 2, 1.2, 1]
BLOESEM = ["minecraft:cherry_leaves", "guhs:guhbloesem_leaves", "minecraft:flowering_azalea_leaves"]
BLOESEM_W = [5, 3, 1]
GOUD = ["minecraft:honeycomb_block", "minecraft:acacia_leaves", "minecraft:oak_leaves", "guhs:kristalkaas_blok"]
GOUD_W = [2, 3, 2, 0.5]
PLANK, HOUT, HEK = "minecraft:spruce_planks", "minecraft:stripped_spruce_wood", "minecraft:spruce_fence"
LAMP_R, LAMP_G = "guhs:lampion_roze", "guhs:lampion_geel"
LANTAARN = "minecraft:lantern[hanging=false,waterlogged=false]"
HANGLAMP = "minecraft:lantern[hanging=true,waterlogged=false]"


def wrap(a):
    return (a + np.pi) % (2 * np.pi) - np.pi


def mix(W, blokken, gew, seed):
    return lambda x, y, z, m: W.kies(blokken, gew, x, y, z, seed)


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


def pol(r, a, y):
    return (r * math.cos(a), y, r * math.sin(a))


# ===================================================================================================== terrein
LOBBEN = [(math.radians(184), 30.0, 0.32), (math.radians(19), 26.0, 0.30), (math.radians(110), 14.0, 0.4)]   # [wereldindeling]


def eilandrand(th):
    r = EILAND_R + 16 * vnoise(np.cos(th) * 300, np.sin(th) * 300, cell=90, seed=7, oct=2)
    for (a, h, w) in LOBBEN:     # root capes under the two piers (west to zone 2, east to zone 4) and a small one south
        r = r + h * np.exp(-(wrap(th - a) / w) ** 2)
    return r


def grondhoogte(XX, ZZ):
    XX, ZZ = np.asarray(XX, float), np.asarray(ZZ, float)
    r = np.hypot(XX, ZZ)
    th = np.arctan2(ZZ, XX)
    g = G + 2.5 * vnoise(XX, ZZ, cell=60, seed=11) + 1.2 * vnoise(XX, ZZ, cell=14, seed=12)
    g += 4 * np.clip(vnoise(XX, ZZ, cell=40, seed=13), 0, 1)                 # heuveltjes
    g += 14 * np.clip(1 - (r - 40) / 150.0, 0, 1) ** 1.6                     # wortelheuvel naar de stam
    rand = eilandrand(th)
    t = np.clip((r - (rand - 55)) / 55.0, 0, 1.4)
    g -= 20 * t ** 2                                                       # zachte schouder naar de rand
    return g


def onderkant(XX, ZZ, g):
    r = np.hypot(XX, ZZ)
    rand = eilandrand(np.arctan2(ZZ, XX))
    q = np.clip(r / rand, 0, 1)
    b = g - 6 - 100 * np.clip(1 - q, 0, 1) ** 0.85 + 6 * vnoise(XX, ZZ, cell=30, seed=17) * q
    return np.maximum(b, 2)


def terrein(W):
    if W.b[1] > 100:
        return
    XX, ZZ, RR, TH = W.XX, W.ZZ, W.RR, W.TH
    g = grondhoogte(XX, ZZ)
    rand = eilandrand(TH)
    eil = RR <= rand
    b = onderkant(XX, ZZ, g)

    def rots(y, m):
        ids = W.kies(["minecraft:stone", "minecraft:tuff", "minecraft:deepslate", "minecraft:andesite",
                      "minecraft:dirt", "minecraft:mud"], [4, 3, 2, 1, 1, 0.6], XX, np.floor(y / 3.0) + XX * 0, ZZ, 5)
        # onderkant: paars-blauwe wortelsteen en hangend mos
        laag_ = y < b + 3
        ids = np.where(laag_, W.kies(["minecraft:deepslate", "minecraft:mangrove_roots", "minecraft:dark_oak_wood",
                                      "minecraft:purple_terracotta"], [3, 2, 2, 1], XX, y, ZZ, 6), ids)
        # bovenrand van de flank: mos en gras dat over de rand hangt
        mosr = (y > g - 9) & (RR > rand - 40)
        return np.where(mosr, W.kies(["minecraft:moss_block", "minecraft:grass_block", "minecraft:dirt",
                                      "minecraft:rooted_dirt"], [3, 2, 1, 1], XX, y, ZZ, 7), ids)
    W.kolom(b, g - 4, None, mask=eil, laagfn=rots)
    W.kolom(g - 3.5, g - 1, "minecraft:dirt", mask=eil)
    onder_kruin = RR < 150
    top = W.kies(["minecraft:grass_block", "minecraft:grass_block", "guhs:knuffelgras", "minecraft:moss_block"],
                 [5, 3, 1.2, 0.6], XX, 1, ZZ, 21)
    schaduw = W.kies(["minecraft:moss_block", "minecraft:podzol", "minecraft:rooted_dirt", "guhs:vadsmos",
                      "minecraft:moss_block", "minecraft:coarse_dirt"], [4, 2, 1, 1.5, 1, 0.6], XX, 1, ZZ, 22)
    top = np.where(onder_kruin, schaduw, top)
    W.kolom(g, g, top, mask=eil)
    W._g = g
    # plantjes: gras, varens, bloemen (alleen bij k=1 zichtbaar genoeg)
    if W.k >= 1 and W.raakt(-400, G - 30, -400, 400, G + 30, 400):
        h = hash3(XX, 3, ZZ, 23)
        planten = W.kies(["minecraft:short_grass", "minecraft:fern", "minecraft:pink_petals[facing=north,flower_amount=4]",
                          "minecraft:allium", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:dandelion"],
                         [6, 3, 2, 1, 1, 1, 1], XX, 4, ZZ, 24)
        onder = W.kies(["minecraft:fern", "minecraft:short_grass", "guhs:glimplant", "minecraft:brown_mushroom",
                        "minecraft:red_mushroom", "guhs:glimpaddenstoel"], [4, 3, 1, 1, 1, 0.8], XX, 4, ZZ, 25)
        planten = np.where(onder_kruin, onder, planten)
        W.kolom(g + 1, g + 1, planten, mask=eil & (h < 0.32), keep=True)


def wortels_boven(W):
    """Bovengrondse wortels: 9 wortelruggen uit de stamvoet, die zich splitsen en in de grond duiken."""
    if W.b[1] > 130:
        return
    rng = np.random.default_rng(31)
    for i in range(9):
        a = 2 * math.pi * i / 9 + 0.3 + rng.uniform(-0.12, 0.12)
        L = rng.uniform(150, 205)
        pts = []
        for t in np.linspace(0, 1, 7):
            s = 34 + L * t
            aa = a + 0.12 * math.sin(t * 5 + i)
            boog = 7 * max(0.0, math.sin(t * 8 + i)) ** 2 * (t > 0.3)
            y = VOET - 2 + boog - 22 * t ** 1.3 + 8 * (1 - t) ** 3
            pts.append(pol(s, aa, y))
        Lp = _spline(pts, 8)
        rr = np.interp(np.linspace(0, 1, len(Lp)), [0, 0.3, 1], [17, 10, 3])
        W.buis(Lp, rr, None, mixfn=_wortelmix(W, 40 + i), plat=0.75)
        for zij in (-1, 1):
            if rng.random() < 0.55:
                continue
            t0 = rng.uniform(0.3, 0.5)
            p0 = Lp[int(t0 * len(Lp))]
            aa = a + zij * rng.uniform(0.25, 0.45)
            p1 = (p0[0] + 50 * math.cos(aa), p0[1] - 6, p0[2] + 50 * math.sin(aa))
            p2 = (p0[0] + 90 * math.cos(aa + zij * 0.1), p0[1] - 18, p0[2] + 90 * math.sin(aa + zij * 0.1))
            W.buis(_spline([p0, p1, p2], 6), np.linspace(7, 2, 13), None, mixfn=_wortelmix(W, 60 + i), plat=0.8)


def _wortelmix(W, seed):
    def f(x, y, z, m):
        ids = W.kies(SCHORS, SCHORS_W, x, y, z, seed)
        g = grondhoogte(x[:, 0, :], z[:, 0, :])[:, None, :]
        mos = (y > g + 4) & (hash3(x, y, z, seed + 7) < 0.7) & (vnoise(x, z, cell=9, seed=seed) > -0.1)
        return np.where(mos, W.kies(MOS, MOS_W, x, y, z, seed + 8), ids)
    return f


def _takmix(W, seed, mos=0.25):
    def f(x, y, z, m):
        ids = W.kies(SCHORS, SCHORS_W, x, y, z, seed)
        mo = vnoise3(x, y, z, cell=10, seed=seed) > 0.45 - mos
        return np.where(mo & (hash3(x, y, z, seed + 1) < 0.6), W.kies(MOS, MOS_W, x, y, z, seed + 2), ids)
    return f


# ===================================================================================================== stam
def r_stam(y, th):
    y = np.asarray(y, float)
    basis = 36 - 8 * np.clip((y - 90) / 200.0, 0, 1)
    lob = (0.55 + 0.45 * np.cos(7 * (th - 0.3))) ** 2
    voet = 30 * np.exp(-np.clip(y - 74, 0, None) / 14.0) * lob
    rib = 1 + 0.045 * np.sin(11 * th + y / 26.0) + 0.025 * np.sin(23 * th - y / 15.0)
    return (basis + voet) * rib


# [zone 3 kaart] the inner spiral stair, rebuilt: ONE stair from the Schorsstad floor (80) to the nest floor (292) with a flat
# landing (bordes) per layer; the steps are HALF blocks (a full block or a bottom slab per stand height), so you never jump.
# lev(phi) = the level (top block y) at helix angle phi (0 = start of the floor landing); the 166 landing is centred on TH_M.
TRAP_LANDINGEN = [80, 166, 232, 260, 284, NEST_Y]   # the last = the nest floor (top block NEST_Y, stand NEST_Y + 1)
TRAP_WL = 0.62                       # landing arc (rad), ~11 blocks along the middle of the stair
TRAP_R0, TRAP_R1 = 16.0, R_IN + 1.0  # stair from the rail (r 16) to the wall


def _trap_knopen():
    a, out = 0.0, []
    for i, L in enumerate(TRAP_LANDINGEN):
        out.append((a, L))
        if i + 1 < len(TRAP_LANDINGEN):
            a += TRAP_WL + 2 * math.pi * (TRAP_LANDINGEN[i + 1] - L) / TRAP["stijg"]
    return out


_KNOPEN = _trap_knopen()
TRAP_EIND = _KNOPEN[-1][0] + TRAP_WL
TRAP_TH0 = TH_M - (_KNOPEN[1][0] + TRAP_WL / 2)


def trap_lev(phi):
    """Level (top block y, continuous) of the stair at helix angle phi (array); NaN outside the stair."""
    phi = np.asarray(phi, float)
    out = np.full(phi.shape, np.nan)
    for i, (a, L) in enumerate(_KNOPEN):
        m = (phi >= a) & (phi <= a + TRAP_WL)
        out = np.where(m, L, out)
        if i + 1 < len(_KNOPEN):
            a2, L2 = _KNOPEN[i + 1]
            m = (phi > a + TRAP_WL) & (phi < a2)
            out = np.where(m, L + (L2 - L) * (phi - a - TRAP_WL) / (a2 - a - TRAP_WL), out)
    return out


def trap_hoek(y):
    """Angle (absolute, not wrapped) where the stair has level y (a landing: its middle)."""
    for i, (a, L) in enumerate(_KNOPEN):
        if abs(y - L) < 1e-6:
            return TRAP_TH0 + a + TRAP_WL / 2
        if i + 1 < len(_KNOPEN) and L < y < _KNOPEN[i + 1][1]:
            a2, L2 = _KNOPEN[i + 1]
            return TRAP_TH0 + a + TRAP_WL + (a2 - a - TRAP_WL) * (y - L) / (L2 - L)
    return TRAP_TH0


def trap_landing(i):
    """(angle of the middle, level) of landing i."""
    a, L = _KNOPEN[i]
    return TRAP_TH0 + a + TRAP_WL / 2, L


def trap_hoogtes(th):
    """Levels of the stair at absolute angle th (list of arrays, one per turn; NaN where none)."""
    f = (np.asarray(th, float) - TRAP_TH0) % (2 * np.pi)
    return [trap_lev(f + 2 * np.pi * k) for k in range(int(TRAP_EIND / (2 * np.pi)) + 2)]


def stand(lev):
    """Stand height for a level, quantized to half blocks (a full block at sh-1, or a bottom slab at floor(sh))."""
    return np.floor(2 * (np.asarray(lev, float) + 1) + 1e-6) / 2


def rij_hoogtes(th):
    p = RIJ
    f = ((th - p["th0"]) % (2 * np.pi)) / (2 * np.pi)
    out = []
    for k in range(6):
        h = p["y0"] + p["stijg"] * (f + k)
        out.append(np.where(h <= p["eind"], np.floor(h), np.nan))
    return out


def rij_punt(t_slagen):
    """Wereldpunt op het rijpad na t slagen (voor de ruiter en de camera)."""
    a = RIJ["th0"] + 2 * math.pi * t_slagen
    y = RIJ["y0"] + RIJ["stijg"] * t_slagen
    r = float(r_stam(y, np.array([a]))[0]) + 2.2
    return (r * math.cos(a), math.floor(y) + 1, r * math.sin(a)), a


def stam(W):
    if not W.raakt(-110, 30, -110, 110, NEST_Y + 2, 110):
        return
    X, Y, Z = W.v.shape
    sx = slice(max(0, W.vx(-110)), min(X, W.vx(110) + 1))
    sz = slice(max(0, W.vz(-110)), min(Z, W.vz(110) + 1))
    XX, ZZ = W.XX[sx, sz], W.ZZ[sx, sz]
    RR, TH = np.hypot(XX, ZZ), np.arctan2(ZZ, XX)
    korrel = TH * 40 + 0.0
    streep = hash3(np.round(TH * 70), 0, 0, 62) < 0.16
    pl, plr, hk = W.id("minecraft:birch_planks"), W.id("minecraft:stripped_birch_wood"), W.id("minecraft:birch_fence")
    for j in range(Y):
        y = W.ys[j]
        if y < 58 or y > NEST_Y + 1:
            continue
        Ro = r_stam(y, TH - y / 160.0)
        m = RR <= Ro
        sl = W.v[sx, j, sz]
        hol = VOET < y < NEST_Y + 1
        if hol:
            sl[RR <= R_IN] = 0
            m &= RR > R_IN
        if not m.any():
            continue
        ids = W.kies(SCHORS, SCHORS_W, XX, y, ZZ, 61)
        ids = np.where(streep, W.id("minecraft:dark_oak_wood"), ids)
        buiten = RR > Ro - 2.5
        # mosplekken, harsdruppels (amber) en barsten met roze gloed op de schors
        mosv = (vnoise3(XX, y * 0.7, ZZ, cell=14, seed=63) > 0.22) & buiten
        ids = np.where(mosv, W.kies(MOS, MOS_W, XX, y, ZZ, 64), ids)
        hars = (vnoise(korrel, y, cell=7, seed=65) > 0.66) & buiten
        ids = np.where(hars, W.kies(["minecraft:resin_block", "minecraft:resin_block", "minecraft:honey_block"],
                                    [2, 1, 1], XX, y, ZZ, 66), ids)
        sl[m] = ids[m]
        if hol:
            wand = m & (RR <= R_IN + 2.2)
            wids = W.kies(["minecraft:spruce_wood", "minecraft:dark_oak_wood", "minecraft:stripped_dark_oak_wood",
                           "minecraft:stripped_spruce_wood", "minecraft:resin_block"], [4, 3, 2, 1, 0.3], XX, y, ZZ, 67)
            sl[wand] = wids[wand]
            # [zone 3 kaart] the stair itself: wenteltrap(W) at the end (after the knot holes and doors)
    if W.raakt(-30, VOET - 2, -30, 30, VOET + 1, 30):
        W.cilinder(0, 0, R_IN + 1, VOET - 1, VOET, None, mixfn=mix(W, ["minecraft:moss_block", "minecraft:rooted_dirt",
                                                                       "minecraft:spruce_planks", "guhs:vadsmos"],
                                                                   [3, 1, 2, 1], 600))
    # [zone 3 kaart] the two big ground gates (north/south) into the Schorsstad: a tunnel from the floor (80) out through the
    # trunk foot; its floor follows the ground down in half steps (bouw3.poorten), so here only the tunnel shell
    for a in POORT_HOEKEN:
        d = (math.cos(a), math.sin(a))
        W.buis([(d[0] * 18, VOET + 4, d[1] * 18), (d[0] * 64, VOET + 2, d[1] * 64)], [5.0, 4.5], None, plat=1.25)
    # knoesten/lichtgaten (kwastgaten) waar daglicht naar binnen valt
    for (a, y, r) in KWASTGATEN:
        d = (math.cos(a), math.sin(a))
        W.buis([(d[0] * (R_IN - 1), y, d[1] * (R_IN - 1)), (d[0] * 50, y + 3, d[1] * 50)], [r, r * 0.8], None)
    # bordessen + deuren + laagborden, then the stair over everything
    for i in range(len(BORDES)):
        bordes(W, i)
    wenteltrap(W)


POORT_HOEKEN = (TH_M + 0.6, TH_M + math.pi)
KWASTGATEN = [(0.4, 120, 4.5), (2.2, 140, 4), (3.9, 108, 4.5), (5.2, 150, 4), (1.2, 196, 4), (4.6, 210, 4),
              (3.0, 250, 3.5)]
TRAP_GATEN = []     # (angle, half width rad, lev min, lev max): no rail there (bridges to the middle); filled by plekken2


def _in_gat(TH, lev, gaten):
    m = np.zeros(np.shape(TH), bool)
    for (a, hw, l0, l1) in gaten:
        m |= (np.abs(wrap(TH - a)) < hw) & (lev >= l0) & (lev <= l1)
    return m


def wenteltrap(W):
    """[zone 3 kaart] The inner spiral stair (half steps, continuous rail on the inside, 2 blocks of support)."""
    if not W.raakt(-30, VOET - 2, -30, 30, NEST_Y + 8, 30):
        return
    X, Y, Z = W.v.shape
    sx = slice(max(0, W.vx(-30)), min(X, W.vx(30) + 1))
    sz = slice(max(0, W.vz(-30)), min(Z, W.vz(30) + 1))
    XX, ZZ = W.XX[sx, sz], W.ZZ[sx, sz]
    RR, TH = np.hypot(XX, ZZ), np.arctan2(ZZ, XX)
    ring = (RR >= TRAP_R0) & (RR <= TRAP_R1)
    rail = (RR >= TRAP_R0) & (RR < TRAP_R0 + 1.0)
    vol, half = W.id("minecraft:birch_planks"), W.id("minecraft:birch_slab[type=bottom,waterlogged=false]")
    steun, hek = W.id("minecraft:stripped_birch_wood"), W.id("minecraft:birch_fence")
    for lev in trap_hoogtes(TH):
        ok = ring & ~np.isnan(lev)
        if not ok.any():
            continue
        lv = np.nan_to_num(lev, nan=-100)
        sh = stand(lv)
        full = (sh % 1) == 0
        ytop = np.where(full, sh - 1, np.floor(sh)).astype(int)
        ry = np.where(full, sh, np.floor(sh) + 1).astype(int)
        gat = _in_gat(TH, lv, TRAP_GATEN)
        for j in range(Y):
            y = int(math.floor(W.ys[j]))
            if y < VOET - 4 or y > NEST_Y + 8:
                continue
            sl = W.v[sx, j, sz]
            m = ok & (y == ytop)
            sl[m & full] = vol
            sl[m & ~full] = half
            sl[ok & (y < ytop) & (y >= ytop - 2)] = steun
            sl[ok & (y > ytop) & (y <= ytop + 6)] = 0
            sl[ok & rail & ~gat & (y == ry)] = hek


def bordes(W, i):
    """[zone 3 kaart] Landing i of the stair (its flat arc is part of the stair): a door out through the wall at the layer's
    floor (Honingtak / the crown terraces), a coloured layer board + lanterns beside the door."""
    a, y = trap_landing(i)
    naam = BORDES[i][1] if i < len(BORDES) else "nest"
    if not W.raakt(-60, y - 6, -60, 60, y + 10, 60):
        return
    d = (math.cos(a), math.sin(a))
    if 0 < i < len(BORDES):
        ro = float(r_stam(y, np.array([a - y / 160.0]))[0]) - 0.5
        eind = 46 if i == 1 else ro + 6
        for rr in np.arange(R_IN - 1, eind, 0.5):
            for w in np.arange(-2.0, 2.01, 0.5):
                x, z = rr * d[0] - w * d[1], rr * d[1] + w * d[0]
                W.wset(x, y, z, PLANK)
                for h in range(1, 5):
                    W.wset(x, y + h, z, "minecraft:air")
        for s in (-1, 1):   # door frame in the outer wall + lanterns
            x, z = ro * d[0] - s * 2.8 * d[1], ro * d[1] + s * 2.8 * d[0]
            W.wfill(x, y + 1, z, x, y + 4, z, "minecraft:stripped_spruce_log[axis=y]")
            # (U-DECO: the lantern stood in the air beside the frame; now on a 2-high post outside the door)
            W.wfill(x + d[0] * 0.9, y + 1, z + d[1] * 0.9, x + d[0] * 0.9, y + 2, z + d[1] * 0.9, "minecraft:spruce_fence")
            W.wset(x + d[0] * 0.9, y + 3, z + d[1] * 0.9, LANTAARN)
    # layer board beside the door, inside: colour per layer + lantern
    kl = {"STAM": "minecraft:brown_wool", "TAKKEN": "minecraft:orange_wool", "KRUIN": "minecraft:pink_wool"}
    kleur = [v for k_, v in kl.items() if naam.startswith(k_)]
    kleur = kleur[0] if kleur else "minecraft:pink_wool"
    for s in (-1, 1):
        aa = a + s * 0.24
        x, z = (R_IN + 0.2) * math.cos(aa), (R_IN + 0.2) * math.sin(aa)
        W.wfill(x, y + 2, z, x, y + 3, z, kleur)
        W.wset(x, y + 4, z, "minecraft:spruce_planks")
        W.wset((R_IN - 0.8) * math.cos(aa), y + 4, (R_IN - 0.8) * math.sin(aa), LANTAARN)
    W.label((R_IN - 3) * d[0], y + 6, (R_IN - 3) * d[1], "trap: " + naam)


# [zone 3 kaart] rail gaps on the riding spiral (onto Honingtak, the branches, kruin 1): (angle, half width, lev min, lev max)
RIJ_GATEN = []


def rijpad(W):
    """Het spiraal-rijpad om de buitenkant (laatst gebouwd: snijdt door takken, dek en bladeren heen).
    [zone 3 kaart] a broad ramp: half steps (path block or a mud-brick slab), a fence rail with lampions, supports."""
    if not W.raakt(-120, RIJ["y0"] - 8, -120, 120, RIJ["eind"] + 10, 120):
        return
    X, Y, Z = W.v.shape
    sx = slice(max(0, W.vx(-120)), min(X, W.vx(120) + 1))
    sz = slice(max(0, W.vz(-120)), min(Z, W.vz(120) + 1))
    XX, ZZ = W.XX[sx, sz], W.ZZ[sx, sz]
    RR, TH = np.hypot(XX, ZZ), np.arctan2(ZZ, XX)
    padmix = W.kies(["minecraft:dirt_path", "minecraft:dirt_path", "minecraft:coarse_dirt", "minecraft:packed_mud",
                     "minecraft:moss_block"], [6, 3, 1, 1, 0.6], XX, 0, ZZ, 70)
    half = W.id("minecraft:mud_brick_slab[type=bottom,waterlogged=false]")
    onderhalf = W.id("minecraft:packed_mud")
    steun = W.id("minecraft:stripped_spruce_wood")
    hek = W.id("minecraft:spruce_fence")
    lampm = np.abs(((TH * 180 / np.pi) % 11) - 5.5) < 0.6
    vine_id = W.kies(["minecraft:cave_vines_plant[berries=true]", "minecraft:cave_vines_plant[berries=false]",
                      "minecraft:pale_hanging_moss[tip=false]"], [3, 2, 1], XX, 2, ZZ, 74)
    p = RIJ
    f = ((TH - p["th0"]) % (2 * np.pi)) / (2 * np.pi)
    for k in range(6):
        lev = p["y0"] + p["stijg"] * (f + k)
        ok0 = lev <= p["eind"]
        if not ok0.any():
            continue
        lev = np.where(ok0, lev, -100)
        Rr = r_stam(lev, TH - lev / 160.0)
        ri, ro = Rr - 2.5, Rr + p["breed"] - 2.5
        band = ok0 & (RR > ri) & (RR < ro)
        sh = stand(lev)
        full = (sh % 1) == 0
        ytop = np.where(full, sh - 1, np.floor(sh)).astype(int)
        ry = np.where(full, sh, np.floor(sh) + 1).astype(int)
        railm = band & (RR >= ro - 1.0) & ~_in_gat(TH, lev, RIJ_GATEN)
        for j in range(Y):
            y = int(math.floor(W.ys[j]))
            if y < p["y0"] - 8 or y > p["eind"] + 10:
                continue
            sl = W.v[sx, j, sz]
            dy = y - ytop
            sl[band & (dy > 0) & (dy <= 7)] = 0
            dek = band & (dy == 0)
            sl[dek & full] = padmix[dek & full]
            sl[dek & ~full] = half
            sl[band & ~full & (dy == -1)] = onderhalf
            onder = ok0 & (RR > Rr - 1) & (dy < 0) & (dy >= -5) & (RR < ro + dy * 1.2) & (sl == 0)
            sl[onder] = steun
            sl[railm & (y == ry)] = hek
            sl[railm & lampm & (y == ry + 1)] = W.id(LAMP_G)
            # glimbessen en lianen die over de rand van het pad hangen
            hang = ok0 & (RR >= ro - 0.2) & (RR < ro + 0.8) & (dy <= -1) & (dy > -2 - 7 * hash3(XX, 5, ZZ, 72)) & \
                (hash3(np.round(TH * 120), 0, 0, 73) < 0.25) & (sl == 0)
            sl[hang] = vine_id[hang]
    p_, a = rij_punt(0.0)
    W.label(p_[0], p_[1] + 10, p_[2], "Rijpad (begin)")


def rij_lev(th, k):
    """Level of the riding path at absolute angle th on turn k (continuous)."""
    f = ((th - RIJ["th0"]) % (2 * math.pi)) / (2 * math.pi)
    return RIJ["y0"] + RIJ["stijg"] * (f + k)


# ===================================================================================================== takken
TAKKEN = []   # gevuld door tak(): (naam, L, rr)


def tak_lijn(a, y0, lengte, stijg, r0, r1, krul=0.0, zak=0.0, n=7):
    pts = []
    for t in np.linspace(0, 1, n):
        s = 16 + lengte * t
        aa = a + krul * t * t + 0.04 * math.sin(t * 6 + a * 3)
        y = y0 + stijg * t ** 1.5 - zak * math.sin(math.pi * t)
        pts.append(pol(s, aa, y))
    L = _spline(pts, 8)
    rr = np.interp(np.linspace(0, 1, len(L)), [0, 0.15, 1], [r0, r0 * 0.75, r1])
    return L, rr


def tak_top(L, rr, s):
    """(x, y_boven, z) op de rug van de tak op afstand s van de stam."""
    d = np.hypot(L[:, 0], L[:, 2])
    i = int(np.argmin(np.abs(d - s)))
    return L[i, 0], L[i, 1] + rr[i] * 0.7, L[i, 2]


def bouw_tak(W, naam, L, rr, seed, blad=True, bovenlaag=True, plat=0.7, bladsoort="takken"):
    lo, hi = L.min(0) - rr.max() - 40, L.max(0) + rr.max() + 40
    TAKKEN.append((naam, L, rr))
    if not W.raakt(*lo, *hi):
        return
    W.buis(L, rr, None, mixfn=_takmix(W, seed), plat=plat)
    if bovenlaag:
        tak_bovenlaag(W, L, rr)
    tak_leven(W, L, rr, seed, blad=blad, bladsoort=bladsoort)


def tak_bovenlaag(W, L, rr, padbreed=2.2):
    lo = L.min(0) - rr.max() - 2
    hi = L.max(0) + rr.max() + 2
    X, Y, Z = W.v.shape
    a, b = max(0, W.vx(lo[0])), min(X, W.vx(hi[0]) + 1)
    c, d = max(0, W.vy(lo[1])), min(Y, W.vy(hi[1]) + 1)
    e, f = max(0, W.vz(lo[2])), min(Z, W.vz(hi[2]) + 1)
    if a >= b or c >= d or e >= f:
        return
    sub = W.v[a:b, c:d, e:f]
    hout = np.isin(sub, W.ids(SCHORS + MOS))
    lb = np.zeros_like(hout)
    lb[:, :-1, :] = sub[:, 1:, :] == 0
    top = hout & lb
    XX, ZZ = np.meshgrid(W.xs[a:b], W.zs[e:f], indexing="ij")
    dmin = np.full(XX.shape, 1e9)
    rloc = np.zeros(XX.shape)
    for i in range(0, len(L), 2):
        dd = np.hypot(XX - L[i, 0], ZZ - L[i, 2])
        bt = dd < dmin
        dmin = np.where(bt, dd, dmin)
        rloc = np.where(bt, rr[i], rloc)
    mos = dmin < rloc * 0.75
    padm = (dmin < padbreed) & (rloc > 5)
    mos_ids = W.kies(MOS, MOS_W, XX, 0, ZZ, 140)
    for j in range(sub.shape[1]):
        t = top[:, j, :]
        s = sub[:, j, :]
        s[t & mos] = mos_ids[t & mos]
        s[t & padm] = W.id("minecraft:dirt_path")
    # plantjes op de rug
    if W.k >= 1:
        for j in range(sub.shape[1] - 1):
            t = top[:, j, :] & mos & ~padm & (hash3(XX, j, ZZ, 141) < 0.22)
            s1 = sub[:, j + 1, :]
            pl = W.kies(["minecraft:short_grass", "minecraft:fern", "minecraft:pink_petals[facing=north,flower_amount=3]",
                         "minecraft:red_mushroom", "minecraft:brown_mushroom", "minecraft:azalea"],
                        [5, 3, 2, 0.6, 0.6, 0.8], XX, j, ZZ, 142)
            s1[t & (s1 == 0)] = pl[t & (s1 == 0)]


def bladwolk(W, c, R, soort="takken", seed=0, ry=0.6):
    """Bladerwolk: 3-5 bollen van verschillende grootte (geen platte bladmassa), met een blokmix per laag."""
    rng = np.random.default_rng(seed)
    for k in range(int(rng.integers(3, 6))):
        o = rng.normal(0, R * 0.45, 3) * np.array([1, 0.4, 1])
        r = R * rng.uniform(0.5, 0.95)
        cc = (c[0] + o[0], c[1] + o[1], c[2] + o[2])
        if W.raakt(cc[0] - r, cc[1] - r, cc[2] - r, cc[0] + r, cc[1] + r, cc[2] + r):
            W.bol(cc, r, None, ry=r * ry * rng.uniform(0.85, 1.15), hollow=3, keep=True,
                  mixfn=bladmix(W, soort, seed + k))


def bladmix(W, soort, seed):
    def f(x, y, z, m):
        yy = np.asarray(y, float) * 1.6 + np.zeros_like(x)
        n1 = vnoise3(x, yy, z, cell=18, seed=seed % 97, oct=2)
        n2 = vnoise3(x + 31, yy, z, cell=14, seed=seed % 89 + 3, oct=2)
        if soort == "kruin1":
            ids = W.kies(BLAD, BLAD_W, x, y, z, seed)
            b = n1 > 0.15
            ids = np.where(b, W.kies(BLOESEM, BLOESEM_W, x, y, z, seed + 1), ids)
            return np.where((n2 > 0.3) & ~b, W.kies(GOUD, GOUD_W, x, y, z, seed + 2), ids)
        if soort == "kruin2":
            ids = W.kies(BLOESEM, BLOESEM_W, x, y, z, seed)
            ids = np.where(n2 > 0.25, W.kies(GOUD, GOUD_W, x, y, z, seed + 2), ids)
            return np.where(n1 > 0.4, W.kies(BLAD, BLAD_W, x, y, z, seed + 5), ids)
        if soort == "kruin3":
            ids = W.kies(GOUD, GOUD_W, x, y, z, seed)
            return np.where(n1 > -0.05, W.kies(BLOESEM, BLOESEM_W, x, y, z, seed + 1), ids)
        # takken: vooral groen, wat amber en bloesem
        ids = W.kies(BLAD, BLAD_W, x, y, z, seed)
        a = n1 > 0.3
        b = n2 > 0.4
        ids = np.where(a, W.kies(["minecraft:acacia_leaves", "minecraft:oak_leaves", "minecraft:honeycomb_block"],
                                 [3, 2, 0.6], x, y, z, seed + 4), ids)
        return np.where(b, W.kies(BLOESEM, BLOESEM_W, x, y, z, seed + 5), ids)
    return f


def tak_leven(W, L, rr, seed, blad=True, bladsoort="takken"):
    """Bladerwolk aan het eind, zijtakjes met blad, lianen, tonderzwammen en nestjes."""
    rng = np.random.default_rng(seed)
    n = len(L)
    if blad:
        bladwolk(W, L[-1] + np.array([0, 4, 0]), 15 + rr[-1], bladsoort, seed + 500)
    for i in range(6, n - 3, 5):
        p, r = L[i], rr[i]
        if not W.raakt(p[0] - 40, p[1] - 40, p[2] - 40, p[0] + 40, p[1] + 40, p[2] + 40):
            continue
        d = L[min(i + 1, n - 1)] - L[i - 1]
        nrm = np.array([-d[2], 0, d[0]])
        nrm /= (np.linalg.norm(nrm) + 1e-9)
        zijde = 1 if (i // 5) % 2 else -1
        # lianen + glimbessen onder de tak
        for _ in range(int(3 + r / 3)):
            ox, oz = rng.uniform(-r * 0.6, r * 0.6, 2)
            yb = p[1] - r * 0.6
            ln = int(rng.integers(4, 18))
            soort = ["minecraft:cave_vines_plant[berries=true]", "minecraft:cave_vines_plant[berries=false]",
                     "minecraft:pale_hanging_moss[tip=false]", "minecraft:pale_hanging_moss[tip=false]"][int(rng.integers(0, 4))]
            for k in range(ln):
                W.wset(p[0] + ox, yb - k, p[2] + oz, soort, keep=True)
        # tonderzwammen op de flank
        if i % 10 == 1:
            for k in range(3):
                q = p + nrm * zijde * (r * 0.62) + np.array([0, -2 + k * 2.5, 0]) + d / (np.linalg.norm(d) + 1e-9) * k * 2
                W.bol(tuple(q + nrm * zijde * 1.5), 2.6 - k * 0.5, None, ry=0.8, keep=True,
                      mixfn=mix(W, ["minecraft:brown_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", "minecraft:shroomlight",
                                    "minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=true,west=true]"], [4, 0.4, 0.6], 150 + i))
        # zijtakje met bladerwolkje
        if i % 10 == 6:
            q0 = p + nrm * zijde * r * 0.5
            q1 = p + nrm * zijde * (r + 10) + np.array([0, 8, 0])
            q2 = p + nrm * zijde * (r + 18) + np.array([0, 16, 0]) + d / (np.linalg.norm(d) + 1e-9) * 6
            W.buis(_spline([q0, q1, q2], 5), np.linspace(max(2.5, r * 0.35), 1.5, 11), None,
                   mixfn=_takmix(W, 155 + i))
            bladwolk(W, q2 + np.array([0, 3, 0]), 8, bladsoort, seed + i, ry=0.65)


def takken(W):
    """De reuzentakken van de Takken-laag (y 176-212) en de takken onder Honingtak."""
    TAKKEN.clear()
    if W.b[4] < 140:
        return
    # Takken-laag: platformtakken die naar de kruinrand opkrullen
    for (naam, a, y0, lengte, stijg, r0, krul) in TAKLIJST:
        L, rr = tak_lijn(math.radians(a), y0, lengte, stijg, r0, 5.5, krul=krul, zak=4)
        bouw_tak(W, naam, L, rr, seed=int(a) + 7)
    # dragers van de kruinverdiepingen: korte gebogen takken die in het blad verdwijnen
    for s, kr in enumerate(KRUIN):
        if s == 0:
            continue
        for k in range(5):
            a = 2 * math.pi * k / 5 + s * 0.6
            L, rr = tak_lijn(a, kr["top"] - kr["dik"] - 6, kr["R"] * 0.75, 10, 7 - s, 3, krul=0.2)
            bouw_tak(W, "drager", L, rr, seed=900 + s * 10 + k, blad=False, bovenlaag=False)


# (naam, hoek in graden (atan2(z,x): 0 = oost, 90 = zuid), y aan de stam, lengte, stijging, straal, krul)
TAKLIJST = [("Bijentak", 52, 196, 132, 14, 11, 0.15), ("Zuidwesttak", 128, 188, 140, 20, 11, -0.1),
            ("Westtak", 196, 200, 138, 12, 12, 0.12), ("Molentak", 262, 192, 128, 18, 10, -0.15),
            ("Werftak", 315, 170, 132, 6, 12, 0.0), ("Noordtak", 352, 206, 120, 10, 9, 0.1)]


def _takken_uitlijnen():
    """[zone 3 kaart] Every big branch (not the Werftak, which merges with Honingtak's) is moved up/down (max 17) so that its
    top meets the riding path where the path passes its foot: you ride off the spiral straight onto the branch."""
    out = []
    for (naam, a, y0, lengte, stijg, r0, krul) in TAKLIJST:
        if naam == "Werftak":
            out.append((naam, a, y0, lengte, stijg, r0, krul))
            continue
        L, rr = tak_lijn(math.radians(a), 0.0, lengte, stijg, r0, 5.5, krul=krul, zak=4)
        d = np.hypot(L[:, 0], L[:, 2])
        beste = None
        for k in range(6):
            # the riding path's outer edge at this angle and height
            lev = rij_lev(math.radians(a), k)
            if lev > RIJ["eind"]:
                break
            ro = float(r_stam(lev, np.array([math.radians(a) - lev / 160.0]))[0]) + RIJ["breed"] - 2.5
            i = int(np.argmin(np.abs(d - (ro + 1.0))))
            th = math.atan2(L[i, 2], L[i, 0])
            lev = rij_lev(th, k)
            off = L[i, 1] + rr[i] * 0.7 - 0.5        # top block of the branch above its centre line (plat 0.7)
            y0n = lev - off
            if y0n > 210 or y0n < 176:     # stay under kruin 1 (the tip rises at most to ~214) and above Honingtak
                continue
            if beste is None or abs(y0n - y0) < abs(beste[0] - y0):
                beste = (y0n, th, lev)
        y0n, th, lev = beste
        out.append((naam, a, float(y0n), lengte, max(2.0, min(stijg, 214 - y0n)), r0, krul))
        RIJ_GATEN.append((th, 0.13, lev - 4, lev + 4))
    TAKLIJST[:] = out


_takken_uitlijnen()


def tak_van(naam):
    for (n, L, rr) in TAKKEN:
        if n == naam:
            return L, rr
    for (n, a, y0, lengte, stijg, r0, krul) in TAKLIJST:
        if n == naam:
            return tak_lijn(math.radians(a), y0, lengte, stijg, r0, 5.5, krul=krul, zak=4)
    raise KeyError(naam)


# ===================================================================================================== kruin
def kruin_profiel(s, XX, ZZ):
    """(top, bodem, binnen-masker, q) van kruinverdieping s.
    [zone 3 kaart] more irregular edges (lobes + stronger noise) and a FLAT terrace round the trunk at the storey's level
    (the stair doors and the riding path come out level with it)."""
    kr = KRUIN[s]
    RR, TH = np.hypot(XX, ZZ), np.arctan2(ZZ, XX)
    Rr = kr["R"] * (1 + 0.12 * vnoise(np.cos(TH) * 200, np.sin(TH) * 200, cell=50, seed=71 + s, oct=3)
                    + 0.06 * np.sin(5 * TH + s * 1.3) + 0.04 * np.sin(9 * TH + s * 2.1))
    q = np.clip(RR / Rr, 0, 1.3)
    binnen = q <= 1
    terras = 40.0 + 4 * s * 0
    qt = np.clip((RR - terras) / np.maximum(Rr - terras, 1.0), 0, 1.3)
    bult = vnoise(XX, ZZ, cell=24, seed=72 + s, oct=2) * np.clip((qt - 0.15) / 0.25, 0, 1)
    top = kr["top"] - 7 * qt ** 2.0 - 7 * np.clip((q - 0.74) / 0.26, 0, 1) ** 2 + 2.0 * bult
    h = kr["dik"] * np.clip(1 - q * q, 0, 1) ** 0.8
    bodem = top - 1.5 - h + 3 * vnoise(XX + 50, ZZ, cell=16, seed=74 + s) * np.clip(1 - q, 0, 1) * 3
    return top, bodem, binnen, q


def kruin(W):
    if W.b[4] < 200 or not W.raakt(-200, 200, -200, 200, 320, 200):
        return
    XX, ZZ = W.XX, W.ZZ
    soorten = ["kruin1", "kruin2", "kruin3"]
    W._kruin = []
    for s, kr in enumerate(KRUIN):
        top, bodem, binnen, q = kruin_profiel(s, XX, ZZ)
        f = bladmix(W, soorten[s], 300 + s * 10)
        hol_ = binnen & (q > 0.0)

        def laag(y, m, f=f):
            yy = np.full(XX.shape, y)
            return f(XX, yy, ZZ, m)
        W.kolom(bodem, top - 1, None, mask=hol_, laagfn=laag)
        # bovenkant: een wei (gras/mos) met bloemen; aan de rand blijft het blad
        wei = binnen & (q < 0.86)
        if s == 0:
            wids = W.kies(["minecraft:grass_block", "minecraft:moss_block", "guhs:knuffelgras",
                           "minecraft:flowering_azalea_leaves"], [5, 2, 1.5, 0.6], XX, 1, ZZ, 310)
        elif s == 1:
            wids = W.kies(["minecraft:pink_concrete_powder", "minecraft:cherry_leaves", "guhs:guhbloesem_leaves",
                           "minecraft:grass_block", "minecraft:pink_wool"], [3, 3, 2, 1.5, 0.6], XX, 1, ZZ, 311)
        else:
            wids = W.kies(["minecraft:honeycomb_block", "minecraft:yellow_concrete_powder", "minecraft:hay_block",
                           "minecraft:grass_block", "minecraft:acacia_leaves"], [3, 2, 2, 1, 1], XX, 1, ZZ, 312)
        W.kolom(top, top, wids, mask=wei)
        W.kolom(top, top, None, mask=binnen & ~wei, laagfn=laag)
        if W.k >= 1:
            h = hash3(XX, s, ZZ, 313)
            bl = [["minecraft:short_grass", "minecraft:pink_petals[facing=north,flower_amount=4]",
                   "minecraft:oxeye_daisy", "minecraft:allium", "minecraft:azalea"],
                  ["minecraft:pink_petals[facing=east,flower_amount=4]", "minecraft:pink_tulip", "minecraft:short_grass",
                   "minecraft:peony[half=lower]", "minecraft:allium"],
                  ["minecraft:dandelion", "minecraft:sunflower[half=lower]", "minecraft:short_grass",
                   "minecraft:pink_petals[facing=south,flower_amount=4]", "minecraft:torchflower"]][s]
            pl = W.kies(bl, [3, 3, 2, 1, 1], XX, 2, ZZ, 314 + s)
            W.kolom(top + 1, top + 1, pl, mask=wei & (h < 0.3), keep=True)
        # hangend groen + glimbessen onder de rand
        lk = binnen & (q > 0.55) & (hash3(XX, s, ZZ, 315) < 0.018 / max(W.k, 0.5))
        lengte = 2 + 9 * hash3(XX, 1, ZZ, 316)
        W.kolom(bodem - lengte, bodem - 1, W.kies(["minecraft:cave_vines_plant[berries=true]",
                                                   "minecraft:cave_vines_plant[berries=false]",
                                                   "minecraft:pale_hanging_moss[tip=false]"], [2, 3, 1], XX, 2, ZZ, 317),
                mask=lk, keep=True)
        W._kruin.append((top, bodem, binnen))
        kussens(W, s)
    kruin_inhoud(W)


def kruin_top(s, x, z):
    t, b, binnen, q = kruin_profiel(s, np.array([float(x)]), np.array([float(z)]))
    return float(t[0])


def kussens(W, s):
    """Bladkussens langs de rand en een paar op de wei: een golvend, rond silhouet (geen platte schijf)."""
    kr = KRUIN[s]
    rng = np.random.default_rng(170 + s)
    soort = ["kruin1", "kruin2", "kruin3"][s]
    n = [40, 26, 15][s]
    sch = (1.0 if s == 0 else 0.8 if s == 1 else 0.6)
    for i in range(n):
        a = 2 * math.pi * i / n + rng.uniform(-0.08, 0.08)
        rr = kr["R"] * rng.uniform(0.88, 0.99)
        R = rng.uniform(11, 19) * sch
        y = kr["top"] - 15 * sch + rng.uniform(-3, 3)
        c = (rr * math.cos(a), y, rr * math.sin(a))
        if W.raakt(c[0] - R, c[1] - R, c[2] - R, c[0] + R, c[1] + R, c[2] + R):
            W.bol(c, R, None, ry=R * rng.uniform(0.5, 0.65), hollow=3, keep=True, mixfn=bladmix(W, soort, 180 + i))
    # hangende kussens onder de verdieping: geen platte onderkant
    for i in range(n // 2):
        a = rng.uniform(0, 2 * math.pi)
        rr = kr["R"] * rng.uniform(0.45, 0.8)
        x, z = rr * math.cos(a), rr * math.sin(a)
        t, b, _, _ = kruin_profiel(s, np.array([x]), np.array([z]))
        R = rng.uniform(8, 14) * sch
        c = (x, float(b[0]) + R * 0.2, z)
        if W.raakt(c[0] - R, c[1] - R, c[2] - R, c[0] + R, c[1] + R, c[2] + R):
            W.bol(c, R, None, ry=R * 0.45, hollow=2.5, keep=True, mixfn=bladmix(W, soort, 230 + i))


VIJVERS = [(1.05, 108, 8), (1.95, 104, 7), (3.5, 110, 8), (4.85, 106, 7), (5.75, 110, 7)]   # [zone 3 kaart] 5 ponds, kruin 1


def kruin_inhoud(W):
    """Leven op de kruinverdiepingen: honingvijvers, bijenkorven, bloesemboompjes, een kampje, een ruïne."""
    rng = np.random.default_rng(401)
    # honingvijvers op kruin 1
    for (a, r, R) in VIJVERS:
        x, z = r * math.cos(a), r * math.sin(a)
        t = kruin_top(0, x, z)
        if W.raakt(x - 12, t - 4, z - 12, x + 12, t + 2, z + 12):
            t = math.floor(t)
            W.cilinder(x, z, R + 2.5, t - 2, t, "minecraft:honeycomb_block")     # [zone 3 kaart] flush rim, 1 deep
            W.cilinder(x, z, R + 3.5, t + 1, t + 4, None)
            W.cilinder(x, z, R, t, t, "minecraft:honey_block")       # flush: a honey surface (a honey pit = no jump out)
            W.label(x, t + 4, z, "honingvijver")
    # bloesemboompjes op elke verdieping (rand vrij)
    for s, kr in enumerate(KRUIN):
        n = [22, 12, 6][s]
        for i in range(n):
            a = rng.uniform(0, 2 * math.pi)
            r = rng.uniform(40 if s < 2 else 34, kr["R"] * 0.78)
            if s < 2 and r < KRUIN[s + 1]["R"] + 8:
                r = KRUIN[s + 1]["R"] + 6 + rng.uniform(0, max(1.0, kr["R"] * 0.88 - KRUIN[s + 1]["R"] - 6))
            x, z = r * math.cos(a), r * math.sin(a)
            t = kruin_top(s, x, z)
            h = rng.uniform(5, 9)
            if not W.raakt(x - 6, t, z - 6, x + 6, t + h + 6, z + 6):
                continue
            log = ["guhs:guhbloesem_log[axis=y]", "minecraft:cherry_log[axis=y]", "minecraft:birch_log[axis=y]"][i % 3]
            W.wfill(x, t + 1, z, x, t + h, z, log)
            W.bol((x, t + h + 1, z), rng.uniform(3, 4.5), None, ry=2.6, keep=True,
                  mixfn=mix(W, ["minecraft:cherry_leaves", "guhs:guhbloesem_leaves", "minecraft:flowering_azalea_leaves",
                                "minecraft:azalea_leaves"], [3, 2, 1, 1], 420 + i))


def schors_leven(W):
    """Aankleding van de buitenkant van de stam (geen kale schors): tonderzwammen, moskussens met bloemetjes,
    hangend mos, knoestgaten met een lampje of een vogelnestje."""
    if not W.raakt(-80, VOET, -80, 80, NEST_Y, 80):
        return
    rng = np.random.default_rng(77)
    zwam = ["minecraft:brown_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", "minecraft:brown_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", "minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=true,west=true]",
            "minecraft:shroomlight"]
    for i in range(420):
        a = rng.uniform(0, 2 * math.pi)
        y = rng.uniform(VOET + 4, NEST_Y - 8)
        ro = float(r_stam(y, np.array([a - y / 160.0]))[0])
        x, z = (ro + 0.5) * math.cos(a), (ro + 0.5) * math.sin(a)
        if not W.raakt(x - 6, y - 10, z - 6, x + 6, y + 6, z + 6):
            continue
        soort = i % 4
        if soort == 0:      # tonderzwammen: 2-3 schijfjes boven elkaar
            for k in range(int(rng.integers(2, 4))):
                W.bol((x, y + k * 2.2, z), rng.uniform(1.8, 3.2) - k * 0.4, None, ry=0.7, keep=True,
                      mixfn=mix(W, zwam, [4, 3, 0.6, 0.4], 640 + i))
        elif soort == 1:    # moskussen met bloemetjes en hangend mos
            W.bol((x, y, z), rng.uniform(2, 3.8), None, ry=1.6, keep=True, mixfn=mix(W, MOS, MOS_W, 650 + i))
            W.wset(x, y + 2, z, ["minecraft:pink_petals[facing=north,flower_amount=4]", "minecraft:azalea",
                                 "minecraft:short_grass"][i % 3], keep=True)
            for k in range(int(rng.integers(3, 9))):
                W.wset(x + 0.5 * math.cos(a), y - 2 - k, z + 0.5 * math.sin(a), "minecraft:pale_hanging_moss[tip=false]"
                       if i % 2 else "minecraft:cave_vines_plant[berries=true]", keep=True)
        elif soort == 2 and i % 8 == 2:   # knoestgat met lampje of nestje
            W.bol((x - 1.2 * math.cos(a), y, z - 1.2 * math.sin(a)), 2.2, None, ry=2.8)
            W.bol((x - 0.2 * math.cos(a), y, z - 0.2 * math.sin(a)), 3.0, None, ry=3.6, hollow=1, keep=True,
                  mixfn=mix(W, ["minecraft:stripped_spruce_wood", "minecraft:spruce_wood"], [2, 1], 660))
            W.wset(x - 1.0 * math.cos(a), y - 1.5, z - 1.0 * math.sin(a),
                   LANTAARN if i % 16 == 2 else "minecraft:hay_block")
        else:               # klimop/lianen langs de schors
            for k in range(int(rng.integers(4, 14))):
                W.wset(x + 0.6 * math.cos(a), y - k, z + 0.6 * math.sin(a),
                       "minecraft:cave_vines_plant[berries=false]" if k % 5 else
                       "minecraft:cave_vines_plant[berries=true]", keep=True)


def nest(W):
    """Het nest bovenop de stam: een kom van gevlochten takken (Ø ~56) met veren, de trap komt in het midden uit."""
    y0 = NEST_Y
    if not W.raakt(-40, y0 - 10, -40, 40, y0 + 24, 40):
        return
    tw = ["minecraft:stripped_spruce_wood", "minecraft:spruce_wood", "minecraft:stripped_dark_oak_wood",
          "minecraft:dark_oak_wood", "minecraft:stripped_oak_wood", "minecraft:hay_block"]
    n = 40
    for k in range(n):
        a = 2 * math.pi * k / n
        for laag_ in range(4):
            r = 22 + laag_ * 2.2 + 1.5 * math.sin(a * 3 + laag_)
            b = a + 0.45 + laag_ * 0.3
            p0 = (r * math.cos(a), y0 - 2 + laag_ * 3.2, r * math.sin(a))
            p1 = ((r + 2) * math.cos(b), y0 + 1 + laag_ * 3.2, (r + 2) * math.sin(b))
            W.buis([p0, p1], [2.2 - laag_ * 0.2, 1.9], None, mixfn=mix(W, tw, [3, 3, 2, 2, 1.5, 1], 90 + laag_))
    W.bol((0, y0 - 1, 0), 22, None, ry=6, ymin=y0 - 1, ymax=y0 + 1, mixfn=mix(W, [
        "minecraft:hay_block", "guhs:roze_hooibaal", "minecraft:pink_wool", "minecraft:white_wool", "guhs:pluiswolblok",
        "minecraft:moss_block"], [3, 2, 2, 1, 1.5, 1], 91))
    # [zone 3 kaart] the stair comes up through a slot in the nest floor (wenteltrap() runs again after this and clears the
    # slot); a fence on the nest floor along the inner side of the slot, open at the arrival (the last landing)
    # (U-DECO: on the nest floor = stand y0 + 1; they were at y0 + 2 and floated one block above the floor)
    for phi in np.arange(_KNOPEN[4][0] + TRAP_WL, _KNOPEN[5][0] - 0.05, 0.02):
        a = TRAP_TH0 + phi
        W.wset((TRAP_R0 - 0.6) * math.cos(a), y0 + 1, (TRAP_R0 - 0.6) * math.sin(a), HEK)
    a, _ = trap_landing(5)
    for s in (-1, 1):
        aa = a + s * (TRAP_WL / 2 + 0.05)
        W.wset((TRAP_R0 - 1.6) * math.cos(aa), y0 + 1, (TRAP_R0 - 1.6) * math.sin(aa), LANTAARN)
    # grote losse veren (roze, wit, goud)
    for k, (a, kl) in enumerate(((0.3, "minecraft:pink_wool"), (1.3, "minecraft:white_wool"),
                                 (2.4, "minecraft:pink_concrete"), (3.4, "minecraft:yellow_wool"),
                                 (4.4, "minecraft:pink_wool"), (5.4, "minecraft:white_wool"))):
        p0 = (25 * math.cos(a), y0 + 9, 25 * math.sin(a))
        p1 = (33 * math.cos(a + 0.15), y0 + 18 + 2 * (k % 2), 33 * math.sin(a + 0.15))
        W.buis([p0, p1], [1.4, 0.7], kl)
    W.label(0, y0 + 20, 0, "Het nest (Kruintop)")


# ===================================================================================================== Wortelduister + Hart
HART = (0, 36, 0, 22)       # midden, straal van de wortelmand
POORT = (132, 0)            # Wortelpoort (x, z) op het maaiveld, oostkant
GANG = [(140, None, 2), (112, 66, -4), (86, 52, 6), (62, 38, 2), (42, 26, 0)]


def gang_lijn():
    g0 = float(grondhoogte(np.array([GANG[0][0]]), np.array([GANG[0][2]]))[0])
    pts = [(GANG[0][0], g0 + 2, GANG[0][2])] + [p for p in GANG[1:]]
    return _spline(pts, 10)


def wortelduister(W):
    if W.b[1] > 90 or not W.raakt(-150, 0, -150, 150, 80, 150):
        return
    X, Y, Z = W.v.shape
    sx = slice(max(0, W.vx(-150)), min(X, W.vx(150) + 1))
    sz = slice(max(0, W.vz(-150)), min(Z, W.vz(150) + 1))
    XX, ZZ = W.XX[sx, sz], W.ZZ[sx, sz]
    RR = np.hypot(XX, ZZ)
    for j in range(Y):
        y = W.ys[j]
        if y < 12 or y > 64:
            continue
        sl = W.v[sx, j, sz]
        # de grote wortelhal: een lage koepel onder de stam, met 3D-ruis voor organische wanden
        n = vnoise3(XX, y * 1.4, ZZ, cell=30, seed=201, oct=2)
        koepel = (RR / 86.0) ** 2 + ((y - 37) / 20.0) ** 2
        hal = (koepel + 0.3 * n < 0.95) & (y > 17)
        sl[hal] = 0
    # wortels die van het plafond (de stamvoet) als pilaren naar de vloer buigen
    for k in range(14):
        a = k * 2 * math.pi / 14 + 0.15
        r0 = 22 + (k % 3) * 8
        p0 = (r0 * math.cos(a), 66, r0 * math.sin(a))
        p1 = ((r0 + 24) * math.cos(a + 0.25), 44, (r0 + 24) * math.sin(a + 0.25))
        p2 = ((r0 + 30 + (k % 2) * 16) * math.cos(a + 0.15), 17, (r0 + 30 + (k % 2) * 16) * math.sin(a + 0.15))
        L = _spline([p0, p1, p2], 8)
        W.buis(L, np.linspace(7, 3.5, len(L)), None, mixfn=_gloeiwortel(W, 220 + k))
    # de gang van de Wortelpoort naar beneden
    L = gang_lijn()
    W.buis(L, np.full(len(L), 6.5), None, plat=1.15)
    W.buis(L - np.array([0, 5.5, 0]), np.full(len(L), 3.2), "minecraft:packed_mud", plat=0.3)
    for i in range(0, len(L), 5):
        x, y, z = L[i]
        y = y - 5
        for s in (-1, 1):
            W.wset(x, y + 1, z + s * 4, HOUT)
            W.wset(x, y + 2, z + s * 4, "minecraft:soul_lantern[hanging=false,waterlogged=false]" if i % 10 else LANTAARN)
    aankleden(W, sx, sz)
    hart(W)
    W.label(70, 40, 0, "Wortelduister")


def _gloeiwortel(W, seed):
    def f(x, y, z, m):
        ids = W.kies(["minecraft:dark_oak_wood", "minecraft:mangrove_wood", "minecraft:spruce_wood",
                      "minecraft:mangrove_roots"], [4, 3, 2, 1], x, y, z, seed)
        ader = vnoise3(x, y, z, cell=6, seed=seed) > 0.42
        return np.where(ader, W.kies(["guhs:weidekristal_lila", "guhs:weidekristal_cyaan", "minecraft:amethyst_block"],
                                     [3, 2, 1], x, y, z, seed + 1), ids)
    return f


def aankleden(W, sx, sz):
    X, Y, Z = W.v.shape
    XX, ZZ = W.XX[sx, sz], W.ZZ[sx, sz]
    RR = np.hypot(XX, ZZ)
    steen = W.ids(["minecraft:stone", "minecraft:tuff", "minecraft:deepslate", "minecraft:andesite", "minecraft:dirt",
                   "minecraft:mud"])
    for j in range(1, Y - 1):
        y = W.ys[j]
        if y < 10 or y > 70:
            continue
        sl, bo, on = W.v[sx, j, sz], W.v[sx, j + 1, sz], W.v[sx, j - 1, sz]
        vast = np.isin(sl, steen) & (RR < 140)
        if not vast.any():
            continue
        vloer = vast & (bo == 0)
        vl = W.kies(["minecraft:moss_block", "minecraft:tuff", "guhs:glimmos", "minecraft:purple_terracotta",
                     "minecraft:sculk", "minecraft:mud"], [3, 2, 1.2, 1, 0.6, 1], XX, y, ZZ, 212)
        sl[vloer] = vl[vloer]
        plafond = vast & (on == 0)
        pl = W.kies(["minecraft:mangrove_roots", "minecraft:dark_oak_wood", "minecraft:deepslate",
                     "guhs:weidekristal_lila"], [3, 2, 2, 0.4], XX, y, ZZ, 213)
        sl[plafond] = pl[plafond]
        wand = vast & ~vloer & ~plafond
        rand_ = np.zeros_like(wand)
        rand_[1:, :] |= sl[:-1, :] == 0
        rand_[:-1, :] |= sl[1:, :] == 0
        rand_[:, 1:] |= sl[:, :-1] == 0
        rand_[:, :-1] |= sl[:, 1:] == 0
        n = vnoise3(XX, y * 1.3, ZZ, cell=12, seed=214, oct=1)
        ader = wand & rand_ & (np.abs(n) < 0.08)
        sl[ader] = np.where(hash3(XX, y, ZZ, 215)[ader] < 0.6, W.id("guhs:weidekristal_lila"),
                            W.id("guhs:weidekristal_cyaan"))
        # glimzwammen op de vloer, hangende wortels aan het plafond
        op = vloer & (hash3(XX, y, ZZ, 216) < 0.05)
        bo[op & (bo == 0)] = np.where(hash3(XX, y, ZZ, 217)[op & (bo == 0)] < 0.6, W.id("guhs:glimpaddenstoel"),
                                      W.id("guhs:glimplant"))
        hang = plafond & (hash3(XX, y, ZZ, 218) < 0.06)
        on[hang & (on == 0)] = W.id("minecraft:hanging_roots")
    # sap-poeltjes in de laagste kommen (gloeiend turkoois)
    # [zone 3 kaart] glowing sap patches on the floor instead of pools (no pits)
    plas = (vnoise(XX, ZZ, cell=16, seed=219) > 0.25) & (RR < 80) & (RR > HART[3] + 10)
    for j in range(1, Y - 1):
        y = W.ys[j]
        if 14 <= y <= 30:
            sl, bo = W.v[sx, j, sz], W.v[sx, j + 1, sz]
            m = plas & (sl != 0) & (bo == 0)
            sl[m] = np.where(hash3(XX, y, ZZ, 219)[m] < 0.7, W.id("guhs:weidekristal_cyaan"), W.id("minecraft:sea_lantern"))


def hart(W):
    """Het Hart van de Boom: een mand van gevlochten wortels met het ei, poort aan de oostkant (naar de gang)."""
    cx, cy, cz, R = HART
    if not W.raakt(cx - 40, 0, cz - 40, cx + 40, 70, cz + 40):
        return
    sch = _gloeiwortel(W, 700)
    for k in range(16):
        a = 2 * math.pi * k / 16
        for s in (-1, 1):
            pts = [pol(R * 0.7, a, cy - R * 0.75), pol(R, a + s * 0.35, cy), pol(R * 0.7, a + s * 0.7, cy + R * 0.8)]
            W.buis(_spline(pts, 6), np.full(13, 1.5), None, mixfn=sch)
    W.cilinder(cx, cz, R * 0.8, cy - R * 0.72, cy - R * 0.72, None, mixfn=mix(W, [
        "minecraft:moss_block", "guhs:glimmos", "minecraft:mangrove_roots"], [3, 1, 1], 701))
    # [zone 3 kaart] no gate: the egg chamber is the last room of the Hart dungeon (a stair comes up through the floor)
    # het ei in een wortelwieg
    ey = cy - R * 0.72 + 6
    W.bol((cx, ey, cz), 4.2, "guhs:weidekristal_roze", ry=6)
    W.bol((cx, ey, cz), 3.0, "minecraft:pearlescent_froglight", ry=4.8)
    for k in range(8):
        a = k * math.pi / 4
        W.buis([pol(7, a, ey - 5), pol(5, a + 0.4, ey - 1), pol(4.6, a + 0.8, ey + 3)], [1.2, 1.0, 0.8], None,
               mixfn=sch)
    # roze aders over de vloer als spaken naar het ei
    for k in range(10):
        a = k * math.pi / 5
        for rr in np.arange(6, R + 26, 0.8):
            x, z = rr * math.cos(a + 0.12 * math.sin(rr / 7)), rr * math.sin(a + 0.12 * math.sin(rr / 7))
            for yy in range(int(cy - R * 0.72) + 4, 10, -1):
                if W.wget(x, yy, z) not in ("minecraft:air", "minecraft:water") and W.wget(x, yy + 1, z) == "minecraft:air":
                    W.wset(x, yy, z, "guhs:weidekristal_roze")
                    break
    W.label(cx, cy + R + 2, cz, "Hart van de Boom (het ei)")


def wortelpoort(W):
    """De ingang van het Wortelduister op het maaiveld: twee wortels die een boog maken, met wegwijzer en lantaarns."""
    x, z = POORT
    g = float(grondhoogte(np.array([x]), np.array([z]))[0])
    if not W.raakt(x - 30, g - 10, z - 30, x + 30, g + 30, z + 30):
        return
    for s in (-1, 1):
        pts = [(x + 14, g - 2, z + s * 13), (x + 4, g + 16, z + s * 9), (x - 6, g + 19, z + s * 2),
               (x - 14, g + 10, z - s * 4), (x - 18, g - 2, z - s * 6)]
        W.buis(_spline(pts, 8), np.linspace(4, 2.2, 33), None, mixfn=_gloeiwortel(W, 730 + s))
    W.buis(gang_lijn()[:6], np.full(6, 6.5), None, plat=1.15)
    for (dx, dz) in ((10, 8), (10, -8), (-4, 9), (-4, -9)):
        gy = float(grondhoogte(np.array([x + dx]), np.array([z + dz]))[0])
        W.wfill(x + dx, gy + 1, z + dz, x + dx, gy + 3, z + dz, HOUT)
        W.wset(x + dx, gy + 4, z + dz, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
    W.wfill(x + 22, g + 1, z + 12, x + 22, g + 5, z + 12, HOUT)
    W.wfill(x + 21, g + 5, z + 12, x + 23, g + 6, z + 12, "minecraft:purple_wool")
    W.label(x, g + 26, z, "Wortelpoort -> Wortelduister + Hart")


def hangwortels(W):
    """Wortels die onder het zwevende eiland uithangen, met gloeiende puntjes (paars-blauw).
    [wereldindeling] 30 more on the west side: long roots hanging into the Wolkenkloof."""
    rng = np.random.default_rng(55)
    for i in range(70):
        if i < 40:
            a = rng.uniform(0, 2 * math.pi)
            r = rng.uniform(40, 200)
        else:
            a = math.radians(rng.uniform(130, 235))
            r = rng.uniform(170, 236)
        x, z = r * math.cos(a), r * math.sin(a)
        g = grondhoogte(np.array([x]), np.array([z]))
        b = float(onderkant(np.array([x]), np.array([z]), g)[0])
        lng = rng.uniform(14, 40) * (1.8 if i >= 40 else 1.0)
        if not W.raakt(x - 10, b - lng - 4, z - 10, x + 10, b + 6, z + 10):
            continue
        p1 = (x + rng.uniform(-6, 6), b - lng * 0.5, z + rng.uniform(-6, 6))
        p2 = (x + rng.uniform(-8, 8), b - lng, z + rng.uniform(-8, 8))
        W.buis(_spline([(x, b + 4, z), p1, p2], 5), np.linspace(3.2, 0.8, 11), None, mixfn=_gloeiwortel(W, 760 + i))
        W.bol(p2, 1.6, ["guhs:weidekristal_lila", "minecraft:verdant_froglight", "guhs:weidekristal_cyaan"][i % 3])


# ===================================================================================================== wolken + grond
def wolken(W):
    if W.b[1] > 70:
        return
    rng = np.random.default_rng(181)
    for i in range(60):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(EILAND_R + 10, 560)
        x0, z0 = rr * math.cos(a), rr * math.sin(a)
        if abs(x0) > 410 or abs(z0) > 410:
            continue
        y0 = rng.uniform(16, 34)
        for j in range(int(rng.integers(3, 7))):
            R = rng.uniform(7, 18)
            c = (x0 + rng.normal(0, 14), y0 + rng.uniform(0, 6), z0 + rng.normal(0, 14))
            if W.raakt(c[0] - R, c[1] - R, c[2] - R, c[0] + R, c[1] + R, c[2] + R):
                W.bol(c, R, None, ry=R * rng.uniform(0.5, 0.7), keep=True, mixfn=mix(W, [
                    "minecraft:white_wool", "minecraft:white_wool", "guhs:pluiswolblok",
                    "minecraft:white_concrete_powder"], [5, 3, 2, 1], 182))


def bosjes(W):
    """Bomen op het eiland buiten de kruin, reuzenpaddenstoelen eronder (geen lege vlaktes)."""
    if W.b[1] > 120:
        return
    rng = np.random.default_rng(191)
    soorten = [("minecraft:oak_log[axis=y]", ["minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves"]),
               ("guhs:guhbloesem_log[axis=y]", ["minecraft:cherry_leaves", "guhs:guhbloesem_leaves"]),
               ("minecraft:birch_log[axis=y]", ["minecraft:oak_leaves", "minecraft:flowering_azalea_leaves"]),
               ("minecraft:cherry_log[axis=y]", ["minecraft:cherry_leaves", "minecraft:oak_leaves"])]
    vrij = [(POORT[0], POORT[1], 26)]
    for i in range(700):
        a = rng.uniform(0, 2 * math.pi)
        rr = math.sqrt(rng.uniform(70 ** 2, (EILAND_R - 16) ** 2))
        x, z = rr * math.cos(a), rr * math.sin(a)
        if any(math.hypot(x - vx, z - vz) < vr for (vx, vz, vr) in vrij + PLEKKEN_VRIJ):
            continue
        if not W.raakt(x - 10, 40, z - 10, x + 10, 130, z + 10):
            continue
        g = float(grondhoogte(np.array([x]), np.array([z]))[0])
        if rr < 150:
            if rng.random() < 0.55:
                continue
            # reuzenpaddenstoel (gloeit zacht in de schemer)
            h = rng.uniform(5, 11)
            W.wfill(x, g + 1, z, x, g + h, z, "minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=true,west=true]")
            kl = ["minecraft:red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", "minecraft:brown_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", "minecraft:purple_wool"][i % 3]
            W.bol((x, g + h, z), rng.uniform(3, 5.5), kl, ry=1.8, ymin=g + h - 0.5)
            W.wset(x, g + h - 1, z, "minecraft:shroomlight")
            continue
        groot = rng.random() < 0.2
        h = rng.uniform(12, 18) if groot else rng.uniform(6, 11)
        R = rng.uniform(6, 8) if groot else rng.uniform(3.2, 5.5)
        log, bl = soorten[i % 4]
        W.wfill(x, g + 1, z, x, g + h, z, log)
        bladwolk(W, (x, g + h + 1, z), R, "takken", 1900 + i, ry=0.75) if groot else \
            W.bol((x, g + h + 1, z), R, None, ry=R * 0.8, keep=True, mixfn=mix(W, bl, [3, 2], 192 + i))


PLEKKEN_VRIJ = []   # (x, z, r): plekken (kampen, liftstation, poort) waar geen bomen mogen


# ===================================================================================================== alles
DELEN = ("terrein", "wortels_boven", "wortelduister", "hangwortels", "takken", "kruin", "stam", "schors_leven",
         "nest", "wolken", "wortelpoort", "bosjes", "rijpad")


def bouw(W, delen=None):
    for d in (delen or DELEN):
        globals()[d](W)
    return W
