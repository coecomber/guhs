"""
1.10 "Het Vadsrijk", zone 2 "De Splinterpieken", slice wezens (Z2_BRIEF.md). Java: feature/vadsrijk/zone2/wezens.

Everything generated (no hand-made art), built on the zone-1 helpers of vadsrijk_wezens.py (Model, anim/kf, _vul/_patroon):
  * GeckoLib models + animations (assets/guhs/geckolib/{models,animations}/entity/vadsrijk/zone2/wezens/):
      Galmhellingen  splinterling (crystal golem), galmvleermuis (crystal-winged bat), sneeuwknabbelaar (snow pack predator),
                     kristalnaaldkruiper (low crystal crawler with needles)
      Knuffelmeer    ijzelgeest (ragged frost ghost), schotsbijter (big toothy ice fish-lizard), versteende_wacht (grey crystal
                     statue knight), ijspuffer (frost blowfish on stubby legs)
      Splinterkloof  mika_mijnwerker (upright Mika, hard hat + lamp + pickaxe), mika_boorder (Mika with a hand drill + engine),
                     kloofgier (grey vulture, crystal-crusted wings), kristalspin (grey spider, crystal shards on its back)
      Spiegelburcht  spiegelwacht (tall crystal guard + mirror shield + halberd), scherfgeest (floating mirror shards round a light)
      critters       sneeuwhaas, ijsvogel, kristalmot
    Real monsters, darker: no guh faces (only the critters have small friendly eyes); glowing eyes and crystals.
  * per creature: <naam>.png, <naam>_gloed.png (glowmask: eyes + crystals), <naam>_elite.png (the zone-1 additive gold glow)
    in textures/vadsrijk/zone2/wezens/.
  * 15 gear base types (items vr_z2_mob_*, icons in textures/item/vadsrijk/zone2/wezens/, armour with a worn look), all Dutch texts.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z2_wezens.py
"""
import json
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import vadsrijk_wezens as vw
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw

Model, kf, anim, anim_bestand = vw.Model, vw.kf, vw.anim, vw.anim_bestand
SUB = "vadsrijk/zone2/wezens"

# =====================================================================================================================
# faces (monsters: no guh faces) and painting
# =====================================================================================================================
OOG_GLOED = {"kristal": (170, 235, 255), "beest": (255, 130, 50), "mika": (255, 60, 50), "geest": (150, 230, 255), "spin": (255, 60, 70),
             "vogel": (255, 200, 60), "lief": None, "spiegel": (255, 240, 200)}


def _ellips(blok, cx, cy, rx, ry, kleur):
    h, w = blok.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    m = ((xx - cx) / max(0.5, rx)) ** 2 + ((yy - cy) / max(0.5, ry)) ** 2 <= 1
    blok[m, :3] = kleur
    return m


def oog(blok, soort, ey=0.4, glow=False):
    """Eyes on a front face. glow=True: the same eyes in their glow colour on the (black) glowmask."""
    h, w = blok.shape[:2]
    s = min(w, h)
    cy = h * ey
    ogen = (w * 0.5 - s * 0.24, w * 0.5 + s * 0.24)
    g = OOG_GLOED.get(soort)
    if soort == "lief":                                   # critters: small round black eyes with a highlight
        if glow:
            return
        for ex in ogen:
            vw._cirkel(blok, ex, cy, max(1.2, s * 0.1), (20, 18, 24))
            vw._cirkel(blok, ex - s * 0.03, cy - s * 0.03, max(0.6, s * 0.035), (250, 250, 255))
        return
    if soort == "spin":
        for i, (dx, dy, r) in enumerate(((-0.28, 0, 0.08), (0.28, 0, 0.08), (-0.12, -0.08, 0.11), (0.12, -0.08, 0.11), (-0.4, 0.1, 0.05),
                                         (0.4, 0.1, 0.05))):
            if not glow:
                vw._cirkel(blok, w * 0.5 + s * dx, cy + s * dy, s * r + 1, (20, 10, 14))
            vw._cirkel(blok, w * 0.5 + s * dx, cy + s * dy, s * r * 0.6 + 0.5, g)
        return
    for i, ex in enumerate(ogen):
        binnen = 1 if i == 0 else -1                      # +1 toward the middle for the left eye
        if soort == "kristal":                            # narrow cold slits in a dark socket
            if not glow:
                _ellips(blok, ex, cy, s * 0.15, s * 0.07, (24, 22, 34))
            _ellips(blok, ex, cy, s * 0.11, s * 0.035, g)
        elif soort == "geest":                            # hollow black holes with a cold light deep inside
            if not glow:
                _ellips(blok, ex, cy, s * 0.12, s * 0.16, (10, 12, 20))
            _ellips(blok, ex, cy + s * 0.02, s * 0.05, s * 0.06, g)
        elif soort in ("beest", "vogel"):                 # small fierce eyes with a slit pupil
            if not glow:
                _ellips(blok, ex, cy, s * 0.11, s * 0.08, (20, 14, 12))
            _ellips(blok, ex, cy, s * 0.08, s * 0.055, g)
            if not glow:
                blok[int(cy - s * 0.05):int(cy + s * 0.05) + 1, int(ex) - 0:int(ex) + 1, :3] = (10, 6, 6)
                vw._lijn(blok, [(ex - binnen * s * 0.16, cy - s * 0.16), (ex + binnen * s * 0.12, cy - s * 0.07)], (30, 24, 28), max(1, int(s * 0.05)))
        elif soort == "mika":                             # the Mika's own evil face: red eyes, black slanted brows
            if not glow:
                vw._cirkel(blok, ex, cy, s * 0.12, (30, 10, 14))
            vw._cirkel(blok, ex, cy, s * 0.095, g)
            if not glow:
                blok[int(cy - s * 0.07):int(cy + s * 0.07) + 1, int(ex):int(ex) + 1, :3] = (16, 6, 8)
                vw._lijn(blok, [(ex - binnen * s * 0.18, cy - s * 0.22), (ex + binnen * s * 0.14, cy - s * 0.1)], (24, 14, 20), max(1, int(s * 0.07)))
        elif soort == "spiegel":
            _ellips(blok, ex, cy, s * 0.1, s * 0.03, g)
    if glow:
        return
    my = cy + s * 0.3
    if soort == "beest":                                  # a row of teeth
        for k in range(-2, 3):
            x = w * 0.5 + k * s * 0.09
            vw._lijn(blok, [(x - s * 0.035, my), (x, my + s * 0.1), (x + s * 0.035, my)], (236, 232, 220), max(1, int(s * 0.04)))
    elif soort == "mika":                                 # a smirk with one fang
        mx = w * 0.5
        vw._lijn(blok, [(mx - s * 0.16, my), (mx, my + s * 0.06), (mx + s * 0.16, my - s * 0.02)], (40, 14, 22), max(1, int(s * 0.04)))
        vw._lijn(blok, [(mx + s * 0.05, my + s * 0.04), (mx + s * 0.08, my + s * 0.13)], (250, 250, 250), max(1, int(s * 0.04)))


def schilder(model, palet, seed, glow_rollen=None, glow=False):
    """palet: rol -> (kleur, patroon or None, tweede kleur). glow=True: the glowmask (black, glow roles + eyes)."""
    S = model.px
    img = np.zeros((model.hoogte() * S, model.W * S, 4), np.uint8)
    img[..., 3] = 255 if glow else 0
    r = vw._rng(seed)
    for reg in model.regios:
        x, y, w, h = reg["r"]
        blok = img[y * S:(y + h) * S, x * S:(x + w) * S]
        if glow:
            if glow_rollen and reg["rol"] in glow_rollen:
                vw._vul(blok, glow_rollen[reg["rol"]], r, 8)
        else:
            kleur, pat, k2 = palet.get(reg["rol"], ((255, 0, 255), None, None))
            f = reg["f"]
            donker = 0.78 if f == "down" else 0.9 if f in ("east", "west", "south") else 1.0
            vw._vul(blok, tuple(np.array(kleur) * donker), r)
            if pat:
                vw._patroon(blok, pat, k2, r, f)
        if reg["gezicht"]:
            oog(blok, reg["gezicht"].get("mood", "beest"), reg["gezicht"].get("ey", 0.4), glow)
    return Image.fromarray(img)


# =====================================================================================================================
# animation helpers
# =====================================================================================================================
def zwaai(bone, a, t=0.6, as_="x"):
    i = {"x": 0, "y": 1, "z": 2}[as_]
    v = lambda s: tuple(s if j == i else 0 for j in range(3))
    return {bone: {"rotation": kf([(0, v(a)), (t / 2, v(-a)), (t, v(a))])}}


def samen(*ds):
    out = {}
    for d in ds:
        for k, v in d.items():
            out.setdefault(k, {}).update(v)
    return out


def adem(bone, t=2.0, f=1.03):
    return {bone: {"scale": kf([(0, (1, 1, 1)), (t / 2, (f, f, f)), (t, (1, 1, 1))])}}


