"""
einde2, slice cast_team - how every character looks (CONTRACT_E2 11.4): one function per CHARACTER, with the look as an
argument, each drawing one Kind with einde2_cast_team_kit.Figuur. Guhs are the sitting guh ("zit") with its own head, face
and ears; Zuka and Oom Miroh are the ordinary Mika. Clothes are painted on the body in model space (so a hem runs round
at one height) and get their relief from a few cubes; hair is a painted cap plus cubes; props are bones of their own.

Coordinates (model units, the front is -z, the character's left is +x):
  zit    body y 1..13, head y 12..26 (face at z -7, eyes at x +-4, y 21.5), ears y 23..30, arms in front at y 7..10.5
  mika   head x +-8, y 0..14, z -12..-2 (face at z -12, eyes at x +-3.4, y 7), ears y 11..18, body behind it to z 11
"""
import numpy as np

from features import einde2_cast_team_kit as ck
from features.einde2_cast_team_kit import donker, rand, samen, stip, strepen

KOP = (0, 1, 2, 3)                        # the cubes of a head without the little snout


# =====================================================================================================================
# shared pieces
# =====================================================================================================================
def _vacht_masker(f):
    """The pixels of the face that are fur (not an eye, a brow or the mouth), taken before anything is painted on it."""
    px, W, H = f.gezicht()
    vacht = np.median(px[..., :3].reshape(-1, 3), axis=0)
    return np.abs(px[..., :3].astype(np.int32) - vacht).max(-1) < 34, px.copy()


def _ogen_terug(f, bewaard, rijen=None):
    """Puts the eyes (and the mouth) back over what was painted on the face."""
    masker, oud = bewaard
    px, W, H = f.gezicht()
    m = ~masker
    if rijen is not None:
        m = m.copy()
        m[rijen:, :] = False
    px[m] = oud[m]


def gewaad(f, zones, botten=("body",), voor=None):
    """Clothes painted on base cubes: zones = (y0, y1, rgb) bands; voor(X, Y, zet) paints more on the front faces."""
    def fn(vlak, X, Y, Z, zet):
        for y0, y1, rgb in zones:
            zet((Y >= y0) & (Y < y1), rgb, 6)
        if voor is not None and vlak == "north":
            voor(X, Y, zet)
    f.verf(botten, fn)


def haarkap(f, rgb, voor, zij, achter, var=9, bot="head", kubussen=KOP):
    """A painted cap of hair on the head: everything on top, and down to voor(X) on the front, zij(Z) on the sides and
    `achter` on the back (numbers or functions of the model coordinate)."""
    def lijn(g, V):
        return g(V) if callable(g) else g

    def fn(vlak, X, Y, Z, zet):
        if vlak == "up":
            zet(np.ones_like(X, bool), rgb, var)
        elif vlak == "north":
            zet(Y >= lijn(voor, X), rgb, var)
        elif vlak == "south":
            zet(Y >= lijn(achter, X), rgb, var)
        elif vlak in ("east", "west"):
            zet(Y >= lijn(zij, Z), rgb, var)
    f.verf(bot, fn, kubussen)


def mouwen(f, rgb, manchet=None):
    """Sleeves on the two little arms of a sitting guh: the back half in the colour, a cuff, the paws stay fur."""
    def fn(vlak, X, Y, Z, zet):
        zet(Z > -5.3, rgb, 6)
        if manchet:
            zet((Z > -5.7) & (Z <= -5.1), manchet, 5)
    f.verf(("arm_left", "arm_right"), fn)


def lege_botten(f, namen, ouder="root"):
    """The bones of the character that this look does not show (its animation file moves them all the same)."""
    for naam in namen:
        if naam not in f.botten:
            f.bot(naam, ouder, (0, 0, 0))


def spiegel(kubus):
    """The same cube on the other side."""
    k = dict(kubus)
    k["origin"] = [-(kubus["origin"][0] + kubus["size"][0]), kubus["origin"][1], kubus["origin"][2]]
    return k


def paar(*kubussen):
    return list(kubussen) + [spiegel(k) for k in kubussen]


# =====================================================================================================================
# Aang-guh
# =====================================================================================================================
PIJL = (72, 150, 226)
PIJL_L = (132, 196, 244)
AANG_BOTTEN = ("aang_staf", "aang_vleugels", "aang_bal", "aang_kraag", "aang_cape")


def aang(h, kind="aang_guh"):
    f = ck.Figuur(h, kind, "zit", (0.045, 0.80, 1.04))
    bewaard = _vacht_masker(f)
    oranje, geel, rood = (232, 118, 44), (244, 200, 78), (206, 78, 40)
    gewaad(f, [(0, 7.6, geel), (7.6, 14, oranje)])

    # the arrow: a stripe over the whole head, its point between the eyes
    def pijl(vlak, X, Y, Z, zet):
        if vlak == "up":
            zet(np.abs(X) <= 1.9, PIJL, 5)
            zet((np.abs(X) > 1.5) & (np.abs(X) <= 1.9), PIJL_L, 4)
        elif vlak == "south":
            zet((np.abs(X) <= 1.9) & (Y > 16.5), PIJL, 5)
        elif vlak == "north":
            zet((np.abs(X) <= 1.9) & (Y >= 23.5), PIJL, 5)
            zet((Y < 24.1) & (Y >= 19.9) & (np.abs(X) <= (Y - 19.9) * 0.86), PIJL, 5)
    f.verf("head", pijl, KOP)
    _ogen_terug(f, bewaard)

    # the arrows on the backs of the paws and on the feet
    def pootpijl(vlak, X, Y, Z, zet):
        mid = np.abs(np.abs(X) - 3.0) <= 0.62
        if vlak == "up":
            zet(mid, PIJL, 4)
        elif vlak == "north":
            zet(mid & (Y > 9.4), PIJL, 4)
    f.verf(("arm_left", "arm_right"), pootpijl)

    def voetpijl(vlak, X, Y, Z, zet):
        if vlak == "up":
            zet((np.abs(np.abs(X) - 4.25) <= 0.6) & (Z < -3.4), PIJL, 4)
    f.verf(("foot_left", "foot_right"), voetpijl)

    f.kleur("cape", oranje, 7)
    f.kleur("cape_rand", geel, 5)
    f.kleur("kraag", rood, 6)
    f.kleur("hout", (132, 88, 52), 9, strepen((104, 66, 38), 8, 2))
    f.kleur("vleugel", (236, 132, 48), 6, samen(strepen((196, 96, 36), 8, 2, True), rand((140, 92, 52), 2)))
    f.kleur("lucht", (214, 236, 250), 8, strepen((255, 255, 255), 8, 3))
    f.kleur("lucht_d", (168, 208, 240), 6, strepen((226, 244, 255), 10, 3, True))
    f.bot("aang_cape", "body", (0, 12, 0), [
        f.k((-5.6, 8.6, -4.6), (11.2, 3.6, 9.2), "cape"),
        f.k((-5.7, 8.2, -4.7), (11.4, 0.7, 9.4), "cape_rand")])
    f.bot("aang_kraag", "body", (0, 12, 0), [
        f.k((-4.8, 11.6, -4.9), (9.6, 1.9, 9.0), "kraag"),
        f.k((-4.9, 13.2, -5.0), (9.8, 0.5, 2.0), "cape_rand")])
    # the glider staff in his right paw, the wings folded away (scale 0 outside "zweef")
    f.bot("aang_staf", "body", (-5.5, 13, -6.7), [
        f.k((-6.0, 0.4, -7.2), (1.0, 26.8, 1.0), "hout"),
        f.k((-6.2, 25.8, -7.4), (1.4, 1.6, 1.4), "vleugel"),
        f.k((-6.2, 4.6, -7.4), (1.4, 1.2, 1.4), "vleugel")])
    f.bot("aang_vleugels", "aang_staf", (-5.5, 13, -6.7), [
        f.k((-15.5, 17.0, -6.9), (20.0, 7.6, 0.4), "vleugel"),
        f.k((-17.5, 19.2, -6.9), (24.0, 3.2, 0.4), "vleugel"),
        f.k((-10.5, 1.6, -6.9), (10.0, 4.4, 0.4), "vleugel")])
    # the ball of air he rides (scale 0 outside "luchtscooter")
    f.bot("aang_bal", "root", (0, 5, 0), [
        f.k((-4, 1, -4), (8, 8, 8), "lucht"),
        f.k((-5, 2, -3), (10, 6, 6), "lucht_d"),
        f.k((-3, 2, -5), (6, 6, 10), "lucht_d"),
        f.k((-3, 0, -3), (6, 10, 6), "lucht")])
    f.bewaar()


# =====================================================================================================================
# Katara-guh and Sokka-guh (the same Water Tribe fur)
# =====================================================================================================================
WATERVACHT = (0.068, 1.25, 0.80)
HAAR_BRUIN = (74, 46, 34)
KATARA_BOTTEN = ("katara_lussen", "katara_vlecht", "katara_buidel", "katara_water", "katara_gloed", "katara_kraag", "katara_zoom")


