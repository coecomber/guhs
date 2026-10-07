"""
bbq2 (ring-h3) - the Barbecuerog: model, textures and animations of the entity guhs:barbecuerog.

DESIGN_130 4 (user decision 2026-10-06): a big, dangerous, devilish BALROG first: a towering horned demon of black charcoal and
cracked glowing embers, fire in the cracks, a burning mane, great smoke-and-ember wings, heavy claws, a flaming whip and a blade
of fire, glowing eyes. Only SLIGHT Mika traits: the two round ears (charred, glowing inside) under the horns, the little blunt
nose and one long crooked fang. The whip is a string of knotted braadworstjes (the one small joke), burning along its length.

What makes him a demon and not a golem of boxes (the polish list of 2026-10-06, written from a capture in a real client):
  - the silhouette: the head carried low and forward between shoulders like boulders, a sloping trapezius, a chest that
    tapers over leaning flank slabs to a narrow waist, arms bent outwards with forearms thicker than the upper arms, legs
    bent like a beast's, spines down the back;
  - fire: a mane of twenty flames (sheets of four tongues, up to four blocks tall) from the crown down the neck and the
    back, on the shoulders, elbows, tail and wing wrists; a furnace for a mouth and for a belly;
  - wings: three SOLID sails a side on ribs and fingers of charred bone, smoke with fire behind it, a scalloped hem that
    burns (not thin see-through rags: in the game those were line art against the wall);
  - teeth: rows of cut-out pointed fangs and four great ones built in steps (not pale pegs);
  - skin: plates of charcoal, ashen in the middle, fire in the seams of broad patches only (a net of thin bright cracks
    all over reads as a lava golem).

The model is built at its real size (no renderer scale): 16 units = one block; 9.7 blocks to the top of his head, 12.5 to the
tips of his horns, his fire to 13.5; his wings are 18 blocks wide folded and 21 spread. Every cube has per-face UVs into one
512 x 512 atlas of painted material tiles (1 px per unit, so 16 px per block like the rest of the game); the glow mask
(<name>_glowmask.png, GeckoLib's AutoGlowingGeoLayer) holds the seams, the eyes, the embers and every flame. Nothing is
translucent: wings, flames and teeth are cut-out.

  build(h)        geckolib/models/entity/barbecuerog.geo.json, geckolib/animations/entity/barbecuerog.animation.json,
                  textures/entity/barbecuerog.png + barbecuerog_glowmask.png
  check(h)        the files exist and every animated bone is a bone of the model
  preview(out)    pictures from every side, from below, of his head, in the dark, and in the key poses of the animations
                  (python tools/features/ring_h3_modellen.py <out> [shot names], from the worktree root)
  schilder(...)   one picture of the model, lit about as the game lights him

Bone names root, body, head, ear_left/right, tail, arm_left/right are the ones the cast's animation vocabulary knows
(feature/ring/client/CastAnimaties); the rest is his own.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

NAAM = "barbecuerog"
GEO = ("geckolib", "models", "entity")
ANIM = ("geckolib", "animations", "entity")
ATLAS = 512
TEGEL = 128

# the rest pose of the whip: it hangs from the fist, bends forward over the floor and lies there in a lazy S
ZWEEP_X = [-4, -6, -30, -32, -20, -4, 4, -4, 4, -3, 3]
ZWEEP_Y = [0, 0, 0, 10, 16, 24, 26, -22, -28, -24, 18]
ZWEEP_N = len(ZWEEP_X)
# the frame of a wing (units, in the flat spread wing: x from the body outwards, the arm of the wing at y = 133):
# ribs of the inner sail (x of the root, angle, length) and fingers of the outer sail (angle, length), all from the arm down
VLEUGEL_RIBBEN = ((16, -5, 84), (68, 14, 92))
VLEUGEL_VINGERS = ((-9, 97), (-33, 110))

# material -> (x, y, w, h) in the atlas
TEGELS = {
    "kool": (0, 0, 128, 128),          # plates of black charcoal, fire in the seams of two fifths of them
    "kool2": (128, 0, 128, 128),       # darker, nearly dead (backs, limbs)
    "gloed": (256, 0, 128, 128),       # glowing coals
    "hoorn": (384, 0, 128, 128),       # ridged horn of gloeikool
    "vleugel": (0, 128, 128, 128),     # the inner sail of a wing: smoke with fire behind it, a torn burning hem
    "vleugel2": (128, 128, 128, 128),  # the outer sail
    "vleugel3": (0, 384, 128, 128),    # the tip
    "vlam_c": (128, 384, 128, 128),    # a sheet of fire: four tongues from one root (the mane)
    "vlam_a": (256, 128, 64, 128),     # a flame tongue
    "vlam_b": (320, 128, 64, 128),
    "zwaard": (384, 128, 64, 128),     # the blade of fire
    "zwaard2": (448, 128, 64, 128),
    "worst": (0, 256, 64, 64),         # a braadworstje of the lash: charred, burst, burning
    "knoop": (64, 256, 32, 32),        # the knot between two worstjes: an ember
    "oog": (96, 256, 32, 32),
    "tand": (128, 256, 32, 64),
    "oor": (160, 256, 64, 64),         # the inside of a Mika ear: ember pink
    "rooster": (224, 256, 32, 64),     # grill iron (his belt)
    "klauw": (256, 256, 32, 64),
    "gebit": (288, 256, 128, 32),      # a row of fangs (hanging: the gum at the top), open between them
    "leeg": (480, 480, 16, 16),        # nothing (see-through)
}


# =====================================================================================================================
# the geometry
# =====================================================================================================================
class Model:
    def __init__(self, seed=21301850):
        self.bones = []
        self.rng = np.random.default_rng(seed)

    def bone(self, name, parent, pivot, rotation=None):
        b = {"name": name, "pivot": [float(v) for v in pivot], "cubes": []}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = [float(v) for v in rotation]
        self.bones.append(b)
        return name

    def _bone(self, name):
        for b in self.bones:
            if b["name"] == name:
                return b
        raise KeyError(name)

    def _uv(self, mat, fw, fh, stretch, flip=False):
        x, y, w, h = TEGELS[mat]
        if stretch:
            # the whole tile on the face; a pair (v0, v1) takes that part of its height (v0 > v1: upside down)
            v0, v1 = (0.0, 1.0) if stretch is True else stretch
            uv, maat = [x, round(y + v0 * h, 2)], [w, round((v1 - v0) * h, 2)]
            if flip:
                uv[0], maat[0] = x + w, -w
            return {"uv": uv, "uv_size": maat}
        pw, ph = max(1.0, min(float(fw), w)), max(1.0, min(float(fh), h))
        u = x + int(self.rng.integers(0, max(1, int(w - pw) + 1)))
        v = y + int(self.rng.integers(0, max(1, int(h - ph) + 1)))
        return {"uv": [u, v], "uv_size": [round(pw, 2), round(ph, 2)]}

    def cube(self, bone, origin, size, mat, faces="nsewud", stretch=False, inflate=0.0, mats=None, flip=""):
        """A cube of `mat`; `faces` = which sides are drawn (n s e w u d); mats = {face letter: material} for exceptions;
        flip = the faces whose picture is mirrored left-right (a thin sail must show the SAME picture on both sides)."""
        sx, sy, sz = size
        dims = {"n": (sx, sy), "s": (sx, sy), "e": (sz, sy), "w": (sz, sy), "u": (sx, sz), "d": (sx, sz)}
        names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
        uv = {}
        for f in faces:
            m = (mats or {}).get(f, mat)
            uv[names[f]] = self._uv(m, dims[f][0], dims[f][1], stretch, f in flip)
        c = {"origin": [round(float(v), 3) for v in origin], "size": [round(float(v), 3) for v in size], "uv": uv}
        if inflate:
            c["inflate"] = inflate
        self._bone(bone)["cubes"].append(c)

    def vlam(self, bone, x, y, z, breed, hoog, mat="vlam_a", kruis=True):
        """A flame: two crossed see-through planes standing on (x, y, z)."""
        self.cube(bone, [x - breed / 2, y, z - 0.1], [breed, hoog, 0.2], mat, faces="ns", stretch=True)
        if kruis:
            self.cube(bone, [x - 0.1, y, z - breed / 2], [0.2, hoog, breed], mat, faces="ew", stretch=True)

    def geo(self):
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{NAAM}", "texture_width": ATLAS, "texture_height": ATLAS,
                            "visible_bounds_width": 24, "visible_bounds_height": 14, "visible_bounds_offset": [0, 6, 0]},
            "bones": self.bones}]}


def _sp(origin, size):
    """The same cube on the other side (x -> -x)."""
    return [-(origin[0] + size[0]), origin[1], origin[2]], size


def maak():
    m = Model()
    m.bone("root", None, [0, 0, 0])

    def beide(fn):
        for kant in (1, -1):
            s = "left" if kant > 0 else "right"

            def c(bone, origin, size, mat, kant=kant, **kw):
                o, sz = (origin, size) if kant > 0 else _sp(origin, size)
                m.cube(bone, o, sz, mat, **kw)
            fn(kant, s, c)

    # --- the hips: narrow, under a belt of grill iron with a loincloth of charred hide plates -------------------------------------
    m.bone("heup", "root", [0, 70, 4])
    m.cube("heup", [-20, 60, -10], [40, 19, 24], "kool2")
    m.cube("heup", [-15, 53, -8], [30, 9, 20], "kool2")
    m.cube("heup", [-22, 75, -11.5], [44, 4.5, 27], "rooster")
    m.cube("heup", [-5.5, 72, -13.6], [11, 10, 3], "hoorn")                 # the buckle: a horned plate with an ember in it
    m.cube("heup", [-2, 75, -14.4], [4, 4, 1.2], "gloed")
    for x, hoog, breed in ((-17, 19, 9), (-5.5, 31, 11), (8, 19, 9)):
        m.cube("heup", [x, 75 - hoog, -12.9], [breed, hoog, 1.6], "kool2")
        m.cube("heup", [x + breed / 2 - 1.5, 75 - hoog - 3, -13.2], [3, 4.5, 2], "hoorn")
    m.cube("heup", [-11, 50, 13.4], [22, 25, 1.6], "kool2")

    # --- the legs: thigh (knee forward), shin (ankle back), a great clawed foot --------------------------------------------------
    def been(kant, s, c):
        m.bone(f"leg_{s}", "heup", [kant * 14, 68, 2], rotation=[-18, 0, kant * -8])
        c(f"leg_{s}", [4, 34, -11], [22, 38, 24], "kool")
        c(f"leg_{s}", [24.5, 42, -8], [4, 26, 18], "kool2")                 # the muscle on the outside of the thigh
        c(f"leg_{s}", [19, 67, -5], [8, 7, 10], "hoorn")                    # a hip spike
        c(f"leg_{s}", [6.5, 29, -14.5], [17, 10, 7], "kool2")               # the knee cap
        c(f"leg_{s}", [12.5, 33, -20], [5, 5, 6.5], "hoorn")                # and its spike
        c(f"leg_{s}", [8, 36, 12.4], [14, 12, 1.2], "gloed")                # the hollow of the knee glows
        m.bone(f"scheen_{s}", f"leg_{s}", [kant * 15, 36, 0], rotation=[38, 0, 0])
        c(f"scheen_{s}", [7, 18, -8], [16, 20, 17], "kool2")
        c(f"scheen_{s}", [8.5, 6, -6.5], [13, 13, 13], "kool2")             # (thinner towards the ankle)
        c(f"scheen_{s}", [9, 15, 8.5], [12, 19, 5], "kool")                 # the calf
        c(f"scheen_{s}", [13, 30, 12], [4, 5, 7], "hoorn")                  # the hock spur
        m.bone(f"voet_{s}", f"scheen_{s}", [kant * 15, 8, 0], rotation=[-20, 0, 0])
        c(f"voet_{s}", [4, 0, -18], [22, 10, 30], "kool")
        c(f"voet_{s}", [7, 9.5, -12], [16, 4, 15], "kool2")                 # the instep
        for i in range(3):
            c(f"voet_{s}", [4.3 + i * 7.6, 0, -28], [5.8, 7, 11], "klauw", stretch=True)
            c(f"voet_{s}", [5.4 + i * 7.6, 0, -33], [3.6, 4.5, 6], "klauw", stretch=(0.5, 1.0))
        c(f"voet_{s}", [12, 2, 11], [7, 6, 8], "klauw", stretch=True)       # the heel spur
    beide(been)

    # --- the tail: thick, spiked, an ember club at the end --------------------------------------------------------------------------
    m.bone("tail", "heup", [0, 72, 14], rotation=[-38, 0, 0])
    m.cube("tail", [-8, 64, 12], [16, 15, 34], "kool")
    m.cube("tail", [-2, 78.5, 20], [4, 5, 5], "hoorn")
    m.cube("tail", [-2, 78.5, 34], [4, 4, 5], "hoorn")
    m.bone("tail_2", "tail", [0, 71, 44], rotation=[22, 0, 0])
    m.cube("tail_2", [-6, 65, 44], [12, 12, 32], "kool2")
    m.cube("tail_2", [-1.5, 76.5, 52], [3, 4, 4], "hoorn")
    m.cube("tail_2", [-1.5, 76.5, 64], [3, 3.5, 4], "hoorn")
    m.bone("tail_3", "tail_2", [0, 71, 74], rotation=[26, 0, 0])
    m.cube("tail_3", [-4, 66.5, 74], [8, 9, 28], "kool")
    m.cube("tail_3", [-5.5, 65, 100], [11, 12, 10], "gloed")                 # the ember club at the end
    m.cube("tail_3", [-1.5, 76, 102], [3, 6, 3], "hoorn")
    m.cube("tail_3", [-8.5, 69, 103], [3, 3, 4], "hoorn")
    m.cube("tail_3", [5.5, 69, 103], [3, 3, 4], "hoorn")

    # --- the waist: narrow, plates of charcoal over the fire in his belly ----------------------------------------------------------
    m.bone("body", "heup", [0, 78, 4], rotation=[10, 0, 0])
    m.cube("body", [-16, 76, -9.5], [32, 26, 21], "kool2")
    m.cube("body", [-11.5, 78, -10.5], [23, 22, 1.2], "gloed")               # the fire, seen between the plates
    for rij in range(3):
        for x in (-10.8, 1.2):
            m.cube("body", [x, 78.6 + rij * 7.4, -12], [9.6, 6.1, 2.2], "kool")
    m.cube("body", [-18.5, 77, -7], [3.5, 23, 16], "kool")                   # the flanks
    m.cube("body", [15, 77, -7], [3.5, 23, 16], "kool")
    m.cube("body", [-2.5, 88, 11], [5, 8, 6], "hoorn")

    # --- the chest: a great wedge, leaning forward, with a rift of fire down the breastbone ---------------------------------------------
    m.bone("borst", "body", [0, 102, 4], rotation=[9, 0, 0])
    m.cube("borst", [-23, 99, -12], [46, 14, 27], "kool2")                   # the lower ribs
    m.cube("borst", [-32, 111, -16], [64, 27, 36], "kool")                   # the barrel of the chest
    m.cube("borst", [-27.5, 114.5, -20], [25.5, 21, 4.5], "kool2")           # the pectoral plates
    m.cube("borst", [2, 114.5, -20], [25.5, 21, 4.5], "kool2")
    m.cube("borst", [-2, 110, -17.8], [4, 27, 1.8], "gloed")                 # the rift between them
    m.cube("borst", [-26, 112.8, -18.4], [52, 1.7, 2.4], "gloed")            # and the glow under them
    m.cube("borst", [-25, 119, 14], [50, 25, 13], "kool2")                   # the hump of his back
    m.cube("borst", [-17, 126, 26], [34, 16, 6], "kool")
    m.cube("borst", [-13, 130, -11], [26, 18, 22], "kool2")                  # the root of the neck, thick as a tree
    for i, y in enumerate((101, 106, 111)):
        m.cube("borst", [-23.6 - i * 4.2, y, -8], [1.2, 2.6, 20], "gloed")   # glowing ribs in his flanks
        m.cube("borst", [22.4 + i * 4.2, y, -8], [1.2, 2.6, 20], "gloed")

    def romp(kant, s, c):
        # the flank: a slab from the waist to the armpit, leaning out (this is what makes his back a V)
        m.bone(f"flank_{s}", "borst", [kant * 20, 99, 0], rotation=[0, 0, kant * 15])
        c(f"flank_{s}", [18, 98, -11], [10, 27, 24], "kool2")
        # the trapezius: a slope from the neck down to the shoulder (no square shoulders)
        m.bone(f"trap_{s}", "borst", [kant * 7, 142, 0], rotation=[0, 0, kant * 22])
        c(f"trap_{s}", [5, 135, -12], [32, 11, 27], "kool2")
        c(f"trap_{s}", [14, 145, -2], [5, 8, 5], "hoorn")
        c(f"trap_{s}", [24, 145, 4], [4, 6, 4], "hoorn")
    beide(romp)
    # the ridge of spines down his back, leaning backwards
    for i, (y, z, hoog, dik) in enumerate(((141, 22, 18, 7), (127, 31, 15, 7), (113, 27, 12, 6), (101, 20, 9, 5))):
        m.bone(f"stekel_{i + 1}", "borst", [0, y, z], rotation=[-28, 0, 0])
        m.cube(f"stekel_{i + 1}", [-dik / 2, y, z - dik / 2], [dik, hoog, dik], "hoorn")
        m.cube(f"stekel_{i + 1}", [-dik / 4, y + hoog, z - dik / 4], [dik / 2, hoog * 0.45, dik / 2], "gloed")

    # --- shoulders like boulders, long arms, hands like shovels ------------------------------------------------------------------------
    def arm(kant, s, c):
        m.bone(f"arm_{s}", "borst", [kant * 38, 130, 2], rotation=[8, 0, kant * -19])
        c(f"arm_{s}", [27, 119, -13], [27, 27, 31], "kool")                  # the shoulder
        c(f"arm_{s}", [29.5, 144.5, -10], [22, 5, 25], "kool2")              # a plate on top of it
        c(f"arm_{s}", [52.5, 124, -9], [4, 17, 22], "kool2")                 # and one on its outside
        for (sx, sy, sz, w, hg) in ((33, 149, -7, 6, 15), (43.5, 148, 2, 5, 12), (36, 149, 9, 5, 10)):
            c(f"arm_{s}", [sx, sy, sz], [w, hg, w], "hoorn")
            c(f"arm_{s}", [sx + w / 4, sy + hg, sz + w / 4], [w / 2, hg * 0.4, w / 2], "gloed")
        c(f"arm_{s}", [29.5, 117.3, -9], [21, 1.8, 23], "gloed")             # fire in the armpit
        c(f"arm_{s}", [32, 92, -8], [18, 28, 19], "kool2")                   # the upper arm
        c(f"arm_{s}", [33.5, 97, -11.2], [15, 17, 4], "kool")                # the biceps
        m.bone(f"onderarm_{s}", f"arm_{s}", [kant * 41, 93, 2], rotation=[-30, 0, kant * 15])
        c(f"onderarm_{s}", [30.5, 62, -10], [22, 34, 23], "kool")            # the forearm: thicker than the upper arm
        c(f"onderarm_{s}", [29, 59, -11.5], [25, 9.5, 26], "kool2")          # a bracer of charcoal round the wrist
        c(f"onderarm_{s}", [29.3, 68.5, -11.8], [24.4, 1.6, 26.6], "gloed")  # with fire under its rim
        c(f"onderarm_{s}", [36.5, 72, -11], [4, 20, 1.2], "gloed")           # a rift down the forearm
        for i in range(3):
            c(f"onderarm_{s}", [52, 71 + i * 8, -2.5], [6.5 - i * 1.2, 4.5, 5.5], "hoorn")   # spikes along its outside
        c(f"onderarm_{s}", [38.5, 89, 12], [6, 7, 12], "hoorn")              # the elbow spike
        m.bone(f"hand_{s}", f"onderarm_{s}", [kant * 41.5, 61, 1], rotation=[-8, 0, 0])
        c(f"hand_{s}", [29.5, 46, -11], [24, 15, 23], "kool2")
        for i in range(3):
            c(f"hand_{s}", [30.6 + i * 7.6, 59.5, -12.8], [4, 4, 3], "hoorn")                 # knuckles
            c(f"hand_{s}", [30.2 + i * 7.8, 35, -12.6], [5.6, 12, 6.6], "klauw", stretch=(0.0, 0.55))
            c(f"hand_{s}", [31.2 + i * 7.8, 25.5, -13.6], [3.6, 10, 4.4], "klauw", stretch=(0.45, 1.0))
        c(f"hand_{s}", [48.5, 38, 3], [5, 10, 6], "klauw", stretch=(0.0, 0.55))                # the thumb claw
        c(f"hand_{s}", [49.2, 30, 3.8], [3.4, 8.5, 4.2], "klauw", stretch=(0.45, 1.0))
    beide(arm)

    # --- the whip: a lash of fire (look close: it is a string of knotted braadworstjes, burning) (left hand) -------------------------------
    m.cube("hand_left", [39, 44, -4.5], [5, 5, 14], "kool2")                 # the grip in his fist
    ouder = "hand_left"
    wx, wy, wz = 41.5, 46, -2
    for i in range(ZWEEP_N):
        naam = f"zweep_{i + 1}"
        dik = 4.6 - i * 0.16
        m.bone(naam, ouder, [wx, wy, wz], rotation=[ZWEEP_X[i], ZWEEP_Y[i], 0])
        m.cube(naam, [wx - dik / 2, wy - 12, wz - dik / 2], [dik, 11.2, dik], "worst")
        m.cube(naam, [wx - dik / 2 + 0.6, wy - 13, wz - dik / 2 + 0.6], [dik - 1.2, 2.0, dik - 1.2], "knoop")
        m.bone(f"vlam_zweep_{i + 1}", naam, [wx, wy - 6, wz])
        groot = i in (2, 5, 8) or i == ZWEEP_N - 1
        m.vlam(f"vlam_zweep_{i + 1}", wx, wy - (11 if groot else 9), wz, 11 if groot else 7.5, 19 if groot else 12, "vlam_a" if i % 2 else "vlam_b")
        ouder, wy = naam, wy - 13

    # --- the blade of fire (right hand) ----------------------------------------------------------------------------------------------
    m.bone("zwaard", "hand_right", [-41.5, 50, -2], rotation=[-12, 0, 0])
    m.cube("zwaard", [-44, 47.5, 6], [5, 5, 12], "kool2")                    # the pommel end of the grip
    m.cube("zwaard", [-44.5, 47, 17], [6, 6, 5], "hoorn")
    m.cube("zwaard", [-44, 47.5, -14], [5, 5, 20], "kool2")
    m.cube("zwaard", [-52, 45, -18], [21, 10, 4.5], "hoorn")                 # the guard, its ends turned forward
    m.cube("zwaard", [-55, 46.5, -24], [4, 7, 8], "hoorn")
    m.cube("zwaard", [-32, 46.5, -24], [4, 7, 8], "hoorn")
    m.bone("zwaardvlam", "zwaard", [-41.5, 50, -18])
    m.cube("zwaardvlam", [-43.3, 48.2, -104], [3.6, 3.6, 86], "gloed")       # the white-hot core
    for z0, lang, breed in ((-52, 34, 18), (-86, 36, 13), (-116, 32, 8)):
        m.cube("zwaardvlam", [-41.5 - breed / 2, 49.9, z0], [breed, 0.2, lang], "zwaard", faces="ud", stretch=True)
        m.cube("zwaardvlam", [-41.6, 50 - breed / 2, z0], [0.2, breed, lang], "zwaard2", faces="ew", stretch=True)

    # --- the head: carried low and forward between the shoulders ------------------------------------------------------------------------
    m.bone("head", "borst", [0, 140, -12], rotation=[-12, 0, 0])
    m.cube("head", [-13, 134, -20], [26, 20, 16], "kool2")                   # the back of the skull, into the neck
    m.cube("head", [-16, 138, -46], [32, 22, 30], "kool")                    # the skull
    m.cube("head", [-18, 153.5, -49.5], [36, 7.5, 11], "kool2")              # the brow: a heavy overhang
    m.cube("head", [-9.5, 150.4, -48.8], [8, 3.6, 3], "kool2")               # and its angry inner corners
    m.cube("head", [1.5, 150.4, -48.8], [8, 3.6, 3], "kool2")
    m.cube("head", [-9.5, 137, -56], [19, 12, 11], "kool")                   # the blunt snout (a Mika's, grown monstrous)
    m.cube("head", [-5, 148.5, -53], [10, 4, 8], "kool2")                    # the bridge of the nose
    m.cube("head", [-2.5, 144.6, -57.2], [5, 3.4, 1.6], "oor")               # his little nose still glows pink
    m.cube("head", [-8.5, 136.2, -55], [17, 0.8, 30], "gloed", faces="d")    # the roof of his mouth: a furnace
    m.cube("head", [-3.5, 160, -42], [7, 3.5, 30], "hoorn")                  # a crest over the skull

    def kop(kant, s, c):
        m.bone(f"oog_{s}", "head", [kant * 10.4, 149.8, -46])                 # (a bone of its own: he can shut it)
        for o, sz in (([4.2, 147.2, -46.7], [6, 4.6, 1.4]), ([9.4, 148.8, -46.7], [7.2, 5.4, 1.4])):
            c(f"oog_{s}", o, sz, "oog", stretch=True)                        # eyes: slanted slits of white fire
        c("head", [14.8, 139, -44], [4, 9.5, 17], "kool2")                   # the cheek bone
        c("head", [17.6, 141.5, -36], [4, 4, 8], "hoorn")                    # and a spike on it
        c("head", [16.1, 139.5, -26], [1.2, 9, 10], "gloed")                 # a glowing gill behind the jaw
        c("head", [12.5, 160.5, -47], [4.5, 7, 4.5], "hoorn")                # a little horn over each eye
    beide(kop)

    def tand(bone, x, y, z, dik, lang, neer=True):
        """A great fang in three steps to a point (neer: hanging from y, else standing on y)."""
        y0 = y
        for deel, (d, l, v0, v1) in enumerate(((1.0, 0.4, 1.0, 0.6), (0.66, 0.33, 0.6, 0.3), (0.34, 0.27, 0.3, 0.0))):
            w, h = dik * d, lang * l
            o = (dik - w) / 2
            if neer:
                m.cube(bone, [x + o, y0 - h, z + o], [w, h, w], "tand", stretch=(v0, v1))
                y0 -= h
            else:
                m.cube(bone, [x + o, y0, z + o], [w, h, w], "tand", stretch=(v1, v0))
                y0 += h
    # his teeth: rows of fangs (flat, cut out: real points), and four great ones with body to them
    m.cube("head", [-9.5, 129.2, -56.25], [19, 8.2, 0.2], "gebit", faces="ns", stretch=True)
    m.cube("head", [-9.7, 130.2, -55.5], [0.2, 7.2, 20], "gebit", faces="ew", stretch=True)
    m.cube("head", [9.5, 130.2, -55.5], [0.2, 7.2, 20], "gebit", faces="ew", stretch=True, flip="ew")
    tand("head", -9.6, 137.6, -56.4, 3.4, 9.5)
    tand("head", 6.2, 137.6, -56.4, 3.4, 9.5)
    tand("head", -14.2, 138.5, -53.5, 4.2, 15)                               # one long crooked fang, as every Mika has
    m.bone("kaak", "head", [0, 138, -18], rotation=[14, 0, 0])
    m.cube("kaak", [-10.5, 126.5, -53], [21, 6.5, 36], "kool2", mats={"u": "gloed"})
    m.cube("kaak", [-7.5, 123, -51], [15, 4, 11], "kool")                    # the chin
    m.cube("kaak", [-2, 118.5, -50], [4, 5, 4], "hoorn")                     # and a spike under it
    m.cube("kaak", [-13.2, 127, -30], [3, 11, 13], "kool")                   # the hinges of the jaw
    m.cube("kaak", [10.2, 127, -30], [3, 11, 13], "kool")
    m.cube("kaak", [-9.5, 133, -53.15], [19, 6.2, 0.2], "gebit", faces="ns", stretch=(1.0, 0.0), flip="ns")
    m.cube("kaak", [-10.3, 133, -52.5], [0.2, 5.6, 19], "gebit", faces="ew", stretch=(1.0, 0.0), flip="ew")
    m.cube("kaak", [10.1, 133, -52.5], [0.2, 5.6, 19], "gebit", faces="ew", stretch=(1.0, 0.0))
    tand("kaak", 8.6, 131, -54.2, 4.0, 13.5, neer=False)                     # a tusk standing up outside the lip (the other side has the fang)

    # horns of gloeikool: a chain of turned pieces, sweeping out, then up and forward like a bull's
    def hoorn(kant, s, c):
        m.bone(f"hoorn_{s}", "head", [kant * 15, 154, -22], rotation=[0, 0, kant * 62])
        c(f"hoorn_{s}", [6, 149, -31], [18, 21, 18], "hoorn")
        m.bone(f"hoorn_{s}_2", f"hoorn_{s}", [kant * 15, 170, -22], rotation=[-12, 0, kant * -26])
        c(f"hoorn_{s}_2", [8.5, 167, -28.5], [13, 21, 13], "hoorn")
        m.bone(f"hoorn_{s}_3", f"hoorn_{s}_2", [kant * 15, 188, -22], rotation=[-20, 0, kant * -28])
        c(f"hoorn_{s}_3", [10.3, 185, -26.7], [9.4, 20, 9.4], "hoorn")
        m.bone(f"hoorn_{s}_4", f"hoorn_{s}_3", [kant * 15, 205, -22], rotation=[-24, 0, kant * -20])
        c(f"hoorn_{s}_4", [12, 202, -25], [6, 17, 6], "hoorn")
        c(f"hoorn_{s}_4", [13.2, 218, -23.8], [3.6, 10, 3.6], "gloed")              # the tip glows
        # the Mika ears: round, charred, glowing inside, sticking out under the horns
        m.bone(f"ear_{s}", "head", [kant * 17, 141, -16], rotation=[0, kant * -16, kant * 74])
        c(f"ear_{s}", [10, 140, -17.5], [14, 12, 2.4], "kool2")
        c(f"ear_{s}", [12, 151, -17.5], [10, 4.5, 2.4], "kool2")
        c(f"ear_{s}", [13, 143, -18.1], [8, 8, 0.8], "oor")
    beide(hoorn)

    # --- the mane: a storm of fire from his crown down his neck and his back, on his shoulders, his elbows, his tail -----------------------
    manen = (("head", 0, 161, -38, 20, 38, "vlam_a"), ("head", -10, 160, -30, 16, 34, "vlam_b"), ("head", 10, 160, -30, 16, 34, "vlam_a"),
             ("head", 0, 162, -26, 36, 62, "vlam_c"), ("head", -11, 158, -14, 20, 42, "vlam_a"), ("head", 11, 158, -14, 20, 42, "vlam_b"),
             ("head", 0, 154, -8, 34, 56, "vlam_c"),
             ("borst", 0, 147, 6, 38, 58, "vlam_c"), ("borst", -15, 144, 8, 20, 38, "vlam_b"), ("borst", 15, 144, 8, 20, 38, "vlam_a"),
             ("borst", 0, 141, 22, 32, 48, "vlam_c"), ("borst", 0, 127, 32, 22, 36, "vlam_a"), ("borst", 0, 110, 28, 16, 26, "vlam_b"),
             ("arm_left", 39, 149, 0, 20, 36, "vlam_a"), ("arm_right", -39, 149, 0, 20, 36, "vlam_b"),
             ("onderarm_left", 41.5, 93, 15, 11, 20, "vlam_b"), ("onderarm_right", -41.5, 93, 15, 11, 20, "vlam_a"),
             ("body", 0, 96, 13, 12, 20, "vlam_a"), ("tail", 0, 79, 28, 11, 18, "vlam_b"), ("tail_2", 0, 77, 58, 10, 16, "vlam_a"))
    for i, (bot, x, y, z, breed, hoog, mat) in enumerate(manen):
        m.bone(f"vlam_{i + 1}", bot, [x, y, z])
        m.vlam(f"vlam_{i + 1}", x, y, z, breed, hoog, mat)
    m.bone("vlam_staart", "tail_3", [0, 77, 105])
    m.vlam("vlam_staart", 0, 77, 105, 15, 26, "vlam_b")

    # --- the wings: great sails of smoke and embers on a frame of charred bone ------------------------------------------------------------
    def vleugel(kant, s, c):
        m.bone(f"vleugel_{s}", "borst", [kant * 13, 134, 19], rotation=[0, kant * -38, kant * -42])
        c(f"vleugel_{s}", [10, 130, 15], [58, 8, 8], "kool2")                # the arm of the wing
        om = "n" if kant > 0 else "s"                                        # (the tile's left edge is the side of the body)
        c(f"vleugel_{s}", [12, 44, 19.2], [58, 88, 0.4], "vleugel", faces="ns", stretch=True, flip=om)
        c(f"vleugel_{s}", [63, 137, 15.5], [6, 14, 6], "hoorn")              # the claw on the wrist
        c(f"vleugel_{s}", [64.5, 151, 17], [3, 6, 3], "gloed")
        for j, (px, hoek, lang) in enumerate(VLEUGEL_RIBBEN):                # the ribs of the inner sail
            m.bone(f"rib_{s}_{j + 1}", f"vleugel_{s}", [kant * px, 133, 19.4], rotation=[0, 0, kant * hoek])
            c(f"rib_{s}_{j + 1}", [px - 1.6, 133 - lang, 17.8], [3.2, lang, 3.2], "kool2")
        m.bone(f"vleugel_{s}_2", f"vleugel_{s}", [kant * 68, 134, 19], rotation=[0, kant * 14, kant * 58])
        c(f"vleugel_{s}_2", [68, 131, 16], [62, 6, 6], "kool2")
        c(f"vleugel_{s}_2", [68, 36, 19.5], [62, 98, 0.4], "vleugel2", faces="ns", stretch=True, flip=om)
        for j, (hoek, lang) in enumerate(VLEUGEL_VINGERS):                   # the fingers of the outer sail
            m.bone(f"vinger_{s}_{j + 1}", f"vleugel_{s}_2", [kant * 68, 133, 19.6], rotation=[0, 0, kant * hoek])
            c(f"vinger_{s}_{j + 1}", [66.6, 133 - lang, 18.2], [2.8, lang, 2.8], "kool2")
        m.bone(f"vleugel_{s}_3", f"vleugel_{s}_2", [kant * 130, 134, 19], rotation=[0, kant * 12, kant * 10])
        c(f"vleugel_{s}_3", [130, 132, 17], [36, 4, 4], "kool2")
        c(f"vleugel_{s}_3", [130, 36, 19.8], [38, 98, 0.4], "vleugel3", faces="ns", stretch=True, flip=om)
        c(f"vleugel_{s}_3", [164, 129, 17], [5, 9, 4], "gloed")              # the burning tip
        m.bone(f"vlam_vleugel_{s}", f"vleugel_{s}", [kant * 66, 150, 18.5])
        m.vlam(f"vlam_vleugel_{s}", kant * 66, 150, 18.5, 12, 22, "vlam_a" if kant > 0 else "vlam_b")
    beide(vleugel)
    return m


# =====================================================================================================================
# the textures
# =====================================================================================================================
def _ruis(rng, n, schaal):
    """Smooth value noise n x n, 0..1."""
    klein = rng.random((n // schaal + 2, n // schaal + 2))
    img = Image.fromarray((klein * 255).astype(np.uint8)).resize(((n // schaal + 2) * schaal, (n // schaal + 2) * schaal), Image.BICUBIC)
    return np.asarray(img).astype(np.float32)[:n, :n] / 255.0


def _barsten(rng, n, aantal, lengte):
    """A crack mask n x n (0..1): random walks that fork."""
    a = np.zeros((n, n), np.float32)
    for _ in range(aantal):
        x, y = rng.uniform(0, n), rng.uniform(0, n)
        hoek = rng.uniform(0, 2 * math.pi)
        for stap in range(int(lengte * rng.uniform(0.5, 1.4))):
            hoek += rng.normal(0, 0.45)
            if rng.random() < 0.06:
                hoek += rng.choice([-1.2, 1.2])
            x, y = x + math.cos(hoek), y + math.sin(hoek)
            xi, yi = int(x) % n, int(y) % n
            a[yi, xi] = 1.0
            if rng.random() < 0.3:
                a[yi, (xi + 1) % n] = max(a[yi, (xi + 1) % n], 0.7)
    return a


def _schollen(rng, n, aantal):
    """Plates (a Voronoi pattern that wraps): the distance to the seam round every plate, and which plate a pixel is in."""
    pts = rng.random((aantal, 2)) * n
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    d1 = np.full((n, n), 1e9, np.float32)
    d2 = d1.copy()
    wie = np.zeros((n, n), np.int32)
    for i, (px, py) in enumerate(pts):
        dx = np.abs(xx - px)
        dx = np.minimum(dx, n - dx)
        dy = np.abs(yy - py)
        dy = np.minimum(dy, n - dy)
        d = np.sqrt(dx * dx + dy * dy)
        nieuw = d < d1
        d2 = np.where(nieuw, d1, np.minimum(d2, d))
        wie = np.where(nieuw, i, wie)
        d1 = np.where(nieuw, d, d1)
    return d2 - d1, wie


def _gloei(a):
    """0..1 heat -> RGB of an ember: dark red, orange, yellow, white."""
    a = np.clip(a, 0, 1)
    r = np.clip(a * 2.7 + 0.08, 0, 1) * 255
    g = np.clip(a * 1.75 - 0.62, 0, 1) * 232
    b = np.clip(a * 3.2 - 2.6, 0, 1) * 190
    return np.stack([r, g, b], -1)


def _vervaag(a, keer=1):
    for _ in range(keer):
        a = (a + np.roll(a, 1, 0) + np.roll(a, -1, 0) + np.roll(a, 1, 1) + np.roll(a, -1, 1)) / 5.0
    return a


def _zet(arr, glow, mat, rgb, alpha=None, gloed=None):
    """Writes a tile: rgb (h, w, 3), alpha (h, w) 0..255 or None, gloed (h, w) 0..1 = how much of it is in the glow mask."""
    x, y, w, h = TEGELS[mat]
    arr[y:y + h, x:x + w, :3] = np.clip(rgb, 0, 255)
    arr[y:y + h, x:x + w, 3] = 255 if alpha is None else alpha
    if gloed is not None:
        g = np.clip(gloed, 0, 1)
        zichtbaar = (g > 0.02) & (arr[y:y + h, x:x + w, 3] > 0)
        glow[y:y + h, x:x + w, :3] = np.where(zichtbaar[..., None], np.clip(rgb, 0, 255), 0)
        glow[y:y + h, x:x + w, 3] = np.where(zichtbaar, np.clip(g * 255, 0, 255), 0)


def _kool(rng, n, donker, heet_deel):
    """Charcoal: a crust of black plates, ashen in their middles. Under it the fire: in broad patches (heet_deel of the skin)
    it shows in the seams between the plates, with a red halo; elsewhere the seams are dead black. (Not a net of thin bright
    lines all over him: that reads as a lava golem.)"""
    rand, wie = _schollen(rng, n, 44)
    toon = rng.random(44)[wie]
    basis = 18 + toon * 18 + _ruis(rng, n, 4) * 10 + rng.random((n, n)) * 6 + np.clip(rand, 0, 6) * 2.3
    basis *= donker
    rgb = np.stack([basis * 1.08, basis * 0.97, basis * 0.95], -1)
    warm = _ruis(rng, n, 32) * 0.65 + _ruis(rng, n, 12) * 0.35
    drempel = np.quantile(warm, 1 - heet_deel)
    warm = np.clip((warm - drempel) / 0.07 + 0.5, 0, 1)
    naad = np.clip(1 - rand / 1.7, 0, 1)
    heet = np.clip(naad + np.exp(-rand / 2.4) * 0.8, 0, 1.5) * warm
    rgb *= (1 - 0.6 * naad * (1 - warm))[..., None]
    g = np.clip(heet, 0, 1)
    rgb = rgb * (1 - g[..., None]) + _gloei(np.clip(heet * 0.7, 0, 1)) * g[..., None]
    return rgb, np.clip(heet * 1.15, 0, 1)


def _vlam(rng, w, h, wit=0.25, slank=1.0):
    """A flame tongue (base at the bottom): rgb, alpha."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = 1 - yy / (h - 1)                                    # 0 at the base, 1 at the tip
    golf = np.sin(t * 9 + rng.uniform(0, 6)) * (w * 0.10) * t + np.sin(t * 23 + rng.uniform(0, 6)) * (w * 0.04)
    breed = (w * 0.46) * slank * np.clip(np.sin(np.clip(t * 1.08 + 0.12, 0, 1) * math.pi), 0, 1) ** 0.8 * (1 - t * 0.35)
    d = np.abs(xx - (w - 1) / 2 - golf) / np.maximum(breed, 0.6)
    rand = rng.random((h, w)) * 0.18
    binnen = d + rand < 1.0
    heet = np.clip((1 - d) * (1.25 - t * 0.9) + wit * (1 - t) * (1 - d), 0, 1.2)
    rgb = _gloei(0.34 + heet * 0.66)
    # loose sparks above the tip
    for _ in range(max(3, w // 8)):
        sx, sy = int(rng.integers(w // 4, 3 * w // 4)), int(rng.integers(0, h // 3))
        binnen[sy:sy + 2, sx:sx + 2] = True
        rgb[sy:sy + 2, sx:sx + 2] = (255, 190, 70)
    return rgb, np.where(binnen, 255, 0)


def _vlammenzee(rng, n):
    """A sheet of fire: four tongues of different heights out of one broad root (base at the bottom): rgb, alpha."""
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    t = 1 - yy / (n - 1)
    binnen = np.zeros((n, n), bool)
    heet = np.zeros((n, n), np.float32)
    for cx, top, breedte in ((0.20, 0.62, 0.17), (0.43, 1.0, 0.21), (0.66, 0.78, 0.18), (0.85, 0.5, 0.14)):
        tt = np.clip(t / top, 0, 1.2)
        golf = np.sin(tt * 8 + rng.uniform(0, 6)) * (n * 0.07) * tt + np.sin(tt * 21 + rng.uniform(0, 6)) * (n * 0.025)
        breed = n * breedte * np.clip(1 - tt, 0, 1) ** 0.62 * (0.75 + 0.25 * np.sin(np.clip(tt * 1.4, 0, 1) * math.pi))
        d = np.abs(xx - cx * n - golf) / np.maximum(breed, 0.5)
        tong = (d + rng.random((n, n)) * 0.2 < 1.0) & (tt < 1.0)
        binnen |= tong
        heet = np.maximum(heet, np.where(tong, np.clip((1 - d) * (1.3 - tt * 0.95) + 0.3 * (1 - tt) * (1 - d), 0, 1.2), 0))
    # the root: one bed of fire across the bottom
    voet = (t < 0.1 + 0.03 * np.sin(xx * 0.4)) & (np.abs(xx - n * 0.5) < n * 0.44)
    binnen |= voet
    heet = np.maximum(heet, np.where(voet, 0.95, 0))
    rgb = _gloei(0.34 + heet * 0.66)
    for _ in range(14):
        sx, sy = int(rng.integers(n // 8, 7 * n // 8)), int(rng.integers(0, n // 2))
        binnen[sy:sy + 2, sx:sx + 2] = True
        rgb[sy:sy + 2, sx:sx + 2] = (255, 190, 70)
    return rgb, np.where(binnen, 255, 0)


# the three sails of a wing as ONE picture (units of the flat spread wing, y down from the arm of the wing):
# name -> (x of its left edge, width, height of its cube)
ZEILEN = {"vleugel": (12, 58, 88), "vleugel2": (68, 62, 98), "vleugel3": (130, 38, 98)}


def _zoom_x(x):
    """The hem of the spread wing at x (units below the arm of the wing): it hangs lowest where a rib or a finger ends and is
    bitten out in a deep bow between two of them; the tip runs up to a point."""
    einden = [(12.0, 80.0)]
    for px, hoek, lang in VLEUGEL_RIBBEN:
        einden.append((px - math.sin(math.radians(hoek)) * lang, math.cos(math.radians(hoek)) * lang))
    for hoek, lang in VLEUGEL_VINGERS:
        einden.append((68 - math.sin(math.radians(hoek)) * lang, math.cos(math.radians(hoek)) * lang))
    einden.append((168.0, 8.0))
    einden.sort()
    for (x0, y0), (x1, y1) in zip(einden, einden[1:]):
        if x0 <= x <= x1:
            f = (x - x0) / max(1e-6, x1 - x0)
            recht = y0 + (y1 - y0) * f
            hap = 0.6 * min(x1 - x0, 40.0) * math.sin(f * math.pi) ** 0.8
            return recht - hap
    return 0.0


def _vleugel(rng, mat):
    """One sail (128 x 128; left = the side of the body, top = the arm of the wing): dark smoke with fire behind it (broad
    smouldering patches), a glowing seam along every rib, embers adrift, burn holes, and a hem that burns. Solid: only the
    bites out of the hem and the holes are open."""
    n = TEGEL
    x_links, breed, hoog = ZEILEN[mat]
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    X = x_links + (xx + 0.5) / n * breed                         # wing units
    Y = (yy + 0.5) / n * hoog                                    # units below the top of the cube
    top = {"vleugel": 134 - 132, "vleugel2": 134 - 134, "vleugel3": 134 - 134}[mat]      # (the cube's top against the arm at 133/134)
    zoom = np.array([_zoom_x(float(x)) for x in X[0]], np.float32)[None, :] + (np.sin(X * 0.9) + np.sin(X * 2.3)) * 1.1
    diepte = zoom - (Y + top)                                     # > 0 inside the sail, 0 on the hem
    alpha = diepte > 0
    rook = 30 + _ruis(rng, n, 24) * 30 + _ruis(rng, n, 6) * 14
    rgb = np.stack([rook * 1.06, rook * 0.9, rook * 0.9], -1)
    # fire behind the smoke
    warm = _ruis(rng, n, 32) * 0.6 + _ruis(rng, n, 10) * 0.4
    warm = np.clip((warm - np.quantile(warm, 0.58)) / 0.16, 0, 1)
    # the seams along the frame
    naad = np.zeros((n, n), np.float32)

    def lijn(x0, y0, x1, y1, dik, kracht):
        nonlocal naad
        dx, dy = x1 - x0, y1 - y0
        f = np.clip(((X - x0) * dx + (Y + top - y0) * dy) / (dx * dx + dy * dy), 0, 1)
        d = np.sqrt((X - x0 - f * dx) ** 2 + (Y + top - y0 - f * dy) ** 2)
        naad = np.maximum(naad, np.clip(1 - d / dik, 0, 1) * kracht)
    for px, hoek, lang in VLEUGEL_RIBBEN:
        lijn(px, 0, px - math.sin(math.radians(hoek)) * lang, math.cos(math.radians(hoek)) * lang, 4.6, 0.85)
    for hoek, lang in VLEUGEL_VINGERS:
        lijn(68, 0, 68 - math.sin(math.radians(hoek)) * lang, math.cos(math.radians(hoek)) * lang, 4.6, 0.85)
    lijn(12, 3, 168, 3, 5.0, 0.7)                                # under the arm of the wing
    for _ in range(7):                                           # thin veins between them
        x0, y0 = rng.uniform(x_links, x_links + breed), rng.uniform(4, 30)
        lijn(x0, y0, x0 + rng.uniform(-14, 14), y0 + rng.uniform(24, 58), 1.3, 0.5)
    # the hem burns; holes burnt through
    rand = np.clip(1 - diepte / 3.0, 0, 1) * alpha
    halo = np.clip(1 - diepte / 9.0, 0, 1) * alpha * 0.55
    gat = np.zeros((n, n), bool)
    gatrand = np.zeros((n, n), np.float32)
    for _ in range(4):
        cx, cy, r = rng.uniform(10, n - 10), rng.uniform(n * 0.3, n * 0.8), rng.uniform(3.0, 6.0)
        d = np.sqrt((xx - cx) ** 2 + ((yy - cy) * 0.8) ** 2)
        gat |= d < r
        gatrand = np.maximum(gatrand, np.clip(1 - (d - r) / 2.5, 0, 1) * (d >= r))
    alpha &= ~gat
    heet = np.clip(np.maximum.reduce([naad, rand, halo, gatrand, warm * 0.34]), 0, 1)
    for _ in range(34):                                          # embers adrift in the smoke
        sx, sy = int(rng.integers(1, n - 1)), int(rng.integers(n // 8, n - 1))
        heet[sy, sx] = 1.0
    rgb = rgb * (1 - heet[..., None]) + _gloei(0.12 + heet * 0.62) * heet[..., None]
    return rgb, np.where(alpha, 255, 0), np.clip(heet * 1.05, 0, 1) * alpha


def textuur(seed=21301851):
    rng = np.random.default_rng(seed)
    arr = np.zeros((ATLAS, ATLAS, 4), np.float32)
    glow = np.zeros((ATLAS, ATLAS, 4), np.float32)
    n = TEGEL
    rgb, heet = _kool(rng, n, 1.0, 0.42)
    _zet(arr, glow, "kool", rgb, gloed=heet)
    rgb, heet = _kool(rng, n, 0.8, 0.13)
    _zet(arr, glow, "kool2", rgb, gloed=heet)
    # glowing coals: bright cells with dark seams
    cel = _ruis(rng, n, 8) * 0.6 + _ruis(rng, n, 3) * 0.4
    naad = _barsten(rng, n, 22, 30)
    heet = np.clip(0.36 + cel * 0.5 - _vervaag(naad, 1) * 0.8, 0.1, 0.84)
    _zet(arr, glow, "gloed", _gloei(heet), gloed=np.clip(heet * 1.3, 0.35, 1))
    # horn: dark ridged gloeikool with fire in the grooves
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    ribbel = (np.sin(yy * 0.9 + np.sin(xx * 0.21) * 1.6) * 0.5 + 0.5)
    basis = 26 + ribbel * 26 + _ruis(rng, n, 8) * 12
    rgb = np.stack([basis * 1.25, basis * 0.86, basis * 0.72], -1)
    groef = np.clip((0.16 - ribbel) * 7, 0, 1) * (0.55 + 0.45 * _ruis(rng, n, 16))
    rgb = rgb * (1 - groef[..., None]) + _gloei(0.25 + groef * 0.55) * groef[..., None]
    _zet(arr, glow, "hoorn", rgb, gloed=groef)
    for mat in ZEILEN:
        rgb, alpha, heet = _vleugel(rng, mat)
        _zet(arr, glow, mat, rgb, alpha=alpha, gloed=heet)
    for mat, wit, slank in (("vlam_a", 0.3, 1.0), ("vlam_b", 0.2, 0.86)):
        rgb, alpha = _vlam(rng, 64, 128, wit, slank)
        _zet(arr, glow, mat, rgb, alpha=alpha, gloed=np.ones((128, 64)))
    rgb, alpha = _vlammenzee(rng, n)
    _zet(arr, glow, "vlam_c", rgb, alpha=alpha, gloed=np.ones((n, n)))
    for mat in ("zwaard", "zwaard2"):
        # the blade: a strip of fire, ragged on both edges, white-hot along its axis
        yy2, xx2 = np.mgrid[0:128, 0:64].astype(np.float32)
        rafel = 24 + np.sin(yy2 * 0.55 + rng.uniform(0, 6)) * 4 + np.sin(yy2 * 1.7 + rng.uniform(0, 6)) * 2.5 + rng.random((128, 64)) * 3
        d = np.abs(xx2 - 31.5) / rafel
        rgb = _gloei(np.clip(1.02 - d * 0.62, 0, 1))
        _zet(arr, glow, mat, rgb, alpha=np.where(d < 1.0, 255, 0), gloed=np.ones((128, 64)))
    # a braadworstje of the lash: charred black-brown, burst open along its length, the fat in it burning
    yy, xx = np.mgrid[0:64, 0:64].astype(np.float32)
    korst = 30 + _ruis(rng, 64, 8) * 26 + rng.random((64, 64)) * 8
    worst = np.stack([korst * 1.5, korst * 0.8, korst * 0.5], -1)
    barst = _barsten(rng, 64, 9, 22)
    scheur = np.clip(barst + _vervaag(barst, 1) * 1.3 + _vervaag(barst, 3) * 1.2, 0, 1.3)
    g = np.clip(scheur, 0, 1)
    worst = worst * (1 - g[..., None]) + _gloei(np.clip(0.2 + scheur * 0.6, 0, 1)) * g[..., None]
    _zet(arr, glow, "worst", worst, gloed=np.clip(scheur * 1.1, 0, 1))
    knoop = 0.6 + rng.random((32, 32)) * 0.3
    _zet(arr, glow, "knoop", _gloei(knoop), gloed=np.ones((32, 32)))
    # the eye: white fire, an orange rim, a slit
    yy, xx = np.mgrid[0:32, 0:32].astype(np.float32)
    d = np.abs(yy - 15.5) / 15.5
    oog = _gloei(1.0 - d * 0.45)
    oog[:, 14:18] = (255, 120, 20)
    oog[4:28, 15:17] = (90, 14, 0)
    _zet(arr, glow, "oog", oog, gloed=np.ones((32, 32)))
    # a fang (the top of the tile = the point): smoked bone, yellowed, black and glowing at the root
    yy, xx = np.mgrid[0:64, 0:32].astype(np.float32)
    t = yy / 63.0
    been = np.array((232, 214, 160))[None, None, :] * (1 - t[..., None]) + np.array((128, 86, 48))[None, None, :] * t[..., None]
    been = been * (0.86 + rng.random((64, 32, 1)) * 0.18)
    been *= (1 - 0.22 * (np.abs(xx - 15.5) / 15.5) ** 2)[..., None]                 # rounder: darker towards its sides
    heet = np.clip((t - 0.82) * 4.5, 0, 1)
    been = been * (1 - heet[..., None]) + _gloei(0.26 + heet * 0.4) * heet[..., None]
    _zet(arr, glow, "tand", been, gloed=heet * 0.8)
    # a row of fangs (hanging; the gum at the top): every one a curved point of its own length, gaps between them
    x0, y0, w, h = TEGELS["gebit"]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    rij = np.zeros((h, w, 3), np.float32)
    open_ = np.zeros((h, w), bool)
    gl = np.zeros((h, w), np.float32)
    x = 3.0
    while x < w - 6:
        breed = float(rng.uniform(9, 15))
        lang = float(rng.uniform(15, 29)) if rng.random() < 0.75 else float(rng.uniform(9, 14))
        krom = float(rng.uniform(-2.5, 2.5))
        d = np.clip((yy - 3) / lang, 0, 1)
        half = breed / 2 * (1 - d) ** 0.85
        binnen = (np.abs(xx - (x + breed / 2) - krom * d * d) < half) & (yy >= 3) & (yy < 3 + lang)
        kleur = np.array((128, 86, 48))[None, None, :] * (1 - d[..., None]) + np.array((236, 220, 168))[None, None, :] * d[..., None]
        kleur = kleur * (1 - 0.3 * np.clip(np.abs(xx - (x + breed / 2) - krom * d * d) / np.maximum(half, 0.5), 0, 1) ** 2)[..., None]
        rij = np.where(binnen[..., None], kleur, rij)
        open_ |= binnen
        x += breed + float(rng.uniform(0.5, 3.5))
    tandvlees = yy < 4
    rij = np.where(tandvlees[..., None], _gloei(np.full((h, w), 0.42)), rij)
    gl[tandvlees] = 0.7
    _zet(arr, glow, "gebit", rij * (0.9 + rng.random((h, w, 1)) * 0.14), alpha=np.where(open_ | tandvlees, 255, 0), gloed=gl)
    yy, xx = np.mgrid[0:64, 0:32].astype(np.float32)
    t = yy / 63.0
    # a claw (the top of the tile = black iron, the bottom red-hot)
    rgb = np.array((30, 26, 28))[None, None, :] * (1 - t[..., None]) + np.array((150, 60, 20))[None, None, :] * t[..., None]
    rgb = rgb * (0.86 + rng.random((64, 32, 1)) * 0.2)
    heet = np.clip((t - 0.62) * 2.8, 0, 1)
    rgb = rgb * (1 - heet[..., None]) + _gloei(0.3 + heet * 0.5) * heet[..., None]
    _zet(arr, glow, "klauw", rgb, gloed=heet)
    # the inside of an ear, the nose: ember pink (what is left of a Mika)
    cel = _ruis(rng, 64, 8)
    oor = np.stack([104 + cel * 56, 26 + cel * 26, 22 + cel * 20], -1)
    _zet(arr, glow, "oor", oor, gloed=0.2 + cel * 0.3)
    # grill iron (his belt): dull, hot in the middle
    yy, xx = np.mgrid[0:64, 0:32].astype(np.float32)
    ijzer = np.full((64, 32, 3), 40, np.float32) + rng.random((64, 32, 1)) * 14
    heet = np.clip(1 - np.abs(yy - 32) / 22.0, 0, 1) * 0.5
    ijzer = ijzer * (1 - heet[..., None]) + _gloei(heet + 0.1) * heet[..., None]
    _zet(arr, glow, "rooster", ijzer, gloed=heet * 0.8)
    return np.clip(arr, 0, 255).astype(np.uint8), np.clip(glow, 0, 255).astype(np.uint8)


# =====================================================================================================================
# the animations
# =====================================================================================================================
VLAMMEN = [b["name"] for b in maak().bones if b["name"].startswith("vlam_")]       # every flame of the model
ZWEEP = [f"zweep_{i}" for i in range(1, ZWEEP_N + 1)]


def _flakker(lengte, kracht=1.0, uit=()):
    """Every flame breathes on its own beat (scale) and leans a little."""
    bones = {}
    for i, naam in enumerate(VLAMMEN):
        if naam in uit:
            continue
        stap = 0.2 + (i % 4) * 0.05
        sleutels, rot = {}, {}
        t, j = 0.0, 0
        while t < lengte + 1e-6:
            hoog = 1.0 + (0.28 if (j + i) % 2 else -0.14) * kracht + (0.12 if (j + i) % 3 == 0 else 0)
            sleutels[str(round(t, 3))] = [1.0 + (0.1 if j % 2 else -0.06), round(hoog, 3), 1.0]
            rot[str(round(t, 3))] = [0, 0, (7 if (j + i) % 2 else -7) * kracht]
            t += stap
            j += 1
        sleutels[str(lengte)] = sleutels["0.0"]
        rot[str(lengte)] = rot["0.0"]
        bones[naam] = {"scale": sleutels, "rotation": rot}
    return bones


def _zweep_golf(lengte, kracht, fase=0.0, slagen=1):
    """A wave running down the whip."""
    bones = {}
    for i, naam in enumerate(ZWEEP):
        rot = {}
        n = 8 * slagen
        for k in range(n + 1):
            t = lengte * k / n
            hoek = math.sin(2 * math.pi * slagen * k / n - i * 0.55 + fase) * kracht * (0.5 + i * 0.09)
            rot[str(round(t, 3))] = [round(hoek, 2), 0, round(hoek * 0.4, 2)]
        bones[naam] = {"rotation": rot}
    return bones


def _vleugels(open_, tip, heen=0.0):
    """Wing pose (added to the folded rest pose): open_ lifts and spreads the arm, tip unfolds the outer part."""
    return {"vleugel_left": [0, 38 * open_ + heen, 42 * open_ - 18 * open_], "vleugel_right": [0, -38 * open_ - heen, -42 * open_ + 18 * open_],
            "vleugel_left_2": [0, -14 * tip, -58 * tip + 12 * tip], "vleugel_right_2": [0, 14 * tip, 58 * tip - 12 * tip]}


def _rot(d):
    return {"rotation": d}


def _sleutels(*paren):
    return {str(t): list(v) for t, v in paren}


def animaties():
    A = {}
    # --- idle: he breathes like a bellows, the wings drift, the tail sweeps, the whip smoulders ---------------------------------
    L = 4.0
    idle = _flakker(L)
    idle.update(_zweep_golf(L, 5, slagen=1))
    idle.update({
        "borst": {"scale": _sleutels((0.0, [1, 1, 1]), (2.0, [1.035, 1.03, 1.05]), (4.0, [1, 1, 1])),
                  "rotation": _sleutels((0.0, [0, 0, 0]), (2.0, [-2, 0, 0]), (4.0, [0, 0, 0]))},
        "head": _rot(_sleutels((0.0, [0, 0, 0]), (1.0, [2, 7, 0]), (2.0, [0, 0, 0]), (3.0, [2, -7, 0]), (4.0, [0, 0, 0]))),
        "kaak": _rot(_sleutels((0.0, [0, 0, 0]), (2.0, [7, 0, 0]), (4.0, [0, 0, 0]))),
        "tail": _rot(_sleutels((0.0, [0, -9, 0]), (2.0, [0, 9, 0]), (4.0, [0, -9, 0]))),
        "tail_2": _rot(_sleutels((0.0, [0, -8, 0]), (2.0, [0, 8, 0]), (4.0, [0, -8, 0]))),
        "tail_3": _rot(_sleutels((0.0, [0, -8, 0]), (2.0, [0, 8, 0]), (4.0, [0, -8, 0]))),
        "vleugel_left": _rot(_sleutels((0.0, [0, 0, 0]), (2.0, [0, 6, 5]), (4.0, [0, 0, 0]))),
        "vleugel_right": _rot(_sleutels((0.0, [0, 0, 0]), (2.0, [0, -6, -5]), (4.0, [0, 0, 0]))),
        "vleugel_left_2": _rot(_sleutels((0.0, [0, 0, 0]), (2.0, [0, 0, -7]), (4.0, [0, 0, 0]))),
        "vleugel_right_2": _rot(_sleutels((0.0, [0, 0, 0]), (2.0, [0, 0, 7]), (4.0, [0, 0, 0]))),
        "arm_left": _rot(_sleutels((0.0, [0, 0, 0]), (2.0, [-3, 0, -2]), (4.0, [0, 0, 0]))),
        "arm_right": _rot(_sleutels((0.0, [0, 0, 0]), (2.0, [-3, 0, 2]), (4.0, [0, 0, 0]))),
        "zwaardvlam": {"scale": _sleutels((0.0, [1, 1, 1]), (0.5, [1.12, 1.12, 1.03]), (1.0, [0.94, 0.94, 0.98]), (1.5, [1.1, 1.1, 1.04]),
                                         (2.0, [1, 1, 1]), (2.5, [1.12, 1.12, 1.03]), (3.0, [0.94, 0.94, 0.98]), (3.5, [1.1, 1.1, 1.04]), (4.0, [1, 1, 1]))},
    })
    A["idle"] = (True, L, idle)
    # --- loop: slow, crushing steps; the whole body rolls, the wings half open ------------------------------------------------------
    L = 2.4
    w = _vleugels(0.35, 0.3)
    loop = _flakker(L, 1.3)
    loop.update(_zweep_golf(L, 12, slagen=2))
    loop.update({
        "heup": {"position": _sleutels((0.0, [0, 0, 0]), (0.3, [0, -3.5, 0]), (0.6, [0, 1.5, 0]), (1.2, [0, 0, 0]), (1.5, [0, -3.5, 0]), (1.8, [0, 1.5, 0]),
                                       (2.4, [0, 0, 0])),
                 "rotation": _sleutels((0.0, [0, 0, 3]), (1.2, [0, 0, -3]), (2.4, [0, 0, 3]))},
        "leg_left": _rot(_sleutels((0.0, [-30, 0, 0]), (1.2, [26, 0, 0]), (2.4, [-30, 0, 0]))),
        "leg_right": _rot(_sleutels((0.0, [26, 0, 0]), (1.2, [-30, 0, 0]), (2.4, [26, 0, 0]))),
        "scheen_left": _rot(_sleutels((0.0, [6, 0, 0]), (0.6, [0, 0, 0]), (1.2, [10, 0, 0]), (1.8, [36, 0, 0]), (2.4, [6, 0, 0]))),
        "scheen_right": _rot(_sleutels((0.0, [10, 0, 0]), (0.6, [36, 0, 0]), (1.2, [6, 0, 0]), (1.8, [0, 0, 0]), (2.4, [10, 0, 0]))),
        "voet_left": _rot(_sleutels((0.0, [18, 0, 0]), (1.2, [-14, 0, 0]), (2.4, [18, 0, 0]))),
        "voet_right": _rot(_sleutels((0.0, [-14, 0, 0]), (1.2, [18, 0, 0]), (2.4, [-14, 0, 0]))),
        "body": _rot(_sleutels((0.0, [5, 6, 0]), (1.2, [5, -6, 0]), (2.4, [5, 6, 0]))),
        "head": _rot(_sleutels((0.0, [4, -6, 0]), (1.2, [4, 6, 0]), (2.4, [4, -6, 0]))),
        "arm_left": _rot(_sleutels((0.0, [20, 0, -6]), (1.2, [-18, 0, -6]), (2.4, [20, 0, -6]))),
        "arm_right": _rot(_sleutels((0.0, [-18, 0, 6]), (1.2, [20, 0, 6]), (2.4, [-18, 0, 6]))),
        "tail": _rot(_sleutels((0.0, [6, -14, 0]), (1.2, [6, 14, 0]), (2.4, [6, -14, 0]))),
        "tail_2": _rot(_sleutels((0.0, [0, -12, 0]), (1.2, [0, 12, 0]), (2.4, [0, -12, 0]))),
        "vleugel_left": _rot(_sleutels((0.0, w["vleugel_left"]), (1.2, [w["vleugel_left"][0], w["vleugel_left"][1] + 8, w["vleugel_left"][2] + 8]),
                                       (2.4, w["vleugel_left"]))),
        "vleugel_right": _rot(_sleutels((0.0, w["vleugel_right"]), (1.2, [w["vleugel_right"][0], w["vleugel_right"][1] - 8, w["vleugel_right"][2] - 8]),
                                        (2.4, w["vleugel_right"]))),
        "vleugel_left_2": _rot(_sleutels((0.0, w["vleugel_left_2"]))), "vleugel_right_2": _rot(_sleutels((0.0, w["vleugel_right_2"]))),
    })
    A["loop"] = (True, L, loop)
    # --- brul: rears back, then the whole of him comes forward with his jaws wide, wings thrown open -----------------------------------
    L = 3.6
    w0, w1 = _vleugels(0.2, 0.1), _vleugels(1.0, 1.0)
    brul = _flakker(L, 2.2)
    brul.update(_zweep_golf(L, 16, slagen=3))

    def wk(naam):
        return _rot(_sleutels((0.0, [0, 0, 0]), (0.5, w0[naam]), (0.95, w1[naam]), (2.9, w1[naam]), (3.6, [0, 0, 0])))
    brul.update({
        "heup": {"position": _sleutels((0.0, [0, 0, 0]), (0.5, [0, -4, 3]), (0.95, [0, 2, -6]), (2.9, [0, 2, -6]), (3.6, [0, 0, 0]))},
        "body": _rot(_sleutels((0.0, [0, 0, 0]), (0.5, [-16, 0, 0]), (0.95, [16, 0, 0]), (2.9, [14, 0, 0]), (3.6, [0, 0, 0]))),
        "borst": {"rotation": _sleutels((0.0, [0, 0, 0]), (0.5, [-10, 0, 0]), (0.95, [10, 0, 0]), (2.9, [8, 0, 0]), (3.6, [0, 0, 0])),
                  "scale": _sleutels((0.0, [1, 1, 1]), (0.5, [1.08, 1.06, 1.1]), (0.95, [1, 1, 1]), (3.6, [1, 1, 1]))},
        "head": _rot(_sleutels((0.0, [0, 0, 0]), (0.5, [-26, 0, 0]), (0.95, [-6, 0, 0]), (1.4, [-6, 10, 4]), (1.9, [-6, -10, -4]), (2.4, [-6, 8, 3]),
                               (2.9, [-6, 0, 0]), (3.6, [0, 0, 0]))),
        "kaak": _rot(_sleutels((0.0, [0, 0, 0]), (0.5, [4, 0, 0]), (0.95, [44, 0, 0]), (2.9, [40, 0, 0]), (3.6, [0, 0, 0]))),
        "arm_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.5, [20, 0, -10]), (0.95, [-24, 0, -42]), (2.9, [-24, 0, -40]), (3.6, [0, 0, 0]))),
        "arm_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.5, [20, 0, 10]), (0.95, [-24, 0, 42]), (2.9, [-24, 0, 40]), (3.6, [0, 0, 0]))),
        "onderarm_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.95, [-28, 0, 0]), (2.9, [-28, 0, 0]), (3.6, [0, 0, 0]))),
        "onderarm_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.95, [-28, 0, 0]), (2.9, [-28, 0, 0]), (3.6, [0, 0, 0]))),
        "tail": _rot(_sleutels((0.0, [0, 0, 0]), (0.95, [22, 0, 0]), (2.9, [22, 0, 0]), (3.6, [0, 0, 0]))),
        "vleugel_left": wk("vleugel_left"), "vleugel_right": wk("vleugel_right"),
        "vleugel_left_2": wk("vleugel_left_2"), "vleugel_right_2": wk("vleugel_right_2"),
        "zwaardvlam": {"scale": _sleutels((0.0, [1, 1, 1]), (0.95, [1.5, 1.5, 1.12]), (2.9, [1.4, 1.4, 1.1]), (3.6, [1, 1, 1]))},
    })
    A["brul"] = (False, L, brul)
    # --- stamp: the right foot comes up and down like a falling pillar -----------------------------------------------------------------
    L = 1.5
    stamp = _flakker(L, 1.6)
    stamp.update({
        "heup": {"position": _sleutels((0.0, [0, 0, 0]), (0.55, [0, 3, 0]), (0.75, [0, -5, 0]), (1.0, [0, 0, 0]), (1.5, [0, 0, 0])),
                 "rotation": _sleutels((0.0, [0, 0, 0]), (0.55, [0, 0, 7]), (0.75, [4, 0, -2]), (1.5, [0, 0, 0]))},
        "leg_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-58, 0, 0]), (0.75, [4, 0, 0]), (1.5, [0, 0, 0]))),
        "scheen_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [50, 0, 0]), (0.75, [-4, 0, 0]), (1.5, [0, 0, 0]))),
        "body": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-8, 0, 0]), (0.75, [14, 0, 0]), (1.5, [0, 0, 0]))),
        "head": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-10, 0, 0]), (0.75, [12, 0, 0]), (1.5, [0, 0, 0]))),
        "arm_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-20, 0, -24]), (0.75, [10, 0, -8]), (1.5, [0, 0, 0]))),
        "arm_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-20, 0, 24]), (0.75, [10, 0, 8]), (1.5, [0, 0, 0]))),
        "vleugel_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [0, 20, 20]), (0.75, [0, 4, 0]), (1.5, [0, 0, 0]))),
        "vleugel_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [0, -20, -20]), (0.75, [0, -4, 0]), (1.5, [0, 0, 0]))),
    })
    A["stamp"] = (False, L, stamp)
    # --- zweep: the left arm goes far back and lashes forward; the worstjes crack like a wave --------------------------------------------
    L = 1.8
    zweep = _flakker(L, 1.8)
    for i, naam in enumerate(ZWEEP):
        v = i * 0.045
        zweep[naam] = _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [round(22 - i * 1.5, 2), 0, 0]), (round(0.8 + v, 3), [round(-34 + i * 1.2, 2), 0, 0]),
                                     (round(1.05 + v, 3), [round(-10 - i * 2.4, 2), 0, 0]), (1.8, [0, 0, 0])))
    zweep.update({
        "body": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-8, -22, 0]), (0.85, [14, 20, 0]), (1.8, [0, 0, 0]))),
        "arm_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [50, 0, -60]), (0.85, [-118, 0, -16]), (1.2, [-70, 0, -10]), (1.8, [0, 0, 0]))),
        "onderarm_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-50, 0, 0]), (0.85, [-6, 0, 0]), (1.8, [0, 0, 0]))),
        "hand_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-30, 0, 0]), (0.85, [-50, 0, 0]), (1.8, [0, 0, 0]))),
        "head": _rot(_sleutels((0.0, [0, 0, 0]), (0.55, [-6, 14, 0]), (0.85, [8, -10, 0]), (1.8, [0, 0, 0]))),
        "kaak": _rot(_sleutels((0.0, [0, 0, 0]), (0.85, [26, 0, 0]), (1.8, [0, 0, 0]))),
        "vleugel_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.85, [0, 24, 22]), (1.8, [0, 0, 0]))),
        "vleugel_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.85, [0, -24, -22]), (1.8, [0, 0, 0]))),
    })
    A["zweep"] = (False, L, zweep)
    # --- zwaard: the blade of fire goes up over his head and comes down -----------------------------------------------------------------
    L = 2.0
    zwaard = _flakker(L, 1.8)
    zwaard.update({
        "body": _rot(_sleutels((0.0, [0, 0, 0]), (0.7, [-14, 14, 0]), (1.0, [22, -10, 0]), (1.5, [18, -8, 0]), (2.0, [0, 0, 0]))),
        "arm_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.7, [-164, 0, 14]), (1.0, [-58, 0, 6]), (1.5, [-50, 0, 6]), (2.0, [0, 0, 0]))),
        "onderarm_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.7, [-30, 0, 0]), (1.0, [8, 0, 0]), (2.0, [0, 0, 0]))),
        "zwaard": _rot(_sleutels((0.0, [0, 0, 0]), (0.7, [-40, 0, 0]), (1.0, [30, 0, 0]), (1.5, [30, 0, 0]), (2.0, [0, 0, 0]))),
        "zwaardvlam": {"scale": _sleutels((0.0, [1, 1, 1]), (0.7, [1.6, 1.6, 1.15]), (1.0, [1.3, 1.3, 1.1]), (2.0, [1, 1, 1]))},
        "head": _rot(_sleutels((0.0, [0, 0, 0]), (0.7, [-14, 0, 0]), (1.0, [16, 0, 0]), (2.0, [0, 0, 0]))),
        "kaak": _rot(_sleutels((0.0, [0, 0, 0]), (0.7, [20, 0, 0]), (1.5, [30, 0, 0]), (2.0, [0, 0, 0]))),
        "vleugel_left": _rot(_sleutels((0.0, [0, 0, 0]), (0.7, [0, 26, 26]), (1.0, [0, 10, 6]), (2.0, [0, 0, 0]))),
        "vleugel_right": _rot(_sleutels((0.0, [0, 0, 0]), (0.7, [0, -26, -26]), (1.0, [0, -10, -6]), (2.0, [0, 0, 0]))),
    })
    A["zwaard"] = (False, L, zwaard)
    # --- donker: a shape in the dark. No flame burns, the wings are wrapped round him, he is bowed: only eyes and cracks glow --------------
    L = 4.0
    uit = {naam: {"scale": _sleutels((0.0, [0, 0, 0]))} for naam in VLAMMEN}
    gebukt = {
        "heup": {"position": _sleutels((0.0, [0, -10, 0]))},
        "leg_left": _rot(_sleutels((0.0, [-20, 0, 0]))), "leg_right": _rot(_sleutels((0.0, [-20, 0, 0]))),
        "scheen_left": _rot(_sleutels((0.0, [34, 0, 0]))), "scheen_right": _rot(_sleutels((0.0, [34, 0, 0]))),
        "voet_left": _rot(_sleutels((0.0, [-14, 0, 0]))), "voet_right": _rot(_sleutels((0.0, [-14, 0, 0]))),
        "body": _rot(_sleutels((0.0, [26, 0, 0]))), "borst": _rot(_sleutels((0.0, [16, 0, 0]))),
        "arm_left": _rot(_sleutels((0.0, [-10, 0, 8]))), "arm_right": _rot(_sleutels((0.0, [-10, 0, -8]))),
        "vleugel_left": _rot(_sleutels((0.0, [0, -30, 30]))), "vleugel_right": _rot(_sleutels((0.0, [0, 30, -30]))),
        "vleugel_left_2": _rot(_sleutels((0.0, [0, -30, 20]))), "vleugel_right_2": _rot(_sleutels((0.0, [0, 30, -20]))),
        "zwaardvlam": {"scale": _sleutels((0.0, [0, 0, 0]))},
    }
    donker = dict(uit)
    donker.update(gebukt)
    donker["head"] = _rot(_sleutels((0.0, [24, 0, 0]), (2.0, [20, 5, 0]), (4.0, [24, 0, 0])))
    donker["borst"] = {"rotation": _sleutels((0.0, [16, 0, 0])), "scale": _sleutels((0.0, [1, 1, 1]), (2.0, [1.03, 1.03, 1.05]), (4.0, [1, 1, 1]))}
    donker["zweep_1"] = {"scale": _sleutels((0.0, [0, 0, 0]))}          # (the whip lies coiled out of sight until it burns)
    A["donker"] = (True, L, donker)
    # --- slaap: the same shape in the dark with his eyes shut (the bridge scene: "two eyes opened" is slaap -> donker) ------------------
    slaap = dict(donker)
    slaap["oog_left"] = {"scale": _sleutels((0.0, [1, 0.06, 1]))}
    slaap["oog_right"] = {"scale": _sleutels((0.0, [1, 0.06, 1]))}
    slaap["head"] = _rot(_sleutels((0.0, [30, 0, 0]), (2.0, [28, 0, 0]), (4.0, [30, 0, 0])))
    A["slaap"] = (True, L, slaap)
    # --- opkomst: out of the dark: the head lifts, the mane catches fire flame by flame, the blade ignites, he rises to his full height
    #     and the wings open like a storm ---------------------------------------------------------------------------------------------------
    L = 7.0
    opkomst = {}
    for i, naam in enumerate(VLAMMEN):
        t0 = 1.2 + (i % 6) * 0.32
        opkomst[naam] = {"scale": _sleutels((0.0, [0, 0, 0]), (round(t0, 2), [0, 0, 0]), (round(t0 + 0.25, 2), [1.3, 1.9, 1.3]), (round(t0 + 0.6, 2), [1, 1, 1]),
                                            (5.2, [1.1, 1.5, 1.1]), (7.0, [1, 1, 1]))}

    def op(naam, laat=3.4):
        g = gebukt[naam]["rotation"]["0.0"]
        return _rot(_sleutels((0.0, g), (laat, g), (5.0, [0, 0, 0]), (7.0, [0, 0, 0])))
    w1 = _vleugels(1.0, 1.0)

    def opw(naam):
        g = gebukt[naam]["rotation"]["0.0"]
        return _rot(_sleutels((0.0, g), (3.6, g), (5.0, w1[naam]), (6.2, w1[naam]), (7.0, [0, 0, 0])))
    opkomst.update({
        "heup": {"position": _sleutels((0.0, [0, -10, 0]), (3.4, [0, -10, 0]), (5.0, [0, 2, 0]), (7.0, [0, 0, 0]))},
        "leg_left": op("leg_left"), "leg_right": op("leg_right"), "scheen_left": op("scheen_left"), "scheen_right": op("scheen_right"),
        "voet_left": op("voet_left"), "voet_right": op("voet_right"),
        "body": _rot(_sleutels((0.0, [26, 0, 0]), (3.4, [22, 0, 0]), (5.0, [-10, 0, 0]), (6.2, [-8, 0, 0]), (7.0, [0, 0, 0]))),
        "borst": {"rotation": _sleutels((0.0, [16, 0, 0]), (3.4, [14, 0, 0]), (5.0, [-8, 0, 0]), (7.0, [0, 0, 0])),
                  "scale": _sleutels((0.0, [1, 1, 1]), (4.2, [1.08, 1.06, 1.1]), (5.0, [1, 1, 1]), (7.0, [1, 1, 1]))},
        "head": _rot(_sleutels((0.0, [24, 0, 0]), (1.0, [4, 0, 0]), (2.2, [0, 12, 0]), (3.2, [0, -12, 0]), (4.2, [-4, 0, 0]), (5.0, [-22, 0, 0]),
                               (6.2, [-18, 0, 0]), (7.0, [0, 0, 0]))),
        "kaak": _rot(_sleutels((0.0, [0, 0, 0]), (4.4, [2, 0, 0]), (5.0, [42, 0, 0]), (6.2, [38, 0, 0]), (7.0, [0, 0, 0]))),
        "arm_left": _rot(_sleutels((0.0, [-10, 0, 8]), (3.4, [-10, 0, 8]), (5.0, [-16, 0, -44]), (6.2, [-16, 0, -40]), (7.0, [0, 0, 0]))),
        "arm_right": _rot(_sleutels((0.0, [-10, 0, -8]), (3.4, [-10, 0, -8]), (5.0, [-16, 0, 44]), (6.2, [-16, 0, 40]), (7.0, [0, 0, 0]))),
        "vleugel_left": opw("vleugel_left"), "vleugel_right": opw("vleugel_right"),
        "vleugel_left_2": opw("vleugel_left_2"), "vleugel_right_2": opw("vleugel_right_2"),
        "zwaardvlam": {"scale": _sleutels((0.0, [0, 0, 0]), (2.6, [0, 0, 0]), (3.0, [1.5, 1.5, 0.5]), (3.5, [1.2, 1.2, 1.1]), (7.0, [1, 1, 1]))},
    })
    opkomst.update({naam: _rot(_sleutels((0.0, [0, 0, 0]), (3.0, [0, 0, 0]), (3.4, [20, 0, 6]), (4.0, [-12, 0, -6]), (7.0, [0, 0, 0]))) for naam in ZWEEP[:5]})
    opkomst["zweep_1"]["scale"] = _sleutels((0.0, [0, 0, 0]), (2.9, [0, 0, 0]), (3.3, [1, 1, 1]), (7.0, [1, 1, 1]))
    A["opkomst"] = (False, L, opkomst)
    # --- wankel: the stone goes from under him: arms flail, wings beat for air that isn't there ---------------------------------------------
    L = 1.2
    wankel = _flakker(L, 2.0)
    wa, wb = _vleugels(1.0, 0.9, 10), _vleugels(0.5, 0.6, -10)
    wankel.update(_zweep_golf(L, 26, slagen=2))
    wankel.update({
        "heup": {"rotation": _sleutels((0.0, [-10, 0, -6]), (0.3, [-16, 0, 6]), (0.6, [-10, 0, -6]), (0.9, [-16, 0, 6]), (1.2, [-10, 0, -6]))},
        "body": _rot(_sleutels((0.0, [-18, 0, 0]), (0.6, [-24, 0, 0]), (1.2, [-18, 0, 0]))),
        "head": _rot(_sleutels((0.0, [-24, 10, 0]), (0.6, [-28, -10, 0]), (1.2, [-24, 10, 0]))),
        "kaak": _rot(_sleutels((0.0, [44, 0, 0]))),
        "arm_left": _rot(_sleutels((0.0, [-120, 0, -50]), (0.3, [-60, 0, -70]), (0.6, [-130, 0, -40]), (0.9, [-70, 0, -70]), (1.2, [-120, 0, -50]))),
        "arm_right": _rot(_sleutels((0.0, [-70, 0, 70]), (0.3, [-130, 0, 40]), (0.6, [-60, 0, 70]), (0.9, [-120, 0, 50]), (1.2, [-70, 0, 70]))),
        "leg_left": _rot(_sleutels((0.0, [-30, 0, 0]), (0.6, [10, 0, 0]), (1.2, [-30, 0, 0]))),
        "leg_right": _rot(_sleutels((0.0, [10, 0, 0]), (0.6, [-30, 0, 0]), (1.2, [10, 0, 0]))),
        "tail": _rot(_sleutels((0.0, [30, -20, 0]), (0.6, [30, 20, 0]), (1.2, [30, -20, 0]))),
        "vleugel_left": _rot(_sleutels((0.0, wa["vleugel_left"]), (0.6, wb["vleugel_left"]), (1.2, wa["vleugel_left"]))),
        "vleugel_right": _rot(_sleutels((0.0, wa["vleugel_right"]), (0.6, wb["vleugel_right"]), (1.2, wa["vleugel_right"]))),
        "vleugel_left_2": _rot(_sleutels((0.0, wa["vleugel_left_2"]), (0.6, wb["vleugel_left_2"]), (1.2, wa["vleugel_left_2"]))),
        "vleugel_right_2": _rot(_sleutels((0.0, wa["vleugel_right_2"]), (0.6, wb["vleugel_right_2"]), (1.2, wa["vleugel_right_2"]))),
    })
    A["wankel"] = (True, L, wankel)
    return {"format_version": "1.8.0", "animations": {
        f"animation.{NAAM}.{naam}": {"loop": lus, "animation_length": lengte, "bones": bones} for naam, (lus, lengte, bones) in A.items()}}


