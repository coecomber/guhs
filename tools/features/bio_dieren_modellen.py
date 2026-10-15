"""
biomes3 slice "dieren" - the looks: GeckoLib models, animations and textures of

  koi              a plump pond fish in guh style: the big glossy guh eyes and a blush on a round head, two round guh ears
                   as little fins on top, pink lips with two barbels, a fan tail.  Five colours: roodwit (white with red,
                   a red cap), driekleur (white, red and black), roze (guh pink with white), blauw (blue with scales, an
                   orange cheek) and goud.  One geo file, a texture per colour (same allocation order = same UVs).
  wolkenschaapje   a ball of cloud with a guh face: a small bare lamb (body, head, four dangling legs) inside a shell of
                   overlapping fluff puffs (bones "pluis" and "pluis_kop": hidden while it is shorn).  The whole model hangs
                   above the ground: it floats.

and the two biome guhs on the guh's own model and texture (the way the Pinguh is painted, tools/features/guhpolder_tex.py):

  bloesemguh       blossom-white fur with pink petals in it, a five-petal blossom by its ear and loose petals on its back
  tanukiguh        grey-brown with a dark mask round the eyes, a cream muzzle and belly, dark paws and ear rims, a big
                   bushy ringed tail and a green leaf on its head

  BONES            the variant bones (make_guh_variants format).  The guh texture sheet is full (3 swatches left on the
                   release), so these bones add NO swatch: each samples a swatch that already exists for another variant
                   (SWATCH below), and only the texture of OUR variant paints it differently.
  variants(v)      the first textures (make_guh_variants); guh_textures(h) then paints the real ones from guh.png
  build(h) / check(h) / preview(out)
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))   # (run as a script: the tools folder)
from features import bio_lib as lib
from features import waterdiertjes_modellen as wm
from features.waterdiertjes_modellen import Atlas, bone, cube, disc, geo, plain

# --- the koi --------------------------------------------------------------------------------------------------------------
# colour: base, belly, [(blotch colour, how many per big face, r from, r to)], cap (forehead blotch) or None, fin, fin rim,
#         inner ear, extra ("schubben", "glitter" or None)
KOI = {
    "roodwit": ((250, 246, 240), (255, 252, 248), [((234, 84, 48), 3, 0.2, 0.36)], (234, 84, 48), (252, 238, 228), (236, 128, 98),
                (244, 150, 170), None),
    "driekleur": ((250, 246, 240), (255, 252, 248), [((228, 76, 46), 2, 0.2, 0.34), ((44, 38, 48), 3, 0.08, 0.15)], (228, 76, 46),
                  (250, 240, 234), (70, 60, 70), (244, 150, 170), None),
    "roze": ((248, 172, 204), (255, 228, 240), [((255, 246, 250), 3, 0.16, 0.3), ((224, 104, 158), 1, 0.1, 0.18)], (255, 246, 250),
             (255, 214, 232), (226, 110, 160), (226, 110, 160), None),
    "blauw": ((124, 168, 214), (250, 244, 236), [], None, (250, 176, 100), (240, 132, 56), (244, 150, 110), "schubben"),
    "goud": ((250, 196, 66), (255, 234, 150), [((255, 232, 140), 2, 0.14, 0.26)], None, (255, 222, 120), (236, 150, 40),
             (240, 150, 70), "glitter"),
}
KOI_KLEUREN = list(KOI)


def koi_huid(kleur, buik=False, kap=False):
    """The skin of a koi face patch: the base (or the belly), its blotches, scales or glitter, and the cap on its forehead."""
    base, onder, vlekken, kapkleur, _, _, _, extra = KOI[kleur]

    def f(W, H, rng, a):
        groot = min(W, H)
        if not buik:
            for colour, n, r0, r1 in vlekken:
                for _ in range(max(1, int(round(n * W * H / (64.0 * 80.0))))):
                    r = rng.uniform(r0, r1) * groot
                    disc(a, rng.uniform(0, W), rng.uniform(0, H), r, colour, 1.0, ry=r * rng.uniform(0.7, 1.2))
            if extra == "schubben":
                donker = np.array((92, 134, 190), np.float32)
                for y in range(H):
                    for x in range(W):
                        if (x + (y // 6) * 3) % 6 == 0 or y % 6 == 0:
                            a[y, x, :3] = a[y, x, :3] * 0.74 + donker * 0.26
        if extra == "glitter":
            for _ in range(max(1, W * H // 260)):
                disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.8, 1.5), (255, 250, 214), 0.9)
        if kap and kapkleur:
            disc(a, W / 2, H * 0.02, W * 0.34, kapkleur, 1.0, ry=H * 0.26)
        if kap and kleur == "blauw":
            for sx in (-1, 1):                                  # orange cheeks
                disc(a, W / 2 + sx * W * 0.4, H * 0.84, W * 0.2, (244, 150, 70), 0.9, ry=H * 0.16)
    return plain(onder if buik else base, 6, f)


def koi_gezicht(kleur):
    base = KOI[kleur][0]
    huid = koi_huid(kleur, kap=True)

    def paint(W, H, rng):
        a = huid(W, H, rng).astype(np.float32)
        wm.guh_eyes(a, W, H, 0.42, 0.25, 0.185, blush=True)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def vin(colour, rand, staart=False):
    """A fin (a thin plane): see-through, soft rays, a darker rim; the tail fin is a fan with two lobes."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        for y in range(H):
            for x in range(W):
                t = x / max(1, W - 1)                          # (0 at the body, 1 at the tip)
                v = (y + 0.5) / H * 2 - 1                      # (-1 top .. 1 bottom)
                if staart:
                    half = 0.28 + 0.72 * t ** 0.7
                    notch = max(0.0, (t - 0.72) / 0.28) * 0.3   # the notch between the two lobes
                    inside = notch <= abs(v) <= half
                    edge = abs(v) > half - 0.12 or (t > 0.72 and abs(v) < notch + 0.1) or t > 0.93
                else:
                    half = 1.0 - 0.75 * t ** 2
                    inside = abs(v) <= half
                    edge = abs(v) > half - 0.2 or t > 0.9
                if not inside:
                    continue
                c = np.array(rand if edge else colour, np.float32)
                if not edge and x % 5 == 0:
                    c = c * 0.9
                a[y, x, :3] = c + rng.normal(0, 4)
                a[y, x, 3] = 240
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def koi(atlas, kleur):
    base, onder, _, _, vinkleur, vinrand, oor, _ = KOI[kleur]
    huid, buik = koi_huid(kleur), koi_huid(kleur, buik=True)
    donker = plain(tuple(int(c * 0.82) for c in base), 5)
    bones = [bone("root", None, [0, 0, 0]),
             bone("body", "root", [0, 2.2, 0], [cube(atlas, [-1.75, 0.4, -2.2], [3.5, 3.6, 5.2], huid, overrides={"down": buik})]),
             bone("head", "body", [0, 2.2, -2.2], [
                 cube(atlas, [-2, 0.2, -5.5], [4, 4, 3.5], huid, overrides={"north": koi_gezicht(kleur), "down": buik}),
                 cube(atlas, [-0.7, 0.7, -5.85], [1.4, 0.8, 0.35], plain((240, 132, 168), 4))]),          # the round lips
             bone("snor_left", "head", [1.05, 0.6, -5.45], [cube(atlas, [0.9, -0.35, -5.6], [0.3, 0.95, 0.3], donker)], rotation=[0, 0, -18]),
             bone("snor_right", "head", [-1.05, 0.6, -5.45], [cube(atlas, [-1.2, -0.35, -5.6], [0.3, 0.95, 0.3], donker)], rotation=[0, 0, 18])]
    bones += wm.ears(atlas, base, oor, 0.85, 4.1, -4.2, 1.4, 1.4)
    bones.append(bone("rugvin", "body", [0, 4.0, 0.3], [
        cube(atlas, [0, 4.0, -1.6], [0, 1.7, 3.8], vin(vinkleur, vinrand), only=("east", "west"))]))
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"vin_{side}", "body", [sx * 1.75, 1.3, -0.9], [
            cube(atlas, [1.75 if sx > 0 else -3.75, 1.3, -1.8], [2, 0, 1.8], vin(vinkleur, vinrand), only=("up", "down"))],
            rotation=[0, sx * -20, sx * -28]))
    bones.append(bone("staart", "body", [0, 2.3, 3.0], [cube(atlas, [-1.1, 1.0, 3.0], [2.2, 2.6, 2.0], huid, overrides={"down": buik})]))
    bones.append(bone("staartvin", "staart", [0, 2.3, 4.8], [
        cube(atlas, [0, -0.3, 4.6], [0, 5.2, 3.8], vin(vinkleur, vinrand, staart=True), only=("east", "west"))]))
    return geo("koi", atlas, bones, 1.2, 0.8)


