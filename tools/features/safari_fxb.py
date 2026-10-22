"""
Op Safari! test round 5 (fix slice FX-B): the texts of its fixes: the Superkompas entry of the safari busstation and the
Beverbeek ranger pointing the way to it (feature/beverbeek/BeverbeekRanger.bus).

  python tools/features/safari_fxb.py     (from the project root): writes these texts into nl_nl.json and, with the
                                          English overlay tools/lang/en/c187_safari_fxb.json, into en_us.json
"""
import json
import os

EN_BESTAND = os.path.join("tools", "lang", "en", "c187_safari_fxb.json")

TEKSTEN = {
    "structure.guhs.safari_busstation": "Safari-busstation",
    "structure.guhs.safari_busstation.tooltip": "Hier vertrekt de gratis safaribus naar Safaripark Vadse Bergen (Guhmension)",
    "gui.guhs.beverbeek.ranger.bus.richting": "Safaripark Vadse Bergen? Daar rijdt de safaribus heen! Het dichtstbijzijnde busstation ligt "
                                              "ongeveer %1$s blokken naar het %2$s. Je Superkompas wijst het ook aan, onder Knus. Daar wonen "
                                              "nog veel meer bevers, bij een échte beverburcht!",
    "gui.guhs.beverbeek.ranger.richting.noord": "noorden",
    "gui.guhs.beverbeek.ranger.richting.noordoost": "noordoosten",
    "gui.guhs.beverbeek.ranger.richting.oost": "oosten",
    "gui.guhs.beverbeek.ranger.richting.zuidoost": "zuidoosten",
    "gui.guhs.beverbeek.ranger.richting.zuid": "zuiden",
    "gui.guhs.beverbeek.ranger.richting.zuidwest": "zuidwesten",
    "gui.guhs.beverbeek.ranger.richting.west": "westen",
    "gui.guhs.beverbeek.ranger.richting.noordwest": "noordwesten",
}


def build(h):
    for k, v in TEKSTEN.items():
        h.NL[k] = v


def _direct():
    with open(EN_BESTAND, encoding="utf-8") as f:
        en = json.load(f)
    for taal, extra in (("nl_nl", TEKSTEN), ("en_us", {k: en.get(k, v) for k, v in TEKSTEN.items()})):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(extra)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"safari_fxb: {len(TEKSTEN)} texts ({len([k for k in TEKSTEN if k not in en])} without English)")


if __name__ == "__main__":
    _direct()
