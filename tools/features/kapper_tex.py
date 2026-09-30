"""
De Pluiskapper "Knip & Vads" (2.8) - the textures: the hair swatches of the 8 hairstyles (painted almost white, so the
kapper's dyes tint them: GuhClothesLayer multiplies the texture by the hair colour; natural = this creamy "pluisblond"),
the kapperscape, the item icons (krulmunt, feestkapselset, kappersschaar, 8 bottles of haarverf, the 8 kapsel icons), the
blocks (kappersstoel, haarwasbak), the particles (haarplukje, krulglitter) and Kapper Krulletje's own look.
"""
import math

import numpy as np
from PIL import Image

HAAR = (252, 246, 238)          # natural: creamy "pluisblond" (the dyes tint this)
HAAR_DONKER = (206, 192, 178)
ROZE = (255, 143, 203)
ROZE_DONKER = (206, 84, 150)
ZILVER = (214, 220, 232)
ZILVER_DONKER = (140, 148, 166)
GOUD = (250, 204, 72)
GOUD_DONKER = (186, 136, 30)

VERVEN = {  # dye: rgb (keep in sync with Haarverf.java)
    "roze": (0xFF, 0x8F, 0xCB), "mint": (0x8F, 0xF0, 0xC4), "citroen": (0xFF, 0xF0, 0x7A), "lavendel": (0xC7, 0xA6, 0xFF),
    "hemelsblauw": (0x8C, 0xCB, 0xFF), "perzik": (0xFF, 0xB4, 0x7F), "zilver": (0xD8, 0xDC, 0xE6), "regenboog": (0xFF, 0x9A, 0xD5)}
STIJLEN = ["krullen", "kuifje", "knotjes", "strikjes", "pluisbol", "vlechtjes", "hanenkam", "matje"]


def clamp(a):
    return np.clip(a, 0, 255)


def img(a):
    return Image.fromarray(clamp(a).astype(np.uint8), "RGBA")


# =====================================================================================================================
# hair swatches (32 x 32 px each, see make_guh_variants: every face of a clothing cube samples the whole swatch)
# =====================================================================================================================
def haar(rng, stijl):
    """A fluffy hair swatch in the style's own pattern, almost white (tinted by the dye)."""
    n = 32
    a = np.zeros((n, n, 3), np.float32)
    base = np.array(HAAR, np.float32)
    dark = np.array(HAAR_DONKER, np.float32)
    yy, xx = np.mgrid[0:n, 0:n]
    if stijl in ("krullen", "pluisbol", "knotjes"):
        # curls: little rings of light and shadow
        t = np.zeros((n, n), np.float32)
        for _ in range(28 if stijl == "krullen" else 40):
            cx, cy, r = rng.uniform(0, n), rng.uniform(0, n), rng.uniform(2.2, 4.2)
            d = np.hypot(xx - cx, yy - cy)
            ring = np.exp(-((d - r) ** 2) / 1.2)
            t = np.maximum(t, ring * rng.uniform(0.6, 1.0))
        a[:] = base - (base - dark) * (0.55 * t[..., None])
    elif stijl in ("kuifje", "matje", "hanenkam"):
        # smooth streaks (combed), a shine band
        streak = 0.5 + 0.5 * np.sin(xx * 0.9 + np.sin(yy * 0.25) * 2.0 + rng.uniform(0, 6))
        a[:] = base - (base - dark) * (0.35 * streak[..., None])
        shine = np.exp(-((yy - 9) ** 2) / 6.0)
        a[:] = a + (255 - a) * 0.5 * shine[..., None]
    else:
        # braids and pigtails: crossing strands
        strand = (np.sin((xx + yy) * 0.8) * np.sin((xx - yy) * 0.8))
        a[:] = base - (base - dark) * (0.3 + 0.3 * strand[..., None])
    a += rng.normal(0, 4, (n, n, 1))
    return clamp(a)


def strik(rng):
    """The bows of the strikjes / the ties of the vlechtjes: white satin with a soft pink edge and a shine."""
    n = 32
    a = np.zeros((n, n, 3), np.float32)
    a[:] = (255, 250, 252)
    a[:3, :] = a[-3:, :] = a[:, :3] = a[:, -3:] = (236, 210, 222)
    for i in range(6, 16):
        a[i, 20 - i:24 - i] = (255, 255, 255)
    return clamp(a + rng.normal(0, 3, (n, n, 1)))


