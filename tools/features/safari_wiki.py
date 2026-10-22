"""
1.6.0 Op Safari! (F13): what the wiki says about Safaripark Vadse Bergen, its animals, the lobby, getting around, the camera,
the quests, the enclosure signs and Het Kriebelhuis. Not a feature module (not in FEATURES): the docs step
(tools/wiki_bbq2) reads WIKI and turns it into pages; the English is in tools/wiki_bbq2/en_safari.py.
The species lists are read from the game data (data/guhs/safari/soorten.json and the Dutch lang), so they follow the park.
"""
import json
import os

_R = os.path.join("src", "main", "resources")


def _lees(*pad):
    try:
        with open(os.path.join(_R, *pad), encoding="utf-8") as f:
            return json.load(f)
    except OSError:
        return {}


_SOORTEN = _lees("data", "guhs", "safari", "soorten.json")
_NL = _lees("assets", "guhs", "lang", "nl_nl.json")
ZONES = [("savanne", "Savanne"), ("delta", "Rivierdelta"), ("regenwoud", "Regenwoud"), ("bamboe", "Bamboebergen"),
         ("ijskust", "IJskust"), ("oertijd", "Oerijstijd-vallei"), ("europa", "Europees Woud"), ("australie", "Australië")]
AFDELINGEN = [("terrarium", "Terrarium"), ("kriebelzaal", "Kriebelzaal"), ("nachtzaal", "Nachtzaal"), ("aquarium", "Diepzeezaal")]


def naam(soort):
    return _NL.get(f"entity.guhs.safari.{soort}", soort.replace("_", " ").capitalize())


def soorten(zone, sectie=None):
    return [naam(s) for s, d in _SOORTEN.items() if d.get("zone") == zone and (sectie is None or d.get("sectie") == sectie)]


def lijst(namen):
    namen = [n[0].lower() + n[1:] if not n[:2].isupper() else n for n in namen]
    return ", ".join(namen[:-1]) + " en " + namen[-1] if len(namen) > 1 else "".join(namen)


def _per_zone():
    return "\n\n".join(f"{titel}: {lijst(soorten(z))}." for z, titel in ZONES if soorten(z))


def _per_afdeling():
    return "\n\n".join(f"{titel}: {lijst(soorten('kriebelhuis', a))}." for a, titel in AFDELINGEN if soorten("kriebelhuis", a))


N_PARK = sum(1 for d in _SOORTEN.values() if d.get("zone") in dict(ZONES))
N_KRIEBEL = sum(1 for d in _SOORTEN.values() if d.get("zone") == "kriebelhuis")

