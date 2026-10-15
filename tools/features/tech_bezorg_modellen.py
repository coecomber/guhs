"""
bbq2 (tech-bezorg) - the looks of the Bezorgguhtje: its GeckoLib model, animations and two textures (awake / asleep).

A mini-guh standing on a step (a kick scooter): guh fur, the big glossy guh eyes, round ears with a little cap between them,
two paws on the handlebar, one foot on the deck and one that pushes. On its back a thermal delivery backpack that is much
too big for it (cheese yellow, a kaasknabbel logo on the back, a lid that stands open when something is in it, a side
pocket) and a pennant on a pole so you see it coming. Textures are painted face by face on an atlas
(boerderij_dieren.Atlas: every cube face its own patch, 4 texels per model pixel).

  build(h)       writes geckolib/models/entity/bezorgguhtje.geo.json, geckolib/animations/entity/bezorgguhtje.animation.json,
                 textures/entity/bezorgguhtje.png and bezorgguhtje_slaapt.png (the same atlas with closed eyes)
  check(h)       every animated bone exists, the animations the Java code asks for exist
  preview(out)   (python tools/features/tech_bezorg_modellen.py <out>) offline renders (wiki_renders), also of the two blocks
Animations (BezorgguhtjeEntity): idle, rijd, slaap (loops), vol / leeg (the backpack, loops), laad, zwaai, blij, hop.
The model is built at "comfortable" pixel sizes and drawn at SCHAAL (the renderer scales it): about 0.8 block tall.
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

Atlas, plain, disc, guh_face, cube, bone, geo = bd.Atlas, bd.plain, bd.disc, bd.guh_face, bd.cube, bd.bone, bd.geo

NAAM = "bezorgguhtje"
SCHAAL = 0.8                        # = TechbezorgClient.SCHAAL (the renderer draws the model this much smaller)
SEED = 21306501

# --- colours ------------------------------------------------------------------------------------------------------------------
VACHT = (195, 160, 205)             # guh fur (the colour of guh.png)
VACHT_LICHT = (226, 204, 232)
OOR_BINNEN = (236, 128, 168)
POOT = (176, 138, 190)
RUGZAK = (250, 200, 62)             # cheese yellow thermal bag
RUGZAK_DONKER = (226, 150, 44)
RUGZAK_RAND = (150, 96, 34)
RIEM = (92, 70, 96)
KNABBEL = (255, 232, 130)
KNABBEL_GAT = (214, 150, 40)
PET = (70, 190, 196)                # teal cap
PET_DONKER = (40, 140, 150)
STEP = (96, 206, 200)               # mint metal
STEP_DONKER = (52, 150, 150)
DEK = (72, 62, 78)                  # grip tape
WIEL = (240, 120, 170)              # pink wheels
WIEL_AS = (250, 236, 240)
HANDVAT = (240, 120, 170)
BEL = (255, 214, 92)
STOK = (220, 214, 224)
VLAG = (255, 120, 170)
KAAS = (250, 206, 84)
PAKJE = (190, 140, 96)
LINT = (240, 110, 160)


# =====================================================================================================================
# painters
# =====================================================================================================================
def slaap_gezicht(fur, eye_y=0.42, eye_dx=0.26, eye_r=0.17):
    """The guh face asleep: two closed eyes (a curved line with lashes), blush and the little nose."""
    def paint(W, H, rng):
        a = plain(fur, 8)(W, H, rng).astype(np.float32)
        r = eye_r * W
        for sx in (-1, 1):
            cx, cy = W / 2 + sx * eye_dx * W, H * eye_y + 0.25 * r
            for x in range(int(cx - r), int(cx + r) + 1):
                t = (x + 0.5 - cx) / r
                if abs(t) > 1:
                    continue
                y = cy + 0.38 * r * (1 - t * t)                     # a smiling arc: lowest in the middle
                for dy in (0, 1):
                    yy = int(y) + dy
                    if 0 <= yy < H and 0 <= x < W:
                        a[yy, x, :3] = (22, 16, 34)
            for lx in (-0.9, 0.9):                                  # lashes at both ends
                x, y = int(cx + lx * r), int(cy - 0.05 * r)
                if 0 <= y - 1 < H and 0 <= x < W:
                    a[y - 1:y + 1, x, :3] = (22, 16, 34)
            disc(a, cx + sx * 0.25 * r, cy + 1.2 * r, 0.62 * r, (255, 150, 186), 0.55, ry=0.32 * r)
        ny = H * eye_y + 1.25 * r
        nw = max(2.0, W * 0.07)
        for y in range(int(ny), int(ny + nw * 0.8) + 1):
            half = nw * (1 - (y - ny) / (nw * 1.4))
            for x in range(int(W / 2 - half), int(W / 2 + half) + 1):
                if 0 <= y < H and 0 <= x < W:
                    a[y, x, :3] = (214, 96, 150)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def stof(colour, rand, var=6, stiksel=None):
    """Bag cloth: the colour with a darker seam around the patch (and an optional dashed stitch line inside it)."""
    def paint(W, H, rng):
        a = plain(colour, var)(W, H, rng).astype(np.float32)
        a[:2, :, :3] = rand
        a[-2:, :, :3] = rand
        a[:, :2, :3] = rand
        a[:, -2:, :3] = rand
        if stiksel:
            for x in range(5, W - 5, 4):
                a[4:5, x:x + 2, :3] = stiksel
                a[H - 5:H - 4, x:x + 2, :3] = stiksel
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def logo(W, H, rng):
    """The back of the bag: a kaasknabbel in a white circle (the delivery logo) and a reflecting strip under it."""
    a = stof(RUGZAK, RUGZAK_RAND, 6, RUGZAK_DONKER)(W, H, rng).astype(np.float32)
    cx, cy, r = W / 2, H * 0.42, min(W, H) * 0.3
    disc(a, cx, cy, r + 1.5, RUGZAK_RAND)
    disc(a, cx, cy, r, (255, 250, 244))
    # the knabbel: a fat wedge of cheese with three holes
    for y in range(int(cy - r * 0.55), int(cy + r * 0.55) + 1):
        t = (y - (cy - r * 0.55)) / (r * 1.1)
        half = r * (0.25 + 0.5 * t)
        for x in range(int(cx - half), int(cx + half) + 1):
            if 0 <= y < H and 0 <= x < W:
                a[y, x, :3] = KNABBEL if t > 0.18 else (255, 244, 180)
    for dx, dy, rr in ((-0.18, 0.2, 0.1), (0.2, 0.32, 0.12), (0.02, -0.08, 0.08)):
        disc(a, cx + dx * r * 2, cy + dy * r * 2, rr * r * 2, KNABBEL_GAT)
    y0 = int(H * 0.8)
    a[y0:y0 + 3, 4:W - 4, :3] = (244, 244, 250)                    # the reflector
    for x in range(4, W - 4, 6):
        a[y0:y0 + 3, x:x + 2, :3] = (255, 120, 170)
    return np.clip(a, 0, 255).astype(np.uint8)


def band(colour, streep, breed=0.34):
    """A patch with a stripe across the middle (the mint deck with its grip tape, the cap with its white band)."""
    def paint(W, H, rng):
        a = plain(colour, 6)(W, H, rng).astype(np.float32)
        h0, h1 = int(H * (0.5 - breed / 2)), int(H * (0.5 + breed / 2)) + 1
        a[h0:h1, :, :3] = streep
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def wiel(W, H, rng):
    """The side of a wheel: a pink tyre (round: see-through corners) around a light hub with a spoke cross."""
    a = np.zeros((H, W, 4), np.float32)
    cx, cy, r = W / 2, H / 2, min(W, H) / 2
    disc(a, cx, cy, r, (150, 60, 100))
    disc(a, cx, cy, r - 1.2, WIEL)
    disc(a, cx, cy, r * 0.45, WIEL_AS)
    for d in range(int(-r * 0.45), int(r * 0.45) + 1):
        for x, y in ((cx + d, cy), (cx, cy + d)):
            if 0 <= int(y) < H and 0 <= int(x) < W:
                a[int(y), int(x), :3] = (200, 150, 176)
    return np.clip(a, 0, 255).astype(np.uint8)


def hartje(colour, hart):
    def paint(W, H, rng):
        a = plain(colour, 5)(W, H, rng).astype(np.float32)
        cx, cy, s = W / 2, H / 2, min(W, H) * 0.2
        disc(a, cx - s * 0.55, cy - s * 0.3, s * 0.62, hart)
        disc(a, cx + s * 0.55, cy - s * 0.3, s * 0.62, hart)
        for y in range(int(cy), int(cy + s * 1.2) + 1):
            half = s * 1.15 * (1 - (y - cy) / (s * 1.25))
            for x in range(int(cx - half), int(cx + half) + 1):
                if 0 <= y < H and 0 <= x < W:
                    a[y, x, :3] = hart
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def kaasgaten(W, H, rng):
    a = plain(KAAS, 6)(W, H, rng).astype(np.float32)
    for _ in range(max(2, W * H // 60)):
        disc(a, rng.uniform(2, W - 2), rng.uniform(2, H - 2), rng.uniform(1.0, 1.9), KNABBEL_GAT)
    return np.clip(a, 0, 255).astype(np.uint8)


def pakje(W, H, rng):
    """A brown parcel with a pink ribbon cross."""
    a = plain(PAKJE, 7)(W, H, rng).astype(np.float32)
    a[H // 2 - 1:H // 2 + 1, :, :3] = LINT
    a[:, W // 2 - 1:W // 2 + 1, :3] = LINT
    return np.clip(a, 0, 255).astype(np.uint8)


# =====================================================================================================================
# the model (front = north = -z; y 0 = the ground)
# =====================================================================================================================
def model(atlas, slaapt=False):
    vacht = plain(VACHT, 8)
    metaal = plain(STEP, 6)
    donker = plain(STEP_DONKER, 5)
    tas = stof(RUGZAK, RUGZAK_RAND, 6, RUGZAK_DONKER)
    klep = stof(RUGZAK_DONKER, RUGZAK_RAND, 6)
    riem = plain(RIEM, 5)
    gezicht = slaap_gezicht(VACHT) if slaapt else guh_face(VACHT, eye_y=0.44, eye_dx=0.25, eye_r=0.17)
    band_wiel = plain(WIEL, 6)
    bones = [
        bone("root", None, [0, 0, 0], []),
        # --- the step: a mint deck with grip tape, two pink wheels, a stem with handlebar, grips and a bell ---
        bone("step", "root", [0, 1.5, 0], [
            cube(atlas, [-1.75, 1.2, -6.5], [3.5, 0.9, 13.5], metaal, overrides={"up": band(STEP, DEK, 0.62)}),
            cube(atlas, [-1.25, 2.0, -8.6], [2.5, 1.4, 2.4], donker),                    # the fork / front fender
            cube(atlas, [-1.25, 2.9, 6.6], [2.5, 0.6, 3.2], donker)]),                   # the rear fender (also the brake)
        bone("wiel_voor", "step", [0, 1.5, -8.0], [
            cube(atlas, [-0.7, 0, -9.5], [1.4, 3, 3], band_wiel, overrides={"east": wiel, "west": wiel})]),
        bone("wiel_achter", "step", [0, 1.5, 8.2], [
            cube(atlas, [-0.7, 0, 6.7], [1.4, 3, 3], band_wiel, overrides={"east": wiel, "west": wiel})]),
        bone("stuur", "step", [0, 2.5, -7.6], [
            cube(atlas, [-0.5, 2.8, -8.1], [1, 4.6, 1], metaal),                         # the stem
            cube(atlas, [-3.4, 6.6, -8.2], [6.8, 0.9, 1.2], donker),                     # the handlebar
            cube(atlas, [-4.5, 6.45, -8.35], [1.3, 1.2, 1.5], plain(HANDVAT, 5)),        # grips
            cube(atlas, [3.2, 6.45, -8.35], [1.3, 1.2, 1.5], plain(HANDVAT, 5)),
            cube(atlas, [1.4, 7.4, -8.4], [1.3, 1.1, 1.3], plain(BEL, 5), overrides={"up": hartje(BEL, (255, 250, 220))})]),
        # --- the mini-guh ---
        bone("guh", "root", [0, 2.1, 0.5], []),
        bone("body", "guh", [0, 5, 0.5], [
            cube(atlas, [-3, 2.9, -2.2], [6, 5.4, 5.2], vacht, overrides={"north": bd.plain(VACHT, 8, bd.buikje(VACHT_LICHT))}),
            # the bag's straps over the chest
            cube(atlas, [-2.6, 3.4, -2.35], [1.0, 4.8, 0.3], riem), cube(atlas, [1.6, 3.4, -2.35], [1.0, 4.8, 0.3], riem),
            cube(atlas, [-2.6, 5.2, -2.4], [5.2, 0.8, 0.3], riem)]),
        bone("tail", "body", [0, 3.6, 3.0], [cube(atlas, [-0.4, 3.2, 2.6], [0.8, 0.8, 0.8], plain(POOT, 5))]),
        bone("head", "guh", [0, 8.0, -0.6], [
            cube(atlas, [-4.5, 7.6, -5.0], [9, 7.4, 7.4], vacht, overrides={"north": gezicht}),
            # the cap between the ears, with its peak
            cube(atlas, [-2.3, 14.8, -4.6], [4.6, 1.3, 5.6], plain(PET, 6), overrides={"north": band(PET, (255, 250, 244), 0.5)}),
            cube(atlas, [-2.3, 14.8, -6.6], [4.6, 0.5, 2.0], plain(PET_DONKER, 5))]),
    ]
    bones += bd.ears(atlas, VACHT, OOR_BINNEN, 2.5, 14.2, -1.6, 3.3, 3.3)
    for side, sx in (("left", 1), ("right", -1)):
        x0 = 3.0 if sx > 0 else -4.2
        bones.append(bone(f"arm_{side}", "guh", [sx * 3.6, 7.0, -2.0], [
            cube(atlas, [x0, 6.3, -7.6], [1.2, 1.2, 5.8], vacht, overrides={"north": plain(POOT, 5)})]))
    # the left foot stands on the deck, the right one pushes (its pivot is the hip)
    bones.append(bone("leg_left", "guh", [1.2, 3.2, 0.4], [cube(atlas, [0.3, 2.1, -1.6], [1.7, 1.1, 3.0], plain(POOT, 6))]))
    bones.append(bone("leg_right", "guh", [-1.2, 3.4, 0.6], [
        cube(atlas, [-2.0, 2.1, -0.6], [1.7, 1.1, 3.0], plain(POOT, 6))]))
    # --- the backpack: far too big ---
    bones += [
        bone("rugzak", "guh", [0, 4.0, 3.0], [
            cube(atlas, [-4.2, 3.4, 3.0], [8.4, 9.0, 6.0], tas, overrides={"south": logo}),
            cube(atlas, [4.2, 4.6, 4.2], [1.1, 3.4, 3.4], klep),                          # the side pocket
            cube(atlas, [-5.3, 4.6, 4.2], [1.1, 3.4, 3.4], klep),
            cube(atlas, [-3.2, 2.7, 3.6], [6.4, 0.7, 4.8], plain(RUGZAK_RAND, 5))]),      # the bottom
        bone("klep", "rugzak", [0, 12.4, 9.0], [
            cube(atlas, [-4.5, 12.2, 2.7], [9.0, 1.1, 6.6], klep),
            cube(atlas, [-1.0, 11.2, 2.5], [2.0, 1.4, 0.4], plain((244, 244, 250), 4))]),  # the buckle
        bone("lading", "rugzak", [0, 12.4, 6.0], [
            cube(atlas, [-3.0, 11.6, 4.2], [2.4, 3.6, 2.4], kaasgaten),
            cube(atlas, [0.4, 11.6, 4.0], [3.0, 2.6, 3.0], pakje)]),
        bone("vlag", "rugzak", [3.9, 12.4, 8.6], [
            cube(atlas, [3.7, 12.4, 8.4], [0.4, 6.6, 0.4], plain(STOK, 4)),
            cube(atlas, [0.7, 16.4, 8.5], [3.0, 2.4, 0.2], hartje(VLAG, (255, 244, 248)))]),
    ]
    return geo(NAAM, bones, 1.5, 1.6)


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(pairs):
    return {str(round(t, 3)): v for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": length, "bones": bones}


WEG = [0.01, 0.01, 0.01]


def animaties():
    oor = lambda L, hoek=16: {
        "ear_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.8, [0, 0, 0]), (L * 0.85, [0, 0, -hoek]), (L * 0.9, [0, 0, 0])])},
        "ear_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [0, 0, 0]), (L * 0.45, [0, 0, hoek]), (L * 0.5, [0, 0, 0])])}}
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.03, 1.03, 1.03]), (3, [1, 1, 1])])},
            "vlag": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 0, 5]), (3, [0, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.2, [-20, 0, 0]), (2.4, [0, 0, 0]), (3, [0, 0, 0])])}}
    idle.update(oor(3.0))
    L = 0.5
    rijd = {"wiel_voor": {"rotation": kf([(0, [0, 0, 0]), (L, [-360, 0, 0])])},
            "wiel_achter": {"rotation": kf([(0, [0, 0, 0]), (L, [-360, 0, 0])])},
            "guh": {"position": kf([(0, [0, 0, 0]), (L * 0.5, [0, 0.35, 0]), (L, [0, 0, 0])]),
                    "rotation": kf([(0, [5, 0, 0]), (L * 0.5, [9, 0, 0]), (L, [5, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [40, 0, 0]), (L * 0.5, [-50, 0, 0]), (L, [40, 0, 0])]),
                          "position": kf([(0, [0, -1.0, 0]), (L * 0.5, [0, -0.3, 0]), (L, [0, -1.0, 0])])},
            "vlag": {"rotation": kf([(0, [-22, 0, 0]), (L * 0.5, [-30, 0, 4]), (L, [-22, 0, 0])])},
            "ear_left": {"rotation": kf([(0, [-14, 0, 0]), (L * 0.5, [-20, 0, 0]), (L, [-14, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [-14, 0, 0]), (L * 0.5, [-20, 0, 0]), (L, [-14, 0, 0])])},
            "klep": {"rotation": kf([(0, [0, 0, 0]), (L * 0.5, [-5, 0, 0]), (L, [0, 0, 0])])}}
    # asleep: it leans on the handlebar, its head to one side, the ears hang, slow breathing
    slaap = {"guh": {"rotation": [14, 0, 0], "position": [0, -0.2, -0.4]},
             "head": {"rotation": kf([(0, [16, 0, 14]), (2, [18, 0, 14]), (4, [16, 0, 14])]), "position": [0, -0.8, -0.6]},
             "body": {"scale": kf([(0, [1, 1, 1]), (2, [1.05, 1.04, 1.05]), (4, [1, 1, 1])])},
             "ear_left": {"rotation": [0, 0, -26]}, "ear_right": {"rotation": [0, 0, 26]},
             "vlag": {"rotation": kf([(0, [0, 0, 8]), (2, [0, 0, 12]), (4, [0, 0, 8])])}}
    vol = {"klep": {"rotation": kf([(0, [58, 0, 0]), (0.6, [54, 0, 0]), (1.2, [58, 0, 0])])},
           "lading": {"scale": [1, 1, 1], "rotation": kf([(0, [0, 0, 2]), (0.6, [0, 0, -2]), (1.2, [0, 0, 2])])}}
    leeg = {"lading": {"scale": WEG, "position": [0, -3, 0]}}
    # at a stop: it turns to its bag, the lid flaps, the paws rummage
    laad = {"guh": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 35, 0]), (0.5, [0, -35, 0]), (0.8, [0, 20, 0]), (1.0, [0, 0, 0])]),
                    "position": kf([(0, [0, 0, 0]), (0.2, [0, 0.8, 0]), (0.35, [0, 0, 0]), (0.5, [0, 0.8, 0]), (0.65, [0, 0, 0]), (1.0, [0, 0, 0])])},
            "klep": {"rotation": kf([(0, [0, 0, 0]), (0.2, [70, 0, 0]), (0.75, [70, 0, 0]), (1.0, [0, 0, 0])])},
            "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-70, 0, 0]), (0.4, [-30, 0, 0]), (0.6, [-70, 0, 0]), (1.0, [0, 0, 0])])},
            "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-70, 0, 0]), (0.5, [-30, 0, 0]), (0.7, [-70, 0, 0]), (1.0, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 30, 0]), (0.55, [0, -30, 0]), (1.0, [0, 0, 0])])}}
    zwaai = {"arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-100, 0, -30]), (0.3, [-100, 0, 10]), (0.45, [-100, 0, -30]),
                                           (0.6, [-100, 0, 10]), (0.75, [-100, 0, -30]), (1.0, [0, 0, 0])])},
             "head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-8, 0, -10]), (0.8, [-8, 0, -10]), (1.0, [0, 0, 0])])},
             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, -18]), (0.5, [0, 0, 0]), (0.7, [0, 0, -18]), (1.0, [0, 0, 0])])}}
    blij = {"root": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 2.4, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.4, 0]), (0.6, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-10, 0, 8]), (0.6, [0, 0, 0])])},
            "vlag": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 18]), (0.3, [0, 0, -18]), (0.45, [0, 0, 18]), (0.6, [0, 0, 0])])}}
    # the "vahoeg" hop when a road is blocked: squash, stretch, poof
    hop = {"root": {"scale": kf([(0, [1, 1, 1]), (0.1, [1.2, 0.7, 1.2]), (0.25, [0.8, 1.3, 0.8]), (0.4, [1.1, 0.9, 1.1]), (0.5, [1, 1, 1])]),
                    "position": kf([(0, [0, 0, 0]), (0.25, [0, 3, 0]), (0.5, [0, 0, 0])])},
           "wiel_voor": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-720, 0, 0])])},
           "wiel_achter": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-720, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(3.0, idle), "rijd": anim(L, rijd), "slaap": anim(4.0, slaap), "vol": anim(1.2, vol), "leeg": anim(1.0, leeg),
        "laad": anim(1.0, laad, False), "zwaai": anim(1.0, zwaai, False), "blij": anim(0.6, blij, False), "hop": anim(0.5, hop, False)}}


NODIG = ["idle", "rijd", "slaap", "vol", "leeg", "laad", "zwaai", "blij", "hop"]   # = what BezorgguhtjeEntity plays


def build(h):
    A = h.A
    atlas = Atlas(SEED)
    h.w(f"{A}/geckolib/models/entity/{NAAM}.geo.json", model(atlas))
    h.save(Image.fromarray(atlas.img), "entity", f"{NAAM}.png")
    slaap = Atlas(SEED)                                   # (the same seed and order: the same layout, only the face differs)
    model(slaap, slaapt=True)
    h.save(Image.fromarray(slaap.img), "entity", f"{NAAM}_slaapt.png")
    h.w(f"{A}/geckolib/animations/entity/{NAAM}.animation.json", animaties())


def check(h):
    import json
    problems = []
    g = json.load(open(f"{h.A}/geckolib/models/entity/{NAAM}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
    names = {b["name"] for b in g["bones"]}
    for b in g["bones"]:
        if b.get("parent") and b["parent"] not in names:
            problems.append(f"{NAAM}: bone {b['name']} has a missing parent {b['parent']}")
    if "head" not in names:
        problems.append(f"{NAAM}: no head bone")
    anims = json.load(open(f"{h.A}/geckolib/animations/entity/{NAAM}.animation.json", encoding="utf-8"))["animations"]
    for an in NODIG:
        if an not in anims:
            problems.append(f"{NAAM}: no animation {an}")
    for an, a in anims.items():
        for bn in a.get("bones", {}):
            if bn not in names:
                problems.append(f"{NAAM}: animation {an} moves a missing bone {bn}")
    for tex in (f"{NAAM}.png", f"{NAAM}_slaapt.png"):
        if not os.path.exists(os.path.join(h.TEX, "entity", tex)):
            problems.append(f"{NAAM}: no texture {tex}")
    return problems


def preview(out):
    """Offline renders into out/: the Bezorgguhtje from four sides (awake and asleep), next to a guh for its size, and the
    Stepstation and the Haltepaaltjes as the game draws their block models."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo_dir = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    tiles = []
    for tex in (NAAM, f"{NAAM}_slaapt"):
        q = wr.geo_quads(os.path.join(geo_dir, f"{NAAM}.geo.json"), f"guhs:entity/{tex}")
        for yaw, pitch in ((205, -16), (150, -16), (25, -18), (270, -8)):
            img = wr.render(q, yaw, pitch, 360, margin=0.06)
            tiles.append(img)
            img.save(os.path.join(out, f"{tex}_{yaw}.png"))
    # next to a real guh (the mini-guh at its in-game scale)
    q = [wr.Quad(k.origin * SCHAAL + np.array([1.1, 0, 0]), k.u * SCHAAL, k.v * SCHAAL, k.tex, k.uv, k.normal)
         for k in wr.geo_quads(os.path.join(geo_dir, f"{NAAM}.geo.json"), f"guhs:entity/{NAAM}")]
    q += wr.geo_quads(os.path.join(geo_dir, "guh.geo.json"), "guhs:entity/guh")
    wr.render(q, 205, -14, 560, margin=0.06).save(os.path.join(out, "bezorgguhtje_naast_guh.png"))
    sheet = Image.new("RGBA", (360 * 4, 360 * ((len(tiles) + 3) // 4)), (236, 232, 240, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 4) * 360, (i // 4) * 360))
    sheet.save(os.path.join(out, "bezorgguhtje_sheet.png"))
    blokken = []
    for ref in ("guhs:block/stepstation_slaapt", "guhs:block/stepstation_werkt", "guhs:block/stepstation_vol",
                "guhs:block/haltepaaltje_ophalen", "guhs:block/haltepaaltje_afleveren", "guhs:block/haltepaaltje_ophalen_neer"):
        for yaw in (25, 325):
            blokken.append(wr.render(wr.model_quads(ref), yaw, -20, 300, margin=0.08))
    sheet = Image.new("RGBA", (300 * 4, 300 * ((len(blokken) + 3) // 4)), (236, 232, 240, 255))
    for i, t in enumerate(blokken):
        sheet.alpha_composite(t, ((i % 4) * 300, (i // 4) * 300))
    sheet.save(os.path.join(out, "bezorg_blokken_sheet.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
