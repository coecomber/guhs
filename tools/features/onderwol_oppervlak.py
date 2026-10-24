"""
1.7.0 "Onder de Wol", ronde 27/28: a more cheerful surface for the common Guhmension (Java: feature/onderwol/
OnderWolOppervlak.java). The surface was mostly pink wool and kaasknabbels ("typisch guh, prima"); this adds:

  1. the quilt (onderwol_pastelwol, Java): big soft patches where the top 1-3 pink wool turns lila, white or peach;
  2. soft gravel along the water (onderwol_viltkiezel, Java): viltkiezel at the waterline of lakes, seas and saus;
  3. flower meadows (onderwol_bloemenweitje + onderwol_weitjesgras): dense noise patches of the mod's flowers and pink grass;
  4. wolkeien (onderwol_wolkei, Java): little round boulders of viltsteen / wool with a pink mossy carpet;
  5. vijvertjes (onderwol_vijvertje, Java) and wolwatervalletjes (onderwol_waterval*, onderwol_kaasval, Java): small ponds
     with lilies and reeds, and springs in hillsides that fall into a little pool;
  6. paddenstoelenkringen (onderwol_paddenstoelenkring, Java): fairy rings of glinsterpaddenstoelen, sometimes a guh inside;
  7. omgevallen stammetjes (onderwol_stammetje_*): vanilla fallen trees of the mod's own logs, with mushrooms and moss;
  8. pluisjes in the air of the meadow biomes (ambient particles guhs:pluisje).

Every step is idempotent: the biome lists get our ids (re-appended at the end of their step so they run after the
lakes/springs already there); carvers, spawners and the rest of the biomes are untouched. New chunks only.

    PYTHONPATH=tools python tools/features/onderwol_oppervlak.py        (from the worktree root)
"""
import json
import os

R = os.path.join("src", "main", "resources")
A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
WG = f"{D}/worldgen"


def g(*names):
    return [f"guhs:{n}" for n in names]


# --- which biomes get what -------------------------------------------------------------------------------------------
WOL = g("guh_fields", "guh_meadows", "pink_puffs", "kaas_flats", "knabbel_crumbs", "guh_peaks", "vads_cliffs")
WEIDES = g("guh_fields", "guh_meadows", "pink_puffs")
PASTELWOL = WOL + g("guh_sea")
KIEZEL = WOL + g("guh_sea", "mikas_biome", "knuffeldal", "vadswoud", "beverbeek")
WOLKEI = WOL + g("knuffeldal", "mikas_biome")
VIJVER = WEIDES + g("knuffeldal", "kaas_flats", "knabbel_crumbs", "vadswoud")
WATERVAL_STEIL = g("guh_peaks", "vads_cliffs")
WATERVAL_ZACHT = WEIDES + g("knuffeldal", "vadswoud", "kaas_flats", "knabbel_crumbs", "mikas_biome")
KAASVAL = g("kaas_flats", "knabbel_crumbs", "vads_cliffs")
KRING = WEIDES + g("knuffeldal", "vadswoud")
STAM = {"onderwol_stammetje_bloesem": WEIDES, "onderwol_stammetje_vads": g("vadswoud"), "onderwol_stammetje_pluis": g("knuffeldal")}
DEELTJES = WEIDES

# step -> [(placed feature, biomes)] in the order they must run
STAPPEN = {
    1: [("onderwol_vijvertje", VIJVER)],
    2: [("onderwol_pastelwol", PASTELWOL), ("onderwol_viltkiezel", KIEZEL), ("onderwol_wolkei", WOLKEI)],
    8: [("onderwol_wolwaterval", WATERVAL_STEIL), ("onderwol_wolwaterval_zacht", WATERVAL_ZACHT), ("onderwol_kaasval", KAASVAL)],
    9: [*STAM.items(), ("onderwol_paddenstoelenkring", KRING), ("onderwol_bloemenweitje", WEIDES), ("onderwol_weitjesgras", WEIDES)],
}
ALLE = {f"guhs:{n}" for lijst in STAPPEN.values() for n, _ in lijst}

# the natural surface ground these features may change or build on
GROND = ["minecraft:pink_wool", "minecraft:magenta_wool", "minecraft:white_wool", "guhs:knuffelblok_perzik", "guhs:block_of_kaasknabbels",
         "minecraft:grass_block", "minecraft:dirt", "minecraft:sand", "minecraft:clay", "minecraft:pink_concrete_powder",
         "minecraft:pink_terracotta", "minecraft:magenta_terracotta", "guhs:knuffelgras", "guhs:vadsmos", "guhs:viltkiezel", "guhs:viltsteen"]
BLOEMEN_HOOG = g("knabbelroos", "kaasbloem", "guhoortjes")
BLOEMEN_LAAG = g("roze_guhbloem", "guhoortjes", "roze_guhbloem")
STAMMEN = {"onderwol_stammetje_bloesem": ("guhs:guhbloesem_log", 9, "minecraft:pink_carpet"),
           "onderwol_stammetje_vads": ("guhs:vadshout_stam", 3, "minecraft:moss_carpet"),
           "onderwol_stammetje_pluis": ("guhs:pluizenboom_stam", 4, "minecraft:pink_carpet")}


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


def features():
    cf, pf = f"{WG}/configured_feature", f"{WG}/placed_feature"
    tag("guhs/tags/block/onderwol/oppervlakgrond", GROND)
    tag("guhs/tags/worldgen/biome/onderwol/pastelwol", PASTELWOL)
    tag("guhs/tags/worldgen/biome/onderwol/viltkiezel", KIEZEL)

    def java(naam, type_, config=None, placement=()):
        w(f"{cf}/{naam}.json", {"type": f"guhs:{type_}", "config": config or {}})
        w(f"{pf}/{naam}.json", {"feature": f"guhs:{naam}", "placement": list(placement)})
    # 1/2. the quilt and the soft gravel: once per chunk (origin on the surface, so the biome filter looks at the surface;
    # the Java checks the biome tag per column as well)
    per_chunk = [{"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
    java("onderwol_pastelwol", "onderwol_pastelwol", placement=per_chunk)
    java("onderwol_viltkiezel", "onderwol_viltkiezel", placement=per_chunk)
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
        "knuffelblok_perzik", "viltkiezel", "viltsteen"))
    problemen += [f"block missing: {b}" for b in sorted(gebruikt) if not bestaat(b)]
    for s, _, _ in STAMMEN.values():
        if "axis=y" not in open(f"{A}/blockstates/{s.split(':')[1]}.json", encoding="utf-8").read():
            problemen.append(f"log without axis: {s}")
    if not os.path.exists(f"{A}/particles/pluisje.json"):
        problemen.append("particle guhs:pluisje missing")
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
    if problemen:
        raise SystemExit("onderwol_oppervlak self-check failed:\n  " + "\n  ".join(problemen))


def build():
    features()
    biomes()
    selfcheck()
    print(f"onderwol_oppervlak: {len(ALLE)} placed features in {len({b for l in STAPPEN.values() for _, bs in l for b in bs})} biomes")


if __name__ == "__main__":
    build()
