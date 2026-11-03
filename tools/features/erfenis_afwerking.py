"""
1.9 "Een opknapper!", afwerking slice: the texts of what the slices left open (Java: feature/erfenis/Afwerking.java,
OpasKelder.java, plus small hooks in kaart/Bestellingen and sherlock/Cast).

  - makelaar Guhrrit's postcards from his holiday (cards 4..8; 1..3 are found under plukjes, see erfenis.py)
  - Sherlock's rewards in the village: Opa's theetaartjes (Bakkerij recipe), Oma Breiguh's orders, Bertha on the Muziektent,
    Opa's kelder (sign + diary) and Opa Knabbel's testament
  - only Dutch; en_us gets the same Dutch text (like the other erfenis modules)

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/erfenis_afwerking.py
"""
import json
import os

A = "erfenis.afwerking."

KAARTEN = {
    4: ("Ansichtkaart uit Guhcapulco", [
        "Groetjes uit Guhcapulco! Het strand is",
        "heel vads. Het zand zit overal, zelfs in",
        "mijn klembord. Bouwkundig rapport van de",
        "zandkastelen hier: krakkemikkig. Njeg!"]),
    5: ("Ansichtkaart uit de Knabbelalpen", [
        "Hallo vanaf de Knabbelalpen! Ik heb een",
        "berg beklommen (met de lift). Boven was",
        "er kaasfondue. Ik ben nu officieel een",
        "kaasguh. Zorg goed voor je hoeve!"]),
    6: ("Ansichtkaart uit Parijs-sur-Guh", [
        "Bonjour uit Parijs-sur-Guh! De Eiffeltoren",
        "is ook maar een hoge steiger, als je het",
        "mij vraagt. Ik kocht een stokbrood dat",
        "langer is dan ik. Vahoeg!"]),
    7: ("Ansichtkaart van de Guhcruise", [
        "Ahoi vanaf de cruise! Veertien dekken en",
        "geen enkel plukje om op te ruimen. Ik mis",
        "je dorp een beetje. Nee hoor, het buffet",
        "is te lekker. Maar toch een beetje."]),
    8: ("Ansichtkaart uit Guhwaii", [
        "Aloha uit Guhwaii! Ik draag nu een",
        "bloemetjeshemd en een sombrero. Dat past",
        "niet bij elkaar, zegt iedereen. Ik kom",
        "gauw terug, met zonnebrand. Trots op je!"]),
}

TEKSTEN = {
    "erfenis.fout.recept_geheim": "Dat recept ken je nog niet. Misschien weet een speurneus er meer van...",
    "erfenis.recept.theetaartje": "Opa's theetaartjes (2 koek)",
    A + "oma.extra": "Oma Breiguh knijpt in je wang en stopt je een extraatje toe: %s Guhlden. \"Voor de knabbels, schatje!\"",
    A + "oma.decobon": "Oma Breiguh stopt je een Decobon toe. \"Koop er iets moois van, schatje. Niks met stropdassen.\"",
    A + "bertha": "Bertha (zangeres)",
    A + "ansicht.groet": "Groetjes, makelaar Guhrrit (op vakantie)",
    A + "ansicht.bezorgd": "Een ansichtkaart van makelaar Guhrrit! Hij hangt ook op je Kaartjesbord.",
    A + "boek.door": "door Opa Knabbel",
    A + "boek.testament.titel": "Opa Knabbels testament",
    A + "boek.testament.0": "Mijn testament.\n\nMijn hoeve gaat naar wie er lief voor zorgt, voor de prijs van één knabbel.\n\nNiet aan iemand met een stropdas.",
    A + "boek.testament.1": "Zorg voor de kippen, de koeien en de koekjes. Zeg Oehoe elke avond welterusten.\n\nEn laat Bertha zingen. Ook al is het vals.",
    A + "boek.testament.2": "Een hoeve is pas iets waard als iemand ervan houdt.\n\nGetekend,\n\nOpa Knabbel\n(pootafdrukje)",
    A + "boek.dagboek.titel": "Opa Knabbels dagboek",
    A + "boek.dagboek.0": "Maandag. Kaas gemaakt. Kaas opgegeten. Nieuwe kaas gemaakt.\n\nOehoe zegt dat ik te veel kaas eet. Oehoe is een uil, wat weet die nou van kaas.",
    A + "boek.dagboek.1": "Woensdag. Een guh met een stropdas kwam langs. Hij wil een parkeerplaats op mijn bloemetjes.\n\nIk heb hem een koekje gegeven en hem weggestuurd. Njeg.",
    A + "boek.dagboek.2": "Zondag. Mijn testament geschreven. De hoeve gaat naar wie er lief voor zorgt.\n\nDe laatste bladzijde verstop ik onder mijn oudste boompje. Ssst.",
    A + "cmd.beloning": "Sherlock-beloning %s gegeven (dev).",
    A + "cmd.geen_kelder": "Je hebt Opa's kelder nog niet. Los eerst zaak 5 op (of: /guhs erfenis beloning opas_kelder).",
    A + "kelder.bord.0": "Opa's kelder",
    A + "kelder.bord.1": "Kaas en knabbels",
    A + "kelder.bord.2": "Geen dassen!",
    A + "kelder.bord.3": "Njeg!",
}
for nr, (naam, regels) in KAARTEN.items():
    TEKSTEN[f"{A}ansicht.{nr}.naam"] = naam
    for i, r in enumerate(regels):
        TEKSTEN[f"{A}ansicht.{nr}.tekst.{i}"] = r
    TEKSTEN[f"erfenis.vondst.ansichtkaart_{nr}"] = "een " + naam[0].lower() + naam[1:]


def build(h):
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"erfenis_afwerking: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
