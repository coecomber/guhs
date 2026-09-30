"""
Mika meppen (whack-a-Mika) in the Mika-mephal: resources for nl.juiced.guhs.feature.meppen.

  - blocks: the board holes (mika_mep_gat), the heads that pop up (mika_mep_kop: Mika / gouden Mika / guh, and squashed
    after a whack) and the Mika trophy (mika_mep_trofee); items: the mepmunt and the loaned Mika-mephamer
  - the Mepguh (texture + name), the Mika-hunter outfit (hat, vest with pockets, medal: bones, textures, icons)
  - the Mika-mephal structure (a fairground hall behind a giant guh-face facade) with a geometry self-check,
    and a small test room for the GameTests (mika_mep_proefhal)
  - advancements, FTB quests and all texts
"""
import json
import math
import random

import numpy as np
from PIL import Image

HALL = "mika_mep_hal"
SALT = 20240120 + 2

# --- palettes ---------------------------------------------------------------------------------------------------------------
HEADS = {  # kind: base, shade, dark (brows, mouth), eye
    "mika": ((222, 166, 186), (196, 138, 160), (52, 20, 34), (228, 48, 44)),
    "goud": ((250, 206, 72), (214, 164, 38), (112, 68, 12), (228, 48, 44)),
    "guh": ((244, 172, 204), (220, 142, 180), (70, 30, 50), (28, 22, 30)),
}


def _noise(base, rng, var=7, size=16):
    a = np.zeros((size, size, 4), np.float32)
    a[..., :3] = base
    a[..., :3] += rng.normal(0, var / 2, (size, size, 1))
    a[..., 3] = 255
    return a


def _img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def _px(a, pts, colour):
    for (x, y) in pts:
        if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
            a[y, x, :3] = colour
            a[y, x, 3] = 255


def head_textures(h):
    """face / bonk face / side / top / ear textures for every head kind."""
    for kind, (base, shade, dark, eye) in HEADS.items():
        rng = np.random.default_rng(len(kind) * 31 + 7)
        # --- the face ---
        a = _noise(base, rng)
        a[14:, :, :3] = np.array(shade, np.float32)[None, None, :] + rng.normal(0, 3, (2, 16, 1))
        if kind == "guh":
            for ex in (3, 10):                                         # round black eyes with a shine
                _px(a, [(ex + dx, 6 + dy) for dx in range(3) for dy in range(3)], eye)
                _px(a, [(ex, 6)], (255, 255, 255))
            _px(a, [(1, 10), (2, 10), (1, 11), (2, 11), (13, 10), (14, 10), (13, 11), (14, 11)], (255, 120, 170))  # blush
            _px(a, [(7, 10), (8, 10)], (200, 90, 130))                 # nose
            _px(a, [(5, 11), (6, 12), (7, 12), (8, 12), (9, 12), (10, 11)], dark)   # a happy smile
        else:
            _px(a, [(2, 3), (3, 3), (4, 4), (5, 4), (6, 5), (13, 3), (12, 3), (11, 4), (10, 4), (9, 5)], dark)   # angry brows
            for ex in (3, 9):                                          # red cat eyes with a slit
                _px(a, [(ex + dx, 6 + dy) for dx in range(4) for dy in range(4) if (dx, dy) not in ((0, 0), (3, 0), (0, 3), (3, 3))], eye)
                _px(a, [(ex + (1 if ex == 9 else 2), 6 + dy) for dy in range(4)], (20, 8, 12))
                _px(a, [(ex + (2 if ex == 9 else 1), 7)], (255, 220, 220))
            _px(a, [(4, 11), (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12), (11, 11)], dark)   # a smug grin
            _px(a, [(9, 13)], (255, 255, 255))                          # with a little fang
            if kind == "goud":
                _px(a, [(1, 1), (14, 2), (12, 14), (2, 13), (7, 1)], (255, 252, 220))
        h.save(_img(a), "block", f"mika_mep_{kind}_face.png")
        # --- squashed (only rows 5..10 show on the flat head) ---
        b = _noise(base, rng)
        for ex in (3, 10):                                              # x_x
            _px(b, [(ex, 6), (ex + 2, 6), (ex + 1, 7), (ex, 8), (ex + 2, 8)], dark if kind != "guh" else (28, 22, 30))
        if kind == "guh":
            _px(b, [(3, 9), (12, 9), (3, 10), (12, 10)], (110, 190, 255))     # tears: you whacked a guh!
            _px(b, [(6, 10), (7, 9), (8, 9), (9, 10)], dark)
        else:
            _px(b, [(5, 10), (6, 9), (7, 10), (8, 9), (9, 10), (10, 9)], dark)  # a wobbly mouth
        h.save(_img(b), "block", f"mika_mep_{kind}_bonk.png")
        # --- sides, top, ears ---
        s = _noise(base, rng)
        s[12:, :, :3] = np.array(shade, np.float32)[None, None, :]
        if kind == "goud":
            _px(s, [(3, 3), (11, 6), (6, 10)], (255, 252, 220))
        h.save(_img(s), "block", f"mika_mep_{kind}_side.png")
        t = _noise(base, rng)
        _px(t, [(7, 7), (8, 7), (7, 8)], shade)
        h.save(_img(t), "block", f"mika_mep_{kind}_top.png")
        e = _noise(base, rng)
        e[3:13, 4:12, :3] = (255, 130, 175) if kind == "guh" else (150, 50, 80) if kind == "mika" else (200, 120, 20)
        h.save(_img(e), "block", f"mika_mep_{kind}_ear.png")


def _face(tex, uv=None, cull=None):
    f = {"texture": tex}
    if uv:
        f["uv"] = uv
    if cull:
        f["cullface"] = cull
    return f


def head_elements(kind, bonk, scale=1.0, y0=0.0):
    """Model elements of a head, facing north (the face on the north side)."""
    def sc(p):  # scale around the block centre (x, z) and from y0 up
        return [8 + (p[0] - 8) * scale, y0 + p[1] * scale, 8 + (p[2] - 8) * scale]

    if bonk:
        head = {"from": sc([1, 0, 2]), "to": sc([15, 5, 14]), "faces": {
            "north": _face("#bonk", [0, 5, 16, 11]), "south": _face("#side", [0, 5, 16, 11]), "east": _face("#side", [0, 5, 16, 11]),
            "west": _face("#side", [0, 5, 16, 11]), "up": _face("#top"), "down": _face("#side", cull="down")}}
        ears = [[0, 3, 6, 2, 4.5, 10], [14, 3, 6, 16, 4.5, 10]]
    else:
        head = {"from": sc([2, 0, 3]), "to": sc([14, 12, 13]), "faces": {
            "north": _face("#face", [0, 0, 16, 16]), "south": _face("#side"), "east": _face("#side"), "west": _face("#side"),
            "up": _face("#top"), "down": _face("#side", cull="down" if scale == 1 else None)}}
        if kind == "guh":
            ears = [[1.5, 10.5, 6, 5, 14, 8.5], [11, 10.5, 6, 14.5, 14, 8.5]]
        else:
            ears = [[2.5, 12, 6.5, 5.5, 14.5, 9], [10.5, 12, 6.5, 13.5, 14.5, 9]]
    els = [head]
    for e in ears:
        els.append({"from": sc(e[:3]), "to": sc(e[3:]), "faces": {d: _face("#ear") for d in ("north", "south", "east", "west", "up", "down")}})
    if not bonk and kind != "guh":                                     # little pointy tips on Mika ears
        for e in ears:
            els.append({"from": sc([e[0] + 0.75, e[4], e[2] + 0.75]), "to": sc([e[3] - 0.75, e[4] + 1.5, e[5] - 0.75]),
                        "faces": {d: _face("#ear") for d in ("north", "south", "east", "west", "up", "down")}})
    return els


