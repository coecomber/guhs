"""
einde2 (the Guheinde expansion), slice bouw_bikinibodem: the build of Bikinibodem onder de leegte (the town under its soap-bubble dome, deep between the islands).
Java: nl.juiced.guhs.feature.einde2.bouwbikinibodem (E2BouwBikinibodemFeature).

Skeleton stub: the bouw_bikinibodem slice replaces this module's content (it stays in features.FEATURES at this place).
For now: plain stand-in blocks under the ids the build of Schelpenstad may place too, and a little stand-in platform in the
deep band as a real guhs:leegte_bouw with its guaranteed set (so the Superkompas knows the id and Schelpenstad has its
neighbour), without the terrain-free box the real town asks for.
"""
import features.einde2_kit as kit

BLOCKS = {"zeepbel": ("Zeepbel", (200, 230, 250)), "bubbelstroom": ("Bubbelstroom", (170, 220, 250)), "bloemwolk": ("Bloemwolk", (250, 190, 220))}


def build(h):
    for block, (naam, rgb) in BLOCKS.items():
        kit.stub_blok(h, block, naam, rgb)
    kit.stub_structuur(h, "bikinibodem", "Bikinibodem")
