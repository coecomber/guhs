"""
1.7.0 "Onder de Wol" (design: MineScapeStory/ontwerp-guhs170-mijnen.md; Java: feature/onderwol): the underground of the
Guhmension. This module makes the resources of the kern and the whole terrain:

  - the blocks (viltsteen and its family, knuffelnaad, guhklei, every ore in a version per layer, vahoegiet, the plants of
    the two cave biomes): models, loot, tags, recipes, texts (textures: onderwol_tex.py);
  - the layers: a surface rule that runs first, below the preliminary surface (so cave floors stay stone): knuffelnaad
    y 0-4, guhkristalsteen up to y ~25, knuffelsteen up to ~45, viltsteen up to ~64, then the pink wool as before. The
    borders wave a few blocks (a 2D noise) and are speckled (vertical gradients);
  - the caves: a term in the final density (inside guhs:bio_terrein, after every older module): vanilla's cheese caves
    (more and bigger the deeper), spaghetti and entrances (the cave mouths), closing up towards the bottom and never
    under the seas; plus vanilla's carvers (winding caves, ravines) for caves that open up to the surface;
  - the cave biomes guhs:glitterspelonk (knuffelsteen) and guhs:pluisgrot (viltsteen): a shift of depth and
    continentalness where their noise says so, deep inside the rock (the same "deep" trick as the Gatenkaasgrotten);
  - the ores, the geodes, the saus springs, the saus sea and the sinkholes in every Guhmension biome.

It must stay the LAST module that touches the Guhmension's terrain (it wraps what the others made). New chunks only.

    python tools/make_resources.py        (from the project root; this module runs as part of it)
"""
import json
import os
import zipfile

from features import knuffeldal_wereld as kw
from features import onderwol_tex as tex
from features import onderwol_tex2 as tex2
from features import onderwol_geluid as geluid
from features.barbecuether import stair_states, wall_multipart

LAGEN = ["vilt", "knuffel", "kristal"]
LAAGSTEEN = {"vilt": "guhs:viltsteen", "knuffel": "guhs:knuffelsteen", "kristal": "guhs:guh_kristalsteen"}
SOORTEN = ["kolenerts", "kopererts", "ijzererts", "gouderts", "lapiserts", "redstoneerts", "diamanterts", "smaragderts",
           "vadserts", "kaasknabbelerts", "kaaskorrelerts", "pigmenterts"]
VANILLA = {"kolenerts": "coal", "kopererts": "copper", "ijzererts": "iron", "gouderts": "gold", "lapiserts": "lapis",
           "redstoneerts": "redstone", "diamanterts": "diamond", "smaragderts": "emerald"}
ERTSEN = [f"{s}_{l}" for s in SOORTEN for l in LAGEN]
VILT = ["viltsteen", "gepolijste_viltsteen", "viltsteen_bakstenen", "viltsteen_tegels", "gebeitelde_viltsteen"]
NIEUW27 = ["roze_knuffelsteen", "viltkiezel"]   # (ronde 27: the pink veins of the knuffelsteen; soft gravel along brooks and shores)
TRAPPEN = ["viltsteen", "gepolijste_viltsteen", "viltsteen_bakstenen", "viltsteen_tegels"]
MUREN = ["viltsteen", "viltsteen_bakstenen"]
PLANTEN = ["glitterrank", "glitterrank_bes", "glinsterpaddenstoel", "pluisbolletje"]
BIOMES = ["glitterspelonk", "pluisgrot", "slaapgrot", "knuffelwortelgrot", "belletjesgrot", "kristalpegelgrot"]
# (ronde 28) every cave biome: its noise, threshold, band of height (bottom, top), and the climate axis it is pushed along
# (the others are pushed along continentalness/erosion/weirdness too, so they never win each other's spot)
GROTTEN = {
    "glitterspelonk": ("onderwol_glitter", 0.28, (26, 44), "continents", 1),
    "pluisgrot": ("onderwol_pluis", 0.30, (46, 62), "continents", -1),
    "slaapgrot": ("onderwol_slaap", 0.30, (46, 62), "erosion", 1),
    "knuffelwortelgrot": ("onderwol_wortel", 0.22, (48, 62), "erosion", -1),   # (only under the forests, see router)
    "belletjesgrot": ("onderwol_belletjes", 0.26, (26, 44), "ridges", 1),
    "kristalpegelgrot": ("onderwol_pegel", 0.36, (10, 40), "ridges", -1),
}
KLIMAAT = {"continents": "continentalness", "erosion": "erosion", "ridges": "weirdness"}
NOISES = {
    "onderwol_golf": {"firstOctave": -6, "amplitudes": [1.0, 0.5]},       # the waves of the layer borders (2D)
    "onderwol_glitter": {"firstOctave": -6, "amplitudes": [1.0, 0.5]},    # where the Glitterspelonk is
    "onderwol_pluis": {"firstOctave": -6, "amplitudes": [1.0, 0.5]},      # where the Pluisgrot is
    "onderwol_slaap": {"firstOctave": -6, "amplitudes": [1.0, 0.5]},      # (ronde 28) the Slaapgrot, Knuffelwortelgrot,
    "onderwol_wortel": {"firstOctave": -6, "amplitudes": [1.0, 0.5]},     # Belletjesgrot and Kristalpegelgrot
    "onderwol_belletjes": {"firstOctave": -6, "amplitudes": [1.0, 0.5]},
    "onderwol_pegel": {"firstOctave": -6, "amplitudes": [1.0, 0.5]},
    # (ronde 27, OnderWolLagen) the knuffelsteen's pink veins (3D), its bubbles of wool (3D), the bare patches of viltsteen on low
    # ground and where the pastel stripes run (2D)
    "onderwol_roze": {"firstOctave": -5, "amplitudes": [1.0, 0.5]},
    "onderwol_wolbel": {"firstOctave": -3, "amplitudes": [1.0]},
    "onderwol_kaal": {"firstOctave": -4, "amplitudes": [1.0, 0.5, 0.25]},
    "onderwol_streep": {"firstOctave": -7, "amplitudes": [1.0]},
}
# the cave biomes: noise threshold (above it the biome), and how far depth/continentalness are pushed there
GLITTER_VANAF, PLUIS_VANAF = 0.28, 0.30
SHIFT = 10.0
# the caves are only worked out where the terrain (without caves) is above this: inside the land and just above it
LUCHT = -0.2
MC_JAR = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "libraries", "com", "mojang", "minecraft", "26.1.2",
                      "minecraft-26.1.2-client.jar")


def _mc(path):
    with zipfile.ZipFile(MC_JAR) as jar:
        return json.loads(jar.read(path))


def g(*names):
    return [f"guhs:{n}" for n in names]


