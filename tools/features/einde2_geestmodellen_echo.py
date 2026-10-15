"""
einde2, slice geest_modellen (phase B) - the spirit characters of the Geestenwereld as GuhNpcEntity kinds (CONTRACT_E2_B 7.7):

  the echo-masters of the White Lotus  BUMI_GUH_ECHO, PAKGUH_ECHO, PIANDAO_GUH_ECHO (cast_volk's masters), OOM_MIROH_ECHO
                                      (cast_team's Miroh in his Lotus robe: "a dream of him", he still lives), and the NEW
                                      JEONG_JEONG_GUH_ECHO (the runaway fire master: wild white hair, a long beard)
  the old members of the order        LOTUS_STICHTER_ECHO (the founder from Roku-guh's time, a staff with a lotus) and
                                      LOTUS_LID_ECHO (mix 3: an earth, a water and a fire member)
  Guru Pathik-guh                     GURU_PATHIK_GUH (the chakra path; his juice bowl)
  earlier Guhvatars                   KURUK_GUH (water), YANGCHEN_GUH (air), SZETO_GUH (fire): spirits for h4's duel and the Top

All are drawn on the sitting guh with cast_volk's workbench (einde2_cast_volk_kern.Pop, the wardrobe einde2_cast_volk_delen),
or taken from the master's own drawing function (read-only, its writes caught by `_Vang`), and then get the ECHO finish:
see-through and softly glowing in a tint of their own (`geest`), a slow ring of mist at their feet (`<p>_nevel`), and the
masters a glowing Pai Sho tile with the guh symbol that they hold out when a trial is won (`<p>_geesttegel`). Guh in the
details: ears always, a lotus with ear-shaped petals on the tiles.

ECHO: kind -> row (prefix of the extra bones, drawing, tint, animations). Files per kind: guh_npc_<kind>.geo.json,
npc_<kind>.png + _glowmask, and its OWN animation file guh_npc_<kind>.animation.json (every echo is its own character).
"""
import copy
import math
import os

import numpy as np

from features import einde2_cast_team_anim as cta
from features import einde2_cast_team_figuren as ctf
from features import einde2_cast_volk_kern as kern
from features import einde2_cast_volk_modellen as cvm
from features import knuffeldal_npcs as kn
from features.einde2_cast_volk_delen import (A_BEIGE, A_DONKER, A_GEEL, A_GROEN, BONT, GEEL, GOUD, HAAR_BRUIN, HAAR_GRIJS,
                                             HAAR_WIT, HAAR_ZWART, HOUT, OUD, ORANJE, PEACH, TAN, V_DONKER, V_ROOD, W_BLAUW,
                                             W_DONKER, W_LICHT, WIT, baard, bontkraag, gewaad, haar, kegelhoed, knot,
                                             lang_haar, mantel, pijl, sik, snor, wenkbrauw)
from features.einde2_cast_volk_kern import Pop, _donker, op, rand, reeks, schud, spoor, stip, strepen, toon, vlam, zoom

R, L, HEAD, BODY, ROOT = "arm_right", "arm_left", "head", "body", "root"
LOTUS_BLAUW = (62, 104, 176)


# =====================================================================================================================
# swatch patterns of the echoes
# =====================================================================================================================
def guhtegel(rand_kleur):
    """The Pai Sho tile of a master: a pale disc, a white lotus whose two upper petals are guh ears, a golden heart; the
    rim in the master's colour."""
    def p(b):
        yy, xx = np.mgrid[0:32, 0:32]
        r, a = np.hypot(xx - 15.5, yy - 15.5), np.arctan2(yy - 15.5, xx - 15.5)
        b[..., :3] = (240, 234, 214)
        b[r > 13.0, :3] = rand_kleur
        blad = (r < 4.5 + 5.0 * np.abs(np.cos(a * 3))) & (r > 2.6)
        b[blad, :3] = (252, 252, 255)
        for sx in (-1, 1):                                              # the two ears on top of the flower
            oor = (np.abs(xx - 15.5 - sx * 5.0) < (yy - 3.0) * 0.45) & (yy > 3.0) & (yy < 10.5)
            b[oor, :3] = (252, 252, 255)
            b[oor & (np.abs(xx - 15.5 - sx * 5.0) < (yy - 5.5) * 0.22) & (yy > 6), :3] = (246, 170, 196)
        b[r <= 2.6, :3] = (244, 200, 80)
        b[r > 15.5, 3] = 0
    return p


def nevel(b):
    """The mist at an echo's feet: wisps with clear gaps."""
    yy, xx = np.mgrid[0:32, 0:32]
    golf = np.sin(xx / 3.2 + np.sin(yy / 4.0) * 1.6)
    b[..., :3] = (236, 244, 255)
    b[..., 3] = np.where(golf > 0.1, 150, np.where(golf > -0.4, 80, 0)).astype(np.uint8)


def schroeiplek(b):
    """Old burn marks: a few pale streaks on the arms of Jeong Jeong-guh (subtle; nobody gets hurt here)."""
    for y in (6, 16, 25):
        b[y:y + 2, 4:28, :3] = (214, 170, 160)


