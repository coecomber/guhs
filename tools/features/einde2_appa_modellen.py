"""
einde2 (the Guheinde expansion), slice appa - the looks of the Appa-guh and the Momo-guh: GeckoLib models, animations and
textures.

The Appa-guh is an Appa, not a guh: a big six-legged flying bison (cream shaggy fur, the brown arrow from his forehead over
his back to the tip of his broad flat tail, two brown horns, a wide soft nose, a wicker saddle basket with a blanket, a
bedroll and the saddle chest), but guh-ified: the big glossy guh eyes with blush, and two big ROUND guh ears with a lilac
inside above his horns. The Momo-guh is a small winged lemur (white fur, a long ringed tail, dark wing membranes under his
arms, green eyes) with guh eyes and ENORMOUS round guh ears.
Textures are painted face by face on an atlas (boerderij_dieren.Atlas, here with its own size: the Appa-guh is big).

  build(h)       writes geo/entity/<id>.geo.json, animations/entity/<id>.animation.json, textures/entity/<id>.png
  check(h)       every animated bone exists, the bones and animations the Java code asks for exist
  preview(out)   (python tools/features/einde2_appa_modellen.py <out>) offline renders of both (wiki_renders)
Animations: appa_guh idle, walk, vlieg, jipjip, brul; momo_guh idle, zit, vlieg, blij, jat.
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

plain, disc, cube, bone = bd.plain, bd.disc, bd.cube, bd.bone

# --- colours ------------------------------------------------------------------------------------------------------------------
VACHT = (248, 240, 224)             # Appa's cream fur
VACHT_SCHADUW = (226, 212, 190)
VACHT_BUIK = (255, 250, 240)
PIJL = (146, 100, 72)               # the arrow, the horns, the toenails
PIJL_DONKER = (112, 74, 54)
NEUS = (132, 92, 86)
OOR_BINNEN = (188, 152, 208)        # guh lilac
HOORN = (128, 90, 66)
HOORN_LICHT = (176, 134, 100)
RIET = (190, 142, 84)               # the wicker of the saddle
RIET_DONKER = (140, 98, 56)
KLEED = (232, 138, 52)              # the saddle blanket: Air Nomad orange...
KLEED_RAND = (250, 206, 92)         # ...with a yellow border
KLEED_DONKER = (186, 86, 40)
KIST = (150, 104, 70)
KIST_BESLAG = (236, 204, 110)
ROL = (120, 150, 196)

MOMO = (246, 242, 236)
MOMO_DONKER = (96, 82, 90)
MOMO_VLIES = (72, 60, 70)
MOMO_OOR = (226, 150, 188)
MOMO_BUIK = (255, 252, 246)


# =====================================================================================================================
# the atlas (boerderij_dieren's, with its own size and texel scale)
# =====================================================================================================================
class Vel(bd.Atlas):
    """A face atlas of `size` model pixels at `s` texels per pixel. boerderij_dieren's atlas fills its rows in the order the
    faces are asked for, which wastes most of a sheet when big and small faces alternate (the Appa-guh): this one is used
    twice. A first run without a plan only measures (it paints nothing); `plan()` then packs the faces sorted by height, and
    the second run puts every face on its planned spot."""

    def __init__(self, seed, size, s, plan=None):
        self.size, self.s, self.plek, self.maten = size, s, plan, []
        self.img = np.zeros((size * s, size * s, 4), np.uint8)
        self.rng = np.random.default_rng(seed)

    def plan(self):
        """Where each measured face goes: rows of faces of about the same height, the tallest first."""
        plek, x, y, rij = {}, 0, 0, 0
        for i in sorted(range(len(self.maten)), key=lambda i: (-self.maten[i][1], -self.maten[i][0], i)):
            w, h = self.maten[i]
            if x + w > self.size:
                x, y, rij = 0, y + rij, 0
            if y + h > self.size:
                raise SystemExit("einde2_appa_modellen: the texture atlas is full")
            plek[i] = (x, y)
            x += w
            rij = max(rij, h)
        return plek

    def patch(self, w, h, painter):
        maat = (int(math.ceil(w)), int(math.ceil(h)))
        self.maten.append(maat)
        if self.plek is None:
            return {"uv": [0, 0], "uv_size": [w, h]}
        u, v = self.plek[len(self.maten) - 1]
        W, H = maat[0] * self.s, maat[1] * self.s
        self.img[v * self.s:v * self.s + H, u * self.s:u * self.s + W] = painter(W, H, self.rng)
        return {"uv": [u, v], "uv_size": [w, h]}


def geo(identifier, bones, width, height, size):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": size, "texture_height": size,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


# =====================================================================================================================
# painters
# =====================================================================================================================
def ruig(colour, schaduw, var=8, dicht=26):
    """Shaggy fur: the colour with short darker downward strokes (tufts) and a few lighter ones."""
    def extra(W, H, rng, a):
        for _ in range(max(3, W * H // dicht)):
            x, y = int(rng.uniform(0, W)), int(rng.uniform(0, H))
            lang = int(rng.uniform(3, 8))
            c = np.array(schaduw if rng.random() < 0.7 else (255, 255, 250), np.float32)
            for k in range(lang):
                if 0 <= y + k < H:
                    t = 0.55 * (1 - k / lang)
                    a[y + k, x, :3] = a[y + k, x, :3] * (1 - t) + c * t
    return plain(colour, var, extra)


def franje(colour, schaduw, punt=6):
    """A fringe of fur: shaggy, its lower edge cut into see-through points."""
    basis = ruig(colour, schaduw)

    def paint(W, H, rng):
        a = basis(W, H, rng)
        x = 0
        while x < W:
            w = int(rng.uniform(punt * 0.7, punt * 1.4))
            diep = int(rng.uniform(H * 0.25, H * 0.6))
            for xx in range(x, min(W, x + w)):
                t = abs((xx - x) - w / 2) / (w / 2 + 1e-6)          # 0 in the middle of a point, 1 at its edges
                weg = int(diep * t)
                if weg:
                    a[H - weg:, xx, 3] = 0
            x += w
        return a
    return paint


def met_streep(basis, breed=0.3, kleur=PIJL, rand=PIJL_DONKER):
    """A fur patch with Appa's arrow stripe down the middle (an 'up' face: the top of the image is the front)."""
    def paint(W, H, rng):
        a = basis(W, H, rng).astype(np.float32)
        x0, x1 = int(W * (0.5 - breed / 2)), int(W * (0.5 + breed / 2))
        a[:, x0:x1, :3] = np.array(kleur) + rng.normal(0, 4, (H, x1 - x0, 1))
        a[:, x0:x0 + 2, :3] = rand
        a[:, x1 - 2:x1, :3] = rand
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def pijlpunt(breed=0.3, schacht=0.3, punt=0.34, kleur=PIJL, rand=PIJL_DONKER):
    """The arrowhead on a forehead (a 'north' face): the stripe comes down from the top edge, widens into a point that ends
    between the eyes. Used as the spots hook of the face painter (it runs before the eyes are drawn)."""
    def f(W, H, rng, a):
        cx = W / 2
        y_schacht, y_punt = H * schacht * 0.45, H * punt
        for y in range(int(y_punt) + 1):
            if y < y_schacht:
                half = W * breed / 2
            else:
                t = (y - y_schacht) / max(1.0, y_punt - y_schacht)
                half = W * breed * 0.95 * (1 - t)
            for x in range(int(cx - half), int(cx + half) + 1):
                if 0 <= x < W and 0 <= y < H:
                    a[y, x, :3] = rand if abs(x + 0.5 - cx) > half - 1.6 else kleur
    return f


