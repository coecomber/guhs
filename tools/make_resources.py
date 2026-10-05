"""
Generates the JSON resources (block models, blockstates, loot tables, worldgen, spawns, sounds.json, lang).
Hand-editing the generated JSON files is fine too - just don't re-run this script afterwards, or tweak it here.

Run from the project root:  python tools/make_resources.py
"""
import json
import os
import sys

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data", "guhs")


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# --- ore blocks: vanilla base texture + cheese puff overlay --------------------------------------------------
ORES = {
    "kaasknabbel_stone": "minecraft:block/stone",
    "kaasknabbel_deepslate": "minecraft:block/deepslate",
    "kaasknabbel_dirt": "minecraft:block/dirt",
    "kaasknabbel_cobblestone": "minecraft:block/cobblestone",
}


def cube(tex):
    return {f: {"texture": tex, "cullface": f} for f in ["down", "up", "north", "south", "west", "east"]}


w(f"{A}/models/block/kaasknabbel_ore_base.json", {
    "parent": "minecraft:block/block",
    "render_type": "minecraft:cutout_mipped",
    "textures": {"particle": "#base"},
    "elements": [
        {"from": [0, 0, 0], "to": [16, 16, 16], "faces": cube("#base")},
        {"from": [0, 0, 0], "to": [16, 16, 16], "faces": cube("#overlay")},
    ],
})
for block, base in ORES.items():
    w(f"{A}/models/block/{block}.json", {
        "parent": "guhs:block/kaasknabbel_ore_base",
        "textures": {"base": base, "overlay": "guhs:block/kaasknabbel_overlay"},
    })
    w(f"{A}/blockstates/{block}.json", {"variants": {"": {"model": f"guhs:block/{block}"}}})
    w(f"{A}/models/item/{block}.json", {"parent": f"guhs:block/{block}"})
    # silk touch -> the block itself, otherwise 1-3 kaas knabbels (fortune gives more)
    w(f"{D}/loot_table/blocks/{block}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"guhs:{block}", "conditions": [
                {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
                    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]},
        ]}]}],
        "random_sequence": f"guhs:blocks/{block}",
    })

w(f"{R}/data/minecraft/tags/block/mineable/shovel.json", {"values": ["guhs:kaasknabbel_dirt"]})

