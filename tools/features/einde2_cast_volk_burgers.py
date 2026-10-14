"""
einde2, slice cast_volk - the crowds (CONTRACT_E2 11.5 layer 1 and 11.2 D): the citizens, villagers, warriors, sages, prisoners
and pupils. ONE function, `burger(h, kind, lijf, kleuren, sets)`, dresses a base body from a table row: the fur, a painted
robe, what every member wears (`gemeen`), and the mix sets: every bone of set i is named `mix_<i>_<ding>`, and the client
shows one set per character, chosen from where it stands. A set brings its own colours (a coat of cubes over the painted
robe, a hat, hair, a tool), so the members of one kind differ in more than a hat.
"""
import numpy as np

from features import einde2_cast_volk_kern as kern
from features.einde2_cast_volk_kern import Pop, blad, glas, net, rand, reeks, ruit, schud, stip, strepen, toon, vlam, zoom, _donker
from features.einde2_cast_volk_delen import *  # noqa: F401,F403
from features import einde2_cast_volk_delen as delen

R, L, HEAD, BODY, ROOT = "arm_right", "arm_left", "head", "body", "root"


class Set:
    """Names the bones of one mix set."""

    def __init__(self, P, i):
        self.P, self.i = P, i

    def kop(self, ding, cubes, **kw):
        return self.P.kbot(f"mix_{self.i}_{ding}", cubes, **kw)

    def bot(self, ding, ouder, pivot, cubes, rot=None):
        return self.P.bot(f"mix_{self.i}_{ding}", ouder, pivot, cubes, rot)

    def jas(self, kleur, patroon=None, kraag=None):
        """A coat of its own colour over the painted robe (sitting guh), or a cloth over the back (Mika)."""
        P = self.P
        if P.lijf == "mika":
            self.bot("jas", BODY, [0, 5, 5], mikajas(P, kleur, patroon))
            return
        lijf, links, rechts = jas(P, kleur, patroon)
        if kraag:
            lijf += [P.c([-5.6, 11.3, -4.6], [11.2, 2.0, 9.2], kraag)]
        self.bot("jas", BODY, [0, 8, 0], lijf)
        self.bot("mouw_links", L, [3.5, 10, -4], links)
        self.bot("mouw_rechts", R, [-3.5, 10, -4], rechts)


def burger(h, kind, lijf, kleuren, sets, gemeen=None):
    """kleuren: vacht (hue, sat, val), gewaad + the keyword colours of delen.gewaad, haar (painted, optional).
    sets: one function (Set) per mix set. gemeen(P): what all of them wear."""
    kl = dict(kleuren)
    P = Pop(h, kind, lijf, *kl.pop("vacht"))
    haar_k = kl.pop("haar", None)
    gewaad(P, kl.pop("gewaad"), **kl)
    if haar_k:
        haar(P, haar_k, voor=24.8, zij=23.4, achter=16.0)
    if gemeen:
        gemeen(P)
    for i, f in enumerate(sets):
        f(Set(P, i))
    if sorted(P.mix) != list(range(len(sets))):
        raise SystemExit(f"einde2_cast_volk: {kind}: a mix set without a bone")
    return P.klaar()


# =============================================================================================================================
# Kyoshi-krijgsterguhs (mix 3)
# =============================================================================================================================
def kyoshi_krijgsterguh(h):
    groen, donker = (70, 132, 84), (38, 84, 58)

    def gemeen(P):
        c = P.c
        schmink(P)
        P.bot("krijgster_harnas", BODY, [0, 8, -4], [c([-4.5, 4.8, -4.7], [9.0, 6.4, 0.8], donker, patroon=strepen(GOUD, 8, 2)),
                                                    c([4.9, 10.4, -3.2], [2.6, 2.0, 6.4], donker, patroon=rand(GOUD, 3)), c([-7.5, 10.4, -3.2], [2.6, 2.0, 6.4], donker, patroon=rand(GOUD, 3)),
                                                    c([-5.4, 11.8, -4.4], [10.8, 1.3, 8.8], WIT)])
        P.bot("krijgster_waaier_links", L, [3, 9.5, -7], waaier(P, 1), rot=[0, 0, -72])
        P.bot("krijgster_waaier_rechts", R, [-3, 9.5, -7], waaier(P, -1), rot=[0, 0, 72])

    def s0(S):
        P = S.P
        S.kop("kroon", kyoshi_kroon(P, groot=1.0))
        S.kop("haar", kap(P, HAAR_BRUIN) + bolknot(P, HAAR_BRUIN, y=24.5, z=6.2, b=4.6))

    def s1(S):
        P = S.P
        S.kop("kroon", kyoshi_kroon(P, groot=0.8))
        S.kop("haar", kap(P, HAAR_ZWART) + lang_haar(P, HAAR_ZWART, tot=8.0, breed=6.0) + [P.k([-1.6, 20.0, 6.2], [3.2, 1.2, 1.8], (204, 40, 52))])

    def s2(S):
        P, k = S.P, S.P.k
        rossig = (150, 78, 44)
        S.kop("kroon", [k([-7.3, 24.3, -7.5], [14.6, 1.3, 1.2], GOUD), k([-2.4, 24.0, -7.9], [4.8, 4.8, 0.7], GOUD, patroon=stip((255, 236, 150), 8, ring=(204, 150, 50))),
                        k([7.3, 19.0, -6.8], [0.8, 5.6, 1.0], (204, 40, 52)), k([-8.1, 19.0, -6.8], [0.8, 5.6, 1.0], (204, 40, 52))])
        S.kop("haar", kap(P, rossig) + [k([7.0, 14.0, -3.0], [1.6, 10.4, 9.0], rossig), k([-8.6, 14.0, -3.0], [1.6, 10.4, 9.0], rossig), k([-7.0, 13.0, 5.6], [14.0, 2.0, 1.4], rossig)])

    return burger(h, "kyoshi_krijgsterguh", "zit", dict(vacht=PEACH, gewaad=groen, rand_k=GOUD, zoom_k=donker, riem=GOUD, haar=HAAR_BRUIN), [s0, s1, s2], gemeen)


