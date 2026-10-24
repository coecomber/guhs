"""
1.7.0 (design: MineScapeStory/ontwerp-guhs170-mijnen.md, rounds 32-40; Java: feature/guhvillagers): the guh villagers.

  - the new guh villager look (model A for grown-ups, model C for children) and the IJzerguhlem: textures in
    guhvillagers_tex.py, models in client/GuhVillagerModellen.java;
  - the four new professions and their workstations (Toverguh-boekenstandaard, Guhrad-werkbank, Ertsenkist,
    Kristalslijpsteen): block models, loot, tags (job sites, mineable), recipes;
  - the picked-up villager item, the IJzerguhlem's loot (iron ingots + pluisgras instead of poppies);
  - all texts (nl_nl + en_us).

Standalone and idempotent (like onderwol.py): from the project root

    PYTHONPATH=tools python tools/features/guhvillagers.py

writes only its own files, adds to the shared tags, merges its texts into nl_nl.json / en_us.json.
"""
import json
import os

from features import guhvillagers_tex as tex

WERKPLEKKEN = ["toverguh_boekenstandaard", "guhrad_werkbank", "ertsenkist", "kristalslijpsteen"]


def blocks_and_items(h):
    A, w = h.A, h.w
    for b in WERKPLEKKEN:
        w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/orientable", "textures": {
            "top": f"guhs:block/{b}_top", "front": f"guhs:block/{b}_front", "side": f"guhs:block/{b}_side"}})
        w(f"{A}/blockstates/{b}.json", {"variants": {f"facing={f}": ({"model": f"guhs:block/{b}", "y": y} if y else {"model": f"guhs:block/{b}"})
                                                     for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
        w(f"{A}/items/{b}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{b}"}})
        h.self_drop(b)
    h.item_model("opgepakte_villager")
    w(f"{A}/items/opgepakte_villager.json", {"model": {"type": "minecraft:model", "model": "guhs:item/opgepakte_villager"}})


def loot(h):
    # like the vanilla iron golem, with pluisgras instead of poppies
    h.w(f"{h.D}/loot_table/entities/ijzerguhlem.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:pluisgras", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}, "add": False}]}]},
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "minecraft:iron_ingot", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 5}, "add": False}]}]}],
        "random_sequence": "guhs:entities/ijzerguhlem"})


def tags(h):
    h.add_tag("minecraft/tags/point_of_interest_type/acquirable_job_site", [f"guhs:{b}" for b in WERKPLEKKEN])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:toverguh_boekenstandaard", "guhs:guhrad_werkbank", "guhs:ertsenkist"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:kristalslijpsteen"])


def recipes(h):
    h.shaped("toverguh_boekenstandaard", ["WBW", " P ", " P "], {"W": "minecraft:pink_wool", "B": "minecraft:book", "P": "#minecraft:planks"},
             "guhs:toverguh_boekenstandaard")
    h.shaped("guhrad_werkbank", ["G", "C"], {"G": "guhs:guh_wheel", "C": "minecraft:crafting_table"}, "guhs:guhrad_werkbank")
    h.shaped("ertsenkist", ["VVV", "VCV"], {"V": "guhs:viltsteen", "C": "minecraft:chest"}, "guhs:ertsenkist")
    h.shaped("kristalslijpsteen", ["K", "G"], {"K": "guhs:guh_kristal", "G": "minecraft:grindstone"}, "guhs:kristalslijpsteen")


def texts(h):
    L = h.lang
    for b, nl, en, lnl, len_ in (
            ("toverguh_boekenstandaard", "Toverguh-boekenstandaard", "Wizard Guh Book Stand",
             "Werkplek van de Toverguh: hij verkoopt Spreukjesboekjes.", "Workstation of the Wizard Guh: sells little spell books."),
            ("guhrad_werkbank", "Guhrad-werkbank", "Guh Wheel Workbench",
             "Werkplek van de Vadsmonteur: guhraderen, guhdraad, guhovens en meer vadskracht.",
             "Workstation of the Chonk Mechanic: guh wheels, guh wire, guh ovens and more chonk power."),
            ("ertsenkist", "Ertsenkist", "Ore Chest",
             "Werkplek van de Mijnwerker: laagstenen, guhklei en lampjes; hij koopt je ertsen.",
             "Workstation of the Miner: layer stones, guh clay and lamps; buys your ores."),
            ("kristalslijpsteen", "Kristalslijpsteen", "Crystal Grindstone",
             "Werkplek van de Kristalslijper: slijpt guhkristal en vahoegiet tot glinsterblokken.",
             "Workstation of the Crystal Cutter: grinds guh crystal and vahoegite into sparkly blocks.")):
        L(f"block.guhs.{b}", en, nl)
        L(f"block.guhs.{b}.lore", len_, lnl)
    for b, nl, en in (("toverguh", "Toverguh", "Wizard Guh"), ("vadsmonteur", "Vadsmonteur", "Chonk Mechanic"),
                      ("mijnwerker", "Mijnwerker", "Miner"), ("kristalslijper", "Kristalslijper", "Crystal Cutter")):
        L(f"entity.minecraft.villager.guhs.{b}", en, nl)
    L("entity.guhs.ijzerguhlem", "Iron Guhlem", "IJzerguhlem")
    L("item.guhs.opgepakte_villager", "Picked-Up Villager", "Opgepakte villager")
    L("item.guhs.opgepakte_villager.named", "%s (picked up)", "%s (opgepakt)")
    L("item.guhs.opgepakte_villager.kind", "Picked-Up Villager Child", "Opgepakt villagerkindje")
    L("item.guhs.opgepakte_villager.guh", "Guh villager", "Guh-villager")
    L("item.guhs.opgepakte_villager.gewoon", "Villager", "Gewone villager")
    L("item.guhs.opgepakte_villager.ruilen", "%s trades", "%s ruilaanbiedingen")
    L("item.guhs.opgepakte_villager.hint", "Right-click a block to put them down", "Rechtsklik op een blok om hem neer te zetten")
    L("gui.guhs.villager_oppakken.kan_niet", "You can't pick up this villager, njeg.", "Deze villager kun je niet oppakken, njeg.")
    L("gui.guhs.villager_oppakken.handelt", "Just a moment: this villager is busy trading!", "Even geduld: deze villager is aan het handelen!")


def selfcheck(h):
    problemen = []
    for b in WERKPLEKKEN:
        for kant in ("top", "front", "side"):
            if not os.path.exists(f"{h.A}/textures/block/{b}_{kant}.png"):
                problemen.append(f"texture missing: {b}_{kant}")
    for p in ("entity/villager/guh_villager", "entity/villager/guh_villager_kind", "entity/villager/guh_villager_hoedjes", "entity/ijzerguhlem",
              "item/opgepakte_villager", *[f"entity/villager/profession/{n}" for n in ("toverguh", "vadsmonteur", "mijnwerker", "kristalslijper")]):
        if not os.path.exists(f"{h.A}/textures/{p}.png"):
            problemen.append(f"texture missing: {p}")
    if problemen:
        raise SystemExit("guhvillagers self-check failed:\n  " + "\n  ".join(problemen))


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    loot(h)
    tags(h)
    recipes(h)
    texts(h)
    selfcheck(h)


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
    h = types.SimpleNamespace(R=R, A=A, D=D, w=w, add_tag=add_tag, item_model=item_model, self_drop=self_drop, shaped=shaped, lang=lang)
    build(h)
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    print(f"guhvillagers: {len(NL)} texts")


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