# =====================================================================================================================
# blocks, items, loot, tags, recipes, texts
# =====================================================================================================================
def blocks_and_items(h):
    A, w = h.A, h.w
    for b in VILT + NIEUW27 + ["knuffelnaad", "guhklei", "lichtmosblok", "pluiskussen", "vahoegiet_erts"] + [e for e in ERTSEN if not e.startswith("redstoneerts")]:
        h.simple_block(b)
    for e in [e for e in ERTSEN if e.startswith("redstoneerts")]:
        w(f"{A}/models/block/{e}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{e}"}})
        w(f"{A}/blockstates/{e}.json", {"variants": {"lit=false": {"model": f"guhs:block/{e}"}, "lit=true": {"model": f"guhs:block/{e}"}}})
        w(f"{A}/models/item/{e}.json", {"parent": f"guhs:block/{e}"})
    w(f"{A}/models/block/lichtmos.json", {"parent": "minecraft:block/carpet", "textures": {"wool": "guhs:block/lichtmos"}})
    w(f"{A}/blockstates/lichtmos.json", {"variants": {"": {"model": "guhs:block/lichtmos"}}})
    w(f"{A}/models/item/lichtmos.json", {"parent": "guhs:block/lichtmos"})
    for p in PLANTEN:
        w(f"{A}/models/block/{p}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{p}"}})
        w(f"{A}/blockstates/{p}.json", {"variants": {"": {"model": f"guhs:block/{p}"}}})
        h.item_model(p, f"guhs:block/{p}")
    for n in TRAPPEN:
        t = f"guhs:block/{n}"
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            w(f"{A}/models/block/{n}_trap{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": t, "top": t, "side": t}})
        w(f"{A}/blockstates/{n}_trap.json", {"variants": stair_states(f"guhs:block/{n}_trap")})
        w(f"{A}/models/item/{n}_trap.json", {"parent": f"guhs:block/{n}_trap"})
        w(f"{A}/models/block/{n}_plaat.json", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
        w(f"{A}/models/block/{n}_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
        w(f"{A}/blockstates/{n}_plaat.json", {"variants": {"type=bottom": {"model": f"guhs:block/{n}_plaat"},
                                                             "type=top": {"model": f"guhs:block/{n}_plaat_top"},
                                                             "type=double": {"model": f"guhs:block/{n}"}}})
        w(f"{A}/models/item/{n}_plaat.json", {"parent": f"guhs:block/{n}_plaat"})
    for n in MUREN:
        t = f"guhs:block/{n}"
        for part, parent in (("post", "template_wall_post"), ("side", "template_wall_side"), ("side_tall", "template_wall_side_tall")):
            w(f"{A}/models/block/{n}_muur_{part}.json", {"parent": f"minecraft:block/{parent}", "textures": {"wall": t}})
        w(f"{A}/models/block/{n}_muur_inventory.json", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t}})
        w(f"{A}/blockstates/{n}_muur.json", {"multipart": wall_multipart(f"guhs:block/{n}_muur")})
        w(f"{A}/models/item/{n}_muur.json", {"parent": f"guhs:block/{n}_muur_inventory"})
    for i in ("vahoegiet", "glitterbes"):
        h.item_model(i)


SILK = [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]
SHEARS = [{"condition": "minecraft:any_of", "terms": [
    {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}, *SILK]}]
FORTUIN = {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"}
DECAY = {"function": "minecraft:explosion_decay"}
# pigment: the common pastel dyes in the upper layers, the rarer colours deeper
PIGMENT = {"vilt": ["pink", "white", "light_blue", "yellow", "lime"], "knuffel": ["pink", "magenta", "light_blue", "yellow", "orange", "lime"],
           "kristal": ["purple", "cyan", "magenta", "blue", "light_gray", "orange"]}


def loot(h):
    D, w = h.D, h.w

    def alt(block, children):
        w(f"{D}/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
            {"type": "minecraft:alternatives", "children": [{"type": "minecraft:item", "name": f"guhs:{block}", "conditions": SILK}, *children]}]}],
            "random_sequence": f"guhs:blocks/{block}"})

    def item(name, fns):
        return {"type": "minecraft:item", "name": name, "functions": fns}
    for b in VILT + NIEUW27 + ["lichtmosblok", "lichtmos", "glinsterpaddenstoel", "pluiskussen"] + [f"{n}_trap" for n in TRAPPEN] + [f"{n}_muur" for n in MUREN]:
        h.self_drop(b)
    for n in TRAPPEN:
        w(f"{D}/loot_table/blocks/{n}_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"guhs:{n}_plaat", "functions": [
                {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property",
                                                                                 "block": f"guhs:{n}_plaat", "properties": {"type": "double"}}]},
                DECAY]}]}]})
    # guhklei: four clay balls (they bake into bricks; the block itself bakes into pink terracotta)
    alt("guhklei", [item("minecraft:clay_ball", [{"function": "minecraft:set_count", "count": 4}, DECAY])])
    # the vanilla ores: vanilla's own loot with our names
    for soort, mc in VANILLA.items():
        tabel = json.dumps(_mc(f"data/minecraft/loot_table/blocks/{mc}_ore.json"))
        for laag in LAGEN:
            e = f"{soort}_{laag}"
            t = json.loads(tabel.replace(f"minecraft:{mc}_ore", f"guhs:{e}").replace(f"minecraft:blocks/{mc}_ore", f"guhs:blocks/{e}"))
            w(f"{D}/loot_table/blocks/{e}.json", t)
    for laag in LAGEN:
        alt(f"vadserts_{laag}", [item("guhs:vahoege_vads", [FORTUIN, DECAY])])
        alt(f"kaasknabbelerts_{laag}", [item("guhs:kaas_knabbels", [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
                                                                   FORTUIN, DECAY])])
        alt(f"kaaskorrelerts_{laag}", [item("guhs:kaaskorrel", [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                                                               FORTUIN, DECAY])])
        kleuren = PIGMENT[laag]
        w(f"{D}/loot_table/blocks/pigmenterts_{laag}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "conditions": SILK, "entries": [{"type": "minecraft:item", "name": f"guhs:pigmenterts_{laag}"}]},
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "bonus_rolls": 0,
             "conditions": [{"condition": "minecraft:inverted", "term": SILK[0]}],
             "entries": [item(f"minecraft:{k}_dye", [DECAY]) for k in kleuren]}], "random_sequence": f"guhs:blocks/pigmenterts_{laag}"})
    alt("vahoegiet_erts", [item("guhs:vahoegiet", [FORTUIN, DECAY])])
    # the vines: berries from a berry end; the vine itself only with shears or silk touch
    w(f"{D}/loot_table/blocks/glitterrank.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "conditions": SHEARS,
                                                                                       "entries": [{"type": "minecraft:item", "name": "guhs:glitterrank"}]}]})
    w(f"{D}/loot_table/blocks/glitterrank_bes.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        item("guhs:glitterbes", [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}, DECAY])]}]})
    w(f"{D}/loot_table/blocks/pluisbolletje.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "conditions": SHEARS,
                                                                                         "entries": [{"type": "minecraft:item", "name": "guhs:pluisbolletje"}]}]})


