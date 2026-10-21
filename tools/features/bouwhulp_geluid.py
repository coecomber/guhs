"""
Op Safari! (1.6.0), slice F12 - the Bouwhulp's sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/bouwhulp/:

  spook        a ghost appears: a soft whoosh with a sparkly arpeggio
  plonk1-3     a block goes in: a cute wooden "tock" with a little pitch drop
  kraan        the Bouwkeet's crane: a ratchet and a creak
  pof          Pof!: a big soft poof with a sparkle tail
  feest        a project is done: a short brassy fanfare (C E G C + chord)
  zwiep1-2     the vormstaf / a kwast stroke: a swish
  papier       the Ontwerpbureau: paper rustling
  knaag1-3     a helper gnawing (bever, specht): the same bites as the Beverbeek's beaver
  graaf1-3     a helper digging (stokstaartje, das, wombat): dirt scraped away
  fluit1-3     a whistling bouwguh: a tiny happy tune

It also writes the sounds.json entries and the subtitles (NL here, EN in tools/lang/en/c184_bouwhulp_geluid.json) straight into
the resources, so no make_resources run is needed. Needs numpy, scipy and soundfile:
    python tools/features/bouwhulp_geluid.py        (from the project root)
The sound events are registered by feature/bouwhulp/BouwGeluid.
"""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import safari_geluid as g   # noqa: E402  (the shared synth helpers)

from safari_geluid import SR, band, env, high, low, mix, ruis, t_, toon   # noqa: E402

A = os.path.join("src", "main", "resources", "assets", "guhs")
OUT = os.path.join(A, "sounds", "bouwhulp")
rng = np.random.default_rng(20261022)

# event -> (files, subtitle key, Dutch subtitle)
EVENTS = {
    "bouwhulp.spook": (["spook"], "Spook verschijnt"),
    "bouwhulp.plonk": (["plonk1", "plonk2", "plonk3"], "Blokje plonkt erin"),
    "bouwhulp.kraan": (["kraan"], "Bouwkraan ratelt"),
    "bouwhulp.pof": (["pof"], "Pof!"),
    "bouwhulp.feest": (["feest"], "Bouwfeestje!"),
    "bouwhulp.zwiep": (["zwiep1", "zwiep2"], "Zwiep"),
    "bouwhulp.papier": (["papier"], "Papier ritselt"),
    "bouwhulp.knaag": (["knaag1", "knaag2", "knaag3"], "Knaag knaag"),
    "bouwhulp.graaf": (["graaf1", "graaf2", "graaf3"], "Graaf graaf"),
    "bouwhulp.fluit": (["fluit1", "fluit2", "fluit3"], "Bouwguh fluit"),
}


def spook():
    whoosh = band(ruis(0.6), 400, 2500) * env(int(0.6 * SR), 0.25, 0.3) * 0.4
    noten = [1047, 1319, 1568, 2093]
    return mix(0.8, (0, whoosh, 1.0), *[(0.12 + 0.07 * i, toon(f, 0.3, harm=(1, 0.3), a=0.002, r=0.27), 0.35) for i, f in enumerate(noten)])


def plonk(v):
    f = (520, 600, 680)[v - 1]
    tok = toon(f, 0.16, glij=-0.25, harm=(1, 0.4, 0.15), a=0.001, r=0.15)
    klik = high(ruis(0.012), 2500) * np.exp(-t_(0.012) * 300)
    return mix(0.2, (0, tok, 1.0), (0, klik, 0.4))


def kraan():
    klikken = []
    for i in range(9):
        k = band(ruis(0.02), 1800, 5000) * np.exp(-t_(0.02) * 200)
        klikken.append((i * 0.07, k, 0.6))
    t = t_(0.9)
    f = 260 + 90 * np.sin(2 * np.pi * 0.8 * t)
    piep = np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.5 + 0.5 * np.sin(2 * np.pi * 23 * t)) * env(len(t), 0.1, 0.3)
    return mix(1.3, *klikken, (0.4, band(piep + 0.3 * ruis(0.9), 200, 1500), 0.35))


def pof():
    puf = low(ruis(1.0), 700) * np.exp(-t_(1.0) * 4.5)
    dreun = toon(70, 0.4, glij=-0.4, harm=(1, 0.5), a=0.002, r=0.35)
    glit = mix(1.2, *[(0.3 + rng.uniform(0, 0.6), toon(rng.uniform(1800, 3200), 0.15, harm=(1,), a=0.002, r=0.14), 0.12) for _ in range(10)])
    return mix(1.4, (0, puf, 1.0), (0, dreun, 0.8), (0, glit, 1.0))


def feest():
    def koper(f, d):
        return toon(f, d, vib=0.006, vibf=5.5, harm=(1, 0.8, 0.6, 0.45, 0.3, 0.2), a=0.02, r=0.08)
    noten = [(0.0, 523, 0.16), (0.17, 659, 0.16), (0.34, 784, 0.16), (0.51, 1047, 0.3)]
    slot = [(0.85, f, 0.9) for f in (523, 659, 784, 1047)]
    trom = low(ruis(0.12), 300) * np.exp(-t_(0.12) * 30)
    return mix(1.9, *[(s, koper(f, d), 0.5) for s, f, d in noten], *[(s, koper(f, d), 0.3) for s, f, d in slot],
               (0.0, trom, 0.6), (0.51, trom, 0.6), (0.85, trom, 0.9))


def zwiep(v):
    d = 0.28 + 0.05 * v
    t = t_(d)
    x = ruis(d)
    out = np.zeros_like(x)
    stukken = 12
    for i in range(stukken):                       # a band that slides up: filter piece by piece
        a, b = i * len(x) // stukken, (i + 1) * len(x) // stukken
        lo = 400 + (3500 - 400) * (i / stukken) * (1 if v == 1 else 0.8)
        out[a:b] = band(x, lo, lo * 1.8)[a:b]
    return out * env(len(t), 0.06, 0.12)


