"""
einde2 (the Guheinde expansion), slice biomes: the six biomes of the outer islands of the Guheinde.
Java: nl.juiced.guhs.feature.einde2.biomes (E2BiomesFeature). Textures: einde2_biomes_tex.py; the sounds (committed OGGs):
einde2_biomes_geluid.py; the wiki texts: einde2_biomes_wiki.py.

  - the layout: the biome source guhs:guheinde_biomes (GuheindeBiomeSource.java) in data/guhs/dimension/guheinde.json, and
    its two slow noises as the temperature and the vegetation of the dimension's noise router. The main island keeps
    guhs:guheinde. Two common biomes (Gatenkaasheuvels, Korstwoestijn) with four rare, small ones inside them
    (Kruimelwolken, Schimmelkaasvlakte, Kristalkaasklippen, Knabbelkoorbos);
  - the TERRAIN IS NOT TOUCHED: final_density, the noise size and the default block stay what features/guheinde.py wrote;
    only the surface rule changes (a biome-conditioned sequence that ends in the old kaaskorst rule) and everything else is
    features (STEP_ORDER / BIOME_FEATURES: one global order per step, as the Barbecuether has it). The unpatched noise
    settings are kept as guhs:einde2_biomes_oud, for the game test that proves the terrain did not move;
  - the blocks of each biome with their models, loot, recipes and tags, the kristalkaas (the ore of the outer islands)
    and the knabbelkoor chain: plant, fruit, popped fruit, the building blocks made of it and the lichtstaafje;
  - advancements, FTB quests (a section of its own in the Guheinde chapter), texts.

build(h) makes everything (h = make_v2); it runs after features/guheinde.py and patches what that wrote.
"""
import json
import os
import zipfile

import features.einde2_kit as kit
from features import einde2_biomes_tex as tex
from features.barbecuether import BIOME, SQUARE, cf, count, patch, pf, stair_states, uniform, wall_multipart

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "De zes streken"

# id: (Dutch name, fog, sky, water, water fog, ambient particle)
BIOMES = {
    "gatenkaasheuvels": ("Gatenkaasheuvels", 0x4A3416, 0x2E1A26, 0xF2B84A, 0x7A4A12,
                         {"options": {"type": "minecraft:dust", "color": [1.0, 0.84, 0.32], "scale": 1.0}, "probability": 0.0025}),
    "korstwoestijn": ("Korstwoestijn", 0x5C3A1C, 0x34201E, 0x58C8D8, 0x1E6A78,
                      {"options": {"type": "minecraft:dust", "color": [0.86, 0.66, 0.36], "scale": 0.7}, "probability": 0.006}),
    "kruimelwolken": ("Kruimelwolken", 0x6E5E6A, 0x4A3A52, 0xF08CC0, 0x6A2A50,
                      {"options": {"type": "minecraft:white_ash"}, "probability": 0.012}),
    "schimmelkaasvlakte": ("Schimmelkaasvlakte", 0x2A5A60, 0x183840, 0x62D2C6, 0x1E5E64,
                           {"options": {"type": "minecraft:warped_spore"}, "probability": 0.018}),
    "kristalkaasklippen": ("Kristalkaasklippen", 0x54380E, 0x30200E, 0xF2B84A, 0x7A4A12,
                           {"options": {"type": "minecraft:dust", "color": [1.0, 0.9, 0.45], "scale": 0.6}, "probability": 0.004}),
    "knabbelkoorbos": ("Knabbelkoorbos", 0x3C1A50, 0x241030, 0xD88CF0, 0x4A2A6A,
                       {"options": {"type": "minecraft:dust", "color": [0.82, 0.48, 0.96], "scale": 0.8}, "probability": 0.004}),
}
MIKA_GEWICHT = 2          # (the main island has 4: the outer islands are the quiet part)

# --- the layout (GuheindeBiomeSource): tuned with E2BiomesGameTests.einde2BiomesStreken, which prints the shares ---
DREMPEL = 0.46
STREEK = {"firstOctave": -8, "amplitudes": [1.0, 0.5]}
ZELDZAAM = {"firstOctave": -7, "amplitudes": [1.0, 0.5]}
XZ_SCHAAL = 0.25

STENEN = ["kruimelrots_stenen", "schimmelkaas_stenen", "klipkaas_stenen"]             # each with _trap, _plaat, _muur
STEEN_VAN = {"kruimelrots_stenen": "kruimelrots", "schimmelkaas_stenen": "schimmelkaas", "klipkaas_stenen": "klipkaas"}
SIMPLE = ["kruimelrots", "gebarsten_korst", "kruimelzand", "dichte_kruimelwolk", "schimmelkaas", "schimmelhoed", "klipkaas",
          "kristalkaas_erts", "kristalkaas_blok", "koorkorst", "knabbelkoorblok"] + STENEN
PILLARS = ["schimmelsteel", "knabbelkoorpilaar"]
PLANTS = ["korstgras", "dorre_korststruik", "schimmelpaddenstoel"]
ALL_BLOCKS = SIMPLE + PILLARS + PLANTS + [f"{s}_{d}" for s in STENEN for d in ("trap", "plaat", "muur")] + [
    "kruimelzandlaag", "kruimelwolk", "kristalkaas_kristal", "knabbelkoorplant", "knabbelkoorbloem", "knabbelkoorblok_trap",
    "knabbelkoorblok_plaat", "lichtstaafje"]
ITEMS = ["kristalkaas", "kristalkaas_brokje", "knabbelkoorvrucht", "gepofte_knabbelkoorvrucht"]
# what the creatures of the outer islands spawn on and the features grow on (with guhs:kaaskorst, from features/einde2.py)
EILANDGROND = ["gebarsten_korst", "kruimelzand", "schimmelkaas", "klipkaas", "koorkorst", "kruimelrots"]

