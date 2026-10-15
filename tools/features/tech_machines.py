"""
bbq2 (tech-machines): Oogster, Knabbelaar, Neerzetter, Knutselmachine + Tekentafel + Bouwtekening, Plantagebak, the powered
molen (the Vadsmolen). Java: feature/techmachine.

Guh machines that do the work for you, all on vadskracht (features/vadskracht.py), all little guhs: a face that shows how it
is doing, two ears and a part that moves while it works. This module makes:
  - the looks (tech_machines_modellen.py: textures, block models, the moving parts, blockstates, item models)
  - the texts (tech_machines_tekst.py: names, the line on the item, the hover readout, the screens, the Bouwtekening)
  - loot tables (every block drops itself; the eight other blocks of a Plantagebak drop nothing), the tags (mineable,
    guhs:vadskracht, guhs:techmachine_knabbelt_niet)
  - the recipes (RECEPTEN; tiers of DESIGN_130: the Vadsmolen is "Knutselen", the Oogster "Zout" (zoutkristal), the rest
    "Saus" (grillspies / blubroom)). Another slice that gates a machine behind its recipe card calls
    recept(h, "<machine>", kaart="guhs:<its card>") from its own module (it runs later, so its file wins).
  - data/guhs/techmachine/malen.json: what the Vadsmolen grinds (MALEN; Java: Maalrecepten)
  - the advancements: hidden guhs:quest/tech_machines_<name> (for FTB tasks of tech-quests) and visible
    guhs:techniek/tech_machines_<name>, both granted by Java when a machine of yours does its thing for the first time
  - the test templates techmachine_test_kamer (9 x 6 x 9) and techmachine_test_bos (17 x 30 x 17), both a stone floor
No FTB quests here: the chapter Guh-technologie is written by tech_quests.py (CONTRACT_130 8).
"""
import os
import re

from features import bbq2, tech_machines_modellen as modellen, tech_machines_tekst as tekst

# name -> (pattern, key, count). The top-left cell is always something the recipe has more of: recept(..., kaart=) puts
# a recipe card there.
RECEPTEN = {
    "vadsmolen": (["IMI", "IDI", "SKS"], {"I": "minecraft:iron_ingot", "M": "guhs:guh_molentje", "D": "guhs:guh_wire",
                                          "S": "#minecraft:stone_crafting_materials", "K": "guhs:kaas_knabbels"}, 1),
    "oogster": (["IHI", "ZDZ", "SKS"], {"I": "minecraft:iron_ingot", "H": "minecraft:iron_hoe", "Z": "guhs:zoutkristal", "D": "guhs:guh_wire",
                                        "S": "#minecraft:stone_crafting_materials", "K": "guhs:kaas_knabbels"}, 1),
    "knabbelaar": (["IPI", "GDG", "SKS"], {"I": "minecraft:iron_ingot", "P": "minecraft:iron_pickaxe", "G": "guhs:grillspies", "D": "guhs:guh_wire",
                                           "S": "#minecraft:stone_crafting_materials", "K": "guhs:kaas_knabbels"}, 1),
    "neerzetter": (["IRI", "GDG", "SKS"], {"I": "minecraft:iron_ingot", "R": "minecraft:dropper", "G": "guhs:grillspies", "D": "guhs:guh_wire",
                                           "S": "#minecraft:stone_crafting_materials", "K": "guhs:kaas_knabbels"}, 1),
    "knutselmachine": (["ICI", "GDG", "SKS"], {"I": "minecraft:iron_ingot", "C": "minecraft:crafting_table", "G": "guhs:grillspies",
                                               "D": "guhs:guh_wire", "S": "#minecraft:stone_crafting_materials", "K": "guhs:kaas_knabbels"}, 1),
    # (the grillspies is its pencil)
    "tekentafel": (["PPP", "WGW", "W W"], {"P": "minecraft:paper", "W": "#minecraft:planks", "G": "guhs:grillspies"}, 1),
    # (blubroom: the best manure there is)
    "plantagebak": (["LEL", "LBL", "LDL"], {"L": "#minecraft:logs", "E": "minecraft:dirt", "B": "guhs:blubroom", "D": "guhs:guh_wire"}, 1),
}

# what the Vadsmolen grinds: (in: item or #tag, out, how many, ticks of work). The first that fits wins.
MALEN = [
    ("#guhs:knus/knabbelgraan", "guhs:knabbelmeel", 1, 20),      # (the guh-molentje: 80 ticks in calm weather)
    ("minecraft:bone", "minecraft:bone_meal", 4, 30),            # (by hand: 3)
    ("minecraft:sugar_cane", "minecraft:sugar", 2, 20),          # (by hand: 1)
    ("guhs:grillspies", "guhs:grillspiespoeder", 3, 40),         # (by hand: 2)
    ("minecraft:blaze_rod", "minecraft:blaze_powder", 3, 40),    # (by hand: 2)
    ("minecraft:cobblestone", "minecraft:gravel", 1, 40),
    ("minecraft:gravel", "minecraft:sand", 1, 40),
]

