"""
1.10 fix round 2 "biomen": one biome per Vadsrijk area, so JourneyMap / F3 show "Het Vadsrijk - <plek>".
Java: feature/vadsrijk/biomen (VadsrijkBiomen = which area -> which biome, VadsrijkBiomeSource, VadsrijkGenerator).

  - data/guhs/worldgen/biome/vadsrijk_<gebied>.json: every biome carries the map feature guhs:vr_kaart (the map must still be
    placed in every chunk) and no natural spawns; sky/fog follow the area moods of wereld/sfeer/client/GebiedMist (fog colour =
    that profile's colour, so the client fog blends into it instead of fighting it);
  - water colours: the Druipgrotten blue-turquoise (the Druipschacht waterfalls), elsewhere a soft pink-blue;
  - the old biome guhs:vadsrijk (vadsrijk_kern.py) stays so chunks of older worlds still load;
  - the dimension's generator (vadsrijk_kern.DIM) is guhs:vadsrijk_kaart with biome source guhs:vadsrijk_gebieden.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_biomen.py
"""
import json
import os

# id -> (name, sky, fog, water, water fog, grass, foliage)
GRAS, LOOF = "#a8e07a", "#8fd46a"
ROZEBLAUW, ROZEBLAUW_MIST = "#b4a8f0", "#8f7fd0"
BIOMEN = {
    # zone 1 (kern Gebieden + the Kloof ravine)
    "knuffelkamp": ("Knuffelkamp", "#ffb3d1", "#ffccb3", ROZEBLAUW, ROZEBLAUW_MIST, GRAS, LOOF),
    "zwamzoet": ("Zwamzoet", "#ffb3d1", "#ffccb3", ROZEBLAUW, ROZEBLAUW_MIST, GRAS, LOOF),
    "haven": ("De Haven", "#ffbad6", "#ffd1b8", "#a4b4f0", "#7f90d0", GRAS, LOOF),
    "klodderweide": ("Klodderweide", "#ffc9b0", "#ffe080", ROZEBLAUW, ROZEBLAUW_MIST, "#b8e070", "#9ad460"),
    "sporenwoud": ("Sporenwoud", "#d0dca0", "#a8cc6b", "#8cc0c8", "#5f9a98", "#98d06a", "#7ec45a"),
    "druipgrotten": ("Druipgrotten", "#9fd6e0", "#52b8cc", "#3fc4d4", "#2a8fa0", "#8fd8b0", "#78c8a0"),
    "kloof": ("De Kloof", "#c4cad8", "#a8b3c4", "#98acd8", "#6f80a8", GRAS, LOOF),
    "zwamtempel": ("Zwamtempel", "#c8a0f0", "#9e6bdb", "#a890ec", "#7a5cc0", "#b0d880", "#98c870"),
    "tussenland": ("Tussenland", "#ffb3d1", "#ffd9c2", ROZEBLAUW, ROZEBLAUW_MIST, GRAS, LOOF),
    "zee": ("De Zee", "#ffb3d1", "#ffd9c2", "#a4b4f0", "#7f90d0", GRAS, LOOF),
    # zone 2 "De Splinterpieken" (Z2WereldPlekken.GEBIEDEN)
    "kandijkom": ("Kandijkom", "#ffc4e0", "#ffd6e8", ROZEBLAUW, ROZEBLAUW_MIST, GRAS, LOOF),
    "galmhellingen": ("Galmhellingen", "#b8d8ff", "#d8e8ff", "#9fc0f0", "#7090c8", "#a0d890", "#88c880"),
    "knuffelmeer": ("Bevroren Knuffelmeer", "#c8e8ff", "#e0f0ff", "#8fd0f0", "#68a8d0", "#b0dcc0", "#98d0b0"),
    "splinterkloof": ("Splinterkloof", "#a8b0d0", "#b8bcd8", "#8fa0d8", "#6070a8", "#98c890", "#80b880"),
    "spiegelburcht": ("Spiegelburcht", "#d0c0ff", "#e0d8ff", "#b0a8f8", "#8070d0", GRAS, LOOF),
    "splinterpieken": ("De Splinterpieken", "#c0d8f8", "#dce8f8", "#a0b8f0", "#7088c8", GRAS, LOOF),
    # zone 3 "De Schemerwortel" (z3-systemen Z3Lagen.GEBIEDEN): LAYERED, the biome source picks these by height too
    "honingtak": ("Honingtak", "#ffc890", "#ffd8a8", "#f0c070", "#c89040", "#b8e070", "#e0b850"),
    "kruinzee": ("De Kruinzee", "#ffd0a0", "#ffe0b8", "#f0c878", "#c89848", "#c0e070", "#f0b8d0"),
    "takkenbanen": ("De Takkenbanen", "#f0b890", "#e8c098", "#d8a870", "#a87840", "#a8d070", "#c8a050"),
    "schorsstad": ("De Schorsstad", "#c88860", "#a87050", "#e0a070", "#906040", "#a0b860", "#b88848"),
    "wortelvelden": ("De Wortelvelden", "#a8b8a0", "#8aa898", "#58c8c0", "#30908a", "#78b860", "#68a858"),
    "wortelduister": ("Het Wortelduister", "#4a3868", "#3a2c58", "#50c8c8", "#285858", "#6a9870", "#5a8860"),
    "zeppelinwerf": ("De Kaaszeppelinwerf", "#c0b8b0", "#b0a8a8", "#a8b0c8", "#788098", "#a0b880", "#90a870"),
    "hart": ("Het Hart van de Boom", "#6a3870", "#582a60", "#e070c0", "#903880", "#7aa070", "#6a9060"),
    "schemerwortel": ("De Schemerwortel", "#ffc8a0", "#ffd8b8", "#b0a8f0", "#8f7fd0", GRAS, LOOF),
}


def biome(sky, fog, water, waterfog, gras, loof):
    return {
        "attributes": {
            "minecraft:visual/fog_color": fog,
            "minecraft:visual/sky_color": sky,
            "minecraft:visual/water_fog_color": waterfog,
        },
        "has_precipitation": False,
        "temperature": 0.8,
        "downfall": 0.0,
        "effects": {"water_color": water, "foliage_color": loof, "grass_color": gras},
        "spawners": {},
        "spawn_costs": {},
        "carvers": [],
        "features": [["guhs:vr_kaart"], [], [], [], [], [], [], [], [], [], []],
    }


def teksten():
    return {f"biome.guhs.vadsrijk_{k}": f"Het Vadsrijk - {v[0]}" for k, v in BIOMEN.items()}


def build(h):
    for k, v in BIOMEN.items():
        h.w(f"{h.D}/worldgen/biome/vadsrijk_{k}.json", biome(*v[1:]))
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    for k, v in BIOMEN.items():
        _schrijf(os.path.join(d, "worldgen", "biome", f"vadsrijk_{k}.json"), biome(*v[1:]))
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(teksten())
        _schrijf(p, data)
    print(f"vadsrijk_biomen: {len(BIOMEN)} biomes")


if __name__ == "__main__":
    _direct()
