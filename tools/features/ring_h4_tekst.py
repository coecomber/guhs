"""
bbq2 (ring-h4) - every Dutch text of chapter 4 of the Knabbelring, "De Spiegel van Guhladriel" (the source language; the
English overlay is phase 3's). Names of this chapter: Guhlórien (the golden wood), Caras Guhladhon (the tree city), de
Grote Spies (the great tree), de Guhduin (the river), de Arguhnath (the two statues), de Sausval van Rauguhs (where the
river goes on without you).
"""
NAAM = "De Spiegel van Guhladriel"
STRUCTUUR = ("Caras Guhladhon", "De boomstad van Vrouwe Guhladriel in het gouden woud Guhlórien, aan de rivier de Guhduin (Guhbarbecuether)")

# --- the questline (gui.guhs.verhalen.ring_h4.*): (stapnaam, nu, waar) per step, the short objective line per step ---------------
UITLEG = ("Na de mijn is iedereen moe en verdrietig. In het gouden woud Guhlórien woont Vrouwe Guhladriel hoog in de bomen. Rust uit, kijk in "
          "haar spiegel als je durft, en vaar dan met de elfenbootjes de Guhduin af. Njeg.")
WAAR = "Caras Guhladhon, de boomstad in de Guhbarbecuether"
STAPPEN = [
    ("Naar het gouden woud", "Loop naar de boomstad van Guhladriel. Het Superkompas en Sam-guh wijzen de weg.", "Het gouden woud Guhlórien, een flink eind lopen"),
    ("Leguhlas bij de poort", "Praat met Leguhlas bij de poort van de boomstad.", WAAR),
    ("Omhoog naar Guhladriel", "Klim de grote wenteltrap rond de Grote Spies op en praat met Vrouwe Guhladriel in haar zaal.", WAAR),
    ("Uitrusten", "Ga naar de gastenvlonder (de boom bij de poort, de trap omhoog) en kruip bij het Rustvuurtje.", WAAR),
    ("De Spiegel", "Loop naar het groene dal in de boomstad en kijk in de Spiegel van Guhladriel (rechtsklik).", WAAR),
    ("De drie gaven", "Praat met Guhladriel bij de spiegel. Ze heeft iets voor je. Drie ietsen.", WAAR),
    ("De Guhduin af", "Loop naar de steiger en stap in het elfenbootje van Leguhlas. Blijf zitten tot de overkant.", WAAR),
]
KLAAR = ("Je bent de Guhduin afgevaren, langs de Arguhnath. Vahoeg!", "De aanlegplaats aan de Guhduin")
KORT = {"0": "Loop naar de boomstad van Guhladriel", "1": "Praat met Leguhlas bij de poort", "2": "Klim naar Guhladriel in de Grote Spies",
        "3": "Rust uit op de gastenvlonder", "4": "Kijk in de Spiegel van Guhladriel", "5": "Praat met Guhladriel bij de spiegel",
        "6": "Stap in het elfenbootje bij de steiger", "klaar": "Hoofdstuk 4 is klaar. Vahoeg!"}

# --- the narrator card (gui.guhs.verhaal.kaart.ring_h4.*) ------------------------------------------------------------------------
KAART_REGELS = [
    "Guhdalf is in de diepte gevallen. Niemand heeft trek. Zelfs Pippguh niet, en dat zegt wat.",
    "Het gezelschap strompelt de mijn uit en komt in een woud waar de satébomen goud kleuren: Guhlórien.",
    "Hoog in de takken woont Vrouwe Guhladriel. Ze weet alles. Ook wat jij gisteren stiekem hebt opgegeten.",
    "Rust uit, ringdrager. Morgen wacht de rivier. En vannacht... een spiegel. Njeg.",
]

