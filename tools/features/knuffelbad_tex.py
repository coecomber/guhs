"""
Het Knuffelbad (2.8) - textures: the slide blocks (glijgoot in five colours with a moving water film, funnel tiles, the
glow-in-the-dark star tiles with an emissive star layer, bath tiles, pink foam), the wash tub, the start gates, the items
(eendjesmunt, guhshampoo, the guh-föhn, a rubber duck), the rubber ducks, the zwembandje, the shimmer of a washed guh and
the particles (soap bubbles, foam flakes, sparkles, splash drops). Pastel, like the Knuffeldal's palette.
"""
import math
import random

import numpy as np
from PIL import Image

ROZE = (245, 150, 195)
ROZE_D = (222, 108, 160)
WIT = (248, 244, 248)
AQUA = (150, 222, 236)
NAVY = (30, 34, 82)
GEEL = (255, 216, 60)
ORANJE = (255, 138, 42)

GOOT_KLEUREN = {"roze": ((246, 156, 200), (255, 214, 232)), "wit": ((244, 238, 246), (255, 255, 255)),
                "blos": (( 236, 96, 152), (255, 160, 200)), "donker": ((52, 36, 64), (110, 90, 130)), "glim": ((34, 40, 96), (70, 86, 170))}


def img(w, h, c=(0, 0, 0, 0)):
    return Image.new("RGBA", (w, h), c)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def noisy(w, h, base, var, seed):
    r = random.Random(seed)
    im = img(w, h)
    px = im.load()
    for y in range(h):
        for x in range(w):
            v = r.randint(-var, var)
            px[x, y] = tuple(max(0, min(255, c + v)) for c in base) + (255,)
    return im


def star(px, x, y, col, size=1):
    """A little 4-point star (plus sign with a bright middle)."""
    for d in range(-size, size + 1):
        for (xx, yy) in ((x + d, y), (x, y + d)):
            if 0 <= xx < 16 and 0 <= yy < 16:
                px[xx, yy] = col + (255,)
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = (255, 255, 240, 255)


# =====================================================================================================================
# blocks
# =====================================================================================================================
def tegels(base, voeg, seed, n=2, glans=True):
    """Glossy tiles (n x n) with grout lines and a shine in the corner of each tile."""
    im = noisy(16, 16, base, 6, seed)
    px = im.load()
    step = 16 // n
    for y in range(16):
        for x in range(16):
            if x % step == 0 or y % step == 0:
                px[x, y] = voeg + (255,)
            elif glans and (x % step, y % step) in ((1, 1), (2, 1), (1, 2)):
                px[x, y] = mix(base, (255, 255, 255), 0.55) + (255,)
            elif (x % step) == step - 1 or (y % step) == step - 1:
                px[x, y] = shade(base, 0.9) + (255,)
    return im


def glijgoot_frames(base, licht, seed, frames=16):
    """The running surface: wet tiles with a thin film of water flowing over them (animated: highlights sliding by)."""
    r = random.Random(seed)
    sheet = img(16, 16 * frames)
    tile = tegels(base, shade(base, 0.86), seed, n=2, glans=False)
    streaks = [(r.uniform(0, 16), r.uniform(0.6, 1.4), r.uniform(0, 16)) for _ in range(5)]
    for f in range(frames):
        frame = tile.copy()
        px = frame.load()
        for y in range(16):
            for x in range(16):
                c = px[x, y][:3]
                # the water film: soft waves moving down the texture
                w = 0.5 + 0.5 * math.sin((y + f * 16 / frames) * 2 * math.pi / 8 + math.sin(x * 0.7) * 0.8)
                c = mix(c, licht, 0.12 * w)
                for (sx, sw, sy) in streaks:
                    yy = (y - sy - f * 16 / frames) % 16
                    if abs(x - (sx + math.sin(yy * 0.5) * 1.5)) < sw * 0.6 and yy < 5:
                        c = mix(c, (255, 255, 255), 0.35 * (1 - yy / 5))
                px[x, y] = c + (255,)
        sheet.paste(frame, (0, 16 * f))
    return sheet


