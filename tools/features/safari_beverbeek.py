"""
1.6.0 deel 2 (F10a, design 2.10): de Beverbeek, a rare brook forest in the Guhmension (Java: nl.juiced.guhs.feature.beverbeek).

  - the biome guhs:beverbeek: a REGION of its own like the Bleekwoud (bleekwoud.py): where the low noise guhs:beverbeek is
    above TERM (and away from the seas, the Knuffeldal, the Guhpolder, the tundra, Guhwai'i, the Bleekwoud and the three
    biomes3 regions) the multi-noise temperature, humidity AND erosion drop by SHIFT, to the Beverbeek's own entry in the
    cold-dry-eroded corner, which no other region moves to. The terrain stays what it is; below KELDER_Y the term fades,
    so the cave biomes underneath stay. Its share (about 1%) is measured by BeverbeekGameTests.beverbeekAandeel.
  - the forest: willows (cherry-shaped crowns of oak leaves that hang down, with vines), alders (dark, slim), vanilla birches
    and fallen trees, grass and ferns, leaf litter, firefly bushes along the water, waterlilies and reed (sugar cane)
  - the brooks, the wild beaver pools and the beaver signs are Java features (beverbeek_beek, beverbeek_poel,
    beverbeek_sporen); no wild beavers spawn
  - the structure beverbeek_rangerpost (occasional): a log cabin with a porch where the ranger-guh (NPC kind
    beverbeek_ranger) stands, a little pond with a lodge, a woodpile and a gnawed stump; his one beaver is made by his role
  - the ranger's texts and the biome name (Dutch; English: tools/lang/en/c175_beverbeek.json)
"""
import json
import math
import os
import random
import shutil

BIOME = "beverbeek"
NOISE = "beverbeek"
NOISE_OCTAVE = -9
TERM = (0.645, 0.651)
SHIFT = 12.0
KELDER_Y = (40, 48)
GUHWAII_OFF = (0.34, 0.40)
BLEEKWOUD_OFF = (0.60, 0.64)        # never next to a Bleekwoud (its term starts at 0.650)

POST = "beverbeek_rangerpost"
POST_W, POST_H, G = 17, 12, 3
POST_C = 8
POST_ANCHOR = "guhs:beverbeek_rangerpost_midden"
RANGER = (6.5, G + 1, 10.5)          # where the ranger stands (on the porch, looking south)

FEITJES = [
    "Een bever knaagt een boompje om in één nachtje. Zijn tanden zijn oranje van het ijzer erin: keihard, njeg!",
    "Achter een beverdam stroomt het water niet meer weg, zo ontstaat een poel. Daarin bouwen ze hun burcht, met de voordeur ónder water. Slim, vads!",
    "Hoor je ineens KLETS? Dan slaat een bever met zijn platte staart op het water. Dat betekent: pas op, allemaal naar binnen!",
    "Een bever kan wel een kwartier onder water blijven. Ik hou het nog geen tien tellen vol.",
    "Bevers eten geen vis, hoor. Schors, blaadjes en waterplantjes: een knabbelende vegetariër!",
    "Hun vacht is zo dicht dat hun huid nooit nat wordt. Ze kammen hem met een gespleten nageltje aan hun achterpoot.",
    "Een beverfamilie werkt samen aan één dam, jaar na jaar. Net een bouwploeg guhs!",
    "Zie je afgeknaagde stronkjes en pootafdrukjes? Dan is er een bever langs geweest. Volg ze maar naar het water.",
]
AAI = ["Spaander knort tevreden.", "Spaander kwispelt met zijn platte staart.", "Wat een zachte, dikke vacht!",
       "Spaander knabbelt even aan je mouw. Lief!"]
