"""
1.10 "Het Vadsrijk", round W, slice W-SCHEPEN (Java: feature/vadsrijk/luchtschip/Loopplank, on top of U-LUCHTSCHIP):

  - every airship docks flush against the open tip of its pier; step on board, click the ship or click the pier tip;
  - the safety net at an open tip (no ship there: back onto the pier, with the next departure).

No assets. Overrides U-LUCHTSCHIP's pier hint (the walk-on way is new).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_w_schepen.py
"""
import json
import os

L = "vadsrijk.luchtschip."

TEKSTEN = {
    L + "loopplank.nog_niet": ("Het luchtschip ligt hier nog niet aan. Volgende vertrek over %s: wacht even op de pier!",
                               "The airship is not here yet. Next departure in %s: wait on the pier for a moment!"),
    L + "pier.open": ("Lijn %s naar %s: vertrek over %s. ✔ Jij mag mee: loop het schip op of klik erop!",
                      "Line %s to %s: departs in %s. ✔ You may board: walk onto the ship or click it!"),
    L + "mag": ("Lijn %s naar %s: jij mag mee! Vertrek over %s. Loop aan het eind van de pier het schip op (of klik erop); aan boord kun je vrij rondlopen.",
                "Line %s to %s: you may board! Departs in %s. Walk onto the ship at the end of the pier (or click it); on board you can walk around freely."),
}


def build(h):
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, d)
    print(f"vadsrijk_w_schepen: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