# --- items ---------------------------------------------------------------------------------------------------
w(f"{A}/models/item/kaas_knabbels.json",
  {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/kaas_knabbels"}})
w(f"{A}/models/item/guh_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


# --- worldgen: kaasknabbel veins on ALL Y levels ---------------------------------------------------------------
def ore_feature(name, targets, size, count, min_y, max_y, hidden=False):
    w(f"{D}/worldgen/configured_feature/{name}.json", {"type": "minecraft:ore", "config": {
        "size": size, "discard_chance_on_air_exposure": 1.0 if hidden else 0.0, "targets": targets}})
    w(f"{D}/worldgen/placed_feature/{name}.json", {"feature": f"guhs:{name}", "placement": [
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range",
         "height": {"type": "minecraft:uniform", "min_inclusive": min_y, "max_inclusive": max_y}},
        {"type": "minecraft:biome"},
    ]})


def tag_target(tag, block):
    return {"target": {"predicate_type": "minecraft:tag_match", "tag": tag}, "state": {"Name": block}}


def block_target(src, block):
    return {"target": {"predicate_type": "minecraft:block_match", "block": src}, "state": {"Name": block}}


BOTTOM, TOP = {"above_bottom": 0}, {"below_top": 0}
# stone + deepslate share one feature, so the vein picks the right variant at every depth
ore_feature("ore_kaasknabbel", [tag_target("minecraft:stone_ore_replaceables", "guhs:kaasknabbel_stone"),
                                tag_target("minecraft:deepslate_ore_replaceables", "guhs:kaasknabbel_deepslate")],
            size=9, count=24, min_y=BOTTOM, max_y=TOP)
ore_feature("ore_kaasknabbel_dirt", [block_target("minecraft:dirt", "guhs:kaasknabbel_dirt")],
            size=8, count=16, min_y=BOTTOM, max_y=TOP)
ore_feature("ore_kaasknabbel_cobblestone", [tag_target("minecraft:stone_ore_replaceables", "guhs:kaasknabbel_cobblestone")],
            size=6, count=6, min_y={"absolute": 0}, max_y=TOP)
w(f"{D}/neoforge/biome_modifier/add_kaasknabbel_ores.json", {
    "type": "neoforge:add_features",
    "biomes": "#minecraft:is_overworld",
    "features": ["guhs:ore_kaasknabbel", "guhs:ore_kaasknabbel_dirt", "guhs:ore_kaasknabbel_cobblestone"],
    "step": "underground_ores",
})

# --- spawning: in every overworld biome, but a bit rarer than vanilla farm animals ------------------------------
# (1.0.0: was weight 150 / 2-5, far too many. Vanilla plains: sheep 12 (4), pig 10 (4), chicken 10 (4), cow 8 (4).
#  Weight 5 in groups of 1-3 makes a guh herd rarer and smaller than a sheep or cow herd. The Guhmension keeps its
#  own, much higher density via its biome files + world/GuhmensionSpawner.)
w(f"{D}/neoforge/biome_modifier/add_guh_spawns.json", {
    "type": "neoforge:add_spawns",
    "biomes": "#minecraft:is_overworld",
    "spawners": [{"type": "guhs:guh", "weight": 5, "minCount": 1, "maxCount": 3}],
})

# --- guh drops a bit of pink wool (it is a plush after all) ----------------------------------------------------
w(f"{D}/loot_table/entities/guh.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "minecraft:pink_wool", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}]}],
    "random_sequence": "guhs:entities/guh"})


# --- sounds (the .ogg files live in assets/guhs/sounds/) --------------------------------------------------------
def sound(event, *files):
    return {"sounds": [f"guhs:{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}


AMBIENT = [f"guh_ambient{i}" for i in range(1, 17)]
w(f"{A}/sounds.json", {
    "entity.guh.ambient": sound("entity.guh.ambient", *AMBIENT),
    "entity.guh.hurt": sound("entity.guh.hurt", "guh_hurt1", "guh_hurt2"),
    "entity.guh.death": sound("entity.guh.death", "guh_death"),
    "entity.guh.eat": sound("entity.guh.eat", "guh_eat"),
    "entity.guh.happy": sound("entity.guh.happy", *AMBIENT),  # a random one of the guh noises
    # Mika: the same noises made evil (tools/make_mika_sounds.py)
    "entity.mika.ambient": sound("entity.mika.ambient", *[a.replace("guh_", "mika_") for a in AMBIENT]),
    "entity.mika.hurt": sound("entity.mika.hurt", "mika_hurt1", "mika_hurt2"),
    "entity.mika.death": sound("entity.mika.death", "mika_death"),
    # music discs: streamed, like the vanilla records
    "music_disc.ze_hangen": {"sounds": [{"name": "guhs:music_disc_ze_hangen", "stream": True}]},
})

# --- Block of Kaasknabbels + portal ------------------------------------------------------------------------------
w(f"{A}/models/block/block_of_kaasknabbels.json",
  {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/block_of_kaasknabbels"}})
w(f"{A}/blockstates/block_of_kaasknabbels.json", {"variants": {"": {"model": "guhs:block/block_of_kaasknabbels"}}})
w(f"{A}/models/item/block_of_kaasknabbels.json", {"parent": "guhs:block/block_of_kaasknabbels"})
w(f"{D}/loot_table/blocks/block_of_kaasknabbels.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:block_of_kaasknabbels"}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    "random_sequence": "guhs:blocks/block_of_kaasknabbels"})
w(f"{R}/data/minecraft/tags/block/mineable/hoe.json", {"values": ["guhs:block_of_kaasknabbels"]})

for name, parent in (("guh_portal_ns", "minecraft:block/nether_portal_ns"), ("guh_portal_ew", "minecraft:block/nether_portal_ew")):
    w(f"{A}/models/block/{name}.json", {"parent": parent, "render_type": "minecraft:translucent",
                                         "textures": {"particle": "guhs:block/guh_portal", "portal": "guhs:block/guh_portal"}})
w(f"{A}/blockstates/guh_portal.json", {"variants": {
    "axis=x": {"model": "guhs:block/guh_portal_ns"},
    "axis=z": {"model": "guhs:block/guh_portal_ew"}}})

w(f"{D}/recipe/block_of_kaasknabbels.json", {
    "type": "minecraft:crafting_shaped", "category": "building",
    "pattern": ["###", "###", "###"], "key": {"#": {"item": "guhs:kaas_knabbels"}},
    "result": {"id": "guhs:block_of_kaasknabbels", "count": 1}})
w(f"{D}/recipe/kaas_knabbels_from_block.json", {
    "type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [{"item": "guhs:block_of_kaasknabbels"}],
    "result": {"id": "guhs:kaas_knabbels", "count": 9}})

# --- The Guhmension ----------------------------------------------------------------------------------------------
# dimension = dimension type (rules: sky, light, height...) + generator (terrain shape + biome)
w(f"{D}/dimension_type/guhmension.json", {
    "ultrawarm": False, "natural": True, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": True,
    "has_raids": False, "has_skylight": True, "has_ceiling": False, "coordinate_scale": 1.0, "ambient_light": 0.1,
    "logical_height": 256, "min_y": 0, "height": 256,
    "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:overworld",
    "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0})
# Small biomes: the biome is picked from two fast-changing noises ("temperature" and "vegetation" in the router),
# so biomes are only ~50-150 blocks across. Mika's biome only wins where both noises are high -> rare.
# A third noise, "relief" (the router's erosion slot), decides the kind of terrain: low = almost flat, middle = rolling
# hills, high = very steep. The same noise scales the hills in final_density, so the terrain always matches its biome.
FLAT, ROLLING, STEEP = [-1.0, -0.25], [-0.25, 0.25], [0.25, 1.0]
BIOMES = {
    # name: (temperature, humidity, relief) -- a point, or a [min, max] range
    "guh_fields": (0.0, 0.0, ROLLING),
    "knabbel_crumbs": (0.45, -0.3, ROLLING),
    "pink_puffs": (-0.45, 0.2, ROLLING),
    "mikas_biome": ([0.45, 1.0], [0.45, 1.0], ROLLING),
    "kaas_flats": (0.35, 0.0, FLAT),
    "guh_meadows": (-0.35, 0.0, FLAT),
    "guh_peaks": (-0.3, 0.0, STEEP),
    "vads_cliffs": (0.3, 0.0, STEEP),
}
FLAT_BIOMES = ["kaas_flats", "guh_meadows"]
STEEP_BIOMES = ["guh_peaks", "vads_cliffs"]
# houses, picnics and fountains need room: rolling + flat biomes (not Mika's)
HAMSTER_BIOMES = ["guh_fields", "knabbel_crumbs", "pink_puffs"] + FLAT_BIOMES
w(f"{D}/dimension/guhmension.json", {
    "type": "guhs:guhmension",
    "generator": {"type": "minecraft:noise", "settings": "guhs:guhmension",
                  "biome_source": {"type": "minecraft:multi_noise", "biomes": [
                      {"biome": f"guhs:{name}", "parameters": {
                          "temperature": t, "humidity": h, "continentalness": 0.0, "erosion": r,
                          "weirdness": 0.0, "depth": 0.0, "offset": 0.0}}
                      for name, (t, h, r) in BIOMES.items()]}}})

# terrain: soft rolling pink wool hills (a y-gradient pushed around by a lazy 3D noise -> the odd puffy overhang)
w(f"{D}/worldgen/noise/guhmension_hills.json", {"firstOctave": -7, "amplitudes": [1.0, 1.0, 0.5, 0.25]})
w(f"{D}/worldgen/noise/guhmension_patches.json", {"firstOctave": -5, "amplitudes": [1.0, 0.5]})
w(f"{D}/worldgen/noise/guhmension_temperature.json", {"firstOctave": -6, "amplitudes": [1.0, 0.5]})
w(f"{D}/worldgen/noise/guhmension_vegetation.json", {"firstOctave": -6, "amplitudes": [1.0, 0.5]})
w(f"{D}/worldgen/noise/guhmension_relief.json", {"firstOctave": -8, "amplitudes": [1.0, 0.5]})
w(f"{D}/worldgen/noise/guhmension_mountains.json", {"firstOctave": -7, "amplitudes": [1.0, 0.5, 0.25]})
w(f"{D}/worldgen/noise/guhmension_spikes.json", {"firstOctave": -5, "amplitudes": [1.0, 0.6, 0.3]})
RELIEF = {"type": "minecraft:noise", "noise": "guhs:guhmension_relief", "xz_scale": 1.0, "y_scale": 0.0}


def relief_spline(points):
    """A smooth function of the relief noise: [(relief, value), ...]."""
    return {"type": "minecraft:spline", "spline": {"coordinate": RELIEF, "points": [
        {"location": loc, "value": val, "derivative": 0.0} for loc, val in points]}}
ZERO = 0.0


def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name}}


def in_biome(*names):
    return {"type": "minecraft:biome", "biome_is": [f"guhs:{n}" for n in names]}


def patches(min_threshold):
    return {"type": "minecraft:noise_threshold", "noise": "guhs:guhmension_patches",
            "min_threshold": min_threshold, "max_threshold": 10.0}


def surface(depth=1):
    """The top `depth + 1` blocks of the ground."""
    return {"type": "minecraft:stone_depth", "offset": depth, "add_surface_depth": False,
            "secondary_depth_range": 0, "surface_type": "floor"}


def cond(if_true, then_run):
    return {"type": "minecraft:condition", "if_true": if_true, "then_run": then_run}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


w(f"{D}/worldgen/noise_settings/guhmension.json", {
    "sea_level": 0, "disable_mob_generation": False, "aquifers_enabled": False, "ore_veins_enabled": False,
    "legacy_random_source": False,
    "default_block": {"Name": "minecraft:pink_wool"},
    "default_fluid": {"Name": "minecraft:air"},
    "noise": {"min_y": 0, "height": 256, "size_horizontal": 2, "size_vertical": 1},
    "noise_router": {
        "barrier": ZERO, "fluid_level_floodedness": ZERO, "fluid_level_spread": ZERO, "lava": ZERO,
        "temperature": {"type": "minecraft:noise", "noise": "guhs:guhmension_temperature", "xz_scale": 1.0, "y_scale": 0.0},
        "vegetation": {"type": "minecraft:noise", "noise": "guhs:guhmension_vegetation", "xz_scale": 1.0, "y_scale": 0.0},
        "continents": ZERO, "erosion": RELIEF, "depth": ZERO, "ridges": ZERO,
        "initial_density_without_jaggedness": ZERO,
        # ground around y70 (from y30 up), and everything below y30 always solid. The relief noise picks the terrain:
        # almost flat (hills x0.03) -> rolling hills (x1) -> very steep (tall 2D mountains + sharp spikes, lifted up)
        "final_density": {"type": "minecraft:max",
            "argument1": {"type": "minecraft:interpolated", "argument": {"type": "minecraft:add",
                # keeps falling all the way up, so nothing floats near the build limit
                "argument1": {"type": "minecraft:y_clamped_gradient", "from_y": 0, "to_y": 256,
                              "from_value": 1.3, "to_value": -3.5},
                "argument2": {"type": "minecraft:add",
                    "argument1": {"type": "minecraft:mul",
                        "argument1": {"type": "minecraft:noise", "noise": "guhs:guhmension_hills", "xz_scale": 1.0, "y_scale": 0.35},
                        "argument2": relief_spline([(-0.32, 0.03), (-0.18, 1.0)])},
                    "argument2": {"type": "minecraft:add",
                        "argument1": relief_spline([(0.18, 0.0), (0.32, 0.45)]),
                        "argument2": {"type": "minecraft:add",
                            "argument1": {"type": "minecraft:mul",
                                "argument1": {"type": "minecraft:noise", "noise": "guhs:guhmension_mountains", "xz_scale": 1.0, "y_scale": 0.06},
                                "argument2": relief_spline([(0.18, 0.0), (0.32, 3.5)])},
                            "argument2": {"type": "minecraft:mul",
                                "argument1": {"type": "minecraft:noise", "noise": "guhs:guhmension_spikes", "xz_scale": 1.0, "y_scale": 0.12},
                                "argument2": relief_spline([(0.18, 0.0), (0.32, 1.6)])}}}}}},
            "argument2": {"type": "minecraft:y_clamped_gradient", "from_y": 29, "to_y": 31, "from_value": 1.0, "to_value": -1.0}},
        "vein_toggle": ZERO, "vein_ridged": ZERO, "vein_gap": ZERO,
    },
    "spawn_target": [],
    # everything below the surface is pink wool (default_block); per biome the top layers differ
    "surface_rule": seq(
        # the very bottom (y=0) is bedrock
        cond({"type": "minecraft:vertical_gradient", "random_name": "guhs:netherrack_floor",
              "true_at_and_below": {"absolute": 0}, "false_at_and_above": {"absolute": 1}}, block("minecraft:bedrock")),
        # Mika's biome: darker pink (magenta wool) with a few pink spots, 4 blocks deep
        cond(in_biome("mikas_biome"), seq(
            cond(surface(0), seq(cond(patches(0.5), block("minecraft:pink_wool")), block("minecraft:magenta_wool"))),
            cond(surface(3), block("minecraft:magenta_wool")))),
        # Knabbel crumbs: mostly kaasknabbels on top, with pink wool spots
        cond(in_biome("knabbel_crumbs"), cond(surface(1), seq(
            cond(patches(0.3), block("minecraft:pink_wool")), block("guhs:block_of_kaasknabbels")))),
        # Guh peaks: pink wool mountains with white "snowy" wool tops (above y125)
        cond(in_biome("guh_peaks"), cond(surface(1), seq(
            cond({"type": "minecraft:y_above", "anchor": {"absolute": 125}, "surface_depth_multiplier": 0,
                  "add_stone_depth": False}, block("minecraft:white_wool")),
            cond(patches(0.4), block("minecraft:pink_terracotta"))))),
        # Vads cliffs: magenta and pink terracotta rock faces
        cond(in_biome("vads_cliffs"), cond(surface(2), seq(
            cond(patches(0.2), block("minecraft:magenta_terracotta")), block("minecraft:pink_terracotta")))),
        # Kaas flats: a flat plain of kaasknabbels, criss-crossed with pink wool
        cond(in_biome("kaas_flats"), cond(surface(1), seq(
            cond(patches(0.45), block("minecraft:pink_wool")), block("guhs:block_of_kaasknabbels")))),
        # Guh meadows: flat pink wool with little patches of pink concrete powder "flowers"
        cond(in_biome("guh_meadows"), cond(surface(0), cond(patches(0.55), block("minecraft:pink_concrete_powder")))),
        # Guh fields: pink wool with big kaasknabbel patches (Pink puffs: plain soft pink wool)
        cond(in_biome("guh_fields"), cond(surface(1), cond(patches(0.35), block("guhs:block_of_kaasknabbels")))),
    ),
})

WOOL = [block_target("minecraft:pink_wool", "guhs:block_of_kaasknabbels"),
        block_target("minecraft:pink_terracotta", "guhs:block_of_kaasknabbels"),
        block_target("minecraft:magenta_wool", "guhs:block_of_kaasknabbels")]
ore_feature("guhmension_kaasknabbel_veins", WOOL, size=20, count=30, min_y=BOTTOM, max_y=TOP)
ore_feature("guhmension_kaasknabbel_veins_rich", WOOL, size=24, count=45, min_y=BOTTOM, max_y=TOP)
# crying obsidian: very rare, every height including the surface (no discarding when exposed to air)
ore_feature("mikas_crying_obsidian",
            [block_target("minecraft:pink_wool", "minecraft:crying_obsidian"),
             block_target("minecraft:magenta_wool", "minecraft:crying_obsidian")],
            size=3, count=3, min_y=BOTTOM, max_y=TOP)

ore_feature("vahoege_vads_ore", [block_target("minecraft:pink_wool", "guhs:compressed_super_vahoege_vads"),
                                 block_target("minecraft:magenta_wool", "guhs:compressed_super_vahoege_vads")],
            size=5, count=5, min_y=BOTTOM, max_y={"absolute": 90}, hidden=True)
# Vads cliffs: extra vads, and here it may show on the cliff faces
ore_feature("vahoege_vads_ore_rich", [block_target("minecraft:pink_wool", "guhs:compressed_super_vahoege_vads"),
                                      block_target("minecraft:pink_terracotta", "guhs:compressed_super_vahoege_vads"),
                                      block_target("minecraft:magenta_terracotta", "guhs:compressed_super_vahoege_vads")],
            size=4, count=10, min_y=BOTTOM, max_y=TOP)

NO_SPAWNS = {"monster": [], "ambient": [], "axolotls": [], "underground_water_creature": [],
             "water_creature": [], "water_ambient": [], "misc": []}


def guhmension_biome(name, sky, fog, ores, creature_probability=0.25, extra_spawns=None, guh_group=(3, 6)):
    spawners = {**NO_SPAWNS, "creature": [{"type": "guhs:guh", "weight": 100, "minCount": guh_group[0],
                                           "maxCount": guh_group[1]}]}
    spawners.update(extra_spawns or {})
    w(f"{D}/worldgen/biome/{name}.json", {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.0,
        "creature_spawn_probability": creature_probability,
        "effects": {"sky_color": sky, "fog_color": fog, "water_color": 0xF7A8C8, "water_fog_color": 0xE0709A,
                    "grass_color": 0xF7B6CB, "foliage_color": 0xF7B6CB,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8,
                                   "offset": 2.0}},
        "spawners": spawners,
        "spawn_costs": {},
        "carvers": {"air": []},
        # 11 generation steps; step 6 = underground_ores
        "features": [[], [], [], [], [], [], [f"guhs:{o}" for o in ores + ["vahoege_vads_ore"]], [], [], [], []],
    })


guhmension_biome("guh_fields", 0xFFB3D1, 0xFFD6E6, ["guhmension_kaasknabbel_veins"])
guhmension_biome("knabbel_crumbs", 0xFFC29E, 0xFFE0C2, ["guhmension_kaasknabbel_veins_rich"])
guhmension_biome("pink_puffs", 0xF7C6E6, 0xFFE8F4, ["guhmension_kaasknabbel_veins"], creature_probability=0.35)
guhmension_biome("kaas_flats", 0xFFD08A, 0xFFE6BF, ["guhmension_kaasknabbel_veins_rich"])
guhmension_biome("guh_meadows", 0xFFA6CC, 0xFFD9EA, ["guhmension_kaasknabbel_veins"], creature_probability=0.45,
                 guh_group=(4, 8))
guhmension_biome("guh_peaks", 0xE8B4F0, 0xF6DDFA, ["guhmension_kaasknabbel_veins"], creature_probability=0.15)
guhmension_biome("vads_cliffs", 0xD08ABF, 0xE8B8DA, ["guhmension_kaasknabbel_veins", "vahoege_vads_ore_rich"],
                 creature_probability=0.12)
guhmension_biome("mikas_biome", 0x8C2A5A, 0xB04A7A, ["guhmension_kaasknabbel_veins", "mikas_crying_obsidian"],
                 creature_probability=0.1,
                 extra_spawns={"monster": [{"type": "guhs:mika", "weight": 100, "minCount": 1, "maxCount": 1}]})

# --- structures: hamster houses (giant guh + guh spawner) and Evil Mika homes -----------------------------------
# The buildings themselves are .nbt templates made by tools/make_structures.py (data/guhs/structure/).


TEMPLATE_SIZES = {  # largest side of each surface structure's template (for the flatness check)
    "hamster_house": 32, "hamster_house_medium": 46, "hamster_house_large": 72, "hamster_house_extra_extra_large": 128,
    "evil_mika_home": 22, "guh_picnic": 13, "cheese_fountain": 17, "grand_cheese_fountain": 31, "guh_statue": 50,
    "guhramid": 41, "guh_village": 64, "mini_picnic": 5, "quartz_statue": 4, "block_guh": 8, "guh_fossil": 18,
    "giant_kaasknabbel": 25, "giant_cake": 17, "kaasknabbel_arch": 41, "vadsig_heiligdom": 21, "mika_kamp": 23, "sleehut": 15}


FLATNESS = {"sleehut": 18, "hamster_house_extra_extra_large": 40}  # (the sled hut is on the steep Guh Peaks)


def structure(name, biomes, spacing, separation, salt, spawn_overrides=None, own_set=True):
    jigsaw = {"type": "minecraft:jigsaw", "biomes": f"#guhs:has_structure/{name}", "step": "surface_structures",
              "spawn_overrides": spawn_overrides or {}, "terrain_adaptation": "beard_box",
              "start_pool": f"guhs:{name}/start", "size": 1, "start_height": {"absolute": 0},
              "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80, "use_expansion_hack": False}
    # only on flat enough ground (guhs:flat_jigsaw samples the surface around the start): no half mountains, no floating
    radius = TEMPLATE_SIZES.get(name, 16)
    w(f"{D}/worldgen/structure/{name}.json", {
        "type": "guhs:flat_jigsaw", "biomes": jigsaw["biomes"], "step": jigsaw["step"], "spawn_overrides": jigsaw["spawn_overrides"],
        "terrain_adaptation": "beard_box", "check_radius": radius, "max_height_difference": FLATNESS.get(name, 6 + radius // 4),
        "keep_clear": (radius + 1) // 2, "jigsaw": jigsaw})
    w(f"{D}/worldgen/template_pool/{name}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{name}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    if own_set:
        w(f"{D}/worldgen/structure_set/{name}.json", {
            "structures": [{"structure": f"guhs:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt}})
    w(f"{D}/tags/worldgen/biome/has_structure/{name}.json",
      {"values": [b if ":" in b else f"guhs:{b}" for b in biomes]})


# Hamster houses come in 3 sizes that share one spawn slot: roughly one per 30x30 chunks (~480x480 blocks),
# mostly small ones. spacing/separation are in chunks.
for size in ("hamster_house", "hamster_house_medium", "hamster_house_large"):
    structure(size, HAMSTER_BIOMES, 0, 0, 0, own_set=False)
# the big ones reach further from their centre
for size in ("hamster_house_medium", "hamster_house_large"):
    path = f"{D}/worldgen/structure/{size}.json"
    data = json.load(open(path))
    data["jigsaw"]["max_distance_from_center"] = 116
    w(path, data)
w(f"{D}/worldgen/structure_set/hamster_house.json", {
    "structures": [{"structure": "guhs:hamster_house", "weight": 5},
                   {"structure": "guhs:hamster_house_medium", "weight": 3},
                   {"structure": "guhs:hamster_house_large", "weight": 2}],
    "placement": {"type": "minecraft:random_spread", "spacing": 30, "separation": 12, "salt": 73451289}})
# Guhland: the extra extra large hamster house, a whole theme park; way rarer (its own slot, ~1 per 64x64 chunks)
structure("hamster_house_extra_extra_large", HAMSTER_BIOMES, spacing=64, separation=24, salt=64646464)
path = f"{D}/worldgen/structure/hamster_house_extra_extra_large.json"
data = json.load(open(path))
data["jigsaw"]["max_distance_from_center"] = 116
w(path, data)
# Guh statue: the guh model built out of blocks. Quite rare.
structure("guh_statue", HAMSTER_BIOMES + ["mikas_biome"], spacing=36, separation=12, salt=36363636)
# Builds copied from the "Guh structures" world by tools/import_world_builds.py. Each sits one block into the ground
# (its bottom layer replaces the top block of the terrain), just like in the world they were built in.
IMPORTED = [  # name, biomes, spacing, separation (in chunks)
    ("mini_picnic", HAMSTER_BIOMES, 22, 8),     # (1.1.2: was 16/6, about half as many)
    ("quartz_statue", HAMSTER_BIOMES, 24, 9),   # (1.1.2: was 18/6, about half as many)
    ("block_guh", HAMSTER_BIOMES, 20, 7),
    ("guh_fossil", HAMSTER_BIOMES + STEEP_BIOMES + ["mikas_biome"], 20, 7),
    ("giant_kaasknabbel", HAMSTER_BIOMES, 26, 9),
    ("giant_cake", HAMSTER_BIOMES, 28, 10),
    ("kaasknabbel_arch", HAMSTER_BIOMES, 32, 12),
]
for i, (name, biomes, spacing, separation) in enumerate(IMPORTED):
    structure(name, biomes, spacing=spacing, separation=separation, salt=51000000 + i * 1111)
    path = f"{D}/worldgen/structure/{name}.json"
    data = json.load(open(path))
    data["jigsaw"]["start_height"] = {"absolute": -1}
    w(path, data)
structure("evil_mika_home", ["mikas_biome"], spacing=14, separation=5, salt=13666013,
          spawn_overrides={"monster": {"bounding_box": "piece", "spawns": [
              {"type": "guhs:mika", "weight": 1, "minCount": 1, "maxCount": 2}]}})


def count(lo, hi):
    return [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]


w(f"{D}/loot_table/chests/hamster_house.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
        {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 10, "functions": count(4, 16)},
        {"type": "minecraft:item", "name": "guhs:block_of_kaasknabbels", "weight": 4, "functions": count(1, 4)},
        {"type": "minecraft:item", "name": "minecraft:saddle", "weight": 4},
        {"type": "minecraft:item", "name": "guhs:guh_cave_compass", "weight": 3},
        {"type": "minecraft:item", "name": "guhs:challenge_compass", "weight": 2},
        {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 3, "functions": count(1, 4)},
        {"type": "minecraft:item", "name": "minecraft:pink_wool", "weight": 6, "functions": count(4, 12)},
    ]}]})
w(f"{D}/loot_table/chests/evil_mika_home.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
        {"type": "minecraft:item", "name": "guhs:mika_vet", "weight": 8, "functions": count(1, 3)},
        {"type": "minecraft:item", "name": "minecraft:crying_obsidian", "weight": 5, "functions": count(1, 4)},
        {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 6, "functions": count(4, 12)},
        {"type": "minecraft:item", "name": "minecraft:saddle", "weight": 2},
        {"type": "minecraft:item", "name": "guhs:guh_cave_compass", "weight": 3},
        {"type": "minecraft:item", "name": "guhs:challenge_compass", "weight": 2},
    ]}]})

# Hungry Guh picnic: semi-rare, in the normal Guhmension biomes (not Mika's) and in overworld land biomes
OVERWORLD_LAND = ["plains", "sunflower_plains", "meadow", "flower_forest", "forest", "birch_forest", "cherry_grove",
                  "dark_forest", "taiga", "snowy_plains", "savanna", "desert", "old_growth_birch_forest", "grove",
                  "windswept_hills", "jungle", "sparse_jungle", "badlands", "mushroom_fields", "snowy_taiga"]

# Guh caves: an underground jigsaw network (central room + hamster tubes + side rooms) in the Guhmension
w(f"{D}/worldgen/structure/guh_caves.json", {
    "type": "minecraft:jigsaw", "biomes": "#guhs:has_structure/guh_caves", "step": "underground_structures",
    # "none", not "bury": burying piles terrain over tubes under valleys, which also buried surface structures there
    "spawn_overrides": {}, "terrain_adaptation": "none", "start_pool": "guhs:guh_caves/start", "size": 7,
    # a fixed depth, not relative to the surface: the tubes reach up to 80 blocks sideways, and under valleys they used
    # to come out above ground. The Guhmension is always solid below y30, so y14-24 is always underground.
    "start_height": {"absolute": 14},
    "max_distance_from_center": 80, "use_expansion_hack": False})


def pool(name, elements, fallback="minecraft:empty"):
    w(f"{D}/worldgen/template_pool/{name}.json", {"fallback": fallback, "elements": [
        {"weight": weight, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{location}",
                                       "projection": "rigid", "processors": "minecraft:empty"}}
        for location, weight in elements]})


pool("guh_caves/start", [("guh_caves/central_room", 1)])
pool("guh_caves/tubes", [("guh_caves/tube_straight", 8), ("guh_caves/tube_junction", 3), ("guh_caves/nest_room", 2),
                         ("guh_caves/pantry_room", 2), ("guh_caves/portrait_room", 2)], fallback="guhs:guh_caves/ends")
pool("guh_caves/ends", [("guh_caves/tube_end", 1)])
w(f"{D}/worldgen/structure_set/guh_caves.json", {
    "structures": [{"structure": "guhs:guh_caves", "weight": 1}],
    # (spacing 7, not 9: about a quarter of the networks give way to other buildings now, see BouwRuimte.java;
    # 1.1.2: 10/4, about half as many: they were by far the most common building of the Guhmension)
    "placement": {"type": "minecraft:random_spread", "spacing": 10, "separation": 4, "salt": 27272727}})
w(f"{D}/tags/worldgen/biome/has_structure/guh_caves.json",
  {"values": [f"guhs:{b}" for b in HAMSTER_BIOMES + STEEP_BIOMES]})

w(f"{D}/worldgen/structure/challenging_guh_caves.json", {
    "type": "minecraft:jigsaw", "biomes": "#guhs:has_structure/challenging_guh_caves", "step": "underground_structures",
    "spawn_overrides": {"monster": {"bounding_box": "full", "spawns": [
        {"type": "guhs:mika", "weight": 3, "minCount": 1, "maxCount": 2},
        {"type": "minecraft:zombie", "weight": 2, "minCount": 1, "maxCount": 2},
        {"type": "minecraft:skeleton", "weight": 2, "minCount": 1, "maxCount": 1}]}},
    "terrain_adaptation": "none", "start_pool": "guhs:challenging_guh_caves/start", "size": 6,
    # fixed and deep (y6-19), always under the y30 solid layer; not lower: the parkour room reaches 5 below the start
    # and nothing can be built in the bottom layer (y0) of the world
    "start_height": {"absolute": 6},
    "max_distance_from_center": 80, "use_expansion_hack": False})
pool("challenging_guh_caves/start", [("challenging_guh_caves/dungeon_hall", 1)])
pool("challenging_guh_caves/tubes", [("challenging_guh_caves/tube_straight", 6), ("challenging_guh_caves/tube_junction", 2),
                                     ("challenging_guh_caves/spawner_room", 3), ("challenging_guh_caves/parkour_room", 3),
                                     ("challenging_guh_caves/mika_den", 2)], fallback="guhs:challenging_guh_caves/ends")
pool("challenging_guh_caves/ends", [("challenging_guh_caves/tube_end", 1)])
w(f"{D}/worldgen/structure_set/challenging_guh_caves.json", {
    "structures": [{"structure": "guhs:challenging_guh_caves", "weight": 1}],
    # (spacing 18, not 20: some give way to other buildings now, see BouwRuimte.java; 1.1.2: 24/9, about half as many)
    "placement": {"type": "minecraft:random_spread", "spacing": 24, "separation": 9, "salt": 31313131}})
w(f"{D}/tags/worldgen/biome/has_structure/challenging_guh_caves.json",
  {"values": [f"guhs:{b}" for b in HAMSTER_BIOMES + STEEP_BIOMES + ["mikas_biome"]]})

# Guhramid: a very rare pink pyramid (treasure hall, mummy guh, TNT trap, secret room with a golden guh)
structure("guhramid", HAMSTER_BIOMES, spacing=56, separation=20, salt=77777777)
w(f"{D}/loot_table/chests/guhramid.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
        {"type": "minecraft:item", "name": "minecraft:gold_ingot", "weight": 8, "functions": count(2, 8)},
        {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 6, "functions": count(1, 3)},
        {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 6, "functions": count(2, 6)},
        {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 5, "functions": count(1, 3)},
        {"type": "minecraft:item", "name": "minecraft:book", "weight": 4,
         "functions": [{"function": "minecraft:enchant_randomly"}]},
        {"type": "minecraft:item", "name": "minecraft:golden_apple", "weight": 3},
        {"type": "minecraft:item", "name": "minecraft:saddle", "weight": 3},
        {"type": "minecraft:item", "name": "guhs:guh_cave_compass", "weight": 3},
        {"type": "minecraft:item", "name": "guhs:challenge_compass", "weight": 3},
        {"type": "minecraft:item", "name": "guhs:diamond_guh_armor", "weight": 2},
    ]}]})