TEKSTEN = {
    "biome.guhs.beverbeek": "Beverbeek",
    "entity.guhs.guh_npc.beverbeek_ranger": "Beverbeek-ranger",
    "gui.guhs.beverbeek.ranger.hallo": "Hoi! Ik ben de ranger van de Beverbeek. Dat daar is Spaander, mijn bever. Wil je iets weten, njeg?",
    "gui.guhs.beverbeek.ranger.optie.bevers": "Vertel eens over bevers",
    "gui.guhs.beverbeek.ranger.optie.aaien": "Mag ik Spaander aaien?",
    "gui.guhs.beverbeek.ranger.optie.bus": "Waar is het safaripark?",
    "gui.guhs.beverbeek.ranger.optie.nog": "Nog eentje!",
    "gui.guhs.beverbeek.ranger.optie.terug": "Iets anders vragen",
    "gui.guhs.beverbeek.ranger.optie.doei": "Doei!",
    "gui.guhs.beverbeek.ranger.aaien": "Tuurlijk! Spaander is dol op aaitjes. Maar hij woont hier bij mij, hoor: meenemen of temmen gaat niet. "
                                       "Wil je zelf bevers? In Safaripark Vadse Bergen kun je een verblijfsbord voor ze halen.",
    "gui.guhs.beverbeek.ranger.bus": "Safaripark Vadse Bergen? Daar rijdt de safaribus heen, vanaf een busstation hier in de Guhmension. "
                                     "Kijk op je Superkompas, of vraag het een reisguh. Daar wonen nog veel meer bevers, bij een échte beverburcht!",
}
for _i, _t in enumerate(FEITJES):
    TEKSTEN[f"gui.guhs.beverbeek.ranger.feit.{_i}"] = _t
for _i, _t in enumerate(AAI):
    TEKSTEN[f"gui.guhs.beverbeek.aai.{_i}"] = _t


