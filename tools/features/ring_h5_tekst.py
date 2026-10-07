"""
bbq2 (ring-h5) - every Dutch text of chapter 5, "De Zwarte Roosterpoort" (the source language; English comes in phase 3).
Signs in the valley are written where they stand (ring_h5_bouw.py, prefix sign.guhs.ring_h5).
"""

STRUCTUUR = ("De Zwarte Roosterpoort", "De poort naar het land van Sausron, met de toren van het Oog erachter. Dicht. Heel erg dicht (Guhbarbecuether)")

TEKSTEN = {
    # --- what is in the valley -----------------------------------------------------------------------------------------------
    "entity.guhs.oog_van_sausron": "Het Oog van Sausron",
    "entity.guhs.ringh5_roosterwachter": "Roosterwachter",
    "block.guhs.oog_van_sausron_beeldje": "Beeldje van het Oog van Sausron",
    "block.guhs.ringh5_poortrooster": "Poortrooster",
    "block.guhs.ringh5_blik": "Blik van het Oog",
    "gui.guhs.ringh5.beeldje.0": "Het Oog knippert. Hij heeft gewoon trek, njeg.",
    "gui.guhs.ringh5.beeldje.1": "Hij kijkt naar je broekzak. Daar zat ooit een knabbel in.",
    "gui.guhs.ringh5.beeldje.2": "Sausron spint. Echt waar. Zachtjes.",
    "gui.guhs.ringh5.beeldje.3": "Eén stukje ring en hij was tevreden. Had dat nou meteen gezegd.",
    "gui.guhs.ringh5.beeldje.4": "Het Oog ziet alles. Behalve rotsen. En de afwas.",
    # --- the Eye, the guards ---------------------------------------------------------------------------------------------------
    "quest.guhs.ringh5.oog.gezien": "Het Oog kijkt naar je! Weg uit het licht, njeg!",
    "quest.guhs.ringh5.oog.ring": "Het Oog voelt de ring... Doe hem af, snel!",
    "quest.guhs.ringh5.wachter.gezien": "Hé! Jij daar! Staan blijven, njeg!",
    "quest.guhs.ring.terug.poortwachter": "Een Roosterwachter zag je en duwde je terug naar je rustpunt. \"Verboden voor guhs, staat er toch?\"",
    "quest.guhs.ringh5.wachter.snuif.0": "*snuf snuf* Ruik jij ook knabbel? Ik ruik knabbel. Ik zie geen knabbel.",
    "quest.guhs.ringh5.wachter.snuif.1": "Wie zei daar \"njeg\"? ... Niemand. Ik word oud.",
    "quest.guhs.ringh5.wachter.snuif.2": "Er tocht hier iets. Iets met een ring erom. Ach, zal wel.",
    "quest.guhs.ringh5.wachter.weg": "Wegwezen! Dit hek is alleen voor personeel. En voor wie we niet zien.",
    "quest.guhs.ringh5.wachter.na.0": "Loop maar door, Ringdrager. Bedankt voor het hapje, vahoeg!",
    "quest.guhs.ringh5.wachter.na.1": "De baas slaapt eindelijk. Wij hebben de hele dag pauze. Njeg.",
    # --- Boromika at the camp ----------------------------------------------------------------------------------------------------
    "quest.guhs.ringh5.boromika.hallo": "Ha, de ringdrager. Kom bij het vuur zitten. Ik houd de wacht. En de ring in de gaten.",
    "quest.guhs.ringh5.boromika.spijt.0": "Kijk me niet zo aan. Ik schaam me al genoeg voor drie Mika's.",
    "quest.guhs.ringh5.boromika.spijt.1": "Ik blijf hier op wacht. Ver weg van die ring. Het is beter zo, njeg.",
    "quest.guhs.ringh5.boromika.spijt.2": "Vertrouw die dunne niet te veel. Hij kijkt naar de ring zoals ik naar de ring keek.",
    "quest.guhs.ringh5.boromika.na.0": "Gefrituurd en gedeeld! En ik kreeg ook een stukje. Dat had ik niet verdiend. Het was wel lekker.",
    "quest.guhs.ringh5.boromika.na.1": "Ik houd nog steeds de wacht. Er valt niks meer te bewaken, maar ik ben er goed in geworden.",
    # --- Smikagol becomes the guide ----------------------------------------------------------------------------------------------
    "quest.guhs.ringh5.geritsel": "Er ritselt iets bij de proviand. Iemand zit aan de knabbels!",
    "quest.guhs.ringh5.smikagol.betrapt": "Niet slaan! Niet slaan! Wij stelen niet, wij... proeven alleen. Het rook naar ons vadsje, njeg. "
                                          "Lieve guh draagt het vadsje, ja? Wij weten een weg langs het Oog. Een sluipweg. Wij wijzen hem!",
    "gui.guhs.ringh5.smikagol.ja": "Goed. Wijs ons de weg.",
    "gui.guhs.ringh5.smikagol.nee": "Blijf met je poten van de proviand af!",
    "quest.guhs.ringh5.smikagol.zweer": "Wij zweren het! Op het vadsje! Smikagol wijst de weg. Achterom, door het Roosterpoortje. Volg ons, vadsje... eh, baasje.",
    "quest.guhs.ringh5.smikagol.toe_nou": "Toe nou, toe nou. Zonder Smikagol kijkt het Oog je zo terug naar huis. Klik maar op ons als je je bedenkt, njeg.",
    "quest.guhs.ringh5.sam.vertrouw_niet": "Ik vertrouw hem voor geen knabbel. Maar een weg is een weg. Ik houd mijn koekenpan bij de poot.",
    "quest.guhs.ringh5.smikagol.stap.3": "De trap op, de rug over. Boven ziet baasje waar wij heen moeten. Niet schrikken.",
    "quest.guhs.ringh5.smikagol.stap.4": "Het Asveld. Zie je het licht op de grond? Daar kijkt hij. Ren van muurtje naar muurtje als het voorbij is. Wij wachten bij elk muurtje.",
    "quest.guhs.ringh5.smikagol.stap.5": "Kaal, kaal, alles kaal. Geen muurtjes meer. Maar baasje heeft het manteltje! Bukken en stilstaan als het licht komt. Stil als een steen.",
    "quest.guhs.ringh5.smikagol.stap.6": "De Schaduwlaan. Hier kijkt het Oog niet. Eerst de rook: laat het flesje flitsen, dan blaast hij weg. Daarna de ruiters: flits ze blind, of wees een rots tot ze voorbij zijn.",
    "quest.guhs.ringh5.smikagol.stap.7": "Roosterwachters. Mika's! Die zien geen ring... ssst. Bij de schedelpaal het vadsje om, door het hek, en meteen weer af. Niet eerder om, anders ruiken de ruiters het. Niet langer, anders heeft het Oog je.",
    "quest.guhs.ringh5.smikagol.stap.8": "Daar, daar! Het Roosterpoortje. Loop maar, baasje. Wij peuteren het slot wel open. Wij zijn goed met slotjes.",
    "quest.guhs.ringh5.smikagol.stap.9": "Open! Open! Rennen, baasje, de gang door, voor de witte klaar is met zijn vuurwerk!",
    "quest.guhs.ringh5.smikagol.klaar": "Langs het Oog, langs de Negen, langs de wachters. Goed gedaan, baasje. Nu alleen nog de berg op. Wij weten een trap... een héél lange trap. Njeg.",
    # (the way on Smikagol names when the chapter is done: Plekken.UITGANGEN, the one with the least rock behind it)
    "quest.guhs.ringh5.smikagol.verder.0": "En nu rechtdoor, baasje, aan de achterkant het dal uit. Volg je neusje: het ruikt daar naar frituur. Lekker vet. Njeg.",
    "quest.guhs.ringh5.smikagol.verder.1": "Niet rechtdoor, baasje, daar zit het dal potdicht van de rots. Het gangetje hier vlakbij, door de rotswand bij het lantaarntje. Daar ruikt het naar frituur!",
    "quest.guhs.ringh5.smikagol.verder.2": "Niet rechtdoor, baasje, daar zit rots. Onder de toren van het Oog door, ssst, zachtjesss... en aan de overkant het gangetje door de rotswand. Daar ruikt het naar frituur!",
    "quest.guhs.ringh5.sam.klaar": "Twee stoofpotjes voor de klim. En ik blijf achter die griezel lopen, dan zie ik wat hij doet.",
    # --- the lane ------------------------------------------------------------------------------------------------------------------
    "quest.guhs.ringh5.rook.dicht": "Je ziet geen poot voor ogen in die rook. Had je niet iets dat licht geeft?",
    "quest.guhs.ringh5.rook.weg": "Het Lichtflesje flitst en de rook waait weg. Vahoeg, de laan is vrij!",
    "quest.guhs.ringh5.poortje.dicht": "Het Roosterpoortje zit op slot. Kloppen heeft geen zin, njeg.",
    # --- the Guhdex: where to go, what you get ---------------------------------------------------------------------------------------
    "gui.guhs.ringh5.doel.0": "Het kamp voor de Zwarte Roosterpoort",
    "gui.guhs.ringh5.doel.1": "Boromika bij het kampvuur",
    "gui.guhs.ringh5.doel.2": "De proviand bij het kamp",
    "gui.guhs.ringh5.doel.3": "De uitkijkrug",
    "gui.guhs.ringh5.doel.4": "De Slakkenhut",
    "gui.guhs.ringh5.doel.5": "De Holte",
    "gui.guhs.ringh5.doel.6": "De schedelpaal",
    "gui.guhs.ringh5.doel.7": "Het vuurtje bij het Roosterpoortje",
    "gui.guhs.ringh5.doel.8": "Het Roosterpoortje",
    "gui.guhs.ringh5.doel.9": "Het vuurtje achter de muur",
    "gui.guhs.ringh5.doel.ingang": "De ingang van het dal van de Zwarte Roosterpoort",
    "gui.guhs.ringh5.beloning.gids": "Smikagol wijst je de weg",
}

