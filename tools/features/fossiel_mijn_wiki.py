"""
bbq2 (fossiel-mijn): what the wiki step needs to know about features/fossiel_mijn.py (CONTRACT_130 2.4). Not in FEATURES: the
docs step of the merge reads WIKI. Items, blocks, structures and the two NPCs get their own page from the game data.
"""
from features import wereld

WIKI = {
    "verhalen": dict([
        wereld.wiki_questlijn(
            "archeoloog", "De Tyrannoguhrus Njex",
            "De Archeoloog-guh graaft in de Guhbarbecuether het skelet van een reuzenguh op. Het grote skelet blijft liggen, maar in "
            "het bottenzand van de put zit nog een kleintje in stukjes. Je krijgt een Guhkwastje, kwast vijf botten los en zet de "
            "Tyrannoguhrus Njex bot voor bot in elkaar op het rek onder het afdakje. Iedere speler vindt zijn eigen botten en ziet "
            "zijn eigen skelet: het zand en het rek veranderen voor niemand anders.",
            "fossiel_opgraving", ["archeoloogguh"], [("guhs_barbecuether", "fossiel_mijn_opgraving")], related=["mijnwerker"]),
        wereld.wiki_questlijn(
            "mijnwerker", "Zout op de boterham",
            "Het dak van de Zoutkristalmijn is naar beneden gekomen en het karrenspoor ligt vol puin. Hak vijf brokken van het "
            "spoor, volg het tot in de kristalgrot en hak in de roze kristalader. De Mijnwerker-guh wil drie zoutkristallen op zijn "
            "boterham en geeft je er zijn Zoutkristalhouweel voor terug.",
            "zoutkristalmijn", ["mijnwerkerguh"], [("guhs_barbecuether", "fossiel_mijn_mijn")], related=["archeoloog"]),
    ]),
    "systemen": {
        "zoutkristal": ("Zoutkristal", "zoutkristal",
                        "Zoutkristal is de grondstof voor de tweede stap van de Guh-technologie (buizen, sensoren, het Hapluikje, de "
                        "Knabbelbatterij). Het komt uit de kristalader van de Zoutkristalmijn, uit zoutkristalerts en soms uit bottenzand.",
                        ["mijnwerker"]),
    },
    "npc_home": {"archeoloogguh": "bouwwerken/fossiel_opgraving", "mijnwerkerguh": "bouwwerken/zoutkristalmijn"},
    "entity_home": {},
    "tekst": [
        ("zoutkristal", "De kristalader",
         "De kristalader in de grot van de Zoutkristalmijn raakt nooit op. Elke speler heeft er een eigen voorraad in: hooguit 24 "
         "kristallen, en elke halve minuut groeit er één aan. Een houweel hakt er één kristal per keer uit, de Zoutkristalhouweel "
         "twee. Het blok zelf blijft altijd zitten, dus het maakt niet uit wie er voor jou was. De ader geeft pas zout als je voor de "
         "Mijnwerker-guh het spoor hebt vrijgemaakt."),
        ("zoutkristal", "Zoutkristalerts",
         "In de rotsen van de Guhbarbecuether zit zoutkristalerts: houtskoolsteen met kristallen erin. Die erts ontstaat alleen in "
         "stukken wereld waar nog nooit iemand is geweest. Wie in een al verkende Guhbarbecuether woont, haalt zijn zout dus uit de "
         "kristalader."),
        ("zoutkristal", "Wat maak je ervan",
         "Vier zoutkristallen maken een zoutkristalblok (het geeft zacht licht), twee maken twee plukjes zoutkristalletjes. En alle "
         "recepten van de stap Zout van de Guh-technologie hebben zoutkristal nodig."),
        ("archeoloog", "Elke dag een vondst",
         "Vanaf het moment dat je alle vijf de botten hebt, ligt er in elk plekje bottenzand één keer per dag iets kleins voor jou: "
         "een paar kaasknabbels, een botje, een verkoold guhbot, soms een zoutkristal of wat goudklompjes. Het Guhkwastje slijt nooit."),
        ("mijnwerker", "Puin dat terugvalt",
         "Het puin dat je van het spoor hakt, valt na een minuut vanzelf terug, zodat de volgende speler ook iets op te ruimen heeft. "
         "Jouw vijf brokken blijven gewoon geteld."),
    ],
}
