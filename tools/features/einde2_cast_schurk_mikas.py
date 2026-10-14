"""
einde2, slice cast_schurk - the twelve Mika Kinds of the Guhvatar story (CONTRACT_E2 11.6), drawn with the kit of
einde2_cast_schurk_blad: Vuurheer Mikai in his three looks, Azula-Mika, Ty Lee-Mika, admiraal Zhao-Mika, Long Feng-Mika, the
Dai Li-Mika's, the Vuurnatie-Mika's (three mix sets), the gevangenisdirecteur, the spijtige Mika-kok and the vesting-kapitein.

Every one of them stays a Mika at a glance: the Mika's own big head with its two round ears, its red slit eyes and its
fang (only the mood of the face changes), and all but the tall Vuurheer on four short paws. What makes each the character
of the parodied show is worn ON that: hair and a topknot, a crown, sideburns, a moustache, a hat, armour, a thing in a paw.
A Mika of this cast holds its thing in its RIGHT front paw, which is lifted out to the side of its face (the paw's own cube
is moved: the bone stays), so the thing shows from the front.

MAKERS: kind id -> maker(h) -> (Blad, animations). einde2_cast_schurk_modellen writes the files.
"""
import math

import numpy as np

from features import boerderij_dieren as bd
from features.einde2_cast_schurk_blad import (BLAUW_VUUR, BRUIN, BRUIN_LICHT, GEEL, GOUD, GOUD_DONKER, GRIJS, GRIJS_LICHT, GROEN, GROEN_LICHT, HAAR,
                                              HAAR_LICHT, KAAS, KAAS_GAT, KOPER, MARINE, MIKA, MIKA_DONKER, ORANJE, ROOD, ROOD_DONKER, ROOD_LICHT, ROZE,
                                              ROZE_DONKER, STEEN, WEG, WIT, ZWART, Blad, OpKop, anim, bliksem, disc, effen, golf, haardos, haren,
                                              kf, knot, lang_lijf, met, metaal, oren, pantser, rond, snuit, steen, stof, streep, vlam, vlamkroon, F)

P = "animation.guh_sitting."


# =====================================================================================================================
# shared parts on the ordinary Mika (model pixels: head x -8..8, y 0..14, z -12..0, its face at z -12; body behind it)
# =====================================================================================================================
def harnas(b, naam, paint, boven=None, k=0.5):
    """A shell round the Mika's body (armour, a robe, a coat)."""
    return b.bot(naam, "body", [0, 5, 5], [b.kubus([-7.0, 0.3, -1.9], [14.0, 10.3, 13.3], paint, overrides={"up": boven} if boven else None, k=k)])


def schouders(b, naam, paint, punt=True, breed=1.0):
    """Shoulder plates behind the head, sticking out past it (pointed: the Fire Nation's)."""
    cubes = b.paar([5.2, 8.4, -1.6], [5.2 * breed, 2.3, 6.4], paint)
    if punt:
        cubes += b.paar([5.2 + 5.2 * breed - 1.6, 10.2, -0.9], [2.6, 1.9, 5.0], paint)
        cubes += b.paar([5.2 + 5.2 * breed + 0.2, 11.6, -0.3], [1.5, 1.7, 3.8], paint)
    return b.bot(naam, "body", [0, 9, 2], cubes)


def poot_opzij(b, x=-11.0, y=1.0, z=-13.4):
    """Lifts the right front paw out to the side of the face (its cube moves, the bone and its pivot follow)."""
    poot = b.basisbot("leg_front_right")
    poot["cubes"][0]["origin"] = [x, y, z]
    poot["pivot"] = [x + 1.75, y + 1, z + 1.75]
    return x + 1.75, y + 2.0, z + 1.75                              # the middle of the paw's top


def kruisvlam(b, cx, y, cz, breed, hoog, painter, gloed=True):
    """A flame as two crossed thin planes."""
    return [b.kubus([cx - breed / 2, y, cz - 0.1], [breed, hoog, 0.2], painter, gloed=gloed, alleen=("north", "south")),
            b.kubus([cx - 0.1, y, cz - breed / 2], [0.2, hoog, breed], painter, gloed=gloed, alleen=("east", "west"))]


def lege_botten(b, namen):
    """Bones without cubes for the names another look of the same character has (one animation file per character)."""
    heeft = set(b.namen())
    for n in namen:
        if n not in heeft:
            b.geo["bones"].append({"name": n, "parent": "root", "pivot": [0, 0, 0], "cubes": []})


def mika_idle(L=12.0, staart=14, kop=2.5, extra=None, oor=0.0):
    """The idle every four-legged Mika of the cast starts from (the Mika's bones: no arms): breathing, the tail, the ears,
    a slow tilt of the head (the stock renderer owns the head's x and y: it looks at the player)."""
    b = {"body": {"scale": kf([(0, [1, 1, 1]), (L / 4, [1.02, 1.03, 1.02]), (L / 2, [1, 1, 1]), (L * 3 / 4, [1.02, 1.03, 1.02]), (L, [1, 1, 1])])},
         "tail": {"rotation": golf(1, staart, L, n=16)},
         "head": {"rotation": golf(2, kop, L, n=8)}}
    b.update(oren(L, om=oor))
    b.update(extra or {})
    return anim(L, b)


def praat(extra=None):
    """What a click plays: the head bobs as it talks (24 ticks)."""
    b = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 4]), (0.4, [0, 0, -4]), (0.65, [0, 0, 3]), (0.9, [0, 0, -2]), (1.2, [0, 0, 0])]),
                  "position": kf([(0, [0, 0, 0]), (0.15, [0, 0.5, 0]), (0.3, [0, 0, 0]), (0.5, [0, 0.5, 0]), (0.7, [0, 0, 0]), (0.9, [0, 0.4, 0]), (1.2, [0, 0, 0])])},
         "tail": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 16, 0]), (0.6, [0, -16, 0]), (0.9, [0, 10, 0]), (1.2, [0, 0, 0])])}}
    b.update(extra or {})
    return anim(1.2, b, False)


def toon(naam, van, tot, L, groot=(1, 1, 1)):
    """A prop bone that only shows between `van` and `tot` seconds of an animation of length L."""
    e = 0.04
    pts = [(0, WEG)] if van > e else []
    pts += [(max(0, van - e), WEG), (van, list(groot)), (tot, list(groot)), (min(L, tot + e), WEG)]
    if tot + e < L:
        pts.append((L, WEG))
    return {naam: {"scale": kf(pts)}}


def namen(d):
    return {P + k: v for k, v in d.items()}


# =====================================================================================================================
# Azula-Mika
# =====================================================================================================================
def azula_mika(h):
    b = Blad(h, "azula_mika")
    # sharp: narrow eyes under thin steep brows, a thin one-sided smile
    snuit(b, brauw="streng", mond="dun", lid=0.36, onderlid=0.12, gloei=False)
    haardos(b, "azula_haar")
    knot(b, "azula_knot", band=ROOD)
    vlamkroon(b, "azula_kroon", y=17.0, z=-6.2, schaal=1.15)
    # exactly TWO strands of hair framing the face
    b.bot("azula_lokken", "head", [0, 12.5, -12.4], [])
    lok = haren()
    for side, sx in (("links", 1), ("rechts", -1)):
        x = 5.7 if sx > 0 else -6.8
        b.bot(f"azula_lok_{side}", "azula_lokken", [x + 0.55, 12.6, -12.4], [
            b.kubus([x, 4.6, -12.75], [1.1, 8.2, 0.7], lok), b.kubus([x + 0.2 * sx, 3.4, -12.7], [0.8, 1.4, 0.6], lok)])
    # red and black armour with gold edges, pointed shoulders
    harnas(b, "azula_harnas", pantser(ROOD_DONKER, GOUD), boven=pantser((52, 26, 34), GOUD))
    schouders(b, "azula_schouders", pantser((58, 28, 36), GOUD))
    b.bot("azula_kraag", "head", [0, 1, -12], [b.kubus([-4.2, 0.0, -12.7], [8.4, 1.3, 0.8], metaal(GOUD, (255, 240, 170)))])
    # a blue flame over the raised paw
    px, py, pz = poot_opzij(b)
    blauw = vlam(kern=(236, 250, 255), rand=BLAUW_VUUR, punt=(36, 90, 226))
    b.bot("azula_vlam", "leg_front_right", [px, py, pz], kruisvlam(b, px, py, pz, 5.2, 7.6, blauw))
    # the bolt (only in "bliksem"): from the paw forward
    bout = bliksem()
    b.bot("azula_bliksem", "leg_front_right", [px, py + 1.5, pz], [
        b.kubus([px - 1.6, py + 1.4, pz - 15], [3.2, 0.2, 14], bout, gloed=True, alleen=("up", "down")),
        b.kubus([px - 1.6, py + 2.6, pz - 15], [3.2, 0.2, 14], bout, gloed=True, alleen=("up", "down"))])
    L = 12.0
    flakker = {"azula_vlam": {"scale": kf([(i * 0.5, [1 + 0.1 * math.sin(i * 2.1), 1 + 0.16 * math.sin(i * 1.3 + 1), 1 + 0.1 * math.sin(i * 2.1)]) for i in range(25)]),
                              "rotation": kf([(i * 1.0, [0, i * 30.0, 0]) for i in range(13)])},
               "azula_bliksem": {"scale": WEG}}
    idle = mika_idle(L, staart=8, kop=1.5, extra=flakker)
    # the signature woven in: she looks at her own flame and it flares
    idle["bones"]["leg_front_right"] = {"position": kf([(0, [0, 0, 0]), (7.0, [0, 0, 0]), (7.6, [0, 1.6, 0]), (9.0, [0, 1.6, 0]), (9.8, [0, 0, 0])])}
    stil = {"azula_bliksem": {"scale": WEG}}
    A = {"idle": idle,
         "praat": praat(stil),
         "bliksem": anim(2.0, {
             "leg_front_right": {"position": kf([(0, [0, 0, 0]), (0.3, [3, 3, 0]), (0.6, [6, 0, 0]), (0.9, [3, -0.8, 0]), (1.2, [0, 3, 0]), (1.35, [0, 1.5, -3]), (1.8, [0, 1.5, -3]), (2.0, [0, 0, 0])])},
             "leg_front_left": {"position": kf([(0, [0, 0, 0]), (0.3, [-3, 3, 0]), (0.6, [-6, 0, 0]), (0.9, [-3, -0.8, 0]), (1.2, [0, 3, 0]), (1.4, [0, 0, 0])])},
             "azula_vlam": {"scale": kf([(0, [1, 1, 1]), (1.2, [0.4, 0.4, 0.4]), (1.3, WEG), (1.8, WEG), (2.0, [1, 1, 1])])},
             "body": {"rotation": kf([(0, [0, 0, 0]), (1.2, [-6, 0, 0]), (1.4, [5, 0, 0]), (2.0, [0, 0, 0])])},
             **toon("azula_bliksem", 1.3, 1.75, 2.0)}, False),
         "grijns": anim(1.6, {
             "head": {"rotation": kf([(0, [0, 0, 0]), (0.4, [0, 0, 9]), (1.2, [0, 0, 9]), (1.6, [0, 0, 0])]),
                      "position": kf([(0, [0, 0, 0]), (0.4, [0, 0.6, -0.6]), (1.2, [0, 0.6, -0.6]), (1.6, [0, 0, 0])])},
             "azula_vlam": {"scale": kf([(0, [1, 1, 1]), (0.5, [1.4, 1.6, 1.4]), (1.2, [1.3, 1.5, 1.3]), (1.6, [1, 1, 1])])},
             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.4, [0, 0, -10]), (1.2, [0, 0, -10]), (1.6, [0, 0, 0])])}, **stil}, False),
         # one strand falls out of place and she freezes: only the strand and an ear twitch
         "lok": anim(2.4, {
             "azula_lok_links": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, -34]), (0.4, [0, 0, -28]), (2.0, [0, 0, -28]), (2.4, [0, 0, 0])])},
             "head": {"position": kf([(0, [0, 0, 0]), (0.3, [0, 0.4, 0]), (0.5, [0.15, 0.4, 0]), (0.6, [-0.15, 0.4, 0]), (0.7, [0.15, 0.4, 0]), (0.8, [0, 0.4, 0]), (2.0, [0, 0.4, 0]), (2.4, [0, 0, 0])])},
             "ear_right": {"rotation": kf([(0, [0, 0, 0]), (1.0, [0, 0, 0]), (1.1, [0, 0, 16]), (1.2, [0, 0, 0]), (1.5, [0, 0, 16]), (1.6, [0, 0, 0])])},
             "tail": {"rotation": kf([(0, [0, 0, 0]), (0.3, [20, 0, 0]), (2.0, [20, 0, 0]), (2.4, [0, 0, 0])])}, **stil}, False)}
    return b, A


