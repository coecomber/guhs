"""
Het Knuffeldal (2.8) - the worldgen: the noise, the biome, its plants and trees, and the town's structure JSON.

The Knuffeldal comes from one low noise, guhs:guhmension_knuffel (~500 blocks per bump, like the sea noise), in four
places at once (tools/features/diepzee.py does the same for the Diepe Guhzee):

  - the biome: the knuffel "term" t (0 outside, 1 inside: a spline of the noise from TERM_FROM to TERM_TO, times 0 near
    a deep sea: the sea noise from SEA_OFF) pushes the multi-noise "depth" down by DEPTH_SHIFT * t and the weirdness up
    by WEIRD_SHIFT * t. At the surface (above y ~62) the Guhmension has depth -1 and weirdness 0, and every surface biome
    has depth [-1, 1] and weirdness 0; guhs:knuffeldal has depth DEPTH_RANGE, weirdness WEIRD_RANGE and every other
    parameter free. Outside (t = 0) it is ~1.8 away from everything, further than any gap between the other biomes (so
    no stray bits of Knuffeldal); while t goes up all other surface biomes get equally far away, and from t ~0.4 on the
    Knuffeldal is nearer. The Diepe Guhzee needs weirdness -2 (and t is 0 near it). Deep under the Knuffeldal the
    cheese caves may come up a bit more than elsewhere (their weirdness), nothing else changes.
  - the terrain (noise router, final_density): towards the middle (FLAT_FROM .. FLAT_TO) the terrain blends into a
    flat meadow at FLAT_Y with soft hills of +-2 blocks (the noise guhs:guhmension_knuffel_heuvels). The town (the
    structure knuffeldal_stadje, KnuffeldalStadjeStructure.java) stands on the peak of the noise, where it is all flat.
  - the town: on the highest point of every Knuffeldal (at least TOWN_MIN), with the noise at least FLAT_TO on a ring
    of TOWN_FLAT_RADIUS around it.
  - the surface: knuffelgras (soft pink fluffy grass) on pink wool.

build(h) writes it all; the numbers are tuned with the game test KnuffeldalGameTests.knuffeldalDeelEnStadjes.
"""
import json
import os
import zipfile

NOISE = "guhmension_knuffel"
HILLS = "guhmension_knuffel_heuvels"
SEA_NOISE = "guhmension_zee"
BIOME = "knuffeldal"
STADJE = "knuffeldal_stadje"

# the knuffel noise (the same kind as the sea noise: std ~0.3 on the scale of ~500 blocks)
TERM_FROM, TERM_TO = 0.51, 0.53          # the biome term 0 -> 1 (the biome wins from about 40% of the way)
FLAT_FROM, FLAT_TO = 0.51, 0.56          # the terrain: its own hills at FLAT_FROM, flat at FLAT_TO
SEA_OFF = (0.28, 0.30)                   # never next to a deep sea: the term fades out when the sea noise gets this high
DEPTH_SHIFT = 1.0                        # the multi-noise depth goes down this much in the Knuffeldal (the surface: -1 -> -2)
WEIRD_SHIFT = 1.5                        # ... and the weirdness up this much (the surface: 0 -> 1.5)
DEPTH_RANGE = (-2.0, -1.95)              # the biome's depth and weirdness ranges (parameters must stay in [-2, 2])
WEIRD_RANGE = (1.5, 2.0)
FLAT_Y = 70                              # the meadow's top block (the Guhmension's own baseline is ~69)

# the town (KnuffeldalStadjeStructure)
CELL_CHUNKS = 4                          # a cell = the structure set's spacing (4 x 4 chunks)
NEIGHBOURHOOD = 12                       # cells around: the highest peak of a dal within 12 cells (768 blocks) wins
TOWN_MIN = 0.585                         # the peak must be at least this high
TOWN_FLAT_RADIUS = 40                    # ... with the noise at least TOWN_RING on this ring around it (almost flat there;
TOWN_RING = 0.54                         # the town's beard fills in the rest)
TOWN_DAL = 0.47                          # two peaks are in the same dal when the noise stays at least this high on the
                                         # straight line between them (a bit under the biome's edge: a bent dal counts once)
TOWN_SALT = 20280101
TOWN_REACH = 80                          # jigsaw max distance from the centre


def vanilla_json(path):
    """A JSON file from the vanilla game (the resources jar in build/moddev)."""
    jar = os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")
    with zipfile.ZipFile(jar) as z:
        return json.loads(z.read(path))


def noise_ref(name):
    return {"type": "minecraft:noise", "noise": f"guhs:{name}", "xz_scale": 1.0, "y_scale": 0.0}


def spline(noise, points):
    return {"type": "minecraft:spline", "spline": {"coordinate": noise_ref(noise), "points": [
        {"location": loc, "value": val, "derivative": 0.0} for loc, val in points]}}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def gradient(from_y, to_y, from_value, to_value):
    return {"type": "minecraft:y_clamped_gradient", "from_y": from_y, "to_y": to_y, "from_value": from_value, "to_value": to_value}


