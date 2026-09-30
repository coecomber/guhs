"""
3.0 (Guhverhalen) - the fundament's worldgen: the two new biomes and the one-per-region structure placement.

Both biomes follow the Knuffeldal / Guhpolder recipe (worldgen research §1.3): a low noise of their own, a "term" t (0 outside,
1 inside), masks against the other regions, and a blended terrain bed in the noise router (final_density = terrain * (1 - b) +
bed * b). Unlike the older regions their term moves the multi-noise CONTINENTALNESS (the crystal noise, [-1, 1] at the surface):
the tundra +3, Guhwai'i -3, to their own entries at continentalness 2 / -2. The weirdness axis is shared by the Knuffeldal
(+1.5), the Guhpolder (-1.2) and the sea (-2): a band on it would be crossed half-way by their transitions (thin rings of
tundra around every Knuffeldal, of Guhwai'i around every polder); no other region moves continentalness, so nothing crosses.

  - Sneeuwguhtoendra (guhs:sneeuwguhtoendra): white rolling hills (y ~68-92) on land, away from the deep seas, the Knuffeldal
    and the Guhpolder. Continentalness 2 (shift +3), depth [-1, 0.3], any weirdness. Snowy (vanilla snow may pile up there: NOT in
    guhs:geen_weer). Terrain constants: the marker block <balto-terrein> (only balto edits it, only the numbers).
  - Guhwai'i (guhs:guhwaii): a warm tropical lagoon-sea with an island in the middle, on the coasts of the Diepe Guhzee (the sea
    noise between GUHWAII_KUST: just outside the deep sea's beach dam, so it lies "aan de Guhzee"), where the guhwaii noise is
    high, away from the Knuffeldal, the Guhpolder and the tundra. From its rim inward: a sandy rim at the water level, its own
    turquoise water (ZEE_BODEM_Y deep; the placed feature guhs:guhwaii_water, like the Diepe Guhzee's water), a shallow lagoon
    ring (y ~58-60), a beach (y63-65) and a gentle hill in the middle (~y80). The deep seas themselves are too small for islands
    of 150-300 blocks (their shelf is ~50 blocks wide) and their middle belongs to the Guhbubbel, so the islands bring their
    own sea. Continentalness -2 (shift -3), depth [-1, 1] (the lagoon floor too), any weirdness. The surface (sand, grass on the island) follows the noises, not the
    biome, so the lagoon floor under the water is sand too. Terrain constants: the marker block <guhwaii-terrein>.

The region structures: placement guhs:regio_piek + structure type guhs:regio_jigsaw (Java: feature/verhaal/wereld/), one start
per region on its peak ("piek"), on its coast ("kust") or out at sea ("zee"), found by /locate, the Superkompas and the
compasses. regio_structuur(...) is the ONLY way slices write these (it also registers the make_v2 bouwruimte/grond hooks).
The masks of the Java side (Regio masks) use the midpoints of the python splines below, so both agree where a region is.
"""
import json
import os

from features import knuffeldal_wereld as kw
from features import guhpolder_wereld as gw
from features import diepzee as dz

SEA_NOISE = "guhmension_zee"
KNUFFEL_NOISE = kw.NOISE           # guhmension_knuffel
POLDER_NOISE = gw.NOISE            # guhmension_polder