def koi_anims():
    zwem = {"body": {"rotation": {"0.0": [0, 6, 0], "0.6": [0, -6, 0], "1.2": [0, 6, 0]}},
            "head": {"rotation": {"0.0": [0, -5, 0], "0.6": [0, 5, 0], "1.2": [0, -5, 0]}},
            "staart": {"rotation": {"0.0": [0, -18, 0], "0.6": [0, 18, 0], "1.2": [0, -18, 0]}},
            "staartvin": {"rotation": {"0.0": [0, -22, 0], "0.3": [0, 0, 0], "0.6": [0, 22, 0], "0.9": [0, 0, 0], "1.2": [0, -22, 0]}},
            "vin_left": {"rotation": {"0.0": [0, 0, 0], "0.6": [0, 0, -22], "1.2": [0, 0, 0]}},
            "vin_right": {"rotation": {"0.0": [0, 0, 22], "0.6": [0, 0, 0], "1.2": [0, 0, 22]}}}
    drijf = {"body": {"position": {"0.0": [0, 0, 0], "1.5": [0, 0.3, 0], "3.0": [0, 0, 0]}},
             "staart": {"rotation": {"0.0": [0, -8, 0], "1.5": [0, 8, 0], "3.0": [0, -8, 0]}},
             "staartvin": {"rotation": {"0.0": [0, -10, 0], "1.5": [0, 10, 0], "3.0": [0, -10, 0]}},
             "vin_left": {"rotation": {"0.0": [0, 0, 0], "0.75": [0, 0, -16], "1.5": [0, 0, 0], "2.25": [0, 0, -16], "3.0": [0, 0, 0]}},
             "vin_right": {"rotation": {"0.0": [0, 0, 0], "0.75": [0, 0, 16], "1.5": [0, 0, 0], "2.25": [0, 0, 16], "3.0": [0, 0, 0]}},
             "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.2": [0, 0, 0], "2.35": [0, 0, -18], "2.5": [0, 0, 0]}},
             "ear_right": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 0], "1.15": [0, 0, 18], "1.3": [0, 0, 0]}}}
    spartel = {"root": {"rotation": {"0.0": [0, 0, 90]}, "position": {"0.0": [0, 1.6, 0]}},
               "staart": {"rotation": {"0.0": [0, -30, 0], "0.15": [0, 30, 0], "0.3": [0, -30, 0]}},
               "head": {"rotation": {"0.0": [0, 10, 0], "0.15": [0, -10, 0], "0.3": [0, 10, 0]}}}
    hap = {"head": {"rotation": {"0.0": [0, 0, 0], "0.12": [-28, 0, 0], "0.3": [-10, 0, 0], "0.45": [-28, 0, 0], "0.7": [0, 0, 0]},
                    "scale": {"0.0": [1, 1, 1], "0.12": [1.06, 0.95, 1.06], "0.3": [1, 1, 1], "0.45": [1.06, 0.95, 1.06], "0.7": [1, 1, 1]}},
           "ear_left": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, -22], "0.4": [0, 0, 0], "0.55": [0, 0, -22], "0.7": [0, 0, 0]}},
           "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 22], "0.4": [0, 0, 0], "0.55": [0, 0, 22], "0.7": [0, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "zwem": {"loop": True, "animation_length": 1.2, "bones": zwem},
        "drijf": {"loop": True, "animation_length": 3.0, "bones": drijf},
        "spartel": {"loop": True, "animation_length": 0.3, "bones": spartel},
        "hap": {"loop": False, "animation_length": 0.7, "bones": hap}}}


