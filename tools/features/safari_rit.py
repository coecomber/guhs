"""
1.6.0 Op Safari! step 2, slice S3 (vervoer & momenten): the resources of the rides and the moments (Java:
feature/safari/rit + feature/safari/momenten; park data: tools/features/safari_park/zone_s3_rit.py + route_data.py).

  * the safari truck: the phase-0 model (tools/features/safari_rit_bron/, from safari-ontwerp/objecten) plus a little
    trailer for the players' own guhs;
  * the canoe and the kabelbaan gondola (made here, in the same chibi style);
  * the schemabord block (blockstate, model, texture, item, loot);
  * the Dutch texts (tools/lang/en/c163_safari_rit.json has the English).

Runs inside make_resources (build(h)), or on its own (python tools/features/safari_rit.py, from the project root):
then it writes the files directly and merges its texts into the language files.
"""
import copy
import json
import math
import os
import sys

import numpy as np
from PIL import Image

NAME = "safari_rit"
CLOTHES = []
BONES = {}

HIER = os.path.dirname(os.path.abspath(__file__))
BRON = os.path.join(HIER, "safari_rit_bron")

ZONES = ["entree", "savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie"]
ZONE_NL = {"entree": "Entree", "savanne": "Savanne", "delta": "Rivierdelta", "regenwoud": "Regenwoud", "bamboe": "Bamboebergen",
           "ijskust": "IJskust", "oertijd": "Oerijstijd-vallei", "europa": "Europees Woud", "australie": "Australië"}
VOEDER_NL = {"entree": "de geitjes", "savanne": "de olifanten", "delta": "de nijlpaarden", "regenwoud": "de orang-oetans",
             "bamboe": "de reuzenpanda's", "ijskust": "de pinguïns", "oertijd": "de mammoets", "europa": "de otters",
             "australie": "de kangoeroes"}
FEITEN = {
    "entree": ["Welkom aan boord van de safaritruck! Armen en pootjes binnen de truck, njeg.",
               "In de aaihoek wonen een geitje, een alpaca, een cavia en een ezeltje. Ze zijn dol op knuffels.",
               "Op het schemabord bij het plein zie je wanneer de voedertijden zijn. Vahoeg!",
               "Alle dieren in dit park zijn net zo lief als een guh. Bijna net zo lief dan."],
    "savanne": ["Leeuwen slapen wel 20 uur per dag. Dat noemen wij een vadse leeuw.",
                "Een giraffe heeft net zoveel nekwervels als jij: zeven! Alleen een stuk langer.",
                "Stokstaartjes houden om de beurt de wacht. Kijk maar: eentje staat altijd rechtop!",
                "Elke zebra heeft een eigen streepjespatroon, net als jouw vingerafdruk."],
    "delta": ["Nijlpaarden kunnen niet zwemmen: ze lopen gewoon over de bodem. Supervads.",
              "Flamingo's zijn roze door wat ze eten. Eet jij veel kaas, word je dan geel?",
              "Een capibara is het relaxte knaagdier van de wereld. Iedereen mag op zijn rug zitten.",
              "De zeekoe is familie van de olifant. Echt waar, njeg!"],
    "regenwoud": ["Een luiaard gaat maar één keer per week naar de wc. Dat is pas rustig aan doen.",
                  "De okapi lijkt op een zebra, maar is familie van de giraffe. Heel verlegen!",
                  "Orang-oetans bouwen elke avond een nieuw bed van bladeren in de boom.",
                  "Achter de waterval wonen glimwormpjes. Ze geven zelf licht, net als nachtlampjes."],
    "bamboe": ["Een reuzenpanda eet wel 12 uur per dag bamboe. Een echte smulguh.",
               "Rode panda's gebruiken hun dikke staart als dekentje als het koud is.",
               "Gibbons slingeren zo snel van tak naar tak dat het lijkt alsof ze vliegen.",
               "In de mist van de bamboebergen hoor je de panda's knabbelen. Luister maar!"],
    "ijskust": ["Pinguïns kunnen niet vliegen, maar zwemmen supersnel. En glijden op hun buik!",
                "Een ijsbeer is onder zijn witte vacht eigenlijk zwart. Gek hè?",
                "Zeehonden kunnen hun neusgaten dichtdoen onder water.",
                "Als je geluk hebt, zie je hier 's nachts het noorderlicht. En misschien de narwal..."],
    "oertijd": ["Mammoets leefden lang geleden, maar hier mogen ze gewoon even logeren.",
                "De sabeltandtijger heeft lange tanden, maar hij gebruikt ze vooral om te glimlachen.",
                "In de warme bronnen nemen de mammoets een lekker warm bad.",
                "Een wolharige neushoorn draagt zijn eigen winterjas. Altijd!"],
    "europa": ["Bevers bouwen dammen van takken en modder. Echte bouwmeesters!",
               "Een egel heeft wel 5000 stekels. Niet aaien tegen de haren in!",
               "Otters houden elkaars pootje vast als ze slapen, zodat ze niet wegdrijven.",
               "Een das woont in een burcht met heel veel kamertjes. Gezellig!"],
    "australie": ["Een kangoeroe kan niet achteruit lopen. Alleen maar vooruit springen!",
                  "Koala's slapen wel 18 uur per dag. Bijna net zoveel als een vadse guh.",
                  "De poepjes van een wombat zijn vierkant. Echt waar, njeg!",
                  "De quokka is het vrolijkste dier van de wereld. Hij lacht altijd!"],
}

