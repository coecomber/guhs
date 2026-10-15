"""
bbq2 (camping-markt): the models of features/camping_markt.py.

  blocks (JSON models, the 1.21.1 shapes; tools/mc26.py converts)
    tent canvas       a block, stairs and a slab per colour (the vanilla shapes)
    kampeerplek       a little post with a painted board ("VRIJ" / "BEZET"), turned with the pitch
    haring            a tent peg: sticking out and askew, or hammered in to its head
    hakblok           a stump; with a spruce log standing on it, or the two split halves and the axe in the wood
    kampvuur          a big pile of crossed logs on a bed of ash with four stones; burning: glowing logs and two tall
                      crossed flames (the vanilla camp fire's own animated textures)
    weegschaal        a brass balance: a foot, a post, a beam in three pieces and two pans on chains; three models (level,
                      down on the left, down on the right)
    vadsstapel        a tray with six stacked bars of vads and a brass number plate; five models
  characters (the sitting guh / the Mika's model with extra bones, textures painted in free cells of their sheets)
    kampbaasguh       khaki green, a wide ranger hat with a pink band, a scout's neckerchief, a whistle on a cord
    houthakkerguh     russet, a red woolly hat with a pompom, a checked collar, an axe over his shoulder
    marktmeester_mika a Nether-Mika with a tall purple hat (golden band), a big moustache, a monocle and a chain of office
    campingmarkt_kraam_mika   the stall holders: a Nether-Mika in a flat cap and a striped apron
"""
import os

import numpy as np
from PIL import Image

from features import barbecuether
from features import knuffeldal_npcs as kn
from features import sterrenwacht_hulp as hulp
from features import camping_markt_tex as tex

KLEUREN = ("rood", "geel", "groen", "blauw", "creme")


def _doos(x0, y0, z0, x1, y1, z1, t, anders=None, rot=None):
    """A model element with explicit uv on every face (the texture laid flat over the box). anders = {face: (texture, uv)}."""
    def uv(a0, a1, b0, b1):
        w, hgt = min(16.0, a1 - a0), min(16.0, b1 - b0)
        u0 = max(0.0, min(16.0 - w, a0 % 16))
        v0 = max(0.0, min(16.0 - hgt, (16 - b1) % 16))
        return [round(u0, 3), round(v0, 3), round(u0 + w, 3), round(v0 + hgt, 3)]
    faces = {"north": {"uv": uv(x0, x1, y0, y1), "texture": t}, "south": {"uv": uv(x0, x1, y0, y1), "texture": t},
             "west": {"uv": uv(z0, z1, y0, y1), "texture": t}, "east": {"uv": uv(z0, z1, y0, y1), "texture": t},
             "up": {"uv": uv(x0, x1, z0, z1), "texture": t}, "down": {"uv": uv(x0, x1, z0, z1), "texture": t}}
    for face, (tt, u) in (anders or {}).items():
        faces[face] = {"uv": u, "texture": tt}
    el = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
    if rot:
        el["rotation"] = rot
    return el


def _model(h, naam, textures, elements, render_type=None):
    model = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if render_type:
        model["render_type"] = render_type
    h.w(f"{h.A}/models/block/{naam}.json", model)


