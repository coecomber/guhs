"""
bbq2 (ring-sausuman) - the wiki entries of the Toren van Sausuman (CONTRACT_130 2.4; the docs step of phase 3 turns this into
pages; this module is not in FEATURES and builds nothing).
"""
from . import wereld

WIKI = {
    "verhalen": dict([wereld.wiki_questlijn(
        "ring_sausuman", "De Toren van Sausuman",
        "De extra halte van In de ban van de Knabbelring. In een zwarte toren vol sputterende machines woont Sausuman van de Vele Sauzen, "
        "een tovenaar-Mika die ook een hapje van de ring wil. Hij krijgt het niet, dus bakt hij er zelf een. Het wordt een uienring, en "
        "hij mokt er nog steeds over.",
        "sausuman_toren", ["sausuman"], [("guhs_knabbelring", "ring_sausuman")], related=["verhalen/knabbelring", "systemen/vadskracht"])]),
    "systemen": {},
    "npc_home": {"sausuman": "bouwwerken/sausuman_toren"},
    "entity_home": {},
    "tekst": [
        ("verhalen/ring_sausuman", "Hoe kom je er?",
         "De toren gaat open zodra je hoofdstuk 4 (De Spiegel van Guhladriel) af hebt. Hij staat een paar honderd blokken van de boomstad en "
         "verschijnt dan in je Superkompas (tab Barbecue). Voor de reis naar de Frituurberg hoeft het niet: het is een uitstapje. Je kunt er "
         "ook na het hele verhaal nog heen; Sausuman is dan vooral boos dat de ring al op is."),
        ("verhalen/ring_sausuman", "De questlijn",
         "Praat met Sausuman in de hal. Haal daarna de drie ingrediënten voor zijn Ringenbakker: ringdeeg van de Deegkneder (eerste "
         "verdieping), hete frituursaus uit de Sauskraan (tweede) en kaas uit de Kaaskast (derde). In de Kaaskast ligt alleen nog een ui. "
         "Trek beneden aan de hendel van de Ringenbakker: na het filmpje heb je een uienring en zit Sausuman in zijn Mokhoek. Bied hem een "
         "hapje aan (of geef toe dat je hem zelf hebt opgegeten) en het is klaar."),
        ("verhalen/ring_sausuman", "Wat krijg je?",
         "De Pannantír (een pan waarin je dingen ziet, om thuis neer te zetten), vier uienringen, en de Ringenbakker bakt daarna elke dag "
         "één uienring voor je. Iedereen doet de questlijn voor zichzelf: de voorraden raken nooit op."),
        ("verhalen/ring_sausuman", "Waarom doen zijn machines het niet?",
         "De machines in de toren zijn echte machines van de Guh-technologie, met echt Guhdraad. Sausuman heeft er vijf aan één Mika-rad "
         "gehangen, en dat geeft maar 10 vadskracht. Kijk naar een machine en je leest het zelf: te zwaar, dus alles staat stil. Boven loopt "
         "de draad helemaal nergens heen. Een Guhrad met een guh erin werkt beter."),
        ("verhalen/ring_sausuman", "Sputterpijpen",
         "De pijpen die de toren laten roken kun je zelf maken (gepolijst roosterijzer om een gloeikool). Ze puffen en sputteren vanzelf, "
         "zolang er lucht boven zit. Ze doen niets en niemand pijn."),
    ],
}