# =====================================================================================================================
# the echo finish (shared)
# =====================================================================================================================
class _Vang:
    """Wraps the generator namespace for a drawing function of another slice: what it would write (its model, its sheet)
    is caught here instead, so that nothing of that slice is written twice."""

    def __init__(self, h):
        self._h = h
        self.geo, self.png = None, None

    def __getattr__(self, naam):
        return getattr(self._h, naam)

    def w(self, pad, obj):
        if not str(pad).endswith(".geo.json"):
            raise SystemExit(f"einde2_geestmodellen_echo: unexpected write {pad}")
        self.geo = obj

    def save(self, img, *pad):
        if not pad[-1].endswith("_glowmask.png"):
            self.png = img


def _als_pop(h, kind, geo_file, a, vast):
    """A Pop over a model and sheet drawn elsewhere (deep copies): the workbench's c / k / bot / geest / klaar work on it."""
    P = Pop.__new__(Pop)
    P.h, P.kind, P.lijf = h, kind, "zit"
    _, _, P.dk, P.oog = kern.LIJVEN["zit"]
    P.geo_file = copy.deepcopy(geo_file)
    P.geo = P.geo_file["minecraft:geometry"][0]
    P.geo["description"]["identifier"] = f"geometry.guh_npc_{kind}"
    used = kn._used(P.geo)
    P.cellen = [(x, y) for y in range(0, kn.UV - kn.SW + 1, kn.SW) for x in range(0, kn.UV - kn.SW + 1, kn.SW)
                if not used[y:y + kn.SW, x:x + kn.SW].any()]
    P.sw = {}
    P.rng = kern.kit.rng(f"geestmodellen/echo/{kind}")
    P.a = np.asarray(a).copy()
    P.vast = vast.copy()
    x0, y0, x1, y1 = Pop._rechthoek(P.bone("head")["cubes"][2], "north")
    P.gezicht = (x0, y0, x1, y1)
    bx0, by0, bx1, by1 = Pop._rechthoek(P.bone("body")["cubes"][3], "east")
    P.huid = tuple(int(v) for v in np.median(P.a[by0:by1, bx0:bx1, :3].reshape(-1, 3), 0))
    P.mix, P.gloei = {}, None
    return P


def _van_volk(h, kind, teken):
    """An echo of a cast_volk master: its own drawing (caught), on a Pop of the echo."""
    v = _Vang(h)
    bron = teken(v)
    return _als_pop(h, kind, bron.geo_file, bron.a, bron.vast)


def _van_miroh(h, kind):
    """The dream of Oom Miroh: cast_team's Miroh in the robe of the White Lotus (caught); his eyes and mouth are what differs
    most from the fur on the face."""
    v = _Vang(h)
    ctf.miroh(v, kind=kind, look="lotus")
    a = np.asarray(v.png.convert("RGBA")).copy()
    geo = v.geo["minecraft:geometry"][0]
    head = next(b for b in geo["bones"] if b["name"] == "head")
    x0, y0, x1, y1 = Pop._rechthoek(head["cubes"][2], "north")
    vlak = a[y0:y1, x0:x1, :3].astype(np.float32)
    mediaan = np.median(vlak.reshape(-1, 3), 0)
    vast = np.zeros(a.shape[:2], bool)
    vast[y0:y1, x0:x1] = np.abs(vlak - mediaan).sum(-1) > 150
    return _als_pop(h, kind, v.geo, a, vast)


def echo_af(P, rij):
    """The finish: the mist ring, the tile (a master), see-through + glow in the echo's tint, the props that glow bright."""
    p = rij["p"]
    c = P.c
    nv = P.kl((236, 244, 255), nevel, var=0)
    P.bot(f"{p}_nevel", ROOT, [0, 0.6, 0], [kn._cube([-8.5, 0.1, -8.5], [17, 0.6, 17], nv), kn._cube([-6.5, 1.0, -6.5], [13, 0.5, 13], nv),
                                            kn._cube([-10.0, 0.0, -4.0], [3.0, 0.4, 8.0], nv), kn._cube([7.0, 0.0, -4.0], [3.0, 0.4, 8.0], nv)])
    helder = []
    if rij.get("tegel"):
        t = P.kl((240, 234, 214), guhtegel(rij["tegel"]), var=0)
        P.bot(f"{p}_geesttegel", R, [-3, 10, -6.5], [kn._cube([-4.9, 11.2, -9.2], [3.6, 3.6, 0.5], t)])
        helder.append((t, 1.0))
    for rgb, patroon in rij.get("helder", ()):
        helder.append((P.kl(rgb, patroon), 1.0))
    helder.append((nv, 0.55))
    # what keeps its own colour and alpha and glows: the tile, the flames, the glowing eyes (full) and the mist (soft)
    S = kn.SW * kern.PX
    bewaard = [((x, y), P.a[y * kern.PX:y * kern.PX + S, x * kern.PX:x * kern.PX + S].copy(), f) for (x, y), f in helder]
    tint, mate, alfa = rij["geest"]
    P.geest(tint=tint, mate=mate, alfa=alfa, gloed=0.46)
    for (x, y), blok, f in bewaard:
        sy, sx = y * kern.PX, x * kern.PX
        P.a[sy:sy + S, sx:sx + S] = blok
        g = P.gloei[sy:sy + S, sx:sx + S]
        g[..., :3] = np.clip(blok[..., :3].astype(np.float32) * f, 0, 255).astype(np.uint8)
        g[..., 3] = np.where(blok[..., 3] > 0, 255, 0)
    return P


