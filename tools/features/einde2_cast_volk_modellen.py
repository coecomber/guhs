"""
einde2, slice cast_volk - the models of the masters, rulers and townsfolk of the Guhvatar story (CONTRACT_E2 11.5): for every
GuhNpcEntity.Kind of the slice a model (geckolib/models/entity/guh_npc_<kind>.geo.json), a 512 px sheet
(textures/entity/npc_<kind>.png, + _glowmask for the memory and the two spirits) and per character one animation file
(geckolib/animations/entity/guh_npc_<basis>.animation.json: idle with the signature move, praat, the named ones).

Three layers: the wardrobe (einde2_cast_volk_delen) dresses the crowds from table rows (einde2_cast_volk_burgers), the named
characters below add a few bones of their own, a handful has a real prop (the cabbage cart, the pie, the paper cone).
`CAST` is the table; Java's `castvolk.CastVolk` holds the same rows and `check(h)` compares the two.
`preview(out)` draws every kind (every mix set apart) from the front and three-quarter.
"""
import json
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw

from features import einde2_cast_volk_kern as kern
from features.einde2_cast_volk_kern import Pop, blad, glas, karton, mikamasker, op, rand, reeks, ruit, schud, spoor, stip, strepen, toon, vlam, wiel, zoom, _donker
from features.einde2_cast_volk_delen import *  # noqa: F401,F403  (the wardrobe and its colours)
from features import einde2_cast_volk_delen as delen

R, L, HEAD, BODY, ROOT = "arm_right", "arm_left", "head", "body", "root"
RUST_MIKA = (-10, 0, 0)          # the Mika's head at rest (Steele-Mika's idle: chin up)


# =============================================================================================================================
# moves that many share
# =============================================================================================================================
def praat(lijf):
    if lijf == "zit":
        return [reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.2, [-6, 0, 0]), (0.45, [4, 0, 4]), (0.7, [-5, 0, -4]), (0.95, [3, 0, 0]), (1.2, [0, 0, 0])),
                reeks(R, "rotation", (0, [0, 0, 0]), (0.25, [-55, 0, 0]), (0.5, [-30, 0, 0]), (0.8, [-60, 0, 0]), (1.2, [0, 0, 0]))]
    return [reeks(HEAD, "rotation", (0, RUST_MIKA), (0.2, [-18, 0, 0]), (0.45, [-6, 0, 5]), (0.7, [-16, 0, -5]), (0.95, [-8, 0, 0]), (1.2, RUST_MIKA)),
            reeks(BODY, "position", (0, [0, 0, 0]), (0.3, [0, 1, 0]), (0.6, [0, 0, 0]), (0.9, [0, 0.8, 0]), (1.2, [0, 0, 0]))]


def zwaai(arm=R):
    return [reeks(arm, "rotation", (0, [0, 0, 0]), (0.3, [-130, 0, 0]), (0.5, [-130, 0, 25]), (0.7, [-130, 0, -25]), (0.9, [-130, 0, 25]),
                  (1.1, [-130, 0, -25]), (1.5, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [-6, 0, 6]), (1.1, [-6, 0, 6]), (1.5, [0, 0, 0]))]


def buig(lijf="zit"):
    if lijf == "zit":
        return [reeks(BODY, "rotation", (0, [0, 0, 0]), (0.45, [26, 0, 0]), (1.0, [26, 0, 0]), (1.5, [0, 0, 0])),
                reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.45, [14, 0, 0]), (1.0, [14, 0, 0]), (1.5, [0, 0, 0]))]
    return [reeks(HEAD, "rotation", (0, RUST_MIKA), (0.45, [24, 0, 0]), (1.0, [24, 0, 0]), (1.5, RUST_MIKA))]


def knik():
    return [reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.3, [14, 0, 0]), (0.6, [0, 0, 0]), (0.9, [11, 0, 0]), (1.2, [0, 0, 0]))]


def armen_op(t0, t1, hoek=-150, spreid=22):
    return [op(R, t0, t1, (hoek, 0, spreid)), op(L, t0, t1, (hoek, 0, -spreid))]


def verschijn():
    return [reeks(ROOT, "position", (0, [0, -34, 0]), (1.6, [0, 1.5, 0]), (2.0, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [18, 0, 0]), (1.6, [-4, 0, 0]), (2.0, [0, 0, 0]))]


# =============================================================================================================================
# the named characters
# =============================================================================================================================
def guhatso(h):
    P = Pop(h, "guhatso", "zit", *PEACH)
    c = P.c
    gewaad(P, GEEL, rand_k=ORANJE, zoom_k=ORANJE)
    P.verf(BODY, ORANJE, lambda x, y, z, f: (f in ("north", "south")) & (y > 6.0 + (x * (1 if f == "north" else -1) + 5) * 0.62))
    pijl(P)
    wenkbrauw(P, (226, 226, 232), binnen=0.5, buiten=0.1, dikte=0.4)
    P.bot("guhatso_kraag", BODY, [0, 12, 0], [c([-5.5, 11.5, -4.5], [11, 1.7, 9], ORANJE)])
    P.kbot("guhatso_snor", snor(P, (232, 232, 236), hang=5.2, breed=5.4, dik=0.6))
    P.verf(BODY, HOUT, lambda x, y, z, f: (f == "north") & (np.abs(np.hypot(x, y - 11.0) - 3.3) < 0.5) & (y < 11.0))
    P.bot("guhatso_ketting", BODY, [0, 7, -4], [c([-1.3, 6.2, -4.75], [2.6, 2.6, 0.7], HOUT, patroon=stip((96, 160, 214), 6, ring=(246, 226, 170)))])
    taart = lambda: [c([-5.4, 10.6, -8.6], [4.8, 1.2, 4.8], (226, 180, 112)), c([-4.9, 11.8, -8.1], [3.8, 0.6, 3.8], (176, 70, 150)),
                     c([-3.7, 12.4, -6.9], [1.4, 0.8, 1.4], WIT)]
    P.bot("guhatso_taart", R, [-3, 10.5, -6.5], taart())
    P.bot("guhatso_vliegtaart", ROOT, [-3, 11.5, -6.5], taart())
    P.bot("guhatso_wind", ROOT, [-3, 11.5, -9], [c([-5.4, 10.2, -11], [3.4, 0.5, 0.5], (226, 242, 255)), c([-3.6, 11.8, -12], [2.8, 0.5, 0.5], (226, 242, 255)),
                                                c([-5.0, 13.2, -10.5], [2.4, 0.5, 0.5], (226, 242, 255))])
    P.geest(tint=(255, 240, 214), mate=0.16, alfa=184)
    return P.klaar()


def anim_guhatso():
    gooi = [reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [25, 0, 0]), (0.6, [-115, 0, 0]), (1.0, [-45, 0, 0]), (1.5, [0, 0, 0])),
            reeks("guhatso_taart", "scale", (0, [1, 1, 1]), (0.55, [1, 1, 1]), (0.6, [0, 0, 0]), (1.5, [0, 0, 0]), (1.8, [1, 1, 1])),
            reeks("guhatso_vliegtaart", "scale", (0, [0, 0, 0]), (0.58, [0, 0, 0]), (0.62, [1, 1, 1]), (1.35, [1, 1, 1]), (1.45, [0, 0, 0])),
            reeks("guhatso_vliegtaart", "position", (0.6, [0, 2, -2]), (1.0, [0, 15, -26]), (1.4, [0, 4, -52])),
            reeks("guhatso_vliegtaart", "rotation", (0.6, [0, 0, 0]), (1.4, [-360, 0, 0])),
            reeks("guhatso_wind", "scale", (0, [0, 0, 0]), (0.55, [0, 0, 0]), (0.62, [1, 1, 1]), (0.95, [1.6, 1, 1.6]), (1.1, [0, 0, 0])),
            reeks("guhatso_wind", "position", (0.6, [0, 0, 0]), (1.1, [0, 3, -9])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.6, [-8, 0, 0]), (1.4, [-8, 0, 4]), (1.9, [0, 0, 0]))]
    lach = [schud(BODY, 0, 1.5, (0, 0, 3), n=5), reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.25, [-14, 0, 0]), (1.2, [-12, 0, 0]), (1.5, [0, 0, 0]))]
    return {"gooi_taart": (40, gooi), "lach": (30, lach)}, ("guhatso_vliegtaart", "guhatso_wind"), 8.0


def pakguh(h):
    P = Pop(h, "pakguh", "zit", *TAN)
    c = P.c
    gewaad(P, W_BLAUW, rand_k=BONT, kraag=W_DONKER, zoom_k=BONT, riem=W_DONKER)
    haar(P, HAAR_WIT, voor=25.2, zij=20.5, achter=13.0, vanaf_z=-2.0)
    wenkbrauw(P, (236, 236, 240), binnen=-1.0, buiten=0.5, dikte=0.6)
    P.kbot("pakguh_haar", lang_haar(P, HAAR_WIT, tot=10.0, breed=12.0))
    P.kbot("pakguh_snor", snor(P, HAAR_WIT, hang=8.6, breed=5.6, dik=0.8) + sik(P, HAAR_WIT, 2.4, 1.6))
    P.bot("pakguh_kraag", BODY, [0, 12, 0], bontkraag(P))
    P.bot("pakguh_water", ROOT, [0, 9, 0], [c([-9, 9.4, -9], [6, 1.2, 1.2], (120, 190, 240)), c([-3, 10.2, -11], [6, 1.2, 1.2], (150, 210, 250)),
                                           c([3, 9.6, -9.5], [6, 1.2, 1.2], (120, 190, 240)), c([8.5, 10.6, -7], [2, 1.6, 1.6], (200, 235, 255))])
    return P.klaar()


def anim_pakguh():
    snor_ = [reeks(R, "rotation", (0, [0, 0, 0]), (0.45, [-100, -22, 0]), (0.95, [-78, -22, 0]), (1.4, [-100, -22, 0]), (1.75, [-78, -22, 0]), (2.0, [0, 0, 0])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.45, [-5, 0, -4]), (1.75, [-5, 0, -4]), (2.0, [0, 0, 0]))]
    zweep = [reeks("pakguh_water", "scale", (0, [0, 0, 0]), (0.25, [0, 0, 0]), (0.4, [1, 1, 1]), (1.2, [1, 1, 1]), (1.4, [0, 0, 0])),
             reeks("pakguh_water", "rotation", (0.25, [0, 70, 0]), (0.8, [0, -70, 0]), (1.4, [0, 40, 0])),
             reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-95, -35, 0]), (0.8, [-95, 40, 0]), (1.5, [0, 0, 0])),
             reeks(L, "rotation", (0, [0, 0, 0]), (0.3, [-70, -35, 0]), (0.8, [-70, 40, 0]), (1.5, [0, 0, 0]))]
    neus = [reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [-16, 42, 0])), reeks(R, "rotation", (0, [0, 0, 0]), (0.5, [-30, -30, 0])),
            reeks(L, "rotation", (0, [0, 0, 0]), (0.5, [-30, 30, 0]))]
    return {"snor": (40, snor_), "waterzweep": (30, zweep), "neus_op": (0, neus)}, ("pakguh_water",), 8.0