def tags(h):
    stenen = VILT + [f"{n}_trap" for n in TRAPPEN] + [f"{n}_plaat" for n in TRAPPEN] + [f"{n}_muur" for n in MUREN]
    h.add_tag("minecraft/tags/block/mineable/pickaxe", g(*stenen, *ERTSEN, "vahoegiet_erts"))
    h.add_tag("minecraft/tags/block/mineable/shovel", g("guhklei", "viltkiezel"))
    h.add_tag("minecraft/tags/block/mineable/pickaxe", g("roze_knuffelsteen"))
    h.add_tag("minecraft/tags/block/mineable/hoe", g("lichtmosblok", "lichtmos", "pluiskussen"))
    h.add_tag("minecraft/tags/block/needs_stone_tool", g(*[f"{s}_{l}" for s in ("ijzererts", "kopererts", "lapiserts") for l in LAGEN]))
    h.add_tag("minecraft/tags/block/needs_iron_tool", g(*[f"{s}_{l}" for s in ("gouderts", "redstoneerts", "diamanterts", "smaragderts", "vadserts")
                                                          for l in LAGEN], "vahoegiet_erts"))
    for kind in ("block", "item"):
        h.add_tag(f"minecraft/tags/{kind}/stairs", g(*[f"{n}_trap" for n in TRAPPEN]))
        h.add_tag(f"minecraft/tags/{kind}/slabs", g(*[f"{n}_plaat" for n in TRAPPEN]))
        h.add_tag(f"minecraft/tags/{kind}/walls", g(*[f"{n}_muur" for n in MUREN]))
        # the vanilla ores count as their vanilla kind (and for other mods as c:ores)
        for soort, mc in VANILLA.items():
            h.add_tag(f"minecraft/tags/{kind}/{mc}_ores", g(*[f"{soort}_{l}" for l in LAGEN]))
            h.add_tag(f"c/tags/{kind}/ores/{mc}", g(*[f"{soort}_{l}" for l in LAGEN]))
        h.add_tag(f"c/tags/{kind}/ores", g(*ERTSEN, "vahoegiet_erts"))
    # the rock of the layers (what ores, springs, geodes and the cave plants grow into)
    h.add_tag("guhs/tags/block/onderwol/laagsteen", ["minecraft:pink_wool", "guhs:viltsteen", "guhs:knuffelsteen", "guhs:roze_knuffelsteen",
                                                    "guhs:guh_kristalsteen"])
    # (1.7.0 extra: a golfballetje also pays for a go in the grijpmachine, like the other minigame coins)
    h.add_tag("guhs/tags/item/knus/grijptickets", ["guhs:golfballetje"])


def recipes(h):
    D = h.D

    def bak(naam, van, naar, xp, kook=True):
        for kind, typ, t in (("smelten", "minecraft:smelting", 200), ("blazen", "minecraft:blasting", 100)):
            if kind == "blazen" and not kook:
                continue
            h.w(f"{D}/recipe/{naam}_{kind}.json", {"type": typ, "category": "misc", "ingredient": van, "result": {"id": naar},
                                                   "experience": xp, "cookingtime": t})
    uit = {"kolenerts": ("minecraft:coal", 0.1), "kopererts": ("minecraft:copper_ingot", 0.7), "ijzererts": ("minecraft:iron_ingot", 0.7),
           "gouderts": ("minecraft:gold_ingot", 1.0), "lapiserts": ("minecraft:lapis_lazuli", 0.2), "redstoneerts": ("minecraft:redstone", 0.7),
           "diamanterts": ("minecraft:diamond", 1.0), "smaragderts": ("minecraft:emerald", 1.0), "vadserts": ("guhs:vahoege_vads_ingot", 1.0),
           "kaasknabbelerts": ("guhs:kaas_knabbels", 0.2), "kaaskorrelerts": ("guhs:kaaskorrel", 0.2), "pigmenterts": ("minecraft:pink_dye", 0.1)}
    for soort, (naar, xp) in uit.items():
        bak(f"{soort}_onderwol", [f"guhs:{soort}_{l}" for l in LAGEN], naar, xp)
    bak("vahoegiet_van_erts", "guhs:vahoegiet_erts", "guhs:vahoegiet", 1.0)
    # guhklei: the block bakes into pink terracotta (the dakpannen and pleister of 1.6.0), the clay balls into bricks (vanilla)
    bak("roze_terracotta_van_guhklei", "guhs:guhklei", "minecraft:pink_terracotta", 0.35, kook=False)
    # viltsteen: polished, bricks, tiles, carved; the shapes; and all of it on the stonecutter
    h.shaped("gepolijste_viltsteen", ["VV", "VV"], {"V": "guhs:viltsteen"}, "guhs:gepolijste_viltsteen", 4)
    h.shaped("viltsteen_bakstenen", ["VV", "VV"], {"V": "guhs:gepolijste_viltsteen"}, "guhs:viltsteen_bakstenen", 4)
    h.shaped("viltsteen_tegels", ["VV", "VV"], {"V": "guhs:viltsteen_bakstenen"}, "guhs:viltsteen_tegels", 4)
    h.shaped("gebeitelde_viltsteen", ["P", "P"], {"P": "guhs:viltsteen_bakstenen_plaat"}, "guhs:gebeitelde_viltsteen")
    for n in TRAPPEN:
        h.shaped(f"{n}_trap", ["S  ", "SS ", "SSS"], {"S": f"guhs:{n}"}, f"guhs:{n}_trap", 4)
        h.shaped(f"{n}_plaat", ["SSS"], {"S": f"guhs:{n}"}, f"guhs:{n}_plaat", 6)
    for n in MUREN:
        h.shaped(f"{n}_muur", ["SSS", "SSS"], {"S": f"guhs:{n}"}, f"guhs:{n}_muur", 6)
    zagen = [(b, 1) for b in VILT[1:]] + [(f"{n}_trap", 1) for n in TRAPPEN] + [(f"{n}_plaat", 2) for n in TRAPPEN] + [(f"{n}_muur", 1) for n in MUREN]
    for out, c in zagen:
        h.w(f"{D}/recipe/{out}_van_viltsteen_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": "guhs:viltsteen",
                                                                "result": {"id": f"guhs:{out}", "count": c}})