# --- the cutscene (scene.guhs.ringh4_spiegel.*) -----------------------------------------------------------------------------------
SCENE_TITEL = "De Spiegel van Guhladriel"
SCENE = {
    "nacht": "Midden in de nacht. Zachte pootjes op het mos. Guhladriel loopt naar het dal...",
    "kijk": "Wil je in de Spiegel kijken, ringdrager?",
    "snoet": "Ik zie alleen mijn eigen snoet, baas. Er zit nog stoofpot op, njeg.",
    "dingen": "De Spiegel toont dingen die waren, dingen die zijn... en dingen die nog gebakken moeten worden.",
    "gouw": "Je ziet de Knabbelgouw. De feesttent. De lange tafels...",
    "kruimels": "...en overal Mika's. Ze eten álle knabbels op. Zelfs de kruimels. Zelfs die onder de bank.",
    "oog": "Dan wordt het water rood. Een OOG. Een reusachtig Mika-oog, en het kijkt recht naar jou!",
    "trek": "Het Oog van Sausron: \"Ik zie je... Heb jij mijn knabbel? Ik heb alleen maar een béétje trek!\"",
    "weet": "Ik weet wat je zag. Het staat ook in mijn gedachten. En het knort ook in mijn maag.",
    "aanbod": "Wil jij de ring dan? Hier. Dan ben ik er lekker vanaf, njeg.",
    "koningin": "In plaats van een Donkere Heer krijg je een KONINGIN! Niet duister, maar ROND en VADS als de dageraad!",
    "houden": "Iedereen zal van mij houden... en mij KNABBELS brengen! Emmers vol!",
    "test": "...Nee. Ik doorsta de proef. Ik blijf klein, ik blijf Guhladriel, en ik eet gewoon mijn eigen knabbels.",
    "bed": "Mag ik nu weer naar bed, baas? Njeg.",
}
SCENE_NAMEN = {"guhladriel": "Guhladriel", "sam": "Sam-guh"}

