"""
1.10.9 Guhwinter, slice hoentjes - the looks: GeckoLib models, animations and textures of the two hoentjes. Birds, not
guhs, but with the guh twist of the 3.0 birds (vogels_modellen, whose atlas and painters this reuses): the big glossy guh
eyes, a pink blush and two little round guh ears on top of the head.

  guhwinter_waterhoen    a round common moorhen: dark slate body with an olive-brown back and a white line along the
                         flank, a red frontal shield and a red bill with a yellow tip, a short cocked tail that is white
                         underneath with a black middle (the tail flick!), green legs with a red garter and long green toes.
                         On the dark head the guh eyes sit in a lighter patch, with a warm red ring (moorhens have red eyes).
  guhwinter_alpinehoen   a plump rock ptarmigan in winter white: a black "eyeliner" stripe from the bill to the eye, a small
                         red comb above each eye, a black-edged tail, a little black bill and big fluffy feathered feet.

Bones (both): root > body > head (DefaultedEntityGeoModel turns "head"), ear_left/ear_right, beak, wing_left/wing_right,
tail, leg_left/leg_right (+ the waterhoen's shield on the head, the alpine hoentje's comb_left/comb_right).
Every texture also has a `<name>_dicht.png` (eyes closed: blinking).

Animations: waterhoen idle / walk / zwem / roep (head bobbing all the time, the tail flicking); alpine hoentje idle / walk /
hop / roep.

  build(h)      writes geo/animations/textures, raises SystemExit on a problem (check)
  preview(out)  (python tools/features/guhwinter_hoentjes_modellen.py <dir>) renders both with wiki_renders
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

from features import vogels_modellen as vm
from features.vogels_modellen import (Atlas, bone, ear, eye, fluff, kf, loop, once, rounded, solid, vgradient, rows_of_feathers,
                                      write_geo, wave, PX, blush)

NAMEN = ("guhwinter_waterhoen", "guhwinter_alpinehoen")

# --- colours --------------------------------------------------------------------------------------------------------------
# waterhoen (common moorhen)
SLATE, SLATE_L, HOOFD = (62, 68, 90), (88, 96, 122), (46, 50, 68)
RUG, RUG_L, RUG_D = (98, 88, 62), (122, 110, 80), (72, 64, 46)
WIT, WIT_D = (248, 248, 250), (220, 222, 232)
ROOD, ROOD_D, GEEL = (222, 44, 46), (170, 28, 34), (252, 214, 64)
POOT, POOT_D = (178, 208, 74), (128, 160, 50)
OOR_IN = (240, 138, 170)
# alpine hoentje (rock ptarmigan, winter)
SNEEUW, SNEEUW_S, SNEEUW_L = (250, 250, 253), (222, 228, 242), (255, 255, 255)
ZWART, KAM = (36, 34, 44), (234, 52, 62)
OOR_IN_A = (255, 150, 182)


def cube(atlas, origin, size, paint, pivot=None, rotation=None, inflate=None):
    """vogels_modellen.cube plus an optional per-cube rotation around `pivot` (the splayed toes)."""
    c = vm.cube(atlas, origin, size, paint, inflate=inflate)
    if rotation:
        c["pivot"] = [round(p, 3) for p in pivot]
        c["rotation"] = rotation
    return c


# =====================================================================================================================
# painters (painter(w, h, face, rng, closed) -> (h, w, 4) float)
# =====================================================================================================================
def oog_gezicht(base, eye_y, eye_r, spread, iris, fur, patch=None, streep=None, blush_y=0.74):
    """The front of a hoentje's head: `base` with two glossy guh eyes and a blush (like vogels_modellen.face), plus:
    patch: a lighter disc behind each eye (so the eyes read on the moorhen's dark head);
    streep: (colour) the ptarmigan's black stripe from the middle (the bill) to each eye."""
    def p(w, h, f, rng, closed):
        a = base(w, h, f, rng, closed)
        r = eye_r * min(w, h)
        cy = h * eye_y
        for side in (-1, 1):
            cx = w / 2 - 0.5 + side * w * spread
            if patch is not None:
                for y in range(h):
                    for x in range(w):
                        if math.hypot(x - cx, y - cy) <= r * 1.35:
                            a[y, x, :3] = np.array(patch, np.float32) + rng.integers(-4, 5, 3)
            if streep is not None:
                # a slightly curved band from beside the eye down towards the bill, 1.5 eye-radii long
                for y in range(h):
                    for x in range(w):
                        t = (x - (w / 2 - 0.5)) * side / max(1e-3, (cx - (w / 2 - 0.5)) * side)   # 0 middle .. 1 the eye
                        if 0.15 <= t <= 1.2:
                            yc = cy + r * 0.35 + (1 - t) * r * 0.45
                            if abs(y - yc) <= max(0.9, r * 0.28):
                                a[y, x, :3] = streep
            eye(a, cx, cy, r, iris=iris, closed=closed, fur=fur)
        for side in (-1, 1):
            blush(a, w / 2 - 0.5 + side * w * (spread + 0.1), h * blush_y, w * 0.12, h * 0.07)
        return a
    return p


def flank(base, stripe, y0=0.74, y1=0.86):
    """The moorhen's wing side: feather rows with the white line along the flank near the bottom edge."""
    def p(w, h, f, rng, closed):
        a = base(w, h, f, rng, closed)
        if f in ("east", "west"):
            for y in range(h):
                t = (y + 0.5) / h
                for x in range(w):
                    wob = 0.025 * math.sin(x * 0.9)
                    if y0 + wob <= t <= y1 + wob:
                        a[y, x, :3] = np.array(stripe, np.float32) + rng.integers(-4, 5, 3)
        return a
    return p


def staart_wit(w_col, zwart):
    """The moorhen's undertail: white with a black middle (what you see when it flicks)."""
    def p(w, h, f, rng, closed):
        a = solid(w_col, 4)(w, h, f, rng, closed)
        if f in ("south", "down", "up"):
            m0, m1 = w * 0.36, w * 0.64
            for x in range(w):
                if m0 <= x + 0.5 <= m1:
                    a[:, x, :3] = np.array(zwart, np.float32) + rng.integers(-4, 5, (h, 1))
        if f in ("east", "west", "north"):
            a[...] = solid(zwart, 5)(w, h, f, rng, closed)
            a[-max(1, h // 3):, :, :3] = np.array(w_col, np.float32)
        return a
    return p


def snavel(kleur, punt, frac=0.38):
    """A bill whose tip (the front, `frac` of its length) is another colour: north is the very tip."""
    def p(w, h, f, rng, closed):
        a = solid(kleur, 4)(w, h, f, rng, closed)
        if f == "north":
            a[...] = solid(punt, 4)(w, h, f, rng, closed)
        elif f in ("east", "west", "up", "down"):
            n = w if f in ("east", "west") else h
            k = max(1, int(round(n * frac)))
            for i in range(k):
                if f == "east":
                    a[:, i, :3] = punt                  # (the east face runs back-to-front: its tip is on the left, see vogels.tipped)
                elif f == "west":
                    a[:, n - 1 - i, :3] = punt
                else:
                    a[i if f == "up" else n - 1 - i, :, :3] = punt
        return a
    return p


def toe(col, dark):
    def p(w, h, f, rng, closed):
        a = solid(col, 4)(w, h, f, rng, closed)
        if f in ("east", "west", "up"):
            a[:, :, :3] = a[:, :, :3] * 0.96
        a[-1:, :, :3] = dark
        return a
    return p


def pluimvoet(col, light, klauw):
    """A feathered ptarmigan foot: soft white fluff with three tiny dark claws peeking out at the front bottom."""
    def p(w, h, f, rng, closed):
        a = fluff(col, light, 5, rmax=2)(w, h, f, rng, closed)
        # soft feather tips along the bottom edge (every other texel a little lighter/darker)
        for x in range(w):
            if x % 3 == 0:
                a[-1, x, :3] = np.array(col, np.float32) * 0.9
        if f == "north":
            for k in range(3):
                cx = int(round((k + 1) * w / 4))
                for y in (h - 2, h - 1):
                    if 0 <= cx < w:
                        a[y, cx, :3] = klauw
        return a
    return p


def kam(col):
    def p(w, h, f, rng, closed):
        a = solid(col, 6)(w, h, f, rng, closed)
        if f in ("up", "north", "east", "west"):
            for x in range(w):
                if x % 3 == 1:
                    a[0, x, :3] = np.minimum(255, a[0, x, :3] + 30)     # a little bumpy top edge
        return a
    return p


def zwartrand_staart(wit, zwart):
    """The ptarmigan's tail: white coverts on top, black outer feathers that show at the sides and the back."""
    def p(w, h, f, rng, closed):
        a = solid(wit, 4)(w, h, f, rng, closed)
        if f in ("south", "east", "west", "down"):
            k = max(1, int(round(h * 0.55)))
            a[h - k:, :, :3] = np.array(zwart, np.float32) + rng.integers(-4, 5, (k, w, 1))
            if f == "south":
                a[h - 1:, ::3, :3] = np.array(wit, np.float32)                # thin white tips
        return a
    return p


# =====================================================================================================================
# the waterhoentje
# =====================================================================================================================
def waterhoen(h):
    at = Atlas("guhwinter_waterhoen", 48, 48, seed=11009001)
    body = {"*": flank(rows_of_feathers(SLATE, (48, 52, 70), SLATE_L), WIT), "up": rows_of_feathers(RUG, RUG_D, RUG_L),
            "down": fluff(SLATE_L, (110, 118, 142)), "north": fluff(SLATE, SLATE_L), "south": fluff(SLATE, SLATE_L)}
    gezicht = oog_gezicht(fluff(HOOFD, (60, 66, 86), n=3), eye_y=0.44, eye_r=0.19, spread=0.26,
                          iris=((255, 140, 120), (196, 40, 52)), fur=HOOFD, patch=(102, 110, 138), blush_y=0.76)
    head = {"*": fluff(HOOFD, (60, 66, 86)), "up": fluff(HOOFD, (60, 66, 86)), "north": gezicht}
    wing = {"*": flank(rows_of_feathers(RUG, RUG_D, RUG_L), WIT, 0.8, 0.92), "up": solid(RUG), "down": solid(SLATE_L)}
    poot = toe(POOT, POOT_D)
    bones = [
        bone("root", None, [0, 0, 0]),
        bone("body", "root", [0, 3.5, 0], [cube(at, [-2.75, 2, -3], [5.5, 4.6, 6.6], body),
                                           cube(at, [-2.25, 6.6, -2.2], [4.5, 0.5, 4.6], {"*": solid(RUG), "up": rows_of_feathers(RUG, RUG_D, RUG_L)})]),
        bone("head", "body", [0, 6.2, -2.4], [cube(at, [-2.1, 5.8, -6.0], [4.2, 4.0, 3.8], head)]),
        # the red frontal shield up the forehead, between the eyes
        bone("shield", "head", [0, 8.2, -6.0], [cube(at, [-0.55, 7.1, -6.3], [1.1, 1.6, 0.4], {"*": solid(ROOD, 5), "south": solid(ROOD_D)})]),
        bone("ear_left", "head", [1.4, 9.6, -4.4], [cube(at, [0.7, 9.5, -4.6], [1.5, 1.5, 0.5], {"*": ear(HOOFD, OOR_IN)})]),
        bone("ear_right", "head", [-1.4, 9.6, -4.4], [cube(at, [-2.2, 9.5, -4.6], [1.5, 1.5, 0.5], {"*": ear(HOOFD, OOR_IN)})]),
        bone("beak", "head", [0, 6.8, -6.0], [cube(at, [-0.6, 6.3, -7.5], [1.2, 0.9, 1.5], snavel(ROOD, GEEL))]),
        bone("wing_left", "body", [2.75, 6.0, -1.8], [cube(at, [2.55, 3.1, -2.2], [0.5, 3.0, 5.0], wing)]),
        bone("wing_right", "body", [-2.75, 6.0, -1.8], [cube(at, [-3.05, 3.1, -2.2], [0.5, 3.0, 5.0], wing)]),
        bone("tail", "body", [0, 5.2, 3.4], [cube(at, [-1.6, 4.5, 3.4], [3.2, 1.5, 2.0], {"*": rounded(staart_wit(WIT, (30, 30, 38)), 0.25)})],
             rotation=[-32, 0, 0]),
    ]
    for side, sx in (("left", 1), ("right", -1)):
        x = 1.1 * sx
        bones.append(bone(f"leg_{side}", "body", [x, 2.3, 0.2], [
            cube(at, [x - 0.25, 0.25, -0.05], [0.5, 2.1, 0.5], poot),
            cube(at, [x - 0.35, 1.9, -0.15], [0.7, 0.45, 0.7], solid(ROOD, 5)),                     # the red garter
            cube(at, [x - 0.15, 0, -2.75], [0.3, 0.25, 2.9], poot),                                 # the long middle toe
            cube(at, [x - 0.15, 0, -2.25], [0.3, 0.25, 2.4], poot, pivot=[x, 0, 0.1], rotation=[0, 28, 0]),
            cube(at, [x - 0.15, 0, -2.25], [0.3, 0.25, 2.4], poot, pivot=[x, 0, 0.1], rotation=[0, -28, 0]),
            cube(at, [x - 0.15, 0, 0.1], [0.3, 0.25, 1.1], poot)]))                                 # the hind toe
    write_geo(h, "guhwinter_waterhoen", at, bones, 1.0, 0.9)
    at.save(h)
    return bones


def waterhoen_anims(h):
    # the head bob of a walking moorhen: forward with every step, a tiny dip; the tail flicks with it
    bob = lambda L: kf([(0, (0, 0, -0.7)), (L * 0.25, (0, -0.2, 0.5)), (L * 0.5, (0, 0, -0.7)), (L * 0.75, (0, -0.2, 0.5)), (L, (0, 0, -0.7))])  # noqa: E731
    flick = lambda L: kf([(0, (0, 0, 0)), (L * 0.2, (-22, 0, 0)), (L * 0.4, (0, 0, 0)), (L * 0.7, (-22, 0, 0)), (L * 0.9, (0, 0, 0)), (L, (0, 0, 0))])  # noqa: E731
    anims = {
        "idle": loop(3.2, {
            "body": {"scale": kf([(0, (1, 1, 1)), (1.6, (1.02, 1.03, 1.02)), (3.2, (1, 1, 1))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.8, (0, 18, 0)), (1.0, (0, 18, 0)), (1.2, (0, 0, 0)), (2.0, (0, -14, 6)), (2.3, (0, -14, 6)),
                                     (2.6, (0, 0, 0)), (3.2, (0, 0, 0))])},
            "tail": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, 0)), (1.3, (-24, 0, 0)), (1.4, (0, 0, 0)), (1.5, (-24, 0, 0)), (1.6, (0, 0, 0)),
                                     (2.7, (0, 0, 0)), (2.8, (-20, 0, 0)), (2.9, (0, 0, 0)), (3.2, (0, 0, 0))])},
            "ear_left": {"rotation": kf([(0, (0, 0, 0)), (2.2, (0, 0, 0)), (2.3, (0, 0, -18)), (2.45, (0, 0, 0)), (3.2, (0, 0, 0))])},
            "ear_right": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 0, 0)), (0.6, (0, 0, 18)), (0.75, (0, 0, 0)), (3.2, (0, 0, 0))])}}),
        "walk": loop(0.6, {
            "head": {"position": bob(0.6)},
            "tail": {"rotation": flick(0.6)},
            "body": {"rotation": kf([(0, (0, 0, 4)), (0.3, (0, 0, -4)), (0.6, (0, 0, 4))])},
            "leg_left": {"rotation": kf([(0, (32, 0, 0)), (0.3, (-28, 0, 0)), (0.6, (32, 0, 0))])},
            "leg_right": {"rotation": kf([(0, (-28, 0, 0)), (0.3, (32, 0, 0)), (0.6, (-28, 0, 0))])}}),
        "zwem": loop(0.8, {
            "head": {"position": bob(0.8)},
            "tail": {"rotation": flick(0.8)},
            "body": {"position": kf([(0, (0, 0, 0)), (0.4, (0, 0.3, 0)), (0.8, (0, 0, 0))]),
                     "rotation": kf([(0, (0, 0, 2)), (0.4, (0, 0, -2)), (0.8, (0, 0, 2))])},
            "leg_left": {"rotation": kf([(0, (55, 0, 0)), (0.4, (-15, 0, 0)), (0.8, (55, 0, 0))])},
            "leg_right": {"rotation": kf([(0, (-15, 0, 0)), (0.4, (55, 0, 0)), (0.8, (-15, 0, 0))])}}),
        # "krrr-uk!": the head goes up twice, the shield and the tail with it, a little puff
        "roep": once(0.9, {
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.1, (-26, 0, 0)), (0.3, (-8, 0, 0)), (0.42, (-26, 0, 0)), (0.75, (0, 0, 0))])},
            "body": {"scale": kf([(0, (1, 1, 1)), (0.1, (1.05, 1.06, 1.05)), (0.75, (1, 1, 1))])},
            "tail": {"rotation": kf([(0, (0, 0, 0)), (0.1, (-30, 0, 0)), (0.3, (0, 0, 0)), (0.42, (-30, 0, 0)), (0.75, (0, 0, 0))])},
            "wing_left": {"rotation": kf([(0, (0, 0, 0)), (0.1, (0, 0, -14)), (0.4, (0, 0, 0))])},
            "wing_right": {"rotation": kf([(0, (0, 0, 0)), (0.1, (0, 0, 14)), (0.4, (0, 0, 0))])}}),
    }
    h.w(f"{h.A}/geckolib/animations/entity/guhwinter_waterhoen.animation.json", {"format_version": "1.8.0", "animations": anims})
    return set(anims)