# =====================================================================================================================
# Sneeuwguhtoendra
# =====================================================================================================================
TOENDRA = "sneeuwguhtoendra"
TOENDRA_NOISE = "sneeuwguhtoendra"
TOENDRA_HEUVELS = "sneeuwguhtoendra_heuvels"
TOENDRA_CONT = (2.0, 2.0)          # the biome entry: continentalness (the term shifts it by TOENDRA_CONT_SHIFT) and depth
TOENDRA_DEPTH = (-1.0, 0.3)
TOENDRA_TERM = (0.51, 0.53)        # the biome term 0 -> 1 (the fundament's share tuning, see VerhaalWereldGameTests)
TOENDRA_CONT_SHIFT = 3.0
TOENDRA_POLDER_OFF = (0.50, 0.56)  # never next to a Guhpolder
TOENDRA_KNUFFEL_OFF = gw.KNUFFEL_OFF   # nor next to a Knuffeldal (the polder's own mask)
# <balto-terrein>
TOENDRA_BLEND = (0.50, 0.55)       # the terrain: its own hills at BLEND[0], the tundra's rolling hills from BLEND[1]
TOENDRA_BED_Y = 80                 # the middle height of the white hills
TOENDRA_HEUVEL_AMP = 10            # +- this many blocks of rolling hills (the noise guhs:sneeuwguhtoendra_heuvels)
TOENDRA_HEUVEL_OCTAVE = -6         # its size (~64 blocks per hill)
# </balto-terrein>

# =====================================================================================================================
# Guhwai'i
# =====================================================================================================================
GUHWAII = "guhwaii"
GUHWAII_NOISE = "guhwaii"
GUHWAII_CONT = (-2.0, -2.0)
GUHWAII_DEPTH = (-1.0, 1.0)
GUHWAII_CONT_SHIFT = -3.0
# the sea noise: towards the Diepe Guhzee (up, full, full, gone), outside its dam (1.0.0: moved up with dz.ZEE_KRIMP)
GUHWAII_KUST = tuple(round(v + dz.ZEE_KRIMP, 3) for v in (-0.20, -0.12, 0.27, 0.30))
GUHWAII_TOENDRA_OFF = (0.46, 0.50)        # never next to a tundra
# <guhwaii-terrein>
EILAND_ZEE = 0.42                  # the guhwaii noise where the region starts (its sandy rim; the water starts just inside)
ZEE_BODEM_Y = 50                   # the floor of the lagoon-sea around the island (its top block)
EILAND_RAND = 0.53                 # the noise where the island's shallow lagoon ring starts
LAGUNE_DIEPTE = 4                  # the lagoon floor lies this far under the water (the water's top block is y62)
EILAND_STRAND = 0.56               # the noise at the beach (y EILAND_STRAND_Y)
EILAND_STRAND_Y = 64
EILAND_TOP = 0.70                  # the noise where the hill reaches its top
EILAND_TOP_Y = 80
# </guhwaii-terrein>

WATER_Y = dz.WATER_LEVEL           # 63: water below this height
REGIO_SALTS = {}                   # name -> salt (regio_structuur), for the tests / the report


# --- helpers ---------------------------------------------------------------------------------------------------------------
def spline(noise, points):
    return kw.spline(noise, points)


def geen_polder():
    return spline(POLDER_NOISE, [(TOENDRA_POLDER_OFF[0], 1.0), (TOENDRA_POLDER_OFF[1], 0.0)])


def geen_knuffel():
    return spline(KNUFFEL_NOISE, [(TOENDRA_KNUFFEL_OFF[0], 1.0), (TOENDRA_KNUFFEL_OFF[1], 0.0)])


def toendra_buiten():
    """1 away from the deep seas, the Knuffeldal and the Guhpolder, 0 near them."""
    return kw.mul(kw.mul(kw.geen_zee(), geen_knuffel()), geen_polder())


def toendra_term():
    return kw.mul(spline(TOENDRA_NOISE, [(TOENDRA_TERM[0], 0.0), (TOENDRA_TERM[1], 1.0)]), toendra_buiten())


def toendra_blend():
    return kw.mul(spline(TOENDRA_NOISE, [(TOENDRA_BLEND[0], 0.0), (TOENDRA_BLEND[1], 1.0)]), toendra_buiten())


def geen_toendra():
    return spline(TOENDRA_NOISE, [(GUHWAII_TOENDRA_OFF[0], 1.0), (GUHWAII_TOENDRA_OFF[1], 0.0)])


