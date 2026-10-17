"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.4), slice sp_d1_stad: every Dutch text of part 1 steps 5-6 (jellyfishing with Patguh, wink
7; the boating exam with juf Pufguh, wink 5). The source; the English overlay is tools/lang/en/c113_e2_sp_d1_stad.json.
Read by einde2_sp_d1_stad (the step texts STAPPEN / EXTRA / KORT live there: the skelet reads them from that module).

  SCENES   {naam: (titel, {key: tekst}, {acteur: naam})}   Java D1StadScenes; scene.guhs.einde2_sp_d1_stad_<naam>.*
  KIJK     {naam: (titel, tekst)}                           the hints' look scenes scene.guhs.einde2_sp_d1_stad_kijk_<naam>.*
  TEXTS    every other key (hints, the two minigames, the clicks, the net, the boat)
  FTB      (key, titel, tekst, icon, stap)                  one quest per step

Tone (DESIGN 5b, CONTRACT_E2_C 5.1): pure comedy, warm and silly, guh details ("njeg", ears), the narrator with a French
touch. Own words only: no quote of the series; Guhdalf's line is the DESIGN's own (approved) parody.
"""

P = "gui.guhs.einde2_sp_d1_stad"
VERTELLER = "De Verteller"

SCENES = {
    "patguh": ("Patguh wordt wakker", {
        "intro": "Ah, Patguh Ster. Hij slaapt graag. En als hij klaar is met slapen... slaapt hij nog een keertje. Magnifique.",
        "snurk": "Zzzz... nog vijf minuutjes... zzzz... nog vijf kwalletjes...",
        "wakker": "Huh?! Is het al morgen? O nee, het is nog steeds vandaag. Gelukkig, daar was ik net aan gewend.",
        "jam": "Hé, jij bent de nieuwe van De Krokante Knabbel! Meneer Krabguh wil kwallenjam op de Knabbelburgers. "
               "En ik weet alles van kwallen. Bijna alles. Ik weet dat ze kwal heten.",
        "net": "Hier, een netje voor jou! Gewoon zachtjes tikken, dan floep, zit hij in de pot. Acht kwallen, dan is de pot vol.",
        "veld": "De Kwallenvelden zijn aan het eind van de straat, naar het westen. Wie het laatst is, is een rotte kwal! Njeg!",
    }, {"patguh": "Patguh", "verteller": VERTELLER}),
    "knipoog_rookguh": ("De grootste kwal", {
        "bloep": "Bloep. Bloep. Ik ben een kwal. Bloep.",
        "grootste": "HEBBES! Kijk dan! Dit is de grootste kwal die ik ooit gevangen heb!",
        "zucht": "...Zucht.",
        "netje": "Hé! Hij neemt mijn netje mee! ...Nou ja. Hij heeft het harder nodig.",
    }, {"patguh": "Patguh", "rookguh": "Rookguh"}),
    "kwallen_klaar": ("Een volle pot", {
        "held": "En zo, met acht kwallen in een pot, werd er een held geboren. Een kleverige held, njeg.",
        "acht": "ACHT! De pot is vol! Meneer Krabguh gaat dansen van blijdschap. Of geld tellen. Meestal geld tellen.",
        "jam": "Hier, drie potjes jam voor jou. En mijn reservebroek met bloemetjes! Die staat jou vast prachtig. Mij ook trouwens.",
        "examen": "O ja! Sponsguh doet vandaag zijn bootrijexamen. Alweer. Hij zit al bij de bootschool in het westen te trillen. "
                  "Ga jij hem helpen? Ik zou zelf gaan, maar ik moet nog uitrusten van het uitrusten.",
    }, {"patguh": "Patguh", "verteller": VERTELLER}),
    "pufguh": ("Examen nummer achtendertig", {
        "ondertussen": "Ondertussen, bij de bootschool... Juf Pufguh heeft al heel veel examens gezien. Te veel, zeggen haar zenuwen.",
        "nummer": "Sponsguh Kaasbroek. Examen nummer... achtendertig. Ik heb er een liedje over geschreven. Het is heel kort en het "
                  "eindigt met gillen.",
        "klaar": "Deze keer gaat het lukken, juf! Ik heb geoefend! In mijn hoofd! En kijk: ik heb een rij-maatje meegenomen!",
        "maatje": "Een rij-maatje? ...Prima. Jij stuurt, Sponsguh zit ernaast. Dan heeft hij zijn handen vrij om te gillen.",
        "regels": "Eén rondje over het parcours: volg de lichtjes, over het bruggetje en terug naar de finish. Je mag hooguit acht "
                  "keer ergens tegenaan botsen. Rustig aan, njeg?",
        "paniek": "Ik ben er klaar voor! ...Ben ik er klaar voor? Ik ben er... ietsjes klaar voor. Mijn oortjes trillen.",
        "boot": "Het lesbootje staat bij de startstreep, een stukje naar het noorden. Instappen, en vooral: ademen.",
    }, {"pufguh": "Juf Pufguh", "sponsguh": "Sponsguh", "verteller": VERTELLER}),
    "knipoog_guhdalf": ("Niet passeren", {
        "passeren": "Gij zult niet... passeren... zonder rijbewijs!",
        "omweg": "AAAH! Een tovenaar! Omweg! OMWEEEG!",
        "puf": "Pfffff... Daar ga ik weer.",
    }, {"guhdalf": "Guhdalf", "sponsguh": "Sponsguh", "pufguh": "Juf Pufguh"}),
    "examen_klaar": ("Voorlopig geslaagd", {
        "geslaagd": "Dat... was... GESLAAGD?!",
        "rijbewijs": "Geslaagd? GESLAAGD! Juf, ik heb mijn rijbewijs! Ik ga het inlijsten! En dan de lijst inlijsten!",
        "voorlopig": "Voorlopig, Sponsguh. Voor-lo-pig. Je mag alleen varen met je rij-maatje ernaast. Dat staat erop, in hele grote letters.",
        "team": "Hoor je dat? Wij zijn een team! Het beste bootteam van heel Bikinibodem!",
        "bel": "Maar ver weg, bij De Krokante Knabbel, klonk een bel. Geen gewone bel. De bel die nooit mocht klinken...",
        "alarm": "De alarmbel van meneer Krabguh! Dat betekent iets ergs. Of iets héél ergs! Snel, terug naar De Krokante Knabbel!",
    }, {"pufguh": "Juf Pufguh", "sponsguh": "Sponsguh", "verteller": VERTELLER}),
}

KIJK = {
    "patguh": ("Patguh's kruimelrots", "Patguh woont onder de grote kruimelrots aan de Schelpstraat. Hij ligt ervoor te slapen."),
    "kwallen": ("De Kwallenvelden", "Aan het westeinde van de Schelpstraat zweven de kruimelkwallen. Patguh wacht aan de rand."),
    "pufguh": ("De bootschool", "Juf Pufguh wacht bij de startstreep van haar pylonenparcours, aan de westkant van de stad."),
    "boot": ("Het lesbootje", "Het lesbootje staat klaar bij de startstreep. Klik erop om in te stappen; Sponsguh springt erbij."),
}

TEXTS = {
    # the hints of the halves of the two steps (%s = the direction)
    f"{P}.hint.patguh": "Loop naar Patguh: hij slaapt voor zijn grote kruimelrots aan de Schelpstraat, %s.",
    f"{P}.hint.kwallen": "Naar de Kwallenvelden aan het westeinde van de Schelpstraat, %s. Tik er acht kruimelkwallen met je netje!",
    f"{P}.hint.pufguh": "Loop naar juf Pufguh bij de startstreep van de bootschool, aan de westkant van de stad, %s.",
    f"{P}.hint.boot": "Klik op het lesbootje bij de startstreep, %s, en vaar het parcours.",
    # jellyfishing
    f"{P}.kwallen.titel": "Kwallen vangen!",
    f"{P}.kwallen.regel": "Tik een kwal met je netje (links- of rechtsklik). Vang er %s!",
    f"{P}.kwallen.teller": "Kwallen: %s/%s",
    f"{P}.kwallen.pauze": "Je pot staat op %s/%s. Kom terug naar het Kwallenveld om verder te vangen.",
    f"{P}.kwallen.niet_hier": "Patguh vangt alleen kwallen in het Kwallenveld naast hem, njeg.",
    f"{P}.kwallen.al": "Die zat al in je pot en glipte er weer uit. Probeer een andere!",
    f"{P}.kwallen.vol": "De pot is vol!",
    f"{P}.kwallen.vol_onder": "Acht kruimelkwallen, njeg!",
    f"{P}.kwallen.beloning": "Je krijgt drie potjes kwallenjam en Patguh's bloemetjesbroek (voor je guh)!",
    # the boating exam
    f"{P}.examen.titel": "Bootrijexamen!",
    f"{P}.examen.regel": "Volg de lichtjes. Hooguit %s keer botsen!",
    f"{P}.examen.teller": "Poortje %s/%s  -  Geraakt: %s/%s",
    f"{P}.examen.instappen": "Klik op het lesbootje om (weer) in te stappen.",
    f"{P}.examen.start_sponsguh": "Gordels om! O wacht, er zijn geen gordels. Handjes vast dan!",
    f"{P}.examen.gil.1": "AAAAAH!",
    f"{P}.examen.gil.2": "Mijn oortjes! Mijn oortjes zagen mijn leven voorbij flitsen!",
    f"{P}.examen.gil.3": "Dat was een pylon! Of een paal! Of mijn hele toekomst!",
    f"{P}.examen.gil.4": "Niks aan de hand! Niks aan de hand! NIKS AAN DE HAND!",
    f"{P}.examen.gil.5": "Ik zie mijn knabbelkoorvrucht al! Is dat de hemel?",
    f"{P}.examen.gil.6": "Iiiieeek! ...Sorry. Ga door. Iiiieeek.",
    f"{P}.examen.geraakt": "Geraakt! %s/%s",
    f"{P}.examen.mislukt": "Oei, te vaak gebotst!",
    f"{P}.examen.opnieuw": "Geen nood: het bootje staat weer bij de start.",
    f"{P}.examen.zucht": "Pffff... Nog een keertje dan. Ik blaas mezelf alvast een beetje op.",
    f"{P}.examen.geslaagd": "Geslaagd!",
    f"{P}.examen.geslaagd_onder": "Met maar %s keer botsen!",
    # clicks while the two steps run
    f"{P}.klik.patguh_veld": "Tik ze met je netje! Nog %s, dan is de pot vol. Ik tel mee. Op mijn vingers. Ik heb er niet zoveel.",
    f"{P}.klik.sponsguh_kwallen": "Kwallen vangen met Patguh? Dat is het allerleukste wat er is! Na Knabbelburgers bakken. En na bellen blazen.",
    f"{P}.klik.pufguh_boot": "Het lesbootje staat bij de startstreep. Volg de lichtjes, en als Sponsguh gilt: gewoon doorvaren.",
    f"{P}.klik.sponsguh_boot": "Ik ben er klaar voor! Stap in, rij-maatje!",
    f"{P}.klik.sponsguh_examen": "Mijn examen! Juf Pufguh wacht bij de bootschool. Ik kom eraan. Mijn knieën nog niet, maar de rest wel.",
    f"{P}.klik.patguh_examen": "Sponsguh doet weer examen. Ik zou zelf gaan kijken, maar dan moet ik opstaan.",
    # the net and the boat
    "item.guhs.einde2_sp_d1_stad_kwallennet": "Kwallennetje",
    "item.guhs.einde2_sp_d1_stad_kwallennet.lore": "Van Patguh gekregen. Zachtjes tikken, dan floep!",
    "entity.guhs.einde2_sp_d1_stad_lesboot": "Lesbootje",
}

# FTB: one quest per step (quest i is done when the player reaches step i + 1)
FTB = [
    ("einde2_sp_d1_stad_r5", "Kwallen vangen", "Patguh weet alles van kwallen. Bijna alles. Help hem acht &dkruimelkwallen&r te vangen "
     "voor de kwallenjam van De Krokante Knabbel.", "guhs:einde2_sp_d1_stad_kwallennet", 5),
    ("einde2_sp_d1_stad_r6", "Het bootrijexamen", "Sponsguh doet zijn bootrijexamen. Voor de achtendertigste keer. Jij bent zijn "
     "&drij-maatje&r: stuur het lesbootje van juf Pufguh over het pylonenparcours.", "guhs:einde2_bouw_bikinibodem_pylon", 6),
]