def anim_kyoshi_krijgsterguh():
    w = [reeks(R, "rotation", (0, [0, 0, 0]), (0.3, [-70, 0, 0]), (1.1, [-70, 0, 0]), (1.5, [0, 0, 0])), reeks(L, "rotation", (0, [0, 0, 0]), (0.3, [-70, 0, 0]), (1.1, [-70, 0, 0]), (1.5, [0, 0, 0])),
         reeks("krijgster_waaier_rechts", "scale", (0, [1, 1, 1]), (0.15, [0.15, 1, 1]), (0.5, [0.15, 1, 1]), (0.58, [1.2, 1, 1]), (0.7, [1, 1, 1])),
         reeks("krijgster_waaier_links", "scale", (0, [1, 1, 1]), (0.15, [0.15, 1, 1]), (0.5, [0.15, 1, 1]), (0.58, [1.2, 1, 1]), (0.7, [1, 1, 1])),
         reeks(BODY, "rotation", (0, [0, 0, 0]), (0.5, [0, 0, 0]), (0.62, [0, 24, 0]), (1.1, [0, 24, 0]), (1.5, [0, 0, 0]))]
    wacht = [reeks(R, "rotation", (0, [0, 0, 0]), (0.5, [-25, -42, 0])), reeks(L, "rotation", (0, [0, 0, 0]), (0.5, [-25, 42, 0])),
             reeks("krijgster_waaier_rechts", "scale", (0, [1, 1, 1]), (0.4, [0.15, 1, 1])), reeks("krijgster_waaier_links", "scale", (0, [1, 1, 1]), (0.4, [0.15, 1, 1])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [0, 0, 0]), (1.5, [0, 30, 0]), (3.0, [0, -30, 0]), (4.0, [0, 0, 0]))]
    return {"waaier": (30, w), "wacht": (0, wacht)}, (), 8.0


