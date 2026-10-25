"""
1.7.0 "Onder de Wol", ontwerpronde 22 ("behouden + aanpassen"): the underground structures from before the stone layers
fit in the new underground of the Guhmension.

  - their template pools use the processor list guhs:onderwol_oude_bouw (Java OudeBouwLaagProcessor): imitation rock
    (stone, dirt, deepslate, gatenkaas...: tag onderwol/oude_bouw_grond) always becomes the layer stone of its depth, wall
    and fill blocks (pink wool, concrete, blackstone: tag onderwol/oude_bouw_vulling) only where the rooms don't show them,
    and a mantle of layer stone closes them off from the new halls and caves; not in the biomes without layers
    (biome tag onderwol/zonder_lagen: the Gatenkaasgrotten);
  - the ones that lie at the depth of the new caves come a bit less often (spacing and separation +25%).
The knabbelmoria (Barbecuether, no layers there) and the guaranteed copies are left alone. Only new chunks: saved pieces
keep the processor list they were made with.

All patches are idempotent (fixed values, not multiplied). On its own: python tools/features/onderwol_oud.py from the
project root.
"""
import json
import os

PROCESSOR = "guhs:onderwol_oude_bouw"
STRUCTUREN = ("guh_caves", "challenging_guh_caves", "kaasmijn", "gatenkaas_mijnschacht", "stille_voorraadkelder")
# structure set -> (spacing, separation): the old values x 1.25 (old: 10/4, 24/9, 14/5, 32/12). The kaasmijn (90/30, plus a
# guaranteed copy) is rare enough already.
SPREIDING = {"guh_caves": (13, 5), "challenging_guh_caves": (30, 11), "gatenkaas_mijnschacht": (18, 6),
             "stille_voorraadkelder": (40, 15)}
GROND = ["minecraft:stone", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:deepslate", "minecraft:andesite",
         "minecraft:diorite", "minecraft:granite", "minecraft:tuff", "minecraft:gravel", "guhs:gatenkaas"]
VULLING = ["minecraft:pink_wool", "minecraft:magenta_wool", "minecraft:pink_concrete", "minecraft:white_concrete",
           "minecraft:blackstone", "minecraft:black_concrete"]
ZONDER_LAGEN = ["guhs:gatenkaasgrotten"]   # (the surface rule guhs:onderwol_lagen skips this biome too)


def build(h):
    D = h.D
    h.w(f"{D}/worldgen/processor_list/onderwol_oude_bouw.json", {"processors": [{"processor_type": PROCESSOR}]})
    h.w(f"{D}/tags/block/onderwol/oude_bouw_grond.json", {"replace": False, "values": GROND})
    h.w(f"{D}/tags/block/onderwol/oude_bouw_vulling.json", {"replace": False, "values": VULLING})
    h.w(f"{D}/tags/worldgen/biome/onderwol/zonder_lagen.json", {"replace": False, "values": ZONDER_LAGEN})
    for naam in STRUCTUREN:
        map_ = f"{D}/worldgen/template_pool/{naam}"
        for f in sorted(os.listdir(map_)):
            if f.endswith(".json"):
                h.patch_json(f"{map_}/{f}", _pool)
    for naam, (spacing, separation) in SPREIDING.items():
        h.patch_json(f"{D}/worldgen/structure_set/{naam}.json",
                     lambda d, s=spacing, p=separation: d["placement"].update({"spacing": s, "separation": p}))


def _pool(d):
    for e in d["elements"]:
        if "processors" in e["element"]:
            e["element"]["processors"] = PROCESSOR


def _direct():
    import types
    D = os.path.join("src", "main", "resources", "data", "guhs")

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def patch_json(path, fn):
        data = json.load(open(path, encoding="utf-8"))
        fn(data)
        w(path, data)
    build(types.SimpleNamespace(D=D, w=w, patch_json=patch_json))
    print(f"onderwol_oud: {len(STRUCTUREN)} structures on {PROCESSOR}, {len(SPREIDING)} spreads")


if __name__ == "__main__":
    _direct()
