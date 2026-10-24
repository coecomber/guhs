"""
1.7.0 "Onder de Wol", ronde 27-30: a more cheerful surface for the common Guhmension (Java: feature/onderwol/
OnderWolOppervlak.java). The surface was mostly pink wool and kaasknabbels ("typisch guh, prima"); this adds:

  1. the quilt (onderwol_pastelwol, Java): big soft patches where the top 1-3 pink wool turns lila, white or peach;
  2. the shores (onderwol_viltkiezel, Java): viltkiezel at the waterline of lakes, seas and saus, and (ronde 29) stretches
     of pink sand (guhs:roze_zand, with little shells guhs:schelpje) and of plain sand between them;
  3. flower meadows (onderwol_bloemenweitje + onderwol_weitjesgras): dense noise patches of the mod's flowers and pink grass;
  4. wolkeien (onderwol_wolkei, Java): little round boulders of viltsteen / wool with a pink mossy carpet;
  5. vijvertjes (onderwol_vijvertje, Java) and wolwatervalletjes (onderwol_waterval*, onderwol_kaasval, Java): small ponds
     with lilies and reeds, and springs in hillsides that fall into a little pool;
  6. paddenstoelenkringen (onderwol_paddenstoelenkring, Java): fairy rings of glinsterpaddenstoelen, sometimes a guh inside;
  7. omgevallen stammetjes (onderwol_stammetje_*): vanilla fallen trees of the mod's own logs, with mushrooms and moss;
  8. pluisjes in the air of the meadow biomes (ambient particles guhs:pluisje).
 ronde 29/30:
  9. boompjes & struikjes: small groups of little blossom / fluff trees (onderwol_boomgroepje_*), flowering bushes
     (onderwol_struikje) and short hedges (onderwol_heg, Java) in the open fields;
 10. eilandjes (onderwol_eilandje, Java): little islands with a tree in bigger lakes and shallow seas;
 11. the kaaslappendeken (onderwol_kaaslappendeken + onderwol_kaasplasje, Java): gatenkaas / kaasmos patches, kaasbloemen
     and kaas saus puddles on the kaasknabbels of kaas_flats and knabbel_crumbs;
 12. hartjesmeertjes (onderwol_hartjesmeertje*, Java): rare heart-shaped lakes with a flower rim and hartjeslelies;
 13. regenboogheuvels (onderwol_regenboogheuvels, Java): rare pastel-striped hillsides;
 14. the Bloemenvallei: a small rare surface biome full of flowers, bees, butterflies and fluff (its biome JSON, its
     climate entry in the dimension's biome_source, the onderwol_vallei_* features, a spawn modifier);
 15. sfeer: softer pastel fog/sky for the ordinary surface biomes, glimguhtjes by night over the fields and the sea.

Every step is idempotent: the biome lists get our ids (re-appended at the end of their step so they run after the
lakes/springs already there); carvers, spawners and the rest of the biomes are untouched. New chunks only.

    PYTHONPATH=tools python tools/features/onderwol_oppervlak.py        (from the worktree root)
"""
import copy
import json
import os

R = os.path.join("src", "main", "resources")
A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
WG = f"{D}/worldgen"


def g(*names):
    return [f"guhs:{n}" for n in names]


# --- which biomes get what -------------------------------------------------------------------------------------------
VALLEI = g("bloemenvallei")
WOL = g("guh_fields", "guh_meadows", "pink_puffs", "kaas_flats", "knabbel_crumbs", "guh_peaks", "vads_cliffs")
WEIDES = g("guh_fields", "guh_meadows", "pink_puffs") + VALLEI
PASTELWOL = WOL + g("guh_sea")
KIEZEL = WOL + VALLEI + g("guh_sea", "mikas_biome", "knuffeldal", "vadswoud", "beverbeek")
STRAND = WOL + VALLEI + g("guh_sea")                      # (pink and plain sand stretches; the others only viltkiezel)
WOLKEI = WOL + VALLEI + g("knuffeldal", "mikas_biome")
VIJVER = WEIDES + g("knuffeldal", "kaas_flats", "knabbel_crumbs", "vadswoud")
WATERVAL_STEIL = g("guh_peaks", "vads_cliffs")
WATERVAL_ZACHT = WEIDES + g("knuffeldal", "vadswoud", "kaas_flats", "knabbel_crumbs", "mikas_biome")
KAASVAL = g("kaas_flats", "knabbel_crumbs", "vads_cliffs")
KRING = WEIDES + g("knuffeldal", "vadswoud")
STAM = {"onderwol_stammetje_bloesem": WEIDES, "onderwol_stammetje_vads": g("vadswoud"), "onderwol_stammetje_pluis": g("knuffeldal")}
DEELTJES = g("guh_fields", "guh_meadows", "pink_puffs")   # (the Bloemenvallei has its own mix)
BOOM = g("guh_fields", "guh_meadows", "pink_puffs")
STRUIK = WEIDES
EILAND = WOL + VALLEI + g("guh_sea")
KAAS = g("kaas_flats", "knabbel_crumbs")
HART = g("guh_fields", "guh_meadows", "pink_puffs")
REGENBOOG = g("guh_fields", "guh_meadows", "pink_puffs", "guh_peaks", "vads_cliffs")

