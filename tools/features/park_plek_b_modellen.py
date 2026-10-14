"""
1.4.2 Het Knabbelpark, park-plekjes b - the looks (tools/features/park_plek_b.py builds the rest):

  duifguhtje        a round pigeon parody: blue-grey with a glossy green-and-purple neck band, two dark bars on its wings, a dark
                    tail tip, coral feet, and (like every bird of the Guhmensie, tools/features/vogels_modellen.py) big glossy
                    guh eyes, a blush and two round guh ears. Animations: the birds' idle / hop / fly / peck and "koer" (the
                    coo: a puffed chest and a bobbing head). Texture + _dicht (eyes shut) for the birds' renderer.
  omaguh            the sitting guh in soft lilac-grey: a white bun, round glasses, a knitted shawl and a paper bag of crumbs
                    on her lap (bone oma_zakje: client PlekBClient throws a handful now and then).
  mokmika           the Mika's model in rainy blue-grey with two cross eyebrows and a little rain cloud over his head (bone
                    mok_wolk, it bobs); a sulky idle (the head hangs).
  guhkindje         a plain small sitting guh (bbq2.stub_npc: texture only).
  blocks            park_verrekijker (a pink brass telescope on a tripod), park_poppenkast (the stage: curtains shut, or open
                    with the guh puppet and the Mika puppet), park_wensstam (blossom bark with ribbons tied round it).
  items             park_kruimels (a paper bag of crumbs), duifguhtje_spawn_egg.
"""
import os

import numpy as np
from PIL import Image

from features import knuffeldal_npcs as kn
from features import paleizen_modellen as pm
from features import park_modellen as parkm
from features import vogels_modellen as vm


# =====================================================================================================================
# the Duifguhtje
# =====================================================================================================================
GRIJS, GRIJS_L, GRIJS_D, WIT = (150, 160, 190), (184, 192, 214), (104, 112, 142), (232, 234, 244)
NEK_G, NEK_P = (84, 190, 140), (168, 104, 196)


def _nek(w, h, face, rng, closed):
    """The glossy neck band: green into purple, a little shimmer."""
    a = np.empty((h, w, 4), np.float32)
    for y in range(h):
        for x in range(w):
            t = (x + y * 0.6) / max(1, w + h * 0.6)
            c = np.array(NEK_G) * (1 - t) + np.array(NEK_P) * t
            a[y, x, :3] = c + rng.integers(-8, 9, 3)
            a[y, x, 3] = 255
    return np.clip(a, 0, 255)


