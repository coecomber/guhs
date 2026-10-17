"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.6), slice sp_d2_heen: every Dutch text of part 2 steps 0-4 (the English overlay is
tools/lang/en/c115_e2_sp_d2_heen.json). Not a feature module: einde2_sp_d2_heen.py writes these.

Copyright (CONTRACT_E2_C 5.1, C-B 6.3): every line is our own. The famous moments are told in our own words and guh-ified.
The one literal line is the user's own (CONTRACT_E2_C 5.3): Sponsguh at his sundae says EXACTLY "zeg.. hoort hier geen
poppetje bij?" (lower-case "zeg", two dots).
"""

G = "gui.guhs.einde2_sp_d2_heen."
Q = "quest.guhs.einde2_sp_d2_heen."
SCENE = "einde2_sp_d2_heen_"

# the speakers' names (scene.guhs.<scene>.naam.<acteur>)
NAMEN = {
    "verteller": "De Verteller", "sponsguh": "Sponsguh", "patguh": "Patguh", "krabguh": "Meneer Krabguh", "octo": "Octo Tentakel",
    "ijsverkoper": "De ijsverkoper", "balto": "Baltoguh", "neptuguh": "Koning Neptuguh", "plankmika": "Plankmika",
    "mika": "Een stoere Mika", "barman": "De barman", "uitsmijter": "De uitsmijter", "kroeg": "Alle Mika's",
    "samen": "Sponsguh en Patguh",
}


def namen(*wie):
    return {w: NAMEN[w] for w in wie}


# =====================================================================================================================
# the narrator card of part 2 (Verteller "einde2_sp_d2_heen"): the road from Bikinibodem to Schelpenstad
# =====================================================================================================================
KAART_TITEL = "Deel 2: De kroon van koning Neptuguh"
KAART_REGELS = [
    "Ah, Bikinibodem. Het recept is terug, de hele stad heeft gezongen, en alles is weer zoals het hoort, mes amis.",
    "Maar diep in zijn kantoor telt Meneer Krabguh zijn knabbels. En hij droomt. Van méér knabbels.",
    "Een tweede restaurant! En een tweede restaurant heeft een manager nodig. Sponsguh weet het zeker: dat wordt hij.",
    "Wat nog niemand weet: de weg naar Schelpenstad is lang, ruig en vol stoere Mika's. Voilà, njeg.",
]

# =====================================================================================================================
# the scenes: name -> (title, {key: line}, {actor: name})
# =====================================================================================================================
SCENES = {
    "manager": ("De nieuwe manager", {
        "nieuws": "Luister allemaal! De Krokante Knabbel gaat... VERDUBBELEN! Pal hiernaast komt De Krokante Knabbel 2. Door de muur. Dat scheelt huur.",
        "fluister": "Een tweede restaurant heeft een manager nodig. Ik oefen al jaren mijn managergezicht. Kijk. Kijk dan. Dit is het.",
        "wordt": "En de manager van De Krokante Knabbel 2 wordt...",
        "octo": "...Octo Tentakel!",
        "zucht": "Hoera. Nu kan ik in twee restaurants tegelijk zuchten.",
        "maar": "Maar... ik ben al driehonderdtwaalf keer Werknemer van de Week geweest! Op een rij!",
        "kind": "Je bent mijn allerbeste bakker, jongen. Maar een manager? Je bent nog maar een kind. Een heel vierkant kind.",
        "oortjes": "Sponsguhs oortjes zakken. Heel langzaam. Helemaal tot op de vloer, njeg.",
        "ijs": "SPONSGUH! Ik hoorde iets heel zieligs! Weet je wat daartegen helpt? IJS! Heel veel ijs!",
        "mee": "Jij gaat ook mee, toch? De Guhfy Guhber heeft de grootste ijsjes van de hele zeepbel. Hij is links. Of rechts. Hij is daar.",
    }, namen("verteller", "sponsguh", "patguh", "krabguh", "octo")),
    "ijssalon": ("Een ijsje in de Guhfy Guhber", {
        "torens": "Drie Guhfy Guhber Knabbeltorens! Met extra alles, en extra extra alles erbovenop.",
        "top": "Wauw... Ik zie de top niet eens. Zou daar sneeuw liggen?",
        "mooi": "Het is... prachtig. Het mooiste wat ik ooit gezien heb. Na een Knabbelburger.",
        # CONTRACT_E2_C 5.3: THE line of the user, verbatim
        "poppetje": "zeg.. hoort hier geen poppetje bij?",
        "speelgoed": "...Meneer. Dit is een ijssalon. Geen speelgoedwinkel.",
        "gelijk": "Hij heeft wel gelijk, hoor. Ik heb ook nog nooit een poppetje gekregen. Nooit.",
        "hap": "Dan is het ijsje gewoon het poppetje! HAP! ...AAAH! Koud in mijn oortjes!",
        "zingen": "Guhfy Guhber! Guhfy Guhber! GUHFY GUHBEEER!",
        "hoedje": "Hier. Een ijshoedje voor de nieuwe stamgast. Van het huis. Als jullie nu maar ophouden met zingen.",
    }, namen("sponsguh", "patguh", "ijsverkoper", "samen")),
    "knipoog_ijs": ("Baltoguh in de ijssalon", {
        "ijs": "IJs. Sneeuw. Bijna hetzelfde.",
        "zoeken": "Ik blijf nog even zoeken, njeg.",
        "kistje": "Naast zijn kruk staat het medicijnkistje. Hij ziet het niet.",
    }, namen("verteller", "balto")),
    "bevroren": ("De kroon is weg!", {
        "nacht": "Die nacht, mes amis. Het is donker. Stel je dat even voor: onder een zeepbel zie je het verschil niet zo goed.",
        "plan": "Plan Z, deel twee. Eerst de kroon van de koning. En dan... de schuld voor Krabguh. Hihihi.",
        "pakket": "Zo. Een pakketje naar Schelpenstad. En dit lege kussentje... leg ik bij Krabguh.",
        "verstopt": "Plankmika legt het lege kroonkussentje op het bureau van Meneer Krabguh. Kwaad. Héél kwaad.",
        "ochtend": "De volgende ochtend, njeg...",
        "wie": "WIE HEEFT MIJN KROON GESTOLEN?!",
        "zag": "Ik zag het kroonkussentje bij Krabguh liggen! Toevallig. Heel erg toevallig.",
        "kussentje": "Dit kussentje? Ik dacht: een gratis kussentje, dat neem ik mee. Gratis is mijn lievelingsprijs!",
        "waar": "En waar is mijn KROON?!",
        "weet_niet": "Kroon? Welke kroon? Hoeveel is die eigenlijk waard? ...Ik bedoel: ik weet van niks!",
        "fluister": "Die kroon ligt allang in Schelpenstad. Hihi.",
        "dief": "DIEF! Dan krijg jij een koude straf!",
        "nooit": "Nee! Meneer Krabguh zou nooit iets stelen! Nou ja... behalve misschien een knabbel van de vloer.",
        "schelpenstad": "Mijn wachters zagen vannacht een pakketje vertrekken. Naar Schelpenstad. Waar nog nooit iemand uit terugkwam.",
        "wij": "Dan halen wíj uw kroon terug! Ik, Patguh en... onze nieuwe collega!",
        "lach": "HAHAHA! Een vierkantje, een sterretje en een... dingetje! Goed dan.",
        "zonsondergang": "Ligt mijn kroon hier voor zonsondergang, dan ontdooi ik jullie baas. Zo niet, dan blijft hij voor altijd een ijsje.",
        "zeepbel": "Eh, majesteit? Wij wonen onder een zeepbel. Hoe zien wij nou wanneer de zon ondergaat?",
        "probleem": "...Dat is jullie probleem.",
        "mobiel": "Naar de Knabbelburgermobiel! Hij staat voor De Krokante Knabbel!",
    }, namen("verteller", "sponsguh", "patguh", "krabguh", "neptuguh", "plankmika")),
    "rit": ("De Knabbelburgermobiel", {
        "kijk": "De Knabbelburgermobiel! Het snelste broodje van Bikinibodem! Met echte sesamzaadjes op het dak.",
        "wie": "Wie gaat er rijden?",
        "voorlopig": "Ik heb alleen een voorlopig rijbewijs. Voor boten. En juf Pufguh huilt nog steeds.",
        "jij": "Dan rijdt... JIJ!",
        "weg": "De stadspoort uit, naar het oosten, en dan gewoon de weg volgen tot een roestig sleepbootje. Wat kan er nou misgaan, njeg?",
    }, namen("sponsguh", "patguh")),
    "kroeg": ("De Ruige Kroeg", {
        "kroeg": "De Ruige Kroeg. Hier komen alleen de allerstoerste Mika's van de hele leegte.",
        "stoer": "Ik ben ook stoer. Kijk maar. Grrr. ...Was dat stoer?",
        "sleutel": "Mag ik de sleutel even vasthouden? Ik heb nog nooit een autosleutel vastgehouden. Alleen spatels.",
        "mooi": "Mooi sleuteltje. Nu is hij van mij.",
        "he": "Hé! Dat is de sleutel van de Knabbelburgermobiel!",
        "baby": "Kom hem maar halen. Als je durft, baby'tje.",
        "later": "Vijf minuten later, njeg...",
        "slaapt": "...is de stoere Mika in slaap gevallen. Aan zijn tafel. Met het sleuteltje aan zijn riem.",
        "makkie": "Hij slaapt! Makkie!",
        "tien": "Patguh. Daarbinnen zitten er nog tien. En die slapen niet.",
    }, namen("verteller", "sponsguh", "patguh", "mika")),
    "baby": ("Wie was het baby'tje?", {
        "ruikt": "De Ruige Kroeg. Het ruikt er naar oud vet, natte sokken en stoere verhalen.",
        "stoer_doen": "Heel stoer doen, iedereen. Niemand mag merken dat wij lief zijn.",
        "wie": "...Wie blies daar een bel?",
        "baby": "Alleen baby'tjes blazen hier bellen.",
        "allemaal": "WIE WAS HET BABY'TJE?!",
        "stil": "Niemand beweegt. Heel lang. Dan haalt de barman zijn schouders op, en iedereen kijkt weer weg.",
        "huiswerk": "Als ze kijken, sta je stil. Dan zien ze je niet. Zo doe ik het ook altijd met mijn huiswerk.",
        "glas": "Hoor je het glas van de barman rinkelen? Dan kijken ze zo op. Ga jij maar. Wij blijven hier... heel... stil... staan.",
    }, namen("verteller", "sponsguh", "patguh", "barman", "uitsmijter", "kroeg")),
    "sleutel_terug": ("Het sleuteltje", {
        "held": "Je hebt hem! Zonder ook maar één belletje! Jij bent een echte stille held.",
        "stil": "Ik heb ook heel goed stilgestaan. Ik sta trouwens nog steeds stil. Kijk.",
        "verder": "Op naar Schelpenstad! Hou vol, Meneer Krabguh! Niet smelten! ...Nee, wacht. Juist wel smelten. Straks.",
    }, namen("sponsguh", "patguh")),
}

# the look scenes of the hints (StapHint.kijkScene, ~3 s, one caption): name -> (title, caption)
KIJK = {
    "knabbel": ("De Krokante Knabbel", "De Krokante Knabbel. Meneer Krabguh wacht binnen met groot nieuws."),
    "ijssalon": ("De Guhfy Guhber", "De Guhfy Guhber-ijssalon, met de ijscoupe op het dak. Jouw kruk staat aan de bar."),
    "plein": ("Het Bellenplein", "Het Bellenplein bij de lift. Daar heeft koning Neptuguh zijn troon neergezet."),
    "mobiel": ("De Knabbelburgermobiel", "De Knabbelburgermobiel staat voor De Krokante Knabbel. Sponsguh en Patguh wachten al."),
    "kroeg": ("De Ruige Kroeg", "De Ruige Kroeg: het roestige sleepbootje aan de weg. Het sleuteltje ligt binnen."),
}

# =====================================================================================================================
# the step texts (einde2_sp_verhaal puts them into the line): (stapnaam, nu, waar), the variants, the short lines
# =====================================================================================================================
STAPPEN = {
    0: ("De nieuwe manager",
        "Meneer Krabguh heeft groot nieuws voor iedereen. Loop De Krokante Knabbel binnen: het grote restaurant aan de Hoofdstraat, "
        "ten oosten van het Bellenplein.",
        "De Krokante Knabbel, aan de Hoofdstraat oost van het plein"),
    1: ("Een ijsje",
        "Sponsguh is verdrietig: hij wordt geen manager. Patguh weet raad: ijs! Ga naar de Guhfy Guhber-ijssalon (de ijscoupe op het "
        "dak), aan de Hoofdstraat ten westen van het plein, en ga op je kruk aan de bar zitten.",
        "Jouw kruk aan de bar van de Guhfy Guhber, west van het plein"),
    2: ("De kroon is weg!",
        "Er schalt een hoorn over de stad: koning Neptuguh is op het Bellenplein, en hij is woedend. Ren naar zijn troon op het plein, "
        "naast de lift.",
        "De troon van koning Neptuguh op het Bellenplein"),
    3: ("De Knabbelburgermobiel",
        "Krabguh is bevroren en de kroon ligt in Schelpenstad. Sponsguh en Patguh wachten bij de Knabbelburgermobiel voor De "
        "Krokante Knabbel. Stap in (klik de auto): jij rijdt.",
        "De Knabbelburgermobiel voor De Krokante Knabbel"),
    4: ("De Ruige Kroeg",
        "Een stoere Mika heeft de sleutel van de Knabbelburgermobiel gepakt en slaapt nu aan zijn tafel. Ga De Ruige Kroeg binnen "
        "(het roestige sleepbootje aan de weg) en pak het sleuteltje terug zonder dat iemand je ziet bewegen.",
        "Het sleuteltje van de slapende Mika in De Ruige Kroeg"),
}
EXTRA = {
    "3_b": ("Rij de stadspoort uit naar het oosten en volg de weg naar De Ruige Kroeg, het roestige sleepbootje. Blijf op de weg: "
            "de Knabbelburgermobiel stopt vanzelf bij een rand. Parkeer in het middelste vak.",
            "De parkeervakken van De Ruige Kroeg, aan de weg naar Schelpenstad"),
    "4_b": ("Sluip naar het sleuteltje van de slapende Mika. Rinkelt het glas van de barman? Sta stil: zolang de Mika's kijken, mag je "
            "niet bewegen.",
            "Het sleuteltje van de slapende Mika in De Ruige Kroeg"),
}
KORT = {
    "0": "Naar De Krokante Knabbel (oost van het plein)",
    "1": "IJs in de Guhfy Guhber (west van het plein)",
    "2": "Naar de troon op het plein!",
    "3": "Stap in de Knabbelburgermobiel",
    "3_b": "Rij naar De Ruige Kroeg (oost)",
    "4": "Pak het sleuteltje in De Ruige Kroeg",
    "4_b": "Sta stil als ze kijken!",
}

# =====================================================================================================================
# chat, titles, hints, clicks
# =====================================================================================================================
TEKSTEN = {
    # the hints (StapHint; %s = the direction, "in het oosten, ..." or "hier vlakbij")
    G + "hint.knabbel": "Parel komt aangerend: \"Papa heeft groot nieuws voor iedereen!\" Ga naar De Krokante Knabbel, %s.",
    G + "hint.ijssalon": "Sponsguh en Patguh zijn al vooruit naar de Guhfy Guhber-ijssalon (de ijscoupe op het dak), %s. Ga op je kruk aan de bar zitten.",
    G + "hint.plein": "Er schalt een hoorn over de stad! Ren naar de troon van koning Neptuguh op het Bellenplein, %s.",
    G + "hint.rit": "Naar De Ruige Kroeg! Stap in de Knabbelburgermobiel voor De Krokante Knabbel en rij de stadspoort uit naar het oosten. Je doel: %s.",
    G + "hint.kroeg": "Het sleuteltje ligt in De Ruige Kroeg, het roestige sleepbootje, %s. Sta stil als de Mika's opkijken!",
    # the reward
    Q + "beloning.hoedje": "Je guhs kunnen nu het Guhfy Guhber-ijshoedje dragen (kijk in de kledingkast)!",
    # the horn after the ice cream
    Q + "hoorn": "Er schalt een enorme hoorn over heel Bikinibodem. Iemand op het plein is heel, heel boos.",
    # the drive (travel banter in the chat is fine)
    Q + "rit.instappen": "Klik de Knabbelburgermobiel om in te stappen. Jij rijdt!",
    Q + "rit.poort": "<Patguh> Dag Bikinibodem! Dag zeepbel! Dag... eh, hoe heette mijn rots ook alweer?",
    Q + "rit.naad": "<Sponsguh> Volgens de kaart is het nog maar een klein stukje. De kaart is wel een servetje met een vlek erop.",
    Q + "rit.bijna": "<Sponsguh> Daar! Het roestige sleepbootje! Parkeer maar in het middelste vak, naast het anker.",
    Q + "rit.uitgestapt": "<Sponsguh> Hé, waar ga je heen? Wij wachten wel in de auto. Klik de Knabbelburgermobiel als je verder wilt rijden!",
    Q + "rit.nieuw": "<Patguh> Ik heb de Knabbelburgermobiel weer voor de deur gezet. Hij was een beetje kwijt. Net als ik.",
    # the stand-still game in De Ruige Kroeg
    G + "kroeg.titel": "Stilstaan!",
    G + "kroeg.regel": "Beweeg alleen als niemand kijkt. Rinkelt het glas? Sta stil!",
    G + "kroeg.balk": "Sleuteltje: nog %1$s blokken  |  Rustpunt %2$s/2  |  Gezien: %3$s keer",
    G + "kroeg.tink": "*tink tink* De barman tikt tegen zijn glas...",
    G + "kroeg.kijkt": "ZE KIJKEN!",
    G + "kroeg.kijkt_sub": "◉ ◉   sta stil...",
    G + "kroeg.weg_kijk": "Ze kijken weer weg. Vooruit!",
    G + "kroeg.gezien": "Wie bewoog daar?!",
    G + "kroeg.gezien_sub": "Terug naar je rustpunt. Nog een keer, njeg!",
    G + "kroeg.rustpunt": "Rustpunt %s/2! Zien ze je, dan begin je hier weer.",
    G + "kroeg.gehaald": "Het sleuteltje!",
    G + "kroeg.gehaald_sub": "Heel stil naar buiten...",
    G + "kroeg.weg": "Je bent De Ruige Kroeg uit. Het stilstaan-spel begint opnieuw als je weer binnenkomt.",
    # clicks on the cast during these steps (SponsguhWezen.BIJ_KLIK): <cast>.<step>
    G + "klik.sponsguh.0": "Meneer Krabguh heeft nieuws! Ik denk dat ik manager word. Ik heb mijn managersokken al aan.",
    G + "klik.sponsguh.1": "Ik ben niet verdrietig. Mijn oortjes doen gewoon even een dutje. Zullen we naar de ijssalon gaan?",
    G + "klik.sponsguh.2": "Hoor je die hoorn? Dat klinkt als een heel boze koning. Snel, naar het plein!",
    G + "klik.sponsguh.3": "De Knabbelburgermobiel staat voor de deur! Jij rijdt, ik zwaai.",
    G + "klik.sponsguh.4": "Stil zijn is moeilijk als je zo blij bent als ik. Maar ik doe mijn best. Psst!",
    G + "klik.patguh.0": "Krabguh heeft nieuws? Ik hoop dat het over ijs gaat. Het gaat meestal niet over ijs.",
    G + "klik.patguh.1": "IJS! In de Guhfy Guhber! Aan de Hoofdstraat! Of een andere straat! Kom op!",
    G + "klik.patguh.2": "Die hoorn komt van het plein. Of van mijn buik. Nee, van het plein.",
    G + "klik.patguh.3": "Ik mag voorin! Nee, achterin. Waar zit het stuur? Laat maar, jij rijdt.",
    G + "klik.patguh.4": "Ik oefen alvast met stilstaan. Ik ben er heel goed in. Ik doe het de hele dag.",
    G + "klik.meneer_krabguh.0": "Kom binnen, kom binnen! Iedereen moet het horen. Het gaat over GELD. Over méér geld!",
    G + "klik.meneer_krabguh.1": "Ga maar ijs eten met hem, collega. Maar reken het niet af met mijn knabbels.",
    G + "klik.octo_mika.0": "Wat het nieuws ook is: ik zucht alvast.",
    G + "klik.octo_mika.1": "Manager. Ik. Twee keer zoveel klanten om een hekel aan te hebben. Joepie.",
    G + "klik.octo_mika.2": "Een boze koning op het plein? Eindelijk eens iets interessants.",
    G + "klik.bevroren": "Meneer Krabguh zegt niets: hij is een ijsklontje. Alleen zijn ogen volgen je. En de knabbels in je zak.",
}

# =====================================================================================================================
# FTB: one quest per step, in the section einde2_sp_deel2 (the skelet names it): (key, title, text, icon, adv)
# =====================================================================================================================
FTB = [
    ("einde2_sp_d2_heen_k0", "De nieuwe manager",
     "Meneer Krabguh opent een tweede restaurant en zoekt een manager. Sponsguh weet het zeker: dat wordt hij. Toch?",
     "guhs:sponsguh_spawn_egg", "sponsguh_kroon_stap_1"),
    ("einde2_sp_d2_heen_k1", "Een ijsje in de Guhfy Guhber",
     "Patguh weet wat helpt tegen verdriet: de grootste ijscoupe van de hele zeepbel. Al mist er wel iets...",
     "guhs:patguh_spawn_egg", "sponsguh_kroon_stap_2"),
    ("einde2_sp_d2_heen_k2", "De kroon is weg!",
     "Koning Neptuguh is woedend: zijn kroon is gestolen, en Meneer Krabguh krijgt de schuld. Brrr.",
     "guhs:neptuguh_spawn_egg", "sponsguh_kroon_stap_3"),
    ("einde2_sp_d2_heen_k3", "De Knabbelburgermobiel",
     "Rij met Sponsguh en Patguh de stadspoort uit, over de weg naar De Ruige Kroeg. Jij zit achter het stuur!",
     "guhs:knabbelburger", "sponsguh_kroon_stap_4"),
    ("einde2_sp_d2_heen_k4", "De Ruige Kroeg",
     "Een stoere Mika heeft je sleuteltje. Sluip erheen, en sta stil zodra de Mika's opkijken. Wie was het baby'tje?",
     "minecraft:tripwire_hook", "sponsguh_kroon_stap_5"),
]
