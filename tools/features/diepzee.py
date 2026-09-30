"""
De Diepe Guhzee (2.7): the Guhmension's big, deep sea, with in the middle of every sea one Guhbubbel (onderwater.py).

The Guhmension has no sea level (sea_level 0, no fluid): its valleys go down to y ~30 and must stay dry (a sea level
of 63 would flood some 20% of the land). So the deep sea is made by one low noise, guhs:guhmension_zee (~500 blocks per
bump; 8 of every 100 columns or so are sea), in four places at once:

  - the terrain (noise router, final_density): where the noise is above DEEP_FROM the terrain is lowered, smoothly, to a
    flat sea floor at FLOOR_Y (y 34, with low dunes); from DEEP_TO on it is all sea floor. Around the sea, on the band
    DAM of the noise, a dam: solid up to one block above the water, sloping down DAM_DEPTH blocks on both sides (so
    valleys that run into the sea are closed off there);
  - the water: the feature guhs:diepzee_water (DiepzeeWaterFeature.java) fills every column with the noise above
    WATER_FROM (inside the dam) with water below WATER_LEVEL: the surface is at y 62. Next to such a column is always
    another sea column or the dam, so the sea can't leak;
  - the biome guhs:diepe_guhzee (weirdness: 0 on the surface everywhere else, at least -0.5 under the ground; the sea
    adds -2): from BIOME_FROM/BIOME_TO on, so the beaches on the dam belong to it too. Its sea floor is sand with pink terracotta and clay, the beach sand.
    Kaaskoraal, corals, seagrass, kelp and sea pickles grow on the bottom; guh fish swim in it, Zeemeerguhs turn up
    (Zeemeerguh.java, biome tag guhs:guhzeeen) and guhs walk the beaches;
  - the Guhbubbel (onderwater.py writes its structure with the numbers here, GuhbubbelStructure.java): on the highest
    point of the noise of every sea (at least PEAK_MIN), if the floor under the whole template is deep enough.

The gatenkaas caves' "deep" (the rock far under the surface) is made again from the new terrain (gatenkaas.router_deep),
and under the sea the weirdness is too low for them: no cheese holes under the water, no crystal mines in it.
Also: the advancement find_diepe_guhzee, an FTB quest (row y = 78) and the texts.

build(h) makes everything (h = make_v2); ftb(fq) adds the quest.
"""
import json

from features import gatenkaas, onderwater as ow

BIOME = ow.SEA_BIOME                  # "diepe_guhzee"
SEA_NOISE = "guhmension_zee"          # the sea noise (the higher, the deeper)
BED_NOISE = "guhmension_zeebodem"     # the dunes on the sea floor
WATER_LEVEL = 63                      # water below this height: the top water block is at y = 62
FLOOR_Y = WATER_LEVEL - 1 - (ow.G - ow.F)   # 34: the flat sea floor, as deep as the Guhbubbel's floor

# the sea noise (values: std ~0.3). 1.0.0: every threshold of the sea is ZEE_KRIMP higher than in 2.7-3.0 (0.325 -> 0.395
# for the biome...): the same seas, only fewer and a bit smaller (the Diepe Guhzee's share ~12% -> ~7.5% of the Guhmension;
# every sea keeps its dam, its basin and its Guhbubbel). Keep in sync: RegioPlek.BUBBEL_MIN/BUBBEL_ZEE (Java),
# verhaal_wereld.GUHWAII_KUST and VerhaalWereldGameTests.KUST / the bubble numbers there.
ZEE_KRIMP = 0.07
BIOME_FROM, BIOME_TO = round(0.325 + ZEE_KRIMP, 3), round(0.340 + ZEE_KRIMP, 3)   # the biome: weirdness -2 from here (it wins from about 0.330 + krimp)
DAM = tuple(round(v + ZEE_KRIMP, 3) for v in (0.330, 0.345, 0.360, 0.375))   # the dam: up from DAM[0], full DAM[1]-DAM[2], gone at DAM[3]
DAM_DEPTH = 12                        # blocks: the dam slopes down this far on both sides
WATER_FROM = round(0.357 + ZEE_KRIMP, 3)   # water in every column from here (inside the dam: at least 0.012 in)
DEEP_FROM, DEEP_TO = round(0.345 + ZEE_KRIMP, 3), round(0.44 + ZEE_KRIMP, 3)   # the terrain goes down to the sea floor between these

