"""
bbq2 (sausdieren) - the looks of the two creatures: GeckoLib models, animations and textures.

De Sausloper: a round guh (the big glossy guh eyes, blush, round pink-lined ears) in warm sauce orange on two very long,
thin legs with flat spoon feet, a quiff of fries on its forehead and a curl of a tail. A cold one (on dry land) is the same
model in a pale blue coat (sausloper_koud.png: same atlas layout). The saddle is the bone "zadel" (hidden unless it wears one).
Het Sausblubje: a wobbly cube of golden sauce in three slices (like the magma cube: they come apart when it jumps and show
the glowing core), a caramel crust on top, a guh face over the front of all three slices, two small ears. One model for the
three sizes (the renderer scales it); sausblubje_glowmask.png lights the core and the eyes' shine.

Textures are painted face by face on an atlas (boerderij_dieren.Atlas: every cube face its own patch, 4 texels per pixel).

  build(h)       writes geckolib/models/entity/<x>.geo.json, geckolib/animations/entity/<x>.animation.json and
                 textures/entity/{sausloper,sausloper_koud,sausblubje,sausblubje_glowmask}.png
  check(h)       every animated bone exists, the animations the Java code asks for exist, the saddle and head bones exist
  preview(out)   (python tools/features/sausdieren_modellen.py <out>) offline renders of both (wiki_renders), saddled too
"""
import math
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import boerderij_dieren as bd
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import boerderij_dieren as bd

Atlas, plain, disc, guh_face, cube, bone, geo, transparent = bd.Atlas, bd.plain, bd.disc, bd.guh_face, bd.cube, bd.bone, bd.geo, bd.transparent
S = bd.S

# --- colours ------------------------------------------------------------------------------------------------------------------
LOPER = {   # fur, light (belly, tail), inner ear, leg, foot, fries, fries tip
    "warm": ((244, 150, 100), (255, 214, 168), (236, 120, 160), (190, 100, 66), (252, 206, 110), (250, 208, 84), (255, 238, 160)),
    "koud": ((176, 190, 228), (222, 230, 248), (150, 150, 214), (128, 138, 190), (200, 208, 236), (226, 218, 150), (244, 240, 200)),
}
ZADEL = (150, 84, 48)
ZADEL_LICHT = (196, 124, 70)
ZADEL_GESP = (236, 200, 96)
SAUS = (242, 160, 46)
SAUS_LICHT = (255, 206, 96)
KORST = (196, 106, 38)
KERN = (255, 232, 120)
OOR_BINNEN = (240, 128, 110)


# =====================================================================================================================
# painters
# =====================================================================================================================
def buik(fur, light):
    """Fur with a light round belly patch low on the face."""
    def f(W, H, rng, a):
        disc(a, W / 2, H * 0.86, W * 0.3, light, 0.85, ry=H * 0.2)
    return f


def onderkant(fur, light):
    def f(W, H, rng, a):
        disc(a, W / 2, H / 2, W * 0.36, light, 0.9, ry=H * 0.36)
    return plain(fur, 8, f)


