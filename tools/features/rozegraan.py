"""
1.8.0: roze graan, the farm thing of De Zingende Weide. Java: feature/rozegraan (RozegraanFeature, RozeGraanBlock).

  build(h)    the crop guhs:roze_graan_plant (4 stages, tinted from vanilla wheat), the items roze_zaadjes / roze_graan /
              roze_broodje / roze_knabbelkoekje, the block roze_hooibaal, the recipes, the loot of the crop, and the loot of
              the pink weide grasses: with shears the grass itself (zingendeweide.py), by hand roze zaadjes instead of wheat
              seeds (this module runs AFTER zingendeweide.py and only swaps the seed item); the crop/seed/snack tags and the
              texts (Dutch in nl_nl, English in tools/lang/en/c215_rozegraan.json)

Standalone (from the project root): PYTHONPATH=tools python tools/features/rozegraan.py
"""
import json
import os
import sys

from features import zingendeweide as zw

ROZE = (246, 140, 186)
GRASSEN = ["weide_roze_gras", "weide_lichtroze_gras", "weide_hoog_roze_gras", "weide_hoog_lichtroze_gras"]
ITEMS = ["roze_zaadjes", "roze_graan", "roze_broodje", "roze_knabbelkoekje"]

TEKSTEN = {  # key -> (Dutch, English)
    "block.guhs.roze_graan_plant": ("Roze graan", "Pink Grain"),
    "block.guhs.roze_hooibaal": ("Roze hooibaal", "Pink Hay Bale"),
    "item.guhs.roze_zaadjes": ("Roze zaadjes", "Pink Seeds"),
    "item.guhs.roze_zaadjes.lore": ("Uit het roze gras van de Zingende Weide. Plant ze op akkerland: er groeit roze graan uit.",
                                    "From the pink grass of the Singing Meadow. Plant them on farmland: pink grain grows from them."),
    "item.guhs.roze_graan": ("Roze graan", "Pink Grain"),
    "item.guhs.roze_graan.lore": ("Glinstert een beetje. Niemand weet waarom. Njeg, het is gewoon zo.",
                                  "It glitters a little. Nobody knows why. Njeg, it just does."),
    "item.guhs.roze_broodje": ("Roze broodje", "Pink Bun"),
    "item.guhs.roze_broodje.lore": ("Guhs en Blubs zijn er dol op: daar tem je ze net zo goed mee als met een kaasknabbel. Vahoeg lekker!",
                                    "Guhs and Blubs adore it: it tames them just as well as a kaasknabbel. Vahoeg tasty!"),
    "item.guhs.roze_knabbelkoekje": ("Roze knabbelkoekje", "Pink Nibble Cookie"),
    "item.guhs.roze_knabbelkoekje.lore": ("Een koekje met een kaasknabbel erin. Een snackje voor je guh (en stiekem ook voor jou).",
                                          "A cookie with a kaasknabbel in it. A little snack for your guh (and secretly for you too)."),
}


