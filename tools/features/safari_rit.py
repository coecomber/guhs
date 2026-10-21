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


# round 3 (J2): the safaripoort, the glass-bottom boat, the truck quiz and Safari FM
TEKSTEN.update({
    "block.guhs.safaripoort": "Safaripoort",
    "item.guhs.safaripoort": "Safaripoort",
    "entity.guhs.safari_glasboot": "Glasbodemboot",
    "gui.guhs.safari.rit.instappen": "Instappen! Sluipen = uitstappen. Je eigen guhs rennen gezellig naast de truck mee.",
    "gui.guhs.safari.quiz.vraag": "[Gids Guh] Quizvraag! %s (klik op je antwoord)",
    "gui.guhs.safari.quiz.goed": "Goed zo, vahoeg! +%s Safarimunten voor de slimmerik.",
    "gui.guhs.safari.quiz.goed_vol": "Goed zo! (Je quizpot voor vandaag is al vol, njeg.)",
    "gui.guhs.safari.quiz.fout": "Njeg, helaas! Het goede antwoord was: %s",
    "gui.guhs.safari.quiz.te_laat": "Die vraag is al voorbij. Bij de volgende halte komt er een nieuwe!",
    "gui.guhs.safari.boot.instappen": "Instappen! Kijk door de glazen bodem naar beneden. Uitstappen kan alleen bij de steiger.",
    "gui.guhs.safari.boot.onderweg": "De glasbodemboot is onderweg. Wacht even bij de steiger!",
    "gui.guhs.safari.boot.blijf_zitten": "Blijf zitten, njeg! Anders word je kletsnat.",
    "gui.guhs.safari.boot.terug": "Terug bij de steiger! Hier kun je uitstappen. Vahoeg!",
    "gui.guhs.safari.boot.schipper": "[Schipper Guh] %s",
    "gui.guhs.safari.boot.delta.0": "Kijk naar beneden! Daar zwemmen de nijlpaarden. Ze kunnen wel 5 minuten onder water blijven.",
    "gui.guhs.safari.boot.delta.1": "Nijlpaarden zwemmen eigenlijk niet: ze huppelen over de bodem. Huppa, huppa!",
    "gui.guhs.safari.boot.delta.2": "Zie je die zeekoe? Die eet wel 50 kilo waterplantjes per dag. Vads!",
    "gui.guhs.safari.boot.ijskust.0": "Daar onder het glas: pinguïns! Onder water vliegen ze bijna, zo snel gaan ze.",
    "gui.guhs.safari.boot.ijskust.1": "De narwal voelt met zijn lange tand hoe koud en zout het water is. Slim, hè?",
    "gui.guhs.safari.boot.ijskust.2": "Pinguïns geven elkaar soms een mooi steentje als cadeautje. Njeg, wat lief!",
    "gui.guhs.safari.fm.aan": "♪ Safari FM ♪ De zender van het park! Lekker meewiebelen in de truck.",
    "gui.guhs.safari.fm.reclame": "♪ Safari FM reclame: %s",
    "gui.guhs.safari.fm.reclame.0": "Kaasknabbels van Opa Kaasguh: nu met extra gaatjes! De gaatjes zijn gratis.",
    "gui.guhs.safari.fm.reclame.1": "Zand in je vacht? Probeer Vadsshampoo! Nu met de geur van natte zebra.",
    "gui.guhs.safari.fm.reclame.2": "Gevonden: één zonnebril van een pinguïn. Hij ziet er nu wel heel cool uit.",
    "gui.guhs.safari.fm.reclame.3": "Kom naar de Struisvogelrace! Winnaars krijgen eeuwige roem, verliezers een klein beetje.",
    "gui.guhs.safari.fm.reclame.4": "Munten op? Beantwoord de quizvragen in de truck! De gids weet alles. Nou ja, bijna alles.",
    "gui.guhs.safari.fm.reclame.5": "Nieuw in de souvenirwinkel: een knuffelnijlpaard dat net zo hard snurkt als het echte!",
    "gui.guhs.safari.fm.reclame.6": "Het weerbericht: zonnig op de savanne, sneeuw bij de ijskust en vahoeg overal.",
    "gui.guhs.safari.fm.reclame.7": "Dit was Safari FM. Vergeet niet naar de giraf te zwaaien. Hij zwaait (bijna) terug!",
    "subtitles.guhs.safari.fm": "Safari FM speelt een deuntje"
})
QUIZ = {"entree": [["Hoe heet dit safaripark?", "Safaripark Vadse Bergen", "Njegpark Noord", "Kaasdierentuin Guhstad"], ["Wat mag je NIET doen bij de dieren?", "Over de hekken klimmen", "Foto's maken", "Zachtjes zwaaien"], ["Waarmee rijd je door het park?", "Met de safaritruck", "Met een vliegende kaas", "Met een skateboard"]], "savanne": [["Wat is het grootste landdier?", "De olifant", "De zebra", "De stokstaart"], ["Waarom heeft een zebra strepen?", "Om vliegen en roofdieren in de war te brengen", "Omdat hij in pyjama slaapt", "Zodat hij sneller lijkt"], ["Hoe lang is de nek van een giraf ongeveer?", "Bijna 2 meter", "10 centimeter", "8 meter"]], "delta": [["Waar zijn nijlpaarden overdag het liefst?", "In het water", "Boven in een boom", "Bij de kassa"], ["Wat eet een nijlpaard vooral?", "Gras", "Vis", "Pannenkoeken"], ["Hoe koelt een krokodil af?", "Hij ligt met zijn bek open", "Met een ijsje", "Hij zet een ventilator aan"]], "regenwoud": [["Wat betekent 'orang-oetan'?", "Mens van het bos", "Oranje banaan", "Lieve njeg"], ["Hoe beweegt een luiaard?", "Heel erg langzaam", "Op rolschaatsen", "Supersnel"], ["Waarom is een gifkikker zo fel gekleurd?", "Als waarschuwing: niet opeten!", "Voor een feestje", "Omdat hij net bij de kapper was"]], "bamboe": [["Wat eet een reuzenpanda bijna de hele dag?", "Bamboe", "Pizza", "Sneeuw"], ["Hoeveel uur per dag eet een panda ongeveer?", "Wel 12 uur", "1 uur", "Nooit, hij is altijd vol"], ["Welke kleur heeft een rode panda?", "Roodbruin", "Zwart-wit", "Groen met stippen"]], "ijskust": [["Kunnen pinguïns vliegen?", "Nee, maar ze zwemmen supergoed", "Ja, heel hoog", "Alleen op dinsdag"], ["Wat is de lange 'hoorn' van een narwal eigenlijk?", "Een tand", "Een neus", "Een ijspegel"], ["Waarom heeft een ijsbeer het niet koud?", "Een dikke vetlaag en een warme vacht", "Hij draagt een sjaal", "Hij zit altijd bij de kachel"]], "oertijd": [["Hoe bleef een mammoet warm?", "Met een dikke wollige vacht", "Met een elektrische deken", "Niet, hij rilde gewoon"], ["Waarvoor gebruikte een mammoet zijn slagtanden?", "Om sneeuw weg te schuiven en eten te zoeken", "Om mee te breien", "Als kapstok"], ["Welk ijstijddier had heel lange hoektanden?", "De sabeltandkat", "Het sabeltandkonijn", "De sabeltandpinguïn"]], "europa": [["Wat bouwt een bever?", "Een dam", "Een kasteel", "Een kaasfabriek"], ["Wanneer houdt een egel zijn winterslaap?", "In de winter", "In de zomer", "Alleen in het weekend"], ["Hoe houden otters elkaar vast als ze slapen?", "Ze houden pootjes vast", "Met plakband", "Ze bellen elkaar"]], "australie": [["Waar draagt een kangoeroe haar baby?", "In haar buidel", "In een rugzak", "Op haar hoofd"], ["Wat eet een koala?", "Eucalyptusblaadjes", "Hamburgers", "Kiezelsteentjes"], ["Hoe ver springt een kangoeroe in één sprong?", "Wel 8 meter", "Een halve meter", "Tot aan de maan"]]}
for _z, _vragen in QUIZ.items():
    for _i, (_v, _g, _f1, _f2) in enumerate(_vragen):
        for _k, _t in (("vraag", _v), ("goed", _g), ("fout1", _f1), ("fout2", _f2)):
            TEKSTEN[f"gui.guhs.safari.quiz.{_z}.{_i}.{_k}"] = _t


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


