"""
De Guhboerderij (2.8) - the textures of the farm's blocks, items and particles (all painted here, 16 x 16 pixel art),
plus the small pixel helpers that tuintjes_tex.py uses too.
"""
import math
import random

from PIL import Image


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def noisy(base, var, rng, size=16):
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), clamp(ch + v for ch in base) + (255,))
    return img


def art(rows, pal):
    """Pixel art from rows of characters ('.' and unknown characters are see-through)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    top = (16 - len(rows)) // 2
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, top + y), tuple(pal[ch][:3]) + (255,))
    return img


def guh_oogjes(img, xs=(5, 10), y=6, blush=True, ring=(80, 170, 230)):
    """Two tiny guh eyes (2 x 2: dark with a light dot and a blue lower ring) and blush, on a 16 x 16 texture."""
    px = img.load()
    for x in xs:
        px[x, y] = (24, 20, 40, 255)
        px[x + 1, y] = (250, 250, 255, 255)
        px[x, y + 1] = ring + (255,)
        px[x + 1, y + 1] = (24, 20, 40, 255)
        if blush:
            bx = x - 1 if x < 8 else x + 2
            if 0 <= bx < 16:
                px[bx, y + 2] = (255, 150, 186, 255)
    return img


def guh_gezicht(img, cx=8, cy=8, r=5.0):
    """A bigger guh face (for block fronts): round eyes with a blue ring and highlights, a small nose and blush."""
    px = img.load()
    for sx in (-1, 1):
        ex, ey = cx + sx * r * 0.5, cy - r * 0.15
        er = r * 0.36
        for y in range(16):
            for x in range(16):
                dx, dy = (x + 0.5 - ex) / er, (y + 0.5 - ey) / (er * 1.1)
                d = math.hypot(dx, dy)
                if d <= 1:
                    c = (24, 18, 36)
                    if dy > 0.1 and d > 0.45:
                        c = (70, 150, 220) if dy < 0.6 else (100, 200, 225)
                    px[x, y] = c + (255,)
        hx, hy = int(ex + er * 0.25), int(ey - er * 0.35)
        if 0 <= hx < 16 and 0 <= hy < 16:
            px[hx, hy] = (255, 255, 255, 255)
        bx, by = int(ex + sx * er * 0.6), int(ey + er * 1.7)
        for dx in (0, sx):
            if 0 <= bx + dx < 16 and 0 <= by < 16:
                px[bx + dx, by] = (255, 150, 186, 255)
    nx, ny = int(cx - 0.5), int(cy + r * 0.45)
    for dx in (0, 1):
        if 0 <= ny < 16:
            px[nx + dx, ny] = (214, 96, 150, 255)
    return img


# =====================================================================================================================
# blocks
# =====================================================================================================================
def pluiswol(rng):
    img = noisy((255, 236, 244), 5, rng)
    px = img.load()
    for _ in range(26):                                   # curls
        cx, cy = rng.randrange(16), rng.randrange(16)
        for (dx, dy, c) in ((0, 0, (255, 250, 252)), (1, 0, (255, 248, 250)), (0, 1, (236, 206, 222)), (1, 1, (244, 218, 232))):
            px[(cx + dx) % 16, (cy + dy) % 16] = c + (255,)
    return img


def voerbak_hout(rng):
    img = noisy((236, 150, 180), 6, rng)
    px = img.load()
    for y in (0, 5, 10, 15):
        for x in range(16):
            px[x, y] = (206, 116, 150, 255)
    for x in (3, 12):
        for y in range(16):
            if y % 5:
                px[x, y] = (214, 128, 160, 255)
    return img


def voerbak_voorkant(rng):
    img = voerbak_hout(rng)
    return guh_gezicht(img, 8, 8, 5.0)


def knabbelvoer_blok(rng):
    img = noisy((236, 196, 96), 10, rng)
    px = img.load()
    for _ in range(40):                                   # pellets
        x, y = rng.randrange(15), rng.randrange(15)
        c = rng.choice([(250, 214, 110), (214, 160, 60), (246, 180, 80), (180, 130, 50)])
        px[x, y] = c + (255,)
        px[x + 1, y] = clamp(v - 20 for v in c) + (255,)
    return img


def stro(rng):
    img = noisy((236, 204, 110), 8, rng)
    px = img.load()
    for _ in range(30):
        x, y = rng.randrange(14), rng.randrange(16)
        c = rng.choice([(250, 226, 140), (206, 166, 76)])
        for k in range(3):
            px[x + k, y] = c + (255,)
    return img


def ei(rng):
    img = noisy((255, 226, 130), 6, rng)
    px = img.load()
    for _ in range(10):
        px[rng.randrange(16), rng.randrange(16)] = (220, 160, 60, 255)
    return img


# =====================================================================================================================
# items
# =====================================================================================================================
PAL = {"k": (40, 26, 44), "w": (255, 255, 255), "p": (246, 150, 196), "P": (214, 96, 150), "r": (255, 214, 232), "b": (90, 170, 235)}


def icon_pluiswol():
    return art([
        "....rrr.........",
        "..rrwwwrr.rr....",
        ".rwwwwwwwrwwr...",
        "rwwwwrwwwwwwwr..",
        "rwwrwwwwwrwwwwr.",
        "rwwwwwwrwwwwrwr.",
        ".rwwrwwwwwwwwwr.",
        "..rrwwwwrwwwrr..",
        "....rrrrrrrr....",
    ], {"r": (240, 196, 216), "w": (255, 244, 248)})


def icon_knabbelei():
    return art([
        "......kkk.......",
        ".....kyyyk......",
        "....kyywyyk.....",
        "...kyywyyyyk....",
        "...kyyyydyyk....",
        "..kyydyyyyyyk...",
        "..kyyyyyydyyk...",
        "..kyyyyyyyyok...",
        "...kyydyyyok....",
        "....kooooook....",
        ".....kkkkkk.....",
    ], {"k": (150, 100, 40), "y": (255, 222, 120), "w": (255, 250, 220), "d": (214, 150, 50), "o": (236, 186, 80)})


def icon_kaasmelk():
    return art([
        "......ccc.......",
        "......ggg.......",
        ".......g........",
        "......gmg.......",
        ".....gmmmg......",
        "....gmmmmmg.....",
        "....gmyyymg.....",
        "....gmyhymg.....",
        "....gmyyymg.....",
        "....gmmmmmg.....",
        "....gmmmmmg.....",
        ".....ggggg......",
    ], {"c": (170, 120, 70), "g": (200, 226, 240), "m": (255, 246, 214), "y": (250, 200, 70), "h": (214, 150, 40)})


def icon_guhborstel():
    return art([
        "..........pp....",
        ".........pppp...",
        "........ppppp...",
        ".......ohhhh....",
        "......ohhhh.....",
        ".....oo.........",
        "....oo..........",
        "...oo...........",
        "..oo............",
        ".oo.............",
    ], {"p": (255, 170, 206), "h": (180, 120, 70), "o": (214, 150, 90)})


def icon_knabbelvoer():
    return art([
        ".....tttttt.....",
        "....tsssssst....",
        "....tsssssst....",
        "...tssyssyyst...",
        "...tsyyysssst...",
        "..tssssyysssst..",
        "..tsspPpsssyst..",
        "..tsssPssyysst..",
        "..tsssssssssst..",
        "...tttttttttt...",
    ], {"t": (150, 110, 70), "s": (226, 196, 140), "y": (240, 190, 70), "p": (246, 150, 196), "P": (214, 96, 150)})


def particle_wolplukje(i):
    col = [(255, 240, 246), (255, 224, 236), (250, 250, 255)][i]
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for y in range(8):
        for x in range(8):
            d = math.dist((x + 0.5, y + 0.5), (4, 4))
            if d < 3.4:
                img.putpixel((x, y), clamp(c - (18 if d > 2.4 else 0) for c in col) + (255,))
    return img


def particle_melkdruppel(i):
    img = Image.new("RGBA", (4, 4), (0, 0, 0, 0))
    col = [(255, 246, 214), (250, 216, 120)][i]
    for (x, y) in ((1, 0), (1, 1), (2, 1), (0, 2), (1, 2), (2, 2), (1, 3), (2, 3)):
        img.putpixel((x, y), col + (255,))
    return img


def textures(h):
    rng = random.Random(28501)
    save = h.save
    save(pluiswol(rng), "block", "pluiswolblok.png")
    save(voerbak_hout(rng), "block", "guh_voerbak.png")
    save(voerbak_voorkant(rng), "block", "guh_voerbak_voorkant.png")
    save(knabbelvoer_blok(rng), "block", "guh_voerbak_voer.png")
    save(stro(rng), "block", "kippennestje.png")
    save(ei(rng), "block", "kippennestje_ei.png")
    save(icon_pluiswol(), "item", "pluiswol.png")
    save(icon_knabbelei(), "item", "knabbelei.png")
    save(icon_kaasmelk(), "item", "kaasmelk.png")
    save(icon_guhborstel(), "item", "guhborstel.png")
    save(icon_knabbelvoer(), "item", "knabbelvoer.png")
    for name, n, painter in (("wolplukje", 3, particle_wolplukje), ("melkdruppel", 2, particle_melkdruppel)):
        for i in range(n):
            save(painter(i), "particle", f"{name}_{i}.png")
        h.w(f"{h.A}/particles/{name}.json", {"textures": [f"guhs:{name}_{i}" for i in range(n)]})