def staatogen(P, p):
    """The glowing white eyes of the Guhvatar state: two plates over the eyes, shown only in `staat`."""
    wit = (250, 252, 255)
    P.kbot(f"{p}_staatogen", [P.k([1.4, 18.8, -7.95], [5.6, 5.4, 0.3], wit), P.k([-7.0, 18.8, -7.95], [5.6, 5.4, 0.3], wit)])
    return (wit,)


# =====================================================================================================================
# the characters
# =====================================================================================================================
def jeong_jeong(h, kind):
    """The runaway fire master: grey-white wild hair down his back, a long beard and drooping moustache, dark worn robes
    without sleeves, old burn marks on his arms, the stern brows of someone who fears his own fire."""
    P = Pop(h, kind, "zit", *OUD)
    c, k = P.c, P.k
    ROBE, ZOOM = (96, 54, 44), (60, 34, 30)
    gewaad(P, ROBE, zoom_k=ZOOM, riem=(128, 92, 60), kraag=ZOOM, hals=_donker(ROBE, 0.8), mouw=P.huid)
    P.verf("arm_left", (214, 170, 160), lambda x, y, z, f: (f in ("up", "east", "west")) & (np.abs(y - 8.5) < 0.5))
    haar(P, HAAR_WIT, voor=24.0, zij=19.5, achter=8.0)
    wenkbrauw(P, HAAR_WIT, binnen=-1.1, buiten=0.4, dikte=0.7)
    # wild hair: long down the back and sticking out at the sides
    P.kbot("jeong_haar", lang_haar(P, HAAR_WIT, tot=5.0, breed=13.0)
           + [k([7.2, 14.0, -3.5], [2.2, 9.0, 8.0], HAAR_WIT), k([8.8, 16.0, -1.5], [1.4, 4.0, 4.0], HAAR_GRIJS),
              k([-9.4, 14.0, -3.5], [2.2, 9.0, 8.0], HAAR_WIT), k([-10.2, 16.0, -1.5], [1.4, 4.0, 4.0], HAAR_GRIJS),
              k([-6.0, 24.6, -6.5], [12.0, 1.0, 12.0], HAAR_WIT), k([-3.5, 25.4, -3.0], [3.0, 1.2, 4.0], HAAR_GRIJS),
              k([1.5, 25.4, -1.0], [3.0, 1.0, 3.5], HAAR_GRIJS)])
    P.kbot("jeong_baard", baard(P, HAAR_WIT, lengte=9.0, breed=7.4) + snor(P, HAAR_WIT, hang=7.0, breed=6.0, dik=0.8))
    P.bot("jeong_band", BODY, [0, 5, 0], [c([-6.0, 4.6, -4.8], [12.0, 1.2, 9.6], (128, 92, 60)), c([2.0, 1.6, -5.0], [1.2, 3.2, 0.6], (128, 92, 60))])
    # the flame he holds so carefully (only in "kaars")
    P.bot("jeong_vlam", L, [3, 10, -7], [c([2.0, 10.6, -8.0], [2.0, 2.6, 2.0], (255, 150, 60), patroon=vlam((255, 120, 40), (255, 236, 150))),
                                        c([2.4, 13.0, -7.6], [1.2, 1.6, 1.2], (255, 220, 120))])
    return P


def pathik(h, kind):
    """Guru Pathik-guh: a tiny, very old guh, all bones and kindness, a long white beard and wild brows, a simple ochre
    cloth, a string of wooden beads, a dot on his forehead; his bowl of onion-and-banana juice (only in "sap")."""
    P = Pop(h, kind, "zit", 0.08, 0.6, 1.0)
    c, k = P.c, P.k
    DOEK = (214, 150, 80)
    P.verf("body", DOEK, lambda x, y, z, f: y < 6.5)
    P.verf("body", DOEK, lambda x, y, z, f: (f == "north") & (np.abs(x + (y - 6.5) * 0.7) < 1.6))
    P.verf(HEAD, (214, 96, 60), lambda x, y, z, f: (f == "north") & (np.hypot(x, y - 23.6) < 0.6), over=True)
    wenkbrauw(P, HAAR_WIT, binnen=0.8, buiten=-0.4, dikte=0.8)
    P.kbot("pathik_wenkbrauw", [k([1.6, 23.6, -7.9], [5.0, 0.8, 0.7], HAAR_WIT), k([-6.6, 23.6, -7.9], [5.0, 0.8, 0.7], HAAR_WIT),
                                k([6.2, 22.6, -7.9], [1.6, 1.2, 0.6], HAAR_WIT), k([-7.8, 22.6, -7.9], [1.6, 1.2, 0.6], HAAR_WIT)])
    P.kbot("pathik_baard", baard(P, HAAR_WIT, lengte=11.0, breed=6.4) + snor(P, HAAR_WIT, hang=3.0, breed=5.4, dik=0.6))
    kraal = (150, 104, 62)
    P.bot("pathik_kralen", BODY, [0, 12, 0], [c([-5.4, 11.2, -4.6], [10.8, 0.7, 9.2], kraal, patroon=stip(_donker(kraal, 0.7), 4.0)),
                                             c([-1.0, 8.8, -4.8], [2.0, 2.0, 0.6], kraal)])
    P.bot("pathik_kom", L, [3, 10, -7], [c([1.4, 10.0, -8.6], [3.4, 2.2, 3.4], (120, 84, 50), patroon=zoom((90, 60, 36), 6)),
                                        c([1.8, 12.0, -8.2], [2.6, 0.3, 2.6], (236, 210, 120)), c([3.6, 12.0, -7.4], [0.4, 3.2, 0.4], (240, 236, 220))])
    return P


