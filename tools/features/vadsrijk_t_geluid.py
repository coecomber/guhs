"""Het Vadsrijk 1.10, T-GELUID: area music, boss themes, stingers and the ambience pass (Java: feature/vadsrijk/geluid).

Writes the sound events into sounds.json and the config texts (en_us = real English):
  vr.geluid.gebied.<thema>    soft area themes (streamed)          assets/guhs/sounds/vadsrijk/muziek/gebied/<thema>.ogg
  vr.geluid.baas.<baas>       boss themes, seamless loops (streamed) assets/guhs/sounds/vadsrijk/muziek/baas/<baas>.ogg
  vr.geluid.stinger.<soort>   short "ta-daa"s                       assets/guhs/sounds/vadsrijk/muziek/stinger/<soort>.ogg
  vr.geluid.bed.<bed>         22 s ambience loops                   assets/guhs/sounds/vadsrijk/sfeer/bed_<bed>.ogg
  vr.geluid.sfeer.<bed>       now-and-then sounds per bed: our one-shots (sfeer/<soort>_<n>.ogg) mixed with existing events
The OGGs are committed; make them with  python tools/vadsrijk/geluid/maak_muziek.py  and  python tools/vadsrijk/geluid/maak_sfeer.py
(they need the GeneralUser GS SoundFont, see maak_muziek.py). build() checks that every file exists.
Run standalone from the project root:  PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_geluid.py
"""
import json
import os

A = os.path.join("src", "main", "resources")
SND = os.path.join(A, "assets", "guhs", "sounds", "vadsrijk")

GEBIED = ["klodderweide", "sporenwoud", "druipgrotten", "zwamtempel", "knuffelmeer", "galmhellingen", "splinterkloof", "spiegelburcht",
          "wortelvelden", "wortelduister", "hart", "schorsstad", "stamspiraal", "takkenbanen", "kruinzee", "zeppelinwerf",
          "lampionkust_dag", "lampionkust_avond", "hoogvads", "hoogvads_markt"]
BAZEN = ["klodderreus", "koning_klodder", "opperzwam", "mika_spion", "boormeester", "galmwolf", "spiegelzucht", "baron_bleu", "nestwachter",
         "wortelwurger", "piepvel", "kruinroofvogel", "vahoegnir", "brokkel", "roquefort", "taleggio", "zeeslurf", "vliegervanger", "algemeen"]
STINGERS = ["ontdekking", "quest_klaar", "level_op", "baas_verslagen", "geheim"]
BEDDEN = ["weide", "bos", "sporen", "grot", "tempel", "meer", "berg", "kloof", "wortel", "kruin", "honing", "kust", "krekels"]
LOS = {"vogel": 4, "druppel": 4, "golf": 2, "bij": 2, "kraak": 2, "glinster": 3, "krekel": 2}


def f(soort, w=1, vol=1.0, pitch=1.0):
    """All variants of one of our one-shots."""
    return [{"name": f"guhs:vadsrijk/sfeer/{soort}_{i}", "weight": w, "volume": vol, "pitch": pitch} for i in range(1, LOS[soort] + 1)]


def ev(naam, w=1, vol=1.0, pitch=1.0):
    return {"name": naam, "type": "event", "weight": w, "volume": vol, "pitch": pitch}


WINDGONG = "guhs:klaterdal.windgong"
# the now-and-then sounds per bed (no wind anywhere)
SFEER = {
    "weide": f("vogel", 3, 0.6) + [ev(WINDGONG, 1, 0.45)],
    "bos": f("vogel", 3, 0.6) + f("kraak", 1, 0.35),
    "sporen": f("glinster", 2, 0.5) + f("vogel", 1, 0.5),
    "grot": f("druppel", 3, 0.6),
    "tempel": f("druppel", 2, 0.5) + f("glinster", 1, 0.4),
    "meer": f("vogel", 2, 0.55) + f("druppel", 1, 0.4) + [ev("minecraft:entity.frog.ambient", 2, 0.35, 1.3)],
    "berg": [ev(WINDGONG, 6, 0.5)] + f("glinster", 1, 0.35) + f("vogel", 1, 0.45),
    "kloof": f("druppel", 2, 0.55) + f("vogel", 1, 0.45),
    "wortel": f("druppel", 3, 0.6) + f("glinster", 1, 0.35),
    "kruin": f("vogel", 3, 0.6) + f("kraak", 1, 0.3),
    "honing": f("bij", 4, 0.45) + f("vogel", 1, 0.5) + [ev("minecraft:block.beehive.work", 2, 0.3, 1.2)],
    "kust": [ev("guhs:vogels.zeemeeuwtje", 8, 0.55)] + f("golf", 3, 0.5) + [ev(WINDGONG, 2, 0.4)],
    "krekels": [ev("guhs:vogels.guh_uiltje", 6, 0.5)] + f("krekel", 3, 0.5) + [ev(WINDGONG, 2, 0.35, 0.9)],
}

