"""
3.0 (Guhverhalen), slice hemel: Het Hemelkapelletje, het Knuffelhart en de Herinnering (DESIGN_30 §4, CONTRACT_30).

  - the structure hemelkapelletje (hemel_bouw.py: a cloud chapel on a floating islet above a little cloud plaza with two
    wolkenliften; GUHMENSION_LAND, spacing 100 / separation 36 (1.0.0, was 60 / 22): the rarest story building) + its hemelkist
  - the block knuffelhart (a glowing pink heart under a glass dome on a little golden pedestal; not craftable, not minable,
    no item: it only exists in the chapel): block model (pedestal + dome, translucent) and the heart + glow models the
    KnuffelhartRenderer draws beating
  - the wolkenhoeder (NPC WOLKENHOEDER, hemel_npc.py): name, Guhdex page, its own model
  - the Herinnering star (guhs:herinnering): a twinkling animated icon, name and tooltip texts
  - the particle hemel_sterretje (a twinkly little star: the heart's sparkle and a guh's GLANS after coming back)
  - sounds (hemel_geluid.py): hemel.muziek (the chapel's soft music), hemel.hartklop, hemel.terug, hemel.ster
  - the clothes (GuhClothes marker <hemel>, source "hemel"): the gouden aureooltje (HEAD) and the wolkenvleugeltjes (BACK)
  - texts (all Dutch, also in en_us), advancements (tab verhalen + hidden quest ones), the game test template, FTB section
    "Het Hemelkapelletje"
"""
import os
import re

import numpy as np
from PIL import Image

from features import hemel_bouw, hemel_geluid, hemel_npc, spelen, uvfix, verhaal

NAME = hemel_bouw.NAME
CLOTHES = ["hemel_aureooltje", "hemel_wolkenvleugeltjes"]
H_PIVOT = [0, 6, -2]            # the standing guh's head pivot (make_guh_variants.HEAD_PIVOT)
B_PIVOT = [0, 6, 6]             # the body pivot


# =====================================================================================================================
# the clothes (make_guh_variants.py / make_clothes_icons.py)
# =====================================================================================================================
def _vleugel(sx):
    """One little cloud wing on the back, sx = +1 (left) or -1 (right): puffs stepping outwards and up."""
    cubes = []
    for (x, y, z, w, hgt, d) in ((2.2, 10.8, 1.8, 3.2, 2.6, 3.8), (4.8, 11.6, 1.4, 3.2, 3.2, 3.6), (7.4, 12.8, 1.6, 2.8, 3.4, 3.0),
                                 (9.6, 14.4, 1.9, 2.0, 2.8, 2.4), (4.6, 10.2, 2.2, 3.4, 1.6, 2.8), (7.2, 11.6, 2.3, 2.6, 1.4, 2.2)):
        ox = x if sx > 0 else -x - w
        cubes.append(([round(ox, 2), y, z], [w, hgt, d], 0.05))
    return cubes


def _rand(sx):
    """The golden edge along the top of a wing."""
    cubes = []
    for (x, y, w) in ((2.4, 13.4, 2.8), (5.0, 14.8, 2.8), (7.6, 16.2, 2.4), (9.7, 17.2, 1.6)):
        ox = x if sx > 0 else -x - w
        cubes.append(([round(ox, 2), y, 2.6], [w, 0.5, 1.2], 0))
    return cubes


def _halo():
    """A golden ring floating above the head (front, back, sides and four rounded corners)."""
    y, t = 18.6, 0.6
    return [([-2.6, y, -9.4], [5.2, t, 0.7], 0), ([-2.6, y, -2.9], [5.2, t, 0.7], 0),
            ([-3.9, y, -8.1], [0.7, t, 4.5], 0), ([3.2, y, -8.1], [0.7, t, 4.5], 0),
            ([-3.4, y, -8.9], [0.9, t, 0.9], 0), ([2.5, y, -8.9], [0.9, t, 0.9], 0),
            ([-3.4, y, -3.4], [0.9, t, 0.9], 0), ([2.5, y, -3.4], [0.9, t, 0.9], 0)]


BONES = {
    "outfit_hemel_aureool": ("head", H_PIVOT, "hemel_goud", _halo()),
    "outfit_hemel_aureool_glans": ("head", H_PIVOT, "hemel_glans", [([-0.4, 18.5, -9.6], [0.8, 0.8, 0.4], 0)]),
    "outfit_hemel_vleugel_links": ("body", B_PIVOT, "hemel_wolk", _vleugel(1)),
    "outfit_hemel_vleugel_rechts": ("body", B_PIVOT, "hemel_wolk", _vleugel(-1)),
    "outfit_hemel_vleugel_rand": ("body", B_PIVOT, "hemel_goud", _rand(1) + _rand(-1)),
}


