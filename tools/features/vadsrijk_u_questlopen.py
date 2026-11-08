"""
1.10.0 "Het Vadsrijk", round U, U-QUESTLOPEN: the admin screen that clicks through the main line of zones 1-6 (OP only,
/guhs vadsrijk questlopen). Java: feature/vadsrijk/questlopen (+ client). Texts only (nl + real English).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_questlopen.py
"""
import json
import os

P = "vadsrijk.questlopen."


def teksten():
    """key -> (nl, en)"""
    return {
        P + "titel": ("Questlopen (admin)", "Quest walker (admin)"),
        P + "karakter": ("karakter: %s", "character: %s"),
        P + "let_op": ("Let op!", "Careful!"),
        P + "waarschuwing": (
            "Dit scherm verandert de voortgang van je HUIDIGE karakter (%s): quests, level, verhaalvlaggen, beloningen en "
            "rijdieren gaan echt mee. Alleen bedoeld om het verhaal snel te testen. Andere karakters blijven zoals ze zijn.",
            "This screen changes the progress of your CURRENT character (%s): quests, level, story flags, rewards and mounts "
            "really change. Only meant for testing the story quickly. Other characters stay as they are."),
        P + "knop.begrepen": ("Begrepen", "Understood"),
        P + "zone": ("Zone %s", "Zone %s"),
        P + "soort.0": ("Begin", "Start"),
        P + "soort.1": ("Aannemen", "Take"),
        P + "soort.2": ("Stap", "Step"),
        P + "soort.3": ("Inleveren", "Hand in"),
        P + "waar": ("Waar:", "Where:"),
        P + "wie": ("Wie:", "Who:"),
        P + "lv": ("Min. level %s · verwacht %s (jij: lv %s, klasse %s)", "Min. level %s · expected %s (you: lv %s, class %s)"),
        P + "lv.klasse": ("Min. klasselevel %s · verwacht %s (jij: lv %s, klasse %s)", "Min. class level %s · expected %s (you: lv %s, class %s)"),
        P + "baas": ("Baasgevecht! Kies hieronder.", "Boss fight! Choose below."),
        P + "opening": ("Aankomst in het Knuffelkamp", "Arrival in the Knuffelkamp"),
        P + "opening.wat": ("de openingsfilm", "the opening film"),
        P + "wat.neem": ("neem de quest aan", "take the quest"),
        P + "wat.lever": ("lever de quest in", "hand in the quest"),
        P + "vecht": ("Vecht maar! (Te zwaar? /guhs vadsrijk vol, of /guhs vadsrijk questlopen verder -> Laat hem verslaan)",
                      "Fight! (Too hard? /guhs vadsrijk vol, or /guhs vadsrijk questlopen verder -> Let it be beaten)"),
        # state lines
        P + "melding.klaar": ("De hele hoofdlijn is klaar!", "The whole main line is done!"),
        P + "melding.wacht": ("Even wachten: er speelt nog een scène of gesprek.", "Wait a moment: a scene or dialogue is still playing."),
        P + "melding.geen_baas": ("Geen echte baas gevonden: de verslagen-haak is afgevuurd.", "No real boss found: the defeat hook was fired."),
        P + "melding.immuun": ("De baas liet zich niet verslaan: de verslagen-haak is afgevuurd.", "The boss could not be beaten: the defeat hook was fired."),
        P + "melding.terug": ("Teruggezet. Beloningen, items en verhaalvlaggen blijven wel.", "Set back. Rewards, items and story flags do stay."),
        P + "melding.spoelen": ("Vooruitspoelen (stil)... stap %s -> %s", "Fast-forwarding (muted)... step %s -> %s"),
        P + "melding.gesprongen": ("Gesprongen! Alle eerdere stappen zijn gedaan.", "Jumped! All earlier steps are done."),
        P + "melding.geen_scene": ("Nog geen scène gespeeld bij deze stap.", "No scene played at this step yet."),
        P + "melding.reset": ("Questlog van dit karakter gewist (ook zijquests, geheimen en verhaalvlaggen). Level en items blijven.",
                              "Quest log of this character wiped (side quests, secrets and story flags too). Level and items stay."),
        P + "melding.hangt": ("Een scène of gesprek duurde te lang; het scherm is terug.", "A scene or dialogue took too long; the screen is back."),
        P + "melding.vast": ("Deze stap telt niet mee, ook niet via de haak. Probeer Spring naar de volgende stap.",
                             "This step does not count, not even through the hook. Try jumping to the next step."),
        P + "fase.naar": ("Op weg...", "On the way..."),
        P + "fase.wacht": ("Bezig: wacht tot de scène klaar is...", "Busy: waiting for the scene to end..."),
        P + "fase.aankomst": ("Aangekomen, even kijken of er iets gebeurt...", "Arrived, checking whether something happens..."),
        P + "fase.baas_in": ("De arena in: intro...", "Into the arena: intro..."),
        P + "fase.versla": ("De baas wordt verslagen...", "The boss is being beaten..."),
        P + "fase.vecht": ("Je vecht zelf. Het scherm komt terug als de baas verslagen is.", "You are fighting. The screen comes back when the boss is beaten."),
        P + "fase.spoel": ("Vooruitspoelen...", "Fast-forwarding..."),
        P + "fase.herhaal": ("Scène wordt herhaald...", "Replaying the scene..."),
        # buttons
        P + "knop.volgende": ("Volgende ▶", "Next ▶"),
        P + "knop.terug": ("◀ Terug", "◀ Back"),
        P + "knop.zelf": ("Zelf vechten", "Fight myself"),
        P + "knop.versla": ("Laat hem verslaan", "Let it be beaten"),
        P + "knop.spring": ("Spring naar...", "Jump to..."),
        P + "knop.spring.naar": ("Spring naar %s", "Jump to %s"),
        P + "knop.stop": ("Stop", "Stop"),
        P + "knop.herhaal": ("Herhaal scène", "Replay scene"),
        P + "knop.teleport": ("Alleen teleporteren", "Teleport only"),
        # W-SPEL: the way out of a stuck step
        P + "knop.overslaan": ("Sla stap over", "Sla stap over"),
        P + "knop.veilig": ("Naar veilige plek", "Naar veilige plek"),
        P + "tip.overslaan": ("Deze stap telt meteen mee (via de haak, anders de teller), ook midden in een gevecht. Daarna ga je naar de plek van de volgende stap.",
                              "Deze stap telt meteen mee (via de haak, anders de teller), ook midden in een gevecht. Daarna ga je naar de plek van de volgende stap."),
        P + "tip.veilig": ("Stopt wat er loopt en zet je terug op de laatste rustige plek buiten een arena (anders bij de huidige stap of in het Knuffelkamp).",
                           "Stopt wat er loopt en zet je terug op de laatste rustige plek buiten een arena (anders bij de huidige stap of in het Knuffelkamp)."),
        P + "melding.overgeslagen": ("Stap overgeslagen: hij telt mee.", "Stap overgeslagen: hij telt mee."),
        P + "melding.veilig": ("Terug op een veilige plek.", "Terug op een veilige plek."),
        P + "knop.reset": ("Reset hoofdlijn", "Reset main line"),
        P + "knop.reset.zeker": ("Zeker? Klik nog eens", "Sure? Click again"),
        P + "knop.level.min": ("Level: minimum", "Level: minimum"),
        P + "knop.level.verwacht": ("Level: verwacht", "Level: expected"),
        P + "knop.adv.aan": ("Advancements: aan", "Advancements: on"),
        P + "knop.adv.uit": ("Advancements: uit", "Advancements: off"),
        P + "knop.rijdier.aan": ("Rijdier/huisdier: aan", "Mount/pet: on"),
        P + "knop.rijdier.uit": ("Rijdier/huisdier: uit", "Mount/pet: off"),
        P + "knop.menu": ("Questlopen (admin)", "Quest walker (admin)"),
        P + "tip.level": ("Naar welk level Volgende je zet: het minimum dat de stap vraagt, of het level dat een gewone speler hier ongeveer heeft.",
                          "Which level Next sets you to: the minimum the step needs, or the level a normal player has around here."),
        P + "tip.adv": ("Ook de advancement-quests van je klasse meenemen (lv 17: de specialisatiekeuze komt echt in beeld, lv 35, lv 58).",
                        "Also do your class's advancement quests (lv 17: the specialisation choice really pops up, lv 35, lv 58)."),
        P + "tip.rijdier": ("Ook de rijdier-quests en de zijquests met een huisdier als beloning meenemen.",
                            "Also do the mount quests and the side quests that reward a pet."),
        P + "tip.spring": ("Klik eerst een regel in de lijst. Verder = alle stappen ervoor worden stil gedaan (met hun vlaggen); terug = de quests worden teruggezet.",
                           "Click a row in the list first. Forward = every earlier step is done silently (with its flags); back = the quests are set back."),
    }


def build(h):
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for k, v in t.items():
            data[k] = v[i]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    _direct()
