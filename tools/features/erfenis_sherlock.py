"""
1.9 De Erfenis: het Sherlock-verhaal (Java: feature/erfenis/sherlock). The one source of:

  - every text of the story (Dutch; en_us gets the same Dutch text): the eleven cases with their intro, cards, suspects'
    sweet protests, witnesses and solution, the courtroom (incl. the four cutscenes' lines), the Speurboekje/speurbord UI,
    the titles, the deco and reward names, Guhrrit's standard lines (also for the tutorial);
  - who says which dialogue line: data/guhs/erfenis/sherlock/regels.json ({"zaak.eieren.intro": ["vadson", "sherlock", ...]});
    "" = the narrator, a name ending in "!" = Morris-Mika shoving you aside (gently);
  - the looks of the cast: geo models on the sitting guh (Sherlock Guh with deerstalker, sleepy eyelids and a bubble pipe;
    Dokter Vadson with bowler, moustache and the Speurboekje; makelaar Guhrrit in his suit, and back from holiday with
    sunburn and a sombrero; Oehoe the cuddly owl; Edelachtbare Knabbelhamer with a huge wig and a kaasknabbel gavel) and on
    the Mika (Morris-Mika in a shiny grey suit with tie and briefcase; Pim the intern with a badge, a bow and a sprout),
    textures, Sherlock's bubble animation and the Mikas' animations;
  - the item sherlock_vergrootglas (the look of the existing detective_vergrootglas).

Standalone (from the worktree root): PYTHONPATH="tools;tools/features" python tools/features/erfenis_sherlock.py
"""
import copy
import json
import os
import sys

import numpy as np
from PIL import Image

# =====================================================================================================================
# the texts
# =====================================================================================================================
WIE = {
    "sherlock": "Sherlock Guh", "vadson": "Dokter Vadson", "guhrrit": "Makelaar Guhrrit", "guhrrit_vakantie": "Makelaar Guhrrit",
    "makelaar": "Makelaar Guhrrit", "morris": "Morris-Mika", "pim": "Pim", "oehoe": "Oehoe", "knabbelhamer": "Edelachtbare Knabbelhamer",
    "verteller": "Het Speurboekje", "bonnie": "Boerin Bonnie", "meelmuts": "Molenaar Meelmuts", "wolletje": "Wolletje",
    "koos": "Koos van De Vadse Koe", "bouwer": "Bouwer Bram", "oma": "Oma Breiguh", "hendrik": "Hendrik de Haan", "eendje": "Een eendje",
    "babyguhs": "De babyguhs", "spook": "Het spook", "postguh": "De postguh", "schaapje": "Een schaapje", "konijntjes": "De konijntjes",
    "bertha": "Bertha de koe", "muzikanten": "De muzikanten", "glimworm": "De glimwormguhtjes",
}

# dialogue groups: a list of (speaker, text); "" = narrator, "morris!" = Morris shoves you aside
ALGEMEEN = {
    "welkom": [
        ("sherlock", "Goedemiddag! Sherlock Guh, beroemd speurneus. Dat zeg ik zelf, maar het klopt ook. Njeg."),
        ("vadson", "En ik ben Dokter Vadson! Ik schrijf alles op in het Speurboekje. ALLES. Ook wat we lunchen."),
        ("sherlock", "Dit dorp ruikt naar mysterie. En naar koek. Vooral naar mysterie... zzz..."),
        ("vadson", "Hij slaapt. Dat doet hij vaak. Hier, neem zijn vergrootglas maar. Houd het vast in je dorp, dan glinsteren de sporen!"),
        ("vadson", "Zodra er een zaakje is, ligt het in de brievenbus van het bureau. Vahoeg, wat spannend!"),
    ],
    "proloog": [
        ("", "Tussen de brandnetels glimt iets: een roestig sleuteltje, met een piepklein kaasje als hanger."),
        ("", "Waar zou het van zijn? Je stopt het in je Vondstenkistje. Misschien weet iemand het later..."),
    ],
}

