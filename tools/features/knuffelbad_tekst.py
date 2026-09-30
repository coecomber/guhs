"""
Het Knuffelbad (2.8) - all texts (Dutch in every language, with plenty of vads, njeg and VAHOEG).
"""

GLIJBANEN = {"roze_trechter": "Roze Trechter", "glimtunnel": "Glimtunnel", "grote_plons": "Grote Plons"}

EENDJES = {  # id: (name, where / what)
    "vadseendje": ("Vadseendje", "Een extra rond eendje met een vadsig buikje. Je kunt nooit te vads zijn, alleen nog niet vahoeg genoeg. VAHOEG!"),
    "kaaseendje": ("Kaaseendje", "Geel met gaatjes, net een stukje kaas. Ruikt een beetje naar kaasknabbels. Njeg, niet opeten!"),
    "guheendje": ("Guheendje", "Een eendje met guhoortjes en roze blosjes. Het zegt geen kwak, het zegt guh."),
    "badmeestereendje": ("Badmeester-eendje", "Met een rood petje en een fluitje, net Badmeester Bubbel. Tuuut, niet rennen!"),
    "pluiseendje": ("Pluiseendje", "Superzacht en roze, met een pluizig kuifje. Drijft alleen in de Roze Trechter."),
    "trechtereendje": ("Trechtereendje", "Heeft een spiraal op zijn buik en is een beetje duizelig. Zit alleen in de Roze Trechter."),
    "sterreneendje": ("Sterreneendje", "Donkerblauw met gloeiende sterretjes. Je vindt het alleen in de Glimtunnel."),
    "glimeendje": ("Glimeendje", "Het gloeit helemaal in het donker! Alleen in de Glimtunnel. Glim glim, VAHOEG!"),
    "maaneendje": ("Maaneendje", "Een slaperig eendje met een slaapmutsje en een maantje. Alleen in de Glimtunnel. Zzz... kwak."),
    "duikeendje": ("Duikeendje", "Met een snorkel en een duikbril, klaar voor de Grote Plons. Alleen daar te vinden."),
    "plonseendje": ("Plonseendje", "Hemelsblauw met een waterkroontje: het koninkje van de Grote Plons."),
    "gouden_eendje": ("Gouden eendje", "Het zeldzaamste eendje van het hele Knuffelbad! Het glanst als een gouden kaasknabbel. Wie hem pakt is supervahoeg."),
}

MIJLPALEN = {
    "knuffelbad_gevonden": "Het Knuffelbad gevonden",
    "knuffelbad_eerste_was": "De eerste wasbeurt",
    "knuffelbad_wassen": "10 guhs gewassen",
    "knuffelbad_roze_trechter": "De Roze Trechter af",
    "knuffelbad_glimtunnel": "De Glimtunnel af",
    "knuffelbad_grote_plons": "De Grote Plons af",
    "knuffelbad_eendjes": "100 badeendjes gepakt",
    "knuffelbad_record": "250 punten op een glijbaan",
}

ADVANCEMENTS = {  # name: (title, description)
    "knuffelbad_gevonden": ("Plons!", "Vind het Knuffelbad aan de guhzee"),
    "knuffelbad_gewassen": ("Schoon en vahoeg", "Was je eigen guh in een guh-wastobbe: inzepen, schuimen, spoelen, föhnen"),
    "knuffelbad_glijbaan": ("Wieeeee!", "Glijd van een glijbaan van het Knuffelbad"),
    "knuffelbad_alle_glijbanen": ("Glijbaankampioen", "Glijd van alle drie de glijbanen: de Roze Trechter, de Glimtunnel en de Grote Plons"),
    "knuffelbad_badeendjes": ("Eendjesverzamelaar", "Vind alle twaalf bijzondere badeendjes op de glijbanen"),
}

