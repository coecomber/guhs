"""
einde2 (the Guheinde expansion), slice appa: the Appa-guh and the Momo-guh.
Java: nl.juiced.guhs.feature.einde2.appa (E2AppaFeature).

Skeleton stub: the appa slice replaces this module's content (it stays in features.FEATURES at this place).
For now: stand-in Guhdex texts and names for its two creatures.
"""
import features.einde2_kit as kit

DIEREN = {  # entity id (= its Guhdex page): (Dutch name, where)
    "appa_guh": ("Appa-guh", "Er is er maar één, en die kiest zelf"), "momo_guh": ("Momo-guh", "Er is er maar één, en die kiest zelf"),
}


def build(h):
    for id, (naam, waar) in DIEREN.items():
        kit.pagina(h, id, naam, waar, f"{naam}: woont op de buiteneilanden van het Guheinde. Njeg, meer weten we nog niet.")
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})