def stil(bone="root"):
    return {bone: {"rotation": kf([(0, (0, 0, 0))])}}


def een(lengte, bones):
    return anim(lengte, bones, loop=False)


def leg_anims(m, a):
    """Every anim the Java code may ask for exists (RijkWezen plays "blij" after a defeat: invisible anyway, but it must exist)."""
    a.setdefault("blij", anim(1.0, stil()))
    a.setdefault("aanval", een(0.4, stil()))
    return a


# =====================================================================================================================
# builders
# =====================================================================================================================
def golem(naam, hoog=1.0, schild=False, wapen=None, px=4, W=128):
    """A big biped of grey crystal (splinterling, versteende_wacht, spiegelwacht). hoog scales the legs/body."""
    m = Model(naam, px=px, breedte=W, bounds=(3, 4, 1.5))
    L = 10 * hoog
    m.bone("root")
    m.bone("been_l", "root", (-3.5, L, 0))
    m.cube("been_l", "steen", (-6.5, 0, -2.5), (5, L, 5))
    m.bone("been_r", "root", (3.5, L, 0))
    m.spiegel("been_r", "steen", (-6.5, 0, -2.5), (5, L, 5))
    m.bone("lijf", "root", (0, L, 0))
    m.cube("lijf", "steen", (-7, L, -4), (14, 12 * hoog, 8))
    m.cube("lijf", "donker", (-5, L + 1, -4.4), (10, 5, 0.5))
    m.bone("kop", "lijf", (0, L + 12 * hoog, 0))
    top = L + 12 * hoog
    m.cube("kop", "steen", (-4, top, -4.5), (8, 7, 7), gezicht={"mood": "kristal", "ey": 0.45})
    m.cube("kop", "kristal", (-1, top + 7, -1.5), (2, 4, 2), rot=(0, 0, 10), pivot=(0, top + 7, -0.5))
    m.bone("kristallen", "lijf", (0, top, 2))
    for (x, y, z, hg, rx, rz) in ((-5, top - 2, 2, 7, -20, 18), (4, top - 1, 1.5, 8, -15, -20), (-1, top - 4, 3.5, 6, -35, 5), (6, top - 6, 2, 5, -10, -35),
                                  (-7, top - 7, 1, 5, 0, 35)):
        m.cube("kristallen", "kristal", (x, y, z), (2.4, hg, 2.4), rot=(rx, 0, rz), pivot=(x + 1.2, y, z + 1.2))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "arm_" + kant
        m.bone(b, "lijf", (sx * 8, top - 1, 0))
        ox = -12 if sx < 0 else 7
        m.cube(b, "steen", (ox, top - 14 * hoog, -2.5), (5, 14 * hoog, 5))
        m.cube(b, "kristal", (ox + 1, top - 15 * hoog - 2, -1.5), (3, 3, 3))
    if schild:
        m.bone("schild", "arm_l", (-10, top - 7, -3))
        m.cube("schild", "spiegel", (-13, top - 15, -5), (1.5, 14, 10), gezicht=None)
        m.cube("schild", "steen", (-13.6, top - 16, -5.5), (0.8, 16, 11))
    if wapen == "hellebaard":
        m.bone("wapen", "arm_r", (9.5, top - 13, 0))
        m.cube("wapen", "steel", (8.7, top - 22, -0.8), (1.6, 32, 1.6))
        m.cube("wapen", "kristal", (8.2, top + 6, -4.5), (2.6, 7, 9))
    a = {
        "idle": anim(2.4, samen(adem("lijf", 2.4, 1.02), {"kristallen": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 3, 0)), (2.4, (0, 0, 0))])}})),
        "loop": anim(1.2, samen(zwaai("been_l", 22, 1.2), zwaai("been_r", -22, 1.2), zwaai("arm_l", -14, 1.2), zwaai("arm_r", 14, 1.2),
                                {"lijf": {"rotation": kf([(0, (0, 0, -3)), (0.6, (0, 0, 3)), (1.2, (0, 0, -3))])}})),
        "aanval": een(0.6, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.25, (-120, 0, 0)), (0.4, (20, 0, 0)), (0.6, (0, 0, 0))])}}),
        "speciaal": een(0.5, {"kristallen": {"scale": kf([(0, (1, 1, 1)), (0.15, (1.35, 1.35, 1.35)), (0.5, (1, 1, 1))])},
                              "lijf": {"position": kf([(0, (0, 0, 0)), (0.1, (0, 0, 1)), (0.5, (0, 0, 0))])}}),
        "hef": anim(1.0, {"arm_l": {"rotation": kf([(0, (-160, 0, 15))])}, "arm_r": {"rotation": kf([(0, (-160, 0, -15))])},
                          "lijf": {"rotation": kf([(0, (-10, 0, 0))])}}),
        "klap": een(0.6, {"arm_l": {"rotation": kf([(0, (-160, 0, 15)), (0.15, (-30, 0, 5)), (0.6, (0, 0, 0))])},
                          "arm_r": {"rotation": kf([(0, (-160, 0, -15)), (0.15, (-30, 0, -5)), (0.6, (0, 0, 0))])},
                          "lijf": {"rotation": kf([(0, (-10, 0, 0)), (0.15, (18, 0, 0)), (0.6, (0, 0, 0))])}}),
        "uithaal": anim(1.0, {"arm_r": {"rotation": kf([(0, (-30, 70, -40))])}, "lijf": {"rotation": kf([(0, (0, 30, 0))])}}),
    }
    return m, a


def vierpoot(naam, lang=12, hoog=6, kop=6, poot=5, W=96, px=4, gezicht="beest", staart=True):
    """A four-legged beast; front = -z."""
    m = Model(naam, px=px, breedte=W, bounds=(3, 2.5, 0.6))
    m.bone("root")
    m.bone("lijf", "root", (0, poot + hoog / 2, 0))
    m.cube("lijf", "vacht", (-hoog * 0.7, poot, -lang / 2), (hoog * 1.4, hoog, lang))
    m.bone("kop", "lijf", (0, poot + hoog * 0.7, -lang / 2))
    m.cube("kop", "vacht", (-kop / 2, poot + hoog * 0.4, -lang / 2 - kop), (kop, kop * 0.85, kop), gezicht={"mood": gezicht, "ey": 0.35})
    m.cube("kop", "snuit", (-kop * 0.3, poot + hoog * 0.4, -lang / 2 - kop - kop * 0.45), (kop * 0.6, kop * 0.45, kop * 0.45))
    for nm, x, z in (("poot_lv", -hoog * 0.45, -lang / 2 + 2), ("poot_rv", hoog * 0.45, -lang / 2 + 2), ("poot_la", -hoog * 0.45, lang / 2 - 2),
                     ("poot_ra", hoog * 0.45, lang / 2 - 2)):
        m.bone(nm, "root", (x, poot, z))
        m.cube(nm, "poot", (x - 1.2, 0, z - 1.2), (2.4, poot, 2.4))
    if staart:
        m.bone("staart", "lijf", (0, poot + hoog * 0.7, lang / 2), (-30, 0, 0))
        m.cube("staart", "vacht", (-1, poot + hoog * 0.5, lang / 2), (2, 2, kop))
    lk = lambda a: {"rotation": kf([(0, (a, 0, 0)), (0.2, (-a, 0, 0)), (0.4, (a, 0, 0))])}
    a = {
        "idle": anim(2.0, samen(adem("lijf", 2.0, 1.02), {"kop": {"rotation": kf([(0, (0, 0, 0)), (1, (0, 8, 0)), (2, (0, 0, 0))])}})),
        "loop": anim(0.4, {"poot_lv": lk(35), "poot_ra": lk(35), "poot_rv": lk(-35), "poot_la": lk(-35),
                           "lijf": {"position": kf([(0, (0, 0, 0)), (0.1, (0, 0.5, 0)), (0.2, (0, 0, 0)), (0.3, (0, 0.5, 0)), (0.4, (0, 0, 0))])}}),
        "aanval": een(0.45, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.12, (-25, 0, 0)), (0.25, (25, 0, 0)), (0.45, (0, 0, 0))]),
                                     "position": kf([(0, (0, 0, 0)), (0.25, (0, 0, -2)), (0.45, (0, 0, 0))])}}),
    }
    return m, a


