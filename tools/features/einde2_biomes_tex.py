"""
einde2, slice biomes: the textures of the blocks and items of the outer islands (called by einde2_biomes.build).

Everything is cheese in some state: the yellow crust of the Guheinde (kaaskorst) toasted into crumbs (kruimelrots,
kruimelzand), dried out and cracked (gebarsten korst), gone blue (schimmelkaas), aged to amber on the cliffs (klipkaas)
with golden crystals in it (kristalkaas), or gone purple under the knabbelkoor (koorkorst). Most are vanilla textures
painted from one colour to another (h.ramp), with our own crumbs, cracks, veins and crystals drawn on top. Every painter
is seeded from a NAME (kit.seed), never from an index.

    textures(h) -> writes them all; BLOCKS / ITEMS list what it wrote (the self-check of einde2_biomes reads them).
"""
import math
import random

from PIL import Image, ImageDraw

import features.einde2_kit as kit

CHEESE_DARK, CHEESE_LIGHT = (196, 132, 40), (255, 226, 132)        # (the kaaskorst of features/guheinde.py)
CRUMB_DARK, CRUMB_LIGHT = (126, 80, 34), (228, 178, 104)
SAND_DARK, SAND_LIGHT = (210, 164, 88), (250, 228, 164)
MOULD_DARK, MOULD_LIGHT = (176, 172, 142), (246, 242, 218)
VEIN, VEIN_DEEP = (84, 150, 158), (52, 102, 130)
CLIFF_DARK, CLIFF_LIGHT = (138, 76, 18), (238, 168, 60)
GOLD_DARK, GOLD, GOLD_LIGHT = (206, 132, 16), (255, 214, 84), (255, 250, 200)
KOOR_DARK, KOOR_LIGHT = (72, 32, 96), (200, 124, 216)
BLOK_DARK, BLOK_LIGHT = (116, 58, 138), (234, 172, 234)

BLOCKS, ITEMS = [], []


def _rng(name):
    return random.Random(kit.seed("tex/" + name))


def _put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c[:3]) + (img.getpixel((x, y))[3] if len(c) == 3 else c[3],))


def _shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def speckle(img, name, colours, chance):
    """Loose crumbs: single pixels in these colours."""
    r = _rng(name)
    out = img.copy()
    for y in range(out.height):
        for x in range(out.width):
            if out.getpixel((x, y))[3] and r.random() < chance:
                _put(out, x, y, r.choice(colours))
    return out


def cracks(img, name, dark, light, n=5):
    """Dry cracks: crooked dark lines that fork, each with a lit edge under it (tiles: the lines wrap around)."""
    r = _rng(name)
    out = img.copy()
    w = out.width
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(w)
        dx, dy = r.choice(((1, 0), (0, 1), (1, 1), (1, -1)))
        for step in range(r.randint(5, 11)):
            _put(out, x % w, y % w, dark)
            if out.getpixel((x % w, (y + 1) % w))[:3] != tuple(dark):
                _put(out, x % w, (y + 1) % w, light)
            if r.random() < 0.35:
                dx, dy = r.choice(((1, 0), (0, 1), (1, 1), (1, -1), (-1, 1)))
            x, y = x + dx, y + dy
    return out


def veins(img, name, n=4, blobs=5):
    """Blue mould: wandering veins with little pockets where they pool."""
    r = _rng(name)
    out = img.copy()
    w = out.width
    for _ in range(n):
        x, y = r.uniform(0, w), r.uniform(0, w)
        a = r.uniform(0, 6.28)
        for step in range(r.randint(6, 13)):
            _put(out, int(x) % w, int(y) % w, VEIN if r.random() < 0.7 else VEIN_DEEP)
            a += r.uniform(-0.9, 0.9)
            x, y = x + math.cos(a), y + math.sin(a)
    for _ in range(blobs):
        x, y = r.randrange(w), r.randrange(w)
        _put(out, x, y, VEIN_DEEP)
        for ox, oy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
            if r.random() < 0.5:
                _put(out, (x + ox) % w, (y + oy) % w, VEIN)
    return out


