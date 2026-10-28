"""
1.8.0 (uitbreiding U1): the own droomwolkje and the droomgasten. Java: feature/droomwolkje (DroomwolkjeFeature, Wolkjes,
Droomgasten, WolkjeBusBlock, DroomwolkjeCommando).

  the "Mijn droomwolkje" brievenbusje (the plein busje's model with a cloud-white bus texture), its loot, and the texts
  (Dutch in nl_nl, English in tools/lang/en/c216_droomwolkje.json)

Standalone (from the project root): PYTHONPATH=tools python tools/features/droomwolkje.py
"""
import json
import os

from PIL import Image

BLOK = "droombrievenbus_wolkje"
G = "gui.guhs.droomwolkje."

# key -> (Dutch, English)
TEKSTEN = {
    f"block.guhs.{BLOK}": ("Brievenbusje: Mijn droomwolkje", "Mailbox: My Dream Cloud"),
    f"block.guhs.{BLOK}.lore": ("Staat op het Droomplein. Klik erop in een droom: Droompost brengt je naar je eigen droomwolkje. "
                                "Sluipklik: hoe je vriendjes laat meebouwen.",
                                "Stands on the Dream Square. Click it in a dream: Dream Post takes you to your own dream cloud. "
                                "Sneak-click: how to let friends build along."),
    "gui.guhs.dromen.eiland.wolkje": ("Mijn droomwolkje", "My dream cloud"),
    G + "nog_niet": ("Je droomwolkje is nog niet klaar... help eerst Snoes in slaap, njeg!",
                     "Your dream cloud isn't ready yet... help Snoes fall asleep first, njeg!"),
    G + "geen_wolkje": ("Die heeft nog geen droomwolkje. Nog niet vads genoeg gedroomd!",
                        "They have no dream cloud yet. Not dreamed vads enough!"),
    G + "bezoeker": ("Je bent op bezoek. Kijken mag, bouwen niet. Veeg je pootjes!",
                     "You're visiting. Looking is fine, building isn't. Wipe your paws!"),
    G + "niet_van_jou": ("Niet van jou, vadsje! Op bezoek kijk je alleen.", "Not yours, vadsje! When visiting you only look."),
    G + "bus.uitleg": ("Je droomwolkje is van jou: bouw er wat je wilt! Vriendjes laten meebouwen: /droomwolkje vriend <naam> "
                       "(weer weg: /droomwolkje weg <naam>, lijstje: /droomwolkje vrienden). Op bezoek bij iemand: "
                       "/droomwolkje bezoek <naam>. Terug naar het plein: de steiger met de grote zeepbel.",
                       "Your dream cloud is yours: build whatever you like! Let friends build along: /droomwolkje vriend <name> "
                       "(remove: /droomwolkje weg <name>, the list: /droomwolkje vrienden). Visit someone: "
                       "/droomwolkje bezoek <name>. Back to the square: the jetty with the big bubble."),
    G + "vriend.erbij": ("%s mag nu meebouwen op je droomwolkje. Samen vadsen!", "%s may now build along on your dream cloud. Vads together!"),
    G + "vriend.weg": ("%s bouwt niet meer mee op je droomwolkje.", "%s no longer builds along on your dream cloud."),
    G + "vriend.melding": ("%1$s heeft je uitgenodigd om mee te bouwen op hun droomwolkje! (/droomwolkje bezoek %1$s)",
                           "%1$s invited you to build along on their dream cloud! (/droomwolkje bezoek %1$s)"),
    G + "vriend.zelf": ("Jezelf? Jij mag toch al alles op je eigen wolkje, njeg.", "Yourself? You may already do anything on your own cloud, njeg."),
    G + "vrienden.geen": ("Nog niemand bouwt mee op je droomwolkje.", "Nobody builds along on your dream cloud yet."),
    G + "vrienden.lijst": ("Meebouwers op je droomwolkje: %s", "Building along on your dream cloud: %s"),
    G + "bezoek.droom": ("Op bezoek gaan doe je in een droom. Eerst lekker slapen!", "Visiting happens in a dream. Have a good sleep first!"),
    G + "gast.naam": ("%s (droomgast)", "%s (dream guest)"),
    G + "gast.knuffel.jij": ("Je gaf %s een knuffel. Hij droomt nu nog fijner!", "You gave %s a hug. Now it dreams even sweeter!"),
    G + "gast.knabbel.jij": ("Je gaf %s een knabbel. Smikkel smikkel, in z'n slaap!", "You gave %s a nibble. Munch munch, in its sleep!"),
    G + "gast.bericht.knuffel": ("%s gaf %s een knuffel in Guhdromenland 💤", "%s gave %s a hug in Guh Dreamland 💤"),
    G + "gast.bericht.knabbel": ("%s gaf %s een knabbel in Guhdromenland 💤", "%s gave %s a nibble in Guh Dreamland 💤"),
}

RES = os.path.join("src", "main", "resources")
A = os.path.join(RES, "assets", "guhs")


def _schrijf(pad, data):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def textuur():
    """The plein busje's texture made cloud-white with a soft lavender edge (it is the busje to your own cloud)."""
    bron = Image.open(os.path.join(A, "textures", "block", "droombrievenbus_plein.png")).convert("RGBA")
    uit = Image.new("RGBA", bron.size)
    for y in range(bron.height):
        for x in range(bron.width):
            r, g, b, a = bron.getpixel((x, y))
            licht = (r + g + b) / 3 / 255
            # cloud white with a lavender shade where the original is dark
            nr, ng, nb = int(196 + 58 * licht), int(186 + 66 * licht), int(232 + 22 * licht)
            uit.putpixel((x, y), (min(nr, 255), min(ng, 255), min(nb, 255), a))
    uit.save(os.path.join(A, "textures", "block", BLOK + ".png"))


def bestanden():
    _schrijf(os.path.join(A, "blockstates", BLOK + ".json"), {"variants": {
        f"facing={f}": ({"model": f"guhs:block/{BLOK}"} | ({"y": y} if y else {}))
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    _schrijf(os.path.join(A, "models", "block", BLOK + ".json"), {
        "parent": "guhs:block/droombrievenbus_plein",
        "textures": {"bus": f"guhs:block/{BLOK}", "particle": f"guhs:block/{BLOK}"}})
    _schrijf(os.path.join(A, "models", "item", BLOK + ".json"), {"parent": f"guhs:block/{BLOK}"})
    _schrijf(os.path.join(A, "items", BLOK + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{BLOK}"}})
    _schrijf(os.path.join(RES, "data", "guhs", "loot_table", "blocks", BLOK + ".json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{BLOK}"}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def teksten():
    _schrijf(os.path.join("tools", "lang", "en", "c216_droomwolkje.json"), {k: en for k, (nl, en) in TEKSTEN.items()})
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, d)


if __name__ == "__main__":
    textuur()
    bestanden()
    teksten()
    print(f"droomwolkje: brievenbusje, {len(TEKSTEN)} texts")
