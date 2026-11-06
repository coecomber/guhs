"""
1.10 night round T, slice T-WOLKWAL: the zone-5 leftovers (Java: feature/vadsrijk/zone5/wolkwal).

  - the models/textures/animations (Wolkwacht-ridder in 6 furs, the Sparringridder version, Moeder Wolkwal's moving fin and fluke
    tips) are made by tools/vadsrijk/wolkwal/maak_modellen.py (run here too, so a full resource build keeps them in step);
  - texts: the two panoramas (Ooggaarde, Staartpark).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_wolkwal.py
"""
import importlib.util
import json
import os

TEKSTEN = {   # key -> (nl, en)
    "vadsrijk.wolkwal.uitzicht.z5_ooggaarde": ("De Ooggaarde", "The Eye Garden"),
    "vadsrijk.wolkwal.uitzicht.z5_ooggaarde.onder": ("Hier kijkt Moeder Wolkwal vooruit, de wolken in.",
                                                     "Here Mother Cloudwhale looks ahead, into the clouds."),
    "vadsrijk.wolkwal.uitzicht.z5_staartpark": ("Het Staartpark", "Tail Park"),
    "vadsrijk.wolkwal.uitzicht.z5_staartpark.onder": ("Haar staart wuift zacht: heel Hoogvads zwemt met haar mee.",
                                                      "Her tail waves softly: all of Hoogvads swims along with her."),
}


def _modellen():
    pad = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "vadsrijk", "wolkwal", "maak_modellen.py")
    spec = importlib.util.spec_from_file_location("wolkwal_maak_modellen", pad)
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    m.main()


def build(h):
    _modellen()
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)    # make_v2.lang(key, en, nl)


def _direct():
    _modellen()
    a = os.path.join("src", "main", "resources", "assets", "guhs", "lang")
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(a, f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print(f"vadsrijk_t_wolkwal: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
