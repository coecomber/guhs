"""
einde2 (the Guheinde expansion), slice biomeguhs: the six tameable guh variants of the outer islands and their tricks.
Java: nl.juiced.guhs.feature.einde2.biomeguhs (E2BiomeguhsFeature).

Skeleton stub: the biomeguhs slice replaces this module's content (it stays in features.FEATURES at this place).
For now: six plain stand-in furs (so guh_<id>.png and the sleeping eyes exist) and stand-in Guhdex texts.
"""
import features.einde2_kit as kit

GUHS = {  # variant id: (Dutch name, where, fur colour)
    "korstguh": ("Korstguh", "Gatenkaasheuvels", (214, 170, 90)), "duinguh": ("Duinguh", "Korstwoestijn", (232, 204, 150)),
    "kruimelguh": ("Kruimelguh", "Kruimelwolken", (246, 236, 214)), "schimmelguh": ("Schimmelguh", "Schimmelkaasvlakte", (170, 200, 190)),
    "kristalguh": ("Kristalguh", "Kristalkaasklippen", (250, 214, 110)), "koorguh": ("Koorguh", "Knabbelkoorbos", (196, 140, 214)),
}


def build(h):
    for id, (naam, waar, _fur) in GUHS.items():
        kit.pagina(h, id, naam, f"Alleen in de {waar} van het Guheinde",
                   f"Een guh van de buiteneilanden van het Guheinde. Je vindt hem in de {waar}. Njeg, meer weten we nog niet.", variant=True)


def variants(rng, v):
    # (plain furs; the real ones are painted with kit.rng(<variant id>), never with the rng handed in here)
    return {id: (fur, {}) for id, (_naam, _waar, fur) in GUHS.items()}