def katara(h, kind="katara_guh"):
    f = ck.Figuur(h, kind, "zit", WATERVACHT)
    bewaard = _vacht_masker(f)
    blauw, diep, wit = (62, 112, 178), (40, 76, 136), (244, 246, 250)

    def voor(X, Y, zet):
        zet((np.abs(X) <= 0.8 + (12.4 - Y) * 0.0) & (Y > 4) & (Y < 12.4) & (np.abs(X) <= 0.9), wit, 4)     # the white front seam
    gewaad(f, [(0, 5.2, diep), (5.2, 14, blauw)], voor=voor)
    mouwen(f, blauw, wit)
    haarkap(f, HAAR_BRUIN, voor=lambda X: 23.9 - 0.9 * (np.abs(X) < 0.5), zij=lambda Z: np.where(Z > -1.5, 16.0, 22.4), achter=13.0)
    _ogen_terug(f, bewaard)
    f.kleur("haar", HAAR_BRUIN, 9)
    f.kleur("kraal", (120, 190, 240), 4)
    f.kleur("bont", wit, 5, strepen((222, 228, 238), 6, 2, True))
    f.kleur("band", (34, 62, 120), 4)
    f.kleur("hanger", (150, 206, 240), 4, samen(stip((236, 248, 255), 9), stip((90, 150, 214), 4)))
    f.kleur("leer", (150, 108, 74), 8, strepen((112, 78, 52), 10, 2))
    f.kleur("stop", (240, 234, 220), 4)
    f.kleur("water", (112, 186, 244), 8, strepen((214, 240, 255), 8, 3))
    f.kleur("gloed", (190, 236, 255), 5, stip((255, 255, 255), 9))
    # the hair loops: from the parting down the sides of the face (a blue bead on each) and back to the bun
    f.bot("katara_lussen", "head", (0, 24, -6), paar(
        f.k((0.8, 23.3, -7.6), (5.6, 1.0, 0.8), "haar"),
        f.k((6.3, 17.6, -7.6), (1.1, 6.6, 0.9), "haar"),
        f.k((6.1, 19.6, -7.8), (1.5, 1.3, 1.3), "kraal"),
        f.k((7.9, 17.4, -7.0), (0.9, 1.1, 11.0), "haar")) + [
        f.k((-2.4, 22.4, 5.4), (4.8, 4.4, 3.0), "haar"),                       # the bun
        f.k((-2.6, 23.9, 5.8), (5.2, 1.0, 2.8), "kraal")])
    f.bot("katara_vlecht", "head", (0, 22, 7), [
        f.k((-1.3, 13.0, 6.6), (2.6, 9.6, 2.2), "haar"),
        f.k((-1.5, 12.2, 6.4), (3.0, 1.2, 2.6), "kraal"),
        f.k((-1.0, 10.2, 6.8), (2.0, 2.0, 1.8), "haar")])
    f.bot("katara_kraag", "body", (0, 12, 0), [
        f.k((-5.7, 11.2, -4.9), (11.4, 1.8, 9.8), "bont"),
        f.k((-3.4, 12.6, -5.1), (6.8, 0.7, 0.5), "band"),                     # the necklace
        f.k((-1.0, 11.3, -5.4), (2.0, 1.9, 0.6), "hanger")])
    f.bot("katara_zoom", "body", (0, 3, 0), [f.k((-5.5, 2.3, -4.5), (11.0, 1.7, 9.0), "bont")])
    f.bot("katara_buidel", "body", (6.5, 6, 0), [
        f.k((6.3, 2.6, -2.4), (2.0, 4.6, 3.4), "leer"),
        f.k((6.6, 7.2, -1.5), (1.4, 1.2, 1.6), "stop"),
        f.k((6.2, 5.6, -2.5), (2.2, 0.7, 3.6), "band")])
    # the ribbon of water (scale 0 outside "waterzweep"): an arc from the pouch round her front
    f.bot("katara_water", "root", (7, 7, -1), [
        f.k((7.2, 7.6, -4.2), (1.6, 3.4, 1.6), "water"),
        f.k((6.4, 10.6, -7.2), (1.6, 1.8, 3.6), "water"),
        f.k((2.4, 11.6, -10.2), (5.2, 1.6, 1.6), "water"),
        f.k((-3.6, 12.6, -10.8), (6.4, 1.6, 1.6), "water"),
        f.k((-8.2, 11.4, -9.6), (5.0, 1.6, 1.6), "water"),
        f.k((-9.8, 8.6, -8.0), (1.8, 3.2, 1.8), "water")])
    f.bot("katara_gloed", "root", (0, 8, -6), paar(
        f.k((1.1, 6.6, -7.4), (3.8, 4.3, 1.4), "gloed"),
        f.k((1.6, 7.1, -8.0), (2.8, 3.3, 0.8), "gloed")))
    f.bewaar()


SOKKA_BOTTEN = ("sokka_staartje", "sokka_boemerang", "sokka_vlucht", "sokka_band", "sokka_riem")


def sokka(h, kind="sokka_guh"):
    f = ck.Figuur(h, kind, "zit", WATERVACHT)
    bewaard = _vacht_masker(f)
    blauw, diep, wit = (74, 126, 186), (44, 82, 140), (240, 242, 246)
    stoppel = (150, 104, 80)

    def voor(X, Y, zet):
        zet((Y > 8.0) & (np.abs(np.abs(X) - (12.6 - Y) * 0.62) <= 0.55), wit, 4)        # the white V of his tunic
    gewaad(f, [(0, 4.6, diep), (4.6, 14, blauw)], voor=voor)
    # shaved sides (a darker stubble), a strip of hair over the top to the wolf tail
    haarkap(f, stoppel, voor=24.4, zij=20.5, achter=17.0, var=12)

    def strook(vlak, X, Y, Z, zet):
        if vlak == "up":
            zet(np.abs(X) <= 3.4, HAAR_BRUIN, 9)
        elif vlak == "north":
            zet((np.abs(X) <= 3.4) & (Y >= 23.8), HAAR_BRUIN, 9)
        elif vlak == "south":
            zet((np.abs(X) <= 3.4) & (Y >= 21.0), HAAR_BRUIN, 9)
    f.verf("head", strook, KOP)
    _ogen_terug(f, bewaard)
    f.kleur("haar", HAAR_BRUIN, 9)
    f.kleur("lint", (58, 104, 190), 4)
    f.kleur("staal", (136, 170, 206), 6, samen(rand((84, 112, 150), 3), strepen((176, 204, 232), 16, 3)))
    f.kleur("band", (34, 62, 120), 4)
    f.kleur("tand", (244, 240, 226), 4)
    f.kleur("riem", (120, 84, 56), 7)
    f.kleur("gesp", (226, 226, 214), 4)
    f.bot("sokka_staartje", "head", (0, 25, 3.5), [
        f.k((-1.5, 24.6, 2.6), (3.0, 1.6, 3.0), "lint"),
        f.k((-1.3, 26.0, 2.8), (2.6, 2.6, 2.6), "haar"),
        f.k((-1.1, 26.8, 5.0), (2.2, 2.4, 2.6), "haar"),
        f.k((-0.8, 25.4, 7.2), (1.6, 2.6, 1.4), "haar")])
    f.bot("sokka_band", "body", (0, 12, 0), [
        f.k((-5.2, 11.9, -4.5), (10.4, 0.9, 9.0), "band"),
        f.k((-0.7, 11.2, -4.9), (1.4, 1.5, 0.5), "tand")])
    f.bot("sokka_riem", "body", (0, 5, 0), [
        f.k((-5.3, 4.2, -4.4), (10.6, 1.2, 8.8), "riem"),
        f.k((-1.0, 4.0, -4.7), (2.0, 1.6, 0.5), "gesp")])
    # the boomerang on his back (a bone turned 45 degrees, so the two arms make a V); the one that flies is a prop
    boem = lambda dx, dy, dz: [f.k((dx - 4.2, dy - 0.9, dz), (6.0, 1.8, 0.9), "staal"), f.k((dx - 0.0, dy - 0.9, dz), (1.8, 6.0, 0.9), "staal")]
    f.bot("sokka_boemerang", "body", (0, 9, 4.6), boem(0, 9, 4.3), rotatie=(0, 0, 135))
    f.bot("sokka_vlucht", "root", (-3, 10, -8), boem(-3, 10, -8.4), rotatie=(0, 0, 135))
    f.bewaar()


# =====================================================================================================================
# Toph-guh
# =====================================================================================================================
TOPH_BOTTEN = ("toph_band", "toph_haar", "toph_riem", "toph_zuil", "toph_kraag")