TEKSTEN = {
    "entity.guhs.safari_truck": "Safaritruck",
    "entity.guhs.safari_kano": "Kano",
    "entity.guhs.safari_gondel": "Gondel van de kabelbaan",
    "entity.guhs.safari_race_struisvogel": "Racestruisvogel",
    "block.guhs.safari_schemabord": "Schemabord",
    "item.guhs.safari_schemabord": "Schemabord",
    "gui.guhs.safari.rit.vol": "De truck is vol, njeg! De volgende komt zo.",
    "gui.guhs.safari.rit.instappen": "Instappen! Sluipen = uitstappen. Je eigen guhs zitten in het aanhangertje.",
    "gui.guhs.safari.rit.gids": "[Gids Guh] Halte %s! %s",
    "gui.guhs.safari.kabel.aangekomen": "Aangekomen: %s! Sluipen om uit te stappen.",
    "gui.guhs.safari.kabel.onderweg": "De gondel is onderweg. Wacht op het perron!",
    "gui.guhs.safari.kabel.instappen": "Instappen! Geniet van het uitzicht. Uitstappen kan alleen in een station.",
    "gui.guhs.safari.kabel.blijf_zitten": "Blijf zitten, njeg! Het is hier heel hoog.",
    "gui.guhs.safari.kano.rit": "Daar gaan we! De kano peddelt zelf de rivier op en weer terug.",
    "gui.guhs.safari.kano.vrij": "Je eigen kano! W/S peddelen, A/D sturen. Sluipen = uitstappen.",
    "gui.guhs.safari.kano.terug": "Terug bij de steiger. Vahoeg!",
    "gui.guhs.safari.race.uitleg": "Struisvogelrace: %s rondjes langs de hooibalen! W = rennen, A/D = sturen, spatie = springen.",
    "gui.guhs.safari.race.rennen": "RENNEN!",
    "gui.guhs.safari.race.ronde": "Ronde %s van %s!",
    "gui.guhs.safari.race.finish": "Finish! Jouw tijd: %s.",
    "gui.guhs.safari.race.finish_top": "Finish! Jouw tijd: %s. Je staat op plek %s van het scorebord! Vahoeg!",
    "gui.guhs.safari.race.gestopt": "De race is gestopt. Je struisvogel loopt terug naar de start.",
    "gui.guhs.safari.race.bord": "Struisvogelrace: snelste tijden",
    "gui.guhs.safari.moment.voeder_straks": "Over 1 minuut: voedertijd bij %s! (%s)",
    "gui.guhs.safari.moment.voeder_nu": "Voedertijd bij %s is begonnen! (%s) Nu komen ook de verlegen diertjes tevoorschijn.",
    "gui.guhs.safari.moment.praatje_straks": "Over 1 minuut: rangerpraatje bij de truckhalte van %s!",
    "gui.guhs.safari.moment.praatje_nu": "Het rangerpraatje bij de halte van %s begint! Blijf luisteren voor een paar Safarimunten.",
    "gui.guhs.safari.moment.nacht_straks": "Vannacht is er nachtsafari! De lampiontrucks rijden zo.",
    "gui.guhs.safari.moment.nacht_nu": "De nachtsafari is begonnen! Stap in een lampiontruck en kijk naar de gloeiende oogjes.",
    "gui.guhs.safari.moment.jarig": "Vandaag is de %s jarig! Foto's van de jarige leveren dubbele Safarimunten op. Hiep hiep hoera!",
    "gui.guhs.safari.ranger.voeder": "Ranger Guh (voedertijd)",
    "gui.guhs.safari.ranger.praatje": "Ranger Guh (praatje)",
    "gui.guhs.safari.ranger.zegt": "[Ranger Guh] %s",
    "gui.guhs.safari.ranger.bedankt": "[Ranger Guh] Bedankt voor het luisteren, njeg! Hier zijn wat Safarimunten.",
    "gui.guhs.safari.ranger.bedankt_vol": "[Ranger Guh] Bedankt voor het luisteren! (Je hebt vandaag al genoeg munten verdiend met praatjes.)",
    "gui.guhs.safari.ranger.hoi": "[Ranger Guh] Hoi! Ssst, niet te hard, dan schrikken de dieren. Vahoeg!",
    "gui.guhs.safari.schema.titel": "Schemabord Safaripark Vadse Bergen (nu: %s)",
    "gui.guhs.safari.schema.voeder": "Voedertijd: %s (%s)",
    "gui.guhs.safari.schema.praatje": "Rangerpraatje bij de halte van %s",
    "gui.guhs.safari.schema.nacht": "Nachtsafari met lampiontrucks",
    "gui.guhs.safari.schema.nu": " - nu!",
    "gui.guhs.safari.schema.geweest": " - geweest",
    "gui.guhs.safari.schema.straks": "",
    "gui.guhs.safari.schema.jarig": "Jarig vandaag: de %s! (dubbele munten voor zijn foto's)",
    "commands.guhs.safari.ritten": "Route %s blokken, %s haltes, %s trucks · kano %s · kabelbaan %s · racebaan %s · jarig: %s · nachtsafari nu: %s",
}
for _z in ZONES:
    TEKSTEN[f"gui.guhs.safari.zone.{_z}"] = ZONE_NL[_z]
    TEKSTEN[f"gui.guhs.safari.voeder.dieren.{_z}"] = VOEDER_NL[_z]
    for _i, _f in enumerate(FEITEN[_z]):
        TEKSTEN[f"gui.guhs.safari.feit.{_z}.{_i}"] = _f