# =====================================================================================================================
# tent canvas
# =====================================================================================================================
def tentdoek(h):
    A = h.A
    for i, kleur in enumerate(KLEUREN):
        n = f"campingmarkt_tentdoek_{kleur}"
        t = f"guhs:block/{n}"
        h.save(tex.tentdoek(kleur, 31320 + i), "block", f"{n}.png")
        h.simple_block(n)
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            h.w(f"{A}/models/block/{n}_trap{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": t, "top": t, "side": t}})
        h.w(f"{A}/blockstates/{n}_trap.json", {"variants": barbecuether.stair_states(f"guhs:block/{n}_trap")})
        h.w(f"{A}/models/item/{n}_trap.json", {"parent": f"guhs:block/{n}_trap"})
        h.w(f"{A}/models/block/{n}_plaat.json", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
        h.w(f"{A}/models/block/{n}_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
        h.w(f"{A}/blockstates/{n}_plaat.json", {"variants": {"type=bottom": {"model": f"guhs:block/{n}_plaat"},
                                                             "type=top": {"model": f"guhs:block/{n}_plaat_top"},
                                                             "type=double": {"model": f"guhs:block/{n}"}}})
        h.w(f"{A}/models/item/{n}_plaat.json", {"parent": f"guhs:block/{n}_plaat"})


# =====================================================================================================================
# the camping's blocks
# =====================================================================================================================
def kampeerplek(h):
    h.save(tex.plank(31331), "block", "campingmarkt_plank.png")
    h.save(tex.plank(31332, "VRIJ", (70, 140, 64)), "block", "campingmarkt_bord_vrij.png")
    h.save(tex.plank(31333, "BEZET", (196, 56, 50)), "block", "campingmarkt_bord_bezet.png")
    for staat in ("vrij", "bezet"):
        naam = "campingmarkt_kampeerplek" + ("" if staat == "vrij" else "_bezet")
        bord = f"guhs:block/campingmarkt_bord_{staat}"
        # (the board looks north in the model; the blockstate turns it)
        _model(h, naam, {"particle": "guhs:block/campingmarkt_plank", "hout": "guhs:block/campingmarkt_plank", "bord": bord}, [
            _doos(7, 0, 7, 9, 9, 9, "#hout"),
            _doos(1, 8, 6, 15, 15, 7, "#hout", {"north": ("#bord", [1, 4, 15, 11]), "south": ("#bord", [1, 4, 15, 11])})])
    states = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for bezet in ("false", "true"):
            v = {"model": "guhs:block/campingmarkt_kampeerplek" + ("_bezet" if bezet == "true" else "")}
            if y:
                v["y"] = y
            states[f"bezet={bezet},facing={facing}"] = v
    h.w(f"{h.A}/blockstates/campingmarkt_kampeerplek.json", {"variants": states})
    h.w(f"{h.A}/models/item/campingmarkt_kampeerplek.json", {"parent": "guhs:block/campingmarkt_kampeerplek"})


def haring(h):
    h.save(tex.ijzer(31334), "block", "campingmarkt_haring.png")
    t = {"particle": "guhs:block/campingmarkt_haring", "ijzer": "guhs:block/campingmarkt_haring", "touw": "guhs:block/campingmarkt_tentdoek_creme"}
    scheef = {"origin": [8, 0, 8], "axis": "z", "angle": 22.5}
    _model(h, "campingmarkt_haring", t, [
        _doos(7.25, 0, 7.25, 8.75, 8, 8.75, "#ijzer", rot=scheef), _doos(6.25, 8, 6.75, 9.75, 9, 9.25, "#ijzer", rot=scheef),
        _doos(7.6, 5.5, 7.6, 8.4, 6.3, 12, "#touw")])
    _model(h, "campingmarkt_haring_vast", t, [
        _doos(7.25, 0, 7.25, 8.75, 3, 8.75, "#ijzer"), _doos(6.25, 3, 6.75, 9.75, 4, 9.25, "#ijzer"),
        _doos(7.6, 1.6, 7.6, 8.4, 2.4, 12, "#touw")])
    h.w(f"{h.A}/blockstates/campingmarkt_haring.json", {"variants": {"vast=false": {"model": "guhs:block/campingmarkt_haring"},
                                                                     "vast=true": {"model": "guhs:block/campingmarkt_haring_vast"}}})
    h.w(f"{h.A}/models/item/campingmarkt_haring.json", {"parent": "guhs:block/campingmarkt_haring", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [1.2, 1.2, 1.2]}}})


