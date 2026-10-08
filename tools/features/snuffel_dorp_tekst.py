"""
Het Snuffeleiland, slice snuffel-dorp: every Dutch text of the island's first series that is not a step of the questline
(those stand in snuffel.py, STAPPEN / KORT / EXTRA) and not a sign in the island (snuffel_dorp_bouw.py).

  GESPREKKEN   what the residents say: {"<wie>.<wat>": [page, page...]} -> quest.guhs.snuffeldorp.<wie>.<wat>.<i>
               (Java: feature/snuffeldorp/Gesprek.java; the number of pages travels in dorp.json). %s / %1$s = the
               arguments the role gives (mostly the name of the player's dog).
  SCENES       the four cutscenes: (title, {line key: text}, {actor: name})
  LANG         everything else: screen lines, scents, good deeds, the exam
"""

GESPREKKEN = {
    # --- Jutje Kwispel, at the strandpoort ---------------------------------------------------------------------------------
    "redder.welkom": [
        "Daar ben je, %s! Welkom in Snuffeldorp. Hier woont iedereen op vier poten, njeg. Ik ben Jutje Kwispel: ik jut het strand af en vind van "
        "alles. Vandaag vond ik jou!",
        "Je kwam uit zee gerold als een natte sok. Geen zorgen: dat je nu een hond bent, hoort bij het eiland. Thuis ben je weer gewoon jezelf, en "
        "al je spulletjes liggen daar veilig op je te wachten.",
        "Maar eerst naar Dokter Pleisterpoot. Zijn praktijk is het witte huis met het rode kruis, vlak bij het plein. Vertel hem maar alles. Hup, "
        "staart omhoog!",
    ],
    "redder.dag": ["Al iets moois gevonden? Ik vond vandaag drie schelpen, een laars en een hond. Die hond was jij, njeg."],
    "redder.klaar": ["Een spoor van je vader, ik hoorde het al! Als ik op het strand iets van hem vind, bewaar ik het voor je. Erewoord van een jutter."],
    # --- Dokter Pleisterpoot ------------------------------------------------------------------------------------------------
    "dokter.verhaal": [
        "Zo zo, een aangespoelde pup. Even kijken... neus nat, staart heel, vier poten. Kerngezond! Maar je kijkt zo bezorgd, %s. Vertel het de "
        "dokter maar.",
        "Je broertje of zusje is ziek... en je vader is al weken weg om de geneesbloem te zoeken? Hm. Ja. Die bloem bestaat echt. Hij groeit hier, "
        "diep op het Snuffeleiland.",
        "Maar je kunt hem niet ZIEN, alleen RUIKEN. Alleen een echte snuffelneus vindt hem. En jouw neus... tja. Dat is nog een mensenneus in een "
        "hondensnuit, njeg.",
        "Ga naar Meester Truffelneus, in de wei achter het poortje aan de noordkant van het plein. Hij maakt van elke pup een snuffelaar. Zeg maar "
        "dat Pleisterpoot je stuurt. En veeg je poten!",
    ],
    "dokter.dag": ["Neus nat, staart omhoog: dan komt alles goed. Doktersadvies! En snuffel elke dag iets nieuws."],
    "dokter.klaar": ["Een sjaal van je vader, bij het boompje! Dan is hij hier echt geweest. Rust goed uit, kleine snuffelpup. Het spoor loopt niet weg."],
    # --- Meester Truffelneus ------------------------------------------------------------------------------------------------
    "trainer.eerst_dokter": ["Hmm... jij ruikt naar zeewater en vragen. Loop eerst langs Dokter Pleisterpoot in het dorp. Daarna praten wij."],
    "trainer.les1": [
        "Aha, %s. Pleisterpoot stuurt je. Ik ben Meester Truffelneus, en ik rook je al aankomen toen je nog op het strand lag. Dat ga jij ook leren.",
        "Les één: HOUD DE SNUFFELTOETS INGEDRUKT. Je neus gaat naar de grond en op je scherm verschijnt de geurmeter. Hoe dichter bij de geur, hoe "
        "harder hij uitslaat.",
        "De kleur zegt wat je ruikt. Oranje is iets lekkers. Ik heb hier vlakbij een kluifje begraven. Slaat de meter helemaal uit? Dan sta je "
        "erop: graaf met de aanvalsknop. Zoek!",
    ],
    "trainer.les1_hint": ["Neus omlaag, staart omhoog! Houd de snuffeltoets ingedrukt en loop naar waar de oranje meter het hardst uitslaat. Het "
                          "kluifje ligt hier vlakbij in het gras. Dan graven!"],
    "trainer.les2": [
        "Een kluifje, en nog niet eens opgegeten. Knap! Dat was oranje: iets lekkers. Les twee: BLAUW. Blauw is een ding.",
        "Ik ben mijn fluitje kwijt. Het ligt ergens verder weg in de wei, richting de heg. Hoe verder weg, hoe zwakker de geur: loop gewoon de "
        "kant op waar de meter groeit. Zoek!",
    ],
    "trainer.les2_hint": ["Mijn fluitje ligt verder weg, aan de westkant van de wei, bij de heg. Loop met je neus aan de grond tot de blauwe meter "
                          "groeit. Slaat hij helemaal uit: graven!"],
    "trainer.les3": [
        "Mijn fluitje! Fwiet... hm, er zit zand in. Dank je. Les drie: GROEN. Groen is een dier. En niet alles ligt onder de grond: sommige geuren "
        "hangen ergens.",
        "In de grote eik aan de westkant van de wei wonen bijen. Ga er vlakbij staan en blijf snuffelen tot je de geur te pakken hebt. Niet "
        "graven, niet happen. Bijen happen terug, njeg.",
    ],
    "trainer.les3_hint": ["De bijen wonen in de grote eik aan de westkant van de wei. Ga er vlakbij staan en houd de snuffeltoets ingedrukt tot je "
                          "neus het weet. Graven hoeft niet."],
    "trainer.lessen_klaar": [
        "Lekkers, dingen, dieren: drie kleuren in je snuffelboekje, %s. De vierde kleur is paars: iets vreemds. Dat kan ik je niet leren. Dat ruik "
        "je vanzelf, op een dag.",
        "Hoor je dat? Gerammel en geblaf, op het plein! Er valt daar de hele week al van alles om en niemand ziet wie het doet. Ga jij eens "
        "kijken, met die verse neus van je.",
    ],
    "trainer.kabaal": ["Dat kabaal komt van het plein. Ga maar kijken bij de put, pup. Ik blijf hier: mijn oren zijn te lang om te rennen."],
    "trainer.daden": ["Echte snuffelaars helpen het dorp. Je hebt %s van de %s goede daden gedaan. Is het boompje van je maatje een jong boompje? "
                      "Dan kom je terug voor je examen."],
    "trainer.examen": [
        "Het boompje staat erbij als een plaatje, %s. Tijd voor het SNUFFELEXAMEN! Zakken bestaat niet: je neus mag er zo lang over doen als hij wil.",
        "Ik heb vier geuren verstopt in de wei en het dorp. Van elke kleur één: iets lekkers, een ding, een dier en... iets vreemds. Twee liggen "
        "begraven, twee hangen ergens.",
        "Op je scherm zie je hoeveel je er hebt. Heb je ze alle vier? Kom dan terug voor je diploma. Neus omlaag, staart omhoog. Zoek!",
    ],
    "trainer.examen_hint": ["Je hebt er %s van de %s. Iets lekkers ligt in de wei, mijn reservepet bij het weipoortje, het dier woont in de "
                            "moestuin van Tuinder Knolletje en het vreemde staat vlak bij de wegversperring. Zoek!"],
    "trainer.diploma": [
        "Alle vier! En niet één opgegeten! Ga zitten. Nee, blijf maar staan, je kwispelt te hard. Ahum.",
        "Hierbij verklaar ik, Meester Truffelneus, dat %1$s geslaagd is voor het snuffelexamen. Je bent vanaf nu officieel: %2$s. Vahoeg!",
        "Er zijn vijf rangen. Snuffelneus, de tweede, word je bij twintig geuren: dan mag je langs de wegversperring. Maar dat is voor later. "
        "Hé... je maatje wappert zo. Volgens mij wil het je iets laten zien bij het boompje.",
    ],
    "trainer.boom": ["Je maatje wil je iets laten zien bij het boompje, op het heuveltje. Ga maar kijken. En neem je neus mee."],
    "trainer.klaar": ["Een spoor van je vader, voorbij de wegversperring... Daar mag je pas door als Snuffelneus, rang twee van vijf. Blijf "
                      "snuffelen, pup. Elke nieuwe geur telt, njeg."],
    # --- Bakker Kruimelsnuit: the rolling pin ---------------------------------------------------------------------------------
    "bakker.hallo": ["Vers brood! Nou ja, bijna. Zonder deegroller wordt het eerder een bal. Kom later maar terug, pup, ik heb het druk met mopperen."],
    "bakker.vraag": [
        "Mijn deegroller is weg! Gisteren lag hij nog op de vensterbank, vanochtend: foetsie. Zonder deegroller bak ik alleen maar bolletjes. Het "
        "hele dorp eet al drie dagen bolletjes!",
        "Jij hebt toch snuffelles gehad? Hij ruikt naar deeg: iets lekkers, dus oranje. Zoek hem voor me, dan bak ik weer platte broden. Vahoeg!",
    ],
    "bakker.hint": ["Mijn deegroller ruikt naar vers deeg. Ik hoorde vannacht iets rollen, voorbij het schooltje, richting de vissershut. Daar "
                    "ergens in het gras, denk ik. Neus omlaag!"],
    "bakker.terug": ["Mijn deegroller! En er zit nog deeg aan! Eindelijk weer plat brood. Voor jou altijd een korstje, pup. Njeg!"],
    "bakker.dank": ["Plat brood, platte koek, platte alles. Dankzij jou! Wil je een korstje? O nee, wacht. Opgegeten. Sorry."],
    # --- Visser Natneus: his favourite float ----------------------------------------------------------------------------------
    "visser.hallo": ["Sst. De vissen slapen. Of ik. Een van de twee. Kom later terug, pup."],
    "visser.vraag": [
        "Mijn lievelingsdobber is weg. Rood met wit. Zonder dobber weet ik niet of ik beet heb. Ik zit hier al sinds dinsdag. Het is toch nog dinsdag?",
        "Hij ruikt naar vis. Alles van mij ruikt naar vis. Dat is groen op je meter: een dier. Zoek jij hem? Dan vang ik een vis voor je. Als ik "
        "beet heb.",
    ],
    "visser.hint": ["Mijn dobber dreef weg met het tij, naar het strandje ten zuiden van de haven. Volg de kust maar met je neus. Hij ruikt naar "
                    "vis, net als ik."],
    "visser.terug": ["Mijn dobber! Rood, wit en nog nat. Nu weet ik weer wanneer ik beet heb. Hé... ik heb beet! O nee. Zeewier."],
    "visser.dank": ["Dobber dobbert, visser vist. Zo hoort het. Dank je, pup. Sst, niet zo hard kwispelen, de vissen schrikken."],
    # --- Juf Blaffetje: the school bell ---------------------------------------------------------------------------------------
    "juf.hallo": ["Goedemorgen! Of is het al middag? Zonder schoolbel weet niemand hoe laat het is. Kom later maar terug, pup. Als ik weet wanneer "
                  "later is."],
    "juf.vraag": [
        "De schoolbel is weg! Nu begint de les nooit, en hij is ook nooit afgelopen. De pups zitten al sinds maandag in de pauze. Zij vinden het "
        "prachtig. Ik niet.",
        "Een bel is een ding, dus blauw op je meter. Hij ruikt naar koper en krijtjes. Zoek jij hem? Dan krijg je een tien voor snuffelen.",
    ],
    "juf.hint": ["Ik hoorde 's nachts gerinkel in de wei, helemaal achterin bij de vijver. De bel ruikt naar koper en krijtjes. Blauw op je meter!"],
    "juf.terug": ["TINGELING! O, wat heb ik dat gemist. De pauze is voorbij, pups! ...Ze zijn al weggerend. Een tien met een griffel voor jou."],
    "juf.dank": ["Tingeling! De les begint weer op tijd. De pups vinden dat minder leuk dan ik, njeg."],
    # --- Oma Wolletje: her ball of wool ---------------------------------------------------------------------------------------
    "oma.hallo": ["Dag lieverd. Wat ben jij dun. Heb je wel een sjaal? Kom later terug, dan brei ik er een. Als ik mijn wol kan vinden."],
    "oma.vraag": [
        "Mijn bol wol is weggerold, lieverd. De lila. Ik was bezig met een trui voor de kapitein. Hij heeft nu één mouw. Dat staat zo raar.",
        "Wol ruikt naar schaap, dus groen op je meter. Als jij hem vindt, brei ik voor jou ook iets. Vier sokjes. Voor elke poot één.",
    ],
    "oma.hint": ["Hij rolde de deur uit, het strandpoortje door, helemaal naar het zand aan de westkant van het strand. Wol ruikt naar schaap, "
                 "lieverd. Groen!"],
    "oma.terug": ["Mijn lila bol! Vol zand, maar dat klop ik er wel uit. Nu krijgt de kapitein zijn tweede mouw. En jij vier sokjes. Njeg, lieverd."],
    "oma.dank": ["De trui van de kapitein is af. Hij heeft per ongeluk drie mouwen. Maar het is het gebaar dat telt, lieverd."],
    # --- Tuinder Knolletje: his watering can ----------------------------------------------------------------------------------
    "tuinder.hallo": ["Niet op de worteltjes staan! En niet graven in mijn moestuin. Graven doen we buiten het hek. Kom later maar terug, pup."],
    "tuinder.vraag": [
        "Mijn gietertje is weg! Mijn knollen hebben dorst. Ik geef ze nu water met mijn hoed, maar die lekt. En dan heb ik een natte kop.",
        "Een gieter is een ding: blauw. Hij ruikt naar regenwater en een beetje naar knol. Vind jij hem? Dan krijg je de grootste wortel van de "
        "tuin. Om naar te kijken.",
    ],
    "tuinder.hint": ["Ik had hem het laatst in de wei, aan de oostkant, waar ik klaver pluk. Blauw op je meter, en hij ruikt naar regenwater."],
    "tuinder.terug": ["Mijn gietertje! Kom maar, knolletjes, drinken! En voor jou: de grootste wortel van de tuin. Nee, niet opgraven. Kijken."],
    "tuinder.dank": ["De knollen groeien als kool. En de kool groeit ook als kool. Dankzij jou en mijn gietertje, pup."],
    # --- Kleine Kwijlebal: his bouncy ball ------------------------------------------------------------------------------------
    "pup.hallo": ["Kef! Kef kef! Spelen? Ik heb een bal! O nee. Ik HAD een bal. Kef..."],
    "pup.vraag": [
        "Kef! Mijn stuiterbal is weg! Hij stuiterde en stuiterde en toen was hij WEG. Ik heb overal gekeken. Nou ja, hier. Ik heb hier gekeken.",
        "Hij is rood en hij zit vol kwijl. Van mij! Zoek je hem? Zoek je hem? Zoek je hem? Kef!",
    ],
    "pup.hint": ["Hij stuiterde het strandpoortje uit, naar het strand! Daar mag ik niet alleen heen. Hij zit vol kwijl, dus je ruikt hem zo. Kef!"],
    "pup.terug": ["MIJN BAL! Kef kef kef! Jij bent de beste hond van het hele eiland! Na mij. Spelen? Spelen!"],
    "pup.dank": ["Kef! Bal! Stuiter! Bal! Kef!"],
    # --- Kapitein Zoutsnoet, at his boat --------------------------------------------------------------------------------------
    "kapitein.vraag": ["Ahoi, landrot! Mijn bootje ligt klaar. Zal ik je terugvaren naar huis? Je komt precies uit waar je vandaan kwam, met al je "
                       "spulletjes. Het eiland loopt niet weg, en je neus ook niet, njeg."],
    "kapitein.vraag_klaar": ["Ahoi, snuffelpup! Naar huis varen? Met je Guhstation kom je terug wanneer je wilt. Ben je dat kastje kwijt? Zeg het "
                             "gerust, ik heb er nog een paar in het ruim."],
    "kapitein.station": ["Kapitein Zoutsnoet schuift je een nieuw Guhstation toe. Het gaat met je mee naar huis, njeg."],
    # (the island's music disc, for whoever finished the story before the disc existed: a line in the chat, once)
    "kapitein.plaat": ["Kapitein Zoutsnoet: Wacht eens, snuffelpup! Deze muziekplaat is voor jou. Ik draai hem al jaren op mijn boot en de "
                       "meeuwen zijn er helemaal klaar mee. Thuis in de jukebox ermee, njeg!"],
}

