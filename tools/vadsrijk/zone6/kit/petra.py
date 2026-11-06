"""Petra-kit for Rozenkloof (zone 6, U-Z6WERELD rozenkloof fork): rock-cut architecture with real depth.

Everything works on a face plane ("Vlak") in the rock: u runs along the face, d is the depth INTO the rock (d = 0 is the
first rock cell of the face, d < 0 is out in the air in front of it), y is absolute. One facade = one bay of a canyon face,
carved into solid rock (porticos with columns + capitals, pediments, recessed doorways, statue niches, oriels with
mashrabiya, shop fronts with silk awnings, colonnaded galleries, shrines), plus the interiors behind them (real floors,
round hanging brass lamps on chains).

Also here: the natural rock strata (wavy bands, not stripes) and small generic pieces (guh statues, lamps, doors).
Deterministic: every random choice comes from rng_van(position).
"""
import math
import numpy as np
import schemerkit as SK
import zijbouw as ZB
from zijbouw import zet, rng_van, FACE
from zpalet import WOL, TAPIJT, b, trap, plaat

LUCHT = "minecraft:air"
G = "guhs:"
GEZICHT = G + "roze_marmer_guhgezicht"
MURO = G + "rozenzandsteen_muur"
HEK = G + "dadelpalm_hek"
KETTING = "minecraft:iron_chain[axis=y,waterlogged=false]"


def mlamp(hangend=True):
    return f"guhs:messing_lantaarn[hanging={'true' if hangend else 'false'},waterlogged=false]"


# ============================================================================================ strata
# The rock of the canyon: irregular wavy bands of pinks, peach, cream and rose-red with varying thickness.
_BAND_MAT = ["rots", "rots_perzik", "zs", "rots_roze", "rots_rood", "rots_marmer", "muur_vanille", "rots", "zs",
             "rots_perzik", "rots_diep"]
_BAND_W = [26, 16, 16, 12, 9, 6, 6, 0, 0, 0, 2]


def _band_tabel(n=400, seed=12345):
    rng = np.random.RandomState(seed)
    w = np.array(_BAND_W, float)
    w /= w.sum()
    tabel, vorige = [], None
    while len(tabel) < n:
        m = int(rng.choice(len(_BAND_MAT), p=w))
        if m == vorige:
            continue
        dik = int(rng.choice([1, 1, 2, 2, 2, 3, 3, 4, 5]))
        if _BAND_MAT[m] in ("rots_rood", "rots_diep"):
            dik = min(dik, 2)
        tabel += [m] * dik
        vorige = m
    return np.array(tabel[:n])


BANDTABEL = _band_tabel()


def strata_t(x, y, z):
    """Band coordinate: y bent by soft noise (layers that dip and swell) and a slight tilt to the east."""
    x = np.asarray(x, float)
    z = np.asarray(z, float)
    return (np.asarray(y, float) + 3.4 * SK.vnoise(x, z, cell=46, seed=71, oct=2) + 1.2 * SK.vnoise(x, z, cell=13, seed=72, oct=1)
            + 0.035 * (x - 3400) - 0.02 * (z + 720))


def strata_ids(W, x, y, z):
    """Block ids (array) of the rock strata at x, y, z (arrays of the same shape)."""
    if not hasattr(W, "_strata_ids"):
        W._strata_ids = np.array([W.id(b(k)) for k in _BAND_MAT], np.uint16)
    t = strata_t(x, y, z)
    d = SK.hash3(x, y, z, 77) < 0.10                    # a little dithering at the band seams
    t = t + np.where(d, 0.8, 0.0)
    i = np.floor(t).astype(int) % len(BANDTABEL)
    return W._strata_ids[BANDTABEL[i]]


def strata(x, y, z):
    """Block key of the strata at one block (for filling rock)."""
    t = float(strata_t(np.array([x + 0.5]), np.array([y]), np.array([z + 0.5]))[0])
    if SK.hash3(x, y, z, 77) < 0.10:
        t += 0.8
    return _BAND_MAT[BANDTABEL[int(math.floor(t)) % len(BANDTABEL)]]


