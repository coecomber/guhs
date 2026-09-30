"""
De Elf-Guhjestocht (2.9) - textures and block/item models: the elfstempel coin, guh-schaatsen (a flat icon and a real
3D skate for your feet), the stempelkaart, warme chocovet and a kommetje snert, the Elf-Guhjeskruisje (medal block),
the night lampion, the vuurkorf and the tray of warm cups.
"""
import math
import random

from PIL import Image, ImageDraw

T = (0, 0, 0, 0)


def px(rows, pal):
    img = Image.new("RGBA", (16, 16), T)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


def noise(img, amount, seed, only_opaque=True):
    rng = random.Random(seed)
    out = img.copy()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            p = img.getpixel((x, y))
            if only_opaque and p[3] == 0:
                continue
            d = rng.randint(-amount, amount)
            out.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in p[:3]) + (p[3],))
    return out


ELFSTEMPEL = [
    "................",
    ".....oooooo.....",
    "...ooOOOOOOoo...",
    "..oOOppOOppOOo..",
    "..oOpPPOOPPpOo..",
    ".oOOOPPPPPPOOOo.",
    ".oOOPPkPPkPPOOo.",
    ".oOOPPPPPPPPOOo.",
    ".oOOPrPPPPrPOOo.",
    ".oOOOPPmmPPOOOo.",
    "..oOOOPPPPOOOo..",
    "..oOOOOOOOOOOo..",
    "...ooOOOOOOoo...",
    ".....oooooo.....",
    "................",
    "................",
]
SCHAATS = [
    "................",
    "................",
    "..bbbb..........",
    "..bPPbb.........",
    "..bPwPb.........",
    "..bPPwPb........",
    "..bPwPPbb.......",
    "..bPPwPPPbb.....",
    "..bPPPPPPPPbbb..",
    "..bPPPPPPPPPPPb.",
    "..bbbbbbbbbbbbb.",
    "...g.......g....",
    "..sSSSSSSSSSSSs.",
    ".s...........ss.",
    "..............s.",
    "................",
]
KAART = [
    "................",
    ".cccccccccccccc.",
    ".cCCCCCCCCCCCCc.",
    ".cCoCoCoCoCoCCc.",
    ".cCCCCCCCCCCCCc.",
    ".cCoCoCxCxCxCCc.",
    ".cCCCCCCCCCCCCc.",
    ".cCxCxCxCxCCCCc.",
    ".cCCCCCCCCCCCCc.",
    ".cCCCrrrrrrCCCc.",
    ".cCCCCCCCCCCCCc.",
    ".cCCPPPCCCCCCCc.",
    ".cCCPkPCCCCCCCc.",
    ".cCCCCCCCCCCCCc.",
    ".cccccccccccccc.",
    "................",
]
CHOCOVET = [
    "......w...w.....",
    ".....w...w......",
    "......w...w.....",
    "....wwwwwww.....",
    "...wWWWWWWWw....",
    "...pbbbbbbbp....",
    "...pPPPPPPPpppp.",
    "...pPkPPPkPp..p.",
    "...pPPPPPPPp..p.",
    "...pPrPmPrPpppp.",
    "...pPPPPPPPp....",
    "...pPPPPPPPp....",
    "....ppppppp.....",
    "..ddddddddddd...",
    "................",
    "................",
]
SNERT = [
    "...........s....",
    "..........s.....",
    ".....w...s......",
    "....w...s..w....",
    ".....w.s..w.....",
    "..ooooosooooo...",
    ".oGGgGGsGGgGGo..",
    ".oGgGRGGGgGGGo..",
    "..oGGGGgGGRGo...",
    "..oooooooooooo..",
    "...oOOOOOOOOo...",
    "....oOOOOOOo....",
    ".....oooooo.....",
    "................",
    "................",
    "................",
]


