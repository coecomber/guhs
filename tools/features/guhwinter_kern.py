"""
1.10.9 "Guhwinter", kern (Java: feature/guhwinter + feature/guhwinter/kern; API: guhs_winter/CONTRACT.md).

  * the pepernoot (item guhs:guhwinter_pepernoot): a 16x16 painted kruidnootje (dome with light from the top left, a darker
    baked foot, a few spice specks and sugar glints) + item model + client item definition;
  * the tags guhs:guhwinter_smelt (items that melt on 3 Jan), guhs:guhwinter_smelt_blok (blocks that melt on 3 Jan) and
    guhs:guhwinter_plein_mag (items you may use on the plaza's blocks): made empty here (fireworks in plein_mag), the
    slices add their own entries (h.add_tag);
  * the Dutch texts (the English: tools/lang/en/c480_guhwinter_kern.json).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/guhwinter_kern.py
"""
import json
import math
import os

from PIL import Image

ITEM = "guhwinter_pepernoot"

# the dough, dark to light, and the outline
RAMP = [(86, 40, 20), (118, 60, 28), (150, 86, 42), (180, 114, 60), (206, 146, 86), (228, 180, 122)]
RAND = (58, 26, 12)
KRUID = (92, 44, 22)
SUIKER = (248, 222, 180)
SCHADUW = (0, 0, 0, 70)