def bumi_guh(h):
    P = Pop(h, "bumi_guh", "zit", *OUD)
    c, k = P.c, P.k
    P.ogen(links=1.32, rechts=0.72)
    PAARS, GROEN = (126, 78, 150), (104, 156, 84)
    gewaad(P, PAARS, rand_k=GROEN)
    haar(P, HAAR_WIT, voor=99, zij=99, achter=19.0, vanaf_z=3.0)
    mond(P, (120, 50, 70), breed=4.2, hoek=0.16, y0=14.9, dikte=0.45)
    P.verf(HEAD, WIT, lambda x, y, z, f: (f == "north") & (x > 1.6) & (x < 2.8) & (y > 14.9) & (y < 16.0), over=True)      # one tooth
    wenkbrauw(P, HAAR_WIT, binnen=0.3, buiten=0.9, dikte=0.6)
    P.kbot("bumi_kroon", [k([-6.0, 25.2, -6.2], [12, 1.5, 11], PAARS, patroon=rand(GOUD, 4)),
                          k([-1.2, 26.7, -1.5], [2.4, 5.5, 1.4], GROEN), k([-0.8, 32.2, -1.4], [1.6, 2.6, 1.2], (150, 200, 110)),
                          k([-3.0, 26.7, -1.2], [1.4, 3.6, 1.0], (90, 140, 200)), k([1.6, 26.7, -1.2], [1.4, 3.6, 1.0], (90, 140, 200)),
                          k([4.2, 26.2, -5.4], [1.6, 2.4, 1.6], (236, 228, 204)), k([5.2, 28.2, -5.3], [1.3, 2.4, 1.4], (236, 228, 204)),
                          k([6.0, 30.2, -5.1], [1.0, 1.8, 1.0], (236, 228, 204)),
                          k([-5.8, 26.2, -5.4], [1.6, 2.4, 1.6], (236, 228, 204)), k([-6.5, 28.2, -5.3], [1.3, 2.4, 1.4], (236, 228, 204)),
                          k([-7.0, 30.2, -5.1], [1.0, 1.8, 1.0], (236, 228, 204))])
    P.kbot("bumi_haar", [k([7.6, 14.5, -2.5], [2.6, 6.0, 5.5], HAAR_WIT), k([9.6, 16.5, -1.5], [1.6, 3.0, 3.5], HAAR_WIT), k([8.6, 13.0, -1.0], [1.4, 1.8, 3.0], HAAR_WIT),
                         k([-10.2, 14.5, -2.5], [2.6, 6.0, 5.5], HAAR_WIT), k([-11.2, 16.5, -1.5], [1.6, 3.0, 3.5], HAAR_WIT), k([-10.0, 13.0, -1.0], [1.4, 1.8, 3.0], HAAR_WIT)]
           + sik(P, HAAR_WIT, 4.2, 1.8))
    # the far too wide robe: a skirt and a mantle that hang around him
    P.bot("bumi_gewaad", BODY, [0, 0.5, 0], [c([-8.0, 0.5, -5.4], [16, 6.5, 10.8], GROEN, patroon=zoom(GOUD, 4)),
                                            c([-6.6, 7.0, -4.8], [13.2, 5.6, 9.6], PAARS, patroon=zoom(GROEN, 5)),
                                            c([-7.6, 10.4, -3.6], [15.2, 2.4, 7.2], GROEN)])
    P.bot("bumi_spieren", BODY, [0, 8, -2], [c([-5.6, 6.5, -5.2], [5.0, 4.6, 1.6], P.huid), c([0.6, 6.5, -5.2], [5.0, 4.6, 1.6], P.huid),
                                            c([-4.6, 2.6, -4.9], [9.2, 3.6, 1.2], P.huid, patroon=strepen(_donker(P.huid, 0.86), 10, 2, liggend=False)),
                                            c([5.0, 8.4, -4.4], [3.6, 4.6, 6.0], P.huid), c([-8.6, 8.4, -4.4], [3.6, 4.6, 6.0], P.huid)])
    P.bot("bumi_kristal", R, [-3, 10, -7], [c([-3.8, 10.4, -7.6], [1.6, 3.4, 1.6], (120, 226, 150)), c([-3.5, 13.8, -7.3], [1.0, 1.2, 1.0], (190, 250, 204))])
    return P.klaar()


def anim_bumi_guh():
    giechel = [schud(BODY, 0, 1.5, (0, 0, 4), n=6), reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.2, [-10, 0, 6]), (0.7, [-12, 0, -6]), (1.2, [-10, 0, 5]), (1.5, [0, 0, 0])),
               schud("bumi_kroon", 0, 1.5, (0, 0, 6), n=6)]
    spier = [reeks("bumi_gewaad", "scale", (0, [1, 1, 1]), (0.3, [1.1, 0.14, 1.1]), (1.6, [1.1, 0.14, 1.1]), (2.0, [1, 1, 1])),
             reeks("bumi_spieren", "scale", (0, [0, 0, 0]), (0.22, [0, 0, 0]), (0.35, [1.1, 1.1, 1.1]), (1.6, [1, 1, 1]), (1.75, [0, 0, 0])),
             reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [-140, 0, 35]), (1.6, [-140, 0, 35]), (2.0, [0, 0, 0])),
             reeks(L, "rotation", (0, [0, 0, 0]), (0.4, [-140, 0, -35]), (1.6, [-140, 0, -35]), (2.0, [0, 0, 0])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [-12, 0, 0]), (1.6, [-12, 0, 0]), (2.0, [0, 0, 0]))]
    snoep = [toon("bumi_kristal", 0.15, 1.85), reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [-100, -20, 0]), (0.8, [-88, -20, 0]), (1.2, [-100, -20, 0]), (1.6, [-88, -20, 0]), (2.0, [0, 0, 0])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [6, 0, 5]), (1.6, [6, 0, 5]), (2.0, [0, 0, 0]))]
    return {"giechel": (30, giechel), "spierbal": (40, spier), "kristalsnoep": (40, snoep)}, ("bumi_spieren", "bumi_kristal"), 8.0


def aardekoning_guh(h):
    P = Pop(h, "aardekoning_guh", "zit", *PEACH)
    c, k = P.c, P.k
    gewaad(P, A_GEEL, rand_k=A_GROEN, kraag=A_GROEN, hals=(250, 244, 226), zoom_k=A_GROEN, riem=A_DONKER)
    haar(P, HAAR_ZWART, voor=25.0, zij=23.0, achter=15.0)
    P.kbot("aardekoning_bril", bril(P, (150, 110, 50), klein=0.3))
    P.kbot("aardekoning_hoed", [k([-3.6, 25.8, -3.8], [7.2, 6.4, 7.2], A_GEEL, patroon=rand(GOUD, 4)), k([-4.6, 32.2, -4.8], [9.2, 1.0, 9.2], GOUD),
                                k([-1.2, 27.6, -4.2], [2.4, 2.8, 0.6], (70, 170, 110)), k([-5.2, 31.0, -5.2], [0.8, 1.2, 0.8], (70, 170, 110)), k([4.4, 31.0, -5.2], [0.8, 1.2, 0.8], (70, 170, 110))])
    P.kbot("aardekoning_vlecht", vlecht(P, HAAR_ZWART, tot=4.0, bol=GOUD))
    P.bot("aardekoning_mouw_links", L, [3.5, 10, -4], [c([1.0, 5.2, -5.9], [4.0, 5.6, 3.6], A_GEEL, patroon=zoom(A_GROEN, 6))])
    P.bot("aardekoning_mouw_rechts", R, [-3.5, 10, -4], [c([-5.0, 5.2, -5.9], [4.0, 5.6, 3.6], A_GEEL, patroon=zoom(A_GROEN, 6))])
    return P.klaar()


def anim_aardekoning_guh():
    bril_ = [reeks(R, "rotation", (0, [0, 0, 0]), (0.5, [-118, -18, 0]), (0.9, [-124, -18, 0]), (1.5, [0, 0, 0])),
             reeks("aardekoning_bril", "position", (0, [0, 0, 0]), (0.2, [0, -0.8, 0]), (0.6, [0, -0.8, 0]), (0.85, [0, 0.3, 0]), (1.5, [0, 0, 0]))]
    klap = [reeks(R, "rotation", (0, [0, 0, 0]), (0.2, [-35, -28, 0]), (0.4, [-35, 8, 0]), (0.6, [-35, -28, 0]), (0.8, [-35, 8, 0]), (1.0, [-35, -28, 0]), (1.2, [-35, 8, 0]), (1.5, [0, 0, 0])),
            reeks(L, "rotation", (0, [0, 0, 0]), (0.2, [-35, 28, 0]), (0.4, [-35, -8, 0]), (0.6, [-35, 28, 0]), (0.8, [-35, -8, 0]), (1.0, [-35, 28, 0]), (1.2, [-35, -8, 0]), (1.5, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.3, [-6, 0, 5]), (0.9, [-6, 0, -5]), (1.5, [0, 0, 0]))]
    return {"bril": (30, bril_), "klap": (30, klap)}, (), 8.0


def arnook_guh(h):
    P = Pop(h, "arnook_guh", "zit", *BRUIN)
    c, k = P.c, P.k
    gewaad(P, W_DONKER, rand_k=BONT, zoom_k=W_LICHT, riem=W_LICHT)
    haar(P, HAAR_BRUIN, voor=24.4, zij=22.0, achter=13.5)
    wenkbrauw(P, HAAR_BRUIN, binnen=-0.5, buiten=0.5, dikte=0.5)
    kraal = (120, 190, 240)
    P.kbot("arnook_haar", [k([6.6, 13.0, -6.6], [1.3, 9.5, 1.3], HAAR_BRUIN), k([6.45, 16.0, -6.75], [1.6, 1.2, 1.6], kraal), k([6.45, 13.4, -6.75], [1.6, 1.0, 1.6], WIT),
                           k([-7.9, 13.0, -6.6], [1.3, 9.5, 1.3], HAAR_BRUIN), k([-8.05, 16.0, -6.75], [1.6, 1.2, 1.6], kraal), k([-8.05, 13.4, -6.75], [1.6, 1.0, 1.6], WIT)]
           + knot(P, HAAR_BRUIN, band=kraal, z=2.0, b=3.4, hh=2.4) + lang_haar(P, HAAR_BRUIN, tot=13.0, breed=10.0))
    P.kbot("arnook_sik", sik(P, HAAR_BRUIN, 1.8, 2.4))
    P.bot("arnook_kraag", BODY, [0, 12, 0], [c([-6.3, 10.6, -5.2], [12.6, 3.0, 10.4], (206, 204, 210)), c([-6.6, 10.2, -3.5], [13.2, 1.2, 7.0], (176, 174, 182))])
    P.bot("arnook_speer", ROOT, [8.8, 0, -2.5], speer(P, 8.8, 27.0))
    return P.klaar()


def anim_knik():
    return {"knik": (24, knik())}, (), 8.0


def piandao_guh(h):
    P = Pop(h, "piandao_guh", "zit", *TAN)
    c, k = P.c, P.k
    BLAUW = (70, 108, 176)
    gewaad(P, WIT, rand_k=BLAUW, kraag=BLAUW, hals=(226, 232, 244), zoom_k=BLAUW, riem=BLAUW)
    P.verf(BODY, GOUD, lambda x, y, z, f: (f == "north") & (np.hypot(x + 2.4, y - 8.2) < 1.15))          # the lotus tile on his chest
    P.verf(BODY, WIT, lambda x, y, z, f: (f == "north") & (np.hypot(x + 2.4, y - 8.2) < 0.55))
    haar(P, HAAR_ZWART, voor=24.9, zij=23.0, achter=16.0)
    wenkbrauw(P, HAAR_ZWART, binnen=0.0, buiten=0.3, dikte=0.4)
    P.kbot("piandao_knot", knot(P, HAAR_ZWART, band=GOUD, z=1.0, b=3.2, hh=3.0))
    P.kbot("piandao_snor", snor(P, HAAR_ZWART, hang=1.2, breed=5.0, dik=0.6) + sik(P, HAAR_ZWART, 2.2, 1.5))
    P.kbot("piandao_penseel", [k([8.0, 21.6, -5.0], [0.7, 0.7, 6.0], HOUT), k([7.9, 21.5, -6.4], [0.9, 0.9, 1.4], ZWART)])
    schede = (44, 40, 60)
    P.bot("piandao_zwaard", BODY, [0, 4, -7], [c([-7.5, 3.4, -8.0], [11.5, 1.3, 1.3], schede, patroon=strepen(GOUD, 16, 2, liggend=False)), c([-8.3, 3.3, -8.1], [0.9, 1.5, 1.5], GOUD),
                                              c([3.9, 2.7, -8.7], [0.7, 2.7, 2.7], GOUD), c([4.6, 3.3, -8.1], [3.6, 1.5, 1.5], V_DONKER, patroon=ruit(ZWART, 6)),
                                              c([8.2, 3.2, -8.2], [0.7, 1.7, 1.7], GOUD)])
    return P.klaar()


def anim_piandao_guh():
    penseel = [reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [-105, 0, 0]), (0.8, [-72, 28, 0]), (1.2, [-112, -18, 0]), (1.6, [-82, 10, 0]), (2.0, [0, 0, 0])),
               reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [-5, 0, -4]), (1.5, [-5, 0, 4]), (2.0, [0, 0, 0]))]
    return {"penseel": (40, penseel), "buig": (30, buig())}, (), 8.0