def elfstempel():
    return noise(px(ELFSTEMPEL, {"o": (180, 70, 20, 255), "O": (242, 128, 40, 255), "p": (255, 190, 214, 255), "P": (255, 160, 196, 255),
                                  "k": (40, 30, 50, 255), "r": (236, 100, 150, 255), "m": (150, 60, 100, 255)}), 8, 1)


def schaatsen_icoon():
    return noise(px(SCHAATS, {"b": (170, 60, 110, 255), "P": (250, 150, 196, 255), "w": (255, 255, 255, 255), "g": (120, 120, 130, 255),
                              "s": (190, 205, 220, 255), "S": (230, 240, 250, 255)}), 6, 2)


def stempelkaart():
    return noise(px(KAART, {"c": (170, 140, 100, 255), "C": (250, 240, 214, 255), "o": (242, 128, 40, 255), "x": (210, 200, 180, 255),
                            "r": (236, 100, 150, 255), "P": (255, 160, 196, 255), "k": (40, 30, 50, 255)}), 5, 3)


def chocovet():
    return noise(px(CHOCOVET, {"w": (240, 240, 250, 150), "W": (255, 250, 240, 255), "b": (110, 64, 36, 255), "p": (200, 90, 140, 255),
                               "P": (255, 170, 204, 255), "k": (40, 30, 50, 255), "r": (236, 110, 160, 255), "m": (150, 60, 100, 255),
                               "d": (170, 130, 90, 255)}), 5, 4)


def snert():
    return noise(px(SNERT, {"w": (240, 240, 250, 150), "o": (120, 80, 50, 255), "O": (160, 110, 70, 255), "G": (132, 170, 60, 255),
                            "g": (110, 150, 50, 255), "R": (200, 80, 70, 255), "s": (200, 205, 215, 255)}), 6, 5)


def block_tex(base, var, seed, pattern=None):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = rng.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))
    if pattern:
        pattern(img)
    return img


def lampion(aan):
    """Orange paper with ribs and a little white guh face (bright when lit, dusky when not)."""
    base = (255, 150, 50) if aan else (150, 90, 50)

    def pat(img):
        for y in range(16):
            for x in (0, 5, 10, 15):
                p = img.getpixel((x, y))
                img.putpixel((x, y), tuple(int(c * 0.8) for c in p[:3]) + (255,))
        face = (255, 250, 230) if aan else (200, 180, 160)
        for (x, y) in ((5, 6), (10, 6)):
            img.putpixel((x, y), (40, 30, 40, 255))
        for x in range(6, 10):
            img.putpixel((x, 10), (160, 60, 90, 255) if x in (7, 8) else face)
        for (x, y) in ((4, 8), (11, 8)):
            img.putpixel((x, y), (255, 120, 170, 255))
        for (x, y) in ((3, 1), (4, 1), (11, 1), (12, 1)):
            img.putpixel((x, y), face)
    return block_tex(base, 10 if aan else 6, 11 if aan else 12, pat)


def vuurkorf():
    def pat(img):
        for y in range(16):
            for x in range(16):
                if x % 4 == 0 or y % 5 == 0:
                    img.putpixel((x, y), (30, 30, 36, 255))
                else:
                    img.putpixel((x, y), (0, 0, 0, 0))
    return block_tex((60, 60, 66), 8, 21, pat)


def ijzer():
    return block_tex((58, 58, 64), 10, 22)


def hout():
    def pat(img):
        for x in range(16):
            for y in range(0, 16, 4):
                img.putpixel((x, y), (96, 64, 40, 255))
    return block_tex((140, 96, 60), 10, 23, pat)


def goud():
    def pat(img):
        for i in range(16):
            img.putpixel((i, i), (255, 240, 160, 255))
    return block_tex((236, 190, 60), 14, 24, pat)


def lint():
    def pat(img):
        for y in range(16):
            for x in (0, 1, 14, 15):
                img.putpixel((x, y), (255, 255, 255, 255))
    return block_tex((242, 120, 36), 8, 25, pat)


