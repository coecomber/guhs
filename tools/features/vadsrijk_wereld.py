"""
1.10.0 "Het Vadsrijk", the wereld slice (CONTRACT_VADSRIJK.md). Java: feature/vadsrijk/wereld.

  - blocks/items: vr_w_portaal (sleeping/awake, animated), vr_w_poortsteen + vr_w_vonk (the craftable portal, recipes),
    vr_w_litteken_gras / vr_w_litteken_plant (the grey dragon scars; tinted per player on the client);
  - the sky textures (second moon, the far dragon, four far landmarks);
  - the Guhmension structure guh:vr_w_portaaltempel (structure + set + pool + biome tag; the .nbt itself is written by
    tools/vadsrijk/maak_kaart.py, together with the map tiles);
  - every wereld text (Dutch; en_us gets the same Dutch).

The map itself: tools/vadsrijk/maak_kaart.py (tiles in data/guhs/structure/vadsrijk/tegels, WereldPlekken.java, plekken).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_wereld.py
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

A = os.path.join("src", "main", "resources")
AS = os.path.join(A, "assets", "guhs")
DA = os.path.join(A, "data", "guhs")
W = "vadsrijk.wereld."

TEKSTEN = {
    "block.guhs.vr_w_portaal": "Vadsrijk-portaal",
    "block.guhs.vr_w_poortsteen": "Vadsrijk-poortsteen",
    "block.guhs.vr_w_litteken_gras": "Grauw litteken",
    "block.guhs.vr_w_litteken_plant": "Grauw bloempje",
    "item.guhs.vr_w_vonk": "Vadsrijk-vonk",
    # y-tempel: the Superkompas entry (tab Avontuur) for the ordetempel (the fixed one first, else the nearest worldgen one)
    "structure.guhs.vr_w_portaaltempel": "Het Vadsrijk",
    "structure.guhs.vr_w_portaaltempel.tooltip": "De oude ordetempel met het slapende portaal naar Het Vadsrijk. Praat eerst met Werver Wimpel!",
    W + "portaal.eerst_zelf": "Njeg! Het portaal herkent je nog niet. Vind eerst zelf de oude ordetempel in de Guhmension.",
    W + "portaal.geen_raam": "Dat is geen dicht raam van Vadsrijk-poortsteen. Bouw een rechthoek (binnen max. 21 bij 21).",
    W + "portaal.aan": "Het portaal ontwaakt! Vahoeg, op naar het Vadsrijk.",
    W + "scene.ontwaken.titel": "Het Vadsrijk ontwaakt",
    W + "scene.ontwaken.onder": "Ver weg brult iets groots...",
    W + "deur.titel.ordetempel": "De Knuffeldeur",
    W + "deur.onder.ordetempel": "Tempel van De Knuffelorde",
    W + "deur.titel.zwamtempel": "De Zwamtempel",
    W + "deur.onder.zwamtempel": "Kerker van zone 1 - lv 18-20",
    W + "deur.titel.zwamtempel_binnen": "De Zaal van de Opperzwam",
    W + "deur.onder.zwamtempel_binnen": "Wie het licht brengt, mag naar binnen",
    W + "deur.dicht.ordetempel": "De Knuffeldeur blijft dicht. Gebruik de zijdeur, njeg!",
    W + "deur.dicht.zwamtempel": "De tempeldeur zit potdicht. Misschien weet iemand in Zwamzoet hoe hij opengaat.",
    W + "deur.dicht.zwamtempel_binnen": "De binnendeur luistert niet. Los eerst de puzzels op!",
    W + "deur.vast": "Deze deur zit vast zolang hier gevochten wordt.",
    W + "luchtschip.titel.kamp": "Knuffelkamp",
    W + "luchtschip.titel.haven": "De Haven",
    W + "luchtschip.onder": "Met de guhballon-lijn. Vahoeg!",
    W + "luchtschip.nog_niet": "De kapitein doet nog een dutje. Kom straks terug, njeg.",
    W + "luchtschip.overslaan": "Sluipen = de rondvlucht overslaan",
    W + "uitkijk.klodderkijk": "Klodderkijk",
    W + "uitkijk.zwamnaald": "Zwamnaald",
    W + "uitkijk.druipspits": "Druipspits",
    W + "uitkijk.haventand": "Haventand",
    W + "uitkijk.slijmnaald": "Slijmnaald",
    W + "uitkijk.onder": "Uitkijkpunt ontdekt",
    W + "uitkijk.gevonden": "Uitkijkpunt gevonden: %s. Wat een vads uitzicht!",
}

PRIJS = {
    "vr_w_poortsteen": {"type": "minecraft:crafting_shaped", "category": "building",
                        "pattern": ["RAR", "AEA", "RAR"],
                        "key": {"R": "guhs:roze_knuffelsteen", "A": "minecraft:amethyst_shard", "E": "minecraft:ender_pearl"},
                        "result": {"id": "guhs:vr_w_poortsteen", "count": 8}},
    "vr_w_vonk": {"type": "minecraft:crafting_shapeless", "category": "misc",
                  "ingredients": ["minecraft:amethyst_shard", "minecraft:pink_dye", "minecraft:glowstone_dust", "minecraft:flint"],
                  "result": {"id": "guhs:vr_w_vonk", "count": 2}},
}

GUHMENSION = ["guhs:guh_fields", "guhs:knabbel_crumbs", "guhs:pink_puffs", "guhs:kaas_flats", "guhs:guh_meadows", "guhs:bloemenvallei"]


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(img, *pad):
    p = os.path.join(AS, "textures", *pad)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


# ------------------------------------------------------------------------------------------------ textures
def tex_portaal(wakker):
    rng = random.Random(7 if wakker else 3)
    frames = []
    for f in range(8):
        im = Image.new("RGBA", (16, 16))
        px = im.load()
        for x in range(16):
            for y in range(16):
                a = math.atan2(y - 7.5, x - 7.5)
                r = math.hypot(x - 7.5, y - 7.5)
                s = math.sin(a * 3 + r * 0.9 - f * math.pi / 4)
                if wakker:
                    c = (int(236 + 18 * s), int(120 + 50 * s), int(196 + 30 * s), 200)
                else:
                    g = int(120 + 25 * s)
                    c = (g + 10, g - 5, g + 30, 190)
                px[x, y] = c
        for _ in range(4 if wakker else 3):
            x, y = rng.randrange(16), rng.randrange(16)
            px[x, y] = (255, 255, 255, 255) if wakker else (230, 226, 240, 230)
        frames.append(im)
    strip = Image.new("RGBA", (16, 16 * 8))
    for i, im in enumerate(frames):
        strip.paste(im, (0, i * 16))
    return strip


def tex_poortsteen():
    im = Image.new("RGBA", (16, 16), (232, 156, 186, 255))
    d = ImageDraw.Draw(im)
    rng = random.Random(11)
    for x in range(16):
        for y in range(16):
            n = rng.randint(-10, 10)
            r, g, b, _ = im.getpixel((x, y))
            im.putpixel((x, y), (r + n, g + n, b + n, 255))
    for y in (0, 8):
        d.line([(0, y), (15, y)], fill=(196, 112, 150, 255))
    d.line([(0, 15), (15, 15)], fill=(196, 112, 150, 255))
    for (x, y0, y1) in ((0, 0, 8), (8, 8, 15), (15, 0, 8)):
        d.line([(x, y0), (x, y1)], fill=(196, 112, 150, 255))
    # little glowing guh glyph in the middle
    d.ellipse((5, 3, 10, 8), fill=(255, 214, 236, 255))
    d.point([(5, 2), (10, 2)], fill=(255, 214, 236, 255))
    d.point([(6, 5), (9, 5)], fill=(90, 60, 120, 255))
    d.point([(5, 10), (10, 12), (3, 13), (12, 11)], fill=(214, 160, 255, 255))
    return im


def tex_plant():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line([(8, 15), (8, 8)], fill=(150, 150, 150, 255))
    d.line([(8, 12), (5, 10)], fill=(150, 150, 150, 255))
    d.line([(8, 13), (11, 11)], fill=(150, 150, 150, 255))
    for (x, y) in ((8, 4), (6, 6), (10, 6), (7, 8), (9, 8)):
        d.ellipse((x - 2, y - 2, x + 1, y + 1), fill=(236, 236, 236, 255))
    d.point([(8, 6)], fill=(255, 255, 255, 255))
    return im


def tex_vonk():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pts = []
    for k in range(10):
        a = -math.pi / 2 + k * math.pi / 5
        r = 7 if k % 2 == 0 else 3
        pts.append((7.5 + r * math.cos(a), 7.5 + r * math.sin(a)))
    d.polygon(pts, fill=(255, 128, 196, 255), outline=(170, 60, 140, 255))
    d.point([(6, 6), (9, 6)], fill=(60, 30, 70, 255))
    d.point([(7, 9), (8, 9)], fill=(60, 30, 70, 255))
    d.point([(5, 4), (11, 11)], fill=(255, 255, 255, 255))
    return im


def tex_maan():
    S = 64
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((6, 10, 58, 62), fill=(255, 176, 214, 255))
    d.polygon([(12, 22), (16, 2), (26, 14)], fill=(255, 176, 214, 255))
    d.polygon([(52, 22), (48, 2), (38, 14)], fill=(255, 176, 214, 255))
    d.polygon([(15, 17), (17, 8), (22, 15)], fill=(255, 214, 232, 255))
    d.polygon([(49, 17), (47, 8), (42, 15)], fill=(255, 214, 232, 255))
    for cx in (22, 42):
        d.ellipse((cx - 5, 28, cx + 5, 40), fill=(70, 40, 80, 255))
        d.ellipse((cx - 3, 29, cx, 33), fill=(255, 255, 255, 255))
    for cx in (15, 49):
        d.ellipse((cx - 4, 42, cx + 4, 47), fill=(255, 130, 180, 255))
    d.line([(28, 47), (30, 50), (32, 47), (34, 50), (36, 47)], fill=(70, 40, 80, 255), width=2)
    return im.filter(ImageFilter.SMOOTH)


def tex_draak():
    W_, H = 128, 64
    im = Image.new("RGBA", (W_, H * 2), (0, 0, 0, 0))
    for f in range(2):
        d = ImageDraw.Draw(im)
        oy = f * H
        kleur = (70, 58, 86, 255)
        d.ellipse((44, oy + 26, 92, oy + 40), fill=kleur)                                     # body
        d.polygon([(46, oy + 33), (8, oy + 30), (2, oy + 34), (44, oy + 38)], fill=kleur)      # tail
        d.ellipse((86, oy + 24, 106, oy + 36), fill=kleur)                                    # head
        d.polygon([(96, oy + 26), (100, oy + 14), (103, oy + 26)], fill=kleur)                 # guh-ear horns
        d.polygon([(90, oy + 26), (91, oy + 15), (96, oy + 26)], fill=kleur)
        if f == 0:   # wings up
            d.polygon([(58, oy + 30), (40, oy + 2), (66, oy + 12), (80, oy + 4), (76, oy + 30)], fill=kleur)
        else:        # wings down
            d.polygon([(58, oy + 34), (36, oy + 60), (62, oy + 50), (78, oy + 62), (76, oy + 34)], fill=kleur)
    return im


def tex_verte(i):
    W_, H = 256, 128
    im = Image.new("RGBA", (W_, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rng = random.Random(40 + i)
    kleur = [(196, 214, 255, 210), (214, 196, 255, 200), (176, 214, 196, 200), (150, 120, 160, 210)][i]
    if i == 0:      # crystal spires
        for k in range(9):
            x = 30 + k * 24 + rng.randint(-6, 6)
            h = rng.randint(40, 120)
            d.polygon([(x - 9, H), (x, H - h), (x + 9, H)], fill=kleur)
    elif i == 1:    # floating castle on a cloud island
        d.ellipse((60, 70, 200, 100), fill=kleur)
        d.polygon([(80, 80), (130, 124), (180, 80)], fill=kleur)
        for (x, h) in ((90, 40), (120, 64), (150, 48), (175, 34)):
            d.rectangle((x - 7, 80 - h, x + 7, 80), fill=kleur)
            d.polygon([(x - 9, 80 - h), (x, 80 - h - 16), (x + 9, 80 - h)], fill=kleur)
    elif i == 2:    # a giant tree
        d.rectangle((118, 50, 138, H), fill=kleur)
        for (x, y, r) in ((128, 34, 40), (92, 48, 28), (164, 46, 30), (128, 12, 22)):
            d.ellipse((x - r, y - r * 0.7, x + r, y + r * 0.7), fill=kleur)
    else:           # dark volcano with black fluff
        d.polygon([(20, H), (110, 30), (146, 30), (236, H)], fill=kleur)
        for k in range(6):
            x, y, r = 128 + rng.randint(-24, 24), 24 - k * 4, 10 + k * 2
            d.ellipse((x - r, y - r, x + r, y + r), fill=(60, 50, 70, 190))
    return im.filter(ImageFilter.GaussianBlur(1.2))


# ------------------------------------------------------------------------------------------------ models
def modellen():
    bs = os.path.join(AS, "blockstates")
    md = os.path.join(AS, "models", "block")
    it = os.path.join(AS, "items")
    # portal: two axes x sleeping/awake
    var = {}
    for wak in ("false", "true"):
        m = "guhs:block/vr_w_portaal_" + ("wakker" if wak == "true" else "slaap")
        var[f"axis=x,wakker={wak}"] = {"model": m}
        var[f"axis=z,wakker={wak}"] = {"model": m, "y": 90}
    _schrijf(os.path.join(bs, "vr_w_portaal.json"), {"variants": var})
    for naam in ("slaap", "wakker"):
        _schrijf(os.path.join(md, f"vr_w_portaal_{naam}.json"), {
            "textures": {"particle": f"guhs:block/vadsrijk/wereld/portaal_{naam}", "portal": f"guhs:block/vadsrijk/wereld/portaal_{naam}"},
            "elements": [{"from": [0, 0, 6], "to": [16, 16, 10], "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#portal"}, "south": {"uv": [0, 0, 16, 16], "texture": "#portal"}}}],
            "render_type": "minecraft:translucent"})
    # poortsteen
    _schrijf(os.path.join(bs, "vr_w_poortsteen.json"), {"variants": {"": {"model": "guhs:block/vr_w_poortsteen"}}})
    _schrijf(os.path.join(md, "vr_w_poortsteen.json"), {"parent": "minecraft:block/cube_all",
                                                         "textures": {"all": "guhs:block/vadsrijk/wereld/poortsteen"}})
    # scar grass: vanilla grass textures (the top + overlay are grey and get our tint)
    _schrijf(os.path.join(bs, "vr_w_litteken_gras.json"), {"variants": {"": [{"model": "guhs:block/vr_w_litteken_gras"},
                                                                            {"model": "guhs:block/vr_w_litteken_gras", "y": 90},
                                                                            {"model": "guhs:block/vr_w_litteken_gras", "y": 180},
                                                                            {"model": "guhs:block/vr_w_litteken_gras", "y": 270}]}})
    _schrijf(os.path.join(md, "vr_w_litteken_gras.json"), {"parent": "minecraft:block/grass_block", "textures": {
        "particle": "minecraft:block/dirt", "bottom": "minecraft:block/dirt", "top": "minecraft:block/grass_block_top",
        "side": "minecraft:block/grass_block_side", "overlay": "minecraft:block/grass_block_side_overlay"}})
    _schrijf(os.path.join(bs, "vr_w_litteken_plant.json"), {"variants": {"": {"model": "guhs:block/vr_w_litteken_plant"}}})
    _schrijf(os.path.join(md, "vr_w_litteken_plant.json"), {"parent": "minecraft:block/tinted_cross",
                                                             "textures": {"cross": "guhs:block/vadsrijk/wereld/litteken_plant"},
                                                             "render_type": "minecraft:cutout"})
    # items
    _schrijf(os.path.join(it, "vr_w_poortsteen.json"), {"model": {"type": "minecraft:model", "model": "guhs:block/vr_w_poortsteen"}})
    _schrijf(os.path.join(it, "vr_w_litteken_gras.json"), {"model": {"type": "minecraft:model", "model": "guhs:block/vr_w_litteken_gras",
                                                                      "tints": [{"type": "minecraft:constant", "value": -7435628}]}})
    _schrijf(os.path.join(AS, "models", "item", "vr_w_litteken_plant.json"), {"parent": "minecraft:item/generated",
                                                                              "textures": {"layer0": "guhs:block/vadsrijk/wereld/litteken_plant"}})
    _schrijf(os.path.join(it, "vr_w_litteken_plant.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_w_litteken_plant",
                                                                       "tints": [{"type": "minecraft:constant", "value": -6120280}]}})
    _schrijf(os.path.join(AS, "models", "item", "vr_w_vonk.json"), {"parent": "minecraft:item/generated",
                                                                    "textures": {"layer0": "guhs:item/vadsrijk/wereld/vonk"}})
    _schrijf(os.path.join(it, "vr_w_vonk.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_w_vonk"}})


def textures():
    for naam, wak in (("slaap", False), ("wakker", True)):
        _png(tex_portaal(wak), "block", "vadsrijk", "wereld", f"portaal_{naam}.png")
        p = os.path.join(AS, "textures", "block", "vadsrijk", "wereld", f"portaal_{naam}.png.mcmeta")
        _schrijf(p, {"animation": {"frametime": 3 if wak else 6, "interpolate": True}})
    _png(tex_poortsteen(), "block", "vadsrijk", "wereld", "poortsteen.png")
    _png(tex_plant(), "block", "vadsrijk", "wereld", "litteken_plant.png")
    _png(tex_vonk(), "item", "vadsrijk", "wereld", "vonk.png")
    _png(tex_maan(), "vadsrijk", "wereld", "lucht_maan.png")
    _png(tex_draak(), "vadsrijk", "wereld", "lucht_draak.png")
    for i in range(4):
        _png(tex_verte(i), "vadsrijk", "wereld", f"lucht_verte_{i}.png")


def data():
    for naam, r in PRIJS.items():
        _schrijf(os.path.join(DA, "recipe", naam + ".json"), r)
    # loot: poortsteen drops itself
    _schrijf(os.path.join(DA, "loot_table", "blocks", "vr_w_poortsteen.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:vr_w_poortsteen"}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # the ruined order temple in the Guhmension (kermis-rare)
    _schrijf(os.path.join(DA, "worldgen", "structure", "vr_w_portaaltempel.json"), {
        "type": "guhs:flat_jigsaw", "biomes": "#guhs:has_structure/vr_w_portaaltempel", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "beard_box", "check_radius": 30, "max_height_difference": 12, "keep_clear": 40,
        "jigsaw": {"type": "minecraft:jigsaw", "biomes": "#guhs:has_structure/vr_w_portaaltempel", "step": "surface_structures",
                   "spawn_overrides": {}, "terrain_adaptation": "beard_box", "start_pool": "guhs:vr_w_portaaltempel/start", "size": 1,
                   "start_height": {"absolute": -3}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
                   "max_distance_from_center": 80, "use_expansion_hack": False}})
    _schrijf(os.path.join(DA, "worldgen", "structure_set", "vr_w_portaaltempel.json"), {
        "structures": [{"structure": "guhs:vr_w_portaaltempel", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 34, "separation": 14, "salt": 110010501}})
    _schrijf(os.path.join(DA, "worldgen", "template_pool", "vr_w_portaaltempel", "start.json"), {
        "fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
            "element_type": "minecraft:single_pool_element", "location": "guhs:vadsrijk/wereld/portaaltempel",
            "projection": "rigid", "processors": "minecraft:empty"}}]})
    _schrijf(os.path.join(DA, "tags", "worldgen", "biome", "has_structure", "vr_w_portaaltempel.json"), {"values": GUHMENSION})


def build(h):
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)
    modellen()
    textures()
    data()


def _direct():
    modellen()
    textures()
    data()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(AS, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(TEKSTEN)
        _schrijf(p, d)
    print(f"vadsrijk_wereld: {len(TEKSTEN)} texts, blocks/items/sky/structure written")


if __name__ == "__main__":
    _direct()