# step -> [(placed feature, biomes)] in the order they must run (one global order: the same relative order in every biome)
STAPPEN = {
    1: [("onderwol_vijvertje", VIJVER), ("onderwol_hartjesmeertje", HART), ("onderwol_hartjesmeertje_vallei", VALLEI)],
    2: [("onderwol_eilandje", EILAND), ("onderwol_pastelwol", PASTELWOL), ("onderwol_regenboogheuvels", REGENBOOG),
        ("onderwol_kaaslappendeken", KAAS), ("onderwol_bloemenvallei_grond", VALLEI), ("onderwol_viltkiezel", KIEZEL),
        ("onderwol_kaasplasje", KAAS), ("onderwol_wolkei", WOLKEI)],
    8: [("onderwol_wolwaterval", WATERVAL_STEIL), ("onderwol_wolwaterval_zacht", WATERVAL_ZACHT), ("onderwol_kaasval", KAASVAL)],
    9: [*STAM.items(), ("onderwol_paddenstoelenkring", KRING), ("onderwol_bloemenweitje", WEIDES), ("onderwol_weitjesgras", WEIDES),
        ("onderwol_boomgroepje_bloesem", BOOM), ("onderwol_boomgroepje_pluis", BOOM), ("onderwol_struikje", STRUIK), ("onderwol_heg", STRUIK),
        ("onderwol_vallei_boompje", VALLEI), ("onderwol_vallei_hoge_bloemen", VALLEI), ("onderwol_vallei_bloemen", VALLEI),
        ("onderwol_vallei_blaadjes", VALLEI)],
}
ALLE = {f"guhs:{n}" for lijst in STAPPEN.values() for n, _ in lijst}

# the natural surface ground these features may change or build on
GROND = ["minecraft:pink_wool", "minecraft:magenta_wool", "minecraft:white_wool", "guhs:knuffelblok_perzik", "guhs:block_of_kaasknabbels",
         "minecraft:grass_block", "minecraft:dirt", "minecraft:sand", "minecraft:clay", "minecraft:pink_concrete_powder",
         "minecraft:pink_terracotta", "minecraft:magenta_terracotta", "guhs:knuffelgras", "guhs:vadsmos", "guhs:viltkiezel", "guhs:viltsteen",
         # ronde 29/30: the peach of the quilt, the stripes of the rainbow hills, the pink sand and the cheese quilt
         "minecraft:white_terracotta", "minecraft:light_blue_wool", "guhs:roze_zand", "guhs:gatenkaas", "guhs:kaasmos"]
BLOEMEN_HOOG = g("knabbelroos", "kaasbloem", "guhoortjes")
BLOEMEN_LAAG = g("roze_guhbloem", "guhoortjes", "roze_guhbloem")
STAMMEN = {"onderwol_stammetje_bloesem": ("guhs:guhbloesem_log", 9, "minecraft:pink_carpet"),
           "onderwol_stammetje_vads": ("guhs:vadshout_stam", 3, "minecraft:moss_carpet"),
           "onderwol_stammetje_pluis": ("guhs:pluizenboom_stam", 4, "minecraft:pink_carpet")}
# the Bloemenvallei's flowers in all guh colours (the vanilla ones only take on its grass: simple_block checks canSurvive)
VALLEI_BLOEMEN = [("guhs:roze_guhbloem", 4), ("guhs:knabbelroos", 3), ("guhs:guhoortjes", 3), ("guhs:kaasbloem", 2),
                  ("minecraft:pink_tulip", 3), ("minecraft:white_tulip", 2), ("minecraft:allium", 3), ("minecraft:azure_bluet", 2),
                  ("minecraft:cornflower", 2), ("minecraft:oxeye_daisy", 2), ("minecraft:lily_of_the_valley", 2)]
VALLEI_HOOG = [("minecraft:peony", 3), ("minecraft:lilac", 2)]

# the Bloemenvallei's place in the climate (see the report: a narrow point between guh_fields (0, 0) and guh_meadows
# (-0.35, 0) with an offset, so it only wins small pockets, mostly of those two)
VALLEI_KLIMAAT = {"temperature": -0.17, "humidity": 0.0, "continentalness": [-1.0, 1.0], "erosion": [-0.7, 0.15], "weirdness": 0.0,
                  "depth": [-1.0, 1.0], "offset": 0.18}

# 15. sfeer: the original fog / sky of the ordinary surface biomes and the soft pastel each is nudged towards (fixed
# values, so a rerun gives the same colours)
SFEER = {  # biome: (fog, sky, fog target, sky target, share)
    "guh_fields": ("#ffd6e6", "#ffb3d1", "#fbe6f1", "#e9c4ee", 0.25),
    "guh_meadows": ("#ffd9ea", "#ffa6cc", "#fbe6f1", "#e9c4ee", 0.25),
    "pink_puffs": ("#ffe8f4", "#f7c6e6", "#fbe6f1", "#e9c4ee", 0.25),
    "kaas_flats": ("#ffe6bf", "#ffd08a", "#fde9dc", "#f6cfc8", 0.25),
    "knabbel_crumbs": ("#ffe0c2", "#ffc29e", "#fde9dc", "#f6cfc8", 0.25),
    "guh_peaks": ("#f6ddfa", "#e8b4f0", "#f7e6f6", "#e6c8f2", 0.25),
    "vads_cliffs": ("#e8b8da", "#d08abf", "#f2d6ea", "#dcaad6", 0.25),
    "guh_sea": ("#ffddee", "#f4b8dc", "#f8e4f2", "#e4c6ee", 0.25),
}
VALLEI_KLEUREN = {"fog": "#fde4f0", "sky": "#efbfe6", "gras": "#f9bcd6"}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def patch_json(path, fn):
    data = json.load(open(path, encoding="utf-8"))
    fn(data)
    w(path, data)


