"""
Every Dutch text of Among Guhs (the engine): names, the HUD, the queue, the meeting with its ready-made statements, the
ship map, the Kapitein-guh and the Logboek-guh, the Guhdex section, the titles. English: tools/lang/en/c34_px_among.json.

In a statement %1$s is the participant it is about and %2$s the room; every kind has three wordings (.0, .1, .2).
Room names carry their article because they are used inside sentences ("Ik was in de Kantine").
"""

KLEUREN = {
    "rood": "Rood", "blauw": "Blauw", "groen": "Groen", "geel": "Geel", "roze": "Roze", "oranje": "Oranje", "paars": "Paars", "wit": "Wit",
    "bruin": "Bruin", "mint": "Mint",
}
KAMERS = {
    "slaapzaal": "de Slaapzaal", "kantine": "de Kantine", "navigatie": "de Navigatie", "reactor": "de Reactor", "ziekenboeg": "de Ziekenboeg",
    "elektra": "Elektra", "machinekamer": "de Machinekamer", "voorraadkamer": "de Voorraadkamer", "schildkamer": "de Schildkamer",
    "gang": "de gang",
}
# kind: (name, what the placeholder panel says while its bar fills)
TAKEN = {
    "worstjes": ("Worstjes knopen", "De worstjes worden aan elkaar geknoopt. Rood aan rood, njeg."),
    "pasje": ("Pasje door de lezer", "Het pasje gaat door de lezer. Niet te vlug, niet te traag."),
    "kruimelbak": ("Kruimelbak legen", "De hendel is over. Alle kruimels vliegen de ruimte in."),
    "pindasaus": ("Pindasaus tanken", "De pindasaus klotst de tank in. Niet proeven!"),
    "sorteren": ("Knabbels sorteren", "Kaasknabbels links, gewone knabbels rechts, kruimels in je snoet."),
    "dromen": ("Dromen downloaden", "De dromen komen binnen. Dit kan even duren, njeg..."),
    "wegen": ("Wegen", "Stilstaan op de weegschaal... Resultaat: vads."),
    "schakelaars": ("Schakelaars goedzetten", "Klik, klak, klik. Alle schakelaars omhoog."),
    "herstel_licht": ("Het licht repareren", "De schakelaars gaan een voor een weer aan."),
    "herstel_alarm": ("Alarmcode intoetsen", "De code: k-n-a-b-b-e-l. Niet verklappen."),
}
UITSPRAKEN = {
    "gevonden": ("Ik vond %1$s slapend in %2$s!", "%1$s ligt te pitten in %2$s. Dat ging niet vanzelf, njeg.", "Help! %1$s slaapt in %2$s!"),
    "knop": ("Ik drukte op de knop. Er klopt iets niet aan boord.", "Noodvergadering! Ik heb iets geks gezien.",
             "Sorry voor het storen, maar dit moest even, njeg."),
    "waar_ik": ("Ik was in %2$s.", "Ik zat gewoon in %2$s met mijn taak.", "Ik? In %2$s. Echt waar, njeg."),
    "gezien": ("Ik zag %1$s in %2$s.", "%1$s liep net nog rond in %2$s.", "Volgens mij was %1$s in %2$s."),
    "bij_slaper": ("Ik zag %1$s vlak bij de slaper in %2$s!", "%1$s stond er zo naast in %2$s. Sus, njeg.",
                   "Toen ik binnenkwam in %2$s liep %1$s net weg."),
    "luik": ("Ik zag %1$s uit het luik komen!", "%1$s kroop door de ventilatie. Dat doet alleen een Mika!",
             "Het luik ging open en daar was %1$s. Njeg!"),
    "duw": ("Ik zag %1$s iemand in slaap duwen!", "%1$s deed het! Met een kussen! Ik zag het zelf!", "%1$s is de Mika. Ik stond erbij, njeg."),
    "verdenk": ("Ik verdenk %1$s.", "%1$s is sus, als je het mij vraagt.", "Ik vertrouw %1$s niet zo."),
    "sta_in": ("Ik sta in voor %1$s, die was bij mij.", "%1$s was de hele tijd bij mij. Lief vadsje.", "%1$s kan het niet zijn: wij waren samen."),
    "klopt_niet": ("Dat klopt niet: ik zag %1$s net nog in %2$s.", "%1$s was daar helemaal niet. Ik zag je in %2$s!",
                   "%1$s jokt. Die was in %2$s, njeg."),
    "niks": ("Ik heb niks gezien, njeg.", "Ik sliep bijna. Wat is er gebeurd?", "Geen idee. Ik dacht aan knabbels."),
    "sus": ("%1$s is sus, njeg.", "Ik zeg het maar: het is altijd %1$s.", "%1$s kijkt zo verdacht."),
}
ZEG = {
    "waar_ik": "Ik was in...", "gezien": "Ik zag...", "bij_slaper": "Bij de slaper...", "verdenk": "Ik verdenk...", "sta_in": "Ik sta in voor...",
    "duw": "Duw gezien!", "luik": "Luik gezien!", "niks": "Niks gezien",
}

