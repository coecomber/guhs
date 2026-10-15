"""
einde2, slice geestmodellen - the creatures of the Geestenwereld (DESIGN_EINDE2 5): entity types of their own on the Java
class nl.juiced.guhs.feature.einde2.geestwezens.GeestWezen (table: GeestWezens), with python-written GeckoLib models.

  koh             Koh de Gezichtenrover: a centipede of about 12 blocks, the front reared up, many legs, a head with the face
                  he wears now: six textures koh.png, koh_1.png .. koh_5.png (a pale mask, a guh, a Mika, laughing, sad, blank)
  hei_bai_guh     two forms in one model (the renderer shows one by Soort): bone `klein` = the little panda spirit guh with its
                  white face, bone `groot` = the angry giant of about 5 blocks, black, six arms, a white mask face
  fang            Roku-guh's spirit dragon: a long red see-through dragon with the guh's face and ears, a mane, a seat on his
                  neck (bone `zadel`, Java FangEntity.ZADEL)
  geest_koi       Tui (white, black spot, geest_koi.png) and La (black, white spot, geest_koi_1.png): koi of about 6 blocks
                  with long veil fins, see-through and glowing
  libelkonijnguh  a tiny bunny with the guh face, long ears and four dragonfly wings
  lantaarngeest   a paper lantern with a painted face; four colours (lantaarngeest.png, _1 .. _3); its eyelid `ooglid` winks
  schaduwgeest    a smoky dark tall shape with two white eyes and long arms

WEZENS: id -> (hitbox width, height, looks, a texture per look, see-through, glow, [(animation besides idle, ticks; 0 = a pose
or loop that lasts)]) - the same table as the Java enum GeestWezens (java_rij gives a row's arguments; check() compares).
Every file holds idle, praat and every name of its row. Every texture of a creature with glow has <texture>_glowmask.png:
the glowing pixels in their colour, everything else black and clear (the mask is ADDED on top, GuhRenderTypes.eyes).
"""
import math
import os

import numpy as np
from PIL import Image

from features import boerderij_dieren as bd
from features import einde2_kit as kit

S = bd.S
plain, guh_face, cube, bone, ears, disc = bd.plain, bd.guh_face, bd.cube, bd.bone, bd.ears, bd.disc
WEG = [0.001, 0.001, 0.001]

WEZENS = {
    "koh": (2.6, 3.6, 6, True, False, True, [("praat", 24), ("kruip", 0), ("wissel", 20), ("steel", 60)]),
    "hei_bai_guh": (0.8, 1.0, 2, False, False, True, [("praat", 24), ("groei", 40), ("kalm", 40)]),
    "fang": (3.0, 2.4, 0, False, True, True, [("praat", 24), ("vlieg", 0), ("buig", 40)]),
    "geest_koi": (2.0, 1.6, 2, True, True, True, [("praat", 24), ("zwem", 0)]),
    "libelkonijnguh": (0.4, 0.4, 0, False, True, True, [("praat", 16), ("vlieg", 0), ("schiet_weg", 16)]),
    "lantaarngeest": (0.7, 0.9, 4, True, False, True, [("praat", 24), ("knipoog", 20)]),
    "schaduwgeest": (0.8, 2.6, 0, False, True, True, [("praat", 24), ("duw", 16)]),
}
# the seat on Fang's neck in model pixels: the top of the saddle cloth (Java: FangEntity.ZADEL = (0, 34/16, 40/16), +z forwards there)
FANG_ZADEL = (0, 34, -40)
# Koh's faces by Soort (Java: KohEntity.MASKER .. BLANCO)
KOH_GEZICHTEN = ("masker", "guh", "mika", "lach", "verdriet", "blanco")


def java_rij(id):
    b, h, soorten, per_soort, door, gloed, anims = WEZENS[id]
    return (f"{b}f, {h}f, {soorten}, {str(per_soort).lower()}, {str(door).lower()}, {str(gloed).lower()}, "
            + ", ".join(f'"{n}:{t}"' for n, t in anims))


def texturen(id):
    soorten, per_soort = WEZENS[id][2], WEZENS[id][3]
    return [id] + ([f"{id}_{n}" for n in range(1, soorten)] if per_soort else [])


# =====================================================================================================================
# the atlas: boerderij_dieren's, with a texel density, the slice's own named seed, and a glow mask
# =====================================================================================================================
class Vel(bd.Atlas):
    def __init__(self, naam, k=1.0):
        super().__init__(0)
        self.rng = kit.rng(f"geestmodellen/{naam}")
        self.k = k
        self.gloed = np.zeros(self.img.shape[:2], bool)
        self.dim = np.ones(self.img.shape[:2], np.float32)

    def alloc(self, w, h):
        try:
            return super().alloc(w, h)
        except SystemExit:
            raise SystemExit("einde2_geestmodellen_wezens: the texture atlas is full (lower this model's k)")

    def patch(self, w, h, painter):
        k = getattr(painter, "k", self.k)
        w2, h2 = max(w * k, 0.25), max(h * k, 0.25)
        uv = super().patch(w2, h2, painter)
        fn = getattr(painter, "gloed", None)
        if fn is not None:
            u, v = uv["uv"]
            W, H = int(math.ceil(w2)) * S, int(math.ceil(h2)) * S
            blok = self.img[v * S:v * S + H, u * S:u * S + W]
            m = fn(blok) & (blok[..., 3] > 0)
            self.gloed[v * S:v * S + H, u * S:u * S + W] |= m
            self.dim[v * S:v * S + H, u * S:u * S + W] = getattr(painter, "dim", 1.0)
        return uv

    def glowmask(self):
        m = np.zeros_like(self.img)
        m[self.gloed, :3] = np.clip(self.img[self.gloed, :3].astype(np.float32) * self.dim[self.gloed, None], 0, 255).astype(np.uint8)
        m[self.gloed, 3] = 255
        return m


def gloei(painter, fn=None, dim=1.0):
    """The same painter, its pixels (or those fn(rgba block) picks) glowing at this brightness."""
    def paint(W, H, rng):
        return painter(W, H, rng)
    paint.gloed = fn or (lambda a: np.ones(a.shape[:2], bool))
    paint.dim = dim
    for attr in ("k",):
        if hasattr(painter, attr):
            setattr(paint, attr, getattr(painter, attr))
    return paint


def scherp(painter, k):
    """The same painter at its own texel density (a face needs more pixels than a leg)."""
    def paint(W, H, rng):
        return painter(W, H, rng)
    paint.k = k
    for attr in ("gloed", "dim"):
        if hasattr(painter, attr):
            setattr(paint, attr, getattr(painter, attr))
    return paint


def met(painter, extra):
    def paint(W, H, rng):
        a = painter(W, H, rng).astype(np.float32)
        extra(W, H, rng, a)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def alfa(painter, a_):
    """See-through: every pixel gets at most this alpha."""
    def paint(W, H, rng):
        arr = painter(W, H, rng).copy()
        arr[..., 3] = np.minimum(arr[..., 3], a_)
        return arr
    return paint


