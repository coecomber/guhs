"""
1.7.0 "Onder de Wol", the mining helpers (design: MineScapeStory/ontwerp-guhs170-mijnen.md rondes 31, 37-40, 44; Java:
feature/mijnhulp). Models, item definitions, loot, tags, recipes, texts (textures: mijnhulp_tex.py) of:

  - the Vahoege Vadsknakker (recipe type guhs:vadsknakker: paxel + shears + hoe, enchantments merged);
  - super vahoege vads: Vahoegietstaaf, Super-vads-sjabloon (+ copying it), the smithing upgrades of every vads piece, the
    equipment asset, and the loot table guhs:mijnhulp/super_vads_sjabloon for the deep cave structures' chests;
  - the Wolkenliftje, the Pluisladder, the Mijnlampje (guh clothes) and its invisible light, the terrariumbodem and the
    block tag guhs:terrarium_ruimte.

Standalone and idempotent (writes its own files, adds its values to shared tags, merges its texts into nl_nl/en_us):

    python tools/features/mijnhulp.py        (from the project root)
"""
import json
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from features import mijnhulp_tex as tex  # noqa: E402

VADS = ["sword", "pickaxe", "axe", "shovel", "hoe", "paxel", "shears", "helmet", "chestplate", "leggings", "boots"]
SUPER = [f"super_vahoege_vads_{n}" for n in VADS] + ["super_vahoege_vadsknakker"]
HANDHELD = ["vahoege_vadsknakker", "super_vahoege_vadsknakker"] + [f"super_vahoege_vads_{n}" for n in ("sword", "pickaxe", "axe", "shovel", "hoe", "paxel")]
PLAT = ["vahoegietstaaf", "super_vads_sjabloon", "mijnlampje", "pluisladder"] + [f"super_vahoege_vads_{n}" for n in ("shears", "helmet", "chestplate", "leggings", "boots")]
BLOKKEN = ["wolkenliftje", "pluisladder", "terrariumbodem"]


def g(*names):
    return [f"guhs:{n}" for n in names]


def models(h):
    A, w = h.A, h.w
    for n in HANDHELD:
        h.item_model(n, parent="minecraft:item/handheld")
    for n in PLAT:
        h.item_model(n)
    # the Wolkenliftje: a cloud block, arrows on top
    w(f"{A}/models/block/wolkenliftje.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/wolkenliftje_boven", "side": "guhs:block/wolkenliftje_zij", "bottom": "guhs:block/wolkenliftje_onder"}})
    w(f"{A}/blockstates/wolkenliftje.json", {"variants": {"": {"model": "guhs:block/wolkenliftje"}}})
    w(f"{A}/models/item/wolkenliftje.json", {"parent": "guhs:block/wolkenliftje"})
    # the Pluisladder: a ladder (the top piece with the rolled-up rest); the item is the roll
    for n in ("pluisladder", "pluisladder_top"):
        w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/ladder", "render_type": "minecraft:cutout",
                                          "textures": {"particle": f"guhs:block/{n}", "texture": f"guhs:block/{n}"}})
    draai = {"north": 0, "east": 90, "south": 180, "west": 270}
    w(f"{A}/blockstates/pluisladder.json", {"variants": {
        f"facing={f},top={str(t).lower()}": {"model": "guhs:block/pluisladder" + ("_top" if t else ""), **({"y": r} if r else {})}
        for f, r in draai.items() for t in (False, True)}})
    # the Mijnlampje's light: nothing to see
    w(f"{A}/models/block/mijnlampje_licht.json", {"textures": {"particle": "minecraft:block/pink_wool"}})
    w(f"{A}/blockstates/mijnlampje_licht.json", {"variants": {"": {"model": "guhs:block/mijnlampje_licht"}}})
    # the terrariumbodem: the terrarium's wooden base and sand floor, terrariumglas around and on top (culled against
    # the other pieces of the space)
    glas = {"sprite": "guhs:block/safari/terrariumglas", "force_translucent": True}
    w(f"{A}/models/block/terrariumbodem.json", {
        "textures": {"particle": glas, "glas": glas, "hout": "minecraft:block/stripped_oak_log", "bodem": "guhs:block/safari_terrarium_bodem"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 2, 16], "faces": {d: {"texture": "#hout", **({"cullface": "down"} if d == "down" else {})}
                                                             for d in ("north", "south", "east", "west", "down")}},
            {"from": [0.5, 2, 0.5], "to": [15.5, 2.5, 15.5], "faces": {"up": {"texture": "#bodem"}}},
            {"from": [0, 2, 0], "to": [16, 16, 16], "faces": {d: {"texture": "#glas", "cullface": d}
                                                              for d in ("north", "south", "east", "west", "up")}},
        ]})
    w(f"{A}/blockstates/terrariumbodem.json", {"variants": {"": {"model": "guhs:block/terrariumbodem"}}})
    w(f"{A}/models/item/terrariumbodem.json", {"parent": "guhs:block/terrariumbodem"})
    # super vads armour: its own equipment asset
    w(f"{A}/equipment/super_vahoege_vads.json", {"layers": {"humanoid": [{"texture": "guhs:super_vahoege_vads"}],
                                                            "humanoid_leggings": [{"texture": "guhs:super_vahoege_vads"}]}})