# =====================================================================================================================
# Ty Lee-Mika
# =====================================================================================================================
def ty_lee_mika(h):
    b = Blad(h, "ty_lee_mika", tint=(0.93, 1.5, 1.04))               # a rosier pink
    snuit(b, ogen="rond", brauw="blij", mond="blij", groot=1.16, wang=(255, 120, 170))
    bruin = (120, 74, 48)
    haardos(b, "ty_lee_haar", kleur=bruin, licht=(170, 118, 80), pony=1.5)
    p = haren(bruin, (170, 118, 80))
    lint = effen(ROZE_DONKER, 5)
    # the long braid: up out of the crown, back in a bow, down behind her (three bones: it swings)
    b.bot("ty_lee_vlecht", "head", [0, 14, -3.4], [b.kubus([-1.4, 13.8, -4.8], [2.8, 1.8, 2.8], p), b.kubus([1.2, 13.5, -4.3], [7.6, 1.5, 1.8], p),
                                                   b.kubus([-1.6, 13.9, -5.0], [3.2, 0.8, 3.2], lint)])
    b.bot("ty_lee_vlecht_2", "ty_lee_vlecht", [9.5, 14.2, -3.4], [b.kubus([8.4, 6.4, -4.5], [2.2, 8.6, 2.2], p)])
    b.bot("ty_lee_vlecht_3", "ty_lee_vlecht_2", [9.5, 6.4, -3.4], [b.kubus([8.5, 2.2, -4.4], [2.0, 4.2, 2.0], p), b.kubus([8.3, 5.4, -4.6], [2.4, 0.9, 2.4], lint),
                                                                 b.kubus([8.1, 0.2, -4.8], [2.8, 2.2, 2.8], p)])
    # a pink top, wide trousers
    top = stof(ROZE, zoom=ROZE_DONKER, var=6)
    b.bot("ty_lee_topje", "body", [0, 5, 2], [b.kubus([-7.0, 0.3, -1.9], [14.0, 10.3, 6.4], top, k=0.5)])
    broek = stof((255, 176, 206), zoom=ROZE_DONKER)
    for side, sx in (("left", 1), ("right", -1)):
        x = 4.0 if sx > 0 else -8.6
        b.bot(f"ty_lee_broek_{side}", f"leg_back_{side}", [sx * 6, 1, 8], [b.kubus([x, 0.0, 5.2], [4.6, 4.4, 6.0], broek)])
        fx = 2.0 if sx > 0 else -6.0
        b.bot(f"ty_lee_band_{side}", f"leg_front_{side}", [sx * 4, 1, -11], [b.kubus([fx, 1.4, -13.6], [4.0, 0.9, 3.9], lint)])
    L = 10.0
    zwaai = {"ty_lee_vlecht": {"rotation": golf(2, 8, L, n=8)}, "ty_lee_vlecht_2": {"rotation": golf(2, 9, L, n=8, fase=0.6)},
             "ty_lee_vlecht_3": {"rotation": golf(2, 12, L, n=8, fase=1.2)}}
    idle = mika_idle(L, staart=24, kop=5, extra=zwaai)
    # the signature woven in: a happy hop on the spot
    idle["bones"]["root"] = {"position": kf([(0, [0, 0, 0]), (6.0, [0, 0, 0]), (6.2, [0, 3.5, 0]), (6.4, [0, 0, 0]), (6.6, [0, 2.5, 0]), (6.8, [0, 0, 0])])}
    rad = [(i * 0.15, [0, 0, -45.0 * i]) for i in range(9)]            # a whole turn in 1.2 s
    A = {"idle": idle, "praat": praat({"ty_lee_vlecht_3": {"rotation": golf(2, 14, 1.2)}}),
         "radslag": anim(1.4, {
             "root": {"rotation": kf(rad + [(1.4, [0, 0, -360])]),
                      "position": kf([(0, [0, 0, 0]), (0.3, [0, 9, 0]), (0.6, [0, 12, 0]), (0.9, [0, 9, 0]), (1.2, [0, 0, 0]), (1.4, [0, 0, 0])])},
             "ty_lee_vlecht_3": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, 50]), (1.2, [0, 0, -20]), (1.4, [0, 0, 0])])},
             "leg_front_left": {"position": kf([(0, [0, 0, 0]), (0.6, [3, 0, 0]), (1.2, [0, 0, 0])])},
             "leg_front_right": {"position": kf([(0, [0, 0, 0]), (0.6, [-3, 0, 0]), (1.2, [0, 0, 0])])}}, False),
         "prik": anim(1.0, {
             "leg_front_right": {"position": kf([(0, [0, 0, 0]), (0.1, [0, 3, -4]), (0.2, [0, 2, 0]), (0.35, [0, 4, -4]), (0.45, [0, 2, 0]), (0.6, [0, 3, -4.5]), (0.75, [0, 1, 0]), (1.0, [0, 0, 0])])},
             "body": {"rotation": kf([(0, [0, 0, 0]), (0.1, [4, 0, 0]), (0.6, [4, 0, 0]), (1.0, [0, 0, 0])])},
             "head": {"position": kf([(0, [0, 0, 0]), (0.1, [0, 0, -1]), (0.6, [0, 0, -1]), (1.0, [0, 0, 0])])}}, False),
         "handstand": anim(2.0, {
             "root": {"rotation": kf([(0, [100, 0, 0]), (1.0, [104, 0, 0]), (2.0, [100, 0, 0])]), "position": [0, 13, -6]},
             "leg_back_left": {"position": golf(1, 1.2, 2.0)}, "leg_back_right": {"position": golf(1, -1.2, 2.0)},
             "tail": {"rotation": golf(1, 24, 2.0)}, "ty_lee_vlecht_2": {"rotation": [0, 0, 150]}})}
    return b, A