w(f"{D}/loot_table/chests/guhramid_secret.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
        {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 8, "functions": count(3, 8)},
        {"type": "minecraft:item", "name": "minecraft:netherite_scrap", "weight": 4, "functions": count(1, 2)},
        {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 6, "functions": count(2, 5)},
        {"type": "minecraft:item", "name": "minecraft:enchanted_golden_apple", "weight": 1},
        {"type": "minecraft:item", "name": "guhs:netherite_guh_armor", "weight": 1},
    ]}]})
# Guh villages: about as common as villages in the overworld, two layouts
structure("guh_village", HAMSTER_BIOMES, spacing=34, separation=8, salt=10387312)
pool("guh_village/start", [("guh_village/layout_a", 1), ("guh_village/layout_b", 1)])
structure("guh_picnic", HAMSTER_BIOMES + [f"minecraft:{b}" for b in OVERWORLD_LAND],
          spacing=28, separation=10, salt=42424242)
# Cheese fountain: rare, same places as the picnic
structure("cheese_fountain", HAMSTER_BIOMES + [f"minecraft:{b}" for b in OVERWORLD_LAND],
          spacing=26, separation=9, salt=18181818)
# Grand cheese fountain: the big one, as rare as the normal fountain used to be
structure("grand_cheese_fountain", HAMSTER_BIOMES + [f"minecraft:{b}" for b in OVERWORLD_LAND],
          spacing=40, separation=14, salt=19191919)
w(f"{D}/loot_table/chests/guh_picnic.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
        {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 10, "functions": count(8, 20)},
        {"type": "minecraft:item", "name": "minecraft:iron_ingot", "weight": 5, "functions": count(1, 5)},
        {"type": "minecraft:item", "name": "guhs:mika_vet", "weight": 2},
        {"type": "minecraft:item", "name": "minecraft:cake", "weight": 3},
        {"type": "minecraft:item", "name": "guhs:guh_cave_compass", "weight": 3},
        {"type": "minecraft:item", "name": "guhs:challenge_compass", "weight": 2},
        {"type": "minecraft:item", "name": "minecraft:pink_wool", "weight": 4, "functions": count(2, 8)},
    ]}]})
# (1.2.8) the grand cheese fountain's chest: only kaasknabbels, a few fried ones and a little iron and gold. Nothing else, no
# vads (Balans128GameTests checks that). The small cheese fountain has no chest.
w(f"{D}/loot_table/chests/grand_cheese_fountain.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 6}, "entries": [
        {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 10, "functions": count(6, 14)},
        {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 4, "functions": count(2, 4)},
        {"type": "minecraft:item", "name": "minecraft:iron_nugget", "weight": 3, "functions": count(4, 9)},
        {"type": "minecraft:item", "name": "minecraft:gold_nugget", "weight": 3, "functions": count(3, 8)},
        {"type": "minecraft:item", "name": "minecraft:iron_ingot", "weight": 2, "functions": count(1, 2)},
        {"type": "minecraft:item", "name": "minecraft:gold_ingot", "weight": 1, "functions": count(1, 2)},
    ]}]})

# --- Bank Guh: infinite storage block (drawn by GeckoLib; the block model only provides break particles) -----------
w(f"{A}/models/block/bank_guh.json", {"textures": {"particle": "minecraft:block/pink_wool"}})
w(f"{A}/blockstates/bank_guh.json", {"variants": {f"facing={f}": {"model": "guhs:block/bank_guh"} for f in ("north", "east", "south", "west")}})
w(f"{A}/models/item/bank_guh.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/bank_guh"}})
w(f"{A}/models/item/quest_guh_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
# always drops, and takes its whole stomach with it (like a shulker box)
w(f"{D}/loot_table/blocks/bank_guh.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:bank_guh", "functions": [
        {"function": "minecraft:copy_components", "source": "block_entity", "include": ["guhs:bank_contents"]}]}]}],
    "random_sequence": "guhs:blocks/bank_guh"})