ANIMATIES = ("idle", "loop", "brul", "stamp", "zweep", "zwaard", "donker", "slaap", "opkomst", "wankel")


# =====================================================================================================================
def build(h):
    m = maak()
    h.w(os.path.join(h.A, *GEO, f"{NAAM}.geo.json"), m.geo())
    h.w(os.path.join(h.A, *ANIM, f"{NAAM}.animation.json"), animaties())
    tex, glow = textuur()
    h.save(Image.fromarray(tex), "entity", f"{NAAM}.png")
    h.save(Image.fromarray(glow), "entity", f"{NAAM}_glowmask.png")


def check(h):
    problems = []
    geo = os.path.join(h.A, *GEO, f"{NAAM}.geo.json")
    anim = os.path.join(h.A, *ANIM, f"{NAAM}.animation.json")
    for p in (geo, anim, os.path.join(h.TEX, "entity", f"{NAAM}.png"), os.path.join(h.TEX, "entity", f"{NAAM}_glowmask.png")):
        if not os.path.exists(p):
            problems.append(p)
    if problems:
        return problems
    bones = {b["name"] for b in json.load(open(geo, encoding="utf-8"))["minecraft:geometry"][0]["bones"]}
    for need in ("root", "body", "head", "ear_left", "ear_right", "tail", "arm_left", "arm_right", "kaak", "zwaardvlam"):
        if need not in bones:
            problems.append(f"the model has no bone {need}")
    anims = json.load(open(anim, encoding="utf-8"))["animations"]
    for naam in ANIMATIES:
        if f"animation.{NAAM}.{naam}" not in anims:
            problems.append(f"no animation {naam}")
    for anim_name, a in anims.items():
        for bone in a["bones"]:
            if bone not in bones:
                problems.append(f"{anim_name}: no bone {bone}")
    return problems


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
def _waarde(sleutels, t):
    """A keyframe track at time t (linear)."""
    ks = sorted((float(k), v) for k, v in sleutels.items())
    if t <= ks[0][0]:
        return ks[0][1]
    for (t0, v0), (t1, v1) in zip(ks, ks[1:]):
        if t <= t1:
            f = (t - t0) / max(1e-6, t1 - t0)
            return [a + (b - a) * f for a, b in zip(v0, v1)]
    return ks[-1][1]