def vlieger(naam, lijf=(5, 4, 7), vleugel=(12, 0.6, 7), kop=4, W=96, px=4, gezicht="beest", snavel=False):
    """A flyer: body, head, two wings (bones vleugel_l/vleugel_r flap around z)."""
    bx, by, bz = lijf
    m = Model(naam, px=px, breedte=W, bounds=(4, 2, 0.5))
    m.bone("root")
    m.bone("lijf", "root", (0, 4, 0))
    m.cube("lijf", "vacht", (-bx / 2, 4, -bz / 2), (bx, by, bz))
    m.bone("kop", "lijf", (0, 4 + by * 0.6, -bz / 2))
    m.cube("kop", "vacht", (-kop / 2, 4 + by * 0.3, -bz / 2 - kop), (kop, kop, kop), gezicht={"mood": gezicht, "ey": 0.4})
    if snavel:
        m.cube("kop", "snavel", (-0.8, 4 + by * 0.3 + kop * 0.2, -bz / 2 - kop - 2), (1.6, 1.4, 2))
    vx, vy, vz = vleugel
    m.bone("vleugel_l", "lijf", (-bx / 2, 4 + by - 0.5, 0))
    m.cube("vleugel_l", "vleugel", (-bx / 2 - vx, 4 + by - 1, -vz / 2), (vx, vy, vz))
    m.bone("vleugel_r", "lijf", (bx / 2, 4 + by - 0.5, 0))
    m.spiegel("vleugel_r", "vleugel", (-bx / 2 - vx, 4 + by - 1, -vz / 2), (vx, vy, vz))
    m.bone("staart", "lijf", (0, 4 + by * 0.5, bz / 2))
    m.cube("staart", "vleugel", (-bx * 0.35, 4 + by * 0.4, bz / 2), (bx * 0.7, 0.6, vz * 0.5))
    flap = lambda t, a: samen({"vleugel_l": {"rotation": kf([(0, (0, 0, a)), (t / 2, (0, 0, -a)), (t, (0, 0, a))])}},
                              {"vleugel_r": {"rotation": kf([(0, (0, 0, -a)), (t / 2, (0, 0, a)), (t, (0, 0, -a))])}},
                              {"lijf": {"position": kf([(0, (0, 0, 0)), (t / 2, (0, 0.8, 0)), (t, (0, 0, 0))])}})
    a = {
        "idle": anim(0.5, flap(0.5, 30)),
        "vlieg": anim(0.4, flap(0.4, 40)),
        "aanval": een(0.4, samen({"kop": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-30, 0, 0)), (0.3, (25, 0, 0)), (0.4, (0, 0, 0))])}}, flap(0.4, 50))),
        "duik": anim(1.0, {"vleugel_l": {"rotation": kf([(0, (0, 30, 60))])}, "vleugel_r": {"rotation": kf([(0, (0, -30, -60))])},
                           "lijf": {"rotation": kf([(0, (35, 0, 0))])}}),
        "ram": een(0.5, {"lijf": {"rotation": kf([(0, (60, 0, 0)), (0.5, (0, 0, 0))])}}),
        "uitgeput": anim(1.2, {"vleugel_l": {"rotation": kf([(0, (0, 0, -70)), (0.6, (0, 0, -60)), (1.2, (0, 0, -70))])},
                               "vleugel_r": {"rotation": kf([(0, (0, 0, 70)), (0.6, (0, 0, 60)), (1.2, (0, 0, 70))])},
                               "kop": {"rotation": kf([(0, (20, 0, 0))])}, "lijf": {"position": kf([(0, (0, -3, 0))])}}),
    }
    return m, a


def zwever(naam, hoog=14, breed=7, W=96, px=4, gezicht="geest"):
    """A floating ghost: a tapering body of ragged layers, arms, no legs; shards orbiting (bone baan)."""
    m = Model(naam, px=px, breedte=W, bounds=(3, 3, 1))
    m.bone("root")
    m.bone("lijf", "root", (0, 6, 0))
    for i in range(4):
        b = breed - i * 1.4
        m.cube("lijf", "mantel", (-b / 2, 2 + i * (hoog / 4), -b / 2 + 0.3), (b, hoog / 4 + 0.5, b - 0.6))
    m.bone("kop", "lijf", (0, 2 + hoog, 0))
    m.cube("kop", "mantel", (-3.5, 2 + hoog, -3.5), (7, 7, 7), gezicht={"mood": gezicht, "ey": 0.48})
    m.cube("kop", "kristal", (-3.8, 2 + hoog + 6, -1), (7.6, 1.5, 2))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "arm_" + kant
        m.bone(b, "lijf", (sx * breed / 2, hoog - 1, 0), (0, 0, sx * 15))
        ox = -breed / 2 - 2.5 if sx < 0 else breed / 2
        m.cube(b, "mantel", (ox, hoog - 10, -1.2), (2.5, 10, 2.4))
    m.bone("baan", "root", (0, hoog * 0.7, 0))
    for i in range(5):
        hk = i * 72 * np.pi / 180
        x, z = np.cos(hk) * 7, np.sin(hk) * 7
        m.cube("baan", "kristal", (x - 0.8, hoog * 0.7 + (i % 2) * 2, z - 0.8), (1.6, 3.5, 1.6), rot=(20, i * 30, 15), pivot=(x, hoog * 0.7, z))
    zweef = {"lijf": {"position": kf([(0, (0, 0, 0)), (1.0, (0, 1.2, 0)), (2.0, (0, 0, 0))])},
             "baan": {"rotation": kf([(0, (0, 0, 0)), (2.0, (0, 180, 0)), (4.0, (0, 360, 0))])}}
    a = {
        "idle": anim(4.0, samen(zweef, {"arm_l": {"rotation": kf([(0, (0, 0, -15)), (2, (0, 0, -22)), (4, (0, 0, -15))])}})),
        "vlieg": anim(4.0, samen(zweef, {"lijf": {"rotation": kf([(0, (12, 0, 0))])}})),
        "spreuk": een(0.6, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-110, 0, -20)), (0.6, (0, 0, 0))])},
                            "arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-110, 0, 20)), (0.6, (0, 0, 0))])}}),
        "speciaal": een(1.0, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.4, (1.2, 0.9, 1.2)), (1.0, (1, 1, 1))])},
                              "baan": {"scale": kf([(0, (1, 1, 1)), (0.4, (1.5, 1, 1.5)), (1.0, (1, 1, 1))])}}),
        "aanval": een(0.5, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-90, 0, 0)), (0.5, (0, 0, 0))])}}),
    }
    return m, a


def mika(naam, gereedschap):
    """An upright Mika (the cat-like villains: pink fur, round ears, thin tail, evil red eyes) in mining gear."""
    m = Model(naam, px=4, breedte=96, bounds=(3, 3, 1))
    m.bone("root")
    for kant, sx in (("l", -1), ("r", 1)):
        b = "been_" + kant
        m.bone(b, "root", (sx * 2, 9, 0))
        m.cube(b, "broek", (sx * 2 - 1.5, 2, -1.5), (3, 7, 3))
        m.cube(b, "laars", (sx * 2 - 1.8, 0, -2.3), (3.6, 2.5, 4))
    m.bone("lijf", "root", (0, 9, 0))
    m.cube("lijf", "vacht", (-3.5, 9, -2.2), (7, 9, 4.4))
    m.cube("lijf", "vest", (-3.8, 10, -2.5), (7.6, 7, 5))
    m.bone("staart", "lijf", (0, 10, 2.2), (-40, 0, 0))
    m.cube("staart", "vacht", (-0.6, 9.5, 2.2), (1.2, 1.2, 9))
    m.bone("kop", "lijf", (0, 18, 0))
    m.cube("kop", "vacht", (-4.5, 18, -4.5), (9, 8, 8), gezicht={"mood": "mika", "ey": 0.5})
    m.cube("kop", "snuit", (-2, 18.5, -5.3), (4, 2.5, 1))
    for sx in (-1, 1):
        m.cube("kop", "vacht", (sx * 3.3 - 1.7, 25, -1.5), (3.4, 3.4, 1.2))
        m.cube("kop", "oor", (sx * 3.3 - 1.1, 25.5, -1.7), (2.2, 2.2, 0.4))
    m.cube("kop", "helm", (-5, 25.5, -5), (10, 2.2, 9))
    m.cube("kop", "helm", (-3.5, 27.5, -3.5), (7, 1.8, 6))
    m.cube("kop", "lamp", (-1.2, 25.8, -5.8), (2.4, 2, 1))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "arm_" + kant
        m.bone(b, "lijf", (sx * 4.5, 17, 0))
        ox = -6 if sx < 0 else 3.5
        m.cube(b, "vacht", (ox + (0 if sx < 0 else 0), 9.5, -1.2), (2.5, 8, 2.5))
        m.cube(b, "poot", (ox, 8.5, -1.3), (2.5, 1.5, 2.6))
    if gereedschap == "houweel":
        m.bone("wapen", "arm_r", (4.7, 9.5, 0))
        m.cube("wapen", "steel", (4.1, 9, -0.6), (1.2, 12, 1.2), rot=(-60, 0, 0), pivot=(4.7, 9.5, 0))
        m.cube("wapen", "ijzer", (3.6, 19.5, -4.5), (2.2, 1.6, 9), rot=(-60, 0, 0), pivot=(4.7, 9.5, 0))
    else:
        m.bone("wapen", "arm_r", (4.7, 9.5, 0))
        m.cube("wapen", "ijzer", (2.8, 8.5, -6), (3.8, 3.8, 6))
        m.cube("wapen", "boor", (3.7, 9.4, -10.5), (2, 2, 4.5))
        m.bone("motor", "lijf", (0, 12, 3))
        m.cube("motor", "ijzer", (-3, 11, 2.4), (6, 6, 3.5))
        m.cube("motor", "steel", (-2, 17, 3.5), (1.2, 3, 1.2))
    lk = lambda a, t=0.6: {"rotation": kf([(0, (a, 0, 0)), (t / 2, (-a, 0, 0)), (t, (a, 0, 0))])}
    a = {
        "idle": anim(2.0, samen(adem("lijf", 2.0, 1.02), {"staart": {"rotation": kf([(0, (-40, -15, 0)), (1, (-40, 15, 0)), (2, (-40, -15, 0))])}})),
        "loop": anim(0.6, {"been_l": lk(30), "been_r": lk(-30), "arm_l": lk(-25), "staart": {"rotation": kf([(0, (-30, -20, 0)), (0.3, (-30, 20, 0)),
                                                                                                             (0.6, (-30, -20, 0))])}}),
        "ren": anim(0.4, {"been_l": lk(45, 0.4), "been_r": lk(-45, 0.4), "arm_l": lk(-60, 0.4), "arm_r": lk(60, 0.4),
                          "lijf": {"rotation": kf([(0, (15, 0, 0))])}}),
        "aanval": een(0.5, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-100, 0, 0)), (0.35, (30, 0, 0)), (0.5, (0, 0, 0))])}}),
        "hef": anim(1.0, {"arm_r": {"rotation": kf([(0, (-170, 0, 0))])}, "arm_l": {"rotation": kf([(0, (-150, 0, 0))])}}),
        "klap": een(0.5, {"arm_r": {"rotation": kf([(0, (-170, 0, 0)), (0.15, (20, 0, 0)), (0.5, (0, 0, 0))])},
                          "arm_l": {"rotation": kf([(0, (-150, 0, 0)), (0.15, (20, 0, 0)), (0.5, (0, 0, 0))])}}),
        "mik": anim(0.3, {"arm_r": {"rotation": kf([(0, (-85, 0, 0)), (0.15, (-88, 0, 2)), (0.3, (-85, 0, 0))])},
                          "arm_l": {"rotation": kf([(0, (-80, 30, 0))])}}),
        "spuug": een(0.4, {"arm_r": {"rotation": kf([(0, (-85, 0, 0)), (0.1, (-70, 0, 0)), (0.4, (0, 0, 0))])},
                           "wapen": {"scale": kf([(0, (1, 1, 1)), (0.1, (1.2, 1.2, 0.8)), (0.4, (1, 1, 1))])}}),
    }
    return m, a