def _goud(v, rng, glans=(255, 250, 206)):
    a = v.fabric((246, 196, 64), rng, 6)
    n = a.shape[0]
    for y in range(n):
        for x in range(n):
            t = abs(((x + y) % 16) - 5) / 5.0
            a[y, x] = a[y, x] * (0.72 + 0.28 * min(1, t)) + np.array(glans, np.float32) * 0.3 * max(0, 1 - t)
    for y, x in ((3, 6), (11, 21), (19, 4), (26, 16)):
        a[y, x] = (255, 255, 244)
    return np.clip(a, 0, 255)


def _wolk(v, rng):
    """Cloud fluff: white with soft pink and sky-blue shadows and a few sparkles."""
    a = v.fabric((250, 250, 255), rng, 8)
    n = a.shape[0]
    r = np.random.default_rng(3011)
    for (col, k) in (((255, 200, 226), 6), ((200, 226, 252), 5)):
        for _ in range(k):
            cx, cy, rad = r.integers(0, n), r.integers(0, n), r.integers(3, 7)
            for y in range(max(0, cy - rad), min(n, cy + rad)):
                for x in range(max(0, cx - rad), min(n, cx + rad)):
                    if (x - cx) ** 2 + (y - cy) ** 2 < rad * rad:
                        a[y, x] = a[y, x] * 0.6 + np.array(col, np.float32) * 0.4
    for _ in range(8):
        x, y = r.integers(1, n - 1, 2)
        a[y, x] = (255, 244, 200)
    return np.clip(a, 0, 255)


def clothes(rng, v):
    return {
        "hemel_aureooltje": {"hemel_goud": lambda: _goud(v, rng), "hemel_glans": lambda: np.clip(v.fabric((255, 250, 226), rng, 4), 0, 255)},
        "hemel_wolkenvleugeltjes": {"hemel_wolk": lambda: _wolk(v, rng), "hemel_goud": lambda: _goud(v, rng)},
    }


AUREOOL_ICON = ["................", "................", "................", "....gggggggg....", "..gghhhhhhhhgg..", ".gh..........hg.",
                ".g............g.", ".gh..........hg.", "..ggaaaaaaaagg..", "....gggggggg....", "................", "................",
                "......p..p......", ".....ppppp......", "......ppp.......", ".......p........"]
VLEUGEL_ICON = ["................", ".gg..........gg.", "gwwg........gwwg", "gwwwg......gwwwg", "gwbwwg....gwwbwg", ".gwwwwg..gwwwwg.",
                ".gwwbwwggwwbwwg.", "..gwwwwppwwwwg..", "..gwwbwppwbwwg..", "...gwwwwwwwwg...", "...gwwbwwbwwg...", "....gwwwwwwg....",
                ".....gwwwwg.....", "......gggg......", "................", "................"]


def icons(ic):
    return {"hemel_aureooltje": ic.icon(AUREOOL_ICON, {"g": (238, 186, 52), "h": (255, 236, 150), "a": (176, 124, 30),
                                                        "p": (246, 120, 176)}),
            "hemel_wolkenvleugeltjes": ic.icon(VLEUGEL_ICON, {"g": (226, 180, 60), "w": (252, 252, 255), "b": (200, 224, 250),
                                                               "p": (246, 130, 180)})}