def frietje(kleur, tip):
    """A fry: golden, lighter towards the top, a darker fried edge."""
    def paint(W, H, rng):
        a = plain(kleur, 8)(W, H, rng).astype(np.float32)
        for y in range(H):
            t = 1 - y / max(1, H - 1)
            a[y, :, :3] = a[y, :, :3] * (1 - 0.5 * t) + np.array(tip) * 0.5 * t
        a[:, 0, :3] *= 0.86
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def leer(kleur, licht):
    """Saddle leather: a lighter stitched edge."""
    def paint(W, H, rng):
        a = plain(kleur, 8)(W, H, rng).astype(np.float32)
        if W > 6 and H > 6:
            for x in range(1, W - 1, 2):
                a[1, x, :3] = licht
                a[H - 2, x, :3] = licht
            for y in range(1, H - 1, 2):
                a[y, 1, :3] = licht
                a[y, W - 2, :3] = licht
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def saus(basis=SAUS, licht=SAUS_LICHT, korst=None, glans=True):
    """Glossy sauce: soft lighter blobs (bubbles), a shine in the top left; with `korst` a caramel crust with drips."""
    def paint(W, H, rng):
        a = plain(basis, 8)(W, H, rng).astype(np.float32)
        for _ in range(max(1, W * H // 90)):
            cx, cy, r = rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.2, 2.6)
            disc(a, cx, cy, r, licht, 0.5)
            disc(a, cx - r * 0.3, cy - r * 0.3, r * 0.35, (255, 244, 200), 0.7)
        if korst:
            for x in range(W):
                d = 0.38 + 0.22 * math.sin(x * 0.55 + 1.3) + 0.12 * math.sin(x * 1.7)
                for y in range(int(H * max(0.12, d))):
                    a[y, x, :3] = np.array(korst) * (0.94 + 0.1 * math.sin(x * 0.9 + y))
        if glans:
            disc(a, W * 0.22, H * 0.24, max(1.2, W * 0.07), (255, 250, 226), 0.55)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def korst_boven(W, H, rng):
    """The top of the Sausblubje: a caramel crust with a few glossy sauce bubbles breaking through."""
    a = plain(KORST, 10)(W, H, rng).astype(np.float32)
    for _ in range(max(2, W * H // 70)):
        cx, cy, r = rng.uniform(1, W - 1), rng.uniform(1, H - 1), rng.uniform(1.4, 2.8)
        disc(a, cx, cy, r, SAUS, 0.9)
        disc(a, cx - r * 0.3, cy - r * 0.3, r * 0.4, SAUS_LICHT, 0.9)
    return np.clip(a, 0, 255).astype(np.uint8)


def kern(W, H, rng):
    """The glowing core: bright yellow, a little hotter in the middle."""
    a = plain(KERN, 6)(W, H, rng).astype(np.float32)
    disc(a, W / 2, H / 2, W * 0.3, (255, 250, 200), 0.6, ry=H * 0.3)
    return np.clip(a, 0, 255).astype(np.uint8)


def gezicht_deel(vol, rijen, van, tot):
    """One horizontal slice (rows van..tot of `rijen` model pixels, top = 0) of a face painted over the whole front."""
    def paint(W, H, rng):
        heel = vol(W, int(round(rijen * S)), np.random.default_rng(21301203))
        heel = np.pad(heel, ((0, 2 * S), (0, 0), (0, 0)), mode="edge")     # (a patch is whole pixels high: a bit more than the slice)
        y0 = int(round(van * S))
        return heel[y0:y0 + H, :W].copy()
    return paint


def alleen(painter, gloei):
    """For the glowmask atlas: a painter stays when it glows, else its patch is see-through (the layout stays the same)."""
    def paint(W, H, rng):
        a = painter(W, H, rng)
        return a if gloei else np.zeros_like(a)
    return paint


def ogen_glans(vol):
    """Of a face painter only the white shine of the eyes (for the glowmask), a little dimmed."""
    def paint(W, H, rng):
        a = vol(W, H, rng).astype(np.int32)
        wit = (a[..., 0] > 245) & (a[..., 1] > 245) & (a[..., 2] > 245)
        out = np.zeros((H, W, 4), np.uint8)
        out[wit] = (255, 255, 255, 150)
        return out
    return paint


# =====================================================================================================================
# models
# =====================================================================================================================
def sausloper(atlas, kleur="warm"):
    fur, light, inner, poot, voet, friet, tip = LOPER[kleur]
    vacht = plain(fur, 8)
    snoet = guh_face(fur, eye_y=0.42, eye_dx=0.25, eye_r=0.165, snoet=True, spots=buik(fur, light))
    poot_p, voet_p = plain(poot, 7), plain(voet, 7)
    fr = frietje(friet, tip)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("lichaam", "root", [0, 17, 0], []),
             # the round guh on top: the "head" turns to where it looks
             bone("head", "lichaam", [0, 17, 0], [
                 cube(atlas, [-7, 16, -7], [14, 12, 14], vacht, overrides={"north": snoet, "down": onderkant(fur, light)}),
                 cube(atlas, [-6, 28, -6], [12, 1.5, 12], vacht),
                 cube(atlas, [-6, 15, -6], [12, 1, 12], plain(light, 6)),
                 cube(atlas, [-7.8, 18, -5], [0.8, 8, 10], vacht), cube(atlas, [7, 18, -5], [0.8, 8, 10], vacht),
                 cube(atlas, [-5, 18, 7], [10, 8, 0.8], vacht)]),
             bone("tail", "head", [0, 20, 7.6], [cube(atlas, [-1.2, 19, 7.6], [2.4, 2.4, 2.2], plain(light, 6)),
                                                 cube(atlas, [-0.8, 21, 9.2], [1.6, 1.8, 1.4], plain(light, 6))])]
    bones += bd.ears(atlas, fur, inner, 3.2, 27.4, -2.6, 5.0, 5.0)
    # a quiff of fries on its forehead: six sticks, each its own bone so they can wiggle
    frieten = [(-2.6, -5.2, 4.0, -14, 10), (-1.2, -5.8, 5.4, -6, 4), (0.4, -5.4, 4.6, -10, -5), (1.8, -5.0, 3.6, -16, -12),
               (-0.6, -4.2, 3.2, 6, 2), (1.0, -4.0, 2.8, 8, -8)]
    bones.append(bone("frietjes", "head", [0, 29.5, -4.6], []))
    for i, (x, z, hoog, rx, rz) in enumerate(frieten):
        bones.append(bone(f"friet_{i}", "frietjes", [x + 0.5, 29.2, z + 0.5], [cube(atlas, [x, 29.0, z], [1, hoog, 1], fr)],
                          rotation=[rx, 0, rz]))
    # the two long legs with spoon feet
    for side, sx in (("left", 1), ("right", -1)):
        x = 2.4 if sx > 0 else -4.4
        bones.append(bone(f"leg_{side}", "lichaam", [x + 1, 17, 0], [
            cube(atlas, [x, 1, -1], [2, 15.5, 2], poot_p),
            cube(atlas, [x - 0.3, 7.6, -1.3], [2.6, 1.8, 2.6], plain(tuple(min(255, c + 24) for c in poot), 6)),
            cube(atlas, [x - 1, 0, -3.6], [4, 1.2, 5.4], voet_p)]))
    # the saddle (only drawn when it wears one): a seat, a pommel, two flaps, the girth
    zadel, gesp = leer(ZADEL, ZADEL_LICHT), plain(ZADEL_GESP, 6)
    bones.append(bone("zadel", "head", [0, 29, 1], [
        cube(atlas, [-4.5, 29.4, -3.2], [9, 1.3, 9.6], zadel),
        cube(atlas, [-3.5, 30.7, 5.0], [7, 1.4, 1.4], zadel),
        cube(atlas, [-1.2, 30.7, -3.2], [2.4, 1.6, 1.6], zadel),
        cube(atlas, [-8.3, 21.5, -2.6], [0.6, 7, 7], zadel), cube(atlas, [7.7, 21.5, -2.6], [0.6, 7, 7], zadel),
        cube(atlas, [-8.6, 23.6, 0.2], [0.5, 1.6, 1.6], gesp), cube(atlas, [8.1, 23.6, 0.2], [0.5, 1.6, 1.6], gesp),
        cube(atlas, [-7.4, 28.2, 0.0], [14.8, 1.4, 1.8], plain(ZADEL_LICHT, 6))]))
    return geo("sausloper", bones, 1.6, 2.4)


def sausblubje(atlas, gloei=False):
    """8 x 7.5 x 8 pixels: three slices over a core. With gloei=True the same layout painted for the glowmask."""
    face = guh_face(SAUS, eye_y=0.5, eye_dx=0.25, eye_r=0.19, snoet=True)
    korst_face = saus(korst=KORST)

    def voorkant(W, H, rng):
        # the face over a sauce front with the crust dripping down from the top
        a = korst_face(W, H, rng).astype(np.float32)
        f = face(W, H, rng).astype(np.float32)
        y0 = int(H * 0.26)
        a[y0:] = f[y0:]
        return a.astype(np.uint8)

    zij = saus(korst=KORST)
    onder = saus(glans=False)

    def p(painter, glows=False, ogen=None):
        if not gloei:
            return painter
        return ogen_glans(ogen) if ogen else alleen(painter, glows)

    def laag(naam, y, hoog, van, top=None):
        north = gezicht_deel(voorkant, 7.5, van, van + hoog)
        zijkant = gezicht_deel(zij, 7.5, van, van + hoog)
        over = {"north": p(north, ogen=north), "up": p(top or onder), "down": p(onder)}
        return bone(naam, "body", [0, y, 0], [cube(atlas, [-4, y, -4], [8, hoog, 8], p(zijkant), overrides=over)])

    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 0, 0], []),
             bone("kern", "body", [0, 3.5, 0], [cube(atlas, [-3, 0.4, -3], [6, 6.6, 6], p(kern, glows=True))]),
             laag("laag_onder", 0, 2.5, 5.0),
             laag("laag_midden", 2.5, 2.5, 2.5),
             laag("laag_boven", 5.0, 2.5, 0.0, top=korst_boven)]
    # a blob of sauce on top and two small ears on the top slice
    bones.append(bone("klodder", "laag_boven", [0.6, 7.5, 0.4], [cube(atlas, [-0.6, 7.5, -0.8], [2.4, 0.9, 2.4], p(saus()))]))
    def oor(W, H, rng):
        a = plain(SAUS, 8)(W, H, rng).astype(np.float32)
        disc(a, W / 2, H * 0.58, W * 0.3, OOR_BINNEN, 1.0, ry=H * 0.3)
        return np.clip(a, 0, 255).astype(np.uint8)
    for side, sx in (("left", 1), ("right", -1)):
        x = 1.6 if sx > 0 else -3.8
        bones.append(bone(f"ear_{side}", "laag_boven", [x + 1.1, 7.5, -0.4], [
            cube(atlas, [x, 7.5, -1.0], [2.2, 1.8, 1.2], p(plain(SAUS, 8)), overrides={"north": p(oor)})]))
    return geo("sausblubje", bones, 1.6, 1.6)


