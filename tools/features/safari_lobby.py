"""
1.6.0 Op Safari! step 2, slice S2: the lobby of Safaripark Vadse Bergen (Java: feature/safari/lobby; English:
tools/lang/en/c161_safari_lobby.json).

Here: the Dutch texts of the lobby's characters, giftshop, aaihoek, lodge and welcome; the skins of the 13 lobby guhs
(the sitting guh recoloured: khaki for the rangers and staff, the zone colour for the eight experts); the items komkommer,
paprika and the eight zone badges (textures + models); and data/guhs/safari/winkel.json, the giftshop's item list (an entry
whose item does not exist is simply not shown, so S6 only has to make the items it promises here).
The spots of the characters (data/guhs/safari/lobby.json) come from the park generator (safari_park/zone_s2_lobby.py).
"""
import json
import os

from PIL import Image, ImageDraw

NAME = "safari_lobby"
CLOTHES = []
BONES = {}

ZONES = ["savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie"]
ZONE_NAAM = {"savanne": "Savanne", "delta": "Rivierdelta", "regenwoud": "Regenwoud", "bamboe": "Bamboebergen",
             "ijskust": "IJskust", "oertijd": "Oerijstijd", "europa": "Europees Woud", "australie": "Australië"}
# zone: (hue, sat, val) for the expert's uniform, and the badge colour
ZONE_KLEUR = {
    "savanne": ((0.13, 0.85, 1.0), (232, 196, 70)), "delta": ((0.50, 0.75, 0.9), (60, 180, 190)),
    "regenwoud": ((0.33, 0.85, 0.75), (60, 150, 60)), "bamboe": ((0.24, 0.8, 0.95), (140, 200, 80)),
    "ijskust": ((0.56, 0.55, 1.0), (140, 200, 240)), "oertijd": ((0.58, 0.12, 0.9), (200, 205, 210)),
    "europa": ((0.38, 0.7, 0.6), (70, 120, 60)), "australie": ((0.07, 0.9, 1.0), (220, 120, 50)),
}

NPCS = {
    # kind: (name, hue, sat, val)
    "safari_hoofdranger": ("Hoofdranger Guhmbo", 0.11, 0.55, 0.75),
    "safari_fotograaf": ("Fotograaf Flitsguh", 0.11, 0.45, 0.85),
    "safari_verzorger": ("Dierenverzorger Knuffelien", 0.30, 0.45, 0.7),
    "safari_verkoper": ("Giftshop-guh Kassie", 0.12, 0.35, 0.95),
    "safari_lodgebeheerder": ("Lodgebeheerder Snurkguh", 0.08, 0.45, 0.65),
    # test round 3 (J3): the ijskraam and the kampvuur
    "safari_ijsverkoper": ("IJsverkoper Smeltje", 0.90, 0.35, 1.0),
    "safari_verteller": ("Verteller Opa Knor", 0.05, 0.5, 0.6),
}
for _z in ZONES:
    NPCS[f"safari_expert_{_z}"] = (f"Expert {ZONE_NAAM[_z]}",) + ZONE_KLEUR[_z][0]