# =====================================================================================================================
# the creatures
# =====================================================================================================================
GRIJS = (138, 136, 150)
KRISTAL_GRIJS = ((190, 192, 208), "facet", (150, 150, 170))


def m_splinterling():
    m, a = golem("splinterling", 1.0)
    pal = {"steen": ((112, 110, 124), "plekjes", (96, 94, 108)), "donker": ((70, 68, 82), None, None), "kristal": KRISTAL_GRIJS}
    return m, a, pal, {"kristal": (60, 64, 90), "donker": (20, 22, 40)}


def m_versteende_wacht():
    m, a = golem("versteende_wacht", 1.25)
    m.bone("helm", "kop", (0, 30, 0))
    m.cube("helm", "kristal", (-4.6, 29.5, -5), (9.2, 3, 8))
    pal = {"steen": ((128, 126, 136), "facet", (112, 110, 122)), "donker": ((86, 84, 96), None, None), "kristal": ((168, 166, 182), "facet", (140, 138, 154))}
    return m, a, pal, {"kristal": (40, 40, 60)}


def m_spiegelwacht():
    m, a = golem("spiegelwacht", 1.15, schild=True, wapen="hellebaard")
    pal = {"steen": ((150, 150, 168), "facet", (128, 128, 146)), "donker": ((60, 60, 78), None, None), "kristal": ((214, 214, 232), "facet", (170, 170, 196)),
           "spiegel": ((226, 232, 244), "glans", None), "steel": ((90, 86, 100), None, None)}
    return m, a, pal, {"kristal": (80, 80, 110), "spiegel": (120, 126, 150)}


def m_galmvleermuis():
    m, a = vlieger("galmvleermuis", lijf=(5, 4.5, 6), vleugel=(13, 0.6, 8), kop=4.5)
    for sx in (-1, 1):
        m.cube("kop", "vacht", (sx * 1.6 - 0.9, 9.5, -4.5), (1.8, 3.5, 0.8))
    for i in range(3):
        m.cube("vleugel_l", "kristal", (-7 - i * 3.5, 8, -4.2 + i), (1.2, 1.2, 4 - i))
        m.spiegel("vleugel_r", "kristal", (-7 - i * 3.5, 8, -4.2 + i), (1.2, 1.2, 4 - i))
    pal = {"vacht": ((70, 64, 82), "plekjes", (56, 50, 66)), "snuit": ((90, 80, 96), None, None), "vleugel": ((96, 88, 120), None, None),
           "kristal": ((200, 196, 240), "facet", (160, 150, 220)), "poot": ((60, 54, 70), None, None)}
    return m, a, pal, {"kristal": (110, 100, 170)}


def m_sneeuwknabbelaar():
    m, a = vierpoot("sneeuwknabbelaar", lang=14, hoog=7, kop=6.5, poot=6)
    m.cube("lijf", "manen", (-5.5, 11.5, -7.5), (11, 3, 7))
    m.cube("kop", "vacht", (-3.4, 15.4, -10.5), (2, 2.4, 1), rot=(0, 0, -15), pivot=(-2.4, 15.4, -10))
    m.spiegel("kop", "vacht", (-3.4, 15.4, -10.5), (2, 2.4, 1), rot=(0, 0, -15), pivot=(-2.4, 15.4, -10))
    a["loer"] = anim(0.6, {"lijf": {"position": kf([(0, (0, -2, 0))]), "rotation": kf([(0, (8, 0, 0))])},
                           "kop": {"rotation": kf([(0, (10, 0, 0)), (0.3, (8, 0, 0)), (0.6, (10, 0, 0))])}})
    a["speciaal"] = een(1.2, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-45, 0, 0)), (1.0, (-45, 0, 0)), (1.2, (0, 0, 0))])}})
    pal = {"vacht": ((214, 216, 224), "plekjes", (188, 190, 202)), "manen": ((166, 168, 182), "strepen", (150, 152, 166)),
           "snuit": ((196, 196, 206), None, None), "poot": ((150, 150, 162), None, None)}
    return m, a, pal, {}


def m_kristalnaaldkruiper():
    m, a = vierpoot("kristalnaaldkruiper", lang=16, hoog=4.5, kop=5.5, poot=3, gezicht="kristal")
    m.bone("naalden", "lijf", (0, 7.5, 0))
    for i, (x, z, h, rx, rz) in enumerate(((-2, -5, 9, -15, 10), (1.5, -2, 11, 5, -12), (-1, 1.5, 10, 10, 8), (2, 4.5, 8, 20, -18), (-2.5, 6, 7, 25, 20),
                                          (0, -7, 6, -25, 0))):
        m.cube("naalden", "kristal", (x - 0.8, 7, z - 0.8), (1.6, h, 1.6), rot=(rx, 0, rz), pivot=(x, 7, z))
    # buried: everything below the snow except the needles
    a["verstopt"] = anim(2.0, {"root": {"position": kf([(0, (0, -9, 0))])},
                               "naalden": {"scale": kf([(0, (1, 1, 1)), (1, (1, 1.03, 1)), (2, (1, 1, 1))])}})
    a["tril"] = anim(0.2, {"root": {"position": kf([(0, (-0.3, -7, 0)), (0.1, (0.3, -6.5, 0)), (0.2, (-0.3, -7, 0))])}})
    a["speciaal"] = een(0.6, {"root": {"position": kf([(0, (0, -7, 0)), (0.2, (0, 2, 0)), (0.6, (0, 0, 0))])},
                              "naalden": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.3, 1.3, 1.3)), (0.6, (1, 1, 1))])}})
    pal = {"vacht": ((96, 94, 108), "facet", (82, 80, 94)), "snuit": ((80, 78, 92), None, None), "poot": ((70, 68, 80), None, None),
           "kristal": ((206, 206, 224), "facet", (160, 160, 184))}
    return m, a, pal, {"kristal": (70, 70, 100)}