# never gnawed (next to everything with a block entity and every vadskracht block, which Java refuses by itself): blocks
# that are not meant to be mined, and the invisible parts of the mod's bigger things (a bite would take the whole thing)
KNABBELT_NIET = ["minecraft:reinforced_deepslate", "minecraft:budding_amethyst", "guhs:guhhuisje_deel", "guhs:speelgoed_deel",
                 "guhs:slee_rail_part", "guhs:guh_wheel_part",
                 # (added at the merge) machine parts of the other tech slices that have no block entity and are no
                 # vadskracht block, and the two quest blocks of the Zoutkristalmijn that only "break" for a player
                 "guhs:knabbelbuis", "guhs:sausslang", "guhs:tekentafel", "guhs:fossielmijn_zoutader", "guhs:fossielmijn_puin"]


def recept(h, naam, kaart=None):
    """Writes the crafting recipe of a machine. kaart: an item id that takes the top-left cell (a recipe card that stays:
    give that item itself as its crafting remainder)."""
    patroon, sleutel, aantal = RECEPTEN[naam]
    patroon, sleutel = list(patroon), dict(sleutel)
    if kaart:
        patroon[0] = "X" + patroon[0][1:]
        sleutel["X"] = kaart
        sleutel = {k: v for k, v in sleutel.items() if any(k in rij for rij in patroon)}
    h.shaped(naam, patroon, sleutel, f"guhs:{naam}", aantal)


def recepten(h):
    for naam in RECEPTEN:
        recept(h, naam)
    # an empty sheet: paper turned blue. The Tekentafel is what is hard to get, not the paper
    h.shapeless("bouwtekening", ["minecraft:paper", "minecraft:blue_dye"], "guhs:bouwtekening", 2)


def data(h):
    h.w(f"{h.D}/techmachine/malen.json", {"recepten": [{"in": i, "uit": u, "aantal": n, "ticks": t} for i, u, n, t in MALEN]})
    h.add_tag("guhs/tags/block/techmachine_knabbelt_niet", KNABBELT_NIET)


def texts(h):
    for key, nl in tekst.teksten().items():
        h.lang(key, nl, nl)


def advancements(h):
    for naam, (ouder, icoon, lijst, titel, uitleg) in tekst.MIJLPALEN.items():
        bbq2.verborgen(h, f"tech_machines_{naam}")
        bbq2.zichtbaar(h, "techniek", f"tech_machines_{naam}", ouder, icoon, lijst, titel, uitleg)


def test_templates(h):
    s = h.Structure((9, 6, 9))
    s.fill(0, 0, 0, 8, 0, 8, "minecraft:stone")
    s.save("techmachine_test_kamer")
    s = h.Structure((17, 30, 17))
    s.fill(0, 0, 0, 16, 0, 16, "minecraft:stone")
    s.save("techmachine_test_bos")


def selfcheck(h):
    A, D = h.A, h.D
    paden = []
    for m in modellen.MACHINES:
        paden += [f"{A}/blockstates/{m}.json", f"{A}/models/item/{m}.json", f"{D}/loot_table/blocks/{m}.json", f"{D}/recipe/{m}.json"]
        paden += [f"{A}/models/block/{m}_{s}.json" for s in modellen.STATEN]
        paden += [os.path.join(h.TEX, "block", f"{m}_voor_{s}.png") for s in modellen.STATEN]
    paden += [f"{A}/models/block/{d}.json" for d in modellen.DELEN]
    paden += [f"{A}/blockstates/techmachine_plantagebak_deel.json", f"{A}/models/item/bouwtekening.json", f"{A}/models/item/bouwtekening_getekend.json",
              f"{D}/recipe/bouwtekening.json", f"{D}/techmachine/malen.json", f"{D}/tags/block/techmachine_knabbelt_niet.json",
              f"{D}/structure/techmachine_test_kamer.nbt", f"{D}/structure/techmachine_test_bos.nbt"]
    paden += [f"{D}/advancement/quest/tech_machines_{n}.json" for n in tekst.MIJLPALEN]
    paden += [f"{D}/advancement/techniek/tech_machines_{n}.json" for n in tekst.MIJLPALEN]
    missing = [p for p in paden if not os.path.exists(p)]
    missing += [k for k in tekst.teksten() if k not in h.NL]
    # the moving parts: the same list here and in the renderer's table
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techmachine", "client", "MachineRenderer.java"),
                encoding="utf-8").read()
    in_java = set(re.findall(r'deel\("([a-z_]+)"', java))
    if in_java != set(modellen.DELEN):
        missing.append(f"MachineRenderer parts {sorted(in_java)} != models {sorted(modellen.DELEN)}")
    # every advancement Java grants exists
    for pad in ("TechBlockEntity.java", "TekentafelMenu.java", "OogsterBlockEntity.java", "KnabbelaarBlockEntity.java", "NeerzetterBlockEntity.java",
                "VadsmolenBlockEntity.java", "KnutselmachineBlockEntity.java", "PlantagebakBlockEntity.java"):
        src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techmachine", pad), encoding="utf-8").read()
        for naam in re.findall(r'beloon\((?:\w+, )?"([a-z_]+)"\)', src):
            if naam not in tekst.MIJLPALEN:
                missing.append(f"{pad}: beloon(\"{naam}\") has no advancement")
    if missing:
        raise SystemExit(f"tech_machines: missing {missing}")


def build(h):
    modellen.build(h)
    recepten(h)
    data(h)
    texts(h)
    advancements(h)
    test_templates(h)
    selfcheck(h)
