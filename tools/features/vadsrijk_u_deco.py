"""
1.10 "Het Vadsrijk", round U, slice U-DECO (feedback round 4): the texts of the name boards over the landings of the spiral
stair in the tree (Java: feature/vadsrijk/udeco/Verdiepingborden). The map side of U-DECO (lamp posts, Honingtak street deco,
the Kruinroofvogel arena underside, the stair colour bands, the turtle) lives in the map generators, not here.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_deco.py
"""
import json
import os

V = "vadsrijk.udeco.verdieping."


def teksten():
    """key -> (nl, en)."""
    t = {V + "nummer": ("Verdieping %s van %s", "Floor %s of %s")}
    vloeren = [
        ("Schorsstad", "Schorsstad", "↑ Honingtak", "↑ Honingtak"),
        ("Honingtak", "Honingtak", "↑ Kruin 1    ↓ Schorsstad", "↑ Crown 1    ↓ Schorsstad"),
        ("Kruin 1", "Crown 1", "↑ Kruin 2    ↓ Honingtak", "↑ Crown 2    ↓ Honingtak"),
        ("Kruin 2", "Crown 2", "↑ Kruin 3    ↓ Kruin 1", "↑ Crown 3    ↓ Crown 1"),
        ("Kruin 3", "Crown 3", "↑ Het Nest    ↓ Kruin 2", "↑ The Nest    ↓ Crown 2"),
        ("Het Nest", "The Nest", "↓ Kruin 3 (de trap af)", "↓ Crown 3 (down the stairs)"),
    ]
    for i, (nl, en, nl_r, en_r) in enumerate(vloeren):
        t[f"{V}{i + 1}.naam"] = (nl, en)
        t[f"{V}{i + 1}.richting"] = (nl_r, en_r)
    return t


def build(h):
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs", "lang")
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            raw = f.read()
        data = json.loads(raw)
        nieuw = {k: v[i] for k, v in t.items() if data.get(k) != v[i]}
        if not nieuw:
            continue
        # add-only, keeping the file's own formatting: insert before the closing brace
        regels = "".join(f',\n  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in nieuw.items()
                         if k not in data)
        if regels:
            raw = raw.rstrip()
            assert raw.endswith("}")
            raw = raw[:-1].rstrip() + regels + "\n}\n"
        for k, v in nieuw.items():
            if k in data:
                raise SystemExit(f"{k} exists with another text; update it by hand")
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write(raw)
    print(f"vadsrijk_u_deco: {len(t)} texts")


if __name__ == "__main__":
    _direct()
