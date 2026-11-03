"""
Guhs 1.9 "Een opknapper!", slice DECO (Java: feature/erfenis/DecoWinkel, Paden, DecoActies, Knusheid, client/DecoBalk):
the texts of the decowinkel (shop tab in the overview), drawing paths and hekjes per tile, the gum, moving and turning deco,
the Knusheid milestones and the three Knusheid titles. Dutch in nl_nl AND en_us (house rule).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/erfenis_deco.py
"""
import json
import os

G = "erfenis."
W = "erfenis.decowinkel."

PADEN = {
    "zandpaadje": "Zandpaadje",
    "knarspad": "Knarspad",
    "stapsteentjes": "Stapsteentjes",
    "klinkguhpad": "Klinkguhpad",
    "pootjespad": "Pootjespad",
    "kinderkopjes": "Kinderkopjes",
    "bloesemblaadjespad": "Bloesemblaadjespad",
    "vadsgoudpad": "Vadsgoud tegelpad",
    "sterrenpad": "Sterrenpad",
}

CATEGORIEEN = {
    "paden": "Paden",
    "hekjes": "Hekjes",
    "bloemen": "Bloemen",
    "zitjes": "Zitjes",
    "licht": "Licht",
    "water": "Water",
    "beelden": "Beelden",
    "boerderij": "Boerderij",
    "bijzonder": "Bijzonder",
}