# =============================================================================================================================
# Waterstam-guhs (mix 4)
# =============================================================================================================================
def waterstam_guh(h):
    kraal = (124, 196, 244)

    def kap_op(P, kleur):
        k = P.k
        return [k([-7.7, 23.9, -6.6], [15.4, 2.6, 13.4], kleur), k([-8.5, 13.2, 3.4], [17.0, 12.2, 3.4], kleur), k([8.0, 13.8, -5.2], [0.9, 10.6, 9.0], kleur), k([-8.9, 13.8, -5.2], [0.9, 10.6, 9.0], kleur),
                k([-8.9, 23.3, -7.6], [17.8, 1.6, 1.4], BONT), k([7.5, 13.4, -7.6], [1.6, 10.2, 1.4], BONT), k([-9.1, 13.4, -7.6], [1.6, 10.2, 1.4], BONT)]

    def s0(S):                                              # the hood up
        P = S.P
        S.jas(W_BLAUW, zoom(BONT, 5), kraag=BONT)
        S.kop("kap", kap_op(P, W_BLAUW))

    def s1(S):                                              # the hood down, hair loops with beads, a braid
        P, k = S.P, S.P.k
        S.jas(W_LICHT, zoom(BONT, 5), kraag=BONT)
        S.bot("kap", BODY, [0, 12, 5], [P.c([-5.0, 9.6, 4.2], [10, 4.4, 2.6], W_LICHT), P.c([-5.4, 13.0, 4.4], [10.8, 1.2, 2.6], BONT)])
        S.kop("haar", kap(P, HAAR_BRUIN) + [k([5.6, 14.6, -7.9], [1.2, 8.4, 1.2], HAAR_BRUIN), k([-6.8, 14.6, -7.9], [1.2, 8.4, 1.2], HAAR_BRUIN),
                                            k([5.45, 17.0, -8.05], [1.5, 1.2, 1.5], kraal), k([-6.95, 17.0, -8.05], [1.5, 1.2, 1.5], kraal),
                                            k([5.0, 22.6, -7.7], [2.4, 1.6, 1.2], HAAR_BRUIN), k([-7.4, 22.6, -7.7], [2.4, 1.6, 1.2], HAAR_BRUIN)]
              + bolknot(P, HAAR_BRUIN, y=24.0, z=6.3, b=3.6) + vlecht(P, HAAR_BRUIN, tot=9.0, bol=kraal))

    def s2(S):                                              # a wolf tail and a fishing rod
        P, k, c = S.P, S.P.k, S.P.c
        S.jas(W_DONKER, zoom(W_LICHT, 5), kraag=BONT)
        S.kop("haar", kap(P, HAAR_ZWART) + [k([-1.4, 22.0, 6.2], [2.8, 2.8, 3.4], HAAR_ZWART), k([-1.6, 23.0, 6.4], [3.2, 1.0, 1.4], kraal)])
        S.bot("hengel", R, [-3, 9.5, -6.5], [c([-3.4, 8.6, -7.4], [0.8, 20.0, 0.8], HOUT), c([-3.3, 28.6, -7.3], [0.6, 0.6, 0.6], GRIJS),
                                             c([-3.2, 19.0, -7.2], [0.25, 9.6, 0.25], (236, 236, 240)), c([-4.0, 17.2, -7.6], [1.8, 1.8, 1.0], (150, 190, 226)),
                                             c([-3.5, 16.2, -7.5], [0.8, 1.0, 0.8], (150, 190, 226))], rot=[-24, 0, 12])

    def s3(S):                                              # a fur hat, and a child's mittens on a string
        P, k, c = S.P, S.P.k, S.P.c
        licht = (156, 204, 238)
        S.jas(licht, zoom(BONT, 5), kraag=BONT)
        S.kop("muts", [k([-7.6, 23.6, -7.6], [15.2, 2.6, 14.2], BONT), k([-6.4, 26.2, -6.4], [12.8, 1.2, 11.8], BONT), k([-1.4, 27.4, -1.4], [2.8, 2.2, 2.8], licht),
                       k([7.6, 15.5, -4.0], [1.2, 8.2, 7.0], BONT), k([-8.8, 15.5, -4.0], [1.2, 8.2, 7.0], BONT)])
        want = (226, 90, 100)
        S.bot("want_links", L, [3.5, 10, -4], [c([1.1, 6.5, -7.6], [3.8, 4.4, 2.6], want, patroon=zoom(BONT, 8, boven=False)), c([4.9, 8.6, -7.2], [1.0, 1.6, 1.6], want)])
        S.bot("want_rechts", R, [-3.5, 10, -4], [c([-4.9, 6.5, -7.6], [3.8, 4.4, 2.6], want, patroon=zoom(BONT, 8, boven=False)), c([-5.9, 8.6, -7.2], [1.0, 1.6, 1.6], want)])
        S.bot("koord", BODY, [0, 11, -4], [c([-3.4, 10.6, -4.6], [6.8, 0.4, 0.4], want)])

    return burger(h, "waterstam_guh", "zit", dict(vacht=BRUIN, gewaad=W_BLAUW, rand_k=BONT, zoom_k=BONT, haar=HAAR_BRUIN), [s0, s1, s2, s3])


def anim_zwaai():
    from features.einde2_cast_volk_modellen import zwaai
    return {"zwaai": (30, zwaai())}, (), 8.0