def kuruk(h, kind):
    """Kuruk-guh, the Guhvatar of the Water Tribe before Aang-guh: tall, long dark hair with a wolf-tail, a short beard, a
    blue parka with white fur; water swirls round him (only in "water")."""
    P = Pop(h, kind, "zit", *TAN)
    c, k = P.c, P.k
    gewaad(P, W_BLAUW, rand_k=BONT, zoom_k=BONT, riem=W_DONKER, kraag=W_DONKER)
    haar(P, HAAR_ZWART, voor=24.8, zij=22.0, achter=11.0)
    wenkbrauw(P, HAAR_ZWART, binnen=-0.3, buiten=0.5, dikte=0.55)
    P.kbot("kuruk_haar", lang_haar(P, HAAR_ZWART, tot=8.0, breed=12.0) + knot(P, HAAR_ZWART, band=W_LICHT, y=24.5, z=3.5, b=3.2, hh=3.0)
           + [k([7.0, 15.0, -5.0], [1.2, 7.0, 1.2], HAAR_ZWART), k([-8.2, 15.0, -5.0], [1.2, 7.0, 1.2], HAAR_ZWART)])
    P.kbot("kuruk_baard", sik(P, HAAR_ZWART, 3.4, 2.4) + snor(P, HAAR_ZWART, hang=1.2, breed=5.0, dik=0.6))
    P.bot("kuruk_kraag", BODY, [0, 12, 0], bontkraag(P, BONT, 2.6))
    P.bot("kuruk_water", ROOT, [0, 9, 0], [c([-9, 9.4, -9], [6, 1.2, 1.2], (120, 190, 240)), c([-3, 10.6, -11], [6, 1.2, 1.2], (150, 210, 250)),
                                          c([3, 9.6, -9.5], [6, 1.2, 1.2], (120, 190, 240)), c([8.5, 10.8, -7], [2, 1.6, 1.6], (200, 235, 255)),
                                          c([-10.5, 12.5, -2], [1.6, 1.6, 6], (150, 210, 250))])
    return P


def yangchen(h, kind):
    """Yangchen-guh, the air nomad Guhvatar: the blue arrow over her head, a yellow and orange robe, long prayer beads with
    a wooden pendant; a swirl of wind (only in "lucht")."""
    P = Pop(h, kind, "zit", *PEACH)
    c, k = P.c, P.k
    gewaad(P, GEEL, rand_k=ORANJE, zoom_k=ORANJE, riem=ORANJE)
    P.verf(BODY, ORANJE, lambda x, y, z, f: (f in ("north", "south")) & (y > 6.0 + (x * (1 if f == "north" else -1) + 5) * 0.62))
    pijl(P)
    wenkbrauw(P, (60, 44, 40), binnen=0.2, buiten=0.6, dikte=0.4)
    kraal = (124, 82, 50)
    P.bot("yangchen_kralen", BODY, [0, 12, 0], [c([-5.5, 11.0, -4.7], [11.0, 0.8, 9.4], kraal, patroon=stip((200, 160, 90), 4.0)),
                                               c([-0.5, 5.0, -4.9], [1.0, 6.0, 0.5], kraal), c([-1.4, 3.2, -5.1], [2.8, 2.2, 0.7], (196, 140, 80))])
    P.bot("yangchen_wind", ROOT, [-3, 11.5, -9], [c([-5.4, 10.2, -11], [3.4, 0.5, 0.5], (226, 242, 255)), c([-3.6, 11.8, -12], [2.8, 0.5, 0.5], (226, 242, 255)),
                                                 c([-5.0, 13.2, -10.5], [2.4, 0.5, 0.5], (226, 242, 255)), c([2.0, 11.0, -11], [3.0, 0.5, 0.5], (226, 242, 255))])
    return P


