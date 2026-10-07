"""
einde2 (the Guheinde expansion), slice bouw_vuur: the build of the Vuurnatie-hoofdstad (DESIGN 5, chapter 4), the fourth
story structure of the Guhvatar. Java: nl.juiced.guhs.feature.einde2.bouwvuur (E2BouwVuurFeature).

  - the structure guhs:vuurnatie_hoofdstad (type guhs:leegte_bouw, one guaranteed copy per world, hidden: in no compass),
    its tiles vuurnatie_hoofdstad/stuk_<i>_<j>, its overlay vuurnatie_hoofdstad/laag_leeuwschildpad and its markers in
    data/guhs/einde2/plekken/vuurnatie_hoofdstad.json: einde2_bouw_vuur_bouw.py (with _grond, _stad and _buiten);
  - the six decoration blocks it is built with (lukewarm lava and a lukewarm flame, the emblem, the portrait of the
    Vuurheer, the sun stone of the Zonnekrijgers, the tile of the Witte Lotus): textures in einde2_bouw_vuur_tex.py;
  - the chests (modest, never something a quest needs), the texts, the test room of the game tests.

BUILD only: no story logic and no characters. A later phase reads the markers (Plekken, build contract 6.6 - 6.8).
"""
import json
import os

import features.einde2_kit as kit
from features import einde2_bouw_vuur_bouw as bouw
from features import einde2_bouw_vuur_tex as tex

BLOKKEN = ["einde2_bouw_vuur_lauwe_lava", "einde2_bouw_vuur_lauwe_vlam", "einde2_bouw_vuur_embleem", "einde2_bouw_vuur_portret_mikai",
           "einde2_bouw_vuur_zonnesteen", "einde2_bouw_vuur_lotustegel"]
KISTEN = ["winkel", "school", "paleis", "gevangenis", "strandhuis", "lotus"]

TEXTS = {
    "block.guhs.einde2_bouw_vuur_lauwe_lava": "Lauwe lava",
    "block.guhs.einde2_bouw_vuur_lauwe_lava.lore": "Ziet er gevaarlijk uit, voelt als een kruik",
    "block.guhs.einde2_bouw_vuur_lauwe_vlam": "Lauwe vlam",
    "block.guhs.einde2_bouw_vuur_lauwe_vlam.lore": "Brandt niet, kietelt alleen een beetje, njeg",
    "block.guhs.einde2_bouw_vuur_embleem": "Vuurnatie-embleem",
    "block.guhs.einde2_bouw_vuur_embleem.lore": "Een vlam met een boze blik en één tandje",
    "block.guhs.einde2_bouw_vuur_portret_mikai": "Portret van Vuurheer Mikai",
    "block.guhs.einde2_bouw_vuur_portret_mikai.lore": "Hij kijkt je aan. Van alle kanten",
    "block.guhs.einde2_bouw_vuur_zonnesteen": "Zonnekrijgersteen",
    "block.guhs.einde2_bouw_vuur_zonnesteen.lore": "Duizend jaar oud en nog steeds lekker warm",
    "block.guhs.einde2_bouw_vuur_lotustegel": "Witte Lotus-tegel",
    "block.guhs.einde2_bouw_vuur_lotustegel.lore": "Wie hem neerlegt krijgt thee van een oude meester",
}
TITEL = "Vuurnatie-hoofdstad"
TOOLTIP = "De hoofdstad van de Vuurnatie, in de krater van een vulkaan van aangebrande kaaskorst"


def blocks_and_items(h):
    A, w = h.A, h.w
    for b in ("einde2_bouw_vuur_lauwe_lava", "einde2_bouw_vuur_embleem", "einde2_bouw_vuur_zonnesteen", "einde2_bouw_vuur_lotustegel"):
        h.simple_block(b)
    # the flame: two crossed planes, like a plant
    f = "einde2_bouw_vuur_lauwe_vlam"
    w(f"{A}/models/block/{f}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{f}"}})
    w(f"{A}/blockstates/{f}.json", {"variants": {"": {"model": f"guhs:block/{f}"}}})
    h.item_model(f, f"guhs:block/{f}")
    # the portrait: the Vuurheer on four sides, the frame on top and under it
    p = "einde2_bouw_vuur_portret_mikai"
    w(f"{A}/models/block/{p}.json", {"parent": "minecraft:block/cube_column", "textures": {"end": f"guhs:block/{p}_top", "side": f"guhs:block/{p}"}})
    w(f"{A}/blockstates/{p}.json", {"variants": {"": {"model": f"guhs:block/{p}"}}})
    w(f"{A}/models/item/{p}.json", {"parent": f"guhs:block/{p}"})
    for b in BLOKKEN:
        h.self_drop(b)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{b}" for b in BLOKKEN if b != f])