# =============================================================================================================================
# burgers van Ba Sing Guh (mix 5)
# =============================================================================================================================
def ba_sing_guh_burger(h):
    def s0(S):                                              # the lower ring: plain brown, a straw hat, a basket
        P, c = S.P, S.P.c
        S.jas(A_BRUIN, zoom(_donker(A_BRUIN, 0.75), 4))
        S.kop("hoed", kegelhoed(P, STRO))
        riet = (206, 170, 104)
        S.bot("mand", L, [3.5, 9, -6], [c([0.6, 3.6, -9.4], [5.4, 4.0, 4.0], riet, patroon=ruit(_donker(riet, 0.7), 6)), c([0.4, 7.6, -9.6], [5.8, 0.6, 4.4], _donker(riet, 0.8)),
                                        c([1.2, 7.8, -8.8], [1.8, 1.4, 1.8], (214, 70, 60)), c([3.4, 7.8, -8.6], [1.8, 1.6, 1.8], (120, 180, 90))])

    def s1(S):                                              # patched green, a flat cap
        P, c = S.P, S.P.c
        groen = (96, 132, 84)
        def lap(b):
            b[6:14, 4:13, :3] = (150, 120, 80)
            b[18:26, 18:28, :3] = (70, 100, 70)
            b[6:14, 4, :3] = b[6:14, 12, :3] = (60, 50, 40)
        S.jas(groen, lap)
        S.kop("pet", pet(P, (120, 100, 76), klep=(96, 80, 60), hoog=1.5))

    def s2(S):                                              # the middle ring: a neat robe, a bun with pins
        P, k = S.P, S.P.k
        S.jas(A_BEIGE, zoom(A_GROEN, 5), kraag=A_GROEN)
        S.kop("haar", kap(P, HAAR_ZWART) + bolknot(P, HAAR_ZWART, y=25.4, z=2.0, b=5.2) + [k([-4.4, 26.6, 3.6], [8.8, 0.5, 0.5], GOUD), k([-3.6, 24.2, 3.8], [7.2, 0.5, 0.5], (214, 60, 70))])

    def s3(S):                                              # the upper ring: silk, and a tall hat
        P, k = S.P, S.P.k
        zijde = (52, 140, 132)
        S.jas(zijde, ruit(GOUD, 8), kraag=GOUD)
        S.kop("hoed", [k([-3.6, 25.6, -3.8], [7.2, 5.2, 7.2], ZWART, patroon=zoom(GOUD, 5)), k([-4.2, 30.8, -4.4], [8.4, 0.8, 8.4], ZWART),
                       k([-7.3, 24.3, -7.3], [14.6, 1.3, 13.6], ZWART), k([-0.9, 27.0, -4.2], [1.8, 1.8, 0.6], (70, 180, 120))])
        S.kop("snor", snor(P, HAAR_ZWART, hang=3.4, breed=4.8, dik=0.5))

    def s4(S):                                              # an upper ring lady with flowers in her hair and a paper umbrella
        P, k, c = S.P, S.P.k, S.P.c
        zijde = (188, 220, 176)
        S.jas(zijde, zoom((226, 130, 160), 5), kraag=(226, 130, 160))
        S.kop("haar", kap(P, HAAR_ZWART) + bolknot(P, HAAR_ZWART, y=24.6, z=6.3, b=4.6) + [k([4.4, 25.8, -5.6], [2.0, 1.6, 2.0], (240, 130, 170)), k([5.0, 26.4, -5.0], [0.8, 1.4, 0.8], (255, 226, 120)),
                                                                                           k([-6.2, 25.8, -4.4], [1.6, 1.4, 1.6], WIT)])
        papier = (250, 238, 214)
        S.bot("paraplu", L, [3, 9.5, -7], [c([2.7, 6.0, -7.4], [0.7, 27.0, 0.7], HOUT), c([-6.0, 32.0, -16.0], [18, 0.7, 18], papier, patroon=rand((214, 60, 70), 4)),
                                           c([-3.0, 32.7, -13.0], [12, 0.8, 12], papier), c([-0.2, 33.5, -10.2], [6.4, 0.8, 6.4], (214, 60, 70))], rot=[8, 0, -10])

    return burger(h, "ba_sing_guh_burger", "zit", dict(vacht=TAN, gewaad=A_BRUIN, rand_k=A_BEIGE, haar=HAAR_ZWART), [s0, s1, s2, s3, s4])


def anim_ba_sing_guh_burger():
    from features.einde2_cast_volk_modellen import zwaai
    fluister = [reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.3, [0, 36, 0]), (0.7, [0, -36, 0]), (1.0, [8, 0, 0]), (1.7, [8, 0, 0]), (2.0, [0, 0, 0])),
                reeks(BODY, "rotation", (0, [0, 0, 0]), (0.9, [0, 0, 0]), (1.1, [14, 0, 0]), (1.7, [14, 0, 0]), (2.0, [0, 0, 0])),
                reeks(R, "rotation", (0, [0, 0, 0]), (0.9, [0, 0, 0]), (1.1, [-100, -26, 0]), (1.7, [-100, -26, 0]), (2.0, [0, 0, 0]))]
    return {"zwaai": (30, zwaai()), "fluister": (40, fluister)}, (), 8.0


# =============================================================================================================================
# burgers van Omashguh (mix 3), dorpsguhs van Kyoshi (mix 3), gevangenen (mix 3)
# =============================================================================================================================
def omashguh_burger(h):
    def s0(S):
        P = S.P
        S.jas(A_GROEN, zoom(A_GEEL, 6), kraag=A_GEEL)
        S.kop("muts", rondmutsje(P, A_GROEN, band=A_GEEL))

    def s1(S):                                              # a parcel for the chutes
        P, c = S.P, S.P.c
        S.jas(A_GEEL, zoom(A_GROEN, 6), kraag=A_GROEN)
        S.kop("muts", rondmutsje(P, A_GEEL, band=A_GROEN, kwast=(214, 60, 60)))
        doos = (176, 132, 86)
        S.bot("pakket", BODY, [0, 7, -7], [c([-3.4, 5.4, -9.4], [6.8, 5.0, 4.6], doos, patroon=strepen((240, 232, 210), 16, 3, liggend=False)), c([-3.5, 7.5, -9.5], [7.0, 0.7, 4.8], (240, 232, 210)),
                                           c([-1.0, 10.4, -8.0], [2.0, 0.6, 1.6], (214, 60, 60))])

    def s2(S):                                              # olive, with a wide wound cap
        P, k = S.P, S.P.k
        olijf = (132, 146, 76)
        S.jas(olijf, zoom((80, 96, 50), 5), kraag=A_GEEL)
        S.kop("muts", [k([-7.0, 24.6, -6.6], [14.0, 2.4, 12.6], A_GEEL, patroon=strepen(A_GROEN, 8, 3, liggend=False)), k([-5.0, 27.0, -4.6], [10.0, 1.4, 8.6], A_GEEL),
                       k([-1.0, 25.2, -7.0], [2.0, 1.6, 0.6], (70, 180, 120))])
        S.kop("baard", sik(P, HAAR_BRUIN, 2.4, 2.2))

    return burger(h, "omashguh_burger", "zit", dict(vacht=TAN, gewaad=A_GROEN, rand_k=A_GEEL, haar=HAAR_BRUIN), [s0, s1, s2])


