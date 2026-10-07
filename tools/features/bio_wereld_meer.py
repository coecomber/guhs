"""
biomes3 wereld: the Bloesemmeertje (guhs:bloesemmeertje). Not in FEATURES: bio_wereld.py calls biome(), surface() and build().
Java: feature/bio/wereld/MeerTerrein (the shape: the basin, the islands) and MeerVulling (water, sand, the placeholder
big tree on every large island).

Owner after the kern: the Bloesemmeertje polish agent. This file is the extension point for everything of the biome that
is data: final colours (clear turquoise water, underwater fog, sky, fog), music, ambience, the ambient particle, trees and
plants (lilies, reeds, seagrass, drijvende_bloesemblaadjes: placed features in step 9 of biome()["features"]), the lake
floor (surface(); MeerVulling.bodem for a floor that changes with depth). What is a placeholder now is marked PLACEHOLDER.
Keep step 1 of the features exactly ["guhs:bio_wereld_vulling"] (bio_wereld.py asserts it).
Blocks of other slices: lib.blok(h, "<id>", "<stand-in>").
"""
WATER_Y = 49   # MeerTerrein.WATER: the top water block of every lake


def biome(h, lib, ores, lege_spawners):
    """The biome file (the 1.21.1 shape; tools/mc26.py converts the effects)."""
    return {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.5, "creature_spawn_probability": 0.3,
        # PLACEHOLDER colours: turquoise water
        "effects": {"sky_color": 0xF4C4E2, "fog_color": 0xFFE9F3, "water_color": 0x5FD3D6, "water_fog_color": 0x2F9FA8,
                    "grass_color": 0xF7B6CB, "foliage_color": 0xF7B6CB,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "minecraft:music.overworld.cherry_grove", "min_delay": 12000, "max_delay": 24000,
                              "replace_current_music": False}},
        # (koi, kikkerguhs and the bloesemguh of slice dieren come in through its own biome modifiers; no Mika's here)
        "spawners": {**lege_spawners, "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 2, "maxCount": 4}]},
        "spawn_costs": {}, "carvers": {"air": []},
        # PLACEHOLDER plants: a few guhbloesem trees on the shore and the islands
        "features": [[], ["guhs:bio_wereld_vulling"], [], [], [], [], ores, [], [], ["guhs:guhbloesem_rare"], []]}


def surface(h, lib):
    """The surface rule inside the biome: sand on everything below the shore (the lake floor, also where the feature has
    not put water yet), four blocks deep; the land stays the Guhmensie's pink wool."""
    boven = {"type": "minecraft:y_above", "anchor": {"absolute": WATER_Y + 1}, "surface_depth_multiplier": 0, "add_stone_depth": False}
    diep = {"type": "minecraft:stone_depth", "offset": 3, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
    return {"type": "minecraft:condition", "if_true": {"type": "minecraft:not", "invert": boven},
            "then_run": {"type": "minecraft:condition", "if_true": diep,
                         "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:sand"}}}}


def build(h, lib):
    """Everything else of the biome (configured / placed features, tags, sounds): nothing yet."""
