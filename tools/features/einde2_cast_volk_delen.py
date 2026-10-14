"""
einde2, slice cast_volk - the wardrobe: parts that several characters share (CONTRACT_E2 11.5, layer 1: "the many are cheap").
Every function dresses a `Pop` (einde2_cast_volk_kern): painted parts (robes, hair, the air nomads' arrow, the Kyoshi paint)
go straight onto the base cubes; things with a shape (hats, knots, beards, glasses, fans, spears) return CUBES in the
sitting guh's head or body coordinates, for the caller to hang on a bone of its own (`<basis>_<ding>` or `mix_<i>_<ding>`).
"""
import numpy as np

from features.einde2_cast_volk_kern import rand, strepen, zoom, _donker

# the four nations (and what everybody has)
ORANJE, GEEL, PIJL = (226, 112, 40), (246, 196, 70), (66, 146, 226)
W_BLAUW, W_LICHT, W_DONKER, BONT = (62, 104, 170), (124, 172, 222), (40, 62, 112), (244, 244, 248)
A_GROEN, A_DONKER, A_GEEL, A_BRUIN, A_BEIGE = (98, 150, 76), (52, 98, 60), (228, 198, 92), (150, 112, 70), (214, 190, 140)
V_ROOD, V_DONKER, V_ZWART = (178, 44, 44), (106, 30, 38), (50, 40, 46)
GOUD, WIT, GRIJS, ZWART, HOUT, STRO, LEER = (240, 196, 70), (248, 248, 250), (172, 172, 180), (42, 38, 44), (150, 104, 62), (226, 200, 120), (128, 84, 52)
HAAR_ZWART, HAAR_BRUIN, HAAR_GRIJS, HAAR_WIT = (46, 40, 44), (96, 62, 40), (176, 176, 184), (240, 240, 244)
# fur: (hue, sat, val) for Pop
PEACH, TAN, BRUIN, OUD = (0.06, 1.25, 1.0), (0.075, 1.9, 0.93), (0.07, 2.6, 0.8), (0.08, 0.75, 0.97)

ARMEN = ("arm_left", "arm_right")


# --- painted ---------------------------------------------------------------------------------------------------------------
def gewaad(P, kleur, rand_k=None, kraag=None, riem=None, zoom_k=None, mouw=None, hals=None, kort=None):
    """A robe, tunic or parka over the body: the body and the sleeves in `kleur` (the paws stay fur), with an optional cuff
    (rand_k), a V collar (kraag; `hals` = what shows inside the V), a belt (riem), a hem (zoom_k). kort = the colour of the
    part under the belt when that differs (trousers, a skirt)."""
    if P.lijf == "zit":
        P.verf("body", kleur)
        P.verf(ARMEN, mouw or kleur, lambda x, y, z, f: f != "north")
        if kort:
            P.verf("body", kort, lambda x, y, z, f: y < 4.9)
        if rand_k:
            P.verf(ARMEN, rand_k, lambda x, y, z, f: (f != "north") & (z < -5.5))
        if hals:
            P.verf("body", hals, lambda x, y, z, f: (f == "north") & (y > 8.4) & (np.abs(x) < (y - 8.4) * 0.75))
        if kraag:
            P.verf("body", kraag, lambda x, y, z, f: (f == "north") & (y > 8.0) & (np.abs(np.abs(x) - (y - 8.4) * 0.75) < 0.62))
            P.verf("body", kraag, lambda x, y, z, f: (f != "north") & (y > 12.0))
        if riem:
            P.verf("body", riem, lambda x, y, z, f: (y > 4.7) & (y < 6.0))
        if zoom_k:
            P.verf("body", zoom_k, lambda x, y, z, f: y < 2.2)
    else:
        P.verf("body", kleur)
        if kort:
            P.verf("body", kort, lambda x, y, z, f: z > 8.5)
        if kraag:
            P.verf("body", kraag, lambda x, y, z, f: z < 0.6)
        if riem:
            P.verf("body", riem, lambda x, y, z, f: (z > 4.6) & (z < 6.0))
        if zoom_k:
            P.verf("body", zoom_k, lambda x, y, z, f: y < 1.9)


