"""
1.10 night round T, slice T-Z5WERELD: zone 5 "Hoogvads" (the capital on Moeder Wolkwal; Java: feature/vadsrijk/zone5/wereld,
the map itself is tools/vadsrijk/zone5, built by maak_kaart.py --zone 5). The blocks are vadsrijk_t_z5blokken.py.

  - data/guhs/worldgen/biome/vadsrijk_<id>.json: one biome per district ("Het Vadsrijk - Hoogvads <wijk>" in F3/JourneyMap),
    same shape as vadsrijk_biomen.py (the map feature guhs:vr_kaart, no natural spawns); a bright sky high above the clouds,
    golden-pink fog, the whale itself bluer;
  - texts: the biome names, the area names (vadsrijk.kern.gebied.z5_*), the Wolkvangnet message.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z5wereld.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vadsrijk_biomen as VB  # noqa: E402

LUCHT, MIST = "#a8d4ff", "#ffe0ec"
WATER, WATERMIST = "#9ad0ff", "#7aa8e0"
GRAS, LOOF = "#a8e88a", "#9ae07a"
# biome id -> (area id, nl name, en name, sky, fog, water, water fog, grass, foliage)
BIOMEN = {
    "hoogvads_paleis": ("z5_paleisheuvel", "Hoogvads - de Paleisheuvel", "Hoogvads - Palace Hill", "#b0d8ff", "#ffe4c8", WATER, WATERMIST, GRAS, LOOF),
    "hoogvads_universiteit": ("z5_universiteit", "Hoogvads - de Universiteit", "Hoogvads - the University", LUCHT, MIST, WATER, WATERMIST, GRAS, LOOF),
    "hoogvads_academie": ("z5_academie", "Hoogvads - de Academie van de Wolkwacht", "Hoogvads - Academy of the Cloud Guard", LUCHT, "#f4e4f0",
                          WATER, WATERMIST, GRAS, LOOF),
    "hoogvads_markt": ("z5_marktlaan", "Hoogvads - de Marktlaan", "Hoogvads - Market Lane", "#b4d8ff", "#ffdce4", WATER, WATERMIST, GRAS, LOOF),
    "hoogvads_kathedraal": ("z5_kathedraal", "Hoogvads - de Ordekathedraal", "Hoogvads - the Order Cathedral", "#bcd4ff", "#ffe8d8", WATER,
                            WATERMIST, GRAS, LOOF),
    "hoogvads_luchthaven": ("z5_luchthaven", "Hoogvads - de Luchthaven", "Hoogvads - the Airport", "#9ccaff", "#e8f0ff", WATER, WATERMIST, GRAS, LOOF),
    "hoogvads_staartpark": ("z5_staartpark", "Hoogvads - het Staartpark", "Hoogvads - Tail Park", LUCHT, "#ffd8ec", WATER, WATERMIST, "#b4ec8a",
                            "#ffb8d8"),
    "hoogvads_ooggaarde": ("z5_ooggaarde", "Hoogvads - de Ooggaarde", "Hoogvads - the Eye Garden", LUCHT, "#ffd8ec", WATER, WATERMIST, "#b4ec8a",
                           "#ffb8d8"),
    "hoogvads": ("z5_hoogvads", "Hoogvads", "Hoogvads", LUCHT, MIST, WATER, WATERMIST, GRAS, LOOF),
    "moeder_wolkwal": ("z5_moeder_wolkwal", "Moeder Wolkwal", "Mother Cloudwhale", "#94c4ff", "#dce8ff", WATER, WATERMIST, GRAS, LOOF),
}
TEKSTEN = {   # key -> (nl, en)
    "vadsrijk.z5wereld.vangnet": ("Een zacht wolkje vangt je op en zet je terug op Moeder Wolkwal.",
                                  "A soft little cloud catches you and sets you back on Mother Cloudwhale."),
}


def teksten():
    t = {}
    for k, v in BIOMEN.items():
        t[f"biome.guhs.vadsrijk_{k}"] = (f"Het Vadsrijk - {v[1]}", f"Het Vadsrijk - {v[2]}")
        t[f"vadsrijk.kern.gebied.{v[0]}"] = (v[1], v[2])
    t.update(TEKSTEN)
    return t


def build(h):
    for k, v in BIOMEN.items():
        h.w(f"{h.D}/worldgen/biome/vadsrijk_{k}.json", VB.biome(*v[3:]))
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)    # make_v2.lang(key, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    for k, v in BIOMEN.items():
        VB._schrijf(os.path.join(d, "worldgen", "biome", f"vadsrijk_{k}.json"), VB.biome(*v[3:]))
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in teksten().items()})
        VB._schrijf(p, data)
    print(f"vadsrijk_t_z5wereld: {len(BIOMEN)} biomes, {len(teksten())} texts")


if __name__ == "__main__":
    _direct()
