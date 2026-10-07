"""
einde2, slice mikas - the looks of its three creatures: GeckoLib models, animations and textures.

  - ender_mika   the tall Mika (build contract 3.3: the ordinary Mika stood upright and stretched; 46 model pixels high,
                 legs 2 x 22 x 2, body 6 x 12 x 3, arms 2 x 20 x 2 hanging to the knees, the Mika's own head with its round
                 ears and its evil face at about 10 x 12, its thin devil tail). Dark purple-black, the eyes and a few
                 ender specks glow (ender_mika_glowmask.png). The head keeps its own UVs on a recoloured copy of mika.png
                 (spiesburcht_modellen.Sheet); the limbs and the body are painted face by face in the free part of that sheet.
                 The bone hand_right is where the stolen knabbel is drawn.
  - kaasmijt     a tiny mite made of cheese crumbs: three wobbling gatenkaas segments, bead eyes, six little legs and two
                 feelers with glowing ender tips (kaasmijt_glowmask.png)
  - schuilguh    a guh in a round wooden kaasdoos: the box (doos), its lid on a hinge at the back (deksel) and the guh's head
                 with the big guh eyes and round ears that peeks out. Built sitting on a floor (lid up); the renderer turns
                 the whole thing to the face it is stuck to.

  build(h)       writes geo/entity/<id>.geo.json, animations/entity/<id>.animation.json, textures/entity/<id>.png (+ glow masks)
  check(h)       every animated bone exists, the bones of the contract are there, the animations the Java code asks for exist
  preview(out)   (python tools/features/einde2_mikas_modellen.py <out>) offline renders of the three (wiki_renders)
Animations: ender_mika idle, walk, draag, betrapt, jat, flits, duw; kaasmijt idle, walk, kietel; schuilguh dicht, gluur,
open, kusje, eet, schrik.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import boerderij_dieren as bd
    from features import einde2_kit as kit
    from features import spiesburcht_modellen as sm
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import boerderij_dieren as bd
    from features import einde2_kit as kit
    from features import spiesburcht_modellen as sm

PX = sm.PX
FACES = bd.FACES

# --- colours ------------------------------------------------------------------------------------------------------------------
ENDER_DONKER = (16, 9, 26)          # the Ender-Mika's fur: purple-black...
ENDER_LICHT = (62, 40, 88)          # ...up to a dusky purple where the light catches it
ENDER_POOT = (132, 82, 150)         # hands and feet: a last bit of Mika pink under the purple
ENDER_BUIK = (86, 56, 116)
ENDER_OOG = (238, 128, 255)         # the glowing eyes
ENDER_VONK = (206, 120, 248)        # the ender specks in its fur
ENDER_WENKBRAUW = (150, 108, 190)   # brows and smirk: lighter than the fur, or the evil face would be gone

KAAS = (250, 214, 96)
KAAS_LICHT = (255, 236, 150)
KAAS_DONKER = (226, 172, 52)
KAAS_GAT = (190, 128, 36)
MIJT_POOT = (150, 96, 60)
MIJT_VONK = (214, 132, 255)

HOUT = (226, 190, 130)              # the kaasdoos: thin pale poplar wood
HOUT_DONKER = (186, 146, 90)
HOUT_BINNEN = (104, 74, 48)
RUIT_ROOD = (206, 52, 60)
RUIT_WIT = (250, 244, 232)
GUH = (214, 178, 222)               # the guh's own lilac-pink fur
GUH_OOR = (236, 128, 168)


# =====================================================================================================================
# the Ender-Mika (on the Mika's sheet)
# =====================================================================================================================
def vlak(sheet, w, h, painter, glow=None):
    """A painted patch of w x h model pixels in a free part of the sheet (painter(W, H) -> float RGB array of H x W px;
    glow(px) -> mask of the pixels that go into the glow mask). Returns its face uv."""
    w, h = int(math.ceil(w)), int(math.ceil(h))
    for v in range(sm.UNITS - h, -1, -1):
        for u in range(0, sm.UNITS - w + 1):
            if not sheet.used[v:v + h, u:u + w].any():
                sheet.used[max(0, v - 1):v + h + 1, max(0, u - 1):u + w + 1] = True      # (a pixel of air around every patch)
                px = np.clip(painter(w * PX, h * PX), 0, 255).astype(np.float32)
                sheet.img[v * PX:(v + h) * PX, u * PX:(u + w) * PX, :3] = px
                sheet.img[v * PX:(v + h) * PX, u * PX:(u + w) * PX, 3] = 255
                if glow is not None:
                    m = glow(px)
                    region = sheet.glow[v * PX:(v + h) * PX, u * PX:(u + w) * PX]
                    region[m, :3] = px[m]
                    region[m, 3] = 255
                return {"uv": [u, v], "uv_size": [w, h]}
    raise SystemExit("einde2_mikas models: no free texture space on the Ender-Mika's sheet")


def is_vonk(px):
    return np.abs(px - np.array(ENDER_VONK, np.float32)).sum(-1) < 4


def ender_vel(rng, poot=0.0, buik=False):
    """The Ender-Mika's skin: purple-black with a soft sheen down the middle, a few glowing ender specks, and (poot > 0) the
    last `poot` part of the patch fading into a pinkish-purple paw with two little toe lines; buik: a lighter belly oval."""
    def paint(W, H):
        a = np.zeros((H, W, 3), np.float32)
        for y in range(H):
            for x in range(W):
                sheen = 1.0 - abs((x + 0.5) / W - 0.5) * 1.1
                c = np.array(ENDER_DONKER, np.float32) * (1 - 0.55 * sheen) + np.array(ENDER_LICHT, np.float32) * 0.55 * sheen
                a[y, x] = c
        a += rng.normal(0, 2.2, (H, W, 1))
        if buik:
            for y in range(H):
                for x in range(W):
                    e = ((x + 0.5 - W / 2) / (W * 0.34)) ** 2 + ((y + 0.5 - H * 0.56) / (H * 0.36)) ** 2
                    if e < 1:
                        t = 0.75 * (1 - e) ** 0.6
                        a[y, x] = a[y, x] * (1 - t) + np.array(ENDER_BUIK, np.float32) * t
        if poot > 0:
            start = int(H * (1 - poot))
            for y in range(start, H):
                t = min(1.0, (y - start) / max(1.0, (H - start) * 0.45))
                a[y] = a[y] * (1 - t) + np.array(ENDER_POOT, np.float32) * t
            for x in (W // 3, (2 * W) // 3):                       # two toes
                a[H - max(3, H // 14):H, x] = np.array(ENDER_DONKER, np.float32) * 1.6
        for _ in range(max(1, W * H // 260)):                      # ender specks (they glow)
            x, y = int(rng.integers(0, W)), int(rng.integers(0, max(1, int(H * (1 - poot)) - 1)))
            a[y, x] = ENDER_VONK
        return a
    return paint


def effen(colour, rng, var=2.5):
    def paint(W, H):
        return np.array(colour, np.float32)[None, None, :] + rng.normal(0, var, (H, W, 1))
    return paint


def kubus(origin, size, uv):
    return {"origin": [round(o, 3) for o in origin], "size": [round(s, 3) for s in size], "uv": uv}


def ender_mika(h):
    mika = sm.load_geo(h, "mika")
    s = 0.64                                                       # the Mika's head, 16 wide -> about 10
    sh = (0, 34, 7 * s)                                            # ...on top of the body, centred over it
    head = sm.moved(mika["head"], "head", "body", s, sh)
    ears = [sm.moved(mika[e], e, "head", s, sh) for e in ("ear_left", "ear_right")]
    tail = sm.moved(mika["tail"], "tail", "body", 0.7, (0, 23.2, -6.2))
    tail["rotation"] = [-35, 0, 0]                                 # (it hangs down behind the long legs)
    basis = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    orig = np.asarray(basis).astype(np.float32)
    sheet, a, fur = sm.mika_sheet(h, ENDER_DONKER, ENDER_LICHT, [head, tail] + ears)
    rng = kit.rng("ender_mika")
    # the evil face: glowing eyes; the brows and the smirk (dark on pink) become lighter than the dark fur
    eyes = sm.eyes_mask(orig)
    lum = orig[..., :3] @ np.array((0.3, 0.59, 0.11), np.float32)
    # (the whole region of the face cube: everything in it that is neither fur nor eye is a brow, the mouth or the fang)
    gezicht = np.zeros(fur.shape, bool)
    gezicht[49 * PX:61 * PX, 11 * PX:25 * PX] = True
    donker = gezicht & ~fur & ~eyes & (orig[..., 3] > 0) & (lum < 120)
    sheet.img[donker, :3] = ENDER_WENKBRAUW
    pupil = np.zeros(fur.shape, bool)
    for dx in (-1, 1):                                             # the slit pupils sit between two halves of an eye
        pupil |= np.roll(eyes, dx, 1)
    pupil &= donker & np.roll(eyes, 2, 1) & np.roll(eyes, -2, 1)
    sheet.img[pupil, :3] = (40, 14, 60)
    sheet.img[eyes, :3] = ENDER_OOG
    sheet.glow[eyes] = ENDER_OOG + (255,)
    for c in tail["cubes"]:                                         # the devil tail: its dark red tip becomes ender purple
        for f in c["uv"].values():
            (u, v), (w_, h_) = f["uv"], f["uv_size"]
            deel = np.zeros(fur.shape, bool)
            deel[int(v * PX):int(math.ceil((v + h_) * PX)), int(u * PX):int(math.ceil((u + w_) * PX))] = True
            deel &= ~fur & (orig[..., 3] > 0)
            sheet.img[deel, :3] = ENDER_POOT
    speck = fur & (rng.random(fur.shape) < 0.006)
    sheet.img[speck, :3] = ENDER_VONK
    sheet.glow[speck] = ENDER_VONK + (255,)

    vel, kap = ender_vel(rng), effen(ENDER_DONKER, rng)
    lijf_v = vlak(sheet, 6, 12, ender_vel(rng, buik=True), is_vonk)
    lijf_a = vlak(sheet, 6, 12, vel, is_vonk)
    lijf_z = vlak(sheet, 3, 12, vel, is_vonk)
    lijf_k = vlak(sheet, 6, 3, kap)
    been = vlak(sheet, 2, 22, ender_vel(rng, poot=0.2), is_vonk)
    arm = vlak(sheet, 2, 20, ender_vel(rng, poot=0.16), is_vonk)
    top = vlak(sheet, 2, 2, kap)
    zool = vlak(sheet, 2, 2, effen(ENDER_POOT, rng))

    def lid(uv):
        return {"north": uv, "south": uv, "east": uv, "west": uv, "up": top, "down": zool}
    body = sm.bone("body", "root", [0, 22, 0], [kubus([-3, 22, -1.5], [6, 12, 3], {
        "north": lijf_v, "south": lijf_a, "east": lijf_z, "west": lijf_z, "up": lijf_k, "down": lijf_k})])
    arm_l = sm.bone("arm_left", "body", [4, 33.5, 0], [kubus([3, 14, -1], [2, 20, 2], lid(arm))])
    arm_r = sm.bone("arm_right", "body", [-4, 33.5, 0], [kubus([-5, 14, -1], [2, 20, 2], lid(arm))])
    hand = sm.bone("hand_right", "arm_right", [-4, 14.5, -1])       # (no cubes: the knabbel it stole is drawn here)
    leg_l = sm.bone("leg_left", "root", [1.5, 22, 0], [kubus([0.5, 0, -1], [2, 22, 2], lid(been))])
    leg_r = sm.bone("leg_right", "root", [-1.5, 22, 0], [kubus([-2.5, 0, -1], [2, 22, 2], lid(been))])
    bones = [sm.bone("root", None, [0, 0, 0]), body, head] + ears + [tail, arm_l, arm_r, hand, leg_l, leg_r]
    sm.write_geo(h, "ender_mika", bones, 2, 3.4)
    sheet.save(h, "ender_mika", glowmask=True)

    loop, once, wave = sm.loop, sm.once, sm.wave
    oren = {"ear_left": {"rotation": {"0.0": [0, 0, 0], "2.4": [0, 0, 0], "2.55": [0, 0, -14], "2.7": [0, 0, 0], "3.2": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "1.1": [0, 0, 0], "1.25": [0, 0, 14], "1.4": [0, 0, 0], "3.2": [0, 0, 0]}}}
    idle = {"body": {"rotation": wave(3.2, 1.2, axis=0)}, "arm_left": {"rotation": wave(3.2, 2.5, axis=2, offset=(0, 0, -2))},
            "arm_right": {"rotation": wave(3.2, -2.5, axis=2, offset=(0, 0, 2))}, "head": {"rotation": wave(3.2, 2.5, axis=2, phase=0.25)},
            "tail": {"rotation": wave(3.2, 16, axis=1)}}
    idle.update(oren)
    # holding its loot up in front of its chest with both long arms bent in, peeking at it
    draag = {"arm_right": {"rotation": wave(1.6, 3, axis=0, offset=(-58, -14, 0))},
             "arm_left": {"rotation": wave(1.6, -3, axis=0, offset=(-40, 20, 0))},
             "head": {"rotation": wave(1.6, 3, axis=0, offset=(14, 0, 0))}, "body": {"rotation": wave(1.6, 1, axis=0, offset=(4, 0, 0))},
             "tail": {"rotation": wave(0.8, 28, axis=1, steps=4)}}
    # caught: it freezes, hunched, and trembles all over
    tril = {f"{0.05 * i:.2f}": [0.35 if i % 2 else -0.35, 0, 0] for i in range(9)}
    betrapt = {"root": {"position": tril}, "body": {"rotation": {"0.0": [10, 0, 0], "0.4": [10, 0, 0]}},
               "head": {"rotation": {"0.0": [-16, 0, 0], "0.2": [-16, 0, 4], "0.4": [-16, 0, 0]}},
               "arm_right": {"rotation": {"0.0": [-58, -14, 0], "0.4": [-58, -14, 0]}},
               "arm_left": {"rotation": {"0.0": [-12, 0, -30], "0.2": [-12, 0, -36], "0.4": [-12, 0, -30]}},
               "ear_left": {"rotation": {"0.0": [0, 0, 24], "0.4": [0, 0, 24]}}, "ear_right": {"rotation": {"0.0": [0, 0, -24], "0.4": [0, 0, -24]}},
               "tail": {"rotation": {"0.0": [-30, 0, 0], "0.4": [-30, 0, 0]}}}
    h.w(f"{h.A}/geckolib/animations/entity/ender_mika.animation.json", {"format_version": "1.8.0", "animations": {
        "idle": loop(3.2, idle),
        "walk": loop(1.2, {"leg_left": {"rotation": wave(1.2, 24, axis=0)}, "leg_right": {"rotation": wave(1.2, -24, axis=0)},
                           "arm_left": {"rotation": wave(1.2, -16, axis=0)}, "arm_right": {"rotation": wave(1.2, 16, axis=0)},
                           "body": {"rotation": wave(0.6, 1.2, axis=0, steps=4, offset=(3, 0, 0))},
                           "head": {"rotation": wave(1.2, 3, axis=2)}, "tail": {"rotation": wave(1.2, 22, axis=1)}}),
        "draag": loop(1.6, draag),
        "betrapt": loop(0.4, betrapt),
        "jat": once(0.5, {"arm_right": {"rotation": {"0.0": [0, 0, 0], "0.12": [-96, 0, 0], "0.25": [-104, 10, 0], "0.5": [-58, -14, 0]}},
                          "body": {"rotation": {"0.0": [0, 0, 0], "0.15": [14, 0, 0], "0.5": [4, 0, 0]}},
                          "head": {"rotation": {"0.0": [0, 0, 0], "0.15": [10, 0, 0], "0.5": [14, 0, 0]}}}),
        "flits": once(0.35, {"root": {"scale": {"0.0": [0.2, 1.5, 0.2], "0.15": [1.15, 0.9, 1.15], "0.35": [1, 1, 1]}},
                             "arm_left": {"rotation": {"0.0": [0, 0, -40], "0.35": [0, 0, 0]}},
                             "arm_right": {"rotation": {"0.0": [0, 0, 40], "0.35": [0, 0, 0]}}}),
        # ("pounce" in MikaEntity: the shove, with both long arms)
        "duw": once(0.5, {"arm_left": {"rotation": {"0.0": [0, 0, 0], "0.15": [-100, 0, 0], "0.3": [-70, 0, 0], "0.5": [0, 0, 0]}},
                          "arm_right": {"rotation": {"0.0": [0, 0, 0], "0.15": [-100, 0, 0], "0.3": [-70, 0, 0], "0.5": [0, 0, 0]}},
                          "body": {"rotation": {"0.0": [0, 0, 0], "0.15": [12, 0, 0], "0.5": [0, 0, 0]}}}),
    }})
    return bones


# =====================================================================================================================
# the Kaasmijt and the Schuilguh (their own atlases)
# =====================================================================================================================
class Vel(bd.Atlas):
    """boerderij_dieren's atlas that also remembers which patches glow."""

    def __init__(self, naam):
        super().__init__(kit.seed(naam))
        self.rng = kit.rng(naam)
        self.gloed = []

    def glow_patch(self, w, h, painter):
        uv = self.patch(w, h, painter)
        self.gloed.append((uv["uv"], (int(math.ceil(w)), int(math.ceil(h)))))
        return uv

    def glowmask(self):
        out = np.zeros_like(self.img)
        for (u, v), (w, h) in self.gloed:
            out[v * bd.S:(v + h) * bd.S, u * bd.S:(u + w) * bd.S] = self.img[v * bd.S:(v + h) * bd.S, u * bd.S:(u + w) * bd.S]
        return out