# --- everything else -------------------------------------------------------------------------------------------------------------
Q = "quest.guhs.ringh4."
TEKSTEN = {
    "block.guhs.ringh4_spiegel": "Spiegel van Guhladriel",
    "block.guhs.ringh4_spiegel.lore": "Een zilveren schaal met heel stil water. Toont wat was, wat is, en wat je vanavond eet.",
    "entity.guhs.ringh4_elfenbootje": "Elfenbootje",
    # the compass's name of the spot a step sends you to
    "gui.guhs.ringh4.doel.poort": "De poort van Caras Guhladhon",
    "gui.guhs.ringh4.doel.leguhlas_poort": "Leguhlas bij de poort",
    "gui.guhs.ringh4.doel.zaal": "De zaal van Guhladriel",
    "gui.guhs.ringh4.doel.gast": "De gastenvlonder",
    "gui.guhs.ringh4.doel.spiegel": "De Spiegel van Guhladriel",
    "gui.guhs.ringh4.doel.dal": "Guhladriel bij de spiegel",
    "gui.guhs.ringh4.doel.steiger": "De steiger met de elfenbootjes",
    # what happens by itself
    Q + "aankomst": "Je bent in Guhlórien. Bij de poort staat een bekende elf met heel goede ogen. En heel veel meningen.",
    Q + "rust.0": "Je kruipt bij het vuurtje. Sam-guh snurkt al voordat zijn pannen de grond raken.",
    Q + "rust.1": "Je slaapt als een roosje. Als een rond, vads roosje. Tot je midden in de nacht zachte pootjes hoort...",
    Q + "rust.2": "Guhladriel loopt naar het groene dal. Daar staat haar Spiegel.",
    Q + "klaar": "Hoofdstuk 4 is klaar! Voor je ligt het Asdal. Ergens daar staat de Zwarte Roosterpoort.",
    Q + "zegen": "Guhladriels zegen: in haar stad val je zo zacht als een blaadje. Een vads blaadje.",
    Q + "gaven_gekregen": "Je hebt de drie gaven van Guhladriel: het Lichtflesje, het Elfenmanteltje en het Elfentouw.",
    Q + "hint.zaal": "klim de grote wenteltrap rond de Grote Spies op, naar Guhladriel",
    Q + "hint.rust": "rust uit op de gastenvlonder (de boom bij de poort, bij het Rustvuurtje)",
    Q + "hint.spiegel": "kijk in de Spiegel in het groene dal (rechtsklik)",
    Q + "hint.gaven": "praat met Guhladriel bij de spiegel",
    Q + "hint.boot": "stap bij de steiger in het elfenbootje van Leguhlas",
    # Leguhlas at the gate
    Q + "leguhlas.poort.wacht": "Momentje. Mijn elfenogen zien dat je nog ergens naar kijkt.",
    Q + "leguhlas.poort.welkom.0": "Welkom in Caras Guhladhon! Ik zag je al aankomen. Drie dagen geleden. Je liep toen nog de andere kant op.",
    Q + "leguhlas.poort.welkom.1": "Vrouwe Guhladriel wacht boven in de Grote Spies. De wenteltrap op, tot je niet meer hoger kunt. Niet naar beneden kijken. Of juist wel, het is mooi.",
    Q + "leguhlas.poort.later": "Dit is het gouden woud. Niks aanraken met vette pootjes, njeg.",
    # Guhladriel
    Q + "guhladriel.welkom.0": "Welkom, ringdrager. Ik wist dat je zou komen. Ik wist ook dat je onderweg twee keer bent verdwaald.",
    Q + "guhladriel.welkom.1": "Jullie rouwen om Guhdalf. Wees niet bang: een tovenaar valt nooit zomaar. Hij valt precies wanneer hij dat van plan is.",
    Q + "guhladriel.welkom.2": "Maar nu ben je moe. Op de gastenvlonder brandt een vuurtje en ligt een slaapzak die naar bloemetjes ruikt. Ga. Slaap.",
    Q + "guhladriel.rust": "Je valt bijna om van de slaap. Eerst rusten, op de gastenvlonder. Daarna praten we, njeg.",
    Q + "guhladriel.spiegel": "De Spiegel staat in het dal, tussen de witte bloemetjes. Kijk erin. Als je durft.",
    Q + "guhladriel.gaven.0": "Je reis gaat verder, en ik laat niemand met lege pootjes gaan. Drie gaven.",
    Q + "guhladriel.gaven.lichtflesje": "Het Lichtflesje. Het licht van onze liefste ster, in een potje. Blaast rook weg en verblindt de Negen. Niet opdrinken.",
    Q + "guhladriel.gaven.elfenmanteltje": "Het Elfenmanteltje. Buk, sta heel stil, en het Oog denkt dat je een rots bent. Een vadsige rots, maar toch.",
    Q + "guhladriel.gaven.elfentouw": "En het Elfentouw. Het trekt je naar elke haak die je ziet. Het kriebelt niet, zegt Sam-guh.",
    Q + "guhladriel.gaven.1": "Bij de steiger wacht Leguhlas met de bootjes. Vaarwel, ringdrager. Mogen je knabbels nooit op raken.",
    Q + "guhladriel.opnieuw": "Je was een gave kwijt. Ik wist het al voordat jij het wist. Hier, een nieuwe. En nu op je spullen letten, njeg.",
    Q + "guhladriel.vaarwel": "De rivier wacht. Leguhlas ook, en die wacht niet graag.",
    Q + "guhladriel.na.0": "Je bent terug. Dat wist ik. De thee staat al koud te worden.",
    Q + "guhladriel.na.1": "De Spiegel is rustig tegenwoordig. Hij laat vooral zien wat we vanavond eten.",
    Q + "guhladriel.na.2": "Ik ben klein gebleven. Het bevalt me goed. Kleine guhs passen beter in een hangmat.",
    # the mirror
    Q + "spiegel.donker": "De schaal is leeg en donker. Guhladriel moet hem eerst vullen, njeg.",
    Q + "spiegel.eigen_snoet": "Je ziet alleen je eigen snoet. Mooi hoor.",
    # Leguhlas and Gimguh on the quay
    Q + "leguhlas.steiger.later": "Deze bootjes zijn van elfenhout. Ze zinken nooit. Tenzij Gimguh erin gaat staan.",
    Q + "leguhlas.steiger.wacht.0": "Eerst naar Vrouwe Guhladriel, boven in de Grote Spies. De bootjes lopen niet weg. Varen wel, maar niet zonder mij.",
    Q + "leguhlas.steiger.wacht.1": "Eerst naar Vrouwe Guhladriel, boven in de Grote Spies. De bootjes lopen niet weg. Varen wel, maar niet zonder mij.",
    Q + "leguhlas.steiger.wacht.2": "Vrouwe Guhladriel wacht op je, boven. Ik zie haar vanaf hier zitten. Ze kijkt op haar klokje.",
    Q + "leguhlas.steiger.wacht.3": "Je hebt wallen tot op je knieën. Eerst slapen, op de gastenvlonder. Dan varen.",
    Q + "leguhlas.steiger.wacht.4": "Heb je al in de Spiegel gekeken? Het dal in. Ik durf zelf niet meer: ik zag de vorige keer Gimguh in bad.",
    Q + "leguhlas.steiger.wacht.5": "Guhladriel heeft nog iets voor je, bij de spiegel. Niemand vaart hier weg zonder cadeautjes.",
    Q + "leguhlas.steiger.bootje_weg": "Het bootje is even de rivier af. Het komt zo terug. Elfenbootjes weten de weg.",
    Q + "gimguh.steiger.0": "Een dwergguh hoort niet op het water. Een dwergguh hoort ónder de grond. Met een boterham.",
    Q + "gimguh.steiger.1": "Die elf zegt dat hij drieënveertig knabbels heeft. Ik heb er vierenveertig. Eentje zit in mijn baard.",
    Q + "gimguh.steiger.2": "Ik heb Vrouwe Guhladriel om één haartje gevraagd. Ze gaf er drie! En een knabbel. Ik ga nooit meer weg.",
    Q + "gimguh.steiger.3": "Als dat bootje wiebelt, ga ik op de bodem liggen. Dat is geen angst. Dat is ballast.",
    # the boat trip
    Q + "vaart.nog_niet": "Nog niet, njeg. Eerst rusten, de Spiegel en de gaven van Guhladriel. Dan varen we.",
    Q + "vaart.instappen": "Stap maar in. Ga zitten, hou je staart binnenboord. We vertrekken zo!",
    Q + "vaart.erbij": "Je schuift erbij in het bootje. Gezellig.",
    Q + "vaart.deco": "Dit bootje is van Guhladriel zelf. Afblijven, njeg.",
    Q + "vaart.terug_nee": "Dit bootje vaart terug naar de boomstad. Jij moet de andere kant op, ringdrager.",
    Q + "vaart.terug": "Het elfenbootje vaart je terug naar de boomstad. Het weet de weg.",
    Q + "vaart.terug_er": "Terug in Caras Guhladhon.",
    Q + "vaart.nog_eens": "Nog een rondje? Kan altijd. De beelden blijven groot.",
    Q + "vaart.blijf_zitten": "Blijf zitten, njeg! Niet uitstappen tijdens het varen.",
    Q + "vaart.af": "Daar gaan we. De Guhduin: de langste sausrivier van de hele Guhbarbecuether. Niet met je pootjes roeren.",
    Q + "vaart.water": "Het wiebelt. Zeg tegen die elf dat het wiebelt. Ik zeg het niet, ik praat niet met hem.",
    Q + "vaart.rivier": "Rustig maar, Gimguh. Het is kaassaus. Als je erin valt, eet je je gewoon een weg naar de kant.",
    Q + "vaart.kijk": "Kijk! Daar, om de bocht! De Arguhnath, de Pilaren van de Guhkoningen!",
    Q + "vaart.groot": "...Njeg. Die zijn GROOT. Zelfs hun oren zijn groter dan mijn hele huis.",
    Q + "vaart.koningen": "De oude Guhkoningen, de voorouders van Araguh. Ze steken hun pootje op: tot hier, en geen kruimel verder.",
    Q + "vaart.sam": "Ik kan niet zwemmen, baas. Maar als het saus is, wil ik het best proberen, njeg.",
    Q + "vaart.tel": "Vierenveertig knabbels. Ik heb net onder de bank gekeken. Ik sta voor.",
    Q + "vaart.tel_terug": "Die was van mij. Ik had hem daar neergelegd. Voor straks. Drieënveertig alle twee, dus.",
    Q + "vaart.aanleg": "Daar is de aanlegplaats. Verder kan niemand varen: achter de bocht stort de Guhduin naar beneden, de Sausval van Rauguhs.",
    Q + "vaart.afscheid.0": "Hier ga jij aan land, ringdrager. Aan de overkant begint het Asdal, en daar staat de Zwarte Roosterpoort. Mijn elfenogen zien hem al. Hij ziet er niet gezellig uit.",
    Q + "vaart.afscheid.1": "Pas op jezelf. En als je onderweg knabbels vindt: die tellen voor mij, njeg!",
    Q + "vaart.gezwommen": "Zo kan het ook, baas. Dwars door de saus. Ik ruik nu drie dagen naar kaas. Vahoeg.",
}

