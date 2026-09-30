"""
3.0 (Guhverhalen), slice landdiertjes - the critters' looks: GeckoLib models, animations and textures of the pluisegeltje, the
guh-konijntje (four furs), the pluiseekhoorntje and Sjokkel.

The three little mammals are guh-INSPIRED (not guhs): the big glossy guh eyes (dark pupil, a blue-to-teal ring, two white
highlights), pink blush, a tiny snoet, round ears. Sjokkel is a real Sjokkel in Minecraft style: a round red shell full of
cheese holes (like gatenkaas), a yellow little head with bead eyes, yellow feet that poke out of the holes.
Textures are painted face by face on an atlas (boerderij_dieren.Atlas: every cube face its own patch, 4 texels per model pixel).

  build(h)       writes geo/entity/<dier>.geo.json, animations/entity/<dier>.animation.json, textures/entity/<dier>.png
                 (guh_konijntje_<kleur>.png for the bunny) for the four critters
  check(h)       every animated bone exists, every model has a head, the animations the Java code asks for exist
  preview(out)   (python tools/features/landdiertjes_modellen.py <out>) offline renders of the four (wiki_renders)
Animations (Landdiertje + the critters): idle, walk, zit, blij, eet; egeltje opgerold + rollen; konijntje binky;
eekhoorntje graaf; Sjokkel schelp + poets.
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

Atlas, plain, disc, guh_face, cube, bone, geo, round_ear = bd.Atlas, bd.plain, bd.disc, bd.guh_face, bd.cube, bd.bone, bd.geo, bd.round_ear

# --- colours ------------------------------------------------------------------------------------------------------------------
EGEL_STEKEL = (214, 160, 146)       # soft pinkish-brown fluff spikes
EGEL_TIP = (255, 238, 230)          # their light, fluffy tips
EGEL_DONKER = (138, 88, 82)
EGEL_BUIK = (252, 232, 214)
EGEL_SNOET = (246, 208, 192)
EGEL_NEUS = (120, 56, 72)
OOR_BINNEN = (236, 128, 168)
BLOS = (255, 150, 186)

KONIJN = {   # fur, light (belly, muzzle, tail), inner ear, paws
    "roze": ((250, 196, 214), (255, 238, 244), (238, 128, 170), (246, 176, 200)),
    "wit": ((250, 248, 246), (255, 255, 255), (246, 170, 196), (238, 232, 230)),
    "choco": ((170, 116, 86), (238, 206, 176), (232, 140, 160), (150, 100, 74)),
    "grijs": ((176, 176, 188), (236, 236, 242), (236, 150, 176), (156, 156, 170)),
}
KONIJN_KLEUREN = ["roze", "wit", "choco", "grijs"]    # = GuhKonijntjeEntity.Kleur order

EEKHOORN = (226, 136, 82)
EEKHOORN_DONKER = (192, 104, 58)
EEKHOORN_LICHT = (255, 232, 204)
EEKHOORN_STAART = (238, 156, 98)

SCHELP = (214, 46, 54)
SCHELP_DONKER = (164, 28, 38)
SCHELP_LICHT = (236, 88, 88)
GAT_RAND = (252, 226, 126)          # the cheese holes: a yellow rim...
GAT = (196, 132, 40)                # ...and a darker inside
GEEL = (250, 212, 70)
GEEL_DONKER = (218, 170, 38)
KRAAL = (18, 14, 20)


# =====================================================================================================================
# painters
# =====================================================================================================================
def stekels(base=EGEL_STEKEL, tip=EGEL_TIP, dark=EGEL_DONKER, rij=8.0):
    """Soft fluffy spikes: rows of little upward tufts (triangles, dark at the root, a light fluffy tip). Rows are drawn top
    to bottom, so each row's tips lie over the roots of the row above, like a real hedgehog's coat."""
    def paint(W, H, rng):
        a = plain(dark, 6)(W, H, rng).astype(np.float32)
        hoog, breed, stap_y, stap_x = rij * 1.5, rij * 0.8, rij * 0.62, rij * 0.62
        rows = int(H / stap_y) + 3
        for k in range(rows):
            root = -hoog * 0.3 + k * stap_y + hoog
            x = -breed + (k % 2) * stap_x / 2 + rng.uniform(-1, 1)
            while x < W + breed:
                h_ = hoog * rng.uniform(0.85, 1.1)
                w_ = breed * rng.uniform(0.85, 1.1)
                lean = rng.uniform(-0.25, 0.25)
                for yy in range(int(root - h_), int(root) + 1):
                    t = (root - yy) / h_
                    half = w_ / 2 * (1 - t)
                    cx = x + lean * (root - yy)
                    for xx in range(int(cx - half - 1), int(cx + half) + 2):
                        if 0 <= yy < H and 0 <= xx < W:
                            d = abs(xx + 0.5 - cx)
                            if d <= half:
                                c = np.array(base) * (1 - t) ** 0.7 + np.array(tip) * (1 - (1 - t) ** 0.7)
                                if d > half - 1.0:
                                    c = c * 0.8 + np.array(dark) * 0.2
                                a[yy, xx, :3] = c
                x += stap_x * rng.uniform(0.9, 1.15)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def fluffy(colour, light, var=10, n=35):
    """Soft fur with little lighter fluff curls (the squirrel's tail, the bunny's pompon)."""
    def extra(W, H, rng, a):
        for _ in range(max(2, W * H // n)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.5, 3.0), light, 0.35)
    return plain(colour, var, extra)


def kaasgaten(base=SCHELP, dichtheid=110, rmin=2.2, rmax=4.2):
    """Sjokkel's shell: red with cheese holes (a yellow rim around a darker hole) and a soft shine."""
    def paint(W, H, rng):
        a = plain(base, 8)(W, H, rng).astype(np.float32)
        # a soft darker bottom edge and a lighter top (a round shell)
        for y in range(H):
            f = y / max(1, H - 1)
            a[y, :, :3] = a[y, :, :3] * (1.08 - 0.22 * f)
        placed = []
        tries = 0
        target = max(1, int(W * H / dichtheid))
        while len(placed) < target and tries < target * 30:
            tries += 1
            r = rng.uniform(rmin, rmax)
            cx, cy = rng.uniform(r, max(r + 0.1, W - r)), rng.uniform(r, max(r + 0.1, H - r))
            if any(math.hypot(cx - px, cy - py) < r + pr + 1.5 for px, py, pr in placed):
                continue
            placed.append((cx, cy, r))
            disc(a, cx, cy, r, GAT_RAND, 1.0)
            disc(a, cx + 0.3, cy + 0.4, r * 0.62, GAT, 1.0)
            disc(a, cx + 0.6, cy + 0.8, r * 0.3, (150, 96, 26), 1.0)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def kraaloogjes(W, H, rng):
    """Sjokkel's face: yellow with two bead eyes (a glint each) and a tiny smile."""
    a = plain(GEEL, 8)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):
        cx, cy = W / 2 + sx * W * 0.22, H * 0.42
        disc(a, cx, cy, W * 0.09, KRAAL, 1.0, ry=H * 0.13)
        disc(a, cx + W * 0.03, cy - H * 0.05, W * 0.035, (255, 255, 255), 1.0)
        disc(a, cx + sx * W * 0.06, cy + H * 0.22, W * 0.07, (255, 170, 120), 0.45, ry=H * 0.05)
    my = int(H * 0.68)
    for x in range(int(W * 0.42), int(W * 0.58) + 1):
        dy = int(round(abs(x - W / 2) / (W * 0.08) * 1.2))
        if 0 <= my - dy < H:
            a[my - dy, x, :3] = GEEL_DONKER
    return np.clip(a, 0, 255).astype(np.uint8)