def loot(h):
    D, w = h.D, h.w
    h.self_drop("wolkenliftje")
    h.self_drop("terrariumbodem")
    # the Pluisladder: only the top piece gives the roll back (the rest rolls up with it)
    w(f"{D}/loot_table/blocks/pluisladder.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
        {"type": "minecraft:item", "name": "guhs:pluisladder"}], "conditions": [
        {"condition": "minecraft:block_state_property", "block": "guhs:pluisladder", "properties": {"top": "true"}},
        {"condition": "minecraft:survives_explosion"}]}], "random_sequence": "guhs:blocks/pluisladder"})
    # for the chests of the deepest cave structures (Guhtempel, bankkluis): one roll, a template one time in four
    w(f"{D}/loot_table/mijnhulp/super_vads_sjabloon.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
        {"type": "minecraft:item", "name": "guhs:super_vads_sjabloon", "weight": 1},
        {"type": "minecraft:empty", "weight": 3}]}], "random_sequence": "guhs:mijnhulp/super_vads_sjabloon"})


def tags(h):
    t = h.add_tag
    t("minecraft/tags/block/climbable", g("pluisladder"))
    t("guhs/tags/block/terrarium_ruimte", g("terrariumglas", "terrariumbodem"))
    t("minecraft/tags/block/impermeable", g("terrariumbodem"))
    knakkers = g("vahoege_vadsknakker", "super_vahoege_vadsknakker")
    t("guhs/tags/item/keep_on_death", g("vahoege_vadsknakker", *SUPER))
    t("guhs/tags/item/vads_uitrusting", g(*[f"super_vahoege_vads_{n}" for n in ("helmet", "chestplate", "leggings", "boots")]))
    t("minecraft/tags/item/swords", g("super_vahoege_vads_sword"))
    t("minecraft/tags/item/pickaxes", g("super_vahoege_vads_pickaxe", "super_vahoege_vads_paxel") + knakkers)
    t("minecraft/tags/item/axes", g("super_vahoege_vads_axe", "super_vahoege_vads_paxel") + knakkers)
    t("minecraft/tags/item/shovels", g("super_vahoege_vads_shovel", "super_vahoege_vads_paxel") + knakkers)
    t("minecraft/tags/item/hoes", g("super_vahoege_vads_hoe") + knakkers)
    for stuk, tag in (("helmet", "head"), ("chestplate", "chest"), ("leggings", "leg"), ("boots", "foot")):
        t(f"minecraft/tags/item/{tag}_armor", g(f"super_vahoege_vads_{stuk}"))
    t("c/tags/item/tools/shear", g("super_vahoege_vads_shears"))
    t("minecraft/tags/item/enchantable/mining", g("super_vahoege_vads_shears"))


