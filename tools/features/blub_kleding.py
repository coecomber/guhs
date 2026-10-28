"""
1.8.0 extension (round U2), the Blub-kleding: eight pieces besides the strikjes. Their bones and texture paint live in
blub_modellen.py (kleding_bones, painted in every Blub texture, drawn by BlubClient's KledingLaag); here: the unlock items
guhs:blubkleding_<id> (icons, item models), and their texts. Ids as in dromen-ontwerp/BOUWPLAN_180_UITBREIDING.md.
"""
from PIL import Image, ImageDraw

try:
    from features import blub_modellen as modellen
except ImportError:
    import os
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import blub_modellen as modellen

# id -> (plek, NL name, EN name, NL lore, EN lore)
STUKKEN = {
    "slaapmutsje": ("hoofd", "Slaapmutsje", "Nightcap",
                    "Met een pompon die over één oortje bungelt. Welterusten, vadsje!",
                    "With a pompom dangling over one ear. Sleep tight, little vads!"),
    "sjaaltje": ("nek", "Gebreid sjaaltje", "Knitted scarf",
                 "Door Oma-guh gebreid, met één steek los. Dat hoort zo, njeg.",
                 "Knitted by Granny guh, one stitch loose. That's how it should be, njeg."),
    "sterrenkroontje": ("hoofd", "Sterrenkroontje", "Star crown",
                        "Voor de prinses of prins van de droomzee. Glinstert een beetje.",
                        "For the princess or prince of the dream sea. Sparkles a bit."),
    "bubbelhelm": ("hoofd", "Bubbelhelm", "Bubble helmet",
                   "Een zeepbel om je hele Blub. Hij heet niet voor niets Bubbles.",
                   "A soap bubble around your whole Blub. He's not called Bubbles for nothing."),
    "pyjamaatje": ("lijf", "Pyjamaatje", "Little pyjamas",
                   "Gestreept, warm, en zijn gezichtje blijft vrij. Bedtijd!",
                   "Striped, warm, and his face stays free. Bedtime!"),
    "slaapmaskertje": ("ogen", "Slaapmaskertje", "Sleep mask",
                       "Met geborduurde dichte oogjes. Hij ziet niks, maar droomt wel.",
                       "With embroidered closed eyes. He sees nothing, but he dreams."),
    "bloemenkransje": ("hoofd", "Bloemenkransje", "Flower wreath",
                       "Van echte maanbloemetjes. Ruikt naar nachtlucht en knabbels.",
                       "Made of real moon flowers. Smells of night air and nibbles."),
    "brilletje": ("ogen", "Rond brilletje", "Round glasses",
                  "Oma-stijl. Hij leest er geen boekjes mee, maar hij kijkt wel heel wijs.",
                  "Granny style. He doesn't read with them, but he looks very wise."),
}
assert list(STUKKEN) == modellen.KLEDING
ITEMS = [f"blubkleding_{k}" for k in STUKKEN]

TEKSTEN = {
    "gui.guhs.blubguh.kast.plek.hoofd": ("Hoofdje", "Head"),
    "gui.guhs.blubguh.kast.plek.nek": ("Nekje", "Neck"),
    "gui.guhs.blubguh.kast.plek.lijf": ("Lijfje", "Body"),
    "gui.guhs.blubguh.kast.plek.ogen": ("Oogjes", "Eyes"),
    "gui.guhs.blubguh.kast.op_slot": ("Nog op slot: maak hem los met Blub-kleding (het droomwinkeltje heeft ze!)",
                                      "Still locked: unlock it with Blub clothing (the dream shop has them!)"),
    "gui.guhs.blubguh.kleding.ontgrendeld": ("%s hangt nu in de Blub-kledingkast van al je Blubs!",
                                             "%s now hangs in the Blub wardrobe of all your Blubs!"),
    "gui.guhs.blubguh.kleding.al_gehad": ("%s hangt al in de kast. Geef hem aan een vriendje, njeg!",
                                          "%s is already in the wardrobe. Give it to a friend, njeg!"),
    "item.guhs.blubkleding.gebruik": ("Rechtsklik: voor altijd in de Blub-kledingkast", "Right-click: in the Blub wardrobe for good"),
}
for _id, (_plek, _nl, _en, _lnl, _len) in STUKKEN.items():
    TEKSTEN[f"gui.guhs.blubguh.kast.kleding.{_id}"] = (_nl, _en)
    TEKSTEN[f"item.guhs.blubkleding_{_id}"] = (f"Blub-kleding: {_nl.lower()}", f"Blub clothing: {_en.lower()}")
    TEKSTEN[f"item.guhs.blubkleding_{_id}.lore"] = (_lnl, _len)