def neusje(colour, nose, W_frac=0.36):
    """A muzzle patch with a little rounded nose on top and a soft mouth."""
    def paint(W, H, rng):
        a = plain(colour, 6)(W, H, rng).astype(np.float32)
        disc(a, W / 2, H * 0.32, W * W_frac / 2, nose, 1.0, ry=H * 0.2)
        disc(a, W / 2 - W * 0.06, H * 0.26, W * 0.05, (255, 255, 255), 0.6)
        y = int(H * 0.72)
        a[y:y + 2, int(W * 0.3):int(W * 0.7), :3] = np.array(colour) * 0.7
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def rond_onder(colour, var=8):
    """A face patch with see-through bottom corners (a lop ear's rounded tip)."""
    def paint(W, H, rng):
        a = plain(colour, var)(W, H, rng)
        r = W / 2
        for y in range(H):
            for x in range(W):
                if y > H - r and math.hypot(x + 0.5 - W / 2, y + 0.5 - (H - r)) > r:
                    a[y, x, 3] = 0
        return a
    return paint


def buik_op(fur, light):
    """Fur with a light round belly patch."""
    def f(W, H, rng, a):
        disc(a, W / 2, H * 0.6, W * 0.34, light, 1.0, ry=H * 0.4)
    return plain(fur, 8, f)