def m_schotsbijter():
    m, a = vierpoot("schotsbijter", lang=20, hoog=7, kop=9, poot=3, W=128)
    m.bone("kaak", "kop", (0, 6.4, -10))
    m.cube("kaak", "snuit", (-3.6, 4.6, -22), (7.2, 2, 11))
    for i in range(6):
        m.cube("kaak", "tand", (-3.2 + i * 1.25, 6.6, -21.5), (0.7, 1.4, 0.7))
    m.bone("kam", "lijf", (0, 10, 0))
    for i in range(5):
        m.cube("kam", "kristal", (-0.8, 10, -8 + i * 4), (1.6, 4 - abs(i - 2) * 0.8, 2.5))
    a["onder"] = anim(2.0, {"root": {"position": kf([(0, (0, -16, 0))])}})
    a["speciaal"] = een(0.8, {"root": {"position": kf([(0, (0, -14, 0)), (0.25, (0, 3, 0)), (0.8, (0, 0, 0))])},
                              "kaak": {"rotation": kf([(0, (0, 0, 0)), (0.25, (35, 0, 0)), (0.5, (0, 0, 0))])}})
    a["aanval"] = een(0.5, {"kaak": {"rotation": kf([(0, (0, 0, 0)), (0.15, (40, 0, 0)), (0.3, (0, 0, 0))])},
                            "kop": {"position": kf([(0, (0, 0, 0)), (0.2, (0, 0, -2)), (0.5, (0, 0, 0))])}})
    pal = {"vacht": ((80, 104, 128), "plekjes", (64, 86, 110)), "snuit": ((96, 120, 142), None, None), "poot": ((60, 80, 100), None, None),
           "tand": ((236, 240, 244), None, None), "kristal": KRISTAL_GRIJS}
    return m, a, pal, {"kristal": (50, 60, 90)}


def m_ijspuffer():
    m = Model("ijspuffer", px=4, breedte=96, bounds=(3, 2.5, 0.6))
    m.bone("root")
    m.bone("lijf", "root", (0, 8, 0))
    m.cube("lijf", "huid", (-6, 3, -6), (12, 11, 12), gezicht={"mood": "beest", "ey": 0.4})
    m.cube("lijf", "buik", (-5, 2.6, -5), (10, 1, 10))
    m.bone("stekels", "lijf", (0, 8, 0))
    for i in range(10):
        hk = i * 36 * np.pi / 180
        x, z = np.cos(hk) * 6.2, np.sin(hk) * 6.2
        m.cube("stekels", "kristal", (x - 0.6, 6 + (i % 3) * 2.5, z - 0.6), (1.2, 1.2, 1.2), rot=(0, i * 36, 45), pivot=(x, 6 + (i % 3) * 2.5, z))
    for nm, x, z in (("poot_l", -3.5, 0), ("poot_r", 3.5, 0)):
        m.bone(nm, "root", (x, 3, z))
        m.cube(nm, "poot", (x - 1.5, 0, z - 2), (3, 3, 4))
    m.bone("vin", "lijf", (0, 14, 2))
    m.cube("vin", "vin", (-0.4, 14, -1), (0.8, 3, 5))
    lk = lambda a: {"rotation": kf([(0, (a, 0, 0)), (0.3, (-a, 0, 0)), (0.6, (a, 0, 0))])}
    a = {
        "idle": anim(1.6, samen(adem("lijf", 1.6, 1.04), {"vin": {"rotation": kf([(0, (0, 0, -10)), (0.8, (0, 0, 10)), (1.6, (0, 0, -10))])}})),
        "loop": anim(0.6, {"poot_l": lk(30), "poot_r": lk(-30), "lijf": {"rotation": kf([(0, (0, 0, -6)), (0.3, (0, 0, 6)), (0.6, (0, 0, -6))])}}),
        "aanval": een(0.4, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.15, (0, 0, -3)), (0.4, (0, 0, 0))])}}),
        "opblazen": anim(0.75, {"lijf": {"scale": kf([(0, (1.0, 1.0, 1.0)), (0.75, (1.5, 1.5, 1.5))])},
                                "stekels": {"scale": kf([(0, (1, 1, 1)), (0.75, (1.4, 1.4, 1.4))])}}, loop="hold"),
        "puf": een(0.6, {"lijf": {"scale": kf([(0, (1.5, 1.5, 1.5)), (0.2, (0.85, 0.85, 0.85)), (0.6, (1, 1, 1))])}}),
    }
    pal = {"huid": ((150, 196, 222), "plekjes", (126, 172, 204)), "buik": ((214, 232, 240), None, None), "poot": ((110, 150, 180), None, None),
           "vin": ((120, 170, 210), None, None), "kristal": ((220, 240, 255), "facet", (170, 210, 240))}
    return m, a, pal, {"kristal": (60, 90, 120)}


def m_ijzelgeest():
    m, a = zwever("ijzelgeest", hoog=15, breed=8)
    pal = {"mantel": ((178, 206, 228), "strepen", (150, 182, 210)), "kristal": ((226, 244, 255), "facet", (180, 220, 248))}
    return m, a, pal, {"kristal": (80, 130, 170), "mantel": (24, 40, 56)}


def m_scherfgeest():
    m, a = zwever("scherfgeest", hoog=9, breed=5.5, gezicht="spiegel")
    m.bone("hart", "lijf", (0, 10, 0))
    m.cube("hart", "licht", (-1.5, 9, -1.5), (3, 3, 3))
    pal = {"mantel": ((150, 150, 166), "facet", (124, 124, 142)), "kristal": ((224, 224, 236), "facet", (180, 180, 200)),
           "licht": ((255, 236, 190), None, None)}
    return m, a, pal, {"licht": (255, 220, 150), "kristal": (120, 110, 90)}


def m_mika_mijnwerker():
    m, a = mika("mika_mijnwerker", "houweel")
    pal = {"vacht": ((212, 150, 170), "plekjes", (196, 136, 156)), "snuit": ((236, 176, 192), None, None), "oor": ((240, 150, 170), None, None),
           "poot": ((240, 170, 186), None, None), "vest": ((210, 150, 50), "strepen", (60, 56, 60)), "broek": ((70, 74, 96), None, None),
           "laars": ((60, 44, 36), None, None), "helm": ((236, 196, 60), None, None), "lamp": ((255, 248, 200), None, None),
           "steel": ((120, 86, 56), None, None), "ijzer": ((150, 152, 164), None, None)}
    return m, a, pal, {"lamp": (255, 240, 170)}


def m_mika_boorder():
    m, a = mika("mika_boorder", "boor")
    pal = {"vacht": ((196, 140, 160), "plekjes", (180, 126, 146)), "snuit": ((226, 168, 184), None, None), "oor": ((236, 146, 166), None, None),
           "poot": ((236, 166, 182), None, None), "vest": ((90, 92, 100), "strepen", (220, 120, 40)), "broek": ((80, 66, 56), None, None),
           "laars": ((50, 40, 34), None, None), "helm": ((220, 110, 40), None, None), "lamp": ((255, 248, 200), None, None),
           "steel": ((60, 60, 66), None, None), "ijzer": ((128, 130, 142), "strepen", (100, 100, 112)), "boor": ((200, 200, 210), "strepen", (150, 150, 160))}
    return m, a, pal, {"lamp": (255, 240, 170), "boor": (90, 40, 10)}


def m_kloofgier():
    m, a = vlieger("kloofgier", lijf=(7, 6, 11), vleugel=(20, 0.8, 11), kop=4.5, W=128, gezicht="vogel", snavel=True)
    m.cube("kop", "nek", (-2.5, 9.2, -8.5), (5, 2, 3))
    for i in range(4):
        m.cube("vleugel_l", "kristal", (-9 - i * 4, 9.6, -3 + i * 0.5), (2.4, 1, 3))
        m.spiegel("vleugel_r", "kristal", (-9 - i * 4, 9.6, -3 + i * 0.5), (2.4, 1, 3))
    for sx in (-1, 1):
        m.bone("klauw_" + ("l" if sx < 0 else "r"), "lijf", (sx * 2, 4, 1))
        m.cube("klauw_" + ("l" if sx < 0 else "r"), "snavel", (sx * 2 - 0.8, 1, 0), (1.6, 3, 2))
    pal = {"vacht": ((96, 90, 92), "plekjes", (80, 74, 76)), "vleugel": ((70, 66, 70), "veer", (50, 46, 50)), "nek": ((214, 208, 200), None, None),
           "snavel": ((226, 196, 120), None, None), "kristal": KRISTAL_GRIJS}
    return m, a, pal, {"kristal": (50, 50, 70)}