# the Guhbubbel (GuhbubbelStructure.java)
CELL_CHUNKS = 4                       # a cell = the structure set's spacing (4 x 4 chunks)
NEIGHBOURHOOD = 8                     # cells around: the highest peak of a sea within 8 cells (512 blocks) wins
PEAK_MIN = round(0.44 + ZEE_KRIMP, 3)   # the deep middle of a sea
BUBBLE_CENTRE_DEPTH = 25              # the sea floor in the middle: at most y 37
BUBBLE_MIN_DEPTH = 12                 # the sea floor on the rings (r 46 and 62): at most y 50, all under water

FTB_Y = 78
# the plants of the sea floor (placed features made in worldgen), in this order
VEGETATION = ["guhs:diepzee_koraal", "guhs:diepzee_zeegras", "guhs:diepzee_kelp", "guhs:diepzee_zeeaugurken", "guhs:diepzee_kaaskoraal"]


def noise(name, y_scale=0.0):
    return {"type": "minecraft:noise", "noise": f"guhs:{name}", "xz_scale": 1.0, "y_scale": y_scale}


def spline(points):
    """A smooth function of the sea noise: [(noise value, value), ...] (flat outside the points)."""
    return {"type": "minecraft:spline", "spline": {"coordinate": noise(SEA_NOISE), "points": [
        {"location": loc, "value": val, "derivative": 0.0} for loc, val in points]}}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def gradient(from_y, to_y, from_value, to_value):
    return {"type": "minecraft:y_clamped_gradient", "from_y": from_y, "to_y": to_y, "from_value": from_value, "to_value": to_value}


def sea_router(d):
    """The terrain (lowered into a basin, a dam around it) and the biome (weirdness) of the deep seas."""
    r = d["noise_router"]
    final = r["final_density"]
    assert final["type"] == "minecraft:max" and final["argument1"]["type"] == "minecraft:interpolated", "unexpected final_density"
    terrain = final["argument1"]["argument"]
    bottom = final["argument2"]
    # the sea floor: flat at FLOOR_Y (density 0.025 per block, like the Guhmension's own ~0.019), low dunes (+-1 block)
    bed = add(gradient(0, 256, 0.025 * (FLOOR_Y + 0.5), 0.025 * (FLOOR_Y + 0.5 - 256)), mul(noise(BED_NOISE), 0.03))
    deep = spline([(DEEP_FROM, 0.0), (DEEP_TO, 1.0)])
    lowered = {"type": "minecraft:interpolated", "argument": add(terrain, mul(deep, add(bed, mul(terrain, -1.0))))}
    # the dam: solid where dam + g(y) > 1, g = 1 from DAM_DEPTH under the surface to 0 one block above it
    dam = spline([(DAM[0], 0.0), (DAM[1], 1.0), (DAM[2], 1.0), (DAM[3], 0.0)])
    g = gradient(WATER_LEVEL + 1 - DAM_DEPTH, WATER_LEVEL + 1, 1.0, 0.0)
    d["noise_router"]["final_density"] = {"type": "minecraft:max", "argument1": lowered, "argument2": {
        "type": "minecraft:max", "argument1": bottom, "argument2": add(add(dam, g), -1.0)}}
    # the cheese caves' "deep" follows the new terrain; then the sea's weirdness on top of theirs. Theirs is cut off at
    # -0.5 (only its top, >= 1.22, matters), so that only the sea (-2) ever gets near the Diepe Guhzee's -1.5
    gatenkaas.router_deep(d)
    caves = {"type": "minecraft:clamp", "min": -0.5, "max": 2.0, "input": r["ridges"]}
    r["ridges"] = add(caves, mul(spline([(BIOME_FROM, 0.0), (BIOME_TO, 1.0)]), -2.0))