def sausblubje_glowmask(atlas):
    """The same atlas layout with only the core (and the shine of the eyes) painted."""
    return sausblubje(atlas, gloei=True)


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(pairs):
    return {str(round(t, 3)): v for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": length, "bones": bones}


def frietjes_wuif(length, hoek=6):
    return {"frietjes": {"rotation": kf([(0, [0, 0, 0]), (length * 0.25, [hoek, 0, hoek * 0.6]), (length * 0.75, [-hoek * 0.6, 0, -hoek]),
                                         (length, [0, 0, 0])])}}


def oortjes(length, hoek=16):
    return {"ear_left": {"rotation": kf([(0, [0, 0, 0]), (length * 0.7, [0, 0, 0]), (length * 0.76, [0, 0, -hoek]), (length * 0.82, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (length * 0.3, [0, 0, 0]), (length * 0.36, [0, 0, hoek]), (length * 0.42, [0, 0, 0])])}}


def sausloper_anims():
    idle = {"head": {"position": kf([(0, [0, 0, 0]), (1.5, [0, 0.6, 0]), (3.0, [0, 0, 0])]),
                     "scale": kf([(0, [1, 1, 1]), (1.5, [1.015, 1.02, 1.015]), (3.0, [1, 1, 1])])},
            "tail": {"rotation": kf([(0, [0, 0, 0]), (0.75, [0, 14, 0]), (2.25, [0, -14, 0]), (3.0, [0, 0, 0])])},
            "leg_left": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 0, -1.5]), (3.0, [0, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 0, 1.5]), (3.0, [0, 0, 0])])}}
    idle.update(frietjes_wuif(3.0, 5))
    idle.update(oortjes(3.0))
    L = 0.9
    walk = {"leg_left": {"rotation": kf([(0, [32, 0, 0]), (L / 2, [-32, 0, 0]), (L, [32, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [-32, 0, 0]), (L / 2, [32, 0, 0]), (L, [-32, 0, 0])])},
            "head": {"position": kf([(0, [0, 0, 0]), (L / 4, [0, 1.0, 0]), (L / 2, [0, 0, 0]), (L * 3 / 4, [0, 1.0, 0]), (L, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 3.5]), (L / 2, [0, 0, -3.5]), (L, [0, 0, 3.5])])},
            "tail": {"rotation": kf([(0, [0, 16, 0]), (L / 2, [0, -16, 0]), (L, [0, 16, 0])])}}
    walk.update(frietjes_wuif(L, 9))
    # shivering: the whole guh trembles, knees together, ears flat, the fries rattle
    tril = [(round(i * 0.05, 2), [0, 0, 2.2 if i % 2 else -2.2]) for i in range(9)]
    bibber = {"head": {"rotation": kf(tril), "position": [0, -0.8, 0]},
              "leg_left": {"rotation": kf([(t, [0, 0, 5 + (0.8 if i % 2 else -0.8)]) for i, (t, _) in enumerate(tril)])},
              "leg_right": {"rotation": kf([(t, [0, 0, -5 - (0.8 if i % 2 else -0.8)]) for i, (t, _) in enumerate(tril)])},
              "ear_left": {"rotation": [0, 0, 18]}, "ear_right": {"rotation": [0, 0, -18]},
              "frietjes": {"rotation": kf([(t, [3 if i % 2 else -3, 0, 0]) for i, (t, _) in enumerate(tril)])},
              "tail": {"rotation": kf([(t, [0, 8 if i % 2 else -8, 0]) for i, (t, _) in enumerate(tril)])}}
    Lk = 1.2
    stappen = [(round(i * 0.1, 2), i) for i in range(13)]
    walk_koud = {"leg_left": {"rotation": kf([(0, [18, 0, 4]), (Lk / 2, [-18, 0, 4]), (Lk, [18, 0, 4])])},
                 "leg_right": {"rotation": kf([(0, [-18, 0, -4]), (Lk / 2, [18, 0, -4]), (Lk, [-18, 0, -4])])},
                 "head": {"rotation": kf([(t, [0, 0, 2.0 if i % 2 else -2.0]) for t, i in stappen]), "position": [0, -0.8, 0]},
                 "ear_left": {"rotation": [0, 0, 18]}, "ear_right": {"rotation": [0, 0, -18]},
                 "frietjes": {"rotation": kf([(t, [3 if i % 2 else -3, 0, 0]) for t, i in stappen])}}
    # sitting: the legs fold forward flat on the ground, the guh rests on them
    zit = {"lichaam": {"position": [0, -14.2, 0]},
           "leg_left": {"rotation": [-86, 0, -8]}, "leg_right": {"rotation": [-86, 0, 8]},
           "head": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.02, 0.985, 1.02]), (3.0, [1, 1, 1])])},
           "tail": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 10, 0]), (3.0, [0, 0, 0])])}}
    zit.update(frietjes_wuif(3.0, 4))
    blij = {"lichaam": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 3.0, 0]), (0.3, [0, 0, 0]), (0.45, [0, 2.0, 0]), (0.6, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-8, 0, 7]), (0.45, [-6, 0, -7]), (0.6, [0, 0, 0])])},
            "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, -22]), (0.3, [0, 0, 0]), (0.45, [0, 0, -22]), (0.6, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 22]), (0.3, [0, 0, 0]), (0.45, [0, 0, 22]), (0.6, [0, 0, 0])])},
            "leg_left": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, -9]), (0.3, [0, 0, 0]), (0.45, [0, 0, -6]), (0.6, [0, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 9]), (0.3, [0, 0, 0]), (0.45, [0, 0, 6]), (0.6, [0, 0, 0])])},
            "tail": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 30, 0]), (0.2, [0, -30, 0]), (0.3, [0, 30, 0]), (0.4, [0, -30, 0]), (0.6, [0, 0, 0])])}}
    eet = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [24, 0, 0]), (0.35, [16, 0, 0]), (0.5, [24, 0, 0]), (0.65, [16, 0, 0]),
                                    (0.8, [24, 0, 0]), (1.0, [0, 0, 0])])},
           "frietjes": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-12, 0, 0]), (0.6, [10, 0, 0]), (1.0, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(3.0, idle), "walk": anim(L, walk), "bibber": anim(0.4, bibber), "walk_koud": anim(Lk, walk_koud),
        "zit": anim(3.0, zit), "blij": anim(0.6, blij, False), "eet": anim(1.0, eet, False)}}


