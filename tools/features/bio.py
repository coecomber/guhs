"""
biomes3, the kern (Java: feature/bio; English: tools/lang/en/c80_bio_kern.json; contract: CONTRACT_BIO.md).

Three new Guhmensie biomes (Bloesemmeertje, Klaterdal, Wolkenweide) with their blocks, animals and structures, and the
Superkompas tab "Biomes"; one module per slice (bio_<slice>.py, each a stub until its slice fills it in), helpers in
bio_lib.py. The kern itself makes no resources: it only checks that the pieces every slice leans on are there.
"""
import json
import os

BIOMES = ("bloesemmeertje", "klaterdal", "wolkenweide")
# (the slice numbers of the contract = the chunk numbers c80 + Nr; FEATURES runs the two block slices before wereld)
SLICES = ("kern", "wereld", "blokken_dal", "blokken_wolk", "dieren", "kompas", "bouw_dal", "bouw_meer", "bouw_wolk1", "bouw_wolk2", "systemen")


def build(h):
    pass


def selfcheck(h):
    """Called by the LAST module (bio_systemen), when every slice module has run."""
    problems = []
    for b in BIOMES:
        if not os.path.exists(f"{h.D}/worldgen/biome/{b}.json"):
            problems.append(f"missing data/guhs/worldgen/biome/{b}.json")
        if f"biome.guhs.{b}" not in h.NL:
            problems.append(f"no name biome.guhs.{b}")
    chunks = {c["id"] for c in json.load(open(os.path.join("tools", "lang", "chunks.json"), encoding="utf-8"))["chunks"]}
    for i, s in enumerate(SLICES):
        cid = f"c{80 + i}_bio_{s}"
        if cid not in chunks:
            problems.append(f"tools/lang/chunks.json has no chunk {cid}")
        if not os.path.exists(os.path.join("tools", "lang", "en", f"{cid}.json")):
            problems.append(f"missing tools/lang/en/{cid}.json")
    if problems:
        raise SystemExit("biomes3 self-check failed:\n  " + "\n  ".join(problems))