TEXTS = {
    # --- blocks ---
    "block.guhs.korstgras": "Korstgras",
    "block.guhs.kruimelrots": "Kruimelrots",
    "block.guhs.kruimelrots_stenen": "Kruimelrotsstenen",
    "block.guhs.kruimelrots_stenen_trap": "Kruimelrotsstenen trap",
    "block.guhs.kruimelrots_stenen_plaat": "Kruimelrotsstenen plaat",
    "block.guhs.kruimelrots_stenen_muur": "Kruimelrotsstenen muur",
    "block.guhs.gebarsten_korst": "Gebarsten korst",
    "block.guhs.kruimelzand": "Kruimelzand",
    "block.guhs.kruimelzandlaag": "Laagje kruimelzand",
    "block.guhs.dorre_korststruik": "Dorre korststruik",
    "block.guhs.kruimelwolk": "Kruimelwolk",
    "block.guhs.dichte_kruimelwolk": "Dichte kruimelwolk",
    "block.guhs.schimmelkaas": "Schimmelkaas",
    "block.guhs.schimmelkaas_stenen": "Schimmelkaasstenen",
    "block.guhs.schimmelkaas_stenen_trap": "Schimmelkaasstenen trap",
    "block.guhs.schimmelkaas_stenen_plaat": "Schimmelkaasstenen plaat",
    "block.guhs.schimmelkaas_stenen_muur": "Schimmelkaasstenen muur",
    "block.guhs.schimmelpaddenstoel": "Schimmelpaddenstoel",
    "block.guhs.schimmelhoed": "Schimmelhoed",
    "block.guhs.schimmelsteel": "Schimmelsteel",
    "block.guhs.klipkaas": "Klipkaas",
    "block.guhs.klipkaas_stenen": "Klipkaasstenen",
    "block.guhs.klipkaas_stenen_trap": "Klipkaasstenen trap",
    "block.guhs.klipkaas_stenen_plaat": "Klipkaasstenen plaat",
    "block.guhs.klipkaas_stenen_muur": "Klipkaasstenen muur",
    "block.guhs.kristalkaas_erts": "Kristalkaaserts",
    "block.guhs.kristalkaas_kristal": "Kristalkaaskristal",
    "block.guhs.kristalkaas_blok": "Blok kristalkaas",
    "block.guhs.koorkorst": "Koorkorst",
    "block.guhs.knabbelkoorplant": "Knabbelkoorplant",
    "block.guhs.knabbelkoorbloem": "Knabbelkoorbloem",
    "block.guhs.knabbelkoorblok": "Knabbelkoorblok",
    "block.guhs.knabbelkoorpilaar": "Knabbelkoorpilaar",
    "block.guhs.knabbelkoorblok_trap": "Knabbelkoorblok trap",
    "block.guhs.knabbelkoorblok_plaat": "Knabbelkoorblok plaat",
    "block.guhs.lichtstaafje": "Lichtstaafje",
    # --- items ---
    "item.guhs.kristalkaas": "Kristalkaas",
    "item.guhs.kristalkaas_brokje": "Kristalkaasbrokje",
    "item.guhs.knabbelkoorvrucht": "Knabbelkoorvrucht",
    "item.guhs.knabbelkoorvrucht.lore": "Hap... floep! Je staat ineens een stukje verder, njeg",
    "item.guhs.gepofte_knabbelkoorvrucht": "Gepofte knabbelkoorvrucht",
    # --- sounds ---
    "subtitles.guhs.einde2_biomes.knabbelkoor_zing": "Knabbelkoor zingt zachtjes",
    "subtitles.guhs.einde2_biomes.vrucht_flits": "Floep!",
    "subtitles.guhs.einde2_biomes.wolk_plof": "Iemand ploft in een wolk",
}
SOUNDS = {"einde2_biomes.knabbelkoor_zing": ["zing1", "zing2", "zing3"], "einde2_biomes.vrucht_flits": ["flits"],
          "einde2_biomes.wolk_plof": ["plof1", "plof2"]}


