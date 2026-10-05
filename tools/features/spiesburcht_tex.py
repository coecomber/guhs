"""
Textures, block models and item icons of the Spiesburcht slice (PIL, 16x16 pixel art):
the verkoolde mikakop (standing and on a wall), the Guhbrouwketel (a pan on a little barbecue, with its fire and
five brews), the Knabbelbaken (pink glass, a grillkool base, the gloeister inside), and the items (grillspies,
grillspiespoeder, gloeister, gloeiend kooltje, the four Guhdrankjes in guh-shaped bottles).
"""
import math
import random

from PIL import Image, ImageDraw

from . import barbecuether_tex as bt

CHAR = (40, 33, 34)
CHAR_L = (72, 62, 60)
EMBER = (255, 120, 30)
EMBER_HOT = (255, 210, 90)
IRON = (54, 54, 62)
IRON_L = (96, 96, 108)
IRON_D = (26, 26, 30)

BROUWSELS = {"bouillon": (242, 194, 60), "vahoegheid": (240, 140, 180), "rookloop": (240, 120, 42), "sluipknabbel": (126, 154, 90),
             "guhsprong": (140, 210, 240),
             # bbq2 (CONTRACT_130 5.4, the order of Brouwsel.java): blubroom (sausdieren), pepervuur and peperzoet (toren-peper)
             "blubroom": (232, 162, 60), "pepervuur": (216, 50, 42), "peperzoet": (242, 122, 140)}


def noise16(base, var, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base[:3]) + (255,))
    return img


def cracks(img, colour, seed, n=3, length=(4, 9)):
    rng = random.Random(seed)
    px = img.load()
    s = img.width
    for _ in range(n):
        x, y = rng.uniform(0, s), rng.uniform(0, s)
        a = rng.uniform(0, 2 * math.pi)
        for _step in range(rng.randint(*length)):
            px[int(x) % s, int(y) % s] = colour + (255,)
            a += rng.uniform(-0.8, 0.8)
            x += math.cos(a)
            y += math.sin(a)
    return img


def paint(img, rows, pal, x0=0, y0=0):
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                px[x0 + x, y0 + y] = pal[ch]
    return img


# =====================================================================================================================
# blocks
# =====================================================================================================================
def mikakop(h):
    side = cracks(noise16(CHAR, 8, 51), EMBER, 52, 3)
    top = cracks(noise16(CHAR_L, 8, 53), EMBER, 54, 2)
    front = noise16(CHAR, 8, 55)
    face = ["................", "................", "................", ".bb..........bb.", "..bbb......bbb..", "...bb......bb...",
            "..rrrr....rrrr..", "..rRRr....rRRr..", "..rrrr....rrrr..", "................", "......kkkk......", "....kkkkkkkk....",
            "....kwkkkkwk....", ".....w....w.....", "................", "................"]
    paint(front, face, {"b": (14, 10, 12, 255), "r": (200, 30, 20, 255), "R": (255, 200, 80, 255), "k": (18, 12, 14, 255),
                        "w": (236, 230, 220, 255)})
    cracks(front, EMBER, 56, 1, (3, 5))
    h.save(front, "block", "verkoolde_mikakop_voor.png")
    h.save(side, "block", "verkoolde_mikakop_zij.png")
    h.save(top, "block", "verkoolde_mikakop_top.png")
    horn = noise16((24, 18, 20), 6, 57)
    h.save(horn, "block", "verkoolde_mikakop_hoorn.png")

    def elements(dy, dz):
        full = [0, 0, 16, 16]
        faces = {"north": {"uv": full, "texture": "#voor"}, "south": {"uv": full, "texture": "#zij"}, "east": {"uv": full, "texture": "#zij"},
                 "west": {"uv": full, "texture": "#zij"}, "up": {"uv": full, "texture": "#top"}, "down": {"uv": full, "texture": "#zij"}}
        horn = {f: {"uv": [0, 0, 4, 4], "texture": "#hoorn"} for f in ("north", "south", "east", "west", "up", "down")}
        return [{"from": [4, dy, 4 + dz], "to": [12, 8 + dy, 12 + dz], "faces": faces},
                {"from": [4.5, 8 + dy, 6 + dz], "to": [6, 10 + dy, 7.5 + dz], "faces": horn},
                {"from": [5, 10 + dy, 6.5 + dz], "to": [5.8, 11.2 + dy, 7.3 + dz], "faces": horn},
                {"from": [10, 8 + dy, 6 + dz], "to": [11.5, 10 + dy, 7.5 + dz], "faces": horn},
                {"from": [10.2, 10 + dy, 6.5 + dz], "to": [11, 11.2 + dy, 7.3 + dz], "faces": horn}]
    tex = {"particle": "guhs:block/verkoolde_mikakop_zij", "voor": "guhs:block/verkoolde_mikakop_voor", "zij": "guhs:block/verkoolde_mikakop_zij",
           "top": "guhs:block/verkoolde_mikakop_top", "hoorn": "guhs:block/verkoolde_mikakop_hoorn"}
    A = h.A
    h.w(f"{A}/models/block/verkoolde_mikakop.json", {"textures": tex, "elements": elements(0, 0)})
    h.w(f"{A}/models/block/verkoolde_mikakop_muur.json", {"textures": tex, "elements": elements(4, 4)})
    for name in ("verkoolde_mikakop", "verkoolde_mikakop_muur"):
        h.w(f"{A}/blockstates/{name}.json", {"variants": {f"facing={f}": ({"model": f"guhs:block/{name}", "y": r} if r else {"model": f"guhs:block/{name}"})
                                                          for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    h.w(f"{A}/models/item/verkoolde_mikakop.json", {"parent": "guhs:block/verkoolde_mikakop", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [1.0, 1.0, 1.0]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 4, 0], "scale": [1.0, 1.0, 1.0]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 8, 0], "scale": [2.0, 2.0, 2.0]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]}}})