# ======================================================================================================================
# models
def _bd():
    sys.path.insert(0, HIER)
    sys.path.insert(0, os.path.dirname(HIER))
    try:
        from features import boerderij_dieren as bd
    except ImportError:
        import boerderij_dieren as bd
    return bd


def _uv_copy(cube):
    return copy.deepcopy(cube["uv"])


def truck():
    """The phase-0 truck + a trailer (its faces borrow the truck's own paint: green sides, planks, a wheel)."""
    with open(os.path.join(BRON, "safaritruck.geo.json"), encoding="utf-8") as f:
        g = json.load(f)
    geom = g["minecraft:geometry"][0]
    bones = {b["name"]: b for b in geom["bones"]}
    body = bones["truck"]["cubes"]
    vloer, zijkant = body[0], body[1]
    wiel = bones["wiel_lv"]["cubes"][0]
    def c(origin, size, bron):
        return {"origin": origin, "size": size, "uv": _uv_copy(bron)}
    aanhanger = [
        c([-1, 6, 36], [2, 2, 10], zijkant),                    # drawbar
        c([-12, 5, 45], [24, 3, 24], vloer),                    # floor
        c([-12, 8, 45], [1, 6, 24], zijkant), c([11, 8, 45], [1, 6, 24], zijkant),
        c([-12, 8, 45], [24, 6, 1], zijkant), c([-12, 8, 68], [24, 6, 1], zijkant),
    ]
    wielen = [c([-15, 0, 52], [3, 9, 9], wiel), c([12, 0, 52], [3, 9, 9], wiel)]
    geom["bones"].append({"name": "aanhanger", "parent": "truck", "pivot": [0, 6, 40], "cubes": aanhanger})
    geom["bones"].append({"name": "aanhanger_wielen", "parent": "aanhanger", "pivot": [0, 4.5, 56.5], "cubes": wielen})
    geom["description"]["visible_bounds_width"] = 6
    with open(os.path.join(BRON, "safaritruck.animation.json"), encoding="utf-8") as f:
        anim = json.load(f)
    a = anim["animations"]["animation.safaritruck.rijden"]
    a["bones"]["aanhanger_wielen"] = {"rotation": {"0.0": [0, 0, 0], "0.8": [360, 0, 0]}}
    tex = Image.open(os.path.join(BRON, "safaritruck.png")).convert("RGBA")
    return g, tex, anim