# =====================================================================================================================
# blocks and items: models, loot, tags, recipes
# =====================================================================================================================
def _vanilla_json(path):
    with zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")) as jar:
        return jar.read(path).decode("utf-8")


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    for b in SIMPLE:
        h.simple_block(b)
    h.simple_block("kruimelwolk", render_type="minecraft:translucent")
    for b in PILLARS:
        w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cube_column", "textures": {"end": f"guhs:block/{b}_top", "side": f"guhs:block/{b}"}})
        w(f"{A}/models/block/{b}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                    "textures": {"end": f"guhs:block/{b}_top", "side": f"guhs:block/{b}"}})
        w(f"{A}/blockstates/{b}.json", {"variants": {"axis=y": {"model": f"guhs:block/{b}"},
                                                      "axis=z": {"model": f"guhs:block/{b}_horizontal", "x": 90},
                                                      "axis=x": {"model": f"guhs:block/{b}_horizontal", "x": 90, "y": 90}}})
        w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
    for p in PLANTS:
        w(f"{A}/models/block/{p}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{p}"}})
        w(f"{A}/blockstates/{p}.json", {"variants": {"": {"model": f"guhs:block/{p}"}}})
        h.item_model(p, f"guhs:block/{p}")
    # stairs, slabs and walls of the three brick sets, stairs and slabs of the knabbelkoorblok
    for n in STENEN + ["knabbelkoorblok"]:
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
        if n in STENEN:
            for part, parent in (("post", "template_wall_post"), ("side", "template_wall_side"), ("side_tall", "template_wall_side_tall")):
                w(f"{A}/models/block/{n}_muur_{part}.json", {"parent": f"minecraft:block/{parent}", "textures": {"wall": t}})
            w(f"{A}/models/block/{n}_muur_inventory.json", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t}})
            w(f"{A}/blockstates/{n}_muur.json", {"multipart": wall_multipart(f"guhs:block/{n}_muur")})
            w(f"{A}/models/item/{n}_muur.json", {"parent": f"guhs:block/{n}_muur_inventory"})
    # the dune layers: the snow models in crumb sand (eight layers = a whole block of kruimelzand)
    zand = "guhs:block/kruimelzand"
    for i in range(1, 8):
        w(f"{A}/models/block/kruimelzandlaag_{i}.json", {"parent": f"minecraft:block/snow_height{i * 2}", "textures": {"particle": zand, "texture": zand}})
    w(f"{A}/blockstates/kruimelzandlaag.json", {"variants": {**{f"layers={i}": {"model": f"guhs:block/kruimelzandlaag_{i}"} for i in range(1, 8)},
                                                              "layers=8": {"model": "guhs:block/kruimelzand"}}})
    w(f"{A}/models/item/kruimelzandlaag.json", {"parent": "guhs:block/kruimelzandlaag_1"})
    # the kristalkaas cluster: the amethyst cluster's cross, turned to the side it grows on
    w(f"{A}/models/block/kristalkaas_kristal.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                     "textures": {"cross": "guhs:block/kristalkaas_kristal"}})
    m = {"model": "guhs:block/kristalkaas_kristal"}
    w(f"{A}/blockstates/kristalkaas_kristal.json", {"variants": {
        "facing=up": m, "facing=down": {**m, "x": 180}, "facing=north": {**m, "x": 90}, "facing=south": {**m, "x": 90, "y": 180},
        "facing=east": {**m, "x": 90, "y": 90}, "facing=west": {**m, "x": 90, "y": 270}}})
    h.item_model("kristalkaas_kristal", "guhs:block/kristalkaas_kristal")
    # the knabbelkoor: the vanilla chorus models with our textures (and the vanilla blockstate with our model names)
    kp = "guhs:block/knabbelkoorplant"
    for part in ("", "_side", "_noside", "_noside1", "_noside2", "_noside3"):
        w(f"{A}/models/block/knabbelkoorplant{part}.json", {"parent": f"minecraft:block/chorus_plant{part}",
                                                           "textures": {"texture": kp, "inside": kp, "particle": kp}})
    w(f"{A}/blockstates/knabbelkoorplant.json", json.loads(_vanilla_json("assets/minecraft/blockstates/chorus_plant.json")
                                                             .replace("minecraft:block/chorus_plant", "guhs:block/knabbelkoorplant")))
    w(f"{A}/models/item/knabbelkoorplant.json", {"parent": "guhs:block/knabbelkoorplant"})
    for model, texture in (("knabbelkoorbloem", "knabbelkoorbloem"), ("knabbelkoorbloem_dood", "knabbelkoorbloem_dood")):
        w(f"{A}/models/block/{model}.json", {"parent": "minecraft:block/template_chorus_flower",
                                             "textures": {"texture": f"guhs:block/{texture}", "bottom": kp, "particle": f"guhs:block/{texture}"}})
    w(f"{A}/blockstates/knabbelkoorbloem.json", {"variants": {**{f"age={i}": {"model": "guhs:block/knabbelkoorbloem"} for i in range(5)},
                                                               "age=5": {"model": "guhs:block/knabbelkoorbloem_dood"}}})
    w(f"{A}/models/item/knabbelkoorbloem.json", {"parent": "guhs:block/knabbelkoorbloem"})
    # the lichtstaafje: the end rod's model and blockstate
    w(f"{A}/models/block/lichtstaafje.json", {"parent": "minecraft:block/end_rod",
                                              "textures": {"end_rod": "guhs:block/lichtstaafje", "particle": "guhs:block/lichtstaafje"}})
    w(f"{A}/blockstates/lichtstaafje.json", json.loads(_vanilla_json("assets/minecraft/blockstates/end_rod.json")
                                                         .replace("minecraft:block/end_rod", "guhs:block/lichtstaafje")))
    w(f"{A}/models/item/lichtstaafje.json", {"parent": "guhs:block/lichtstaafje"})
    for i in ITEMS:
        h.item_model(i)


def loot(h):
    D, w = h.D, h.w
    silk = [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]
    eigen = {"kristalkaas_erts", "kristalkaas_kristal", "schimmelhoed", "knabbelkoorplant", "kruimelzandlaag"} | {f"{n}_plaat" for n in STENEN + ["knabbelkoorblok"]}
    for b in ALL_BLOCKS:
        if b not in eigen:
            h.self_drop(b)

    def alt(block, other, fns):
        w(f"{D}/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"guhs:{block}", "conditions": silk},
                {"type": "minecraft:item", "name": other, "functions": fns}]}]}]})
    fortuin = {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"}
    decay = {"function": "minecraft:explosion_decay"}
    # modest: one or two kristalkaas from the ore, one from a cluster (fortune counts for both)
    alt("kristalkaas_erts", "guhs:kristalkaas", [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}, fortuin, decay])
    alt("kristalkaas_kristal", "guhs:kristalkaas", [fortuin, decay])
    alt("schimmelhoed", "guhs:schimmelpaddenstoel", [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": -5, "max": 2}},
                                                    {"function": "minecraft:limit_count", "limit": {"min": 0}}, decay])
    # the stem gives a fruit now and then (as the chorus plant does)
    w(f"{D}/loot_table/blocks/knabbelkoorplant.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:knabbelkoorvrucht", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}, decay]}]}]})
    w(f"{D}/loot_table/blocks/kruimelzandlaag.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:kruimelzandlaag", "functions": [
            {"function": "minecraft:set_count", "count": i, "conditions": [{"condition": "minecraft:block_state_property",
                                                                             "block": "guhs:kruimelzandlaag", "properties": {"layers": str(i)}}]}
            for i in range(2, 9)] + [decay]}]}]})
    for n in STENEN + ["knabbelkoorblok"]:
        w(f"{D}/loot_table/blocks/{n}_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"guhs:{n}_plaat", "functions": [
                {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property",
                                                                                 "block": f"guhs:{n}_plaat", "properties": {"type": "double"}}]},
                decay]}]}]})


