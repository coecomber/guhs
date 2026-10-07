"""
einde2 (the Guheinde expansion), slice zwevers: the Kruimelkwallen, the Leegtewalvisguh and the four element animals.
Java: nl.juiced.guhs.feature.einde2.zwevers (E2ZweversFeature).

Skeleton stub: the zwevers slice replaces this module's content (it stays in features.FEATURES at this place).
For now: stand-in Guhdex texts and names for its six creatures.
"""
import features.einde2_kit as kit

DIEREN = {  # entity id (= its Guhdex page): (Dutch name, where)
    "kruimelkwal": ("Kruimelkwal", "In zwermen in de Kruimelwolken"), "leegtewalvisguh": ("Leegtewalvisguh", "Heel zeldzaam, tussen de eilanden"),
    "vleermuis_lemurguh": ("Vleermuis-lemurguh", "Zeldzaam in de Kruimelwolken"),
    "otter_pinguinguh": ("Otter-pinguïnguh", "Zeldzaam op de Schimmelkaasvlakte"),
    "das_molguh": ("Das-molguh", "Zeldzaam in de Kristalkaasklippen"),
    "schildpad_eendguh": ("Schildpad-eendguh", "Zeldzaam in het Knabbelkoorbos"),
}


def build(h):
    for id, (naam, waar) in DIEREN.items():
        kit.pagina(h, id, naam, waar, f"{naam}: woont op de buiteneilanden van het Guheinde. Njeg, meer weten we nog niet.")
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})
