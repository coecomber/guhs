"""
bbq2 (bestaand): the wiki entries of the slice (CONTRACT_130 2.4; not in FEATURES: the docs step reads WIKI). The items, blocks
and NPCs get their own wiki pages from the game data; this adds the two questlines, where the NPCs live and a few paragraphs
for the pages of the two buildings.
"""
from features import wereld

WIKI = {
    "verhalen": dict([
        wereld.wiki_questlijn(
            "wachter", "De wacht bij de Spiesburcht",
            "In de hal van elke Spiesburcht staat de Wachter-guh in zijn wachthokje, naast het beeld. De Vonk-Mika's hebben de vier "
            "brugvuren uitgeblazen en zijn pindasaus-tuintje staat vol Mikakruid. Steek met zijn aansteekspies de vuurkorf op elke brug "
            "aan en wied de zes pollen Mikakruid, dan krijg je het recept van het Zielig lantaarntje, twee lantaarntjes en het wachterspak.",
            "spiesburcht", ["wachterguh"], [("guhs_barbecuether", "bestaand_wachter")], related=["knuffelmaker"]),
        wereld.wiki_questlijn(
            "knuffelmaker", "De gestolen knuffels",
            "Op de eerste verdieping van elk Mika-grillpaleis zit de gevangen Knuffelmaker-guh in zijn naaihoek. De Mika's laten hem "
            "sokken stoppen en hebben zijn knuffelguhs in drie kooien gezet. Bevrijd ze met een vahoege vads per kooi, of sluip en "
            "peuter het slot los als geen Nether-Mika kijkt. Breng daarna vier draad: je krijgt het knuffelpatroon en drie knuffels.",
            "mika_grillpaleis", ["knuffelmakerguh"], [("guhs_barbecuether", "bestaand_knuffelmaker")], related=["wachter"]),
    ]),
    "systemen": {},
    "npc_home": {"wachterguh": "bouwwerken/spiesburcht", "knuffelmakerguh": "bouwwerken/mika_grillpaleis"},
    "entity_home": {},
    "tekst": [
        ("bouwwerken/spiesburcht", "De Wachter-guh",
         "Naast het beeld in de hal staat een wachthokje met de Wachter-guh. Hij en zijn hokje verschijnen ook in Spiesburchten die al "
         "bestonden voordat hij er was: zodra je in de buurt komt, staat hij er. Op elke brug staat een brugvuurkorf op een sokkel. "
         "Een vuur dat jij aansteekt, brandt alleen voor jou: een andere speler ziet het nog koud en kan het zelf aansteken. Zo kan "
         "iedereen op de server de wacht bij de Spiesburcht doen, zo vaak er spelers zijn."),
        ("bouwwerken/spiesburcht", "Mikakruid in het tuintje",
         "Mikakruid is paars, prikkerig onkruid dat de Mika's in het pindasaus-tuintje zaaien. Het staat er alleen voor wie het voor de "
         "Wachter-guh moet wieden: klik op een pol om hem eruit te trekken. Er zijn er zes."),
        ("bouwwerken/mika_grillpaleis", "De Knuffelmaker-guh",
         "Naast de drie kooien met gestolen knuffelguhs op de eerste verdieping staat de naaihoek van de Knuffelmaker-guh, met een "
         "weefgetouw en een bordje van de Mika's. Ook die verschijnt in paleizen die al bestonden. Een knuffel die jij bevrijdt, is "
         "alleen voor jou uit zijn kooi: je ziet hem daarna bij de Knuffelmaker-guh zitten. Betrapt worden bij het sluipen kost niets: "
         "een Nether-Mika geeft je alleen een duw. Een Sluipknabbeldrankje maakt je onzichtbaar voor hun blik, en een Mika die aan een "
         "vadsstaaf snuffelt let even niet op."),
        ("bouwwerken/mika_grillpaleis", "De lege troon",
         "De troon van de Grote Nether-Mika blijft leeg. Volgens de Knuffelmaker-guh woont hij in zijn eigen kasteel, ergens in de "
         "sauszee."),
    ],
}