def crystals(img, name, n=5):
    """Kristalkaas grown into the rock: small golden lozenges with a white glint."""
    r = _rng(name)
    out = img.copy()
    spots = []
    while len(spots) < n:
        x, y = r.randint(2, 13), r.randint(2, 13)
        if all(abs(x - a) + abs(y - b) > 4 for a, b in spots):
            spots.append((x, y))
    for x, y in spots:
        tall = r.random() < 0.6
        cells = [(0, 0), (0, -1), (0, 1), (-1, 0), (1, 0)] + ([(0, -2)] if tall else []) + ([(1, 1)] if r.random() < 0.5 else [])
        for ox, oy in cells:
            _put(out, x + ox, y + oy, GOLD)
        _put(out, x + 1, y, GOLD_DARK)
        _put(out, x, y + 1, GOLD_DARK)
        _put(out, x, y - 1 if tall else y, GOLD_LIGHT)
        _put(out, x - 1, y, GOLD_LIGHT if r.random() < 0.5 else GOLD)
    return out


def cloud(name, alpha):
    """A puff of crumb cloud: soft cream with lit tops, a little shadow and a crumb or two caught in it."""
    r = _rng(name)
    img = Image.new("RGBA", (16, 16), (250, 243, 224, alpha))
    puffs = [(r.uniform(0, 16), r.uniform(0, 16), r.uniform(2.2, 4.2)) for _ in range(7)]
    for y in range(16):
        for x in range(16):
            v = 0.0
            for px, py, pr in puffs:
                # (wraps around, so the cloud has no seams)
                dx, dy = min(abs(x - px), 16 - abs(x - px)), min(abs(y - py), 16 - abs(y - py))
                v = max(v, 1 - math.hypot(dx, dy) / pr)
            c = _mix((232, 218, 190), (255, 253, 244), max(0.0, min(1.0, 0.35 + v * 0.9)))
            a = alpha if alpha == 255 else int(alpha - 34 + 50 * max(0.0, min(1.0, v * 1.4)))
            img.putpixel((x, y), c + (max(0, min(255, a)),))
    for _ in range(4):
        _put(img, r.randrange(16), r.randrange(16), (226, 184, 116))
    return img


