"""
einde2, slice sponsguh_wezens - the looks of the cast of the Sponsguh story: GeckoLib models, animations and textures of
the twenty characters and of the Knabbelburgermobiel.

Every character is recognisable at a glance AND clearly a guh or a Mika: a guh has the big glossy guh eyes (dark pupil, a
blue-to-teal ring, two white highlights), blush, a tiny snoet and two ROUND guh ears with a darker inside; a Mika has the
Mika's head: red slit eyes under dark brows, a little fang grin and its two big round ears with a pink inside.
Textures are painted face by face on an atlas (boerderij_dieren.Atlas: every cube face its own patch); a big model gets
fewer texels per model pixel (Vel.k), so David Hasselguh (nine blocks long) still fits on one sheet.

  build(h)       writes geo/entity/<id>.geo.json, animations/entity/<id>.animation.json, textures/entity/<id>.png for every
                 model (+ the extra textures: octoliam, loopgraafmonster_<0..2> with their glowmasks, bikinibodem_guh_<0..5>)
  check(h)       every animated bone exists, the animations the Java code asks for exist (NODIG), no model without one
  preview(out)   (python tools/features/einde2_sponsguh_wezens_modellen.py <out>) offline renders of all of them
Bones every character has: root, body, head (+ ear_left, ear_right and, where it has them, arm_*, leg_*, tail).
Animations every character has: idle, loop, praat, lach, schrik, juich; the specials are listed in NODIG (= the Java
table Cast). A synced flag or a look ("Soort") shows and hides bones through the looping animations vlag_aan / vlag_uit and
soort_<n> (the renderer never has to know a bone).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import boerderij_dieren as bd
    from features import einde2_sp_cast_looks as looks
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import boerderij_dieren as bd
    from features import einde2_sp_cast_looks as looks

plain, guh_face, cube, bone, round_ear = bd.plain, bd.guh_face, bd.cube, bd.bone, bd.round_ear
S = bd.S


def disc(a, cx, cy, r, colour, alpha=1.0, ry=None):
    """boerderij_dieren.disc (a filled ellipse, blended), without the pixel loop: this module paints thousands of them."""
    H, W = a.shape[:2]
    ry = ry or r
    y0, y1 = max(0, int(cy - ry - 1)), min(H, int(cy + ry + 2))
    x0, x1 = max(0, int(cx - r - 1)), min(W, int(cx + r + 2))
    if y0 >= y1 or x0 >= x1:
        return
    yy, xx = np.mgrid[y0:y1, x0:x1]
    m = ((xx + 0.5 - cx) / r) ** 2 + ((yy + 0.5 - cy) / ry) ** 2 <= 1
    blok = a[y0:y1, x0:x1]
    blok[m, :3] = blok[m, :3] * (1 - alpha) + np.array(colour, np.float32)[:3] * alpha
    blok[m, 3] = 255

# --- colours ------------------------------------------------------------------------------------------------------------------
GUH = (170, 140, 198)            # the normal Guhmensie fur
GUH_DONKER = (136, 104, 170)
GUH_LICHT = (214, 194, 232)
OOR_BINNEN = (236, 128, 168)
MIKA = (228, 170, 186)           # the Mika's pink
MIKA_OOR = (214, 110, 132)
KAAS = (250, 214, 74)
KAAS_GAT = (214, 160, 38)
KAAS_DIEP = (176, 120, 24)
BRUIN = (136, 86, 44)
WIT = (248, 248, 250)
ROOD = (214, 44, 48)
ZWART = (30, 28, 34)
GOUD = (250, 200, 60)
PATGUH = (255, 150, 176)
PATGUH_DONKER = (232, 112, 146)
KRAB = (224, 62, 50)
KRAB_LICHT = (246, 120, 92)
PAREL = (150, 160, 176)
OCTO = (120, 168, 164)           # grey-teal
OCTO_LICHT = (168, 204, 198)
PLANK = (44, 104, 62)
PUF = (236, 206, 130)
PUF_VLEK = (180, 130, 70)
STAART_GROEN = (62, 160, 96)
STAART_TURKOOIS = (64, 196, 196)
LEER = (40, 38, 46)
KOPER = (196, 142, 62)
KOPER_LICHT = (240, 200, 110)
SPOOK = (120, 232, 150)
SPOOK_DONKER = (50, 150, 96)
BROODJE = (232, 170, 86)
BROODJE_LICHT = (248, 206, 130)
SLA = (120, 200, 80)
AUGURK = (94, 150, 60)


# =====================================================================================================================
# the atlas: boerderij_dieren.Atlas with a texel density (k uv units per model pixel) and a glow layer
# =====================================================================================================================
class Vel(bd.Atlas):
    def __init__(self, naam, k=1.0):
        import features.einde2_kit as kit
        super().__init__(kit.seed("sponsguh_wezens/" + naam))
        self.k = k
        self.gloed = np.zeros_like(self.img)

    def alloc(self, w, h):
        try:
            return super().alloc(w, h)
        except SystemExit:
            raise SystemExit("einde2_sponsguh_wezens_modellen: the texture atlas is full (lower this model's k)")

    def patch(self, w, h, painter):
        w, h = max(w * self.k, 0.25), max(h * self.k, 0.25)
        face = super().patch(w, h, painter)
        mask = getattr(painter, "gloeit", None)
        if mask is not None:
            (u, v), W, H = face["uv"], int(math.ceil(w)) * S, int(math.ceil(h)) * S
            arr = self.img[v * S:v * S + H, u * S:u * S + W]
            m = mask(arr)
            self.gloed[v * S:v * S + H, u * S:u * S + W][m] = arr[m]
        return face


def geo(identifier, bones, width, height):
    return bd.geo(identifier, bones, width, height)


# =====================================================================================================================
# painters
# =====================================================================================================================
def alpha(painter, a):
    """The same patch, see-through (glass, ice, a ghost)."""
    def paint(W, H, rng):
        arr = painter(W, H, rng).copy()
        arr[..., 3] = (arr[..., 3].astype(np.float32) * a / 255).astype(np.uint8)
        return arr
    return paint


def gaten(W, H, rng, a, rand=KAAS_GAT, diep=KAAS_DIEP, dichtheid=120, rmin=1.6, rmax=3.6):
    """Round cheese holes (a darker rim, a deep middle) on a patch that is already painted."""
    placed, tries, target = [], 0, max(1, int(W * H / dichtheid))
    while len(placed) < target and tries < target * 30:
        tries += 1
        r = rng.uniform(rmin, rmax)
        cx, cy = rng.uniform(r, max(r + 0.1, W - r)), rng.uniform(r, max(r + 0.1, H - r))
        if any(math.hypot(cx - px, cy - py) < r + pr + 1.2 for px, py, pr in placed):
            continue
        placed.append((cx, cy, r))
        disc(a, cx, cy, r, rand, 1.0)
        disc(a, cx + 0.3, cy + 0.4, r * 0.6, diep, 1.0)


def gatenkaas(base=KAAS, **kw):
    return plain(base, 8, lambda W, H, rng, a: gaten(W, H, rng, a, **kw))


def strepen(c1, c2, breed=4, staand=False, var=6):
    def paint(W, H, rng):
        a = plain(c1, var)(W, H, rng).astype(np.float32)
        for i in range(0, (W if staand else H), breed * 2):
            if staand:
                a[:, i:i + breed, :3] = np.array(c2) + rng.normal(0, var / 3, (H, min(breed, W - i), 1))
            else:
                a[i:i + breed, :, :3] = np.array(c2) + rng.normal(0, var / 3, (min(breed, H - i), W, 1))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def stippen(base, stip, elke=8, r=1.6, hart=None, var=6):
    """Dots or little flowers (hart = the flower's middle)."""
    def extra(W, H, rng, a):
        for j, y in enumerate(range(elke // 2, H + elke, elke)):
            for x in range((elke // 2) * (j % 2), W + elke, elke):
                disc(a, x + rng.uniform(-0.6, 0.6), y + rng.uniform(-0.6, 0.6), r, stip, 1.0)
                if hart:
                    disc(a, x, y, r * 0.4, hart, 1.0)
    return plain(base, var, extra)


def schubben(base, licht, rij=5):
    """Fish scales: rows of little arcs."""
    def extra(W, H, rng, a):
        for j, y in enumerate(range(0, H + rij, rij)):
            for x in range((rij // 2) * (j % 2) - rij, W + rij, rij):
                disc(a, x + rij / 2, y, rij * 0.52, np.array(base) * 0.82, 1.0)
                disc(a, x + rij / 2, y - 0.9, rij * 0.5, licht if (x + j) % 3 == 0 else base, 1.0)
    return plain(base, 6, extra)


def haar(base, licht, n=30):
    """Hair or a beard: soft vertical locks."""
    def extra(W, H, rng, a):
        for _ in range(max(2, W * H // n)):
            x, y, l = int(rng.uniform(0, W)), int(rng.uniform(0, H)), int(rng.uniform(3, 8))
            a[y:y + l, x:x + 1, :3] = licht if rng.random() < 0.5 else np.array(base) * 0.8
    return plain(base, 8, extra)


def metaal(base, licht, klinknagels=True):
    """Old brass or steel: a soft shine from the top left and rivets along the edge."""
    def paint(W, H, rng):
        a = plain(base, 8)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        t = np.clip(1 - (xx / max(1, W) + yy / max(1, H)), 0, 1)[..., None]
        a[..., :3] = a[..., :3] * (1 - 0.5 * t) + np.array(licht, np.float32) * 0.5 * t
        if klinknagels and W >= 16 and H >= 16:
            for x in range(3, W - 2, 8):
                disc(a, x, 2.5, 1.1, np.array(base) * 0.6)
                disc(a, x, H - 2.5, 1.1, np.array(base) * 0.6)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def rond(painter):
    """The patch cut round (see-through corners): a wheel, a starfish arm's tip, an eyeball."""
    def paint(W, H, rng):
        a = painter(W, H, rng).copy()
        yy, xx = np.mgrid[0:H, 0:W]
        a[((xx + 0.5 - W / 2) / (W / 2)) ** 2 + ((yy + 0.5 - H / 2) / (H / 2)) ** 2 > 1, 3] = 0
        return a
    return paint


def los_oog(fur, mika=False, kleur=None):
    """ONE eye filling the patch (an eye on a stalk, Plankmika's one eye, a diver's window)."""
    def paint(W, H, rng):
        a = plain(fur, 6)(W, H, rng).astype(np.float32)
        cx, cy, r = W / 2, H / 2, min(W, H) * 0.44
        yy, xx = np.mgrid[0:H, 0:W]
        dx, dy = (xx + 0.5 - cx) / r, (yy + 0.5 - cy) / r
        d = np.hypot(dx, dy)
        binnen = d <= 1
        if mika:
            a[binnen, :3] = kleur or (226, 40, 36)
            a[binnen & (np.abs(dx) < 0.16 * (1 - np.abs(dy)) + 0.05), :3] = (20, 8, 10)      # the slit
            a[binnen & (d > 0.86), :3] = (60, 10, 16)
        else:
            a[binnen, :3] = (18, 18, 34)
            ring = binnen & (dy > -0.05) & (d > 0.5)
            t = np.clip((dy + 0.05) / 0.9, 0, 1)
            a[ring, 0], a[ring, 1], a[ring, 2] = (40 + 50 * t)[ring], (100 + 110 * t)[ring], (200 + 20 * t)[ring]
            a[binnen & ((d > 0.86) | ((dy < -0.55) & (d > 0.78))), :3] = (22, 16, 34)
        disc(a, cx + 0.3 * r, cy - 0.34 * r, 0.26 * r, (255, 255, 255))
        if not mika:
            disc(a, cx - 0.32 * r, cy + 0.28 * r, 0.12 * r, (226, 232, 240))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def mika_gezicht(fur, eye_y=0.42, eye_dx=0.24, eye_r=0.15, brauw="boos", bril=False, mond="grijns", oog=(226, 40, 36), spots=None):
    """The Mika's face on a fur patch: red eyes with a slit pupil under dark slanted brows, a small pink nose and a grin with
    a little fang. brauw: boos (slanted), moe (heavy lids: Octo), een (one long eyebrow: Octoliam); bril = sunglasses."""
    def paint(W, H, rng):
        a = plain(fur, 8, spots)(W, H, rng).astype(np.float32)
        r = eye_r * W
        donker = (58, 20, 40)
        for sx in (-1, 1):
            cx, cy = W / 2 + sx * eye_dx * W, H * eye_y
            yy, xx = np.mgrid[0:H, 0:W]
            dx, dy = (xx + 0.5 - cx) / r, (yy + 0.5 - cy) / (r * 0.92)
            d = np.hypot(dx, dy)
            binnen = d <= 1
            a[binnen, :3] = oog
            a[binnen & (np.abs(dx) < 0.17 * (1 - np.abs(dy)) + 0.05), :3] = (20, 8, 10)
            a[binnen & (d > 0.84), :3] = (70, 12, 18)
            if brauw == "moe":
                a[binnen & (dy < -0.15), :3] = np.array(fur) * 0.74                    # a heavy eyelid
            disc(a, cx + 0.3 * r, cy - 0.05 * r if brauw == "moe" else cy - 0.36 * r, 0.2 * r, (255, 236, 230))
            if brauw == "boos":                                               # slanted down to the nose
                for x in range(int(cx - 1.25 * r), int(cx + 1.25 * r) + 1):
                    t = (x - cx) / (1.25 * r) * sx
                    y0 = cy - 1.05 * r + t * -0.55 * r
                    for y in range(int(y0 - 0.2 * r), int(y0 + 0.16 * r) + 1):
                        if 0 <= y < H and 0 <= x < W:
                            a[y, x, :3] = donker
        if brauw in ("een", "moe"):
            y0 = H * eye_y - (1.3 if brauw == "een" else 1.15) * r
            dik = 0.34 * r if brauw == "een" else 0.2 * r
            for x in range(int(W / 2 - (eye_dx * W + 1.2 * r)), int(W / 2 + eye_dx * W + 1.2 * r) + 1):
                for y in range(int(y0 - dik), int(y0 + dik) + 1):
                    if 0 <= y < H and 0 <= x < W:
                        a[y, x, :3] = donker
        if bril:
            y0, y1 = int(H * eye_y - 1.15 * r), int(H * eye_y + 1.0 * r)
            a[y0:y0 + max(1, int(0.3 * r)), :, :3] = (14, 14, 18)
            for sx in (-1, 1):
                cx = W / 2 + sx * eye_dx * W
                a[y0:y1, int(cx - 1.25 * r):int(cx + 1.25 * r), :3] = (14, 14, 18)
                a[y0 + 1:y0 + 3, int(cx - 0.9 * r):int(cx - 0.2 * r), :3] = (120, 130, 150)   # a glint
        ny = H * eye_y + 1.2 * r
        nw = max(1.5, W * 0.05)
        disc(a, W / 2, ny + nw * 0.5, nw, (232, 110, 140), 1.0, ry=nw * 0.7)
        my = int(ny + nw * 2.2)
        breed = W * 0.2
        for x in range(int(W / 2 - breed), int(W / 2 + breed) + 1):
            t = abs(x - W / 2) / breed
            dy = int(round(t * t * 2.5)) * (1 if mond == "zuur" else -1)
            for k in range(max(1, int(H * 0.02))):
                if 0 <= my + dy + k < H and 0 <= x < W:
                    a[my + dy + k, x, :3] = donker
        if mond == "grijns":                                                  # a little fang
            fx = int(W / 2 - breed * 0.5)
            fh = max(2, int(H * 0.06))
            a[my:my + fh, fx:fx + max(1, int(W * 0.035)), :3] = (255, 255, 255)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def met(painter, extra):
    """painter, then extra(W, H, rng, a) on top (teeth, wrinkles, a patch)."""
    def paint(W, H, rng):
        a = painter(W, H, rng).astype(np.float32)
        extra(W, H, rng, a)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def rechthoek(a, x0, y0, x1, y1, colour):
    H, W = a.shape[:2]
    a[max(0, int(y0 * H)):min(H, int(math.ceil(y1 * H))), max(0, int(x0 * W)):min(W, int(math.ceil(x1 * W))), :3] = colour


# =====================================================================================================================
# model parts
# =====================================================================================================================
def oren(atlas, fur, x, y, z, maat=3.6, binnen=OOR_BINNEN, parent="head"):
    """Two round guh ears (thin cubes) at +-x, their round face to the front."""
    return bd.ears(atlas, fur, binnen, x, y, z, maat, maat, parent)


def stok(atlas, x, y, z, hoogte, colour, dik=1.0):
    return cube(atlas, [x - dik / 2, y, z - dik / 2], [dik, hoogte, dik], plain(colour, 6))


def ledematen(atlas, bones, naam, x, y, z, s, painter, parent="body", onder=None, pivot_boven=True):
    """A left/right pair of limbs: the cube of the LEFT one at (x, y, z) with size s, mirrored for the right."""
    for side, sx in (("left", 1), ("right", -1)):
        ox = x if sx > 0 else -x - s[0]
        bones.append(bone(f"{naam}_{side}", parent, [ox + s[0] / 2, y + (s[1] if pivot_boven else 0), z + s[2] / 2],
                          [cube(atlas, [ox, y, z], list(s), painter, overrides={"down": onder} if onder else None)]))


def bij(bones, naam):
    return next(b for b in bones if b["name"] == naam)


# =====================================================================================================================
# the models
# =====================================================================================================================
def sponsguh(a):
    """Sponsguh Kaasbroek: a square block of gatenkaas, guh ears on top, the guh face with two front teeth, square brown
    trousers with a belt, a white collar and a red tie, thin arms and legs."""
    kaas = gatenkaas(KAAS)

    def tandjes(W, H, rng, arr):
        gaten(W, H, rng, arr, dichtheid=260, rmin=1.4, rmax=2.6)

    def gezicht(W, H, rng):
        arr = guh_face(KAAS, eye_y=0.36, eye_dx=0.21, eye_r=0.15, spots=tandjes)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0.41, 0.69, 0.49, 0.80, WIT)                    # two buck teeth
        rechthoek(arr, 0.51, 0.69, 0.59, 0.80, WIT)
        for sx in (-1, 1):                                             # three freckles on each cheek
            for k in range(3):
                disc(arr, W / 2 + sx * W * (0.36 + 0.04 * (k % 2)), H * (0.56 + 0.035 * k), 0.7, KAAS_DIEP)
        return np.clip(arr, 0, 255).astype(np.uint8)

    def broek(W, H, rng):
        arr = plain(BRUIN, 8)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0, 0.0, 1, 0.26, (60, 40, 30))                  # the belt
        for x in range(2, W - 2, 6):
            arr[int(H * 0.08):int(H * 0.2), x:x + 3, :3] = (30, 22, 18)
        return np.clip(arr, 0, 255).astype(np.uint8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [
                 cube(a, [-5.8, 5.4, -2.8], [11.6, 3.4, 5.6], broek, overrides={"up": plain(BRUIN, 6), "down": plain(BRUIN, 6)}),
                 cube(a, [-4.3, 4.2, -1.3], [2.6, 1.4, 2.6], plain(BRUIN, 6)), cube(a, [1.7, 4.2, -1.3], [2.6, 1.4, 2.6], plain(BRUIN, 6)),
                 cube(a, [-5.6, 8.6, -2.6], [11.2, 1.6, 5.2], plain(WIT, 5)),
                 cube(a, [-2.6, 9.2, -3.2], [2.0, 1.2, 0.6], plain(WIT, 4)), cube(a, [0.6, 9.2, -3.2], [2.0, 1.2, 0.6], plain(WIT, 4)),
                 cube(a, [-0.6, 8.8, -3.4], [1.2, 1.2, 0.6], plain(ROOD, 6)), cube(a, [-0.9, 5.6, -3.3], [1.8, 3.4, 0.5], plain(ROOD, 6))]),
             bone("head", "body", [0, 10, 0], [
                 cube(a, [-5.5, 10, -2.5], [11, 10.5, 5], kaas, overrides={"north": gezicht}),
                 cube(a, [-0.6, 13.3, -3.9], [1.2, 1.2, 1.6], plain(KAAS, 5))])]
    bones += oren(a, KAAS, 2.6, 19.6, -0.5, 3.6)
    geel, schoen = plain(KAAS, 6), plain(ZWART, 5)
    for side, sx in (("left", 1), ("right", -1)):
        x = 2.5 if sx > 0 else -3.5
        bones.append(bone(f"leg_{side}", "body", [x + 0.5, 5, 0], [
            cube(a, [x, 1, -0.5], [1, 4, 1], geel), cube(a, [x - 0.15, 1.4, -0.65], [1.3, 1.4, 1.3], plain(WIT, 4)),
            cube(a, [x - 0.5, 0, -2.2], [2, 1.2, 3.2], schoen)]))
        ax = 5.5 if sx > 0 else -6.9
        bones.append(bone(f"arm_{side}", "head", [sx * 5.6, 12.2, 0], [
            cube(a, [ax, 11.2, -1], [1.4, 1.6, 2], plain(WIT, 4)),
            cube(a, [sx * 6.2 - 0.5, 6.0, -0.5], [1, 5.4, 1], geel), cube(a, [sx * 6.2 - 0.8, 5.0, -0.8], [1.6, 1.4, 1.6], geel)]))
    return geo("sponsguh", bones, 1.6, 1.8)


def patguh(a):
    """Patguh Ster: a pink five-point star: the top point with the guh ears, two arm points, two leg points, green trunks
    with purple flowers."""
    roze = plain(PATGUH, 9, lambda W, H, rng, arr: [disc(arr, rng.uniform(0, W), rng.uniform(0, H), 0.8, PATGUH_DONKER, 0.7)
                                                    for _ in range(W * H // 70)])
    zwembroek = stippen((112, 196, 84), (150, 92, 200), 9, 2.0, hart=(250, 230, 120))
    gezicht = guh_face(PATGUH, eye_y=0.40, eye_dx=0.2, eye_r=0.15)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 7, 0], [
                 cube(a, [-5, 5, -2.5], [10, 6, 5], roze),
                 cube(a, [-5.4, 4.6, -2.9], [10.8, 4.6, 5.8], zwembroek)]),
             bone("head", "body", [0, 11, 0], [
                 cube(a, [-4.5, 11, -2.5], [9, 7, 5], roze, overrides={"north": gezicht}),
                 cube(a, [-3.2, 18, -2.2], [6.4, 3, 4.4], roze), cube(a, [-2, 21, -1.8], [4, 2.6, 3.6], roze),
                 cube(a, [-1, 23.6, -1.3], [2, 2, 2.6], roze)])]
    bones += oren(a, PATGUH, 0.9, 23.2, -0.6, 3.0)
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"arm_{side}", "body", [sx * 4.6, 11.4, 0], [
            cube(a, [sx * 4.6 - 1.75, 4.6, -1.75], [3.5, 6.8, 3.5], roze), cube(a, [sx * 4.6 - 1.1, 2.4, -1.1], [2.2, 2.2, 2.2], roze)],
            rotation=[0, 0, -62 * sx]))
        bones.append(bone(f"leg_{side}", "body", [sx * 2.8, 5.4, 0], [
            cube(a, [sx * 2.8 - 2, 0.8, -2], [4, 4.6, 4], roze), cube(a, [sx * 2.8 - 1.3, -1.0, -1.3], [2.6, 1.8, 2.6], roze),
            cube(a, [sx * 2.8 - 2.3, 2.8, -2.3], [4.6, 2.6, 4.6], zwembroek)],
            rotation=[0, 0, -20 * sx]))
    return geo("patguh", bones, 2.0, 2.0)


def meneer_krabguh(a):
    """Meneer Krabguh: a wide flat red crab guh on short legs, two big claws, guh eyes on stalks with the guh ears beside
    them, a light blue shirt; the ice block of his flag Bevroren is the bone ijs."""
    rood = plain(KRAB, 9, lambda W, H, rng, arr: [disc(arr, rng.uniform(0, W), rng.uniform(0, H), 1.0, KRAB_LICHT, 0.6)
                                                  for _ in range(W * H // 90)])
    hemd = (150, 200, 236)

    def voor(W, H, rng):
        arr = plain(KRAB, 8)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0, 0.5, 1, 1, hemd)
        rechthoek(arr, 0.47, 0.5, 0.53, 1, (120, 170, 210))
        for k in range(2):
            disc(arr, W / 2, H * (0.64 + 0.2 * k), 1.0, WIT)
        disc(arr, W / 2, H * 0.2, W * 0.05, (160, 30, 30), 1.0, ry=H * 0.1)        # the snoet
        for x in range(int(W * 0.3), int(W * 0.7)):                               # a wide money smile
            dy = int(round((abs(x - W / 2) / (W * 0.2)) ** 2 * -2))
            arr[int(H * 0.38) + dy:int(H * 0.38) + dy + 2, x, :3] = (120, 20, 26)
        for sx in (-1, 1):
            disc(arr, W / 2 + sx * W * 0.33, H * 0.3, W * 0.06, (255, 150, 186), 0.6, ry=H * 0.08)
        return np.clip(arr, 0, 255).astype(np.uint8)
    blauw = plain(hemd, 6)
    ijs = alpha(plain((176, 226, 250), 14, lambda W, H, rng, arr: [disc(arr, rng.uniform(0, W), rng.uniform(0, H), 1.4, (236, 250, 255), 0.8)
                                                                   for _ in range(W * H // 120)]), 120)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [
                 cube(a, [-7, 3, -3.5], [14, 7, 7], rood, overrides={"north": voor}),
                 cube(a, [-7.2, 2.8, -3.7], [14.4, 3.7, 7.4], blauw, overrides={"north": bd.transparent, "up": bd.transparent}),
                 cube(a, [-5.5, 10, -2.5], [11, 1.2, 5], rood)]),
             bone("head", "body", [0, 10.5, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"steel_{side}", "head", [sx * 2.2, 11, 0], [
            stok(a, sx * 2.2, 11, 0, 5.5, KRAB, 1.2),
            cube(a, [sx * 2.2 - 1.8, 16, -1.6], [3.6, 3.8, 3.2], plain(WIT, 4), overrides={"north": los_oog(WIT)})]))
    bones += oren(a, KRAB, 4.6, 10.6, -0.5, 3.4)
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"arm_{side}", "body", [sx * 7, 7.5, 0], [
            cube(a, [sx * 8.5 - 1.5, 6.5, -1], [3, 2, 2], rood),
            cube(a, [sx * 12 - 2.5, 5.5, -2], [5, 4.6, 4], rood), cube(a, [sx * 12 - 2.5, 10.1, -2], [5, 1.6, 1.6], rood),
            cube(a, [sx * 12 - 2.5, 10.1, 0.4], [5, 1.6, 1.6], plain(KRAB_LICHT, 6))]))
        for k, z in enumerate((-2.2, 1.2)):
            x = sx * 4.6 - 0.6
            bones.append(bone(f"leg_{side}" if k == 0 else f"achterpoot_{side}", "body", [x + 0.6, 3, z + 0.6], [
                cube(a, [x, 0, z], [1.2, 3.2, 1.2], rood)]))
    bones.append(bone("ijs", "root", [0, 0, 0], [cube(a, [-16, 0, -6], [32, 22, 12], ijs)]))
    return geo("meneer_krabguh", bones, 2.6, 1.6)


def parel_guh(a):
    """Parel: a tall whale-ish grey guh with a big square head, a blond ponytail, lipstick and a pink dress."""
    grijs = plain(PAREL, 8)
    jurk = stippen((250, 130, 180), (255, 200, 222), 10, 1.4)

    def gezicht(W, H, rng):
        arr = guh_face(PAREL, eye_y=0.36, eye_dx=0.23, eye_r=0.13, snoet=False)(W, H, rng).astype(np.float32)
        disc(arr, W / 2, H * 0.72, W * 0.16, (236, 60, 110), 1.0, ry=H * 0.07)     # big lips
        arr[int(H * 0.72):int(H * 0.72) + 1, int(W * 0.37):int(W * 0.63), :3] = (150, 20, 60)
        return np.clip(arr, 0, 255).astype(np.uint8)
    blond = haar((250, 220, 110), (255, 240, 170))
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [
                 cube(a, [-4.5, 13, -3], [9, 11, 6], jurk),
                 cube(a, [-6, 8, -4], [12, 6, 8], jurk), cube(a, [-7, 7, -4.6], [14, 1.6, 9.2], plain(WIT, 4))]),
             bone("head", "body", [0, 24, 0], [
                 cube(a, [-7, 24, -6], [14, 13, 11], grijs, overrides={"north": gezicht}),
                 cube(a, [-7.3, 34.5, -6.3], [14.6, 2.8, 11.6], blond),
                 cube(a, [-1, 37, 1], [2, 2.4, 2], plain((240, 90, 140), 5))]),
             bone("staartje", "head", [0, 38, 3], [
                 cube(a, [-1.6, 37.6, 3], [3.2, 3, 4], blond), cube(a, [-1.3, 32, 6.4], [2.6, 7, 2.6], blond),
                 cube(a, [-0.9, 29.4, 6.8], [1.8, 2.8, 1.8], blond)])]
    bones += oren(a, PAREL, 5.6, 35.6, -1.5, 4.2)
    ledematen(a, bones, "arm", 4.5, 15, -1.2, (2.4, 8.6, 2.4), grijs)
    ledematen(a, bones, "leg", 1.0, 0, -1.4, (2.8, 8.2, 2.8), grijs, onder=plain((240, 90, 140), 5))
    for side in ("left", "right"):
        b = bij(bones, f"leg_{side}")
        x = b["cubes"][0]["origin"][0]
        b["cubes"].append(cube(a, [x - 0.3, 0, -2.6], [3.4, 1.6, 4.4], plain((240, 90, 140), 5)))
    return geo("parel_guh", bones, 2.2, 3.4)


def sandy_guh(a):
    """Sandy-guh: a plain Guhmensie guh, upright, in a white air suit (a blue and a red ring on arms and legs, an acorn badge)
    with a round glass helmet and a pink flower on it."""
    pak = plain(WIT, 5)

    def borst(W, H, rng):
        arr = plain(WIT, 5)(W, H, rng).astype(np.float32)
        disc(arr, W / 2, H * 0.42, W * 0.14, (176, 120, 60), 1.0, ry=H * 0.14)     # the acorn
        rechthoek(arr, 0.36, 0.24, 0.64, 0.36, (120, 76, 36))
        rechthoek(arr, 0, 0.86, 1, 1, (150, 110, 200))                            # a purple hem
        return np.clip(arr, 0, 255).astype(np.uint8)

    def mouw(W, H, rng):
        arr = plain(WIT, 5)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0, 0.62, 1, 0.74, (70, 120, 220))
        rechthoek(arr, 0, 0.78, 1, 0.9, ROOD)
        return np.clip(arr, 0, 255).astype(np.uint8)
    vacht = plain(GUH, 8)
    glas = alpha(plain((214, 240, 250), 5, lambda W, H, rng, arr: rechthoek(arr, 0.12, 0.1, 0.2, 0.5, (255, 255, 255))), 70)
    bloem, hart = plain((255, 130, 190), 6), plain((250, 220, 90), 5)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(a, [-4, 5, -2.5], [8, 8, 5], pak, overrides={"north": borst})]),
             bone("tail", "body", [0, 6.5, 2.5], [cube(a, [-0.75, 6, 2.5], [1.5, 1, 6], vacht)]),
             bone("head", "body", [0, 13, 0], [
                 cube(a, [-5.5, 13, -4.5], [11, 8.5, 8], vacht, overrides={"north": guh_face(GUH, eye_y=0.42, eye_dx=0.25, eye_r=0.17)}),
                 cube(a, [-3, 12.4, -3.4], [6, 1, 6.4], pak)])]
    bones += oren(a, GUH, 3.8, 19.8, -1.5, 3.8, binnen=GUH_DONKER)
    bones.append(bone("helm", "head", [0, 13, 0], [
        cube(a, [-8.2, 12.6, -5.6], [16.4, 12.8, 11.2], glas),
        cube(a, [-1.2, 25.4, -1.2], [2.4, 0.8, 2.4], hart), cube(a, [-3, 25.4, -0.8], [1.8, 0.8, 1.6], bloem),
        cube(a, [1.2, 25.4, -0.8], [1.8, 0.8, 1.6], bloem), cube(a, [-0.8, 25.4, -3], [1.6, 0.8, 1.8], bloem),
        cube(a, [-0.8, 25.4, 1.2], [1.6, 0.8, 1.8], bloem)]))
    ledematen(a, bones, "arm", 4, 6.2, -1.2, (2.2, 6.6, 2.4), mouw, onder=plain(GUH, 6))
    ledematen(a, bones, "leg", 0.8, 0, -1.4, (2.6, 5.2, 2.8), mouw, onder=plain((150, 110, 200), 5))
    return geo("sandy_guh", bones, 1.8, 2.0)