ZAKEN = [
    dict(id="eieren", titel="De verdwenen eieren",
         kort="Het kippenhok levert al twee dagen geen eieren meer op. Waar zijn ze gebleven?",
         wacht="bouw het Detectivebureau (kan vanaf Hoeve level 3)",
         intro=[("vadson", "Sherlock! SHERLOCK! Het kippenhok heeft al twee dagen geen enkel eitje gelegd!"),
                ("sherlock", "Hmm. Geen eieren. Dat is... eierloos. Heel verdacht."),
                ("sherlock", "Speurneus, neem het vergrootglas en zoek rond het kippenhok. Ik denk ondertussen heel hard na. Met mijn ogen dicht."),
                ("vadson", "En vraag Boerin Bonnie wat ze gezien heeft! Zij ziet alles. Echt alles. Het is een beetje eng.")],
         kaarten={"rood_veertje": ("Een rood veertje", "Een veertje! Een VEERTJE, Sherlock! En nog rood ook. Welke kip is er nou rood?"),
                  "pootafdrukjes": ("Drietenige pootafdrukjes", "Drie teentjes, op weg naar de eendenvijver. Iemand heeft hier heel vrolijk gehuppeld."),
                  "hartjesschaal": ("Een eierschaal met een hartje", "Er staat een hartje op getekend. Met een snavel, zo te zien. Hoe romantisch, njeg!"),
                  "stagiairspasje": ("Een pasje: 'Pim, stagiair'", "Een pasje van Morris-Mika Projectontwikkeling. Wat deed Pim bij ons hekje?"),
                  "bonnie_zuchten": ("Getuige: Boerin Bonnie", "Bonnie hoorde 's nachts iemand zuchten. 'Heel romantisch zuchten', zegt ze.")},
         fout={"pim": "Ik? Ik kan niet eens een ei tillen, njeg. Ik stond alleen bij het hek omdat de bloemetjes daar zo mooi zijn...",
               "eendje": "Kwak?! Kwak kwak! (Het eendje is diep beledigd en zwemt een boos rondje.)"},
         getuigen={"bonnie": dict(hallo="Speurneus! Kom je voor de eieren? Ik zie alles, hoor. ALLES.",
                                  gezien="'s Nachts hoorde ik iemand zuchten bij de vijver. Heel romantisch zuchten. En iemand die 'kukeleku' fluisterde. Heel zachtjes.",
                                  waar="Ik? Ik lag in mijn bedje, met één oog open. Zoals altijd. Daarom zie ik alles.",
                                  toon="Oh, dat ken ik! Dat heb ik ook al gezien. Ik zie alles, weet je nog?")},
         oplossing=[("vadson", "Hendrik de Haan?! Maar waarom zou een haan eieren pakken?"),
                    ("sherlock", "Volg de pootjes naar het riet, Vadson. Zoals ik al zei... zzz... romantiek."),
                    ("", "In het riet bij de vijver ligt een enorm hart van eitjes. Ernaast zit een eendje, met hele grote ogen."),
                    ("hendrik", "Kukeleku betekent 'ik hou van je' in het eends. Denk ik."),
                    ("vadson", "Wat lief! En kijk, onder het hart ligt een oud labeltje: 'Eigendom van Opa Knabbel'..."),
                    ("vadson", "Opa Knabbel? Wie is Opa Knabbel? Ik schrijf het op. Met een vraagteken. Een GROOT vraagteken.")]),
    dict(id="kruimels", titel="Het kruimelspoor",
         kort="De eerste lading koek is weg. Er staan alleen nog lege bakblikken.",
         wacht="bouw een Bakkerij",
         intro=[("vadson", "Sherlock, ramp! De allereerste koek uit de bakkerij is verdwenen! Alleen de bakblikken zijn er nog."),
                ("sherlock", "Lege bakblikken... dat betekent dat de koek er niet meer in zit. Noteer dat, Vadson."),
                ("vadson", "Genoteerd! Speurneus, er loopt een kruimelspoor over het pad. Volg het met je vergrootglas!")],
         kaarten={"kruimelspoor": ("Een kruimelspoor", "Kruimels! Ze lopen van de bakkerij naar... de speeltuin? Heel kleine hapjes."),
                  "handjes_in_meel": ("Piepkleine handjes in het meel", "Handafdrukjes in het meel. Zó klein. Wie heeft er nou zulke kleine handjes?"),
                  "theeservies": ("Een klein theeservies", "Achter de speeltuin: een poppentheeservies, met kruimels op elk bordje."),
                  "meelzak": ("Een opengescheurde meelzak", "Op de zak staat 'Meelmuts'. Hij is open. Maar meel is toch geen koek?"),
                  "meelspoken": ("Getuige: Molenaar Meelmuts", "Meelmuts zag 's nachts twee hele kleine meelspoken bij de speeltuin.")},
         fout={"meelmuts": "Ik? Ik eet alleen meel. Rauw. Met een lepeltje. Dat is gezond, toch? ...Toch?",
               "pim": "Koek? Ik mag geen koek van Morris. 'Koek is niet efficiënt', zegt hij. Snif."},
         getuigen={"meelmuts": dict(hallo="Hatsjoe! Sorry, ik zit altijd onder het meel. Wat wil je weten, speurneus?",
                                    gezien="Gisternacht zag ik twee hele kleine meelspoken! Ze liepen naar de speeltuin en giechelden. Spoken giechelen niet, toch?",
                                    waar="Ik stond in de molen. Ik sta altijd in de molen. Ik ben de molenaar. Het staat op mijn muts.",
                                    toon="Hmm, dat is meel. Mijn meel! Ik ken elk korreltje persoonlijk.")},
         oplossing=[("vadson", "De babyguhs?! Maar die zijn nog zo klein!"),
                    ("", "Achter de speeltuin zitten twee babyguhs aan een piepklein tafeltje, met hun knuffels en de hele koek."),
                    ("babyguhs", "Het was een héél belangrijke vergadering, njeg."),
                    ("sherlock", "Een theekransje. Zoals ik al zei. Zeg, is er nog een stukje over?"),
                    ("vadson", "Kijk, de laatste kruimels verdwijnen in een kier in de vloer van het oude schuurtje. Als je erop tikt, klinkt het hol..."),
                    ("vadson", "Hol! Er zit iets ONDER het schuurtje! Ik schrijf het op. Met drie uitroeptekens.")]),
    dict(id="spook", titel="Het spook in het schuurtje",
         kort="'s Nachts kraakt en fluistert het in het schuurtje. De bouwer weigert te werken: 'Er spookt het!'",
         wacht="verbeter de Hoeve naar level 4",
         intro=[("vadson", "Sherlock! De bouwer wil het schuurtje niet meer verbouwen. Hij zegt dat het er spookt!"),
                ("sherlock", "Spoken bestaan niet, Vadson. Behalve als ze wel bestaan. Dan bestaan ze wel."),
                ("morris!", "Pardon, pardon, aan de kant. Ik moet even... opmeten. Niets aan de hand. Doorlopen."),
                ("vadson", "Wie was DAT? Hij duwde je gewoon opzij! In een glimmend pak!"),
                ("sherlock", "Een guh met een rolmaat. Interessant. Speurneus, zoek in en rond het schuurtje. Ik let op... zzz...")],
         kaarten={"uilenveertjes": ("Zachte veertjes op een luik", "Er zit een LUIK in de vloer! Met donzige veertjes erop. Spoken hebben toch geen veren?"),
                  "grijs_dons": ("Grijs dons op de vensterbank", "Heel zacht grijs dons. Net een klein spookjasje."),
                  "luik_slotje": ("Een luik met een slotje", "Een slotje met een kaasje erop... Wacht. Je roestige sleuteltje past! Klik!"),
                  "schoenafdrukken": ("Grote gepoetste schoenafdrukken", "Enorme schoenen. Glimmend gepoetst. Van een Mika! Maar spoken dragen toch geen schoenen?"),
                  "hoe_hoe": ("Getuige: Bouwer Bram", "Bram hoorde 's nachts gefluister. 'Het klonk als hoe, hoe', zegt hij.")},
         fout={"morris": "Ik? Een spook? Belachelijk. Ik ben projectontwikkelaar. Ik ontwikkel projecten. Hier is mijn kaartje.",
               "pim": "Ik? Ik ben bang in het donker. Ik slaap met een nachtlampje in de vorm van een worteltje."},
         getuigen={"bouwer": dict(hallo="Ik ga daar niet meer naar binnen, hoor. Echt niet. Ik ben een stoere bouwer, maar ik ben niet gek.",
                                  gezien="'s Nachts hoorde ik gekraak. En gefluister! Het klonk als: 'hoe, hoe'. Zo praten spoken, toch?",
                                  waar="Ik zat in mijn bouwkeet. Met de deur op slot. En de gordijntjes dicht.",
                                  toon="Dat? Brrr. Doe dat maar weg. Of nee, geef maar. Nee, toch maar niet.")},
         oplossing=[("", "Je doet het luik open. In het donker gaan twee grote ronde ogen open."),
                    ("oehoe", "Hoe, hoe, hoe kom jij aan dat sleuteltje?"),
                    ("vadson", "Het spook is een UIL! Een knuffel-uil!"),
                    ("oehoe", "Ik ben Oehoe. Ik was de beste vriendin van Opa Knabbel. Ik bewaak dit luik al jaren. Hoe dan ook."),
                    ("sherlock", "Zoals ik al zei: een spook met veren. Maar wie liep er dan op die grote schoenen?"),
                    ("vadson", "Kijk, een folder in het gras: 'Binnenkort: KNABBELPARKING. 400 plekken. 0 bloemetjes.' Van Morris-Mika!"),
                    ("oehoe", "Het luik blijft nog dicht, hoor. Het zit vol wortels. Wie zoekt onder het oudste, vindt het nieuwste.")]),
    dict(id="wol", titel="De verdwenen wol",
         kort="Elke nacht verdwijnt er een klein beetje wol uit de opslag. Heel netjes, dat wel.",
         wacht="bouw een Weverij en lever 10 bestellingen",
         intro=[("vadson", "Sherlock, er verdwijnt wol uit de opslag! Elke nacht een klein beetje. Heel netjes, dat wel."),
                ("sherlock", "Netjes stelen... dat is bijna lief. Speurneus, volg de wol."),
                ("vadson", "En vraag Wolletje van de schaapjes wat ze weet! Al vergeet ze meestal alles.")],
         kaarten={"wolspoor": ("Een wolletje over de wei", "Een draadje wol, helemaal van de opslag naar het karrenplein. Wie loopt er nou met wol naar de karretjes?"),
                  "breinaalden": ("Breinaalden onder een karretje", "Twee breinaalden! Met een roze bolletje erop. Iemand breit er stevig op los."),
                  "krullerig_regeltje": ("Een bestelbrief met een extra regel", "Onderaan de bestelling, in krullerig handschrift: 'En een beetje wol, schatje.'"),
                  "schapenwol_hek": ("Schapenwol aan het hek", "Een plukje wol aan het hek. Het schaapje kijkt heel onschuldig. Té onschuldig?"),
                  "welke_wol": ("Getuige: Wolletje", "Wolletje weet niks meer van wol. Maar wel van een oma in een karretje...")},
         fout={"postguh": "Ik bezorg alleen maar! Wat er in mijn karretje zit, gaat mij niet aan. Ook als het breit.",
               "wolletje": "Welke wol? Ik heb geen wol. Oh wacht, ik pas op de schaapjes. Dan heb ik wel wol. Welke wol?",
               "schaapje": "Bèèèh! (Het schaapje laat zien dat al haar wol er nog aan zit. Allemaal.)"},
         getuigen={"wolletje": dict(hallo="Hallo! Wie ben jij ook alweer? Oh ja, de speurneus! Of was het de bakker?",
                                    gezien="Welke wol? ...Oh! Wacht. Ik zag wel een lieve oma in het bestelkarretje zitten. Ze zei 'dag schatje' en breide maar door.",
                                    waar="Ik was bij de schaapjes. Denk ik. Of bij de eendjes. Ergens met dieren.",
                                    toon="Oh, wat mooi! Wat is het? Heb ik dat gemaakt? Nee? Jammer.")},
         oplossing=[("vadson", "Een oma? In het bestelkarretje?"),
                    ("", "Uit het karretje van het buurdorp klimt Oma Breiguh, met een sjaal van wel twintig meter lang."),
                    ("oma", "Ik heb het niet gestolen, schatje, ik heb het vóóruit geleend."),
                    ("oma", "Het is een reuzensjaal voor het dorpsfeest. Voor de muziektent! Of voor een hele lange guh."),
                    ("oma", "Weet je, ik kende Opa Knabbel. Hij gaf alles weg voor een knabbel. Hij zei altijd: een hoeve is pas iets waard als iemand ervan houdt."),
                    ("sherlock", "Zoals ik al zei: breien. Vadson, schrijf op: Opa Knabbel. Alweer die naam.")]),
    dict(id="kelder", titel="De kelder van Opa Knabbel",
         kort="Onder het schuurtje ligt een oude kaaskelder met Opa Knabbels dagboek. Maar de laatste bladzijden zijn eruit gescheurd!",
         wacht="verbeter de Hoeve naar level 5",
         intro=[("oehoe", "Hoe, hoe! De wortels zijn weg. Het luik gaat open. Kom maar, speurneus."),
                ("", "Onder het schuurtje ligt een gezellige oude kaaskelder. Op een tafeltje ligt een dik dagboek: 'Opa Knabbel'."),
                ("vadson", "Zijn dagboek! Maar kijk... de laatste bladzijden zijn eruit gescheurd!"),
                ("sherlock", "Gescheurd. Dat is het tegenovergestelde van geplakt. Zoek de bladzijden, speurneus.")],
         kaarten={"geknabbelde_randjes": ("Geknabbelde papierrandjes", "De randjes zijn niet gescheurd. Ze zijn geknabbeld! Met kleine tandjes."),
                  "angora_pluisjes": ("Konijnenpluisjes op de trap", "Superzacht pluis op de kelderstoep. Konijnenpluis! Dat komt toch van..."),
                  "papier_in_ren": ("Plukjes papier in de konijnenren", "Papiersnippers in het hooi van de konijnenren. Heel zacht opgevouwen."),
                  "visitekaartje": ("Een grijs visitekaartje", "'Morris-Mika Projectontwikkeling: wij maken van groen grijs.' Was hij hier?!"),
                  "knabbel_knabbel": ("Getuige: Oehoe", "Oehoe sliep. Maar ze hoorde wel heel veel geknabbel. Knabbel knabbel, de hele dag.")},
         fout={"morris": "Ik? Bewijs laten verdwijnen? Ik heb mijn kaartje alleen even laten liggen. Per ongeluk. Expres. Per ongeluk.",
               "pim": "Ik kan helemaal niet lezen in het donker! En ik ben bang van kelders. En van dagboeken."},
         getuigen={"oehoe": dict(hallo="Hoe, hoe... gaap. Je maakt me wakker, speurneus.",
                                 gezien="Ik sliep. Uilen slapen overdag, weet je wel. Maar ik hoorde wel heel veel geknabbel. Knabbel knabbel, de hele dag.",
                                 waar="In mijn nestje boven het luik. Waar anders? Hoe dan ook.",
                                 toon="Hoe... dat ruikt naar wortel. En naar hooi.")},
         oplossing=[("", "In de konijnenren ligt het zachtste nestje ooit: helemaal van dagboekbladzijden. Drie konijntjes slapen erin."),
                    ("vadson", "Dus het bewijsmateriaal is... knus geworden?"),
                    ("konijntjes", "(De konijntjes knipperen. Eentje geeuwt. Ze vinden het een heel fijn nestje.)"),
                    ("sherlock", "Zoals ik al zei: knabbelaars. Laat ze maar liggen, op één bladzijde na. Lees maar voor, Vadson."),
                    ("vadson", "'Mijn hoeve gaat naar wie er lief voor zorgt, voor de prijs van één knabbel. Niet aan iemand met een stropdas.' Een TESTAMENT!"),
                    ("vadson", "Het was dus geen oplichterij! Het was de wens van Opa! Maar... de allerlaatste bladzijde, met zijn handtekening, ontbreekt.")]),
    dict(id="koeienbel", titel="De gestolen koeienbel",
         kort="Net voor het eerste dorpsfeest is de bel van café De Vadse Koe verdwenen!",
         wacht="bouw een Muziektent",
         intro=[("vadson", "Sherlock, het eerste dorpsfeest! Muziek! Taart! En... de koeienbel van café De Vadse Koe is weg!"),
                ("sherlock", "Een feest zonder bel is een... feest. Maar dan zonder bel. Erg."),
                ("vadson", "Koos van het café is helemaal overstuur. Speurneus, zoek bij de muziektent!")],
         kaarten={"hoefafdrukjes": ("Hoefafdrukjes op het podium", "Hoefjes! Op het podium! Iemand heeft hier... gedanst?"),
                  "bladmuziek": ("Bladmuziek met kauwsporen", "De muziek is half opgegeten. Het liedje heet 'Boe, boe, baby'."),
                  "emmer_podium": ("Een emmer: 'MIJN PODIUM'", "Op de emmer staat met krijt: 'MIJN PODIUM'. In hoefjeshandschrift."),
                  "folders": ("Een stapel folders", "'Binnenkort: Knabbelparking!' Pim heeft ze overal neergelegd. Maar een bel?"),
                  "vals_met_gevoel": ("Getuige: Koos", "Koos zegt dat Bertha de koe al weken 's nachts oefent in de wei. Vals, maar met gevoel.")},
         fout={"pim": "Ik moest folders uitdelen van Morris. Ik heb er wel een bloemetje op getekend. Stiekem.",
               "muzikanten": "Wij? Wij hebben onze eigen bel! Tring! Zie je wel? Nou ja, het is een fietsbel."},
         getuigen={"koos": dict(hallo="Welkom bij De Vadse Koe! Wil je iets drinken? Of een roddeltje?",
                                gezien="Bertha de koe oefent al weken 's nachts in de wei. Vals, maar met gevoel. Ze droomt van het podium, als je het mij vraagt.",
                                waar="Ik stond achter de bar. Ik sta altijd achter de bar. Van daar hoor je alles.",
                                toon="Oei. Dat is een kauwspoor. Ik ken maar één iemand die bladmuziek eet.")},
         oplossing=[("", "Achter de muziektent staat Bertha de koe, met de bel als microfoon voor haar snoet. Ze zingt. Heel hard."),
                    ("bertha", "Boe. Dat is Frans voor 'ik ben een artiest'."),
                    ("sherlock", "Zoals ik al zei: talent. Laat haar maar optreden, Vadson."),
                    ("morris!", "Stop de muziek! Luister allemaal: ik heb een BOUWVERGUNNING. Over één maand komt hier asfalt!"),
                    ("", "Morris stoot per ongeluk de feesttaart om. Gelukkig landt die op een groot zacht kussen."),
                    ("vadson", "Een bouwvergunning voor de Knabbelparking... Speurneus, dit wordt ernstig.")]),
    dict(id="paaltjes", titel="De verschoven paaltjes",
         kort="'s Ochtends staan de grenspaaltjes van je perceel twee vakjes naar binnen. Je dorp is ineens kleiner!",
         wacht="verbeter de Hoeve naar level 7",
         intro=[("vadson", "Sherlock! De grenspaaltjes zijn verschoven! Ons dorp is twee vakjes kleiner geworden!"),
                ("sherlock", "Kleiner? Dat is het tegenovergestelde van groter. Heel slecht."),
                ("vadson", "Speurneus, loop langs de randen van het perceel. Daar staan de paaltjes!")],
         kaarten={"schouderafdrukje": ("Een klein schouderafdrukje", "Iemand heeft met zijn schoudertje tegen het paaltje geduwd. Een héél klein schoudertje."),
                  "touw_strikje": ("Een touw met een strikje", "Om het paaltje zit een touw met een keurig roze strikje. Zodat het minder gemeen is?"),
                  "moestuinboekje": ("Een moestuinboekje", "Een boekje vol tekeningen van worteltjes. Voorop staat: 'Van Pim'."),
                  "plattegrond": ("Een plattegrond met nieuwe grenzen", "Een kaart van ons dorp. Met nieuwe grenzen, in het handschrift van Morris!"),
                  "kleintje_huilde": ("Getuige: Oehoe", "Oehoe zag 's nachts een kleintje bij de paaltjes. 'Hij huilde een beetje', zegt ze.")},
         fout={"morris": "Ik heb niets verschoven. Ik verschuif nooit iets zelf. Daar heb ik... stagiairs voor.",
               "bouwer": "Ik? Als ik paaltjes verzet, staan ze recht. Deze staan scheef. Met een strikje. Niet mijn stijl."},
         getuigen={"oehoe": dict(hallo="Hoe, hoe. Ik zag het, speurneus. Uilen zien alles in het donker.",
                                 gezien="Een kleintje. Hij duwde de paaltjes met zijn schoudertje. En hij huilde een beetje. Hoe zielig.",
                                 waar="Op mijn tak. Ik zit altijd op mijn tak. Ik heb er een kussentje op gelegd.",
                                 toon="Hoe... dat strikje. Zo'n strikje heb ik eerder gezien. Op een klein Mika'tje.")},
         oplossing=[("pim", "Ik was het... Morris zei dat ik het moest doen. Ik heb er een strikje omheen gedaan, zodat het minder gemeen is."),
                    ("pim", "Ik wilde eigenlijk alleen maar een worteltje kweken, njeg."),
                    ("vadson", "Ooh, Pim. Kom maar. Je mag hier een moestuintje hebben. Een échte!"),
                    ("pim", "Echt? Dan vertel ik je alles. Morris wil de verkoop 'ongeldig' laten verklaren. Want 'één knabbel is geen echte prijs', zegt hij."),
                    ("sherlock", "Zoals ik al zei: een strikje. Speurneus, we hebben een verklikker.")]),
    dict(id="licht", titel="Licht in de kelder",
         kort="'s Nachts flikkert er licht in Opa's kelder, en er verdwijnen kaarsjes.",
         wacht="bouw een Kaarsenmakerij",
         intro=[("vadson", "Sherlock, er flikkert 's nachts licht in Opa's kelder! En er verdwijnen kaarsjes uit de kaarsenmakerij!"),
                ("sherlock", "Licht in het donker... dat is precies waar licht hoort. Toch verdacht."),
                ("vadson", "Speurneus, zoek rond de kelder. En Bonnie heeft vast weer iets gezien!")],
         kaarten={"glinsterstof": ("Glinsterstof op de trap", "Groen glinsterstof. Het licht een beetje op. Of verbeeld ik me dat?"),
                  "gesmolten_was": ("Een spoor van gesmolten was", "Gesmolten kaarsvet, in piepkleine druppeltjes. Warm, knus, en heel klein."),
                  "zoemende_honingpot": ("Een honingpot die zoemt", "De pot zoemt! Maar er zitten geen bijtjes in. Wel... lichtjes?"),
                  "uilenveertje_trap": ("Een uilenveertje op de keldertrap", "Een veertje van Oehoe! Is zij 's nachts in de kelder geweest?"),
                  "groene_lichtjes": ("Getuige: Boerin Bonnie", "Bonnie zag groene lichtjes. 'Heel kleine. Héél veel', zegt ze.")},
         fout={"oehoe": "Hoe, hoe vaak moet ik het nog zeggen? Ik BEWAAK de kelder. Ik ga er niet in feesten.",
               "pim": "Ik slaap met een nachtlampje, ja. Maar dat is een worteltje. Geen kaarsje."},
         getuigen={"bonnie": dict(hallo="Speurneus! Ik heb weer iets gezien. Natuurlijk. Ik zie alles.",
                                  gezien="Groene lichtjes! Heel kleine. Héél veel. Ze dansten de kelder in, met kaarsjes op hun rug.",
                                  waar="Ik lag in mijn bedje. Met één oog open, zoals altijd.",
                                  toon="Oh, dat glinstert net als die lichtjes! Zie je wel, ik zei het toch.")},
         oplossing=[("", "In de kelder, rond een kaarsje, zit een hele familie glimwormguhtjes. Ze zijn er knus komen wonen."),
                    ("sherlock", "Zoals ik al zei... lichtjes. (Hij wordt net wakker.)"),
                    ("vadson", "Wacht... in het groene licht wordt er iets zichtbaar op de keldermuur! Onzichtbare honing-inkt!"),
                    ("", "'De laatste bladzijde ligt onder mijn oudste boompje.' (Opa Knabbel)"),
                    ("vadson", "Het oudste boompje! Dat staat in het laatste overwoekerde stuk van het dorp!")]),
    dict(id="blikje", titel="Het blikje onder het boompje",
         kort="Onder het oudste boompje graaf je een oud kaasblikje op. Maar het is leeg!",
         wacht="verbeter de Hoeve naar level 8",
         intro=[("", "Onder het oudste boompje graaf je een oud kaasblikje op. Je doet het deksel open..."),
                ("vadson", "LEEG?! Het blikje is leeg! Iemand is ons voor geweest!"),
                ("sherlock", "Een leeg blikje is een blikje zonder inhoud, Vadson. Er is een dief. Of een lief iemand. Zoek, speurneus.")],
         kaarten={"veertjes_boompje": ("Uilenveertjes bij het boompje", "Weer veertjes. Zacht en donzig. Iemand is hier geland."),
                  "nesttakje": ("Een nesttakje met papiersnippers", "Een takje uit een nest, met een snippertje oud papier eraan."),
                  "ansichtkaart": ("Een ansichtkaart van de makelaar", "Van makelaar Guhrrit, op vakantie: 'Goed op je spulletjes letten, hè!'"),
                  "glimmend_knoopje": ("Een glimmend knoopje", "Een knoopje van een glimmend pak. Van Morris! Hij is hier geweest!"),
                  "iets_wits": ("Getuige: Pim", "Pim zag Oehoe wegvliegen met iets wits in haar snavel. Vlak voordat Morris kwam graven.")},
         fout={"morris": "Ja, ik was hier. Ik ben uitgegleden. Mijn knoopje is eraf. Maar het blikje was al leeg! Hmpf.",
               "pim": "Ik? Ik graaf alleen naar worteltjes! En die zitten niet in blikjes.",
               "makelaar": "(Een kaartje uit de zon:) 'Ik? Ik lig op het strand, schat! Maar let goed op je spulletjes, hè!'"},
         getuigen={"pim": dict(hallo="Hoi speurneus! Ik ben nu jouw verklikker. Ik heb zelfs een verklikkershoedje. Nou ja, een emmertje.",
                               gezien="Ik zag Oehoe wegvliegen met iets wits in haar snavel! Vlak daarna kwam Morris met een schep. Hij gleed uit. Hihi.",
                               waar="In mijn moestuintje. Mijn worteltjes zijn al zó groot. Wil je ze zien?",
                               toon="Oh! Dat knoopje is van Morris. Hij was er heel boos over. Hij heeft er nog maar zes.")},
         oplossing=[("oehoe", "Hoe, hoe vaak moet ik het nog zeggen? Ik BEWAAK dingen."),
                    ("oehoe", "Toen die Mika kwam graven, heb ik de bladzijde in mijn nestje verstopt. Hier. Hij is van jou."),
                    ("vadson", "De laatste bladzijde! Met Opa Knabbels handtekening! Een pootafdrukje!"),
                    ("sherlock", "Zoals ik al zei. We hebben alles. Nu nog... zzz..."),
                    ("", "Dan valt er een rode brief in de bus. Op de envelop staat: DAGVAARDING."),
                    ("vadson", "Morris-Mika daagt ons voor de rechter! Speurneus, we moeten onze zaak voorbereiden!")]),
    dict(id="voorbereiding", titel="De voorbereiding",
         kort="Morris-Mika daagt je voor de rechter. Verzamel de getuigenissen van iedereen die Opa Knabbel kende.",
         wacht="los eerst Het blikje onder het boompje op",
         intro=[("vadson", "Een rechtszaak! Speurneus, we hebben getuigen nodig. Iedereen die iets weet over Opa Knabbel."),
                ("sherlock", "Praat met Oehoe, Oma Breiguh, Pim, Hendrik de Haan... en de makelaar, als hij ooit terugkomt."),
                ("guhrrit_vakantie", "Ik ben terug! Wat heb ik gemist? Een rechtszaak? Wat leuk!"),
                ("vadson", "Hij heeft een zonnebrand. En een sombrero. Ik schrijf het op.")],
         kaarten={"oehoe_bewaakt": ("Getuige: Oehoe", "Oehoe bewaakte de laatste bladzijde, op verzoek van Opa Knabbel zelf."),
                  "oma_kende_opa": ("Getuige: Oma Breiguh", "Oma Breiguh: 'Opa Knabbel wilde zijn hoeve aan iemand liefs geven. Dat zei hij elke dag.'"),
                  "pim_biecht": ("Getuige: Pim", "Pim vertelt alles over de plannen van Morris. Met strikjes en al."),
                  "hendrik_komt_graag": ("Getuige: Hendrik de Haan", "Hendrik heeft niks gezien. Maar hij komt graag. Met zijn eendje."),
                  "guhrrit_proefde": ("Getuige: Makelaar Guhrrit", "Guhrrit voerde het testament uit. En hij weet iets over die kaasknabbel...")},
         fout={},
         getuigen={"oehoe": dict(hallo="Hoe, hoe. Een rechtszaak? Ik getuig graag. Ik heb er zelfs een strikje voor om.",
                                 gezien="Opa Knabbel gaf mij de laatste bladzijde. 'Bewaak hem goed, Oehoe', zei hij. En dat deed ik. Hoe dan ook.",
                                 waar="Hier. Altijd hier. Ik bewaak dingen, weet je nog?"),
                   "oma": dict(hallo="Dag schatje! Ik brei een warm sjaaltje voor de rechter. Rechtbanken zijn altijd zo tochtig.",
                               gezien="Opa Knabbel zei elke dag: 'Mijn hoeve gaat naar iemand die er lief voor zorgt.' Ik zal het de rechter vertellen, schatje.",
                               waar="In het karretje. Ik reis graag mee. Het breit zo lekker, dat gehobbel."),
                   "pim": dict(hallo="Ik getuig voor jou! Ik ben wel een beetje bang. Mag ik mijn worteltje meenemen?",
                               gezien="Morris zegt dat één knabbel geen echte prijs is. En dat het testament nep is. Maar dat is niet waar! Ik heb het gezien!",
                               waar="Ik oefen mijn getuigenis. In mijn moestuintje. De worteltjes luisteren heel goed."),
                   "hendrik": dict(hallo="Kukeleku! (Hendrik de Haan staat heel trots op één poot.)",
                                   gezien="Kukeleku! Ik heb niks gezien. Maar ik kom graag. Mag mijn eendje ook mee?",
                                   waar="Kukeleku. Bij de vijver. Met mijn eendje. Romantisch."),
                   "guhrrit": dict(hallo="Speurneus! Kijk mijn kleurtje! Bouwkundig rapport van mijn vakantie: vads.",
                                   gezien="Ik heb Opa's testament precies uitgevoerd: één knabbel, voor iemand die lief voor de hoeve zorgt. En ik heb die knabbel even geproefd. Om te kijken of hij echt was! Hij was echt. Heerlijk zelfs.",
                                   waar="Op het strand! Met mijn sombrero. Maar nu ben ik terug, voor de rechtszaak. Wat leuk!")},
         oplossing=[("vadson", "We hebben alle getuigen! Onze zaak is rond, speurneus!"),
                    ("sherlock", "Zoals ik al zei: rond. Nu nog een Hoeve van level 9, dan gaan we naar de rechter."),
                    ("guhrrit_vakantie", "Ik ga mee! Ik neem mijn beste sombrero. Rechtszaken zijn zó leuk.")]),
    dict(id="rechtszaak", titel="De Grote Knabbelzaak",
         kort="De rechtszaak tegen Morris-Mika, in de rechtbank van het buurdorp. Kies het juiste bewijs en roep: BEZWAAR, NJEG!",
         wacht="verbeter de Hoeve naar level 9 (en los De voorbereiding op)",
         intro=[], kaarten={}, fout={}, getuigen={}, oplossing=[]),
]