def geen_zee():
    """1 away from the deep seas, 0 where the sea noise reaches SEA_OFF[1]."""
    return spline(SEA_NOISE, [(SEA_OFF[0], 1.0), (SEA_OFF[1], 0.0)])


def term():
    """0 outside the Knuffeldal, 1 inside (biome)."""
    return mul(spline(NOISE, [(TERM_FROM, 0.0), (TERM_TO, 1.0)]), geen_zee())


def blend():
    """0 = the Guhmension's own terrain, 1 = the Knuffeldal's flat meadow."""
    return mul(spline(NOISE, [(FLAT_FROM, 0.0), (FLAT_TO, 1.0)]), geen_zee())


def router(d):
    """The terrain (flattened towards the middle) and the biome (depth) of the Knuffeldal."""
    r = d["noise_router"]
    final = r["final_density"]
    assert final["type"] == "minecraft:max" and final["argument1"]["type"] == "minecraft:interpolated", "unexpected final_density"
    terrain = final["argument1"]["argument"]
    # the meadow: flat at FLAT_Y (density 0.025 per block, like the sea floor), soft hills +-2 blocks
    bed = add(gradient(0, 256, 0.025 * (FLAT_Y + 0.5), 0.025 * (FLAT_Y + 0.5 - 256)), mul(noise_ref(HILLS), 0.05))
    b = blend()
    # terrain * (1 - b) + bed * b (the terrain only once: it's the expensive part)
    final["argument1"] = {"type": "minecraft:interpolated", "argument": add(mul(terrain, add(mul(b, -1.0), 1.0)), mul(bed, b))}
    r["depth"] = add(r["depth"], mul(term(), -DEPTH_SHIFT))
    r["ridges"] = add(r["ridges"], mul(term(), WEIRD_SHIFT))


def surface(d):
    """Knuffelgras on top (pink wool below, the Guhmension's default)."""
    rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]},
            "then_run": {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0,
                         "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"},
                         "then_run": {"type": "minecraft:block", "result_state": {"Name": "guhs:knuffelgras"}}}}
    rules = d["surface_rule"]["sequence"]
    d["surface_rule"]["sequence"] = [x for x in rules if x.get("if_true", {}).get("biome_is") != [f"guhs:{BIOME}"]] + [rule]


