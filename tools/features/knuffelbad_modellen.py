"""
Het Knuffelbad (2.8) - models: the entities' GeckoLib models (the zwembandje: a pink-and-white swim ring with a little guh
head; the rubber duck with the special kinds' extras; Badmeester Bubbel: the sitting guh with a yellow swim cap, a red and
white lifebuoy and a whistle on a cord) and the blocks' models and blockstates (the glijgoot in 8 layers and 5 colours,
the glow tiles with a full-bright star layer, the wash tub with water or foam, the start gates).
"""
import json
import os

import numpy as np
from PIL import Image

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
KLEUREN = ["roze", "wit", "blos", "donker", "glim"]
GLIJBANEN = ["roze_trechter", "glimtunnel", "grote_plons"]
EENDSOORTEN = ["normaal", "vadseendje", "kaaseendje", "guheendje", "badmeestereendje", "pluiseendje", "trechtereendje", "sterreneendje",
               "glimeendje", "maaneendje", "duikeendje", "plonseendje", "gouden_eendje"]


def cube(origin, size, swatch, inflate=0.0, sw=8, faces=None):
    """A GeckoLib cube whose faces each show one swatch (faces: face -> other swatch)."""
    out = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": {}}
    for f in ("north", "south", "east", "west", "up", "down"):
        s = (faces or {}).get(f, swatch)
        out["uv"][f] = {"uv": list(s), "uv_size": [sw, sw]}
    if inflate:
        out["inflate"] = inflate
    return out


def geo(identifier, tw, th, bones, bounds=(2, 1.5, 0.5)):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": identifier, "texture_width": tw, "texture_height": th, "visible_bounds_width": bounds[0],
                        "visible_bounds_height": bounds[1], "visible_bounds_offset": [0, bounds[2], 0]},
        "bones": bones}]}


# =====================================================================================================================
# the zwembandje
# =====================================================================================================================
RING_SW = {"roze": (0, 0), "wit": (8, 0), "vacht": (16, 0), "oor": (24, 0), "gezicht": (32, 0)}


def zwembandje(h):
    sw = RING_SW
    bones = [{"name": "root", "pivot": [0, 0, 0]}, {"name": "ring", "parent": "root", "pivot": [0, 0, 0]}]
    for k in range(12):
        bones.append({"name": f"ring_{k}", "parent": "ring", "pivot": [0, 0, 0], "rotation": [0, k * 30, 0],
                      "cubes": [cube([-2.75, 0.3, -11.2], [5.5, 3.6, 4.2], sw["roze"] if k % 2 == 0 else sw["wit"])]})
    bones.append({"name": "kop", "parent": "root", "pivot": [0, 4, -9], "cubes": [
        cube([-3.2, 3.6, -13.4], [6.4, 5.2, 5.2], sw["vacht"], faces={"north": sw["gezicht"]}),
        cube([-1.2, 4.2, -14.0], [2.4, 1.4, 0.8], sw["vacht"])]})
    bones.append({"name": "oor_links", "parent": "kop", "pivot": [2.3, 8.8, -10.8], "cubes": [cube([1.3, 8.6, -11.4], [2.2, 2.6, 1.0], sw["oor"])]})
    bones.append({"name": "oor_rechts", "parent": "kop", "pivot": [-2.3, 8.8, -10.8], "cubes": [cube([-3.5, 8.6, -11.4], [2.2, 2.6, 1.0], sw["oor"])]})
    bones.append({"name": "staartje", "parent": "root", "pivot": [0, 4, 10], "cubes": [cube([-1.0, 3.6, 9.4], [2.0, 2.0, 2.2], sw["vacht"])]})
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "zwembandje.geo.json"), geo("geometry.zwembandje", 64, 64, bones, (2.2, 1.2, 0.3)))
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "zwembandje.animation.json"), {"format_version": "1.8.0", "animations": {}})
    return sw


# =====================================================================================================================
# the rubber duck (13 kinds: the plain one and the twelve special ones)
# =====================================================================================================================
EEND_SW = {"lijf": (0, 0), "kop": (8, 0), "snavel": (16, 0), "oog": (24, 0), "petje": (0, 8), "oren": (8, 8), "kuifje": (16, 8),
           "slaapmuts": (24, 8), "snorkel": (0, 16), "bril": (8, 16), "kroontje": (16, 16), "fluitje": (24, 16), "buikje": (0, 24)}