def kyoshi_dorpsguh(h):
    def s0(S):
        P = S.P
        S.jas((52, 70, 130), zoom((214, 206, 180), 4), kraag=(214, 206, 180))
        S.kop("hoed", kegelhoed(P, (206, 184, 120), breed=17.0))

    def s1(S):                                              # a fishing net over the shoulder
        P, c, k = S.P, S.P.c, S.P.k
        S.jas((70, 98, 152), zoom((40, 56, 110), 5))
        S.kop("haar", kap(P, HAAR_BRUIN) + knot(P, HAAR_BRUIN, band=(214, 206, 180), z=1.0, b=2.8, hh=2.8))
        draad = (226, 214, 180)
        S.bot("net", BODY, [0, 8, 0], [c([-0.5, 3.0, -5.0], [6.6, 9.6, 0.4], draad, patroon=net(6)), c([-0.5, 12.4, -5.0], [6.6, 0.4, 9.8], draad, patroon=net(6)),
                                       c([-0.5, 2.0, 4.6], [6.6, 10.6, 0.4], draad, patroon=net(6)), c([0.6, 2.4, -5.6], [1.4, 1.4, 1.2], (214, 90, 60)), c([4.0, 2.4, -5.6], [1.4, 1.4, 1.2], (214, 90, 60))])

    def s2(S):                                              # a headband and an apron
        P, c = S.P, S.P.c
        S.jas((40, 56, 112), zoom((120, 150, 200), 4))
        S.kop("band", hoofdband(P, (214, 206, 180), y=24.0) + knot(P, HAAR_ZWART, z=2.2, b=2.6, hh=2.4))
        S.bot("schort", BODY, [0, 6, -4], [c([-4.0, 1.4, -4.8], [8.0, 6.4, 0.6], (226, 216, 190), patroon=rand((176, 166, 140), 2)), c([-5.4, 7.2, -4.5], [10.8, 0.7, 9.0], (226, 216, 190))])

    return burger(h, "kyoshi_dorpsguh", "zit", dict(vacht=TAN, gewaad=(52, 70, 130), rand_k=(214, 206, 180), haar=HAAR_BRUIN), [s0, s1, s2])


def gevangene_guh(h):
    bruinrood, donker = (156, 68, 52), (104, 44, 38)

    def gemeen(P):
        P.verf(BODY, (226, 216, 190), lambda x, y, z, f: (f == "north") & (x > 0.6) & (x < 3.4) & (y > 7.4) & (y < 9.4))           # the number patch
        P.verf(BODY, donker, lambda x, y, z, f: (f == "north") & (x > 1.0) & (x < 3.0) & (np.abs(y - 8.4) < 0.3))

    def s0(S):                                              # earth kingdom: a topknot with a green band
        P = S.P
        S.kop("haar", kap(P, HAAR_ZWART) + knot(P, HAAR_ZWART, band=A_GROEN, z=1.0, b=3.0, hh=2.8) + sik(P, HAAR_ZWART, 1.6, 2.6))

    def s1(S):                                              # water tribe: hair loops with a blue bead, a wolf tail
        P, k = S.P, S.P.k
        kraal = (124, 196, 244)
        S.kop("haar", kap(P, HAAR_BRUIN) + [k([5.6, 15.4, -7.9], [1.2, 7.6, 1.2], HAAR_BRUIN), k([5.45, 17.4, -8.05], [1.5, 1.2, 1.5], kraal),
                                            k([-1.4, 22.0, 6.2], [2.8, 2.8, 3.2], HAAR_BRUIN), k([-1.6, 23.0, 6.4], [3.2, 1.0, 1.4], kraal)])

    def s2(S):                                              # a big beard and a rag round the head
        P = S.P
        S.kop("doek", hoofdband(P, (190, 184, 170), y=23.9, dik=1.4))
        S.kop("baard", baard(P, HAAR_BRUIN, lengte=3.4, breed=8.4) + snor(P, HAAR_BRUIN, hang=1.4, breed=6.0, dik=0.9))

    return burger(h, "gevangene_guh", "zit", dict(vacht=TAN, gewaad=bruinrood, rand_k=donker, kraag=donker, zoom_k=donker, riem=donker), [s0, s1, s2], gemeen)


