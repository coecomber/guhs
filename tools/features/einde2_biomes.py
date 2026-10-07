"""
einde2 (the Guheinde expansion), slice biomes: the six biomes of the outer islands: their blocks, plants, worldgen features, the kristalkaas and the knabbelkoor.
Java: nl.juiced.guhs.feature.einde2.biomes (E2BiomesFeature).

Skeleton stub: the biomes slice replaces this module's content (it stays in features.FEATURES at this place).
For now: six stand-in biomes (copies of guhs:guheinde, so the biome tags of einde2 load; the dimension keeps its one fixed biome
until the slice gives it the real biome source) and plain stand-in blocks and items under the ids the other slices build with.
"""
import json

import features.einde2_kit as kit

BIOMES = {"gatenkaasheuvels": "Gatenkaasheuvels", "korstwoestijn": "Korstwoestijn", "kruimelwolken": "Kruimelwolken",
          "schimmelkaasvlakte": "Schimmelkaasvlakte", "kristalkaasklippen": "Kristalkaasklippen", "knabbelkoorbos": "Knabbelkoorbos"}
BLOCKS = {  # id: (Dutch name, colour)
    "kruimelrots": ("Kruimelrots", (214, 176, 96)), "kruimelzand": ("Kruimelzand", (236, 206, 140)),
    "gebarsten_korst": ("Gebarsten korst", (200, 150, 70)), "kruimelwolk": ("Kruimelwolk", (250, 240, 214)),
    "dichte_kruimelwolk": ("Dichte kruimelwolk", (240, 226, 190)), "schimmelkaas": ("Schimmelkaas", (206, 214, 190)),
    "klipkaas": ("Klipkaas", (226, 190, 110)), "kristalkaas_kristal": ("Kristalkaaskristal", (255, 214, 90)),
    "koorkorst": ("Koorkorst", (190, 150, 190)), "knabbelkoorplant": ("Knabbelkoorplant", (170, 110, 180)),
    "knabbelkoorbloem": ("Knabbelkoorbloem", (214, 150, 220)), "knabbelkoorblok": ("Knabbelkoorblok", (180, 130, 190)),
    "lichtstaafje": ("Lichtstaafje", (255, 240, 200)),
}
ITEMS = {
    "kristalkaas": ("Kristalkaas", (255, 206, 80)), "kristalkaas_brokje": ("Kristalkaasbrokje", (255, 224, 130)),
    "knabbelkoorvrucht": ("Knabbelkoorvrucht", (200, 130, 210)), "gepofte_knabbelkoorvrucht": ("Gepofte knabbelkoorvrucht", (230, 190, 230)),
}


def build(h):
    assert sorted(BIOMES) == sorted(kit.BUITEN)
    midden = json.load(open(f"{h.D}/worldgen/biome/guheinde.json", encoding="utf-8"))
    for biome, naam in BIOMES.items():
        h.w(f"{h.D}/worldgen/biome/{biome}.json", midden)
        kit.lang(h, f"biome.guhs.{biome}", naam)
    for block, (naam, rgb) in BLOCKS.items():
        kit.stub_blok(h, block, naam, rgb)
    for item, (naam, rgb) in ITEMS.items():
        kit.stub_item(h, item, naam, rgb)