# =====================================================================================================================
# admiraal Zhao-Mika
# =====================================================================================================================
def zhao_mika(h):
    b = Blad(h, "zhao_mika")
    snuit(b, brauw="boos", mond="grijns", lid=0.14)
    bruin, licht = (74, 46, 34), (116, 78, 54)
    haardos(b, "zhao_haar", kleur=bruin, licht=licht)
    knot(b, "zhao_knot", kleur=bruin, band=ROOD, maat=2.8)
    vlamkroon(b, "zhao_vlam", y=16.6, z=-6.3, schaal=0.8)
    # HUGE sideburns: down both cheeks, flaring out at the jaw
    p = haren(bruin, licht, dicht=9)
    cubes = []
    cubes += b.paar([5.9, 3.4, -12.9], [2.5, 7.2, 1.1], p)
    cubes += b.paar([4.6, 0.6, -13.2], [4.4, 3.6, 1.5], p)
    cubes += b.paar([7.7, 0.4, -12.6], [2.2, 8.6, 5.4], p)
    cubes += b.paar([8.9, 0.2, -12.0], [1.6, 4.6, 3.6], p)
    b.bot("zhao_bakkebaarden", "head", [0, 4, -12], cubes)
    harnas(b, "zhao_harnas", pantser(ROOD_DONKER, GOUD), boven=pantser((60, 30, 34), GOUD))
    schouders(b, "zhao_schouders", pantser(ROOD, GOUD), breed=1.25)
    b.bot("zhao_kraag", "head", [0, 1, -12], [b.kubus([-3.6, 0.0, -12.8], [7.2, 1.6, 0.9], metaal(GOUD, (255, 240, 170)))])
    b.bot("zhao_rook", "head", [0, 16, -7], b.paar([6.4, 17.0, -8.4], [2.6, 3.4, 2.6], rond(effen((210, 210, 214), 16))))
    L = 12.0
    weg = {"zhao_rook": {"scale": WEG}}
    idle = mika_idle(L, staart=6, kop=1.5, extra=weg)
    # the signature woven in: he puffs himself up with pride, once per loop
    idle["bones"]["body"]["scale"] = kf([(0, [1, 1, 1]), (3, [1.02, 1.03, 1.02]), (6, [1, 1, 1]), (7.0, [1.12, 1.1, 1.06]), (8.6, [1.12, 1.1, 1.06]), (9.4, [1, 1, 1]), (L, [1, 1, 1])])
    idle["bones"]["head"]["position"] = kf([(0, [0, 0, 0]), (6, [0, 0, 0]), (7.0, [0, 1.2, 0]), (8.6, [0, 1.2, 0]), (9.4, [0, 0, 0])])
    A = {"idle": idle, "praat": praat(weg),
         "opschep": anim(2.0, {
             "root": {"scale": kf([(0, [1, 1, 1]), (0.5, [1.18, 1.18, 1.18]), (1.5, [1.18, 1.18, 1.18]), (2.0, [1, 1, 1])])},
             "head": {"position": kf([(0, [0, 0, 0]), (0.5, [0, 1.5, 0]), (1.5, [0, 1.5, 0]), (2.0, [0, 0, 0])]),
                      "rotation": kf([(0, [0, 0, 0]), (0.7, [0, 0, 5]), (1.1, [0, 0, -5]), (1.5, [0, 0, 0])])},
             "tail": {"rotation": kf([(0, [0, 0, 0]), (0.5, [30, 0, 0]), (1.5, [30, 0, 0]), (2.0, [0, 0, 0])])}, **weg}, False),
         "woede": anim(1.8, {
             "leg_front_left": {"position": kf([(t * 0.2, [0, 2.2 if t % 2 else 0, 0]) for t in range(9)] + [(1.8, [0, 0, 0])])},
             "leg_front_right": {"position": kf([(t * 0.2, [0, 0 if t % 2 else 2.2, 0]) for t in range(9)] + [(1.8, [0, 0, 0])])},
             "body": {"position": kf([(t * 0.2, [0, 0.8 if t % 2 else 0, 0]) for t in range(9)] + [(1.8, [0, 0, 0])])},
             "head": {"rotation": kf([(t * 0.1, [0, 0, 4 if t % 2 else -4]) for t in range(17)] + [(1.8, [0, 0, 0])])},
             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, 20]), (1.6, [0, 0, 20]), (1.8, [0, 0, 0])])},
             "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, -20]), (1.6, [0, 0, -20]), (1.8, [0, 0, 0])])},
             "zhao_rook": {"scale": kf([(0, WEG), (0.2, [0.6, 0.6, 0.6]), (1.5, [1.7, 1.7, 1.7]), (1.7, WEG), (1.8, WEG)]),
                           "position": kf([(0, [0, 0, 0]), (0.2, [0, 0, 0]), (1.5, [0, 6, 0]), (1.8, [0, 0, 0])])}}, False)}
    return b, A


# =====================================================================================================================
# Long Feng-Mika
# =====================================================================================================================
def long_feng_mika(h):
    b = Blad(h, "long_feng_mika")
    snuit(b, brauw="streng", mond="recht", lid=0.32)
    haardos(b, "long_feng_haar")
    knot(b, "long_feng_knot", band=GOUD, maat=2.6, y=14.4)
    p = haren()
    # a long thin moustache that hangs past the chin, and a goatee
    cubes = b.paar([0.6, 3.5, -13.3], [5.6, 1.0, 0.8], p)
    cubes += b.paar([5.5, 0.0, -13.4], [1.0, 4.5, 0.9], p)
    cubes += b.paar([6.0, 0.0, -14.6], [0.8, 0.8, 1.6], p)
    cubes += [b.kubus([-1.1, 1.6, -13.3], [2.2, 1.3, 0.8], p), b.kubus([-0.6, 0.6, -14.75], [1.2, 2.2, 0.7], p)]
    b.bot("long_feng_snor", "head", [0, 3.8, -13], cubes)
    # the long braid, down his back
    b.bot("long_feng_vlecht", "head", [0, 11, 0.4], [b.kubus([-0.8, 10.4, 0.2], [1.6, 1.5, 3.2], p)])
    b.bot("long_feng_vlecht_2", "long_feng_vlecht", [0, 11, 3.4], [b.kubus([-0.7, 10.3, 3.4], [1.4, 1.3, 9.2], p), b.kubus([-0.9, 10.2, 11.4], [1.8, 1.5, 0.9], effen(GOUD, 4)),
                                                                   b.kubus([-0.6, 6.8, 12.4], [1.2, 4.4, 1.2], p)])
    # a dark green robe with gold; both front paws hidden in the sleeves that meet
    harnas(b, "long_feng_gewaad", stof(GROEN, zoom=GOUD, boven=GOUD_DONKER), boven=stof(GROEN_LICHT, strepen=(GOUD, 28)))
    b.bot("long_feng_kraag", "body", [0, 9, -1], b.paar([5.4, 8.6, -1.9], [4.0, 2.2, 5.6], stof(GROEN_LICHT, zoom=GOUD)))

    def manchet(W, H, rng, a):
        a[:, W // 2 - 2:W // 2 + 2, :3] = GOUD_DONKER
        a[:, W // 2 - 1:W // 2 + 1, :3] = GOUD
    b.bot("long_feng_mouwen", "root", [0, 1.5, -12], [b.kubus([-5.2, 0.0, -14.6], [10.4, 2.6, 4.8], stof(GROEN, zoom=None), overrides={
        "north": met(stof(GROEN), manchet), "up": met(stof(GROEN), manchet)})])
    L = 14.0
    idle = mika_idle(L, staart=4, kop=1.2)
    # the signature woven in: a slow, false bow
    idle["bones"]["body"]["rotation"] = kf([(0, [0, 0, 0]), (8, [0, 0, 0]), (9, [5, 0, 0]), (10.4, [5, 0, 0]), (11.4, [0, 0, 0])])
    idle["bones"]["head"]["position"] = kf([(0, [0, 0, 0]), (8, [0, 0, 0]), (9, [0, -0.3, -1.6]), (10.4, [0, -0.3, -1.6]), (11.4, [0, 0, 0])])
    idle["bones"]["long_feng_snor"] = {"rotation": golf(2, 2.5, L, n=8)}
    A = {"idle": idle, "praat": praat({"long_feng_snor": {"rotation": golf(2, 5, 1.2)}}),
         "buig": anim(2.4, {
             "body": {"rotation": kf([(0, [0, 0, 0]), (0.8, [9, 0, 0]), (1.6, [9, 0, 0]), (2.4, [0, 0, 0])])},
             "head": {"position": kf([(0, [0, 0, 0]), (0.8, [0, -0.4, -2.6]), (1.6, [0, -0.4, -2.6]), (2.4, [0, 0, 0])]),
                      "rotation": kf([(0, [0, 0, 0]), (0.8, [0, 0, 0]), (1.2, [0, 0, 3]), (1.6, [0, 0, 0]), (2.4, [0, 0, 0])])},
             "tail": {"rotation": kf([(0, [0, 0, 0]), (0.8, [24, 0, 0]), (1.6, [24, 0, 0]), (2.4, [0, 0, 0])])}}, False),
         "fluister": anim(2.0, {
             "root": {"position": kf([(0, [0, 0, 0]), (0.5, [0, 0, -3.2]), (1.5, [0, 0, -3.2]), (2.0, [0, 0, 0])])},
             "head": {"rotation": kf([(0, [0, 0, 0]), (0.5, [0, 0, 12]), (1.5, [0, 0, 12]), (2.0, [0, 0, 0])])},
             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.5, [0, 0, 16]), (1.5, [0, 0, 16]), (2.0, [0, 0, 0])])},
             "long_feng_snor": {"rotation": kf([(0.5 + i * 0.125, [0, 0, 4 if i % 2 else -4]) for i in range(9)] + [(0, [0, 0, 0]), (2.0, [0, 0, 0])])}}, False)}
    return b, A


