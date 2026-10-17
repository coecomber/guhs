"""
einde2 phase B (reizen): what the wiki must say about travelling in the Guheinde (build contract 2.4; not in FEATURES: the
docs step of the merge reads WIKI). The item page of guhs:einde2_reizen_walvishoorn comes from the game data by itself.
Zuka's ship (guhs:einde2_reizen_zuka_schip) is a story prop, not a creature: it gets no creature page.
"""

WIKI = {
    "verhalen": {
        "leegtewalvis": dict(           # (a story page takes the dict shape of wiki_bbq2's "verhalen", not the systemen tuple)
            nl="De grote zingende", img="entity:guhs:leegtewalvisguh",
            lead_nl="De questline van de oude kapitein-guh in havenkroeg De Vadsige Walvis (Leegtehaven): geloof hem, wacht in de leegte tot "
            "de walvis opdoemt, breng bewijs naar de kroeg en tem een Leegtewalvisguh voor jezelf.",
            related=["leegtewalvisguh", "leegteparel", "einde2_reizen_walvishoorn", "leegtehaven"]),
    },
    "systemen": {
        "reizen-met-appa": (
            "Reizen met Appa", "entity:guhs:appa_guh",
            "Appa vliegt je tussen de hoofdstukken van het Guhvatar-verhaal (met je team op zijn rug en soms Zuka's schip achter je "
            "aan) en, met het Appa-fluitje, naar elk gebouw dat je in het Guheinde kent. Vrienden reizen mee als gast.",
            ["appa_guh", "appa_fluitje", "momo_guh"]),
        "eigen-walvis": (
            "Je eigen walvis", "entity:guhs:leegtewalvisguh",
            "Een getemde Leegtewalvisguh: roep hem met de hoorn van de kapitein, stuur hem vanaf zijn rug en richt die rug in met een "
            "huisje, kisten, een bed, een tuintje, lantaarns en een vlag.",
            ["leegtewalvisguh", "einde2_reizen_walvishoorn", "knabbelkoorvrucht"]),
    },
    "npc_home": {"kapiteinguh": "bouwwerken/leegtehaven", "kroegbaasguh": "bouwwerken/leegtehaven"},
    "entity_home": {},
    "tekst": [
        ("reizen-met-appa", "De verhaalvluchten",
         "Na elk hoofdstuk van het Guhvatar-verhaal brengt Appa je naar het volgende gebouw. Het is een echte vlucht van een paar "
         "minuten: hij wijst de weg en je mag zelf sturen; laat je los, dan vliegt hij zelf. Je team zit mee op zijn rug en kletst "
         "onderweg in de chat. Tussen het eerste en tweede en het tweede en derde hoofdstuk duikt Zuka's schip op. Hij schiet "
         "vuurballen die alleen duwen: druk op een looptoets (links, rechts, springen of sluipen) en Appa duikt opzij."),
        ("reizen-met-appa", "Het fluitlijstje",
         "Zit je op Appa's kop in het Guheinde, blaas dan op het Appa-fluitje. Je krijgt een lijstje van elk verhaalgebouw dat je "
         "hebt vrijgespeeld en elk Guheinde-gebouw dat je hebt ontdekt, met de afstand. Kies er een en Appa vliegt erheen. Een "
         "verhaalvlucht die je onderweg hebt onderbroken staat bovenaan, met een ster."),
        ("reizen-met-appa", "Samen reizen",
         "In Appa's zadelmand passen drie vrienden. Wie meevliegt is gast: zolang je in de buurt van je gastheer blijft (64 blokken) "
         "en hij in het gebouw blijft waar jullie samen landden, mag je mee waar hij mag komen en zie je zijn team. Je eigen "
         "verhaalvoortgang verandert daar niet door."),
        ("reizen-met-appa", "Het team",
         "In elk van de vier verhaalgebouwen zit je team rond het kampvuur: Aang-guh na hoofdstuk 1, Katara-guh en Sokka-guh na "
         "hoofdstuk 2, Toph-guh na hoofdstuk 3, Zuka zodra hij zich bij jullie aansluit. Klik ze voor een praatje."),
        ("leegtewalvis", "De questline",
         "Praat met de oude kapitein in De Vadsige Walvis en geloof zijn verhaal. Hij noemt een plek ver buiten de haven: vlieg "
         "erheen en wacht daar stil, boven de leegte. Eerst hoor je het lied, dan doemt de walvis op. Voer hem een "
         "knabbelkoorvrucht en breng de leegteparel naar de kapitein: de hele kroeg gelooft hem eindelijk, en jij krijgt zijn oude "
         "hoorn. Voer daarna een walvis acht keer en hij wordt de jouwe."),
        ("eigen-walvis", "Roepen, sturen, inrichten",
         "Blaas in het Guheinde op de hoorn van de kapitein en je walvis komt naar je toe. Sta op zijn rug met de hoorn in je hand "
         "en hij zwemt langzaam waar jij kijkt; kijk omhoog of omlaag en hij stijgt of zakt. Sluip en blaas: kies wat er op zijn rug "
         "staat. De twee kisten bewaren echt je spullen (alleen jij kunt erin), het bed geeft een uitrustend dutje, het tuintje "
         "geeft elke dag twee knabbelkoorvruchten, en de vlag kies je in zes kleuren. Alleen wie in de walvis gelooft, ziet hem."),
    ],
}
