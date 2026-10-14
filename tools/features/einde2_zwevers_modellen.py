"""
einde2 (the Guheinde expansion), slice zwevers - the creatures' looks: GeckoLib models, animations and textures (with their
glow masks) of the Kruimelkwal, the Leegtewalvisguh and the four element animals.

Every one of them is guh-family at a glance: the big glossy guh eyes, pink blush and two round guh ears (boerderij_dieren's
guh_face / ears), on a body of its own:
  kruimelkwal          a see-through bell of warm cheese crumbs with a glowing core, four long tentacles and four frills
  leegtewalvisguh      a huge void-lilac whale with a cream belly, starry flanks (they glow), a flat crusty back on which
                       knabbelkoor grows, round guh ears, a jaw that sings. Modelled small (1 model pixel = one block):
                       the entity lives at scale 16 (SCHAAL_WALVIS, its scale attribute), and its Guhdex picture shows the
                       model as it is
  vleermuis_lemurguh   a cream lemur with huge ears, dark wing membranes and a ringed tail (lucht)
  otter_pinguinguh     a slate-blue penguin body with a white otter face, whiskers and four flippers (water)
  das_molguh           a low brown digger with a badger's striped head, eyes shut for good (blind), big claws (aarde)
  schildpad_eendguh    a yellow duck in a green turtle shell (vuur: the palace pond)
Textures are painted face by face on an atlas (boerderij_dieren.Atlas; Vel adds the glow mask and its own texel size).

  build(h)       writes geckolib/models/entity/<id>.geo.json, geckolib/animations/entity/<id>.animation.json,
                 textures/entity/<id>.png for the six, and textures/entity/<id>_glowmask.png for the two that glow (GLOEIT)
  check(h)       every animated bone exists, the animations the Java code asks for exist (NODIG), no bone clashes with
                 the guh variant bone prefixes
  preview(out)   (python tools/features/einde2_zwevers_modellen.py <out>) offline renders of the six, and of the poses
                 that are easy to get wrong (wings folded, hanging, the belly slide, dug in, in the shell)
"""
import copy
import json
import math
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import boerderij_dieren as bd
    from features import landdiertjes_modellen as lm
    from features import einde2_kit as kit
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import boerderij_dieren as bd
    from features import landdiertjes_modellen as lm
    from features import einde2_kit as kit

plain, disc, guh_face, cube, bone, geo = bd.plain, bd.disc, bd.guh_face, bd.cube, bd.bone, bd.geo
kf, anim = lm.kf, lm.anim

SCHAAL_WALVIS = 16.0                # = LeegtewalvisguhEntity.SCHAAL (the entity's scale attribute)

# --- colours ------------------------------------------------------------------------------------------------------------------
KWAL = (255, 222, 140)              # the bell: warm cheese crumb
KWAL_LICHT = (255, 246, 206)
KRUIM = (222, 156, 72)
KRUIM_DONKER = (176, 108, 50)
KERN = (255, 252, 226)

WALVIS = (118, 104, 190)            # void lilac (the back and the flanks)
WALVIS_DONKER = (86, 74, 152)
WALVIS_BUIK = (252, 234, 198)       # cream cheese belly
WALVIS_PLOOI = (226, 196, 150)
STER = (212, 242, 255)              # the little stars on its flanks: exactly this colour = glows
KORST = (236, 196, 96)              # kaaskorst on its back, where the knabbelkoor roots
KORST_GAT = (190, 138, 52)
KOOR = (132, 92, 164)               # knabbelkoor stem
KOOR_DONKER = (96, 62, 126)
KOOR_KNABBEL = (250, 216, 120)
KOOR_BLOEM = (222, 180, 236)        # exactly this colour = glows softly
OOR_BINNEN = (236, 128, 168)

LEMUR = (238, 232, 224)
LEMUR_LICHT = (255, 252, 246)
LEMUR_DONKER = (96, 72, 66)
VLIES = (124, 92, 88)
VLIES_BOT = (176, 140, 128)

OTTER = (74, 104, 156)
OTTER_DONKER = (50, 74, 120)
OTTER_WIT = (250, 250, 252)
OTTER_VOET = (244, 156, 62)

DAS = (132, 98, 70)
DAS_DONKER = (72, 50, 40)
DAS_LICHT = (242, 230, 208)
DAS_NEUS = (238, 136, 160)
KLAUW = (250, 240, 214)

EEND = (252, 218, 112)
EEND_LICHT = (255, 240, 176)
SNAVEL = (246, 150, 52)
SCHILD = (110, 158, 88)
SCHILD_DONKER = (66, 106, 60)
SCHILD_RAND = (198, 214, 132)


# =====================================================================================================================
# the atlas with a glow mask
# =====================================================================================================================
class Vel(bd.Atlas):
    """boerderij_dieren's atlas, plus: a second sheet with what glows (a painter with a `gloei(arr) -> RGBA` attribute
    writes there too), its own texel size (the whale: 8 texels per model pixel, chosen when it lived at scale 8; at scale 16 a texel is two pixels of a block), and a
    generator seeded from a name (kit.rng: never from a place in a list)."""

    def __init__(self, naam, s=bd.S):
        self.s = s
        self.img = np.zeros((bd.ATLAS * s, bd.ATLAS * s, 4), np.uint8)
        self.glow = np.zeros_like(self.img)
        self.planken = []                                               # shelves: [y, height, x of the free end]
        self.rng = kit.rng(f"zwevers/{naam}")

    def alloc(self, w, h):
        """Shelf packing (the whale has faces from 1 to 28 pixels high: one running row wastes half the sheet): a patch
        goes on the lowest shelf it fits on that is at most 2 pixels too high for it, else on a new shelf."""
        w, h = int(math.ceil(w)), int(math.ceil(h))
        best = None
        for plank in self.planken:
            if plank[2] + w <= bd.ATLAS and h <= plank[1] <= h + 2 and (best is None or plank[1] < best[1]):
                best = plank
        if best is None:
            y = sum(p[1] for p in self.planken)
            if y + h > bd.ATLAS:
                raise SystemExit("einde2_zwevers_modellen: the texture atlas is full")
            best = [y, h, 0]
            self.planken.append(best)
        u, v = best[2], best[0]
        best[2] += w
        return u, v

    def patch(self, w, h, painter):
        u, v = self.alloc(w, h)
        s = self.s
        W, H = int(math.ceil(w)) * s, int(math.ceil(h)) * s
        arr = painter(W, H, self.rng)
        self.img[v * s:v * s + H, u * s:u * s + W] = arr
        gloei = getattr(painter, "gloei", None)
        if gloei is not None:
            self.glow[v * s:v * s + H, u * s:u * s + W] = gloei(arr)
        return {"uv": [u, v], "uv_size": [w, h]}


def gloeit(painter, sterkte=255):
    """The same patch, and all of it glows (alpha = how much)."""
    def paint(W, H, rng):
        return painter(W, H, rng)

    def gloei(arr):
        out = arr.copy()
        out[..., 3] = np.where(arr[..., 3] > 0, sterkte, 0)
        return out
    paint.gloei = gloei
    return paint


def gloeit_kleur(painter, *kleuren, sterkte=255):
    """The same patch; only the texels of exactly these colours glow (stars, flower tips)."""
    def paint(W, H, rng):
        return painter(W, H, rng)

    def gloei(arr):
        out = np.zeros_like(arr)
        for k in kleuren:
            m = np.all(arr[..., :3] == np.array(k, np.uint8), axis=2) & (arr[..., 3] > 0)
            out[m, :3] = k
            out[m, 3] = sterkte
        return out
    paint.gloei = gloei
    return paint


