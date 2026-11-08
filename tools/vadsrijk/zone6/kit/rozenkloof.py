"""Rozenkloof: the arrival city of the Zijdeland, CARVED into a rose sandstone canyon (Petra). U-Z6WERELD round 2.

Game coordinates (x east, z south). The canyon axis is zijwereld.KLOOF (sea in the west at x 3236 -> end wall x 3478).

What changed against the concept render (the walls were flat striped surfaces):
  * the canyon cross-section in the city (x 3316..3478) is REBUILT as designed terraces: canyon floor, ledge 1 (+16),
    ledge 2 (+32) and the rim terrace (+42..52, follows the natural plateau), in BAYS of 11-21 blocks: every bay is one
    carved facade per side and tier, so the faces jog naturally between bays;
  * natural rock strata (wavy bands of pink, peach, cream, rose-red with varying thickness: petra.strata) over the whole
    canyon, overhanging lips at the top of every face, eroded pockets, hanging gardens trailing over the rims;
  * per bay a different rock-cut facade (petra: tomb temple, two-storey tholos facade, rock house with mashrabiya oriel,
    shop front with a silk awning, colonnaded gallery, shrine niche), most with a REAL interior behind;
  * rock-cut staircases (arcaded, inside the face) between all tiers on both sides, two stone arch bridges at ledge 2, a
    rope bridge at the rim, railings (rozenzandsteen walls) on every ledge edge, lamps everywhere;
  * the Rozenplein (fountain, market, palms), the herberg, the bank, the tapijtwinkel, the bultguh-stal, the Postkantoor;
  * De Schatkamer: the big two-storey facade in the end wall with Kloofmoeder Roosje's hall carved behind it;
  * the Siq up to the plateau (gate arch, lamp niches), De Zeemond (pier, two guh colossi, the Hoogvads lookout);
  * the Luchthaven (line E): mooring tower with a spiral stair, deck, pier over the canyon rim, mast, bell, board.

Game data: zijbouw.plek (all Rozenkloof ids of plan6), zijbouw.huis_rol (houses with a role), zijfiguren mounts
(W.rijdieren). bouw(W) only builds what falls in the window and is deterministic (rng from positions).
"""
import math
import numpy as np
import schemerkit as SK
import zijbouw as ZB
import zijwereld as ZW
import petra as PT
from zijbouw import zet, doos, leeg, raakt, rng_van, FACE
from zpalet import Z, WOL, TAPIJT, b, trap, plaat
from petra import Vlak, mlamp, LUCHT, MURO, HEK, KETTING

BOX = (3230, 40, -810, 3580, 200, -630)
X0, X1 = 3316, 3479            # the rebuilt stretch (x0..x1-1); x >= 3479 is the natural end wall
LW = 7                          # ledge width
TH = 16                         # tier height (floor -> ledge 1 -> ledge 2)
RAND = 9                        # rim terrace width
BLEND = 12                      # rim terrace -> natural plateau
N, S = -1, 1                    # sides: north wall (rock towards -z), south wall

# the bays (x1, x2): x2 exclusive. Index 0 is the transition to the natural mouth.
BAYS = [(3322, 3328), (3328, 3340), (3340, 3361), (3361, 3373), (3373, 3387), (3387, 3408), (3408, 3421), (3421, 3432),
        (3432, 3453), (3453, 3466), (3466, 3479)]
# rock-cut staircases: (bay, side, tier) -> the stair runs east (+x) inside face 'tier', from ledge tier to tier+1
TRAPPEN = {(2, N, 0), (2, S, 1), (2, S, 2), (5, S, 0), (5, N, 1), (5, N, 2), (8, S, 0), (8, N, 1), (8, S, 2)}
BRUGGEN = [(3365, "brug_1"), (3426, "brug_2")]      # stone arch bridges at ledge 2 (x of the middle, 5 wide)
TOUWBRUG_X = 3380                                   # rope bridge between the rim terraces
SIQ_BAYS_N = {9, 10}                                # north tiers here are cut by the Siq: no facades
PLEIN = (3400, -714)
LUCHTHAVEN = (3398, -786)


# ============================================================================================ the canyon profile
def as_op(x):
    """Axis z, half width and floor height (bodem) of the canyon at x (linear between the KLOOF points)."""
    K = ZW.KLOOF
    for i in range(len(K) - 1):
        a, c = K[i], K[i + 1]
        if a[0] <= x <= c[0]:
            t = (x - a[0]) / (c[0] - a[0])
            return a[1] + t * (c[1] - a[1]), a[2] + t * (c[2] - a[2]), a[3] + t * (c[3] - a[3])
    a = K[0] if x < K[0][0] else K[-1]
    return a[1], a[2], a[3]


def _nat_h(x, z):
    H, _, _ = ZW.velden(np.array([[x + 0.5]]), np.array([[z + 0.5]]))
    return int(math.floor(H[0, 0]))


class Bay:
    def __init__(self, i, x1, x2):
        self.i, self.x1, self.x2 = i, x1, x2
        self.xm = (x1 + x2 - 1) / 2.0
        cz, hw, bod = as_op(self.xm)
        self.czr = int(round(cz))
        F = int(round(0.44 * hw))
        self.c = [F, F + LW, F + 2 * LW]                  # face offsets from the axis (first rock cell)
        self.L0 = int(math.floor(bod)) + 1                # stand level on the canyon floor
        self.L = [self.L0, self.L0 + TH, self.L0 + 2 * TH]
        self.LT = {}                                      # stand level of the rim terrace per side (set by _rand_niveaus)
        self.hw = hw

    def z(self, side, off):
        return self.czr + side * off

    def U(self, side, k):
        """Stand level above face k (the ledge / terrace it carries)."""
        return self.L[k + 1] if k < 2 else self.LT[side]

    def vlak(self, W, side, k):
        return Vlak(W, (0, -side), self.z(side, self.c[k]))


def _maak_bays():
    bays = [Bay(i, a, c) for i, (a, c) in enumerate(BAYS)]
    for side in (N, S):
        doel = []
        for B in bays:
            o = B.c[2] + RAND + 4
            hs = [_nat_h(x, B.z(side, o + k)) for x in (B.x1 + 1, int(B.xm), B.x2 - 2) for k in (0, 4, 8)]
            doel.append(int(np.median(hs)) + 1)
        vorig = None
        for B, t in zip(bays, doel):
            t = max(B.L[2] + 10, min(B.L[2] + 20, t))
            if vorig is not None:
                t = max(vorig - 1, min(vorig + 1, t))
                t = max(B.L[2] + 10, t)
            B.LT[side] = t
            vorig = t
    return bays


BAYLIST = _maak_bays()


def bay_van(x):
    for B in BAYLIST:
        if B.x1 <= x < B.x2:
            return B
    return BAYLIST[0] if x < BAYLIST[0].x1 else BAYLIST[-1]


import functools


@functools.lru_cache(maxsize=None)
def _lip_c(x, side, k, bi):
    return _lip(x, side, k, BAYLIST[bi])


def _lip(x, side, k, B):
    """How far the top of face k leans out over the ledge below it (0..3 blocks)."""
    if (B.i, side, k) in TRAPPEN or B.i == 0:
        return 0
    for bx, _ in BRUGGEN:
        if abs(x - bx) <= 4 and k == 1:
            return 0
    if abs(x - TOUWBRUG_X) <= 3 and k == 2:
        return 0
    if side == N and B.i in SIQ_BAYS_N:
        return 0
    v = SK.vnoise(np.array([x + 0.5]), np.array([side * 97.0 + k * 31.0]), cell=7, seed=91, oct=2)[0]
    return int(max(0, min(3, round(1.3 + 2.2 * v))))


@functools.lru_cache(maxsize=None)
def _siq(x, z):
    """Siq floor top (or None) at x, z: the narrow canyon up to the plateau (no noise, clean walls)."""
    d, v, t = ZW.lijn_afstand(np.array([[x + 0.5]]), np.array([[z + 0.5]]), ZW.SIQ)
    if d[0, 0] < v[0, 0, 0]:
        return int(math.floor(v[0, 0, 1]))
    return None


def profiel(x, z, hn=None):
    """Designed column at x, z: (top solid y, lip (y1, y2) or None, kind). hn = natural top (for the blends)."""
    B = bay_van(x)
    side = S if z >= B.czr else N
    o = abs(z - B.czr)
    c0, c1, c2 = B.c
    lip = None
    soort = "rand"
    if o < c0:
        top, soort = B.L0 - 1, "vloer"
        k = 0
    elif o < c1:
        top, soort, k = B.L[1] - 1, "richel1", 1
    elif o < c2:
        top, soort, k = B.L[2] - 1, "richel2", 2
    elif o < c2 + RAND:
        top, k = B.LT[side] - 1, 3
    else:
        if hn is None:
            hn = _nat_h(x, z)
        s = min(1.0, (o - c2 - RAND) / float(BLEND))
        top = int(round(B.LT[side] - 1 + (hn - (B.LT[side] - 1)) * s))
        k = 3
    # the lip of the face just outside this cell (face k at offset c_k leans out over this ledge)
    if k <= 2:
        fk = k                                        # face index whose top leans over this cell
        if fk <= 2:
            lp = _lip_c(x, side, fk, B.i)
            if lp and o >= B.c[fk] - lp:
                dist = B.c[fk] - o
                rows = min(4, 2 + (lp - dist))
                U = B.U(side, fk)
                lip = (U - rows, U - 1)
    if x < X0 + 6:                                   # blend into the natural mouth
        if hn is None:
            hn = _nat_h(x, z)
        w = max(0.0, (x - X0) / 6.0)
        top = int(round(hn + (top - hn) * w))
        lip = None
    sq = _siq(x, z)
    if sq is not None and sq < top:
        top, lip, soort = sq, None, "siq"
    return top, lip, soort


def oppervlak(x, z, hn=None):
    """The walkable top (incl. lips) of a designed column."""
    top, lip, _ = profiel(x, z, hn)
    return lip[1] if lip else top


# ============================================================================================ terrain rebuild + strata
_NAT_BANDEN = ("rots", "rots_rood", "rots_perzik", "rots_marmer", "rots_diep", "rots_roze")


def _strata_box(W, x1, z1, x2, z2, y1, y2, alleen_natuur=True):
    """Re-colour the rock in a box with the wavy strata (only the land's band blocks when alleen_natuur)."""
    bx = W._box(x1, y1, z1, x2, y2, z2)
    if bx is None:
        return
    (sx, sy, sz), _ = bx
    xs, ys, zs = W.xs[sx], W.ys[sy], W.zs[sz]
    XX, ZZ = np.meshgrid(xs, zs, indexing="ij")
    n2 = 3.4 * SK.vnoise(XX, ZZ, cell=46, seed=71, oct=2) + 1.2 * SK.vnoise(XX, ZZ, cell=13, seed=72, oct=1) \
        + 0.035 * (XX - 3400) - 0.02 * (ZZ + 720)
    if not hasattr(W, "_strata_ids"):
        W._strata_ids = np.array([W.id(b(k)) for k in PT._BAND_MAT], np.uint16)
    nat = W.ids([b(k) for k in _NAT_BANDEN])
    for jj, y in enumerate(ys):
        sl = W.v[sx, sy.start + jj, sz]
        m = np.isin(sl, nat) if alleen_natuur else (sl != 0)
        if not m.any():
            continue
        t = y + n2 + np.where(SK.hash3(XX, y, ZZ, 77) < 0.10, 0.8, 0.0)
        ids = W._strata_ids[PT.BANDTABEL[np.floor(t).astype(int) % len(PT.BANDTABEL)]]
        sl[m] = ids[m]


