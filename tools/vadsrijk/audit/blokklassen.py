"""Block classes for the walkability audit: what a block does to a walking player (collision, floor height, climbing).

Only names that occur in the Vadsrijk map matter; unknown blocks count as SOLID (a full block), which is the safe guess.
"""

# movement classes (uint8)
AIR, SOLID, WATER, CLIMB, SLAB, FENCE, STAIR, DOOR, PLANT, LAVA = range(10)
PASSABLE = (AIR, WATER, CLIMB, DOOR, PLANT)

# material tags (uint8, for the decoration checks)
M_ANDERS, M_PAD, M_BOOM, M_GRAS, M_BLOEM, M_BOUW, M_TRAP, M_DEUR = range(8)

_LUCHT = {"air", "cave_air", "void_air", "light", "structure_void", "vr_w_portaal", "nether_portal", "end_portal"}
_WATER = {"water", "bubble_column", "kelp", "kelp_plant", "seagrass", "tall_seagrass"}
_KLIM = {"ladder", "vine", "scaffolding", "twisting_vines", "twisting_vines_plant", "weeping_vines", "weeping_vines_plant",
         "cave_vines", "cave_vines_plant"}
# no (or negligible) collision: decoration you walk through
_DOOR_LOS = {"torch", "wall_torch", "soul_torch", "redstone_torch", "lever", "rail", "tripwire", "end_rod", "iron_chain",
             "lantern", "soul_lantern", "pale_hanging_moss", "spruce_sign", "oak_sign", "light_weighted_pressure_plate",
             "brown_mushroom", "red_mushroom", "glimpaddenstoel", "guh_waterlelie", "lily_pad", "cobweb",
             "mini_luchtballon"}
_BLOEMEN = {"allium", "azure_bluet", "cornflower", "oxeye_daisy", "pink_tulip", "red_tulip", "white_tulip", "orange_tulip",
            "poppy", "dandelion", "lily_of_the_valley", "pink_petals", "wildflowers", "roze_guhbloem", "guhoortjes",
            "knabbelroos", "roze_hibiscus", "vr_w_litteken_plant", "firefly_bush", "short_grass", "tall_grass", "fern",
            "large_fern", "bloesemmeertje_riet", "dead_bush", "sweet_berry_bush", "leaf_litter", "moss_carpet", "sunflower",
            "lilac", "rose_bush", "peony", "short_dry_grass", "tall_dry_grass", "bush"}
_TAPIJT = ("_carpet",)
_GRAS = {"grass_block", "knuffelgras", "weide_roze_gras", "vr_w_litteken_gras", "moss_block", "podzol", "mycelium", "dirt",
         "coarse_dirt", "rooted_dirt"}
_PAD = {"dirt_path", "knuffelklinkers", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"}
_BOUW_WOORDEN = ("planks", "dakpan", "wool", "pleister", "baksteen", "bricks", "tentdoek", "glasinlood", "lakhout", "concrete",
                 "knuffelblok", "tegel_", "gladde_knuffelsteen", "_glass")
_BOOM_WOORDEN = ("_log", "_leaves", "_wood", "paddenstoel_hoed", "paddenstoel_steel", "schimmelsteel", "schimmelpaddenstoel",
                 "mushroom_block", "mushroom_stem", "bladeren")


def splits(state):
    naam, _, rest = state.partition("[")
    props = {}
    if rest:
        for kv in rest.rstrip("]").split(","):
            if "=" in kv:
                k, v = kv.split("=", 1)
                props[k] = v
    return naam, props


def klasse(state):
    """state string -> (movement class, material tag)."""
    naam, props = splits(state)
    n = naam.split(":", 1)[-1]
    mat = M_ANDERS
    if n in _PAD:
        mat = M_PAD
    elif n in _GRAS:
        mat = M_GRAS
    elif n in _BLOEMEN:
        mat = M_BLOEM
    elif any(w in n for w in _BOOM_WOORDEN):
        mat = M_BOOM
    elif any(w in n for w in _BOUW_WOORDEN):
        mat = M_BOUW
    if n in _LUCHT:
        return AIR, mat
    if n in _WATER:
        return WATER, mat
    if n == "lava":
        return LAVA, mat
    if n in _KLIM:
        return CLIMB, mat
    if n == "wolkenstroom":
        return (AIR if props.get("down") == "true" else CLIMB), mat
    if n.endswith("trapdoor"):
        return DOOR, mat
    if n.endswith("_door") or n.endswith("_deur") or n.endswith("fence_gate"):
        return (SOLID if n.startswith("iron") else DOOR), M_DEUR
    if n in _DOOR_LOS or n in _BLOEMEN or n.endswith(_TAPIJT) or n.endswith("_button") or n.endswith("pressure_plate") \
            or n.endswith("_sign") or n.endswith("_banner") or n.endswith("_sapling") or n.endswith("_torch"):
        return PLANT, mat
    if n.endswith("_fence") or n.endswith("_wall") or n.endswith("_muur"):
        return FENCE, mat
    if n.endswith("_stairs") or n.endswith("_trap"):
        return STAIR, M_TRAP
    if n.endswith("_slab") or n.endswith("_plaat"):
        return (SLAB if props.get("type", "bottom") == "bottom" else SOLID), mat
    if n in ("campfire", "soul_campfire", "red_bed", "white_bed", "pink_bed"):
        return SLAB, mat
    return SOLID, mat