def gerrit(a):
    """Slak Gerrit: a snail with guh eyes on two stalks, little guh ears, a pink shell with a blue swirl."""
    lijf = plain((150, 196, 226), 7)

    def huisje(W, H, rng):
        arr = plain((250, 150, 190), 8)(W, H, rng).astype(np.float32)
        cx, cy = W / 2, H / 2
        for t in np.linspace(0, 5.2 * math.pi, 700):                             # the blue swirl
            r = 0.9 + t / (5.2 * math.pi) * min(W, H) * 0.42
            disc(arr, cx + math.cos(t) * r, cy + math.sin(t) * r, 0.9, (90, 130, 226))
        for k in range(5):
            disc(arr, rng.uniform(2, W - 2), rng.uniform(2, H - 2), 1.0, (255, 200, 222), 0.8)
        return np.clip(arr, 0, 255).astype(np.uint8)

    def snoet(W, H, rng):
        arr = plain((150, 196, 226), 6)(W, H, rng).astype(np.float32)
        disc(arr, W / 2, H * 0.35, W * 0.09, (214, 96, 150), 1.0, ry=H * 0.12)
        for x in range(int(W * 0.3), int(W * 0.7)):
            dy = int(round((abs(x - W / 2) / (W * 0.2)) ** 2 * -2))
            arr[int(H * 0.72) + dy, x, :3] = (80, 110, 150)
        for sx in (-1, 1):
            disc(arr, W / 2 + sx * W * 0.32, H * 0.55, W * 0.1, (255, 150, 186), 0.6, ry=H * 0.14)
        return np.clip(arr, 0, 255).astype(np.uint8)
    roze = plain((250, 150, 190), 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1, 0], [
                 cube(a, [-2.2, 0, -5], [4.4, 2.2, 10], lijf, overrides={"north": snoet}),
                 cube(a, [-1.4, 0, 5], [2.8, 1.2, 2], lijf)]),
             bone("huisje", "body", [0, 2, 1.5], [
                 cube(a, [-2.4, 2, -1.6], [4.8, 6.4, 6.4], roze, overrides={"east": huisje, "west": huisje}),
                 cube(a, [-2.0, 8.4, -0.6], [4, 0.8, 4.4], roze), cube(a, [-2.0, 3, 4.8], [4, 4.4, 0.8], roze)]),
             bone("head", "body", [0, 2, -3.6], [])]
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"steel_{side}", "head", [sx * 1.3, 2, -3.6], [
            stok(a, sx * 1.3, 2, -3.6, 4.2, (150, 196, 226), 0.8),
            cube(a, [sx * 1.3 - 1.3, 6, -4.8], [2.6, 2.6, 2.2], plain(WIT, 4), overrides={"north": los_oog(WIT)})]))
        bones.append(bone(f"ear_{side}", f"steel_{side}", [sx * 2.6, 8.4, -3.7], [
            cube(a, [sx * 2.6 - 0.9, 7.6, -4.0], [1.8, 1.8, 0.6], plain((150, 196, 226), 5),
                 overrides={"north": round_ear((150, 196, 226), OOR_BINNEN), "south": round_ear((150, 196, 226), OOR_BINNEN)})]))
    return geo("gerrit", bones, 1.0, 0.9)