S = 1.35


def _c(o, s, key, faces=None):
    return cube([v * S for v in o], [v * S for v in s], EEND_SW[key], faces=faces)


def badeendje(h):
    sw = EEND_SW
    bones = [{"name": "root", "pivot": [0, 0, 0]},
             {"name": "lijf", "parent": "root", "pivot": [0, 0, 0], "cubes": [_c([-3, 0, -3.5], [6, 4, 8], "lijf"),
                                                                                _c([-1.5, 3, 4], [3, 2.5, 1.5], "lijf")]},
             {"name": "kop", "parent": "root", "pivot": [0, 4, -2.5], "cubes": [
                 _c([-2, 3.5, -4.5], [4, 4, 4], "kop"),
                 _c([-1.5, 4.3, -6.3], [3, 1, 1.8], "snavel"),
                 _c([1.9, 5.6, -4.1], [0.4, 1, 1], "oog"),
                 _c([-2.3, 5.6, -4.1], [0.4, 1, 1], "oog")]},
             {"name": "eend_buikje", "parent": "lijf", "pivot": [0, 0, 0], "cubes": [_c([-3.6, 0, -3], [7.2, 3.4, 7], "buikje")]},
             {"name": "eend_oren", "parent": "kop", "pivot": [0, 7.5, -2.5], "cubes": [_c([-2.3, 7.4, -3.2], [1.4, 1.7, 0.6], "oren"),
                                                                                        _c([0.9, 7.4, -3.2], [1.4, 1.7, 0.6], "oren")]},
             {"name": "eend_petje", "parent": "kop", "pivot": [0, 7.5, -2.5], "cubes": [_c([-2.2, 7.4, -4.7], [4.4, 1, 4.4], "petje"),
                                                                                         _c([-2, 7.4, -6.3], [4, 0.4, 1.7], "petje")]},
             {"name": "eend_fluitje", "parent": "kop", "pivot": [0, 4, -4], "cubes": [_c([1.4, 3.6, -5.6], [1.2, 0.8, 1.8], "fluitje")]},
             {"name": "eend_kuifje", "parent": "kop", "pivot": [0, 7.5, -2.5], "cubes": [_c([-0.8, 7.4, -3.4], [1.6, 1.8, 1.6], "kuifje")]},
             {"name": "eend_slaapmuts", "parent": "kop", "pivot": [0, 7.5, -2.5], "cubes": [_c([-2.2, 7.4, -4.7], [4.4, 1.6, 4.4], "slaapmuts"),
                                                                                             _c([-1, 8.6, -1.2], [2, 2, 2], "slaapmuts", faces={"up": EEND_SW["kuifje"]})]},
             {"name": "eend_snorkel", "parent": "kop", "pivot": [0, 5, -2.5], "cubes": [_c([2.1, 5, -3.6], [0.8, 4.2, 0.8], "snorkel")]},
             {"name": "eend_bril", "parent": "kop", "pivot": [0, 6, -4.5], "cubes": [_c([-2.3, 5.5, -4.8], [4.6, 1.3, 0.6], "bril")]},
             {"name": "eend_kroontje", "parent": "kop", "pivot": [0, 7.5, -2.5], "cubes": [_c([-1.5, 7.4, -3.8], [3, 1.4, 3], "kroontje")]}]
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "badeendje.geo.json"), geo("geometry.badeendje", 32, 32, bones, (1.2, 1.0, 0.3)))
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "badeendje.animation.json"), {"format_version": "1.8.0", "animations": {}})
    return sw


# =====================================================================================================================
# Badmeester Bubbel (the sitting guh with his gear; swatches in free parts of the sitting guh's texture)
# =====================================================================================================================
UV = 128
SW = 8


def _used(g):
    used = np.zeros((UV, UV), bool)
    for bone in g["bones"]:
        for c in bone.get("cubes", []):
            uv = c.get("uv")
            if isinstance(uv, dict):
                for face in uv.values():
                    (u, v), (w, hh) = face["uv"], face["uv_size"]
                    used[int(min(v, v + hh)):int(np.ceil(max(v, v + hh))), int(min(u, u + w)):int(np.ceil(max(u, u + w)))] = True
            elif isinstance(uv, list):
                u, v = uv
                sx, sy, sz = c["size"]
                used[int(v):int(np.ceil(v + sz + sy)), int(u):int(np.ceil(u + 2 * (sx + sz)))] = True
    return used


