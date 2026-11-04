"""
1.10 "Het Vadsrijk" zone 2 "De Splinterpieken", npc slice: what the wiki says about Kandijkom, the story, the quests and the secrets
of zone 2 (the in-game version is the "Gids" tab of the questlog, J: pages z2_*). Not a feature module (not in FEATURES): the docs step
(tools/wiki_bbq2) reads WIKI, like vadsrijk_npc_wiki.py; integration adds this module to that step next to zone 1's.
Spoiler-free: it tells how zone 2 starts, never what lies in the ravine or how the zone ends.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "vadsrijk-splinterpieken": (
            "De Splinterpieken (zone 2)", "item:minecraft:amethyst_shard",
            "Een ijzig kristalgebergte vol echo's en sneeuw, voor avonturiers van level 20 tot 35. Na het verhaal van zone 1 vliegt "
            "Stuurguh Zeppie je vanuit De Haven naar Kandijkom. Hier versteent de Vadsloosheid wezens tot grauw kristal.",
            ["het-vadsrijk", "vadsrijk-kandijkom", "vadsrijk-z2-quests", "vadsrijk-z2-geheimen"]),
        "vadsrijk-kandijkom": (
            "Kandijkom", "item:minecraft:pink_stained_glass",
            "Een warm stadje in een open reuzengeode van roze kwarts: chocomelkhuisjes met dikke sneeuwmutsen, guhs in gebreide truien, "
            "een schaatsvijver op het plein en een gondelbaan naar de toppen. De Kristalwacht bewaakt de poorten.",
            ["vadsrijk-splinterpieken", "vadsrijk-z2-quests"]),
        "vadsrijk-z2-quests": (
            "Quests in de Splinterpieken", "item:minecraft:writable_book",
            "Het verhaal van zone 2 (de gouden !) leidt je van Kandijkom over de Galmhellingen en het Bevroren Knuffelmeer naar de "
            "Splinterkloof en de Spiegelburcht. Elke stap zegt bij wie en waar je moet zijn; rechtsboven en in je questlog (J) zie je "
            "altijd je volgende stap.",
            ["vadsrijk-splinterpieken", "vadsrijk-kandijkom", "vadsrijk-z2-geheimen"]),
        "vadsrijk-z2-geheimen": (
            "Geheimen van de Splinterpieken", "item:minecraft:spyglass",
            "Tien knuffels zitten vast onder het ijs, vijf vreemde boorsporen wijzen naar iets wat diep in de berg bromt, en er liggen "
            "schatten op plekken waar je eigenlijk niet kunt komen. Ze glinsteren als je dichtbij bent.",
            ["vadsrijk-splinterpieken", "vadsrijk-z2-quests"]),
    },
    "tekst": [
        ("systemen/vadsrijk-splinterpieken", "Erheen",
         "Maak eerst het verhaal van zone 1 af en word level 20. Dan heeft Stuurguh Zeppie in De Haven een nieuwe luchtschiplijn: naar "
         "Kandijkom. De eerste vlucht vergeet je niet snel. Doe je sjaal om."),
        ("systemen/vadsrijk-splinterpieken", "De gebieden",
         "De Galmhellingen in het oosten (lv 20-25): sneeuwvelden met kristalnaalden die terugzingen. Het Bevroren Knuffelmeer in het "
         "westen (lv 25-30): knuffels onder het ijs, en het ijs scheurt. De Splinterkloof in het noorden (lv 30-34): hangbruggen en "
         "Mika-boorinstallaties. En op de hoogste piek: de Spiegelburcht."),
        ("systemen/vadsrijk-splinterpieken", "Echte monsters",
         "In de bergen vecht je tegen echte monsters van grauw kristal: verslagen spatten ze uit elkaar in scherven die wegsmelten. "
         "Mika's vluchten in een rookwolk naar huis. Guhs doe je nooit pijn: een versteende guh bevrijd je met een klik."),
        ("systemen/vadsrijk-kandijkom", "Wie woont er?",
         "Cafébaas Mallow (chocomelk!), Burgemeester Kandij, Kapitein Kwarts van de Kristalwacht, Juweliersguh Glinster (edelstenen in "
         "je sokjes), Smid Gloeikool, Breister Wolwinter, Schaatsjuf Zwierig, Gondelguh Kabeltje, Ordemeester Kristalijn in het "
         "ordetempeltje (klasse wisselen en op level 30 je derde titel) en nog veel meer guhs met een winters probleempje."),
        ("systemen/vadsrijk-kandijkom", "Reizen",
         "Kapitein Poolster vliegt je van Kandijkom terug naar De Haven. Met de gondel van Gondelguh Kabeltje ben je zo boven op de "
         "Galmhellingen."),
        ("systemen/vadsrijk-z2-quests", "Zijquests",
         "Ruim twintig zijquests: wanten van Opa Sneeuwbal terughalen, kandijkoekjes, een echokoor met de zingende naalden, post per "
         "slee, de grootste sneeuwpop, wiebelbruggen, premies van de Kristalwacht en je eerste edelsteen bij de juwelier."),
        ("systemen/vadsrijk-z2-geheimen", "Glinsters",
         "Hartjes onder het ijs = een knuffel van Kleine Vlokje, paarse sterretjes = een boorspoor, goudgeel = een schat. Het "
         "FTB-hoofdstuk \"Het Vadsrijk\" heeft een eigen stukje voor de Splinterpieken; niks staat op slot."),
    ],
}