# =====================================================================================================================
# the Dai Li-Mika's
# =====================================================================================================================
def dai_li_mika(h):
    b = Blad(h, "dai_li_mika")

    def schaduw(f):                                                # the hat's shadow: the eyes are never seen
        H, W = f.shape[:2]
        for y in range(H):
            t = 1.0 if y < 26 else max(0.0, 1 - (y - 26) / 5.0)
            f[y, :, :3] = f[y, :, :3] * (1 - t) + np.array((22, 30, 26), np.float32) * t
    snuit(b, brauw="geen", mond="recht", ogen="dicht", extra=schaduw)
    hoed, rand = stof((30, 56, 44), var=5), stof((22, 42, 34), zoom=GOUD_DONKER, var=5, zoom_dik=0.34)
    b.bot("dai_li_hoed", "head", [0, 9, -6], [
        b.kubus([-11.5, 7.2, -16.2], [23.0, 1.0, 21.0], hoed, k=0.5),                    # the brim, pulled low
        b.kubus([-8.6, 8.2, -12.8], [17.2, 3.2, 15.2], rand, k=0.5),
        b.kubus([-7.4, 11.4, -11.6], [14.8, 1.9, 12.8], hoed, k=0.5),
        b.kubus([-6.2, 13.3, -10.4], [12.4, 1.5, 10.4], hoed, k=0.5),
        b.kubus([-3.4, 14.8, -8.2], [6.8, 1.1, 6.0], hoed, k=0.5),
        b.kubus([-1.0, 15.9, -6.2], [2.0, 1.0, 2.0], metaal(GOUD, (255, 240, 170)))])
    harnas(b, "dai_li_gewaad", stof((34, 66, 50), zoom=GOUD_DONKER, boven=(22, 42, 34)), boven=stof((34, 66, 50), strepen=((22, 42, 34), 20)))
    b.bot("dai_li_kraag", "head", [0, 1, -12], [b.kubus([-5.0, 0.0, -12.7], [10.0, 1.4, 0.8], stof((22, 42, 34), zoom=GOUD_DONKER, zoom_dik=0.3))])
    st = steen()
    for side, naam, sx in (("left", "links", 1), ("right", "rechts", -1)):
        x = 1.8 if sx > 0 else -6.2
        b.bot(f"dai_li_handschoen_{naam}", f"leg_front_{side}", [sx * 4, 1.4, -11.6], [
            b.kubus([x, 0.0, -14.4], [4.4, 3.0, 4.8], st), b.kubus([x + 0.5, 3.0, -13.6], [3.4, 0.6, 3.2], st)])
    L = 12.0
    idle = mika_idle(L, staart=3, kop=0.8)
    # the signature woven in: a stone glove shoots a short way out and snaps back
    idle["bones"]["dai_li_handschoen_rechts"] = {"position": kf([(0, [0, 0, 0]), (7.0, [0, 0, 0]), (7.15, [0, 0.6, -7]), (7.5, [0, 0.6, -7]), (7.9, [0, 0, 0])])}
    A = {"idle": idle, "praat": praat(),
         "stenen_hand": anim(1.2, {
             "dai_li_handschoen_rechts": {"position": kf([(0, [0, 0, 0]), (0.25, [0, 1.5, 1.5]), (0.4, [0, 1.0, -10]), (0.7, [0, 1.0, -10]), (1.0, [0, 0, 0]), (1.2, [0, 0, 0])]),
                                          "rotation": kf([(0, [0, 0, 0]), (0.4, [0, 0, 180]), (0.7, [0, 0, 360])])},
             "leg_front_right": {"position": kf([(0, [0, 0, 0]), (0.25, [0, 1.5, 1.5]), (0.4, [0, 1.0, -2]), (1.0, [0, 0, 0]), (1.2, [0, 0, 0])])},
             "body": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-5, 0, 0]), (0.4, [5, 0, 0]), (1.0, [0, 0, 0]), (1.2, [0, 0, 0])])}}, False),
         # rises out of the floor (a pose: it stays up; phase B starts it from below)
         "verschijn": anim(2.0, {"root": {"position": kf([(0, [0, -17, 0]), (1.2, [0, 0.6, 0]), (1.5, [0, 0, 0]), (2.0, [0, 0, 0])])},
                                 "dai_li_handschoen_links": {"position": kf([(0, [0, 3, 0]), (1.2, [0, 3, 0]), (1.6, [0, 0, 0]), (2.0, [0, 0, 0])])},
                                 "dai_li_handschoen_rechts": {"position": kf([(0, [0, 3, 0]), (1.2, [0, 3, 0]), (1.6, [0, 0, 0]), (2.0, [0, 0, 0])])}}, "hold_on_last_frame")}
    return b, A


# =====================================================================================================================
# the Vuurnatie-Mika's: soldiers and guards (three mix sets on one Kind)
# =====================================================================================================================
MIX_VUURNATIE = [["mix_0_helm", "mix_0_masker"], ["mix_1_helm", "mix_1_wangen"], ["mix_2_helm", "mix_2_sjaal"]]


def vuurnatie_mika(h):
    b = Blad(h, "vuurnatie_mika")
    snuit(b, brauw="boos", mond="zuur", lid=0.1)
    harnas(b, "vuurnatie_harnas", pantser(ROOD_DONKER, (40, 22, 28)), boven=pantser((54, 28, 34), ROOD))
    schouders(b, "vuurnatie_schouders", pantser((54, 28, 34), ROOD_LICHT))
    # the spear (it is a shoving stick), upright in the lifted paw
    px, py, pz = poot_opzij(b)
    hout = effen((120, 82, 52), 8)
    b.bot("vuurnatie_speer", "leg_front_right", [px, py, pz], [
        b.kubus([px - 0.5, 0.0, pz - 0.5], [1.0, 19.0, 1.0], hout, k=0.5),
        b.kubus([px - 1.0, 17.4, pz - 1.0], [2.0, 1.2, 2.0], effen(ROOD, 5)),
        b.kubus([px - 0.9, 19.0, pz - 0.4], [1.8, 2.2, 0.8], metaal(GRIJS, WIT)),
        b.kubus([px - 0.45, 21.2, pz - 0.3], [0.9, 1.6, 0.6], metaal(GRIJS, WIT))])
    zwart = (52, 26, 32)

    def schedel(W, H, rng):                                        # the white face plate: hollow eyes, a nose slit, teeth
        a = bd.plain((238, 234, 226), 6)(W, H, rng).astype(np.float32)
        for sx in (-1, 1):
            disc(a, W / 2 + sx * W * 0.24, H * 0.44, W * 0.13, (30, 16, 20), ry=H * 0.2)
            disc(a, W / 2 + sx * W * 0.24, H * 0.48, W * 0.045, (232, 44, 40), ry=H * 0.06)
        disc(a, W / 2, H * 0.68, W * 0.035, (30, 16, 20), ry=H * 0.07)
        for x in range(int(W * 0.26), int(W * 0.76), max(2, W // 14)):
            a[int(H * 0.84):H, x:x + 1, :3] = (60, 40, 44)
        a[int(H * 0.83):int(H * 0.86), int(W * 0.24):int(W * 0.78), :3] = (60, 40, 44)
        return np.clip(a, 0, 255).astype(np.uint8)
    # set 0: the soldier: the helmet with the white skull-like face plate
    helm0 = pantser(zwart, ROOD, platen=False)
    b.bot("mix_0_helm", "head", [0, 12, -6], [
        b.kubus([-8.5, 9.4, -12.6], [17.0, 5.2, 13.0], helm0, k=0.5), b.kubus([-8.9, 1.0, -9.4], [0.9, 8.4, 9.6], helm0, k=0.5),
        b.kubus([8.0, 1.0, -9.4], [0.9, 8.4, 9.6], helm0, k=0.5), b.kubus([-1.0, 14.6, -9.0], [2.0, 2.0, 6.0], effen(ROOD, 5))])
    b.bot("mix_0_masker", "head", [0, 6, -12.6], [b.kubus([-7.3, 0.8, -13.1], [14.6, 9.0, 0.9], effen((238, 234, 226), 5), overrides={"north": schedel})])
    # set 1: the guard: a pointed helmet, the face open
    helm1 = pantser(ROOD, GOUD, platen=False)
    b.bot("mix_1_helm", "head", [0, 12, -6], [
        b.kubus([-8.4, 11.0, -12.5], [16.8, 3.6, 12.9], helm1, k=0.5), b.kubus([-5.0, 14.6, -9.6], [10.0, 1.8, 7.4], helm1, k=0.5),
        b.kubus([-2.2, 16.4, -7.6], [4.4, 2.0, 3.6], helm1), b.kubus([-0.9, 18.4, -6.6], [1.8, 2.4, 1.6], metaal(GOUD, (255, 240, 170)))])
    b.bot("mix_1_wangen", "head", [0, 6, -11], b.paar([7.6, 1.6, -12.5], [1.0, 9.4, 4.4], helm1))
    # set 2: the marine of the fleet: a round helmet, a neck cloth
    helm2 = pantser((70, 40, 50), GRIJS_LICHT, platen=False)
    b.bot("mix_2_helm", "head", [0, 12, -6], [
        b.kubus([-8.3, 11.2, -12.4], [16.6, 2.6, 12.8], helm2, k=0.5), b.kubus([-7.0, 13.8, -11.2], [14.0, 1.6, 10.6], helm2, k=0.5),
        b.kubus([-5.0, 15.4, -9.6], [10.0, 1.1, 7.4], helm2, k=0.5), b.kubus([-1.4, 11.6, -12.8], [2.8, 2.4, 0.6], metaal(GOUD, (255, 240, 170)))])
    b.bot("mix_2_sjaal", "head", [0, 1, -12], [b.kubus([-5.6, 0.0, -12.9], [11.2, 2.0, 1.0], stof(ROOD_LICHT, zoom=WIT, zoom_dik=0.2))])
    L = 12.0
    idle = mika_idle(L, staart=5, kop=0.6)
    # the signature woven in: the watch: the whole Mika looks left, then right
    idle["bones"]["root"] = {"rotation": kf([(0, [0, 0, 0]), (5.0, [0, 0, 0]), (5.8, [0, 28, 0]), (7.2, [0, 28, 0]), (8.4, [0, -28, 0]), (9.8, [0, -28, 0]), (10.6, [0, 0, 0])])}
    A = {"idle": idle, "praat": praat(),
         "wacht": anim(4.0, {"root": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 32, 0]), (1.6, [0, 32, 0]), (2.4, [0, -32, 0]), (3.4, [0, -32, 0]), (4.0, [0, 0, 0])])},
                             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (1.0, [0, 0, -14]), (1.2, [0, 0, 0]), (4.0, [0, 0, 0])])},
                             "ear_right": {"rotation": kf([(0, [0, 0, 0]), (2.8, [0, 0, 14]), (3.0, [0, 0, 0]), (4.0, [0, 0, 0])])}}, False),
         # the spear sideways: move along
         "duw": anim(1.2, {"vuurnatie_speer": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, 84]), (0.9, [0, 0, 84]), (1.2, [0, 0, 0])]),
                                               "position": kf([(0, [0, 0, 0]), (0.25, [9, 5, -3]), (0.45, [9, 5, -7]), (0.7, [9, 5, -3]), (0.9, [9, 5, -3]), (1.2, [0, 0, 0])])},
                           "root": {"position": kf([(0, [0, 0, 0]), (0.25, [0, 0, 0]), (0.45, [0, 0, -3]), (0.9, [0, 0, 0]), (1.2, [0, 0, 0])])}}, False),
         "salueer": anim(1.6, {"leg_front_left": {"position": kf([(0, [0, 0, 0]), (0.3, [3.5, 8.5, 0]), (1.2, [3.5, 8.5, 0]), (1.6, [0, 0, 0])])},
                               "body": {"scale": kf([(0, [1, 1, 1]), (0.3, [1.04, 1.08, 1.0]), (1.2, [1.04, 1.08, 1.0]), (1.6, [1, 1, 1])])},
                               "tail": {"rotation": kf([(0, [0, 0, 0]), (0.3, [35, 0, 0]), (1.2, [35, 0, 0]), (1.6, [0, 0, 0])])},
                               "vuurnatie_speer": {"position": kf([(0, [0, 0, 0]), (0.3, [0, 1.5, 0]), (0.45, [0, 0, 0]), (1.6, [0, 0, 0])])}}, False)}
    return b, A