def toph(h, kind="toph_guh"):
    f = ck.Figuur(h, kind, "zit", (0.075, 0.38, 1.06))
    bewaard = _vacht_masker(f)
    zwart, groen, creme, goud = (30, 28, 34), (96, 146, 78), (236, 224, 172), (232, 190, 70)
    gewaad(f, [(0, 4.0, creme), (4.0, 8.6, groen), (8.6, 14, creme)])
    mouwen(f, creme, groen)
    haarkap(f, zwart, voor=23.7, zij=lambda Z: np.where(Z > 1.0, 17.5, 22.6), achter=15.0, var=7)
    _ogen_terug(f, bewaard)
    # her eyes: pale and clouded, they look straight ahead (the dark of the eye turns milky, the ring a soft grey-green)
    px, W, H = f.gezicht()
    rgb = px[..., :3].astype(np.int32)
    oog = (~bewaard[0]) & (np.arange(H)[:, None] < 25)
    donker_ = oog & (rgb.max(-1) < 90)
    ring = oog & (rgb[..., 2] > rgb[..., 0] + 25) & ~donker_
    px[donker_, :3] = (206, 224, 216)
    px[ring, :3] = (164, 196, 186)
    px[oog & (rgb.min(-1) > 225), :3] = (226, 238, 232)
    rand_ = np.zeros_like(oog)
    rand_[1:-1, 1:-1] = oog[1:-1, 1:-1] & ~(oog[:-2, 1:-1] & oog[2:, 1:-1] & oog[1:-1, :-2] & oog[1:-1, 2:])
    px[rand_, :3] = (104, 132, 124)
    # bare paws: bigger feet, the pads to the front
    for voet, sx in (("foot_left", 1), ("foot_right", -1)):
        f.breder(voet, 1.2, 1.25, 1.12, om=(4.25 * sx, 0, -2))

    def zolen(vlak, X, Y, Z, zet):
        if vlak == "north":
            zet(np.ones_like(X, bool), (250, 214, 204), 4)
            cx = np.sign(X) * 4.25
            zet(np.hypot((X - cx) / 1.25, (Y - 0.95) / 0.8) <= 1, (244, 150, 160), 4)
            for d in (-1.3, 0, 1.3):
                zet(np.hypot(X - cx - d, Y - 2.05) <= 0.42, (244, 150, 160), 4)
    f.verf(("foot_left", "foot_right"), zolen)
    f.kleur("haar", zwart, 7)
    f.kleur("band", groen, 5, strepen((70, 112, 58), 16, 3))
    f.kleur("kwast", (246, 236, 190), 5, strepen((214, 196, 130), 6, 2, True))
    f.kleur("riem", (70, 112, 58), 5, rand(goud, 3))
    f.kleur("gesp", goud, 5, samen(stip((250, 228, 130), 11), stip((96, 146, 78), 6), stip(goud, 3)))
    f.kleur("steen", (140, 130, 118), 12, strepen((108, 100, 92), 11, 2))
    f.kleur("gras", (118, 104, 88), 10)
    f.bot("toph_haar", "head", (0, 25, 0), [
        f.k((-3.6, 25.6, -2.2), (7.2, 3.6, 6.6), "haar"),                       # the big bun
        f.k((-2.6, 29.2, -1.2), (5.2, 1.4, 4.6), "haar"),
        f.k((-6.6, 21.6, -7.5), (13.2, 2.2, 0.7), "haar"),                      # the fringe, down to her eyes
        f.k((-7.3, 17.6, -7.5), (1.7, 5.0, 0.8), "haar"), f.k((5.6, 17.6, -7.5), (1.7, 5.0, 0.8), "haar")])
    f.bot("toph_band", "head", (0, 24, 0), [
        f.k((-7.4, 23.6, -7.7), (14.8, 1.5, 13.9), "band"),
        f.k((-8.6, 21.0, -6.2), (1.5, 2.8, 1.5), "kwast"), f.k((7.1, 21.0, -6.2), (1.5, 2.8, 1.5), "kwast"),
        f.k((-8.2, 23.4, -5.9), (0.8, 1.0, 0.9), "band"), f.k((7.4, 23.4, -5.9), (0.8, 1.0, 0.9), "band")])
    f.bot("toph_kraag", "body", (0, 12, 0), [f.k((-5.3, 11.5, -4.5), (10.6, 1.3, 9.0), "band")])
    f.bot("toph_riem", "body", (0, 5, 0), [
        f.k((-5.4, 4.4, -4.5), (10.8, 2.3, 9.0), "riem"),
        f.k((-2.1, 3.7, -5.2), (4.2, 3.7, 0.8), "gesp")])
    # the little pillar of rock that shoots up beside her (scale 0 outside "stamp")
    f.bot("toph_zuil", "root", (11.5, 0, -1), [
        f.k((9.0, 0, -3.5), (5.0, 9.0, 5.0), "steen"),
        f.k((9.6, 9.0, -2.9), (3.8, 3.0, 3.8), "steen"),
        f.k((8.4, 0, -4.1), (6.2, 1.6, 6.2), "gras")])
    f.bewaar()


# =====================================================================================================================
# Zuka (a Mika), six looks through the story (CONTRACT_E2 11.9): the banished prince (balling), the Blue Spirit (geest), the
# exile who waits tables (ober, the kind ZUKA), the prince back home (prins), one of the team (team), the Vuurheer
# =====================================================================================================================
ZUKA_BOTTEN = ("zuka_litteken", "zuka_knot", "zuka_schort", "zuka_dienblad", "zuka_harnas", "zuka_kroon", "zuka_vlam",
               "zuka_staart", "zuka_masker", "zuka_zwaarden", "zuka_zwaard_links", "zuka_zwaard_rechts", "zuka_kraag")
MIKA_HAAR = (58, 36, 30)


def _litteken(block):
    """The burn scar: a ragged dark red patch, the eye in it narrowed to a slit."""
    yy, xx = np.mgrid[0:32, 0:32]
    d = np.hypot((xx - 15.5) / 15.5, (yy - 15.5) / 14.0) + 0.10 * np.sin(xx * 1.3) * np.cos(yy * 0.9)
    block[..., :3] = (150, 52, 50)
    block[d > 0.62, :3] = (120, 36, 40)
    block[d > 0.86, :3] = (96, 28, 34)
    block[d > 1.0, 3] = 0
    block[17:19, 7:25, :3] = (40, 16, 20)                      # the narrowed eye
    block[19:20, 9:23, :3] = (40, 16, 20)
    block[18:19, 12:20, :3] = (236, 170, 60)
    block[18:19, 15:17, :3] = (30, 12, 14)
    block[6:8, 6:22, :3] = (96, 28, 34)                        # no brow left: a dark ridge


def _mika_ogen(f, iris, licht, streep=None):
    """A Mika's red eyes in another colour (the dark slit and the outline stay)."""
    px, W, H = f.gezicht()
    rgb = px[..., :3].astype(np.int32)
    rood = (rgb[..., 0] > 150) & (rgb[..., 1] < 110) & (rgb[..., 2] < 110)
    lichtrood = rood & (rgb[..., 1] > 55)
    px[rood, :3] = iris
    px[lichtrood, :3] = licht


def _mika_wenkbrauwen(f, bewaard, rgb, vorm):
    """Replaces the angry brows: vorm(u, v) says for a pixel of the face (u 0..1 from the nose outward, v = rows from the
    top) whether it is brow. The old brow pixels above the eyes become fur."""
    px, W, H = f.gezicht()
    masker, oud = bewaard
    vacht = np.median(oud[..., :3][masker].reshape(-1, 3), axis=0)
    c = px[..., :3].astype(np.int32)
    wenk = (~masker) & (c.max(-1) < 90) & (np.arange(H)[:, None] < 18)
    oog = (c[..., 0] > 150) & (c[..., 1] < 120)
    # brow pixels: dark ones that are not the outline of an eye (no eye pixel right below or beside them)
    naast = np.zeros_like(oog)
    naast[:-1] |= oog[1:]
    naast[1:] |= oog[:-1]
    naast[:, 1:] |= oog[:, :-1]
    naast[:, :-1] |= oog[:, 1:]
    weg = wenk & ~naast & (np.arange(H)[:, None] < 14)
    px[weg, :3] = np.clip(vacht + f.rng.normal(0, 2, (int(weg.sum()), 1)), 0, 255).astype(np.uint8)
    yy, xx = np.mgrid[0:H, 0:W]
    u = np.abs(xx + 0.5 - W / 2) / (W / 2)
    px[vorm(u, yy) & (~oog), :3] = rgb


