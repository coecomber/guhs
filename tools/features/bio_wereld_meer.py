"""
biomes3 wereld: the Bloesemmeertje (guhs:bloesemmeertje). Not in FEATURES: bio_wereld.py calls biome(), surface() and build().

Java (feature/bio/wereld):
  MeerTerrein   the shape: shore with headlands and bays, the depth, large and small islands, boulders, stepping stones,
                and where every tree stands (all part of the terrain model)
  MeerVulling   step "lakes": the bed (bluer with depth, mixed block by block), the water, beach sand, stone, and the big
                tree of every large island
  MeerBodem     (biomes3 fix-klein) the bed's own blocks: meerzand and three tones of meerslib, and what they are for:
                meertegels with slab and stairs (bodem() below)
  MeerLeven     step "vegetal decoration" (feature guhs:bloesemmeertje_leven, once per chunk, clear of buildings): the
                other trees, flowers, bloesemriet and guh-waterlelies in sheltered corners, drijvende bloesemblaadjes
                under the crowns and in drifting streaks, seagrass, kaaskoraal tufts, a reuzenschelp
  BloesemBoom   the tree in four sizes;  MeerRitme + client/MeerClient: the day rhythm of the falling petals

This file: the biome (colours, music, ambience, the ambient petals), the lake floor's surface rule, the feature's data,
the block guhs:bloesemmeertje_riet (Bloesemriet), the sound events. The OGGs come from bio_wereld_meer_geluid.py (not part
of make_resources.py; committed like the other sounds). Keep step 1 of the features exactly ["guhs:bio_wereld_vulling"]
(bio_wereld.py asserts it). Blocks of other slices: lib.blok(h, "<id>", "<stand-in>").
"""
import os

import numpy as np
from PIL import Image

WATER_Y = 49   # MeerTerrein.WATER: the top water block of every lake
RIET = "bloesemmeertje_riet"
LEVEN = "bloesemmeertje_leven"
# sound event -> (files under assets/guhs/sounds/bloesemmeertje/, streamed)
GELUID = {"bloesemmeertje.muziek": ["muziek1", "muziek2"], "bloesemmeertje.ambient": ["ambient"]}
# biomes3 fix-klein: the lake bed's own sediment (MeerBodem.java), from the shallows to the deep, and the tile it bakes into
MEERZAND = "bloesemmeertje_meerzand"
SLIB = ["bloesemmeertje_meerslib_licht", "bloesemmeertje_meerslib", "bloesemmeertje_meerslib_diep"]
TEGELS = "bloesemmeertje_meertegels"
# texture: (light, dark, fleck) per sediment; under the lake's turquoise water they read as sand that turns blue-green
BODEM_TINT = {MEERZAND: ((236, 232, 212), (220, 219, 200), (246, 246, 236)),
              SLIB[0]: ((158, 214, 210), (132, 198, 200), (190, 230, 224)),
              SLIB[1]: ((92, 176, 180), (68, 156, 166), (124, 196, 196)),
              SLIB[2]: ((44, 134, 148), (28, 112, 132), (66, 152, 160))}
BODEM_BLOKKEN = [MEERZAND, *SLIB, TEGELS, f"{TEGELS}_plaat", f"{TEGELS}_trap"]
BODEM_RECEPTEN = [TEGELS, f"{TEGELS}_plaat", f"{TEGELS}_trap", f"{TEGELS}_plaat_steenzagen", f"{TEGELS}_trap_steenzagen"]
TEKSTEN = {f"block.guhs.{RIET}": "Bloesemriet",
           f"block.guhs.{MEERZAND}": "Meerzand", f"block.guhs.{SLIB[0]}": "Licht meerslib", f"block.guhs.{SLIB[1]}": "Meerslib",
           f"block.guhs.{SLIB[2]}": "Diep meerslib", f"block.guhs.{TEGELS}": "Meertegels", f"block.guhs.{TEGELS}_plaat": "Meertegelplaat",
           f"block.guhs.{TEGELS}_trap": "Meertegeltrap"}


