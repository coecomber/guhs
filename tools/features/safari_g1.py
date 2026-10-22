"""
Op Safari! (1.6.0), slice G1 - the gaps of the design audit (Java: feature/safari/extra/ and the classes named there).

  build(h)      the texts (NL here, English in tools/lang/en/c186_safari_g1.json); the safarihek and rotswanden (textures,
                models, blockstates, loot, recipes, tags); the Aussie guhs' skins; the night-eyes textures of the safari
                animals (<texture>_nachtogen.png: a soft glint per eye (test round 5: no glowing pupils); the client draws it in the dark); the
                painting variants of the Kriebelhuis painting and of every species' photo painting (safari_foto_<soort>).
  python tools/features/safari_g1.py --direct
                writes only this slice's files (no full make_resources), from the project root. Add --fotos to (re)render
                the photo paintings (tools/features/safari_schilderijen.py, a minute or so).
"""
import json
import os
import sys

import numpy as np
from PIL import Image

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data", "guhs")
SAFARI_TEX = os.path.join(A, "textures", "entity", "safari")
SOORTEN = os.path.join(D, "safari", "soorten.json")
EN_BESTAND = os.path.join("tools", "lang", "en", "c186_safari_g1.json")

# --- texts --------------------------------------------------------------------------------------------------------------
TEKSTEN = {
    # own guhs talk about the animals they see (GuhReacties.lijn): %1$s the guh, %2$s the animal in a sentence
    "gui.guhs.safari.guhziet.eng.0": "%1$s: Njeg! Een %2$s! Hou me vast!",
    "gui.guhs.safari.guhziet.eng.1": "%1$s: Eeeh... die %2$s kijkt naar mij.",
    "gui.guhs.safari.guhziet.eng.2": "%1$s: Ik ben helemaal niet bang voor die %2$s. Een heel klein beetje maar.",
    "gui.guhs.safari.guhziet.zwaai.0": "%1$s: Hoi %2$s! *zwaait*",
    "gui.guhs.safari.guhziet.zwaai.1": "%1$s: Kijk, een %2$s! Zó schattig!",
    "gui.guhs.safari.guhziet.zwaai.2": "%1$s: Daaag %2$s! Zwaai je terug?",
    "gui.guhs.safari.guhziet.groot.0": "%1$s: Njeg, een %2$s! Die is nog vahoeger dan ik!",
    "gui.guhs.safari.guhziet.groot.1": "%1$s: Wauw, wat een grote %2$s!",
    "gui.guhs.safari.guhziet.groot.2": "%1$s: Een %2$s! Mag ik erop? Nee? Oké.",
    "gui.guhs.safari.guhziet.baby.0": "%1$s: Een kleine %2$s! Ik smelt...",
    "gui.guhs.safari.guhziet.baby.1": "%1$s: Kijk dan, een baby-%2$s! Mag die mee naar huis?",
    "gui.guhs.safari.guhziet.baby.2": "%1$s: Aaaaw, een kleintje! Een %2$s!",
    "gui.guhs.safari.guhziet.zie.0": "%1$s: Njeg, een %2$s!",
    "gui.guhs.safari.guhziet.zie.1": "%1$s: Kijk daar, een %2$s!",
    "gui.guhs.safari.guhziet.zie.2": "%1$s: Ooh, een %2$s! Maak een foto!",
    "gui.guhs.safari.guhziet.rit.0": "%1$s: Joehoe, ik rij op een %2$s!",
    "gui.guhs.safari.guhziet.rit.1": "%1$s: Deze %2$s is mijn nieuwe taxi.",
    "gui.guhs.safari.guhziet.rit.2": "%1$s: Rustig aan, %2$s, ik zit er nog op!",
    # night photos and the golden camera
    "gui.guhs.safari.foto.nacht": " in het donker, met gloeiende oogjes",
    "item.guhs.safari_foto.goud": "In een gouden lijstje",
    # the photo printed as a painting
    "gui.guhs.safari.dex.schilderij": "Schilderij",
    "gui.guhs.safari.dex.schilderij.tip": "Je beste foto als schilderij om op te hangen (2 vellen papier).",
    "gui.guhs.safari.schilderij.papier": "Je hebt %s vellen papier nodig voor een schilderij.",
    "gui.guhs.safari.schilderij.klaar": "Een schilderij van je %s! Hang hem mooi op.",
    "painting.guhs.safari_foto.title": "Mijn safarifoto: %s",
    "painting.guhs.safari_foto.author": "Een foto uit Safaripark Vadse Bergen",
    # the giftshop: requirements and the toys
    "gui.guhs.safari.winkel.eis.dex": "Fotografeer eerst %s diersoorten (je hebt er %s).",
    "gui.guhs.safari.winkel.eis.zone": "Maak eerst de dex van %s compleet.",
    "gui.guhs.safari.winkel.groep.speelgoed": "Dierenspeelgoed",
    "gui.guhs.safari.winkel.speelgoed": "Voor je thuisdieren: ze worden er blij van.",
    "gui.guhs.safari.winkel.rieten_dak_bulk": "Rieten dak (grote bos)",
    # the new safari blocks
    "block.guhs.safarihek": "Safarihek",
    "block.guhs.rotswand": "Rotswand",
    "block.guhs.rotswand_trap": "Rotswandtrap",
    "block.guhs.rotswand_muur": "Rotsmuurtje",
    # the Aussie guhs (design 1.4): Bazza in the hammock, Shazza at the post office, Davo at the camp
    "entity.guhs.guh_npc.safari_aussie_hangmat": "Hangmatguh Bazza",
    "entity.guhs.guh_npc.safari_aussie_post": "Postguh Shazza",
    "entity.guhs.guh_npc.safari_aussie_kamp": "Kampvuurguh Davo",
    "gui.guhs.safari.aussie.optie": "Hooroo!",
    "gui.guhs.safari.aussie.hangmat.0": "Zzz... huh? O, g'day maatje! Niks aan de hand, ik test deze hangmat. Al drie weken. Beauty hangmat.",
    "gui.guhs.safari.aussie.hangmat.1": "No worries, maatje. De koala's slapen twintig uur per dag. Ik doe er eenentwintig. Ik win.",
    "gui.guhs.safari.aussie.hangmat.2": "Strewth, is het al middag? Wek me maar als de kangoeroes iets spannends doen. Zzz...",
    "gui.guhs.safari.aussie.post.0": "G'day! Een kaartje versturen? Komt goed, maatje. De vorige is trouwens nooit aangekomen... die ligt vast nog ergens in de buurt.",
    "gui.guhs.safari.aussie.post.1": "Post voor de wombats? Nee hoor, die graven gewoon een tunnel naar de buren. Lekker makkelijk.",
    "gui.guhs.safari.aussie.post.2": "Het is een rustige dag, maatje. Het is altijd een rustige dag. Fair dinkum.",
    "gui.guhs.safari.aussie.kamp.0": "G'day! Ga lekker zitten bij het vuurtje. 's Avonds scharrelen hier de wombats rond, als je stil bent.",
    "gui.guhs.safari.aussie.kamp.1": "Weet je wat een billabong is, maatje? Een vijvertje waar de kangoeroes komen drinken. Daar bij de windpomp!",
    "gui.guhs.safari.aussie.kamp.2": "Crikey, wat een sterren vannacht. In de outback zie je ze allemaal. Ook die ene die op een guh lijkt.",
}