NAMEN = {  # id: (Dutch, English)
    "viltsteen": ("Viltsteen", "Felt Stone"), "gepolijste_viltsteen": ("Gepolijste viltsteen", "Polished Felt Stone"),
    "viltsteen_bakstenen": ("Viltsteen bakstenen", "Felt Stone Bricks"), "viltsteen_tegels": ("Viltsteen tegels", "Felt Stone Tiles"),
    "gebeitelde_viltsteen": ("Gebeitelde viltsteen", "Chiseled Felt Stone"),
    "viltsteen_trap": ("Viltsteentrap", "Felt Stone Stairs"), "viltsteen_plaat": ("Viltsteenplaat", "Felt Stone Slab"),
    "viltsteen_muur": ("Viltsteenmuurtje", "Felt Stone Wall"),
    "gepolijste_viltsteen_trap": ("Gepolijste viltsteentrap", "Polished Felt Stone Stairs"),
    "gepolijste_viltsteen_plaat": ("Gepolijste viltsteenplaat", "Polished Felt Stone Slab"),
    "viltsteen_bakstenen_trap": ("Viltsteen bakstenen trap", "Felt Stone Brick Stairs"),
    "viltsteen_bakstenen_plaat": ("Viltsteen bakstenen plaat", "Felt Stone Brick Slab"),
    "viltsteen_bakstenen_muur": ("Viltsteen bakstenen muur", "Felt Stone Brick Wall"),
    "viltsteen_tegels_trap": ("Viltsteen tegeltrap", "Felt Stone Tile Stairs"), "viltsteen_tegels_plaat": ("Viltsteen tegelplaat", "Felt Stone Tile Slab"),
    "knuffelnaad": ("Knuffelnaad", "Plush Seam"), "guhklei": ("Guhklei", "Guh Clay"),
    "vahoegiet_erts": ("Vahoegieterts", "Vahoegite Ore"),
    "lichtmosblok": ("Lichtmosblok", "Glow Moss Block"), "lichtmos": ("Lichtmos", "Glow Moss"),
    "glitterrank": ("Glitterrank", "Glitter Vine"), "glitterrank_bes": ("Glitterrank met bessen", "Glitter Vine with Berries"),
    "glinsterpaddenstoel": ("Glinsterpaddenstoeltje", "Sparkle Mushroom"), "pluisbolletje": ("Pluisbolletje", "Fluff Puff"),
    "pluiskussen": ("Pluiskussen", "Fluff Cushion"),
    "roze_knuffelsteen": ("Roze knuffelsteen", "Pink Plush Stone"), "viltkiezel": ("Viltkiezel", "Felt Gravel"),
}
ERTSNAAM = {"kolenerts": ("Kolenerts", "Coal Ore"), "kopererts": ("Kopererts", "Copper Ore"), "ijzererts": ("IJzererts", "Iron Ore"),
            "gouderts": ("Gouderts", "Gold Ore"), "lapiserts": ("Lapis-lazulierts", "Lapis Lazuli Ore"), "redstoneerts": ("Redstone-erts", "Redstone Ore"),
            "diamanterts": ("Diamanterts", "Diamond Ore"), "smaragderts": ("Smaragderts", "Emerald Ore"), "vadserts": ("Vadserts", "Vads Ore"),
            "kaasknabbelerts": ("Kaasknabbelerts", "Cheese Snack Ore"), "kaaskorrelerts": ("Kaaskorrelerts", "Cheese Grain Ore"),
            "pigmenterts": ("Pigmenterts", "Pigment Ore")}
LAAGNAAM = {"vilt": ("Viltsteen-", "Felt Stone "), "knuffel": ("Knuffelsteen-", "Plush Stone "), "kristal": ("Guhkristalsteen-", "Guh Crystal Stone ")}


def texts(h):
    for b, (nl, en) in NAMEN.items():
        h.lang(f"block.guhs.{b}", en, nl)
    for soort, (nl, en) in ERTSNAAM.items():
        for laag, (lnl, len_) in LAAGNAAM.items():
            h.lang(f"block.guhs.{soort}_{laag}", f"{len_}{en}", f"{lnl}{nl[0].lower()}{nl[1:]}" if not nl.startswith("IJ") else f"{lnl}ijzererts")
    h.lang("item.guhs.vahoegiet", "Vahoegite", "Vahoegiet")
    h.lang("item.guhs.glitterbes", "Glitter Berry", "Glitterbes")
    h.lang("biome.guhs.glitterspelonk", "Glitter Cavern", "Glitterspelonk")
    h.lang("biome.guhs.pluisgrot", "Fluff Cave", "Pluisgrot")
    h.lang("biome.guhs.slaapgrot", "Sleepy Cave", "Slaapgrot")
    h.lang("biome.guhs.knuffelwortelgrot", "Plush Root Cave", "Knuffelwortelgrot")
    h.lang("biome.guhs.belletjesgrot", "Little Bells Cave", "Belletjesgrot")
    h.lang("biome.guhs.kristalpegelgrot", "Crystal Icicle Cave", "Kristalpegelgrot")
    # tooltips (the guhs mod shows block.<id>.lore under the name)
    lore = {
        "knuffelnaad": ("The seam at the very bottom of the Guhmension. Nothing gets through, njeg.",
                        "De naad helemaal onderin de Guhmension. Daar kom je niet doorheen, njeg."),
        "guhklei": ("Gives clay balls (they bake into bricks). Bake the block itself for pink terracotta.",
                    "Geeft kleiballetjes (die bak je tot bakstenen). Bak het blok zelf tot roze terracotta."),
        "glitterrank_bes": ("Right-click to pick the glitter berries. They grow back on their own.",
                            "Rechtsklik om de glitterbessen te plukken."),
        "pluiskussen": ("Land on it softly: no fall damage and a little bounce.", "Zacht landen: geen valschade en een klein stuitertje."),
    }
    for b, (en, nl) in lore.items():
        h.lang(f"block.guhs.{b}.lore", en, nl)


# =====================================================================================================================
# the terrain
# =====================================================================================================================
def _vg(naam, onder, boven):
    return {"type": "minecraft:vertical_gradient", "random_name": f"guhs:{naam}",
            "true_at_and_below": {"absolute": onder}, "false_at_and_above": {"absolute": boven}}


def _blok(name):
    return {"type": "minecraft:block", "result_state": {"Name": name}}


def _als(cond, rule):
    return {"type": "minecraft:condition", "if_true": cond, "then_run": rule}


def lagen_regel():
    """The layers (ronde 27): Java's guhs:onderwol_lagen (feature/onderwol/OnderWolLagen) measures from each column's real top,
    lets the borders follow the land, keeps the top blocks for the biome and adds the knuffelsteen's pink; not in the
    Gatenkaasgrotten (their rock is gatenkaas). The knuffelnaad at the very bottom always."""
    geen_gatenkaas = {"type": "minecraft:not", "invert": {"type": "minecraft:biome", "biome_is": ["guhs:gatenkaasgrotten"]}}
    return _als(geen_gatenkaas, {"type": "guhs:onderwol_lagen"}), _als(_vg("onderwol_naad_altijd", 0, 1), _blok("guhs:knuffelnaad"))


def _noise(name, xz=1.0, y=1.0):
    return {"type": "minecraft:noise", "noise": name, "xz_scale": xz, "y_scale": y}


def _interp(f):
    return {"type": "minecraft:interpolated", "argument": f}


def _max(a, b):
    return {"type": "minecraft:max", "argument1": a, "argument2": b}


def _min(a, b):
    return {"type": "minecraft:min", "argument1": a, "argument2": b}