# --- the questline (Guhdex tab Verhalen, the travel map, the objective line) --------------------------------------------------------
LIJN_NAAM = "De Zwarte Roosterpoort"
LIJN_UITLEG = ("De poort naar het land van Sausron zit potdicht en vanaf zijn toren kijkt het Oog mee. Smikagol weet een sluipweg. Zegt hij. "
               "Wie gezien wordt, staat zo weer bij het laatste rustvuurtje: je verliest nooit iets.")
# (stapnaam, nu, waar) for the steps 0..9
STAPPEN = [
    ("Naar de Zwarte Roosterpoort", "Loop naar het kamp aan de rand van het Asdal. Je Superkompas en Sam-guh wijzen de weg.",
     "Het kamp voor de Zwarte Roosterpoort, in het Asdal van de Guhbarbecuether"),
    ("Boromika bij het vuur", "Praat met Boromika bij het kampvuur. Hij kijkt wel erg lang naar je ring.", "Het kamp voor de Zwarte Roosterpoort"),
    ("Een dief bij de proviand", "Er ritselt iets bij de zakken met proviand. Ga kijken en klik op de dief.", "Achter de rots bij het kamp"),
    ("De uitkijkrug", "Volg Smikagol de trap op, de rug over. Boven zie je waar je heen moet.", "De trap over de rug achter het kamp"),
    ("Het Asveld", "Sluip over het Asveld. Blijf uit het licht van het Oog: schuil achter een muurtje of onder een afdakje tot het voorbij is.",
     "Het Asveld: schuin naar links, naar de Slakkenhut bij de rotswand"),
    ("De Kale Vlakte", "Hier is niks om achter te zitten. Komt het licht eraan? Buk en sta stil met het Elfenmanteltje bij je: dan ben je een rots.",
     "De Kale Vlakte: naar rechts, naar de Holte in de rotswand"),
    ("De Schaduwlaan", "Blaas de rook weg met het Lichtflesje en glip langs de twee ruiters van de Negen. Een flits verblindt ze, een rots zien ze niet.",
     "De laan onder de muur, tot aan de schedelpaal"),
    ("Het Wachthek", "Drie Roosterwachters. Doe bij de schedelpaal de Knabbelring om, loop door het hek en doe hem er meteen weer af. Niet treuzelen: het Oog voelt de ring!",
     "Het Wachthek: naar het vuurtje bij het Roosterpoortje"),
    ("Het Roosterpoortje", "Loop naar het kleine poortje in de muur.", "Het Roosterpoortje, links in de muur"),
    ("Achter de muur", "Het poortje is open! Ren door de gang naar het vuurtje achter de muur.", "Achter de Zwarte Roosterpoort"),
]
KLAAR = ("Je bent langs het Oog geslopen. Vahoeg! Nu alleen de Frituurberg nog.", "Achter de Zwarte Roosterpoort")
KORT = {"0": "Naar het kamp bij de Zwarte Roosterpoort", "1": "Praat met Boromika bij het vuur", "2": "Kijk wie er bij de proviand ritselt",
        "3": "Volg Smikagol de rug op", "4": "Het Asveld over: blijf uit het licht", "5": "De Kale Vlakte over: buk en word een rots",
        "6": "Rook wegblazen, langs de ruiters", "7": "Ring om, door het Wachthek, ring af", "8": "Naar het Roosterpoortje",
        "9": "Door de gang naar het vuurtje"}