def kust():
    """1 on the coasts of the Diepe Guhzee (GUHWAII_KUST), away from the Knuffeldal, the Guhpolder and the tundra, else 0."""
    a, b, c, d = GUHWAII_KUST
    return {"type": "minecraft:cache_2d", "argument": kw.mul(kw.mul(spline(SEA_NOISE, [(a, 0.0), (b, 1.0), (c, 1.0), (d, 0.0)]),
                                                                    kw.mul(geen_knuffel(), geen_polder())), geen_toendra())}


def guhwaii_term():
    """The biome: the whole region (from its rim inward)."""
    return kw.mul(spline(GUHWAII_NOISE, [(EILAND_ZEE - 0.01, 0.0), (EILAND_ZEE, 1.0)]), kust())


def guhwaii_blend():
    return kw.mul(spline(GUHWAII_NOISE, [(EILAND_ZEE - 0.02, 0.0), (EILAND_ZEE, 1.0)]), kust())


def eiland_hoogte():
    """The top block height of the region as a function of the guhwaii noise: the rim at the water level, the lagoon-sea's
    floor, the island's lagoon ring, the beach and the hill."""
    lagune = WATER_Y - 1 - LAGUNE_DIEPTE
    return spline(GUHWAII_NOISE, [(EILAND_ZEE, WATER_Y), (EILAND_ZEE + 0.03, ZEE_BODEM_Y), (EILAND_RAND - 0.02, ZEE_BODEM_Y),
                                  (EILAND_RAND, lagune), (EILAND_RAND + (EILAND_STRAND - EILAND_RAND) * 0.6, lagune + 1),
                                  (EILAND_STRAND, EILAND_STRAND_Y), (EILAND_STRAND + 0.02, EILAND_STRAND_Y + 1),
                                  (EILAND_TOP, EILAND_TOP_Y)])


def bed(height_function):
    """A solid bed whose top block is at the given height (density 0.025 per block, like the others)."""
    return kw.add(kw.gradient(0, 256, 0.025 * 0.5, 0.025 * (0.5 - 256)), kw.mul(height_function, 0.025))


def router(d):
    r = d["noise_router"]
    final = r["final_density"]
    assert final["type"] == "minecraft:max" and final["argument1"]["type"] == "minecraft:interpolated", "unexpected final_density"
    inner = final["argument1"]["argument"]
    # the tundra's white hills
    heuvels = kw.add(TOENDRA_BED_Y + 0.0, kw.mul(kw.noise_ref(TOENDRA_HEUVELS), float(TOENDRA_HEUVEL_AMP)))
    b1 = toendra_blend()
    inner = kw.add(kw.mul(inner, kw.add(kw.mul(b1, -1.0), 1.0)), kw.mul(bed(heuvels), b1))
    # Guhwai'i: its rim, its lagoon-sea and the island
    b2 = guhwaii_blend()
    inner = kw.add(kw.mul(inner, kw.add(kw.mul(b2, -1.0), 1.0)), kw.mul(bed(eiland_hoogte()), b2))
    final["argument1"] = {"type": "minecraft:interpolated", "argument": inner}
    r["continents"] = kw.add(kw.add(r["continents"], kw.mul(toendra_term(), TOENDRA_CONT_SHIFT)), kw.mul(guhwaii_term(), GUHWAII_CONT_SHIFT))


def _is_ons(rule):
    return (rule.get("if_true", {}).get("biome_is") in ([f"guhs:{TOENDRA}"], [f"guhs:{GUHWAII}"])
            or rule.get("if_true", {}).get("noise") == f"guhs:{GUHWAII_NOISE}")   # (the region rule by the noises)