def anim_gevangene_guh():
    zucht = [reeks(BODY, "scale", (0, [1, 1, 1]), (0.5, [1.04, 1.07, 1.04]), (1.1, [1, 0.95, 1]), (1.5, [1, 1, 1])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.5, [-6, 0, 0]), (1.1, [16, 0, 0]), (1.5, [0, 0, 0]))]
    juich = [reeks(R, "rotation", (0, [0, 0, 0]), (0.25, [-150, 0, 22]), (1.2, [-150, 0, 22]), (1.5, [0, 0, 0])), reeks(L, "rotation", (0, [0, 0, 0]), (0.25, [-150, 0, -22]), (1.2, [-150, 0, -22]), (1.5, [0, 0, 0])),
             reeks(BODY, "position", (0, [0, 0, 0]), (0.25, [0, 2.4, 0]), (0.5, [0, 0, 0]), (0.75, [0, 2.4, 0]), (1.0, [0, 0, 0]), (1.5, [0, 0, 0])),
             reeks(HEAD, "rotation", (0, [0, 0, 0]), (0.25, [-12, 0, 0]), (1.2, [-12, 0, 0]), (1.5, [0, 0, 0]))]
    return {"zucht": (30, zucht), "juich": (30, juich)}, (), 8.0


# =============================================================================================================================
# the fire nation's Mikas: the sages (mix 2), the townsfolk (mix 5), the pupils (mix 3)
# =============================================================================================================================
RUST_MIKA = (-10, 0, 0)


def vuurwijze_mika(h):
    from features.einde2_cast_volk_modellen import wijzenhoed

    def gemeen(P):
        P.bot("wijze_mantel", BODY, [0, 5, 0], [P.c([-7.0, 0.6, -2.6], [14.0, 10.2, 2.2], V_DONKER, patroon=rand(GOUD, 3))])

    def s0(S):                                              # a grey goatee
        P = S.P
        S.kop("hoed", wijzenhoed(P, hoog=1.0))
        S.kop("sik", sik(P, HAAR_GRIJS, 3.0, 2.0) + snor(P, HAAR_GRIJS, hang=0.0, breed=4.4, dik=0.6))

    def s1(S):                                              # none, and a chain of office
        P, k = S.P, S.P.k
        S.kop("hoed", wijzenhoed(P, hoog=0.88))
        S.kop("ketting", [k([-3.6, 12.6, -7.6], [7.2, 0.7, 0.6], GOUD), k([-1.2, 10.6, -7.8], [2.4, 2.4, 0.6], GOUD, patroon=vlam(V_ROOD, (255, 190, 80)))])

    return burger(h, "vuurwijze_mika", "mika", dict(vacht=(0.0, 0.95, 1.0), gewaad=V_ROOD, kraag=GOUD, zoom_k=GOUD, riem=V_DONKER), [s0, s1], gemeen)


def anim_vuurwijze_mika():
    plechtig = [reeks(HEAD, "rotation", (0, RUST_MIKA), (0.5, [22, 0, 0]), (1.1, [22, 0, 0]), (1.5, RUST_MIKA))]
    betrapt = [reeks(HEAD, "rotation", (0, RUST_MIKA), (0.2, [-14, 62, 0]), (0.9, [-14, 62, 0]), (1.1, [-14, -30, 0]), (1.5, RUST_MIKA)),
               reeks("ear_left", "rotation", (0, [0, 0, 0]), (0.2, [0, 0, -18]), (0.9, [0, 0, -18]), (1.5, [0, 0, 0])),
               reeks("ear_right", "rotation", (0, [0, 0, 0]), (0.2, [0, 0, 18]), (0.9, [0, 0, 18]), (1.5, [0, 0, 0]))]
    return {"plechtig": (30, plechtig), "betrapt": (30, betrapt)}, (), 8.0


