"""
3.0 (Guhverhalen), slice vogels: the GeckoLib models, textures and animations of the four little birds of the Guhmensie.
They are birds, not guhs, but every one has a guh twist: the big glossy guh eyes (dark pupil, blue/amber ring, two white
shines), a pink blush and two little round guh ears on top of the head.

  pluisvinkje    a round pink-white ball of fluff with a little tuft; flies in flocks
  kaasmeesje     a great tit in cheese colours: cheese-yellow breast with a cheese stripe (with holes), a lilac guh cap,
                 white cheeks, a mossy back and blue-grey wings; hangs upside down under leaves
  guh_uiltje     a round cocoa-pink owl whose ear tufts are guh ears; a heart-shaped face with huge amber eyes that glow at
                 night (guh_uiltje_glowmask.png); the head turns all the way round (the renderer)
  zeemeeuwtje    a white gull with grey wings (black tips with white dots), a yellow beak with a red spot that opens
                 ("Mijn! Mijn!"), a cheeky half-lidded look and pink webbed feet

Every model: bones root > body > head (DefaultedEntityGeoModel turns "head"), ear_left/ear_right, beak (+ beak_lower for the
gull), wing_left/wing_right, tail, leg_left/leg_right. Each cube face gets its own painted patch (8 px per model unit, so
the eyes are crisp) in a packed atlas. Every texture also has a `<name>_dicht.png` (eyes closed: blinking, sleeping).

Animations (animation.<name>.<x>): idle, hop, fly, peck, + kaasmeesje hang, guh_uiltje slaap, zeemeeuwtje roep / glide.
build(h) writes all of it, check(h) is the self-check (bones, eyes on the face, texture sizes, animation bone names).
"""
import math
import os

import numpy as np
from PIL import Image

PX = 8               # texture pixels per model unit
SW = (5, 7)          # (unused, kept for reference)
BIRDS = ("pluisvinkje", "kaasmeesje", "guh_uiltje", "zeemeeuwtje")
FACES = ("north", "south", "east", "west", "up", "down")


# =====================================================================================================================
# the atlas: every cube face gets its own painted patch
# =====================================================================================================================
class Atlas:
    def __init__(self, name, width=64, height=64, seed=0):
        self.name = name
        self.W, self.H = width, height                     # in model units
        self.img = np.zeros((height * PX, width * PX, 4), np.float32)
        self.glow = np.zeros_like(self.img)
        self.x = self.y = self.row = 0
        self.rng = np.random.default_rng(seed)
        self.patches = []                                  # (x, y, w, h, painter, face, glow painter)

    def alloc(self, w, h):
        w, h = max(1, w), max(1, h)
        if self.x + w + 1 > self.W * PX:
            self.x, self.y, self.row = 0, self.y + self.row + 1, 0
        if self.y + h > self.H * PX:
            raise SystemExit(f"vogels models: the {self.name} atlas is full")
        at = (self.x, self.y)
        self.x += w + 1
        self.row = max(self.row, h)
        return at

    def face(self, painter, w_units, h_units, face, glow=None):
        w, h = max(1, round(w_units * PX)), max(1, round(h_units * PX))
        x, y = self.alloc(w, h)
        self.patches.append((x, y, w, h, painter, face, glow))
        return {"uv": [x / PX, y / PX], "uv_size": [w / PX, h / PX]}

    def paint(self, closed=False):
        """Paints every patch (closed: the eyes are shut). Deterministic: every patch has its own seed."""
        img = np.zeros_like(self.img)
        glow = np.zeros_like(self.img)
        for i, (x, y, w, h, painter, face, gl) in enumerate(self.patches):
            rng = np.random.default_rng(1000 * i + 7)
            px = painter(w, h, face, rng, closed)
            img[y:y + h, x:x + w] = px
            if gl is not None and not closed:
                glow[y:y + h, x:x + w] = gl(w, h, face, np.random.default_rng(1000 * i + 7))
        return img, glow

    def save(self, h, glowmask=False):
        img, glow = self.paint(False)
        h.save(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)), "entity", f"{self.name}.png")
        dicht, _ = self.paint(True)
        h.save(Image.fromarray(np.clip(dicht, 0, 255).astype(np.uint8)), "entity", f"{self.name}_dicht.png")
        if glowmask:
            h.save(Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8)), "entity", f"{self.name}_glowmask.png")


def cube(atlas, origin, size, paint, glow=None, inflate=None):
    """A cube; paint is a painter or {face: painter, "*": default}. Zero-thick faces are left out."""
    sx, sy, sz = size
    dims = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
    uv = {}
    for f in FACES:
        w_, h_ = dims[f]
        if w_ <= 0 or h_ <= 0:
            continue
        p = paint.get(f, paint.get("*")) if isinstance(paint, dict) else paint
        if p is None:
            continue
        g = glow.get(f) if isinstance(glow, dict) else None
        uv[f] = atlas.face(p, w_, h_, f, g)
    c = {"origin": [round(o, 3) for o in origin], "size": [round(s, 3) for s in size], "uv": uv}
    if inflate:
        c["inflate"] = inflate
    return c


def bone(name, parent, pivot, cubes=(), rotation=None):
    b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": list(cubes)}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    return b


def write_geo(h, name, atlas, bones, width, height):
    h.w(f"{h.A}/geckolib/models/entity/{name}.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{name}", "texture_width": atlas.W, "texture_height": atlas.H,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]})


# =====================================================================================================================
# painters: painter(w, h, face, rng, closed) -> (h, w, 4) float array
# =====================================================================================================================
def rgba(c, a=255):
    return np.array(tuple(c[:3]) + (a,), np.float32)


def solid(col, var=6):
    def p(w, h, face, rng, closed):
        a = np.empty((h, w, 4), np.float32)
        a[...] = rgba(col)
        a[..., :3] += rng.integers(-var, var + 1, (h, w, 1))
        return np.clip(a, 0, 255)
    return p