# =====================================================================================================================
# the Knuffelhart block: pedestal + glass dome (block model), the heart and its glow (drawn by KnuffelhartRenderer)
# =====================================================================================================================
HART_ROWS = [".XX...XX.", "XXXX.XXXX", "XXXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "...XXX...", "....X...."]
HART_X0, HART_Y0 = 4, 5                   # the heart spans x 4..13, y 5..13 (model pixels), z 6..10


def _hart_cells():
    out = set()
    for i, row in enumerate(HART_ROWS):
        for j, ch in enumerate(row):
            if ch == "X":
                out.add((HART_X0 + j, HART_Y0 + len(HART_ROWS) - 1 - i))
    return out


def _hart_elements(tex, z0=6.0, z1=10.0, extra=0.0):
    """The heart as one element per run of pixels in a row; the front/back faces map the heart picture, the sides a pink strip."""
    cells = _hart_cells()
    els = []
    for y in sorted({c[1] for c in cells}):
        xs = sorted(x for (x, yy) in cells if yy == y)
        runs, start = [], xs[0]
        for a, b in zip(xs, xs[1:] + [None]):
            if b != a + 1:
                runs.append((start, a + 1))
                start = b
        for (x0, x1) in runs:
            fx0, fx1, fy0, fy1 = x0 - extra, x1 + extra, y - extra, y + 1 + extra
            uv = [x0, 16 - (y + 1), x1, 16 - y]
            side = [8, 8, 9, 9]
            els.append({"from": [fx0, fy0, z0 - extra], "to": [fx1, fy1, z1 + extra], "faces": {
                "north": {"uv": [16 - x1, 16 - (y + 1), 16 - x0, 16 - y], "texture": tex},
                "south": {"uv": uv, "texture": tex},
                "east": {"uv": side, "texture": tex}, "west": {"uv": side, "texture": tex},
                "up": {"uv": side, "texture": tex}, "down": {"uv": side, "texture": tex}}})
    # a rounder middle: a slightly thicker core
    els.append({"from": [6 - extra, 7 - extra, z0 - 0.8 - extra], "to": [11 + extra, 12 + extra, z1 + 0.8 + extra], "faces": {
        f: {"uv": [6, 4, 11, 9] if f in ("north", "south") else [8, 8, 9, 9], "texture": tex} for f in ("north", "south", "east", "west", "up", "down")}})
    return els


def _hart_texture(glow=False):
    img = np.zeros((16, 16, 4), np.uint8)
    cells = _hart_cells()
    for (x, y) in cells:
        py = 16 - (y + 1)
        edge = any((x + a, y + b) not in cells for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if glow:
            img[py, x] = (255, 150, 206, 120 if edge else 90)
            continue
        col = np.array((240, 76, 150)) if edge else np.array((255, 118, 182))
        shade = (y - HART_Y0) / 8
        col = col * (0.82 + 0.18 * shade)
        img[py, x, :3] = np.clip(col, 0, 255)
        img[py, x, 3] = 255
    if not glow:
        for (x, y) in ((6, 12), (7, 12), (6, 11), (5, 11)):        # the shine on the left lobe
            img[16 - (y + 1), x] = (255, 236, 246, 255)
        img[16 - 12, 11] = (255, 214, 236, 255)
        # the core's front (6..11 x 4..9 in uv) and the side strip stay pink
        for yy in range(4, 10):
            for xx in range(6, 11):
                if img[yy, xx, 3] == 0:
                    img[yy, xx] = (255, 118, 182, 255)
        img[8, 8] = (236, 70, 146, 255)
    else:
        for yy in range(4, 10):
            for xx in range(6, 11):
                if img[yy, xx, 3] == 0:
                    img[yy, xx] = (255, 150, 206, 90)
        img[8, 8] = (255, 150, 206, 100)
    return Image.fromarray(img)


def knuffelhart(h):
    A = h.A
    # textures
    voet = h.noise_tex((244, 240, 236), 4, 3021)
    px = voet.load()
    for i in range(16):
        for j in (0, 15):
            px[i, j] = (240, 196, 72, 255)
            px[j, i] = (240, 196, 72, 255)
    for i in range(2, 14, 3):                                   # little pink hearts round the pedestal
        px[i, 7] = (246, 120, 176, 255)
        px[i + 1, 7] = (246, 120, 176, 255)
        px[i, 8] = (246, 120, 176, 255)
    h.save(voet, "block", "knuffelhart_voet.png")
    h.save(h.noise_tex((246, 198, 70), 12, 3022, spots=[((255, 240, 176), 0.12)]), "block", "knuffelhart_goud.png")
    h.save(h.noise_tex((238, 110, 170), 6, 3023, spots=[((255, 160, 206), 0.2)]), "block", "knuffelhart_kussen.png")
    glas = Image.new("RGBA", (16, 16), (255, 220, 240, 46))
    gp = glas.load()
    for i in range(16):
        gp[i, 15] = (246, 206, 96, 200)                         # a golden foot line
        gp[0, i] = (255, 240, 250, 90)
        gp[15, i] = (255, 240, 250, 90)
    for k in range(5):                                          # the white shine streaks
        gp[3 + k, 3 + k] = (255, 255, 255, 150)
        gp[4 + k, 3 + k] = (255, 255, 255, 90)
    gp[11, 3] = (255, 255, 255, 120)
    gp[12, 4] = (255, 255, 255, 90)
    h.save(glas, "block", "knuffelhart_glas.png")
    h.save(_hart_texture(), "block", "knuffelhart_hart.png")
    h.save(_hart_texture(glow=True), "block", "knuffelhart_gloed.png")

    def box(fr, to, tex, faces=("north", "south", "east", "west", "up", "down")):
        return {"from": fr, "to": to, "faces": {f: {"texture": tex} for f in faces}}

    all6 = ("north", "south", "east", "west", "up", "down")
    no_down = ("north", "south", "east", "west", "up")
    elements = [
        box([1, 0, 1], [15, 2, 15], "#voet", all6),
        box([2, 2, 2], [14, 3, 14], "#goud", no_down),
        box([3, 3, 3], [13, 4, 13], "#kussen", no_down),
        box([2.4, 3, 2.4], [13.6, 4, 13.6], "#goud", ("north", "south", "east", "west")),
        box([2.5, 4, 2.5], [13.5, 13, 13.5], "#glas", no_down),
        box([3.5, 13, 3.5], [12.5, 15.5, 12.5], "#glas", no_down),
        box([5, 15.5, 5], [11, 17, 11], "#glas", no_down),
        box([6.5, 17, 6.5], [9.5, 18, 9.5], "#glas", no_down),
        box([7.25, 18, 7.25], [8.75, 19.5, 8.75], "#goud", no_down),
    ]
    h.w(f"{A}/models/block/knuffelhart.json", {"render_type": "minecraft:translucent", "ambientocclusion": False,
                                                "textures": {"particle": "guhs:block/knuffelhart_goud", "voet": "guhs:block/knuffelhart_voet",
                                                             "goud": "guhs:block/knuffelhart_goud", "kussen": "guhs:block/knuffelhart_kussen",
                                                             "glas": "guhs:block/knuffelhart_glas"},
                                                "elements": uvfix.binnen(elements)})   # (3.0 QA: the dome's uvs stay on the glass)
    h.w(f"{A}/models/block/knuffelhart_hart.json", {"ambientocclusion": False, "textures": {"particle": "guhs:block/knuffelhart_hart",
                                                                                             "hart": "guhs:block/knuffelhart_hart"},
                                                     "elements": _hart_elements("#hart")})
    h.w(f"{A}/models/block/knuffelhart_gloed.json", {"ambientocclusion": False, "textures": {"particle": "guhs:block/knuffelhart_gloed",
                                                                                              "hart": "guhs:block/knuffelhart_gloed"},
                                                      "elements": _hart_elements("#hart", extra=0.6)})
    h.w(f"{A}/blockstates/knuffelhart.json", {"variants": {"": {"model": "guhs:block/knuffelhart"}}})


# =====================================================================================================================
# the Herinnering star and the sparkle particle
# =====================================================================================================================
STER = ["................", ".......gg.......", ".......gg.......", "......gyyg......", "......gyyg......", "gggggyyyyyyggggg",
        ".gyyyyppyppyyyg.", "..gyypppppppyg..", "...gyppppppyg...", "...gyyppppyyg...", "..gyyyyppyyyyg..", "..gyyyggggyyyg..",
        ".gyyyg....gyyyg.", ".gyyg......gyyg.", "gyg..........gyg", "gg............gg"]


def herinnering(h):
    pal = {"g": (224, 160, 40, 255), "y": (255, 226, 110, 255), "p": (246, 110, 176, 255), "w": (255, 255, 255, 255)}
    frames = []
    glints = [((7, 4), (3, 7)), ((12, 9), (8, 3)), ((4, 12), (11, 6)), ((10, 12), (6, 8))]
    for f in range(4):
        img = h.grid(STER, pal)
        px = img.load()
        px[6, 7] = (255, 220, 240, 255)                        # the shine on the little heart
        for (x, y) in glints[f]:
            if px[x, y][3]:
                px[x, y] = (255, 255, 255, 255)
        frames.append(img)
    sheet = Image.new("RGBA", (16, 64))
    for i, fr in enumerate(frames):
        sheet.paste(fr, (0, i * 16))
    h.save(sheet, "item", "herinnering.png")
    h.w(f"{h.TEX}/item/herinnering.png.mcmeta", {"animation": {"frametime": 5, "interpolate": True}})
    h.item_model("herinnering")
    # the particle: a tiny four-pointed twinkle, pink and gold
    for i, (col, size) in enumerate((((255, 236, 150), 1), ((255, 190, 225), 2), ((255, 255, 255), 3), ((255, 190, 225), 2))):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        p = img.load()
        for d in range(-size, size + 1):
            a = 255 if abs(d) < size else 150
            p[3 + d, 3] = col + (a,)
            p[3, 3 + d] = col + (a,)
        p[3, 3] = (255, 255, 255, 255)
        h.save(img, "particle", f"hemel_sterretje_{i}.png")
    h.w(f"{h.A}/particles/hemel_sterretje.json", {"textures": [f"guhs:hemel_sterretje_{i}" for i in range(4)]})


# =====================================================================================================================
# sounds
# =====================================================================================================================
ONDERTITELS = {"muziek": "Zachte hemelmuziek", "hartklop": "Het Knuffelhart klopt", "terug": "Terug uit de wolkjes!",
               "ster": "Een sterretje twinkelt"}


def sounds(h):
    hemel_geluid.schrijf(h)

    def patch(d):
        d["hemel.muziek"] = {"sounds": [{"name": "guhs:hemel/muziek", "stream": True, "volume": 0.55}], "subtitle": "subtitles.guhs.hemel.muziek"}
        for naam, vol in (("hartklop", 0.7), ("terug", 0.9), ("ster", 0.7)):
            d[f"hemel.{naam}"] = {"sounds": [{"name": f"guhs:hemel/{naam}", "volume": vol}], "subtitle": f"subtitles.guhs.hemel.{naam}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    for naam, tekst in ONDERTITELS.items():
        h.lang(f"subtitles.guhs.hemel.{naam}", tekst, tekst)


# =====================================================================================================================
# advancements (tab verhalen) and the hidden quest ones
# =====================================================================================================================
QUEST = ["hemel_hoeder", "hemel_kristal", "hemel_knabbel", "hemel_veertje", "hemel_hart", "hemel_terug", "hemel_ster"]


def advancements(h):
    for name in QUEST:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    z = verhaal.zichtbaar
    z(h, "verhalen", "hemel_kapelletje", "root", "guhs:herinnering", "task", "Hoog in de wolkjes",
      "Vind het Hemelkapelletje op zijn zwevende eilandje en neem de wolkenlift omhoog",
      criteria={"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": [f"guhs:{NAME}"]}}}}})
    z(h, "verhalen", "hemel_hoeder", "hemel_kapelletje", "minecraft:feather", "task", "De wolkenhoeder",
      "Praat met de wolkenhoeder: hij past op het slapende Knuffelhart")
    z(h, "verhalen", "hemel_hart", "hemel_hoeder", "guhs:hemel_aureooltje", "goal", "Ba-dum, ba-dum!",
      "Breng een guhkristal, een gouden kaasknabbel en een pluisveertje: het Knuffelhart klopt weer. VAHOEG!")
    z(h, "verhalen", "hemel_terug", "hemel_hart", "guhs:herinnering", "goal", "Terug uit de wolkjes",
      "Laat het Knuffelhart een van je guhs uit de wolkjes terughalen, met al zijn hartjes en zijn dagboekje")
    z(h, "verhalen", "hemel_ster", "hemel_hart", "minecraft:nether_star", "task", "Een lief sterretje",
      "Breng een Herinnering-sterretje naar het Knuffelhart")
    z(h, "verhalen", "hemel_engeltje", "hemel_hart", "guhs:hemel_wolkenvleugeltjes", "challenge", "Een echt hemelguhtje",
      "Ontgrendel het gouden aureooltje en de wolkenvleugeltjes")