def tag(path, values):
    w(f"{R}/data/{path}.json", {"replace": False, "values": values})


def add_tag(path, values):
    full = f"{R}/data/{path}.json"
    data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    w(full, data)


def st(name, **props):
    s = {"Name": name}
    if props:
        s["Properties"] = props
    return {"type": "minecraft:simple_state_provider", "state": s}


def gewogen(*paren):
    return {"type": "minecraft:weighted_state_provider", "entries": [{"data": {"Name": n}, "weight": wt} for n, wt in paren]}


def op_oppervlak(heightmap="WORLD_SURFACE_WG", zeldzaam=None, aantal=None, gebouwen=False):
    p = []
    if zeldzaam:
        p.append({"type": "minecraft:rarity_filter", "chance": zeldzaam})
    if aantal:
        p.append({"type": "minecraft:count", "count": aantal})
    p += [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": heightmap}, {"type": "minecraft:biome"}]
    if gebouwen:
        p.append({"type": "guhs:buiten_gebouwen"})
    return p


def lucht_op_grond():
    return {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
        {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
        {"type": "minecraft:matching_block_tag", "offset": [0, -1, 0], "tag": "guhs:onderwol/oppervlakgrond"}]}}


def mix(a, b, t):
    ca, cb = [int(a[i:i + 2], 16) for i in (1, 3, 5)], [int(b[i:i + 2], 16) for i in (1, 3, 5)]
    return "#" + "".join(f"{round(x + (y - x) * t):02x}" for x, y in zip(ca, cb))


# =====================================================================================================================
# 9. the little trees and bushes (vanilla tree features with the mod's wood)
# =====================================================================================================================
ONDER_STAM = {"type": "minecraft:rule_based_state_provider", "rules": [   # (dirt under the trunk only where it grows on grass)
    {"if_true": {"type": "minecraft:matching_blocks", "blocks": "minecraft:grass_block"}, "then": st("minecraft:dirt")}]}


def blad(naam):
    return {"Name": naam, "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}


def boompje(stam, blad_, bijen=0.0):
    return {"type": "minecraft:tree", "config": {
        "below_trunk_provider": ONDER_STAM,
        "decorators": [{"type": "minecraft:beehive", "probability": bijen}] if bijen else [],
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "height": 3, "offset": 0, "radius": 2},
        "foliage_provider": {"type": "minecraft:simple_state_provider", "state": blad(blad_)},
        "ignore_vines": True,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 3, "height_rand_a": 1, "height_rand_b": 1},
        "trunk_provider": st(stam, axis="y")}}


def struikje():
    return {"type": "minecraft:tree", "config": {
        "below_trunk_provider": ONDER_STAM,
        "decorators": [],
        "foliage_placer": {"type": "minecraft:bush_foliage_placer", "height": 2, "offset": 1, "radius": 2},
        "foliage_provider": {"type": "minecraft:weighted_state_provider", "entries": [
            {"data": blad("guhs:pluizenboom_bladeren"), "weight": 3}, {"data": blad("guhs:guhbloesem_leaves"), "weight": 2}]},
        "ignore_vines": True,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 0, "lower_size": 0, "upper_size": 0},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 1, "height_rand_a": 0, "height_rand_b": 0},
        "trunk_provider": st("guhs:pluizenboom_stam", axis="y")}}


def groepje(zeldzaam, aantal, spreiding, zaailing="guhs:guhbloesem_sapling"):
    """A few tries around one spot (each snapped back to the ground), only where a sapling could grow, never in a building."""
    return [{"type": "minecraft:rarity_filter", "chance": zeldzaam}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}, {"type": "guhs:buiten_gebouwen"},
            {"type": "minecraft:count", "count": aantal},
            {"type": "minecraft:random_offset", "xz_spread": {"type": "minecraft:trapezoid", "min": -spreiding, "max": spreiding, "plateau": 0},
             "y_spread": 0},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": {"Name": zaailing, "Properties": {"stage": "0"}}}},
            {"type": "guhs:buiten_gebouwen"}]


def bloemenplek(clusters, per_cluster, spreiding, y=2):
    return [{"type": "minecraft:count", "count": clusters}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}, {"type": "guhs:buiten_gebouwen"},
            {"type": "minecraft:count", "count": per_cluster},
            {"type": "minecraft:random_offset", "xz_spread": {"type": "minecraft:trapezoid", "min": -spreiding, "max": spreiding, "plateau": 0},
             "y_spread": {"type": "minecraft:trapezoid", "min": -y, "max": y, "plateau": 0}},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}},
            {"type": "guhs:buiten_gebouwen"}]