# =====================================================================================================================
# the gevangenisdirecteur
# =====================================================================================================================
def gevangenis_directeur_mika(h):
    b = Blad(h, "gevangenis_directeur_mika")
    grijs, licht = (150, 148, 156), (214, 212, 220)
    snuit(b, brauw=("dik", (120, 118, 128)), mond="zuur", lid=0.22)
    haardos(b, "directeur_haar", kleur=grijs, licht=licht, zij=False)
    knot(b, "directeur_knot", kleur=grijs, band=ROOD, maat=2.6)

    def embleem(W, H, rng):
        return vlam(kern=GEEL, rand=GOUD, punt=GOUD_DONKER, tongen=3)(W, H, rng)
    b.bot("directeur_hoofdband", "head", [0, 12, -6], [
        b.kubus([-8.25, 11.3, -12.3], [16.5, 1.5, 12.6], stof(ROOD, var=5), k=0.5),
        b.kubus([-1.5, 10.6, -12.75], [3.0, 3.0, 0.5], metaal(GOUD, (255, 240, 170)))])
    p = haren(grijs, licht, dicht=9)
    cubes = b.paar([0.3, 3.1, -13.3], [3.4, 1.3, 0.8], p) + b.paar([3.1, 1.7, -13.3], [1.1, 2.2, 0.8], p)
    cubes += [b.kubus([-1.4, 0.4, -13.2], [2.8, 1.9, 0.8], p), b.kubus([-0.8, 0.0, -13.25], [1.6, 0.8, 0.8], p)]
    b.bot("directeur_baard", "head", [0, 3, -13], cubes)

    def knopen(W, H, rng, a):
        for y in range(H // 5, H, max(5, H // 4)):
            disc(a, W * 0.5, y, 1.3, GOUD)
    harnas(b, "directeur_uniform", stof(ROOD, zoom=GOUD, boven=ROOD_DONKER), boven=met(stof(ROOD), knopen))
    b.bot("directeur_epauletten", "body", [0, 9, 2], b.paar([5.2, 9.6, -1.4], [4.6, 1.3, 5.4], metaal(GOUD, (255, 240, 170)))
          + b.paar([8.6, 7.6, -1.2], [1.2, 2.2, 5.0], stof(GOUD_DONKER, strepen=(GOUD, 5))))
    # the big ring of keys, held up in the lifted paw
    px, py, pz = poot_opzij(b, x=-11.4, y=1.6)
    ijzer, sleutel = metaal((120, 120, 132), GRIJS_LICHT), metaal(KOPER, (255, 226, 150))
    rx, ry, rz = px - 2.6, py + 0.2, pz - 0.3
    cubes = [b.kubus([rx, ry + 5.0, rz], [5.2, 0.7, 0.6], ijzer), b.kubus([rx, ry, rz], [5.2, 0.7, 0.6], ijzer),
             b.kubus([rx, ry, rz], [0.7, 5.7, 0.6], ijzer), b.kubus([rx + 4.5, ry, rz], [0.7, 5.7, 0.6], ijzer)]
    b.bot("directeur_sleutels", "leg_front_right", [px, ry + 5.4, rz + 0.3], cubes)
    for i, (dx, lang) in enumerate(((0.3, 3.6), (2.2, 4.4), (4.1, 3.2))):
        b.bot(f"directeur_sleutel_{i + 1}", "directeur_sleutels", [rx + dx + 0.4, ry + 0.3, rz + 0.3], [
            b.kubus([rx + dx, ry - lang, rz - 0.05], [0.8, lang + 0.3, 0.7], sleutel),
            b.kubus([rx + dx + 0.8, ry - lang, rz - 0.05], [1.0, 0.6, 0.7], sleutel), b.kubus([rx + dx + 0.8, ry - lang + 1.1, rz - 0.05], [0.7, 0.5, 0.7], sleutel)])
    L = 12.0
    rammel = {f"directeur_sleutel_{i}": {"rotation": golf(2, 6 + 2 * i, L, n=12, fase=i)} for i in (1, 2, 3)}
    idle = mika_idle(L, staart=6, kop=1.5, extra=rammel)
    # the signature woven in: he counts his keys
    for i in (1, 2, 3):
        idle["bones"][f"directeur_sleutel_{i}"] = {"rotation": kf([(0, [0, 0, 0]), (6.2 + i * 0.6, [0, 0, 0]), (6.4 + i * 0.6, [0, 0, 40]), (6.7 + i * 0.6, [0, 0, -12]), (7.0 + i * 0.6, [0, 0, 0])])}
    A = {"idle": idle, "praat": praat({"directeur_sleutels": {"rotation": golf(2, 8, 1.2)}}),
         "tier": anim(2.0, {
             "leg_front_right": {"position": kf([(0, [0, 0, 0]), (0.25, [0, 6, 0]), (1.6, [0, 6, 0]), (2.0, [0, 0, 0])])},
             "directeur_sleutels": {"rotation": kf([(0.25 + i * 0.15, [0, 0, 26 if i % 2 else -26]) for i in range(10)] + [(0, [0, 0, 0]), (2.0, [0, 0, 0])])},
             "head": {"rotation": kf([(i * 0.2, [0, 0, 5 if i % 2 else -5]) for i in range(10)] + [(2.0, [0, 0, 0])]),
                      "scale": kf([(0, [1, 1, 1]), (0.25, [1.06, 1.08, 1.06]), (1.6, [1.06, 1.08, 1.06]), (2.0, [1, 1, 1])])},
             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, 18]), (1.6, [0, 0, 18]), (2.0, [0, 0, 0])])},
             "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, -18]), (1.6, [0, 0, -18]), (2.0, [0, 0, 0])])}}, False),
         "sleutels": anim(2.4, {f"directeur_sleutel_{i}": {"rotation": kf([(0, [0, 0, 0]), (0.2 + i * 0.6, [0, 0, 0]), (0.4 + i * 0.6, [0, 0, 44]), (0.7 + i * 0.6, [0, 0, 0]), (2.4, [0, 0, 0])])}
                                for i in (1, 2, 3)} | {"head": {"rotation": kf([(0, [0, 0, 0]), (0.4, [0, 0, -8]), (2.0, [0, 0, -8]), (2.4, [0, 0, 0])])}}, False)}
    return b, A


