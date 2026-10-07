"""
Textures and models of the guhpixel slice "grap2":

  blocks   guhmon_badgedoos (a multipart: the open case + one little model per badge that is in it), bzg_brievenbus,
           bzg_ingelijste_brief
  items    guhmon_badge_dutjes / _njeg / _knabbel (16 x 16 icons; the same pictures lie in the case)
  NPCs     Gymleider Dutjes (trainer cap, gym scarf), Presentatrice Guhvon (blonde hairdo, pink scarf, microphone), Boer
           Guhrrit (straw hat, blue overall, a straw in his mouth): the sitting guh with extra bones (sterrenwacht_hulp)

Everything is drawn with fixed seeds, so a rerun gives the same bytes.
"""
import random

import numpy as np
from PIL import Image

from features import guhpixel_lib as lib
from features import sterrenwacht_hulp as hulp
from features import uvfix

BADGES = ("dutjes", "njeg", "knabbel")
# where the badges lie in the case (model coordinates of the slot's corner, facing north): 4 columns x 2 rows
VAKJES = [(2.75 + k * 2.75, 4.6 + r * 3.6) for r in range(2) for k in range(4)]
BADGE_VAK = {"dutjes": 0, "njeg": 1, "knabbel": 2}

HOUT = (150, 104, 62)
FLUWEEL = (106, 62, 140)


def _ruis(kleur, var, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in kleur) + (255,))
    return img


def _icoon(rows, kleuren):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in kleuren:
                img.putpixel((x, y), kleuren[ch] + (255,))
    return img


# --- the three badges ------------------------------------------------------------------------------------------------------
BADGE_DUTJES = [
    "................",
    ".....gggggg.....",
    "...ggbbbbbbgg...",
    "..gbbbbbbbbbbg..",
    "..gbbbmmmbbwbg..",
    ".gbbbmmbbbwwwbg.",
    ".gbbmmbbbbbbwbg.",
    ".gbbmmbbbbbwbbg.",
    ".gbbmmbbbbwwwbg.",
    ".gbbbmmbbbbbbbg.",
    "..gbbbmmmmbbbg..",
    "..gbbbbbbbbbbg..",
    "...ggbbbbbbgg...",
    ".....gggggg.....",
    "................",
    "................",
]
BADGE_NJEG = [
    "................",
    "..gggggggggggg..",
    ".gppppppppppppg.",
    ".gpwwwwwwwwwwpg.",
    ".gpwdwwdwdddwpg.",
    ".gpwddwdwwdwwpg.",
    ".gpwdwddwwdwwpg.",
    ".gpwdwwdwddwwpg.",
    ".gpwwwwwwwwwwpg.",
    ".gppppppwwpppg..",
    "..ggggggpwpgg...",
    "........gpg.....",
    ".........g......",
    "................",
    "................",
    "................",
]
BADGE_KNABBEL = [
    "................",
    "......gggg......",
    "....ggyyyygg....",
    "...gyyyyyyyyg...",
    "..gyyyoyyyyyyg..",
    "..gyyooyyyyoyg..",
    ".gyyyyyyyyyyyyg.",
    ".gyyyyyyoyyyyyg.",
    ".gyoyyyyooyyyyg.",
    ".gyyyyyyyyyyoyg.",
    "..gyyyyyyyyyyg..",
    "..gyyyoyyyyyyg..",
    "...gyyyyyyyyg...",
    "....ggyyyygg....",
    "......gggg......",
    "................",
]


def badges(h):
    goud = (214, 164, 52)
    h.save(_icoon(BADGE_DUTJES, {"g": goud, "b": (62, 54, 128), "m": (250, 226, 120), "w": (236, 236, 250)}), "item", "guhmon_badge_dutjes.png")
    h.save(_icoon(BADGE_NJEG, {"g": goud, "p": (236, 96, 160), "w": (255, 246, 250), "d": (120, 40, 90)}), "item", "guhmon_badge_njeg.png")
    h.save(_icoon(BADGE_KNABBEL, {"g": (196, 132, 40), "y": (250, 206, 70), "o": (232, 150, 50)}), "item", "guhmon_badge_knabbel.png")
    for b in BADGES:
        h.w(f"{h.A}/models/item/guhmon_badge_{b}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/guhmon_badge_{b}"}})