def hakblok(h):
    h.save(tex.stronk_zij(31335), "block", "campingmarkt_hakblok_zij.png")
    h.save(tex.stronk_boven(31336), "block", "campingmarkt_hakblok_boven.png")
    t = {"particle": "guhs:block/campingmarkt_hakblok_zij", "zij": "guhs:block/campingmarkt_hakblok_zij", "boven": "guhs:block/campingmarkt_hakblok_boven",
         "stam": "minecraft:block/spruce_log", "kop": "minecraft:block/spruce_log_top", "binnen": "minecraft:block/stripped_spruce_log",
         "ijzer": "guhs:block/campingmarkt_haring", "steel": "guhs:block/campingmarkt_plank"}
    stronk = _doos(1, 0, 1, 15, 9, 15, "#zij", {"up": ("#boven", [0, 0, 16, 16]), "down": ("#boven", [0, 0, 16, 16])})
    _model(h, "campingmarkt_hakblok", t, [
        stronk, _doos(5, 9, 5, 11, 16, 11, "#stam", {"up": ("#kop", [5, 5, 11, 11]), "down": ("#kop", [5, 5, 11, 11])})])
    # split: the two halves fallen apart, the axe still in the block
    _model(h, "campingmarkt_hakblok_leeg", t, [
        stronk,
        _doos(2, 9, 4, 5, 12, 12, "#stam", {"east": ("#binnen", [4, 0, 12, 3]), "north": ("#kop", [5, 5, 8, 8]), "south": ("#kop", [5, 5, 8, 8])}),
        _doos(11, 9, 4, 14, 12, 12, "#stam", {"west": ("#binnen", [4, 0, 12, 3]), "north": ("#kop", [8, 5, 11, 8]), "south": ("#kop", [8, 5, 11, 8])}),
        _doos(7.5, 9, 6, 8.5, 14, 10, "#ijzer"),
        _doos(7.5, 13, 9, 8.5, 14, 16, "#steel", rot={"origin": [8, 13.5, 9], "axis": "x", "angle": 22.5})])
    h.w(f"{h.A}/blockstates/campingmarkt_hakblok.json", {"variants": {"stam=true": {"model": "guhs:block/campingmarkt_hakblok"},
                                                                      "stam=false": {"model": "guhs:block/campingmarkt_hakblok_leeg"}}})
    h.w(f"{h.A}/models/item/campingmarkt_hakblok.json", {"parent": "guhs:block/campingmarkt_hakblok"})


def kampvuur(h):
    h.save(tex.asbed(31337), "block", "campingmarkt_kampvuur_as.png")
    for brandt in (False, True):
        naam = "campingmarkt_kampvuur" + ("_aan" if brandt else "")
        t = {"particle": "minecraft:block/spruce_log", "as": "guhs:block/campingmarkt_kampvuur_as", "steen": "guhs:block/houtskoolsteen_stenen",
             "stam": "minecraft:block/campfire_log_lit" if brandt else "minecraft:block/spruce_log", "kop": "minecraft:block/spruce_log_top",
             "vuur": "minecraft:block/campfire_fire"}
        el = [_doos(1, 0, 1, 15, 1, 15, "#as")]
        for (x, z) in ((0, 0), (13, 0), (0, 13), (13, 13)):                       # four stones on the corners
            el.append(_doos(x, 0, z, x + 3, 3, z + 3, "#steen"))
        # two logs along x, two across them, one on top
        log = [0, 0, 16, 4]
        for z in (3, 9):
            el.append(_doos(1, 1, z, 15, 5, z + 4, "#stam", {"north": ("#stam", log), "south": ("#stam", log), "up": ("#stam", log), "down": ("#stam", log),
                                                             "west": ("#kop", [6, 6, 10, 10]), "east": ("#kop", [6, 6, 10, 10])}))
        for x in (3, 9):
            el.append(_doos(x, 5, 1, x + 4, 9, 15, "#stam", {"west": ("#stam", log), "east": ("#stam", log),
                                                             "up": ("#stam", log),
                                                             "down": ("#stam", log), "north": ("#kop", [6, 6, 10, 10]), "south": ("#kop", [6, 6, 10, 10])}))
        el.append(_doos(2, 9, 6, 14, 12, 10, "#stam", {"north": ("#stam", log), "south": ("#stam", log), "up": ("#stam", log), "down": ("#stam", log),
                                                       "west": ("#kop", [6, 6, 10, 10]), "east": ("#kop", [6, 6, 10, 10])}))
        if brandt:
            for hoek in (45, -45):                                                # two tall crossed flames
                el.append({"from": [-4, 4, 8], "to": [20, 28, 8], "shade": False,
                           "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": hoek, "rescale": True},
                           "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#vuur"}, "south": {"uv": [0, 0, 16, 16], "texture": "#vuur"}}})
        _model(h, naam, t, el, render_type="minecraft:cutout")
    h.w(f"{h.A}/blockstates/campingmarkt_kampvuur.json", {"variants": {"brandt=false": {"model": "guhs:block/campingmarkt_kampvuur"},
                                                                       "brandt=true": {"model": "guhs:block/campingmarkt_kampvuur_aan"}}})
    h.w(f"{h.A}/models/item/campingmarkt_kampvuur.json", {"parent": "guhs:block/campingmarkt_kampvuur_aan", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [0.5, 0.5, 0.5]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.32, 0.32, 0.32]}}})