def vacht(colour, licht, n=24, var=7):
    """Shaggy fur: soft hanging locks."""
    def extra(W, H, rng, a):
        for _ in range(max(2, W * H // n)):
            x, y, lang = int(rng.uniform(0, W)), int(rng.uniform(0, H)), int(rng.uniform(3, 9))
            a[y:y + lang, x:x + 1, :3] = licht if rng.random() < 0.5 else np.array(colour, np.float32) * 0.86
    return plain(colour, var, extra)


def ellips(W, H, cx, cy, rx, ry):
    yy, xx = np.mgrid[0:H, 0:W]
    return ((xx + 0.5 - cx) / rx) ** 2 + ((yy + 0.5 - cy) / ry) ** 2 <= 1


def boog(a, cx, cy, rx, ry, dik, rgb, onder=True):
    """A painted arc (half an ellipse ring): a closed eye or a smile (onder: the lower half)."""
    H, W = a.shape[:2]
    yy, xx = np.mgrid[0:H, 0:W]
    d = np.sqrt(((xx + 0.5 - cx) / rx) ** 2 + ((yy + 0.5 - cy) / ry) ** 2)
    m = (np.abs(d - 1) * min(rx, ry) <= dik / 2) & ((yy + 0.5 >= cy) if onder else (yy + 0.5 <= cy))
    a[m, :3] = rgb
    a[m, 3] = 255


def gezicht_van(bestand, rect, achter, neus=None):
    """A face taken from a base texture (guh or Mika: the north face of its head), stretched over the patch."""
    bron = Image.open(bestand).convert("RGBA").crop(rect)

    def paint(W, H, rng):
        a = plain(achter, 6)(W, H, rng).astype(np.float32)
        hoog = int(round(W * bron.height / bron.width))
        stuk = np.asarray(bron.resize((W, hoog), Image.NEAREST)).astype(np.float32)
        y0 = int((H - hoog) * 0.42)
        vak = a[y0:y0 + hoog]
        vak[stuk[..., 3] > 0] = stuk[stuk[..., 3] > 0][:, :4]
        if neus:
            ny = y0 + int(hoog * 0.66)
            disc(a, W / 2, ny, W * 0.06, neus, ry=H * 0.035)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


# =====================================================================================================================
# animation helpers
# =====================================================================================================================
def kf(pairs):
    return {str(round(t, 3)): [round(x, 3) for x in v] for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": round(length, 3), "bones": bones}


def golf(as_, amp, length, n=4, fase=0.0, om=(0, 0, 0), keer=1):
    """A sine on one axis over `length` seconds (`keer` periods), keys at n per period."""
    out = []
    for i in range(n * keer + 1):
        v = list(om)
        v[as_] += amp * math.sin(2 * math.pi * keer * (i / (n * keer)) + fase)
        out.append((length * i / (n * keer), v))
    return kf(out)


def oor_idle(L, hoek=16):
    return {"ear_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.62, [0, 0, 0]), (L * 0.64, [0, 0, -hoek]), (L * 0.67, [0, 0, 0]), (L, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [0, 0, 0]), (L * 0.32, [0, 0, hoek]), (L * 0.35, [0, 0, 0]), (L, [0, 0, 0])])}}


def knik(L=1.2, amp=8):
    return {"rotation": kf([(0, [0, 0, 0]), (L * 0.17, [-amp, 0, amp / 2]), (L * 0.42, [amp * 0.6, 0, -amp / 2]), (L * 0.67, [-amp * 0.7, 0, amp / 3]),
                            (L, [0, 0, 0])])}


# =====================================================================================================================
# koh: Koh de Gezichtenrover
# =====================================================================================================================
KOH_N, KOH_Z0 = 12, -56                # segments (each 16 px long), the front of segment 0 when he lies flat
KOH_OP = (-18, -24, -24, -14)          # the reared front: the turn of segment 3, 2, 1, 0 (negative x lifts what points forward)
KOH_KOP = 84                           # the head turned back so that the face looks ahead (a little down)
KOH_PLAAT, KOH_RAND, KOH_BUIK = (44, 38, 52), (92, 74, 104), (176, 128, 70)
KOH_POOT, KOH_TEEN = (122, 40, 44), (226, 196, 170)
KOH_HUID = (238, 230, 218)             # the pale face
KOH_STIP = (160, 226, 255)


def _koh_seg_z(i):
    return KOH_Z0 + 16 * i


def koh_gezicht(soort):
    """The face patch of a look (painted on the north face of the face cube)."""
    tex = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "entity")
    if soort == 1:
        return gezicht_van(os.path.join(tex, "guh_sitting.png"), (440, 128, 496, 172), (252, 206, 220), neus=(214, 96, 150))
    if soort == 2:
        return gezicht_van(os.path.join(tex, "mika.png"), (44, 196, 100, 240), (214, 168, 180))

    def paint(W, H, rng):
        a = plain(KOH_HUID, 5)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        rand = 1 - np.clip(np.hypot((xx - W / 2) / (W / 2), (yy - H / 2) / (H / 2)) - 0.7, 0, 1) * 0.35
        a[..., :3] *= rand[..., None]                                   # (a soft shadow towards the rim)
        donker, rood = (40, 30, 40), (196, 52, 64)
        if soort == 0:                                                  # the noh-like mask: high smudged brows, narrow eyes, small red lips
            for sx in (-1, 1):
                disc(a, W / 2 + sx * W * 0.2, H * 0.2, W * 0.07, (70, 60, 70), 0.85, ry=H * 0.035)
                m = ellips(W, H, W / 2 + sx * W * 0.2, H * 0.44, W * 0.11, H * 0.025)
                a[m, :3] = donker
                disc(a, W / 2 + sx * W * 0.2 + W * 0.03, H * 0.44, W * 0.012, (250, 250, 250))
                disc(a, W / 2 + sx * W * 0.24, H * 0.56, W * 0.07, (240, 170, 180), 0.35, ry=H * 0.03)
            disc(a, W / 2, H * 0.74, W * 0.07, rood, ry=H * 0.03)
            disc(a, W / 2 - W * 0.035, H * 0.725, W * 0.035, rood, ry=H * 0.022)
            disc(a, W / 2 + W * 0.035, H * 0.725, W * 0.035, rood, ry=H * 0.022)
        elif soort == 3:                                                # laughing: closed happy eyes, a wide open mouth
            for sx in (-1, 1):
                boog(a, W / 2 + sx * W * 0.21, H * 0.46, W * 0.1, H * 0.06, max(2, W * 0.025), donker, onder=False)
                disc(a, W / 2 + sx * W * 0.27, H * 0.58, W * 0.08, (246, 150, 170), 0.6, ry=H * 0.04)
            m = ellips(W, H, W / 2, H * 0.66, W * 0.2, H * 0.14) & (yy + 0.5 >= H * 0.62)
            a[m, :3] = (110, 30, 40)
            disc(a, W / 2, H * 0.76, W * 0.09, (230, 110, 120), ry=H * 0.035)
            a[(yy >= int(H * 0.62)) & (yy < int(H * 0.645)) & (np.abs(xx - W / 2) < W * 0.2), :3] = donker
        elif soort == 4:                                                # sad: drooping eyes with tears, a turned-down mouth
            for sx in (-1, 1):
                boog(a, W / 2 + sx * W * 0.21, H * 0.48, W * 0.09, H * 0.05, max(2, W * 0.025), donker, onder=True)
                m = (np.abs(yy - (H * 0.3 - sx * (xx - W / 2 - sx * W * 0.2) * 0.25)) < H * 0.012) & (np.abs(xx - W / 2 - sx * W * 0.2) < W * 0.09)
                a[m, :3] = (80, 66, 78)
                for k in range(3):
                    disc(a, W / 2 + sx * W * 0.27, H * (0.58 + 0.06 * k), W * 0.022, (120, 190, 240), ry=H * 0.025)
            boog(a, W / 2, H * 0.82, W * 0.12, H * 0.06, max(2, W * 0.022), donker, onder=False)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def koh_plaat():
    def extra(W, H, rng, a):
        r = max(1, W // 12)
        a[:r, :, :3] = KOH_RAND
        a[-r:, :, :3] = np.array(KOH_PLAAT) * 0.7
    return plain(KOH_PLAAT, 7, extra)


def koh_rug():
    """The ridge on his back: a darker plate with two pale glowing spots."""
    def extra(W, H, rng, a):
        for f in (0.3, 0.7):
            disc(a, W / 2, H * f, max(1.2, W * 0.22), KOH_STIP)
    return gloei(plain((60, 50, 72), 6, extra), lambda b: (b[..., 2] > 200) & (b[..., 0] < 200), 0.8)


def koh(a, soort=0):
    plaat, buik, rug = koh_plaat(), plain(KOH_BUIK, 8), koh_rug()
    poot, teen = plain(KOH_POOT, 6), plain(KOH_TEEN, 5)
    kap = met(plain((34, 30, 40), 6), lambda W, H, rng, ar: [disc(ar, x, y, 1.3, KOH_RAND) for x in range(3, W, 7) for y in range(3, H, 7)])
    bones = [bone("root", None, [0, 0, 0], []), bone("body", "root", [0, 0, 0], [])]
    for i in range(KOH_N):
        zs = _koh_seg_z(i)
        ouder = {3: "body", 2: "seg_3", 1: "seg_2", 0: "seg_1"}.get(i, "body")
        draai = {3: KOH_OP[0], 2: KOH_OP[1], 1: KOH_OP[2], 0: KOH_OP[3]}.get(i)
        smal = 1.0 if i < 9 else 1.0 - (i - 8) * 0.12                   # (the last few taper off)
        w = 22 * smal
        cubes = [cube(a, [-w / 2, 3, zs + 1], [w, 12, 15], plaat, overrides={"down": buik}),
                 cube(a, [-w / 2 + 2, 1.5, zs + 2], [w - 4, 2, 13], buik),
                 cube(a, [-3, 15, zs + 3], [6, 2, 10], rug)]
        if i == KOH_N - 1:
            cubes.append(cube(a, [-3, 6, zs + 16], [6, 5, 9], plaat))                     # the tail spike
            cubes.append(cube(a, [-1.5, 7, zs + 25], [3, 3, 6], plain(KOH_TEEN, 5)))
        bones.append(bone(f"seg_{i}", ouder, [0, 8.5, zs + 16], cubes, rotation=[draai, 0, 0] if draai else None))
        for kant, sx in (("left", 1), ("right", -1)):
            x0 = w / 2
            bones.append(bone(f"poot_{i}_{kant}", f"seg_{i}", [sx * x0, 7, zs + 8], [
                cube(a, [x0 if sx > 0 else -x0 - 9, 6.4, zs + 7.2], [9, 1.6, 1.6], poot),
                cube(a, [x0 + 7.4 if sx > 0 else -x0 - 9, -1, zs + 7.2], [1.6, 8, 1.6], poot, overrides={"down": teen}),
                cube(a, [x0 + 7.4 if sx > 0 else -x0 - 9, -1.6, zs + 7.2], [1.6, 1.4, 1.6], teen)],
                rotation=[0, sx * (12 - i * 2), 0]))
    # the head on the front of segment 0 (in the frame of a flat body: forwards is -z)
    hz = KOH_Z0
    gezicht = scherp(koh_gezicht(soort), 1.5)
    bones.append(bone("head", "seg_0", [0, 8.5, hz], [
        cube(a, [-14, -4, hz - 6], [28, 27, 6], kap),                               # the dark hood round the face
        cube(a, [-11, -1.5, hz - 13], [22, 22, 8], plain(KOH_HUID, 5), overrides={"north": gezicht}),
        cube(a, [-12, 20, hz - 10], [24, 3, 6], kap)], rotation=[KOH_KOP, 0, 0]))
    for kant, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"kaak_{kant}", "head", [sx * 6, -1.5, hz - 9], [
            cube(a, [sx * 6 - 1.5, -5, hz - 12], [3, 4, 3], teen), cube(a, [sx * 6 - (2.5 if sx > 0 else -0.5), -6.5, hz - 13.5], [2, 2, 2.5], teen)]))
        bones.append(bone(f"spriet_{kant}", "head", [sx * 6, 21, hz - 8], [
            cube(a, [sx * 6 - 0.6, 21, hz - 8.6], [1.2, 20, 1.2], poot), cube(a, [sx * 6 - 0.8, 40, hz - 8.8], [1.6, 2, 1.6], teen)],
            rotation=[-35, 0, -18 * sx]))
    return geo_("koh", bones, 26, 9, [0, 2, 0])


def koh_animaties():
    L = 12.0
    segs = [f"seg_{i}" for i in range(KOH_N)]
    poten = [(i, k) for i in range(KOH_N) for k in ("left", "right")]

    def poot_rimpel(periode, amp, keer):
        return {f"poot_{i}_{k}": {"rotation": golf(1, amp * (1 if k == "left" else -1), periode * keer, n=4, fase=i * 0.7, keer=keer)}
                for i, k in poten}
    idle = {"seg_3": {"rotation": golf(1, 5, L, keer=1)}, "seg_2": {"rotation": golf(2, 3, L, fase=1.0, keer=2)},
            "seg_1": {"rotation": golf(0, 3, L, keer=2)}, "head": {"rotation": golf(1, 8, L, fase=0.5, keer=1)},
            "spriet_left": {"rotation": golf(0, 8, L, keer=4)}, "spriet_right": {"rotation": golf(0, 8, L, fase=1.2, keer=4)},
            "kaak_left": {"rotation": golf(1, 6, L, keer=6)}, "kaak_right": {"rotation": golf(1, -6, L, keer=6)}}
    for i in range(4, KOH_N):
        idle[f"seg_{i}"] = {"position": golf(0, 0.8, L, fase=i * 0.6, keer=2)}
    idle.update(poot_rimpel(3.0, 6, 4))
    praat = {"head": knik(1.2, 8), "kaak_left": {"rotation": golf(1, 14, 1.2, keer=3)}, "kaak_right": {"rotation": golf(1, -14, 1.2, keer=3)},
             "spriet_left": {"rotation": golf(0, 14, 1.2, keer=2)}, "spriet_right": {"rotation": golf(0, -14, 1.2, keer=2)}}
    plat = {"seg_3": -KOH_OP[0], "seg_2": -KOH_OP[1], "seg_1": -KOH_OP[2], "seg_0": -KOH_OP[3]}
    kruip = {s: {"rotation": golf(1, 4, 2.0, fase=j * 0.8, om=(r, 0, 0))} for j, (s, r) in enumerate(plat.items())}
    kruip["head"] = {"rotation": [-KOH_KOP + 4, 0, 0]}
    for i in range(4, KOH_N):
        kruip[f"seg_{i}"] = {"position": golf(0, 2.2, 2.0, fase=i * 0.7)}
    kruip.update(poot_rimpel(0.5, 16, 4))
    wissel = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, -40, 8]), (0.3, [0, 70, -10]), (0.45, [0, 110, 0]), (0.6, [0, 30, 0]),
                                         (0.8, [0, -10, 0]), (1.0, [0, 0, 0])]),
                       "scale": kf([(0, [1, 1, 1]), (0.4, [1.12, 0.75, 1]), (0.5, [0.9, 1.15, 1]), (0.7, [1, 1, 1]), (1.0, [1, 1, 1])])}}
    vast = 2.2
    steel = {"seg_3": {"rotation": kf([(0, [0, 0, 0]), (0.25, [12, 0, 0]), (vast, [12, 0, 0]), (3.0, [0, 0, 0])])},
             "seg_2": {"rotation": kf([(0, [0, 0, 0]), (0.25, [16, 0, 0]), (vast, [16, 0, 0]), (3.0, [0, 0, 0])])},
             "seg_1": {"rotation": kf([(0, [0, 0, 0]), (0.25, [16, 0, 0]), (vast, [16, 0, 0]), (3.0, [0, 0, 0])])},
             "seg_0": {"rotation": kf([(0, [0, 0, 0]), (0.25, [10, 0, 0]), (vast, [10, 0, 0]), (3.0, [0, 0, 0])])},
             "head": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-50, 0, 0]), (vast, [-50, 0, 0]), (3.0, [0, 0, 0])])},
             "root": {"position": kf([(0, [0, 0, 0]), (0.25, [0, 0, -10]), (vast, [0, 0, -10]), (3.0, [0, 0, 0])])},
             "spriet_left": {"rotation": kf([(0, [0, 0, 0]), (0.25, [30, 0, -10]), (vast, [30, 0, -10]), (3.0, [0, 0, 0])])},
             "spriet_right": {"rotation": kf([(0, [0, 0, 0]), (0.25, [30, 0, 10]), (vast, [30, 0, 10]), (3.0, [0, 0, 0])])}}
    for i, k in poten:                                                  # (frozen: every leg holds still)
        steel[f"poot_{i}_{k}"] = {"rotation": kf([(0, [0, 0, 0]), (0.25, [0, 0, 8 if k == "left" else -8]), (vast, [0, 0, 8 if k == "left" else -8]),
                                                   (3.0, [0, 0, 0])])}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "kruip": anim(2.0, kruip), "wissel": anim(1.0, wissel, False),
            "steel": anim(3.0, steel, False)}