# =====================================================================================================================
# texts (Dutch in both languages)
# =====================================================================================================================
LANG = {
    # the block, the item, the structure
    "block.guhs.knuffelhart": "Het Knuffelhart",
    "item.guhs.herinnering": "Herinnering",
    "item.guhs.herinnering.van": "Herinnering aan %s",
    "item.guhs.herinnering.lore": "Een gloeiend sterretje vol lieve herinneringen. Njeg <3",
    "item.guhs.herinnering.variant": "Een %s",
    "item.guhs.herinnering.naar_het_hart": "Breng het naar het Knuffelhart in het Hemelkapelletje (superkompas, tab Verhalen).",
    "item.guhs.herinnering.ook_zonder": "Het Knuffelhart kan hem ook zonder sterretje terughalen.",
    "item.guhs.herinnering.knuffel": "Rechtsklik: knuffel de herinnering",
    "item.guhs.hemel_aureooltje": "Gouden aureooltje",
    "item.guhs.hemel_wolkenvleugeltjes": "Wolkenvleugeltjes",
    f"structure.guhs.{NAME}": "Het Hemelkapelletje",
    f"structure.guhs.{NAME}.tooltip": "Een klein wolkentempeltje op een zwevend eilandje, met het Knuffelhart",
    "gui.guhs.kledingbron.hemel": "Het Hemelkapelletje",
    # the wolkenhoeder (NPC page)
    "entity.guhs.guh_npc.wolkenhoeder": "De wolkenhoeder",
    "gui.guhs.guhdex.rarity.wolkenhoeder": "Zeldzaamheid: Eén in elk Hemelkapelletje",
    "gui.guhs.guhdex.info.wolkenhoeder": "Een zachte wolkenguh met een gouden herdersstaf en een piepklein aureooltje. Hij past op het "
                                         "Knuffelhart en weet precies wat het nodig heeft om weer te kloppen.",
    # the intro scene
    "gui.guhs.hemel.intro.1": "Oh! Een bezoekje, hier hoog in de wolkjes... Welkom in het Hemelkapelletje, lieve guhvriend. Njeg <3",
    "gui.guhs.hemel.intro.2": "Ik ben de wolkenhoeder. Ik pas op het Knuffelhart: dat roze hartje daar, onder zijn glazen koepeltje.",
    "gui.guhs.hemel.intro.3": "Het Knuffelhart kan guhs die naar de wolkjes zijn gegaan weer thuis laten komen. Met al hun hartjes, "
                              "hun favorietjes, hun kleertjes en hun dagboekje!",
    "gui.guhs.hemel.intro.4": "Maar... het slaapt al heel lang. Zzz... njeg. Om het weer te laten kloppen heb ik drie dingen nodig.",
    "gui.guhs.hemel.intro.5": "Een guhkristal, voor de glinstering. Een gouden kaasknabbel, voor de warmte en de vadsheid. "
                              "En een pluisveertje van de Pluisvinkjes, voor de zachtheid.",
    "gui.guhs.hemel.optie.zoeken": "Ik ga ze zoeken!",
    "gui.guhs.hemel.optie.waar": "Waar vind ik die?",
    "gui.guhs.hemel.optie.wolkjes": "Laat me mijn guhs in de wolkjes zien",
    "gui.guhs.hemel.optie.vertel": "Vertel over het Knuffelhart",
    "gui.guhs.hemel.waar": "Guhkristallen glimmen in de kristalgrotten van de Guhmensie. Gouden kaasknabbels vallen uit de lucht bij een "
                           "kaasregen, en soms liggen ze in de schat van een kaasknabbelnest. Pluisveertjes laten de Pluisvinkjes vallen, "
                           "in de Guhvelden en de Roze pluisjes. Kijk ook eens in mijn hemelkist, in de tuin!",
    "gui.guhs.hemel.gebracht.kristal": "Een guhkristal! Kijk hoe hij glinstert... Het Knuffelhart knippert even. Njeg!",
    "gui.guhs.hemel.gebracht.knabbel": "Een gouden kaasknabbel! Mmm, warm en vads. Het Knuffelhart wordt een beetje rozer...",
    "gui.guhs.hemel.gebracht.veertje": "Een pluisveertje! Zo zacht... Hoor je dat? Heel zachtjes: ba... dum...",
    "gui.guhs.hemel.nog_niks": "Heb je al iets gevonden? Een guhkristal, een gouden kaasknabbel of een pluisveertje... njeg.",
    "quest.guhs.next.hemel_zoeken": "breng de wolkenhoeder een guhkristal, een gouden kaasknabbel en een pluisveertje",
    "quest.guhs.next.hemel_terug": "praat met de wolkenhoeder of klik op het Knuffelhart om een guh uit de wolkjes terug te halen",
    # the heart wakes up
    "gui.guhs.hemel.klopt.1": "Kijk... kijk dan! Het Knuffelhart gloeit...",
    "gui.guhs.hemel.klopt.2": "Ba-dum... ba-dum... HET KLOPT WEER! VAHOEG!",
    "gui.guhs.hemel.klopt.3": "Dank je wel, lieve guhvriend. Vanaf nu kan het Knuffelhart al jouw guhs uit de wolkjes terughalen. "
                              "Gratis, en zo vaak als je wilt. Want niemand wordt vergeten. Njeg.",
    "gui.guhs.hemel.klopt.4": "En voor jou: een gouden aureooltje en wolkenvleugeltjes, voor een van je guhs. Dan lijkt hij net een "
                              "klein wolkenguhtje!",
    "gui.guhs.hemel.klopt.chat": "Het Knuffelhart klopt weer! Klik erop om je guhs uit de wolkjes te zien. VAHOEG!",
    # after the questline
    "gui.guhs.hemel.hoeder.klaar": "Het Knuffelhart klopt, ba-dum ba-dum. Er zijn %s van jouw guhs in de wolkjes. Wil je ze zien?",
    "gui.guhs.hemel.hoeder.klaar_leeg": "Het Knuffelhart klopt, ba-dum ba-dum. Al jouw guhs zijn bij jou: niemand in de wolkjes. Fijn, hè? Njeg <3",
    "gui.guhs.hemel.vertel": "Het Knuffelhart is gemaakt van alle knuffels die ooit aan een guh zijn gegeven. Daarom vergeet het niemand. "
                             "Als een tamme guh naar de wolkjes gaat, blijft er een sterretje achter, en zijn dagboekje blijft in je Guhdex. "
                             "Hier kan het Knuffelhart hem terughalen, gratis en zo vaak als nodig.",
    # the heart
    "gui.guhs.hemel.slaapt": "Het Knuffelhart slaapt nog... zzz. Vraag de wolkenhoeder hoe je het wakker maakt.",
    "gui.guhs.hemel.ster.leeft": "%s is al lang weer terug! Bewaar dit sterretje maar als lieve herinnering. Njeg <3",
    "gui.guhs.hemel.ster.niet_van_jou": "Dit sterretje hoort bij het guhtje van iemand anders... Geef het aan zijn baasje, njeg.",
    "gui.guhs.hemel.ster.knuffel": "Je knuffelt de herinnering aan %s... Njeg <3",
    "gui.guhs.hemel.ster.leeg": "Een sterretje zonder naam... het twinkelt toch heel lief.",
    "gui.guhs.hemel.terug": "VAHOEG! %s is terug uit de wolkjes! Met al zijn hartjes, favorietjes en zijn dagboekje. Njeg <3",
    "gui.guhs.hemel.terug.mislukt": "Njeg... dat lukte niet. Is hij misschien al terug?",
    "gui.guhs.hemel.te_ver": "Ga wat dichter bij het Knuffelhart staan.",
    "gui.guhs.hemel.dood.hint": "Ergens hoog in de lucht staat het Hemelkapelletje. Het Knuffelhart daar kan %s terughalen...",
    "gui.guhs.hemel.dood.hint_klopt": "Het Knuffelhart in het Hemelkapelletje kan %s terughalen. Gratis, en zo vaak als je wilt.",
    "gui.guhs.hemel.mijnguhs_hint": "♥ Het Knuffelhart in het Hemelkapelletje kan hem terughalen (superkompas, tab Verhalen).",
    "gui.guhs.hemel.no_build": "Njeg! Het Hemelkapelletje is heilig: hier mag je niks slopen of bouwen.",
    "gui.guhs.hemel.hart_heel": "Het Knuffelhart kun je niet kapotmaken. Het is gemaakt van knuffels!",
    # the screen
    "gui.guhs.hemel.scherm.titel": "Het Knuffelhart",
    "gui.guhs.hemel.scherm.sub": "Wie wil je terughalen uit de wolkjes?",
    "gui.guhs.hemel.scherm.leeg": "Al jouw guhs zijn hier, bij jou.",
    "gui.guhs.hemel.scherm.leeg2": "Niemand in de wolkjes. Njeg <3",
    "gui.guhs.hemel.scherm.knop": "Haal %s terug ♥",
    "gui.guhs.hemel.scherm.hartjes": "♥ %s hartjes",
    "gui.guhs.hemel.scherm.sinds": "In de wolkjes sinds dag %s",
    "gui.guhs.hemel.scherm.vandaag": "Vandaag naar de wolkjes gegaan",
    "gui.guhs.hemel.scherm.gratis": "Gratis, en zo vaak als je wilt",
    "gui.guhs.hemel.scherm.net": "%s is terug! VAHOEG!",
    "gui.guhs.hemel.scherm.kleertjes": "Met zijn kleertjes aan",
}