# =====================================================================================================================
# painters
# =====================================================================================================================
def kwal_bel(alpha=205, zij=True, gezicht=False):
    """The Kruimelkwal's bell: see-through cheese crumb, lighter on top, crumbs in it; on its front the guh face (the eyes
    are not see-through and do not glow)."""
    def paint(W, H, rng):
        if gezicht:
            seed = int(rng.integers(1 << 30))
            a = guh_face(KWAL, eye_y=0.5, eye_dx=0.24, eye_r=0.15, snoet=False)(W, H, np.random.default_rng(seed)).astype(np.float32)
            kaal = plain(KWAL, 8)(W, H, np.random.default_rng(seed)).astype(np.float32)
            masker = np.abs(a[..., :3] - kaal[..., :3]).sum(2) > 45
        else:
            a = plain(KWAL, 8)(W, H, rng).astype(np.float32)
            masker = np.zeros((H, W), bool)
        if zij:
            f = (np.arange(H) / max(1, H - 1))[:, None, None]
            a[..., :3] = np.where(masker[..., None], a[..., :3], a[..., :3] * (1.07 - 0.17 * f))
        for _ in range(max(2, W * H // 70)):
            x, y = int(rng.uniform(0, W - 1)), int(rng.uniform(0, H - 1))
            if masker[y:y + 2, x:x + 2].any():
                continue
            a[y:y + 2, x:x + 2, :3] = (KRUIM, KRUIM_DONKER, KWAL_LICHT)[int(rng.integers(3))]
        a[..., 3] = np.where(masker, 255, alpha)
        return np.clip(a, 0, 255).astype(np.uint8)

    def gloei(arr):
        out = arr.copy()
        out[..., 3] = np.where(arr[..., 3] == 255, 0, 105)
        return out
    paint.gloei = gloei
    return paint


def kwal_sliert(alpha=210):
    """A tentacle: light, fading out towards its tip."""
    def paint(W, H, rng):
        a = plain(KWAL_LICHT, 8)(W, H, rng).astype(np.float32)
        f = np.arange(H) / max(1, H - 1)
        a[..., 3] = (alpha * (1.0 - 0.55 * f))[:, None]
        for _ in range(max(1, W * H // 40)):
            x, y = int(rng.uniform(0, W)), int(rng.uniform(0, H))
            a[y:y + 1, x:x + 1, :3] = KRUIM
        return np.clip(a, 0, 255).astype(np.uint8)

    def gloei(arr):
        out = arr.copy()
        out[..., 3] = (arr[..., 3] * 0.45).astype(np.uint8)
        return out
    paint.gloei = gloei
    return paint


def sterren(a, rng, n, masker=None):
    """Little four-pointed stars of exactly STER (the glow mask picks that colour)."""
    H, W = a.shape[:2]
    for _ in range(n):
        x, y = int(rng.uniform(2, W - 3)), int(rng.uniform(2, H - 3))
        if masker is not None and not masker[y, x]:
            continue
        groot = rng.random() < 0.35
        a[y, x, :3] = STER
        if groot:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                a[y + dy, x + dx, :3] = STER


def walvishuid(kleur=WALVIS, n_sterren=1.0, vlekken=True):
    """The whale's skin: void lilac with big soft darker and lighter blotches, and stars."""
    def paint(W, H, rng):
        a = plain(kleur, 7)(W, H, rng).astype(np.float32)
        if vlekken:
            for _ in range(max(2, W * H // 900)):
                disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(5, 13), WALVIS_DONKER if rng.random() < 0.6 else (140, 126, 208),
                     0.22, ry=rng.uniform(4, 9))
        a = np.clip(a, 0, 255)
        sterren(a, rng, int(W * H / 260 * n_sterren))
        return a.astype(np.uint8)
    return gloeit_kleur(paint, STER)


def walvisflank(n_sterren=1.4):
    """A flank: lilac above, the cream belly below, a wavy line between them; the stars are thickest along the line."""
    def paint(W, H, rng):
        a = plain(WALVIS, 7)(W, H, rng).astype(np.float32)
        for _ in range(max(2, W * H // 900)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H * 0.6), rng.uniform(5, 12), WALVIS_DONKER, 0.2, ry=rng.uniform(4, 8))
        fase = rng.uniform(0, 6.28)
        boven = np.zeros((H, W), bool)
        for x in range(W):
            grens = int(H * 0.66 + math.sin(x / W * 9.0 + fase) * H * 0.045)
            boven[:grens, x] = True
            a[grens:, x, :3] = np.array(WALVIS_BUIK) + rng.normal(0, 3, (H - grens, 1))
            if 0 <= grens - 1 < H:
                a[grens - 1, x, :3] = WALVIS_DONKER
        a = np.clip(a, 0, 255)
        sterren(a, rng, int(W * H / 200 * n_sterren), boven)
        return a.astype(np.uint8)
    return gloeit_kleur(paint, STER)


def walvisbuik(langs_z=True):
    """The belly: cream with the long throat pleats of a whale."""
    def paint(W, H, rng):
        a = plain(WALVIS_BUIK, 6)(W, H, rng).astype(np.float32)
        stap = max(6, (W if langs_z else H) // 9)
        for k in range(stap // 2, (W if langs_z else H), stap):
            if langs_z:
                a[:, k:k + 2, :3] = WALVIS_PLOOI
            else:
                a[k:k + 2, :, :3] = WALVIS_PLOOI
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def walvisdek(plekken):
    """The back, from above: skin, and around every (u, v) of plekken (0..1) a patch of kaaskorst full of holes, where
    the knabbelkoor roots. Fewer stars here: you walk on it."""
    def paint(W, H, rng):
        a = plain(WALVIS, 7)(W, H, rng).astype(np.float32)
        for _ in range(max(2, W * H // 800)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(6, 14), WALVIS_DONKER, 0.2, ry=rng.uniform(5, 10))
        # a darker line along the spine
        a[:, W // 2 - 1:W // 2 + 1, :3] *= 0.88
        for (u, v) in plekken:
            cx, cy = u * W, v * H
            for _ in range(7):
                disc(a, cx + rng.uniform(-9, 9), cy + rng.uniform(-9, 9), rng.uniform(5, 9), KORST, 1.0)
            for _ in range(6):
                disc(a, cx + rng.uniform(-11, 11), cy + rng.uniform(-11, 11), rng.uniform(1.2, 2.6), KORST_GAT, 1.0)
        a = np.clip(a, 0, 255)
        sterren(a, rng, int(W * H / 700))
        return a.astype(np.uint8)
    return gloeit_kleur(paint, STER)


def blaasgat(W, H, rng, a):
    disc(a, W / 2, H * 0.5, W * 0.06, WALVIS_DONKER, 1.0, ry=H * 0.16)
    disc(a, W / 2, H * 0.5, W * 0.03, (40, 30, 70), 1.0, ry=H * 0.09)


def staartvin(W, H, rng):
    """The tail fluke from above/below: two lobes, a notch in the middle of its trailing edge (see-through corners)."""
    a = walvishuid(n_sterren=0.6)(W, H, rng).copy()
    for y in range(H):
        for x in range(W):
            u, v = (x + 0.5) / W - 0.5, (y + 0.5) / H
            rand = 0.55 + 0.45 * math.cos(u * math.pi * 0.95)          # the lobes get shorter to the sides
            kerf = v > 1.0 - 0.5 * max(0.0, 1.0 - abs(u) / 0.16)       # the notch in the middle
            voor = v < 0.34 * (abs(u) / 0.5) ** 1.4                    # swept-back leading edge
            if v > rand or kerf or voor:
                a[y, x, 3] = 0
    return a


def borstvin(spiegel=False):
    """A side fin from above/below: rounded tip (see-through corners)."""
    def paint(W, H, rng):
        a = walvishuid(n_sterren=0.5)(W, H, rng).copy()
        for y in range(H):
            for x in range(W):
                u, v = (x + 0.5) / W, (y + 0.5) / H
                if spiegel:
                    u = 1 - u
                if u > 0.55 and ((u - 0.55) / 0.45) ** 2 + ((v - 0.5) / 0.5) ** 2 > 1:
                    a[y, x, 3] = 0
                if v > 0.72 + 0.28 * (1 - u) or v < 0.12 * u:
                    a[y, x, 3] = 0
        return a
    return gloeit_kleur(paint, STER)


def koorstengel(W, H, rng):
    """A knabbelkoor stem: purple, darker edges, little nibble-yellow dots."""
    a = plain(KOOR, 9)(W, H, rng).astype(np.float32)
    a[:, :max(1, W // 8), :3] = np.array(KOOR_DONKER)
    a[:, -max(1, W // 8):, :3] = np.array(KOOR_DONKER)
    for _ in range(max(1, W * H // 55)):
        x, y = int(rng.uniform(1, W - 2)), int(rng.uniform(0, H - 1))
        a[y:y + 2, x:x + 2, :3] = KOOR_KNABBEL if rng.random() < 0.5 else (166, 124, 196)
    return np.clip(a, 0, 255).astype(np.uint8)


def _koorbloem(W, H, rng):
    a = plain((176, 132, 204), 8)(W, H, rng).astype(np.float32)
    r = W * 0.3
    for y in range(H):
        for x in range(W):
            if math.hypot(x + 0.5 - W / 2, y + 0.5 - H / 2) < r:
                a[y, x, :3] = KOOR_BLOEM
    disc(a, W / 2, H / 2, max(1.0, W * 0.1), KOOR_KNABBEL, 1.0)
    return np.clip(a, 0, 255).astype(np.uint8)


koorbloem = gloeit_kleur(_koorbloem, KOOR_BLOEM, KOOR_KNABBEL, sterkte=200)


def vlies(spiegel=False):
    """A wing membrane: dark, with lighter finger bones fanning out from the shoulder and a scalloped lower edge."""
    def paint(W, H, rng):
        a = plain(VLIES, 8)(W, H, rng).astype(np.float32)
        for k in range(3):
            # a finger bone from the top inner corner down to the lower edge
            x1 = W * (0.3 + 0.33 * k)
            for i in range(H * 2):
                t = i / (H * 2 - 1)
                x, y = int(t * x1), int(t * (H - 1) * (0.95 - 0.12 * k))
                a[y:y + 1, max(0, x):x + 2, :3] = VLIES_BOT
        a[:2, :, :3] = VLIES_BOT                                        # the arm along the top
        boog = W / 3.0
        for x in range(W):
            diep = abs(math.sin((x + 0.5) / boog * math.pi)) * H * 0.2
            for y in range(H):
                if y > H - 1 - diep:
                    a[y, x, 3] = 0
        if spiegel:
            a = a[:, ::-1]
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def ringen(langs="x", a=LEMUR_LICHT, b=LEMUR_DONKER, breed=4):
    """The lemur's ringed tail: bands across the tail (langs = the patch axis the tail runs along)."""
    def paint(W, H, rng):
        arr = plain(a, 6)(W, H, rng).astype(np.float32)
        donker = plain(b, 6)(W, H, rng).astype(np.float32)
        n = W if langs == "x" else H
        for k in range(n):
            if (k // breed) % 2 == 1:
                if langs == "x":
                    arr[:, k] = donker[:, k]
                else:
                    arr[k, :] = donker[k, :]
        return np.clip(arr, 0, 255).astype(np.uint8)
    return paint


def otter_gezicht(W, H, rng):
    """The otter-penguin's face: white, a slate cap coming down between the eyes, guh eyes, whisker dots."""
    def spots(W, H, rng, a):
        for y in range(H):
            for x in range(W):
                u, v = (x + 0.5) / W - 0.5, (y + 0.5) / H
                if v < 0.2 + 0.16 * max(0.0, 1 - abs(u) / 0.14) or abs(u) > 0.5 - 0.06 * (1 - v):
                    a[y, x, :3] = np.array(OTTER) + (a[y, x, :3] - np.array(OTTER_WIT))
    a = guh_face(OTTER_WIT, eye_y=0.5, eye_dx=0.25, eye_r=0.16, snoet=False, spots=spots)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):
        for k in range(3):
            x, y = int(W / 2 + sx * W * (0.12 + 0.05 * k)), int(H * (0.86 + 0.03 * (k % 2)))
            a[y:y + 1, x:x + 1, :3] = OTTER_DONKER
    return np.clip(a, 0, 255).astype(np.uint8)


def das_gezicht(W, H, rng):
    """The badger-mole's face: cream, two dark badger stripes down over the eyes, and the eyes shut in two content little
    arcs (it is blind: it "sees" with its nose and its paws). Pink blush."""
    a = plain(DAS_LICHT, 7)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):
        cx = W / 2 + sx * W * 0.26
        half = W * 0.13
        for y in range(H):
            for x in range(int(cx - half), int(cx + half) + 1):
                if 0 <= x < W:
                    a[y, x, :3] = np.array(DAS_DONKER) + rng.normal(0, 3)
        # the shut eye: a little arc, light on the dark stripe
        ey, r = H * 0.42, W * 0.105
        for i in range(-int(r), int(r) + 1):
            yy = int(ey + (abs(i) / r) ** 2 * r * 0.9)
            xx = int(cx + i)
            if 0 <= xx < W and 0 <= yy < H - 2:
                a[yy:yy + 2, xx, :3] = DAS_LICHT
        disc(a, cx + sx * W * 0.14, H * 0.74, W * 0.07, (255, 150, 186), 0.6, ry=H * 0.07)
    return np.clip(a, 0, 255).astype(np.uint8)


def das_streep(langs_h=True):
    """Seen from above: brown with the badger's light stripe down the middle and a dark one on either side of it."""
    def paint(W, H, rng):
        a = plain(DAS, 9)(W, H, rng).astype(np.float32)
        n = W
        for x in range(n):
            u = abs((x + 0.5) / n - 0.5)
            if u < 0.09:
                a[:, x, :3] = np.array(DAS_LICHT) + rng.normal(0, 3, (H, 1))
            elif u < 0.2:
                a[:, x, :3] = np.array(DAS_DONKER) + rng.normal(0, 3, (H, 1))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def schildplaten(rand=True):
    """The turtle shell: green plates between darker seams, a lighter rim."""
    def paint(W, H, rng):
        a = plain(SCHILD, 8)(W, H, rng).astype(np.float32)
        stap = max(6, min(W, H) // 2 + 2)
        for y in range(H):
            for x in range(W):
                rij = y // stap
                xx = x + (stap // 2 if rij % 2 else 0)
                if y % stap == 0 or xx % stap == 0:
                    a[y, x, :3] = SCHILD_DONKER
                elif (y % stap) in (1, 2) and (xx % stap) in (1, 2, 3):
                    a[y, x, :3] = np.array(SCHILD) * 1.18              # a glint on every plate
        if rand:
            a[-max(2, H // 6):, :, :3] = np.array(SCHILD_RAND) + rng.normal(0, 3, (max(2, H // 6), W, 1))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def snavel(W, H, rng):
    a = plain(SNAVEL, 6)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):
        x, y = int(W / 2 + sx * W * 0.2), int(H * 0.3)
        a[y:y + 1, x:x + 1, :3] = (170, 90, 30)
    a[int(H * 0.62):int(H * 0.62) + 1, :, :3] = (196, 108, 36)
    return np.clip(a, 0, 255).astype(np.uint8)


# =====================================================================================================================
# models
# =====================================================================================================================
def kruimelkwal(atlas):
    bel, bel_top, sliert = kwal_bel(), kwal_bel(zij=False), kwal_sliert()
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 8, 0], []),
             bone("kern", "body", [0, 8.5, 0], [cube(atlas, [-2.5, 6.8, -2.5], [5, 3.6, 5], gloeit(plain(KERN, 5), 255))]),
             bone("bel", "body", [0, 5.5, 0], [
                 cube(atlas, [-5, 6, -5], [10, 5.5, 10], bel, overrides={"north": kwal_bel(gezicht=True), "up": bel_top, "down": bel_top}),
                 cube(atlas, [-3.5, 11.5, -3.5], [7, 1.4, 7], bel, overrides={"up": bel_top, "down": bel_top}),
                 cube(atlas, [-5.6, 5.0, -5.6], [11.2, 1.2, 11.2], bel, overrides={"up": bel_top, "down": bel_top})])]
    # two tiny round guh ears on the cap
    bones += bd.ears(atlas, KWAL, OOR_BINNEN, 1.6, 12.4, -1.0, 2.2, 2.2, parent="bel")
    for i, (x, z) in enumerate(((3.2, -3.2), (-3.2, -3.2), (3.2, 3.2), (-3.2, 3.2))):
        bones.append(bone(f"tentakel_{i + 1}", "body", [x, 5.2, z], [cube(atlas, [x - 0.6, 0.2, z - 0.6], [1.2, 5.0, 1.2], sliert)]))
    for i, (x, z) in enumerate(((0, -4.2), (0, 4.2), (4.2, 0), (-4.2, 0))):
        bones.append(bone(f"sliert_{i + 1}", "body", [x, 5.2, z], [cube(atlas, [x - 0.5, 2.4, z - 0.5], [1.0, 2.8, 1.0], sliert)]))
    # crumbs that float along with it
    for i, (x, y, z) in enumerate(((6.6, 9.5, 1.5), (-6.8, 7.2, -1.0), (1.0, 13.6, 4.4))):
        bones.append(bone(f"kruimel_{i + 1}", "body", [x, y, z], [cube(atlas, [x - 0.5, y - 0.5, z - 0.5], [1, 1, 1], gloeit(plain(KRUIM, 10), 160))]))
    return geo("kruimelkwal", bones, 1.6, 1.6)


def kruimelkwal_anims():
    def puls(L, sterk):
        return {"bel": {"scale": kf([(0, [1, 1, 1]), (L * 0.35, [1 + sterk, 1 - sterk * 0.9, 1 + sterk]), (L * 0.6, [1 - sterk * 0.5, 1 + sterk * 0.6, 1 - sterk * 0.5]),
                                     (L, [1, 1, 1])])},
                "kern": {"scale": kf([(0, [1, 1, 1]), (L * 0.5, [1.12, 1.12, 1.12]), (L, [1, 1, 1])])},
                "body": {"position": kf([(0, [0, 0, 0]), (L * 0.5, [0, 0.8 * sterk * 10, 0]), (L, [0, 0, 0])])}}

    def slierten(L, hoek):
        out = {}
        for i in range(4):
            sx, sz = (1, -1, 1, -1)[i], (-1, -1, 1, 1)[i]
            out[f"tentakel_{i + 1}"] = {"rotation": kf([(0, [sz * hoek * 0.3, 0, -sx * hoek * 0.3]), (L * 0.5, [-sz * hoek, 0, sx * hoek]),
                                                        (L, [sz * hoek * 0.3, 0, -sx * hoek * 0.3])])}
        for i, (ax, az) in enumerate(((1, 0), (-1, 0), (0, -1), (0, 1))):
            out[f"sliert_{i + 1}"] = {"rotation": kf([(0, [0, 0, 0]), (L * 0.5, [ax * hoek * 1.3, 0, az * hoek * 1.3]), (L, [0, 0, 0])])}
        return out

    def kruimels(L):
        return {f"kruimel_{i + 1}": {"position": kf([(0, [0, 0, 0]), (L * (0.3 + 0.15 * i), [0, 0.9, 0]), (L, [0, 0, 0])])} for i in range(3)}
    idle = {**puls(2.4, 0.07), **slierten(2.4, 10), **kruimels(2.4)}
    zwem = {**puls(1.2, 0.12), **slierten(1.2, 22), **kruimels(1.2)}
    stuiter = {"bel": {"scale": kf([(0, [1, 1, 1]), (0.08, [1.35, 0.5, 1.35]), (0.25, [0.85, 1.25, 0.85]), (0.42, [1.1, 0.9, 1.1]), (0.6, [1, 1, 1])])},
               "body": {"position": kf([(0, [0, 0, 0]), (0.08, [0, -2.5, 0]), (0.25, [0, 1.2, 0]), (0.6, [0, 0, 0])])},
               "kern": {"scale": kf([(0, [1, 1, 1]), (0.1, [1.5, 1.5, 1.5]), (0.6, [1, 1, 1])])}}
    for i in range(4):
        sx, sz = (1, -1, 1, -1)[i], (-1, -1, 1, 1)[i]
        stuiter[f"tentakel_{i + 1}"] = {"rotation": kf([(0, [0, 0, 0]), (0.1, [-sz * 55, 0, sx * 55]), (0.35, [sz * 15, 0, -sx * 15]), (0.6, [0, 0, 0])])}
    blij = {"body": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 120, 0]), (0.4, [0, 240, 0]), (0.6, [0, 360, 0])]),
                     "position": kf([(0, [0, 0, 0]), (0.3, [0, 2, 0]), (0.6, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(2.4, idle), "zwem": anim(1.2, zwem), "stuiter": anim(0.6, stuiter, False), "blij": anim(0.6, blij, False)}}


# the knabbelkoor on the whale's back: (x, z, height, [(branch dx, dz, at height)]) in model pixels (1 = one block);
# the middle of the back stays free to walk and to land on
KOOR_PLANTEN = [(-7.0, -8.0, 7.0, [(1, 0, 3.0)]), (6.5, -3.5, 9.0, [(-1, 0, 4.0), (0, 1, 6.5)]), (-6.5, 4.0, 5.0, []),
                (7.0, 9.5, 8.0, [(0, -1, 3.5)]), (-3.0, 12.0, 6.0, [(1, 0, 2.5)]), (1.5, -12.0, 4.0, [])]
RUG_Y = 12.0                        # the top of the back in model pixels (= LeegtewalvisguhEntity.RUG / the scale)


def leegtewalvisguh(atlas):
    huid, flank, buik = walvishuid(), walvisflank(), walvisbuik()
    dek = walvisdek([((x + 10) / 20.0, (z + 14) / 28.0) for x, z, _, _ in KOOR_PLANTEN])
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [
                 # the hull: its top is the back you walk on (RUG_Y)
                 cube(atlas, [-10, 1, -14], [20, 11, 28], flank, overrides={"up": dek, "down": buik, "north": huid, "south": huid}),
                 cube(atlas, [-8.5, 0, -12], [17, 1, 24], buik),
                 cube(atlas, [-11, 3.5, -11], [1, 6.5, 22], huid), cube(atlas, [10, 3.5, -11], [1, 6.5, 22], huid)]),
             bone("head", "body", [0, 6, -14], [
                 cube(atlas, [-9, 2, -20], [18, 10, 6], flank, overrides={"up": plain(WALVIS, 7, blaasgat), "down": buik, "north": huid}),
                 cube(atlas, [-7.5, 3, -23], [15, 8.5, 3], huid, overrides={
                     "north": guh_face(WALVIS, eye_y=0.42, eye_dx=0.28, eye_r=0.15, snoet=True)})]),
             bone("jaw", "head", [0, 3.2, -17], [
                 cube(atlas, [-7.5, 0.6, -22.6], [15, 2.4, 6.6], walvisbuik(False), overrides={"up": plain((236, 150, 170), 8)})]),
             bone("tail_1", "body", [0, 6.5, 14], [cube(atlas, [-7, 2.5, 14], [14, 8.5, 6], flank, overrides={"up": huid, "down": buik})]),
             bone("tail_2", "tail_1", [0, 6.5, 20], [cube(atlas, [-5.5, 3.5, 20], [11, 6.5, 6], flank, overrides={"up": huid, "down": buik})]),
             bone("tail_3", "tail_2", [0, 6.6, 26], [cube(atlas, [-3.5, 4.6, 26], [7, 4.2, 4], huid)]),
             bone("fluke", "tail_3", [0, 6.6, 29.5], [cube(atlas, [-10, 5.9, 29], [20, 1.4, 6.5], huid,
                                                           overrides={"up": gloeit_kleur(staartvin, STER), "down": gloeit_kleur(staartvin, STER)})])]
    bones += bd.ears(atlas, WALVIS, OOR_BINNEN, 5.0, 10.6, -18.5, 5.0, 5.0)
    for side, sx in (("left", 1), ("right", -1)):
        x0 = 10.5 if sx > 0 else -19.5
        vin = borstvin(spiegel=sx < 0)
        bones.append(bone(f"fin_{side}", "body", [sx * 10.5, 3.5, -5.5], [
            cube(atlas, [x0, 2.8, -9], [9, 1.4, 7], huid, overrides={"up": vin, "down": vin})], rotation=[0, 0, 14 * sx]))
    for i, (x, z, hoog, takken) in enumerate(KOOR_PLANTEN):
        cubes = [cube(atlas, [x - 1.6, RUG_Y, z - 1.6], [3.2, 0.5, 3.2], plain(KORST, 8, lambda W, H, rng, a: disc(a, W * 0.3, H * 0.6, 1.6, KORST_GAT))),
                 cube(atlas, [x - 1, RUG_Y + 0.5, z - 1], [2, hoog - 0.5, 2], koorstengel),
                 cube(atlas, [x - 1.2, RUG_Y + hoog, z - 1.2], [2.4, 2.4, 2.4], koorbloem)]
        for dx, dz, op in takken:
            cubes.append(cube(atlas, [x - 1 + dx * 2, RUG_Y + op, z - 1 + dz * 2], [2, 2, 2], koorstengel))
            cubes.append(cube(atlas, [x - 1.1 + dx * 2, RUG_Y + op + 2, z - 1.1 + dz * 2], [2.2, 2.2, 2.2], koorbloem))
        bones.append(bone(f"koor_{i + 1}", "body", [x, RUG_Y, z], cubes))
    return geo("leegtewalvisguh", bones, 30, 14)


def leegtewalvisguh_anims():
    def staart(L, sterk):
        return {"tail_1": {"rotation": kf([(0, [sterk * 0.6, 0, 0]), (L / 2, [-sterk * 0.6, 0, 0]), (L, [sterk * 0.6, 0, 0])])},
                "tail_2": {"rotation": kf([(0, [0, 0, 0]), (L * 0.25, [sterk, 0, 0]), (L * 0.75, [-sterk, 0, 0]), (L, [0, 0, 0])])},
                "tail_3": {"rotation": kf([(0, [-sterk * 0.5, 0, 0]), (L * 0.35, [sterk, 0, 0]), (L * 0.85, [-sterk, 0, 0]), (L, [-sterk * 0.5, 0, 0])])},
                "fluke": {"rotation": kf([(0, [-sterk, 0, 0]), (L * 0.45, [sterk * 1.4, 0, 0]), (L * 0.95, [-sterk * 1.4, 0, 0]), (L, [-sterk, 0, 0])])},
                "fin_left": {"rotation": kf([(0, [0, 0, 0]), (L / 2, [0, -6, 9]), (L, [0, 0, 0])])},
                "fin_right": {"rotation": kf([(0, [0, 0, 0]), (L / 2, [0, 6, -9]), (L, [0, 0, 0])])}}

    def koor(L, hoek):
        out = {}
        for i in range(len(KOOR_PLANTEN)):
            f = (i * 0.17) % 1.0
            out[f"koor_{i + 1}"] = {"rotation": kf([(0, [0, 0, hoek * math.sin(f * 6.28)]), (L * 0.25, [hoek * 0.6, 0, hoek * math.sin(f * 6.28 + 1.57)]),
                                                    (L * 0.5, [0, 0, hoek * math.sin(f * 6.28 + 3.14)]), (L * 0.75, [-hoek * 0.6, 0, hoek * math.sin(f * 6.28 + 4.71)]),
                                                    (L, [0, 0, hoek * math.sin(f * 6.28)])])}
        return out
    oren = lm.oortjes(8.0, 10)
    idle = {**staart(8.0, 5.0), **koor(8.0, 2.5), **oren}
    # singing: the jaw opens on every long note (LeegtewalvisguhEntity.ZANG: a note every 2 seconds), the knabbelkoor sways along
    zing = {**staart(8.0, 6.0), **koor(2.0, 7.0), **lm.oortjes(8.0, 16),
            "jaw": {"rotation": kf([(0, [0, 0, 0]), (0.3, [15, 0, 0]), (1.5, [12, 0, 0]), (1.9, [0, 0, 0]), (2.0, [0, 0, 0]), (2.3, [18, 0, 0]),
                                    (3.5, [14, 0, 0]), (3.9, [0, 0, 0]), (4.0, [0, 0, 0]), (4.3, [13, 0, 0]), (5.5, [10, 0, 0]), (5.9, [0, 0, 0]),
                                    (6.0, [0, 0, 0]), (6.3, [20, 0, 0]), (7.6, [14, 0, 0]), (8.0, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (4.0, [-2.5, 0, 0]), (8.0, [0, 0, 0])])}}
    eet = {"jaw": {"rotation": kf([(0, [0, 0, 0]), (0.4, [26, 0, 0]), (0.9, [26, 0, 0]), (1.1, [0, 0, 0]), (1.4, [8, 0, 0]), (1.6, [0, 0, 0]),
                                   (1.9, [8, 0, 0]), (2.1, [0, 0, 0])])},
           "ear_left": {"rotation": kf([(0, [0, 0, 0]), (1.2, [0, 0, -16]), (1.5, [0, 0, 0]), (1.8, [0, 0, -16]), (2.1, [0, 0, 0])])},
           "ear_right": {"rotation": kf([(0, [0, 0, 0]), (1.2, [0, 0, 16]), (1.5, [0, 0, 0]), (1.8, [0, 0, 16]), (2.1, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {"idle": anim(8.0, idle), "zing": anim(8.0, zing), "eet": anim(2.1, eet, False)}}


def vleermuis_lemurguh(atlas):
    vacht = plain(LEMUR, 7)
    donker = plain(LEMUR_DONKER, 7)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4.8, 0], [
                 cube(atlas, [-2, 2, -1.5], [4, 4.5, 3], vacht, overrides={"north": lm.buik_op(LEMUR, LEMUR_LICHT)})]),
             bone("head", "body", [0, 6.5, 0], [
                 cube(atlas, [-3, 6.5, -3], [6, 5, 5], vacht, overrides={
                     "north": guh_face(LEMUR, eye_y=0.46, eye_dx=0.25, eye_r=0.18, snoet=False)}),
                 cube(atlas, [-1, 7.0, -3.5], [2, 1.4, 0.6], plain(LEMUR_LICHT, 5), overrides={"north": lm.neusje(LEMUR_LICHT, (110, 70, 70), 0.5)})])]
    # the big bat ears of a lemur: dark outside, pink inside
    bones += bd.ears(atlas, LEMUR_DONKER, OOR_BINNEN, 1.9, 10.4, -1.2, 3.6, 4.4)
    for side, sx in (("left", 1), ("right", -1)):
        x0, x1 = (2.0, 7.5) if sx > 0 else (-7.5, -12.0)
        v = vlies(spiegel=sx < 0)
        bones.append(bone(f"wing_{side}", "body", [sx * 2.0, 6.3, 0], [
            cube(atlas, [x0 if sx > 0 else -7.5, 2.4, -0.3], [5.5, 4.0, 0.6], donker, overrides={"north": v, "south": vlies(spiegel=sx > 0)})]))
        bones.append(bone(f"wing_{side}_tip", f"wing_{side}", [sx * 7.5, 6.3, 0], [
            cube(atlas, [7.5 if sx > 0 else -12.0, 3.0, -0.3], [4.5, 3.4, 0.6], donker, overrides={"north": v, "south": vlies(spiegel=sx > 0)})]))
        lx = 0.3 if sx > 0 else -1.9
        bones.append(bone(f"leg_{side}", "body", [lx + 0.8, 2, 0], [cube(atlas, [lx, 0, -1.2], [1.6, 2.0, 2.2], donker)]))
    ring_zij, ring_top = ringen("x", breed=4), ringen("y", breed=4)
    for i in (1, 2):
        z = 1.5 + (i - 1) * 4.0
        bones.append(bone(f"tail_{i}", "body" if i == 1 else "tail_1", [0, 2.8, z], [
            cube(atlas, [-0.7, 2.1, z], [1.4, 1.4, 4.0], plain(LEMUR_LICHT, 5),
                 overrides={"east": ring_zij, "west": ring_zij, "up": ring_top, "down": ring_top, "south": plain(LEMUR_DONKER, 5)})]))
    return geo("vleermuis_lemurguh", bones, 1.6, 1.2)


def _vleugels(dicht, tip):
    """Both wings folded down along the body by this many degrees (0 = spread)."""
    return {"wing_left": {"rotation": [0, 0, dicht]}, "wing_right": {"rotation": [0, 0, -dicht]},
            "wing_left_tip": {"rotation": [0, 0, tip]}, "wing_right_tip": {"rotation": [0, 0, -tip]}}


def vleermuis_lemurguh_anims():
    staart = {"tail_1": {"rotation": kf([(0, [0, 0, 0]), (1.0, [0, 14, 0]), (2.0, [0, -14, 0]), (3.0, [0, 0, 0])])},
              "tail_2": {"rotation": kf([(0, [-20, 0, 0]), (1.0, [-20, 18, 0]), (2.0, [-20, -18, 0]), (3.0, [-20, 0, 0])])}}
    idle = {**_vleugels(78, 70), **staart, **lm.oortjes(3.0, 20),
            "head": {"rotation": kf([(0, [0, 0, 0]), (1.2, [0, 0, 7]), (1.8, [0, 0, 7]), (2.4, [0, 0, -5]), (3.0, [0, 0, 0])])}}
    L = 0.4
    walk = {**_vleugels(78, 70),
            "body": {"position": kf([(0, [0, 0, 0]), (L * 0.4, [0, 1.6, 0]), (L, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 0]), (L * 0.3, [-8, 0, 0]), (L * 0.7, [6, 0, 0]), (L, [0, 0, 0])])},
            "leg_left": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [40, 0, 0]), (L, [0, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [0, 0, 0]), (L * 0.4, [40, 0, 0]), (L, [0, 0, 0])])},
            "tail_1": {"rotation": kf([(0, [-10, 0, 0]), (L * 0.5, [14, 0, 0]), (L, [-10, 0, 0])])}}
    F = 0.3
    vlieg = {"body": {"rotation": [28, 0, 0], "position": kf([(0, [0, 0, 0]), (F / 2, [0, 0.9, 0]), (F, [0, 0, 0])])},
             "head": {"rotation": [-24, 0, 0]},
             "wing_left": {"rotation": kf([(0, [0, 0, -38]), (F / 2, [0, 0, 34]), (F, [0, 0, -38])])},
             "wing_right": {"rotation": kf([(0, [0, 0, 38]), (F / 2, [0, 0, -34]), (F, [0, 0, 38])])},
             "wing_left_tip": {"rotation": kf([(0, [0, 0, -14]), (F / 2, [0, 0, 26]), (F, [0, 0, -14])])},
             "wing_right_tip": {"rotation": kf([(0, [0, 0, 14]), (F / 2, [0, 0, -26]), (F, [0, 0, 14])])},
             "leg_left": {"rotation": [55, 0, 0]}, "leg_right": {"rotation": [55, 0, 0]},
             "tail_1": {"rotation": kf([(0, [-12, 0, 0]), (F / 2, [-4, 0, 0]), (F, [-12, 0, 0])])}}
    # upside down from its feet (the body turns over around the middle of its hitbox), the wings wrapped along the body
    hang = {**_vleugels(84, 80),
            "body": {"rotation": kf([(0, [0, 0, 180]), (2.0, [3, 0, 183]), (4.0, [0, 0, 180]), (6.0, [-3, 0, 177]), (8.0, [0, 0, 180])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (4.0, [10, 0, 0]), (8.0, [0, 0, 0])])},
            "tail_1": {"rotation": [76, 0, 0]}, "tail_2": {"rotation": [10, 0, 0]},
            "ear_left": {"rotation": kf([(0, [0, 0, 0]), (6.4, [0, 0, 0]), (6.6, [0, 0, -22]), (6.8, [0, 0, 0]), (8.0, [0, 0, 0])])},
            "ear_right": {"rotation": kf([(0, [0, 0, 0]), (3.0, [0, 0, 0]), (3.2, [0, 0, 22]), (3.4, [0, 0, 0]), (8.0, [0, 0, 0])])}}
    blij = lm.blij_hop({"wing_left": {"rotation": kf([(0, [0, 0, 78]), (0.15, [0, 0, -20]), (0.3, [0, 0, 30]), (0.45, [0, 0, -20]), (0.6, [0, 0, 78])])},
                        "wing_right": {"rotation": kf([(0, [0, 0, -78]), (0.15, [0, 0, 20]), (0.3, [0, 0, -30]), (0.45, [0, 0, 20]), (0.6, [0, 0, -78])])},
                        "wing_left_tip": {"rotation": kf([(0, [0, 0, 70]), (0.15, [0, 0, 0]), (0.45, [0, 0, 0]), (0.6, [0, 0, 70])])},
                        "wing_right_tip": {"rotation": kf([(0, [0, 0, -70]), (0.15, [0, 0, 0]), (0.45, [0, 0, 0]), (0.6, [0, 0, -70])])}})
    eet = lm.eet_knik()
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(3.0, idle), "walk": anim(L, walk), "vlieg": anim(F, vlieg), "hang": anim(8.0, hang),
        "blij": anim(0.6, blij, False), "eet": anim(1.0, eet, False)}}


def otter_pinguinguh(atlas):
    rug = plain(OTTER, 8)
    wit = plain(OTTER_WIT, 5)
    voet = plain(OTTER_VOET, 6)

    def kap(W, H, rng, a):                                              # the top of the head: all slate
        pass
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [
                 cube(atlas, [-4, 1.5, -3], [8, 8.5, 6], rug, overrides={"north": lm.buik_op(OTTER, OTTER_WIT), "down": wit}),
                 cube(atlas, [-3, 2.2, -3.6], [6, 6.5, 0.6], wit)]),
             bone("head", "body", [0, 10, 0], [
                 cube(atlas, [-3.5, 10, -3.5], [7, 5.5, 6.5], rug, overrides={"north": otter_gezicht}),
                 cube(atlas, [-1.4, 10.6, -4.2], [2.8, 1.8, 0.8], wit, overrides={"north": lm.neusje(OTTER_WIT, (40, 40, 60), 0.45)})])]
    bones += bd.ears(atlas, OTTER, OOR_BINNEN, 2.2, 14.9, -1.4, 2.2, 2.0)
    for side, sx in (("left", 1), ("right", -1)):
        x = 4.0 if sx > 0 else -5.2
        bones.append(bone(f"flipper_{side}", "body", [sx * 4.0, 9.2, 0], [
            cube(atlas, [x, 4.2, -1.5], [1.2, 5.0, 3.0], plain(OTTER_DONKER, 7), overrides={"west" if sx > 0 else "east": wit})]))
        x2 = 4.0 if sx > 0 else -5.0
        bones.append(bone(f"flipper2_{side}", "body", [sx * 4.0, 4.4, 0.4], [
            cube(atlas, [x2, 2.0, -0.8], [1.0, 2.4, 2.4], plain(OTTER_DONKER, 7))]))
        lx = 0.8 if sx > 0 else -3.4
        bones.append(bone(f"leg_{side}", "body", [lx + 1.3, 1.5, -0.5], [cube(atlas, [lx, 0, -3.6], [2.6, 1.5, 4.2], voet)]))
    bones.append(bone("tail", "body", [0, 2.4, 3], [cube(atlas, [-1.5, 1.6, 3], [3, 1.4, 4.5], plain(OTTER_DONKER, 7))]))
    return geo("otter_pinguinguh", bones, 1.4, 1.4)


def otter_pinguinguh_anims():
    idle = {"flipper_left": {"rotation": kf([(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.2, [0, 0, -28]), (2.4, [0, 0, 0]), (2.6, [0, 0, -28]), (2.8, [0, 0, 0]), (4.0, [0, 0, 0])])},
            "flipper_right": {"rotation": kf([(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.2, [0, 0, 28]), (2.4, [0, 0, 0]), (2.6, [0, 0, 28]), (2.8, [0, 0, 0]), (4.0, [0, 0, 0])])},
            "tail": {"rotation": kf([(0, [0, 0, 0]), (1.0, [0, 12, 0]), (3.0, [0, -12, 0]), (4.0, [0, 0, 0])])},
            "body": {"scale": kf([(0, [1, 1, 1]), (2.0, [1.02, 1.02, 1.02]), (4.0, [1, 1, 1])])}}
    idle.update(lm.oortjes(4.0, 16))
    L = 0.6
    walk = {"body": {"rotation": kf([(0, [0, 0, 8]), (L / 2, [0, 0, -8]), (L, [0, 0, 8])])},
            "leg_left": {"rotation": kf([(0, [22, 0, 0]), (L / 2, [-22, 0, 0]), (L, [22, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [-22, 0, 0]), (L / 2, [22, 0, 0]), (L, [-22, 0, 0])])},
            "flipper_left": {"rotation": kf([(0, [0, 0, -12]), (L / 2, [0, 0, -22]), (L, [0, 0, -12])])},
            "flipper_right": {"rotation": kf([(0, [0, 0, 22]), (L / 2, [0, 0, 12]), (L, [0, 0, 22])])},
            "tail": {"rotation": kf([(0, [0, -10, 0]), (L / 2, [0, 10, 0]), (L, [0, -10, 0])])}}
    # on its belly: the body tips forward around its middle and sinks to the ice, the head looks ahead, the flippers back
    G = 0.8
    glij = {"body": {"rotation": kf([(0, [84, 0, 2]), (G / 2, [84, 0, -2]), (G, [84, 0, 2])]), "position": [0, -3.6, 1.5]},
            "head": {"rotation": [-66, 0, 0]},
            "flipper_left": {"rotation": kf([(0, [35, 0, -30]), (G / 2, [35, 0, -42]), (G, [35, 0, -30])])},
            "flipper_right": {"rotation": kf([(0, [35, 0, 30]), (G / 2, [35, 0, 42]), (G, [35, 0, 30])])},
            "flipper2_left": {"rotation": [20, 0, -25]}, "flipper2_right": {"rotation": [20, 0, 25]},
            "leg_left": {"rotation": kf([(0, [-60, 0, 0]), (G / 2, [-75, 0, 0]), (G, [-60, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [-75, 0, 0]), (G / 2, [-60, 0, 0]), (G, [-75, 0, 0])])},
            "tail": {"rotation": [-25, 0, 0]}}
    blij = lm.blij_hop({"flipper_left": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 0, -60]), (0.2, [0, 0, -10]), (0.3, [0, 0, -60]), (0.45, [0, 0, -10]), (0.6, [0, 0, 0])])},
                        "flipper_right": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 0, 60]), (0.2, [0, 0, 10]), (0.3, [0, 0, 60]), (0.45, [0, 0, 10]), (0.6, [0, 0, 0])])}})
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(4.0, idle), "walk": anim(L, walk), "glij": anim(G, glij),
        "blij": anim(0.6, blij, False), "eet": anim(1.0, lm.eet_knik(), False)}}