def gezichtje():
    """The medal's middle: a pink guh face on gold."""
    img = block_tex((236, 190, 60), 10, 26)
    d = ImageDraw.Draw(img)
    d.ellipse([2, 3, 13, 14], fill=(255, 170, 204, 255))
    d.ellipse([1, 1, 5, 6], fill=(255, 150, 190, 255))
    d.ellipse([10, 1, 14, 6], fill=(255, 150, 190, 255))
    for (x, y) in ((5, 8), (10, 8)):
        d.rectangle([x, y, x, y + 1], fill=(40, 30, 50, 255))
    d.line([(6, 11), (9, 11)], fill=(160, 60, 100, 255))
    return img


def kopje(inhoud):
    def pat(img):
        for y in range(0, 5):
            for x in range(16):
                img.putpixel((x, y), inhoud + (255,))
        for x in range(16):
            img.putpixel((x, 5), (255, 250, 240, 255))
        for (x, y) in ((5, 9), (10, 9)):
            img.putpixel((x, y), (40, 30, 50, 255))
    return block_tex((250, 160, 200), 8, 27 + inhoud[0] % 5, pat)


def schaats_atlas():
    """The 3D skate's texture: top-left boot (pink), top-right laces/cuff (white), bottom-left blade (silver),
    bottom-right sole (dark)."""
    img = Image.new("RGBA", (16, 16))
    rng = random.Random(28)
    for y in range(16):
        for x in range(16):
            if y < 8 and x < 8:
                c = (250, 150, 196)
            elif y < 8:
                c = (255, 255, 255) if (x + y) % 3 else (230, 230, 240)
            elif x < 8:
                c = (205, 220, 235) if y % 2 else (240, 248, 255)
            else:
                c = (80, 50, 70)
            d = rng.randint(-8, 8)
            img.putpixel((x, y), tuple(max(0, min(255, v + d)) for v in c) + (255,))
    return img


def el(frm, to, tex, faces=None, uv=None):
    faces = faces or ("north", "south", "east", "west", "up", "down")
    e = {"from": frm, "to": to, "faces": {}}
    for f in faces:
        e["faces"][f] = {"texture": tex}
        if uv:
            e["faces"][f]["uv"] = uv
    return e


