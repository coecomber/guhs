"""
einde2 (the Guheinde expansion), slice mikas: the Ender-Mika, the Kaasmijt, the Schuilguh and the Schuildoos.
Java: nl.juiced.guhs.feature.einde2.mikas (E2MikasFeature).

Skeleton stub: the mikas slice replaces this module's content (it stays in features.FEATURES at this place).
For now: stand-in Guhdex texts and names for its three creatures and plain stand-in items for flitskruimel and doosschil.
"""
import features.einde2_kit as kit

DIEREN = {  # entity id (= its Guhdex page): (Dutch name, where)
    "ender_mika": ("Ender-Mika", "Op de buiteneilanden van het Guheinde"), "kaasmijt": ("Kaasmijt", "Soms uit een knabbelkoorvrucht"),
    "schuilguh": ("Schuilguh", "Op de muren van een Mika-vesting"),
}
ITEMS = {"flitskruimel": ("Flitskruimel", (120, 60, 160)), "doosschil": ("Doosschil", (236, 196, 110))}


def build(h):
    for id, (naam, waar) in DIEREN.items():
        kit.pagina(h, id, naam, waar, f"{naam}: woont op de buiteneilanden van het Guheinde. Njeg, meer weten we nog niet.")
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})
    for item, (naam, rgb) in ITEMS.items():
        kit.stub_item(h, item, naam, rgb)