# =====================================================================================================================
# the spijtige Mika-kok
# =====================================================================================================================
def mika_kok(h):
    b = Blad(h, "mika_kok")

    def traan(f):
        disc(f, 56 / 2 - 13.4 - 4.5, 31.5, 1.5, (150, 210, 255), ry=2.4)
    snuit(b, brauw="droevig", mond="zuur", lid=0.3, extra=traan)
    for side, sx in (("ear_left", 1), ("ear_right", -1)):                # drooping ears
        b.basisbot(side)["rotation"] = [0, 0, 52 * sx]
    wit = stof(WIT, var=4)
    b.bot("kok_muts", "head", [0, 13, -6], [b.kubus([-5.2, 12.8, -10.4], [10.4, 3.8, 9.0], stof(WIT, strepen=(GRIJS_LICHT, 6), var=4))])
    b.bot("kok_muts_bol", "kok_muts", [0, 16.4, -6], [b.kubus([-7.0, 16.2, -12.0], [14.0, 3.6, 12.2], wit, k=0.75),
                                                     b.kubus([-5.8, 19.6, -10.8], [10.0, 1.6, 9.6], wit, k=0.75),
                                                     b.kubus([-6.2, 15.4, -11.2], [12.4, 1.0, 10.6], wit, k=0.75)], rotation=[0, 0, -15])

    def vlekken(W, H, rng, a):
        for _ in range(max(2, W * H // 300)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.2, 2.6), (232, 196, 110), 0.7)
    schort = met(stof(WIT, zoom=GRIJS_LICHT, var=4), vlekken)
    harnas(b, "kok_schort", schort, boven=stof((206, 198, 190), strepen=(WIT, 9)))
    b.bot("kok_slab", "head", [0, 1, -12], [b.kubus([-4.6, 0.0, -12.9], [9.2, 2.6, 0.8], schort), b.kubus([-1.2, 2.2, -13.0], [2.4, 1.0, 0.9], effen(ROOD_LICHT, 5))])
    # the ladle, in the lifted paw
    px, py, pz = poot_opzij(b)
    staal = metaal(GRIJS, WIT)
    b.bot("kok_lepel", "leg_front_right", [px, py, pz], [
        b.kubus([px - 0.4, py - 1.0, pz - 0.4], [0.8, 10.5, 0.8], staal), b.kubus([px - 1.7, py + 9.4, pz - 1.7], [3.4, 1.0, 3.4], staal),
        b.kubus([px - 1.3, py + 10.3, pz - 1.3], [2.6, 0.5, 2.6], effen((232, 190, 96), 8))], rotation=[0, 0, 12])
    L = 12.0
    idle = mika_idle(L, staart=3, kop=2.5, oor=0)
    # the signature woven in: he looks at the floor and sighs
    idle["bones"]["head"]["position"] = kf([(0, [0, 0, 0]), (6.5, [0, 0, 0]), (7.5, [0, -0.5, -0.8]), (9.2, [0, -0.5, -0.8]), (10.2, [0, 0, 0])])
    idle["bones"]["body"]["scale"] = kf([(0, [1, 1, 1]), (3, [1.02, 1.03, 1.02]), (6.5, [1, 1, 1]), (7.5, [1.05, 1.08, 1.04]), (9.0, [0.98, 0.95, 1.0]), (10.2, [1, 1, 1]), (L, [1, 1, 1])])
    idle["bones"]["kok_muts_bol"] = {"rotation": golf(2, 3, L, n=8)}
    A = {"idle": idle, "praat": praat({"kok_muts_bol": {"rotation": golf(2, 6, 1.2)}}),
         "roer": anim(2.0, {"leg_front_right": {"position": kf([(i * 0.25, [2.0 * math.cos(i * math.pi / 2), 0.5, 2.0 * math.sin(i * math.pi / 2)]) for i in range(9)])},
                            "kok_lepel": {"rotation": kf([(0, [0, 0, 168]), (2.0, [0, 0, 168])]), "position": [0, 9, -3]},
                            "head": {"rotation": golf(2, 4, 2.0)}}, False),
         "spijt": anim(2.4, {"head": {"position": kf([(0, [0, 0, 0]), (0.6, [0, -0.7, -1.2]), (1.8, [0, -0.7, -1.2]), (2.4, [0, 0, 0])]),
                                      "rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, 7]), (1.8, [0, 0, 7]), (2.4, [0, 0, 0])])},
                             "body": {"scale": kf([(0, [1, 1, 1]), (0.6, [1.05, 1.08, 1.04]), (1.6, [0.97, 0.94, 1.0]), (2.4, [1, 1, 1])])},
                             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, 16]), (1.8, [0, 0, 16]), (2.4, [0, 0, 0])])},
                             "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, -16]), (1.8, [0, 0, -16]), (2.4, [0, 0, 0])])},
                             "kok_lepel": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, 30]), (1.8, [0, 0, 30]), (2.4, [0, 0, 0])])},
                             "tail": {"rotation": kf([(0, [0, 0, 0]), (0.6, [-10, 0, 0]), (1.8, [-10, 0, 0]), (2.4, [0, 0, 0])])}}, False)}
    return b, A


# =====================================================================================================================
# the vesting-kapitein
# =====================================================================================================================
def vestingkapitein_mika(h):
    b = Blad(h, "vestingkapitein_mika")
    snuit(b, brauw="boos", mond="grijns", lid=0.12)
    vilt, galon = stof((34, 36, 58), var=5), metaal(GOUD, (255, 240, 170))
    # the two-pointed hat, worn across: its points left and right, behind the ears
    cubes = [b.kubus([-10.0, 12.4, -5.8], [20.0, 1.6, 5.6], vilt, k=0.5), b.kubus([-7.6, 14.0, -5.4], [15.2, 2.6, 4.8], vilt, k=0.5),
             b.kubus([-4.6, 16.6, -5.0], [9.2, 1.8, 4.0], vilt, k=0.5), b.kubus([-2.0, 18.4, -4.6], [4.0, 1.0, 3.2], vilt),
             b.kubus([-10.0, 13.6, -6.0], [20.0, 0.5, 0.4], galon, k=0.5), b.kubus([-7.6, 16.2, -5.6], [15.2, 0.5, 0.4], galon, k=0.5),
             b.kubus([-4.6, 18.0, -5.2], [9.2, 0.5, 0.4], galon, k=0.5), b.kubus([-1.3, 14.4, -6.1], [2.6, 2.6, 0.5], rond(effen(ROOD_LICHT, 5)))]
    cubes += b.paar([9.6, 11.2, -5.6], [3.0, 1.8, 5.2], vilt) + b.paar([11.8, 10.0, -5.2], [1.8, 1.6, 4.4], vilt)
    b.bot("kapitein_steek", "head", [0, 13, -3], cubes)

    def knopen(W, H, rng, a):
        for y in range(H // 5, H, max(5, H // 4)):
            disc(a, W * 0.4, y, 1.3, GOUD)
            disc(a, W * 0.6, y, 1.3, GOUD)
    harnas(b, "kapitein_jas", stof(MARINE, zoom=GOUD, boven=(26, 34, 66)), boven=met(stof(MARINE), knopen))
    b.bot("kapitein_epauletten", "body", [0, 9, 2], b.paar([5.2, 9.8, -1.6], [5.4, 1.4, 5.8], galon)
          + b.paar([9.6, 7.6, -1.4], [1.2, 2.4, 5.4], stof(GOUD_DONKER, strepen=(GOUD, 5))))
    b.bot("kapitein_kraag", "head", [0, 1, -12], [b.kubus([-4.4, 0.0, -12.8], [8.8, 1.5, 0.8], stof(WIT, var=4))])
    # the spyglass, in the lifted paw
    px, py, pz = poot_opzij(b)
    koper, lens = metaal(KOPER, (255, 226, 150)), effen((150, 214, 240), 6)
    b.bot("kapitein_kijker", "leg_front_right", [px, py + 0.9, pz], [
        b.kubus([px - 0.9, py, pz - 5.0], [1.8, 1.8, 7.0], koper), b.kubus([px - 1.2, py - 0.3, pz - 8.6], [2.4, 2.4, 3.8], koper, overrides={"north": lens}),
        b.kubus([px - 0.7, py + 0.2, pz + 2.0], [1.4, 1.4, 1.4], koper)])
    L = 12.0
    idle = mika_idle(L, staart=8, kop=1.5)
    # the signature woven in: the spyglass goes to one eye
    idle["bones"]["leg_front_right"] = {"position": kf([(0, [0, 0, 0]), (6.0, [0, 0, 0]), (6.8, [7.2, 5.4, 0]), (9.2, [7.2, 5.4, 0]), (10.0, [0, 0, 0])])}
    A = {"idle": idle, "praat": praat(),
         "tuur": anim(3.0, {"leg_front_right": {"position": kf([(0, [0, 0, 0]), (0.5, [7.2, 5.4, 0]), (2.5, [7.2, 5.4, 0]), (3.0, [0, 0, 0])])},
                            "root": {"rotation": kf([(0, [0, 0, 0]), (0.9, [0, 22, 0]), (1.7, [0, -22, 0]), (2.5, [0, 0, 0]), (3.0, [0, 0, 0])])},
                            "tail": {"rotation": golf(1, 18, 3.0)}}, False),
         "brul": anim(1.6, {"head": {"scale": kf([(0, [1, 1, 1]), (0.25, [1.1, 1.14, 1.1]), (1.2, [1.1, 1.14, 1.1]), (1.6, [1, 1, 1])]),
                                     "rotation": kf([(0.25 + i * 0.1, [0, 0, 4 if i % 2 else -4]) for i in range(10)] + [(0, [0, 0, 0]), (1.6, [0, 0, 0])]),
                                     "position": kf([(0, [0, 0, 0]), (0.25, [0, 1.6, 0]), (1.2, [0, 1.6, 0]), (1.6, [0, 0, 0])])},
                            "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, 20]), (1.2, [0, 0, 20]), (1.6, [0, 0, 0])])},
                            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, -20]), (1.2, [0, 0, -20]), (1.6, [0, 0, 0])])},
                            "body": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-7, 0, 0]), (1.2, [-7, 0, 0]), (1.6, [0, 0, 0])])}}, False)}
    return b, A


# =====================================================================================================================
# Vuurheer Mikai: tall (two looks) and, after the finale, round
# =====================================================================================================================
MIKAI_BOTTEN = ["mikai_haar", "mikai_knot", "mikai_kroon", "mikai_baard", "mikai_mantel", "mikai_rok", "mikai_schouders", "mikai_mouw_left",
                "mikai_mouw_right", "mikai_vlammen", "mikai_vlammen_left", "mikai_vlammen_right", "mikai_knabbel", "mikai_kruimels",
                "arm_left", "arm_right", "leg_left", "leg_right", "leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"]