def pepernoot():
    """The kruidnootje: a round dome on a flat foot (it sat on the baking tray), lit from the top left, with a crack, spice
    specks, two sugar glints and a soft shadow under it, so it never reads as a potato."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy, rx, ry = 7.5, 10.0, 6.4, 7.0      # a dome: the top of an ellipse...
    vloer = 12                                  # ...cut off flat at this row (the foot is rows 11-12)
    licht = (-0.55, -0.7, 0.45)
    binnen = {}
    for y in range(16):
        for x in range(16):
            if y > vloer:
                continue
            u, v = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            r2 = u * u + v * v
            if r2 > 1.0:
                continue
            n = (u, min(v, 0.0) * 1.0, math.sqrt(max(0.0, 1.0 - r2)))
            l = sum(a * b for a, b in zip(n, licht))
            t = max(0.0, min(0.999, 0.18 + 0.95 * l))
            i = int(t * len(RAMP))
            if y >= vloer - 1:                  # the baked foot: darker
                i = max(0, min(i, 2) - 1)
            binnen[(x, y)] = i
    for x in range(3, 13):                      # a soft shadow on the "table"
        img.putpixel((x, vloer + 1), SCHADUW)
    for (x, y), i in binnen.items():
        rand = any((x + dx, y + dy) not in binnen for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        img.putpixel((x, y), (RAND if rand else RAMP[i]) + (255,))
    for x, y in ((8, 6), (9, 7), (10, 7)):                                  # a little crack in the top
        if (x, y) in binnen:
            img.putpixel((x, y), RAMP[max(0, binnen[(x, y)] - 2)] + (255,))
            if (x, y - 1) in binnen:
                img.putpixel((x, y - 1), RAMP[min(len(RAMP) - 1, binnen[(x, y - 1)] + 1)] + (255,))
    for x, y in ((5, 8), (11, 9), (7, 10), (3, 9), (9, 4)):                # spice specks
        if (x, y) in binnen:
            img.putpixel((x, y), KRUID + (255,))
    for x, y in ((4, 5), (6, 4)):                                          # sugar glints where the light falls
        if (x, y) in binnen:
            img.putpixel((x, y), SUIKER + (255,))
    return img


TEKSTEN = {
    # key: (Dutch, English)  (the English also lives in tools/lang/en/c480_guhwinter_kern.json, which wins)
    "item.guhs.guhwinter_pepernoot": ("Pepernoot", "Pepernoot"),
    "item.guhs.guhwinter_pepernoot.lore": ("Kruidig, rond en heel veel waard op het Winterplein. Njeg!",
                                           "Spicy, round and worth a lot on the Winterplein. Yum!"),
    "guhwinter.kern.kop": ("Guhwinter!", "Guhwinter!"),
    "guhwinter.kern.klik": ("[Hoe kom ik daar?]", "[How do I get there?]"),
    "guhwinter.kern.klik.hover": ("Klik: het Winterplein komt in je Reisguh-lijst, en je ziet de weg.",
                                  "Click: the Winterplein goes into your Reisguh list, and you see the way."),
    "guhwinter.kern.reis.winterplein": ("Winterplein", "Winterplein"),
    "guhwinter.kern.fase.voor": ("nog niet begonnen (telt als Sint)", "not started yet (counts as Sint)"),
    "guhwinter.kern.fase.sint": ("Sinterklaas", "Sinterklaas"),
    "guhwinter.kern.fase.pakjesavond": ("Pakjesavond", "Pakjesavond (gift evening)"),
    "guhwinter.kern.fase.kerst": ("Kerst", "Christmas"),
    "guhwinter.kern.fase.oudjaar": ("Oud & Nieuw", "New Year's Eve"),
    "guhwinter.kern.fase.afgelopen": ("afgelopen", "over"),
    "guhwinter.kern.login.sint": (
        "Sinterklaas is verdwaald in de Guhmension! Bij Slot Loevestijn nam hij de verkeerde afslag... Kom snel helpen op het Winterplein. Vahoeg! %s",
        "Sinterklaas got lost in the Guhmension! He took the wrong turn at Slot Loevestijn... Come and help on the Winterplein. Hooray! %s"),
    "guhwinter.kern.login.pakjesavond": (
        "Bonk bonk bonk! Er wordt op de deur gebonsd... Het is pakjesavond op het Winterplein! %s",
        "Bonk bonk bonk! Someone is knocking on the door... It's pakjesavond on the Winterplein! %s"),
    "guhwinter.kern.login.kerst": (
        "Bij Papa Krijn en Mama Corine knettert de open haard: het is kerst op het Winterplein! Neem een warme chocomel mee. %s",
        "The fire is crackling at Papa Krijn and Mama Corine's: it's Christmas on the Winterplein! Bring a warm chocomel. %s"),
    "guhwinter.kern.login.oudjaar": (
        "Ruik je dat? Oliebollen! Op het Winterplein vieren de guhs Oud & Nieuw. Wat is jouw goede voornemen? %s",
        "Can you smell that? Oliebollen! The guhs celebrate New Year's Eve on the Winterplein. What's your resolution? %s"),
    "guhwinter.kern.nieuw.pakjesavond": (
        "Bonk bonk bonk! Er wordt op de deur gebonsd... Het is PAKJESAVOND! Snel naar het Winterplein! %s",
        "Bonk bonk bonk! Someone is knocking on the door... It's PAKJESAVOND! Hurry to the Winterplein! %s"),
    "guhwinter.kern.nieuw.kerst": (
        "Sinterklaas is weer op weg naar huis (deze keer rechtsaf bij Slot Loevestijn). Nu is het KERST op het Winterplein! Joost en Niels gaan een boom halen... met de trein?! %s",
        "Sinterklaas is on his way home again (turning right at Slot Loevestijn this time). Now it's CHRISTMAS on the Winterplein! Joost and Niels are fetching a tree... by train?! %s"),
    "guhwinter.kern.nieuw.oudjaar": (
        "De kerstboom-tekeningen zijn uitgedeeld en de oliebollen staan te pruttelen: OUD & NIEUW op het Winterplein! Wie bakt de lekkerste? %s",
        "The Christmas tree drawings have been handed out and the oliebollen are sizzling: NEW YEAR'S EVE on the Winterplein! Who bakes the tastiest one? %s"),
    "guhwinter.kern.einde": (
        "De sneeuw is gesmolten en het Winterplein is weer leeg. Dankjewel voor een knusse Guhwinter! Op het feestplein komt later weer iets nieuws. VAHOEG!",
        "The snow has melted and the Winterplein is empty again. Thank you for a cosy Guhwinter! Something new will come to the party square later. HOORAY!"),
    "guhwinter.kern.geen_bouw": ("Het Winterplein is van iedereen: hier bouw of sloop je niks. Njeg!",
                                 "The Winterplein belongs to everyone: no building or breaking here. Nope!"),
    "guhwinter.kern.smelt.item": ("Je %s is gesmolten! De winter is voorbij... dag dag, tot volgend jaar!",
                                  "Your %s melted! Winter is over... bye bye, see you next year!"),
    "guhwinter.kern.cmd.waar": (
        "Het %s staat nu in je Reisguh-lijst! Praat met een Reisguh in de Guhmension en kies het. Lopen kan ook: het ligt vlak naast het Guhportaal bij spawn (x %s, z %s).",
        "The %s is in your Reisguh list now! Talk to a Reisguh in the Guhmension and pick it. You can walk too: it's right next to the Guh portal at spawn (x %s, z %s)."),
    "guhwinter.kern.cmd.waar.voorbij": ("De Guhwinter is voorbij. Het feestplein wacht op het volgende feest!",
                                        "The Guhwinter is over. The party square is waiting for the next party!"),
    "guhwinter.kern.cmd.datum.gezet": ("Guhwinter-datum (dev) staat nu op %s: fase %s. De klok loopt vanaf hier door.",
                                       "Guhwinter date (dev) is now %s: phase %s. The clock keeps running from here."),
    "guhwinter.kern.cmd.datum.echt": ("Guhwinter-datum: weer de echte (%s, fase %s).", "Guhwinter date: the real one again (%s, phase %s)."),
    "guhwinter.kern.cmd.datum.fout": ("'%s' is geen datum. Zo: 2026-12-05 18:59 (of 'echt').",
                                      "'%s' is not a date. Like this: 2026-12-05 18:59 (or 'echt')."),
    "guhwinter.kern.cmd.moment.opnieuw": ("Moment %s vuurt opnieuw zodra zijn tijd voorbij is.", "Moment %s fires again as soon as its time has passed."),
    "guhwinter.kern.cmd.moment.nooit": ("Moment %s was nog niet gevuurd.", "Moment %s had not fired yet."),
    "guhwinter.kern.cmd.status": ("Guhwinter: fase %s, nu %s (%s)", "Guhwinter: phase %s, now %s (%s)"),
    "guhwinter.kern.cmd.status.echt": ("echte datum", "real date"),
    "guhwinter.kern.cmd.status.nep": ("dev-datum!", "dev date!"),
    "guhwinter.kern.cmd.status.moment": ("  %s  %s (over %s)", "  %s  %s (in %s)"),
    "guhwinter.kern.cmd.status.geen": ("  Geen momenten meer: alles is geweest.", "  No moments left: it's all over."),
    "guhwinter.kern.cmd.status.luisteraars": ("Luisteraars: %s, waarvan %s gevuurd:", "Listeners: %s, %s of them fired:"),
}


# =====================================================================================================================
# writing
# =====================================================================================================================
def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _tag(r, pad, waarden, w):
    """Makes a tag (or adds to it, keeping what others put there)."""
    vol = os.path.join(r, "data", "guhs", "tags", *pad.split("/")) + ".json"
    data = {"replace": False, "values": []}
    if os.path.exists(vol):
        with open(vol, encoding="utf-8") as f:
            data = json.load(f)
    for v in waarden:
        if v not in data["values"]:
            data["values"].append(v)
    w(vol, data)


def assets(a, r, w=_schrijf_json):
    os.makedirs(os.path.join(a, "textures", "item"), exist_ok=True)
    pepernoot().save(os.path.join(a, "textures", "item", ITEM + ".png"))
    w(os.path.join(a, "models", "item", ITEM + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/" + ITEM}})
    w(os.path.join(a, "items", ITEM + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + ITEM}})
    _tag(r, "item/guhwinter_smelt", [], w)
    _tag(r, "block/guhwinter_smelt_blok", [], w)
    _tag(r, "item/guhwinter_plein_mag", ["minecraft:firework_rocket"], w)


def build(h):
    assets(h.A, h.R, h.w)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    r = os.path.join("src", "main", "resources")
    a = os.path.join(r, "assets", "guhs")
    assets(a, r)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf_json(p, data)
    print(f"guhwinter_kern: 1 item, 3 tags, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