# --- advancements: (name, parent in the tab knabbelring, icon, frame, title, text); every name is also a hidden quest/<name> ---------
ADVANCEMENTS = [
    ("ring_h4_spiegel", "ring_gekregen", "guhs:ringh4_spiegel", "task", "Wat nog gebakken moet worden", "Kijk in de Spiegel van Guhladriel"),
    ("ring_h4_gevaren", "ring_h4_spiegel", "minecraft:birch_boat", "task", "Tot hier, en geen kruimel verder", "Vaar de Guhduin af, tussen de twee Guhkoningen van de Arguhnath door"),
    ("ring_h4_klaar", "ring_h4_gevaren", "guhs:lichtflesje", "goal", "Vaarwel, Guhlórien", "Maak hoofdstuk 4 af: De Spiegel van Guhladriel"),
]
VERBORGEN = ["ring_h4_gerust", "ring_h4_aan_boord"]

# --- FTB (chapter guhs_knabbelring, section ring_h4): (key, title, text, icon, step advancement) ----------------------------------
FTB = [
    ("ring_h4_woud", "Het gouden woud", "Na de mijn wijst het &6Superkompas&r ('Mijn verhaal') naar &dGuhlórien&r, waar de satébomen goud kleuren. "
     "Loop naar de poort van de boomstad &6Caras Guhladhon&r. Zodra je er bent gaat Guhdalfs sluier voor jou open.", "guhs:sate_stam", 1),
    ("ring_h4_leguhlas", "Elfenogen", "Bij de poort staat &dLeguhlas&r. Hij zag je al dagen aankomen en heeft daar een mening over. Praat met hem.",
     "minecraft:spyglass", 2),
    ("ring_h4_guhladriel", "Vrouwe Guhladriel", "Klim de grote wenteltrap rond de &6Grote Spies&r op, helemaal tot de zaal onder de kroon. "
     "Daar zit &dGuhladriel&r. Ze weet alles al, maar praat toch even met haar. Vallen doet in haar stad geen pijn.", "minecraft:quartz_pillar", 3),
    ("ring_h4_rust", "Slapen als een vads roosje", "Op de &6gastenvlonder&r (de boom bij de poort) brandt een &6Rustvuurtje&r en liggen slaapzakken. "
     "Kruip erbij. Dit is meteen je rustpunt.", "guhs:ring_rustvuur", 4),
    ("ring_h4_spiegel", "De Spiegel van Guhladriel", "Midden in de nacht loopt Guhladriel naar het groene dal. Rechtsklik op de &bSpiegel&r en kijk "
     "wat was, wat is, en wat nog gebakken moet worden. Niet schrikken van het Oog: hij heeft alleen een beetje trek.", "guhs:ringh4_spiegel", 5),
    ("ring_h4_gaven", "Drie gaven", "Praat met Guhladriel bij de spiegel. Je krijgt het &bLichtflesje&r, het &aElfenmanteltje&r en het "
     "&7Elfentouw&r. Je hebt ze verderop hard nodig. Kwijt? Guhladriel geeft een nieuwe.", "guhs:lichtflesje", 6),
    ("ring_h4_vaart", "Tussen de Guhkoningen door", "Stap bij de steiger in het &6elfenbootje&r van Leguhlas (klik op het bootje of op hem). "
     "Het vaart vanzelf de &dGuhduin&r af, langs de twee reuzenbeelden van de &6Arguhnath&r. Blijf lekker zitten en kijk omhoog.", "minecraft:birch_boat", 7),
]
