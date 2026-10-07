"""
biomes3 wereld: the Klaterdal (guhs:klaterdal). Not in FEATURES: bio_wereld.py calls biome(), surface() and build().
Java: feature/bio/wereld/DalTerrein (the shape: terraces, rock faces, the river) and DalVulling (its blocks).

Owner after the kern: the Klaterdal polish agent. This file is the extension point for everything of the biome that is
data: final colours (water, underwater fog, sky, fog), music, ambience, the ambient particle, trees and plants (placed
features in step 9 of biome()["features"], or new steps), the rock of the faces (surface()). What is a placeholder now is
marked PLACEHOLDER. Keep step 1 of the features exactly ["guhs:bio_wereld_vulling"] (bio_wereld.py asserts it).
Blocks of other slices: lib.blok(h, "<id>", "<stand-in>").
"""


def biome(h, lib, ores, lege_spawners):
    """The biome file (the 1.21.1 shape; tools/mc26.py converts the effects)."""
    return {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.4, "creature_spawn_probability": 0.3,
        # PLACEHOLDER colours: slightly lighter water than the lake
        "effects": {"sky_color": 0xF4C4E2, "fog_color": 0xFFE9F3, "water_color": 0x7FE0E0, "water_fog_color": 0x4FB6C0,
                    "grass_color": 0xF7B6CB, "foliage_color": 0xF7B6CB,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "minecraft:music.overworld.cherry_grove", "min_delay": 12000, "max_delay": 24000,
                              "replace_current_music": False}},
        # (the animals and wild guh variants of slice dieren come in through its own biome modifiers; no Mika's here)
        "spawners": {**lege_spawners, "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 2, "maxCount": 4}]},
        "spawn_costs": {}, "carvers": {"air": []},
        # PLACEHOLDER plants: a few guhbloesem trees and flowers
        # (no ore veins: the valley and the lake cut down to where the veins are, they would lie open at the surface)
        "features": [[], ["guhs:bio_wereld_vulling"], [], [], [], [], [], [], [], ["guhs:klaterdal_bloesemboom", "guhs:guh_flowers"], []]}


def surface(h, lib):
    """The surface rule inside the biome: the two top blocks stay the Guhmensie's pink wool, everything under them is
    white rock, so the faces between the terraces are rock (PLACEHOLDER: one plain rock)."""
    rots = lib.blok(h, "gladde_knuffelsteen", "guhs:knuffelsteen")
    top = {"type": "minecraft:stone_depth", "offset": 1, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
    return {"type": "minecraft:condition", "if_true": {"type": "minecraft:not", "invert": top},
            "then_run": {"type": "minecraft:block", "result_state": {"Name": rots}}}


def build(h, lib):
    """Everything else of the biome (configured / placed features, tags, sounds)."""
    # PLACEHOLDER tree: the Guhmensie's guhbloesem tree, only on dry ground (the heightmap of world generation does not
    # see the water the vulling feature placed, so without the air check trees would stand on the river bed and lake floor)
    h.w(f"{h.D}/worldgen/placed_feature/klaterdal_bloesemboom.json", {"feature": "guhs:guhbloesem", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 8}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
            {"type": "minecraft:would_survive", "state": {"Name": "guhs:guhbloesem_sapling", "Properties": {"stage": "0"}}}]}},
        {"type": "minecraft:biome"}]})
