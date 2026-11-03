"""
1.10.0 "Het Vadsrijk", npc slice: what the wiki says about the story, the NPCs, quests and secrets (the in-game version is the
"Gids" tab of the questlog, J). Not a feature module (not in FEATURES): the docs step (tools/wiki_bbq2) reads WIKI, like
guhs190_wiki.py; integration adds this module to that step (and the other slices add their own pages for classes, loot...).
Spoiler-free: it tells how the story starts, never who the spy is or how zone 1 ends.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "het-vadsrijk": (
            "Het Vadsrijk", "item:minecraft:pink_wool",
            "Een eigen avonturenwereld vol klodders en zwammen. Word rekruut van De Knuffelorde, kies een klasse en vrolijk op wat "
            "grauw en knorrig is geworden. Je eigen spullen blijven veilig thuis.",
            ["vadsrijk-knuffelkamp", "vadsrijk-zwamzoet", "vadsrijk-quests", "vadsrijk-geheimen"]),
        "vadsrijk-knuffelkamp": (
            "Knuffelkamp", "item:minecraft:campfire",
            "Je komt bij zonsopgang per luchtschip aan op Knuffelkamp, het oefeneiland van De Knuffelorde. Praat met Sergeant "
            "Pluizebol, kies je pad bij het klasse-tempeltje, oefen op de trainingspoppen en ga even zitten bij het kampvuur...",
            ["het-vadsrijk", "vadsrijk-zwamzoet", "vadsrijk-quests"]),
        "vadsrijk-zwamzoet": (
            "Zwamzoet", "item:minecraft:red_mushroom",
            "De paddenstoelenstad van De Knuffelorde, met huisjes op reuzenhoeden, een markt, een smid, een stal en bovenop de "
            "hoogste zwam de ordetempel. Hier wonen guhs die je hulp goed kunnen gebruiken.",
            ["het-vadsrijk", "vadsrijk-quests", "vadsrijk-geheimen"]),
        "vadsrijk-quests": (
            "Quests en de questlog", "item:minecraft:writable_book",
            "Een gouden ! boven een guh is een verhaalquest, een gele ! een zijquest, een ? betekent: hier inleveren of even praten. "
            "Druk op J voor je questlog met je quests, je geheimen, de wezens die je opvrolijkte en de Avonturiersgids.",
            ["het-vadsrijk", "vadsrijk-geheimen"]),
        "vadsrijk-geheimen": (
            "Geheimen van het Vadsrijk", "item:minecraft:spyglass",
            "Overal liggen geheimen: tien verloren knuffels van Kleine Kiki, verborgen schatten en vijf vreemde aanwijzingen. Ze "
            "glinsteren een beetje als je dichtbij komt. Waar leiden die aanwijzingen naartoe?",
            ["het-vadsrijk", "vadsrijk-quests"]),
    },
    "tekst": [
        ("systemen/het-vadsrijk", "De Vadsloosheid",
         "Vroeger beschermde de grote roze draak Vahoegnir het rijk: waar hij vloog, bloeiden de bloemen. Nu is hij grauw, en waar "
         "hij langskomt worden wezens chagrijnig en verwelken de bloemen. Dat heet de Vadsloosheid. De Knuffelorde vrolijkt alles "
         "weer op: verslagen wezens worden niet verwond, ze ploffen roze en huppen blij weg."),
        ("systemen/het-vadsrijk", "Het verhaal van zone 1",
         "Het hoofdverhaal (de gouden !) leidt je van Knuffelkamp naar Zwamzoet, langs de grauwe littekens van de draak, door de "
         "Klodderweide, het Sporenwoud en de Druipgrotten naar de Zwamtempel op de berg. Tussenfilmpjes lopen vanzelf; de "
         "gesprekken klik je door."),
        ("systemen/vadsrijk-knuffelkamp", "Rustig, en dan...",
         "Neem rustig de tijd in het kamp: praat met iedereen, vis met Visser Dobber of klim naar de vuurtoren voor het uitzicht op "
         "Zwamzoet. Als het kampvuur brandt en de zon opkomt boven het water, gebeurt er iets wat niemand verwacht."),
        ("systemen/vadsrijk-knuffelkamp", "Het luchtschip",
         "Na je opleiding vliegt Kapitein Wolkewiet je naar De Haven van Zwamzoet. Stuurguh Zeppie vliegt je weer terug. Er is "
         "geen snelreizen: de kampvuren zijn je checkpoints."),
        ("systemen/vadsrijk-zwamzoet", "Wie woont er?",
         "Grootmeester Vadsvoet in de ordetempel (klasse wisselen en de klassenproeven op level 10 en 20), Smid Hamerbol, "
         "Marktmeester Hamertje-Tik, Stalmeester Hoefje (je eerste rijdier rond level 8), Klerk Stempeltje bij het questbord, en "
         "een heleboel guhs met een probleempje: Oma Truus, Bakker Bram, Kleine Kiki, Dichter Daan, Professor Pluis en nog veel meer."),
        ("systemen/vadsrijk-quests", "Soorten quests",
         "Praten, wezens opvrolijken, spulletjes verzamelen (ze vallen vanzelf als je ze nodig hebt), een plek vinden, een kampvuur "
         "aansteken, een baas opvrolijken, een guh begeleiden die achter je aan loopt, en geheimen zoeken. Beloningen: xp, "
         "Vadsmunten en buit. In een groep tellen opvrolijk-quests voor iedereen in de buurt mee."),
        ("systemen/vadsrijk-quests", "Het FTB-hoofdstuk",
         "In het FTB-boek staat het hoofdstuk \"Het Vadsrijk\". Elke quest vinkt zichzelf af als je hem in het Vadsrijk inlevert. "
         "Niks staat op slot."),
        ("systemen/vadsrijk-geheimen", "Glinsters",
         "Hartjes = een verloren knuffel, goudgeel = een schat, paarse sterretjes = een aanwijzing. Je questlog (J) houdt bij wat je "
         "al gevonden hebt."),
    ],
}
