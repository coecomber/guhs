"""
bbq2 (ring-h3) - every Dutch text of chapter 3, De Mijnen van Knabbelmoria (the source language; English is phase 3;
features/ring_h3.py writes them). The texts of the two camera scenes are in ring_h3_scene.py, next to their timing.

ONE line of this chapter is English in both languages on purpose (DESIGN_130 4, user decision): Guhdalf on the bridge,
"YOU.. SHALL.. NOT.. VADS!" (ring_h3_scene.ZIN_YOU). Phase 3 keeps it verbatim.
"""
NAAM = "De Mijnen van Knabbelmoria"
STRUCTUUR = (NAAM, "De oude mijn van de dwerg-guhs onder de Houtskoolvlakte. Alleen voor wie de poort open krijgt (Guhbarbecuether)")

# the signs in the template (four lines each; sign_text makes them translate components, prefix sign.guhs.ringh3)
BORDEN = {
    "plein_west": ["De Mijnen van", "Knabbelmoria", "Alleen voor", "vrienden. Njeg."],
    "plein_oost": ["Uitgang", "Knabbelmoria", "Niet achterom", "kijken. Njeg."],
    "stele_1": ["Een dwerg-guh eet", "in vaste orde:", "eerst de WORST,", "dan de KAAS,"],
    "stele_2": ["de SAUS erover", "en de KNABBEL", "toe. Wie smokkelt", "begint opnieuw."],
    "stele_3": ["Hal van de", "Hefbomen", "Trek ze in", "de goede orde"],
    "tombe_1": ["Hier rust", "DURGUH", "Heer van", "Knabbelmoria"],
    "tombe_2": ["Hij hield van", "één ding het", "allermeest:", "KAAS. Veel kaas."],
    "ingestort": ["Ingestort.", "Omlopen kan", "niet. Vraag", "een dwerg-guh."],
    "brug": ["Brug van", "Knabbel-dûm", "Max. 1 guh", "tegelijk. Njeg!"],
}

# the questline (verhaal_motor.verhaallijn): (step name, what to do now, where), and the short objective line per step
UITLEG = ("Hoofdstuk 3 van de Knabbelring. Onder de Houtskoolvlakte ligt de oude mijn van de dwerg-guhs: een poort met een raadsel, "
          "hefbomen, een put waar je beter niks in kunt laten vallen, en heel diep iets dat al eeuwen ligt te smeulen.")
STAPPEN = [
    ("De Poort van Knabbelmoria",
     "Loop met Sam-guh naar de Mijnen van Knabbelmoria, onder de Houtskoolvlakte. Het Superkompas (Mijn verhaal) wijst de weg.",
     "De Guhbarbecuether, een flink eind lopen van Guhvendel"),
    ("Zeg njeg en treed binnen",
     "De poort zit potdicht. Boven de deur gloeit een spreuk: 'Zeg njeg en treed binnen.' Typ het woord in de chat, of klik op de "
     "gloeiende runen en kies het goede antwoord.",
     "De westpoort van Knabbelmoria"),
    ("De Hal van de Hefbomen",
     "Vier hefbomen, één goede volgorde. Lees het rijmpje op de steen in het midden van de hal en trek ze in die volgorde over. "
     "Fout? Dan begin je gewoon opnieuw.",
     "De Hal van de Hefbomen, de trap af vanaf de poort"),
    ("De wachtkamer met de put",
     "Het valhek is open. Loop door naar de wachtkamer met de put. En laat Pippguh nergens aankomen.",
     "Door het valhek, naar het westen"),
    ("Gimguhs geheime doorgang",
     "De gang is ingestort en de trommels komen dichterbij. Gimguh weet een dwergendeur: klop drie keer op de rune van wat Durguh het "
     "allerlekkerst vond. Zijn tombe staat in dezelfde kamer.",
     "De westmuur van de wachtkamer"),
    ("Vlucht!",
     "Door de Zuilenhal, over het Brokkelpad en de Brug van Knabbel-dûm. Blijf lopen: het Brokkelpad valt onder je pootjes weg en de "
     "Barbecuerog komt achter je aan. Hij doet je niks, hij zet je alleen terug bij het rustvuurtje.",
     "De Zuilenhal, diep in de mijn"),
    ("Naar buiten",
     "Guhdalf is in de diepte gevallen. Klim de lange trap op naar de oostpoort en praat buiten met Araguh.",
     "De oostpoort van Knabbelmoria"),
]
KLAAR = ("Je bent door de Mijnen van Knabbelmoria gekomen. Zonder Guhdalf, maar met de ring. Sam-guh weet de weg naar het Satebos.",
         "Het Satebos: de Spiegel van Guhladriel")