def gezicht(fur, ring0=(40, 100, 200), ring1=(90, 210, 220), eye_y=0.42, eye_dx=0.26, eye_r=0.17, blush=True, spots=None,
            schaduw=None):
    """A guh face (boerderij_dieren.guh_face) with its own ring colours: the big glossy eyes (outline with lashes, dark pupil,
    a coloured ring below, two white highlights) and the pink blush under them. No snoet: the creature has its own nose."""
    basis = ruig(fur, schaduw) if schaduw else plain(fur, 8)

    def paint(W, H, rng):
        a = basis(W, H, rng).astype(np.float32)
        if spots:
            spots(W, H, rng, a)
        r = eye_r * W
        for sx in (-1, 1):
            cx, cy = W / 2 + sx * eye_dx * W, H * eye_y
            for y in range(max(0, int(cy - r * 1.2)), min(H, int(cy + r * 1.2) + 2)):
                for x in range(max(0, int(cx - r - 1)), min(W, int(cx + r) + 2)):
                    dx, dy = (x + 0.5 - cx) / r, (y + 0.5 - cy) / (r * 1.08)
                    d = math.hypot(dx, dy)
                    if d > 1:
                        continue
                    if d > 0.86 or (dy < -0.55 and d > 0.78):
                        c = (22, 16, 34)
                    elif dy > -0.05 and d > 0.5:
                        t = min(1.0, (dy + 0.05) / 0.9)
                        c = tuple(int(ring0[i] + (ring1[i] - ring0[i]) * t) for i in range(3))
                    else:
                        c = (18, 18, 34)
                    a[y, x, :3] = c
            disc(a, cx + 0.22 * r, cy - 0.32 * r, 0.3 * r, (255, 255, 255))
            disc(a, cx - 0.32 * r, cy + 0.28 * r, 0.13 * r, (226, 232, 240))
            if blush:
                disc(a, cx + sx * 0.25 * r, cy + 1.45 * r, 0.62 * r, (255, 150, 186), 0.55, ry=0.32 * r)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def rond_oor(atlas, fur, inner, x, y, z, w, h, k, rand=None):
    """The cubes of one round guh ear whose lower left corner is (x, y), w x h pixels (whole numbers), 1 thick: a wide and a
    tall cube that cross (the corners, k pixels each, stay open), so the ear is round from every side and has no square frame.
    Both cubes show their part of ONE painted ear: the fur (`rand`: the colour of its rim) with the round inside."""
    s, klaar = atlas.s, {}

    def heel(rng):
        if "a" not in klaar:
            W, H = w * s, h * s
            a = plain(rand or fur, 6)(W, H, rng).astype(np.float32)
            disc(a, W / 2, H * 0.54, W * 0.34, inner, 1.0, ry=H * 0.34)
            disc(a, W * 0.4, H * 0.4, W * 0.08, (255, 255, 255), 0.25)
            klaar["a"] = np.clip(a, 0, 255).astype(np.uint8)
        return klaar["a"]

    def breed(W, H, rng):
        return heel(rng)[k * s:k * s + H, :W]

    def hoog(W, H, rng):
        return heel(rng)[:H, k * s:k * s + W]
    rand_ = plain(rand or fur, 6)
    return [cube(atlas, [x, y + k, z], [w, h - 2 * k, 1], rand_, overrides={"north": breed, "south": breed}),
            cube(atlas, [x + k, y, z], [w - 2 * k, h, 1], rand_, overrides={"north": hoog, "south": hoog})]