def _w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _tag(rel, waarden):
    """Adds values to a (vanilla or guhs) tag file under src/main/resources/data/<rel>.json, keeping what is there."""
    path = os.path.join(R, "data", rel + ".json")
    d = {"replace": False, "values": []}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
    for v in waarden:
        if v not in d["values"]:
            d["values"].append(v)
    _w(path, d)


# --- block textures -----------------------------------------------------------------------------------------------------
def _ruis(rng, kleur, var=10, size=16):
    a = np.zeros((size, size, 4), np.float32)
    a[..., :3] = np.array(kleur, np.float32) + rng.normal(0, var, (size, size, 1))
    a[..., 3] = 255
    return a


def tex_safarihek(rng):
    """Dark wood with vertical grain and a light mesh of little crosses (the park fence: dark posts with gaas)."""
    a = _ruis(rng, (70, 48, 34), 6)
    for x in range(16):
        a[:, x, :3] *= 0.9 + 0.1 * ((x * 7) % 5) / 4
    for y in range(1, 16, 4):
        for x in range(1, 16, 4):
            a[y, x, :3] = (150, 132, 104)
            a[y + 1 if y < 15 else y, x + 1 if x < 15 else x, :3] = (150, 132, 104)
    return a


def tex_rotswand(rng):
    """Warm orange-brown rock with darker cracks and a few pale spots (the park's rock walls)."""
    a = _ruis(rng, (176, 112, 74), 9)
    for _ in range(5):
        x, y = rng.integers(0, 16, 2)
        for _ in range(int(rng.integers(3, 7))):
            a[y % 16, x % 16, :3] = (112, 70, 48)
            x += int(rng.integers(-1, 2))
            y += 1
    for _ in range(6):
        x, y = rng.integers(0, 16, 2)
        a[y, x, :3] = (214, 170, 130)
    return a