def kano(bd):
    bd.ATLAS, bd.S = 128, 2
    A = bd.Atlas(31)
    romp = bd.plain((214, 92, 52), 8)
    rand = bd.plain((150, 62, 36), 6)
    hout = bd.plain((196, 150, 96), 8)
    peddel = bd.plain((170, 120, 70), 6)
    cubes = [
        bd.cube(A, [-5, 0, -18], [10, 2, 36], romp),
        bd.cube(A, [-6, 1, -18], [1, 5, 36], romp, {"up": rand}), bd.cube(A, [5, 1, -18], [1, 5, 36], romp, {"up": rand}),
        bd.cube(A, [-4, 1, -23], [8, 5, 5], romp, {"up": rand}), bd.cube(A, [-4, 1, 18], [8, 5, 5], romp, {"up": rand}),
        bd.cube(A, [-2, 4, -26], [4, 3, 3], rand), bd.cube(A, [-2, 4, 23], [4, 3, 3], rand),
        bd.cube(A, [-5, 3, -9], [10, 1, 3], hout), bd.cube(A, [-5, 3, 7], [10, 1, 3], hout),
    ]
    ped = [bd.cube(A, [-13, 9.5, -5], [26, 1, 1], peddel), bd.cube(A, [-16, 7, -6.5], [3, 5, 4], peddel),
           bd.cube(A, [13, 7, -6.5], [3, 5, 4], peddel)]
    bones = [bd.bone("root", None, [0, 0, 0], []), bd.bone("kano", "root", [0, 0, 0], cubes),
             bd.bone("peddel", "kano", [0, 10, -4.5], ped)]
    g = bd.geo("kano", bones, 3, 2)
    anim = {"format_version": "1.8.0", "animations": {"animation.kano.peddel": {"loop": True, "animation_length": 1.2, "bones": {
        "peddel": {"rotation": {"0.0": [0, 20, 25], "0.6": [0, -20, -25], "1.2": [0, 20, 25]}},
        "kano": {"rotation": {"0.0": [0, 0, 1.5], "0.6": [0, 0, -1.5], "1.2": [0, 0, 1.5]}}}}}}
    return g, Image.fromarray(A.img), anim


def gondel(bd):
    bd.ATLAS, bd.S = 256, 1
    A = bd.Atlas(32)
    rood = bd.plain((206, 58, 64), 8)
    wit = bd.plain((240, 236, 226), 5)
    grijs = bd.plain((92, 96, 104), 5)
    hout = bd.plain((150, 110, 70), 6)
    hout_licht = bd.plain((196, 156, 104), 6)
    c = []
    c.append(bd.cube(A, [-14, 0, -14], [28, 2, 28], hout))                                   # floor
    for x0, z0 in ((-14, -14), (12, -14), (-14, 12), (12, 12)):
        c.append(bd.cube(A, [x0, 2, z0], [2, 24, 2], rood))                                  # corner posts
    for (o, s) in (([-12, 2, -14], [24, 10, 2]), ([-12, 2, 12], [24, 10, 2]), ([-14, 2, -12], [2, 10, 24]), ([12, 2, -12], [2, 10, 24])):
        c.append(bd.cube(A, o, s, rood, {"up": hout_licht}))                                 # low walls (open windows above)
    c.append(bd.cube(A, [-16, 26, -16], [32, 2, 32], wit))                                   # roof
    c.append(bd.cube(A, [-10, 28, -10], [20, 2, 20], rood))
    c.append(bd.cube(A, [-10, 4, -12], [20, 2, 3], hout_licht))                              # bench
    arm = [bd.cube(A, [-1, 30, -1], [2, 50, 2], grijs), bd.cube(A, [-3, 80, -4], [6, 4, 8], grijs)]
    bones = [bd.bone("root", None, [0, 0, 0], []), bd.bone("cabine", "root", [0, 84, 0], c),
             bd.bone("arm", "cabine", [0, 84, 0], arm)]
    g = bd.geo("gondel", bones, 3, 6)
    anim = {"format_version": "1.8.0", "animations": {"animation.gondel.schommel": {"loop": True, "animation_length": 3.0, "bones": {
        "cabine": {"rotation": {"0.0": [1.5, 0, -2], "1.5": [-1.5, 0, 2], "3.0": [1.5, 0, -2]}}}}}}
    return g, Image.fromarray(A.img), anim