def fluff(col, light, var=6, n=None, rmax=3):
    """Soft feathers: the colour with little lighter tufts."""
    def p(w, h, face, rng, closed):
        a = solid(col, var)(w, h, face, rng, closed)
        yy, xx = np.ogrid[:h, :w]
        for _ in range(n if n is not None else max(2, w * h // 30)):
            x, y, r = rng.integers(0, w), rng.integers(0, h), rng.integers(1, rmax + 1)
            m = (xx - x) ** 2 + (yy - y) ** 2 < r * r
            a[m, :3] = a[m, :3] * 0.5 + np.array(light, np.float32) * 0.5
        return a
    return p


def vgradient(top, bottom, split=0.5, soft=0.25, var=6, tufts=None):
    """Top colour above, bottom colour below (a soft, wavy border at `split` of the height)."""
    def p(w, h, face, rng, closed):
        a = np.empty((h, w, 4), np.float32)
        a[..., 3] = 255
        for y in range(h):
            for x in range(w):
                wave = 0.06 * math.sin(x * 1.3) + 0.04 * math.sin(x * 0.55 + 1)
                t = np.clip(((y + 0.5) / h - split - wave) / max(soft, 1e-3) + 0.5, 0, 1)
                a[y, x, :3] = np.array(top, np.float32) * (1 - t) + np.array(bottom, np.float32) * t
        a[..., :3] += rng.integers(-var, var + 1, (h, w, 1))
        if tufts:
            yy, xx = np.ogrid[:h, :w]
            for _ in range(max(2, w * h // 36)):
                x, y, r = rng.integers(0, w), rng.integers(0, h), rng.integers(1, 3)
                m = (xx - x) ** 2 + (yy - y) ** 2 < r * r
                a[m, :3] = a[m, :3] * 0.6 + np.array(tufts, np.float32) * 0.4
        return np.clip(a, 0, 255)
    return p


def rows_of_feathers(col, dark, light, var=5):
    """Scalloped feather rows (little 'u' shapes): wings, backs, the owl's body."""
    def p(w, h, face, rng, closed):
        a = solid(col, var)(w, h, face, rng, closed)
        step = max(4, PX // 2 + 1)
        for row, y0 in enumerate(range(1, h, step)):
            off = (row % 2) * step // 2
            for x0 in range(-off, w, step):
                for i in range(step):
                    x = x0 + i
                    y = y0 + int(round(1.6 * math.sin(math.pi * i / max(1, step - 1))))
                    if 0 <= x < w and 0 <= y < h:
                        a[y, x, :3] = dark
                    if 0 <= x < w and 0 <= y - 1 < h and 1 <= i < step - 1:
                        a[y - 1, x, :3] = a[y - 1, x, :3] * 0.5 + np.array(light, np.float32) * 0.5
        return a
    return p


def tipped(base, tip, along="x", frac=0.3, spots=None, var=5, reverse=False):
    """A wing / tail feather: `base` with the last `frac` in `tip` (with a few white spots on the tip: the gull)."""
    def p(w, h, face, rng, closed):
        a = rows_of_feathers(base, tuple(max(0, c - 28) for c in base), tuple(min(255, c + 25) for c in base), var)(w, h, face, rng, closed)
        n = w if along == "x" else h
        k = int(round(n * frac))
        rev = reverse != (along == "x" and face == "east")      # (the east face runs back-to-front: its tip is on the left)
        for i in range(k):
            j = (n - 1 - i) if not rev else i
            if along == "x":
                a[:, j, :3] = np.array(tip, np.float32) + rng.integers(-var, var + 1, (h, 1))
            else:
                a[j, :, :3] = np.array(tip, np.float32) + rng.integers(-var, var + 1, (w, 1))
        if spots and k >= 3:
            for s in range(2):
                j = (n - 1 - k // 2 - s * 2) if not rev else (k // 2 + s * 2)
                c = (h // 2, j) if along == "x" else (j, w // 2)
                yy, xx = np.ogrid[:h, :w]
                m = (yy - c[0]) ** 2 + (xx - c[1]) ** 2 <= 1.2
                a[m, :3] = spots
        return a
    return p


def rounded(painter, corner=0.35):
    """Cuts the corners off (transparent): round ears, tail ends, face discs."""
    def p(w, h, face, rng, closed):
        a = painter(w, h, face, rng, closed)
        r = corner * min(w, h)
        for y in range(h):
            for x in range(w):
                dx = max(0.0, r - (x + 0.5), (x + 0.5) - (w - r))
                dy = max(0.0, r - (y + 0.5), (y + 0.5) - (h - r))
                if dx * dx + dy * dy > r * r:
                    a[y, x, 3] = 0
        return a
    return p


def ear(outer, inner):
    """A round guh ear seen from the front: a disc with the pink inner ear (the back is plain)."""
    def p(w, h, face, rng, closed):
        a = solid(outer, 5)(w, h, face, rng, closed)
        cx, cy = (w - 1) / 2, (h - 1) / 2
        R = min(w, h) / 2
        for y in range(h):
            for x in range(w):
                d = math.hypot(x - cx, (y - cy) * 1.05)
                if d > R:
                    a[y, x, 3] = 0
                elif face in ("north",) and d < R * 0.62 and y > cy - R * 0.5:
                    a[y, x, :3] = inner
        return a
    return p


def eye(a, cx, cy, r, iris=((120, 220, 240), (40, 110, 220)), lid=0.0, closed=False, fur=None, lash=(20, 16, 28)):
    """The glossy guh eye (guh.png): a big dark pupil, a light-to-deep ring on its lower half, a lash arc on top, a big
    white shine top-left and a small one bottom-right. lid: 0..1, a cheeky half-closed top lid (the gull). Closed: a soft
    'u' curve with three little lashes, painted over the fur."""
    h, w = a.shape[:2]
    if closed:
        for y in range(h):
            for x in range(w):
                if math.hypot(x - cx, y - cy) <= r + 0.6 and fur is not None:
                    a[y, x, :3] = fur
        pts = []
        for i in range(25):
            t = i / 12 - 1
            pts.append((cx + t * (r - 0.8), cy - r * 0.1 + r * 0.45 * (1 - t * t)))
        for (x, y) in pts:
            for dy in (0, 1):
                xi, yi = int(round(x)), int(round(y)) + dy
                if 0 <= xi < w and 0 <= yi < h:
                    a[yi, xi, :3] = lash
        for t in (-0.65, 0.0, 0.65):
            x, y = cx + t * (r - 0.8), cy - r * 0.1 + r * 0.45 * (1 - t * t)
            for k in (1, 2):
                xi, yi = int(round(x + t * k * 0.7)), int(round(y + k))
                if 0 <= xi < w and 0 <= yi < h:
                    a[yi, xi, :3] = lash
        return
    ring0, ring1 = np.array(iris[0], np.float32), np.array(iris[1], np.float32)
    for y in range(h):
        for x in range(w):
            d = math.hypot(x - cx, y - cy)
            if d > r:
                continue
            col = np.array((14, 12, 26), np.float32)                  # the pupil
            below = (y - cy) / r
            if below > -0.1 and d > r * 0.55:                           # the ring, lower half
                t = np.clip((below + 0.1) / 1.1, 0, 1)
                col = ring0 * (1 - t) + ring1 * t
            if d > r - 1.1 and y < cy:                                  # the lash arc on top
                col = np.array(lash, np.float32)
            a[y, x, :3] = col
    for (sx, sy, sr) in ((cx - r * 0.35, cy - r * 0.38, r * 0.3), (cx + r * 0.38, cy + r * 0.15, r * 0.14)):
        for y in range(h):
            for x in range(w):
                if math.hypot(x - sx, y - sy) <= max(0.7, sr):
                    a[y, x, :3] = 250
    if lid > 0 and fur is not None:                                     # a cheeky half-closed lid
        edge = cy - r + 2 * r * lid
        for y in range(h):
            for x in range(w):
                if math.hypot(x - cx, y - cy) <= r + 0.5 and y < edge + 0.3 * math.cos((x - cx) / r * 1.4):
                    a[y, x, :3] = fur
        for x in range(w):
            if abs(x - cx) <= r * 0.95:
                y = int(round(edge + 0.3 * math.cos((x - cx) / r * 1.4)))
                if 0 <= y < h:
                    a[y, x, :3] = lash


def blush(a, cx, cy, rx, ry, col=(255, 120, 160), strength=0.6):
    h, w = a.shape[:2]
    for y in range(h):
        for x in range(w):
            e = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if e < 1 and a[y, x, 3] > 0:
                k = strength * (1 - e)
                a[y, x, :3] = a[y, x, :3] * (1 - k) + np.array(col, np.float32) * k


def face(base, eye_y=0.42, eye_r=0.2, spread=0.26, iris=None, lid=0.0, blush_y=0.72, disc=None, cheeks=None, fur=None):
    """The front of a head: `base` (a painter) with two guh eyes and a blush. Sizes are fractions of the face.
    disc: (colour, rim) a heart-shaped face disc (the owl); cheeks: a colour for white cheek patches (the tit)."""
    def p(w, h, f, rng, closed):
        a = base(w, h, f, rng, closed)
        if disc:
            col, rim = disc
            for y in range(h):
                for x in range(w):
                    u, v = (x + 0.5) / w - 0.5, (y + 0.5) / h - 0.5
                    # two overlapping circles (a heart without its point): the owl's face disc
                    d = min(math.hypot(u + 0.2, v + 0.02), math.hypot(u - 0.2, v + 0.02))
                    if d < 0.3 or (abs(u) < 0.2 and -0.25 < v < 0.34 - abs(u)):
                        a[y, x, :3] = rim if (d > 0.27 and d < 0.3) else col
                        a[y, x, :3] += rng.integers(-4, 5, 3)
        if cheeks:
            for side in (-1, 1):
                cx, cy = w / 2 + side * w * 0.3, h * 0.66
                for y in range(h):
                    for x in range(w):
                        if ((x - cx) / (w * 0.22)) ** 2 + ((y - cy) / (h * 0.22)) ** 2 < 1:
                            a[y, x, :3] = cheeks
        r = eye_r * min(w, h)
        furc = fur or tuple(int(v) for v in np.median(a[..., :3].reshape(-1, 3), 0))
        for side in (-1, 1):
            cx = w / 2 - 0.5 + side * w * spread
            eye(a, cx, h * eye_y, r, iris=iris or ((120, 220, 240), (40, 110, 220)), lid=lid, closed=closed, fur=furc)
        for side in (-1, 1):
            blush(a, w / 2 - 0.5 + side * w * (spread + 0.1), h * blush_y, w * 0.12, h * 0.07)
        return a
    return p


def cheese_stripe(base, stripe, hole):
    """The tit's breast: cheese yellow with a darker cheese stripe down the middle, with little holes (kaasgaatjes)."""
    def p(w, h, face, rng, closed):
        a = fluff(base, tuple(min(255, c + 25) for c in base), 5)(w, h, face, rng, closed)
        if face in ("north", "down"):
            sw_ = max(2, w // 4)
            x0 = w // 2 - sw_ // 2
            a[:, x0:x0 + sw_, :3] = np.array(stripe, np.float32) + rng.integers(-4, 5, (h, sw_, 1))
            for y in range(2, h - 1, 5):
                xx = x0 + (1 if (y // 5) % 2 else sw_ - 2)
                if 0 <= xx < w:
                    a[y, xx, :3] = hole
                    if y + 1 < h:
                        a[y + 1, xx, :3] = hole
        return a
    return p


def beak(col, dark, spot=None):
    def p(w, h, face, rng, closed):
        a = solid(col, 4)(w, h, face, rng, closed)
        a[-1:, :, :3] = dark
        if spot is not None and face in ("north", "down", "east", "west"):
            a[max(0, h - 3):, max(0, w // 2 - 1):w // 2 + 1, :3] = spot
        return a
    return p


# =====================================================================================================================
# the birds
# =====================================================================================================================
def pluisvinkje(h):
    at = Atlas("pluisvinkje", 32, 32)
    PINK, PINK_L, WHITE, WHITE_P = (246, 150, 190), (255, 196, 220), (255, 246, 250), (255, 226, 238)
    BEAK, BEAK_D = (255, 170, 120), (226, 120, 90)
    body = {"*": vgradient(PINK, WHITE_P, 0.55, 0.3, tufts=PINK_L), "up": fluff(PINK, PINK_L), "down": fluff(WHITE, WHITE_P),
            "north": fluff(WHITE, WHITE_P), "south": vgradient(PINK, WHITE_P, 0.6, 0.3)}
    head = {"*": vgradient(PINK, WHITE_P, 0.7, 0.3, tufts=PINK_L), "up": fluff(PINK, PINK_L),
            "north": face(vgradient(PINK, WHITE, 0.25, 0.3), eye_y=0.42, eye_r=0.19, spread=0.25, blush_y=0.74)}
    wing = {"*": tipped(PINK, (214, 110, 160), frac=0.3), "up": solid(PINK), "down": solid(PINK_L)}
    tail = {"*": rounded(tipped(WHITE_P, PINK, along="y", frac=0.4), 0.45)}
    leg = solid((236, 140, 130))
    bones = [
        bone("root", None, [0, 0, 0]),
        bone("body", "root", [0, 3, 0], [cube(at, [-2.5, 1, -2.5], [5, 4.5, 5.5], body)]),
        bone("head", "body", [0, 5, -1.5], [cube(at, [-2, 4.5, -4], [4, 3.8, 3.6], head),
                                             cube(at, [-0.5, 8.3, -2.8], [1, 0.9, 1.4], fluff(PINK, PINK_L))]),
        bone("ear_left", "head", [1.5, 8, -2.2], [cube(at, [0.9, 7.8, -2.4], [1.6, 1.6, 0.5], {"*": ear(PINK, (255, 120, 170))})]),
        bone("ear_right", "head", [-1.5, 8, -2.2], [cube(at, [-2.5, 7.8, -2.4], [1.6, 1.6, 0.5], {"*": ear(PINK, (255, 120, 170))})]),
        bone("beak", "head", [0, 5.3, -4], [cube(at, [-0.6, 4.85, -4.9], [1.2, 0.85, 0.9], beak(BEAK, BEAK_D))]),
        bone("wing_left", "body", [2.5, 4.9, -1.2], [cube(at, [2.5, 2.0, -1.8], [0.5, 2.9, 4.4], wing)]),
        bone("wing_right", "body", [-2.5, 4.9, -1.2], [cube(at, [-3.0, 2.0, -1.8], [0.5, 2.9, 4.4], wing)]),
        bone("tail", "body", [0, 3.4, 2.8], [cube(at, [-1.5, 3.1, 2.8], [3, 0.5, 2.6], tail)], rotation=[-20, 0, 0]),
        bone("leg_left", "body", [1, 1, 0], [cube(at, [0.75, 0, -0.25], [0.5, 1, 0.5], leg), cube(at, [0.5, 0, -1.1], [1, 0.25, 1.2], leg)]),
        bone("leg_right", "body", [-1, 1, 0], [cube(at, [-1.25, 0, -0.25], [0.5, 1, 0.5], leg), cube(at, [-1.5, 0, -1.1], [1, 0.25, 1.2], leg)]),
    ]
    write_geo(h, "pluisvinkje", at, bones, 0.8, 0.8)
    at.save(h)
    return bones


def kaasmeesje(h):
    at = Atlas("kaasmeesje", 32, 32)
    CHEESE, STRIPE, HOLE = (250, 214, 84), (226, 160, 48), (170, 106, 30)
    CAP, CAP_L = (184, 150, 214), (206, 180, 232)
    MOSS, WING, WING_TIP, WHITE = (170, 190, 112), (126, 150, 190), (86, 104, 150), (252, 250, 244)
    body = {"*": vgradient(MOSS, CHEESE, 0.4, 0.3), "up": fluff(MOSS, (190, 206, 130)), "north": cheese_stripe(CHEESE, STRIPE, HOLE),
            "down": cheese_stripe(CHEESE, STRIPE, HOLE), "south": vgradient(MOSS, CHEESE, 0.6, 0.3)}
    headp = {"*": vgradient(CAP, WHITE, 0.45, 0.15), "up": fluff(CAP, CAP_L), "south": vgradient(CAP, MOSS, 0.6, 0.2),
             "north": face(vgradient(CAP, WHITE, 0.28, 0.12), eye_y=0.46, eye_r=0.18, spread=0.25, cheeks=WHITE, blush_y=0.8)}
    wing = {"*": tipped(WING, WING_TIP, frac=0.35), "up": solid(WING), "down": solid(WING)}

    def wingbar(w, h_, f, rng, closed):     # a white wing bar
        a = wing["*"](w, h_, f, rng, closed)
        if f in ("east", "west"):
            a[h_ // 3:h_ // 3 + 2, :, :3] = WHITE
        return a
    wing["east"] = wing["west"] = wingbar
    tail = {"*": rounded(tipped(WING, WING_TIP, along="y", frac=0.5), 0.35)}
    leg = solid((140, 150, 176))
    bones = [
        bone("root", None, [0, 0, 0]),
        bone("body", "root", [0, 3, 0], [cube(at, [-2, 1, -2.5], [4, 4, 5.5], body)]),
        bone("head", "body", [0, 5, -1.5], [cube(at, [-1.8, 4.3, -4], [3.6, 3.5, 3.4], headp)]),
        bone("ear_left", "head", [1.3, 7.6, -2.4], [cube(at, [0.8, 7.5, -2.6], [1.3, 1.3, 0.5], {"*": ear(CAP, (255, 140, 180))})]),
        bone("ear_right", "head", [-1.3, 7.6, -2.4], [cube(at, [-2.1, 7.5, -2.6], [1.3, 1.3, 0.5], {"*": ear(CAP, (255, 140, 180))})]),
        bone("beak", "head", [0, 5.0, -4], [cube(at, [-0.45, 4.6, -4.8], [0.9, 0.65, 0.8], beak((70, 64, 76), (40, 36, 44)))]),
        bone("wing_left", "body", [2, 4.5, -1.2], [cube(at, [2, 1.8, -1.8], [0.5, 2.7, 4.6], wing)]),
        bone("wing_right", "body", [-2, 4.5, -1.2], [cube(at, [-2.5, 1.8, -1.8], [0.5, 2.7, 4.6], wing)]),
        bone("tail", "body", [0, 3.2, 2.8], [cube(at, [-1.1, 2.9, 2.8], [2.2, 0.45, 3.4], tail)], rotation=[-12, 0, 0]),
        bone("leg_left", "body", [0.8, 1, 0], [cube(at, [0.55, 0, -0.25], [0.45, 1, 0.45], leg), cube(at, [0.35, 0, -1], [0.9, 0.25, 1.1], leg)]),
        bone("leg_right", "body", [-0.8, 1, 0], [cube(at, [-1.0, 0, -0.25], [0.45, 1, 0.45], leg), cube(at, [-1.25, 0, -1], [0.9, 0.25, 1.1], leg)]),
    ]
    write_geo(h, "kaasmeesje", at, bones, 0.8, 0.8)
    at.save(h)
    return bones


def guh_uiltje(h):
    at = Atlas("guh_uiltje", 48, 48)
    COCOA, COCOA_D, COCOA_L = (196, 142, 136), (150, 98, 96), (222, 176, 166)
    CREAM, CREAM_D = (252, 232, 220), (214, 170, 160)
    DISC, RIM = (255, 236, 232), (206, 140, 140)
    AMBER = ((255, 226, 120), (238, 150, 40))

    def chevrons(base, mark):
        def p(w, h_, f, rng, closed):
            a = fluff(base, (255, 246, 240), 4, rmax=2)(w, h_, f, rng, closed)
            for y in range(3, h_ - 1, 6):
                for x in range(2 + (y // 6) % 2 * 3, w - 2, 6):
                    for i in (-1, 0, 1):
                        if 0 <= x + i < w and 0 <= y + abs(i) < h_:
                            a[y - abs(i) + 1, x + i, :3] = mark
            return a
        return p

    def eye_glow(w, h_, f, rng):
        """The glowmask of the face: just the eyes' amber rings and shines (they light up at night)."""
        a = np.zeros((h_, w, 4), np.float32)
        if f != "north":
            return a
        r = 0.19 * min(w, h_)
        for side in (-1, 1):
            cx, cy = w / 2 - 0.5 + side * w * 0.22, h_ * 0.45
            for y in range(h_):
                for x in range(w):
                    d = math.hypot(x - cx, y - cy)
                    if d <= r and ((y - cy) / r > -0.1 and d > r * 0.55):
                        a[y, x] = (255, 214, 110, 255)
                    if math.hypot(x - (cx - r * 0.35), y - (cy - r * 0.38)) <= r * 0.3:
                        a[y, x] = (255, 255, 236, 255)
        return a
    body = {"*": rows_of_feathers(COCOA, COCOA_D, COCOA_L), "north": chevrons(CREAM, CREAM_D), "down": solid(CREAM),
            "up": rows_of_feathers(COCOA, COCOA_D, COCOA_L)}
    headp = {"*": rows_of_feathers(COCOA, COCOA_D, COCOA_L), "up": fluff(COCOA, COCOA_L),
             "north": face(solid(COCOA, 5), eye_y=0.45, eye_r=0.19, spread=0.22, iris=AMBER, blush_y=0.74,
                           disc=(DISC, RIM), fur=DISC)}
    wing = {"*": tipped(COCOA, COCOA_D, frac=0.25), "east": rows_of_feathers(COCOA, COCOA_D, CREAM),
            "west": rows_of_feathers(COCOA, COCOA_D, CREAM)}
    tail = {"*": rounded(tipped(COCOA, COCOA_D, along="y", frac=0.4), 0.35)}
    talon = solid((250, 204, 120))
    bones = [
        bone("root", None, [0, 0, 0]),
        bone("body", "root", [0, 4, 0], [cube(at, [-3, 1, -2.5], [6, 6, 5.5], body)]),
        bone("head", "body", [0, 7, 0], [cube(at, [-3.3, 6.6, -3.2], [6.6, 5.2, 5.6], headp,
                                              glow={"north": eye_glow})]),
        bone("ear_left", "head", [2.6, 11.6, -1.2], [cube(at, [1.4, 11.2, -1.6], [2.4, 2.4, 0.8], {"*": ear(COCOA, (255, 150, 176))})]),
        bone("ear_right", "head", [-2.6, 11.6, -1.2], [cube(at, [-3.8, 11.2, -1.6], [2.4, 2.4, 0.8], {"*": ear(COCOA, (255, 150, 176))})]),
        bone("beak", "head", [0, 8.2, -3.2], [cube(at, [-0.55, 7.4, -3.9], [1.1, 1.3, 0.8], beak((150, 98, 110), (100, 60, 72)))]),
        bone("wing_left", "body", [3, 6.3, -1], [cube(at, [3, 1.8, -1.9], [0.6, 4.6, 4.8], wing)]),
        bone("wing_right", "body", [-3, 6.3, -1], [cube(at, [-3.6, 1.8, -1.9], [0.6, 4.6, 4.8], wing)]),
        bone("tail", "body", [0, 2.2, 3], [cube(at, [-1.6, 1.8, 2.9], [3.2, 0.5, 2.2], tail)], rotation=[20, 0, 0]),
        bone("leg_left", "body", [1.3, 1, -1], [cube(at, [0.6, 0, -2.2], [1.4, 1, 1.4], talon)]),
        bone("leg_right", "body", [-1.3, 1, -1], [cube(at, [-2.0, 0, -2.2], [1.4, 1, 1.4], talon)]),
    ]
    write_geo(h, "guh_uiltje", at, bones, 1.0, 1.0)
    at.save(h, glowmask=True)
    return bones


def zeemeeuwtje(h):
    at = Atlas("zeemeeuwtje", 48, 48)
    WHITE, WHITE_D, GREY, GREY_D, BLACK = (252, 252, 250), (226, 230, 236), (178, 188, 204), (140, 150, 170), (40, 40, 48)
    YEL, YEL_D, RED = (255, 212, 70), (220, 160, 40), (230, 50, 60)
    body = {"*": vgradient(WHITE, WHITE_D, 0.8, 0.3), "up": fluff(GREY, (200, 208, 220)), "north": fluff(WHITE, WHITE_D),
            "down": fluff(WHITE, WHITE_D)}
    headp = {"*": fluff(WHITE, WHITE_D), "north": face(fluff(WHITE, WHITE_D), eye_y=0.46, eye_r=0.18, spread=0.25, lid=0.38,
                                                         blush_y=0.78, fur=WHITE)}
    wing = {"*": tipped(GREY, BLACK, frac=0.3, spots=WHITE), "up": tipped(GREY, BLACK, along="y", frac=0.3, spots=WHITE),
            "down": solid(WHITE_D)}
    tail = {"*": rounded(vgradient(WHITE, GREY, 0.7, 0.3), 0.3)}
    leg = solid((250, 150, 140))
    bones = [
        bone("root", None, [0, 0, 0]),
        bone("body", "root", [0, 4, 0], [cube(at, [-2.5, 1.8, -3], [5, 4.4, 7.2], body)]),
        bone("head", "body", [0, 6, -2.5], [cube(at, [-2, 5.4, -5.4], [4, 3.8, 3.8], headp)]),
        bone("ear_left", "head", [1.5, 9, -3.6], [cube(at, [0.9, 8.9, -3.8], [1.4, 1.4, 0.5], {"*": ear(WHITE, (255, 150, 176))})]),
        bone("ear_right", "head", [-1.5, 9, -3.6], [cube(at, [-2.3, 8.9, -3.8], [1.4, 1.4, 0.5], {"*": ear(WHITE, (255, 150, 176))})]),
        bone("beak", "head", [0, 6.6, -5.4], [cube(at, [-0.6, 6.4, -7.6], [1.2, 0.8, 2.2], beak(YEL, YEL_D))]),
        bone("beak_lower", "head", [0, 6.4, -5.4], [cube(at, [-0.55, 5.9, -7.3], [1.1, 0.5, 1.9], beak(YEL, YEL_D, spot=RED))]),
        bone("wing_left", "body", [2.5, 5.8, -2], [cube(at, [2.5, 2.6, -2.6], [0.5, 3.2, 7.4], wing)]),
        bone("wing_right", "body", [-2.5, 5.8, -2], [cube(at, [-3.0, 2.6, -2.6], [0.5, 3.2, 7.4], wing)]),
        bone("tail", "body", [0, 3.4, 4], [cube(at, [-1.8, 3.1, 4.1], [3.6, 0.6, 2.4], tail)], rotation=[-8, 0, 0]),
        bone("leg_left", "body", [1, 1.9, 0], [cube(at, [0.75, 0.2, -0.25], [0.5, 1.7, 0.5], leg), cube(at, [0.3, 0, -1.3], [1.4, 0.25, 1.5], leg)]),
        bone("leg_right", "body", [-1, 1.9, 0], [cube(at, [-1.25, 0.2, -0.25], [0.5, 1.7, 0.5], leg), cube(at, [-1.7, 0, -1.3], [1.4, 0.25, 1.5], leg)]),
    ]
    write_geo(h, "zeemeeuwtje", at, bones, 1.2, 1.0)
    at.save(h)
    return bones


# =====================================================================================================================
# animations
# =====================================================================================================================
def kf(pairs):
    return {f"{t:.3f}": list(v) for t, v in pairs}


def wave(length, amp, axis=0, phase=0.0, steps=8, offset=(0, 0, 0)):
    out = []
    for i in range(steps + 1):
        v = list(offset)
        v[axis] += round(amp * math.sin(2 * math.pi * (i / steps + phase)), 3)
        out.append((length * i / steps, v))
    return kf(out)


def loop(length, bones):
    return {"loop": True, "animation_length": length, "bones": bones}


def once(length, bones):
    return {"animation_length": length, "bones": bones}


def flap(length, up, down, wide=False):
    """Wing beats: both wings swing out to the sides (z) between `up` and `down` degrees."""
    mid = (up + down) / 2
    left = kf([(0, (0, 0, -up)), (length / 2, (0, 0, -down)), (length, (0, 0, -up))])
    right = kf([(0, (0, 0, up)), (length / 2, (0, 0, down)), (length, (0, 0, up))])
    del mid
    return {"wing_left": {"rotation": left}, "wing_right": {"rotation": right}}


def animations(h, name, extra=None, flap_len=0.24):
    common = {
        "idle": loop(3.0, {
            "body": {"scale": kf([(0, (1, 1, 1)), (1.5, (1.03, 1.03, 1.02)), (3.0, (1, 1, 1))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 0, 0)), (1.15, (0, 0, 14)), (1.9, (0, 0, 14)), (2.05, (0, 0, 0)), (3.0, (0, 0, 0))])},
            "tail": {"rotation": kf([(0, (0, 0, 0)), (2.3, (0, 0, 0)), (2.4, (12, 0, 0)), (2.5, (0, 0, 0)), (2.6, (12, 0, 0)), (2.7, (0, 0, 0)), (3.0, (0, 0, 0))])},
            "ear_left": {"rotation": kf([(0, (0, 0, 0)), (2.6, (0, 0, 0)), (2.7, (0, 0, -18)), (2.85, (0, 0, 0)), (3.0, (0, 0, 0))])},
            "ear_right": {"rotation": kf([(0, (0, 0, 0)), (2.6, (0, 0, 0)), (2.7, (0, 0, 18)), (2.85, (0, 0, 0)), (3.0, (0, 0, 0))])}}),
        "hop": loop(0.4, {
            "body": {"position": kf([(0, (0, 0, 0)), (0.1, (0, 1.4, 0)), (0.2, (0, 1.6, 0)), (0.3, (0, 0.6, 0)), (0.4, (0, 0, 0))])},
            "leg_left": {"rotation": kf([(0, (0, 0, 0)), (0.1, (35, 0, 0)), (0.3, (-10, 0, 0)), (0.4, (0, 0, 0))])},
            "leg_right": {"rotation": kf([(0, (0, 0, 0)), (0.1, (35, 0, 0)), (0.3, (-10, 0, 0)), (0.4, (0, 0, 0))])},
            "wing_left": {"rotation": kf([(0, (0, 0, 0)), (0.1, (0, 0, -18)), (0.3, (0, 0, 0))])},
            "wing_right": {"rotation": kf([(0, (0, 0, 0)), (0.1, (0, 0, 18)), (0.3, (0, 0, 0))])}}),
        "fly": loop(flap_len, dict(flap(flap_len, 70, 10), **{
            "body": {"rotation": kf([(0, (12, 0, 0))]), "position": kf([(0, (0, 0, 0)), (flap_len / 2, (0, 0.5, 0)), (flap_len, (0, 0, 0))])},
            "leg_left": {"rotation": kf([(0, (70, 0, 0))])}, "leg_right": {"rotation": kf([(0, (70, 0, 0))])},
            "head": {"rotation": kf([(0, (-10, 0, 0))])},
            "tail": {"rotation": kf([(0, (-6, 0, 0)), (flap_len / 2, (4, 0, 0)), (flap_len, (-6, 0, 0))])}})),
        "peck": once(0.6, {
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.1, (45, 0, 0)), (0.2, (10, 0, 0)), (0.3, (45, 0, 0)), (0.45, (0, 0, 0))])},
            "body": {"rotation": kf([(0, (0, 0, 0)), (0.1, (12, 0, 0)), (0.3, (12, 0, 0)), (0.45, (0, 0, 0))])},
            "tail": {"rotation": kf([(0, (0, 0, 0)), (0.1, (-14, 0, 0)), (0.45, (0, 0, 0))])}}),
    }
    common.update(extra or {})
    h.w(f"{h.A}/geckolib/animations/entity/{name}.animation.json", {"format_version": "1.8.0", "animations": {
        f"animation.{name}.{k}": v for k, v in common.items()}})
    return set(common)


def all_animations(h):
    out = {}
    out["pluisvinkje"] = animations(h, "pluisvinkje", {
        # a little happy dance when you give it seeds: bounce, wings out, a feather puff
        "blij": once(1.0, {
            "body": {"position": kf([(0, (0, 0, 0)), (0.15, (0, 1.5, 0)), (0.3, (0, 0, 0)), (0.45, (0, 1.5, 0)), (0.6, (0, 0, 0))]),
                     "scale": kf([(0, (1, 1, 1)), (0.3, (1.12, 0.9, 1.12)), (0.6, (1, 1, 1))])},
            "wing_left": {"rotation": kf([(0, (0, 0, 0)), (0.15, (0, 0, -60)), (0.3, (0, 0, -20)), (0.45, (0, 0, -60)), (0.7, (0, 0, 0))])},
            "wing_right": {"rotation": kf([(0, (0, 0, 0)), (0.15, (0, 0, 60)), (0.3, (0, 0, 20)), (0.45, (0, 0, 60)), (0.7, (0, 0, 0))])}}),
    })
    out["kaasmeesje"] = animations(h, "kaasmeesje", {
        # hanging upside down (the renderer turns the whole bird over): a gentle swing, pecking now and then
        "hang": loop(2.4, {
            "body": {"rotation": wave(2.4, 10, axis=2)},
            "head": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, 0)), (1.3, (30, 0, 0)), (1.4, (0, 0, 0)), (1.5, (30, 0, 0)), (1.6, (0, 0, 0)), (2.4, (0, 0, 0))])},
            "wing_left": {"rotation": kf([(0, (0, 0, 0)), (1.9, (0, 0, 0)), (2.0, (0, 0, -30)), (2.1, (0, 0, 0)), (2.4, (0, 0, 0))])},
            "wing_right": {"rotation": kf([(0, (0, 0, 0)), (1.9, (0, 0, 0)), (2.0, (0, 0, 30)), (2.1, (0, 0, 0)), (2.4, (0, 0, 0))])},
            "leg_left": {"rotation": kf([(0, (0, 0, 0))])}, "leg_right": {"rotation": kf([(0, (0, 0, 0))])}}),
    })
    out["guh_uiltje"] = animations(h, "guh_uiltje", {
        # asleep on its perch by day: fluffed up, slow breathing, the head sunk in
        "slaap": loop(4.0, {
            "body": {"scale": kf([(0, (1.04, 1.0, 1.04)), (2.0, (1.1, 1.04, 1.1)), (4.0, (1.04, 1.0, 1.04))])},
            "head": {"position": kf([(0, (0, -0.6, 0))]), "rotation": kf([(0, (8, 0, 0)), (2.0, (12, 0, 0)), (4.0, (8, 0, 0))])},
            "ear_left": {"rotation": kf([(0, (0, 0, -20))])}, "ear_right": {"rotation": kf([(0, (0, 0, 20))])}}),
        # oehoe: puffs up and bobs
        "roep": once(1.2, {
            "body": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.08, 1.05, 1.08)), (0.5, (1, 1, 1)), (0.7, (1.08, 1.05, 1.08)), (1.0, (1, 1, 1))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-12, 0, 0)), (0.5, (0, 0, 0)), (0.7, (-12, 0, 0)), (1.0, (0, 0, 0))])}}),
    }, flap_len=0.4)
    out["zeemeeuwtje"] = animations(h, "zeemeeuwtje", {
        # "Mijn! Mijn!": the beak opens twice, the head goes up
        "roep": once(1.0, {
            "beak_lower": {"rotation": kf([(0, (0, 0, 0)), (0.08, (-35, 0, 0)), (0.3, (-35, 0, 0)), (0.4, (0, 0, 0)), (0.5, (-35, 0, 0)),
                                           (0.72, (-35, 0, 0)), (0.82, (0, 0, 0))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.1, (-28, 0, 0)), (0.4, (-10, 0, 0)), (0.52, (-28, 0, 0)), (0.9, (0, 0, 0))])},
            "body": {"rotation": kf([(0, (0, 0, 0)), (0.1, (-8, 0, 0)), (0.9, (0, 0, 0))])},
            "wing_left": {"rotation": kf([(0, (0, 0, 0)), (0.1, (0, 0, -25)), (0.4, (0, 0, 0)), (0.52, (0, 0, -25)), (0.9, (0, 0, 0))])},
            "wing_right": {"rotation": kf([(0, (0, 0, 0)), (0.1, (0, 0, 25)), (0.4, (0, 0, 0)), (0.52, (0, 0, 25)), (0.9, (0, 0, 0))])}}),
        # gliding on the sea wind: wings spread, a slow tilt
        "glide": loop(3.0, {
            "wing_left": {"rotation": wave(3.0, 6, axis=2, offset=(0, 0, -80))},
            "wing_right": {"rotation": wave(3.0, 6, axis=2, offset=(0, 0, 80))},
            "body": {"rotation": wave(3.0, 5, axis=2, offset=(6, 0, 0))},
            "leg_left": {"rotation": kf([(0, (70, 0, 0))])}, "leg_right": {"rotation": kf([(0, (70, 0, 0))])}}),
    }, flap_len=0.4)
    return out


