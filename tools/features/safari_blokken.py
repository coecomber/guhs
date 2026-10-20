"""
Op Safari! (1.6.0), step 2 slice S6 - the safari things (Java: feature/safari/spullen/; contract SAFARI_CONTRACT.md).

  build(h)      textures, models, blockstates, loot, recipes and tags of the safari blocks (rieten dak + trap + plaat,
                bamboewand, bamboe-omheining, safari-acaciaplanken, dierentuinbordje); the items safarivoer, safarihoed
                (worn on the player's head) and verrekijker (zooms like a spyglass); sounds.json for every safari sound
                (the OGGs: tools/features/safari_geluid.py); the painting variants guhs:safari_<naam> (the art:
                tools/features/safari_schilderijen.py); the names (NL here, English in tools/lang/en/c166_safari_spullen.json);
                the dierpatronen for the later pattern brush (only as PNGs in tools/features/safari_patronen/).
  BONES / CLOTHES / clothes / icons
                the guh outfits safarihoedje, safari_rangerpet, safari_vestje, safari_verrekijker (GuhClothes SAFARI_*;
                the bones come from the phase-0 design safari-ontwerp/objecten/kleding_bones.json).
"""
import os

import numpy as np
from PIL import Image

try:
    from features import safari_schilderijen as schilderijen
    from features import safari_geluid as geluid
except ImportError:                                   # (run as a script from the project root)
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import safari_schilderijen as schilderijen
    from features import safari_geluid as geluid

PATRONEN = os.path.join(os.path.dirname(os.path.abspath(__file__)), "safari_patronen")
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
BLOKKEN = ["rieten_dak", "rieten_dak_trap", "rieten_dak_plaat", "bamboewand", "bamboe_omheining", "safari_acaciaplanken", "dierentuinbordje"]

# =====================================================================================================================
# the guh outfits (format of make_guh_variants.BONES: name -> (parent, pivot, swatch, [(origin, size, inflate), ...]))
# =====================================================================================================================
_H = [0, 6, -2]
BONES = {
    "outfit_safari_hat": ("head", _H, "safari_hat", [([-6.5, 15, -12.8], [13, 0.6, 12], 0), ([-4, 15.6, -10], [8, 3.6, 6.5], 0)]),
    "outfit_safari_hat_band": ("head", _H, "safari_hat_band", [([-4.2, 15.6, -10.2], [8.4, 1.0, 6.9], 0)]),
    "outfit_ranger_cap": ("head", _H, "ranger_cap", [([-4.4, 15, -9.6], [8.8, 2.8, 7.4], 0.05), ([-4.4, 15, -13.2], [8.8, 0.6, 3.8], 0)]),
    "outfit_ranger_badge": ("head", _H, "ranger_badge", [([-1.0, 16.0, -9.85], [2.0, 1.6, 0.3], 0)]),
    "outfit_safari_vest": ("body", [0, 6, 6], "safari_vest", [([-6.4, 1.2, -4.2], [12.8, 6.6, 9.0], 0.25)]),
    "outfit_binoculars": ("body", [0, 6, 6], "binoculars", [([-2.4, 2.2, -5.4], [2.0, 2.0, 2.4], 0), ([0.4, 2.2, -5.4], [2.0, 2.0, 2.4], 0),
                                                              ([-0.4, 2.8, -5.2], [0.8, 0.8, 1.6], 0)]),
    "outfit_binoculars_strap": ("body", [0, 6, 6], "binoculars_strap", [([-6.2, 4.4, -4.4], [12.4, 0.5, 0.5], 0),
                                                                        ([-2.0, 3.0, -4.4], [0.4, 1.6, 0.5], 0), ([1.6, 3.0, -4.4], [0.4, 1.6, 0.5], 0)]),
}
CLOTHES = ["safari_hoedje", "safari_rangerpet", "safari_vestje", "safari_verrekijker"]