# =====================================================================================================================
# models
# =====================================================================================================================
def egeltje(atlas):
    st = stekels()
    buik = plain(EGEL_BUIK, 6)
    poot = plain(EGEL_SNOET, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3, 0], []),
             bone("stekels", "body", [0, 3, 0], [
                 cube(atlas, [-4.5, 2.2, -3.5], [9, 6, 9.5], st, overrides={"down": plain(EGEL_DONKER, 6)}),
                 cube(atlas, [-3.5, 8.2, -2.6], [7, 1.3, 7.6], st),
                 cube(atlas, [-3.6, 3.0, 6.0], [7.2, 4.6, 1.2], st),
                 cube(atlas, [-5.2, 3.2, -2.5], [0.7, 4.2, 7.5], st), cube(atlas, [4.5, 3.2, -2.5], [0.7, 4.2, 7.5], st),
                 cube(atlas, [-2.6, 9.4, -1.4], [2.0, 0.8, 2.0], st), cube(atlas, [0.8, 9.4, 1.2], [2.0, 0.8, 2.2], st),
                 cube(atlas, [-1.2, 9.4, 3.4], [2.2, 0.7, 1.8], st), cube(atlas, [-3.2, 8.6, 5.0], [1.8, 1.0, 1.6], st),
                 cube(atlas, [1.6, 8.6, -2.2], [1.8, 0.9, 1.6], st)]),
             bone("buik", "body", [0, 3, 0], [cube(atlas, [-3.6, 1.2, -4.2], [7.2, 3.2, 8.5], buik)]),
             bone("head", "body", [0, 4.8, -3.6], [
                 cube(atlas, [-3.2, 1.8, -6.8], [6.4, 5.0, 3.4], buik, overrides={
                     "north": guh_face(EGEL_BUIK, eye_y=0.44, eye_dx=0.25, eye_r=0.18, snoet=False)}),
                 cube(atlas, [-3.6, 6.4, -6.0], [7.2, 1.6, 3.2], st),
                 cube(atlas, [-1.3, 2.2, -8.6], [2.6, 2.0, 1.9], plain(EGEL_SNOET, 5), overrides={"north": neusje(EGEL_SNOET, EGEL_NEUS, 0.5)})])]
    bones += bd.ears(atlas, EGEL_BUIK, OOR_BINNEN, 2.3, 6.1, -5.5, 1.9, 1.9)
    for name, x, z in (("leg_front_left", 1.5, -3.2), ("leg_front_right", -3.1, -3.2), ("leg_back_left", 1.5, 3.0), ("leg_back_right", -3.1, 3.0)):
        bones.append(bone(name, "body", [x + 0.8, 2, z + 0.8], [cube(atlas, [x, 0, z], [1.6, 2.0, 1.6], poot)]))
    return geo("pluisegeltje", bones, 0.9, 0.8)


def konijntje(atlas, kleur):
    fur, light, inner, paw = KONIJN[kleur]
    vacht = plain(fur, 8)
    licht = fluffy(light, (255, 255, 255), 5, 25)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [
                 cube(atlas, [-3.5, 1.4, -2.4], [7, 6, 7.6], vacht, overrides={"north": buik_op(fur, light)}),
                 cube(atlas, [-3.0, 7.4, -1.8], [6, 0.8, 6.6], vacht)]),
             bone("tail", "body", [0, 4.5, 5.2], [cube(atlas, [-1.6, 3.2, 5.2], [3.2, 3.2, 2.2], licht)]),
             bone("head", "body", [0, 8.2, -1.8], [
                 cube(atlas, [-3.6, 6.8, -6.2], [7.2, 6.0, 5.4], vacht, overrides={
                     "north": guh_face(fur, eye_y=0.40, eye_dx=0.25, eye_r=0.17, snoet=False)}),
                 cube(atlas, [-1.3, 7.4, -6.7], [2.6, 1.6, 0.5], plain(light, 5), overrides={"north": neusje(light, (236, 110, 150), 0.5)})]),
             ]
    # the lop ears: long, soft, hanging down beside the head, round at the tip
    for side, sx in (("left", 1), ("right", -1)):
        x0 = 3.5 if sx > 0 else -4.7
        tip = rond_onder(fur)
        bones.append(bone(f"ear_{side}", "head", [sx * 3.9, 12.4, -3.4], [
            cube(atlas, [x0, 5.4, -4.9], [1.2, 7.4, 3.0], vacht,
                 overrides={"north": tip, "south": tip, "east" if sx > 0 else "west": tip,
                            "west" if sx > 0 else "east": rond_onder(inner, 6)})],
            rotation=[0, 0, -12 * sx]))
    for name, x, z in (("leg_front_left", 0.6, -2.8), ("leg_front_right", -2.6, -2.8)):
        bones.append(bone(name, "body", [x + 1, 2.4, z + 1], [cube(atlas, [x, 0, z], [2, 2.4, 2], plain(paw, 6))]))
    for name, x in (("leg_back_left", 1.4), ("leg_back_right", -3.6)):
        bones.append(bone(name, "body", [x + 1.1, 2, 2.2], [cube(atlas, [x, 0, 0.6], [2.2, 1.8, 4.2], plain(paw, 6),
                                                                  overrides={"down": plain(light, 4)})]))
    return geo("guh_konijntje", bones, 0.9, 1.0)