def m_kristalspin():
    m = Model("kristalspin", px=4, breedte=128, bounds=(3, 2, 0.5))
    m.bone("root")
    m.bone("lijf", "root", (0, 6, 0))
    m.cube("lijf", "vacht", (-4, 4, -5), (8, 5, 8))
    m.bone("achterlijf", "lijf", (0, 6, 3))
    m.cube("achterlijf", "vacht", (-5.5, 4, 3), (11, 8, 11))
    for i, (x, z, h, rx, rz) in enumerate(((-2.5, 5, 6, 20, 15), (2, 6.5, 7, 25, -15), (0, 9.5, 5, 40, 0), (-3.5, 10, 4, 35, 25), (3.5, 10.5, 4, 35, -25))):
        m.cube("achterlijf", "kristal", (x - 0.9, 11, z), (1.8, h, 1.8), rot=(rx, 0, rz), pivot=(x, 11, z + 0.9))
    m.bone("kop", "lijf", (0, 6.5, -5))
    m.cube("kop", "vacht", (-3, 4.5, -10), (6, 4.5, 5), gezicht={"mood": "spin", "ey": 0.4})
    m.cube("kop", "tand", (-2, 3.2, -10.6), (1, 2, 1))
    m.cube("kop", "tand", (1, 3.2, -10.6), (1, 2, 1))
    for i in range(4):
        z = -3.5 + i * 2.6
        for kant, sx in (("l", -1), ("r", 1)):
            b = f"poot_{kant}{i}"
            m.bone(b, "lijf", (sx * 4, 6.5, z), (0, sx * (30 - i * 20), sx * -35))
            ox = -14 if sx < 0 else 4
            m.cube(b, "poot", (ox, 5.8, z - 0.6), (10, 1.3, 1.3))
    lp = {}
    for i in range(4):
        for kant, sx in (("l", -1), ("r", 1)):
            fase = 0.2 if (i + (0 if sx < 0 else 1)) % 2 else 0
            lp[f"poot_{kant}{i}"] = {"rotation": kf([(0, (0, sx * (30 - i * 20) + 12 * (1 if fase else -1), sx * -35)),
                                                     (0.2, (0, sx * (30 - i * 20) - 12 * (1 if fase else -1), sx * -30)),
                                                     (0.4, (0, sx * (30 - i * 20) + 12 * (1 if fase else -1), sx * -35))])}
    a = {
        "idle": anim(2.0, samen(adem("achterlijf", 2.0, 1.03))),
        "loop": anim(0.4, lp),
        "aanval": een(0.4, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.12, (-20, 0, 0)), (0.25, (20, 0, 0)), (0.4, (0, 0, 0))])}}),
        "spuug": een(0.5, {"achterlijf": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-35, 0, 0)), (0.5, (0, 0, 0))])}}),
    }
    pal = {"vacht": ((92, 90, 98), "plekjes", (72, 70, 80)), "poot": ((70, 68, 76), None, None), "tand": ((190, 186, 200), None, None),
           "kristal": ((200, 198, 220), "facet", (156, 154, 180))}
    return m, a, pal, {"kristal": (70, 66, 100)}


# critters (passive, small friendly eyes: a tiny guh touch)
def m_sneeuwhaas():
    m, a = vierpoot("sneeuwhaas", lang=7, hoog=5, kop=4, poot=2.5, W=64, gezicht="lief", staart=False)
    for sx in (-1, 1):
        m.cube("kop", "vacht", (sx * 1.1 - 0.7, 9.5, -6.5), (1.4, 5, 0.8), rot=(-15, 0, sx * 8), pivot=(sx * 1.1, 9.5, -6))
        m.cube("kop", "oor", (sx * 1.1 - 0.45, 10, -6.7), (0.9, 4, 0.3), rot=(-15, 0, sx * 8), pivot=(sx * 1.1, 9.5, -6))
    m.bone("pluim", "lijf", (0, 6, 3.5))
    m.cube("pluim", "buik", (-1.2, 5.5, 3.5), (2.4, 2.4, 2))
    a["loop"] = anim(0.5, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.15, (0, 2.5, 0)), (0.35, (0, 0.5, 0)), (0.5, (0, 0, 0))]),
                                    "rotation": kf([(0, (0, 0, 0)), (0.15, (-15, 0, 0)), (0.35, (10, 0, 0)), (0.5, (0, 0, 0))])}})
    pal = {"vacht": ((244, 244, 250), None, None), "snuit": ((236, 230, 236), None, None), "poot": ((230, 230, 238), None, None),
           "oor": ((240, 190, 200), None, None), "buik": ((255, 255, 255), None, None)}
    return m, a, pal, {}


def m_ijsvogel():
    m, a = vlieger("ijsvogel", lijf=(3, 2.8, 4.5), vleugel=(5, 0.4, 3.5), kop=2.8, W=64, gezicht="lief", snavel=True)
    a["loop"] = anim(0.4, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.2, (0, 0.8, 0)), (0.4, (0, 0, 0))])}})
    a["idle"] = anim(2.0, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 25, 0)), (1.2, (0, -20, 0)), (2.0, (0, 0, 0))])}})
    pal = {"vacht": ((120, 190, 230), None, None), "vleugel": ((70, 130, 200), "veer", (50, 100, 170)), "snavel": ((240, 160, 60), None, None)}
    return m, a, pal, {}


def m_kristalmot():
    m, a = vlieger("kristalmot", lijf=(2, 2, 4), vleugel=(5, 0.3, 5), kop=2, W=64, gezicht="lief")
    a["loop"] = a["vlieg"]
    pal = {"vacht": ((200, 190, 230), None, None), "vleugel": ((224, 214, 255), "facet", (180, 160, 240))}
    return m, a, pal, {"vleugel": (120, 100, 200), "vacht": (40, 36, 60)}


WEZENS = [("splinterling", m_splinterling), ("galmvleermuis", m_galmvleermuis), ("sneeuwknabbelaar", m_sneeuwknabbelaar),
          ("kristalnaaldkruiper", m_kristalnaaldkruiper), ("ijzelgeest", m_ijzelgeest), ("schotsbijter", m_schotsbijter),
          ("versteende_wacht", m_versteende_wacht), ("ijspuffer", m_ijspuffer), ("mika_mijnwerker", m_mika_mijnwerker),
          ("mika_boorder", m_mika_boorder), ("kloofgier", m_kloofgier), ("kristalspin", m_kristalspin), ("spiegelwacht", m_spiegelwacht),
          ("scherfgeest", m_scherfgeest)]
CRITTERS = [("sneeuwhaas", m_sneeuwhaas), ("ijsvogel", m_ijsvogel), ("kristalmot", m_kristalmot)]

# animations the Java code triggers or holds, per creature (the generator checks they exist)
NODIG = {
    "splinterling": ["idle", "loop", "aanval", "speciaal", "blij"],
    "galmvleermuis": ["idle", "vlieg", "aanval", "blij"],
    "sneeuwknabbelaar": ["idle", "loop", "aanval", "loer", "speciaal", "blij"],
    "kristalnaaldkruiper": ["idle", "loop", "aanval", "verstopt", "tril", "speciaal", "blij"],
    "ijzelgeest": ["idle", "vlieg", "spreuk", "speciaal", "aanval", "blij"],
    "schotsbijter": ["idle", "loop", "aanval", "onder", "speciaal", "blij"],
    "versteende_wacht": ["idle", "loop", "aanval", "hef", "klap", "blij"],
    "ijspuffer": ["idle", "loop", "aanval", "opblazen", "puf", "blij"],
    "mika_mijnwerker": ["idle", "loop", "aanval", "hef", "ren", "klap", "blij"],
    "mika_boorder": ["idle", "loop", "aanval", "mik", "spuug", "blij"],
    "kloofgier": ["idle", "vlieg", "aanval", "uitgeput", "duik", "ram", "blij"],
    "kristalspin": ["idle", "loop", "aanval", "spuug", "blij"],
    "spiegelwacht": ["idle", "loop", "aanval", "uithaal", "speciaal", "blij"],
    "scherfgeest": ["idle", "vlieg", "spreuk", "aanval", "blij"],
    "sneeuwhaas": ["idle", "loop"], "ijsvogel": ["idle", "loop", "vlieg"], "kristalmot": ["idle", "loop", "vlieg"],
}


# =====================================================================================================================
# gear
# =====================================================================================================================
HARNAS = {
    # id: (slot, name, colour, accent, set)
    "sneeuwmuts": ("hoofd", "Sneeuwmuts", (230, 236, 246), (150, 196, 230), None),
    "galmpantser": ("borst", "Galmpantser", (150, 146, 180), (210, 206, 240), None),
    "schotsbroek": ("benen", "Schotsbroek", (110, 150, 190), (230, 240, 250), None),
    "ijzelslofjes": ("voeten", "IJzelslofjes", (170, 214, 240), (255, 255, 255), None),
    "kristalkroon": ("hoofd", "Kristalkroon", (196, 196, 220), (255, 160, 210), "kristal"),
    "kristalborstplaat": ("borst", "Kristalborstplaat", (196, 196, 220), (255, 160, 210), "kristal"),
    "kristalbeenstukken": ("benen", "Kristalbeenstukken", (196, 196, 220), (255, 160, 210), "kristal"),
    "kristalschoentjes": ("voeten", "Kristalschoentjes", (196, 196, 220), (255, 160, 210), "kristal"),
    "mijnhelm": ("hoofd", "Mika-mijnhelm", (236, 196, 60), (255, 248, 200), "mika"),
    "buitvest": ("borst", "Buitvest", (210, 150, 50), (60, 56, 60), "mika"),
    "boorbroek": ("benen", "Boorbroek", (80, 76, 96), (220, 120, 40), "mika"),
    "kaaslaarzen": ("voeten", "Kaaslaarzen", (250, 214, 96), (190, 128, 36), "mika"),
}
SIERADEN = {
    "kristalring": ("ring", "Kristalring", (220, 214, 236), (255, 140, 200)),
    "spiegelketting": ("ketting", "Spiegelketting", (210, 210, 224), (250, 250, 255)),
    "galmoorbel": ("oorbel", "Galmoorbel", (200, 196, 240), (150, 140, 230)),
}