DINGEN = ["een guhkristal", "een gouden kaasknabbel", "een pluisveertje"]      # HemelQuest.Ding order (bits 1, 2, 4)


def nodig_teksten():
    """gui.guhs.hemel.nodig.<mask>: what the Knuffelhart still needs (mask = the missing bits)."""
    out = {}
    for mask in range(1, 8):
        dingen = [d for i, d in enumerate(DINGEN) if mask & (1 << i)]
        lijst = dingen[0] if len(dingen) == 1 else ", ".join(dingen[:-1]) + " en " + dingen[-1]
        out[f"gui.guhs.hemel.nodig.{mask}"] = f"Het Knuffelhart heeft nog nodig: {lijst}. Njeg!"
    return out


def texts(h):
    for key, text in {**LANG, **nodig_teksten()}.items():
        h.lang(key, text, text)


# =====================================================================================================================
# game test template, loot, structure
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((12, 8, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:white_concrete" if (x + z) % 2 else "minecraft:pink_concrete")
    t.save("hemel_test_wolk")


def loot(h):
    fn = h.count_fn
    h.w(f"{h.D}/loot_table/chests/{NAME}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:wolkensuikerspin", "functions": fn(2, 4)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 1},
                                 {"type": "minecraft:empty", "weight": 1}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:gouden_kaasknabbel", "weight": 1},
                                 {"type": "minecraft:empty", "weight": 2}]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 4, "functions": fn(6, 14)},
            {"type": "minecraft:item", "name": "minecraft:feather", "weight": 2, "functions": fn(2, 6)},
            {"type": "minecraft:item", "name": "guhs:wolkenlift", "weight": 1, "functions": fn(1, 2)},
            {"type": "minecraft:item", "name": "minecraft:pink_tulip", "weight": 2, "functions": fn(1, 3)}]}]})


