"""
einde2 phase B/C (CONTRACT_E2_B 7), slice h1_lucht: chapter 1 of the Guhvatar story (the kaasbol, the opening film, the Zuidelijke Luchttempel, Zuka at the pier), the recipe of the Oog van Vadsig, the travel map.
Java: nl.juiced.guhs.feature.einde2.h1lucht (E2H1LuchtFeature).

Skelet stub: the h1_lucht slice replaces this module's content (it stays in features.FEATURES at this place). Chunk:
CONTRACT_E2_B 1.3. For now: the name of the stand-in entity kaasbol.
"""
import features.einde2_kit as kit  # noqa: F401

# the kaasbol (CONTRACT_E2_B 7.2): a stand-in entity type (Java: E2H1LuchtFeature.KAASBOL) until this slice makes the real one
DIEREN = {"kaasbol": "Kaasbol"}


def build(h):
    for id, naam in DIEREN.items():
        kit.lang(h, f"entity.guhs.{id}", naam)
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})
