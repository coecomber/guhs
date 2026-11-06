"""
1.10.0 "Het Vadsrijk", night round T, slice T-KARAKTER: character slots + game modes (Java: feature/vadsrijk/karakter).

Only texts (Dutch + real English): the character screen, the game modes, the switch place rule, the Rustende Ridders-zaal,
the NPC choice, the tip.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_karakter.py
"""
import json
import os

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")

K = "vadsrijk.karakter."
TEKSTEN = {
    # game modes
    K + "modus.normaal": ("Normaal", "Normal"),
    K + "modus.normaal.kort": ("Normaal", "Normal"),
    K + "modus.normaal.uitleg": ("Het gewone avontuur. Flauwvallen kost niets: je wordt wakker bij je laatste kampvuur.",
                                 "The normal adventure. Fainting costs nothing: you wake up at your last campfire."),
    K + "modus.eenvads": ("Eénvads", "One-Vads"),
    K + "modus.eenvads.kort": ("Eénvads", "One-Vads"),
    K + "modus.eenvads.uitleg": ("Eén leven! Val je flauw buiten een stad, dan gaat je ridder met pensioen: hij krijgt een standbeeld in de "
                                 "Rustende Ridders-zaal in Zwamzoet en speelt daarna verder als Normaal (met een gouden ster). Zolang hij leeft: "
                                 "een gouden hartje bij je naam en 10% meer xp.",
                                 "One life! Faint outside a town and your knight retires: a statue in the Rustende Ridders hall in Zwamzoet, "
                                 "then they play on as Normal (with a gold star). While alive: a gold heart by your name and 10% more xp."),
    K + "modus.knuffel": ("Knuffelmodus", "Cuddle mode"),
    K + "modus.knuffel.kort": ("Knuffel", "Cuddle"),
    K + "modus.knuffel.uitleg": ("Rustig het verhaal spelen: monsters doen maar de helft van hun schade bij jou. Je krijgt wel 10% minder xp. "
                                 "Een roze wolkje bij je naam.",
                                 "Play the story at ease: monsters only do half their damage to you. You do get 10% less xp. "
                                 "A pink cloud by your name."),
    # the screen
    K + "scherm.titel": ("Je ridders", "Your knights"),
    K + "scherm.leeg": ("Leeg slot", "Empty slot"),
    K + "scherm.nieuw": ("Nieuw karakter", "New character"),
    K + "scherm.pensioen": ("Rustende Ridder", "Retired Knight"),
    K + "scherm.nooit": ("Nog nooit gespeeld", "Never played yet"),
    K + "scherm.level": ("Level %s", "Level %s"),
    K + "scherm.klasse": ("%s lv %s", "%s lv %s"),
    K + "scherm.actief": ("Je speelt deze", "Playing now"),
    K + "scherm.spelen": ("Spelen", "Play"),
    K + "scherm.hernoem": ("Naam wijzigen", "Rename"),
    K + "scherm.makkelijker": ("Makkelijker...", "Easier..."),
    K + "scherm.makkelijker.tip": ("Zet de spelmodus lager. Dat kan nooit meer terug omhoog!", "Lowers the game mode. It can never go back up!"),
    K + "scherm.uitleg": ("Elke ridder heeft eigen levels, spullen, quests en Vadsmunten. Kleding, rijdieren, huisdieren en de bank deel je.",
                          "Every knight has their own levels, gear, quests and Vadsmunten. Clothes, mounts, pets and the bank are shared."),
    K + "scherm.verder": ("Verder als %s", "Continue as %s"),
    K + "scherm.maak": ("Nieuw karakter (slot %s)", "New character (slot %s)"),
    K + "scherm.naam": ("Naam", "Name"),
    K + "scherm.naam_hint": ("Naam van je ridder", "Your knight's name"),
    K + "scherm.kies_modus": ("Kies een spelmodus:", "Choose a game mode:"),
    K + "scherm.alleen_omlaag": ("Let op: een modus kan later alleen nog makkelijker, nooit moeilijker.",
                                 "Note: a mode can only be made easier later, never harder."),
    K + "scherm.maak_speel": ("Maken en spelen", "Create and play"),
    K + "scherm.alleen_maak": ("Alleen maken", "Only create"),
    K + "scherm.terug": ("Terug", "Back"),
    K + "scherm.opslaan": ("Opslaan", "Save"),
    # the place rule
    K + "niet.plek": ("Wisselen van ridder kan alleen in een stad of bij een kampvuur.", "You can only switch knights in a town or at a campfire."),
    K + "niet.arena": ("Niet midden in een baasgevecht!", "Not in the middle of a boss fight!"),
    K + "niet.scene": ("Wacht even tot het filmpje klaar is.", "Wait until the cutscene is over."),
    # messages
    K + "gemaakt": ("Nieuwe ridder: %s (%s).", "New knight: %s (%s)."),
    K + "gewisseld": ("Je speelt nu als %s.", "You now play as %s."),
    K + "modus_gezet": ("%s speelt nu in %s.", "%s now plays in %s."),
    K + "titel.nieuw": ("Een nieuw avontuur begint!", "A new adventure begins!"),
    K + "titel.terug": ("Welkom terug, ridder!", "Welcome back, knight!"),
    K + "fout.slot": ("Dat slot kan niet (vol of leeg).", "That slot can't be used (taken or empty)."),
    K + "fout.naam": ("Geef je ridder een naam (letters en cijfers, max. 16).", "Give your knight a name (letters and digits, max 16)."),
    K + "fout.modus": ("Een spelmodus kan alleen makkelijker, niet moeilijker.", "A game mode can only get easier, not harder."),
    K + "fout.al": ("Die ridder speel je al.", "You already play that knight."),
    # Eénvads retiring
    K + "pensioen.titel": ("Met pensioen!", "Retired!"),
    K + "pensioen.onder": ("Je ridder krijgt een standbeeld in de Rustende Ridders-zaal", "Your knight gets a statue in the Rustende Ridders hall"),
    K + "pensioen.iedereen": ("%s (%s) gaat met pensioen als Rustende Ridder, op level %s. Er komt een standbeeld in Zwamzoet!",
                              "%s (%s) retires as a Rustende Ridder at level %s. A statue goes up in Zwamzoet!"),
    # the hall
    K + "zaal.naam": ("Rustende Ridders-zaal", "Hall of Rustende Ridders"),
    K + "zaal.onder": ("Ze vielen één keer flauw, en rusten nu in ere.", "They fainted once, and now rest in honour."),
    K + "zaal.plaat": ("lv %s %s - %s", "lv %s %s - %s"),
    # NPC + tip
    K + "optie": ("Ik wil een andere ridder spelen", "I want to play another knight"),
    K + "tip.slots": ("Je kunt tot 3 ridders hebben! Praat met de Grootmeester in Zwamzoet of een kampwacht, of typ /guhs vadsrijk karakter.",
                      "You can have up to 3 knights! Talk to the Grootmeester in Zwamzoet or a campfire guard, or type /guhs vadsrijk karakter."),
}


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


EN_OVERLAY = os.path.join("tools", "lang", "en", "c300_vr_t_karakter.json")


def _engels():
    """The real English goes into the overlay (make_v2.write_lang builds en_us from nl_nl + tools/lang/en)."""
    _schrijf(EN_OVERLAY, {k: v[1] for k, v in TEKSTEN.items()})


def build(h):
    _engels()
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)   # (T-INT: make_v2.lang(key, en, nl))


def _direct():
    _engels()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, data)
    print(f"vadsrijk_t_karakter: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