def das_molguh(atlas):
    vacht = plain(DAS, 9)
    licht = plain(DAS_LICHT, 6)
    poot = plain(DAS_DONKER, 6)
    klauw = plain(KLAUW, 4)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 5, 0], [
                 cube(atlas, [-5.5, 1.5, -5], [11, 8, 12], vacht, overrides={"up": das_streep(), "down": licht}),
                 cube(atlas, [-4.5, 9.5, -4], [9, 1, 10], vacht, overrides={"up": das_streep()})]),
             bone("head", "body", [0, 5.5, -5], [
                 cube(atlas, [-4.5, 2, -10.5], [9, 7, 5.5], vacht, overrides={"north": das_gezicht, "up": das_streep(), "down": licht})]),
             bone("snoet", "head", [0, 4, -10.5], [
                 cube(atlas, [-2, 2.4, -13], [4, 3.2, 2.5], licht, overrides={"north": lm.neusje(DAS_LICHT, DAS_NEUS, 0.72)})]),
             bone("tail", "body", [0, 5, 7], [cube(atlas, [-1.2, 4, 7], [2.4, 2.4, 2.4], plain(DAS_DONKER, 7))])]
    bones += bd.ears(atlas, DAS_DONKER, OOR_BINNEN, 2.6, 8.6, -8.4, 2.4, 2.2)
    for side, sx in (("left", 1), ("right", -1)):
        x = 3.0 if sx > 0 else -6.6
        klauwen = [cube(atlas, [x + 0.2 + i * 1.2, 0, -11.0], [0.9, 1.0, 1.6], klauw) for i in range(3)]
        bones.append(bone(f"leg_front_{side}", "body", [sx * 4.8, 2.6, -6.5], [cube(atlas, [x, 0, -9.6], [3.6, 2.6, 5.0], poot)] + klauwen))
        bx = 3.0 if sx > 0 else -6.0
        bones.append(bone(f"leg_back_{side}", "body", [sx * 4.5, 2.2, 5.0], [cube(atlas, [bx, 0, 3.4], [3.0, 2.2, 3.4], poot)]))
    return geo("das_molguh", bones, 1.6, 1.2)