def eekhoorntje(atlas):
    vacht = plain(EEKHOORN, 9)
    staart = fluffy(EEKHOORN_STAART, EEKHOORN_LICHT, 9, 30)
    licht = plain(EEKHOORN_LICHT, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3, 0], [
                 cube(atlas, [-2.6, 1.4, -2.2], [5.2, 5.2, 5.6], vacht, overrides={"north": buik_op(EEKHOORN, EEKHOORN_LICHT)})]),
             bone("head", "body", [0, 6.4, -1.4], [
                 cube(atlas, [-3.0, 5.6, -5.2], [6.0, 5.0, 4.6], vacht, overrides={
                     "north": guh_face(EEKHOORN, eye_y=0.42, eye_dx=0.25, eye_r=0.18, snoet=False, spots=wangetjes)}),
                 cube(atlas, [-1.2, 6.0, -5.8], [2.4, 1.6, 0.6], licht, overrides={"north": neusje(EEKHOORN_LICHT, (150, 70, 60), 0.5)})])]
    bones += bd.ears(atlas, EEKHOORN, OOR_BINNEN, 1.9, 9.8, -3.6, 2.0, 2.2)
    for side, sx in (("left", 1), ("right", -1)):                     # the little ear tufts
        bones.append(bone(f"pluimpje_{side}", f"ear_{side}", [sx * 2.9, 12.0, -3.1], [
            cube(atlas, [sx * 2.9 - 0.45, 11.9, -3.5], [0.9, 1.4, 0.8], plain(EEKHOORN_DONKER, 6))]))
    bones += [bone("tail_1", "body", [0, 3.2, 3.0], [cube(atlas, [-1.8, 2.2, 3.0], [3.6, 3.6, 3.2], staart)], rotation=[-20, 0, 0]),
              bone("tail_2", "tail_1", [0, 5.4, 5.6], [cube(atlas, [-2.8, 5.0, 4.2], [5.6, 6.0, 4.2], staart)], rotation=[-12, 0, 0]),
              bone("tail_3", "tail_2", [0, 10.6, 5.6], [cube(atlas, [-3.0, 10.4, 3.0], [6.0, 4.2, 5.2], staart,
                                                              overrides={"up": fluffy(EEKHOORN_LICHT, (255, 250, 240), 6)}),
                                                         cube(atlas, [-2.4, 11.2, 1.6], [4.8, 3.0, 1.6], fluffy(EEKHOORN_LICHT, (255, 250, 240), 6))],
                   rotation=[28, 0, 0])]
    for name, x, z in (("arm_left", 0.7, -3.0), ("arm_right", -2.0, -3.0)):
        bones.append(bone(name, "body", [x + 0.65, 4.6, z + 0.65], [cube(atlas, [x, 2.6, z], [1.3, 2.2, 1.3], licht)]))
    for name, x in (("leg_left", 1.0), ("leg_right", -2.9)):
        bones.append(bone(name, "body", [x + 0.95, 2, 1.8], [cube(atlas, [x, 0, 0.2], [1.9, 2.0, 3.0], vacht,
                                                                   overrides={"down": licht})]))
    return geo("pluiseekhoorntje", bones, 0.8, 1.0)


def wangetjes(W, H, rng, a):
    """The squirrel's fat little cheeks (lighter)."""
    for sx in (-1, 1):
        disc(a, W / 2 + sx * W * 0.34, H * 0.78, W * 0.16, EEKHOORN_LICHT, 0.9, ry=H * 0.16)


