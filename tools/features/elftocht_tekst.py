"""De Elf-Guhjestocht (2.9, 2.10) - all Dutch texts (nl_nl and en_us get the same Dutch)."""
from features import elftocht_route as R

DORPEN = R.NAMEN

TEKSTEN = {
    # --- the characters -------------------------------------------------------------------------------------------------
    "entity.guhs.guh_npc.schaatsmeesterguh": "Schaatsmeester Guhglij",
    "entity.guhs.guh_npc.stempelguh": "Stempelguh",
    "gui.guhs.guhdex.rarity.schaatsmeesterguh": "Uniek (in Guhwarden, bij de start van de Elf-Guhjestocht)",
    "gui.guhs.guhdex.info.schaatsmeesterguh": "Schaatsmeester Guhglij heeft een oranje pompommuts, een fluitje aan een koord en twee "
                                               "schaatsjes om zijn nek. Hij leent je guh-schaatsen, geeft je een stempelkaart en fluit "
                                               "het startschot: elf dorpjes, elf stempels, it giet oan! Voor elfstempels verkoopt hij "
                                               "warme wintertruitjes. Hij zegt altijd: 'Hoe vadsiger, hoe mooier je glijdt! Je moet "
                                               "alleen vahoeg genoeg zijn, en daar helpt warme chocovet bij.'",
    "gui.guhs.guhdex.rarity.stempelguh": "Elf (een in elk dorpje van de Elf-Guhjestocht, elk met een eigen hoedje)",
    "gui.guhs.guhdex.info.stempelguh": "In elk dorpje langs de Elf-Guhjestocht staat een Stempelguh met een stempel in zijn pootje. "
                                        "Kom je in de goede volgorde langs? PLOF! Weer een stempel op je kaart. Elke Stempelguh "
                                        "draagt het hoedje en sjaaltje van zijn eigen dorp.",
    # --- the structure ---------------------------------------------------------------------------------------------------
    "structure.guhs.elfguhjestocht": "De Elf-Guhjestocht",
    "structure.guhs.elfguhjestocht.tooltip": "Een bevroren kanaal dat door de Guhpolder kronkelt, langs elf dorpjes, slootjes met knotwilgen "
                                             "en sneeuwheuveltjes: schaatsen, stempels en warme chocovet!",
    # --- items and blocks ------------------------------------------------------------------------------------------------
    "item.guhs.elfstempel": "Elfstempel",
    "item.guhs.guh_schaatsen": "Guh-schaatsen",
    "item.guhs.guh_schaatsen.lore": "Geleend van Schaatsmeester Guhglij. Houd ze vast en glij over het ijs!",
    "item.guhs.guh_schaatsen.how": "Op ijs schaats je supervahoeg; ernaast loop je gewoon, njeg.",
    "item.guhs.stempelkaart": "Stempelkaart",
    "item.guhs.stempelkaart.lore": "%s van de 11 stempels. Rechtsklik: de hele kaart.",
    "item.guhs.warme_chocovet": "Warme chocovet",
    "item.guhs.warme_chocovet.lore": "Dikke warme chocomelk met slagroom. Warm vanbinnen = even extra vahoeg!",
    "item.guhs.snert_kommetje": "Kommetje snert",
    "item.guhs.snert_kommetje.lore": "Erwtensoep zo dik dat de lepel blijft staan. Dan schaats je vanzelf harder!",
    "block.guhs.elf_guhjeskruisje": "Elf-Guhjeskruisje",
    "block.guhs.elftocht_lampion": "Nachtlampion",
    "block.guhs.elftocht_vuurkorf": "Vuurkorf",
    "block.guhs.elftocht_kopjes": "Dienblad met warme kopjes",
    "item.guhs.elftocht_schaatsmuts": "Schaatsmuts met pompom",
    "item.guhs.elftocht_truitje": "Wollen schaatstruitje",
    "item.guhs.elftocht_sjaal": "Oranje schaatssjaal",
    "item.guhs.elftocht_oorwarmers": "Pluizige oorwarmers",
    # --- the stall ------------------------------------------------------------------------------------------------------
    "gui.guhs.elftocht.kopjes.pak_chocovet": "Een warme chocovet voor jou! Drink hem op voor een vahoege boost.",
    "gui.guhs.elftocht.kopjes.pak_snert": "Een kommetje snert voor jou! Lepel het op voor een vahoege boost.",
    "gui.guhs.elftocht.kopjes.wacht_chocovet": "De chocovet is nog te heet, njeg! Even wachten...",
    "gui.guhs.elftocht.kopjes.wacht_snert": "De snert pruttelt nog, njeg! Even wachten...",
    "gui.guhs.elftocht.warm": "Warm vanbinnen! Je glijdt even extra vahoeg!",
    # --- Schaatsmeester Guhglij ------------------------------------------------------------------------------------------
    "quest.guhs.elftocht.hallo": "Hoi schaatser! Zin in de Elf-Guhjestocht? Elf dorpjes, elf stempels, en het ijs is spiegelglad. VAHOEG!",
    "quest.guhs.elftocht.hallo_bezig": "Je bent al aan het schaatsen, vahoeg! Wil je stoppen? Dat kan hier.",
    "quest.guhs.elftocht.start": "Schaatsen aan, kaart in je zak... Eerst naar %s! Op mijn fluitje: 3... 2... 1...",
    "quest.guhs.elftocht.vrij": "Vrij schaatsen! Geen klok, geen stempels, gewoon lekker glijden. Kom je de schaatsen terugbrengen?",
    "quest.guhs.elftocht.al_bezig": "Je hebt al schaatsen, njeg! Eerst deze rit afmaken of stoppen.",
    "quest.guhs.elftocht.gestopt": "Schaatsen weer terug. Dankjewel! Kom je snel weer glijden?",
    "quest.guhs.elftocht.finish": "FINISH! %s! Hier zijn %s elfstempels (+%s voor je snelheid). Wat ben jij vahoeg!",
    # --- the Stempelguhs ------------------------------------------------------------------------------------------------
    "quest.guhs.elftocht.stempel.geen_tocht": "Hoi! Ik ben de Stempelguh van %s. Wil je een stempel? Haal eerst een stempelkaart bij Schaatsmeester Guhglij in Guhwarden!",
    "quest.guhs.elftocht.stempel.geen_tocht_start": "Hoi! Ik geef de laatste stempel, in %s: de finish! Begin bij Schaatsmeester Guhglij, hier vlakbij.",
    "quest.guhs.elftocht.stempel.vrij": "Vrij schaatsen is zonder stempels, njeg! Maar een knuffel van %s mag altijd.",
    "quest.guhs.elftocht.stempel.wacht": "Wacht op het startschot, vadsig ongeduldig guhtje!",
    "quest.guhs.elftocht.stempel.al": "Die stempel van %s heb je al! Door naar %s, vahoeg!",
    "quest.guhs.elftocht.stempel.volgorde": "Njeg! Dit is niet de goede volgorde. Eerst naar %s!",
    "quest.guhs.elftocht.stempel.plof": "PLOF! Stempel van %s! Nog %s te gaan. Door naar %s!",
    "quest.guhs.elftocht.stempel.kwijt": "Huh? Ik weet niet eens meer bij welk dorpje ik hoor... njeg.",
    # --- during the tour ------------------------------------------------------------------------------------------------
    "gui.guhs.elftocht.klaar": "Klaar voor de start...",
    "gui.guhs.elftocht.go": "Op naar %s!",
    "gui.guhs.elftocht.balk": "⏱ %s  ·  volgende: %s (%s/%s)",
    "gui.guhs.elftocht.split": "%s: %s",
    "gui.guhs.elftocht.vrij_balk": "Vrij schaatsen · breng de schaatsen terug naar Guhglij om te stoppen",
    "gui.guhs.elftocht.verlaten": "Je bent van de tocht af geschaatst, njeg. De schaatsen gaan terug naar Guhglij.",
    "gui.guhs.elftocht.bijna_weg": "Hé, niet zo ver weg van de polder, njeg! Schaats terug, anders gaan je schaatsen terug naar Guhglij.",
    "gui.guhs.elftocht.te_lang": "Zo lang?! De Stempelguhs gaan slapen. De tocht is voorbij, probeer het nog eens!",
    "gui.guhs.elftocht.finish": "FINISH!",
    "gui.guhs.elftocht.record": "Nieuw persoonlijk record! VAHOEG!",
    "gui.guhs.elftocht.kruisje": "Je eerste Elf-Guhjestocht! Je krijgt het Elf-Guhjeskruisje en %s elfstempels extra. Zet hem op een mooi plekje!",
    "gui.guhs.elftocht.server_record": "De snelste schaatsguh van de hele wereld! Je staat bovenaan het bord!",
    "gui.guhs.elftocht.kaart.titel": "Stempelkaart Elf-Guhjestocht",
    "gui.guhs.elftocht.no_build": "Niet aan het ijs komen! De Elf-Guhjestocht moet mooi blijven voor iedereen.",
    # --- his screen ------------------------------------------------------------------------------------------------------
    "gui.guhs.elftocht.regels": "Schaats het bevroren kanaal rond en haal bij elke Stempelguh een stempel, in de goede volgorde: "
                                "van Snuh tot Dokguh, en in Guhwarden de laatste (de finish). Je tijd komt op het bord. Onderweg: "
                                "warme chocovet en snert voor een boost, en een juichend publiek. Elke tocht: 12 elfstempels, tot 8 extra als je snel "
                                "bent, en de eerste keer nog 5 extra. Hoe sneller, hoe meer elfstempels!",
    "gui.guhs.elftocht.knop.start": "Start de Elf-Guhjestocht!",
    "gui.guhs.elftocht.knop.start.tooltip": "Schaatsen en een stempelkaart lenen, naar de startstreep, en op het fluitje: VAHOEG!",
    "gui.guhs.elftocht.knop.vrij": "Vrij schaatsen",
    "gui.guhs.elftocht.knop.vrij.tooltip": "Schaatsen lenen en rondglijden zonder klok (geen elfstempels).",
    "gui.guhs.elftocht.knop.stop": "Stoppen",
    "gui.guhs.elftocht.knop.stop.tooltip": "De schaatsen teruggeven (je rit telt niet).",
    "gui.guhs.elftocht.knop.winkel": "Winkeltje",
    "gui.guhs.elftocht.knop.winkel.tooltip": "Wintertruitjes, sjaals, mutsen en warme drankjes voor elfstempels.",
    "gui.guhs.elftocht.jouw_best": "Jouw beste tijd: %s (%s keer uitgereden)",
    "gui.guhs.elftocht.nog_nooit": "Je hebt de tocht nog nooit uitgereden. Tijd voor je eerste Elf-Guhjeskruisje!",
    "gui.guhs.elftocht.server_best": "Record: %s in %s",
    "gui.guhs.elftocht.geen_record": "Nog geen record. Word jij de eerste?",
    "gui.guhs.scorebord.elftocht": "Elf-Guhjestocht",
    "gui.guhs.scorebord.elftocht.tijd": "Snelste tochten",
    # --- sounds ----------------------------------------------------------------------------------------------------------
    "subtitles.guhs.elftocht.glij": "Schaatsen glijden",
    "subtitles.guhs.elftocht.kras": "Schaats krast over het ijs",
    "subtitles.guhs.elftocht.plof": "Stempel: PLOF!",
    "subtitles.guhs.elftocht.fluit": "Startfluitje",
    "subtitles.guhs.elftocht.juich": "Publiek juicht: VAHOEG!",
    "subtitles.guhs.elftocht.finish": "Finish-fanfare",
}

for n, naam in DORPEN.items():
    TEKSTEN[f"gui.guhs.elftocht.dorp.{n}"] = naam
    TEKSTEN[f"gui.guhs.elftocht.stempelguh.{n}"] = f"Stempelguh van {naam}"

ADVANCEMENTS = {
    "elftocht_gevonden": ("Een bevroren kanaal!", "Vind de Elf-Guhjestocht in de Guhpolder"),
    "elftocht_eerste_rit": ("It giet oan!", "Start de Elf-Guhjestocht bij Schaatsmeester Guhglij"),
    "elftocht_uitgereden": ("Elf-Guhjeskruisje", "Schaats de hele Elf-Guhjestocht uit: elf stempels, in de goede volgorde"),
    "elftocht_snel": ("Supervahoege schaatsguh", "Rij de Elf-Guhjestocht in minder dan 3:40"),
    "elftocht_kleding": ("Warm ingepakt", "Ontgrendel de schaatsmuts, het schaatstruitje, de sjaal en de oorwarmers"),
}