def blocks_and_items(h):
    head_textures(h)
    A = h.A
    FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    # --- the heads ---
    variants = {}
    for kind in HEADS:
        for bonk in (False, True):
            name = f"mika_mep_kop_{kind}" + ("_bonk" if bonk else "")
            tex = {k: f"guhs:block/mika_mep_{kind}_{k}" for k in ("face", "bonk", "side", "top", "ear")}
            tex["particle"] = f"guhs:block/mika_mep_{kind}_side"
            h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                   "textures": tex, "elements": head_elements(kind, bonk)})
            for facing, y in FACINGS:
                variants[f"bonk={str(bonk).lower()},facing={facing},kop={kind}"] = {"model": f"guhs:block/{name}", **({"y": y} if y else {})}
    h.w(f"{A}/blockstates/mika_mep_kop.json", {"variants": variants})
    # --- the hole: a gold rim around a dark hole, pink sides with golden rivets ---
    rng = np.random.default_rng(99)
    top = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            c = (250, 206, 70) if d == 0 else (200, 140, 30) if d == 1 else (236, 150, 190) if d == 2 else None
            if c is None:
                r = math.hypot(x - 7.5, y - 7.5)
                k = max(0.0, 1 - r / 6.5)
                c = (60 - 40 * k, 30 - 20 * k, 46 - 30 * k)
            top[y, x, :3] = c
            top[y, x, 3] = 255
    top[..., :3] += rng.normal(0, 2, (16, 16, 1))
    h.save(_img(top), "block", "mika_mep_gat_top.png")
    side = _noise((236, 150, 190), rng, 6)
    side[0:2, :, :3] = (250, 206, 70)
    side[2, :, :3] = (200, 140, 30)
    side[14:, :, :3] = (200, 110, 150)
    _px(side, [(2, 8), (13, 8)], (250, 206, 70))
    h.save(_img(side), "block", "mika_mep_gat_side.png")
    h.w(f"{A}/models/block/mika_mep_gat.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/mika_mep_gat_top", "side": "guhs:block/mika_mep_gat_side", "bottom": "guhs:block/mika_mep_gat_side"}})
    h.w(f"{A}/blockstates/mika_mep_gat.json", {"variants": {"": {"model": "guhs:block/mika_mep_gat"}}})
    h.w(f"{A}/models/item/mika_mep_gat.json", {"parent": "guhs:block/mika_mep_gat"})
    # --- the trophy: a small Mika head on a golden plinth with a red cushion ---
    els = [{"from": [3, 0, 3], "to": [13, 3, 13], "faces": {d: _face("#gold") for d in ("north", "south", "east", "west", "up", "down")}},
           {"from": [4, 3, 4], "to": [12, 4, 12], "faces": {d: _face("#cushion") for d in ("north", "south", "east", "west", "up", "down")}}]
    els += head_elements("mika", False, scale=0.7, y0=4)
    h.w(f"{A}/models/block/mika_mep_trofee.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": "minecraft:block/gold_block", "gold": "minecraft:block/gold_block", "cushion": "minecraft:block/red_wool",
        **{k: f"guhs:block/mika_mep_mika_{k}" for k in ("face", "side", "top", "ear")}}, "elements": els,
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}}})
    h.w(f"{A}/blockstates/mika_mep_trofee.json", {"variants": {f"facing={f}": {"model": "guhs:block/mika_mep_trofee", **({"y": y} if y else {})}
                                                          for f, y in FACINGS}})
    h.w(f"{A}/models/item/mika_mep_trofee.json", {"parent": "guhs:block/mika_mep_trofee"})
    h.self_drop("mika_mep_trofee")
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:mika_mep_trofee"])

    # --- items: the mepmunt (a gold coin with a Mika face) and the loaned mallet ---
    coin = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r <= 7.2:
                coin[y, x] = (*((150, 95, 15) if r > 6.3 else (220, 165, 40) if r > 5.4 else (250, 208, 70)), 255)
    _px(coin, [(5, 5), (6, 6), (10, 5), (9, 6)], (110, 60, 10))        # brows
    _px(coin, [(5, 7), (6, 7), (9, 7), (10, 7)], (220, 40, 40))         # red eyes
    _px(coin, [(5, 10), (6, 11), (7, 11), (8, 11), (9, 11), (10, 10)], (110, 60, 10))
    _px(coin, [(8, 12)], (255, 255, 255))
    _px(coin, [(4, 3), (3, 4)], (255, 245, 200))
    h.save(_img(coin), "item", "mepmunt.png")
    h.item_model("mepmunt")
    ham = np.zeros((16, 16, 4), np.float32)
    for y in range(16):                               # the handle: bottom left to the middle
        for x in range(16):
            u, v = (x - y) / 2.0, (x + y) / 2.0       # u: along the handle, v: across
            if -7 <= u <= 1 and abs(v - 7.5) <= 0.8:
                ham[y, x] = (*((250, 205, 60) if -5.8 <= u <= -5.0 else (150, 95, 55)), 255)
    for y in range(16):                               # the soft head: a fat pink-and-white roll across the top of the handle
        for x in range(16):
            u, v = (x - y) / 2.0 - 3.0, (x + y) / 2.0 - 7.5
            if abs(u) <= 2.1 and abs(v) <= 3.9:
                edge = abs(u) > 1.5 or abs(v) > 3.3
                ham[y, x] = (*((120, 40, 80) if edge else (255, 150, 200) if int(v + 5) % 3 else (255, 240, 248)), 255)
    ham = _img(ham)
    h.save(ham, "item", "mika_mep_hamer.png")
    h.item_model("mika_mep_hamer", parent="minecraft:item/handheld")


# --- the Mika-hunter outfit ------------------------------------------------------------------------------------------------
H = [0, 6, -2]
BONES = {
    # a Tyrolean hunter's hat: brim, crown, a red band and a pink feather
    "outfit_jagershoed": ("head", H, "jagershoed", [([-5.5, 15, -10.8], [11, 0.5, 9.2], 0), ([-3.5, 15.5, -9.2], [7, 3, 6], 0),
                                                  ([-2.5, 18.5, -8.6], [5, 1, 4.8], 0)]),
    "outfit_jagershoed_band": ("head", H, "jagershoed_band", [([-3.5, 15.5, -9.2], [7, 0.9, 6], 0.1)]),
    "outfit_jagershoed_veer": ("head", H, "jagershoed_veer", [([3.4, 16, -6.8], [0.5, 4.5, 0.5], 0), ([3.4, 20, -6.3], [0.5, 1.5, 2], 0),
                                                            ([3.4, 18.5, -5.5], [0.5, 1.5, 1], 0)]),
    # pockets on the sides of the vest (the vest itself is the suit)
    "outfit_mepvest_zakken": ("body", [0, 6, 6], "mepvest_zakken", [([6.4, 3, 0.5], [0.9, 3, 3.5], 0), ([6.4, 3, 5.5], [0.9, 3, 3.5], 0),
                                                                    ([-7.3, 3, 0.5], [0.9, 3, 3.5], 0), ([-7.3, 3, 5.5], [0.9, 3, 3.5], 0)]),
    # a gold medal on a pink ribbon, under the chin
    "outfit_mepmedaille": ("head", H, "mepmedaille_lint", [([-4.5, 1.6, -11.5], [9, 0.6, 0.6], 0), ([-1.9, -0.4, -11.7], [1, 2.2, 0.5], 0),
                                                          ([0.9, -0.4, -11.7], [1, 2.2, 0.5], 0)]),
    "outfit_mepmedaille_munt": ("head", H, "mepmedaille", [([-1.6, -3.2, -12], [3.2, 3, 0.6], 0)]),
}
CLOTHES = ["mikajager_hoed", "mikajager_vest", "mikamepper_medaille"]


def clothes(rng, v):
    def vest():
        a = v.fabric((120, 140, 70), rng, 8)          # olive hunter's vest with pockets, stitches and gold buttons
        for y0 in (6, 18):
            a[y0:y0 + 7, 3:12] = (95, 112, 52)
            a[y0:y0 + 7, 20:29] = (95, 112, 52)
            a[y0, 3:12] = (70, 85, 40)
            a[y0, 20:29] = (70, 85, 40)
        a[:, 15:17] = (85, 100, 45)
        for y in range(3, 32, 7):
            a[y:y + 2, 15:17] = (250, 205, 60)
        return a

    def medal():
        a = v.fabric((245, 200, 60), rng, 6)
        a[10:22, 10:22] = (255, 225, 110)
        a[13:16, 12:15] = (220, 50, 50)               # the Mika's red eyes on the medal
        a[13:16, 17:20] = (220, 50, 50)
        a[19:21, 12:20] = (130, 80, 10)
        return a

    return {
        "mikajager_hoed": {"jagershoed": lambda: v.fabric((70, 105, 60), rng, 8),
                           "jagershoed_band": lambda: v.stripes((210, 40, 70), (245, 170, 200), rng, 2),
                           "jagershoed_veer": lambda: v.fabric((255, 120, 190), rng, 10)},
        "mikajager_vest": {"suit": vest, "mepvest_zakken": lambda: v.fabric((95, 112, 52), rng, 6)},
        "mikamepper_medaille": {"mepmedaille_lint": lambda: v.stripes((230, 70, 140), (250, 245, 245), rng, 3),
                                "mepmedaille": medal},
    }


