"""The emotes feature (2.5): guhs wave, dance, sleep (and snore), do a VAHOEG jump, roll over, munch and act shy.

  animations()  the seven emote animations (GeckoLib, merged into guh.animation.json by make_guh_variants.add_animations;
                they can be polished in Blockbench later). They all loop: the game decides how long a guh keeps going.
  build(h)      particle textures (zzz, "VAHOEG!"), lang, hidden advancements.
  ftb(fq)       the emote quests (row y=50).

Bone notes: rotations/positions are Blockbench (bedrock) values. The renderer turns the "head" bone to where the guh looks
(its x/y rotation is overwritten), so head nods are done in the game (the look control) and the animations only tilt it (z).
"""
import math

import numpy as np
from PIL import Image, ImageDraw

# the emote ids (same order as the Emote enum in feature/emotes/Emote.java)
EMOTES = ["zwaaien", "dansen", "slapen", "vahoeg", "rollen", "smakken", "verlegen", "gapen", "zingen", "knuffelen",
          # 2.10 (Lieve vadsjes van elkaar)
          "hartjes", "knuffeldansje", "bff_knuffel", "verdrietje",
          # 3.0 (Guhverhalen): only the 626-guh; its animation comes from tools/features/guhwaii.py animations(), not from here
          "ukelele"]


def _kf(pairs):
    return {str(float(t)): v for t, v in pairs}


def _wave():
    t = [0.0, 0.3, 0.6, 0.9, 1.2]
    return {"loop": True, "animation_length": 1.2, "bones": {
        # the right front paw up next to the cheek, waving
        "leg_front_right": {"position": _kf([(0, [-5, 7, -8])]), "scale": _kf([(0, [1, 1.7, 1])]),
                            "rotation": _kf(zip(t, [[0, 0, -30], [0, 0, 20], [0, 0, -30], [0, 0, 20], [0, 0, -30]]))},
        "root": {"rotation": _kf(zip(t, [[0, 0, 3], [0, 0, 5], [0, 0, 3], [0, 0, 5], [0, 0, 3]]))},
        "body": {"scale": _kf([(0, [1.02, 1.02, 1]), (0.6, [1.04, 1.0, 1]), (1.2, [1.02, 1.02, 1])])},
        "head": {"rotation": _kf([(0, [0, 0, -8]), (0.6, [0, 0, -12]), (1.2, [0, 0, -8])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 0]), (0.3, [0, 0, -12]), (0.6, [0, 0, 0]), (0.9, [0, 0, -12]), (1.2, [0, 0, 0])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, 0]), (0.3, [0, 0, 12]), (0.6, [0, 0, 0]), (0.9, [0, 0, 12]), (1.2, [0, 0, 0])])},
        "tail": {"rotation": _kf([(0, [0, -25, 0]), (0.3, [0, 25, 0]), (0.6, [0, -25, 0]), (0.9, [0, 25, 0]), (1.2, [0, -25, 0])])},
    }}


def _dance():
    t = [0.0, 0.2, 0.4, 0.6, 0.8]
    up, down = [0, 2.5, 0], [0, 0, 0]
    paw_up, paw_down = [-45, 0, 0], [0, 0, 0]
    return {"loop": True, "animation_length": 0.8, "bones": {
        "root": {"position": _kf(zip(t, [down, up, down, up, down])),
                 "rotation": _kf(zip(t, [[0, -18, 0], [0, 0, 6], [0, 18, 0], [0, 0, -6], [0, -18, 0]]))},
        "body": {"scale": _kf(zip(t, [[1.1, 0.88, 1.05], [0.96, 1.08, 1], [1.1, 0.88, 1.05], [0.96, 1.08, 1], [1.1, 0.88, 1.05]]))},
        "head": {"rotation": _kf(zip(t, [[0, 0, 10], [0, 0, 0], [0, 0, -10], [0, 0, 0], [0, 0, 10]]))},
        "leg_front_left": {"position": _kf(zip(t, [[0, 0, 0], [0, 3, -1], [0, 0, 0], [0, 0, 0], [0, 0, 0]])),
                           "rotation": _kf(zip(t, [paw_down, paw_up, paw_down, paw_down, paw_down]))},
        "leg_front_right": {"position": _kf(zip(t, [[0, 0, 0], [0, 0, 0], [0, 0, 0], [0, 3, -1], [0, 0, 0]])),
                            "rotation": _kf(zip(t, [paw_down, paw_down, paw_down, paw_up, paw_down]))},
        "ear_left": {"rotation": _kf(zip(t, [[0, 0, 25], [0, 0, -10], [0, 0, 25], [0, 0, -10], [0, 0, 25]]))},
        "ear_right": {"rotation": _kf(zip(t, [[0, 0, -25], [0, 0, 10], [0, 0, -25], [0, 0, 10], [0, 0, -25]]))},
        "tail": {"rotation": _kf(zip(t, [[0, -35, 0], [15, 0, 0], [0, 35, 0], [15, 0, 0], [0, -35, 0]]))},
    }}