def werktuigguh(h):
    P = Pop(h, "werktuigguh", "zit", *PEACH)
    c, k = P.c, P.k
    koper = (196, 140, 70)
    gewaad(P, (120, 150, 96), rand_k=A_BRUIN, zoom_k=A_BRUIN)
    haar(P, HAAR_GRIJS, voor=99, zij=99, achter=20.0, vanaf_z=4.0)
    wenkbrauw(P, HAAR_GRIJS, binnen=0.6, buiten=0.2, dikte=0.5)
    # the goggles pushed up on his forehead: a strap round the head, two brass rings with glass
    P.kbot("werktuigguh_bril", [k([-7.3, 24.6, -7.3], [14.6, 1.2, 13.6], LEER), k([-8.25, 23.8, -4.7], [16.5, 1.0, 8.4], LEER),
                                k([0.7, 23.9, -8.3], [4.6, 3.2, 1.2], koper, patroon=glas(koper)), k([-5.3, 23.9, -8.3], [4.6, 3.2, 1.2], koper, patroon=glas(koper)),
                                k([-0.7, 25.0, -8.0], [1.4, 0.9, 0.7], koper)], pivot=(0, 25, -7))
    P.kbot("werktuigguh_monocle", bril(P, GOUD, alleen=-1) + [k([-7.6, 16.6, -7.8], [0.4, 2.4, 0.4], GOUD), k([-7.2, 14.8, -7.8], [0.4, 2.0, 0.4], GOUD),
                                                             k([-6.8, 13.2, -7.8], [0.4, 1.8, 0.4], GOUD)])
    P.kbot("werktuigguh_haar", [k([7.4, 15.5, -3.5], [2.8, 7.5, 7.0], HAAR_GRIJS), k([9.6, 18.0, -2.0], [1.8, 3.4, 4.0], HAAR_GRIJS), k([8.6, 22.6, -1.0], [1.6, 1.8, 3.0], HAAR_GRIJS),
                                k([-10.2, 15.5, -3.5], [2.8, 7.5, 7.0], HAAR_GRIJS), k([-11.4, 18.0, -2.0], [1.8, 3.4, 4.0], HAAR_GRIJS), k([-10.2, 22.6, -1.0], [1.6, 1.8, 3.0], HAAR_GRIJS)]
           + snor(P, HAAR_GRIJS, hang=0.9, breed=4.6, dik=0.7))
    P.bot("werktuigguh_schort", BODY, [0, 7, -4], [c([-4.2, 1.6, -4.7], [8.4, 8.6, 0.7], LEER, patroon=rand(_donker(LEER, 0.7), 3)), c([-2.6, 4.2, -5.0], [5.2, 2.6, 0.5], _donker(LEER, 0.8)),
                                                  c([-2.2, 6.8, -5.0], [0.7, 1.6, 0.5], (226, 60, 60)), c([-0.9, 6.8, -5.0], [0.7, 2.0, 0.5], (240, 200, 70)), c([0.4, 6.8, -5.0], [0.7, 1.4, 0.5], (70, 130, 220)),
                                                  c([1.6, 6.8, -5.0], [0.7, 1.8, 0.5], (90, 180, 100)),
                                                  c([-4.4, 10.0, -4.6], [0.9, 2.6, 0.5], LEER), c([3.5, 10.0, -4.6], [0.9, 2.6, 0.5], LEER)])
    P.bot("werktuigguh_vingers", L, [3, 9, -6.5], [c([1.6, 9.4, -8.0], [0.7, 0.7, 1.6], HOUT), c([2.65, 9.4, -8.3], [0.7, 0.7, 1.9], HOUT), c([3.7, 9.4, -8.0], [0.7, 0.7, 1.6], HOUT),
                                                  c([1.4, 6.9, -6.7], [3.2, 3.7, 0.5], HOUT)])
    staal = (170, 176, 190)
    P.bot("werktuigguh_sleutel", R, [-3, 10, -6.5], [c([-3.4, 9.6, -7.4], [0.9, 5.6, 0.8], staal), c([-4.3, 15.2, -7.5], [2.7, 1.2, 1.0], staal),
                                                    c([-4.3, 16.4, -7.5], [0.9, 1.3, 1.0], staal), c([-2.5, 16.4, -7.5], [0.9, 1.3, 1.0], staal)])
    return P.klaar()


def anim_werktuigguh():
    knutsel = [reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-40, 12, 0]), (0.55, [-40, 12, 28]), (0.8, [-40, 12, -10]), (1.05, [-40, 12, 28]), (1.3, [-40, 12, -10]), (1.55, [-40, 12, 28]), (2.0, [0, 0, 0])),
               reeks(L, "rotation", (0, [0, 0, 0]), (0.3, [-40, -12, 0]), (1.6, [-40, -12, 0]), (2.0, [0, 0, 0])),
               reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.3, [10, 0, 0]), (1.6, [10, 0, 0]), (2.0, [0, 0, 0]))]
    eureka = [reeks("werktuigguh_bril", "position", (0, [0, 0, 0]), (0.18, [0, -3.5, -0.2]), (1.2, [0, -3.5, -0.2]), (1.5, [0, 0, 0])),
              op(R, 0.1, 1.5, (-150, 0, 22)), op(L, 0.1, 1.5, (-150, 0, -22)),
              reeks(BODY, "position", (0, [0, 0, 0]), (0.25, [0, 2, 0]), (0.45, [0, 0, 0]), (1.5, [0, 0, 0]))]
    niet_af = [reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [60, 20, 0])), reeks(L, "rotation", (0, [0, 0, 0]), (0.4, [60, -20, 0])),
               reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [-6, -18, 0])), reeks(BODY, "rotation", (0, [0, 0, 0]), (0.4, [0, 14, 0]))]
    return {"knutsel": (40, knutsel), "eureka": (30, eureka), "nog_niet_af": (0, niet_af)}, (), 8.0


def roku_guh(h, kind="roku_guh"):
    P = Pop(h, kind, "zit", *OUD)
    c, k = P.c, P.k
    gewaad(P, V_ROOD, rand_k=GOUD, kraag=GOUD, hals=V_DONKER, zoom_k=GOUD, riem=V_DONKER)
    haar(P, HAAR_WIT, voor=24.9, zij=21.5, achter=13.0)
    wenkbrauw(P, HAAR_WIT, binnen=0.2, buiten=0.5, dikte=0.55)
    P.kbot("roku_haar", lang_haar(P, HAAR_WIT, tot=8.0, breed=12.0) + knot(P, HAAR_WIT, band=V_ROOD, z=0.5, b=3.2, hh=2.6)
           + [k([-1.7, 28.6, 0.0], [0.9, 3.0, 1.0], GOUD), k([0.8, 28.6, 0.0], [0.9, 3.0, 1.0], GOUD), k([-1.7, 28.2, -0.1], [3.4, 0.8, 1.2], GOUD)])
    P.kbot("roku_baard", baard(P, HAAR_WIT, lengte=7.0, breed=6.6) + snor(P, HAAR_WIT, hang=2.0, breed=5.4, dik=0.8))
    P.bot("roku_mantel", BODY, [0, 12, 0], mantel(P, V_DONKER, patroon=rand(GOUD, 4)) + [c([5.6, 12.6, -3.0], [3.0, 0.9, 6.0], GOUD), c([-8.6, 12.6, -3.0], [3.0, 0.9, 6.0], GOUD)])
    if kind.endswith("_beeld"):
        P.steen()
    else:
        P.geest()
    return P.klaar()


def anim_roku_guh():
    raad = [reeks(R, "rotation", (0, [0, 0, 0]), (0.8, [-105, -8, 0]), (1.5, [-105, -8, 0]), (2.0, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.8, [6, 0, 0]), (1.5, [6, 0, 0]), (2.0, [0, 0, 0]))]
    return {"raad": (40, raad), "verschijn": (0, verschijn())}, (), 16.0


def kyoshi_guh(h, kind="kyoshi_guh"):
    P = Pop(h, kind, "zit", *PEACH)
    c, k = P.c, P.k
    groen, donker = (62, 120, 76), (36, 78, 54)
    gewaad(P, groen, rand_k=GOUD, zoom_k=donker, riem=GOUD)
    haar(P, HAAR_BRUIN, voor=24.9, zij=22.8, achter=14.0)
    schmink(P)
    P.kbot("kyoshi_kroon", kyoshi_kroon(P, groot=1.2))
    P.kbot("kyoshi_haar", bolknot(P, HAAR_BRUIN, y=24.5, z=5.5, b=5.0))
    P.bot("kyoshi_harnas", BODY, [0, 8, -4], [c([-4.5, 4.8, -4.7], [9.0, 6.4, 0.8], donker, patroon=strepen(GOUD, 8, 2)), c([4.9, 10.4, -3.2], [2.8, 2.2, 6.4], donker, patroon=rand(GOUD, 3)),
                                             c([-7.7, 10.4, -3.2], [2.8, 2.2, 6.4], donker, patroon=rand(GOUD, 3)), c([-5.4, 11.8, -4.4], [10.8, 1.3, 8.8], WIT)])
    P.bot("kyoshi_waaier_links", L, [3, 9.5, -7], waaier(P, 1), rot=[0, 0, -72])
    P.bot("kyoshi_waaier_rechts", R, [-3, 9.5, -7], waaier(P, -1), rot=[0, 0, 72])
    laars = (92, 60, 40)
    P.bot("kyoshi_laars_links", "foot_left", [4, 1, -4], [c([1.9, 0, -9.6], [4.9, 3.2, 7.8], laars, patroon=zoom(_donker(laars, 0.6), 5)), c([2.2, 3.2, -5.2], [4.3, 1.6, 3.4], laars)])
    P.bot("kyoshi_laars_rechts", "foot_right", [-4, 1, -4], [c([-6.8, 0, -9.6], [4.9, 3.2, 7.8], laars, patroon=zoom(_donker(laars, 0.6), 5)), c([-6.5, 3.2, -5.2], [4.3, 1.6, 3.4], laars)])
    if kind.endswith("_beeld"):
        P.steen()
    else:
        P.geest()
    return P.klaar()


def anim_kyoshi_guh():
    w = [reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-70, 0, 0]), (1.1, [-70, 0, 0]), (1.5, [0, 0, 0])),
         reeks("kyoshi_waaier_rechts", "scale", (0, [1, 1, 1]), (0.15, [0.15, 1, 1]), (0.5, [0.15, 1, 1]), (0.58, [1.2, 1, 1]), (0.7, [1, 1, 1])),
         reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [0, 0, 0]), (0.6, [-6, 0, 0]), (1.5, [0, 0, 0]))]
    return {"waaier": (30, w), "verschijn": (0, verschijn())}, (), 16.0


def anim_beeld(namen):
    """A statue holds still: every name of the character, all without a move."""
    stil = [spoor(HEAD, "rotation", {0: (0, 0, 0)})]
    return {n: (t, list(stil)) for n, t in namen}


def joo_dee_guh(h):
    P = Pop(h, "joo_dee_guh", "zit", *PEACH)
    c, k = P.c, P.k
    P.ogen(links=1.12, rechts=1.12)
    gewaad(P, (190, 198, 104), rand_k=A_DONKER, kraag=A_DONKER, hals=(240, 236, 210), zoom_k=A_DONKER, riem=A_GROEN)
    haar(P, HAAR_ZWART, voor=24.2, zij=22.0, achter=14.5)
    # the smile: far too wide, painted, with a row of teeth; it never moves
    lip = (196, 44, 70)
    kromme = lambda x: 14.4 + 2.5 * (np.abs(x) / 6.0) ** 2
    P.verf(HEAD, lip, lambda x, y, z, f: (f == "north") & (np.abs(x) < 6.2) & (y > kromme(x) - 0.45) & (y < kromme(x) + 1.55), over=True)
    P.verf(HEAD, (255, 255, 255), lambda x, y, z, f: (f == "north") & (np.abs(x) < 5.6) & (y > kromme(x)) & (y < kromme(x) + 1.1), over=True)
    P.verf(HEAD, (206, 206, 214), lambda x, y, z, f: (f == "north") & (np.abs(x) < 5.6) & (y > kromme(x)) & (y < kromme(x) + 1.1) & (np.abs((x + 20) % 1.4 - 0.7) > 0.58), over=True)
    blos(P, (255, 150, 170), 1.2)
    P.kbot("joo_dee_knot", bolknot(P, HAAR_ZWART, y=25.0, z=3.5, b=5.5) + [k([-4.0, 26.2, 4.8], [8.0, 0.6, 0.6], GOUD)])
    P.kbot("joo_dee_haar", [k([6.6, 15.0, -6.4], [1.2, 8.5, 1.4], HAAR_ZWART), k([-7.8, 15.0, -6.4], [1.2, 8.5, 1.4], HAAR_ZWART)])
    P.bot("joo_dee_kraag", BODY, [0, 12, 0], [c([-5.3, 11.6, -4.4], [10.6, 1.4, 8.8], A_DONKER)])
    return P.klaar()


def anim_joo_dee_guh():
    glimlach = [reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.25, [0, 0, 20]), (1.2, [0, 0, 20]), (1.5, [0, 0, 0]))]
    herstart = [reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.05, [0, 0, 0]), (1.0, [0, 0, 0]), (1.06, [7, 0, -9]), (1.2, [-3, 0, 4]), (1.4, [0, 0, 0]), (2.0, [0, 0, 0])),
                reeks(BODY, "scale", (0, [1, 1, 1]), (1.0, [1, 1, 1]), (1.1, [1.03, 1.04, 1.03]), (2.0, [1, 1, 1])),
                reeks(R, "rotation", (0, [0, 0, 0]), (1.0, [0, 0, 0]), (1.1, [-14, 0, 0]), (1.4, [0, 0, 0]))]
    return {"glimlach": (30, glimlach), "herstart": (40, herstart)}, (), 8.0


