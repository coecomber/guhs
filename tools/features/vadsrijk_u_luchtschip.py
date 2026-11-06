"""
1.10 "Het Vadsrijk", round U, slice U-LUCHTSCHIP (Java: feature/vadsrijk/luchtschip, on top of T-LUCHTSCHIP):

  - texts for the closed-line explanations (what to do first and why, per line gate), the "may I board?" state in the action
    bar near the pier, the extra departure-board line (who may fly from here) and the board click.

No assets: the click boxes (SchipDeel) and the walkable deck (LuchtschipDek) are code only.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_luchtschip.py
"""
import json
import os

L = "vadsrijk.luchtschip."

TEKSTEN = {
    L + "dicht.kop": ("Lijn %s is nog dicht voor jou.", "Line %s is still closed for you."),
    L + "dicht.verhaal": ("Volg eerst de hoofdquest: na %s kun je hier vliegen. Je tracker rechtsboven wijst de weg.",
                          "Follow the main quest first: after %s you can fly from here. Your tracker (top right) shows the way."),
    L + "dicht.nu": ("Je bent nu bij: %s.", "You are now at: %s."),
    L + "dicht.ook_level": ("En je moet level %s zijn (jij: level %s).", "And you need to be level %s (you: level %s)."),
    L + "dicht.level": ("Het verhaal heb je al gedaan, maar deze lijn is pas vanaf level %s (jij: level %s). Nog even oefenen!",
                        "You have done the story, but this line is only from level %s (you: level %s). Practise a little more!"),
    L + "kort.verhaal": ("nog dicht voor jou: volg eerst de hoofdquest tot na %s", "still closed for you: follow the main quest until after %s"),
    L + "kort.level": ("nog dicht voor jou: pas vanaf level %s", "still closed for you: only from level %s"),
    L + "kort.dicht": ("nog dicht voor jou: volg eerst de hoofdquest", "still closed for you: follow the main quest first"),
    L + "pier.kop": ("Lijn %s:", "Line %s:"),
    L + "pier.open": ("Lijn %s naar %s: vertrek over %s. ✔ Jij mag mee: klik ergens op het schip!",
                      "Line %s to %s: departs in %s. ✔ You may board: click anywhere on the ship!"),
    L + "pier.dicht": ("Lijn %s naar %s: vertrek over %s. ✖ %s", "Line %s to %s: departs in %s. ✖ %s"),
    L + "bord.voor": ("Mee na de hoofdquest \"%s\"", "Board after the main quest \"%s\""),
    L + "bord.voor_level": ("Mee na de hoofdquest \"%s\", vanaf level %s", "Board after the main quest \"%s\", from level %s"),
    L + "bord.terug": ("Terugvliegen mag altijd", "Flying back is always allowed"),
    L + "mag": ("Lijn %s naar %s: jij mag mee! Vertrek over %s. Klik ergens op het schip om in te stappen; aan boord kun je vrij rondlopen.",
                "Line %s to %s: you may board! Departs in %s. Click anywhere on the ship to board; on board you can walk around freely."),
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
    print(f"vadsrijk_u_luchtschip: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