# =====================================================================================================================
# the alpine hoentje
# =====================================================================================================================
def alpinehoen(h):
    at = Atlas("guhwinter_alpinehoen", 48, 48, seed=11009002)
    sneeuw = fluff(SNEEUW, SNEEUW_L, 4)
    body = {"*": vgradient(SNEEUW, SNEEUW_S, 0.78, 0.3, var=4, tufts=SNEEUW_L), "up": sneeuw, "down": fluff(SNEEUW_S, SNEEUW, 4),
            "north": fluff(SNEEUW, SNEEUW_L, 4)}
    gezicht = oog_gezicht(fluff(SNEEUW, SNEEUW_L, 4, n=3), eye_y=0.45, eye_r=0.19, spread=0.27,
                          iris=((140, 220, 246), (40, 110, 220)), fur=SNEEUW, streep=ZWART, blush_y=0.78)
    head = {"*": sneeuw, "north": gezicht}
    wing = {"*": rows_of_feathers(SNEEUW, (214, 220, 234), SNEEUW_L), "up": sneeuw, "down": solid(SNEEUW_S)}
    voet = pluimvoet(SNEEUW, SNEEUW_L, (60, 56, 64))
    bones = [
        bone("root", None, [0, 0, 0]),
        bone("body", "root", [0, 3.4, 0], [cube(at, [-3, 1.8, -3.2], [6, 5.2, 6.6], body),
                                           cube(at, [-2.5, 7.0, -2.4], [5, 0.5, 5], {"*": sneeuw})]),
        bone("head", "body", [0, 6.6, -2.6], [cube(at, [-2.2, 6.2, -6.2], [4.4, 4.0, 4.0], head)]),
        bone("ear_left", "head", [1.45, 10.0, -4.6], [cube(at, [0.7, 9.9, -4.8], [1.5, 1.5, 0.5], {"*": ear(SNEEUW, OOR_IN_A)})]),
        bone("ear_right", "head", [-1.45, 10.0, -4.6], [cube(at, [-2.2, 9.9, -4.8], [1.5, 1.5, 0.5], {"*": ear(SNEEUW, OOR_IN_A)})]),
        bone("beak", "head", [0, 7.4, -6.2], [cube(at, [-0.5, 7.0, -7.0], [1.0, 0.8, 0.9], {"*": solid(ZWART, 4)})]),
        bone("wing_left", "body", [3.0, 6.2, -1.8], [cube(at, [2.75, 2.9, -2.2], [0.5, 3.2, 5.0], wing)]),
        bone("wing_right", "body", [-3.0, 6.2, -1.8], [cube(at, [-3.25, 2.9, -2.2], [0.5, 3.2, 5.0], wing)]),
        bone("tail", "body", [0, 4.4, 3.4], [cube(at, [-2.0, 3.7, 3.3], [4.0, 1.4, 1.8], {"*": rounded(zwartrand_staart(SNEEUW, ZWART), 0.2)})],
             rotation=[-16, 0, 0]),
    ]
    # the small red combs above the eyes (the eyes sit at x +-1.19, y 8.4, radius 0.76 on the face at z -6.2)
    for side, sx in (("left", 1), ("right", -1)):
        x0 = 0.45 if sx > 0 else -1.95
        bones.append(bone(f"comb_{side}", "head", [sx * 1.2, 9.25, -6.0], [cube(at, [x0, 9.15, -6.4], [1.5, 0.55, 1.4], kam(KAM))]))
    for side, sx in (("left", 1), ("right", -1)):
        x = 1.35 * sx
        bones.append(bone(f"leg_{side}", "body", [x, 2.0, 0], [
            cube(at, [x - 0.95, 0, -1.75], [1.9, 1.85, 2.5], voet)]))            # fluffy snowshoes
    write_geo(h, "guhwinter_alpinehoen", at, bones, 1.0, 0.9)
    at.save(h)
    return bones