def biome(h, lib, ores, lege_spawners):
    """The biome file (the 1.21.1 shape; tools/mc26.py converts the effects)."""
    return {
        # it can rain here (the petals come down thicker then: MeerRitme)
        "has_precipitation": True, "temperature": 0.8, "downfall": 0.5, "creature_spawn_probability": 0.3,
        "effects": {
            # a soft, fresh look: the Guhmensie's pink sky a shade lighter and cooler, a pale haze, light pink leaves
            "sky_color": 0xEBCBEC, "fog_color": 0xFDEEF6, "grass_color": 0xF9BFD6, "foliage_color": 0xF9BFD6,
            # clear turquoise water; the fog under water is light, so the bed still shows at seven deep
            "water_color": 0x4FD6D2, "water_fog_color": 0x3CB8BE,
            # a few petals always fall; more with wind, rain and at dusk and dawn (client/MeerClient)
            "particle": {"options": {"type": "minecraft:cherry_leaves"}, "probability": 0.004},
            "ambient_sound": "guhs:bloesemmeertje.ambient",
            "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
            "music": {"sound": "guhs:bloesemmeertje.muziek", "min_delay": 6000, "max_delay": 14000, "replace_current_music": False}},
        # (koi, kikkerguhs and the bloesemguh of slice dieren come in through its own biome modifiers; no Mika's here)
        "spawners": {**lege_spawners, "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 2, "maxCount": 4}]},
        "spawn_costs": {}, "carvers": {"air": []},
        # (no ore veins: the valley and the lake cut down to where the veins are, they would lie open at the surface)
        "features": [[], ["guhs:bio_wereld_vulling"], [], [], [], [], [], [], [], [f"guhs:{LEVEN}"], []]}


def surface(h, lib):
    """The surface rule inside the biome: sand on everything below the shore (the lake floor, also where the feature has
    not put water yet), four blocks deep; the land stays the Guhmensie's pink wool. (MeerVulling then lays the real bed:
    sand in the shallows, then meerzand and meerslib, two blocks thick.)"""
    boven = {"type": "minecraft:y_above", "anchor": {"absolute": WATER_Y + 1}, "surface_depth_multiplier": 0, "add_stone_depth": False}
    diep = {"type": "minecraft:stone_depth", "offset": 3, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
    return {"type": "minecraft:condition", "if_true": {"type": "minecraft:not", "invert": boven},
            "then_run": {"type": "minecraft:condition", "if_true": diep,
                         "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:sand"}}}}


# ---------------------------------------------------------------------------------------------------------------------
def riet_tex(lib):
    """Bloesemriet, two pictures (bottom and top of a two-block plant): thin pale stalks with soft pink plumes."""
    rng = lib.rng("bloesemmeertje_riet")
    onder, boven = Image.new("RGBA", (16, 16), (0, 0, 0, 0)), Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    stengel = [(198, 208, 168), (176, 190, 150), (214, 220, 186)]
    pluim = [(250, 176, 206), (255, 204, 224), (242, 150, 188), (255, 232, 240)]

    def zet(x, yy, kleur):
        if 0 <= x < 16 and 0 <= yy < 32:
            img, y = (boven, yy) if yy < 16 else (onder, yy - 16)
            img.putpixel((x, y), tuple(int(max(0, min(255, c + rng.integers(-6, 7)))) for c in kleur) + (255,))

    # (x at the foot, lean per 8 px, height out of 32, length of the plume)
    for x0, lean, hoog, lang in ((2, 0.5, 27, 7), (5, -0.25, 31, 9), (8, 0.15, 24, 6), (11, -0.4, 29, 8), (13, 0.3, 21, 6)):
        top = 32 - hoog
        for yy in range(top, 32):
            x = int(round(x0 + lean * (32 - yy) / 8))
            zet(x, yy, stengel[(yy + x0) % 3])
        # the plume: a soft feather, widest a third down, leaning with the stalk
        for i in range(lang):
            yy = top + i
            x = int(round(x0 + lean * (32 - yy) / 8))
            breed = 0 if i == 0 or i >= lang - 1 else 1
            for dx in range(-breed, breed + 1):
                if dx != 0 and rng.random() < 0.3:
                    continue
                zet(x + dx, yy, pluim[int(rng.integers(0, 4)) if dx == 0 else int(rng.integers(0, 2))])
    # a few blades at the foot
    for x0, hoog, kant in ((3, 6, -1), (7, 5, 1), (10, 7, -1), (14, 4, 1)):
        for i in range(hoog):
            zet(x0 + (kant if i > hoog * 0.6 else 0), 31 - i, stengel[1])
    return onder, boven