# =====================================================================================================================
# worldgen
# =====================================================================================================================
def _leaves():
    return {"type": "minecraft:simple_state_provider",
            "state": {"Name": "minecraft:oak_leaves", "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}}


def _log(name):
    return {"type": "minecraft:simple_state_provider", "state": {"Name": name, "Properties": {"axis": "y"}}}


def _onder():
    return {"type": "minecraft:rule_based_state_provider", "rules": [{
        "if_true": {"type": "minecraft:not", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:cannot_replace_below_tree_trunk"}},
        "then": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:dirt"}}}]}


def _strooisel(kans):
    """vanilla's leaf litter decorator (as in oak_leaf_litter)"""
    return {"type": "minecraft:place_on_ground", "tries": 96, "radius": 4, "height": 2,
            "block_state_provider": {"type": "minecraft:weighted_state_provider", "entries": [
                {"weight": 1, "data": {"Name": "minecraft:leaf_litter", "Properties": {"facing": f, "segment_amount": str(n)}}}
                for n in (1, 2, 3) for f in ("north", "east", "south", "west")]}} if kans else None


def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    # the willow: a cherry-like branchy trunk, a wide crown of oak leaves that hangs down far, vines
    w(f"{wg}/configured_feature/beverbeek_wilg.json", {"type": "minecraft:tree", "config": {
        "below_trunk_provider": _onder(), "trunk_provider": _log("minecraft:oak_log"), "foliage_provider": _leaves(),
        "trunk_placer": {"type": "minecraft:cherry_trunk_placer", "base_height": 6, "height_rand_a": 2, "height_rand_b": 0,
                         "branch_count": {"type": "minecraft:weighted_list", "distribution": [{"data": 2, "weight": 1}, {"data": 3, "weight": 1}]},
                         "branch_horizontal_length": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 4},
                         "branch_start_offset_from_top": {"min_inclusive": -4, "max_inclusive": -3},
                         "branch_end_offset_from_top": {"type": "minecraft:uniform", "min_inclusive": -1, "max_inclusive": 0}},
        "foliage_placer": {"type": "minecraft:cherry_foliage_placer", "radius": 4, "offset": 0, "height": 5,
                           "wide_bottom_layer_hole_chance": 0.15, "corner_hole_chance": 0.3,
                           "hanging_leaves_chance": 0.55, "hanging_leaves_extension_chance": 0.7},
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 2},
        "decorators": [{"type": "minecraft:leave_vine", "probability": 0.3}, _strooisel(True)],
        "ignore_vines": False}})
    # the alder: slim and dark, a small round crown
    w(f"{wg}/configured_feature/beverbeek_els.json", {"type": "minecraft:tree", "config": {
        "below_trunk_provider": _onder(), "trunk_provider": _log("minecraft:dark_oak_log"), "foliage_provider": _leaves(),
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 6, "height_rand_a": 3, "height_rand_b": 0},
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "radius": 2, "offset": 0, "height": 4},
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "decorators": [_strooisel(True)], "ignore_vines": True}})
    survive = {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                           "state": {"Name": "minecraft:oak_sapling", "Properties": {"stage": "0"}}}}
    for naam in ("beverbeek_wilg", "beverbeek_els"):
        w(f"{wg}/placed_feature/{naam}_checked.json", {"feature": f"guhs:{naam}", "placement": [survive]})
    w(f"{wg}/configured_feature/beverbeek_bomen.json", {"type": "minecraft:random_selector", "config": {
        "default": "guhs:beverbeek_els_checked",
        "features": [{"chance": 0.38, "feature": "guhs:beverbeek_wilg_checked"},
                     {"chance": 0.3, "feature": "minecraft:birch_bees_0002_leaf_litter"},
                     {"chance": 0.04, "feature": "minecraft:fallen_birch_tree"},
                     {"chance": 0.04, "feature": "minecraft:fallen_oak_tree"}]}})
    w(f"{wg}/placed_feature/beverbeek_bomen.json", {"feature": "guhs:beverbeek_bomen", "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [{"data": 6, "weight": 3}, {"data": 8, "weight": 1}]}},
        {"type": "minecraft:in_square"}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}]})
    # the Java features: once per chunk (the brooks and the signs), the pools in one chunk in 3 (of those with a brook in the middle)
    for naam, eerst in (("beverbeek_beek", []), ("beverbeek_poel", [{"type": "minecraft:rarity_filter", "chance": 3}]), ("beverbeek_sporen", [])):
        w(f"{wg}/configured_feature/{naam}.json", {"type": f"guhs:{naam}", "config": {}})
        # (the heightmap first: without it the biome filter looks at the bottom of the chunk, in the caves)
        w(f"{wg}/placed_feature/{naam}.json", {"feature": f"guhs:{naam}", "placement": [
            *eerst, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})

    # the biome (written in the 26.1 shape: mc26.biome leaves it alone)
    base = json.load(open(f"{wg}/biome/pink_puffs.json", encoding="utf-8"))
    ores = base["features"][6]
    w(f"{wg}/biome/{BIOME}.json", {
        "attributes": {"minecraft:visual/fog_color": "#d4ece6", "minecraft:visual/sky_color": "#9fd3ea",
                       "minecraft:visual/water_fog_color": "#2f6f7a",
                       "minecraft:visual/ambient_particles": [{"particle": {"type": "minecraft:falling_water"}, "probability": 0.0006}]},
        "has_precipitation": True, "temperature": 0.7, "downfall": 0.8,
        "effects": {"water_color": "#4aa3bf", "foliage_color": "#5fa64a", "dry_foliage_color": "#8a7a4a", "grass_color": "#79b85a"},
        "creature_spawn_probability": 0.12,
        "spawners": {**{k: [] for k in base["spawners"]}, "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 2, "maxCount": 3}]},
        "spawn_costs": {}, "carvers": [],
        "features": [[], [], ["guhs:beverbeek_beek", "guhs:beverbeek_poel"], [], [], [], ores, [], [],
                     ["guhs:beverbeek_bomen", "minecraft:patch_grass_forest", "minecraft:flower_default", "minecraft:patch_leaf_litter",
                      "minecraft:patch_bush", "minecraft:patch_firefly_bush_near_water", "minecraft:patch_waterlily",
                      "minecraft:patch_sugar_cane", "guhs:beverbeek_sporen"], []]})

    # the region: the noise, the router terms on temperature, humidity and erosion, the biome's own entry
    from features import bio_wereld as bw
    from features import knuffeldal_wereld as kw
    from features import verhaal_wereld as vw
    w(f"{wg}/noise/{NOISE}.json", {"firstOctave": NOISE_OCTAVE, "amplitudes": [1.0]})

    def term():
        buiten = kw.mul(kw.mul(kw.geen_zee(), vw.geen_knuffel()), kw.mul(vw.geen_polder(), kw.mul(
            vw.geen_toendra(), kw.spline(vw.GUHWAII_NOISE, [(GUHWAII_OFF[0], 1.0), (GUHWAII_OFF[1], 0.0)]))))
        buiten = kw.mul(buiten, kw.spline("bleekwoud", [(BLEEKWOUD_OFF[0], 1.0), (BLEEKWOUD_OFF[1], 0.0)]))
        for soort in bw.ENTRIES:                                  # never in the three biomes3 regions
            buiten = kw.mul(buiten, kw.add(1.0, kw.mul(bw._regio(soort), -1.0)))
        vlak = {"type": "minecraft:cache_2d", "argument": kw.mul(kw.spline(NOISE, [(TERM[0], 0.0), (TERM[1], 1.0)]), buiten)}
        return kw.mul(vlak, kw.gradient(KELDER_Y[0], KELDER_Y[1], 0.0, 1.0))

    def router(d):
        r = d["noise_router"]
        if f"guhs:{NOISE}\"" not in json.dumps(r["erosion"]):      # (idempotent)
            for slot in ("temperature", "vegetation", "erosion"):
                r[slot] = kw.add(r[slot], kw.mul(term(), -SHIFT))
    h.patch_json(f"{wg}/noise_settings/guhmension.json", router)

    def biome_source(d):
        alles = [-2.0, 2.0]
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] != f"guhs:{BIOME}"]
        entries.append({"biome": f"guhs:{BIOME}", "parameters": {"temperature": -2.0, "humidity": -2.0, "continentalness": alles,
                                                                 "erosion": -2.0, "weirdness": alles, "depth": alles, "offset": 0.5}})
        d["generator"]["biome_source"]["biomes"] = entries
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    def surface(d):
        rules = d["surface_rule"]["sequence"]
        vloer = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
        onder = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True, "secondary_depth_range": 0, "surface_type": "floor"}
        rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]},
                "then_run": {"type": "minecraft:sequence", "sequence": [
                    {"type": "minecraft:condition", "if_true": vloer, "then_run": {"type": "minecraft:condition",
                     "if_true": {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False},
                     "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:grass_block", "Properties": {"snowy": "false"}}}}},
                    {"type": "minecraft:condition", "if_true": onder, "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:dirt"}}}]}}
        d["surface_rule"]["sequence"] = [r for r in rules if r.get("if_true", {}).get("biome_is") != [f"guhs:{BIOME}"]] + [rule]
    h.patch_json(f"{wg}/noise_settings/guhmension.json", surface)
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({f"guhs:{BIOME}": {"villager_type": "guhs:guh"}}))
    h.add_tag("guhs/tags/worldgen/biome/has_structure/knabbelkelder", [f"guhs:{BIOME}"])