def models(h):
    A = h.A
    w = h.w
    # --- items ---
    for name, img in (("elfstempel", elfstempel()), ("guh_schaatsen", schaatsen_icoon()), ("stempelkaart", stempelkaart()),
                      ("warme_chocovet", chocovet()), ("snert_kommetje", snert())):
        h.save(img, "item", f"{name}.png")
    for name in ("elfstempel", "stempelkaart", "warme_chocovet", "snert_kommetje"):
        h.item_model(name)
    h.save(schaats_atlas(), "item", "guh_schaatsen_3d.png")
    # the 3D skate (for SchaatsLaag: the "head" view): boot on a sole, a blade with a curl at the front (+z)
    skate = "guhs:item/guh_schaatsen_3d"
    w(f"{A}/models/item/guh_schaatsen_3d.json", {"textures": {"t": skate, "particle": skate}, "elements": [
        el([5.5, 2, 5.5], [10.5, 6.5, 11], "#t", uv=[0, 0, 8, 8]),               # the boot
        el([5.3, 5.5, 5.3], [10.7, 7, 8.5], "#t", uv=[8, 0, 16, 8]),              # the white cuff
        el([6.5, 1.3, 4.5], [9.5, 2, 12], "#t", uv=[8, 8, 16, 16]),               # the sole
        el([7.6, 0, 2.5], [8.4, 1.3, 14], "#t", uv=[0, 8, 8, 16]),                # the blade
        el([7.6, 1.3, 13.2], [8.4, 2.4, 14], "#t", uv=[0, 8, 8, 16]),             # the curl at the front
        el([7.6, 0.2, 3.0], [8.4, 1.3, 4.0], "#t", uv=[0, 8, 8, 16]),             # the stand at the back
    ], "display": {"head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}})
    w(f"{A}/models/item/guh_schaatsen.json", {
        "loader": "neoforge:separate_transforms",
        "base": {"parent": "minecraft:item/handheld", "textures": {"layer0": "guhs:item/guh_schaatsen"}},
        "perspectives": {"head": {"parent": "guhs:item/guh_schaatsen_3d"}}})
    # --- the night lampion (like the lampions of 2.x, lit or not) ---
    h.save(lampion(True), "block", "elftocht_lampion_aan.png")
    h.save(lampion(False), "block", "elftocht_lampion_uit.png")
    for aan in (True, False):
        tex = f"guhs:block/elftocht_lampion_{'aan' if aan else 'uit'}"
        for hang in (False, True):
            name = f"elftocht_lampion_{'aan' if aan else 'uit'}{'_hanging' if hang else ''}"
            w(f"{A}/models/block/{name}.json", {"parent": f"guhs:block/lampion_geel{'_hanging' if hang else ''}",
                                               "textures": {"paper": tex, "particle": tex, "cap": "minecraft:block/dark_oak_planks"}})
    w(f"{A}/blockstates/elftocht_lampion.json", {"variants": {
        f"hanging={str(hang).lower()},lit={str(aan).lower()}": {"model": f"guhs:block/elftocht_lampion_{'aan' if aan else 'uit'}{'_hanging' if hang else ''}"}
        for hang in (False, True) for aan in (False, True)}})
    w(f"{A}/models/item/elftocht_lampion.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/elftocht_lampion"}})
    h.save(lampion(True).resize((16, 16)), "item", "elftocht_lampion.png")
    # --- the vuurkorf ---
    h.save(vuurkorf(), "block", "elftocht_vuurkorf.png")
    h.save(ijzer(), "block", "elftocht_vuurkorf_ijzer.png")
    base = [
        el([3, 0, 3], [4, 6, 4], "#ijzer"), el([12, 0, 3], [13, 6, 4], "#ijzer"), el([7.5, 0, 12], [8.5, 6, 13], "#ijzer"),   # legs
        el([2, 6, 2], [14, 7, 14], "#ijzer"),                                                                                 # bottom
        el([2, 7, 2], [14, 13, 2.5], "#korf", ("north", "south")), el([2, 7, 13.5], [14, 13, 14], "#korf", ("north", "south")),
        el([2, 7, 2], [2.5, 13, 14], "#korf", ("east", "west")), el([13.5, 7, 2], [14, 13, 14], "#korf", ("east", "west")),
        el([4, 7, 5], [12, 9, 7], "#log"), el([5, 7, 9], [11, 9, 11], "#log"), el([6, 9, 4], [8, 11, 12], "#log"),
    ]
    tex = {"ijzer": "guhs:block/elftocht_vuurkorf_ijzer", "korf": "guhs:block/elftocht_vuurkorf", "log": "minecraft:block/oak_log",
           "particle": "guhs:block/elftocht_vuurkorf_ijzer"}
    w(f"{A}/models/block/elftocht_vuurkorf_uit.json", {"textures": tex, "elements": base})
    fire = [{"from": [3, 9, 8], "to": [13, 17, 8], "faces": {"north": {"texture": "#vuur"}, "south": {"texture": "#vuur"}}},
            {"from": [8, 9, 3], "to": [8, 17, 13], "faces": {"east": {"texture": "#vuur"}, "west": {"texture": "#vuur"}}}]
    tex_aan = dict(tex, vuur="minecraft:block/campfire_fire")
    w(f"{A}/models/block/elftocht_vuurkorf_aan.json", {"textures": tex_aan, "elements": base + fire, "render_type": "minecraft:cutout"})
    w(f"{A}/models/block/elftocht_vuurkorf_uit.json", {"textures": tex, "elements": base, "render_type": "minecraft:cutout"})
    w(f"{A}/blockstates/elftocht_vuurkorf.json", {"variants": {"lit=false": {"model": "guhs:block/elftocht_vuurkorf_uit"},
                                                              "lit=true": {"model": "guhs:block/elftocht_vuurkorf_aan"}}})
    w(f"{A}/models/item/elftocht_vuurkorf.json", {"parent": "guhs:block/elftocht_vuurkorf_aan"})
    # --- the tray of warm cups ---
    h.save(hout(), "block", "elftocht_kopjes_hout.png")
    h.save(kopje((110, 64, 36)), "block", "elftocht_kopje_chocovet.png")
    h.save(kopje((132, 170, 60)), "block", "elftocht_kopje_snert.png")
    for soort in ("chocovet", "snert"):
        els = [el([1, 0, 1], [15, 1, 15], "#hout")]
        for (x, z) in ((3, 3), (9, 4), (5, 9), (10, 10)):
            els.append(el([x, 1, z], [x + 3, 5, z + 3], "#kopje"))
        w(f"{A}/models/block/elftocht_kopjes_{soort}.json", {"textures": {"hout": "guhs:block/elftocht_kopjes_hout", "kopje": f"guhs:block/elftocht_kopje_{soort}",
                                                                          "particle": "guhs:block/elftocht_kopjes_hout"}, "elements": els})
    rot = {"north": 180, "east": 270, "south": 0, "west": 90}
    w(f"{A}/blockstates/elftocht_kopjes.json", {"variants": {
        f"facing={f},soort={soort}": {"model": f"guhs:block/elftocht_kopjes_{soort}", "y": r} for f, r in rot.items() for soort in ("chocovet", "snert")}})
    w(f"{A}/models/item/elftocht_kopjes.json", {"parent": "guhs:block/elftocht_kopjes_chocovet"})
    # --- the Elf-Guhjeskruisje ---
    h.save(goud(), "block", "elf_guhjeskruisje_goud.png")
    h.save(lint(), "block", "elf_guhjeskruisje_lint.png")
    h.save(gezichtje(), "block", "elf_guhjeskruisje_gezicht.png")
    els = [
        el([4, 0, 6], [12, 1.5, 10], "#hout"),                                      # the little stand
        el([7.5, 1.5, 7.5], [8.5, 4, 8.5], "#hout"),
        el([6, 11, 7.6], [10, 15.5, 8.4], "#lint"),                                 # the ribbon
        el([6.5, 3, 7.5], [9.5, 12, 8.5], "#goud"),                                 # the cross: upright
        el([3, 6, 7.5], [13, 9, 8.5], "#goud"),                                     # the cross: arms
        el([5.5, 5, 7.2], [10.5, 10, 8.8], "#gezicht", ("north", "south")),        # the guh face in the middle
    ]
    w(f"{A}/models/block/elf_guhjeskruisje.json", {"textures": {"hout": "guhs:block/elftocht_kopjes_hout", "goud": "guhs:block/elf_guhjeskruisje_goud",
                                                                "lint": "guhs:block/elf_guhjeskruisje_lint", "gezicht": "guhs:block/elf_guhjeskruisje_gezicht",
                                                                "particle": "guhs:block/elf_guhjeskruisje_goud"}, "elements": els})
    w(f"{A}/blockstates/elf_guhjeskruisje.json", {"variants": {f"facing={f}": {"model": "guhs:block/elf_guhjeskruisje", "y": r} for f, r in rot.items()}})
    w(f"{A}/models/item/elf_guhjeskruisje.json", {"parent": "guhs:block/elf_guhjeskruisje"})
    # loot: the medal and the deco blocks drop themselves
    for b in ("elf_guhjeskruisje", "elftocht_lampion", "elftocht_vuurkorf", "elftocht_kopjes"):
        h.self_drop(b)