# test round 3 (J3): the ijskraam, the kampvuuravond, the kijk-guhs, the penguin parade and the message budget
J3_TEKSTEN = {
    # ijskraam
    "item.guhs.safari_pandaijsje": "Pandaijsje",
    "item.guhs.safari_pandaijsje.lore": "Vanille met chocoladeoortjes. Even lekker snel!",
    "item.guhs.safari_pandaijsje.op": "Mmm, pandaijsje! Je voetjes gaan vanzelf sneller.",
    "item.guhs.safari_zebrastreep": "Zebrastreep",
    "item.guhs.safari_zebrastreep.lore": "Zwart, wit, zwart, wit... en dan hup!",
    "item.guhs.safari_zebrastreep.op": "Zebrastreep op! Je springt als een vrolijke zebra.",
    "item.guhs.safari_pinguinijsje": "Pinguïnijsje",
    "item.guhs.safari_pinguinijsje.lore": "Zo koud dat je ervan gaat zwemmen.",
    "item.guhs.safari_pinguinijsje.op": "Brrr! Je zwemt nu als een pinguïn, njeg.",
    "item.guhs.safari_flamingosorbet": "Flamingosorbet",
    "item.guhs.safari_flamingosorbet.lore": "Roze, zoet, en je ziet er 's nachts alles mee.",
    "item.guhs.safari_flamingosorbet.op": "Flamingosorbet! Je ogen glinsteren in het donker.",
    "item.guhs.safari_marshmallow": "Geroosterde marshmallow",
    "item.guhs.safari_marshmallow.lore": "Van het kampvuur. Buiten krokant, binnen vads.",
    "item.guhs.safari_marshmallow.op": "Kleverig en warm... heerlijk!",
    "gui.guhs.safari.ijsverkoper.hallo": "Welkom bij het IJskraam! Ik ben Smeltje. Elk dierenijsje kost %1$s Safarimunten (je hebt er %2$s). Wat wordt het, vads?",
    "gui.guhs.safari.ijsverkoper.arm": "Oei, daar heb je niet genoeg munten voor. Een ijsje kost %s Safarimunten. Maak een paar mooie foto's en kom terug, njeg!",
    "gui.guhs.safari.ijsverkoper.alsjeblieft.0": "Eén pandaijsje! Pas op, hij kijkt je aan met zijn chocoladeoogjes.",
    "gui.guhs.safari.ijsverkoper.alsjeblieft.1": "Eén zebrastreep! Of was hij wit met zwarte strepen? Niemand weet het.",
    "gui.guhs.safari.ijsverkoper.alsjeblieft.2": "Eén pinguïnijsje! Snel opeten, voor hij wegwaggelt.",
    "gui.guhs.safari.ijsverkoper.alsjeblieft.3": "Eén flamingosorbet! Op één been eten mag ook.",
    "gui.guhs.safari.ijsverkoper.optie.nog": "Nog eentje!",
    # kampvuur
    "gui.guhs.safari.verteller.welkom": "Kom erbij zitten, vads, het vuurtje is warm. Hier, drie marshmallows om te roosteren. Luister...\n\n%s",
    "gui.guhs.safari.verteller.optie.nog": "Nog een verhaal!",
    "gui.guhs.safari.verteller.verhaal.0": "Er was eens een giraf die een geheimpje wilde fluisteren. Maar tegen de tijd dat het geheimpje zijn hele nek af was, was hij vergeten wat het was. Nu fluistert hij gewoon \"guh\". Iedereen vindt dat een prima geheimpje.",
    "gui.guhs.safari.verteller.verhaal.1": "De pinguïns hebben eens een dansfeest gehouden. Ze dansten de hele nacht, maar niemand zag het verschil met gewoon lopen. \"Dat ís onze dans,\" zei de oudste pinguïn trots.",
    "gui.guhs.safari.verteller.verhaal.2": "Een nijlpaard probeerde zich ooit te verstoppen achter een rietje. Heel stil. Héél stil. De ranger liep er drie keer langs en zei: \"Goedemorgen, nijlpaard.\" Het nijlpaard is nog steeds beledigd.",
    "gui.guhs.safari.verteller.verhaal.3": "De kangoeroe vond eens een guh in haar buidel. \"Hoe kom jij daar?\" vroeg ze. \"Geen idee,\" zei de guh, \"maar het is hier warm en er liggen koekkruimels.\" Ze zijn nu beste vrienden.",
    "gui.guhs.safari.verteller.verhaal.4": "De mammoet had het een keer warm. Hij wilde zijn jas uittrekken, maar dat ging niet, want hij wás zijn jas. Sindsdien woont hij in de IJstijd. Vahoeg, zegt hij, veel beter.",
    "gui.guhs.safari.verteller.verhaal.5": "Een zebra liep eens per ongeluk langs een vers geverfd hek. Nu heeft ze één witte streep te veel. Niemand heeft het gemerkt, behalve de zebra zelf. Ze telt ze elke ochtend.",
    "gui.guhs.safari.verteller.verhaal.6": "De luiaard begon op maandag aan een grap. Op vrijdag kwam de clou. Iedereen was het begin vergeten, maar ze lachten toch. Zo beleefd zijn ze hier.",
    "gui.guhs.safari.verteller.verhaal.7": "Er was eens een panda die geen zin had in bamboe. \"Wat wil je dan?\" vroeg de verzorger. De panda dacht heel lang na en zei: \"...bamboe.\" Het was een lange dag.",
    "gui.guhs.safari.verteller.verhaal.8": "De flamingo's houden elke avond een wedstrijd: wie het langst op één been kan staan. Er is nog nooit een winnaar geweest, want ze vallen allemaal tegelijk in slaap. Njeg.",
    # kijk-guhs
    "gui.guhs.safari.kijkguh.ziet.0": "Sst! Daar, door mijn verrekijker... een %s! Hij weet niet dat ik hem zie. Hihi.",
    "gui.guhs.safari.kijkguh.ziet.1": "Ik zit hier al sinds het ontbijt. Net zag ik een %s. Of was het een struik? Nee, toch een %1$s. Denk ik.",
    "gui.guhs.safari.kijkguh.ziet.2": "Kijk, kijk! Een %s! Ik heb al zeventien foto's. Allemaal een beetje wazig, maar met heel veel liefde.",
    "gui.guhs.safari.kijkguh.ziet.3": "Mijn verrekijker zegt: %s. Mijn buik zegt: lunch. Ik luister eerst naar de verrekijker.",
    "gui.guhs.safari.kijkguh.ziet.4": "Een %s! Die keek net recht naar mij. Ik heb heel beleefd teruggezwaaid, vads.",
    "gui.guhs.safari.kijkguh.ziet.5": "Daar loopt een %s. Ik geef hem een tien voor stijl en een negen voor waggelen.",
    "gui.guhs.safari.kijkguh.hint.0": "Psst... gisteren zag ik héél in de verte een %s, in de %s. Zeldzaam, hoor! Niet verder vertellen. Behalve aan jou.",
    "gui.guhs.safari.kijkguh.hint.1": "Weet je wat ik nog nooit gezien heb? Een %s. Ze zeggen dat hij zich verstopt in de %s. Als jij hem vindt, zeg je het dan?",
    "gui.guhs.safari.kijkguh.hint.2": "Tip van een oude kijkguh: wie stil is in de %2$s, ziet misschien een %1$s. Ik ben nooit stil. Daarom zie ik hem nooit.",
    "gui.guhs.safari.kijkguh.niks": "Ik zie... gras. Heel mooi gras, hoor. Ik wacht nog even, er komt vast zo een dier langs.",
    # message budget and the parade
    "gui.guhs.safari.melding.samen": "Straks in het park: %s",
    "gui.guhs.safari.melding.komma": ", ",
    "gui.guhs.safari.melding.en": " en ",
    "gui.guhs.safari.melding.meer": " en nog %s",
    "gui.guhs.safari.melding.voeder": "voedertijd bij %s (%s, %s)",
    "gui.guhs.safari.melding.praatje": "een rangerpraatje bij de halte %s (%s)",
    "gui.guhs.safari.melding.nacht": "de nachtsafari met lampiontrucks (%s)",
    "gui.guhs.safari.melding.parade": "de pinguïnparade door de IJskust en het dorp (%s)",
    "gui.guhs.safari.schema.parade": "Pinguïnparade (IJskust en dorp)",
}

