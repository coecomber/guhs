"""Het Vadsrijk 1.10, U-GEROEZEMOES: the soft guh crowd murmur of the busy places (Hoogvads, the Saffraanpoort bazaar, ...).
Java: feature/vadsrijk/geluid (Geroezemoes, client/GeroezemoesSpeler; replaces u-audio2's small "markt" murmur).

Writes into sounds.json (add-only):
  vr.geroezemoes.bed.<rustig|druk|markt>   the murmur loops       vadsrijk/geroezemoes/bed_<naam>.ogg (maak_geroezemoes.py)
  vr.geroezemoes.<njeg|lach|guh|oeh|piep|blub>   clear one-shots: the EXISTING guh / pieppiepmuisje / blub takes (no new audio)
and the slider's config texts (en_us = real English).
Run standalone from the project root:  PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_geroezemoes.py
"""
import json
import os

A = os.path.join("src", "main", "resources")
SND = os.path.join(A, "assets", "guhs", "sounds")
BEDDEN = ["rustig", "druk", "markt"]


def s(naam, vol=1.0, pitch=1.0, afstand=16):
    return {"name": naam, "volume": vol, "pitch": pitch, "weight": 1, "attenuation_distance": afstand}


def guh(nrs, vol=0.9):
    return [s(f"guhs:guh_ambient{n}", vol) for n in nrs]


LOS = {
    "njeg": guh([5, 9]),
    "lach": guh([6, 12]),
    "guh": guh([3, 4, 7, 10, 11, 16]),
    "oeh": guh([1, 8]),
    "piep": [s(f"guhs:piep_ambient{n}", 0.8) for n in (1, 2, 3, 5)],
    "blub": [s(f"guhs:waterdiertjes/blub{n}", 0.8) for n in (1, 2, 3)] + [s("guhs:blubguh/idle/blub", 0.8)],
}

TEKSTEN = {  # key: (en, nl)
    "guhs.configuration.geroezemoes": ("Vadsrijk: crowd murmur", "Vadsrijk: geroezemoes"),
    "guhs.configuration.geroezemoes.tooltip": (
        "A soft guh crowd murmur in busy places (Hoogvads, the Saffraanpoort bazaar, the Kandijkom market, ...), now and then a "
        "'njeg', a giggle, a squeak or a blub. Quieter at night (percent, 0 = off). Also follows the Ambient slider.",
        "Zacht guh-geroezemoes op drukke plekken (Hoogvads, de bazaar van Saffraanpoort, de markt van Kandijkom, ...), met af en "
        "toe een 'njeg', een giechel, een piepje of een blub. 's Nachts stiller (procent, 0 = uit). Volgt ook de schuif Omgeving."),
}


def sounds(d):
    for b in BEDDEN:
        d[f"vr.geroezemoes.bed.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/geroezemoes/bed_{b}"}]}   # (short: static, loops gapless)
    for k, v in LOS.items():
        d[f"vr.geroezemoes.{k}"] = {"sounds": v}


def controleer():
    for b in BEDDEN:
        p = os.path.join(SND, "vadsrijk", "geroezemoes", f"bed_{b}.ogg")
        if not os.path.exists(p):
            raise SystemExit(f"vadsrijk_u_geroezemoes: {p} missing (run tools/vadsrijk/geluid/maak_geroezemoes.py)")
    for lijst in LOS.values():
        for e in lijst:
            p = os.path.join(SND, e["name"][5:] + ".ogg")
            if not os.path.exists(p):
                raise SystemExit(f"vadsrijk_u_geroezemoes: reused sound missing: {p}")


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
    print(f"vadsrijk_u_geroezemoes: {len(BEDDEN)} beds, {len(LOS)} one-shots, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