def loot(h):
    """The six chests of the capital: a handful of knabbels and something that fits the place. Nothing a quest needs."""
    def item(name, lo=1, hi=1, weight=1):
        e = {"type": "minecraft:item", "name": name, "weight": weight}
        if hi > 1:
            e["functions"] = h.count_fn(lo, hi)
        return e
    knabbels = {"rolls": 1, "entries": [item("guhs:kaas_knabbels", 2, 6)]}
    inhoud = {
        "winkel": [item("minecraft:bread", 1, 3, 3), item("minecraft:red_wool", 1, 4, 2), item("minecraft:orange_dye", 1, 3, 2), item("minecraft:paper", 2, 5, 2)],
        "school": [item("minecraft:paper", 2, 6, 3), item("minecraft:book", 1, 2, 2), item("minecraft:ink_sac", 1, 2, 1), item("minecraft:feather", 1, 2, 1)],
        "paleis": [item("minecraft:gold_nugget", 3, 9, 3), item("minecraft:red_carpet", 2, 6, 2), item("minecraft:book", 1, 1, 1),
                   item("guhs:einde2_bouw_vuur_embleem", 1, 1, 1)],
        "gevangenis": [item("minecraft:bread", 1, 2, 3), item("minecraft:iron_nugget", 2, 6, 2), item("minecraft:bowl", 1, 2, 2), item("minecraft:string", 1, 3, 1)],
        "strandhuis": [item("minecraft:nautilus_shell", 1, 1, 1), item("minecraft:orange_carpet", 1, 3, 2), item("minecraft:bamboo", 2, 6, 3),
                       item("guhs:einde2_bouw_vuur_portret_mikai", 1, 1, 1)],
        "lotus": [item("guhs:einde2_bouw_vuur_lotustegel", 1, 2, 2), item("minecraft:white_wool", 1, 3, 2), item("minecraft:paper", 1, 3, 2),
                  item("minecraft:honey_bottle", 1, 1, 1)],
    }
    for naam in KISTEN:
        h.w(f"{h.D}/loot_table/chests/einde2_bouw_vuur_{naam}.json", {"type": "minecraft:chest", "pools": [
            knabbels, {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": inhoud[naam]}]})


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def structuur(h):
    """The build: self-check, tiles, overlay, and everything of the structure the kit writes."""
    b = bouw.maak(h, opslaan=True, controle=True)
    kit.leegte_structuur(h, b, titel=TITEL, tooltip=TOOLTIP, y=bouw.Y_WERELD)
    return b


def testkamer(h):
    """The room of the game tests: a floor with one of each decoration block on it."""
    s = h.Structure((9, 5, 5))
    s.fill(0, 0, 0, 8, 0, 4, "guhs:houtskoolsteen_stenen")
    for k, blok in enumerate(BLOKKEN):
        s.set(1 + k, 1, 2, f"guhs:{blok}")
    s.save("einde2_bouw_vuur_test_kamer")


def selfcheck(h, b):
    """Every block has its files and its texts; the structure has its tiles, its overlay and its sidecar; every marker of
    the build contract is in the sidecar and lies inside the build."""
    A, D = h.A, h.D
    missing = []
    for blok in BLOKKEN:
        for path in (f"{A}/blockstates/{blok}.json", f"{A}/models/block/{blok}.json", f"{A}/models/item/{blok}.json",
                     f"{D}/loot_table/blocks/{blok}.json", os.path.join(h.TEX, "block", f"{blok}.png")):
            if not os.path.exists(path):
                missing.append(path)
    missing += [k for k in TEXTS if k not in h.NL]
    for naam in tex.BLOCKS:
        if not os.path.exists(os.path.join(h.TEX, "block", f"{naam}.png")):
            missing.append(f"texture {naam}")
    for naam in KISTEN:
        if not os.path.exists(f"{D}/loot_table/chests/einde2_bouw_vuur_{naam}.json"):
            missing.append(f"loot {naam}")
    nx, nz, saved = b.tiles
    for i, j in saved:
        if not os.path.exists(f"{D}/structure/{bouw.NAAM}/stuk_{i}_{j}.nbt"):
            missing.append(f"tile {i}_{j}")
    if not os.path.exists(f"{D}/structure/{bouw.NAAM}/laag_leeuwschildpad.nbt"):
        missing.append("laag_leeuwschildpad")
    blad = json.load(open(f"{D}/einde2/plekken/{bouw.NAAM}.json", encoding="utf-8"))
    mis = b.missend(bouw.VERWACHT + kit.STANDAARD + kit.VERHAAL_STANDAARD)
    if mis or len(blad["plekken"]) != len(b.plekken) or "leeuwschildpad" not in blad.get("lagen", {}):
        missing.append(f"sidecar: {mis}")
    for kist in {bl[2]["LootTable"] for bl in b.s.blocks.values() if bl[2] and "LootTable" in bl[2]}:
        if not os.path.exists(f"{D}/loot_table/{kist.split(':')[1]}.json"):
            missing.append(f"loot table {kist}")
    if missing:
        raise SystemExit(f"einde2_bouw_vuur: missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    loot(h)
    texts(h)
    testkamer(h)
    b = structuur(h)
    selfcheck(h, b)
