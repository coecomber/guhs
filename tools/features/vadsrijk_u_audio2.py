"""Het Vadsrijk 1.10, U-AUDIO2 (feedback round 4, V4): boss music with phases + own fanfares, world point sounds, NPC voices.
Java: feature/vadsrijk/geluid (GeluidPlan, GeluidRegistry, StemPlan, WereldBron, client/WereldGeluiden, client/StemSpeler).

Writes into sounds.json (add-only, next to T-GELUID's entries):
  vr.geluid.baas.<new boss>        the 5 new boss themes (streamed)      muziek/baas/<baas>.ogg
  vr.geluid.baas.<baas>_fase       the last-phase loop of every boss     muziek/baas/<baas>_fase.ogg (same length: crossfaded)
  vr.geluid.fanfare.<baas>         every boss's own victory fanfare      muziek/fanfare/<baas>.ogg
  vr.stem.<soort>                  NPC voices: the EXISTING guh / Mika / pieppiepmuisje / blub takes (no new audio)
  vr.wereld.<soort>                world point sounds: existing takes (waterval ruis, hamers, windgong, bijen, kraak, guh babble)
and the two new sliders' config texts (en_us = real English). The OGGs come from tools/vadsrijk/geluid/maak_muziek.py.
Run standalone from the project root:  PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_audio2.py
"""
import json
import os

A = os.path.join("src", "main", "resources")
SND = os.path.join(A, "assets", "guhs", "sounds", "vadsrijk")

NIEUW = ["mika_imker", "grauwe_droom", "donderbuik", "gorgonzola", "zilverstaart"]
BAZEN = ["klodderreus", "koning_klodder", "opperzwam", "mika_spion", "boormeester", "galmwolf", "spiegelzucht", "baron_bleu", "nestwachter",
         "wortelwurger", "piepvel", "kruinroofvogel", "vahoegnir", "brokkel", "roquefort", "taleggio", "zeeslurf", "vliegervanger"] + NIEUW + ["algemeen"]


def s(naam, vol=1.0, pitch=1.0, w=1, afstand=None, stream=False):
    d = {"name": naam, "volume": vol, "pitch": pitch, "weight": w}
    if afstand:
        d["attenuation_distance"] = afstand
    if stream:
        d["stream"] = True
    return d


def ev(naam, vol=1.0, pitch=1.0, w=1, afstand=None):
    d = s(naam, vol, pitch, w, afstand)
    d["type"] = "event"
    return d


def guh(nrs, vol=1.0, pitch=1.0, afstand=12):
    return [s(f"guhs:guh_ambient{n}", vol, pitch, 1, afstand) for n in nrs]


# NPC voices: the bioscoop mapping of the takes (guh/vahoeg = hello, oeh/njeg = a question, lach = glad)
STEMMEN = {
    "groet": guh([3, 4, 7, 10, 11, 16]),
    "vraag": guh([1, 8, 5, 9]),
    "dank": guh([6, 12]) + [ev("guhs:entity.guh.happy", 1.0, 1.0, 1, 12)],
    "mika": [s(f"guhs:mika_ambient{n}", 0.9, 1.0, 1, 12) for n in range(1, 17)],
    "piep": [s(f"guhs:piep_ambient{n}", 0.9, 1.0, 1, 12) for n in range(1, 6)],
    "blub": [s(f"guhs:waterdiertjes/blub{n}", 0.9, 1.0, 1, 12) for n in range(1, 4)],
}