# =====================================================================================================================
# hei_bai_guh: the little panda spirit and the angry giant
# =====================================================================================================================
ZWART, PANDA_WIT = (34, 32, 38), (246, 244, 240)


def panda_gezicht(W, H, rng):
    """The panda guh face: white, black tear-shaped patches round the guh's eyes, a little black nose."""
    a = plain(PANDA_WIT, 6)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):                                                  # the black patches first, the guh's eyes in them
        disc(a, W / 2 + sx * W * 0.25, H * 0.5, W * 0.21, ZWART, ry=H * 0.27)
        disc(a, W / 2 + sx * W * 0.27, H * 0.66, W * 0.13, ZWART, ry=H * 0.14)
    oog = guh_face(PANDA_WIT, eye_y=0.46, eye_dx=0.24, eye_r=0.13, snoet=False, blush=False)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):
        m = ellips(W, H, W / 2 + sx * W * 0.24, H * 0.46, W * 0.135, H * 0.145)
        a[m] = oog[m]
    return np.clip(a, 0, 255).astype(np.uint8)


def panda_snoet(W, H, rng):
    a = plain(PANDA_WIT, 5)(W, H, rng).astype(np.float32)
    disc(a, W / 2, H * 0.35, W * 0.22, ZWART, ry=H * 0.25)
    boog(a, W / 2, H * 0.62, W * 0.16, H * 0.18, max(1.5, H * 0.1), (60, 50, 60), onder=True)
    return np.clip(a, 0, 255).astype(np.uint8)