def haar(P, kleur, voor=24.6, zij=22.6, achter=13.0, vanaf_z=None, tot_z=None):
    """Painted hair: the crown (from vanaf_z to the back: a shaved front; up to tot_z: only a fringe), a hairline on the
    front above `voor`, the sides above `zij` and behind the ears down to `achter`, the back of the head down to `achter`."""
    def waar(x, y, z, f):
        binnen = np.ones(x.shape, bool)
        if vanaf_z is not None:
            binnen &= z > vanaf_z
        if tot_z is not None:
            binnen &= z < tot_z
        if f == "up":
            return binnen
        if f == "north":
            return (y > voor) & binnen
        if f == "south":
            return (y > achter) & binnen
        if f in ("east", "west"):
            return ((y > zij) | ((z > 1.5) & (y > achter))) & binnen
        return np.zeros(x.shape, bool)
    P.verf("head", kleur, waar, var=8)


def pijl(P, kleur=PIJL, scheef=0.0):
    """The air nomads' arrow: over the crown from the back, down the forehead to a point between the eyes; and on the paws."""
    P.verf("head", kleur, lambda x, y, z, f: (f in ("up", "south")) & (np.abs(x - scheef * 0.1 * z) < 1.5) & ((f == "up") | (y > 20)))
    P.verf("head", kleur, lambda x, y, z, f: (f == "north") & (((y >= 22.4) & (np.abs(x - scheef) < 1.5))
                                                              | ((y < 22.4) & (y > 19.6) & (np.abs(x - scheef) < (y - 19.6) * 0.95))))
    if P.lijf == "zit":
        P.verf(ARMEN, kleur, lambda x, y, z, f: (f == "up") & (np.abs(np.abs(x) - 3.0) < 0.7))


def schmink(P, rood=(204, 40, 52), wit=(250, 248, 244)):
    """The Kyoshi paint: a white face, red above and beside the eyes, red lips."""
    ox, oy, r = P.oog
    P.verf("head", wit, lambda x, y, z, f: (f == "north") | ((f in ("east", "west")) & (z < -2.5)) | ((f == "down") & (z < -2.5)))
    P.verf("head", rood, lambda x, y, z, f: (f == "north") & (np.hypot((np.abs(x) - ox) * 0.8, y - oy) < r + 1.5) & (y > oy - 0.3)
           & (np.abs(x) > 1.0))
    P.verf("head", rood, lambda x, y, z, f: (f == "north") & (np.abs(x) > ox + r - 0.4) & (np.abs(x) < 7.2) & (np.abs(y - oy - 0.4 - (np.abs(x) - ox - r) * 0.5) < 0.75))
    P.verf("head", rood, lambda x, y, z, f: (f == "north") & (np.abs(x) < 1.3) & (y > 15.2) & (y < 16.3))


def wenkbrauw(P, kleur, binnen=-0.9, buiten=0.6, dikte=0.55, hoog=0.0):
    """Painted brows over the eyes: `binnen` / `buiten` = how far the inner and the outer end stand above the eye's top
    (a low inner end = stern, a low outer end = kind or sad)."""
    ox, oy, r = P.oog
    def waar(x, y, z, f):
        ax = np.abs(x)
        t = np.clip((ax - (ox - r)) / (2 * r), 0, 1)
        lijn = oy + r + hoog + binnen + (buiten - binnen) * t
        return (f == "north") & (ax > ox - r - 0.2) & (ax < ox + r + 0.4) & (np.abs(y - lijn) < dikte)
    P.verf("head", kleur, waar, over=True)


def blos(P, kleur=(255, 104, 128), r=1.6):
    ox, oy, _ = P.oog
    P.verf("head", kleur, lambda x, y, z, f: (f == "north") & (np.hypot(np.abs(x) - 5.4, y - 17.2) < r))


def mond(P, kleur, breed=2.6, hoek=0.0, y0=15.6, dikte=0.4):
    """A painted mouth under the snout: a line that curls up at the ends (hoek tilts it: a crooked grin)."""
    P.verf("head", kleur, lambda x, y, z, f: (f == "north") & (np.abs(x) < breed)
           & (np.abs(y - (y0 + 0.9 * (x / breed) ** 2 + hoek * x)) < dikte), over=True)


def buikvlek(P, kleur, breed=3.0, y0=3.0, y1=10.5):
    """A bare chest: a lighter patch on the front of the body."""
    P.verf("body", kleur, lambda x, y, z, f: (f == "north") & (y > y0) & (y < y1) & (np.abs(x) < breed * np.sqrt(np.clip(1 - ((y - (y0 + y1) / 2) / ((y1 - y0) / 2)) ** 2, 0, 1)) + 0.4))