def zuka(h, kind="zuka", look="ober"):
    f = ck.Figuur(h, kind, "mika", (0.985, 0.72, 1.06))
    bewaard = _vacht_masker(f)
    goud, rood = (236, 190, 66), (170, 40, 40)
    _mika_ogen(f, (226, 150, 40), (250, 204, 96))
    if look == "vuurheer":                                   # a calmer face: level brows
        _mika_wenkbrauwen(f, bewaard, MIKA_HAAR, lambda u, v: (v >= 9) & (v <= 11) & (u > 0.12) & (u < 0.86))
    # the scar over his LEFT eye (+x), up to the ear, which is burnt too
    f.kleur("litteken", (150, 52, 50), 5, _litteken)
    f.kleur("litteken_zij", (132, 44, 44), 9)
    f.bot("zuka_litteken", "head", (3.6, 8, -12), [
        f.k((0.5, 4.5, -12.3), (6.9, 7.2, 0.35), "litteken"),
        f.k((7.75, 5.4, -11.6), (0.35, 6.0, 5.0), "litteken_zij")])

    def oor(vlak, X, Y, Z, zet):
        zet(np.ones_like(X, bool), (150, 60, 60), 12)
        zet(Y > 16.0, (116, 40, 44), 10)
    f.verf("ear_left", oor)
    f.kleur("haar", MIKA_HAAR, 9)
    f.kleur("goud", goud, 6)
    f.kleur("vlam", (250, 150, 40), 8, stip((255, 232, 120), 10))
    f.kleur("vlam_l", (255, 226, 110), 6, stip((255, 250, 220), 8))
    f.bot("zuka_vlam", "head", (0, 4, -14), [
        f.k((-1.6, 2.6, -16.4), (3.2, 3.2, 3.2), "vlam"),
        f.k((-1.0, 5.4, -15.8), (2.0, 2.2, 2.0), "vlam_l"),
        f.k((-0.5, 7.2, -15.3), (1.0, 1.4, 1.0), "vlam_l")])
    if look == "ober":
        groen, licht = (104, 132, 84), (198, 196, 150)
        # shaggy hair, no neat knot; a waiter's green apron with its bow on his back; a little tray with one cup
        haarkap(f, MIKA_HAAR, voor=lambda X: 11.7 + 0.5 * np.sin(X * 2.1), zij=9.6, achter=5.0, var=10)
        _ogen_terug(f, bewaard, 30)
        _mika_ogen(f, (226, 150, 40), (250, 204, 96))
        gewaad(f, [(0, 2.2, licht), (2.2, 12, (150, 132, 96))])
        f.bot("zuka_knot", "head", (0, 14, -6), [
            f.k((-5.9, 13.6, -10.4), (11.8, 1.3, 9.0), "haar"),
            f.k((-3.8, 14.8, -9.4), (7.6, 1.1, 6.6), "haar"),
            f.k((-1.4, 15.6, -7.6), (3.2, 1.3, 3.0), "haar"),
            f.k((2.2, 15.4, -5.6), (2.0, 1.2, 2.2), "haar"),
            f.k((-5.4, 12.3, -12.7), (2.6, 2.0, 0.9), "haar"),
            f.k((-2.0, 11.9, -12.7), (3.0, 2.4, 0.9), "haar"),
            f.k((2.0, 12.4, -12.7), (2.2, 1.9, 0.9), "haar")])
        f.kleur("schort", groen, 6, rand(donker(groen, 0.78), 3))
        f.kleur("schort_l", donker(groen, 0.8), 5)
        f.kleur("hout", (150, 104, 62), 8, rand((108, 72, 44), 3))
        f.kleur("kopje", (244, 242, 232), 4, strepen((120, 170, 130), 32, 5))
        f.kleur("thee", (150, 112, 50), 4)
        f.bot("zuka_schort", "body", (0, 6, 2), [
            f.k((-7.0, 0.2, -1.0), (14.0, 10.4, 5.2), "schort"),
            f.k((-2.0, 10.4, 3.0), (1.8, 1.5, 1.4), "schort_l"), f.k((0.2, 10.4, 3.0), (1.8, 1.5, 1.4), "schort_l"),
            f.k((-0.6, 10.5, 3.2), (1.2, 1.2, 1.0), "schort"),
            f.k((-1.6, 7.4, 4.3), (1.0, 3.2, 0.5), "schort_l"), f.k((0.6, 7.0, 4.3), (1.0, 3.6, 0.5), "schort_l")])
        f.bot("zuka_dienblad", "leg_front_right", (-6, 2.5, -15), [
            f.k((-9.4, 2.2, -17.6), (6.4, 0.6, 5.2), "hout"),
            f.k((-7.2, 2.8, -16.0), (2.0, 1.9, 2.0), "kopje"),
            f.k((-6.9, 4.5, -15.7), (1.4, 0.25, 1.4), "thee")])
    elif look == "prins":
        donkerrood = (120, 30, 34)
        gewaad(f, [(0, 12, (60, 34, 40))])
        f.kleur("harnas", donkerrood, 6, rand(goud, 3))
        f.kleur("harnas_d", (84, 26, 32), 6)
        f.kleur("lint", rood, 5)
        f.bot("zuka_knot", "head", (0, 14, -6), [
            f.k((-1.7, 14.0, -7.7), (3.4, 1.2, 3.4), "lint"),
            f.k((-1.3, 15.2, -7.3), (2.6, 3.4, 2.6), "haar"),
            f.k((-1.1, 17.0, -4.9), (2.2, 1.9, 2.8), "haar"),
            f.k((-0.9, 13.6, -2.7), (1.8, 4.8, 1.6), "haar"),
            f.k((-0.7, 15.0, -8.1), (1.4, 2.2, 0.5), "goud"),                    # the little flame pin
            f.k((-0.3, 17.2, -8.1), (0.6, 0.9, 0.5), "goud")])
        f.bot("zuka_harnas", "body", (0, 6, 0), [
            f.k((-8.6, 0.2, -2.9), (17.2, 11.4, 2.4), "harnas"),                 # the collar behind his head
            f.k((-9.4, 8.4, -2.7), (2.4, 3.4, 2.0), "harnas"), f.k((7.0, 8.4, -2.7), (2.4, 3.4, 2.0), "harnas"),
            f.k((-7.3, 5.8, -0.6), (14.6, 4.9, 5.4), "harnas"),                  # the shoulder plates
            f.k((-7.0, 0.4, 4.6), (14.0, 9.8, 1.2), "harnas_d")])
    elif look == "balling":
        # shaved bald but for one dark ponytail at the crown; dark red and black armour with shoulder plates; a grim face
        kaal = (196, 120, 116)

        def kruin(vlak, X, Y, Z, zet):                                      # the shaved scalp: paler stubble on top
            if vlak == "up":
                zet(np.ones_like(X, bool), kaal, 10)
            elif vlak in ("east", "west"):
                zet(Y >= 11.6, kaal, 10)
            elif vlak == "south":
                zet(Y >= 9.0, kaal, 10)
        f.verf("head", kruin, KOP)
        _ogen_terug(f, bewaard, 30)
        _mika_ogen(f, (226, 150, 40), (250, 204, 96))
        zwart, donkerrood = (38, 30, 34), (104, 26, 30)
        gewaad(f, [(0, 12, zwart)])
        f.kleur("lint", donkerrood, 5)
        f.kleur("harnas", donkerrood, 6, rand(zwart, 4))
        f.kleur("harnas_d", zwart, 6, strepen(donkerrood, 8, 2))
        f.bot("zuka_staart", "head", (0, 14, -6), [
            f.k((-1.3, 13.9, -7.3), (2.6, 1.0, 2.6), "lint"),
            f.k((-1.1, 14.6, -7.1), (2.2, 1.6, 2.2), "haar"),
            f.k((-0.9, 13.4, -5.0), (1.8, 1.6, 2.6), "haar"),               # it falls back over the crown
            f.k((-0.8, 9.6, -2.8), (1.6, 4.2, 1.4), "haar")])
        f.bot("zuka_harnas", "body", (0, 6, 0), [
            f.k((-8.2, 0.2, -2.8), (16.4, 10.6, 2.2), "harnas"),                 # the collar behind his head
            f.k((-9.8, 7.8, -2.6), (3.0, 3.8, 3.0), "harnas_d"), f.k((6.8, 7.8, -2.6), (3.0, 3.8, 3.0), "harnas_d"),
            f.k((-7.0, 6.0, -0.4), (14.0, 4.6, 5.6), "harnas"),                  # the shoulder plates on his back
            f.k((-6.6, 0.6, 5.0), (13.2, 9.0, 1.2), "harnas_d")])
    elif look == "geest":
        # the Blue Spirit: the blue grinning demon mask over the whole face, black clothes, two dao swords crossed on the back
        zwart = (34, 32, 40)
        gewaad(f, [(0, 12, zwart)])

        def verf_kop(vlak, X, Y, Z, zet):                                   # a black hood over the back of the head
            if vlak in ("up", "south"):
                zet(np.ones_like(X, bool), zwart, 5)
            elif vlak in ("east", "west"):
                zet(Z > -8.0, zwart, 5)
        f.verf("head", verf_kop, KOP)

        def masker(block):
            yy, xx = np.mgrid[0:32, 0:32]
            block[..., :3] = (40, 70, 150)
            block[(yy < 4), :3] = (26, 46, 108)                              # the dark brow ridge
            for cx in (9, 22):                                               # the white eyes with a dark pupil, black brows
                oog = np.hypot((xx - cx) / 5.0, (yy - 10.5) / 3.6) <= 1
                block[oog, :3] = (246, 246, 250)
                block[np.hypot(xx - cx, yy - 10.5) <= 1.4, :3] = (20, 20, 30)
                helling = 0.35 if cx < 16 else -0.35
                block[(np.abs(yy - (5.5 + (xx - cx) * helling)) <= 1.0) & (np.abs(xx - cx) <= 5), :3] = (14, 14, 22)
            block[(yy >= 18) & (yy <= 27) & (np.abs(xx - 15.5) <= 12 - (27 - yy) * 0.4), :3] = (150, 30, 40)   # the grin
            tand = (yy >= 19) & (yy <= 25) & (np.abs(xx - 15.5) <= 10.5) & (((xx - 4) % 3) != 0)
            block[tand, :3] = (248, 246, 236)
            block[(yy == 22) & (np.abs(xx - 15.5) <= 10.5), :3] = (120, 24, 34)
            block[(np.abs(xx - 15.5) <= 1.2) & (yy >= 12) & (yy <= 16), :3] = (26, 46, 108)   # the nose ridge
        f.kleur("masker", (40, 70, 150), 4, masker)
        f.kleur("masker_rand", (26, 46, 108), 4)
        f.kleur("staal", (190, 196, 206), 5, strepen((226, 230, 238), 8, 2, True))
        f.kleur("gevest", (40, 30, 26), 5, strepen((150, 112, 60), 6, 2))
        f.kleur("riem", (60, 44, 34), 5)
        f.bot("zuka_masker", "head", (0, 7, -12), [
            f.k((-7.4, 0.9, -12.9), (14.8, 12.0, 0.7), "masker"),
            f.k((-7.6, 12.4, -12.7), (15.2, 1.0, 1.4), "masker_rand"),       # its rim and the brow horns
            f.k((-6.6, 12.9, -12.6), (1.6, 1.6, 1.0), "masker_rand"), f.k((5.0, 12.9, -12.6), (1.6, 1.6, 1.0), "masker_rand")])
        f.bot("zuka_zwaarden", "body", (0, 10, 4.5), [f.k((-5.6, 9.6, 1.0), (11.2, 0.5, 1.2), "riem")])
        for naam, hoek in (("zuka_zwaard_links", 32), ("zuka_zwaard_rechts", -32)):
            f.bot(naam, "zuka_zwaarden", (0, 10.6, 4.5), [
                f.k((-0.5, 10.3, -1.0), (1.0, 0.5, 9.0), "staal"),               # the broad blade
                f.k((-1.3, 10.2, 7.8), (2.6, 0.7, 0.6), "gevest"),               # the guard
                f.k((-0.45, 10.25, 8.4), (0.9, 0.6, 3.0), "gevest")], rotatie=(0, hoek, 0))
    elif look == "team":
        # one of the team: short shaggy hair (no knot), a plain red tunic with a gold edge, no armour, a shy, softer face
        haarkap(f, MIKA_HAAR, voor=lambda X: 11.4 + 0.6 * np.sin(X * 1.7), zij=9.0, achter=4.0, var=10)
        _ogen_terug(f, bewaard, 30)
        _mika_ogen(f, (226, 150, 40), (250, 204, 96))
        # soft brows that rise towards the middle (shy, a little unsure)
        _mika_wenkbrauwen(f, bewaard, MIKA_HAAR, lambda u, v: (u > 0.12) & (u < 0.86) & (np.abs(v - (8.6 + 2.4 * u)) <= 0.9))
        rood = (176, 44, 42)
        gewaad(f, [(0, 1.8, goud), (1.8, 12, rood)])
        f.kleur("rood", rood, 6, rand(goud, 3))
        f.bot("zuka_knot", "head", (0, 14, -6), [
            f.k((-6.0, 13.5, -10.6), (12.0, 1.1, 9.2), "haar"),
            f.k((-4.6, 14.4, -9.8), (9.2, 1.0, 7.4), "haar"),
            f.k((-5.6, 11.6, -12.7), (3.0, 2.4, 0.9), "haar"),                  # a shaggy fringe
            f.k((-2.4, 11.2, -12.7), (2.6, 2.8, 0.9), "haar"),
            f.k((0.4, 11.5, -12.7), (2.8, 2.5, 0.9), "haar"),
            f.k((3.4, 11.9, -12.7), (2.4, 2.1, 0.9), "haar"),
            f.k((-6.4, 9.6, -3.2), (12.8, 4.4, 1.4), "haar")])                  # the back of his neck
        f.bot("zuka_kraag", "body", (0, 6, 0), [f.k((-7.0, 7.4, -2.6), (14.0, 2.4, 1.6), "rood")])
    else:
        robe = (150, 34, 38)
        haarkap(f, MIKA_HAAR, voor=11.9, zij=8.6, achter=3.0, var=8)
        _ogen_terug(f, bewaard, 30)
        _mika_ogen(f, (226, 150, 40), (250, 204, 96))
        _mika_wenkbrauwen(f, bewaard, MIKA_HAAR, lambda u, v: (v >= 9) & (v <= 11) & (u > 0.12) & (u < 0.86))
        gewaad(f, [(0, 2.0, goud), (2.0, 12, robe)])
        f.kleur("mantel", robe, 6, rand(goud, 3))
        f.kleur("mantel_d", (112, 24, 30), 6)
        f.bot("zuka_knot", "head", (0, 14, -6), [
            f.k((-5.8, 13.7, -10.3), (11.6, 0.9, 8.8), "haar"),
            f.k((-1.9, 14.5, -7.9), (3.8, 2.6, 3.8), "haar"),
            f.k((-6.9, 6.0, -1.4), (13.8, 6.6, 1.4), "haar")])                  # his hair down his neck
        f.bot("zuka_kroon", "zuka_knot", (0, 15, -8), [                          # the five-point flame
            f.k((-3.3, 14.6, -8.6), (6.6, 1.1, 0.7), "goud"),
            f.k((-0.6, 15.6, -8.6), (1.2, 4.6, 0.7), "goud"),
            f.k((-1.95, 15.6, -8.6), (1.1, 3.2, 0.7), "goud"), f.k((0.85, 15.6, -8.6), (1.1, 3.2, 0.7), "goud"),
            f.k((-3.3, 15.6, -8.6), (1.1, 1.9, 0.7), "goud"), f.k((2.2, 15.6, -8.6), (1.1, 1.9, 0.7), "goud")])
        f.bot("zuka_harnas", "body", (0, 6, 0), [
            f.k((-8.8, 0.2, -2.9), (17.6, 11.6, 2.4), "mantel"),
            f.k((-9.6, 9.0, -2.7), (2.6, 3.6, 2.0), "mantel"), f.k((7.0, 9.0, -2.7), (2.6, 3.6, 2.0), "mantel"),
            f.k((-7.4, 5.6, -0.6), (14.8, 5.2, 7.0), "mantel"),
            f.k((-7.0, 0.4, 6.4), (14.0, 5.4, 1.0), "mantel_d")])
    lege_botten(f, ZUKA_BOTTEN)
    f.bewaar()


