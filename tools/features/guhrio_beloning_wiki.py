"""
bbq2 (guhrio-beloning): the wiki entries of the slice (CONTRACT_130 2.4; not in FEATURES: the docs step reads WIKI). The
items, blocks, clothes and NPCs get their own wiki pages from the game data; this adds the page of Pad-guh's kraam and the
rewards, the page of the green pipe, where the two NPCs live and a few paragraphs for the castle's page.
"""
WIKI = {
    "verhalen": {
        "pad-guhs-kraam": dict(
            nl="Pad-guh's kraam en de beloningen", img="struct_guhrio_kasteel",
            lead_nl="Op het voorplein van het Kasteel van de Grote Nether-Mika zit Pad-guh op de toonbank van zijn paddenstoelenkraam. Hij "
                    "vertelt dat Prinses Perzikguh is meegenomen 'voor een stukje taart', verkoopt outfits en bouwblokken voor de munten uit de "
                    "levels, en heeft een gouden vadspet voor wie alle achttien grote vadsmunten vindt. Na het duel wacht in de torenkamer "
                    "Guhshi op je: één per speler. Njeg!",
            ftb=[("guhs_guhrio", "guhrio_beloning")], structure="guhrio_kasteel", npcs=["padguh", "perzikguh"],
            related=["verhalen/super-guhrio", "systemen/super-guhrio-spelen", "systemen/groene-reispijp"]),
    },
    "systemen": {
        "groene-reispijp": ("De groene reispijp", "block_guhriobeloning_pijp",
                            "De groene pijp die echt werkt: ga erop staan, sluip, en je komt uit de dichtstbijzijnde pijp van dezelfde kleur.",
                            ["verhalen/pad-guhs-kraam"]),
    },
    "npc_home": {"padguh": "bouwwerken/guhrio_kasteel", "perzikguh": "bouwwerken/guhrio_kasteel"},
    "entity_home": {},
    "tekst": [
        ("verhalen/pad-guhs-kraam", "De kraam",
         "Pad-guh rekent in de munten van Super Guhrio: elke munt in een level telt de eerste keer dat je hem pakt voor je buidel, en bij "
         "elke vlaggenmast krijg je een beetje fooi (één munt per vijf op je paneel). Hij verkoopt drie outfits (de rode Guhrio-pet met "
         "snor en de groene Luiguh-pet voor 30 munten, het Schild-Mika-schild voor 40) en drie bouwblokken (het Knabbel-vraagtekenblok "
         "voor 20, vier kasteelvlaggenmasten voor 10, twee groene reispijpen voor 25). Iedere speler heeft een eigen buidel."),
        ("verhalen/pad-guhs-kraam", "Bedankt! Maar de prinses...",
         "Elke keer dat je een wereld helemaal hebt uitgespeeld (de binnentuin, de kelders, de burcht) bedankt Pad-guh je en vertelt hij "
         "dat de prinses in een ander kasteeldeel is. Ze zit al die tijd gewoon in de torenkamer taart te eten: je kunt via de trap in "
         "de levelhal bij haar langs."),
        ("verhalen/pad-guhs-kraam", "Guhshi is van jou",
         "Wie het duel met de Grote Nether-Mika wint, mag Guhshi meenemen. Hij staat in de torenkamer naast Prinses Perzikguh: klik op hem "
         "en zeg ja. Je krijgt een eigen Guhshi, getemd en met zijn rode zadel al op. De Guhshi in de torenkamer blijft daar voor de "
         "volgende speler: iedereen krijgt er één. Van de prinses krijg je haar kroontje en een taart."),
        ("verhalen/pad-guhs-kraam", "Wat Guhshi kan",
         "Op zijn rug spring je met spatie. Houd spatie vast in de lucht en hij fladdert: hij valt even niet en klimt zelfs een beetje, "
         "één keer per sprong, goed voor gaten van een blok of zeven. Ligt er binnen vijf blokken een kaasknabbel op de grond, dan hapt "
         "hij die met zijn lange tong weg (één per twee tellen; knabbels die net zijn neergegooid of voor iemand bestemd zijn laat hij "
         "liggen). In zijn guh-menu zet je de tong uit."),
        ("verhalen/pad-guhs-kraam", "De grote vadsmunten en de gouden vadspet",
         "In elk level liggen drie grote vadsmunten. De Guhdex (Verhalen > Pad-guh's kraam en de beloningen) laat ze alle achttien zien, "
         "per level, met een vinkje bij wat je hebt; Pad-guh laat het ook zien als je ernaar vraagt. Heb je ze allemaal, dan geeft hij "
         "je de gouden vadspet."),
        ("verhalen/pad-guhs-kraam", "Het highscorebord",
         "Rechts op het voorplein staat het highscorebord. Boven het gouden blok zweven de snelste tijden van de server: de top drie van "
         "het hele kasteel in één keer en het record van elk level. Klik op het blok en je leest in de chat je eigen beste tijden "
         "ernaast."),
        ("systemen/groene-reispijp", "Zo werkt hij",
         "Zet twee groene reispijpen neer, hooguit 50 blokken uit elkaar. Stapel je er meer op elkaar, dan wordt de pijp hoger: de "
         "bovenste is de mond. Ga op de mond staan en sluip (of klik op de pijp waar je op staat): je glijdt erin en komt uit de "
         "dichtstbijzijnde andere pijp van dezelfde kleur. Boven die pijp moeten twee blokken vrij zijn. Laat de sluiptoets even los "
         "voordat je teruggaat. Rijdend op een guh pas je er niet in."),
        ("systemen/groene-reispijp", "Meer paren",
         "Klik met verf op een pijp en de hele pijp krijgt die kleur. Een rode pijp hoort bij de dichtstbijzijnde rode, een blauwe bij "
         "een blauwe: zo zet je meerdere paren vlak bij elkaar. Onderweg kan niets je raken, en je verliest niets."),
        ("bouwwerken/guhrio_kasteel", "Het voorplein",
         "Links van het pad staat Pad-guh's paddenstoelenkraam, met ernaast een klein paddenstoeltje en een zwevend Knabbel-vraagtekenblok "
         "tussen twee stenen (spring ertegen: elke dag één kaasknabbel). Rechts staan het highscorebord en een vlaggenmast. Aan elke kant "
         "staat een groene reispijp: samen zijn ze een paar, dus je kunt het meteen proberen."),
        ("bouwwerken/guhrio_kasteel", "De torenkamer",
         "Achter de duelzaal ligt de kamer van Prinses Perzikguh: een roze loper naar haar taarttafel, een perzik van wol in een gouden "
         "lijst, haar hemelbed, een theehoekje, een piano en het nest van Guhshi."),
    ],
}