def _vleugel(w, h, face, rng, closed):
    """A wing: grey with two dark bars across, darker flight feathers at the end."""
    a = vm.tipped(GRIJS, GRIJS_D, frac=0.28)(w, h, face, rng, closed)
    for frac in (0.35, 0.55):
        x0 = int(w * frac)
        a[:, x0:x0 + max(1, w // 10), :3] = (70, 76, 100)
    return a


def duifguhtje(h):
    at = vm.Atlas("duifguhtje", 48, 48)
    body = {"*": vm.vgradient(GRIJS_L, GRIJS, 0.55, 0.3), "up": vm.fluff(GRIJS, GRIJS_L), "down": vm.fluff(WIT, GRIJS_L),
            "north": vm.fluff(GRIJS_L, WIT)}
    head = {"*": vm.fluff(GRIJS_L, GRIJS), "up": vm.fluff(GRIJS, GRIJS_L),
            "north": vm.face(vm.fluff(GRIJS_L, WIT), eye_y=0.44, eye_r=0.2, spread=0.26, iris=((255, 186, 96), (226, 110, 40)),
                             blush_y=0.76, fur=GRIJS_L)}
    nek = {"*": _nek}
    wing = {"*": _vleugel, "up": _vleugel, "down": vm.solid(GRIJS_L)}
    tail = {"*": vm.rounded(vm.tipped(GRIJS, (60, 64, 88), along="y", frac=0.35), 0.4)}
    leg = vm.solid((236, 112, 120))
    bones = [
        vm.bone("root", None, [0, 0, 0]),
        vm.bone("body", "root", [0, 3, 0], [vm.cube(at, [-2.6, 1, -3], [5.2, 4.6, 6.6], body),
                                            vm.cube(at, [-2.2, 4.0, -3.6], [4.4, 1.8, 2.6], nek)]),
        vm.bone("head", "body", [0, 5.2, -2.2], [vm.cube(at, [-1.8, 5.2, -4.6], [3.6, 3.4, 3.2], head)]),
        vm.bone("ear_left", "head", [1.3, 8.4, -2.8], [vm.cube(at, [0.7, 8.2, -3.0], [1.4, 1.4, 0.5], {"*": vm.ear(GRIJS, (255, 150, 176))})]),
        vm.bone("ear_right", "head", [-1.3, 8.4, -2.8], [vm.cube(at, [-2.1, 8.2, -3.0], [1.4, 1.4, 0.5], {"*": vm.ear(GRIJS, (255, 150, 176))})]),
        vm.bone("beak", "head", [0, 6.2, -4.6], [vm.cube(at, [-0.5, 5.8, -5.5], [1.0, 0.7, 0.9], vm.beak((90, 92, 104), (50, 50, 60))),
                                                vm.cube(at, [-0.55, 6.4, -4.9], [1.1, 0.4, 0.4], vm.solid(WIT, 3))]),
        vm.bone("wing_left", "body", [2.6, 5.2, -1.6], [vm.cube(at, [2.6, 2.0, -2.2], [0.5, 3.2, 5.4], wing)]),
        vm.bone("wing_right", "body", [-2.6, 5.2, -1.6], [vm.cube(at, [-3.1, 2.0, -2.2], [0.5, 3.2, 5.4], wing)]),
        vm.bone("tail", "body", [0, 3.4, 3.4], [vm.cube(at, [-1.7, 3.1, 3.4], [3.4, 0.5, 3.0], tail)], rotation=[-14, 0, 0]),
        vm.bone("leg_left", "body", [1, 1, 0], [vm.cube(at, [0.75, 0, -0.25], [0.5, 1, 0.5], leg), vm.cube(at, [0.4, 0, -1.2], [1.2, 0.25, 1.3], leg)]),
        vm.bone("leg_right", "body", [-1, 1, 0], [vm.cube(at, [-1.25, 0, -0.25], [0.5, 1, 0.5], leg), vm.cube(at, [-1.6, 0, -1.2], [1.2, 0.25, 1.3], leg)]),
    ]
    vm.write_geo(h, "duifguhtje", at, bones, 0.9, 0.9)
    at.save(h)
    kf, once = vm.kf, vm.once
    anims = vm.animations(h, "duifguhtje", {
        # roekoe: the chest puffs up, the head bobs back and forth, a little turn on the spot
        "koer": once(1.4, {
            "body": {"scale": kf([(0, (1, 1, 1)), (0.3, (1.14, 1.08, 1.06)), (1.0, (1.14, 1.08, 1.06)), (1.4, (1, 1, 1))]),
                     "rotation": kf([(0, (0, 0, 0)), (0.5, (0, 25, 0)), (1.0, (0, -25, 0)), (1.4, (0, 0, 0))])},
            "head": {"position": kf([(0, (0, 0, 0)), (0.2, (0, 0, -0.8)), (0.4, (0, 0, 0.4)), (0.6, (0, 0, -0.8)), (0.8, (0, 0, 0.4)),
                                     (1.0, (0, 0, -0.8)), (1.2, (0, 0, 0)), (1.4, (0, 0, 0))])},
            "tail": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-20, 0, 0)), (1.0, (-20, 0, 0)), (1.4, (0, 0, 0))])}}),
    })
    return bones, anims


# =====================================================================================================================
# the NPCs
# =====================================================================================================================
def _src(h, name):
    return Image.open(os.path.join(h.TEX, "entity", name)).convert("RGBA")


