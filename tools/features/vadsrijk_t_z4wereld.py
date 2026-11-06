"""
1.10 night round T, slice T-Z4WERELD: zone 4 "De Lampionkust" (Java: feature/vadsrijk/zone4/wereld; the map itself is
tools/vadsrijk/zone4, built by maak_kaart.py --zone 4).

  - data/guhs/worldgen/biome/vadsrijk_<island>.json: one biome per island ("Het Vadsrijk - <island>" in F3/JourneyMap), same
    shape as vadsrijk_biomen.py (the map feature guhs:vr_kaart, no natural spawns); pink-lilac sea water, soft fog; the
    Mysterie-eiland and the Wrak greyer;
  - texts: the biome names, the area names (vadsrijk.kern.gebied.z4_*), the Kringstroom message.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z4wereld.py
"""
import json
import os

import vadsrijk_biomen as VB

ZEEWATER, ZEEMIST = "#b8a4f0", "#9a86d8"
GRAS, LOOF = "#a8e07a", "#8fd46a"
# id -> (nl name, en name, sky, fog, water, water fog, grass, foliage)
BIOMEN = {
    "lampionhaven": ("Lampionhaven", "Lampionhaven", "#ffb8d4", "#ffd4c4", ZEEWATER, ZEEMIST, GRAS, LOOF),
    "knuffeltij": ("Het Knuffeltij-strand", "Cuddle Tide Beach", "#ffbcd8", "#ffd8cc", ZEEWATER, ZEEMIST, GRAS, LOOF),
    "strandfeest": ("Strandfeesteiland", "Beach Party Island", "#ffb0c8", "#ffd0b0", ZEEWATER, ZEEMIST, GRAS, LOOF),
    "bloemeneiland": ("Bloemeneiland", "Flower Island", "#ffc0dc", "#ffdcd0", ZEEWATER, ZEEMIST, "#b8e880", "#a0dc70"),
    "koraalbaai": ("De Koraalbaai", "Coral Bay", "#ffb4d8", "#ffd0dc", "#f0a0d0", "#c070a8", GRAS, LOOF),
    "vliegereiland": ("Vliegereiland", "Kite Island", "#c8dcff", "#e0ecff", ZEEWATER, ZEEMIST, "#a8e08a", "#90d47a"),
    "vuurtoreneiland": ("Vuurtoreneiland", "Lighthouse Island", "#ffc4d0", "#ffdcd4", ZEEWATER, ZEEMIST, GRAS, LOOF),
    "pierkop": ("Pierkop en de Opstijgpier", "Pierhead and the Ascent Pier", "#d4e4ff", "#ecf2ff", ZEEWATER, ZEEMIST, GRAS, LOOF),
    "mysterie": ("Het Mysterie-eiland", "Mystery Island", "#b8b4c0", "#a8a8b0", "#9aa0b8", "#707888", "#98b088", "#88a078"),
    "schildpadeiland": ("Het Schildpadeiland", "Turtle Island", "#ffb8d4", "#ffd4c4", ZEEWATER, ZEEMIST, "#a0e080", "#88d070"),
    "wrak": ("Het Wrak van de Vliegervanger", "Wreck of the Kite Catcher", "#c0b8c8", "#b8b0b8", "#a0a0c8", "#787898", GRAS, LOOF),
    "lampionkust": ("De Lampionkust", "The Lantern Coast", "#ffb8d4", "#ffd8cc", ZEEWATER, ZEEMIST, GRAS, LOOF),
}
GEBIEDEN = {   # area id -> biome id (the names are the same)
    "z4_lampionhaven": "lampionhaven", "z4_knuffeltij": "knuffeltij", "z4_strandfeest": "strandfeest",
    "z4_bloemeneiland": "bloemeneiland", "z4_koraalbaai": "koraalbaai", "z4_vliegereiland": "vliegereiland",
    "z4_vuurtoreneiland": "vuurtoreneiland", "z4_pierkop": "pierkop", "z4_mysterie": "mysterie", "z4_schildpad": "schildpadeiland",
    "z4_wrak": "wrak", "z4_zee": "lampionkust",
}
TEKSTEN = {   # key -> (nl, en)
    "vadsrijk.z4wereld.kringstroom": ("De Kringstroom duwt je zachtjes terug naar de eilandjes...",
                                      "The Ring Current gently pushes you back towards the islands..."),
}


def teksten():
    t = {}
    for k, v in BIOMEN.items():
        t[f"biome.guhs.vadsrijk_{k}"] = (f"Het Vadsrijk - {v[0]}", f"Het Vadsrijk - {v[1]}")
    for g, b in GEBIEDEN.items():
        t[f"vadsrijk.kern.gebied.{g}"] = (BIOMEN[b][0], BIOMEN[b][1])
    t.update(TEKSTEN)
    return t


def build(h):
    for k, v in BIOMEN.items():
        h.w(f"{h.D}/worldgen/biome/vadsrijk_{k}.json", VB.biome(*v[2:]))
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)   # (T-INT: make_v2.lang(key, en, nl))


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    for k, v in BIOMEN.items():
        VB._schrijf(os.path.join(d, "worldgen", "biome", f"vadsrijk_{k}.json"), VB.biome(*v[2:]))
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in teksten().items()})
        VB._schrijf(p, data)
    print(f"vadsrijk_t_z4wereld: {len(BIOMEN)} biomes, {len(teksten())} texts")


if __name__ == "__main__":
    _direct()
