"""
einde2 (audio): the wiki entries of the slice (build contract C-A 2.4; not in FEATURES: the docs step of the merge reads WIKI).
One system page: the music of the Guheinde and its credits, generated from the licence ledger (no spoilers: the moments
are named only as "a big moment of the Guhvatar story").
"""
import json
import os

_LEDGER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "einde2_audio_licenties.json")
_GEBIED = {"lucht": "de Luchtnomaden", "water": "de Waterstam", "aarde": "het Aarderijk", "vuur": "de Vuurnatie", "geest": "de Geestenwereld"}


def _credits():
    regels = []
    for s in json.load(open(_LEDGER, encoding="utf-8"))["stukken"]:
        soort, naam = s["naam"].split(".", 1)
        waar = ("achtergrondmuziek bij " + _GEBIED[naam.rsplit("_", 1)[0]]) if soort == "muziek" else "een groot moment van het Guhvatar-verhaal"
        regels.append(f"\"{s['titel']}\" van {s['maker']} ({waar}). Bron: {s['bron_url']}. Licentie: {s['licentie']} ({s['licentie_url']}).")
    return " ".join(regels)


WIKI = {
    "systemen": {
        "muziek-guheinde": (
            "De muziek van het Guheinde", "item:minecraft:written_book",
            "Bij de vier volken van het Guhvatar-verhaal en in de Geestenwereld hoor je eigen muziek, en onder de grote momenten "
            "van het verhaal speelt een episch stuk. De muziek is van echte makers die hun werk vrij hebben gegeven.",
            []),
    },
    "entity_home": {},
    "tekst": [
        ("muziek-guheinde", "Muziek per plek",
         "Net als in gewoon Minecraft speelt er af en toe een stuk muziek, met stille stukjes ertussen. Ben je bij de "
         "Luchtnomaden, de Waterstam, in het Aarderijk, in de Vuurnatie of in de Geestenwereld, dan hoor je de muziek van die "
         "plek: fluiten en windklokjes, tokkelende snaren, grote trommels, strijkers en klankschalen. Ook de zijplaatsen met een "
         "element hebben hun muziek. Daar zwijgt de gewone Minecraft-muziek even. De muziekschuif in de instellingen werkt gewoon."),
        ("muziek-guheinde", "Epische momenten",
         "Onder de grote filmpjes van het verhaal speelt een eigen stuk muziek. Het begint zacht, blijft spelen zolang jij "
         "het filmpje leest en klikt, en sterft weg als het filmpje klaar is. Oom Miroh zingt een eigen wiegenlied: de woorden "
         "staan in beeld."),
        ("muziek-guheinde", "Zelf gemaakt",
         "De deuntjes van het theehuis en de spelletjes, de grappige geluidjes en de geluiden van de Geestenwereld maakten de "
         "guhs zelf, net als Miroh's wiegenlied. Het verjaardagsliedje is een oud liedje van iedereen."),
        ("muziek-guheinde", "Makers en licenties", _credits()),
        ("muziek-guheinde", "Het Muziekboekje",
         "Na het Guhvatar-verhaal krijg je het Muziekboekje van het Guheinde: daarin staat bij elk stuk wie het maakte en "
         "onder welke licentie. De lijst staat ook als tekstbestand in de mod (assets/guhs/einde2_audio/credits.txt)."),
    ],
}