def surface(d):
    """Placeholder surfaces (balto / guhwaii may replace their own rule): snowy grass over dirt; sand beaches and grass."""
    def block(name, props=None):
        return {"type": "minecraft:block", "result_state": {"Name": name, **({"Properties": props} if props else {})}}

    def depth(offset):
        return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}

    toendra = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{TOENDRA}"]},
               "then_run": {"type": "minecraft:sequence", "sequence": [
                   {"type": "minecraft:condition", "if_true": depth(0), "then_run": block("minecraft:grass_block", {"snowy": "true"})},
                   {"type": "minecraft:condition", "if_true": depth(3), "then_run": block("minecraft:dirt")}]}}
    strand = {"type": "minecraft:y_above", "anchor": {"absolute": EILAND_STRAND_Y + 2}, "surface_depth_multiplier": 0, "add_stone_depth": False}

    def drempel(noise, lo, hi):
        return {"type": "minecraft:noise_threshold", "noise": f"guhs:{noise}", "min_threshold": lo, "max_threshold": hi}

    # (by the noises, not the biome: the whole region incl. the floor under its water; the Java Regio masks alike)
    k0, k1, k2, k3 = GUHWAII_KUST
    in_regio = [drempel(GUHWAII_NOISE, EILAND_ZEE - 0.02, 10.0), drempel(SEA_NOISE, (k0 + k1) / 2, (k2 + k3) / 2),
                drempel(KNUFFEL_NOISE, -10.0, sum(TOENDRA_KNUFFEL_OFF) / 2), drempel(POLDER_NOISE, -10.0, sum(TOENDRA_POLDER_OFF) / 2),
                drempel(TOENDRA_NOISE, -10.0, sum(GUHWAII_TOENDRA_OFF) / 2)]
    zand = {"type": "minecraft:sequence", "sequence": [
        {"type": "minecraft:condition", "if_true": depth(0), "then_run": {"type": "minecraft:sequence", "sequence": [
            {"type": "minecraft:condition", "if_true": strand, "then_run": block("minecraft:grass_block", {"snowy": "false"})},
            block("minecraft:sand")]}},
        {"type": "minecraft:condition", "if_true": depth(4), "then_run": block("minecraft:sand")},
        {"type": "minecraft:condition", "if_true": depth(8), "then_run": block("minecraft:sandstone")}]}
    for cond in reversed(in_regio):
        zand = {"type": "minecraft:condition", "if_true": cond, "then_run": zand}
    guhwaii = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{GUHWAII}"]},
               "then_run": {"type": "minecraft:sequence", "sequence": [
                   {"type": "minecraft:condition", "if_true": depth(0), "then_run": {"type": "minecraft:sequence", "sequence": [
                       {"type": "minecraft:condition", "if_true": strand, "then_run": block("minecraft:grass_block", {"snowy": "false"})},
                       block("minecraft:sand")]}},
                   {"type": "minecraft:condition", "if_true": depth(4), "then_run": block("minecraft:sand")},
                   {"type": "minecraft:condition", "if_true": depth(8), "then_run": block("minecraft:sandstone")}]}}
    rules = d["surface_rule"]["sequence"]
    # (ours go first; the region rule by the noises first of all)
    d["surface_rule"]["sequence"] = [zand, toendra, guhwaii] + [x for x in rules if not _is_ons(x)]


def biome_source(d):
    anything = [-2.0, 2.0]
    entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] not in (f"guhs:{TOENDRA}", f"guhs:{GUHWAII}")]
    entries.append({"biome": f"guhs:{TOENDRA}", "parameters": {"temperature": anything, "humidity": anything, "continentalness": list(TOENDRA_CONT),
                    "erosion": anything, "weirdness": anything, "depth": list(TOENDRA_DEPTH), "offset": 0.0}})
    entries.append({"biome": f"guhs:{GUHWAII}", "parameters": {"temperature": anything, "humidity": anything, "continentalness": list(GUHWAII_CONT),
                    "erosion": anything, "weirdness": anything, "depth": list(GUHWAII_DEPTH), "offset": 0.0}})
    d["generator"]["biome_source"]["biomes"] = entries


def patch_router(h, path):
    """Idempotent: only when this generator hasn't wrapped the router yet (the file is written fresh every run)."""
    d = json.load(open(path, encoding="utf-8"))
    if f"guhs:{TOENDRA_NOISE}" not in json.dumps(d["noise_router"]):
        router(d)
    surface(d)
    h.w(path, d)