def sterren_laag(seed, n=6, kleuren=((255, 244, 170), (255, 200, 236), (200, 230, 255))):
    """A layer of glowing stars (drawn full bright on top of a tile)."""
    r = random.Random(seed)
    im = img(16, 16)
    px = im.load()
    spots = []
    while len(spots) < n:
        x, y = r.randint(1, 14), r.randint(1, 14)
        if all(abs(x - a) + abs(y - b) > 4 for a, b in spots):
            spots.append((x, y))
    for i, (x, y) in enumerate(spots):
        if i % 3 == 0:
            star(px, x, y, kleuren[i % len(kleuren)], 1)
        else:
            px[x, y] = kleuren[i % len(kleuren)] + (255,)
    return im


def gezichtje(px, x0, y0, big=False, blos=(255, 150, 190)):
    """A small guh face (eyes with a shine, blush, a little mouth) on a 16x16 texture."""
    k, w = (40, 26, 50), (255, 255, 255)
    for ex in (x0 + 2, x0 + 7):
        for dx in range(2):
            for dy in range(2 + big):
                px[ex + dx, y0 + dy] = k + (255,)
        px[ex, y0] = w + (255,)
    for (x, y) in ((x0 + 1, y0 + 3), (x0 + 10, y0 + 3)):
        px[x, y] = blos + (255,)
    for (x, y) in ((x0 + 4, y0 + 4), (x0 + 5, y0 + 5), (x0 + 6, y0 + 5), (x0 + 7, y0 + 4)):
        px[x, y] = (110, 40, 70, 255)


