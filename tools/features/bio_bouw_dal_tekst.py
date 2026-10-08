"""
biomes3 slice "bouw-dal": every Dutch text (not in FEATURES). English comes later in tools/lang/en/c86_bio_bouw_dal.json.

The scene of het weebhuisje: five variants; who says lines 1-4 is WeebHuis.SPREKERS (E = Evivads, N = Nielsvads), line 5 is
always the two together (quest.guhs.weeb.samen). Variant 1 is the text the user approved, word for word. The tone: short
lines, Dutch with Japanese words sprinkled in; they notice that something Japanese is going on "...in Japan" and leave.
Not wanted: parodies of specific anime, or the joke that they cannot really speak Japanese.
"""

SCENES = [   # (speakers, the four lines)
    ("ENEN", ["Ohayo, guh-chan! Kom binnen, schoenen uit. Kawaii huisje hè?",
              "Sugoi, bezoek! Wil je mijn figuurtjes zien? Niet aankomen. Dat is een limited edition.",
              "Wacht... Niels. Het is kersenbloesemtijd.",
              "...In Japan."]),
    ("ENNE", ["Konnichiwa, guh-chan! Thee? Het is echte matcha. Njeg.",
              "Ga zitten, senpai. Niet op de dakimakura. Die is van mij.",
              "Wacht... Evi. Vandaag gaat de nieuwe treinlijn open. Met een nieuwe Shinkansen.",
              "...In Japan."]),
    ("ENEN", ["Ohayo! Trek een nummertje, guh-chan. Grapje. Het loket is vandaag dicht.",
              "Kijk, plank drie. Allemaal guh-figuurtjes. Kawaii, hè? Niet aankomen.",
              "Wacht... Niels. Vandaag komt dat ene figuurtje uit. De limited edition.",
              "...In Japan."]),
    ("NENE", ["Konnichiwa, guh-chan. Ruik je dat? Evi maakt ramen. Sugoi.",
              "Itadakimasu! ...Hm. Lekker. Maar het is nét niet de echte.",
              "Wacht... Evi. Het is ramenseizoen.",
              "...In Japan."]),
    ("ENEN", ["Ohayo, guh-chan! Mooi hè, onze lampionnen? Kawaii, njeg.",
              "Ik heb ze zelf opgehangen, senpai. Op volgorde van de dienstregeling.",
              "Wacht... Niels. Dit weekend is het lampionnenfestival.",
              "...In Japan."]),
]
SAMEN = "WE GAAN NAAR JAPAN! Ittekimasu!"
VERTREK = [   # (Nielsvads when they set off, Evivads when they step in)
    ("Met de ballon. Alweer. Dat is geen echt openbaar vervoer.", "Paspoorten heb ik! Instappen, Niels. Vahoeg!"),
    ("Een ballon heeft niet eens een dienstregeling. Njeg.", "Maar wel uitzicht! Ittekimasu, guh-chan!"),
    ("Geen perron, geen spoor, geen conducteur. Geen echt openbaar vervoer.", "Hij vliegt wel naar Japan. Kom, senpai!"),
]
ZWAAI = {"e": "Jij ook, guh-chan? Zwaai ons maar uit! We nemen iets voor je mee. Njeg!",
         "n": "Ittekimasu, senpai! Wij gaan naar Japan. Niet aan de figuurtjes komen."}
TADAIMA = "Tadaima!"
TERUG = [   # (speakers, the two lines when a present is handed over)
    ("NE", ["Tadaima! De Shinkansen was op tijd. Op de seconde. Ik moest er bijna van huilen, njeg.",
            "Hier, guh-chan. Een cadeautje uit Japan. Arigato voor het uitzwaaien!"]),
    ("EN", ["Tadaima! Ik heb zeven stempels gehaald. Zeven! Elk station heeft zijn eigen stempel.",
            "En ik zat bij het raam, senpai. Hier, dit is voor jou. Arigato!"]),
    ("EN", ["Tadaima! Bij de paspoortcontrole stond een lange rij. Ik mocht zo doorlopen. Vakvrouw, vads.",
            "Ze kent alle stempels uit haar hoofd. Hier, iets kleins uit Japan. Kawaii, hè?"]),
    ("NE", ["Tadaima! Elf treinen gehad. Nul minuten vertraging. Sugoi.",
            "Hij heeft elke trein gefilmd, guh-chan. Elke. Hier, voor jou. Arigato!"]),
    ("EN", ["Tadaima! Mijn paspoort is vol gestempeld. Ik maak morgen wel een nieuwe voor mezelf. Njeg.",
            "De ballon terug had weer geen dienstregeling. Maar goed. Hier, een cadeautje, senpai."]),
]
THEEGUH = ["Konnichiwa. Thee? Ga lekker zitten. De waterval doet de rest. Njeg.",
           "Sst. Hoor je dat? Klater, klater. Daar word je vads van.",
           "Ik zit hier al de hele dag. Morgen weer. Vahoeg, wat een leven."]

