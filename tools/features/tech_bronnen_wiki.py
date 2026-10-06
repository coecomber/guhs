"""
Wiki texts of the vadskracht sources (bbq2, tech-bronnen). Not a feature module (not in FEATURES): the docs step of the merge
reads WIKI and turns it into pages (CONTRACT_130 2.4). The numbers come from VadsGetallen.java and TechbronGetallen.java.
The blocks themselves (Knuffelgenerator, Disco-dynamo, Blubkacheltje, Gloeisterkern, Knabbelbatterij) get their own wiki
page from the game data; this adds one system page that tells how the sources work together.
"""
from features import tech_bronnen as t
from features import vadskracht as v

_g = v.getal
_max = v.bron_max()

WIKI = {
    "systemen": {
        "vadskrachtbronnen": ("Bronnen van vadskracht", "knuffelgenerator",
                              "Vadskracht komt van guhs. Ze rennen hem bij elkaar in een Guhrad, knuffelen hem bij elkaar op een "
                              "Knuffelgenerator of dansen hem bij elkaar op een Disco-dynamo. Twee bronnen doen het zonder guh: het "
                              "Blubkacheltje en de Gloeisterkern.",
                              ["systemen/vadskracht"]),
    },
    "tekst": [
        ("systemen/vadskrachtbronnen", "Het Guhrad",
         f"Een tamme guh in een Guhrad geeft {_g('GUHRAD')} vadskracht, een blij guhtje {_g('GUHRAD_BLIJ')}. Een guh wordt nooit moe en "
         "hoeft niet te eten. De guhs uit de verhalen doen het elk op hun eigen manier: de Baltoguh rent harder dan wie ook, Guhtwo "
         "rent helemaal niet (hij zweeft en laat het rad vanzelf draaien), de 626-guh telt dubbel, Sam-guh sjouwt het rad rond met "
         "rugzak en al en Guhshi fladdertrappelt. Zij geven "
         + " of ".join(str(n) for n in sorted({k for k, _ in v.GUHRAD_VARIANTEN.values()} | {b for _, b in v.GUHRAD_VARIANTEN.values()}))
         + f" vadskracht. Er tellen hooguit {_max['guhrad']} Guhraden per opstelling."),
        ("systemen/vadskrachtbronnen", "Een blij guhtje rent harder",
         "Of een guh blij is wordt bekeken op het moment dat je hem in het rad zet, en dat blijft zo zolang hij rent. Een guh die "
         f"net op een Knuffelgenerator lag is nog {t.getal('BLIJ_NA_KNUFFEL') // 20} tellen blij: pak hem op van het kussen en zet "
         "hem meteen in het rad. Kijk naar het rad en je leest of hij blij rent."),
        ("systemen/vadskrachtbronnen", "De Knuffelgenerator",
         f"Een groot roze kussen van 2 bij 2 blokken. Tamme guhs binnen {t.getal('BEREIK')} blokken komen er vanzelf op liggen. Elke "
         f"guh die ligt te knuffelen geeft {_g('KNUFFEL_PER_GUH')} vadskracht, en er passen er {_g('KNUFFEL_MAX_GUHS')} op "
         f"({_g('KNUFFEL_PER_GUH') * _g('KNUFFEL_MAX_GUHS')} vadskracht). Het geborduurde snoetje slaapt als het kussen leeg is en "
         f"kijkt verbaasd als het vol ligt. Er tellen hooguit {_max['knuffelgenerator']} Knuffelgeneratoren per opstelling."),
        ("systemen/vadskrachtbronnen", "De Disco-dynamo",
         "Een dansvloer van 3 bij 3 blokken met een draaitafel. Leg er een muziekplaat op (klik met de plaat op de vloer) en hij "
         f"blijft draaien, steeds opnieuw. Tamme guhs binnen {t.getal('BEREIK')} blokken komen dansen: elke danser geeft "
         f"{_g('DISCO_PER_GUH')} vadskracht, hooguit {_g('DISCO_MAX_GUHS')} guhs ({_g('DISCO_PER_GUH') * _g('DISCO_MAX_GUHS')} "
         "vadskracht). Elke plaat heeft zijn eigen lichtshow op de vloer. Met de zeldzame plaat 'Ze hangen aan me veh' geeft elke "
         f"danser {t.getal('DISCO_BONUS')} vadskracht extra, en klopt er een groot hart op de vloer. Een lege hand haalt de plaat er "
         f"weer af. Er tellen hooguit {_max['disco_dynamo']} Disco-dynamo's per opstelling."),
        ("systemen/vadskrachtbronnen", "Welke guhs komen er?",
         "Tamme guhs die vrij zijn. Zet in het Guh-menu 'Rondvadsen' uit en je guh blijft op het kussen of de dansvloer als jij "
         "wegloopt: zo blijft je fabriek draaien. Een guh die jou volgt doet alleen mee zolang jij in de buurt bent, en loopt daarna "
         "weer met je mee. Een guh die moet zitten telt alleen mee als hij óp het kussen of de vloer zit. Guhs die in een Guhhuisje "
         "wonen komen niet: die hebben hun klusjes en hun eigen bedje. Wilde guhs ook niet."),
        ("systemen/vadskrachtbronnen", "Het Blubkacheltje",
         "Een kacheltje met een glazen pot erop. Stop er een Sausblubje in een potje in en geef het af en toe een kaasknabbel: dan "
         f"blubt het {_g('BLUBKACHELTJE')} vadskracht bij elkaar. Eén knabbel houdt het {t.getal('BLUB_SECONDEN') // 60} minuten warm "
         f"en in het bakje passen {t.getal('BLUB_VOER_MAX')} knabbels. Een Knabbelbuis of trechter mag de knabbels ook brengen. Heeft "
         "het blubje trek, dan geeft het niks. Sluipen en klikken met een lege hand haalt het blubje er weer uit, potje en al. Er "
         f"tellen hooguit {_max['blubkacheltje']} Blubkacheltjes per opstelling."),
        ("systemen/vadskrachtbronnen", "De Gloeisterkern",
         f"De gloeister van de Aangebrande Mika in een kooitje van zoutkristal. Hij kost één gloeister en geeft dan "
         f"{_g('GLOEISTERKERN')} vadskracht, voor altijd: geen guh, geen knabbels, hij raakt nooit op. Er telt er maar "
         f"{_max['gloeisterkern']} per opstelling; een tweede valt in slaap."),
        ("systemen/vadskrachtbronnen", "De Knabbelbatterij",
         f"Bewaart wat je opstelling over heeft, tot {_g('BATTERIJ')} vadskracht (zo'n {_g('BATTERIJ') // _g('GUHRAD') // 60} minuten "
         "van één Guhrad), en past bij als je machines even meer vragen dan je bronnen geven. Breek je hem af, dan houdt hij zijn "
         "lading: je kunt een volle batterij meenemen naar een andere opstelling. Op het item lees je hoeveel erin zit."),
    ],
}
