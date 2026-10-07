"""
einde2, slice deco: the textures and models of the four little blocks the decoration buildings are dressed with (called by
einde2_deco.build).

    einde2_deco_kool           a cabbage: a round head of pale green leaves with light veins (and a flat, squashed one)
    einde2_deco_windgong       a wind chime: a wooden disc, five tubes of pale gold, a clapper and an orange wind catcher
    einde2_deco_eeuwige_vlam   the eternal flame: a little fire in every colour of the dragons, eight frames
    einde2_deco_lotustegel     the white lotus tile of Pai Sho: a pale round tile with a pink lotus and a golden rim

Every painter is seeded from a NAME (kit.seed), never from an index. BLOCKS lists the ids (the self-check reads it).
"""
import colorsys
import json
import math
import os
import random

from PIL import Image

import features.einde2_kit as kit

BLOCKS = ["einde2_deco_kool", "einde2_deco_windgong", "einde2_deco_eeuwige_vlam", "einde2_deco_lotustegel"]
VLAM_FRAMES = 8

BLAD_DONKER, BLAD, BLAD_LICHT, NERF = (84, 140, 52), (128, 186, 78), (172, 216, 110), (214, 238, 168)
HOUT_DONKER, HOUT, HOUT_LICHT = (104, 72, 44), (150, 108, 66), (186, 142, 92)
BUIS_DONKER, BUIS, BUIS_LICHT = (166, 138, 70), (224, 198, 118), (252, 238, 178)
DOEK, DOEK_DONKER, PIJL = (236, 132, 44), (190, 92, 28), (92, 176, 222)
TEGEL_DONKER, TEGEL, TEGEL_LICHT = (206, 196, 176), (240, 234, 220), (255, 252, 244)
LOTUS, LOTUS_DONKER, GOUD = (242, 150, 186), (208, 98, 148), (226, 178, 66)


def _rng(name):
    return random.Random(kit.seed("deco_tex/" + name))