def shuckle(atlas):
    schelp = kaasgaten()
    schelp_klein = kaasgaten(dichtheid=90, rmin=1.8, rmax=3.0)
    geel = plain(GEEL, 7)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2, 0], []),
             bone("schelp", "body", [0, 5, 0], [
                 cube(atlas, [-5.0, 1.8, -4.6], [10, 6.4, 10], schelp, overrides={"down": plain(SCHELP_DONKER, 6)}),
                 cube(atlas, [-4.0, 8.2, -3.6], [8, 1.3, 8], schelp_klein),
                 cube(atlas, [-5.6, 2.8, -3.4], [0.6, 4.6, 7.6], schelp_klein), cube(atlas, [5.0, 2.8, -3.4], [0.6, 4.6, 7.6], schelp_klein),
                 cube(atlas, [-3.8, 2.8, 5.4], [7.6, 4.6, 0.6], schelp_klein),
                 cube(atlas, [-3.8, 3.4, -5.2], [7.6, 4.0, 0.6], schelp_klein)]),
             bone("head", "body", [0, 4.2, -5.0], [
                 cube(atlas, [-2.4, 2.0, -8.6], [4.8, 4.2, 3.8], geel, overrides={"north": kraaloogjes})])]
    # yellow feet poking out of the holes: three on each side
    for i, z in enumerate((-3.0, 0.0, 3.0)):
        for side, sx in (("left", 1), ("right", -1)):
            x = 4.4 if sx > 0 else -5.8
            bones.append(bone(f"voet_{side}_{i}", "body", [sx * 5.0, 2.4, z + 0.7], [
                cube(atlas, [x, 0, z], [1.4, 2.4, 1.4], geel, overrides={"down": plain(GEEL_DONKER, 5)})]))
    return geo("shuckle", bones, 1.1, 1.0)


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(pairs):
    return {str(round(t, 3)): v for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": length, "bones": bones}


def benen(names, swing, length):
    out = {}
    for i, n in enumerate(names):
        s = swing if i in (0, 3) else -swing
        out[n] = {"rotation": kf([(0, [s, 0, 0]), (length / 2, [-s, 0, 0]), (length, [s, 0, 0])])}
    return out


def oortjes(length, hoek=18):
    return {"ear_left": {"rotation": kf([(0, [0, 0, 0]), (length * 0.8, [0, 0, 0]), (length * 0.85, [0, 0, -hoek]), (length * 0.9, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (length * 0.4, [0, 0, 0]), (length * 0.45, [0, 0, hoek]), (length * 0.5, [0, 0, 0])])}}


def blij_hop(extra=None):
    b = {"body": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 2.5, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.5, 0]), (0.6, [0, 0, 0])])},
         "head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-10, 0, 8]), (0.6, [0, 0, 0])])}}
    b.update(extra or {})
    return b


def eet_knik():
    return {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [30, 0, 0]), (0.35, [22, 0, 0]), (0.5, [30, 0, 0]), (0.65, [22, 0, 0]),
                                     (0.8, [30, 0, 0]), (1.0, [0, 0, 0])])}}


def egeltje_anims():
    legs = ["leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"]
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.02, 1.03, 1.02]), (3, [1, 1, 1])])},
            "head": {"position": kf([(0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.35, [0, 0, -0.3]), (2.5, [0, 0, 0]), (2.65, [0, 0, -0.3]),
                                     (2.8, [0, 0, 0]), (3, [0, 0, 0])])}}
    idle.update(oortjes(3.0))
    walk = benen(legs, 32, 0.5)
    walk["body"] = {"rotation": kf([(0, [0, 0, 3]), (0.25, [0, 0, -3]), (0.5, [0, 0, 3])])}
    weg = [0.01, 0.01, 0.01]
    bal = {"head": {"position": [0, -1.5, 3.0], "scale": weg}, "ear_left": {"scale": weg}, "ear_right": {"scale": weg},
           "buik": {"scale": weg}, "stekels": {"position": [0, -1.4, 0], "scale": [1.04, 1.12, 1.0]}}
    for n in legs:
        bal[n] = {"scale": weg}
    opgerold = {k: dict(v) for k, v in bal.items()}
    opgerold["body"] = {"scale": kf([(0, [1, 1, 1]), (1.0, [1.02, 0.98, 1.02]), (2.0, [1, 1, 1])])}
    rollen = {k: dict(v) for k, v in bal.items()}
    rollen["body"] = {"rotation": kf([(0, [0, 0, 0]), (0.25, [-90, 0, 0]), (0.5, [-180, 0, 0]), (0.75, [-270, 0, 0]), (1.0, [-360, 0, 0])]),
                      "position": [0, 2.0, 0]}
    zit = {n: {"scale": [1, 0.4, 1]} for n in legs}
    zit["body"] = {"position": [0, -1.0, 0]}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(3.0, idle), "walk": anim(0.5, walk), "zit": anim(2.0, zit),
        "blij": anim(0.6, blij_hop(), False), "eet": anim(1.0, eet_knik(), False),
        "opgerold": anim(2.0, opgerold), "rollen": anim(1.0, rollen)}}