def textures(h):
    ramp, vanilla = h.ramp, h.vanilla
    BLOCKS.clear()
    ITEMS.clear()

    def blok(name, img):
        h.save(img, "block", f"{name}.png")
        BLOCKS.append(name)

    def item(name, img):
        h.save(img, "item", f"{name}.png")
        ITEMS.append(name)

    toast = [(96, 58, 24), (112, 70, 30)]
    # --- Gatenkaasheuvels ---
    blok("korstgras", speckle(ramp(vanilla("block/short_grass"), (146, 92, 26), (250, 214, 104)), "korstgras", [(120, 72, 22)], 0.07))
    rots = speckle(speckle(ramp(vanilla("block/cobblestone"), CRUMB_DARK, CRUMB_LIGHT), "kruimelrots", toast, 0.05), "kruimelrots2", [(246, 210, 140)], 0.04)
    blok("kruimelrots", rots)
    blok("kruimelrots_stenen", speckle(ramp(vanilla("block/stone_bricks"), CRUMB_DARK, CRUMB_LIGHT), "kruimelrots_stenen", toast, 0.03))
    # --- Korstwoestijn ---
    korst = ramp(vanilla("block/end_stone"), (178, 116, 38), (240, 200, 108))
    blok("gebarsten_korst", cracks(korst, "gebarsten_korst", (112, 66, 20), (252, 222, 140)))
    zand = speckle(speckle(ramp(vanilla("block/sand"), SAND_DARK, SAND_LIGHT), "kruimelzand", [(186, 134, 62), (170, 118, 52)], 0.07),
                   "kruimelzand2", [(255, 240, 190)], 0.05)
    blok("kruimelzand", zand)
    blok("dorre_korststruik", ramp(vanilla("block/dead_bush"), (98, 58, 22), (204, 146, 72)))
    # --- Kruimelwolken ---
    blok("kruimelwolk", cloud("kruimelwolk", 196))
    blok("dichte_kruimelwolk", cloud("dichte_kruimelwolk", 255))
    # --- Schimmelkaasvlakte ---
    kaas = h.noise_tex((238, 232, 206), 5, kit.seed("tex/schimmelkaas"))
    blok("schimmelkaas", veins(kaas, "schimmelkaas"))
    blok("schimmelkaas_stenen", veins(ramp(vanilla("block/end_stone_bricks"), MOULD_DARK, MOULD_LIGHT), "schimmelkaas_stenen", n=2, blobs=3))
    zwam = vanilla("block/red_mushroom")
    blok("schimmelpaddenstoel", h.recolour(zwam, hue=0.5, sat=0.75, val=1.25, only=lambda hh, s, v: s > 0.45))
    blok("schimmelhoed", speckle(ramp(vanilla("block/brown_mushroom_block"), (34, 104, 128), (124, 222, 212)), "schimmelhoed", [(214, 250, 240), (190, 244, 232)], 0.06))
    blok("schimmelsteel", veins(ramp(vanilla("block/mushroom_stem"), (198, 192, 162), (248, 246, 228)), "schimmelsteel", n=1, blobs=2))
    blok("schimmelsteel_top", ramp(vanilla("block/mushroom_block_inside"), (190, 184, 152), (244, 240, 220)))
    # --- Kristalkaasklippen ---
    klip = speckle(ramp(vanilla("block/granite"), CLIFF_DARK, CLIFF_LIGHT), "klipkaas", [(255, 206, 110)], 0.03)
    blok("klipkaas", klip)
    blok("klipkaas_stenen", ramp(vanilla("block/deepslate_bricks"), (128, 68, 14), (240, 172, 64)))
    blok("kristalkaas_erts", crystals(klip, "kristalkaas_erts"))
    blok("kristalkaas_kristal", ramp(vanilla("block/amethyst_cluster"), GOLD_DARK, GOLD_LIGHT))
    blok("kristalkaas_blok", ramp(vanilla("block/amethyst_block"), (212, 138, 18), (255, 244, 170)))
    # --- Knabbelkoorbos ---
    blok("koorkorst", speckle(ramp(vanilla("block/end_stone"), (112, 72, 118), (220, 180, 204)), "koorkorst", [(246, 214, 130), (232, 190, 110)], 0.05))
    blok("knabbelkoorplant", ramp(vanilla("block/chorus_plant"), KOOR_DARK, KOOR_LIGHT))
    blok("knabbelkoorbloem", ramp(vanilla("block/chorus_flower"), (98, 40, 124), (255, 190, 244)))
    blok("knabbelkoorbloem_dood", ramp(vanilla("block/chorus_flower_dead"), (74, 38, 92), (206, 150, 214)))
    blok("knabbelkoorblok", ramp(vanilla("block/purpur_block"), BLOK_DARK, BLOK_LIGHT))
    blok("knabbelkoorpilaar", ramp(vanilla("block/purpur_pillar"), BLOK_DARK, BLOK_LIGHT))
    blok("knabbelkoorpilaar_top", ramp(vanilla("block/purpur_pillar_top"), BLOK_DARK, BLOK_LIGHT))
    blok("lichtstaafje", ramp(vanilla("block/end_rod"), (150, 74, 150), (255, 246, 226)))
    # --- items ---
    item("kristalkaas", ramp(vanilla("item/amethyst_shard"), (196, 118, 10), (255, 250, 196)))
    item("kristalkaas_brokje", ramp(vanilla("item/gold_nugget"), (204, 128, 14), (255, 248, 186)))
    item("knabbelkoorvrucht", ramp(vanilla("item/chorus_fruit"), (86, 36, 118), (250, 176, 240)))
    item("gepofte_knabbelkoorvrucht", ramp(vanilla("item/popped_chorus_fruit"), (156, 96, 150), (255, 236, 250)))


def preview(h, path):
    """A contact sheet of everything above (for looking at, not shipped)."""
    import os
    names = [("block", b) for b in BLOCKS] + [("item", i) for i in ITEMS]
    cols = 8
    sheet = Image.new("RGBA", (cols * 72, ((len(names) + cols - 1) // cols) * 72), (58, 30, 52, 255))
    for i, (kind, name) in enumerate(names):
        im = Image.open(os.path.join(h.TEX, kind, f"{name}.png")).convert("RGBA").crop((0, 0, 16, 16)).resize((64, 64), Image.NEAREST)
        sheet.paste(im, (i % cols * 72 + 4, i // cols * 72 + 4), im)
    sheet.save(path)
    ImageDraw.Draw(sheet)
