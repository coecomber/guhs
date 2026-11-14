"""
1.10.0 "Het Vadsrijk", night round T, slice T-KARAKTER: character slots + game modes (Java: feature/vadsrijk/karakter).

Only texts (Dutch + real English): the character screen, the game modes, the switch place rule, the Rustende Ridders-zaal,
the NPC choice, the tip. U-KARAKTER: fixed slots, the welcome, the inventory button / tab, Werver Wimpel pages 8..10.

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
    K + "modus.normaal.uitleg": ("Het gewone avontuur. Flauwvallen kost niets: je wordt wakker bij je dichtstbijzijnde kampvuur.",
                                 "The normal adventure. Fainting costs nothing: you wake up at your nearest campfire."),
    K + "modus.eenvads": ("Eénvads", "One-Vads"),
    K + "modus.eenvads.kort": ("Eénvads", "One-Vads"),
    K + "modus.eenvads.uitleg": ("Eén leven! Val je flauw buiten een stad, dan gaat je ridder met pensioen: hij krijgt een standbeeld in de "
                                 "Rustende Ridders-zaal in Zwamzoet en rust daar voorgoed (je speelt hem niet meer). Daarna kun je rechts een "
                                 "nieuwe Eénvads-ridder maken. Zolang hij leeft: een gouden hartje bij je naam en 10% meer xp.",
                                 "One life! Faint outside a town and your knight retires: a statue in the Rustende Ridders hall in Zwamzoet, "
                                 "where they rest for good (you don't play them any more). Then the right slot can make a new One-Vads knight. "
                                 "While alive: a gold heart by your name and 10% more xp."),
    K + "modus.knuffel": ("Knuffelmodus", "Cuddle mode"),
    K + "modus.knuffel.kort": ("Knuffel", "Cuddle"),
    K + "modus.knuffel.uitleg": ("Rustig het verhaal spelen: monsters doen maar de helft van hun schade bij jou. Je krijgt wel 10% minder xp. "
                                 "Een roze wolkje bij je naam.",
                                 "Play the story at ease: monsters only do half their damage to you. You do get 10% less xp. "
                                 "A pink cloud by your name."),
    # the screen
    K + "scherm.titel": ("Je ridders", "Your knights"),
    K + "scherm.leeg": ("Leeg slot", "Empty slot"),
    K + "scherm.pensioen": ("Rustende Ridder", "Retired Knight"),
    K + "scherm.nooit": ("Nog nooit gespeeld", "Never played yet"),
    K + "scherm.level": ("Level %s", "Level %s"),
    K + "scherm.klasse": ("%s lv %s", "%s lv %s"),
    K + "scherm.actief": ("Je speelt deze", "Playing now"),
    K + "scherm.spelen": ("Spelen", "Play"),
    K + "scherm.hernoem": ("Naam wijzigen", "Rename"),
    K + "scherm.uitleg": ("Elke ridder heeft eigen levels, spullen, quests en Vadsmunten. Kleding, rijdieren, huisdieren en de bank deel je.", "Every knight has their own levels, gear, quests and Vadsmunten. Clothes, mounts, pets and the bank are shared."),
    K + "scherm.verder": ("Verder als %s", "Continue as %s"),
    K + "scherm.naam": ("Naam", "Name"),
    K + "scherm.naam_hint": ("Naam van je ridder", "Your knight's name"),
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
    K + "titel.nieuw": ("Een nieuw avontuur begint!", "A new adventure begins!"),
    K + "titel.terug": ("Welkom terug, ridder!", "Welcome back, knight!"),
    K + "fout.slot": ("Dat slot kan niet (vol of leeg).", "That slot can't be used (taken or empty)."),
    K + "fout.naam": ("Geef je ridder een naam (letters en cijfers, max. 16).", "Give your knight a name (letters and digits, max 16)."),
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
    K + "optie": ("Karakters: wissel van ridder of maak een nieuwe", "Characters: switch knight or make a new one"),
    K + "tip.slots": ("Je kunt drie ridders hebben: links Normaal, midden Knuffelmodus, rechts Eénvads. Knop ⚔ naast je inventaris!", "You can have three knights: left Normal, middle Cuddle mode, right One-Vads. The ⚔ button next to your inventory!"),
    # ---- U-KARAKTER: three fixed slots (left Normaal, middle Knuffelmodus, right Eénvads), findable without commands
    K + "modus.normaal.regel": ("Het gewone avontuur", "The normal adventure"),
    K + "modus.knuffel.regel": ("Halve schade, 10% minder xp", "Half damage, 10% less xp"),
    K + "modus.eenvads.regel": ("Eén leven, 10% meer xp", "One life, 10% more xp"),
    K + "scherm.nieuw.normaal": ("Maak een Normaal-ridder", "Make a Normal knight"),
    K + "scherm.nieuw.knuffel": ("Maak een Knuffelridder", "Make a Cuddle knight"),
    K + "scherm.nieuw.eenvads": ("Maak een Eénvads-ridder", "Make a One-Vads knight"),
    K + "scherm.nieuw.eenvads_weer": ("Nieuwe Eénvads-ridder", "New One-Vads knight"),
    K + "scherm.maak.normaal": ("Nieuwe ridder (links): Normaal", "New knight (left): Normal"),
    K + "scherm.maak.knuffel": ("Nieuwe ridder (midden): Knuffelmodus", "New knight (middle): Cuddle mode"),
    K + "scherm.maak.eenvads": ("Nieuwe ridder (rechts): Eénvads", "New knight (right): One-Vads"),
    K + "scherm.vast": ("Elk slot heeft een vaste spelmodus. Die hoort bij deze ridder en kan nooit veranderen.",
                        "Every slot has a fixed game mode. It belongs to this knight and can never change."),
    K + "scherm.rust": ("Rust in de Rustende Ridders-zaal in Zwamzoet", "Rests in the Rustende Ridders hall in Zwamzoet"),
    K + "scherm.welkom": ("Welkom in Het Vadsrijk! Kies hoe je wilt spelen. Je hebt drie ridders, elk met een vaste spelmodus: links "
                          "Normaal, in het midden Knuffelmodus, rechts Eénvads. Je eigen ridder staat links al klaar.",
                          "Welcome to Het Vadsrijk! Choose how you want to play. You have three knights, each with a fixed game mode: left "
                          "Normal, middle Cuddle mode, right One-Vads. Your own knight is ready on the left."),
    K + "scherm.uitleg_kort": ("Wisselen kan in een stad of bij een kampvuur. Kleding, rijdieren en huisdieren deel je.",
                               "Switch in a town or at a campfire. Clothes, mounts and pets are shared."),
    K + "tab": ("Ridders", "Knights"),
    K + "knop.uitleg": ("Karakters: je ridders (wisselen of een nieuwe maken)", "Characters: your knights (switch or make a new one)"),
    K + "fout.rust": ("Die ridder rust in de Rustende Ridders-zaal. Maak rechts een nieuwe Eénvads-ridder!",
                      "That knight rests in the Rustende Ridders hall. Make a new One-Vads knight on the right!"),
    K + "fout.rust_actief": ("Wissel eerst naar een andere ridder.", "Switch to another knight first."),
    K + "na_rust": ("Je speelt verder als %s. Je Eénvads-ridder staat nu in de Rustende Ridders-zaal; rechts kun je een nieuwe maken.",
                    "You play on as %s. Your One-Vads knight now stands in the Rustende Ridders hall; you can make a new one on the right."),
    K + "welkom.chat": ("Je ridders vind je altijd terug: de knop ⚔ links van je inventaris, het tabblad Ridders, shift + rechtsklik op een "
                        "kampvuur, of praat met de Grootmeester, de Rekruteerder of een kampwacht.",
                        "You can always find your knights: the ⚔ button left of your inventory, the Knights tab, shift + right-click a "
                        "campfire, or talk to the Grootmeester, the Rekruteerder or a camp guard."),
    K + "tip.kampvuur": ("Shift + rechtsklik op een kampvuur opent je ridders: wissel van ridder of maak een nieuwe.",
                         "Shift + right-click a campfire to open your knights: switch or make a new one."),
    K + "optie.rekruteerder": ("Karakters: hoe zat dat met die drie ridders?", "Characters: what about those three knights?"),
    # Werver Wimpel at the portal (Werver.GESPREK pages 8..10: npc, jij, npc)
    "vadsrijk.npc.werver.gesprek.8": ("O ja, nog iets! In Het Vadsrijk mag je drie ridders hebben, elk met een vaste manier van spelen. "
                                      "Links Normaal, in het midden Knuffelmodus: monsters doen je maar half zoveel pijn. En rechts Eénvads: "
                                      "één leven, en val je flauw, dan krijg je een standbeeld!",
                                      "Oh, one more thing! In Het Vadsrijk you may have three knights, each with a fixed way to play. Left "
                                      "Normal, in the middle Cuddle mode: monsters only hurt you half as much. And right One-Vads: one life, "
                                      "and if you faint, you get a statue!"),
    "vadsrijk.npc.werver.gesprek.9": ("Een standbeeld? En hoe kies ik dan?", "A statue? And how do I choose?"),
    "vadsrijk.npc.werver.gesprek.10": ("Na het welkomstfilmpje krijg je het scherm vanzelf. Later vind je je ridders met de knop ⚔ naast je "
                                       "inventaris, met shift + rechtsklik op een kampvuur, of bij een kampwacht. Wisselen kan in een stad of "
                                       "bij een kampvuur. Vahoeg, rekruut!",
                                       "After the welcome scene the screen opens by itself. Later you find your knights with the ⚔ button next "
                                       "to your inventory, with shift + right-click on a campfire, or at a camp guard. You can switch in a town "
                                       "or at a campfire. Vahoeg, recruit!"),
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