def tags(h):
    g = lambda names: [f"guhs:{n}" for n in names]  # noqa: E731
    trappen = [f"{n}_trap" for n in STENEN + ["knabbelkoorblok"]]
    platen = [f"{n}_plaat" for n in STENEN + ["knabbelkoorblok"]]
    muren = [f"{n}_muur" for n in STENEN]
    h.add_tag("minecraft/tags/block/mineable/pickaxe", g(
        ["kruimelrots", "gebarsten_korst", "schimmelkaas", "klipkaas", "kristalkaas_erts", "kristalkaas_kristal", "kristalkaas_blok", "koorkorst",
         "knabbelkoorblok", "knabbelkoorpilaar"] + STENEN + trappen + platen + muren))
    h.add_tag("minecraft/tags/block/mineable/shovel", g(["kruimelzand", "kruimelzandlaag"]))
    h.add_tag("minecraft/tags/block/mineable/axe", g(["schimmelhoed", "schimmelsteel", "knabbelkoorplant"]))
    h.add_tag("minecraft/tags/block/mineable/hoe", g(["dichte_kruimelwolk", "kruimelwolk"]))
    h.add_tag("minecraft/tags/block/needs_iron_tool", g(["kristalkaas_erts", "kristalkaas_blok"]))
    h.add_tag("minecraft/tags/block/crystal_sound_blocks", g(["kristalkaas_blok"]))
    for kind in ("block", "item"):
        h.add_tag(f"minecraft/tags/{kind}/stairs", g(trappen))
        h.add_tag(f"minecraft/tags/{kind}/slabs", g(platen))
        h.add_tag(f"minecraft/tags/{kind}/walls", g(muren))
    h.add_tag("guhs/tags/block/einde2/eilandgrond", g(EILANDGROND))
    h.add_tag("guhs/tags/block/einde2/wolk", g(["kruimelwolk", "dichte_kruimelwolk"]))
    # what a knabbelkoor stands on (Knabbelkoor.GROND): the ground of the islands, and its own building blocks (so a guh
    # gardener can grow one at home)
    h.w(f"{h.D}/tags/block/einde2/biomes_koorgrond.json", {"replace": False, "values": [
        "#guhs:einde2/eilandgrond", "guhs:knabbelkoorblok", "guhs:knabbelkoorpilaar", "minecraft:end_stone"]})
    # without these the vestingen and terugpoorten vanish from every new chunk of the outer islands (the natural islands of
    # the Kruimelwolken are not thinned, so it is in too)
    for structuur in ("mika_vesting", "guheinde_terugpoort"):
        h.add_tag(f"guhs/tags/worldgen/biome/has_structure/{structuur}", g(BIOMES))