def riet(h, lib):
    A, D = h.A, h.D
    onder, boven = riet_tex(lib)
    h.save(onder, "block", f"{RIET}_bottom.png")
    h.save(boven, "block", f"{RIET}_top.png")
    for half in ("bottom", "top"):
        h.w(f"{A}/models/block/{RIET}_{half}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                     "textures": {"cross": f"guhs:block/{RIET}_{half}"}})
    h.w(f"{A}/blockstates/{RIET}.json", {"variants": {"half=lower": {"model": f"guhs:block/{RIET}_bottom"},
                                                       "half=upper": {"model": f"guhs:block/{RIET}_top"}}})
    h.item_model(RIET, f"guhs:block/{RIET}_top")
    h.w(f"{D}/loot_table/blocks/{RIET}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"guhs:{RIET}", "conditions": [
            {"condition": "minecraft:block_state_property", "block": f"guhs:{RIET}", "properties": {"half": "lower"}}]}]}]})
    h.add_tag("minecraft/tags/block/replaceable_by_trees", [f"guhs:{RIET}"])
    h.add_tag("minecraft/tags/block/sword_efficient", [f"guhs:{RIET}"])


# ---------------------------------------------------------------------------------------------------------------------
# biomes3 fix-klein: the bed's own blocks
def _veld(lib, naam, grof_x, grof_y, n=16):
    """A soft field of n x n values 0..1 that tiles on all four sides."""
    f = lib.rng(naam).random((n, n))
    k = np.fft.fftfreq(n) * n
    g = np.exp(-(k[:, None] ** 2) / (2 * grof_y ** 2) - (k[None, :] ** 2) / (2 * grof_x ** 2))
    v = np.real(np.fft.ifft2(np.fft.fft2(f) * g))
    return (v - v.min()) / (v.max() - v.min() + 1e-9)


def _png(rgb):
    a = np.clip(np.rint(rgb), 0, 255).astype(np.uint8)
    return Image.fromarray(np.concatenate([a, np.full(a.shape[:2] + (1,), 255, np.uint8)], axis=2), "RGBA")


def sediment_tex(lib, naam):
    """Soft sediment: calm low-contrast clouds, a breath of lying ripple, a few pale grains; no outline, tiles."""
    licht, donker, korrel = (np.asarray(c, np.float32) for c in BODEM_TINT[naam])
    v = 0.6 * _veld(lib, naam + "_wolk", 1.6, 1.6) + 0.4 * _veld(lib, naam + "_ribbel", 1.0, 3.2)
    v = np.clip((v - 0.25) / 0.5, 0, 1)
    rgb = licht[None, None, :] * (1 - v[..., None]) + donker[None, None, :] * v[..., None]
    r = lib.rng(naam + "_korrel")
    rgb += r.integers(-3, 4, (16, 16, 1))
    for _ in range(9 if naam == MEERZAND else 6):
        x, y = int(r.integers(16)), int(r.integers(16))
        rgb[y, x] = rgb[y, x] * 0.45 + korrel * 0.55
    return _png(rgb)


def tegel_tex(lib):
    """Meertegels: four glazed tiles of soft turquoise in a pale joint, each a shade of its own, lighter towards the top left."""
    r = lib.rng("bloesemmeertje_meertegels")
    rgb = np.zeros((16, 16, 3), np.float32)
    voeg = np.asarray((238, 246, 242), np.float32)
    glans = _veld(lib, "bloesemmeertje_meertegels_glans", 2.0, 2.0)
    for ty in range(2):
        for tx in range(2):
            basis = np.asarray((166, 222, 214), np.float32) + float(r.integers(-7, 8)) * np.asarray((1.0, 0.7, 0.6), np.float32)
            for y in range(8):
                for x in range(8):
                    px, py = tx * 8 + x, ty * 8 + y
                    if x == 0 or y == 0:
                        rgb[py, px] = voeg
                        continue
                    c = basis + 7 * (glans[py, px] - 0.5)
                    if x == 1 or y == 1:
                        c = c * 0.6 + 255 * 0.4         # the lit edge of the glaze
                    elif x == 7 or y == 7:
                        c = c * 0.93                    # and its shaded one
                    rgb[py, px] = c
    return _png(rgb)


