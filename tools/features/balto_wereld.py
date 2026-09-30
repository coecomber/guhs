"""
Baltoguh en Nomguh (3.0 Guhverhalen, slice balto) - the Sneeuwguhtoendra's content (the biome itself, its noise and terrain
are the fundament's: verhaal_wereld.py; balto owns the biome's features from step 2 on and its surface rule, CONTRACT_30 §4.8):

  surface   white rolling hills: snowy guh grass with big patches of plain snow, rijpgras (the Guhpolder's frosty grass) in
            the hollows, dirt under it
  features  step 9 (vegetal decoration): sneeuwguhspar groves (the spruce-like guh tree with a sleepy face, now and then a
            big one), soft snow drifts piled up by the wind, snowy boulders, frosty sprigs and guh ice flowers, and here and
            there a little snow hill with a sneeuwpopguh (the Guhpolder's feature); step 10: vanilla's snow layer + ice
  trees     the configured feature guhs:balto_sneeuwguhspar (Java BaltoWorldgen.Sneeuwguhspar) is also what the sapling grows
"""
import json

from features import knuffeldal_wereld as kw
from features import verhaal_wereld as vw

BIOME = vw.TOENDRA
PLAATSEN = ["balto_sneeuwguhspar", "balto_sneeuwguhspar_groot", "balto_sneeuwduin", "balto_sneeuwkei", "balto_rijpsprietjes",
            "balto_ijsbloempjes"]


def placed(feature, *mods):
    return {"feature": feature, "placement": [*mods, {"type": "minecraft:in_square"},
                                              {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]}


def plant_patch(block, tries, spread):
    """A random patch of a plant on snowy guh grass or rijpgras."""
    p = kw.plant_patch(block, tries, spread)
    for pred in p["config"]["feature"]["placement"][0]["predicate"]["predicates"]:
        if pred.get("offset") == [0, -1, 0]:
            pred["blocks"] = ["minecraft:grass_block", "guhs:rijpgras"]
    return p


def features(h):
    wg = f"{h.D}/worldgen"
    w = h.w
    for name in ("balto_sneeuwguhspar", "balto_sneeuwduin", "balto_sneeuwkei"):
        w(f"{wg}/configured_feature/{name}.json", {"type": f"guhs:{name}", "config": {}})
    # groves: most chunks a few trees, some none, some a little wood
    w(f"{wg}/placed_feature/balto_sneeuwguhspar.json", placed("guhs:balto_sneeuwguhspar", {"type": "minecraft:count", "count": {
        "type": "minecraft:weighted_list", "distribution": [{"weight": 4, "data": 0}, {"weight": 4, "data": 1}, {"weight": 3, "data": 2},
                                                            {"weight": 2, "data": 4}, {"weight": 1, "data": 7}]}}))
    w(f"{wg}/placed_feature/balto_sneeuwguhspar_groot.json", placed("guhs:balto_sneeuwguhspar", {"type": "minecraft:rarity_filter", "chance": 5}))
    w(f"{wg}/placed_feature/balto_sneeuwduin.json", placed("guhs:balto_sneeuwduin", {"type": "minecraft:rarity_filter", "chance": 2}))
    w(f"{wg}/placed_feature/balto_sneeuwkei.json", placed("guhs:balto_sneeuwkei", {"type": "minecraft:rarity_filter", "chance": 7}))
    w(f"{wg}/configured_feature/balto_rijpsprietjes.json", plant_patch("guhs:rijpsprietjes", 24, 5))
    w(f"{wg}/placed_feature/balto_rijpsprietjes.json", placed("guhs:balto_rijpsprietjes", {"type": "minecraft:rarity_filter", "chance": 2}))
    w(f"{wg}/configured_feature/balto_ijsbloempjes.json", plant_patch("guhs:guh_ijsbloempje", 10, 4))
    w(f"{wg}/placed_feature/balto_ijsbloempjes.json", placed("guhs:balto_ijsbloempjes", {"type": "minecraft:rarity_filter", "chance": 6}))

    def biome(d):
        steps = d["features"]
        for i in range(2, len(steps)):
            steps[i] = [f for f in steps[i] if not f.startswith("guhs:balto_") and f not in ("minecraft:trees_snowy", "minecraft:patch_grass_plain")]
        steps[9] = steps[9] + [f"guhs:{n}" for n in PLAATSEN] + ["guhs:guhpolder_sneeuwguhheuvel"]
        steps[9] = list(dict.fromkeys(steps[9]))
        if "minecraft:freeze_top_layer" not in steps[10]:
            steps[10].append("minecraft:freeze_top_layer")
    h.patch_json(f"{wg}/biome/{BIOME}.json", biome)


def surface(h):
    """Replaces the fundament's placeholder rule of the tundra in place (the entry with biome_is [guhs:sneeuwguhtoendra])."""
    def block(name, props=None):
        return {"type": "minecraft:block", "result_state": {"Name": name, **({"Properties": props} if props else {})}}

    def depth(offset):
        return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}

    def ruis(noise, lo):
        return {"type": "minecraft:noise_threshold", "noise": noise, "min_threshold": lo, "max_threshold": 10.0}

    top = {"type": "minecraft:sequence", "sequence": [
        {"type": "minecraft:condition", "if_true": ruis("guhs:guhmension_patches", 0.35), "then_run": block("minecraft:snow_block")},
        {"type": "minecraft:condition", "if_true": ruis("minecraft:surface", 0.25), "then_run": block("guhs:rijpgras", {"snowy": "true"})},
        block("minecraft:grass_block", {"snowy": "true"})]}
    rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]},
            "then_run": {"type": "minecraft:sequence", "sequence": [
                {"type": "minecraft:condition", "if_true": depth(0), "then_run": top},
                {"type": "minecraft:condition", "if_true": depth(3), "then_run": block("minecraft:dirt")}]}}

    def patch(d):
        seq = d["surface_rule"]["sequence"]
        for i, r in enumerate(seq):
            if r.get("if_true", {}).get("biome_is") == [f"guhs:{BIOME}"]:
                seq[i] = rule
                return
        raise SystemExit("balto: the fundament's tundra surface rule is missing")
    h.patch_json(f"{h.D}/worldgen/noise_settings/guhmension.json", patch)


def build(h):
    features(h)
    surface(h)