# --- guh armour ---------------------------------------------------------------------------------------------------
for tier, ingot in (("iron", "minecraft:iron_ingot"), ("diamond", "minecraft:diamond")):
    w(f"{A}/models/item/{tier}_guh_armor.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{tier}_guh_armor"}})
    w(f"{D}/recipe/{tier}_guh_armor.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["I I", "IKI", "III"], "key": {"I": {"item": ingot}, "K": {"item": "guhs:kaas_knabbels"}},
        "result": {"id": f"guhs:{tier}_guh_armor", "count": 1}})
w(f"{A}/models/item/netherite_guh_armor.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/netherite_guh_armor"}})
w(f"{D}/recipe/netherite_guh_armor_smithing.json", {"type": "minecraft:smithing_transform",
    "template": {"item": "minecraft:netherite_upgrade_smithing_template"}, "base": {"item": "guhs:diamond_guh_armor"},
    "addition": {"item": "minecraft:netherite_ingot"}, "result": {"id": "guhs:netherite_guh_armor", "count": 1}})

# --- Vahoege Vads ----------------------------------------------------------------------------------------------------
w(f"{A}/models/block/compressed_super_vahoege_vads.json", {"parent": "minecraft:block/cube_all",
                                                            "textures": {"all": "guhs:block/compressed_super_vahoege_vads"}})
w(f"{A}/blockstates/compressed_super_vahoege_vads.json", {"variants": {"": {"model": "guhs:block/compressed_super_vahoege_vads"}}})
w(f"{A}/models/item/compressed_super_vahoege_vads.json", {"parent": "guhs:block/compressed_super_vahoege_vads"})
w(f"{D}/loot_table/blocks/compressed_super_vahoege_vads.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": "guhs:compressed_super_vahoege_vads", "conditions": [
            {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
                {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
        {"type": "minecraft:item", "name": "guhs:vahoege_vads", "functions": [
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
            {"function": "minecraft:explosion_decay"}]}]}]}],
    "random_sequence": "guhs:blocks/compressed_super_vahoege_vads"})
w(f"{R}/data/minecraft/tags/block/needs_iron_tool.json", {"values": ["guhs:compressed_super_vahoege_vads"]})
for item in ("vahoege_vads", "vahoege_vads_ingot", "vahoege_vads_helmet", "vahoege_vads_chestplate", "vahoege_vads_leggings",
             "vahoege_vads_boots"):
    w(f"{A}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{item}"}})
TOOLS = ("sword", "pickaxe", "axe", "shovel", "hoe", "paxel")
for tool in TOOLS:
    w(f"{A}/models/item/vahoege_vads_{tool}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"guhs:item/vahoege_vads_{tool}"}})
for kind in ("smelting", "blasting"):
    w(f"{D}/recipe/vahoege_vads_ingot_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc",
        "ingredient": {"item": "guhs:vahoege_vads"}, "result": {"id": "guhs:vahoege_vads_ingot"},
        "experience": 1.0, "cookingtime": 200 if kind == "smelting" else 100})
V = {"item": "guhs:vahoege_vads_ingot"}
STICK = {"item": "minecraft:stick"}
SHAPES = {
    "sword": ["V", "V", "S"], "pickaxe": ["VVV", " S ", " S "], "axe": ["VV", "VS", " S"], "shovel": ["V", "S", "S"],
    "hoe": ["VV", " S", " S"], "helmet": ["VVV", "V V"], "chestplate": ["V V", "VVV", "VVV"],
    "leggings": ["VVV", "V V", "V V"], "boots": ["V V", "V V"],
}
for piece, pattern in SHAPES.items():
    key = {"V": V}
    if any("S" in row_ for row_ in pattern):
        key["S"] = STICK
    w(f"{D}/recipe/vahoege_vads_{piece}.json", {"type": "minecraft:crafting_shaped",
        "category": "equipment", "pattern": pattern, "key": key,
        "result": {"id": f"guhs:vahoege_vads_{piece}", "count": 1}})
# the paxel: pickaxe + axe + shovel in one (mines everything those three can)
w(f"{D}/recipe/vahoege_vads_paxel.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
    "ingredients": [{"item": "guhs:vahoege_vads_pickaxe"}, {"item": "guhs:vahoege_vads_axe"}, {"item": "guhs:vahoege_vads_shovel"}],
    "result": {"id": "guhs:vahoege_vads_paxel", "count": 1}})
w(f"{D}/tags/block/mineable/paxel.json", {"values": ["#minecraft:mineable/pickaxe", "#minecraft:mineable/axe",
                                                     "#minecraft:mineable/shovel"]})
GEAR = [f"guhs:vahoege_vads_{p}" for p in list(SHAPES) + ["paxel"]]
w(f"{D}/tags/item/keep_on_death.json", {"values": GEAR})
for tag, members in (("swords", ["sword"]), ("pickaxes", ["pickaxe", "paxel"]), ("axes", ["axe", "paxel"]),
                     ("shovels", ["shovel", "paxel"]),
                     ("hoes", ["hoe"]), ("head_armor", ["helmet"]), ("chest_armor", ["chestplate"]),
                     ("leg_armor", ["leggings"]), ("foot_armor", ["boots"])):
    w(f"{R}/data/minecraft/tags/item/{tag}.json", {"replace": False, "values": [f"guhs:vahoege_vads_{m}" for m in members]})

# --- guh compasses: vanilla compass needle, our recoloured frames ---------------------------------------------------------
for compass in ("guh_cave_compass", "challenge_compass"):
    overrides = []
    for i in range(32):
        frame = f"{compass}_{i:02d}"
        w(f"{A}/models/item/{frame}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{frame}"}})
        overrides.append({"predicate": {"angle": (i - 0.5) / 32 if i else 0.0}, "model": f"guhs:item/{frame}"})
    overrides.append({"predicate": {"angle": 0.984375}, "model": f"guhs:item/{compass}_00"})
    w(f"{A}/models/item/{compass}.json", {"parent": "minecraft:item/generated",
                                           "textures": {"layer0": f"guhs:item/{compass}_16"}, "overrides": overrides})

# --- kaas saus (fluid) ------------------------------------------------------------------------------------------
w(f"{A}/blockstates/kaas_saus.json", {"variants": {"": {"model": "guhs:block/kaas_saus"}}})
w(f"{A}/models/block/kaas_saus.json", {"textures": {"particle": "minecraft:block/water_still"}})
w(f"{A}/models/item/kaas_saus_bucket.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/kaas_saus_bucket"}})

for name, rolls, extra in (
        ("challenging_guh_caves", (3, 6), []),
        ("challenging_guh_caves_treasure", (5, 8), [
            {"type": "minecraft:item", "name": "guhs:netherite_guh_armor", "weight": 2},
            {"type": "minecraft:item", "name": "minecraft:enchanted_golden_apple", "weight": 1},
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 6, "functions": count(3, 8)}])):
    w(f"{D}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": [
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 6, "functions": count(1, 4)},
            {"type": "minecraft:item", "name": "guhs:vahoege_vads", "weight": 8, "functions": count(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 5, "functions": count(1, 3)},
            {"type": "minecraft:item", "name": "guhs:diamond_guh_armor", "weight": 3},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 6, "functions": count(2, 8)},
            {"type": "minecraft:item", "name": "guhs:mika_vet", "weight": 5, "functions": count(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:golden_apple", "weight": 3},
            {"type": "minecraft:item", "name": "minecraft:book", "weight": 4,
             "functions": [{"function": "minecraft:enchant_randomly"}]},
            {"type": "minecraft:item", "name": "guhs:guh_cave_compass", "weight": 2},
            {"type": "minecraft:item", "name": "guhs:challenge_compass", "weight": 2},
        ] + extra}]})

# --- Mika drops Mika's vet ----------------------------------------------------------------------------------------
w(f"{D}/loot_table/entities/mika.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "guhs:mika_vet", "functions": count(1, 2) + [
        {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
         "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}],
    "random_sequence": "guhs:entities/mika"})

# --- frying pan, guh wheel, guh wire, guh spawner: models, blockstates, drops, recipes -------------------------------
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def self_drop(name):
    w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{name}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"guhs:blocks/{name}"})


def box(frm, to, tex, faces=("down", "up", "north", "south", "west", "east"), rotation=None):
    element = {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}
    if rotation:
        element["rotation"] = rotation
    return element


# frying pan: iron pan with a rim and a handle pointing back at you; the oil is an extra part when filled
w(f"{A}/models/block/frying_pan.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "guhs:block/frying_pan_iron", "iron": "guhs:block/frying_pan_iron", "handle": "guhs:block/frying_pan_handle"},
    "elements": [
        box([2, 0, 2], [14, 1, 14], "#iron"),
        box([2, 1, 2], [14, 3, 3], "#iron"), box([2, 1, 13], [14, 3, 14], "#iron"),
        box([2, 1, 3], [3, 3, 13], "#iron"), box([13, 1, 3], [14, 3, 13], "#iron"),
        box([7, 1.5, 14], [9, 2.5, 20], "#handle"),
    ]})
w(f"{A}/models/block/frying_pan_oil.json", {"textures": {"particle": "guhs:block/frying_pan_oil", "oil": "guhs:block/frying_pan_oil"},
    "elements": [box([3, 1, 3], [13, 1.75, 13], "#oil", faces=("up",))]})
w(f"{A}/blockstates/frying_pan.json", {"multipart":
    [{"when": {"facing": f}, "apply": {"model": "guhs:block/frying_pan", "y": r}} for f, r in FACING_Y.items()] +
    [{"when": {"facing": f, "filled": "true"}, "apply": {"model": "guhs:block/frying_pan_oil", "y": r}}
     for f, r in FACING_Y.items()]})
w(f"{A}/models/item/frying_pan.json", {"parent": "guhs:block/frying_pan"})
self_drop("frying_pan")

# guh wheel: purple stand (block model) + coral flower ring (separate model, spun by GuhWheelRenderer)
# the placed wheel is drawn 2x bigger by GuhWheelRenderer (stand + spinning ring); the block model only gives particles
w(f"{A}/models/block/guh_wheel.json", {"textures": {"particle": "guhs:block/guh_wheel_coral"}})
w(f"{A}/blockstates/guh_wheel_part.json", {"multipart": [{"apply": {"model": "guhs:block/guh_wheel"}}]})
w(f"{A}/models/block/guh_wheel_stand.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "guhs:block/guh_wheel_purple", "stand": "guhs:block/guh_wheel_purple"},
    "elements": [
        box([2, 0, 2], [14, 2, 14], "#stand"),
        box([7, 1, 14], [9, 15, 16], "#stand", rotation={"angle": 22.5, "axis": "z", "origin": [8, 14, 15]}),
        box([7, 1, 14], [9, 15, 16], "#stand", rotation={"angle": -22.5, "axis": "z", "origin": [8, 14, 15]}),
        box([7, 13, 12.5], [9, 15, 15], "#stand"),
    ]})
HUB = [8, 14, 8]


def turned(angle):
    return {"angle": angle, "axis": "z", "origin": HUB} if angle else None


RING = []
for angle in (0, 45, -45):
    RING += [
        box([3.8, 22.25, 3.5], [12.2, 24.75, 12.5], "#coral", rotation=turned(angle)),  # top bar
        box([3.8, 3.25, 3.5], [12.2, 5.75, 12.5], "#coral", rotation=turned(angle)),    # bottom bar
        box([6.5, 24.75, 5], [9.5, 26.25, 11], "#coral", rotation=turned(angle)),       # flower scallop, top
        box([6.5, 1.75, 5], [9.5, 3.25, 11], "#coral", rotation=turned(angle)),         # flower scallop, bottom
    ]
RING += [
    box([-2.75, 9.8, 3.5], [-0.25, 18.2, 12.5], "#coral"), box([16.25, 9.8, 3.5], [18.75, 18.2, 12.5], "#coral"),
    box([-4.25, 12.5, 5], [-2.75, 15.5, 11], "#coral"), box([18.75, 12.5, 5], [20.25, 15.5, 11], "#coral"),
    # flower spokes on the back, and the hub
    box([-0.25, 13.25, 12.5], [16.25, 14.75, 13.5], "#coral"),
    box([7.25, 5.75, 12.5], [8.75, 22.25, 13.5], "#coral"),
    box([7.25, 5.75, 12.5], [8.75, 22.25, 13.5], "#coral", rotation=turned(45)),
    box([7.25, 5.75, 12.5], [8.75, 22.25, 13.5], "#coral", rotation=turned(-45)),
    box([6.5, 12.5, 12], [9.5, 15.5, 14], "#stand"),
]
w(f"{A}/models/block/guh_wheel_ring.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "guhs:block/guh_wheel_coral", "coral": "guhs:block/guh_wheel_coral", "stand": "guhs:block/guh_wheel_purple"},
    "elements": RING})