def grotten(vast):
    """The caves (density: below 0 is air). Cheese caves more and bigger the deeper (none above ~y 47), closing up towards
    the knuffelnaad each along its own noise (no flat floors), spaghetti a bit rarer in the wool, vanilla's entrances (the
    cave mouths). Everything sits in one interpolated, as in vanilla: the final density is filled per cell without moving the cache_once counter, so a cache_once or a noise
    outside interpolation is either frozen per 8x4x8 cell or paid per block. vast: the terrain without caves (a cache_once
    the terrain's own interpolated shares); where it is clearly air (a few blocks above the land) no cave noise is worked
    out at all, which also spares the structure searches (getBaseHeight) most of the caves' cost."""
    kaas = kw.add({"type": "minecraft:clamp", "min": -1.0, "max": 1.0, "input": _noise("minecraft:cave_cheese", 1.0, 0.6666666666666666)},
                  kw.add(kw.gradient(18, 30, 0.24, 0.46), kw.gradient(42, 48, 0.0, 1.6)))
    mond = kw.mul(5.0, "minecraft:overworld/caves/entrances")
    spaghetti = kw.add(kw.add("minecraft:overworld/caves/spaghetti_2d", "minecraft:overworld/caves/spaghetti_roughness_function"),
                       kw.gradient(60, 74, 0.0, 0.25))
    dicht = kw.gradient(5, 14, 0.7, 0.0)
    # never in the bottom blocks, never under the seas (the sea noise of the Knuffeldal module: 1 away from the seas)
    wacht = _max(kw.gradient(4, 6, 2.0, -10.0), kw.add(-10.0, kw.mul(kw.add(1.0, kw.mul(kw.geen_zee(), -1.0)), 12.0)))
    holtes = _max(kw.add(_min(_min(kaas, mond), spaghetti), dicht), wacht)
    # (no noodle caves: vanilla's noodle has interpolators of its own, so its four noises would be worked out at every cell
    # corner, in the air too: measured, the structure searches took 3x as long as in 1.6.4)
    return _interp({"type": "minecraft:range_choice", "input": vast, "min_inclusive": -1000000.0, "max_exclusive": LUCHT,
                    "when_in_range": 1.0, "when_out_of_range": holtes})


def router(d):
    r = d["noise_router"]
    final = r["final_density"]
    assert final.get("type") == "guhs:bio_terrein", "onderwol must come after bio_wereld"
    binnen = final["input"]
    if binnen["type"] == "minecraft:range_choice" and binnen["input"]["type"] == "minecraft:cache_once":   # (the first test build's shape)
        binnen = final["input"] = binnen["input"]["argument"]
    if binnen["type"] == "minecraft:min" and "minecraft:overworld/caves/entrances" in json.dumps(binnen["argument2"]):   # (idempotent)
        binnen = binnen["argument1"]
    assert binnen["type"] == "minecraft:max" and binnen["argument1"]["type"] == "minecraft:interpolated", "unexpected final_density"
    if binnen["argument1"]["argument"]["type"] == "minecraft:cache_once":   # (idempotent)
        binnen["argument1"]["argument"] = binnen["argument1"]["argument"]["argument"]
    terrein = json.loads(json.dumps(binnen["argument1"]))
    # the terrain value is shared (inside interpolation) with the caves' check "is this solid?"; below y 30 the bottom of
    # the Guhmension (binnen's second half) is solid whatever the terrain says
    gedeeld = {"type": "minecraft:cache_once", "argument": binnen["argument1"]["argument"]}
    binnen["argument1"]["argument"] = gedeeld
    vast = _max(gedeeld, {"type": "minecraft:y_clamped_gradient", "from_y": 29, "to_y": 31, "from_value": 1.0, "to_value": -1.0})
    # (guhs:grotten only marks the term: the quick height probe of the searches, ZoekHoogte, leaves it out)
    final["input"] = _min(binnen, {"type": "guhs:grotten", "argument": grotten(vast)})
    # the preliminary surface is the real terrain now (was a constant), with the biomes3 regions' own terrain model laid over
    # it (ronde 27: the Klaterdal's floors lie far below the plain terrain); OnderWolLagen lets the layer borders follow it
    r["preliminary_surface_level"] = {"type": "minecraft:find_top_surface",
                                      "density": {"type": "guhs:bio_terrein", "input": terrein["argument"], "ruis": final["ruis"]},
                                      "upper_bound": 256, "lower_bound": 0, "cell_height": 4}
    # (idempotent: take a shift from an earlier run off first; it is the second half of an add)
    for k in ("depth", "continents", "erosion", "ridges"):
        if "guhs:onderwol_" in json.dumps(r[k]):
            r[k] = r[k]["argument1"]
    # the cave biomes: deep inside the rock (as the Gatenkaasgrotten' "deep"), in their own band of height, where their noise
    # is high, depth goes up by SHIFT and their own climate axis up or down by SHIFT (see GROTTEN)
    rots = {"type": "minecraft:clamp", "min": 0.0, "max": 1.0, "input": kw.mul(kw.add(terrein, -0.28), 6.0)}
    # (the Knuffelwortelgrot only under the forests: where vegetation is high, as the Vadswoud and the Bleekwoud)
    bos = {"type": "minecraft:clamp", "min": 0.0, "max": 1.0, "input": kw.mul(kw.add(json.loads(json.dumps(r["vegetation"])), -0.35), 5.0)}

    def band(onder, boven):
        return kw.mul(kw.gradient(onder - 3, onder + 3, 0.0, 1.0), kw.gradient(boven - 3, boven + 3, 1.0, 0.0))

    def sel(noise, vanaf):
        return {"type": "minecraft:clamp", "min": 0.0, "max": 1.0, "input": kw.mul(kw.add(_noise(f"guhs:{noise}", 1.0, 1.0), -vanaf), 8.0)}
    alle, assen = None, {}
    for biome, (noise, vanaf, (onder, boven), as_, teken) in GROTTEN.items():
        f = kw.mul(band(onder, boven), sel(noise, vanaf))
        if biome == "knuffelwortelgrot":
            f = kw.mul(f, bos)
        alle = f if alle is None else kw.add(alle, f)
        term = f if teken > 0 else kw.mul(f, -1.0)
        assen[as_] = term if as_ not in assen else kw.add(assen[as_], term)
    # (the terrain check "inside the rock" once per axis: it is the dearest part)
    r["depth"] = kw.add(r["depth"], kw.mul(kw.mul(alle, rots), SHIFT))
    for as_, term in assen.items():
        r[as_] = kw.add(r[as_], kw.mul(kw.mul(term, rots), SHIFT))


def surface(d):
    rules = d["surface_rule"]["sequence"]
    lagen, naad = lagen_regel()
    # (idempotent; also takes out the first test builds' rule, the y borders under the preliminary surface)
    rules[:] = [r for r in rules if not any(m in json.dumps(r) for m in ("onderwol_laag_", "guhs:onderwol_lagen", "guhs:onderwol_naad"))]
    rules.insert(0, lagen)
    rules.insert(0, naad)


def biome_source(d):
    """The cave biomes' climate points lie beyond what the noises reach by themselves (only the shift of router() gets
    there); the offset keeps them from winning by default where depth is naturally high, at the bottom of the world."""
    alles = [-2.0, 2.0]
    entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] not in g(*BIOMES)]
    for biome, (_, _, _, as_, teken) in GROTTEN.items():
        p = {"temperature": alles, "humidity": alles, "continentalness": alles, "erosion": alles, "weirdness": alles, "depth": 2.0, "offset": 1.0}
        p[KLIMAAT[as_]] = 2.0 * teken
        entries.append({"biome": f"guhs:{biome}", "parameters": p})
    d["generator"]["biome_source"]["biomes"] = entries


def _hoogte(lo, hi, piek=None):
    if piek is None:
        return {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}
    return {"type": "minecraft:trapezoid", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}