def octo(a, naam="octo_mika", huid=OCTO, licht=OCTO_LICHT, hemd=(150, 100, 58), gezicht=None):
    """The tall Mika shape (46 model pixels high: build contract 3.3, the same numbers as the Ender-Mika): the Mika's head with
    its round ears on a thin body with long arms. Octo Tentakel: grey-teal, heavy eyelids, a long droopy nose, a bald dome
    between the ears, a brown shirt, FOUR tentacles for legs, no tail, a clarinet in his hand. Octoliam: the same model with
    another texture (one long eyebrow, a red velvet jacket with a golden collar)."""
    vel = plain(huid, 8)
    gezicht = gezicht or mika_gezicht(huid, eye_y=0.36, eye_dx=0.23, eye_r=0.15, brauw="moe", mond="zuur", oog=(226, 196, 60))

    def jas(W, H, rng):
        arr = plain(hemd, 8)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0.44, 0, 0.56, 1, np.array(hemd) * 0.7)
        if naam == "octoliam":
            rechthoek(arr, 0, 0, 1, 0.14, GOUD)
            for k in range(3):
                disc(arr, W / 2, H * (0.3 + 0.22 * k), 1.1, GOUD)
        else:
            rechthoek(arr, 0.3, 0, 0.7, 0.1, np.array(hemd) * 1.2)
        return np.clip(arr, 0, 255).astype(np.uint8)
    zuig = plain(huid, 7, lambda W, H, rng, arr: [disc(arr, W / 2, y, W * 0.22, licht) for y in range(3, H, 7)])
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 22, 0], [cube(a, [-3, 22, -1.5], [6, 12, 3], plain(hemd, 7), overrides={"north": jas})]),
             bone("head", "body", [0, 34, 0], [
                 cube(a, [-5, 34, -4], [10, 9.5, 8], vel, overrides={"north": gezicht}),
                 cube(a, [-3.4, 43.5, -3.2], [6.8, 2.5, 6.4], plain(licht, 6)),          # the bald dome
                 cube(a, [-1.2, 30.2, -5.8], [2.4, 7.4, 2.0], plain(np.array(huid) * 0.84, 6)),   # the droopy nose
                 cube(a, [-3.3, 33.4, -1.8], [6.6, 1.0, 3.6], plain(np.array(hemd) * 1.2, 5))])]
    bones += bd.ears(a, huid, MIKA_OOR, 3.6, 41.5, -1.5, 4.6, 4.6)
    for side, sx in (("left", 1), ("right", -1)):
        ox = 3 if sx > 0 else -5
        bones.append(bone(f"arm_{side}", "body", [ox + 1, 33.5, 0], [
            cube(a, [ox, 14, -1], [2, 20, 2], vel, overrides={"down": plain(licht, 5)})]))
    for i, (x, z) in enumerate(((1.0, -1.6), (-3.0, -1.6), (1.0, 0.6), (-3.0, 0.6)), 1):
        bones.append(bone(f"tentacle_{i}", "body", [x + 1, 22, z + 1], [
            cube(a, [x, 2, z], [2, 20, 2], zuig), cube(a, [x - 0.3 + (0.6 if x > 0 else -0.6), 0, z - (1.4 if z < 0 else -0.4)], [2.6, 2, 3], vel)]))
    hout = plain(ZWART, 5, lambda W, H, rng, arr: [rechthoek(arr, 0, y, 1, y + 0.05, (200, 200, 210)) for y in (0.2, 0.4, 0.6, 0.8)])
    bones.append(bone("clarinet", "arm_right", [-4, 15, -1.5], [
        cube(a, [-4.5, 12, -2.6], [1, 11, 1], hout), cube(a, [-4.9, 10.8, -3.0], [1.8, 1.4, 1.8], plain(ZWART, 5))],
        rotation=[-28, 0, 0]))
    return geo("octo_mika", bones, 1.6, 3.2)


def octoliam(a):
    return octo(a, "octoliam", huid=(132, 176, 168), licht=(180, 212, 204), hemd=(150, 30, 46),
                gezicht=mika_gezicht((132, 176, 168), eye_y=0.36, eye_dx=0.23, eye_r=0.15, brauw="een", mond="grijns", oog=(226, 196, 60)))


def plankmika(a):
    """Plankmika: a tiny dark green Mika, upright, with ONE big red eye under one eyebrow; its two Mika ears are thin as
    antennas with a little round tip."""
    groen = plain(PLANK, 8)

    def gezicht(W, H, rng):
        arr = plain(PLANK, 8)(W, H, rng).astype(np.float32)
        oog = los_oog(PLANK, mika=True)(W, int(H * 0.5), rng)
        arr[int(H * 0.14):int(H * 0.14) + oog.shape[0], :, :3] = oog[..., :3]
        rechthoek(arr, 0.12, 0.06, 0.88, 0.16, (14, 40, 22))                      # the one eyebrow
        for x in range(int(W * 0.25), int(W * 0.75)):                            # an evil little grin
            dy = int(round((abs(x - W / 2) / (W * 0.25)) ** 2 * -2))
            arr[int(H * 0.8) + dy:int(H * 0.8) + dy + 1, x, :3] = (10, 30, 16)
        return np.clip(arr, 0, 255).astype(np.uint8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1.5, 0], []),
             bone("head", "body", [0, 1.5, 0], [
                 cube(a, [-1.6, 1.5, -1.3], [3.2, 4.6, 2.6], groen, overrides={"north": gezicht}),
                 cube(a, [-1.2, 6.1, -1.0], [2.4, 0.6, 2.0], groen)])]
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"ear_{side}", "head", [sx * 0.9, 6.4, 0], [
            stok(a, sx * 0.9, 6.4, 0, 3.4, PLANK, 0.4),
            cube(a, [sx * 0.9 - 0.6, 9.4, -0.3], [1.2, 1.2, 0.6], plain(PLANK, 5),
                 overrides={"north": round_ear(PLANK, MIKA_OOR), "south": round_ear(PLANK, MIKA_OOR)})], rotation=[0, 0, 14 * sx]))
        bones.append(bone(f"arm_{side}", "head", [sx * 1.7, 4.0, 0], [cube(a, [sx * 1.9 - 0.3, 1.8, -0.3], [0.6, 2.4, 0.6], groen)]))
        bones.append(bone(f"leg_{side}", "body", [sx * 0.8, 1.5, 0], [cube(a, [sx * 0.8 - 0.35, 0, -0.35], [0.7, 1.6, 0.7], groen)]))
    return geo("plankmika", bones, 0.8, 0.9)


def juf_pufguh(a):
    """Juf Pufguh: a round puffer guh in a blue sailor dress and hat; the spikes of her flag Opgeblazen are the bone stekels."""
    vel = plain(PUF, 8, lambda W, H, rng, arr: [disc(arr, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.8, 1.5), PUF_VLEK, 0.8)
                                               for _ in range(W * H // 70)])
    blauw = (56, 96, 190)

    def jurk(W, H, rng):
        arr = plain(blauw, 7)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0, 0.0, 1, 0.16, WIT)
        rechthoek(arr, 0, 0.84, 1, 0.92, WIT)
        return np.clip(arr, 0, 255).astype(np.uint8)

    def voor(W, H, rng):
        arr = jurk(W, H, rng).astype(np.float32)
        disc(arr, W / 2, H * 0.3, W * 0.09, ROOD, 1.0, ry=H * 0.14)               # the red knot
        return np.clip(arr, 0, 255).astype(np.uint8)
    gezicht = guh_face(PUF, eye_y=0.40, eye_dx=0.23, eye_r=0.15, spots=lambda W, H, rng, arr: [
        disc(arr, rng.uniform(0, W), rng.uniform(0, H * 0.25), 1.2, PUF_VLEK, 0.8) for _ in range(6)])
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2, 0], [
                 cube(a, [-5, 2, -4.5], [10, 5, 9], plain(blauw, 7), overrides={"north": voor, "south": jurk, "east": jurk, "west": jurk}),
                 cube(a, [-5.8, 1.2, -5.2], [11.6, 1.4, 10.4], plain(WIT, 4))]),
             bone("head", "body", [0, 7, 0], [
                 cube(a, [-5.5, 7, -5], [11, 7.5, 10], vel, overrides={"north": gezicht}),
                 cube(a, [-4.5, 14.5, -4], [9, 1.2, 8], vel),
                 cube(a, [-3.4, 15.6, -3], [6.8, 1.8, 6], plain(WIT, 4)), cube(a, [-4.2, 15.5, -3.8], [8.4, 0.7, 7.6], plain(blauw, 6)),
                 cube(a, [-0.7, 17.4, -0.7], [1.4, 0.9, 1.4], plain(ROOD, 5))]),
             bone("tail", "body", [0, 6, 4.5], [cube(a, [-0.5, 3.5, 4.5], [1, 5, 3], plain(PUF_VLEK, 6))])]
    bones += oren(a, PUF, 4.2, 13.6, -1.5, 3.4)
    ledematen(a, bones, "arm", 5.4, 5.6, -1.5, (0.9, 3.4, 3), plain(PUF_VLEK, 6), parent="head")
    ledematen(a, bones, "leg", 1.2, 0, -1.6, (2, 2, 3), plain(ROOD, 5))
    punt = plain(PUF_VLEK, 6)
    st = []
    for x in (-3.5, 0, 3.5):
        for y in (8.5, 12):
            st.append(cube(a, [x - 0.4, y, -6.2], [0.8, 0.8, 1.2], punt))
            st.append(cube(a, [x - 0.4, y, 5.0], [0.8, 0.8, 1.2], punt))
    for z in (-3, 0.5, 3.5):
        for y in (8.5, 12):
            st.append(cube(a, [-6.7, y, z - 0.4], [1.2, 0.8, 0.8], punt))
            st.append(cube(a, [5.5, y, z - 0.4], [1.2, 0.8, 0.8], punt))
    bones.append(bone("stekels", "head", [0, 10.5, 0], st))
    return geo("juf_pufguh", bones, 2.6, 2.6)