def cape(rng):
    """The kapperscape: pink and white stripes with little silver scissors."""
    n = 32
    a = np.zeros((n, n, 3), np.float32)
    for x in range(n):
        a[:, x] = ROZE if (x // 4) % 2 == 0 else (255, 244, 250)
    for (cx, cy) in ((8, 8), (24, 20), (8, 26), (24, 4)):
        for d in range(-3, 4):
            for (x, y) in ((cx + d, cy + d), (cx + d, cy - d)):
                if 0 <= x < n and 0 <= y < n:
                    a[y, x] = ZILVER_DONKER if abs(d) < 3 else ZILVER
        for (x, y) in ((cx - 3, cy + 3), (cx + 3, cy + 3)):
            if 0 <= x < n and 0 <= y < n:
                a[y, x] = ROZE_DONKER
    return clamp(a + rng.normal(0, 3, (n, n, 1)))


# =====================================================================================================================
# 16x16 icons
# =====================================================================================================================
def grid(rows, colours):
    out = np.zeros((16, 16, 4), np.float32)
    top = (16 - len(rows)) // 2
    for y, row in enumerate(rows):
        for x, ch in enumerate(row.ljust(16, ".")[:16]):
            if ch in colours:
                out[top + y, x] = (*colours[ch], 255)
    return img(out)


# a little guh head (pink, with eyes) wearing each hairstyle: h = hair, d = hair shadow, s = bow
HEAD = ["................", "................", "................", "................", "................",
        "..pppppppppppp..", ".pppppppppppppp.", ".ppkkppppppkkpp.", ".ppkwppppppkwpp.", ".prrppppppppprp.",
        ".ppppppmmpppppp.", "..pppppppppppp..", "...pppppppppp...", "................", "................", "................"]
HAIR = {
    "krullen": ["...hh.hh.hh.....", "..hdhhdhhdhh....", ".hhhhhhhhhhhhh..", "hhdhhdhhdhhdhhh.", "hhhhhhhhhhhhhhh."],
    "kuifje": [".........hhh....", "........hhdh....", "......hhhhh.....", "...hhhhhhhhhh...", "..hhhhhhhhhhhh.."],
    "knotjes": ["..hhh......hhh..", ".hhdhh....hhdhh.", ".hhhhh....hhhhh.", "..hhhhhhhhhhhh..", "..hhhhhhhhhhhh.."],
    "strikjes": ["................", "s.s..........s.s", ".sss.hhhhhh.sss.", "s.shhhhhhhhhhs.s", "..hhhhhhhhhhhh.."],
    "pluisbol": ["...hhhhhhhhh....", "..hhdhhhhhdhh...", ".hhhhhhdhhhhhh..", "hhhdhhhhhhhdhhh.", "hhhhhhhhhhhhhhh."],
    "vlechtjes": ["................", "................", "....hhhhhhh.....", "..hhhhhhhhhhh...", "..hhhhhhhhhhhh.."],
    "hanenkam": ["......h.........", "......hh........", ".......hd.......", "......hhh.......", "...hhhhhhhhhh..."],
    "matje": ["................", "................", "...hhhhhhhhh....", "..hhhhhhhhhhhh..", "..hhhhhhhhhhhh.."],
}
EXTRA = {  # hair below the top rows (sides, braids, the mullet)
    "vlechtjes": {(r, c): "h" for r in range(6, 15) for c in (0, 14) if r % 2 == 0} | {(r, c): "d" for r in range(7, 15) for c in (0, 14) if r % 2}
    | {(15, 0): "s", (15, 14): "s"},
    "matje": {(r, c): "h" for r in range(6, 14) for c in (0, 1, 13, 14)},
    "strikjes": {(r, c): "h" for r in range(6, 12) for c in (0, 14)},
    "pluisbol": {(r, c): "h" for r in range(5, 11) for c in (0, 15)},
}


def kapsel_icon(stijl):
    rows = [list(r) for r in HEAD]
    for i, r in enumerate(HAIR[stijl]):
        for c, ch in enumerate(r):
            if ch != ".":
                rows[i + 1][c] = ch
    for (r, c), ch in EXTRA.get(stijl, {}).items():
        if 0 <= r < 16 and 0 <= c < 16:
            rows[r][c] = ch
    colours = {"p": (246, 170, 204), "k": (40, 26, 52), "w": (255, 255, 255), "r": (255, 120, 170), "m": (150, 72, 116),
               "h": (246, 214, 176), "d": (206, 162, 120), "s": (255, 110, 170)}
    out = np.zeros((16, 16, 4), np.float32)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colours:
                out[y, x] = (*colours[ch], 255)
    return img(out)


def cape_icon():
    rows = ["...aaaaaaaa.....", "..abbbbbbbba....", "..abcbbcbbba....", ".abbbbbbbcbba...", ".abcbbbbbbbba...",
            "abbbbbcbbbbbba..", "abcbbbbbbbcbba..", "aaaaaaaaaaaaaa.."]
    return grid(rows, {"a": ROZE_DONKER, "b": ROZE, "c": (255, 250, 252)})


def krulmunt():
    a = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 7.2:
                if d > 6.2:
                    col = GOUD_DONKER
                else:
                    shine = max(0.0, 1 - math.hypot(x - 4.5, y - 4.5) / 6)
                    col = tuple(np.array(GOUD) + (255 - np.array(GOUD)) * shine * 0.6)
                a[y, x] = (*col, 255)
    # a pink curl (a spiral) in the middle
    for k in range(40):
        t = k / 40 * 3.2 * math.pi
        r = 0.6 + t * 0.42
        x, y = int(round(7.5 + r * math.cos(t))), int(round(7.5 + r * math.sin(t)))
        if 0 <= x < 16 and 0 <= y < 16 and math.hypot(x - 7.5, y - 7.5) < 6.0:
            a[y, x, :3] = ROZE_DONKER
    a[3, 4, :3] = a[4, 3, :3] = (255, 255, 240)
    return img(a)


def schaar():
    """The kappersschaar: silver blades, pink rings."""
    rows = [".............zz.", "............zZz.", "...........zZz..", "..........zZz...", ".........zZz....",
            "....z...zZz.....", ".....z.zZz......", "......zZz.......", ".....zZzz.......", "....zZz..z......",
            "..rrrz.....z....", ".r..rr......r...", ".r..r......rrr..", "..rr......r..r..", "..........r..r..", "...........rr..."]
    return grid(rows, {"z": ZILVER_DONKER, "Z": (245, 248, 255), "r": ROZE_DONKER})


def haarverf(kleur):
    """A little bottle of dye: glass, the colour inside, a pink cap; regenboog has stripes."""
    rgb = VERVEN[kleur]
    a = np.zeros((16, 16, 4), np.float32)
    for y in range(2, 5):
        for x in range(6, 10):
            a[y, x] = (*ROZE_DONKER, 255) if y == 2 else (*ROZE, 255)
    for y in range(5, 7):
        for x in range(6, 10):
            a[y, x] = (220, 236, 246, 255)
    for y in range(7, 15):
        for x in range(4, 12):
            edge = x in (4, 11) or y == 14
            if kleur == "regenboog":
                hue = ((y - 7) / 8.0)
                import colorsys
                c = tuple(int(v * 255) for v in colorsys.hsv_to_rgb(hue, 0.55, 1.0))
            else:
                c = rgb
            col = np.array(c, np.float32) * (0.75 if edge else 1.0)
            if x == 5 and 8 <= y <= 12:
                col = col + (255 - col) * 0.6                # a shine on the glass
            a[y, x] = (*col, 255)
    for x in range(5, 11):                                   # a label with a heart
        a[10, x, :3] = (255, 250, 252)
        a[11, x, :3] = (255, 250, 252)
    a[10, 7, :3] = a[10, 8, :3] = a[11, 7, :3] = a[11, 8, :3] = ROZE_DONKER
    return img(a)


def feestkapselset():
    """A pink box with a big bow, a comb and a glitter spray peeking out."""
    rows = ["......s..s......", ".....sss.sss....", "......ssss......", "..gg...ss...zz..", "..gg..........z.",
            ".bbbbbbsbbbbbbb.", ".bcbbbbsbbbbcbb.", ".bbbbbbsbbbbbbb.", ".bbbcbbsbbcbbbb.", ".bbbbbbsbbbbbbb.",
            ".bbbbbbsbbbbbcb.", ".bcbbbbsbbbbbbb.", ".bbbbbbsbbbbbbb.", ".aaaaaaaaaaaaaa."]
    return grid(rows, {"s": (255, 250, 252), "b": ROZE, "a": ROZE_DONKER, "c": (255, 236, 120), "g": GOUD, "z": ZILVER})


# =====================================================================================================================
# blocks
# =====================================================================================================================
def noisy(base, var, rng, size=16):
    a = np.zeros((size, size, 4), np.float32)
    a[..., :3] = np.array(base, np.float32) + rng.normal(0, var, (size, size, 1))
    a[..., 3] = 255
    return a


def kussen(rng):
    """Pink leather with buttons (the chair's cushion)."""
    a = noisy(ROZE, 5, rng)
    for y in range(16):
        for x in range(16):
            if (x % 5 == 2 and y % 5 == 2):
                a[y, x, :3] = ROZE_DONKER
            if x in (0, 15) or y in (0, 15):
                a[y, x, :3] = np.array(ROZE_DONKER) * 1.05
    return img(a)


def chroom(rng):
    a = noisy(ZILVER, 3, rng)
    for y in range(16):
        a[y, :, :3] *= 1.08 - y / 60
    for i in range(3, 12):
        a[i, i - 1, :3] = (255, 255, 255)
    return img(a)


def porselein(rng):
    """The basin: white porcelain with a pink rim."""
    a = noisy((250, 246, 250), 3, rng)
    a[0:2, :, :3] = ROZE
    a[2, :, :3] = ROZE_DONKER
    return img(a)


def schuim(rng):
    """Foam inside the basin: white bubbles on pale blue."""
    a = noisy((200, 230, 246), 4, rng)
    for _ in range(18):
        x, y, r = rng.integers(0, 16), rng.integers(0, 16), rng.uniform(0.8, 2.2)
        for yy in range(16):
            for xx in range(16):
                if math.hypot(xx - x, yy - y) <= r:
                    a[yy, xx, :3] = (255, 255, 255)
    return img(a)


def goud(rng):
    a = noisy(GOUD, 6, rng)
    a[:, 0:2, :3] = GOUD_DONKER
    return img(a)


# =====================================================================================================================
# particles
# =====================================================================================================================
def plukje(i):
    """A little tuft of hair (a curl) in three shapes."""
    cols = [(246, 222, 190), (255, 190, 226), (230, 206, 170)]
    shapes = [[(2, 1), (3, 1), (1, 2), (4, 2), (1, 3), (3, 3), (4, 4), (5, 5)],
              [(1, 1), (2, 2), (3, 2), (4, 3), (5, 3), (3, 4), (2, 5)],
              [(3, 0), (2, 1), (4, 1), (2, 2), (4, 3), (3, 4), (2, 5), (1, 6)]]
    out = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for (x, y) in shapes[i]:
        out.putpixel((x, y), cols[i] + (255,))
    return out


def glitter(i):
    """A sparkle that grows and shrinks (4 frames), pink-gold."""
    out = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    r = [1, 2, 3, 2][i]
    col = [(255, 236, 140), (255, 190, 230), (255, 255, 255), (255, 170, 220)][i]
    for d in range(-r, r + 1):
        out.putpixel((4 + d, 4), col + (255 if abs(d) < r else 160,))
        out.putpixel((4, 4 + d), col + (255 if abs(d) < r else 160,))
    if r >= 2:
        for (x, y) in ((3, 3), (5, 3), (3, 5), (5, 5)):
            out.putpixel((x, y), col + (140,))
    return out


def textures(h):
    rng = np.random.default_rng(2840)
    save = h.save
    save(krulmunt(), "item", "krulmunt.png")
    save(schaar(), "item", "kappersschaar.png")
    save(feestkapselset(), "item", "feestkapselset.png")
    for kleur in VERVEN:
        save(haarverf(kleur), "item", f"haarverf_{kleur}.png")
    save(kussen(rng), "block", "kappersstoel_kussen.png")
    save(chroom(rng), "block", "kappersstoel_chroom.png")
    save(porselein(rng), "block", "haarwasbak_porselein.png")
    save(schuim(rng), "block", "haarwasbak_schuim.png")
    save(goud(rng), "block", "haarwasbak_kraan.png")
    for i in range(3):
        save(plukje(i), "particle", f"haarplukje_{i}.png")
    for i in range(4):
        save(glitter(i), "particle", f"krulglitter_{i}.png")
    h.w(f"{h.A}/particles/haarplukje.json", {"textures": [f"guhs:haarplukje_{i}" for i in range(3)]})
    h.w(f"{h.A}/particles/krulglitter.json", {"textures": [f"guhs:krulglitter_{i}" for i in range(4)]})
