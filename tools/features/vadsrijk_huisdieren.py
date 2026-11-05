"""
1.10.0 "Het Vadsrijk", G-PETS: the pet system's assets (Java: feature/vadsrijk/huisdieren: Huisdieren, Huisdier, HuisdierEntity,
client/HuisdierScherm, client/HuisdierClient).

Every pet is a tiny chibi copy of a boss: the boss's own GeckoLib model + texture (reused as they are), with the pet's OWN animation
file that also does the chibi tweak (bigger head / crown, short legs: a constant bone scale in every animation):
  * Mini Grauwe Klodderreus: the Koning Klodder's minion klodder model (vr_baas_kk_klodder) with a grey version of its pink texture
    (generated here: textures/entity/vr_huisdier_klodderreus.png; the blush stays pink);
  * Mini Koning Klodder: vr_baas_koning_klodder + its texture, the crown a bit too big;
  * Mini Galmwolf: vr_z2_baas_galmwolf + its texture (+ glowmask), a crystal pup: big head, short legs, small crystals.
Animations (all pets): idle, hop (walking), sprong (in the air). HuisdierEntity picks them.
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_huisdieren.py
"""
import json
import os

from PIL import Image


def kf(pairs):
    return {f"{t:g}": [float(v) for v in val] for t, val in pairs}


def anim(lengte, bones, loop=True):
    return {"animation_length": lengte, "loop": loop, "bones": bones}


def _chibi(basis, anims):
    """Adds the constant chibi bone changes (scale/position) to every animation (unanimated bones reset between animations)."""
    for a in anims.values():
        for bone, kanalen in basis.items():
            b = a["bones"].setdefault(bone, {})
            for kanaal, waarde in kanalen.items():
                if kanaal not in b:
                    b[kanaal] = kf([(0, waarde)])
                elif kanaal == "position":   # add the constant offset to every keyframe
                    for t, v in b[kanaal].items():
                        b[kanaal][t] = [v[0] + waarde[0], v[1] + waarde[1], v[2] + waarde[2]]
    return anims


# =====================================================================================================================
# the klodders (Mini Grauwe Klodderreus: kk_klodder bones body/ear_left/ear_right; Mini Koning Klodder: + crown/zoom)
# =====================================================================================================================
def anims_klodder(kroon=False):
    oren = lambda a, t: {"ear_left": {"rotation": kf([(0, (0, 0, 0)), (t / 2, (0, 0, a)), (t, (0, 0, 0))])},
                         "ear_right": {"rotation": kf([(0, (0, 0, 0)), (t / 2, (0, 0, -a)), (t, (0, 0, 0))])}}
    idle = {"body": {"rotation": kf([(0, (0, 0, 0)), (0.8, (0, 0, 4)), (1.6, (0, 0, 0)), (2.4, (0, 0, -4)), (3.2, (0, 0, 0))])}}
    idle["body"]["scale"] = kf([(0, (1, 1, 1)), (0.8, (1.06, 0.94, 1.06)), (1.6, (1, 1, 1)), (2.4, (1.06, 0.94, 1.06)), (3.2, (1, 1, 1))])
    idle.update(oren(14, 3.2))
    # hop: squish down, spring up, stretch, land
    hop = {"body": {"position": kf([(0, (0, 0, 0)), (0.1, (0, 0, 0)), (0.25, (0, 4, 0)), (0.4, (0, 0, 0)), (0.5, (0, 0, 0))]),
                    "scale": kf([(0, (1.15, 0.82, 1.15)), (0.12, (0.9, 1.15, 0.9)), (0.25, (0.95, 1.08, 0.95)), (0.4, (1.18, 0.8, 1.18)),
                                 (0.5, (1.15, 0.82, 1.15))])}}
    hop.update(oren(-25, 0.5))
    sprong = {"body": {"scale": kf([(0, (0.9, 1.15, 0.9))])},
              "ear_left": {"rotation": kf([(0, (0, 0, -30))])}, "ear_right": {"rotation": kf([(0, (0, 0, 30))])}}
    anims = {"idle": anim(3.2, idle), "hop": anim(0.5, hop), "sprong": anim(0.4, sprong)}
    if kroon:
        # the crown wobbles a little (and is a bit too big: the chibi tweak)
        for naam, a in anims.items():
            a["bones"]["crown"] = {"rotation": kf([(0, (0, 0, -5)), (a["animation_length"] / 2, (0, 0, 5)), (a["animation_length"], (0, 0, -5))])}
        return _chibi({"crown": {"scale": (1.3, 1.3, 1.3)}}, anims)
    return anims


