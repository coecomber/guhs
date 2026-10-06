"""
bbq2 (tech-machines): every Dutch text of the guh machines (names, the line on the item, the hover readout, the screens,
the Bouwtekening, the advancements). tech_machines.py writes them; the numbers of vadskracht come from VadsGetallen.java.
"""
from features import vadskracht

M = "gui.guhs.techmachine."


def teksten():
    vk = vadskracht.getal
    return {
        # --- the blocks and what the item says ---
        "block.guhs.vadsmolen": "Vadsmolen",
        "block.guhs.vadsmolen.lore": "Het grote zusje van het guh-molentje: haar wieken draaien op vadskracht in plaats van op wind, dus ze maalt "
                                     f"altijd even hard. Knabbelgraan, botten, suikerriet en meer. Vraagt {vk('MOLEN')} vadskracht.",
        "block.guhs.oogster": "Oogster",
        "block.guhs.oogster.lore": "Maait het rijpe gewas op het veld van 5 bij 5 voor zijn snoet en plant het meteen weer in. "
                                   f"Vraagt {vk('OOGSTER')} vadskracht. Njeg, nooit meer bukken!",
        "block.guhs.knabbelaar": "Knabbelaar",
        "block.guhs.knabbelaar.lore": "Knabbelt het blok voor zijn snoet weg, ongeveer één per twee tellen. Hij bijt zo hard als een ijzeren "
                                      "houweel, maar van kisten, machines en andermans huisje blijft hij netjes af. "
                                      f"Vraagt {vk('KNABBELAAR')} vadskracht.",
        "block.guhs.neerzetter": "Neerzetter",
        "block.guhs.neerzetter.lore": "Zet de blokken uit zijn buikje één voor één neer voor zijn snoet. Zaadjes en zaailingen ook! "
                                      f"Vraagt {vk('NEERZETTER')} vadskracht.",
        "block.guhs.knutselmachine": "Knutselmachine",
        "block.guhs.knutselmachine.lore": "Leg er een Bouwtekening in en hij knutselt precies dat, van alles wat buizen, guhs en jij erin "
                                          f"stoppen. Tik tik tik, klaar. Vraagt {vk('KNUTSELMACHINE')} vadskracht.",
        "block.guhs.tekentafel": "Tekentafel",
        "block.guhs.tekentafel.lore": "Leg een recept op tafel, geef hem een vel en hij tekent er een Bouwtekening van. Tekenen kost niks: "
                                      "alles van tafel krijg je terug. Geen vadskracht nodig, alleen een scherp potloodje.",
        "block.guhs.plantagebak": "Plantagebak",
        "block.guhs.plantagebak.lore": "Een bak van 3 bij 3 vol aarde. Stop er een zaailing of een zwammetje in en binnen een minuut staat er "
                                       "een boom. Klik met een bijl op de bak en de hele boom gaat in één keer om. "
                                       f"Vraagt {vk('PLANTAGEBAK')} vadskracht.",
        "block.guhs.techmachine_plantagebak_deel": "Plantagebak",
        # --- the Bouwtekening ---
        "item.guhs.bouwtekening": "Bouwtekening",
        "item.guhs.bouwtekening.leeg": "Lege bouwtekening",
        "item.guhs.bouwtekening.van": "Bouwtekening: %s",
        "item.guhs.bouwtekening.leeg.lore": "Nog niks op getekend. Leg hem op een Tekentafel bij een recept.",
        "item.guhs.bouwtekening.maakt": "Maakt %s× %s",
        "item.guhs.bouwtekening.nodig": "Daar is voor nodig:",
        "item.guhs.bouwtekening.regel": "  %s× %s",
        # --- the hover readout (under the vadskracht lines) ---
        M + "vol": "Vol! Haal eruit wat erin zit, dan gaat hij verder.",
        M + "mag_niet": "Hier mag hij niks veranderen: dit is beschermd of van iemand anders. Njeg.",
        M + "oogster.veld": "Oogst het veld van %s bij %s voor zijn snoet",
        M + "knabbelaar.lust_niet": "%s lust hij niet",
        M + "neerzetter.leeg": "Zijn buikje is leeg: stop er blokken in",
        M + "neerzetter.bezet": "Er staat al iets voor zijn snoet",
        M + "neerzetter.past_niet": "Dit blok kan hier niet staan",
        M + "vadsmolen.leeg": "Niks te malen: stop er knabbelgraan in",
        M + "knutselmachine.geen_tekening": "Geen Bouwtekening: hij weet niet wat hij moet knutselen",
        M + "knutselmachine.maakt": "Knutselt %s× %s",
        M + "knutselmachine.onbekend": "Dit recept kent hij niet (meer). Teken het opnieuw.",
        M + "knutselmachine.mist": "Er ontbreekt nog %s× %s",
        M + "plantagebak.leeg": "De bak is leeg: stop er een zaailing of een zwammetje in",
        M + "plantagebak.groeit": "Hier groeit %s: nog %s tellen",
        M + "plantagebak.wil_niet": "%s wil niet groeien. Is er plek boven de bak? Sommige soorten groeien alleen met z'n vieren: "
                                    "stop er dan vier in.",
        M + "plantagebak.boom": "De boom staat! Hak hem om, of klik met een bijl op de bak: dan gaat hij in één keer.",
        # --- the screens ---
        M + "scherm.geen_kracht": "Geen vadskracht: het snoetje slaapt",
        M + "scherm.geen_tekening": "Leg links een Bouwtekening",
        M + "scherm.mist": "Er ontbreekt nog iets",
        M + "scherm.vol": "Vol! Haal de knutsels eruit",
        M + "scherm.onbekend": "Dit recept kent hij niet",
        M + "scherm.wil_niet": "Wil niet groeien. Plek? Vier?",
        M + "scherm.boom": "De boom staat! Hakken maar",
        M + "scherm.zaailing": "Stop er een zaailing in",
        M + "tekentafel.leg": "Leg een recept op het rooster",
        M + "tekentafel.vel": "Leg er een Bouwtekening bij",
        M + "tekentafel.onbekend": "Dit recept kent de tafel niet",
    }


# the visible advancements of the tab Guh-technologie: name -> (parent, icon, frame, title, text).
# Java grants them (TechBlockEntity.beloon / TekentafelMenu) together with the hidden guhs:quest/tech_machines_<name>.
MIJLPALEN = {
    "gemalen": ("root", "guhs:vadsmolen", "task", "Wieken op vadskracht", "Laat een Vadsmolen iets malen"),
    "geoogst": ("root", "guhs:oogster", "task", "Nooit meer bukken", "Laat een Oogster je veld maaien"),
    "geknabbeld": ("root", "guhs:knabbelaar", "task", "Knabbel, knabbel, weg", "Laat een Knabbelaar een blok wegknabbelen"),
    "neergezet": ("tech_machines_geknabbeld", "guhs:neerzetter", "task", "Zet maar neer", "Laat een Neerzetter een blok neerzetten"),
    "tekening": ("root", "guhs:tekentafel", "task", "Getekend: guh", "Teken een recept op een Bouwtekening"),
    "geknutseld": ("tech_machines_tekening", "guhs:knutselmachine", "goal", "Het knutselt vanzelf",
                   "Laat een Knutselmachine maken wat op je Bouwtekening staat"),
    "boom": ("root", "guhs:plantagebak", "goal", "Een bos in een bak", "Laat in een Plantagebak een boom groeien"),
}