def papier():
    delen = [(s, high(ruis(d), 2500) * env(int(d * SR), 0.01, d * 0.6), gg) for s, d, gg in ((0, 0.12, 0.8), (0.16, 0.09, 0.6), (0.3, 0.18, 0.9))]
    return mix(0.55, *delen)


def graaf(v):
    n = 2 + v % 2
    delen = []
    for i in range(n):
        d = 0.18
        schraap = band(ruis(d), 250, 1800) * env(int(d * SR), 0.02, 0.1)
        kruimel = mix(d, *[(rng.uniform(0, d - 0.02), high(ruis(0.01), 3000), 0.3) for _ in range(6)])
        delen.append((i * 0.24, schraap + kruimel, 0.9))
    return mix(n * 0.24 + 0.1, *delen)


def fluit(v):
    wijsjes = [(1319, 1568, 1760), (1568, 1319, 1568, 2093), (1175, 1568, 1319)]
    delen = [(0.13 * i, toon(f, 0.12, vib=0.02, vibf=7.0, harm=(1, 0.08), a=0.01, r=0.05), 0.6) for i, f in enumerate(wijsjes[v - 1])]
    return mix(0.13 * len(wijsjes[v - 1]) + 0.1, *delen)


def bestanden():
    yield "spook", spook(), 0.6
    for v in (1, 2, 3):
        yield f"plonk{v}", plonk(v), 0.6
    yield "kraan", kraan(), 0.5
    yield "pof", pof(), 0.8
    yield "feest", feest(), 0.7
    for v in (1, 2):
        yield f"zwiep{v}", zwiep(v), 0.5
    yield "papier", papier(), 0.5
    for v in (1, 2, 3):
        yield f"knaag{v}", g.knaag(v), 0.55
        yield f"graaf{v}", graaf(v), 0.55
        yield f"fluit{v}", fluit(v), 0.45


def _json(path, patch):
    with open(path, encoding="utf-8") as f:
        d = json.load(f)
    patch(d)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, indent=2, ensure_ascii=False)
        f.write("\n")


def resources():
    """sounds.json entries and subtitles (also the F12 safari ones: their events come from safari_blokken.geluiden, which
    reads safari_geluid's lists, but writing them here keeps a full make_resources run unnecessary)."""
    entries = {e: {"sounds": [{"name": f"guhs:bouwhulp/{f}"} for f in fs], "subtitle": f"subtitles.guhs.{e}"} for e, (fs, _) in EVENTS.items()}
    for grp in g.F12_GROEPEN:
        entries[f"safari.dier.{grp}"] = {"sounds": [{"name": f"guhs:safari/dier/{grp}{v}"} for v in (1, 2, 3)],
                                         "subtitle": f"subtitles.guhs.safari.dier.{grp}"}
    for z in g.F12_ZONES:
        entries[f"safari.sfeer.{z}"] = {"sounds": [{"name": f"guhs:safari/sfeer/{z}", "stream": True}], "subtitle": "subtitles.guhs.safari.sfeer"}
    entries["beverbeek.sfeer"] = {"sounds": [{"name": "guhs:safari/beverbeek_sfeer", "stream": True}], "subtitle": "subtitles.guhs.beverbeek.sfeer"}
    entries["beverbeek.knaag"] = {"sounds": [{"name": f"guhs:safari/beverbeek_knaag{v}"} for v in (1, 2, 3)], "subtitle": "subtitles.guhs.beverbeek.knaag"}
    entries["knabbelvelden.sfeer"] = {"sounds": [{"name": "guhs:safari/knabbelvelden_sfeer", "stream": True}],
                                      "subtitle": "subtitles.guhs.knabbelvelden.sfeer"}
    _json(os.path.join(A, "sounds.json"), lambda d: d.update(entries))
    nl = {f"subtitles.guhs.{e}": t for e, (_, t) in EVENTS.items()}
    nl.update(NL_EXTRA)
    with open(os.path.join("tools", "lang", "en", "c184_bouwhulp_geluid.json"), encoding="utf-8") as f:
        en = json.load(f)
    _json(os.path.join(A, "lang", "nl_nl.json"), lambda d: d.update(nl))
    _json(os.path.join(A, "lang", "en_us.json"), lambda d: d.update({k: en.get(k, v) for k, v in nl.items()}))


NL_EXTRA = {
    "subtitles.guhs.safari.dier.kriebel": "Kriebeldiertje ritselt",
    "subtitles.guhs.safari.dier.hommel": "Hommel zoemt",
    "subtitles.guhs.safari.dier.vis": "Visje blubt",
    "subtitles.guhs.safari.dier.kikker": "Kikker kwaakt",
    "subtitles.guhs.safari.dier.vleermuis": "Vleermuisje piept",
    "subtitles.guhs.beverbeek.sfeer": "Kabbelend beekje",
    "subtitles.guhs.beverbeek.knaag": "Bever knaagt",
    "subtitles.guhs.knabbelvelden.sfeer": "Zachte droommuziek",
}


def main():
    import soundfile as sf
    os.makedirs(OUT, exist_ok=True)
    for naam, x, piek in bestanden():
        x = x * env(len(x), 0.003, 0.04)
        data = g.norm(x, piek).astype(np.float32)
        with sf.SoundFile(os.path.join(OUT, naam + ".ogg"), "w", SR, 1, format="OGG", subtype="VORBIS") as f:
            for i in range(0, len(data), 8192):
                f.write(data[i:i + 8192])
        print("wrote", naam, f"{len(x) / SR:.2f}s")
    resources()


if __name__ == "__main__":
    main()
