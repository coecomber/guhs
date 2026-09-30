"""
3.0 (Guhverhalen), balto_slee - the models:

  geo/entity/baltoslee_slee.geo.json        the Nomguh sled (also your own sneeuwslee, with another texture): two runners with
                                             curled fronts that reach far back (the musher stands on them), a slatted bed with a
                                             blanket, a basket rail, a brush bow with a little guh head on it, the handlebar with
                                             three bells (bone "bellen"), a lantern on a pole (bone "lantaarn") and the medicine
                                             chest with a pink heart (bone "kist", shown after the berghut)
  geo/entity/baltoslee_sledehondje.geo.json  a guh-sledehondje: a round guh with husky ears, a curly tail, a red harness, legs
                                             that gallop (bones lijf, kop, oor_l/oor_r, staart, tong, belletje, been_lv/rv/la/ra;
                                             moved by the renderer, no animation file)
  models/block/baltoslee_*.json              Steele-Mika's winter deco (element models; front = north)

The swatch tables (SLEE_SW, HOND_SW) are shared with balto_slee_tex.py, which paints them.
"""
import os

SW = 8                     # swatch size in the entity textures

# the sled's 64x64 texture: name -> (u, v)
SLEE_SW = {n: ((i % 8) * SW, (i // 8) * SW) for i, n in enumerate([
    "hout", "hout_donker", "ijzer", "deken", "deken_rand", "goud", "glas", "frame",
    "kist", "kist_hart", "riem", "guhkop", "guhgezicht", "oor", "lint", "deksel"])}

# the dog's 32x32 texture
HOND_SW = {n: ((i % 4) * SW, (i // 4) * SW) for i, n in enumerate([
    "vacht", "buik", "gezicht", "snoet",
    "oor", "tuig", "tong", "poot",
    "goud", "staart", "rug", "neus",
    "oor_binnen", "masker", "wit", "leeg"])}


def cube(origin, size, uv, faces=None, inflate=0.0, s=1.0):
    """A GeckoLib cube (scaled by s) whose faces each show one 8x8 swatch (faces: face -> another swatch)."""
    out = {"origin": [round(v * s, 3) for v in origin], "size": [round(v * s, 3) for v in size], "uv": {}}
    for f in ("north", "south", "east", "west", "up", "down"):
        u = (faces or {}).get(f, uv)
        out["uv"][f] = {"uv": list(u), "uv_size": [SW, SW]}
    if inflate:
        out["inflate"] = inflate
    return out


def geo(identifier, tw, th, bones, bounds=(3, 2, 0.5)):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": identifier, "texture_width": tw, "texture_height": th, "visible_bounds_width": bounds[0],
                        "visible_bounds_height": bounds[1], "visible_bounds_offset": [0, bounds[2], 0]},
        "bones": bones}]}


