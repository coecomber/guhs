"""
Het Knuffeldal (2.8.0 "Knuffeldal", phase 1): a small, soft pink valley in the Guhmensie with exactly one town in its
middle, the Grote Knusfeest, the seasons, and the shared Knus framework of 2.8 (see guhs_work28/KNUFFEL_CONTRACT.md).

  - the biome guhs:knuffeldal (its own noise, flattened towards the middle; knuffeldal_wereld.py)
  - the town knuffeldal_stadje (knuffeldal_stadje.py): the plein with the guh fountain, Burgemeester Vadsema, Opa Guh's
    kampvuurkring, the feestbuffet, the grijpmachine arcade and four building slots for the other 2.8 features; four
    corners with six guh houses (a resident each), Cocotje's house and a playground
  - the palette of every 2.8 building: knuffelsteen (+ trap, plaat, muur, gezicht), pluisdak (+ trap, plaat), knuffelklinkers
  - the biome's plants: knuffelgras, pluisgras, the pluizenboom, the guh-paddenstoel (small and huge)
  - the Knusfeest: knusfeestlijstje, feestbuffettafel, knus_oorkonde; the Kruimel-Mika (knuffeldal_npcs.py)
  - the seasons: seizoensbloembak, seizoensslinger, bladerhoopje, sneeuwpopguh (+ sneeuwguhkopje); seasonal clothes:
    bloesemkransje, zonnehoedje, knus sjaaltje; and the burgemeesterssjerp
  - the Pluisguh variant (bones pluis_*), textures (knuffeldal_tex.py), lang (knuffeldal_tekst.py), advancements (tab
    guhs:knuffeldal), FTB quests (rows y = 80 and 81.5), the game test rooms, the Knus item tags of 2.8
Run the town's self-check on its own:  python tools/features/knuffeldal_stadje.py   (from the project root)
"""
import json
import math
import os

from features import knuffeldal_npcs as npcs
from features import knuffeldal_stadje as stadje
from features import knuffeldal_tekst as tekst
from features import knuffeldal_tex as tex
from features import knuffeldal_wereld as wereld

