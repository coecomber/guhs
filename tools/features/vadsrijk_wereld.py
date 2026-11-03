"""
1.10.0 "Het Vadsrijk", the wereld slice (CONTRACT_VADSRIJK.md). Java: feature/vadsrijk/wereld.
Stub: the wereld slice fills TEKSTEN (keys vadsrijk.wereld.*, item/entity/block names of its own ids) and its assets.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_wereld.py
"""
import json
import os

TEKSTEN = {
}


def build(h):
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="
") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("
")


def _direct():
    a = os.path.join("src", "main", "resources")
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_wereld: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
