"""
Wiki texts of the vadskracht (bbq2, foundation F1). Not a feature module (not in FEATURES): the docs step of the merge reads
WIKI and turns it into pages (CONTRACT_130 2.4). The numbers come from VadsGetallen.java (vadskracht.getal).
"""
from features import vadskracht as v

WIKI = {
    "systemen": {
        "vadskracht": ("Vadskracht", "guh_wheel",
                       "Vadskracht is de kracht waar alle guhmachines op lopen. Guhs rennen hem bij elkaar in een Guhrad, Guhdraad brengt "
                       "hem naar je machines, en als je meer vraagt dan je guhs geven staat alles even stil.",
                       ["systemen/guhhuisje"]),
    },
    "tekst": [
        ("systemen/vadskracht", "Hoe het werkt",
         "Alles wat met Guhdraad aan elkaar zit is samen één opstelling. Bronnen geven vadskracht, machines gebruiken het. Een Guhrad "
         f"met een guh erin geeft {v.getal('GUHRAD')} vadskracht, een blij guhtje {v.getal('GUHRAD_BLIJ')}; een Guhoven gebruikt er "
         f"{v.getal('GUH_OVEN')}. Een machine mag ook direct naast een bron staan, dan heb je geen draad nodig."),
        ("systemen/vadskracht", "Te zwaar? Dan staat alles stil",
         "Vragen je machines samen meer vadskracht dan je bronnen geven, dan staat de HELE opstelling stil. Niks gaat langzamer en niks "
         "krijgt voorrang: het is alles of niks. Zet er een bron bij, of haal een machine weg. Een machine telt mee zodra hij aan "
         "staat, ook als hij even niks te doen heeft, dus je opstelling gaat niet knipperen."),
        ("systemen/vadskracht", "Kijken is weten",
         "Een meter heb je niet nodig. Kijk naar een Guhrad, een stuk Guhdraad of een machine en je leest het meteen: 'Deze opstelling "
         "gebruikt 16/20 vadskracht'. Staat alles stil, dan lees je ook waarom. Kijk je naar een bron, dan zie je wat die geeft; bij "
         "een machine wat die gebruikt."),
        ("systemen/vadskracht", "Niet te veel van hetzelfde",
         "Per opstelling tellen hooguit "
         + ", ".join(f"{n} {v.TEXTS[v.K + 'soort.' + soort + ('.enkel' if n == 1 else '')]}" for soort, n in v.bron_max().items())
         + ". Zet je er meer neer, dan doen de extra's niks ('telt niet mee'). De sterkste tellen het eerst."),
        ("systemen/vadskracht", "De snoet van een machine",
         "Elke guhmachine heeft een snoet. Slaapt hij, dan heeft hij geen vadskracht. Kijkt hij blij, dan heeft hij vadskracht. Kijkt "
         "hij verbaasd met een rond mondje, dan zit hij vol: haal eruit wat hij gemaakt heeft."),
        ("systemen/vadskracht", "Guhdraad en redstone",
         "Guhdraad geeft nog steeds een redstonesignaal zolang de opstelling draait, dus een lamp aan het eind van je draad brandt "
         "gewoon. Andersom werkt het niet meer: een hendel of redstoneblok geeft geen vadskracht. Daar heb je echt een guh voor nodig, njeg!"),
    ],
}
