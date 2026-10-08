"""
The pictures of the FTB Quests chapters (made by tools/make_ftbquests.py, which calls make_art()):
  textures/ftbquests/<chapter>/title.png     a cute title banner: the chapter name on a ribbon, guh renders, kaasknabbels, hearts
  textures/ftbquests/<chapter>/welkom.png    a group picture next to the "Hoe kom je hier?" quest (guhs / NPCs / a structure)
  textures/ftbquests/<chapter>/kop_<id>.png  a section header: a ribbon with a portrait and the section title
  textures/ftbquests/<chapter>/slot_<module>.png   (bbq2) a "to be continued" card at the end of a chapter: what is still missing
  textures/ftbquests/icon_<chapter>.png      the chapter's guh icon (a custom FTB icon)
  textures/ftbquests/icon_vraag.png          (guhpad) the question-mark icon of the quests of "Het echte Guheinde"
A portrait 'silhouet:<spec>' is the black shape of that render with a question mark (guhpad); the ribbon colour "nacht" has
question marks where the other colours have hearts and kaasknabbels.
Guhs, NPCs and creatures are rendered from their own .geo.json models with tools/wiki_renders.py; structures come from the
wiki renders (docs/wiki/img, also made by wiki_renders.py). Text uses Minecraft's own pixel font (from the vanilla jar).

Rendering takes a while, so a picture is only redrawn when its recipe changes (tools/ftbquests_art.json keeps a hash per
file) or when it is missing:  python tools/make_ftbquests.py --art  redraws everything.
"""
import hashlib
import json
import math
import os
import random
import sys
from functools import lru_cache

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wiki_renders as wr  # noqa: E402

TEX = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "ftbquests")
STAMP = os.path.join("tools", "ftbquests_art.json")
WIKI_IMG = os.path.join("docs", "wiki", "img")
ART_VERSION = 2

# ribbon colours per chapter: (light, main, dark outline)
PALETTE = {
    "pink": ((255, 196, 228), (255, 132, 196), (122, 36, 84)),
    "magenta": ((246, 180, 255), (214, 104, 236), (86, 28, 110)),
    "gold": ((255, 236, 150), (255, 190, 60), (120, 70, 10)),
    "blue": ((178, 228, 255), (84, 170, 240), (22, 60, 120)),
    "red": ((255, 190, 190), (236, 92, 110), (110, 20, 40)),
    "purple": ((214, 190, 255), (150, 104, 230), (54, 26, 110)),
    "orange": ((255, 214, 160), (250, 140, 60), (110, 44, 10)),
    "lime": ((236, 250, 160), (190, 214, 70), (70, 90, 14)),
    "peach": ((255, 222, 214), (255, 160, 170), (120, 50, 60)),
    "green": ((200, 244, 196), (104, 200, 110), (26, 84, 36)),   # 3.0: Diertjes van de Guhmensie
    "cyan": ((196, 246, 250), (84, 204, 224), (16, 84, 104)),    # bbq2: Guh-technologie
    "nacht": ((126, 104, 170), (52, 40, 80), (14, 10, 24)),      # guhpad: Het echte Guheinde (a mystery: no hearts, question marks)
}


