"""
1.6.0 (F13): what the wiki says about De Eeuwige Knabbelvelden and the Knabbelportaal. Not a feature module (not in
FEATURES): the docs step (tools/wiki_bbq2) reads WIKI; the English is in tools/wiki_bbq2/en_safari.py.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "knabbelvelden": (
            "De Eeuwige Knabbelvelden", "block:guhs:knabbelportaal",
            "Een roze kaasvlakte waar je guhs met pensioen gaan. Met het Knabbelportaal stuur je ze erheen en haal je ze weer op: "
            "niet dood hoor, gewoon met pensioen, njeg!",
            ["knabbelportaal", "dimensies/knabbelvelden", "guhdex", "temmen"]),
    },
    "npc_home": {},
    "entity_home": {},
    "tekst": [
        ("knabbelvelden", "Het Knabbelportaal",
         "Maak een Knabbelportaal van 4 blokken kaasknabbels, 4 kaasknabbels en een roze kleurstof en zet het neer: een staand "
         "lijstje van 2 bij 2 met een wolkje van roze kaas. Je loopt er niet doorheen: rechtsklik opent het scherm."),
        ("knabbelvelden", "Het scherm",
         "Links staan je guhs uit de Guhdex, rechts de guhs die met pensioen zijn. Zoek op naam, sorteer op nummer, naam, vacht of "
         "datum, en kies er een of meer. Met de pijltjes stuur je ze naar de Knabbelvelden of haal je ze terug. Klik op een guh om "
         "alles over hem te zien: zijn aard, kleding en hoe lang hij al van jou is."),
        ("knabbelvelden", "Wie mag er met pensioen?",
         "Elke tamme guh van jou, ook een die ver weg is: die wordt eerst opgehaald en gaat dan met een poef. Is een guh ergens mee "
         "bezig (hij woont in een Guhhuisje, rent in een Guhrad, logeert in de Guhkamer of rijdt ergens op), dan vraagt het scherm "
         "eerst of het echt mag. Een opgepakte guh, een guh op vakantie of op kantoor, en een guh in de wolkjes kan niet."),
        ("knabbelvelden", "Weer naar huis",
         "Een guh die je terughaalt komt als opgepakte guh in je zakken, met alles erop en eraan. Zitten je zakken vol, dan wacht hij "
         "bij het portaal op je."),
        ("knabbelvelden", "Op bezoek",
         "Elke speler heeft zijn eigen stukje Knabbelveld. Ga erheen via het scherm: je gepensioneerde guhs knabbelen, dutten en "
         "spelen om je heen, doorzichtig en met een aureooltje. Klik op een guh om hem te aaien; sluip + klik vraagt of hij mee naar "
         "huis wil. Er is altijd dag, niemand raakt gewond, en bouwen of slopen kan er niet."),
    ],
}
