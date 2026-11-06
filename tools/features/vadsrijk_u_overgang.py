"""
1.10 "Het Vadsrijk", U-OVERGANG: natural zone transitions (FEEDBACK_RONDE4 "zone-overgangen moeten natuurlijker").

The water/sky/fog colour came from the biome, and the biome came from area BOXES per zone, so the open sea between two zones
changed colour along straight lines (zone 2's ring box ended at x 1004, the "De Zee" strip, then zone 3's root-field biome with
teal water from x 1024). Java (biomen.VadsrijkBiomen, U-OVERGANG block) now gives every open-sea column OUTSIDE the zones' coast
shapes a "sea band" biome between the two nearest zones: 7 steps per zone pair (1/8 .. 7/8) with interpolated water, water fog,
sky and fog colours, from one zone's sea colours (its catch-all biome = the anchor) to the other's. Together with the vanilla
biome blend the colour now drifts over the whole gap instead of jumping at a tile edge.

  - data/guhs/worldgen/biome/vadsrijk_zeeband_<a>_<b>_<k>.json (a < b in z1, z2, z3, z4; k = 1..7 = weight of b in eighths);
  - their names ("Het Vadsrijk - De Zee");
  - the viewpoint names of the new epic viewpoints (Uitkijken, W3), nl + en.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_overgang.py
"""
import json
import os

A = os.path.join("src", "main", "resources")
D = os.path.join(A, "data", "guhs")
AS = os.path.join(A, "assets", "guhs")

# the anchor biome per zone (= what VadsrijkBiomen gives just inside the zone's coast shape) and its colours
# (water, water fog, sky, fog) as in vadsrijk_biomen.py / vadsrijk_t_z4wereld.py
ANKERS = {
    "z1": ("tussenland", "#b4a8f0", "#8f7fd0", "#ffb3d1", "#ffd9c2"),
    "z2": ("splinterpieken", "#a0b8f0", "#7088c8", "#c0d8f8", "#dce8f8"),
    "z3": ("schemerwortel", "#b0a8f0", "#8f7fd0", "#ffc8a0", "#ffd8b8"),
    "z4": ("lampionkust", "#b8a4f0", "#9a86d8", "#ffb8d4", "#ffd8cc"),
}
ZONES = ["z1", "z2", "z3", "z4"]
STAPPEN = 8          # k = 1 .. STAPPEN - 1 (Java: VadsrijkBiomen.ZEEBAND_STAPPEN)
GRAS, LOOF = "#a8e07a", "#8fd46a"

# W3 viewpoints (U-OVERGANG): key -> (Dutch, English)
TEKSTEN = {
    # the name card of each viewpoint (Uitkijken.panorama: "vadsrijk.wereld.uitkijk.<id>"); these had no text yet
    "vadsrijk.wereld.uitkijk.hoedtop": ("De Reuzenhoed", "The Giant Cap"),
    "vadsrijk.wereld.uitkijk.nest": ("Het Kruinnest", "The Crown Nest"),
    "vadsrijk.wereld.uitkijk.vuurtoren": ("De Vuurtorengalerij", "The Lighthouse Gallery"),
    "vadsrijk.wereld.uitkijk.lampionduin": ("De Lampionduin", "The Lantern Dune"),
    "vadsrijk.wereld.uitkijk.pierkop": ("De Pierkop", "The Pier Head"),
    "vadsrijk.wereld.uitkijk.vliegerheuvel": ("De Vliegerheuvel", "The Kite Hill"),
}


def paren():
    return [(a, b) for i, a in enumerate(ZONES) for b in ZONES[i + 1:]]


def band_ids():
    """Same order as VadsrijkBiomen.ZEEBAND."""
    return [f"zeeband_{a}_{b}_{k}" for a, b in paren() for k in range(1, STAPPEN)]


def _rgb(h):
    h = h.lstrip("#")
    return [int(h[i:i + 2], 16) for i in (0, 2, 4)]


def _mix(c1, c2, t):
    a, b = _rgb(c1), _rgb(c2)
    return "#" + "".join(f"{round(x + (y - x) * t):02x}" for x, y in zip(a, b))


def biome(water, waterfog, sky, fog):
    return {
        "attributes": {
            "minecraft:visual/fog_color": fog,
            "minecraft:visual/sky_color": sky,
            "minecraft:visual/water_fog_color": waterfog,
        },
        "has_precipitation": False,
        "temperature": 0.8,
        "downfall": 0.0,
        "effects": {"water_color": water, "foliage_color": LOOF, "grass_color": GRAS},
        "spawners": {},
        "spawn_costs": {},
        "carvers": [],
        "features": [["guhs:vr_kaart"], [], [], [], [], [], [], [], [], [], []],
    }


def biomen():
    uit = {}
    for a, b in paren():
        ka, kb = ANKERS[a][1:], ANKERS[b][1:]
        for k in range(1, STAPPEN):
            t = k / STAPPEN
            uit[f"zeeband_{a}_{b}_{k}"] = biome(*(_mix(x, y, t) for x, y in zip(ka, kb)))
    return uit


def teksten():
    t = {f"biome.guhs.vadsrijk_{k}": ("Het Vadsrijk - De Zee", "Het Vadsrijk - The Sea") for k in band_ids()}
    t.update(TEKSTEN)
    return t


def build(h):
    for k, v in biomen().items():
        h.w(f"{h.D}/worldgen/biome/vadsrijk_{k}.json", v)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    for k, v in biomen().items():
        _schrijf(os.path.join(D, "worldgen", "biome", f"vadsrijk_{k}.json"), v)
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(AS, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: (v[0] if taal == "nl_nl" else v[1]) for k, v in teksten().items()})
        _schrijf(p, data)
    print(f"vadsrijk_u_overgang: {len(biomen())} sea-band biomes, {len(teksten())} texts")


if __name__ == "__main__":
    _direct()