def recipes(h):
    D, w = h.D, h.w
    w(f"{D}/recipe/vahoege_vadsknakker.json", {"type": "guhs:vadsknakker", "category": "equipment",
                                               "ingredients": g("vahoege_vads_paxel", "vahoege_vads_shears", "vahoege_vads_hoe"),
                                               "result": {"id": "guhs:vahoege_vadsknakker"}})
    w(f"{D}/recipe/super_vahoege_vadsknakker_van_delen.json", {"type": "guhs:vadsknakker", "category": "equipment",
                                                               "ingredients": g("super_vahoege_vads_paxel", "super_vahoege_vads_shears", "super_vahoege_vads_hoe"),
                                                               "result": {"id": "guhs:super_vahoege_vadsknakker"}})
    w(f"{D}/recipe/vahoegietstaaf.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                          "ingredients": g(*["vahoegiet"] * 4, *["vahoege_vads_ingot"] * 4),
                                          "result": {"id": "guhs:vahoegietstaaf"}})
    h.shaped("super_vads_sjabloon", ["VSV", "VKV", "VVV"], {"V": "guhs:vahoegiet", "S": "guhs:super_vads_sjabloon", "K": "guhs:guh_kristalsteen"},
             "guhs:super_vads_sjabloon", 2)
    for n in VADS + ["knakker"]:
        van = "guhs:vahoege_vadsknakker" if n == "knakker" else f"guhs:vahoege_vads_{n}"
        naar = "guhs:super_vahoege_vadsknakker" if n == "knakker" else f"guhs:super_vahoege_vads_{n}"
        w(f"{D}/recipe/{naar.split(':')[1]}_smithing.json", {"type": "minecraft:smithing_transform", "template": "guhs:super_vads_sjabloon",
                                                             "base": van, "addition": "guhs:vahoegietstaaf", "result": {"id": naar}})
    # early-game friendly: wool, feathers and a bit of vahoege vads
    h.shaped("wolkenliftje", ["WFW", "FVF", "WFW"], {"W": "minecraft:pink_wool", "F": "minecraft:feather", "V": "guhs:vahoege_vads"},
             "guhs:wolkenliftje", 2)
    h.shaped("pluisladder", ["S S", "WWW", "S S"], {"S": "minecraft:string", "W": "minecraft:pink_wool"}, "guhs:pluisladder")
    h.shaped("terrariumbodem", ["G", "S", "P"], {"G": "guhs:terrariumglas", "S": "minecraft:sand", "P": "minecraft:stripped_oak_log"},
             "guhs:terrariumbodem", 2)
    h.shaped("mijnlampje", [" L ", "WWW", "W W"], {"L": "minecraft:lantern", "W": "minecraft:pink_wool"}, "guhs:mijnlampje")