def masker_gezicht(W, H, rng):
    """The giant's white mask: an oval face with dark slanted eye holes (a glowing pupil in each) and a wide dark mouth."""
    a = vacht(ZWART, (60, 56, 66), 18)(W, H, rng).astype(np.float32)
    m = ellips(W, H, W / 2, H * 0.52, W * 0.46, H * 0.47)
    wit = plain((236, 234, 228), 5)(W, H, rng).astype(np.float32)
    a[m] = wit[m]
    yy, xx = np.mgrid[0:H, 0:W]
    for sx in (-1, 1):
        cx, cy = W / 2 + sx * W * 0.2, H * 0.42
        oog = ellips(W, H, cx, cy, W * 0.13, H * 0.09) & (yy - cy > (xx - cx) * sx * 0.35 - H * 0.06)
        a[oog, :3] = (20, 18, 24)
        disc(a, cx + sx * W * 0.02, cy + H * 0.01, W * 0.03, (200, 240, 255))
    mond = ellips(W, H, W / 2, H * 0.74, W * 0.26, H * 0.07)
    a[mond, :3] = (26, 22, 30)
    return np.clip(a, 0, 255).astype(np.uint8)


def hei_bai_guh(a):
    wit, zwart = plain(PANDA_WIT, 6), vacht(ZWART, (58, 54, 64), 22)
    rook = alfa(vacht((50, 46, 58), (80, 76, 90), 14), 210)
    klauw = plain((220, 214, 200), 5)
    oog_gloed = lambda b: (b[..., 2] > 230) & (b[..., 0] > 180) & (b[..., 0] < 225)
    bones = [bone("root", None, [0, 0, 0], []),
             # --- the little one ---
             bone("klein", "root", [0, 0, 0], []),
             bone("body", "klein", [0, 6, 0], [cube(a, [-5, 3, -5], [10, 9, 12], wit),
                                                cube(a, [-5.3, 2.8, -5.3], [10.6, 9.4, 4.6], zwart)]),
             bone("head", "body", [0, 11, -5], [cube(a, [-5, 8.5, -11], [10, 9, 8], wit, overrides={"north": scherp(panda_gezicht, 2.0)}),
                                                 cube(a, [-1.6, 10.4, -11.6], [3.2, 2.2, 0.8], wit, overrides={"north": scherp(panda_snoet, 3.0)})]),
             bone("tail", "body", [0, 9, 7], [cube(a, [-1.2, 8, 7], [2.4, 2.4, 2], wit)])]
    bones += ears(a, ZWART, (80, 74, 88), 3.4, 16.5, -8, 3.4, 3.4)
    for kant, sx in (("left", 1), ("right", -1)):
        for voor, z in (("front", -4.6), ("back", 3)):
            x = 1.2 if sx > 0 else -4.2
            bones.append(bone(f"leg_{voor}_{kant}", "body", [x + 1.5, 4, z + 1.5], [cube(a, [x, 0, z], [3, 4, 3], zwart)]))
    # --- the giant: about 5 blocks, black, six arms, the white mask face ---
    bones.append(bone("groot", "root", [0, 0, 0], []))
    for kant, sx in (("left", 1), ("right", -1)):
        for voor, z in (("front", -10), ("back", 6)):
            x = 7 if sx > 0 else -13
            bones.append(bone(f"groot_poot_{voor}_{kant}", "groot", [x + 3, 34, z + 3], [
                cube(a, [x, 4, z], [6, 30, 6], zwart), cube(a, [x - 1, 0, z - 1.5], [8, 4, 8], zwart),
                cube(a, [x - 2, 12, z - 2], [10, 6, 10], rook)]))
    bones.append(bone("groot_lijf", "groot", [0, 34, 0], [
        cube(a, [-15, 32, -12], [30, 30, 24], zwart), cube(a, [-12, 30, -9], [24, 4, 18], zwart),
        cube(a, [-11, 61, -4], [9, 7, 9], rook), cube(a, [2, 62, -6], [8, 8, 8], rook), cube(a, [-4, 60, 6], [9, 6, 8], rook)],
        rotation=[14, 0, 0]))
    bones.append(bone("groot_kop", "groot_lijf", [0, 58, -12], [
        cube(a, [-12, 49, -18], [24, 26, 8], zwart),
        cube(a, [-10, 51, -24], [20, 22, 7], zwart, overrides={"north": scherp(gloei(masker_gezicht, oog_gloed, 1.0), 1.0)})],
        rotation=[-10, 0, 0]))
    for n, (y, z, hoek) in enumerate(((58, -6, 22), (50, -2, 34), (42, 2, 46)), start=1):
        for kant, sx in (("left", 1), ("right", -1)):
            x = 15 if sx > 0 else -19
            bones.append(bone(f"groot_arm_{n}_{kant}", "groot_lijf", [sx * 15, y, z], [
                cube(a, [x, y - 30, z - 2], [4, 30, 4], zwart),
                cube(a, [x - 0.5, y - 34, z - 2.5], [5, 4, 5], zwart),
                cube(a, [x + 0.2, y - 38, z - 2.5], [1.2, 4, 1.2], klauw), cube(a, [x + 2.6, y - 38, z - 2.5], [1.2, 4, 1.2], klauw),
                cube(a, [x + 1.4, y - 38, z + 1.3], [1.2, 4, 1.2], klauw)],
                rotation=[-10 + 12 * (n - 1), 0, -sx * hoek]))
    return geo_("hei_bai_guh", bones, 8, 7, [0, 2.5, 0])