def szeto(h, kind):
    """Szeto-guh, the Fire Nation Guhvatar who was a counsellor: deep red robes with gold, a pointed shoulder mantle, the
    flame crown of an official on his topknot, a neat long moustache and a long chin beard, a scroll under his arm."""
    P = Pop(h, kind, "zit", *TAN)
    c, k = P.c, P.k
    gewaad(P, V_ROOD, rand_k=GOUD, kraag=GOUD, hals=V_DONKER, zoom_k=GOUD, riem=V_DONKER)
    haar(P, HAAR_ZWART, voor=24.9, zij=22.0, achter=14.0)
    wenkbrauw(P, HAAR_ZWART, binnen=0.0, buiten=0.4, dikte=0.5)
    P.kbot("szeto_kroon", knot(P, HAAR_ZWART, band=GOUD, z=0.0, b=3.2, hh=2.8)
           + [k([-1.7, 28.6, -0.6], [0.9, 2.6, 1.0], GOUD), k([0.8, 28.6, -0.6], [0.9, 2.6, 1.0], GOUD), k([-0.45, 29.2, -0.6], [0.9, 2.8, 1.0], GOUD),
              k([-6.2, 25.0, -6.2], [12.4, 0.6, 12.4], (60, 24, 30))])
    P.kbot("szeto_baard", sik(P, HAAR_ZWART, 5.2, 1.6) + snor(P, HAAR_ZWART, hang=4.4, breed=5.2, dik=0.5))
    P.bot("szeto_mantel", BODY, [0, 12, 0], mantel(P, V_DONKER, patroon=rand(GOUD, 4)))
    P.bot("szeto_rol", L, [3, 9, -6], [c([1.2, 8.4, -9.4], [3.6, 1.4, 1.4], (236, 222, 186), patroon=strepen((180, 150, 110), 8, 1, liggend=False)),
                                      c([0.8, 8.3, -9.5], [0.5, 1.6, 1.6], V_DONKER), c([4.8, 8.3, -9.5], [0.5, 1.6, 1.6], V_DONKER)])
    P.bot("szeto_vlam", R, [-3, 10, -7], [c([-4.0, 10.4, -10.6], [2.0, 2.4, 3.0], (255, 150, 60), patroon=vlam((255, 120, 40), (255, 236, 150))),
                                         c([-3.6, 10.8, -12.6], [1.2, 1.6, 2.0], (255, 220, 120))])
    return P


def stichter(h, kind):
    """The founder of the White Lotus, from Roku-guh's time: an old guh in white and lotus blue, the lotus on his chest, a
    hood over his shoulders, a long beard, and a staff that ends in a stone lotus with two ear petals."""
    P = Pop(h, kind, "zit", *OUD)
    c, k = P.c, P.k
    gewaad(P, WIT, rand_k=LOTUS_BLAUW, kraag=LOTUS_BLAUW, hals=(226, 232, 244), zoom_k=LOTUS_BLAUW, riem=LOTUS_BLAUW)
    P.verf(BODY, LOTUS_BLAUW, lambda x, y, z, f: (f == "north") & (np.hypot(x, y - 8.6) < 1.9))
    P.verf(BODY, WIT, lambda x, y, z, f: (f == "north") & (np.hypot(x, y - 8.6) < 1.0))
    haar(P, HAAR_WIT, voor=99, zij=99, achter=16.0, vanaf_z=2.0)
    wenkbrauw(P, HAAR_WIT, binnen=0.6, buiten=0.2, dikte=0.55)
    P.kbot("stichter_baard", baard(P, HAAR_WIT, lengte=8.0, breed=7.0) + snor(P, HAAR_WIT, hang=2.4, breed=5.6, dik=0.7))
    P.kbot("stichter_kap", [k([-7.6, 13.2, 3.8], [15.2, 11.6, 3.2], LOTUS_BLAUW, patroon=zoom(WIT, 4)), k([-7.4, 24.4, -2.0], [14.8, 1.2, 9.0], LOTUS_BLAUW)])
    steen = (204, 206, 214)
    P.bot("stichter_staf", ROOT, [-8.5, 0, -3], [c([-8.95, 0, -3.45], [0.9, 25.0, 0.9], HOUT, patroon=strepen(_donker(HOUT, 0.8), 8, 2)),
                                                 c([-10.0, 25.0, -4.5], [3.0, 1.2, 3.0], steen), c([-10.6, 26.0, -5.1], [1.4, 2.4, 1.4], steen),
                                                 c([-8.2, 26.0, -5.1], [1.4, 2.4, 1.4], steen), c([-9.4, 26.2, -3.9], [1.2, 2.0, 1.2], (244, 200, 80))])
    return P


def lotus_lid(h, kind):
    """A member of the order, from every nation (mix 3): the white robe with the lotus, and per set the hat and hair of an
    earth, a water or a fire member."""
    P = Pop(h, kind, "zit", *PEACH)
    c, k = P.c, P.k
    gewaad(P, (236, 238, 244), rand_k=LOTUS_BLAUW, zoom_k=LOTUS_BLAUW, riem=LOTUS_BLAUW)
    P.verf(BODY, LOTUS_BLAUW, lambda x, y, z, f: (f == "north") & (np.hypot(x - 2.2, y - 9.0) < 1.3))
    wenkbrauw(P, (70, 56, 50), binnen=0.2, buiten=0.4, dikte=0.45)
    # 0: earth (the conical straw hat, a green sash)
    P.kbot("mix_0_hoed", kegelhoed(P, A_BEIGE, punt=A_DONKER))
    P.bot("mix_0_sjerp", BODY, [0, 8, 0], [c([-5.8, 6.2, -4.9], [11.6, 1.2, 9.8], A_GROEN)])
    # 1: water (a topknot with a blue band, wolf-tail hair at the cheeks, a fur collar)
    P.kbot("mix_1_haar", knot(P, HAAR_BRUIN, band=W_LICHT, y=24.6, z=1.0, b=3.0, hh=2.8)
           + [k([-7.2, 24.2, -7.2], [14.4, 0.8, 13.4], HAAR_BRUIN), k([6.6, 14.0, -6.6], [1.2, 8.0, 1.2], HAAR_BRUIN), k([-7.8, 14.0, -6.6], [1.2, 8.0, 1.2], HAAR_BRUIN)])
    P.bot("mix_1_kraag", BODY, [0, 12, 0], bontkraag(P, BONT, 2.0))
    # 2: fire (a topknot with a little gold flame, a short red shoulder cloth, a fine moustache)
    P.kbot("mix_2_haar", knot(P, HAAR_ZWART, band=GOUD, y=25.4, z=0.0, b=2.8, hh=2.4)
           + [k([-0.45, 27.8, -0.4], [0.9, 2.0, 0.8], GOUD), k([-6.0, 24.6, -6.2], [12.0, 0.8, 12.0], HAAR_ZWART)]
           + snor(P, HAAR_ZWART, hang=1.4, breed=4.8, dik=0.5))
    P.bot("mix_2_mantel", BODY, [0, 12, 0], mantel(P, V_ROOD, patroon=rand(GOUD, 3)))
    return P