KORT = {"0": "Loop naar de Mijnen van Knabbelmoria", "1": "Los het raadsel van de poort op: wat moet je zeggen?",
        "2": "Trek de vier hefbomen over in de volgorde van het rijmpje", "3": "Loop de wachtkamer met de put in",
        "4": "Klop drie keer op de goede rune (kijk bij de tombe van Durguh)", "5": "Ren! Over het Brokkelpad en de brug naar de overkant",
        "6": "Klim de lange trap op en praat buiten met Araguh", "klaar": "Op naar het Satebos, njeg"}

# the narrator card
KAART_TITEL = NAAM
KAART_REGELS = [
    "Van Guhvendel trok het gezelschap de zwarte Houtskoolvlakte over: negen reizigers, één knabbelring en veel te weinig proviand.",
    "Over de berg kon niet. Eromheen duurde te lang. Dus bleef er maar één weg over: eronderdoor.",
    "Onder de vlakte liggen de Mijnen van Knabbelmoria, waar de dwerg-guhs ooit kaasgoud dolven. Tot ze te diep groeven en iets wakker maakten.",
    "Niemand weet meer wat. 'Vast niks,' zei Pippguh. Njeg.",
]

RUNEN = {"kaas": "Rune van de kaas", "worst": "Rune van de worst", "saus": "Rune van de saus", "knabbel": "Rune van de knabbel",
         "bot": "Rune van het bot", "vlam": "Rune van de vlam", "njeg": "Njeg-rune", "trommel": "Rune van de trommel"}