def kaaskorrel(dichtheid=70):
    """A crumb of gatenkaas: warm yellow, lighter on top, with little round holes."""
    def gaten(W, H, rng, a):
        for y in range(H):
            a[y, :, :3] *= 1.06 - 0.16 * y / max(1, H - 1)
        placed = []
        for _ in range(max(1, W * H // dichtheid) * 12):
            if len(placed) >= max(1, W * H // dichtheid):
                break
            r = rng.uniform(1.0, 2.1)
            cx, cy = rng.uniform(r, max(r + 0.1, W - r)), rng.uniform(r, max(r + 0.1, H - r))
            if any(math.hypot(cx - px, cy - py) < r + pr + 1.2 for px, py, pr in placed):
                continue
            placed.append((cx, cy, r))
            bd.disc(a, cx, cy, r, KAAS_DONKER, 1.0)
            bd.disc(a, cx + 0.3, cy + 0.3, r * 0.6, KAAS_GAT, 1.0)
    return bd.plain(KAAS, 8, gaten)


def mijt_gezicht(W, H, rng):
    """The mite's little face: two bead eyes with a glint, a blush and a tiny smile."""
    a = bd.plain(KAAS_LICHT, 6)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):
        cx, cy = W / 2 + sx * W * 0.24, H * 0.42
        bd.disc(a, cx, cy, W * 0.13, (30, 14, 44), 1.0, ry=H * 0.2)
        bd.disc(a, cx + W * 0.04, cy - H * 0.07, W * 0.045, (255, 255, 255), 1.0)
        bd.disc(a, cx + sx * W * 0.07, cy + H * 0.3, W * 0.09, (255, 150, 150), 0.5, ry=H * 0.07)
    my = int(H * 0.74)
    for x in range(int(W * 0.4), int(W * 0.6) + 1):
        dy = int(round(abs(x - W / 2) / (W * 0.1) * 1.0))
        if 0 <= my - dy < H:
            a[my - dy, x, :3] = KAAS_GAT
    return np.clip(a, 0, 255).astype(np.uint8)


def kaasmijt(atlas):
    kaas = kaaskorrel()
    licht = bd.plain(KAAS_LICHT, 6)
    poot = bd.plain(MIJT_POOT, 6)

    def gloei(W, H, rng):
        a = bd.plain(MIJT_VONK, 14)(W, H, rng)
        return a
    vonk = {f: atlas.glow_patch(1, 1, gloei) for f in FACES}
    bones = [bd.bone("root", None, [0, 0, 0], []),
             bd.bone("body", "root", [0, 1.5, 0], []),
             bd.bone("head", "body", [0, 2, -2], [
                 bd.cube(atlas, [-1.75, 0.7, -4.4], [3.5, 3.0, 2.6], kaas, overrides={"north": mijt_gezicht, "down": licht})]),
             bd.bone("seg_mid", "body", [0, 2, 0], [
                 bd.cube(atlas, [-2.25, 0.6, -1.8], [4.5, 3.6, 3.4], kaas, overrides={"down": licht})]),
             bd.bone("seg_rear", "body", [0, 2, 1.6], [
                 bd.cube(atlas, [-1.75, 0.6, 1.6], [3.5, 3.0, 2.4], kaas, overrides={"down": licht}),
                 bd.cube(atlas, [-1.0, 0.8, 4.0], [2.0, 2.0, 1.2], kaas, overrides={"down": licht})])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 0.7 if sx > 0 else -1.1
        bones.append(bd.bone(f"voel_{side}", "head", [x + 0.2, 3.7, -3.6], [
            bd.cube(atlas, [x, 3.7, -3.8], [0.4, 1.9, 0.4], poot),
            {"origin": [x - 0.1, 5.5, -3.9], "size": [0.6, 0.6, 0.6], "uv": vonk}], rotation=[18, 0, -14 * sx]))
        for i, z in enumerate((-3.4, -0.6, 2.2)):
            px = 1.9 if sx > 0 else -2.6
            bones.append(bd.bone(f"leg_{side}_{i}", "body", [px + 0.35, 0.9, z + 0.35], [bd.cube(atlas, [px, 0, z], [0.7, 0.9, 0.7], poot)]))
    return bd.geo("kaasmijt", bones, 0.8, 0.6)


LM = [f"leg_{s}_{i}" for i in range(3) for s in ("left", "right")]


def kaasmijt_anims():
    kf = lambda pairs: {str(round(t, 3)): v for t, v in pairs}  # noqa: E731

    def anim(length, bones, loop=True):
        return {"loop": loop, "animation_length": length, "bones": bones}
    voel = {"voel_left": {"rotation": kf([(0, [0, 0, 0]), (0.5, [10, 0, -8]), (1.0, [0, 0, 0]), (1.5, [-8, 0, 6]), (2.0, [0, 0, 0])])},
            "voel_right": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-8, 0, -6]), (1.0, [0, 0, 0]), (1.5, [10, 0, 8]), (2.0, [0, 0, 0])])}}
    idle = {"seg_mid": {"scale": kf([(0, [1, 1, 1]), (1.0, [1.04, 1.06, 1.04]), (2.0, [1, 1, 1])])},
            "seg_rear": {"rotation": kf([(0, [0, 0, 0]), (0.5, [0, 8, 0]), (1.5, [0, -8, 0]), (2.0, [0, 0, 0])])}}
    idle.update(voel)
    L = 0.3
    walk = {n: {"rotation": kf([(0, [(34 if i % 2 else -34), 0, 0]), (L / 2, [(-34 if i % 2 else 34), 0, 0]), (L, [(34 if i % 2 else -34), 0, 0])])}
            for i, n in enumerate(LM)}
    walk["head"] = {"rotation": kf([(0, [0, 10, 0]), (L / 2, [0, -10, 0]), (L, [0, 10, 0])])}
    walk["seg_rear"] = {"rotation": kf([(0, [0, -14, 0]), (L / 2, [0, 14, 0]), (L, [0, -14, 0])])}
    walk["seg_mid"] = {"position": kf([(0, [0, 0, 0]), (L / 4, [0, 0.25, 0]), (L / 2, [0, 0, 0]), (3 * L / 4, [0, 0.25, 0]), (L, [0, 0, 0])])}
    # the tickle: it rears up and all six legs go at once
    kietel = {n: {"rotation": kf([(0.08 * k, [(50 if (k + i) % 2 else -50), 0, 0]) for k in range(11)])} for i, n in enumerate(LM)}
    kietel["body"] = {"rotation": kf([(0, [0, 0, 0]), (0.15, [-24, 0, 0]), (0.65, [-24, 0, 0]), (0.8, [0, 0, 0])]),
                      "position": kf([(0, [0, 0, 0]), (0.15, [0, 0.8, 0]), (0.65, [0, 0.8, 0]), (0.8, [0, 0, 0])])}
    kietel["voel_left"] = {"rotation": kf([(0.1 * k, [0, 0, (-24 if k % 2 else 12)]) for k in range(9)])}
    kietel["voel_right"] = {"rotation": kf([(0.1 * k, [0, 0, (24 if k % 2 else -12)]) for k in range(9)])}
    return {"format_version": "1.8.0", "animations": {"idle": anim(2.0, idle), "walk": anim(L, walk), "kietel": anim(0.8, kietel, False)}}


