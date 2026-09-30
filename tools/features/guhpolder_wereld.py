"""
De Guhpolder (2.9) - the worldgen: the noise, the biome and its features.

Like the Knuffeldal (knuffeldal_wereld.py), the polder comes from one low noise of its own, guhs:guhmension_polder
(firstOctave -10: ~1000 blocks per bump, twice as wide as the Knuffeldal's, so a polder is a big open plain), used in
three places:

  - the biome: the polder "term" t (0 outside, 1 inside: a spline of the noise from TERM_FROM to TERM_TO, times 0 near a
    deep sea (the sea noise) and near a Knuffeldal (the knuffel noise)) pushes the multi-noise "depth" down by DEPTH_SHIFT * t
    and the weirdness DOWN by WEIRD_SHIFT * t. At the surface the Guhmension has depth -1 and weirdness 0 and every
    surface biome has depth [-1, 1] and weirdness 0; guhs:guhpolder has depth DEPTH_RANGE and weirdness WEIRD_RANGE
    (the Knuffeldal uses weirdness >= 1.5, the Diepe Guhzee <= -1.5: the polder sits between, [-1.4, -1.0]). Where t = 0
    nothing changes at all (the term multiplies everything), so no other biome moves outside the polder
    (GuhpolderGameTests checks every sample).
  - the terrain (noise router, final_density): towards the middle (FLAT_FROM .. FLAT_TO) the terrain blends into a flat
    plain at FLAT_Y with ripples of at most a block (the noise guhs:guhmension_polder_golfjes). It wraps the Knuffeldal's
    blend (which wraps the sea's), the same way.
  - the surface: rijpgras on the pink guh soil.

The Elf-Guhjestocht (the elftocht feature) sits on the peak of the polder noise: see PEAK_MIN / FLAT_TO / TOCHT_RING.
"""
import json

from features import knuffeldal_wereld as kw

NOISE = "guhmension_polder"
RIPPLES = "guhmension_polder_golfjes"
BIOME = "guhpolder"

TERM_FROM, TERM_TO = 0.61, 0.63          # the biome term 0 -> 1
FLAT_FROM, FLAT_TO = 0.59, 0.63          # the terrain: its own hills at FLAT_FROM, flat from FLAT_TO on (the biome is flat right to its edge)
DEPTH_SHIFT = 1.0                        # the multi-noise depth goes down this much in the polder (the surface: -1 -> -2)
WEIRD_SHIFT = 1.2                        # ... and the weirdness DOWN this much (the surface: 0 -> -1.2)
DEPTH_RANGE = (-2.0, -1.95)
WEIRD_RANGE = (-1.4, -1.0)
FLAT_Y = 68                              # the plain's top block (a polder lies a bit low, VAHOEG)
KNUFFEL_OFF = (0.40, 0.46)               # never next to a Knuffeldal: the term fades out when the knuffel noise gets this high

# for the Elf-Guhjestocht (elftocht slice): a polder peak high enough that a ~256 x 256 tour fits on the flat part
PEAK_MIN = 0.66                          # the peak of the polder noise must be at least this high ...
TOCHT_RING = 0.63                        # ... with the noise at least this (= FLAT_TO: dead flat) on a ring of ...
TOCHT_RADIUS = 136                       # ... this radius around it


def geen_knuffel():
    return kw.spline(kw.NOISE, [(KNUFFEL_OFF[0], 1.0), (KNUFFEL_OFF[1], 0.0)])


def buiten():
    """1 away from the deep seas and the Knuffeldal, 0 near them."""
    return kw.mul(kw.geen_zee(), geen_knuffel())


def term():
    return kw.mul(kw.spline(NOISE, [(TERM_FROM, 0.0), (TERM_TO, 1.0)]), buiten())


def blend():
    return kw.mul(kw.spline(NOISE, [(FLAT_FROM, 0.0), (FLAT_TO, 1.0)]), buiten())


def router(d):
    r = d["noise_router"]
    final = r["final_density"]
    assert final["type"] == "minecraft:max" and final["argument1"]["type"] == "minecraft:interpolated", "unexpected final_density"
    inner = final["argument1"]["argument"]
    bed = kw.add(kw.gradient(0, 256, 0.025 * (FLAT_Y + 0.5), 0.025 * (FLAT_Y + 0.5 - 256)), kw.mul(kw.noise_ref(RIPPLES), 0.02))
    b = blend()
    final["argument1"] = {"type": "minecraft:interpolated", "argument": kw.add(kw.mul(inner, kw.add(kw.mul(b, -1.0), 1.0)), kw.mul(bed, b))}
    r["depth"] = kw.add(r["depth"], kw.mul(term(), -DEPTH_SHIFT))
    r["ridges"] = kw.add(r["ridges"], kw.mul(term(), -WEIRD_SHIFT))


def surface(d):
    """Rijpgras on top (the pink guh soil below, the Guhmension's default)."""
    rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]},
            "then_run": {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0,
                         "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"},
                         "then_run": {"type": "minecraft:block", "result_state": {"Name": "guhs:rijpgras", "Properties": {"snowy": "false"}}}}}
    rules = d["surface_rule"]["sequence"]
    d["surface_rule"]["sequence"] = [x for x in rules if x.get("if_true", {}).get("biome_is") != [f"guhs:{BIOME}"]] + [rule]


def patch_router(h, path):
    """Idempotent: the router is only wrapped when this generator hasn't wrapped it yet (the noise settings file is
    written fresh by make_resources/make_v2 before the features run, so normally it is always fresh)."""
    d = json.load(open(path, encoding="utf-8"))
    if f"guhs:{NOISE}" in json.dumps(d["noise_router"]):
        return
    router(d)
    surface(d)
    h.w(path, d)