def recipes(h):
    D = h.D
    for n in STENEN:
        steen = STEEN_VAN[n]
        h.shaped(n, ["SS", "SS"], {"S": f"guhs:{steen}"}, f"guhs:{n}", 4)
        h.shaped(f"{n}_trap", ["S  ", "SS ", "SSS"], {"S": f"guhs:{n}"}, f"guhs:{n}_trap", 4)
        h.shaped(f"{n}_plaat", ["SSS"], {"S": f"guhs:{n}"}, f"guhs:{n}_plaat", 6)
        h.shaped(f"{n}_muur", ["SSS", "SSS"], {"S": f"guhs:{n}"}, f"guhs:{n}_muur", 6)
        for src, out, c in ((steen, n, 1), (steen, f"{n}_trap", 1), (steen, f"{n}_plaat", 2), (steen, f"{n}_muur", 1),
                            (n, f"{n}_trap", 1), (n, f"{n}_plaat", 2), (n, f"{n}_muur", 1)):
            h.w(f"{D}/recipe/{out}_van_{src}_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"guhs:{src}"},
                                                                "result": {"id": f"guhs:{out}", "count": c}})
    # crumbs: four handfuls of sand press into a rock, three blocks of sand spread out into six layers, four clouds press
    # into a dense one
    h.shaped("kruimelrots_van_kruimelzand", ["ZZ", "ZZ"], {"Z": "guhs:kruimelzand"}, "guhs:kruimelrots")
    h.shaped("kruimelzandlaag", ["ZZZ"], {"Z": "guhs:kruimelzand"}, "guhs:kruimelzandlaag", 6)
    h.shaped("dichte_kruimelwolk", ["WW", "WW"], {"W": "guhs:kruimelwolk"}, "guhs:dichte_kruimelwolk")
    # kristalkaas: nine brokjes are one kristalkaas, nine kristalkaas one block (and back)
    h.shaped("kristalkaas_van_brokjes", ["BBB", "BBB", "BBB"], {"B": "guhs:kristalkaas_brokje"}, "guhs:kristalkaas")
    h.shapeless("kristalkaas_brokje", ["guhs:kristalkaas"], "guhs:kristalkaas_brokje", 9)
    h.shaped("kristalkaas_blok", ["KKK", "KKK", "KKK"], {"K": "guhs:kristalkaas"}, "guhs:kristalkaas_blok")
    h.shapeless("kristalkaas_van_blok", ["guhs:kristalkaas_blok"], "guhs:kristalkaas", 9)
    for kind, typ, time in (("smelten", "minecraft:smelting", 200), ("blazen", "minecraft:blasting", 100)):
        h.w(f"{D}/recipe/kristalkaas_van_erts_{kind}.json", {"type": typ, "category": "misc", "ingredient": {"item": "guhs:kristalkaas_erts"},
                                                             "result": {"id": "guhs:kristalkaas"}, "experience": 1.0, "cookingtime": time})
    # the knabbelkoor chain: pop the fruit, four popped fruits are four blocks, a popped fruit on a guhkristal is light
    h.w(f"{D}/recipe/gepofte_knabbelkoorvrucht.json", {"type": "minecraft:smelting", "category": "misc",
                                                       "ingredient": {"item": "guhs:knabbelkoorvrucht"},
                                                       "result": {"id": "guhs:gepofte_knabbelkoorvrucht"}, "experience": 0.1, "cookingtime": 200})
    h.shaped("knabbelkoorblok", ["PP", "PP"], {"P": "guhs:gepofte_knabbelkoorvrucht"}, "guhs:knabbelkoorblok", 4)
    h.shaped("knabbelkoorpilaar", ["P", "P"], {"P": "guhs:knabbelkoorblok_plaat"}, "guhs:knabbelkoorpilaar")
    h.shaped("knabbelkoorblok_trap", ["B  ", "BB ", "BBB"], {"B": "guhs:knabbelkoorblok"}, "guhs:knabbelkoorblok_trap", 4)
    h.shaped("knabbelkoorblok_plaat", ["BBB"], {"B": "guhs:knabbelkoorblok"}, "guhs:knabbelkoorblok_plaat", 6)
    for out, c in (("knabbelkoorpilaar", 1), ("knabbelkoorblok_trap", 1), ("knabbelkoorblok_plaat", 2)):
        h.w(f"{D}/recipe/{out}_van_knabbelkoorblok_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": "guhs:knabbelkoorblok"},
                                                                     "result": {"id": f"guhs:{out}", "count": c}})
    h.shaped("lichtstaafje", ["P", "K"], {"P": "guhs:gepofte_knabbelkoorvrucht", "K": "guhs:guh_kristal"}, "guhs:lichtstaafje", 4)


# =====================================================================================================================
# the dimension: the biome source, the two noises, the surface rule (the terrain itself is not touched)
# =====================================================================================================================
def if_(c, r):
    return {"type": "minecraft:condition", "if_true": c, "then_run": r}


def seq(*r):
    return {"type": "minecraft:sequence", "sequence": list(r)}


def blk(name):
    return {"type": "minecraft:block", "result_state": {"Name": name}}


def biome_is(b):
    return {"type": "minecraft:biome", "biome_is": [f"guhs:{b}"]}


def top(lagen):
    """The top `lagen` blocks under open air."""
    return {"type": "minecraft:stone_depth", "offset": lagen - 1, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}


def surface_rule():
    """A sequence of biome rules that ends in the rule the Guheinde always had (everything is kaaskorst):
    the Korstwoestijn: three blocks of cracked crust with patches of crumb sand on top (never on an underside: sand falls);
    the Schimmelkaasvlakte and the Kristalkaasklippen: their own cheese all the way down; the Knabbelkoorbos: one block of
    koorkorst; the Gatenkaasheuvels and the Kruimelwolken keep the plain crust."""
    onderkant = {"type": "minecraft:stone_depth", "offset": 1, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "ceiling"}
    zand = {"type": "minecraft:noise_threshold", "noise": "guhs:einde2_biomes_zand", "min_threshold": 0.12, "max_threshold": 1.7976931348623157e308}
    return seq(
        if_(biome_is("korstwoestijn"), seq(
            if_(top(1), if_({"type": "minecraft:not", "invert": onderkant}, if_(zand, blk("guhs:kruimelzand")))),
            if_(top(3), blk("guhs:gebarsten_korst")))),
        if_(biome_is("schimmelkaasvlakte"), blk("guhs:schimmelkaas")),
        if_(biome_is("kristalkaasklippen"), blk("guhs:klipkaas")),
        if_(biome_is("knabbelkoorbos"), if_(top(1), blk("guhs:koorkorst"))),
        blk("guhs:kaaskorst"))


def shifted(noise):
    return {"type": "minecraft:shifted_noise", "noise": noise, "xz_scale": XZ_SCHAAL, "y_scale": 0.0,
            "shift_x": "minecraft:shift_x", "shift_y": 0.0, "shift_z": "minecraft:shift_z"}


def dimension(h):
    D = h.D
    h.w(f"{D}/worldgen/noise/guheinde_streek.json", STREEK)
    h.w(f"{D}/worldgen/noise/guheinde_zeldzaam.json", ZELDZAAM)
    h.w(f"{D}/worldgen/noise/einde2_biomes_zand.json", {"firstOctave": -5, "amplitudes": [1.0, 1.0, 0.5]})
    path = f"{D}/worldgen/noise_settings/guheinde.json"
    oud = json.load(open(path, encoding="utf-8"))
    if oud["noise_router"]["temperature"] != 0.0 or oud["surface_rule"]["type"] != "minecraft:block":
        raise SystemExit("einde2_biomes: worldgen/noise_settings/guheinde.json is not the one features/guheinde.py writes (run the whole chain)")
    # the Guheinde's noise settings as they were: the game test compares the terrain of the two
    h.w(f"{D}/worldgen/noise_settings/einde2_biomes_oud.json", oud)

    def patch_settings(d):
        d["noise_router"]["temperature"] = shifted("guhs:guheinde_streek")
        d["noise_router"]["vegetation"] = shifted("guhs:guheinde_zeldzaam")
        d["surface_rule"] = surface_rule()
    h.patch_json(path, patch_settings)

    def patch_dimension(d):
        d["generator"]["biome_source"] = {
            "type": "guhs:guheinde_biomes", "midden": "guhs:guheinde", "straal_midden": kit.STRAAL_MIDDEN,
            "gewoon": [f"guhs:{b}" for b in kit.GEWOON], "zeldzaam": [f"guhs:{b}" for b in kit.ZELDZAAM], "drempel": DREMPEL}
    h.patch_json(f"{D}/dimension/guheinde.json", patch_dimension)