def omaguh(h):
    geo_file = kn._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_omaguh"
    sw = kn._swatches(geo, ["haar", "knot", "bril", "brilrand", "sjaal", "sjaal_rand", "zak", "kruim"])
    c = kn._cube
    geo["bones"].append({"name": "oma_haar", "parent": "head", "pivot": [0, 13, 0], "cubes": [
        c([-5.8, 25.8, -5.3], [11.6, 0.9, 9.6], sw["haar"]),
        c([-2.4, 26.6, -2.0], [4.8, 3.0, 4.2], sw["knot"]),
        c([-1.6, 29.6, -1.3], [3.2, 0.8, 2.8], sw["knot"])]})
    geo["bones"].append({"name": "oma_bril", "parent": "head", "pivot": [0, 13, 0], "cubes": [
        c([-5.6, 17.4, -7.5], [4.0, 3.4, 0.5], sw["bril"]), c([1.6, 17.4, -7.5], [4.0, 3.4, 0.5], sw["bril"]),
        c([-1.6, 18.8, -7.5], [3.2, 0.6, 0.5], sw["brilrand"])]})
    geo["bones"].append({"name": "oma_sjaal", "parent": "body", "pivot": [0, 10, 0], "cubes": [
        c([-4.6, 9.4, -4.6], [9.2, 2.8, 9.2], sw["sjaal"]), c([-4.8, 9.0, -4.8], [9.6, 0.6, 9.6], sw["sjaal_rand"]),
        c([-1.4, 6.4, -4.9], [2.8, 3.0, 0.6], sw["sjaal"])]})
    geo["bones"].append({"name": "oma_zakje", "parent": "body", "pivot": [0, 6, -6], "cubes": [
        c([-2.2, 4.6, -7.8], [4.4, 4.2, 3.0], sw["zak"]), c([-1.8, 8.8, -7.4], [3.6, 0.4, 2.2], sw["kruim"])]})
    kn._save_geo(h, "guh_npc_omaguh.geo.json", geo_file)
    img = h.recolour(_src(h, "guh_sitting.png"), hue=0.80, sat=0.28, val=1.0, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(1420320)
    p = kn._paint
    p(a, sw["haar"], (240, 240, 246), rng, 6, pm._streep((220, 220, 232), 6, 1))
    p(a, sw["knot"], (246, 246, 250), rng, 6, pm._streep((222, 222, 234), 5, 1))
    p(a, sw["bril"], (206, 232, 244), rng, 3, pm._glas)
    p(a, sw["brilrand"], (60, 50, 60), rng, 3)
    p(a, sw["sjaal"], (236, 140, 180), rng, 8, pm._brei((250, 196, 220)))
    p(a, sw["sjaal_rand"], (255, 244, 250), rng, 5)
    p(a, sw["zak"], (214, 176, 120), rng, 8, pm._streep((190, 150, 96), 10, 1))
    p(a, sw["kruim"], (244, 206, 140), rng, 30)
    h.save(Image.fromarray(a), "entity", "npc_omaguh.png")


def mokmika(h):
    geo_file, geo = pm._mika(h, "guh_npc_mokmika")
    sw = kn._swatches(geo, ["wolk", "wolk_d", "drup", "brauw"])
    c = kn._cube
    geo["bones"].append({"name": "mok_wolk", "parent": "head", "pivot": [0, 18, -6], "cubes": [
        c([-4.5, 17.2, -9.0], [9.0, 2.6, 5.0], sw["wolk"]), c([-3.0, 19.6, -8.2], [6.0, 1.8, 3.6], sw["wolk"]),
        c([-4.0, 16.6, -8.6], [8.0, 0.8, 4.2], sw["wolk_d"]),
        c([-2.6, 14.6, -7.6], [0.5, 1.2, 0.5], sw["drup"]), c([1.4, 15.2, -6.6], [0.5, 1.0, 0.5], sw["drup"]),
        c([-0.4, 14.0, -8.2], [0.5, 1.1, 0.5], sw["drup"])]})
    brauw = []
    for x0, hoek in ((-6.0, -18), (2.2, 18)):
        cube = c([x0, 10.2, -12.4], [3.8, 0.8, 0.5], sw["brauw"])
        cube["pivot"] = [x0 + 1.9, 10.6, -12.2]
        cube["rotation"] = [0, 0, hoek]
        brauw.append(cube)
    geo["bones"].append({"name": "mok_brauwen", "parent": "head", "pivot": [0, 10, -12], "cubes": brauw})
    kn._save_geo(h, "guh_npc_mokmika.geo.json", geo_file)
    img = h.recolour(_src(h, "mika.png"), hue=0.62, sat=0.30, val=0.92, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(1420321)
    p = kn._paint
    p(a, sw["wolk"], (176, 182, 196), rng, 10)
    p(a, sw["wolk_d"], (128, 134, 152), rng, 8)
    p(a, sw["drup"], (120, 180, 250), rng, 6)
    p(a, sw["brauw"], (40, 34, 50), rng, 3)
    h.save(Image.fromarray(a), "entity", "npc_mokmika.png")
    anims = pm._mika_anims(schommel=False)
    idle = anims["animations"]["animation.guh_sitting.idle"]["bones"]
    idle["head"] = {"rotation": {"0.0": [10, 0, 0], "1.5": [12, 8, 0], "2.0": [10, 0, 0], "3.5": [12, -8, 0], "4.0": [10, 0, 0]}}
    idle["mok_wolk"] = {"position": {"0.0": [0, 0, 0], "1.0": [0, 0.6, 0], "2.0": [0, 0, 0], "3.0": [0, 0.6, 0], "4.0": [0, 0, 0]}}
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "guh_npc_mokmika.animation.json"), anims)


