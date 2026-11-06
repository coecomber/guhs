"""
1.10.0 "Het Vadsrijk", round U, U-TOETSEN: the key overview in the pause menu (Esc) inside the Vadsrijk, + /toetsen.
Java: feature/vadsrijk/toetsen (+ client). Texts only (nl + real English).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_toetsen.py
"""
import json
import os

P = "vadsrijk.toetsen."
U = P + "uitleg."


def teksten():
    """key -> (nl, en)"""
    return {
        P + "titel": ("Toetsen in het Vadsrijk", "Keys in the Vadsrijk"),
        P + "voet": ("Klik op een toets om hem te wijzigen", "Click a key to change it"),
        P + "knop": ("Toetsen ▸", "Keys ▸"),
        P + "tip.klik": ("Klik: naar Besturing (toetsen wijzigen)", "Click: open Controls (change keys)"),
        P + "tip.los": ("Niet ingesteld: deze toets doet nu niks", "Not bound: this key does nothing right now"),
        P + "tip.conflict": ("Botst met: %s", "Clashes with: %s"),
        P + "groep.gevecht": ("Gevecht", "Combat"),
        P + "groep.menus": ("Menu's", "Menus"),
        P + "groep.rijden": ("Rijden", "Riding"),
        P + "groep.sociaal": ("Sociaal", "Social"),
        P + "groep.overig": ("Overig", "Other"),
        # short descriptions per key (the mapping's own name is the fallback)
        U + "vr_r": ("Vaardigheid 1", "Skill 1"),
        U + "vr_v": ("Vaardigheid 2", "Skill 2"),
        U + "vr_g": ("Vaardigheid 3", "Skill 3"),
        U + "vr_z": ("Ultieme vaardigheid", "Ultimate skill"),
        U + "vr_sprong": ("Knuffelsprong (ontwijken)", "Hug leap (dodge)"),
        U + "vr_vast": ("Losknuffelen (vastgelopen)", "Unstuck (to a safe spot)"),
        U + "vr_kl_boom": ("Skillboom", "Skill tree"),
        U + "vr_questlog": ("Questlog", "Quest log"),
        U + "vr_boek": ("Verkenningsboek", "Exploration book"),
        U + "vr_rijdieren": ("Rijdieren", "Mounts"),
        U + "vr_huisdieren": ("Huisdieren", "Pets"),
        U + "vr_kledingkast": ("Kledingkast", "Wardrobe"),
        U + "vr_knapzak": ("Knapzak", "Knapsack"),
        U + "vr_ambachten": ("Ambachten", "Crafts"),
        U + "vr_rijdier": ("Opstappen / afstappen", "Mount / dismount"),
        U + "vr_emotes": ("Emote-wiel", "Emote wheel"),
        U + "vr_t_drankje1": ("Drankjesriem 1", "Potion belt 1"),
        U + "vr_t_drankje2": ("Drankjesriem 2", "Potion belt 2"),
        U + "vr_t_drankje3": ("Drankjesriem 3", "Potion belt 3"),
        U + "vr_hudbewerker": ("HUD aanpassen", "Edit HUD"),
        U + "vr_volger": ("Questvolger aan/uit", "Quest tracker on/off"),
        U + "vr_straal": ("Lichtstraal aan/uit", "Light beam on/off"),
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
