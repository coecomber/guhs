"""
1.7.0 "Onder de Wol", the new cave kinds (Java: feature/onderwol/OnderWolGrot, Kristalpegel, OnderWolGrotBlokken,
OnderWolGrotWorldgen). This module makes their resources (textures exist already in assets/guhs/textures/block):

  - the blocks: kristalpegelblok + kristalpegel (a pink guhkristal dripstone), knuffelwortelaarde + knuffelwortels (rooted
    dirt and hanging roots), belletjeskristal + belletjestrosje (musical amethyst), rozenkwarts_druppel (a small bud):
    blockstates, models (cutout where needed), item models, 26.1 item definitions, loot, mineable tags, nl/en texts;
  - the features (configured + placed; the biomes that get them are onderwol.py's business):
      a. kristalpegel caves      guhs:onderwol_kristalpegel_cluster, guhs:onderwol_kristalpegel      (step 7)
      b. knuffelwortel caves     guhs:onderwol_knuffelwortel_plafond, guhs:onderwol_knuffelwortel_vloer   (step 9)
      c. belletjes caves         guhs:onderwol_belletjes                                             (step 7)
      d. rozenkwarts druppels    guhs:onderwol_druppels   (y 22-48, every Guhmension biome)          (step 9)

It writes only its own files (all writes idempotent) and merges its texts into nl_nl.json / en_us.json.

    PYTHONPATH=tools python tools/features/onderwol_grot.py        (from the project root)
"""
import json
import os

BLOKKEN = ["kristalpegelblok", "kristalpegel", "knuffelwortelaarde", "knuffelwortels", "belletjeskristal", "belletjestrosje",
           "rozenkwarts_druppel"]
DIKTES = ["tip", "tip_merge", "frustum", "middle", "base"]
TEXTURES = ["kristalpegelblok", "knuffelwortelaarde", "knuffelwortels", "belletjeskristal", "belletjestrosje", "rozenkwarts_druppel"] + \
    [f"kristalpegel_{r}_{d}" for r in ("up", "down") for d in DIKTES]

# the feature ids per cave kind, and the biome feature step (index in a biome's "features") each belongs in
KRISTALPEGEL_GROT = ["guhs:onderwol_kristalpegel_cluster", "guhs:onderwol_kristalpegel"]        # step 7: underground_decoration
KNUFFELWORTEL_GROT = ["guhs:onderwol_knuffelwortel_plafond", "guhs:onderwol_knuffelwortel_vloer"]  # step 9: vegetal_decoration
BELLETJES_GROT = ["guhs:onderwol_belletjes"]                                                      # step 7: underground_decoration
DRUPPELS = ["guhs:onderwol_druppels"]                                                             # step 9: vegetal_decoration