# ore: (layers, size, count, lowest y, highest y, triangular?, discard chance on air)
ERTS_PLAATSING = {
    "kolenerts": (["vilt", "knuffel"], 14, 7, 44, 72, True, 0.0),
    "kopererts": (["vilt", "knuffel"], 10, 6, 38, 62, True, 0.0),
    "ijzererts": (["vilt", "knuffel", "kristal"], 9, 9, 20, 60, True, 0.0),
    "lapiserts": (["knuffel", "kristal"], 7, 4, 16, 44, True, 0.0),
    "gouderts": (["knuffel", "kristal"], 9, 6, 5, 36, True, 0.0),
    "redstoneerts": (["kristal"], 8, 6, 5, 20, False, 0.0),
    "diamanterts": (["kristal"], 6, 5, 5, 16, False, 0.5),
    "smaragderts": (["vilt", "knuffel"], 3, 5, 30, 64, False, 0.0),
    "vadserts": (["knuffel", "kristal"], 6, 8, 8, 34, True, 0.0),
    "kaasknabbelerts": (["vilt", "knuffel"], 10, 6, 40, 66, True, 0.0),
    "kaaskorrelerts": (["knuffel", "kristal"], 8, 6, 5, 24, True, 0.0),
    "pigmenterts": (["knuffel", "kristal"], 8, 7, 18, 48, True, 0.0),
}
SMARAGD_BIOMES = g("guh_peaks", "vads_cliffs")


