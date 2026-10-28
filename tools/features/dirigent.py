"""
1.8.0 (uitbreiding U3): de Dirigent in de Koorkom. Java: feature/dirigent (DirigentFeature, Dirigeerstokje, Concert,
DirigentGameTests); the Koorkom's podium waits while a concert runs (zingendeweide/KoorkomPodium).

  build(h)    the dirigeerstokje and the golden one (item textures + models), the recipe, the hidden quest advancements
              (dirigent_concert, dirigent_drie_sterren), the texts (Dutch in nl_nl, English in tools/lang/en/c216_dirigent.json)

The FTB quests live in tools/features/snoesverhaal.py (section De Zingende Weide).
Standalone (from the project root): PYTHONPATH=tools python tools/features/dirigent.py
"""
import json
import os
import sys

from PIL import Image

ITEMS = ["dirigeerstokje", "gouden_dirigeerstokje"]
ADVANCEMENTS = ["dirigent_concert", "dirigent_drie_sterren"]
T = "gui.guhs.dirigent."

# key -> (Dutch, English)
TEKSTEN = {
    "item.guhs.dirigeerstokje": ("Dirigeerstokje", "Conductor's Baton"),
    "item.guhs.dirigeerstokje.lore": ("Met een sterretje op het puntje. De Blubs van de Koorkom luisteren ernaar. Meestal.",
                                      "With a little star on the tip. The Blubs of the Koorkom listen to it. Mostly."),
    "item.guhs.dirigeerstokje.gebruik": ("Klik bij het podium van een Koorkom: de Blubs komen zingen. Kijk op de tel naar de "
                                         "Blub die zijn oortjes opsteekt, en klik!",
                                         "Click near the podium of a Koorkom: the Blubs come to sing. On the beat, look at the "
                                         "Blub that lifts its ears, and click!"),
    "item.guhs.gouden_dirigeerstokje": ("Gouden dirigeerstokje", "Golden Conductor's Baton"),
    "item.guhs.gouden_dirigeerstokje.lore": ("Alleen voor echte maestro's. Dirigeert precies hetzelfde, maar dan veel vahoeger.",
                                             "Only for true maestros. Conducts exactly the same, only much more vahoeg."),
    T + "geen_podium": ("Zoek eerst het podium van een Koorkom, in De Zingende Weide.",
                        "Find the podium of a Koorkom first, in the Singing Meadow."),
    T + "al_bezig": ("Hier dirigeert al iemand. Wacht even je beurt af, vadsje!", "Someone is already conducting here. Wait your turn, vadsje!"),
    T + "snoes_eerst": ("Je ziet de wilde Blubs nog niet... help eerst Snoes in slaap, njeg!",
                        "You can't see the wild Blubs yet... help Snoes fall asleep first, njeg!"),
    T + "geen_blubs": ("Er zijn hier geen Blubs om te dirigeren. Het koortje is even weg, njeg.",
                       "There are no Blubs here to conduct. The choir is out for a bit, njeg."),
    T + "begin": ("Tik tik met het stokje! %s Blubs huppelen naar het podium.", "Tap tap with the baton! %s Blubs hop to the podium."),
    T + "verzamelen": ("De Blubs zoeken hun plekje...", "The Blubs are finding their spot..."),
    T + "aftellen": ("Daar gaan we: %s...", "Here we go: %s..."),
    T + "naast": ("Huh? Er was geen tel. Klik op de maat!", "Huh? There was no beat. Click in time!"),
    T + "verkeerd": ("Piep?! Dat was niet zijn beurt.", "Squeak?! That wasn't its turn."),
    T + "stand": ("♪ %s/%s · combo %s · %s", "♪ %s/%s · combo %s · %s"),
    T + "gestopt": ("Het concert is gestopt. De Blubs huppelen verder.", "The concert has stopped. The Blubs hop on."),
    T + "einde.0": ("Oei... de Blubs piepten alle kanten op (%s/%s punten, %s perfect, beste combo %s). Nog een keertje?",
                    "Oops... the Blubs squeaked every which way (%s/%s points, %s perfect, best combo %s). One more time?"),
    T + "einde.1": ("★ Een lief liedje! %s/%s punten, %s perfect, beste combo %s.",
                    "★ A sweet little song! %s/%s points, %s perfect, best combo %s."),
    T + "einde.2": ("★★ Prachtig gezongen! %s/%s punten, %s perfect, beste combo %s.",
                    "★★ Beautifully sung! %s/%s points, %s perfect, best combo %s."),
    T + "einde.3": ("★★★ VAHOEG! Een echte maestro! %s/%s punten, %s perfect, beste combo %s.",
                    "★★★ VAHOEG! A true maestro! %s/%s points, %s perfect, best combo %s."),
    T + "record": ("Nieuw record: %s punten! (Was %s.)", "New record: %s points! (Was %s.)"),
    T + "prijs": ("De Blubs geven je een bedankje: %s!", "The Blubs give you a thank-you: %s!"),
    T + "goud": ("Wat glinstert daar? Een GOUDEN dirigeerstokje, voor de allerbeste maestro!",
                 "What's sparkling there? A GOLDEN conductor's baton, for the very best maestro!"),
}