# =====================================================================================================================
# animations
# =====================================================================================================================
def verschijn_eenmalig():
    """An echo appears: it rises out of its mist (one-shot)."""
    return [reeks(ROOT, "position", (0, [0, -22, 0]), (1.4, [0, 1.2, 0]), (2.0, [0, 0, 0])),
            reeks(ROOT, "scale", (0, [0.6, 0.6, 0.6]), (1.4, [1.02, 1.02, 1.02]), (2.0, [1, 1, 1])),
            reeks(HEAD, "rotation", (0, [16, 0, 0]), (1.4, [-6, 0, 0]), (2.0, [0, 0, 0]))]


def tegel_geef(p):
    """A master holds out his glowing Pai Sho tile and nods: the trial is won."""
    return [toon(f"{p}_geesttegel", 0.35, 1.9, aan=0.2),
            reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [-82, 6, 0]), (1.6, [-82, 6, 0]), (2.0, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [6, 0, 0]), (0.9, [14, 0, 0]), (1.2, [6, 0, 0]), (2.0, [0, 0, 0])),
            reeks(BODY, "rotation", (0, [0, 0, 0]), (0.8, [8, 0, 0]), (1.3, [8, 0, 0]), (2.0, [0, 0, 0]))]


def staat(p):
    """The Guhvatar state (a pose that lasts): arms spread, a little above the ground, glowing white eyes."""
    return [spoor(f"{p}_staatogen", "scale", {0.0: (0, 0, 0), 0.2: (1, 1, 1), 1.0: (1, 1, 1)}),
            reeks(R, "rotation", (0, [0, 0, 0]), (0.6, [-40, 0, 55])), reeks(L, "rotation", (0, [0, 0, 0]), (0.6, [-40, 0, -55])),
            reeks(ROOT, "position", (0, [0, 0, 0]), (0.8, [0, 4, 0]), (1.0, [0, 4, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.6, [-10, 0, 0]))]


def _miroh_sporen(naam):
    """One of cast_team's Miroh moves as tracks of the workbench."""
    lengte, botten = cta._miroh()[naam]
    out = []
    for bot, kanalen in botten.items():
        for kanaal, sleutels in kanalen.items():
            out.append(spoor(bot, kanaal, {float(t): v for t, v in sleutels.items()}))
    return lengte, out


def _volk(anim_fn, *namen):
    """Named moves of a cast_volk master: {name: (ticks, tracks)} and its hidden props."""
    a = anim_fn()
    return {n: a[0][n] for n in namen}, tuple(a[1])


def anims(kind):
    """(the named moves in order {name: (ticks, tracks)}, hidden props, idle length) of an echo."""
    p = ECHO[kind]["p"]
    vers = (40, verschijn_eenmalig())
    tegel = (40, tegel_geef(p))
    if kind == "jeong_jeong_guh_echo":
        kaars = [toon("jeong_vlam", 0.3, 1.8), reeks(L, "rotation", (0, [0, 0, 0]), (0.4, [-62, -12, 0]), (1.6, [-62, -12, 0]), (2.0, [0, 0, 0])),
                 reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [14, -10, 0]), (1.6, [14, -10, 0]), (2.0, [0, 0, 0]))]
        waarschuw = [reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-150, 0, 18]), (1.1, [-150, 0, 18]), (1.5, [0, 0, 0])),
                     schud(HEAD, 0.3, 1.2, (0, 10, 0), n=2)]
        return {"kaars": (40, kaars), "waarschuw": (30, waarschuw), "tegel": tegel, "verschijn": vers}, ("jeong_vlam",), 10.0
    if kind == "oom_miroh_echo":
        out = {}
        for naam in ("thee", "lach", "wijsheid"):
            lengte, sporen = _miroh_sporen(naam)
            out[naam] = (int(round(lengte * 20)), sporen)
        out["tegel"], out["verschijn"] = tegel, vers
        return out, ("miroh_tegel",), 12.0
    if kind == "bumi_guh_echo":
        out, weg = _volk(cvm.anim_bumi_guh, "giechel", "spierbal")
        return {**out, "tegel": tegel, "verschijn": vers}, weg, 8.0
    if kind == "pakguh_echo":
        out, weg = _volk(cvm.anim_pakguh, "waterzweep", "snor")
        return {**out, "tegel": tegel, "verschijn": vers}, weg, 8.0
    if kind == "piandao_guh_echo":
        kling = [spoor("piandao_kling", "scale", {0.0: (1, 1, 1)})]
        garde = kling + [reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [-70, -20, 0])), reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [4, 0, 0]))]
        pareer = kling + [reeks(R, "rotation", (0, [-70, -20, 0]), (0.15, [-120, 30, 20]), (0.35, [-120, 30, 20]), (0.5, [-70, -20, 0]))]
        slag = kling + [reeks(R, "rotation", (0, [-70, -20, 0]), (0.12, [-150, 0, 0]), (0.25, [-30, -10, 0]), (0.5, [-70, -20, 0])),
                        reeks(BODY, "rotation", (0, [0, 0, 0]), (0.2, [10, 0, 0]), (0.5, [0, 0, 0]))]
        return ({"buig": (30, cvm.buig()), "garde": (0, garde), "pareer": (10, pareer), "slag": (10, slag), "tegel": tegel, "verschijn": vers},
                ("piandao_kling",), 8.0)
    if kind == "lotus_stichter_echo":
        verhaal = [reeks(R, "rotation", (0, [0, 0, 0]), (0.5, [-100, 0, 30]), (1.5, [-90, 0, 60]), (2.4, [-100, 0, 30]), (3.0, [0, 0, 0])),
                   reeks(L, "rotation", (0, [0, 0, 0]), (0.8, [-80, 0, -40]), (2.2, [-80, 0, -40]), (3.0, [0, 0, 0])),
                   reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.6, [-8, 12, 0]), (1.6, [-8, -12, 0]), (2.6, [-4, 0, 0]), (3.0, [0, 0, 0]))]
        return {"verhaal": (60, verhaal), "buig": (30, cvm.buig()), "verschijn": vers}, (), 10.0
    if kind == "lotus_lid_echo":
        return {"buig": (30, cvm.buig()), "knik": (24, cvm.knik()), "verschijn": vers}, (), 8.0
    if kind == "guru_pathik_guh":
        wijs = [reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-160, 0, 8]), (1.1, [-160, 0, 8]), (1.5, [0, 0, 0])),
                reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.3, [-12, 0, 0]), (1.2, [-12, 0, 0]), (1.5, [0, 0, 0]))]
        sap = [toon("pathik_kom", 0.2, 1.85), reeks(L, "rotation", (0, [0, 0, 0]), (0.4, [-70, 0, 0]), (1.6, [-70, 0, 0]), (2.0, [0, 0, 0])),
               reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [0, 0, 8]), (1.5, [0, 0, 8]), (2.0, [0, 0, 0]))]
        mediteer = [reeks(R, "rotation", (0, [0, 0, 0]), (0.6, [-55, 30, 0])), reeks(L, "rotation", (0, [0, 0, 0]), (0.6, [-55, -30, 0])),
                    reeks(ROOT, "position", (0, [0, 0, 0]), (1.0, [0, 3, 0])), reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.6, [8, 0, 0]))]
        return {"wijs": (30, wijs), "sap": (40, sap), "mediteer": (0, mediteer), "verschijn": vers}, ("pathik_kom",), 10.0
    element = {"kuruk_guh": ("water", "kuruk_water"), "yangchen_guh": ("lucht", "yangchen_wind"), "szeto_guh": ("vuur", "szeto_vlam")}[kind]
    naam, prop = element
    if naam == "water":
        beweeg = [toon(prop, 0.25, 1.6), reeks(prop, "rotation", (0.25, [0, 70, 0]), (1.6, [0, -110, 0])),
                  reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-95, -35, 0]), (0.8, [-95, 40, 0]), (1.6, [-80, 0, 0]), (2.0, [0, 0, 0])),
                  reeks(L, "rotation", (0, [0, 0, 0]), (0.3, [-70, -35, 0]), (0.8, [-70, 40, 0]), (2.0, [0, 0, 0]))]
    elif naam == "lucht":
        beweeg = [reeks(prop, "position", (0.3, [0, 0, 0]), (1.4, [0, 2, -10])),
                  reeks(prop, "scale", (0, [0, 0, 0]), (0.29, [0, 0, 0]), (0.4, [1, 1, 1]), (1.2, [1.8, 1, 1.8]), (1.4, [0, 0, 0])),
                  reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-90, 0, 0]), (1.2, [-90, 0, 0]), (2.0, [0, 0, 0])),
                  reeks(L, "rotation", (0, [0, 0, 0]), (0.3, [-90, 0, 0]), (1.2, [-90, 0, 0]), (2.0, [0, 0, 0])),
                  reeks(BODY, "position", (0, [0, 0, 0]), (0.3, [0, 0, -1]), (1.2, [0, 0, -1]), (2.0, [0, 0, 0]))]
    else:
        beweeg = [toon(prop, 0.35, 1.5), reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-20, 0, 0]), (0.45, [-95, 0, 0]), (1.4, [-95, 0, 0]), (2.0, [0, 0, 0])),
                  reeks(BODY, "rotation", (0, [0, 0, 0]), (0.45, [8, -10, 0]), (1.4, [8, -10, 0]), (2.0, [0, 0, 0]))]
    weg = (prop, f"{ECHO[kind]['p']}_staatogen")
    return {naam: (40, beweeg), "verschijn": vers, "staat": (0, staat(ECHO[kind]["p"]))}, weg, 10.0