TEXTS = {
    # --- names ---
    "block.guhs.among_taakpaneel": "Taakjes-paneel",
    "block.guhs.among_taakpaneel.lore": "Taak 1 van 1: niks doen. Gelukt, njeg!",
    "block.guhs.among_noodknop": "Noodknop",
    "block.guhs.among_noodknop.lore": "Alleen drukken bij nood. Of bij verveling.",
    "block.guhs.among_ventilatieluik": "Ventilatieluik",
    "block.guhs.among_ventilatieluik.lore": "Daar past alleen een Mika door.",
    "item.guhs.among_kussen": "Mika-kussentje",
    "item.guhs.among_saboteerkaart": "Saboteerkaart",
    "item.guhs.among_stembriefje": "Stembriefje",
    "entity.guhs.among_guh": "Ruimteguh",
    "gui.guhs.among.pakje": "Ruimtepakje (%s)",
    "gui.guhs.among.niveau.normaal": "Normaal",
    "gui.guhs.among.niveau.lastig": "Lastig",
    # --- the start of a round ---
    "gui.guhs.among.rol.crew": "JIJ BENT CREW",
    "gui.guhs.among.rol.crew.onder": "Doe je taken en vind de Mika",
    "gui.guhs.among.rol.mika": "JIJ BENT DE MIKA",
    "gui.guhs.among.rol.mika.onder": "Duw iedereen in slaap. Laat je niet zien, njeg",
    "gui.guhs.among.begin.crew": "Je bent %1$s van de crew van De Vadsvaarder. Mika's aan boord: %2$s. Doe je taken bij de panelen (ze staan "
                                 "linksboven), meld een slaper met rechtsklik en druk op de noodknop in de Kantine als je iets geks ziet.",
    "gui.guhs.among.begin.mika": "Je bent %1$s en stiekem een Mika (Mika's aan boord: %2$s). Kussentje: duw een guh in slaap als niemand "
                                 "kijkt. Saboteerkaart: licht uit, Knabbelalarm of deuren dicht. Rechtsklik op een luik om weg te kruipen. "
                                 "En doe alsof je taken doet!",
    "gui.guhs.among.begin.maat": "Je mede-Mika is %s. Niet tegen elkaar aan duwen, njeg.",
    # --- what happens ---
    "gui.guhs.among.geduwd": "DUTJE!",
    "gui.guhs.among.geduwd.onder": "%s duwde je in slaap. Je droomt gewoon verder.",
    "gui.guhs.among.droomguh": "Je bent nu een droomguh: je zweeft, gaat door muren en niemand ziet je. Je taken tellen nog mee, dus maak ze af! "
                               "Alleen andere droomguhs horen wat je zegt.",
    "gui.guhs.among.droomchat": "[droomguh] %1$s: %2$s",
    "gui.guhs.among.gemeld": "%1$s vond %2$s slapend! Vergadering in de Kantine.",
    "gui.guhs.among.knop": "%s drukte op de noodknop! Vergadering in de Kantine.",
    "gui.guhs.among.zegt": "%1$s: %2$s",
    "gui.guhs.among.stemmen": "Tijd om te stemmen: kies een guh of sla over.",
    "gui.guhs.among.uitslag": "DE UITSLAG",
    "gui.guhs.among.uitslag.niemand": "Niemand is weggestemd.",
    "gui.guhs.among.uitslag.gelijk": "Gelijkspel: niemand is weggestemd.",
    "gui.guhs.among.uitslag.mika": "%1$s vliegt met een kussen naar de Slaapzaal. %1$s was de Mika!",
    "gui.guhs.among.uitslag.geen_mika": "%1$s vliegt met een kussen naar de Slaapzaal. %1$s was niet de Mika...",
    "gui.guhs.among.weggestemd.jij": "Je bent weggestemd en ligt nu lekker in de Slaapzaal. Als droomguh kun je je taken nog afmaken.",
    "gui.guhs.among.sabotage.licht": "LICHT UIT!",
    "gui.guhs.among.sabotage.licht.onder": "Zet de schakelaars goed in Elektra",
    "gui.guhs.among.sabotage.licht.klaar": "%s heeft het licht weer aangedaan.",
    "gui.guhs.among.sabotage.alarm": "KNABBELALARM!",
    "gui.guhs.among.sabotage.alarm.onder": "Toets de code in bij de Reactor en in de Navigatie, binnen 45 tellen",
    "gui.guhs.among.sabotage.alarm.half": "%1$s toetste de code in (%2$s). Nog een paneel!",
    "gui.guhs.among.sabotage.alarm.klaar": "Het Knabbelalarm is uit: %s toetste de laatste code in.",
    "gui.guhs.among.sabotage.deuren": "De deuren van %s zitten even dicht!",
    "gui.guhs.among.taak.klaar": "Taak klaar, njeg!",
    "gui.guhs.among.taak.stap": "Stap klaar: op naar het volgende paneel",
    "gui.guhs.among.taak.stop": "Stoppen",
    "gui.guhs.among.taak.waar": "Paneel in %s",
    "gui.guhs.among.taak.waar_stap": "Paneel in %1$s, stap %2$s van %3$s",
    "gui.guhs.among.einde.crew_wint": "DE CREW WINT",
    "gui.guhs.among.einde.mika_wint": "DE MIKA WINT",
    "gui.guhs.among.einde.taken": "Alle taken zijn af.",
    "gui.guhs.among.einde.mikas_weg": "Er is geen Mika meer wakker.",
    "gui.guhs.among.einde.overmacht": "Er zijn te weinig wakkere guhs over.",
    "gui.guhs.among.einde.alarm": "Het Knabbelalarm liep af: alle knabbels zijn op.",
    "gui.guhs.among.einde.verlaten": "De ronde is gestopt.",
    "gui.guhs.among.einde.mikas": "Stiekem een Mika: %s.",
    "gui.guhs.among.einde.gewonnen": "Gewonnen! Dat is %1$s muntjes erbij (vandaag kun je er nog %2$s verdienen).",
    "gui.guhs.among.einde.verloren": "Verloren, maar gezellig was het wel. Dat is %1$s muntjes erbij (vandaag kun je er nog %2$s verdienen).",
    # --- "that does not work" ---
    "gui.guhs.among.nee.afkoel": "Nog %s tellen wachten, njeg",
    "gui.guhs.among.nee.te_ver": "Daar sta je te ver vanaf",
    "gui.guhs.among.nee.niet_nu": "Dat kan nu even niet",
    "gui.guhs.among.nee.mag_niet": "Dat mag jij niet, njeg",
    "gui.guhs.among.nee.knop_op": "Je hebt je noodknop al gebruikt",
    "gui.guhs.among.nee.knop_alarm": "Eerst het Knabbelalarm uitzetten!",
    "gui.guhs.among.nee.droom": "Droomguhs kunnen dat niet",
    "gui.guhs.among.nee.luik": "Daar past alleen een Mika door, njeg",
    "gui.guhs.among.nee.geen_taak": "Hier heb jij nu niks te doen",
    "gui.guhs.among.nee.niets_stuk": "Hier is niks stuk",
    "gui.guhs.among.nee.taak_mislukt": "Dat ging te vlug: probeer het nog eens",
    "gui.guhs.among.nee.zeggen": "Dat kun je nu niet zeggen",
    "gui.guhs.among.nee.geen_vergadering": "Er is nu geen vergadering. Je taken staan linksboven",
    "gui.guhs.among.thuis.taakpaneel": "Taak 1 van 1: niks doen. Gelukt, njeg!",
    "gui.guhs.among.thuis.noodknop": "Noodvergadering! ...er komt niemand, iedereen slaapt",
    "gui.guhs.among.thuis.ventilatieluik": "Daar pas je niet door. Te veel knabbels gehad, njeg",
    "gui.guhs.among.thuis.spelitem": "Dit werkt alleen aan boord van De Vadsvaarder",
    # --- the HUD ---
    "gui.guhs.among.hud.crew": "CREW",
    "gui.guhs.among.hud.mika": "MIKA",
    "gui.guhs.among.hud.droomguh": "droomguh",
    "gui.guhs.among.hud.balk": "Taken van de crew: %1$s/%2$s",
    "gui.guhs.among.hud.taak": "%1$s, %2$s",
    "gui.guhs.among.hud.taak_stappen": "%1$s, %2$s (%3$s/%4$s)",
    "gui.guhs.among.hud.duwen": "Kussentje: %s",
    "gui.guhs.among.hud.saboteren": "Saboteren: %s",
    "gui.guhs.among.hud.klaar": "klaar!",
    "gui.guhs.among.hud.bezig": "bezig",
    "gui.guhs.among.hud.seconden": "%s tellen",
    "gui.guhs.among.hud.licht": "Licht uit! Schakelaars in Elektra",
    "gui.guhs.among.hud.alarm": "KNABBELALARM: %1$s (%2$s/2 codes)",
    "gui.guhs.among.hud.deuren": "Deuren dicht: %1$s (%2$s)",
    # --- the ship map ---
    "gui.guhs.among.kaart.sabotage": "Saboteerkaart",
    "gui.guhs.among.kaart.luik": "Ventilatieluik",
    "gui.guhs.among.kaart.luik.uitleg": "Waar kruip je heen?",
    "gui.guhs.among.kaart.naar": "Naar %s",
    "gui.guhs.among.kaart.licht": "Licht uit",
    "gui.guhs.among.kaart.alarm": "Knabbelalarm",
    "gui.guhs.among.kaart.deuren": "Deuren dicht van:",
    "gui.guhs.among.kaart.kies": "Kies wat je saboteert",
    "gui.guhs.among.kaart.afkoel": "Weer saboteren over %s",
    "gui.guhs.among.kaart.bezig": "Er is al iets stuk, njeg",
    # --- the queue ---
    "gui.guhs.among.wachtrij.titel": "Among Guhs: de wachtrij",
    "gui.guhs.among.wachtrij.regels.normaal": "Normaal: 9 deelnemers, 1 Mika",
    "gui.guhs.among.wachtrij.regels.lastig": "Lastig: 10 deelnemers, 2 Mika's, anderhalf keer de muntjes",
    "gui.guhs.among.wachtrij.klaar": "Ik ben klaar!",
    "gui.guhs.among.wachtrij.niet_klaar": "Toch niet klaar",
    "gui.guhs.among.wachtrij.niveau": "Niveau: %s",
    "gui.guhs.among.wachtrij.verlaten": "Wachtrij verlaten",
    "gui.guhs.among.wachtrij.sluiten": "Sluiten",
    "gui.guhs.among.wachtrij.leider": "%s (leider)",
    "gui.guhs.among.wachtrij.is_klaar": "klaar",
    "gui.guhs.among.wachtrij.wacht": "wacht...",
    "gui.guhs.among.wachtrij.guhs": "+ %s guhs in ruimtepakjes",
    "gui.guhs.among.wachtrij.stand": "%1$s van %2$s klaar",
    "gui.guhs.among.wachtrij.eerst_oefenen": "Speel eerst het oefenrondje bij de Kapitein-guh",
    "gui.guhs.among.wachtrij.vol": "De wachtrij is vol, njeg",
    "gui.guhs.among.wachtrij.eruit": "Je bent uit de wachtrij gelopen",
    # --- the meeting ---
    "gui.guhs.among.vergadering.titel": "Vergadering",
    "gui.guhs.among.vergadering.bespreken": "Bespreken: %s",
    "gui.guhs.among.vergadering.stemmen": "Stemmen: %s",
    "gui.guhs.among.vergadering.uitslag": "De uitslag",
    "gui.guhs.among.vergadering.droomguh": "Je droomt: je kijkt alleen mee.",
    "gui.guhs.among.vergadering.stem": "Stem",
    "gui.guhs.among.vergadering.overslaan": "Overslaan",
    "gui.guhs.among.vergadering.chatten": "Chatten",
    "gui.guhs.among.vergadering.jij": "%s (jij)",
    "gui.guhs.among.vergadering.weg": "weg",
    "gui.guhs.among.vergadering.slaapt": "slaapt",
    "gui.guhs.among.vergadering.stemmen_n": "%s st.",
    "gui.guhs.among.vergadering.maat": "Mika",
    "gui.guhs.among.vergadering.gestemd": "Je stem is binnen.",
    "gui.guhs.among.vergadering.straks": "Straks mag je stemmen.",
    "gui.guhs.among.vergadering.overgeslagen": "Overgeslagen: %s",
    "gui.guhs.among.zeg.kies": "Zeg iets (nog %s keer):",
    "gui.guhs.among.zeg.wie": "%s Over wie?",
    "gui.guhs.among.zeg.waar": "%s Waar?",
    "gui.guhs.among.zeg.op": "Je hebt genoeg gezegd, njeg.",
    # --- the Kapitein-guh and the Logboek-guh ---
    "quest.guhs.among.kapitein.hallo": "Welkom aan boord van De Vadsvaarder, njeg! Ergens tussen de crew zit een Mika die iedereen in slaap "
                                       "duwt. Durf je mee?",
    "quest.guhs.among.kapitein.optie.spelen": "Ik doe mee!",
    "quest.guhs.among.kapitein.optie.uitleg": "Hoe werkt het?",
    "quest.guhs.among.kapitein.uitleg": "Crew: doe je taken bij de panelen, meld slapers en stem de Mika weg. Mika: duw iedereen in slaap zonder "
                                        "dat iemand het ziet. Niemand doet elkaar pijn: wie slaapt, droomt verder als droomguh. Winnen geeft "
                                        "%1$s muntjes, verliezen %2$s, en elke eigen taak %3$s. Hooguit %4$s muntjes per dag.",
    "quest.guhs.among.kapitein.eerst_oefenen": "Eerst het oefenrondje, njeg. Daarna mag je het echt proberen.",
    "quest.guhs.among.logboek.cijfers": "Jouw logboek: %1$s rondes gespeeld. Gewonnen als crew: %2$s. Gewonnen als Mika: %3$s. Onterecht "
                                        "weggestemd: %4$s keer (sneu, njeg). Taken gedaan: %5$s. Vandaag verdiend: %6$s van de %7$s muntjes.",
    "gui.guhs.among.logboek.kop": "JOUW CIJFERS",
    "gui.guhs.among.logboek.regel": "Logboek van De Vadsvaarder",
    # --- the Guhdex ---
    "gui.guhs.among.gids.kop": "Among Guhs",
    "gui.guhs.among.gids.uitleg": "Het echte spel bij de Kapitein-guh: taken doen, slapers melden en de Mika wegstemmen. Of zelf de Mika zijn, njeg.",
    "gui.guhs.among.gids.rondes": "Rondes gespeeld",
    "gui.guhs.among.gids.winst_crew": "Gewonnen als crew",
    "gui.guhs.among.gids.winst_mika": "Gewonnen als Mika",
    "gui.guhs.among.gids.onterecht": "Onterecht weggestemd",
    "gui.guhs.among.gids.taken": "Taken gedaan",
    "gui.guhs.among.gids.vandaag": "Muntjes vandaag",
    # --- titles ---
    "gui.guhs.titels.naam.among_sus": "Sus",
    "gui.guhs.titels.hint.among_sus": "Word drie keer weggestemd in Among Guhs",
    "gui.guhs.titels.naam.among_onterecht": "Onterecht weggestemd",
    "gui.guhs.titels.hint.among_onterecht": "Word weggestemd terwijl je niet de Mika was",
    "gui.guhs.titels.naam.among_kussenkampioen": "Kussenkampioen",
    "gui.guhs.titels.hint.among_kussenkampioen": "Win vijf rondes Among Guhs als Mika",
}
for _kleur, _naam in KLEUREN.items():
    TEXTS[f"gui.guhs.among.kleur.{_kleur}"] = _naam
for _kamer, _naam in KAMERS.items():
    TEXTS[f"gui.guhs.among.kamer.{_kamer}"] = _naam
for _soort, (_naam, _bezig) in TAKEN.items():
    TEXTS[f"gui.guhs.among.taak.{_soort}"] = _naam
    TEXTS[f"gui.guhs.among.taak.{_soort}.bezig"] = _bezig
for _soort, _varianten in UITSPRAKEN.items():
    for _i, _zin in enumerate(_varianten):
        TEXTS[f"gui.guhs.among.uitspraak.{_soort}.{_i}"] = _zin
for _soort, _knop in ZEG.items():
    TEXTS[f"gui.guhs.among.zeg.{_soort}"] = _knop
