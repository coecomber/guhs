"""
Super Guhrio, world 2 (bbq2, slice guhrio-w2) - what the docs step (phase 3) needs to write about the cellars. Not in
FEATURES: nothing here is built by the generators. CONTRACT_130 2.4 gives the shape of WIKI. The story page itself
("verhalen/super-guhrio") and the page about playing a level belong to the engine (guhrio_wiki.py); this file only adds
paragraphs to them. The blocks of this world (Keldergrond, Keldersteen, Warppijp, Eierslot, Broednest) get their wiki pages
from the game data by themselves.
"""
WIKI = {
    "verhalen": {},
    "systemen": {},
    "npc_home": {},
    "entity_home": {},
    "tekst": [
        ("verhalen/super-guhrio", "Wereld 2: de kelders",
         "Onder de binnentuin liggen de kelders van het kasteel: donker, blauwgroen gemetseld en vol groene pijpen. Level 2-1 heet De "
         "buizenkelder. In de pijpen wonen Hapbloemen: zolang zo'n bloem uit haar pijp steekt krijg je een natte zoen en sta je weer bij "
         "je vlaggetje, maar is ze binnen, dan kun je op de pijp gaan staan en erin duiken (S). De eerste pijp leidt naar een verborgen "
         "muntenkelder, en de pijp op het eilandje in de kaassaus is een geheime doorsteek. Over de saus glijden platforms heen en weer, "
         "verderop vallen de blokken onder je voeten weg en brengt een lift je naar een hoge richel."),
        ("verhalen/super-guhrio", "De vadsmunt in de rode kooi",
         "In level 2-1 hangt een grote vadsmunt in een kooi van rode blokken. De uitroeptekenschakelaar die de kooi opent zit aan het "
         "eind van een tunneltje waar jij niet in past. Het schild van een Schild-Mika wel: spring op de Mika die ervoor loopt, geef "
         "zijn schild een zetje richting het tunneltje en spring daarna vanaf de richel omhoog, de kooi in. Let op: het schild komt "
         "ook weer terug, njeg."),
        ("verhalen/super-guhrio", "Het ei van Guhshi",
         "Level 2-2 heet Het nest van Guhshi. Halverwege staat een hek met een eierslot: het gaat alleen open voor wie het ei van "
         "Guhshi bij zich heeft. Het ei ligt in het eierkamertje, achter de deur onder het geschilderde ei (ga erin staan en druk op "
         "W). Met het ei loop je door het hek naar het broednest, een warm kacheltje met stro erop. Daar kruipt Guhshi uit zijn ei; hij "
         "denkt dat jij zijn mama bent. Vanaf dat moment wacht hij op zijn nest op je en mag je in de burcht (wereld 3) ook op zijn rug. "
         "Het filmpje van het uitkomen kun je in de Guhdex (Verhalen > Super Guhrio) nog eens bekijken."),
        ("verhalen/super-guhrio", "De warpkamer",
         "Niet elke pijp komt uit waar je denkt. In level 2-2, vlak voor de grote trap, woont een Hapbloem in een pijp die naar de "
         "warpkamer leidt: een pikzwarte kamer met drie gouden warppijpen, elk onder een getal: 3-1, 3-2 en 1-1. Duik in zo'n pijp en "
         "je staat meteen in dat level, en de poort van dat level in de levelhal blijft voortaan voor je open. Het level waaruit je "
         "vertrok telt dan niet als gehaald, en voor de tijd van het hele kasteel in één keer telt een warp ook niet mee. In de "
         "warpkamer ligt bovendien de derde grote vadsmunt van level 2-2; de groene pijp aan het eind brengt je gewoon terug."),
    ],
}
