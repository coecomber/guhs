"""
1.10.0 "Het Vadsrijk", round U, slice U-OPSLAGVIP: only the first bought storage expansion is for sale ("Binnenkort!" for the
rest, price curve in [opslag] of the balance file) + the "WORD GUH VIP++" parody overlay (Java: feature/vadsrijk/opslag).

Only texts (Dutch + real English). Standalone (from the project root):
PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_opslagvip.py
"""
import json
import os

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")

P = "vadsrijk.opslag."
V = P + "vip."
TEKSTEN = {
    # locked "Binnenkort!" tabs
    P + "tab.binnenkort": ("Binnenkort!", "Coming soon!"),
    P + "tab.binnenkort.uitleg": ("Deze uitbreiding komt in een latere update. Even geduld, vadsje!",
                                  "This expansion arrives in a later update. Patience, little vads!"),
    # the joke overlay (a PARODY: there is no real payment anywhere in Guhs)
    V + "titel": ("WORD GUH VIP++", "BECOME GUH VIP++"),
    V + "van": ("VAN %s VOOR MAAR 34,99!!!", "FROM %s FOR ONLY 34.99!!!"),
    V + "oud": ("50 EURO", "50 EURO"),
    V + "klok": ("⏰ NOG MAAR %s!", "⏰ ONLY %s LEFT!"),
    V + "badge": ("\U0001f525 BESTE DEAL \U0001f525", "\U0001f525 BEST DEAL \U0001f525"),
    V + "sticker": ("97%% KORTING", "97%% OFF"),
    V + "koop": ("KOOP NU!!!", "BUY NOW!!!"),
    V + "nee": ("Nee bedankt, ik betaal liever de volle prijs", "No thanks, I prefer paying full price"),
    V + "voordeel1": ("✔ Gouden naambordje (niemand ziet het)", "✔ Golden name tag (nobody sees it)"),
    V + "voordeel2": ("✔ Een heel speciaal gevoel van binnen", "✔ A very special feeling inside"),
    V + "voordeel3": ("✔ 1 extra knapzakvak (dat kon al met munten)", "✔ 1 extra knapsack tab (coins could do that already)"),
    V + "alleen": ("ALLEEN VANDAAG! (en morgen) (en altijd)", "TODAY ONLY! (and tomorrow) (and forever)"),
    V + "kopers": ("%s guhs kochten dit in het afgelopen uur!!", "%s guhs bought this in the last hour!!"),
    V + "parodie": ("(Grapje! Guhs heeft geen echte betalingen. Nergens. Nooit.)",
                    "(Just kidding! Guhs has no real payments. Anywhere. Ever.)"),
    V + "niks": ("Er gebeurt niks. Dat is de grap.", "Nothing happens. That's the joke."),
    # the dialogue after the joke
    V + "naam": ("Winkelguh Snoes", "Shop guh Snoes"),
    V + "plek": ("Opslagwinkeltje", "Storage shop"),
    V + "tekst": ("Hihi, nee is cap. Je kan het gewoon met in-game munten kopen: de eerste uitbreiding kost %s.",
                  "Hehe, no, that's cap. You can just buy it with in-game coins: the first expansion costs %s."),
    V + "kopen": ("Kopen", "Buy"),
    V + "nee_dank": ("Nee, dank je", "No, thank you"),
}


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


EN_OVERLAY = os.path.join("tools", "lang", "en", "c310_vr_u_opslagvip.json")


def _engels():
    _schrijf(EN_OVERLAY, {k: v[1] for k, v in TEKSTEN.items()})


def build(h):
    _engels()
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    _engels()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, data)
    print(f"vadsrijk_u_opslagvip: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