# =====================================================================================================================
# build + self-check
# =====================================================================================================================
MODELS = {"pluisvinkje": pluisvinkje, "kaasmeesje": kaasmeesje, "guh_uiltje": guh_uiltje, "zeemeeuwtje": zeemeeuwtje}
NEEDED = ("root", "body", "head", "ear_left", "ear_right", "beak", "wing_left", "wing_right", "tail", "leg_left", "leg_right")


def build(h):
    bones = {name: f(h) for name, f in MODELS.items()}
    anims = all_animations(h)
    problems = check(h, bones, anims)
    if problems:
        raise SystemExit("vogels models:\n  " + "\n  ".join(problems))


def check(h, bones, anims):
    import json
    p = []
    for name, bs in bones.items():
        names = {b["name"] for b in bs}
        p += [f"{name}: no bone {n}" for n in NEEDED if n not in names]
        for b in bs:
            if b.get("parent") and b["parent"] not in names:
                p.append(f"{name}: {b['name']} hangs on a missing {b['parent']}")
        top = max(c["origin"][1] + c["size"][1] for b in bs for c in b["cubes"])
        if top > 16:
            p.append(f"{name}: {top} units tall")
        with open(os.path.join(h.A, "geckolib", "animations", "entity", f"{name}.animation.json"), encoding="utf-8") as f:
            data = json.load(f)["animations"]
        for an, a in data.items():
            for bn in a["bones"]:
                if bn not in names:
                    p.append(f"{name}: animation {an} moves a missing bone {bn}")
        need = {"idle", "hop", "fly", "peck"} | {"kaasmeesje": {"hang"}, "guh_uiltje": {"slaap", "roep"},
                                                 "zeemeeuwtje": {"roep", "glide"}, "pluisvinkje": {"blij"}}[name]
        p += [f"{name}: no animation {n}" for n in need - anims[name]]
        # the guh eyes on the face: the head's north face has dark pupils, a coloured ring and white shines, open and closed differ
        head = [b for b in bs if b["name"] == "head"][0]["cubes"][0]["uv"]["north"]
        (u, v), (w, hh) = head["uv"], head["uv_size"]
        faces = []
        for tex in ("", "_dicht"):
            a = np.asarray(Image.open(os.path.join(h.TEX, "entity", f"{name}{tex}.png")).convert("RGBA")).astype(np.int32)
            faces.append(a[int(v * PX):int((v + hh) * PX), int(u * PX):int((u + w) * PX)])
        dark = int((faces[0][..., :3].max(-1) < 50).sum())
        white = int((faces[0][..., :3].min(-1) > 245).sum())
        if dark < 20 or white < 4:
            p.append(f"{name}: no glossy guh eyes on the face (dark {dark}, shine {white})")
        ring = lambda f: int(((f[..., :3].max(-1) - f[..., :3].min(-1)) > 110).sum() - ((f[..., 0] > 200) & (f[..., 1] < 170) & (f[..., 2] > 120)).sum())  # noqa: E731
        if ring(faces[1]) > ring(faces[0]) * 0.3 or int((np.abs(faces[0] - faces[1]).sum(-1) > 30).sum()) < 40:
            p.append(f"{name}: the closed eyes (_dicht) look open")
    g = os.path.join(h.TEX, "entity", "guh_uiltje_glowmask.png")
    if not os.path.exists(g) or np.asarray(Image.open(g).convert("RGBA"))[..., 3].sum() == 0:
        p.append("guh_uiltje: an empty glowmask")
    return p
