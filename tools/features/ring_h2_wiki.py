"""
bbq2 (ring-h2) - the wiki entries of chapter 2 of the Knabbelring, "De Raad van Guhrond" (CONTRACT_130 2.4; the docs step of
phase 3 turns this into pages; this module is not in FEATURES and builds nothing).
"""
WIKI = {
    "verhalen": {
        "ring_h2": dict(
            nl="De Raad van Guhrond", img="structure_guhvendel",
            lead_nl="Hoofdstuk 2 van In de ban van de Knabbelring. In Guhvendel, het elfenhuis van Guhrond onder drie watervallen van "
                    "kaassaus, komt een raad bijeen over de ring. Iedereen wil hem opeten, Gimguh probeert het zelfs. Jij meldt je aan "
                    "om hem naar de Frituurberg te brengen en vertrekt met acht metgezellen: het Reisgenootschap van de Knabbelring.",
            ftb=[("guhs_knabbelring", "ring_h2")], structure="guhvendel",
            npcs=["guhrond", "guhdalf", "araguh", "leguhlas", "gimguh", "boromika", "merrie", "pippguh"],
            related=["verhalen/knabbelring", "systemen/rustpunten"]),
    },
    "systemen": {},
    "npc_home": {"guhrond": "bouwwerken/guhvendel"},
    "entity_home": {},
    "tekst": [
        ("verhalen/ring_h2", "Hoe kom je er?",
         "Het hoofdstuk begint zodra je na hoofdstuk 1 door het grillportaal in de Guhbarbecuether staat: je krijgt eerst de vertelkaart met "
         "het kaartje te zien. Guhvendel staat precies één keer in elke wereld, in het Worstenwoud, een paar honderd blokken van het midden "
         "van de Guhbarbecuether. Het Superkompas (Mijn verhaal) en Sam-guh wijzen de weg. Tot je hoofdstuk 1 af hebt zie je alleen een "
         "muur van rook: Guhdalfs sluier."),
        ("verhalen/ring_h2", "De stappen",
         "1. Loop Guhvendel binnen door de poort in het zuiden. 2. Praat met Guhrond op de stoep van de hal. 3. Maak kennis met Araguh (bij "
         "de poort: buk en klik hem nog een keer aan, hij wil zien of je kunt sluipen), Leguhlas en Gimguh (in de tuin, bij hun stapel "
         "knabbels), Boromika (in de hal, bij de scherven van Knabsil) en Merrie en Pippguh (in de keuken; ze snoepen één kaasknabbel uit je "
         "tas als je die hebt). 4. Luid de bel in de raadskring: de raad begint. 5. Zeg tegen Guhrond dat jij de ring wel meeneemt. "
         "6. Praat met Guhdalf als je klaar bent voor vertrek."),
        ("verhalen/ring_h2", "Wat krijg je?",
         "Proviand van Guhrond (zes kaasknabbels en twee stoofpotjes, één keer per speler) en de advancements De Raad van Guhrond en Het "
         "Reisgenootschap. Bij het huis staat een Rustvuurtje: loop erlangs en Guhvendel is je rustpunt."),
        ("verhalen/ring_h2", "Samen spelen",
         "Alles is per speler. De bel start de raad alleen voor wie aan die stap toe is; een vriend die verder of minder ver is mag gewoon "
         "meelopen. Het gezelschap zie je alleen op de momenten van je eigen verhaal: eerst overal in het huis, na de bel in de raadskring, "
         "en na het hoofdstuk woont alleen Guhrond er nog. Niemand in Guhvendel doet je iets en er valt niets stuk te maken."),
        ("bouwwerken/guhvendel", "Het Laatste Knusse Huis",
         "Een hoefijzer van rots met drie watervallen van kaassaus. Op de westoever de hal: wit, met spitse ramen, een steil dak van groen "
         "koper en een ronde toren. Op de oostoever de raadskring: een rond terras in een krans van zuilen, met de stenen tafel waar de ring "
         "op ligt en de hoge zetel van Guhrond. Ertussen de smalle boogbrug zonder leuning. In het zuidoosten de keuken met het buffet en "
         "het Rustvuurtje, in het zuidwesten de tuin met guhbloesembomen en de knabbelstapel van Leguhlas en Gimguh."),
    ],
}