def _png(a, *rel):
    path = os.path.join(A, "textures", *rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(path)


def _item(name, model=None):
    _w(os.path.join(A, "items", f"{name}.json"), {"model": {"type": "minecraft:model", "model": model or f"guhs:block/{name}"}})


def _drop(name):
    _w(os.path.join(D, "loot_table", "blocks", f"{name}.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{name}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def blokken():
    rng = np.random.default_rng(186)
    _png(tex_safarihek(rng), "block", "safarihek.png")
    _png(tex_rotswand(rng), "block", "rotswand.png")
    M = os.path.join(A, "models", "block")
    BS = os.path.join(A, "blockstates")
    # safarihek: a fence (vanilla templates)
    t = {"texture": "guhs:block/safarihek"}
    _w(os.path.join(M, "safarihek_post.json"), {"parent": "minecraft:block/fence_post", "textures": t})
    _w(os.path.join(M, "safarihek_side.json"), {"parent": "minecraft:block/fence_side", "textures": t})
    _w(os.path.join(M, "safarihek_inventory.json"), {"parent": "minecraft:block/fence_inventory", "textures": t})
    parts = [{"apply": {"model": "guhs:block/safarihek_post"}}]
    for richting, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        app = {"model": "guhs:block/safarihek_side", "uvlock": True}
        if y:
            app["y"] = y
        parts.append({"apply": app, "when": {richting: "true"}})
    _w(os.path.join(BS, "safarihek.json"), {"multipart": parts})
    _item("safarihek", "guhs:block/safarihek_inventory")
    # rotswand: a block, stairs and a wall
    _w(os.path.join(M, "rotswand.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/rotswand"}})
    _w(os.path.join(BS, "rotswand.json"), {"variants": {"": [{"model": "guhs:block/rotswand"}, {"model": "guhs:block/rotswand", "y": 90},
                                                            {"model": "guhs:block/rotswand", "y": 180}, {"model": "guhs:block/rotswand", "y": 270}]}})
    _item("rotswand")
    st = {"bottom": "guhs:block/rotswand", "top": "guhs:block/rotswand", "side": "guhs:block/rotswand"}
    for naam, parent in (("rotswand_trap", "stairs"), ("rotswand_trap_inner", "inner_stairs"), ("rotswand_trap_outer", "outer_stairs")):
        _w(os.path.join(M, f"{naam}.json"), {"parent": f"minecraft:block/{parent}", "textures": st})
    _w(os.path.join(BS, "rotswand_trap.json"), {"variants": _trap_varianten("guhs:block/rotswand_trap")})
    _item("rotswand_trap")
    w = {"wall": "guhs:block/rotswand"}
    for naam, parent in (("rotswand_muur_post", "template_wall_post"), ("rotswand_muur_side", "template_wall_side"),
                         ("rotswand_muur_side_tall", "template_wall_side_tall"), ("rotswand_muur_inventory", "wall_inventory")):
        _w(os.path.join(M, f"{naam}.json"), {"parent": f"minecraft:block/{parent}", "textures": w})
    parts = [{"apply": {"model": "guhs:block/rotswand_muur_post"}, "when": {"up": "true"}}]
    for richting, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for hoogte, model in (("low", "rotswand_muur_side"), ("tall", "rotswand_muur_side_tall")):
            app = {"model": f"guhs:block/{model}", "uvlock": True}
            if y:
                app["y"] = y
            parts.append({"apply": app, "when": {richting: hoogte}})
    _w(os.path.join(BS, "rotswand_muur.json"), {"multipart": parts})
    _item("rotswand_muur", "guhs:block/rotswand_muur_inventory")
    for n in ("safarihek", "rotswand", "rotswand_trap", "rotswand_muur"):
        _drop(n)
    # recipes: a safarihek from dark oak + iron nuggets (the mesh); rotswand from terracotta + cobblestone
    R_ = os.path.join(D, "recipe")
    _w(os.path.join(R_, "safarihek.json"), {"type": "minecraft:crafting_shaped", "category": "building",
                                             "pattern": ["LNL", "LNL"], "key": {"L": "minecraft:dark_oak_log", "N": "minecraft:iron_nugget"},
                                             "result": {"id": "guhs:safarihek", "count": 6}})
    _w(os.path.join(R_, "rotswand.json"), {"type": "minecraft:crafting_shaped", "category": "building",
                                            "pattern": ["TC", "CT"], "key": {"T": "minecraft:orange_terracotta", "C": "minecraft:cobblestone"},
                                            "result": {"id": "guhs:rotswand", "count": 4}})
    _w(os.path.join(R_, "rotswand_trap.json"), {"type": "minecraft:crafting_shaped", "category": "building",
                                                 "pattern": ["R  ", "RR ", "RRR"], "key": {"R": "guhs:rotswand"},
                                                 "result": {"id": "guhs:rotswand_trap", "count": 4}})
    _w(os.path.join(R_, "rotswand_muur.json"), {"type": "minecraft:crafting_shaped", "category": "building",
                                                 "pattern": ["RRR", "RRR"], "key": {"R": "guhs:rotswand"},
                                                 "result": {"id": "guhs:rotswand_muur", "count": 6}})
    for n, uit in (("rotswand_trap", 1), ("rotswand_muur", 1)):
        _w(os.path.join(R_, f"{n}_steenhouwen.json"), {"type": "minecraft:stonecutting", "ingredient": "guhs:rotswand",
                                                        "result": {"id": f"guhs:{n}", "count": uit}})
    _tag("minecraft/tags/block/fences", ["guhs:safarihek"])
    _tag("minecraft/tags/block/wooden_fences", ["guhs:safarihek"])
    _tag("minecraft/tags/item/wooden_fences", ["guhs:safarihek"])
    _tag("minecraft/tags/block/mineable/axe", ["guhs:safarihek"])
    _tag("minecraft/tags/block/walls", ["guhs:rotswand_muur"])
    _tag("minecraft/tags/item/walls", ["guhs:rotswand_muur"])
    _tag("minecraft/tags/block/stairs", ["guhs:rotswand_trap"])
    _tag("minecraft/tags/item/stairs", ["guhs:rotswand_trap"])
    _tag("minecraft/tags/block/mineable/pickaxe", ["guhs:rotswand", "guhs:rotswand_trap", "guhs:rotswand_muur"])


def _trap_varianten(m):
    """Vanilla stair blockstate variants (facing x half x shape) for the models m, m_inner, m_outer."""
    uit = {}
    yaw = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y0 in yaw.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = m if shape == "straight" else m + ("_inner" if shape.startswith("inner") else "_outer")
                y = y0
                if shape in ("inner_left", "outer_left"):
                    y = (y0 + 270) % 360 if half == "bottom" else y0
                elif half == "top" and shape in ("inner_right", "outer_right"):
                    y = (y0 + 90) % 360
                v = {"model": model}
                if half == "top":
                    v["x"] = 180
                if y:
                    v["y"] = y
                if half == "top" or y or shape != "straight":
                    v["uvlock"] = True
                uit[f"facing={facing},half={half},shape={shape}"] = v
    return uit


# --- the Aussie guhs' skins ---------------------------------------------------------------------------------------------
AUSSIES = {"safari_aussie_hangmat": (0.09, 0.45, 0.80), "safari_aussie_post": (0.06, 0.55, 0.85), "safari_aussie_kamp": (0.10, 0.40, 0.70)}


def aussies(recolour, pinkish):
    """The three Aussie guhs: the sitting guh recoloured, with (test round 5) a cork hat of their own: a wide brown brim,
    a low crown with a band and little corks dangling on strings all round (a model of its own per kind, registered in
    SafariExtraClient). Bazza sleeps in his hammock (eyes shut)."""
    from features import bio_bouw_meer
    from features import beroepen_tex as bt

    class H:
        TEX = os.path.join(A, "textures")

        @staticmethod
        def w(path, obj):
            _w(path, obj)

        @staticmethod
        def save(img, *path):
            img.save(os.path.join(H.TEX, *path))

    H.A = A
    H.recolour = staticmethod(recolour)
    H.pinkish = staticmethod(pinkish)
    HOOFD = ("head", [0, 13, 0])
    kurken = []
    for x in (-7.5, -4, -0.5, 3, 6.5):                          # in front, and a few at the back and the sides
        kurken.append(([x, 24.2, -8.6], [0.25, 2.0, 0.25], "touwtje", 0))
        kurken.append(([x - 0.4, 23.0, -8.8], [1.0, 1.4, 0.8], "kurk", 0))
    for z in (-4.5, 0, 4.5):
        for x in (-9.4, 8.6):
            kurken.append(([x + 0.4, 24.2, z], [0.25, 2.0, 0.25], "touwtje", 0))
            kurken.append(([x, 23.0, z - 0.4], [0.8, 1.4, 1.0], "kurk", 0))
    for kind, (hue, sat, val) in AUSSIES.items():
        bt._npc(H, kind, hue, sat, val, {
            "kurkenhoed": (HOOFD, [([-9.5, 26.0, -9.0], [19.0, 0.8, 17.0], "rand", 0), ([-5.5, 26.8, -5.5], [11.0, 3.6, 10.0], "bol", 0),
                                   ([-5.6, 26.8, -5.6], [11.2, 1.0, 10.2], "band", 0)]),
            "kurken": (HOOFD, kurken),
        }, {"rand": ((150, 104, 62), 5, None), "bol": ((160, 112, 68), 5, None), "band": ((96, 60, 36), 3, None),
            "touwtje": ((70, 52, 40), 2, None), "kurk": ((214, 176, 124), 7, None)}, 1606 + len(kind))
        if kind.endswith("hangmat"):                             # Bazza is asleep in his hammock (eyes shut)
            pad = os.path.join(A, "textures", "entity", f"npc_{kind}.png")
            img = Image.open(pad).convert("RGBA")
            Image.fromarray(bio_bouw_meer._ogen_dicht(np.array(img)), "RGBA").save(pad)


# --- night eyes ---------------------------------------------------------------------------------------------------------
def nachtogen():
    """For every safari texture with a _slaap twin: the pixels where the two differ are the eyes. Test round 5: no glowing
    green pupils any more (they took the cuteness away); each eye only gets a soft white glint, its own highlight pixels
    (or, without one, its top-left pixel), as if a lantern shines in it. Everything else is see-through. The client draws
    the texture additively in the dark, so a soft grey-white is a gentle twinkle."""
    n = 0
    for f in sorted(os.listdir(SAFARI_TEX)):
        if not f.endswith(".png") or f.endswith(("_slaap.png", "_glowmask.png", "_nachtogen.png")):
            continue
        stam = f[:-4]
        slaap = os.path.join(SAFARI_TEX, stam + "_slaap.png")
        if not os.path.exists(slaap):
            continue
        wakker = np.array(Image.open(os.path.join(SAFARI_TEX, f)).convert("RGBA")).astype(np.int32)
        dicht = np.array(Image.open(slaap).convert("RGBA")).astype(np.int32)
        if wakker.shape != dicht.shape:
            continue
        verschil = (np.abs(wakker - dicht).sum(axis=2) > 24) & (wakker[..., 3] > 0)
        if not verschil.any():
            continue
        lum = wakker[..., :3].mean(axis=2)
        uit = np.zeros(wakker.shape, np.uint8)
        h, w = verschil.shape
        gezien = np.zeros_like(verschil)
        for y0 in range(h):
            for x0 in range(w):
                if not verschil[y0, x0] or gezien[y0, x0]:
                    continue
                oog, stapel = [], [(y0, x0)]          # one eye: the touching changed pixels
                gezien[y0, x0] = True
                while stapel:
                    y, x = stapel.pop()
                    oog.append((y, x))
                    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        ny, nx = y + dy, x + dx
                        if 0 <= ny < h and 0 <= nx < w and verschil[ny, nx] and not gezien[ny, nx]:
                            gezien[ny, nx] = True
                            stapel.append((ny, nx))
                licht = [(y, x) for y, x in oog if lum[y, x] >= 170]
                for y, x in (licht or [min(oog)]):
                    uit[y, x] = (150, 150, 140, 255)
        Image.fromarray(uit, "RGBA").save(os.path.join(SAFARI_TEX, stam + "_nachtogen.png"))
        n += 1
    return n


# --- paintings ----------------------------------------------------------------------------------------------------------
def schilderijen(foto_render=False):
    from features import safari_schilderijen as sch
    with open(SOORTEN, encoding="utf-8") as f:
        soorten = {s: v.get("zone") for s, v in json.load(f).items() if not v.get("decor")}
    if foto_render:
        sch.fotos(soorten)
    titels = {}
    # the Kriebelhuis painting (the ninth badge; also sold in the giftshop)
    bw, bh, nl, _en, *_r = sch.SCHILDERIJEN["kriebelhuis"]
    _w(os.path.join(D, "painting_variant", "safari_kriebelhuis.json"), {
        "asset_id": "guhs:safari_kriebelhuis", "width": bw, "height": bh,
        "title": {"translate": "painting.guhs.safari_kriebelhuis.title", "color": "yellow"},
        "author": {"translate": "painting.guhs.safari.author", "color": "gray"}})
    titels["painting.guhs.safari_kriebelhuis.title"] = f"{nl} ({bw}×{bh})"
    _tag("minecraft/tags/painting_variant/placeable", ["guhs:safari_kriebelhuis"])
    # the photo paintings: not placeable at random (only from the Guhdex's print button)
    n = 0
    for s in soorten:
        if not os.path.exists(os.path.join(A, "textures", "painting", f"safari_foto_{s}.png")):
            continue
        _w(os.path.join(D, "painting_variant", f"safari_foto_{s}.json"), {
            "asset_id": f"guhs:safari_foto_{s}", "width": 1, "height": 1,
            "title": {"translate": "painting.guhs.safari_foto.title", "with": [{"translate": f"entity.guhs.safari.{s}"}], "color": "yellow"},
            "author": {"translate": "painting.guhs.safari_foto.author", "color": "gray"}})
        n += 1
    return titels, n


def build(h):
    for k, v in TEKSTEN.items():
        h.NL[k] = v
    blokken()
    aussies(h.recolour, h.pinkish)
    nachtogen()
    titels, _ = schilderijen()
    h.NL.update(titels)


def _direct(fotos=False):
    sys.path.insert(0, "tools")
    import make_v2 as v2
    nl = dict(TEKSTEN)
    blokken()
    aussies(v2.recolour, v2.pinkish)
    n_ogen = nachtogen()
    titels, n_foto = schilderijen(fotos)
    nl.update(titels)
    with open(EN_BESTAND, encoding="utf-8") as f:
        en = json.load(f)
    for taal, extra in (("nl_nl", nl), ("en_us", {k: en.get(k, v) for k, v in nl.items()})):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(extra)
        _w(p, d)
    missing = [k for k in nl if k not in en]
    print(f"safari_g1: {len(nl)} texts ({len(missing)} without English), {n_ogen} night-eye textures, {n_foto} photo paintings")


if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct(fotos="--fotos" in sys.argv)