# --- the wolkenschaapje -----------------------------------------------------------------------------------------------------
LAM = (255, 232, 228)          # the bare lamb under the fluff
LAM_OOR = (246, 160, 190)
HOEF = (236, 168, 192)
PLUIS = (250, 252, 255)
PLUIS_SCHADUW = (222, 236, 252)
PLUIS_ROZE = (255, 226, 240)


def wolk(hoek=0.26, onder=False):
    """A fluff face: cloud white with soft sky-blue shadows and a pink glow, the corners rounded off (see-through), so the
    overlapping puffs make a lumpy cloud and not a box."""
    def paint(W, H, rng):
        a = plain(PLUIS, 4)(W, H, rng).astype(np.float32)
        k = min(W, H)
        for _ in range(max(2, W * H // 700)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.14, 0.3) * k, PLUIS_SCHADUW, 0.5)
        for _ in range(max(1, W * H // 1500)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.12, 0.24) * k, PLUIS_ROZE, 0.35)
        for _ in range(max(2, W * H // 800)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.1, 0.2) * k, (255, 255, 255), 0.85)
        if onder:
            a[..., :3] = a[..., :3] * 0.8 + np.array(PLUIS_SCHADUW, np.float32) * 0.2
        else:                                                    # a soft shadow towards the lower edge
            ramp = np.clip((np.arange(H) / max(1, H - 1) - 0.55) / 0.45, 0, 1)[:, None, None]
            a[..., :3] = a[..., :3] * (1 - 0.22 * ramp) + np.array(PLUIS_SCHADUW, np.float32) * 0.22 * ramp
        r = hoek * k
        for y in range(H):
            for x in range(W):
                dx = max(0.0, r - (x + 0.5), (x + 0.5) - (W - r))
                dy = max(0.0, r - (y + 0.5), (y + 0.5) - (H - r))
                if dx * dx + dy * dy > r * r:
                    a[y, x, 3] = 0
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def schaapje(atlas):
    vel, pluis, pluis_onder = plain(LAM, 5), wolk(), wolk(onder=True)
    gezicht = wm.guh_face(LAM, eye_y=0.42, eye_dx=0.25, eye_r=0.175, snoet=True)

    def puf(origin, size, hoek=0.26):
        return cube(atlas, origin, size, wolk(hoek), overrides={"down": pluis_onder})
    bones = [bone("root", None, [0, 0, 0]),
             bone("body", "root", [0, 10, 0], [cube(atlas, [-3, 7, -4.5], [6, 6, 9], vel)]),
             # the fluff: one big puff and smaller ones poking out of it on every side
             bone("pluis", "body", [0, 11, 0], [
                 puf([-5, 6, -5.5], [10, 9, 11]),
                 puf([-3.5, 14, -4], [7, 2.5, 5], 0.3), puf([-2.5, 14.5, 0.5], [5, 1.6, 4.5], 0.3),
                 puf([4.4, 7.5, -3.5], [2, 6, 6], 0.3), puf([-6.4, 7.5, -3.5], [2, 6, 6], 0.3),
                 puf([4.2, 8.5, 1.5], [1.6, 4, 3.6], 0.3), puf([-5.8, 8.5, 1.5], [1.6, 4, 3.6], 0.3),
                 puf([-3.5, 7.5, 5], [7, 6, 2], 0.3), puf([-3.5, 5, -3.5], [7, 1.4, 7], 0.3),
                 puf([-1.6, 10.5, 6.6], [3.2, 3.2, 2.2], 0.34)]),                                       # the tail puff
             bone("head", "body", [0, 10.5, -5], [
                 cube(atlas, [-3.5, 7, -10.5], [7, 7, 5.5], vel, overrides={"north": gezicht})]),
             bone("pluis_kop", "head", [0, 14, -8], [
                 puf([-3.2, 13.4, -10], [6.4, 2.2, 5.4], 0.3), puf([-1.8, 15.2, -9], [3.6, 1.5, 3.2], 0.34),
                 puf([-4.1, 9.5, -6.6], [8.2, 4.6, 2.0], 0.3)])]                                          # a ruff behind the face
    bones += wm.ears(atlas, LAM, LAM_OOR, 3.1, 11.3, -8.4, 2.6, 2.6)
    for name, x, z in (("leg_fl", 1.1, -3.7), ("leg_fr", -2.9, -3.7), ("leg_bl", 1.1, 1.9), ("leg_br", -2.9, 1.9)):
        bones.append(bone(name, "body", [x + 0.9, 7.6, z + 0.9], [
            cube(atlas, [x, 4.4, z], [1.8, 3.6, 1.8], vel),
            cube(atlas, [x - 0.1, 3.6, z - 0.1], [2, 0.9, 2], plain(HOEF, 4))]))
    return geo("wolkenschaapje", atlas, bones, 1.4, 1.5)


def schaapje_anims():
    def poten(graden, lengte, fase=(0, 0.5, 0.5, 0)):
        out = {}
        for name, f in zip(("leg_fl", "leg_fr", "leg_bl", "leg_br"), fase):
            t0, t1 = round(lengte * f, 2), round(lengte * ((f + 0.5) % 1.0), 2)
            keys = {"0.0": [graden if f == 0 else -graden, 0, 0], str(round(lengte / 2, 2)): [-graden if f == 0 else graden, 0, 0],
                    str(lengte): [graden if f == 0 else -graden, 0, 0]}
            out[name] = {"rotation": keys}
        return out
    zweef = {"body": {"position": {"0.0": [0, 0, 0], "2.0": [0, 1.3, 0], "4.0": [0, 0, 0]},
                      "rotation": {"0.0": [0, 0, -1.5], "2.0": [0, 0, 1.5], "4.0": [0, 0, -1.5]}},
             "pluis": {"scale": {"0.0": [1, 1, 1], "2.0": [1.03, 1.04, 1.03], "4.0": [1, 1, 1]}},
             "head": {"rotation": {"0.0": [0, 0, 0], "1.3": [0, 8, 0], "2.7": [0, -8, 0], "4.0": [0, 0, 0]}},
             "ear_left": {"rotation": {"0.0": [0, 0, 0], "3.0": [0, 0, 0], "3.15": [0, 0, -18], "3.3": [0, 0, 0]}},
             "ear_right": {"rotation": {"0.0": [0, 0, 0], "1.4": [0, 0, 0], "1.55": [0, 0, 18], "1.7": [0, 0, 0]}}}
    zweef.update(poten(7, 4.0))
    drijf = {"body": {"position": {"0.0": [0, 0, 0], "0.8": [0, 1.0, 0], "1.6": [0, 0, 0]},
                      "rotation": {"0.0": [3, 0, -2.5], "0.8": [3, 0, 2.5], "1.6": [3, 0, -2.5]}},
             "pluis": {"scale": {"0.0": [1, 1, 1], "0.8": [1.02, 1.03, 1.02], "1.6": [1, 1, 1]}}}
    drijf.update(poten(22, 1.6))
    blij = {"body": {"position": {"0.0": [0, 0, 0], "0.2": [0, 3, 0], "0.4": [0, 0.5, 0], "0.6": [0, 2.4, 0], "0.9": [0, 0, 0]},
                     "rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 8], "0.45": [0, 0, -8], "0.7": [0, 0, 5], "0.9": [0, 0, 0]}},
            "pluis": {"scale": {"0.0": [1, 1, 1], "0.2": [1.08, 1.08, 1.08], "0.9": [1, 1, 1]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, -24], "0.45": [0, 0, 0], "0.65": [0, 0, -24], "0.9": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 24], "0.45": [0, 0, 0], "0.65": [0, 0, 24], "0.9": [0, 0, 0]}}}
    pluis = {"body": {"rotation": {"0.0": [0, 0, 0], "0.08": [0, 0, 7], "0.16": [0, 0, -7], "0.24": [0, 0, 6], "0.32": [0, 0, -6],
                                   "0.4": [0, 0, 4], "0.5": [0, 0, 0]}},
             "head": {"rotation": {"0.0": [0, 0, 0], "0.15": [-12, 0, 0], "0.5": [0, 0, 0]}}}
    geluid = {"head": {"rotation": {"0.0": [0, 0, 0], "0.15": [-22, 0, 0], "0.5": [-18, 0, 0], "0.7": [0, 0, 0]}},
              "pluis": {"scale": {"0.0": [1, 1, 1], "0.15": [1.04, 1.05, 1.04], "0.7": [1, 1, 1]}}}
    return {"format_version": "1.8.0", "animations": {
        "zweef": {"loop": True, "animation_length": 4.0, "bones": zweef},
        "drijf": {"loop": True, "animation_length": 1.6, "bones": drijf},
        "blij": {"loop": False, "animation_length": 0.9, "bones": blij},
        "pluis": {"loop": False, "animation_length": 0.5, "bones": pluis},
        "geluid": {"loop": False, "animation_length": 0.7, "bones": geluid}}}


# =====================================================================================================================
# the biome guhs: bones on the guh model, textures on the guh sheet
# =====================================================================================================================
_H = [0, 6, -2]            # the guh's head pivot
_STAART = [0, 1.5, 12]     # its tail pivot
# The guh sheet has no room for new swatches, so our bones sample swatches of other variants (a variant only shows its own
# bones and has its own texture, so nobody sees the difference):  our name -> the swatch on the sheet
SWATCH = {"bloesem_blad": "koning_manen", "bloesem_hart": "koning_goud",
          "tanuki_vacht": "koning_manen", "tanuki_donker": "koning_goud", "tanuki_blad": "neck"}


def _bloem(cx, cz, y, r=1.45, blad=1.5):
    """Five petals round (cx, cz), lying flat at height y."""
    return [([round(cx + r * math.cos(math.radians(a)) - blad / 2, 2), y, round(cz + r * math.sin(math.radians(a)) - blad / 2, 2)],
             [blad, 0.5, blad], 0) for a in range(-90, 270, 72)]


def _staart():
    """The Tanukiguh's tail: thick segments stepping back and up, tan and dark in turn, a dark tip."""
    tan, donker = [], []
    z, y = 11.2, 1.3
    for i, (lengte, dikte) in enumerate(((2.2, 4.0), (1.5, 4.8), (2.2, 5.4), (1.5, 5.6), (2.0, 5.2), (1.6, 4.0))):
        seg = ([round(-dikte / 2, 2), round(y + (5.6 - dikte) / 2, 2), round(z, 2)], [dikte, dikte, lengte], 0)
        (tan if i % 2 == 0 else donker).append(seg)
        z += lengte
        y += 0.55
    return tan, donker


_TAN, _DONKER = _staart()
BONES = {
    # the Bloesemguh: a blossom by its left ear, loose petals on its head and back
    "bloesem_bloem": ("head", _H, SWATCH["bloesem_blad"], _bloem(3.0, -7.6, 15.0)),
    "bloesem_hart": ("head", _H, SWATCH["bloesem_hart"], [([2.35, 15.2, -8.25], [1.3, 0.7, 1.3], 0)]),
    "bloesem_blaadjes_kop": ("head", _H, SWATCH["bloesem_blad"], [([-3.6, 15.0, -5.2], [1.3, 0.3, 1.1], 0), ([-1.2, 15.0, -9.3], [1.1, 0.3, 1.3], 0)]),
    "bloesem_blaadjes": ("body", [0, 6, 6], SWATCH["bloesem_blad"], [
        ([-3.2, 11.0, 2.0], [1.4, 0.3, 1.2], 0), ([1.6, 11.0, 4.6], [1.2, 0.3, 1.4], 0), ([-1.0, 11.0, 7.6], [1.4, 0.3, 1.2], 0),
        ([2.6, 11.0, 1.2], [1.1, 0.3, 1.1], 0), ([4.9, 9.5, 5.8], [1.2, 0.3, 1.3], 0), ([-6.1, 9.5, 3.4], [1.3, 0.3, 1.1], 0)]),
    # the Tanukiguh: the ringed tail and the leaf on its head
    "tanuki_staart": ("tail", _STAART, SWATCH["tanuki_vacht"], _TAN),
    "tanuki_ringen": ("tail", _STAART, SWATCH["tanuki_donker"], _DONKER),
    "tanuki_blad": ("head", _H, SWATCH["tanuki_blad"], [
        ([-2.0, 15.0, -8.4], [4.0, 0.5, 4.2], 0), ([-1.2, 15.0, -9.6], [2.4, 0.5, 1.2], 0), ([-0.5, 15.0, -10.3], [1.0, 0.5, 0.7], 0),
        ([-1.3, 15.0, -4.2], [2.6, 0.5, 0.9], 0), ([-0.3, 15.1, -3.4], [0.6, 0.4, 1.7], 0)]),
}
BLOESEM_VACHT = (255, 232, 240)
TANUKI_VACHT = (156, 128, 104)
TANUKI_DONKER = (60, 44, 42)
TANUKI_CREME = (232, 212, 184)


def _petal_swatch(v, rng):
    a = v.fabric((250, 170, 200), rng, 10)
    px = a.shape[0]
    yy, xx = np.mgrid[0:px, 0:px]
    d = np.hypot(xx - px / 2, yy - px / 2) / (px / 2)
    a = a * (1 - 0.35 * np.clip(1 - d, 0, 1)[..., None]) + np.array((255, 236, 244)) * 0.35 * np.clip(1 - d, 0, 1)[..., None]
    a[d > 1.15] = a[d > 1.15] * 0.9 + np.array((232, 120, 170)) * 0.1
    return np.clip(a, 0, 255)


def _hart_swatch(v, rng):
    a = v.fabric((252, 214, 90), rng, 10)
    px = a.shape[0]
    for _ in range(14):
        x, y = rng.integers(1, px - 2, 2)
        a[y:y + 2, x:x + 2] = (240, 150, 50)
    return np.clip(a, 0, 255)


def _vacht_swatch(v, rng, colour, licht=18):
    a = v.fabric(colour, rng, 16)
    px = a.shape[0]
    for _ in range(40):                                         # fluffy strands
        x, y = rng.integers(0, px - 1), rng.integers(0, px - 3)
        a[y:y + 3, x] = np.clip(a[y:y + 3, x] + licht, 0, 255)
    return np.clip(a, 0, 255)


def _blad_swatch(v, rng):
    a = v.fabric((98, 172, 86), rng, 10)
    px = a.shape[0]
    a[:, px // 2 - 1:px // 2 + 1] = (176, 222, 140)              # the midrib
    for y in range(3, px, 6):                                   # the side veins
        for x in range(px):
            dy = y - abs(x - px / 2) * 0.35
            if 0 <= int(dy) < px:
                a[int(dy), x] = (150, 206, 120)
    a[:2, :] = a[-2:, :] = (70, 138, 66)
    a[:, :2] = a[:, -2:] = (70, 138, 66)
    return np.clip(a, 0, 255)


def variants(v):
    """The first textures (make_guh_variants.py): the fur colour and our bones' swatches. guh_textures(h) paints the rest."""
    r = lib.rng("bio_dieren_variants")
    return {
        "bloesemguh": (BLOESEM_VACHT, {SWATCH["bloesem_blad"]: lambda: _petal_swatch(v, r), SWATCH["bloesem_hart"]: lambda: _hart_swatch(v, r)}),
        "tanukiguh": (TANUKI_VACHT, {SWATCH["tanuki_vacht"]: lambda: _vacht_swatch(v, r, (176, 148, 118)),
                                     SWATCH["tanuki_donker"]: lambda: _vacht_swatch(v, r, TANUKI_DONKER, 10),
                                     SWATCH["tanuki_blad"]: lambda: _blad_swatch(v, r)}),
    }


def _guh_geo(h):
    return json.load(open(os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]


def _swatch_rect(g, bone_name):
    """The texture rectangle (pixels) of the swatch a variant bone samples, or None while the bone is not in the model."""
    from features import guhpolder_tex as gt
    cubes = gt._faces(g, bone_name)
    return cubes[0]["north"] if cubes else None


def _ellips(p, r, cx, cy, rx, ry, colour):
    """An ellipse on the fur inside a face rectangle (fractions of its size), cut off at the rectangle's edges."""
    x0, y0, x1, y1 = r
    w, hh = x1 - x0, y1 - y0
    yy, xx = np.mgrid[0:p.fur.shape[0], 0:p.fur.shape[1]]
    mask = ((xx - (x0 + cx * w)) / max(1, rx * w)) ** 2 + ((yy - (y0 + cy * hh)) / max(1, ry * hh)) ** 2 <= 1
    mask &= (xx >= x0) & (xx < x1) & (yy >= y0) & (yy < y1)
    p.recolour(mask, colour)


def _plak(a, rect, swatch):
    x0, y0, x1, y1 = rect
    s = np.asarray(Image.fromarray(np.clip(swatch, 0, 255).astype(np.uint8)).resize((x1 - x0, y1 - y0), Image.NEAREST)).astype(np.int32)
    a[y0:y1, x0:x1, :3] = s[..., :3]
    a[y0:y1, x0:x1, 3] = 255


def bloesemguh(base, g, v):
    """Blossom-white fur with petals in it: little two-lobed pink and white petals all over the fur (never over the eyes:
    only fur pixels take paint), pinker ears and a deeper blush."""
    from features import guhpolder_tex as gt
    p = gt.Painter(base, BLOESEM_VACHT, fluff=0.03)
    r = lib.rng("bio_dieren_bloesemguh")
    hgt, wid = p.fur.shape
    yy, xx = np.mgrid[0:hgt, 0:wid]
    kleuren = [(248, 160, 194), (240, 128, 172), (252, 190, 212), (255, 252, 254)]
    geplaatst = 0
    for _ in range(4000):
        if geplaatst >= 420:
            break
        y, x = int(r.integers(4, hgt - 4)), int(r.integers(4, wid - 4))
        if not p.fur[y, x]:
            continue
        hoek, lengte = r.uniform(0, math.pi), r.uniform(3.2, 5.4)
        c = kleuren[int(r.integers(0, len(kleuren)))]
        mask = np.zeros(p.fur.shape, bool)
        for s in (-0.45, 0.45):                                 # two lobes side by side: a petal
            cx, cy = x + math.cos(hoek + math.pi / 2) * lengte * s, y + math.sin(hoek + math.pi / 2) * lengte * s
            u, w = (xx - cx) * math.cos(hoek) + (yy - cy) * math.sin(hoek), -(xx - cx) * math.sin(hoek) + (yy - cy) * math.cos(hoek)
            mask |= (u / lengte) ** 2 + (w / (lengte * 0.62)) ** 2 <= 1
        p.recolour(mask, c)
        geplaatst += 1
    for ear in ("ear_left", "ear_right"):                      # blossom-pink ears
        for c in gt._faces(g, ear):
            for k, f in c.items():
                if not k.startswith("_"):
                    p.rect(f, (250, 182, 208))
    a = p.a
    r2 = lib.rng("bio_dieren_bloesemguh_swatch")
    for bone_name, swatch in (("bloesem_bloem", _petal_swatch(v, r2)), ("bloesem_hart", _hart_swatch(v, r2))):
        rect = _swatch_rect(g, bone_name)
        if rect:
            _plak(a, rect, swatch)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def tanukiguh(base, g, v):
    """A tanuki: grey-brown, a cream belly and muzzle, the dark mask round the eyes with a light blaze between them, dark
    paws and ear rims; its tail and leaf bones get their swatches."""
    from features import guhpolder_tex as gt
    p = gt.Painter(base, TANUKI_VACHT, fluff=0.05)
    for c in gt._faces(g, "body"):
        p.rect(c["down"], TANUKI_CREME)
        p.rect(c["north"], TANUKI_CREME, rows=(0.35, 1.0))
        for side in ("east", "west"):
            p.rect(c[side], TANUKI_CREME, rows=(0.62, 1.0), fluff=0.04)
    head = gt._faces(g, "head")
    front = min((c for c in head if c["_size"][0] >= 10), key=lambda c: c["_origin"][2])    # the frontmost big cube: the eyes
    for c in head:
        if c["_size"][0] <= 4:
            continue                                                # (the snoet keeps its pink)
        p.rect(c["down"], TANUKI_CREME)
        if c is not front:
            p.rect(c["north"], TANUKI_DONKER, rows=(0.1, 0.72))      # the strips beside the face: the mask goes on
            p.rect(c["north"], TANUKI_CREME, rows=(0.72, 1.0))
    f = front["north"]
    for cx in (0.2, 0.8):                                          # the mask: a dark patch round each eye, down to the cheek
        _ellips(p, f, cx, 0.4, 0.36, 0.46, TANUKI_DONKER)
    _ellips(p, f, 0.5, 0.2, 0.075, 0.42, (214, 190, 160))            # the light blaze between the eyes
    _ellips(p, f, 0.5, 0.9, 0.3, 0.2, TANUKI_CREME)                  # the cream muzzle
    for cx in (0.06, 0.94):
        _ellips(p, f, cx, 0.9, 0.1, 0.12, TANUKI_CREME)              # cream cheek tufts under the mask
    _ellips(p, f, 0.5, 0.0, 0.5, 0.07, TANUKI_VACHT)                 # (the brow stays fur)
    for ear in ("ear_left", "ear_right"):
        for c in gt._faces(g, ear):
            for k, fr in c.items():
                if not k.startswith("_"):
                    p.rect(fr, TANUKI_DONKER)
    for leg in ("leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"):
        for c in gt._faces(g, leg):
            for k, fr in c.items():
                if not k.startswith("_"):
                    p.rect(fr, TANUKI_DONKER, anything=True)
    for c in gt._faces(g, "tail"):                                 # (the thin tail inside the bushy one)
        for k, fr in c.items():
            if not k.startswith("_"):
                p.rect(fr, TANUKI_DONKER, anything=True)
    a = p.a
    r2 = lib.rng("bio_dieren_tanukiguh_swatch")
    for bone_name, swatch in (("tanuki_staart", _vacht_swatch(v, r2, (176, 148, 118))), ("tanuki_ringen", _vacht_swatch(v, r2, TANUKI_DONKER, 10)),
                              ("tanuki_blad", _blad_swatch(v, r2))):
        rect = _swatch_rect(g, bone_name)
        if rect:
            _plak(a, rect, swatch)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def slaap(img, base, lash, vul=None):
    """The sleeping texture (closed eyes) of one of our guhs, like tools/make_sleep_eyes.py makes for the variants in its
    list: the eye boxes come from the plain guh (the model is the same), the eyes are painted over (with fur from the top
    of the head, or with the colour `vul`: the Tanukiguh's mask) and closed lashes are drawn on."""
    import make_sleep_eyes as mse
    boxes = mse.eye_boxes(base, mse.fur_colour(base))
    assert len(boxes) == 2, boxes
    out = img.copy()
    r = lib.rng("bio_dieren_slaap")
    for x0, y0, x1, y1 in boxes:
        for x in range(x0 - 1, x1 + 2):
            for y in range(y0 - 1, y1 + 2):
                if mse.FACE[0] <= x < mse.FACE[2] and mse.FACE[1] <= y < mse.FACE[3]:
                    if vul:
                        n = int(r.integers(-5, 6))
                        out.putpixel((x, y), (vul[0] + n, vul[1] + n, vul[2] + n, 255))
                    else:
                        out.putpixel((x, y), out.getpixel((mse.TOP[0] + x - mse.FACE[0], mse.TOP[1] + y - mse.FACE[1])))
    for box in boxes:
        mse.closed_eye(out, box, lash)
    return out


def guh_textures(h):
    """guh_bloesemguh.png and guh_tanukiguh.png (and their sleeping ones) from guh.png. Returns False while our bones are
    not in guh.geo.json yet (run tools/make_guh_variants.py first)."""
    import make_guh_variants as v
    g = _guh_geo(h)
    names = {b["name"] for b in g["bones"]}
    klaar = all(b in names for b in BONES)
    if not klaar:
        print("bio_dieren: run tools/make_guh_variants.py first (the Bloesemguh's and Tanukiguh's bones are not in guh.geo.json yet)")
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    bl, ta = bloesemguh(base, g, v), tanukiguh(base, g, v)
    h.save(bl, "entity", "guh_bloesemguh.png")
    h.save(ta, "entity", "guh_tanukiguh.png")
    h.save(slaap(bl, base, (150, 70, 110, 255)), "entity", "guh_slaap", "guh_bloesemguh.png")
    h.save(slaap(ta, base, (236, 216, 190, 255), vul=TANUKI_DONKER), "entity", "guh_slaap", "guh_tanukiguh.png")
    return klaar


# =====================================================================================================================
# build, check, preview
# =====================================================================================================================
MODELS = ["koi", "wolkenschaapje"]
TEXTURES = [f"koi_{k}" for k in KOI] + ["wolkenschaapje", "guh_bloesemguh", "guh_tanukiguh", "guh_slaap/guh_bloesemguh", "guh_slaap/guh_tanukiguh"]
ANIMS = {"koi": ["zwem", "drijf", "spartel", "hap"], "wolkenschaapje": ["zweef", "drijf", "blij", "pluis", "geluid"]}


def build(h):
    g = None
    for kleur in KOI:
        atlas = Atlas(32, 16, 84010101)
        g = koi(atlas, kleur)
        h.save(atlas.image(), "entity", f"koi_{kleur}.png")
    wm._write(h.A, "geckolib/models/entity/koi.geo.json", g)
    wm._write(h.A, "geckolib/animations/entity/koi.animation.json", koi_anims())
    atlas = Atlas(64, 8, 84010102)
    wm._write(h.A, "geckolib/models/entity/wolkenschaapje.geo.json", schaapje(atlas))
    h.save(atlas.image(), "entity", "wolkenschaapje.png")
    wm._write(h.A, "geckolib/animations/entity/wolkenschaapje.animation.json", schaapje_anims())
    return guh_textures(h)


def check(h):
    A = h.A
    problems = []
    for name in MODELS:
        g = json.load(open(os.path.join(A, "geckolib", "models", "entity", f"{name}.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
        names = [b["name"] for b in g["bones"]]
        if len(set(names)) != len(names):
            problems.append(f"{name}: duplicate bone names")
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: {b['name']} hangs on a missing {b['parent']}")
            for c in b.get("cubes", []):
                for f, uv in c["uv"].items():
                    (u, v), (w, hh) = uv["uv"], uv["uv_size"]
                    if u + w > g["description"]["texture_width"] + 1e-6 or v + hh > g["description"]["texture_height"] + 1e-6:
                        problems.append(f"{name}: a uv outside the atlas ({b['name']} {f})")
        anims = json.load(open(os.path.join(A, "geckolib", "animations", "entity", f"{name}.animation.json"), encoding="utf-8"))["animations"]
        for an in ANIMS[name]:
            if an not in anims:
                problems.append(f"{name}: no animation {an}")
        for an, body in anims.items():
            for bn in body["bones"]:
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
    # the wolkenschaapje floats: nothing of it touches the ground; and its fluff is its own bones (shorn: hidden)
    g = json.load(open(os.path.join(A, "geckolib", "models", "entity", "wolkenschaapje.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
    laagste = min(c["origin"][1] for b in g["bones"] for c in b.get("cubes", []))
    if laagste < 3:
        problems.append(f"wolkenschaapje: it should float (lowest cube at {laagste})")
    if not {"pluis", "pluis_kop"} <= {b["name"] for b in g["bones"]}:
        problems.append("wolkenschaapje: no fluff bones (pluis, pluis_kop)")
    for t in TEXTURES:
        if not os.path.exists(os.path.join(h.TEX, "entity", *f"{t}.png".split("/"))):
            problems.append(f"missing texture entity/{t}.png")
    # the guh look: glossy guh eyes (blue ring) on every koi and on the wolkenschaapje
    for t in [f"koi_{k}" for k in KOI] + ["wolkenschaapje"]:
        p = os.path.join(h.TEX, "entity", f"{t}.png")
        if os.path.exists(p):
            a = np.asarray(Image.open(p).convert("RGBA")).astype(np.int32)
            blue = int(((a[..., 2] > a[..., 0] + 60) & (a[..., 2] > a[..., 1] + 10) & (a[..., 3] > 0) & (a[..., 0] < 120)).sum())
            if blue < 30:
                problems.append(f"{t}: no guh eyes ({blue})")
    # the variant bones only use swatches that exist, and add none
    import make_guh_variants as v
    bekend = {b[2] for b in v.BONES.values()}
    for name, (_, _, swatch, _) in BONES.items():
        if swatch not in bekend:
            problems.append(f"{name}: swatch {swatch} is not a base swatch of make_guh_variants (our bones may not add one)")
        if not name.startswith(("bloesem", "tanuki")):
            problems.append(f"{name}: a variant bone must start with bloesem or tanuki (GuhVariant.VARIANT_BONES)")
    return problems


HIDE = ("saddle", "armor_iron", "armor_iron_body", "armor_diamond", "armor_diamond_body", "armor_netherite", "armor_netherite_body")


def preview(out):
    """Renders every model/texture of this slice to out/ with wiki_renders (for looking at them, not part of the build)."""
    here = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(0, os.path.dirname(here))
    import wiki_renders as wr
    A = os.path.join("src", "main", "resources", "assets", "guhs")
    os.makedirs(out, exist_ok=True)
    bg = (120, 170, 210, 255)
    views = ((20, -15), (70, -20), (150, -30), (0, -80))

    def tile(quads, name, size=320):
        t = Image.new("RGBA", (size * len(views), size), bg)
        for i, (yaw, pitch) in enumerate(views):
            t.alpha_composite(wr.render(quads, yaw, pitch, size, margin=0.07), (i * size, 0))
        t.save(os.path.join(out, f"{name}.png"))
        return t
    geo_path = lambda n: os.path.join(A, "geckolib", "models", "entity", f"{n}.geo.json")   # noqa: E731
    tiles = []
    for k in KOI:
        tiles.append(tile(wr.geo_quads(geo_path("koi"), f"guhs:entity/koi_{k}"), f"koi_{k}"))
    # (wiki_renders hides every bone that starts with a guh variant prefix, "pluis" among them: ask for ours)
    tiles.append(tile(wr.geo_quads(geo_path("wolkenschaapje"), "guhs:entity/wolkenschaapje", show_only_variant_bones=("pluis",)), "wolkenschaapje"))
    tiles.append(tile(wr.geo_quads(geo_path("wolkenschaapje"), "guhs:entity/wolkenschaapje"), "wolkenschaapje_geschoren"))
    guh = json.load(open(geo_path("guh"), encoding="utf-8"))["minecraft:geometry"][0]
    for variant, prefix in (("bloesemguh", "bloesem"), ("tanukiguh", "tanuki")):
        weg = HIDE + tuple(b["name"] for b in guh["bones"] if b["name"].startswith(("bloesem", "tanuki")) and not b["name"].startswith(prefix))
        tiles.append(tile(wr.geo_quads(geo_path("guh"), f"guhs:entity/guh_{variant}", hide=weg), variant))
        tiles.append(tile(wr.geo_quads(geo_path("guh"), f"guhs:entity/guh_slaap/guh_{variant}", hide=weg), f"{variant}_slaapt"))
    tiles.append(tile(wr.geo_quads(geo_path("guh"), "guhs:entity/guh_wolk", hide=HIDE + tuple(
        b["name"] for b in guh["bones"] if b["name"].startswith(("bloesem", "tanuki"))), show_only_variant_bones=("wolk",)), "wolkguh"))
    tiles.append(tile(wr.geo_quads(geo_path("kikkerguh"), "guhs:entity/kikkerguh_mint"), "kikkerguh"))
    sheet = Image.new("RGBA", (tiles[0].width, tiles[0].height * len(tiles)), bg)
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, (0, i * t.height))
    sheet.save(os.path.join(out, "_alle.png"))
    print("preview:", out)


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