# =====================================================================================================================
# the sled
# =====================================================================================================================
def slee():
    w = SLEE_SW
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    romp = []
    for x in (5.0, -6.2):                                   # the runners (left, right): iron shoe, wooden runner, curled front
        romp.append(cube([x, 0, -16], [1.2, 0.8, 37.5], w["ijzer"]))
        romp.append(cube([x, 0.8, -14], [1.2, 1.0, 34.5], w["hout_donker"]))
        romp.append(cube([x, 0.6, -17.6], [1.2, 1.4, 1.8], w["ijzer"]))
        romp.append(cube([x, 1.8, -18.6], [1.2, 2.2, 1.4], w["hout_donker"]))
        romp.append(cube([x, 3.8, -18.3], [1.2, 1.8, 1.2], w["hout_donker"]))
        romp.append(cube([x, 5.4, -17.4], [1.2, 1.0, 1.4], w["hout_donker"]))
        for z in (-12.0, -4.0, 4.0, 11.0):                   # the stanchions
            romp.append(cube([x + 0.1, 1.8, z], [1.0, 3.4, 1.0], w["hout"]))
        romp.append(cube([x + 0.2, 7.2, -14], [0.8, 0.8, 26.5], w["hout"]))        # the basket rail
        for z in (-12.0, -4.0, 4.0):
            romp.append(cube([x + 0.25, 5.2, z + 0.15], [0.7, 2.0, 0.7], w["hout"]))
    for z in range(-14, 12, 3):                              # the slats of the bed
        romp.append(cube([-6.2, 5.0, z], [12.4, 0.8, 2.2], w["hout"]))
    romp.append(cube([-6.2, 5.0, -15.4], [12.4, 3.6, 1.0], w["hout_donker"]))    # the brush bow
    romp.append(cube([-5.0, 5.8, -12.5], [10.0, 0.7, 18.0], w["deken"], faces={"up": w["deken"]}))   # the blanket
    romp.append(cube([-5.0, 6.5, -13.8], [10.0, 1.6, 2.2], w["deken_rand"]))   # rolled up at the front
    romp.append(cube([-5.2, 5.6, 5.2], [10.4, 1.0, 1.2], w["deken_rand"]))     # its fringe
    romp.append(cube([-0.6, 2.2, -18.9], [1.2, 1.2, 1.2], w["ijzer"]))          # the hook for the ropes
    romp.append(cube([-6.2, 3.2, -18.9], [12.4, 0.8, 0.8], w["hout_donker"]))   # the front bar between the curls
    # the handlebar at the back, and the brake board between the runners
    romp.append(cube([4.9, 5.0, 12.4], [1.0, 11.0, 1.0], w["hout"]))
    romp.append(cube([-5.9, 5.0, 12.4], [1.0, 11.0, 1.0], w["hout"]))
    romp.append(cube([-6.0, 15.4, 12.2], [12.0, 1.4, 1.4], w["hout_donker"]))
    romp.append(cube([-4.2, 15.2, 12.0], [2.4, 1.8, 1.8], w["riem"]))
    romp.append(cube([1.8, 15.2, 12.0], [2.4, 1.8, 1.8], w["riem"]))
    romp.append(cube([-5.9, 9.5, 12.5], [11.8, 0.8, 0.8], w["hout"]))
    bones.append({"name": "romp", "parent": "root", "pivot": [0, 0, 0], "cubes": romp})
    # the little guh head on the brush bow (a guh twist)
    bones.append({"name": "guhkopje", "parent": "romp", "pivot": [0, 8.6, -15], "cubes": [
        cube([-1.8, 8.6, -16.2], [3.6, 3.0, 2.4], w["guhkop"], faces={"north": w["guhgezicht"]}),
        cube([-1.9, 11.3, -15.4], [1.1, 1.2, 0.8], w["oor"]),
        cube([0.8, 11.3, -15.4], [1.1, 1.2, 0.8], w["oor"])]})
    # three bells on a red ribbon under the handlebar
    bellen = [cube([-4.2, 14.9, 12.6], [8.4, 0.5, 0.5], w["lint"])]
    for x in (-3.4, -0.8, 1.8):
        bellen.append(cube([x, 13.2, 12.3], [1.6, 1.8, 1.6], w["goud"]))
        bellen.append(cube([x + 0.5, 12.8, 12.8], [0.6, 0.4, 0.6], w["hout_donker"]))
    bones.append({"name": "bellen", "parent": "romp", "pivot": [0, 15.2, 12.9], "cubes": bellen})
    # the lantern on its pole (front left)
    bones.append({"name": "lantaarnpaal", "parent": "romp", "pivot": [4.8, 7, -13], "cubes": [
        cube([4.4, 7.4, -13.8], [0.8, 10.0, 0.8], w["hout_donker"]),
        cube([2.4, 16.6, -13.8], [2.8, 0.8, 0.8], w["hout_donker"])]})
    bones.append({"name": "lantaarn", "parent": "lantaarnpaal", "pivot": [3.0, 16.6, -13.4], "cubes": [
        cube([1.7, 12.6, -14.7], [2.6, 3.6, 2.6], w["glas"], faces={"up": w["frame"], "down": w["frame"]}),
        cube([1.5, 16.2, -14.9], [3.0, 0.6, 3.0], w["frame"]),
        cube([1.5, 12.2, -14.9], [3.0, 0.5, 3.0], w["frame"])]})
    # the medicine chest (after the berghut): white with a pink heart, a leather strap
    bones.append({"name": "kist", "parent": "romp", "pivot": [0, 6.5, -3], "cubes": [
        cube([-4.0, 6.5, -6.5], [8.0, 5.2, 6.0], w["kist"], faces={"east": w["kist_hart"], "west": w["kist_hart"], "south": w["kist_hart"],
                                                                     "north": w["kist_hart"], "up": w["deksel"]}),
        cube([-4.3, 9.8, -4.3], [8.6, 0.6, 1.6], w["riem"]),
        cube([-1.0, 11.7, -4.0], [2.0, 0.8, 1.0], w["riem"])]})
    return geo("geometry.baltoslee_slee", 64, 64, bones, (3.2, 1.6, 0.5))