def das_molguh_anims():
    snuf = {"snoet": {"position": kf([(0, [0, 0, 0]), (1.6, [0, 0, 0]), (1.7, [0, 0.4, -0.3]), (1.8, [0, 0, 0]), (1.9, [0, 0.4, -0.3]), (2.0, [0, 0, 0]),
                                      (2.1, [0, 0.4, -0.3]), (2.2, [0, 0, 0]), (4.0, [0, 0, 0])])}}
    idle = {**snuf, **lm.oortjes(4.0, 16),
            "head": {"rotation": kf([(0, [0, 0, 0]), (1.0, [4, 10, 0]), (2.0, [-4, 0, 0]), (3.0, [4, -10, 0]), (4.0, [0, 0, 0])])},
            "body": {"scale": kf([(0, [1, 1, 1]), (2.0, [1.015, 1.03, 1.015]), (4.0, [1, 1, 1])])}}
    legs = ["leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"]
    walk = lm.benen(legs, 26, 0.7)
    walk["body"] = {"rotation": kf([(0, [0, 0, 3]), (0.35, [0, 0, -3]), (0.7, [0, 0, 3])])}
    walk["head"] = {"rotation": kf([(0, [6, 6, 0]), (0.35, [6, -6, 0]), (0.7, [6, 6, 0])])}

    def scheppen(L, n):
        stap = L / n
        links = [(i * stap, [-55 if i % 2 else 20, 0, 12]) for i in range(n + 1)]
        rechts = [(i * stap, [20 if i % 2 else -55, 0, -12]) for i in range(n + 1)]
        return {"leg_front_left": {"rotation": kf(links)}, "leg_front_right": {"rotation": kf(rechts)}}
    # nose first into the ground: only a strip of back and the tip of the snout stay above it
    graaf_in = {**scheppen(1.0, 8),
                "body": {"rotation": kf([(0, [0, 0, 0]), (0.4, [24, 0, 0]), (0.8, [10, 0, 0]), (1.0, [0, 0, 0])]),
                         "position": kf([(0, [0, 0, 0]), (0.3, [0, -1.5, -1]), (0.8, [0, -6.5, 0]), (1.0, [0, -7.4, 0])])},
                "head": {"rotation": kf([(0, [0, 0, 0]), (0.4, [18, 0, 0]), (1.0, [-12, 0, 0])])}}
    onder = {**scheppen(1.6, 8),
             "body": {"position": kf([(0, [0, -7.4, 0]), (0.8, [0, -7.0, 0]), (1.6, [0, -7.4, 0])]),
                      "rotation": kf([(0, [0, 0, 2]), (0.8, [0, 0, -2]), (1.6, [0, 0, 2])])},
             "head": {"rotation": [-12, 0, 0]},
             "snoet": {"position": kf([(0, [0, 0, 0]), (0.2, [0, 0.4, -0.3]), (0.4, [0, 0, 0]), (1.6, [0, 0, 0])])}}
    snuffel = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-28, 0, 0]), (0.5, [-28, 14, 0]), (0.8, [-28, -14, 0]), (1.1, [-28, 0, 0]), (1.4, [0, 0, 0])])},
               "snoet": {"position": kf([(t / 10, [0, 0.4 if t % 2 else 0, -0.3 if t % 2 else 0]) for t in range(2, 12)] + [(1.4, [0, 0, 0])])}}
    blij = lm.blij_hop({"tail": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 30, 0]), (0.2, [0, -30, 0]), (0.3, [0, 30, 0]), (0.6, [0, 0, 0])])}})
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(4.0, idle), "walk": anim(0.7, walk), "graaf_in": anim(1.0, graaf_in, False), "onder": anim(1.6, onder),
        "snuffel": anim(1.4, snuffel, False), "blij": anim(0.6, blij, False), "eet": anim(1.0, lm.eet_knik(), False)}}