DECO = {"hartjesnest": "Hartjesnest", "theekransje_tafeltje": "Theekransje-tafeltje", "uilenhuisje": "Uilenhuisje van Oehoe",
        "reuzensjaal": "Reuzensjaal", "koeienbel_lantaarn": "Koeienbel-lantaarn", "moestuintje_pim": "Pims moestuintje",
        "glimwormguhtjes": "Glimwormguhtjes (sfeerlicht)", "oudste_boompje": "Het oudste boompje (met bankje)", "getuigenbank": "Getuigenbank",
        "standbeeld_opa_knabbel": "Standbeeld van Opa Knabbel", "knabbelparkinkje": "Knabbelparkinkje"}

BELONING = {
    "recept_theetaartje": "De bakkerij kent nu het recept van het theetaartje!",
    "oma_bestelling": "Oma Breiguh bestelt voortaan vaak bij je, met een extraatje erbij.",
    "opas_kelder": "Opa's kelder is nu een knusse kamer in je Hoeve, met plekjes voor je vondsten!",
    "bertha_optreden": "Bertha de koe treedt voortaan op tijdens dorpsfeestjes!",
    "paaltjes_terug": "Pim zet alle grenspaaltjes weer recht, strikje en al. Je dorp is weer precies even groot als altijd! Als sorry stopt hij je zijn mooiste worteltje toe. Njeg.",
    "testament": "Opa Knabbels testament is compleet, met de laatste bladzijde!",
    "bureau_eindlook": "Je Detectivebureau krijgt zijn mooiste look!",
}