# =====================================================================================================================
# the guh-sledehondje (scaled down: a whole team fits in front of the sled)
# =====================================================================================================================
S = 0.8


def hondje():
    w = HOND_SW
    c = lambda o, sz, uv, faces=None: cube(o, sz, uv, faces, s=S)  # noqa: E731
    bones = [{"name": "root", "pivot": [0, 0, 0]},
             {"name": "lijf", "parent": "root", "pivot": [0, 3.2 * S, 0], "cubes": [
                 c([-2.8, 3.0, -3.6], [5.6, 4.8, 7.6], w["vacht"], {"down": w["buik"], "up": w["rug"], "north": w["buik"]}),
                 c([-3.05, 3.9, -3.1], [6.1, 3.2, 1.3], w["tuig"]),         # the harness round the chest
                 c([-0.8, 7.75, -3.1], [1.6, 0.35, 6.2], w["tuig"]),        # and along the back
                 c([-3.0, 3.3, 0.6], [6.0, 0.7, 1.0], w["tuig"])]},
             {"name": "belletje", "parent": "lijf", "pivot": [0, 3.4 * S, -4.2 * S], "cubes": [
                 c([-0.8, 2.6, -4.7], [1.6, 1.6, 1.6], w["goud"])]},
             {"name": "kop", "parent": "lijf", "pivot": [0, 7.0 * S, -3.6 * S], "cubes": [
                 c([-3.4, 5.8, -8.8], [6.8, 6.0, 5.6], w["vacht"], {"north": w["gezicht"], "down": w["buik"]}),
                 c([-1.7, 6.0, -10.1], [3.4, 2.2, 1.4], w["snoet"], {"north": w["neus"]})]},
             {"name": "tong", "parent": "kop", "pivot": [0, 6.0 * S, -9.9 * S], "cubes": [
                 c([-0.6, 4.9, -9.95], [1.2, 1.3, 0.35], w["tong"])]},
             {"name": "oor_l", "parent": "kop", "pivot": [2.2 * S, 11.8 * S, -6.3 * S], "cubes": [
                 c([1.2, 11.7, -6.9], [2.1, 1.8, 1.0], w["oor"], {"north": w["oor_binnen"]}),
                 c([1.65, 13.4, -6.8], [1.2, 1.1, 0.8], w["oor"], {"north": w["oor_binnen"]})]},
             {"name": "oor_r", "parent": "kop", "pivot": [-2.2 * S, 11.8 * S, -6.3 * S], "cubes": [
                 c([-3.3, 11.7, -6.9], [2.1, 1.8, 1.0], w["oor"], {"north": w["oor_binnen"]}),
                 c([-2.85, 13.4, -6.8], [1.2, 1.1, 0.8], w["oor"], {"north": w["oor_binnen"]})]},
             {"name": "staart", "parent": "lijf", "pivot": [0, 6.8 * S, 3.9 * S], "cubes": [
                 c([-0.7, 6.4, 3.8], [1.4, 1.4, 1.8], w["staart"]),
                 c([-0.7, 7.4, 5.2], [1.4, 2.2, 1.4], w["staart"]),
                 c([-0.6, 9.4, 4.5], [1.2, 1.1, 1.2], w["wit"])]}]
    for name, x, z in (("been_lv", 0.8, -3.3), ("been_rv", -2.6, -3.3), ("been_la", 0.8, 1.6), ("been_ra", -2.6, 1.6)):
        bones.append({"name": name, "parent": "root", "pivot": [(x + 0.9) * S, 3.2 * S, (z + 0.9) * S], "cubes": [
            c([x, 0, z], [1.8, 3.3, 1.8], w["poot"], {"down": w["wit"]})]})
    return geo("geometry.baltoslee_sledehondje", 32, 32, bones, (1.4, 1.2, 0.3))


def build(h):
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "baltoslee_slee.geo.json"), slee())
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "baltoslee_slee.animation.json"), {"format_version": "1.8.0", "animations": {}})
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "baltoslee_sledehondje.geo.json"), hondje())
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "baltoslee_sledehondje.animation.json"), {"format_version": "1.8.0", "animations": {}})
    deco(h)