def snuit(W, H, rng):
    """Appa's wide soft nose: two nostrils, a shine, and under it the happy mouth line with a little tongue."""
    a = plain(VACHT_BUIK, 5)(W, H, rng).astype(np.float32)
    disc(a, W / 2, H * 0.3, W * 0.36, NEUS, 1.0, ry=H * 0.26)
    disc(a, W / 2 - W * 0.1, H * 0.2, W * 0.07, (190, 150, 140), 0.8, ry=H * 0.07)
    for sx in (-1, 1):
        disc(a, W / 2 + sx * W * 0.15, H * 0.34, W * 0.055, (70, 44, 44), 1.0, ry=H * 0.1)
    my = int(H * 0.7)
    for x in range(int(W * 0.14), int(W * 0.86)):
        dy = int(round((1 - abs(x - W / 2) / (W * 0.36)) * H * 0.1))
        if 0 <= my + dy < H:
            a[my + dy:my + dy + 2, x, :3] = PIJL_DONKER
    disc(a, W / 2, H * 0.9, W * 0.09, (240, 130, 150), 1.0, ry=H * 0.09)
    return np.clip(a, 0, 255).astype(np.uint8)


def teennagels(W, H, rng):
    """The front of a foot: three round brown toenails on the fur."""
    a = ruig(VACHT, VACHT_SCHADUW)(W, H, rng).astype(np.float32)
    for k in range(3):
        cx = W * (0.2 + 0.3 * k)
        disc(a, cx, H * 0.74, W * 0.12, PIJL_DONKER, 1.0, ry=H * 0.26)
        disc(a, cx - W * 0.02, H * 0.68, W * 0.085, PIJL, 1.0, ry=H * 0.18)
        disc(a, cx - W * 0.04, H * 0.6, W * 0.03, HOORN_LICHT, 0.9)
    return np.clip(a, 0, 255).astype(np.uint8)


def hoorn(W, H, rng):
    a = plain(HOORN, 6)(W, H, rng).astype(np.float32)
    for y in range(0, H, 5):                                       # growth rings
        a[y:y + 1, :, :3] = HOORN_LICHT
    return np.clip(a, 0, 255).astype(np.uint8)


