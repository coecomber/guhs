"""
1.10.0 "Het Vadsrijk", night round T, T-OPSLAG: knapzak (per character), materiaaltas + bank (account-wide), HUD additions
(buff row, target frame, cast bars). Java: feature/vadsrijk/opslag (+ client).
Texts (nl + real English) and the item tag guhs:vr_materiaal (empty list: the first materials are registered in Java,
OpslagRegistry; data packs and other generators may add to it).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_opslag.py
"""
import json
import os

P = "vadsrijk.opslag."


def teksten():
    """key -> (nl, en)"""
    return {
        "key.guhs.vr_knapzak": ("Knapzak", "Knapsack"),
        # screens
        P + "knapzak": ("Knapzak", "Knapsack"),
        P + "bank": ("Mijn kluis", "My vault"),
        P + "tas": ("Materiaaltas", "Material bag"),
        P + "tab": ("Vak %s van %s", "Tab %s of %s"),
        P + "tab.naar": ("Naar vak %s (of Ctrl+%s)", "Go to tab %s (or Ctrl+%s)"),
        P + "tab.dicht": ("Dit vak is nog dicht.", "This tab is still locked."),
        P + "tab.kopen": ("Klik om dit vak te kopen voor %s", "Click to buy this tab for %s"),
        P + "tab.bevestig": ("Klik nog een keer: kopen voor %s!", "Click once more: buy for %s!"),
        P + "bank.eerst_vorige": ("Koop eerst het vak ervoor.", "Buy the tab before it first."),
        P + "knapzak.bron.zone1": ("Maak het verhaal van zone 1 af", "Finish the zone 1 story"),
        P + "knapzak.bron.zone2": ("Maak het verhaal van zone 2 af", "Finish the zone 2 story"),
        P + "knapzak.bron.kopen": ("Koop een vak voor %s", "Buy a tab for %s"),
        P + "knapzak.bron.later": ("Een beloning verderop in het Vadsrijk", "A reward further on in the Vadsrijk"),
        P + "knapzak.nieuw_vak": ("Je knapzak heeft er een vak bij! Nu %s vakken (toets I).", "Your knapsack got a new tab! Now %s tabs (key I)."),
        P + "knapzak.gekocht": ("Gekocht: een extra vak in je knapzak!", "Bought: an extra knapsack tab!"),
        P + "bank.gekocht": ("Gekocht: een extra vak in je kluis! Nu %s vakken.", "Bought: an extra vault tab! Now %s tabs."),
        P + "te_weinig": ("Te weinig Vadsmunten (nodig: %s).", "Not enough Vadsmunten (needed: %s)."),
        P + "alleen_rijk": ("Dat kan alleen in het Vadsrijk.", "That only works in the Vadsrijk."),
        P + "optie.bank": ("Naar mijn kluis", "To my vault"),
        # buttons
        P + "knop.sorteer": ("Sorteren", "Sort"),
        P + "knop.sorteer.uitleg": ("Zet dit vak netjes op een rij en voegt gelijke stapels samen.", "Tidies this tab and merges equal stacks."),
        P + "knop.materialen": ("Materialen ➜ tas", "Materials ➜ bag"),
        P + "knop.materialen.uitleg": ("Alle materialen uit je inventaris gaan in je Materiaaltas.", "All materials from your inventory go into your Material bag."),
        P + "knop.tas": ("Materiaaltas", "Material bag"),
        P + "knop.tas.uitleg": ("Je Materiaaltas: al je materialen, zo veel als je wilt.", "Your Material bag: all your materials, as many as you like."),
        P + "knop.knapzak": ("Knapzak", "Knapsack"),
        P + "knop.knapzak.uitleg": ("Je knapzak (toets I): extra vakken voor je spullen.", "Your knapsack (key I): extra tabs for your things."),
        P + "knop.alles": ("Alles opbergen", "Store all"),
        P + "knop.alles.uitleg": ("Alle materialen uit je inventaris gaan in de tas.", "All materials from your inventory go into the bag."),
        P + "knop.auto.aan": ("Oprapen: aan", "Pick up: on"),
        P + "knop.auto.uit": ("Oprapen: uit", "Pick up: off"),
        P + "knop.auto.uitleg": ("Aan: materialen die je opraapt gaan meteen in de tas, ook als je inventaris vol is.",
                                 "On: materials you pick up go straight into the bag, even with a full inventory."),
        # the bag
        P + "tas.zoek": ("Zoeken...", "Search..."),
        P + "tas.samen": ("%s stuks, %s soorten", "%s items, %s kinds"),
        P + "tas.aantal": ("In de tas: %s", "In the bag: %s"),
        P + "tas.klik": ("Klik: een stapel. Rechts: de helft. Shift: naar je inventaris.", "Click: a stack. Right: half. Shift: to your inventory."),
        P + "tas.leeg": ("Nog leeg. Materialen die je opraapt komen hier vanzelf in.", "Still empty. Materials you pick up land here by themselves."),
        P + "tas.niets_gevonden": ("Niets gevonden.", "Nothing found."),
        P + "tas.auto_aan": ("Opgeraapte materialen gaan vanzelf in de tas.", "Picked-up materials go into the bag by themselves."),
        P + "tas.auto_uit": ("Oprapen in de tas staat uit.", "Picking up into the bag is off."),
        P + "tas.erin": ("+%s %s in je Materiaaltas (%s)", "+%s %s into your Material bag (%s)"),
        P + "tas.opgeborgen": ("%s materialen in je Materiaaltas gestopt.", "%s materials put into your Material bag."),
        P + "tas.geen_materiaal": ("Dat is geen materiaal: dat past niet in de tas.", "That is not a material: it does not fit in the bag."),
        # first-time hints
        P + "tip.knapzak.titel": ("Je knapzak", "Your knapsack"),
        P + "tip.knapzak": ("Extra vakken voor je spullen, overal in het Vadsrijk (toets I). Meer vakken krijg je door het verhaal, of koop er een.",
                            "Extra tabs for your things, anywhere in the Vadsrijk (key I). You get more tabs through the story, or buy one."),
        P + "tip.tas.titel": ("De Materiaaltas", "The Material bag"),
        P + "tip.tas": ("Materialen (steentjes, draad, kristallen...) gaan vanzelf in je Materiaaltas. Geen stapel-limiet! De Smid pakt ze er zelf uit.",
                        "Materials (stones, thread, crystals...) go into your Material bag by themselves. No stack limit! The Smith takes them out himself."),
        P + "tip.bank.titel": ("Je kluis", "Your vault"),
        P + "tip.bank": ("Je kluis is van al je ridders samen en staat in elke stad bij de bankier.",
                         "Your vault is shared by all your knights and waits at the banker in every town."),
        # HUD
        P + "doel.level": ("Nv %s", "Lv %s"),
        P + "giet.onderbroken": ("Onderbroken!", "Interrupted!"),
        P + "giet.losknuffelen": ("Losknuffelen...", "Unsticking..."),
        P + "giet.test": ("Proefspreuk", "Test spell"),
        P + "giet.baastest": ("Grote Plonsstamp", "Big Splash Stomp"),
    }


def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _tag(root):
    _schrijf_json(os.path.join(root, "data", "guhs", "tags", "item", "vr_materiaal.json"), {"replace": False, "values": []})


def build(h):
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)   # (T-INT: make_v2.lang(key, en, nl))
    _tag(os.path.join("src", "main", "resources"))


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for k, v in t.items():
            data[k] = v[i]
        _schrijf_json(p, data)
    _tag(os.path.join("src", "main", "resources"))


if __name__ == "__main__":
    _direct()
