"""
bbq2 (oude-scenes) - the wiki notes of the six camera scenes of the older stories (CONTRACT_130 2.4; the docs step of phase 3
turns this into pages; this module is not in FEATURES and builds nothing). No new story, building or character: one paragraph
on each story's own page, and one system page that explains the scenes.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "oude_scenes": ("Filmpjes bij de oude verhalen", "icon_spyglass",
                        "Zes oudere verhalen hebben elk een kort filmpje gekregen op hun grootste moment: Baltoguh, de Guhtwo, "
                        "626-guh, het Hemelkapelletje, de Grillguh en de Timmerguh. Je ziet het één keer, precies op het moment "
                        "in het verhaal, en kunt het daarna terugkijken in de Guhdex.",
                        ["systemen/guhdex"]),
    },
    "npc_home": {},
    "entity_home": {},
    "tekst": [
        ("systemen/oude_scenes", "Hoe werkt het?",
         "Een filmpje duurt 15 tot 25 seconden. De camera neemt het even over: er komen zwarte balken in beeld, je kunt niet lopen "
         "en er kan je niets gebeuren. Daarna gaat het verhaal gewoon verder waar het was. Elk filmpje speelt één keer per speler; "
         "speel je samen, dan krijgt ieder zijn eigen filmpje op zijn eigen moment en merkt de ander er niets van. Ook de sneeuwstorm, "
         "het onweer en de nacht in een filmpje zijn alleen voor wie kijkt: voor alle anderen blijft het weer zoals het was."),
        ("systemen/oude_scenes", "Terugkijken",
         "Open de Guhdex, ga naar het tabblad Verhalen en klik het verhaal aan. Onderaan staat Opnieuw bekijken. Sta je bij het "
         "gebouw van het verhaal, dan speelt het filmpje daar opnieuw af. Ben je ergens anders, dan krijg je het als prentenboek: "
         "een tekening met de zinnen van het filmpje."),
        ("systemen/oude_scenes", "Was je al verder?",
         "Had je een verhaal al (bijna) uit toen de filmpjes erbij kwamen? Dan hoef je niets opnieuw te doen. Het filmpje staat "
         "gewoon in je Guhdex klaar om te bekijken, njeg."),
        ("systemen/oude_scenes", "De zes filmpjes",
         "De witte wolf-guh (Baltoguh en Nomguh): op het dieptepunt van de tocht terug sneeuwt het ineens zo hard dat je geen drie "
         "blokken ver meer kijkt, en dan staat ze op de Wolvenrots. Dag 45: de nacht van de knal (het kloon-eiland): wat Professor "
         "Knabbelkloon zich herinnert als je hem alle zes labnotities brengt. Ohana, bij het kampvuurtje (Guhwai'i): 626-guh met "
         "het prentenboek, en Lilo-guh die erbij komt zitten. Het Knuffelhart klopt (het Hemelkapelletje): de wolken schuiven open "
         "zodra de wolkenhoeder alle drie de dingen heeft. Hij brandt weer! (de Grillguh): het vlammetje loopt het grillkoolframe "
         "rond, en in de verte kijkt iemand met een punthoed toe. Het dak zit erop (de Timmerguh): de vlag in top, en het eerste "
         "bewonertje trippelt naar binnen."),
        ("verhalen/balto", "Het filmpje",
         "Op het dieptepunt van de tocht terug speelt de eerste keer een filmpje: de storm, de witte wolf-guh op de Wolvenrots en "
         "het huilen. Daarna klaart de storm op en rijd je verder. Moet je de tocht nog een keer doen, dan krijg je op die plek het "
         "gesprek van vroeger."),
        ("verhalen/mewtwo", "Het filmpje",
         "Breng je de professor alle zes labnotities, dan zie je eerst wat hij zich herinnert: de nacht van dag 45."),
        ("verhalen/guhwaii", "Het filmpje",
         "Ga je na de drie lieve dingen terug naar Lilo-guh, dan zie je eerst het filmpje bij het kampvuurtje naast het paalhuisje."),
        ("verhalen/hemel", "Het filmpje",
         "Heeft de wolkenhoeder het guhkristal, de gouden kaasknabbel en het pluisveertje, dan zie je het Knuffelhart beginnen te "
         "kloppen. Pas na het filmpje klopt het echt voor jou."),
        ("verhalen/grillguh", "Het filmpje",
         "Steek je de put van de Grillguh aan, dan zie je het vuur door het frame lopen. Wie er in de verte staat te kijken, kom je "
         "later tegen in In de ban van de Knabbelring."),
        ("verhalen/timmerguh", "Het filmpje",
         "Ligt het laatste dakpluisje op het dak, dan zie je de vlag in top gaan en het eerste bewonertje naar binnen trippelen. "
         "Daarna krijg je je eigen kleine guhhuisje."),
    ],
}