# =====================================================================================================================
# the table
# =====================================================================================================================
def _rij(p, teken, geest, tegel=None, mix=0, helder=(), extra=None):
    return dict(p=p, teken=teken, geest=geest, tegel=tegel, mix=mix, helder=helder, extra=extra)


VUUR_HELDER = (((255, 150, 60), vlam((255, 120, 40), (255, 236, 150))), ((255, 220, 120), None))
ECHO = {
    "jeong_jeong_guh_echo": _rij("jeong", jeong_jeong, ((255, 214, 190), 0.30, 176), tegel=(206, 70, 52), helder=VUUR_HELDER),
    "oom_miroh_echo": _rij("miroh", None, ((255, 228, 176), 0.28, 172), tegel=(244, 200, 80)),
    "bumi_guh_echo": _rij("bumi", cvm.bumi_guh, ((206, 236, 214), 0.30, 176), tegel=(104, 156, 84)),
    "pakguh_echo": _rij("pakguh", cvm.pakguh, ((200, 226, 255), 0.30, 176), tegel=(62, 104, 170)),
    "piandao_guh_echo": _rij("piandao", cvm.piandao_guh, ((226, 232, 255), 0.30, 176), tegel=(70, 108, 176)),
    "lotus_stichter_echo": _rij("stichter", stichter, ((214, 232, 255), 0.34, 168)),
    "lotus_lid_echo": _rij("lid", lotus_lid, ((214, 232, 255), 0.34, 168), mix=3),
    "guru_pathik_guh": _rij("pathik", pathik, ((255, 236, 200), 0.26, 182)),
    "kuruk_guh": _rij("kuruk", kuruk, ((176, 220, 255), 0.30, 176), helder=(((250, 252, 255), None),)),
    "yangchen_guh": _rij("yangchen", yangchen, ((255, 244, 210), 0.30, 176), helder=(((250, 252, 255), None),)),
    "szeto_guh": _rij("szeto", szeto, ((255, 210, 190), 0.30, 176), helder=(((250, 252, 255), None),) + VUUR_HELDER),
}
MIXBOTTEN = {}                  # kind -> [[bones of set 0], ...], known after build


