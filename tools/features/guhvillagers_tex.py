"""
1.7.0: the textures of feature/guhvillagers (called by guhvillagers.py; also on its own):

  - entity/villager/guh_villager.png       model A "Guhkop-villager" (the grown-up guh villager), 128x64 model px, 4x
  - entity/villager/guh_villager_kind.png  model C "Knuffel-villager" (the guh villager child), 128x64 model px, 4x
  - entity/villager/guh_villager_hoedjes.png  the hats of the four new professions, 64x96 model px, 4x
  - entity/villager/profession/{toverguh,vadsmonteur,mijnwerker,kristalslijper}.png  their outfits on the vanilla
    villager layout (64x64 model px, 2x): used by the guh villagers (on model A's body) and by vanilla villagers
  - entity/ijzerguhlem.png                 the IJzerguhlem (model B "Pluizige IJzerguhlem"), 128x128 model px, 4x
  - block/<workstation>_{top,front,side}.png and item/opgepakte_villager.png (16x16)

The UV spots are those of client/GuhVillagerModellen.java (the vanilla villager / iron golem spots for the bodies).
Colours and shapes follow the render scripts (mijnen-ontwerp/_bron/villagers: guhstijl.py, villager_ontwerp.py,
golem_ontwerp.py). Deterministic (fixed seeds).

    PYTHONPATH=tools python tools/features/guhvillagers_tex.py     (from the project root; also writes a contact sheet
                                                                   ../guhvillagers_tex_vel.png, or the path given, to look at)
"""
import os

import numpy as np
from PIL import Image

A_STANDAARD = os.path.join("src", "main", "resources", "assets", "guhs")

# the colours of the real guh (guhstijl.py)
FUR = (199, 163, 210)
FUR_L = (219, 190, 228)
FUR_D = (170, 133, 185)
EAR_IN = (168, 116, 182)
BLUSH = (246, 150, 196)
MOUTH = (128, 74, 128)
PINK = (244, 156, 200)
PINK_L = (252, 205, 226)
PINK_D = (214, 104, 160)
GOLD = (246, 200, 84)
SNOET = (214, 156, 210)


def lichter(c, d=26):
    return tuple(min(255, int(x + d)) for x in c)


def donkerder(c, f=0.82):
    return tuple(int(x * f) for x in c)


# =====================================================================================================================
# a canvas in model pixels with k texels per pixel, and the box layout of a ModelPart cube
# =====================================================================================================================
class Vel:
    def __init__(self, w, h, k):
        self.k = k
        self.a = np.zeros((int(h * k), int(w * k), 4), np.float32)

    def view(self, x, y, w, h):
        k = self.k
        x0, y0 = int(round(x * k)), int(round(y * k))
        x1, y1 = int(round((x + w) * k)), int(round((y + h) * k))
        return self.a[y0:max(y0 + 1, y1), x0:max(x0 + 1, x1)]

    def box(self, u, v, w, h, d, faces):
        """faces: name -> painter(view, k) for top, bottom, west, north, east, south ('around' = the four sides,
        'all' = every face). The spots are ModelPart's: top (u+d, v), bottom (u+d+w, v), west (u, v+d),
        north/front (u+d, v+d), east (u+d+w, v+d), south/back (u+2d+w, v+d)."""
        spots = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "west": (u, v + d, d, h),
                 "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}
        for name, spot in spots.items():
            p = faces.get(name) or (faces.get("around") if name in ("west", "north", "east", "south") else None) or faces.get("all")
            if p is not None:
                p(self.view(*spot), self.k)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        Image.fromarray(np.clip(self.a, 0, 255).astype(np.uint8)).save(path)


_seed = [0]


def _rng():
    _seed[0] += 1
    return np.random.default_rng(1700 + _seed[0])


def fill(t, c, noise=6.0, a=255):
    r = _rng()
    t[..., :3] = np.array(c, np.float32) + r.normal(0, noise, t.shape[:2] + (1,)) if noise else c
    t[..., 3] = a


def fur(c, noise=7.0, specks=0.08):
    """A painter: fur of colour c with soft noise and lighter/darker specks."""
    def p(t, k):
        fill(t, c, noise)
        r = _rng()
        m = r.random(t.shape[:2])
        t[m < specks / 2, :3] = lichter(c, 14)
        t[(m > 1 - specks / 2), :3] = donkerder(c, 0.92)
    return p


def stof(c, noise=4.0):
    """A painter: cloth (less noise)."""
    return lambda t, k: fill(t, c, noise)


def samen(*painters):
    def p(t, k):
        for q in painters:
            q(t, k)
    return p


def rect(t, x, y, w, h, c, a=255):
    H, W = t.shape[:2]
    x0, y0, x1, y1 = max(0, int(round(x))), max(0, int(round(y))), min(W, int(round(x + w))), min(H, int(round(y + h)))
    if x1 > x0 and y1 > y0:
        t[y0:y1, x0:x1, :3] = c
        t[y0:y1, x0:x1, 3] = a