def icons(ic):
    hat = ic.icon(ic.pad(["...........pp...", "..........pp....", ".....aaaaap.....", "....abbbbba.....", "....abbbbba.....",
                          "....rrrrrrr.....", "..aabbbbbbbaa...", "aaaaaaaaaaaaaaa."]),
                  {"a": (45, 70, 40), "b": (70, 105, 60), "r": (210, 40, 70), "p": (255, 120, 190)})
    vest = ic.icon(ic.pad(["...aa......aa...", "..abba....abba..", ".abbbba..abbbba.", ".abbbbaggabbbba.", ".abbbba..abbbba.",
                           ".accccaggacccca.", ".abbbba..abbbba.", ".abbbbaggabbbba.", ".accccaa.acccca.", ".abbbba..abbbba.",
                           ".aaaaaa..aaaaaa."]),
                   {"a": (60, 75, 35), "b": (120, 140, 70), "c": (95, 112, 52), "g": (250, 205, 60)})
    medal = ic.icon(ic.pad(["..pw......wp....", "...pw....wp.....", "....pw..wp......", ".....pwwp.......", "......pp........",
                            ".....gggg.......", "....gyyyyg......", "...gyryyryg.....", "...gyyyyyyg.....", "...gyykkyyg.....",
                            "....gyyyyg......", ".....gggg......."]),
                    {"p": (230, 70, 140), "w": (250, 245, 245), "g": (190, 140, 30), "y": (250, 210, 70), "r": (220, 50, 50),
                     "k": (130, 80, 10)})
    return {"mikajager_hoed": hat, "mikajager_vest": vest, "mikamepper_medaille": medal}


# --- the Mika-mephal ----------------------------------------------------------------------------------------------------------
W, HH, D = 100, 36, 92
X0, X1, Z0, Z1 = 14, 86, 6, 66          # the hall's walls
ROOF = 13
CX, CZ = 50, 30                          # the middle of the stage (and of the board)
BOARD = [(x, z) for x in (47, 49, 51, 53) for z in (27, 29, 31, 33)]
NPC = (43.5, 1.0, 44.5)
PASSABLE = ("minecraft:air", "minecraft:cave_air", "guhs:vlaggetjes", "guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem",
            "guhs:guhoortjes", "minecraft:pink_carpet", "minecraft:white_carpet", "minecraft:light_weighted_pressure_plate",
            "guhs:guh_waterlelie")
LIGHTS = {"guhs:lampion_roze": 15, "guhs:lampion_geel": 15, "guhs:lampion_mint": 15, "minecraft:pearlescent_froglight": 15,
          "minecraft:shroomlight": 15, "minecraft:sea_lantern": 15, "minecraft:glowstone": 15, "minecraft:ochre_froglight": 15}
SOLID_EXTRA = ("minecraft:water",)      # (not walkable, but not a hole either)


def lamp(s, x, y, z, colour="roze", hanging=False):
    s.set(x, y, z, f"guhs:lampion_{colour}", {"hanging": str(hanging).lower(), "waterlogged": "false"})