def schildpad_eendguh(atlas):
    dons = plain(EEND, 8)
    voet = plain(SNAVEL, 6)
    schild_zij, schild_top = schildplaten(True), schildplaten(False)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3, 0], [
                 cube(atlas, [-3, 1.5, -4], [6, 3.5, 8], dons, overrides={"down": plain(EEND_LICHT, 6)})]),
             bone("schild", "body", [0, 5, 0], [
                 cube(atlas, [-4, 4.2, -4.5], [8, 3, 9], schild_zij, overrides={"up": schild_top, "down": plain(SCHILD_RAND, 6)}),
                 cube(atlas, [-3, 7.2, -3.5], [6, 1.2, 7], schild_top)]),
             bone("head", "body", [0, 4.5, -4], [
                 cube(atlas, [-1.5, 3.2, -5.6], [3, 3.2, 2.4], dons),
                 cube(atlas, [-2.6, 5.4, -8.6], [5.2, 4.6, 4.4], dons, overrides={
                     "north": guh_face(EEND, eye_y=0.42, eye_dx=0.26, eye_r=0.19, snoet=False)}),
                 cube(atlas, [-1.6, 5.9, -10.3], [3.2, 1.3, 1.8], plain(SNAVEL, 6), overrides={"up": snavel})]),
             bone("tail", "body", [0, 3.6, 4], [cube(atlas, [-1, 3.0, 4], [2, 1.6, 1.6], plain(EEND_LICHT, 6))])]
    bones += bd.ears(atlas, EEND, OOR_BINNEN, 1.5, 9.6, -6.6, 1.9, 1.8)
    for side, sx in (("left", 1), ("right", -1)):
        x = 0.9 if sx > 0 else -2.7
        bones.append(bone(f"leg_{side}", "body", [x + 0.9, 1.5, 0.2], [cube(atlas, [x, 0, -1.6], [1.8, 1.5, 2.8], voet)]))
    return geo("schildpad_eendguh", bones, 1.2, 1.0)