CUTOUT = "minecraft:cutout"
# (shears = anything that can shears_dig, like the mod's other plants: the vadsschaar cuts these off too)
SHEARS = [{"condition": "minecraft:any_of", "terms": [
    {"condition": "neoforge:can_item_perform_ability", "ability": "shears_dig"},
    {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}]
# (vanilla's amethyst cluster rotations per facing)
FACING_ROT = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "east": {"x": 90, "y": 90}, "south": {"x": 90, "y": 180},
              "west": {"x": 90, "y": 270}}


def g(*names):
    return [f"guhs:{n}" for n in names]


# =====================================================================================================================
# blocks, items, loot, tags, texts
# =====================================================================================================================
def blocks_and_items(h):
    A, w = h.A, h.w
    for b in ("kristalpegelblok", "knuffelwortelaarde", "belletjeskristal"):
        h.simple_block(b)
    # the kristalpegel: vanilla's pointed dripstone model (crossed planes) per direction and thickness
    staten = {}
    for r in ("up", "down"):
        for d in DIKTES:
            w(f"{A}/models/block/kristalpegel_{r}_{d}.json", {"parent": "minecraft:block/pointed_dripstone", "render_type": CUTOUT,
                                                             "textures": {"cross": f"guhs:block/kristalpegel_{r}_{d}"}})
            staten[f"thickness={d},vertical_direction={r}"] = {"model": f"guhs:block/kristalpegel_{r}_{d}"}
    w(f"{A}/blockstates/kristalpegel.json", {"variants": staten})
    w(f"{A}/models/item/kristalpegel.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:block/kristalpegel_up_tip"},
                                             "display": {"thirdperson_righthand": {"rotation": [0, 100, 0], "translation": [-1, -1, 0], "scale": [0.9, 0.9, 0.9]},
                                                         "firstperson_righthand": {"rotation": [0, 100, 0], "translation": [0, -2, 0], "scale": [0.9, 0.9, 0.9]}}})
    # the knuffelwortels: a cross, like hanging roots
    w(f"{A}/models/block/knuffelwortels.json", {"parent": "minecraft:block/cross", "render_type": CUTOUT, "textures": {"cross": "guhs:block/knuffelwortels"}})
    w(f"{A}/blockstates/knuffelwortels.json", {"variants": {"": {"model": "guhs:block/knuffelwortels"}}})
    h.item_model("knuffelwortels", "guhs:block/knuffelwortels")
    # the belletjestrosje and the rozenkwarts druppel: a cross turned to the face they sit on, like amethyst
    for b in ("belletjestrosje", "rozenkwarts_druppel"):
        w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cross", "render_type": CUTOUT, "textures": {"cross": f"guhs:block/{b}"}})
        w(f"{A}/blockstates/{b}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{b}", **rot} for f, rot in FACING_ROT.items()}})
        w(f"{A}/models/item/{b}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:block/{b}"},
                                        "display": {"head": {"translation": [0, 14, -5]}}})


def loot(h):
    D, w = h.D, h.w
    for b in ("kristalpegelblok", "kristalpegel", "knuffelwortelaarde", "belletjeskristal", "belletjestrosje", "rozenkwarts_druppel"):
        h.self_drop(b)
    # the knuffelwortels only with shears (or silk touch), like hanging roots
    w(f"{D}/loot_table/blocks/knuffelwortels.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "conditions": SHEARS,
                                                                                          "entries": [{"type": "minecraft:item", "name": "guhs:knuffelwortels"}]}],
                                                     "random_sequence": "guhs:blocks/knuffelwortels"})


def tags(h):
    h.add_tag("minecraft/tags/block/mineable/pickaxe", g("kristalpegelblok", "kristalpegel", "belletjeskristal", "belletjestrosje", "rozenkwarts_druppel"))
    h.add_tag("minecraft/tags/block/mineable/shovel", g("knuffelwortelaarde"))
    # knuffelwortelaarde counts as dirt (as rooted dirt does): plants and saplings grow on it
    h.add_tag("minecraft/tags/block/dirt", g("knuffelwortelaarde"))
    h.add_tag("minecraft/tags/item/dirt", g("knuffelwortelaarde"))


NAMEN = {  # id: (Dutch, English)
    "kristalpegelblok": ("Kristalpegelblok", "Crystal Icicle Block"),
    "kristalpegel": ("Kristalpegel", "Crystal Icicle"),
    "knuffelwortelaarde": ("Knuffelwortelaarde", "Plush Root Soil"),
    "knuffelwortels": ("Knuffelwortels", "Plush Roots"),
    "belletjeskristal": ("Belletjeskristal", "Bell Crystal"),
    "belletjestrosje": ("Belletjestrosje", "Little Bell Cluster"),
    "rozenkwarts_druppel": ("Rozenkwarts druppel", "Rose Quartz Drop"),
}
LORE = {  # id: (English, Dutch)
    "kristalpegel": ("Hangs and stands like dripstone. The tips drip a little cheese sauce. Watch your head!",
                     "Hangt en staat net als druipsteen. De puntjes druppen een beetje kaassaus. Pas op je hoofd!"),
    "knuffelwortelaarde": ("Bone meal grows plush roots underneath.", "Met beendermeel groeien er knuffelwortels onder."),
    "belletjeskristal": ("Rings a note when you step on it or hit it. Every spot has its own note: play a tune!",
                         "Klinkt een toontje als je erop stapt of ertegen slaat. Elke plek heeft zijn eigen toon: speel een liedje!"),
    "belletjestrosje": ("Click it and it chimes. Every spot has its own note.", "Klik erop en het tingelt. Elke plek heeft zijn eigen toon."),
}


