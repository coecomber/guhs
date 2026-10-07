"""
einde2 (the Guheinde expansion), slice sponsguh_wezens: the cast of the Sponsguh story as creatures, the Knabbelburgermobiel and the story's pure-content rewards (food, blocks, a toy, outfits).
Java: nl.juiced.guhs.feature.einde2.sponsguh (E2SponsguhFeature).

Skeleton stub: the sponsguh_wezens slice replaces this module's content (it stays in features.FEATURES at this place).
For now: plain stand-in blocks and items under the ids the two builds of the story place, and stand-in Guhdex texts and names
for the buddy and the pet (the rest of the cast is the slice's own).
"""
import features.einde2_kit as kit

BLOCKS = {  # id: (Dutch name, colour)
    "karen": ("Karen", (70, 110, 120)), "krokante_grill": ("Krokante grill", (120, 120, 130)), "besteltafel": ("Besteltafel", (170, 120, 70)),
    "vruchthuis_wand": ("Vruchthuiswand", (230, 140, 60)), "vruchthuis_dak": ("Vruchthuisdak", (90, 170, 80)),
    "vruchthuis_raam": ("Vruchthuisraam", (170, 210, 230)), "vruchthuis_deur": ("Vruchthuisdeur", (150, 100, 190)),
    "bellenblaas": ("Bellenblaas", (240, 150, 200)), "spooklantaarn": ("Spooklantaarn", (120, 230, 150)),
}
ITEMS = {"knabbelburger": ("Knabbelburger", (200, 130, 60)), "kwallenjam": ("Kwallenjam", (230, 120, 190))}
DIEREN = {  # entity id (= its Guhdex page): (Dutch name, where)
    "sponsguh": ("Sponsguh Kaasbroek", "Woont in Bikinibodem, diep onder de eilanden"), "gerrit": ("Gerrit", "De slak van Sponsguh"),
}


def build(h):
    for block, (naam, rgb) in BLOCKS.items():
        kit.stub_blok(h, block, naam, rgb)
    for item, (naam, rgb) in ITEMS.items():
        kit.stub_item(h, item, naam, rgb)
    for id, (naam, waar) in DIEREN.items():
        kit.pagina(h, id, naam, waar, f"{naam}: woont diep onder de buiteneilanden van het Guheinde. Njeg, meer weten we nog niet.")
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})