def hei_bai_animaties():
    L = 8.0
    armen = [f"groot_arm_{n}_{k}" for n in (1, 2, 3) for k in ("left", "right")]
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (L / 2, [1.03, 1.04, 1.03]), (L, [1, 1, 1])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [0, 0, 10]), (L * 0.45, [0, 0, 10]), (L * 0.6, [0, 0, 0]), (L, [0, 0, 0])])},
            "tail": {"rotation": golf(1, 20, L, keer=4)},
            "groot_lijf": {"rotation": golf(2, 3, L, keer=1), "scale": kf([(0, [1, 1, 1]), (L / 2, [1.03, 1.05, 1.03]), (L, [1, 1, 1])])},
            "groot_kop": {"rotation": golf(1, 10, L, keer=1, fase=0.6)}}
    idle.update(oor_idle(L))
    for j, b in enumerate(armen):
        idle[b] = {"rotation": golf(0, 8, L, fase=j * 0.9, keer=2)}
    praat = {"head": knik(1.2, 10), "groot_kop": knik(1.2, 6), "ear_left": {"rotation": golf(2, -12, 1.2, keer=2)},
             "ear_right": {"rotation": golf(2, 12, 1.2, keer=2)}}
    groei = {"groot": {"scale": kf([(0, [0.15, 0.15, 0.15]), (0.7, [0.8, 1.15, 0.8]), (1.1, [1.12, 0.92, 1.12]), (1.4, [0.96, 1.04, 0.96]),
                                    (1.7, [1, 1, 1]), (2.0, [1, 1, 1])])},
             "klein": {"scale": kf([(0, [1, 1, 1]), (0.4, WEG), (2.0, WEG)])},
             "groot_kop": {"rotation": kf([(0, [0, 0, 0]), (1.1, [-25, 0, 0]), (1.6, [-25, 0, 0]), (2.0, [0, 0, 0])])}}
    for b in armen:
        sx = 1 if b.endswith("left") else -1
        groei[b] = {"rotation": kf([(0, [0, 0, 0]), (1.1, [-60, 0, -sx * 40]), (1.6, [-60, 0, -sx * 40]), (2.0, [0, 0, 0])])}
    kalm = {"groot": {"scale": kf([(0, [1, 1, 1]), (0.4, [1.05, 0.95, 1.05]), (1.6, [0.25, 0.25, 0.25]), (2.0, [0.15, 0.15, 0.15])])},
            "groot_kop": {"rotation": kf([(0, [0, 0, 0]), (0.5, [20, 0, 0]), (2.0, [20, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.5, [0, 0, 14]), (1.5, [0, 0, 14]), (2.0, [0, 0, 0])])},
            "body": {"position": kf([(0, [0, 0, 0]), (0.3, [0, 1.5, 0]), (0.6, [0, 0, 0]), (0.9, [0, 1, 0]), (1.2, [0, 0, 0]), (2.0, [0, 0, 0])])}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "groei": anim(2.0, groei, False), "kalm": anim(2.0, kalm, False)}


# =====================================================================================================================
# fang: the spirit dragon of Roku-guh
# =====================================================================================================================
FANG_N = 10
FANG_ROOD, FANG_LICHT, FANG_ROOM, FANG_GOUD = (206, 46, 44), (246, 110, 80), (252, 236, 206), (255, 206, 96)
FANG_ALFA = 176


def _fang_seg(i):
    """(x, y of the middle, z of the front, thickness) of segment i: a gentle S, tapering to the tail."""
    dik = 14 - 7 * (i / (FANG_N - 1))
    return 6 * math.sin(i * 0.5), 24 + 3 * math.sin(i * 0.7 + 0.4) - (0 if i else 0), -52 + 16 * i, dik


def fang_gezicht(W, H, rng):
    """The guh face in Fang's red, the eyes gold instead of blue."""
    a = guh_face(FANG_LICHT, eye_y=0.42, eye_dx=0.25, eye_r=0.15)(W, H, rng).astype(np.float32)
    rgb = a[..., :3]
    ring = (rgb[..., 2] > 150) & (rgb[..., 0] < 120)                    # the blue -> teal ring of the guh eyes
    t = np.clip((rgb[..., 1] - 100) / 110, 0, 1)
    a[ring, 0] = 230 + 25 * t[ring]
    a[ring, 1] = 150 + 70 * t[ring]
    a[ring, 2] = 40
    return np.clip(a, 0, 255).astype(np.uint8)


def fang(a):
    huid = alfa(plain(FANG_ROOD, 7, lambda W, H, rng, ar: [disc(ar, x + (y // 4 % 2) * 2, y, 1.4, FANG_LICHT, 0.5)
                                                              for y in range(2, H, 4) for x in range(2, W, 4)]), FANG_ALFA)
    buik = alfa(plain(FANG_ROOM, 6, lambda W, H, rng, ar: [ar.__setitem__((slice(y, y + 1), slice(None), slice(0, 3)), (226, 196, 160))
                                                           for y in range(0, H, 4)]), 200)
    manen = gloei(alfa(vacht(FANG_ROOM, (255, 250, 236), 12), 215), dim=0.55)
    stekel = gloei(alfa(plain(FANG_GOUD, 5), 220), dim=0.7)
    klauw = gloei(plain((255, 236, 190), 4), dim=0.6)
    bones = [bone("root", None, [0, 0, 0], []), bone("body", "root", [0, 0, 0], [])]
    for i in range(FANG_N):
        x, y, zs, dik = _fang_seg(i)
        cubes = [cube(a, [x - dik / 2, y - dik / 2, zs], [dik, dik, 16], huid, overrides={"down": buik}),
                 cube(a, [x - 1, y + dik / 2, zs + 5], [2, 2 + dik * 0.2, 6], stekel)]
        if i <= 2:
            cubes.append(cube(a, [x - dik / 2 - 1, y + dik / 2 - 3, zs + 2], [dik + 2, 4.5, 13], manen))
        if i in (1, 6):
            for sx in (-1, 1):
                lx = x + sx * (dik / 2 - 1) - 1.5
                cubes += [cube(a, [lx, y - dik / 2 - 7, zs + 6], [3, 8, 3], huid), cube(a, [lx - 0.5, y - dik / 2 - 8.5, zs + 5], [4, 1.5, 4], klauw)]
        if i == FANG_N - 1:
            cubes.append(cube(a, [x - 4, y - 5, zs + 16], [8, 10, 12], manen))                     # the tail tuft
        bones.append(bone(f"seg_{i}", "body", [x, y, zs + 8], cubes))
    hx, hy, hz = _fang_seg(0)[0], 24, -52
    gezicht = scherp(fang_gezicht, 2.0)
    snuit = alfa(met(plain(FANG_LICHT, 6), lambda W, H, rng, ar: (disc(ar, W * 0.3, H * 0.4, W * 0.07, (90, 20, 30)),
                                                                     disc(ar, W * 0.7, H * 0.4, W * 0.07, (90, 20, 30)))), 220)
    bones.append(bone("head", "body", [hx, hy, hz], [
        cube(a, [hx - 9, hy - 7, hz - 18], [18, 16, 18], huid, overrides={"north": gezicht}),
        cube(a, [hx - 6, hy - 6, hz - 25], [12, 9, 7], huid, overrides={"north": snuit}),
        cube(a, [hx - 2, hy + 1.5, hz - 25.6], [4, 2.5, 1], plain((236, 128, 168), 4)),          # the guh's pink nose
        cube(a, [hx - 11, hy + 7, hz - 12], [22, 6, 12], manen),                                 # the mane over the head
        cube(a, [hx - 3, hy - 11, hz - 23], [6, 5, 4], manen)]))                                 # the beard
    for b in ears(a, FANG_LICHT, (236, 128, 168), 5.5, hy + 9, hz - 10, 6, 6):
        b["pivot"][0] += hx
        for c_ in b["cubes"]:
            c_["origin"][0] += hx
        bones.append(b)
    for kant, sx in (("left", 1), ("right", -1)):                          # the long whiskers
        ox = hx + 6 if sx > 0 else hx - 6 - 22
        bones.append(bone(f"snor_{kant}", "head", [hx + sx * 6, hy - 2, hz - 23],
                          [cube(a, [ox, hy - 2.5, hz - 23.5], [22, 1, 1], stekel)], rotation=[0, -10 * sx, -25 * sx]))
    zx, zy, zz = FANG_ZADEL
    bones.append(bone("zadel", "body", [zx + hx, zy, zz], [
        cube(a, [zx + hx - 6, zy - 1.5, zz - 5], [12, 1.5, 10], gloei(plain((196, 40, 52), 5, lambda W, H, rng, ar: (
            ar.__setitem__((slice(0, 2), slice(None), slice(0, 3)), FANG_GOUD), ar.__setitem__((slice(-2, None), slice(None), slice(0, 3)), FANG_GOUD))),
            lambda b: b[..., 1] > 180, 0.5))]))
    return geo_("fang", bones, 16, 6, [0, 1.5, 0])


def fang_animaties():
    L = 6.0
    idle = {"head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.3, [-6, 14, 0]), (L * 0.6, [4, -10, 0]), (L, [0, 0, 0])])},
            "snor_left": {"rotation": golf(2, 8, L, keer=3)}, "snor_right": {"rotation": golf(2, -8, L, keer=3)}}
    vlieg = {"head": {"rotation": golf(0, 4, 2.0)}, "snor_left": {"rotation": golf(1, 12, 2.0, keer=2)}, "snor_right": {"rotation": golf(1, -12, 2.0, keer=2)}}
    for i in range(FANG_N):
        rust = 0.3 if i < 2 else 1.0                                     # (the neck under the seat moves little)
        idle[f"seg_{i}"] = {"position": golf(1, 1.6 * rust, L, fase=i * 0.6, keer=1)}
        vlieg[f"seg_{i}"] = {"position": golf(1, 3.2 * rust, 2.0, fase=i * 0.8), "rotation": golf(1, 5 * rust, 2.0, fase=i * 0.8 + 1.0)}
    praat = {"head": knik(1.2, 10), "snor_left": {"rotation": golf(2, 14, 1.2, keer=2)}, "snor_right": {"rotation": golf(2, -14, 1.2, keer=2)}}
    buig = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.6, [32, 0, 0]), (1.5, [32, 0, 0]), (2.0, [0, 0, 0])]),
                     "position": kf([(0, [0, 0, 0]), (0.6, [0, -5, 0]), (1.5, [0, -5, 0]), (2.0, [0, 0, 0])])},
            "seg_0": {"position": kf([(0, [0, 0, 0]), (0.6, [0, -3, 0]), (1.5, [0, -3, 0]), (2.0, [0, 0, 0])])}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "vlieg": anim(2.0, vlieg), "buig": anim(2.0, buig, False)}