TEKSTEN = {
    # names
    "entity.guhs.barbecuerog": "Barbecuerog",
    "block.guhs.ringh3_rune": "Runensteen van Knabbelmoria",
    "block.guhs.ringh3_rune.lore": "Uit de muur gehakt door Gimguh. Klop erop voor een ander teken. De njeg-rune geeft licht.",
    "block.guhs.ringh3_brokkelsteen": "Brokkelsteen",
    "block.guhs.ringh3_brokkelsteen.lore": "Houdt het precies lang genoeg uit. Meestal. Blijf lopen, njeg!",
    "block.guhs.ringh3_hendel": "Dwergen-hefboom",
    "block.guhs.ringh3_hendel.lore": "Springt vanzelf terug. Dwerg-guhs vertrouwen niemand met een hendel.",
    "gui.guhs.verhalen.ring_h3.beloning.rune": "Vier runenstenen, uitgehakt door Gimguh",
    # the gate
    "gui.guhs.ringh3.poort.naam": "De spreuk boven de poort",
    "gui.guhs.ringh3.poort.schrift": "In gloeiende runen staat er: 'De Poort van Durguh, Heer van Knabbelmoria. Zeg njeg en treed binnen.' Wat zeg je?",
    "gui.guhs.ringh3.poort.antwoord.vads": "Vads!",
    "gui.guhs.ringh3.poort.antwoord.njeg": "Njeg.",
    "gui.guhs.ringh3.poort.antwoord.sesam": "Sesam, open u!",
    "quest.guhs.ringh3.poort.lees": "Boven de poort gloeien runen: 'Zeg njeg en treed binnen.' Verder gebeurt er niks. Nog niet.",
    "quest.guhs.ringh3.poort.al_open": "De poort kent je nog. Hij schuift al open, njeg.",
    "quest.guhs.ringh3.poort.fout.1": "De poort doet alsof hij je niet hoort. Er staat toch echt wat je moet zeggen...",
    "quest.guhs.ringh3.poort.fout.2": "Nog steeds dicht. Lees de spreuk nog eens héél letterlijk: 'Zeg njeg en treed binnen.'",
    "quest.guhs.ringh3.poort.fout.3": "Sam-guh fluistert: 'Volgens mij moet je gewoon njeg zeggen, baas. Njeg.'",
    "quest.guhs.ringh3.poort.open": "Njeg! De runen lichten op en de poort van Knabbelmoria schuift knarsend open.",
    "quest.guhs.ringh3.guhdalf.poort.0": "De Poort van Durguh. Ik heb hem ooit open gekregen. Ik weet alleen niet meer hoe, njeg.",
    "quest.guhs.ringh3.guhdalf.poort.1": "Vadsus opendus! ...Niks. Knabbelum portalis! ...Ook niks. Hm.",
    "quest.guhs.ringh3.guhdalf.poort.2": "Ik ken elke spreuk in de talen van guhs, Mika's en hamsters. Deze poort luistert naar geen een.",
    "quest.guhs.ringh3.guhdalf.poort.3": "'Zeg njeg en treed binnen.' Wat kan dat betekenen? Het is vast een heel diep raadsel. Laat me nadenken.",
    "quest.guhs.ringh3.guhdalf.poort.4": "...Wacht eens. Het staat er gewoon. Jij hebt een toetsenbord, toch? Zég het dan, njeg!",
    "quest.guhs.ringh3.guhdalf.poort.open": "Ha! Ik wist het de hele tijd. Ik wilde alleen kijken of jij het ook wist.",
    # the levers
    "quest.guhs.ringh3.hefboom.goed": "KLIK. Hefboom %s van %s",
    "quest.guhs.ringh3.hefboom.fout": "KLONK. Fout! Alles springt terug, njeg.",
    "quest.guhs.ringh3.hefboom.open": "KLIK-KLAK-KLONK. Het valhek gaat ratelend omhoog. Vahoeg!",
    "quest.guhs.ringh3.hefboom.niet_van_jou": "De hefboom klemt. Dit raadsel is nog niet van jou, njeg.",
    "quest.guhs.ringh3.hefboom.al_gedaan": "Deze had je al. Het valhek gaat voor je open.",
    "quest.guhs.ringh3.hefboom.sam": "Op die steen in het midden staat een rijmpje, baas: eerst de worst, dan de kaas, de saus erover en de knabbel toe.",
    "quest.guhs.ringh3.guhdalf.hal.0": "Ik heb geen herinnering aan deze plek. Wel aan dat rijmpje. Het ging over eten, zoals alles hier.",
    "quest.guhs.ringh3.guhdalf.hal.1": "Lees de steen in het midden. En trek niet zomaar ergens aan. Dat doet Pippguh al, njeg.",
    # the well
    "quest.guhs.ringh3.pippguh.put.0": "Ik raak niks meer aan. Echt niet. ...Is dat een hendeltje?",
    "quest.guhs.ringh3.pippguh.put.1": "Het was maar een klein emmertje. Het maakte alleen een heel groot geluid.",
    "quest.guhs.ringh3.pippguh.put.2": "Guhdalf zei 'dwaas'. Maar hij zei het met een hoofdletter. Dat hoorde ik, njeg.",
    "quest.guhs.ringh3.guhdalf.put": "Trommels. Ze weten dat we er zijn. Help Gimguh met zijn deur, en snel een beetje!",
    # Gimguh's door
    "quest.guhs.ringh3.gimguh.wacht": "Een dwerg-guh hoort thuis onder de grond. Lekker donker. Lekker dicht bij de kaas.",
    "quest.guhs.ringh3.gimguh.deur.0": "Hier zit een dwergendeur, daar durf ik mijn baard om te verwedden. Je klopt drie keer op de rune van wat "
                                       "Durguh het lekkerst vond. Alleen... wat was dat ook alweer?",
    "quest.guhs.ringh3.gimguh.deur.1": "Bot? Worst? Vlam? Ik weet het niet meer! Kijk bij de tombe van Durguh, daar aan de overkant. Het staat erop.",
    "quest.guhs.ringh3.gimguh.deur.2": "Drie keer kloppen op dezelfde rune, zonder er eentje tussendoor. Een dwergendeur is precies, njeg.",
    "quest.guhs.ringh3.gimguh.open": "Zie je wel! Een dwerg-guh vergeet nooit een deur. Hooguit even.",
    "quest.guhs.ringh3.rune.klop": "TOK. Klop %s van %s",
    "quest.guhs.ringh3.rune.fout": "Tok. %s? Daar gebeurt niks mee. Opnieuw, njeg.",
    "quest.guhs.ringh3.rune.open": "TOK. TOK. TOK. Een stuk muur schuift opzij: Gimguhs geheime doorgang. Vahoeg!",
    "quest.guhs.ringh3.rune.sam": "Op die tombe staat waar Durguh het meest van hield, baas. Het is geel en er zitten gaten in.",
    # the hall, the Barbecuerog, the bridge
    "quest.guhs.ringh3.rog.trommels": "Doem... doem... Trommels in de diepte. Er komt iets.",
    "quest.guhs.ringh3.rog.vlucht": "DE BARBECUEROG! REN! Over het Brokkelpad, naar de brug!",
    "quest.guhs.ring.terug.barbecuerog": "ZWIEP! De braadworstzweep van de Barbecuerog kreeg je te pakken. Hij zette je netjes terug bij je "
                                         "rustvuurtje. Je ruikt nu wel naar barbecue, njeg.",
    "quest.guhs.ringh3.brug.weg": "De brug is weg. Guhdalf ook. Hier kun je even niet langs, njeg.",
    "quest.guhs.ringh3.brug.heel": "De dwerg-guhs van vroeger bouwden voor de eeuwigheid: de Brug van Knabbel-dûm ligt er alweer.",
    "quest.guhs.ringh3.na_brug": "Guhdalf is gevallen. Sam-guh snuft. Kom: de lange trap op, naar buiten.",
    # outside
    "quest.guhs.ringh3.araguh.einde": "We zijn erdoor. Niet allemaal... Maar de ring is veilig, en Guhdalf zou willen dat we doorliepen. "
                                      "Gimguh hakte nog wat runenstenen voor je uit de muur. Als aandenken.",
    "quest.guhs.ringh3.araguh.na": "Het Satebos is niet ver meer. Daar woont Guhladriel. Zij weet raad, en ze heeft knabbels.",
    "quest.guhs.ringh3.klaar": "Hoofdstuk 3 is klaar: je bent door de Mijnen van Knabbelmoria. Op naar het Satebos!",
    # sounds
    "subtitles.guhs.ringh3.barbecuerog.brul": "Barbecuerog brult",
    "subtitles.guhs.ringh3.barbecuerog.grom": "Barbecuerog gromt",
    "subtitles.guhs.ringh3.barbecuerog.stap": "Barbecuerog stampt",
    "subtitles.guhs.ringh3.barbecuerog.zweep": "Braadworstzweep knalt",
    "subtitles.guhs.ringh3.barbecuerog.ontbrand": "Vlammen laaien op",
    "subtitles.guhs.ringh3.trommel": "Trommels in de diepte",
    "subtitles.guhs.ringh3.emmer": "Emmer klettert",
    "subtitles.guhs.ringh3.staf": "Staf slaat op steen",
    "subtitles.guhs.ringh3.breuk": "Brug breekt",
    "subtitles.guhs.ringh3.val": "Iets valt de diepte in",
    "subtitles.guhs.ringh3.poort": "Stenen deur schuift",
    "subtitles.guhs.ringh3.klonk": "Hefboom springt terug",
    "subtitles.guhs.ringh3.brokkel": "Steen brokkelt",
}
for _naam, _tekst in RUNEN.items():
    TEKSTEN[f"gui.guhs.ringh3.rune.{_naam}"] = _tekst