# the longer truck: bed stretch, width, and the seats (x, seat surface y, z centre; model pixels, front = -z). Seat 0 is the
# guide's (driving); then three rows of two. TruckEntity.ZIT has the same numbers.
BED_REK = 82 / 60
TRUCK_BREED = 1.2
TRUCK_STOELEN = [(-11.5, 15, -17.5), (-11.5, 16, 1.5), (11.5, 16, 1.5), (-11.5, 18, 21.5), (11.5, 18, 21.5),
                 (-11.5, 20, 41.5), (11.5, 20, 41.5)]


def _uv_copy(cube):
    return copy.deepcopy(cube["uv"])


def truck():
    """The phase-0 truck made longer (round 3, J2): the bed stretched from 60 to 82 pixels and a little wider, three roomy
    rows of two fixed seats behind the guide's seat (rising towards the back, like a little grandstand), no trailer (the
    players' own guhs walk along beside it). The faces keep their phase-0 paint (stretched along)."""
    with open(os.path.join(BRON, "safaritruck.geo.json"), encoding="utf-8") as f:
        g = json.load(f)
    geom = g["minecraft:geometry"][0]
    bones = {b["name"]: b for b in geom["bones"]}
    body = bones["truck"]["cubes"]
    bank, leuning = copy.deepcopy(body[19]), copy.deepcopy(body[20])

    def z_(z):
        """The bed (z -26..34) stretched to -26..56; the bonnet and cabin (z < -26) stay as they are."""
        return z if z <= -26 else -26 + (z + 26) * BED_REK

    def rek(c):
        x0, y0, z0 = c["origin"]
        sx, sy, sz = c["size"]
        z1 = z_(z0 + sz) if sz > 2 else z_(z0) + sz
        c["origin"] = [round(x0 * TRUCK_BREED, 3), y0, round(z_(z0), 3)]
        c["size"] = [round(sx * TRUCK_BREED, 3), sy, round(z1 - z_(z0), 3)]
        return c

    nieuw = [rek(c) for i, c in enumerate(body) if not 19 <= i <= 34]
    for (x, y, z) in TRUCK_STOELEN:
        b, l = copy.deepcopy(bank), copy.deepcopy(leuning)
        b["origin"], b["size"] = [x - 9, y - 5, z - 4.5], [18, 5, 9]
        l["origin"], l["size"] = [x - 9, y, z + 4.5], [18, 8, 1]
        nieuw += [b, l]
    bones["truck"]["cubes"] = nieuw
    for naam in ("wiel_lv", "wiel_rv", "wiel_la", "wiel_ra"):
        w = bones[naam]
        dz = 0 if naam.endswith("v") else z_(23) - 23 + 2
        w["pivot"] = [round(w["pivot"][0] * TRUCK_BREED, 3), w["pivot"][1], w["pivot"][2] + dz]
        for c in w["cubes"]:
            c["origin"] = [round(c["origin"][0] * TRUCK_BREED + (1 if c["origin"][0] > 0 else -1) * 0.6, 3), c["origin"][1], c["origin"][2] + dz]
    geom["description"]["visible_bounds_width"] = 7
    with open(os.path.join(BRON, "safaritruck.animation.json"), encoding="utf-8") as f:
        anim = json.load(f)
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