def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    w(f"{wg}/noise/{TOENDRA_NOISE}.json", {"firstOctave": -9, "amplitudes": [1.0]})
    w(f"{wg}/noise/{TOENDRA_HEUVELS}.json", {"firstOctave": TOENDRA_HEUVEL_OCTAVE, "amplitudes": [1.0, 0.5, 0.25]})
    w(f"{wg}/noise/{GUHWAII_NOISE}.json", {"firstOctave": -9, "amplitudes": [1.0, 0.4]})
    patch_router(h, f"{wg}/noise_settings/guhmension.json")
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)
    # Guhwai'i's own water (Java: verhaal.wereld.GuhwaiiWaterFeature): the region's columns from just inside its rim
    w(f"{wg}/configured_feature/guhwaii_water.json", {"type": "guhs:guhwaii_water", "config": {
        "regio": dict(regio(GUHWAII_NOISE), min_value=0.0, dal_value=0.0), "min_value": EILAND_ZEE + 0.004, "water_level": WATER_Y}})
    w(f"{wg}/placed_feature/guhwaii_water.json", {"feature": "guhs:guhwaii_water", "placement": []})

    base = json.load(open(f"{wg}/biome/pink_puffs.json", encoding="utf-8"))
    ores = base["features"][6]
    toendra = {
        "has_precipitation": True, "temperature": -0.5, "downfall": 0.6, "creature_spawn_probability": 0.3,
        "effects": {"sky_color": 0xC6DDF4, "fog_color": 0xEEF4FB, "water_color": 0x7FB2E0, "water_fog_color": 0x46729E,
                    "grass_color": 0xDCEBEA, "foliage_color": 0x9DB5A8,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "minecraft:music.overworld.snowy_slopes", "min_delay": 12000, "max_delay": 24000,
                              "replace_current_music": False},
                    "particle": {"options": {"type": "minecraft:white_ash"}, "probability": 0.002}},
        "spawners": {**{k: [] for k in base["spawners"]},
                     "creature": [{"type": "guhs:guh", "weight": 100, "minCount": 2, "maxCount": 4}]},
        "spawn_costs": {}, "carvers": {"air": []},
        # placeholder vegetation (balto owns steps 2+ of this biome: spar guh trees, snow drifts...)
        "features": [[], [], [], [], [], [], ores, [], [], ["minecraft:trees_snowy", "minecraft:patch_grass_plain"], ["minecraft:freeze_top_layer"]]}
    w(f"{wg}/biome/{TOENDRA}.json", toendra)
    sea = json.load(open(f"{wg}/biome/{dz.BIOME}.json", encoding="utf-8"))
    guhwaii = {
        "has_precipitation": True, "temperature": 0.95, "downfall": 0.8, "creature_spawn_probability": 0.3,
        "effects": {"sky_color": 0x8FD3FF, "fog_color": 0xFFE8D6, "water_color": 0x3FD0D8, "water_fog_color": 0x1F8FA8,
                    "grass_color": 0x7FE07A, "foliage_color": 0x5ED06A,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "minecraft:music.overworld.jungle", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}},
        "spawners": {**{k: [] for k in sea["spawners"]},
                     "creature": [{"type": "guhs:guh", "weight": 80, "minCount": 2, "maxCount": 4}],
                     "water_ambient": [{"type": "guhs:guh_vis", "weight": 25, "minCount": 3, "maxCount": 6}]},
        "spawn_costs": {}, "carvers": {"air": []},
        # its own water (air under y63 becomes water: the lagoon-sea); placeholder plants (guhwaii owns steps 2+)
        "features": [[], ["guhs:guhwaii_water"], [], [], [], [], ores, [], [],
                     ["minecraft:patch_grass_plain", "minecraft:flower_default", "minecraft:patch_sugar_cane"], []]}
    w(f"{wg}/biome/{GUHWAII}.json", guhwaii)
    # guh villagers born here are guh villagers; the Knabbelkelders may lie under them; the band's favourite places
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({f"guhs:{TOENDRA}": {"villager_type": "guhs:guh"}, f"guhs:{GUHWAII}": {"villager_type": "guhs:guh"}}))
    h.add_tag("guhs/tags/worldgen/biome/has_structure/knabbelkelder", [f"guhs:{TOENDRA}", f"guhs:{GUHWAII}"])
    h.add_tag("guhs/tags/worldgen/biome/band/favoriete_plekken", [f"guhs:{TOENDRA}", f"guhs:{GUHWAII}"])
    # 3.0: the weather leaves only these biomes alone (the Guhpolder; NOT the tundra, where the snow may pile up)
    h.w(f"{D}/tags/worldgen/biome/geen_weer.json", {"replace": False, "values": [f"guhs:{gw.BIOME}"]})