def blok_texturen(h):
    save = h.save
    save(tegels((244, 150, 196), (255, 222, 236), 2801), "block", "trechtertegel.png")
    save(tegels((246, 250, 252), AQUA, 2802, n=4), "block", "knuffelbad_badtegel.png")
    # glimtegel: dark blue tiles, faintly lighter edges; the stars glow (a full-bright layer)
    g = noisy(16, 16, NAVY, 5, 2803)
    px = g.load()
    for i in range(16):
        for (x, y) in ((i, 0), (0, i)):
            px[x, y] = (46, 52, 118, 255)
        for (x, y) in ((i, 15), (15, i)):
            px[x, y] = (22, 24, 60, 255)
    save(g, "block", "glimtegel.png")
    save(sterren_laag(2804, 7), "block", "glimtegel_sterren.png")
    fel = img(16, 16)
    px = fel.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            px[x, y] = mix((255, 150, 220), (255, 250, 255), max(0.0, 1 - d / 8)) + (255,)
    save(fel, "block", "glimtegel_fel.png")
    gloed = img(16, 16)
    px = gloed.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 7.6:
                px[x, y] = mix((255, 180, 230), (255, 255, 255), max(0.0, 1 - d / 6)) + (255,)
    save(gloed, "block", "glimtegel_fel_gloed.png")
    # the running surface, five colours: an animated wet top and tiled sides
    for kleur, (base, licht) in GOOT_KLEUREN.items():
        save(glijgoot_frames(base, licht, 2810 + len(kleur)), "block", f"knuffelbad_glijgoot_{kleur}.png")
        h.w(f"{h.TEX}/block/knuffelbad_glijgoot_{kleur}.png.mcmeta", {"animation": {"frametime": 2, "interpolate": True}})
        save(tegels(shade(base, 0.92), shade(base, 0.75), 2820 + len(kleur), n=2, glans=False), "block", f"knuffelbad_glijgoot_{kleur}_zijkant.png")
    save(sterren_laag(2830, 5), "block", "knuffelbad_glijgoot_glim_sterren.png")
    # pink foam: soft bubbles (see-through here and there)
    r = random.Random(2840)
    sch = img(16, 16)
    px = sch.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (255, 214, 234, 235)
    for _ in range(14):
        cx, cy, rad = r.uniform(0, 16), r.uniform(0, 16), r.uniform(1.2, 3.2)
        for y in range(16):
            for x in range(16):
                for ox in (-16, 0, 16):
                    for oy in (-16, 0, 16):
                        d = math.hypot(x + 0.5 - cx - ox, y + 0.5 - cy - oy)
                        if d < rad:
                            t = d / rad
                            c = mix((255, 250, 253), (246, 170, 206), t ** 3)
                            px[x, y] = c + (255,)
                            if t < 0.35 and x + 0.5 - cx - ox < 0 and y + 0.5 - cy - oy < 0:
                                px[x, y] = (255, 255, 255, 255)
    save(sch, "block", "knuffelbad_schuim.png")
    # the wash tub: pink wooden staves with golden bands, a guh face on the front, water or foam inside
    hout = noisy(16, 16, (236, 160, 176), 7, 2850)
    px = hout.load()
    for y in range(16):
        for x in range(16):
            if x % 4 == 0:
                px[x, y] = (206, 124, 146, 255)
            if y in (2, 3, 12, 13):
                px[x, y] = (246, 206, 96, 255) if y in (2, 12) else (214, 170, 70, 255)
    h.save(hout, "block", "guh_wastobbe_zijkant.png")
    voor = hout.copy()
    gezichtje(voor.load(), 2, 6, big=True)
    h.save(voor, "block", "guh_wastobbe_voor.png")
    h.save(noisy(16, 16, (196, 120, 140), 6, 2851), "block", "guh_wastobbe_bodem.png")
    water = noisy(16, 16, (126, 206, 236), 8, 2852)
    px = water.load()
    for (x, y) in ((3, 3), (4, 3), (9, 8), (10, 8), (5, 12), (12, 4)):
        px[x, y] = (230, 250, 255, 255)
    h.save(water, "block", "guh_wastobbe_water.png")
    h.save(sch.copy(), "block", "guh_wastobbe_schuim.png")
    # the start gate: pink posts with white stripes, and a sign in the slide's colours with a guh face
    paal = noisy(16, 16, (240, 160, 196), 5, 2860)
    px = paal.load()
    for y in range(0, 16, 4):
        for x in range(16):
            px[x, y] = (255, 240, 248, 255)
    h.save(paal, "block", "glijbaan_start_paal.png")
    borden = {"roze_trechter": ((250, 140, 196), (255, 224, 240)), "glimtunnel": ((34, 40, 96), (255, 236, 150)),
              "grote_plons": ((70, 176, 236), (230, 250, 255))}
    for gid, (bg, fg) in borden.items():
        bord = noisy(16, 16, bg, 4, 2861 + len(gid))
        px = bord.load()
        for x in range(16):
            px[x, 0] = px[x, 15] = shade(bg, 0.7) + (255,)
        for y in range(16):
            px[0, y] = px[15, y] = shade(bg, 0.7) + (255,)
        if gid == "roze_trechter":                    # a spiral
            for k in range(60):
                a = k * 0.35
                rr = 0.9 + k * 0.1
                x, y = int(7.5 + math.cos(a) * rr), int(7.5 + math.sin(a) * rr)
                if 1 <= x <= 14 and 1 <= y <= 14:
                    px[x, y] = fg + (255,)
        elif gid == "glimtunnel":
            for (x, y) in ((3, 3), (11, 4), (6, 9), (12, 11), (3, 12)):
                star(px, x, y, fg, 1)
        else:                                         # a big splash
            for k in range(12):
                a = math.pi + k * math.pi / 11
                for rr in range(3, 7):
                    x, y = int(7.5 + math.cos(a) * rr), int(10 + math.sin(a) * rr)
                    if 1 <= x <= 14 and 1 <= y <= 14 and (rr + k) % 2 == 0:
                        px[x, y] = fg + (255,)
            for x in range(2, 14):
                px[x, 11] = fg + (255,)
        h.save(bord, "block", f"glijbaan_start_{gid}.png")
    gz = img(16, 16, (255, 236, 244, 255))
    gezichtje(gz.load(), 2, 5, big=True)
    h.save(gz, "block", "glijbaan_start_gezicht.png")


# =====================================================================================================================
# items
# =====================================================================================================================
def grid(rows, pal):
    im = img(16, 16)
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                px[x, y] = pal[ch] + (255,) if len(pal[ch]) == 3 else pal[ch]
    return im


EENDJE_ICOON = [
    "................",
    "................",
    "........yyy.....",
    ".......yyyyy....",
    ".......ykyyy....",
    "......oyyyyy....",
    ".....ooyyyyy....",
    "........yyy.....",
    "..yy...yyyyyy...",
    ".yyyyyyyyyyyyy..",
    ".yyyyyyyyyyhyy..",
    ".yyyyyyyyyyyyy..",
    "..yyyyyyyyyyy...",
    "...ddddddddd....",
    "................",
    "................"]