def schildpad_eendguh_anims():
    idle = {"head": {"rotation": kf([(0, [0, 0, 0]), (1.0, [0, 12, 0]), (1.6, [0, 12, 0]), (2.4, [0, -10, 0]), (3.0, [0, 0, 0])]),
                     "position": kf([(0, [0, 0, 0]), (2.6, [0, 0, 0]), (2.75, [0, 0.4, 0]), (2.9, [0, 0, 0]), (3.0, [0, 0, 0])])},
            "tail": {"rotation": kf([(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.1, [0, 28, 0]), (2.2, [0, -28, 0]), (2.3, [0, 28, 0]), (2.4, [0, 0, 0]), (3.0, [0, 0, 0])])}}
    idle.update(lm.oortjes(3.0, 16))
    L = 0.5
    walk = {"body": {"rotation": kf([(0, [0, 0, 7]), (L / 2, [0, 0, -7]), (L, [0, 0, 7])])},
            "leg_left": {"rotation": kf([(0, [28, 0, 0]), (L / 2, [-28, 0, 0]), (L, [28, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [-28, 0, 0]), (L / 2, [28, 0, 0]), (L, [-28, 0, 0])])},
            "head": {"position": kf([(0, [0, 0, 0]), (L / 2, [0, 0, -0.5]), (L, [0, 0, 0])])},
            "tail": {"rotation": kf([(0, [0, 16, 0]), (L / 2, [0, -16, 0]), (L, [0, 16, 0])])}}
    Z = 0.8
    zwem = {"body": {"position": kf([(0, [0, 0, 0]), (Z / 2, [0, 0.5, 0]), (Z, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 3]), (Z / 2, [0, 0, -3]), (Z, [0, 0, 3])])},
            "leg_left": {"rotation": kf([(0, [50, 0, 0]), (Z / 2, [-30, 0, 0]), (Z, [50, 0, 0])])},
            "leg_right": {"rotation": kf([(0, [-30, 0, 0]), (Z / 2, [50, 0, 0]), (Z, [-30, 0, 0])])},
            "tail": {"rotation": kf([(0, [0, 20, 0]), (Z / 2, [0, -20, 0]), (Z, [0, 20, 0])])}}
    weg = [0.01, 0.01, 0.01]
    schelp = {"head": {"position": [0, -0.6, 4.4], "scale": weg}, "tail": {"scale": weg, "position": [0, 0, -2]},
              "leg_left": {"scale": weg, "position": [0, 1.4, 0]}, "leg_right": {"scale": weg, "position": [0, 1.4, 0]},
              "ear_left": {"scale": weg}, "ear_right": {"scale": weg},
              "body": {"position": [0, -1.4, 0]},
              "schild": {"scale": kf([(0, [1, 1, 1]), (1.0, [1.01, 1.03, 1.01]), (2.0, [1, 1, 1])])}}
    blij = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-25, 0, 0]), (0.45, [-25, 0, 0]), (0.6, [0, 0, 0])])},
            "tail": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 35, 0]), (0.2, [0, -35, 0]), (0.3, [0, 35, 0]), (0.4, [0, -35, 0]), (0.6, [0, 0, 0])])},
            "body": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 1.6, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.0, 0]), (0.6, [0, 0, 0])])}}
    eet = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.2, [38, 0, 0]), (0.35, [28, 0, 0]), (0.5, [38, 0, 0]), (0.65, [28, 0, 0]), (0.8, [38, 0, 0]), (1.0, [0, 0, 0])])},
           "tail": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-20, 0, 0]), (1.0, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": anim(3.0, idle), "walk": anim(L, walk), "zwem": anim(Z, zwem), "schelp": anim(2.0, schelp),
        "blij": anim(0.6, blij, False), "eet": anim(1.0, eet, False)}}