# ============================================================================================ face frame
class Vlak:
    """A face plane. kant = outward normal (dx, dz); c = plane coordinate of the first rock cell (z for a z-plane,
    x for an x-plane). u = the other horizontal coordinate. d = depth into the rock."""

    def __init__(self, W, kant, c):
        self.W, self.kant, self.c = W, kant, int(c)
        self.dx, self.dz = kant
        self.out = FACE[kant]                 # stairs facing outward (rising towards the canyon)
        self.inn = FACE[(-self.dx, -self.dz)]  # rising into the rock
        if self.dx == 0:
            self.up, self.um = "east", "west"
        else:
            self.up, self.um = "south", "north"

    def pos(self, u, d, y):
        if self.dx == 0:
            return u, y, self.c - self.dz * d
        return self.c - self.dx * d, y, u

    def P(self, u, d, y, k, keep=False):
        x, y, z = self.pos(u, d, y)
        zet(self.W, x, y, z, k, keep=keep)

    def G(self, u, d, y):
        x, y, z = self.pos(u, d, y)
        return self.W.wget(int(math.floor(x)), int(math.floor(y)), int(math.floor(z)))

    def doos(self, u1, u2, d1, d2, y1, y2, k):
        for u in range(min(u1, u2), max(u1, u2) + 1):
            for d in range(min(d1, d2), max(d1, d2) + 1):
                for y in range(min(y1, y2), max(y1, y2) + 1):
                    self.P(u, d, y, k)

    def lucht(self, u1, u2, d1, d2, y1, y2):
        self.doos(u1, u2, d1, d2, y1, y2, LUCHT)

    def rots(self, u1, u2, d1, d2, y1, y2):
        for u in range(min(u1, u2), max(u1, u2) + 1):
            for d in range(min(d1, d2), max(d1, d2) + 1):
                for y in range(min(y1, y2), max(y1, y2) + 1):
                    x, yy, z = self.pos(u, d, y)
                    zet(self.W, x, yy, z, strata(int(x), int(yy), int(z)))

    def wereld(self, u, d):
        x, _, z = self.pos(u, d, 0)
        return x, z


# ============================================================================================ small pieces
def guhbeeld(V, u, d, y, sokkel="marmer_glad"):
    """A small guh statue (3 high) on a plinth: plinth, round body, the carved guh face, two little golden ears."""
    V.P(u, d, y, sokkel)
    V.P(u, d, y + 1, "wit")
    V.P(u, d, y + 2, GEZICHT)
    V.P(u, d, y + 3, plaat("goud"))


def kolos(W, cx, y0, cz, kijk=(0, 1), schaal=1.0):
    """A colossal seated guh carved from rose sandstone: round body, big head with ears, eyes, blush, little paws."""
    s = schaal
    rb, hb = 4.2 * s, 5.0 * s
    for dx in range(-6, 7):
        for dz in range(-6, 7):
            for dy in range(0, int(hb * 2) + 1):
                if (dx / rb) ** 2 + (dz / rb) ** 2 + ((dy - hb) / hb) ** 2 <= 1:
                    zet(W, cx + dx, y0 + dy, cz + dz, "zs")
    yh = y0 + int(hb * 2) + int(2.6 * s)
    rh = 4.0 * s
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            for dy in range(-5, 6):
                if (dx / rh) ** 2 + (dz / (rh * 0.9)) ** 2 + (dy / (rh * 0.92)) ** 2 <= 1:
                    zet(W, cx + dx, yh + dy, cz + dz, "zs")
    kx, kz = kijk
    zx, zz = (1, 0) if kx == 0 else (0, 1)          # sideways along the face
    for sgn in (-1, 1):                                # ears
        ex, ez = cx + zx * sgn * 3, cz + zz * sgn * 3
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if dx * dx + dy * dy <= 4:
                    zet(W, ex + zx * dx, yh + 4 + dy, ez + zz * dx, "rots_roze" if dx * dx + dy * dy <= 1 else "zs")
    fx, fz = cx + kx * int(rh * 0.9), cz + kz * int(rh * 0.9)
    for sgn in (-1, 1):                                # eyes + blush
        zet(W, fx + zx * sgn * 2, yh, fz + zz * sgn * 2, "minecraft:black_concrete")
        zet(W, fx + zx * sgn * 2, yh + 1, fz + zz * sgn * 2, "minecraft:black_concrete")
        zet(W, fx + zx * sgn * 3 - kx, yh - 1, fz + zz * sgn * 3 - kz, "rots_rood")
    zet(W, fx, yh - 2, fz, "rots_rood")                 # little mouth
    for sgn in (-1, 1):                                # paws
        px, pz = cx + kx * int(rb) + zx * sgn * 2, cz + kz * int(rb) + zz * sgn * 2
        for dy in range(0, 2):
            zet(W, px, y0 + dy, pz, "zs")
            zet(W, px + kx, y0, pz + kz, "zs")