def koolman(h):
    P = Pop(h, "koolman", "zit", *TAN)
    c, k = P.c, P.k
    gewaad(P, A_GROEN, rand_k=A_DONKER, kraag=A_DONKER, hals=A_BEIGE, zoom_k=A_DONKER, riem=A_BRUIN)
    haar(P, HAAR_BRUIN, voor=99, zij=23.5, achter=18.0, vanaf_z=2.0)
    wenkbrauw(P, HAAR_BRUIN, binnen=0.7, buiten=-0.2, dikte=0.45)
    P.kbot("koolman_hoed", kegelhoed(P, STRO))
    P.kbot("koolman_baard", snor(P, HAAR_BRUIN, hang=1.6, breed=4.6, dik=0.5) + sik(P, HAAR_BRUIN, 3.0, 1.2))
    # the cart on his right: a box on two wheels with two handles towards him, full of cabbages
    hout, donker = (176, 128, 80), (112, 78, 50)
    plank = strepen(donker, 8, 1)
    kar = [c([-24.5, 4.0, -6.5], [13.0, 1.0, 11.0], hout, patroon=plank),
           c([-24.5, 5.0, -6.5], [13.0, 3.6, 0.8], hout, patroon=plank), c([-24.5, 5.0, 3.7], [13.0, 3.6, 0.8], hout, patroon=plank),
           c([-24.5, 5.0, -5.7], [0.8, 3.6, 9.4], hout, patroon=plank), c([-12.3, 5.0, -5.7], [0.8, 3.6, 9.4], hout, patroon=plank),
           c([-11.5, 5.6, -5.6], [4.6, 0.8, 0.8], donker), c([-11.5, 5.6, 2.8], [4.6, 0.8, 0.8], donker),
           c([-20.0, 0.0, -7.5], [8.0, 8.0, 0.9], hout, patroon=wiel(hout, donker)), c([-20.0, 0.0, 4.6], [8.0, 8.0, 0.9], hout, patroon=wiel(hout, donker)),
           c([-16.6, 3.4, -6.8], [1.2, 1.2, 11.6], donker), c([-13.4, 0.0, -1.6], [1.0, 4.0, 1.0], donker)]
    P.bot("koolman_kar", ROOT, [-18, 4, -1], kar)
    kool, licht, nerf = (112, 178, 84), (190, 226, 140), (150, 206, 110)
    kb = blad(licht, nerf)
    for i, (x, y, z, s) in enumerate([(-23.4, 6.0, -5.2, 4.0), (-18.8, 6.0, -5.0, 3.8), (-23.0, 6.0, -0.6, 3.8), (-18.4, 6.0, -0.4, 4.2), (-21.2, 9.4, -3.2, 4.0)], 1):
        P.bot(f"koolman_kool_{i}", "koolman_kar", [x + s / 2, y, z + s / 2], [c([x, y, z], [s, s * 0.85, s], kool, patroon=kb), c([x + 0.5, y + s * 0.85, z + 0.5], [s - 1, 0.5, s - 1], licht)])
    return P.klaar()


def anim_koolman():
    kolen = armen_op(0.1, 1.9, -155, 26) + [reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.3, [-22, 0, 0]), (1.6, [-22, 0, 0]), (2.0, [0, 0, 0])),
                                           reeks(BODY, "position", (0, [0, 0, 0]), (0.25, [0, 1.5, 0]), (0.5, [0, 0, 0]), (2.0, [0, 0, 0]))]
    for i in range(1, 6):
        t = 0.15 + i * 0.06
        kolen.append(reeks(f"koolman_kool_{i}", "position", (0, [0, 0, 0]), (t, [0, 0, 0]), (t + 0.2, [0, 5.5, 0]), (t + 0.42, [0, 0, 0]), (t + 0.6, [0, 2.6, 0]), (t + 0.78, [0, 0, 0]), (2.0, [0, 0, 0])))
        kolen.append(reeks(f"koolman_kool_{i}", "rotation", (0, [0, 0, 0]), (t, [0, 0, 0]), (t + 0.42, [0, 40 * (1 if i % 2 else -1), 12]), (t + 0.78, [0, 0, 0])))
    poets = [reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-38, -50, 0]), (0.6, [-52, -62, 0]), (0.9, [-38, -50, 0]), (1.2, [-52, -62, 0]), (1.5, [-38, -50, 0]), (2.0, [0, 0, 0])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.3, [8, -22, 6]), (1.5, [8, -22, 6]), (2.0, [0, 0, 0])),
             reeks("koolman_kool_4", "rotation", (0, [0, 0, 0]), (0.6, [0, 12, 0]), (1.2, [0, -12, 0]), (2.0, [0, 0, 0]))]
    return {"mijn_kolen": (40, kolen), "poets_kool": (40, poets)}, (), 12.0


def loketguh(h):
    P = Pop(h, "loketguh", "zit", *PEACH)
    c, k = P.c, P.k
    gewaad(P, A_DONKER, rand_k=GOUD, zoom_k=(36, 70, 46))
    P.verf(BODY, GOUD, lambda x, y, z, f: (f == "north") & (np.abs(x) < 0.5) & (((y - 2.5) % 2.6) < 1.0) & (y > 2.4) & (y < 11.5))    # a row of buttons
    haar(P, HAAR_GRIJS, voor=24.3, zij=22.2, achter=15.0)
    wenkbrauw(P, (120, 120, 130), binnen=-1.1, buiten=0.2, dikte=0.55)
    mond(P, (130, 70, 90), breed=1.8, y0=15.9, dikte=0.35)
    P.kbot("loketguh_knot", [k([-2.4, 26.0, -1.6], [4.8, 3.4, 4.8], HAAR_GRIJS), k([-1.6, 29.4, -0.8], [3.2, 0.8, 3.2], HAAR_GRIJS), k([-3.4, 27.2, 0.4], [6.8, 0.5, 0.5], ZWART)])
    P.kbot("loketguh_bril", bril(P, (40, 40, 48), laag=-2.2, smal=True, z=-8.2) + [k([7.0, 18.6, -7.6], [0.5, 0.5, 5.0], (40, 40, 48)), k([-7.5, 18.6, -7.6], [0.5, 0.5, 5.0], (40, 40, 48))])
    P.bot("loketguh_kraag", BODY, [0, 12, 0], [c([-4.8, 11.6, -4.4], [9.6, 2.0, 8.8], (36, 70, 46), patroon=zoom(GOUD, 4, boven=True))])
    P.bot("loketguh_stempel", R, [-3, 10, -6.5], [c([-3.7, 11.6, -7.6], [1.4, 3.0, 1.4], HOUT), c([-4.2, 14.6, -8.1], [2.4, 1.6, 2.4], HOUT),
                                                 c([-4.6, 10.0, -8.5], [3.2, 1.6, 3.2], (60, 56, 66)), c([-4.4, 9.6, -8.3], [2.8, 0.4, 2.8], (214, 44, 56))])
    return P.klaar()


def anim_loketguh():
    stempel = [reeks(R, "rotation", (0, [0, 0, 0]), (0.35, [-120, 0, 0]), (0.55, [-125, 0, 0]), (0.63, [18, 0, 0]), (0.8, [10, 0, 0]), (1.1, [10, 0, 0]), (1.5, [0, 0, 0])),
               reeks(BODY, "position", (0, [0, 0, 0]), (0.63, [0, 0, 0]), (0.68, [0, -0.7, 0]), (0.8, [0, 0, 0])),
               reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.35, [-6, 0, 0]), (0.63, [12, 0, 0]), (1.1, [8, 0, 0]), (1.5, [0, 0, 0]))]
    nee = [schud(HEAD, 0.1, 1.4, (0, 22, 0), n=3), reeks(BODY, "rotation", (0, [0, 0, 0]), (0.3, [-4, 0, 0]), (1.2, [-4, 0, 0]), (1.5, [0, 0, 0]))]
    return {"stempel": (30, stempel), "nee": (30, nee)}, (), 8.0


def kei_guh(h, kind="kei_guh", toneel=False):
    P = Pop(h, kind, "zit", *(TAN if not toneel else (None, 1.0, 1.0)))
    c, k = P.c, P.k
    groen = (70, 130, 80)
    if toneel:                                               # the biggest actor of the troupe plays the little girl
        gewaad(P, (132, 176, 110), rand_k=(240, 232, 200), zoom_k=(240, 232, 200), riem=(240, 232, 200))
        P.verf(HEAD, (176, 150, 160), lambda x, y, z, f: (f == "north") & (y < 16.4) & (np.abs(x) < 4.6) & (y > 13.6) & (P.rng.random(x.shape) < 0.5))   # stubble
        haar(P, HAAR_ZWART, voor=24.8, zij=23.2, achter=17.0)
        P.kbot("toneel_bandje", [k([-2.6, 25.9, -5.4], [5.2, 0.9, 1.2], (120, 190, 110)), k([-3.4, 25.0, -5.6], [0.9, 1.4, 0.9], (240, 232, 200)), k([2.5, 25.0, -5.6], [0.9, 1.4, 0.9], (240, 232, 200)),
                                 k([-1.6, 26.6, -1.6], [3.2, 2.4, 3.2], HAAR_ZWART, patroon=kern.gebreid(HAAR_ZWART))])
        pre = "toneel"
    else:
        P.verf(BODY, groen, lambda x, y, z, f: y < 4.6)                                 # the wrestling trunks
        buikvlek(P, tuple(min(255, int(v * 1.12) + 8) for v in P.huid), breed=3.4, y0=4.8, y1=11.5)
        P.verf(BODY, _donker(P.huid, 0.82), lambda x, y, z, f: (f == "north") & (np.abs(y - 8.6) < 0.25) & (np.abs(x) > 0.4) & (np.abs(x) < 3.0))   # the chest
        haar(P, HAAR_ZWART, voor=99, zij=99, achter=22.5, vanaf_z=1.0)
        wenkbrauw(P, HAAR_ZWART, binnen=-1.0, buiten=0.4, dikte=0.6)
        P.kbot("kei_knot", knot(P, HAAR_ZWART, band=(214, 60, 60), z=1.0, b=2.8, hh=2.6))
        P.bot("kei_riem", BODY, [0, 4, 0], [c([-5.4, 3.6, -4.4], [10.8, 2.4, 8.8], (60, 110, 70), patroon=rand(GOUD, 4)), c([-6.8, 3.6, -3.3], [13.6, 2.4, 6.6], (60, 110, 70), patroon=rand(GOUD, 4)),
                                           c([-2.2, 3.0, -5.0], [4.4, 3.6, 0.7], GOUD, patroon=stip((90, 170, 110), 7, ring=(250, 230, 140)))])
        pre = "kei"
    merk = stip((70, 130, 80), 8, ring=_donker(P.huid, 0.9)) if not toneel else None
    # the enormous arms and shoulders
    P.bot(f"{pre}_schouders", BODY, [0, 10, 0], [c([4.6, 7.6, -3.8], [3.8, 5.2, 6.6], P.huid, patroon=merk), c([-8.4, 7.6, -3.8], [3.8, 5.2, 6.6], P.huid),
                                                c([-5.6, 10.6, -4.4], [11.2, 2.2, 8.4], P.huid)])
    P.bot(f"{pre}_arm_links", L, [3.5, 10, -4], [c([1.0, 5.9, -7.3], [4.8, 5.4, 5.2], P.huid), c([1.5, 6.6, -8.1], [3.8, 4.0, 0.9], _donker(P.huid, 0.92))])
    P.bot(f"{pre}_arm_rechts", R, [-3.5, 10, -4], [c([-5.8, 5.9, -7.3], [4.8, 5.4, 5.2], P.huid), c([-5.3, 6.6, -8.1], [3.8, 4.0, 0.9], _donker(P.huid, 0.92))])
    return P if toneel else P.klaar()


def anim_kei_guh():
    spier = [reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [-135, 0, 38]), (0.9, [-128, 0, 30]), (1.3, [-135, 0, 38]), (2.0, [0, 0, 0])),
             reeks(L, "rotation", (0, [0, 0, 0]), (0.4, [-135, 0, -38]), (0.9, [-128, 0, -30]), (1.3, [-135, 0, -38]), (2.0, [0, 0, 0])),
             reeks("kei_arm_links", "scale", (0, [1, 1, 1]), (0.45, [1.18, 1.18, 1.1]), (1.4, [1.18, 1.18, 1.1]), (2.0, [1, 1, 1])),
             reeks("kei_arm_rechts", "scale", (0, [1, 1, 1]), (0.45, [1.18, 1.18, 1.1]), (1.4, [1.18, 1.18, 1.1]), (2.0, [1, 1, 1])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [-10, 0, 0]), (1.4, [-10, 0, 0]), (2.0, [0, 0, 0]))]
    brul = [reeks(R, "rotation", (0, [0, 0, 0]), (0.25, [-30, -45, 0]), (0.45, [-30, -10, 0]), (0.75, [-30, -45, 0]), (0.95, [-30, -10, 0]), (1.25, [-30, -45, 0]), (1.45, [-30, -10, 0]), (2.0, [0, 0, 0])),
            reeks(L, "rotation", (0, [0, 0, 0]), (0.25, [-30, 10, 0]), (0.5, [-30, 45, 0]), (0.7, [-30, 10, 0]), (1.0, [-30, 45, 0]), (1.2, [-30, 10, 0]), (1.5, [-30, 45, 0]), (2.0, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.3, [-20, 0, 0]), (1.5, [-20, 0, 0]), (2.0, [0, 0, 0])),
            schud(BODY, 0.2, 1.6, (0, 0, 2.5), n=5)]
    return {"spierbal": (40, spier), "brul": (40, brul)}, (), 8.0