# =====================================================================================================================
# Oom Miroh (a round old Mika): tea house owner, prisoner, the Witte Lotus
# =====================================================================================================================
MIROH_BOTTEN = ("miroh_baard", "miroh_knot", "miroh_kopje", "miroh_stoom", "miroh_schort", "miroh_kraag", "miroh_tegel", "miroh_lotus")
GRIJS = (206, 204, 206)


def _lotus(block):
    """The white lotus tile: a pale disc, the flower's petals round a golden heart."""
    yy, xx = np.mgrid[0:32, 0:32]
    r, a = np.hypot(xx - 15.5, yy - 15.5), np.arctan2(yy - 15.5, xx - 15.5)
    block[..., :3] = (236, 226, 200)
    block[r > 14.5, :3] = (150, 120, 84)
    blad = (r < 5.5 + 5.5 * np.abs(np.cos(a * 4))) & (r > 3)
    block[blad, :3] = (250, 250, 252)
    block[blad & (np.abs(np.cos(a * 4)) < 0.35), :3] = (176, 196, 226)
    block[r <= 3, :3] = (244, 200, 80)
    block[r > 15.5, 3] = 0


def miroh(h, kind="oom_miroh", look="thee"):
    f = ck.Figuur(h, kind, "mika", (0.035, 0.62, 0.98))
    bewaard = _vacht_masker(f)
    goud = (232, 190, 70)
    f.breder("body", 1.25, 1.12, 1.04, om=(0, 0, 4))                   # ROUND
    f.breder("leg_back_left", 1.1, 1.0, 1.0, om=(7.6, 0, 8))
    f.breder("leg_back_right", 1.1, 1.0, 1.0, om=(-7.6, 0, 8))
    for been, sx in (("leg_back_left", 1), ("leg_back_right", -1)):
        for c in f.botten[been]["cubes"]:
            c["origin"][0] += 1.6 * sx
    # kind eyes: warm amber, and soft grey brows that arch instead of frown
    _mika_ogen(f, (214, 150, 60), (246, 206, 120))
    _mika_wenkbrauwen(f, bewaard, GRIJS, lambda u, v: (u > 0.14) & (u < 0.9) & (np.abs(v - (9.0 - 3.2 * np.sin((u - 0.14) / 0.76 * np.pi))) <= 1.3))
    wild = look == "gevangene"
    baard = (150, 148, 152) if wild else GRIJS
    f.kleur("baard", baard, 12 if wild else 7, strepen(donker(baard, 0.86), 5, 2, True))
    # grey hair round the back of his head, bald in front
    haarkap(f, baard, voor=99, zij=lambda Z: np.where(Z > -6.0, 7.0, 99), achter=1.0, var=10)

    def kruin(vlak, X, Y, Z, zet):
        if vlak == "up":
            zet(Z < -5.6, tuple(int(v) for v in np.median(bewaard[1][..., :3][bewaard[0]].reshape(-1, 3), axis=0)), 5)
    f.verf("head", kruin, KOP)
    stukken = paar(f.k((6.5, 1.0, -12.7), (1.6, 6.4, 1.6), "baard"),             # the sideburns
                   f.k((3.6, 0.2, -12.9), (3.2, 2.2, 1.2), "baard"))
    stukken += [f.k((-3.4, 0.0, -13.2), (6.8, 2.6, 1.4), "baard"), f.k((-2.0, 0.0, -14.0), (4.0, 1.7, 1.0), "baard")]
    if wild:                                                               # tangled: it sticks out everywhere
        stukken += [f.k((-5.6, 0.0, -13.6), (2.2, 3.4, 1.2), "baard"), f.k((3.0, 0.0, -14.2), (2.4, 2.8, 1.2), "baard"),
                    f.k((-1.0, 0.0, -15.0), (2.4, 1.2, 1.2), "baard"), f.k((7.6, 4.0, -11.0), (1.2, 3.0, 2.6), "baard"),
                    f.k((-8.8, 2.4, -11.4), (1.2, 3.6, 2.2), "baard")]
    f.bot("miroh_baard", "head", (0, 2, -12), stukken)
    f.kleur("goud", goud, 6)
    f.kleur("stoom", (250, 250, 252), 3)
    f.kleur("tegel", (236, 226, 200), 4, _lotus)
    f.bot("miroh_tegel", "root", (-7.5, 0.4, -16), [f.k((-9.2, 0.0, -17.6), (3.4, 0.7, 3.4), "tegel")])
    if look == "thee":
        groen = (88, 132, 74)
        gewaad(f, [(0, 2.4, goud), (2.4, 13, groen)])
        f.kleur("schort", (238, 230, 204), 5, rand((204, 190, 150), 3))
        f.kleur("kopje", (246, 244, 236), 4, strepen((70, 130, 170), 32, 5))
        f.kleur("thee", (168, 128, 54), 4)
        f.bot("miroh_knot", "head", (0, 14, -5), [
            f.k((-1.6, 14.0, -5.6), (3.2, 2.0, 3.2), "baard"),
            f.k((-2.0, 14.4, -4.4), (4.0, 0.5, 0.8), "goud")])
        f.bot("miroh_schort", "body", (0, 6, 4), [
            f.k((-8.5, 0.2, 1.2), (17.0, 11.6, 5.6), "schort"),
            f.k((-1.6, 11.6, 3.0), (3.2, 1.4, 1.8), "schort")])
        kop = [f.k((2.4, 2.0, -16.2), (2.8, 2.5, 2.8), "kopje"), f.k((2.8, 4.3, -15.8), (2.0, 0.25, 2.0), "thee")]
    elif look == "gevangene":
        vod = (128, 122, 116)
        gewaad(f, [(0, 13, vod)])

        def gaten(vlak, X, Y, Z, zet):                                    # torn: holes and a ragged hem
            if vlak in ("east", "west", "south"):
                zet((Y < 2.2 + 1.3 * (np.sin(Z * 2.3 + X) > 0.1)), (96, 90, 86), 5)
                zet((np.hypot(Z - 3.0, Y - 6.0) < 1.3) | (np.hypot(Z - 8.0, Y - 4.4) < 1.0), (188, 150, 140), 6)
        f.verf("body", gaten)
        f.kleur("tin", (150, 156, 164), 8, rand((104, 110, 120), 3))
        f.kleur("thee", (120, 104, 84), 4)
        f.bot("miroh_knot", "head", (0, 14, -5), [                         # his hair has come loose
            f.k((-4.6, 13.8, -5.0), (9.2, 1.0, 3.6), "baard"), f.k((2.0, 14.6, -4.0), (2.6, 1.0, 2.0), "baard"),
            f.k((-7.6, 8.0, -1.6), (15.2, 5.0, 1.6), "baard")])
        kop = [f.k((2.5, 2.0, -16.1), (2.6, 2.9, 2.6), "tin"), f.k((5.1, 2.6, -15.3), (0.9, 1.7, 1.0), "tin"),
               f.k((2.9, 4.7, -15.7), (1.8, 0.2, 1.8), "thee")]
    else:
        blauw, wit = (62, 104, 176), (244, 244, 248)
        gewaad(f, [(0, 2.4, blauw), (2.4, 13, wit)])

        def bloem(vlak, X, Y, Z, zet):                                    # the lotus on both flanks and on his back
            if vlak in ("east", "west"):
                r, a = np.hypot(Z - 5.0, Y - 6.2), np.arctan2(Y - 6.2, Z - 5.0)
                zet(r < 3.6, blauw, 4)
                zet((r < 1.4 + 1.6 * np.abs(np.cos(a * 4))) & (r > 0.8), wit, 3)
                zet(r <= 0.8, goud, 3)
        f.verf("body", bloem)
        f.kleur("kraag", blauw, 6, rand(wit, 4))
        f.kleur("kopje", (246, 244, 236), 4, strepen((70, 130, 170), 32, 5))
        f.kleur("thee", (168, 128, 54), 4)
        f.bot("miroh_knot", "head", (0, 14, -5), [
            f.k((-1.6, 14.0, -5.6), (3.2, 2.0, 3.2), "baard"),
            f.k((-2.0, 14.4, -4.4), (4.0, 0.5, 0.8), "goud")])
        f.bot("miroh_kraag", "body", (0, 6, 0), [                          # the high collar of the Witte Lotus
            f.k((-9.4, 0.2, -3.0), (18.8, 12.6, 2.6), "kraag"),
            f.k((-9.0, 6.2, -0.6), (18.0, 5.4, 6.0), "kraag")])
        f.bot("miroh_lotus", "root", (-7.5, 0.4, -16), [f.k((-9.4, 0.0, -17.8), (3.8, 0.8, 3.8), "tegel")])
        kop = [f.k((2.4, 2.0, -16.2), (2.8, 2.5, 2.8), "kopje"), f.k((2.8, 4.3, -15.8), (2.0, 0.25, 2.0), "thee")]
    f.bot("miroh_kopje", "leg_front_left", (4, 2, -14), kop)
    f.bot("miroh_stoom", "miroh_kopje", (3.8, 5, -14.8), [] if wild else [
        f.k((3.4, 5.2, -15.2), (0.8, 1.4, 0.8), "stoom"), f.k((4.0, 7.0, -14.8), (0.7, 1.2, 0.7), "stoom"),
        f.k((3.3, 8.6, -15.0), (0.6, 1.0, 0.6), "stoom")])
    lege_botten(f, MIROH_BOTTEN)
    f.bewaar()