def surface(d):
    """The sea floor: sand with patches of pink terracotta and clay, sandstone under it; the beaches: sand."""
    def block(name):
        return {"type": "minecraft:block", "result_state": {"Name": name}}

    def patches(lo, hi):
        return {"type": "minecraft:noise_threshold", "noise": "guhs:guhmension_patches", "min_threshold": lo, "max_threshold": hi}

    def depth(offset):
        return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}

    rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]},
            "then_run": {"type": "minecraft:sequence", "sequence": [
                {"type": "minecraft:condition", "if_true": depth(0), "then_run": {"type": "minecraft:sequence", "sequence": [
                    {"type": "minecraft:condition", "if_true": {"type": "minecraft:y_above", "anchor": {"absolute": WATER_LEVEL - 1},
                                                                "surface_depth_multiplier": 0, "add_stone_depth": False},
                     "then_run": block("minecraft:sand")},
                    {"type": "minecraft:condition", "if_true": patches(0.45, 10.0), "then_run": block("minecraft:pink_terracotta")},
                    {"type": "minecraft:condition", "if_true": patches(-10.0, -0.55), "then_run": block("minecraft:clay")},
                    block("minecraft:sand")]}},
                {"type": "minecraft:condition", "if_true": depth(2), "then_run": block("minecraft:sand")},
                {"type": "minecraft:condition", "if_true": depth(5), "then_run": block("minecraft:sandstone")}]}}
    rules = d["surface_rule"]["sequence"]
    d["surface_rule"]["sequence"] = [x for x in rules if x.get("if_true", {}).get("biome_is") != [f"guhs:{BIOME}"]] + [rule]