def texts(h):
    for b, (nl, en) in NAMEN.items():
        h.lang(f"block.guhs.{b}", en, nl)
    for b, (en, nl) in LORE.items():
        h.lang(f"block.guhs.{b}.lore", en, nl)


# =====================================================================================================================
# the features
# =====================================================================================================================
def _hoogte(lo, hi):
    return {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}


def _uniform(lo, hi):
    return {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}


def _scan(richting, toestaan=None):
    """Searches from the placed spot through cave air to the floor (down) or ceiling (up); lands on the air next to it."""
    return [{"type": "minecraft:environment_scan", "allowed_search_condition": toestaan or {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
             "direction_of_search": richting, "max_steps": 12, "target_condition": {"type": "minecraft:solid"}},
            {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1 if richting == "down" else -1}]


def _st(naam):
    return {"type": "minecraft:simple_state_provider", "state": {"Name": naam}}


def features(h):
    w = h.w
    wg = f"{h.D}/worldgen"

    def zet(naam, config, plaatsing):
        w(f"{wg}/configured_feature/{naam}.json", config)
        w(f"{wg}/placed_feature/{naam}.json", {"feature": f"guhs:{naam}", "placement": plaatsing + [{"type": "minecraft:biome"}]})

    # --- a. the kristalpegel caves: vanilla's dripstone_cluster and pointed_dripstone, scaled to the Guhmension's caves ---
    zet("onderwol_kristalpegel_cluster", {"type": "guhs:onderwol_kristalpegel_cluster", "config": {
        "floor_to_ceiling_search_range": 12, "height": _uniform(2, 5), "radius": _uniform(2, 7), "max_stalagmite_stalactite_height_diff": 1,
        "height_deviation": 3, "dripstone_block_layer_thickness": _uniform(2, 4),
        "density": {"type": "minecraft:uniform", "min_inclusive": 0.3, "max_exclusive": 0.7}, "wetness": 0.0,
        "chance_of_dripstone_column_at_max_distance_from_center": 0.1, "max_distance_from_edge_affecting_chance_of_dripstone_column": 3,
        "max_distance_from_center_affecting_height_bias": 8}},
        [{"type": "minecraft:count", "count": _uniform(40, 64)}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(6, 62)}])
    lucht_of_water = {"type": "minecraft:any_of", "predicates": [{"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                                                                 {"type": "minecraft:matching_blocks", "blocks": "minecraft:water"}]}
    punt = {"type": "guhs:onderwol_kristalpegel", "config": {"chance_of_directional_spread": 0.7, "chance_of_spread_radius2": 0.5,
                                                             "chance_of_spread_radius3": 0.5, "chance_of_taller_dripstone": 0.2}}
    zet("onderwol_kristalpegel", {"type": "minecraft:simple_random_selector", "config": {"features": [
        {"feature": punt, "placement": _scan("down", lucht_of_water)}, {"feature": punt, "placement": _scan("up", lucht_of_water)}]}},
        [{"type": "minecraft:count", "count": _uniform(120, 160)}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(6, 62)},
         {"type": "minecraft:count", "count": _uniform(1, 5)},
         {"type": "minecraft:random_offset",
          "xz_spread": {"type": "minecraft:clamped_normal", "mean": 0.0, "deviation": 3.0, "min_inclusive": -10, "max_inclusive": 10},
          "y_spread": {"type": "minecraft:clamped_normal", "mean": 0.0, "deviation": 0.6, "min_inclusive": -2, "max_inclusive": 2}}])

    # --- b. the knuffelwortel caves: knuffelwortelaarde in the ceiling with knuffelwortels (now and then a glitterrank), a
    # soft floor of knuffelgras with pluisgras and guhpaddenstoeltjes ---
    w(f"{wg}/configured_feature/onderwol_knuffelwortels.json", {"type": "minecraft:simple_block", "config": {"to_place": _st("guhs:knuffelwortels")}})
    w(f"{wg}/configured_feature/onderwol_knuffelwortel_hangers.json", {"type": "minecraft:random_selector", "config": {
        "features": [{"chance": 0.12, "feature": {"feature": "guhs:onderwol_glitterrank", "placement": []}}],
        "default": {"feature": "guhs:onderwol_knuffelwortels", "placement": []}}})
    w(f"{wg}/configured_feature/onderwol_knuffelwortel_vloerplantjes.json", {"type": "minecraft:simple_block", "config": {"to_place": {
        "type": "minecraft:weighted_state_provider", "entries": [{"data": {"Name": "guhs:pluisgras"}, "weight": 8},
                                                                 {"data": {"Name": "guhs:guhpaddenstoel"}, "weight": 1}]}}})

    def patch(naam, grond, oppervlak, kans, plant, diepte):
        return {"type": "minecraft:vegetation_patch", "config": {
            "depth": diepte, "extra_bottom_block_chance": 0.0, "extra_edge_column_chance": 0.3, "ground_state": _st(grond),
            "replaceable": "#guhs:onderwol/laagsteen", "surface": oppervlak, "vegetation_chance": kans,
            "vegetation_feature": {"feature": plant, "placement": []}, "vertical_range": 5,
            "xz_radius": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 6}}}
    plek = lambda richting: [{"type": "minecraft:count", "count": 60}, {"type": "minecraft:in_square"},  # noqa: E731
                             {"type": "minecraft:height_range", "height": _hoogte(6, 64)}, *_scan(richting)]
    zet("onderwol_knuffelwortel_plafond", patch("onderwol_knuffelwortel_plafond", "guhs:knuffelwortelaarde", "ceiling", 0.55,
                                                "guhs:onderwol_knuffelwortel_hangers", _uniform(1, 2)), plek("up"))
    zet("onderwol_knuffelwortel_vloer", patch("onderwol_knuffelwortel_vloer", "guhs:knuffelgras", "floor", 0.4,
                                              "guhs:onderwol_knuffelwortel_vloerplantjes", 1), plek("down"))

    # --- c. the belletjes caves: belletjeskristal on the walls, floor and ceiling around a spot of cave air, trosjes on it ---
    zet("onderwol_belletjes", {"type": "guhs:onderwol_belletjes", "config": {}},
        [{"type": "minecraft:count", "count": 24}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(6, 60)}])

    # --- d. the rozenkwarts druppels: in every Guhmension cave in the knuffelsteen, on ceilings and now and then a wall ---
    zet("onderwol_druppels", {"type": "guhs:onderwol_druppels", "config": {}},
        [{"type": "minecraft:count", "count": 10}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(22, 48)},
         *_scan("up")])


