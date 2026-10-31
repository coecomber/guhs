"""
1.8.1: the stalls of the bigger guh kermis. Java: feature/kermiskraam.

  build(h)    the seven counters guhs:kermiskraam_<id> (one shared shape, an own painted front per stall), the five kermis foods,
              the hidden advancements quest/kermiskraam_gespeeld and quest/kermiskraam_alle_perfect, and the texts (Dutch in
              nl_nl, English in tools/lang/en/c218_kermiskraam.json)
  ftb(fq)     the kermis quests next to the old kermis section (English overlay tools/lang/en/c218_kermiskraam_ftb.json)

Standalone (from the project root): PYTHONPATH=tools python tools/features/kermiskraam.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

FTB_CHAPTER = "guhs_minigames"
FTB_SECTION = "De kraampjes van de Guhkermis"

KRAMEN = ("churros", "suikerspin", "poffertjes", "oliebollen", "vissen", "blikken", "schieten")
ETEN = ("churros", "churros_guhtella", "suikerspin", "poffertjes", "oliebollen")
T = "gui.guhs.kermiskraam."

# stall: (Dutch name, English name, Dutch guh, English guh, Dutch hello/how-to, English)
NAMEN = {
    "churros": ("Churroskraam", "Churros Stall", "Churros-guh", "Churros Guh"),
    "suikerspin": ("Suikerspinkraam", "Candy Floss Stall", "Suikerspin-guh", "Candy Floss Guh"),
    "poffertjes": ("Poffertjeskraam", "Poffertjes Stall", "Poffertjes-guh", "Poffertjes Guh"),
    "oliebollen": ("Oliebollenkraam", "Oliebollen Stall", "Oliebollen-guh", "Oliebollen Guh"),
    "vissen": ("Guhtjes vissen", "Guhtje Fishing", "Vissen-guh", "Fishing Guh"),
    "blikken": ("Blikkengooien", "Tin Toss", "Blikken-guh", "Tin Toss Guh"),
    "schieten": ("Knabbelschieten", "Nibble Shooting", "Schiet-guh", "Shooting Guh"),
}

TEKSTEN = {
    "item.guhs.churros": ("Churros", "Churros"),
    "item.guhs.churros.lore": ("Linguhs worden hier misselijk van. Niemand weet waarom.", "Linguhs get queasy from these. Nobody knows why."),
    "item.guhs.churros_guhtella": ("Churros met Guhtella", "Churros with Guhtella"),
    "item.guhs.churros_guhtella.lore": ("Nog zo'n ding waar Linguhs misselijk van worden. Dubbel lekker dus, njeg.",
                                        "One more thing that makes Linguhs queasy. Doubly tasty then, njeg."),
    "item.guhs.suikerspin": ("Suikerspin", "Candy Floss"),
    "item.guhs.suikerspin.lore": ("Een wolkje suiker op een stokje. Plakt aan je guhsnor.", "A little cloud of sugar on a stick. Sticks to your guh moustache."),
    "item.guhs.poffertjes": ("Poffertjes", "Poffertjes"),
    "item.guhs.poffertjes.lore": ("Bakje vol, met een berg poedersuiker. Vahoeg!", "A tray full, under a mountain of powdered sugar. Vahoeg!"),
    "item.guhs.oliebollen": ("Oliebollen", "Oliebollen"),
    "item.guhs.oliebollen.lore": ("Rond, warm en vadsig. Precies als een guh.", "Round, warm and vadsig. Just like a guh."),
    "advancements.guhs.quest.kermiskraam_gespeeld.title": ("Kraampjesvads", "Stall Vads"),
    "advancements.guhs.quest.kermiskraam_gespeeld.description": ("Speel een spelletje bij een kraampje op de Guhkermis", "Play a game at a stall of the Guh Kermis"),
    "advancements.guhs.quest.kermiskraam_alle_perfect.title": ("Kermiskoning", "Kermis King"),
    "advancements.guhs.quest.kermiskraam_alle_perfect.description": ("Haal de hoogste score bij alle drie de spelkraampjes", "Get the top score at all three game stalls"),
    T + "churros.hallo": ("Njeg! Churros voor 15 knabbels, met Guhtella voor 18. Verse vads!", "Njeg! Churros for 15 nibbles, with Guhtella for 18. Fresh vads!"),
    T + "suikerspin.hallo": ("Suikerspin! Acht knabbels voor een wolkje. Vahoeg zoet!", "Candy floss! Eight nibbles for a little cloud. Vahoeg sweet!"),
    T + "poffertjes.hallo": ("Een bakje poffertjes voor 10 knabbels? Poedersuiker gratis, njeg!", "A tray of poffertjes for 10 nibbles? Powdered sugar is free, njeg!"),
    T + "oliebollen.hallo": ("Oliebollen! 12 knabbels, en ja, het mag het hele jaar.", "Oliebollen! 12 nibbles, and yes, you may have them all year."),
    T + "vissen.titel": ("Guhtjes vissen", "Guhtje Fishing"),
    T + "vissen.uitleg": ("Klik als een guhtje onder de haak drijft", "Click when a guhtje floats under the hook"),
    T + "blikken.titel": ("Blikkengooien", "Tin Toss"),
    T + "blikken.uitleg": ("Klik als het streepje in het groen staat", "Click when the marker is in the green"),
    T + "schieten.titel": ("Knabbelschieten", "Nibble Shooting"),
    T + "schieten.uitleg": ("Klik op de knabbels voor ze weg zijn", "Click the nibbles before they are gone"),
    T + "score": ("Score: %s van %s", "Score: %s of %s"),
    T + "worpen": ("Nog %s keer", "%s left"),
    T + "knabbels_over": ("Nog %s knabbels", "%s nibbles left"),
    T + "blikken_om": ("%s blikken om!", "%s tins down!"),
    T + "einde.perfect": ("VAHOEG! Alles raak!", "VAHOEG! All hits!"),
    T + "einde.goed": ("Njeg, goed gedaan!", "Njeg, well done!"),
    T + "einde.jammer": ("Bijna! Een troostknabbel dan.", "Almost! Have a consolation nibble."),
    T + "wacht": ("De kraam-guh zet alles klaar. Nog %s seconden, vadsje!", "The stall guh is setting up. %s more seconds, vadsje!"),
    T + "prijs.perfect": ("Topscore! 2 kermisbonnen voor jou!", "Top score! 2 kermisbonnen for you!"),
    T + "prijs.bon": ("Gewonnen: een kermisbon!", "You won a kermisbon!"),
    T + "prijs.troost": ("Een troostknabbel, omdat je het zo lief probeerde.", "A consolation nibble, because you tried so sweetly."),
}
for _k, (_nl, _en, _gnl, _gen) in NAMEN.items():
    TEKSTEN[f"block.guhs.kermiskraam_{_k}"] = (_nl, _en)
    TEKSTEN[f"entity.guhs.kermiskraam.{_k}"] = (_gnl, _gen)
    TEKSTEN[f"block.guhs.kermiskraam_{_k}.lore"] = ("Een kraampje van de Guhkermis. De kraam-guh komt vanzelf achter de toonbank staan.",
                                                    "A stall of the Guh Kermis. The stall guh comes to stand behind the counter by itself.")

WIT, ROZE, ROZE_D, HOUT, HOUT_D = (255, 244, 224), (232, 92, 132), (176, 56, 96), (196, 140, 88), (140, 92, 54)


def _vierkant(kleur):
    return Image.new("RGBA", (16, 16), kleur + (255,))


def _zijkant():
    img = _vierkant(WIT)
    d = ImageDraw.Draw(img)
    for x in range(0, 16, 4):
        d.rectangle([x, 0, x + 1, 15], fill=ROZE + (255,))
    d.rectangle([0, 0, 15, 15], outline=ROZE_D + (255,))
    return img


def _blad():
    img = _vierkant(HOUT)
    d = ImageDraw.Draw(img)
    for y in (3, 7, 11):
        d.line([0, y, 15, y], fill=HOUT_D + (255,))
    d.rectangle([0, 0, 15, 15], outline=HOUT_D + (255,))
    return img


def _plaatje(kraam):
    """The little picture on the front of a stall (on the striped front, in a white frame)."""
    img = _zijkant()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, 12, 12], fill=WIT + (255,), outline=ROZE_D + (255,))
    k = (226, 164, 92, 255)
    if kraam == "churros":
        d.line([5, 11, 7, 4], fill=k, width=2)
        d.line([9, 11, 11, 4], fill=k, width=2)
    elif kraam == "suikerspin":
        d.ellipse([5, 4, 10, 9], fill=(255, 170, 210, 255))
        d.line([8, 9, 8, 12], fill=HOUT_D + (255,))
    elif kraam == "poffertjes":
        for x, y in ((5, 8), (8, 8), (6, 5), (9, 5)):
            d.ellipse([x, y, x + 2, y + 2], fill=k)
    elif kraam == "oliebollen":
        d.ellipse([5, 5, 10, 10], fill=(150, 92, 44, 255))
        d.point([(6, 6), (9, 7), (7, 9)], fill=(255, 255, 255, 255))
    elif kraam == "vissen":
        d.rectangle([5, 7, 10, 10], fill=(255, 176, 204, 255))
        d.point([(5, 6), (10, 6)], fill=(255, 176, 204, 255))
        d.point([(6, 8), (9, 8)], fill=(32, 16, 24, 255))
    elif kraam == "blikken":
        for x, y in ((5, 8), (8, 8), (6, 5)):
            d.rectangle([x, y, x + 2, y + 2], fill=(192, 200, 208, 255))
    else:
        d.ellipse([4, 4, 11, 11], outline=ROZE_D + (255,))
        d.ellipse([7, 7, 8, 8], fill=ROZE + (255,))
    return img


def _px(a, x, y, c):
    a.putpixel((x, y), c + (255,))


def _capsule(a, x1, y1, x2, y2, kleur, licht, rand):
    d = ImageDraw.Draw(a)
    d.line([x1, y1, x2, y2], fill=rand + (255,), width=4)
    d.line([x1, y1, x2, y2], fill=kleur + (255,), width=2)
    d.line([x1, y1, x2, y2], fill=licht + (255,), width=1)


def _churros():
    a = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _capsule(a, 5, 12, 6, 2, (226, 164, 92), (240, 196, 130), (142, 82, 36))
    _capsule(a, 10, 12, 12, 3, (226, 164, 92), (240, 196, 130), (142, 82, 36))
    for y in range(11, 16):
        for x in range(3, 14):
            _px(a, x, y, (250, 240, 228) if (x + y) % 4 else (232, 92, 132))
    return a


def _guhtella():
    a = _churros()
    for y in range(0, 7):
        for x in range(16):
            if a.getpixel((x, y))[3]:
                _px(a, x, y, (82, 44, 30) if (x + y) % 3 else (120, 68, 46))
    return a


def _suikerspin():
    a = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(a)
    d.line([8, 8, 8, 15], fill=(226, 200, 160, 255), width=1)
    d.ellipse([2, 0, 13, 10], fill=(255, 170, 210, 255), outline=(232, 110, 170, 255))
    for x, y in ((5, 3), (9, 2), (7, 6), (10, 6), (4, 6)):
        _px(a, x, y, (255, 214, 232))
    return a


def _poffertjes():
    a = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(a)
    d.rectangle([1, 10, 14, 13], fill=(250, 240, 228, 255), outline=(190, 170, 150, 255))
    for x, y in ((2, 6), (6, 6), (10, 6), (4, 3), (8, 3)):
        d.ellipse([x, y, x + 4, y + 4], fill=(226, 164, 92, 255), outline=(160, 100, 50, 255))
        _px(a, x + 2, y + 1, (255, 250, 245))
    return a


def _oliebollen():
    a = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(a)
    for x, y in ((1, 6), (7, 7), (4, 1)):
        d.ellipse([x, y, x + 7, y + 7], fill=(150, 92, 44, 255), outline=(96, 54, 24, 255))
    for x, y in ((3, 8), (6, 3), (9, 9), (11, 11), (4, 11), (8, 4)):
        _px(a, x, y, (255, 255, 255))
    return a


ICONEN = {"churros": _churros, "churros_guhtella": _guhtella, "suikerspin": _suikerspin, "poffertjes": _poffertjes, "oliebollen": _oliebollen}


def build(h):
    h.save(_zijkant(), "block", "kermiskraam_zijkant.png")
    h.save(_blad(), "block", "kermiskraam_blad.png")

    def kubus(van, tot, voor="zijkant"):
        return {"from": van, "to": tot, "faces": {
            f: {"texture": "#" + ("blad" if f in ("up", "down") else voor if f == "north" else "zijkant")}
            for f in ("north", "south", "east", "west", "up", "down")}}
    for k in KRAMEN:
        naam = f"kermiskraam_{k}"
        h.save(_plaatje(k), "block", f"{naam}_voor.png")
        tx = {"zijkant": "guhs:block/kermiskraam_zijkant", "blad": "guhs:block/kermiskraam_blad", "voor": f"guhs:block/{naam}_voor",
              "particle": "guhs:block/kermiskraam_zijkant"}
        h.furniture_model(naam, [kubus([0, 0, 1], [16, 10, 16], "voor"), kubus([0, 10, 0], [16, 11, 16])], tx)
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:kermiskraam_{k}" for k in KRAMEN])
    for naam in ETEN:
        h.save(ICONEN[naam](), "item", f"{naam}.png")
        h.item_model(naam)
    for adv in ("kermiskraam_gespeeld", "kermiskraam_alle_perfect"):
        h.w(os.path.join(h.D, "advancement", "quest", adv + ".json"), {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


# key, Dutch title, Dutch text, English title, English text, icon, x, y, task
FTB = [
    ("churros", "Linguhs opgelet", "Koop op de &dGuhkermis&r een portie &6churros&r bij de Churroskraam: 15 Kaas Knabbels, of 18 met Guhtella.",
     "Linguhs beware", "Buy a portion of &6churros&r at the Churros Stall on the &dGuh Kermis&r: 15 Kaas Knabbels, or 18 with Guhtella.",
     "guhs:churros", 0, ("item", "guhs:churros")),
    ("gespeeld", "Kraampjesvads", "Speel een spelletje bij &6Guhtjes vissen&r, &6Blikkengooien&r of &6Knabbelschieten&r. Goed gespeeld = kermisbonnen!",
     "Stall Vads", "Play a game at &6Guhtje Fishing&r, &6Tin Toss&r or &6Nibble Shooting&r. Play well = kermisbonnen!",
     "guhs:kermisbon", 1.5, ("adv", "kermiskraam_gespeeld")),
    ("perfect", "Kermiskoning", "Haal de hoogste score bij alle drie de spelkraampjes.",
     "Kermis King", "Get the top score at all three game stalls.",
     "guhs:kermisbon", 3, ("adv", "kermiskraam_alle_perfect")),
]


def ftb(fq):
    for key, nl_t, nl_d, _en_t, _en_d, icoon, x, taak in FTB:
        doel = fq.item(taak[1]) if taak[0] == "item" else fq.adv(taak[1])
        fq.q(f"kermiskraam_{key}", nl_t, nl_d, icoon, [doel], rewards=(("guhs:kaas_knabbels", 16),), x=x, y=0, xp=150)


def _schrijf(pad, data):
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    _schrijf(os.path.join("tools", "lang", "en", "c218_kermiskraam.json"), {k: en for k, (nl, en) in TEKSTEN.items()})
    ftb_en = {}
    for key, _nl_t, _nl_d, en_t, en_d, *_ in FTB:
        ftb_en[f"ftb.guhs_minigames.q.kermiskraam_{key}.title"] = en_t
        ftb_en[f"ftb.guhs_minigames.q.kermiskraam_{key}.desc"] = en_d
    ftb_en["ftb.guhs_minigames.section.extra_kermiskraam"] = "The stalls of the Guh Kermis"
    _schrijf(os.path.join("tools", "lang", "en", "c218_kermiskraam_ftb.json"), ftb_en)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, d)
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    for n in [f"kermiskraam_{k}" for k in KRAMEN] + list(ETEN):
        _schrijf(os.path.join(a, n + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}})
    print(f"kermiskraam: {len(KRAMEN)} kramen, {len(ETEN)} etenswaren, {len(TEKSTEN)} teksten")


if __name__ == "__main__":
    _direct()