def vuurnatie_burger_mika(h):
    def gemeen(P):
        c, k = P.c, P.k
        stoom = (244, 244, 250)
        P.kbot("vuurburger_stoom", [k([8.4, 29.6, -3.4], [2.2, 2.2, 2.2], stoom), k([9.4, 32.4, -3.0], [1.4, 1.4, 1.4], stoom), k([-10.6, 29.6, -3.4], [2.2, 2.2, 2.2], stoom),
                                    k([-10.8, 32.4, -3.0], [1.4, 1.4, 1.4], stoom)])
        P.bot("vuurburger_zakje", ROOT, [0, 0, -15], [c([-2.2, 0, -17.4], [4.4, 4.2, 3.2], (214, 60, 50), patroon=vlam((255, 190, 70), (255, 236, 150))),
                                                    c([-1.6, 4.2, -16.8], [1.2, 1.0, 1.0], (250, 150, 50)), c([0.2, 4.2, -16.6], [1.2, 1.4, 1.0], (240, 90, 50))])

    def s0(S):                                              # the baker: a cap, and flour on the apron
        P, k = S.P, S.P.k
        S.kop("muts", [k([-5.0, 25.4, -5.4], [10, 2.0, 9.0], WIT), k([-6.2, 27.4, -6.4], [12.4, 3.0, 11.0], WIT), k([-4.6, 30.4, -4.8], [9.2, 1.0, 8.0], WIT)])
        S.jas((244, 240, 232), zoom((214, 206, 196), 6))

    def s1(S):                                              # the smith: a leather apron and a hammer
        P, c = S.P, S.P.c
        S.kop("knot", knot(P, HAAR_ZWART, band=V_ROOD, z=0.5, b=3.0, hh=3.0))
        S.jas((96, 66, 46), rand((60, 42, 32), 3))
        S.bot("hamer", BODY, [7.5, 6, 5], [c([7.0, 2.0, 4.4], [0.9, 8.0, 0.9], HOUT), c([6.4, 9.4, 3.0], [2.2, 2.2, 3.8], (120, 124, 136))], rot=[20, 0, 0])

    def s2(S):                                              # the mail Mika: a cap with a badge and a bag
        P, c, k = S.P, S.P.c, S.P.k
        S.kop("pet", pet(P, V_DONKER, band=GOUD, klep=V_ZWART, hoog=1.6) + [k([-1.0, 25.8, -6.9], [2.0, 1.3, 0.5], GOUD)])
        S.bot("tas", BODY, [7.5, 5, 5], [c([6.6, 2.4, 2.0], [2.6, 5.4, 6.4], LEER, patroon=rand(_donker(LEER, 0.7), 3)), c([6.4, 6.4, 1.8], [3.0, 1.8, 6.8], _donker(LEER, 0.85)),
                                         c([7.0, 8.0, 3.0], [1.6, 1.4, 0.4], WIT), c([7.2, 8.0, 5.0], [1.4, 1.8, 0.4], (250, 240, 214))])

    def s3(S):                                              # the cook: a cloth round the head and a ladle
        P, c, k = S.P, S.P.c, S.P.k
        S.kop("doek", [k([-7.4, 23.9, -7.4], [14.8, 2.4, 13.8], WIT), k([-6.2, 26.3, -6.2], [12.4, 1.0, 11.4], WIT), k([-1.6, 23.6, 6.4], [3.2, 2.2, 1.4], WIT), k([-2.8, 20.4, 6.6], [1.6, 3.2, 0.7], WIT),
                       k([1.2, 19.8, 6.6], [1.6, 3.8, 0.7], WIT)])
        S.bot("lepel", BODY, [-7.5, 6, 5], [c([-7.9, 3.0, 4.6], [0.8, 8.4, 0.8], HOUT), c([-8.6, 1.2, 3.8], [2.2, 2.0, 2.4], (190, 190, 200))], rot=[-16, 0, 0])

    def s4(S):                                              # a plain shawl
        P, c = S.P, S.P.c
        S.kop("knot", knot(P, HAAR_BRUIN, band=GOUD, z=0.5, b=2.8, hh=2.6))
        S.bot("sjaal", BODY, [0, 5, 0], [c([-7.1, 0.5, -2.7], [14.2, 10.4, 5.0], V_ZWART, patroon=zoom(GOUD, 4)), c([4.4, -0.4, -2.9], [2.4, 3.6, 0.6], V_ZWART, patroon=zoom(GOUD, 8))])

    return burger(h, "vuurnatie_burger_mika", "mika", dict(vacht=(0.0, 1.0, 1.0), gewaad=V_ROOD, kraag=V_ZWART, zoom_k=V_ZWART, riem=GOUD), [s0, s1, s2, s3, s4], gemeen)


def anim_vuurnatie_burger_mika():
    groet = [reeks(HEAD, "rotation", (0, RUST_MIKA), (0.3, [12, 0, 0]), (0.7, [12, 0, 0]), (1.2, RUST_MIKA)),
             reeks("leg_front_right", "position", (0, [0, 0, 0]), (0.3, [0, 3, -1]), (0.7, [0, 3, -1]), (1.2, [0, 0, 0]))]
    vlok = [toon("vuurburger_zakje", 0.1, 2.4),
            reeks(HEAD, "rotation", (0, RUST_MIKA), (0.3, [16, 0, 0]), (0.5, [4, 0, 0]), (0.7, [16, 0, 0]), (0.9, [4, 0, 0]), (1.1, [16, 0, 0]), (1.3, [-22, 0, 0]), (2.2, [-22, 0, 0]), (2.5, RUST_MIKA)),
            reeks("vuurburger_stoom", "scale", (0, [0, 0, 0]), (1.25, [0, 0, 0]), (1.4, [1, 1, 1]), (2.1, [1.5, 1.5, 1.5]), (2.3, [0, 0, 0])),
            reeks("vuurburger_stoom", "position", (1.3, [0, 0, 0]), (2.3, [0, 5, 0])),
            schud("ear_left", 1.3, 2.3, (0, 0, 16), n=4), schud("ear_right", 1.3, 2.3, (0, 0, 16), n=4),
            reeks(BODY, "position", (0, [0, 0, 0]), (1.3, [0, 0, 0]), (1.4, [0, 1.6, 0]), (1.6, [0, 0, 0]))]
    return {"groet": (24, groet), "vuurvlokken": (50, vlok)}, ("vuurburger_stoom", "vuurburger_zakje"), 12.0