def pose(geo_file, anim, t):
    """The model with an animation applied at time t: rotations are added to the bone's own, position moves the bone and its
    children (approximated by shifting cubes and pivots), scale 0 hides the bone's cubes (other scales are ignored)."""
    import copy
    g = copy.deepcopy(geo_file)
    bones = g["minecraft:geometry"][0]["bones"]
    ouders = {b["name"]: b.get("parent") for b in bones}

    def onder(naam, top):
        while naam:
            if naam == top:
                return True
            naam = ouders.get(naam)
        return False
    for naam, sporen in anim["bones"].items():
        b = next(x for x in bones if x["name"] == naam)
        if "rotation" in sporen:
            r = _waarde(sporen["rotation"], t)
            b["rotation"] = [a + c for a, c in zip(b.get("rotation", [0, 0, 0]), r)]
        if "scale" in sporen and max(_waarde(sporen["scale"], t)) < 0.05:
            for x in bones:
                if onder(x["name"], naam):
                    x["cubes"] = []
        if "position" in sporen:
            d = _waarde(sporen["position"], t)
            for x in bones:
                if onder(x["name"], naam):
                    x["pivot"] = [x["pivot"][0] + d[0], x["pivot"][1] + d[1], x["pivot"][2] + d[2]]
                    for c in x["cubes"]:
                        c["origin"] = [c["origin"][0] + d[0], c["origin"][1] + d[1], c["origin"][2] + d[2]]
    return g