def aardrommel_omroeper(h):
    P = Pop(h, "aardrommel_omroeper", "zit", *PEACH)
    c, k = P.c, P.k
    groen = (60, 150, 84)
    gewaad(P, groen, rand_k=GOUD, kraag=GOUD, hals=WIT, zoom_k=GOUD)
    P.verf(BODY, GOUD, lambda x, y, z, f: (f == "north") & (np.abs(np.abs(x) - 1.5) < 0.45) & (((y - 2.6) % 2.4) < 0.9) & (y < 8.2) & (y > 2.4))
    haar(P, HAAR_ZWART, voor=24.6, zij=23.0, achter=17.0)
    wenkbrauw(P, HAAR_ZWART, binnen=0.6, buiten=0.4, dikte=0.4)
    P.kbot("omroeper_kuif", [k([-2.6, 25.8, -6.8], [5.6, 1.6, 4.4], HAAR_ZWART), k([0.4, 27.4, -7.6], [3.2, 1.4, 2.6], HAAR_ZWART), k([2.2, 26.2, -8.6], [1.8, 1.6, 1.6], HAAR_ZWART),
                             k([-1.0, 13.9, -7.8], [2.0, 1.0, 0.8], (214, 60, 60)), k([-2.6, 13.6, -7.7], [1.7, 1.6, 0.7], (214, 60, 60)), k([0.9, 13.6, -7.7], [1.7, 1.6, 0.7], (214, 60, 60))])
    # the huge paper cone: from his right paw up and outwards
    papier, rand_g = (244, 236, 210), (60, 150, 84)
    P.bot("omroeper_toeter", R, [-3, 9.5, -6.5], [c([-3.9, 8.6, -8.6], [1.8, 1.8, 2.2], rand_g), c([-4.5, 8.0, -11.4], [3.0, 3.0, 2.8], papier),
                                                 c([-5.4, 7.1, -14.8], [4.8, 4.8, 3.4], papier), c([-6.6, 5.9, -18.8], [7.2, 7.2, 4.0], papier),
                                                 c([-7.6, 4.9, -20.0], [9.2, 9.2, 1.2], rand_g, patroon=stip((40, 36, 44), 10.5, ring=papier))], rot=[-48, 26, 0])
    return P.klaar()


def anim_aardrommel_omroeper():
    roep = [reeks(R, "rotation", (0, [0, 0, 0]), (0.35, [-40, -20, 0]), (1.5, [-40, -20, 0]), (2.0, [0, 0, 0])),
            reeks(BODY, "rotation", (0, [0, 0, 0]), (0.35, [-12, 0, 0]), (1.5, [-12, 0, 0]), (2.0, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.35, [-16, 0, 0]), (0.7, [-20, 0, 4]), (1.1, [-20, 0, -4]), (1.5, [-16, 0, 0]), (2.0, [0, 0, 0])),
            reeks("omroeper_toeter", "scale", (0, [1, 1, 1]), (0.5, [1.08, 1.08, 1.08]), (0.7, [1, 1, 1]), (0.9, [1.08, 1.08, 1.08]), (1.1, [1, 1, 1]), (2.0, [1, 1, 1]))]
    return {"roep": (40, roep)}, (), 8.0


def fanguh(h):
    P = Pop(h, "fanguh", "zit", *TAN)
    c, k = P.c, P.k
    P.ogen(links=1.28, rechts=1.28)
    x0, y0, x1, y1 = P.gezicht                              # huge shining eyes: more sparkles in them
    ox, oy, r = P.oog
    P.verf(HEAD, (255, 255, 255), lambda x, y, z, f: (f == "north") & ((np.hypot(np.abs(x) - ox + 1.0 * np.sign(x), y - oy - 1.1) < 0.75)
                                                                        | (np.hypot(np.abs(x) - ox - 1.2 * np.sign(x), y - oy + 1.3) < 0.5)), over=True, var=0)
    blauw = (86, 124, 178)
    gewaad(P, blauw, rand_k=A_BEIGE, kraag=A_BEIGE, hals=(236, 226, 200), zoom_k=(60, 90, 140), riem=A_BEIGE)
    haar(P, HAAR_BRUIN, voor=25.2, zij=23.4, achter=16.0)
    P.kbot("fanguh_band", hoofdband(P, (70, 100, 160), y=24.0) + knot(P, HAAR_BRUIN, z=2.5, b=2.8, hh=3.4))
    schuim = (250, 252, 255)
    P.kbot("fanguh_schuim", [k([-2.8, 13.6, -8.8], [5.6, 2.6, 1.6], schuim), k([-1.6, 12.2, -9.2], [3.2, 1.6, 1.6], schuim), k([1.8, 15.4, -9.0], [1.8, 1.6, 1.4], schuim),
                             k([-3.6, 15.2, -8.8], [1.6, 1.4, 1.2], schuim), k([-0.8, 10.8, -9.0], [1.6, 1.4, 1.2], (226, 240, 255))])
    return P.klaar()


def anim_fanguh():
    val = [schud(BODY, 0, 1.0, (0, 0, 5), n=5), schud(HEAD, 0, 1.0, (0, 0, 6), n=5),
           reeks("fanguh_schuim", "scale", (0, [0, 0, 0]), (0.6, [0, 0, 0]), (0.9, [1, 1, 1]), (2.3, [1.1, 1.1, 1.1]), (2.5, [0, 0, 0])),
           reeks(ROOT, "rotation", (0, [0, 0, 0]), (1.1, [0, 0, 0]), (1.5, [-88, 0, 0]), (2.2, [-88, 0, 0]), (2.5, [0, 0, 0])),
           reeks(ROOT, "position", (0, [0, 0, 0]), (1.1, [0, 0, 0]), (1.5, [0, 6, 4]), (2.2, [0, 6, 4]), (2.5, [0, 0, 0])),
           reeks(R, "rotation", (0, [0, 0, 0]), (1.1, [-60, 0, 0]), (1.5, [-150, 0, 20]), (2.2, [-150, 0, 20]), (2.5, [0, 0, 0])),
           reeks(L, "rotation", (0, [0, 0, 0]), (1.1, [-60, 0, 0]), (1.5, [-150, 0, -20]), (2.2, [-150, 0, -20]), (2.5, [0, 0, 0]))]
    flauw = [reeks(ROOT, "rotation", (0, [-88, 0, 0])), reeks(ROOT, "position", (0, [0, 6, 4])), reeks("fanguh_schuim", "scale", (0, [1, 1, 1]), (0.6, [1.12, 1.12, 1.12]), (1.2, [1, 1, 1])),
             reeks(R, "rotation", (0, [-150, 0, 20])), reeks(L, "rotation", (0, [-150, 0, -20]))]
    return {"schuim": (50, val), "flauw": (0, flauw)}, ("fanguh_schuim",), 12.0


def luchtnon_guh(h):
    P = Pop(h, "luchtnon_guh", "zit", *PEACH)
    c, k = P.c, P.k
    gewaad(P, ORANJE, rand_k=GEEL, kraag=GEEL, hals=GEEL, zoom_k=(180, 80, 30))
    haar(P, HAAR_BRUIN, voor=99, zij=99, achter=13.5, vanaf_z=0.5)
    pijl(P)
    P.kbot("luchtnon_haar", lang_haar(P, HAAR_BRUIN, tot=9.0, breed=11.0))
    P.bot("luchtnon_sjaal", BODY, [0, 12, 0], [c([-5.6, 10.6, -4.6], [11.2, 2.6, 9.2], GEEL), c([-5.9, 9.8, -3.4], [11.8, 1.0, 6.8], (232, 176, 60))])
    P.bot("luchtnon_fluit", BODY, [0, 10, -7], [c([-5.6, 10.8, -7.6], [10.4, 0.8, 0.8], HOUT, patroon=strepen(_donker(HOUT, 0.6), 6, 2, liggend=False)), c([4.8, 10.7, -7.7], [0.9, 1.0, 1.0], (226, 196, 140))])
    return P.klaar()


def anim_luchtnon_guh():
    fluit = [reeks("luchtnon_fluit", "position", (0, [0, 0, 0]), (0.4, [0, 4.4, -0.9]), (2.6, [0, 4.4, -0.9]), (3.0, [0, 0, 0])),
             reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [-62, 0, 0]), (2.6, [-62, 0, 0]), (3.0, [0, 0, 0])),
             reeks(L, "rotation", (0, [0, 0, 0]), (0.4, [-62, 0, 0]), (2.6, [-62, 0, 0]), (3.0, [0, 0, 0])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [4, 0, 0]), (0.9, [4, 0, 6]), (1.5, [4, 0, -6]), (2.1, [4, 0, 6]), (2.6, [4, 0, 0]), (3.0, [0, 0, 0]))]
    return {"fluit": (60, fluit)}, (), 12.0


def postguh(h):
    P = Pop(h, "postguh", "zit", *TAN)
    c, k = P.c, P.k
    gewaad(P, A_GROEN, rand_k=A_GEEL, kraag=A_GEEL, zoom_k=A_GEEL)
    P.verf(BODY, LEER, lambda x, y, z, f: (f in ("north", "south")) & (np.abs(y - (7.4 + x * (0.72 if f == "north" else -0.72))) < 0.7))      # the strap of the bag
    haar(P, HAAR_ZWART, voor=99, zij=23.6, achter=17.0, vanaf_z=99)
    P.kbot("postguh_pet", pet(P, A_GROEN, band=A_GEEL, klep=(60, 100, 60)) + [k([-1.0, 25.9, -6.8], [2.0, 1.2, 0.5], GOUD)])
    P.bot("postguh_tas", BODY, [6, 5, -1], [c([4.4, 1.6, -4.6], [3.6, 5.6, 6.6], LEER, patroon=rand(_donker(LEER, 0.7), 3)), c([4.2, 5.4, -4.8], [4.0, 2.0, 7.0], _donker(LEER, 0.85)),
                                           c([7.9, 4.2, -2.0], [0.4, 1.2, 1.4], GOUD), c([5.0, 7.2, -3.8], [2.2, 1.6, 0.4], WIT), c([5.4, 7.2, -2.0], [2.0, 2.2, 0.4], (250, 240, 214))])
    P.bot("postguh_brief", "postguh_tas", [6, 7, 0], [c([5.0, 6.6, 0.2], [2.4, 2.6, 0.4], WIT, patroon=stip((214, 60, 60), 4, cx=16, cy=16))])
    return P.klaar()


def anim_postguh():
    s = [reeks(L, "rotation", (0, [0, 0, 0]), (0.35, [30, -30, 0]), (0.7, [-60, -10, 0]), (1.3, [-60, -10, 0]), (1.7, [30, -30, 0]), (2.0, [0, 0, 0])),
         reeks("postguh_brief", "position", (0, [0, 0, 0]), (0.35, [0, 0, 0]), (0.7, [-1.5, 4.5, -5.5]), (1.3, [-1.5, 4.5, -5.5]), (1.7, [0, 0, 0])),
         reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.35, [10, -20, 0]), (0.7, [6, -8, 0]), (1.3, [6, -8, 0]), (2.0, [0, 0, 0]))]
    return {"sorteer": (40, s)}, (), 8.0