TITELS = {
    "sherlock_speurneus": ("Dorpsspeurneus", "Los in De Erfenis je eerste zaakje met Sherlock Guh op."),
    "sherlock_spokenjager": ("Spokenjager", "Ontdek wat er in het oude schuurtje spookt."),
    "sherlock_archiefguh": ("Archiefguh", "Vind de bladzijden van een heel oud dagboek terug."),
    "sherlock_grenswachter": ("Grenswachter", "Zet de verschoven paaltjes van je dorp terug."),
    "sherlock_lichtpuntje": ("Lichtpuntje", "Ontdek wie er 's nachts licht maakt in de kelder."),
    "sherlock_schatbewaarder": ("Schatbewaarder", "Vind wat onder het oudste boompje lag."),
    "sherlock_meesterspeurneus": ("Meesterspeurneus", "Win De Grote Knabbelzaak."),
}

BEWIJS = {"testament": "Opa Knabbels testament", "laatste_bladzijde": "De laatste bladzijde (met handtekening)",
          "kaasknabbel": "De kaasknabbel zelf", "rood_veertje": "Het rode veertje van Hendrik", "glimmend_knoopje": "Het glimmende knoopje van Morris"}

S = "sherlock."
OVERIG = {
    "item.guhs.sherlock_vergrootglas": "Sherlocks vergrootglas",
    "entity.guhs.guh_npc.sherlock_guh": "Sherlock Guh", "entity.guhs.guh_npc.dokter_vadson": "Dokter Vadson",
    "entity.guhs.guh_npc.makelaar_guhrrit": "Makelaar Guhrrit", "entity.guhs.guh_npc.guhrrit_vakantie": "Makelaar Guhrrit",
    "entity.guhs.guh_npc.morris_mika": "Morris-Mika", "entity.guhs.guh_npc.pim_mika": "Pim", "entity.guhs.guh_npc.oehoe": "Oehoe",
    "entity.guhs.guh_npc.knabbelhamer": "Edelachtbare Knabbelhamer",
    S + "hud.sporen": "Sporen %s/%s · %s",
    S + "bericht.wacht": "%s wacht nog even. Eerst: %s.",
    S + "bericht.klaar": "Er ligt een nieuw zaakje in de brievenbus van het Detectivebureau: %s! %s",
    S + "bericht.klaar.klik": "[Open het bureau]",
    S + "bericht.glas": "Je krijgt het vergrootglas van Sherlock. Houd het vast in je dorp tijdens een zaak: dan glinsteren de sporen!",
    S + "bericht.kaartje": "Nieuw kaartje: %s. Vadson: \"%s\"",
    S + "bericht.alles_gevonden": "Alle kaartjes gevonden! Ga naar het speurbord in het Detectivebureau en wijs de dader aan.",
    S + "bericht.alles_verzameld": "Alle getuigen gesproken!",
    S + "bericht.opgelost": "Zaak opgelost: %s! Je krijgt %s Guhlden.",
    S + "bericht.sticker": "Sticker in je Speurboekje: In één keer goed!",
    S + "bericht.titel": "Nieuwe titel: %s",
    S + "bericht.deco": "Nieuwe deco voor je dorp: %s",
    S + "bericht.pet": "Je krijgt Sherlocks ruitjespet (de Guhlock-pet) voor je guhs!",
    S + "glas.grap.0": "Dit is gras. Heel verdacht gras.",
    S + "glas.grap.1": "Je ziet... je eigen neus. Vergroot.",
    S + "glas.grap.2": "Een mier! Oh nee, een kruimel. Oh nee, een mier met een kruimel.",
    S + "glas.grap.3": "Niks te zien. Alleen heel veel vads.",
    S + "glas.grap.4": "Hmm. Dit steentje kijkt schuldig. Maar er is geen zaak.",
    S + "glas.grap.5": "Zonder zaak is een vergrootglas gewoon een heel rond raampje.",
    S + "glas.niet_hier": "Hier is niks te speuren. De sporen liggen in je eigen dorp.",
    S + "glas.heet": "Heel warm! Er ligt hier vlakbij iets. Kijk ernaar en probeer het nog eens.",
    S + "glas.warm": "Warm... de sporen zijn niet ver weg.",
    S + "glas.koud": "Koud. Brrr. Kijk eens ergens anders in het dorp.",
    S + "glas.alles": "Alle sporen gevonden! Vraag de getuigen, of ga naar het speurbord.",
    S + "optie.gezien": "Wat heb je gezien?", S + "optie.waar": "Waar was je?", S + "optie.toon": "Kijk eens naar dit spoor...",
    S + "optie.doei": "Doei!", S + "optie.bord": "Naar het speurbord", S + "optie.boekje": "Het Speurboekje", S + "optie.naar_huis": "Naar huis",
    S + "getuige.klaar": "Bedankt dat je langskwam, speurneus! Ik heb alles al verteld.",
    S + "vadson.rust": "Geen zaak op dit moment. Ik lees mijn Speurboekje nog eens door. Spannend, hoor!",
    S + "vadson.klaar": "Er ligt een nieuw zaakje in de brievenbus! Kijk maar in het Speurboekje!",
    S + "vadson.tip.sporen": "We missen nog %s spoor(en). Houd het vergrootglas vast en loop rond: wat glinstert, is een spoor!",
    S + "vadson.tip.getuige": "Alle sporen gevonden! Maar %s weet vast nog iets. Ga het eens vragen!",
    S + "vadson.tip.bord": "We hebben alles! Hang drie kaartjes aan één verdachte op het speurbord en wijs de dader aan.",
    S + "bord.te_weinig": "Hang eerst minstens drie kaartjes aan %s, speurneus. Anders gelooft niemand ons.",
    S + "bord.fout.0": "Hmm, dat klopt toch niet helemaal. Kijk nog eens goed naar de kaartjes!",
    S + "bord.fout.1": "Geeft niks! Een echte speurneus probeert het gewoon nog een keer.",
    S + "bord.fout.2": "Sherlock mompelt in zijn slaap: 'koud... kouder...' Probeer iemand anders!",
    S + "bord.haring": "Wacht eens... dit kaartje past niet: %s. Ik heb het even rood gemaakt. Haal dat draadje eens weg!",
    S + "klik.ander": "Hallo! Ik help de speurneus van dit dorp. Jouw eigen zaakjes liggen in je eigen dorp, njeg.",
    S + "klik.iemand": "Njeg?",
    S + "klik.oehoe": "Hoe, hoe. Ik bewaak hier wat. Wat precies? Dat is geheim. Hoe dan ook.",
    S + "klik.morris": "Opzij, opzij. Tijd is asfalt.",
    S + "klik.pim": "Hoi! Wil je mijn worteltjes zien? Ze groeien al heel hard. Een beetje.",
    S + "klik.knabbelhamer": "Orde in de zaal! (Hapje.)",
    S + "klik.rechtbank.rechter": "Stilte in de zaal! ...Nou ja, je mag wel fluisteren.",
    S + "klik.rechtbank.morris": "Hmpf. Wacht maar tot de rechter mij hoort.",
    S + "klik.rechtbank.pim": "Ik ben heel zenuwachtig. Ik heb al drie keer mijn worteltje geaaid.",
    S + "klik.rechtbank.sherlock": "Zzz... BEZWAAR... zzz...",
    S + "klik.rechtbank.vadson": "Ik schrijf alles op! Ook dat de rechter zijn hamer opeet.",
    S + "klik.rechtbank.oehoe": "Hoe, hoe. Ik bewaak de getuigenbank.",
    S + "klik.rechtbank.guhrrit": "Wat een spannende zaak! Bouwkundig rapport van deze rechtbank: vads.",
    S + "klik.rechtbank.oma": "Ik brei een sjaaltje voor de rechter, schatje. Hij heeft het koud onder zijn pruik.",
    S + "klik.rechtbank.iemand": "Njeg?",
    S + "guhrrit.0": "Bouwkundig rapport: vads. Emotioneel rapport: heel vads.",
    S + "guhrrit.1": "Eén kaasknabbel voor een hele hoeve. Een koopje! Ik zou hem zelf nemen, als ik niet zo druk was.",
    S + "guhrrit.2": "Ik heb het altijd gezegd: dit is een opknapper. Met de nadruk op knapper!",
    S + "guhrrit.3": "Kijk eens wat een uitzicht. Nou ja, het zijn nu nog brandnetels. Maar die bloeien straks!",
    S + "guhrrit.4": "Ik ga zo op vakantie. Ik stuur je een kaartje. Met zon erop!",
    S + "guhrrit.5": "Papierwerk? Geregeld! Ik heb overal een krulletje onder gezet.",
    S + "cmd.onbekend": "Die zaak bestaat niet.", S + "cmd.alles_klaar": "Alle zaken zijn al opgelost!",
    S + "cmd.reset": "Sherlock-voortgang gewist.", S + "cmd.geen_zaak": "Er loopt nu geen zaak.",
    # the courtroom
    S + "rechtbank.welkom": "Je bent in de rechtbank van het buurdorp. Iedereen is er!",
    S + "rechtbank.bewering.1": "De grond was te koop voor één knabbel. Eén knabbel is geen ÉCHTE prijs! Dus de verkoop is ongeldig!",
    S + "rechtbank.bewering.2": "Dat testament is nep! Er staat niet eens een handtekening onder!",
    S + "rechtbank.bewering.3": "Die knabbel was van plastic! Er is dus nooit echt betaald!",
    S + "rechtbank.kies": "Speurneus, laat je beste bewijs zien! Kies het van je speurbord.",
    S + "rechtbank.fout.testament": "Het testament hebben we al gezien, speurneus. Iets anders, alstublieft.",
    S + "rechtbank.fout.laatste_bladzijde": "Een handtekening, wat mooi. Maar daar gaat deze vraag niet over.",
    S + "rechtbank.fout.kaasknabbel": "Een knabbel! Lekker. Maar daar gaat het nu nog niet over. Hapje?",
    S + "rechtbank.fout.rood_veertje": "Een veertje? Wat heeft een veertje hiermee te maken? Mooi rood is het wel.",
    S + "rechtbank.fout.glimmend_knoopje": "Een knoopje van meneer Morris. Fijn dat het terug is. Maar het bewijst niets.",
    S + "rechtbank.hint.1": "Psst... wat zei Opa Knabbel ook alweer in zijn testament? Over de prijs?",
    S + "rechtbank.hint.2": "Psst... een handtekening. Wat vonden we ook alweer onder het oudste boompje?",
    S + "rechtbank.hint.3": "Psst... plastic? Wie had er ook alweer even in die knabbel geproefd?",
    S + "rechtbank.terug": "Zitting gesloten. Wil je naar huis, speurneus? Of nog even rondkijken?",
    S + "rechtbank.thuis": "Je bent weer thuis. Het dorp blijft van jou. En Morris parkeert voortaan heel netjes in zijn Knabbelparkinkje.",
    # the screen
    S + "ui.bureau": "Detectivebureau", S + "ui.boekje": "Het Speurboekje van Dokter Vadson",
    S + "ui.proloog.ja": "Proloog: een roestig sleuteltje met een kaasje", S + "ui.proloog.nee": "Proloog: ???",
    S + "ui.geheim": "%s. ???", S + "ui.hoofdstuk": "%s. %s",
    S + "ui.status.wacht": "Nog even geduld...", S + "ui.status.klaar": "Ligt klaar in de brievenbus!", S + "ui.status.bezig": "Bezig",
    S + "ui.status.opgelost": "Opgelost", S + "ui.wacht_op": "Dit hoofdstuk komt later. Eerst: %s.",
    S + "ui.begin": "Begin de zaak!", S + "ui.naar_bord": "Naar het speurbord", S + "ui.naar_rechtbank": "Naar de rechtbank!",
    S + "ui.terugkijken": "Bord terugkijken", S + "ui.sluiten": "Sluiten", S + "ui.sticker": "In één keer goed!",
    S + "ui.bord": "Speurbord: %s", S + "ui.kaartjes": "Kaartjes: %s/%s", S + "ui.wijs": "Wijs aan!", S + "ui.dader": "DADER",
    S + "ui.testament": "Opa Knabbels testament",
    S + "ui.testament.tekst": "Mijn hoeve gaat naar wie er lief voor zorgt, voor de prijs van één knabbel. Niet aan iemand met een stropdas. (Opa Knabbel)",
    S + "ui.opgelost": "Opgelost! Het was %s. Zo lief!", S + "ui.opgelost_rond": "Opgelost! De zaak is rond.",
    S + "ui.uitleg": "Klik een kaartje en dan een verdachte: zo span je een rood draadje. Rechtsklik op een kaartje haalt het los. Drie draadjes aan één verdachte? Wijs aan!",
    S + "ui.uitleg_kies": "Je hebt '%s' vast. Klik nu op een verdachte.",
    S + "ui.uitleg_rood": "Vadson heeft een kaartje rood gemaakt: dat past niet. Haal het draadje weg (rechtsklik) of hang het ergens anders.",
    S + "ui.uitleg_verzamel": "Praat met alle getuigen in je dorp. Elk getuigenis komt hier aan het testament te hangen.",
    S + "ui.terug_boekje": "« Speurboekje", S + "ui.vadson_zegt": "Vadson: %s",
}