def herbouw(W):
    """Rebuild every column of the city stretch to the designed profile (rock in strata, air above)."""
    X, Y, Zn = W.v.shape
    vloer, vloer2 = W.id(b("vloer_zand")), W.id(b("zand"))
    rand_top = W.id(b("zand"))
    if not hasattr(W, "_strata_ids"):
        W._strata_ids = np.array([W.id(b(k)) for k in PT._BAND_MAT], np.uint16)
    y_lo = W.b[1]
    for x in range(X0, X1):
        i = W.vx(x)
        if not (0 <= i < X):
            continue
        B = bay_van(x)
        zmax = B.c[2] + RAND + BLEND + 2
        for z in range(B.czr - zmax, B.czr + zmax + 1):
            k_ = W.vz(z)
            if not (0 <= k_ < Zn):
                continue
            hn = int(math.floor(W.H[i, k_])) if hasattr(W, "H") else _nat_h(x, z)
            o = abs(z - B.czr)
            if o < B.c[0] and hasattr(W, "wat") and not np.isnan(W.wat[i, k_]):
                continue                                  # the stream stays as the land made it
            top, lip, soort = profiel(x, z, hn)
            lo = max(int(y_lo), min(top, hn) - 3, top - 70)
            ys = np.arange(lo, top + 1)
            col = W.v[i, :, k_]
            if len(ys):
                t = PT.strata_t(np.full(len(ys), x + 0.5), ys, np.full(len(ys), z + 0.5))
                t = t + np.where(SK.hash3(np.full(len(ys), x), ys, np.full(len(ys), z), 77) < 0.10, 0.8, 0.0)
                ids = W._strata_ids[PT.BANDTABEL[np.floor(t).astype(int) % len(PT.BANDTABEL)]]
                j0 = W.vy(lo)
                j1 = W.vy(top) + 1
                a, c_ = max(0, j0), min(Y, j1)
                if a < c_:
                    col[a:c_] = ids[a - j0:c_ - j0]
            hoog = max(hn, top) + 2
            a, c_ = max(0, W.vy(top + 1)), min(Y, W.vy(hoog) + 1)
            if a < c_:
                col[a:c_] = 0
            # surface: paved ledges, sandy floor and terrace
            j = W.vy(top)
            if 0 <= j < Y:
                if soort in ("richel1", "richel2"):
                    col[j] = vloer
                elif soort in ("vloer", "siq"):
                    col[j] = vloer2
                elif o < B.c[2] + RAND:
                    col[j] = rand_top
            if lip:
                yy = np.arange(lip[0], lip[1] + 1)
                t = PT.strata_t(np.full(len(yy), x + 0.5), yy, np.full(len(yy), z + 0.5))
                ids = W._strata_ids[PT.BANDTABEL[np.floor(t).astype(int) % len(PT.BANDTABEL)]]
                for q, y in enumerate(yy):
                    jy = W.vy(y)
                    if 0 <= jy < Y:
                        col[jy] = ids[q]
                jy = W.vy(lip[1])
                if 0 <= jy < Y:
                    col[jy] = vloer


# ============================================================================================ facades per bay
TYPEN_RIJ = {0: ["winkel", "woning", "tempel", "galerij", "tholos", "woning", "winkel", "schrijn", "tempel", "woning"],
             1: ["woning", "tempel", "galerij", "woning", "schrijn", "tholos", "winkel", "woning", "tempel", "galerij"],
             2: ["galerij", "woning", "schrijn", "woning", "galerij", "woning", "schrijn", "tempel", "woning", "galerij"]}
# special houses on the canyon floor (bay, side) -> function name
SPECIAAL = {(3, S): "herberg", (4, S): "tapijtwinkel", (6, S): "bank", (6, N): "postkantoor", (8, N): "stal",
            (1, N): "schrijn", (1, S): "schrijn", (7, N): "gilde"}
WINKELS = ["winkel_specerijen", "winkel_dadels", "winkel_lampen", "winkel_potten", "winkel_zijde", "winkel_kruiden",
           "winkel_snoep", "winkel_sieraden"]


def facades(W, log=None):
    huizen = []
    nw = 0
    for B in BAYLIST[1:]:
        for side in (N, S):
            for k in range(3):
                if (B.i, side, k) in TRAPPEN:
                    continue
                if side == N and B.i in SIQ_BAYS_N:
                    continue
                V = B.vlak(W, side, k)
                y = B.L[k]
                h = B.U(side, k) - y
                if not raakt(W, B.x1, y - 2, V.c - 14, B.x2, y + h, V.c + 14):
                    continue
                u1, u2 = B.x1 + 1, B.x2 - 2
                sp = SPECIAAL.get((B.i, side)) if k == 0 else None
                seed = B.i * 7 + k * 3 + (side > 0)
                if sp in ("herberg", "tapijtwinkel", "bank", "postkantoor", "stal", "gilde"):
                    rec = globals()["huis_" + sp](V, u1, u2, y, h, seed)
                else:
                    typ = sp or TYPEN_RIJ[k][(B.i + (0 if side < 0 else 3)) % 10]
                    if h < 14 and typ in ("tempel", "tholos"):
                        typ = "woning"
                    if h < 11 and typ == "woning":
                        typ = "galerij"
                    rol = None
                    if typ == "winkel":
                        rol = WINKELS[nw % len(WINKELS)]
                        nw += 1
                        rec = PT.gevel_winkel(V, u1, u2, y, h, seed, rol=rol)
                    elif typ == "woning":
                        rec = PT.gevel_woning(V, u1, u2, y, h, seed)
                    else:
                        rec = PT.TYPES[typ](V, u1, u2, y, h, seed)
                    # an eroded pocket in the rock band above the facade (not on every bay)
                    rng = rng_van(B.i, side, k, 5)
                    if rng.rand() < 0.45 and h >= 15:
                        PT.erosie(V, int(rng.randint(u1 + 1, u2)), 0, y + h - 7, 1.2 + rng.rand() * 0.8, seed=seed)
                if rec:
                    huizen.append((B, side, k, rec))
    return huizen


# ---- the special houses on the canyon floor
def huis_herberg(V, u1, u2, y, h, seed):
    """Herberg "De Bultguh": an open arcade of three arches, a big hall with a counter (balie), tables, cushions,
    barrels and a round lamp ring, an upper floor with oriels and a crow-step crown."""
    rng = rng_van(u1, y, 31)
    um = (u1 + u2) // 2
    V.lucht(u1, u2, 0, 1, y, y + 10)
    V.doos(u1, u2, 2, 2, y, y + 10, "muur_perzik")
    for u in range(u1, u2 + 1):
        V.P(u, 0, y - 1, "vloer_dambord")
        V.P(u, 1, y - 1, "vloer_dambord")
    for a in (um - 3, um, um + 3):                      # three arches
        for du in (-1, 0, 1):
            for yy in range(y, y + (4 if du else 5)):
                V.P(a + du, 2, yy, LUCHT)
        V.P(a, 2, y + 5, "zs_beitel")
        V.P(a - 1, 2, y + 4, "tegel_turkoois")
        V.P(a + 1, 2, y + 4, "tegel_turkoois")
    for u in range(u1, u2 + 1):
        V.P(u, 1, y + 6, trap("zs", V.out, half="top"))
        V.P(u, 0, y + 6, "luifel_zijde") if u % 2 else None
    for u in (um - 2, um + 2):                          # oriels
        for yy in (y + 7, y + 8):
            V.P(u, 1, yy, "mashrabiya")
        V.P(u, 1, y + 9, plaat("koepel_tegel"))
    PT.kantelkroon(V, u1, u2, y + 10, d=1)
    PT.kamer(V, u1, u2, 3, 13, y, h=6, vloer="planken", seed=seed)
    for d in range(4, 13):                              # the counter along the left wall
        V.P(u1 + 1, d, y, "palm_planken")
        V.P(u1 + 1, d, y + 1, plaat("palm_planken") if d % 3 else "minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]")
        V.P(u1, d, y, "minecraft:barrel[facing=up,open=false]")
    for (u, d) in ((um + 1, 6), (um + 1, 10), (u2 - 1, 8)):   # tables with cushions
        V.P(u, d, y, HEK)
        V.P(u, d, y + 1, plaat("palm_planken"))
        for du, dd in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if u1 + 2 < u + du < u2 + 1:
                V.P(u + du, d + dd, y, PT.kussen(["pink", "yellow", "light_blue", "magenta"][(u + d + du) % 4]))
    x, _, z = V.pos(um + 0.5, 8, 0)
    PT.hanglampring(V.W, x, y + 6, z, r=2.5, n=8, lengte=1, midden=False)
    ZB.plek(V.W, "rozenkloof_herberg", *V.pos(um, 4, y))
    ZB.plek(V.W, "rozenkloof_herberg_balie", *V.pos(u1 + 2, 8, y))
    return ("herberg", PT._binnen(V, um, 2, y), PT._binnen(V, um, 5, y))