def riet(W, H, rng):
    """Woven wicker: a basket weave of light and dark strands."""
    a = plain(RIET, 8)(W, H, rng).astype(np.float32)
    for y in range(0, H, 4):
        for x in range((y // 4 % 2) * 4, W, 8):
            a[y:y + 3, x:x + 4, :3] = np.array(RIET_DONKER) + rng.normal(0, 4, 1)
        a[y + 3:y + 4, :, :3] *= 0.86
    return np.clip(a, 0, 255).astype(np.uint8)


def rietrand(W, H, rng):
    """The thick rolled rim of the basket."""
    a = plain(RIET_DONKER, 6)(W, H, rng).astype(np.float32)
    for x in range(0, W, 6):
        a[:, x:x + 1, :3] = np.array(RIET) * 1.05
    return np.clip(a, 0, 255).astype(np.uint8)


def kleed(W, H, rng):
    """The saddle blanket: orange, a yellow border along the lower edge, a row of little darker diamonds, tassels."""
    a = plain(KLEED, 7)(W, H, rng).astype(np.float32)
    rand = max(3, H // 7)
    a[H - 2 * rand:H - rand, :, :3] = KLEED_RAND
    a[H - 2 * rand - 2:H - 2 * rand, :, :3] = KLEED_DONKER
    for x in range(4, W, 12):
        for k in range(-3, 4):
            y = H // 2 - rand + k
            half = 3 - abs(k)
            if 0 <= y < H:
                a[y, max(0, x - half):min(W, x + half + 1), :3] = KLEED_DONKER
    for x in range(0, W, 4):                                       # tassels: the bottom edge has little gaps
        a[H - rand // 2:, x + 3:x + 4, 3] = 0
    return np.clip(a, 0, 255).astype(np.uint8)


def kleed_boven(W, H, rng):
    a = plain(KLEED, 7)(W, H, rng).astype(np.float32)
    a[:3, :, :3] = KLEED_RAND
    a[H - 3:, :, :3] = KLEED_RAND
    a[:, :3, :3] = KLEED_RAND
    a[:, W - 3:, :3] = KLEED_RAND
    return np.clip(a, 0, 255).astype(np.uint8)


def kist_voor(W, H, rng):
    """The saddle chest: planks, brass bands and a little guh-ear lock."""
    a = plain(KIST, 8)(W, H, rng).astype(np.float32)
    for y in range(0, H, 7):
        a[y:y + 1, :, :3] *= 0.75
    a[:, :3, :3] = KIST_BESLAG
    a[:, W - 3:, :3] = KIST_BESLAG
    a[int(H * 0.36):int(H * 0.36) + 2, :, :3] = np.array(KIST) * 0.6          # the lid's seam
    disc(a, W / 2, H * 0.42, W * 0.09, KIST_BESLAG, 1.0, ry=H * 0.16)
    disc(a, W / 2 - W * 0.07, H * 0.24, W * 0.04, KIST_BESLAG, 1.0)             # two tiny round ears on the lock
    disc(a, W / 2 + W * 0.07, H * 0.24, W * 0.04, KIST_BESLAG, 1.0)
    disc(a, W / 2, H * 0.44, W * 0.03, (90, 60, 40), 1.0)
    return np.clip(a, 0, 255).astype(np.uint8)


def kist_plank(W, H, rng):
    a = plain(KIST, 8)(W, H, rng).astype(np.float32)
    for y in range(0, H, 7):
        a[y:y + 1, :, :3] *= 0.75
    a[:, :2, :3] = KIST_BESLAG
    a[:, W - 2:, :3] = KIST_BESLAG
    return np.clip(a, 0, 255).astype(np.uint8)


def rol(W, H, rng):
    """A rolled-up sleeping bag: blue with two cream straps."""
    a = plain(ROL, 7)(W, H, rng).astype(np.float32)
    for f in (0.25, 0.7):
        a[:, int(W * f):int(W * f) + 3, :3] = (240, 226, 200)
    return np.clip(a, 0, 255).astype(np.uint8)


def rol_kop(W, H, rng):
    a = plain(ROL, 7)(W, H, rng).astype(np.float32)
    for r in (0.42, 0.26, 0.1):
        disc(a, W / 2, H / 2, W * r, tuple(int(v * (0.8 if r != 0.26 else 1.0)) for v in ROL), 1.0, ry=H * r)
    return np.clip(a, 0, 255).astype(np.uint8)


def ringen(W, H, rng):
    """Momo's ringed tail: white with dark bands (the face's long side runs along the tail)."""
    a = plain(MOMO, 6)(W, H, rng).astype(np.float32)
    lang = max(W, H)
    for k in range(2, lang, 9):
        if W >= H:
            a[:, k:k + 4, :3] = MOMO_DONKER
        else:
            a[k:k + 4, :, :3] = MOMO_DONKER
    return np.clip(a, 0, 255).astype(np.uint8)


def vlies(W, H, rng):
    """A wing membrane: dark, thin 'fingers', a scalloped see-through lower edge."""
    a = plain(MOMO_VLIES, 6)(W, H, rng).astype(np.float32)
    for k in range(1, 4):
        x = int(W * k / 4)
        a[:, x:x + 1, :3] = np.array(MOMO_DONKER) * 1.25
    bogen = 3
    for x in range(W):
        t = (x % (W / bogen)) / (W / bogen)
        weg = int(H * 0.28 * math.sin(t * math.pi))
        if weg:
            a[H - weg:, x, 3] = 0
    return np.clip(a, 0, 255).astype(np.uint8)


def momo_masker(W, H, rng, a):
    """The dark fur around Momo's eyes and the little pink nose with a w-mouth."""
    for sx in (-1, 1):
        disc(a, W / 2 + sx * W * 0.26, H * 0.42, W * 0.25, MOMO_DONKER, 0.35, ry=H * 0.34)
    disc(a, W / 2, H * 0.7, W * 0.055, (228, 120, 150), 1.0, ry=H * 0.05)
    y = int(H * 0.8)
    for x in range(int(W * 0.4), int(W * 0.6)):
        dy = int(round(abs(math.sin((x - W * 0.4) / (W * 0.2) * 2 * math.pi)) * H * 0.04))
        if 0 <= y + dy < H:
            a[y + dy, x, :3] = MOMO_DONKER


# =====================================================================================================================
# models
# =====================================================================================================================
APPA_ATLAS, APPA_S = 256, 4
MOMO_ATLAS, MOMO_S = 64, 8
POTEN = ["leg_front_left", "leg_front_right", "leg_mid_left", "leg_mid_right", "leg_back_left", "leg_back_right"]


def appa(atlas):
    vacht = ruig(VACHT, VACHT_SCHADUW)
    buik = plain(VACHT_BUIK, 6)
    streep = met_streep(vacht, 0.3)
    fr = franje(VACHT, VACHT_SCHADUW)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 22, 0], [
                 cube(atlas, [-20, 10, -28], [40, 26, 60], vacht, overrides={"up": streep, "down": buik}),
                 cube(atlas, [-16, 36, -24], [32, 2, 52], vacht, overrides={"up": met_streep(vacht, 0.375)}),
                 # the shaggy coat hanging down his flanks, chest and rump
                 cube(atlas, [-21, 7, -26], [1, 20, 56], fr), cube(atlas, [20, 7, -26], [1, 20, 56], fr),
                 cube(atlas, [-18, 6, 32], [36, 16, 1], fr),
                 cube(atlas, [-17, 5, -29], [34, 9, 1], fr)]),
             bone("head", "body", [0, 24, -28], [
                 cube(atlas, [-15, 11, -47], [30, 26, 19], vacht, overrides={
                     "north": gezicht(VACHT, eye_y=0.5, eye_dx=0.27, eye_r=0.15, spots=pijlpunt(0.3), schaduw=VACHT_SCHADUW),
                     "up": met_streep(vacht, 0.3), "down": buik}),
                 cube(atlas, [-12, 37, -45], [24, 2, 15], vacht, overrides={"up": met_streep(vacht, 0.375)}),
                 cube(atlas, [-16, 9, -45], [1, 12, 15], fr), cube(atlas, [15, 9, -45], [1, 12, 15], fr),
                 cube(atlas, [-8, 11.5, -49], [16, 9, 2], buik, overrides={"north": snuit})])]
    # two big round guh ears above the horns
    for side, sx in (("left", 1), ("right", -1)):
        ox = 8 if sx > 0 else -21
        bones.append(bone(f"ear_{side}", "head", [sx * 14.5, 35, -39.5], rond_oor(atlas, VACHT, OOR_BINNEN, ox, 34, -40, 13, 13, 3)))
    # the horns: out to the side, then a tip that curls up
    for side, sx in (("left", 1), ("right", -1)):
        x0 = 15 if sx > 0 else -21
        x1 = 19 if sx > 0 else -23
        bones.append(bone(f"hoorn_{side}", "head", [sx * 15, 31, -38], [
            cube(atlas, [x0, 28.5, -40.5], [6, 5, 5], hoorn),
            cube(atlas, [x1, 32.5, -40], [4, 6, 4], hoorn, overrides={"up": plain(HOORN_LICHT, 5)})]))
    # six sturdy legs with toenails
    for name, sx, z in (("leg_front_left", 1, -25), ("leg_front_right", -1, -25), ("leg_mid_left", 1, -4), ("leg_mid_right", -1, -4),
                        ("leg_back_left", 1, 18), ("leg_back_right", -1, 18)):
        x = 11 if sx > 0 else -21
        bones.append(bone(name, "body", [x + 5, 12, z + 5.5], [
            cube(atlas, [x, 0, z], [10, 13, 11], vacht, overrides={"north": teennagels, "down": plain(PIJL_DONKER, 6)}),
            cube(atlas, [x - 0.5, 9, z - 0.5], [11, 3, 12], fr)]))
    # the broad flat tail in two parts, the arrow runs to its tip
    bones += [
        bone("tail_1", "body", [0, 27, 32], [cube(atlas, [-11, 24, 31], [22, 6, 17], vacht, overrides={"up": met_streep(vacht, 0.5)})]),
        bone("tail_2", "tail_1", [0, 27, 48], [
            cube(atlas, [-15, 24.5, 48], [30, 5, 18], vacht, overrides={"up": met_streep(vacht, 0.4)}),
            cube(atlas, [-13, 25, 66], [26, 4, 2], vacht, overrides={"up": met_streep(vacht, 0.46)})])]
    # the saddle: a blanket over his back, the wicker basket on it, the saddle chest, a bedroll and a knabbel sack
    w = riet
    bones += [
        bone("zadel", "body", [0, 38, 2], [
            cube(atlas, [-21.5, 28, -21], [43, 10.4, 46], kleed, overrides={"up": kleed_boven, "down": bd.transparent}),
            cube(atlas, [-20, 38.4, -20], [40, 1.6, 44], w),
            cube(atlas, [-20, 40, -20], [40, 6, 1.6], w), cube(atlas, [-20, 40, 22.4], [40, 7, 1.6], w),
            cube(atlas, [-20, 40, -18.4], [1.6, 6, 40.8], w), cube(atlas, [18.4, 40, -18.4], [1.6, 6, 40.8], w),
            cube(atlas, [-21, 45.5, -21], [42, 1.6, 2.6], rietrand), cube(atlas, [-21, 46.5, 21.4], [42, 1.6, 2.6], rietrand),
            cube(atlas, [-21, 45.5, -18.4], [2.6, 1.6, 39.8], rietrand), cube(atlas, [18.4, 45.5, -18.4], [2.6, 1.6, 39.8], rietrand),
            cube(atlas, [-4, 47, -21.2], [8, 3.5, 3], rietrand)]),                       # the pommel the reins are tied to
        bone("kist", "zadel", [0, 40, 17], [
            cube(atlas, [-7, 40, 13], [14, 8, 8.5], kist_plank, overrides={"north": kist_voor}),
            cube(atlas, [-7.4, 46.4, 12.6], [14.8, 2, 9.3], kist_plank)]),
        bone("bagage", "zadel", [0, 40, 17], [
            cube(atlas, [9, 40, 14], [8, 4.5, 4.5], rol, overrides={"east": rol_kop, "west": rol_kop}),
            cube(atlas, [9.5, 44.5, 14.5], [7, 3.5, 3.5], rol, overrides={"east": rol_kop, "west": rol_kop}),
            cube(atlas, [-16.5, 40, 13.5], [6.5, 6, 6], plain(KLEED_RAND, 9)),
            cube(atlas, [-15, 46, 15], [3.5, 1.6, 3], plain(KLEED_DONKER, 6))])]
    return geo("appa_guh", bones, 8.5, 5.0, APPA_ATLAS)


def momo(atlas):
    vacht = plain(MOMO, 7)
    donker = plain(MOMO_DONKER, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3, 0], [
                 cube(atlas, [-2.2, 2.4, -1.6], [4.4, 4.6, 3.6], vacht, overrides={"north": plain(MOMO_BUIK, 4)})]),
             bone("head", "body", [0, 7, -0.4], [
                 cube(atlas, [-3.2, 6.6, -3.4], [6.4, 5.2, 4.8], vacht, overrides={
                     "north": gezicht(MOMO, ring0=(40, 150, 70), ring1=(170, 236, 110), eye_y=0.42, eye_dx=0.25, eye_r=0.2,
                                      spots=momo_masker)})])]
    # the enormous round ears: dark rims, a pink inside
    for side, sx in (("left", 1), ("right", -1)):
        ox = 1.6 if sx > 0 else -7.6
        bones.append(bone(f"ear_{side}", "head", [sx * 2.4, 10.6, -1.0],
                          rond_oor(atlas, MOMO, MOMO_OOR, ox, 9.6, -1.5, 6, 6, 1, rand=MOMO_DONKER), rotation=[0, 0, -16 * sx]))
    # arms with the wing membrane down to the hip
    for side, sx in (("left", 1), ("right", -1)):
        ax = 2.2 if sx > 0 else -3.2
        vx = 2.6 if sx > 0 else -7.2
        bones.append(bone(f"arm_{side}", "body", [sx * 2.4, 6.6, 0], [
            cube(atlas, [ax, 3.4, -0.6], [1.0, 3.4, 1.2], vacht, overrides={"down": plain(MOMO_OOR, 4)}),
            cube(atlas, [vx, 2.2, 0.2], [4.6, 4.6, 0.3], vlies)]))
    for side, sx in (("left", 1), ("right", -1)):
        x = 0.5 if sx > 0 else -1.9
        bones.append(bone(f"leg_{side}", "body", [x + 0.7, 2.6, 0.4], [
            cube(atlas, [x, 0, -1.2], [1.4, 2.6, 2.2], vacht, overrides={"down": plain(MOMO_OOR, 4)})]))
    bones += [bone("tail_1", "body", [0, 3.4, 2.0], [cube(atlas, [-0.7, 2.8, 1.8], [1.4, 1.4, 4.0], ringen)], rotation=[25, 0, 0]),
              bone("tail_2", "tail_1", [0, 3.5, 5.8], [cube(atlas, [-0.7, 2.8, 5.6], [1.4, 1.4, 4.0], ringen)], rotation=[30, 0, 0]),
              bone("tail_3", "tail_2", [0, 3.5, 9.6], [cube(atlas, [-0.7, 2.8, 9.4], [1.4, 1.4, 3.4], ringen,
                                                              overrides={"south": donker})], rotation=[35, 0, 0])]
    return geo("momo_guh", bones, 1.4, 1.4, MOMO_ATLAS)


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(pairs):
    return {str(round(t, 3)): v for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": length, "bones": bones}


def oren(L, hoek=14):
    return {"ear_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.8, [0, 0, 0]), (L * 0.85, [0, 0, -hoek]), (L * 0.9, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [0, 0, 0]), (L * 0.45, [0, 0, hoek]), (L * 0.5, [0, 0, 0])])}}


def appa_anims():
    L = 4.0
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (L / 2, [1.012, 1.02, 1.012]), (L, [1, 1, 1])])},
            "tail_1": {"rotation": kf([(0, [0, 0, 0]), (L / 2, [6, 0, 0]), (L, [0, 0, 0])])},
            "tail_2": {"rotation": kf([(0, [0, 0, 0]), (L / 2, [8, 0, 0]), (L, [0, 0, 0])])}}
    idle.update(oren(L))
    # walking: a slow six-legged amble, the three pairs half a step apart
    W = 1.6
    walk = {}
    for i, n in enumerate(POTEN):
        fase = ((i // 2) + (i % 2) * 1.5) % 3 / 3.0
        s = 16
        walk[n] = {"rotation": kf([(0, [s * math.sin(2 * math.pi * fase), 0, 0])] + [
            (W * k / 8, [s * math.sin(2 * math.pi * (k / 8 + fase)), 0, 0]) for k in range(1, 9)])}
    walk["body"] = {"rotation": kf([(0, [0, 0, 1.2]), (W / 2, [0, 0, -1.2]), (W, [0, 0, 1.2])]),
                    "position": kf([(0, [0, 0, 0]), (W / 4, [0, 0.6, 0]), (W / 2, [0, 0, 0]), (W * 0.75, [0, 0.6, 0]), (W, [0, 0, 0])])}
    walk["tail_1"] = {"rotation": kf([(0, [4, 4, 0]), (W / 2, [4, -4, 0]), (W, [4, 4, 0])])}
    walk["head"] = {"rotation": kf([(0, [2, 0, 0]), (W / 2, [-1, 0, 0]), (W, [2, 0, 0])])}
    # flying: he swims through the air: legs tucked and paddling a little, the big tail beating up and down
    V = 2.0
    vlieg = {}
    for i, n in enumerate(POTEN):
        voor = -28 if i < 2 else (0 if i < 4 else 30)
        fase = (i // 2) / 3.0
        vlieg[n] = {"rotation": kf([(V * k / 4, [voor + 8 * math.sin(2 * math.pi * (k / 4 + fase)), 0, 0]) for k in range(5)])}
    vlieg["tail_1"] = {"rotation": kf([(0, [-14, 0, 0]), (V / 2, [16, 0, 0]), (V, [-14, 0, 0])])}
    vlieg["tail_2"] = {"rotation": kf([(0, [-6, 0, 0]), (V * 0.15, [-18, 0, 0]), (V * 0.65, [20, 0, 0]), (V, [-6, 0, 0])])}
    vlieg["body"] = {"position": kf([(0, [0, 0, 0]), (V / 2, [0, 1.6, 0]), (V, [0, 0, 0])]),
                     "rotation": kf([(0, [-2, 0, 0]), (V / 2, [2, 0, 0]), (V, [-2, 0, 0])])}
    vlieg["head"] = {"rotation": kf([(0, [3, 0, 0]), (V / 2, [-3, 0, 0]), (V, [3, 0, 0])])}
    vlieg["ear_left"] = {"rotation": kf([(0, [0, 0, 8]), (V / 2, [0, 0, 16]), (V, [0, 0, 8])])}
    vlieg["ear_right"] = {"rotation": kf([(0, [0, 0, -8]), (V / 2, [0, 0, -16]), (V, [0, 0, -8])])}
    # jip jip: he ducks, slaps his tail on the ground and springs up
    jip = {"body": {"position": kf([(0, [0, 0, 0]), (0.3, [0, -3, 0]), (0.5, [0, -3.4, 0]), (0.75, [0, 4, 0]), (1.0, [0, 0, 0])]),
                    "rotation": kf([(0, [0, 0, 0]), (0.3, [4, 0, 0]), (0.75, [-12, 0, 0]), (1.0, [0, 0, 0])])},
           "tail_1": {"rotation": kf([(0, [0, 0, 0]), (0.3, [30, 0, 0]), (0.5, [-26, 0, 0]), (0.75, [10, 0, 0]), (1.0, [0, 0, 0])])},
           "tail_2": {"rotation": kf([(0, [0, 0, 0]), (0.3, [24, 0, 0]), (0.55, [-30, 0, 0]), (0.8, [8, 0, 0]), (1.0, [0, 0, 0])])},
           "head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [10, 0, 0]), (0.75, [-14, 0, 0]), (1.0, [0, 0, 0])])},
           "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.5, [0, 0, -22]), (1.0, [0, 0, 0])])},
           "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.5, [0, 0, 22]), (1.0, [0, 0, 0])])}}
    for i, n in enumerate(POTEN):
        jip[n] = {"rotation": kf([(0, [0, 0, 0]), (0.4, [-12 if i < 2 else 12 if i > 3 else 0, 0, 0]), (0.75, [20, 0, 0]), (1.0, [0, 0, 0])])}
    # the roar: head up, a deep breath, the whole head trembling
    brul = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-20, 0, 0]), (0.4, [-18, 0, 3]), (0.55, [-20, 0, -3]), (0.7, [-18, 0, 3]),
                                      (0.85, [-20, 0, -3]), (1.2, [0, 0, 0])]),
                     "scale": kf([(0, [1, 1, 1]), (0.25, [1.04, 1.06, 1.04]), (0.85, [1.04, 1.06, 1.04]), (1.2, [1, 1, 1])])},
            "ear_left": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, -20]), (0.85, [0, 0, -20]), (1.2, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, 20]), (0.85, [0, 0, 20]), (1.2, [0, 0, 0])])},
            "tail_1": {"rotation": kf([(0, [0, 0, 0]), (0.3, [12, 0, 0]), (1.2, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(L, idle), "walk": anim(W, walk), "vlieg": anim(V, vlieg),
        "jipjip": anim(1.0, jip, False), "brul": anim(1.2, brul, False)}}


def momo_anims():
    L = 3.0
    staart = {"tail_1": {"rotation": kf([(0, [25, 0, 0]), (L / 2, [25, 10, 0]), (L, [25, 0, 0])])},
              "tail_2": {"rotation": kf([(0, [30, 0, 0]), (L / 2, [22, 14, 0]), (L, [30, 0, 0])])},
              "tail_3": {"rotation": kf([(0, [35, 0, 0]), (L / 2, [25, 18, 0]), (L, [35, 0, 0])])}}
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (L / 2, [1.03, 1.04, 1.03]), (L, [1, 1, 1])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [0, 0, 10]), (L * 0.6, [0, 0, -8]), (L, [0, 0, 0])])}}
    idle.update(staart)
    idle.update({"ear_left": {"rotation": kf([(0, [0, 0, -16]), (L * 0.8, [0, 0, -16]), (L * 0.85, [0, 0, -36]), (L * 0.9, [0, 0, -16])])},
                 "ear_right": {"rotation": kf([(0, [0, 0, 16]), (L * 0.4, [0, 0, 16]), (L * 0.45, [0, 0, 36]), (L * 0.5, [0, 0, 16])])}})
    # sitting (on a shoulder, at home): hunched, arms folded, the tail curled around
    zit = {"body": {"rotation": [12, 0, 0], "position": [0, -0.6, 0]}, "head": {"rotation": kf([(0, [10, 0, 0]), (L / 2, [10, 0, 6]), (L, [10, 0, 0])])},
           "arm_left": {"rotation": [-30, 0, 0]}, "arm_right": {"rotation": [-30, 0, 0]},
           "leg_left": {"rotation": [-60, 0, 0]}, "leg_right": {"rotation": [-60, 0, 0]},
           "tail_1": {"rotation": kf([(0, [-6, 40, 0]), (L / 2, [-6, 46, 0]), (L, [-6, 40, 0])])},
           "tail_2": {"rotation": [0, 40, 0]}, "tail_3": {"rotation": [6, 40, 0]}}
    zit.update({k: v for k, v in idle.items() if k.startswith("ear_")})
    # flying: arms out, the membranes beating, the tail streaming behind
    V = 0.4
    vlieg = {"body": {"rotation": [50, 0, 0], "position": kf([(0, [0, 0, 0]), (V / 2, [0, 0.8, 0]), (V, [0, 0, 0])])},
             "head": {"rotation": [-44, 0, 0]},
             "arm_left": {"rotation": kf([(0, [0, 0, -100]), (V / 2, [0, 0, -40]), (V, [0, 0, -100])])},
             "arm_right": {"rotation": kf([(0, [0, 0, 100]), (V / 2, [0, 0, 40]), (V, [0, 0, 100])])},
             "leg_left": {"rotation": [40, 0, 0]}, "leg_right": {"rotation": [40, 0, 0]},
             "tail_1": {"rotation": [-40, 0, 0]}, "tail_2": {"rotation": kf([(0, [-4, 0, 0]), (V / 2, [6, 0, 0]), (V, [-4, 0, 0])])},
             "tail_3": {"rotation": [0, 0, 0]}}
    blij = {"body": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 2, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.4, 0]), (0.6, [0, 0, 0])])},
            "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, -80]), (0.3, [0, 0, -20]), (0.45, [0, 0, -80]), (0.6, [0, 0, 0])])},
            "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 80]), (0.3, [0, 0, 20]), (0.45, [0, 0, 80]), (0.6, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-10, 0, 10]), (0.6, [0, 0, 0])])}}
    # stealing a knabbel: a quick grab with both arms, then nibbling with his head down
    jat = {"body": {"rotation": kf([(0, [0, 0, 0]), (0.2, [24, 0, 0]), (0.45, [0, 0, 0]), (1.4, [0, 0, 0])])},
           "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-80, 0, 0]), (0.45, [-60, 20, 0]), (1.2, [-60, 20, 0]), (1.4, [0, 0, 0])])},
           "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-80, 0, 0]), (0.45, [-60, -20, 0]), (1.2, [-60, -20, 0]), (1.4, [0, 0, 0])])},
           "head": {"rotation": kf([(0, [0, 0, 0]), (0.45, [16, 0, 0]), (0.6, [10, 0, 0]), (0.75, [16, 0, 0]), (0.9, [10, 0, 0]),
                                     (1.05, [16, 0, 0]), (1.4, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(L, idle), "zit": anim(L, zit), "vlieg": anim(V, vlieg),
        "blij": anim(0.6, blij, False), "jat": anim(1.4, jat, False)}}


# =====================================================================================================================
DIEREN = [("appa_guh", appa, appa_anims, "einde2_appa_appa", APPA_ATLAS, APPA_S),
          ("momo_guh", momo, momo_anims, "einde2_appa_momo", MOMO_ATLAS, MOMO_S)]
NODIG = {"appa_guh": ["idle", "walk", "vlieg", "jipjip", "brul"],
         "momo_guh": ["idle", "zit", "vlieg", "blij", "jat"]}
BOTTEN = {"appa_guh": ["head", "body", "zadel", "kist", "tail_1", "tail_2", "ear_left", "ear_right", "hoorn_left", "hoorn_right"] + POTEN,
          "momo_guh": ["head", "body", "ear_left", "ear_right", "arm_left", "arm_right", "tail_1", "tail_2", "tail_3"]}


def _seed(naam):
    import features.einde2_kit as kit
    return kit.seed(naam)


def maak(naam):
    """(model, animations, texture image) of one of the two, deterministic."""
    for id, maker, anims, zaad, size, s in DIEREN:
        if id == naam:
            meet = Vel(0, size, s)
            maker(meet)
            atlas = Vel(_seed(zaad), size, s, meet.plan())
            return maker(atlas), anims(), Image.fromarray(atlas.img)
    raise KeyError(naam)


def build(h):
    for id, *_ in DIEREN:
        model, anims, tex = maak(id)
        h.w(f"{h.A}/geckolib/models/entity/{id}.geo.json", model)
        h.w(f"{h.A}/geckolib/animations/entity/{id}.animation.json", anims)
        h.save(tex, "entity", f"{id}.png")


def check(h):
    import json
    problems = []
    for name, wanted in NODIG.items():
        g = json.load(open(f"{h.A}/geckolib/models/entity/{name}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
        for b in BOTTEN[name]:
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


def _pose(model, anims, naam, t):
    """The model with the bones turned as animation `naam` has them at time t (rotations only; the nearest key at or before t)."""
    import copy
    model = copy.deepcopy(model)
    for b in model["minecraft:geometry"][0]["bones"]:
        r = anims["animations"][naam]["bones"].get(b["name"], {}).get("rotation")
        if r is None:
            continue
        if isinstance(r, dict):
            keys = sorted(r, key=float)
            r = r[max([k for k in keys if float(k) <= t] or keys[:1], key=float)]
        b["rotation"] = list(r)          # (an animation's rotation replaces the rest pose here: good enough to look at)
    return model


def preview(out, klein=False):
    """Offline renders of both into out/: the rest pose from four sides, and one frame of every animation."""
    import json
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    px = 300 if klein else 480
    for id, *_ in DIEREN:
        model, anims, tex = maak(id)
        tex.save(os.path.join(out, f"{id}_atlas.png"))
        arr = np.asarray(tex).astype(np.float32)
        tiles = []

        def shot(m, yaw, pitch):
            geo_path = os.path.join(out, f"{id}.geo.json")
            json.dump(m, open(geo_path, "w"))
            tiles.append(wr.render(wr.geo_quads(geo_path, arr), yaw, pitch, px, margin=0.06))
        for yaw, pitch in ((205, -16), (150, -10), (25, -20), (200, -55)):
            shot(model, yaw, pitch)
        for naam, a in anims["animations"].items():
            shot(_pose(model, anims, naam, a["animation_length"] * 0.5), 215, -14)
        sheet = Image.new("RGBA", (px * 4, px * ((len(tiles) + 3) // 4)), (236, 232, 240, 255))
        for i, t in enumerate(tiles):
            sheet.alpha_composite(t, ((i % 4) * px, (i // 4) * px))
        sheet.save(os.path.join(out, f"{id}_sheet.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".", klein="klein" in sys.argv)
