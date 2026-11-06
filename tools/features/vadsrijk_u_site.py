"""
1.10.0 "Het Vadsrijk", round U, U-SITE: the live map of guhs.nl/vadsrijk (posities.json) and its opt-out command
(/guhs vadsrijk kaart verberg | toon). Java: feature/vadsrijk/web (WebPosities, WebStats, WebRegistry). Texts only (nl + real English).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_site.py
"""
import json
import os

P = "vadsrijk.web.kaart."


def teksten():
    """key -> (nl, en)"""
    return {
        P + "verborgen": ("Je staat niet meer op de live kaart van guhs.nl. Terug erop? /guhs vadsrijk kaart toon",
                          "You are no longer on the live map of guhs.nl. Back on it? /guhs vadsrijk kaart toon"),
        P + "getoond": ("Je staat weer op de live kaart van guhs.nl (niet in kerkers, huisjes en de Bondenvallei). Weg ermee? /guhs vadsrijk kaart verberg",
                        "You are on the live map of guhs.nl again (never in dungeons, houses or the Bondenvallei). Hide? /guhs vadsrijk kaart verberg"),
    }


def build(h):
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for k, v in t.items():
            data[k] = v[i]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    _direct()