TEKSTEN = {
    # --- characters ---
    "gui.guhs.safari.optie.doei": "Doei!",
    "gui.guhs.safari.optie.dank": "Dankjewel, vads!",
    "gui.guhs.safari.ranger.hallo": "Hallo daar, junior-ranger! Ik ben Guhmbo, de hoofdranger van Safaripark Vadse Bergen. Kan ik je ergens mee helpen, njeg?",
    "gui.guhs.safari.ranger.optie.tip": "Heb je een tip van de dag?",
    "gui.guhs.safari.ranger.optie.uitleg": "Wat kan ik hier allemaal doen?",
    "gui.guhs.safari.ranger.tip": "Psst... vanochtend zag ik de %1$s in de %2$s, ergens bij x %3$s, z %4$s. Sluip er voorzichtig heen, vads, anders verstopt hij zich!",
    "gui.guhs.safari.ranger.niks": "alle dieren",
    "gui.guhs.safari.ranger.uitleg": "Spot en fotografeer alle dieren voor je Safari-dex! Bij het expertenpaviljoen haal je een badge als een zone vol is. In de aaihoek kun je helpen, in de giftshop kun je met Safarimunten winkelen, en in de lodge slaap je lekker. Vahoeg!",
    "gui.guhs.safari.fotograaf.hallo": "Klik-klik! Ik ben Flitsguh. Hoe dichterbij, hoe scherper, en een dier dat iets leuks doet geeft extra sterren. Njeg, en niet bewegen!",
    "gui.guhs.safari.fotograaf.camera": "Jij hebt nog geen camera! Hier, deze mag je hebben. Foto's van dieren komen in je Safari-dex. Zeg eens kaas... of eigenlijk: zeg eens knabbel!",
    "gui.guhs.safari.verzorger.hallo": "Hoi! Ik ben Knuffelien van de aaihoek. Vandaag hebben ze trek in:\n%s\nHelp je mee? Voeren, borstelen, of de alpaca uitlaten aan het touwtje!",
    "gui.guhs.safari.verzorger.optie.voer": "Mag ik een zakje voer?",
    "gui.guhs.safari.verzorger.voer": "Alsjeblieft: een beetje van alles, plus een borstel en een touwtje als je die nog niet had. Raad maar wat ze vandaag lekker vinden, njeg!",
    "gui.guhs.safari.verzorger.voer.al": "Je hebt vandaag al een zakje gehad, vads. Morgen weer!",
    "gui.guhs.safari.verkoper.hallo": "Welkom in de giftshop! Alles voor Safarimunten, en niks is te vads.",
    "gui.guhs.safari.lodge.hallo": "Welkom in de lodge! Pak gerust een hutje: slaap je hier, dan word je ook hier wakker. En wie hier vroeg opstaat, krijgt voor zijn eerste foto van de dag dubbele munten. Snurk lekker, njeg!",
    "gui.guhs.safari.lodge.vroege_vogel": "Goedemorgen, vroege vogel! Je eerste foto van vandaag levert dubbele Safarimunten op.",
    # --- welcome (first visit) ---
    "gui.guhs.safari.welkom.1": "Welkom in Safaripark Vadse Bergen! Ik ben Guhmbo, de hoofdranger. Wat fijn dat je er bent, vads!",
    "gui.guhs.safari.welkom.2": "Hier wonen meer dan veertig soorten dieren, van leeuwen tot luiaards. Sommigen verstoppen zich heel goed, dus kijk goed om je heen.",
    "gui.guhs.safari.welkom.3": "Fotografeer ze voor je Safari-dex: hoe mooier de foto, hoe meer Safarimunten.",
    "gui.guhs.safari.welkom.3.camera": "Hier is je camera! Fotografeer de dieren voor je Safari-dex: hoe mooier de foto, hoe meer Safarimunten.",
    "gui.guhs.safari.welkom.4": "De experts in het paviljoen, de aaihoek, de giftshop en de lodge: kijk overal eens rond. Njeg, veel spotplezier!",
    "gui.guhs.safari.welkom.optie": "Op safari, vahoeg!",
    "gui.guhs.safari.bus.terug": "Toet toet! De Guh-safaribus brengt je terug naar huis. Tot de volgende safari, vahoeg!",
    # --- experts ---
    **{f"gui.guhs.safari.expert.{z}.nog": f"Ik ben de expert van de {ZONE_NAAM[z]}. Je hebt %1$s van de %2$s soorten hier gefotografeerd. Nog %3$s te gaan, vads! Kom terug als ze er allemaal in zitten." for z in ZONES if z != "australie"},
    **{f"gui.guhs.safari.expert.{z}.al": f"Je bent een echte {ZONE_NAAM[z]}-kenner! Al %1$s van de %2$s soorten in je dex. Vahoeg!" for z in ZONES if z != "australie"},
    **{f"gui.guhs.safari.expert.{z}.beloning": "Alle soorten van de %s in je dex? Wauw! Hier is je zonebadge, een schilderij voor thuis en 100 Safarimunten. Njeg, wat ben ik trots!" for z in ZONES if z != "australie"},
    "gui.guhs.safari.expert.australie.nog": "G'day, njeg mate! Je hebt %1$s van de %2$s outback-soorten gekiekt. Nog %3$s te gaan, no worries, vads!",
    "gui.guhs.safari.expert.australie.al": "Strewth, wat een vahoege outback-kenner! %1$s van de %2$s soorten. Good on ya, mate!",
    "gui.guhs.safari.expert.australie.beloning": "Crikey! Alle soorten van %s in de pocket! Hier, mate: je badge, een schilderij en 100 Safarimunten. No worries, njeg!",
    # --- aaihoek ---
    "gui.guhs.safari.aaihoek.hint": "%1$s: %2$s",
    "gui.guhs.safari.aaihoek.voer.wortel": "iets oranjes en knapperigs",
    "gui.guhs.safari.aaihoek.voer.hooi": "iets droogs en geels",
    "gui.guhs.safari.aaihoek.voer.appel": "iets ronds en sappigs",
    "gui.guhs.safari.aaihoek.voer.komkommer": "iets groens en frisjes",
    "gui.guhs.safari.aaihoek.voer.paprika": "iets felroods met een steeltje",
    "gui.guhs.safari.aaihoek.voer.suikerklontje": "iets wits en zoets",
    "gui.guhs.safari.aaihoek.smul": "%s smult ervan! Vahoeg!",
    "gui.guhs.safari.aaihoek.geborsteld": "%s glanst helemaal na het borstelen!",
    "gui.guhs.safari.aaihoek.bah.niet": "Bah! Dat eet %s helemaal niet, njeg.",
    "gui.guhs.safari.aaihoek.bah.vandaag": "Bah! Daar heeft %s vandaag geen zin in. Vraag de verzorger maar.",
    "gui.guhs.safari.aaihoek.al_gedaan": "Dat heb je vandaag al gedaan. Morgen weer, vads!",
    "gui.guhs.safari.aaihoek.genoeg": "(Je hebt vandaag al genoeg verdiend, maar het diertje vindt het nog steeds fijn.)",
    "gui.guhs.safari.aaihoek.tip.geitje": "Het geitje knabbelt aan je mouw. Voer, borstel, of vraag de verzorger wat het lekker vindt!",
    "gui.guhs.safari.aaihoek.tip.alpaca": "De alpaca kijkt je aan. Met een touwtje kun je hem uitlaten!",
    "gui.guhs.safari.aaihoek.tip.cavia": "Piep piep! De cavia hoopt op iets lekkers.",
    "gui.guhs.safari.aaihoek.tip.ezeltje": "Iiee-aah! Het ezeltje wil geborsteld worden.",
    "gui.guhs.safari.aaihoek.wandel.begin": "Je laat de alpaca uit! Loop het rondje langs alle punten en breng hem terug naar zijn hok.",
    "gui.guhs.safari.aaihoek.wandel.stap": "Punt %1$s van %2$s, de alpaca dribbelt vrolijk mee!",
    "gui.guhs.safari.aaihoek.wandel.klaar": "Rondje klaar! De alpaca is moe maar blij.",
    "gui.guhs.safari.aaihoek.wandel.weg": "De alpaca is terug naar zijn hok gedribbeld.",
    "gui.guhs.safari.aaihoek.wandel.al": "Je laat al een alpaca uit, njeg.",
    "gui.guhs.safari.aaihoek.wandel.bezet": "Deze alpaca wordt al uitgelaten.",
    # --- visitor guhs ---
    "gui.guhs.safari.bezoeker.0": "Njeg, ik heb net een leeuw gezien!",
    "gui.guhs.safari.bezoeker.1": "Ik ben op safari, ik ga niet mee naar huis, vads.",
    "gui.guhs.safari.bezoeker.2": "Heb jij de okapi al gevonden? Ik zoek al de hele dag!",
    "gui.guhs.safari.bezoeker.3": "Die pinguïns zijn nóg vadsiger dan ik. Vahoeg!",
    "gui.guhs.safari.bezoeker.4": "Ssst... ik spot een stokstaartje.",
    # --- giftshop ---
    "gui.guhs.safari.winkel.titel": "Giftshop Vadse Bergen",
    "gui.guhs.safari.winkel.label": "SAFARI",
    "gui.guhs.safari.winkel.saldo": "%s Safarimunten",
    "gui.guhs.safari.winkel.grap.0": "Alles op voorraad, behalve de olifant.",
    "gui.guhs.safari.winkel.grap.1": "Knuffels niet in de bek nemen. Ook niet als je een leeuw bent.",
    "gui.guhs.safari.winkel.grap.2": "Nieuw: pinguïnhoedjes voor pinguïns (geen pinguïns gevonden die ze willen).",
    "gui.guhs.safari.winkel.grap.3": "Betalen kan met Safarimunten. Knabbeltjes worden opgegeten.",
    "gui.guhs.safari.winkel.grap.4": "De giraffe vond deze winkel te laag.",
    "gui.guhs.safari.winkel.grap.5": "Gratis inpakken? Nee, njeg. Vadsig inpakken: ja!",
    "gui.guhs.safari.winkel.kleine_lettertjes": "Kleine lettertjes: verblijfsborden alleen voor dieren uit je Safari-dex, één per soort per 7 dagen. Dieren niet terugbrengen.",
    "gui.guhs.safari.winkel.groep.kleding": "Safarikleding",
    "gui.guhs.safari.winkel.groep.borden": "Verblijfsborden",
    "gui.guhs.safari.winkel.groep.blokken": "Safari-bouwblokken",
    "gui.guhs.safari.winkel.groep.kunst": "Posters en schilderijen",
    "gui.guhs.safari.winkel.groep.voer": "Voer",
    "gui.guhs.safari.winkel.groep.blauwdrukken": "Blauwdrukken",
    "gui.guhs.safari.winkel.leeg": "De schappen zijn leeg. Kom later terug, njeg!",
    "gui.guhs.safari.winkel.aantal": "%s dingen",
    "gui.guhs.safari.winkel.aantal.1": "1 ding",
    "gui.guhs.safari.winkel.prijs": "%1$s Safarimunten",
    "gui.guhs.safari.winkel.prijs_al": "%1$s Safarimunten · al %2$s gekocht",
    "gui.guhs.safari.winkel.heb_je": "Heb je al, vahoeg!",
    "gui.guhs.safari.winkel.knop.koop": "Koop",
    "gui.guhs.safari.winkel.knop.op": "Heb je",
    "gui.guhs.safari.winkel.max": "Gekocht: %1$s van %2$s",
    "gui.guhs.safari.winkel.gekocht": "Gekocht: %s. Vahoeg!",
    "gui.guhs.safari.winkel.tekort": "Nog %s Safarimunten tekort. Ga nog wat dieren kieken, njeg!",
    "gui.guhs.safari.winkel.nee.onbekend": "Dat verkoop ik niet (meer), njeg.",
    "gui.guhs.safari.winkel.nee.niet_hier": "Kopen kan alleen in Safaripark Vadse Bergen, njeg.",
    "gui.guhs.safari.winkel.nee.max": "Daar heb je er al genoeg van, njeg.",
    "gui.guhs.safari.winkel.nee.eis": "Dat kun je nog niet kopen, njeg.",
    "gui.guhs.safari.winkel.nee.te_duur": "Daar heb je nog niet genoeg Safarimunten voor, njeg.",
    "gui.guhs.safari.winkel.nee.mislukt": "Dat lukte niet. Je munten heb je nog, njeg.",
    "gui.guhs.safari.winkel.binnenkort": "Binnenkort in de winkel!",
    "gui.guhs.safari.winkel.blauwdrukken": "Safari-blauwdrukken",
    "gui.guhs.safari.winkel.blauwdrukken.uitleg": "Bouw de lodge, de rangerpost of de uitkijktoren gewoon thuis na. Binnenkort, vads!",
    "gui.guhs.safari.winkel.aaihoekvoer": "Een handje voer voor de aaihoek (4 stuks).",
    "gui.guhs.safari.winkel.bord": "Verblijfsbord: %s",
    "gui.guhs.safari.winkel.bord.uitleg.gewoon": "Een familie van 4 voor je eigen base (2 volwassenen + 2 jongen). Een gewoon dier.",
    "gui.guhs.safari.winkel.bord.uitleg.middel": "Een familie van 4 voor je eigen base (2 volwassenen + 2 jongen). Een bijzonder dier.",
    "gui.guhs.safari.winkel.bord.uitleg.zeldzaam": "Een familie van 4 voor je eigen base (2 volwassenen + 2 jongen). Een zeldzaam dier!",
    "gui.guhs.safari.winkel.bord.wacht_dagen": "Nog %s dag(en) wachten voor een nieuw bord van deze soort.",
    "gui.guhs.safari.winkel.bord.wacht_uren": "Nog %s uur wachten voor een nieuw bord van deze soort.",
    # --- items ---
    "item.guhs.safari_komkommer": "Komkommer",
    "item.guhs.safari_paprika": "Paprika",
    **{f"item.guhs.safari_badge_{z}": f"Zonebadge: {ZONE_NAAM[z]}" for z in ZONES},
    **J3_TEKSTEN,
}