# the cutscenes of the courtroom: id -> (title, lines, speaker names)
_NAMEN = {"rechter": "Edelachtbare Knabbelhamer", "morris": "Morris-Mika", "sherlock": "Sherlock Guh", "speler": "Jij", "vadson": "Dokter Vadson",
          "pim": "Pim", "oehoe": "Oehoe", "guhrrit": "Makelaar Guhrrit", "oma": "Oma Breiguh"}
SCENES = {
    "sherlock_rechtbank_begin": ("De Grote Knabbelzaak", {
        "open": "Stilte in de zaal! De rechtbank van het buurdorp is geopend.",
        "zaak": "Vandaag: De Grote Knabbelzaak. Morris-Mika tegen de nieuwe eigenaar van de hoeve van Opa Knabbel. Hapje.",
        "bewering1": "Edelachtbare! De grond was te koop voor één knabbel. Eén knabbel is geen ÉCHTE prijs! De verkoop is ongeldig!",
        "wakker": "Zzz... huh? Is het al begonnen? Speurneus, jouw beurt.",
    }),
    "sherlock_rechtbank_bezwaar1": ("BEZWAAR, NJEG! (1)", {
        "bezwaar": "BEZWAAR, NJEG!",
        "bewijs": "Opa Knabbels testament: 'Mijn hoeve gaat naar wie er lief voor zorgt, voor de prijs van één knabbel.' Het was zijn wens!",
        "toegewezen": "Bezwaar toegewezen! Een knabbel is een prima prijs. Hapje.",
        "reactie": "Grrr... maar... maar...",
        "bewering2": "Dat testament is nep! Er staat niet eens een handtekening onder!",
    }),
    "sherlock_rechtbank_bezwaar2": ("BEZWAAR, NJEG! (2)", {
        "bezwaar": "BEZWAAR, NJEG!",
        "bewijs": "De laatste bladzijde, gevonden onder het oudste boompje! Met Opa Knabbels pootafdruk als handtekening!",
        "toegewezen": "Toegewezen! Een echte pootafdruk. Wat een schattig pootje. Hapje.",
        "reactie": "Dat... dat is wel een héél overtuigend pootje.",
        "bewering3": "Maar die knabbel was van PLASTIC! Er is dus nooit echt betaald!",
    }),
    "sherlock_rechtbank_bezwaar3": ("BEZWAAR, NJEG! (3)", {
        "bezwaar": "BEZWAAR, NJEG!",
        "bewijs": "De kaasknabbel zelf! Met het tandafdrukje van makelaar Guhrrit. Hij moest toch even proeven of hij echt was?",
        "toegewezen": "Toegewezen! Een plastic knabbel proef je niet. Deze is echt. Ik heb ook even geproefd. Hapje.",
        "reactie": "Nee... nee... mijn Knabbelparking...",
        "uitspraak": "Uitspraak: de hoeve blijft bij wie er lief voor zorgt. Zitting gesloten!",
        "hapje": "(Hij neemt een hapje uit zijn kaasknabbel-hamer.) Mmm.",
        "zucht": "Zucht... Weet je, ik wilde eigenlijk alleen maar ergens mijn eigen karretje parkeren.",
        "plekje": "Dan krijg je toch gewoon een plekje bij ons? Eén parkeervak. Met een bloembak.",
        "bloemetjes": "Een... Knabbelparkinkje? Voor mij? ...Bloemetjes zijn eigenlijk best oké.",
        "knipoog": "Maar mijn neefjes, de kippendieven... die zijn een stuk minder aardig. Let maar goed op je eitjes. Hihi.",
        "zoals_gezegd": "Zoals ik al zei: opgelost. Zzz...",
    }),
}