# =====================================================================================================================
# (id, model, animations, texels per model pixel)
DIEREN = [("kruimelkwal", kruimelkwal, kruimelkwal_anims, bd.S),
          ("leegtewalvisguh", leegtewalvisguh, leegtewalvisguh_anims, 8),
          ("vleermuis_lemurguh", vleermuis_lemurguh, vleermuis_lemurguh_anims, bd.S),
          ("otter_pinguinguh", otter_pinguinguh, otter_pinguinguh_anims, bd.S),
          ("das_molguh", das_molguh, das_molguh_anims, bd.S),
          ("schildpad_eendguh", schildpad_eendguh, schildpad_eendguh_anims, bd.S)]
# the animations the Java side plays (KruimelkwalEntity, LeegtewalvisguhEntity, Elementdier and its four)
NODIG = {"kruimelkwal": ["idle", "zwem", "stuiter", "blij"],
         "leegtewalvisguh": ["idle", "zing", "eet"],
         "vleermuis_lemurguh": ["idle", "walk", "vlieg", "hang", "blij", "eet"],
         "otter_pinguinguh": ["idle", "walk", "glij", "blij", "eet"],
         "das_molguh": ["idle", "walk", "graaf_in", "onder", "snuffel", "blij", "eet"],
         "schildpad_eendguh": ["idle", "walk", "zwem", "schelp", "blij", "eet"]}