def structuur(h):
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[NAME] = 14                # the flatness check samples the ground this far around the plaza
    h.FLATNESS[NAME] = 9
    h.structure(NAME, h.GUHMENSION_LAND, spacing=100, separation=36, salt=20300401, start_y=-hemel_bouw.G, reach=44,
                centre=hemel_bouw.ANCHOR,
                spawn_overrides={"monster": none, "creature": none, "ambient": none})
    s = hemel_bouw.build(h)
    s.save(NAME)


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck():
    problems = []
    for key, text in LANG.items():
        low = text.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {key}")
    kleding = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    blok = kleding[kleding.index("// <hemel>"):kleding.index("// </hemel>")]
    enum = [m.lower() for m in re.findall(r"^\s+([A-Z_]+)\(Slot", blok, re.M)]
    if enum != CLOTHES:
        problems.append(f"GuhClothes <hemel> {enum} != CLOTHES {CLOTHES}")
    for bone in re.findall(r'"(outfit_[a-z_]+)"', blok):
        if not any(b.startswith(bone) for b in BONES):
            problems.append(f"no bone for {bone}")
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "hemel", "HemelProtection.java"), encoding="utf-8").read()
    for const, val in (("SKY_FROM", hemel_bouw.SKY_FROM), ("PLAZA_RADIUS", int(hemel_bouw.PLAZA_R) + 1),
                       ("PLAZA_X", hemel_bouw.PLAZA[0]), ("PLAZA_Z", hemel_bouw.PLAZA[1]), ("GROND", hemel_bouw.G)):
        if not re.search(rf"\b{const}\s*=\s*{val}\b", java):
            problems.append(f"HemelProtection.{const} != {val}")
    if problems:
        raise SystemExit("hemel self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    spelen.stub_npc(h, "wolkenhoeder", "De wolkenhoeder", "Eén in elk Hemelkapelletje", LANG["gui.guhs.guhdex.info.wolkenhoeder"], 0.6)
    hemel_npc.build(h)
    knuffelhart(h)
    herinnering(h)
    sounds(h)
    advancements(h)
    texts(h)
    loot(h)
    structuur(h)
    test_templates(h)
    selfcheck()


# =====================================================================================================================
# FTB quests (chapter guhs_verhalen, section "Het Hemelkapelletje")
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("hemel_kapelletje", "Het Hemelkapelletje", "Hoog boven een klein wolkenpleintje zweeft een eilandje met een wolkentempeltje: het "
      "&dHemelkapelletje&r. Zoek het met je superkompas (tab &dVerhalen&r) en stap in de &bwolkenlift&r. Wieee!",
      "guhs:wolkenlift", [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 10),), shape="gear", xp=100)
    q("hemel_hoeder", "De wolkenhoeder", "In het kapelletje staat de &fwolkenhoeder&r naast het slapende &dKnuffelhart&r. Praat met "
      "hem: hij vertelt wat het hartje nodig heeft.", "minecraft:feather", [adv("hemel_hoeder")], deps=["hemel_kapelletje"], xp=50)
    q("hemel_kristal", "Voor de glinstering", "Breng de wolkenhoeder een &bguhkristal&r. Die glimmen in de kristalgrotten van de "
      "Guhmensie.", "guhs:guh_kristal", [adv("hemel_kristal")], deps=["hemel_hoeder"], xp=50)
    q("hemel_knabbel", "Voor de warmte", "Breng de wolkenhoeder een &6gouden kaasknabbel&r. Die vallen uit de lucht bij een kaasregen "
      "(en soms liggen ze in een schat). Warm en vads!", "guhs:gouden_kaasknabbel", [adv("hemel_knabbel")], deps=["hemel_hoeder"], xp=50)
    q("hemel_veertje", "Voor de zachtheid", "Breng de wolkenhoeder een &dpluisveertje&r. De ronde Pluisvinkjes laten ze soms vallen, in "
      "de Guhvelden en de Roze pluisjes.", "guhs:pluisveertje", [adv("hemel_veertje")], deps=["hemel_hoeder"], xp=50)
    q("hemel_hart", "Ba-dum, ba-dum!", "Met alle drie de dingen wordt het &dKnuffelhart&r wakker en klopt het weer. VAHOEG! Vanaf nu "
      "haalt het al jouw guhs uit de wolkjes terug: &agratis en onbeperkt&r.", "minecraft:pink_dye",
      [adv("hemel_hart")], rewards=(("guhs:kaas_knabbels", 24),), deps=["hemel_kristal", "hemel_knabbel", "hemel_veertje"],
      shape="heart", xp=200)
    q("hemel_aureooltje", "Een gouden aureooltje", "Het bedankje van de wolkenhoeder: een &6gouden aureooltje&r dat boven het hoofd "
      "van je guh zweeft. Houd het ingedrukt om het te ontgrendelen.", "guhs:hemel_aureooltje", [item("guhs:hemel_aureooltje")],
      deps=["hemel_hart"])
    q("hemel_vleugeltjes", "Wolkenvleugeltjes", "En twee kleine &fwolkenvleugeltjes&r voor op de rug. Samen met het aureooltje is je guh "
      "een echt hemelguhtje!", "guhs:hemel_wolkenvleugeltjes", [item("guhs:hemel_wolkenvleugeltjes")], deps=["hemel_hart"])
    q("hemel_terug", "Terug uit de wolkjes", "Is een van je tamme guhs naar de wolkjes gegaan? Klik op het &dKnuffelhart&r en kies hem: "
      "hij komt terug met al zijn hartjes, favorietjes, kleertjes en zijn dagboekje. Niemand wordt vergeten. Njeg.",
      "guhs:herinnering", [adv("hemel_terug")], deps=["hemel_hart"], xp=100)
    q("hemel_ster", "Een lief sterretje", "Als een tamme guh naar de wolkjes gaat, blijft er een gloeiend &dHerinnering&r-sterretje "
      "achter. Breng het naar het Knuffelhart: dan weet het hartje meteen wie je terug wilt.", "minecraft:nether_star",
      [adv("hemel_ster")], deps=["hemel_hart"])