# ---------------------------------------------------------------------------------------------------------------------
# Minecraft's pixel font
# ---------------------------------------------------------------------------------------------------------------------
@lru_cache(maxsize=None)
def _font():
    sheet = wr.texture("font/ascii").convert("RGBA")  # (the first 'frame' crop in texture() keeps the square sheet)
    glyphs = {}
    for code in range(32, 127):
        gx, gy = (code % 16) * 8, (code // 16) * 8
        g = sheet.crop((gx, gy, gx + 8, gy + 8))
        cols = [x for x in range(8) if any(g.getpixel((x, y))[3] > 0 for y in range(8))]
        width = (max(cols) + 1) if cols else 3
        glyphs[chr(code)] = (g, width)
    return glyphs


def _plain(text):
    import re
    text = re.sub(r"&[0-9a-fk-or]", "", text)
    for a, b in (("é", "e"), ("ë", "e"), ("è", "e"), ("ï", "i"), ("ö", "o"), ("ü", "u"), ("á", "a"), ("’", "'"), ("→", ">")):
        text = text.replace(a, b)
    return text


def text_mask(text, scale):
    """White text on transparent, Minecraft font, `scale` px per font pixel."""
    glyphs = _font()
    text = _plain(text)
    w = sum(glyphs.get(c, glyphs["?"])[1] + 1 for c in text) + 1
    img = Image.new("RGBA", (w, 8), (0, 0, 0, 0))
    x = 0
    for c in text:
        g, gw = glyphs.get(c, glyphs["?"])
        img.alpha_composite(g, (x, 0))
        x += gw + 1
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


def fancy_text(text, scale, fill, outline, shadow=True):
    """Text with a thick outline and a soft drop shadow (like a sticker)."""
    m = text_mask(text, scale)
    pad = scale * 2 + 2
    W, H = m.width + pad * 2, m.height + pad * 2
    alpha = Image.new("L", (W, H), 0)
    alpha.paste(m.getchannel("A"), (pad, pad))
    ring = alpha.filter(ImageFilter.MaxFilter(2 * max(1, scale // 2) + 1))
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    if shadow:
        sh = Image.new("RGBA", (W, H), (40, 10, 30, 110))
        sh.putalpha(ring.point(lambda v: 110 if v else 0))
        out.alpha_composite(sh, (max(1, scale // 2), max(1, scale // 2)))
    ol =Image.new("RGBA", (W, H), outline + (255,))
    ol.putalpha(ring)
    out.alpha_composite(ol)
    top = Image.new("RGBA", (W, H), fill + (255,))
    top.putalpha(alpha)
    out.alpha_composite(top)
    # a little light on the top half of each letter
    hl = Image.new("RGBA", (W, H), (255, 255, 255, 0))
    hl_alpha = Image.new("L", (W, H), 0)
    hl_alpha.paste(alpha.crop((0, 0, W, pad + m.height // 2)), (0, 0))
    hl.putalpha(hl_alpha.point(lambda v: 60 if v else 0))
    out.alpha_composite(hl)
    return out


# ---------------------------------------------------------------------------------------------------------------------
# little decorations
# ---------------------------------------------------------------------------------------------------------------------
HEART = [".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."]


def heart(scale, colour=(255, 92, 150), outline=(122, 20, 60)):
    h, w = len(HEART), len(HEART[0])
    img = Image.new("RGBA", (w + 2, h + 2), (0, 0, 0, 0))
    for y, row in enumerate(HEART):
        for x, c in enumerate(row):
            if c == "X":
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    if img.getpixel((x + 1 + dx, y + 1 + dy))[3] == 0:
                        img.putpixel((x + 1 + dx, y + 1 + dy), outline + (255,))
    for y, row in enumerate(HEART):
        for x, c in enumerate(row):
            if c == "X":
                img.putpixel((x + 1, y + 1), colour + (255,))
    img.putpixel((2, 2), (255, 220, 235, 255))
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


def item(ref, px):
    ns, path = ref.split(":", 1)
    for r in ([ref] if "/" in path else [f"{ns}:item/{path}", f"{ns}:item/{path}_item", f"{ns}:block/{path}"]):
        try:
            return wr.texture(r).resize((px, px), Image.NEAREST)
        except (OSError, KeyError):
            pass
    print("  (no texture for", ref, "- a kaasknabbel instead)")
    return wr.texture("guhs:item/kaas_knabbels").resize((px, px), Image.NEAREST)


def knabbel(px, angle=0):
    img = item("guhs:kaas_knabbels", px)
    return img.rotate(angle, resample=Image.NEAREST, expand=True) if angle else img


# ---------------------------------------------------------------------------------------------------------------------
# sprites: guhs, NPCs, creatures (rendered) and structures (wiki renders)
# ---------------------------------------------------------------------------------------------------------------------
GEO = lambda n: os.path.join(wr.ASSETS, "geckolib", "models", "entity", n + ".geo.json")  # noqa: E731
HIDE = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
VARIANT_BONES = {"teckel": ("teckel",), "ender": ("ender",), "koning": ("koning",), "wolk": ("wolk",), "zeemeerguh": ("zeemeer",),
                 "asguh": ("asguh",), "pluisguh": ("pluis",), "brontosaurus": ("neck",), "vahoege_ender": ("ender",),
                 "baltoguh": ("balto",), "mewtwo": ("mewtwo",), "stitch626": ("stitch",),   # 3.0: the story guhs
                 "sam_guh": ("samguh",),   # bbq2: Sam-guh (his pack, bedroll, pan and tuft)
                 "guhshi": ("guhshi",),    # bbq2: Guhshi (his nose, crest, saddle, belly, tail and boots)
                 "bloesemguh": ("bloesem",), "tanukiguh": ("tanuki",)}   # biomes3
# bbq2 (ring-kern): two looks in one NPC model (the game picks one per viewer): a picture shows the look of the start of
# the story, so Guhdalf is grey and Araguh wears no crown yet (no spoilers in the quest book)
NPC_HIDE = {"guhdalf": ("wit_hoed", "wit_mantel", "wit_knop"), "araguh": ("araguh_kroon",)}


@lru_cache(maxsize=None)
def sprite(spec, size=256):
    """spec: 'guh:<variant>' | 'npc:<kind>' (2.8 NPCs have their own model, the rest sit) | 'geo:<model>:<texture>' |
    'wiki:<picture>' | 'item:<ns:id>' | 'silhouet:<any of those>' (guhpad: its black shape with a question mark).
    Returns a cropped RGBA image."""
    kind, _, rest = spec.partition(":")
    img = None
    if kind == "silhouet":
        return silhouet(sprite(rest, size))
    if kind == "guh":
        tex = "guhs:entity/guh" if rest in ("", "normal") else f"guhs:entity/guh_{rest}"
        if rest == "rainbow":
            tex = "guhs:entity/guh_rainbow_0"
        lift = ("head", (0, 22, -4.5)) if rest == "brontosaurus" else None
        img = wr.render(wr.geo_quads(GEO("guh"), tex, hide=HIDE, show_only_variant_bones=VARIANT_BONES.get(rest, ()), lift=lift),
                        24, -10, size, margin=0.02)
    elif kind == "npc":
        own = GEO(f"guh_npc_{rest}")
        model = own if os.path.exists(own) else GEO("guh_sitting")
        img = wr.render(wr.geo_quads(model, f"guhs:entity/npc_{rest}", hide=NPC_HIDE.get(rest, ())), 24, -10, size, margin=0.02)
    elif kind == "geo":
        model, tex = rest.split(":", 1)
        img = wr.render(wr.geo_quads(GEO(model), f"guhs:entity/{tex}"), 26, -12, size, margin=0.02)
    elif kind == "wiki":
        path = os.path.join(WIKI_IMG, rest + ".png")
        if os.path.exists(path):
            img = Image.open(path).convert("RGBA")
        else:
            print("  (no wiki picture", rest, "- a guh instead)")
            return sprite("guh:normal", size)
    elif kind == "item":
        img = item(rest, size)
    if img is None:
        raise ValueError(spec)
    box = img.getbbox()
    return img.crop(box) if box else img


def silhouet(img):
    """guhpad: the black shape of a render with a lilac edge of light and a question mark on it: somebody we do not know yet."""
    alpha = img.getchannel("A").point(lambda v: 255 if v > 40 else 0)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    rand = alpha.filter(ImageFilter.MaxFilter(max(3, (img.width // 60) * 2 + 1)))
    out.paste((120, 96, 176, 255), (0, 0), rand)
    out.paste((14, 10, 24, 255), (0, 0), alpha)
    q = fancy_text("?", max(2, img.width // 26), (176, 144, 224), (14, 10, 24), shadow=False)
    q = q.crop(q.getbbox())
    out.alpha_composite(q, ((img.width - q.width) // 2, max(0, int(img.height * 0.42) - q.height // 2)))
    return out


def vraagteken(px=64):
    """guhpad: the icon of a quest nobody knows anything about yet: a question mark on a dark tile."""
    img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    light, main, dark = PALETTE["nacht"]
    d.rounded_rectangle((3, 3, px - 4, px - 4), radius=px // 5, fill=main + (255,), outline=dark + (255,), width=max(2, px // 20))
    d.rounded_rectangle((8, 8, px - 9, px // 2), radius=px // 8, fill=light + (70,))
    q = fancy_text("?", max(2, px // 12), (204, 180, 240), dark, shadow=False)
    q = q.crop(q.getbbox())
    img.alpha_composite(q, ((px - q.width) // 2, (px - q.height) // 2))
    return img


def versiering(colour, i=0, scale=1.0):
    """The little thing that floats around a picture: a heart, or (guhpad, the colour "nacht") a question mark."""
    if colour != "nacht":
        return heart(int((4 if i != 1 else 5) * scale))
    q = fancy_text("?", max(2, int((3 if i != 1 else 4) * scale)), PALETTE["nacht"][0], PALETTE["nacht"][2], shadow=False)
    return q.crop(q.getbbox())


def fit(img, w, h):
    s = min(w / img.width, h / img.height)
    return img.resize((max(1, int(img.width * s)), max(1, int(img.height * s))), Image.LANCZOS)


def face(spec, px=64):
    """The chapter icon: the upper part of a render (the face), square."""
    img = sprite(spec, 256)
    top = img.crop((0, 0, img.width, min(img.height, max(img.width, int(img.height * 0.62)))))
    side = max(top.width, top.height)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.alpha_composite(top, ((side - top.width) // 2, (side - top.height) // 2))
    return sq.resize((px, px), Image.LANCZOS)


# ---------------------------------------------------------------------------------------------------------------------
# ribbons
# ---------------------------------------------------------------------------------------------------------------------
def ribbon(draw, x0, y0, x1, y1, colours, tails=True, stitch=True):
    light, main, dark = colours
    h = y1 - y0
    if tails:
        t = h * 0.55
        for side in (-1, 1):
            bx = x0 + h * 0.35 if side < 0 else x1 - h * 0.35
            ex = bx + side * (t + h * 0.25)
            pts = [(bx, y0 + h * 0.28), (ex, y0 + h * 0.28), (ex - side * h * 0.28, y0 + h * 0.64), (ex, y1 + h * 0.02),
                   (bx, y1 + h * 0.02)]
            draw.polygon(pts, fill=tuple(int(c * 0.78) for c in main) + (255,), outline=dark + (255,), width=max(2, h // 22))
    r = h // 2
    draw.rounded_rectangle((x0, y0, x1, y1), radius=r, fill=main + (255,), outline=dark + (255,), width=max(3, h // 18))
    inset = max(4, h // 9)
    draw.rounded_rectangle((x0 + inset, y0 + inset, x1 - inset, y0 + h * 0.46), radius=max(2, r - inset), fill=light + (150,))
    # stitching
    step = max(8, h // 7)
    y = y1 - inset * 1.4
    for x in [] if not stitch else range(int(x0 + r), int(x1 - r), step * 2):
        draw.line((x, y, x + step, y), fill=light + (200,), width=max(1, h // 40))


def scatter(img, rng, n_knabbels, n_hearts, avoid, scale=1.0, colour=None):
    """Kaasknabbels and hearts around the edges (not on the rectangles in `avoid`); guhpad: question marks for "nacht"."""
    placed = list(avoid)
    for i in range(n_knabbels + n_hearts):
        if colour == "nacht":
            deco = versiering(colour, rng.choice((0, 1)), scale)
            deco.putalpha(deco.getchannel("A").point(lambda v, f=rng.choice((110, 160, 220)): v * f // 255))
        else:
            deco = knabbel(int(rng.choice((30, 36, 42)) * scale), rng.randint(-25, 25)) if i < n_knabbels else \
                heart(int(rng.choice((3, 4, 5)) * scale))
        for _ in range(60):
            x, y = rng.randint(0, img.width - deco.width), rng.randint(0, img.height - deco.height)
            box = (x - 4, y - 4, x + deco.width + 4, y + deco.height + 4)
            if not any(box[0] < b[2] and box[2] > b[0] and box[1] < b[3] and box[3] > b[1] for b in placed):
                img.alpha_composite(deco, (x, y))
                placed.append(box)
                break


# ---------------------------------------------------------------------------------------------------------------------
# the pictures
# ---------------------------------------------------------------------------------------------------------------------
def title_banner(title, colour, left, right, seed):
    """1024x256: the chapter name on a big ribbon, a render on each side, kaasknabbels and hearts."""
    W, H = 1024, 256
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rng = random.Random(seed)
    cols = PALETTE[colour]
    rx0, rx1, ry0, ry1 = 190, W - 190, 70, 196
    d = ImageDraw.Draw(img, "RGBA")
    ribbon(d, rx0, ry0, rx1, ry1, cols)
    for scale in (6, 5, 4, 3):
        txt = fancy_text(title, scale, (255, 255, 255), cols[2])
        if txt.width <= rx1 - rx0 - 70:
            break
    img.alpha_composite(txt, ((W - txt.width) // 2, (ry0 + ry1) // 2 - txt.height // 2))
    boxes = [(rx0 - 60, ry0 - 10, rx1 + 60, ry1 + 20)]
    for spec, x_side in ((left, 0), (right, 1)):
        if not spec:
            continue
        spr = fit(sprite(spec), 200, 236)
        x = 8 if x_side == 0 else W - spr.width - 8
        img.alpha_composite(spr, (x, H - spr.height - 4))
        boxes.append((x, H - spr.height - 4, x + spr.width, H))
    # three little hearts floating over the ribbon's middle
    for i, dx in enumerate((-40, 0, 40)):
        hrt = versiering(colour, i)
        img.alpha_composite(hrt, (W // 2 + dx - hrt.width // 2, ry0 - hrt.height - (8 if i == 1 else 0)))
        boxes.append((W // 2 + dx - 20, ry0 - 40, W // 2 + dx + 20, ry0))
    scatter(img, rng, 6, 6, boxes, colour=colour)
    return img


def welcome_picture(specs, colour, seed, stage=None):
    """768x384: a soft rounded card with guhs/NPCs standing in a row (and a structure behind them)."""
    W, H = 768, 384
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    light, main, dark = PALETTE[colour]
    d = ImageDraw.Draw(img, "RGBA")
    d.rounded_rectangle((6, 6, W - 6, H - 6), radius=48, fill=light + (235,), outline=dark + (255,), width=6)
    d.rounded_rectangle((20, 20, W - 20, H - 20), radius=38, outline=main + (255,), width=3)
    # a pink floor
    d.ellipse((40, H - 120, W - 40, H - 36), fill=main + (140,))
    if stage:
        st = fit(sprite(stage), W - 120, H - 150)
        img.alpha_composite(st, ((W - st.width) // 2, H - 90 - st.height))
    n = len(specs)
    slot = (W - 80) / max(1, n)
    for i, spec in enumerate(specs):
        size = min(slot * 0.95, 230 if not stage else 170)
        spr = fit(sprite(spec), size, size)
        x = int(40 + slot * i + (slot - spr.width) / 2)
        img.alpha_composite(spr, (x, H - 56 - spr.height))
    rng = random.Random(seed)
    for _ in range(5):
        hrt = versiering(colour, 0, rng.choice((0.8, 1.0)))
        img.alpha_composite(hrt, (rng.randint(40, W - 70), rng.randint(30, 80)))
    for x, y in ((34, 30), (W - 76, 30), (34, H - 80), (W - 76, H - 80)):
        img.alpha_composite(versiering(colour, 1, 1.4) if colour == "nacht" else knabbel(40, rng.randint(-20, 20)), (x, y))
    return img


def header(title, portrait, colour, sub=None):
    """1024x160: a ribbon with a round portrait on the left and the section title (and a 'komt na' line). The letters are
    big (6 px per font pixel, the 'komt na' line 4) so they stay readable at the quest book's normal zoom."""
    W, H = 1024, 160
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    light, main, dark = PALETTE[colour]
    d = ImageDraw.Draw(img, "RGBA")
    ribbon(d, 84, 22, W - 10, H - 22, PALETTE[colour], tails=False, stitch=not sub)
    d.ellipse((4, 4, H - 4, H - 4), fill=light + (255,), outline=dark + (255,), width=6)
    if portrait:
        spr = fit(sprite(portrait), 122, 122)
        img.alpha_composite(spr, (H // 2 - spr.width // 2, H // 2 - spr.height // 2))
    x0, room = 174, W - 174 - 72
    crop = lambda t: t.crop(t.getbbox())  # noqa: E731
    for scale in (6, 5, 4, 3):
        txt = crop(fancy_text(title, scale, (255, 255, 255), dark, shadow=False))
        if txt.width <= room:
            break
    lines = [txt]
    if sub:   # the longest text that fits, as big as possible ('Komt na: X (chapter)', else 'Komt na: X')
        subs = [sub] if isinstance(sub, str) else sub
        scale, sub = next(((sc, t) for sc in (4, 3) for t in subs if text_mask(t, sc).width < room), (2, subs[-1]))
        lines.append(crop(fancy_text(sub, scale, (255, 246, 200), dark, shadow=False)))
    gap = 6
    y = H // 2 - (sum(t.height for t in lines) + gap * (len(lines) - 1)) // 2
    for t in lines:
        img.alpha_composite(t, (x0, y))
        y += t.height + gap
    hrt = versiering(colour, 0)
    img.alpha_composite(hrt, (W - 64 + (10 if colour == "nacht" else 0), H // 2 - hrt.height // 2))
    return img


def _dashed_ellipse(d, box, colour, width, dashes=14):
    """An ellipse drawn as dashes (an empty spot where a quest will come)."""
    step = 360 / dashes
    for i in range(dashes):
        d.arc(box, i * step, i * step + step * 0.55, fill=colour, width=width)


def _dashed_frame(d, box, colour, width, dash=26, gap=16):
    """A rectangle drawn as dashes."""
    x0, y0, x1, y1 = box
    for x in range(int(x0), int(x1), dash + gap):
        d.line((x, y0, min(x + dash, x1), y0), fill=colour, width=width)
        d.line((x, y1, min(x + dash, x1), y1), fill=colour, width=width)
    for y in range(int(y0), int(y1), dash + gap):
        d.line((x0, y, x0, min(y + dash, y1)), fill=colour, width=width)
        d.line((x1, y, x1, min(y + dash, y1)), fill=colour, width=width)


def slot(title, portrait, colour, lines):
    """1024x352 (bbq2, FTB_SLOT): a "to be continued" card at the end of a chapter. A ribbon like a section header, but the
    card under it is only a dashed outline with the lines of text and a row of empty, dashed quest spots with a question
    mark that fade away to the right: this part is still missing."""
    W, H = 1024, 352
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    light, main, dark = PALETTE[colour]
    d = ImageDraw.Draw(img, "RGBA")
    d.rounded_rectangle((14, 84, W - 14, H - 8), radius=30, fill=light + (70,))
    _dashed_frame(d, (14, 84, W - 14, H - 8), dark + (230,), 5)
    ribbon(d, 84, 22, W - 10, 138, PALETTE[colour], tails=False, stitch=False)
    d.ellipse((4, 4, 156, 156), fill=light + (255,), outline=dark + (255,), width=6)
    if portrait:
        spr = fit(sprite(portrait), 122, 122)
        img.alpha_composite(spr, (80 - spr.width // 2, 80 - spr.height // 2))
    crop = lambda t: t.crop(t.getbbox())  # noqa: E731
    room = W - 174 - 40
    for scale in (6, 5, 4, 3):
        txt = crop(fancy_text(title, scale, (255, 255, 255), dark, shadow=False))
        if txt.width <= room:
            break
    img.alpha_composite(txt, (174, 80 - txt.height // 2))
    # the lines, as big as the longest one allows
    scale = next((sc for sc in (3, 2) if all(text_mask(t, sc).width <= W - 250 for t in lines)), 2)
    y = 166
    for t in lines:
        line = crop(fancy_text(t, scale, (255, 255, 255), dark, shadow=False))
        img.alpha_composite(line, (184, y))
        y += 8 * scale + 8
    # empty quest spots, fading out: nothing here yet
    for i in range(6):
        fade = max(40, 235 - i * 38)
        cx, cy, r = 250 + i * 128, H - 46, 28
        _dashed_ellipse(d, (cx - r, cy - r, cx + r, cy + r), dark + (fade,), 5)
        q = crop(fancy_text("?", 4, light, dark, shadow=False))
        q.putalpha(q.getchannel("A").point(lambda v, f=fade: v * f // 255))
        img.alpha_composite(q, (cx - q.width // 2, cy - q.height // 2))
    return img


# ---------------------------------------------------------------------------------------------------------------------
def _stamp():
    try:
        return json.load(open(STAMP, encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def make_art(jobs, force=False):
    """jobs: {texture path under textures/ftbquests: (function name, args)}. Draws what changed; removes old pictures."""
    stamp, new_stamp, drawn = _stamp(), {}, 0
    for rel, (fn, args) in sorted(jobs.items()):
        key = hashlib.md5(json.dumps([ART_VERSION, fn, args], ensure_ascii=False).encode()).hexdigest()
        path = os.path.join(TEX, rel)
        new_stamp[rel] = key
        if not force and stamp.get(rel) == key and os.path.exists(path):
            continue
        img = {"title": title_banner, "welcome": welcome_picture, "header": header, "icon": face, "slot": slot,
               "vraag": vraagteken}[fn](*args)   # (guhpad: "vraag", a question-mark quest icon)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path, optimize=True)
        drawn += 1
        print("  drew", rel)
    for rel in stamp:
        if rel not in new_stamp and os.path.exists(os.path.join(TEX, rel)):
            os.remove(os.path.join(TEX, rel))
            print("  removed", rel)
    with open(STAMP, "w", encoding="utf-8") as f:
        json.dump(new_stamp, f, indent=1, sort_keys=True)
        f.write("\n")
    return drawn