def ketel(h):
    A = h.A
    pan = noise16(IRON, 8, 61)
    px = pan.load()
    for x in range(16):
        px[x, 0] = IRON_L + (255,)
        px[x, 15] = IRON_D + (255,)
    for x in (2, 13):
        for y in (3, 12):
            px[x, y] = (150, 150, 160, 255)                     # rivets
    paint(pan, ["................", "................", "................", "................", "....pp....pp....", "...pwkp..pwkp...",
                "...pkkp..pkkp...", "....pp....pp....", "................", "..mm........mm..", "......mmmm......", "................"],
          {"p": (238, 141, 173, 255), "w": (255, 255, 255, 255), "k": (30, 20, 40, 255), "m": (190, 60, 110, 255)}, 0, 2)
    h.save(pan, "block", "guhbrouwketel_pan.png")
    rim = noise16(IRON_L, 6, 62)
    h.save(rim, "block", "guhbrouwketel_rand.png")
    inside = noise16((34, 32, 36), 6, 63)
    h.save(inside, "block", "guhbrouwketel_binnen.png")
    poot = noise16(IRON_D, 5, 64)
    h.save(poot, "block", "guhbrouwketel_poot.png")
    aan = noise16((70, 30, 20), 20, 65)
    for _ in range(40):
        aan.putpixel((random.Random(_).randint(0, 15), random.Random(_ * 7).randint(0, 15)), EMBER_HOT + (255,))
    cracks(aan, EMBER, 66, 5)
    h.save(aan, "block", "guhbrouwketel_vuur_aan.png")
    uit = noise16((60, 56, 56), 10, 67)
    h.save(uit, "block", "guhbrouwketel_vuur_uit.png")
    for name, col in BROUWSELS.items():
        img = noise16(col, 10, 68 + len(name))
        rng = random.Random(len(name))
        for _ in range(9):                                       # bubbles
            x, y = rng.randint(1, 14), rng.randint(1, 14)
            img.putpixel((x, y), tuple(min(255, c + 50) for c in col) + (255,))
            img.putpixel((x + 1, y), tuple(min(255, c + 25) for c in col) + (255,))
        if name == "vahoegheid":
            paint(img, ["..w..w..", ".wkw.wkw", "..w..w..", "........", ".m....m.", "..mmmm.."],
                  {"w": (255, 255, 255, 255), "k": (40, 20, 40, 255), "m": (190, 60, 110, 255)}, 4, 5)
        h.save(img, "block", f"guhbrouwketel_{name}.png")

    def box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
        return {"from": frm, "to": to, "faces": {f: ({"texture": tex, "uv": uv} if uv else {"texture": tex}) for f in faces}}
    base = [box([1, 0, 1], [3, 8, 3], "#poot"), box([13, 0, 1], [15, 8, 3], "#poot"), box([1, 0, 13], [3, 8, 15], "#poot"),
            box([13, 0, 13], [15, 8, 15], "#poot"), box([2, 2, 2], [14, 3, 14], "#poot"), box([3, 3, 3], [13, 4, 13], "#vuur"),
            box([0, 7, 0], [16, 8, 16], "#binnen"),
            box([0, 8, 0], [16, 14, 1], "#pan", uv=[0, 5, 16, 11]), box([0, 8, 15], [16, 14, 16], "#pan", uv=[0, 5, 16, 11]),
            box([0, 8, 1], [1, 14, 15], "#pan", uv=[0, 5, 16, 11]), box([15, 8, 1], [16, 14, 15], "#pan", uv=[0, 5, 16, 11]),
            box([0, 14, 0], [16, 14.5, 1], "#rand"), box([0, 14, 15], [16, 14.5, 16], "#rand"), box([0, 14, 1], [1, 14.5, 15], "#rand"),
            box([15, 14, 1], [16, 14.5, 15], "#rand"), box([6.5, 11, -3], [9.5, 12, 0], "#poot")]
    for lit in ("aan", "uit"):
        h.w(f"{A}/models/block/guhbrouwketel_{lit}.json", {"textures": {
            "particle": "guhs:block/guhbrouwketel_pan", "poot": "guhs:block/guhbrouwketel_poot", "vuur": f"guhs:block/guhbrouwketel_vuur_{lit}",
            "binnen": "guhs:block/guhbrouwketel_binnen", "pan": "guhs:block/guhbrouwketel_pan", "rand": "guhs:block/guhbrouwketel_rand"},
            "elements": base})
    parts = []
    for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit, model in (("true", "aan"), ("false", "uit")):
            apply = {"model": f"guhs:block/guhbrouwketel_{model}"}
            if r:
                apply["y"] = r
            parts.append({"when": {"facing": f, "lit": lit}, "apply": apply})
    for i, name in enumerate(BROUWSELS):
        for v in (1, 2, 3):
            top = 8 + 1.9 * v
            h.w(f"{A}/models/block/guhbrouwketel_{name}_{v}.json", {"textures": {"particle": f"guhs:block/guhbrouwketel_{name}", "saus": f"guhs:block/guhbrouwketel_{name}"},
                                                                     "elements": [{"from": [1, 8, 1], "to": [15, top, 15], "faces": {
                                                                         "up": {"texture": "#saus"}, "north": {"texture": "#saus"}, "south": {"texture": "#saus"},
                                                                         "east": {"texture": "#saus"}, "west": {"texture": "#saus"}}}]})
            parts.append({"when": {"vulling": str(v), "brouwsel": str(i)}, "apply": {"model": f"guhs:block/guhbrouwketel_{name}_{v}"}})
    h.w(f"{A}/blockstates/guhbrouwketel.json", {"multipart": parts})
    h.w(f"{A}/models/item/guhbrouwketel.json", {"parent": "guhs:block/guhbrouwketel_aan"})
    return list(BROUWSELS)