def huis_tapijtwinkel(V, u1, u2, y, h, seed):
    """Tapijtwinkel: a shop front with carpets hanging on the walls and rolled up on the floor."""
    rec = PT.gevel_winkel(V, u1, u2, y, h, seed, rol="winkel_tapijten")
    kl = ["magenta", "turkoois", "geel", "roze", "lila", "oranje", "blauw"]
    for u in range(u1, u2 + 1):
        for yy in range(y + 1, y + 5):
            V.P(u, 8, yy, WOL[kl[(u + yy // 2) % len(kl)]])
    for u in range(u1 + 1, u2, 2):
        V.P(u, 3, y, TAPIJT[kl[u % len(kl)]])
    V.P(u2, -1, y, "zijde")
    ZB.plek(V.W, "rozenkloof_tapijtwinkel", *V.pos((u1 + u2) // 2, -2, y))
    V.W._tapijtwinkel = (V, (u1 + u2) // 2, y)
    return ("winkel_tapijten", rec[1], rec[2])


def huis_bank(V, u1, u2, y, h, seed):
    """De Rozenbank: a temple facade, behind it a vault hall with a counter, gold and a brass lamp ring."""
    PT.gevel_tempel(V, u1, u2, y, h, seed, kamer_=False)
    um = (u1 + u2) // 2
    PT.kamer(V, u1, u2, 8, 16, y, h=6, vloer="dambord", seed=seed)
    for u in range(u1, u2 + 1):
        if u != um:
            V.P(u, 11, y, "palm_planken")
            V.P(u, 11, y + 1, plaat("goud"))
    for u in range(u1, u2 + 1, 2):
        V.P(u, 16, y, "minecraft:gold_block")
        V.P(u, 16, y + 1, "minecraft:chest[facing=" + V.out + ",type=single,waterlogged=false]")
    x, _, z = V.pos(um + 0.5, 12, 0)
    PT.hanglampring(V.W, x, y + 6, z, r=2.4, n=8, lengte=1)
    ZB.plek(V.W, "rozenkloof_bank", *V.pos(um, 9, y))
    return ("bank", PT._binnen(V, um, 3, y), PT._binnen(V, um, 9, y))


def huis_postkantoor(V, u1, u2, y, h, seed):
    """Het Postkantoor of Postguh Stempel: a tholos facade, behind it a hall with pigeonholes (barrels) and a desk."""
    PT.gevel_tholos(V, u1, u2, y, h, seed)
    um = (u1 + u2) // 2
    PT.kamer(V, um - 4, um + 4, 7, 13, y, h=6, vloer="mozaiek", seed=seed)
    for u in range(um - 4, um + 5):
        for yy in (y, y + 1, y + 2):
            V.P(u, 13, yy, "minecraft:barrel[facing=" + V.out + ",open=false]")
    for u in range(um - 2, um + 3):
        V.P(u, 10, y, "palm_planken")
        V.P(u, 10, y + 1, plaat("palm_planken"))
    V.lucht(um - 1, um + 1, 5, 7, y, y + 3)
    ZB.plek(V.W, "rozenkloof_postguh", *V.pos(um, 8, y))
    return ("postkantoor", PT._binnen(V, um, 3, y), PT._binnen(V, um, 8, y))


def huis_gilde(V, u1, u2, y, h, seed):
    """Het Karavaanhuis (guild recruit board): a gallery with a big board on the back wall."""
    PT.gevel_galerij(V, u1, u2, y, h, seed)
    um = (u1 + u2) // 2
    for u in range(um - 2, um + 3):
        for yy in (y + 1, y + 2, y + 3):
            V.P(u, 4, yy, "minecraft:dark_oak_planks" if u in (um - 2, um + 2) or yy == y + 3 else WOL["geel"])
    V.P(um, 3, y, LUCHT)
    V.P(um - 1, 3, y, LUCHT)
    V.P(um + 1, 3, y, LUCHT)
    ZB.plek(V.W, "rozenkloof_recruitbord", *V.pos(um, 2, y))
    return None


def huis_stal(V, u1, u2, y, h, seed):
    """De bultguh-stal of Stalmeester Hobbel: a big pointed archway into a rock hall with hay, stalls of palm
    fences, troughs and lamps; above the arch a frieze of carved guhs and a crow-step crown."""
    um = (u1 + u2) // 2
    W = V.W
    V.lucht(u1, u2, 0, 13, y, y + 7)
    for u in range(u1, u2 + 1):
        for d in range(0, 14):
            V.P(u, d, y - 1, "minecraft:hay_block[axis=y]" if (d > 6 and (u + d) % 3) else "zs")
    for u in range(u1, u2 + 1):                         # the archway (pointed) in the face
        top = y + 7 - int(abs(u - um) ** 1.5 * 0.25)
        for yy in range(top, y + 9):
            V.P(u, 0, yy, "zs")
        V.P(u, 0, y + 9, "zs_beitel" if (u - um) % 3 == 0 else "goud")
    for u in (u1, u2):
        for yy in range(y, y + 7):
            V.P(u, 0, yy, "zuil")
        V.P(u, 0, y + 7, "zuilkop")
    PT.kantelkroon(V, u1, u2, y + 10, d=0)
    for u in range(u1 + 1, u2, 4):                      # stalls (fences between, no fence on the walkway)
        for d in range(9, 14):
            V.P(u, d, y, HEK)
    for u in range(u1 + 2, u2, 4):
        V.P(u, 13, y, "minecraft:composter[level=7]")
    for d in (3, 8):
        x, _, z = V.pos(um, d, 0)
        PT.hanglampring(W, x, y + 8, z, r=2.0, n=6, lengte=1)
    sx, _, sz = V.pos(um, 4, 0)
    ZB.plek(W, "rozenkloof_stal", *V.pos(um, 6, y))
    ZB.plek(W, "rozenkloof_stalmeester", *V.pos(um - 3, 2, y))
    W.label(sx, y + 12, sz, "Bultguh-stal")
    W._stal = (V, um, y)
    return ("stal", PT._binnen(V, um, 0, y), PT._binnen(V, um, 6, y))


# ============================================================================================ stairs
def trappen(W):
    for (bi, side, k) in sorted(TRAPPEN):
        B = BAYLIST[bi]
        V = B.vlak(W, side, k)
        L, U = B.L[k], B.U(side, k)
        n = U - L
        s0 = B.x1 + 2
        if not raakt(W, B.x1, L - 2, V.c - 6, B.x2, U + 4, V.c + 6):
            continue
        # entry from the ledge in front
        V.lucht(s0 - 2, s0 - 1, 0, 2, L, L + 3)
        for u in (s0 - 2, s0 - 1):
            for d in (0, 1, 2):
                V.P(u, d, L - 1, "vloer_zand")
        for i in range(n):
            u = s0 + i
            yb = L + i
            for d in (1, 2):
                V.P(u, d, yb, trap("vloer_zand", V.up))
                for yy in range(yb + 1, yb + 5):
                    V.P(u, d, yy, LUCHT)
            # the arcade on the canyon side: parapet up to the step + 1, openings above, a column every 4
            V.P(u, 0, yb + 1, "zs" if i % 4 else "marmer_glad")
            V.P(u, 0, yb, "zs")
            if i % 4 == 0:
                for yy in range(yb + 2, min(yb + 5, U - 1)):
                    V.P(u, 0, yy, "zuil")
                if yb + 5 < U - 1:
                    V.P(u, 0, yb + 5, "zuilkop")
            else:
                for yy in range(yb + 2, min(yb + 5, U - 1)):
                    V.P(u, 0, yy, LUCHT)
                if yb + 5 < U - 1:
                    V.P(u, 0, yb + 5, trap("zs", V.out, half="top"))
            if i % 6 == 3 and yb + 5 < U - 1:
                V.P(u, 2, yb + 4, mlamp(True))
            V.P(u, 3, yb + 1, "zs")                      # inner wall stays (no peeking into rooms)
        # the top: an opening in the ledge above; railing beside it (d 3) and lamp
        for i in range(max(0, n - 5), n):
            V.P(s0 + i, 3, U, MURO)
        for u in (s0 + n, s0 + n + 1):
            for d in (1, 2):
                V.P(u, d, U - 1, "vloer_zand")
                V.lucht(u, u, d, d, U, U + 3)
        x, _, z = V.pos(s0 + n - 6, 3, 0)
        zet(W, x, U + 1, z, mlamp(False))
        if not hasattr(W, "_trap_cellen"):
            W._trap_cellen = set()
        for i in range(-2, n + 2):
            for d in range(-1, 4):
                W._trap_cellen.add(V.wereld(s0 + i, d))


# ============================================================================================ rims, lights, gardens
def randen(W):
    """Railings on every ledge/terrace edge (a drop of 3+), lamps on them, planters and trailing hanging gardens."""
    gereserveerd = set(getattr(W, "_vrij", set()))
    gereserveerd |= set(getattr(W, "_trap_cellen", set()))
    x1, z1 = X0, BOX[2]
    xs = range(X0 + 6, X1 + 1)
    zs = range(-800, -630)
    top = {}
    for x in range(X0 + 5, X1 + 2):
        for z in range(-801, -629):
            if x >= X1:
                top[(x, z)] = int(math.floor(W.H[W.vx(x), W.vz(z)])) if W.binnen(x, 100, z) else _nat_h(x, z)
            else:
                hn = int(math.floor(W.H[W.vx(x), W.vz(z)])) if W.binnen(x, 100, z) else None
                top[(x, z)] = oppervlak(x, z, hn)
    teller = 0
    for x in xs:
        for z in zs:
            if not W.binnen(x, 100, z):
                continue
            t = top[(x, z)]
            buren = [top.get((x + dx, z + dz), t) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            laag = min(buren)
            if t - laag < 3 or (x, z) in gereserveerd:
                continue
            if t < 60:
                continue
            if W.wget(x, t + 1, z) != LUCHT:
                continue
            if W.wget(x, t, z) == LUCHT:
                continue
            rng = rng_van(x, z, 41)
            teller += 1
            if rng.rand() < 0.14 and x < X1:
                zet(W, x, t + 1, z, "palm_planken")
                zet(W, x, t + 2, z, ZB.BLOEMEN[rng.randint(len(ZB.BLOEMEN))].replace("[half=lower]", ""))
            else:
                zet(W, x, t + 1, z, MURO)
                if (x * 7 + z * 3) % 11 == 0:
                    zet(W, x, t + 2, z, mlamp(False))
            # trailing hanging garden over the drop
            if rng.rand() < 0.07:
                dx, dz = [(dx, dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if top.get((x + dx, z + dz), t) == laag][0]
                for dy in range(0, rng.randint(3, 8)):
                    yy = t - dy
                    if yy <= laag + 4:
                        break
                    if W.wget(x + dx, yy, z + dz) == LUCHT:
                        zet(W, x + dx, yy, z + dz, "minecraft:flowering_azalea_leaves" if rng.rand() < 0.5 else
                            "minecraft:cherry_leaves")


def lantaarnpaal(W, x, y, z):
    zet(W, x, y, z, "marmer_glad")
    zet(W, x, y + 1, z, HEK)
    zet(W, x, y + 2, z, HEK)
    zet(W, x, y + 3, z, mlamp(False))


def vloerlicht(W):
    """Lamp posts along both promenades of the canyon floor + lantern strings across the canyon at ledge height."""
    for B in BAYLIST[1:]:
        for side in (N, S):
            for x in range(B.x1 + 3, B.x2, 7):
                z = B.z(side, B.c[0] - 5)
                if abs(x - PLEIN[0]) < 23 and abs(z - PLEIN[1]) < 11:
                    continue
                if W.binnen(x, B.L0, z) and W.wget(x, B.L0, z) == LUCHT and W.wget(x, B.L0 - 1, z) != LUCHT \
                        and not W.wget(x, B.L0 - 1, z).startswith("minecraft:water"):
                    lantaarnpaal(W, x, B.L0, z)
    for lx in range(3334, 3445, 13):
        B = bay_van(lx)
        y = B.L[1] + 6
        za, zb = B.z(N, B.c[0]) - 1, B.z(S, B.c[0]) + 1
        n = zb - za
        for i in range(n + 1):
            t = i / n
            yy = y - 3.5 * 4 * t * (1 - t)
            zet(W, lx, yy, za + i, "minecraft:iron_chain[axis=z,waterlogged=false]")
            if i % 5 == 2:
                zet(W, lx, yy - 1, za + i, mlamp(True))


# ============================================================================================ bridges
def bruggen(W):
    if not hasattr(W, "_vrij"):
        W._vrij = set()
    for bx, naam in BRUGGEN:
        B = bay_van(bx)
        zn, zs_ = B.z(N, B.c[1]), B.z(S, B.c[1])
        y = B.L[2]
        if not raakt(W, bx - 4, y - 16, zn, bx + 4, y + 4, zs_):
            continue
        L = zs_ - zn
        for z in range(zn, zs_ + 1):
            t = (z - zn) / L
            boog = y - 2 - int(round(11 * (2 * t - 1) ** 2))
            for xx in range(bx - 2, bx + 3):
                zet(W, xx, y - 1, z, "vloer_dambord" if xx == bx else "vloer_zand")
                zet(W, xx, y - 2, z, "zs")
                for yy in range(boog, y - 2):
                    if xx in (bx - 2, bx + 2) or yy == boog:
                        zet(W, xx, yy, z, "zs" if yy > boog else "goud")
                for yy in range(y, y + 3):
                    zet(W, xx, yy, z, LUCHT)
            for xx in (bx - 2, bx + 2):
                if zn < z < zs_:
                    zet(W, xx, y, z, MURO if (z - zn) % 6 else "marmer_glad")
                    if (z - zn) % 6 == 0:
                        zet(W, xx, y + 1, z, mlamp(False))
            W._vrij.update({(xx, z) for xx in range(bx - 1, bx + 2)})
        for zz in (zn, zs_):
            for xx in range(bx - 1, bx + 2):
                W._vrij.add((xx, zz))
                W._vrij.add((xx, zz - (1 if zz == zn else -1) * -1))
        zm = (zn + zs_) // 2
        for yy in range(y - 7, y - 2):
            zet(W, bx, yy, zm, WOL["roze"] if yy % 2 else "zijde")
        ZB.plek(W, "rozenkloof_" + naam, bx, y, zm)
    # rope bridge between the rim terraces
    B = bay_van(TOUWBRUG_X)
    zn, zs_ = B.z(N, B.c[2]), B.z(S, B.c[2])
    yn, ys = B.LT[N], B.LT[S]
    if raakt(W, TOUWBRUG_X - 3, min(yn, ys) - 8, zn, TOUWBRUG_X + 3, max(yn, ys) + 3, zs_):
        L = zs_ - zn
        for z in range(zn, zs_ + 1):
            t = (z - zn) / L
            yy = int(round(yn - 1 + (ys - yn) * t - 3 * 4 * t * (1 - t)))
            for xx in (TOUWBRUG_X - 1, TOUWBRUG_X, TOUWBRUG_X + 1):
                zet(W, xx, yy, z, plaat("palm_planken", "top"))
                for q in range(1, 4):
                    zet(W, xx, yy + q, z, LUCHT)
            for xx in (TOUWBRUG_X - 2, TOUWBRUG_X + 2):
                zet(W, xx, yy + 1, z, HEK)
            for xx in range(TOUWBRUG_X - 1, TOUWBRUG_X + 2):
                W._vrij.add((xx, z))
        for zz, yy in ((zn, yn), (zs_, ys)):
            for xx in (TOUWBRUG_X - 2, TOUWBRUG_X + 2):
                for q in range(0, 3):
                    zet(W, xx, yy + q, zz, "palm_stam")
                zet(W, xx, yy + 3, zz, mlamp(False))


# ============================================================================================ the floor: plein, market
def plein(W, figuren=True):
    px, pz = PLEIN
    B = bay_van(px)
    yb = B.L0
    if not raakt(W, px - 24, yb - 2, pz - 12, px + 24, yb + 10, pz + 12):
        return
    for dx in range(-22, 23):
        for dz in range(-11, 12):
            x, z = px + dx, pz + dz
            if (dx / 22.0) ** 2 + (dz / 11.0) ** 2 <= 1:
                zet(W, x, yb - 1, z, "vloer_dambord" if (abs(dx) + abs(dz)) % 7 else
                    ["mozaiek_ster", "mozaiek_bloem", "mozaiek_guh"][(abs(dx) // 7) % 3])
                zet(W, x, yb - 2, z, "zs")
                leeg(W, x, yb, z, x, yb + 4, z)
    fx = px + 6
    ZB.fontein(W, fx, yb, pz, r=5, tegel="mozaiek_ster")
    ZB.plek(W, "rozenkloof_plein", px - 8, yb, pz)
    ZB.plek(W, "rozenkloof_fontein", fx, yb, pz + 7)
    for i, (mx, mz, kk) in enumerate(((3384, -706, (0, -1)), (3392, -705, (0, -1)), (3414, -705, (0, -1)),
                                      (3421, -707, (0, -1)), (3373, -723, (0, 1)), (3420, -722, (0, 1)))):
        ZB.kraam(W, mx, yb, mz, kant=kk, seed=i)
    for (lx, lz) in ((px - 16, pz - 9), (px - 16, pz + 9), (px + 18, pz - 8), (px + 18, pz + 8), (px - 2, pz - 10),
                     (px - 2, pz + 10)):
        lantaarnpaal(W, lx, yb, lz)
    # campfire (unlit = the kern checkpoint) + its stand spot
    zet(W, px - 12, yb, pz + 2, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    for k in range(4):
        a = k * math.pi / 2 + 0.4
        zet(W, px - 12 + round(math.cos(a) * 2), yb, pz + 2 + round(math.sin(a) * 2), PT.kussen("pink"))
    ZB.plek(W, "kampvuur_rozenkloof_plein", px - 10, yb, pz + 4)
    ZB.plek(W, "rozenkloof_oma_dadel", px + 12, yb, pz - 4)
    ZB.plek(W, "rozenkloof_bobbel", px + 14, yb, pz - 3)
    ZB.plek(W, "rozenkloof_tuinguh", px - 18, yb, pz + 2)
    W.label(px, yb + 9, pz, "Het Rozenplein")
    # palms along the stream (outside the plaza)
    for i, x in enumerate(range(3332, 3425, 15)):
        Bx = bay_van(x)
        for side in (N, S):
            z = Bx.czr + side * (5 + (i % 3))
            if abs(x - px) < 24 and abs(z - pz) < 12:
                continue
            if W.binnen(x, Bx.L0, z) and W.wget(x, Bx.L0 - 1, z) != LUCHT and not W.wget(x, Bx.L0 - 1, z).startswith(
                    "minecraft:water") and W.wget(x, Bx.L0, z) == LUCHT:
                ZB.palm(W, x, Bx.L0, z, seed=i)
    # little bridges over the stream
    for bx in range(3330, 3470, 22):
        Bx = bay_van(bx)
        for dz in range(-3, 4):
            for xx in (bx, bx + 1):
                if W.wget(xx, Bx.L0 - 1, Bx.czr + dz).startswith("minecraft:water") or W.wget(xx, Bx.L0 - 2,
                                                                                            Bx.czr + dz).startswith("minecraft:water"):
                    zet(W, xx, Bx.L0 - 1, Bx.czr + dz, plaat("palm_planken", "top"))
    if figuren:
        import zijfiguren as ZF
        rng = np.random.RandomState(3)
        for kx in range(30):
            gx, gz = px + rng.uniform(-20, 20), pz + rng.uniform(-9, 9)
            if math.hypot(gx - fx, gz - pz) < 7:
                continue
            W.wguh(gx, yb, gz, rng.uniform(0, 360), skin=ZF.SKINS[rng.randint(len(ZF.SKINS))])
        ZF.zet_tapijt(W, px - 4.5, yb, pz + 7.5, 250, patroon=1, fase=0.4, skin="guh_bloesemguh", hoogte=1.4)
        ZF.zet_bultguh(W, px + 17.5, yb, pz - 2.5, 280, variant="kaneel", deken=3, skin="guh_golden", lopend=True)
        ZF.zet_pluimguh(W, px - 19.5, yb, pz - 4.5, 100, "mint", ruiter=False)


def figuren_rest(W):
    import zijfiguren as ZF
    st = getattr(W, "_stal", None)
    if st:
        V, um, y = st
        for i, (du, d, yaw) in enumerate(((-4, 9, 0), (0, 10, 30), (4, 9, 330))):
            x, _, z = V.pos(um + du + 0.5, d, 0)
            ZF.zet_bultguh(W, x, y, z + 0.5, yaw, variant=["saffraan", "zandroze", "wolkwit"][i], deken=i, ruiter=False)
        x, _, z = V.pos(um + 0.5, -3, 0)
        ZF.zet_bultguh(W, x + 3, y, z + 0.5, 90, variant="kaneel", deken=2, ruiter=True, skin="guh_choco")
        ZF.zet_pluimguh(W, x - 4, y, z + 0.5, 70, "honing")
        ZF.zet_tapijt(W, x - 7, y, z + 0.5, 0, patroon=2, ruiter=False, hoogte=0.9)
    tw = getattr(W, "_tapijtwinkel", None)
    if tw:
        V, um, y = tw
        x, _, z = V.pos(um - 3 + 0.5, -2, 0)
        ZF.zet_tapijt(W, x, y, z + 0.5, 90, patroon=0, ruiter=False, hoogte=0.8)
        x, _, z = V.pos(um + 3 + 0.5, -2, 0)
        ZF.zet_tapijt(W, x, y, z + 0.5, 90, patroon=2, ruiter=True, skin="guh_konijntje_roze", hoogte=1.2)
        x, _, z = V.pos(um + 0.5, -5, 0)
        ZF.zet_bultguh(W, x, y, z + 0.5, 270, variant="zandroze", deken=4, ruiter=False)


# ============================================================================================ De Schatkamer
def schatkamer(W):
    """The great facade in the end wall (faces west) and Kloofmoeder Roosje's hall behind it."""
    B = BAYLIST[-1]
    cz = B.czr
    y = B.L0
    V = Vlak(W, (-1, 0), X1)                     # first rock cell x 3479, rock towards +x
    if not raakt(W, X1 - 6, y - 3, cz - 16, X1 + 40, y + 48, cz + 16):
        return
    # [u-z6afronding] rebuilt as a truly deep Al-Khazneh: the whole facade sits FD blocks back in a rough rock frame with a
    # shadow slot around it; a recessed portico with free-standing ROUND columns, a projecting multi-layer entablature + a
    # projecting pediment with a guh medallion; above it two projecting pavilions with half pediments (the broken pediment)
    # and a round tholos with free columns, a cone and an urn, recessed into the rock under the plateau.
    hw = 15                                      # half width of the facade field
    FD = 3                                       # facade plane depth (the frame recess)
    y0 = y + 2                                   # portico floor stand
    yb = y0 + 22                                 # upper order floor stand (geheimen(): the hidden stair arrives here)
    ytop = yb + 17                               # top of the field (the rock is built up over it where the plateau is lower)
    # make sure the end wall is solid rock over the facade's width, depth and height
    for u in range(cz - hw - 4, cz + hw + 5):
        for d in range(0, 30):              # a rock crown over the facade where the plateau is lower (the tholos needs a
            # canopy): full height up to d 14 and over the field width, then falling off in rough steps
            du_ = max(0, abs(u - cz) - hw - 1)
            kroon = ytop + 4 - int(max(0, d - 14) * 1.3 + du_ * 2.5 + SK.hash3(u, d, cz, 23) * 2.5)
            for yy in range(y - 1, max(kroon, min(ytop + 4, _nat_h(*V.wereld(u, d)) + 1))):
                xw, _, zw = V.pos(u, d, yy)
                if W.wget(xw, yy, zw) == LUCHT:
                    zet(W, xw, yy, zw, PT.strata(xw, yy, zw))
    # ---- the rock frame: field set back FD, a deeper shadow slot around it, weathered irregular frame edge
    V.lucht(cz - hw, cz + hw, 0, FD - 1, y0, ytop)
    for yy in range(y0, ytop + 1):
        for u in (cz - hw - 1, cz + hw + 1):
            V.lucht(u, u, 0, FD + 1, yy, yy)
        for k_ in (2, 3):                        # weathering: the frame edge eaten away irregularly
            for u in (cz - hw - k_, cz + hw + k_):
                dd = int(SK.hash3(u, yy, cz, 17) * 3.2) - (k_ - 2)
                if dd > 0:
                    V.lucht(u, u, 0, dd - 1, yy, yy)
    V.lucht(cz - hw - 1, cz + hw + 1, 0, FD + 1, ytop + 1, ytop + 1)
    for u in range(cz - hw - 3, cz + hw + 4):
        dd = int(SK.hash3(u, ytop, cz, 19) * 3.0)
        if dd > 0:
            V.lucht(u, u, 0, dd - 1, ytop + 2, ytop + 2)
    for (eu, ey, er) in ((cz - hw - 3, y0 + 9, 1.6), (cz + hw + 3, y0 + 20, 1.8), (cz - hw - 2, ytop - 5, 1.4)):
        PT.erosie(V, eu, 0, ey, er, seed=eu)
    # podium: 3 steps over the whole width in front, floor into the frame
    for u in range(cz - hw, cz + hw + 1):
        for i, d in enumerate((-3, -2, -1)):
            V.P(u, d, y + i - 1, trap("marmer_glad", V.inn))
            for q in range(0, i):
                V.P(u, d, y + q - 1, "marmer_glad")
        for d in range(0, FD):
            V.P(u, d, y0 - 1, "marmer_glad")
            V.P(u, d, y0 - 2, "marmer_glad")

    def zuil_rond(u, d, y1, y2):
        """A free-standing ROUND column (plus-shaped shaft, 3 wide) with a square base, torus and a flared capital."""
        for du in (-1, 0, 1):
            for dd in (-1, 0, 1):
                V.P(u + du, d + dd, y1, "marmer_glad")
                V.P(u + du, d + dd, y2, "goud")
        for (du, dd) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            V.P(u + du, d + dd, y1 + 1, "rots_marmer")
            for yy in range(y1 + 2, y2 - 1):
                V.P(u + du, d + dd, yy, "zuil")
            V.P(u + du, d + dd, y2 - 1, "zuilkop")
        for (du, dd) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            V.P(u + du, d + dd, y2 - 1, trap("goud", V.out, half="top") if dd < 0 else plaat("goud", "top"))
            V.P(u + du, d + dd, y1 + 1, plaat("marmer_glad"))

    # ---- LOWER ORDER: portico (4 deep behind the columns), 6 free-standing round columns
    PH = 13                                       # portico clear height (y0 .. y0+12)
    PB = FD + 6                                   # portico back wall depth
    V.lucht(cz - 13, cz + 13, 0, PB - 1, y0, y0 + PH - 1)
    for u in range(cz - 13, cz + 14):
        for d in range(0, PB):
            V.P(u, d, y0 - 1, "marmer_glad" if (u + d) % 2 else ("mozaiek_ster" if abs(u - cz) < 3 else "tegel_parel"))
            V.P(u, d, y0 - 2, "marmer_glad")
    for u in (cz - 11, cz - 7, cz - 3, cz + 3, cz + 7, cz + 11):
        zuil_rond(u, FD + 1, y0, y0 + PH - 1)
    for u in range(cz - 14, cz + 15):            # portico ceiling + side walls
        for d in range(FD, PB + 1):
            V.P(u, d, y0 + PH, "rots_marmer" if (u + d) % 3 else "marmer_glad")
    for u in (cz - 14, cz + 14):
        for d in range(FD, PB + 1):
            for yy in range(y0, y0 + PH):
                V.P(u, d, yy, "zs" if (yy - y0) % 4 else "rots_roze")
    # back wall: door 5 x 8 with a gold lintel + guh keystone, two-storey statue niches in the side bays
    for u in range(cz - 13, cz + 14):
        for yy in range(y0, y0 + PH):
            V.P(u, PB, yy, "rots_roze" if (yy - y0) % 5 == 4 else "zs")
    V.lucht(cz - 2, cz + 2, PB, PB + 1, y0, y0 + 7)
    for u in range(cz - 3, cz + 4):
        V.P(u, PB - 1, y0 + 8, trap("goud", V.inn, half="top"))
    V.P(cz, PB - 1, y0 + 9, "zs_beitel")
    for yy in range(y0, y0 + 8):
        V.P(cz - 3, PB - 1, yy, "zuil")
        V.P(cz + 3, PB - 1, yy, "zuil")
    for side in (-1, 1):
        u = cz + side * 9
        for yy in (y0 + 1, y0 + 7):
            V.lucht(u - 1, u + 1, PB, PB + 1, yy, yy + 4)
            V.P(u, PB, yy + 5, "zs_beitel")
            for du in (-1, 1):
                V.P(u + du, PB, yy + 5, trap("zs", V.up if du < 0 else V.um, half="top"))
            V.doos(u - 1, u + 1, PB, PB + 1, yy - 1, yy - 1, "marmer_glad")
            PT.guhbeeld(V, u, PB + 1, yy)
            V.P(u, PB - 1, yy - 1, plaat("goud", "top"))
    lx_, _, lz_ = V.pos(cz, FD + 4, 0)
    PT.hanglampring(W, lx_ + 0.5, y0 + PH, lz_ + 0.5, r=2.6, n=8, lengte=2)
    for side in (-1, 1):
        x_, _, z_ = V.pos(cz + side * 9, FD + 3, 0)
        PT.hanglamp(W, x_, y0 + PH, z_, lengte=2)
    # ---- ENTABLATURE: architrave, carved frieze, 2-step projecting cornice
    ye = y0 + PH
    for u in range(cz - hw, cz + hw + 1):
        V.P(u, FD, ye, "marmer_glad")                                   # architrave
        V.P(u, FD - 1, ye, trap("marmer_glad", V.inn, half="top"))
        V.P(u, FD, ye + 1, "zs_beitel" if (u - cz) % 3 == 0 else "rots_roze")   # frieze of carved guhs
        V.P(u, FD - 1, ye + 1, plaat("goud", "top") if (u - cz) % 3 == 0 else LUCHT)
        V.P(u, FD, ye + 2, "goud")                                       # cornice
        V.P(u, FD - 1, ye + 2, "zs")
        V.P(u, FD - 2, ye + 2, trap("zs", V.inn, half="top"))
        for d in range(FD + 1, PB + 1):
            V.P(u, d, ye + 1, "zs")
            V.P(u, d, ye + 2, "zs")
    # ---- PEDIMENT: projects in front of the cornice line, tympanum with a round guh medallion, acroteria
    yp = ye + 3
    for u in range(cz - hw, cz + hw + 1):        # attic behind it over the whole width
        for yy in range(yp, yb - 1):
            V.P(u, FD, yy, "zs" if (yy - yp) % 3 else "rots_perzik")
    PT.fronton(V, cz, 9, yp, d=FD - 2, vul="rots_roze", motief=False)
    for (du, dy, k) in ((0, 1, PT.GEZICHT), (-1, 1, "goud"), (1, 1, "goud"), (0, 0, "goud"), (0, 2, "goud")):
        V.P(cz + du, FD - 1, yp + dy, k)          # the medallion: a guh face in a gold ring
    for u in (cz - 9, cz + 9):                    # acroteria: little guhs on the pediment corners + an urn on the apex
        PT.guhbeeld(V, u, FD - 2, yp + 2, sokkel="goud")
    V.P(cz, FD - 2, yp + 7, "minecraft:decorated_pot[facing=west,waterlogged=false,cracked=false]")
    for u in range(cz - hw, cz + hw + 1):
        V.P(u, FD, yb - 2, "goud")
        V.P(u, FD - 1, yb - 2, trap("zs", V.inn, half="top"))
    # ---- UPPER ORDER: central tholos recess (deep), two projecting pavilions with half pediments (broken pediment)
    V.lucht(cz - 7, cz + 7, 0, 13, yb, ytop)
    for u in range(cz - 7, cz + 8):
        for d in range(0, 14):
            V.P(u, d, yb - 1, "marmer_glad" if (u + d) % 2 else "tegel_parel")
    tx_, _, tz_ = V.pos(cz, FD + 5, 0)
    tx_, tz_ = int(tx_), int(tz_)
    th = max(5, min(8, ytop - yb - 9))
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            r_ = math.hypot(dx, dz)
            if r_ <= 4.3:
                zet(W, tx_ + dx, yb - 1, tz_ + dz, "goud" if r_ > 3.4 else "mozaiek_ster")        # round podium
            if r_ <= 2.3:                                                                        # the drum (cella)
                for yy in range(yb, yb + th):
                    zet(W, tx_ + dx, yy, tz_ + dz, "rots_roze" if (yy - yb) % 3 else "marmer_glad")
    zet(W, tx_ - 2, yb, tz_, LUCHT)                               # a little doorway in the drum (faces the canyon)
    zet(W, tx_ - 2, yb + 1, tz_, LUCHT)
    zet(W, tx_ - 2, yb + 3, tz_, PT.GEZICHT)
    for kq in range(10):                                          # free-standing columns round the drum
        a_ = kq * 2 * math.pi / 10
        px_, pz_ = tx_ + round(math.cos(a_) * 3.6), tz_ + round(math.sin(a_) * 3.6)
        zet(W, px_, yb, pz_, "marmer_glad")
        for yy in range(yb + 1, yb + th):
            zet(W, px_, yy, pz_, "zuil")
        zet(W, px_, yb + th, pz_, "zuilkop")
    for dx in range(-5, 6):                                       # entablature ring
        for dz in range(-5, 6):
            r_ = math.hypot(dx, dz)
            if r_ <= 4.6:
                zet(W, tx_ + dx, yb + th + 1, tz_ + dz, "goud" if r_ > 3.6 else "marmer_glad")
            if 3.6 < r_ <= 4.6 and (dx * 7 + dz * 3) % 3 == 0:
                zet(W, tx_ + dx, yb + th + 2, tz_ + dz, plaat("goud"))
    kh = min(4.6, max(2.0, ytop - (yb + th + 2) - 3))
    ZB.koepel(W, tx_ + 0.0, yb + th + 2, tz_ + 0.0, 3.4, k_dak="rots_marmer", vorm="spits", ribben=8, rib="goud",
              oortjes=False, hoogte=kh)
    yu = min(ytop, yb + th + 3 + int(math.ceil(kh)) + 1)
    zet(W, tx_, yu - 1, tz_, "goudblok")                          # the urn (finial)
    zet(W, tx_, yu, tz_, "minecraft:decorated_pot[facing=west,waterlogged=false,cracked=false]")
    for side in (-1, 1):                                          # the pavilions
        ua, ub = cz + side * 8, cz + side * 14
        lo, hi = min(ua, ub), max(ua, ub)
        V.doos(lo, hi, 0, FD - 1, yb - 1, yb - 1, "marmer_glad")  # the projecting pavilion plinth
        for u in range(lo, hi + 1):
            V.P(u, -1, yb - 1, trap("marmer_glad", V.inn, half="top"))
        V.lucht(lo + 1, hi - 1, 1, FD + 2, yb, yb + 9)            # the pavilion's own deep niche behind its columns
        for u in range(lo + 1, hi):
            for d in range(1, FD + 3):
                V.P(u, d, yb - 1, "marmer_glad")
        for u in (ua, ub):
            PT.zuil(V, u, 0, yb, yb + 9)
            PT.zuil(V, u, 1, yb, yb + 9)
        PT.guhbeeld(V, (ua + ub) // 2, FD + 1, yb, sokkel="goud")
        V.P((ua + ub) // 2, FD + 3, yb + 4, "zs_beitel")
        for u in range(lo, hi + 1):                               # pavilion entablature
            V.P(u, 0, yb + 10, "goud")
            V.P(u, 1, yb + 10, "goud")
            V.P(u, 0, yb + 11, "zs_beitel" if (u - ua) % 2 == 0 else "rots_roze")
            V.P(u, -1, yb + 11, trap("zs", V.inn, half="top"))
            for d in range(1, FD + 3):
                V.P(u, d, yb + 10, "zs")
        for q in range(4):                                        # half pediment rising towards the centre (broken)
            for u in range(lo, hi + 1):
                k = (u - ub) * -side                              # 0 at the outer end, growing towards the tholos
                if k >= 2 * q:
                    V.P(u, 0, yb + 12 + q, "zs" if k > 2 * q else trap("zs", V.um if side > 0 else V.up))
                    V.P(u, 1, yb + 12 + q, "rots_roze")
        PT.guhbeeld(V, ub, 0, yb + 12, sokkel="goud")           # acroterion on the outer corner
    W.label(X1, ytop + 3, cz, "De Schatkamer (hal van Kloofmoeder Roosje)")
    yt = ytop
    # the forecourt: paved, with a star of mosaic and four lamp posts
    for x in range(3452, X1 - 3):
        for z in range(cz - 11, cz + 12):
            if W.wget(x, y - 1, z) != LUCHT and not W.wget(x, y - 1, z).startswith("minecraft:water") and W.wget(x, y, z) == LUCHT:
                d = math.hypot(x - 3466, z - cz)
                zet(W, x, y - 1, z, ["mozaiek_ster", "mozaiek_bloem"][int(d) % 2] if d < 6 else "vloer_dambord")
    for (lx, lz) in ((3456, cz - 9), (3456, cz + 9), (3472, cz - 10), (3472, cz + 10)):
        lantaarnpaal(W, lx, y, lz)
    ZB.plek(W, "rozenkloof_schatkamer", X1 - 5, y, cz)
    # ---- the hall
    hx1, hx2 = X1 + 12, X1 + 39                     # [u-z6afronding] behind the deeper portico (back wall X1 + 9)
    hz1, hz2 = cz - 12, cz + 12
    hy = y0
    H_ = 14
    for x in range(X1 + 9, X1 + 12):                 # passage
        for z in range(cz - 2, cz + 3):
            for yy in range(hy, hy + 9):
                zet(W, x, yy, z, LUCHT)
            zet(W, x, hy - 1, z, "marmer_glad")
    for x in range(hx1, hx2 + 1):
        for z in range(hz1, hz2 + 1):
            # stepped (coffered) ceiling: higher in the middle
            dz = abs(z - cz)
            top = hy + H_ - (0 if dz < 6 else (1 if dz < 9 else 3))
            for yy in range(hy, top):
                zet(W, x, yy, z, LUCHT)
            zet(W, x, top, z, "rots_marmer" if (x + z) % 2 else "zs")
            k = "marmer_glad"
            if dz <= 2:
                k = "mozaiek_ster" if (x % 4 == 0) else "tegel_parel"
            elif (x + z) % 5 == 0:
                k = "mozaiek_bloem"
            zet(W, x, hy - 1, z, k)
    for x in range(hx1, hx2 + 1):                    # wall bands: a frieze of carved guhs, a gold band, a wainscot
        for z in (hz1 - 1, hz2 + 1):
            if W.wget(x, hy + 2, z) != LUCHT:
                zet(W, x, hy + 9, z, "goud")
                zet(W, x, hy + 10, z, "zs_beitel" if x % 3 == 0 else "rots_marmer")
                zet(W, x, hy, z, "mozaiek_bloem" if x % 2 else "marmer_glad")
                zet(W, x, hy + 1, z, "marmer_glad")
    for x in range(hx1, hx2 - 4):                    # runner carpet
        zet(W, x, hy, cz, TAPIJT["magenta"])
        zet(W, x, hy, cz - 1, TAPIJT["roze"]) if False else None
    for x in range(hx1 + 2, hx2 - 3, 5):             # two rows of columns + lamp rings between
        for z in (cz - 7, cz + 7):
            for yy in range(hy, hy + H_ - 2):
                zet(W, x, yy, z, "zuil")
            zet(W, x, hy + H_ - 2, z, "zuilkop")
            zet(W, x, hy + H_ - 1, z, "goud")
        PT.hanglampring(W, x + 2.5, hy + H_, cz + 0.5, r=2.6, n=10, lengte=3)
    for x in range(hx1 + 1, hx2, 4):                 # side niches with statues
        for z in (hz1, hz2):
            zz = z - 1 if z == hz1 else z + 1
            for yy in range(hy, hy + 5):
                zet(W, x, yy, zz, LUCHT)
            zet(W, x, hy, zz, "marmer_glad")
            zet(W, x, hy + 1, zz, "wit")
            zet(W, x, hy + 2, zz, PT.GEZICHT)
            zet(W, x, hy + 5, zz, "zs_beitel")
    # the dais at the back with Roosje's seat under a mosaic apse
    for x in range(hx2 - 4, hx2 + 1):
        for z in range(cz - 4, cz + 5):
            zet(W, x, hy, z, "marmer_glad" if x > hx2 - 4 else trap("marmer_glad", "east"))
    zet(W, hx2 - 1, hy + 1, cz, "minecraft:cherry_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
    zet(W, hx2 - 1, hy + 1, cz - 1, PT.kussen("magenta"))
    zet(W, hx2 - 1, hy + 1, cz + 1, PT.kussen("magenta"))
    for z in range(cz - 4, cz + 5):
        for yy in range(hy + 1, hy + 10):
            if math.hypot(z - cz, (yy - hy - 1) * 0.7) < 5.5:
                zet(W, hx2 + 1, yy, z, ["mozaiek_ster", "mozaiek_guh", "mozaiek_bloem"][(z + yy) % 3] if (z + yy) % 2 else "tegel_parel")
    for z in (cz - 3, cz + 3):
        zet(W, hx2 - 2, hy + 1, z, "minecraft:pink_candle[candles=4,lit=true,waterlogged=false]")
    ZB.plek(W, "rozenkloof_schatkamer_hal", hx1 + 3, hy, cz + 2)
    ZB.plek(W, "rozenkloof_kloofmoeder", hx2 - 5, hy, cz + 2)
    W._schatkamer_huis = ("hal", (X1 + 8, y0, cz), (hx1 + 3, hy, cz + 2))


# ============================================================================================ Siq, Zeemond, lookout
def siq(W):
    """The Siq: gate arch over the entrance, lamp niches along the walls, the stand spots."""
    P = ZW.SIQ
    # lamp niches every ~9 blocks on both walls
    for i in range(len(P) - 1):
        (ax, az, ahw, ab), (cx_, cz_, chw, cb) = P[i], P[i + 1]
        L = math.hypot(cx_ - ax, cz_ - az)
        dx, dz = (cx_ - ax) / L, (cz_ - az) / L
        for s in np.arange(4, L, 9):
            px, pz = ax + dx * s, az + dz * s
            hw = ahw + (chw - ahw) * s / L
            yb = int(math.floor(ab + (cb - ab) * s / L)) + 1
            for sgn in (-1, 1):
                nx, nz = px - dz * sgn * (hw + 0.6), pz + dx * sgn * (hw + 0.6)
                if W.binnen(nx, yb, nz):
                    zet(W, nx, yb + 2, nz, LUCHT)
                    zet(W, nx, yb + 3, nz, LUCHT)
                    zet(W, nx, yb + 1, nz, "marmer_glad")
                    zet(W, nx, yb + 2, nz, mlamp(False))
                    zet(W, nx, yb + 4, nz, "zs_beitel")
    # gate arch where the Siq leaves the canyon
    (ax, az, ahw, ab), (cx_, cz_, chw, cb) = P[0], P[1]
    L = math.hypot(cx_ - ax, cz_ - az)
    dx, dz = (cx_ - ax) / L, (cz_ - az) / L
    s = 10.0
    px, pz = ax + dx * s, az + dz * s
    hw = ahw + (chw - ahw) * s / L
    yb = int(math.floor(ab + (cb - ab) * s / L)) + 1
    for q in np.arange(-hw - 1.5, hw + 1.6, 0.5):
        ax_, az_ = px - dz * q, pz + dx * q
        boogy = yb + 7 + int(round(2.5 * (1 - (abs(q) / (hw + 0.5)) ** 2) ** 0.5)) if abs(q) <= hw else yb
        for yy in range(boogy, yb + 11):
            zet(W, ax_, yy, az_, "zs")
        zet(W, ax_, yb + 11, az_, "goud")
        zet(W, ax_, yb + 12, az_, plaat("zs"))
    zet(W, px, yb + 8, pz, "zs_beitel")
    for yy in (yb + 6, yb + 7, yb + 8):
        zet(W, px, yy, pz, KETTING)
    zet(W, px, yb + 5, pz, mlamp(True))
    ZB.plek(W, "rozenkloof_siq_onder", ax - 4, bay_van(ax - 4).L0, az + 3)
    tx, tz, _, tb = P[-1]
    ZB.plek(W, "rozenkloof_siq_boven", tx, None, tz)
    W.label(px, yb + 16, pz, "De Siq (naar het plateau en de karavaanweg)")


def zeemond(W, figuren=True):
    """The pier at the sea mouth, two guh colossi guarding the canyon, the Hoogvads lookout on the north rim."""
    for xx in range(3236, 3297):
        for zz in (-746, -745, -744):
            zet(W, xx, ZW.ZEE + 1, zz, plaat("palm_planken", "top"))
            for q in range(1, 4):
                zet(W, xx, ZW.ZEE + 1 + q, zz, LUCHT)
        if xx % 6 == 0:
            for zz in (-747, -743):
                for y in range(ZW.ZEE - 4, ZW.ZEE + 3):
                    zet(W, xx, y, zz, "palm_stam")
                zet(W, xx, ZW.ZEE + 3, zz, mlamp(False))
    ZB.plek(W, "rozenkloof_zeemond", 3272, ZW.ZEE + 2, -745)   # [u-z6afronding] x < 3264 is outside the tiles
    W.label(3270, 82, -745, "De Zeemond")
    # the colossi in bay 1, in front of face 0
    B = BAYLIST[1]
    for side in (N, S):
        z = B.z(side, B.c[0] - 5)
        PT.kolos(W, B.x1 + 5, B.L0, z, kijk=(0, -side), schaal=1.0)
    # the lookout balcony on the north rim terrace of bay 1, jutting out west over the mouth
    yb = B.LT[N]
    zc = B.z(N, B.c[2] + 4)
    if raakt(W, 3310, yb - 6, zc - 5, 3330, yb + 4, zc + 5):
        for xx in range(3312, 3331):
            for zz in range(zc - 3, zc + 4):
                zet(W, xx, yb - 1, zz, "vloer_dambord" if (xx + zz) % 2 else "marmer_glad")
                for q in range(0, 4):
                    zet(W, xx, yb + q, zz, LUCHT)
                if xx < 3324:
                    zet(W, xx, yb - 2, zz, trap("zs", "east", half="top") if xx == 3312 else "zs")
        for xx in range(3312, 3324):
            for zz in (zc - 4, zc + 4):
                zet(W, xx, yb, zz, MURO)
        for zz in range(zc - 4, zc + 5):
            zet(W, 3311, yb, zz, MURO)
        zet(W, 3311, yb + 1, zc - 4, mlamp(False))
        zet(W, 3311, yb + 1, zc + 4, mlamp(False))
        zet(W, 3313, yb, zc, "minecraft:copper_block")
        zet(W, 3313, yb + 1, zc, "minecraft:lightning_rod[facing=west,powered=false,waterlogged=false]")
        zet(W, 3315, yb, zc + 2, trap("palm_planken", "east"))
        zet(W, 3315, yb, zc - 2, trap("palm_planken", "east"))
        ZB.plek(W, "uitkijk_zeemond", 3314, yb, zc)
        if not hasattr(W, "_vrij"):
            W._vrij = set()
        W._vrij.update({(xx, zz) for xx in range(3310, 3331) for zz in range(zc - 4, zc + 5)})


# ============================================================================================ rim pavilions + gardens
def paviljoens(W):
    """Little domed houses and hanging-garden terraces on the rim terraces (post 6, tuin 3, a few houses)."""
    plekken = [(BAYLIST[3], N, "post"), (BAYLIST[6], S, "tuin"), (BAYLIST[9], S, "huis"), (BAYLIST[2], N, "tuin2")]
    for B, side, rol in plekken:
        x = int(B.xm)
        z = B.z(side, B.c[2] + RAND // 2 + 1)
        y = B.LT[side]
        if not raakt(W, x - 6, y - 2, z - 6, x + 6, y + 12, z + 6):
            continue
        if rol in ("post", "huis"):
            doos(W, x - 3, y - 1, z - 3, x + 3, y - 1, z + 3, "marmer_glad")
            for xx in range(x - 3, x + 4):
                for zz in range(z - 3, z + 4):
                    rand = xx in (x - 3, x + 3) or zz in (z - 3, z + 3)
                    for yy in range(y, y + 5):
                        zet(W, xx, yy, zz, "muur_vanille" if rand else LUCHT)
                    zet(W, xx, y + 5, zz, "goud" if rand else "muur_vanille")
            ZB.koepel(W, x + 0.5, y + 6, z + 0.5, 3.2, k_dak="koepel_tegel", vorm="ui", ribben=6, oortjes=True)
            dz = -side
            zet(W, x, y, z + dz * 3, LUCHT)
            zet(W, x, y + 1, z + dz * 3, LUCHT)
            zet(W, x, y, z + dz * 3, f"minecraft:cherry_door[facing={FACE[(0, dz)]},half=lower,hinge=left,open=false,powered=false]")
            zet(W, x, y + 1, z + dz * 3, f"minecraft:cherry_door[facing={FACE[(0, dz)]},half=upper,hinge=left,open=false,powered=false]")
            for sx in (-3, 3):
                zet(W, x + sx, y + 2, z, "mashrabiya")
            PT.hanglamp(W, x, y + 5, z, lengte=1)
            zet(W, x - 2, y, z + side * 2, "minecraft:barrel[facing=up,open=false]")
            zet(W, x + 2, y, z + side * 2, PT.kussen("pink"))
            if rol == "post":
                ZB.plek(W, "rozenkloof_post_6", x, y, z + dz * 4)
                W._paviljoen_post = ("woning", (x, y, z + dz * 3), (x, y, z))
            else:
                W._paviljoen_huis = ("woning", (x, y, z + dz * 3), (x, y, z))
        else:
            for xx in range(x - 5, x + 6):
                for zz in (z - 3, z + 3):
                    if (xx + zz) % 3:
                        zet(W, xx, y, zz, "palm_planken")
                        zet(W, xx, y + 1, zz, "minecraft:flowering_azalea_leaves")
            ZB.palm(W, x - 4, y, z, h=8, seed=B.i)
            ZB.palm(W, x + 4, y, z, h=7, seed=B.i + 1)
            for xx in (x - 2, x + 2):
                for q in range(0, 4):
                    zet(W, xx, y + q, z - 2, HEK)
                    zet(W, xx, y + q, z + 2, HEK)
            for xx in range(x - 2, x + 3):
                for zz in range(z - 2, z + 3):
                    if (xx + zz) % 2 == 0:
                        zet(W, xx, y + 4, zz, "minecraft:flowering_azalea_leaves")
            ZB.potten(W, x - 1, z - 1, x + 1, z + 1, y, seed=B.i, dicht=0.2)
            ZB.plek(W, "rozenkloof_tuin_3" if rol == "tuin" else "rozenkloof_tuin_2_terras", x, y, z)


# ============================================================================================ de Luchthaven (line E)
def luchthaven(W, figuren=True):
    x, z = LUCHTHAVEN
    B5 = BAYLIST[5]
    LB = B5.LT[N]
    yd = LB + 26
    if not raakt(W, x - 24, LB - 10, z - 24, x + 24, yd + 30, B5.z(N, B5.c[2]) + 2):
        return
    # level plaza round the tower + a path to the rim terrace
    zrand = B5.z(N, B5.c[2] + 2)
    for xx in range(x - 13, x + 14):
        for zz in range(z - 13, zrand + 1):
            disk = math.hypot(xx - x, zz - z) <= 12.5
            pad = abs(xx - x) <= 4 and zz > z
            if not (disk or pad):
                continue
            zet(W, xx, LB - 1, zz, "vloer_dambord" if disk and math.hypot(xx - x, zz - z) < 10 else "vloer_zand")
            for yy in range(LB - 4, LB - 1):
                if W.wget(xx, yy, zz) == LUCHT:
                    zet(W, xx, yy, zz, "zs")
            for yy in range(LB, LB + 14):
                zet(W, xx, yy, zz, LUCHT)
    # the tower: round, rose stone with tile bands and arched windows
    r_out, r_in = 8.2, 6.6
    for yy in range(LB, yd - 1):
        for dx in range(-9, 10):
            for dz in range(-9, 10):
                d = math.hypot(dx, dz)
                if r_in < d <= r_out:
                    a = math.atan2(dz, dx)
                    rij = (yy - LB) % 9
                    k = "steen_roze" if rij else "koepel_tegel"
                    if math.cos(a * 8) > 0.93:
                        k = "zs" if rij else "goud"                       # pilasters
                    if rij in (3, 4, 5, 6) and 0.35 < math.cos(a * 8 + 0.39 * 8 / 2) and LB + 8 < yy and d > r_out - 1.2:
                        k = "mashrabiya" if rij < 6 else "mozaiek_ster"   # lattice windows with a tiled head
                    if yy >= yd - 4 and d > r_out - 1.2:
                        k = "zs_beitel" if (yy == yd - 3 and math.cos(a * 12) > 0.5) else ("goud" if yy == yd - 4 else "zs")
                    zet(W, x + dx, yy, z + dz, k)
                elif d <= r_in:
                    zet(W, x + dx, yy, z + dz, LUCHT)
    for kq in range(8):                                 # a base arcade: eight pointed blind arches with tile frames
        a = kq * math.pi / 4 + math.pi / 8
        for t in range(-2, 3):
            px_ = x + (r_out + 0.6) * math.cos(a) - math.sin(a) * t
            pz_ = z + (r_out + 0.6) * math.sin(a) + math.cos(a) * t
            top = 5 - (1 if abs(t) == 2 else 0)
            for yy in range(LB, LB + top):
                zet(W, px_, yy, pz_, "zs" if abs(t) == 2 else ("mozaiek_bloem" if yy == LB + 3 and t == 0 else "steen_vanille"))
            zet(W, px_, LB + top, pz_, "tegel_turkoois")
    for dx in range(-1, 2):                             # door to the south (to the rim)
        for yy in range(LB, LB + 4):
            for dz in range(6, 10):
                zet(W, x + dx, yy, z + dz, LUCHT)
    for dx in (-2, 2):
        for yy in range(LB, LB + 5):
            zet(W, x + dx, yy, z + 8, "zuil")
    for dx in range(-2, 3):
        zet(W, x + dx, LB + 4, z + 8, "goud")
        zet(W, x + dx, LB + 5, z + 8, "zs_beitel" if dx == 0 else "goud")
    # central pillar + spiral stair (counter-clockwise, 1 up per step, 3 wide)
    for yy in range(LB - 1, yd + 26):
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if yy < yd - 1 or (dx == 0 and dz == 0):
                    zet(W, x + dx, yy, z + dz, "steen_vanille" if yy < yd else "palm_stam")
    n = yd - LB
    dth = 1.0 / 3.2
    th0 = math.pi / 2                                    # starts at the door side (south)
    sector = {}
    for dx in range(-6, 7):
        for dz in range(-6, 7):
            d = math.hypot(dx, dz)
            if 1.6 < d <= 4.6:
                a = (math.atan2(dz, dx) - th0) % (2 * math.pi)
                sector[(dx, dz)] = a
    for i in range(n):
        a0 = (i * dth) % (2 * math.pi)
        a1 = a0 + dth
        ylvl = LB + i
        for (dx, dz), a in sector.items():
            aa = a if a >= a0 else a + 2 * math.pi
            if a0 <= aa < a1:
                th = math.atan2(dz, dx)
                tx_, tz_ = -math.sin(th), math.cos(th)       # tangent (counter-clockwise in x/z)
                f = FACE[(int(round(tx_)), 0)] if abs(tx_) > abs(tz_) else FACE[(0, int(round(tz_)))]
                zet(W, x + dx, ylvl, z + dz, trap("vloer_zand", f))
                zet(W, x + dx, ylvl - 1, z + dz, "steen_vanille")
                for q in range(1, 4):
                    if ylvl + q < yd - 1:
                        zet(W, x + dx, ylvl + q, z + dz, LUCHT)
        if i % 9 == 4:
            th = th0 + a0
            zet(W, x + round(math.cos(th) * 6.0), ylvl + 2, z + round(math.sin(th) * 6.0), mlamp(False)) if False else None
    for k in range(0, n, 8):                            # lamps on the inner wall
        th = th0 + (k * dth) + 0.6
        zet(W, x + round(math.cos(th) * 1.8), LB + k + 3, z + round(math.sin(th) * 1.8), mlamp(False)) if False else None
        lx_, lz_ = x + round(math.cos(th) * 6.0), z + round(math.sin(th) * 6.0)
        zet(W, lx_, LB + k + 3, lz_, "steen_vanille")
        zet(W, lx_, LB + k + 4, lz_, mlamp(False))
    # the deck
    for dx in range(-11, 12):
        for dz in range(-11, 12):
            d = math.hypot(dx, dz)
            if d <= 10.6 and not (abs(dx) <= 1 and abs(dz) <= 1):
                zet(W, x + dx, yd - 1, z + dz, "goud" if d > 9.6 else ("vloer_dambord" if d > 2 else "marmer_glad"))
                if d > 8.2:
                    zet(W, x + dx, yd - 2, z + dz, trap("steen_roze", FACE[(0, 1)] if False else "north", half="top")
                        if False else "steen_roze")
            if 9.6 < d <= 10.6 and not (abs(dx) <= 1 and dz > 0):
                zet(W, x + dx, yd, z + dz, HEK)
                if (dx + dz) % 6 == 0:
                    zet(W, x + dx, yd + 1, z + dz, mlamp(False))
    # stairwell opening in the deck at the top of the spiral + railings
    for i in range(n - 4, n):
        a0 = (i * dth) % (2 * math.pi)
        for (dx, dz), a in sector.items():
            aa = a if a >= a0 else a + 2 * math.pi
            if a0 <= aa < a0 + dth:
                zet(W, x + dx, yd - 1, z + dz, LUCHT) if LB + i < yd - 1 else None
    for i in range(n - 7, n - 4):
        a0 = (i * dth) % (2 * math.pi)
        for (dx, dz), a in sector.items():
            aa = a if a >= a0 else a + 2 * math.pi
            if a0 <= aa < a0 + dth:
                zet(W, x + dx, yd, z + dz, MURO)
    for dx in range(-7, 8):                             # deck parasol ring + mast
        for dz in range(-7, 8):
            if 2.5 < math.hypot(dx, dz) <= 3.4:
                zet(W, x + dx, yd + 24, z + dz, "goud")
    zet(W, x, yd + 26, z, "goudblok")
    zet(W, x, yd + 27, z, "minecraft:pearlescent_froglight")
    ZB.guh_oortjes(W, x, yd + 26, z, 1.4)
    for yy in range(yd + 4, yd + 12):
        zet(W, x + 1, yy, z, WOL["roze"] if yy % 2 else "zijde")
    # the pier south over the rim, open end for the airship
    zp1, zp2 = z + 11, zrand + 7
    for zz in range(zp1, zp2 + 1):
        for xx in range(x - 1, x + 2):
            zet(W, xx, yd - 1, zz, "palm_planken" if xx != x else "vloer_dambord")
            for q in range(0, 4):
                zet(W, xx, yd + q, zz, LUCHT)
        if zz < zp2:
            zet(W, x - 2, yd, zz, HEK)
            zet(W, x + 2, yd, zz, HEK)
            if (zz - zp1) % 5 == 0:
                zet(W, x - 2, yd + 1, zz, mlamp(False))
                zet(W, x + 2, yd + 1, zz, mlamp(False))
        if (zz - zp1) % 7 == 3 and zz < zrand - 2:
            for yy in range(LB - 1, yd - 1):
                zet(W, x, yy, zz, "steen_roze" if yy % 5 else "tegel_turkoois")
    for xx in range(x - 1, x + 2):
        zet(W, xx, yd, zp2 + 1, "minecraft:cherry_fence_gate[facing=south,in_wall=false,open=false,powered=false]")
    # mooring mast at the pier end
    mx, mz = x + 3, zp2 - 1
    for yy in range(yd - 1, yd + 11):
        zet(W, mx, yy, mz, "palm_stam")
    zet(W, mx, yd + 11, mz, "goudblok")
    zet(W, mx, yd + 12, mz, mlamp(False))
    zet(W, mx - 1, yd - 1, mz, "palm_planken")
    zet(W, x + 2, yd - 1, mz, "palm_planken")
    # bell, departure board
    zet(W, x + 1, yd, z + 13, "minecraft:bell[attachment=floor,facing=east,powered=false]")
    for yy in range(yd, yd + 3):
        zet(W, x + 5, yy, z + 6, HEK)
        zet(W, x + 8, yy, z + 6, HEK)
    for xx in range(x + 5, x + 9):
        zet(W, xx, yd + 3, z + 6, "minecraft:dark_oak_planks")
        zet(W, xx, yd + 4, z + 6, WOL["roze"] if xx % 2 else WOL["geel"])
    zet(W, x + 6, yd + 5, z + 6, mlamp(False))
    W.label(x, yd + 30, z, "Luchthaven Rozenkloof (lijn E naar Hoogvads)")
    # spots
    ZB.plek(W, "haven_z6_z5_vertrek", x, yd, zp2)
    ZB.plek(W, "haven_6_5_landing", x, yd, (zp1 + zp2) // 2)
    ZB.plek(W, "haven_6_5_bel", x, yd, z + 14)
    ZB.plek(W, "haven_6_5_vertrekbord", x + 6, yd, z + 7)
    ZB.plek(W, "haven_6_5_kapitein", x - 1, yd, zp2 - 2)
    ZB.plek(W, "haven_6_5_luchtschip", x, yd + 4, zp2 + 12)
    ZB.plek(W, "haven_6_5_mast", mx, yd + 11, mz)
    ZB.plek(W, "rozenkloof_aankomst", x - 4, yd, z + 7)
    ZB.plek(W, "rozenkloof_havenmeester", x - 6, yd, z + 2)
    # campfire at the tower foot
    cx_, cz_ = x + 7, z + 8
    zet(W, cx_, LB, cz_, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    zet(W, cx_ - 1, LB, cz_, PT.kussen("light_blue"))
    zet(W, cx_ + 1, LB, cz_, PT.kussen("yellow"))
    ZB.plek(W, "kampvuur_rozenkloof_luchthaven", x + 5, LB, z + 10)
    for (lx, lz) in ((x - 6, z + 11), (x + 6, z + 11), (x - 4, zrand - 2), (x + 4, zrand - 2)):
        lantaarnpaal(W, lx, LB, lz)
    if not hasattr(W, "_vrij"):
        W._vrij = set()
    W._vrij.update({(xx, zz) for xx in range(x - 4, x + 5) for zz in range(z, zrand + 3)})
    if figuren:
        import zijfiguren as ZF
        ZF.zet_bultguh(W, x - 9.5, LB, z + 13.5, 200, variant="saffraan", deken=1, ruiter=False)
        ZF.zet_tapijt(W, x + 11.5, LB, z + 4.5, 300, patroon=3, ruiter=False, hoogte=0.9)
        ZF.zet_pluimguh(W, x - 11.5, LB, z + 6.5, 40, "roze")


# ============================================================================================ secrets + extra spots
def geheimen(W):
    """Extra spots for u-z6verhaal: the date stall, the carpet display, the hidden stair to the Schatkamer's tholos,
    a treasure nook by the Schatkamer and a hidden nook on a high ledge."""
    px, pz = PLEIN
    yb = bay_van(px).L0
    # dates stall: crates of dates next to Oma Dadel
    if W.binnen(px + 12, yb, pz - 7):
        for dx in range(10, 15):
            zet(W, px + dx, yb, pz - 8, "minecraft:barrel[facing=up,open=false]" if dx % 2 else "minecraft:orange_terracotta")
        zet(W, px + 12, yb + 1, pz - 8, "minecraft:brown_terracotta")
        zet(W, px + 11, yb + 1, pz - 8, plaat("palm_planken"))
        ZB.plek(W, "rozenkloof_dadels", px + 12, yb, pz - 6)
    # carpet display by the tapijtwinkel
    tw = getattr(W, "_tapijtwinkel", None)
    if tw:
        V, um, y = tw
        ZB.plek(W, "rozenkloof_tapijten", *V.pos(um - 3, -4, y))
    # the hidden stair: south ledge 2 of bay 10 -> into the end wall -> down to the tholos recess
    B = BAYLIST[-1]
    cz = B.czr
    y2 = B.L[2]
    yb_ = B.L0 + 2 + 22                                     # tholos recess floor stand (schatkamer: y0 + 22)
    z0 = B.z(S, B.c[1] + 3)
    XS = X1 + 7                     # [u-z6afronding] behind the pavilion niches of the deeper facade (they reach X1 + 5)
    pad = [(x, z0) for x in range(X1 - 1, XS + 1)]
    for z in range(z0 - 1, cz + 7, -1):
        pad.append((XS, z))
    y = y2
    if raakt(W, X1 - 2, yb_ - 2, cz, XS + 3, y2 + 4, z0 + 2):
        for i, (x, z) in enumerate(pad):
            richting = None
            if i >= len(pad) - (z0 - cz - 8) and y > yb_:
                y -= 1
                richting = "south"                          # descending northwards: the stair rises to the south
            zet(W, x, y - 1, z, "zs")
            if richting:
                zet(W, x, y - 1, z, trap("zs", richting))
                zet(W, x, y - 2, z, "zs")
            for q in range(0, 3):
                zet(W, x, y + q, z, LUCHT)
            if i % 6 == 3:
                zet(W, x, y + 2, z, mlamp(True)) if False else None
        zet(W, XS + 1, y2 + 1, z0, mlamp(False))
        zet(W, XS + 1, y2, z0, "zs")
        for z in range(cz + 6, cz + 8):
            for q in range(0, 3):
                zet(W, XS, yb_ + q, z, LUCHT)
        # the entrance is half hidden behind hanging blossom
        zet(W, X1 - 1, y2 + 2, z0 + 1, "minecraft:flowering_azalea_leaves")
        ZB.plek(W, "schatkamer_koepeltje", X1 + 2, yb_, cz + 4)
        if not hasattr(W, "_vrij"):
            W._vrij = set()
        W._vrij.update({(x, z) for (x, z) in pad})
    # treasure nook in the north face of bay 10 (floor level), behind a pot
    zf = B.z(N, B.c[0])
    xn = 3475
    if W.binnen(xn, B.L0, zf):
        for d in range(0, 4):
            for q in range(0, 2):
                zet(W, xn, B.L0 + q, zf - d, LUCHT)
            zet(W, xn, B.L0 - 1, zf - d, "mozaiek_guh")
        zet(W, xn, B.L0, zf - 4, "minecraft:chest[facing=south,type=single,waterlogged=false]")
        zet(W, xn, B.L0 + 2, zf - 1, "zs_beitel")
        ZB.plek(W, "geheim_schat_1", xn, B.L0, zf - 2)
    # hidden nook on the south ledge 2 of bay 7 (behind the corner pilaster)
    B7 = BAYLIST[7]
    zf = B7.z(S, B7.c[2])
    xn = B7.x2 - 1
    y = B7.L[2]
    if W.binnen(xn, y, zf):
        for d in range(0, 3):
            for q in range(0, 2):
                zet(W, xn, y + q, zf + d, LUCHT)
            zet(W, xn, y - 1, zf + d, "vloer_zand")
        zet(W, xn, y + 2, zf, "minecraft:cherry_leaves")
        zet(W, xn, y, zf + 3, "minecraft:decorated_pot[facing=north,waterlogged=false,cracked=false]")
        ZB.plek(W, "geheim_knuffel_1", xn, y, zf + 1)


# ============================================================================================ the city
def _plekken_tiers(W, huizen):
    """Post houses, ledge spots and the hanging gardens of ledges 1 and 2."""
    per_tier = {0: [], 1: [], 2: []}
    for (B, side, k, rec) in huizen:
        if rec[0] in ("woning",) or rec[0].startswith("winkel"):
            per_tier[k].append((B, side, rec))
    keuze = [(0, 0), (1, 0), (1, 1), (2, 0), (2, 1)]
    nr = 1
    for (k, j) in keuze:
        lst = per_tier[k]
        if len(lst) > j:
            B, side, rec = lst[j * 2 % len(lst)] if len(lst) > 2 else lst[j]
            dx, dy, dz = rec[1]
            ZB.plek(W, f"rozenkloof_post_{nr}", dx, dy, dz - side * 3)
        nr += 1
    for k, naam in ((1, "rozenkloof_richel_1"), (2, "rozenkloof_richel_2")):
        B = bay_van(3381)
        ZB.plek(W, naam, 3383, B.L[k], B.z(N, B.c[k - 1] + 3))
    B = bay_van(3381)
    ZB.plek(W, "rozenkloof_richel_3", 3384, B.LT[N], B.z(N, B.c[2] + 4))
    for k, naam, bx in ((1, "rozenkloof_tuin_1", 3361), (2, "rozenkloof_tuin_2", 3414)):
        B = bay_van(bx)
        z = B.z(N, B.c[k - 1] + 1)
        y = B.L[k]
        ZB.plek(W, naam, bx, y, B.z(N, B.c[k - 1] + 3))
        if W.binnen(bx, y, z):
            for xx in range(bx - 2, bx + 3):
                zet(W, xx, y, B.z(N, B.c[k] - 1), "palm_planken")
                zet(W, xx, y + 1, B.z(N, B.c[k] - 1), ZB.BLOEMEN[(xx + k) % len(ZB.BLOEMEN)].replace("[half=lower]", ""))


def bouw(W, figuren=True):
    """Builds the whole canyon city (only what falls in the window)."""
    if not raakt(W, *BOX, marge=0):
        return
    W._vrij = set()
    # 1. the rock: strata over the natural rock around, then the designed terraces
    _strata_box(W, BOX[0], BOX[2], BOX[3], BOX[5], 40, 150)
    herbouw(W)
    # 2. architecture
    huizen = facades(W)
    trappen(W)
    schatkamer(W)
    siq(W)
    zeemond(W, figuren)
    plein(W, figuren)
    bruggen(W)
    luchthaven(W, figuren)
    paviljoens(W)
    _plekken_tiers(W, huizen)
    geheimen(W)
    # 3. railings, gardens, light
    randen(W)
    vloerlicht(W)
    if figuren:
        figuren_rest(W)
    # 4. houses with a role
    tel = {}
    for (B, side, k, rec) in huizen:
        rol, deur_, binnen = rec
        tel[rol] = tel.get(rol, 0) + 1
        hid = f"rozenkloof_{rol}" + (f"_{tel[rol]}" if rol in ("woning", "schrijn") or tel[rol] > 1 else "")
        ZB.huis_rol(W, hid, rol, deur_, binnen)
    if hasattr(W, "_schatkamer_huis"):
        ZB.huis_rol(W, "rozenkloof_schatkamer", *W._schatkamer_huis)
    for a, hid in (("_paviljoen_post", "rozenkloof_paviljoen_post"), ("_paviljoen_huis", "rozenkloof_paviljoen")):
        if hasattr(W, a):
            ZB.huis_rol(W, hid, *getattr(W, a))
    W.label(3400, 128, -760, "Rozenkloof")
