"""
biomes3 slice "wereld" (Java: feature/bio/wereld; English: tools/lang/en/c81_bio_wereld.json; contract: CONTRACT_BIO.md).

The three biomes of the Guhmensie: Bloesemmeertje, Klaterdal, Wolkenweide. This module is the KERN of the slice: where the
biomes lie, the shape of their land, and the structure type guhs:bio_plek. It is the only module that edits the
Guhmensie's dimension and noise settings for these biomes; what a biome looks like (its biome file, its surface, its
plants) is in one module per biome, so three people can polish them side by side:

    bio_wereld_dal.py    the Klaterdal          (Java: DalTerrein, DalVulling)
    bio_wereld_meer.py   the Bloesemmeertje     (Java: MeerTerrein, MeerVulling)
    bio_wereld_wolk.py   the Wolkenweide        (Java: WolkTerrein, WolkVulling)
    bio_wereld_plek.py   guhs:bio_plek for other slices (plek()), and the test structures of every spot kind
    bio_wereld_eiland.py the island / cloud / lift builder for templates (wave B's structures in the Wolkenweide)

How it works (Java: BioModel). The terrain is NOT density-function JSON: a structure has to know where the river, the
waterfall and the lake island are before any block exists, so the land is worked out by a terrain model in Java, a pure
function of four noises of ours (NOISES) and the six region noises of the older biomes. Two custom density functions put
it into the world:
  - guhs:bio_terrein wraps the WHOLE final_density of the Guhmensie (router()). Outside our regions it hands the old
    value through untouched, so the rest of the dimension generates exactly as it did. NOTE for whoever adds a terrain
    region after this module: final_density is no longer {"type": "minecraft:max", ...} at the top; the old tree is its
    "input" (a later module must unwrap that, or run before this one: this module is last in FEATURES on purpose).
  - guhs:bio_regio (1 inside a biome, else 0) times SHIFT is added to the multi-noise temperature, humidity (vegetation)
    and erosion, which moves the climate to that biome's own entry in a far corner (ENTRIES). It is a hard step in whole
    columns, so there are no rings of other biomes around ours, and since the entries sit in three corners at once no
    climate outside our regions ever gets near them.
The shares of the biomes are numbers in Java (BioModel: DAL_VANAF, WEIDE_VANAF; DalTerrein.TRAP), measured by the game
test BioWereldGameTests.bioWereldAandeel.
"""
import json

from features import bio_lib as lib
from features import bio_wereld_dal as dal
from features import bio_wereld_meer as meer
from features import bio_wereld_plek as plekken
from features import bio_wereld_wolk as wolk

NAMEN = {"bloesemmeertje": "Bloesemmeertje", "klaterdal": "Klaterdal", "wolkenweide": "Wolkenweide"}
BIOMEN = {"klaterdal": dal, "bloesemmeertje": meer, "wolkenweide": wolk}

# our noises (the first four of BioModel.RUIS); the dal noise is one octave lower than the other regions' (valleys of
# several hundred blocks, so the terraces are broad)
NOISES = {
    "klaterdal_regio": {"firstOctave": -10, "amplitudes": [1.0]},
    "wolkenweide_regio": {"firstOctave": -9, "amplitudes": [1.0]},
    "klaterdal_rivier": {"firstOctave": -8, "amplitudes": [1.0]},
    "klaterdal_detail": {"firstOctave": -5, "amplitudes": [1.0, 0.5]},
}
# every noise the model reads, in the order of BioModel.RUIS (ours, then the older regions')
RUIS = [*NOISES, "guhmension_zee", "guhmension_knuffel", "guhmension_polder", "sneeuwguhtoendra", "guhwaii", "bleekwoud"]
SHIFT = 12.0
# biome -> the signs of (temperature, humidity, erosion): its entry lies at 2 x the sign on each
ENTRIES = {"bloesemmeertje": (1, 1, 1), "klaterdal": (1, -1, 1), "wolkenweide": (-1, -1, 1)}
VULLING = "bio_wereld_vulling"


def _ruis():
    return [f"guhs:{n}" for n in RUIS]


def _regio(soort):
    return {"type": "guhs:bio_regio", "soort": soort, "ruis": _ruis()}


def router(d):
    """The terrain (final_density wrapped) and the three climate shifts. Idempotent."""
    r = d["noise_router"]
    if "guhs:bio_terrein" in json.dumps(r["final_density"]):
        return
    r["final_density"] = {"type": "guhs:bio_terrein", "input": r["final_density"], "ruis": _ruis()}
    for as_nr, slot in enumerate(("temperature", "vegetation", "erosion")):
        for biome, tekens in ENTRIES.items():
            r[slot] = {"type": "minecraft:add", "argument1": r[slot],
                       "argument2": {"type": "minecraft:mul", "argument1": _regio(biome), "argument2": SHIFT * tekens[as_nr]}}