def spikkels(P, kleur, kans=0.05, botten=("body", "head", "arm_left", "arm_right", "foot_left", "foot_right")):
    """Sand, flour or sawdust on the fur."""
    P.verf([b for b in botten if b in [x["name"] for x in P.geo["bones"]]], kleur,
           lambda x, y, z, f: P.rng.random(x.shape) < kans)


# --- cubes on the head (sitting guh's head coordinates) --------------------------------------------------------------------
def kap(P, kleur, patroon=None):
    """A cap of hair (or cloth) over the crown, the back of the head and behind the ears."""
    k = P.k
    return [k([-7.25, 24.2, -7.2], [14.5, 0.7, 13.5], kleur, patroon=patroon), k([-6.75, 24.8, -6.25], [13.5, 0.6, 11.5], kleur, patroon=patroon),
            k([-5.75, 25.3, -5.25], [11.5, 1.0, 9.5], kleur, patroon=patroon), k([-7.2, 14.5, 5.8], [14.4, 10.2, 0.6], kleur, patroon=patroon),
            k([7.0, 16.5, 1.2], [1.3, 8.2, 5.0], kleur, patroon=patroon), k([-8.3, 16.5, 1.2], [1.3, 8.2, 5.0], kleur, patroon=patroon)]


def knot(P, kleur, band=None, y=26.0, z=-0.5, b=3.0, hh=2.6):
    k = P.k
    out = [k([-b / 2, y, z - b / 2], [b, hh, b], kleur)]
    if band:
        out.append(k([-b / 2 - 0.2, y, z - b / 2 - 0.2], [b + 0.4, 0.8, b + 0.4], band))
    return out


def bolknot(P, kleur, y=24.0, z=5.0, b=5.0):
    """A bun at the back of the head."""
    k = P.k
    return [k([-b / 2, y - b / 2, z], [b, b, b * 0.7], kleur), k([-b / 2 + 0.6, y - b / 2 + 0.6, z + b * 0.7], [b - 1.2, b - 1.2, 0.6], kleur)]


def lang_haar(P, kleur, tot=11.0, breed=11.0):
    """Long hair down the back."""
    k = P.k
    return [k([-breed / 2, tot, 5.9], [breed, 24.4 - tot, 1.3], kleur), k([-breed / 2 + 1, tot - 1.5, 6.0], [breed - 2, 1.5, 1.0], kleur)]


def vlecht(P, kleur, tot=6.0, bol=None):
    k = P.k
    out = [k([-0.8, tot, 6.0], [1.6, 21 - tot, 1.4], kleur)]
    if bol:
        out.append(k([-1.0, tot - 0.2, 5.8], [2.0, 1.2, 1.8], bol))
    return out


def baard(P, kleur, lengte=4.0, breed=7.0):
    """A beard under the chin, narrowing down."""
    k = P.k
    out = [k([-breed / 2, 12.2, -7.5], [breed, 2.2, 1.3], kleur)]
    stap, y, b = 0, 12.2, breed
    while lengte > 0.2:
        d = min(2.0, lengte)
        b = max(1.6, b - 1.8)
        y -= d
        out.append(k([-b / 2, y, -7.6 - 0.05 * stap], [b, d, 1.1], kleur))
        lengte -= d
        stap += 1
    return out


def sik(P, kleur, lengte=2.2, breed=1.6):
    return [P.k([-breed / 2, 13.6 - lengte, -7.7], [breed, lengte, 0.8], kleur)]


def snor(P, kleur, hang=0.0, breed=5.4, dik=0.7):
    """A moustache beside and under the snout: two tufts that start at the nose; hang > 0: their ends droop that far (the
    long thin kind of a master)."""
    k = P.k
    out = []
    for sx in (1, -1):
        x0 = 0.5 if sx > 0 else -breed / 2
        out.append(k([x0, 15.5, -7.95], [breed / 2 - 0.5, dik + 0.3, 0.8], kleur))
        if hang:
            xe = breed / 2 - 0.2 if sx > 0 else -breed / 2 - 0.6
            out += [k([xe, 16.3 - min(hang, 2.2), -8.0], [0.8, min(hang, 2.2), 0.8], kleur)]
            if hang > 2.2:
                out += [k([xe + 0.1 * sx, 16.3 - hang, -8.0], [0.6, hang - 2.2, 0.7], kleur)]
    return out