def _zeestaart(a, kleur, licht, y0, breed, diep, hoogte):
    """A mer-guh's tail standing on its fin: three narrowing segments down to two flukes on the floor."""
    sch = schubben(kleur, licht)
    h3 = hoogte / 3
    return [bone("staart_1", "body", [0, y0, 0], [cube(a, [-breed / 2, y0 - h3, -diep / 2], [breed, h3 + 0.4, diep], sch)]),
            bone("staart_2", "staart_1", [0, y0 - h3, 0], [cube(a, [-breed * 0.4, y0 - 2 * h3, -diep * 0.4], [breed * 0.8, h3 + 0.2, diep * 0.8], sch)]),
            bone("staart_3", "staart_2", [0, y0 - 2 * h3, 0], [
                cube(a, [-breed * 0.28, 1.2, -diep * 0.3], [breed * 0.56, y0 - 2 * h3 - 1.0, diep * 0.6], sch),
                cube(a, [-breed * 0.9, 0, -diep * 0.2], [breed * 0.8, 1.4, diep * 1.1], plain(licht, 6)),
                cube(a, [breed * 0.1, 0, -diep * 0.2], [breed * 0.8, 1.4, diep * 1.1], plain(licht, 6))])]


def neptuguh(a):
    """Koning Neptuguh: a huge merman guh king: a green tail, a broad chest, a red beard and hair, a trident, and a golden
    crown (bone kroon); without the crown (flag Kroon off) a shiny bald head (bone kaal)."""
    vacht = plain(GUH, 8)
    baard = haar((206, 60, 40), (240, 110, 70))

    def borst(W, H, rng):
        arr = plain(GUH, 8)(W, H, rng).astype(np.float32)
        for sx in (-1, 1):
            disc(arr, W / 2 + sx * W * 0.2, H * 0.3, W * 0.17, GUH_DONKER, 0.35, ry=H * 0.16)
        rechthoek(arr, 0.48, 0.5, 0.52, 0.95, np.array(GUH) * 0.85)
        rechthoek(arr, 0, 0.9, 1, 1, GOUD)
        return np.clip(arr, 0, 255).astype(np.uint8)
    goud = metaal(GOUD, (255, 244, 170), klinknagels=False)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 22, 0], [cube(a, [-7, 22, -3.5], [14, 13, 7], vacht, overrides={"north": borst})])]
    bones += _zeestaart(a, STAART_GROEN, (150, 226, 150), 22.4, 12, 6.5, 21)
    bones += [bone("head", "body", [0, 35, 0], [
        cube(a, [-6, 35, -5], [12, 10, 9.5], vacht, overrides={"north": guh_face(GUH, eye_y=0.36, eye_dx=0.24, eye_r=0.15)}),
        cube(a, [-6.4, 33.4, -5.6], [12.8, 4.4, 2.6], baard, overrides={"up": bd.transparent}),
        cube(a, [-4.6, 28.6, -5.4], [9.2, 5, 2.2], baard), cube(a, [-2.4, 25.6, -5.2], [4.8, 3.2, 1.8], baard),
        cube(a, [-6.5, 38, 2.5], [13, 7.4, 2.4], baard)])]
    bones += oren(a, GUH, 5.0, 43.4, -1.5, 4.0, binnen=GUH_DONKER)
    bones.append(bone("kaal", "head", [0, 45, 0], [cube(a, [-5, 45, -4], [10, 1.4, 7.5], plain(GUH_LICHT, 4, lambda W, H, rng, arr: disc(
        arr, W * 0.3, H * 0.3, W * 0.12, (255, 255, 255), 0.9)))]))
    kroon = [cube(a, [-5, 45, -4.2], [10, 2.2, 8], goud)]
    kroon += [cube(a, [x, 47.2, z], [1.6, 2.8, 1.6], goud) for x in (-5, -0.8, 3.4) for z in (-4.2, 2.2)]
    kroon.append(cube(a, [-0.9, 46, -4.6], [1.8, 1.4, 0.6], plain((230, 40, 80), 5)))
    bones.append(bone("kroon", "head", [0, 45, 0], kroon))
    ledematen(a, bones, "arm", 7, 23, -2, (4, 11.6, 4), vacht, onder=plain(GUH_LICHT, 5))
    bones.append(bone("drietand", "arm_right", [-9, 24, -3], [
        cube(a, [-9.6, 10, -4.6], [1.2, 38, 1.2], goud), cube(a, [-13.2, 48, -4.6], [8.4, 1.4, 1.2], goud),
        cube(a, [-13.2, 49.4, -4.6], [1.2, 5, 1.2], goud), cube(a, [-9.6, 49.4, -4.6], [1.2, 6.4, 1.2], goud),
        cube(a, [-6.0, 49.4, -4.6], [1.2, 5, 1.2], goud)]))
    return geo("neptuguh", bones, 3.0, 3.8)


def mindy_guh(a):
    """Prinses Mindy-guh: a mermaid guh with glasses, a small crown, dark hair and a turquoise tail."""
    vacht = plain(GUH, 8)
    hr = haar((70, 46, 90), (110, 80, 140))

    def gezicht(W, H, rng):
        arr = guh_face(GUH, eye_y=0.42, eye_dx=0.25, eye_r=0.16)(W, H, rng).astype(np.float32)
        r = 0.2 * W
        for sx in (-1, 1):                                                       # the glasses
            cx, cy = W / 2 + sx * 0.25 * W, H * 0.42
            for t in np.linspace(0, 2 * math.pi, 120):
                x, y = int(cx + math.cos(t) * r), int(cy + math.sin(t) * r * 1.05)
                if 0 <= x < W and 0 <= y < H:
                    arr[y, x, :3] = (30, 24, 40)
        arr[int(H * 0.42), int(W * 0.45):int(W * 0.55), :3] = (30, 24, 40)
        return np.clip(arr, 0, 255).astype(np.uint8)
    top = stippen((150, 110, 200), (220, 190, 240), 7, 1.0)
    goud = plain(GOUD, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 11, 0], [cube(a, [-3.5, 11, -2.2], [7, 6, 4.4], vacht),
                                               cube(a, [-3.7, 13.4, -2.4], [7.4, 2.6, 4.8], top)])]
    bones += _zeestaart(a, STAART_TURKOOIS, (170, 240, 236), 11.3, 7, 4.2, 10.4)
    bones += [bone("head", "body", [0, 17, 0], [
        cube(a, [-5, 17, -4], [10, 8, 7.5], vacht, overrides={"north": gezicht}),
        cube(a, [-5.3, 23.4, -4.3], [10.6, 2, 8.1], hr), cube(a, [-5.3, 17.6, 2.4], [10.6, 6.4, 1.6], hr),
        cube(a, [-1.6, 12, 3], [3.2, 6, 1.6], hr),
        cube(a, [-2, 25.4, -2], [4, 0.9, 3.2], goud), cube(a, [-2, 26.3, -2], [0.9, 1.2, 0.9], goud),
        cube(a, [-0.45, 26.3, -2], [0.9, 1.6, 0.9], goud), cube(a, [1.1, 26.3, -2], [0.9, 1.2, 0.9], goud)])]
    bones += oren(a, GUH, 3.6, 23.6, -1.2, 3.4, binnen=GUH_DONKER)
    ledematen(a, bones, "arm", 3.5, 11.4, -1, (1.8, 5.6, 2), vacht)
    return geo("mindy_guh", bones, 1.8, 2.2)


def dennis_mika(a):
    """Dennis-Mika: a big tough Mika in black leather, sunglasses, a hat and spiked boots; his motorbike is the bone motor
    (flag OpMotor)."""
    leer = plain(LEER, 6)
    vel = plain(MIKA, 8)

    def jas(W, H, rng):
        arr = plain(LEER, 6)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0.47, 0, 0.53, 1, (150, 150, 160))                        # the zip
        rechthoek(arr, 0, 0.84, 1, 0.96, (90, 60, 40))
        disc(arr, W / 2, H * 0.9, 1.6, (200, 200, 210))
        return np.clip(arr, 0, 255).astype(np.uint8)

    def laars(W, H, rng):
        arr = plain((60, 40, 30), 6)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0, 0.0, 1, 0.14, (200, 200, 210))
        return np.clip(arr, 0, 255).astype(np.uint8)
    staal = metaal((150, 150, 160), (236, 236, 244), klinknagels=False)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 11, 0], [cube(a, [-5.5, 11, -3], [11, 12, 6], leer, overrides={"north": jas}),
                                               cube(a, [-7, 20.5, -3.4], [14, 3, 6.8], leer)]),
             bone("head", "body", [0, 23, 0], [
                 cube(a, [-5.5, 23, -4.5], [11, 8.5, 8.5], vel, overrides={"north": mika_gezicht(MIKA, eye_y=0.40, eye_dx=0.24, bril=True, mond="zuur")}),
                 cube(a, [-7, 31.2, -6], [14, 0.8, 11.5], plain((70, 46, 30), 6)), cube(a, [-4.5, 32, -3.5], [9, 3.2, 6.5], plain((70, 46, 30), 6)),
                 cube(a, [-4.6, 32, -3.6], [9.2, 0.9, 6.7], plain(ZWART, 4))])]
    bones += bd.ears(a, MIKA, MIKA_OOR, 4.4, 29.4, -1.5, 4.6, 4.6)
    ledematen(a, bones, "arm", 5.5, 10.5, -1.8, (3.4, 11.6, 3.6), leer, onder=plain(MIKA, 6))
    for side, sx in (("left", 1), ("right", -1)):
        x = 1.0 if sx > 0 else -4.6
        punten = [cube(a, [x + 0.4 + k * 1.2, 2.0, -3.6], [0.6, 0.6, 1.0], staal) for k in range(3)]
        bones.append(bone(f"leg_{side}", "body", [x + 1.8, 11, 0], [
            cube(a, [x, 4, -1.8], [3.6, 7.2, 3.6], leer), cube(a, [x - 0.3, 0, -2.8], [4.2, 4.2, 5.2], laars)] + punten))
    band = rond(plain((34, 32, 38), 6, lambda W, H, rng, arr: disc(arr, W / 2, H / 2, W * 0.26, (170, 170, 180))))
    zwart = plain((34, 32, 38), 5)
    bones.append(bone("motor", "root", [0, 5, 0], [
        cube(a, [-1.2, 0, -15], [2.4, 9, 9], zwart, overrides={"east": band, "west": band}),
        cube(a, [-1.2, 0, 7], [2.4, 9, 9], zwart, overrides={"east": band, "west": band}),
        cube(a, [-2, 5.5, -8], [4, 4.5, 17], plain(ROOD, 7)), cube(a, [-2.4, 9.5, -1], [4.8, 1.4, 9], plain((60, 40, 30), 5)),
        cube(a, [-1, 5, -12], [2, 11, 1.6], staal, pivot=[0, 5, -11], rotation=[-20, 0, 0]),
        cube(a, [-6, 15.4, -8.6], [12, 1.2, 1.2], staal), cube(a, [-1.6, 12.6, -10.6], [3.2, 3.2, 1.6], plain((250, 240, 150), 5)),
        cube(a, [2, 3, 2], [1.4, 1.4, 12], staal)]))
    return geo("dennis_mika", bones, 2.4, 2.6)


def cycloop_mika(a):
    """De Cycloop: a Mika in an old diving suit: a big brass helmet with ONE round window (behind it one red Mika eye), the
    two Mika ears as brass caps on the helmet, lead boots and thick gloves."""
    pak = plain((150, 136, 110), 8, lambda W, H, rng, arr: [rechthoek(arr, 0, y, 1, y + 0.03, (120, 106, 84)) for y in (0.3, 0.6)])
    koper = metaal(KOPER, KOPER_LICHT)

    def raam(W, H, rng):
        arr = metaal(KOPER, KOPER_LICHT)(W, H, rng).astype(np.float32)
        cx, cy, r = W / 2, H * 0.5, W * 0.34
        disc(arr, cx, cy, r, np.array(KOPER) * 0.6)
        disc(arr, cx, cy, r * 0.84, (22, 40, 52))
        oog = los_oog((22, 40, 52), mika=True)(int(r), int(r), rng)
        y0, x0 = int(cy - r / 2), int(cx - r / 2)
        m = np.any(oog[..., :3] != np.array((22, 40, 52)), axis=-1) | True
        blok = arr[y0:y0 + oog.shape[0], x0:x0 + oog.shape[1], :3]
        yy, xx = np.mgrid[0:oog.shape[0], 0:oog.shape[1]]
        binnen = ((xx + 0.5 - oog.shape[1] / 2) ** 2 + (yy + 0.5 - oog.shape[0] / 2) ** 2) <= (oog.shape[0] * 0.44) ** 2
        blok[binnen & m] = oog[..., :3][binnen & m]
        for t in range(8):                                                       # the bolts round the window
            hoek = t * math.pi / 4
            disc(arr, cx + math.cos(hoek) * r * 0.92, cy + math.sin(hoek) * r * 0.92, 1.0, KOPER_LICHT)
        disc(arr, cx - r * 0.4, cy - r * 0.45, r * 0.14, (200, 230, 240), 0.8)
        return np.clip(arr, 0, 255).astype(np.uint8)
    lood = metaal((90, 92, 104), (170, 172, 186), klinknagels=False)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 9, 0], [cube(a, [-5, 9, -3], [10, 13, 6], pak),
                                              cube(a, [-5.6, 19.4, -3.6], [11.2, 3, 7.2], koper),
                                              cube(a, [-1.5, 13, -3.8], [3, 4, 0.8], lood)]),
             bone("head", "body", [0, 22, 0], [
                 cube(a, [-6.5, 22, -6], [13, 12.5, 12], koper, overrides={"north": raam}),
                 cube(a, [-2, 34.5, -2], [4, 1.4, 4], koper)])]
    for side, sx in (("left", 1), ("right", -1)):
        ox = 5.2 if sx > 0 else -10.2
        bones.append(bone(f"ear_{side}", "head", [sx * 7.7, 33, 0], [
            cube(a, [ox, 30.5, -0.8], [5, 5, 1.6], koper, overrides={"north": round_ear(KOPER, MIKA_OOR), "south": round_ear(KOPER, KOPER_LICHT)})]))
    ledematen(a, bones, "arm", 5, 9.6, -1.6, (3, 11, 3.2), pak)
    ledematen(a, bones, "leg", 0.8, 0, -1.6, (3.4, 9, 3.4), pak)
    for side, sx in (("left", 1), ("right", -1)):
        arm, leg = bij(bones, f"arm_{side}"), bij(bones, f"leg_{side}")
        x = arm["cubes"][0]["origin"][0]
        arm["cubes"].append(cube(a, [x - 0.5, 8, -2.1], [4, 3.6, 4.2], plain((120, 80, 50), 6)))
        x = leg["cubes"][0]["origin"][0]
        leg["cubes"].append(cube(a, [x - 0.5, 0, -3.4], [4.4, 3.4, 6.2], lood))
    return geo("cycloop_mika", bones, 2.4, 2.8)