def blend(t, x, y, c, a):
    H, W = t.shape[:2]
    if 0 <= x < W and 0 <= y < H:
        t[y, x, :3] = t[y, x, :3] * (1 - a) + np.array(c) * a
        t[y, x, 3] = 255


def ellipse(t, cx, cy, rx, ry, c, a=1.0):
    for y in range(int(cy - ry - 1), int(cy + ry + 2)):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                blend(t, x, y, c, a)


def eye(t, cx, cy, rx, ry, look=(0.05, 0.1)):
    """A big shiny guh eye (texels): dark rim, blue iris getting lighter downwards, pupil, two shines."""
    top, bot = (44, 92, 178), (110, 196, 238)
    for y in range(int(cy - ry - 1), int(cy + ry + 2)):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            d = (dx * dx + dy * dy) ** 0.5
            if d > 1:
                continue
            if d > 0.78:
                c = (38, 26, 66)
            else:
                q = min(1, max(0, (dy + 0.8) / 1.6))
                c = tuple(int(top[i] * (1 - q) + bot[i] * q) for i in range(3))
                if ((dx - look[0]) ** 2 + (dy - look[1]) ** 2) ** 0.5 < 0.42:
                    c = (24, 26, 62)
            blend(t, x, y, c, 1)
    ellipse(t, cx - rx * 0.32, cy - ry * 0.34, max(0.8, rx * 0.30), max(0.8, ry * 0.30), (255, 255, 255))
    ellipse(t, cx + rx * 0.36, cy + ry * 0.36, max(0.6, rx * 0.14), max(0.6, ry * 0.14), (255, 255, 255))