# =====================================================================================================================
# Suki-guh: the Kyoshi warrior, and the prisoner of Het Kokend Rotsblok
# =====================================================================================================================
SUKI_BOTTEN = ("suki_kroon", "suki_waaier_links", "suki_waaier_rechts", "suki_harnas", "suki_haar")
ROODBRUIN = (124, 62, 42)


def _waaier(block):
    """A golden fan: ribs fanning out from the grip, a green rim."""
    yy, xx = np.mgrid[0:32, 0:32]
    block[..., :3] = (240, 198, 76)
    a = np.arctan2(31.5 - yy, xx - 15.5)
    block[(np.abs(np.sin(a * 9)) < 0.22), :3] = (190, 140, 44)
    block[:4, :, :3] = (70, 130, 84)


def suki(h, kind="suki_guh", look="krijger"):
    f = ck.Figuur(h, kind, "zit", (0.06, 0.40, 1.06))
    bewaard = _vacht_masker(f)
    goud = (240, 198, 76)
    f.kleur("haar", ROODBRUIN, 9)
    if look == "krijger":
        groen, diep = (52, 104, 70), (34, 70, 50)
        gewaad(f, [(0, 4.4, diep), (4.4, 14, groen)])
        mouwen(f, groen, goud)
        haarkap(f, ROODBRUIN, voor=23.9, zij=lambda Z: np.where(Z > -2.5, 15.0, 22.2), achter=13.0)

        # the white face paint, red round the eyes, a red mouth
        def schmink(vlak, X, Y, Z, zet):
            if vlak == "north":
                zet(Y < 23.9, (250, 248, 246), 3)
            elif vlak in ("east", "west"):
                zet((Z < -2.5) & (Y < 22.2), (250, 248, 246), 3)
            elif vlak == "down":
                zet(Z < -2.5, (250, 248, 246), 3)
        f.verf("head", schmink)
        px, W, H = f.gezicht()
        oog = ~bewaard[0]
        oog[25:, :] = False
        yy, xx = np.mgrid[0:H, 0:W]
        rood = np.zeros_like(oog)
        for cx in (12, 44):                                              # a red wing round and beyond each eye
            sx = -1 if cx < 28 else 1
            rood |= np.hypot((xx - cx) / 13.5, (yy - 12.5) / 12.5) <= 1
            rood |= (np.abs(yy - (9 - (xx - cx) * sx * 0.55)) <= 2.6) & ((xx - cx) * sx > 6) & ((xx - cx) * sx < 15)
        px[rood, :3] = (206, 44, 48)
        _ogen_terug(f, bewaard)
        mond = (~bewaard[0]) & (np.arange(H)[:, None] >= 25)
        px[mond, :3] = (196, 36, 44)
        f.kleur("goud", goud, 6)
        f.kleur("kroon", goud, 6, samen(rand((190, 140, 44), 3), stip((206, 44, 48), 6)))
        f.kleur("kwast", (246, 214, 110), 5, strepen((206, 160, 60), 6, 2, True))
        f.kleur("plaat", (40, 60, 48), 6, samen(strepen((28, 44, 36), 8, 2), rand(goud, 2)))
        f.kleur("waaier", goud, 4, _waaier)
        f.kleur("gordel", (196, 170, 80), 6)
        f.bot("suki_kroon", "head", (0, 24, -6), [
            f.k((-6.6, 23.6, -7.7), (13.2, 1.5, 0.9), "goud"),
            f.k((-2.8, 24.6, -7.9), (5.6, 3.6, 0.9), "kroon"),
            f.k((-1.6, 28.2, -7.9), (3.2, 1.3, 0.9), "goud"),
            f.k((-7.4, 18.4, -7.5), (1.1, 5.2, 1.1), "kwast"), f.k((6.3, 18.4, -7.5), (1.1, 5.2, 1.1), "kwast")])
        f.bot("suki_haar", "head", (0, 20, 0), paar(f.k((7.95, 15.0, -2.4), (0.8, 8.0, 6.0), "haar")) + [
            f.k((-6.8, 13.0, 5.95), (13.6, 10.0, 0.8), "haar")])
        f.bot("suki_harnas", "body", (0, 8, 0), [
            f.k((-4.7, 6.2, -4.7), (9.4, 5.2, 1.0), "plaat"),
            f.k((-5.4, 4.3, -4.5), (10.8, 1.7, 9.0), "gordel")] + paar(f.k((4.6, 9.6, -3.4), (2.6, 2.6, 6.8), "plaat")))
        # the fans, open: one in each paw, out to the sides
        for naam, sx, arm in (("suki_waaier_links", 1, "arm_left"), ("suki_waaier_rechts", -1, "arm_right")):
            rij = [((4.6, 8.6), 1.6), ((3.8, 10.2), 2.2), ((3.0, 12.4), 2.4), ((3.6, 14.8), 1.5)]
            breed = [1.6, 5.4, 7.4, 6.0]
            kub = []
            for ((x, y), hh), b in zip(rij, breed):
                x0 = 5.2 - b * 0.35 + 1.4
                kub.append(f.k((x0 if sx > 0 else -(x0 + b), y, -7.5), (b, hh, 0.45), "waaier"))
            f.bot(naam, arm, (5.4 * sx, 9, -7.2), kub)
    else:
        kleed, don = (156, 78, 56), (116, 54, 42)
        gewaad(f, [(0, 3.4, don), (3.4, 11.0, kleed), (11.0, 14, don)])
        mouwen(f, kleed, don)
        haarkap(f, ROODBRUIN, voor=lambda X: 23.6 - 0.8 * (np.abs(X) > 5.0), zij=lambda Z: np.where(Z > -3.0, 14.5, 21.6), achter=13.0)
        _ogen_terug(f, bewaard)
        # her hair down: it hangs to her shoulders
        f.bot("suki_haar", "head", (0, 20, 0), paar(
            f.k((7.95, 12.6, -3.2), (0.9, 10.6, 7.6), "haar"),
            f.k((6.2, 13.4, -7.5), (1.6, 8.4, 0.8), "haar")) + [
            f.k((-7.0, 11.6, 5.95), (14.0, 12.0, 0.9), "haar"),
            f.k((-6.6, 22.4, -7.5), (4.4, 1.9, 0.7), "haar"), f.k((2.2, 22.4, -7.5), (4.4, 1.9, 0.7), "haar")])
    lege_botten(f, SUKI_BOTTEN)
    f.bewaar()