def _mantel(colour=ROOD, rand=GOUD):
    def paint(W, H, rng):
        a = stof(colour, var=6)(W, H, rng).astype(np.float32)
        a[:, :max(1, W // 8), :3] = rand
        a[:, W - max(1, W // 8):, :3] = rand
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def _mantel_voor(open_):
    """The front of the robe: black down the middle between golden bands (shut), or the bare chest (open)."""
    def paint(W, H, rng):
        a = stof(ROOD, var=6)(W, H, rng).astype(np.float32)
        m0, m1 = int(W * 0.3), int(W * 0.7)
        a[:, m0:m1, :3] = np.array((250, 208, 220) if open_ else (34, 24, 30), np.float32) + rng.normal(0, 3, (H, m1 - m0, 1))
        a[:, m0 - max(1, W // 12):m0, :3] = GOUD
        a[:, m1:m1 + max(1, W // 12), :3] = GOUD
        if not open_:
            a[H - max(1, H // 10):, :, :3] = GOUD
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def vel_(b):
    from features.einde2_cast_schurk_blad import vel
    return vel(b.vacht)


def vuurheer_mikai(h, komeet=False):
    kind = "vuurheer_mikai_komeet" if komeet else "vuurheer_mikai"
    b = Blad(h, kind, basis=None)
    sh, s = lang_lijf(b)
    K = OpKop(b, sh, s)
    if komeet:                                                     # the power of the knabbelkomeet: the eyes glow white-gold
        snuit(b, brauw="streng", mond="grijns", lid=0.1, oog=(255, 190, 60), oog_licht=(255, 252, 214), gloei=True)
    else:
        snuit(b, brauw="streng", mond="zuur", lid=0.3)
    haardos(K, "mikai_haar")
    b.basisbot("mikai_haar")["cubes"].append(b.kubus([-2.5, 12.6, 1.0], [5.0, 12.0, 1.4], haren(), k=1.5))   # long hair down his back
    knot(K, "mikai_knot", band=GOUD)
    vlamkroon(K, "mikai_kroon", y=16.6, z=-6.4, schaal=1.7)
    p = haren()
    K.bot("mikai_baard", "head", [0, 0.4, -12.6], [K.kubus([-1.3, -3.0, -13.0], [2.6, 3.4, 0.9], p), K.kubus([-0.8, -5.4, -12.9], [1.6, 2.4, 0.8], p),
                                                   K.kubus([-0.4, -7.0, -12.85], [0.8, 1.6, 0.7], p)])
    # the robe: red, black and gold, wide pointed shoulders
    voor, zij = _mantel_voor(komeet), _mantel()
    b.bot("mikai_mantel", "body", [0, 18, 0], [b.kubus([-2.5, 14.0, -1.5], [5.0, 8.3, 3.0], zij, overrides={"north": voor}, k=2.0)])
    rok = stof(ROOD, zoom=GOUD, zoom_dik=0.08)
    rok_voor = _mantel_voor(False)
    b.bot("mikai_rok", "body", [0, 14.3, 0], [b.kubus([-2.9, 4.6, -1.75], [5.8, 9.9, 3.5], rok, overrides={"north": rok_voor}, k=2.0),
                                              b.kubus([-3.5, 0.3, -2.2], [7.0, 4.6, 4.4], rok, overrides={"north": rok_voor}, k=2.0)])
    plaat = pantser((44, 26, 32), GOUD, platen=False)
    cubes = b.paar([1.6, 20.5, -1.7], [4.2, 1.6, 3.4], plaat, k=2.0) + b.paar([4.6, 21.6, -1.4], [2.0, 1.5, 2.8], plaat, k=2.0)
    cubes += b.paar([5.8, 22.7, -1.1], [1.3, 1.4, 2.2], plaat, k=2.0)
    b.bot("mikai_schouders", "body", [0, 21, 0], cubes)
    mouw = stof(ROOD, zoom=GOUD, zoom_dik=0.16)
    mouw_dwars = stof(ROOD, var=6)
    for side, sx in (("left", 1), ("right", -1)):
        x = 1.75 if sx > 0 else -3.75
        b.bot(f"mikai_mouw_{side}", f"arm_{side}", [sx * 2.6, 21.8, 0], [b.kubus([x, 13.6, -1.0], [2.0, 8.2, 2.0], mouw, k=2.0),
                                                                        b.kubus([x - 0.3, 12.6, -1.3], [2.6, 2.4, 2.6], mouw, k=2.0)])
    if komeet:                                                     # flames round both fists and along the shoulders
        vuur = vlam()
        cubes = []
        for sx in (-1, 1):
            cubes += kruisvlam(b, sx * 4.2, 22.0, 0, 3.4, 4.2, vuur) + kruisvlam(b, sx * 6.4, 23.6, 0, 2.6, 3.4, vuur)
        b.bot("mikai_vlammen", "body", [0, 22, 0], cubes)
        for side, sx in (("left", 1), ("right", -1)):
            b.bot(f"mikai_vlammen_{side}", f"arm_{side}", [sx * 2.6, 9.6, 0], kruisvlam(b, sx * 2.6, 6.6, 0, 4.6, 6.4, vuur))
            b.basisbot(f"arm_{side}")["rotation"] = [0, 0, -34 * sx]
    else:                                                          # arms folded: an upper arm down, the forearm across the chest
        arm = vel_(b)
        for side, sx, y in (("left", 1, 16.6), ("right", -1, 15.1)):
            x = 1.95 if sx > 0 else -3.25
            fx = -1.5 if sx > 0 else -3.25
            b.basisbot(f"arm_{side}")["cubes"] = [b.kubus([x, y, -0.65], [1.3, 22.1 - y, 1.3], arm, k=2.0),
                                                  b.kubus([fx, y, -3.0], [4.75, 1.3, 1.3], arm, k=2.0),
                                                  b.kubus([x, y, -3.0], [1.3, 1.3, 2.4], arm, k=2.0)]
            mx = -1.0 if sx > 0 else -3.75
            b.basisbot(f"mikai_mouw_{side}")["cubes"] = [b.kubus([x - 0.35, y - 0.2, -1.0], [2.0, 22.3 - y, 2.0], mouw, k=2.0),
                                                         b.kubus([mx, y - 0.35, -3.35], [4.75, 2.0, 2.0], mouw_dwars, k=2.0),
                                                         b.kubus([x - 0.35, y - 0.35, -3.35], [2.0, 2.0, 2.6], mouw, k=2.0)]
    lege_botten(b, MIKAI_BOTTEN)
    return b


def vuurheer_mikai_vadsig(h):
    """After the finale: an ordinary, VERY round, content Mika."""
    b = Blad(h, "vuurheer_mikai_vadsig")
    for c in b.basisbot("body")["cubes"]:                           # half again as wide (and a little higher)
        c["origin"] = [c["origin"][0] * 1.5, c["origin"][1], c["origin"][2] - 0.6]
        c["size"] = [c["size"][0] * 1.5, c["size"][1] * 1.12, c["size"][2] * 1.12]
    for side, sx in (("left", 1), ("right", -1)):
        poot = b.basisbot(f"leg_back_{side}")
        poot["cubes"][0]["origin"][0] += 3.0 * sx
        poot["pivot"][0] += 3.0 * sx

    def kruimels(f):
        for _ in range(26):
            x, y = b.rng.uniform(12, 44), b.rng.uniform(32, 43)
            f[int(y):int(y) + 2, int(x):int(x) + 2, :3] = KAAS if b.rng.random() < 0.6 else KAAS_GAT
    snuit(b, brauw="blij", mond="grijns", lid=0.52, onderlid=0.1, wang=(255, 130, 160), extra=kruimels)
    haardos(b, "mikai_haar")
    b.basisbot("mikai_haar")["cubes"].append(b.kubus([-5.0, 11.2, 0.0], [10.0, 1.0, 9.0], haren(), k=0.75))
    knot(b, "mikai_knot", band=GOUD, maat=2.6)
    # the crown: too small now, and askew
    vlamkroon(b, "mikai_kroon", y=16.4, z=-6.6, schaal=0.85, rotation=[0, 0, 24], pivot=[0, 16.4, -6.2])
    p = haren()
    b.bot("mikai_baard", "head", [0, 1, -12.6], [b.kubus([-3.4, 0.6, -13.0], [1.6, 1.5, 0.8], p), b.kubus([-3.1, 0.0, -13.05], [1.0, 0.8, 0.8], p)])

    def gaatjes(W, H, rng, a):
        for _ in range(max(2, W * H // 60)):
            disc(a, rng.uniform(1, W - 1), rng.uniform(1, H - 1), rng.uniform(0.9, 1.8), KAAS_GAT)
    kaas = met(bd.plain(KAAS, 6), gaatjes)
    b.bot("mikai_knabbel", "root", [0, 1, -15], [b.kubus([-2.3, 0.0, -17.0], [4.6, 3.4, 3.6], kaas), b.kubus([-1.5, 3.4, -16.4], [2.4, 0.9, 2.4], kaas)])
    kr = effen(KAAS, 10)
    b.bot("mikai_kruimels", "root", [0, 0, -15], [b.kubus([x, 0.0, z], [0.7, 0.5, 0.7], kr) for x, z in ((-6.4, -15.4), (5.6, -16.6), (3.2, -18.2), (-3.8, -18.0), (7.4, -14.2))])
    b.bot("mikai_sjerp", "body", [0, 5, 5], [b.kubus([-10.3, 3.2, -2.7], [20.6, 2.4, 14.8], stof(ROOD, zoom=GOUD, boven=GOUD, zoom_dik=0.2), k=0.5)])
    lege_botten(b, MIKAI_BOTTEN)
    return b


def mikai_animaties():
    """ONE file for the three looks (each look has every bone name, empty where it has nothing to show): the tall looks
    move arms and legs, the round one its four paws, its knabbel and its crown."""
    L = 12.0
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (3, [1.02, 1.02, 1.03]), (6, [1, 1, 1]), (7.2, [1.07, 1.03, 1.1]), (8.6, [1.07, 1.03, 1.1]), (9.6, [1, 1, 1]), (L, [1, 1, 1])])},
            "tail": {"rotation": golf(1, 8, L, n=16)},
            "head": {"rotation": golf(2, 1.5, L, n=8),
                     # the signature woven in: the chin goes up (the tall one looks down on you; the round one takes a bite)
                     "position": kf([(0, [0, 0, 0]), (6.4, [0, 0, 0]), (7.2, [0, 0.5, 0]), (8.6, [0, 0.5, 0]), (9.6, [0, 0, 0])])},
            "mikai_knabbel": {"position": kf([(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.5, [0, 1.6, 3.2]), (2.8, [0, 1.2, 3.2]), (3.1, [0, 1.6, 3.2]), (3.4, [0, 1.2, 3.2]), (4.0, [0, 0, 0])]),
                              "scale": kf([(0, [1, 1, 1]), (2.5, [1, 1, 1]), (3.4, [0.94, 0.9, 1]), (L, [1, 1, 1])])},
            "mikai_kroon": {"rotation": golf(2, 2, L, n=8)},
            "mikai_vlammen": {"scale": kf([(i * 0.5, [1 + 0.12 * math.sin(i * 2.3), 1 + 0.2 * math.sin(i * 1.7 + 1), 1]) for i in range(25)])},
            "mikai_vlammen_left": {"scale": kf([(i * 0.5, [1 + 0.1 * math.sin(i * 1.9 + 2), 1 + 0.22 * math.sin(i * 2.1), 1]) for i in range(25)])},
            "mikai_vlammen_right": {"scale": kf([(i * 0.5, [1 + 0.1 * math.sin(i * 2.2), 1 + 0.22 * math.sin(i * 1.6 + 2), 1]) for i in range(25)])},
            "mikai_haar": {"rotation": golf(0, 1.2, L, n=8)}}
    idle.update(oren(L))
    pr = praat({"arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-18, 0, 0]), (0.9, [-18, 0, 0]), (1.2, [0, 0, 0])])},
                "mikai_knabbel": {"position": kf([(0, [0, 0, 0]), (0.3, [0, 0.8, 0]), (0.6, [0, 0, 0]), (0.9, [0, 0.8, 0]), (1.2, [0, 0, 0])])}})
    brul = anim(1.6, {"head": {"scale": kf([(0, [1, 1, 1]), (0.25, [1.12, 1.16, 1.12]), (1.2, [1.12, 1.16, 1.12]), (1.6, [1, 1, 1])]),
                               "rotation": kf([(0.25 + i * 0.1, [0, 0, 4 if i % 2 else -4]) for i in range(10)] + [(0, [0, 0, 0]), (1.6, [0, 0, 0])])},
                      "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.25, [40, 50, -60]), (1.2, [40, 50, -60]), (1.6, [0, 0, 0])])},
                      "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.25, [40, -50, 60]), (1.2, [40, -50, 60]), (1.6, [0, 0, 0])])},
                      "body": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-8, 0, 0]), (1.2, [-8, 0, 0]), (1.6, [0, 0, 0])])},
                      "mikai_vlammen": {"scale": kf([(0, [1, 1, 1]), (0.3, [1.5, 1.8, 1.5]), (1.2, [1.4, 1.7, 1.4]), (1.6, [1, 1, 1])])}}, False)
    # one arm thrusts forward: a lukewarm flame
    stoot = anim(1.4, {"arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.2, [30, -40, 20]), (0.4, [-40, -58, 0]), (1.0, [-40, -58, 0]), (1.4, [0, 0, 0])])},
                       "body": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 16, 0]), (0.4, [4, -14, 0]), (1.0, [4, -14, 0]), (1.4, [0, 0, 0])])},
                       "leg_left": {"rotation": kf([(0, [0, 0, 0]), (0.4, [-18, 0, 0]), (1.0, [-18, 0, 0]), (1.4, [0, 0, 0])])},
                       "mikai_vlammen_right": {"scale": kf([(0, [1, 1, 1]), (0.4, [1.8, 2.2, 1.8]), (0.9, [1.4, 1.6, 1.4]), (1.4, [1, 1, 1])])}}, False)
    # sits (phase B puts the throne under him), chin up
    troon = anim(4.0, {"root": {"position": [0, -6.4, 0]}, "leg_left": {"rotation": [-88, -8, 0]}, "leg_right": {"rotation": [-88, 8, 0]},
                       "mikai_rok": {"rotation": [-80, 0, 0], "scale": [1, 0.72, 1]},
                       "body": {"scale": kf([(0, [1, 1, 1]), (2, [1.02, 1.02, 1.03]), (4, [1, 1, 1])])},
                       "head": {"position": [0, 0.5, 0], "rotation": golf(2, 1.5, 4.0)}, "tail": {"rotation": golf(1, 6, 4.0)},
                       "mikai_vlammen": {"scale": kf([(i * 0.5, [1, 1 + 0.2 * math.sin(i * 1.7), 1]) for i in range(9)])}})
    # rises, the flames grow
    komeet = anim(4.0, {"root": {"position": kf([(0, [0, 6, 0]), (1, [0, 7.5, 0]), (2, [0, 6, 0]), (3, [0, 7.5, 0]), (4, [0, 6, 0])])},
                        "arm_left": {"rotation": kf([(0, [0, 0, -50]), (2, [0, 0, -58]), (4, [0, 0, -50])])},
                        "arm_right": {"rotation": kf([(0, [0, 0, 50]), (2, [0, 0, 58]), (4, [0, 0, 50])])},
                        "leg_left": {"rotation": [8, 0, -4]}, "leg_right": {"rotation": [-6, 0, 4]}, "mikai_rok": {"rotation": golf(0, 3, 4.0)},
                        "head": {"position": [0, 0.5, 0]}, "mikai_haar": {"rotation": golf(0, 5, 4.0, om=(8, 0, 0))},
                        "mikai_vlammen": {"scale": kf([(i * 0.5, [1.6 + 0.2 * math.sin(i * 2.3), 2.0 + 0.3 * math.sin(i * 1.7), 1.6]) for i in range(9)])},
                        "mikai_vlammen_left": {"scale": kf([(i * 0.5, [1.6, 1.9 + 0.3 * math.sin(i * 2.1), 1.6]) for i in range(9)])},
                        "mikai_vlammen_right": {"scale": kf([(i * 0.5, [1.6, 1.9 + 0.3 * math.sin(i * 1.6 + 2), 1.6]) for i in range(9)])}})
    # the round one: munching, a burp, a wave
    smul = anim(2.0, {"mikai_knabbel": {"position": kf([(0, [0, 0, 0]), (0.3, [0, 1.6, 3.2])] + [(0.5 + i * 0.2, [0, 1.6 if i % 2 else 1.1, 3.2]) for i in range(6)] + [(2.0, [0, 0, 0])]),
                                        "scale": kf([(0, [1, 1, 1]), (0.5, [1, 1, 1]), (1.6, [0.9, 0.84, 1]), (2.0, [0.9, 0.84, 1])])},
                      "head": {"position": kf([(0.5 + i * 0.2, [0, 0.4 if i % 2 else 0, 0]) for i in range(6)] + [(0, [0, 0, 0]), (2.0, [0, 0, 0])])},
                      "leg_front_left": {"position": kf([(0, [0, 0, 0]), (0.3, [-0.8, 1.4, 0]), (1.6, [-0.8, 1.4, 0]), (2.0, [0, 0, 0])])},
                      "leg_front_right": {"position": kf([(0, [0, 0, 0]), (0.3, [0.8, 1.4, 0]), (1.6, [0.8, 1.4, 0]), (2.0, [0, 0, 0])])},
                      "tail": {"rotation": golf(1, 20, 2.0)}}, False)
    boer = anim(1.2, {"body": {"scale": kf([(0, [1, 1, 1]), (0.4, [1.08, 1.1, 1.06]), (0.55, [0.97, 0.95, 1.0]), (1.2, [1, 1, 1])])},
                      "head": {"position": kf([(0, [0, 0, 0]), (0.4, [0, -0.4, 0]), (0.55, [0, 1.6, -0.6]), (0.8, [0, 0.4, 0]), (1.2, [0, 0, 0])]),
                               "scale": kf([(0, [1, 1, 1]), (0.55, [1.06, 1.08, 1.06]), (0.9, [1, 1, 1])])},
                      "mikai_kroon": {"position": kf([(0, [0, 0, 0]), (0.55, [0, 1.6, 0]), (0.9, [0, 0, 0])]),
                                      "rotation": kf([(0, [0, 0, 0]), (0.55, [0, 0, 14]), (1.2, [0, 0, 0])])},
                      "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.55, [0, 0, 18]), (1.2, [0, 0, 0])])},
                      "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.55, [0, 0, -18]), (1.2, [0, 0, 0])])}}, False)
    zwaai = anim(1.6, {"leg_front_left": {"position": kf([(0, [0, 0, 0]), (0.25, [4.5, 6, 0])] + [(0.4 + i * 0.2, [4.5 + (1.4 if i % 2 else -1.4), 6.4, 0]) for i in range(5)] + [(1.6, [0, 0, 0])])},
                       "head": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, -6]), (1.3, [0, 0, -6]), (1.6, [0, 0, 0])])},
                       "tail": {"rotation": golf(1, 26, 1.6)}}, False)
    return {"idle": anim(L, idle), "praat": pr, "brul": brul, "vuurstoot": stoot, "troon": troon, "komeet": komeet, "smul": smul, "boer": boer, "zwaai": zwaai}


