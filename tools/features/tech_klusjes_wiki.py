"""
Wiki texts of the chore slice (bbq2, tech-klusjes). Not a feature module (not in FEATURES): the docs step of the merge
reads WIKI and turns it into pages (CONTRACT_130 2.4). No page of its own: the paragraphs belong on the pages of the
Guhhuisje (the chores), the guh machines and the Bank Guh.
"""
from features import tech_klusjes as tk

WIKI = {
    "tekst": [
        ("systemen/guhhuisje", "Klusje: machines bijvullen & leeghalen",
         "Bewoners met dit klusje houden je guh-machines aan de gang. Ze halen op wat er klaarligt (staven uit de Guh Oven, Guhdrankjes "
         "uit de Brouwautomaat, wat de Knutselmachine heeft geknutseld, de oogst van de Oogster...) en brengen dat naar de Bank Guh, het "
         "Hapluikje of de kist, net als bij elk ander klusje. En ze vullen de machines bij uit de kist naast het huisje of de Bank Guh. "
         "Dat bijvullen doen ze nooit zomaar: een Guh Oven lust alles, en niemand wil dat een hele kist eikenhout stiekem houtskool "
         "wordt. Ze brengen alleen wat jij hebt voorgedaan. Leg zelf één keer een stapeltje in de machine: de bewoners onthouden per "
         "vakje wat erin hoort en houden dat vakje daarna gevuld met hetzelfde. Leg je er iets anders in, dan onthouden ze dat. Breek "
         "je de machine af, dan is ze het vergeten. Een Knutselmachine hoef je niets voor te doen: die krijgt precies wat er op zijn "
         "Bouwtekening staat. Bewoners lopen niet heen en weer voor één knabbel: een vakje wordt bijgevuld als het leeg is, hooguit "
         "half vol, of als er minstens acht bij passen. Het klusje werkt bij de Guh Oven, Knutselmachine, Brouwautomaat, "
         "Frituurautomaat, Vadsmolen, Grillkoolpers, Oogster, Knabbelaar, Neerzetter en Opzuiger, en alleen bij machines die de "
         "eigenaar van het huisje zelf heeft neergezet: de staven van de buren blijven van de buren. (Guhs)"),
        ("systemen/guhhuisje", "Klusje: plantage",
         "Staat er een Plantagebak in de klus-area, dan hakt een bewoner met dit klusje de boom om zodra hij er staat: even zwaaien met "
         "zijn bijltje en de hele boom ligt in één keer plat. Het hout, de stokjes en de appels gaan naar de kist; een paar zaailingen "
         "blijven in de bak, die daarmee zelf de volgende boom plant. Is een bak helemaal leeg, dan haalt de bewoner zaailingen uit de "
         "kist (ook sate- en worstzwammetjes, paddenstoelen en azalea). Bomen die ergens anders groeien laat hij altijd staan: hakken "
         "doet hij alleen in Plantagebakken. De bak zelf heeft vadskracht nodig om te groeien, het hakken niet. (Guhs)"),
        ("systemen/guhhuisje", "Klusje farmen: nog meer oogsten",
         "Het klusje Farmen oogst meer dan vroeger. Naast alle gewassen en guhtuintjes plukken bewoners nu ook pompoenen en meloenen "
         "die aan hun stengel hangen (de stengel blijft staan; een pompoen die jij ergens hebt neergezet is geen oogst en blijft "
         "liggen), suikerriet (het onderste stuk blijft staan), rijpe cacao waar ze bij kunnen (hooguit twee blokken boven de grond), "
         "netherwrat en pepers. Van elk pinda- en mosterdscheutjesplantje plukken ze één scheutje; het plantje blijft staan en rust "
         "daarna vijf minuten uit, net als een bloem."),
        ("systemen/guhhuisje", "Waar gaan de spulletjes heen?",
         "Wat je bewoners met hun klusjes verzamelen, brengen ze naar een Bank Guh in de klus-area (die sorteert, tot 256 van een "
         "soort). Staat er geen Bank Guh in de klus-area, of zit die vol van iets, dan stoppen ze het in een Hapluikje dat in de "
         "klus-area staat: dat hapt alles door naar de Bank Guh waar het met de Banksleutel aan gekoppeld is, hoe ver weg die ook "
         "staat. Zo kan je bank gewoon thuis blijven terwijl je bewoners ver weg op een akker werken. Het luikje moet wel werken: "
         "vadskracht, gekoppeld, en zijn bank moet ergens staan. Is ook die bank vol, dan gaat de rest in de kist naast het huisje, "
         "en zonder kist komt het voor de deur te liggen. Er gaat nooit iets verloren. Het huisje-scherm zegt waar de spulletjes nu "
         "heen gaan, en 'Wat kan hier?' telt de werkende Hapluikjes. Geleende spulletjes gaan nooit een bank in. Een guh-machine "
         "telt nooit als kist, ook niet als hij pal naast het huisje staat."),
        ("systemen/guhmachines", "Bewoners van een Guhhuisje helpen mee",
         "Zet je machines in de klus-area van een Guhhuisje en geef een bewoner het klusje Machines bijvullen & leeghalen: dan heb je "
         "nog geen Knabbelbuizen nodig. Leg zelf één keer in de machine wat erin moet en zet de voorraad in de kist naast het huisje. "
         "Voor een Plantagebak is er het klusje Plantage: de bewoner hakt de boom en zorgt voor zaailingen."),
        ("bank-guh-buikje", "Het Hapluikje in de klus-area van een Guhhuisje",
         "Een werkend Hapluikje in de klus-area van een Guhhuisje is een plek waar de bewoners de spulletjes van hun klusjes heen "
         "brengen, als er geen Bank Guh in de klus-area staat of als die vol zit."),
    ],
}

# (so the docs step can list what the chore serves without reading the Java)
MACHINES = tk.MACHINES