def piandao_kling(P):
    """The drawn blade of the echo of Piandao-guh (only in garde, pareer and slag): a guard and a long glowing blade."""
    c = P.c
    return P.bot("piandao_kling", R, [-3, 10, -6.5], [c([-3.6, 9.6, -8.0], [1.2, 1.2, 2.4], (44, 40, 60)), c([-4.2, 9.4, -9.0], [2.4, 1.6, 0.6], GOUD),
                                                      c([-3.4, 9.9, -21.0], [0.8, 0.6, 12.0], (236, 244, 255))])


def teken(h, kind):
    """Draws one echo (model, sheet, glow mask) and returns its Pop."""
    rij = ECHO[kind]
    if kind == "oom_miroh_echo":
        P = _van_miroh(h, kind)
    elif rij["teken"] in (cvm.bumi_guh, cvm.pakguh, cvm.piandao_guh):
        P = _van_volk(h, kind, rij["teken"])
    else:
        P = rij["teken"](h, kind)
    if kind == "piandao_guh_echo":
        piandao_kling(P)
    if kind in ("kuruk_guh", "yangchen_guh", "szeto_guh"):
        staatogen(P, rij["p"])
    echo_af(P, rij)
    MIXBOTTEN[kind] = [P.mix[i] for i in sorted(P.mix)]
    P.klaar()
    return P


def animaties(kind):
    """[(name, ticks)] besides idle: praat first, then the named moves of the echo (0 = a pose that lasts)."""
    a = anims(kind)[0]
    return [("praat", 24)] + [(n, t) for n, (t, _) in a.items()]


def bestand(h, kind):
    """Writes guh_npc_<kind>.animation.json: idle (with its first named move woven in, the mist turning slowly), praat and
    every named move."""
    moves, weg, lengte = anims(kind)
    p = ECHO[kind]["p"]
    eerste = next(iter(moves.values()))[1]
    idle = kern.idle(h, "zit", eerste, lengte, start=lengte / 2 + 0.4)
    idle.append(spoor(f"{p}_nevel", "rotation", {0.0: (0, 0, 0), lengte / 2: (0, 180, 0), lengte: (0, 360, 0)}))
    idle.append(spoor(f"{p}_nevel", "scale", {0.0: (1, 1, 1), lengte / 2: (1.08, 1.4, 1.08), lengte: (1, 1, 1)}))
    alle = {"praat": (24, cvm.praat("zit"))}
    alle.update(moves)
    verborgen = tuple(weg) + ((f"{p}_geesttegel",) if ECHO[kind]["tegel"] else ())
    return kern.bestand(h, kind, "zit", idle, alle, verborgen, lengte)


def build(h):
    for kind in ECHO:
        teken(h, kind)
        bestand(h, kind)


def bot_prefixen(kind):
    """The prefixes the extra bones of an echo may carry (its own, the master's it was drawn from, mix_)."""
    p = ECHO[kind]["p"]
    return (f"{p}_", "mix_")