# the giftshop's item list: S6 makes these items (an entry whose item does not exist is not shown)
WINKEL = [
    # (ids of slice S6, checked at the step-2 integration; the paintings are Java offers in SafariKoppelingen)
    {"id": "kleding_safarihoed", "groep": "kleding", "item": "guhs:safarihoed", "prijs": 25, "max": 1},
    {"id": "kleding_verrekijker", "groep": "kleding", "item": "guhs:verrekijker", "prijs": 40, "max": 1},
    {"id": "kleding_guh_hoedje", "groep": "kleding", "item": "guhs:safari_hoedje", "prijs": 15},
    {"id": "kleding_guh_vestje", "groep": "kleding", "item": "guhs:safari_vestje", "prijs": 20},
    {"id": "kleding_guh_rangerpet", "groep": "kleding", "item": "guhs:safari_rangerpet", "prijs": 15},
    {"id": "kleding_guh_verrekijker", "groep": "kleding", "item": "guhs:safari_verrekijker", "prijs": 20},
    {"id": "voer_safarivoer", "groep": "voer", "item": "guhs:safarivoer", "prijs": 5, "aantal": 4},
    {"id": "blok_rieten_dak", "groep": "blokken", "item": "guhs:rieten_dak", "prijs": 5, "aantal": 16},
    {"id": "blok_rieten_dak_trap", "groep": "blokken", "item": "guhs:rieten_dak_trap", "prijs": 5, "aantal": 16},
    {"id": "blok_rieten_dak_plaat", "groep": "blokken", "item": "guhs:rieten_dak_plaat", "prijs": 5, "aantal": 16},
    {"id": "blok_bamboewand", "groep": "blokken", "item": "guhs:bamboewand", "prijs": 5, "aantal": 16},
    {"id": "blok_bamboe_omheining", "groep": "blokken", "item": "guhs:bamboe_omheining", "prijs": 5, "aantal": 16},
    {"id": "blok_safari_acaciaplanken", "groep": "blokken", "item": "guhs:safari_acaciaplanken", "prijs": 5, "aantal": 16},
    {"id": "blok_dierentuinbordje", "groep": "blokken", "item": "guhs:dierentuinbordje", "prijs": 8, "aantal": 4},
]