# =====================================================================================================================
# de rangerpost
# =====================================================================================================================
def _jigsaw(s, x, y, z, final):
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": POST_ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": final,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def _as(axis="y"):
    return {"axis": axis}


def rangerpost(h):
    s = h.Structure((POST_W, POST_H, POST_W))
    rng = random.Random(20261021)
    C = POST_C
    trap = {"half": "bottom", "shape": "straight", "waterlogged": "false"}
    # the plate: grass on dirt, round, so the forest floor runs on at the corners
    fp = []
    for x in range(POST_W):
        for z in range(POST_W):
            if math.dist((x, z), (C, C)) <= 8.2:
                for y in range(G):
                    s.set(x, y, z, "minecraft:dirt")
                s.set(x, G, z, "minecraft:grass_block", {"snowy": "false"})
                fp.append((x, z))
    s.clear_above(fp, G + 1)
    # the cabin: x 3..9, z 3..8, spruce planks between oak corner logs, a dark oak gable roof (ridge along x)
    x0, x1, z0, z1 = 3, 9, 3, 8
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, G, z, "minecraft:spruce_planks")
            rand = x in (x0, x1) or z in (z0, z1)
            for y in range(G + 1, G + 4):
                if rand:
                    hoek = x in (x0, x1) and z in (z0, z1)
                    s.set(x, y, z, "minecraft:oak_log" if hoek else "minecraft:spruce_planks", _as() if hoek else None)
                else:
                    s.set(x, y, z, "minecraft:air")
    for i in range(4):
        for x in range(x0 - 1, x1 + 2):
            s.set(x, G + 4 + i, z0 - 1 + i, "minecraft:dark_oak_stairs", {**trap, "facing": "south"})
            s.set(x, G + 4 + i, z1 + 1 - i, "minecraft:dark_oak_stairs", {**trap, "facing": "north"})
    for z in range(z0, z1 + 1):
        i = min(z - (z0 - 1), (z1 + 1) - z)
        for y in range(G + 4, G + 4 + i):
            for x in range(x0, x1 + 1):
                s.set(x, y, z, "minecraft:spruce_planks" if x in (x0, x1) else "minecraft:air")
    # moss on the roof here and there
    for _ in range(10):
        x, i = rng.randint(x0 - 1, x1 + 1), rng.randint(0, 2)
        z = z0 - 1 + i if rng.random() < 0.5 else z1 + 1 - i
        if s.get(x, G + 5 + i, z) is None:
            s.set(x, G + 5 + i, z, "minecraft:moss_carpet")
    # the door (south, toward the porch) and the windows
    s.set(6, G + 1, z1, "minecraft:spruce_door", {"facing": "south", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    s.set(6, G + 2, z1, "minecraft:spruce_door", {"facing": "south", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    for (x, z) in ((x0, 5), (x0, 6), (x1, 5), (x1, 6), (4, z1), (8, z1), (6, z0)):
        s.set(x, G + 2, z, "minecraft:glass_pane", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    # inside: a table with two chairs, a barrel and a chest, a lantern, a potted fern, a bed
    s.set(5, G + 1, 5, "guhs:guh_tafel")
    s.set(4, G + 1, 5, "guhs:guh_stoel", {"facing": "east"})
    s.set(5, G + 1, 4, "guhs:guh_stoel", {"facing": "south"})
    s.set(8, G + 1, 4, "minecraft:barrel", {"facing": "up", "open": "false"})
    s.set(8, G + 1, 7, "minecraft:potted_fern")
    s.set(4, G + 1, 7, "minecraft:flower_pot")
    s.set(6, G + 3, 5, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    s.set(7, G + 1, 4, "minecraft:green_bed", {"facing": "south", "part": "head", "occupied": "false"})
    s.set(7, G + 1, 5, "minecraft:green_bed", {"facing": "south", "part": "foot", "occupied": "false"})
    # the porch: a deck in front of the door, two posts with lanterns, a bench
    for x in range(x0, x1 + 1):
        for z in (z1 + 1, z1 + 2):
            s.set(x, G, z, "minecraft:spruce_planks")
    for x in (x0, x1):
        s.set(x, G + 1, z1 + 2, "minecraft:oak_fence", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
        s.set(x, G + 2, z1 + 2, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    s.set(8, G + 1, z1 + 1, "minecraft:spruce_stairs", {**trap, "facing": "north"})
    # the woodpile against the west wall, a chopping stump with a gnawed top
    for z in range(4, 8):
        for y in range(G + 1, G + 1 + (2 if z % 2 else 3)):
            s.set(x0 - 1, y, z, "minecraft:oak_log", _as("z"))
    s.set(1, G + 1, 10, "minecraft:oak_log", _as())
    s.set(1, G + 2, 10, "minecraft:stripped_oak_log", _as())
    # Spaander's pond (south east): water two deep, a muddy edge, a little lodge in it, reed and a lily pad
    px, pz = 11, 12
    for x in range(px - 3, px + 4):
        for z in range(pz - 2, pz + 3):
            e = ((x - px) / 3.2) ** 2 + ((z - pz) / 2.4) ** 2
            if e <= 1.0:
                s.set(x, G - 2, z, "minecraft:mud")
                s.set(x, G - 1, z, "minecraft:water", {"level": "0"})
                s.set(x, G, z, "minecraft:water", {"level": "0"})
            elif e <= 1.5 and s.get(x, G, z) == "minecraft:grass_block":
                s.set(x, G, z, "minecraft:mud")
    for (dx, dz, dy) in ((1, 0, 1), (2, 0, 1), (1, 1, 1), (2, -1, 0), (1, 0, 2)):
        s.set(px + dx, G + dy, pz + dz, "minecraft:oak_log" if dy == 1 else "minecraft:muddy_mangrove_roots", _as("x") if dy == 1 else {"axis": "y"})
    s.set(px + 1, G + 3, pz, "minecraft:leaf_litter", {"facing": "north", "segment_amount": "3"})
    s.set(px - 2, G + 1, pz - 2, "minecraft:sugar_cane", {"age": "0"})
    s.set(px - 1, G + 1, pz + 2, "minecraft:lily_pad")
    # leaf litter and a few flowers on the plate
    for _ in range(18):
        x, z = rng.randint(0, POST_W - 1), rng.randint(0, POST_W - 1)
        if s.get(x, G, z) == "minecraft:grass_block" and s.get(x, G + 1, z) in (None, "minecraft:air"):
            if rng.random() < 0.7:
                s.set(x, G + 1, z, "minecraft:leaf_litter", {"facing": rng.choice(["north", "east", "south", "west"]),
                                                              "segment_amount": str(rng.randint(1, 4))})
            else:
                s.set(x, G + 1, z, rng.choice(["minecraft:lily_of_the_valley", "minecraft:blue_orchid", "minecraft:fern"]))
    _jigsaw(s, C, 0, C, "minecraft:dirt")
    # the ranger on his porch
    ms = h.ms
    s.entity(RANGER[0], float(RANGER[1]), RANGER[2], {"id": "guhs:guh_npc", "Kind": "beverbeek_ranger", "PersistenceRequired": ms.Byte(1),
                                                       "Rotation": ms.floats(0.0, 0.0)})
    return s


def check(s):
    problemen = []
    x, y, z = int(RANGER[0]), int(RANGER[1]), int(RANGER[2])
    if s.get(x, y - 1, z) != "minecraft:spruce_planks":
        problemen.append(f"the ranger does not stand on the porch: {s.get(x, y - 1, z)}")
    if s.get(x, y, z) not in (None, "minecraft:air") or s.get(x, y + 1, z) not in (None, "minecraft:air"):
        problemen.append("the ranger's spot is not free")
    if s.get(6, G + 1, 8) != "minecraft:spruce_door":
        problemen.append("no door")
    return problemen


def structures(h):
    s = rangerpost(h)
    problemen = check(s)
    if problemen:
        raise SystemExit("beverbeek rangerpost check failed:\n  " + "\n  ".join(problemen))
    s.save(POST)
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[POST] = 17
    h.FLATNESS[POST] = 8
    h.structure(POST, [BIOME], spacing=6, separation=3, salt=20261022, start_y=-G, reach=40, centre=POST_ANCHOR,
                spawn_overrides={"monster": none})
    print(f"safari_beverbeek: rangerpost ok ({len(s.blocks)} blocks)")


def textures(h):
    """The ranger wears the safari rangers' uniform (the hoofdranger's skin)."""
    bron = os.path.join(h.A, "textures", "entity", "npc_safari_hoofdranger.png")
    if os.path.exists(bron):
        shutil.copyfile(bron, os.path.join(h.A, "textures", "entity", "npc_beverbeek_ranger.png"))


def texts(h):
    for k, v in TEKSTEN.items():
        h.lang(k, v, v)


def build(h):
    worldgen(h)
    structures(h)
    textures(h)
    texts(h)