def hanglampring(W, cx, y_plafond, cz, r=2.5, n=8, lengte=2, midden=True):
    """A ROUND hanging lamp cluster: a ring of brass lanterns on chains (no wooden grid), with a centre lamp lower."""
    for k in range(n):
        a = k * 2 * math.pi / n
        x, z = cx + math.cos(a) * r, cz + math.sin(a) * r
        for dy in range(1, lengte + 1):
            zet(W, x, y_plafond - dy, z, KETTING)
        zet(W, x, y_plafond - lengte - 1, z, mlamp(True))
    if midden:
        for dy in range(1, lengte + 3):
            zet(W, cx, y_plafond - dy, cz, KETTING)
        zet(W, cx, y_plafond - lengte - 3, cz, mlamp(True))


def hanglamp(W, x, y_plafond, z, lengte=1):
    for dy in range(1, lengte + 1):
        zet(W, x, y_plafond - dy, z, KETTING)
    zet(W, x, y_plafond - lengte - 1, z, mlamp(True))


def deur(V, u, d, y, hout="minecraft:cherry_door"):
    f = V.out
    V.P(u, d, y, f"{hout}[facing={f},half=lower,hinge=left,open=false,powered=false]")
    V.P(u, d, y + 1, f"{hout}[facing={f},half=upper,hinge=left,open=false,powered=false]")


def tegelvloer(V, u1, u2, d1, d2, y, seed=0, soort=None):
    rng = rng_van(u1, d1, y, seed)
    soort = soort or ["mozaiek", "dambord", "planken", "parel"][rng.randint(4)]
    for u in range(u1, u2 + 1):
        for d in range(d1, d2 + 1):
            if soort == "mozaiek":
                k = ["mozaiek_ster", "mozaiek_bloem", "mozaiek_guh"][(u + d) % 3] if (u + d) % 2 == 0 else "zs"
            elif soort == "dambord":
                k = "vloer_dambord"
            elif soort == "parel":
                k = "tegel_parel" if (u * 3 + d) % 4 else "mozaiek_ster"
            else:
                k = "palm_planken"
            V.P(u, d, y, k)


def kussen(kleur):
    return f"guhs:{kleur}_kussen"


# ============================================================================================ facade parts
def kroonlijst(V, u1, u2, y, d=-1, k="zs"):
    """A projecting cornice: upside-down stairs pointing out over the face."""
    for u in range(u1, u2 + 1):
        V.P(u, d, y, trap(k, V.inn, half="top"))


def zuil(V, u, d, y1, y2, schacht="zuil", kop="zuilkop", basis="marmer_glad"):
    V.P(u, d, y1, basis)
    for y in range(y1 + 1, y2):
        V.P(u, d, y, schacht)
    V.P(u, d, y2, kop)