def item_texturen(h):
    pal = {"y": GEEL, "k": (40, 26, 50), "o": ORANJE, "d": (224, 170, 30), "h": (255, 244, 170)}
    h.save(grid(EENDJE_ICOON, pal), "item", "knuffelbad_badeendje.png")
    munt = grid(["................", ".....gggggg.....", "...gghhhhhhgg...", "..ghhhyyyhhhhg..", "..ghhyykyyhhhg..", ".ghhhoyyyyhhhhg.",
                 ".ghhhhyyyhhhhhg.", ".ghyyyyyyyyyhhg.", ".ghyyyyyyyyyyhg.", ".ghhyyyyyyyyhhg.", ".ghhhddddddhhhg.", "..ghhhhhhhhhhg..",
                 "..gghhhhhhhhgg..", "...gggggggggg...", ".....gggggg.....", "................"],
                {"g": (196, 140, 30), "h": (250, 208, 70), "y": (255, 240, 170), "k": (60, 40, 30), "o": ORANJE, "d": (230, 190, 60)})
    h.save(munt, "item", "eendjesmunt.png")
    shampoo = grid(["......ww........", ".....wwww.......", "......ww........", "......pp........", ".....pppp.......", "....pppppp..b...",
                    "...pppppppp.....", "...pllllllp..b..", "...plkllklp.....", "...pllllllp.....", "...plbllblp.b...", "...pllmmllp.....",
                    "...pllllllp.....", "...pppppppp.....", "....pppppp......", "................"],
                   {"w": (240, 240, 250), "p": (240, 110, 170), "l": (255, 230, 240), "k": (40, 26, 50), "b": (200, 236, 255), "m": (200, 80, 130)})
    h.save(shampoo, "item", "guhshampoo.png")
    fohn = grid(["................", "................", ".....pppppp.....", "...pppppppppa...", "..pplppppppppaa.", "..pppppkpppppa.a",
                 "..pppppppppppaa.", "...pppppppppa...", ".....ppppp......", ".......pp.......", ".......pp.......", ".......hh.......",
                 ".......hh.......", ".......hh.......", "......hhhh......", "................"],
                {"p": (246, 150, 196), "l": (255, 220, 236), "k": (60, 40, 70), "a": (210, 240, 255), "h": (200, 110, 150)})
    h.save(fohn, "item", "guh_fohn.png")


# =====================================================================================================================
# entities
# =====================================================================================================================
def zwembandje_tex(h, sw):
    """The swim ring: pink and white segments (a glossy stripe), a pink guh head with a face, the ears."""
    a = np.zeros((64, 64, 4), np.uint8)
    rng = np.random.default_rng(2870)

    def paint(key, col, var=6, fn=None):
        x, y = sw[key]
        block = a[y:y + 8, x:x + 8]
        block[..., :3] = np.clip(np.array(col) + rng.normal(0, var / 2, (8, 8, 1)), 0, 255).astype(np.uint8)
        block[..., 3] = 255
        if fn:
            fn(block)

    def glans(block):
        block[1:3, 1:6, :3] = np.clip(block[1:3, 1:6, :3].astype(int) + 60, 0, 255)
    paint("roze", (246, 120, 176), fn=glans)
    paint("wit", (250, 246, 250), 3, fn=glans)
    paint("vacht", (246, 160, 200))
    paint("oor", (230, 110, 160))

    def gezicht(block):
        block[..., :3] = (246, 160, 200)
        for ex in (1, 5):
            block[2:4, ex:ex + 2, :3] = (40, 26, 50)
            block[2, ex, :3] = (255, 255, 255)
        block[5, 1, :3] = block[5, 6, :3] = (255, 110, 160)
        block[5, 3:5, :3] = (130, 50, 90)
    paint("gezicht", (246, 160, 200), fn=gezicht)
    h.save(Image.fromarray(a), "entity", "zwembandje.png")


