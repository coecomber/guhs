"""
Guhs 1.6.0, test round 5, fix slice FX-C: the verblijfsbord and the Bouwkeet made clear (design ontwerp-safaripark-en-bouwen.md
chapter 11): every button explains itself, a greyed-out one says why, the checklist says how to tick each line, the
fitting home is a proposal first, the keet's reach (64 .. 192 by level) with red ghost blocks outside it, the shape grid,
the missing blocks as a written book.

  build(h)      the texts (NL here, English in tools/lang/en/c187_bouwhulp_fxc.json)
  python tools/features/bouwhulp_fxc.py
                on its own (from the project root): merges the texts into nl_nl.json / en_us.json
"""
import json
import os
import sys

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
EN_BESTAND = os.path.join("tools", "lang", "en", "c187_bouwhulp_fxc.json")

TEKSTEN = {
    # --- the verblijfsbord: the tabs ---
    "gui.guhs.verblijf.tab.familie.uitleg": "De vier dieren van dit bord: hun namen, hoe ze zich voelen, terugroepen of even verstoppen.",
    "gui.guhs.verblijf.tab.verblijf.uitleg": "Hoe groot het verblijf is (de dieren blijven erbinnen), de rand laten zien en waar ze 's nachts slapen.",
    "gui.guhs.verblijf.tab.tevreden.uitleg": "Hoe blij de familie is, en wat ze nog willen. Ga met je muis op een regel staan voor uitleg.",
    "gui.guhs.verblijf.tab.cadeautjes.uitleg": "Blije dieren laten soms een cadeautje achter. Hier haal je ze op.",
    "gui.guhs.verblijf.tab.info.uitleg": "Een weetje over dit dier, je beste foto en wanneer je weer een bord van deze soort mag kopen.",
    "gui.guhs.verblijf.tab.bouw.uitleg": "Laat de familie iets bouwen: ze kijken zelf naar het terrein en bouwen iets dat past.",
    "gui.guhs.verblijf.tab.bezorgen.uitleg": "De ooievaar brengt pakketjes naar de ooievaarsbus van een vriend.",
    # --- greyed-out buttons: why ---
    "gui.guhs.verblijf.reden.niet_jouw": "Dit is het bord van %s: jij mag alleen kijken.",
    "gui.guhs.verblijf.reden.al_klus": "Dit bord heeft al een klus: %s. Stop die eerst (tab Bouwopdracht, knop Stoppen). Een bord doet één klus tegelijk.",
    "gui.guhs.verblijf.reden.al_voorstel": "Er staat al een voorstel: %s. Keur het goed of annuleer het eerst (tab Tevredenheid).",
    "gui.guhs.verblijf.reden.geen_slaapplek": "Er is nog geen slaapplek gekozen.",
    "gui.guhs.verblijf.reden.geen_cadeaus": "Er liggen nog geen cadeautjes. Blije dieren (60% of meer) laten er af en toe een achter.",
    "gui.guhs.verblijf.reden.geen_vrienden": "Nog niemand anders heeft een ooievaarsbus geplaatst.",
    "gui.guhs.verblijf.reden.geen_bestemming": "Kies eerst naar wie het pakketje gaat.",
    "gui.guhs.verblijf.reden.geen_spook": "Deze klus heeft geen spook (de dieren sjouwen of brengen alleen): er valt niets te verschuiven.",
    "gui.guhs.verblijf.reden.bouwt_al": "Het spook wordt al gebouwd. Zet het eerst op pauze in het spookpaneel.",
    # --- the Familie tab ---
    "gui.guhs.verblijf.hernoem.veld.uitleg": "Typ een naam (leeg = geen naam). Klik daarna op Naam.",
    "gui.guhs.verblijf.hernoem.uitleg": "Geeft dit dier de naam uit het vakje. Je ziet hem boven zijn hoofd.",
    "gui.guhs.verblijf.terugroepen.uitleg": "Alle dieren komen meteen terug naar het bord, ook als ze zich verstopt hebben.",
    "gui.guhs.verblijf.verstoppen.uitleg": "De dieren verstoppen zich 20 seconden (handig als je wilt bouwen). Daarna komen ze vanzelf terug, of eerder met Terugroepen.",
    "gui.guhs.verblijf.verstopt": "even weg",
    # --- the Verblijf tab ---
    "gui.guhs.verblijf.vorm.uitleg": "Wissel tussen een rond en een rechthoekig verblijf.",
    "gui.guhs.verblijf.schuif.uitleg": "Sleep om het verblijf groter of kleiner te maken. Je ziet de nieuwe rand meteen.",
    "gui.guhs.verblijf.rand": "Rand tonen",
    "gui.guhs.verblijf.rand.verstop": "Rand verstoppen",
    "gui.guhs.verblijf.rand.uitleg": "Laat de rand van het verblijf zien met roze stofjes op de grond (2 minuten, of tot je hem weer verstopt). De dieren blijven binnen die rand.",
    "gui.guhs.verblijf.thuisplek.wijs": "Slaapplek aanwijzen",
    "gui.guhs.verblijf.thuisplek.wijs.uitleg": "Het menu gaat dicht. Loop naar de plek en klik met de rechtermuisknop op een blok binnen het verblijf: daar slapen de dieren 's nachts. Klik op het bord om te stoppen.",
    "gui.guhs.verblijf.thuisplek.wijs.begin": "Klik met de rechtermuisknop op een blok binnen het verblijf: daar slapen ze 's nachts. (Klik op het bord om te stoppen; je hebt 2 minuten.)",
    "gui.guhs.verblijf.thuisplek.wijs.gestopt": "Slaapplek aanwijzen gestopt.",
    "gui.guhs.verblijf.thuisplek.gezet": "Slaapplek gekozen bij %s. Welterusten!",
    "gui.guhs.verblijf.thuisplek.te_ver": "Die plek ligt buiten het verblijf. Kies een blok binnen de rand.",
    "gui.guhs.verblijf.thuisplek.weghalen": "Slaapplek weghalen",
    "gui.guhs.verblijf.thuisplek.weghalen.uitleg": "Zonder slaapplek slapen de dieren waar ze op dat moment zijn.",
    "gui.guhs.verblijf.thuisplek.geen": "Nog geen slaapplek: ze slapen waar ze zijn.",
    # --- the Tevredenheid tab ---
    "gui.guhs.verblijf.tevreden.hover": "Ga met je muis op een regel staan om te zien hoe je hem afvinkt.",
    "gui.guhs.verblijf.tip.water": "Zorg voor minstens %s blokken water binnen de rand (graven en een emmer water erin).",
    "gui.guhs.verblijf.tip.blokken": "Zet minstens %s van deze blokken binnen de rand: %s.",
    "gui.guhs.verblijf.tip.waar": "Er telt alles binnen de rand, van 6 blokken onder tot 10 boven het bord.",
    "gui.guhs.verblijf.tip.ruimte": "Maak het verblijf groter: een straal van minstens 12, of een rechthoek van minstens 8 bij 8 (tab Verblijf).",
    "gui.guhs.verblijf.tip.geaaid": "Aai vandaag een van de dieren: rechtsklik met een lege hand.",
    "gui.guhs.verblijf.tip.speeltje": "Zet een %s binnen 8 blokken in het verblijf. Te koop in de safari-giftshop (Dierenspeelgoed), of zelf te maken.",
    "gui.guhs.verblijf.tip.vrienden": "Zet binnen 24 blokken van dit bord een verblijfsbord van een vriendje: %s.",
    "gui.guhs.verblijf.tip.iedereen": "elk ander dier",
    "gui.guhs.verblijf.tip.niemand": "(dit dier is liever alleen)",
    "gui.guhs.verblijf.voer.traktatie": "Lekkers (voeren mag, maar hoeft niet: ze eten zelf): %s. Een traktatie geeft hartjes.",
    "gui.guhs.verblijf.tevreden.bouw": "Bouw passend verblijf",
    "gui.guhs.bouwopdracht.verblijf.uitleg": "Zet een voorstel neer als spook: een verblijf met alles wat dit dier blij maakt, met een hek eromheen. Er wordt nog niets gebouwd. Bevalt het, kies dan Goedgekeurd: de dieren bouwen het dan zelf, met jouw blokken (uit je inventaris en de gekoppelde Bank Guh).",
    "gui.guhs.verblijf.voorstel.klaar": "Daar staat het voorstel (het spook). Kijk rond en kies in het bord Goedgekeurd of Annuleren.",
    "gui.guhs.verblijf.voorstel.uitleg": "Het voorstel staat er als spook. Bevalt het? Kies Goedgekeurd: de dieren lopen erheen en bouwen het met jouw blokken. Ontbreekt er iets, dan bouwen ze wat er is en blijft de rest spook.",
    "gui.guhs.verblijf.voorstel.annuleren": "Annuleren",
    "gui.guhs.verblijf.voorstel.annuleren.uitleg": "Haalt het voorstel weg. Er is nog niets gebouwd en niets betaald.",
    "gui.guhs.verblijf.voorstel.goed": "Goedgekeurd, laat bouwen",
    "gui.guhs.verblijf.voorstel.nodig": "Nog nodig:",
    "gui.guhs.verblijf.voorstel.alles_er": "Alle blokken zijn er! (uit je inventaris en de gekoppelde Bank Guh)",
    "gui.guhs.verblijf.voorstel.lijstje.uitleg": "Geeft je een boekje met alle blokken die nog ontbreken voor dit voorstel: de precieze naam en hoeveel. Handig naast de werkbank.",
    "gui.guhs.verblijf.voorstel.lijstje_naam": "Verblijf: %s",
    "gui.guhs.verblijf.reden.niets_nodig": "Er ontbreekt niets: alle blokken zijn er.",
    "gui.guhs.verblijf.voorstel.goed.uitleg": "De familie gaat op werkuitje en bouwt het verblijf zelf, blok voor blok. De blokken komen uit je inventaris en de gekoppelde Bank Guh.",
    "gui.guhs.verblijf.voorstel.tekort": "Nog niet alles is er (de rest blijft spook tot je het hebt): %s",
    "gui.guhs.verblijf.voorstel.gaan": "Op werkuitje! De %s-familie gaat het verblijf bouwen.",
    "gui.guhs.verblijf.voorstel.weg": "Voorstel weggehaald.",
    # --- Cadeautjes, Bezorgen ---
    "gui.guhs.verblijf.cadeautjes.pak.uitleg": "Alle cadeautjes gaan naar je inventaris (vol? dan vallen ze op de grond).",
    "gui.guhs.verblijf.bezorgen.kies.uitleg": "Klik om de volgende vriend met een ooievaarsbus te kiezen.",
    "gui.guhs.verblijf.bezorgen.stuur.uitleg": "De ooievaar neemt de spullen uit een kist naast het bord mee en vliegt naar de ooievaarsbus van je vriend.",
    # --- the Bouwopdracht tab ---
    "gui.guhs.bouwopdracht.variant.uitleg": "Klik voor de volgende variant van deze klus.",
    "gui.guhs.bouwopdracht.zoek.uitleg": "De familie zoekt zelf een goede plek (binnen 48 blokken van het bord) en zet de klus neer als spook. Je kunt het daarna nog verschuiven.",
    "gui.guhs.bouwopdracht.wijs.uitleg": "Het menu gaat dicht. Klik daarna met een vormstaf op het blok (of de 2 blokken) waar de klus moet komen, binnen 48 blokken van het bord. Je hebt 2 minuten.",
    "gui.guhs.bouwopdracht.bedien.uitleg": "Opent het spookpaneel: verschuiven, draaien, bouwen of pauzeren.",
    "gui.guhs.bouwopdracht.stop.uitleg": "Stopt de klus: het spook gaat weg (wat al gebouwd is blijft staan) en de familie komt naar huis.",
    "gui.guhs.bouwopdracht.bank.uitleg": "Klik met een Banksleutel op het bord om een Bank Guh te koppelen: de familie pakt haar blokken dan ook uit de bank.",
    # --- the Bouwkeet: its reach ---
    "gui.guhs.bouwhulp.bereik.buiten": "Let op: een deel van het gebouw (%s blokken, rood) valt buiten het bereik van de keet (%s blokken). Dat deel wordt niet gebouwd.",
    "gui.guhs.bouwhulp.bereik.buiten.klaar": "Een deel van het gebouw (%s blokken) valt buiten het bereik van de keet (%s blokken): dat blijft spook.",
    "gui.guhs.bouwhulp.bereik.tip": "Tip: een hoger niveau van de keet heeft een groter bereik. Of schuif het spook dichter naar de keet.",
    "gui.guhs.bouwhulp.bereik.tip.volgend": "Tip: schuif het dichter naar de keet, of upgrade de keet: het volgende niveau bouwt tot %s blokken ver.",
    "gui.guhs.bouwhulp.bereik.tip.max": "Tip: schuif het dichter naar de keet.",
    "gui.guhs.bouwkeet.niveau.wat.0": "Vormstaf en kwasten, vormen tot 16, daken, eigen blauwdrukken tot 16, de bibliotheek. Bouwt tot 64 blokken ver",
    "gui.guhs.bouwkeet.niveau.wat.1": "Bouwguhs, vormen tot 24, draadloos materiaal uit alle kisten op het vadsnet. Bouwt tot 96 blokken ver",
    "gui.guhs.bouwkeet.niveau.wat.2": "Natuurvormen, de egaliseerder, bloktekst, blauwdrukken tot 32. Bouwt tot 128 blokken ver",
    "gui.guhs.bouwkeet.niveau.wat.3": "Verfmachine, tunnelboor, publiceren in de bibliotheek. Bouwt tot 160 blokken ver",
    "gui.guhs.bouwkeet.niveau.wat.4": "Vormen en blauwdrukken tot 64, en Pof! Bouwt tot 192 blokken ver",
    "gui.guhs.bouwkeet.nog.sec": "nog ~%s s",
    "gui.guhs.bouwkeet.nog.min": "nog ~%s min",
    "gui.guhs.bouwkeet.bouw.uitleg": "Laat de keet het spook bouwen. Ontbreekt er iets, dan vraagt hij eerst of hij toch moet bouwen (wat mist blijft spook).",
    "gui.guhs.bouwkeet.pauze.uitleg": "Zet het bouwen even stil. Met Bouw! gaat hij verder.",
    "gui.guhs.bouwkeet.niet_jouw.uitleg": "Dit is niet jouw keet: alleen de eigenaar mag bouwen.",
    "gui.guhs.bouwkeet.nodig.hover": "Ga met je muis op een blokje staan voor de naam.",
    # --- the shortage list ---
    "gui.guhs.bouwhulp.tip.aantal": "Nog %s nodig",
    "gui.guhs.bouwhulp.tip.heb": "Je hebt er %s van de %s",
    "gui.guhs.bouwhulp.lijstje.knop": "Lijstje",
    "gui.guhs.bouwhulp.lijstje.uitleg": "Exporteer naar lijstje: je krijgt een boek met alle blokken die nog ontbreken (naam en aantal). Handig bij de werkbank.",
    "gui.guhs.bouwhulp.lijstje.niets.uitleg": "Er ontbreekt niets (in creative bouwt het gratis).",
    "gui.guhs.bouwhulp.lijstje.niets": "Er ontbreekt niets!",
    "gui.guhs.bouwhulp.lijstje.gegeven": "Het boodschappenlijstje zit in je inventaris.",
    "gui.guhs.bouwhulp.lijstje.titel": "Boodschappenlijstje",
    "gui.guhs.bouwhulp.lijstje.kop": "Voor: %s",
    "gui.guhs.bouwhulp.lijstje.totaal": "%s blokken in totaal",
    # --- the shape grid ---
    "gui.guhs.bouwhulp.raster.titel": "Kies: %s",
    "gui.guhs.bouwhulp.raster.uitleg": "Klik op een tegel. Grijs = nog op slot (ga erop staan om te zien waarom).",
    "gui.guhs.bouwhulp.raster.knop": "Klik: kies uit alle mogelijkheden. Shift-klik: één terug.",
}


def build(h):
    for k, v in TEKSTEN.items():
        h.NL[k] = v


def _direct():
    with open(EN_BESTAND, encoding="utf-8") as f:
        en = json.load(f)
    for taal, extra in (("nl_nl", TEKSTEN), ("en_us", {k: en.get(k, v) for k, v in TEKSTEN.items()})):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(extra)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    missing = [k for k in TEKSTEN if k not in en]
    print(f"bouwhulp_fxc: {len(TEKSTEN)} texts ({len(missing)} without English)")


if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