# =====================================================================================================================
# the market's blocks
# =====================================================================================================================
def weegschaal(h):
    h.save(tex.messing(31338), "block", "campingmarkt_messing.png")
    t = {"particle": "guhs:block/campingmarkt_messing", "messing": "guhs:block/campingmarkt_messing", "voet": "guhs:block/gepolijst_roosterijzer",
         "ketting": "guhs:block/roosterijzer"}

    def schaal(x0, y):
        """A pan (5 x 5) at height y with its two chains up to the beam's end at y + 8."""
        return [_doos(x0, y, 5.5, x0 + 5, y + 1, 10.5, "#messing"), _doos(x0 + 0.5, y + 1, 5.5, x0 + 4.5, y + 1.5, 6, "#messing"),
                _doos(x0 + 0.5, y + 1, 10, x0 + 4.5, y + 1.5, 10.5, "#messing"),
                _doos(x0 + 2.25, y + 1, 6.5, x0 + 2.75, y + 8, 7, "#ketting"), _doos(x0 + 2.25, y + 1, 9, x0 + 2.75, y + 8, 9.5, "#ketting")]

    basis = [_doos(4, 0, 4, 12, 1, 12, "#voet"), _doos(5.5, 1, 5.5, 10.5, 2, 10.5, "#voet"), _doos(7, 2, 7, 9, 14, 9, "#messing"),
             _doos(6.5, 14, 6.5, 9.5, 15.5, 9.5, "#messing")]
    # (seen from the front, the north side of the model, the left pan is the one at high x)
    standen = {"midden": (5, 5, 13, 13), "links": (8, 2, 15, 11), "rechts": (2, 8, 11, 15)}   # pan y at low x / high x, beam y at low x / high x
    for stand, (y_laag_x, y_hoog_x, balk_laag_x, balk_hoog_x) in standen.items():
        el = list(basis)
        el.append(_doos(5.5, 13, 7.5, 10.5, 14, 8.5, "#messing"))                 # the middle of the beam
        el.append(_doos(0.5, balk_laag_x, 7.5, 5.5, balk_laag_x + 1, 8.5, "#messing"))
        el.append(_doos(10.5, balk_hoog_x, 7.5, 15.5, balk_hoog_x + 1, 8.5, "#messing"))
        el += schaal(0.5, y_laag_x) + schaal(10.5, y_hoog_x)
        _model(h, "campingmarkt_weegschaal" + ("" if stand == "midden" else f"_{stand}"), t, el)
    states = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for stand in standen:
            v = {"model": "guhs:block/campingmarkt_weegschaal" + ("" if stand == "midden" else f"_{stand}")}
            if y:
                v["y"] = y
            states[f"facing={facing},stand={stand}"] = v
    h.w(f"{h.A}/blockstates/campingmarkt_weegschaal.json", {"variants": states})
    h.w(f"{h.A}/models/item/campingmarkt_weegschaal.json", {"parent": "guhs:block/campingmarkt_weegschaal"})


def vadsstapel(h):
    h.save(tex.vadsstaven(31339), "block", "campingmarkt_vadsstapel.png")
    for n in range(1, 6):
        h.save(tex.nummerbord(n, 31340 + n), "block", f"campingmarkt_nummer_{n}.png")
        t = {"particle": "guhs:block/campingmarkt_vadsstapel", "vads": "guhs:block/campingmarkt_vadsstapel", "blad": "guhs:block/gepolijst_roosterijzer",
             "bord": f"guhs:block/campingmarkt_nummer_{n}", "messing": "guhs:block/campingmarkt_messing"}
        staaf = [0, 0, 8, 4]
        el = [_doos(1, 0, 3, 15, 1, 13, "#blad")]
        for (x, y) in ((1.5, 1), (6, 1), (10.5, 1), (3.75, 4), (8.25, 4), (6, 7)):    # 3 + 2 + 1 bars, their ends to the front
            el.append(_doos(x, y, 3.5, x + 4, y + 3, 12.5, "#vads", {"north": ("#vads", staaf), "south": ("#vads", staaf)}))
        el.append(_doos(5, 1, 2.5, 11, 7, 3, "#messing", {"north": ("#bord", [3, 3, 13, 13])}))
        _model(h, f"campingmarkt_vadsstapel_{n}", t, el)
    states = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for n in range(1, 6):
            v = {"model": f"guhs:block/campingmarkt_vadsstapel_{n}"}
            if y:
                v["y"] = y
            states[f"facing={facing},nummer={n}"] = v
    h.w(f"{h.A}/blockstates/campingmarkt_vadsstapel.json", {"variants": states})
    h.w(f"{h.A}/models/item/campingmarkt_vadsstapel.json", {"parent": "guhs:block/campingmarkt_vadsstapel_1"})