# world point sounds: short range (attenuation 10-12), the client keeps them soft and sparse
WERELD = {
    "fontein": [s("guhs:waterval/ruis", 0.7, 1.45)],   # (a loop; the client puts the distance in the volume)
    "bijen": [s("guhs:vadsrijk/sfeer/bij_1", 0.8, 1.0, 2, 10), s("guhs:vadsrijk/sfeer/bij_2", 0.8, 1.0, 2, 10),
              ev("minecraft:block.beehive.work", 0.6, 1.0, 1, 10)],
    "smidse": [s("guhs:bio_bouw_wolk2/hamer1", 0.7, 1.0, 1, 12), s("guhs:bio_bouw_wolk2/hamer2", 0.7, 1.0, 1, 12),
               s("guhs:beroepen/hamer1", 0.7, 1.0, 1, 12), s("guhs:beroepen/hamer2", 0.7, 1.0, 1, 12)],
    "markt": guh([2, 5, 9, 12], 0.55, 1.15, 12) + guh([3, 7], 0.5, 0.95, 12),
    "windgong": [s(f"guhs:klaterdal/windgong{n}", 0.55, 1.0, 1, 12) for n in range(1, 4)],
    "kampvuur": [ev("minecraft:block.campfire.crackle", 0.8, 1.0, 2, 10), ev("minecraft:block.fire.ambient", 0.35, 1.0, 1, 10)],
    "kraak": [s("guhs:vadsrijk/sfeer/kraak_1", 0.8, 1.0, 1, 12), s("guhs:vadsrijk/sfeer/kraak_2", 0.8, 1.0, 1, 12),
              s("guhs:einde2_vetbootje/kraak1", 0.6, 0.9, 1, 12), s("guhs:einde2_vetbootje/kraak2", 0.6, 0.9, 1, 12)],
}

TEKSTEN = {  # key: (en, nl)
    "guhs.configuration.wereldgeluiden": ("Vadsrijk: world sounds", "Vadsrijk: wereldgeluiden"),
    "guhs.configuration.wereldgeluiden.tooltip": (
        "Soft little sounds of places close by: fountains, beehives, the smithy hammer, market murmur, wind chimes, campfires, "
        "creaking lifts, an echo of your steps in caves (percent, 0 = off). Also follows the Ambient slider.",
        "Zachte geluidjes van plekken dichtbij: fonteintjes, bijenkorven, de smidshamer, marktgeroezemoes, windgongen, kampvuren, "
        "krakende liften, echo van je voetstappen in grotten (procent, 0 = uit). Volgt ook de schuif Omgeving."),
    "guhs.configuration.stemmen": ("Vadsrijk: NPC voices", "Vadsrijk: NPC-stemmetjes"),
    "guhs.configuration.stemmen.tooltip": (
        "The little guh voices of NPCs: a 'guh!' when you talk to them, a question, a thank-you (percent, 0 = off). "
        "Also follows the Friendly Creatures slider.",
        "De guh-stemmetjes van NPC's: een 'guh!' als je ze aanspreekt, een vraagje, een bedankje (procent, 0 = uit). "
        "Volgt ook de schuif Vriendelijke wezens."),
}


def sounds(d):
    for b in NIEUW:
        d[f"vr.geluid.baas.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/baas/{b}", "stream": True}]}
    for b in BAZEN:
        d[f"vr.geluid.baas.{b}_fase"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/baas/{b}_fase", "stream": True}]}
        d[f"vr.geluid.fanfare.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/fanfare/{b}"}]}
    for k, v in STEMMEN.items():
        d[f"vr.stem.{k}"] = {"sounds": v}
    for k, v in WERELD.items():
        d[f"vr.wereld.{k}"] = {"sounds": v}


def bestanden():
    l = [os.path.join("muziek", "baas", b) for b in NIEUW]
    l += [os.path.join("muziek", "baas", b + "_fase") for b in BAZEN] + [os.path.join("muziek", "fanfare", b) for b in BAZEN]
    return l


def controleer():
    mis = [p for p in bestanden() if not os.path.exists(os.path.join(SND, p + ".ogg"))]
    if mis:
        raise SystemExit(f"vadsrijk_u_audio2: sounds missing: {mis} (run tools/vadsrijk/geluid/maak_muziek.py)")
    # the reused takes must exist too
    root = os.path.join(A, "assets", "guhs", "sounds")
    for lijst in list(STEMMEN.values()) + list(WERELD.values()):
        for e in lijst:
            if e.get("type") != "event" and e["name"].startswith("guhs:"):
                p = os.path.join(root, e["name"][5:] + ".ogg")
                if not os.path.exists(p):
                    raise SystemExit(f"vadsrijk_u_audio2: reused sound missing: {p}")


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
    print(f"vadsrijk_u_audio2: {len(bestanden())} music files, {len(STEMMEN)} voices, {len(WERELD)} world sounds, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
