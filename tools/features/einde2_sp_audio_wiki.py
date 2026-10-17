"""
einde2 "Sponsguh" (sp_audio): the wiki entries of the slice (build contract C-A 2.4; not in FEATURES: the docs step of the merge
reads WIKI). One system page: the music of Bikinibodem and Schelpenstad and its credits, generated from the licence ledger
(no spoilers: the moments are named only as "a big moment of the Sponsguh story").
"""
import json
import os

_LEDGER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "einde2_sp_audio_licenties.json")
_PLEK = {"bikinibodem_1": "Bikinibodem", "bikinibodem_2": "Bikinibodem", "schelpenstad": "Schelpenstad", "loopgraaf": "de loopgraaf"}


def _credits():
    regels = []
    for s in json.load(open(_LEDGER, encoding="utf-8"))["stukken"]:
        soort, naam = s["naam"].split(".", 1)
        waar = ("achtergrondmuziek in " + _PLEK[naam]) if soort == "muziek" else "een groot moment van het Sponsguh-verhaal"
        regels.append(f"\"{s['titel']}\" van {s['maker']} ({waar}). Bron: {s['bron_url']}. Licentie: {s['licentie']} ({s['licentie_url']}).")
    return " ".join(regels)


WIKI = {
    "systemen": {
        "muziek-bikinibodem": (
            "De muziek van Bikinibodem", "item:guhs:knabbelburger",
            "Onder de zeepbel van Bikinibodem, in Schelpenstad en in de loopgraaf hoor je eigen muziek, en onder de grote "
            "momenten van het Sponsguh-verhaal speelt een eigen stuk. Drie liedjes schreven de guhs helemaal zelf.",
            []),
    },
    "entity_home": {},
    "tekst": [
        ("muziek-bikinibodem", "Muziek per plek",
         "Net als in gewoon Minecraft speelt er af en toe een stuk muziek, met stille stukjes ertussen. Onder de koepel van "
         "Bikinibodem hoor je ukelele, steeldrums en marimba; in Schelpenstad een zonnig strandorgeltje; in de loopgraaf iets "
         "griezelig-grappigs met een theremin. De gewone Minecraft-muziek zwijgt daar even. De muziekschuif werkt gewoon."),
        ("muziek-bikinibodem", "Eigen liedjes",
         "Drie liedjes zijn helemaal van de guhs zelf: eigen melodie, eigen Nederlandse tekst. De tekst staat in beeld terwijl "
         "het lied speelt. Welke dat zijn? Dat hoor je vanzelf in het verhaal."),
        ("muziek-bikinibodem", "Spelletjes en grapjes",
         "De deuntjes onder de spelletjes (van de grill tot de band) en de grappige geluidjes, zoals het accordeontje van de "
         "verteller en de valse klarinet, maakten de guhs ook zelf."),
        ("muziek-bikinibodem", "Makers en licenties", _credits()),
        ("muziek-bikinibodem", "In de mod",
         "De lijst staat ook als tekstbestand in de mod: assets/guhs/einde2_sp_audio/credits.txt."),
    ],
}