def professor_zei_guh(h):
    P = Pop(h, "professor_zei_guh", "zit", *TAN)
    c, k = P.c, P.k
    kaki, doek = (166, 160, 110), (236, 226, 196)
    gewaad(P, kaki, rand_k=A_BRUIN, kraag=A_BRUIN, hals=doek, zoom_k=A_BRUIN, riem=LEER)
    spikkels(P, (226, 204, 150), 0.07)
    haar(P, HAAR_BRUIN, voor=99, zij=23.6, achter=18.0, vanaf_z=99)
    P.kbot("zei_hoed", [k([-8.0, 25.2, -8.0], [16, 0.8, 15], (214, 196, 150)), k([-5.4, 26.0, -5.2], [10.8, 3.0, 9.6], (214, 196, 150), patroon=zoom(LEER, 6)),
                        k([-4.4, 29.0, -4.2], [8.8, 0.8, 7.6], (214, 196, 150)),
                        k([-6.8, 12.6, 6.0], [13.6, 12.8, 0.6], doek), k([7.4, 13.6, 0.6], [0.6, 11.8, 5.6], doek), k([-8.0, 13.6, 0.6], [0.6, 11.8, 5.6], doek)])
    P.kbot("zei_bril", bril(P, (70, 60, 54)))
    P.kbot("zei_snor", snor(P, HAAR_BRUIN, hang=0.0, breed=4.4, dik=0.8))
    P.bot("zei_rol", L, [3.5, 8, -4], [c([4.4, 6.2, -7.6], [1.8, 1.8, 8.4], (244, 234, 204)), c([4.3, 6.1, -4.6], [2.0, 2.0, 1.2], (200, 60, 60))])
    P.bot("zei_meetlint", R, [-3, 9, -7], [c([-4.3, 8.2, -7.9], [2.4, 2.4, 1.2], (240, 200, 70), patroon=stip((60, 60, 70), 5))])
    P.bot("zei_lint", "zei_meetlint", [-2, 9.3, -7.4], [c([-2.0, 9.0, -7.5], [9.0, 0.7, 0.3], (250, 236, 150), patroon=strepen((60, 60, 70), 4, 1, liggend=False))])
    return P.klaar()


def anim_professor_zei_guh():
    meet = [reeks("zei_lint", "scale", (0, [0, 0, 0]), (0.3, [0.1, 1, 1]), (0.9, [1, 1, 1]), (1.5, [1, 1, 1]), (1.8, [0.1, 1, 1]), (1.85, [0, 0, 0])),
            reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-55, -30, 0]), (1.6, [-55, -30, 0]), (2.0, [0, 0, 0])),
            reeks(L, "rotation", (0, [0, 0, 0]), (0.9, [-55, 30, 0]), (1.6, [-55, 30, 0]), (2.0, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [12, 0, 0]), (1.6, [12, 0, 4]), (2.0, [0, 0, 0]))]
    return {"meet": (40, meet)}, ("zei_lint",), 8.0


def kyoshi_dorpsoudste(h):
    P = Pop(h, "kyoshi_dorpsoudste", "zit", *TAN)
    c, k = P.c, P.k
    blauw = (52, 70, 130)
    gewaad(P, blauw, rand_k=(214, 206, 180), kraag=(214, 206, 180), hals=(120, 140, 190), zoom_k=(36, 50, 100), riem=(214, 206, 180))
    haar(P, HAAR_WIT, voor=99, zij=99, achter=19.0, vanaf_z=2.5)
    wenkbrauw(P, HAAR_WIT, binnen=0.5, buiten=-0.3, dikte=0.7)
    P.kbot("oudste_knot", knot(P, HAAR_WIT, band=blauw, z=2.0, b=2.8, hh=2.8))
    P.kbot("oudste_baard", baard(P, HAAR_WIT, lengte=5.0, breed=7.6) + snor(P, HAAR_WIT, hang=1.6, breed=5.8, dik=0.9)
           + [k([7.2, 14.0, -5.0], [1.2, 6.0, 4.0], HAAR_WIT), k([-8.4, 14.0, -5.0], [1.2, 6.0, 4.0], HAAR_WIT)])
    P.bot("oudste_stok", ROOT, [8, 0, -5], [c([7.5, 0, -5.6], [1.0, 15.0, 1.0], HOUT), c([7.2, 15.0, -5.9], [1.6, 1.6, 1.6], _donker(HOUT, 0.75))])
    return P.klaar()


def waterstam_wachter(h):
    P = Pop(h, "waterstam_wachter", "zit", *BRUIN)
    c, k = P.c, P.k
    gewaad(P, W_DONKER, rand_k=BONT, zoom_k=BONT, riem=(30, 44, 84))
    P.verf(HEAD, WIT, lambda x, y, z, f: (f == "north") & (y > 17.9) & (y < 18.8) & (np.abs(x) > 2.3))                # the face stripe under the eyes
    P.verf(HEAD, W_LICHT, lambda x, y, z, f: (f == "north") & (y > 16.6) & (y < 17.4) & (np.abs(x) > 2.6) & (np.abs(x) < 6.0))
    wolf, donker = (214, 216, 224), (150, 154, 168)
    P.kbot("wachter_helm", [k([-7.5, 23.9, -7.5], [15, 2.8, 14], wolf), k([-6.0, 26.7, -6.0], [12, 0.9, 11], wolf),
                            k([-2.8, 24.2, -10.6], [5.6, 2.2, 3.4], wolf), k([-1.2, 25.0, -11.1], [2.4, 1.2, 0.8], ZWART),
                            k([-2.4, 23.5, -10.4], [0.8, 0.9, 0.8], WIT), k([1.6, 23.5, -10.4], [0.8, 0.9, 0.8], WIT),
                            k([-4.6, 25.6, -7.7], [1.6, 0.9, 0.5], (250, 214, 90)), k([3.0, 25.6, -7.7], [1.6, 0.9, 0.5], (250, 214, 90)),
                            k([2.0, 27.4, -4.0], [2.6, 2.4, 1.6], wolf), k([2.6, 29.8, -3.8], [1.4, 1.6, 1.2], donker),
                            k([-4.6, 27.4, -4.0], [2.6, 2.4, 1.6], wolf), k([-4.0, 29.8, -3.8], [1.4, 1.6, 1.2], donker),
                            k([7.2, 14.5, -5.5], [1.2, 9.6, 11.4], donker), k([-8.4, 14.5, -5.5], [1.2, 9.6, 11.4], donker), k([-7.2, 13.5, 5.8], [14.4, 10.6, 0.9], donker)])
    P.bot("wachter_kraag", BODY, [0, 12, 0], bontkraag(P, (206, 206, 214), dik=2.8, y=10.8))
    P.bot("wachter_speer", ROOT, [-8.8, 0, -2.5], speer(P, -8.8, 28.0))
    return P.klaar()


def anim_waterstam_wachter():
    halt = [reeks(L, "rotation", (0, [0, 0, 0]), (0.25, [-88, 0, 0]), (1.1, [-88, 0, 0]), (1.5, [0, 0, 0])),
            reeks("wachter_speer", "rotation", (0, [0, 0, 0]), (0.25, [0, 0, -14]), (1.1, [0, 0, -14]), (1.5, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.25, [6, 0, 0]), (1.1, [6, 0, 0]), (1.5, [0, 0, 0]))]
    return {"halt": (30, halt)}, (), 8.0


def zonnekrijger_guh(h):
    P = Pop(h, "zonnekrijger_guh", "zit", *BRUIN)
    c, k = P.c, P.k
    rood = (190, 50, 44)
    P.verf(BODY, rood, lambda x, y, z, f: y < 5.0)
    P.verf(BODY, GOUD, lambda x, y, z, f: (y > 4.6) & (y < 5.8))
    P.verf(BODY, GOUD, lambda x, y, z, f: (f == "north") & (np.abs(x) < 1.6) & (y < 4.6))                              # the front flap
    buikvlek(P, tuple(min(255, int(v * 1.12) + 8) for v in P.huid), breed=3.2, y0=5.6, y1=11.6)
    P.verf(HEAD, rood, lambda x, y, z, f: (f == "north") & (np.abs(x) > 4.6) & (np.abs(x) < 6.9) & (((y - 15.2) % 1.5) < 0.7) & (y > 15.0) & (y < 18.6))   # cheek stripes
    P.verf(HEAD, GOUD, lambda x, y, z, f: (f == "north") & (np.hypot(x, y - 23.3) < 0.8))
    haar(P, HAAR_ZWART, voor=99, zij=23.8, achter=14.0, vanaf_z=99)
    zon = stip((255, 226, 110), 8, ring=(226, 150, 40))
    veren = []
    for x, hh, kl in ((-6.4, 5.0, rood), (-4.2, 7.5, (236, 120, 40)), (-2.0, 9.5, rood), (0.2, 10.5, (250, 190, 60)), (2.4, 9.5, rood), (4.6, 7.5, (236, 120, 40)), (6.8, 5.0, rood)):
        veren += [k([x - 1.0, 25.4, 1.2], [2.0, hh, 0.9], kl), k([x - 0.6, 25.4 + hh, 1.3], [1.2, 1.2, 0.7], (250, 226, 150))]
    P.kbot("zonnekrijger_tooi", [k([-7.4, 24.1, -7.4], [14.8, 1.5, 13.8], GOUD), k([-2.8, 23.6, -8.1], [5.6, 5.6, 0.8], GOUD, patroon=zon)] + veren)
    P.bot("zonnekrijger_ring_links", L, [3.5, 10, -4], [c([1.2, 6.7, -5.4], [3.6, 4.1, 0.9], GOUD), c([1.2, 6.7, -4.0], [3.6, 4.1, 0.6], GOUD)])
    P.bot("zonnekrijger_ring_rechts", R, [-3.5, 10, -4], [c([-4.8, 6.7, -5.4], [3.6, 4.1, 0.9], GOUD), c([-4.8, 6.7, -4.0], [3.6, 4.1, 0.6], GOUD)])
    P.bot("zonnekrijger_ketting", BODY, [0, 11, -4], [c([-4.4, 10.8, -4.5], [8.8, 1.4, 0.7], GOUD), c([-1.2, 9.0, -4.7], [2.4, 2.0, 0.7], GOUD, patroon=zon)])
    return P.klaar()


def anim_zonnekrijger_guh():
    groet = armen_op(0.1, 1.9, -160, 28) + [reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [-24, 0, 0]), (1.6, [-24, 0, 0]), (2.0, [0, 0, 0])),
                                           reeks(BODY, "scale", (0, [1, 1, 1]), (0.8, [1.04, 1.06, 1.04]), (2.0, [1, 1, 1]))]
    return {"zonnegroet": (40, groet)}, (), 12.0


# --- the fire nation's Mikas with a name ---------------------------------------------------------------------------------------
def wijzenhoed(P, hoog=1.0, flap=True):
    """The tall pointed hat of the fire sages with its golden flame, and the cloth that hangs over the ears."""
    k = P.k
    em = vlam(GOUD, (255, 236, 150))
    out = [k([-4.8, 25.2, -5.6], [9.6, 3.0 * hoog, 8.6], V_ROOD, patroon=zoom(GOUD, 6)), k([-3.7, 25.2 + 3.0 * hoog, -4.4], [7.4, 3.0 * hoog, 6.4], V_ROOD),
           k([-2.6, 25.2 + 6.0 * hoog, -3.2], [5.2, 3.0 * hoog, 4.2], V_ROOD), k([-1.5, 25.2 + 9.0 * hoog, -2.0], [3.0, 2.4 * hoog, 2.2], V_DONKER),
           k([-2.0, 25.6, -6.1], [4.0, 5.6 * hoog, 0.6], V_DONKER, patroon=em)]
    if flap:
        out += [k([-7.3, 24.0, 5.4], [14.6, 1.6, 1.0], V_ROOD), k([-6.4, 14.0, 6.1], [12.8, 10.6, 0.7], V_ROOD, patroon=zoom(GOUD, 4))]
    return out


def vuurwijze_shyu(h):
    P = Pop(h, "vuurwijze_shyu", "mika", 0.02, 0.8, 1.02)
    c, k = P.c, P.k
    gewaad(P, V_ROOD, kraag=GOUD, zoom_k=GOUD, riem=V_DONKER)
    # a kind face: the Mika's scowl painted away, soft grey brows that slope outwards
    ox, oy, r = P.oog
    vacht = tuple(int(v) for v in np.median(P.a[P.gezicht[1]:P.gezicht[3], P.gezicht[0]:P.gezicht[2]][~P.vast[P.gezicht[1]:P.gezicht[3], P.gezicht[0]:P.gezicht[2]]].reshape(-1, 4), 0)[:3])
    P.verf(HEAD, vacht, lambda x, y, z, f: (f == "north") & (y > oy + r * 0.45), over=True)
    wenkbrauw(P, (206, 206, 212), binnen=0.7, buiten=-0.5, dikte=0.5, hoog=0.2)
    P.kbot("shyu_hoed", wijzenhoed(P, hoog=0.72), pivot=(0, 26, 0), rot=[0, 0, 9])
    P.kbot("shyu_baard", sik(P, (226, 226, 230), 2.6, 2.0) + snor(P, (226, 226, 230), hang=1.2, breed=5.0, dik=0.6))
    P.bot("shyu_mantel", BODY, [0, 5, 0], [c([-7.0, 0.6, -2.6], [14.0, 10.2, 2.2], V_DONKER, patroon=rand(GOUD, 3))])
    return P.klaar()