def selfcheck(h):
    problemen = [f"texture missing: {t}" for t in TEXTURES if not os.path.exists(f"{h.A}/textures/block/{t}.png")]
    problemen += [f"loot table missing: {b}" for b in BLOKKEN if not os.path.exists(f"{h.D}/loot_table/blocks/{b}.json")]
    if not os.path.exists(f"{h.D}/worldgen/configured_feature/onderwol_glitterrank.json"):
        problemen.append("guhs:onderwol_glitterrank is gone (the knuffelwortel ceilings use it): run onderwol.py")
    if problemen:
        raise SystemExit("onderwol_grot self-check failed:\n  " + "\n  ".join(problemen))


def build(h):
    blocks_and_items(h)
    loot(h)
    tags(h)
    texts(h)
    features(h)
    selfcheck(h)


# =====================================================================================================================
# on its own: PYTHONPATH=tools python tools/features/onderwol_grot.py from the project root (the same helpers as
# onderwol.py's _direct; a full make_resources run is NOT safe on this tree)
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

    def lang(key, en, nl):
        EN[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=R, A=A, D=D, w=w, add_tag=add_tag, simple_block=simple_block, item_model=item_model, self_drop=self_drop,
                              lang=lang)
    build(h)
    # the 26.1 item definitions
    for i in BLOKKEN:
        w(f"{A}/items/{i}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{i}"}})
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    print(f"onderwol_grot: {len(BLOKKEN)} blocks, {len(NL)} texts")


if __name__ == "__main__":
    _direct()