def bodem(h, lib):
    """The four sediments (shovel, drop themselves), the tile they bake into with its slab and stairs, tags and recipes."""
    from features import bio_blokken_dal as dal_blokken   # (only its copy of vanilla's slab and stairs files)
    A, D, w = h.A, h.D, h.w
    for b in (MEERZAND, *SLIB):
        h.save(sediment_tex(lib, b), "block", f"{b}.png")
        h.simple_block(b)
        h.self_drop(b)
    h.save(tegel_tex(lib), "block", f"{TEGELS}.png")
    h.simple_block(TEGELS)
    h.self_drop(TEGELS)
    with dal_blokken._jar() as z:
        dal_blokken.copy_set(h, z, z.namelist(), {f"{TEGELS}_trap": "brick_stairs", f"{TEGELS}_plaat": "brick_slab"},
                             {"minecraft:block/bricks": f"guhs:block/{TEGELS}"}, TEGELS, TEGELS, "bricks")
    for b in (f"{TEGELS}_trap", f"{TEGELS}_plaat"):
        w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
    # any meerslib bakes into a tile (a furnace, like clay into terracotta); meerzand melts into glass like sand
    w(f"{D}/recipe/{TEGELS}.json", {"type": "minecraft:smelting", "category": "blocks", "ingredient": {"tag": "guhs:bloesemmeertje_meerslib"},
                                    "result": {"id": f"guhs:{TEGELS}"}, "experience": 0.1, "cookingtime": 200})
    for result, n in ((f"{TEGELS}_trap", 1), (f"{TEGELS}_plaat", 2)):
        w(f"{D}/recipe/{result}_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"guhs:{TEGELS}"},
                                                   "result": {"id": f"guhs:{result}", "count": n}})
    h.add_tag("guhs/tags/item/bloesemmeertje_meerslib", [f"guhs:{b}" for b in SLIB])
    h.add_tag("minecraft/tags/item/smelts_to_glass", [f"guhs:{MEERZAND}"])
    h.add_tag("minecraft/tags/block/mineable/shovel", [f"guhs:{b}" for b in (MEERZAND, *SLIB)])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{TEGELS}", f"guhs:{TEGELS}_trap", f"guhs:{TEGELS}_plaat"])
    for kind in ("block", "item"):
        h.add_tag(f"minecraft/tags/{kind}/stairs", [f"guhs:{TEGELS}_trap"])
        h.add_tag(f"minecraft/tags/{kind}/slabs", [f"guhs:{TEGELS}_plaat"])
    mis = [p for b in BODEM_BLOKKEN for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json")
           if not os.path.exists(p)]
    mis += [f"{D}/recipe/{r}.json" for r in BODEM_RECEPTEN if not os.path.exists(f"{D}/recipe/{r}.json")]
    if mis:
        raise SystemExit("bio_wereld_meer: the bed's blocks are incomplete, missing:\n  " + "\n  ".join(mis))


def geluid(h):
    def patch(d):
        d["bloesemmeertje.muziek"] = {"sounds": [{"name": f"guhs:bloesemmeertje/{f}", "stream": True, "weight": 3, "volume": 0.9}
                                                 for f in GELUID["bloesemmeertje.muziek"]]
                                      # now and then the game's own cherry grove music, so the two tracks do not wear out
                                      + [{"name": "minecraft:music.overworld.cherry_grove", "type": "event", "weight": 3}]}
        d["bloesemmeertje.ambient"] = {"sounds": [{"name": "guhs:bloesemmeertje/ambient", "stream": True, "volume": 0.55}]}
    h.patch_json(f"{h.A}/sounds.json", patch)


def build(h, lib):
    """Everything else of the biome (the feature's data, the reed, the sounds, the texts)."""
    wg = f"{h.D}/worldgen"
    h.w(f"{wg}/configured_feature/{LEVEN}.json", {"type": f"guhs:{LEVEN}", "config": {}})
    h.w(f"{wg}/placed_feature/{LEVEN}.json", {"feature": f"guhs:{LEVEN}", "placement": []})
    # the kern's placeholder tree feature is gone (MeerLeven places the trees)
    oud = f"{wg}/placed_feature/bloesemmeertje_bloesemboom.json"
    if os.path.exists(oud):
        os.remove(oud)
    riet(h, lib)
    bodem(h, lib)   # biomes3 fix-klein
    geluid(h)
    lib.teksten(h, TEKSTEN)
    mis = [f for fs in GELUID.values() for f in fs if not os.path.exists(f"{h.A}/sounds/bloesemmeertje/{f}.ogg")]
    if mis:
        raise SystemExit(f"bio_wereld_meer: sounds missing: {mis} (run: python tools/features/bio_wereld_meer_geluid.py)")