# the visible advancements of the tab knabbelring: (name, parent, icon, frame, title, text, hidden)
ADVANCEMENTS = [
    ("ring_h3_poort", "ring_gekregen", "guhs:ringh3_rune", "task", "Zeg njeg en treed binnen", "Los het raadsel van de Poort van Knabbelmoria op", False),
    ("ring_h3_emmer", "ring_h3_poort", "minecraft:bucket", "task", "Dwaas van een Pippguh", "Wees erbij als er een emmertje in de put valt", False),
    ("ring_h3_gevallen", "ring_h3_emmer", "guhs:gloeikool", "task", "Even de diepte in", "Val in de Kloof van Knabbelmoria. Sam-guh vist je er wel weer uit", True),
    ("ring_h3_gepakt", "ring_h3_emmer", "minecraft:lead", "task", "Aan de worst geregen", "Laat je pakken door de braadworstzweep van de Barbecuerog", True),
    ("ring_h3_brug", "ring_h3_emmer", "minecraft:blaze_rod", "challenge", "YOU.. SHALL.. NOT.. VADS!", "Sta met Guhdalf op de Brug van Knabbel-dûm", False),
    ("ring_h3_klaar", "ring_h3_brug", "guhs:houtskoolsteen_stenen", "goal", "Eronderdoor", "Kom door de Mijnen van Knabbelmoria", False),
]
# hidden ones (FTB tasks) that have no visible twin
VERBORGEN = ["ring_h3_njeg", "ring_h3_hefbomen", "ring_h3_gang"]

