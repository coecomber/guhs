"""
Wiki texts of tech tier 5 (einde2, slice trap5_machines). Not a feature module (not in FEATURES): the docs step of the merge
reads WIKI and turns it into pages (C-A 2.4). The numbers come from T5Getallen.java. The blocks themselves get their own
wiki page from the game data; this adds the system page of tier 5 and the Werktuigguh's questline.
"""
from features import einde2_trap5_machines_tekst as t

G = t.G

WIKI = {
    "systemen": {
        "trap5": ("Guh-technologie trap 5", "vadszender",
                  "Na de finale van het Guhvatar-verhaal gaat de werkplaats van de Werktuigguh open, in de Zuidelijke Luchttempel. "
                  "Hij leert je trap 5 in projecten: kracht door de lucht, je bank overal, de Bestelguh, post door de poort, je eigen "
                  "Thuispoorten, de vier Elementmachines en twee nieuwe bronnen. Trap 5 is niet het einde: de Werktuigguh heeft altijd "
                  "nog plannen.",
                  ["systemen/vadskracht", "systemen/vadskrachtbronnen"]),
    },
    "entity_home": {"bestelguh": "systemen/trap5"},
    "tekst": [
        ("systemen/trap5", "De werkplaats van de Werktuigguh",
         "De deur van de werkplaats in de Zuidelijke Luchttempel blijft dicht tot je het hele Guhvatar-verhaal hebt uitgespeeld. Daarna "
         "loop je er gewoon doorheen; voor wie nog bezig is blijft hij een muur. De eerste keer komt de Werktuigguh je tegemoet. Hij "
         "geeft je zijn Bouwplan: dat hoort in elk recept van trap 5 en blijft in het werkbankrooster liggen. Kwijt? Sluip-klik de "
         "Werktuigguh voor een nieuw, voor een paar kaasknabbels. In zijn werkplaats staan zes oefenopstellingen: van elk project één "
         "dat al werkt. Klik ze aan, de Werktuigguh legt uit."),
        ("systemen/trap5", "Projecten",
         "Elk project is af zodra JOUW machine in je eigen wereld zijn ding doet. Je hoeft er niet voor terug naar de werkplaats, en wat "
         "je alvast bouwt telt gewoon mee. Van de eerste vijf projecten geeft de Werktuigguh je het eerste exemplaar; de rest knutsel "
         "je zelf, met kristalkaas, doosschil, flitskruimel en knabbelkoor uit het Guheinde. Na alle projecten maakt hij voor het eerst in "
         "honderd jaar iets af: een diploma, voor jou."),
        ("systemen/trap5", "Vadszender en Vadsontvanger",
         f"Een Vadszender zendt de vadskracht van zijn net naar Vadsontvangers binnen {G('ZENDER_BEREIK')} blokken (hooguit "
         f"{G('ZENDER_MAX')} per zender). Een ontvanger geeft zijn eigen net precies wat het tekortkomt, en de zender vraagt dat allemaal "
         f"(plus zijn eigen {G('ZENDER')}) van zijn net. Zo blijven ze één net met dezelfde regel: is er ergens te weinig, dan staat "
         "alles stil. Wat een ontvanger binnenkrijgt, zendt een zender in hetzelfde net niet nog eens door: er is geen kanaal over "
         "elke afstand."),
        ("systemen/trap5", "De Guhterminal",
         f"Een blok, geen tablet. Klik je Bank Guh aan met de Banksleutel en daarna de terminal: dan opent de terminal het scherm van "
         f"die bank, waar je ook bent, ook in een andere dimensie. Zoeken, sorteren, pakken en storten werken als bij de bank zelf, met "
         f"dezelfde grens van 256 per soort (tot de bank het Bodemloos Knabbelmaagje heeft). De terminal vraagt {G('TERMINAL')} "
         "vadskracht."),
        ("systemen/trap5", "De Bestelguh",
         f"Zet een Bestelbalie naast je Knutselmachines (binnen {G('BALIE_BEREIK')} blokken) en koppel hem met de Banksleutel aan je "
         "bank. De Bestelguh met zijn schortje verschijnt vanzelf. Op de balie, op de Bestelguh of met de knop Bestellen in het "
         "bankscherm van je Guhterminal zie je alles wat de machines kunnen maken. Kies een aantal: hij haalt de ingrediënten uit je "
         "bank, maakt eerst de tussenproducten (als daar ook een machine voor staat), stopt alles in de bank en laat je een bonnetje "
         "zien. Sluip-klik de balie om een bestelling te annuleren."),
        ("systemen/trap5", "De Knabbelpoort-buis",
         f"Sluip-klik twee buizen na elkaar en ze zijn maatjes. Wat een Knabbelbuis, een trechter of een klusjesguh in de ene stopt, "
         f"komt bij de andere aan de voorkant (de snoet) weer naar buiten, ook als die in een andere dimensie staat. Beide kanten "
         f"moeten geladen zijn en vadskracht hebben ({G('POORTBUIS')} per buis)."),
        ("systemen/trap5", "De Thuispoort",
         f"Je eigen netwerk van poorten, zo groot als je wilt. Klik een poort en kies waar je heen springt. Een sprong kost "
         f"{G('THUIS_BASIS')} vadskracht plus {G('THUIS_PER_POORT')} voor elke andere poort die je hebt; dat komt uit de batterij van "
         f"de poort zelf ({G('THUIS_BATTERIJ')} vadskracht), die zich oplaadt met wat het net overhoudt. Je guhs aan de lijn (binnen "
         f"{G('THUIS_LIJN')} blokken) en je rijdier springen mee. Alleen jij gebruikt je poorten, en wie je toelaat. Houd een item vast, open "
         "een poort en klik op Icoon: dat wordt zijn icoontje."),
        ("systemen/trap5", "De vier Elementmachines",
         f"De Luchtblazer blaast spullen, guhs en spelers weg (tot {G('BLAZER_BEREIK')} blokken); zet je hem neer terwijl je naar "
         "beneden kijkt, dan blaast hij omhoog: een liftje, en niemand valt hard. De Waterdrager bevloeit de akker om zich heen, vult "
         "ketels naast zich en lege emmers. De Korstduwer schuift bij een redstonepuls of een klik een rij van hooguit "
         f"{G('DUWER_MAX')} blokken op (nooit kisten, machines of iets in een beschermd gebouw). De Vuurkorf bakt wat er rond hem op "
         "de grond ligt en smelt sneeuw en ijs."),
        ("systemen/trap5", "Het Enderguhrad en de Komeetkern",
         f"In een Enderguhrad vliegt een Vahoege Enderguh zijn rondjes: {G('ENDERGUHRAD')} vadskracht, één per opstelling. De "
         "Komeetkern is de scherf van de knabbelkomeet uit de finale. Hij ligt bij het duelplein van de Vuurnatie-hoofdstad, alleen "
         f"zichtbaar voor wie het verhaal heeft uitgespeeld en hem nog niet heeft. Stap erop. {G('KOMEETKERN')} vadskracht, de "
         "sterkste bron tot nu toe; één per speler, en wie hem breekt krijgt hem terug."),
    ],
}