def _swatches(g, names):
    used = _used(g)
    out = {}
    for name in names:
        for y in range(0, UV - SW + 1, SW):
            for x in range(0, UV - SW + 1, SW):
                if not used[y:y + SW, x:x + SW].any():
                    used[y:y + SW, x:x + SW] = True
                    out[name] = (x, y)
                    break
            if name in out:
                break
        if name not in out:
            raise SystemExit(f"knuffelbad_modellen: no free texture space for {name}")
    return out


def badmeester(h):
    g_file = json.load(open(os.path.join(h.A, "geckolib", "models", "entity", "guh_sitting.geo.json"), encoding="utf-8"))
    g = g_file["minecraft:geometry"][0]
    g["description"]["identifier"] = "geometry.guh_npc_badmeesterguh"
    g["bones"] = [b for b in g["bones"] if not b["name"].startswith("badmeester_")]
    sw = _swatches(g, ["muts", "bloem", "rood", "wit", "zilver", "koord"])

    def c(o, s, key, inflate=0.0):
        return cube(o, s, sw[key], inflate=inflate)
    g["bones"].append({"name": "badmeester_muts", "parent": "head", "pivot": [0, 13, 0], "cubes": [
        c([-6.9, 23.4, -6.6], [13.8, 1.8, 12.2], "muts"),
        c([-5.9, 25.2, -5.6], [11.8, 1.4, 10.2], "muts"),
        c([-3.8, 26.6, -3.8], [7.6, 0.8, 6.6], "muts"),
        c([4.6, 24.6, -3.2], [2.6, 2.6, 2.6], "bloem")]})
    # the lifebuoy round his tummy: red and white quarters
    boei = []
    for (o, s, k) in (([-7.4, 4.2, -6.6], [7.4, 2.8, 1.8], "rood"), ([0.0, 4.2, -6.6], [7.4, 2.8, 1.8], "wit"),
                      ([-7.4, 4.2, 4.6], [7.4, 2.8, 1.8], "wit"), ([0.0, 4.2, 4.6], [7.4, 2.8, 1.8], "rood"),
                      ([-8.6, 4.2, -6.6], [1.8, 2.8, 5.6], "wit"), ([-8.6, 4.2, -1.0], [1.8, 2.8, 7.4], "rood"),
                      ([6.8, 4.2, -6.6], [1.8, 2.8, 5.6], "rood"), ([6.8, 4.2, -1.0], [1.8, 2.8, 7.4], "wit")):
        boei.append(c(o, s, k))
    g["bones"].append({"name": "badmeester_boei", "parent": "body", "pivot": [0, 5.6, 0], "cubes": boei})
    g["bones"].append({"name": "badmeester_fluitje", "parent": "body", "pivot": [0, 12.4, -4.2], "cubes": [
        c([-2.3, 9.2, -4.9], [0.5, 3.4, 0.5], "koord"), c([1.8, 9.2, -4.9], [0.5, 3.4, 0.5], "koord"),
        c([-2.3, 9.0, -4.9], [4.6, 0.5, 0.5], "koord"),
        c([-1.0, 7.8, -6.0], [2.0, 1.4, 2.4], "zilver"), c([-0.4, 8.2, -6.9], [0.8, 0.8, 1.0], "zilver")]})
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "guh_npc_badmeesterguh.geo.json"), g_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.53, sat=1.0, val=1.0, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2895)

    def paint(key, col, var=8, fn=None):
        x, y = sw[key]
        blk = a[y * 4:(y + SW) * 4, x * 4:(x + SW) * 4]
        blk[..., :3] = np.clip(np.array(col, np.float32) + rng.normal(0, var / 2, (SW * 4, SW * 4, 1)), 0, 255).astype(np.uint8)
        blk[..., 3] = 255
        if fn:
            fn(blk)
    paint("muts", (255, 214, 60), 6)

    def bloem(blk):
        blk[..., :3] = (255, 150, 200)
        blk[12:20, 12:20, :3] = (255, 236, 120)
    paint("bloem", (255, 150, 200), 4, bloem)
    paint("rood", (226, 52, 64), 6)
    paint("wit", (248, 248, 250), 3)
    paint("zilver", (206, 214, 228), 6)
    paint("koord", (70, 120, 210), 5)
    h.save(Image.fromarray(a), "entity", "npc_badmeesterguh.png")