def sausblubje_anims():
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (0.8, [1.04, 0.95, 1.04]), (1.6, [1, 1, 1])])},
            "laag_boven": {"position": kf([(0, [0, 0, 0]), (0.8, [0, 0.25, 0]), (1.6, [0, 0, 0])]),
                           "rotation": kf([(0, [0, 0, 0]), (0.4, [0, 0, 1.5]), (1.2, [0, 0, -1.5]), (1.6, [0, 0, 0])])},
            "laag_midden": {"position": kf([(0, [0, 0, 0]), (0.8, [0, 0.12, 0]), (1.6, [0, 0, 0])])},
            "ear_left": {"rotation": kf([(0, [0, 0, 0]), (1.1, [0, 0, 0]), (1.2, [0, 0, -18]), (1.3, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.5, [0, 0, 0]), (0.6, [0, 0, 18]), (0.7, [0, 0, 0])])}}
    # in the air the three slices come apart and the core shows (the magma cube's jump)
    lucht = {"body": {"scale": [0.92, 1.1, 0.92]},
             "laag_boven": {"position": kf([(0, [0, 2.0, 0]), (0.3, [0, 2.4, 0]), (0.6, [0, 2.0, 0])])},
             "laag_midden": {"position": kf([(0, [0, 1.0, 0]), (0.3, [0, 1.2, 0]), (0.6, [0, 1.0, 0])])},
             "kern": {"scale": [0.96, 1.25, 0.96]},
             "ear_left": {"rotation": [0, 0, -20]}, "ear_right": {"rotation": [0, 0, 20]}}
    plof = {"body": {"scale": kf([(0, [1.28, 0.62, 1.28]), (0.12, [0.94, 1.1, 0.94]), (0.22, [1.05, 0.96, 1.05]), (0.3, [1, 1, 1])])}}
    blij = {"body": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 1.6, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.0, 0]), (0.6, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 10]), (0.3, [0, 0, -10]), (0.45, [0, 0, 6]), (0.6, [0, 0, 0])])},
            "laag_boven": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 1.2, 0]), (0.3, [0, 0, 0]), (0.6, [0, 0, 0])])}}
    eet = {"laag_boven": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 1.8, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.2, 0]), (0.6, [0, 0, 0]), (0.8, [0, 0, 0])]),
                          "rotation": kf([(0, [0, 0, 0]), (0.15, [-14, 0, 0]), (0.3, [0, 0, 0]), (0.45, [-10, 0, 0]), (0.6, [0, 0, 0])])},
           "body": {"scale": kf([(0, [1, 1, 1]), (0.3, [1.06, 0.94, 1.06]), (0.6, [1.06, 0.94, 1.06]), (0.8, [1, 1, 1])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(1.6, idle), "lucht": anim(0.6, lucht), "plof": anim(0.3, plof, False), "blij": anim(0.6, blij, False),
        "eet": anim(0.8, eet, False)}}


# =====================================================================================================================
NODIG = {"sausloper": (["idle", "walk", "bibber", "walk_koud", "zit", "blij", "eet"], ["head", "zadel", "leg_left", "leg_right"]),
         "sausblubje": (["idle", "lucht", "plof", "blij", "eet"], ["body", "kern", "laag_boven"])}
SEED_LOPER, SEED_BLUBJE = 21301251, 21301252


def maak():
    """Everything in memory: {file name: geo / animation json}, {texture name: RGBA array}."""
    json_uit, tex = {}, {}
    for kleur, naam in (("warm", "sausloper"), ("koud", "sausloper_koud")):
        atlas = Atlas(SEED_LOPER)
        model = sausloper(atlas, kleur)
        if kleur == "warm":
            json_uit["models/entity/sausloper.geo.json"] = model
        tex[naam] = atlas.img
    json_uit["animations/entity/sausloper.animation.json"] = sausloper_anims()
    atlas = Atlas(SEED_BLUBJE)
    json_uit["models/entity/sausblubje.geo.json"] = sausblubje(atlas)
    tex["sausblubje"] = atlas.img
    atlas = Atlas(SEED_BLUBJE)
    sausblubje_glowmask(atlas)
    tex["sausblubje_glowmask"] = atlas.img
    json_uit["animations/entity/sausblubje.animation.json"] = sausblubje_anims()
    return json_uit, tex


def build(h):
    json_uit, tex = maak()
    for pad, d in json_uit.items():
        h.w(f"{h.A}/geckolib/{pad}", d)
    for naam, img in tex.items():
        h.save(Image.fromarray(img), "entity", f"{naam}.png")


def check(h):
    import json
    problems = []
    for name, (wanted, botten) in NODIG.items():
        g = json.load(open(f"{h.A}/geckolib/models/entity/{name}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
        for b in botten:
            if b not in names:
                problems.append(f"{name}: no bone {b}")
        anims = json.load(open(f"{h.A}/geckolib/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
        for an in wanted:
            if an not in anims:
                problems.append(f"{name}: no animation {an}")
        for an, a in anims.items():
            for bn in a.get("bones", {}):
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
    return problems


def preview(out):
    """Offline renders (front-left, side, behind) of the Sausloper (warm, cold, saddled) and the Sausblubje into out/."""
    import json
    import tempfile
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    json_uit, tex = maak()
    arrays = {f"mem:{k}": v.astype(np.float32) for k, v in tex.items()}
    echte = wr.tex_array
    wr.tex_array = lambda ref: arrays[ref] if ref in arrays else echte(ref)
    tmp = tempfile.mkdtemp()
    paths = {}
    for pad, d in json_uit.items():
        if pad.startswith("models/"):
            paths[os.path.basename(pad).split(".")[0]] = os.path.join(tmp, os.path.basename(pad))
            json.dump(d, open(paths[os.path.basename(pad).split(".")[0]], "w"))
    for naam, img in tex.items():
        Image.fromarray(img).save(os.path.join(out, f"tex_{naam}.png"))
    tiles = []
    for model, textuur, hide in (("sausloper", "sausloper", ("zadel",)), ("sausloper", "sausloper", ()), ("sausloper", "sausloper_koud", ("zadel",)),
                                 ("sausblubje", "sausblubje", ()), ("sausblubje", "sausblubje_glowmask", ())):
        q = wr.geo_quads(paths[model], f"mem:{textuur}", hide=hide)
        for yaw, pitch in ((205, -14), (270, -8), (25, -18)):
            img = wr.render(q, yaw, pitch, 360, margin=0.06)
            tiles.append(img)
    sheet = Image.new("RGBA", (360 * 3, 360 * ((len(tiles) + 2) // 3)), (62, 50, 54, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 3) * 360, (i // 3) * 360))
    sheet.save(os.path.join(out, "sausdieren_sheet.png"))
    wr.tex_array = echte


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
