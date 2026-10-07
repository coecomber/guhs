"""
einde2 (the Guheinde expansion), slice bouw_zij2: the builds of the Kyoshi-dorp, the Bibliotheek van Wan Shi Guh, the Leeuwschildpad-eiland, the Leegtehaven and the ringenrace.
Java: nl.juiced.guhs.feature.einde2.bouwzij2 (E2BouwZij2Feature).

Skeleton stub: the bouw_zij2 slice replaces this module's content (it stays in features.FEATURES at this place).
For now: a little stand-in platform per place, as a real guhs:leegte_bouw (so the Superkompas knows its ids).
"""
import features.einde2_kit as kit

PLEKKEN = {"kyoshi_dorp": "Kyoshi-dorp", "bibliotheek_wan_shi_guh": "Bibliotheek van Wan Shi Guh", "leeuwschildpad_eiland": "Leeuwschildpad-eiland",
           "leegtehaven": "Leegtehaven", "guhvleugel_ringenrace": "Guhvleugel-ringenrace"}


def build(h):
    for id, titel in PLEKKEN.items():
        kit.stub_structuur(h, id, titel)