def schemabord_textuur():
    rng = np.random.default_rng(7)
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = (120, 84, 52)
    a[..., 3] = 255
    a[1:15, 1:15, :3] = (238, 226, 196)
    for y in (3, 6, 9, 12):
        a[y, 3:13, :3] = (120, 110, 96)
        a[y, 3, :3] = (230, 120, 150)
    a[1:15, 1:15, :3] = np.clip(a[1:15, 1:15, :3].astype(int) + rng.integers(-6, 6, (14, 14, 1)), 0, 255)
    return Image.fromarray(a, "RGBA")


def schemabord_model():
    return {
        "parent": "minecraft:block/block",
        "textures": {"bord": "guhs:block/safari_schemabord", "paal": "minecraft:block/spruce_log", "particle": "minecraft:block/spruce_planks"},
        "elements": [
            {"from": [1, 0, 7], "to": [3, 24, 9], "faces": {d: {"texture": "#paal"} for d in ("north", "south", "east", "west", "up", "down")}},
            {"from": [13, 0, 7], "to": [15, 24, 9], "faces": {d: {"texture": "#paal"} for d in ("north", "south", "east", "west", "up", "down")}},
            {"from": [0, 9, 7.5], "to": [16, 23, 8.5], "faces": {
                "north": {"texture": "#bord", "uv": [0, 1, 16, 15]}, "south": {"texture": "#bord", "uv": [0, 1, 16, 15]},
                "east": {"texture": "#paal"}, "west": {"texture": "#paal"}, "up": {"texture": "#paal"}, "down": {"texture": "#paal"}}},
        ],
    }


# ======================================================================================================================
def bestanden():
    """Every file: {path under src/main/resources: json object or PIL image}."""
    bd = _bd()
    A, D = "assets/guhs", "data/guhs"
    uit = {}
    for naam, (g, tex, anim) in (("safaritruck", truck()), ("kano", kano(bd)), ("gondel", gondel(bd))):
        uit[f"{A}/geckolib/models/entity/safari_rit/{naam}.geo.json"] = g
        uit[f"{A}/textures/entity/safari_rit/{naam}.png"] = tex
        uit[f"{A}/geckolib/animations/entity/safari_rit/{naam}.animation.json"] = anim
    uit[f"{A}/blockstates/safari_schemabord.json"] = {"variants": {
        f"facing={f}": {"model": "guhs:block/safari_schemabord", **({"y": r} if r else {})}
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}
    uit[f"{A}/models/block/safari_schemabord.json"] = schemabord_model()
    uit[f"{A}/textures/block/safari_schemabord.png"] = schemabord_textuur()
    uit[f"{A}/items/safari_schemabord.json"] = {"model": {"type": "minecraft:model", "model": "guhs:block/safari_schemabord"}}
    uit[f"{D}/loot_table/blocks/safari_schemabord.json"] = {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": "guhs:safari_schemabord"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
    return uit


def build(h):
    for pad, obj in bestanden().items():
        if isinstance(obj, Image.Image):
            vol = os.path.join("src", "main", "resources", *pad.split("/"))
            os.makedirs(os.path.dirname(vol), exist_ok=True)
            obj.save(vol)
        else:
            base = h.A if pad.startswith("assets/") else h.D
            h.w(base + pad[pad.index("/", pad.index("/") + 1):], obj)
    for k, v in TEKSTEN.items():
        h.NL[k] = v


def _zelf():
    """On its own: write the files and merge the texts into nl_nl.json / en_us.json."""
    root = os.path.join("src", "main", "resources")
    for pad, obj in bestanden().items():
        vol = os.path.join(root, *pad.split("/"))
        os.makedirs(os.path.dirname(vol), exist_ok=True)
        if isinstance(obj, Image.Image):
            obj.save(vol)
        else:
            with open(vol, "w", encoding="utf-8", newline="\n") as f:
                json.dump(obj, f, indent=2, ensure_ascii=False)
                f.write("\n")
    en_pad = os.path.join(HIER, "..", "lang", "en", "c163_safari_rit.json")
    with open(en_pad, encoding="utf-8") as f:
        en = json.load(f)
    for taal, teksten in (("nl_nl", TEKSTEN), ("en_us", {k: en.get(k, v) for k, v in TEKSTEN.items()})):
        p = os.path.join(root, "assets", "guhs", "lang", taal + ".json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(teksten)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"safari_rit: {len(bestanden())} bestanden, {len(TEKSTEN)} teksten")


if __name__ == "__main__":
    _zelf()