def mika_pixel_face(s, h, plane, x0, y0, z0, facing_sign=1):
    """A big cute-evil Mika face on a wall (11 wide, 10 high). plane 'x': the face lies in a YZ plane at x0."""
    rows = ["..PP.....PP..",
            ".PPPP...PPPP.",
            "PPPPPPPPPPPPP",
            "PPDDPPPPPDDPP",
            "PPPPDDPDDPPPP",
            "PPRRKRPRKRRPP",
            "PPRRKRPRKRRPP",
            "PPPPPPPPPPPPP",
            "PPPDPPPPPDPPP",
            "PPPPDDDDDWPPP",
            ".PPPPPPPPPPP.",
            "..PPPPPPPPP.."]
    pal = {"P": "pink_concrete_powder", "D": "black_concrete", "R": "red_concrete", "K": "black_concrete", "W": "white_concrete"}
    for r, row in enumerate(rows):
        for c, ch in enumerate(row):
            if ch == ".":
                continue
            y = y0 + len(rows) - 1 - r
            off = (c - len(row) // 2) * facing_sign
            if plane == "x":
                s.set(x0, y, z0 + off, h.mc(pal[ch]))
            else:
                s.set(x0 + off, y, z0, h.mc(pal[ch]))


def text_display(h, x, y, z, text, scale, tags=(), yaw=0.0, width=240, background=0xC0301A26 - (1 << 32)):
    ms = h.ms
    nbt = {"id": "minecraft:text_display", "text": json.dumps(text), "billboard": "fixed", "line_width": width,
           "background": background, "shadow": h.Byte(1), "Rotation": h.floats(yaw, 0.0),
           "transformation": {"left_rotation": h.floats(0, 0, 0, 1), "right_rotation": h.floats(0, 0, 0, 1),
                              "translation": h.floats(0, 0, 0), "scale": h.floats(scale, scale, scale)}}
    if tags:
        nbt["Tags"] = ms.NbtList(8, list(tags))
    return (x, y, z, nbt)


def hall(h):
    mc, Byte, floats = h.mc, h.Byte, h.floats
    s = h.Structure((W, HH, D))
    rng = random.Random(2402)
    fp = [(x, z) for x in range(W) for z in range(D)]
    inside = lambda x, z: X0 < x < X1 and Z0 < z < Z1

    # --- ground: garden around, paved plaza in front (south), the hall floor: pink and white tiles ---
    for x, z in fp:
        if X0 <= x <= X1 and Z0 <= z <= Z1:
            s.set(x, 0, z, mc("smooth_quartz") if ((x // 2) + (z // 2)) % 2 else mc("pink_concrete"))
        elif z > Z1 and 36 <= x <= 64:
            s.set(x, 0, z, mc("smooth_quartz") if (x + z) % 5 else mc("pink_concrete"))
        else:
            s.set(x, 0, z, mc("grass_block"), {"snowy": "false"})
    for x, z in fp:                                   # hedges around the edge, flowers; the path in stays open
        ring = min(x, z, W - 1 - x, D - 1 - z)
        if X0 <= x <= X1 and Z0 <= z <= Z1 or (z > Z1 and 36 <= x <= 64):
            continue
        if ring == 0 and (x + z) % 4:
            s.set(x, 1, z, mc("flowering_azalea_leaves"), {"persistent": "true", "distance": "7", "waterlogged": "false"})
        elif ring in (2, 3) and (x * 7 + z * 3) % 6 == 0:
            s.set(x, 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
    for z in range(Z1 + 1, D):                        # the plaza's pink border
        for x in (36, 64):
            s.set(x, 0, z, mc("pink_terracotta"))

    # --- walls: pink with white pilasters, windows, a gold band ---
    for y in range(1, ROOF):
        for i in range(X0, X1 + 1):
            for (x, z) in ((i, Z0), (i, Z1)):
                s.set(x, y, z, wall_block(h, i - X0, y))
        for i in range(Z0, Z1 + 1):
            for (x, z) in ((X0, i), (X1, i)):
                s.set(x, y, z, wall_block(h, i - Z0, y))
    for i in range(Z0 + 3, Z1 - 2):                   # windows in the long walls
        if (i - Z0) % 6 in (2, 3, 4) and not 22 <= i <= 38:
            for y in range(4, 9):
                for x in (X0, X1):
                    s.set(x, y, i, mc("pink_stained_glass_pane"),
                          {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})

    # --- the roof: circus stripes, and a striped tent over the stage (hollow, with lights) ---
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if 36 <= x <= 64 and 16 <= z <= 44:
                continue
            a = math.atan2(z - CZ, x - CX)
            s.set(x, ROOF, z, mc("pink_wool") if int((a + math.pi) / (math.pi / 12)) % 2 else mc("white_wool"))
    for k in range(16):
        half, y = 15 - k, ROOF + k
        for x in range(CX - half - 1, CX + half + 2):
            for z in range(CZ - half - 1, CZ + half + 2):
                if max(abs(x - CX), abs(z - CZ)) not in ((half,) if k in (0, 15) else (half, half + 1)):
                    continue
                a = math.atan2(z - CZ, x - CX)
                block = mc("pink_wool") if int((a + math.pi) / (math.pi / 8)) % 2 else mc("white_wool")
                if k in (1, 5, 9) and (x + z) % 4 == 0:
                    block = mc("pearlescent_froglight")
                if k == 15:
                    block = mc("gold_block")
                s.set(x, y, z, block)
    s.fill(CX, ROOF + 16, CZ, CX, ROOF + 19, CZ, mc("birch_fence"))
    for (dx, dy) in ((1, 0), (2, 0), (3, 0), (1, 1), (2, 1)):
        s.set(CX + dx, ROOF + 19 - dy, CZ, mc("pink_wool"))
    s.fill(CX, ROOF + 10, CZ, CX, ROOF + 14, CZ, mc("chain"), {"axis": "y", "waterlogged": "false"})
    lamp(s, CX, ROOF + 9, CZ, "geel", hanging=True)
    # lampgions under the flat roof, and bunting across the hall
    for x in range(18, X1, 7):
        for z in range(10, Z1, 7):
            if not (35 <= x <= 65 and 15 <= z <= 45):
                s.fill(x, ROOF - 3, z, x, ROOF - 1, z, mc("chain"), {"axis": "y", "waterlogged": "false"})
                lamp(s, x, ROOF - 4, z, ("roze", "geel", "mint")[(x + z) % 3], hanging=True)
    # and kermis lights in the floor
    for x in range(X0 + 3, X1 - 1, 6):
        for z in range(Z0 + 3, Z1 - 1, 6):
            s.set(x, 0, z, mc("pearlescent_froglight") if (x + z) % 12 else mc("ochre_froglight"))
    for z in (11, 49, 58):
        for x in range(X0 + 1, X1):
            if not (35 <= x <= 65 and 15 <= z <= 45) and s.get(x, ROOF - 1, z) is None:
                s.set(x, ROOF - 1, z, "guhs:vlaggetjes", {"axis": "x"})

    stage(h, s)
    scoreboard(h, s)
    bleachers(h, s)
    stalls(h, s, rng)
    facade(h, s)
    plaza(h, s, rng)

    # the guh face on the floor inside the entrance
    floor_face(h, s, CX, 57, 5)
    # Mika trophies on little pillars along the aisle
    for (x, z) in ((24, 46), (34, 46), (66, 46), (76, 46), (24, 14), (76, 14)):
        s.set(x, 1, z, mc("quartz_pillar"), {"axis": "y"})
        s.set(x, 2, z, "guhs:mika_mep_trofee", {"facing": "south"})
    # two big Mika faces on the side walls, looking at the stage
    mika_pixel_face(s, h, "x", X0 + 1, 1, CZ, facing_sign=-1)
    mika_pixel_face(s, h, "x", X1 - 1, 1, CZ, facing_sign=1)

    # the Mepguh at his booth next to the stage stairs
    s.entity(NPC[0], NPC[1], NPC[2], {"id": "guhs:guh_npc", "Kind": "mepguh", "PersistenceRequired": Byte(1), "Rotation": floats(0.0, 0.0)})

    # inside: all air (no terrain in the hall), outside: cleared above the ground
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            for y in range(1, ROOF):
                if s.get(x, y, z) is None:
                    s.set(x, y, z, mc("air"))
    for x in range(CX - 15, CX + 16):
        for z in range(CZ - 15, CZ + 16):
            for y in range(ROOF, ROOF + 16):
                if max(abs(x - CX), abs(z - CZ)) < 15 - (y - ROOF) and s.get(x, y, z) is None:
                    s.set(x, y, z, mc("air"))
    s.clear_above(fp, 1)
    return s


def wall_block(h, i, y):
    mc = h.mc
    if y == ROOF - 1:
        return mc("gold_block") if i % 6 == 0 else mc("yellow_concrete")
    if i % 6 == 0:
        return mc("white_concrete")
    return mc("pink_concrete") if y > 1 else mc("magenta_concrete")


def stage(h, s):
    """The stage: a guh head (ears, eyes, cheeks, nose) with the whack-a-Mika board for a mouth."""
    mc = h.mc
    R = 10.4
    for x in range(CX - 13, CX + 14):
        for z in range(CZ - 14, CZ + 12):
            d = math.hypot(x - CX, z - CZ)
            ear = min(math.hypot(x - (CX - 8), z - (CZ - 9)), math.hypot(x - (CX + 8), z - (CZ - 9)))
            if d <= R:
                if d > R - 1:
                    s.set(x, 1, z, mc("pearlescent_froglight") if (x + z) % 3 == 0 else mc("gold_block"))
                else:
                    s.set(x, 1, z, mc("pink_wool"))
            elif ear <= 3.2:
                s.set(x, 1, z, mc("magenta_terracotta") if ear <= 1.6 else mc("pink_wool"))
    for ex in (CX - 4, CX + 3):                                  # eyes (with a shine) and a nose
        for dx in (0, 1):
            for z in (CZ - 8, CZ - 7):
                s.set(ex + dx, 1, z, mc("black_concrete"))
        s.set(ex, 1, CZ - 8, mc("white_concrete"))
    for x in (CX - 1, CX, CX + 1):
        s.set(x, 1, CZ - 6, mc("pink_terracotta"))
    for cx in (CX - 8, CX + 7):                                  # blush
        for dx in (0, 1):
            for z in (CZ + 1, CZ + 2):
                s.set(cx + dx, 1, z, mc("magenta_concrete"))
    # the board: a gold rim, 16 holes with quartz paths between them
    for x in range(CX - 4, CX + 5):
        for z in range(CZ - 4, CZ + 5):
            if max(abs(x - CX), abs(z - CZ)) == 4:
                s.set(x, 1, z, mc("gold_block"))
            elif (x, z) in BOARD:
                s.set(x, 1, z, "guhs:mika_mep_gat")
            else:
                s.set(x, 1, z, mc("smooth_quartz") if (x + z) % 2 else mc("white_concrete"))
    # stairs up (south), and four lamp posts with trophies on the rim
    for x in range(CX - 2, CX + 3):
        s.set(x, 1, CZ + 11, mc("quartz_stairs"), {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for (x, z) in ((CX - 7, CZ - 7), (CX + 7, CZ - 7), (CX - 7, CZ + 7), (CX + 7, CZ + 7)):
        s.fill(x, 2, z, x, 3, z, mc("birch_fence"))
        lamp(s, x, 4, z, "roze")
    # the Mepguh's booth: striped awning on posts
    bx0, bx1, bz0, bz1 = 40, 46, 42, 46
    for (x, z) in ((bx0, bz0), (bx1, bz0), (bx0, bz1), (bx1, bz1)):
        s.fill(x, 1, z, x, 4, z, mc("birch_fence"))
    for x in range(bx0, bx1 + 1):
        for z in range(bz0, bz1 + 1):
            s.set(x, 5, z, mc("red_wool") if x % 2 else mc("yellow_wool"))
    lamp(s, 43, 4, 44, "geel", hanging=True)
    s.set(41, 1, 43, "guhs:mika_mep_trofee", {"facing": "south"})
    s.set(45, 1, 43, "guhs:guh_taart")


def floor_face(h, s, cx, cz, r):
    mc = h.mc
    for x in range(cx - r - 3, cx + r + 4):
        for z in range(cz - r - 3, cz + r + 2):
            d = math.hypot(x - cx, z - cz)
            ear = min(math.hypot(x - (cx - r + 1), z - (cz - r)), math.hypot(x - (cx + r - 1), z - (cz - r)))
            if d <= r + 0.4:
                s.set(x, 0, z, mc("pink_terracotta") if d > r - 0.6 else mc("pink_wool"))
            elif ear <= 2.2:
                s.set(x, 0, z, mc("pink_terracotta"))
    for ex in (cx - 2, cx + 2):
        s.set(ex, 0, cz - 1, mc("black_concrete"))
    for (x, z) in ((cx - 1, cz + 2), (cx, cz + 3), (cx + 1, cz + 2)):
        s.set(x, 0, z, mc("magenta_concrete"))
    s.set(cx - 3, 0, cz + 1, mc("magenta_concrete"))
    s.set(cx + 3, 0, cz + 1, mc("magenta_concrete"))


def top_marker(h, x, y, z):
    """An invisible text display (no text, no background) marking where the world's top 3 floats (quest.Scorebord)."""
    return text_display(h, x, y, z, {"text": ""}, 1.0, ("guhs_mep_top",), background=0)


def scoreboard(h, s):
    """The scoreboard wall (north): a black board in a gold frame with the world's top 3, the live score and the rules."""
    mc = h.mc
    z = Z0 + 1
    for x in range(30, 71):
        for y in range(1, 12):
            edge = x in (30, 70) or y in (1, 11)
            s.set(x, y, z, mc("gold_block") if edge else mc("black_concrete"))
    for x in range(30, 71, 4):
        lamp(s, x, 12, z + 1, "roze", hanging=True)
    ent = [top_marker(h, 38.5, 4.0, z + 1.4),
           text_display(h, 50.5, 5.0, z + 1.05, {"translate": "gui.guhs.mika_mep.live.title"}, 4.0, ("guhs_mep_live",), width=150),
           text_display(h, 62.5, 3.5, z + 1.05, {"translate": "gui.guhs.mika_mep.rules"}, 2.0, width=150)]
    for e in ent:
        s.entity(*e)


def bleachers(h, s):
    mc = h.mc
    for side in (-1, 1):
        front = CX - 17 if side < 0 else CX + 17
        facing = "east" if side < 0 else "west"
        for k in range(3):
            xb = front + side * 2 * k                   # the bench row
            xw = xb + side                              # the walkway behind it
            for z in range(CZ - 9, CZ + 10):
                for x in (xb, xw):
                    for y in range(1, k + 1):
                        s.set(x, y, z, mc("cherry_planks") if y == k else mc("pink_concrete"))
                s.set(xb, k + 1, z, "guhs:guh_bank", {"facing": facing})
        xr = front + side * 6                           # a railing at the back
        for z in range(CZ - 9, CZ + 10):
            for y in range(1, 3):
                s.set(xr, y, z, mc("pink_concrete"))
            s.set(xr, 3, z, mc("cherry_fence"), {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
        for z in (CZ - 10, CZ + 10):                    # steps up at the ends
            for k in range(1, 3):
                x = front + side * 2 * k
                for dx in (0, side):
                    for y in range(1, k):
                        s.set(x + dx, y, z, mc("pink_concrete"))
                    s.set(x + dx, k, z, mc("cherry_slab"), {"type": "bottom", "waterlogged": "false"})


def stalls(h, s, rng):
    """Two fair stalls along the side walls: a kaasknabbel stall and a balloon stall."""
    mc = h.mc
    for (x0, colour, goods) in ((18, "yellow_wool", "guhs:block_of_kaasknabbels"), (76, "light_blue_wool", "minecraft:pink_wool")):
        z0, z1 = 54, 60
        for x in range(x0, x0 + 7):
            s.set(x, 1, z0 + 3, mc("stripped_birch_log"), {"axis": "x"})
        for (x, z) in ((x0, z0), (x0 + 6, z0), (x0, z1), (x0 + 6, z1)):
            s.fill(x, 1, z, x, 4, z, mc("birch_fence"))
        for x in range(x0, x0 + 7):
            for z in range(z0, z1 + 1):
                s.set(x, 5, z, mc(colour) if (x + z) % 2 else mc("white_wool"))
        s.set(x0 + 2, 2, z0 + 3, "guhs:guh_taart")
        s.set(x0 + 4, 2, z0 + 3, "guhs:mika_mep_trofee", {"facing": "south"})
        for x in range(x0 + 1, x0 + 6, 2):
            s.set(x, 1, z0 + 1, goods)
        lamp(s, x0 + 3, 4, z0 + 3, "mint", hanging=True)


def facade(h, s):
    """The south wall is a giant guh face; its open mouth is the entrance."""
    mc = h.mc
    fy = 10
    for x in range(CX - 18, CX + 19):
        for y in range(1, HH):
            d = math.hypot(x - CX, y - fy)
            ear = min(math.hypot(x - (CX - 10), y - (fy + 10)), math.hypot(x - (CX + 10), y - (fy + 10)))
            block = None
            if d <= 12.4:
                block = mc("pink_terracotta") if d > 11.4 else mc("pink_wool")
            elif ear <= 4.3:
                block = mc("magenta_terracotta") if ear <= 2.2 else mc("pink_wool")
            if block:
                for z in (Z1, Z1 + 1):
                    s.set(x, y, z, block)
    for ex in (CX - 5, CX + 5):                                   # eyes with a shine
        for dx in (-1, 0, 1):
            for dy in (0, 1, 2, 3):
                if abs(dx) == 1 and dy in (0, 3):
                    continue
                s.set(ex + dx, fy + 2 + dy, Z1 + 1, mc("black_concrete"))
        s.set(ex - 1, fy + 4, Z1 + 1, mc("white_concrete"))
    for (cx, cy) in ((CX - 9, fy - 1), (CX + 8, fy - 1)):         # cheeks
        for dx in (0, 1):
            for dy in (0, 1):
                s.set(cx + dx, cy + dy, Z1 + 1, mc("magenta_concrete"))
    for x in (CX - 1, CX, CX + 1):                                # nose
        s.set(x, fy, Z1 + 1, mc("pink_terracotta"))
    s.set(CX, fy - 1, Z1 + 1, mc("pink_terracotta"))
    for i in range(5):                                            # whiskers
        for side in (-1, 1):
            s.set(CX + side * (12 + i), fy + 1 + (i // 2), Z1 + 1, mc("white_concrete"))
            s.set(CX + side * (12 + i), fy - 1 - (i // 2), Z1 + 1, mc("white_concrete"))
    # the mouth: the door, with a dark smile around it
    for x in range(CX - 4, CX + 5):
        for y in range(1, 6):
            if (abs(x - CX) == 4 and y == 5):
                continue
            for z in (Z1, Z1 + 1):
                s.set(x, y, z, mc("air"))
    for (x, y) in [(CX - 5, y) for y in range(1, 5)] + [(CX + 5, y) for y in range(1, 5)] + [(CX - 4, 5), (CX + 4, 5)] \
            + [(x, 6) for x in range(CX - 3, CX + 4)]:
        s.set(x, y, Z1 + 1, mc("black_concrete"))
    s.set(CX + 2, 5, Z1 + 1, mc("white_concrete"))                # a little tooth
    s.entity(*text_display(h, CX + 0.5, fy + 5.5, Z1 + 2.05, {"translate": "gui.guhs.mika_mep.sign"}, 3.0, width=200))


def plaza(h, s, rng):
    """In front: a guh-face fountain, two (cute) Mika statues and lamp posts."""
    mc = h.mc
    fx, fz, r = CX, 80, 5
    for x in range(fx - r - 3, fx + r + 4):
        for z in range(fz - r - 4, fz + r + 2):
            d = math.hypot(x - fx, z - fz)
            ear = min(math.hypot(x - (fx - 4), z - (fz - 5)), math.hypot(x - (fx + 4), z - (fz - 5)))
            if d <= r + 0.4 or ear <= 2.2:
                edge = d > r - 0.6 and ear > 1.3 or (d > r + 0.4 and ear > 1.2)
                s.set(x, 0, z, mc("smooth_quartz"))
                if edge:
                    s.set(x, 1, z, mc("pink_terracotta"))
                else:
                    s.set(x, 1, z, mc("water"), {"level": "0"})
    for ex in (fx - 2, fx + 2):                                   # eyes and a nose sticking out of the water
        s.set(ex, 1, fz - 1, mc("black_concrete"))
        s.set(ex, 2, fz - 1, mc("air"))
    s.set(fx, 1, fz + 1, mc("pink_terracotta"))
    s.set(fx, 2, fz + 1, mc("air"))
    for (x, z) in ((fx - 1, fz + 3), (fx + 1, fz + 3)):
        s.set(x, 1, z, mc("white_concrete"))
    s.set(fx - 3, 2, fz + 1, "guhs:guh_waterlelie")
    s.set(fx + 3, 2, fz + 2, "guhs:guh_waterlelie")
    # the Mika statues: big cute heads with a bandage (they've been whacked a lot)
    for sx in (38, 62):
        sz = 76
        s.fill(sx - 1, 1, sz - 1, sx + 1, 1, sz + 1, mc("quartz_block"))
        for x in range(sx - 2, sx + 3):
            for z in range(sz - 2, sz + 3):
                for y in range(2, 6):
                    s.set(x, y, z, mc("pink_concrete_powder"))
        for x in (sx - 2, sx + 2):
            s.set(x, 6, sz, mc("pink_concrete_powder"))
            s.set(x, 7, sz, mc("magenta_terracotta"))
        for x in (sx - 1, sx + 1):
            s.set(x, 4, sz + 2, mc("red_concrete"))
            s.set(x, 5, sz + 2, mc("black_concrete"))
        for x in range(sx - 1, sx + 2):
            s.set(x, 2, sz + 2, mc("black_concrete"))
        s.set(sx + 1, 2, sz + 2, mc("white_concrete"))
        for x in range(sx - 2, sx + 3):
            s.set(x, 6, sz, mc("white_wool"))
        for z in range(sz - 2, sz + 3):
            s.set(sx, 6, z, mc("white_wool"))
        s.set(sx, 7, sz, "guhs:mika_mep_trofee", {"facing": "south"})
    for (x, z) in ((44, 70), (56, 70), (44, 89), (56, 89), (40, 84), (60, 84), (40, 70), (60, 70)):
        s.fill(x, 1, z, x, 3, z, mc("birch_fence"))
        lamp(s, x, 4, z, "geel")
    for (x, z) in ((42, 86), (58, 86)):
        s.set(x, 1, z, "guhs:guh_bank", {"facing": "north"})


# --- the self-check -----------------------------------------------------------------------------------------------------------
def solid(name):
    return name is not None and name not in PASSABLE and name != "minecraft:water" and not name.startswith("guhs:lampion") \
        and name != "minecraft:chain"


def standable(s, x, y, z):
    """Can a player stand with its feet at (x, y, z)?"""
    below = s.get(x, y - 1, z)
    return (below is not None and solid(below) and below not in ("minecraft:birch_fence", "minecraft:cherry_fence")
            and all(s.get(x, y + d, z) in (None,) + PASSABLE for d in (0, 1)))


def check(s, npc, stand, entrance_z):
    problems = []
    # 1. the floor: no holes anywhere at ground level
    for x in range(W):
        for z in range(D):
            b = s.get(x, 0, z)
            if b is None or b in PASSABLE:
                problems.append(f"hole in the floor at {x},0,{z}")
    # 2. the entrance is reachable from the edge of the plaza, and the board from the entrance (walking, 1 step up)
    start = (CX, 1, D - 2)
    seen, todo = {start}, [start]
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1, -2):
                n = (x + dx, y + dy, z + dz)
                if n in seen or not (0 <= n[0] < W and 1 <= n[1] < HH - 2 and 0 <= n[2] < D):
                    continue
                if dy == 1 and s.get(x, y + 2, z) not in (None,) + PASSABLE:
                    continue
                if standable(s, *n):
                    seen.add(n)
                    todo.append(n)
                    break
    if stand not in seen:
        problems.append(f"the board's middle {stand} can't be reached from the plaza")
    if not any(p[2] < entrance_z for p in seen):
        problems.append("can't get into the hall")
    # 3. the Mepguh sits on solid ground, with room, and can be reached
    nx, ny, nz = int(npc[0]), int(npc[1]), int(npc[2])
    if not standable(s, nx, ny, nz):
        problems.append(f"the Mepguh at {npc} has no solid ground or no room")
    if not any(abs(p[0] - nx) + abs(p[2] - nz) <= 2 and p != (nx, ny, nz) for p in seen):
        problems.append("nobody can walk up to the Mepguh")
    # 4. no floating blocks: everything hangs together with the ground
    blocks = {p for p, b in s.blocks.items() if b[0] not in ("minecraft:air", "minecraft:cave_air")}
    ground = [p for p in blocks if p[1] == 0]
    conn, todo = set(ground), list(ground)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in blocks and n not in conn:
                conn.add(n)
                todo.append(n)
    floating = blocks - conn
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {sorted(floating)[:5]}")
    # 5. light: every spot you can stand on inside the hall gets block light (nothing spawns)
    light = {}
    todo = []
    for p, b in s.blocks.items():
        if b[0] in LIGHTS:
            light[p] = LIGHTS[b[0]]
            todo.append(p)
    while todo:
        nxt = []
        for p in todo:
            lv = light[p] - 1
            if lv <= 0:
                continue
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (p[0] + d[0], p[1] + d[1], p[2] + d[2])
                if not s.inside(*n) or light.get(n, 0) >= lv:
                    continue
                b = s.get(*n)
                if b is None or not solid(b) or b in ("guhs:guh_bank", "guhs:mika_mep_trofee", "guhs:guh_taart") \
                        or "fence" in b or "pane" in b or "stairs" in b or "slab" in b or b in LIGHTS:
                    light[n] = lv
                    nxt.append(n)
        todo = nxt
    dark = [p for p in seen if X0 < p[0] < X1 and Z0 < p[2] < Z1 and light.get(p, 0) < 1]
    if dark:
        problems.append(f"{len(dark)} dark spots in the hall, e.g. {dark[:5]}")
    return problems, len(seen)


def proefhal(h):
    """A small room for the GameTests: 4 holes, the Mepguh, the live board and the spot for the top 3."""
    mc, Byte, floats = h.mc, h.Byte, h.floats
    s = h.Structure((9, 5, 11))
    for x in range(9):
        for z in range(11):
            s.set(x, 0, z, mc("white_concrete"))
    for (x, z) in ((3, 3), (5, 3), (3, 5), (5, 5)):
        s.set(x, 0, z, "guhs:mika_mep_gat")
    s.entity(4.5, 1.0, 8.5, {"id": "guhs:guh_npc", "Kind": "mepguh", "PersistenceRequired": Byte(1), "Rotation": floats(0.0, 0.0)})
    s.entity(*top_marker(h, 2.5, 2.0, 0.5))
    s.entity(*text_display(h, 6.5, 2.0, 0.5, {"text": ""}, 0.5, ("guhs_mep_live",)))
    s.save("mika_mep_proefhal")


# --- texts ----------------------------------------------------------------------------------------------------------------------
TEXTS = {  # key: (English, Dutch)
    "entity.guhs.guh_npc.mepguh": ("Whack Guh", "Mepguh"),
    "block.guhs.mika_mep_gat": ("Whack-a-Mika Hole", "Mika-mepgat"),
    "block.guhs.mika_mep_kop": ("Whack-a-Mika Head", "Mika-mepkop"),
    "block.guhs.mika_mep_trofee": ("Mika Trophy", "Mika-trofee"),
    "item.guhs.mepmunt": ("Whack Coin", "Mepmunt"),
    "item.guhs.mika_mep_hamer": ("Mika Whacker (loaned)", "Mika-mephamer (geleend)"),
    "item.guhs.mika_mep_hamer.lore": ("Borrowed from the Whack Guh: it goes back after the game. Left-click the Mikas!",
                                      "Geleend van de Mepguh: na het potje gaat hij terug. Linksklik op de Mika's!"),
    "item.guhs.mikajager_hoed": ("Mika Hunter's Hat", "Mika-jagershoed"),
    "item.guhs.mikajager_vest": ("Whack Vest with Pockets", "Mepvest met zakjes"),
    "item.guhs.mikamepper_medaille": ("Mika Whacker Medal", "Mika-mepper-medaille"),
    f"structure.guhs.{HALL}": ("Mika Whack Hall", "Mika-mephal"),
    f"structure.guhs.{HALL}.tooltip": ("Minigame: whack-a-Mika with the Whack Guh, whack coins and the Mika hunter outfit",
                                       "Minigame: Mika meppen bij de Mepguh, mepmunten en het Mika-jagerpakje"),
    # the Mepguh talks
    "quest.guhs.mika_mep.hello_first": ("NJEG! A new whacker! The Mikas keep popping out of my board... Grab my Mika whacker and whack them back in! Want to try? VAHOEG!",
                                        "NJEG! Een nieuwe mepper! De Mika's ploppen steeds uit mijn bord... Pak mijn Mika-mephamer en mep ze terug hun hol in! Zin in een potje? VAHOEG!"),
    "quest.guhs.mika_mep.hello": ("There you are again! The Mikas are getting cheeky. Up for a round?",
                                  "Daar ben je weer! De Mika's worden brutaal. Nog een potje meppen?"),
    "quest.guhs.mika_mep.running": ("Shhh, %s is whacking! %s seconds to go. Sit on the stands and watch, you're next!",
                                    "Sssst, %s is aan het meppen! Nog %s seconden. Ga lekker vadsig op de tribune zitten, daarna ben jij!"),
    "quest.guhs.mika_mep.running_you": ("Why are you talking to me? MEP! The Mikas are escaping!",
                                        "Waarom praat je met mij? MEPPEN! De Mika's ontsnappen!"),
    "quest.guhs.mika_mep.broken": ("Njeg... my board is broken, I can't find the holes any more.",
                                   "Njeg... mijn bord is kapot, ik kan de gaten niet meer vinden."),
    "quest.guhs.mika_mep.go": ("Here's your Mika whacker! Stand on the board and left-click the Mikas. Golden Mika = jackpot. And whatever you do: DON'T WHACK THE GUH!",
                               "Hier is je Mika-mephamer! Sta op het bord en linksklik op de Mika's. Gouden Mika = jackpot. En wat je ook doet: NIET DE GUH MEPPEN!"),
    "quest.guhs.mika_mep.help1": ("Mika meppen: 60 seconds, and the Mikas pop up faster and faster. Left-click them with the whacker!",
                                  "Mika meppen: 60 seconden, en de Mika's ploppen steeds sneller op. Linksklik ze met de mephamer!"),
    "quest.guhs.mika_mep.help2": ("A Mika is 10 points, a golden Mika 50. Every 5 in a row your combo goes up (up to x5)!",
                                  "Een Mika is 10 punten, een gouden Mika 50. Elke 5 op rij gaat je combo omhoog (tot x5)!"),
    "quest.guhs.mika_mep.help3": ("Sometimes a guh pops up. DON'T whack it: minus 25 and your combo is gone. Let a guh go and you get 5 points. A Mika that escapes or an empty hole: combo gone.",
                                  "Soms plopt er een guh op. NIET meppen: min 25 en je combo is weg. Laat je hem met rust, dan krijg je 5 punten. Een Mika die ontsnapt of een mep in een leeg gat: combo weg."),
    "quest.guhs.mika_mep.help4": ("You get whack coins for your score, and with those you buy the Mika hunter outfit for your guh. The best three whackers of the world float on the scoreboard wall. You never need your own stuff: you can't get hurt or hungry while whacking. Njeg!",
                                  "Voor je score krijg je mepmunten, en daarmee koop je het Mika-jagerpakje voor je guh. De drie beste meppers van de wereld zweven bij de scorebordmuur. Je hebt nooit eigen spullen nodig: tijdens het meppen word je niet moe of gewond. Njeg!"),
    "quest.guhs.mika_mep.end": ("TIME! Your score: %s points (%s Mikas, %s golden, %s guhs whacked by accident, best combo %s).",
                                "TIJD! Je score: %s punten (%s Mika's, %s gouden, %s guhs per ongeluk gemept, beste combo %s)."),
    "quest.guhs.mika_mep.coins": ("You get %s whack coin(s)! The whacker goes back to the Whack Guh.",
                                  "Je krijgt %s mepmunt(en)! De mephamer gaat terug naar de Mepguh."),
    "quest.guhs.mika_mep.first": ("Your very first game! As a thank-you: %s extra whack coins and a bag of kaasknabbels. Vads!",
                                  "Je allereerste potje! Als bedankje: %s extra mepmunten en een zakje kaasknabbels. Vads!"),
    "quest.guhs.mika_mep.record": ("NEW RECORD: %s points! VAHOEG!", "NIEUW RECORD: %s punten! VAHOEG!"),
    "quest.guhs.mika_mep.best": ("Your record is still %s points. Keep whacking, vads!", "Jouw record blijft %s punten. Doormeppen, vads!"),
    "quest.guhs.mika_mep.stop.left": ("You walked away from the board... game over (no coins). I'll take the whacker back!",
                                      "Je liep weg van het mepbord... het potje is voorbij (geen mepmunten). De mephamer neem ik weer mee!"),
    "quest.guhs.mika_mep.stop.stopped": ("Stopped. I'll take the whacker back. See you soon!", "Gestopt. De mephamer neem ik weer mee. Tot zo!"),
    "quest.guhs.mika_mep.stop.gone": ("The game of Mika meppen stopped: the whacker went back to the Whack Guh.",
                                      "Het potje Mika meppen is gestopt: de mephamer is terug bij de Mepguh."),
    # the game
    "gui.guhs.mika_mep.ready": ("Get your whacker ready...", "Mephamer klaar..."),
    "gui.guhs.mika_mep.go": ("MEP!", "MEP!"),
    "gui.guhs.mika_mep.go.sub": ("Whack the Mikas, not the guh!", "Mep de Mika's, niet de guh!"),
    "gui.guhs.mika_mep.ten_left": ("10 more seconds! Faster!", "Nog 10 seconden! Sneller!"),
    "gui.guhs.mika_mep.time": ("TIME!", "TIJD!"),
    "gui.guhs.mika_mep.score": ("%s points", "%s punten"),
    "gui.guhs.mika_mep.hit": ("MEP! +%s  (%s in a row, x%s)", "MEP! +%s  (%s op rij, x%s)"),
    "gui.guhs.mika_mep.combo_up": ("COMBO! +%s  (%s in a row: now x%s!)", "COMBO! +%s  (%s op rij: nu x%s!)"),
    "gui.guhs.mika_mep.gold": ("GOLDEN MIKA!", "GOUDEN MIKA!"),
    "gui.guhs.mika_mep.njeg": ("NJEG!", "NJEG!"),
    "gui.guhs.mika_mep.not_the_guh": ("Don't whack the guh! -%s", "Niet de guh meppen! -%s"),
    "gui.guhs.mika_mep.spared": ("The guh is safe, vads! +%s", "De guh is veilig, vads! +%s"),
    "gui.guhs.mika_mep.escaped": ("A Mika escaped: combo gone!", "Een Mika ontsnapte: combo weg!"),
    "gui.guhs.mika_mep.miss": ("BONK! Empty hole: combo gone", "BONK! Leeg gat: combo weg"),
    "gui.guhs.mika_mep.take_hammer": ("Take your Mika whacker in your hand!", "Pak je Mika-mephamer in je hand!"),
    "gui.guhs.mika_mep.not_playing": ("Only whack with the Whack Guh's whacker: talk to the Whack Guh to play!",
                                      "Meppen doe je met de hamer van de Mepguh: praat met de Mepguh om te spelen!"),
    "gui.guhs.mika_mep.bar.ready": ("Mika meppen: get ready...", "Mika meppen: klaar voor de start..."),
    "gui.guhs.mika_mep.bar": ("Mika meppen: %s points  -  combo x%s  -  %ss", "Mika meppen: %s punten  -  combo x%s  -  %ss"),
    "gui.guhs.mika_mep.live.title": ("MIKA MEPPEN", "MIKA MEPPEN"),
    "gui.guhs.mika_mep.live.idle": ("Talk to the Whack Guh!", "Praat met de Mepguh!"),
    "gui.guhs.mika_mep.live.score": ("%s points", "%s punten"),
    "gui.guhs.mika_mep.live.combo": ("combo x%s  -  %ss", "combo x%s  -  %ss"),
    "gui.guhs.scorebord.meppen": ("Top 3 Mika whackers", "Top 3 Mika-meppers"),
    "gui.guhs.mika_mep.rules": ("Mika = 10\nGolden Mika = 50\n5 in a row = combo up\nGUH = -25 (don't!)",
                                "Mika = 10\nGouden Mika = 50\n5 op rij = combo omhoog\nGUH = -25 (niet doen!)"),
    "gui.guhs.mika_mep.sign": ("MIKA WHACK HALL", "MIKA-MEPHAL"),
    "gui.guhs.mika_mep.no_build": ("Njeg! No building or breaking in the Mika whack hall. Whacking is only for Mikas!",
                                   "Njeg! In de Mika-mephal mag je niks slopen of bouwen. Meppen doe je alleen bij de Mika's!"),
    # the screen
    "gui.guhs.mika_mep.question": ("The Mikas keep popping out of my board! Whack them back in: 60 seconds, faster and faster. Mepmunten for your score!",
                                   "De Mika's ploppen steeds uit mijn bord! Mep ze terug: 60 seconden, steeds sneller. Mepmunten voor je score!"),
    "gui.guhs.mika_mep.question.busy": ("%s is whacking right now (%s s to go). Watch from the stands, you're next!",
                                        "%s is nu aan het meppen (nog %s s). Kijk mee vanaf de tribune, daarna ben jij!"),
    "gui.guhs.mika_mep.question.you": ("You're playing! %s seconds to go. Do you really want to stop?",
                                       "Je bent aan het spelen! Nog %s seconden. Wil je echt stoppen?"),
    "gui.guhs.mika_mep.play": ("Whack! (60 s)", "Meppen! (60 s)"),
    "gui.guhs.mika_mep.play.tooltip": ("You get a loaned whacker and stand in the middle of the board. No own items needed.",
                                       "Je krijgt een geleende mephamer en staat midden op het bord. Je hebt niks zelf nodig."),
    "gui.guhs.mika_mep.stop": ("Stop playing", "Stoppen"),
    "gui.guhs.mika_mep.stop.tooltip": ("Stop now (no whack coins)", "Nu stoppen (geen mepmunten)"),
    "gui.guhs.mika_mep.help": ("How does it work?", "Hoe werkt het?"),
    "gui.guhs.mika_mep.help.tooltip": ("The Whack Guh explains everything (in the chat)", "De Mepguh legt alles uit (in de chat)"),
    "gui.guhs.mika_mep.shop": ("Shop", "Winkeltje"),
    "gui.guhs.mika_mep.shop.tooltip": ("The Mika hunter outfit for your guh (only sold here!), for whack coins",
                                       "Het Mika-jagerpakje voor je guh (alleen hier te koop!), voor mepmunten"),
    "gui.guhs.mika_mep.records": ("Your record: %s   -   World record: %s", "Jouw record: %s   -   Wereldrecord: %s"),
    "gui.guhs.mika_mep.coins": ("Whack coins in your pocket: %s", "Mepmunten op zak: %s"),
}

SHOWN_ADVANCEMENTS = [  # name, parent, icon, frame, criterion, title (en, nl), description (en, nl)
    ("find_mika_mep_hal", "enter_guhmension", "guhs:mika_mep_trofee", "goal",
     {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": [f"guhs:{HALL}"]}}}},
     ("Who's popping up there?", "Wie plopt daar op?"), ("Find the rare Mika whack hall", "Vind de zeldzame Mika-mephal")),
    ("mika_meppen", "find_mika_mep_hal", "guhs:mepmunt", "goal",
     {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:mepmunt"}]}},
     ("MEP!", "MEP!"), ("Play a game of Mika meppen and earn whack coins", "Speel een potje Mika meppen en verdien mepmunten")),
    ("mika_mep_pakje", "mika_meppen", "guhs:mikajager_hoed", "challenge",
     {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
         {"items": "guhs:mikajager_hoed"}, {"items": "guhs:mikajager_vest"}, {"items": "guhs:mikamepper_medaille"}]}},
     ("Mika Hunter", "Mika-jager"), ("Buy the whole Mika hunter outfit from the Whack Guh", "Koop het hele Mika-jagerpakje bij de Mepguh")),
]
QUEST_ADVANCEMENTS = ["mika_meppen_gespeeld", "mika_meppen_500", "mika_meppen_1500", "mika_meppen_2500", "mika_meppen_goud", "mika_meppen_combo"]


def build(h):
    blocks_and_items(h)
    # the Mepguh: a coral-orange fairground guh
    src = Image.open(f"{h.TEX}/entity/guh_sitting.png")
    h.save(h.recolour(src, hue=0.05, sat=1.3, val=1.02, only=h.pinkish), "entity", "npc_mepguh.png")
    # the hall: rare (like the kermis), big (100 x 92), on flat ground; nothing spawns in it
    h.TEMPLATE_SIZES[HALL] = 100
    h.FLATNESS[HALL] = 34
    none = {"bounding_box": "full", "spawns": []}
    h.structure(HALL, h.GUHMENSION_LAND, spacing=44, separation=16, salt=SALT,          # (as rare as the kermis)
                spawn_overrides={"creature": none, "monster": none, "ambient": none})
    s = hall(h)
    problems, walkable = check(s, NPC, (CX, 2, CZ), Z1)
    if problems:
        raise SystemExit("Mika-mephal self-check failed:\n  " + "\n  ".join(problems))
    print(f"Mika-mephal: self-check ok ({walkable} walkable spots)")
    s.save(HALL)
    proefhal(h)
    # advancements
    for name, parent, icon, frame, crit, (ten, tnl), (den, dnl) in SHOWN_ADVANCEMENTS:
        h.w(f"{h.D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        h.lang(f"advancements.guhs.guhmension.{name}.title", ten, tnl)
        h.lang(f"advancements.guhs.guhmension.{name}.description", den, dnl)
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for key, (en, nl) in TEXTS.items():
        h.lang(key, en, nl)


def ftb(fq):
    q, item, adv, structure = fq.q, fq.item, fq.adv, fq.structure
    y = 30
    q("meppen_hal", "Wie plopt daar op?", "Zoek de zeldzame &dMika-mephal&r (superkompas: Minigames > Mika-mephal). Achter de gevel met het reuzenguhgezicht woont de Mepguh.",
      "guhs:guhmensie_superkompas", [structure(HALL)], x=-8, y=y)
    q("meppen_eerste", "MEP!", "Praat met de Mepguh en speel een potje &dMika meppen&r: 60 seconden Mika's terugmeppen met de geleende mephamer. Niet de guh meppen!",
      "guhs:mepmunt", [adv("mika_meppen_gespeeld")], rewards=(("guhs:kaas_knabbels", 16),), x=-6, y=y, xp=100)
    q("meppen_goud", "Gouden mep", "Mep een &6gouden Mika&r: 50 punten keer je combo!", "guhs:mika_mep_trofee",
      [adv("mika_meppen_goud")], rewards=(("guhs:mepmunt", 3),), x=-4, y=y, xp=100)
    q("meppen_500", "Mepper", "Haal 500 punten in een potje Mika meppen. Tip: 5 op rij = hogere combo.", "guhs:mepmunt",
      [adv("mika_meppen_500")], rewards=(("guhs:mepmunt", 3),), x=-2, y=y, xp=200)
    q("meppen_1500", "Mika-meester", "Haal 1500 punten in een potje Mika meppen. Laat geen Mika ontsnappen!", "guhs:mepmunt",
      [adv("mika_meppen_1500")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=0, y=y, xp=300)
    q("meppen_2500", "VAHOEGE MEPPER", "2500 punten! Zo snel heeft de Mepguh nog nooit iemand zien meppen. Sta jij al in de &6top 3&r op de scorebordmuur? VAHOEG!", "guhs:mika_mep_trofee",
      [adv("mika_meppen_2500")], rewards=(("guhs:vahoege_vads_ingot", 2),), x=2, y=y, shape="gear", xp=500)
    q("meppen_medaille", "Mika-mepper-medaille", "Koop de Mika-mepper-medaille bij de Mepguh (4 mepmunten). Wat staat hij je guh goed!",
      "guhs:mikamepper_medaille", [item("guhs:mikamepper_medaille")], x=4, y=y)
    q("meppen_pakje", "Mika-jager", "Koop het hele Mika-jagerpakje voor je guh: de jagershoed, het mepvest met zakjes en de medaille. Alleen te koop bij de Mepguh!",
      "guhs:mikajager_hoed", [adv("guhs:guhmension/mika_mep_pakje")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=6, y=y, shape="gear", xp=300)