def _badge(kleur):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    donker = tuple(int(c * 0.55) for c in kleur)
    d.ellipse([1, 1, 14, 14], fill=(240, 200, 80, 255), outline=(140, 100, 30, 255))
    d.ellipse([3, 3, 12, 12], fill=kleur + (255,), outline=donker + (255,))
    # a little paw print
    d.ellipse([6, 7, 9, 10], fill=(255, 250, 235, 255))
    for (x, y) in ((5, 5), (7, 4), (9, 4), (10, 6)):
        d.point((x, y), fill=(255, 250, 235, 255))
    d.point((7, 14), fill=(200, 60, 60, 255))
    d.point((8, 14), fill=(200, 60, 60, 255))
    return img


def _komkommer():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([(3, 12), (12, 3)], fill=(40, 110, 40, 255), width=4)
    d.line([(4, 12), (12, 4)], fill=(80, 160, 60, 255), width=2)
    for (x, y) in ((6, 9), (9, 6), (11, 5)):
        d.point((x, y), fill=(190, 230, 150, 255))
    d.point((12, 2), fill=(120, 90, 40, 255))
    return img


def _paprika():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([3, 5, 12, 14], fill=(210, 40, 40, 255), outline=(140, 20, 20, 255))
    d.line([(7, 6), (7, 12)], fill=(150, 25, 25, 255))
    d.point((5, 8), fill=(255, 140, 140, 255))
    d.rectangle([7, 2, 8, 5], fill=(60, 130, 40, 255))
    return img