def _sleep():
    return {"loop": True, "animation_length": 3.0, "bones": {
        # curled up like sitting, breathing slowly, head tilted, ears flopped
        "root": {"position": _kf([(0, [0, -1, 0])])},
        "body": {"scale": _kf([(0, [1.06, 0.88, 1.02]), (1.5, [1.11, 0.95, 1.05]), (3.0, [1.06, 0.88, 1.02])])},
        "head": {"rotation": _kf([(0, [0, 0, 14]), (1.5, [0, 0, 17]), (3.0, [0, 0, 14])]),
                 "scale": _kf([(0, [1, 1, 1]), (1.5, [1.03, 1.02, 1.02]), (3.0, [1, 1, 1])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 38])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, -38])])},
        "leg_front_left": {"position": _kf([(0, [0, 0.6, 1])])},
        "leg_front_right": {"position": _kf([(0, [0, 0.6, 1])])},
        "leg_back_left": {"position": _kf([(0, [0, 0.6, -1])])},
        "leg_back_right": {"position": _kf([(0, [0, 0.6, -1])])},
        "tail": {"rotation": _kf([(0, [0, 50, 0])])},
    }}


def _vahoeg():
    return {"loop": True, "animation_length": 1.1, "bones": {
        # crouch, jump (with a full spin), paws spread wide, land with a squish
        "root": {"position": _kf([(0, [0, 0, 0]), (0.15, [0, -1, 0]), (0.3, [0, 9, 0]), (0.45, [0, 13, 0]),
                                  (0.6, [0, 9, 0]), (0.75, [0, 0, 0]), (0.85, [0, 1, 0]), (0.95, [0, 0, 0]), (1.1, [0, 0, 0])]),
                 "rotation": _kf([(0, [0, 0, 0]), (0.2, [0, 0, 0]), (0.7, [0, 360, 0]), (1.1, [0, 360, 0])])},
        "body": {"scale": _kf([(0, [1, 1, 1]), (0.15, [1.18, 0.78, 1.12]), (0.3, [0.92, 1.12, 0.98]), (0.6, [1, 1, 1]),
                               (0.75, [1.2, 0.8, 1.12]), (0.9, [0.97, 1.04, 1]), (1.1, [1, 1, 1])])},
        "head": {"rotation": _kf([(0, [0, 0, 0]), (0.45, [0, 0, 10]), (0.75, [0, 0, -6]), (1.1, [0, 0, 0])])},
        "leg_front_left": {"rotation": _kf([(0, [0, 0, 0]), (0.2, [0, 0, 0]), (0.4, [-60, 0, -30]), (0.65, [-60, 0, -30]), (0.75, [0, 0, 0])])},
        "leg_front_right": {"rotation": _kf([(0, [0, 0, 0]), (0.2, [0, 0, 0]), (0.4, [-60, 0, 30]), (0.65, [-60, 0, 30]), (0.75, [0, 0, 0])])},
        "leg_back_left": {"rotation": _kf([(0, [0, 0, 0]), (0.2, [0, 0, 0]), (0.4, [55, 0, -25]), (0.65, [55, 0, -25]), (0.75, [0, 0, 0])])},
        "leg_back_right": {"rotation": _kf([(0, [0, 0, 0]), (0.2, [0, 0, 0]), (0.4, [55, 0, 25]), (0.65, [55, 0, 25]), (0.75, [0, 0, 0])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 0]), (0.3, [0, 0, -25]), (0.6, [0, 0, 30]), (0.8, [0, 0, -10]), (1.1, [0, 0, 0])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, 0]), (0.3, [0, 0, 25]), (0.6, [0, 0, -30]), (0.8, [0, 0, 10]), (1.1, [0, 0, 0])])},
        "tail": {"rotation": _kf([(0, [0, 0, 0]), (0.4, [45, 0, 0]), (0.75, [-10, 0, 0]), (1.1, [0, 0, 0])])},
    }}


def _roll():
    wig = [(0.75, [-25, 0, 0]), (0.9, [20, 0, 0]), (1.05, [-25, 0, 0]), (1.2, [20, 0, 0]), (1.35, [0, 0, 0])]
    wig2 = [(t, [-v[0], 0, 0]) for t, v in wig]
    legs = lambda w: _kf([(0, [0, 0, 0]), (0.6, [0, 0, 0])] + w + [(2.0, [0, 0, 0])])
    return {"loop": True, "animation_length": 2.0, "bones": {
        # over the side onto its back, a happy wiggle with the paws in the air, and over the other side back up
        "root": {"rotation": _kf([(0, [0, 0, 0]), (0.35, [0, 0, 90]), (0.6, [0, 0, 180]), (1.4, [0, 0, 180]),
                                  (1.65, [0, 0, 270]), (2.0, [0, 0, 360])]),
                 "position": _kf([(0, [0, 0, 0]), (0.35, [0, 8, 0]), (0.6, [0, 15, 0]), (1.4, [0, 15, 0]),
                                  (1.65, [0, 8, 0]), (2.0, [0, 0, 0])])},
        "body": {"scale": _kf([(0, [1, 1, 1]), (0.6, [1.05, 0.95, 1.02]), (0.9, [1.08, 0.92, 1.03]), (1.2, [1.05, 0.95, 1.02]), (2.0, [1, 1, 1])])},
        "leg_front_left": {"rotation": legs(wig)},
        "leg_front_right": {"rotation": legs(wig2)},
        "leg_back_left": {"rotation": legs(wig2)},
        "leg_back_right": {"rotation": legs(wig)},
        "tail": {"rotation": _kf([(0, [0, 0, 0]), (0.8, [0, -30, 0]), (1.1, [0, 30, 0]), (1.4, [0, 0, 0])])},
        # ears folded back while rolling (they would poke through the ground)
        "ear_left": {"rotation": _kf([(0, [0, 0, 0]), (0.25, [0, -65, 0]), (1.75, [0, -65, 0]), (2.0, [0, 0, 0])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, 0]), (0.25, [0, 65, 0]), (1.75, [0, 65, 0]), (2.0, [0, 0, 0])])},
    }}