# =====================================================================================================================
# geest_koi: Tui and La
# =====================================================================================================================
KOI = {0: dict(huid=(246, 244, 240), vlek=(26, 24, 30), vin=(214, 236, 255), rand=(190, 226, 255)),
       1: dict(huid=(32, 30, 40), vlek=(246, 244, 240), vin=(120, 150, 210), rand=(180, 214, 255))}
KOI_ALFA = 180


def koi_schubben(c):
    def extra(W, H, rng, a):
        rij = 6
        for j, y in enumerate(range(0, H + rij, rij)):
            for x in range((rij // 2) * (j % 2) - rij, W + rij, rij):
                disc(a, x + rij / 2, y, rij * 0.55, np.array(c["huid"]) * 0.86 + np.array(c["rand"]) * 0.1, 1.0)
                disc(a, x + rij / 2, y - 1.0, rij * 0.48, c["huid"], 1.0)
    return alfa(plain(c["huid"], 5, extra), KOI_ALFA)


def koi_vin(c, vorm=None):
    """A veil fin: see-through with light rays; `vorm(u, v)` (0..1 over the face) cuts its outline (the rest is clear)."""
    def extra(W, H, rng, a):
        for x in range(0, W, 3):
            a[:, x:x + 1, :3] = c["rand"]
        if vorm is not None:
            yy, xx = np.mgrid[0:H, 0:W]
            a[~vorm((xx + 0.5) / W, (yy + 0.5) / H), 3] = 0
    return gloei(alfa(plain(c["vin"], 6, extra), 120), dim=0.55)


# the thin edges of a shaped fin are clear (else they draw the box round the shape)
RANDLOOS_X = {f: bd.transparent for f in ("north", "south", "up", "down")}
RANDLOOS_Y = {f: bd.transparent for f in ("north", "south", "east", "west")}


def _staart(u, v):
    """The tail fin on its side face (u along the body, 0 = at the tail; v from top to bottom): a fan with a fork."""
    breed = 0.18 + 0.82 * u                                              # (it widens away from the body)
    vork = np.abs(v - 0.5) < 0.42 * np.clip(u - 0.55, 0, 1)             # (the notch in the middle of the far end)
    return (np.abs(v - 0.5) <= breed / 2) & ~vork


def _rugvin(u, v):
    """The dorsal fin on its side face: high at the front, sloping down to the back."""
    return v >= 0.15 + 0.75 * u


def _vinnetje(u, v):
    """A pectoral fin seen from above: a rounded paddle."""
    return ((u - 0.45) / 0.55) ** 2 + ((v - 0.5) / 0.5) ** 2 <= 1


def geest_koi(a, soort=0):
    c = KOI[soort]
    huid, vin = koi_schubben(c), koi_vin(c)
    staart, rug, vinnetje = koi_vin(c, _staart), koi_vin(c, _rugvin), koi_vin(c, _vinnetje)
    vlek = gloei(alfa(met(plain(c["huid"], 5), lambda W, H, rng, ar: disc(ar, W * 0.5, H * 0.45, W * 0.32, c["vlek"], ry=H * 0.3)), KOI_ALFA),
                 lambda b: np.abs(b[..., :3].astype(int) - np.array(c["vlek"])).sum(-1) < 60, 0.6 if soort else 0.25)
    gezicht = scherp(alfa(guh_face(c["huid"], eye_y=0.4, eye_dx=0.3, eye_r=0.13, blush=True), 220), 1.5)
    snor = gloei(alfa(plain(c["rand"], 4), 200), dim=0.5)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 13, 0], [cube(a, [-10, 3, -28], [20, 20, 44], huid, overrides={"up": vlek})]),
             bone("head", "body", [0, 13, -28], [cube(a, [-8.5, 4.5, -38], [17, 16, 10], huid, overrides={"north": gezicht})]),
             bone("rugvin", "body", [0, 23, -6], [cube(a, [-0.5, 23, -20], [1, 9, 30], vin, overrides={"east": rug, "west": rug, **RANDLOOS_X})]),
             bone("tail", "body", [0, 13, 16], [cube(a, [-5.5, 7, 16], [11, 12, 14], huid)]),
             bone("staartvin", "tail", [0, 13, 30], [cube(a, [-0.5, -8, 30], [1, 42, 30], vin, overrides={"east": staart, "west": staart, **RANDLOOS_X})])]
    for kant, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"fin_{kant}", "body", [sx * 10, 8, -18], [cube(a, [10 if sx > 0 else -24, 8, -24], [14, 0.6, 12], vin,
                                                                           overrides={"up": vinnetje, "down": vinnetje, **RANDLOOS_Y})],
                          rotation=[0, 0, -20 * sx]))
        bones.append(bone(f"buikvin_{kant}", "body", [sx * 6, 4, 4], [cube(a, [6 if sx > 0 else -14, 4, 0], [8, 0.6, 9], vin,
                                                                           overrides={"up": vinnetje, "down": vinnetje, **RANDLOOS_Y})],
                          rotation=[0, 0, -35 * sx]))
        bones.append(bone(f"snor_{kant}", "head", [sx * 5, 6, -38], [cube(a, [sx * 5 - 0.5, 5.5, -48], [1, 1, 10], snor)],
                          rotation=[25, 15 * sx, 0]))
    return geo_("geest_koi", bones, 8, 5, [0, 1, 0])


def koi_animaties():
    def zwem(L, amp):
        return {"tail": {"rotation": golf(1, amp, L)}, "staartvin": {"rotation": golf(1, amp * 1.3, L, fase=-1.0)},
                "body": {"rotation": golf(1, amp * 0.25, L, fase=math.pi), "position": golf(1, 0.6, L * 2, keer=1)},
                "head": {"rotation": golf(1, amp * 0.2, L, fase=0.4)},
                "fin_left": {"rotation": golf(2, amp, L, om=(0, 0, -20))}, "fin_right": {"rotation": golf(2, -amp, L, om=(0, 0, 20))},
                "buikvin_left": {"rotation": golf(2, amp * 0.6, L, fase=1.0, om=(0, 0, -35))},
                "buikvin_right": {"rotation": golf(2, -amp * 0.6, L, fase=1.0, om=(0, 0, 35))},
                "rugvin": {"rotation": golf(1, amp * 0.3, L, fase=-0.5)},
                "snor_left": {"rotation": golf(0, 10, L, om=(25, 15, 0))}, "snor_right": {"rotation": golf(0, 10, L, fase=0.5, om=(25, -15, 0))}}
    idle = zwem(4.0, 12)
    praat = {"head": knik(1.2, 8), "snor_left": {"rotation": golf(1, 20, 1.2, keer=2, om=(25, 15, 0))},
             "snor_right": {"rotation": golf(1, -20, 1.2, keer=2, om=(25, -15, 0))}}
    return {"idle": anim(4.0, idle), "praat": anim(1.2, praat, False), "zwem": anim(2.0, zwem(2.0, 22))}


# =====================================================================================================================
# libelkonijnguh
# =====================================================================================================================
LIBEL_VACHT, LIBEL_VLEUGEL, LIBEL_ADER = (232, 226, 248), (186, 238, 255), (110, 214, 255)


def libel_vleugel(W, H, rng):
    a = np.zeros((H, W, 4), np.float32)
    a[..., :3] = LIBEL_VLEUGEL
    a[..., 3] = 110
    for x in range(0, W, 4):
        a[:, x:x + 1, :3] = LIBEL_ADER
        a[:, x:x + 1, 3] = 220
    a[:1, :, :3], a[:1, :, 3] = LIBEL_ADER, 230
    a[-1:, :, :3], a[-1:, :, 3] = LIBEL_ADER, 230
    return a.astype(np.uint8)