TEKSTEN = {
    "entity.guhs.guh_npc.weeb_evivads": "Evivads",
    "entity.guhs.guh_npc.weeb_nielsvads": "Nielsvads",
    "entity.guhs.guh_npc.dal_theeguh": "Thee-guh",
    "entity.guhs.weeb_ballon": "Ballon van Evivads en Nielsvads",
    "quest.guhs.weeb.samen": SAMEN,
    "quest.guhs.weeb.tadaima": TADAIMA,
    "quest.guhs.weeb.zwaai.e": ZWAAI["e"],
    "quest.guhs.weeb.zwaai.n": ZWAAI["n"],
    "quest.guhs.weeb.cadeau": "Je krijgt een cadeautje uit Japan: %s",
    "quest.guhs.weeb.ruil.ja": "Een dubbele? Sugoi. Ruilen is ook verzamelen, senpai.",
    "quest.guhs.weeb.ruil.hier": "Hier, guh-chan. Deze had je nog niet. Kawaii!",
    "quest.guhs.weeb.ruil.enige": "Die heb je maar één keer. Die hou je lekker zelf. Breng maar een dubbele, njeg.",
    "quest.guhs.weeb.ruil.compleet": "Jouw reeks is al compleet, guh-chan. Sugoi! Die dubbele mag je houden.",
    "quest.guhs.weeb.compleet": "Je hele reeks is compleet. Een echte weeb. Vahoeg, wat zijn wij trots!",
    "gui.guhs.weeb.ballon": "De ballon van Evivads en Nielsvads. Geen echt openbaar vervoer, zegt Nielsvads.",
    "gui.guhs.weeb.beschermd": "Niet aan het weebhuisje komen. Njeg!",
    "gui.guhs.weeb.klik.figuurtjes": "Niet aankomen. Dat is een limited edition.",
    "gui.guhs.weeb.klik.poster": "Die hangt precies recht. Niet aankomen, senpai.",
    "gui.guhs.weeb.klik.mangastapel": "Deel 1 tot en met 47. Op volgorde. Njeg.",
    "gui.guhs.weeb.klik.dakimakura": "Een guh-dakimakura. Die is van Nielsvads. Niet op gaan zitten.",
    "gui.guhs.weeb.klik.loket": "Het mini-loket van Evivads. Stempels, een inktkussen en een stapeltje paspoorten.",
    "gui.guhs.weeb.klik.nummerautomaat": "Uw nummer: 047. Er zijn 46 wachtenden voor u. Njeg.",
    "gui.guhs.weeb.klik.loketbord.open": "Loket open. Trek een nummertje. (Paspoorten alleen op het guhmeentehuis.)",
    "gui.guhs.weeb.klik.loketbord.dicht": "Loket gesloten wegens Japan.",
    "gui.guhs.weeb.klik.briefje": "Zijn even naar Japan. Morgen terug. Niet aan de figuurtjes komen. — E & N",
    "gui.guhs.japan.lore": "Uit Japan. Een cadeautje van Evivads en Nielsvads.",
    "gui.guhs.japan.eten": "Uit Japan. Lekker voor jou en voor je guh. Itadakimasu!",
    "gui.guhs.kledingbron.weeb_japan": "Cadeautje uit Japan (het weebhuisje)",
    "item.guhs.japan_kimono": "Guh-kimono",
    "item.guhs.japan_hachimaki": "Hachimaki",
    "item.guhs.japan_kattenoortjes": "Kattenoortjes",
    "item.guhs.japan_strikje": "Japans strikje",
    "structure.guhs.weebhuisje": "Het weebhuisje",
    "structure.guhs.weebhuisje.tooltip": "Een guh-Japans huisje in het Klaterdal. Evivads en Nielsvads wonen er. Als ze thuis zijn.",
}
for _i, (_wie, _regels) in enumerate(SCENES):
    for _j, _regel in enumerate(_regels):
        TEKSTEN[f"quest.guhs.weeb.scene.{_i + 1}.{_j + 1}"] = _regel
for _i, (_n, _e) in enumerate(VERTREK):
    TEKSTEN[f"quest.guhs.weeb.vertrek.{_i + 1}.n"] = _n
    TEKSTEN[f"quest.guhs.weeb.vertrek.{_i + 1}.e"] = _e
for _i, (_wie, _regels) in enumerate(TERUG):
    for _j, _regel in enumerate(_regels):
        TEKSTEN[f"quest.guhs.weeb.terug.{_i + 1}.{_j + 1}"] = _regel
for _i, _regel in enumerate(THEEGUH):
    TEKSTEN[f"quest.guhs.dal.theeguh.{_i + 1}"] = _regel