EEND_KLEUREN = {  # body, beak, extra (for the kind's patterns)
    "normaal": (GEEL, ORANJE, None), "vadseendje": ((255, 200, 70), ORANJE, None), "kaaseendje": ((250, 214, 90), ORANJE, "gaatjes"),
    "guheendje": ((250, 176, 206), (240, 110, 160), "blos"), "badmeestereendje": ((255, 220, 70), ORANJE, None),
    "pluiseendje": ((255, 200, 226), (246, 130, 176), "pluis"), "trechtereendje": ((255, 190, 220), ORANJE, "spiraal"),
    "sterreneendje": ((44, 50, 120), (255, 214, 90), "sterren"), "glimeendje": ((150, 255, 180), (255, 120, 200), "glim"),
    "maaneendje": ((210, 214, 255), (255, 214, 90), "maan"), "duikeendje": ((255, 216, 60), ORANJE, None),
    "plonseendje": ((120, 210, 255), (255, 150, 60), "druppels"), "gouden_eendje": ((255, 200, 40), (255, 150, 40), "goud")}


def eend_texturen(h, sw, soorten):
    """One texture per kind (and a glow mask for the glowing ones)."""
    for soort in soorten:
        body, beak, patroon = EEND_KLEUREN[soort]
        a = np.zeros((32, 32, 4), np.uint8)
        g = np.zeros((32, 32, 4), np.uint8)
        rng = np.random.default_rng(2880 + len(soort))

        def paint(key, col, var=5):
            x, y = sw[key]
            blk = a[y:y + 8, x:x + 8]
            blk[..., :3] = np.clip(np.array(col) + rng.normal(0, var / 2, (8, 8, 1)), 0, 255).astype(np.uint8)
            blk[..., 3] = 255
            return blk
        lijf = paint("lijf", body)
        kop = paint("kop", body)
        paint("snavel", beak)
        paint("oog", (40, 26, 50), 2)[1:3, 1:3, :3] = 255
        if patroon == "gaatjes":
            for blk in (lijf, kop):
                for (x, y) in ((1, 1), (5, 2), (3, 5), (6, 6)):
                    blk[y, x, :3] = (214, 170, 50)
        elif patroon == "blos":
            kop[5:7, 0:2, :3] = (255, 120, 170)
            kop[5:7, 6:8, :3] = (255, 120, 170)
        elif patroon == "pluis":
            for blk in (lijf, kop):
                for (x, y) in ((0, 2), (3, 0), (6, 3), (2, 6), (5, 5)):
                    blk[y, x, :3] = (255, 240, 248)
        elif patroon == "spiraal":
            for k in range(20):
                x, y = int(3.5 + math.cos(k * 0.6) * k * 0.2), int(3.5 + math.sin(k * 0.6) * k * 0.2)
                if 0 <= x < 8 and 0 <= y < 8:
                    lijf[y, x, :3] = (240, 90, 160)
        elif patroon in ("sterren", "glim", "maan", "goud"):
            col = {"sterren": (255, 244, 170), "glim": (200, 255, 210), "maan": (255, 240, 170), "goud": (255, 250, 200)}[patroon]
            for key in ("lijf", "kop"):
                x0, y0 = sw[key]
                for (x, y) in ((1, 1), (5, 2), (3, 5), (6, 6), (2, 3)):
                    a[y0 + y, x0 + x, :3] = col
                    g[y0 + y, x0 + x] = col + (255,)
            if patroon == "glim":
                g[..., :] = np.where(a[..., 3:4] > 0, np.concatenate([np.clip(a[..., :3].astype(int) + 30, 0, 255).astype(np.uint8),
                                                                      np.full((32, 32, 1), 150, np.uint8)], axis=2), 0)
        elif patroon == "druppels":
            for (x, y) in ((1, 1), (5, 3), (3, 6)):
                lijf[y, x, :3] = (230, 250, 255)
        # the extras: cap (badmeester: red with a white band), ears (guh pink), tuft (pink fluff), nightcap (blue, white
        # pompom), snorkel (orange), goggles (blue glass), crown (gold)
        for key, col in (("petje", (230, 60, 70)), ("oren", (246, 150, 196)), ("kuifje", (255, 214, 236)), ("slaapmuts", (120, 140, 230)),
                         ("snorkel", (255, 140, 40)), ("bril", (120, 200, 255)), ("kroontje", (255, 206, 60)), ("fluitje", (220, 224, 236)),
                         ("buikje", body)):
            paint(key, col, 4)
        Image.fromarray(a).save(f"{h.TEX}/entity/badeendje_{soort}.png")
        if soort in ("sterreneendje", "glimeendje", "maaneendje", "gouden_eendje"):
            Image.fromarray(g).save(f"{h.TEX}/entity/badeendje_{soort}_glow.png")