SCENES = {
    "snuffeldorp_wakker": ("Aangespoeld", {
        "golven": "Golven. Zand in je oren. En iets nats dat aan je snuit snuffelt...",
        "hallo": "Snuf snuf... Hé! Jij leeft nog! Njeg, wat ben jij nat.",
        "wakker": "Wakker worden, slaapkop! Je bent aangespoeld op het Snuffeleiland.",
        "pootjes": "Wacht eens... pootjes?! Een staart?! Je bent een HOND!",
        "kom": "Ik ben Jutje Kwispel. Kom mee naar het dorp, daar woont een dokter!",
    }, {"jutje": "Jutje Kwispel"}),
    "snuffeldorp_maatje": ("Wie doet dat toch?", {
        "rammel": "Rammel... rammel...",
        "kef": "Kef! De emmer! Hij beweegt vanzelf!",
        "alweer": "Alweer! Wie DOET dat toch? Ik zie helemaal niemand, njeg!",
        "alleen_jij": "Daar, boven de put... ziet niemand dat dan?",
        "zien": "Hihi... wacht. Jij kijkt naar mij. Kun jij mij ZIEN?!",
        "blijf": "Niemand ziet mij ooit! Ik blijf bij jou. Njeg!",
    }, {"pup": "Kleine Kwijlebal", "bakker": "Bakker Kruimelsnuit", "maatje": "Het bosgeestje"}),
    "snuffeldorp_spoor": ("Een spoor van papa", {
        "graaf": "Tussen de stenen ligt iets zachts...",
        "sjaal": "Een sjaal. Blauw met strepen. Hij ruikt naar... PAPA!",
        "spoor": "Papa is hier geweest! Zijn spoor loopt het eiland op, voorbij de wegversperring.",
    }, {}),
    "snuffeldorp_afvaart": ("Naar huis", {
        "los": "Trossen los! Staart binnenboord, njeg!",
    }, {"kapitein": "Kapitein Zoutsnoet"}),
}