def konijntje_anims():
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.02, 1.025, 1.02]), (3, [1, 1, 1])])},
            "head": {"position": kf([(0, [0, 0, 0]), (1.0, [0, 0, 0]), (1.08, [0, 0.25, 0]), (1.16, [0, 0, 0]), (1.24, [0, 0.25, 0]),
                                     (1.32, [0, 0, 0]), (3, [0, 0, 0])])},
            "ear_left": {"rotation": kf([(0, [0, 0, 0]), (1.5, [6, 0, -4]), (3, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (1.5, [6, 0, 4]), (3, [0, 0, 0])])}}
    L = 0.45
    walk = {"body": {"position": kf([(0, [0, 0, 0]), (L * 0.35, [0, 2.2, 0]), (L * 0.7, [0, 0.4, 0]), (L, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 0]), (L * 0.25, [-12, 0, 0]), (L * 0.55, [10, 0, 0]), (L, [0, 0, 0])])},
            "leg_back_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [50, 0, 0]), (L * 0.7, [-10, 0, 0]), (L, [0, 0, 0])])},
            "leg_back_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [50, 0, 0]), (L * 0.7, [-10, 0, 0]), (L, [0, 0, 0])])},
            "leg_front_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [-40, 0, 0]), (L * 0.7, [20, 0, 0]), (L, [0, 0, 0])])},
            "leg_front_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [-40, 0, 0]), (L * 0.7, [20, 0, 0]), (L, [0, 0, 0])])},
            "ear_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [-18, 0, -10]), (L, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [-18, 0, 10]), (L, [0, 0, 0])])}}
    zit = {"body": {"position": [0, -0.6, 0]}, "leg_front_left": {"scale": [1, 0.6, 1]}, "leg_front_right": {"scale": [1, 0.6, 1]},
           "ear_left": {"rotation": [8, 0, -4]}, "ear_right": {"rotation": [8, 0, 4]}}
    binky = {"body": {"position": kf([(0, [0, 0, 0]), (0.25, [0, 6, 0]), (0.5, [0, 7, 0]), (0.8, [0, 0, 0]), (1.0, [0, 0, 0])]),
                      "rotation": kf([(0, [0, 0, 0]), (0.25, [0, 35, -15]), (0.5, [0, -35, 15]), (0.8, [0, 0, 0]), (1.0, [0, 0, 0])])},
             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-40, 0, -30]), (0.5, [-20, 0, 20]), (0.8, [0, 0, 0])])},
             "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-40, 0, 30]), (0.5, [-20, 0, -20]), (0.8, [0, 0, 0])])},
             "leg_back_left": {"rotation": kf([(0, [0, 0, 0]), (0.3, [60, 0, 0]), (0.8, [0, 0, 0])])},
             "leg_back_right": {"rotation": kf([(0, [0, 0, 0]), (0.3, [60, 0, 0]), (0.8, [0, 0, 0])])}}
    blij = blij_hop({"tail": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 25, 0]), (0.2, [0, -25, 0]), (0.3, [0, 25, 0]), (0.6, [0, 0, 0])])}})
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(3.0, idle), "walk": anim(L, walk), "zit": anim(2.0, zit),
        "blij": anim(0.6, blij, False), "eet": anim(1.0, eet_knik(), False), "binky": anim(1.0, binky, False)}}