def david_hasselguh(a):
    """David Hasselguh: a giant guh lying flat to swim (about nine blocks long): arms stretched forward, legs kicking, red
    swim trunks, a hairy chest, a wave of dark hair and a wide white smile. The players sit on his back."""
    vacht = plain(GUH, 6)
    krul = haar((70, 44, 30), (120, 84, 60), n=16)

    def gezicht(W, H, rng):
        arr = guh_face(GUH, eye_y=0.36, eye_dx=0.24, eye_r=0.13, snoet=True)(W, H, rng).astype(np.float32)
        disc(arr, W / 2, H * 0.74, W * 0.2, WIT, 1.0, ry=H * 0.07)                 # the wide white smile
        arr[int(H * 0.74), int(W * 0.31):int(W * 0.69), :3] = (200, 200, 210)
        for x in np.linspace(W * 0.34, W * 0.66, 7):
            arr[int(H * 0.69):int(H * 0.8), int(x), :3] = (214, 214, 224)
        return np.clip(arr, 0, 255).astype(np.uint8)
    borst = plain(GUH, 6, lambda W, H, rng, arr: [disc(arr, W / 2 + rng.normal(0, W * 0.14), H * 0.3 + rng.normal(0, H * 0.12), 0.9, (70, 44, 30), 0.9)
                                                  for _ in range(W * H // 12)])
    broek = plain(ROOD, 7, lambda W, H, rng, arr: rechthoek(arr, 0, 0, 1, 0.12, (250, 220, 90)))
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [
                 cube(a, [-18, 4, -30], [36, 20, 62], vacht, overrides={"down": borst}),
                 cube(a, [-18.6, 3.4, 12], [37.2, 21.2, 21], broek)]),
             bone("tail", "body", [0, 12, 33], [cube(a, [-1.5, 14, 32], [3, 2, 14], vacht)]),
             bone("head", "body", [0, 14, -30], [
                 cube(a, [-15, 5, -56], [30, 24, 26], vacht, overrides={"north": gezicht}),
                 cube(a, [-13, 29, -55], [26, 5, 22], krul), cube(a, [-9, 33, -58], [18, 4, 12], krul),
                 cube(a, [-15.6, 20, -40], [31.2, 9.4, 10.6], krul, overrides={"north": bd.transparent})])]
    bones += oren(a, GUH, 11, 26, -46, 11, binnen=GUH_DONKER)
    for side, sx in (("left", 1), ("right", -1)):
        ox = 18 if sx > 0 else -28
        bones.append(bone(f"arm_{side}", "body", [sx * 20, 12, -26], [
            cube(a, [ox, 7, -66], [10, 9, 42], vacht), cube(a, [ox - 1, 6, -76], [12, 11, 11], plain(GUH_LICHT, 5))]))
        ox = 4 if sx > 0 else -16
        bones.append(bone(f"leg_{side}", "body", [sx * 10, 12, 32], [
            cube(a, [ox, 6, 32], [12, 11, 40], vacht), cube(a, [ox - 1, 4, 70], [14, 16, 7], plain(GUH_LICHT, 5))]))
    return geo("david_hasselguh", bones, 12.0, 4.0)


MONSTER_HUID = [(42, 60, 110), (40, 96, 78), (86, 50, 116)]     # per Soort: deep blue, sea green, void purple
MONSTER_OOG = [(255, 236, 90), (150, 255, 170), (255, 150, 236)]


def loopgraafmonster(a, soort=0):
    """A monster of the loopgraaf: a hulking dark blob with a huge jaw full of teeth and glowing eyes, and two tiny guh ears.
    Three looks (NBT Soort): 0 = horns and long clawed arms, 1 = an anglerfish lure and a row of back spikes, 2 = feelers
    and a big frill. Every look has every bone (soort_<n> shows its own)."""
    huid, oog = MONSTER_HUID[soort], MONSTER_OOG[soort]
    vel = plain(huid, 10, lambda W, H, rng, arr: [disc(arr, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.8, 1.8), np.array(huid) * 0.7, 0.8)
                                                 for _ in range(W * H // 50)])
    gloed = lambda arr: (np.abs(arr[..., :3].astype(int) - np.array(oog)).sum(-1) < 40) & (arr[..., 3] > 0)   # noqa: E731

    def kop(W, H, rng):
        arr = vel(W, H, rng).astype(np.float32)
        for sx in (-1, 1):                                                       # glowing eyes: mean on the outside...
            cx, cy = W / 2 + sx * W * 0.22, H * 0.26
            yy, xx = np.mgrid[0:H, 0:W]
            dx, dy = (xx + 0.5 - cx) / (W * 0.14), (yy + 0.5 - cy) / (H * 0.12)
            arr[(dx * dx + dy * dy <= 1) & (dy > -0.5 + 0.5 * dx * sx), :3] = oog
            disc(arr, cx, cy + H * 0.02, W * 0.035, (10, 10, 16))
        rechthoek(arr, 0.06, 0.5, 0.94, 0.94, (16, 10, 24))                       # the jaw
        n = 7
        for k in range(n):                                                       # teeth, top and bottom
            x0 = 0.08 + k * 0.86 / n
            for y in range(int(H * 0.5), int(H * 0.68)):
                t = (y - H * 0.5) / (H * 0.18)
                half = (0.86 / n / 2) * (1 - t) * W
                c = (x0 + 0.86 / n / 2) * W
                arr[y, int(c - half):int(c + half) + 1, :3] = (250, 246, 230)
            for y in range(int(H * 0.8), int(H * 0.94)):
                t = (H * 0.94 - y) / (H * 0.14)
                half = (0.86 / n / 2) * (1 - t) * W * 0.8
                c = (x0 + 0.86 / n) * W
                arr[y, max(0, int(c - half)):int(c + half) + 1, :3] = (250, 246, 230)
        return np.clip(arr, 0, 255).astype(np.uint8)
    kop.gloeit = gloed
    lamp = plain(oog, 4)
    lamp.gloeit = lambda arr: arr[..., 3] > 0
    klauw = plain((236, 230, 210), 5)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 8, 0], [cube(a, [-8, 4, -6], [16, 12, 12], vel), cube(a, [-6, 0, -4.5], [12, 4, 9], vel)]),
             bone("head", "body", [0, 16, 0], [cube(a, [-9, 16, -7.5], [18, 14, 14], vel, overrides={"north": kop}),
                                               cube(a, [-7, 30, -5.5], [14, 2, 10], vel)]),
             bone("tail", "body", [0, 6, 6], [cube(a, [-2, 2, 6], [4, 4, 8], vel), cube(a, [-1, 2.6, 14], [2, 2.4, 5], vel)])]
    bones += oren(a, huid, 6.6, 30.4, -1.5, 3.0, binnen=np.array(oog) * 0.7)
    ledematen(a, bones, "arm", 8, 6, -2, (3.6, 10, 4), vel)
    ledematen(a, bones, "leg", 2, 0, -3.4, (4.4, 4.6, 6), vel, onder=klauw)
    bones.append(bone("soort_a", "head", [0, 30, 0], [
        cube(a, [-8.6, 30, -2], [2.4, 4.6, 2.4], klauw), cube(a, [6.2, 30, -2], [2.4, 4.6, 2.4], klauw),
        cube(a, [-9.6, 34, -1.6], [1.6, 3, 1.6], klauw), cube(a, [8.0, 34, -1.6], [1.6, 3, 1.6], klauw)]))
    for side, sx in (("left", 1), ("right", -1)):
        ox = 8.4 if sx > 0 else -11.6
        bones.append(bone(f"soort_a_klauw_{side}", f"arm_{side}", [sx * 10, 6, 0], [
            cube(a, [ox, 0.4, -2.4], [3.2, 5.6, 4.6], vel)] + [cube(a, [ox + 0.2 + k * 1.1, -1.4, -3.2], [0.7, 2.2, 1.4], klauw) for k in range(3)]))
    bones.append(bone("soort_b", "head", [0, 30, -5], [
        cube(a, [-0.5, 31.6, -6], [1, 6.4, 1], vel), cube(a, [-0.5, 37, -10.6], [1, 1, 5.6], vel),
        cube(a, [-1.5, 33.6, -12.4], [3, 3.4, 3], lamp)]))
    bones.append(bone("soort_b_rug", "body", [0, 16, 5], [cube(a, [-0.8, 16 - 3.2 * k, 5.6 + 0.2 * k], [1.6, 2.6, 2.6 - 0.3 * k], klauw) for k in range(4)]))
    bones.append(bone("soort_c", "head", [0, 22, -7], [
        cube(a, [-10.6, 15.6, -3], [1.6, 15, 9], plain(np.array(oog) * 0.6, 8)), cube(a, [9, 15.6, -3], [1.6, 15, 9], plain(np.array(oog) * 0.6, 8)),
        cube(a, [-5.6, 12, -8.4], [1, 5, 1], vel), cube(a, [4.6, 12, -8.4], [1, 5, 1], vel),
        cube(a, [-2.6, 10.6, -8.4], [1, 6.4, 1], vel), cube(a, [1.6, 10.6, -8.4], [1, 6.4, 1], vel)]))
    return geo("loopgraafmonster", bones, 3.0, 3.0)


BURGER_KLEUR = [((250, 190, 90), (255, 232, 150)), ((120, 190, 236), (200, 232, 250)), ((150, 220, 130), (214, 244, 190)),
                ((250, 150, 130), (255, 212, 196)), ((190, 150, 236), (228, 208, 250)), ((240, 130, 190), (255, 204, 228))]
BURGER_KLEDING = [(214, 60, 60), (250, 240, 236), (70, 110, 200), (120, 76, 44), (250, 200, 80), (60, 150, 130)]


def bikinibodem_guh(a, soort=0):
    """A citizen of Bikinibodem: an upright fish-like guh: fish lips under the guh face, guh ears, a tail fin, arm fins and
    a fin on the head. Six looks (NBT Soort): the colour and clothes, and one of three head fins (soort_<n> shows its own)."""
    kleur, licht = BURGER_KLEUR[soort]
    vel = schubben(kleur, licht, rij=6)
    glad = plain(kleur, 8)
    kleding = BURGER_KLEDING[soort]

    def gezicht(W, H, rng):
        arr = guh_face(kleur, eye_y=0.38, eye_dx=0.24, eye_r=0.16, snoet=False)(W, H, rng).astype(np.float32)
        disc(arr, W / 2, H * 0.76, W * 0.17, np.array(kleur) * 0.72, 1.0, ry=H * 0.08)   # fish lips
        disc(arr, W / 2, H * 0.76, W * 0.11, (236, 110, 130), 1.0, ry=H * 0.04)
        return np.clip(arr, 0, 255).astype(np.uint8)

    def trui(W, H, rng):
        arr = plain(kleding, 7)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0, 0.82, 1, 1, np.array(kleding) * 0.7)
        return np.clip(arr, 0, 255).astype(np.uint8)
    vin = plain(np.array(kleur) * 0.78, 7, lambda W, H, rng, arr: [rechthoek(arr, x, 0, x + 0.06, 1, np.array(kleur) * 0.6) for x in (0.2, 0.5, 0.8)])
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [cube(a, [-3.5, 4, -2.4], [7, 7, 4.8], glad), cube(a, [-3.7, 4.4, -2.6], [7.4, 5, 5.2], trui)]),
             bone("tail", "body", [0, 6, 2.4], [cube(a, [-1, 4.6, 2.4], [2, 3, 2.6], glad), cube(a, [-0.5, 2.6, 4.6], [1, 7, 3.4], vin)]),
             bone("head", "body", [0, 11, 0], [cube(a, [-4.5, 11, -3.8], [9, 8, 7.2], vel, overrides={"north": gezicht})])]
    bones += oren(a, kleur, 3.2, 17.6, -1.2, 3.2)
    ledematen(a, bones, "arm", 3.5, 5.6, -1.4, (0.9, 4.8, 2.8), vin)
    ledematen(a, bones, "leg", 0.7, 0, -1.2, (2.2, 4, 2.4), glad, onder=plain(ZWART, 4))
    for side in ("left", "right"):
        b = bij(bones, f"leg_{side}")
        x = b["cubes"][0]["origin"][0]
        b["cubes"].append(cube(a, [x - 0.2, 0, -2.2], [2.6, 1.3, 3.6], plain(ZWART, 4)))
    bones.append(bone("soort_a", "head", [0, 19, 0], [cube(a, [-0.5, 19, -2.6], [1, 2.6, 5.6], vin), cube(a, [-0.5, 21.6, -0.6], [1, 1.6, 3], vin)]))
    bones.append(bone("soort_b", "head", [0, 19, 0], [cube(a, [-0.5, 19, -3 + 2.2 * k], [1, 2.4 - 0.4 * k, 1.4], vin) for k in range(3)]))
    bones.append(bone("soort_c", "head", [0, 19, 0], [cube(a, [-3, 19, -1.4], [6, 1.2, 4.4], plain(kleding, 6)),
                                                       cube(a, [-2, 20.2, -0.6], [4, 2.2, 3], plain(kleding, 6)),
                                                       cube(a, [-3.6, 19, -3.4], [7.2, 0.5, 2.2], plain(kleding, 6))]))
    return geo("bikinibodem_guh", bones, 1.6, 1.8)


def spookkapitein(a):
    """The ghost captain of De Vliegende Guhlander: a see-through green ghost guh with a captain's hat, a beard and a wisp
    instead of legs."""
    doorzicht = 190
    geest = alpha(plain(SPOOK, 10, lambda W, H, rng, arr: [disc(arr, rng.uniform(0, W), rng.uniform(0, H), 1.6, (190, 255, 206), 0.6)
                                                           for _ in range(W * H // 80)]), doorzicht)

    def gezicht(W, H, rng):
        arr = guh_face(SPOOK, eye_y=0.38, eye_dx=0.24, eye_r=0.16, blush=False)(W, H, rng).astype(np.float32)
        for sx in (-1, 1):                                                       # bushy brows
            rechthoek(arr, 0.5 + sx * 0.24 - 0.14, 0.12, 0.5 + sx * 0.24 + 0.14, 0.19, SPOOK_DONKER)
        arr[..., 3] = 235
        return np.clip(arr, 0, 255).astype(np.uint8)
    baard = alpha(haar((200, 255, 214), (236, 255, 240)), 220)
    hoed = alpha(plain((22, 80, 56), 6), 240)
    rand = alpha(plain((240, 226, 120), 5), 240)
    jas = alpha(plain(SPOOK_DONKER, 8, lambda W, H, rng, arr: [disc(arr, W * x, H * 0.5, 1.0, (240, 226, 120)) for x in (0.35, 0.65)]), 215)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [cube(a, [-5, 12, -3], [10, 9, 6], geest), cube(a, [-5.3, 12.4, -3.3], [10.6, 7, 6.6], jas)]),
             bone("sliert_1", "body", [0, 12, 0], [cube(a, [-4, 8, -2.4], [8, 4.4, 4.8], geest)]),
             bone("sliert_2", "sliert_1", [0, 8, 0], [cube(a, [-2.8, 4.6, -1.4], [5.6, 3.8, 3.4], geest)]),
             bone("sliert_3", "sliert_2", [0, 4.6, 0], [cube(a, [-1.6, 2, 0], [3.2, 3, 2.4], geest), cube(a, [-0.8, 0.6, 1.6], [1.6, 1.8, 2], geest)]),
             bone("head", "body", [0, 21, 0], [
                 cube(a, [-5.5, 21, -4.5], [11, 8.5, 8.5], geest, overrides={"north": gezicht}),
                 cube(a, [-5.8, 20, -5], [11.6, 3.6, 2.2], baard), cube(a, [-3.4, 16.4, -4.8], [6.8, 3.8, 1.8], baard),
                 cube(a, [-1.4, 14.2, -4.6], [2.8, 2.4, 1.4], baard),
                 cube(a, [-7.5, 29.5, -5], [15, 1.2, 9.5], hoed), cube(a, [-5, 30.7, -3.6], [10, 3.4, 6.6], hoed),
                 cube(a, [-5.1, 30.7, -3.7], [10.2, 0.9, 6.8], rand), cube(a, [-1.2, 31.8, -3.9], [2.4, 2, 0.5], rand)])]
    bones += [dict(b, cubes=[dict(c) for c in b["cubes"]]) for b in oren(a, SPOOK, 4.2, 27.6, -1.5, 3.6, binnen=SPOOK_DONKER)]
    ledematen(a, bones, "arm", 5, 13.4, -1.2, (2.4, 7.4, 2.4), geest)
    return geo("spookkapitein", bones, 2.2, 2.6)


def _held(a, naam, lijf, breed, hoogte_kop, extra_kop, pak, pak_voor, been, schoen, masker=None, vacht=None):
    """An upright old superhero guh (Zeemeerman and Mosselguh share the build): returns its bones."""
    vacht = vacht or (190, 170, 206)                 # (a greyed guh fur: they are old)
    v = plain(vacht, 8)

    def gezicht(W, H, rng):
        arr = guh_face(vacht, eye_y=0.42, eye_dx=0.25, eye_r=0.15)(W, H, rng).astype(np.float32)
        for y in (0.14, 0.2):                                                    # wrinkles
            rechthoek(arr, 0.3, y, 0.7, y + 0.02, np.array(vacht) * 0.8)
        if masker:
            rechthoek(arr, 0, 0.3, 1, 0.56, masker)
            oog = guh_face(masker, eye_y=0.42, eye_dx=0.25, eye_r=0.15, snoet=False, blush=False)(W, H, rng)
            arr[int(H * 0.3):int(H * 0.56)] = oog[int(H * 0.3):int(H * 0.56)]
        return np.clip(arr, 0, 255).astype(np.uint8)
    w, h, d = lijf
    y0 = been
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, y0, 0], [cube(a, [-w / 2, y0, -d / 2], [w, h, d], pak, overrides={"north": pak_voor})]),
             bone("tail", "body", [0, y0 + 1, d / 2], [cube(a, [-0.6, y0 + 0.6, d / 2], [1.2, 1, 4], v)]),
             bone("head", "body", [0, y0 + h, 0], [
                 cube(a, [-breed / 2, y0 + h, -3.6], [breed, hoogte_kop, 7], v, overrides={"north": gezicht})] + extra_kop(y0 + h))]
    bones += oren(a, vacht, breed / 2 - 1.6, y0 + h + hoogte_kop - 1.6, -1.2, 3.2, binnen=GUH_DONKER)
    ledematen(a, bones, "arm", w / 2, y0 + 1, -1, (1.8, h - 1.4, 2), pak, onder=plain(vacht, 6))
    ledematen(a, bones, "leg", 0.6, 0, -1.1, (2, been + 0.2, 2.2), plain(vacht, 7), onder=plain(schoen, 5))
    for side in ("left", "right"):
        b = bij(bones, f"leg_{side}")
        x = b["cubes"][0]["origin"][0]
        b["cubes"].append(cube(a, [x - 0.3, 0, -2.3], [2.6, 1.2, 3.8], plain(schoen, 5)))
    return bones