def mouth_w(t, cx, cy, s, c=MOUTH):
    s = max(1, int(s))
    for i in range(-2 * s, 2 * s + 1):
        j = abs((abs(i) % (2 * s)) - s)
        rect(t, int(cx + i), int(cy + (s - j) * 0.5), 1, max(1, s // 2 + 1), c)


def guh_gezicht(base=FUR, eye_y=0.42, eye_dx=0.25, eye_r=(1.55, 1.75), mouth_y=0.88):
    """A painter: the front of a guh head (fur, blushes, two big eyes, a little w mouth)."""
    def p(t, k):
        fur(base)(t, k)
        H, W = t.shape[:2]
        cx = W / 2
        for s in (-1, 1):
            ellipse(t, cx + s * W * (eye_dx + 0.07), H * (eye_y + 0.24), k * 1.3, k * 0.7, BLUSH, 0.75)
        for s in (-1, 1):
            eye(t, cx + s * W * eye_dx, H * eye_y, eye_r[0] * k, eye_r[1] * k)
        mouth_w(t, cx, H * mouth_y, max(1, k // 2))
    return p


def oor_voor(base=FUR, inner=EAR_IN):
    def p(t, k):
        fur(base)(t, k)
        H, W = t.shape[:2]
        ellipse(t, W / 2, H / 2 + k * 0.2, W * 0.32, H * 0.30, inner)
    return p


def snoet(col=SNOET):
    def p(t, k):
        fill(t, col, 2)
        H, W = t.shape[:2]
        rect(t, 1, 1, max(1, W * 0.45), max(1, k * 0.6), lichter(col))
        rect(t, 0, H - 1, W, 1, donkerder(col, 0.86))
    return p


def hartje(t, cx, cy, sc, c):
    m = [" XX XX ", "XXXXXXX", "XXXXXXX", " XXXXX ", "  XXX  ", "   X   "]
    for j, row in enumerate(m):
        for i, ch in enumerate(row):
            if ch == "X":
                rect(t, cx - 3.5 * sc + i * sc, cy - 3 * sc + j * sc, sc, sc, c)


def ster(t, cx, cy, c, sc=1):
    m = ["  X  ", " XXX ", "XXXXX", " XXX ", " X X "]
    for j, row in enumerate(m):
        for i, ch in enumerate(row):
            if ch == "X":
                rect(t, cx - 2.5 * sc + i * sc, cy - 2.5 * sc + j * sc, sc, sc, c)


def sterretjes(t, c, n, seed, groot=False):
    r = np.random.default_rng(seed)
    H, W = t.shape[:2]
    for _ in range(n):
        x, y = int(r.integers(1, max(2, W - 2))), int(r.integers(1, max(2, H - 2)))
        rect(t, x, y, 1, 1, c)
        if groot or r.random() < 0.4:
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                rect(t, x + dx, y + dy, 1, 1, c)


# =====================================================================================================================
# the outfits (villager_ontwerp.py OUTFITS)
# =====================================================================================================================
class Outfit:
    def __init__(self, robe, trim, sleeve=None, cuff=None, legs=None, belt=None, buckle=GOLD, pattern=None, front=None):
        self.robe, self.trim = robe, trim
        self.sleeve, self.cuff = sleeve or robe, cuff or trim
        self.legs = legs or donkerder(robe, 0.7)
        self.belt, self.buckle, self.pattern, self.front = belt, buckle, pattern, front

    def kant(self, face, belt_y, hoog):
        """A painter for one side of the robe (belt_y and hoog in model px)."""
        def p(t, k):
            fill(t, self.robe, 3.5)
            if self.pattern:
                self.pattern(t, k, face)
            H, W = t.shape[:2]
            rect(t, 0, H - 1.5 * k, W, 1.5 * k, self.trim)                  # hem
            rect(t, 0, H - 1.5 * k, W, 0.5 * k, lichter(self.trim, 25))
            if self.belt:
                rect(t, 0, belt_y * k, W, 1.5 * k, self.belt)
            if face == "north":
                rect(t, 0, 0, W, k, self.trim)                                 # collar + V neck
                cx = W // 2
                for i in range(int(1.5 * k)):
                    rect(t, cx - (1.5 * k - i), k + i, 2 * (1.5 * k - i), 1, self.trim)
                if self.belt:
                    rect(t, cx - k, belt_y * k - 1, 2 * k, 1.5 * k + 2, self.buckle)
                    rect(t, cx - k + 2, belt_y * k + 1, 2 * k - 4, 1.5 * k - 2, self.belt)
                if self.front:
                    self.front(t, k, belt_y, hoog)
        return p


def pat_std(t, k, face):
    H, W = t.shape[:2]
    s = 3 * k
    for y in range(2 * k, H - 3 * k, s):
        for x in range(k + (y // s) % 2 * (s // 2), W - 1, s):
            rect(t, x, y, k, k, (246, 196, 220))


def front_std(t, k, by, hoog):
    hartje(t, t.shape[1] / 2, (by - 3.5) * k, max(1, k // 2), PINK_D)


def pat_tover(t, k, face):
    H, W = t.shape[:2]
    sterretjes(t, (255, 226, 130), max(2, W * H // (30 * k * k)), len(face) + 3)
    sterretjes(t, (190, 230, 255), max(1, W * H // (70 * k * k)), len(face) + 9)


def front_tover(t, k, by, hoog):
    W = t.shape[1]
    cx = W // 2
    rect(t, cx - k, 3 * k, 2 * k, (hoog - 5) * k, (236, 160, 214))     # long sash
    ster(t, cx, (by + 4) * k, GOLD, max(1, k // 2))


def pat_vads(t, k, face):
    if face in ("east", "west", "south"):
        H = t.shape[0]
        rect(t, 0, 0, t.shape[1], 6 * k, (250, 240, 232))


def front_vads(t, k, by, hoog):
    W = t.shape[1]
    cx = W // 2
    mint = (126, 214, 204)
    rect(t, 0, 0, W, (by - 3) * k, (250, 240, 232))                     # shirt
    rect(t, k, (by - 4) * k, W - 2 * k, 4 * k, mint)                     # bib
    rect(t, k, 0, k, (by - 4) * k, mint)                                 # straps
    rect(t, W - 2 * k, 0, k, (by - 4) * k, mint)
    rect(t, k, k, k, k, GOLD)
    rect(t, W - 2 * k, k, k, k, GOLD)
    rect(t, cx - 1.5 * k, (by - 3.5) * k, 3 * k, 3 * k, (98, 186, 178))  # pocket with a vads lightning bolt
    for x, y in [(1, 0), (0, 1), (1, 1), (0, 2), (1, 2), (2, 2), (1, 3), (0, 4)]:
        rect(t, cx - k + x * k * 0.6, (by - 3.3) * k + y * k * 0.55, max(1, k * 0.6), max(1, k * 0.55), (150, 90, 200))


def pat_mijn(t, k, face):
    H, W = t.shape[:2]
    for yy in (14, 17):
        rect(t, 0, yy * k, W, k, (255, 236, 120))


def front_mijn(t, k, by, hoog):
    W = t.shape[1]
    rect(t, W / 2 - 1, 2 * k, 2, (by - 2) * k, (190, 110, 120))          # zip


def front_kristal(t, k, by, hoog):
    W = t.shape[1]
    rect(t, k, (by - 5) * k, W - 2 * k, (hoog - by + 3.5) * k, (236, 244, 252))   # apron
    rect(t, 2 * k, (by + 2) * k, W - 4 * k, 4 * k, (204, 220, 244))                # pocket
    for i, (x, c) in enumerate([(2.5, (255, 150, 210)), (4, (150, 214, 255)), (5.3, (196, 160, 255))]):
        if x * k < W - 2 * k:
            rect(t, x * k, (by + 0.5) * k, max(1, 0.8 * k), 2 * k, c)                 # crystals in the pocket
            rect(t, x * k, (by + 0.5) * k, max(1, 0.8 * k), max(1, 0.5 * k), lichter(c, 40))


OUTFITS = {
    "std": Outfit((250, 232, 238), PINK, belt=PINK_D, pattern=pat_std, front=front_std, legs=(186, 150, 196)),
    "toverguh": Outfit((112, 70, 168), (246, 196, 120), pattern=pat_tover, front=front_tover, legs=(88, 56, 132)),
    "vadsmonteur": Outfit((126, 214, 204), (98, 186, 178), sleeve=(250, 240, 232), cuff=(126, 214, 204), belt=(150, 96, 66),
                          buckle=(220, 220, 230), pattern=pat_vads, front=front_vads, legs=(98, 170, 164)),
    "mijnwerker": Outfit((246, 150, 128), (255, 236, 120), belt=(120, 84, 70), pattern=pat_mijn, front=front_mijn, legs=(150, 110, 100)),
    "kristalslijper": Outfit((176, 160, 232), (236, 244, 252), belt=(150, 196, 236), front=front_kristal, legs=(130, 116, 186)),
}


def mouw(o, hoog):
    """A painter: a sleeve with its cuff at the hand end."""
    def p(t, k):
        fill(t, o.sleeve, 3.5)
        rect(t, 0, t.shape[0] - k, t.shape[1], k, o.cuff)
    return p


def pootjes(o, paw=FUR):
    """A painter: the front of the crossed arms: sleeves at the sides, two paws in the middle."""
    def p(t, k):
        fill(t, o.sleeve, 3.5)
        H, W = t.shape[:2]
        w = W / k
        s = 2 if w >= 7 else 1.2
        rect(t, s * k, 0, 0.6 * k, H, o.cuff)
        rect(t, W - (s + 0.6) * k, 0, 0.6 * k, H, o.cuff)
        m0, m1 = (s + 0.6) * k, W - (s + 0.6) * k
        fur(paw, 4)(t[:, int(m0):int(m1)], k)
        mw = (m1 - m0) / 2
        for i in range(2):
            ellipse(t, m0 + mw * (i + 0.5), H * 0.5, mw * 0.36, H * 0.22, PINK_L, 0.9)
        rect(t, m0 + mw - 0.5, 0, 1, H, donkerder(paw, 0.85))
    return p


def been(o, paw=FUR_L):
    def p(t, k):
        fill(t, o.legs, 3.5)
        rect(t, 0, t.shape[0] - 1.6 * k, t.shape[1], 1.6 * k, paw)
    return p


def villager_lijf(v, o, paw=FUR, met_lijf=True):
    """The vanilla villager body spots (64x64): body, robe (jacket), sleeves, crossed-arm bar, legs."""
    if met_lijf:
        v.box(16, 20, 8, 12, 6, {"all": stof(o.robe)})
    v.box(0, 38, 8, 20, 6, {**{f: o.kant(f, 11, 20) for f in ("north", "south", "east", "west")},
                             "top": stof(o.robe), "bottom": stof(o.trim)})
    v.box(44, 22, 4, 8, 4, {"around": mouw(o, 8), "top": stof(o.sleeve), "bottom": fur(paw, 4)})
    v.box(40, 38, 8, 4, 4, {"north": pootjes(o, paw), "all": stof(o.sleeve)})
    v.box(0, 22, 4, 12, 4, {"around": been(o), "top": stof(o.legs), "bottom": fur(FUR_L, 4)})


# =====================================================================================================================
# model A, model C, the hats
# =====================================================================================================================
def guh_villager():
    v = Vel(128, 64, 4)
    villager_lijf(v, OUTFITS["std"])
    v.box(64, 0, 12, 9, 8, {"all": fur(FUR)})
    v.box(64, 17, 10, 11, 8, {"all": fur(FUR)})
    v.box(64, 36, 10, 9, 10, {"all": fur(FUR), "north": guh_gezicht(eye_y=0.42, eye_dx=0.25, eye_r=(1.55, 1.75), mouth_y=0.88)})
    v.box(104, 0, 2, 2.5, 1.5, {"all": snoet()})
    v.box(104, 6, 7, 5, 1, {"all": fur(FUR), "north": oor_voor()})
    v.box(104, 12, 5, 7, 1.2, {"all": fur(FUR), "north": oor_voor()})
    v.box(104, 20, 2, 2, 4, {"all": fur(FUR)})
    return v


def guh_villager_kind():
    o = OUTFITS["std"]
    v = Vel(128, 64, 4)
    v.box(0, 0, 14, 10, 9, {"all": fur(FUR)})
    v.box(0, 19, 12, 12, 9, {"all": fur(FUR)})
    v.box(0, 40, 12, 10, 11, {"all": fur(FUR), "north": guh_gezicht(eye_y=0.46, eye_dx=0.25, eye_r=(1.9, 2.1), mouth_y=0.86)})
    v.box(46, 0, 2.5, 2, 1, {"all": snoet()})
    v.box(46, 3, 8, 6, 1, {"all": fur(FUR), "north": oor_voor()})
    v.box(46, 10, 6, 8, 1.2, {"all": fur(FUR), "north": oor_voor()})
    v.box(64, 0, 8, 9, 6, {**{f: o.kant(f, 4.5, 9) for f in ("north", "south", "east", "west")}, "top": stof(o.robe), "bottom": stof(o.trim)})

    def rok(t, k):
        fill(t, o.robe, 3.5)
        pat_std(t, k, "rok")
        rect(t, 0, t.shape[0] - 1.2 * k, t.shape[1], 1.2 * k, o.trim)
    v.box(64, 15, 10, 3, 8, {"around": rok, "top": stof(o.robe), "bottom": stof(o.trim)})
    v.box(64, 26, 2.5, 5, 2.5, {"around": mouw(o, 5), "top": stof(o.sleeve), "bottom": fur(FUR, 4)})
    v.box(76, 26, 5.5, 2.5, 2.5, {"north": pootjes(o), "all": stof(o.sleeve)})
    v.box(64, 34, 3, 3, 3, {"around": been(o), "top": stof(o.legs), "bottom": fur(FUR_L, 4)})
    v.box(78, 34, 1.5, 1.5, 3.5, {"all": fur(FUR)})
    return v


def hoedjes():
    v = Vel(64, 96, 4)
    paars, paars_l = (124, 76, 178), (156, 112, 210)

    def paars_ster(groot=False):
        def p(t, k):
            fill(t, paars, 3)
            sterretjes(t, (255, 226, 130), max(1, t.shape[0] * t.shape[1] // 90), t.shape[1] + t.shape[0])
            if groot:
                ster(t, t.shape[1] / 2, t.shape[0] / 2 + 1, GOLD, 2)
        return p
    v.box(0, 0, 14, 1, 12, {"all": stof(paars_l)})
    v.box(0, 13, 8, 1, 8, {"all": stof(GOLD, 6)})
    v.box(32, 13, 8, 4, 8, {"around": paars_ster(), "north": paars_ster(True), "top": stof(paars)})
    v.box(0, 25, 6, 3, 6, {"all": paars_ster()})
    v.box(24, 25, 4, 3, 4, {"all": paars_ster()})
    v.box(40, 25, 2, 3, 2, {"all": stof(paars)})
    v.box(48, 25, 2, 2, 2, {"all": stof((255, 214, 120), 5)})
    # the miner's helmet
    geel, geel_d = (252, 206, 82), (226, 168, 56)
    v.box(0, 34, 13, 3.5, 11, {"all": fur(geel, 5, 0.04)})
    v.box(0, 49, 9, 1.2, 7, {"all": stof(lichter(geel, 14)), "top": samen(stof((255, 222, 120)), lambda t, k: rect(t, t.shape[1] / 2 - 0.7 * k, 0, 1.4 * k, t.shape[0], PINK))})
    v.box(32, 49, 13, 0.8, 1.5, {"all": stof(geel_d)})

    def lamp(t, k):
        fill(t, (150, 150, 170), 3)
        rect(t, 0.5 * k, 0.4 * k, t.shape[1] - k, t.shape[0] - 0.8 * k, (255, 246, 190))
        rect(t, 0.8 * k, 0.6 * k, k, k * 0.6, (255, 255, 255))
    v.box(32, 52, 3.5, 2.8, 1.2, {"all": stof((150, 150, 170)), "north": lamp})
    # the vadsmonteur's goggles
    v.box(0, 58, 12, 1.5, 10, {"all": stof((120, 74, 60), 5)})

    def glas(t, k):
        fill(t, (214, 140, 80), 4)
        rect(t, 0.5 * k, 0.5 * k, t.shape[1] - k, t.shape[0] - k, (120, 230, 236))
        rect(t, 0.8 * k, 0.8 * k, 0.8 * k, 0.8 * k, (255, 255, 255))
    v.box(44, 58, 3.5, 3.5, 1.5, {"all": stof((214, 140, 80), 4), "north": glas})
    # the kristalslijper's headband, crystal and loupe
    v.box(0, 70, 12, 1.25, 10, {"all": stof((150, 196, 236))})

    def kristal(t, k):
        fill(t, (255, 150, 210), 3)
        rect(t, 0, 0, t.shape[1], k, (255, 200, 236))
    v.box(44, 70, 1.5, 3, 1.5, {"all": kristal})

    def loep(t, k):
        fill(t, (226, 176, 92), 3)
        rect(t, 0.4 * k, 0.4 * k, t.shape[1] - 0.8 * k, t.shape[0] - 0.8 * k, (176, 236, 255))
        rect(t, 0.6 * k, 0.6 * k, 0.6 * k, 0.6 * k, (255, 255, 255))
    v.box(52, 70, 2.5, 2.5, 1.5, {"all": stof((226, 176, 92)), "north": loep})
    return v


def beroep(naam):
    """A profession outfit on the vanilla villager layout (64x64 model px, 2 texels per pixel), head spots empty."""
    v = Vel(64, 64, 2)
    villager_lijf(v, OUTFITS[naam])
    return v


# =====================================================================================================================
# the IJzerguhlem
# =====================================================================================================================
IJZER, IJZER_D, IJZER_L = (198, 180, 224), (150, 120, 178), (226, 212, 244)
PLUIS, PLUIS_L = (250, 180, 214), (255, 214, 234)


def ijzer(ranken=True, seed=0):
    """A painter: lavender iron with plate seams and rivets, and (ranken) pink fluff vines with little flowers."""
    def p(t, k):
        fill(t, IJZER, 5)
        H, W = t.shape[:2]
        r = np.random.default_rng(seed + W * 7 + H)
        for y in range(0, H, 6 * k):                       # plate seams
            rect(t, 0, y, W, 1, IJZER_D)
            rect(t, 0, y + 1, W, 1, IJZER_L)
        for y in range(3 * k, H, 6 * k):
            for x in range(2 * k, W - k, 5 * k):
                rect(t, x, y, k * 0.75, k * 0.75, IJZER_L)
                rect(t, x + k * 0.5, y + k * 0.5, k * 0.25, k * 0.25, IJZER_D)
        if ranken and W >= 3 * k:
            for _ in range(max(1, W // (5 * k))):
                x = r.uniform(k, W - k)
                y = 0.0
                while y < H:
                    rect(t, x, y, k * 0.75, k * 0.75, PLUIS if r.random() < 0.65 else PLUIS_L)
                    if r.random() < 0.10:
                        rect(t, x - k * 0.25, y, k * 1.25, k * 1.25, (255, 246, 160))
                    x = min(W - k, max(0, x + r.uniform(-0.6, 0.6) * k))
                    y += k * 0.6
    return p


def ijzerguhlem():
    v = Vel(128, 128, 4)
    # the vanilla iron golem spots: body, waist, arms, legs
    v.box(0, 40, 18, 12, 11, {"all": ijzer(seed=1)})
    v.box(0, 70, 9, 5, 6, {"all": ijzer(False, seed=2)})
    v.box(60, 21, 4, 30, 6, {"all": ijzer(seed=3)})
    v.box(60, 58, 4, 30, 6, {"all": ijzer(seed=4)})
    v.box(37, 0, 6, 16, 5, {"all": ijzer(seed=5)})
    v.box(60, 0, 6, 16, 5, {"all": ijzer(seed=6)})
    # the round guh head
    v.box(0, 81, 12, 9, 8, {"all": fur(FUR)})
    v.box(0, 98, 10, 11, 8, {"all": fur(FUR)})
    v.box(82, 0, 10, 9, 10, {"all": fur(FUR), "north": guh_gezicht(eye_y=0.42, eye_dx=0.25, eye_r=(1.55, 1.75), mouth_y=0.86)})
    v.box(82, 19, 2, 2.5, 1.5, {"all": snoet()})
    v.box(90, 19, 7, 5, 1, {"all": fur(FUR), "north": oor_voor()})
    v.box(106, 19, 5, 7, 1.2, {"all": fur(FUR), "north": oor_voor()})
    for i, c in enumerate([(255, 246, 150), (255, 255, 255), (250, 150, 200), (196, 226, 255)]):
        v.box(i * 6, 117, 1.5, 1, 1.5, {"all": stof(c, 3)})
    # the fluffy collar, wrists, knees
    v.box(82, 28, 4.5, 3, 3, {"all": fur(PLUIS, 9, 0.2)})
    v.box(82, 34, 3, 3, 4.5, {"all": fur(PLUIS_L, 9, 0.2)})
    v.box(82, 42, 5, 3, 7, {"all": fur(PLUIS, 9, 0.2)})
    v.box(82, 52, 5, 2, 7, {"all": fur(PLUIS_L, 9, 0.2)})
    v.box(82, 61, 7, 2, 6, {"all": fur(PLUIS, 9, 0.2)})
    # the bunch of pluisgras: stems and fluffy heads
    v.box(110, 28, 1, 9, 1, {"all": stof((226, 120, 172), 4)})
    v.box(110, 40, 3, 3, 3, {"all": fur((255, 214, 236), 6, 0.25)})
    return v


# =====================================================================================================================
# 16x16: the four workstations and the picked-up villager item
# =====================================================================================================================
def t16(c, noise=5.0, seed=0):
    r = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.float32)
    a[..., :3] = np.array(c, np.float32) + r.normal(0, noise, (16, 16, 1))
    a[..., 3] = 255
    return a


def planken(c, seed, rand=None):
    a = t16(c, 4, seed)
    for y in (3, 7, 11, 15):
        rect(a, 0, y, 16, 1, donkerder(c, 0.8))
    r = np.random.default_rng(seed)
    for y in (0, 4, 8, 12):
        rect(a, int(r.integers(2, 14)), y, 1, 3, donkerder(c, 0.86))
    if rand:
        for x0, y0, w, h in ((0, 0, 16, 1), (0, 15, 16, 1), (0, 0, 1, 16), (15, 0, 1, 16)):
            rect(a, x0, y0, w, h, rand)
    return a


def werkplekken():
    uit = {}
    # Toverguh-boekenstandaard: purple wood, an open pink spell book with a heart on top, little pastel books on the sides
    hout = (126, 84, 168)
    top = planken(hout, 11, donkerder(hout, 0.7))
    rect(top, 2, 3, 12, 10, (252, 246, 232))
    rect(top, 7, 3, 2, 10, (226, 214, 196))
    for y in range(5, 12, 2):
        rect(top, 3, y, 3, 1, (200, 180, 210))
    hartje(top, 11.5, 7.5, 1, PINK_D)
    rect(top, 2, 13, 12, 1, (236, 110, 170))
    voor = planken(hout, 12, donkerder(hout, 0.7))
    rect(voor, 3, 3, 10, 8, (236, 110, 170))
    rect(voor, 4, 4, 8, 6, (246, 150, 196))
    hartje(voor, 8, 7.5, 1, GOLD)
    ster(voor, 8, 13, GOLD)
    zij = planken(hout, 13, donkerder(hout, 0.7))
    for i, c in enumerate([(255, 196, 220), (196, 226, 255), (255, 236, 150), (200, 240, 210), (230, 200, 255)]):
        rect(zij, 2 + i * 2.4, 4, 2, 7, c)
        rect(zij, 2 + i * 2.4, 4, 2, 1, lichter(c, 20))
    uit["toverguh_boekenstandaard"] = (top, voor, zij)
    # Guhrad-werkbank: a mint workbench with a little guh wheel on top, drawers and a vads lightning bolt in front
    mint, mint_d = (126, 214, 204), (88, 170, 162)
    top = planken((214, 170, 120), 21, (150, 110, 80))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.2 < d < 6.0:
                blend(top, x, y, PINK, 1)
            elif d <= 1.4:
                blend(top, x, y, PINK_D, 1)
    for x, y in ((7, 2), (7, 13), (2, 7), (13, 7)):
        rect(top, x, y, 2, 1, (250, 240, 232))
    voor = t16(mint, 4, 22)
    rect(voor, 0, 0, 16, 3, (214, 170, 120))
    rect(voor, 0, 3, 16, 1, (150, 110, 80))
    for y0 in (5, 10):
        rect(voor, 2, y0, 12, 4, mint_d)
        rect(voor, 7, y0 + 1, 2, 1, GOLD)
    for x, y in [(9, 5), (8, 6), (9, 6), (8, 7), (9, 7), (10, 7), (9, 8)]:
        rect(voor, x - 4, y + 5, 1, 1, (150, 90, 200))
    zij = t16(mint, 4, 23)
    rect(zij, 0, 0, 16, 3, (214, 170, 120))
    rect(zij, 0, 3, 16, 1, (150, 110, 80))
    rect(zij, 3, 6, 1, 7, (196, 206, 220))
    rect(zij, 2, 5, 3, 2, (196, 206, 220))
    rect(zij, 3, 12, 1, 2, PINK)
    rect(zij, 9, 6, 4, 4, (98, 186, 178))
    uit["guhrad_werkbank"] = (top, voor, zij)
    # Ertsenkist: a wooden chest full of ore chunks, iron bands, a pink pickaxe on the front
    bruin = (170, 112, 74)
    top = planken(bruin, 31, (110, 74, 54))
    r = np.random.default_rng(32)
    rect(top, 2, 2, 12, 12, (92, 70, 74))
    for _ in range(26):
        c = [(240, 200, 180), (90, 90, 96), (255, 220, 120), (120, 200, 255), (250, 150, 205), (196, 160, 255), (230, 120, 110)][int(r.integers(0, 7))]
        x, y = int(r.integers(2, 13)), int(r.integers(2, 13))
        rect(top, x, y, 2, 2, c)
        rect(top, x, y, 1, 1, lichter(c, 30))
    voor = planken(bruin, 33, (110, 74, 54))
    for y0 in (2, 12):
        rect(voor, 0, y0, 16, 2, (176, 168, 190))
    rect(voor, 7, 6, 2, 3, (176, 168, 190))
    rect(voor, 7, 7, 2, 1, (80, 70, 90))
    for i in range(6):
        rect(voor, 3 + i, 10 - i, 1, 1, (176, 116, 76))
    rect(voor, 7, 3, 4, 1, (250, 150, 205))
    rect(voor, 10, 4, 1, 2, (250, 150, 205))
    zij = planken(bruin, 34, (110, 74, 54))
    for y0 in (2, 12):
        rect(zij, 0, y0, 16, 2, (176, 168, 190))
    uit["ertsenkist"] = (top, voor, zij)
    # Kristalslijpsteen: a pink crystal grinding wheel on a viltsteen base
    steen = (196, 150, 176)
    top = t16(steen, 5, 41)
    rect(top, 2, 5, 12, 6, (255, 170, 214))
    rect(top, 2, 7, 12, 2, (255, 210, 236))
    rect(top, 0, 7, 2, 2, (150, 110, 80))
    rect(top, 14, 7, 2, 2, (150, 110, 80))
    voor = t16(steen, 5, 42)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 6.5) ** 2) ** 0.5
            if d <= 5.6:
                blend(voor, x, y, (255, 170, 214) if d > 1.6 else (150, 110, 80), 1)
            if 4.4 < d <= 5.6:
                blend(voor, x, y, (226, 120, 180), 1)
    for x, y in ((5, 4), (9, 8), (8, 3)):
        rect(voor, x, y, 1, 1, (255, 255, 255))
    rect(voor, 0, 13, 16, 3, donkerder(steen, 0.8))
    zij = t16(steen, 5, 43)
    rect(zij, 0, 13, 16, 3, donkerder(steen, 0.8))
    rect(zij, 6, 4, 4, 4, (150, 110, 80))
    for x, y, c in ((2, 9, (255, 150, 210)), (11, 9, (150, 214, 255)), (12, 3, (196, 160, 255))):
        rect(zij, x, y, 2, 3, c)
        rect(zij, x, y, 2, 1, lichter(c, 40))
    uit["kristalslijpsteen"] = (top, voor, zij)
    return uit


def opgepakte_villager():
    """The item: a pink bundle with a guh villager head peeking out."""
    a = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            if ((x - 7.5) / 6.5) ** 2 + ((y - 11.5) / 4.6) ** 2 <= 1:
                blend(a, x, y, PINK if (x + y) % 5 else PINK_L, 1)
    rect(a, 6, 7, 4, 1, PINK_D)
    for y in range(16):
        for x in range(16):
            if 2 <= y <= 9 and ((x - 7.5) / 5.2) ** 2 + ((y - 5.5) / 4.0) ** 2 <= 1:
                blend(a, x, y, FUR, 1)
    for x, y in ((2, 1), (3, 1), (12, 1), (13, 1), (2, 2), (13, 2)):
        rect(a, x, y, 1, 1, FUR_D)
    for x in (5, 9):
        rect(a, x, 4, 2, 3, (44, 92, 178))
        rect(a, x, 4, 1, 1, (255, 255, 255))
    rect(a, 7, 6, 2, 2, SNOET)
    rect(a, 4, 7, 1, 1, BLUSH)
    rect(a, 11, 7, 1, 1, BLUSH)
    return a


def _png(a, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).save(path)


def textures(h):
    """Writes every texture into h.A (assets/guhs)."""
    A = h.A
    E = os.path.join(A, "textures", "entity")
    guh_villager().save(os.path.join(E, "villager", "guh_villager.png"))
    guh_villager_kind().save(os.path.join(E, "villager", "guh_villager_kind.png"))
    hoedjes().save(os.path.join(E, "villager", "guh_villager_hoedjes.png"))
    for naam in ("toverguh", "vadsmonteur", "mijnwerker", "kristalslijper"):
        beroep(naam).save(os.path.join(E, "villager", "profession", naam + ".png"))
    ijzerguhlem().save(os.path.join(E, "ijzerguhlem.png"))
    for naam, (top, voor, zij) in werkplekken().items():
        _png(top, os.path.join(A, "textures", "block", naam + "_top.png"))
        _png(voor, os.path.join(A, "textures", "block", naam + "_front.png"))
        _png(zij, os.path.join(A, "textures", "block", naam + "_side.png"))
    _png(opgepakte_villager(), os.path.join(A, "textures", "item", "opgepakte_villager.png"))


if __name__ == "__main__":
    import types
    textures(types.SimpleNamespace(A=A_STANDAARD))
    # a contact sheet to look at
    E = os.path.join(A_STANDAARD, "textures", "entity")
    namen = [os.path.join(E, "villager", f) for f in ("guh_villager.png", "guh_villager_kind.png", "guh_villager_hoedjes.png")] + \
            [os.path.join(E, "villager", "profession", n + ".png") for n in ("toverguh", "vadsmonteur", "mijnwerker", "kristalslijper")] + \
            [os.path.join(E, "ijzerguhlem.png")]
    ims = [Image.open(n).convert("RGBA") for n in namen]
    W = sum(min(512, i.width) for i in ims) + 10 * len(ims)
    vel = Image.new("RGBA", (W, 520), (60, 50, 70, 255))
    x = 0
    for im in ims:
        vel.alpha_composite(im, (x, 0))
        x += im.width + 10
    import sys
    vel.save(sys.argv[1] if len(sys.argv) > 1 else os.path.join("..", "guhvillagers_tex_vel.png"))
    print("guhvillagers_tex: written")
