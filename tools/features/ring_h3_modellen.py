"""
bbq2 (ring-h3) - the Barbecuerog: model, textures and animations of the entity guhs:barbecuerog.

DESIGN_130 4 (user decision 2026-10-06): a big, dangerous, devilish BALROG first: a towering horned demon of black charcoal and
cracked glowing embers, fire in the cracks, a burning mane, great smoke-and-ember wings, heavy claws, a flaming whip and a blade
of fire, glowing eyes. Only SLIGHT Mika traits: the two round ears (charred and torn, glowing inside) under the horns, the
little blunt nose and one crooked fang. The whip is a string of knotted braadworstjes (the one small joke) and his belly is a
furnace behind grill bars.

The model is built at its real size (no renderer scale): 16 units = one block; he stands about 9.6 blocks to the top of his
head, 11.5 to the tips of his horns, and his wings span about 19 blocks when they are spread. Every cube has per-face UVs into
one 512 x 512 atlas of painted material tiles (1 px per unit, so 16 px per block like the rest of the game); the glow mask
(<name>_glowmask.png, GeckoLib's AutoGlowingGeoLayer) holds the cracks, the eyes, the embers and every flame.

  build(h)        geckolib/models/entity/barbecuerog.geo.json, geckolib/animations/entity/barbecuerog.animation.json,
                  textures/entity/barbecuerog.png + barbecuerog_glowmask.png
  check(h)        the files exist and every animated bone is a bone of the model
  preview(out)    pictures from four sides, in the dark, and in the key poses of the animations
                  (python tools/features/ring_h3_modellen.py <out>, from the worktree root)

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

# material -> (x, y, w, h) in the atlas
TEGELS = {
    "kool": (0, 0, 128, 128),          # black charcoal, many glowing cracks
    "kool2": (128, 0, 128, 128),       # darker, few cracks (backs, limbs)
    "gloed": (256, 0, 128, 128),       # glowing coals
    "hoorn": (384, 0, 128, 128),       # ridged horn of gloeikool
    "vleugel": (0, 128, 128, 128),     # the smoke membrane of a wing (torn, see-through bites)
    "vleugel2": (128, 128, 128, 128),  # the outer membrane
    "vlam_a": (256, 128, 64, 128),     # a flame tongue
    "vlam_b": (320, 128, 64, 128),
    "zwaard": (384, 128, 64, 128),     # the blade of fire
    "zwaard2": (448, 128, 64, 128),
    "worst": (0, 256, 64, 64),         # a braadworstje of the whip
    "knoop": (64, 256, 32, 32),        # the knot between two worstjes
    "oog": (96, 256, 32, 32),
    "tand": (128, 256, 32, 64),
    "oor": (160, 256, 64, 64),         # the inside of a Mika ear: ember pink
    "rooster": (224, 256, 32, 64),     # a grill bar
    "klauw": (256, 256, 32, 64),
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

    def _uv(self, mat, fw, fh, stretch):
        x, y, w, h = TEGELS[mat]
        if stretch:
            return {"uv": [x, y], "uv_size": [w, h]}
        pw, ph = max(1.0, min(float(fw), w)), max(1.0, min(float(fh), h))
        u = x + int(self.rng.integers(0, max(1, int(w - pw) + 1)))
        v = y + int(self.rng.integers(0, max(1, int(h - ph) + 1)))
        return {"uv": [u, v], "uv_size": [round(pw, 2), round(ph, 2)]}

    def cube(self, bone, origin, size, mat, faces="nsewud", stretch=False, inflate=0.0, mats=None):
        """A cube of `mat`; `faces` = which sides are drawn (n s e w u d); mats = {face letter: material} for exceptions."""
        sx, sy, sz = size
        dims = {"n": (sx, sy), "s": (sx, sy), "e": (sz, sy), "w": (sz, sy), "u": (sx, sz), "d": (sx, sz)}
        names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
        uv = {}
        for f in faces:
            m = (mats or {}).get(f, mat)
            uv[names[f]] = self._uv(m, dims[f][0], dims[f][1], stretch)
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
    # --- the hips and the legs ----------------------------------------------------------------------------------------------
    m.bone("heup", "root", [0, 70, 4])
    m.cube("heup", [-23, 60, -11], [46, 20, 26], "kool2")
    m.cube("heup", [-17, 54, -9], [34, 8, 22], "kool2")
    # a loincloth of charred hide plates on a chain of grill iron
    m.cube("heup", [-24, 74, -12.5], [48, 4, 29], "rooster")
    for i, (x, hoog) in enumerate(((-13, 22), (-4, 28), (5, 21))):
        m.cube("heup", [x, 74 - hoog, -13.4], [8, hoog, 1.6], "kool2")
        m.cube("heup", [x + 2.5, 74 - hoog - 2, -13.6], [3, 3, 2], "gloed")
    for kant in (1, -1):
        s = "left" if kant > 0 else "right"

        def k(origin, size):
            return (origin, size) if kant > 0 else _sp(origin, size)
        # thigh (knee forward), shin (ankle back), a great clawed foot
        m.bone(f"leg_{s}", "heup", [kant * 15, 68, 2], rotation=[-16, 0, kant * -3])
        o, sz = k([5, 34, -10], [21, 36, 23])
        m.cube(f"leg_{s}", o, sz, "kool")
        o, sz = k([7, 30, -13], [17, 9, 6])
        m.cube(f"leg_{s}", o, sz, "kool2")                                   # the knee cap
        o, sz = k([13, 36, -17], [5, 5, 6])
        m.cube(f"leg_{s}", o, sz, "hoorn")                                   # a knee spike
        m.bone(f"scheen_{s}", f"leg_{s}", [kant * 15.5, 36, 0], rotation=[34, 0, 0])
        o, sz = k([8, 8, -8], [15, 30, 16])
        m.cube(f"scheen_{s}", o, sz, "kool2")
        o, sz = k([9.5, 12, 7], [12, 20, 4])
        m.cube(f"scheen_{s}", o, sz, "kool")                                 # the calf
        m.bone(f"voet_{s}", f"scheen_{s}", [kant * 15.5, 9, 0], rotation=[-18, 0, 0])
        o, sz = k([4.5, 0, -16], [22, 10, 28])
        m.cube(f"voet_{s}", o, sz, "kool")
        for i in range(3):
            o, sz = k([5.5 + i * 7.5, 0, -25], [5, 6, 10])
            m.cube(f"voet_{s}", o, sz, "klauw", stretch=True)
        o, sz = k([12, 2, 11], [7, 6, 7])
        m.cube(f"voet_{s}", o, sz, "klauw", stretch=True)                    # the heel spur
    # --- the tail ------------------------------------------------------------------------------------------------------------
    m.bone("tail", "heup", [0, 72, 14], rotation=[-38, 0, 0])
    m.cube("tail", [-8, 64, 12], [16, 15, 34], "kool")
    m.bone("tail_2", "tail", [0, 71, 44], rotation=[22, 0, 0])
    m.cube("tail_2", [-6, 65, 44], [12, 12, 32], "kool2")
    m.bone("tail_3", "tail_2", [0, 71, 74], rotation=[26, 0, 0])
    m.cube("tail_3", [-4, 66.5, 74], [8, 9, 28], "kool")
    m.cube("tail_3", [-5.5, 65, 100], [11, 12, 10], "gloed")                 # the ember club at the end
    m.cube("tail_3", [-1.5, 76, 102], [3, 6, 3], "hoorn")
    m.cube("tail_3", [-8.5, 69, 103], [3, 3, 4], "hoorn")
    m.cube("tail_3", [5.5, 69, 103], [3, 3, 4], "hoorn")
    m.bone("vlam_staart", "tail_3", [0, 77, 105])
    m.vlam("vlam_staart", 0, 77, 105, 12, 20, "vlam_b")
    # --- the body: belly (a furnace behind grill bars) and the chest -----------------------------------------------------------
    m.bone("body", "heup", [0, 78, 4], rotation=[9, 0, 0])
    m.cube("body", [-20, 76, -11], [40, 26, 25], "kool2")
    m.cube("body", [-11, 80, -12.2], [22, 17, 2], "gloed")                   # the fire in his belly
    for i in range(5):
        m.cube("body", [-10.4 + i * 4.9, 79, -13.2], [1.6, 19, 1.4], "rooster", stretch=True)
    m.cube("body", [-12.5, 96.5, -13.4], [25, 2.2, 2], "rooster")
    m.cube("body", [-12.5, 77.5, -13.4], [25, 2.2, 2], "rooster")
    m.bone("borst", "body", [0, 102, 4], rotation=[7, 0, 0])
    m.cube("borst", [-31, 100, -15], [62, 36, 34], "kool")
    m.cube("borst", [-26, 104, -17.5], [23, 22, 3], "kool2")                 # the pectoral plates, a glowing rift between them
    m.cube("borst", [3, 104, -17.5], [23, 22, 3], "kool2")
    m.cube("borst", [-3, 103, -16.2], [6, 25, 1.4], "gloed")
    m.cube("borst", [-22, 134, -10], [44, 6, 24], "kool2")                   # the trapezius
    for i, y in enumerate((106, 113, 120)):
        m.cube("borst", [-31.6, y, -9], [1, 2.4, 22], "gloed")               # glowing ribs in his flanks
        m.cube("borst", [30.6, y, -9], [1, 2.4, 22], "gloed")
    # back ridge: charcoal spines down the spine
    m.cube("borst", [-21, 122, 12], [42, 16, 12], "kool2")                   # the hump of his back
    for i, (y, z, hoog) in enumerate(((132, 21, 14), (118, 23, 12), (104, 19, 9))):
        m.cube("borst", [-3.5, y, z], [7, hoog, 7], "hoorn")
    m.cube("body", [-2.5, 88, 14], [5, 8, 5], "hoorn")
    # --- shoulders, arms, hands -------------------------------------------------------------------------------------------------
    for kant in (1, -1):
        s = "left" if kant > 0 else "right"

        def k(origin, size):
            return (origin, size) if kant > 0 else _sp(origin, size)
        m.bone(f"arm_{s}", "borst", [kant * 37, 128, 2], rotation=[6, 0, kant * -11])
        o, sz = k([29, 118, -11], [23, 23, 27])
        m.cube(f"arm_{s}", o, sz, "kool")                                    # the shoulder
        for (sx, sy, szz, w, hg) in ((34, 141, -5, 6, 12), (44, 139, 3, 5, 9), (38, 140, 9, 4, 7)):
            o, sz = k([sx, sy, szz], [w, hg, w])
            m.cube(f"arm_{s}", o, sz, "hoorn")
        o, sz = k([33, 92, -7], [17, 28, 18])
        m.cube(f"arm_{s}", o, sz, "kool2")
        m.bone(f"onderarm_{s}", f"arm_{s}", [kant * 41.5, 93, 2], rotation=[-22, 0, kant * 5])
        o, sz = k([31.5, 60, -9], [20, 35, 21])
        m.cube(f"onderarm_{s}", o, sz, "kool")
        o, sz = k([39, 88, 10], [5, 6, 9])
        m.cube(f"onderarm_{s}", o, sz, "hoorn")                              # the elbow spike
        o, sz = k([51, 68, -3], [2, 20, 8])
        m.cube(f"onderarm_{s}", o, sz, "gloed")                              # a rift down the forearm
        m.bone(f"hand_{s}", f"onderarm_{s}", [kant * 41.5, 60, 1], rotation=[-8, 0, 0])
        o, sz = k([30.5, 46, -10], [22, 15, 22])
        m.cube(f"hand_{s}", o, sz, "kool2")
        for i in range(3):
            o, sz = k([31.5 + i * 7.5, 34, -12], [5, 13, 6])
            m.cube(f"hand_{s}", o, sz, "klauw", stretch=True)
        o, sz = k([47, 37, 3], [5, 10, 6])
        m.cube(f"hand_{s}", o, sz, "klauw", stretch=True)                    # the thumb claw
    # --- the whip of knotted braadworstjes, burning (left hand) ------------------------------------------------------------------
    ouder = "hand_left"
    wx, wy, wz = 41.5, 46, -2
    for i in range(11):
        naam = f"zweep_{i + 1}"
        m.bone(naam, ouder, [wx, wy, wz], rotation=[ZWEEP_X[i], ZWEEP_Y[i], 0])
        m.cube(naam, [wx - 2.6, wy - 12, wz - 2.6], [5.2, 11.2, 5.2], "worst")
        m.cube(naam, [wx - 1.6, wy - 13, wz - 1.6], [3.2, 2.0, 3.2], "knoop")
        if i in (2, 5, 8, 10):
            m.bone(f"vlam_zweep_{i + 1}", naam, [wx, wy - 6, wz])
            m.vlam(f"vlam_zweep_{i + 1}", wx, wy - 8, wz, 9, 14, "vlam_a" if i % 2 else "vlam_b")
        ouder, wy = naam, wy - 13
    # --- the blade of fire (right hand) ------------------------------------------------------------------------------------------
    m.bone("zwaard", "hand_right", [-41.5, 50, -2], rotation=[-12, 0, 0])
    m.cube("zwaard", [-44, 47.5, 6], [5, 5, 12], "kool2")                    # the pommel end of the grip
    m.cube("zwaard", [-44, 47.5, -14], [5, 5, 20], "kool2")
    m.cube("zwaard", [-51, 45, -18], [19, 10, 4.5], "hoorn")                 # the guard
    m.bone("zwaardvlam", "zwaard", [-41.5, 50, -18])
    m.cube("zwaardvlam", [-43.1, 48.4, -96], [3.2, 3.2, 78], "gloed")        # the white-hot core
    for z0, lang, breed in ((-48, 30, 15), (-80, 32, 11), (-108, 28, 7)):
        m.cube("zwaardvlam", [-41.5 - breed / 2, 49.9, z0], [breed, 0.2, lang], "zwaard", faces="ud", stretch=True)
        m.cube("zwaardvlam", [-41.6, 50 - breed / 2, z0], [0.2, breed, lang], "zwaard2", faces="ew", stretch=True)
    # --- the head ---------------------------------------------------------------------------------------------------------------
    m.bone("head", "borst", [0, 136, -8], rotation=[-10, 0, 0])
    m.cube("head", [-13, 126, -13], [26, 12, 18], "kool2")                   # the thick neck
    m.cube("head", [-17, 134, -34], [34, 26, 30], "kool")                    # the skull
    m.cube("head", [-18.5, 152, -37], [37, 7, 10], "kool2")                  # the brow: a heavy overhang
    m.cube("head", [-9.5, 149.5, -36.4], [8, 3.4, 3], "kool2")               # and its angry inner corners
    m.cube("head", [1.5, 149.5, -36.4], [8, 3.4, 3], "kool2")
    m.cube("head", [-9, 134, -43], [18, 11.5, 10], "kool")                    # the blunt snout (a Mika's, grown monstrous)
    m.cube("head", [-2.5, 141.6, -44.2], [5, 3.4, 1.6], "oor")                   # his little nose still glows pink
    for kant in (1, -1):                                                     # eyes: slanted slits of white fire
        for o, sz in (([5.4, 145.4, -35.2], [5.6, 4.2, 1.6]), ([10.6, 146.8, -35.2], [6.4, 4.8, 1.6])):
            o, sz = (o, sz) if kant > 0 else _sp(o, sz)
            m.cube("head", o, sz, "oog", stretch=True)
    m.cube("head", [-17.5, 137, -32], [1.2, 10, 16], "gloed")                # glowing cheeks
    m.cube("head", [16.3, 137, -32], [1.2, 10, 16], "gloed")
    m.cube("head", [-4, 160, -30], [8, 3, 24], "hoorn")                      # a crest over the skull
    for x, lang in ((-7.6, 5), (-1.4, 4), (4.8, 5.6)):
        m.cube("head", [x, 134 - lang, -42.6], [2.8, lang, 2.8], "tand", stretch=True)
    m.cube("head", [-12.6, 124.5, -41.4], [3.8, 11, 3.8], "tand", stretch=True)   # one long crooked fang, as every Mika has
    m.bone("kaak", "head", [0, 135, -14], rotation=[12, 0, 0])
    m.cube("kaak", [-11, 124.5, -41], [22, 6, 28], "kool2", mats={"u": "gloed"})
    for x in (-4.6, 1.8, 7.6):
        m.cube("kaak", [x, 130.5, -40.4], [2.8, 4.6, 2.8], "tand", stretch=True)
    m.cube("kaak", [-7, 121.5, -39], [14, 3.2, 9], "kool")                   # the chin
    # horns of gloeikool: a chain of turned pieces, sweeping out, then up and forward like a bull's
    for kant in (1, -1):
        s = "left" if kant > 0 else "right"

        def k(origin, size):
            return (origin, size) if kant > 0 else _sp(origin, size)
        m.bone(f"hoorn_{s}", "head", [kant * 15, 154, -18], rotation=[0, 0, kant * 62])
        o, sz = k([7, 150, -26], [16, 20, 16])
        m.cube(f"hoorn_{s}", o, sz, "hoorn")
        m.bone(f"hoorn_{s}_2", f"hoorn_{s}", [kant * 15, 170, -18], rotation=[-12, 0, kant * -26])
        o, sz = k([9, 167, -24], [12, 21, 12])
        m.cube(f"hoorn_{s}_2", o, sz, "hoorn")
        m.bone(f"hoorn_{s}_3", f"hoorn_{s}_2", [kant * 15, 188, -18], rotation=[-20, 0, kant * -28])
        o, sz = k([10.5, 185, -22.5], [9, 20, 9])
        m.cube(f"hoorn_{s}_3", o, sz, "hoorn")
        m.bone(f"hoorn_{s}_4", f"hoorn_{s}_3", [kant * 15, 205, -18], rotation=[-24, 0, kant * -20])
        o, sz = k([12, 202, -21], [6, 17, 6])
        m.cube(f"hoorn_{s}_4", o, sz, "gloed")
        o, sz = k([13.2, 218, -19.8], [3.6, 9, 3.6])
        m.cube(f"hoorn_{s}_4", o, sz, "gloed")
        # the Mika ears: round, charred, glowing inside, sticking out under the horns
        m.bone(f"ear_{s}", "head", [kant * 17, 141, -12], rotation=[0, kant * -16, kant * 74])
        o, sz = k([10, 140, -13.5], [14, 12, 2.4])
        m.cube(f"ear_{s}", o, sz, "kool2")
        o, sz = k([12, 151, -13.5], [10, 4.5, 2.4])
        m.cube(f"ear_{s}", o, sz, "kool2")
        o, sz = k([13, 143, -14.1], [8, 8, 0.8])
        m.cube(f"ear_{s}", o, sz, "oor")
    # --- the burning mane: on the crown, down the neck and the back ---------------------------------------------------------------
    manen = (("head", 0, 162, -28, 14, 24), ("head", -10, 160, -18, 12, 24), ("head", 10, 160, -18, 12, 24), ("head", 0, 162, -10, 18, 36),
             ("head", 0, 150, -2, 17, 34), ("borst", 0, 138, 6, 18, 34), ("borst", -12, 137, 4, 13, 24), ("borst", 12, 137, 4, 13, 24),
             ("borst", 0, 136, 16, 16, 28), ("arm_left", 38, 141, 2, 11, 20), ("arm_right", -38, 141, 2, 11, 20), ("body", 0, 96, 15, 12, 20))
    for i, (bot, x, y, z, breed, hoog) in enumerate(manen):
        m.bone(f"vlam_{i + 1}", bot, [x, y, z])
        m.vlam(f"vlam_{i + 1}", x, y, z, breed, hoog, "vlam_a" if i % 2 == 0 else "vlam_b")
    # --- the wings of smoke and embers -------------------------------------------------------------------------------------------
    for kant in (1, -1):
        s = "left" if kant > 0 else "right"

        def k(origin, size):
            return (origin, size) if kant > 0 else _sp(origin, size)
        m.bone(f"vleugel_{s}", "borst", [kant * 13, 132, 19], rotation=[0, kant * -38, kant * -42])
        o, sz = k([10, 129, 16], [78, 7, 7])
        m.cube(f"vleugel_{s}", o, sz, "kool2")                               # the arm of the wing
        o, sz = k([13, 52, 19.4], [75, 78, 0.3])
        m.cube(f"vleugel_{s}", o, sz, "vleugel", faces="ns", stretch=True)
        o, sz = k([84, 134, 15], [6, 12, 6])
        m.cube(f"vleugel_{s}", o, sz, "hoorn")                               # the claw on the wrist
        m.bone(f"vleugel_{s}_2", f"vleugel_{s}", [kant * 88, 132, 19], rotation=[0, kant * 14, kant * 58])
        o, sz = k([88, 130, 17], [84, 5, 5])
        m.cube(f"vleugel_{s}_2", o, sz, "kool2")
        o, sz = k([88, 46, 19.6], [86, 86, 0.3])
        m.cube(f"vleugel_{s}_2", o, sz, "vleugel2", faces="ns", stretch=True)
        o, sz = k([170, 133, 17.5], [4, 9, 4])
        m.cube(f"vleugel_{s}_2", o, sz, "gloed")
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


def _kool(rng, n, donker, barsten):
    basis = 18 + _ruis(rng, n, 16) * 16 + _ruis(rng, n, 4) * 12 + rng.random((n, n)) * 7
    basis *= donker
    rgb = np.stack([basis * 1.06, basis * 0.98, basis * 0.98], -1)
    # facets: darker seams like split charcoal
    naad = _barsten(rng, n, 10, 40)
    rgb *= (1 - 0.45 * naad)[..., None]
    b = _barsten(rng, n, barsten, 34)
    heet = np.clip(b + _vervaag(b, 1) * 1.2 + _vervaag(b, 3) * 1.4, 0, 1.6)
    kern = _gloei(np.clip(b * 0.95 + _vervaag(b, 1) * 0.5, 0, 1))
    gloeiend = np.clip(heet, 0, 1)
    rgb = rgb * (1 - gloeiend[..., None]) + (_gloei(heet * 0.62) * gloeiend[..., None])
    rgb = np.where((b > 0.5)[..., None], kern, rgb)
    return rgb, np.clip(heet * 1.1, 0, 1)


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


def _vleugel(rng, n, buiten):
    """The smoke membrane: solid along the top (the arm of the wing), torn into long rags at the bottom, glowing veins and rims."""
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    rook = 16 + _ruis(rng, n, 16) * 26 + _ruis(rng, n, 4) * 10
    rgb = np.stack([rook * 1.02, rook * 0.96, rook * 1.0], -1)
    # the torn lower edge: a deep scalloped rag line between the fingers
    vingers = 4
    rand_y = np.zeros(n)
    for x in range(n):
        f = (x / n * vingers) % 1.0
        boog = math.sin(f * math.pi) ** 0.7                    # 0 at a finger, 1 between two
        rand_y[x] = n - 4 - boog * n * (0.34 if not buiten else 0.42) - (math.sin(x * 0.9) + math.sin(x * 2.3)) * 2.2
    if buiten:
        rand_y -= np.linspace(0, n * 0.22, n)                  # the outer membrane tapers towards the tip
    alpha = (yy < rand_y[None, :]).astype(np.float32)
    # bites and burn holes
    for _ in range(9 if buiten else 6):
        cx, cy, r = rng.uniform(8, n - 8), rng.uniform(n * 0.25, n * 0.8), rng.uniform(2.5, 6.5)
        gat = (xx - cx) ** 2 + ((yy - cy) * 0.8) ** 2 < r * r
        alpha[gat] = 0
    # veins: the fingers, fanning down from the top corner
    heet = np.zeros((n, n), np.float32)
    for i in range(vingers + 1):
        x_onder = i / vingers * (n - 1)
        for y in range(n):
            x = int(x_onder * (0.25 + 0.75 * y / n))
            if 0 <= x < n:
                heet[y, max(0, x - 1):x + 2] = 0.34
    # glowing rims where it is torn or burnt
    rand = (alpha > 0) & ((np.roll(alpha, -1, 0) == 0) | (np.roll(alpha, 1, 1) == 0) | (np.roll(alpha, -1, 1) == 0) | (np.roll(alpha, -2, 0) == 0))
    heet[rand] = 0.62
    heet = np.clip(heet + _vervaag(heet, 1) * 0.5, 0, 1)
    # embers drifting in the smoke
    for _ in range(26):
        sx, sy = int(rng.integers(2, n - 2)), int(rng.integers(n // 5, n - 2))
        heet[sy, sx] = 0.95
    rgb = rgb * (1 - heet[..., None]) + _gloei(0.16 + heet * 0.62) * heet[..., None]
    # thinner, lighter smoke towards the rags
    dun = np.clip((yy - n * 0.45) / (n * 0.55), 0, 1)
    rgb = rgb * (1 - dun[..., None] * 0.3) + np.array([52, 44, 48]) * dun[..., None] * 0.3
    return rgb, alpha * 255, heet


def textuur(seed=21301851):
    rng = np.random.default_rng(seed)
    arr = np.zeros((ATLAS, ATLAS, 4), np.float32)
    glow = np.zeros((ATLAS, ATLAS, 4), np.float32)
    n = TEGEL
    rgb, heet = _kool(rng, n, 1.0, 16)
    _zet(arr, glow, "kool", rgb, gloed=heet)
    rgb, heet = _kool(rng, n, 0.78, 6)
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
    for mat, buiten in (("vleugel", False), ("vleugel2", True)):
        rgb, alpha, heet = _vleugel(rng, n, buiten)
        _zet(arr, glow, mat, rgb, alpha=alpha, gloed=heet)
    for mat, wit, slank in (("vlam_a", 0.3, 1.0), ("vlam_b", 0.2, 0.86)):
        rgb, alpha = _vlam(rng, 64, 128, wit, slank)
        _zet(arr, glow, mat, rgb, alpha=alpha, gloed=np.ones((128, 64)))
    for mat in ("zwaard", "zwaard2"):
        # the blade: a strip of fire, ragged on both edges, white-hot along its axis
        yy2, xx2 = np.mgrid[0:128, 0:64].astype(np.float32)
        rafel = 24 + np.sin(yy2 * 0.55 + rng.uniform(0, 6)) * 4 + np.sin(yy2 * 1.7 + rng.uniform(0, 6)) * 2.5 + rng.random((128, 64)) * 3
        d = np.abs(xx2 - 31.5) / rafel
        rgb = _gloei(np.clip(1.02 - d * 0.62, 0, 1))
        _zet(arr, glow, mat, rgb, alpha=np.where(d < 1.0, 255, 0), gloed=np.ones((128, 64)))
    # a braadworstje: brown-red, a shine along it, dark grill stripes, split and glowing where it burst
    yy, xx = np.mgrid[0:64, 0:64].astype(np.float32)
    worst = np.stack([150 + _ruis(rng, 64, 8) * 40, 70 + _ruis(rng, 64, 8) * 26, 38 + _ruis(rng, 64, 4) * 14], -1)
    glans = np.clip(1 - np.abs((xx % 16) - 5) / 3.0, 0, 1)[..., None]
    worst = worst + glans * np.array([50, 40, 26])
    streep = ((yy + xx * 0.5) % 9 < 2.2)[..., None]
    worst = np.where(streep, worst * 0.36, worst)
    barst = _barsten(rng, 64, 3, 16)
    worst = np.where((barst > 0.5)[..., None], _gloei(np.full((64, 64), 0.8)), worst)
    _zet(arr, glow, "worst", worst, gloed=np.clip(barst + _vervaag(barst, 1), 0, 1))
    _zet(arr, glow, "knoop", np.full((32, 32, 3), (64, 30, 18), np.float32) + rng.random((32, 32, 1)) * 16)
    # the eye: white fire, an orange rim, a slit
    yy, xx = np.mgrid[0:32, 0:32].astype(np.float32)
    d = np.abs(yy - 15.5) / 15.5
    oog = _gloei(1.0 - d * 0.45)
    oog[:, 14:18] = (255, 120, 20)
    oog[4:28, 15:17] = (90, 14, 0)
    _zet(arr, glow, "oog", oog, gloed=np.ones((32, 32)))
    # tooth and claw: charred bone, glowing at the root (the top of the tile = the tip)
    yy, xx = np.mgrid[0:64, 0:32].astype(np.float32)
    t = yy / 63.0
    for mat, tip, wortel in (("tand", (214, 204, 178), (120, 60, 30)), ("klauw", (30, 26, 28), (150, 60, 20))):
        rgb = np.array(tip)[None, None, :] * (1 - t[..., None]) + np.array(wortel)[None, None, :] * t[..., None]
        rgb = rgb * (0.86 + rng.random((64, 32, 1)) * 0.2)
        heet = np.clip((t - 0.72) * 3.2, 0, 1)
        rgb = rgb * (1 - heet[..., None]) + _gloei(0.3 + heet * 0.5) * heet[..., None]
        _zet(arr, glow, mat, rgb, gloed=heet)
    # the inside of an ear, the nose: ember pink (what is left of a Mika)
    cel = _ruis(rng, 64, 8)
    oor = np.stack([150 + cel * 60, 34 + cel * 40, 30 + cel * 30], -1)
    _zet(arr, glow, "oor", oor, gloed=np.full((64, 64), 0.5))
    # a grill bar: dull iron, hot in the middle
    yy, xx = np.mgrid[0:64, 0:32].astype(np.float32)
    ijzer = np.full((64, 32, 3), 40, np.float32) + rng.random((64, 32, 1)) * 14
    heet = np.clip(1 - np.abs(yy - 32) / 22.0, 0, 1) * 0.5
    ijzer = ijzer * (1 - heet[..., None]) + _gloei(heet + 0.1) * heet[..., None]
    _zet(arr, glow, "rooster", ijzer, gloed=heet * 0.8)
    return np.clip(arr, 0, 255).astype(np.uint8), np.clip(glow, 0, 255).astype(np.uint8)


# =====================================================================================================================
# the animations
# =====================================================================================================================
VLAMMEN = [f"vlam_{i}" for i in range(1, 13)] + ["vlam_staart", "vlam_zweep_3", "vlam_zweep_6", "vlam_zweep_9", "vlam_zweep_11"]
ZWEEP = [f"zweep_{i}" for i in range(1, 12)]


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
    A["donker"] = (True, L, donker)
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


ANIMATIES = ("idle", "loop", "brul", "stamp", "zweep", "zwaard", "donker", "opkomst", "wankel")


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


def preview(out, alleen=()):
    import tempfile
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    m = maak()
    geo_file = m.geo()
    tex, glow = textuur()
    a = tex.astype(np.float32)
    g = glow.astype(np.float32)
    k = (g[..., 3:4] / 255.0)
    licht = a.copy()
    licht[..., :3] = np.clip(a[..., :3] * (1 - k) + np.clip(g[..., :3] * 1.25, 0, 255) * k, 0, 255)
    nacht = a.copy()
    nacht[..., :3] = np.clip(a[..., :3] * 0.22 * (1 - k) + np.clip(g[..., :3] * 1.3, 0, 255) * k, 0, 255)
    Image.fromarray(tex).save(os.path.join(out, "atlas.png"))
    Image.fromarray(glow).save(os.path.join(out, "atlas_gloed.png"))
    anims = animaties()["animations"]

    def teken(naam, gf, t_arr, yaw, pitch=-8, size=560, zonder=()):
        if zonder:
            import copy
            gf = copy.deepcopy(gf)
            for b in gf["minecraft:geometry"][0]["bones"]:
                if b["name"].startswith(zonder):
                    b["cubes"] = []
        with tempfile.NamedTemporaryFile("w", suffix=".geo.json", delete=False) as f:
            json.dump(gf, f)
            pad = f.name
        q = wr.geo_quads(pad, t_arr)
        os.unlink(pad)
        img = wr.render(q, yaw=yaw, pitch=pitch, size=size, ss=1)
        vel = Image.new("RGBA", img.size, (16, 12, 14, 255))
        vel.alpha_composite(img)
        vel.save(os.path.join(out, naam))
    shots = [("rust_voor", geo_file, licht, 25), ("rust_zij", geo_file, licht, 100), ("rust_achter", geo_file, licht, 200), ("rust_voor2", geo_file, licht, -30),
             ("donker", pose(geo_file, anims[f"animation.{NAAM}.donker"], 0.0), nacht, 20),
             ("brul", pose(geo_file, anims[f"animation.{NAAM}.brul"], 1.0), licht, 25),
             ("brul_voor", pose(geo_file, anims[f"animation.{NAAM}.brul"], 1.0), licht, 0),
             ("zweep", pose(geo_file, anims[f"animation.{NAAM}.zweep"], 0.85), licht, 40),
             ("zwaard", pose(geo_file, anims[f"animation.{NAAM}.zwaard"], 0.7), licht, 25),
             ("loop", pose(geo_file, anims[f"animation.{NAAM}.loop"], 0.0), licht, 70),
             ("stamp", pose(geo_file, anims[f"animation.{NAAM}.stamp"], 0.55), licht, 40),
             ("opkomst5", pose(geo_file, anims[f"animation.{NAAM}.opkomst"], 5.0), licht, 15),
             ("wankel", pose(geo_file, anims[f"animation.{NAAM}.wankel"], 0.3), licht, 30)]
    for naam, gf, t_arr, yaw in shots:
        if alleen and naam not in alleen:
            continue
        teken(f"{NAAM}_{naam}.png", gf, t_arr, yaw)
    for naam, yaw in (("lijf_voor", 20), ("lijf_zij", 90), ("lijf_laag", 12)):
        if not alleen or naam in alleen:
            teken(f"{NAAM}_{naam}.png", geo_file, licht, yaw, pitch=(12 if naam == "lijf_laag" else -6), zonder=("vleugel", "zweep", "vlam_zweep"))
    print("bones:", len(geo_file["minecraft:geometry"][0]["bones"]), "cubes:", sum(len(b["cubes"]) for b in geo_file["minecraft:geometry"][0]["bones"]))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".", alleen=sys.argv[2:])