def sieraad_icoon(soort, kleur, steen, seed):
    r = vw._rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    img = Image.fromarray(a)
    from PIL import ImageDraw
    d = ImageDraw.Draw(img)
    k = tuple(int(c) for c in kleur) + (255,)
    dk = tuple(int(c * 0.55) for c in kleur) + (255,)
    s = tuple(int(c) for c in steen) + (255,)
    if soort == "ring":
        d.ellipse((3, 5, 12, 14), outline=dk, width=3)
        d.ellipse((3, 5, 12, 14), outline=k, width=2)
        d.polygon([(7, 1), (10, 3), (7, 6), (4, 3)], fill=s, outline=dk)
    elif soort == "ketting":
        d.arc((2, 0, 13, 11), 20, 160, fill=dk, width=2)
        d.arc((2, 0, 13, 11), 20, 160, fill=k, width=1)
        d.polygon([(7, 9), (10, 12), (7, 15), (4, 12)], fill=s, outline=dk)
    else:
        d.ellipse((5, 1, 9, 5), outline=k, width=1)
        d.line((7, 5, 7, 8), fill=dk, width=1)
        d.polygon([(7, 8), (10, 11), (7, 15), (4, 11)], fill=s, outline=dk)
    out = np.asarray(img).copy()
    m = out[..., 3] > 0
    out[m, :3] = np.clip(out[m, :3].astype(int) + r.normal(0, 5, (m.sum(), 3)).astype(int), 0, 255)
    return Image.fromarray(out)


ENTITEITEN = {
    "splinterling": "Splinterling", "galmvleermuis": "Galmvleermuis", "sneeuwknabbelaar": "Sneeuwknabbelaar",
    "kristalnaaldkruiper": "Kristalnaaldkruiper", "ijzelgeest": "IJzelgeest", "schotsbijter": "Schotsbijter", "versteende_wacht": "Versteende Wacht",
    "ijspuffer": "IJspuffer", "mika_mijnwerker": "Mika-mijnwerker", "mika_boorder": "Mika-boorder", "kloofgier": "Kloofgier",
    "kristalspin": "Grauwe Kristalspin", "spiegelwacht": "Spiegelwacht", "scherfgeest": "Scherfgeest",
    "sneeuwhaas": "Sneeuwhaas", "ijsvogel": "IJsvogeltje", "kristalmot": "Kristalmot",
}

FLAVOUR = {
    "sneeuwmuts": ["Gebreid door een guh met koude oortjes.", "Houdt je hoofd warm en je gedachten koel.", "Er zit nog een sneeuwvlok in. Hij blijft."],
    "galmpantser": ["Galmt zachtjes na als je erop klopt. Niet doen.", "Hoorde iemand dat? Dat was je pantser.", "Gesmeed van een zingende kristalnaald."],
    "schotsbroek": ["Getest op dun ijs. Meestal geslaagd.", "Stevig als een ijsschots, minder glad.", "Met extra knieën voor op het meer."],
    "ijzelslofjes": ["Glijden is een keuze. Deze sloffen kiezen ja.", "Warm van binnen, ijzig van buiten.", "Je laat kleine ijsbloemetjes achter."],
    "kristalkroon": ["Het grauw is eruit gepoetst. Bijna.", "Weegt niks, glimt alles.", "Een Splinterling droeg hem als hoofd. Was."],
    "kristalborstplaat": ["Klinkt als een klokje als je een knuffel krijgt.", "Doorzichtig genoeg om je hartje te zien.", "Kou kaatst er gewoon vanaf."],
    "kristalbeenstukken": ["Rinkel, rinkel, daar komt de held.", "Knieën van kristal, moed van pluis.", "Niet tegen tafelpoten stoten."],
    "kristalschoentjes": ["Elke stap een belletje.", "Heel mooi. Heel koud. Neem sokken mee.", "Glazen muiltjes, maar dan stoerder."],
    "mijnhelm": ["Nog warm van een Mika-hoofd. Bah.", "De lamp doet het. Het lampje van de Mika niet meer.", "Ruikt een beetje naar kaas en buskruit."],
    "buitvest": ["Zakken vol gestolen spullen. Nu leeg. Nu van jou.", "Twaalf zakken, nul geweten.", "Gebuit van een Mika, dus eerlijk gebuit."],
    "boorbroek": ["Trilt nog na van het boren.", "Met een extra zak voor reserveboorkoppen.", "Mika-maatje XL. Past je toch."],
    "kaaslaarzen": ["Ja, ze zijn van kaas. Nee, niet opeten.", "Stinkt een beetje. Werkt heel goed.", "Gatenkaas, dus lekker luchtig."],
    "kristalring": ["Er zit een klein roze lichtje in. Het knipoogt.", "Past om elke guhvinger. Guhs hebben geen vingers.", "Gevonden in een Kristalspin. Niet vragen."],
    "spiegelketting": ["Je ziet jezelf erin. Je ziet er goed uit.", "Spiegeltje, spiegeltje aan je nek.", "Kaatst slechte bedoelingen terug."],
    "galmoorbel": ["Je hoort er zacht de bergen in zingen.", "Een echo die nooit stopt. Gezellig.", "Galmt als je knikt. Knik maar veel."],
}

