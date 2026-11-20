"""
1.10.10: the Erfenis Muziekdoosje plays a dance tune (Java: feature/erfenis/ErfenisMuziek + slaapzorg/SlaapBlokken.Speeldoos).

build(h) writes the sounds.json entry of the tune (the OGG is made once by erfenis_muziek_geluid.py and committed), the texts
(Dutch in nl_nl; the English is in tools/lang/en/c512_erfenis_muziek.json, which _direct() writes, so a full make_resources
run keeps it) and checks that every Muziektent level (vormen/muziektent/L1..L10, exported by
renders-erfenis/_bron/export_vormen.py from kern.py) has its Muziekdoosje, quiet (speelt=0: a box stamped as "playing" has
no tick waiting and used to ignore clicks).

Standalone (no full regeneration; from the project root):  PYTHONPATH=tools python tools/features/erfenis_muziek.py
"""
import json
import os
import sys

SOUNDS = {"muziekdoosje.dansje": ["dansje"]}

# (Dutch, English)
TEKSTEN = {
    "subtitles.guhs.muziekdoosje.dansje": ("Muziekdoosje speelt een guhdansje", "Music Box plays a guh dance"),
    "gui.guhs.muziekdoosje.dansen": ("♪ Iedereen dansen, vadsjes! ♪", "♪ Everybody dance, vadsjes! ♪"),
    "gui.guhs.muziekdoosje.speelt_al": ("Het muziekdoosje speelt al!", "The music box is already playing!"),
    "gui.guhs.muziekdoosje.niet_eigen": ("Dit muziekdoosje is van de boer van dit dorp. Alleen die mag het opwinden, njeg!",
                                         "This music box belongs to this village's farmer. Only they may wind it up!"),
}
MUZIEKTENT = os.path.join("src", "main", "resources", "data", "guhs", "erfenis", "vormen", "muziektent")


def sounds(h):
    def patch(d):
        for event, files in SOUNDS.items():
            # streamed (30 seconds); heard up to about 20 blocks (the dancers are within 15; ErfenisMuziek.VOLUME = 1.25)
            d[event] = {"sounds": [{"name": f"guhs:muziekdoosje/{f}", "stream": True, "attenuation_distance": 20} for f in files],
                        "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def texts(h):
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def selfcheck(h):
    problems = []
    for event, files in SOUNDS.items():
        if f"subtitles.guhs.{event}" not in TEKSTEN:
            problems.append(f"missing subtitle of {event}")
        for f in files:
            if not os.path.exists(os.path.join(h.A, "sounds", "muziekdoosje", f + ".ogg")):
                problems.append(f"missing sound muziekdoosje/{f}.ogg (run tools/features/erfenis_muziek_geluid.py)")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    for n in range(1, 11):
        p = os.path.join(MUZIEKTENT, f"L{n}.json")
        if not os.path.exists(p):
            problems.append(f"missing Muziektent shape L{n}")
            continue
        with open(p, encoding="utf-8") as f:
            pal = json.load(f)["pal"]
        dozen = [b for b in pal if b.startswith("guhs:muziekdoosje")]
        if not dozen:
            problems.append(f"Muziektent L{n} has no Muziekdoosje")
        elif any("speelt=0" not in b for b in dozen):
            problems.append(f"Muziektent L{n}: the Muziekdoosje must be quiet (speelt=0): {dozen}")
    if problems:
        raise SystemExit("erfenis_muziek self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    sounds(h)
    texts(h)
    selfcheck(h)


def _direct():
    """Only this module's outputs (no full make_resources run): sounds.json, its lang keys in nl_nl/en_us, its English overlay."""
    sys.path.insert(0, "tools")
    import make_v2 as h   # (its helpers: patch_json, lang; importing it runs nothing)
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c512_erfenis_muziek.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"erfenis_muziek: {len(SOUNDS)} sound event, {len(TEKSTEN)} texts, Muziektent L1..L10 checked")


if __name__ == "__main__":
    _direct()