def features():
    cf, pf = f"{WG}/configured_feature", f"{WG}/placed_feature"
    tag("guhs/tags/block/onderwol/oppervlakgrond", GROND)
    tag("guhs/tags/worldgen/biome/onderwol/pastelwol", PASTELWOL)
    tag("guhs/tags/worldgen/biome/onderwol/viltkiezel", KIEZEL)
    tag("guhs/tags/worldgen/biome/onderwol/roze_strand", STRAND)
    tag("guhs/tags/worldgen/biome/onderwol/regenboogheuvels", REGENBOOG)
    tag("guhs/tags/worldgen/biome/onderwol/kaaslappendeken", KAAS)

    def java(naam, type_, config=None, placement=()):
        w(f"{cf}/{naam}.json", {"type": f"guhs:{type_}", "config": config or {}})
        w(f"{pf}/{naam}.json", {"feature": f"guhs:{naam}", "placement": list(placement)})
    # 1/2. the quilt and the shores: once per chunk (origin on the surface, so the biome filter looks at the surface;
    # the Java checks the biome tag per column as well)
    per_chunk = [{"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
    java("onderwol_pastelwol", "onderwol_pastelwol", placement=per_chunk)
    java("onderwol_viltkiezel", "onderwol_viltkiezel", placement=per_chunk)
    # (ronde 30: the passes for small or patchy areas run in every chunk one of their biomes touches, without the corner
    # biome filter; the Java checks the biome per column)
    elke_chunk = [{"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}]
    java("onderwol_regenboogheuvels", "onderwol_regenboogheuvels", placement=elke_chunk)
    java("onderwol_kaaslappendeken", "onderwol_kaaslappendeken", placement=elke_chunk)
    java("onderwol_bloemenvallei_grond", "onderwol_bloemenvallei_grond", placement=elke_chunk)
    # 4. wolkeien: about one per 6 chunks
    java("onderwol_wolkei", "onderwol_wolkei", placement=op_oppervlak(zeldzaam=6))
    # 5. vijvertjes (rare) and wolwatervalletjes (many tries, most fail: they need a cliff of 4+ next to them)
    java("onderwol_vijvertje", "onderwol_vijvertje", placement=op_oppervlak(zeldzaam=22))
    water = {"state": {"Name": "minecraft:water", "Properties": {"level": "0"}}}
    saus = {"state": {"Name": "guhs:kaas_saus", "Properties": {"level": "0"}}}
    java("onderwol_wolwaterval", "onderwol_waterval", water, op_oppervlak(aantal=10))
    w(f"{cf}/onderwol_kaasval.json", {"type": "guhs:onderwol_waterval", "config": saus})
    w(f"{pf}/onderwol_wolwaterval_zacht.json", {"feature": "guhs:onderwol_wolwaterval", "placement": op_oppervlak(aantal=3)})
    w(f"{pf}/onderwol_kaasval.json", {"feature": "guhs:onderwol_kaasval", "placement": op_oppervlak(zeldzaam=2, aantal=4)})
    # 6. paddenstoelenkringen: about one per 40 chunks
    java("onderwol_paddenstoelenkring", "onderwol_paddenstoelenkring",
         placement=op_oppervlak("MOTION_BLOCKING_NO_LEAVES", zeldzaam=40, gebouwen=True))
    # 7. omgevallen stammetjes: vanilla's fallen tree with the mod's logs, mushrooms and a mossy carpet on top
    for naam, (stam, zeldzaam, tapijt) in STAMMEN.items():
        w(f"{cf}/{naam}.json", {"type": "minecraft:fallen_tree", "config": {
            "trunk_provider": st(stam, axis="y"),
            "log_length": {"type": "minecraft:uniform", "min_inclusive": 4, "max_inclusive": 7},
            "stump_decorators": [],
            "log_decorators": [
                {"type": "minecraft:attached_to_logs", "probability": 0.12, "directions": ["up"],
                 "block_provider": gewogen(("guhs:glinsterpaddenstoel", 2), ("guhs:guhpaddenstoel", 2))},
                {"type": "minecraft:attached_to_logs", "probability": 0.45, "directions": ["up"],
                 "block_provider": gewogen((tapijt, 3), ("guhs:lichtmos", 1))}]}})
        w(f"{pf}/{naam}.json", {"feature": f"guhs:{naam}", "placement": [
            *op_oppervlak("MOTION_BLOCKING_NO_LEAVES", zeldzaam=zeldzaam, gebouwen=True), lucht_op_grond()]})
    # 3. bloemenweitjes: where the (seedless) biome info noise is high, a few dense clusters per chunk; the flower mix
    # wanders with its own noise
    w(f"{cf}/onderwol_bloemenweitje.json", {"type": "minecraft:simple_block", "config": {"to_place": {
        "type": "minecraft:noise_threshold_provider", "seed": 6402, "noise": {"firstOctave": 0, "amplitudes": [1.0]}, "scale": 0.012,
        "threshold": -0.4, "high_chance": 0.4, "default_state": {"Name": "guhs:roze_guhbloem"},
        "low_states": [{"Name": n} for n in BLOEMEN_LAAG], "high_states": [{"Name": n} for n in BLOEMEN_HOOG]}}})
    w(f"{cf}/onderwol_weitjesgras.json", {"type": "minecraft:simple_block", "config": {"to_place": st("guhs:roze_gras")}})

    def weitje(dicht, spreiding):
        return [{"type": "minecraft:noise_based_count", "noise_to_count_ratio": 4, "noise_factor": 48.0, "noise_offset": -0.3},
                {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"},
                {"type": "guhs:buiten_gebouwen"}, {"type": "minecraft:count", "count": dicht},
                {"type": "minecraft:random_offset",
                 "xz_spread": {"type": "minecraft:trapezoid", "min": -spreiding, "max": spreiding, "plateau": 0},
                 "y_spread": {"type": "minecraft:trapezoid", "min": -2, "max": 2, "plateau": 0}},
                {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                    {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                    {"type": "minecraft:would_survive", "state": {"Name": "guhs:roze_guhbloem"}}]}},
                {"type": "guhs:buiten_gebouwen"}]
    w(f"{pf}/onderwol_bloemenweitje.json", {"feature": "guhs:onderwol_bloemenweitje", "placement": weitje(48, 5)})
    w(f"{pf}/onderwol_weitjesgras.json", {"feature": "guhs:onderwol_weitjesgras", "placement": weitje(28, 6)})

    # ---- ronde 29/30 ---------------------------------------------------------------------------------------------------
    # 9. boompjes & struikjes: a group of 2-4 little trees about once per 14 / 18 chunks, a flowering bush (1-2) about one
    # per 3 chunks, a hedge about one per 6
    w(f"{cf}/onderwol_boompje_bloesem.json", boompje("guhs:guhbloesem_log", "guhs:guhbloesem_leaves"))
    w(f"{cf}/onderwol_boompje_pluis.json", boompje("guhs:pluizenboom_stam", "guhs:pluizenboom_bladeren"))
    w(f"{cf}/onderwol_boompje_bijen.json", boompje("guhs:guhbloesem_log", "guhs:guhbloesem_leaves", bijen=0.5))
    w(f"{cf}/onderwol_struikje.json", struikje())
    twee_vier = {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 4}
    w(f"{pf}/onderwol_boomgroepje_bloesem.json", {"feature": "guhs:onderwol_boompje_bloesem", "placement": groepje(14, twee_vier, 5)})
    w(f"{pf}/onderwol_boomgroepje_pluis.json", {"feature": "guhs:onderwol_boompje_pluis", "placement": groepje(18, twee_vier, 5)})
    w(f"{pf}/onderwol_struikje.json", {"feature": "guhs:onderwol_struikje",
                                       "placement": groepje(3, {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}, 4)})
    java("onderwol_heg", "onderwol_heg", placement=op_oppervlak("MOTION_BLOCKING_NO_LEAVES", zeldzaam=6, gebouwen=True))
    # 10. eilandjes: one try per 5 chunks (only works on shallow water with room around it)
    java("onderwol_eilandje", "onderwol_eilandje", placement=op_oppervlak(zeldzaam=5))
    # 11. kaasplasjes: one try per 3 chunks of the kaas biomes
    java("onderwol_kaasplasje", "onderwol_kaasplasje", placement=op_oppervlak(zeldzaam=3))
    # 12. hartjesmeertjes: rare in the meadows (one try per 45 chunks; it needs a flat spot of ~15x15), more often in the
    # Bloemenvallei
    java("onderwol_hartjesmeertje", "onderwol_hartjesmeertje", placement=op_oppervlak(zeldzaam=45))
    w(f"{pf}/onderwol_hartjesmeertje_vallei.json", {"feature": "guhs:onderwol_hartjesmeertje", "placement": op_oppervlak(zeldzaam=8)})
    # 14. the Bloemenvallei: a bee tree now and then, tall flowers, dense flowers of every colour and pink petals
    w(f"{pf}/onderwol_vallei_boompje.json", {"feature": "guhs:onderwol_boompje_bijen", "placement": groepje(2, 1, 0)})
    w(f"{cf}/onderwol_vallei_bloemen.json", {"type": "minecraft:simple_block", "config": {"to_place": gewogen(*VALLEI_BLOEMEN)}})
    w(f"{pf}/onderwol_vallei_bloemen.json", {"feature": "guhs:onderwol_vallei_bloemen", "placement": bloemenplek(8, 24, 6)})
    w(f"{cf}/onderwol_vallei_hoge_bloemen.json", {"type": "minecraft:simple_block", "config": {"to_place": gewogen(*VALLEI_HOOG)}})
    w(f"{pf}/onderwol_vallei_hoge_bloemen.json", {"feature": "guhs:onderwol_vallei_hoge_bloemen", "placement": bloemenplek(2, 10, 5, 1)})
    w(f"{cf}/onderwol_vallei_blaadjes.json", {"type": "minecraft:simple_block", "config": {"to_place": {
        "type": "minecraft:weighted_state_provider", "entries": [
            {"data": {"Name": "minecraft:pink_petals", "Properties": {"facing": f, "flower_amount": str(a)}}, "weight": 1}
            for f in ("north", "east", "south", "west") for a in (1, 2, 3, 4)]}}})
    w(f"{pf}/onderwol_vallei_blaadjes.json", {"feature": "guhs:onderwol_vallei_blaadjes", "placement": bloemenplek(3, 20, 5)})


# =====================================================================================================================
# the new blocks: roze zand, schelpje, hartjeslelie
# =====================================================================================================================
BLOKKEN = ["roze_zand", "schelpje", "hartjeslelie"]
NAMEN = {  # id: (Dutch, English)
    "roze_zand": ("Roze zand", "Pink Sand"),
    "schelpje": ("Schelpje", "Little Shell"),
    "hartjeslelie": ("Hartjeslelie", "Heart Lily Pad"),
}
DRAAI = [{}, {"y": 90}, {"y": 180}, {"y": 270}]


def textures():
    import numpy as np
    from PIL import Image
    r = np.random.default_rng(2929)
    # roze zand: soft pink grains with lighter and darker specks and a few white sparkles
    t = np.zeros((16, 16, 4), np.float32)
    basis = np.array([244, 190, 208], np.float32)
    t[..., :3] = basis + r.normal(0, 5.5, (16, 16, 1))
    for _ in range(26):
        y, x = r.integers(0, 16, 2)
        t[y, x, :3] = [253, 222, 232]
    for _ in range(22):
        y, x = r.integers(0, 16, 2)
        t[y, x, :3] = [224, 160, 186]
    for _ in range(4):
        y, x = r.integers(0, 16, 2)
        t[y, x, :3] = [255, 250, 252]
    t[..., 3] = 255
    Image.fromarray(np.clip(t, 0, 255).astype(np.uint8)).save(f"{A}/textures/block/roze_zand.png")
    # schelpje: a little pink scallop shell (a ribbed fan with a scalloped edge and two ears at the hinge)
    s = np.zeros((16, 16, 4), np.uint8)
    ax, ay = 8.0, 13.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - ax, ay - (y + 0.5)
            rr, th = np.hypot(dx, dy), np.arctan2(dy, dx)
            if dy < 0.3 or th < 0.32 or th > np.pi - 0.32:
                continue
            f = (th - 0.32) / (np.pi - 0.64) * 6.0          # six ribs across the fan
            fr = f - np.floor(f)
            rand = 7.6 - 0.9 * (abs(fr - 0.5) * 2) ** 2       # a scallop per rib
            if rr > rand:
                continue
            groef = fr < 0.18 or fr > 0.82
            k = np.array([236, 150, 184]) if groef else np.array([250, 196, 214])
            if rr > rand - 1.0 and not groef:
                k = np.array([255, 228, 238])                 # the light edge
            if dx < 0 and not groef:
                k = np.minimum(k + 6, 255)                    # light from the left
            s[y, x, :3] = k
            s[y, x, 3] = 255
    for x in range(5, 11):                                    # the ears at the hinge
        s[13, x] = [226, 140, 176, 255]
    s[14, 6:10] = [210, 124, 162, 255]
    Image.fromarray(s).save(f"{A}/textures/block/schelpje.png")


def blokken():
    # roze zand: a full block, turned at random (less tiling)
    w(f"{A}/models/block/roze_zand.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/roze_zand"}})
    w(f"{A}/blockstates/roze_zand.json", {"variants": {"": [{"model": "guhs:block/roze_zand", **d} for d in DRAAI]}})
    w(f"{A}/models/item/roze_zand.json", {"parent": "guhs:block/roze_zand"})
    # schelpje: one flat face lying on the ground (cutout), turned at random
    w(f"{A}/models/block/schelpje.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
                                          "textures": {"shell": "guhs:block/schelpje", "particle": "guhs:block/schelpje"},
                                          "elements": [{"from": [0, 0.25, 0], "to": [16, 0.25, 16], "shade": False,
                                                        "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#shell"},
                                                                  "down": {"uv": [0, 16, 16, 0], "texture": "#shell"}}}]})
    w(f"{A}/blockstates/schelpje.json", {"variants": {"": [{"model": "guhs:block/schelpje", **d} for d in DRAAI]}})
    w(f"{A}/models/item/schelpje.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:block/schelpje"}})
    # hartjeslelie: the guh lily pad's look (the hearts are what makes it special)
    w(f"{A}/blockstates/hartjeslelie.json", {"variants": {"": [{"model": "guhs:block/guh_waterlelie", **d} for d in DRAAI]}})
    w(f"{A}/models/item/hartjeslelie.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:block/guh_waterlelie"}})
    for b in BLOKKEN:
        w(f"{A}/items/{b}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{b}"}})
        w(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{b}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"guhs:blocks/{b}"})
    add_tag("minecraft/tags/block/mineable/shovel", g("roze_zand"))
    add_tag("minecraft/tags/block/sand", g("roze_zand"))              # (sugar cane, cactus and dead bushes grow on it)
    add_tag("minecraft/tags/item/smelts_to_glass", g("roze_zand"))     # (melts into glass like sand)