# =====================================================================================================================
# prinses Yue-guh, and the moon spirit
# =====================================================================================================================
YUE_BOTTEN = ("yue_haar", "yue_kraag", "yue_zoom", "yue_jurk", "yue_maan")


def _maan(block):
    yy, xx = np.mgrid[0:32, 0:32]
    vol = np.hypot(xx - 15.5, yy - 15.5) <= 13
    hap = np.hypot(xx - 21.5, yy - 12.5) <= 11.5
    block[..., 3] = 0
    block[vol & ~hap, :3] = (255, 250, 214)
    block[vol & ~hap, 3] = 255


def yue(h, kind="yue_guh", look="prinses"):
    geest = look == "maan"
    f = ck.Figuur(h, kind, "zit", (0.60, 0.20, 1.10) if geest else (0.07, 0.95, 0.90))
    bewaard = _vacht_masker(f)
    wit = (250, 250, 255)
    paars, licht = ((214, 230, 250), (244, 248, 255)) if geest else ((142, 110, 196), (238, 234, 246))
    sier = (150, 200, 250) if geest else (110, 84, 176)

    def voor(X, Y, zet):
        zet((np.abs(X) <= 1.3) & (Y > 3.6) & (Y < 12.6), licht, 4, geest)
    gewaad(f, [(0, 14, paars)], voor=voor)
    mouwen(f, paars, licht)
    haarkap(f, wit, voor=lambda X: 23.8 - 0.8 * (np.abs(X) < 0.6), zij=lambda Z: np.where(Z > -2.0, 15.5, 22.4), achter=12.5, var=4)
    _ogen_terug(f, bewaard)
    f.kleur("haar", wit, 4, strepen((226, 232, 246), 7, 2, True), gloed=geest)
    f.kleur("sier", sier, 5, gloed=geest)
    f.kleur("bont", licht, 4, strepen((218, 216, 236), 6, 2, True), gloed=geest)
    f.kleur("jurk", paars, 5, strepen(licht, 16, 3), gloed=geest)
    f.bot("yue_haar", "head", (0, 24, 0), paar(
        f.k((6.3, 14.6, -7.6), (1.3, 8.8, 1.3), "haar"),                       # the two loops beside her face
        f.k((8.0, 14.6, -6.4), (1.2, 8.8, 1.3), "haar"),
        f.k((6.3, 13.6, -7.6), (2.9, 1.2, 2.5), "haar"),
        f.k((6.0, 19.6, -7.9), (1.9, 1.3, 1.9), "sier"),
        f.k((7.7, 17.2, -6.7), (1.8, 1.3, 1.9), "sier")) + [
        f.k((-2.6, 25.8, -2.6), (5.2, 3.4, 5.2), "haar"),                       # the knot on top, with its ornament
        f.k((-2.9, 26.3, -2.9), (5.8, 1.0, 5.8), "sier"),
        f.k((-1.6, 29.2, -1.6), (3.2, 1.6, 3.2), "haar"),
        f.k((-5.2, 9.6, 5.95), (10.4, 13.6, 1.3), "haar")])                     # long, down her back
    f.bot("yue_kraag", "body", (0, 12, 0), [f.k((-5.9, 11.0, -5.0), (11.8, 2.2, 10.0), "bont")])
    f.bot("yue_zoom", "body", (0, 3, 0), [f.k((-5.6, 2.2, -4.6), (11.2, 1.8, 9.2), "bont")])
    if geest:
        f.kleur("maan", (255, 250, 214), 3, _maan, gloed=True)
        f.bot("yue_jurk", "body", (0, 2, 0), [                                # the long dress, flowing out wide
            f.k((-6.4, 2.0, -5.2), (12.8, 2.6, 10.4), "jurk"),
            f.k((-7.4, 0.2, -6.0), (14.8, 2.0, 13.0), "jurk"),
            f.k((-6.2, 0.2, 7.0), (12.4, 1.2, 3.4), "bont")])
        f.bot("yue_maan", "head", (0, 31, 0), [f.k((-2.4, 30.9, -0.4), (4.8, 4.8, 0.5), "maan")])
        f.doorzichtig(172)
    lege_botten(f, YUE_BOTTEN)
    f.bewaar(gloed=geest)


# =====================================================================================================================
# the five of the Leegtehaven and the ringenrace
# =====================================================================================================================
def _ruit(a, b):
    def p(block):
        block[..., :3] = a
        for y in range(4):
            for x in range(4):
                if (x + y) % 2:
                    block[y * 8:(y + 1) * 8, x * 8:(x + 1) * 8, :3] = b
    return p


def veerguh(h, kind="veerguh"):
    f = ck.Figuur(h, kind, "zit", (0.93, 0.70, 1.02))
    marine, wit = (40, 62, 120), (246, 246, 248)
    gewaad(f, [(y, y + 1.2, marine if i % 2 else wit) for i, y in enumerate(np.arange(0.4, 14, 1.2))])
    mouwen(f, wit, marine)
    f.kleur("marine", marine, 5)
    f.kleur("wit", wit, 4)
    f.kleur("touw", (206, 172, 112), 8, strepen((160, 126, 76), 6, 3))
    f.kleur("ijzer", (160, 166, 176), 7)
    f.kleur("rood", (200, 56, 52), 5)
    f.kleur("kaartje", (250, 236, 170), 4, samen(rand((200, 150, 60), 3), stip((60, 50, 40), 3, 24, 16)))
    f.bot("veerguh_pet", "head", (0, 26, 0), [
        f.k((-4.8, 25.2, -5.2), (9.6, 1.5, 9.0), "marine"),
        f.k((-5.8, 26.6, -6.2), (11.6, 1.9, 11.0), "wit"),
        f.k((-0.9, 28.5, -1.6), (1.8, 0.9, 1.8), "marine"),
        f.k((-1.2, 25.5, -5.6), (2.4, 1.0, 0.5), "kaartje")])
    f.bot("veerguh_touw", "body", (5, 9, 0), [                                 # a coil of rope over his left shoulder
        f.k((3.9, 4.6, -5.6), (1.5, 9.4, 1.3), "touw"), f.k((3.9, 4.6, 4.3), (1.5, 9.4, 1.3), "touw"),
        f.k((3.9, 12.7, -5.6), (1.5, 1.3, 11.2), "touw"), f.k((3.9, 3.4, -5.6), (1.5, 1.3, 11.2), "touw"),
        f.k((5.5, 5.4, -5.3), (1.3, 8.2, 1.2), "touw"), f.k((5.5, 5.4, 4.1), (1.3, 8.2, 1.2), "touw"),
        f.k((5.5, 12.4, -5.3), (1.3, 1.2, 10.6), "touw")])
    f.bot("veerguh_tang", "arm_right", (-3, 9, -7), [                           # the ticket punch
        f.k((-4.0, 9.2, -8.8), (1.6, 0.8, 2.6), "ijzer"), f.k((-4.0, 8.0, -8.8), (1.6, 0.8, 1.2), "ijzer"),
        f.k((-4.0, 8.2, -7.0), (1.6, 0.9, 1.6), "rood")])
    f.bot("veerguh_kaartje", "arm_left", (3, 9, -7), [f.k((1.6, 8.3, -7.5), (2.8, 1.8, 0.35), "kaartje")])
    f.bewaar()


def scheepsbouwer(h, kind="scheepsbouwerguh"):
    f = ck.Figuur(h, kind, "zit", (0.02, 0.85, 0.94))
    leer = (150, 100, 60)
    gewaad(f, [(0, 14, (222, 214, 196))])
    mouwen(f, (222, 214, 196), (190, 180, 160))

    def zaagsel(vlak, X, Y, Z, zet):                                           # sawdust on the fur
        zet(f.rng.random(X.shape) < 0.035, (236, 206, 150), 6)
    f.verf(("head", "ear_left", "ear_right", "arm_left", "arm_right", "foot_left", "foot_right"), zaagsel)
    f.kleur("leer", leer, 8, samen(rand(donker(leer, 0.74), 2), strepen(donker(leer, 0.9), 16, 2)))
    f.kleur("zak", donker(leer, 0.8), 6, rand(donker(leer, 0.6), 3))
    f.kleur("band", donker(leer, 0.7), 5)
    f.kleur("hout", (200, 160, 104), 8, strepen((166, 126, 76), 7, 2))
    f.kleur("steel", (140, 96, 58), 7)
    f.kleur("potlood", (246, 204, 60), 4)
    f.kleur("punt", (50, 44, 40), 3)
    f.bot("scheepsbouwerguh_schort", "body", (0, 8, -4), [
        f.k((-4.6, 2.4, -4.9), (9.2, 8.6, 1.0), "leer"),
        f.k((-2.4, 3.6, -5.2), (4.8, 2.8, 0.5), "zak"),
        f.k((-5.3, 6.2, -4.4), (10.6, 1.1, 8.8), "band"),
        f.k((-3.4, 10.8, -4.6), (1.0, 2.0, 9.0), "band"), f.k((2.4, 10.8, -4.6), (1.0, 2.0, 9.0), "band")])
    f.bot("scheepsbouwerguh_hamer", "arm_right", (-3, 9, -7), [
        f.k((-3.5, 7.4, -7.6), (1.0, 8.2, 1.0), "steel"),
        f.k((-5.2, 15.0, -8.4), (4.4, 2.8, 2.6), "hout")])
    f.bot("scheepsbouwerguh_potlood", "head", (7.4, 24, -2), [
        f.k((7.0, 23.6, -5.2), (0.8, 0.8, 6.4), "potlood"),
        f.k((7.1, 23.7, -6.0), (0.6, 0.6, 0.8), "punt")])
    f.bewaar()