def bril(P, kleur, laag=0.0, smal=False, alleen=None, z=-7.85, dik=0.4, klein=0.0):
    """Glasses: a thin frame round each eye with the corners left open, so it reads round (smal = low narrow reading glasses),
    and a bridge. alleen = +1 / -1: a monocle. klein shrinks the frame."""
    k = P.k
    ox, oy, r = P.oog
    oy += laag
    r = r - klein
    hh = 1.2 if smal else r + 0.1
    out = []
    for sx in (1, -1):
        if alleen and sx != alleen:
            continue
        x0, x1 = (ox - r - 0.1, ox + r + 0.1) if sx > 0 else (-ox - r - 0.1, -ox + r + 0.1)
        out += [k([x0 + 0.5, oy + hh, z], [x1 - x0 - 1.0, dik, 0.4], kleur), k([x0 + 0.5, oy - hh - dik, z], [x1 - x0 - 1.0, dik, 0.4], kleur),
                k([x0 - dik + 0.1, oy - hh + 0.5, z], [dik, 2 * hh - 1.0, 0.4], kleur), k([x1 - 0.1, oy - hh + 0.5, z], [dik, 2 * hh - 1.0, 0.4], kleur),
                k([x0, oy + hh - 0.5, z], [0.5, 0.5, 0.4], kleur), k([x1 - 0.5, oy + hh - 0.5, z], [0.5, 0.5, 0.4], kleur),
                k([x0, oy - hh, z], [0.5, 0.5, 0.4], kleur), k([x1 - 0.5, oy - hh, z], [0.5, 0.5, 0.4], kleur)]
    if not alleen:
        out.append(k([-(ox - r - 0.1), oy - 0.1, z], [2 * (ox - r - 0.1), dik, 0.4], kleur))
    return out


def kegelhoed(P, kleur, punt=None, breed=19.0):
    """The wide conical straw hat of the Earth Kingdom."""
    k = P.k
    pt = strepen(_donker(kleur, 0.82), 6, 1, liggend=False)
    b = breed
    return [k([-b / 2, 25.3, -b / 2 + 0.5], [b, 0.8, b - 2], kleur, patroon=pt), k([-b * 0.36, 26.1, -b * 0.36 + 0.5], [b * 0.72, 1.3, b * 0.72 - 1.4], kleur, patroon=pt),
            k([-b * 0.22, 27.4, -b * 0.22 + 0.3], [b * 0.44, 1.3, b * 0.44 - 0.8], kleur, patroon=pt), k([-1.5, 28.7, -1.3], [3, 1.2, 3], punt or _donker(kleur, 0.8))]


def pet(P, kleur, band=None, klep=None, hoog=1.8, breed=12.0):
    """A flat cap, with an optional band and a peak to the front."""
    k = P.k
    out = [k([-breed / 2, 25.4, -6.3], [breed, hoog, 11.2], kleur)]
    if band:
        out.append(k([-breed / 2 - 0.15, 25.4, -6.45], [breed + 0.3, 0.8, 11.5], band))
    if klep:
        out.append(k([-breed / 2 + 2, 25.3, -9.2], [breed - 4, 0.6, 3.0], klep))
    return out


def rondmutsje(P, kleur, band=None, kwast=None):
    """The round cap of Omashguh."""
    k = P.k
    out = [k([-5.5, 25.4, -5.2], [11, 2.0, 9.4], kleur), k([-4.2, 27.4, -4.0], [8.4, 1.0, 7.0], kleur)]
    if band:
        out.append(k([-5.7, 25.4, -5.4], [11.4, 0.9, 9.8], band))
    if kwast:
        out += [k([-0.6, 28.4, -1.2], [1.2, 1.0, 1.2], kwast)]
    return out


def hoofdband(P, kleur, y=23.9, dik=1.1, knoop=True):
    k = P.k
    out = [k([-7.3, y + 0.5, -7.3], [14.6, dik, 13.6], kleur), k([-8.25, y - 0.3, -4.7], [16.5, dik, 8.4], kleur)]
    if knoop:
        out += [k([-1.2, y - 0.2, 6.2], [2.4, 1.6, 1.0], kleur), k([-2.6, y - 2.4, 6.4], [1.4, 2.4, 0.6], kleur), k([1.2, y - 2.8, 6.4], [1.4, 2.8, 0.6], kleur)]
    return out