w(f"{A}/blockstates/guh_wheel.json", {"multipart": [{"apply": {"model": "guhs:block/guh_wheel"}}]})
# inventory item: stand + ring together (the stand boxes are repeated here)
STAND = [
    box([2, 0, 2], [14, 2, 14], "#stand"),
    box([7, 1, 14], [9, 15, 16], "#stand", rotation={"angle": 22.5, "axis": "z", "origin": [8, 14, 15]}),
    box([7, 1, 14], [9, 15, 16], "#stand", rotation={"angle": -22.5, "axis": "z", "origin": [8, 14, 15]}),
    box([7, 13, 12.5], [9, 15, 15], "#stand"),
]
w(f"{A}/models/item/guh_wheel.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "guhs:block/guh_wheel_coral", "coral": "guhs:block/guh_wheel_coral", "stand": "guhs:block/guh_wheel_purple"},
    "elements": STAND + RING,
    "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.45, 0.45, 0.45]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.45, 0.45, 0.45]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}})
self_drop("guh_wheel")

# guh wire: uses the vanilla redstone dust models (dot/line/corner/up), tinted pink by GuhsClient's colour handler
SIDE = "side|up"
# our own copies of the dust models, marked "cutout" (vanilla sets that in code for redstone; without it the
# see-through pixels render black)
for part in ("dot", "side0", "side1", "side_alt0", "side_alt1", "up"):
    w(f"{A}/models/block/guh_wire_{part}.json", {"parent": f"minecraft:block/redstone_dust_{part}",
                                                "render_type": "minecraft:cutout"})
w(f"{A}/blockstates/guh_wire.json", {"multipart": [
    {"apply": {"model": "guhs:block/guh_wire_dot"}, "when": {"OR": [
        {"east": "none", "north": "none", "south": "none", "west": "none"},
        {"east": SIDE, "north": SIDE}, {"east": SIDE, "south": SIDE}, {"south": SIDE, "west": SIDE}, {"north": SIDE, "west": SIDE}]}},
    {"apply": {"model": "guhs:block/guh_wire_side0"}, "when": {"north": SIDE}},
    {"apply": {"model": "guhs:block/guh_wire_side_alt0"}, "when": {"south": SIDE}},
    {"apply": {"model": "guhs:block/guh_wire_side_alt1", "y": 270}, "when": {"east": SIDE}},
    {"apply": {"model": "guhs:block/guh_wire_side1", "y": 270}, "when": {"west": SIDE}},
    {"apply": {"model": "guhs:block/guh_wire_up"}, "when": {"north": "up"}},
    {"apply": {"model": "guhs:block/guh_wire_up", "y": 90}, "when": {"east": "up"}},
    {"apply": {"model": "guhs:block/guh_wire_up", "y": 180}, "when": {"south": "up"}},
    {"apply": {"model": "guhs:block/guh_wire_up", "y": 270}, "when": {"west": "up"}},
]})
w(f"{A}/models/item/guh_wire.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/guh_wire"}})
w(f"{A}/models/item/picked_up_guh.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/picked_up_guh"}})
self_drop("guh_wire")

# guh spawner: pink cage (the little spinning guh inside is drawn by GuhSpawnerRenderer)
w(f"{A}/models/block/guh_spawner.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout",
                                         "textures": {"all": "guhs:block/guh_spawner"}})
w(f"{A}/blockstates/guh_spawner.json", {"variants": {"": {"model": "guhs:block/guh_spawner"}}})
w(f"{A}/models/item/guh_spawner.json", {"parent": "guhs:block/guh_spawner"})
self_drop("guh_spawner")

w(f"{R}/data/minecraft/tags/block/mineable/pickaxe.json",
  {"values": ["guhs:kaasknabbel_stone", "guhs:kaasknabbel_deepslate", "guhs:kaasknabbel_cobblestone",
              "guhs:frying_pan", "guhs:guh_wheel", "guhs:guh_spawner",
                         "guhs:compressed_super_vahoege_vads"]})

for item in ("mika_vet", "gefrituurde_kaasknabbels"):
    w(f"{A}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{item}"}})
w(f"{A}/models/item/mika_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})

w(f"{D}/recipe/frying_pan.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["II ", "IIS"], "key": {"I": {"item": "minecraft:iron_ingot"}, "S": {"item": "minecraft:stick"}},
    "result": {"id": "guhs:frying_pan", "count": 1}})
w(f"{D}/recipe/guh_wheel.json", {"type": "minecraft:crafting_shaped", "category": "redstone",
    "pattern": ["PPP", "PKP", "IRI"],
    "key": {"P": {"item": "minecraft:pink_concrete"}, "K": {"item": "guhs:kaas_knabbels"},
            "I": {"item": "minecraft:iron_ingot"}, "R": {"item": "minecraft:redstone"}},
    "result": {"id": "guhs:guh_wheel", "count": 1}})
w(f"{D}/recipe/guh_wire.json", {"type": "minecraft:crafting_shapeless", "category": "redstone",
    "ingredients": [{"item": "minecraft:redstone"}, {"item": "minecraft:pink_dye"}, {"item": "guhs:kaas_knabbels"}],
    "result": {"id": "guhs:guh_wire", "count": 3}})


# --- guh villages: job site blocks, guh clothes, the Mika-mepper, the guh villager type --------------------------------
WORKSTATIONS = {  # block: recipe (3x3 rows, key)
    "knabbelbak": (["KKK", "PBP", "PPP"], {"K": "guhs:kaas_knabbels", "P": "minecraft:pink_wool", "B": "minecraft:composter"}),
    "naaitafel": (["SS", "PP"], {"S": "minecraft:string", "P": "minecraft:pink_wool"}),
    "vadsaambeeld": (["VV", "PP"], {"V": "guhs:vahoege_vads", "P": "minecraft:pink_terracotta"}),
    "buizenbank": (["GG", "PP"], {"G": "minecraft:yellow_stained_glass", "P": "#minecraft:planks"}),
    "mikatrofee": (["M", "B"], {"M": "guhs:mika_vet", "B": "minecraft:polished_blackstone"}),
}
for name, (pattern, key) in WORKSTATIONS.items():
    w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": f"guhs:block/{name}_top", "front": f"guhs:block/{name}_front", "side": f"guhs:block/{name}_side"}})
    w(f"{A}/blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{name}", **({"y": y} if y else {})}
                                                    for f, y in FACING_Y.items()}})
    w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
    self_drop(name)
    w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
        "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()},
        "result": {"id": f"guhs:{name}", "count": 1}})
w(f"{R}/data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": [f"guhs:{n}" for n in WORKSTATIONS]})
# villagers look for job sites in this tag
w(f"{R}/data/minecraft/tags/point_of_interest_type/acquirable_job_site.json", {"replace": False,
                                                                              "values": [f"guhs:{n}" for n in WORKSTATIONS]})
CLOTHES = ["pink_onesie", "striped_sweater", "raincoat", "chef_jacket", "rain_hat", "party_hat", "chef_hat", "red_bowtie", "black_bowtie",
           "sunglasses", "heart_glasses", "monocle", "eyepatch", "firefighter_helmet", "firefighter_jacket", "police_cap", "police_uniform", "doctor_coat", "stethoscope", "builder_helmet", "safety_vest", "straw_hat", "overalls", "santa_hat", "christmas_sweater", "winter_scarf", "sint_mitre", "piet_beret", "witch_hat", "ghost_sheet", "pumpkin_head", "orange_crown", "orange_shirt", "wizard_hat", "wizard_robe", "knight_helmet", "knight_armour", "royal_crown", "royal_cape", "pirate_hat", "guh_backpack", "kermis_hoed", "kermis_jasje", "kermis_strik", "detective_pet", "detective_vergrootglas", "detective_jas", "koning_kroon", "koning_mantel", "koning_ketting"]
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import features  # noqa: E402
for _module in features.modules():  # the 2.4 features' own clothes
    CLOTHES += getattr(_module, "CLOTHES", [])
for item in CLOTHES:
    w(f"{A}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{item}"}})
w(f"{A}/models/item/mika_mepper.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "guhs:item/mika_mepper"}})
w(f"{R}/data/minecraft/tags/item/swords.json", {"replace": False, "values": ["guhs:vahoege_vads_sword", "guhs:mika_mepper"]})
# guh villagers are born in the Guhmension (the villager type follows the biome)
w(f"{R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
  {"values": {f"guhs:{b}": {"villager_type": "guhs:guh"} for b in BIOMES}})


# --- themed guh clothes as extra loot (wild guhs never wear these) ---------------------------------------------------------
THEMED_CLOTHES = {  # loot table: ([clothes], weight of "nothing")
    # 2.9 (kleding): every piece has exactly one source (feature.kleding.KledingBronLijst): the heart glasses, the monocle and
    # the royal crown are Guhdex milestones only, the eyepatch belongs to the caves' pirate loot
    "hamster_house": (["sint_mitre", "piet_beret", "winter_scarf"], 3),
    "evil_mika_home": (["witch_hat", "ghost_sheet", "pumpkin_head"], 3),
    "guh_picnic": (["santa_hat", "christmas_sweater", "orange_crown", "orange_shirt", "party_hat"], 3),
    "challenging_guh_caves": (["pirate_hat", "eyepatch", "knight_helmet"], 4),
    "challenging_guh_caves_treasure": (["knight_armour", "knight_helmet"], 2),
    "guhramid": (["royal_cape", "wizard_hat", "wizard_robe"], 2),
    "guhramid_secret": (["royal_cape"], 1),
}
# 2.9: what the chests got instead of the pieces that moved (a treat, never clothes)
THEMED_EXTRA = {
    "hamster_house": [("guhs:gefrituurde_kaasknabbels", 2, 4)],
    "challenging_guh_caves_treasure": [("guhs:guh_kristal", 6, 12), ("minecraft:gold_ingot", 3, 6)],
    "guhramid": [("minecraft:gold_block", 1, 2), ("guhs:guh_kristal", 8, 16)],
    "guhramid_secret": [("minecraft:diamond", 2, 4), ("guhs:vahoege_vads_ingot", 1, 2)],
}
for table, (pieces, nothing) in THEMED_CLOTHES.items():
    path = f"{D}/loot_table/chests/{table}.json"
    data = json.load(open(path))
    data["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{c}", "weight": 1} for c in pieces]
                          + [{"type": "minecraft:empty", "weight": nothing}]})
    if table in THEMED_EXTRA:
        data["pools"].append({"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": i, "weight": 1, "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
            for i, lo, hi in THEMED_EXTRA[table]]})
    w(path, data)
# the backpack for your guh: 18 extra slots, worn in its wardrobe
w(f"{D}/recipe/guh_backpack.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SLS", "WCW", "WWW"],
    "key": {"S": {"item": "minecraft:string"}, "L": {"item": "minecraft:leather"}, "W": {"item": "minecraft:pink_wool"},
            "C": {"item": "minecraft:chest"}}, "result": {"id": "guhs:guh_backpack", "count": 1}})

# --- advancements: a "Guhmension" tab ------------------------------------------------------------------------------
ENTERED_GUHMENSION = {"trigger": "minecraft:changed_dimension", "conditions": {"to": "guhs:guhmension"}}
RODE_SADDLED_GUH = {"trigger": "minecraft:started_riding", "conditions": {
    "player": {"vehicle": {"type": "guhs:guh", "nbt": "{Saddle:1b}"}}}}
w(f"{D}/advancement/guhmension/root.json", {
    "display": {"icon": {"id": "guhs:block_of_kaasknabbels"},
                "title": {"translate": "advancements.guhs.guhmension.root.title"},
                "description": {"translate": "advancements.guhs.guhmension.root.description"},
                "background": "minecraft:textures/block/pink_wool.png",
                "show_toast": False, "announce_to_chat": False},
    # the tab shows up once you hold a Block of Kaasknabbels (or somehow end up in the Guhmension)
    "criteria": {"has_kaasknabbel_block": {"trigger": "minecraft:inventory_changed",
                                           "conditions": {"items": [{"items": "guhs:block_of_kaasknabbels"}]}},
                 "entered_guhmension": ENTERED_GUHMENSION,
                 "rode_guh": RODE_SADDLED_GUH},
    "requirements": [["has_kaasknabbel_block", "entered_guhmension", "rode_guh"]],
})
w(f"{D}/advancement/guhmension/enter_guhmension.json", {
    "parent": "guhs:guhmension/root",
    "display": {"icon": {"id": "minecraft:pink_wool"},
                "title": {"translate": "advancements.guhs.guhmension.enter.title"},
                "description": {"translate": "advancements.guhs.guhmension.enter.description"},
                "frame": "goal", "show_toast": True, "announce_to_chat": True},
    "criteria": {"entered_guhmension": ENTERED_GUHMENSION},
})

w(f"{D}/advancement/guhmension/ride_guh.json", {
    "parent": "guhs:guhmension/root",
    "display": {"icon": {"id": "minecraft:saddle"},
                "title": {"translate": "advancements.guhs.guhmension.ride.title"},
                "description": {"translate": "advancements.guhs.guhmension.ride.description"},
                "frame": "goal", "show_toast": True, "announce_to_chat": True},
    "criteria": {"rode_guh": RODE_SADDLED_GUH},
})

def found(structures):
    """Criterion: standing inside one of these structures."""
    return {"trigger": "minecraft:location", "conditions": {"player": {"location": {
        "structures": [f"guhs:{st}" for st in structures]}}}}


