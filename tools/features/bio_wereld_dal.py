"""
biomes3 wereld: the Klaterdal (guhs:klaterdal). Not in FEATURES: bio_wereld.py calls biome(), surface() and build().
Java: feature/bio/wereld/DalTerrein (the shape: broad terraces, rock faces, the river with its cascades and tall falls,
pools, stepping stones, boulders, natural steps), DalVulling (its water and rock), DalPlanten (everything that grows),
DalBlokken (the biome's own blocks and sounds), client/DalSfeer (more petals in wind and rain).

What is data, and so written here:
  - the biome file: colours (water a shade lighter than the lake's turquoise, a pale blue-pink sky, a warm pink haze),
    the falling petals in the air, the valley's own music (klaterdal.muziek), the babbling river as ambience loop
    (klaterdal.sfeer) and a wind chime now and then (klaterdal.windgong). No placed features: every plant of the valley
    is placed by DalPlanten from the terrain model (it has to know how far the river is; that is how no tree hides it).
  - the surface: the two top blocks stay the Guhmensie's pink wool, under them white rounded rock (gladde knuffelsteen)
    with thin streaks of parelmoer, so the faces between the terraces are rock.
  - the biome's own blocks: klaterdal_mos (+ _tapijt), klaterdal_riet, klaterdal_bonsaiblad: textures
    (bio_wereld_dal_tex.py), models, loot, recipes, tags, Dutch names.
  - sounds.json and the OGGs (bio_wereld_dal_geluid.py; written once).
Keep step 1 of the features exactly ["guhs:bio_wereld_vulling"] (bio_wereld.py asserts it).
Blocks of other slices: lib.blok(h, "<id>", "<stand-in>").
"""
import os

from features import bio_wereld_dal_geluid as geluid
from features import bio_wereld_dal_tex as tex

# The lake's water is 0x5FD3D6 (bio_wereld_meer.py); the valley's is the same turquoise a shade lighter and clearer.
WATER, WATER_MIST = 0x86E4E0, 0x5CC3C8
HEMEL, MIST = 0xDCE4F8, 0xFDEBF3
GRAS, LOOF = 0xF6BCD0, 0xF2B3CC

TEKSTEN = {
    "block.guhs.klaterdal_mos": "Klatermos",
    "block.guhs.klaterdal_mos_tapijt": "Klatermostapijt",
    "block.guhs.klaterdal_riet": "Bloesemriet",
    "block.guhs.klaterdal_bonsaiblad": "Bonsailoof",
    "subtitles.guhs.klaterdal.sfeer": "Beekje klatert",
    "subtitles.guhs.klaterdal.windgong": "Windgong klingelt",
}
BLOKKEN = ["klaterdal_mos", "klaterdal_mos_tapijt", "klaterdal_riet", "klaterdal_bonsaiblad"]


def biome(h, lib, ores, lege_spawners):
    """The biome file (the 1.21.1 shape; tools/mc26.py converts the effects)."""
    return {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.4, "creature_spawn_probability": 0.3,
        "effects": {"sky_color": HEMEL, "fog_color": MIST, "water_color": WATER, "water_fog_color": WATER_MIST,
                    "grass_color": GRAS, "foliage_color": LOOF,
                    # petals in the air, all day (client/DalSfeer adds many more in a gust of wind and in the rain)
                    "particle": {"options": {"type": "minecraft:cherry_leaves"}, "probability": 0.0028},
                    "ambient_sound": "guhs:klaterdal.sfeer",
                    "additions_sound": {"sound": "guhs:klaterdal.windgong", "tick_chance": 0.0009},
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "guhs:klaterdal.muziek", "min_delay": 3600, "max_delay": 9600, "replace_current_music": False}},
        # (the animals and wild guh variants of slice dieren come in through its own biome modifiers; no Mika's here)
        "spawners": {**lege_spawners, "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 2, "maxCount": 4}]},
        "spawn_costs": {}, "carvers": {"air": []},
        # (no ore veins: the valley cuts down to where the veins are, they would lie open at the surface; no placed plants:
        # DalPlanten places them with the terrain)
        "features": [[], ["guhs:bio_wereld_vulling"], [], [], [], [], [], [], [], [], []]}


