"""
1.10 "Het Vadsrijk", T-WERELD (wereldindeling W-3): the texts of the connected world layout. Java: feature/vadsrijk/indeling
(VadsrijkZones = the zone offsets, Barrieres = the soft push-back at the zone barriers, ZeeVulling = the procedural sea).

  - the new viewpoints (Noordkliftop, Kandijkom-balkon, Galmkam, Westtak-uitkijk, Oosttak-uitkijk);
  - the barrier messages (what pushes you back: the current, the wind, the mist) with real English;
  - the harbour names (shared POI ids haven_<van>_<naar>_vertrek; the flight itself is t-luchtschip's).

The map itself: tools/vadsrijk/maak_kaart.py (indeling.py, barriere.py, havens.py).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_wereld.py
"""
import json
import os

A = os.path.join("src", "main", "resources")
AS = os.path.join(A, "assets", "guhs")
W = "vadsrijk.wereld."
B = "vadsrijk.indeling."

# key -> (Dutch, English)
TEKSTEN = {
    # viewpoints at the transitions (Uitkijken: title card + chat line)
    W + "uitkijk.noordkliftop": ("De Noordkliftop", "The North Cliff Top"),
    W + "uitkijk.kandijkom_balkon": ("Het Kandijkom-balkon", "The Kandijkom Balcony"),
    W + "uitkijk.galmkam": ("De Galmkam", "The Echo Ridge"),
    W + "uitkijk.westtak": ("De Westtak-uitkijk", "The West Branch Lookout"),
    W + "uitkijk.oosttak": ("De Oosttak-uitkijk", "The East Branch Lookout"),
    # the barriers (action bar while you are pushed back)
    B + "barriere.stroming": ("De Zoete-Zeestroom duwt je terug. Alleen een luchtschip komt hier voorbij.",
                              "The Sweet Sea current pushes you back. Only an airship gets past here."),
    B + "barriere.wand": ("De Splinterwand is te steil en de wind te hard. Neem het luchtschip vanaf de Noordklifhaven.",
                          "The Splinter Wall is too steep and the wind too strong. Take the airship from the North Cliff Harbour."),
    B + "barriere.kloof": ("De Wolkenkloof is te diep en te breed. Het luchtschip vanaf het Galmdok vliegt eroverheen.",
                           "The Cloud Gorge is too deep and too wide. The airship from the Echo Dock flies across."),
    B + "barriere.wolkenval": ("De Wolkenval en het tij duwen je terug. Neem het luchtschip vanaf de Oosttaksteiger.",
                               "The Cloudfall and the tide push you back. Take the airship from the East Branch Pier."),
    B + "barriere.mist": ("Een dikke grauwe mist... je raakt de weg kwijt en drijft terug.",
                          "A thick grey mist... you lose your way and drift back."),
    B + "barriere.wind": ("Een harde windmuur blaast je terug. Hier kom je alleen met een luchtschip voorbij.",
                          "A strong wall of wind blows you back. Only an airship gets past here."),
    # harbours (names for boards / the travel map)
    B + "haven.haven_z1_z2_vertrek": ("Noordklifhaven", "North Cliff Harbour"),
    B + "haven.haven_z2_z1_vertrek": ("Kandijkom-dok", "Kandijkom Dock"),
    B + "haven.haven_z2_z3_vertrek": ("Galmdok", "Echo Dock"),
    B + "haven.haven_z3_z2_vertrek": ("Weststeiger", "West Pier"),
    B + "haven.haven_z3_z4_vertrek": ("Oosttaksteiger", "East Branch Pier"),
    B + "haven.haven_z4_z3_vertrek": ("Lampionhaven", "Lantern Harbour"),
    # dev command feedback
    B + "dev.tp": ("Naar %s (%s).", "To %s (%s)."),
    B + "dev.onbekend": ("Onbekende plek: %s", "Unknown place: %s"),
    B + "dev.barriere": ("Barrières: %s", "Barriers: %s"),
}


def _schrijf(pad, obj):
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def build(h):
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)   # (T-INT: make_v2.lang(key, en, nl))


def _direct():
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(AS, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: (v[0] if taal == "nl_nl" else v[1]) for k, v in TEKSTEN.items()})
        _schrijf(p, d)
    print(f"vadsrijk_t_wereld: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