def vuurnatie_leerling(h):
    uniform, kraag = (128, 44, 48), (230, 196, 150)

    def gemeen(P):
        P.kbot("leerling_band", hoofdband(P, (214, 44, 50), y=23.9, dik=1.2))

    def s0(S):
        P = S.P
        S.kop("knot", knot(P, HAAR_ZWART, band=GOUD, z=0.5, b=3.0, hh=3.0))

    def s1(S):                                              # the tsungi horn
        P, c = S.P, S.P.c
        koper = (226, 170, 70)
        S.kop("knot", knot(P, HAAR_BRUIN, band=GOUD, z=0.5, b=2.6, hh=2.4))
        S.bot("hoorn", BODY, [8, 5, 5], [c([6.8, 2.0, 1.0], [1.6, 1.6, 8.0], koper), c([6.8, 7.0, 1.0], [1.6, 1.6, 8.0], koper), c([6.8, 3.6, 7.6], [1.6, 3.4, 1.4], koper),
                                         c([6.4, 0.8, -1.8], [2.4, 4.0, 2.8], koper, patroon=rand(_donker(koper, 0.7), 4)), c([6.9, 7.2, -0.4], [1.4, 1.2, 1.4], _donker(koper, 0.8))])

    def s2(S):                                              # two little buns and a drum
        P, c, k = S.P, S.P.c, S.P.k
        S.kop("knotjes", [k([3.0, 26.0, -1.0], [2.6, 2.4, 2.6], HAAR_ZWART), k([-5.6, 26.0, -1.0], [2.6, 2.4, 2.6], HAAR_ZWART)])
        vel = (240, 228, 200)
        S.bot("trommel", BODY, [-8.5, 5, 5], [c([-10.6, 2.0, 2.0], [4.0, 6.0, 6.0], V_ROOD, patroon=strepen(GOUD, 8, 2, liggend=False)), c([-10.9, 2.4, 2.4], [0.4, 5.2, 5.2], vel),
                                              c([-6.7, 2.4, 2.4], [0.4, 5.2, 5.2], vel), c([-9.4, 8.2, 3.0], [0.6, 3.6, 0.6], HOUT), c([-8.2, 8.2, 6.0], [0.6, 3.2, 0.6], HOUT)])

    return burger(h, "vuurnatie_leerling", "mika", dict(vacht=(0.0, 1.0, 1.02), gewaad=uniform, kraag=kraag, zoom_k=V_ZWART, riem=V_ZWART), [s0, s1, s2], gemeen)


def anim_vuurnatie_leerling():
    hand = [reeks("leg_front_right", "position", (0, [0, 0, 0]), (0.2, [0, 7, -1]), (0.9, [0, 7, -1]), (1.2, [0, 0, 0])),
            reeks("leg_front_right", "rotation", (0, [0, 0, 0]), (0.2, [-70, 0, 0]), (0.9, [-70, 0, 0]), (1.2, [0, 0, 0])),
            reeks(HEAD, "rotation", (0, RUST_MIKA), (0.2, [-20, 0, 0]), (0.9, [-20, 0, 0]), (1.2, RUST_MIKA)),
            reeks(BODY, "position", (0, [0, 0, 0]), (0.2, [0, 1.2, 0]), (0.9, [0, 1.2, 0]), (1.2, [0, 0, 0]))]
    dans = [schud(BODY, 0, 2.0, (0, 0, 8), n=4), schud(HEAD, 0, 2.0, (0, 0, -8), n=4, rust=RUST_MIKA),
            reeks("leg_front_left", "position", (0, [0, 0, 0]), (0.25, [0, 2, 0]), (0.5, [0, 0, 0]), (1.0, [0, 0, 0]), (1.25, [0, 2, 0]), (1.5, [0, 0, 0]), (2.0, [0, 0, 0])),
            reeks("leg_front_right", "position", (0, [0, 0, 0]), (0.5, [0, 0, 0]), (0.75, [0, 2, 0]), (1.0, [0, 0, 0]), (1.5, [0, 0, 0]), (1.75, [0, 2, 0]), (2.0, [0, 0, 0])),
            schud("tail", 0, 2.0, (0, 24, 0), n=4)]
    return {"hand_op": (24, hand), "dans": (40, dans)}, (), 8.0