def _mikai(h, kind):
    if kind == "vuurheer_mikai_vadsig":
        return vuurheer_mikai_vadsig(h), mikai_animaties()
    return vuurheer_mikai(h, komeet=kind.endswith("komeet")), mikai_animaties()


# kind id -> maker(h) -> (Blad, {animation name: animation})
MAKERS = {
    "vuurheer_mikai": lambda h: _mikai(h, "vuurheer_mikai"),
    "vuurheer_mikai_komeet": lambda h: _mikai(h, "vuurheer_mikai_komeet"),
    "vuurheer_mikai_vadsig": lambda h: _mikai(h, "vuurheer_mikai_vadsig"),
    "azula_mika": azula_mika,
    "ty_lee_mika": ty_lee_mika,
    "zhao_mika": zhao_mika,
    "long_feng_mika": long_feng_mika,
    "dai_li_mika": dai_li_mika,
    "vuurnatie_mika": vuurnatie_mika,
    "gevangenis_directeur_mika": gevangenis_directeur_mika,
    "mika_kok": mika_kok,
    "vestingkapitein_mika": vestingkapitein_mika,
}
# bones the offline preview leaves out (props that only show during a move)
VERBORGEN = {"azula_mika": ("azula_bliksem",), "zhao_mika": ("zhao_rook",)}