def surface(h, lib):
    """The surface rule inside the biome: the two top blocks stay the Guhmensie's pink wool, everything under them is
    white rock with thin streaks of parelmoer, so the faces between the terraces are rounded white rock."""
    rots = lib.blok(h, "gladde_knuffelsteen", "guhs:knuffelsteen")
    top = {"type": "minecraft:stone_depth", "offset": 1, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
    streep = {"type": "minecraft:noise_threshold", "noise": "guhs:klaterdal_detail", "min_threshold": 0.42, "max_threshold": 0.56}
    return {"type": "minecraft:condition", "if_true": {"type": "minecraft:not", "invert": top},
            "then_run": {"type": "minecraft:sequence", "sequence": [
                {"type": "minecraft:condition", "if_true": streep, "then_run": {"type": "minecraft:block", "result_state": {"Name": "guhs:parelmoer"}}},
                {"type": "minecraft:block", "result_state": {"Name": rots}}]}}


def blokken(h):
    A, D, w = h.A, h.D, h.w
    tex.build(h)
    h.simple_block("klaterdal_mos")
    h.simple_block("klaterdal_bonsaiblad", render_type="minecraft:cutout_mipped")
    w(f"{A}/models/block/klaterdal_mos_tapijt.json", {"parent": "minecraft:block/carpet", "textures": {"wool": "guhs:block/klaterdal_mos"}})
    w(f"{A}/blockstates/klaterdal_mos_tapijt.json", {"variants": {"": {"model": "guhs:block/klaterdal_mos_tapijt"}}})
    w(f"{A}/models/item/klaterdal_mos_tapijt.json", {"parent": "guhs:block/klaterdal_mos_tapijt"})
    w(f"{A}/models/block/klaterdal_riet.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                "textures": {"cross": "guhs:block/klaterdal_riet"}})
    w(f"{A}/blockstates/klaterdal_riet.json", {"variants": {"": {"model": "guhs:block/klaterdal_riet"}}})
    h.item_model("klaterdal_riet", "guhs:block/klaterdal_riet")
    for b in BLOKKEN:
        h.self_drop(b)
    # the moss is found in the valley; a patch can be made anywhere from vanilla moss and a pink petal
    h.shapeless("klaterdal_mos", ["minecraft:moss_block", "minecraft:pink_petals"], "guhs:klaterdal_mos", 1)
    h.shaped("klaterdal_mos_tapijt", ["MM"], {"M": "guhs:klaterdal_mos"}, "guhs:klaterdal_mos_tapijt", 3)
    add = h.add_tag
    add("minecraft/tags/block/mineable/hoe", ["guhs:klaterdal_mos", "guhs:klaterdal_mos_tapijt", "guhs:klaterdal_bonsaiblad"])
    add("minecraft/tags/block/sword_efficient", ["guhs:klaterdal_mos_tapijt", "guhs:klaterdal_riet"])
    # (dirt: flowers, saplings and guh-bamboe stand on the moss, as on vanilla's)
    add("minecraft/tags/block/dirt", ["guhs:klaterdal_mos"])
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/leaves", ["guhs:klaterdal_bonsaiblad"])
    add("minecraft/tags/block/replaceable_by_trees", ["guhs:klaterdal_riet"])
    return dict(TEKSTEN)


def sounds(h):
    geluid.schrijf(h)

    def patch(d):
        d["klaterdal.muziek"] = {"sounds": [{"name": "guhs:klaterdal/muziek", "stream": True, "volume": 0.6}]}
        d["klaterdal.sfeer"] = {"sounds": [{"name": "guhs:klaterdal/sfeer", "stream": True, "volume": 0.5}],
                                "subtitle": "subtitles.guhs.klaterdal.sfeer"}
        d["klaterdal.windgong"] = {"sounds": [{"name": f"guhs:klaterdal/windgong{i}", "volume": 0.6} for i in (1, 2, 3)],
                                   "subtitle": "subtitles.guhs.klaterdal.windgong"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def build(h, lib):
    """Everything else of the biome: its own blocks, its sounds, its texts."""
    lib.teksten(h, blokken(h))
    sounds(h)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for b in BLOKKEN:
        for pad in (f"{h.A}/blockstates/{b}.json", f"{h.A}/models/item/{b}.json", f"{h.D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(pad):
                problems.append(f"missing {pad}")
    for t in ("klaterdal_mos", "klaterdal_riet", "klaterdal_bonsaiblad"):
        if not os.path.exists(f"{h.A}/textures/block/{t}.png"):
            problems.append(f"missing texture {t}")
    for naam in geluid.GELUIDEN:
        if not os.path.exists(os.path.join(h.A, "sounds", "klaterdal", f"{naam}.ogg")):
            problems.append(f"missing sound klaterdal/{naam}.ogg")
    if problems:
        raise SystemExit("bio_wereld_dal self-check failed:\n  " + "\n  ".join(problems))