TEKSTEN = {  # key: (en, nl)
    "guhs.configuration.gebiedsmuziek": ("Vadsrijk: area music", "Vadsrijk: gebiedsmuziek"),
    "guhs.configuration.gebiedsmuziek.tooltip": (
        "Soft music outside the towns, with long silences in between (percent, 0 = off). Also follows Minecraft's Music slider.",
        "Zachte muziek buiten de steden, met lange stiltes ertussen (procent, 0 = uit). Volgt ook de schuif Muziek van Minecraft."),
    "guhs.configuration.bazenmuziek": ("Vadsrijk: boss music", "Vadsrijk: bazenmuziek"),
    "guhs.configuration.bazenmuziek.tooltip": ("Every boss has its own theme during the fight (percent, 0 = off).",
                                               "Elke baas heeft een eigen thema tijdens het gevecht (procent, 0 = uit)."),
    "guhs.configuration.stingers": ("Vadsrijk: stingers", "Vadsrijk: stingers (ta-daa's)"),
    "guhs.configuration.stingers.tooltip": (
        "Short musical ta-daas: a new area, a quest done, a level up, a boss beaten, a secret found (percent, 0 = off).",
        "Korte muzikale ta-daa's: nieuw gebied, quest klaar, level omhoog, baas verslagen, geheim gevonden (procent, 0 = uit)."),
    "guhs.configuration.omgeving": ("Vadsrijk: ambience", "Vadsrijk: omgevingsgeluid"),
    "guhs.configuration.omgeving.tooltip": (
        "Birds, water, chimes, crickets at night, drips in caves, bees, waves (percent, 0 = off). Also follows the Ambient slider.",
        "Vogels, water, belletjes, krekels 's nachts, druppels in grotten, bijen, golven (procent, 0 = uit). Volgt ook de schuif Omgeving."),
    "guhs.configuration.pauze_min": ("Vadsrijk: silence between pieces, at least (s)", "Vadsrijk: stilte tussen stukken, minstens (s)"),
    "guhs.configuration.pauze_min.tooltip": ("Seconds of only ambience after an area piece.", "Seconden met alleen omgevingsgeluid na een gebiedsstuk."),
    "guhs.configuration.pauze_max": ("Vadsrijk: silence between pieces, at most (s)", "Vadsrijk: stilte tussen stukken, hoogstens (s)"),
    "guhs.configuration.pauze_max.tooltip": ("Seconds of only ambience after an area piece.", "Seconden met alleen omgevingsgeluid na een gebiedsstuk."),
}


def sounds(d):
    for t in GEBIED:
        d[f"vr.geluid.gebied.{t}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/gebied/{t}", "stream": True}]}
    for b in BAZEN:
        d[f"vr.geluid.baas.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/baas/{b}", "stream": True}]}
    for s in STINGERS:
        d[f"vr.geluid.stinger.{s}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/stinger/{s}"}]}
    for b in BEDDEN:
        d[f"vr.geluid.bed.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/sfeer/bed_{b}"}]}
        d[f"vr.geluid.sfeer.{b}"] = {"sounds": SFEER[b]}


def bestanden():
    l = [os.path.join("muziek", "gebied", t) for t in GEBIED] + [os.path.join("muziek", "baas", b) for b in BAZEN]
    l += [os.path.join("muziek", "stinger", s) for s in STINGERS] + [os.path.join("sfeer", "bed_" + b) for b in BEDDEN]
    l += [os.path.join("sfeer", f"{s}_{i}") for s, n in LOS.items() for i in range(1, n + 1)]
    return l


def controleer():
    mis = [p for p in bestanden() if not os.path.exists(os.path.join(SND, p + ".ogg"))]
    if mis:
        raise SystemExit(f"vadsrijk_t_geluid: sounds missing: {mis} (run tools/vadsrijk/geluid/maak_muziek.py + maak_sfeer.py)")


def build(h):
    controleer()
    h.patch_json(os.path.join(A, "assets", "guhs", "sounds.json"), sounds)
    for k, (en, nl) in TEKSTEN.items():
        h.lang(k, en, nl)


def _schrijf(pad, obj):
    with open(pad, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


def _direct():
    controleer()
    p = os.path.join(A, "assets", "guhs", "sounds.json")
    with open(p, encoding="utf-8") as fh:
        d = json.load(fh)
    sounds(d)
    _schrijf(p, d)
    for taal, i in (("en_us", 0), ("nl_nl", 1)):
        p = os.path.join(A, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as fh:
            data = json.load(fh)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, data)
    print(f"vadsrijk_t_geluid: {len(bestanden())} files, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