def clothes(rng, v):
    def hoed():
        a = v.fabric((204, 186, 132), rng, 8)
        for y in range(0, 32, 6):
            a[y, :] = a[y, :] * 0.93
        return a

    def band():
        return v.fabric((124, 82, 48), rng, 6)

    def pet():
        a = v.fabric((112, 126, 72), rng, 8)
        a[0:2, :] = a[0:2, :] * 0.85
        return a

    def badge():
        a = v.fabric((236, 196, 70), rng, 4)
        a[12:20, 12:20] = (250, 230, 140)
        return a

    def vest():
        a = v.fabric((204, 186, 132), rng, 8)
        for (x, y) in ((4, 10), (20, 10), (4, 20), (20, 20)):        # little pockets with a flap
            a[y:y + 6, x:x + 8] = a[y:y + 6, x:x + 8] * 0.9
            a[y:y + 2, x:x + 8] = (150, 128, 84)
        a[:, 15:17] = (150, 128, 84)                                 # the zipper
        return a

    def kijker():
        a = v.fabric((34, 32, 38), rng, 4)
        a[10:20, 10:20] = (120, 170, 210)                            # lens glint
        return a

    def riem():
        return v.fabric((124, 82, 48), rng, 6)

    return {
        "safari_hoedje": {"safari_hat": hoed, "safari_hat_band": band},
        "safari_rangerpet": {"ranger_cap": pet, "ranger_badge": badge},
        "safari_vestje": {"safari_vest": vest},
        "safari_verrekijker": {"binoculars": kijker, "binoculars_strap": riem},
    }


def icons(ic):
    hoed = ["................", ".....aaaaaa.....", "....abbbbbba....", "....abbbbbba....", "....acccccca....",
            ".aaaabbbbbbaaaa.", "abbbbbbbbbbbbbba", ".aaaaaaaaaaaaaa."]
    pet = ["....aaaaaa......", "...abbbbbba.....", "..abbbybbbba....", "..abbbbbbbba....", "..aaaaaaaaaaaaa.",
           "..abbbbbbbbbbba.", "...aaaaaaaaaaa.."]
    vest = ["...aa....aa.....", "..abba..abba....", ".abbbbaabbbba...", ".abbbbccbbbba...", ".adddbccbdddba..",
            ".adddbccbdddba..", ".abbbbccbbbba...", ".abbbbccbbbba...", ".adddbccbdddba..", ".aaaaaaaaaaaa..."]
    kijker = ["..r..........r..", "...r........r...", "....r......r....", ".....r....r.....", "...aaaa..aaaa...",
              "..abbbba.abbbba.", "..abbbbaaabbbba.", "..abbbba.abbbba.", "..acccca.acccca.", "...aaaa..aaaa..."]
    k = (204, 186, 132)
    return {
        "safari_hoedje": ic.icon(ic.pad(hoed), {"a": (120, 100, 64), "b": k, "c": (124, 82, 48)}),
        "safari_rangerpet": ic.icon(ic.pad(pet), {"a": (70, 80, 42), "b": (112, 126, 72), "y": (236, 196, 70)}),
        "safari_vestje": ic.icon(ic.pad(vest), {"a": (120, 100, 64), "b": k, "c": (150, 128, 84), "d": (186, 166, 116)}),
        "safari_verrekijker": ic.icon(ic.pad(kijker), {"a": (14, 12, 18), "b": (44, 42, 50), "c": (120, 170, 210), "r": (124, 82, 48)}),
    }


# =====================================================================================================================
# textures (16x16; the phase-0 sample card's recipes)
# =====================================================================================================================
_rng = np.random.default_rng(16)


def _vlak(c, var=6):
    out = np.zeros((16, 16, 4), float)
    out[..., :3] = np.array(c, float) + _rng.normal(0, var, (16, 16, 1))
    out[..., 3] = 255
    return out