HOUT, HOUT_D, WIT, ROZE = (150, 100, 70), (110, 70, 48), (250, 246, 240), (246, 160, 200)
STER, STER_D = (255, 226, 120), (220, 170, 60)
GOUD, GOUD_D, GOUD_L = (250, 200, 70), (196, 140, 40), (255, 236, 160)


def stokje(goud):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    staaf, staaf_d = (GOUD, GOUD_D) if goud else (WIT, (210, 204, 196))
    # the handle (bottom left) and the thin baton going up to the right
    for i in range(3, 11):
        x, y = i, 15 - i
        img.putpixel((x, y), staaf + (255,))
        img.putpixel((x, y + 1) if y + 1 < 16 else (x, y), staaf_d + (255,))
    for (x, y) in ((1, 14), (2, 13), (1, 13), (2, 14), (3, 14)):
        img.putpixel((x, y), ((GOUD_D if goud else HOUT_D) + (255,)) if (x + y) % 2 else ((GOUD if goud else HOUT) + (255,)))
    img.putpixel((2, 12), ROZE + (255,))           # a little pink ribbon on the handle
    img.putpixel((3, 13), ROZE + (255,))
    # the star on the tip
    for (x, y) in ((12, 2), (11, 3), (12, 3), (13, 3), (12, 4), (10, 3), (14, 3), (11, 5), (13, 5)):
        img.putpixel((x, y), STER + (255,))
    for (x, y) in ((12, 1), (12, 3)):
        img.putpixel((x, y), ((GOUD_L if goud else (255, 255, 255)) + (255,)))
    img.putpixel((11, 4), STER_D + (255,))
    img.putpixel((13, 4), STER_D + (255,))
    if goud:
        for (x, y) in ((14, 0), (9, 1), (15, 6)):    # sparkles
            img.putpixel((x, y), GOUD_L + (255,))
    return img


def build(h):
    for naam in ITEMS:
        h.save(stokje(naam.startswith("gouden")), "item", f"{naam}.png")
        h.item_model(naam, parent="minecraft:item/handheld")
    h.shapeless("dirigeerstokje", ["minecraft:stick", "guhs:droomzand", "minecraft:note_block"], "guhs:dirigeerstokje")
    for a in ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{a}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck()


def selfcheck():
    fout = [k for k, (nl, en) in TEKSTEN.items() if nl.count("%s") != en.count("%s")]
    if fout:
        raise SystemExit(f"dirigent self-check failed (placeholders): {fout}")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c216_dirigent.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for naam in ITEMS:
        with open(os.path.join(a, naam + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}}, f, indent=2)
            f.write("\n")
    print(f"dirigent: 2 batons, {len(ADVANCEMENTS)} advancements, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
