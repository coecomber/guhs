"""
1.10 round U, slice U-Z6WERELD: zone 6 "Het Zijdeland" (Java: feature/vadsrijk/zone6/wereld; the map itself is
tools/vadsrijk/zone6, built by maak_kaart.py --zone 6). The blocks are vadsrijk_u_z6blokken.py.

  - data/guhs/worldgen/biome/vadsrijk_zijdeland_<gebied>.json: one biome per area ("Het Vadsrijk - <gebied>" in F3/JourneyMap),
    same shape as vadsrijk_biomen.py (the map feature guhs:vr_kaart, no natural spawns): warm rose skies, peach fog, turquoise
    water, sun-bleached grass/palm foliage; the tower grey-violet;
  - texts: the biome names, the area names (vadsrijk.kern.gebied.z6_*), the barrier messages (De Zijdewind, the closed east).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_z6wereld.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vadsrijk_biomen as VB  # noqa: E402

LUCHT, MIST = "#ffc4d8", "#ffe2cc"
WATER, WATERMIST = "#5fd8d0", "#3aa8b0"
GRAS, LOOF = "#c4d880", "#9cc860"
# biome id -> (area id, nl name, en name, sky, fog, water, water fog, grass, foliage)
BIOMEN = {
    "zijdeland_rozenkloof": ("z6_rozenkloof", "Rozenkloof", "Rose Gorge", "#ffbcd4", "#ffd8c8", WATER, WATERMIST, GRAS, LOOF),
    "zijdeland_saffraanpoort": ("z6_saffraanpoort", "Saffraanpoort", "Saffron Gate", "#ffcab8", "#ffe6c0", WATER, WATERMIST, "#b8dc78",
                                "#8cd060"),
    "zijdeland_toren": ("z6_toren", "De Sterrendovende Toren", "The Star-Snuffing Tower", "#a89cc0", "#b8a8c8", "#7890a8", "#586880",
                        "#a0a49c", "#8c9488"),
    "zijdeland_moederoase": ("z6_moederoase", "De Moederoase", "The Mother Oasis", "#ffc0e0", "#ffe0e8", "#48e0d8", "#28b0b8", "#a8e070",
                             "#7cd050"),
    "zijdeland_ruinedal": ("z6_ruinedal", "Het Ruïnedal", "The Ruin Valley", "#ffb8c0", "#f8d0c0", WATER, WATERMIST, "#c8c880", "#a8b860"),
    "zijdeland_zoutvlakte": ("z6_zoutvlakte", "De Zoutvlakte", "The Salt Flat", "#ffd0e4", "#fff0f4", WATER, WATERMIST, "#d0d8a0", "#b0c880"),
    "zijdeland_palmenkloven": ("z6_palmenkloven", "De Palmenkloven", "The Palm Gorges", "#ffc8d0", "#ffe0d0", "#50e0c8", "#30b0a0", "#a8dc70",
                               "#78cc48"),
    "zijdeland_roze_duinen": ("z6_roze_duinen", "De Roze Duinen", "The Pink Dunes", "#ffc0d4", "#ffdcd0", WATER, WATERMIST, "#d4cc88",
                              "#b4c070"),
    "zijdeland": ("z6_zijdeland", "Het Zijdeland", "The Silklands", LUCHT, MIST, WATER, WATERMIST, GRAS, LOOF),
}
TEKSTEN = {   # key -> (nl, en)
    "vadsrijk.indeling.barriere.zijdewind": ("De warme Zijdewind waait je zachtjes terug. Zonder luchtschip kom je niet over de zeestraat.",
                                             "The warm Silk Wind gently blows you back. Without an airship you cannot cross the strait."),
    "vadsrijk.indeling.barriere.oostgrens": ("Verder naar het oosten wordt het grauw en gevaarlijk. Hier houdt de Oostroute (nog) op.",
                                             "Further east it turns grey and dangerous. The East Road ends here (for now)."),
    "vadsrijk.z6wereld.welkom": ("Het Zijdeland", "The Silklands"),
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
    print(f"vadsrijk_u_z6wereld: {len(BIOMEN)} biomes, {len(teksten())} texts")


if __name__ == "__main__":
    _direct()