def baken(h):
    A = h.A
    glas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = glas.load()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            px[x, y] = (255, 170, 210, 255) if edge else (255, 200, 225, 70)
    for (x, y) in ((2, 2), (3, 2), (2, 3), (12, 12), (13, 12)):
        px[x, y] = (255, 240, 250, 200)
    h.save(glas, "block", "knabbelbaken_glas.png")
    kern = Image.new("RGBA", (16, 16), (255, 150, 60, 255))
    paint(kern, ["......yy........", ".....yooy.......", ".y..yoOOoy...y..", "..yyoOOOOoyyy...", "...ooOOOOOoo....", "..oOOOwkOOOOo...",
                 ".yoOOkkOOkkOoy..", "..oOOOOOOOOOo...", "..oOOmOOOmOOo...", "...ooOmmmOoo....", "..yyoOOOOOoyy...", ".y..yoOOOoy..y..",
                 ".....yooy.......", "......yy........", "................", "................"],
          {"y": (255, 236, 150, 255), "o": (255, 150, 60, 255), "O": (255, 200, 110, 255), "w": (255, 255, 255, 255), "k": (60, 20, 30, 255),
           "m": (220, 70, 120, 255)})
    h.save(kern, "block", "knabbelbaken_kern.png")

    def box(frm, to, tex):
        return {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in ("north", "south", "east", "west", "up", "down")}}
    h.w(f"{A}/models/block/knabbelbaken.json", {"render_type": "minecraft:translucent", "textures": {
        "particle": "guhs:block/knabbelbaken_glas", "glas": "guhs:block/knabbelbaken_glas", "kern": "guhs:block/knabbelbaken_kern",
        "voet": "guhs:block/grillkool"}, "elements": [box([2, 0.1, 2], [14, 3, 14], "#voet"), box([3, 3, 3], [13, 13, 13], "#kern"),
                                                      box([0, 0, 0], [16, 16, 16], "#glas")]})
    h.w(f"{A}/blockstates/knabbelbaken.json", {"variants": {"": {"model": "guhs:block/knabbelbaken"}}})
    h.w(f"{A}/models/item/knabbelbaken.json", {"parent": "guhs:block/knabbelbaken"})