def zeemeerman_guh(a):
    """Zeemeerman: an old superhero guh: an orange top with a belt and an M, a purple starfish on the nose, shell ear muffs,
    green gloves, pink slippers."""
    oranje = (246, 140, 50)

    def voor(W, H, rng):
        arr = plain(oranje, 7)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0, 0.72, 1, 0.9, (30, 30, 36))
        rechthoek(arr, 0.38, 0.68, 0.62, 0.94, (250, 220, 90))
        for x0, x1 in ((0.41, 0.45), (0.48, 0.52), (0.55, 0.59)):                 # the M on the buckle
            rechthoek(arr, x0, 0.72, x1, 0.9, (30, 30, 36))
        return np.clip(arr, 0, 255).astype(np.uint8)
    ster, schelp = plain((150, 90, 200), 6), plain((250, 236, 214), 5, lambda W, H, rng, arr: [
        rechthoek(arr, x, 0, x + 0.08, 1, (226, 196, 160)) for x in (0.2, 0.46, 0.72)])

    def kop(y):
        return [cube(a, [-0.6, y + 2.4, -4.4], [1.2, 1.2, 0.9], ster), cube(a, [-1.9, y + 2.7, -4.3], [3.8, 0.7, 0.7], ster),
                cube(a, [-0.35, y + 1.2, -4.3], [0.7, 3.6, 0.7], ster),
                cube(a, [-5.4, y + 3.6, -1.6], [1.2, 3.4, 3.2], schelp), cube(a, [4.2, y + 3.6, -1.6], [1.2, 3.4, 3.2], schelp)]
    bones = _held(a, "zeemeerman_guh", (8, 8.5, 5), 9, 8, kop, plain(oranje, 7), voor, 5, (255, 150, 190))
    for side in ("left", "right"):
        b = bij(bones, f"arm_{side}")
        x = b["cubes"][0]["origin"][0]
        b["cubes"].append(cube(a, [x - 0.2, 6, -1.2], [2.2, 2.6, 2.4], plain((90, 190, 110), 6)))
    return geo("zeemeerman_guh", bones, 1.8, 2.0)


def mosselguh(a):
    """Mosselguh: a small old guh: a white sailor cap, a red eye mask, a blue top and a blue cape."""
    blauw = (70, 130, 220)

    def voor(W, H, rng):
        arr = plain((90, 200, 220), 7)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0, 0.78, 1, 1, (30, 30, 36))
        disc(arr, W / 2, H * 0.4, W * 0.16, WIT, 1.0, ry=H * 0.2)
        return np.clip(arr, 0, 255).astype(np.uint8)
    pet = plain(WIT, 4)

    def kop(y):
        return [cube(a, [-3.2, y + 6.6, -3], [6.4, 1.6, 6], pet), cube(a, [-3.8, y + 6.4, -3.6], [7.6, 0.6, 7.2], pet),
                cube(a, [-0.5, y + 8.2, -0.5], [1, 0.7, 1], plain(blauw, 5))]
    bones = _held(a, "mosselguh", (6, 5.5, 4), 8, 6.6, kop, plain((90, 200, 220), 7), voor, 3.4, ZWART, masker=(200, 40, 46))
    bones.append(bone("cape", "body", [0, 8.6, 2], [cube(a, [-3.4, 1.6, 2.1], [6.8, 7.2, 0.5], plain(blauw, 7)),
                                                    cube(a, [-3.4, 8.2, -2.2], [6.8, 0.6, 4.6], plain(blauw, 7))]))
    return geo("mosselguh", bones, 1.6, 1.6)


def knabbelburgermobiel(a):
    """The Knabbelburgermobiel: a car shaped like a Knabbelburger: the bottom bun is the chassis on four pickle wheels, the
    patty the floor, cheese and lettuce hang over the rim, a bun bonnet and a bun boot, and the sesame top bun as a roof on
    four cocktail sticks (an olive on top). A driver and two passengers sit between the buns."""
    broodje = plain(BROODJE, 9)

    def sesam(W, H, rng):
        arr = plain(BROODJE, 9)(W, H, rng).astype(np.float32)
        for y in range(H):
            arr[y, :, :3] *= 1.06 - 0.12 * y / max(1, H - 1)
        for _ in range(max(3, W * H // 46)):
            disc(arr, rng.uniform(1, W - 1), rng.uniform(1, H - 1), 1.0, (255, 244, 206), 1.0, ry=0.6)
        return np.clip(arr, 0, 255).astype(np.uint8)
    vlees = plain((110, 62, 34), 12, lambda W, H, rng, arr: [disc(arr, rng.uniform(0, W), rng.uniform(0, H), 0.9, (70, 36, 20), 0.8)
                                                           for _ in range(W * H // 30)])
    kaas = gatenkaas(KAAS, dichtheid=200, rmin=1.0, rmax=2.0)
    sla = plain(SLA, 12, lambda W, H, rng, arr: [rechthoek(arr, x, 0, x + 0.05, 1, (80, 160, 50)) for x in np.arange(0.08, 1, 0.16)])
    wiel = rond(plain(AUGURK, 8, lambda W, H, rng, arr: (disc(arr, W / 2, H / 2, W * 0.34, (150, 200, 100)), [
        disc(arr, W / 2 + math.cos(t) * W * 0.2, H / 2 + math.sin(t) * H * 0.2, 1.0, (224, 240, 170)) for t in np.arange(0, 6.2, 1.05)])))
    groen = plain(AUGURK, 7)
    hout = plain((214, 180, 120), 5)

    def neus(W, H, rng):
        arr = plain(BROODJE, 9)(W, H, rng).astype(np.float32)
        for sx in (-1, 1):                                                       # headlights
            disc(arr, W / 2 + sx * W * 0.32, H * 0.4, W * 0.08, (255, 244, 150), 1.0, ry=H * 0.2)
        rechthoek(arr, 0.3, 0.6, 0.7, 0.9, (40, 36, 40))                          # the grille: a row of teeth, a grin
        for x in np.arange(0.32, 0.68, 0.06):
            rechthoek(arr, x, 0.64, x + 0.03, 0.86, (236, 236, 240))
        return np.clip(arr, 0, 255).astype(np.uint8)

    def bord(W, H, rng):
        arr = plain(BROODJE, 9)(W, H, rng).astype(np.float32)
        rechthoek(arr, 0.3, 0.3, 0.7, 0.7, (250, 250, 240))
        for x in (0.36, 0.44, 0.52, 0.6):
            rechthoek(arr, x, 0.4, x + 0.04, 0.6, (40, 40, 50))
        for sx in (-1, 1):
            disc(arr, W / 2 + sx * W * 0.36, H * 0.5, W * 0.05, (230, 50, 50), 1.0, ry=H * 0.2)
        return np.clip(arr, 0, 255).astype(np.uint8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [
                 cube(a, [-12, 4, -18], [24, 5, 36], broodje, overrides={"north": neus, "south": bord}),
                 cube(a, [-13, 9, -19], [26, 3, 38], vlees),
                 cube(a, [-13.6, 12, -19.6], [27.2, 0.8, 11.6], kaas), cube(a, [-13.6, 12, 9], [27.2, 0.8, 10.6], kaas),
                 cube(a, [-13.6, 9.4, -19.6], [3, 2.6, 0.8], kaas), cube(a, [8, 10.2, -19.6], [4, 1.8, 0.8], kaas),
                 cube(a, [13, 9.8, -8], [0.6, 2.2, 4], kaas), cube(a, [-13.6, 10.4, 12], [0.6, 1.6, 5], kaas),
                 cube(a, [-12.8, 12.8, -19], [25.6, 0.8, 10.4], sla), cube(a, [-12.8, 12.8, 9.4], [25.6, 0.8, 9.6], sla),
                 cube(a, [-11, 13.6, -18], [22, 5, 9], broodje, overrides={"up": sesam}),
                 cube(a, [-11, 13.6, 10], [22, 6, 8], broodje, overrides={"up": sesam}),
                 cube(a, [-10.6, 12, -8], [21.2, 1.2, 17.6], plain(ROOD, 8)),                 # the tomato seat
                 cube(a, [-10, 13.2, 7], [20, 5.6, 2.6], plain(ROOD, 8))]),
             bone("stuur", "body", [-4.5, 16, -8.6], [
                 cube(a, [-5, 14, -9.4], [1, 3.4, 1], hout), cube(a, [-6.6, 16.6, -8.6], [4.2, 4.2, 0.8], rond(plain(ZWART, 5)))],
                 rotation=[-25, 0, 0]),
             bone("dak", "body", [0, 26, 0], [
                 cube(a, [-12, 26, -13], [24, 6, 27], broodje, overrides={"up": sesam, "north": sesam, "south": sesam, "east": sesam, "west": sesam}),
                 cube(a, [-10, 32, -11], [20, 2, 23], broodje, overrides={"up": sesam}),
                 cube(a, [-12.4, 25.2, -13.4], [24.8, 0.8, 27.8], sla),
                 cube(a, [-0.4, 34, -0.4], [0.8, 4, 0.8], hout), cube(a, [-1.4, 36.6, -1.4], [2.8, 2.6, 2.8], groen),
                 cube(a, [-0.6, 37.2, -1.6], [1.2, 1.2, 0.4], plain(ROOD, 5))] +
                 [cube(a, [x, 18.6, z], [0.8, 7.4, 0.8], hout) for x in (-11, 10.2) for z in (-12, 16.6)])]
    for naam, x, z in (("wiel_vl", 11.4, -15), ("wiel_vr", -13.4, -15), ("wiel_al", 11.4, 7), ("wiel_ar", -13.4, 7)):
        bones.append(bone(naam, "body", [x + 1, 4, z + 4], [cube(a, [x, 0, z], [2, 8, 8], groen, overrides={"east": wiel, "west": wiel})]))
    return geo("knabbelburgermobiel", bones, 3.4, 3.0)


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(pairs):
    return {str(round(t, 3)): v for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": length, "bones": bones}


def golf(as_, amp, length, n=4, fase=0.0):
    """A soft back-and-forth on one axis (0 = x, 1 = y, 2 = z) as keyframes over length."""
    out = []
    for i in range(n + 1):
        v = [0, 0, 0]
        v[as_] = round(amp * math.sin(2 * math.pi * (i / n) + fase), 2)
        out.append((length * i / n, v))
    return kf(out)


WEG = [0.001, 0.001, 0.001]


def standaard(namen, loop_lengte=0.8, zweeft=False):
    """The six animations every character has, made from the bones it has (namen)."""
    heeft = lambda n: n in namen   # noqa: E731
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.02, 1.025, 1.02]), (3, [1, 1, 1])])}}
    if zweeft:
        idle["root"] = {"position": kf([(0, [0, 0, 0]), (1.5, [0, 1.6, 0]), (3, [0, 0, 0])])}
    loop = {"body": {"position": kf([(0, [0, 0, 0]), (loop_lengte / 4, [0, 0.5, 0]), (loop_lengte / 2, [0, 0, 0]),
                                     (loop_lengte * 3 / 4, [0, 0.5, 0]), (loop_lengte, [0, 0, 0])])}}
    praat = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-6, 0, 3]), (0.35, [4, 0, -3]), (0.55, [-5, 0, 2]), (0.8, [3, 0, 0]), (1.2, [0, 0, 0])])}}
    lach = {"body": {"position": kf([(t * 0.1, [0, 0.9 if t % 2 else 0, 0]) for t in range(17)]),
                     "rotation": kf([(0, [0, 0, 0]), (0.3, [-8, 0, 0]), (1.3, [-8, 0, 0]), (1.6, [0, 0, 0])])},
            "head": {"rotation": kf([(t * 0.1, [-10, 0, 5 if t % 2 else -5]) for t in range(1, 16)] + [(0, [0, 0, 0]), (1.6, [0, 0, 0])])}}
    schrik = {"body": {"position": kf([(0, [0, 0, 0]), (0.12, [0, 3, 0]), (0.3, [0, 0, 0]), (0.8, [0, 0, 0])]),
                       "rotation": kf([(0, [0, 0, 0]), (0.12, [-10, 0, 0]), (0.6, [-6, 0, 0]), (0.8, [0, 0, 0])])},
              "head": {"scale": kf([(0, [1, 1, 1]), (0.12, [1.06, 1.1, 1.06]), (0.6, [1.02, 1.04, 1.02]), (0.8, [1, 1, 1])])}}
    juich = {"body": {"position": kf([(0, [0, 0, 0]), (0.2, [0, 4, 0]), (0.4, [0, 0, 0]), (0.7, [0, 4, 0]), (0.9, [0, 0, 0]), (1.4, [0, 0, 0])])},
             "head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-12, 0, 0]), (0.9, [-12, 0, 0]), (1.4, [0, 0, 0])])}}
    for side, sx in (("left", 1), ("right", -1)):
        if heeft(f"ear_{side}"):
            idle[f"ear_{side}"] = {"rotation": kf([(0, [0, 0, 0]), (2.1 + 0.3 * sx, [0, 0, 0]), (2.2 + 0.3 * sx, [0, 0, -16 * sx]), (2.35 + 0.3 * sx, [0, 0, 0])])}
            lach[f"ear_{side}"] = {"rotation": kf([(t * 0.1, [0, 0, (-14 if t % 2 else 8) * sx]) for t in range(16)] + [(1.6, [0, 0, 0])])}
            schrik[f"ear_{side}"] = {"rotation": kf([(0, [0, 0, 0]), (0.12, [0, 0, 20 * sx]), (0.8, [0, 0, 0])])}
        if heeft(f"arm_{side}"):
            loop[f"arm_{side}"] = {"rotation": golf(0, -22 * sx, loop_lengte)}
            schrik[f"arm_{side}"] = {"rotation": kf([(0, [0, 0, 0]), (0.12, [0, 0, -120 * sx]), (0.6, [0, 0, -100 * sx]), (0.8, [0, 0, 0])])}
            juich[f"arm_{side}"] = {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, -150 * sx]), (0.45, [0, 0, -120 * sx]), (0.7, [0, 0, -150 * sx]),
                                                    (0.95, [0, 0, -120 * sx]), (1.4, [0, 0, 0])])}
            if sx < 0:
                praat[f"arm_{side}"] = {"rotation": kf([(0, [0, 0, 0]), (0.2, [-50, 0, 20]), (0.5, [-35, 0, 30]), (0.8, [-50, 0, 20]), (1.2, [0, 0, 0])])}
        if heeft(f"leg_{side}"):
            loop[f"leg_{side}"] = {"rotation": golf(0, 26 * sx, loop_lengte)}
    if heeft("tail"):
        idle["tail"] = {"rotation": golf(1, 12, 3.0)}
    return {"idle": anim(3.0, idle), "loop": anim(loop_lengte, loop), "praat": anim(1.2, praat, False), "lach": anim(1.6, lach, False),
            "schrik": anim(0.8, schrik, False), "juich": anim(1.4, juich, False)}