TEKSTEN = {
    W + "tab": "Deco",
    W + "tab_tip": "De decowinkel: paden, hekjes en knusse spulletjes voor Guhlden. Hoe hoger je Hoeve, hoe meer keus, njeg!",
    W + "cat_nieuw": "Nieuw op dit Hoeve-level!",
    W + "bon": "Decobon: %s",
    W + "bon_tip": "Aan: je volgende stukje betaal je met een Decobon in plaats van Guhlden. Gratis is de lekkerste prijs.",
    W + "gum": "Gum",
    W + "gum_tip": "Sleep over paden en hekjes om ze weg te halen. Ze gaan in je voorraad en trek je later gratis weer neer.",
    W + "knus": "Knusheid: +%s",
    W + "knus_pad": "Knusheid: +1 per 10 tegels",
    W + "knus_hek": "Knusheid: +1 per 5 hekjes",
    W + "knus_hek4": "Knusheid: +1 per 4 hekjes",
    W + "prijs": "Prijs: Ƒ%s",
    W + "prijs_tegel": "Prijs: Ƒ%s per tegel",
    W + "voorraad": "%s in je decoschuurtje (gratis neerzetten)",
    W + "voorraad_tegels": "%s tegels in je voorraad (eerst gratis)",
    W + "klik_trek": "Klik, en sleep dan over het gras om te trekken. Hoekjes en kruisingen gaan vanzelf, vahoeg.",
    W + "klik_koop": "Klik: meteen neerzetten (je betaalt bij het neerzetten). Shift-klik: kopen voor in je decoschuurtje.",
    W + "klik_bon": "Klik: neerzetten met een Decobon. Shift-klik: met de bon in je decoschuurtje.",
    W + "klik_deco": "Klik: kiezen (verplaatsen, draaien, opbergen)",
    W + "verplaats_tip": "Pak het op en zet het ergens anders neer. R draait. Je kunt het ook gewoon slepen.",
    W + "weg_tip": "Terug in je decoschuurtje. Gratis, en je raakt niks kwijt.",
    W + "sleep_tip": "Tip: sleep het stukje om het te verplaatsen.",
    W + "streek": "%s tegels · Ƒ%s",
    W + "streek_voorraad": " (%s uit je voorraad)",
    W + "streek_niks": "Hier kan het niet",
    W + "knus_delen": "Gebouwen %s · paden %s · deco %s",
    W + "knus_volgende": "Volgende mijlpaal: %s",
    W + "knus_klik": "Klik: naar de decowinkel",
    W + "hulp_trek": "Slepen: trekken  ·  WASD: schuiven  ·  rechtermuis / Esc: stoppen",
    W + "hulp_gum": "Slepen: weggummen  ·  WASD: schuiven  ·  rechtermuis / Esc: stoppen",
    W + "hulp_koop": "Klik: kopen en neerzetten  ·  R: draaien  ·  rechtermuis / Esc: stoppen",
    W + "hulp_verplaats": "Klik: hier neerzetten  ·  R: draaien  ·  rechtermuis / Esc: laten staan",
    W + "pad_voorraad": "Paden in voorraad",
    W + "naar_winkel": "Naar de decowinkel",
    # what may not
    W + "fout.niet_te_koop": "Dat is een bijzonder stukje: dat koop je niet, dat verdien je!",
    W + "fout.bon_pad": "Een Decobon is voor een echt stukje deco, niet voor een tegeltje pad.",
    W + "fout.geen_bon": "Je hebt geen Decobon meer. Ze zitten in het FTB-hoofdstuk \"Een opknapper!\".",
    W + "fout.gebouw": "Daar staat een gebouw.",
    W + "fout.deco": "Daar staat al deco.",
    W + "fout.al": "Dat ligt er al.",
    W + "fout.op_pad": "Een hekje midden op het pad? Dan struikelt iedereen, njeg.",
    W + "fout.geld_op": "Op is op: de Guhlden waren op na %s tegels.",
    W + "fout.hier_niet": "Hier kan dat niet (overwoekerd, gebouw of deco in de weg).",
    # done
    W + "ok.getrokken": "%s tegels %s erbij! (Ƒ%s)",
    W + "ok.getrokken_voorraad": "%s tegels %s erbij! (Ƒ%s, %s gratis uit je voorraad)",
    W + "ok.gekocht": "%s staat in je decoschuurtje. Knus!",
    W + "ok.gegumd": "%s tegels weggegumd en netjes bewaard.",
    # the milestones (fired once, the first time the village gets there)
    G + "knus.mijlpaal.50": "Knusheid %s! Er hangt een knus luchtje in je dorp. Ruikt naar kaas.",
    G + "knus.mijlpaal.100": "Knusheid %s! De bewoners zwaaien naar elkaar, njeg.",
    G + "knus.mijlpaal.250": "Knusheid %s! Titel verdiend: Knusse Guh. Je Hoeve gaat ervan blozen.",
    G + "knus.mijlpaal.500": "Knusheid %s! Buurdorpen komen stiekem kijken hoe jij het doet.",
    G + "knus.mijlpaal.1000": "Knusheid %s! Titel verdiend: Knusvads. Vahoeg!",
    G + "knus.mijlpaal.1500": "Knusheid %s! Zelfs de Mika's willen hier op vakantie.",
    G + "knus.mijlpaal.2000": "Knusheid %s! Het knusste dorp van de hele Guhmension. Bijna.",
    G + "knus.mijlpaal.2500": "Knusheid %s! Titel verdiend: Knusser Bestaat Niet. Echt niet.",
    G + "knus.mijlpaal.5000": "Knusheid %s! Guhs uit verre landen schrijven ansichtkaarten over je dorp.",
}

TITELS = {
    "erfenis_knus_250": ("Knusse Guh", "Maak je dorp in De Erfenis knus: Knusheid 250."),
    "erfenis_knus_1000": ("Knusvads", "Knusheid 1.000 in je dorp. Paden, hekjes, bloemetjes: alles telt."),
    "erfenis_knus_2500": ("Knusser Bestaat Niet", "Knusheid 2.500. Knusser kan echt niet."),
}


def alle_teksten():
    t = {G + "pad." + k: v for k, v in PADEN.items()}
    t.update({W + "cat." + k: v for k, v in CATEGORIEEN.items()})
    t.update(TEKSTEN)
    for k, (naam, hint) in TITELS.items():
        t["gui.guhs.titels.naam." + k] = naam
        t["gui.guhs.titels.hint." + k] = hint
    return t


def build(h):
    for k, v in alle_teksten().items():
        h.lang(k, v, v)


def _direct():
    t = alle_teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as fh:
            d = json.load(fh)
        d.update(t)
        with open(p, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(d, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    print(f"erfenis_deco: {len(t)} texts")


if __name__ == "__main__":
    _direct()