# =====================================================================================================================
# block models and blockstates
# =====================================================================================================================
def el(frm, to, tex, faces=None, cull=True, uv=None, extra=None):
    e = {"from": frm, "to": to, "faces": {}}
    for f in (faces or ("down", "up", "north", "south", "west", "east")):
        face = {"texture": tex}
        if uv and f in uv:
            face["uv"] = uv[f]
        if cull and ((f == "down" and frm[1] == 0) or (f == "up" and to[1] == 16) or (f == "north" and frm[2] == 0) or (f == "south" and to[2] == 16)
                     or (f == "west" and frm[0] == 0) or (f == "east" and to[0] == 16)):
            face["cullface"] = f
        e["faces"][f] = face
    if extra:
        e.update(extra)
    return e


GLOED = {"neoforge_data": {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}}


def blokken(h):
    A, w = h.A, h.w
    # --- simple tiles ---
    for b in ("trechtertegel", "knuffelbad_badtegel"):
        h.simple_block(b)
    # --- the glow tiles: the tile, and a full-bright layer of stars (or the bright ring's glow) over it ---
    for fel, (base, laag) in ((False, ("glimtegel", "glimtegel_sterren")), (True, ("glimtegel_fel", "glimtegel_fel_gloed"))):
        name = "glimtegel_fel" if fel else "glimtegel"
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                             "textures": {"particle": f"guhs:block/{base}", "tegel": f"guhs:block/{base}", "laag": f"guhs:block/{laag}"},
                                             "elements": [el([0, 0, 0], [16, 16, 16], "#tegel"), el([0, 0, 0], [16, 16, 16], "#laag", extra=GLOED)]})
    w(f"{A}/blockstates/glimtegel.json", {"variants": {"fel=false": {"model": "guhs:block/glimtegel"}, "fel=true": {"model": "guhs:block/glimtegel_fel"}}})
    w(f"{A}/models/item/glimtegel.json", {"parent": "guhs:block/glimtegel"})
    # --- the running surface: 8 layers x 5 colours (the star colour with a full-bright star layer on top) ---
    variants = {}
    for kleur in KLEUREN:
        top, zij = f"guhs:block/knuffelbad_glijgoot_{kleur}", f"guhs:block/knuffelbad_glijgoot_{kleur}_zijkant"
        for lagen in range(1, 9):
            hgt = lagen * 2
            uv_zij = {f: [0, 16 - hgt, 16, 16] for f in ("north", "south", "west", "east")}
            els = [el([0, 0, 0], [16, hgt, 16], "#zij", faces=("down", "north", "south", "west", "east"), uv=uv_zij),
                   el([0, 0, 0], [16, hgt, 16], "#top", faces=("up",))]
            texs = {"particle": zij, "top": top, "zij": zij}
            if kleur == "glim":
                els.append(el([0, 0, 0], [16, hgt, 16], "#ster", faces=("up",), extra=GLOED))
                texs["ster"] = "guhs:block/knuffelbad_glijgoot_glim_sterren"
            name = f"knuffelbad_glijgoot_{kleur}_{lagen}"
            w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": texs, "elements": els})
            variants[f"kleur={kleur},lagen={lagen}"] = {"model": f"guhs:block/{name}"}
    w(f"{A}/blockstates/knuffelbad_glijgoot.json", {"variants": variants})
    w(f"{A}/models/item/knuffelbad_glijgoot.json", {"parent": "guhs:block/knuffelbad_glijgoot_roze_8"})
    # --- pink foam (see-through bubbles; faces between two foam blocks aren't drawn) ---
    w(f"{A}/models/block/knuffelbad_schuim.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                                    "textures": {"all": "guhs:block/knuffelbad_schuim"}})
    w(f"{A}/blockstates/knuffelbad_schuim.json", {"variants": {"": [{"model": "guhs:block/knuffelbad_schuim", "y": r} for r in (0, 90, 180, 270)]}})
    w(f"{A}/models/item/knuffelbad_schuim.json", {"parent": "guhs:block/knuffelbad_schuim"})
    # --- the wash tub: empty, with water, with foam ---
    tobbe = [el([1, 0, 1], [15, 2, 15], "#bodem"),
             el([0, 2, 0], [16, 9, 2], "#zij", faces=("down", "up", "south", "west", "east")),
             el([0, 2, 0], [16, 9, 2], "#voor", faces=("north",), uv={"north": [0, 4, 16, 13]}),
             el([0, 2, 14], [16, 9, 16], "#zij"), el([0, 2, 2], [2, 9, 14], "#zij"), el([14, 2, 2], [16, 9, 14], "#zij"),
             # the little shower on a pole at the back
             el([7, 9, 14.5], [9, 20, 15.5], "#paal", cull=False), el([6, 19, 11], [10, 20, 15.5], "#paal", cull=False)]
    texs = {"particle": "guhs:block/guh_wastobbe_zijkant", "bodem": "guhs:block/guh_wastobbe_bodem", "zij": "guhs:block/guh_wastobbe_zijkant",
            "voor": "guhs:block/guh_wastobbe_voor", "water": "guhs:block/guh_wastobbe_water", "schuim": "guhs:block/guh_wastobbe_schuim",
            "paal": "minecraft:block/iron_block"}
    w(f"{A}/models/block/guh_wastobbe.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": texs, "elements": tobbe})
    w(f"{A}/models/block/guh_wastobbe_water.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": texs,
                                                     "elements": tobbe + [el([2, 7, 2], [14, 7.2, 14], "#water", faces=("up",), cull=False)]})
    w(f"{A}/models/block/guh_wastobbe_schuim.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": texs,
                                                      "elements": tobbe + [el([2, 5, 2], [14, 10, 14], "#schuim", cull=False),
                                                                           el([4, 10, 3], [10, 13, 9], "#schuim", cull=False),
                                                                           el([8, 10, 7], [13, 12, 12], "#schuim", cull=False)]})
    variants = {}
    for f, r in ROT.items():
        for v, m in (("leeg", "guh_wastobbe"), ("water", "guh_wastobbe_water"), ("schuim", "guh_wastobbe_schuim")):
            variants[f"facing={f},vulling={v}"] = {"model": f"guhs:block/{m}", **({"y": r} if r else {})}
    w(f"{A}/blockstates/guh_wastobbe.json", {"variants": variants})
    w(f"{A}/models/item/guh_wastobbe.json", {"parent": "guhs:block/guh_wastobbe_schuim"})
    # --- the start gates: an arch with the slide's sign on top (a guh face on the beam) ---
    variants = {}
    for gid in GLIJBANEN:
        els = [el([0, 0, 5], [3, 16, 11], "#paal", cull=False), el([13, 0, 5], [16, 16, 11], "#paal", cull=False),
               el([0, 13, 5], [16, 16, 11], "#paal", cull=False),
               el([6, 13, 4.9], [10, 16, 4.9], "#gezicht", faces=("north",), cull=False, uv={"north": [2, 4, 14, 13]}),
               el([6, 13, 11.1], [10, 16, 11.1], "#gezicht", faces=("south",), cull=False, uv={"south": [2, 4, 14, 13]}),
               el([2, 16, 7], [14, 26, 9], "#bord", cull=False),
               el([1, 16, 6.5], [2, 25, 9.5], "#paal", cull=False), el([14, 16, 6.5], [15, 25, 9.5], "#paal", cull=False)]
        name = f"glijbaan_start_{gid}"
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
            "particle": "guhs:block/glijbaan_start_paal", "paal": "guhs:block/glijbaan_start_paal", "bord": f"guhs:block/glijbaan_start_{gid}",
            "gezicht": "guhs:block/glijbaan_start_gezicht"}, "elements": els})
        for f, r in ROT.items():
            variants[f"facing={f},glijbaan={gid}"] = {"model": f"guhs:block/{name}", **({"y": r} if r else {})}
    w(f"{A}/blockstates/glijbaan_start.json", {"variants": variants})
    w(f"{A}/models/item/glijbaan_start.json", {"parent": "guhs:block/glijbaan_start_roze_trechter"})
    # --- items ---
    for i in ("eendjesmunt", "guhshampoo", "guh_fohn", "knuffelbad_badeendje"):
        h.item_model(i)


def build(h):
    """The entity models (and their swatches for the textures), Badmeester Bubbel, the block models."""
    sw_ring = zwembandje(h)
    sw_eend = badeendje(h)
    badmeester(h)
    blokken(h)
    return sw_ring, sw_eend