def alpinehoen_anims(h):
    anims = {
        "idle": loop(3.0, {
            "body": {"scale": kf([(0, (1, 1, 1)), (1.4, (1.03, 1.03, 1.03)), (2.2, (1.0, 1.0, 1.0)), (2.3, (1.08, 0.96, 1.08)),
                                  (2.4, (0.98, 1.03, 0.98)), (2.5, (1.05, 0.98, 1.05)), (2.6, (1, 1, 1)), (3.0, (1, 1, 1))])},   # (a fluff shake)
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.9, (0, 0, 0)), (1.05, (0, 0, 16)), (1.7, (0, 0, 16)), (1.85, (0, 0, 0)), (3.0, (0, 0, 0))])},
            "tail": {"rotation": kf([(0, (0, 0, 0)), (2.3, (0, 0, 0)), (2.4, (-14, 0, 0)), (2.6, (0, 0, 0)), (3.0, (0, 0, 0))])},
            "ear_left": {"rotation": kf([(0, (0, 0, 0)), (2.3, (0, 0, -20)), (2.5, (0, 0, 0)), (3.0, (0, 0, 0))])},
            "ear_right": {"rotation": kf([(0, (0, 0, 0)), (2.3, (0, 0, 20)), (2.5, (0, 0, 0)), (3.0, (0, 0, 0))])}}),
        "walk": loop(0.7, {
            "body": {"rotation": kf([(0, (0, 0, 6)), (0.35, (0, 0, -6)), (0.7, (0, 0, 6))])},
            "head": {"rotation": kf([(0, (0, 0, -4)), (0.35, (0, 0, 4)), (0.7, (0, 0, -4))])},
            "leg_left": {"rotation": kf([(0, (24, 0, 0)), (0.35, (-20, 0, 0)), (0.7, (24, 0, 0))])},
            "leg_right": {"rotation": kf([(0, (-20, 0, 0)), (0.35, (24, 0, 0)), (0.7, (-20, 0, 0))])}}),
        # in the air: the snowshoes tucked together, the wings flick out, squash and stretch
        "hop": loop(0.5, {
            "body": {"scale": kf([(0, (0.96, 1.06, 0.96)), (0.25, (1.04, 0.97, 1.04)), (0.5, (0.96, 1.06, 0.96))]),
                     "rotation": kf([(0, (-6, 0, 0)), (0.25, (4, 0, 0)), (0.5, (-6, 0, 0))])},
            "leg_left": {"rotation": kf([(0, (30, 0, 0)), (0.25, (10, 0, 0)), (0.5, (30, 0, 0))])},
            "leg_right": {"rotation": kf([(0, (30, 0, 0)), (0.25, (10, 0, 0)), (0.5, (30, 0, 0))])},
            "wing_left": {"rotation": kf([(0, (0, 0, -24)), (0.25, (0, 0, -6)), (0.5, (0, 0, -24))])},
            "wing_right": {"rotation": kf([(0, (0, 0, 24)), (0.25, (0, 0, 6)), (0.5, (0, 0, 24))])},
            "tail": {"rotation": kf([(0, (-18, 0, 0)), (0.25, (0, 0, 0)), (0.5, (-18, 0, 0))])},
            "head": {"rotation": kf([(0, (8, 0, 0)), (0.25, (-6, 0, 0)), (0.5, (8, 0, 0))])}}),
        # "arr-arr-ka-ka-ka": the head pumps, the combs flush up a little
        "roep": once(1.1, {
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.12, (-20, 0, 0)), (0.3, (-6, 0, 0)), (0.45, (-20, 0, 0)), (0.6, (-6, 0, 0)),
                                     (0.75, (-20, 0, 0)), (1.0, (0, 0, 0))])},
            "comb_left": {"scale": kf([(0, (1, 1, 1)), (0.12, (1.1, 1.5, 1.1)), (0.9, (1.1, 1.5, 1.1)), (1.1, (1, 1, 1))])},
            "comb_right": {"scale": kf([(0, (1, 1, 1)), (0.12, (1.1, 1.5, 1.1)), (0.9, (1.1, 1.5, 1.1)), (1.1, (1, 1, 1))])},
            "body": {"scale": kf([(0, (1, 1, 1)), (0.12, (1.05, 1.05, 1.05)), (1.0, (1, 1, 1))])}}),
    }
    h.w(f"{h.A}/geckolib/animations/entity/guhwinter_alpinehoen.animation.json", {"format_version": "1.8.0", "animations": anims})
    return set(anims)