# --- the badge case --------------------------------------------------------------------------------------------------------
def badgedoos(h, naam, lore):
    h.save(_ruis(HOUT, 9, 33101), "block", "guhmon_badgedoos_hout.png")
    fluweel = _ruis(FLUWEEL, 6, 33102)
    # the eight empty places show as darker dents in the velvet (the texture lies 1:1 on the tray: 12 x 8 model units)
    h.save(fluweel, "block", "guhmon_badgedoos_fluweel.png")
    h.save(_ruis((78, 44, 108), 4, 33103), "block", "guhmon_badgedoos_vakje.png")
    el = h.el
    elements = [
        el([1, 0, 3], [15, 2, 13], "#hout"),                                         # the tray
        el([1.6, 2, 3.6], [14.4, 2.3, 12.4], "#fluweel"),                            # its velvet
        el([1, 2, 12.4], [15, 10.5, 13], "#hout"),                                   # the open lid, standing up at the back
        el([1.6, 2.6, 12.1], [14.4, 9.9, 12.4], "#fluweel", faces=("north", "up", "down", "west", "east")),
        el([7, 9.6, 12.0], [9, 10.2, 12.2], "#goud"),                                 # a little golden clasp
    ]
    for (x, z) in VAKJES:
        elements.append(el([x, 2.3, z], [x + 2.2, 2.4, z + 2.2], "#vakje", faces=("up", "north", "south", "west", "east")))
    textures = {"particle": "guhs:block/guhmon_badgedoos_hout", "hout": "guhs:block/guhmon_badgedoos_hout",
                "fluweel": "guhs:block/guhmon_badgedoos_fluweel", "vakje": "guhs:block/guhmon_badgedoos_vakje", "goud": "minecraft:block/gold_block"}
    lib.deco(h, "guhmon_badgedoos", elements, textures, naam, lore, display={
        "gui": {"rotation": [30, 200, 0], "translation": [0, 2, 0], "scale": [0.8, 0.8, 0.8]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
        "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, -4], "scale": [0.9, 0.9, 0.9]},
        "thirdperson_righthand": {"rotation": [75, 180, 0], "translation": [0, 2.5, 1], "scale": [0.4, 0.4, 0.4]},
        "firstperson_righthand": {"rotation": [10, 200, 0], "translation": [0, 4, 0], "scale": [0.5, 0.5, 0.5]}})
    multipart = [{"when": {"facing": f}, "apply": {"model": "guhs:block/guhmon_badgedoos", **({"y": y} if y else {})}} for f, y in lib.ROT]
    for b in BADGES:
        x, z = VAKJES[BADGE_VAK[b]]
        badge = el([x - 0.1, 2.4, z - 0.1], [x + 2.3, 2.8, z + 2.3], "#badge")
        for face in badge["faces"].values():
            face["uv"] = [1, 1, 15, 15]
        badge["faces"]["up"]["rotation"] = 180          # (upright for whoever looks into the case from its front)
        for face in ("north", "south", "west", "east", "down"):
            badge["faces"][face] = {"texture": "#rand", "uv": [0, 0, 16, 2]}
        h.w(f"{h.A}/models/block/guhmon_badgedoos_{b}.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": f"guhs:item/guhmon_badge_{b}", "badge": f"guhs:item/guhmon_badge_{b}", "rand": "minecraft:block/gold_block"},
            "elements": uvfix.binnen([badge])})
        for f, y in lib.ROT:
            multipart.append({"when": {"facing": f, b: "true"}, "apply": {"model": f"guhs:block/guhmon_badgedoos_{b}", **({"y": y} if y else {})}})
    h.w(f"{h.A}/blockstates/guhmon_badgedoos.json", {"multipart": multipart})
    # the case drops itself and every badge that is in it
    pools = [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:guhmon_badgedoos"}]}]
    for b in BADGES:
        pools.append({"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:guhmon_badge_{b}"}],
                      "conditions": [{"condition": "minecraft:block_state_property", "block": "guhs:guhmon_badgedoos", "properties": {b: "true"}}]})
    h.w(f"{h.D}/loot_table/blocks/guhmon_badgedoos.json", {"type": "minecraft:block", "pools": pools})


