"""
Guhs 1.10.4 (branch xp-rem): texts of the xp brake against much lower targets (kern.Spelers.geefXpDoel) and of the dungeon's
last-creatures tracker line (grotkerker.GrotKerker.volgRegel).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_xprem.py
"""
import json
import os


def teksten():
    return {
        # the popup when the brake cut a gain (at most once per 30 s): the xp you really got
        "vadsrijk.kern.xp_rem": ("+%s xp (te laag level)", "+%s xp (level too low)"),
        # the quest tracker line in a running cave with 3 or fewer creatures left (the client adds the arrow + distance)
        "vadsrijk.grotkerker.volg.over": ("Nog %s over", "%s left"),
        "vadsrijk.grotkerker.volg.waar": ("De laatste wezens lichten op, ook door muren heen.", "The last creatures glow, even through walls."),
    }


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
    print(f"vadsrijk_xprem: {len(t)} texts")


if __name__ == "__main__":
    _direct()
