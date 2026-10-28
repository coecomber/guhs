"""
1.8.0 (expansion U1): the Zandguh's droomwinkeltje on the Droomplein. Java: feature/droomwinkel (DroomWinkel, DroomWinkelFeature,
DroomKraampjeBlock, DroomWinkelPayloads, client/DroomWinkelScherm).

  build(h)    the droomkraampje (a lavender counter under a striped awning; facing like a chest), the stock
              (data/guhs/dromen/winkel.json), the item tag guhs:droomwinkel/bedden (optional entries: the beds of another branch),
              the hidden advancement quest/droomwinkel_gekocht, and the texts (Dutch in nl_nl, English in
              tools/lang/en/c216_droomwinkel.json)

Items that do not exist yet (the slaapkleding, droompyjama and Blub-kleding of other branches) are in the stock anyway: the
shop skips them with one warning in the log until they exist.

Standalone (from the project root): PYTHONPATH=tools python tools/features/droomwinkel.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

BLOK = "droomkraampje"
T = "gui.guhs.droomwinkel."

# the stock: (id, group, item, price[, amount[, max]]); prices next to what droomzand earns (a star 1, the molen 3 a round,
# the droomkistje 2-4 a day)
AANBOD = [
    # guh sleep clothes (agent "slaapkleding")
    ("slaapmasker", "kleding", "guhs:slaapmasker", 12),
    ("nachtjaponnetje", "kleding", "guhs:nachtjaponnetje", 20),
    ("maanmutsje", "kleding", "guhs:maanmutsje", 18),
    ("oorwolkjes", "kleding", "guhs:oorwolkjes", 14),
    ("blub_sloffen", "kleding", "guhs:blub_sloffen", 16),
    # (the knuffeltje is no shop item: it is Snoes' thank-you after the finale)
    # the player's dream pyjamas (craftable too: here only as an extra, a bit dearer)
    ("droompyjama_muts", "pyjama", "guhs:droompyjama_muts", 18),
    ("droompyjama_jasje", "pyjama", "guhs:droompyjama_jasje", 26),
    ("droompyjama_broek", "pyjama", "guhs:droompyjama_broek", 22),
    ("droompyjama_sloffen", "pyjama", "guhs:droompyjama_sloffen", 16),
    # Blub clothes (unlock items, agent "blubkleding")
] + [(f"blubkleding_{k}", "blubkleding", f"guhs:blubkleding_{k}", 8)
     for k in ("slaapmutsje", "sjaaltje", "sterrenkroontje", "bubbelhelm", "pyjamaatje", "slaapmaskertje", "bloemenkransje", "brilletje")] + [
    # dream furniture: the slaapzorg deco, boxes and cat houses
    ("maanlampje_maan", "meubels", "guhs:maanlampje_maan", 3),
    ("maanlampje_ster", "meubels", "guhs:maanlampje_ster", 3),
    ("maanlampje_wolkje", "meubels", "guhs:maanlampje_wolkje", 3),
    ("maanlampje_guh", "meubels", "guhs:maanlampje_guh", 4),
    ("nachtkastje", "meubels", "guhs:nachtkastje", 4),
    ("droomvanger", "meubels", "guhs:droomvanger", 5),
    ("mobiel", "meubels", "guhs:mobiel", 6),
    ("muziekdoosje", "meubels", "guhs:muziekdoosje", 6),
    ("slaapmasker_kapstok", "meubels", "guhs:slaapmasker_kapstok", 4),
    ("krabpaal", "meubels", "guhs:krabpaal", 4),
    ("krabpaal_blauw", "meubels", "guhs:krabpaal_blauw", 4),
    ("doos_knabbels", "meubels", "guhs:doos_knabbels", 2),
    ("verhuisdoos", "meubels", "guhs:verhuisdoos", 3),
    ("schoenendoos", "meubels", "guhs:schoenendoos", 3),
    ("pizzadoos", "meubels", "guhs:pizzadoos", 3),
    ("kattenhuisje_roze", "meubels", "guhs:kattenhuisje_roze", 5),
    ("kattenhuisje_wit", "meubels", "guhs:kattenhuisje_wit", 5),
    ("kattenhuisje_lavendel", "meubels", "guhs:kattenhuisje_lavendel", 5),
    ("kattenhuisje_mint", "meubels", "guhs:kattenhuisje_mint", 5),
    # dreamy knuffels (the Blub-knuffel with wild dice is a Java offer: DroomWinkel.registreerVast)
    ("knuffel_starry", "knuffels", "guhs:knuffel_starry", 16),
    ("knuffel_wolk", "knuffels", "guhs:knuffel_wolk", 14),
    ("knuffel_pluisguh", "knuffels", "guhs:knuffel_pluisguh", 14),
    ("knuffel_rainbow", "knuffels", "guhs:knuffel_rainbow", 20),
    # weerpotjes: only the guh-weertjes you can find elsewhere too, dear, as a way to catch up (once each)
] + [(f"weerpotje_{w}", "weer", f"guhs:weerpotje_{w}", 40, 1, 1)
     for w in ("pluisjes", "bellen", "sterren", "knusmist", "hartjes", "regenboog", "knabbel", "bloesem")]

# the beds of the bedden branch (optional tag entries: a bed that does not exist yet is just not in it)
BEDDEN = ["droombed", "guh_hoofdbed", "blub_bed", "wolkenhemelbed", "guh_blub_mandje", "stapelbedje", "luciferdoosje",
          "schelpbedje", "guh_hoofdbank", "slaaptreintje", "kussenfort", "droomschuitje", "boomhut_stapelbed", "quiltbed",
          "badkuipbed", "knuffelnest"]

# key -> (Dutch, English)
TEKSTEN = {
    f"block.guhs.{BLOK}": ("Droomkraampje", "Dream Stall"),
    f"block.guhs.{BLOK}.lore": ("Het kraampje van de Zandguh op het Droomplein. Klik erop: droomspulletjes voor droomzand!",
                                "The Sandguh's stall on the Dream Square. Click it: dream things for dream sand!"),
    "entity.guhs.droomwinkel_zandguh": ("Zandguh (droomwinkeltje)", "Sandguh (dream shop)"),
    T + "titel": ("Het droomwinkeltje", "The Dream Shop"),
    T + "label": ("zzz", "zzz"),
    T + "saldo": ("Droomzand: %s", "Dream sand: %s"),
    T + "leeg": ("Het kraampje is nog leeg... de Zandguh is aan het dutten.", "The stall is still empty... the Sandguh is napping."),
    T + "kleine_lettertjes": ("Betalen met droomzand. Een droomzandblok telt voor 9; wisselgeld krijg je terug.",
                              "Paid with dream sand. A dream sand block counts as 9; you get your change back."),
    T + "grap.0": ("\"Alles gemalen met liefde, njeg.\"", "\"Everything ground with love, njeg.\""),
    T + "grap.1": ("\"Niet afdingen, ik slaap al half.\"", "\"No haggling, I'm already half asleep.\""),
    T + "grap.2": ("\"Wie wat koopt krijgt een gaap gratis.\"", "\"Buy something and get a yawn for free.\""),
    T + "grap.3": ("\"Droomzand is geen strandzand. Vadsigheid ook niet.\"", "\"Dream sand is not beach sand. Neither is vadsigheid.\""),
    T + "grap.4": ("\"Zzz... oh, een klant! Kijk maar rustig rond.\"", "\"Zzz... oh, a customer! Have a look around.\""),
    T + "grap.5": ("\"Bonnetje? Dat heb ik gedroomd.\"", "\"Receipt? I dreamt it.\""),
    T + "groep.kleding": ("Slaapkleding voor guhs", "Sleepwear for guhs"),
    T + "groep.pyjama": ("Droompyjama (voor jou)", "Dream pyjamas (for you)"),
    T + "groep.blubkleding": ("Blub-kleding", "Blub clothes"),
    T + "groep.meubels": ("Droommeubels", "Dream furniture"),
    T + "groep.bedden": ("Bedden", "Beds"),
    T + "groep.knuffels": ("Knuffels", "Plushies"),
    T + "groep.weer": ("Weerpotjes (inhalen)", "Weather jars (catch up)"),
    T + "aantal": ("%s dingetjes", "%s things"),
    T + "aantal.1": ("1 dingetje", "1 thing"),
    T + "prijs": ("%s droomzand", "%s dream sand"),
    T + "prijs_al": ("%s droomzand (al %sx gekocht)", "%s dream sand (bought %sx)"),
    T + "heb_je": ("Heb je al!", "You've got it!"),
    T + "max": ("Gekocht: %s van %s", "Bought: %s of %s"),
    T + "knop.koop": ("Koop", "Buy"),
    T + "knop.op": ("Op", "Gone"),
    T + "tekort": ("Je komt %s droomzand tekort. Vang wat sterretjes!", "You're %s dream sand short. Catch some stars!"),
    T + "gekocht": ("Gekocht: %s! De Zandguh gaapt tevreden.", "Bought: %s! The Sandguh yawns happily."),
    T + "nee.te_duur": ("Te weinig droomzand, vadsje.", "Not enough dream sand, vadsje."),
    T + "nee.max": ("Die heb je al. Eén is genoeg, zegt de Zandguh.", "You already have that one. One is enough, says the Sandguh."),
    T + "nee.onbekend": ("Dat ligt er niet (meer). Gedroomd misschien?", "That isn't here (any more). Dreamt it, maybe?"),
    T + "nee.mislukt": ("Dat ging mis; je droomzand is terug.", "That went wrong; your dream sand is back."),
    T + "blub_knuffel": ("Wilde Blub-knuffel", "Wild Blub plushie"),
    T + "blub_knuffel.uitleg": ("Een Blub-knuffel van een willekeurige wilde Blub: welke soort, meter en grootte? Verrassing!",
                                "A Blub plushie of a random wild Blub: which kind, meter and size? Surprise!"),
    T + "bed.uitleg": ("Een gloednieuw bed, vers uit de droom.", "A brand new bed, fresh from the dream."),
    "advancements.guhs.quest.droomwinkel_gekocht.title": ("Klant van de Zandguh", "The Sandguh's customer"),
    "advancements.guhs.quest.droomwinkel_gekocht.description": ("Koop iets in het droomwinkeltje", "Buy something in the dream shop"),
}

LAV, LAV_D, LAV_L = (176, 150, 226), (132, 104, 190), (212, 194, 246)
ROZE, WIT, GOUD, STER = (246, 170, 206), (252, 246, 252), (255, 214, 110), (255, 246, 196)


def _toonbank():
    img = Image.new("RGBA", (16, 16), LAV + (255,))
    d = ImageDraw.Draw(img)
    for x in (3, 8, 13):
        d.line([x, 0, x, 15], fill=LAV_D + (255,))
    d.rectangle([0, 0, 15, 15], outline=(96, 70, 150, 255))
    for (x, y) in ((5, 4), (11, 10), (2, 12)):
        d.point([(x, y), (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)], fill=STER + (255,))
    return img


def _blad():
    img = Image.new("RGBA", (16, 16), LAV_L + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=LAV_D + (255,))
    d.ellipse([5, 5, 10, 10], fill=GOUD + (255,))                      # a little moon full of droomzand
    d.point([(7, 6), (8, 7)], fill=(255, 246, 220, 255))
    return img


def _luifel():
    img = Image.new("RGBA", (16, 16), WIT + (255,))
    d = ImageDraw.Draw(img)
    for x in range(0, 16, 4):
        d.rectangle([x, 0, x + 1, 15], fill=ROZE + (255,))
    return img


def _paal():
    img = Image.new("RGBA", (16, 16), (236, 222, 246, 255))
    ImageDraw.Draw(img).line([0, 0, 0, 15], fill=LAV_D + (255,))
    return img


def build(h):
    h.save(_toonbank(), "block", f"{BLOK}_toonbank.png")
    h.save(_blad(), "block", f"{BLOK}_blad.png")
    h.save(_luifel(), "block", f"{BLOK}_luifel.png")
    h.save(_paal(), "block", f"{BLOK}_paal.png")
    tx = {"toonbank": f"guhs:block/{BLOK}_toonbank", "blad": f"guhs:block/{BLOK}_blad", "luifel": f"guhs:block/{BLOK}_luifel",
          "paal": f"guhs:block/{BLOK}_paal", "particle": f"guhs:block/{BLOK}_toonbank"}

    def kubus(van, tot, zij, top=None):
        top = top or zij
        return {"from": van, "to": tot, "faces": {f: {"texture": "#" + (top if f in ("up", "down") else zij)}
                                                  for f in ("north", "south", "east", "west", "up", "down")}}
    elementen = [
        kubus([1, 0, 4], [15, 9, 15], "toonbank"),               # the counter (front = north, to the customer)
        kubus([0, 9, 3], [16, 10, 16], "blad", "blad"),          # its top
        kubus([0, 0, 15], [1, 13, 16], "paal"), kubus([15, 0, 15], [16, 13, 16], "paal"),   # two posts at the back
        kubus([0, 10, 3], [1, 13, 4], "paal"), kubus([15, 10, 3], [16, 13, 4], "paal"),      # two short ones at the front
        kubus([-1, 13, 1], [17, 15, 16], "luifel", "luifel"),    # the striped awning
    ]
    h.furniture_model(BLOK, elementen, tx)
    h.w(os.path.join(h.D, "dromen", "winkel.json"),
        [dict({"id": a[0], "groep": a[1], "item": a[2], "prijs": a[3]},
              **({"aantal": a[4]} if len(a) > 4 and a[4] != 1 else {}), **({"max": a[5]} if len(a) > 5 else {})) for a in AANBOD])
    h.w(os.path.join(h.D, "tags", "item", "droomwinkel", "bedden.json"),
        {"replace": False, "values": [{"id": f"guhs:{b}", "required": False} for b in BEDDEN]})
    h.w(os.path.join(h.D, "advancement", "quest", "droomwinkel_gekocht.json"),
        {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c216_droomwinkel.json")
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
    with open(os.path.join(a, BLOK + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{BLOK}"}}, f, indent=2)
        f.write("\n")
    print(f"droomwinkel: kraampje, {len(AANBOD)} offers, {len(BEDDEN)} optional beds, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
