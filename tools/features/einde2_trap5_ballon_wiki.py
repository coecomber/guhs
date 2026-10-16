"""
einde2 (trap5_ballon): what the wiki must say about the Vredesballon, the big project of tech tier 5. Not a feature module
(not in FEATURES): the docs step reads WIKI and turns it into pages (CONTRACT_E2 2.4). The item page of guhs:vredesballon
(the statuette) comes from the game data by itself; the personal heteluchtballon has its page in einde2_vetbootje_wiki.py.
No spoilers of the launch scene beyond its first moment.
"""
from features import wereld

WIKI = {
    "verhalen": dict([wereld.wiki_questlijn(
        "vredesballon", "De Vredesballon",
        "Na zijn eigen projecten heeft de Werktuigguh nog één groot plan: een reusachtige ballon die alleen maar knabbels laat "
        "vallen. Vroeger tekende hij ballonnen voor de Vuurnatie, en die werden oorlogsballonnen. Deze keer bouw jij er samen met "
        "hem een voor de vrede, met wat jouw fabriek maakt. Iedereen bouwt zijn eigen ballon.",
        "zuidelijke_luchttempel", ["werktuigguh"], [("guhs_techniek", "einde2_trap5_ballon")])]),
    "systemen": {
        "vredesballon": (
            "De Vredesballon", "item:guhs:vredesballon",
            "Het grote project van Guh-technologie trap 5, op de bouwplaats achter de werkplaats van de Werktuigguh in de Zuidelijke "
            "Luchttempel. Vijf fasen fabrieksoutput, een ballon die je ziet groeien, en een lancering boven het Guheinde.",
            ["vredesballon", "heteluchtballon", "zuidelijke_luchttempel"]),
    },
    "entity_home": {},
    "tekst": [
        ("vredesballon", "Beginnen",
         "De bouwplaats gaat open als de werkplaats van de Werktuigguh open is: na het hele Guhvatar-verhaal en zijn eigen projecten. "
         "Klik op het bord aan de muur van de bouwplaats. Het bord laat iedereen zijn eigen voortgang zien."),
        ("vredesballon", "De vijf fasen",
         "1. Het doek: 128 tentdoek (elke kleur) en 64 Guhdraad. 2. De mand: 256 boomstammen en 16 doosschillen. 3. De brander: "
         "2 Vuurkorven, 1 Vadsontvanger, 96 grillkool en 16 kristalkaas (de brander draait op vadskracht). 4. De knabbelvoorraad: 512 "
         "kaasknabbels en 128 gefrituurde kaasknabbels. 5. De versiering: 64 lichtstaafjes en 32 flitskruimels. Je brengt alles in "
         "porties naar het bord: het telt wat je bij je hebt en neemt nooit meer dan nodig."),
        ("vredesballon", "Jouw ballon groeit",
         "Na elke fase staat er meer van jouw ballon op de bouwplaats: eerst het doek aan de kraan, dan de mand, de brander met zijn "
         "snoet, de zakken knabbels en tot slot de oortjes en vlaggetjes. Alleen jij ziet jouw ballon; een vriend ziet de zijne."),
        ("vredesballon", "Opstijgen",
         "Als hij af is, vraagt de Werktuigguh of je mee de lucht in durft. Daarna krijg je het Vredesballon-beeldje, je eigen kleine "
         "heteluchtballon en de titel Vredesbrenger. De lancering kun je opnieuw bekijken in de Guhdex."),
        ("vredesballon", "Elke dag",
         "De Vredesballon maakt elke dag een rondje boven het Guheinde. Klik daarna op het bord: er zit een zakje knabbels in de mand. "
         "Je kleine ballon kwijt? Sluip en klik op het bord, dan heeft de Werktuigguh nog een reserve."),
    ],
}