def _icoon(teken):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    teken(ImageDraw.Draw(img))
    return img


def _muts(d):
    d.polygon([(3, 12), (13, 12), (10, 4), (7, 3)], fill=modellen.MUTS)
    d.line([(10, 4), (13, 7)], fill=modellen.MUTS2, width=2)
    d.rectangle([2, 12, 13, 14], fill=modellen.POMPON)
    d.ellipse([12, 6, 15, 9], fill=modellen.POMPON)


def _sjaal(d):
    d.rectangle([1, 4, 14, 7], fill=modellen.SJAAL)
    for x in range(1, 15, 3):
        d.rectangle([x, 4, x, 7], fill=modellen.SJAAL2)
    d.rectangle([9, 7, 12, 14], fill=modellen.SJAAL)
    d.rectangle([9, 10, 12, 11], fill=modellen.SJAAL2)


def _kroon(d):
    d.rectangle([2, 10, 13, 13], fill=modellen.GOUD)
    d.rectangle([2, 13, 13, 13], fill=modellen.GOUD2)
    for x, h in ((3, 6), (7, 4), (11, 6)):
        d.polygon([(x - 1, 10), (x + 2, 10), (x + 0.5, h)], fill=modellen.STERGEEL)


def _helm(d):
    d.ellipse([1, 1, 14, 14], fill=(196, 230, 252, 120), outline=(150, 200, 240, 220))
    d.ellipse([4, 3, 7, 6], fill=(255, 255, 255, 200))


def _pyjama(d):
    for y in range(3, 14):
        d.line([(2, y), (13, y)], fill=modellen.PYJ if (y // 2) % 2 else modellen.PYJ2)
    d.rectangle([2, 3, 13, 13], outline=(140, 170, 220))


def _masker(d):
    d.rounded_rectangle([1, 5, 14, 11], 3, fill=modellen.MASKER)
    for cx in (5, 10):
        d.arc([cx - 2, 6, cx + 2, 10], 20, 160, fill=modellen.MASKER2)


def _krans(d):
    d.ellipse([2, 4, 13, 13], outline=modellen.BLAD, width=2)
    for x, y in ((3, 5), (8, 3), (12, 6), (11, 11), (5, 11)):
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=modellen.BLOEM)
        d.point((x, y), fill=modellen.BLOEMHART)


def _bril(d):
    d.ellipse([1, 5, 7, 11], outline=modellen.BRIL, width=1)
    d.ellipse([8, 5, 14, 11], outline=modellen.BRIL, width=1)
    d.line([(7, 7), (8, 7)], fill=modellen.BRIL)


ICONEN = {"slaapmutsje": _muts, "sjaaltje": _sjaal, "sterrenkroontje": _kroon, "bubbelhelm": _helm, "pyjamaatje": _pyjama,
          "slaapmaskertje": _masker, "bloemenkransje": _krans, "brilletje": _bril}


def build(h):
    for k in STUKKEN:
        h.save(_icoon(ICONEN[k]), "item", f"blubkleding_{k}.png")
        h.item_model(f"blubkleding_{k}")