# --- the mailbox and the framed letter ---------------------------------------------------------------------------------------
def brievenbus(h, naam, lore):
    rood = _ruis((204, 52, 60), 7, 33201)
    for x in range(16):                                   # two darker bands, like sheet metal
        for y in (4, 11):
            r, g, b, a = rood.getpixel((x, y))
            rood.putpixel((x, y), (int(r * 0.8), int(g * 0.8), int(b * 0.8), a))
    h.save(rood, "block", "bzg_brievenbus.png")
    voor = _ruis((214, 66, 72), 5, 33202)
    for x in range(3, 13):                                # the slot, with a letter sticking out
        voor.putpixel((x, 6), (60, 22, 30, 255))
        voor.putpixel((x, 7), (60, 22, 30, 255))
    for x in range(5, 11):
        voor.putpixel((x, 6), (250, 246, 232, 255))
    for (x, y) in ((7, 11), (8, 11), (6, 10), (7, 10), (8, 10), (9, 10), (6, 9), (9, 9), (7, 12), (8, 12)):   # a little pink heart
        voor.putpixel((x, y), (255, 170, 204, 255))
    h.save(voor, "block", "bzg_brievenbus_voor.png")
    h.save(_ruis((126, 88, 52), 8, 33203), "block", "bzg_brievenbus_paal.png")
    el = h.el
    voorkant = el([4, 9, 3], [12, 15, 13], "#rood")
    voorkant["faces"]["north"] = {"texture": "#voor", "uv": [0, 0, 16, 16]}
    elements = [
        el([7, 0, 7], [9, 9, 9], "#paal"),
        voorkant,
        el([5, 15, 3], [11, 16, 13], "#rood"),                                        # the rounded top
        el([12, 10, 5], [12.6, 15.5, 6], "#vlag"),                                    # the little flag: up, there is post!
        el([12, 13.5, 6], [12.6, 15.5, 8.5], "#vlag"),
        el([5.5, 12.3, 2.2], [10.5, 12.6, 3.4], "#brief"),                            # the letter in the slot
    ]
    textures = {"particle": "guhs:block/bzg_brievenbus", "rood": "guhs:block/bzg_brievenbus", "voor": "guhs:block/bzg_brievenbus_voor",
                "paal": "guhs:block/bzg_brievenbus_paal", "vlag": "minecraft:block/yellow_concrete", "brief": "minecraft:block/white_concrete"}
    lib.deco(h, "bzg_brievenbus", elements, textures, naam, lore, display={
        "gui": {"rotation": [20, 215, 0], "translation": [0, -1, 0], "scale": [0.72, 0.72, 0.72]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
        "thirdperson_righthand": {"rotation": [75, 180, 0], "translation": [0, 1, 1], "scale": [0.35, 0.35, 0.35]},
        "firstperson_righthand": {"rotation": [0, 215, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]}})
    h.shaped("bzg_brievenbus", ["RIR", "IPI", " S "],
             {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "P": "minecraft:paper", "S": "minecraft:stick"}, "guhs:bzg_brievenbus", 1)


def ingelijste_brief(h, naam, lore):
    rng = random.Random(33301)
    brief = Image.new("RGBA", (16, 16), (252, 244, 220, 255))
    for y in range(16):
        for x in range(16):
            v = rng.randint(-5, 5)
            brief.putpixel((x, y), (252 + min(0, v), 244 + v, 220 + v, 255))
    for y in (3, 5, 7, 9, 11):                            # the handwriting
        for x in range(2, 14 - (y % 4)):
            if rng.random() < 0.82:
                brief.putpixel((x, y), (96, 70, 52, 255))
    for x in range(2, 8):
        brief.putpixel((x, 1), (96, 70, 52, 255))
    for (x, y) in ((11, 13), (12, 13), (13, 13), (11, 12), (13, 12), (12, 14)):       # a heart instead of a signature
        brief.putpixel((x, y), (232, 96, 150, 255))
    for x in range(12, 15):                               # the stamp
        for y in range(1, 3):
            brief.putpixel((x, y), (232, 96, 150, 255))
    h.save(brief, "block", "bzg_ingelijste_brief.png")
    h.save(_ruis((140, 96, 54), 9, 33302), "block", "bzg_ingelijste_brief_lijst.png")
    el = h.el
    papier = el([3, 2, 14.6], [13, 14, 15], "#brief", faces=("north",))
    papier["faces"]["north"]["uv"] = [0, 0, 16, 16]
    elements = [
        el([2, 1, 15], [14, 15, 16], "#lijst"),                                         # the back board
        el([2, 14, 14.4], [14, 15, 15], "#lijst"), el([2, 1, 14.4], [14, 2, 15], "#lijst"),     # the frame: top, bottom
        el([2, 2, 14.4], [3, 14, 15], "#lijst"), el([13, 2, 14.4], [14, 14, 15], "#lijst"),     # left, right
        papier,
    ]
    lib.muurdeco(h, "bzg_ingelijste_brief", elements,
                 {"particle": "guhs:block/bzg_ingelijste_brief_lijst", "lijst": "guhs:block/bzg_ingelijste_brief_lijst",
                  "brief": "guhs:block/bzg_ingelijste_brief"}, naam, lore)


# --- the three NPCs ----------------------------------------------------------------------------------------------------------
def _npc(h, kind, bones_fn, hue, sat, val, schilder):
    geo_file, geo = hulp.sitting_geo(h, f"geometry.guh_npc_{kind}")
    sw = bones_fn(geo)
    hulp.save_geo(h, f"guh_npc_{kind}.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=hue, sat=sat, val=val)
    schilder(a, sw)
    h.save(Image.fromarray(a), "entity", f"npc_{kind}.png")


def gymleider(h):
    c = hulp.cube

    def bones(geo):
        sw = hulp.swatches(geo, ["pet", "petwit", "klep", "sjaal"])
        geo["bones"].append({"name": "dutjes_pet", "parent": "head", "pivot": [0, 26, -1], "cubes": [
            c([-6.8, 24.2, -7.4], [13.6, 2.6, 13.0], sw["pet"]), c([-6.0, 26.8, -6.4], [12, 1.0, 11], sw["pet"]),
            c([-3.4, 24.4, -7.7], [6.8, 2.6, 0.4], sw["petwit"]),
            c([-5.6, 24.2, -11.2], [11.2, 0.6, 3.9], sw["klep"])]})
        geo["bones"].append({"name": "dutjes_sjaal", "parent": "body", "pivot": [0, 13, 0], "cubes": [
            c([-5.2, 11.4, -4.6], [10.4, 2.0, 8.4], sw["sjaal"], inflate=0.1), c([2.8, 7.0, -4.9], [2.2, 4.6, 0.8], sw["sjaal"])]})
        return sw

    def schilder(a, sw):
        rng = np.random.default_rng(33401)
        hulp.paint_swatch(a, sw["pet"], (216, 52, 62), rng, 8)

        def knabbel(block):
            for y in range(32):
                for x in range(32):
                    if ((x - 15.5) ** 2 + (y - 15.5) ** 2) ** 0.5 < 9:
                        block[y, x, :3] = (250, 206, 70)
            block[12:15, 12:15, :3] = (232, 150, 50)
            block[18:20, 17:20, :3] = (232, 150, 50)
        hulp.paint_swatch(a, sw["petwit"], (250, 250, 248), rng, 4, knabbel)
        hulp.paint_swatch(a, sw["klep"], (150, 30, 44), rng, 6)

        def streep(block):
            for x in range(32):
                if (x // 5) % 2:
                    block[:, x, :3] = (250, 226, 120)
        hulp.paint_swatch(a, sw["sjaal"], (92, 70, 170), rng, 6, streep)

    _npc(h, "guhmon_gymleider", bones, 0.70, 0.85, 1.0, schilder)


def presentatrice(h):
    c = hulp.cube

    def bones(geo):
        sw = hulp.swatches(geo, ["haar", "sjaal", "micro", "microkop"])
        geo["bones"].append({"name": "guhvon_haar", "parent": "head", "pivot": [0, 26, -1], "cubes": [
            c([-7.0, 24.4, -7.3], [14.0, 2.8, 13.6], sw["haar"]), c([-5.6, 27.2, -5.6], [11.2, 1.2, 10.4], sw["haar"]),
            c([-7.7, 16.6, -4.6], [1.0, 8.2, 7.4], sw["haar"]), c([6.7, 16.6, -4.6], [1.0, 8.2, 7.4], sw["haar"]),
            c([-6.6, 15.8, 5.4], [13.2, 9.4, 1.1], sw["haar"]),
            c([-6.6, 23.4, -7.7], [5.0, 1.4, 0.5], sw["haar"]), c([2.2, 23.0, -7.7], [4.4, 1.8, 0.5], sw["haar"])]})
        geo["bones"].append({"name": "guhvon_sjaal", "parent": "body", "pivot": [0, 13, 0], "cubes": [
            c([-5.2, 11.4, -4.6], [10.4, 2.0, 8.4], sw["sjaal"], inflate=0.1), c([-4.6, 7.4, -4.9], [2.0, 4.2, 0.8], sw["sjaal"])]})
        geo["bones"].append({"name": "guhvon_micro", "parent": "body", "pivot": [4.6, 8, -5.6], "cubes": [
            c([4.0, 6.4, -6.2], [1.2, 4.6, 1.2], sw["micro"]), c([3.6, 11.0, -6.6], [2.0, 2.0, 2.0], sw["microkop"])]})
        return sw

    def schilder(a, sw):
        rng = np.random.default_rng(33402)

        def lok(block):
            for x in range(0, 32, 5):
                block[:, x, :3] = (222, 176, 78)
        hulp.paint_swatch(a, sw["haar"], (246, 212, 112), rng, 8, lok)
        hulp.paint_swatch(a, sw["sjaal"], (240, 110, 170), rng, 6)
        hulp.paint_swatch(a, sw["micro"], (70, 66, 78), rng, 5)

        def gaas(block):
            for y in range(32):
                for x in range(32):
                    if (x // 3 + y // 3) % 2:
                        block[y, x, :3] = (44, 42, 52)
        hulp.paint_swatch(a, sw["microkop"], (96, 94, 108), rng, 5, gaas)

    _npc(h, "bzg_presentatrice", bones, 0.92, 1.0, 1.02, schilder)


def boer(h):
    c = hulp.cube

    def bones(geo):
        sw = hulp.swatches(geo, ["stro", "band", "overall", "knoop", "halm"])
        geo["bones"].append({"name": "guhrrit_hoed", "parent": "head", "pivot": [0, 26, 0], "cubes": [
            c([-8.6, 25.4, -9.2], [17.2, 0.7, 16.4], sw["stro"]),
            c([-4.6, 26.1, -5.0], [9.2, 3.4, 8.4], sw["stro"]),
            c([-4.6, 26.1, -5.0], [9.2, 1.2, 8.4], sw["band"], inflate=0.12)]})
        geo["bones"].append({"name": "guhrrit_halm", "parent": "head", "pivot": [1.2, 16.2, -7.6], "cubes": [
            c([1.2, 16.0, -8.1], [5.4, 0.45, 0.45], sw["halm"])]})
        geo["bones"].append({"name": "guhrrit_overall", "parent": "body", "pivot": [0, 8, -4], "cubes": [
            c([-4.2, 2.2, -4.7], [8.4, 8.6, 0.5], sw["overall"]),
            c([-4.2, 10.8, -4.65], [1.2, 2.2, 0.4], sw["overall"]), c([3.0, 10.8, -4.65], [1.2, 2.2, 0.4], sw["overall"]),
            c([-3.9, 9.6, -4.95], [0.9, 0.9, 0.3], sw["knoop"]), c([3.0, 9.6, -4.95], [0.9, 0.9, 0.3], sw["knoop"]),
            c([-2.0, 4.4, -4.95], [4.0, 2.6, 0.3], sw["overall"], inflate=0.05)]})
        return sw

    def schilder(a, sw):
        rng = np.random.default_rng(33403)

        def vlecht(block):
            for i in range(0, 32, 3):
                block[i, :, :3] = (np.asarray(block[i, :, :3], np.float32) * 0.85).astype(np.uint8)
        hulp.paint_swatch(a, sw["stro"], (226, 196, 110), rng, 10, vlecht)

        def ruit(block):
            for y in range(32):
                for x in range(32):
                    if (x // 4 + y // 4) % 2:
                        block[y, x, :3] = (250, 250, 248)
        hulp.paint_swatch(a, sw["band"], (204, 52, 60), rng, 4, ruit)

        def stiksel(block):
            block[2:4, :, :3] = (150, 186, 232)
            block[:, 2:4, :3] = (150, 186, 232)
            block[:, 28:30, :3] = (150, 186, 232)
        hulp.paint_swatch(a, sw["overall"], (62, 104, 176), rng, 8, stiksel)
        hulp.paint_swatch(a, sw["knoop"], (236, 196, 76), rng, 4)
        hulp.paint_swatch(a, sw["halm"], (206, 170, 84), rng, 6)

    _npc(h, "bzg_boer", bones, 0.07, 0.62, 1.03, schilder)


def build(h, teksten):
    badges(h)
    badgedoos(h, teksten["block.guhs.guhmon_badgedoos"], teksten["block.guhs.guhmon_badgedoos.lore"])
    brievenbus(h, teksten["block.guhs.bzg_brievenbus"], teksten["block.guhs.bzg_brievenbus.lore"])
    ingelijste_brief(h, teksten["block.guhs.bzg_ingelijste_brief"], teksten["block.guhs.bzg_ingelijste_brief.lore"])
    gymleider(h)
    presentatrice(h)
    boer(h)