def biome_source(d):
    alles = [-2.0, 2.0]
    entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] not in [f"guhs:{b}" for b in ENTRIES]]
    for biome, (t, v, e) in ENTRIES.items():
        entries.append({"biome": f"guhs:{biome}", "parameters": {
            "temperature": 2.0 * t, "humidity": 2.0 * v, "continentalness": alles, "erosion": 2.0 * e, "weirdness": alles,
            "depth": alles, "offset": 0.0}})
    d["generator"]["biome_source"]["biomes"] = entries


def surface(h):
    """Each biome's own surface rule (bio_wereld_<biome>.surface()), appended; ours replace our earlier ones."""
    def patch(d):
        ons = [[f"guhs:{b}"] for b in ENTRIES]
        rules = [r for r in d["surface_rule"]["sequence"] if r.get("if_true", {}).get("biome_is") not in ons]
        for biome, module in BIOMEN.items():
            rule = module.surface(h, lib)
            if rule is not None:
                rules.append({"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{biome}"]},
                              "then_run": rule})
        d["surface_rule"]["sequence"] = rules
    return patch


def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    for name, noise in NOISES.items():
        w(f"{wg}/noise/{name}.json", noise)
    h.patch_json(f"{wg}/noise_settings/guhmension.json", router)
    h.patch_json(f"{wg}/noise_settings/guhmension.json", surface(h))
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)
    # the one feature that fills in water, clouds and lifts from the model, once per chunk, in all three biomes
    w(f"{wg}/configured_feature/{VULLING}.json", {"type": f"guhs:{VULLING}", "config": {}})
    w(f"{wg}/placed_feature/{VULLING}.json", {"feature": f"guhs:{VULLING}", "placement": []})
    base = json.load(open(f"{wg}/biome/pink_puffs.json", encoding="utf-8"))
    ores = base["features"][6]
    leeg = {k: [] for k in base["spawners"]}
    for biome, module in BIOMEN.items():
        b = module.biome(h, lib, ores, leeg)
        assert b["features"][1] == [f"guhs:{VULLING}"], f"{biome}: step 1 (lakes) must hold exactly guhs:{VULLING}"
        w(f"{wg}/biome/{biome}.json", b)
        lib.teksten(h, {f"biome.guhs.{biome}": NAMEN[biome]})
        module.build(h, lib)
    ids = [f"guhs:{b}" for b in BIOMEN]
    # guh villagers born here are guh villagers; the Knabbelkelders (fixed rings around spawn) may lie under these biomes
    # too; the band's favourite places
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({b: {"villager_type": "guhs:guh"} for b in ids}))
    h.add_tag("guhs/tags/worldgen/biome/has_structure/knabbelkelder", ids)
    h.add_tag("guhs/tags/worldgen/biome/band/favoriete_plekken", ids)


def build(h):
    worldgen(h)
    plekken.build(h, lib)
    selfcheck(h)


def selfcheck(h):
    D = h.D
    d = json.load(open(f"{D}/worldgen/noise_settings/guhmension.json", encoding="utf-8"))
    r = d["noise_router"]
    problems = []
    if r["final_density"].get("type") != "guhs:bio_terrein" or r["final_density"]["ruis"] != _ruis():
        problems.append("final_density is not wrapped in guhs:bio_terrein (did a later module replace it?)")
    for slot in ("temperature", "vegetation", "erosion"):
        if json.dumps(r[slot]).count("guhs:bio_regio") != 3:
            problems.append(f"{slot}: the three guhs:bio_regio terms are missing or doubled")
    dim = json.load(open(f"{D}/dimension/guhmension.json", encoding="utf-8"))
    names = [e["biome"] for e in dim["generator"]["biome_source"]["biomes"]]
    for b in ENTRIES:
        if names.count(f"guhs:{b}") != 1:
            problems.append(f"guhs:{b} is not exactly once in the Guhmensie's biome source")
    for n in RUIS:
        if not __import__("os").path.exists(f"{D}/worldgen/noise/{n}.json"):
            problems.append(f"noise guhs:{n} does not exist (an older region was renamed?)")
    if problems:
        raise SystemExit("bio_wereld self-check failed:\n  " + "\n  ".join(problems))