# =====================================================================================================================
# the characters
# =====================================================================================================================
def _streep(kleur, elke, dik):
    def f(block):
        for y in range(0, 32, elke):
            block[y:y + dik, :, :3] = kleur
    return f


def _ruit(a, b):
    def f(block):
        for yy in range(32):
            for xx in range(32):
                block[yy, xx, :3] = a if ((xx // 8) + (yy // 8)) % 2 else b
    return f


def kampbaas(h):
    c = hulp.cube
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_kampbaasguh")
    sw = hulp.swatches(geo, ["hoed", "band", "das", "dasrand", "koord", "fluit"])
    geo["bones"].append({"name": "kampbaas_hoed", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-8.2, 24.2, -8.8], [16.4, 0.8, 16.0], sw["hoed"]),                    # the wide flat brim
        c([-4.8, 25.0, -5.4], [9.6, 3.4, 9.2], sw["hoed"]),                      # the crown
        c([-5.0, 25.0, -5.6], [10.0, 1.1, 9.6], sw["band"]),                     # a pink band
        c([-3.4, 28.4, -4.0], [6.8, 0.9, 6.4], sw["hoed"]),
        c([-0.9, 25.2, -6.0], [1.8, 1.5, 0.5], sw["fluit"])]})                   # the camping's badge on the band
    geo["bones"].append({"name": "kampbaas_das", "parent": "body", "pivot": [0, 13, 0], "cubes": [
        c([-5.2, 11.4, -4.6], [10.4, 1.8, 8.4], sw["das"], inflate=0.1), c([-5.3, 11.2, -4.7], [10.6, 0.5, 8.6], sw["dasrand"], inflate=0.1),
        c([-1.6, 9.2, -4.9], [3.2, 2.4, 0.6], sw["das"])]})
    geo["bones"].append({"name": "kampbaas_fluitje", "parent": "body", "pivot": [0, 11.2, -5.0], "cubes": [
        c([2.0, 7.4, -5.3], [0.5, 4.0, 0.4], sw["koord"]), c([1.4, 6.0, -5.8], [1.8, 1.4, 1.0], sw["fluit"])]})
    hulp.save_geo(h, "guh_npc_kampbaasguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.27, sat=0.5, val=0.92)
    rng = np.random.default_rng(31351)
    hulp.paint_swatch(a, sw["hoed"], (150, 124, 78), rng, 8)
    hulp.paint_swatch(a, sw["band"], (240, 130, 176), rng, 6)
    hulp.paint_swatch(a, sw["das"], (236, 196, 72), rng, 6)
    hulp.paint_swatch(a, sw["dasrand"], (196, 60, 54), rng, 5)
    hulp.paint_swatch(a, sw["koord"], (70, 60, 52), rng, 4)
    hulp.paint_swatch(a, sw["fluit"], (226, 228, 234), rng, 4)
    h.save(Image.fromarray(a), "entity", "npc_kampbaasguh.png")


def houthakker(h):
    c = hulp.cube
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_houthakkerguh")
    sw = hulp.swatches(geo, ["muts", "rand", "ruit", "steel", "blad"])
    geo["bones"].append({"name": "houthakker_muts", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-6.8, 23.8, -7.4], [13.6, 1.8, 13.2], sw["rand"]),                    # the turned-up rim
        c([-6.0, 25.6, -6.6], [12.0, 2.4, 11.6], sw["muts"]), c([-4.4, 28.0, -5.0], [8.8, 1.4, 8.4], sw["muts"]),
        c([-1.4, 29.4, -2.0], [2.8, 2.4, 2.8], sw["rand"])]})                    # the pompom
    geo["bones"].append({"name": "houthakker_kraag", "parent": "body", "pivot": [0, 13, 0], "cubes": [
        c([-5.4, 10.4, -4.8], [10.8, 2.8, 8.8], sw["ruit"], inflate=0.12)]})
    geo["bones"].append({"name": "houthakker_bijl", "parent": "body", "pivot": [6.2, 6, 0], "cubes": [
        c([5.7, 2.0, -0.5], [1.0, 16.0, 1.0], sw["steel"], rotation=[0, 0, -12], pivot=[6.2, 6, 0]),
        c([6.4, 15.0, -0.3], [3.4, 3.4, 0.6], sw["blad"], rotation=[0, 0, -12], pivot=[6.2, 6, 0]),
        c([9.4, 14.6, -0.3], [0.8, 4.2, 0.6], sw["blad"], rotation=[0, 0, -12], pivot=[6.2, 6, 0])]})
    hulp.save_geo(h, "guh_npc_houthakkerguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.045, sat=0.72, val=0.9)
    rng = np.random.default_rng(31352)
    hulp.paint_swatch(a, sw["muts"], (200, 52, 48), rng, 8, _streep((166, 38, 38), 4, 1))
    hulp.paint_swatch(a, sw["rand"], (244, 240, 232), rng, 8)
    hulp.paint_swatch(a, sw["ruit"], (190, 44, 44), rng, 4, _ruit((190, 44, 44), (40, 34, 36)))
    hulp.paint_swatch(a, sw["steel"], (142, 100, 60), rng, 6)
    hulp.paint_swatch(a, sw["blad"], (206, 210, 218), rng, 5)
    h.save(Image.fromarray(a), "entity", "npc_houthakkerguh.png")


def _mika(h, identifier):
    geo_file = kn._load(h, "mika.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = f"geometry.{identifier}"
    return geo_file, geo


def _mika_anims():
    """The animations of a Mika character (the names the NPC renderer plays): breathing, a wagging tail, twitching ears."""
    idle = {"body": {"scale": {"0.0": [1, 1, 1], "2.0": [1.02, 1.03, 1.02], "4.0": [1, 1, 1]}},
            "head": {"rotation": {"0.0": [-6, 0, 0], "1.0": [-8, 0, 3], "2.0": [-6, 0, 0], "3.0": [-8, 0, -3], "4.0": [-6, 0, 0]}},
            "tail": {"rotation": {"0.0": [0, -12, 0], "1.0": [0, 12, 0], "2.0": [0, -12, 0], "3.0": [0, 12, 0], "4.0": [0, -12, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.6": [0, 0, 0], "2.75": [0, 0, -14], "2.9": [0, 0, 0], "4.0": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "3.1": [0, 0, 0], "3.25": [0, 0, 14], "3.4": [0, 0, 0], "4.0": [0, 0, 0]}}}
    happy = {"body": {"position": {"0.0": [0, 0, 0], "0.15": [0, 2, 0], "0.3": [0, 0, 0], "0.45": [0, 1.5, 0], "0.6": [0, 0, 0]}},
             "head": {"rotation": {"0.0": [-6, 0, 0], "0.3": [-20, 0, 0], "0.6": [-6, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "animation.guh_sitting.idle": {"loop": True, "animation_length": 4.0, "bones": idle},
        "animation.guh_sitting.happy": {"loop": False, "animation_length": 0.6, "bones": happy}}}


def _glas(block):
    block[..., :3] = (206, 232, 244)
    block[6:14, 6:14, :3] = (240, 250, 255)
    block[:4, :, :3] = block[-4:, :, :3] = (214, 172, 70)
    block[:, :4, :3] = block[:, -4:, :3] = (214, 172, 70)


def _medaillon(block):
    block[..., :3] = (250, 204, 70)
    for yy in range(32):
        for xx in range(32):
            d = ((xx - 15.5) ** 2 + (yy - 15.5) ** 2) ** 0.5
            if d > 14:
                block[yy, xx, :3] = (190, 140, 40)
            elif d < 8:
                block[yy, xx, :3] = (142, 86, 172)                 # a bar of vads in the middle


def marktmeester(h):
    geo_file, geo = _mika(h, "guh_npc_marktmeester_mika")
    sw = kn._swatches(geo, ["hoed", "band", "snor", "monocle", "ketting", "medaillon"])
    c = kn._cube
    H = [0, 12, -6]
    geo["bones"].append({"name": "markt_hoed", "parent": "head", "pivot": H, "cubes": [
        c([-6.8, 12.3, -12.6], [13.6, 0.7, 11.6], sw["hoed"]),                    # the brim
        c([-4.6, 13.0, -10.8], [9.2, 6.0, 8.0], sw["hoed"]),                      # the tall crown
        c([-4.8, 13.0, -11.0], [9.6, 1.3, 8.4], sw["band"]),                      # a golden band
        c([-5.0, 18.8, -11.2], [10.0, 0.7, 8.8], sw["hoed"])]})                   # the flat top, a little wider
    geo["bones"].append({"name": "markt_snor", "parent": "head", "pivot": [0, 4, -12.8], "cubes": [
        c([-5.0, 3.0, -13.4], [4.2, 1.7, 0.8], sw["snor"]), c([0.8, 3.0, -13.4], [4.2, 1.7, 0.8], sw["snor"]),
        c([-6.2, 3.8, -13.3], [1.4, 1.8, 0.7], sw["snor"]), c([4.8, 3.8, -13.3], [1.4, 1.8, 0.7], sw["snor"])]})   # curled up at the tips
    geo["bones"].append({"name": "markt_monocle", "parent": "head", "pivot": H, "cubes": [
        c([1.2, 5.2, -12.8], [4.6, 4.6, 0.5], sw["monocle"]), c([5.4, 1.0, -12.7], [0.4, 4.4, 0.4], sw["ketting"])]})
    geo["bones"].append({"name": "markt_ketting", "parent": "head", "pivot": [0, 1, -8], "cubes": [
        c([-5.0, 0.4, -12.0], [10, 0.9, 0.7], sw["ketting"]), c([-1.6, -2.6, -12.4], [3.2, 3.2, 0.7], sw["medaillon"])]})
    kn._save_geo(h, "guh_npc_marktmeester_mika.geo.json", geo_file)
    a = np.asarray(Image.open(os.path.join(h.TEX, "entity", "nether_mika.png")).convert("RGBA")).copy()
    rng = np.random.default_rng(31353)
    p = kn._paint
    p(a, sw["hoed"], (88, 50, 120), rng, 8, _streep((70, 38, 100), 8, 1))
    p(a, sw["band"], (250, 204, 70), rng, 5)
    p(a, sw["snor"], (236, 232, 224), rng, 8, _streep((204, 198, 190), 4, 1))
    p(a, sw["monocle"], (206, 232, 244), rng, 3, _glas)
    p(a, sw["ketting"], (250, 204, 70), rng, 5)
    p(a, sw["medaillon"], (250, 204, 70), rng, 4, _medaillon)
    h.save(Image.fromarray(a), "entity", "npc_marktmeester_mika.png")
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "guh_npc_marktmeester_mika.animation.json"), _mika_anims())


def kraam_mika(h):
    geo_file, geo = _mika(h, "campingmarkt_kraam_mika")
    sw = kn._swatches(geo, ["pet", "klep", "schort", "schortrand"])
    c = kn._cube
    H = [0, 12, -6]
    geo["bones"].append({"name": "kraam_pet", "parent": "head", "pivot": H, "cubes": [
        c([-6.0, 12.3, -11.8], [12, 1.9, 10], sw["pet"]), c([-5.0, 14.2, -10.6], [10, 0.9, 8], sw["pet"]),
        c([-5.2, 12.3, -14.4], [10.4, 0.6, 2.8], sw["klep"])]})
    geo["bones"].append({"name": "kraam_schort", "parent": "body", "pivot": [0, 6, 2], "cubes": [
        c([-7.2, 3.2, -2.6], [14.4, 6.0, 8.6], sw["schort"]), c([-7.4, 2.6, -2.8], [14.8, 0.8, 9.0], sw["schortrand"])]})
    kn._save_geo(h, "campingmarkt_kraam_mika.geo.json", geo_file)
    a = np.asarray(Image.open(os.path.join(h.TEX, "entity", "nether_mika.png")).convert("RGBA")).copy()
    rng = np.random.default_rng(31354)
    p = kn._paint
    p(a, sw["pet"], (112, 122, 96), rng, 10, _ruit((96, 106, 84), (128, 138, 108)))
    p(a, sw["klep"], (78, 86, 66), rng, 6)
    p(a, sw["schort"], (236, 226, 200), rng, 6, _streep((196, 64, 56), 8, 4))
    p(a, sw["schortrand"], (196, 64, 56), rng, 5)
    h.save(Image.fromarray(a), "entity", "campingmarkt_kraam_mika.png")


def build(h):
    tentdoek(h)
    kampeerplek(h)
    haring(h)
    hakblok(h)
    kampvuur(h)
    weegschaal(h)
    vadsstapel(h)
    kampbaas(h)
    houthakker(h)
    marktmeester(h)
    kraam_mika(h)