def teksten():
    """Every lang key of the story (Dutch) and the speakers per dialogue group."""
    t, regels = dict(OVERIG), {}

    def groep(naam, lijnen):
        regels[naam] = [wie for wie, _ in lijnen]
        for i, (_, tekst) in enumerate(lijnen):
            t[f"{S}{naam}.{i}"] = tekst

    for naam, lijnen in ALGEMEEN.items():
        groep(naam, lijnen)
    for wie, naam in WIE.items():
        t[f"{S}wie.{wie}"] = naam
    for z in ZAKEN:
        p = f"{S}zaak.{z['id']}."
        t[p + "titel"] = z["titel"]
        t[p + "kort"] = z["kort"]
        t[p + "wacht"] = z["wacht"]
        if z["intro"]:
            groep(f"zaak.{z['id']}.intro", z["intro"])
        if z["oplossing"]:
            groep(f"zaak.{z['id']}.oplossing", z["oplossing"])
        for k, (naam, tekst) in z["kaarten"].items():
            t[p + f"kaart.{k}"] = naam
            t[p + f"kaart.{k}.tekst"] = tekst
        for v, tekst in z["fout"].items():
            t[p + f"fout.{v}"] = tekst
        for g, d in z["getuigen"].items():
            for vraag, tekst in d.items():
                t[p + f"getuige.{g}.{vraag}"] = tekst
    for k, v in DECO.items():
        t[f"{S}deco.{k}"] = v
    for k, v in BELONING.items():
        t[f"{S}beloning.{k}"] = v
    for k, v in BEWIJS.items():
        t[f"{S}bewijs.{k}"] = v
    for k, (naam, hint) in TITELS.items():
        t[f"gui.guhs.titels.naam.{k}"] = naam
        t[f"gui.guhs.titels.hint.{k}"] = hint
    for sid, (titel, lijnen) in SCENES.items():
        t[f"scene.guhs.{sid}.titel"] = titel
        for k, v in lijnen.items():
            t[f"scene.guhs.{sid}.{k}"] = v
        for acteur, naam in _NAMEN.items():
            t[f"scene.guhs.{sid}.naam.{acteur}"] = naam
    for k, v in t.items():
        if "%" in v.replace("%s", ""):
            raise SystemExit(f"erfenis_sherlock: a stray % in {k}")
    return t, regels


# =====================================================================================================================
# the looks
# =====================================================================================================================
def _npc_geo(h, basis, kind):
    from features import knuffeldal_npcs as kn
    geo_file = kn._load(h, basis)
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = f"geometry.guh_npc_{kind}"
    return geo_file, geo