def kroegbaas(h, kind="kroegbaasguh"):
    f = ck.Figuur(h, kind, "zit", (0.97, 0.80, 1.0))
    f.breder("body", 1.22, 1.0, 1.25, om=(0, 0, 0))                           # big and round
    hemd, schort = (240, 236, 226), (96, 66, 50)
    gewaad(f, [(0, 7.6, schort), (7.6, 14, hemd)])
    mouwen(f, hemd)
    f.kleur("doek", (248, 248, 246), 4, strepen((206, 70, 64), 12, 3))
    f.kleur("hout", (160, 108, 62), 8, strepen((70, 60, 56), 13, 3))
    f.kleur("schuim", (252, 250, 240), 5)
    f.kleur("snor", (84, 52, 38), 7)
    f.kleur("band", donker(schort, 0.7), 5)
    f.bot("kroegbaasguh_doek", "body", (4.4, 12, 0), [                         # the towel over his shoulder
        f.k((2.8, 7.4, -6.2), (3.0, 6.0, 0.9), "doek"),
        f.k((2.8, 12.5, -6.2), (3.0, 0.9, 11.8), "doek"),
        f.k((2.8, 8.4, 4.7), (3.0, 5.0, 0.9), "doek")])
    f.bot("kroegbaasguh_pul", "arm_right", (-3.4, 9, -8), [
        f.k((-5.0, 7.4, -10.0), (3.2, 4.2, 3.2), "hout"),
        f.k((-5.2, 11.6, -10.2), (3.6, 1.1, 3.6), "schuim"),
        f.k((-5.9, 8.4, -8.9), (0.9, 2.4, 1.0), "hout")])
    f.bot("kroegbaasguh_snor", "head", (0, 17, -8), paar(
        f.k((0.4, 15.4, -8.2), (3.4, 1.3, 0.7), "snor"),
        f.k((3.4, 14.6, -8.1), (1.3, 1.3, 0.7), "snor")))
    f.bot("kroegbaasguh_band", "body", (0, 8, 0), [f.k((-6.3, 7.2, -5.2), (12.6, 1.0, 10.4), "band")])
    f.bewaar()


def kapitein(h, kind="kapiteinguh"):
    f = ck.Figuur(h, kind, "zit", (0.96, 0.34, 0.97))
    bewaard = _vacht_masker(f)
    marine, goud, wit = (38, 52, 92), (232, 190, 70), (244, 244, 246)

    def voor(X, Y, zet):                                                       # two rows of golden buttons
        for y in (4.4, 6.8, 9.2):
            zet(np.hypot(np.abs(X) - 1.6, Y - y) <= 0.55, goud, 3)
    gewaad(f, [(0, 14, marine)], voor=voor)
    mouwen(f, marine, goud)
    # one eye squeezed shut: fur over it, and a little arc
    px, W, H = f.gezicht()
    vacht = np.median(bewaard[1][..., :3][bewaard[0]].reshape(-1, 3), axis=0)
    dicht = (~bewaard[0]) & (np.arange(W)[None, :] >= 28) & (np.arange(H)[:, None] < 25)
    px[dicht, :3] = np.clip(vacht + f.rng.normal(0, 2, (int(dicht.sum()), 1)), 0, 255).astype(np.uint8)
    yy, xx = np.mgrid[0:H, 0:W]
    boog = (np.abs(yy - (15 - 5.0 * np.sqrt(np.clip(1 - ((xx - 43.5) / 10.0) ** 2, 0, 1)))) <= 1.1) & (np.abs(xx - 43.5) <= 10)
    px[boog, :3] = (52, 40, 60)
    f.kleur("baard", wit, 6, strepen((214, 216, 224), 5, 2, True))
    f.kleur("marine", marine, 8, stip(donker(marine, 1.25), 5, 9, 22))
    f.kleur("klep", (26, 26, 34), 4)
    f.kleur("goud", goud, 5)
    f.kleur("anker", marine, 4, samen(stip(goud, 10), stip(marine, 6), strepen(goud, 32, 3, True)))
    f.kleur("jas", marine, 6)
    f.bot("kapiteinguh_baard", "head", (0, 15, -7), [
        f.k((-5.0, 12.4, -7.9), (10.0, 3.6, 1.2), "baard"),
        f.k((-3.4, 10.6, -7.7), (6.8, 2.0, 1.1), "baard"),
        f.k((-1.8, 9.4, -7.5), (3.6, 1.4, 1.0), "baard")] + paar(
        f.k((6.3, 13.6, -7.7), (1.3, 6.0, 1.2), "baard"),
        f.k((2.4, 15.6, -8.2), (2.8, 1.2, 0.7), "baard")))                      # a white moustache
    f.bot("kapiteinguh_pet", "head", (0, 26, 0), [
        f.k((-5.4, 25.0, -6.2), (10.8, 1.7, 10.2), "marine"),
        f.k((-6.0, 26.6, -6.8), (12.0, 1.9, 11.2), "marine"),
        f.k((-4.6, 25.0, -9.0), (9.2, 0.6, 3.0), "klep"),
        f.k((-5.5, 26.2, -6.4), (11.0, 0.5, 0.5), "goud"),
        f.k((-1.2, 25.3, -6.7), (2.4, 2.6, 0.6), "anker")])
    f.bot("kapiteinguh_kraag", "body", (0, 12, 0), [f.k((-5.8, 10.6, -4.9), (11.6, 2.4, 9.8), "jas")])
    f.bewaar()


def racemeester(h, kind="ringenraceguh"):
    f = ck.Figuur(h, kind, "zit", (0.9, 0.55, 1.05))
    leer, vacht = (132, 86, 54), (240, 228, 200)
    gewaad(f, [(0, 3.4, vacht), (3.4, 14, leer)], voor=lambda X, Y, zet: zet((np.abs(X) <= 0.5) & (Y > 3.4), donker(leer, 0.7), 4))
    mouwen(f, leer, vacht)
    rood, wit = (214, 60, 56), (248, 246, 240)
    f.kleur("riem", (104, 70, 46), 6)
    f.kleur("bril", (190, 150, 70), 5, samen(stip((120, 196, 236), 11), stip((214, 240, 252), 4, 11, 11)))
    f.kleur("koper", (190, 150, 70), 6)
    f.kleur("sjaal", rood, 6, strepen(wit, 10, 5))
    f.kleur("franje", wit, 4, strepen(rood, 6, 2, True))
    f.kleur("stok", (120, 84, 56), 6)
    f.kleur("vlag", (250, 250, 250), 2, _ruit((250, 250, 250), (28, 28, 34)))
    f.bot("ringenraceguh_bril", "head", (0, 24, -6), [
        f.k((-7.3, 23.7, -7.4), (14.6, 1.2, 13.6), "riem"),
        f.k((-5.9, 23.3, -8.2), (4.8, 3.2, 1.5), "bril"), f.k((1.1, 23.3, -8.2), (4.8, 3.2, 1.5), "bril"),
        f.k((-1.2, 24.3, -7.9), (2.4, 1.0, 1.0), "koper")])
    f.bot("ringenraceguh_sjaal", "body", (0, 12, 0), [
        f.k((-5.7, 11.2, -4.9), (11.4, 2.3, 9.8), "sjaal"),
        f.k((4.4, 4.6, -5.4), (2.6, 7.0, 1.1), "sjaal"),
        f.k((4.4, 3.6, -5.5), (2.6, 1.1, 1.2), "franje")])
    f.bot("ringenraceguh_sjaal_eind", "ringenraceguh_sjaal", (5.6, 12, 4.5), [    # the long end, blown out behind him
        f.k((4.6, 10.8, 4.4), (2.4, 1.6, 6.0), "sjaal"),
        f.k((4.6, 9.8, 10.2), (2.4, 1.6, 5.0), "sjaal"),
        f.k((4.6, 9.7, 15.0), (2.4, 1.8, 1.2), "franje")])
    f.bot("ringenraceguh_vlag", "arm_right", (-3.2, 9, -7.2), [
        f.k((-3.6, 7.2, -7.7), (0.8, 16.0, 0.8), "stok"),
        f.k((-12.4, 17.2, -7.5), (8.8, 6.0, 0.4), "vlag")])
    f.bewaar()


# what draws every Kind of the slice: kind id -> (function, look)
TEKEN = {
    "aang_guh": (aang, None), "katara_guh": (katara, None), "sokka_guh": (sokka, None), "toph_guh": (toph, None),
    "zuka": (zuka, "ober"), "zuka_prins": (zuka, "prins"), "zuka_vuurheer": (zuka, "vuurheer"),
    "zuka_balling": (zuka, "balling"), "zuka_blauwe_geest": (zuka, "geest"), "zuka_team": (zuka, "team"),
    "oom_miroh": (miroh, "thee"), "oom_miroh_gevangene": (miroh, "gevangene"), "oom_miroh_lotus": (miroh, "lotus"),
    "suki_guh": (suki, "krijger"), "suki_guh_gevangene": (suki, "gevangene"),
    "yue_guh": (yue, "prinses"), "yue_guh_maan": (yue, "maan"),
    "veerguh": (veerguh, None), "scheepsbouwerguh": (scheepsbouwer, None), "kroegbaasguh": (kroegbaas, None),
    "kapiteinguh": (kapitein, None), "ringenraceguh": (racemeester, None),
}


def teken(h, kind):
    fn, look = TEKEN[kind]
    if look is None:
        fn(h, kind)
    else:
        fn(h, kind, look)