# --- the narrator card -----------------------------------------------------------------------------------------------------------------
KAART_TITEL = "Hoofdstuk 5: De Zwarte Roosterpoort"
KAART_REGELS = [
    "De elfenboten brachten je tot waar de sausrivier in de as verdwijnt: het Asdal.",
    "Aan het eind van het dal staat een zwarte muur met één poort. Die poort is een rooster, en hij is dicht.",
    "Erboven, op zijn toren, staart het Oog van Sausron. Hij zoekt zijn knabbel. Hij heeft al héél lang trek.",
    "Wie hij ziet, kijkt hij zo terug naar het laatste vuurtje. Dus: niet gezien worden, njeg.",
]

# --- the scenes: id -> (title of the replay button, {key: line}, {actor: name}) ----------------------------------------------------------
SCENES = {
    "ringh5_boromika": ("Boromika en de ring", {
        "begin": "Aan de rand van het Asdal brandt een vuurtje. Boromika houdt de wacht. Vooral over jouw broekzak.",
        "tonen": "Laat die ring nog eens zien, kleine guh. Hij is zo... rond. En zo knapperig.",
        "hapje": "Eén hapje maar. Wie merkt dat nou? Geef hem aan mij, ik bewaar hem. Veilig. In mijn buik.",
        "foei": "Af! Foei! Poten thuis! Njeg!",
        "wat_deed_ik": "Wat... wat deed ik nou? Mijn poot ging helemaal vanzelf.",
        "vergeef": "Vergeef me. Dat was de ring, niet ik. Nou ja. Een klein beetje ik.",
        "wacht": "Ik maak het goed. Ik blijf hier en houd de wacht, ver bij die ring vandaan. Ga, voor ik weer trek krijg.",
    }, {"boromika": "Boromika", "sam": "Sam-guh"}),
    "ringh5_oog": ("Het Oog van Sausron", {
        "daar": "Daar, baasje. Aan het eind van het dal. De Zwarte Roosterpoort.",
        "dicht": "Dichter dan dicht. Een rooster zo hoog als een huis. En erboven...",
        "oog": "Het Oog van Sausron. Hij is zijn knabbel kwijt. Hij heeft trek. Al héél lang trek.",
        "licht": "Ziet baasje het licht op de grond? Daar kijkt hij. Blijf uit het licht. Achter muurtjes, onder afdakjes. Dan ziet hij niks.",
        "sluipweg": "Smikagol weet een sluipweg. Achterom, door het Roosterpoortje. Volg ons. Zachtjes, zachtjes...",
    }, {"smikagol": "Smikagol"}),
    "ringh5_guhdalf": ("De terugkeer van Guhdalf de Witte", {
        "op_slot": "Op slot! Nee, nee, nee... Wacht, baasje. Wij peuteren. Wij peuteren heel hard.",
        "hoeven": "Hoefgetrappel in de laan. De Negen hebben de ring geroken.",
        "schiet_op": "Ze komen eraan! Schiet op, griezel!",
        "achteruit": "Achteruit, knekels! Ik ben Guhdalf de Witte. En ik heb nét gegeten.",
        "verblind": "Het licht van zijn staf is feller dan honderd Lichtflesjes. De Negen zien geen poot meer voor ogen.",
        "vuurwerk": "Hé, Sausron! Hierzo! Ik heb vuurwerk bij me!",
        "ren": "Ren, kleine ringdrager! Het Oog kijkt nu naar mij. Het poortje door. Njeg!",
    }, {"smikagol": "Smikagol", "sam": "Sam-guh", "guhdalf": "Guhdalf de Witte"}),
}