# =====================================================================================================================
# the one-per-region structures (guhs:regio_piek + guhs:regio_jigsaw)
# =====================================================================================================================
def _masker(noise, lo=None, hi=None, ring=None):
    m = {"noise": f"guhs:{noise}"}
    if lo is not None:
        m["min"] = lo
    if hi is not None:
        m["max"] = hi
    if ring:
        m["ring"] = list(ring)
    return m


def regio(noise):
    """The Java Regio of a region noise: the noise, its masks (the spline midpoints of the terms above), min and dal values."""
    if noise == TOENDRA_NOISE:
        return {"noise": f"guhs:{TOENDRA_NOISE}", "masks": [
            _masker(SEA_NOISE, hi=sum(kw.SEA_OFF) / 2), _masker(KNUFFEL_NOISE, hi=sum(TOENDRA_KNUFFEL_OFF) / 2),
            _masker(POLDER_NOISE, hi=sum(TOENDRA_POLDER_OFF) / 2)],
            "min_value": TOENDRA_BLEND[1] + 0.02, "dal_value": TOENDRA_TERM[0] - 0.05, "buurt": 3}
    if noise == GUHWAII_NOISE:
        a, b, c, d = GUHWAII_KUST
        # (the spline midpoints: the same region as the surface rule and the water)
        return {"noise": f"guhs:{GUHWAII_NOISE}", "masks": [
            _masker(SEA_NOISE, lo=(a + b) / 2, hi=(c + d) / 2), _masker(KNUFFEL_NOISE, hi=sum(TOENDRA_KNUFFEL_OFF) / 2),
            _masker(POLDER_NOISE, hi=sum(TOENDRA_POLDER_OFF) / 2), _masker(TOENDRA_NOISE, hi=sum(GUHWAII_TOENDRA_OFF) / 2)],
            "min_value": EILAND_STRAND + 0.03, "dal_value": EILAND_ZEE - 0.03, "buurt": 2}
    if noise == SEA_NOISE:
        return {"noise": f"guhs:{SEA_NOISE}", "min_value": dz.PEAK_MIN, "dal_value": dz.WATER_FROM, "buurt": 2}
    raise ValueError(noise)


# per noise: the cell of the placement (chunks) and the biomes a start may stand in
# (1.1.2: the tundra 24, not 16: about one Nomguh per Sneeuwguhtoendra, no second one a few hundred blocks away)
REGIO_CEL = {TOENDRA_NOISE: 24, GUHWAII_NOISE: 16, SEA_NOISE: 16}
# 1.0.0: the story structures only in the bigger regions (the region's peak at least this high; the biomes themselves stay):
# about half as many as in 3.0 (VerhaalWereldGameTests.verhaalWereldEenPerRegio counts them). 3.0 used the regio() minimum
# (tundra 0.57, Guhwai'i 0.59, the sea dz.PEAK_MIN).
REGIO_STRUCTUUR_MIN = {TOENDRA_NOISE: 0.70, GUHWAII_NOISE: 0.64, SEA_NOISE: round(dz.PEAK_MIN + 0.09, 3)}
REGIO_BIOMEN = {TOENDRA_NOISE: [f"guhs:{TOENDRA}"], GUHWAII_NOISE: [f"guhs:{GUHWAII}"], SEA_NOISE: [f"guhs:{dz.BIOME}"]}