def glans_tex(h):
    """The shimmer over a freshly washed guh: soft white and pink glints on black (drawn additive: black = nothing)."""
    r = random.Random(2890)
    a = np.zeros((64, 64, 4), np.uint8)
    a[..., 3] = 255
    for _ in range(26):
        x0, y0 = r.uniform(0, 64), r.uniform(0, 64)
        col = np.array(r.choice([(255, 255, 255), (255, 190, 230), (255, 236, 200)]), float)
        length, width = r.uniform(4, 12), r.uniform(0.6, 1.4)
        for t in np.linspace(-length / 2, length / 2, 40):
            x, y = x0 + t * 0.7, y0 - t * 0.7
            for dx in range(-2, 3):
                for dy in range(-2, 3):
                    xx, yy = int(x + dx) % 64, int(y + dy) % 64
                    d = math.hypot(dx, dy) / width
                    k = max(0.0, 1 - d) * (1 - abs(t) / (length / 2)) * 0.8
                    a[yy, xx, :3] = np.clip(a[yy, xx, :3] + col * k, 0, 255)
    for _ in range(18):
        x, y = r.randrange(64), r.randrange(64)
        a[y, x, :3] = 255
    h.save(Image.fromarray(a), "entity", "guh_glans.png")


# =====================================================================================================================
# particles (8 x 8)
# =====================================================================================================================
def deeltjes(h):
    for i, tint in enumerate(((210, 240, 255), (255, 220, 240), (230, 255, 230))):
        im = img(8, 8)
        px = im.load()
        for y in range(8):
            for x in range(8):
                d = math.hypot(x + 0.5 - 4, y + 0.5 - 4)
                if 2.6 < d < 3.7:
                    px[x, y] = tint + (220,)
                elif d <= 2.6:
                    px[x, y] = tint + (60,)
        px[2, 2] = (255, 255, 255, 255)
        px[3, 2] = (255, 255, 255, 200)
        h.save(im, "particle", f"zeepbelletje_{i}.png")
    for i, col in enumerate(((255, 240, 248), (255, 200, 226), (250, 230, 255))):
        im = img(8, 8)
        px = im.load()
        r = random.Random(2900 + i)
        for y in range(8):
            for x in range(8):
                d = math.hypot(x + 0.5 - 4, y + 0.5 - 4) + r.uniform(-0.6, 0.6)
                if d < 3.2:
                    px[x, y] = mix(col, (255, 255, 255), max(0, 1 - d / 3)) + (235,)
        h.save(im, "particle", f"schuimvlokje_{i}.png")
    for i, col in enumerate(((255, 250, 210), (255, 200, 240), (210, 236, 255))):
        im = img(8, 8)
        px = im.load()
        for k in range(8):
            px[k, 3] = px[k, 4] = col + (120,)
            px[3, k] = px[4, k] = col + (120,)
        for k in range(2, 6):
            px[k, 3] = px[k, 4] = col + (255,)
            px[3, k] = px[4, k] = col + (255,)
        px[3, 3] = px[4, 4] = px[3, 4] = px[4, 3] = (255, 255, 255, 255)
        h.save(im, "particle", f"glinstering_{i}.png")
    for i, col in enumerate(((200, 236, 255), (255, 214, 236), (230, 250, 255))):
        im = img(8, 8)
        px = im.load()
        for (x, y) in ((3, 1), (3, 2), (4, 2), (2, 3), (3, 3), (4, 3), (5, 3), (2, 4), (3, 4), (4, 4), (5, 4), (3, 5), (4, 5)):
            px[x, y] = col + (230,)
        px[3, 2] = (255, 255, 255, 255)
        h.save(im, "particle", f"plons_{i}.png")
    for name, n in (("zeepbelletje", 3), ("schuimvlokje", 3), ("glinstering", 3), ("plons", 3)):
        h.w(f"{h.A}/particles/{name}.json", {"textures": [f"guhs:{name}_{i}" for i in range(n)]})


def textures(h, sw_ring, sw_eend, soorten):
    blok_texturen(h)
    item_texturen(h)
    zwembandje_tex(h, sw_ring)
    eend_texturen(h, sw_eend, soorten)
    glans_tex(h)
    deeltjes(h)