def plant_patch(block, tries, spread, props=None):
    p = kw.plant_patch(block, tries, spread, props)
    for pred in p["config"]["feature"]["placement"][0]["predicate"]["predicates"]:
        if pred.get("offset") == [0, -1, 0]:
            pred["blocks"] = ["guhs:rijpgras"]
    return p


def placed(feature, *mods):
    return {"feature": feature, "placement": [*mods, {"type": "minecraft:in_square"},
                                              {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]}


def rarity(n):
    return {"type": "minecraft:rarity_filter", "chance": n}


def count(n):
    return {"type": "minecraft:count", "count": n}


def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    w(f"{wg}/noise/{NOISE}.json", {"firstOctave": -10, "amplitudes": [1.0]})
    w(f"{wg}/noise/{RIPPLES}.json", {"firstOctave": -5, "amplitudes": [1.0, 0.5]})
    patch_router(h, f"{wg}/noise_settings/guhmension.json")

    def biome_source(d):
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] != f"guhs:{BIOME}"]
        anything = [-2.0, 2.0]
        entries.append({"biome": f"guhs:{BIOME}", "parameters": {
            "temperature": anything, "humidity": anything, "continentalness": anything, "erosion": anything,
            "weirdness": list(WEIRD_RANGE), "depth": list(DEPTH_RANGE), "offset": 0.0}})
        d["generator"]["biome_source"]["biomes"] = entries
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    # --- the features (Java: GuhpolderWorldgen, + plant patches) ---
    for name in ("guhpolder_knotwilg", "guhpolder_sloot", "guhpolder_sneeuwguhheuvel", "guhpolder_molentje", "guhpolder_sneeuwplek"):
        w(f"{wg}/configured_feature/{name}.json", {"type": f"guhs:{name}", "config": {}})
    w(f"{wg}/placed_feature/guhpolder_sloot.json", placed("guhs:guhpolder_sloot", rarity(2)))
    w(f"{wg}/placed_feature/guhpolder_knotwilg.json", placed("guhs:guhpolder_knotwilg", rarity(3)))
    w(f"{wg}/placed_feature/guhpolder_sneeuwguhheuvel.json", placed("guhs:guhpolder_sneeuwguhheuvel", rarity(10)))
    w(f"{wg}/placed_feature/guhpolder_molentje.json", placed("guhs:guhpolder_molentje", rarity(24)))
    w(f"{wg}/placed_feature/guhpolder_sneeuwplek.json", placed("guhs:guhpolder_sneeuwplek", rarity(2)))
    # frozen little lakes (vennetjes) of polderijs, flush with the ground
    w(f"{wg}/configured_feature/guhpolder_vennetje.json", {"type": "minecraft:disk", "config": {
        "state_provider": {"fallback": {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:polderijs"}}, "rules": []},
        "target": {"type": "minecraft:matching_blocks", "blocks": ["guhs:rijpgras"]},
        "radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 5}, "half_height": 1}})
    w(f"{wg}/placed_feature/guhpolder_vennetje.json", placed("guhs:guhpolder_vennetje", rarity(6)))
    patches = [("guhpolder_rijpsprietjes", plant_patch("guhs:rijpsprietjes", 40, 6), [count(3)]),
               ("guhpolder_ijsbloempjes", plant_patch("guhs:guh_ijsbloempje", 12, 4), [rarity(2)]),
               ("guhpolder_ijspegels", plant_patch("guhs:ijspegelguh_kristal", 5, 3, {"facing": "up", "waterlogged": "false"}), [rarity(7)])]
    for name, cf, mods in patches:
        w(f"{wg}/configured_feature/{name}.json", cf)
        w(f"{wg}/placed_feature/{name}.json", placed(f"guhs:{name}", *mods))

    # --- the biome: a cold white plain under a pale sky, snow falling, glitter in the air ---
    base = json.load(open(f"{wg}/biome/pink_puffs.json", encoding="utf-8"))
    ores = base["features"][6]
    biome = {
        "has_precipitation": True, "temperature": -0.3, "downfall": 0.5, "creature_spawn_probability": 0.3,
        "effects": {"sky_color": 0xB9D6F2, "fog_color": 0xE6F2FB, "water_color": 0x86BDE6, "water_fog_color": 0x4F84B5,
                    "grass_color": 0xCDE3DE, "foliage_color": 0xA07E6A,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "minecraft:music.overworld.snowy_slopes", "min_delay": 12000, "max_delay": 24000,
                              "replace_current_music": False},
                    "particle": {"options": {"type": "guhs:guhpolder_glinster"}, "probability": 0.004}},
        "spawners": {**{k: [] for k in base["spawners"]},
                     "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 3, "maxCount": 5}]},
        "spawn_costs": {}, "carvers": {"air": []},
        "features": [[], [], ["guhs:guhpolder_sloot", "guhs:guhpolder_vennetje"], [], [], [], ores, [], [],
                     ["guhs:guhpolder_knotwilg", "guhs:guhpolder_sneeuwguhheuvel", "guhs:guhpolder_molentje", "guhs:guhpolder_sneeuwplek",
                      "guhs:guhpolder_rijpsprietjes", "guhs:guhpolder_ijsbloempjes", "guhs:guhpolder_ijspegels"], []]}
    w(f"{wg}/biome/{BIOME}.json", biome)
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({f"guhs:{BIOME}": {"villager_type": "minecraft:snow"}}))
    # the Knabbelkelders (the way to the Guheinde) may also lie under the polder, like under the Knuffeldal
    h.add_tag("guhs/tags/worldgen/biome/has_structure/knabbelkelder", [f"guhs:{BIOME}"])