TEKSTEN = {
    "vadsrijk.z2wezens.ijs_gebroken": "IJs gebroken!",
    "vadsrijk.z2wezens.gespiegeld": "Gespiegeld!",
    "vadsrijk.z2wezens.set.kristal": "Kristalset",
    "vadsrijk.z2wezens.set.mika": "Mika-buitset",
    "vadsrijk.z2wezens.set.stukken": "Set: %s (%s/4 gedragen)",
    "vadsrijk.z2wezens.set.kristal.2": "(2) Kristalhuid: +8 Elementweerstand, Bevroren duurt half zo lang en zware klappen doen dan niks extra",
    "vadsrijk.z2wezens.set.kristal.4": "(4) Splinterslag: 20%% kans dat je klap in scherven barst (25%% schade op alles eromheen)",
    "vadsrijk.z2wezens.set.mika.2": "(2) Mijnlamp: +5 Kritiek",
    "vadsrijk.z2wezens.set.mika.4": "(4) Boorkracht: +15%% schade op versteende wezens, +25%% op je eerste klap",
    # (WERELDVERHAAL §10 #13: under every Mika-buitset piece, Z2WezensClient.setRegels)
    "vadsrijk.z2wezens.set.mika.stempel": "Gestempeld: EIGENDOM VAN NOOITGENOEG. Niet aan knabbelen.",
    "vadsrijk.z2wezens.mika.weg.0": "Dit zeg ik tegen de baas!",
    "vadsrijk.z2wezens.mika.weg.1": "Terugtrekken! TERUGTREKKEN!",
    "vadsrijk.z2wezens.mika.weg.2": "Ik kom terug! Met meer kaas!",
    "vadsrijk.z2wezens.mika.paniek.0": "Mamaaa!",
    "vadsrijk.z2wezens.mika.paniek.1": "Pauze! Ik heb pauze!",
    "vadsrijk.z2wezens.mika.paniek.2": "Niet de snor! Eh... ik heb geen snor!",
    "vadsrijk.z2wezens.mika.hulp.0": "Jongens! HIER!",
    "vadsrijk.z2wezens.mika.hulp.1": "Ik heb vrienden meegenomen. Njehehe!",
    "vadsrijk.z2wezens.mika.hulp.2": "Ploegdienst! Iedereen naar boven!",
    "vadsrijk.z2wezens.cmd.gespawnd": "%s gemaakt (level %s%s).",
    "vadsrijk.z2wezens.cmd.onbekend": "Dat wezen ken ik niet: %s",
    "vadsrijk.z2wezens.cmd.set": "Set %s gegeven (level %s).",
    "vadsrijk.z2wezens.cmd.loot": "Loot van %s (level %s): %s items, %s brons.",
    "vadsrijk.z2wezens.cmd.spawners": "%s zone-2-spawners; max levend per gebied: %s",
    "vadsrijk.z2wezens.cmd.opgeruimd": "%s zone-2-wezens opgeruimd.",
    "vadsrijk.z2wezens.cmd.alle": "%s wezens op een rij gezet.",
    "vadsrijk.z2wezens.tip.bevroren.titel": "Bevroren!",
    "vadsrijk.z2wezens.tip.bevroren": "Je bent Bevroren: je loopt trager en een ZWARE klap doet extra pijn (en breekt het ijs). Ontwijk de grote uithalen!",
    "vadsrijk.z2wezens.tip.versteend.titel": "Versteend",
    "vadsrijk.z2wezens.tip.versteend": "Dit wezen is versteend tot grauw kristal. Roze (vads) doet er x1,5 schade op!",
    "vadsrijk.z2wezens.tip.splinterling.titel": "Splinterling",
    "vadsrijk.z2wezens.tip.splinterling": "Elke klap op een Splinterling schiet scherven terug naar wie hem sloeg. Van ver weg slaan helpt niet!",
    "vadsrijk.z2wezens.tip.galmvleermuis.titel": "Galmvleermuis",
    "vadsrijk.z2wezens.tip.galmvleermuis": "Weg? Luister! Hoor je een hoog belletje, dan duikt hij zo achter je op.",
    "vadsrijk.z2wezens.tip.sneeuwknabbelaar.titel": "Sneeuwknabbelaar",
    "vadsrijk.z2wezens.tip.sneeuwknabbelaar": "Ze jagen in groepjes. Zie je er één laag zakken, dan springt hij: stap opzij.",
    "vadsrijk.z2wezens.tip.kristalnaaldkruiper.titel": "Kristalnaaldkruiper",
    "vadsrijk.z2wezens.tip.kristalnaaldkruiper": "Kristalnaalden in de sneeuw? Daar ligt er een op de loer. Trilt de grond: wegwezen!",
    "vadsrijk.z2wezens.tip.ijzelgeest.titel": "IJzelgeest",
    "vadsrijk.z2wezens.tip.ijzelgeest": "Een ijsblauwe cirkel onder je = daar bevriest de grond. Stap eruit voor je Bevroren raakt.",
    "vadsrijk.z2wezens.tip.schotsbijter.titel": "Schotsbijter",
    "vadsrijk.z2wezens.tip.schotsbijter": "Kraakt het ijs onder je? Rennen! Er bijt zo iets door de schots omhoog.",
    "vadsrijk.z2wezens.tip.versteende_wacht.titel": "Versteende Wacht",
    "vadsrijk.z2wezens.tip.versteende_wacht": "Traag maar loeizwaar. Uit de rode cirkel blijven, anders sta je even te duizelen.",
    "vadsrijk.z2wezens.tip.ijspuffer.titel": "IJspuffer",
    "vadsrijk.z2wezens.tip.ijspuffer": "Blaast hij zich op? Dan komt er een vrieswolk. Even afstand houden.",
    "vadsrijk.z2wezens.tip.mika_mijnwerker.titel": "Mika-mijnwerker",
    "vadsrijk.z2wezens.tip.mika_mijnwerker": "Rent hij weg? Dan haalt hij vriendjes. Maak hem af voor hij terug is!",
    "vadsrijk.z2wezens.tip.mika_boorder.titel": "Mika-boorder",
    "vadsrijk.z2wezens.tip.mika_boorder": "De rode streep is waar zijn boorkop heen vliegt. Stap uit de streep.",
    "vadsrijk.z2wezens.tip.kloofgier.titel": "Kloofgier",
    "vadsrijk.z2wezens.tip.kloofgier": "Rode cirkel = hij duikt. Weg uit de cirkel, en sla hem als hij uitgeput op de grond zit.",
    "vadsrijk.z2wezens.tip.kristalspin.titel": "Grauwe Kristalspin",
    "vadsrijk.z2wezens.tip.kristalspin": "Haar web maakt je Plakkerig. Loop niet door de witte plakplekken.",
    "vadsrijk.z2wezens.tip.spiegelwacht.titel": "Spiegelwacht",
    "vadsrijk.z2wezens.tip.spiegelwacht": "Zijn spiegelschild kaatst pijlen en spreuken terug. Val hem van opzij of van achteren aan!",
    "vadsrijk.z2wezens.tip.scherfgeest.titel": "Scherfgeest",
    "vadsrijk.z2wezens.tip.scherfgeest": "Hij flitst om je heen en gooit dan scherven. Sla hem net na zijn flits.",
}


def teksten():
    t = dict(TEKSTEN)
    for k, n in ENTITEITEN.items():
        t[f"entity.guhs.vr_z2_mob_{k}"] = n
    for k, (slot, naam, kleur, accent, s) in HARNAS.items():
        t[f"item.guhs.vr_z2_mob_{k}"] = naam
        t[f"vadsrijk.loot.{k}"] = naam
    for k, (soort, naam, kleur, steen) in SIERADEN.items():
        t[f"item.guhs.vr_z2_mob_{k}"] = naam
        t[f"vadsrijk.loot.{k}"] = naam
    for k, regels in FLAVOUR.items():
        for i, regel in enumerate(regels):
            t[f"vadsrijk.loot.{k}.flavour.{i}"] = "\"" + regel + "\""
    return t


# =====================================================================================================================
# writing
# =====================================================================================================================
def controleer(naam, m, anims):
    bones = {b["name"] for b in m.bones}
    for an, a in anims.items():
        for b in a["bones"]:
            assert b in bones, f"{naam}: anim {an} moves unknown bone {b}"
    for nodig in NODIG[naam]:
        assert nodig in anims, f"{naam}: missing anim {nodig}"


def assets(a, w=vw._schrijf_json):
    geo_dir = os.path.join(a, "geckolib", "models", "entity", *SUB.split("/"))
    anim_dir = os.path.join(a, "geckolib", "animations", "entity", *SUB.split("/"))
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    n = 0
    for i, (naam, maak) in enumerate(WEZENS + CRITTERS):
        m, anims, pal, glow = maak()
        if (naam, maak) in WEZENS:
            anims = leg_anims(m, anims)
        controleer(naam, m, anims)
        w(os.path.join(geo_dir, naam + ".geo.json"), m.geo())
        w(os.path.join(anim_dir, naam + ".animation.json"), anim_bestand(anims))
        tex = schilder(m, pal, 2200 + i)
        vw._png(os.path.join(tex_dir, naam + ".png"), tex)
        vw._png(os.path.join(tex_dir, naam + "_gloed.png"), schilder(m, pal, 2300 + i, glow, glow=True))
        if (naam, maak) in WEZENS:
            vw._png(os.path.join(tex_dir, naam + "_elite.png"), vw.elite_gloed(tex))
        n += 1
    item_tex = os.path.join(a, "textures", "item", *SUB.split("/"))
    for i, (k, (slot, naam, kleur, accent, s)) in enumerate(HARNAS.items()):
        vw._png(os.path.join(item_tex, k + ".png"), vw.icoon(slot, kleur, accent, "klodder" if s == "mika" else "zwam" if s else None, 2400 + i))
    for i, (k, (soort, naam, kleur, steen)) in enumerate(SIERADEN.items()):
        vw._png(os.path.join(item_tex, k + ".png"), sieraad_icoon(soort, kleur, steen, 2450 + i))
    for k in list(HARNAS) + list(SIERADEN):
        item = "vr_z2_mob_" + k
        w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
        w(os.path.join(a, "models", "item", item + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{SUB}/{k}"}})
    return n


def uiterlijken(a, w=vw._schrijf_json):
    """The worn look of every armour piece (KernRegistry.harnasEigenschappen(slot, "vr_z2_mob_<id>")): vanilla iron tinted."""
    import zipfile
    from io import BytesIO
    jar = os.path.join("build", "moddev", "artifacts", "minecraft-patched-26.1.2.112.jar")
    bron = {}
    if os.path.exists(jar):
        with zipfile.ZipFile(jar) as z:
            for laag in ("humanoid", "humanoid_leggings"):
                bron[laag] = Image.open(BytesIO(z.read(f"assets/minecraft/textures/entity/equipment/{laag}/iron.png"))).convert("RGBA")
    for hid, (_slot, _naam, kleur, _k2, _set) in HARNAS.items():
        uid = "vr_z2_mob_" + hid
        w(os.path.join(a, "equipment", uid + ".json"), {"layers": {"humanoid": [{"texture": "guhs:" + uid}], "humanoid_leggings": [{"texture": "guhs:" + uid}]}})
        for laag in ("humanoid", "humanoid_leggings"):
            doel = os.path.join(a, "textures", "entity", "equipment", laag, uid + ".png")
            if laag not in bron:
                if not os.path.exists(doel):
                    print("vadsrijk_z2_wezens: no minecraft jar (run a gradle build first) for", doel)
                continue
            img = bron[laag].copy()
            px = img.load()
            for y in range(img.height):
                for x in range(img.width):
                    r, g, b, al = px[x, y]
                    l = min(1.0, (0.3 * r + 0.59 * g + 0.11 * b) / 255 * 1.15)
                    px[x, y] = (int(kleur[0] * l), int(kleur[1] * l), int(kleur[2] * l), al)
            os.makedirs(os.path.dirname(doel), exist_ok=True)
            img.save(doel)


def build(h):
    assets(h.A, h.w)
    uiterlijken(h.A, h.w)
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    n = assets(a)
    uiterlijken(a)
    t = teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(t)
        vw._schrijf_json(p, data)
    print(f"vadsrijk_z2_wezens: {n} models, {len(HARNAS) + len(SIERADEN)} items, {len(t)} texts")


if __name__ == "__main__":
    _direct()