def kyoshi_kroon(P, goud=GOUD, groot=1.0, kwast=(204, 40, 52)):
    """The golden fan headdress of Kyoshi and her warriors: a band on the forehead and a fan that spreads above it."""
    k = P.k
    rib = strepen(_donker(goud, 0.78), 5, 1, liggend=False)
    b = 5.0 * groot
    out = [k([-7.3, 24.3, -7.5], [14.6, 1.3, 1.2], goud), k([-b, 25.2, -7.6], [2 * b, 2.6 * groot, 0.9], goud, patroon=rib),
           k([-b * 1.3, 25.2 + 2.6 * groot, -7.6], [2.6 * b, 2.4 * groot, 0.9], goud, patroon=rib),
           k([-b, 25.2 + 5.0 * groot, -7.6], [2 * b, 1.4 * groot, 0.9], goud, patroon=rib),
           k([-1.2, 25.6, -7.9], [2.4, 2.4, 0.5], kwast)]
    out += [k([7.3, 19.0, -6.8], [0.8, 5.6, 1.0], kwast), k([-8.1, 19.0, -6.8], [0.8, 5.6, 1.0], kwast)]
    return out


# --- props (body coordinates of the sitting guh) -----------------------------------------------------------------------------
def waaier(P, zijde, goud=GOUD, steel=V_DONKER):
    """A war fan, open, in a paw (zijde +1 = the left paw). Its bone belongs on that arm with the pivot at the paw and is
    turned outwards by the caller, so that the fan stands beside the body and not under the chin."""
    c = P.c
    rib = strepen(_donker(goud, 0.72), 6, 1, liggend=False)
    x = 3.0 * zijde
    return [c([x - 0.4, 9.6, -7.4], [0.8, 1.8, 0.6], steel), c([x - 2.2, 11.4, -7.5], [4.4, 1.5, 0.5], goud, patroon=rib),
            c([x - 3.2, 12.9, -7.5], [6.4, 2.2, 0.5], goud, patroon=rib), c([x - 2.4, 15.1, -7.5], [4.8, 0.9, 0.5], goud, patroon=rib)]


def speer(P, x, lengte=27.0, hout=HOUT, punt=(232, 232, 224), z=-2.5):
    c = P.c
    return [c([x - 0.45, 0, z - 0.45], [0.9, lengte, 0.9], hout), c([x - 0.8, lengte, z - 0.6], [1.6, 2.6, 1.2], punt),
            c([x - 0.45, lengte + 2.6, z - 0.4], [0.9, 1.6, 0.8], punt), c([x - 0.7, lengte - 1.2, z - 0.7], [1.4, 1.0, 1.4], W_BLAUW)]


def bontkraag(P, kleur=BONT, dik=2.4, y=11.2):
    """A thick fur collar round the neck."""
    return [P.c([-5.9, y, -4.9], [11.8, dik, 9.8], kleur)]


def mantel(P, kleur, patroon=None):
    """A short shoulder mantle with pointed shoulders (the fire nation's and the sages')."""
    c = P.c
    return [c([-5.6, 10.2, -4.6], [11.2, 2.9, 9.2], kleur, patroon=patroon), c([5.0, 11.4, -3.2], [2.8, 1.5, 6.4], kleur, patroon=patroon),
            c([-7.8, 11.4, -3.2], [2.8, 1.5, 6.4], kleur, patroon=patroon)]


def jas(P, kleur, patroon=None):
    """A whole coat as cubes over the body (for a mix set with its own colour): the trunk, the hips and both sleeves.
    Returns (body cubes, left sleeve cubes, right sleeve cubes)."""
    c = P.c
    lijf = [c([-5.25, 1.3, -4.25], [10.5, 11.9, 8.5], kleur, patroon=patroon), c([-6.75, 0.8, -3.25], [13.5, 5.4, 6.5], kleur, patroon=patroon)]
    links = [c([1.3, 6.8, -5.6], [3.4, 3.9, 2.4], kleur)]
    rechts = [c([-4.7, 6.8, -5.6], [3.4, 3.9, 2.4], kleur)]
    return lijf, links, rechts


def mikajas(P, kleur, patroon=None):
    """The same for the Mika: a cloth over the back and the flanks."""
    return [P.c([-6.8, 0.9, -1.2], [13.6, 9.5, 14.6], kleur, patroon=patroon)]