G = "gui.guhs.snuffeldorp."
S = "gui.guhs.snuffel."
LANG = {
    G + "optie.oke": "Oké, njeg!",
    G + "optie.station": "Ik ben mijn Guhstation kwijt",
    G + "les_gevonden": "Gevonden! Ga terug naar Meester Truffelneus",
    G + "breng_terug": "Gevonden! Breng het terug naar %s",
    G + "examen_klaar": "Alle vier de geuren gevonden! Ga terug naar Meester Truffelneus voor je diploma. Vahoeg!",
    G + "daden_klaar": "Het boompje van je maatje is een jong boompje! Meester Truffelneus wacht op je in de wei: tijd voor je examen.",
    G + "versperring": "Hier mag je pas door als Snuffelneus",
    G + "wakker.1": "Je bent een hond op het Snuffeleiland! Loop over het pad naar het dorp: Jutje Kwispel wacht bij het strandpoortje.",
    G + "wakker.2": "Je eigen spulletjes liggen veilig thuis. Met de geheugenkaart in je balk ga je altijd terug naar huis.",
    G + "maatje.1": "%s: Hihi! Die emmer, dat was ik. En de deegroller. En de schoolbel. En... nou ja. Ik verstop graag dingen. Sorry, njeg.",
    G + "maatje.2": "%s: Zullen we alles samen terugbrengen? Van elke goede daad groeit mijn boompje in de wei! Als jij snuffelt, wijs ik de weg.",
    G + "bloesem.1": "%s: Kijk! Mijn boompje bloeit, dankzij jou. Dit bloesemtakje is voor jou. Het gaat met je mee naar huis.",
    G + "bloesem.2": "%s: Hé... ruik jij dat ook? Tussen de stenen bij het boompje. Iets vreemds. Snuffel eens!",
    G + "einde.1": "Een spoor van papa! Het loopt verder het eiland op, voorbij de wegversperring. Daar mag je pas door als Snuffelneus.",
    G + "einde.2": "%s: O ja, dit lag ook nog in mijn verstopplek. Een grijszwart kastje met een guh-snoet erop: een Guhstation! Zet het thuis neer "
                   "en je bent zo weer bij mij.",
    G + "einde.plaat": "%s: En deze muziekplaat moest ik je geven van Kapitein Zoutsnoet. Hij draait hem al jaren op zijn boot en de meeuwen "
                       "zijn er helemaal klaar mee. Thuis in de jukebox ermee, njeg!",
    G + "einde.3": "Het eerste verhaal van het Snuffeleiland is klaar. Je bent een echte Snuffelpup! Wordt vervolgd, njeg.",
    G + "doel.strandpoort": "Het strandpoortje van Snuffeldorp",
    G + "doel.dokter": "De praktijk van Dokter Pleisterpoot",
    G + "doel.wei": "De wei van Meester Truffelneus",
    G + "doel.plein": "Het pleintje van Snuffeldorp",
    G + "doel.boom": "Het boompje van je maatje",
    G + "doel.terug": "Een steigerhuisje (de boot naar het Snuffeleiland)",
    # the exam
    S + "examen.snuffelpup": "Het snuffelexamen",
    # the scents of the island's first series
    S + "geur.kluifje": "Een vers kluifje",
    S + "geur.fluitje": "Het fluitje van Meester Truffelneus",
    S + "geur.bijen": "Zoemende bijen",
    S + "geur.bosgeestje": "Een bosgeestje (mos en kattenkwaad)",
    S + "geur.deegroller": "Een deegroller vol deeg",
    S + "geur.dobber": "Een dobber die naar vis ruikt",
    S + "geur.schoolbel": "De schoolbel (koper en krijtjes)",
    S + "geur.bolwol": "Een lila bol wol",
    S + "geur.gietertje": "Een gietertje",
    S + "geur.stuiterbal": "Een stuiterbal vol kwijl",
    S + "geur.kaasknabbel": "Een verstopte kaasknabbel",
    S + "geur.tweedpet": "De reservepet van Meester Truffelneus",
    S + "geur.kippen": "Kakelende kippen",
    S + "geur.paddenstoel": "Een vreemde paddenstoel",
    S + "geur.papa_sjaal": "De sjaal van papa",
    # the good deeds
    S + "daad.bakker_deegroller": "de deegroller van Bakker Kruimelsnuit teruggebracht",
    S + "daad.visser_dobber": "de dobber van Visser Natneus teruggebracht",
    S + "daad.juf_schoolbel": "de schoolbel van Juf Blaffetje teruggebracht",
    S + "daad.oma_bolwol": "de bol wol van Oma Wolletje teruggebracht",
    S + "daad.tuinder_gietertje": "het gietertje van Tuinder Knolletje teruggebracht",
    S + "daad.pup_stuiterbal": "de stuiterbal van Kleine Kwijlebal teruggebracht",
}


def schrijf(h):
    from . import verhaal_motor
    for id, paginas in GESPREKKEN.items():
        for i, tekst in enumerate(paginas):
            h.lang(f"quest.guhs.snuffeldorp.{id}.{i}", tekst, tekst)
    for id, (titel, regels, namen) in SCENES.items():
        verhaal_motor.scene(h, id, titel, regels, namen)
    for key, tekst in LANG.items():
        h.lang(key, tekst, tekst)