def anim_vuurwijze_shyu():
    wenk = [reeks("leg_front_right", "position", (0, [0, 0, 0]), (0.25, [0, 5, -1.5]), (1.2, [0, 5, -1.5]), (1.5, [0, 0, 0])),
            reeks("leg_front_right", "rotation", (0, [0, 0, 0]), (0.25, [-40, 0, 0]), (0.45, [30, 0, 0]), (0.65, [-40, 0, 0]), (0.85, [30, 0, 0]), (1.05, [-40, 0, 0]), (1.5, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, RUST_MIKA), (0.3, [-6, -14, 10]), (1.2, [-6, -14, 10]), (1.5, RUST_MIKA))]
    return {"wenk": (30, wenk)}, (), 8.0


def vuurnatie_juf(h):
    P = Pop(h, "vuurnatie_juf", "mika", 0.0, 0.9, 1.0)
    c, k = P.c, P.k
    gewaad(P, V_DONKER, kraag=V_ROOD, zoom_k=GOUD, riem=V_ZWART)
    haar(P, HAAR_ZWART, voor=24.6, zij=23.0, achter=16.0)
    P.kbot("juf_knot", knot(P, HAAR_ZWART, band=V_ROOD, z=0.5, b=3.4, hh=3.2) + [k([-3.6, 27.8, 0.2], [7.2, 0.5, 0.5], GOUD), k([-0.5, 29.2, 0.0], [1.0, 1.6, 1.0], GOUD)])
    P.kbot("juf_bril", bril(P, (30, 28, 34), laag=-0.2, smal=True, z=-8.2))
    P.bot("juf_kraag", BODY, [0, 5, 0], [c([-7.0, 0.6, -2.6], [14.0, 10.2, 2.0], V_ROOD, patroon=rand(GOUD, 3))])
    P.bot("juf_stok", "leg_front_right", [-4, 1.5, -12.5], [c([-4.5, 1.2, -13.2], [0.8, 17.0, 0.8], HOUT), c([-4.7, 18.2, -13.4], [1.2, 1.4, 1.2], V_ROOD)], rot=[-28, 0, 14])
    return P.klaar()


def anim_vuurnatie_juf():
    tik = [reeks("juf_stok", "rotation", (0, [0, 0, 0]), (0.2, [-30, 0, 0]), (0.32, [16, 0, 0]), (0.5, [-30, 0, 0]), (0.62, [16, 0, 0]), (0.8, [-30, 0, 0]), (0.92, [16, 0, 0]), (1.5, [0, 0, 0])),
           reeks(HEAD, "rotation", (0, RUST_MIKA), (0.2, [-16, 0, 0]), (0.92, [-2, 0, 0]), (1.5, RUST_MIKA))]
    return {"tik": (30, tik)}, (), 8.0


# --- the Sintelstrand players ----------------------------------------------------------------------------------------------------
def toneelspeler(h, look=""):
    kind = "toneelspeler_guh" + (f"_{look}" if look else "")
    if look == "toph":
        P = kei_guh(h, kind, toneel=True)
        return P.klaar()
    P = Pop(h, kind, "zit")
    c, k = P.c, P.k
    naad = strepen((120, 90, 70), 6, 2)
    if look == "":                                           # "Aang-guh": a bald cap with a seam, the arrow crooked, too much blush
        gewaad(P, (240, 150, 50), rand_k=(250, 220, 90), zoom_k=(250, 220, 90))
        P.verf(BODY, (250, 220, 90), lambda x, y, z, f: (y > 10.2) & (((x + z + 20) % 3.0) < 1.6 + (y - 10.2)))        # a jagged paper collar
        blos(P, (255, 84, 116), 2.0)
        vel = (242, 206, 168)
        P.kbot("toneel_kaalkop", [k([-7.3, 23.6, -7.3], [14.6, 1.3, 13.6], vel, patroon=zoom((150, 110, 90), 3)), k([-6.8, 24.9, -6.3], [13.6, 0.5, 11.6], vel),
                                  k([-5.8, 25.4, -5.3], [11.6, 0.9, 9.6], vel),
                                  k([0.2, 26.3, -4.6], [2.2, 0.3, 8.4], PIJL), k([1.2, 25.5, -6.6], [2.2, 0.4, 2.2], PIJL), k([1.6, 23.2, -7.6], [2.2, 2.4, 0.4], PIJL),
                                  k([0.9, 22.2, -7.6], [3.6, 1.1, 0.4], PIJL), k([1.9, 21.3, -7.6], [1.6, 1.0, 0.4], PIJL)])
        P.bot("toneel_staf", L, [3, 9, -7], [c([2.6, 4.0, -7.6], [0.8, 20, 0.8], (190, 150, 100), patroon=karton((190, 150, 100)))])
    elif look == "katara":                                   # woollen hair loops twice too big, tears painted on
        gewaad(P, (70, 120, 200), rand_k=WIT, kraag=WIT, zoom_k=WIT, riem=WIT)
        ox, oy, r = P.oog
        P.verf(HEAD, (110, 190, 250), lambda x, y, z, f: (f == "north") & (np.abs(np.abs(x) - ox) < 0.55) & (y < oy - r + 0.1) & (y > 15.0), over=True)
        P.verf(HEAD, (110, 190, 250), lambda x, y, z, f: (f == "north") & (np.hypot(np.abs(x) - ox, y - 15.0) < 0.95), over=True)
        wol = (120, 74, 44)
        wp = kern.gebreid(wol)
        P.kbot("toneel_pruik", kap(P, wol, wp) + [k([5.4, 12.0, -9.4], [3.4, 13.0, 3.4], wol, patroon=wp), k([-8.8, 12.0, -9.4], [3.4, 13.0, 3.4], wol, patroon=wp),
                                                  k([4.0, 23.0, -9.0], [4.4, 3.2, 3.0], wol, patroon=wp), k([-8.4, 23.0, -9.0], [4.4, 3.2, 3.0], wol, patroon=wp),
                                                  k([5.6, 15.4, -9.7], [3.0, 1.6, 3.8], (96, 176, 240)), k([-8.6, 15.4, -9.7], [3.0, 1.6, 3.8], (96, 176, 240)),
                                                  k([-3.4, 25.6, 1.0], [6.8, 5.0, 6.0], wol, patroon=wp), k([-1.6, 4.0, 6.4], [3.2, 18.0, 2.8], wol, patroon=wp)])
        P.bot("toneel_ketting", BODY, [0, 11, -4], [c([-3.6, 11.2, -4.6], [7.2, 0.9, 0.6], (60, 90, 170)), c([-1.1, 10.2, -4.8], [2.2, 1.8, 0.6], (150, 214, 250))])
    elif look == "sokka":                                    # a cardboard boomerang, a drawn-on wolf tail, a fake belly of food
        gewaad(P, (80, 124, 190), rand_k=WIT, zoom_k=(50, 84, 150))
        bord = (196, 160, 110)
        kp = karton(bord)
        P.kbot("toneel_staartje", [k([-0.3, 24.6, -1.0], [0.6, 7.0, 5.0], bord, patroon=strepen((70, 50, 40), 5, 2, liggend=False)), k([-0.3, 24.6, 4.0], [0.6, 4.6, 2.4], bord, patroon=kp),
                                   k([-7.3, 24.0, -7.3], [14.6, 1.0, 13.6], (70, 110, 180))])
        P.bot("toneel_buik", BODY, [0, 5, -5], [c([-5.2, 1.6, -7.4], [10.4, 8.0, 3.6], (236, 214, 170), patroon=stip((214, 150, 110), 3)), c([-5.6, 8.6, -7.0], [11.2, 0.6, 0.6], HOUT),
                                               c([-5.6, 2.2, -7.0], [11.2, 0.6, 0.6], HOUT)])
        P.bot("toneel_boemerang", R, [-3, 10, -7], [c([-4.2, 9.6, -7.9], [1.4, 8.0, 0.6], bord, patroon=kp), c([-9.2, 16.4, -7.9], [6.4, 1.4, 0.6], bord, patroon=kp)])
    else:                                                    # "Zuka": a guh with a Mika mask on a stick, the scar on the WRONG side
        gewaad(P, V_ROOD, rand_k=GOUD, kraag=GOUD, hals=V_DONKER, zoom_k=GOUD, riem=V_ZWART)
        P.kbot("toneel_knot", knot(P, ZWART, band=GOUD, z=0.5, b=3.0, hh=3.4) + [k([-7.3, 24.0, -7.3], [14.6, 1.0, 13.6], ZWART)])
        roze = (240, 178, 196)
        P.bot("toneel_masker", L, [3, 9, -7], [c([2.6, 5.0, -7.9], [0.8, 9.0, 0.8], HOUT),
                                              c([-1.0, 13.0, -9.4], [10.5, 9.0, 0.6], roze, patroon=mikamasker(True)),
                                              c([6.6, 20.6, -9.3], [4.4, 3.6, 0.5], roze, patroon=stip((226, 140, 164), 8)), c([-2.6, 20.6, -9.3], [4.4, 3.6, 0.5], roze, patroon=stip((226, 140, 164), 8))],
              rot=[0, 0, -8])
        P.bot("toneel_mantel", BODY, [0, 12, 0], mantel(P, V_DONKER, patroon=rand(GOUD, 4)))
    return P.klaar()


def anim_toneelspeler_guh():
    drama = [reeks(R, "rotation", (0, [0, 0, 0]), (0.4, [-150, -28, 0]), (1.6, [-150, -28, 0]), (2.0, [0, 0, 0])),
             reeks(L, "rotation", (0, [0, 0, 0]), (0.4, [-40, 40, 0]), (1.6, [-40, 40, 0]), (2.0, [0, 0, 0])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.4, [-24, 0, 12]), (1.6, [-24, 0, 12]), (2.0, [0, 0, 0])),
             reeks(BODY, "rotation", (0, [0, 0, 0]), (0.4, [-8, 0, 0]), (1.6, [-8, 0, 0]), (2.0, [0, 0, 0]))]
    return {"drama": (40, drama), "buig": (30, buig())}, (), 8.0


# =============================================================================================================================
# the table (the same rows as Java's castvolk.CastVolk; check(h) compares)
# =============================================================================================================================
from features import einde2_cast_volk_burgers as burgers  # noqa: E402


def _rij(kind, bouw, anim, lijf="zit", schaal=1.0, basis=None, mix=0, doorzichtig=False, gloed=False, bestand=None):
    return dict(kind=kind, basis=basis or kind, lijf=lijf, schaal=schaal, mix=mix, doorzichtig=doorzichtig, gloed=gloed, bouw=bouw, anim=anim,
                bestand=bestand or basis or kind)


