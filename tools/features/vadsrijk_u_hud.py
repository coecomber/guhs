"""
1.10.0 "Het Vadsrijk", round U, U-HUD: party frames, emotes (wheel + /emote), the HUD editor.
Java: feature/vadsrijk/hud (+ client), mixin client/UHudEmotePoseMixin. Texts only (nl + real English).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_hud.py
"""
import json
import os

P = "vadsrijk.uhud."


def teksten():
    """key -> (nl, en)"""
    t = {
        "key.guhs.vr_emotes": ("Emotes (ingedrukt houden)", "Emotes (hold)"),
        # emotes
        P + "emote.zwaai": ("Zwaaien", "Wave"),
        P + "emote.knuffel": ("Knuffelen", "Hug"),
        P + "emote.dans": ("Dansen", "Dance"),
        P + "emote.juich": ("Juichen", "Cheer"),
        P + "emote.zit": ("Zitten", "Sit"),
        P + "emote.slaap": ("Slapen", "Sleep"),
        P + "emote.lach": ("Lachen", "Laugh"),
        P + "emote.wijs": ("Wijzen", "Point"),
        P + "emote.hartje": ("Hartje", "Heart"),
        P + "emote.wiel": ("Emotes", "Emotes"),
        P + "emote.kies": ("Kies...", "Pick..."),
        P + "emote.klik": ("Klik op een emote (Esc = toch niet)", "Click an emote (Esc = never mind)"),
        P + "emote.even_wachten": ("Even wachten...", "Wait a moment..."),
        P + "emote.nu_niet": ("Dat kan nu even niet.", "Not right now."),
        P + "emote.knuffel.jij": ("%s knuffelt je!", "%s hugs you!"),
        P + "emote.knuffel.ander": ("Je knuffelt %s!", "You hug %s!"),
        P + "emote.knuffel.niemand": ("Niemand in de buurt... je knuffelt de lucht.", "Nobody nearby... you hug the air."),
        P + "emote.onbekend": ("Die emote ken ik niet: %s", "Unknown emote: %s"),
        # party frames
        P + "party.level": ("Nv %s", "Lv %s"),
        P + "party.offline": ("offline", "offline"),
        P + "party.flauw": ("Flauwgevallen!", "Fainted!"),
        P + "party.elders": ("elders", "elsewhere"),
        # the HUD editor
        P + "bewerker": ("HUD aanpassen", "Edit HUD"),
        P + "bewerker.knop": ("HUD aanpassen", "Edit HUD"),
        P + "bewerker.alles_terug": ("Alles terug", "Reset all"),
        P + "bewerker.zeker": ("Echt alles terug?", "Really reset all?"),
        P + "bewerker.klaar": ("Klaar", "Done"),
        P + "bewerker.uitleg": ("Slepen = verplaatsen · scrollen = groter/kleiner · rechtsklik = terug · Shift = niet vastklikken",
                                "Drag = move · scroll = bigger/smaller · right click = reset · Shift = no snapping"),
        P + "bewerker.deel": ("%s (%s%%)", "%s (%s%%)"),
    }
    delen = {
        "balken": ("Vads- en energiebalk", "Vads and energy bars"),
        "xp": ("Ervaringsbalk", "Experience bar"),
        "skills": ("Vaardigheden", "Skills"),
        "info": ("Level, munten en opgevrolijkt", "Level, coins and cheered up"),
        "baasbalk": ("Baasbalk", "Boss bar"),
        "tip": ("Tips", "Hints"),
        "party": ("Party", "Party"),
        "buffs": ("Buffs en debuffs", "Buffs and debuffs"),
        "doelframe": ("Doelwit", "Target"),
        "gietbalk": ("Jouw spreukbalk", "Your cast bar"),
        "baasgiet": ("Spreukbalk van de baas", "Boss cast bar"),
        "drankjesriem": ("Drankjesriem", "Potion belt"),
        "questvolger": ("Questvolger", "Quest tracker"),
        "baasspreuk": ("Baas laadt een aanval", "Boss charging an attack"),
        "munten": ("Munten erbij", "Coins gained"),
        "rijdier": ("Rijdier", "Mount"),
        "wind": ("Wind (zeilbootje)", "Wind (sailing boat)"),
    }
    for k, v in delen.items():
        t[P + "deel." + k] = v
    return t


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