def libelkonijnguh(a):
    vacht_ = plain(LIBEL_VACHT, 6)
    gezicht = scherp(guh_face(LIBEL_VACHT, eye_y=0.42, eye_dx=0.25, eye_r=0.17), 4.0)
    oor = met(plain(LIBEL_VACHT, 5), lambda W, H, rng, ar: disc(ar, W / 2, H * 0.55, W * 0.3, (236, 150, 190), ry=H * 0.4))
    vleugel = gloei(libel_vleugel, lambda b: b[..., 3] > 200, 0.8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3.5, 0], [cube(a, [-2.5, 1.5, -3.5], [5, 4.5, 7], vacht_),
                                                 cube(a, [-1, 2.5, 3.4], [2, 2, 1.8], plain((255, 255, 255), 3))]),
             bone("head", "body", [0, 5, -3.5], [cube(a, [-2.6, 3.4, -7.8], [5.2, 4.6, 4.6], vacht_, overrides={"north": gezicht})])]
    for kant, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"ear_{kant}", "head", [sx * 1.4, 7.8, -5.6], [cube(a, [sx * 1.4 - 0.7, 7.8, -6.0], [1.4, 5.2, 0.8], vacht_,
                                                                               overrides={"north": oor})], rotation=[-10, 0, -12 * sx]))
        for voor, z in (("voor", -2.2), ("achter", 1.2)):
            bones.append(bone(f"vleugel_{voor}_{kant}", "body", [sx * 1.2, 6, z], [
                cube(a, [1.2 if sx > 0 else -11.2, 6, z - 1.3], [10, 0.2, 2.6], vleugel)],
                rotation=[0, (14 if voor == "achter" else -10) * sx, -8 * sx]))
        bones.append(bone(f"pootje_{kant}", "body", [sx * 1.2, 2, -2.6], [cube(a, [sx * 1.2 - 0.6, 0.6, -3.2], [1.2, 1.6, 1.2], vacht_)]))
    return geo_("libelkonijnguh", bones, 2, 1, [0, 0.3, 0])


def _flap(L, periode, amp, fase=0.0, om=(0, 0, 0)):
    stap = periode / 4
    n = int(round(L / stap))
    return kf([(i * stap, [om[0], om[1], om[2] + amp * math.sin(2 * math.pi * i * stap / periode + fase)]) for i in range(n + 1)])


def libel_animaties():
    def vleugels(L, periode, amp):
        uit = {}
        for kant, sx in (("left", 1), ("right", -1)):
            for voor, fase, y in (("voor", 0.0, -10), ("achter", 1.4, 14)):
                uit[f"vleugel_{voor}_{kant}"] = {"rotation": _flap(L, periode, amp * sx, fase, om=(0, y * sx, -8 * sx))}
        return uit
    idle = vleugels(2.0, 0.25, 28)
    idle["body"] = {"position": golf(1, 0.6, 2.0, keer=2)}
    idle.update(oor_idle(2.0, 10))
    vlieg = vleugels(1.0, 0.125, 34)
    vlieg["body"] = {"rotation": [14, 0, 0], "position": golf(1, 0.4, 1.0, keer=2)}
    vlieg["ear_left"] = {"rotation": [-30, 0, -12]}
    vlieg["ear_right"] = {"rotation": [-30, 0, 12]}
    weg = vleugels(0.8, 0.1, 36)
    weg["body"] = {"scale": kf([(0, [1, 1, 1]), (0.15, [0.8, 0.8, 1.5]), (0.5, [0.9, 0.9, 1.25]), (0.8, [1, 1, 1])]),
                   "rotation": kf([(0, [0, 0, 0]), (0.15, [22, 0, 0]), (0.8, [0, 0, 0])])}
    weg["ear_left"] = {"rotation": kf([(0, [0, 0, 0]), (0.15, [-55, 0, -10]), (0.6, [-55, 0, -10]), (0.8, [0, 0, 0])])}
    weg["ear_right"] = {"rotation": kf([(0, [0, 0, 0]), (0.15, [-55, 0, 10]), (0.6, [-55, 0, 10]), (0.8, [0, 0, 0])])}
    praat = {"head": knik(0.8, 12), "ear_left": {"rotation": golf(2, -16, 0.8, keer=2)}, "ear_right": {"rotation": golf(2, 16, 0.8, keer=2)}}
    return {"idle": anim(2.0, idle), "praat": anim(0.8, praat, False), "vlieg": anim(1.0, vlieg), "schiet_weg": anim(0.8, weg, False)}


# =====================================================================================================================
# lantaarngeest
# =====================================================================================================================
LANTAARN = ((214, 58, 50), (240, 160, 52), (238, 128, 168), (96, 160, 228))
HOUT = (70, 44, 34)


