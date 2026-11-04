"""Het Vadsrijk 1.10, fix round 1: the combat feel (Java: feature/vadsrijk/kern/beleving).

Only texts (all Dutch, en_us = the same Dutch). The Java code uses translatableWithFallback, so it also works without these.
Run standalone from the project root:  PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_beleving.py
"""
import json
import os

K = "vadsrijk.kern.beleving."
TEKSTEN = {
    K + "zwak": "Zwak!",
    K + "taai": "Taai",
    K + "reeks": "Vrolijke reeks",
    K + "reeks_weg": "Reeks voorbij: ×%s",
    K + "opgevrolijkt": "Opgevrolijkt!",
    K + "vond": "%s vond %s!",
    # phase names (the bazen slice may add per boss: vadsrijk.kern.beleving.fase.<entity id path>.<fase>)
    K + "fase.vr_klodderreus.2": "Fase 2: De reus wordt boos!",
    "guhs.configuration.vadsrijkSchud": "Vadsrijk: schermschud en hit-stop (%)",
    "guhs.configuration.vadsrijkSchud.tooltip": "Hoe hard het scherm schudt en de camera meebeweegt bij zware klappen en baasmomenten. 0 = uit.",
}


def build(h):
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_beleving: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