def _munch():
    t = [0.0, 0.15, 0.3, 0.45, 0.6]
    return {"loop": True, "animation_length": 0.6, "bones": {
        # both front paws up at the mouth, cheeks going (the head squishes), ears wiggling
        "leg_front_left": {"position": _kf(zip(t, [[-3, 3.5, -12], [-3, 4.5, -12], [-3, 3.5, -12], [-3, 4.5, -12], [-3, 3.5, -12]])),
                           "scale": _kf([(0, [1.1, 1.3, 1.1])])},
        "leg_front_right": {"position": _kf(zip(t, [[3, 4.5, -12], [3, 3.5, -12], [3, 4.5, -12], [3, 3.5, -12], [3, 4.5, -12]])),
                            "scale": _kf([(0, [1.1, 1.3, 1.1])])},
        "head": {"scale": _kf(zip(t, [[1, 1, 1], [1.07, 0.95, 1.02], [1, 1.02, 1], [1.07, 0.95, 1.02], [1, 1, 1]])),
                 "rotation": _kf(zip(t, [[0, 0, 0], [0, 0, 3], [0, 0, 0], [0, 0, -3], [0, 0, 0]]))},
        "body": {"scale": _kf([(0, [1.04, 0.96, 1.02]), (0.3, [1.06, 0.95, 1.03]), (0.6, [1.04, 0.96, 1.02])])},
        "root": {"position": _kf([(0, [0, -0.3, 0])])},
        "ear_left": {"rotation": _kf(zip(t, [[0, 0, 0], [0, 0, 8], [0, 0, 0], [0, 0, 8], [0, 0, 0]]))},
        "ear_right": {"rotation": _kf(zip(t, [[0, 0, 0], [0, 0, -8], [0, 0, 0], [0, 0, -8], [0, 0, 0]]))},
        "tail": {"rotation": _kf([(0, [0, -12, 0]), (0.3, [0, 12, 0]), (0.6, [0, -12, 0])])},
    }}


def _shy():
    return {"loop": True, "animation_length": 2.4, "bones": {
        # paws in front of the eyes, a bit small, swaying; now and then it peeks over its right paw
        "leg_front_left": {"position": _kf([(0, [-1.5, 9, -10.5])]), "scale": _kf([(0, [1.15, 1.6, 1.1])])},
        "leg_front_right": {"position": _kf([(0, [1.5, 9, -10.5]), (1.0, [1.5, 9, -10.5]), (1.2, [1.5, 5.5, -10.5]),
                                             (1.6, [1.5, 5.5, -10.5]), (1.8, [1.5, 9, -10.5]), (2.4, [1.5, 9, -10.5])]),
                            "scale": _kf([(0, [1.15, 1.6, 1.1])])},
        "root": {"rotation": _kf([(0, [0, -8, 0]), (1.2, [0, 8, 0]), (2.4, [0, -8, 0])])},
        "body": {"scale": _kf([(0, [0.95, 0.92, 0.95]), (1.2, [0.96, 0.94, 0.96]), (2.4, [0.95, 0.92, 0.95])])},
        "head": {"rotation": _kf([(0, [0, 0, -5]), (1.2, [0, 0, 5]), (2.4, [0, 0, -5])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 22])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, -22])])},
        "tail": {"rotation": _kf([(0, [0, 55, 0])])},
    }}


def _yawn():
    """2.8: a big yawn and a stretch: front paws far forward, back up, a long wobbly stretch, then a happy shake."""
    return {"loop": True, "animation_length": 3.0, "bones": {
        "leg_front_left": {"position": _kf([(0, [0, 0, 0]), (0.6, [0, 0.5, -4]), (1.8, [0, 0.5, -4]), (2.3, [0, 0, 0]), (3.0, [0, 0, 0])]),
                           "rotation": _kf([(0, [0, 0, 0]), (0.6, [-35, 0, 0]), (1.8, [-35, 0, 0]), (2.3, [0, 0, 0]), (3.0, [0, 0, 0])])},
        "leg_front_right": {"position": _kf([(0, [0, 0, 0]), (0.6, [0, 0.5, -4]), (1.8, [0, 0.5, -4]), (2.3, [0, 0, 0]), (3.0, [0, 0, 0])]),
                            "rotation": _kf([(0, [0, 0, 0]), (0.6, [-35, 0, 0]), (1.8, [-35, 0, 0]), (2.3, [0, 0, 0]), (3.0, [0, 0, 0])])},
        "root": {"rotation": _kf([(0, [0, 0, 0]), (0.6, [8, 0, 0]), (1.8, [8, 0, 0]), (2.3, [0, 0, 0]), (2.5, [0, 0, 4]),
                                  (2.65, [0, 0, -4]), (2.8, [0, 0, 3]), (3.0, [0, 0, 0])])},
        "body": {"scale": _kf([(0, [1, 1, 1]), (0.6, [0.94, 0.96, 1.14]), (1.2, [0.93, 0.97, 1.17]), (1.8, [0.94, 0.96, 1.14]),
                               (2.3, [1.04, 0.98, 1]), (3.0, [1, 1, 1])])},
        "head": {"scale": _kf([(0, [1, 1, 1]), (0.8, [1.06, 1.12, 1.04]), (1.5, [1.08, 1.14, 1.05]), (2.0, [1, 1, 1]), (3.0, [1, 1, 1])]),
                 "rotation": _kf([(0, [0, 0, 0]), (0.8, [0, 0, 6]), (1.6, [0, 0, -4]), (2.2, [0, 0, 0]), (3.0, [0, 0, 0])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 0]), (0.6, [0, -30, 10]), (1.8, [0, -30, 10]), (2.3, [0, 0, -15]), (2.6, [0, 0, 12]), (3.0, [0, 0, 0])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, 0]), (0.6, [0, 30, -10]), (1.8, [0, 30, -10]), (2.3, [0, 0, 15]), (2.6, [0, 0, -12]), (3.0, [0, 0, 0])])},
        "tail": {"rotation": _kf([(0, [0, 0, 0]), (0.6, [40, 0, 0]), (1.8, [45, 0, 0]), (2.3, [0, -20, 0]), (2.6, [0, 20, 0]), (3.0, [0, 0, 0])])},
    }}