# --- the Schuilguh ------------------------------------------------------------------------------------------------------------
def hout(nerf=5, donker=1.0):
    """Thin pale box wood: a soft grain along the patch."""
    def paint(W, H, rng):
        a = bd.plain(tuple(c * donker for c in HOUT), 6)(W, H, rng).astype(np.float32)
        for _ in range(max(1, H // nerf)):
            y = int(rng.integers(0, H))
            a[y, :, :3] = a[y, :, :3] * 0.9
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def doos_zij(lid=False):
    """The side of the round box: wood with a darker rim, and (the lid) a red-and-white checked band all around."""
    def paint(W, H, rng):
        a = hout()(W, H, rng).astype(np.float32)
        if lid:
            cel = 4
            for y in range(2, H - 2):
                for x in range(W):
                    rood = ((x // cel) + ((y - 2) // cel)) % 2 == 0
                    a[y, x, :3] = RUIT_ROOD if rood else RUIT_WIT
            a[0:2, :, :3] = HOUT_DONKER
            a[H - 2:H, :, :3] = HOUT_DONKER
        else:
            a[H - 3:H, :, :3] = np.array(HOUT_DONKER, np.float32)
            a[0:1, :, :3] = np.array(HOUT_DONKER, np.float32)
            for x in range(3, W, 14):                              # two little staples that hold the hoop together
                a[H // 2 - 2:H // 2 + 2, x:x + 1, :3] = (120, 120, 126)
        a[:, 0:1, :3] *= 0.88
        a[:, W - 1:W, :3] *= 0.88
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def doos_boven(W, H, rng):
    """The open top of the box: the wooden rim and the dark inside."""
    a = hout()(W, H, rng).astype(np.float32)
    r = 5
    a[r:H - r, r:W - r, :3] = np.array(HOUT_BINNEN, np.float32) + rng.normal(0, 3, (H - 2 * r, W - 2 * r, 1))
    a[r:r + 2, r:W - r, :3] *= 0.75
    return np.clip(a, 0, 255).astype(np.uint8)


def deksel_boven(W, H, rng):
    """The lid's label: a checked rim, a cream round label with a wedge of gatenkaas and two little guh ears on it."""
    a = hout()(W, H, rng).astype(np.float32)
    cel = 4
    for y in range(H):
        for x in range(W):
            d = math.hypot(x + 0.5 - W / 2, y + 0.5 - H / 2)
            if d > W * 0.44 and min(x, y, W - 1 - x, H - 1 - y) >= 2:
                a[y, x, :3] = RUIT_ROOD if ((x // cel) + (y // cel)) % 2 == 0 else RUIT_WIT
    bd.disc(a, W / 2, H / 2, W * 0.4, (150, 40, 50), 1.0)
    bd.disc(a, W / 2, H / 2, W * 0.36, (252, 240, 214), 1.0)
    for sx in (-1, 1):                                              # the guh ears on the label
        bd.disc(a, W / 2 + sx * W * 0.17, H * 0.3, W * 0.085, GUH, 1.0)
        bd.disc(a, W / 2 + sx * W * 0.17, H * 0.31, W * 0.045, GUH_OOR, 1.0)
    cx, cy = W / 2, H * 0.58                                        # a wedge of cheese under them
    for y in range(int(cy - H * 0.16), int(cy + H * 0.16)):
        t = (y - (cy - H * 0.16)) / (H * 0.32)
        half = W * 0.22 * t
        for x in range(int(cx - half), int(cx + half) + 1):
            a[y, x, :3] = KAAS if t < 0.82 else KAAS_DONKER
    for (dx, dy, r) in ((-0.04, 0.04, 0.03), (0.07, 0.09, 0.025), (0.0, 0.11, 0.02)):
        bd.disc(a, cx + dx * W, cy + dy * H, r * W, KAAS_GAT, 1.0)
    a[0:2, :, :3] = HOUT_DONKER
    a[H - 2:H, :, :3] = HOUT_DONKER
    a[:, 0:2, :3] = HOUT_DONKER
    a[:, W - 2:W, :3] = HOUT_DONKER
    return np.clip(a, 0, 255).astype(np.uint8)


DEKSEL_OPEN = -78          # the lid thrown back (degrees around its hinge)
KOP_UIT = 2.4              # how far the head sticks out of the box in the model's own pose (model pixels)


def schuilguh(atlas):
    vacht = bd.plain(GUH, 8)
    binnen = bd.plain(HOUT_BINNEN, 5)
    bones = [bd.bone("root", None, [0, 8, 0], []),
             bd.bone("doos", "root", [0, 0, 0], [
                 bd.cube(atlas, [-7, 0, -7], [14, 7, 14], doos_zij(), overrides={"up": doos_boven, "down": hout(donker=0.86)})]),
             # the lid: a little wider than the box, on a hinge at the back
             # the lid: a little wider than the box, on a hinge at the back. The model's own pose is the open one (lid thrown
             # back, head out): that is what a still picture shows; the animations close it
             bd.bone("deksel", "root", [0, 7, 7.5], [
                 bd.cube(atlas, [-7.5, 7, -7.5], [15, 3.5, 15], doos_zij(lid=True), overrides={"up": deksel_boven, "down": binnen})],
                     rotation=[DEKSEL_OPEN, 0, 0]),
             bd.bone("head", "root", [0, 4 + KOP_UIT, 0], [
                 bd.cube(atlas, [-5, 2.5 + KOP_UIT, -5.4], [10, 8, 9], vacht, overrides={
                     "north": bd.guh_face(GUH, eye_y=0.42, eye_dx=0.25, eye_r=0.17)})])]
    bones += bd.ears(atlas, GUH, GUH_OOR, 3.2, 9.6 + KOP_UIT, -2.9, 3.6, 3.6)
    return bd.geo("schuilguh", bones, 1.6, 1.8)


def schuilguh_anims():
    kf = lambda pairs: {str(round(t, 3)): v for t, v in pairs}  # noqa: E731

    def anim(length, bones, loop=True):
        return {"loop": loop, "animation_length": length, "bones": bones}
    # (the model's own pose is open: positions and the lid's angle count from there)
    IN, GLUUR, UIT = [0, -6.2 - KOP_UIT, 0.4], [0, -2.6 - KOP_UIT, -0.2], [0, 0, 0]   # the head: tucked in, peeking, all the way out
    TOE, KIER, OPEN = [-DEKSEL_OPEN, 0, 0], [-DEKSEL_OPEN - 24, 0, 0], [0, 0, 0]       # the lid: shut, ajar, thrown back
    klein = [0.9, 0.9, 0.9]
    dicht = {"head": {"position": kf([(0, IN), (2, IN)]), "scale": kf([(0, klein), (2, klein)])},
             "deksel": {"rotation": kf([(0, TOE), (1.6, TOE), (1.7, [TOE[0] - 3, 0, 0]), (1.8, TOE), (2, TOE)])}}
    gluur = {"head": {"position": kf([(0, GLUUR), (1.5, [GLUUR[0], GLUUR[1] + 0.4, GLUUR[2]]), (3, GLUUR)])},
             "deksel": {"rotation": kf([(0, KIER), (1.5, [KIER[0] - 3, 0, 0]), (3, KIER)])},
             "ear_left": {"rotation": kf([(0, [0, 0, 40]), (3, [0, 0, 40])])}, "ear_right": {"rotation": kf([(0, [0, 0, -40]), (3, [0, 0, -40])])}}
    open_ = {"head": {"position": kf([(0, UIT), (1.5, [0, 0.5, 0]), (3, UIT)]),
                      "rotation": kf([(0, [0, 0, -3]), (1.5, [0, 0, 3]), (3, [0, 0, -3])])},
             "deksel": {"rotation": kf([(0, OPEN), (3, OPEN)])}}
    open_.update({"ear_left": {"rotation": kf([(0, [0, 0, 0]), (2.3, [0, 0, 0]), (2.45, [0, 0, -18]), (2.6, [0, 0, 0]), (3, [0, 0, 0])])},
                  "ear_right": {"rotation": kf([(0, [0, 0, 0]), (1.0, [0, 0, 0]), (1.15, [0, 0, 18]), (1.3, [0, 0, 0]), (3, [0, 0, 0])])}})
    # the kiss: it leans far out with its snoet forward, a little smack, and back
    kusje = {"head": {"position": kf([(0, UIT), (0.25, [0, 1.2, -3.0]), (0.4, [0, 1.2, -3.8]), (0.5, [0, 1.2, -3.0]), (0.9, UIT)]),
                      "rotation": kf([(0, [0, 0, 0]), (0.25, [14, 0, 0]), (0.5, [14, 0, 0]), (0.9, [0, 0, 0])]),
                      "scale": kf([(0, [1, 1, 1]), (0.4, [1.08, 1.08, 1.12]), (0.9, [1, 1, 1])])},
             "deksel": {"rotation": kf([(0, OPEN), (0.9, OPEN)])},
             "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.4, [0, 0, -26]), (0.9, [0, 0, 0])])},
             "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.4, [0, 0, 26]), (0.9, [0, 0, 0])])}}
    eet = {"head": {"position": kf([(0, UIT), (1.2, UIT)]),
                    "rotation": kf([(0, [0, 0, 0]), (0.2, [22, 0, 0]), (0.35, [14, 0, 0]), (0.5, [22, 0, 0]), (0.65, [14, 0, 0]), (0.8, [22, 0, 0]),
                                    (1.2, [0, 0, 0])])},
           "deksel": {"rotation": kf([(0, OPEN), (1.2, OPEN)])}}
    schrik = {"head": {"position": kf([(0, UIT), (0.12, IN), (0.5, IN)]), "scale": kf([(0, [1, 1, 1]), (0.12, klein), (0.5, klein)])},
              "deksel": {"rotation": kf([(0, OPEN), (0.14, TOE), (0.22, [TOE[0] - 6, 0, 0]), (0.3, TOE), (0.5, TOE)])}}
    return {"format_version": "1.8.0", "animations": {
        "dicht": anim(2.0, dicht), "gluur": anim(3.0, gluur), "open": anim(3.0, open_), "kusje": anim(0.9, kusje, False),
        "eet": anim(1.2, eet, False), "schrik": anim(0.5, schrik, False)}}


# =====================================================================================================================
NODIG = {"ender_mika": ["idle", "walk", "draag", "betrapt", "jat", "flits", "duw"],
         "kaasmijt": ["idle", "walk", "kietel"],
         "schuilguh": ["dicht", "gluur", "open", "kusje", "eet", "schrik"]}
# the bones of the tall Mika shape (build contract 3.3), plus the hand the loot is drawn at
BOTTEN = {"ender_mika": ["root", "body", "head", "ear_left", "ear_right", "arm_left", "arm_right", "leg_left", "leg_right", "tail", "hand_right"],
          "kaasmijt": ["root", "body", "head"], "schuilguh": ["root", "doos", "deksel", "head", "ear_left", "ear_right"]}
# prefixes the offline renderer and the guh model keep for the guh's own variant bones
VERBODEN = ("outfit_", "neck", "teckel", "ender", "koning", "wolk", "zeemeer", "asguh", "pluis", "pinguh", "balto", "mewtwo", "stitch")


def build(h):
    A = h.A
    ender_mika(h)
    for name, maker, anims in (("kaasmijt", kaasmijt, kaasmijt_anims), ("schuilguh", schuilguh, schuilguh_anims)):
        atlas = Vel(name)
        h.w(f"{A}/geckolib/models/entity/{name}.geo.json", maker(atlas))
        h.w(f"{A}/geckolib/animations/entity/{name}.animation.json", anims())
        h.save(Image.fromarray(atlas.img), "entity", f"{name}.png")
        if atlas.gloed:
            h.save(Image.fromarray(atlas.glowmask()), "entity", f"{name}_glowmask.png")


def check(h):
    problems = []
    for name, wanted in NODIG.items():
        g = json.load(open(f"{h.A}/geckolib/models/entity/{name}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
            if b["name"].startswith(VERBODEN):
                problems.append(f"{name}: bone name {b['name']} clashes with the guh variant bone prefixes")
        for need in BOTTEN[name]:
            if need not in names:
                problems.append(f"{name}: no bone {need}")
        anims = json.load(open(f"{h.A}/geckolib/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
        for an in wanted:
            if an not in anims:
                problems.append(f"{name}: no animation {an}")
        for an, a in anims.items():
            for bn in a.get("bones", {}):
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
    # the tall Mika shape: 46 high in all (the ear tips), the legs 22, the body 12
    g = json.load(open(f"{h.A}/geckolib/models/entity/ender_mika.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
    top = max(c["origin"][1] + c["size"][1] for b in g["bones"] for c in b.get("cubes", []))
    if not 45 <= top <= 47:
        problems.append(f"ender_mika: {top} model pixels high, the tall Mika is 46")
    for f in ("ender_mika_glowmask.png", "kaasmijt_glowmask.png"):
        if not os.path.exists(os.path.join(h.TEX, "entity", f)):
            problems.append(f"missing textures/entity/{f}")
    return problems


def preview(out):
    """Offline renders (front-left, and from behind) of the three creatures into out/."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo_dir = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    tiles = []
    for name in NODIG:
        q = wr.geo_quads(os.path.join(geo_dir, f"{name}.geo.json"), f"guhs:entity/{name}")
        for yaw, pitch in ((200, -14), (20, -14), (180, -4)):
            img = wr.render(q, yaw, pitch, 360, margin=0.06)
            tiles.append(img)
            img.save(os.path.join(out, f"{name}_{yaw}.png"))
    sheet = Image.new("RGBA", (360 * 3, 360 * ((len(tiles) + 2) // 3)), (236, 232, 240, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 3) * 360, (i // 3) * 360))
    sheet.save(os.path.join(out, "einde2_mikas_sheet.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