def plant_patch(block, tries, spread, props=None):
    state = {"Name": block, **({"Properties": props} if props else {})}
    return {"type": "minecraft:random_patch", "config": {"tries": tries, "xz_spread": spread, "y_spread": 2, "feature": {
        "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": state}}},
        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
            {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": ["guhs:knuffelgras"]}]}}]}}}


def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    h.w(f"{wg}/noise/{NOISE}.json", {"firstOctave": -9, "amplitudes": [1.0]})   # (one octave: round dalen, no crumbs around them)
    h.w(f"{wg}/noise/{HILLS}.json", {"firstOctave": -5, "amplitudes": [1.0, 0.5]})
    h.patch_json(f"{wg}/noise_settings/guhmension.json", lambda d: (router(d), surface(d)))

    def biome_source(d):
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] != f"guhs:{BIOME}"]
        anything = [-2.0, 2.0]
        entries.append({"biome": f"guhs:{BIOME}", "parameters": {
            "temperature": anything, "humidity": anything, "continentalness": anything, "erosion": anything,
            "weirdness": list(WEIRD_RANGE), "depth": list(DEPTH_RANGE), "offset": 0.0}})
        d["generator"]["biome_source"]["biomes"] = entries
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    # --- trees, mushrooms, fluff ---
    cherry = vanilla_json("data/minecraft/worldgen/configured_feature/cherry.json")
    tree = json.loads(json.dumps(cherry).replace("minecraft:cherry_leaves", "guhs:pluizenboom_bladeren")
                      .replace("minecraft:cherry_log", "guhs:pluizenboom_stam"))
    tree["config"]["dirt_provider"] = {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:knuffelgras"}}
    w(f"{wg}/configured_feature/pluizenboom.json", tree)
    w(f"{wg}/placed_feature/pluizenboom.json", {"feature": "guhs:pluizenboom", "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [
            {"data": 0, "weight": 3}, {"data": 1, "weight": 2}, {"data": 2, "weight": 1}]}},
        {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                     "state": {"Name": "guhs:pluizenboom_zaailing", "Properties": {"stage": "0"}}}},
        {"type": "minecraft:biome"}]})
    cap = {"Name": "guhs:guhpaddenstoel_hoed", "Properties": {d: ("false" if d == "down" else "true") for d in
                                                             ("down", "east", "north", "south", "up", "west")}}
    stem = {"Name": "guhs:guhpaddenstoel_steel", "Properties": {d: ("false" if d in ("down", "up") else "true") for d in
                                                               ("down", "east", "north", "south", "up", "west")}}
    w(f"{wg}/configured_feature/reuze_guhpaddenstoel.json", {"type": "minecraft:huge_red_mushroom", "config": {
        "cap_provider": {"type": "minecraft:simple_state_provider", "state": cap},
        "stem_provider": {"type": "minecraft:simple_state_provider", "state": stem}, "foliage_radius": 2}})
    w(f"{wg}/placed_feature/reuze_guhpaddenstoel.json", {"feature": "guhs:reuze_guhpaddenstoel", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 9}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    for name, block, tries, spread, count in [("pluisgras", "guhs:pluisgras", 40, 6, 4),
                                               ("guhpaddenstoeltjes", "guhs:guhpaddenstoel", 10, 4, 1),
                                               ("knuffeldal_bloemen", None, 24, 5, 2),
                                               ("knuffeldal_bladerhoopjes", "guhs:bladerhoopje", 4, 3, 1)]:
        if block:
            cf = plant_patch(block, tries, spread)
        else:
            cf = plant_patch("guhs:roze_guhbloem", tries, spread)
            cf["config"]["feature"]["feature"]["config"]["to_place"] = {"type": "minecraft:weighted_state_provider", "entries": [
                {"weight": 3, "data": {"Name": "guhs:roze_guhbloem"}}, {"weight": 2, "data": {"Name": "guhs:knabbelroos"}},
                {"weight": 2, "data": {"Name": "guhs:guhoortjes"}}, {"weight": 1, "data": {"Name": "minecraft:pink_tulip"}},
                {"weight": 1, "data": {"Name": "minecraft:allium"}}]}
        w(f"{wg}/configured_feature/{name}.json", cf)
        placement = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
        if count > 1:
            placement.insert(0, {"type": "minecraft:count", "count": count})
        if name == "knuffeldal_bladerhoopjes":
            placement.insert(0, {"type": "minecraft:rarity_filter", "chance": 6})
        if name == "guhpaddenstoeltjes":
            placement.insert(0, {"type": "minecraft:rarity_filter", "chance": 3})
        w(f"{wg}/placed_feature/{name}.json", {"feature": f"guhs:{name}", "placement": placement})

    # --- the biome: pink fluffy meadows, cherry-grove music, drifting pluisjes ---
    base = json.load(open(f"{wg}/biome/pink_puffs.json", encoding="utf-8"))
    ores = base["features"][6]
    biome = {
        "has_precipitation": False, "temperature": 0.7, "downfall": 0.4, "creature_spawn_probability": 0.35,
        "effects": {"sky_color": 0xF7B6D8, "fog_color": 0xFFE4F1, "water_color": 0xF79ACB, "water_fog_color": 0xD76AA6,
                    "grass_color": 0xF7A8CC, "foliage_color": 0xF9B8D6,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "minecraft:music.overworld.cherry_grove", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False},
                    "particle": {"options": {"type": "guhs:pluisje"}, "probability": 0.006}},
        "spawners": {**{k: [] for k in base["spawners"]},
                     "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 3, "maxCount": 5},
                                  {"type": "guhs:guh_bee", "weight": 12, "minCount": 1, "maxCount": 2}]},
        "spawn_costs": {}, "carvers": {"air": []},
        "features": [[], [], [], [], [], [], ores, [], [], ["guhs:pluizenboom", "guhs:reuze_guhpaddenstoel", "guhs:pluisgras",
                                                            "guhs:guhpaddenstoeltjes", "guhs:knuffeldal_bloemen", "guhs:knuffeldal_bladerhoopjes"], []]}
    w(f"{wg}/biome/{BIOME}.json", biome)
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({f"guhs:{BIOME}": {"villager_type": "guhs:guh"}}))


def structure_json(h, reach_hint):
    """The town: guhs:knuffeldal_stadje (its own type), the set and the biome tag; keep_clear comes from bouwruimte()."""
    D, w = h.D, h.w
    none = {"bounding_box": "full", "spawns": []}
    w(f"{D}/worldgen/structure/{STADJE}.json", {
        "type": "guhs:knuffeldal_stadje", "biomes": f"#guhs:has_structure/{STADJE}", "step": "surface_structures",
        "spawn_overrides": {"monster": none, "ambient": none}, "terrain_adaptation": "beard_box",
        "start_pool": f"guhs:{STADJE}/start", "start_jigsaw_name": "guhs:knuffeldal_midden",
        "knuffel_noise": f"guhs:{NOISE}", "cell_chunks": CELL_CHUNKS, "neighbourhood": NEIGHBOURHOOD,
        "min_value": TOWN_MIN, "dal_value": TOWN_DAL, "flat_value": TOWN_RING, "flat_radius": TOWN_FLAT_RADIUS,
        "size": 2, "max_distance_from_center": TOWN_REACH, "keep_clear": reach_hint, "voorrang": 800})
    w(f"{D}/worldgen/structure_set/{STADJE}.json", {"structures": [{"structure": f"guhs:{STADJE}", "weight": 1}],
                                                    "placement": {"type": "minecraft:random_spread", "spacing": CELL_CHUNKS,
                                                                  "separation": 1, "salt": TOWN_SALT}})
    w(f"{D}/tags/worldgen/biome/has_structure/{STADJE}.json", {"values": [f"guhs:{BIOME}"]})


def build(h):
    worldgen(h)