def features(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    for naam, noise in NOISES.items():
        w(f"{wg}/noise/{naam}.json", noise)
    # --- the ores ---
    for soort, (lagen, size, count, lo, hi, driehoek, lucht) in ERTS_PLAATSING.items():
        w(f"{wg}/configured_feature/onderwol_{soort}.json", {"type": "minecraft:ore", "config": {"size": size, "discard_chance_on_air_exposure": lucht,
            "targets": [{"target": {"predicate_type": "minecraft:block_match", "block": LAAGSTEEN[l]}, "state": {"Name": f"guhs:{soort}_{l}"}} for l in lagen]}})
        w(f"{wg}/placed_feature/onderwol_{soort}.json", {"feature": f"guhs:onderwol_{soort}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": _hoogte(lo, hi, driehoek or None)}, {"type": "minecraft:biome"}]})
    w(f"{wg}/configured_feature/onderwol_guhklei.json", {"type": "minecraft:ore", "config": {"size": 33, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": LAAGSTEEN[l]}, "state": {"Name": "guhs:guhklei"}} for l in ("vilt", "knuffel")]}})
    w(f"{wg}/placed_feature/onderwol_guhklei.json", {"feature": "guhs:onderwol_guhklei", "placement": [
        {"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(26, 62)},
        {"type": "minecraft:biome"}]})
    w(f"{wg}/configured_feature/onderwol_vahoegiet.json", {"type": "minecraft:ore", "config": {"size": 3, "discard_chance_on_air_exposure": 0.3, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": LAAGSTEEN["kristal"]}, "state": {"Name": "guhs:vahoegiet_erts"}}]}})
    w(f"{wg}/placed_feature/onderwol_vahoegiet.json", {"feature": "guhs:onderwol_vahoegiet", "placement": [
        {"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(5, 18)},
        {"type": "minecraft:biome"}]})
    # --- the knuffelhart geodes: knuffelsteen, guhkristalsteen, guhkristal with now and then vahoegiet, clusters inside ---
    geode = _mc("data/minecraft/worldgen/configured_feature/amethyst_geode.json")
    st = lambda n: {"type": "minecraft:simple_state_provider", "state": {"Name": n}}  # noqa: E731
    geode["config"]["blocks"].update({"filling_provider": st("minecraft:air"), "inner_layer_provider": st("guhs:guh_kristal_blok"),
                                      "alternate_inner_layer_provider": st("guhs:vahoegiet_erts"), "middle_layer_provider": st("guhs:guh_kristalsteen"),
                                      "outer_layer_provider": st("guhs:knuffelsteen"),
                                      "inner_placements": [{"Name": "guhs:guh_kristal_cluster", "Properties": {"facing": "up"}}]})
    geode["config"]["use_alternate_layer0_chance"] = 0.06
    w(f"{wg}/configured_feature/onderwol_geode.json", geode)
    w(f"{wg}/placed_feature/onderwol_geode.json", {"feature": "guhs:onderwol_geode", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 20}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(8, 30)},
        {"type": "minecraft:biome"}]})
    # --- the saus springs (sausvallen): out of the cave walls in the knuffelsteen and guhkristalsteen ---
    w(f"{wg}/configured_feature/onderwol_sausval.json", {"type": "minecraft:spring_feature", "config": {
        "state": {"Name": "guhs:kaas_saus", "Properties": {"falling": "true"}}, "requires_block_below": True, "rock_count": 4, "hole_count": 1,
        "valid_blocks": ["guhs:knuffelsteen", "guhs:guh_kristalsteen", "guhs:viltsteen"]}})
    w(f"{wg}/placed_feature/onderwol_sausval.json", {"feature": "guhs:onderwol_sausval", "placement": [
        {"type": "minecraft:count", "count": 6}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(12, 44)},
        {"type": "minecraft:biome"}]})
    # --- the Java feature: the saus sea (once per chunk) ---
    w(f"{wg}/configured_feature/onderwol_sauszee.json", {"type": "guhs:onderwol_sauszee", "config": {}})
    w(f"{wg}/placed_feature/onderwol_sauszee.json", {"feature": "guhs:onderwol_sauszee", "placement": [{"type": "minecraft:biome"}]})
    for naam in ("configured_feature", "placed_feature"):   # (the round shafts of the first test build: replaced by the carvers)
        if os.path.exists(f"{wg}/{naam}/onderwol_zinkgat.json"):
            os.remove(f"{wg}/{naam}/onderwol_zinkgat.json")
    # --- the carvers: vanilla's winding caves and (rare) ravines, through the wool and the layers, so the underground now and
    # then opens up to the surface the way it does in the overworld (no lava: its level is under the world) ---
    h.add_tag("guhs/tags/block/onderwol/grot_vervangbaar", ["#minecraft:wool", "#guhs:onderwol/laagsteen", "#c:ores", *GROT_OPPERVLAK])
    for naam, bron, kans, hoogte in (("onderwol_grot", "cave", 0.14, {"type": "minecraft:uniform", "min_inclusive": {"absolute": 10},
                                                                           "max_inclusive": {"absolute": 180}}),
                                     ("onderwol_kloof", "canyon", 0.012, {"type": "minecraft:uniform", "min_inclusive": {"absolute": 24},
                                                                           "max_inclusive": {"absolute": 80}})):
        carver = _mc(f"data/minecraft/worldgen/configured_carver/{bron}.json")
        carver["config"].pop("debug_settings", None)
        carver["config"].update({"probability": kans, "replaceable": "#guhs:onderwol/grot_vervangbaar", "lava_level": {"absolute": -8}, "y": hoogte})
        w(f"{wg}/configured_carver/{naam}.json", carver)
    # --- the plants of the cave biomes ---
    scan = lambda richting: [  # noqa: E731
        {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(6, 66)},
        {"type": "minecraft:environment_scan", "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
         "direction_of_search": richting, "max_steps": 12, "target_condition": {"type": "minecraft:solid"}},
        {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1 if richting == "down" else -1}, {"type": "minecraft:biome"}]

    def gewogen(*paren):
        return {"type": "minecraft:weighted_state_provider", "entries": [{"data": {"Name": n}, "weight": wt} for n, wt in paren]}

    def patch(naam, grond, oppervlak, kans, plant, diepte=1):
        w(f"{wg}/configured_feature/{naam}.json", {"type": "minecraft:vegetation_patch", "config": {
            "depth": diepte, "extra_bottom_block_chance": 0.0, "extra_edge_column_chance": 0.3, "ground_state": st(grond),
            "replaceable": "#guhs:onderwol/laagsteen", "surface": oppervlak, "vegetation_chance": kans,
            "vegetation_feature": {"feature": plant, "placement": []}, "vertical_range": 5,
            "xz_radius": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 6}}})
    w(f"{wg}/configured_feature/onderwol_glitter_vloerplantjes.json", {"type": "minecraft:simple_block", "config": {"to_place": gewogen(
        ("guhs:lichtmos", 30), ("guhs:glinsterpaddenstoel", 10))}})
    w(f"{wg}/configured_feature/onderwol_glitterrank.json", {"type": "minecraft:block_column", "config": {
        "allowed_placement": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, "direction": "down", "prioritize_tip": True,
        "layers": [{"height": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 4}, "provider": st("guhs:glitterrank")},
                   {"height": 1, "provider": gewogen(("guhs:glitterrank_bes", 1), ("guhs:glitterrank", 2))}]}})
    w(f"{wg}/configured_feature/onderwol_pluis_vloerplantjes.json", {"type": "minecraft:simple_block", "config": {"to_place": gewogen(
        ("guhs:pluisbolletje", 6), ("guhs:pluiskussen", 1))}})
    patch("onderwol_glitter_vloer", "guhs:lichtmosblok", "floor", 0.6, "guhs:onderwol_glitter_vloerplantjes")
    patch("onderwol_glitter_plafond", "guhs:lichtmosblok", "ceiling", 0.35, "guhs:onderwol_glitterrank")
    patch("onderwol_pluis_vloer", "guhs:pluiswolblok", "floor", 0.35, "guhs:onderwol_pluis_vloerplantjes", diepte=2)
    patch("onderwol_pluis_plafond", "guhs:pluiswolblok", "ceiling", 0.0, "guhs:onderwol_pluis_vloerplantjes", diepte=2)
    # the crystal drip ponds: a little water on guhkristalsteen
    w(f"{wg}/configured_feature/onderwol_druppelvijver.json", {"type": "minecraft:waterlogged_vegetation_patch", "config": {
        "depth": 1, "extra_bottom_block_chance": 0.0, "extra_edge_column_chance": 0.5, "ground_state": st("guhs:guh_kristalsteen"),
        "replaceable": "#guhs:onderwol/laagsteen", "surface": "floor", "vegetation_chance": 0.0,
        "vegetation_feature": {"feature": "guhs:onderwol_glitter_vloerplantjes", "placement": []}, "vertical_range": 5,
        "xz_radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 3}}})
    for naam, n, richting in (("onderwol_glitter_vloer", 90, "down"), ("onderwol_glitter_plafond", 90, "up"), ("onderwol_pluis_vloer", 90, "down"),
                              ("onderwol_pluis_plafond", 70, "up"), ("onderwol_druppelvijver", 10, "down")):
        w(f"{wg}/placed_feature/{naam}.json", {"feature": f"guhs:{naam}", "placement": [{"type": "minecraft:count", "count": n}, *scan(richting)]})


ERTS_IDS = [f"guhs:onderwol_{s}" for s in ERTS_PLAATSING if s != "smaragderts"] + ["guhs:onderwol_guhklei", "guhs:onderwol_vahoegiet"]
WEG = {"guhs:vahoege_vads_ore", "guhs:vahoege_vads_ore_rich"}   # the old vads lumps in the wool: vads lives in the stone now
STAP2 = ["guhs:onderwol_geode", "guhs:onderwol_sauszee"]
STAP8 = ["guhs:onderwol_sausval"]
STAP9 = ["guhs:onderwol_druppels"]   # (ronde 28: rozenkwarts-druppels in the knuffelsteen caves, onderwol_grot.py)
CARVERS = ["guhs:onderwol_grot", "guhs:onderwol_kloof"]
GROT_SFEER = {   # fog, sky, ambient particle, its chance
    "slaapgrot": ("#efd8f0", "#f6e8ff", {"type": "minecraft:white_ash"}, 0.006),
    "knuffelwortelgrot": ("#f0c8d4", "#f8d8e4", {"type": "minecraft:spore_blossom_air"}, 0.004),
    "belletjesgrot": ("#dcd0f6", "#e8e0ff", {"type": "minecraft:glow"}, 0.003),
    "kristalpegelgrot": ("#f0b0cc", "#ffc0dc", {"type": "minecraft:dripping_dripstone_water"}, 0.002),
}
GROT_DECO = {   # per biome: generation step -> features (the new caves' own: onderwol_grot.py)
    "slaapgrot": {9: ["guhs:onderwol_pluis_vloer"]},
    "knuffelwortelgrot": {9: ["guhs:onderwol_knuffelwortel_plafond", "guhs:onderwol_knuffelwortel_vloer"]},
    "belletjesgrot": {7: ["guhs:onderwol_belletjes"]},
    "kristalpegelgrot": {7: ["guhs:onderwol_kristalpegel_cluster", "guhs:onderwol_kristalpegel"]},
}
ZEE_BIOMES = g("guh_sea", "diepe_guhzee")   # (no carvers: they would cut holes under the water)
# the surface blocks of the Guhmension's biomes the carvers may cut through (besides wool and the layers)
GROT_OPPERVLAK = ["minecraft:grass_block", "minecraft:dirt", "minecraft:sand", "minecraft:sandstone", "minecraft:clay", "minecraft:snow_block",
                  "minecraft:pink_terracotta", "minecraft:magenta_terracotta", "minecraft:pink_concrete_powder", "guhs:rijpgras", "guhs:knuffelgras",
                  "guhs:vadsmos", "guhs:bleekmos", "guhs:kaasmodder", "guhs:modderig_kaasgras", "guhs:gladde_knuffelsteen", "guhs:guhklei",
                  "guhs:block_of_kaasknabbels"]


def biomes(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    dim = json.load(open(f"{D}/dimension/guhmension.json", encoding="utf-8"))
    ids = sorted({e["biome"] for e in dim["generator"]["biome_source"]["biomes"]} - set(g(*BIOMES)) - {"guhs:gatenkaasgrotten"})

    def voeg_toe(f, biome_id):
        while len(f) < 11:
            f.append([])
        f[6] = [x for x in f[6] if x not in WEG]
        f[2] = [x for x in f[2] if x != "guhs:onderwol_zinkgat"]
        for stap, lijst in ((2, STAP2), (6, ERTS_IDS + (["guhs:onderwol_smaragderts"] if biome_id in SMARAGD_BIOMES else [])), (8, STAP8), (9, STAP9)):
            for x in lijst:
                if x not in f[stap]:
                    f[stap].append(x)
    for b in ids:
        pad = f"{wg}/biome/{b.split(':')[1]}.json"
        if not os.path.exists(pad):
            continue
        h.patch_json(pad, lambda d, b=b: voeg_toe(d["features"], b))
        if b not in ZEE_BIOMES:
            h.patch_json(pad, lambda d: d.update(carvers=d.get("carvers", []) + [c for c in CARVERS if c not in d.get("carvers", [])]))
    # the two cave biomes (written in the 26.1 shape)
    kristal = json.load(open(f"{wg}/biome/guh_kristalmijn.json", encoding="utf-8"))
    leeg = {k: [] for k in kristal["spawners"]}
    muziek = kristal.get("attributes", {}).get("minecraft:audio/background_music")   # (older modules write the old shape; mc26 converts it later)

    def biome(fog, sky, deeltje, kans, planten):
        f = [[] for _ in range(11)]
        voeg_toe(f, "")
        for stap, lijst in (planten.items() if isinstance(planten, dict) else ((9, planten),)):
            f[stap] = f[stap] + [x for x in lijst if x not in f[stap]]
        # (the sounds and the music: onderwol_geluid, which runs after this)
        attrs = {"minecraft:visual/fog_color": fog, "minecraft:visual/sky_color": sky, "minecraft:visual/water_fog_color": "#e0709a",
                 "minecraft:visual/ambient_particles": [{"particle": deeltje, "probability": kans}]}
        if muziek:
            attrs["minecraft:audio/background_music"] = muziek
        return {"attributes": attrs, "has_precipitation": False, "temperature": 0.8, "downfall": 0.0,
                "effects": {"water_color": "#f7a8c8", "foliage_color": "#f7b6cb", "grass_color": "#f7b6cb"},
                "creature_spawn_probability": 0.1, "spawners": leeg, "spawn_costs": {}, "carvers": [], "features": f}
    w(f"{wg}/biome/glitterspelonk.json", biome("#f2a0d8", "#ffb0dd", {"type": "minecraft:glow"}, 0.004,
                                              ["guhs:onderwol_glitter_vloer", "guhs:onderwol_glitter_plafond", "guhs:onderwol_druppelvijver"]))
    w(f"{wg}/biome/pluisgrot.json", biome("#f8e6ee", "#fff0f6", {"type": "minecraft:white_ash"}, 0.02,
                                         ["guhs:onderwol_pluis_vloer", "guhs:onderwol_pluis_plafond"]))
    # (ronde 28) the four new ones; their decoration: GROT_DECO (features of onderwol_grot.py)
    for b, (fog, sky, deeltje, kans) in GROT_SFEER.items():
        w(f"{wg}/biome/{b}.json", biome(fog, sky, deeltje, kans, GROT_DECO[b]))
    # the cave biomes may hold the same underground structures as the Guhmension around them
    tagdir = f"{D}/tags/worldgen/biome/has_structure"
    if os.path.isdir(tagdir):
        for f in os.listdir(tagdir):
            t = json.load(open(os.path.join(tagdir, f), encoding="utf-8"))
            if "guhs:guh_kristalmijn" in t.get("values", []) or "guhs:gatenkaasgrotten" in t.get("values", []):
                h.add_tag(f"guhs/tags/worldgen/biome/has_structure/{f[:-5]}", g(*BIOMES))
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({b: {"villager_type": "guhs:guh"} for b in g(*BIOMES)}))


def worldgen(h):
    D = h.D
    features(h)
    h.patch_json(f"{D}/worldgen/noise_settings/guhmension.json", router)
    h.patch_json(f"{D}/worldgen/noise_settings/guhmension.json", surface)
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)
    biomes(h)


def selfcheck(h):
    D, A = h.D, h.A
    problemen = []
    for b in VILT + ["knuffelnaad", "guhklei", "vahoegiet_erts", "lichtmosblok", "lichtmos", "pluiskussen"] + ERTSEN + PLANTEN:
        if not os.path.exists(f"{A}/textures/block/{b}.png"):
            problemen.append(f"texture missing: {b}")
        if b != "knuffelnaad" and not os.path.exists(f"{D}/loot_table/blocks/{b}.json"):   # (unbreakable: no loot)
            problemen.append(f"loot table missing: {b}")
    d = json.load(open(f"{D}/worldgen/noise_settings/guhmension.json", encoding="utf-8"))
    if json.dumps(d["noise_router"]["final_density"]).count("minecraft:overworld/caves/entrances") != 1:
        problemen.append("the caves are missing from (or twice in) the final density")
    if problemen:
        raise SystemExit("onderwol self-check failed:\n  " + "\n  ".join(problemen))


def build(h):
    tex.textures(h)
    tex2.textures(h)   # (ronde 27-28: the new terrain and cave blocks)
    blocks_and_items(h)
    loot(h)
    tags(h)
    recipes(h)
    texts(h)
    worldgen(h)
    geluid.build(h)   # (ronde 28: no scary cave sounds in the Guhmension; every cave biome its own cheerful music)
    selfcheck(h)


# =====================================================================================================================
# on its own (the way the slices since 1.5 run): python tools/features/onderwol.py from the project root. It writes
# only its own files, patches the shared Guhmension files in place (all patches are idempotent), writes the 26.1 item
# definitions, and merges its texts into nl_nl.json / en_us.json. A full make_resources run is NOT safe on this tree
# (later hand fixes, e.g. render types, are not in every generator).
# =====================================================================================================================
def _direct():
    import types
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
    NL, EN = {}, {}

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def patch_json(path, fn):
        data = json.load(open(path, encoding="utf-8"))
        fn(data)
        w(path, data)

    def add_tag(path, values):
        full = f"{R}/data/{path}.json"
        data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
        for v in values:
            if v not in data["values"]:
                data["values"].append(v)
        w(full, data)

    def simple_block(name, tex_=None, render_type=None):
        model = {"parent": "minecraft:block/cube_all", "textures": {"all": tex_ or f"guhs:block/{name}"}}
        if render_type:
            model["render_type"] = render_type
        w(f"{A}/models/block/{name}.json", model)
        w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})
        w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})

    def item_model(name, tex_=None, parent="minecraft:item/generated"):
        w(f"{A}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": tex_ or f"guhs:item/{name}"}})

    def self_drop(name, item=None):
        w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{item or name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    def shaped(name, pattern, key, result, count=1):
        w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": dict(key),
                                      "result": {"id": result, "count": count}})

    def lang(key, en, nl):
        EN[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=R, A=A, D=D, w=w, patch_json=patch_json, add_tag=add_tag, simple_block=simple_block,
                              item_model=item_model, self_drop=self_drop, shaped=shaped, lang=lang)
    build(h)
    # the 26.1 item definitions (mc26.py makes these in a full run)
    items = VILT + [f"{n}_trap" for n in TRAPPEN] + [f"{n}_plaat" for n in TRAPPEN] + [f"{n}_muur" for n in MUREN] + \
        [*NIEUW27, "knuffelnaad", "guhklei", *ERTSEN, "vahoegiet_erts", "lichtmosblok", "lichtmos", *PLANTEN, "pluiskussen", "vahoegiet", "glitterbes"]
    for i in items:
        w(f"{A}/items/{i}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{i}"}})
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    print(f"onderwol: {len(items)} items, {len(NL)} texts")


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