def _mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# =====================================================================================================================
# the kool
# =====================================================================================================================
def kool_zij():
    """The side of the head: leaves lying over each other in wavy bands, a light vein up the middle of each."""
    rng = _rng("kool_zij")
    img = Image.new("RGBA", (16, 16))
    fase = [rng.uniform(0, 6.28) for _ in range(4)]
    for x in range(16):
        for y in range(16):
            band = (y + 1.6 * math.sin(x * 0.8 + fase[(y // 5) % 4])) / 5.0
            t = band - math.floor(band)                       # 0 at the top edge of a leaf, 1 at its lower edge
            c = _mix(BLAD_LICHT, BLAD, min(1.0, t * 1.6)) if t < 0.75 else _mix(BLAD, BLAD_DONKER, (t - 0.75) * 4)
            if (x + (y // 5) * 3) % 6 == 0 and t > 0.15:
                c = _mix(c, NERF, 0.7)
            v = rng.randint(-6, 6)
            img.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    return img


def kool_top(geplet=False):
    """Seen from above: rings of leaves around a pale heart (squashed: the rings torn open, juice stains)."""
    rng = _rng("kool_top_plat" if geplet else "kool_top")
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = max(abs(dx), abs(dy)) * 0.55 + math.hypot(dx, dy) * 0.45
            a = math.atan2(dy, dx)
            ring = d + 0.5 * math.sin(a * 3 + d)
            t = (ring / 2.4) % 1.0
            c = _mix(BLAD_LICHT, BLAD, t * 1.3) if t < 0.7 else _mix(BLAD, BLAD_DONKER, (t - 0.7) * 3.3)
            if d < 1.6:
                c = NERF
            if abs(math.sin(a * 4)) < 0.16 and d > 2:
                c = _mix(c, NERF, 0.55)
            if geplet and rng.random() < 0.22:
                c = _mix(c, BLAD_DONKER, 0.8) if rng.random() < 0.6 else NERF
            v = rng.randint(-6, 6)
            img.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    return img


# =====================================================================================================================
# the windgong: one sheet (left half: the wood of the disc and the clapper; right half: the tubes; a strip of cloth below)
# =====================================================================================================================
def windgong():
    rng = _rng("windgong")
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            if x < 8 and y < 10:                               # wood, with a grain
                t = (math.sin(y * 1.7 + x * 0.3) + 1) / 2
                c = _mix(HOUT_DONKER, HOUT_LICHT, 0.25 + 0.6 * t)
                if (x, y) in ((1, 1), (6, 1), (1, 8), (6, 8)):
                    c = HOUT_DONKER
            elif x >= 8:                                       # a tube: round (light in the middle), a ring near each end
                kolom = (x - 8) % 4
                c = (BUIS_DONKER, BUIS, BUIS_LICHT, BUIS)[kolom]
                if y in (1, 14):
                    c = _mix(c, BUIS_DONKER, 0.7)
            else:                                              # the wind catcher: orange cloth with a blue arrow
                c = DOEK if (x + y) % 5 else DOEK_DONKER
                if (x, y) in ((3, 11), (4, 11), (2, 12), (3, 12), (4, 12), (5, 12), (3, 13), (4, 13), (3, 14), (4, 14)):
                    c = PIJL
            v = rng.randint(-5, 5)
            img.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    return img


def windgong_model():
    """The disc under the block above, five tubes of different lengths around a string with a clapper, and the cloth."""
    T = "#t"

    def doos(van, tot, uv_zij, uv_top):
        faces = {f: {"uv": uv_zij, "texture": T} for f in ("north", "south", "east", "west")}
        faces["up"] = {"uv": uv_top, "texture": T}
        faces["down"] = {"uv": uv_top, "texture": T}
        return {"from": van, "to": tot, "faces": faces}
    hout, hout_top = [0, 0, 8, 2], [0, 0, 8, 8]
    el = [doos([7.5, 13, 7.5], [8.5, 16, 8.5], [3, 0, 4, 3], [3, 0, 4, 1]),             # the hook
          doos([4, 12, 4], [12, 13, 12], hout, hout_top),                               # the disc
          doos([7.75, 5, 7.75], [8.25, 12, 8.25], [2, 2, 2.5, 9], [2, 2, 2.5, 2.5]),    # the string
          doos([6.75, 5.5, 6.75], [9.25, 6.5, 9.25], [0, 4, 2.5, 5], [0, 4, 2.5, 6.5]),  # the clapper
          doos([6.5, 1, 7.75], [9.5, 4.5, 8.25], [1, 10.5, 6, 15], [1, 10.5, 6, 11])]   # the wind catcher
    for i, (x, z, lengte) in enumerate(((4.5, 4.5, 8), (10.5, 4.5, 6), (10.5, 10.5, 9), (4.5, 10.5, 5), (7.5, 10.75, 7))):
        u = 8 + (i % 2) * 4
        el.append(doos([x, 12 - lengte, z], [x + 1, 12, z + 1], [u, 16 - lengte * 1.6, u + 4, 16], [u + 1, 0, u + 3, 1]))
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"t": "guhs:block/einde2_deco_windgong", "particle": "guhs:block/einde2_deco_windgong"}, "elements": el,
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0], "scale": [0.85, 0.85, 0.85]},
                        "fixed": {"translation": [0, -2, 0], "scale": [0.8, 0.8, 0.8]},
                        "ground": {"translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0.5, 0], "scale": [0.4, 0.4, 0.4]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0], "scale": [0.45, 0.45, 0.45]}}}


# =====================================================================================================================
# the eeuwige vlam: eight frames of a little fire whose colours climb through the rainbow
# =====================================================================================================================
def vlam_frame(i):
    rng = _rng(f"vlam_{i}")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    zwaai = math.sin(i / VLAM_FRAMES * 2 * math.pi)
    for y in range(16):
        h = (15 - y) / 14.0                                    # 0 at the foot of the flame, 1 at its tip
        if h < 0 or h > 1.02:
            continue
        breed = 4.4 * min(1.0, 0.45 + h * 3.0) * (1.0 - min(1.0, h)) ** 0.75
        midden = 7.5 + zwaai * 1.3 * h + 0.6 * math.sin(i * 1.7 + y * 0.9) * h
        for x in range(16):
            d = abs(x - midden)
            if d > breed + 0.2 or (d > breed - 0.9 and rng.random() < 0.35):
                continue
            kleur = (h * 0.82 + i / VLAM_FRAMES + 0.02 * d) % 1.0     # red at the foot, through to pink at the tip, turning
            r, g, b = colorsys.hsv_to_rgb(kleur, 0.72 - 0.35 * (1 - d / max(breed, 0.1)), 1.0)
            kern = max(0.0, 1.0 - d / max(breed * 0.55, 0.1)) * (1.0 - h * 0.6)
            c = _mix((int(r * 255), int(g * 255), int(b * 255)), (255, 252, 226), kern * 0.85)
            img.putpixel((x, y), c + (255,))
    for (x, y) in ((6, 15), (7, 15), (8, 15), (9, 15)):         # the embers it stands on
        img.putpixel((x, y), (255, 236, 170, 255))
    return img


def vlam_sheet():
    sheet = Image.new("RGBA", (16, 16 * VLAM_FRAMES), (0, 0, 0, 0))
    for i in range(VLAM_FRAMES):
        sheet.paste(vlam_frame(i), (0, 16 * i))
    return sheet


# =====================================================================================================================
# the lotustegel
# =====================================================================================================================
def lotustegel():
    """A round pale tile with a golden rim and a lotus of eight pink petals around a golden heart."""
    rng = _rng("lotustegel")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if d > 7.4:
                continue
            a = math.atan2(dy, dx)
            c = _mix(TEGEL_LICHT, TEGEL, d / 7.0)
            if d > 6.2:
                c = GOUD if d < 7.0 else _mix(GOUD, TEGEL_DONKER, 0.5)
            else:
                blad = abs(math.cos(a * 4))                    # eight petals
                if 1.2 < d < 1.6 + 3.6 * blad ** 0.7:
                    c = _mix(LOTUS, LOTUS_DONKER, max(0.0, (d - 2.5) / 3.0)) if blad > 0.25 else LOTUS_DONKER
                if d <= 1.3:
                    c = GOUD
            v = rng.randint(-4, 4)
            img.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    return img


def lotus_zij():
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), (GOUD if y % 4 == 0 else (TEGEL if (x // 2) % 2 else TEGEL_DONKER)) + (255,))
    return img


# =====================================================================================================================
def textures(h):
    A, w = h.A, h.w
    T = "guhs:block/einde2_deco_"
    # --- kool ---
    h.save(kool_zij(), "block", "einde2_deco_kool.png")
    h.save(kool_top(), "block", "einde2_deco_kool_top.png")
    h.save(kool_top(True), "block", "einde2_deco_kool_plat.png")

    def vlak(tex, uv):
        return {"uv": uv, "texture": tex}
    heel = [{"from": [3, 0, 3], "to": [13, 8, 13], "faces": {
                **{f: vlak("#zij", [3, 8, 13, 16]) for f in ("north", "south", "east", "west")},
                "up": vlak("#top", [3, 3, 13, 13]), "down": vlak("#top", [3, 3, 13, 13])}},
            {"from": [5, 8, 5], "to": [11, 9, 11], "faces": {
                **{f: vlak("#zij", [5, 0, 11, 1]) for f in ("north", "south", "east", "west")}, "up": vlak("#top", [5, 5, 11, 11])}},
            {"from": [2, 2, 4], "to": [3, 6, 12], "faces": {f: vlak("#zij", [4, 2, 12, 6]) for f in ("north", "south", "west", "up", "down")}},
            {"from": [13, 2, 4], "to": [14, 6, 12], "faces": {f: vlak("#zij", [4, 2, 12, 6]) for f in ("north", "south", "east", "up", "down")}},
            {"from": [4, 2, 2], "to": [12, 6, 3], "faces": {f: vlak("#zij", [4, 2, 12, 6]) for f in ("north", "east", "west", "up", "down")}},
            {"from": [4, 2, 13], "to": [12, 6, 14], "faces": {f: vlak("#zij", [4, 2, 12, 6]) for f in ("south", "east", "west", "up", "down")}}]
    w(f"{A}/models/block/einde2_deco_kool.json", {"parent": "minecraft:block/block", "textures": {
        "zij": T + "kool", "top": T + "kool_top", "particle": T + "kool"}, "elements": heel})
    w(f"{A}/models/block/einde2_deco_kool_plat.json", {"parent": "minecraft:block/block", "textures": {
        "zij": T + "kool", "top": T + "kool_plat", "particle": T + "kool"}, "elements": [
        {"from": [2, 0, 2], "to": [14, 1.5, 14], "faces": {
            **{f: vlak("#zij", [2, 14, 14, 15.5]) for f in ("north", "south", "east", "west")},
            "up": vlak("#top", [2, 2, 14, 14]), "down": vlak("#top", [2, 2, 14, 14])}},
        {"from": [5, 1.5, 4], "to": [11, 2.5, 10], "faces": {
            **{f: vlak("#zij", [5, 13, 11, 14]) for f in ("north", "south", "east", "west")}, "up": vlak("#top", [5, 4, 11, 10])}}]})
    draai = [{"model": "guhs:block/einde2_deco_kool", **({"y": y} if y else {})} for y in (0, 90, 180, 270)]
    plat = [{"model": "guhs:block/einde2_deco_kool_plat", **({"y": y} if y else {})} for y in (0, 90, 180, 270)]
    w(f"{A}/blockstates/einde2_deco_kool.json", {"variants": {"geplet=false": draai, "geplet=true": plat}})
    w(f"{A}/models/item/einde2_deco_kool.json", {"parent": "guhs:block/einde2_deco_kool"})
    # --- windgong ---
    h.save(windgong(), "block", "einde2_deco_windgong.png")
    w(f"{A}/models/block/einde2_deco_windgong.json", windgong_model())
    w(f"{A}/blockstates/einde2_deco_windgong.json", {"variants": {"": {"model": "guhs:block/einde2_deco_windgong"}}})
    w(f"{A}/models/item/einde2_deco_windgong.json", {"parent": "guhs:block/einde2_deco_windgong"})
    # --- eeuwige vlam (animated; its item shows the first frame) ---
    h.save(vlam_sheet(), "block", "einde2_deco_eeuwige_vlam.png")
    with open(os.path.join(h.TEX, "block", "einde2_deco_eeuwige_vlam.png.mcmeta"), "w", encoding="utf-8") as f:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, f, indent=2)
        f.write("\n")
    h.save(vlam_frame(0), "item", "einde2_deco_eeuwige_vlam.png")
    w(f"{A}/models/block/einde2_deco_eeuwige_vlam.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                          "textures": {"cross": T + "eeuwige_vlam"}})
    w(f"{A}/blockstates/einde2_deco_eeuwige_vlam.json", {"variants": {"": {"model": "guhs:block/einde2_deco_eeuwige_vlam"}}})
    h.item_model("einde2_deco_eeuwige_vlam")
    # --- lotustegel: a round tile made of two crossed slabs ---
    h.save(lotustegel(), "block", "einde2_deco_lotustegel.png")
    h.save(lotus_zij(), "block", "einde2_deco_lotustegel_zij.png")
    tegel = []
    for (x0, z0, x1, z1) in ((4, 3, 12, 13), (3, 4, 13, 12)):
        tegel.append({"from": [x0, 0, z0], "to": [x1, 1.5, z1], "faces": {
            **{f: vlak("#zij", [x0 if f in ("north", "south") else z0, 0, x1 if f in ("north", "south") else z1, 1.5]) for f in ("north", "south", "east", "west")},
            "up": vlak("#top", [x0, z0, x1, z1]), "down": vlak("#zij", [x0, z0, x1, z1])}})
    w(f"{A}/models/block/einde2_deco_lotustegel.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "top": T + "lotustegel", "zij": T + "lotustegel_zij", "particle": T + "lotustegel"}, "elements": tegel,
        "display": {"gui": {"rotation": [60, 225, 0], "translation": [0, 3, 0], "scale": [1.1, 1.1, 1.1]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                    "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, -7], "scale": [1.0, 1.0, 1.0]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 2], "scale": [0.45, 0.45, 0.45]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 0], "scale": [0.5, 0.5, 0.5]}}})
    w(f"{A}/blockstates/einde2_deco_lotustegel.json", {"variants": {"": {"model": "guhs:block/einde2_deco_lotustegel"}}})
    w(f"{A}/models/item/einde2_deco_lotustegel.json", {"parent": "guhs:block/einde2_deco_lotustegel"})


def preview(out):
    """A sheet of every texture, eight times enlarged (python tools/features/einde2_deco_tex.py <png>)."""
    tiles = [kool_zij(), kool_top(), kool_top(True), windgong(), lotustegel(), lotus_zij()] + [vlam_frame(i) for i in range(VLAM_FRAMES)]
    sheet = Image.new("RGBA", (len(tiles) * 136 + 8, 144), (60, 50, 70, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t.resize((128, 128), Image.NEAREST), (8 + i * 136, 8))
    sheet.save(out)
