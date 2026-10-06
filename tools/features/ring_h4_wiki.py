"""
bbq2 (ring-h4) - the wiki entries of chapter 4 of the Knabbelring, "De Spiegel van Guhladriel" (CONTRACT_130 2.4; the docs
step of phase 3 turns this into pages; this module is not in FEATURES and builds nothing).
"""
WIKI = {
    "verhalen": {
        "ring_h4": dict(
            nl="De Spiegel van Guhladriel", img="structure_guhladriel_boomstad",
            lead_nl="Hoofdstuk 4 van In de ban van de Knabbelring. Na de mijn rust je uit in Caras Guhladhon, de boomstad van Vrouwe Guhladriel "
                    "in het gouden woud Guhlórien. Je kijkt in haar spiegel, krijgt drie gaven en vaart met de elfenbootjes de Guhduin af, "
                    "tussen de twee reuzenbeelden van de Arguhnath door. Er is in dit hoofdstuk niets dat je iets doet: het is de rust "
                    "voor de Zwarte Roosterpoort.",
            ftb=[("guhs_knabbelring", "ring_h4")], structure="guhladriel_boomstad", npcs=["guhladriel", "leguhlas", "gimguh"],
            related=["verhalen/knabbelring", "systemen/gaven-van-guhladriel"]),
    },
    "systemen": {},
    "npc_home": {"guhladriel": "bouwwerken/guhladriel_boomstad"},
    "entity_home": {"ringh4_elfenbootje": "verhalen/ring_h4"},
    "tekst": [
        ("verhalen/ring_h4", "Stap voor stap",
         "1. Loop naar de boomstad (het Superkompas wijst 'Mijn verhaal'); bij de poort krijg je de vertelkaart van het hoofdstuk. "
         "2. Praat met Leguhlas bij de poort. 3. Klim de wenteltrap rond de Grote Spies op en praat met Guhladriel in haar zaal. "
         "4. Rust uit bij het Rustvuurtje op de gastenvlonder (de boom links van de poort). 5. Rechtsklik op de Spiegel in het groene dal. "
         "6. Praat met Guhladriel bij de spiegel: de drie gaven. 7. Stap bij de steiger in het elfenbootje en blijf zitten tot de overkant."),
        ("verhalen/ring_h4", "De boomstad",
         "Caras Guhladhon ligt in een open plek met witte paden. De Grote Spies in het midden draagt de zaal van Guhladriel; drie andere "
         "bomen dragen vlonders (de gastenvlonder, de voorraadvlonder en de uitkijk) met touwbruggen ertussen. In de hele stad krijg je "
         "geen valschade: Guhladriels zegen."),
        ("verhalen/ring_h4", "De Guhduin en de Arguhnath",
         "Het elfenbootje vaart vanzelf, je kunt niet sturen en niet uitstappen. Er passen twee spelers in; wie binnen drie tellen op het "
         "bootje klikt vaart mee. De rivier is kaassaus en doet niets. Halverwege staan de twee Guhkoningen met hun pootje omhoog. "
         "Na het hoofdstuk mag je zo vaak varen als je wilt, en bij de aanlegplaats ligt een bootje dat terugvaart naar de stad."),
        ("verhalen/ring_h4", "Een gave kwijt?",
         "Praat met Guhladriel (na het hoofdstuk zit ze weer in haar zaal): ze geeft elke gave die je niet meer bij je hebt opnieuw."),
    ],
}
