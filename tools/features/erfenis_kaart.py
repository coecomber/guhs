"""
1.9 "De Erfenis" (PROOF OF CONCEPT, branch 190-erfenis-kaart): DE WERELDKAART of all bases (Grepolis / Tribal Wars style)
and the living NPC villages. Java: feature/erfenis/kaart (KaartFeature, KaartData, KaartLeven, NpcBouwer, NpcNamen,
KaartServer, KaartPayloads, KaartCommando, client/KaartScherm, client/KaartLand, client/DorpIcoon).

  - only texts: Dutch; en_us gets the same Dutch texts for the POC (no English overlay on purpose, like erfenis.py).
  - the map's land is drawn at runtime (a dynamic texture), so there are no image files.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/erfenis_kaart.py
"""
import json
import os

K = "erfenis.kaart."

TEKSTEN = {
    "key.guhs.wereldkaart": "Wereldkaart",
    K + "titel": "Wereldkaart van De Erfenis",
    K + "ondertitel": "%s dorpjes, %s echte spelers. Slepen = schuiven, scrollen = zoomen",
    K + "laden": "De kaart wordt uitgerold... njeg!",
    K + "ranglijst": "Ranglijst",
    K + "nieuws": "Nieuws",
    K + "nieuws.leeg": "Nog geen nieuws. Iedereen ligt te snurken.",
    K + "punten_uitleg": "Punten = Knusheid",
    K + "naar_mijn_dorp": "Naar mijn dorp",
    K + "zoom_in": "Inzoomen (of scrollen)",
    K + "zoom_uit": "Uitzoomen (of scrollen)",
    K + "jouw_erfenis": "Jouw erfenis",
    K + "erfenis_van": "Erfenis van %s",
    K + "legenda.jij": "Jouw dorp",
    K + "legenda.speler": "Echte spelers",
    K + "legenda.npc": "NPC-dorpjes",
    K + "grootte.1": "hutje",
    K + "grootte.2": "huisje",
    K + "grootte.3": "dorpje",
    K + "grootte.4": "torendorp",
    K + "grootte.5": "kasteel",
    K + "bezoeken": "Bezoeken",
    K + "naar_huis": "Naar huis",
    K + "bezoeken.uitleg": "Ga erheen als bezoeker. Kijken mag, aankomen niet, vads!",
    K + "bovenaanzicht": "Bovenaanzicht",
    K + "bovenaanzicht.uitleg": "Ga erheen en kijk meteen van bovenaf (alleen kijken).",
    K + "aanvallen": "Aanvallen",
    K + "aanvallen.later": "Komt later, njeg! Nooit echte schade.",
    K + "soort.eigen": "Dit is jouw eigen dorp. Vahoeg!",
    K + "soort.npc": "NPC-dorpje van %s",
    K + "soort.speler": "Echte speler: %s",
    K + "paneel.level": "Stadhuis-level %s (%s), %s Knusheid",
    K + "paneel.rang": "Ranglijst #%s van %s, plek %s, %s",
    K + "paneel.nieuw": "Net gesticht of gegroeid!",
    K + "tip.eigenaar": "Eigenaar: %s",
    K + "tip.level": "Stadhuis-level %s (%s)",
    K + "tip.punten": "Knusheid: %s (#%s)",
    K + "tip.eigen": "Jouw dorp",
    K + "tip.npc": "NPC-dorpje",
    K + "tip.speler": "Echte speler",
    K + "tip.klik": "Klik voor meer",
    K + "tijd.zojuist": "zojuist",
    K + "tijd.min": "%s min geleden",
    K + "tijd.uur": "%s uur geleden",
    K + "tijd.dag": "%s dagen geleden",
    # server messages
    K + "weg": "Dat dorpje staat niet (meer) op de kaart. Njeg?",
    K + "geen_dim": "De Erfenis-dimensie is er in deze testversie nog niet, njeg! De kaart werkt wel al.",
    K + "bezocht": "Woesj! Je bent op bezoek. Kijken mag, aankomen niet.",
    K + "bovenaanzicht_later": "Woesj! Je bent op bezoek. Het bovenaanzicht van andermans dorp komt zo, vads.",
    K + "cmd.nieuw": "%s nieuwe NPC-dorpjes gesticht (nu %s).",
    K + "cmd.groei": "%s keer laten groeien: %s dorpjes veranderd.",
    K + "cmd.snel_aan": "NPC-dorpjes groeien nu snel (elke 15 seconden).",
    K + "cmd.snel_uit": "NPC-dorpjes groeien weer rustig (elke 4 minuten).",
    # buildings (with article for the news)
    K + "gebouw.stadhuis": "stadhuis",
    K + "gebouw.molen": "Bouwspulmolen",
    K + "gebouw.schuurtje": "schuurtje",
    K + "gebouw.muur": "muur",
    K + "gebouw.boompje": "boompje",
    K + "gebouw.stadhuis.de": "het stadhuis",
    K + "gebouw.molen.de": "de Bouwspulmolen",
    K + "gebouw.schuurtje.de": "het schuurtje",
    K + "gebouw.muur.de": "de muur",
    K + "gebouw.boompje.de": "het boompje",
    # the news
    K + "nieuws.gesticht": "%s heeft een nieuw dorpje gesticht: %s. Welkom op de kaart, njeg!",
    K + "nieuws.stadhuis": "%s is gegroeid naar stadhuis-level %s! Vahoeg!",
    K + "nieuws.nieuw.molen": "%s heeft een nieuwe Bouwspulmolen in %s. Hij draait al vads.",
    K + "nieuws.nieuw.schuurtje": "%s heeft een nieuw schuurtje in %s. Voor nog meer Bouwspul!",
    K + "nieuws.nieuw.boompje": "%s heeft een nieuw boompje geplant in %s.",
    K + "nieuws.nieuw.muur": "%s heeft %s stukjes muur gezet rond %s. Knus en veilig.",
    K + "nieuws.verbeter": "%s heeft %s in %s naar level %s gebracht.",
    K + "nieuws.verbeter.muur": "%s heeft alle muurtjes van %s opgepoetst naar level %s.",
    K + "nieuws.gezellig.0": "%s geeft een kaasfeestje in %s. Iedereen is vads.",
    K + "nieuws.gezellig.1": "In %2$s is een muisje gesignaleerd. %1$s zegt dat het een heel net muisje was.",
    K + "nieuws.gezellig.2": "%s heeft in %s de grootste pannenkoek ooit gebakken. Njeg!",
    K + "nieuws.gezellig.3": "%s doet een middagdutje. Heel %s is stil. Sssst.",
    K + "nieuws.gezellig.4": "%s heeft in %s een dekentje gebreid zo groot als een stadhuis.",
    K + "nieuws.gezellig.5": "Vahoeg! %s won de knuffelwedstrijd van %s.",
    K + "nieuws.gezellig.6": "%s zwaait naar alle buren vanuit %s.",
    K + "nieuws.gezellig.7": "In %2$s regent het knabbels. %1$s heeft een paraplu omgekeerd.",
    K + "nieuws.gezellig.8": "%s heeft in %s een nieuw liedje verzonnen. Het gaat over kaas.",
    K + "nieuws.gezellig.9": "%s telt alle boompjes van %s. Er zijn er precies genoeg.",
}


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
    print(f"erfenis_kaart: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