TIPS = [
    "Stuur met links en rechts (A en D) naar de eendjes. Pak je er een paar achter elkaar, dan krijg je een combo. VAHOEG!",
    "In de trechters word je vanzelf naar buiten tegen de wand geduwd. Stuur naar binnen voor de eendjes onderin!",
    "De Glimtunnel is pikkedonker, maar de sterretjes gloeien. Op de glimeendjes en sterreneendjes moet je goed letten, njeg.",
    "Bij de Grote Plons vlieg je aan het eind door de lucht. Hoe harder je gaat, hoe groter de plons!",
    "Een gewassen guh glanst een hele dag. Zo schoon en pluizig! Je kunt nooit te vads zijn, alleen nog niet vahoeg genoeg. VAHOEG!",
    "De Mika's pikken soms kaasknabbels uit de badtassen. Njeg! Hou je spullen bij je, dan word je guh vahoeg genoeg.",
    "Er is een gouden eendje. Ik heb hem zelf maar één keer gezien. Het was prachtig. *snif*",
    "Houd sluipen ingedrukt als je echt van de glijbaan af wilt. Maar dan krijg je geen eendjesmunten, hoor.",
]

LANG = {
    # blocks and items
    "block.guhs.guh_wastobbe": "Guh-wastobbe",
    "block.guhs.glijbaan_start": "Glijbaanpoortje",
    "block.guhs.glimtegel": "Glimtegel",
    "block.guhs.trechtertegel": "Trechtertegel",
    "block.guhs.knuffelbad_glijgoot": "Glijgoot",
    "block.guhs.knuffelbad_schuim": "Roze badschuim",
    "block.guhs.knuffelbad_badtegel": "Badtegel",
    "item.guhs.eendjesmunt": "Eendjesmunt",
    "item.guhs.guhshampoo": "Guhshampoo",
    "item.guhs.guhshampoo.lore": "Op je tamme guh bij een guh-wastobbe: stap 1 van het wasritueel",
    "item.guhs.guh_fohn": "Guh-föhn",
    "item.guhs.guh_fohn.lore": "Rechtermuis ingedrukt houden op een gespoelde guh: droog, pluizig en glanzend!",
    "item.guhs.knuffelbad_badeendje": "Badeendje",
    "item.guhs.knuffelbad_badeendje.lore": "Zet hem op het water: hij dobbert en piept als je hem aait",
    "item.guhs.badmutsje": "Badmutsje",
    "item.guhs.badjasje": "Badjasje",
    "entity.guhs.zwembandje": "Zwembandje",
    "entity.guhs.badeendje": "Badeendje",
    "entity.guhs.guh_npc.badmeesterguh": "Badmeester Bubbel",
    "gui.guhs.guhdex.rarity.badmeesterguh": "Zeldzaamheid: Zeldzaam (Knuffelbad, aan de guhzee)",
    "gui.guhs.guhdex.info.badmeesterguh": "De badmeester van het Knuffelbad. Hij let op de drie glijbanen, leert je hoe je een guh wast tot hij glanst, en blaast op zijn fluitje als je rent. Tuuut! Zijn winkeltje: badmutsjes, badjasjes en schuim voor eendjesmunten.",
    "structure.guhs.knuffelbad": "Knuffelbad",
    "structure.guhs.knuffelbad.tooltip": "Het zwembad aan de guhzee: een guhkop vol badschuim, wastobben en drie grote glijbanen",
    # sounds
    "subtitles.guhs.knuffelbad.plons": "PLONS!",
    "subtitles.guhs.knuffelbad.glijden": "Water ruist",
    "subtitles.guhs.knuffelbad.eendje_piep": "Badeendje piept",
    "subtitles.guhs.knuffelbad.schuim": "Schuim bubbelt",
    "subtitles.guhs.knuffelbad.fohn": "Föhn zoemt",
    "subtitles.guhs.knuffelbad.fluit": "Badmeester fluit: tuuut!",
    "subtitles.guhs.knuffelbad.spetter": "Water spettert",
    # the slides
    "gui.guhs.knuffelbad.glijbaan.roze_trechter": "Roze Trechter",
    "gui.guhs.knuffelbad.glijbaan.glimtunnel": "Glimtunnel",
    "gui.guhs.knuffelbad.glijbaan.grote_plons": "Grote Plons",
    "gui.guhs.knuffelbad.bezet": "Njeg, %s glijdt nog! Even wachten...",
    "gui.guhs.knuffelbad.start.roze_trechter": "De Roze Trechter! Twee trechters met een guhgezicht slikken je in, en dan plof je in het schuim. Er drijven %s eendjes op de baan.",
    "gui.guhs.knuffelbad.start.glimtunnel": "De Glimtunnel! Pikkedonker, rondjes om de toren van de Sterrenguh, en overal gloeiende sterretjes. Er drijven %s eendjes op de baan.",
    "gui.guhs.knuffelbad.start.grote_plons": "De Grote Plons! Over de tong van de Reuzeguh naar beneden, en dan... PLONS! Er drijven %s eendjes op de baan.",
    "gui.guhs.knuffelbad.besturing": "Sturen: links en rechts (A/D). Kijk rond met je muis. Stoppen: houd sluipen ingedrukt.",
    "gui.guhs.knuffelbad.klaar": "Zwembandje om, pootjes erin...",
    "gui.guhs.knuffelbad.vahoeg": "VAHOEG!",
    "gui.guhs.knuffelbad.speciaal": "Een bijzonder badeendje: %s! Het staat nu in je Guhdex (Knus).",
    "gui.guhs.knuffelbad.plons": "PLONS!",
    "gui.guhs.knuffelbad.eindscore": "%s punten - %s van de %s eendjes",
    "gui.guhs.knuffelbad.uitslag": "%s: %s punten, %s van de %s eendjes, langste combo %s. Je krijgt %s eendjesmunten!",
    "gui.guhs.knuffelbad.alle_eendjes": "ALLE eendjes gepakt! Wat een kampioen, supervahoeg!",
    "gui.guhs.knuffelbad.record": "Nieuw record: %s punten (was %s)!",
    "gui.guhs.knuffelbad.eerste_record": "Je eerste record op deze glijbaan: %s punten!",
    "gui.guhs.knuffelbad.uitgestapt": "Je bent van de glijbaan gestapt. Njeg, volgende keer helemaal naar beneden!",
    "gui.guhs.knuffelbad.niet_bouwen": "Njeg! Het Knuffelbad is van Badmeester Bubbel: hier breken en bouwen we niks.",
    "gui.guhs.knuffelbad.hud.eendjes": "Eendjes: %s/%s",
    "gui.guhs.knuffelbad.hud.combo": "Combo x%s!",
    "gui.guhs.knuffelbad.hud.record": "Jouw record: %s - wereldrecord: %s",
    "gui.guhs.scorebord.knuffelbad": "Glijbaankampioenen van het Knuffelbad",
    # Badmeester Bubbel's screen
    "gui.guhs.knuffelbad.scherm.intro": "Drie glijbanen, twaalf bijzondere badeendjes en schuim tot aan je oren!",
    "gui.guhs.knuffelbad.scherm.jouw_record": "Jouw record: %s",
    "gui.guhs.knuffelbad.scherm.ritten": "Gegleden: %sx",
    "gui.guhs.knuffelbad.scherm.geen_record": "Nog geen wereldrecord",
    "gui.guhs.knuffelbad.scherm.wereldrecord": "Wereldrecord: %s",
    "gui.guhs.knuffelbad.scherm.munten": "Je hebt %s eendjesmunten",
    "gui.guhs.knuffelbad.scherm.wassen_telt": "Guhs gewassen: %s - bijzondere eendjes: %s van de %s",
    "gui.guhs.knuffelbad.scherm.besturing": "Klik bovenin een glijbaan op het poortje. Sturen met A en D!",
    "gui.guhs.knuffelbad.scherm.winkel": "Winkeltje",
    "gui.guhs.knuffelbad.scherm.winkel.tooltip": "Badmutsje, badjasje, shampoo, een föhn, tegels en schuim, voor eendjesmunten",
    "gui.guhs.knuffelbad.scherm.wassen": "Wassen?",
    "gui.guhs.knuffelbad.scherm.wassen.tooltip": "Hoe was je een guh tot hij glanst? (De eerste keer krijg je shampoo en een föhn!)",
    "gui.guhs.knuffelbad.scherm.tip": "Tip!",
    # the washing ritual
    "gui.guhs.knuffelbad.was.niet_van_jou": "Njeg, je kunt alleen je eigen tamme guh wassen.",
    "gui.guhs.knuffelbad.was.geen_tobbe": "Zoek eerst een guh-wastobbe (hooguit 6 blokjes verderop)!",
    "gui.guhs.knuffelbad.was.ingezeept": "%s zit in bad en is ingezeept! Nu schrobben: klik met een lege hand op je guh.",
    "gui.guhs.knuffelbad.was.schrobben": "Schrob schrob... %s van de %s",
    "gui.guhs.knuffelbad.was.geschuimd": "Wat een schuim! Nu afspoelen: klik op de wastobbe (de douche).",
    "gui.guhs.knuffelbad.was.gespoeld": "Schoon gespoeld! Nu föhnen: houd de guh-föhn op je guh gericht.",
    "gui.guhs.knuffelbad.was.fohnen": "Föhnen... %s%%",
    "gui.guhs.knuffelbad.was.klaar": "%s glanst! Zo pluizig en schoon. VAHOEG!",
    "gui.guhs.knuffelbad.was.klaar.chat": "%s is gewassen en glanst nu een hele dag. Je krijgt eendjesmunten van Badmeester Bubbel!",
    "gui.guhs.knuffelbad.was.iemand_anders": "Njeg, iemand anders is deze guh al aan het wassen.",
    "gui.guhs.knuffelbad.was.hint.inzepen": "Eerst inzepen: guhshampoo op je guh bij een wastobbe.",
    "gui.guhs.knuffelbad.was.hint.schuimen": "Eerst schrobben tot er genoeg schuim is (klik met een lege hand op je guh)!",
    "gui.guhs.knuffelbad.was.hint.spoelen": "Eerst afspoelen: klik op de wastobbe voor de douche!",
    "gui.guhs.knuffelbad.was.hint.fohnen": "Nu föhnen: houd de guh-föhn ingedrukt op je guh!",
    # Badmeester Bubbel talks
    "quest.guhs.knuffelbad.badmeester.hallo_nieuw": "Tuuut! Welkom in het Knuffelbad! Ik ben Badmeester Bubbel. Hier wassen we guhs tot ze glanzen, en we hebben de drie grootste glijbanen van de hele Guhmensie. Klik bovenin op een glijbaanpoortje, stap in je zwembandje en stuur naar de badeendjes. Niet rennen, wel VAHOEG!",
    "quest.guhs.knuffelbad.badmeester.hallo": "Tuuut! Daar ben je weer! Zin in een plons, of moet er een guh in bad?",
    "quest.guhs.knuffelbad.badmeester.niet_rennen": "TUUUUT! Niet rennen langs het bad! Glijden doen we alleen op de glijbaan, njeg. Wandel maar lekker vadsig, zoals een echte guh.",
    "quest.guhs.knuffelbad.badmeester.winkel": "Kijk maar rond. Alles voor eendjesmunten: die verdien je op de glijbanen en met wassen.",
    "quest.guhs.knuffelbad.badmeester.wassen": "Guhs wassen gaat zo: 1. guhshampoo op je tamme guh bij een wastobbe (dan springt hij erin), 2. schrobben met je handen tot hij onder het schuim zit, 3. douchen (klik op de tobbe), 4. föhnen tot hij droog is. Dan glanst hij een hele dag. Zo pluizig! Njeg, niet te vads? Nee hoor: nooit te vads, gewoon vahoeg.",
    "quest.guhs.knuffelbad.badmeester.cadeautje": "Hier, een flesje guhshampoo en een föhn van mij. Voor je eerste wasbeurt. Tuuut!",
    "advancements.guhs.knuffeldal.dummy": "",
}
for _i, _t in enumerate(TIPS):
    LANG[f"quest.guhs.knuffelbad.badmeester.tip{_i}"] = _t
for _id, _n in MIJLPALEN.items():
    LANG[f"gui.guhs.knus.mijlpaal.{_id}"] = _n
LANG["gui.guhs.knus.verzameling.badeendjes"] = "Badeendjes"
for _id, (_n, _info) in EENDJES.items():
    LANG[f"gui.guhs.knus.badeendjes.{_id}"] = _n
    LANG[f"gui.guhs.knus.badeendjes.{_id}.info"] = _info
del LANG["advancements.guhs.knuffeldal.dummy"]