def regio_structuur(h, name, pool_start, noise, plek, salt, hoek=0, afstand=0, reach=80, voorrang=None, biome=None, grond_y=4,
                    vaste_y=None, ring=None, spawn_overrides=None, terrain_adaptation="beard_box"):
    """A one-per-region structure (the only way slices write these):
      name        the structure id (guhs:<name>); its template's anchor jigsaw is named guhs:<name>_midden
      pool_start  its start pool id ("guhs:<name>/start"; one element: the template, placed unrotated)
      noise       "sneeuwguhtoendra" | "guhwaii" | "guhmension_zee"
      plek        "piek" (the region's peak) | "kust" (from the peak in direction hash(cell) + hoek degrees to the beach) |
                  "zee" (afstand blocks from the sea peak, deep water on a ring of radius `ring` (default reach), outside the
                  Guhwai'i islands, >= 120 blocks from the Guhbubbel)
      reach       how far the template reaches from its anchor (<= 136)
      voorrang    h.VOORRANG value (who goes first when buildings meet)
      biome       the biomes its start may be in (default per noise)
      grond_y     the template y of its ground (the anchor layer, G): the 2.10 grond start element gets delta grond_y + 1
      vaste_y     the first free y for the anchor instead of the surface (default for "zee": 64: the anchor block at y63,
                  one above the water)
    """
    assert plek in ("piek", "kust", "zee"), plek
    assert noise in REGIO_CEL, noise
    assert reach <= 136, reach
    D, w = h.D, h.w
    if plek == "zee":
        vaste_y = 64 if vaste_y is None else vaste_y
        terrain_adaptation = "none"
    spot = {"regio": dict(regio(noise), min_value=REGIO_STRUCTUUR_MIN[noise]), "cell": REGIO_CEL[noise], "soort": plek, "hoek": float(hoek), "afstand": int(afstand)}
    if plek == "kust":
        spot["kust_value"] = EILAND_STRAND + 0.005 if noise == GUHWAII_NOISE else TOENDRA_TERM[1]
    if plek == "zee":
        spot["ring"] = int(ring if ring is not None else reach)
        spot["diep_value"] = round(0.40 + dz.ZEE_KRIMP, 3)
        spot["vermijd"] = regio(GUHWAII_NOISE)
        spot["bubbel_afstand"] = 120
    none = {"bounding_box": "full", "spawns": []}
    s = {"type": "guhs:regio_jigsaw", "biomes": f"#guhs:has_structure/{name}", "step": "surface_structures",
         "spawn_overrides": spawn_overrides if spawn_overrides is not None else {"monster": none, "ambient": none},
         "terrain_adaptation": terrain_adaptation, "start_pool": pool_start, "start_jigsaw_name": f"guhs:{name}_midden",
         "plek": spot, "keep_clear": reach + 9, "grond_y": grond_y, "reach": reach}
    if vaste_y is not None:
        s["vaste_y"] = vaste_y
    w(f"{D}/worldgen/structure/{name}.json", s)
    w(f"{D}/worldgen/structure_set/{name}.json", {"structures": [{"structure": f"guhs:{name}", "weight": 1}],
                                                   "placement": {"type": "guhs:regio_piek", "salt": salt, "plek": spot}})
    w(f"{D}/tags/worldgen/biome/has_structure/{name}.json", {"values": biome or REGIO_BIOMEN[noise]})
    if voorrang is not None:
        h.VOORRANG[name] = voorrang
    REGIO_SALTS[name] = salt
    hooks(h)


def hooks(h):
    """make_v2's post-passes for guhs:regio_jigsaw: keep_clear (bouwruimte) and the grond start element (grond)."""
    def ruimte(s, jigsaw_reach):
        # (the anchor sits on the spot, anywhere in the start chunk: its reach + half a chunk)
        return jigsaw_reach(s, True) + 8

    def grond(s):
        return s.get("grond_y", 4) + 1

    h.RUIMTE_HOOKS["guhs:regio_jigsaw"] = ruimte
    h.GROND_HOOKS["guhs:regio_jigsaw"] = grond