def vlag(tonen=(), verbergen=(), extra=None):
    """A looping pose that shows / hides bones (a synced flag, a look)."""
    b = {n: {"scale": [1, 1, 1]} for n in tonen}
    b.update({n: {"scale": WEG} for n in verbergen})
    b.update(extra or {})
    return anim(1.0, b)


def animaties(id, namen):
    A = standaard(namen, zweeft=id == "spookkapitein")
    if id == "sponsguh":
        # the contagious laugh: the whole block wobbles, arms flap
        A["lach"]["bones"]["head"]["scale"] = kf([(t * 0.1, [1.06, 0.95, 1] if t % 2 else [0.96, 1.05, 1]) for t in range(16)] + [(1.6, [1, 1, 1])])
        for side, sx in (("left", 1), ("right", -1)):
            A["lach"]["bones"][f"arm_{side}"] = {"rotation": kf([(t * 0.1, [0, 0, (-70 if t % 2 else -40) * sx]) for t in range(1, 16)]
                                                                + [(0, [0, 0, 0]), (1.6, [0, 0, 0])])}
        A["bak"] = anim(1.6, {"arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-80, 0, 0]), (0.5, [-110, 0, 0]), (0.8, [-70, 0, 0]),
                                                            (1.1, [-110, 0, 0]), (1.6, [0, 0, 0])])},
                              "head": {"rotation": kf([(0, [0, 0, 0]), (0.5, [6, 0, 0]), (1.1, [6, 0, 0]), (1.6, [0, 0, 0])])}}, False)
        A["zuig"] = anim(1.2, {"head": {"scale": kf([(0, [1, 1, 1]), (0.4, [0.9, 0.86, 0.9]), (0.8, [1.14, 1.12, 1.1]), (1.2, [1, 1, 1])]),
                                        "rotation": kf([(0, [0, 0, 0]), (0.4, [22, 0, 0]), (0.8, [0, 0, 0])])}}, False)
    if id == "patguh":
        slaap = {"root": {"rotation": [-90, 0, 0], "position": [0, 2.5, 0]},
                 "body": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.04, 1.04, 1.08]), (3, [1, 1, 1])])}}
        A["slaap"] = anim(3.0, slaap)
    if id == "meneer_krabguh":
        A["tel_geld"] = anim(1.0, {"arm_left": {"rotation": kf([(0, [-30, 30, 0]), (0.25, [-45, 40, 0]), (0.5, [-30, 30, 0]), (0.75, [-45, 40, 0]), (1, [-30, 30, 0])])},
                                   "arm_right": {"rotation": kf([(0, [-45, -40, 0]), (0.25, [-30, -30, 0]), (0.5, [-45, -40, 0]), (0.75, [-30, -30, 0]), (1, [-45, -40, 0])])},
                                   "steel_left": {"rotation": golf(2, 8, 1.0)}, "steel_right": {"rotation": golf(2, -8, 1.0)}})
        A["idle"]["bones"]["steel_left"] = {"rotation": golf(2, 5, 3.0)}
        A["idle"]["bones"]["steel_right"] = {"rotation": golf(2, 5, 3.0, fase=1.5)}
        A["loop"]["bones"]["achterpoot_left"] = {"rotation": golf(0, -26, 0.8)}
        A["loop"]["bones"]["achterpoot_right"] = {"rotation": golf(0, 26, 0.8)}
        A["vlag_uit"] = vlag(verbergen=["ijs"])
        A["vlag_aan"] = vlag(tonen=["ijs"], extra={"arm_left": {"rotation": [0, 0, -40]}, "arm_right": {"rotation": [0, 0, 40]},
                                                  "steel_left": {"rotation": [0, 0, -14]}, "steel_right": {"rotation": [0, 0, 14]}})
    if id == "parel_guh":
        A["huil"] = anim(1.0, {"head": {"rotation": kf([(0, [-14, 0, 0]), (0.25, [-18, 0, 5]), (0.5, [-14, 0, 0]), (0.75, [-18, 0, -5]), (1, [-14, 0, 0])])},
                               "arm_left": {"rotation": [-120, -20, 0]}, "arm_right": {"rotation": [-120, 20, 0]},
                               "body": {"position": kf([(0, [0, 0, 0]), (0.25, [0, 0.6, 0]), (0.5, [0, 0, 0]), (0.75, [0, 0.6, 0]), (1, [0, 0, 0])])}})
        A["idle"]["bones"]["staartje"] = {"rotation": golf(2, 8, 3.0)}
    if id == "sandy_guh":
        A["karate"] = anim(1.2, {"arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-90, 0, 60]), (0.3, [-90, 0, -10]), (0.6, [-90, 0, 60]),
                                                               (0.75, [-90, 0, -10]), (1.2, [0, 0, 0])])},
                                 "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-40, 0, -30]), (1.0, [-40, 0, -30]), (1.2, [0, 0, 0])])},
                                 "leg_left": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, 0]), (0.45, [-80, 0, 0]), (0.6, [0, 0, 0]), (1.2, [0, 0, 0])])},
                                 "body": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, -25, 0]), (0.75, [0, 25, 0]), (1.2, [0, 0, 0])])}}, False)
    if id == "gerrit":
        for n in ("idle", "loop"):
            A[n]["bones"]["steel_left"] = {"rotation": golf(2, 7, A[n]["animation_length"])}
            A[n]["bones"]["steel_right"] = {"rotation": golf(2, 7, A[n]["animation_length"], fase=1.6)}
        A["loop"] = anim(1.6, {"body": {"scale": kf([(0, [1, 1, 1]), (0.8, [1, 1, 1.12]), (1.6, [1, 1, 1])])},
                               "huisje": {"rotation": golf(0, 4, 1.6)},
                               "steel_left": {"rotation": golf(2, 7, 1.6)}, "steel_right": {"rotation": golf(2, 7, 1.6, fase=1.6)}})
        A["njeg"] = anim(0.8, {"steel_left": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, -25]), (0.5, [0, 0, -25]), (0.8, [0, 0, 0])])},
                               "steel_right": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, 25]), (0.5, [0, 0, 25]), (0.8, [0, 0, 0])])},
                               "body": {"scale": kf([(0, [1, 1, 1]), (0.2, [1.08, 1.1, 1]), (0.8, [1, 1, 1])])}}, False)
    if id == "octo_mika":
        for i in range(1, 5):
            A["idle"]["bones"][f"tentacle_{i}"] = {"rotation": golf(2 if i % 2 else 0, 4, 3.0, fase=i)}
            A["loop"]["bones"][f"tentacle_{i}"] = {"rotation": golf(0, 24 if i in (1, 4) else -24, 0.8)}
        speel = {"arm_right": {"rotation": [-74, -20, 0]}, "arm_left": {"rotation": [-66, 34, 0]}, "clarinet": {"rotation": [60, 30, 0]},
                 "head": {"rotation": kf([(0, [8, 0, 0]), (0.5, [12, 0, 4]), (1.0, [8, 0, 0]), (1.5, [12, 0, -4]), (2.0, [8, 0, 0])])},
                 "body": {"rotation": golf(2, 3, 2.0)}}
        A["speel"] = anim(2.0, speel)
        A["zucht"] = anim(1.6, {"head": {"rotation": kf([(0, [0, 0, 0]), (0.4, [-10, 0, 0]), (0.9, [22, 0, 0]), (1.3, [22, 0, 0]), (1.6, [0, 0, 0])])},
                                "body": {"scale": kf([(0, [1, 1, 1]), (0.4, [1.06, 1.04, 1.06]), (0.9, [0.96, 0.97, 0.96]), (1.6, [1, 1, 1])])},
                                "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.9, [8, 0, 0]), (1.6, [0, 0, 0])])}}, False)
        # fainted (a lasting pose: flat on his back, arms out, one tentacle still twitching)
        A["flauw"] = anim(2.0, {"root": {"rotation": [-88, 0, 0], "position": [0, 1.5, 0]},
                                "arm_left": {"rotation": [0, 0, -70]}, "arm_right": {"rotation": [0, 0, 70]},
                                "tentacle_1": {"rotation": kf([(0, [0, 0, 0]), (1.6, [0, 0, 0]), (1.75, [14, 0, 0]), (1.9, [0, 0, 0]), (2.0, [0, 0, 0])])}})
    if id == "plankmika":
        A["plan"] = anim(1.0, {"arm_left": {"rotation": kf([(0, [-70, 40, 0]), (0.25, [-70, 20, 0]), (0.5, [-70, 40, 0]), (0.75, [-70, 20, 0]), (1, [-70, 40, 0])])},
                               "arm_right": {"rotation": kf([(0, [-70, -20, 0]), (0.25, [-70, -40, 0]), (0.5, [-70, -20, 0]), (0.75, [-70, -40, 0]), (1, [-70, -20, 0])])},
                               "head": {"rotation": golf(2, 4, 1.0)}})
    if id == "juf_pufguh":
        A["vlag_uit"] = vlag(verbergen=["stekels"])
        A["vlag_aan"] = vlag(tonen=["stekels"], extra={"root": {"scale": [2, 2, 2]}, "arm_left": {"rotation": [0, 0, -70]},
                                                      "arm_right": {"rotation": [0, 0, 70]}})
        A["blaas_op"] = anim(0.8, {"head": {"scale": kf([(0, [1, 1, 1]), (0.2, [0.9, 0.9, 0.9]), (0.5, [1.25, 1.25, 1.25]), (0.8, [1, 1, 1])])},
                                   "body": {"position": kf([(0, [0, 0, 0]), (0.5, [0, 2, 0]), (0.8, [0, 0, 0])])}}, False)
    if id in ("neptuguh", "mindy_guh"):
        for n in ("idle", "loop"):
            L = A[n]["animation_length"]
            A[n]["bones"]["staart_2"] = {"rotation": golf(2, 5, L)}
            A[n]["bones"]["staart_3"] = {"rotation": golf(2, 8, L, fase=1.0)}
    if id == "neptuguh":
        A["vlag_uit"] = vlag(tonen=["kaal"], verbergen=["kroon"])
        A["vlag_aan"] = vlag(tonen=["kroon"], verbergen=["kaal"])
        A["bevries"] = anim(1.4, {"arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.4, [-150, 0, 0]), (0.7, [-70, 0, 0]), (1.1, [-70, 0, 0]), (1.4, [0, 0, 0])])},
                                  "body": {"rotation": kf([(0, [0, 0, 0]), (0.4, [-8, 0, 0]), (0.7, [10, 0, 0]), (1.4, [0, 0, 0])])}}, False)
    if id == "mindy_guh":
        A["zwaai"] = anim(1.4, {"arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, -150]), (0.45, [0, 0, -120]), (0.7, [0, 0, -150]),
                                                             (0.95, [0, 0, -120]), (1.4, [0, 0, 0])])},
                                "head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, 8]), (1.1, [0, 0, 8]), (1.4, [0, 0, 0])])}}, False)
    if id == "dennis_mika":
        A["vlag_uit"] = vlag(verbergen=["motor"])
        zit = {"root": {"position": [0, 3.4, 0]}, "leg_left": {"rotation": [-70, -24, 0]}, "leg_right": {"rotation": [-70, 24, 0]},
               "arm_left": {"rotation": [-62, 12, 0]}, "arm_right": {"rotation": [-62, -12, 0]}, "body": {"rotation": [14, 0, 0]},
               "head": {"rotation": [-12, 0, 0]}, "motor": {"position": [0, -3.4, 0]}}
        A["vlag_aan"] = vlag(tonen=["motor"], extra=zit)
        rij = dict(zit)
        rij["root"] = {"position": kf([(0, [0, 3.4, 0]), (0.1, [0, 3.8, 0]), (0.2, [0, 3.4, 0])])}
        rij["motor"] = {"position": kf([(0, [0, -3.4, 0]), (0.1, [0, -3.8, 0]), (0.2, [0, -3.4, 0])])}
        A["rij"] = anim(0.2, rij)
    if id == "cycloop_mika":
        A["grijp"] = anim(1.2, {"arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-100, 30, 0]), (0.5, [-90, -20, 0]), (0.9, [-90, -20, 0]), (1.2, [0, 0, 0])])},
                                "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-100, -30, 0]), (0.5, [-90, 20, 0]), (0.9, [-90, 20, 0]), (1.2, [0, 0, 0])])},
                                "body": {"rotation": kf([(0, [0, 0, 0]), (0.5, [16, 0, 0]), (0.9, [16, 0, 0]), (1.2, [0, 0, 0])])}}, False)
        A["loop"]["animation_length"] = 1.4
        A["loop"] = standaard(namen, loop_lengte=1.4)["loop"]
    if id == "david_hasselguh":
        L = 2.0
        zwem = {"arm_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.25, [0, 60, 0]), (L * 0.5, [0, 110, 0]), (L * 0.75, [0, 50, 0]), (L, [0, 0, 0])])},
                "arm_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.25, [0, -60, 0]), (L * 0.5, [0, -110, 0]), (L * 0.75, [0, -50, 0]), (L, [0, 0, 0])])},
                "leg_left": {"rotation": golf(0, 12, L, n=8)}, "leg_right": {"rotation": golf(0, -12, L, n=8)},
                "body": {"position": golf(1, 2.0, L)}, "head": {"rotation": golf(0, 3, L)}}
        A["loop"] = anim(L, zwem)
        A["zwem"] = anim(L, zwem)
        A["idle"] = anim(4.0, {"body": {"position": golf(1, 1.2, 4.0)}, "arm_left": {"rotation": golf(1, 8, 4.0)},
                               "arm_right": {"rotation": golf(1, -8, 4.0)}, "leg_left": {"rotation": golf(0, 5, 4.0)},
                               "leg_right": {"rotation": golf(0, -5, 4.0)}, "tail": {"rotation": golf(1, 14, 4.0)}})
    if id in ("loopgraafmonster", "bikinibodem_guh"):
        groepen = {"loopgraafmonster": (["soort_a", "soort_a_klauw_left", "soort_a_klauw_right"], ["soort_b", "soort_b_rug"], ["soort_c"]),
                   "bikinibodem_guh": (["soort_a"], ["soort_b"], ["soort_c"])}[id]
        for n in range(3 if id == "loopgraafmonster" else 6):
            A[f"soort_{n}"] = vlag(tonen=groepen[n % 3], verbergen=[b for k, g in enumerate(groepen) if k != n % 3 for b in g])
    if id == "loopgraafmonster":
        A["brul"] = anim(1.4, {"head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-24, 0, 0]), (1.0, [-20, 0, 6]), (1.4, [0, 0, 0])]),
                                        "scale": kf([(0, [1, 1, 1]), (0.3, [1.12, 1.12, 1.12]), (1.0, [1.1, 1.1, 1.1]), (1.4, [1, 1, 1])])},
                               "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, -130]), (1.0, [0, 0, -120]), (1.4, [0, 0, 0])])},
                               "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, 130]), (1.0, [0, 0, 120]), (1.4, [0, 0, 0])])}}, False)
        A["zing"] = anim(2.0, {"body": {"rotation": golf(2, 8, 2.0)}, "head": {"rotation": golf(2, -6, 2.0)},
                               "arm_left": {"rotation": kf([(0, [0, 0, -100]), (1.0, [0, 0, -140]), (2.0, [0, 0, -100])])},
                               "arm_right": {"rotation": kf([(0, [0, 0, 140]), (1.0, [0, 0, 100]), (2.0, [0, 0, 140])])},
                               "tail": {"rotation": golf(1, 20, 2.0)}})
    if id == "spookkapitein":
        for n in ("idle", "loop"):
            L = A[n]["animation_length"]
            for k in (1, 2, 3):
                A[n]["bones"][f"sliert_{k}"] = {"rotation": golf(2, 6 + 2 * k, L, fase=k * 0.9)}
        A["spook"] = anim(2.0, {"root": {"position": kf([(0, [0, 0, 0]), (0.5, [0, 5, 0]), (1.0, [0, 3, 0]), (1.5, [0, 5, 0]), (2.0, [0, 0, 0])])},
                                "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-140, 0, -30]), (1.5, [-130, 0, -40]), (2.0, [0, 0, 0])])},
                                "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-140, 0, 30]), (1.5, [-130, 0, 40]), (2.0, [0, 0, 0])])},
                                "head": {"rotation": kf([(0, [0, 0, 0]), (0.5, [14, 0, 0]), (1.0, [14, 0, 8]), (1.5, [14, 0, -8]), (2.0, [0, 0, 0])])}}, False)
    if id in ("zeemeerman_guh", "mosselguh"):
        A["held"] = anim(1.6, {"arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-160, 0, 20]), (1.2, [-160, 0, 20]), (1.6, [0, 0, 0])])},
                               "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, -40]), (1.2, [0, 0, -40]), (1.6, [0, 0, 0])])},
                               "body": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-6, 0, 0]), (1.2, [-6, 0, 0]), (1.6, [0, 0, 0])]),
                                        "position": kf([(0, [0, 0, 0]), (0.3, [0, 1, 0]), (1.2, [0, 1, 0]), (1.6, [0, 0, 0])])}}, False)
        A["idle"]["bones"]["body"]["rotation"] = [6, 0, 0]                     # (a bit bent with age)
    if id == "mosselguh":
        A["idle"]["bones"]["cape"] = {"rotation": golf(0, -5, 3.0)}
    looks.animaties(id, A, namen)                                               # sp_cast: the story animations
    return {"format_version": "1.8.0", "animations": A}