def _quads(geo_file):
    import tempfile
    import wiki_renders as wr
    with tempfile.NamedTemporaryFile("w", suffix=".geo.json", delete=False) as f:
        json.dump(geo_file, f)
        pad = f.name
    q = wr.geo_quads(pad, None)
    os.unlink(pad)
    return q


def schilder(geo_file, tex, glow, yaw, pitch=-8, size=900, helder=0.6, achter=(46, 31, 27), zonder=(), kader=None):
    """A picture of the model (orthographic), lit about as the game lights him in his own fire: the skin at `helder`, what is in
    the glow mask at full strength, against a wall of the mine's colour. kader = (x0, y0, x1, y1) in blocks of the turned
    picture: only that part (a close-up)."""
    import copy
    import wiki_renders as wr
    if zonder:
        geo_file = copy.deepcopy(geo_file)
        for b in geo_file["minecraft:geometry"][0]["bones"]:
            if b["name"].startswith(zonder):
                b["cubes"] = []
    quads = _quads(geo_file)
    R = wr.rot_matrix(yaw, pitch)
    hoeken = np.array([R @ (q.origin + a * q.u + b * q.v) for q in quads for a in (0, 1) for b in (0, 1)])
    lo, hi = hoeken[:, :2].min(0), hoeken[:, :2].max(0)
    if kader:
        lo, hi = np.array(kader[:2], float), np.array(kader[2:], float)
    schaal = size * 0.92 / max(hi - lo)
    midden = (lo + hi) / 2
    zon = np.array([-0.35, 0.85, -0.4])
    zon /= np.linalg.norm(zon)
    th, tw = tex.shape[:2]
    PX, PY, PZ, PC = [], [], [], []
    for q in quads:
        if (R @ q.normal)[2] > 1e-6:
            continue
        schaduw = 0.6 + 0.4 * max(0.0, float(np.dot(q.normal, zon)))
        nu = max(2, int(np.linalg.norm(q.u) * schaal * 1.5) + 1)
        nv = max(2, int(np.linalg.norm(q.v) * schaal * 1.5) + 1)
        A, B = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
        S = (q.origin + A[..., None] * q.u + B[..., None] * q.v) @ R.T
        u0, v0, u1, v1 = q.uv
        tu = np.clip((u0 + A * (u1 - u0)) * tw, 0, tw - 1).astype(int)
        tv = np.clip((v0 + B * (v1 - v0)) * th, 0, th - 1).astype(int)
        rgba = tex[tv, tu].astype(np.float32)
        g = glow[tv, tu].astype(np.float32)
        k = g[..., 3:4] / 255.0
        c = rgba[..., :3] * helder * schaduw * (1 - k) + np.clip(np.maximum(g[..., :3], rgba[..., :3] * helder * schaduw) * 1.1, 0, 255) * k
        px = ((S[..., 0] - midden[0]) * schaal + size / 2).astype(int)
        py = (-(S[..., 1] - midden[1]) * schaal + size / 2).astype(int)
        m = (rgba[..., 3] > 20) & (px >= 0) & (px < size) & (py >= 0) & (py < size)
        PX.append(px[m])
        PY.append(py[m])
        PZ.append(S[..., 2][m])
        PC.append(c[m])
    px, py, pz, pc = np.concatenate(PX), np.concatenate(PY), np.concatenate(PZ), np.concatenate(PC)
    volgorde = np.argsort(-pz, kind="stable")                    # far first: the nearest is written last
    beeld = np.zeros((size, size, 3), np.float32) + np.array(achter, np.float32)
    beeld[py[volgorde], px[volgorde]] = pc[volgorde]
    return Image.fromarray(np.clip(beeld, 0, 255).astype(np.uint8))