# =====================================================================================================================
# blocks and items
# =====================================================================================================================
def _el(frm, to, tex, rot=None):
    return parkm._el(frm, to, tex, rot=rot)


def textures(h):
    pal = {".": (0, 0, 0, 0), "k": (40, 30, 44, 255), "w": (255, 255, 255, 255), "p": (246, 166, 200, 255), "P": (222, 120, 166, 255),
           "b": (255, 140, 170, 255), "m": (150, 96, 170, 255), "M": (112, 66, 132, 255), "r": (210, 50, 70, 255), "y": (250, 214, 90, 255)}
    # the puppets' faces: the guh (pink, round eyes, a blush) and the Mika (lilac, cross eyebrows, a cheeky grin)
    parkm._tex(h, "park_pop_guh", [
        "pppppppppppppppp", "pppppppppppppppp", "pppppppppppppppp", "pppkkpppppppkkpp", "ppkwkkpppppkwkkp", "ppkkkkpppppkkkkp",
        "pppkkpppppppkkpp", "pbbppppppppppbbp", "pbbpppppkpppppbp", "pppppppkpkpppppp", "pppppppppppppppp", "pppppppppppppppp",
        "pppppppppppppppp", "PPPPPPPPPPPPPPPP", "PPPPPPPPPPPPPPPP", "PPPPPPPPPPPPPPPP"], pal)
    parkm._tex(h, "park_pop_mika", [
        "mmmmmmmmmmmmmmmm", "mmmmmmmmmmmmmmmm", "mkkkmmmmmmmmkkkm", "mmmkkmmmmmmkkmmm", "mmmkkmmmmmmkkmmm", "mmkwkkmmmmkkwkmm",
        "mmkkkkmmmmkkkkmm", "mmmmmmmmmmmmmmmm", "mmmmkmmmmmmkmmmm", "mmmmmkkkkkkmmmmm", "mmmmmmwmmwmmmmmm", "mmmmmmmmmmmmmmmm",
        "mmmmmmmmmmmmmmmm", "MMMMMMMMMMMMMMMM", "MMMMMMMMMMMMMMMM", "MMMMMMMMMMMMMMMM"], pal)
    # the wish tree's bark: the guhbloesem log with ribbons tied round it
    stam = Image.open(os.path.join(h.TEX, "block", "guhbloesem_log.png")).convert("RGBA")
    a = np.asarray(stam).copy()
    for y0, kleur in ((3, (246, 120, 170)), (9, (120, 190, 250)), (13, (250, 214, 90))):
        a[y0:y0 + 2, :, :3] = kleur
        a[y0:y0 + 2, ::4, :3] = np.array(kleur) * 0.75
    a[4:9, 7:9, :3] = (246, 120, 170)          # a dangling end of the pink ribbon
    h.save(Image.fromarray(a), "block", "park_wensstam.png")
    # the crumbs: a little paper bag with crumbs falling out
    rij = ["................", "................", "....yyy.........", "...yy.yy..y.....", "....PPPPPP...y..", "...PppppppP.....",
           "...PppppppP..y..", "...PpprppP......", "...PppprppP.....", "...PppppppP.y...", "...PppppppP.....", "...PppppppP.....",
           "...PPPPPPPP.....", "................", "................", "................"]
    zak = {".": (0, 0, 0, 0), "y": (244, 206, 140, 255), "P": (170, 128, 80, 255), "p": (214, 176, 120, 255), "r": (236, 110, 150, 255)}
    h.save(h.grid(rij, zak), "item", "park_kruimels.png")