def _sing():
    """2.8: sings along, swaying from side to side, one paw conducting, head tilting to the tune."""
    t = [0.0, 0.4, 0.8, 1.2, 1.6]
    return {"loop": True, "animation_length": 1.6, "bones": {
        "root": {"rotation": _kf(zip(t, [[0, 0, -6], [0, 0, 0], [0, 0, 6], [0, 0, 0], [0, 0, -6]])),
                 "position": _kf(zip(t, [[0, 0, 0], [0, 0.8, 0], [0, 0, 0], [0, 0.8, 0], [0, 0, 0]]))},
        "body": {"scale": _kf(zip(t, [[1.02, 1.0, 1.0], [1.05, 1.04, 1.02], [1.02, 1.0, 1.0], [1.05, 1.04, 1.02], [1.02, 1.0, 1.0]]))},
        "head": {"rotation": _kf(zip(t, [[0, 0, 8], [0, 0, 0], [0, 0, -8], [0, 0, 0], [0, 0, 8]])),
                 "scale": _kf(zip(t, [[1, 1, 1], [1.04, 1.06, 1.03], [1, 1, 1], [1.04, 1.06, 1.03], [1, 1, 1]]))},
        "leg_front_right": {"position": _kf([(0, [-4, 6, -8])]), "scale": _kf([(0, [1, 1.6, 1])]),
                            "rotation": _kf(zip(t, [[0, 0, -20], [0, 0, 10], [0, 0, -20], [0, 0, 10], [0, 0, -20]]))},
        "ear_left": {"rotation": _kf(zip(t, [[0, 0, 12], [0, 0, 0], [0, 0, 12], [0, 0, 0], [0, 0, 12]]))},
        "ear_right": {"rotation": _kf(zip(t, [[0, 0, 0], [0, 0, -12], [0, 0, 0], [0, 0, -12], [0, 0, 0]]))},
        "tail": {"rotation": _kf(zip(t, [[0, -25, 0], [10, 0, 0], [0, 25, 0], [10, 0, 0], [0, -25, 0]]))},
    }}


def _hug():
    """2.8: a big hug: both front paws out and in around a friend, a squeeze, a happy rock from side to side."""
    return {"loop": True, "animation_length": 2.0, "bones": {
        "leg_front_left": {"position": _kf([(0, [-2.5, 5, -9]), (1.0, [-3, 5.5, -9]), (2.0, [-2.5, 5, -9])]),
                           "scale": _kf([(0, [1.1, 1.5, 1.1])]),
                           "rotation": _kf([(0, [0, 0, 35]), (1.0, [0, 0, 45]), (2.0, [0, 0, 35])])},
        "leg_front_right": {"position": _kf([(0, [2.5, 5, -9]), (1.0, [3, 5.5, -9]), (2.0, [2.5, 5, -9])]),
                            "scale": _kf([(0, [1.1, 1.5, 1.1])]),
                            "rotation": _kf([(0, [0, 0, -35]), (1.0, [0, 0, -45]), (2.0, [0, 0, -35])])},
        "root": {"rotation": _kf([(0, [0, 0, -5]), (1.0, [0, 0, 5]), (2.0, [0, 0, -5])])},
        "body": {"scale": _kf([(0, [1, 1, 1]), (0.5, [0.95, 1.03, 0.97]), (1.0, [1, 1, 1]), (1.5, [0.95, 1.03, 0.97]), (2.0, [1, 1, 1])])},
        "head": {"rotation": _kf([(0, [0, 0, 10]), (1.0, [0, 0, -10]), (2.0, [0, 0, 10])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 18])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, -18])])},
        "tail": {"rotation": _kf([(0, [0, -30, 0]), (0.5, [0, 30, 0]), (1.0, [0, -30, 0]), (1.5, [0, 30, 0]), (2.0, [0, -30, 0])])},
    }}