# name: (parent, icon, frame, criterion, English title, English description, Dutch title, Dutch description)
ADVANCEMENTS = {
    "find_hamster_house": ("enter_guhmension", "guhs:guh_spawner", "task",
                           found(["hamster_house", "hamster_house_medium", "hamster_house_large"]),
                           "Home Sweet Hamster Home", "Find a hamster house",
                           "Huisje, boompje, hamster", "Vind een hamsterhuis"),
    "find_guhland": ("find_hamster_house", "guhs:guh_wheel", "challenge", found(["hamster_house_extra_extra_large"]),
                     "Welcome to Guhland!", "Find the very rare guh theme park",
                     "Welkom in Guhland!", "Vind het zeer zeldzame guh-pretpark"),
    "find_guhramid": ("enter_guhmension", "minecraft:pink_glazed_terracotta", "challenge", found(["guhramid"]),
                      "Walk Like a Guhgyptian", "Find the very rare guhramid",
                      "Loop als een Guhgypter", "Vind de zeer zeldzame guhramide"),
    "find_guh_village": ("enter_guhmension", "guhs:knabbelbak", "goal", found(["guh_village"]),
                         "Won't You Be My Neighbour?", "Find a guh village", "Buurguhs!", "Vind een guhdorp"),
    "find_guh_statue": ("enter_guhmension", "minecraft:purpur_block", "goal", found(["guh_statue"]),
                        "Set in Stone", "Find the giant guh statue",
                        "In steen gebeiteld", "Vind het reuzenguhstandbeeld"),
    "find_cheese_fountain": ("enter_guhmension", "guhs:kaas_saus_bucket", "task", found(["cheese_fountain"]),
                             "Say Cheese!", "Find a cheese fountain",
                             "Zeg eens kaas!", "Vind een kaasfontein"),
    "find_grand_cheese_fountain": ("find_cheese_fountain", "guhs:compressed_super_vahoege_vads", "challenge",
                                   found(["grand_cheese_fountain"]),
                                   "The Grandest Saus", "Find the rare grand cheese fountain",
                                   "De allergrootste saus", "Vind de zeldzame grote kaasfontein"),
    "find_evil_mika_home": ("enter_guhmension", "guhs:mika_vet", "task", found(["evil_mika_home"]),
                            "Mika's Lair", "Find an Evil Mika home",
                            "Het hol van Mika", "Vind een huis van Boze Mika"),
    "find_guh_caves": ("enter_guhmension", "guhs:guh_cave_compass", "task", found(["guh_caves"]),
                       "Down the Hamster Tube", "Find the guh caves",
                       "Door de hamsterbuis", "Vind de guhgrotten"),
    "find_challenging_guh_caves": ("find_guh_caves", "guhs:challenge_compass", "goal", found(["challenging_guh_caves"]),
                                   "Something Smells Evil", "Find a challenging guh cave",
                                   "Hier ruikt iets kwaads", "Vind een uitdagende guhgrot"),
    "defeat_big_mika": ("find_challenging_guh_caves", "guhs:mika_vet", "challenge",
                        {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {
                            "type": "guhs:mika", "nbt": "{Boss:1b}"}}},
                        "Big Mika, Bigger Fall", "Defeat Big Mika in a challenging guh cave",
                        "Hoe groter de Mika...", "Versla Grote Mika in een uitdagende guhgrot"),
    "feed_hungry_guh": ("enter_guhmension", "guhs:gefrituurde_kaasknabbels", "goal",
                        {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:bank_guh"}]}},
                        "Vadsig Fed", "Give a Hungry Guh 10 gefrituurde kaasknabbels",
                        "Vadsig gevoerd", "Geef een Hongerige Guh 10 gefrituurde kaasknabbels"),
    "get_vads_ingot": ("enter_guhmension", "guhs:vahoege_vads_ingot", "task",
                       {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:vahoege_vads_ingot"}]}},
                       "Super Vahoege!", "Smelt a Vahoege Vads ingot",
                       "Super vahoege!", "Smelt een vahoege-vadsstaaf"),
}
for name, (parent, icon, frame, crit, *_texts) in ADVANCEMENTS.items():
    w(f"{D}/advancement/guhmension/{name}.json", {
        "parent": f"guhs:guhmension/{parent}",
        "display": {"icon": {"id": icon},
                    "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                    "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                    "frame": frame, "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": crit},
    })
ADV_LANG_EN = {}
ADV_LANG_NL = {}
for name, (_p, _i, _f, _c, en_t, en_d, nl_t, nl_d) in ADVANCEMENTS.items():
    ADV_LANG_EN[f"advancements.guhs.guhmension.{name}.title"] = en_t
    ADV_LANG_EN[f"advancements.guhs.guhmension.{name}.description"] = en_d
    ADV_LANG_NL[f"advancements.guhs.guhmension.{name}.title"] = nl_t
    ADV_LANG_NL[f"advancements.guhs.guhmension.{name}.description"] = nl_d

# --- lang ------------------------------------------------------------------------------------------------------
COMMON = {
    "entity.guhs.guh": "Guh",
    # 1.1.4: the invisible seat you sit on in guh chairs, sofas and sleds (never really shown, but never a raw key either)
    "entity.guhs.guh_seat": "Guhzitplekje",
    "item.guhs.kaas_knabbels": "Kaas Knabbels",
    "itemGroup.guhs": "Guhs",
    "gui.guhs.menu.hp": "HP: %s / %s",
}
# (1.2.0: this old English is only a draft from before 2.0; make_v2.write_lang() makes the real en_us from nl_nl plus the
#  English overlay in tools/lang/en/*.json, so nothing below ends up in the game)
w(f"{A}/lang/en_us.json", {
    **COMMON,
    "item.guhs.guh_spawn_egg": "Guh Spawn Egg",
    "block.guhs.kaasknabbel_stone": "Kaasknabbel Stone",
    "block.guhs.kaasknabbel_deepslate": "Kaasknabbel Deepslate",
    "block.guhs.kaasknabbel_dirt": "Kaasknabbel Dirt",
    "block.guhs.kaasknabbel_cobblestone": "Kaasknabbel Cobblestone",
    "block.guhs.block_of_kaasknabbels": "Block of Kaasknabbels",
    "block.guhs.guh_portal": "Guh Portal",
    "biome.guhs.guh_fields": "Guh Fields",
    "advancements.guhs.guhmension.root.title": "Guhmension",
    "advancements.guhs.guhmension.root.description": "Kaasknabbels, pink wool and lots of guhs",
    "advancements.guhs.guhmension.enter.title": "Welcome to vads dimension!",
    "advancements.guhs.guhmension.enter.description": "Step through a guh portal into the Guhmension",
    "advancements.guhs.guhmension.ride.title": "Giddy-up, Guh!",
    "advancements.guhs.guhmension.ride.description": "Ride a saddled guh",
    **ADV_LANG_EN,
    "entity.guhs.mika": "Mika",
    "entity.guhs.big_mika": "Big Mika",
    "item.guhs.sunglasses": "Guh Sunglasses",
    "item.guhs.heart_glasses": "Heart Glasses",
    "item.guhs.monocle": "Guh Monocle",
    "item.guhs.eyepatch": "Pirate Eyepatch",
    "item.guhs.firefighter_helmet": "Firefighter Helmet",
    "item.guhs.firefighter_jacket": "Firefighter Jacket",
    "item.guhs.police_cap": "Police Cap",
    "item.guhs.police_uniform": "Police Uniform",
    "item.guhs.doctor_coat": "Doctor's Coat",
    "item.guhs.stethoscope": "Stethoscope",
    "item.guhs.builder_helmet": "Builder's Helmet",
    "item.guhs.safety_vest": "Safety Vest",
    "item.guhs.straw_hat": "Straw Hat",
    "item.guhs.overalls": "Overalls",
    "item.guhs.santa_hat": "Christmas Hat",
    "item.guhs.christmas_sweater": "Christmas Sweater",
    "item.guhs.winter_scarf": "Winter Scarf",
    "item.guhs.sint_mitre": "Sint's Mitre",
    "item.guhs.piet_beret": "Piet's Beret",
    "item.guhs.witch_hat": "Witch Hat",
    "item.guhs.ghost_sheet": "Ghost Sheet",
    "item.guhs.pumpkin_head": "Pumpkin Head",
    "item.guhs.orange_crown": "King's Day Crown",
    "item.guhs.orange_shirt": "Orange Shirt",
    "item.guhs.wizard_hat": "Wizard Hat",
    "item.guhs.wizard_robe": "Wizard Robe",
    "item.guhs.knight_helmet": "Knight Helmet",
    "item.guhs.knight_armour": "Knight Armour",
    "item.guhs.royal_crown": "Royal Crown",
    "item.guhs.royal_cape": "Royal Cape",
    "item.guhs.pirate_hat": "Pirate Hat",
    "item.guhs.guh_backpack": "Guh Backpack",
    "gui.guhs.menu.wardrobe": "Clothes & backpack",
    "gui.guhs.wardrobe.title": "%s's wardrobe",
    "gui.guhs.wardrobe.backpack": "Backpack",
    "gui.guhs.wardrobe.no_backpack": "Backpack (put one on first)",
    "gui.guhs.menu.clothes.eyes": "Eyes",
    "gui.guhs.menu.clothes.back": "Back",
    "gui.guhs.launch.cooldown": "Your guh is still catching its breath (%s s)",
    "entity.guhs.guh.rainbow": "Rainbow Guh",
    "entity.guhs.guh.starry": "Starry Guh",
    "entity.guhs.guh.ghost": "Ghost Guh",
    "entity.guhs.guh.teckel": "Teckel Guh",
    "block.guhs.knabbelbak": "Knabbelbak",
    "block.guhs.naaitafel": "Guh Sewing Table",
    "block.guhs.vadsaambeeld": "Vads Anvil",
    "block.guhs.buizenbank": "Tube Workbench",
    "block.guhs.mikatrofee": "Mika Trophy",
    "item.guhs.pink_onesie": "Pink Guh Onesie",
    "item.guhs.striped_sweater": "Striped Guh Sweater",
    "item.guhs.raincoat": "Guh Raincoat",
    "item.guhs.chef_jacket": "Guh Chef Jacket",
    "item.guhs.rain_hat": "Guh Sou'wester",
    "item.guhs.party_hat": "Guh Party Hat",
    "item.guhs.chef_hat": "Guh Chef Hat",
    "item.guhs.red_bowtie": "Red Guh Bow Tie",
    "item.guhs.black_bowtie": "Black Guh Bow Tie",
    "item.guhs.guh_clothes.lore": "Right-click your tamed guh to dress it up",
    "item.guhs.mika_mepper": "Mika-mepper",
    "item.guhs.mika_mepper.lore": "Hits Mikas three times as hard",
    "entity.minecraft.villager.guhs.vads_temmer": "Vads Temmer",
    "entity.minecraft.villager.guhs.guh_kleermaker": "Guh Kleermaker",
    "entity.minecraft.villager.guhs.vadssmid": "Vadssmid",
    "entity.minecraft.villager.guhs.hamsterbouwer": "Hamsterbouwer",
    "entity.minecraft.villager.guhs.mika_jager": "Mika-jager",
    "gui.guhs.menu.clothes": "Clothes",
    "gui.guhs.menu.clothes.head": "Head",
    "gui.guhs.menu.clothes.body": "Body",
    "gui.guhs.menu.clothes.neck": "Neck",
    "gui.guhs.menu.clothes.head.short": "hat",
    "gui.guhs.menu.clothes.body.short": "suit",
    "gui.guhs.menu.clothes.neck.short": "tie",
    "gui.guhs.menu.clothes.empty": "Nothing",
    "gui.guhs.menu.clothes.help": "Left-click: put on the next piece from your inventory - Right-click: take off",
    "gui.guhs.menu.remove_clothes": "Take off clothes",
    "entity.guhs.guh.mummy": "Mummy Guh",
    "gui.guhs.menu.personality": "Personality: %s",
    "gui.guhs.menu.personality.tooltip": "Every guh has one of these personalities:",
    "gui.guhs.personality.playful": "Playful",
    "gui.guhs.personality.playful.description": "Now and then it does happy zoomies. A bit faster than other guhs.",
    "gui.guhs.personality.lazy": "Lazy",
    "gui.guhs.personality.lazy.description": "Slower, and would rather just lie around.",
    "gui.guhs.personality.vadsig": "Vadsig",
    "gui.guhs.personality.vadsig.description": "Sniffs out kaas knabbels on the ground and eats them. Easier to tame.",
    "gui.guhs.personality.shy": "Shy",
    "gui.guhs.personality.shy.description": "Wild: keeps away from you unless you hold kaas knabbels. Harder to tame.",
    "gui.guhs.personality.brave": "Brave",
    "gui.guhs.personality.brave.description": "Hits harder and never runs away.",
    "gui.guhs.personality.chatty": "Chatty",
    "gui.guhs.personality.chatty.description": "Makes guh noises twice as often.",
    "gui.guhs.personality.cuddly": "Cuddly",
    "gui.guhs.personality.cuddly.description": "Heals its owner half a heart now and then when close by.",
    "gui.guhs.personality.curious": "Curious",
    "gui.guhs.personality.curious.description": "Walks up to nearby players to have a good look.",
    "entity.guhs.guh.mint": "Mint Guh",
    "entity.guhs.guh.choco": "Choco Guh",
    "entity.guhs.guh.snow": "Snow Guh",
    "entity.guhs.guh.sweater": "Sweater Guh",
    "entity.guhs.guh.rain": "Rain Guh",
    "entity.guhs.guh.party": "Party Guh",
    "entity.guhs.guh.chef": "Chef Guh",
    "entity.guhs.guh.brontosaurus": "Brontosaurus Guh",
    "entity.guhs.guh.golden": "Golden Guh",
    "entity.guhs.guh.brococolief": "brococolief",
    "item.guhs.secret_note": "Secret Note",
    "item.guhs.mika_spawn_egg": "Mika Spawn Egg",
    "item.guhs.mika_vet": "Mika's Vet",
    "item.guhs.gefrituurde_kaasknabbels": "Gefrituurde Kaasknabbels",
    "block.guhs.frying_pan": "Guh Frying Pan",
    "block.guhs.frying_pan.charges": "Fat left for %s / %s kaas knabbels",
    "block.guhs.frying_pan.no_fat": "The pan needs Mika's vet first",
    "block.guhs.frying_pan.full": "The pan is full of fat",
    "block.guhs.guh_wheel": "Guh Wheel",
    "block.guhs.guh_wheel.no_guh": "Pick up your tamed guh (sneak + right-click it) and right-click the wheel with it",
    "block.guhs.guh_wheel.not_yours": "That is not your guh",
    "block.guhs.guh_wire": "Guh Wire",
    "block.guhs.guh_spawner": "Guh Spawner",
    "biome.guhs.knabbel_crumbs": "Knabbel Crumbs",
    "biome.guhs.pink_puffs": "Pink Puffs",
    "biome.guhs.kaas_flats": "Kaas Flats",
    "biome.guhs.guh_meadows": "Guh Meadows",
    "biome.guhs.guh_peaks": "Guh Peaks",
    "biome.guhs.vads_cliffs": "Vads Cliffs",
    "biome.guhs.mikas_biome": "Mika's Biome",
    "block.guhs.bank_guh": "Bank Guh",
    "block.guhs.compressed_super_vahoege_vads": "Compressed Super Vahoege Vads",
    "item.guhs.vahoege_vads": "Vahoege Vads",
    "item.guhs.vahoege_vads_ingot": "Vahoege Vads Ingot",
    "item.guhs.vahoege_vads_sword": "Vahoege Vads Sword",
    "item.guhs.vahoege_vads_pickaxe": "Vahoege Vads Pickaxe",
    "item.guhs.vahoege_vads_axe": "Vahoege Vads Axe",
    "item.guhs.vahoege_vads_shovel": "Vahoege Vads Shovel",
    "item.guhs.vahoege_vads_hoe": "Vahoege Vads Hoe",
    "item.guhs.vahoege_vads_paxel": "Vahoege Vads Paxel",
    "item.guhs.vahoege_vads_helmet": "Vahoege Vads Helmet",
    "item.guhs.vahoege_vads_chestplate": "Vahoege Vads Chestplate",
    "item.guhs.vahoege_vads_leggings": "Vahoege Vads Leggings",
    "item.guhs.vahoege_vads_boots": "Vahoege Vads Boots",
    "item.guhs.guh_cave_compass": "Guh Cave Compass",
    "item.guhs.guh_cave_compass.lore": "Points to the central room of the nearest guh cave",
    "item.guhs.challenge_compass": "Challenge Compass",
    "item.guhs.challenge_compass.lore": "Points to the dungeon hall of the nearest challenging guh cave",
    "item.guhs.guh_compass.only_guhmension": "Only works in the Guhmension",
    "item.guhs.iron_guh_armor": "Iron Guh Armour",
    "item.guhs.diamond_guh_armor": "Diamond Guh Armour",
    "item.guhs.netherite_guh_armor": "Netherite Guh Armour",
    "gui.guhs.menu.ride.yes": "Rideable - tap right-click to hop on",
    "gui.guhs.menu.ride.saddle": "Big enough to ride - needs a saddle",
    "gui.guhs.menu.ride.small": "Too small to ride (needs %s blocks)",
    "gui.guhs.menu.ride.baby": "Too young to ride",
    "gui.guhs.menu.behavior": "Behaviour: %s",
    "gui.guhs.menu.behavior.tooltip": "Passive (flee): runs away when a mob hurts it. Passive: does nothing. Neutral: fights back. Aggressive: attacks monsters and Mikas within the radius, never sweet critters.",
    "gui.guhs.behavior.passive_flee": "Passive (flee)",
    "gui.guhs.behavior.passive": "Passive",
    "gui.guhs.behavior.neutral": "Neutral",
    "gui.guhs.behavior.aggressive": "Aggressive",
    "gui.guhs.menu.radius": "Attack radius: %s blocks",
    "gui.guhs.menu.name": "Name",
    "gui.guhs.menu.rename": "Rename",
    "gui.guhs.menu.remove_armor": "Take off armour",
    "gui.guhs.menu.sounds": "Sounds...",
    "gui.guhs.menu.sounds.title": "Sounds of %s",
    "gui.guhs.menu.sounds.ambient": "Guh noises: %s",
    "gui.guhs.menu.sounds.frequency": "How often: %s",
    "gui.guhs.menu.sounds.frequency.0": "Very rarely",
    "gui.guhs.menu.sounds.frequency.1": "Rarely",
    "gui.guhs.menu.sounds.frequency.2": "Normal",
    "gui.guhs.menu.sounds.frequency.3": "Often",
    "gui.guhs.menu.sounds.frequency.4": "Very often",
    "block.guhs.kaas_saus": "Kaas Saus",
    "fluid_type.guhs.kaas_saus": "Kaas Saus",
    "item.guhs.kaas_saus_bucket": "Bucket of Kaas Saus",
    "block.guhs.guh_wheel_part": "Guh Wheel",
    "block.guhs.cheese_fountain": "Cheese Fountain",
    "item.guhs.picked_up_guh": "Picked-up Guh",
    "item.guhs.picked_up_guh.named": "%s (picked up)",
    "item.guhs.picked_up_guh.hint": "Right-click a block to put it down, or a Guh Wheel to let it run",
    "block.guhs.guh_wheel.occupied": "There is already a guh running in this wheel",
    "block.guhs.bank_guh.lore": "So vadsig it can store infinite items in its stomach",
    "block.guhs.bank_guh.contents": "%s items (%s kinds) in its stomach",
    "entity.guhs.quest_guh": "Hungry Guh",
    "entity.guhs.quest_guh.request": "Guh guh... I am sooo hungry! Bring me %s gefrituurde kaasknabbels (hold them in your hand) and I will give you something very vadsig. (You are holding %s.)",
    "entity.guhs.quest_guh.thanks": "Nom nom nom! Thank you! Here, take my friend the Bank Guh - it can eat anything and never gets full!",
    "item.guhs.quest_guh_spawn_egg": "Hungry Guh Spawn Egg",
    "gui.guhs.bank.search": "Search... (@mod)",
    "gui.guhs.bank.summary": "%s items, %s kinds",
    "gui.guhs.bank.stored": "In the stomach: %s",
    "gui.guhs.bank.deposit": "Deposit all",
    "gui.guhs.bank.deposit.tooltip": "Put everything from your inventory (not your hotbar) in the stomach",
    "gui.guhs.bank.clear_grid": "Clear grid",
    "gui.guhs.bank.sort.name": "Name",
    "gui.guhs.bank.sort.count": "Count",
    "gui.guhs.bank.sort.mod": "Mod",
    "gui.guhs.bank.sort.tooltip": "Sort by",
    "gui.guhs.bank.filter.all": "All",
    "gui.guhs.bank.filter.blocks": "Blk",
    "gui.guhs.bank.filter.tools": "Tool",
    "gui.guhs.bank.filter.food": "Food",
    "gui.guhs.bank.filter.other": "Misc",
    "gui.guhs.bank.filter.tooltip": "Show: all / blocks / tools & weapons / food / other",
    "gui.guhs.menu.sit": "Sit",
    "gui.guhs.menu.stand": "Stand up",
    "gui.guhs.menu.teleport": "Teleport to me: %s",
    "gui.guhs.menu.gravity": "Gravity: %s",
    "gui.guhs.menu.size": "%s blocks",
    "subtitles.guhs.entity.guh.ambient": "Guh guhs",
    "subtitles.guhs.entity.guh.hurt": "Guh hurts",
    "subtitles.guhs.entity.guh.death": "Guh deflates",
    "subtitles.guhs.entity.guh.eat": "Guh munches",
    "subtitles.guhs.entity.guh.happy": "Guh is happy",
    "subtitles.guhs.entity.mika.ambient": "Mika snarls",
    "subtitles.guhs.entity.mika.hurt": "Mika hurts",
    "subtitles.guhs.entity.mika.death": "Mika deflates evilly",
})
w(f"{A}/lang/nl_nl.json", {
    **COMMON,
    "item.guhs.guh_spawn_egg": "Guh-spawnei",
    "block.guhs.kaasknabbel_stone": "Kaasknabbelsteen",
    "block.guhs.kaasknabbel_deepslate": "Kaasknabbel-diepsteen",
    "block.guhs.kaasknabbel_dirt": "Kaasknabbelaarde",
    "block.guhs.kaasknabbel_cobblestone": "Kaasknabbelkeien",
    "block.guhs.block_of_kaasknabbels": "Blok kaasknabbels",
    "block.guhs.guh_portal": "Guhportaal",
    "biome.guhs.guh_fields": "Guhvelden",
    "advancements.guhs.guhmension.root.title": "Guhmensie",
    "advancements.guhs.guhmension.root.description": "Kaasknabbels, roze wol en heel veel guhs",
    "advancements.guhs.guhmension.enter.title": "Welcome to vads dimension!",
    "advancements.guhs.guhmension.enter.description": "Stap door een guhportaal de Guhmensie in",
    "advancements.guhs.guhmension.ride.title": "Hop hop, Guh!",
    "advancements.guhs.guhmension.ride.description": "Rijd op een guh met zadel",
    **ADV_LANG_NL,
    "entity.guhs.mika": "Mika",
    "entity.guhs.big_mika": "Grote Mika",
    "item.guhs.sunglasses": "Guhzonnebril",
    "item.guhs.heart_glasses": "Hartjesbril",
    "item.guhs.monocle": "Guhmonocle",
    "item.guhs.eyepatch": "Piratenooglapje",
    "item.guhs.firefighter_helmet": "Brandweerhelm",
    "item.guhs.firefighter_jacket": "Brandweerjas",
    "item.guhs.police_cap": "Politiepet",
    "item.guhs.police_uniform": "Politie-uniform",
    "item.guhs.doctor_coat": "Doktersjas",
    "item.guhs.stethoscope": "Stethoscoop",
    "item.guhs.builder_helmet": "Bouwhelm",
    "item.guhs.safety_vest": "Veiligheidshesje",
    "item.guhs.straw_hat": "Strohoed",
    "item.guhs.overalls": "Tuinbroek",
    "item.guhs.santa_hat": "Kerstmuts",
    "item.guhs.christmas_sweater": "Kersttrui",
    "item.guhs.winter_scarf": "Wintersjaal",
    "item.guhs.sint_mitre": "Sinterklaasmijter",
    "item.guhs.piet_beret": "Pietenmuts",
    "item.guhs.witch_hat": "Heksenhoed",
    "item.guhs.ghost_sheet": "Spooklaken",
    "item.guhs.pumpkin_head": "Pompoenhoofd",
    "item.guhs.orange_crown": "Koningsdagkroontje",
    "item.guhs.orange_shirt": "Oranje shirt",
    "item.guhs.wizard_hat": "Tovenaarshoed",
    "item.guhs.wizard_robe": "Tovenaarsmantel",
    "item.guhs.knight_helmet": "Ridderhelm",
    "item.guhs.knight_armour": "Riddersharnas",
    "item.guhs.royal_crown": "Koningskroon",
    "item.guhs.royal_cape": "Koningsmantel",
    "item.guhs.pirate_hat": "Piratenhoed",
    "item.guhs.guh_backpack": "Guhrugzak",
    "gui.guhs.menu.wardrobe": "Kleding & rugzak",
    "gui.guhs.wardrobe.title": "Kledingkast van %s",
    "gui.guhs.wardrobe.backpack": "Rugzak",
    "gui.guhs.wardrobe.no_backpack": "Rugzak (doe er eerst een om)",
    "gui.guhs.menu.clothes.eyes": "Ogen",
    "gui.guhs.menu.clothes.back": "Rug",
    "gui.guhs.launch.cooldown": "Je guh is nog op adem aan het komen (%s s)",
    "entity.guhs.guh.rainbow": "Regenboogguh",
    "entity.guhs.guh.starry": "Sterrenhemelguh",
    "entity.guhs.guh.ghost": "Spookguh",
    "entity.guhs.guh.teckel": "Teckelguh",
    "block.guhs.knabbelbak": "Knabbelbak",
    "block.guhs.naaitafel": "Guhnaaitafel",
    "block.guhs.vadsaambeeld": "Vadsaambeeld",
    "block.guhs.buizenbank": "Buizenwerkbank",
    "block.guhs.mikatrofee": "Mikatrofee",
    "item.guhs.pink_onesie": "Roze guhonesie",
    "item.guhs.striped_sweater": "Gestreepte guhtrui",
    "item.guhs.raincoat": "Guhregenjas",
    "item.guhs.chef_jacket": "Guhkoksbuis",
    "item.guhs.rain_hat": "Guhzuidwester",
    "item.guhs.party_hat": "Guhfeesthoedje",
    "item.guhs.chef_hat": "Guhkoksmuts",
    "item.guhs.red_bowtie": "Rode guhstrik",
    "item.guhs.black_bowtie": "Zwarte guhstrik",
    "item.guhs.guh_clothes.lore": "Rechtsklik op je tamme guh om hem aan te kleden",
    "item.guhs.mika_mepper": "Mika-mepper",
    "item.guhs.mika_mepper.lore": "Slaat Mika's drie keer zo hard",
    "entity.minecraft.villager.guhs.vads_temmer": "Vadstemmer",
    "entity.minecraft.villager.guhs.guh_kleermaker": "Guhkleermaker",
    "entity.minecraft.villager.guhs.vadssmid": "Vadssmid",
    "entity.minecraft.villager.guhs.hamsterbouwer": "Hamsterbouwer",
    "entity.minecraft.villager.guhs.mika_jager": "Mika-jager",
    "gui.guhs.menu.clothes": "Kleertjes",
    "gui.guhs.menu.clothes.head": "Hoofd",
    "gui.guhs.menu.clothes.body": "Lijf",
    "gui.guhs.menu.clothes.neck": "Nek",
    "gui.guhs.menu.clothes.head.short": "hoed",
    "gui.guhs.menu.clothes.body.short": "pak",
    "gui.guhs.menu.clothes.neck.short": "strik",
    "gui.guhs.menu.clothes.empty": "Niets",
    "gui.guhs.menu.clothes.help": "Linksklik: volgende stuk uit je inventaris aantrekken - Rechtsklik: uittrekken",
    "gui.guhs.menu.remove_clothes": "Kleertjes uit",
    "entity.guhs.guh.mummy": "Mummieguh",
    "gui.guhs.menu.personality": "Karakter: %s",
    "gui.guhs.menu.personality.tooltip": "Elke guh heeft een van deze karakters:",
    "gui.guhs.personality.playful": "Speels",
    "gui.guhs.personality.playful.description": "Doet af en toe blije rondjes. Een beetje sneller dan andere guhs.",
    "gui.guhs.personality.lazy": "Lui",
    "gui.guhs.personality.lazy.description": "Langzamer, en ligt liever gewoon wat rond.",
    "gui.guhs.personality.vadsig": "Vadsig",
    "gui.guhs.personality.vadsig.description": "Snuffelt kaasknabbels op de grond op en eet ze. Makkelijker te temmen.",
    "gui.guhs.personality.shy": "Verlegen",
    "gui.guhs.personality.shy.description": "Wild: blijft uit je buurt, tenzij je kaasknabbels vasthoudt. Moeilijker te temmen.",
    "gui.guhs.personality.brave": "Dapper",
    "gui.guhs.personality.brave.description": "Slaat harder en rent nooit weg.",
    "gui.guhs.personality.chatty": "Kletskous",
    "gui.guhs.personality.chatty.description": "Maakt twee keer zo vaak guhgeluidjes.",
    "gui.guhs.personality.cuddly": "Knuffelig",
    "gui.guhs.personality.cuddly.description": "Geneest zijn baasje af en toe een half hartje als die dichtbij is.",
    "gui.guhs.personality.curious": "Nieuwsgierig",
    "gui.guhs.personality.curious.description": "Loopt naar spelers in de buurt om ze goed te bekijken.",
    "entity.guhs.guh.mint": "Muntguh",
    "entity.guhs.guh.choco": "Chocoguh",
    "entity.guhs.guh.snow": "Sneeuwguh",
    "entity.guhs.guh.sweater": "Truiguh",
    "entity.guhs.guh.rain": "Regenguh",
    "entity.guhs.guh.party": "Feestguh",
    "entity.guhs.guh.chef": "Chef-guh",
    "entity.guhs.guh.brontosaurus": "Brontosaurusguh",
    "entity.guhs.guh.golden": "Gouden Guh",
    "entity.guhs.guh.brococolief": "brococolief",
    "item.guhs.secret_note": "Geheim briefje",
    "item.guhs.mika_spawn_egg": "Mika-spawnei",
    "item.guhs.mika_vet": "Mika's vet",
    "item.guhs.gefrituurde_kaasknabbels": "Gefrituurde kaasknabbels",
    "block.guhs.frying_pan": "Guh-koekenpan",
    "block.guhs.frying_pan.charges": "Vet over voor %s / %s kaasknabbels",
    "block.guhs.frying_pan.no_fat": "De pan heeft eerst Mika's vet nodig",
    "block.guhs.frying_pan.full": "De pan zit vol vet",
    "block.guhs.guh_wheel": "Guhrad",
    "block.guhs.guh_wheel.no_guh": "Pak je tamme guh op (sluipen + rechtsklik) en rechtsklik dan op het rad",
    "block.guhs.guh_wheel.not_yours": "Dat is niet jouw guh",
    "block.guhs.guh_wire": "Guhdraad",
    "block.guhs.guh_spawner": "Guhspawner",
    "biome.guhs.knabbel_crumbs": "Knabbelkruimels",
    "biome.guhs.pink_puffs": "Roze pluisjes",
    "biome.guhs.kaas_flats": "Kaasvlakte",
    "biome.guhs.guh_meadows": "Guhweides",
    "biome.guhs.guh_peaks": "Guhpieken",
    "biome.guhs.vads_cliffs": "Vadskliffen",
    "biome.guhs.mikas_biome": "Mika's bioom",
    "block.guhs.bank_guh": "Bankguh",
    "block.guhs.compressed_super_vahoege_vads": "Samengeperste super vahoege vads",
    "item.guhs.vahoege_vads": "Vahoege vads",
    "item.guhs.vahoege_vads_ingot": "Vahoege-vadsstaaf",
    "item.guhs.vahoege_vads_sword": "Vahoege-vadszwaard",
    "item.guhs.vahoege_vads_pickaxe": "Vahoege-vadshouweel",
    "item.guhs.vahoege_vads_axe": "Vahoege-vadsbijl",
    "item.guhs.vahoege_vads_shovel": "Vahoege-vadsschep",
    "item.guhs.vahoege_vads_hoe": "Vahoege-vadsschoffel",
    "item.guhs.vahoege_vads_paxel": "Vahoege-vadspaxel",
    "item.guhs.vahoege_vads_helmet": "Vahoege-vadshelm",
    "item.guhs.vahoege_vads_chestplate": "Vahoege-vadsborstplaat",
    "item.guhs.vahoege_vads_leggings": "Vahoege-vadsbeenstukken",
    "item.guhs.vahoege_vads_boots": "Vahoege-vadslaarzen",
    "item.guhs.guh_cave_compass": "Guhgrotkompas",
    "item.guhs.guh_cave_compass.lore": "Wijst naar de middenkamer van de dichtstbijzijnde guhgrot",
    "item.guhs.challenge_compass": "Uitdagingskompas",
    "item.guhs.challenge_compass.lore": "Wijst naar de kerkerhal van de dichtstbijzijnde uitdagende guhgrot",
    "item.guhs.guh_compass.only_guhmension": "Werkt alleen in de Guhmensie",
    "item.guhs.iron_guh_armor": "IJzeren guhpantser",
    "item.guhs.diamond_guh_armor": "Diamanten guhpantser",
    "item.guhs.netherite_guh_armor": "Netherieten guhpantser",
    "gui.guhs.menu.ride.yes": "Berijdbaar - tik rechtsklik om op te stappen",
    "gui.guhs.menu.ride.saddle": "Groot genoeg om te berijden - heeft een zadel nodig",
    "gui.guhs.menu.ride.small": "Te klein om te berijden (moet %s blokken zijn)",
    "gui.guhs.menu.ride.baby": "Te jong om te berijden",
    "gui.guhs.menu.behavior": "Gedrag: %s",
    "gui.guhs.menu.behavior.tooltip": "Passief (vluchten): rent weg als een mob hem pijn doet. Passief: doet niks. Neutraal: vecht terug. Agressief: valt monsters en Mika's binnen de straal aan, nooit lieve diertjes.",
    "gui.guhs.behavior.passive_flee": "Passief (vluchten)",
    "gui.guhs.behavior.passive": "Passief",
    "gui.guhs.behavior.neutral": "Neutraal",
    "gui.guhs.behavior.aggressive": "Agressief",
    "gui.guhs.menu.radius": "Aanvalsstraal: %s blokken",
    "gui.guhs.menu.name": "Naam",
    "gui.guhs.menu.rename": "Hernoem",
    "gui.guhs.menu.remove_armor": "Pantser uitdoen",
    "gui.guhs.menu.sounds": "Geluiden...",
    "gui.guhs.menu.sounds.title": "Geluiden van %s",
    "gui.guhs.menu.sounds.ambient": "Guhgeluidjes: %s",
    "gui.guhs.menu.sounds.frequency": "Hoe vaak: %s",
    "gui.guhs.menu.sounds.frequency.0": "Heel zelden",
    "gui.guhs.menu.sounds.frequency.1": "Zelden",
    "gui.guhs.menu.sounds.frequency.2": "Normaal",
    "gui.guhs.menu.sounds.frequency.3": "Vaak",
    "gui.guhs.menu.sounds.frequency.4": "Heel vaak",
    "block.guhs.kaas_saus": "Kaassaus",
    "fluid_type.guhs.kaas_saus": "Kaassaus",
    "item.guhs.kaas_saus_bucket": "Emmer kaassaus",
    "block.guhs.guh_wheel_part": "Guhrad",
    "block.guhs.cheese_fountain": "Kaasfontein",
    "item.guhs.picked_up_guh": "Opgepakte guh",
    "item.guhs.picked_up_guh.named": "%s (opgepakt)",
    "item.guhs.picked_up_guh.hint": "Rechtsklik op een blok om hem neer te zetten, of op een guhrad om te rennen",
    "block.guhs.guh_wheel.occupied": "Er rent al een guh in dit rad",
    "block.guhs.bank_guh.lore": "Zo vadsig dat er oneindig veel spullen in zijn buikje passen",
    "block.guhs.bank_guh.contents": "%s spullen (%s soorten) in zijn buikje",
    "entity.guhs.quest_guh": "Hongerige Guh",
    "entity.guhs.quest_guh.request": "Guh guh... ik heb zooo'n honger! Breng me %s gefrituurde kaasknabbels (in je hand) en je krijgt iets heel vadsigs. (Je hebt er nu %s.)",
    "entity.guhs.quest_guh.thanks": "Nom nom nom! Dankjewel! Hier, neem mijn vriendje de Bankguh - die kan alles opeten en zit nooit vol!",
    "item.guhs.quest_guh_spawn_egg": "Hongerige Guh-spawnei",
    "gui.guhs.bank.search": "Zoeken... (@mod)",
    "gui.guhs.bank.summary": "%s spullen, %s soorten",
    "gui.guhs.bank.stored": "In het buikje: %s",
    "gui.guhs.bank.deposit": "Alles erin",
    "gui.guhs.bank.deposit.tooltip": "Stop alles uit je inventaris (niet je hotbar) in het buikje",
    "gui.guhs.bank.clear_grid": "Rooster leeg",
    "gui.guhs.bank.sort.name": "Naam",
    "gui.guhs.bank.sort.count": "Aantal",
    "gui.guhs.bank.sort.mod": "Mod",
    "gui.guhs.bank.sort.tooltip": "Sorteer op",
    "gui.guhs.bank.filter.all": "Alles",
    "gui.guhs.bank.filter.blocks": "Blok",
    "gui.guhs.bank.filter.tools": "Tool",
    "gui.guhs.bank.filter.food": "Eten",
    "gui.guhs.bank.filter.other": "Rest",
    "gui.guhs.bank.filter.tooltip": "Toon: alles / blokken / gereedschap & wapens / eten / overig",
    "gui.guhs.menu.sit": "Zitten",
    "gui.guhs.menu.stand": "Opstaan",
    "gui.guhs.menu.teleport": "Naar mij teleporteren: %s",
    "gui.guhs.menu.gravity": "Zwaartekracht: %s",
    "gui.guhs.menu.size": "%s blokken",
    "subtitles.guhs.entity.guh.ambient": "Guh guht",
    "subtitles.guhs.entity.guh.hurt": "Guh heeft pijn",
    "subtitles.guhs.entity.guh.death": "Guh loopt leeg",
    "subtitles.guhs.entity.guh.eat": "Guh knabbelt",
    "subtitles.guhs.entity.guh.happy": "Guh is blij",
    "subtitles.guhs.entity.mika.ambient": "Mika grauwt",
    "subtitles.guhs.entity.mika.hurt": "Mika heeft pijn",
    "subtitles.guhs.entity.mika.death": "Mika loopt kwaadaardig leeg",
})
print("resources written")
# the 2.0.0 resources add to the lang files and loot tables written above
import runpy  # noqa: E402
runpy.run_path(os.path.join("tools", "make_v2.py"), run_name="__main__")
# Minecraft 26.1.2 formats (items/ definitions, GeckoLib folders, recipes, biomes, dimension types, ...): see tools/mc26.py
sys.path.insert(0, os.path.join("tools"))
import mc26  # noqa: E402
mc26.run()