TEKSTEN = {  # key: (English, Dutch)
    "item.guhs.vahoege_vadsknakker": ("Vahoege Vadscracker", "Vahoege Vadsknakker"),
    "item.guhs.vahoege_vadsknakker.lore": ("Pickaxe, axe, shovel, hoe and shears in one. Right-click is smart: logs, grass, a sheep... Sneak = path",
                                           "Houweel, bijl, schep, schoffel en schaar in een. Rechtsklik doet slim: stammen, gras, een schaap... Sluipen = paadje"),
    "item.guhs.super_vahoege_vadsknakker": ("Super Vahoege Vadscracker", "Super Vahoege Vadsknakker"),
    "item.guhs.vahoegietstaaf": ("Vahoegite Ingot", "Vahoegietstaaf"),
    "item.guhs.vahoegietstaaf.lore": ("Makes vahoege vads super on the smithing table (with the template)",
                                      "Maakt vahoege vads super op de smidstafel (met het sjabloon)"),
    "item.guhs.super_vads_sjabloon": ("Super Vads Template", "Super-vads-sjabloon"),
    "item.guhs.super_vads_sjabloon.lore": ("Smithing table: template + a vahoege vads item + a Vahoegite Ingot. Copy it with 7 vahoegite and guh crystal stone",
                                           "Smidstafel: sjabloon + een vahoege vads-item + een Vahoegietstaaf. Kopieer hem met 7 vahoegiet en guhkristalsteen"),
    "item.guhs.super_vahoege_vads_sword": ("Super Vahoege Vads Sword", "Super vahoege vads-zwaard"),
    "item.guhs.super_vahoege_vads_pickaxe": ("Super Vahoege Vads Pickaxe", "Super vahoege vads-houweel"),
    "item.guhs.super_vahoege_vads_axe": ("Super Vahoege Vads Axe", "Super vahoege vads-bijl"),
    "item.guhs.super_vahoege_vads_shovel": ("Super Vahoege Vads Shovel", "Super vahoege vads-schep"),
    "item.guhs.super_vahoege_vads_hoe": ("Super Vahoege Vads Hoe", "Super vahoege vads-schoffel"),
    "item.guhs.super_vahoege_vads_paxel": ("Super Vahoege Vads Paxel", "Super vadspaxel"),
    "item.guhs.super_vahoege_vads_shears": ("Super Vahoege Vads Shears", "Super vahoege vadsschaar"),
    "item.guhs.super_vahoege_vads_helmet": ("Super Vahoege Vads Helmet", "Super vahoege vads-helm"),
    "item.guhs.super_vahoege_vads_chestplate": ("Super Vahoege Vads Chestplate", "Super vahoege vads-borstplaat"),
    "item.guhs.super_vahoege_vads_leggings": ("Super Vahoege Vads Leggings", "Super vahoege vads-beenbeschermers"),
    "item.guhs.super_vahoege_vads_boots": ("Super Vahoege Vads Boots", "Super vahoege vads-laarzen"),
    "block.guhs.wolkenliftje": ("Cloud Lift", "Wolkenliftje"),
    "block.guhs.wolkenliftje.lore": ("Jump on it: up to the next one above. Sneak: down to the next one below",
                                     "Spring erop: omhoog naar het volgende erboven. Sluipen: omlaag naar het volgende eronder"),
    "block.guhs.pluisladder": ("Fluff Ladder", "Pluisladder"),
    "block.guhs.pluisladder.lore": ("Put it against a wall at the edge of a hole: it unrolls down to the floor by itself",
                                    "Zet hem tegen een wand aan de rand van een gat: hij rolt vanzelf uit tot de bodem"),
    "block.guhs.terrariumbodem": ("Terrarium Floor", "Terrariumbodem"),
    "block.guhs.terrariumbodem.lore": ("Build it onto a terrarium (with terrarium glass): its little creatures walk all through it",
                                       "Bouw het aan een terrarium (met terrariumglas): de diertjes lopen er overal doorheen"),
    "block.guhs.mijnlampje_licht": ("Little Mine Lamp Light", "Licht van een mijnlampje"),
    "item.guhs.mijnlampje": ("Little Mine Lamp", "Mijnlampje"),
    "item.guhs.mijnlampje.lore": ("A pink hard hat with a lamp for your guh: it lights up the dark wherever it walks",
                                  "Een roze helmpje met lampje voor je guh: hij geeft licht in het donker, waar hij ook loopt"),
}


def texts(h):
    for k, (en, nl) in TEKSTEN.items():
        h.lang(k, en, nl)


def build(h):
    tex.textures(h)
    models(h)
    loot(h)
    tags(h)
    recipes(h)
    texts(h)


ITEMS = HANDHELD + PLAT + ["wolkenliftje", "terrariumbodem"]


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
    for i in ITEMS:
        w(f"{A}/items/{i}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{i}"}})
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    print(f"mijnhulp: {len(ITEMS)} items, {len(NL)} texts")


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