def _hearts():
    """2.10: blows little hearts: a paw to the snoet, then out towards you (a kiss), a happy sway and wiggly ears."""
    return {"loop": True, "animation_length": 1.5, "bones": {
        "leg_front_right": {"position": _kf([(0, [-2, 6, -10]), (0.35, [-1.5, 7.5, -11.5]), (0.7, [-4, 7, -6]), (1.0, [-5, 6.5, -5]),
                                             (1.5, [-2, 6, -10])]),
                            "scale": _kf([(0, [1.05, 1.55, 1.05])]),
                            "rotation": _kf([(0, [0, 0, 0]), (0.35, [-10, 0, 10]), (0.7, [-55, 0, -25]), (1.0, [-60, 0, -30]),
                                             (1.5, [0, 0, 0])])},
        "root": {"rotation": _kf([(0, [0, 0, -3]), (0.7, [-4, 0, 3]), (1.5, [0, 0, -3])]),
                 "position": _kf([(0, [0, 0, 0]), (0.7, [0, 0.8, 0]), (1.0, [0, 0.4, 0]), (1.5, [0, 0, 0])])},
        "body": {"scale": _kf([(0, [1, 1, 1]), (0.35, [0.97, 1.03, 0.98]), (0.7, [1.04, 1.0, 1.02]), (1.5, [1, 1, 1])])},
        "head": {"rotation": _kf([(0, [0, 0, 8]), (0.35, [0, 0, 12]), (0.7, [0, 0, -4]), (1.5, [0, 0, 8])]),
                 "scale": _kf([(0, [1, 1, 1]), (0.35, [1.03, 0.98, 1.04]), (0.7, [1, 1, 1])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 10]), (0.7, [0, 0, -14]), (1.1, [0, 0, 6]), (1.5, [0, 0, 10])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, -10]), (0.7, [0, 0, 14]), (1.1, [0, 0, -6]), (1.5, [0, 0, -10])])},
        "tail": {"rotation": _kf([(0, [0, -30, 0]), (0.4, [0, 30, 0]), (0.8, [0, -30, 0]), (1.2, [0, 30, 0]), (1.5, [0, -30, 0])])},
    }}


def _twirl():
    """2.10: the knuffeldansje: little hops round and round (a full turn per loop), paws up in turn, ears flying."""
    t = [0.3 * i for i in range(9)]
    n = range(9)
    return {"loop": True, "animation_length": 2.4, "bones": {
        "root": {"position": _kf(zip(t, [[0, 3, 0] if i % 2 else [0, 0, 0] for i in n])),
                 "rotation": _kf(zip(t, [[0, 45 * i, 0 if i in (0, 8) else (6 if i % 2 else -6)] for i in n]))},
        "body": {"scale": _kf(zip(t, [[0.95, 1.1, 0.98] if i % 2 else [1.1, 0.9, 1.05] for i in n]))},
        "head": {"rotation": _kf(zip(t, [[0, 0, 12 if i % 4 < 2 else -12] for i in n]))},
        "leg_front_left": {"rotation": _kf(zip(t, [[-60, 0, -25] if i % 4 == 1 else [0, 0, 0] for i in n])),
                           "position": _kf(zip(t, [[0, 2.5, -1] if i % 4 == 1 else [0, 0, 0] for i in n]))},
        "leg_front_right": {"rotation": _kf(zip(t, [[-60, 0, 25] if i % 4 == 3 else [0, 0, 0] for i in n])),
                            "position": _kf(zip(t, [[0, 2.5, -1] if i % 4 == 3 else [0, 0, 0] for i in n]))},
        "ear_left": {"rotation": _kf(zip(t, [[0, -20, 30] if i % 2 else [0, 0, -10] for i in n]))},
        "ear_right": {"rotation": _kf(zip(t, [[0, 20, -30] if i % 2 else [0, 0, 10] for i in n]))},
        "tail": {"rotation": _kf(zip(t, [[20, -35, 0] if i % 2 else [20, 35, 0] for i in n]))},
    }}


def _bff_hug():
    """2.10: the bff-knuffel: stands up on its back paws, arms wide open around you, and rocks happily from side to side."""
    rock = [(0, 0), (0.5, -4), (1.0, 0), (1.5, 4), (2.0, 0)]
    arm = lambda s: _kf([(t, [-40 - (10 if i % 2 else 0), 0, s * (60 if i % 2 else 50)]) for i, (t, _) in enumerate(rock)])
    reach = lambda s: _kf([(t, [s * (2 if i % 2 else 3), 6.5 if i % 2 else 6, -10 if i % 2 else -8]) for i, (t, _) in enumerate(rock)])
    return {"loop": True, "animation_length": 2.0, "bones": {
        "root": {"rotation": _kf([(t, [-24 if i % 2 else -22, 0, z]) for i, (t, z) in enumerate(rock)]),
                 "position": _kf([(0, [0, 2, 1]), (1.0, [0, 2.5, 1]), (2.0, [0, 2, 1])])},
        "body": {"scale": _kf([(t, [0.95, 1.09, 0.96] if i % 2 else [0.98, 1.06, 0.98]) for i, (t, _) in enumerate(rock)])},
        "head": {"rotation": _kf([(0, [0, 0, 10]), (1.0, [0, 0, -10]), (2.0, [0, 0, 10])]),
                 "scale": _kf([(0, [1.02, 1.02, 1.02])])},
        "leg_front_left": {"position": reach(-1), "scale": _kf([(0, [1.1, 1.7, 1.1])]), "rotation": arm(1)},
        "leg_front_right": {"position": reach(1), "scale": _kf([(0, [1.1, 1.7, 1.1])]), "rotation": arm(-1)},
        "leg_back_left": {"rotation": _kf([(0, [22, 0, 0])])},
        "leg_back_right": {"rotation": _kf([(0, [22, 0, 0])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 20]), (1.0, [0, 0, 28]), (2.0, [0, 0, 20])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, -20]), (1.0, [0, 0, -28]), (2.0, [0, 0, -20])])},
        "tail": {"rotation": _kf([(t, [30, 35 if i % 2 else -35, 0]) for i, (t, _) in enumerate(rock)])},
    }}