# =====================================================================================================================
# the Galmwolf pup (bones body/kristallen/head/kaak/oor_l/oor_r/pootvl/pootvr/pootal/pootar/staart; legs pivot at y 12)
# =====================================================================================================================
POOT = 0.62           # the legs' y scale (short pup legs)
ZAK = 12 * (1 - POOT)  # how far the body sinks so the paws stay on the ground


def anims_galmwolf():
    poten = ("pootvl", "pootvr", "pootal", "pootar")
    idle = {"staart": {"rotation": kf([(0, (0, -25, 0)), (0.2, (0, 25, 0)), (0.4, (0, -25, 0))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 0, 12)), (2.0, (0, 0, 0)), (3.0, (-6, 8, 0)), (4.0, (0, 0, 0))])},
            "oor_l": {"rotation": kf([(0, (0, 0, 0)), (2.0, (0, 0, -12)), (4.0, (0, 0, 0))])},
            "oor_r": {"rotation": kf([(0, (0, 0, 0)), (2.0, (0, 0, 12)), (4.0, (0, 0, 0))])},
            "kaak": {"rotation": kf([(0, (0, 0, 0)), (3.4, (0, 0, 0)), (3.6, (12, 0, 0)), (3.8, (0, 0, 0)), (4.0, (0, 0, 0))])},
            "body": {"position": kf([(0, (0, 0, 0)), (2.0, (0, 0.3, 0)), (4.0, (0, 0, 0))])}}
    idle = {"staart": idle["staart"], "head": idle["head"], "oor_l": idle["oor_l"], "oor_r": idle["oor_r"], "kaak": idle["kaak"],
            "body": idle["body"]}
    # (the tail loop is 0.4 s inside a 4 s animation: repeat it)
    idle["staart"] = {"rotation": kf([(i * 0.2, (0, -25 if i % 2 == 0 else 25, 0)) for i in range(21)])}
    hop = {}
    for b, s in (("pootvl", 1), ("pootar", 1), ("pootvr", -1), ("pootal", -1)):
        hop[b] = {"rotation": kf([(0, (40 * s, 0, 0)), (0.2, (-40 * s, 0, 0)), (0.4, (40 * s, 0, 0))])}
    hop["body"] = {"position": kf([(0, (0, 0, 0)), (0.1, (0, 1.2, 0)), (0.2, (0, 0, 0)), (0.3, (0, 1.2, 0)), (0.4, (0, 0, 0))])}
    hop["head"] = {"rotation": kf([(0, (4, 0, 0)), (0.2, (-4, 0, 0)), (0.4, (4, 0, 0))])}
    hop["oor_l"] = {"rotation": kf([(0, (-20, 0, 0)), (0.2, (0, 0, 0)), (0.4, (-20, 0, 0))])}
    hop["oor_r"] = {"rotation": kf([(0, (-20, 0, 0)), (0.2, (0, 0, 0)), (0.4, (-20, 0, 0))])}
    hop["staart"] = {"rotation": kf([(0, (0, -30, 0)), (0.1, (0, 30, 0)), (0.2, (0, -30, 0)), (0.3, (0, 30, 0)), (0.4, (0, -30, 0))])}
    sprong = {"pootvl": {"rotation": kf([(0, (-50, 0, 0))])}, "pootvr": {"rotation": kf([(0, (-50, 0, 0))])},
              "pootal": {"rotation": kf([(0, (50, 0, 0))])}, "pootar": {"rotation": kf([(0, (50, 0, 0))])},
              "oor_l": {"rotation": kf([(0, (-30, 0, 0))])}, "oor_r": {"rotation": kf([(0, (-30, 0, 0))])},
              "staart": {"rotation": kf([(0, (30, 0, 0))])}}
    anims = {"idle": anim(4.0, idle), "hop": anim(0.4, hop), "sprong": anim(0.4, sprong)}
    basis = {"head": {"scale": (1.55, 1.55, 1.55)}, "kristallen": {"scale": (0.55, 0.55, 0.55)}, "body": {"position": (0, -ZAK, 0)}}
    for p in poten:
        basis[p] = {"scale": (1, POOT, 1)}
    return _chibi(basis, anims)


# =====================================================================================================================
# the grey klodder texture: the pink minion klodder in grey (eyes stay, the blush stays a soft pink)
# =====================================================================================================================
def grauwe_klodder(bron):
    img = Image.open(bron).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if r - g <= 40 or r <= 150:
                continue   # (the eyes, the mouth)
            lum = 0.35 * r + 0.45 * g + 0.2 * b
            if x < 48 and y < 40 and b < 0.74 * r:   # the blush on the face: a soft pink
                px[x, y] = (min(255, int(lum + 70)), int(lum * 0.9), int(lum * 0.98), a)
            else:
                v = int(lum * 0.92 + 18)
                px[x, y] = (v, min(255, v + 2), min(255, v + 10), a)
    return img


