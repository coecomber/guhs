"""
1.8.0 fix (testronde 1.8): the entrance huts of the 1.7.0 underground buildings (the mijnwerker-guh's hut on the surface,
Java GrotBouwStructure.ingang) stood in the Bloesemmeertje lake. The lakes, rivers and ditches of the Guhmension are
filled in at worldgen step "lakes", long after a structure has chosen its spot, and the dimension's sea level is 0: the
old check "not at sea level" never saw them. Now no hut goes where any of the hut's five ground points lies in one of
these biomes; the build then stands without a hut (or not at all, for the mijningang-hutje).

On its own: PYTHONPATH=tools python tools/features/ingang_hut.py from the project root.
"""
import json
import os

# the wet biomes of the Guhmension: its seas, the lake, the river valley, the beaver brook, the swamp, the polder ditches
GEEN_HUT = ["guhs:guh_sea", "guhs:diepe_guhzee", "guhs:bloesemmeertje", "guhs:klaterdal", "guhs:beverbeek",
            "guhs:kaasmoeras", "guhs:guhpolder", "guhs:guhwaii"]


def build(h):
    h.w(f"{h.D}/tags/worldgen/biome/onderwol/geen_ingang_hut.json", {"replace": False, "values": GEEN_HUT})


def _direct():
    import types
    D = os.path.join("src", "main", "resources", "data", "guhs")

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")
    build(types.SimpleNamespace(D=D, w=w))
    print(f"ingang_hut: {len(GEEN_HUT)} biomes without entrance huts")


if __name__ == "__main__":
    _direct()