def plaatshouder(h, name, kind=None, titel="", W=21, G=4, onder="minecraft:snow_block", vloer="guhs:knuffelklinkers", zee=False):
    """A small placeholder building for a regio structure (the owner slice replaces it): a square with a kiosk and its NPC, the
    anchor jigsaw guhs:<name>_midden in the middle of the ground layer (template y G); for the sea (zee) a little rock island
    from the sea floor up. Writes the template <name> and the pool guhs:<name>/start; returns the pool id."""
    H = G + 10
    base = 0
    if zee:
        # a rock pillar from the floor (y ~34) to the ground: the template is placed with its ground at y63
        base = 30
        H += base
    s = h.Structure((W, H, W))
    c = W // 2
    for x in range(W):
        for z in range(W):
            r = max(abs(x - c), abs(z - c))
            if zee and (x - c) ** 2 + (z - c) ** 2 > (c + 0.4) ** 2:
                continue
            for y in range(G + base):
                s.set(x, y, z, "minecraft:stone" if zee and y < base else onder)
            s.set(x, G + base, z, vloer if r <= 2 or abs(x - c) <= 1 or abs(z - c) <= 1 else ("minecraft:sand" if zee else onder))
    if kind:
        # the kiosk (knuffelsteen posts, a pluisdak roof) with the NPC
        for (x, z) in ((c - 2, c - 2), (c + 2, c - 2), (c - 2, c + 2), (c + 2, c + 2)):
            for y in range(G + base + 1, G + base + 5):
                s.set(x, y, z, "guhs:knuffelsteen_muur", {"up": "true"})
        for x in range(c - 3, c + 4):
            for z in range(c - 3, c + 4):
                edge = max(abs(x - c), abs(z - c))
                s.set(x, G + base + 5 + (3 - edge) // 2, z, "guhs:pluisdak")
                if edge < 3:
                    s.set(x, G + base + 5, z, "guhs:pluisdak")
        s.set(c, G + base + 7, c, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "0"})
        s.entity(c + 0.5, float(G + base + 1), c + 1.5, {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": h.ms.Byte(1),
                                                        "Rotation": h.ms.floats(0.0, 0.0)})
    for (x, z) in ((1, 1), (W - 2, 1), (1, W - 2), (W - 2, W - 2)):
        if not zee or (x - c) ** 2 + (z - c) ** 2 <= c * c:
            s.set(x, G + base + 1, z, "guhs:seizoensbloembak", {"seizoen": "lente"})
    s.set(c, G + base, c, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": f"guhs:{name}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": vloer, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(W)], G + base + 1, top=H)
    s.save(name)
    h.w(f"{h.D}/worldgen/template_pool/{name}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{name}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    if titel:
        h.lang(f"structure.guhs.{name}", titel, titel)
    return f"guhs:{name}/start", G + base


# =====================================================================================================================
# advancements, FTB
# =====================================================================================================================
def advancements(h):
    D = h.D
    for biome, icon, titel, desc in [
        (TOENDRA, "minecraft:snow_block", "Brrr, wat wit!", "Vind de Sneeuwguhtoendra in de Guhmensie"),
        (GUHWAII, "minecraft:sand", "Aloha, njeg!", "Vind een eilandje van Guhwai'i in de Diepe Guhzee"),
    ]:
        name = f"find_{biome}"
        h.w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": "guhs:guhmension/enter_guhmension",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": "task", "show_toast": True, "announce_to_chat": False},
            "criteria": {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": f"guhs:{biome}"}}}}}})
        h.lang(f"advancements.guhs.guhmension.{name}.title", titel, titel)
        h.lang(f"advancements.guhs.guhmension.{name}.description", desc, desc)
    h.lang(f"biome.guhs.{TOENDRA}", "Sneeuwguhtoendra", "Sneeuwguhtoendra")
    h.lang(f"biome.guhs.{GUHWAII}", "Guhwai'i", "Guhwai'i")


def build(h):
    worldgen(h)
    advancements(h)
    hooks(h)