def build(h):
    A, D, w = h.A, h.D, h.w
    # --- the crop: four stages from vanilla wheat 1/3/5/7, tinted pink (the ripe one with glitter dots) -------------------
    for stage, bron in enumerate((1, 3, 5, 7)):
        img = zw.tint(zw.vanilla_png(f"block/wheat_stage{bron}"), ROZE, 0.15)
        if stage == 3:
            img = zw.glans(img, [(3, 4), (8, 2), (12, 5), (6, 8)])
        h.save(img, "block", f"roze_graan_stage{stage}.png")
        w(f"{A}/models/block/roze_graan_stage{stage}.json", {"parent": "minecraft:block/crop",
                                                             "textures": {"crop": f"guhs:block/roze_graan_stage{stage}"}})
    w(f"{A}/blockstates/roze_graan_plant.json",
      {"variants": {f"age={a}": {"model": f"guhs:block/roze_graan_stage{a}"} for a in range(4)}})
    # --- the items -----------------------------------------------------------------------------------------------------
    for naam, bron in (("roze_zaadjes", "item/wheat_seeds"), ("roze_graan", "item/wheat"), ("roze_broodje", "item/bread"),
                       ("roze_knabbelkoekje", "item/cookie")):
        h.save(zw.tint(zw.vanilla_png(bron), ROZE, 0.2), "item", f"{naam}.png")
        h.item_model(naam)
    # --- the hay bale --------------------------------------------------------------------------------------------------
    h.save(zw.tint(zw.vanilla_png("block/hay_block_side"), ROZE, 0.15), "block", "roze_hooibaal_side.png")
    h.save(zw.tint(zw.vanilla_png("block/hay_block_top"), ROZE, 0.15), "block", "roze_hooibaal_top.png")
    w(f"{A}/models/block/roze_hooibaal.json", {"parent": "minecraft:block/cube_column",
                                              "textures": {"end": "guhs:block/roze_hooibaal_top", "side": "guhs:block/roze_hooibaal_side"}})
    w(f"{A}/models/block/roze_hooibaal_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                         "textures": {"end": "guhs:block/roze_hooibaal_top",
                                                                      "side": "guhs:block/roze_hooibaal_side"}})
    w(f"{A}/blockstates/roze_hooibaal.json", {"variants": {
        "axis=y": {"model": "guhs:block/roze_hooibaal"},
        "axis=x": {"model": "guhs:block/roze_hooibaal_horizontal", "x": 90, "y": 90},
        "axis=z": {"model": "guhs:block/roze_hooibaal_horizontal", "x": 90}}})
    w(f"{A}/models/item/roze_hooibaal.json", {"parent": "guhs:block/roze_hooibaal"})
    h.self_drop("roze_hooibaal")
    # --- loot of the crop (like beetroot: ripe = grain + 0-3 extra seeds, else just the seed) -------------------------------
    rijp = {"condition": "minecraft:block_state_property", "block": "guhs:roze_graan_plant", "properties": {"age": "3"}}
    w(f"{D}/loot_table/blocks/roze_graan_plant.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:roze_graan", "conditions": [rijp]},
            {"type": "minecraft:item", "name": "guhs:roze_zaadjes"}]}]},
        {"rolls": 1, "bonus_rolls": 0, "conditions": [rijp], "entries": [{"type": "minecraft:item", "name": "guhs:roze_zaadjes",
            "functions": [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                           "formula": "minecraft:binomial_with_bonus_count", "parameters": {"extra": 3, "probability": 0.5714286}}]}]}],
        "functions": [{"function": "minecraft:explosion_decay"}]})
    # --- the pink grasses: roze zaadjes instead of wheat seeds (the shears part stays as zingendeweide.py wrote it) ----------
    for g in GRASSEN:
        pad = f"{D}/loot_table/blocks/{g}.json"
        if os.path.exists(pad):
            with open(pad, encoding="utf-8") as f:
                tekst = f.read()
            if "minecraft:wheat_seeds" in tekst:
                w(pad, json.loads(tekst.replace("minecraft:wheat_seeds", "guhs:roze_zaadjes")))
    # --- recipes -------------------------------------------------------------------------------------------------------
    h.shaped("roze_broodje", ["GGG"], {"G": "guhs:roze_graan"}, "guhs:roze_broodje")
    h.shapeless("roze_knabbelkoekje", ["guhs:roze_graan", "guhs:roze_graan", "guhs:kaas_knabbels"], "guhs:roze_knabbelkoekje", 4)
    h.shaped("roze_hooibaal", ["GGG", "GGG", "GGG"], {"G": "guhs:roze_graan"}, "guhs:roze_hooibaal")
    h.shapeless("roze_graan_uit_hooibaal", ["guhs:roze_hooibaal"], "guhs:roze_graan", 9)
    # --- tags: a crop for bees, villagers and farmland; the koekje is a guh snack ----------------------------------------
    for tag in ("crops", "bee_growables", "maintains_farmland"):
        _tag(h, f"{h.R}/data/minecraft/tags/block/{tag}.json", "guhs:roze_graan_plant")
    _tag(h, f"{h.R}/data/minecraft/tags/item/villager_plantable_seeds.json", "guhs:roze_zaadjes")
    _tag(h, f"{h.R}/data/minecraft/tags/block/mineable/hoe.json", "guhs:roze_hooibaal")
    _tag(h, f"{D}/tags/item/band/snacks.json", "guhs:roze_knabbelkoekje")
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def _tag(h, pad, waarde):
    d = {"replace": False, "values": []}
    if os.path.exists(pad):
        with open(pad, encoding="utf-8") as f:
            d = json.load(f)
    if waarde not in d["values"]:
        d["values"].append(waarde)
    h.w(pad, d)


def selfcheck(h):
    A, D = h.A, h.D
    nodig = [f"{A}/models/item/{n}.json" for n in ITEMS + ["roze_hooibaal"]] + [f"{A}/blockstates/roze_graan_plant.json"]
    weg = [p for p in nodig if not os.path.exists(p)]
    for g in GRASSEN:
        p = f"{D}/loot_table/blocks/{g}.json"
        if os.path.exists(p) and "wheat_seeds" in open(p, encoding="utf-8").read():
            weg.append(p + " (still wheat seeds)")
    if weg:
        raise SystemExit(f"rozegraan self-check failed: {weg}")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c215_rozegraan.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in ITEMS + ["roze_hooibaal"]:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"rozegraan: crop (4 stages), {len(ITEMS)} items, hooibaal, {len(GRASSEN)} grass loot tables, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
