"""
biomes3 slice "wereld" (Java: feature/bio/wereld; English: tools/lang/en/c81_bio_wereld.json; contract: CONTRACT_BIO.md).

The three biomes: placement, terrain, trees, colours, particles, music, day rhythm. A stub until its slice fills it in.

Until then it holds the PLACEHOLDERS of the three biome ids: a minimal biome file and the name of each, so that the ids
exist for every other slice (a spawn list, a tag or a structure that names a biome that does not exist stops the world
from loading). They are in no dimension yet. The wereld slice replaces the placeholders with the real biomes.
"""
from features import bio_lib as lib

NAMEN = {"bloesemmeertje": "Bloesemmeertje", "klaterdal": "Klaterdal", "wolkenweide": "Wolkenweide"}


def build(h):
    for bid, naam in NAMEN.items():
        lib.biome_plaatshouder(h, bid)
        lib.teksten(h, {f"biome.guhs.{bid}": naam})
    selfcheck(h)


def selfcheck(h):
    pass