# the placed features of each generation step, in one global order (every biome uses them in this order). Step 2 (local
# modifications) digs and builds and keeps away from structures itself; step 9 gets the guhs:buiten_gebouwen filter.
STEP_ORDER = {
    2: ["einde2_biomes_kaasgat", "einde2_biomes_sausmeertje", "einde2_biomes_vijver", "einde2_biomes_oase", "einde2_biomes_kei",
        "einde2_biomes_kei_schaars", "einde2_biomes_kristalspits", "einde2_biomes_kruimeleilandje", "einde2_biomes_wolk"],
    6: ["einde2_biomes_kristalkaas_erts"],
    9: ["einde2_biomes_duin", "einde2_biomes_schimmelreus", "einde2_biomes_knabbelkoor", "einde2_biomes_korstgras",
        "einde2_biomes_korstgras_schaars", "einde2_biomes_korststruik", "einde2_biomes_schimmelpaddenstoelen", "einde2_biomes_kristallen"],
}
BIOME_FEATURES = {
    "gatenkaasheuvels": {"einde2_biomes_kaasgat", "einde2_biomes_sausmeertje", "einde2_biomes_kei", "einde2_biomes_korstgras"},
    "korstwoestijn": {"einde2_biomes_oase", "einde2_biomes_kei_schaars", "einde2_biomes_duin", "einde2_biomes_korststruik"},
    "kruimelwolken": {"einde2_biomes_kei_schaars", "einde2_biomes_kruimeleilandje", "einde2_biomes_wolk", "einde2_biomes_korstgras_schaars"},
    "schimmelkaasvlakte": {"einde2_biomes_vijver", "einde2_biomes_schimmelreus", "einde2_biomes_schimmelpaddenstoelen"},
    "kristalkaasklippen": {"einde2_biomes_kristalspits", "einde2_biomes_kristalkaas_erts", "einde2_biomes_kristallen"},
    "knabbelkoorbos": {"einde2_biomes_knabbelkoor", "einde2_biomes_korstgras_schaars"},
}
GROND = "#guhs:einde2/eilandgrond"


def rarity(n):
    return {"type": "minecraft:rarity_filter", "chance": n}


def op_grond(*eerst):
    """On the surface of an island (the heightmap gives nothing in the void, so nothing is tried there)."""
    return list(eerst) + [SQUARE, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, BIOME]


def in_lucht(kans, lo, hi):
    return [rarity(kans), SQUARE, {"type": "minecraft:height_range", "height": uniform({"absolute": lo}, {"absolute": hi})}, BIOME]


