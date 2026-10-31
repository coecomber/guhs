"""
1.8.1: the kermis' Botsguhs and Spookguhhuis. Java: feature/kermisritten2 (Ritten2Feature, BotsbaanBlockEntity, BotsguhEntity,
SpookhuisBlockEntity, SpookkarEntity, SpookjeEntity, client/Ritten2Client).

  build(h)    the two anchor blocks (floor tiles), the car (seven colours), the cart and the ghost, the proofs, the texts
              (Dutch in nl_nl, English in tools/lang/en/c219_kermisritten2.json)
  ftb(fq)     the two rides: a section of its own in guhs_minigames

Standalone (from the project root): PYTHONPATH=tools python tools/features/kermisritten2.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

HIER = os.path.dirname(os.path.abspath(__file__))
BOTS, SPOOK = "kermisrit_botsguhs", "kermisrit_spookhuis"
MAP = "kermisritten2"
T = "gui.guhs."
FTB_CHAPTER = "guhs_minigames"
FTB_SECTIES = [("kermisritten2", "Botsen en griezelen", f"item:guhs:{BOTS}", None)]

KLEUREN = [(120, 214, 220), (250, 226, 130), (214, 150, 226), (176, 226, 140), (250, 186, 150), (140, 170, 240), (232, 92, 132)]

# key -> (Dutch, English)
TEKSTEN = {
    f"block.guhs.{BOTS}": ("Botsguhs-vloertegel", "Bumper Guhs Floor Tile"),
    f"block.guhs.{SPOOK}": ("Spookguhhuis-tegel", "Spooky Guh House Tile"),
    "entity.guhs.kermis_botsguh": ("Botsguh", "Bumper Guh"),
    "entity.guhs.kermis_spookkar": ("Spookkarretje", "Spooky Cart"),
    "entity.guhs.kermis_spookje": ("Spookje", "Little Ghost"),
    T + "botsguhs.instappen": ("Instappen! W/S rijden, A/D sturen, sluipen = uitstappen. BOTSEN MAAR, VADSJE!",
                               "Hop in! W/S drive, A/D steer, sneak = get out. BUMP AWAY, VADSJE!"),
    T + "botsguhs.bots": ("BOTS! Al %s keer. Vahoeg!", "BUMP! That's %s now. Vahoeg!"),
    T + "botsguhs.bijna": ("Nog tien tellen stroom, njeg! Gauw nog even botsen!", "Ten more seconds of power, njeg! Quick, one more bump!"),
    T + "botsguhs.klaar": ("Rit voorbij! %s keer gebotst: %s kermisbon(nen) voor jou. VAHOEG!",
                           "Ride over! %s bumps: %s fair ticket(s) for you. VAHOEG!"),
    T + "botsguhs.klaar_niks": ("Rit voorbij! %s keer gebotst. Bots er %s voor een kermisbon, vadsje!",
                                "Ride over! %s bumps. Make it %s for a fair ticket, vadsje!"),
    T + "spookhuis.instappen": ("Instappen en vasthouden aan je kaasknabbels! Het karretje gaat zo.",
                                "Hop in and hold on to your cheese snacks! The cart leaves in a moment."),
    T + "spookhuis.niet_nu": ("Dit karretje is al bezet, wacht op het station, njeg.", "This cart is busy, wait at the station, njeg."),
    T + "spookhuis.blijf_zitten": ("Njeg! Blijven zitten, de spookjes rekenen op je!", "Njeg! Stay in your seat, the little ghosts are counting on you!"),
    T + "spookhuis.klaar": ("Je hebt het overleefd! Een kermisbon voor zoveel moed. VAHOEG!",
                            "You survived! A fair ticket for all that bravery. VAHOEG!"),
    T + "spookhuis.boe.0": ("BOE! ...sorry, ben je geschrokken? Ik ook een beetje.", "BOO! ...sorry, did I scare you? Me too, a little."),
    T + "spookhuis.boe.1": ("Hoe-oe-oe! Heb je toevallig een kaasknabbel? Spoken krijgen ook honger.",
                            "Whoo-oo-oo! Got a cheese snack, by any chance? Ghosts get hungry too."),
    T + "spookhuis.boe.2": ("BOE! Dit is mijn griezelige gezicht. Ik heb er lang op geoefend.",
                            "BOO! This is my scary face. I practised it for ages."),
    T + "spookhuis.boe.3": ("Psst! Ik ben niet echt eng hoor, zeg het niet tegen de anderen.",
                            "Psst! I'm not really scary, don't tell the others."),
    T + "spookhuis.boe.4": ("Ik ben het Spook van de Vergeten Vads! Oooh! Zwaai je even terug?",
                            "I am the Ghost of Forgotten Vads! Oooh! Will you wave back?"),
    T + "spookhuis.boe.5": ("Ga je al weg? Kom snel weer griezelen, ik hou je plekje warm. Nou ja, koud.",
                            "Leaving already? Come back for a shiver soon, I'll keep your seat warm. Well, cold."),
}

FTB = [
    ("kermis_botsguhs", "BOTS!", "Stap op de &dGuhkermis&r in een &dBotsguh&r en rijd een hele rit uit. De guhs in de lege karretjes "
     "doen gezellig mee. Vijf botsingen = een &6kermisbon&r.", "guhs:kermisbon", "botsguhs_gereden",
     "BUMP!", "Hop into a &dBumper Guh&r at the &dGuh Fair&r and finish a whole ride. The guhs in the empty cars happily join in. "
     "Five bumps = a &6fair ticket&r.", 0, 0),
    ("kermis_botskampioen", "Botskampioen", "Bots vijftien keer in één rit. Twee kermisbonnen, en de eeuwige bewondering van de guhs.",
     "guhs:kermisbon", "botsguhs_kampioen",
     "Bumper Champion", "Bump fifteen times in one ride. Two fair tickets, and the guhs' eternal admiration.", 2, 0),
    ("kermis_spookhuis", "Hoe-oe-oe!", "Durf jij in het &dSpookguhhuis&r? Stap in een karretje en ontmoet de allerliefste spookjes.",
     "guhs:kermisbon", "spookhuis_gereden",
     "Whoo-oo-oo!", "Do you dare enter the &dSpooky Guh House&r? Hop into a cart and meet the sweetest little ghosts.", 0, 1.5),
]
BEWIJZEN = ("botsguhs_gereden", "botsguhs_kampioen", "spookhuis_gereden")


def _bd():
    sys.path.insert(0, HIER)
    sys.path.insert(0, os.path.dirname(HIER))
    try:
        from features import boerderij_dieren as bd
    except ImportError:
        import boerderij_dieren as bd
    return bd


def auto(bd, kleur):
    """The car: a soft pink bumper all round, a seat, a steering wheel and the pole to the ceiling. Front = -z."""
    bd.ATLAS, bd.S = 256, 1
    A = bd.Atlas(41)
    lak = bd.plain(kleur, 6)
    donker = bd.plain(tuple(int(c * 0.72) for c in kleur), 5)
    rubber = bd.plain((70, 60, 74), 5)
    stoel = bd.plain((255, 244, 214), 5)
    staal = bd.plain((196, 196, 206), 4)
    cubes = [
        bd.cube(A, [-12, 1, -15], [24, 4, 30], rubber),
        bd.cube(A, [-11, 3, -14], [22, 5, 28], lak, {"up": donker}),
        bd.cube(A, [-10, 8, -14], [20, 5, 7], lak),                       # the nose
        bd.cube(A, [-11, 8, 5], [22, 8, 9], lak),                         # the back
        bd.cube(A, [-9, 8, 0], [18, 2, 5], stoel),                        # the seat
        bd.cube(A, [-9, 10, 3], [18, 9, 2], stoel),
        bd.cube(A, [-1, 11, -8], [2, 2, 4], staal),                       # the steering column
        bd.cube(A, [-4, 13, -7], [8, 1, 1], rubber),
    ]
    paal = [bd.cube(A, [-1, 14, 11], [2, 66, 2], staal), bd.cube(A, [-3, 78, 10], [6, 2, 4], rubber)]
    bones = [bd.bone("root", None, [0, 0, 0], []), bd.bone("auto", "root", [0, 0, 0], cubes), bd.bone("paal", "auto", [0, 14, 12], paal)]
    return bd.geo("botsguh", bones, 4, 6), Image.fromarray(A.img)


def kar(bd):
    """The cart: a little purple tub with a ghost on its nose. Front = -z."""
    bd.ATLAS, bd.S = 128, 2
    A = bd.Atlas(42)
    lavendel = bd.plain((150, 120, 200), 8)
    rand = bd.plain((96, 70, 150), 6)
    wit = bd.plain((245, 245, 250), 3)
    oog = bd.plain((30, 26, 40), 1)
    roze = bd.plain((255, 170, 200), 3)
    stoel = bd.plain((232, 92, 132), 6)
    cubes = [
        bd.cube(A, [-9, 2, -14], [18, 3, 28], rand),
        bd.cube(A, [-9, 5, -14], [2, 8, 28], lavendel, {"up": rand}), bd.cube(A, [7, 5, -14], [2, 8, 28], lavendel, {"up": rand}),
        bd.cube(A, [-7, 5, -14], [14, 9, 3], lavendel, {"up": rand}), bd.cube(A, [-7, 5, 12], [14, 9, 2], lavendel, {"up": rand}),
        bd.cube(A, [-7, 5, 1], [14, 3, 8], stoel), bd.cube(A, [-7, 5, -9], [14, 3, 7], stoel),
        bd.cube(A, [-5, 12, -17], [10, 9, 5], wit),
        bd.cube(A, [-3, 16, -17.5], [2, 2, 1], oog), bd.cube(A, [1, 16, -17.5], [2, 2, 1], oog),
        bd.cube(A, [-1, 14, -17.5], [2, 1, 1], roze),
        bd.cube(A, [-8, 0, -11], [3, 3, 4], rand), bd.cube(A, [5, 0, -11], [3, 3, 4], rand),
        bd.cube(A, [-8, 0, 7], [3, 3, 4], rand), bd.cube(A, [5, 0, 7], [3, 3, 4], rand),
    ]
    bones = [bd.bone("root", None, [0, 0, 0], []), bd.bone("kar", "root", [0, 0, 0], cubes)]
    return bd.geo("spookkar", bones, 3, 3), Image.fromarray(A.img)


def spookje(bd):
    """The ghost: a soft white sheet with black eyes and pink cheeks, two little arms that wave. Front = -z, as the ride yaws it."""
    bd.ATLAS, bd.S = 128, 2
    A = bd.Atlas(43)
    wit = bd.plain((244, 244, 250), 3)
    oog = bd.plain((30, 26, 40), 1)
    roze = bd.plain((255, 170, 200), 3)
    romp = [
        bd.cube(A, [-7, 6, -7], [14, 16, 14], wit), bd.cube(A, [-5, 22, -5], [10, 3, 10], wit),
        bd.cube(A, [-7, 3, -7], [4, 3, 4], wit), bd.cube(A, [3, 3, -7], [4, 3, 4], wit), bd.cube(A, [-7, 3, 3], [4, 3, 4], wit),
        bd.cube(A, [3, 3, 3], [4, 3, 4], wit), bd.cube(A, [-2, 3, -2], [4, 3, 4], wit),
        bd.cube(A, [-4, 16, -7.5], [3, 3, 1], oog), bd.cube(A, [1, 16, -7.5], [3, 3, 1], oog),
        bd.cube(A, [-6, 13, -7.5], [3, 2, 1], roze), bd.cube(A, [3, 13, -7.5], [3, 2, 1], roze),
        bd.cube(A, [-1, 11, -7.5], [2, 3, 1], oog),
    ]
    links = [bd.cube(A, [-11, 11, -2], [4, 3, 4], wit)]
    rechts = [bd.cube(A, [7, 11, -2], [4, 3, 4], wit)]
    bones = [bd.bone("root", None, [0, 0, 0], []), bd.bone("spook", "root", [0, 0, 0], romp),
             bd.bone("links", "spook", [-7, 12, 0], links), bd.bone("rechts", "spook", [7, 12, 0], rechts)]
    g = bd.geo("spookje", bones, 3, 3)
    anim = {"format_version": "1.8.0", "animations": {"animation.spookje.boe": {"loop": False, "animation_length": 3.5, "bones": {
        "spook": {"position": {"0.0": [0, -26, 0], "0.3": [0, 3, 0], "0.5": [0, 0, 0], "1.5": [0, 2, 0], "2.5": [0, 0, 0], "3.0": [0, 2, 0], "3.5": [0, -26, 0]},
                  "scale": {"0.0": [0.2, 0.2, 0.2], "0.3": [1.15, 1.15, 1.15], "0.5": [1, 1, 1], "3.0": [1, 1, 1], "3.5": [0.2, 0.2, 0.2]}},
        "links": {"rotation": {"0.3": [0, 0, 0], "0.6": [0, 0, -50], "0.9": [0, 0, -10], "1.2": [0, 0, -50], "1.5": [0, 0, 0]}},
        "rechts": {"rotation": {"0.3": [0, 0, 0], "0.6": [0, 0, 50], "0.9": [0, 0, 10], "1.2": [0, 0, 50], "1.5": [0, 0, 0]}}}}}}
    return g, Image.fromarray(A.img), anim


def tegel_bots():
    """Looks like the Botsguhs floor (retrotegel_paars), with a small pink lightning bolt."""
    img = Image.new("RGBA", (16, 16), (126, 92, 170, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(100, 70, 140, 255))
    d.line([(9, 3), (6, 8), (10, 8), (7, 13)], fill=(255, 170, 200, 255))
    return img


def tegel_spook():
    """Grey paving with a tiny ghost."""
    img = Image.new("RGBA", (16, 16), (138, 132, 142, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(112, 106, 118, 255))
    d.rectangle([5, 5, 10, 11], fill=(240, 240, 248, 255))
    d.point([(6, 7), (9, 7)], fill=(30, 26, 40, 255))
    return img


def bestanden():
    """Every file: {path under src/main/resources: json object or PIL image}."""
    bd = _bd()
    A, D = "assets/guhs", "data/guhs"
    uit = {}
    for i, kleur in enumerate(KLEUREN):
        g, tex = auto(bd, kleur)
        uit[f"{A}/textures/entity/{MAP}/botsguh{'' if i == 0 else '_%d' % i}.png"] = tex
    uit[f"{A}/geckolib/models/entity/{MAP}/botsguh.geo.json"] = g
    leeg = {"format_version": "1.8.0", "animations": {}}
    uit[f"{A}/geckolib/animations/entity/{MAP}/botsguh.animation.json"] = leeg
    g, tex = kar(bd)
    uit[f"{A}/geckolib/models/entity/{MAP}/spookkar.geo.json"] = g
    uit[f"{A}/textures/entity/{MAP}/spookkar.png"] = tex
    uit[f"{A}/geckolib/animations/entity/{MAP}/spookkar.animation.json"] = leeg
    g, tex, anim = spookje(bd)
    uit[f"{A}/geckolib/models/entity/{MAP}/spookje.geo.json"] = g
    uit[f"{A}/textures/entity/{MAP}/spookje.png"] = tex
    uit[f"{A}/geckolib/animations/entity/{MAP}/spookje.animation.json"] = anim
    for blok, tex in ((BOTS, tegel_bots()), (SPOOK, tegel_spook())):
        uit[f"{A}/textures/block/{blok}.png"] = tex
        uit[f"{A}/models/block/{blok}.json"] = {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{blok}"}}
        uit[f"{A}/blockstates/{blok}.json"] = {"variants": {f"facing={f}": {"model": f"guhs:block/{blok}"} for f in ("north", "east", "south", "west")}}
        uit[f"{A}/items/{blok}.json"] = {"model": {"type": "minecraft:model", "model": f"guhs:block/{blok}"}}
    for naam in BEWIJZEN:
        uit[f"{D}/advancement/quest/{naam}.json"] = {"criteria": {"done": {"trigger": "minecraft:impossible"}}}
    return uit


def _schrijf_afbeelding(pad, obj):
    vol = os.path.join("src", "main", "resources", *pad.split("/"))
    os.makedirs(os.path.dirname(vol), exist_ok=True)
    obj.save(vol)


def build(h):
    for pad, obj in bestanden().items():
        if isinstance(obj, Image.Image):
            _schrijf_afbeelding(pad, obj)
        else:
            base = h.A if pad.startswith("assets/") else h.D
            h.w(base + pad[pad.index("/", pad.index("/") + 1):], obj)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def ftb(fq):
    for key, titel, tekst, icoon, bewijs, _, _, x, y in FTB:
        fq.q(key, titel, tekst, icoon, [fq.adv(bewijs)], rewards=(("guhs:kaas_knabbels", 16),), deps=["kermis"], x=x, y=y, xp=100)


def _direct():
    root = os.path.join("src", "main", "resources")
    for pad, obj in bestanden().items():
        if isinstance(obj, Image.Image):
            _schrijf_afbeelding(pad, obj)
        else:
            vol = os.path.join(root, *pad.split("/"))
            os.makedirs(os.path.dirname(vol), exist_ok=True)
            with open(vol, "w", encoding="utf-8", newline="\n") as f:
                json.dump(obj, f, indent=2, ensure_ascii=False)
                f.write("\n")
    en = {k: e for k, (n, e) in TEKSTEN.items()}
    for key, _, _, _, _, titel_en, tekst_en, _, _ in FTB:
        en[f"ftb.{FTB_CHAPTER}.q.{key}.title"] = titel_en
        en[f"ftb.{FTB_CHAPTER}.q.{key}.desc"] = tekst_en
    en[f"ftb.{FTB_CHAPTER}.section.kermisritten2"] = "Bumping and shivering"
    with open(os.path.join("tools", "lang", "en", "c219_kermisritten2.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(root, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"kermisritten2: {len(TEKSTEN)} texts, {len(bestanden())} files")


if __name__ == "__main__":
    _direct()