CAST = [
    _rij("guhatso", guhatso, anim_guhatso, doorzichtig=True, gloed=True),
    _rij("pakguh", pakguh, anim_pakguh),
    _rij("bumi_guh", bumi_guh, anim_bumi_guh),
    _rij("aardekoning_guh", aardekoning_guh, anim_aardekoning_guh),
    _rij("arnook_guh", arnook_guh, anim_knik, schaal=1.1),
    _rij("piandao_guh", piandao_guh, anim_piandao_guh),
    _rij("werktuigguh", werktuigguh, anim_werktuigguh),
    _rij("roku_guh", roku_guh, anim_roku_guh, schaal=1.15, doorzichtig=True, gloed=True),
    _rij("roku_guh_beeld", lambda h: roku_guh(h, "roku_guh_beeld"), anim_roku_guh, schaal=1.15, basis="roku_guh", bestand="roku_guh_beeld"),
    _rij("kyoshi_guh", kyoshi_guh, anim_kyoshi_guh, schaal=1.25, doorzichtig=True, gloed=True),
    _rij("kyoshi_guh_beeld", lambda h: kyoshi_guh(h, "kyoshi_guh_beeld"), anim_kyoshi_guh, schaal=1.25, basis="kyoshi_guh", bestand="kyoshi_guh_beeld"),
    _rij("joo_dee_guh", joo_dee_guh, anim_joo_dee_guh),
    _rij("koolman", koolman, anim_koolman),
    _rij("loketguh", loketguh, anim_loketguh),
    _rij("kei_guh", kei_guh, anim_kei_guh, schaal=1.3),
    _rij("aardrommel_omroeper", aardrommel_omroeper, anim_aardrommel_omroeper, schaal=0.9),
    _rij("fanguh", fanguh, anim_fanguh, schaal=0.9),
    _rij("luchtnon_guh", luchtnon_guh, anim_luchtnon_guh),
    _rij("postguh", postguh, anim_postguh),
    _rij("professor_zei_guh", professor_zei_guh, anim_professor_zei_guh),
    _rij("kyoshi_dorpsoudste", kyoshi_dorpsoudste, anim_knik, schaal=0.9),
    _rij("kyoshi_krijgsterguh", burgers.kyoshi_krijgsterguh, burgers.anim_kyoshi_krijgsterguh, mix=3),
    _rij("waterstam_guh", burgers.waterstam_guh, burgers.anim_zwaai, mix=4),
    _rij("waterstam_wachter", waterstam_wachter, anim_waterstam_wachter, schaal=1.1),
    _rij("ba_sing_guh_burger", burgers.ba_sing_guh_burger, burgers.anim_ba_sing_guh_burger, mix=5),
    _rij("omashguh_burger", burgers.omashguh_burger, burgers.anim_zwaai, mix=3),
    _rij("kyoshi_dorpsguh", burgers.kyoshi_dorpsguh, burgers.anim_zwaai, mix=3),
    _rij("gevangene_guh", burgers.gevangene_guh, burgers.anim_gevangene_guh, mix=3),
    _rij("zonnekrijger_guh", zonnekrijger_guh, anim_zonnekrijger_guh, schaal=1.05),
    _rij("vuurwijze_mika", burgers.vuurwijze_mika, burgers.anim_vuurwijze_mika, lijf="mika", mix=2),
    _rij("vuurwijze_shyu", vuurwijze_shyu, anim_vuurwijze_shyu, lijf="mika"),
    _rij("vuurnatie_burger_mika", burgers.vuurnatie_burger_mika, burgers.anim_vuurnatie_burger_mika, lijf="mika", mix=5),
    _rij("vuurnatie_juf", vuurnatie_juf, anim_vuurnatie_juf, lijf="mika"),
    _rij("vuurnatie_leerling", burgers.vuurnatie_leerling, burgers.anim_vuurnatie_leerling, lijf="mika", schaal=0.7, mix=3),
    _rij("toneelspeler_guh", lambda h: toneelspeler(h, ""), anim_toneelspeler_guh),
    _rij("toneelspeler_guh_katara", lambda h: toneelspeler(h, "katara"), anim_toneelspeler_guh, basis="toneelspeler_guh"),
    _rij("toneelspeler_guh_sokka", lambda h: toneelspeler(h, "sokka"), anim_toneelspeler_guh, basis="toneelspeler_guh"),
    _rij("toneelspeler_guh_toph", lambda h: toneelspeler(h, "toph"), anim_toneelspeler_guh, schaal=1.3, basis="toneelspeler_guh"),
    _rij("toneelspeler_guh_zuka", lambda h: toneelspeler(h, "zuka"), anim_toneelspeler_guh, basis="toneelspeler_guh"),
]
RIJ = {r["kind"]: r for r in CAST}
MIXBOTTEN = {}                 # kind -> [[bones of set 0], ...], known after build(h)


def animaties(kind):
    """[(naam, ticks)] besides idle: praat first, then the names of the row of the character; 0 ticks = a pose that lasts."""
    a = RIJ[kind]["anim"]()[0]
    return [("praat", 24)] + [(n, t) for n, (t, _) in a.items()]


def _animatiebestand(h, rij):
    a = rij["anim"]()
    anims, verborgen, lengte = a[0], a[1], a[2]
    lijf = rij["lijf"]
    if rij["bestand"].endswith("_beeld"):                    # a statue: every name, nothing moves
        stil = anim_beeld([(n, t) for n, (t, _) in anims.items()])
        stil["praat"] = (24, [spoor(HEAD, "rotation", {0: (0, 0, 0)})])
        return kern.bestand(h, rij["bestand"], lijf, [spoor(HEAD, "rotation", {0: (0, 0, 0)})], stil, (), 4.0)
    eerste = next(iter(anims.values()))[1]
    sig = a[3] if len(a) > 3 else eerste
    alle = {"praat": (24, praat(lijf))}
    alle.update(anims)
    return kern.bestand(h, rij["bestand"], lijf, kern.idle(h, lijf, sig, lengte, start=lengte / 2 + 0.4), alle, verborgen, lengte)


def build(h, alleen=None):
    geschreven = set()
    for rij in CAST:
        if alleen and rij["kind"] not in alleen and rij["basis"] not in alleen:
            continue
        P = rij["bouw"](h)
        MIXBOTTEN[rij["kind"]] = [P.mix[i] for i in sorted(P.mix)]
        if rij["bestand"] not in geschreven:
            _animatiebestand(h, rij)
            geschreven.add(rij["bestand"])


# =============================================================================================================================
# the self-check: the files, the bones the animations move, the mix sets, and the table against the Java one
# =============================================================================================================================
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "castvolk", "CastVolk.java")


def java_rij(rij):
    """The row of this kind as CastVolk.java spells it."""
    kind = rij["kind"]
    anim = ",".join(f"{n}:{t}" for n, t in animaties(kind))
    mix = "|".join(" ".join(s) for s in MIXBOTTEN.get(kind, []))
    b = lambda v: "true" if v else "false"
    return (f'{kind.upper()}(Kind.{kind.upper()}, Kind.{rij["basis"].upper()}, Kind.{rij["bestand"].upper()}, "{rij["lijf"]}", {b(rij["doorzichtig"])}, {b(rij["gloed"])},\n'
            f'            "{anim}",\n            "{mix}")')


def check(h):
    fouten = []
    geo_dir = os.path.join(h.A, "geckolib", "models", "entity")
    ani_dir = os.path.join(h.A, "geckolib", "animations", "entity")
    java = open(JAVA, encoding="utf-8").read().replace("\r\n", "\n") if os.path.exists(JAVA) else None
    if java is None:
        fouten.append(f"{JAVA} is missing")
    for rij in CAST:
        kind = rij["kind"]
        pad = os.path.join(geo_dir, f"guh_npc_{kind}.geo.json")
        if not os.path.exists(pad) or not os.path.exists(os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            fouten.append(f"{kind}: no model or texture")
            continue
        geo = json.load(open(pad, encoding="utf-8"))["minecraft:geometry"][0]
        botten = {b["name"] for b in geo["bones"]}
        if geo["description"]["identifier"] != f"geometry.guh_npc_{kind}":
            fouten.append(f"{kind}: identifier")
        for b in kern.BASISBOTTEN[rij["lijf"]]:
            if b not in botten:
                fouten.append(f"{kind}: the base bone {b} is gone")
        mixen = sorted(b for b in botten if b.startswith("mix_"))
        if sorted(sum(MIXBOTTEN.get(kind, []), [])) != mixen or len(MIXBOTTEN.get(kind, [])) != rij["mix"]:
            fouten.append(f"{kind}: mix sets {len(MIXBOTTEN.get(kind, []))} / {rij['mix']}, bones {mixen}")
        if rij["gloed"] != os.path.exists(os.path.join(h.TEX, "entity", f"npc_{kind}_glowmask.png")):
            fouten.append(f"{kind}: glowmask")
        a = Image.open(os.path.join(h.TEX, "entity", f"npc_{kind}.png"))
        if a.size != (512, 512):
            fouten.append(f"{kind}: sheet size {a.size}")
        apad = os.path.join(ani_dir, f"guh_npc_{rij['bestand']}.animation.json")
        if not os.path.exists(apad):
            fouten.append(f"{kind}: no animation file")
            continue
        anims = json.load(open(apad, encoding="utf-8"))["animations"]
        for naam, ticks in [("idle", -1)] + animaties(kind):
            an = anims.get(f"animation.guh_sitting.{naam}")
            if an is None:
                fouten.append(f"{kind}: animation {naam} is missing")
                continue
            for bot in an["bones"]:
                if bot not in botten:
                    fouten.append(f"{kind}: {naam} moves the bone {bot}, which the model does not have")
                if bot.startswith("mix_"):
                    fouten.append(f"{kind}: {naam} moves a mix bone")
            if ticks > 0 and abs(an["animation_length"] * 20 - ticks) > 0.01:
                fouten.append(f"{kind}: {naam} is {an['animation_length']} s, the table says {ticks} ticks")
        if java is not None and java_rij(rij) not in java:
            fouten.append(f"{kind}: its row in CastVolk.java differs from the table:\n{java_rij(rij)}")
    if java is not None and len(re.findall(r"^\s+[A-Z_]+\(Kind\.", java, re.M)) != len(CAST):
        fouten.append("CastVolk.java has another number of rows than the table")
    return fouten


# =============================================================================================================================
# offline previews
# =============================================================================================================================
def _pose(geo, anims, naam, t):
    """The model at second t of an animation: rotations on the bones, shifts into the cubes, scale 0 = gone."""
    an = anims[f"animation.guh_sitting.{naam}"]
    ouders = {b["name"]: b.get("parent") for b in geo["bones"]}

    def waarde(sleutels):
        ks = sorted((float(k), v) for k, v in sleutels.items())
        if t <= ks[0][0]:
            return ks[0][1]
        for (t0, v0), (t1, v1) in zip(ks, ks[1:]):
            if t0 <= t <= t1:
                f = (t - t0) / max(t1 - t0, 1e-6)
                return [v0[i] + (v1[i] - v0[i]) * f for i in range(3)]
        return ks[-1][1]
    weg, schuif = set(), {}
    for bot, kan in an["bones"].items():
        b = next((x for x in geo["bones"] if x["name"] == bot), None)
        if b is None:
            continue
        if "rotation" in kan:
            r, basis = waarde(kan["rotation"]), b.get("rotation", [0, 0, 0])
            b["rotation"] = [basis[i] + r[i] for i in range(3)]
        if "scale" in kan and max(waarde(kan["scale"])) < 0.05:
            weg.add(bot)
        if "position" in kan:
            schuif[bot] = waarde(kan["position"])

    def onder(naam_, voorouders):
        while naam_:
            if naam_ in voorouders:
                return naam_
            naam_ = ouders.get(naam_)
        return None
    for b in geo["bones"]:
        n = b["name"]
        while n:
            if n in schuif:
                d = schuif[n]
                b["pivot"] = [b["pivot"][i] + d[i] for i in range(3)]
                for c in b.get("cubes", []):
                    c["origin"] = [c["origin"][i] + d[i] for i in range(3)]
            n = ouders.get(n)
    geo["bones"] = [b for b in geo["bones"] if not onder(b["name"], weg)]


def preview(out, alleen=None, poses=False, grootte=320):
    """Every kind from the front and three-quarter (every mix set apart) into out/cast_volk_<n>.png, six to a sheet; with
    poses=True instead one tile per named animation, halfway through it."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo_dir = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    ani_dir = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "animations", "entity")
    tegels = []
    for rij in CAST:
        kind = rij["kind"]
        if alleen and kind not in alleen and rij["basis"] not in alleen:
            continue
        geo_file = json.load(open(os.path.join(geo_dir, f"guh_npc_{kind}.geo.json"), encoding="utf-8"))
        anims = json.load(open(os.path.join(ani_dir, f"guh_npc_{rij['bestand']}.animation.json"), encoding="utf-8"))["animations"]
        botten = [b["name"] for b in geo_file["minecraft:geometry"][0]["bones"]]
        verborgen = set(rij["anim"]()[1])
        sets = sorted({int(b.split("_")[1]) for b in botten if b.startswith("mix_")}) or [None]
        for s in sets:
            hide = {b for b in botten if b.startswith("mix_") and int(b.split("_")[1]) != s}
            label = kind + ("" if s is None else f" [{s}]")
            if poses:
                beurten = [(n, (t / 20.0) * 0.5 if t else 99.0) for n, t in animaties(kind)[1:]]
            else:
                beurten = [(None, 0)]
            for naam, t in beurten:
                g = json.loads(json.dumps(geo_file))
                geo = g["minecraft:geometry"][0]
                geo["bones"] = [b for b in geo["bones"] if b["name"] not in hide and (naam or b["name"] not in verborgen)]
                if naam:
                    _pose(geo, anims, naam, t)
                pad = os.path.join(out, "_pose.geo.json")
                json.dump(g, open(pad, "w", encoding="utf-8"))
                q = wr.geo_quads(pad, f"guhs:entity/npc_{kind}")
                for yaw, pitch in (((32, -14),) if naam else ((6, -8), (38, -16))):
                    img = kern.teken(q, yaw, pitch, grootte)
                    ImageDraw.Draw(img).text((6, 4), label + (f" : {naam}" if naam else ""), fill=(40, 30, 60, 255))
                    tegels.append(img)
    per = 12
    for s in range(0, len(tegels), per):
        deel = tegels[s:s + per]
        blad_ = Image.new("RGBA", (grootte * 4, grootte * ((len(deel) + 3) // 4)), (236, 232, 240, 255))
        for i, t in enumerate(deel):
            blad_.paste(t, ((i % 4) * grootte, (i // 4) * grootte))
        blad_.save(os.path.join(out, f"cast_volk_{'pose_' if poses else ''}{s // per}.png"))
    if os.path.exists(os.path.join(out, "_pose.geo.json")):
        os.remove(os.path.join(out, "_pose.geo.json"))
    return (len(tegels) + per - 1) // per