def teksten():
    nl, en = {"biome.guhs.bloemenvallei": "Bloemenvallei"}, {"biome.guhs.bloemenvallei": "Flower Valley"}
    for b, (n, e) in NAMEN.items():
        nl[f"block.guhs.{b}"], en[f"block.guhs.{b}"] = n, e
    for taal, teksten_ in (("nl_nl", nl), ("en_us", en)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        if any(data.get(k) != v for k, v in teksten_.items()):
            data.update(teksten_)
            w(p, data)


# =====================================================================================================================
# 14. the Bloemenvallei biome and its climate entry; 15. sfeer
# =====================================================================================================================
def vreemd():
    """Feature ids of the water module (onderwol_water.py) whose biome lists don't include the Bloemenvallei."""
    try:
        from features import onderwol_water
        return {f"guhs:{n}" for lijst in onderwol_water.stappen().values() for n, bs in lijst if "guhs:bloemenvallei" not in bs}
    except Exception:   # (not importable: the two brooks, the ones with a fixed list when this was written)
        return {"guhs:onderwol_kronkelbeekje", "guhs:onderwol_bronbeekje"}


def bloemenvallei():
    weide = json.load(open(f"{WG}/biome/guh_meadows.json", encoding="utf-8"))
    d = copy.deepcopy(weide)
    attrs = d["attributes"]
    attrs["minecraft:visual/fog_color"] = VALLEI_KLEUREN["fog"]
    attrs["minecraft:visual/sky_color"] = VALLEI_KLEUREN["sky"]
    attrs["minecraft:visual/ambient_particles"] = [{"particle": {"type": "guhs:pluisje"}, "probability": 0.004},
                                                   {"particle": {"type": "guhs:bloesemblaadje"}, "probability": 0.0025}]
    d["effects"] = {**d["effects"], "grass_color": VALLEI_KLEUREN["gras"], "foliage_color": VALLEI_KLEUREN["gras"]}
    wezens = [s for s in d["spawners"]["creature"] if s["type"] != "guhs:guh_bee"]
    wezens.insert(1, {"type": "guhs:guh_bee", "weight": 45, "minCount": 2, "maxCount": 4})   # (bees!)
    d["spawners"]["creature"] = wezens
    # the meadows' features minus ours (ours come back in biomes(), in the one global order) and minus those of other
    # modules that re-append theirs at the end of a step for a fixed biome list without the valley (copied here they would
    # not be re-sorted with the meadows' copy: a feature order cycle); the ones they give every biome stay
    weg = ALLE | vreemd()
    d["features"] = [[x for x in stap if x not in weg] for stap in weide["features"]]
    w(f"{WG}/biome/bloemenvallei.json", d)

    def klimaat(dim):
        lijst = dim["generator"]["biome_source"]["biomes"]
        lijst[:] = [e for e in lijst if e["biome"] != "guhs:bloemenvallei"]
        na = next((i for i, e in enumerate(lijst) if e["biome"] == "guhs:guh_meadows"), len(lijst) - 1)
        lijst.insert(na + 1, {"biome": "guhs:bloemenvallei", "parameters": VALLEI_KLIMAAT})
    patch_json(f"{D}/dimension/guhmension.json", klimaat)
    # butterflies, ladybirds, glimguhtjes, finches and rabbits in the valley; glimguhtjes by night over the fields and the
    # sea too (only where the waterdiertjes / vogels / landdiertjes modifiers don't already add them)
    w(f"{D}/neoforge/biome_modifier/onderwol_vallei_diertjes.json", {"type": "neoforge:add_spawns", "biomes": VALLEI, "spawners": [
        {"type": "guhs:knabbelvlindertje", "weight": 24, "minCount": 2, "maxCount": 4},
        {"type": "guhs:lieveheersbeestje", "weight": 10, "minCount": 1, "maxCount": 2},
        {"type": "guhs:glimguhtje", "weight": 12, "minCount": 2, "maxCount": 4},
        {"type": "guhs:pluisvinkje", "weight": 10, "minCount": 2, "maxCount": 4},
        {"type": "guhs:guh_konijntje", "weight": 6, "minCount": 1, "maxCount": 2}]})
    w(f"{D}/neoforge/biome_modifier/onderwol_glimguhtjes.json", {"type": "neoforge:add_spawns",
                                                                   "biomes": g("guh_fields", "pink_puffs", "guh_sea"), "spawners": [
        {"type": "guhs:glimguhtje", "weight": 8, "minCount": 1, "maxCount": 3}]})


def sfeer():
    for b, (fog, sky, fog_doel, sky_doel, t) in SFEER.items():
        def kleur(d):
            d["attributes"]["minecraft:visual/fog_color"] = mix(fog, fog_doel, t)
            d["attributes"]["minecraft:visual/sky_color"] = mix(sky, sky_doel, t)
        patch_json(f"{WG}/biome/{b}.json", kleur)


def biomes():
    eigen = {}
    for stap, lijst in STAPPEN.items():
        for naam, biomes_ in lijst:
            for b in biomes_:
                eigen.setdefault(b, {}).setdefault(stap, []).append(f"guhs:{naam}")

    def voeg_toe(d, per_stap):
        f = d["features"]
        while len(f) < 11:
            f.append([])
        for stap in range(len(f)):
            f[stap] = [x for x in f[stap] if x not in ALLE]   # (re-append ours at the end: after the lakes and springs there)
        for stap, ids in per_stap.items():
            f[stap].extend(ids)
    for b, per_stap in eigen.items():
        pad = f"{WG}/biome/{b.split(':')[1]}.json"
        patch_json(pad, lambda d, ps=per_stap: voeg_toe(d, ps))
    # 8. pluisjes in the air of the meadows (merged: an existing particle of the same type is replaced, others are kept)
    for b in DEELTJES:
        def deeltjes(d):
            attrs = d.setdefault("attributes", {})
            lijst = [x for x in attrs.get("minecraft:visual/ambient_particles", []) if x.get("particle", {}).get("type") != "guhs:pluisje"]
            lijst.append({"particle": {"type": "guhs:pluisje"}, "probability": 0.003})
            attrs["minecraft:visual/ambient_particles"] = lijst
        patch_json(f"{WG}/biome/{b.split(':')[1]}.json", deeltjes)


def selfcheck():
    problemen = []

    def bestaat(bid):
        ns, n = bid.split(":")
        return ns == "minecraft" or os.path.exists(f"{A}/blockstates/{n}.json")
    gebruikt = set(GROND) | set(BLOEMEN_HOOG) | set(BLOEMEN_LAAG) | {s for s, _, _ in STAMMEN.values()} | {t for _, _, t in STAMMEN.values()} | set(g(
        "glinsterpaddenstoel", "guhpaddenstoel", "lichtmos", "roze_gras", "roze_guhbloem", "kaas_saus", "guh_waterlelie", "bloesemmeertje_riet",
        "knuffelblok_perzik", "viltkiezel", "viltsteen", "guhbloesem_log", "guhbloesem_leaves", "pluizenboom_stam", "pluizenboom_bladeren",
        "guhbloesem_sapling", "gatenkaas", "kaasmos", "kaasmos_tapijt", "kaasbloem", "knabbelroos", "guhoortjes", *BLOKKEN)) | \
        {n for n, _ in VALLEI_BLOEMEN}
    problemen += [f"block missing: {b}" for b in sorted(gebruikt) if not bestaat(b)]
    for s, _, _ in STAMMEN.values():
        if "axis=y" not in open(f"{A}/blockstates/{s.split(':')[1]}.json", encoding="utf-8").read():
            problemen.append(f"log without axis: {s}")
    for p in ("pluisje", "bloesemblaadje", "gezellig_hartje"):
        if not os.path.exists(f"{A}/particles/{p}.json"):
            problemen.append(f"particle guhs:{p} missing")
    for b in BLOKKEN:
        for pad in (f"{A}/items/{b}.json", f"{D}/loot_table/blocks/{b}.json", f"{A}/blockstates/{b}.json"):
            if not os.path.exists(pad):
                problemen.append(f"missing: {pad}")
    for t in ("roze_zand", "schelpje", "guh_waterlelie"):
        if not os.path.exists(f"{A}/textures/block/{t}.png"):
            problemen.append(f"texture missing: {t}")
    for stap, lijst in STAPPEN.items():
        for naam, biomes_ in lijst:
            pf = f"{WG}/placed_feature/{naam}.json"
            feat = json.load(open(pf, encoding="utf-8"))["feature"]
            if not os.path.exists(f"{WG}/configured_feature/{feat.split(':')[1]}.json"):
                problemen.append(f"configured feature missing: {feat}")
            for b in biomes_:
                d = json.load(open(f"{WG}/biome/{b.split(':')[1]}.json", encoding="utf-8"))
                if d["features"][stap].count(f"guhs:{naam}") != 1:
                    problemen.append(f"{b}: {naam} not (once) in step {stap}")
    # one global order: in every biome our ids of a step come in the STAPPEN order
    volgorde = {stap: [f"guhs:{n}" for n, _ in lijst] for stap, lijst in STAPPEN.items()}
    for b in {b for l in STAPPEN.values() for _, bs in l for b in bs}:
        d = json.load(open(f"{WG}/biome/{b.split(':')[1]}.json", encoding="utf-8"))
        for stap, ids in volgorde.items():
            ons = [x for x in d["features"][stap] if x in ALLE]
            if ons != [x for x in ids if x in ons]:
                problemen.append(f"{b}: step {stap} out of order")
    dim = json.load(open(f"{D}/dimension/guhmension.json", encoding="utf-8"))
    if [e["biome"] for e in dim["generator"]["biome_source"]["biomes"]].count("guhs:bloemenvallei") != 1:
        problemen.append("guhs:bloemenvallei not (once) in the biome_source")
    for taal in ("nl_nl", "en_us"):
        data = json.load(open(os.path.join(A, "lang", taal + ".json"), encoding="utf-8"))
        problemen += [f"{taal}: {k} missing" for k in ["biome.guhs.bloemenvallei"] + [f"block.guhs.{b}" for b in BLOKKEN] if k not in data]
    if problemen:
        raise SystemExit("onderwol_oppervlak self-check failed:\n  " + "\n  ".join(problemen))


def vallei_tags():
    """1.7.0: the Bloemenvallei takes ground from the fields and meadows, so it may hold what they hold (also the saved spot of
    a guaranteed building found there before the valley existed): it joins every biome tag that lists guh_fields or guh_meadows."""
    import glob
    for pad in glob.glob(os.path.join("src", "main", "resources", "data", "guhs", "tags", "worldgen", "biome", "**", "*.json"), recursive=True):
        with open(pad, encoding="utf-8") as f:
            d = json.load(f)
        waarden = d.get("values", [])
        namen = [v if isinstance(v, str) else v.get("id") for v in waarden]
        if ("guhs:guh_fields" in namen or "guhs:guh_meadows" in namen) and "guhs:bloemenvallei" not in namen:
            waarden.append("guhs:bloemenvallei")
            with open(pad, "w", encoding="utf-8", newline="\n") as f:
                json.dump(d, f, indent=2, ensure_ascii=False)
                f.write("\n")


def build():
    vallei_tags()
    textures()
    blokken()
    teksten()
    features()
    bloemenvallei()
    sfeer()
    biomes()
    selfcheck()
    print(f"onderwol_oppervlak: {len(ALLE)} placed features in {len({b for l in STAPPEN.values() for _, bs in l for b in bs})} biomes, "
          f"{len(BLOKKEN)} blocks, the Bloemenvallei")


if __name__ == "__main__":
    build()