def _ijsje(bol, extra):
    """An ice cream cone with a scoop of colour bol; extra(d) draws the animal on top."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(5, 8), (11, 8), (8, 15)], fill=(215, 160, 85, 255), outline=(150, 100, 45, 255))
    d.line([(6, 10), (9, 13)], fill=(170, 115, 55, 255))
    d.line([(10, 10), (8, 12)], fill=(170, 115, 55, 255))
    d.ellipse([4, 2, 12, 10], fill=bol + (255,), outline=tuple(int(c * 0.6) for c in bol) + (255,))
    extra(d)
    return img


def _panda(d):
    for (x, y) in ((4, 2), (11, 2)):
        d.rectangle([x, y, x + 1, y + 1], fill=(30, 30, 35, 255))
    d.rectangle([6, 5, 7, 6], fill=(30, 30, 35, 255))
    d.rectangle([9, 5, 10, 6], fill=(30, 30, 35, 255))
    d.point((8, 8), fill=(30, 30, 35, 255))


def _zebra(d):
    for x in (5, 7, 9, 11):
        d.line([(x, 3), (x - 1, 9)], fill=(25, 25, 30, 255))


def _pinguin(d):
    d.ellipse([6, 4, 10, 10], fill=(250, 250, 250, 255))
    d.point((7, 5), fill=(20, 20, 30, 255))
    d.point((9, 5), fill=(20, 20, 30, 255))
    d.point((8, 6), fill=(250, 150, 30, 255))


def _flamingosorbet():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([7, 10, 8, 15], fill=(230, 205, 160, 255))
    d.rounded_rectangle([4, 1, 11, 11], radius=3, fill=(250, 130, 170, 255), outline=(190, 70, 110, 255))
    d.line([(6, 3), (6, 9)], fill=(255, 190, 210, 255))
    d.point((9, 3), fill=(30, 30, 30, 255))
    d.line([(10, 4), (11, 5)], fill=(40, 40, 40, 255))
    return img


def _marshmallow():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([(1, 15), (9, 7)], fill=(120, 85, 50, 255), width=1)
    d.rounded_rectangle([8, 2, 14, 8], radius=2, fill=(250, 240, 225, 255), outline=(170, 110, 60, 255))
    d.line([(9, 7), (13, 7)], fill=(200, 140, 70, 255))
    d.point((12, 3), fill=(220, 170, 100, 255))
    return img


IJSJES = {
    "safari_pandaijsje": lambda: _ijsje((245, 245, 240), _panda),
    "safari_zebrastreep": lambda: _ijsje((240, 240, 240), _zebra),
    "safari_pinguinijsje": lambda: _ijsje((40, 45, 60), _pinguin),
    "safari_flamingosorbet": _flamingosorbet,
    "safari_marshmallow": _marshmallow,
}


def build(h):
    for k, v in TEKSTEN.items():
        h.NL[k] = v
    # the lobby's guhs: the sitting guh recoloured (stub_npc of bbq2)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    for kind, (naam, hue, sat, val) in NPCS.items():
        h.save(h.recolour(src, hue=hue, sat=sat, val=val, only=h.pinkish), "entity", f"npc_{kind}.png")
        h.NL[f"entity.guhs.guh_npc.{kind}"] = naam
    # items
    h.save(_komkommer(), "item", "safari_komkommer.png")
    h.item_model("safari_komkommer")
    h.save(_paprika(), "item", "safari_paprika.png")
    h.item_model("safari_paprika")
    for naam, maak in IJSJES.items():
        h.save(maak(), "item", f"{naam}.png")
        h.item_model(naam)
    for z in ZONES:
        h.save(_badge(ZONE_KLEUR[z][1]), "item", f"safari_badge_{z}.png")
        h.item_model(f"safari_badge_{z}")
    h.w(f"{h.D}/safari/winkel.json", WINKEL)


# --- direct mode -----------------------------------------------------------------------------------------------------
# python tools/features/safari_lobby.py --direct   (from the project root): writes only this slice's files without the
# full make_resources run: the Dutch texts into nl_nl.json, the English overlay (tools/lang/en/c161_safari_lobby.json) into
# en_us.json, the NPC skins, the item textures, models and 26.1 item definitions, and winkel.json.
def _direct():
    import sys
    import numpy as np  # noqa: F401  (recolour)
    sys.path.insert(0, "tools")
    R = os.path.join("src", "main", "resources")
    A = os.path.join(R, "assets", "guhs")

    class H:
        NL = {}
        TEX = os.path.join(A, "textures")
        D = os.path.join(R, "data", "guhs")

        @staticmethod
        def w(path, obj):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                json.dump(obj, f, indent=2, ensure_ascii=False)
                f.write("\n")

        @staticmethod
        def save(img, *path):
            full = os.path.join(H.TEX, *path)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            img.save(full)

        @staticmethod
        def item_model(name):
            H.w(os.path.join(A, "models", "item", f"{name}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{name}"}})
            H.w(os.path.join(A, "items", f"{name}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})

    import make_v2 as _v2_helpers  # noqa: F401  only for recolour/pinkish (cheap module functions)
    H.recolour = staticmethod(_v2_helpers.recolour)
    H.pinkish = staticmethod(_v2_helpers.pinkish)
    build(H)
    with open(os.path.join("tools", "lang", "en", "c161_safari_lobby.json"), encoding="utf-8") as f:
        en = json.load(f)
    for taal, extra in (("nl_nl", H.NL), ("en_us", {k: en.get(k, v) for k, v in H.NL.items()})):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(extra)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"safari_lobby: {len(H.NL)} texts, {len(NPCS)} skins, {2 + len(ZONES) + len(IJSJES)} items")


if __name__ == "__main__" and "--direct" in __import__("sys").argv:
    _direct()