def _sad():
    """2.10: lovingly sad ("ooh njeg..."): ears and head droop, it sinks a little, a slow sigh, a tiny sniff, the tail hangs."""
    return {"loop": True, "animation_length": 3.0, "bones": {
        "root": {"position": _kf([(0, [0, 0, 0]), (0.6, [0, -0.8, 0]), (1.6, [0, -1.0, 0]), (2.2, [0, -0.6, 0]), (3.0, [0, 0, 0])]),
                 "rotation": _kf([(0, [0, 0, 0]), (0.6, [6, 0, 0]), (2.2, [6, 0, 0]), (3.0, [0, 0, 0])])},
        "body": {"scale": _kf([(0, [1, 1, 1]), (0.6, [1.04, 0.92, 1.02]), (1.2, [1.06, 0.9, 1.03]), (1.8, [1.03, 0.94, 1.02]),
                               (2.4, [1.05, 0.91, 1.03]), (3.0, [1, 1, 1])])},
        "head": {"rotation": _kf([(0, [0, 0, 0]), (0.6, [0, 0, 12]), (1.4, [0, 0, 15]), (1.6, [0, 0, 11]), (1.8, [0, 0, 15]),
                                  (2.4, [0, 0, 12]), (3.0, [0, 0, 0])]),
                 "scale": _kf([(0, [1, 1, 1]), (1.6, [1.02, 0.98, 1.0]), (1.8, [1, 1, 1])])},
        "ear_left": {"rotation": _kf([(0, [0, 0, 0]), (0.6, [0, -20, 45]), (2.4, [0, -20, 45]), (3.0, [0, 0, 0])])},
        "ear_right": {"rotation": _kf([(0, [0, 0, 0]), (0.6, [0, 20, -45]), (2.4, [0, 20, -45]), (3.0, [0, 0, 0])])},
        "leg_front_left": {"position": _kf([(0, [0, 0, 0]), (0.6, [0.5, 0, -0.5]), (2.4, [0.5, 0, -0.5]), (3.0, [0, 0, 0])])},
        "leg_front_right": {"position": _kf([(0, [0, 0, 0]), (0.6, [-0.5, 0, -0.5]), (1.6, [-1.5, 3.5, -7]), (2.0, [-0.5, 0, -0.5]),
                                             (3.0, [0, 0, 0])]),
                            "scale": _kf([(0, [1, 1, 1]), (1.6, [1, 1.4, 1]), (2.0, [1, 1, 1])])},
        "tail": {"rotation": _kf([(0, [0, 0, 0]), (0.6, [-30, 0, 0]), (2.4, [-30, 0, 0]), (3.0, [0, 0, 0])])},
    }}


def animations():
    """animation name -> animation, merged into the guh animation file."""
    return {f"animation.guh.emote_{name}": make() for name, make in
            zip(EMOTES, [_wave, _dance, _sleep, _vahoeg, _roll, _munch, _shy, _yawn, _sing, _hug, _hearts, _twirl, _bff_hug, _sad])}


# --- particles ----------------------------------------------------------------------------------------------------------
PINK, DARK = (255, 120, 190), (120, 30, 80)