def eekhoorntje_anims():
    zwaai = {"tail_1": {"rotation": kf([(0, [0, 0, 0]), (1.0, [0, 8, 0]), (2.0, [0, -8, 0]), (3.0, [0, 0, 0])])},
             "tail_2": {"rotation": kf([(0, [0, 0, 0]), (1.0, [4, 10, 0]), (2.0, [4, -10, 0]), (3.0, [0, 0, 0])])},
             "tail_3": {"rotation": kf([(0, [0, 0, 0]), (1.0, [8, 12, 0]), (2.0, [8, -12, 0]), (3.0, [0, 0, 0])])}}
    idle = dict(zwaai)
    idle["arm_left"] = {"rotation": kf([(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.2, [-30, 0, 0]), (2.4, [-10, 0, 0]), (2.6, [-30, 0, 0]),
                                        (2.8, [0, 0, 0]), (3.0, [0, 0, 0])])}
    idle["arm_right"] = {"rotation": kf([(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.2, [-30, 0, 0]), (2.4, [-10, 0, 0]), (2.6, [-30, 0, 0]),
                                         (2.8, [0, 0, 0]), (3.0, [0, 0, 0])])}
    idle.update(oortjes(3.0, 14))
    L = 0.4
    walk = {"body": {"position": kf([(0, [0, 0, 0]), (L * 0.4, [0, 1.8, 0]), (L, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 0]), (L * 0.3, [-10, 0, 0]), (L * 0.7, [8, 0, 0]), (L, [0, 0, 0])])},
            "tail_1": {"rotation": kf([(0, [-10, 0, 0]), (L * 0.5, [12, 0, 0]), (L, [-10, 0, 0])])},
            "tail_2": {"rotation": kf([(0, [-6, 0, 0]), (L * 0.5, [10, 0, 0]), (L, [-6, 0, 0])])},
            "tail_3": {"rotation": kf([(0, [0, 0, 0]), (L * 0.5, [16, 0, 0]), (L, [0, 0, 0])])},
            "leg_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [45, 0, 0]), (L, [0, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [45, 0, 0]), (L, [0, 0, 0])])},
            "arm_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [-45, 0, 0]), (L, [0, 0, 0])])},
            "arm_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [-45, 0, 0]), (L, [0, 0, 0])])}}
    zit = dict(zwaai)
    zit.update({"body": {"rotation": [-10, 0, 0]}, "arm_left": {"rotation": [-35, 0, 0]}, "arm_right": {"rotation": [-35, 0, 0]},
                "head": {"rotation": [8, 0, 0]}})
    graaf = {"body": {"rotation": kf([(0, [0, 0, 0]), (0.2, [22, 0, 0]), (1.6, [22, 0, 0]), (1.8, [0, 0, 0])])},
             "head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [20, 0, 0]), (1.6, [20, 0, 0]), (1.8, [0, 0, 0])])},
             "arm_left": {"rotation": kf([(t / 10, [-60 if (t % 2) else -10, 0, 0]) for t in range(0, 17)] + [(1.8, [0, 0, 0])])},
             "arm_right": {"rotation": kf([(t / 10, [-10 if (t % 2) else -60, 0, 0]) for t in range(0, 17)] + [(1.8, [0, 0, 0])])},
             "tail_3": {"rotation": kf([(0, [0, 0, 0]), (0.4, [0, 18, 0]), (0.8, [0, -18, 0]), (1.2, [0, 18, 0]), (1.8, [0, 0, 0])])}}
    blij = blij_hop({"tail_2": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 20, 0]), (0.3, [0, -20, 0]), (0.45, [0, 20, 0]), (0.6, [0, 0, 0])])}})
    eet = eet_knik()
    eet["arm_left"] = {"rotation": kf([(0, [0, 0, 0]), (0.2, [-50, 0, 0]), (0.8, [-50, 0, 0]), (1.0, [0, 0, 0])])}
    eet["arm_right"] = {"rotation": kf([(0, [0, 0, 0]), (0.2, [-50, 0, 0]), (0.8, [-50, 0, 0]), (1.0, [0, 0, 0])])}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(3.0, idle), "walk": anim(L, walk), "zit": anim(3.0, zit),
        "blij": anim(0.6, blij, False), "eet": anim(1.0, eet, False), "graaf": anim(1.8, graaf, False)}}


VOETEN = [f"voet_{s}_{i}" for i in range(3) for s in ("left", "right")]


