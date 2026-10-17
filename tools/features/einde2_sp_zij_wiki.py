"""
einde2 "Sponsguh" (sp_zij): what the wiki must say about the side lines of Bikinibodem (Sandy-guh's tree dome, De Vliegende
Guhlander, Zeemeerman and Mosselguh) and Octo's house. Not a feature module (not in FEATURES): the docs step reads WIKI and
turns it into pages (CONTRACT_E2 2.4). The pages of the rewards (luchthelm, heldencape, spooklantaarn) and of the place come
from the game data by themselves.
"""

WIKI = {
    "systemen": {
        "bikinibodem_zijdingen": (
            "Bikinibodem: zijdingen", "item:guhs:sponsguh_luchthelm",
            "Zodra je bij De Krokante Knabbel werkt, kunnen drie bewoners van Bikinibodem je hulp gebruiken: Sandy-guh in haar "
            "boomkoepel, de spookkapitein van De Vliegende Guhlander en de bejaarde helden Zeemeerman en Mosselguh. Elk kort "
            "verhaal staat in de Guhdex (Verhalen, onder Sponsguh Kaasbroek) en in het FTB-boek.",
            ["bikinibodem", "sponsguh_luchthelm", "sponsguh_heldencape", "spooklantaarn"]),
    },
    "entity_home": {},
    "tekst": [
        ("bikinibodem_zijdingen", "Sandy-guh's boomkoepel",
         "Door de luchtsluis aan de westkant van haar glazen koepel krijg je een luchthelm op (een grapje: guhs ademen overal). "
         "Op de karatemat duwt Sandy-guh: roept ze HAI, stap dan opzij of spring; tien keer ontwijken wint, en een duw die raak "
         "is kost niets. Daarna zoek je drie raketonderdelen in de stad, en haar raket gaat omhoog. Een klein stukje. Beloning: "
         "Sandy's luchthelm voor je guh."),
        ("bikinibodem_zijdingen", "De Vliegende Guhlander",
         "Wacht tien tellen op de aanlegplaats aan de Schelpstraat: dan drijft het spookschip uit de mist. Een bubbelstroom brengt "
         "je naar het dek, waar de spookkapitein bij kaarslicht De Nacht van de Hakkende Hakker vertelt. Breng hem daarna zijn "
         "drie weggewaaide spooklantaarns terug. Beloning: een spooklantaarn en het recept. Vallen van het schip doet nooit pijn."),
        ("bikinibodem_zijdingen", "Zeemeerman en Mosselguh",
         "De twee oude helden in het rusthuis zijn de riem van Zeemeerman kwijt: hij ligt in de onzichtbare bootmobiel op de "
         "parkeerplaats. Met de riem om bedenkt hij een heldendaad: de lunch halen bij De Krokante Knabbel, in de onzichtbare "
         "bootmobiel (je ziet alleen een randje van witte stipjes om je heen). Beloning: een echte heldencape."),
        ("bikinibodem_zijdingen", "Octo's huis",
         "Octo Tentakel werkt overdag achter de kassa, maar zijn moai-huis aan de Schelpstraat staat open: tweeëntwintig "
         "zelfportretten (klik er een aan) en zijn klarinet op een standaard. Wie die aanraakt, hoort Octo van boven roepen."),
    ],
}
