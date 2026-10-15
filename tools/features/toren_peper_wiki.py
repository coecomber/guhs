"""
Wiki texts of bbq2 (toren-peper): the Rookguh-vuurtoren and the Pepertuin with their questlines, the pepper plant and the two
pepper drinks. Not a feature module (not in FEATURES): the docs step of the merge reads WIKI and turns it into pages
(CONTRACT_130 2.4). The items, blocks, structures, clothes and the two NPCs get their own wiki pages from the game data.
"""
from features import wereld

WIKI = {
    "verhalen": dict([
        wereld.wiki_questlijn(
            "vuurtoren", "De Rookguh-vuurtoren",
            "In de rook van de Guhbarbecuether staat een rood-witte vuurtoren met twee roze guh-oren op zijn koperen dak. De lamp is uit, "
            "en zonder licht vinden de Rookguhs de weg naar huis niet. De Torenwachter-guh vraagt je de lamp aan te steken en drie "
            "verdwaalde Rookguhs naar het licht te brengen. Je krijgt een Bezorgguhtje-fluitje en een wachtersjas voor je guh.",
            "rookguh_vuurtoren", ["torenwachterguh"], [("guhs_barbecuether", "toren_peper_vuurtoren")], related=["systemen/bezorgguhtje"]),
        wereld.wiki_questlijn(
            "pepertuin", "De Pepertuin",
            "Een glazen kas met een tuin vol pepers ernaast. De Peperteler-guh leert je pepers kweken: één plantje, drie pepers, en de "
            "grond bepaalt welke. In zijn kas kweek je ze alle drie in je eigen kweekbakken, en daarna brouw je je eerste peperdrankje. "
            "Je krijgt zaadjes voor thuis, het andere drankje en een peperslinger voor je guh.",
            "pepertuin", ["pepertelerguh"], [("guhs_barbecuether", "toren_peper_pepertuin")], related=["systemen/pepers"])]),
    "systemen": {
        "pepers": ("Pepers en peperdrankjes", "item_torenpeper_vahoegpeper",
                   "De peperplant is de enige plant die het in de rook van de Guhbarbecuether naar zijn zin heeft. Eén plantje geeft "
                   "drie soorten pepers, afhankelijk van de grond. Twee ervan brouw je tot een Guhdrankje.",
                   ["verhalen/pepertuin", "dimensies/barbecuether"]),
    },
    "npc_home": {"torenwachterguh": "bouwwerken/rookguh_vuurtoren", "pepertelerguh": "bouwwerken/pepertuin"},
    "entity_home": {"torenpeper_verdwaalde_rookguh": "bouwwerken/rookguh_vuurtoren"},
    "tekst": [
        ("verhalen/vuurtoren", "Zo gaat het",
         "Praat met de Torenwachter-guh voor de toren. Breng hem vier gloeikoolgruis (hak een brok gloeikool stuk): hij perst er een "
         "lampkooltje van en leent je zijn seinlantaarn. Ga de toren in en klim de wenteltrap op, rondje na rondje, tot in het glazen "
         "lampenhuis. Klik met het lampkooltje op de lamp: floep, hij brandt, en de misthoorn toetert."),
        ("verhalen/vuurtoren", "De verdwaalde Rookguhs",
         "Zodra jouw lamp brandt, zweven er rond de toren drie magere, kleine Rookguhs. Het zijn jouw Rookguhs: boven elk ervan zie jij "
         "een sterretje. Loop ernaartoe met de seinlantaarn in je poot en ze zweven achter je aan. Raakt er een achterop of blijft hij "
         "achter een rots hangen, dan staat hij met een rookwolkje weer naast je. Dicht bij de toren ziet hij het licht: hij eet zich "
         "rond en roze en zweeft naar huis. Heb je er drie thuisgebracht, ga dan terug naar de Torenwachter-guh. Zes kaasknabbels "
         "voeren werkt ook: dan vliegt hij ook naar huis."),
        ("verhalen/vuurtoren", "Met zoveel als je wilt",
         "Iedereen doet het voor zichzelf. De lamp brandt zolang er iemand in de buurt is die hem heeft aangestoken, en gaat weer uit "
         "als die weg is. Zo vindt elke nieuwe speler een donkere toren. Elke speler heeft eigen verdwaalde Rookguhs; die van een ander "
         "volgen jou niet. Mislukken kan niet, en een kwijtgeraakt lampkooltje of een verloren seinlantaarn krijg je gewoon opnieuw."),
        ("verhalen/vuurtoren", "Daarna",
         "De lamp floept aan zodra jij in de buurt komt. Met roosterijzer tralies, glas en gloeikool maak je een eigen vuurtorenlamp voor "
         "thuis, die hetzelfde doet. Ben je je fluitje kwijt, dan snijdt de Torenwachter-guh elke dag een nieuwe voor je."),
        ("verhalen/pepertuin", "Zo gaat het",
         "Praat met de Peperteler-guh voor de kas: je krijgt drie peperzaadjes. In de kas staan links van het pad drie kweekbakken, met "
         "as-aarde, gloeikool en pindasaus-nylium. Plant in elke bak een zaadje. Na 45 seconden zijn ze rijp, ook als je even "
         "wegloopt. Klik op de bak om te plukken; je krijgt twee pepers en je zaadje terug. Heb je "
         "alle drie de soorten geplukt, dan geeft de Peperteler-guh je grillspiespoeder, een emmer kaassaus en drie flesjes. Achter in "
         "de kas staat zijn Guhbrouwketel: stook hem op, giet de saus erin, roer er een rode of roze peper door en vul een flesje. "
         "Laat hem daarna proeven."),
        ("verhalen/pepertuin", "Met zoveel als je wilt",
         "De plantjes in de kweekbakken zijn van jou alleen. Jij ziet je eigen plantje groeien, een ander ziet het zijne, in dezelfde "
         "bak. Niemand kan jouw pepers plukken en je hoeft nooit op elkaar te wachten. De kijkbedden aan de andere kant van het pad "
         "zijn van iedereen: een rijpe plant pluk je met een rechtsklik en hij groeit vanzelf weer aan."),
        ("systemen/pepers", "Drie pepers van één plant",
         "Plant peperzaadjes op as-aarde en je krijgt de groene Njegpeper: mild, om op te knabbelen. Op gloeikool groeit de rode "
         "Vahoegpeper: heet! Op pindasaus-nylium groeit de roze Snoeppeper: zoet. De plant heeft geen licht en geen water nodig. Een "
         "rijpe plant pluk je met een rechtsklik (twee of drie pepers), en dan groeit hij verder. Graaf je een rijpe plant uit, dan "
         "krijg je de pepers en een of twee zaadjes."),
        ("systemen/pepers", "Onder glas",
         "Buiten duurt elke groeistap ongeveer een minuut. Staat er ergens boven de plant glas, dan gaat het drie keer zo snel. Een kas "
         "bouwen loont dus. Er mag niets dichts tussen de plant en het glas zitten, en het glas mag hooguit acht blokken hoger zijn."),
        ("systemen/pepers", "De peperdrankjes",
         "Roer een Vahoegpeper door kaasbouillon in de Guhbrouwketel voor het Pepervuurdrankje: drie minuten lang hak en graaf je veel "
         "sneller, met vlammetjes uit je snoet (Peperadem: je kunt niet bevriezen). Een Snoeppeper geeft het Peperzoetdrankje: je "
         "geneest een halve minuut lang en krijgt twee minuten twee hartjes extra. Een rauwe Vahoegpeper opeten kan ook: je rent er "
         "tien seconden heel hard van. Met rook uit je oren."),
    ],
}