# =====================================================================================================================
# items
# =====================================================================================================================
def icon(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return paint(img, rows, pal)


PAL = {"k": (30, 20, 24, 255), "s": (170, 118, 64, 255), "S": (120, 80, 40, 255), "m": (150, 70, 34, 255), "M": (206, 128, 64, 255),
       "w": (176, 70, 50, 255), "W": (120, 40, 30, 255), "u": (240, 226, 190, 255), "o": (255, 150, 60, 255), "O": (255, 205, 110, 255),
       "y": (255, 238, 150, 255), "r": (220, 60, 40, 255), "p": (240, 140, 180, 255), "P": (255, 200, 222, 255), "g": (200, 200, 210, 255),
       "G": (150, 150, 165, 255), "b": (250, 250, 255, 200), "e": (60, 20, 30, 255), "c": (90, 30, 20, 255), "x": (40, 32, 32, 255)}

ICONS = {
    "grillspies": ["..............S.", ".............S..", "...........MmS..", "..........MmmM..", "..........mmM...", ".........uS.....",
                   "........uuu.....", ".......WwS......", "......WwwwW.....", "......wwwW......", ".....MmS........", "....MmmmM.......",
                   "....mmmM........", "...sS...........", "..sS............", ".s.............."],
    "grillspiespoeder": ["................", "................", "................", "................", "................", "........o.......",
                         ".......oOo......", "......omOMo.....", ".....oOoOoMo....", "....oMoOwoOoo...", "...oOomoOMoOmo..", "..oooOoMoooOooo.",
                         "..kkkkkkkkkkkkk.", "................", "................", "................"],
    "gloeister": ["................", ".......y........", "......yOy.......", "......yOy.......", ".....yOOOy......", "yyyyyOOOOOyyyyy.",
                  ".yOOOOwkOwkOOy..", "..yOOOkkOkkOy...", "...yOOOOOOOy....", "...yOOmmmOOy....", "..yOOOyyyOOOy...", "..yOOy...yOOy...",
                  ".yOy.......yOy..", ".yy.........yy..", "................", "................"],
    "gloeiend_kooltje": ["................", "................", "................", "......o.........", ".....oOo........", "....xxoxx.......",
                         "...xxoOoxxx.....", "..xxoOyOoxxx....", "..xoOOyOOoxx....", "..xxoOOOoxxx....", "...xxoooxxx.....", "....xxxxxx......",
                         "................", "................", "................", "................"],
}


def drankje(col):
    """A guh-shaped bottle: a round belly with two little ears and a cork, full of the brew, with a guh face."""
    c = col + (255,)
    light = tuple(min(255, v + 60) for v in col) + (255,)
    dark = tuple(max(0, v - 60) for v in col) + (255,)
    rows = ["................", "......cc........", "......cc........", ".g..gggggg..g...", ".gg.gbbbbg.gg...", "..gggbbbbggg....",
            "..gLLLLLLLLg....", ".gLLlLLLLLLLg...", ".gLewLLLLewLg...", ".gLeeLLLLeeLg...", ".gLLLLLLLLLLg...", ".gLpLLmmLLpLg...",
            ".gDLLLLLLLLDg...", "..gDDLLLLDDg....", "...gggggggg.....", "................"]
    pal = {"c": (150, 100, 60, 255), "g": (200, 220, 235, 255), "b": (230, 240, 250, 150), "L": c, "l": light, "D": dark,
           "e": (40, 20, 40, 255), "w": (255, 255, 255, 255), "p": (255, 150, 190, 255), "m": (150, 40, 90, 255)}
    return icon(rows, pal)


def items(h):
    for name, rows in ICONS.items():
        h.save(icon(rows, PAL), "item", f"{name}.png")
        h.item_model(name, parent="minecraft:item/handheld" if name == "grillspies" else "minecraft:item/generated")
    for name, key in (("drankje_van_vahoegheid", "vahoegheid"), ("rookloopdrankje", "rookloop"), ("sluipknabbeldrankje", "sluipknabbel"),
                      ("guhsprongdrankje", "guhsprong")):
        h.save(drankje(BROUWSELS[key]), "item", f"{name}.png")
        h.item_model(name)
    for egg in ("rookguh", "vonk_mika", "knekel_mika", "aangebrande_mika"):
        h.w(f"{h.A}/models/item/{egg}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def build(h):
    mikakop(h)
    ketel(h)
    baken(h)
    items(h)