def papier(kleur, gezicht=False):
    def paint(W, H, rng):
        a = plain(kleur, 8)(W, H, rng).astype(np.float32)
        yy, _ = np.mgrid[0:H, 0:W]
        licht = 1.0 + 0.22 * np.exp(-((yy - H / 2) / (H * 0.35)) ** 2)    # (lit from inside: brighter in the middle)
        a[..., :3] *= licht[..., None]
        for y in range(0, H, max(3, H // 6)):                             # the ribs
            a[y:y + 1, :, :3] *= 0.72
        if gezicht:
            donker = (34, 24, 30)
            for sx in (-1, 1):
                cx, cy = W / 2 + sx * W * 0.2, H * 0.45
                disc(a, cx, cy, W * 0.09, donker, ry=H * 0.1)
                disc(a, cx + W * 0.03, cy - H * 0.035, W * 0.03, (255, 255, 255))
                disc(a, cx + sx * W * 0.05, cy + H * 0.15, W * 0.075, (255, 196, 210), 0.65, ry=H * 0.04)
            boog(a, W / 2 - W * 0.045, H * 0.6, W * 0.045, H * 0.04, max(1.5, W * 0.02), donker, onder=True)
            boog(a, W / 2 + W * 0.045, H * 0.6, W * 0.045, H * 0.04, max(1.5, W * 0.02), donker, onder=True)
        return np.clip(a, 0, 255).astype(np.uint8)
    return gloei(paint, lambda b: b[..., :3].astype(int).max(-1) > 110, 0.5)


def lantaarngeest(a, soort=0):
    kleur = LANTAARN[soort]
    hout, kwast = plain(HOUT, 6), plain((236, 196, 70) if soort != 0 else (250, 210, 80), 6)
    lid = met(plain(tuple(min(255, int(c * 1.22)) for c in kleur), 5), lambda W, H, rng, ar: boog(ar, W / 2, H * 0.3, W * 0.42, H * 0.4, max(1.5, W * 0.12),
                                                                                                 (34, 24, 30), onder=True))
    bones = [bone("root", None, [0, 0, 0], []),
             bone("head", "root", [0, 9, 0], [
                 cube(a, [-6.5, 3, -6.5], [13, 12, 13], papier(kleur), overrides={"north": scherp(papier(kleur, True), 3.0)}),
                 cube(a, [-4.5, 15, -4.5], [9, 1.5, 9], hout), cube(a, [-4.5, 1.5, -4.5], [9, 1.5, 9], hout),
                 cube(a, [-0.6, 16.5, -3], [1.2, 3, 1.2], hout), cube(a, [-0.6, 16.5, 1.8], [1.2, 3, 1.2], hout),
                 cube(a, [-0.6, 19.5, -3], [1.2, 1.2, 6], hout)]),
             bone("kwast", "head", [0, 1.5, 0], [cube(a, [-0.5, -1.5, -0.5], [1, 3, 1], kwast), cube(a, [-1, -5.5, -1], [2, 4, 2], kwast)]),
             # the eyelid of the wink: over the eye at the viewer's left (the lantern's right), shown only in knipoog
             bone("ooglid", "head", [-2.6, 9.6, -6.6], [cube(a, [-4.0, 8.2, -6.75], [2.8, 2.8, 0.3], scherp(lid, 3.0))])]
    return geo_("lantaarngeest", bones, 2, 2, [0, 0.5, 0])


def lantaarn_animaties():
    L = 4.0
    idle = {"head": {"position": golf(1, 1.0, L, keer=1), "rotation": golf(2, 4, L, fase=1.0, keer=1)},
            "kwast": {"rotation": golf(2, -10, L, fase=1.6, keer=2)}, "ooglid": {"scale": WEG}}
    praat = {"head": {"scale": kf([(0, [1, 1, 1]), (0.2, [1.08, 0.92, 1.08]), (0.45, [0.95, 1.06, 0.95]), (0.7, [1.04, 0.97, 1.04]), (1.2, [1, 1, 1])]),
                      "position": kf([(0, [0, 0, 0]), (0.25, [0, 1.5, 0]), (0.5, [0, 0, 0]), (1.2, [0, 0, 0])])},
             "ooglid": {"scale": WEG}}
    knipoog = {"ooglid": {"scale": kf([(0, WEG), (0.15, WEG), (0.2, [1, 1, 1]), (0.7, [1, 1, 1]), (0.75, WEG), (1.0, WEG)])},
               "head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, -10]), (0.7, [0, 0, -10]), (1.0, [0, 0, 0])])}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "knipoog": anim(1.0, knipoog, False)}


# =====================================================================================================================
# schaduwgeest
# =====================================================================================================================
SCHADUW, SCHADUW_LICHT = (24, 20, 32), (56, 44, 72)


def rook(a_):
    def extra(W, H, rng, ar):
        for _ in range(max(2, W * H // 16)):
            disc(ar, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.5, 3.5), SCHADUW_LICHT, 0.5)
    return alfa(plain(SCHADUW, 8, extra), a_)


def schaduw_gezicht(W, H, rng):
    a = rook(255)(W, H, rng).astype(np.float32)
    yy, xx = np.mgrid[0:H, 0:W]
    for sx in (-1, 1):
        cx, cy = W / 2 + sx * W * 0.22, H * 0.48
        oog = ellips(W, H, cx, cy, W * 0.13, H * 0.09) & (yy - cy > -(xx - cx) * sx * 0.3 - H * 0.05)
        a[oog, :3] = (246, 248, 255)
        a[oog, 3] = 255
    return np.clip(a, 0, 255).astype(np.uint8)


def schaduwgeest(a):
    dik, mid, dun = rook(235), rook(190), rook(120)
    gezicht = scherp(gloei(schaduw_gezicht, lambda b: b[..., :3].astype(int).min(-1) > 200, 1.0), 2.0)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 18, 0], [cube(a, [-3, 2, -3], [6, 8, 6], dun), cube(a, [-5, 9, -4], [10, 12, 8], mid),
                                                cube(a, [-6.5, 20, -5], [13, 11, 10], dik), cube(a, [-8, 28, -4.5], [16, 4, 9], dik)]),
             bone("head", "body", [0, 32, 0], [cube(a, [-5, 32, -5.5], [10, 10, 10], dik, overrides={"north": gezicht}),
                                                cube(a, [-3, 41.5, -2.5], [6, 4, 6], dun)])]
    for kant, sx in (("left", 1), ("right", -1)):
        x = 8 if sx > 0 else -11
        bones.append(bone(f"arm_{kant}", "body", [sx * 9.5, 30, 0], [cube(a, [x, 13, -1.5], [3, 18, 3], mid),
                                                                     cube(a, [x - 0.5, 8, -2], [4, 6, 4], dun)]))
    for n, (x, y, z) in enumerate(((-6, 6, 3), (5, 12, -4), (-4, 24, 6), (6, 20, 5)), start=1):
        bones.append(bone(f"rook_{n}", "body", [x, y, z], [cube(a, [x - 1.5, y - 1.5, z - 1.5], [3, 3, 3], dun)]))
    return geo_("schaduwgeest", bones, 2, 3, [0, 1.4, 0])


def schaduw_animaties():
    L = 6.0
    idle = {"body": {"rotation": golf(2, 3, L, keer=1), "position": golf(1, 0.8, L, keer=2)},
            "head": {"rotation": golf(1, 8, L, fase=0.7, keer=1)},
            "arm_left": {"rotation": golf(0, 6, L, keer=2, om=(0, 0, -6))}, "arm_right": {"rotation": golf(0, 6, L, fase=1.2, keer=2, om=(0, 0, 6))}}
    for n in range(1, 5):
        idle[f"rook_{n}"] = {"position": kf([(t * L / 4, [math.sin(t + n) * 1.5, (t * 2.5) % 6, math.cos(t * 1.3 + n) * 1.5]) for t in range(5)]),
                             "scale": kf([(0, [1, 1, 1]), (L / 2, [0.5, 0.5, 0.5]), (L, [1, 1, 1])])}
    duw = {"arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-85, 0, -10]), (0.5, [-85, 0, -10]), (0.8, [0, 0, 0])])},
           "arm_right": {"rotation": kf([(0, [0, 0, 0]), (0.25, [-85, 0, 10]), (0.5, [-85, 0, 10]), (0.8, [0, 0, 0])])},
           "body": {"rotation": kf([(0, [0, 0, 0]), (0.25, [12, 0, 0]), (0.5, [12, 0, 0]), (0.8, [0, 0, 0])])}}
    praat = {"head": knik(1.2, 8), "arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-30, 0, -10]), (0.9, [-20, 0, -10]), (1.2, [0, 0, 0])])}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "duw": anim(0.8, duw, False)}


# =====================================================================================================================
# writing, checking, the offline renders
# =====================================================================================================================
def geo_(identifier, bones, width, height, offset=None):
    g = bd.geo(identifier, bones, width, height)
    if offset is not None:
        g["minecraft:geometry"][0]["description"]["visible_bounds_offset"] = offset
    return g


# id -> (maker(atlas, soort), texel density, animations(), the looks that are their own texture)
MODELLEN = {
    "koh": (koh, 0.22, koh_animaties),
    "hei_bai_guh": (lambda a, s: hei_bai_guh(a), 0.36, hei_bai_animaties),
    "fang": (lambda a, s: fang(a), 0.3, fang_animaties),
    "geest_koi": (geest_koi, 0.36, koi_animaties),
    "libelkonijnguh": (lambda a, s: libelkonijnguh(a), 3.0, libel_animaties),
    "lantaarngeest": (lantaarngeest, 1.5, lantaarn_animaties),
    "schaduwgeest": (lambda a, s: schaduwgeest(a), 1.0, schaduw_animaties),
}


def maak(id, soort=0):
    maker, k, _ = MODELLEN[id]
    atlas = Vel(id, k)
    model = maker(atlas, soort)
    return atlas, model


def build(h):
    A = h.A
    for id, (_, _, animaties) in MODELLEN.items():
        gloed = WEZENS[id][5]
        for n, tex in enumerate(texturen(id)):
            atlas, model = maak(id, n)
            if n == 0:
                h.w(f"{A}/geckolib/models/entity/{id}.geo.json", model)
                h.w(f"{A}/geckolib/animations/entity/{id}.animation.json", {"format_version": "1.8.0", "animations": animaties()})
            h.save(Image.fromarray(atlas.img), "entity", f"{tex}.png")
            if gloed:
                h.save(Image.fromarray(atlas.glowmask()), "entity", f"{tex}_glowmask.png")


def check(h, model_check):
    problems = []
    for id, (_, _, soorten, _, _, gloed, anims) in WEZENS.items():
        wil = ["idle"] + [n for n, _ in anims]
        model_check(id, f"{h.A}/geckolib/models/entity/{id}.geo.json", f"{h.A}/geckolib/animations/entity/{id}.animation.json", wil, ("root",))
        if anims[0][0] != "praat":
            problems.append(f"{id}: the first animation of the row must be praat")
        for tex in texturen(id):
            if not os.path.exists(f"{h.A}/textures/entity/{tex}.png"):
                problems.append(f"{id}: no texture {tex}.png")
            if gloed and not os.path.exists(f"{h.A}/textures/entity/{tex}_glowmask.png"):
                problems.append(f"{id}: no glow mask {tex}_glowmask.png")
    if len(KOH_GEZICHTEN) != WEZENS["koh"][2]:
        problems.append("koh: a face per look")
    if len(LANTAARN) != WEZENS["lantaarngeest"][2] or len(KOI) != WEZENS["geest_koi"][2]:
        problems.append("lantaarngeest / geest_koi: a colour per look")
    return problems


def voorbeelden(alleen=None):
    """(name, geo, texture, glowmask, hidden bones, animations) per creature and look, for the preview."""
    uit = []
    for id, (_, _, animaties) in MODELLEN.items():
        if alleen and id not in alleen:
            continue
        n_looks = WEZENS[id][2] if WEZENS[id][3] else 1
        for n in range(max(1, n_looks)):
            atlas, model = maak(id, n)
            namen = [f"{id}_{n}" if n_looks > 1 else id]
            hides = [()]
            if id == "hei_bai_guh":
                namen, hides = [f"{id}_0", f"{id}_1"], [("groot",), ("klein",)]
            for naam, hide in zip(namen, hides):
                uit.append((naam, model, atlas.img, atlas.glowmask(), hide, animaties()))
    return uit
