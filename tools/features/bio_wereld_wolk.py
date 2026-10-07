"""
biomes3 wereld: the Wolkenweide (guhs:wolkenweide). Not in FEATURES: bio_wereld.py calls biome(), surface() and build().
Java: feature/bio/wereld/WolkTerrein (the meadow, the floating islands, the stairs, where clouds and lifts go) and
WolkVulling (the cloud blocks and the wolkenlift / wolkenstroom columns).

Owner after the kern: the Wolkenweide polish agent. This file is the extension point for everything of the biome that is
data: final colours (sky, fog, the warmer sunset), music, ambience, the ambient particle, plants and trees on the meadow
and the islands (placed features in step 9 of biome()["features"]; a heightmap placement finds the island tops too), the
blocks of the islands' bodies (surface()). The Python twin of the island shapes for templates is bio_wereld_eiland.py.
What is a placeholder now is marked PLACEHOLDER. Keep step 1 of the features exactly ["guhs:bio_wereld_vulling"].
Blocks of other slices: lib.blok(h, "<id>", "<stand-in>").
"""
EILAND_VANAF_Y = 77   # above the meadow's highest ground (WolkTerrein.WEIDE_Y + GLOOIING + the 4 blocks an island floats)


def biome(h, lib, ores, lege_spawners):
    """The biome file (the 1.21.1 shape; tools/mc26.py converts the effects)."""
    return {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.3, "creature_spawn_probability": 0.3,
        # PLACEHOLDER colours: a pastel sky
        "effects": {"sky_color": 0xC9D6FF, "fog_color": 0xF3E6FF, "water_color": 0x8FD8F0, "water_fog_color": 0x5FA8D0,
                    "grass_color": 0xF7B6CB, "foliage_color": 0xF7B6CB,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "minecraft:music.overworld.meadow", "min_delay": 12000, "max_delay": 24000,
                              "replace_current_music": False}},
        # (wolkenschaapjes and the wild wolkguh of slice dieren come in through its own biome modifiers; no Mika's here)
        "spawners": {**lege_spawners, "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 2, "maxCount": 4}]},
        "spawn_costs": {}, "carvers": {"air": []},
        # PLACEHOLDER plants: flowers and pink grass
        "features": [[], ["guhs:bio_wereld_vulling"], [], [], [], [], ores, [], [], ["guhs:guh_flowers", "guhs:roze_gras"], []]}


def surface(h, lib):
    """The surface rule inside the biome: the top block of every island and of the meadow stays the Guhmensie's pink wool;
    the body of an island (everything under its top block, above the meadow) is PLACEHOLDER pink terracotta in bands
    with white rock."""
    top = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
    hoog = {"type": "minecraft:y_above", "anchor": {"absolute": EILAND_VANAF_Y}, "surface_depth_multiplier": 0, "add_stone_depth": False}
    band = {"type": "minecraft:noise_threshold", "noise": "guhs:klaterdal_detail", "min_threshold": 0.15, "max_threshold": 10.0}

    def blok(name):
        return {"type": "minecraft:block", "result_state": {"Name": name}}
    return {"type": "minecraft:condition", "if_true": hoog,
            "then_run": {"type": "minecraft:condition", "if_true": {"type": "minecraft:not", "invert": top},
                         "then_run": {"type": "minecraft:sequence", "sequence": [
                             {"type": "minecraft:condition", "if_true": band, "then_run": blok("guhs:knuffelsteen")},
                             blok("minecraft:pink_terracotta")]}}}


def build(h, lib):
    """Everything else of the biome (configured / placed features, tags, sounds): nothing yet."""