def _put(a, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        a[y, x, :3] = c
        a[y, x, 3] = 255


def _img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def texturen():
    t = {}
    a = _vlak((214, 178, 92), 10)
    for x in range(16):
        for y in range(16):
            if (x * 7 + y * 3) % 5 == 0:
                a[y, x, :3] = (178, 138, 62)
            elif (x * 3 + y) % 7 == 0:
                a[y, x, :3] = (236, 206, 124)
    t["rieten_dak"] = a
    a = _vlak((168, 190, 98), 5)
    for x in range(16):
        if x % 4 == 0:
            a[:, x, :3] = (120, 146, 62)
        elif x % 4 == 1:
            a[:, x, :3] = (196, 214, 128)
    for y in (5, 13):
        a[y, :, :3] = (132, 150, 70)
        a[y - 1, :, :3] = (210, 222, 150)
    t["bamboewand"] = a
    a = _vlak((206, 112, 66), 6)
    for y in range(16):
        if y % 4 == 3:
            a[y, :, :3] = (150, 74, 40)
        for x in range(16):
            if (x + (y // 4) * 5) % 16 == 0 and y % 4 != 3:
                a[y, x, :3] = (162, 84, 46)
    for (x, y) in ((5, 1), (12, 9)):
        _put(a, x, y, (138, 66, 36))
        _put(a, x + 1, y, (176, 92, 52))
    t["safari_acaciaplanken"] = a
    a = _vlak((128, 86, 50), 5)
    a[0, :, :3] = a[15, :, :3] = (92, 60, 32)
    a[:, 0, :3] = a[:, 15, :3] = (92, 60, 32)
    a[2:14, 2:14, :3] = (244, 204, 92)
    olifant = [(4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7),
               (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8), (11, 9), (11, 10), (4, 9), (5, 9),
               (8, 9), (9, 9), (4, 10), (5, 10), (8, 10), (9, 10), (9, 5), (10, 5), (10, 6)]
    for (x, y) in olifant:
        _put(a, x, y, (90, 70, 60))
    t["dierentuinbordje"] = a
    p = _vlak((150, 102, 60), 6)
    for x in range(16):
        if x % 5 == 0:
            p[:, x, :3] = (122, 82, 46)
    t["dierentuinbordje_paal"] = p
    return t


def item_texturen():
    t = {}
    a = np.zeros((16, 16, 4), float)                  # safarivoer: a little sack with seeds and a carrot top
    for y in range(4, 15):
        for x in range(3, 13):
            if (x - 7.5) ** 2 / 25 + (y - 10) ** 2 / 30 <= 1:
                a[y, x] = (196, 164, 110, 255)
    for x in range(5, 11):
        a[4, x] = (150, 120, 76, 255)
    for (x, y, c) in ((6, 3, (240, 140, 40)), (7, 2, (240, 140, 40)), (8, 3, (90, 170, 60)), (9, 2, (90, 170, 60)),
                      (6, 9, (230, 200, 90)), (9, 11, (230, 200, 90)), (7, 12, (200, 90, 60))):
        a[y, x] = (*c, 255)
    t["safarivoer"] = a
    t["safarihoed_stof"] = _vlak((204, 186, 132), 6)
    t["safarihoed_band"] = _vlak((124, 82, 48), 5)
    t["verrekijker_zwart"] = _vlak((40, 38, 46), 4)
    lens = _vlak((40, 38, 46), 3)
    lens[4:12, 4:12, :3] = (120, 170, 210)
    lens[5:7, 5:7, :3] = (220, 240, 250)
    t["verrekijker_lens"] = lens
    return t


def _el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down")):
    return {"from": frm, "to": to, "faces": {f: {"texture": "#" + tex} for f in faces}}


def blokken(h):
    A, D, w = h.A, h.D, h.w
    for naam, a in texturen().items():
        h.save(_img(a), "block", naam + ".png")
    for naam, a in item_texturen().items():
        h.save(_img(a), "item", naam + ".png")
    # rieten dak (+ trap, plaat)
    w(f"{A}/models/block/rieten_dak.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/rieten_dak"}})
    w(f"{A}/blockstates/rieten_dak.json", {"variants": {"": {"model": "guhs:block/rieten_dak"}}})
    t = {"top": "guhs:block/rieten_dak", "bottom": "guhs:block/rieten_dak", "side": "guhs:block/rieten_dak"}
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(f"{A}/models/block/rieten_dak_trap{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": t})
    from features import barbecuether
    w(f"{A}/blockstates/rieten_dak_trap.json", {"variants": barbecuether.stair_states("guhs:block/rieten_dak_trap")})
    w(f"{A}/models/block/rieten_dak_plaat.json", {"parent": "minecraft:block/slab", "textures": t})
    w(f"{A}/models/block/rieten_dak_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
    w(f"{A}/blockstates/rieten_dak_plaat.json", {"variants": {"type=bottom": {"model": "guhs:block/rieten_dak_plaat"},
                                                              "type=top": {"model": "guhs:block/rieten_dak_plaat_top"},
                                                              "type=double": {"model": "guhs:block/rieten_dak"}}})
    # bamboewand, safari-acaciaplanken
    for b in ("bamboewand", "safari_acaciaplanken"):
        w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{b}"}})
        w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"guhs:block/{b}"}}})
    # bamboe-omheining (a bamboo screen) and the dierentuinbordje (a zoo sign on a post): facing the player
    w(f"{A}/models/block/bamboe_omheining.json", {"textures": {"b": "guhs:block/bamboewand", "particle": "guhs:block/bamboewand"},
                                                  "elements": [_el([1, 0, 7], [3, 16, 9], "b"), _el([5, 0, 7], [7, 15, 9], "b"),
                                                               _el([9, 0, 7], [11, 16, 9], "b"), _el([13, 0, 7], [15, 14, 9], "b"),
                                                               _el([0, 10, 6], [16, 12, 10], "b"), _el([0, 3, 6], [16, 5, 10], "b")]})
    w(f"{A}/models/block/dierentuinbordje.json", {"textures": {"p": "guhs:block/dierentuinbordje_paal", "b": "guhs:block/dierentuinbordje",
                                                               "particle": "guhs:block/dierentuinbordje_paal"},
                                                  "elements": [_el([7, 0, 7], [9, 10, 9], "p"), _el([1, 8, 7], [15, 16, 9], "b")]})
    for b in ("bamboe_omheining", "dierentuinbordje"):
        w(f"{A}/blockstates/{b}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{b}", **({"y": y} if y else {})}
                                                     for f, y in FACING_Y.items()}})
    for b in BLOKKEN:
        w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
    # items: safarivoer (flat), safarihoed and verrekijker (3D, the hat sits on the player's head)
    h.item_model("safarivoer")
    w(f"{A}/models/item/safarihoed.json", {
        "textures": {"s": "guhs:item/safarihoed_stof", "b": "guhs:item/safarihoed_band", "particle": "guhs:item/safarihoed_stof"},
        "elements": [_el([1, 0, 1], [15, 1, 15], "s"), _el([4, 1, 4], [12, 5, 12], "s"), _el([3.8, 1, 3.8], [12.2, 2, 12.2], "b")],
        "display": {"head": {"translation": [0, 8.25, 0], "scale": [1.05, 1.05, 1.05]},
                    "gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [0.8, 0.8, 0.8]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                    "fixed": {"rotation": [90, 0, 0], "scale": [0.8, 0.8, 0.8]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 0], "scale": [0.45, 0.45, 0.45]}}})
    w(f"{A}/models/item/verrekijker.json", {
        "textures": {"z": "guhs:item/verrekijker_zwart", "l": "guhs:item/verrekijker_lens", "particle": "guhs:item/verrekijker_zwart"},
        "elements": [{"from": [4, 6, 5], "to": [7, 9, 11], "faces": {"north": {"texture": "#l"}, "south": {"texture": "#l"},
                                                                     "east": {"texture": "#z"}, "west": {"texture": "#z"},
                                                                     "up": {"texture": "#z"}, "down": {"texture": "#z"}}},
                     {"from": [9, 6, 5], "to": [12, 9, 11], "faces": {"north": {"texture": "#l"}, "south": {"texture": "#l"},
                                                                      "east": {"texture": "#z"}, "west": {"texture": "#z"},
                                                                      "up": {"texture": "#z"}, "down": {"texture": "#z"}}},
                     _el([7, 7, 7], [9, 8, 9], "z")],
        "display": {"gui": {"rotation": [30, 225, 0], "scale": [1.2, 1.2, 1.2]},
                    "ground": {"translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
                    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 0, 1], "scale": [0.7, 0.7, 0.7]},
                    "firstperson_righthand": {"rotation": [0, -20, 0], "translation": [1, 1, 0], "scale": [0.8, 0.8, 0.8]},
                    "fixed": {"scale": [1, 1, 1]}}})
    # loot
    for b in BLOKKEN:
        if b != "rieten_dak_plaat":
            h.self_drop(b)
    w(f"{D}/loot_table/blocks/rieten_dak_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:rieten_dak_plaat", "functions": [
            {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property",
                                                                             "block": "guhs:rieten_dak_plaat", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    # recipes (the giftshop sells them too)
    h.shaped("rieten_dak", ["HW", "WH"], {"H": "minecraft:hay_block", "W": "minecraft:wheat"}, "guhs:rieten_dak", 4)
    h.shaped("rieten_dak_trap", ["D  ", "DD ", "DDD"], {"D": "guhs:rieten_dak"}, "guhs:rieten_dak_trap", 4)
    h.shaped("rieten_dak_plaat", ["DDD"], {"D": "guhs:rieten_dak"}, "guhs:rieten_dak_plaat", 6)
    h.shaped("bamboewand", ["BB", "BB"], {"B": "minecraft:bamboo_planks"}, "guhs:bamboewand", 4)
    h.shaped("bamboe_omheining", ["SBS", "SBS"], {"B": "guhs:bamboewand", "S": "minecraft:bamboo"}, "guhs:bamboe_omheining", 6)
    h.shapeless("safari_acaciaplanken", ["minecraft:acacia_planks", "minecraft:acacia_planks", "minecraft:acacia_planks",
                                         "minecraft:acacia_planks", "minecraft:brown_dye"], "guhs:safari_acaciaplanken", 4)
    h.shapeless("dierentuinbordje", ["#minecraft:signs", "minecraft:yellow_dye"], "guhs:dierentuinbordje", 1)
    h.shapeless("safarivoer", ["minecraft:wheat", "minecraft:carrot", "minecraft:wheat_seeds"], "guhs:safarivoer", 4)
    # tags
    add = h.add_tag
    add("minecraft/tags/block/mineable/hoe", ["guhs:rieten_dak", "guhs:rieten_dak_trap", "guhs:rieten_dak_plaat"])
    add("minecraft/tags/block/mineable/axe", ["guhs:bamboewand", "guhs:bamboe_omheining", "guhs:safari_acaciaplanken", "guhs:dierentuinbordje"])
    add("minecraft/tags/block/stairs", ["guhs:rieten_dak_trap"])
    add("minecraft/tags/block/slabs", ["guhs:rieten_dak_plaat"])
    add("minecraft/tags/item/stairs", ["guhs:rieten_dak_trap"])
    add("minecraft/tags/item/slabs", ["guhs:rieten_dak_plaat"])
    add("minecraft/tags/item/planks", ["guhs:safari_acaciaplanken"])
    add("minecraft/tags/block/planks", ["guhs:safari_acaciaplanken"])
    add("guhs/tags/item/safarivoer", ["guhs:safarivoer"])


def geluiden(h):
    entries = {}
    for g in geluid.DIER_GROEPEN:
        entries[f"safari.dier.{g}"] = {"sounds": [{"name": f"guhs:safari/dier/{g}{v}"} for v in (1, 2, 3)],
                                       "subtitle": f"subtitles.guhs.safari.dier.{g}"}
    for z in geluid.ZONES:
        entries[f"safari.sfeer.{z}"] = {"sounds": [{"name": f"guhs:safari/sfeer/{z}", "stream": True}],
                                        "subtitle": "subtitles.guhs.safari.sfeer"}
    entries["safari.muziek.lobby"] = {"sounds": [{"name": "guhs:safari/muziek_lobby", "stream": True}], "subtitle": "subtitles.guhs.safari.muziek"}
    for e, f in (("safari.camera", "camera"), ("safari.munt", "munt"), ("safari.truck.motor", "truck_motor"),
                 ("safari.truck.toeter", "truck_toeter"), ("safari.kano", "kano")):
        entries[e] = {"sounds": [{"name": f"guhs:safari/{f}"}], "subtitle": f"subtitles.guhs.{e}"}

    def patch(d):
        d.update(entries)
    h.patch_json(f"{h.A}/sounds.json", patch)


def schilderijen_data(h):
    for naam, (bw, bh, nl, en, *_rest) in schilderijen.SCHILDERIJEN.items():
        h.w(f"{h.D}/painting_variant/safari_{naam}.json", {
            "asset_id": f"guhs:safari_{naam}", "width": bw, "height": bh,
            "title": {"translate": f"painting.guhs.safari_{naam}.title", "color": "yellow"},
            "author": {"translate": "painting.guhs.safari.author", "color": "gray"}})
    h.add_tag("minecraft/tags/painting_variant/placeable", [f"guhs:safari_{n}" for n in schilderijen.SCHILDERIJEN])


def dierpatronen():
    """The animal patterns for the later pattern brush (bouwhulp): PNGs in tools only, no blocks yet."""
    os.makedirs(PATRONEN, exist_ok=True)
    rng = np.random.default_rng(16)

    def wol(c):
        a = np.zeros((16, 16, 4), float)
        a[..., :3] = np.array(c, float) + rng.normal(0, 5, (16, 16, 1))
        a[..., 3] = 255
        for y in range(16):
            for x in range(16):
                if (x + 2 * y) % 4 == 0:
                    a[y, x, :3] = np.array(c) * 0.94
        return a

    zw, wit = (40, 36, 44), (246, 244, 240)
    uit = {}
    a = wol(wit)
    for y in range(16):
        for x in range(16):
            if ((x + 3 * np.sin(y / 2.4)) % 6) < 2.6:
                a[y, x, :3] = zw
    uit["zebra"] = a
    a = wol((246, 232, 200))
    for (cx, cy) in ((3, 3), (11, 2), (7, 9), (14, 11), (1, 12), (11, 15)):
        for y in range(16):
            for x in range(16):
                dx, dy = (x - cx + 8) % 16 - 8, (y - cy + 8) % 16 - 8
                if dx * dx + dy * dy <= 6.5 + (x * 7 + y * 3) % 3:
                    a[y, x, :3] = (204, 128, 58)
    uit["giraffe"] = a
    a = wol((238, 196, 110))
    for (cx, cy) in ((3, 3), (11, 4), (6, 11), (13, 12)):
        for y in range(16):
            for x in range(16):
                dx, dy = (x - cx + 8) % 16 - 8, (y - cy + 8) % 16 - 8
                r = (dx * dx + dy * dy) ** 0.5
                if 1.4 < r <= 2.6:
                    a[y, x, :3] = (70, 50, 40)
                elif r <= 1.4:
                    a[y, x, :3] = (204, 150, 76)
    uit["luipaard"] = a
    a = wol((240, 206, 120))
    for (cx, cy) in ((2, 2), (7, 4), (12, 1), (4, 9), (10, 8), (14, 12), (1, 14), (8, 13)):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            a[(cy + dy) % 16, (cx + dx) % 16, :3] = (50, 40, 34)
    uit["jachtluipaard"] = a
    a = wol(zw)
    for y in range(16):
        for x in range(16):
            if ((x - 7.5) / 5.5) ** 2 + ((y - 10) / 7) ** 2 <= 1:
                a[y, x, :3] = wit
    uit["pinguin"] = a
    a = wol(wit)
    for (cx, cy, rx, ry) in ((0, 0, 4, 4), (16, 0, 4, 4), (0, 16, 4, 4), (16, 16, 4, 4), (8, 8, 3.5, 2.6)):
        for y in range(16):
            for x in range(16):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1:
                    a[y, x, :3] = zw
    uit["panda"] = a
    a = wol((206, 96, 50))
    for y in range(16):
        if y % 8 in (0, 1, 2):
            a[y, :, :3] = (246, 222, 196)
        elif y % 8 == 3:
            a[y, :, :3] = (150, 64, 34)
    uit["rode_panda"] = a
    a = wol((232, 140, 56))
    for y in range(16):
        for x in range(16):
            if ((x + 2.2 * np.sin(y / 2.5 + x // 8)) % 8) < 1.8 and 2 < y % 16 < 14 - (x % 3):
                a[y, x, :3] = (40, 30, 30)
    uit["tijgerstreep"] = a
    for naam, arr in uit.items():
        _img(arr).save(os.path.join(PATRONEN, f"patroon_{naam}.png"))


# =====================================================================================================================
# the names (Dutch; English: tools/lang/en/c166_safari_spullen.json)
# =====================================================================================================================
TEKSTEN = {
    "block.guhs.rieten_dak": "Rieten dak",
    "block.guhs.rieten_dak_trap": "Rieten-daktrap",
    "block.guhs.rieten_dak_plaat": "Rieten-dakplaat",
    "block.guhs.bamboewand": "Bamboewand",
    "block.guhs.bamboe_omheining": "Bamboe-omheining",
    "block.guhs.safari_acaciaplanken": "Safari-acaciaplanken",
    "block.guhs.dierentuinbordje": "Dierentuinbordje",
    "item.guhs.safarivoer": "Safarivoer",
    "item.guhs.safarihoed": "Safarihoed",
    "item.guhs.verrekijker": "Verrekijker",
    "item.guhs.safari_hoedje": "Safarihoedje",
    "item.guhs.safari_rangerpet": "Rangerpet",
    "item.guhs.safari_vestje": "Kaki safarivestje",
    "item.guhs.safari_verrekijker": "Verrekijkertje",
    "gui.guhs.kledingbron.safari": "Safaripark Vadse Bergen (giftshop)",
    "painting.guhs.safari.author": "Safaripark Vadse Bergen",
    "subtitles.guhs.safari.sfeer": "Geluiden van het safaripark",
    "subtitles.guhs.safari.muziek": "Vrolijk safarimuziekje",
    "subtitles.guhs.safari.camera": "Klik! Een foto",
    "subtitles.guhs.safari.munt": "Safarimunt rinkelt",
    "subtitles.guhs.safari.truck.motor": "Safaritruck pruttelt",
    "subtitles.guhs.safari.truck.toeter": "Safaritruck toetert",
    "subtitles.guhs.safari.kano": "Peddel plonst",
    "subtitles.guhs.safari.dier.grote_kat": "Grote kat brult schattig",
    "subtitles.guhs.safari.dier.olifant": "Olifant toetert",
    "subtitles.guhs.safari.dier.grazer": "Grazer snuift",
    "subtitles.guhs.safari.dier.klein": "Diertje piept",
    "subtitles.guhs.safari.dier.vogel": "Vogeltje tjilpt",
    "subtitles.guhs.safari.dier.groot_vogel": "Grote vogel roept",
    "subtitles.guhs.safari.dier.pinguin": "Pinguïn kwettert",
    "subtitles.guhs.safari.dier.zee": "Zeedier blubbert",
    "subtitles.guhs.safari.dier.beer": "Beer bromt",
    "subtitles.guhs.safari.dier.aap": "Aap roept oe-oe",
    "subtitles.guhs.safari.dier.reptiel": "Reptiel sist lief",
    "subtitles.guhs.safari.dier.nijlpaard": "Nijlpaard giechelt",
    "subtitles.guhs.safari.dier.geit": "Geitje mèht",
    "subtitles.guhs.safari.dier.ezel": "Ezeltje balkt",
    "subtitles.guhs.safari.dier.cavia": "Cavia piept",
}
for _naam, (_bw, _bh, _nl, _en, *_r) in schilderijen.SCHILDERIJEN.items():
    TEKSTEN[f"painting.guhs.safari_{_naam}.title"] = f"{_nl} ({_bw}×{_bh})"


def build(h):
    blokken(h)
    geluiden(h)
    schilderijen_data(h)
    for k, v in TEKSTEN.items():
        h.lang(k, v, v)
    for c in CLOTHES:
        h.item_model(c)
    dierpatronen()
    selfcheck(h)


def selfcheck(h):
    mis = []
    for g in geluid.DIER_GROEPEN:
        for v in (1, 2, 3):
            if not os.path.exists(os.path.join(h.A, "sounds", "safari", "dier", f"{g}{v}.ogg")):
                mis.append(f"geluid dier/{g}{v}")
    for z in geluid.ZONES:
        if not os.path.exists(os.path.join(h.A, "sounds", "safari", "sfeer", f"{z}.ogg")):
            mis.append(f"geluid sfeer/{z}")
    for n in schilderijen.SCHILDERIJEN:
        if not os.path.exists(os.path.join(h.TEX, "painting", f"safari_{n}.png")):
            mis.append(f"schilderij {n} (python tools/features/safari_schilderijen.py)")
    if mis:
        print("safari_blokken: ONTBREEKT:", ", ".join(mis))