def models(h):
    A, w = h.A, h.w
    # the verrekijker (looking north): three tripod legs, a tilted pink tube with golden rings, an eyepiece at the back
    schuin = {"origin": [8, 12, 8], "axis": "x", "angle": 22.5}
    kijker = [
        _el([7.5, 0, 3], [8.5, 11, 4], "#poot", rot={"origin": [8, 11, 8], "axis": "x", "angle": -22.5}),
        _el([3, 0, 11], [4, 11, 12], "#poot", rot={"origin": [8, 11, 8], "axis": "z", "angle": 22.5}),
        _el([12, 0, 11], [13, 11, 12], "#poot", rot={"origin": [8, 11, 8], "axis": "z", "angle": -22.5}),
        _el([7, 10, 7], [9, 12, 9], "#messing"),
        _el([6, 11, 1], [10, 15, 13], "#buis", rot=schuin),
        _el([5.5, 10.5, 0], [10.5, 15.5, 2], "#messing", rot=schuin),
        _el([7, 12, 13], [9, 14, 16], "#messing", rot=schuin),
    ]
    w(f"{A}/models/block/park_verrekijker.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "minecraft:block/pink_concrete", "poot": "minecraft:block/stripped_cherry_log", "buis": "minecraft:block/pink_concrete",
        "messing": "minecraft:block/gold_block"}, "elements": kijker, "display": parkm.DISPLAY})
    w(f"{A}/blockstates/park_verrekijker.json", {"variants": h.facing_states("park_verrekijker")})
    w(f"{A}/models/item/park_verrekijker.json", {"parent": "guhs:block/park_verrekijker"})
    # the stage (front = north: the side the audience sees): a counter, the frame, curtains; open: the curtains to the sides
    # and the two puppets
    rand = [_el([0, 0, 0], [16, 3, 16], "#hout"), _el([0, 3, 12], [16, 16, 16], "#hout"), _el([0, 14, 0], [16, 16, 12], "#geel")]
    dicht = rand + [_el([0.5, 3, 1], [8, 14, 2], "#doek"), _el([8, 3, 1], [15.5, 14, 2], "#doek")]
    pop_guh = [_el([2.5, 3, 5], [7.5, 9, 9], "#lijf"), _el([2, 9, 4.5], [8, 14, 9.5], "#guh"),
               _el([1.5, 14, 6], [3.5, 16, 7], "#lijf"), _el([6.5, 14, 6], [8.5, 16, 7], "#lijf")]
    pop_mika = [_el([9, 3, 6], [14, 8, 10], "#mlijf"), _el([8.5, 8, 5.5], [14.5, 13, 10.5], "#mika"),
                _el([8, 13, 7], [10, 15, 8], "#mlijf"), _el([13, 13, 7], [15, 15, 8], "#mlijf")]
    for e in pop_guh[1:2] + pop_mika[1:2]:
        for f in ("east", "west", "south", "up", "down"):
            e["faces"][f]["texture"] = "#lijf" if e in pop_guh else "#mlijf"
    open_ = rand + [_el([0.5, 3, 1], [2.5, 14, 2], "#doek"), _el([13.5, 3, 1], [15.5, 14, 2], "#doek")] + pop_guh + pop_mika
    tex = {"particle": "minecraft:block/red_wool", "hout": "minecraft:block/spruce_planks", "geel": "minecraft:block/yellow_wool",
           "doek": "minecraft:block/red_wool", "lijf": "minecraft:block/pink_wool", "guh": "guhs:block/park_pop_guh",
           "mlijf": "minecraft:block/purple_wool", "mika": "guhs:block/park_pop_mika"}
    for naam, el in (("park_poppenkast", dicht), ("park_poppenkast_open", open_)):
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": tex, "elements": el, "display": parkm.DISPLAY})
    w(f"{A}/blockstates/park_poppenkast.json", {"variants": {
        f"facing={f},open={o}": ({"model": "guhs:block/park_poppenkast" + ("_open" if o == "true" else ""), **({"y": y} if y else {})})
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for o in ("false", "true")}})
    w(f"{A}/models/item/park_poppenkast.json", {"parent": "guhs:block/park_poppenkast"})
    # the wish tree's trunk
    w(f"{A}/models/block/park_wensstam.json", {"parent": "minecraft:block/cube_column", "textures": {
        "side": "guhs:block/park_wensstam", "end": "guhs:block/guhbloesem_log_top"}})
    w(f"{A}/blockstates/park_wensstam.json", {"variants": {"": {"model": "guhs:block/park_wensstam"}}})
    w(f"{A}/models/item/park_wensstam.json", {"parent": "guhs:block/park_wensstam"})
    for b in ("park_verrekijker", "park_poppenkast", "park_wensstam"):
        h.self_drop(b)
    h.item_model("park_kruimels")


def spawn_egg(h):
    import mc26
    name = "duifguhtje_spawn_egg"
    egg = Image.alpha_composite(mc26.tint(mc26.vanilla_121("item/spawn_egg"), 0x9AA0BE), mc26.tint(mc26.vanilla_121("item/spawn_egg_overlay"), 0x54BE8C))
    h.save(egg, "item", f"{name}.png")
    h.item_model(name)


def build(h):
    bones, anims = duifguhtje(h)
    names = {b["name"] for b in bones}
    need = {"idle", "hop", "fly", "peck", "koer"} - set(anims)
    if need or not {"head", "wing_left", "wing_right", "beak"} <= names:
        raise SystemExit(f"park_plek_b: the Duifguhtje misses {need or 'bones'}")
    omaguh(h)
    mokmika(h)
    textures(h)
    models(h)
    spawn_egg(h)
