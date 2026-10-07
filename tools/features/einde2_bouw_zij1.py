"""
einde2 (the Guheinde expansion), slice bouw_zij1: the builds of Omashguh, the Westelijke Luchttempel and the Vuurtempel van Roku-guh.
Java: nl.juiced.guhs.feature.einde2.bouwzij1 (E2BouwZij1Feature).

Skeleton stub: the bouw_zij1 slice replaces this module's content (it stays in features.FEATURES at this place).
For now: a little stand-in platform per place, as a real guhs:leegte_bouw (so the Superkompas knows its ids).
"""
import features.einde2_kit as kit

PLEKKEN = {"omashguh": "Omashguh", "westelijke_luchttempel": "Westelijke Luchttempel", "vuurtempel_roku": "Vuurtempel van Roku-guh"}


def build(h):
    for id, titel in PLEKKEN.items():
        kit.stub_structuur(h, id, titel)