def preview(out, alleen=()):
    os.makedirs(out, exist_ok=True)
    m = maak()
    geo_file = m.geo()
    tex, glow = textuur()
    Image.fromarray(tex).save(os.path.join(out, "atlas.png"))
    Image.fromarray(glow).save(os.path.join(out, "atlas_gloed.png"))
    anims = animaties()["animations"]

    def a(naam, t):
        return pose(geo_file, anims[f"animation.{NAAM}.{naam}"], t)
    kaal = ("vleugel", "rib_", "vinger_", "zweep", "vlam_zweep", "vlam_vleugel")
    shots = {
        "rust_voor": (geo_file, 25, -8, {}), "rust_zij": (geo_file, 100, -6, {}), "rust_achter": (geo_file, 205, -8, {}),
        "rust_voor2": (geo_file, -30, -8, {}), "rust_recht": (geo_file, 0, -4, {}),
        "laag": (geo_file, 18, 16, {}),                                             # from below, as a guh sees him
        "kop": (geo_file, 22, -4, {"kader": (-3.6, 6.4, 3.0, 13.0)}),
        "kop_recht": (geo_file, 0, 0, {"kader": (-3.0, 6.6, 3.0, 12.6)}),
        "kop_laag": (a("brul", 1.2), 14, 14, {"kader": (-3.6, 5.0, 3.6, 12.2)}),
        "lijf_voor": (geo_file, 20, -6, {"zonder": kaal}), "lijf_zij": (geo_file, 90, -6, {"zonder": kaal}),
        "donker": (a("donker", 0.0), 20, -6, {"helder": 0.1, "achter": (10, 7, 7)}),
        "brul": (a("brul", 1.0), 25, -6, {}), "brul_voor": (a("brul", 1.0), 0, 6, {}), "brul_achter": (a("brul", 1.0), 180, -6, {}),
        "zweep": (a("zweep", 0.85), 40, -6, {}), "zwaard": (a("zwaard", 0.7), 25, -6, {}), "loop": (a("loop", 0.0), 70, -6, {}),
        "stamp": (a("stamp", 0.55), 40, -6, {}), "opkomst5": (a("opkomst", 5.0), 15, 4, {}), "wankel": (a("wankel", 0.3), 30, -6, {}),
    }
    for naam, (gf, yaw, pitch, kw) in shots.items():
        if alleen and naam not in alleen:
            continue
        schilder(gf, tex, glow, yaw, pitch, **kw).save(os.path.join(out, f"{NAAM}_{naam}.png"))
    bones = geo_file["minecraft:geometry"][0]["bones"]
    print("bones:", len(bones), "cubes:", sum(len(b["cubes"]) for b in bones))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".", alleen=sys.argv[2:])