# --- advancements: (name, parent, icon, frame, title, text) in the tab knabbelring; every one also as a hidden quest/<name> ----------------
ADVANCEMENTS = [
    ("ring_h5_gids", "ring_gekregen", "minecraft:cod", "task", "Wij zweren het op het vadsje", "Laat Smikagol je de weg wijzen langs de Zwarte Roosterpoort"),
    ("ring_h5_rook", "ring_h5_gids", "guhs:lichtflesje", "task", "Wie het licht heeft", "Blaas de rook van de Schaduwlaan weg met het Lichtflesje"),
    ("ring_h5_langs_wachters", "ring_h5_gids", "guhs:knabbelring", "task", "Ruik jij ook knabbel?", "Loop met de ring om langs een Roosterwachter"),
    ("ring_h5_klaar", "ring_h5_gids", "guhs:oog_van_sausron_beeldje", "goal", "Achterom", "Sluip langs het Oog van Sausron, de Negen en de Roosterwachters"),
]
VERBORGEN = ["ring_h5_betrapt"]

# --- the FTB quests of the section "De Zwarte Roosterpoort": (title, text, icon) for the steps 1..10 ---------------------------------------
FTB = [
    ("Aan de rand van het Asdal", "De reis gaat verder naar het &6Asdal&r. Je Superkompas ('Mijn verhaal') en Sam-guh wijzen de weg naar het kamp voor de "
     "&cZwarte Roosterpoort&r. Daar brandt een rustvuurtje.", "guhs:ring_rustvuur"),
    ("Eén hapje maar", "Bij het vuur zit &dBoromika&r, een Mika die écht wil helpen. Praat met hem. Hij kijkt alleen wel erg lang naar je ring...",
     "guhs:knabbelring"),
    ("Wij zweren het op het vadsje", "Er ritselt iets bij de proviand. Het is &dSmikagol&r, een dunne Mika die zijn 'vadsje' kwijt is. Hij weet een "
     "sluipweg langs het Oog. Klik op hem en laat hem de weg wijzen. Sam-guh vertrouwt hem voor geen knabbel.", "minecraft:cod"),
    ("Het Oog van Sausron", "Volg Smikagol de rug op. Daarboven zie je het voor het eerst: de poort, de toren en het &cOog&r. Zijn blik is een "
     "&6lichtvlek op de grond&r. Daar kijkt hij.", "minecraft:ender_eye"),
    ("Van muurtje naar muurtje", "Steek het &6Asveld&r over. Het licht van het Oog loopt heen en weer: wacht achter een muurtje of onder een afdakje "
     "tot het voorbij is en ren dan naar het volgende. Gezien? Dan sta je weer bij je laatste rustvuurtje. Je verliest niks.", "guhs:houtskoolsteen_stenen"),
    ("Ik ben een rots. Echt.", "Op de &6Kale Vlakte&r is niks om achter te zitten. Met het &aElfenmanteltje&r bij je: &6buk en sta stil&r als het licht "
     "eraan komt. Het Oog vindt rotsen saai.", "guhs:elfenmanteltje"),
    ("De Schaduwlaan", "Onder de muur kijkt het Oog niet. Maar er hangt &7rook&r (laat het &bLichtflesje&r flitsen: weg is hij) en er rijden twee "
     "ruiters van de &cNegen&r. Een flits verblindt ze, een rots rijden ze voorbij.", "guhs:lichtflesje"),
    ("Ruik jij ook knabbel?", "Drie &cRoosterwachters&r bij het Wachthek. Mika's zien geen ringdrager: doe bij de schedelpaal de &6Knabbelring&r om, "
     "loop door het hek en doe hem er &6meteen&r weer af. Het Oog voelt de ring, en de Negen ruiken hem.", "minecraft:iron_bars"),
    ("Guhdalf de Witte", "Het &6Roosterpoortje&r zit op slot en de Negen komen eraan. Maar kijk eens wie daar op de muur staat...",
     "minecraft:firework_rocket"),
    ("Achterom", "Het poortje door, de gang onder de muur door. Je bent langs het Oog van Sausron geslopen, vahoeg! Sam-guh geeft je twee stoofpotjes "
     "voor de klim: nu alleen de &cFrituurberg&r nog.", "guhs:oog_van_sausron_beeldje"),
]