# =====================================================================================================================
# the winter deco (block models, front = north; GuhFurnitureBlock turns them to face you)
# =====================================================================================================================
def _el(frm, to, tex, uv=None, faces=None, rot=None, tint=None):
    e = {"from": frm, "to": to, "faces": {}}
    for f in faces or ("down", "up", "north", "south", "west", "east"):
        face = {"texture": tex if isinstance(tex, str) else tex.get(f, tex["*"])}
        if uv:
            face["uv"] = uv
        e["faces"][f] = face
    if rot:
        e["rotation"] = rot
    return e


DECO = ["baltoslee_sneeuwguh", "baltoslee_minislee", "baltoslee_sledebellen", "baltoslee_beker", "baltoslee_hondenmand", "baltoslee_lantaarnpaal"]


def deco(h):
    T = "guhs:block/"
    # the snow guh: a big snowball body, a round guh head with ears, a carrot snout, a red scarf, stick arms
    sneeuw, gezicht = "#sneeuw", "#gezicht"
    h.furniture_model("baltoslee_sneeuwguh", [
        _el([3, 0, 3], [13, 1, 13], "#sneeuw"), _el([2, 1, 2], [14, 8, 14], "#sneeuw"), _el([3, 8, 3], [13, 9, 13], "#sneeuw"),
        _el([4.5, 9, 4.5], [11.5, 10, 11.5], "#sneeuw"),
        _el([4, 10, 4], [12, 15, 12], {"*": sneeuw, "north": gezicht}),
        _el([4.5, 15, 4.5], [11.5, 16, 11.5], "#sneeuw"),
        _el([4.4, 15.6, 6.6], [6.4, 17.2, 8.4], "#sneeuw"), _el([9.6, 15.6, 6.6], [11.6, 17.2, 8.4], "#sneeuw"),
        _el([7.2, 11.2, 2.3], [8.8, 12.6, 4], "#wortel"),
        _el([3.8, 9, 3.8], [12.2, 10.4, 12.2], "#sjaal"),
        _el([9.3, 4, 1.6], [11.3, 9.4, 2.6], "#sjaal"),
        _el([0.2, 5.5, 7.4], [2.2, 6.4, 8.4], "#stok", rot={"origin": [2.2, 6, 8], "axis": "z", "angle": 22.5}),
        _el([13.8, 5.5, 7.4], [15.8, 6.4, 8.4], "#stok", rot={"origin": [13.8, 6, 8], "axis": "z", "angle": -22.5}),
        _el([7.4, 2.5, 1.7], [8.6, 3.7, 2], "#stok"), _el([7.4, 5, 1.7], [8.6, 6.2, 2], "#stok")],
        {"particle": T + "baltoslee_sneeuw", "sneeuw": T + "baltoslee_sneeuw", "gezicht": T + "baltoslee_sneeuwguh_gezicht",
         "wortel": T + "baltoslee_wortel", "sjaal": T + "baltoslee_sjaal", "stok": T + "baltoslee_hout"})
    # the mini sled: two runners with curled fronts, slats, a red blanket (you can sit in it)
    els = []
    for x0 in (1, 13.5):
        els += [_el([x0, 0, 1], [x0 + 1.5, 1, 16], "#ijzer"), _el([x0, 1, 2], [x0 + 1.5, 2, 15], "#hout"),
                _el([x0, 1, 0], [x0 + 1.5, 4, 1.5], "#hout"), _el([x0, 4, 0.5], [x0 + 1.5, 5.5, 2], "#hout")]
    els += [_el([1, 2, 2], [15, 3, 15], "#hout"), _el([2, 3, 3], [14, 4, 14], "#deken"), _el([2, 4, 2.5], [14, 5.5, 4.5], "#rand"),
            _el([1, 3, 14], [15, 7, 15.5], "#hout"), _el([1, 7, 13.8], [15, 8, 15.7], "#hout")]
    h.furniture_model("baltoslee_minislee", els, {"particle": T + "baltoslee_hout", "hout": T + "baltoslee_hout", "ijzer": T + "baltoslee_ijzer",
                                                  "deken": T + "baltoslee_deken", "rand": T + "baltoslee_deken_rand"})
    # the bell arch: two posts, a beam with a red ribbon, three golden bells
    h.furniture_model("baltoslee_sledebellen", [
        _el([1, 0, 6.5], [3, 14, 9.5], "#hout"), _el([13, 0, 6.5], [15, 14, 9.5], "#hout"),
        _el([0.5, 13, 6], [15.5, 15, 10], "#hout"), _el([0.4, 12.5, 7.4], [15.6, 13.3, 8.6], "#lint"),
        _el([2.2, 14.9, 7.2], [4, 16, 8.8], "#sneeuw"), _el([12, 14.9, 7.2], [13.8, 16, 8.8], "#sneeuw"),
        _el([3.5, 8.5, 6.8], [5.5, 11, 9.2], "#goud"), _el([4.1, 11, 7.6], [4.9, 12.5, 8.4], "#lint"),
        _el([7, 7.5, 6.8], [9, 10, 9.2], "#goud"), _el([7.6, 10, 7.6], [8.4, 12.5, 8.4], "#lint"),
        _el([10.5, 8.5, 6.8], [12.5, 11, 9.2], "#goud"), _el([11.1, 11, 7.6], [11.9, 12.5, 8.4], "#lint")],
        {"particle": T + "baltoslee_hout", "hout": T + "baltoslee_hout", "lint": T + "baltoslee_lint", "goud": T + "baltoslee_goud",
         "sneeuw": T + "baltoslee_sneeuw"})
    # the sledesprint cup: a wide base, a stem, the cup with two ears (guh ears on top!)
    h.furniture_model("baltoslee_beker", [
        _el([4, 0, 4], [12, 1.5, 12], "#voet"), _el([5, 1.5, 5], [11, 2.5, 11], "#goud"), _el([7, 2.5, 7], [9, 6, 9], "#goud"),
        _el([4.5, 6, 4.5], [11.5, 11.5, 11.5], {"*": "#goud", "north": "#bord"}), _el([5, 11.5, 5], [11, 12, 11], "#goud"),
        _el([2.5, 7.5, 7.2], [4.5, 10.5, 8.8], "#goud"), _el([11.5, 7.5, 7.2], [13.5, 10.5, 8.8], "#goud"),
        _el([4.5, 11.5, 7], [6, 13, 9], "#goud"), _el([10, 11.5, 7], [11.5, 13, 9], "#goud")],
        {"particle": T + "baltoslee_goud", "goud": T + "baltoslee_goud", "voet": T + "baltoslee_hout", "bord": T + "baltoslee_beker_bord"})
    # the dog basket: a round-ish wicker basket with a plaid cushion and a bone-shaped knabbel
    h.furniture_model("baltoslee_hondenmand", [
        _el([1, 0, 1], [15, 1, 15], "#mand"), _el([1, 1, 1], [15, 5, 2.5], "#mand"), _el([1, 1, 13.5], [15, 5, 15], "#mand"),
        _el([1, 1, 2.5], [2.5, 5, 13.5], "#mand"), _el([13.5, 1, 2.5], [15, 5, 13.5], "#mand"),
        _el([1, 1, 1], [15, 3.5, 1.2], "#mand"), _el([2.5, 1, 2.5], [13.5, 3, 13.5], "#kussen"),
        _el([5, 3, 6.5], [11, 4, 7.5], "#knabbel"), _el([4.4, 2.8, 6], [5.6, 4.2, 8], "#knabbel"), _el([10.4, 2.8, 6], [11.6, 4.2, 8], "#knabbel")],
        {"particle": T + "baltoslee_mand", "mand": T + "baltoslee_mand", "kussen": T + "baltoslee_kussen", "knabbel": T + "baltoslee_knabbel"})
    # the route lantern: a wooden post with a red ribbon, a lantern with warm glass and a snow cap
    h.furniture_model("baltoslee_lantaarnpaal", [
        _el([6.5, 0, 6.5], [9.5, 11, 9.5], "#hout"), _el([6.3, 7, 6.3], [9.7, 8, 9.7], "#lint"),
        _el([5, 11, 5], [11, 11.8, 11], "#frame"), _el([5.4, 11.8, 5.4], [10.6, 15, 10.6], "#glas"), _el([5, 15, 5], [11, 15.8, 11], "#frame"),
        _el([5.5, 15.8, 5.5], [10.5, 16, 10.5], "#sneeuw")],
        {"particle": T + "baltoslee_hout", "hout": T + "baltoslee_hout", "lint": T + "baltoslee_lint", "frame": T + "baltoslee_ijzer",
         "glas": T + "baltoslee_glas", "sneeuw": T + "baltoslee_sneeuw"})