def worldgen(h):
    D = h.D
    wg = f"{D}/worldgen"
    h.w(f"{wg}/noise/{SEA_NOISE}.json", {"firstOctave": -9, "amplitudes": [1.0, 0.3]})
    h.w(f"{wg}/noise/{BED_NOISE}.json", {"firstOctave": -5, "amplitudes": [1.0, 0.5]})
    h.patch_json(f"{wg}/noise_settings/guhmension.json", lambda d: (sea_router(d), surface(d)))

    def biome_source(d):
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] != f"guhs:{BIOME}"]
        anything = [-2.0, 2.0]
        # (depth: the surface and the water, down to the sea floor; not the deep rock, where "deep" adds up to 1)
        entries.append({"biome": f"guhs:{BIOME}", "parameters": {
            "temperature": anything, "humidity": anything, "continentalness": anything, "erosion": anything,
            "weirdness": [-2.0, -1.5], "depth": [-2.0, 1.0], "offset": 0.0}})
        d["generator"]["biome_source"]["biomes"] = entries
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    # --- the water, and what grows on the bottom ---
    h.w(f"{wg}/configured_feature/diepzee_water.json", {"type": "guhs:diepzee_water", "config": {
        "noise": f"guhs:{SEA_NOISE}", "min_value": WATER_FROM, "water_level": WATER_LEVEL}})
    h.w(f"{wg}/placed_feature/diepzee_water.json", {"feature": "guhs:diepzee_water", "placement": []})
    coral = {"Name": "guhs:kaaskoraal", "Properties": {"waterlogged": "true"}}
    h.w(f"{wg}/configured_feature/diepzee_kaaskoraal.json", {"type": "minecraft:random_patch", "config": {
        "tries": 24, "xz_spread": 5, "y_spread": 2, "feature": {
            "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": coral}}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:water"},
                {"type": "minecraft:matching_blocks", "offset": [0, 1, 0], "blocks": "minecraft:water"},
                {"type": "minecraft:would_survive", "state": coral}]}}]}}})
    # the sea floor's plants (vanilla's warm ocean ones and our kaaskoraal), never in the Guhbubbel (it has its own reef)
    outside = {"type": "guhs:outside_structure", "structure": "guhs:onderwater"}
    floor = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:biome"}, outside]
    for name, feature, count in [
        ("diepzee_kaaskoraal", "guhs:diepzee_kaaskoraal", [{"type": "minecraft:rarity_filter", "chance": 2}]),
        ("diepzee_koraal", "minecraft:warm_ocean_vegetation",
         [{"type": "minecraft:noise_based_count", "noise_to_count_ratio": 20, "noise_factor": 400.0, "noise_offset": 0.0}]),
        ("diepzee_zeegras", "minecraft:seagrass_tall", [{"type": "minecraft:count", "count": 48}]),
        ("diepzee_kelp", "minecraft:kelp", [{"type": "minecraft:noise_based_count", "noise_to_count_ratio": 80, "noise_factor": 80.0, "noise_offset": 0.0}]),
        ("diepzee_zeeaugurken", "minecraft:sea_pickle", [{"type": "minecraft:rarity_filter", "chance": 16}]),
    ]:
        h.w(f"{wg}/placed_feature/{name}.json", {"feature": feature, "placement": count + floor})

    # --- the biome: the guh sea's sky, deeper pink water ---
    base = json.load(open(f"{wg}/biome/guh_sea.json", encoding="utf-8"))
    b = json.loads(json.dumps(base))
    b["effects"].update({"water_color": 0xE86AB8, "water_fog_color": 0x8E3A86, "fog_color": 0xFFD4EA})
    b["spawners"] = {k: [] for k in base["spawners"]}
    b["spawners"]["water_ambient"] = [{"type": "guhs:guh_vis", "weight": 25, "minCount": 3, "maxCount": 6}]
    b["spawners"]["creature"] = [{"type": "guhs:guh", "weight": 60, "minCount": 2, "maxCount": 4}]
    # (no ore veins: they would eat into the Guhbubbel's rock; the seas are small enough to go round)
    b["features"] = [[], ["guhs:diepzee_water"], [], [], [], [], [], [], [], list(VEGETATION), []]
    h.w(f"{wg}/biome/{BIOME}.json", b)
    # wild Zeemeerguhs turn up in both guh seas; guh villagers born here are guh villagers
    h.w(f"{D}/tags/worldgen/biome/guhzeeen.json", {"values": ["guhs:guh_sea", f"guhs:{BIOME}"]})
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({f"guhs:{BIOME}": {"villager_type": "guhs:guh"}}))


def advancement(h):
    D = h.D
    name = "find_diepe_guhzee"
    h.w(f"{D}/advancement/guhmension/{name}.json", {
        "parent": "guhs:guhmension/enter_guhmension",
        "display": {"icon": {"id": "guhs:guh_vis_bucket"}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                    "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                    "frame": "task", "show_toast": True, "announce_to_chat": False},
        "criteria": {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": f"guhs:{BIOME}"}}}}}})
    for key, en, nl in [
        (f"biome.guhs.{BIOME}", "Deep Guh Sea", "Diepe Guhzee"),
        (f"advancements.guhs.guhmension.{name}.title", "Blub, So Deep!", "Blub, wat diep!"),
        (f"advancements.guhs.guhmension.{name}.description", "Find a Deep Guh Sea in the Guhmension",
         "Vind een Diepe Guhzee in de Guhmensie"),
    ]:
        h.lang(key, en, nl)


def build(h):
    worldgen(h)
    advancement(h)


# =====================================================================================================================
# FTB quest (row y = 78, no dependencies)
# =====================================================================================================================
def ftb(fq):
    fq.q("diepzee_vind", "De Diepe Guhzee",
         "Ergens in de Guhmensie ligt een grote, diepe roze zee met zandstrandjes: de &9Diepe Guhzee&r. Op de bodem groeit kaaskoraal, "
         "er zwemmen guhvisjes en soms een Zeemeerguh. En in het midden van elke Diepe Guhzee ligt de Guhbubbel!",
         "guhs:guh_vis_bucket", [fq.biome(BIOME)], rewards=(("guhs:kaas_knabbels", 16),), x=-8, y=FTB_Y, shape="octagon", xp=100)