CLOTHES = ["burgemeesterssjerp", "bloesemkransje", "zonnehoedje", "knus_sjaaltje"]
FTB_Y = (80, 81.5)
SEIZOENEN = ["lente", "zomer", "herfst", "winter"]
PARTICLES = {"pluisje": 4, "kruimel": 3, "bloesemblaadje": 3, "sneeuwvlokje": 2}
SOUNDS = {
    "knuffeldal.kruimel_giechel": [{"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 1.7, "volume": 0.8}],
    "knuffeldal.feestbel": [{"name": "minecraft:block.note_block.bell", "type": "event"}],
    "knuffeldal.seizoen": [{"name": "minecraft:block.amethyst_block.chime", "type": "event"}],
}
# the 18 item tags of 2.8 (guhs:knus/<name>) and what phase 1 puts in them
KNUS_TAGS = ["kaasmelk", "knabbelei", "pluiswol", "theekruid", "knabbelgraan", "guhbloem", "oogst", "gebak", "thee", "marshmallow",
             "lekkernij", "grijptickets", "feesttaart", "theeservies", "feestkapsels", "feestslingers", "feestbloemen", "sterrenlantaarns"]
KNUS_TAG_START = {"lekkernij": ["guhs:kaas_knabbels", "#guhs:knus/gebak"], "grijptickets": ["guhs:kermisbon"]}

# =====================================================================================================================
# guh clothes and the Pluisguh (make_guh_variants.py / make_clothes_icons.py)
# =====================================================================================================================
_H = [0, 6, -2]   # head pivot
BONES = {
    # the Pluisguh: a fluffy tuft on its head and fluffy cheeks (shown only on the Pluisguh: GuhVariant PLUISGUH, "pluis")
    "pluis_kuif": ("head", _H, "pluis_kuif", [([-2.5, 14.8, -7.5], [5, 1.8, 4], 0.1), ([-1.5, 16.5, -6.8], [3, 1.4, 2.6], 0),
                                                ([-0.7, 17.8, -6.2], [1.4, 1.0, 1.4], 0)]),
    "pluis_wangen": ("head", _H, "pluis_wang", [([6.9, 3.2, -11.6], [1.5, 4.0, 3.6], 0.05), ([-8.4, 3.2, -11.6], [1.5, 4.0, 3.6], 0.05)]),
    # the bloesemkransje: blossoms all round the top of the head, with little leaves
    "outfit_bloesemkransje": ("head", _H, "bloesem", [([round(4.6 * math.cos(k * 0.785) - 1, 2), 14.9,
                                                        round(-6 + 4.2 * math.sin(k * 0.785) - 1, 2)], [2, 1.4, 2], 0)
                                                      for k in range(8)]),
    "outfit_bloesemkransje_blad": ("head", _H, "kransblad", [([round(4.4 * math.cos(k * 0.785 + 0.39) - 0.6, 2), 14.8,
                                                             round(-6 + 4.0 * math.sin(k * 0.785 + 0.39) - 0.6, 2)],
                                                            [1.2, 0.8, 1.2], 0) for k in range(8)]),
    # the zonnehoedje: a wide straw brim, a round crown and a pink ribbon
    "outfit_zonnehoedje": ("head", _H, "zonnehoed", [([-7.5, 15.1, -12.5], [15, 0.5, 12], 0), ([-4, 15.6, -9.2], [8, 3.0, 6.4], 0)]),
    "outfit_zonnehoedje_band": ("head", _H, "zonnehoed_band", [([-4, 15.6, -9.2], [8, 1.1, 6.4], 0.12)]),
}


def clothes(rng, v):
    np = __import__("numpy")

    def sjerp():
        a = v.fabric((200, 40, 80), rng, 8)
        a[:4, :] = (246, 200, 70)
        a[-4:, :] = (246, 200, 70)
        for x in range(2, 32, 8):
            a[12:20, x:x + 4] = (255, 236, 120)          # little golden guh-heads (well, dots)
        return a

    def knoop():
        return v.fabric((246, 200, 70), rng, 10)

    def bloesem():
        a = v.fabric((255, 214, 232), rng, 6)
        for _ in range(24):
            x, y = rng.integers(1, 30, 2)
            a[y:y + 3, x:x + 3] = (255, 150, 196) if rng.random() < 0.6 else (255, 250, 250)
        return np.clip(a, 0, 255)

    def blad():
        return v.fabric((120, 196, 120), rng, 10)

    def band():
        a = v.fabric((240, 110, 170), rng, 6)
        a[:, ::6] = (255, 190, 222)
        return a

    def sjaal():
        return v.stripes((246, 150, 196), (255, 244, 248), rng, 4)

    return {"burgemeesterssjerp": {"sash": sjerp, "sash_knot": knoop},
            "bloesemkransje": {"bloesem": bloesem, "kransblad": blad},
            "zonnehoedje": {"zonnehoed": lambda: v.straw(rng), "zonnehoed_band": band},
            "knus_sjaaltje": {"scarf": sjaal}}


def variants(rng, v):
    np = __import__("numpy")

    def kuif():
        a = v.fabric((255, 226, 238), rng, 14)
        for _ in range(30):
            x, y = rng.integers(0, 30, 2)
            a[y:y + 2, x:x + 2] = (255, 250, 252)
        return np.clip(a, 0, 255)

    def wang():
        return v.fabric((255, 188, 214), rng, 12)
    return {"pluisguh": ((250, 188, 216), {"pluis_kuif": kuif, "pluis_wang": wang})}


def icons(ic):
    sjerp = ic.icon(ic.pad(["aa..............", "abba............", ".abba...........", "..abba..........", "...abba.........",
                            "....abba........", ".....abba.......", "......abbacc....", ".......abcddc...", "........cddc....",
                            ".........cc....."]),
                    {"a": (140, 20, 50), "b": (200, 40, 80), "c": (200, 150, 40), "d": (246, 200, 70)})
    krans = ic.icon(ic.pad(["....pw.gp.w.....", "..gp........pg..", ".w............w.", "p..............p", ".w............w.",
                            "..gp........pg..", "....pw.gp.w....."]),
                    {"p": (255, 150, 196), "w": (255, 250, 250), "g": (120, 196, 120)})
    return {"burgemeesterssjerp": sjerp, "bloesemkransje": krans,
            "zonnehoedje": ic.shaped("rim_hat", (170, 140, 70), (230, 200, 120), (240, 110, 170)),
            "knus_sjaaltje": ic.shaped("scarf", (200, 90, 140), (246, 150, 196), (255, 244, 248))}


# =====================================================================================================================
# blocks and items: models, blockstates, loot, recipes, tags
# =====================================================================================================================
def stair_states(m):
    out = {}
    for facing, rot in (("east", 0), ("south", 90), ("west", 180), ("north", 270)):
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = m + ("" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer")
                y = rot
                if shape in ("inner_left", "outer_left"):
                    y = (rot + 270) % 360
                if half == "top":
                    if shape in ("inner_right", "outer_right"):
                        y = (rot + 90) % 360
                    elif shape in ("inner_left", "outer_left"):
                        y = rot
                v = {"model": model}
                if half == "top":
                    v["x"] = 180
                if y:
                    v["y"] = y
                if half == "top" or y:
                    v["uvlock"] = True
                out[f"facing={facing},half={half},shape={shape}"] = v
    return out


def wall_multipart(m):
    parts = [{"when": {"up": "true"}, "apply": {"model": m + "_post"}}]
    for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        parts.append({"when": {d: "low"}, "apply": {"model": m + "_side", **({"y": r} if r else {}), "uvlock": True}})
        parts.append({"when": {d: "tall"}, "apply": {"model": m + "_side_tall", **({"y": r} if r else {}), "uvlock": True}})
    return parts


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def el(frm, to, tex, faces=None, rot=None, uv=None):
    e = {"from": frm, "to": to, "faces": {f: ({"texture": tex, "uv": uv} if uv else {"texture": tex})
                                          for f in (faces or ("down", "up", "north", "south", "west", "east"))}}
    if rot:
        e["rotation"] = rot
    return e


def facing_variants(model_for, extra=None):
    out = {}
    for f, r in ROT.items():
        for key, model in (extra or {"": model_for}).items():
            name = f"facing={f}" + (f",{key}" if key else "")
            out[name] = {"model": model, **({"y": r} if r else {})}
    return out


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # --- the biome ---
    w(f"{A}/models/block/knuffelgras.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/knuffelgras_top", "bottom": "minecraft:block/pink_wool", "side": "guhs:block/knuffelgras_side"}})
    w(f"{A}/blockstates/knuffelgras.json", {"variants": {"": [{"model": "guhs:block/knuffelgras", "y": r} for r in (0, 90, 180, 270)]}})
    w(f"{A}/models/item/knuffelgras.json", {"parent": "guhs:block/knuffelgras"})
    for p in ("pluisgras", "pluizenboom_zaailing", "guhpaddenstoel"):
        w(f"{A}/models/block/{p}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{p}"}})
        w(f"{A}/blockstates/{p}.json", {"variants": {"": {"model": f"guhs:block/{p}"}}})
        h.item_model(p, f"guhs:block/{p}")
    w(f"{A}/models/block/pluizenboom_stam.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "guhs:block/pluizenboom_stam_top", "side": "guhs:block/pluizenboom_stam"}})
    w(f"{A}/models/block/pluizenboom_stam_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {
        "end": "guhs:block/pluizenboom_stam_top", "side": "guhs:block/pluizenboom_stam"}})
    w(f"{A}/blockstates/pluizenboom_stam.json", {"variants": {
        "axis=y": {"model": "guhs:block/pluizenboom_stam"},
        "axis=z": {"model": "guhs:block/pluizenboom_stam_horizontal", "x": 90},
        "axis=x": {"model": "guhs:block/pluizenboom_stam_horizontal", "x": 90, "y": 90}}})
    w(f"{A}/models/item/pluizenboom_stam.json", {"parent": "guhs:block/pluizenboom_stam"})
    h.simple_block("pluizenboom_bladeren", render_type="minecraft:cutout_mipped")
    for name, tex_ in (("guhpaddenstoel_hoed", "guhs:block/guhpaddenstoel_hoed"), ("guhpaddenstoel_steel", "guhs:block/guhpaddenstoel_steel")):
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/template_single_face", "textures": {"texture": tex_}})
        w(f"{A}/models/item/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex_}})
        parts = []
        for d, rot in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270}), ("up", {"x": 270}), ("down", {"x": 90})):
            parts.append({"when": {d: "true"}, "apply": {"model": f"guhs:block/{name}", **rot, **({"uvlock": True} if rot else {})}})
            parts.append({"when": {d: "false"}, "apply": {"model": "minecraft:block/mushroom_block_inside", **rot, **({"uvlock": False} if rot else {})}})
        w(f"{A}/blockstates/{name}.json", {"multipart": parts})
    # --- the building palette ---
    for base in ("knuffelsteen", "pluisdak"):
        t = f"guhs:block/{base}"
        h.simple_block(base)
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            w(f"{A}/models/block/{base}_trap{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": t, "top": t, "side": t}})
        w(f"{A}/blockstates/{base}_trap.json", {"variants": stair_states(f"guhs:block/{base}_trap")})
        w(f"{A}/models/item/{base}_trap.json", {"parent": f"guhs:block/{base}_trap"})
        w(f"{A}/models/block/{base}_plaat.json", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
        w(f"{A}/models/block/{base}_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
        w(f"{A}/blockstates/{base}_plaat.json", {"variants": {"type=bottom": {"model": f"guhs:block/{base}_plaat"},
                                                             "type=top": {"model": f"guhs:block/{base}_plaat_top"},
                                                             "type=double": {"model": f"guhs:block/{base}"}}})
        w(f"{A}/models/item/{base}_plaat.json", {"parent": f"guhs:block/{base}_plaat"})
    t = "guhs:block/knuffelsteen"
    for part, parent in (("post", "template_wall_post"), ("side", "template_wall_side"), ("side_tall", "template_wall_side_tall")):
        w(f"{A}/models/block/knuffelsteen_muur_{part}.json", {"parent": f"minecraft:block/{parent}", "textures": {"wall": t}})
    w(f"{A}/models/block/knuffelsteen_muur_inventory.json", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t}})
    w(f"{A}/blockstates/knuffelsteen_muur.json", {"multipart": wall_multipart("guhs:block/knuffelsteen_muur")})
    w(f"{A}/models/item/knuffelsteen_muur.json", {"parent": "guhs:block/knuffelsteen_muur_inventory"})
    for mood in range(4):
        w(f"{A}/models/block/knuffelsteen_gezicht_{mood}.json", {"parent": "minecraft:block/orientable", "textures": {
            "top": t, "front": f"guhs:block/knuffelsteen_gezicht_{mood}", "side": t}})
    w(f"{A}/blockstates/knuffelsteen_gezicht.json", {"variants": {
        f"facing={f},stemming={m}": {"model": f"guhs:block/knuffelsteen_gezicht_{m}", **({"y": r} if r else {})}
        for f, r in ROT.items() for m in range(4)}})
    w(f"{A}/models/item/knuffelsteen_gezicht.json", {"parent": "guhs:block/knuffelsteen_gezicht_0"})
    h.simple_block("knuffelklinkers")
    # --- the feestbuffettafel: a table with a checked cloth; laid: plates, a cake, cups and a teapot ---
    cloth, rand = "#kleed", "#rand"
    tafel = [el([0, 12, 0], [16, 16, 16], "#kleed", faces=("up", "down")),
             el([0, 9, 0], [16, 16, 0.5], rand, faces=("north", "south")), el([0, 9, 15.5], [16, 16, 16], rand, faces=("north", "south")),
             el([0, 9, 0.5], [0.5, 16, 15.5], rand, faces=("west", "east")), el([15.5, 9, 0.5], [16, 16, 15.5], rand, faces=("west", "east")),
             el([1.5, 0, 1.5], [3.5, 12, 3.5], "#poot"), el([12.5, 0, 1.5], [14.5, 12, 3.5], "#poot"),
             el([1.5, 0, 12.5], [3.5, 12, 14.5], "#poot"), el([12.5, 0, 12.5], [14.5, 12, 14.5], "#poot")]
    tex_t = {"particle": "guhs:block/feestbuffettafel_top", "kleed": "guhs:block/feestbuffettafel_top",
             "rand": "guhs:block/feestbuffettafel_rand", "poot": "minecraft:block/cherry_planks",
             "hapjes": "guhs:block/feestbuffettafel_hapjes", "taart": "minecraft:block/cake_side", "taart_top": "minecraft:block/cake_top",
             "pot": "minecraft:block/white_terracotta", "knabbel": "guhs:block/block_of_kaasknabbels"}
    w(f"{A}/models/block/feestbuffettafel.json", {"parent": "minecraft:block/block", "textures": tex_t, "elements": tafel})
    gedekt = tafel[:1] + [el([0.01, 16, 0.01], [15.99, 16.01, 15.99], "#hapjes", faces=("up",))] + tafel[1:] + [
        el([2, 16, 2], [7, 19, 7], "#taart", faces=("north", "south", "west", "east")), el([2, 19, 2], [7, 19, 7], "#taart_top", faces=("up",)),
        el([10, 16, 3], [13, 20, 6], "#pot"), el([13, 18, 4], [14.5, 19, 5], "#pot"), el([9, 20, 4], [14, 20.5, 5], "#pot"),
        el([3, 16, 10], [5, 17.5, 12], "#pot"), el([6, 16, 11], [8, 17.5, 13], "#pot"),
        el([10, 16, 10], [14, 17, 14], "#knabbel")]
    w(f"{A}/models/block/feestbuffettafel_gedekt.json", {"parent": "minecraft:block/block", "textures": tex_t, "elements": gedekt})
    w(f"{A}/blockstates/feestbuffettafel.json", {"variants": {
        f"facing={f},gedekt={g}": {"model": "guhs:block/feestbuffettafel" + ("_gedekt" if g == "true" else ""), **({"y": r} if r else {})}
        for f, r in ROT.items() for g in ("false", "true")}})
    w(f"{A}/models/item/feestbuffettafel.json", {"parent": "guhs:block/feestbuffettafel_gedekt"})
    # --- the knus_oorkonde: a golden frame on a little stand ---
    w(f"{A}/models/block/knus_oorkonde.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/knus_oorkonde", "papier": "guhs:block/knus_oorkonde", "lijst": "guhs:block/knus_oorkonde_lijst"}, "elements": [
        el([3, 0, 5], [13, 1.5, 11], "#lijst"),
        el([1, 1.5, 7], [15, 15, 9], "#lijst"),
        {"from": [1.5, 2, 6.9], "to": [14.5, 14.5, 6.9], "faces": {"north": {"texture": "#papier", "uv": [0, 0, 16, 16]}}},
        {"from": [1.5, 2, 9.1], "to": [14.5, 14.5, 9.1], "faces": {"south": {"texture": "#papier", "uv": [0, 0, 16, 16]}}}]})
    w(f"{A}/blockstates/knus_oorkonde.json", {"variants": facing_variants("guhs:block/knus_oorkonde")})
    w(f"{A}/models/item/knus_oorkonde.json", {"parent": "guhs:block/knus_oorkonde"})
    # --- the seasons: the flower box, the garland, the leaf pile, the snow guh ---
    for s in SEIZOENEN:
        els = [el([0, 0, 0], [16, 8, 16], "#bak", faces=("down", "north", "south", "west", "east")),
               el([1, 7.5, 1], [15, 7.5, 15], "#aarde", faces=("up",))]
        els.append({"from": [0.8, 7.5, 8], "to": [15.2, 23.5, 8], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
                    "shade": False, "faces": {"north": {"texture": "#plant", "uv": [0, 0, 16, 16]}, "south": {"texture": "#plant", "uv": [0, 0, 16, 16]}}})
        els.append({"from": [8, 7.5, 0.8], "to": [8, 23.5, 15.2], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
                    "shade": False, "faces": {"west": {"texture": "#plant", "uv": [0, 0, 16, 16]}, "east": {"texture": "#plant", "uv": [0, 0, 16, 16]}}})
        if s == "winter":
            els.append(el([0.5, 8, 0.5], [15.5, 9, 15.5], "#sneeuw", faces=("up", "north", "south", "west", "east")))
        w(f"{A}/models/block/seizoensbloembak_{s}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
            "particle": "guhs:block/seizoensbloembak_zijkant", "bak": "guhs:block/seizoensbloembak_zijkant",
            "aarde": "guhs:block/seizoensbloembak_aarde", "plant": f"guhs:block/seizoensbloembak_{s}",
            "sneeuw": "guhs:block/seizoensbloembak_sneeuw"}, "elements": els})
        w(f"{A}/models/block/seizoensslinger_{s}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                          "textures": {"particle": f"guhs:block/seizoensslinger_{s}", "vlag": f"guhs:block/seizoensslinger_{s}"},
                                                          "elements": [{"from": [0, 4, 8], "to": [16, 16, 8], "shade": False, "faces": {
                                                              "north": {"texture": "#vlag", "uv": [0, 0, 16, 12]},
                                                              "south": {"texture": "#vlag", "uv": [0, 0, 16, 12]}}}]})
    w(f"{A}/blockstates/seizoensbloembak.json", {"variants": {f"seizoen={s}": {"model": f"guhs:block/seizoensbloembak_{s}"} for s in SEIZOENEN}})
    w(f"{A}/models/item/seizoensbloembak.json", {"parent": "guhs:block/seizoensbloembak_lente"})
    w(f"{A}/blockstates/seizoensslinger.json", {"variants": {
        f"axis={a},seizoen={s}": {"model": f"guhs:block/seizoensslinger_{s}", **({"y": 90} if a == "z" else {})}
        for a in ("x", "z") for s in SEIZOENEN}})
    h.item_model("seizoensslinger", "guhs:block/seizoensslinger_lente")
    blad = "#blad"
    w(f"{A}/models/block/bladerhoopje_vol.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/bladerhoopje", "blad": "guhs:block/bladerhoopje"}, "elements": [
        el([0.5, 0, 0.5], [15.5, 5, 15.5], blad), el([2, 5, 2], [14, 8, 14], blad), el([4, 8, 4], [12, 10, 12], blad), el([6, 10, 5], [10, 11, 9], blad)]})
    w(f"{A}/models/block/bladerhoopje_laag.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/bladerhoopje", "blad": "guhs:block/bladerhoopje"}, "elements": [
        el([0, 0, 0], [16, 1, 16], blad), el([4, 1, 3], [11, 2, 10], blad)]})
    w(f"{A}/blockstates/bladerhoopje.json", {"variants": {"vol=true": [{"model": "guhs:block/bladerhoopje_vol", "y": r} for r in (0, 90, 180, 270)],
                                                         "vol=false": [{"model": "guhs:block/bladerhoopje_laag", "y": r} for r in (0, 90, 180, 270)]}})
    w(f"{A}/models/item/bladerhoopje.json", {"parent": "guhs:block/bladerhoopje_vol"})
    sn = {"particle": "guhs:block/sneeuwpopguh", "sneeuw": "guhs:block/sneeuwpopguh", "gezicht": "guhs:block/sneeuwpopguh_gezicht",
          "sjaal": "minecraft:block/pink_wool", "oor": "minecraft:block/pink_wool"}
    w(f"{A}/models/block/sneeuwpopguh_onder.json", {"parent": "minecraft:block/block", "textures": sn, "elements": [
        el([1, 0, 1], [15, 14, 15], "#sneeuw"), el([3, 14, 3], [13, 16, 13], "#sneeuw"),
        el([2.5, 14, 2.5], [13.5, 16, 13.5], "#sjaal", faces=("north", "south", "west", "east"))]})
    w(f"{A}/models/block/sneeuwpopguh_boven.json", {"parent": "minecraft:block/block", "textures": sn, "elements": [
        el([3, 0, 3], [13, 10, 13], "#sneeuw", faces=("down", "up", "south", "west", "east")),
        {"from": [3, 0, 3], "to": [13, 10, 13], "faces": {"north": {"texture": "#gezicht", "uv": [0, 2, 16, 16]}}},
        el([1.5, 7, 6.5], [4.5, 11, 8], "#oor"), el([11.5, 7, 6.5], [14.5, 11, 8], "#oor")]})
    w(f"{A}/blockstates/sneeuwpopguh.json", {"variants": {
        f"facing={f},half={hf}": {"model": f"guhs:block/sneeuwpopguh_{'onder' if hf == 'lower' else 'boven'}", **({"y": r} if r else {})}
        for f, r in ROT.items() for hf in ("lower", "upper")}})
    h.item_model("sneeuwpopguh", "guhs:item/sneeuwguhkopje")
    # --- items ---
    h.item_model("knusfeestlijstje")
    h.item_model("sneeuwguhkopje")
    w(f"{A}/models/item/kruimel_mika_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})

    # --- loot ---
    for blk in ("knuffelgras", "pluizenboom_stam", "pluizenboom_zaailing", "guhpaddenstoel", "knuffelsteen", "knuffelsteen_trap", "knuffelsteen_muur",
                "knuffelsteen_gezicht", "pluisdak", "pluisdak_trap", "knuffelklinkers", "feestbuffettafel", "knus_oorkonde", "seizoensbloembak",
                "seizoensslinger", "bladerhoopje"):
        h.self_drop(blk)
    for slab in ("knuffelsteen_plaat", "pluisdak_plaat"):
        w(f"{D}/loot_table/blocks/{slab}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
            "type": "minecraft:item", "name": f"guhs:{slab}", "functions": [
                {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property", "block": f"guhs:{slab}",
                                                                                 "properties": {"type": "double"}}]},
                {"function": "minecraft:explosion_decay"}]}]}]})
    shears = {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}
    w(f"{D}/loot_table/blocks/pluisgras.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:pluisgras", "conditions": [shears]}]}]})
    leaves = wereld.vanilla_json("data/minecraft/loot_table/blocks/cherry_leaves.json")
    txt = json.dumps(leaves).replace("minecraft:cherry_leaves", "guhs:pluizenboom_bladeren").replace("minecraft:cherry_sapling", "guhs:pluizenboom_zaailing")
    w(f"{D}/loot_table/blocks/pluizenboom_bladeren.json", json.loads(txt))
    w(f"{D}/loot_table/blocks/guhpaddenstoel_hoed.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:guhpaddenstoel", "functions": h.count_fn(0, 2)}]}]})
    w(f"{D}/loot_table/blocks/guhpaddenstoel_steel.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:guhpaddenstoel_steel", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
            "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}]}]})
    w(f"{D}/loot_table/blocks/sneeuwpopguh.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:sneeuwguhkopje"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:snowball", "functions": h.count_fn(4, 6)}]}]})

    # --- recipes ---
    h.shaped("knuffelsteen", ["SSS", "SPS", "SSS"], {"S": "minecraft:smooth_sandstone", "P": "minecraft:pink_dye"}, "guhs:knuffelsteen", 8)
    h.shaped("knuffelsteen_trap", ["S  ", "SS ", "SSS"], {"S": "guhs:knuffelsteen"}, "guhs:knuffelsteen_trap", 4)
    h.shaped("knuffelsteen_plaat", ["SSS"], {"S": "guhs:knuffelsteen"}, "guhs:knuffelsteen_plaat", 6)
    h.shaped("knuffelsteen_muur", ["SSS", "SSS"], {"S": "guhs:knuffelsteen"}, "guhs:knuffelsteen_muur", 6)
    h.shapeless("knuffelsteen_gezicht", ["guhs:knuffelsteen", "guhs:kaas_knabbels"], "guhs:knuffelsteen_gezicht", 1)
    h.shaped("pluisdak", ["WW", "WW"], {"W": "minecraft:pink_wool"}, "guhs:pluisdak", 4)
    h.shaped("pluisdak_trap", ["S  ", "SS ", "SSS"], {"S": "guhs:pluisdak"}, "guhs:pluisdak_trap", 4)
    h.shaped("pluisdak_plaat", ["SSS"], {"S": "guhs:pluisdak"}, "guhs:pluisdak_plaat", 6)
    h.shaped("knuffelklinkers", ["PW", "WP"], {"P": "minecraft:pink_terracotta", "W": "minecraft:white_terracotta"}, "guhs:knuffelklinkers", 4)
    h.shaped("seizoensbloembak", ["S S", "SDS"], {"S": "guhs:knuffelsteen", "D": "minecraft:dirt"}, "guhs:seizoensbloembak", 1)
    h.shaped("seizoensslinger", ["SSS", "WWW"], {"S": "minecraft:string", "W": "#minecraft:wool"}, "guhs:seizoensslinger", 3)
    h.shaped("feestbuffettafel", ["CCC", "PPP", "P P"], {"C": "minecraft:pink_carpet", "P": "#minecraft:planks"}, "guhs:feestbuffettafel", 2)
    h.shapeless("sneeuwguhkopje", ["minecraft:snowball", "minecraft:snowball", "minecraft:snowball", "guhs:kaas_knabbels"], "guhs:sneeuwguhkopje", 1)
    h.shaped("bladerhoopje", ["LL", "LL"], {"L": "#minecraft:leaves"}, "guhs:bladerhoopje", 1)
    h.shapeless("pluizenboom_planken", ["guhs:pluizenboom_stam"], "minecraft:cherry_planks", 4)

    # --- tags ---
    add = h.add_tag
    add("minecraft/tags/block/dirt", ["guhs:knuffelgras"])
    add("minecraft/tags/block/mineable/pickaxe", [f"guhs:{b}" for b in ("knuffelsteen", "knuffelsteen_trap", "knuffelsteen_plaat",
                                                                       "knuffelsteen_muur", "knuffelsteen_gezicht", "knuffelklinkers", "seizoensbloembak")])
    add("minecraft/tags/block/mineable/axe", ["guhs:pluizenboom_stam", "guhs:feestbuffettafel", "guhs:knus_oorkonde", "guhs:guhpaddenstoel_hoed",
                                              "guhs:guhpaddenstoel_steel"])
    add("minecraft/tags/block/mineable/hoe", ["guhs:pluizenboom_bladeren", "guhs:bladerhoopje"])
    add("minecraft/tags/block/mineable/shovel", ["guhs:knuffelgras", "guhs:sneeuwpopguh"])
    add("minecraft/tags/block/replaceable", ["guhs:pluisgras"])
    add("minecraft/tags/block/sword_efficient", ["guhs:pluisgras", "guhs:guhpaddenstoel"])
    add("minecraft/tags/block/walls", ["guhs:knuffelsteen_muur"])
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/leaves", ["guhs:pluizenboom_bladeren"])
        add(f"minecraft/tags/{kind}/saplings", ["guhs:pluizenboom_zaailing"])
        add(f"minecraft/tags/{kind}/logs_that_burn", ["guhs:pluizenboom_stam"])
        add(f"minecraft/tags/{kind}/stairs", ["guhs:knuffelsteen_trap", "guhs:pluisdak_trap"])
        add(f"minecraft/tags/{kind}/slabs", ["guhs:knuffelsteen_plaat", "guhs:pluisdak_plaat"])
    add("minecraft/tags/item/walls", ["guhs:knuffelsteen_muur"])
    # the Knus tags of 2.8: created here, filled by their owners (see the contract, par. 7)
    for name in KNUS_TAGS:
        add(f"guhs/tags/item/knus/{name}", KNUS_TAG_START.get(name, []))
    add("guhs/tags/block/knus/knuffels", [])


# =====================================================================================================================
# sounds, advancements, lang
# =====================================================================================================================
def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


QUEST_ADVANCEMENTS = ["knuffeldal_stadje_bezocht", "knuffeldal_burgemeester", "knuffeldal_taakje_gebracht", "knuffeldal_kruimel_mika",
                      "knuffeldal_finale", "knuffeldal_seizoensfeest", "knuffeldal_cocotje", "knuffeldal_alle_vriendjes", "knuffeldal_seizoen_alle",
                      "seen_pluisguh", "seen_burgemeesterguh", "seen_kruimel_mika", "seen_cocotje"] + \
                     [f"knusfeest_{t}_gemaakt" for t in tekst.TAKEN]


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    pluis_tame = {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                                "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"pluisguh\"}"}}]}}
    h.w(f"{D}/advancement/quest/tamed_pluisguh.json", {"criteria": {"done": pluis_tame}})
    impossible = {"done": {"trigger": "minecraft:impossible"}}
    tab = [
        ("root", None, "guhs:knuffelsteen_gezicht", "task", {"done": {"trigger": "minecraft:changed_dimension", "conditions": {"to": "guhs:guhmension"}}}),
        ("bezocht", "root", "guhs:knuffelgras", "task",
         {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": "guhs:knuffeldal"}}}}}),
        ("stadje", "bezocht", "guhs:seizoensbloembak", "goal",
         {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:knuffeldal_stadje"}}}}}),
        ("burgemeester", "stadje", "guhs:knusfeestlijstje", "task", impossible),
        ("kruimel_mika_verjaagd", "burgemeester", "guhs:kaas_knabbels", "goal", impossible),
        ("knusfeest_klaar", "burgemeester", "guhs:feestbuffettafel", "goal", impossible),
        ("knuffelburgemeester", "knusfeest_klaar", "guhs:knus_oorkonde", "challenge", impossible),
        ("pluisguh_getemd", "bezocht", "guhs:pluisgras", "goal", {"done": pluis_tame}),
        ("seizoen_alle", "root", "guhs:bladerhoopje", "challenge", impossible),
        ("cocotje", "stadje", "guhs:kaas_knabbels", "task", impossible),
    ]
    for name, parent, icon, frame, crit in tab:
        title, desc = tekst.ADVANCEMENTS[name]
        adv = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                           "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                           "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
               "criteria": crit}
        if parent:
            adv["parent"] = f"guhs:knuffeldal/{parent}"
        else:
            adv["display"]["background"] = "minecraft:block/pink_concrete_powder"
            adv["display"]["announce_to_chat"] = False
        h.w(f"{D}/advancement/knuffeldal/{name}.json", adv)
        h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
        h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


def texts(h):
    for key, text in tekst.LANG.items():
        h.lang(key, text, text)


def test_templates(h):
    """Rooms for the game tests: a little square with a feestbuffettafel, a flower box and room for NPCs and guhs."""
    mc = h.mc
    s = h.Structure((15, 6, 15))
    for x in range(15):
        for z in range(15):
            s.set(x, 0, z, "guhs:knuffelklinkers" if (x + z) % 3 else "guhs:knuffelgras")
    s.set(7, 1, 3, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "false"})
    s.set(8, 1, 3, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "false"})
    s.set(2, 1, 12, "guhs:seizoensbloembak", {"seizoen": "lente"})
    s.set(12, 1, 12, "guhs:bladerhoopje", {"vol": "false"})
    s.save("knuffeldal_test_plein")


def selfcheck_assets(h):
    """check_assets.py only reads the registry classes: this checks our own blocks and items."""
    A = h.A
    blocks = ["knuffelgras", "pluisgras", "pluizenboom_stam", "pluizenboom_bladeren", "pluizenboom_zaailing", "guhpaddenstoel",
              "guhpaddenstoel_hoed", "guhpaddenstoel_steel", "knuffelsteen", "knuffelsteen_trap", "knuffelsteen_plaat", "knuffelsteen_muur",
              "knuffelsteen_gezicht", "pluisdak", "pluisdak_trap", "pluisdak_plaat", "knuffelklinkers", "feestbuffettafel", "knus_oorkonde",
              "seizoensbloembak", "seizoensslinger", "bladerhoopje", "sneeuwpopguh"]
    missing = []
    for b in blocks:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ("knusfeestlijstje", "sneeuwguhkopje", "kruimel_mika_spawn_egg"):
        if not os.path.exists(f"{A}/models/item/{i}.json") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for f in ("kruimel_mika.geo.json", "guh_npc_burgemeesterguh.geo.json", "guh_npc_cocotje.geo.json"):
        if not os.path.exists(f"{A}/geo/entity/{f}"):
            missing.append(f"geo {f}")
    for t in ("kruimel_mika", "npc_burgemeesterguh", "npc_cocotje"):
        if not os.path.exists(f"{A}/textures/entity/{t}.png"):
            missing.append(f"texture {t}")
    for root, _dirs, files in os.walk(f"{A}/models/block"):
        for f in files:
            if f.startswith(tuple(blocks)):
                model = json.load(open(os.path.join(root, f), encoding="utf-8"))
                for t in model.get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    if missing:
        raise SystemExit(f"knuffeldal assets missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    wereld.worldgen(h)
    npcs.build(h)
    summary = stadje.build_all(h)
    overlap, reach = stadje.check_stukken()
    if overlap:
        raise SystemExit(f"knuffeldal_stadje pieces overlap: {overlap}")
    # keep_clear: the town's reach from its anchor, plus a cell (the anchor is on the dal's peak, anywhere in its cell)
    wereld.structure_json(h, reach + 1 + wereld.CELL_CHUNKS * 16)
    sounds(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck_assets(h)
    print(f"knuffeldal: town {summary}")


# =====================================================================================================================
# FTB quests (rows y = 80 and 81.5), no dependencies
# =====================================================================================================================
def ftb(fq):
    q, y1, y2 = fq.q, FTB_Y[0], FTB_Y[1]
    q("knuffeldal_dal", "Het Knuffeldal",
      "Ergens in de Guhmensie ligt een klein, zacht roze dal vol &dknuffelgras&r, pluizenbomen en guhpaddenstoelen: het &dKnuffeldal&r. "
      "Het superkompas (categorie Knus) wijst de weg naar het stadje in het midden.",
      "guhs:knuffelgras", [fq.biome("knuffeldal")], rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y1, shape="circle", xp=100)
    q("knuffeldal_stadje", "Het stadje", "In het midden van elk Knuffeldal ligt een knus stadje: een plein met een grote guhfontein, "
      "guhhuisjes met bewoners, Opa Guh bij het kampvuur en het feestbuffet. VAHOEG!",
      "guhs:seizoensbloembak", [fq.structure("knuffeldal_stadje")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-6.5, y=y1, shape="gear", xp=200)
    q("knuffeldal_burgemeester", "Burgemeester Vadsema", "Praat met &dBurgemeester Vadsema&r op zijn bordes. Het Grote Knusfeest komt eraan... "
      "maar er is niks klaar, njeg! Hij geeft je een lijstje met zes feesttaakjes.",
      "guhs:knusfeestlijstje", [fq.adv("knuffeldal_burgemeester")], rewards=(("guhs:kaas_knabbels", 16),), x=-5, y=y1, xp=100)
    x = -3.5
    for tid, naam in tekst.TAKEN.items():
        q(f"knuffeldal_{tid}", naam, f"Feesttaakje: {naam.lower()}. {tekst.TAAK_WAAR[tid]} Breng het daarna naar de burgemeester.",
          "guhs:knusfeestlijstje", [fq.adv(f"knusfeest_{tid}_gemaakt")], rewards=(("guhs:kaas_knabbels", 12),), x=x, y=y1, xp=100)
        x += 1.5
    q("knuffeldal_kruimel", "Kruimeldief!", "Onderweg naar de burgemeester pikt een &6Kruimel-Mika&r de feesttaart, het theeservies of de slingers! "
      "Volg het kruimelspoor en lok hem weg met een lekkernij (kaasknabbels of gebak). Hij vecht nooit: hij giechelt alleen.",
      "guhs:kruimel_mika_spawn_egg", [fq.adv("knuffeldal_kruimel_mika")], rewards=(("guhs:guh_ballon", 2),), x=-8, y=y2, xp=150)
    q("knuffeldal_finale", "Het Grote Knusfeest", "Breng alle zes feesttaakjes naar Burgemeester Vadsema. Dan begint het Grote Knusfeest aan het "
      "feestbuffet, en al je tamme guhs komen smullen! Je wordt &dKnuffelburgemeester&r en krijgt de Knus-oorkonde en de burgemeesterssjerp.",
      "guhs:knus_oorkonde", [fq.adv("knuffeldal_finale")], rewards=(("guhs:vahoege_vads_ingot", 3),), x=-6.5, y=y2, shape="gear", xp=500)
    q("knuffeldal_seizoensfeest", "Elk seizoen een feest", "Na het Grote Knusfeest organiseert de burgemeester elk seizoen een seizoensfeest "
      "met een paar nieuwe feesttaakjes. Vier er een!", "guhs:feestbuffettafel", [fq.adv("knuffeldal_seizoensfeest")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=-5, y=y2, xp=200)
    q("knuffeldal_pluisguh", "Superpluizig", "In het Knuffeldal wonen &dPluisguhs&r: extra pluizige roze guhs met een kuifje. Tem er een met kaasknabbels!",
      "guhs:pluisgras", [fq.adv("tamed_pluisguh")], rewards=(("guhs:pluizenboom_zaailing", 3),), x=-3.5, y=y2, xp=150)
    q("knuffeldal_seizoenen", "Het hele jaar knus", "Elk seizoen kun je iets anders in het Knuffeldal: bloesemkransjes vlechten (lente), "
      "zonnehoedjes (zomer), in bladerhoopjes springen (herfst) en sneeuwpopguhs bouwen (winter). Vul het seizoensplakboek in je Guhdex!",
      "guhs:bladerhoopje", [fq.adv("knuffeldal_seizoen_alle")], rewards=(("guhs:seizoensbloembak", 4),), x=-2, y=y2, shape="gear", xp=300)
    q("knuffeldal_cocotje", "Cocotje", "In een klein huisje met lange hangende oren woont &dCocotje&r. Ze is iets kwijt... Weet jij waar ze zijn?",
      "guhs:kaas_knabbels", [fq.adv("knuffeldal_cocotje")], rewards=(("guhs:kaas_knabbels", 8),), x=-0.5, y=y2, xp=100)
    q("knuffeldal_vriendjes", "Knuffelvriendjes", "Praat met alle zes de bewoners van de guhhuisjes in het stadje (rechtsklik met een lege hand). "
      "Ze staan in je Guhdex onder Knus!", "guhs:guh_spawn_egg", [fq.adv("knuffeldal_alle_vriendjes")], rewards=(("guhs:kaas_knabbels", 16),),
      x=1, y=y2, xp=150)
    q("knuffeldal_guhdex", "Nieuwe gezichten", "Zet Burgemeester Vadsema, Cocotje, een Kruimel-Mika en een Pluisguh in je Guhdex.",
      "guhs:guhdex", [fq.adv("seen_burgemeesterguh"), fq.adv("seen_cocotje"), fq.adv("seen_kruimel_mika"), fq.adv("seen_pluisguh")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=2.5, y=y2, shape="rsquare", xp=150)