def mobiel_animaties():
    wielen = ["wiel_vl", "wiel_vr", "wiel_al", "wiel_ar"]
    idle = {"dak": {"position": kf([(0, [0, 0, 0]), (1.0, [0, 0.3, 0]), (2.0, [0, 0, 0])])}}
    rij = {w: {"rotation": kf([(0, [0, 0, 0]), (0.25, [-90, 0, 0]), (0.5, [-180, 0, 0]), (0.75, [-270, 0, 0]), (1.0, [-360, 0, 0])])} for w in wielen}
    rij["body"] = {"position": kf([(0, [0, 0, 0]), (0.25, [0, 0.5, 0]), (0.5, [0, 0, 0]), (0.75, [0, 0.5, 0]), (1.0, [0, 0, 0])])}
    rij["dak"] = {"rotation": kf([(0, [0, 0, 1.5]), (0.5, [0, 0, -1.5]), (1.0, [0, 0, 1.5])])}
    rij["stuur"] = {"rotation": kf([(0, [0, 0, 8]), (0.5, [0, 0, -8]), (1.0, [0, 0, 8])])}
    return {"format_version": "1.8.0", "animations": {"idle": anim(2.0, idle), "rij": anim(1.0, rij)}}


# =====================================================================================================================
# id: (maker, texels per model pixel, the specials the Java table Cast lists for it)
CAST = {
    "sponsguh": (sponsguh, 1.0, ["bak", "zuig"]),
    "patguh": (patguh, 1.0, ["slaap"]),
    "meneer_krabguh": (meneer_krabguh, 0.5, ["tel_geld", "vlag_aan", "vlag_uit"]),
    "parel_guh": (parel_guh, 0.5, ["huil"]),
    "sandy_guh": (sandy_guh, 0.75, ["karate"]),
    "gerrit": (gerrit, 1.0, ["njeg"]),
    "octo_mika": (octo, 1.25, ["speel", "zucht", "flauw"]),
    "plankmika": (plankmika, 2.0, ["plan"]),
    "juf_pufguh": (juf_pufguh, 0.75, ["blaas_op", "vlag_aan", "vlag_uit"]),
    "neptuguh": (neptuguh, 0.5, ["bevries", "vlag_aan", "vlag_uit"]),
    "mindy_guh": (mindy_guh, 0.75, ["zwaai"]),
    "dennis_mika": (dennis_mika, 0.5, ["rij", "vlag_aan", "vlag_uit"]),
    "cycloop_mika": (cycloop_mika, 0.5, ["grijp"]),
    "david_hasselguh": (david_hasselguh, 0.2, ["zwem"]),
    "loopgraafmonster": (loopgraafmonster, 0.5, ["brul", "zing", "soort_0", "soort_1", "soort_2"]),
    "bikinibodem_guh": (bikinibodem_guh, 0.75, [f"soort_{n}" for n in range(6)]),
    "spookkapitein": (spookkapitein, 0.75, ["spook"]),
    "zeemeerman_guh": (zeemeerman_guh, 1.0, ["held"]),
    "mosselguh": (mosselguh, 1.0, ["held"]),
}
BASIS = ["idle", "loop", "praat", "lach", "schrik", "juich"]
for _id, _namen in looks.STORY.items():            # sp_cast: the story animations (Java: Cast.eigen)
    CAST[_id][2].extend(n for n in _namen if n not in CAST[_id][2])
NODIG = {id: BASIS + extra for id, (_, _, extra) in CAST.items()}
NODIG["knabbelburgermobiel"] = ["idle", "rij"]
# the extra textures on a model of another id: texture -> (model, maker)
EXTRA_TEXTUREN = {"octoliam": ("octo_mika", lambda a: octoliam(a))}
EXTRA_TEXTUREN.update({f"loopgraafmonster_{n}": ("loopgraafmonster", (lambda a, n=n: loopgraafmonster(a, n))) for n in range(3)})
EXTRA_TEXTUREN.update({f"bikinibodem_guh_{n}": ("bikinibodem_guh", (lambda a, n=n: bikinibodem_guh(a, n))) for n in range(6)})
GLOED = ("loopgraafmonster", "loopgraafmonster_0", "loopgraafmonster_1", "loopgraafmonster_2")


def _maak(id):
    maker, k, _ = CAST[id] if id in CAST else (knabbelburgermobiel, 0.4, None)
    atlas = Vel(id, k)
    return atlas, looks.voeg_toe(id, atlas, maker(atlas))    # (sp_cast: the look bones, painted after the model's own patches)


def build(h):
    A = h.A
    for id in list(CAST) + ["knabbelburgermobiel"]:
        atlas, model = _maak(id)
        namen = {b["name"] for b in model["minecraft:geometry"][0]["bones"]}
        h.w(f"{A}/geckolib/models/entity/{id}.geo.json", model)
        h.w(f"{A}/geckolib/animations/entity/{id}.animation.json", mobiel_animaties() if id == "knabbelburgermobiel" else animaties(id, namen))
        h.save(Image.fromarray(atlas.img), "entity", f"{id}.png")
        if id in GLOED:
            h.save(Image.fromarray(atlas.gloed), "entity", f"{id}_glowmask.png")
    for tex, (model, maker) in EXTRA_TEXTUREN.items():
        atlas = Vel(model, CAST[model][1])        # (the same seed and sizes as the model: the same layout)
        looks.voeg_toe(model, atlas, maker(atlas))
        h.save(Image.fromarray(atlas.img), "entity", f"{tex}.png")
        if tex in GLOED:
            h.save(Image.fromarray(atlas.gloed), "entity", f"{tex}_glowmask.png")


def check(h):
    problems = []
    for id, wanted in NODIG.items():
        try:
            g = json.load(open(f"{h.A}/geckolib/models/entity/{id}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
            anims = json.load(open(f"{h.A}/geckolib/animations/entity/{id}.animation.json", encoding="utf-8"))["animations"]
        except OSError as e:
            problems.append(f"{id}: {e}")
            continue
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{id}: bone {b['name']} has a missing parent {b['parent']}")
            if b["name"].startswith(("outfit_", "neck", "pluis", "teckel_")):
                problems.append(f"{id}: bone name {b['name']} clashes with the guh variant bone prefixes")
        for must in ("root", "body") + (() if id == "knabbelburgermobiel" else ("head",)):
            if must not in names:
                problems.append(f"{id}: no bone {must}")
        for an in wanted:
            if an not in anims:
                problems.append(f"{id}: no animation {an}")
        if not anims:
            problems.append(f"{id}: no animations at all (GeckoLib refuses that)")
        for an, a in anims.items():
            for bn in a.get("bones", {}):
                if bn not in names:
                    problems.append(f"{id}: animation {an} moves a missing bone {bn}")
        if not os.path.exists(f"{h.A}/textures/entity/{id}.png"):
            problems.append(f"{id}: no texture")
        for lb in looks.botten(id):
            if lb not in names:
                problems.append(f"{id}: no look bone {lb}")
    for tex in EXTRA_TEXTUREN:
        if not os.path.exists(f"{h.A}/textures/entity/{tex}.png"):
            problems.append(f"missing texture {tex}.png")
    for tex in GLOED:
        if not os.path.exists(f"{h.A}/textures/entity/{tex}_glowmask.png"):
            problems.append(f"missing glowmask of {tex}")
    return problems


# what the offline renders leave out or pose: (texture, model, bones hidden, pose = {bone: rotation})
VOORBEELDEN = [("sponsguh", "sponsguh", (), None), ("patguh", "patguh", (), None), ("meneer_krabguh", "meneer_krabguh", ("ijs",), None),
               ("parel_guh", "parel_guh", (), None), ("sandy_guh", "sandy_guh", ("helm",), None), ("gerrit", "gerrit", (), None),
               ("octo_mika", "octo_mika", (), None), ("octoliam", "octo_mika", (), None), ("plankmika", "plankmika", (), None),
               ("juf_pufguh", "juf_pufguh", ("stekels",), None), ("juf_pufguh", "juf_pufguh", (), None),
               ("neptuguh", "neptuguh", ("kaal",), None), ("neptuguh", "neptuguh", ("kroon",), None), ("mindy_guh", "mindy_guh", (), None),
               ("dennis_mika", "dennis_mika", ("motor",), None), ("dennis_mika", "dennis_mika", (), None),
               ("cycloop_mika", "cycloop_mika", (), None), ("david_hasselguh", "david_hasselguh", (), None),
               ("loopgraafmonster_0", "loopgraafmonster", ("soort_b", "soort_b_rug", "soort_c"), None),
               ("loopgraafmonster_1", "loopgraafmonster", ("soort_a", "soort_a_klauw_left", "soort_a_klauw_right", "soort_c"), None),
               ("loopgraafmonster_2", "loopgraafmonster", ("soort_a", "soort_a_klauw_left", "soort_a_klauw_right", "soort_b", "soort_b_rug"), None),
               ("bikinibodem_guh_0", "bikinibodem_guh", ("soort_b", "soort_c"), None), ("bikinibodem_guh_1", "bikinibodem_guh", ("soort_a", "soort_c"), None),
               ("bikinibodem_guh_2", "bikinibodem_guh", ("soort_a", "soort_b"), None), ("bikinibodem_guh_3", "bikinibodem_guh", ("soort_b", "soort_c"), None),
               ("bikinibodem_guh_4", "bikinibodem_guh", ("soort_a", "soort_c"), None), ("bikinibodem_guh_5", "bikinibodem_guh", ("soort_a", "soort_b"), None),
               ("spookkapitein", "spookkapitein", (), None), ("zeemeerman_guh", "zeemeerman_guh", (), None), ("mosselguh", "mosselguh", (), None),
               ("knabbelburgermobiel", "knabbelburgermobiel", (), None),
               ("sponsguh", "sponsguh", (), {"arm_left": [0, 0, -150], "arm_right": [0, 0, 150]}),
               ("sandy_guh", "sandy_guh", ("helm",), {"arm_right": [-90, 0, 60], "leg_left": [-80, 0, 0]})]


def preview(out, alleen=None):
    """Offline renders (front-left, and from behind) of every model into out/ (one sheet per eight)."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo_dir = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    tiles = []
    for tex, model, hide, pose in VOORBEELDEN:
        if alleen and model not in alleen:
            continue
        pad = os.path.join(geo_dir, f"{model}.geo.json")
        if pose:
            g = json.load(open(pad, encoding="utf-8"))
            for b in g["minecraft:geometry"][0]["bones"]:
                if b["name"] in pose:
                    b["rotation"] = pose[b["name"]]
            pad = os.path.join(out, "_pose.geo.json")
            json.dump(g, open(pad, "w", encoding="utf-8"))
        q = wr.geo_quads(pad, f"guhs:entity/{tex}", hide=tuple(hide) + tuple(looks.botten(model)))
        for yaw, pitch in ((28, -14), (205, -16)):
            tiles.append(wr.render(q, yaw, pitch, 300, margin=0.06))
    for s in range(0, len(tiles), 16):
        deel = tiles[s:s + 16]
        sheet = Image.new("RGBA", (300 * 4, 300 * ((len(deel) + 3) // 4)), (236, 232, 240, 255))
        for i, t in enumerate(deel):
            sheet.alpha_composite(t, ((i % 4) * 300, (i // 4) * 300))
        sheet.save(os.path.join(out, f"sponsguh_wezens_{s // 16}.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".", sys.argv[2:] or None)
