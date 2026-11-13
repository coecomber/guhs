"""
1.10.0 "Het Vadsrijk", fix round 1 "momenten" (Java: feature/vadsrijk/momenten; notes: NOTITIES_FIX_MOMENTEN.md).

Generated (no hand-made art), with the wezens slice's model builder and painter:
  * the GeckoLib caravan (assets/guhs/geckolib/{models,animations}/entity/vadsrijk/momenten/karavaan.*): a big friendly snail with
    a guh face and guh ears pulling a little cart with goods under a striped awning; animations kruip / stil / bang;
    texture textures/vadsrijk/momenten/karavaan.png;
  * the Regenboogdrankje icon (textures/item/vadsrijk/momenten/regenboogdrankje.png) + item model;
  * all Dutch texts (en_us = the same Dutch).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_momenten.py
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

try:
    import vadsrijk_wezens as W
except ImportError:  # (inside make_resources: the package import)
    from features import vadsrijk_wezens as W

SUB = "vadsrijk/momenten"


# =====================================================================================================================
# the caravan model (front = north = -z; 16 px = 1 block; the trader sits on the bench at z ~ -2, y ~ 15)
# =====================================================================================================================
def model_karavaan():
    m = W.Model("karavaan", px=4, breedte=128, bounds=(4, 3, 1.0))
    m.bone("root")
    # --- the snail: a long soft foot, the head with the guh face, guh ears, two little feelers, the big shell
    m.bone("slak", "root", (0, 0, -16))
    m.cube("slak", "huid", (-5, 0, -32), (10, 5, 26))
    m.bone("kop", "slak", (0, 5, -28))
    m.cube("kop", "huid", (-5, 3, -35), (10, 10, 8), gezicht={"ey": 0.45, "schaal": 1.0})
    m.cube("kop", "huid", (-4, 13, -34), (8, 1.5, 6))
    m.bone("oor_l", "kop", (-3.8, 14, -31), (0, 0, 18))
    m.cube("oor_l", "huid", (-5.8, 13.5, -32), (3, 3.5, 2))
    m.cube("oor_l", "oor", (-5.3, 14, -32.3), (2, 2.5, 0.5))
    m.bone("oor_r", "kop", (3.8, 14, -31), (0, 0, -18))
    m.spiegel("oor_r", "huid", (-5.8, 13.5, -32), (3, 3.5, 2))
    m.spiegel("oor_r", "oor", (-5.3, 14, -32.3), (2, 2.5, 0.5))
    m.bone("sprieten", "kop", (0, 14.5, -33))
    m.cube("sprieten", "huid", (-1.6, 14.5, -33.5), (0.8, 4, 0.8), rot=(-15, 0, 12), pivot=(-1.2, 14.5, -33))
    m.cube("sprieten", "tip", (-2.6, 18.2, -34.6), (1.6, 1.6, 1.6))
    m.cube("sprieten", "huid", (0.8, 14.5, -33.5), (0.8, 4, 0.8), rot=(-15, 0, -12), pivot=(1.2, 14.5, -33))
    m.cube("sprieten", "tip", (1.0, 18.2, -34.6), (1.6, 1.6, 1.6))
    m.bone("huis", "slak", (0, 5, -16))
    m.cube("huis", "schelp", (-5.5, 5, -23), (11, 14, 15))
    m.cube("huis", "schelp", (-4.5, 19, -21), (9, 2, 11))
    # --- the cart: tow bar, box, bench, wheels, goods, an awning
    m.bone("kar", "root", (0, 0, 8))
    m.cube("kar", "donker", (-0.75, 4, -9), (1.5, 1.5, 6))
    m.cube("kar", "hout", (-9, 5, -4), (18, 8, 24))
    m.cube("kar", "rand", (-9.5, 13, -4.5), (19, 1, 25))
    m.cube("kar", "donker", (-7, 13, -4), (14, 2.5, 4))          # the bench (Kruimel sits here)
    m.cube("kar", "kussen", (-6, 15.5, -3.5), (12, 0.8, 3))
    for i, (x, z, s) in enumerate(((-7.5, 2, (6, 5, 6)), (-0.5, 1.5, (6, 6, 6)), (-7.5, 10, (5, 7, 5)), (0.5, 10, (7, 4, 8)), (-3, 16, (6, 3, 3)))):
        rol = ("kist", "zak", "ton", "stof", "kist")[i]
        m.cube("kar", rol, (x, 14, z), s)
    for z in (-3, 18):
        m.cube("kar", "donker", (-9, 14, z), (1, 11, 1))
        m.spiegel("kar", "donker", (-9, 14, z), (1, 11, 1))
    m.cube("kar", "luifel", (-10, 25, -4), (20, 1.5, 24))
    for naam, x, z in (("wiel_lv", -10.5, 0), ("wiel_rv", 9, 0), ("wiel_la", -10.5, 15), ("wiel_ra", 9, 15)):
        m.bone(naam, "kar", (x + 0.75, 3.5, z + 3.5))
        m.cube(naam, "wiel", (x, 0.5, z + 0.5), (1.5, 6, 6))
        m.cube(naam, "as", (x - 0.2, 3, z + 3), (1.9, 1, 1))
    return m


def anims_karavaan():
    kf, anim = W.kf, W.anim
    wielen = {n: {"rotation": kf([(0, (0, 0, 0)), (2.0, (-360, 0, 0))])} for n in ("wiel_lv", "wiel_rv", "wiel_la", "wiel_ra")}
    kruip = {"slak": {"scale": kf([(0, (1, 1, 1)), (0.5, (0.96, 1.03, 1.06)), (1.0, (1, 1, 1)), (1.5, (0.96, 1.03, 1.06)), (2.0, (1, 1, 1))])},
             "kop": {"position": kf([(0, (0, 0, 0)), (0.5, (0, 0.3, -0.8)), (1.0, (0, 0, 0)), (1.5, (0, 0.3, -0.8)), (2.0, (0, 0, 0))]),
                     "rotation": kf([(0, (0, -3, 0)), (1.0, (0, 3, 0)), (2.0, (0, -3, 0))])},
             "huis": {"position": kf([(0, (0, 0, 0)), (0.5, (0, 0.4, 0)), (1.0, (0, 0, 0)), (1.5, (0, 0.4, 0)), (2.0, (0, 0, 0))])},
             "oor_l": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 0, -10)), (1.0, (0, 0, 0)), (1.5, (0, 0, -10)), (2.0, (0, 0, 0))])},
             "oor_r": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 0, 10)), (1.0, (0, 0, 0)), (1.5, (0, 0, 10)), (2.0, (0, 0, 0))])},
             "sprieten": {"rotation": kf([(0, (0, 0, 0)), (0.5, (8, 0, 0)), (1.0, (0, 0, 0)), (1.5, (8, 0, 0)), (2.0, (0, 0, 0))])},
             "kar": {"position": kf([(0, (0, 0, 0)), (1.0, (0, 0.15, 0)), (2.0, (0, 0, 0))])}}
    kruip.update(wielen)
    stil = {"huis": {"scale": kf([(0, (1, 1, 1)), (1.5, (1.02, 1.03, 1.02)), (3.0, (1, 1, 1))])},
            "kop": {"rotation": kf([(0, (0, 0, 0)), (1.0, (-6, 10, 0)), (2.0, (0, -10, 0)), (3.0, (0, 0, 0))])},
            "oor_l": {"rotation": kf([(0, (0, 0, 0)), (1.5, (0, 0, -6)), (3.0, (0, 0, 0))])},
            "oor_r": {"rotation": kf([(0, (0, 0, 0)), (1.5, (0, 0, 6)), (3.0, (0, 0, 0))])},
            "sprieten": {"rotation": kf([(0, (0, 0, 0)), (0.75, (10, 0, 0)), (1.5, (0, 0, 0)), (2.25, (-6, 0, 0)), (3.0, (0, 0, 0))])}}
    bang = {"kop": {"position": kf([(0, (0, -1, 5)), (0.4, (0, -1, 5))]), "rotation": kf([(0, (10, 0, 0)), (0.4, (10, 0, 0))])},
            "huis": {"rotation": kf([(0, (0, 0, -3)), (0.1, (0, 0, 3)), (0.2, (0, 0, -3)), (0.3, (0, 0, 3)), (0.4, (0, 0, -3))])},
            "oor_l": {"rotation": kf([(0, (0, 0, 40)), (0.4, (0, 0, 40))])},
            "oor_r": {"rotation": kf([(0, (0, 0, -40)), (0.4, (0, 0, -40))])},
            "sprieten": {"scale": kf([(0, (1, 0.4, 1)), (0.4, (1, 0.4, 1))])}}
    return {"kruip": anim(2.0, kruip), "stil": anim(3.0, stil), "bang": anim(0.4, bang)}


PALET = {
    "huid": ((255, 196, 226), "glans", None),
    "oor": ((255, 160, 200), None, None),
    "tip": ((255, 120, 180), None, None),
    "schelp": ((250, 176, 122), None, None),
    "hout": ((184, 132, 88), "strepen", (164, 114, 74)),
    "rand": ((150, 104, 70), None, None),
    "donker": ((112, 76, 54), None, None),
    "kussen": ((255, 130, 180), None, None),
    "kist": ((196, 146, 92), "strepen", (170, 122, 76)),
    "zak": ((228, 210, 168), "plekjes", (206, 186, 146)),
    "ton": ((150, 100, 70), "strepen", (120, 80, 56)),
    "stof": ((170, 140, 255), "strepen", (255, 150, 200)),
    "luifel": ((255, 244, 248), "strepen", (255, 140, 190)),
    "wiel": ((120, 80, 56), None, None),
    "as": ((80, 60, 50), None, None),
}


def schilder_karavaan(m):
    img = W.schilder(m, PALET, "blij", 1700, False)
    a = np.asarray(img).copy()
    S = m.px
    # a spiral on the sides of the shell, and a lighter rim on its top
    for reg in m.regios:
        if reg["rol"] != "schelp":
            continue
        x, y, w, h = reg["r"]
        blok = a[y * S:(y + h) * S, x * S:(x + w) * S]
        if reg["f"] in ("east", "west") and h >= 10:
            hh, ww = blok.shape[:2]
            cx, cy = ww * 0.5, hh * 0.52
            pts = []
            for i in range(160):
                t = i / 159 * 3.1 * math.pi
                rr = (0.08 + 0.42 * t / (3.1 * math.pi)) * min(ww, hh)
                pts.append((cx + rr * math.cos(t), cy + rr * math.sin(t)))
            W._lijn(blok, pts, (196, 110, 80), max(2, S // 2 + 1))
        elif reg["f"] == "up":
            blok[..., :3] = np.clip(blok[..., :3].astype(int) + 14, 0, 255)
    return Image.fromarray(a)


# =====================================================================================================================
# the potion icon
# =====================================================================================================================
def icoon_drankje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 1, 9, 2], fill=(150, 104, 70, 255))                  # cork
    d.rectangle([6, 3, 9, 5], fill=(220, 236, 255, 230))                 # neck
    d.ellipse([2, 5, 13, 15], fill=(220, 236, 255, 230), outline=(90, 70, 110, 255))
    kleuren = [(255, 90, 120), (255, 170, 80), (255, 230, 90), (120, 220, 120), (110, 170, 255), (190, 120, 255)]
    for i, k in enumerate(kleuren):                                        # rainbow liquid in bands
        y = 8 + i
        x0, x1 = (4, 11) if 9 <= y <= 12 else (5, 10)
        if y <= 13:
            d.line([(x0, y), (x1, y)], fill=k + (255,))
    d.point([(5, 8), (6, 7)], fill=(255, 255, 255, 255))                   # sparkle
    d.point([(12, 3), (13, 2), (3, 2)], fill=(255, 200, 230, 255))
    return img


# =====================================================================================================================
# texts
# =====================================================================================================================
def teksten():
    t = {
        "entity.guhs.vr_mom_karavaan": "Karavaanslak",
        "entity.guhs.vr_mom_handelaar": "Kruimel",
        "item.guhs.vr_mom_regenboogdrankje": "Regenboogdrankje",
        "item.guhs.vr_mom_regenboogdrankje.uitleg": "Geeft je 700 Vads en 120 energie, en 90 s Vahoeg, Snelpootjes en een Knuffelschild.",
        "vadsrijk.momenten.drankje.buiten": "Dit drankje werkt alleen in het Vadsrijk.",
        "vadsrijk.momenten.drankje.op": "Je glinstert in alle kleuren van de regenboog!",
        "vadsrijk.momenten.golf.titel": "Het Vadsrijk kleurt weer",
        "vadsrijk.momenten.golf.onder": "De littekens van de draak zijn geheeld",
        "vadsrijk.economie.winkel.karavaan": "Karavaan van Kruimel",
        "vadsrijk.momenten.karavaan.handelaar": "Kruimel",
        "vadsrijk.momenten.karavaan.zegt": "%s: %s",
        "vadsrijk.momenten.karavaan.aankomst.haven": "Karavaan in De Haven! Zeldzame waren, vers van het luchtschip!",
        "vadsrijk.momenten.karavaan.aankomst.kustpad": "Even pauze voor Slakkie. Wil er iemand nog iets kopen?",
        "vadsrijk.momenten.karavaan.aankomst.zwamzoet": "De karavaan staat bij het plein van Zwamzoet! Kom maar kijken!",
        "vadsrijk.momenten.karavaan.aankomst.sporenwoud": "Karavaan aan de rand van het Sporenwoud! Niet niezen van de sporen, hoor.",
        "vadsrijk.momenten.karavaan.help_eerst": "Eerst die rakkers weg, dan doen we zaken!",
        "vadsrijk.momenten.karavaan.aanval.weide": "Help! Klodders vallen de karavaan aan! Slakkie kruipt in zijn huisje!",
        "vadsrijk.momenten.karavaan.aanval.woud": "Help! Boze zwammen bij de karavaan! Red mijn waren!",
        "vadsrijk.momenten.karavaan.opgegeven": "Pfoe, ze hebben het opgegeven en hobbelen weg. Kom op Slakkie, we gaan weer!",
        "vadsrijk.momenten.karavaan.dank.0": "Dankjewel, %s! Zonder jou was ik nu slakkensoep!",
        "vadsrijk.momenten.karavaan.dank.1": "%s, jij bent een echte karavaanheld! Hier, voor de moeite.",
        "vadsrijk.momenten.karavaan.dank.2": "Hoera! Slakkie durft weer uit zijn huisje. Dank je wel, %s!",
        "vadsrijk.momenten.karavaan.beloning": "+%s van Kruimel",
        "vadsrijk.momenten.karavaan.zeldzaam": "Kruimel stopt je stiekem iets zeldzaams toe: %s!",
        "vadsrijk.momenten.karavaan.titel": "Nieuwe titel: Karavaanvriend!",
        "gui.guhs.titels.naam.vr_karavaanvriend": "Karavaanvriend",
        "gui.guhs.titels.hint.vr_karavaanvriend": "Help de karavaan van Kruimel drie keer uit de nood.",
    }
    return t


# =====================================================================================================================
# writing
# =====================================================================================================================
def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(pad, img):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    img.save(pad)


def assets(a, w=_schrijf_json):
    m = model_karavaan()
    w(os.path.join(a, "geckolib", "models", "entity", *SUB.split("/"), "karavaan.geo.json"), m.geo())
    w(os.path.join(a, "geckolib", "animations", "entity", *SUB.split("/"), "karavaan.animation.json"), W.anim_bestand(anims_karavaan()))
    _png(os.path.join(a, "textures", *SUB.split("/"), "karavaan.png"), schilder_karavaan(m))
    _png(os.path.join(a, "textures", "item", *SUB.split("/"), "regenboogdrankje.png"), icoon_drankje())
    item = "vr_mom_regenboogdrankje"
    w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
    w(os.path.join(a, "models", "item", item + ".json"), {"parent": "minecraft:item/generated",
                                                          "textures": {"layer0": f"guhs:item/{SUB}/regenboogdrankje"}})


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
    print(f"vadsrijk_momenten: 1 model, 1 item, {len(t)} texts")


if __name__ == "__main__":
    _direct()