# =====================================================================================================================
# texts
# =====================================================================================================================
def teksten():
    t = {
        "entity.guhs.vr_huisdier": "Huisdier",
        "key.guhs.vr_huisdieren": "Huisdieren (scherm)",
        "vadsrijk.huisdieren.titel": "Huisdieren",
        "vadsrijk.huisdieren.verzameld": "Verzameld %s/%s",
        "vadsrijk.huisdieren.nog_niet": "Nog niet gevonden",
        "vadsrijk.huisdieren.hoe": "Zo krijg je hem:",
        "vadsrijk.huisdieren.meenemen": "Meenemen",
        "vadsrijk.huisdieren.thuislaten": "Thuislaten",
        "vadsrijk.huisdieren.loopt_mee": "Loopt met je mee!",
        "vadsrijk.huisdieren.geen_actief": "Er loopt nu geen huisdier met je mee.",
        "vadsrijk.huisdieren.uitleg": "Huisdieren zijn er voor de gezelligheid: ze lopen met je mee door het Vadsrijk. Versla een baas voor het eerst en je krijgt er een!",
        "vadsrijk.huisdieren.knop": "Huisdieren",
        "vadsrijk.huisdieren.alleen_rijk": "Huisdieren wonen in het Vadsrijk.",
        "vadsrijk.huisdieren.nieuw.titel": "Nieuw huisdier!",
        "vadsrijk.huisdieren.nieuw.chat": "Nieuw huisdier: %s! Druk op %s om je huisdieren te bekijken.",
        "vadsrijk.huisdieren.mee": "%s loopt nu met je mee.",
        "vadsrijk.huisdieren.thuis": "%s blijft lekker thuis.",
        "vadsrijk.huisdieren.naam_van": "%s van %s",   # N-EXTRA: the nameplate "Mini Koning Klodder van Juiced"
        # the pets
        "vadsrijk.huisdier.klodderreus.naam": "Mini Grauwe Klodderreus",
        "vadsrijk.huisdier.klodderreus.tekst": "Viel ooit met een enorme PLONS in de kampvijver. Nu past hij in je broekzak. Nog steeds een beetje grauw, maar elke knuffel helpt.",
        "vadsrijk.huisdier.klodderreus.bron": "Versla de Grauwe Klodderreus bij de kampvijver van Knuffelkamp voor het eerst.",
        "vadsrijk.huisdier.koning_klodder.naam": "Mini Koning Klodder",
        "vadsrijk.huisdier.koning_klodder.tekst": "Zijn kroontje is drie maten te groot. Hij eist elke ochtend een troon en krijgt een kussen. Hij is er heel blij mee.",
        "vadsrijk.huisdier.koning_klodder.bron": "Versla Koning Klodder voor het eerst.",
        "vadsrijk.huisdier.galmwolf.naam": "Mini Galmwolf",
        "vadsrijk.huisdier.galmwolf.tekst": "Een kristalpupje dat keihard naar de maan wil huilen, maar alleen 'piep' kan zeggen. Glimt een beetje in het donker.",
        "vadsrijk.huisdier.galmwolf.bron": "Versla de Galmwolf in de Galmhellingen voor het eerst.",
    }
    return t


def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def assets(a, w=_schrijf_json):
    ani = os.path.join(a, "geckolib", "animations", "entity")
    w(os.path.join(ani, "vr_huisdier_klodderreus.animation.json"), {"format_version": "1.8.0", "animations": anims_klodder()})
    w(os.path.join(ani, "vr_huisdier_koning_klodder.animation.json"), {"format_version": "1.8.0", "animations": anims_klodder(kroon=True)})
    w(os.path.join(ani, "vr_huisdier_galmwolf.animation.json"), {"format_version": "1.8.0", "animations": anims_galmwolf()})
    bron = os.path.join(a, "textures", "vadsrijk", "bazen", "kk_klodder_roze.png")
    doel = os.path.join(a, "textures", "entity", "vr_huisdier_klodderreus.png")
    os.makedirs(os.path.dirname(doel), exist_ok=True)
    grauwe_klodder(bron).save(doel)


def build(h):
    assets(h.A, h.w)
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    assets(a)
    t = teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(t)
        _schrijf_json(p, data)
    print(f"vadsrijk_huisdieren: 3 pets, {len(t)} texts")


if __name__ == "__main__":
    _direct()