def _leeuwenschild(W, H, rng):
    """The round shield of the safaripoort: a lion head with guh eyes (big, glossy, a blush) in a shaggy mane, a pink rim."""
    bd = _bd()
    gezicht = bd.guh_face((236, 190, 98), eye_y=0.46, eye_dx=0.2, eye_r=0.13)(W, H, rng).astype(np.float32)
    a = np.zeros((H, W, 4), np.float32)
    cx, cy, R = W / 2, H / 2, W / 2
    for y in range(H):
        for x in range(W):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy) / R
            hoek = math.atan2(dy, dx)
            manen = 0.66 + 0.06 * math.sin(hoek * 11) + 0.03 * rng.random()
            if d > 1:
                continue
            if d > 0.92:
                a[y, x, :3] = (236, 112, 164)                       # pink rim
            elif d > manen:
                t = (d - manen) / max(0.01, 0.92 - manen)
                a[y, x, :3] = (176 - 50 * t, 98 - 30 * t, 40)       # the mane: dark ginger towards the rim
                a[y, x, :3] += rng.normal(0, 6)
            else:
                a[y, x, :3] = gezicht[y, x, :3]
            a[y, x, 3] = 255
    return np.clip(a, 0, 255).astype(np.uint8)


def safaripoort(bd):
    """The safaripoort (round 3, J2): a 4 x 6 block gate drawn by GeckoLib from its corner block. Two thick wooden doors
    (hinges at the outer edges, they swing open to one side), a crossbeam with pink rope loops, the round lion-guh shield
    with pink guh ears in the middle, little pink lamps hanging at both ends. The model is centred (x -32..32) and lies
    along x; the renderer moves it to the middle of the four blocks and turns it for a gate along z."""
    bd.ATLAS, bd.S = 512, 2
    A = bd.Atlas(41)
    hout = bd.plain((132, 88, 52), 10)
    hout_donker = bd.plain((92, 60, 36), 8)
    ijzer = bd.plain((58, 58, 64), 5)
    roze = bd.plain((240, 130, 178), 8)
    lamp = bd.plain((255, 186, 214), 5)
    weg = bd.transparent

    def planken(W, H, rng):
        """Vertical planks with dark seams and a few knots."""
        a = hout(W, H, rng).astype(np.float32)
        breed = 6 * bd.S
        for x in range(0, W, breed):
            a[:, x:x + max(1, bd.S // 2) + 0, :3] *= 0.62
            a[:, x:x + breed, :3] *= 0.94 + 0.12 * rng.random()
        for _ in range(3):
            bd.disc(a, rng.uniform(4, W - 4), rng.uniform(4, H - 4), 1.2 * bd.S, (96, 62, 36), 0.8)
        return np.clip(a, 0, 255).astype(np.uint8)

    deur_l, deur_r = [], []
    for kant, lijst in ((-1, deur_l), (1, deur_r)):
        x0 = -31 if kant < 0 else 1
        lijst.append(bd.cube(A, [x0, 1, -2], [30, 70, 4], hout, {"north": planken, "south": planken}))   # the thick door
        lijst.append(bd.cube(A, [x0, 1, -2.6], [30, 4, 5.2], hout_donker))                   # frame: bottom, top
        lijst.append(bd.cube(A, [x0, 67, -2.6], [30, 4, 5.2], hout_donker))
        lijst.append(bd.cube(A, [x0 + (0 if kant < 0 else 26), 5, -2.6], [4, 62, 5.2], hout_donker))   # hinge stile
        lijst.append(bd.cube(A, [x0 + 3, 34, -2.4], [24, 3, 4.8], hout_donker))              # cross brace
        for hy in (10, 58):                                                                  # iron hinges
            lijst.append(bd.cube(A, [x0 + (0 if kant < 0 else 22), hy, -2.8], [8, 3, 5.6], ijzer))
    balk = [bd.cube(A, [-32, 80, -3], [64, 10, 6], hout_donker, {"up": hout}),
            bd.cube(A, [-32, 78.5, -3.6], [64, 1.5, 7.2], roze)]                            # the rope along the beam
    for x in range(-28, 29, 8):                                                              # rope loops hanging down
        for z in (-3.8, 3.2):
            balk.append(bd.cube(A, [x - 2, 75.5, z], [4, 1, 0.6], roze))
            balk.append(bd.cube(A, [x - 3, 76.5, z], [1, 2, 0.6], roze))
            balk.append(bd.cube(A, [x + 2, 76.5, z], [1, 2, 0.6], roze))
    for x in (-30, 28):                                                                      # little pink lamps
        balk.append(bd.cube(A, [x + 0.5, 76, -0.5], [1, 4, 1], ijzer))
        balk.append(bd.cube(A, [x - 0.5, 71, -1.5], [3, 5, 3], lamp, inflate=0.1))
    schild = [bd.cube(A, [-11, 72, -4.5], [22, 22, 9], weg, {"north": _leeuwenschild, "south": _leeuwenschild})]
    roze_oor = bd.round_ear((248, 160, 196), (232, 98, 150))
    for sx in (-1, 1):
        schild.append(bd.cube(A, [sx * 8 - 3.5, 92, -0.6], [7, 7, 1.2], weg, {"north": roze_oor, "south": roze_oor}))
    bones = [bd.bone("root", None, [0, 0, 0], []),
             bd.bone("deur_l", "root", [-31, 0, 0], deur_l), bd.bone("deur_r", "root", [31, 0, 0], deur_r),
             bd.bone("balk", "root", [0, 80, 0], balk), bd.bone("schild", "balk", [0, 83, 0], schild)]
    g = bd.geo("safaripoort", bones, 6, 7)
    def zwaai(van, naar):
        return {"loop": "hold_on_last_frame", "animation_length": 1.0, "bones": {
            "deur_l": {"rotation": {"0.0": [0, van, 0], "1.0": {"post": [0, naar, 0], "lerp_mode": "catmullrom"}}},
            "deur_r": {"rotation": {"0.0": [0, -van, 0], "1.0": {"post": [0, -naar, 0], "lerp_mode": "catmullrom"}}}}}
    anim = {"format_version": "1.8.0", "animations": {"animation.safaripoort.open": zwaai(0, 92),
                                                      "animation.safaripoort.dicht": zwaai(92, 0)}}
    return g, Image.fromarray(A.img), anim


def safaripoort_icoon():
    """The item icon: the gate in miniature (two doors, the pink beam, the lion shield)."""
    a = np.zeros((16, 16, 4), np.uint8)
    a[2:16, 1:3] = (92, 60, 36, 255)
    a[2:16, 13:15] = (92, 60, 36, 255)
    a[5:16, 3:8] = (132, 88, 52, 255)
    a[5:16, 8:13] = (140, 94, 56, 255)
    a[5:16, 7:9] = (92, 60, 36, 255)
    a[2:4, 1:15] = (92, 60, 36, 255)
    a[4, 1:15] = (240, 130, 178, 255)
    for x, y in ((6, 1), (9, 1)):
        a[y, x:x + 2] = (248, 160, 196, 255)
    a[1:5, 6:10] = (236, 190, 98, 255)
    a[2, 6] = a[2, 9] = (176, 98, 40, 255)
    a[2, 7] = a[2, 8] = (22, 16, 34, 255)
    a[0, 1] = a[0, 14] = (255, 186, 214, 255)
    a[1, 1] = a[1, 14] = (255, 186, 214, 255)
    return Image.fromarray(a, "RGBA")


def glasboot(bd):
    """The glass-bottom boat (round 3, J2): a wide, calm white boat with a sky-blue rim, a striped sun canopy on four posts,
    benches along both sides and in the middle a big glass floor (see-through panes in a light frame: you look down at
    the hippos, manatees, penguins and the narwhal). The hull sits a little in the water (y below 0)."""
    bd.ATLAS, bd.S = 512, 1
    A = bd.Atlas(43)
    wit = bd.plain((240, 238, 230), 5)
    blauw = bd.plain((92, 170, 220), 6)
    hout = bd.plain((178, 128, 82), 8)
    paal = bd.plain((210, 210, 214), 4)
    roze = bd.plain((240, 130, 178), 6)

    def glas(W, H, rng):
        """See-through panes in a light frame, a few glints."""
        a = np.zeros((H, W, 4), np.uint8)
        stap = 7 * bd.S
        for x in range(0, W, stap):
            a[:, x:x + bd.S] = (200, 230, 244, 255)
        for y in range(0, H, stap):
            a[y:y + bd.S, :] = (200, 230, 244, 255)
        a[:bd.S, :] = a[-bd.S:, :] = (150, 196, 226, 255)
        a[:, :bd.S] = a[:, -bd.S:] = (150, 196, 226, 255)
        for _ in range(W * H // 900 + 2):
            gx, gy = int(rng.integers(2, max(3, W - 4))), int(rng.integers(2, max(3, H - 4)))
            a[gy, gx:gx + 3] = (236, 248, 255, 255)
        return a

    def streep(W, H, rng):
        a = wit(W, H, rng)
        for x in range(0, W, 8 * bd.S):
            a[:, x:x + 4 * bd.S, :3] = (240, 130, 178)
        return a

    c = [
        bd.cube(A, [-22, -6, -44], [44, 4, 16], wit), bd.cube(A, [-22, -6, 28], [44, 4, 16], wit),      # bottom fore and aft
        bd.cube(A, [-22, -6, -28], [8, 4, 56], wit), bd.cube(A, [14, -6, -28], [8, 4, 56], wit),        # bottom along the glass
        bd.cube(A, [-14, -5, -28], [28, 1, 56], bd.transparent, {"up": glas, "down": glas}),            # the glass floor
        bd.cube(A, [-24, -6, -44], [2, 12, 88], wit, {"up": blauw}), bd.cube(A, [22, -6, -44], [2, 12, 88], wit, {"up": blauw}),
        bd.cube(A, [-22, -6, -46], [44, 12, 2], wit, {"up": blauw}), bd.cube(A, [-22, -6, 44], [44, 12, 2], wit, {"up": blauw}),
        bd.cube(A, [-24.2, 2, -46.2], [48.4, 2, 92.4], blauw, {"up": bd.transparent, "down": bd.transparent}, inflate=0.05),
        bd.cube(A, [-12, -6, -54], [24, 10, 8], wit, {"up": blauw}),                                    # the bow
    ]
    for x in (-21, 13):                                                                                  # benches
        c.append(bd.cube(A, [x, 3, -36], [8, 2, 72], hout))
    for x, z in ((-22, -40), (20, -40), (-22, 38), (20, 38)):
        c.append(bd.cube(A, [x, 6, z], [2, 30, 2], paal))
    dak = [bd.cube(A, [-25, 36, -44], [50, 2, 86], streep, {"down": wit}),
           bd.cube(A, [-25, 34, -44], [50, 2, 1], roze), bd.cube(A, [-25, 34, 41], [50, 2, 1], roze)]
    bones = [bd.bone("root", None, [0, 0, 0], []), bd.bone("boot", "root", [0, 0, 0], c), bd.bone("dak", "boot", [0, 36, 0], dak)]
    g = bd.geo("glasboot", bones, 7, 3)
    anim = {"format_version": "1.8.0", "animations": {"animation.glasboot.dobber": {"loop": True, "animation_length": 4.0, "bones": {
        "boot": {"rotation": {"0.0": [0.8, 0, -0.8], "2.0": [-0.8, 0, 0.8], "4.0": [0.8, 0, -0.8]},
                 "position": {"0.0": [0, 0, 0], "1.0": [0, 0.3, 0], "2.0": [0, 0, 0], "3.0": [0, -0.3, 0], "4.0": [0, 0, 0]}}}}}}
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
    for naam, (g, tex, anim) in (("safaritruck", truck()), ("kano", kano(bd)), ("gondel", gondel(bd)), ("glasboot", glasboot(bd))):
        uit[f"{A}/geckolib/models/entity/safari_rit/{naam}.geo.json"] = g
        uit[f"{A}/textures/entity/safari_rit/{naam}.png"] = tex
        uit[f"{A}/geckolib/animations/entity/safari_rit/{naam}.animation.json"] = anim
    g, tex, anim = safaripoort(bd)
    uit[f"{A}/geckolib/models/block/safaripoort.geo.json"] = g
    uit[f"{A}/textures/block/safaripoort.png"] = tex
    uit[f"{A}/geckolib/animations/block/safaripoort.animation.json"] = anim
    uit[f"{A}/blockstates/safaripoort.json"] = {"variants": {"": {"model": "guhs:block/safaripoort"}}}
    uit[f"{A}/models/block/safaripoort.json"] = {"textures": {"particle": "minecraft:block/stripped_dark_oak_log"}}
    uit[f"{A}/textures/item/safaripoort.png"] = safaripoort_icoon()
    uit[f"{A}/items/safaripoort.json"] = {"model": {"type": "minecraft:model", "model": "guhs:item/safaripoort"}}
    uit[f"{A}/models/item/safaripoort.json"] = {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/safaripoort"}}
    # (the block drops its item itself, once for the whole gate: SafariPoortBlock.playerWillDestroy)
    uit[f"{D}/loot_table/blocks/safaripoort.json"] = {"type": "minecraft:block", "pools": []}
    uit[f"{A}/blockstates/safari_schemabord.json"] = {"variants": {
        f"facing={f}": {"model": "guhs:block/safari_schemabord", **({"y": r} if r else {})}
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}
    uit[f"{A}/models/block/safari_schemabord.json"] = schemabord_model()
    uit[f"{A}/textures/block/safari_schemabord.png"] = schemabord_textuur()
    uit[f"{A}/items/safari_schemabord.json"] = {"model": {"type": "minecraft:model", "model": "guhs:block/safari_schemabord"}}
    uit[f"{D}/loot_table/blocks/safari_schemabord.json"] = {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": "guhs:safari_schemabord"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
    return uit


# Safari FM's tunes (tools/features/safari_fm_geluid.py), for sounds.json
GELUIDEN = {f"safari.fm.{i}": {"sounds": [{"name": f"guhs:safari/fm_{i}", "stream": True}], "subtitle": "subtitles.guhs.safari.fm"}
            for i in (1, 2, 3)}


def build(h):
    h.patch_json(f"{h.A}/sounds.json", lambda d: d.update(GELUIDEN))
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
    sp = os.path.join(root, "assets", "guhs", "sounds.json")
    with open(sp, encoding="utf-8") as f:
        geluid = json.load(f)
    geluid.update(GELUIDEN)
    with open(sp, "w", encoding="utf-8", newline="\n") as f:
        json.dump(geluid, f, indent=2, ensure_ascii=False)
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