WIKI = {
    "verhalen": {},
    "systemen": {
        "safaripark": (
            "Safaripark Vadse Bergen", "safaripark_savanne",
            "Een groot, vast safaripark in een eigen dimensie, vol schattige chibi-dieren in guh-stijl. Je spot en fotografeert ze, "
            "rijdt mee met de safaritruck, verdient Safarimunten en neemt een dierenfamilie mee naar je eigen base.",
            ["safari-lobby", "safari-dieren", "safari-vervoer", "safaricamera", "safari-quests", "verblijfsborden", "kriebelhuis",
             "dimensies/safaripark"]),
        "safari-lobby": (
            "Het entreedorp", "safari_lobby_plein",
            "Het dorp bij de ingang van het safaripark: de giftshop, het rangerstation, de lodge, het IJskraam, de speeltuin, het "
            "kampvuur, de aaihoek, het expertenpaviljoen en de halte van de safaribus.",
            ["safaripark", "safari-quests", "verblijfsborden", "kriebelhuis"]),
        "safari-dieren": (
            "De safaridieren", "safaricamera",
            "Leeuwen, olifanten, pinguïns, bevers, rode panda's, een narwal en nog veel meer: alle dieren van het safaripark, per zone. "
            "Ze zijn passief en onsterfelijk, en blijven altijd in hun eigen gebied.",
            ["safaripark", "safaricamera", "verblijfsborden", "kriebelhuis"]),
        "safari-vervoer": (
            "Rondgaan in het safaripark", "block:guhs:safaripoort",
            "Met de safaribus kom je er, en in het park rijd je mee met de safaritruck, peddel je de rivier af met een kano, zweef je met "
            "de kabelbaan over de IJskust en vaar je met de glasbodemboot langs de steigers.",
            ["safaripark", "safari-lobby"]),
        "safaricamera": (
            "De safaricamera en de Safari-dex", "safaricamera",
            "Fotografeer de dieren met de safaricamera. Elke soort die je fotografeert komt in de Safari-tab van je Guhdex, met je "
            "beste foto en tot drie sterren. Foto's leveren Safarimunten op.",
            ["safaripark", "safari-dieren", "safari-quests", "guhdex"]),
        "safari-quests": (
            "Quests, geheimpjes en badges", "safari_badge_savanne",
            "Twee korte zijquests, een geheimpje in elke zone, een zone-expert per zone met een badge, drie titels en een beloning "
            "voor de volle dex.",
            ["safaripark", "safaricamera", "safari-lobby", "ftb-quests"]),
        "verblijfsborden": (
            "Verblijfsborden: dieren in je eigen base", "block:guhs:safari_verblijfsbord",
            "Koop in de giftshop een verblijfsbord, zet hem neer en er komt een dierenfamilie bij je wonen: een mannetje, een vrouwtje "
            "en twee kleintjes. Aaien, voeren, speeltjes en cadeautjes; en sommige dieren bouwen zelfs voor je.",
            ["safari-dieren", "safari-lobby", "helperdieren", "safari_terrarium"]),
        "kriebelhuis": (
            "Het Kriebelhuis", "safari_kriebelhuis",
            "Een groot gebouw in het entreedorp vol kleine diertjes: een terrarium onder een glazen koepel, de Kriebelzaal, een donkere "
            "Nachtzaal, een Diepzeezaal en een roze glazen tunnel onder de vijver.",
            ["safaripark", "safari-lobby", "safari-dieren", "verblijfsborden"]),
    },
    "npc_home": {
        "safari_hoofdranger": "safari-lobby", "safari_fotograaf": "safari-lobby", "safari_verzorger": "safari-lobby",
        "safari_verkoper": "safari-lobby", "safari_lodgebeheerder": "safari-lobby", "safari_ijsverkoper": "safari-lobby",
        "safari_verteller": "safari-lobby", "safari_kriebelhuis_balie": "kriebelhuis", "safari_kriebelhuis_winkel": "kriebelhuis",
        "safari_expert_savanne": "safari-quests", "safari_expert_delta": "safari-quests", "safari_expert_regenwoud": "safari-quests",
        "safari_expert_bamboe": "safari-quests", "safari_expert_ijskust": "safari-quests", "safari_expert_oertijd": "safari-quests",
        "safari_expert_europa": "safari-quests", "safari_expert_australie": "safari-quests", "safari_expert_kriebelhuis": "kriebelhuis",
    },
    "entity_home": {
        "safari_dier": "safari-dieren", "safari_thuisdier": "verblijfsborden", "safari_truck": "safari-vervoer",
        "safari_kano": "safari-vervoer", "safari_gondel": "safari-vervoer", "safari_glasboot": "safari-vervoer",
        "safari_race_struisvogel": "safari-vervoer",
    },
    "tekst": [
        # --- the park ---
        ("safaripark", "Erheen",
         "Het park ligt in een eigen dimensie. Je komt er met de Guh-safaribus, die vertrekt vanaf een busstation in de Guhmensie: "
         "een rieten lodge met guh-oortjes op de nok en een poort van twee olifanten. Klik op de bus en de chauffeur-guh rijdt je "
         "erheen. Met de bus kom je ook weer terug, bij het station waar je instapte. Je eigen guhs die je volgen gaan gewoon mee en "
         "krijgen in het park een safarihoedje op."),
        ("safaripark", "De zones",
         "Het park is zo'n 512 bij 512 blokken, met één rivier die door alle zones slingert: de Savanne (de grootste, met de Trotse "
         "Rots), de Rivierdelta, het Regenwoud, de Bamboebergen, de IJskust, de Oerijstijd-vallei, het Europees Woud en Australië. "
         "Elke zone heeft een uitkijktoren, een kijkhut, een truckhalte en een geheimpje. Tussen de zones rijdt de truck door grote "
         "safaripoorten."),
        ("safaripark", "Veilig",
         "In het park val je niet te pletter, krijg je geen honger en raak je niet gewond. Slopen en bouwen kan er niet; deuren, "
         "hekjes, knoppen en de guhs werken wel. De dieren ontsnappen nooit."),
        ("safaripark", "De dag in het park",
         "Op vaste momenten van de dag zijn er voedertijden (dan komen ook de verstoppers tevoorschijn), een rangerpraatje bij een "
         "truckhalte (blijf luisteren voor een paar munten) en soms 's nachts een nachtsafari met lampiontrucks: wakkere dieren "
         "hebben dan gloeiende oogjes. Elke echte dag is er een andere soort jarig, met slingers en een taartje: foto's van die soort "
         "geven dubbele munten. Het schemabord bij het rangerstation zegt wat er wanneer is."),
        # --- the lobby ---
        ("safari-lobby", "Wie woont er?",
         "Hoofdranger Guhmbo heet je welkom, geeft je bij je eerste bezoek een safaricamera en elke dag een tip. Fotograaf Flitsguh "
         "geeft fototips, en een nieuwe camera als je er geen hebt. Dierenverzorger Knuffelien zorgt voor de aaihoek. Giftshop-guh Kassie verkoopt van alles voor "
         "Safarimunten. Lodgebeheerder Snurkguh geeft je een hutje. IJsverkoper Smeltje verkoopt dierenijsjes, en 's avonds vertelt "
         "Verteller Opa Knor verhalen bij het kampvuur. In het expertenpaviljoen zitten negen zone-experts."),
        ("safari-lobby", "De lodge",
         "In de lodge staan hutjes op palen in de vijver. Slaap je daar, dan word je ook daar wakker. Wie in de lodge wakker wordt, "
         "krijgt dubbele munten voor de eerste foto van de dag."),
        ("safari-lobby", "De aaihoek",
         "In de aaihoek wonen een geitje, een alpaca, een cavia en een ezeltje, elk met een jong. Je mag ze voeren, borstelen en "
         "aaien, en de alpaca aan een touwtje uitlaten. Elk dier heeft elke dag een andere lievelingskost; Knuffelien geeft een hint. "
         "Elk klusje geeft munten, het goede voer een extra munt."),
        ("safari-lobby", "Het IJskraam, de speeltuin en het kampvuur",
         "Bij het IJskraam koop je een pandaijsje (sneller lopen), een zebrastreep (hoger springen), een pinguïnijsje (beter "
         "zwemmen) of een flamingosorbet (zien in het donker). In de speeltuin spelen bezoekers-guhs, en bij het kampvuur rooster je "
         "marshmallows. Elke dag is er ook een pinguïnparade door het dorp."),
        ("safari-lobby", "De giftshop",
         "Voor Safarimunten koop je verblijfsborden (100, 175 of 300 munten), safarivoer, safarikleding voor jou en je guhs, "
         "safari-bouwblokken, speeltjes voor je dieren thuis, posters en schilderijen, en de parkgebouwen als blauwdruk. Het "
         "safarihek gaat open als er 10 soorten in je dex staan, de rotswanden bij 20."),
        # --- the animals ---
        ("safari-dieren", "Per zone", _per_zone()),
        ("safari-dieren", "Spotten",
         "Sommige dieren zijn verstoppers: ze duiken een hol, struik of het water in als je te snel of te dichtbij komt. Sluipen helpt. "
         "De okapi, de sabeltandtijger, de bever, de vuursalamander, de das en de quokka wonen op één lastig plekje; pootafdrukken "
         "leiden ernaartoe en lichten op door een verrekijker. De narwal is het zeldzaamste dier: hij laat zich maar heel soms zien."),
        ("safari-dieren", "Wat ze doen",
         "De dieren lopen rond, eten, spelen, slapen 's nachts (oogjes dicht en zzz), en hebben elk hun eigen kunstjes: pinguïns "
         "glijden op hun buik van de glijbaan, olifanten spuiten zich nat, bevers slepen takjes naar hun dam en dassen gaan door hun "
         "tunnel heen en weer. Vanuit elke kijkhut zie je ze regelmatig langskomen. Op elk uitzichtpunt zit een kijk-guh met een "
         "verrekijker."),
        ("safari-dieren", "De aaihoek en de Gouden dikdik",
         "De vier aaihoekdieren wonen in het entreedorp. De Gouden dikdik is geheim: die krijg je pas met een volle dex."),
        # --- getting around ---
        ("safari-vervoer", "De safaritruck",
         "Een open truck rijdt een vaste route door alle zones, met haltes en grote safaripoorten tussen de zones. Stap in waar je "
         "wilt en stap uit bij elke halte: ongeveer elke minuut komt er weer een langs. Een gids-guh vertelt bij elke halte een "
         "weetje, en je mag fotograferen tijdens het rijden."),
        ("safari-vervoer", "Kano, kabelbaan en boot",
         "Bij het dorp huur je een kanootje en peddel je de rivier af, langs nijlpaarden, otters en krokodillen. De kabelbaan zweeft "
         "in een rondje over de IJskust en de Bamboebergen. De glasbodemboot vaart vanaf de hoofdsteiger bij het dorp langs steigers "
         "in de Savanne, de Delta, het Regenwoud en Europa: je kunt bij elke steiger op- en afstappen."),
        ("safari-vervoer", "De struisvogelrace",
         "Op de racebaan race je op een struisvogel. De beste tijden staan op het scorebord."),
        # --- the camera ---
        ("safaricamera", "Een foto maken",
         "Houd rechtsklik ingedrukt om in te zoomen en laat los om af te drukken. Hoe groter het dier in beeld, hoe beter, en een "
         "dier dat iets bijzonders doet (slapen, eten, spelen, buikglijden) is extra leuk. Een foto krijgt één tot drie sterren, en "
         "het kaartje zegt waarom."),
        ("safaricamera", "De Safari-dex",
         "Elke soort die je gefotografeerd hebt komt in de Safari-tab van de Guhdex, met je beste foto. Daar kun je een foto laten "
         "afdrukken als fotokaartje, of als schilderijtje van 1 bij 1 (twee vellen papier). Foto's van je eigen dieren thuis tellen "
         "ook mee."),
        ("safaricamera", "Safarimunten",
         "De eerste foto van een soort geeft 10, 20 of 40 munten (een betere foto later geeft het verschil). Elke dag zijn er drie "
         "fotomissies van 40 munten. Munten krijg je ook voor geheimpjes, complete zones, de zijquests en de aaihoek. 's Nachts "
         "gemaakte foto's van wakkere dieren met gloeiende oogjes krijgen een extra punt."),
        ("safaricamera", "De gouden safaricamera",
         "Met een volle dex krijg je de gouden safaricamera: elke foto ermee krijgt een gouden lijstje."),
        # --- quests ---
        ("safari-quests", "De vermomde guh",
         "Een guh is in een zelfgemaakt zebrapakje het park in geglipt (\"njeg, ik ben echt een zebra\"). Vind hem drie keer, steeds "
         "in een ander dierenpakje. Daarna gaat hij met je mee naar huis, en zijn zebrapakje wordt kleding voor al je guhs."),
        ("safari-quests", "De te vahe olifant",
         "Je kunt niet te vads zijn... maar deze olifant is wel te vah: hij past niet meer door zijn poortje. Help hem met een "
         "grappig dieetje en de olifantenloopband. Je krijgt de loopband als blok en een fotolijstje van de slanke olifant."),
        ("safari-quests", "Geheimpjes",
         "In elke zone ligt een geheimpje verstopt, zoals de verloren zonnebril van een krokodil of een oude theepot achter de "
         "pagode. Elk geheimpje geeft 25 munten; vind je ze allemaal, dan krijg je een trofee en de titel Speurneus. In Australië "
         "zoekt de postguh nog een verloren ansichtkaart."),
        ("safari-quests", "Zone-experts en badges",
         "In het expertenpaviljoen zitten negen experts, een voor elke zone en een voor het Kriebelhuis (Professor Kriebelguh). "
         "Staan alle dieren van een zone in je dex, dan licht zijn balie op: je krijgt de zonebadge, een zoneschilderij en 100 munten."),
        ("safari-quests", "Titels en de volle dex",
         "Speurneus krijg je voor alle geheimpjes, Beverbaas als je bevers vijf beverdammen hebben gebouwd (zie Helperdieren) en "
         "Safarikoning met een volle dex. Bij de volle dex horen ook de gouden safaricamera, een gouden dierenbeeldje en het "
         "verblijfsbord van de Gouden dikdik."),
        ("safari-quests", "Bekende bewoners",
         "Zestien dieren in het park hebben een eigen naam, zoals Flappie de olifant en Dikkie. Je ziet hun naam boven hun kopje, ze "
         "staan in je Guhdex, en bij hun verblijf staat een bordje met een weetje."),
        # --- enclosure signs ---
        ("verblijfsborden", "Kopen",
         "Een verblijfsbord koop je in de giftshop zodra de soort in je dex staat: 100, 175 of 300 Safarimunten, afhankelijk van het "
         "dier. Van elke soort kun je eens per zeven dagen een bord kopen. Kleine diertjes uit het Kriebelhuis krijgen een "
         "terrariumbord: dan verschijnt er een glazen bakje waar ze in wonen."),
        ("verblijfsborden", "De familie",
         "Op elk bord komt precies één mannetje, één vrouwtje en twee kleintjes wonen. Ze blijven binnen hun gebied (8 tot 32 blokken, "
         "rond of rechthoekig), ook zonder hek. Sla je het bord kapot, dan gaan ze met een puf terug naar het park en krijg je het "
         "bord terug: zo kun je ze verhuizen."),
        ("verblijfsborden", "Thuis met je dieren",
         "Aai ze, voer ze met eten dat bij ze past (safarivoer telt dubbel) en geef ze een speeltje: een speelbal, krabboom, "
         "schommelband of glijbaantje. Blije dieren geven af en toe een cadeautje. Vrienden mogen je dieren ook aaien en voeren."),
        ("verblijfsborden", "Het menu",
         "Klik op het bord voor het menu: Familie (namen geven, terugroepen), Verblijf (grootte, vorm, slaapplek), Tevredenheid (wat "
         "het dier nog mist, en de knop Bouw passend verblijf), Cadeautjes, Info (een weetje, je beste foto, wanneer je weer een bord "
         "mag kopen) en bij helperdieren de tab Bouwopdracht."),
        # --- the Kriebelhuis ---
        ("kriebelhuis", "Het gebouw",
         "Bij de balie zit Kaartjesguh Piepje (de kaartjes zijn gratis). Daarna loop je door het Terrarium onder een glazen koepel, "
         "de Kriebelzaal met een slingerpad, kooitjes, een mierenboerderij en een vlindertuin, de stille Nachtzaal onder een "
         "sterrenplafond en de Diepzeezaal. Een roze glazen tunnel loopt onder de vijver door naar een kijkgrot in het kelpbos. Bij de "
         "uitgang zit Winkelguh Spinneke met een winkeltje, en er is een aai-lagune."),
        ("kriebelhuis", "De diertjes", _per_afdeling()),
        ("kriebelhuis", "Kijken mag",
         "Ook guhs-diertjes zoals koi, kikkerguhs, vlindertjes en lieveheersbeestjes wonen er. Die mag je bekijken, maar niet "
         "meenemen. Bij de verblijven staan infoborden: klik erop voor een weetje over het dier."),
    ],
}
