"""Het Vadsrijk 1.10, fix round 2 (I-AUDIO): town music + town ambience (Java: feature/vadsrijk/stadsmuziek).

Writes the sound events into sounds.json and the config texts:
  vr.stad.muziek.<stad>_<1|2>   the town tunes (streamed OGGs from vadsrijk_stadsmuziek_geluid.py, assets/guhs/sounds/vadsrijk/stad)
  vr.stad.bed.<naam>            the ambience beds (seamless 20 s loops; golfjes, briesje, haardvuur)
  vr.stad.sfeer.<stad>          now-and-then town sounds by day, mixed from existing Guhs + vanilla sound events
  vr.stad.sfeer.nacht           the same at night (all towns)
The OGGs are committed (make them with: python tools/features/vadsrijk_stadsmuziek_geluid.py); build() checks they exist.
Run standalone from the project root:  PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_stadsmuziek.py
"""
import json
import os

A = os.path.join("src", "main", "resources")
STEDEN = ["zwamzoet", "knuffelkamp", "haven", "kandijkom"]
BEDDEN = ["golfjes", "briesje", "haardvuur"]


def ev(naam, weight=1, volume=1.0, pitch=1.0):
    return {"name": naam, "type": "event", "weight": weight, "volume": volume, "pitch": pitch}


# day sounds per town: sweet and cute (birds, guh giggles, the market chatter, wind chimes, little bells, fire baskets)
SFEER = {
    "zwamzoet": [ev("guhs:vogels.pluisvinkje", 3, 0.7), ev("guhs:vogels.kaasmeesje", 2, 0.7), ev("guhs:klaterdal.windgong", 2, 0.6),
                 ev("guhs:knuffeldal.kruimel_giechel", 2, 0.45), ev("guhs:einde2_sponsguh_wezens.guh_babbel", 3, 0.35, 1.25),
                 ev("guhs:wereldleven.ijscobel", 1, 0.35)],
    "knuffelkamp": [ev("guhs:vogels.pluisvinkje", 3, 0.75), ev("guhs:vogels.kaasmeesje", 3, 0.75), ev("guhs:creche.babygiechel", 1, 0.4),
                    ev("guhs:knuffeldal.kruimel_giechel", 1, 0.45), ev("guhs:klaterdal.windgong", 1, 0.5),
                    ev("minecraft:entity.baby_chicken.ambient", 1, 0.3)],
    "haven": [ev("guhs:vogels.zeemeeuwtje", 4, 0.6), ev("minecraft:entity.boat.paddle_water", 2, 0.35, 0.8), ev("minecraft:block.bell.use", 1, 0.18, 1.5),
              ev("guhs:einde2_sponsguh_wezens.guh_babbel", 2, 0.3, 1.2), ev("guhs:knuffeldal.kruimel_giechel", 1, 0.4),
              ev("guhs:klaterdal.windgong", 1, 0.45)],
    "kandijkom": [ev("guhs:baltoslee.bellen", 2, 0.35), ev("guhs:balto.belletjes", 1, 0.3), ev("guhs:klaterdal.windgong", 2, 0.5, 1.15),
                  ev("minecraft:block.campfire.crackle", 3, 0.7), ev("guhs:knuffeldal.kruimel_giechel", 2, 0.4),
                  ev("guhs:einde2_sponsguh_wezens.guh_babbel", 2, 0.3, 1.3)],
}
NACHT = [ev("guhs:vogels.guh_uiltje", 2, 0.5), ev("guhs:klaterdal.windgong", 2, 0.4, 0.9), ev("guhs:creche.babygiechel", 1, 0.25, 0.85)]

TEKSTEN = {
    "guhs.configuration.stadsmuziek": "Vadsrijk: stadsmuziek",
    "guhs.configuration.stadsmuziek.tooltip": "Lieve deuntjes in Zwamzoet, Knuffelkamp, De Haven en Kandijkom (schuif Muziek). UIT = daar weer de gewone muziek.",
}


def sounds(d):
    for stad in STEDEN:
        for n in (1, 2):
            d[f"vr.stad.muziek.{stad}_{n}"] = {"sounds": [{"name": f"guhs:vadsrijk/stad/{stad}_{n}", "stream": True}]}
        d[f"vr.stad.sfeer.{stad}"] = {"sounds": SFEER[stad]}
    for b in BEDDEN:
        d[f"vr.stad.bed.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/stad/bed_{b}"}]}
    d["vr.stad.sfeer.nacht"] = {"sounds": NACHT}


def bestanden():
    return [f"{s}_{n}" for s in STEDEN for n in (1, 2)] + [f"bed_{b}" for b in BEDDEN]


def controleer():
    mis = [f for f in bestanden() if not os.path.exists(os.path.join(A, "assets", "guhs", "sounds", "vadsrijk", "stad", f + ".ogg"))]
    if mis:
        raise SystemExit(f"vadsrijk_stadsmuziek: sounds missing: {mis} (run: python tools/features/vadsrijk_stadsmuziek_geluid.py)")


def build(h):
    controleer()
    h.patch_json(os.path.join(A, "assets", "guhs", "sounds.json"), sounds)
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    controleer()
    p = os.path.join(A, "assets", "guhs", "sounds.json")
    with open(p, encoding="utf-8") as f:
        d = json.load(f)
    sounds(d)
    _schrijf(p, d)
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(A, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_stadsmuziek: {len(bestanden())} files, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