def features(h):
    none = {}
    # --- Gatenkaasheuvels: full of round holes, a sauce pond now and then, boulders, crust grass ---
    cf(h, "einde2_biomes_kaasgat", "guhs:einde2_kaasgat", none)
    pf(h, "einde2_biomes_kaasgat", "einde2_biomes_kaasgat", op_grond(count(1)))
    cf(h, "einde2_biomes_sausmeertje", "guhs:einde2_sausmeertje", none)
    pf(h, "einde2_biomes_sausmeertje", "einde2_biomes_sausmeertje", op_grond(rarity(9)))
    cf(h, "einde2_biomes_kei", "guhs:einde2_kruimelrots", none)
    pf(h, "einde2_biomes_kei", "einde2_biomes_kei", op_grond(rarity(5)))
    pf(h, "einde2_biomes_kei_schaars", "einde2_biomes_kei", op_grond(rarity(10)))
    cf(h, "einde2_biomes_korstgras", "minecraft:random_patch", patch("guhs:korstgras", None, 14, 6, 2, on=GROND))
    pf(h, "einde2_biomes_korstgras", "einde2_biomes_korstgras", op_grond(count(1)))
    pf(h, "einde2_biomes_korstgras_schaars", "einde2_biomes_korstgras", op_grond(rarity(3)))
    # --- Korstwoestijn: flat and dry: dunes, a dead bush, one oasis in a long way ---
    cf(h, "einde2_biomes_oase", "guhs:einde2_oase", none)
    pf(h, "einde2_biomes_oase", "einde2_biomes_oase", op_grond(rarity(48)))
    cf(h, "einde2_biomes_duin", "guhs:einde2_duin", none)
    pf(h, "einde2_biomes_duin", "einde2_biomes_duin", op_grond(rarity(2)))
    cf(h, "einde2_biomes_korststruik", "minecraft:random_patch", patch("guhs:dorre_korststruik", None, 5, 7, 2, on=GROND))
    pf(h, "einde2_biomes_korststruik", "einde2_biomes_korststruik", op_grond(rarity(2)))
    # --- Kruimelwolken: crumbs and clouds floating between and above the islands (sparse: the void stays the void) ---
    cf(h, "einde2_biomes_kruimeleilandje", "guhs:einde2_kruimeleilandje", none)
    pf(h, "einde2_biomes_kruimeleilandje", "einde2_biomes_kruimeleilandje", in_lucht(4, 34, 104))
    cf(h, "einde2_biomes_wolk", "guhs:einde2_wolk", none)
    pf(h, "einde2_biomes_wolk", "einde2_biomes_wolk", in_lucht(4, 44, 110))
    # --- Schimmelkaasvlakte: ponds, giant glowing mushrooms and little ones ---
    cf(h, "einde2_biomes_vijver", "guhs:einde2_vijver", none)
    pf(h, "einde2_biomes_vijver", "einde2_biomes_vijver", op_grond(rarity(4)))
    cf(h, "einde2_biomes_schimmelreus", "guhs:einde2_schimmelreus", none)
    pf(h, "einde2_biomes_schimmelreus", "einde2_biomes_schimmelreus", op_grond(rarity(2)))
    cf(h, "einde2_biomes_schimmelpaddenstoelen", "minecraft:random_patch", patch("guhs:schimmelpaddenstoel", None, 10, 6, 2, on=GROND))
    pf(h, "einde2_biomes_schimmelpaddenstoelen", "einde2_biomes_schimmelpaddenstoelen", op_grond(count(1)))
    # --- Kristalkaasklippen: cliff columns, the ore inside the klipkaas, loose clusters on the ground ---
    cf(h, "einde2_biomes_kristalspits", "guhs:einde2_kristalspits", none)
    pf(h, "einde2_biomes_kristalspits", "einde2_biomes_kristalspits", op_grond(rarity(2)))
    cf(h, "einde2_biomes_kristalkaas_erts", "minecraft:ore", {"size": 5, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": "guhs:klipkaas"}, "state": {"Name": "guhs:kristalkaas_erts"}}]})
    pf(h, "einde2_biomes_kristalkaas_erts", "einde2_biomes_kristalkaas_erts",
       [count(5), SQUARE, {"type": "minecraft:height_range", "height": uniform({"absolute": 8}, {"absolute": 72})}, BIOME])
    cf(h, "einde2_biomes_kristallen", "minecraft:random_patch",
       patch("guhs:kristalkaas_kristal", {"facing": "up", "waterlogged": "false"}, 5, 5, 2, on="guhs:klipkaas"))
    pf(h, "einde2_biomes_kristallen", "einde2_biomes_kristallen", op_grond(rarity(2)))
    # --- Knabbelkoorbos: the singing poles (and the same plant for bone meal on a young flower) ---
    cf(h, "einde2_biomes_knabbelkoor", "guhs:einde2_knabbelkoor", none)
    pf(h, "einde2_biomes_knabbelkoor", "einde2_biomes_knabbelkoor", op_grond(count(uniform(1, 3))))
    cf(h, "einde2_biomes_knabbelkoor_gekweekt", "guhs:einde2_knabbelkoor_gekweekt", none)


def biomes(h):
    assert sorted(BIOMES) == sorted(kit.BUITEN) and set(BIOME_FEATURES) == set(BIOMES)
    alle = [f for order in STEP_ORDER.values() for f in order]
    assert len(alle) == len(set(alle)) and set().union(*BIOME_FEATURES.values()) == set(alle), "every placed feature is used, once in the order"
    for b, (naam, fog, sky, water, water_fog, particle) in BIOMES.items():
        steps = [[] for _ in range(11)]
        for step, order in STEP_ORDER.items():
            steps[step] = [f"guhs:{f}" for f in order if f in BIOME_FEATURES[b]]
        h.w(f"{h.D}/worldgen/biome/{b}.json", {
            "has_precipitation": False, "temperature": 0.5, "downfall": 0.5,
            "effects": {"sky_color": sky, "fog_color": fog, "water_color": water, "water_fog_color": water_fog,
                        "grass_color": 0xE8B83C, "foliage_color": 0xE8B83C, "particle": particle,
                        "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}},
            # (no creature spawners: the dimension has no mob generation; the wild ones come by top-up)
            "spawners": {"monster": [{"type": "guhs:mika", "weight": MIKA_GEWICHT, "minCount": 1, "maxCount": 1}]},
            "spawn_costs": {}, "carvers": {}, "features": steps})
        kit.lang(h, f"biome.guhs.{b}", naam)


# =====================================================================================================================
# sounds, advancements, texts, the room of the game tests
# =====================================================================================================================
def sounds(h):
    def patch_sounds(d):
        for event, files in SOUNDS.items():
            d[event] = {"sounds": [f"guhs:einde2_biomes/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch_sounds)


def advancements(h):
    kit.zichtbaar(h, "einde2_biomes_zes_streken", "guheinde_poort", "guhs:gebarsten_korst", "goal", "Zes smaken kaas",
                  "Bezoek alle zes de streken van de buiteneilanden", criteria={
                      b: {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": f"guhs:{b}"}}}} for b in BIOMES})
    kit.zichtbaar(h, "einde2_biomes_kristalkaas", "einde2_biomes_zes_streken", "guhs:kristalkaas", "task", "Kaas met een randje goud",
                  "Hak kristalkaas uit de Kristalkaasklippen",
                  criteria={"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:kristalkaas"}]}}})
    kit.zichtbaar(h, "einde2_biomes_flits", "einde2_biomes_zes_streken", "guhs:knabbelkoorvrucht", "task", "Floep!",
                  "Eet een knabbelkoorvrucht en sta ineens ergens anders")


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def testkamer(h):
    """The room of E2BiomesGameTests: a thick floor of kaaskorst (six blocks: enough for a hole and a pond) under open air,
    wide enough that no feature reaches out of it (E2BiomesWorldgen.BEREIK)."""
    s = h.Structure((33, 40, 33))
    s.fill(0, 0, 0, 32, 5, 32, "guhs:kaaskorst")
    s.save("einde2_biomes_test_kamer")


def selfcheck(h):
    """Every block and item has its model, texture and name; every sound its file; every feature of a biome exists."""
    A, D = h.A, h.D
    missing = []
    for b in ALL_BLOCKS:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if not os.path.exists(f"{D}/loot_table/blocks/{b}.json"):
            missing.append(f"loot {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS:
        if not os.path.exists(f"{A}/models/item/{i}.json") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for kind, names in (("block", tex.BLOCKS), ("item", tex.ITEMS)):
        for n in names:
            if not os.path.exists(os.path.join(h.TEX, kind, f"{n}.png")):
                missing.append(f"texture {kind}/{n}")
    for path, _dirs, files in os.walk(f"{A}/models/block"):
        for f in files:
            if f.startswith(tuple(ALL_BLOCKS)):
                model = json.load(open(os.path.join(path, f), encoding="utf-8"))
                for t in model.get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    for files in SOUNDS.values():
        for f in files:
            if not os.path.exists(f"{A}/sounds/einde2_biomes/{f}.ogg"):
                missing.append(f"sound {f} (run tools/features/einde2_biomes_geluid.py)")
    for event in SOUNDS:
        if f"subtitles.guhs.{event}" not in h.NL:
            missing.append(f"subtitle {event}")
    for order in STEP_ORDER.values():
        for f in order:
            if not os.path.exists(f"{D}/worldgen/placed_feature/{f}.json"):
                missing.append(f"placed feature {f}")
    for b in BIOMES:
        if f"guhs:{b}" not in json.load(open(f"{h.R}/data/guhs/tags/worldgen/biome/has_structure/mika_vesting.json", encoding="utf-8"))["values"]:
            missing.append(f"{b} in has_structure/mika_vesting")
    if missing:
        raise SystemExit(f"einde2_biomes: missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    loot(h)
    tags(h)
    recipes(h)
    dimension(h)
    features(h)
    biomes(h)
    sounds(h)
    advancements(h)
    texts(h)
    testkamer(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: a section of its own at the end of the Guheinde chapter (see / find / make; nothing is locked)
# =====================================================================================================================
STREEK_TEKST = {
    "gatenkaasheuvels": "Glooiende gele heuvels vol ronde gaten, met hier en daar een meertje kaassaus. Wie heeft hier zo zitten knabbelen?",
    "korstwoestijn": "Vlak, droog en gebarsten, met duinen van kruimelzand waar je in wegzakt. Ergens ligt een oase, njeg.",
    "kruimelwolken": "Kruimels die zweven en wolken waar je langzaam doorheen zakt. Kijk goed waar je landt!",
    "schimmelkaasvlakte": "Blauwaderkaas zo ver je kijkt, mist, vijvers en reuzenpaddenstoelen die licht geven. Het ruikt hier... apart.",
    "kristalkaasklippen": "Hoge pilaren van oude klipkaas met gouden kristallen erop. Hier groeit de &6kristalkaas&r.",
    "knabbelkoorbos": "Hoge paarse knabbelkoorplanten die zachtjes zingen. Stil zijn en luisteren: elke bloem heeft zijn eigen toon.",
}


def ftb(fq):
    q, item, biome = fq.q, fq.item, fq.biome
    for b, (naam, *_rest) in BIOMES.items():
        zeldzaam = b in kit.ZELDZAAM
        q(f"einde2_biomes_{b}", naam, f"Bezoek {'het' if b == 'knabbelkoorbos' else 'de'} &6{naam}&r op de buiteneilanden van het Guheinde. {STREEK_TEKST[b]}"
          + (" Deze streek is klein en zeldzaam: zoek maar eens goed." if zeldzaam else ""), "minecraft:filled_map", [biome(b)],
          rewards=(("guhs:kaas_knabbels", 10 if zeldzaam else 6),), shape="circle")
    q("einde2_biomes_kristalkaas", "Kaas met een randje goud", "In de Kristalkaasklippen zit &6kristalkaaserts&r in de klipkaas, en op de pilaren "
      "groeien gouden kristallen. Hak er wat &6kristalkaas&r uit. Negen brokjes zijn ook een hele. (Niet opeten: daar is het veel te hard voor.)",
      "guhs:kristalkaas", [item("guhs:kristalkaas", 3)], rewards=(("guhs:kaas_knabbels", 12),), shape="diamond")
    q("einde2_biomes_vrucht", "Floep!", "Hak een knabbelkoorplant om en eet een &dknabbelkoorvrucht&r. Floep: je staat ineens een stukje "
      "verder. Handig bij een afgrond. Of juist niet, njeg.", "guhs:knabbelkoorvrucht", [item("guhs:knabbelkoorvrucht")],
      rewards=(("guhs:kaas_knabbels", 8),))
    q("einde2_biomes_gepoft", "Gepoft is ook lekker", "Leg een knabbelkoorvrucht in de oven: hij poft open. Van vier &dgepofte "
      "knabbelkoorvruchten&r maak je vier knabbelkoorblokken, en daarvan weer trappen, platen en pilaren.", "guhs:gepofte_knabbelkoorvrucht",
      [item("guhs:gepofte_knabbelkoorvrucht", 4), item("guhs:knabbelkoorblok", 4)], rewards=(("guhs:kaas_knabbels", 8),))
    q("einde2_biomes_lichtstaafje", "Een lichtje voor je guh", "Een gepofte knabbelkoorvrucht op een guhkristal: vier &dlichtstaafjes&r. "
      "Zo is het in het Guheinde nooit meer helemaal donker.", "guhs:lichtstaafje", [item("guhs:lichtstaafje", 4)],
      rewards=(("guhs:kaas_knabbels", 8),), shape="rsquare")