def shuckle_anims():
    idle = {"head": {"position": kf([(0, [0, 0, 0]), (2.0, [0, 0, -0.5]), (4.0, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 0]), (1.0, [0, 6, 0]), (3.0, [0, -6, 0]), (4.0, [0, 0, 0])])},
            "schelp": {"scale": kf([(0, [1, 1, 1]), (2.0, [1.01, 1.02, 1.01]), (4.0, [1, 1, 1])])}}
    L = 1.6                                                            # (slow...)
    walk = {}
    for k, n in enumerate(VOETEN):
        fase = (k % 2) * L / 2
        walk[n] = {"rotation": kf([(0, [18 if fase == 0 else -18, 0, 0]), (L / 2, [-18 if fase == 0 else 18, 0, 0]),
                                   (L, [18 if fase == 0 else -18, 0, 0])])}
    walk["body"] = {"rotation": kf([(0, [0, 0, 2]), (L / 2, [0, 0, -2]), (L, [0, 0, 2])])}
    walk["head"] = {"position": kf([(0, [0, 0, -0.3]), (L / 2, [0, 0, 0]), (L, [0, 0, -0.3])])}
    weg = [0.01, 0.01, 0.01]
    schelp = {"head": {"position": [0, -0.6, 4.2], "scale": weg}, "body": {"position": [0, -1.9, 0]}}
    for n in VOETEN:
        schelp[n] = {"scale": weg, "position": [0, 1.5, 0]}
    zit = {"body": {"position": [0, -1.0, 0]}}
    for n in VOETEN:
        zit[n] = {"scale": [1, 0.5, 1]}
    poets = {"schelp": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 6]), (0.3, [0, 0, -6]), (0.45, [0, 0, 6]), (0.6, [0, 0, -6]),
                                         (0.75, [0, 0, 0])])}}
    for k, n in enumerate(VOETEN):
        poets[n] = {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 25 if k % 2 else -25, 0]), (0.4, [0, -25 if k % 2 else 25, 0]),
                                    (0.6, [0, 25 if k % 2 else -25, 0]), (0.75, [0, 0, 0])])}
    blij = {"schelp": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 0, 8]), (0.2, [0, 0, -8]), (0.3, [0, 0, 8]), (0.4, [0, 0, -8]), (0.6, [0, 0, 0])])},
            "head": {"position": kf([(0, [0, 0, 0]), (0.2, [0, 0.8, -0.8]), (0.6, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(4.0, idle), "walk": anim(L, walk), "zit": anim(2.0, zit), "schelp": anim(2.0, schelp),
        "blij": anim(0.6, blij, False), "eet": anim(1.0, eet_knik(), False), "poets": anim(0.75, poets, False)}}


# =====================================================================================================================
DIEREN = [("pluisegeltje", egeltje, egeltje_anims, 2030151),
          ("pluiseekhoorntje", eekhoorntje, eekhoorntje_anims, 2030153),
          ("shuckle", shuckle, shuckle_anims, 2030154)]
NODIG = {"pluisegeltje": ["idle", "walk", "zit", "blij", "eet", "opgerold", "rollen"],
         "guh_konijntje": ["idle", "walk", "zit", "blij", "eet", "binky"],
         "pluiseekhoorntje": ["idle", "walk", "zit", "blij", "eet", "graaf"],
         "shuckle": ["idle", "walk", "zit", "blij", "eet", "schelp", "poets"]}


def build(h):
    A = h.A
    for name, maker, anims, seed in DIEREN:
        atlas = Atlas(seed)
        h.w(f"{A}/geckolib/models/entity/{name}.geo.json", maker(atlas))
        h.w(f"{A}/geckolib/animations/entity/{name}.animation.json", anims())
        h.save(Image.fromarray(atlas.img), "entity", f"{name}.png")
    for i, kleur in enumerate(KONIJN_KLEUREN):
        atlas = Atlas(2030152)
        model = konijntje(atlas, kleur)
        if i == 0:
            h.w(f"{A}/geckolib/models/entity/guh_konijntje.geo.json", model)
            h.save(Image.fromarray(atlas.img), "entity", "guh_konijntje.png")      # (the default texture: roze)
        h.save(Image.fromarray(atlas.img), "entity", f"guh_konijntje_{kleur}.png")
    h.w(f"{A}/geckolib/animations/entity/guh_konijntje.animation.json", konijntje_anims())


def check(h):
    import json
    problems = []
    for name, wanted in NODIG.items():
        g = json.load(open(f"{h.A}/geckolib/models/entity/{name}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
            if b["name"].startswith(("pluis", "neck", "outfit_")) and name != "pluiseekhoorntje":
                problems.append(f"{name}: bone name {b['name']} clashes with the guh variant bone prefixes")
        if "head" not in names:
            problems.append(f"{name}: no head bone")
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
    """Offline renders (front-left, and from behind) of the four critters + the four bunny furs, into out/."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo_dir = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    tiles = []
    for name, tex in [("pluisegeltje", "pluisegeltje"), ("guh_konijntje", "guh_konijntje_roze"), ("guh_konijntje", "guh_konijntje_wit"),
                      ("guh_konijntje", "guh_konijntje_choco"), ("guh_konijntje", "guh_konijntje_grijs"),
                      ("pluiseekhoorntje", "pluiseekhoorntje"), ("shuckle", "shuckle")]:
        q = wr.geo_quads(os.path.join(geo_dir, f"{name}.geo.json"), f"guhs:entity/{tex}")
        for yaw, pitch in ((200, -18), (20, -18)):
            img = wr.render(q, yaw, pitch, 320, margin=0.06)
            tiles.append(img)
            img.save(os.path.join(out, f"{tex}_{yaw}.png"))
    sheet = Image.new("RGBA", (320 * 4, 320 * ((len(tiles) + 3) // 4)), (236, 232, 240, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 4) * 320, (i // 4) * 320))
    sheet.save(os.path.join(out, "landdiertjes_sheet.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
