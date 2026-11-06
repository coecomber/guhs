"""
1.10 "Het Vadsrijk", round U integration (V-INT): texts of the glue between the round-U slices (Java: feature/vadsrijk/vint):
the Knuffelbond + Mijn huis tabs of the Vadsrijk-menu, the HUD / Specialisatie sub-tabs, the spec line in the Karakter tab,
the menu key in the key overview.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_v_int.py
"""
import json
import os

V = "vadsrijk.vint."


def teksten():
    """key -> (nl, en)."""
    return {
        "vadsrijk.menu.tab.bond": ("Knuffelbond", "Knuffelbond"),
        "vadsrijk.menu.tab.bond.uitleg": ("Je bond (guild): leden, weektaken, perks en jullie kasteel", "Your bond (guild): members, weekly tasks, perks and your castle"),
        "vadsrijk.menu.tab.huis": ("Mijn huis", "My house"),
        "vadsrijk.menu.tab.huis.uitleg": ("Het Knuffelkwartier: je eigen huis in een buurtje", "The Knuffelkwartier: your own house in a neighbourhood"),
        V + "tab.hud": ("HUD aanpassen", "Edit HUD"),
        V + "tab.specs": ("Specialisatie", "Specialisation"),
        V + "karakter.spec": ("Specialisatie: %s", "Specialisation: %s"),
        V + "karakter.spec.geen": ("Specialisatie: nog niet gekozen (vanaf level 17)", "Specialisation: not chosen yet (from level 17)"),
        V + "toets.menu": ("Vadsrijk-menu (alle tabbladen)", "Vadsrijk menu (all tabs)"),
        V + "bond.titel": ("Je Knuffelbond", "Your Knuffelbond"),
        V + "bond.uitleg.1": ("Een Knuffelbond is een club van spelers. Samen verdien je bondsxp met alles wat je toch al doet: monsters, quests, levels en ambachten.",
                              "A Knuffelbond is a club of players. Together you earn bond xp with everything you already do: monsters, quests, levels and professions."),
        V + "bond.uitleg.2": ("Lid worden kan al vanaf zone 1: kijk op een Bondenbord in elke stad. Zelf een bond oprichten kan pas in Hoogvads, bij Bondsklerk Zegeltje.",
                              "You can join from zone 1: look at a bond board in every town. Founding your own bond is only possible in Hoogvads, at clerk Zegeltje."),
        V + "bond.uitleg.3": ("Elke bond krijgt een kasteel in de Bondenvallei, dat jullie samen uitbouwen (niveau 1 t/m 5), met een kluis, trofeeënzaal en werkplaatsen.",
                              "Every bond gets a castle in the Bondenvallei that you build up together (level 1 to 5), with a vault, trophy hall and workshops."),
        V + "bond.voet": ("Knoppen openen het bondspaneel in de chat (ook: /bond, /bc om te chatten)", "Buttons open the bond panel in the chat (also: /bond, /bc to chat)"),
        V + "huis.titel": ("Mijn huis in het Knuffelkwartier", "My house in the Knuffelkwartier"),
        V + "huis.intro": ("Het Knuffelkwartier is een eigen buurtje met straatjes, buren, een plein en een markt. Je koopt er een kavel met een huisje dat je zelf inricht.",
                           "The Knuffelkwartier is a neighbourhood of its own with streets, neighbours, a square and a market. You buy a plot with a house you furnish yourself."),
        V + "huis.waar": ("Erheen: praat met de Kwartiermeester op het plein van elke hoofdstad (Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads).",
                          "Getting there: talk to the Kwartiermeester on the square of every main town (Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads)."),
    }


def build(h):
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs", "lang")
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            raw = f.read()
        data = json.loads(raw)
        nieuw = {k: v[i] for k, v in t.items() if data.get(k) != v[i]}
        if not nieuw:
            continue
        # add-only, keeping the file's own formatting: insert before the closing brace
        regels = "".join(f',\n  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in nieuw.items()
                         if k not in data)
        if regels:
            raw = raw.rstrip()
            assert raw.endswith("}")
            raw = raw[:-1].rstrip() + regels + "\n}\n"
        for k, v in nieuw.items():
            if k in data:
                raise SystemExit(f"{k} exists with another text; update it by hand")
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write(raw)
    print(f"vadsrijk_v_int: {len(t)} texts")


if __name__ == "__main__":
    _direct()