def _maak(h, kind, basis, tex, kleur, bones, paint, seed, na=None):
    """A character: extra bones {name: (parent, pivot, [(origin, size, swatch, inflate)])} with painted swatches."""
    from features import knuffeldal_npcs as kn
    geo_file, geo = _npc_geo(h, basis, kind)
    namen = sorted({c[2] for (_, _, cubes) in bones.values() for c in cubes})
    sw = kn._swatches(geo, namen)
    for naam, (parent, pivot, cubes) in bones.items():
        geo["bones"].append({"name": naam, "parent": parent, "pivot": pivot,
                             "cubes": [kn._cube(o, s, sw[n], inf) for (o, s, n, inf) in cubes]})
    kn._save_geo(h, f"guh_npc_{kind}.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", tex)).convert("RGBA")
    img = h.recolour(src, hue=kleur[0], sat=kleur[1], val=kleur[2], only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    if len(kleur) > 3:
        _tint(a, kleur[3])
    rng = np.random.default_rng(seed)
    for naam, (colour, var, pattern) in paint.items():
        if colour == "vacht":
            colour = _vacht(a)
        kn._paint(a, sw[naam], colour, rng, var, pattern)
    if na:
        na(a)
    h.save(Image.fromarray(a), "entity", f"npc_{kind}.png")


def _tint(a, rgb):
    """The white fur of the sitting guh gets the character's own colour (light, low-saturation pixels only)."""
    c = a[..., :3].astype(np.int32)
    licht = (a[..., 3] > 0) & (c.min(axis=2) > 165) & (c.max(axis=2) - c.min(axis=2) < 45)
    t = np.array(rgb, np.int32)
    a[..., :3][licht] = np.clip(c[licht] * t // 255, 0, 255).astype(np.uint8)


def _vacht(a):
    """The fur colour of a recoloured sitting guh: the middle of its face (uv 110,32 + a bit, 4 px per uv)."""
    blok = a[(32 + 7) * 4:(32 + 9) * 4, (110 + 6) * 4:(110 + 8) * 4, :3].reshape(-1, 3)
    return tuple(int(x) for x in np.median(blok, axis=0))


def _ruit(a, b, stap=8):
    def f(block):
        for yy in range(32):
            for xx in range(32):
                block[yy, xx, :3] = a if ((xx // stap) + (yy // stap)) % 2 else b
                if xx % stap == 0 or yy % stap == 0:
                    block[yy, xx, :3] = tuple(max(0, c - 30) for c in b)
    return f


def _streep(kleur, stap=8, dik=3):
    def f(block):
        for yy in range(0, 32, stap):
            block[yy:yy + dik, :, :3] = kleur
    return f


def _oog(block):
    """An owl's eye: a yellow disk with a black pupil and a little glint."""
    block[..., :3] = (120, 84, 50)
    for yy in range(32):
        for xx in range(32):
            r = ((xx - 15.5) ** 2 + (yy - 15.5) ** 2) ** 0.5
            if r < 15:
                block[yy, xx, :3] = (250, 200, 60)
            if r < 8:
                block[yy, xx, :3] = (24, 18, 22)
    block[8:12, 10:14, :3] = (255, 255, 255)


def _krul(block):
    block[..., :3] = (236, 232, 246)
    for yy in range(0, 32, 6):
        for xx in range(32):
            if (xx + yy // 2) % 8 < 2:
                block[yy:yy + 2, xx, :3] = (178, 170, 200)


def _bloemetjes(block):
    block[..., :3] = (60, 170, 200)
    rng = np.random.default_rng(1909)
    for _ in range(14):
        y, x = int(rng.integers(2, 30)), int(rng.integers(2, 30))
        kleur = [(250, 120, 160), (255, 220, 80), (255, 255, 255)][int(rng.integers(3))]
        block[y - 2:y + 2, x - 1:x + 1, :3] = kleur
        block[y - 1:y + 1, x - 2:x + 2, :3] = kleur


def _sombrero(block):
    block[..., :3] = (236, 200, 110)
    for yy in range(0, 32, 4):
        block[yy, :, :3] = (210, 170, 84)


def _pas(block):
    block[..., :3] = (252, 252, 248)
    block[0:7, :, :3] = (80, 140, 210)
    block[12:15, 6:26, :3] = (120, 120, 130)
    block[19:22, 6:22, :3] = (160, 160, 170)


def _knabbel(block):
    block[..., :3] = (238, 150, 50)
    for (y, x) in ((8, 8), (20, 18), (12, 24), (24, 6)):
        block[y - 2:y + 2, x - 2:x + 2, :3] = (200, 110, 30)


def modellen(h):
    H = [0, 13, 0]
    B = [0, 10, -4]
    # Sherlock Guh: caramel; a brown checked deerstalker (both peaks, ear flaps tied with a bow on top), sleepy eyelids, a
    # bubble pipe with a pink rim and two bubbles, a checked Inverness cape over his shoulders
    _maak(h, "sherlock_guh", "guh_sitting.geo.json", "guh_sitting.png", (0.075, 0.42, 1.0, (246, 214, 172)), {
        "sh_pet": ("head", H, [([-6.2, 24.4, -6.4], [12.4, 3.4, 12.0], "pet", 0), ([-5.0, 27.8, -5.2], [10.0, 0.8, 9.6], "pet", 0),
                              ([-4.2, 24.3, -9.4], [8.4, 0.6, 3.1], "pet_d", 0), ([-4.2, 24.3, 5.5], [8.4, 0.6, 2.8], "pet_d", 0),
                              ([-6.9, 25.0, -2.0], [0.8, 2.6, 3.6], "pet_d", 0), ([6.1, 25.0, -2.0], [0.8, 2.6, 3.6], "pet_d", 0),
                              ([-1.4, 28.5, -0.9], [2.8, 0.9, 1.8], "strik", 0)]),
        "sh_oogleden": ("head", H, [([-6.7, 22.3, -7.3], [4.8, 2.4, 0.4], "lid", 0), ([1.9, 22.3, -7.3], [4.8, 2.4, 0.4], "lid", 0),
                                   ([-6.7, 22.1, -7.4], [4.8, 0.3, 0.4], "wimper", 0), ([1.9, 22.1, -7.4], [4.8, 0.3, 0.4], "wimper", 0)]),
        "sh_pijp": ("head", H, [([1.4, 16.9, -10.6], [0.7, 0.7, 3.2], "pijp", 0), ([1.0, 16.4, -12.2], [1.7, 2.1, 1.7], "pijp", 0),
                               ([0.9, 18.5, -12.3], [1.9, 0.4, 1.9], "pijp_rand", 0)]),
        "sh_bel": ("head", [1.85, 19.6, -11.35], [([1.3, 19.4, -11.9], [1.1, 1.1, 1.1], "bel", 0)]),
        "sh_bel2": ("head", [2.6, 21.6, -11.0], [([2.25, 21.6, -11.35], [0.7, 0.7, 0.7], "bel", 0)]),
        "sh_cape": ("body", B, [([-5.3, 8.6, -4.6], [10.6, 3.4, 9.2], "cape", 0), ([-5.0, 7.6, 3.6], [10.0, 1.2, 1.0], "cape", 0)]),
    }, {"pet": ((168, 126, 82), 6, _ruit((150, 108, 66), (186, 146, 100))), "pet_d": ((128, 92, 58), 6, None),
        "strik": ((214, 96, 124), 6, None), "lid": ("vacht", 3, None), "wimper": ((70, 44, 40), 2, None),
        "pijp": ((112, 70, 40), 6, None), "pijp_rand": ((244, 150, 180), 4, None), "bel": ((196, 236, 255), 4, None),
        "cape": ((150, 112, 76), 6, _ruit((132, 96, 62), (164, 126, 88), 6))}, 190101)
    # Dokter Vadson: soft blue-grey; a bowler hat with a pink band, a moustache, a bow tie and the red Speurboekje in his paws
    _maak(h, "dokter_vadson", "guh_sitting.geo.json", "guh_sitting.png", (0.58, 0.22, 0.98, (206, 218, 238)), {
        "va_hoed": ("head", H, [([-6.0, 24.4, -6.6], [12.0, 0.6, 12.6], "hoed", 0), ([-4.4, 25.0, -5.0], [8.8, 3.4, 9.4], "hoed", 0),
                               ([-4.5, 25.0, -5.1], [9.0, 0.8, 9.6], "band", 0), ([-3.8, 28.4, -4.4], [7.6, 0.6, 8.0], "hoed", 0)]),
        "va_snor": ("head", H, [([-3.0, 17.9, -7.45], [6.0, 1.0, 0.6], "snor", 0), ([-3.8, 17.3, -7.45], [1.2, 1.2, 0.6], "snor", 0),
                               ([2.6, 17.3, -7.45], [1.2, 1.2, 0.6], "snor", 0)]),
        "va_strik": ("body", B, [([-1.6, 10.8, -4.85], [3.2, 1.4, 0.6], "strik", 0)]),
        "va_boek": ("body", B, [([-2.7, 6.4, -7.9], [5.4, 4.4, 1.1], "boek", 0), ([-2.5, 6.6, -8.0], [0.6, 4.0, 0.2], "pagina", 0)]),
    }, {"hoed": ((58, 52, 60), 4, None), "band": ((210, 120, 150), 4, None), "snor": ((96, 66, 50), 6, None),
        "strik": ((120, 166, 214), 5, None), "boek": ((190, 58, 70), 6, _streep((236, 196, 90), 14, 2)), "pagina": ((252, 244, 226), 3, None)}, 190102)
    # makelaar Guhrrit: mint; a navy jacket with lapels, a red tie, a clipboard, sunglasses pushed up on his head
    guhrrit_jas = {
        "gu_das": ("body", B, [([-0.8, 4.4, -4.95], [1.6, 6.4, 0.5], "das", 0), ([-1.0, 10.6, -5.05], [2.0, 1.2, 0.6], "das", 0)]),
        "gu_colbert": ("body", B, [([-4.4, 2.6, -4.75], [3.4, 9.0, 0.4], "colbert", 0), ([1.0, 2.6, -4.75], [3.4, 9.0, 0.4], "colbert", 0)]),
        "gu_bord": ("body", B, [([-7.0, 4.8, -7.8], [3.8, 5.0, 0.6], "bord", 0), ([-6.6, 5.2, -7.95], [3.0, 4.0, 0.2], "papier", 0),
                               ([-5.8, 9.6, -7.95], [1.4, 0.7, 0.7], "clip", 0)]),
    }
    _maak(h, "makelaar_guhrrit", "guh_sitting.geo.json", "guh_sitting.png", (0.36, 0.30, 1.0, (204, 238, 216)), dict(guhrrit_jas, **{
        "gu_bril": ("head", H, [([-4.8, 25.0, -6.6], [9.6, 0.9, 1.0], "bril", 0)]),
        "gu_kuif": ("head", H, [([-3.0, 25.6, -5.4], [6.0, 1.2, 4.0], "kuif", 0)]),
    }), {"das": ((214, 60, 84), 6, _streep((150, 30, 54), 8, 2)), "colbert": ((62, 82, 136), 6, None), "bord": ((156, 112, 70), 6, None),
         "papier": ((252, 250, 244), 3, _streep((150, 160, 190), 6, 1)), "clip": ((200, 204, 214), 4, None), "bril": ((30, 28, 36), 3, None),
         "kuif": ((92, 150, 120), 6, None)}, 190103)
    # ... back from holiday: sunburnt, a flowery shirt instead of the jacket, a sombrero and his sunglasses on
    _maak(h, "guhrrit_vakantie", "guh_sitting.geo.json", "guh_sitting.png", (0.98, 0.62, 1.04, (255, 196, 184)), {
        "gu_hemd": ("body", B, [([-4.6, 2.4, -4.75], [9.2, 9.2, 0.4], "hemd", 0)]),
        "gu_sombrero": ("head", H, [([-11.0, 24.6, -11.0], [22.0, 0.6, 22.0], "sombrero", 0), ([-4.4, 25.2, -4.6], [8.8, 4.4, 8.8], "sombrero", 0),
                                   ([-4.5, 25.2, -4.7], [9.0, 1.0, 9.0], "sombrero_band", 0), ([-3.4, 29.6, -3.6], [6.8, 1.0, 6.8], "sombrero", 0)]),
        "gu_zonnebril": ("head", H, [([-6.8, 20.8, -7.45], [13.6, 2.4, 0.4], "zonnebril", 0)]),
        "gu_bord": guhrrit_jas["gu_bord"],
    }, {"hemd": ((60, 170, 200), 6, _bloemetjes), "sombrero": ((236, 200, 110), 6, _sombrero), "sombrero_band": ((214, 60, 84), 6, None),
        "zonnebril": ((24, 22, 30), 3, None), "bord": ((156, 112, 70), 6, None), "papier": ((252, 250, 244), 3, None),
        "clip": ((200, 204, 214), 4, None)}, 190104)
    # Oehoe: a cuddly brown owl; big yellow eyes, a beak, a cream chest with chevrons, feather tufts and little wings
    _maak(h, "oehoe", "guh_sitting.geo.json", "guh_sitting.png", (0.075, 0.40, 0.72, (186, 146, 110)), {
        "oe_ogen": ("head", H, [([-6.8, 19.4, -7.35], [5.0, 5.0, 0.35], "oog", 0), ([1.8, 19.4, -7.35], [5.0, 5.0, 0.35], "oog", 0)]),
        "oe_snavel": ("head", H, [([-0.9, 16.8, -8.0], [1.8, 2.4, 1.0], "snavel", 0)]),
        "oe_pluim": ("head", H, [([-6.4, 24.2, -1.4], [2.2, 3.2, 1.4], "pluim", 0), ([4.2, 24.2, -1.4], [2.2, 3.2, 1.4], "pluim", 0)]),
        "oe_borst": ("body", B, [([-3.4, 2.6, -4.7], [6.8, 7.4, 0.5], "borst", 0)]),
        "oe_vleugels": ("body", B, [([-5.7, 3.2, -2.8], [0.7, 7.2, 5.6], "vleugel", 0), ([5.0, 3.2, -2.8], [0.7, 7.2, 5.6], "vleugel", 0)]),
    }, {"oog": ((250, 200, 60), 2, _oog), "snavel": ((240, 160, 60), 6, None), "pluim": ((110, 76, 46), 8, None),
        "borst": ((242, 226, 196), 6, _streep((196, 160, 120), 6, 1)), "vleugel": ((112, 80, 52), 8, _streep((92, 64, 40), 6, 2))}, 190105)
    # Edelachtbare Knabbelhamer: a huge white curly wig, a black robe with a white jabot, a kaasknabbel as his gavel
    _maak(h, "knabbelhamer", "guh_sitting.geo.json", "guh_sitting.png", (0.92, 0.30, 1.0, (250, 214, 226)), {
        "kh_pruik": ("head", H, [([-7.2, 24.2, -6.8], [14.4, 2.8, 12.6], "pruik", 0), ([-9.6, 14.6, -4.4], [2.8, 10.4, 7.4], "pruik", 0),
                                ([6.8, 14.6, -4.4], [2.8, 10.4, 7.4], "pruik", 0), ([-7.2, 13.6, 4.2], [14.4, 11.0, 2.6], "pruik", 0),
                                ([-9.8, 12.6, -4.0], [3.0, 2.2, 6.6], "pruik", 0), ([6.8, 12.6, -4.0], [3.0, 2.2, 6.6], "pruik", 0)]),
        "kh_toga": ("body", B, [([-4.7, 1.8, -4.85], [9.4, 10.4, 0.4], "toga", 0), ([-1.7, 8.2, -5.15], [3.4, 3.4, 0.4], "jabot", 0)]),
        "kh_hamer": ("body", B, [([-7.4, 5.6, -7.2], [0.8, 6.4, 0.8], "steel", 0), ([-9.0, 11.6, -8.1], [4.0, 2.4, 2.4], "knabbel", 0)]),
    }, {"pruik": ((236, 232, 246), 4, _krul), "toga": ((36, 32, 40), 4, None), "jabot": ((252, 252, 250), 3, _streep((226, 226, 232), 5, 1)),
        "steel": ((150, 104, 60), 6, None), "knabbel": ((238, 150, 50), 8, _knabbel)}, 190106)

    # the Mikas
    from features import paleizen_modellen as pm
    for kind, kleur, bones, paint, seed in (
            # Morris-Mika: a shiny grey suit, slicked hair, a dark red tie, a briefcase by his paw
            ("morris_mika", (0.62, 0.08, 0.88), {
                "mo_kuif": ("head", [0, 5, -2], [([-4.0, 13.6, -9.4], [8.0, 1.6, 6.4], "haar", 0), ([-4.4, 13.0, -10.2], [8.8, 1.0, 2.0], "haar", 0)]),
                "mo_das": ("head", [0, 5, -2], [([-2.6, 0.2, -12.5], [2.0, 1.6, 0.6], "das", 0), ([0.6, 0.2, -12.5], [2.0, 1.6, 0.6], "das", 0), ([-0.6, 0.5, -12.7], [1.2, 1.0, 0.6], "knoop", 0)]),
                "mo_koffer": ("root", [0, 0, 0], [([7.0, 0.0, -14.8], [5.4, 3.8, 1.8], "koffer", 0), ([8.7, 3.8, -14.2], [2.0, 0.6, 0.5], "greep", 0)]),
            }, {"haar": ((70, 72, 82), 4, _streep((110, 112, 124), 4, 1)), "das": ((46, 48, 66), 4, None), "knoop": ((90, 94, 120), 3, None),
                "koffer": ((112, 76, 46), 6, _streep((90, 60, 36), 10, 1)), "greep": ((200, 200, 210), 3, None)}, 190107),
            # Pim: a soft green intern with a STAGIAIR badge, a pink bow on his ear and a little sprout on his head
            ("pim_mika", (0.30, 0.32, 1.0), {
                "pi_pas": ("head", [0, 5, -2], [([-1.8, 0.0, -12.6], [3.6, 2.6, 0.4], "pas", 0), ([-0.3, 2.6, -12.6], [0.6, 1.2, 0.4], "koord", 0)]),
                "pi_strik": ("head", [0, 5, -2], [([5.0, 13.4, -8.6], [2.6, 1.4, 0.7], "strik", 0)]),
                "pi_spruit": ("head", [0, 14, -6], [([-0.3, 13.8, -6.3], [0.6, 2.2, 0.6], "stengel", 0), ([-1.8, 15.6, -6.4], [1.6, 0.4, 0.8], "blad", 0),
                                                   ([0.2, 15.9, -6.4], [1.6, 0.4, 0.8], "blad", 0)]),
            }, {"pas": ((252, 252, 248), 2, _pas), "koord": ((80, 140, 210), 3, None), "strik": ((244, 130, 170), 5, None),
                "stengel": ((90, 170, 70), 5, None), "blad": ((120, 210, 90), 6, None)}, 190108)):
        _maak(h, kind, "mika.geo.json", "mika.png", kleur, bones, paint, seed,
              na=_glans if kind == "morris_mika" else None)
        anims = pm._mika_anims(schommel=False)
        if kind == "pim_mika":
            idle = anims["animations"]["animation.guh_sitting.idle"]["bones"]
            idle["pi_spruit"] = {"rotation": {"0.0": [0, 0, -8], "2.0": [0, 0, 8], "4.0": [0, 0, -8]}}
        h.w(os.path.join(h.A, "geckolib", "animations", "entity", f"guh_npc_{kind}.animation.json"), anims)

    # Sherlock's own animation file: the sitting guh's, his head nodding off (and jerking awake) and bubbles rising
    bron = json.load(open(os.path.join(h.A, "geckolib", "animations", "entity", "guh_sitting.animation.json"), encoding="utf-8"))
    anims = copy.deepcopy(bron)
    idle = anims["animations"]["animation.guh_sitting.idle"]
    idle["animation_length"] = 4.0
    bones = idle.setdefault("bones", {})
    bones["head"] = {"rotation": {"0.0": [0, 0, 0], "1.8": [10, 0, 4], "2.6": [16, 0, 6], "2.75": [-4, 0, 0], "3.1": [0, 0, 0], "4.0": [0, 0, 0]}}
    bones["sh_bel"] = {"position": {"0.0": [0, 0, 0], "2.0": [0.3, 3.2, 0], "2.01": [0, 0, 0], "4.0": [0.4, 3.4, 0]},
                       "scale": {"0.0": [0.2, 0.2, 0.2], "0.6": [1, 1, 1], "1.9": [1.2, 1.2, 1.2], "2.0": [0, 0, 0], "2.01": [0.2, 0.2, 0.2],
                                 "2.6": [1, 1, 1], "3.9": [1.1, 1.1, 1.1], "4.0": [0, 0, 0]}}
    bones["sh_bel2"] = {"position": {"0.0": [0, 1.4, 0], "1.0": [0.2, 3.0, 0], "1.01": [0, 0, 0], "4.0": [0.3, 2.8, 0]},
                        "scale": {"0.0": [1, 1, 1], "0.99": [1, 1, 1], "1.0": [0, 0, 0], "1.01": [0.1, 0.1, 0.1], "1.8": [0.9, 0.9, 0.9], "4.0": [1, 1, 1]}}
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "guh_npc_sherlock_guh.animation.json"), anims)


def _glans(a):
    """Morris' suit shines: a sprinkle of light specks."""
    rng = np.random.default_rng(190109)
    opaque = np.argwhere(a[..., 3] > 0)
    for i in rng.choice(len(opaque), 500, replace=False):
        y, x = opaque[i]
        a[y, x, :3] = np.minimum(255, a[y, x, :3].astype(int) + 70)


def items(h):
    h.w(os.path.join(h.A, "items", "sherlock_vergrootglas.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/sherlock_vergrootglas"}})
    h.w(os.path.join(h.A, "models", "item", "sherlock_vergrootglas.json"),
        {"parent": "minecraft:item/handheld", "textures": {"layer0": "guhs:item/detective_vergrootglas"}})


def regels_json(h, regels):
    h.w(os.path.join(h.D, "erfenis", "sherlock", "regels.json"), dict(sorted(regels.items())))


def build(h):
    t, regels = teksten()
    for k, v in t.items():
        h.lang(k, v, v)
    regels_json(h, regels)
    items(h)
    modellen(h)


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    t, regels = teksten()
    regels_json(h, regels)
    items(h)
    modellen(h)
    for taal in ("nl_nl", "en_us"):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as fh:
            d = json.load(fh)
        d.update(t)
        with open(p, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(d, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    print(f"erfenis_sherlock: {len(t)} texts, {len(regels)} dialogue groups")


if __name__ == "__main__":
    _direct()