def _zzz(size):
    """A 'z' (sizes 0..2: small to big), soft blue-white with a dark edge."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = [6, 9, 12][size]
    x0 = y0 = (16 - s) // 2
    w = max(1, s // 5)
    col = (215, 230, 255, 255)
    for dx, dy, c in ((1, 1, (60, 70, 120, 255)), (0, 0, col)):
        d.rectangle([x0 + dx, y0 + dy, x0 + s - 1 + dx, y0 + w - 1 + dy], fill=c)
        d.rectangle([x0 + dx, y0 + s - w + dy, x0 + s - 1 + dx, y0 + s - 1 + dy], fill=c)
        for i in range(s):
            x = x0 + s - 1 - i * (s - 1) // (s - 1 if s > 1 else 1)
            y = y0 + i
            d.rectangle([x - w // 2 + dx - (w - 1) // 2, y + dy, x + w // 2 + dx, y + dy], fill=c)
    return img


# a tiny 5x7 pixel font for "VAHOEG!"
FONT = {
    "V": ["10001", "10001", "10001", "10001", "01010", "01010", "00100"],
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01111"],
    "!": ["1", "1", "1", "1", "1", "0", "1"],
}


def _vahoeg_text():
    """'VAHOEG!' in chunky pink letters with a dark outline (128x32: the particle is four times as wide as high)."""
    text = "VAHOEG!"
    scale, gap = 2, 4
    w, h = 128, 32
    cols = sum(len(FONT[c][0]) * scale + gap for c in text) - gap
    a = np.zeros((h, w, 4), np.uint8)
    x = (w - cols) // 2
    y = (h - 7 * scale) // 2
    mask = np.zeros((h, w), bool)
    for c in text:
        rows = FONT[c]
        for j, row in enumerate(rows):
            for i, bit in enumerate(row):
                if bit == "1":
                    mask[y + j * scale:y + (j + 1) * scale, x + i * scale:x + (i + 1) * scale] = True
        x += len(rows[0]) * scale + gap
    outline = np.zeros_like(mask)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            outline |= np.roll(np.roll(mask, dy, 0), dx, 1)
    a[outline] = (*DARK, 255)
    grad = np.linspace(0, 1, h)[:, None].repeat(w, 1)
    top, bottom = np.array((255, 200, 230)), np.array(PINK)
    colour = (top[None, None, :] * (1 - grad[..., None]) + bottom[None, None, :] * grad[..., None]).astype(np.uint8)
    a[mask, :3] = colour[mask]
    a[mask, 3] = 255
    return Image.fromarray(a)


def build(h):
    for i in range(3):
        h.save(_zzz(i), "particle", f"guh_zzz_{i}.png")
    h.w(f"{h.A}/particles/guh_zzz.json", {"textures": [f"guhs:guh_zzz_{i}" for i in range(3)]})
    h.save(_vahoeg_text(), "particle", "guh_vahoeg.png")
    h.w(f"{h.A}/particles/guh_vahoeg.json", {"textures": ["guhs:guh_vahoeg"]})
    # hidden advancements, granted by the game (EmoteGame / GuhEmotes)
    for name in ("emote_gedaan", "emote_lievelings", "emote_alle", "emote_jukebox", "emote_gezwaaid"):
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for key, (en, nl) in LANG.items():
        h.lang(key, en, nl)


LANG = {
    "gui.guhs.menu.emotes": ("Emotes", "Emotes"),
    "gui.guhs.menu.emotes.tooltip": ("Laat je guh zwaaien, dansen, slapen, springen, rollen, smakken, verlegen doen, gapen, zingen, "
                                     "knuffelen, hartjes blazen, een knuffeldansje of een bff-knuffel doen. Kies ook een "
                                     "lievelingsemote: die doet hij af en toe uit zichzelf.",
                                     "Laat je guh zwaaien, dansen, slapen, springen, rollen, smakken, verlegen doen, gapen, zingen, "
                                     "knuffelen, hartjes blazen, een knuffeldansje of een bff-knuffel doen. Kies ook een "
                                     "lievelingsemote: die doet hij af en toe uit zichzelf."),
    "gui.guhs.emotes.title": ("Emotes of %s", "Emotes van %s"),
    "gui.guhs.emotes.now": ("Now", "Nu"),
    "gui.guhs.emotes.now.tooltip": ("Do it once.", "Eén keer doen."),
    "gui.guhs.emotes.loop": ("Keep going", "Blijven doen"),
    "gui.guhs.emotes.loop.tooltip": ("Keep doing it until you press Stop (or something happens, like getting hurt).",
                                     "Blijft het doen tot je op Stop drukt (of er iets gebeurt, zoals pijn)."),
    "gui.guhs.emotes.favourite.tooltip": ("Make this the favourite emote: your guh does it by itself now and then.",
                                          "Maak dit de lievelingsemote: je guh doet hem af en toe uit zichzelf."),
    "gui.guhs.emotes.stop": ("Stop", "Stop"),
    "gui.guhs.emotes.stop.tooltip": ("Stop the emote it is doing now.", "Stop de emote die hij nu doet."),
    "gui.guhs.emotes.favourite": ("Favourite: %s", "Lievelingsemote: %s"),
    "gui.guhs.emotes.favourite.none": ("none", "geen"),
    "gui.guhs.emotes.favourite.clear": ("No favourite", "Geen lievelingsemote"),
    "gui.guhs.emotes.favourite.clear.tooltip": ("Your guh won't do an emote by itself any more (only when you ask).",
                                                "Je guh doet dan geen emotes meer uit zichzelf (alleen als jij het vraagt)."),
    "gui.guhs.emotes.doing": ("Doing now: %s", "Nu bezig: %s"),
    "gui.guhs.emotes.busy": ("Njeg! Not now, it's busy.", "Njeg! Nu even niet, hij is bezig."),
    # 3.0 (Guhverhalen): the 626-guh's own emote
    "emote.guhs.ukelele": ("Ukelele", "Ukelele"),
    "emote.guhs.ukelele.description": ("Tokkelt een klein ukeleletje en wiebelt met zijn extra armpjes. Aloha, njeg!",
                                       "Tokkelt een klein ukeleletje en wiebelt met zijn extra armpjes. Aloha, njeg!"),
    "gui.guhs.emotes.alleen_voor": ("Alleen de %s kan dat! Njeg.", "Alleen de %s kan dat! Njeg."),
    "gui.guhs.emotes.hint": ("Star = favourite", "Ster = lievelingsemote"),
    "emote.guhs.zwaaien": ("Wave", "Zwaaien"),
    "emote.guhs.dansen": ("Dance", "Dansen"),
    "emote.guhs.slapen": ("Sleep", "Slapen"),
    "emote.guhs.vahoeg": ("VAHOEG jump", "VAHOEG-sprong"),
    "emote.guhs.rollen": ("Roll over", "Rollen"),
    "emote.guhs.smakken": ("Munch", "Smakken"),
    "emote.guhs.verlegen": ("Shy", "Verlegen"),
    "emote.guhs.zwaaien.description": ("Waves a little paw. Hoi!", "Zwaait met een pootje. Hoi!"),
    "emote.guhs.dansen.description": ("Wiggles its vads to the beat.", "Schudt met zijn vads op de maat."),
    "emote.guhs.slapen.description": ("Curls up and snores softly. Zzz...", "Rolt zich op en snurkt zachtjes. Zzz..."),
    "emote.guhs.vahoeg.description": ("A happy jump with a spin. VAHOEG!", "Een blije sprong met een draai. VAHOEG!"),
    "emote.guhs.rollen.description": ("Rolls onto its back and wiggles its paws.", "Rolt op zijn rug en wiebelt met zijn pootjes."),
    "emote.guhs.smakken.description": ("Munches on an imaginary kaasknabbel. Crumbs everywhere!",
                                       "Smakt op een denkbeeldige kaasknabbel. Overal kruimels!"),
    "emote.guhs.verlegen.description": ("Hides its eyes behind its paws... and peeks.", "Verstopt zijn ogen achter zijn pootjes... en gluurt."),
    # 2.8 (Knuffeldal): three more
    "emote.guhs.gapen": ("Gapen", "Gapen"),
    "emote.guhs.zingen": ("Zingen", "Zingen"),
    "emote.guhs.knuffelen": ("Knuffelen", "Knuffelen"),
    "emote.guhs.gapen.description": ("Een enorme gaap en een lange rek. Goeiemorgen, guh!", "Een enorme gaap en een lange rek. Goeiemorgen, guh!"),
    "emote.guhs.zingen.description": ("Zingt een guhliedje en wiebelt mee. Tuintjes in de buurt groeien er harder van!",
                                      "Zingt een guhliedje en wiebelt mee. Tuintjes in de buurt groeien er harder van!"),
    "emote.guhs.knuffelen.description": ("Geeft een dikke vadsige knuffel. Hartjes!", "Geeft een dikke vadsige knuffel. Hartjes!"),
    # 2.10 (Lieve vadsjes van elkaar): four more (the samen slice ties the first three to the hartjes levels)
    "emote.guhs.hartjes": ("Hartjes", "Hartjes"),
    "emote.guhs.knuffeldansje": ("Knuffeldansje", "Knuffeldansje"),
    "emote.guhs.bff_knuffel": ("Bff-knuffel", "Bff-knuffel"),
    "emote.guhs.verdrietje": ("Verdrietje", "Verdrietje"),
    "emote.guhs.hartjes.description": ("Blaast kleine hartjes naar je toe, pootje bij de snoet. Lieve vadsjes van elkaar!",
                                       "Blaast kleine hartjes naar je toe, pootje bij de snoet. Lieve vadsjes van elkaar!"),
    "emote.guhs.knuffeldansje.description": ("Een blij rondjesdansje met huppeltjes. Mega lief en mega vadsig!",
                                             "Een blij rondjesdansje met huppeltjes. Mega lief en mega vadsig!"),
    "emote.guhs.bff_knuffel.description": ("Gaat op zijn achterpootjes staan en knuffelt je zo hard als hij kan. Zielsguh bff 5evr <3",
                                           "Gaat op zijn achterpootjes staan en knuffelt je zo hard als hij kan. Zielsguh bff 5evr <3"),
    "emote.guhs.verdrietje.description": ("Oortjes omlaag, een klein zuchtje... ooh njeg. Gelukkig helpt een knuffel altijd.",
                                          "Oortjes omlaag, een klein zuchtje... ooh njeg. Gelukkig helpt een knuffel altijd."),
}


def ftb(fq):
    q, adv = fq.q, fq.adv
    y = 50
    q("emote_eerste", "Kijk eens wat ik kan!",
      "Houd rechtsklik op je tamme guh, kies &dEmotes&r en laat hem iets doen: zwaaien, dansen, slapen, een &6VAHOEG&r-sprong, "
      "rollen, smakken of verlegen doen.", "guhs:kaas_knabbels", [adv("emote_gedaan")], x=-8, y=y)
    q("emote_lievelings", "Zijn lievelingsemote",
      "Geef je guh een &dlievelingsemote&r (het sterretje in het Emotes-menu). Die doet hij voortaan af en toe helemaal uit zichzelf.",
      "minecraft:nether_star", [adv("emote_lievelings")], rewards=(("guhs:kaas_knabbels", 12),), x=-6, y=y)
    q("emote_alle", "Guh met talent",
      "Laat je guhs alle emotes een keer doen (ook gapen, zingen, knuffelen en het verdrietje). De hartjes-emotes tellen niet mee: "
      "die ontgrendel je met hartjes. Hij kan meer dan alleen vadsig zijn!",
      "minecraft:note_block", [adv("emote_alle")], rewards=(("guhs:gefrituurde_kaasknabbels", 3),), x=-4, y=y, shape="gear")
    q("emote_jukebox", "Guhdisco",
      "Zet een jukebox aan met muziek erin. Guhs in de buurt kunnen het niet laten: ze gaan &ddansen&r! "
      "(Wacht er even bij, tamme of wilde.)", "minecraft:jukebox", [adv("emote_jukebox")], x=-2, y=y)
    q("emote_gezwaaid", "Hoi hoi!",
      "Loop naar een &dwilde&r guh toe. Met een beetje geluk zwaait hij naar je! (Verlegen guhs doen liever verlegen.)",
      "guhs:guh_spawn_egg", [adv("emote_gezwaaid")], x=0, y=y)
