"""
1.8.0 (Dromen en vadskracht): de Koepel, the endgame machines on the roof of the Guhtechniek-beurs. Java: feature/koepel
(KoepelFeature, KoepelBlock, KoepelGetallen and one block entity per machine).

  build(h)    the five machines (mega_oven 3x2x2, knabbel_weerstation 1x2x1, sterrenkoepel 3x2x2, draaimolen 3x2x2,
              wolkenvanger 1x1x1): textures (the vadskracht face of tools/features/vadskracht.py plus their own details),
              one block model per face state (snoet) that draws the WHOLE machine from the kern (the Meerblok parts are the
              invisible guhs:machine_deel), item models, loot, recipes, tags, the texts (Dutch in nl_nl, English in
              tools/lang/en/c217_koepel.json) and the blokuitleg entries (tools/wiki_site/blokuitleg.py)
  ftb(fq)     a section "De Koepel" in guhs_techniek, one quest per machine (English: tools/lang/en/c217_koepel_ftb.json)

Standalone (from the project root): PYTHONPATH=tools python tools/features/koepel.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
import vadskracht  # noqa: E402

BLOKKEN = ["mega_oven", "knabbel_weerstation", "sterrenkoepel", "draaimolen", "wolkenvanger"]
KLEUR = {  # basis, accent
    "mega_oven": ((214, 150, 120), (255, 196, 120)),
    "knabbel_weerstation": ((170, 200, 236), (255, 214, 110)),
    "sterrenkoepel": ((120, 104, 190), (255, 236, 150)),
    "draaimolen": ((244, 170, 206), (150, 214, 236)),
    "wolkenvanger": ((214, 220, 236), (180, 150, 230)),
}
G = "gui.guhs.koepel."
BEELDEN = [("De Grote Guh", "The Great Guh"), ("Het Knabbeltje", "The Little Nibble"), ("De Slapende Guh", "The Sleeping Guh"),
           ("Het Guhrad", "The Guh Wheel"), ("De Blub", "The Blub"), ("De Kaaspan", "The Cheese Pan"),
           ("Het Vadsige Oortje", "The Vads Ear"), ("De Rookguh", "The Smoke Guh"), ("De Knuffelbeer", "The Cuddle Bear"),
           ("Het Pieppiepmuisje", "The Squeak Mouse"), ("De Kaasmaan", "The Cheese Moon"), ("De Volle Guh", "The Full Guh")]
OVEN = [("roze_broodjes", "Roze broodjes", "Pink buns"), ("knabbelkoekjes", "Knabbelkoekjes", "Nibble cookies"),
        ("guh_taarten", "Guh-taarten", "Guh cakes"), ("cupcakes", "Guh-cupcakes", "Guh cupcakes"), ("broden", "Broden", "Breads")]

TEKSTEN = {  # key -> (Dutch, English)
    "block.guhs.mega_oven": ("Mega-oven", "Mega Oven"),
    "block.guhs.mega_oven.lore": ("Een industriële guh-bakkerij van 3 bij 2 bij 2. Bakt in bulk en eet bergen knabbels en graan. "
                                  "Lege hand: volgend programma. Sluip + lege hand: de baksels eruit.",
                                  "An industrial guh bakery of 3 by 2 by 2. Bakes in bulk and eats mountains of nibbles and grain. "
                                  "Empty hand: next programme. Sneak + empty hand: take out the baking."),
    "block.guhs.knabbel_weerstation": ("Knabbel-weerstation", "Nibble Weather Station"),
    "block.guhs.knabbel_weerstation.lore": ("Een weertoren. Zolang hij knabbels eet, maakt elke Weer vadser tot 16 blokken bij hem vandaan "
                                            "zijn weertje over 150 blokken: je hele basis!",
                                            "A weather tower. As long as it eats nibbles, every Weather Vadser up to 16 blocks away makes "
                                            "its weather over 150 blocks: your whole base!"),
    "block.guhs.sterrenkoepel": ("Sterrenkoepel", "Star Dome"),
    "block.guhs.sterrenkoepel.lore": ("Het Vadsigheids-monument. Voer hem knabbels: bij elk niveau licht een nieuw guh-sterrenbeeld op, "
                                      "tot een hele sterrenhemel van guhs. Iedereen krijgt er iets liefs van.",
                                      "The Vadsiness Monument. Feed it nibbles: every level lights up a new guh constellation, up to a "
                                      "whole sky full of guhs. Everybody gets something sweet from it."),
    "block.guhs.draaimolen": ("Draaimolen", "Merry-Go-Round"),
    "block.guhs.draaimolen.lore": ("Een guh-pretpark-attractie. Een kaasknabbel is een kaartje: tamme guhs in de buurt rijden mee en "
                                   "worden blij (dan doen ze hun klusjes beter en rennen ze harder in een Guhrad).",
                                   "A guh fun fair ride. A cheese nibble is a ticket: tame guhs nearby ride along and get happy (then "
                                   "they do their chores better and run faster in a Guh Wheel)."),
    "block.guhs.wolkenvanger": ("Wolkenvanger", "Cloud Catcher"),
    "block.guhs.wolkenvanger.lore": ("Een schotel voor op het dak die voorbijdrijvende wolkjes tot pluis condenseert. Hoe hoger hij staat, "
                                     "hoe meer pluis. Een kist eronder vangt het op.",
                                     "A dish for the roof that condenses passing little clouds into fluff. The higher it stands, the "
                                     "more fluff. A chest under it catches it."),
    "block.guhs.koepel.lore.kracht": ("Vraagt %s vadskracht als hij werkt", "Asks %s vadskracht while it works"),
    G + "oven.programma": ("Mega-oven bakt nu: %s (%s)", "Mega Oven now bakes: %s (%s)"),
    G + "oven.uitlezing": ("Bakt: %s", "Baking: %s"),
    G + "station.gevoerd": ("Weerstation gevoerd! Voorraad: %s knabbels", "Weather station fed! Stock: %s nibbles"),
    G + "station.aan": ("Het weerstation draait: elke Weer vadser tot %2$s blokken hier vandaan maakt zijn weertje over %1$s blokken.",
                        "The weather station runs: every Weather Vadser up to %2$s blocks from here makes its weather over %1$s blocks."),
    G + "station.uit": ("Het weerstation slaapt: het wil %s vadskracht en knabbels. Njeg...",
                        "The weather station is asleep: it wants %s vadskracht and nibbles. Njeg..."),
    G + "station.uitlezing": ("Knabbels in voorraad: %s", "Nibbles in stock: %s"),
    G + "koepel.nieuw": ("✦ Op de Sterrenkoepel licht een nieuw sterrenbeeld op: %s (%s/%s)! Iedereen is even uitgerust. VAHOEG!",
                         "✦ A new constellation lights up on the Star Dome: %s (%s/%s)! Everybody feels rested for a while. VAHOEG!"),
    G + "koepel.vol": ("De hele sterrenhemel van guhs brandt al. Onberekenbaar vahoeg!", "The whole sky of guhs is lit already. Incalculably vahoeg!"),
    G + "koepel.gevoerd": ("%s knabbels in de koepel! Nog %s tot het volgende sterrenbeeld.", "%s nibbles in the dome! %s more to the next constellation."),
    G + "koepel.stand": ("Sterrenkoepel: %s/%s sterrenbeelden, nog %s knabbels tot het volgende. De grootste gevers:",
                         "Star Dome: %s/%s constellations, %s more nibbles to the next one. The biggest givers:"),
    G + "koepel.uitlezing": ("Sterrenbeelden: %s/%s", "Constellations: %s/%s"),
    G + "molen.rit": ("Instappen! De draaimolen draait, guhs rijden mee.", "All aboard! The merry-go-round turns, guhs ride along."),
    G + "molen.slaapt": ("Kaartje gekocht, maar de draaimolen wil %s vadskracht. Njeg!", "Ticket bought, but the merry-go-round wants %s vadskracht. Njeg!"),
    G + "molen.draait": ("De draaimolen draait! Wiiie!", "The merry-go-round is turning! Wheee!"),
    G + "molen.kaartje": ("Een kaasknabbel is een kaartje.", "A cheese nibble is a ticket."),
    G + "molen.uitlezing": ("Rit: nog %s seconden", "Ride: %s seconds left"),
    G + "vanger.stand": ("De wolkenvanger vangt hier elke %s seconden een pluisje.", "The cloud catcher catches a bit of fluff here every %s seconds."),
    G + "vanger.dak": ("De wolkenvanger wil open lucht boven zich: zet hem op het dak!", "The cloud catcher wants open sky above it: put it on the roof!"),
    G + "vanger.uitlezing": ("Eén pluisje per %s seconden", "One bit of fluff per %s seconds"),
}
for i, (nl, en) in enumerate(BEELDEN):
    TEKSTEN[G + f"koepel.beeld.{i}"] = (nl, en)
for pid, nl, en in OVEN:
    TEKSTEN[G + f"oven.p.{pid}"] = (nl, en)

UITLEG = {  # blokuitleg (wiki "Zo werkt het")
    "mega_oven": ("Een grote bakkerij van 3 bij 2 bij 2 op vadskracht.\n\n- Klik met een lege hand: het volgende bakprogramma "
                  "(roze broodjes, knabbelkoekjes, guh-taarten, cupcakes, broden).\n- Ingrediënten gaan erin met de hand of met een buis; "
                  "de baksels gaan er met een buis uit, of sluip + lege hand.\n- Hij bakt meer per ingrediënt dan een werkbank en heeft geen "
                  "melk nodig.",
                  "A big bakery of 3 by 2 by 2 on vadskracht.\n\n- Click with an empty hand: the next programme (pink buns, nibble cookies, "
                  "guh cakes, cupcakes, breads).\n- Ingredients go in by hand or by pipe; the baking comes out by pipe, or sneak + empty hand.\n"
                  "- It bakes more per ingredient than a crafting table and needs no milk."),
    "knabbel_weerstation": ("Een weertoren die de Weer vadsers in de buurt groter maakt.\n\n- Voer hem kaasknabbels of knabbelblokken; "
                            "hij eet één knabbel per 30 seconden.\n- Zolang hij vadskracht en knabbels heeft, maakt elke Weer vadser tot 16 "
                            "blokken bij hem vandaan zijn weertje over 150 blokken.",
                            "A weather tower that makes the Weather Vadsers nearby bigger.\n\n- Feed it cheese nibbles or nibble blocks; it eats "
                            "one nibble per 30 seconds.\n- As long as it has vadskracht and nibbles, every Weather Vadser up to 16 blocks from it "
                            "makes its weather over 150 blocks."),
    "sterrenkoepel": ("Het Vadsigheids-monument: hij groeit nooit, hij gaat stralen.\n\n- Klik met kaasknabbels of knabbelblokken: alles "
                      "uit je hand gaat erin.\n- Bij elk niveau licht een nieuw guh-sterrenbeeld op (12 in totaal) en is iedereen online "
                      "even uitgerust.\n- Lege hand: de stand en de grootste gevers.\n- De sterren stralen alleen op vadskracht.",
                      "The Vadsiness Monument: it never grows, it shines.\n\n- Click with cheese nibbles or nibble blocks: everything in your "
                      "hand goes in.\n- Every level lights up a new guh constellation (12 in all) and everybody online feels rested for a while.\n"
                      "- Empty hand: the standing and the biggest givers.\n- The stars only shine on vadskracht."),
    "draaimolen": ("Een draaimolen voor je guhs.\n\n- Klik met een kaasknabbel: dat is een kaartje voor een rit van 30 seconden.\n- Tamme "
                   "guhs tot 8 blokken ver rijden mee en worden blij: dan doen ze hun klusjes beter en lopen ze harder in een Guhrad.\n"
                   "- Jij wordt er gezellig van.",
                   "A merry-go-round for your guhs.\n\n- Click with a cheese nibble: that is a ticket for a ride of 30 seconds.\n- Tame guhs up "
                   "to 8 blocks away ride along and get happy: then they do their chores better and run faster in a Guh Wheel.\n- It makes you "
                   "feel cosy."),
    "wolkenvanger": ("Een schotel die wolkjes tot pluis condenseert.\n\n- Werkt alleen onder de open lucht.\n- Hoe hoger, hoe sneller: "
                     "op zeeniveau elke 30 seconden een pluisje, vanaf hoogte 192 elke 6 seconden.\n- Een kist of buis eronder krijgt het "
                     "pluis meteen; anders: klik met een lege hand.",
                     "A dish that condenses little clouds into fluff.\n\n- Only works under the open sky.\n- The higher, the faster: at sea "
                     "level a bit of fluff every 30 seconds, from height 192 every 6 seconds.\n- A chest or pipe under it gets the fluff at "
                     "once; otherwise: click with an empty hand."),
}

FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [("koepel", "De Koepel", "item:guhs:sterrenkoepel", None)]
FTB_EN = {
    "koepel_oven": ("Bakken in bulk", "Bake a &6Mega-oven&r: a whole guh bakery of 3 by 2 by 2 on vadskracht. Click with an empty hand to choose "
                    "what it bakes.", "Baking in Bulk", "Build a &6Mega Oven&r: a whole guh bakery of 3 by 2 by 2 on vadskracht. Click with an "
                    "empty hand to choose what it bakes."),
    "koepel_station": ("Weer voor de hele basis", "Zet een &bKnabbel-weerstation&r naast je Weer vadser en voer hem knabbels: het weertje "
                       "geldt dan voor je hele basis.", "Weather for the Whole Base", "Put a &bNibble Weather Station&r next to your Weather "
                       "Vadser and feed it nibbles: then the weather covers your whole base."),
    "koepel_koepel": ("Een sterrenhemel van guhs", "Bouw de &dSterrenkoepel&r en voer hem knabbels tot het eerste guh-sterrenbeeld oplicht.",
                      "A Sky Full of Guhs", "Build the &dStar Dome&r and feed it nibbles until the first guh constellation lights up."),
    "koepel_molen": ("Kermis voor guhs", "Bouw een &dDraaimolen&r en koop een kaartje (een kaasknabbel): je guhs rijden mee en worden blij.",
                     "A Fair for Guhs", "Build a &dMerry-Go-Round&r and buy a ticket (a cheese nibble): your guhs ride along and get happy."),
    "koepel_vanger": ("Wolken vangen", "Zet een &bWolkenvanger&r op het dak: hoe hoger, hoe meer pluis.", "Catching Clouds",
                      "Put a &bCloud Catcher&r on the roof: the higher, the more fluff."),
}


# =====================================================================================================================
# textures
# =====================================================================================================================
def _vlak(kleur, rand=None, seed=0):
    img = Image.new("RGBA", (16, 16), kleur + (255,))
    d = ImageDraw.Draw(img)
    if rand:
        d.rectangle([0, 0, 15, 15], outline=rand + (255,))
    return img


def _donker(c, f=0.7):
    return tuple(int(v * f) for v in c)


def _licht(c, f=0.25):
    return tuple(int(v + (255 - v) * f) for v in c)


def textures(h):
    """Per machine: the face (three states, from vadskracht.py), a side, a top, and its own detail textures."""
    for n in BLOKKEN:
        basis, accent = KLEUR[n]
        for staat in vadskracht.STATEN:
            h.save(vadskracht._voorkant(n, basis, accent, staat), "block", f"{n}_voor_{staat}.png")
        h.save(vadskracht._zijkant(n, basis, accent), "block", f"{n}_zij.png")
        h.save(vadskracht._bovenkant(n, basis), "block", f"{n}_boven.png")
    # the oven door: a glowing window
    img = _vlak((150, 100, 80), (110, 70, 56))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 4, 12, 12], fill=(60, 36, 30, 255), outline=(200, 170, 140, 255))
    d.rectangle([4, 8, 11, 11], fill=(255, 150, 60, 255))
    d.point([(5, 9), (8, 10), (10, 9)], fill=(255, 230, 120, 255))
    h.save(img, "block", "mega_oven_deur.png")
    # the dome: deep blue with stars
    img = _vlak((44, 40, 92))
    d = ImageDraw.Draw(img)
    for (x, y) in ((2, 3), (7, 1), (12, 4), (4, 9), (10, 10), (14, 13), (6, 14), (1, 12)):
        d.point((x, y), fill=(255, 240, 170, 255))
    for (x, y) in ((9, 6), (3, 6), (13, 8)):
        d.point([(x, y), (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)], fill=(255, 250, 210, 255))
    h.save(img, "block", "sterrenkoepel_hemel.png")
    # the merry-go-round's roof: pink and white stripes
    img = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(img)
    for x in range(16):
        d.line([x, 0, x, 15], fill=((244, 150, 196) if (x // 2) % 2 == 0 else (255, 246, 250)) + (255,))
    h.save(img, "block", "draaimolen_dak.png")
    # a cloud
    img = _vlak((246, 248, 255), (214, 220, 236))
    ImageDraw.Draw(img).point([(4, 5), (11, 9), (7, 12)], fill=(255, 220, 240, 255))
    h.save(img, "block", "koepel_wolk.png")
    # a little guh head (draaimolen seats)
    img = _vlak((246, 178, 210), (214, 130, 170))
    d = ImageDraw.Draw(img)
    d.point([(5, 7), (10, 7)], fill=(40, 30, 40, 255))
    d.point([(4, 9), (11, 9)], fill=(255, 130, 170, 255))
    d.line([7, 10, 8, 10], fill=(120, 60, 90, 255))
    h.save(img, "block", "koepel_guhkopje.png")


# =====================================================================================================================
# models: one per face state, the whole machine drawn from the kern (north = front, the box goes back to +z)
# =====================================================================================================================
UV = [0, 0, 16, 16]


def el(frm, to, tex, voor=None):
    """An element with every face the texture tex (and its north face 'voor' when given); uv stretched over the face."""
    faces = {f: {"uv": UV, "texture": tex} for f in ("down", "up", "north", "south", "west", "east")}
    if voor:
        faces["north"] = {"uv": UV, "texture": voor}
    return {"from": frm, "to": to, "faces": faces}


def vorm(n):
    """The elements of machine n (textures: #voor, #zij, #boven and its own)."""
    if n == "mega_oven":
        return [el([0, 0, 0], [16, 24, 32], "#zij", "#voor"),
                el([-16, 0, 0], [0, 24, 32], "#zij", "#deur"), el([16, 0, 0], [32, 24, 32], "#zij", "#deur"),
                el([-14, 24, 2], [30, 28, 30], "#boven"), el([18, 28, 18], [24, 32, 24], "#zij")]
    if n == "knabbel_weerstation":
        return [el([2, 0, 2], [14, 20, 14], "#zij", "#voor"), el([6, 20, 6], [10, 28, 10], "#boven"),
                el([2, 28, 2], [14, 32, 14], "#wolk")]
    if n == "sterrenkoepel":
        return [el([-16, 0, 0], [0, 10, 32], "#zij"), el([0, 0, 0], [16, 10, 32], "#zij", "#voor"), el([16, 0, 0], [32, 10, 32], "#zij"),
                el([-12, 10, 4], [28, 20, 28], "#hemel"), el([-8, 20, 8], [24, 27, 24], "#hemel"),
                el([-2, 27, 12], [18, 32, 20], "#hemel")]
    if n == "draaimolen":
        return [el([-14, 0, 2], [30, 4, 30], "#boven"), el([3, 4, 0], [13, 14, 5], "#zij", "#voor"),
                el([6, 4, 14], [10, 27, 18], "#zij"), el([-16, 27, 0], [32, 31, 32], "#dak"),
                el([-11, 8, 7], [-5, 14, 13], "#guh"), el([21, 8, 7], [27, 14, 13], "#guh"),
                el([-11, 8, 19], [-5, 14, 25], "#guh"), el([21, 8, 19], [27, 14, 25], "#guh"),
                el([-8.5, 14, 9.5], [-7.5, 27, 10.5], "#zij"), el([23.5, 14, 9.5], [24.5, 27, 10.5], "#zij")]
    # wolkenvanger
    return [el([3, 0, 3], [13, 6, 13], "#zij", "#voor"), el([7, 6, 7], [9, 10, 9], "#boven"),
            el([1, 10, 1], [15, 12, 15], "#wolk"), el([0, 12, 0], [16, 13, 2], "#boven"), el([0, 12, 14], [16, 13, 16], "#boven"),
            el([0, 12, 2], [2, 13, 14], "#boven"), el([14, 12, 2], [16, 13, 14], "#boven")]


def textuurmap(n, staat):
    t = {"voor": f"guhs:block/{n}_voor_{staat}", "zij": f"guhs:block/{n}_zij", "boven": f"guhs:block/{n}_boven",
         "particle": f"guhs:block/{n}_zij", "deur": "guhs:block/mega_oven_deur", "hemel": "guhs:block/sterrenkoepel_hemel",
         "dak": "guhs:block/draaimolen_dak", "wolk": "guhs:block/koepel_wolk", "guh": "guhs:block/koepel_guhkopje"}
    return t


GUI = {  # item model display in the inventory: the big ones smaller
    "mega_oven": 0.28, "sterrenkoepel": 0.28, "draaimolen": 0.28, "knabbel_weerstation": 0.42, "wolkenvanger": 0.625}


def modellen(h):
    for n in BLOKKEN:
        for staat in vadskracht.STATEN:
            h.w(f"{h.A}/models/block/{n}_{staat}.json", {"parent": "minecraft:block/block", "textures": textuurmap(n, staat),
                                                          "elements": vorm(n)})
        h.w(f"{h.A}/blockstates/{n}.json", {"variants": {
            f"facing={f},snoet={staat}": {"model": f"guhs:block/{n}_{staat}", **({"y": r} if r else {})}
            for f, r in vadskracht.ROT.items() for staat in vadskracht.STATEN}})
        s = GUI[n]
        h.w(f"{h.A}/models/item/{n}.json", {"parent": f"guhs:block/{n}_werkt", "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [s, s, s]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [s * 0.6] * 3},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [s, s, s]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [s * 0.8] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [s * 0.8] * 3}}})
        h.self_drop(n)


def recepten(h):
    h.shaped("mega_oven", ["SKS", "OBO", "SSS"], {"S": "minecraft:smooth_stone", "K": "guhs:block_of_kaasknabbels",
                                                 "O": "guhs:guh_oven", "B": "minecraft:blast_furnace"}, "guhs:mega_oven")
    h.shaped("knabbel_weerstation", ["ILI", "IVI", "KKK"], {"I": "minecraft:iron_ingot", "L": "minecraft:lightning_rod",
                                                            "V": "guhs:weer_vadser", "K": "guhs:block_of_kaasknabbels"},
             "guhs:knabbel_weerstation")
    h.shaped("sterrenkoepel", ["GGG", "GSG", "KAK"], {"G": "minecraft:glass", "S": "guhs:sterrenplafond",
                                                      "A": "minecraft:amethyst_block", "K": "guhs:block_of_kaasknabbels"},
             "guhs:sterrenkoepel")
    h.shaped("draaimolen", ["PPP", "FNF", "KSK"], {"P": "minecraft:pink_wool", "F": "minecraft:oak_fence", "N": "minecraft:note_block",
                                                   "K": "guhs:kaas_knabbels", "S": "minecraft:smooth_stone_slab"}, "guhs:draaimolen")
    h.shaped("wolkenvanger", ["CCC", " I ", "IDI"], {"C": "guhs:wolkenblok_wit", "I": "minecraft:iron_ingot",
                                                     "D": "minecraft:daylight_detector"}, "guhs:wolkenvanger")


def build(h):
    textures(h)
    modellen(h)
    recepten(h)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in BLOKKEN])
    h.add_tag("guhs/tags/block/vadskracht", [f"guhs:{n}" for n in BLOKKEN])
    vadskracht.toon(h, *BLOKKEN)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def ftb(fq):
    item = fq.item
    plek = {"koepel_oven": (0, 0), "koepel_station": (2, 0), "koepel_koepel": (4, 0), "koepel_molen": (6, 0), "koepel_vanger": (8, 0)}
    blok = {"koepel_oven": "mega_oven", "koepel_station": "knabbel_weerstation", "koepel_koepel": "sterrenkoepel",
            "koepel_molen": "draaimolen", "koepel_vanger": "wolkenvanger"}
    for key, (titel, tekst, _, _) in FTB_EN.items():
        x, y = plek[key]
        fq.q(key, titel, tekst, f"guhs:{blok[key]}", [item(f"guhs:{blok[key]}")], rewards=(("guhs:kaas_knabbels", 32),),
             x=x, y=y, shape="gear" if key == "koepel_koepel" else None, xp=200)


def selfcheck(h):
    problems = []
    for n in BLOKKEN:
        for p in (f"{h.A}/blockstates/{n}.json", f"{h.A}/models/item/{n}.json", f"{h.D}/recipe/{n}.json", f"{h.D}/loot_table/blocks/{n}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%") != en.count("%"):
            problems.append(f"% count differs: {k}")
    if problems:
        raise SystemExit("koepel self-check failed:\n  " + "\n  ".join(problems))


def _blokuitleg():
    """Adds (or refreshes) the Koepel machines in tools/wiki_site/blokuitleg.py, in a marked block at the end."""
    p = os.path.join("tools", "wiki_site", "blokuitleg.py")
    s = open(p, encoding="utf-8").read()
    begin, eind = "# <koepel>", "# </koepel>"
    if begin in s:
        s = s[:s.index(begin)].rstrip("\n") + "\n" + s[s.index(eind) + len(eind):].lstrip("\n")
    blok = [begin]
    for n, (nl, en) in UITLEG.items():
        blok.append(f"UITLEG[{n!r}] = dict(\n    nl={nl!r},\n    en={en!r},\n    meer=[])")
    blok.append(eind)
    s = s.rstrip("\n") + "\n\n\n" + "\n".join(blok) + "\n"
    open(p, "w", encoding="utf-8", newline="\n").write(s)


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    os.makedirs(os.path.join("tools", "lang", "en"), exist_ok=True)
    with open(os.path.join("tools", "lang", "en", "c217_koepel.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    ftb_en = {}
    for key, (_, _, ten, den) in FTB_EN.items():
        ftb_en[f"ftb.{FTB_CHAPTER}.q.{key}.title"] = ten
        ftb_en[f"ftb.{FTB_CHAPTER}.q.{key}.desc"] = den
    ftb_en[f"ftb.{FTB_CHAPTER}.section.koepel"] = "The Dome"
    with open(os.path.join("tools", "lang", "en", "c217_koepel_ftb.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(ftb_en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in BLOKKEN:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    _blokuitleg()
    print(f"koepel: {len(BLOKKEN)} machines, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