def fronton(V, um, hw, y, d=0, vul="rots_roze", rand="zs", motief=True):
    """Triangular pediment as stepped relief: outline of stairs, tympanum recessed by one, a carved guh in the middle."""
    for i in range(hw + 1):
        yy = y + i // 2
        a, c_ = um - hw + i, um + hw - i
        if a > c_:
            break
        if i % 2 == 0:
            V.P(a, d, yy, trap(rand, V.up))
            V.P(c_, d, yy, trap(rand, V.um))
        else:
            V.P(a, d, yy, rand)
            V.P(c_, d, yy, rand)
        for u in range(a + 1, c_):
            V.P(u, d, yy, LUCHT)
            V.P(u, d + 1, yy, vul)
    if motief:
        V.P(um, d + 1, y + 1, "zs_beitel")
    V.P(um, d, y + hw // 2 + 1, "goudblok")
    V.P(um, d, y + hw // 2 + 2, plaat("goud"))
    for u in (um - hw, um + hw):
        V.P(u, d, y, "goud")
        V.P(u, d, y + 1, plaat("goud"))


def kantelkroon(V, u1, u2, y, d=0):
    """Nabataean crow-steps cut out of the rock: steps 1-2-3-2-1 per 5, air above them against a recessed back."""
    for u in range(u1, u2 + 1):
        h = (1, 2, 3, 2, 1, 0)[(u - u1) % 6]
        for dy in range(4):
            V.P(u, d, y + dy, "zs" if dy < h else LUCHT)
        V.P(u, d + 1, y + 3, LUCHT if h < 3 else "zs")


def nis_beeld(V, u, d, y, h=4):
    """A round-headed niche (1 wide) with a guh statue."""
    for yy in range(y, y + h):
        V.P(u, d, yy, LUCHT)
    V.P(u, d, y + h, trap("zs", V.out, half="top"))
    guhbeeld(V, u, d + 0, y - 0) if False else None
    V.P(u, d, y, "marmer_glad")
    V.P(u, d, y + 1, "wit")
    V.P(u, d, y + 2, GEZICHT)
    V.P(u, d - 1, y - 1, plaat("goud", "top"))


def raam(V, u, d, y, soort="mash"):
    """A window 1 wide, 2 high, glass/lattice at the back (d+1), arched head."""
    V.P(u, d, y, LUCHT)
    V.P(u, d, y + 1, LUCHT)
    V.P(u, d + 1, y, "mashrabiya" if soort == "mash" else "glas")
    V.P(u, d + 1, y + 1, "mashrabiya" if soort == "mash" else "glas")
    V.P(u, d, y + 2, trap("zs", V.out, half="top"))
    V.P(u, d - 1, y - 1, trap("zs", V.inn, half="top"))   # sill


def bloembak(V, u, d, y, rng):
    V.P(u, d, y, plaat("palm_planken", "top"))
    V.P(u, d, y + 1, ZB.BLOEMEN[rng.randint(len(ZB.BLOEMEN))].replace("[half=lower]", ""))


def erosie(V, u, d, y, r, seed=0):
    """A wind-eroded pocket: a flattened half-ball carved r deep into the face (never below y)."""
    rng = rng_van(u, y, seed)
    for du in range(-int(r) - 1, int(r) + 2):
        for dy in range(0, int(r * 1.3) + 2):
            for dd in range(0, int(r) + 1):
                if (du / r) ** 2 + ((dy - r * 0.6) / (r * 0.9)) ** 2 + (dd / (r * 0.8)) ** 2 <= 1:
                    V.P(u + du, d + dd, y + dy, LUCHT)
    if rng.rand() < 0.5:
        V.P(u, d, y, ZB.PLANTEN[rng.randint(len(ZB.PLANTEN))])


# ============================================================================================ interiors
def kamer(V, u1, u2, d1, d2, y, h=5, vloer=None, seed=0, lampen=True):
    """A room carved into the rock: air, a real floor at y-1, a carved cornice, round hanging lamps."""
    rng = rng_van(u1, d1, y, seed)
    V.lucht(u1, u2, d1, d2, y, y + h - 1)
    tegelvloer(V, u1, u2, d1, d2, y - 1, seed=seed, soort=vloer)
    # ceiling: a stepped cornice all around (muqarnas hint)
    for u in range(u1, u2 + 1):
        for d in (d1, d2):
            V.P(u, d, y + h - 1, trap("zs", V.inn if d == d2 else V.out, half="top"))
    for d in range(d1 + 1, d2):
        V.P(u1, d, y + h - 1, trap("zs", V.um, half="top"))
        V.P(u2, d, y + h - 1, trap("zs", V.up, half="top"))
    if lampen:
        um, dm = (u1 + u2) / 2.0, (d1 + d2) / 2.0
        x, _, z = V.pos(um, dm, 0)
        if u2 - u1 >= 6 and d2 - d1 >= 6 and h >= 6:
            hanglampring(V.W, x, y + h, z, r=2.2, n=8, lengte=1)
        else:
            hanglamp(V.W, x, y + h, z, lengte=max(0, h - 4))
    return rng


def meubels(V, u1, u2, d1, d2, y, soort, rng):
    """Furniture along the back and side walls of a room (the middle stays free)."""
    if soort == "woning":
        V.P(u1, d2, y, "minecraft:barrel[facing=up,open=false]")
        V.P(u1, d2, y + 1, "minecraft:flower_pot")
        V.P(u2, d2, y, "minecraft:chiseled_bookshelf[facing=" + V.out + ",slot_0_occupied=true,slot_1_occupied=false,"
                        "slot_2_occupied=true,slot_3_occupied=true,slot_4_occupied=false,slot_5_occupied=true]")
        V.P(u2 - 1, d2, y, "minecraft:cherry_stairs[facing=" + V.inn + ",half=bottom,shape=straight,waterlogged=false]")
        V.P(u1, d2 - 1, y, kussen("pink"))
        V.P(u1 + 1, d2, y, kussen("yellow"))
        for u in range(u1 + 1, u2):
            V.P(u, (d1 + d2) // 2, y - 1, TAPIJT["roze" if u % 2 else "turkoois"].replace("_carpet", "_wool"))
        V.P(u2, d1, y, ZB.PLANTEN[rng.randint(len(ZB.PLANTEN))])
        V.P((u1 + u2) // 2, d2, y + 2, "minecraft:painting") if False else None
    elif soort.startswith("winkel"):
        for u in range(u1, u2 + 1):
            V.P(u, d2, y, ZB.WAREN[rng.randint(len(ZB.WAREN))])
            V.P(u, d2, y + 1, ZB.WAREN[rng.randint(len(ZB.WAREN))] if rng.rand() < 0.6 else LUCHT)
        dt = max(d1 + 1, (d1 + d2) // 2)
        for u in range(u1 + 1, u2):
            V.P(u, dt, y, "palm_planken")
            V.P(u, dt, y + 1, plaat("palm_planken") if rng.rand() < 0.5 else ZB.WAREN[rng.randint(len(ZB.WAREN))]
                if rng.rand() < 0.3 else plaat("palm_planken"))
        for d in range(d1, d2 + 1):
            if rng.rand() < 0.4:
                V.P(u1, d, y, "minecraft:decorated_pot[facing=north,waterlogged=false,cracked=false]")
    elif soort == "schrijn":
        guhbeeld(V, (u1 + u2) // 2, d2, y)
        for u in (u1, u2):
            V.P(u, d2, y, "minecraft:pink_candle[candles=3,lit=true,waterlogged=false]")


# ============================================================================================ facade types
# Every facade gets: V (Vlak of the face), u1..u2 (inclusive, the bay minus its edge pilasters), y = the stand level of
# the ledge in front, h = rows available (facade + rock above it until the lip). Returns an optional house record
# (rol, door (x, y, z), inside (x, y, z)) when it has an enterable interior.

def _binnen(V, u, d, y):
    x, yy, z = V.pos(u, d, y)
    return (int(math.floor(x)), int(yy), int(math.floor(z)))


def gevel_tempel(V, u1, u2, y, h, seed=0, kamer_=True):
    """Tomb-temple facade: podium, a portico of 4-6 columns in a 3-deep recess, gold architrave, a frieze with carved
    guhs, a projecting cornice, a pediment with a guh in the tympanum, statue niches between the outer columns and a deep
    doorway into a small shrine chamber."""
    rng = rng_van(u1, y, seed)
    w = u2 - u1 + 1
    um = (u1 + u2) // 2
    hp = min(7, h - 5)                            # portico height
    V.lucht(u1, u2, -0, 2, y, y + hp - 1)
    for u in range(u1, u2 + 1):                   # podium step + recess floor
        V.P(u, 0, y - 1, "marmer_glad")
        V.P(u, 1, y - 1, "marmer_glad")
        V.P(u, 2, y - 1, "zs")
    n = 6 if w >= 13 else 4
    us = [int(round(u1 + 1 + i * (w - 3) / (n - 1))) for i in range(n)]
    for u in us:
        zuil(V, u, 1, y, y + hp - 1)
    for u in range(u1, u2 + 1):                   # architrave, frieze, cornice
        V.P(u, 0, y + hp, "goud")
        V.P(u, 0, y + hp + 1, "zs_beitel" if (u - um) % 3 == 0 else "marmer_glad")
        V.P(u, 1, y + hp, "zs")
        V.P(u, 1, y + hp + 1, "zs")
    kroonlijst(V, u1 - 1, u2 + 1, y + hp + 2, d=-1)
    for u in range(u1 - 1, u2 + 2):
        V.P(u, 0, y + hp + 2, "zs")
    fronton(V, um, min((w - 1) // 2, 2 * max(1, h - hp - 4)), y + hp + 3, d=0)
    # statue niches in the back wall between the outer columns
    for u in (us[0] + 1, us[-1] - 1):
        if u not in us:
            nis_beeld(V, u, 3, y, h=3)
    # deep doorway -> shrine chamber
    V.lucht(um - 1, um + 1, 3, 7, y, y + 4)
    V.lucht(um, um, 3, 7, y + 5, y + 5)
    for yy in range(y, y + 5):
        V.P(um - 2, 3, yy, "marmer_glad" if yy < y + 4 else "goud")
        V.P(um + 2, 3, yy, "marmer_glad" if yy < y + 4 else "goud")
    V.P(um, 3, y + 6, "zs_beitel")
    for d in range(3, 8):
        for u in range(um - 1, um + 2):
            V.P(u, d, y - 1, "marmer_glad" if d % 2 else "mozaiek_ster")
    rec = None
    if kamer_:
        kamer(V, um - 2, um + 2, 8, 12, y, h=6, vloer="parel", seed=seed)
        meubels(V, um - 2, um + 2, 8, 12, y, "schrijn", rng)
        rec = ("schrijn", _binnen(V, um, 3, y), _binnen(V, um, 10, y))
    hanglamp(V.W, *V.pos(um, 1, 0)[0:1], y + hp, V.pos(um, 1, 0)[2], lengte=1) if False else None
    for u in (us[1], us[-2]):
        x, _, z = V.pos(u + (1 if u < um else -1), 1, 0)
        hanglamp(V.W, x, y + hp, z, lengte=1)
    return rec


def gevel_tholos(V, u1, u2, y, h, seed=0):
    """Two-storey Khazneh-like facade: a lower portico, an entablature, and above it a tholos (round temple with an
    ear-topped dome in relief) set in a deep recess between two half pediments."""
    rng = rng_van(u1, y, seed + 1)
    w = u2 - u1 + 1
    um = (u1 + u2) // 2
    h1 = 5
    V.lucht(u1, u2, 0, 2, y, y + h1 - 1)
    for u in range(u1, u2 + 1):
        V.P(u, 0, y - 1, "marmer_glad")
        V.P(u, 1, y - 1, "mozaiek_bloem" if u % 2 else "marmer_glad")
        V.P(u, 2, y - 1, "marmer_glad")
    us = [u1 + 1, um - 2, um + 2, u2 - 1]
    for u in us:
        zuil(V, u, 0, y, y + h1 - 1)
    for u in range(u1, u2 + 1):
        V.P(u, 0, y + h1, "goud")
        V.P(u, 0, y + h1 + 1, "zs_beitel" if u in us else "marmer_glad")
    kroonlijst(V, u1, u2, y + h1 + 2, d=-1)
    V.lucht(um - 1, um + 1, 3, 6, y, y + 3)        # door
    for u in range(um - 1, um + 2):
        V.P(u, 3, y + 4, "goud")
    V.P(um, 3, y + 5, "zs_beitel")
    # upper storey: recess 4 deep, tholos in the middle, half pediments on pavilions at the sides
    yb = y + h1 + 3
    top = y + h - 5
    if top - yb < 4:
        return None
    V.lucht(u1 + 3, u2 - 3, 0, 3, yb, top)
    for u in range(u1 + 3, u2 - 2):
        V.P(u, 0, yb - 1, "marmer_glad")
    for du in (-2, -1, 0, 1, 2):                    # tholos columns (front half), drum, dome relief
        u = um + du
        dd = 2 - int(round(math.sqrt(max(0, 4 - du * du)) * 0.9))
        if du in (-2, 0, 2):
            zuil(V, u, dd + 1, yb, top - 3)
        V.P(u, dd + 1, top - 2, "goud")
    for du in range(-2, 3):
        V.P(um + du, 3, top - 1, "rots_marmer")
    V.P(um - 1, 2, top, "rots_marmer")
    V.P(um, 2, top, "rots_marmer")
    V.P(um + 1, 2, top, "rots_marmer")
    V.P(um, 1, top, "goudblok")
    for sgn in (-1, 1):                             # guh ears on the dome
        V.P(um + sgn, 1, top, plaat("koepel_roze", "bottom"))
    for side in (-1, 1):                            # pavilions with half pediments
        ua, ub = (u1, u1 + 2) if side < 0 else (u2 - 2, u2)
        for u in (ua, ub):
            zuil(V, u, 0, yb, top - 2)
        V.lucht(ua + 1, ua + 1, 0, 1, yb, top - 3)
        nis_beeld(V, ua + 1, 1, yb, h=3)
        for u in range(ua, ub + 1):
            V.P(u, 0, top - 1, "goud")
        inner = ub if side < 0 else ua
        outer = ua if side < 0 else ub
        V.P(outer, 0, top, "zs")
        V.P(inner, 0, top, trap("zs", V.up if side > 0 else V.um))
    return ("schrijn", _binnen(V, um, 3, y), _binnen(V, um, 5, y))


def gevel_woning(V, u1, u2, y, h, seed=0, rol="woning"):
    """Rock house: a plaster front set 2 deep into the rock between rock pilasters, an arched door, lattice windows with
    flower boxes, an oriel with mashrabiya on corbels, a stepped crown cut from the rock; behind it a real room."""
    rng = rng_van(u1, y, seed + 2)
    um = (u1 + u2) // 2
    gevel = ["muur_roze", "muur_perzik", "muur_vanille", "muur_boter", "muur_lavendel", "rots_marmer"][rng.randint(6)]
    hh = min(10, h - 5)
    V.lucht(u1, u2, 0, 1, y, y + hh - 1)
    V.doos(u1, u2, 2, 2, y, y + hh - 1, gevel)
    for u in range(u1, u2 + 1):
        V.P(u, 0, y - 1, "zs")
        V.P(u, 1, y - 1, "zs")
        V.P(u, 1, y + hh - 1, trap("zs", V.out, half="top"))
        V.P(u, 2, y + hh - 2, "goud" if u % 2 else "tegel_turkoois")
    # door (cherry) under a pointed arch of tiles
    V.P(um, 2, y, LUCHT)
    V.P(um, 2, y + 1, LUCHT)
    deur(V, um, 2, y)
    V.P(um - 1, 2, y + 2, trap("mozaiek_ster", V.up, half="top") if False else "mozaiek_ster")
    V.P(um + 1, 2, y + 2, "mozaiek_ster")
    V.P(um, 2, y + 2, "zs_beitel")
    V.P(um - 1, 1, y + 2, mlamp(False)) if False else None
    x, _, z = V.pos(um, 1, 0)
    hanglamp(V.W, x, y + hh - 1, z, lengte=max(0, hh - 6))
    # windows with flower boxes
    for u in (um - 2, um + 2):
        if u1 < u < u2:
            V.P(u, 2, y + 1, "mashrabiya")
            V.P(u, 2, y + 2, "mashrabiya")
            V.P(u, 1, y, plaat("palm_planken", "top")) if False else None
    # oriel (erker) on corbels on the upper floor
    if hh >= 9:
        yo = y + 5
        for u in range(um - 1, um + 2):
            V.P(u, 1, yo - 1, trap("palm_planken", V.inn, half="top"))
            V.P(u, 0, yo - 1, trap("palm_planken", V.inn, half="top"))
            for yy in range(yo, yo + 2):
                V.P(u, 0, yy, "mashrabiya")
            V.P(u, 0, yo + 2, plaat("koepel_tegel"))
            V.P(u, 1, yo + 2, plaat("koepel_tegel"))
        for u in (um - 2, um + 2):
            for yy in range(yo, yo + 2):
                V.P(u, 1, yy, "palm_planken")
                V.P(u, 2, yy, gevel)
        for u in (um - 3, um + 3):
            if u1 < u < u2:
                V.P(u, 2, yo, "glas")
                V.P(u, 2, yo + 1, "glas")
                bloembak(V, u, 1, yo - 1, rng)
    kantelkroon(V, u1, u2, y + hh, d=0)
    # terrace pots on the corners (path stays free)
    V.P(u1, -1, y, ZB.PLANTEN[rng.randint(len(ZB.PLANTEN))])
    V.P(u2, -1, y, ZB.PLANTEN[rng.randint(len(ZB.PLANTEN))])
    # the room behind
    ru1, ru2 = u1, u2
    kamer(V, ru1, ru2, 3, 9, y, h=5, seed=seed)
    meubels(V, ru1, ru2, 3, 9, y, rol, rng)
    return (rol, _binnen(V, um, 2, y), _binnen(V, um, 5, y))


def gevel_winkel(V, u1, u2, y, h, seed=0, rol="winkel"):
    """Shop front: a wide pointed opening, a silk awning, goods spilling out; inside a counter, shelves and lamps."""
    rng = rng_van(u1, y, seed + 3)
    um = (u1 + u2) // 2
    hb = min(3, (u2 - u1) // 2 - 1)
    V.lucht(u1, u2, 0, 0, y, y + 6)
    V.doos(u1, u2, 1, 1, y, y + 6, "rots_marmer")
    for u in range(um - hb, um + hb + 1):
        top = y + 4 + (1 if abs(u - um) < hb else 0) + (1 if u == um else 0)
        for yy in range(y, top):
            V.P(u, 1, yy, LUCHT)
        V.P(u, 1, top, "mozaiek_guh" if u == um else "tegel_turkoois")
    for u in range(u1, u2 + 1):
        V.P(u, 0, y - 1, "zs")
        V.P(u, 1, y - 1, "zs")
        V.P(u, 1, y + 7, trap("zs", V.out, half="top"))
        V.P(u, 0, y + 7, "goud")
    # silk awning out over the front
    for u in range(um - hb - 1, um + hb + 2):
        V.P(u, -1, y + 5, "luifel_zijde")
        V.P(u, 0, y + 6, "luifel_zijde") if False else None
    for u in (um - hb - 1, um + hb + 1):
        V.P(u, -1, y, "minecraft:barrel[facing=up,open=false]" if rng.rand() < 0.5 else
            "minecraft:decorated_pot[facing=north,waterlogged=false,cracked=false]")
    # upper floor windows + crown
    for u in range(u1 + 1, u2, 3):
        if u != um:
            raam(V, u, 1, y + 8, "mash" if u % 2 else "glas")
    if h - 5 >= 12:
        kantelkroon(V, u1, u2, y + 11, d=0)
    kamer(V, u1, u2, 2, 8, y, h=6, seed=seed)
    meubels(V, u1, u2, 2, 8, y, "winkel", rng)
    return (rol, _binnen(V, um, 1, y), _binnen(V, um, 3, y))


def gevel_galerij(V, u1, u2, y, h, seed=0):
    """Colonnaded gallery along the ledge: cut 4 deep, columns every 3 at the front carry the rock above (an overhang),
    benches and niches at the back, lanterns between the columns; above it rows of small rock-cut windows."""
    rng = rng_van(u1, y, seed + 4)
    hg = 5
    V.lucht(u1, u2, 0, 3, y, y + hg - 1)
    for u in range(u1, u2 + 1):
        for d in range(0, 4):
            V.P(u, d, y - 1, "vloer_dambord" if d == 0 else ("mozaiek_bloem" if (u + d) % 4 == 0 else "zs"))
        V.P(u, 0, y + hg, "goud")
        V.P(u, 0, y + hg + 1, "zs_beitel" if u % 4 == 0 else "zs")
    kroonlijst(V, u1, u2, y + hg + 2, d=-1)
    for u in range(u1, u2 + 1, 3):
        zuil(V, u, 0, y, y + hg - 1)
    for u in range(u1 + 1, u2, 3):
        x, _, z = V.pos(u + 0.5, 1, 0)
        hanglamp(V.W, x, y + hg, z, lengte=0)
    for u in range(u1 + 1, u2):
        if u % 3 == 1:
            nis_beeld(V, u, 4, y, h=3) if rng.rand() < 0.4 else V.P(u, 3, y, trap("zs", V.out))
        elif u % 3 == 2:
            V.P(u, 3, y, trap("palm_planken", V.out))
    for u in range(u1 + 1, u2, 2):
        if y + hg + 4 + 2 < y + h - 4:
            raam(V, u, 0, y + hg + 4, "mash" if (u // 2) % 2 else "glas")
    return None


def gevel_schrijn(V, u1, u2, y, h, seed=0):
    """Shrine niche in rough rock: an arched niche with a statue, candles and flowers, eroded pockets, a planter."""
    rng = rng_van(u1, y, seed + 5)
    um = (u1 + u2) // 2
    for u in range(um - 1, um + 2):
        for yy in range(y, y + 5):
            V.P(u, 0, yy, LUCHT)
            V.P(u, 1, yy, LUCHT)
    V.P(um, 0, y + 5, LUCHT)
    V.P(um, 1, y + 5, LUCHT)
    V.P(um - 1, 0, y + 5, trap("zs", V.up, half="top"))
    V.P(um + 1, 0, y + 5, trap("zs", V.um, half="top"))
    V.P(um, 0, y + 6, "zs_beitel")
    guhbeeld(V, um, 2, y)
    V.P(um, 2, y + 4, trap("zs", V.out, half="top"))
    V.P(um - 1, 1, y, "minecraft:pink_candle[candles=4,lit=true,waterlogged=false]")
    V.P(um + 1, 1, y, ZB.PLANTEN[rng.randint(len(ZB.PLANTEN))])
    for u in range(um - 1, um + 2):
        V.P(u, 0, y - 1, "marmer_glad")
        V.P(u, 1, y - 1, "mozaiek_ster")
    for k in range(2):
        uu = u1 + 1 + k * (u2 - u1 - 2)
        if abs(uu - um) > 2:
            erosie(V, uu, 0, y + 3 + rng.randint(4), 1.5 + rng.rand(), seed=seed + k)
    return None


TYPES = {"tempel": gevel_tempel, "tholos": gevel_tholos, "woning": gevel_woning, "winkel": gevel_winkel,
         "galerij": gevel_galerij, "schrijn": gevel_schrijn}