# the ones with a glow mask (E2ZweversClient gives exactly these an AutoGlowingGeoLayer)
GLOEIT = ("kruimelkwal", "leegtewalvisguh")
# bone name prefixes that belong to the guh model's variants and clothes: not ours to use
VERBODEN = ("pluis", "neck", "outfit_", "teckel", "ender", "koning", "wolk", "zeemeer", "asguh", "pinguh", "balto", "mewtwo", "stitch",
            "korstguh", "duinguh", "kruimelguh", "schimmelguh", "kristalguh", "koorguh")


def maak(naam):
    """(geo, animations, texture, glow mask) of one creature."""
    for id, model, anims, s in DIEREN:
        if id == naam:
            atlas = Vel(id, s)
            return model(atlas), anims(), Image.fromarray(atlas.img), Image.fromarray(atlas.glow)
    raise KeyError(naam)


def build(h):
    A = h.A
    for id, _, _, _ in DIEREN:
        model, anims, tex, glow = maak(id)
        h.w(f"{A}/geckolib/models/entity/{id}.geo.json", model)
        h.w(f"{A}/geckolib/animations/entity/{id}.animation.json", anims)
        h.save(tex, "entity", f"{id}.png")
        if id in GLOEIT:
            h.save(glow, "entity", f"{id}_glowmask.png")


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
        if "body" not in names or "root" not in names:
            problems.append(f"{name}: no root/body bone")
        anims = json.load(open(f"{h.A}/geckolib/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
        for an in wanted:
            if an not in anims:
                problems.append(f"{name}: no animation {an}")
        for an, a in anims.items():
            for bn in a.get("bones", {}):
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
        for f in (f"{name}.png",) + ((f"{name}_glowmask.png",) if name in GLOEIT else ()):
            if not os.path.exists(f"{h.A}/textures/entity/{f}"):
                problems.append(f"{name}: no texture {f}")
    return problems


# =====================================================================================================================
# preview (offline renders, also of single animation poses)
# =====================================================================================================================
def _pose(model, anims, naam, t=0.0):
    """The model with one moment of an animation baked into its bones (rotation and position; scale 0.01 = the bone's
    cubes are left out), so the offline renderer (which only knows the rest pose) can draw it."""
    def waarde(v):
        if isinstance(v, dict):
            keys = sorted(v, key=float)
            best = keys[0]
            for k in keys:
                if float(k) <= t + 1e-6:
                    best = k
            return v[best]
        return v
    out = copy.deepcopy(model)
    g = out["minecraft:geometry"][0]
    bones = anims["animations"][naam]["bones"]
    for b in g["bones"]:
        a = bones.get(b["name"])
        if not a:
            continue
        if "rotation" in a:
            r0 = b.get("rotation", [0, 0, 0])
            b["rotation"] = [x + y for x, y in zip(r0, waarde(a["rotation"]))]
        if "scale" in a and min(waarde(a["scale"])) < 0.05:
            b["cubes"] = []
        if "position" in a:
            dx, dy, dz = waarde(a["position"])
            dx = -dx                                                    # (bedrock animations move x the other way)
            stack = [b["name"]]
            while stack:
                n = stack.pop()
                for c in g["bones"]:
                    if c["name"] == n:
                        c["pivot"] = [c["pivot"][0] + dx, c["pivot"][1] + dy, c["pivot"][2] + dz]
                        for q in c.get("cubes", []):
                            q["origin"] = [q["origin"][0] + dx, q["origin"][1] + dy, q["origin"][2] + dz]
                            if "pivot" in q:
                                q["pivot"] = [q["pivot"][0] + dx, q["pivot"][1] + dy, q["pivot"][2] + dz]
                    elif c.get("parent") == n:
                        stack.append(c["name"])
    return out


POSES = [("vleermuis_lemurguh", "idle"), ("vleermuis_lemurguh", "hang"), ("vleermuis_lemurguh", "vlieg"), ("otter_pinguinguh", "glij"),
         ("das_molguh", "onder"), ("schildpad_eendguh", "schelp"), ("leegtewalvisguh", "zing", 0.3)]


def preview(out):
    """Offline renders into out/: every creature from the front-left and from behind, the poses of POSES, one sheet."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    tiles = []

    def teken(naam, model, tex, label, hoeken=((205, -18), (25, -22))):
        pad = os.path.join(out, f"_{naam}.geo.json")
        json.dump(model, open(pad, "w"))
        q = wr.geo_quads(pad, np.asarray(tex).astype(np.float32))
        os.remove(pad)
        for yaw, pitch in hoeken:
            img = wr.render(q, yaw, pitch, 360, margin=0.06)
            img.save(os.path.join(out, f"{label}_{yaw}.png"))
            tiles.append(img)
    gemaakt = {}
    for id, _, _, _ in DIEREN:
        gemaakt[id] = maak(id)
        model, anims, tex, glow = gemaakt[id]
        tex.save(os.path.join(out, f"{id}_tex.png"))
        glow.save(os.path.join(out, f"{id}_glow.png"))
        teken(id, model, tex, id)
    for p in POSES:
        id, an = p[0], p[1]
        model, anims, tex, _ = gemaakt[id]
        teken(id, _pose(model, anims, an, p[2] if len(p) > 2 else 0.0), tex, f"{id}_{an}", hoeken=((205, -18),))
    sheet = Image.new("RGBA", (360 * 4, 360 * ((len(tiles) + 3) // 4)), (236, 232, 240, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 4) * 360, (i // 4) * 360))
    sheet.save(os.path.join(out, "zwevers_sheet.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