SOUNDS = {
    "ringh3.barbecuerog.brul": [{"name": "minecraft:entity.ravager.roar", "type": "event", "pitch": 0.5},
                                {"name": "minecraft:entity.ender_dragon.growl", "type": "event", "pitch": 0.6},
                                {"name": "minecraft:entity.warden.roar", "type": "event", "pitch": 0.7}],
    "ringh3.barbecuerog.grom": [{"name": "minecraft:entity.ravager.ambient", "type": "event", "pitch": 0.5},
                                {"name": "minecraft:entity.warden.agitated", "type": "event", "pitch": 0.6},
                                {"name": "minecraft:entity.blaze.ambient", "type": "event", "pitch": 0.5}],
    "ringh3.barbecuerog.stap": [{"name": "minecraft:entity.ravager.step", "type": "event", "pitch": 0.5},
                                {"name": "minecraft:entity.warden.step", "type": "event", "pitch": 0.6},
                                {"name": "minecraft:entity.iron_golem.step", "type": "event", "pitch": 0.5}],
    "ringh3.barbecuerog.zweep": [{"name": "minecraft:entity.blaze.shoot", "type": "event", "pitch": 0.6},
                                 {"name": "minecraft:entity.player.attack.sweep", "type": "event", "pitch": 0.5}],
    "ringh3.barbecuerog.ontbrand": [{"name": "minecraft:item.firecharge.use", "type": "event", "pitch": 0.6},
                                    {"name": "minecraft:entity.ghast.shoot", "type": "event", "pitch": 0.6}],
    "ringh3.trommel": [{"name": "minecraft:entity.warden.heartbeat", "type": "event", "pitch": 0.55}],
    "ringh3.emmer": [{"name": "minecraft:block.anvil.land", "type": "event", "pitch": 1.5, "volume": 0.7},
                     {"name": "minecraft:block.chain.break", "type": "event", "pitch": 0.6}],
    "ringh3.staf": [{"name": "minecraft:block.anvil.land", "type": "event", "pitch": 0.6},
                    {"name": "minecraft:block.respawn_anchor.deplete", "type": "event", "pitch": 0.8}],
    "ringh3.breuk": [{"name": "minecraft:entity.generic.explode", "type": "event", "pitch": 0.6},
                     {"name": "minecraft:entity.wither.break_block", "type": "event", "pitch": 0.6}],
    "ringh3.val": [{"name": "minecraft:entity.ender_dragon.flap", "type": "event", "pitch": 0.5},
                   {"name": "minecraft:entity.phantom.swoop", "type": "event", "pitch": 0.5}],
    "ringh3.poort": [{"name": "minecraft:block.piston.extend", "type": "event", "pitch": 0.5},
                     {"name": "minecraft:block.grindstone.use", "type": "event", "pitch": 0.5}],
    "ringh3.klonk": [{"name": "minecraft:block.anvil.land", "type": "event", "pitch": 0.9, "volume": 0.5},
                     {"name": "minecraft:block.iron_trapdoor.close", "type": "event", "pitch": 0.5}],
    "ringh3.brokkel": [{"name": "minecraft:block.deepslate.break", "type": "event", "pitch": 0.7},
                       {"name": "minecraft:block.basalt.break", "type": "event", "pitch": 0.6}],
}