# =====================================================================================================================
# build + self-check
# =====================================================================================================================
NEEDED = ("root", "body", "head", "ear_left", "ear_right", "beak", "wing_left", "wing_right", "tail", "leg_left", "leg_right")
ANIMS = {"guhwinter_waterhoen": {"idle", "walk", "zwem", "roep"}, "guhwinter_alpinehoen": {"idle", "walk", "hop", "roep"}}


def build(h):
    bones = {"guhwinter_waterhoen": waterhoen(h), "guhwinter_alpinehoen": alpinehoen(h)}
    anims = {"guhwinter_waterhoen": waterhoen_anims(h), "guhwinter_alpinehoen": alpinehoen_anims(h)}
    problems = check(h, bones, anims)
    if problems:
        raise SystemExit("guhwinter_hoentjes models:\n  " + "\n  ".join(problems))


def check(h, bones, anims):
    p = []
    for name, bs in bones.items():
        names = {b["name"] for b in bs}
        p += [f"{name}: no bone {n}" for n in NEEDED if n not in names]
        if len(names) != len(bs):
            p.append(f"{name}: duplicate bone names")
        for b in bs:
            if b.get("parent") and b["parent"] not in names:
                p.append(f"{name}: {b['name']} hangs on a missing {b['parent']}")
        top = max(c["origin"][1] + c["size"][1] for b in bs for c in b["cubes"])
        if top > 16:
            p.append(f"{name}: {top} units tall")
        with open(os.path.join(h.A, "geckolib", "animations", "entity", f"{name}.animation.json"), encoding="utf-8") as f:
            data = json.load(f)["animations"]
        for an, a in data.items():
            for bn in a["bones"]:
                if bn not in names:
                    p.append(f"{name}: animation {an} moves a missing bone {bn}")
        p += [f"{name}: no animation {n}" for n in ANIMS[name] - anims[name]]
        # the guh eyes on the face: dark pupils and white shines; the closed texture differs there
        head = [b for b in bs if b["name"] == "head"][0]["cubes"][0]["uv"]["north"]
        (u, v), (w, hh) = head["uv"], head["uv_size"]
        faces = []
        for tex in ("", "_dicht"):
            a = np.asarray(Image.open(os.path.join(h.TEX, "entity", f"{name}{tex}.png")).convert("RGBA")).astype(np.int32)
            faces.append(a[int(v * PX):int((v + hh) * PX), int(u * PX):int((u + w) * PX)])
        dark = int((faces[0][..., :3].max(-1) < 40).sum())
        white = int((faces[0][..., :3].min(-1) > 245).sum())
        if dark < 20 or white < 4:
            p.append(f"{name}: no glossy guh eyes on the face (dark {dark}, shine {white})")
        if int((np.abs(faces[0] - faces[1]).sum(-1) > 30).sum()) < 40:
            p.append(f"{name}: the closed eyes (_dicht) look open")
    return p


def preview(out):
    """Renders both (front and side) to out/ with wiki_renders (for looking at them; not part of the build)."""
    here = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(0, os.path.dirname(here))
    import wiki_renders as wr
    A = os.path.join("src", "main", "resources", "assets", "guhs")
    os.makedirs(out, exist_ok=True)
    tiles = []
    for name in NAMEN:
        q = wr.geo_quads(os.path.join(A, "geckolib", "models", "entity", f"{name}.geo.json"), f"guhs:entity/{name}", show_only_variant_bones=("",))
        tile = Image.new("RGBA", (768, 256), (120, 170, 210, 255))
        for i, (yaw, pitch) in enumerate(((20, -18), (70, -20), (160, -22))):
            tile.alpha_composite(wr.render(q, yaw, pitch, 256, margin=0.06), (256 * i, 0))
        tile.save(os.path.join(out, f"{name}.png"))
        tiles.append(tile)
    sheet = Image.new("RGBA", (768, 256 * len(tiles)), (120, 170, 210, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, (0, 256 * i))
    sheet.save(os.path.join(out, "_hoentjes.png"))
    print("preview:", out)


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
