"""
1.8.0 ronde 3b (and ronde 3): the Blub-guh's own chores in a Guhhuisje and its hug with a guh. Called by blub.py.

  build(h)    the Blub-bubbelbadje (block textures, models, blockstate, loot, recipe), the three chore icons (items, only for
              the huisje screen), the effect icon "fris en blij", and the texts (TEKSTEN: key -> (Dutch, English))
The chores (Java: feature/blub/klus): blub_bellen (Bellenbesproeier), blub_bezorgen (Knabbelbezorger), blub_zingen
(Slaapliedjeszanger), blub_bad (Bubbelbadmeester). Like Sjokkel's polijsten they are "alleen Blubs" in the huisje screen.
"""
from PIL import Image, ImageDraw

ROZE, ROZE_D, ROZE_L = (246, 170, 200), (214, 128, 168), (255, 214, 230)
WATER, WATER_D = (150, 214, 246), (110, 178, 230)
SCHUIM, SCHUIM_D = (255, 255, 255), (226, 236, 246)
BLAUW, BLAUW_L, ST = (138, 201, 247), (160, 213, 247), (112, 67, 80)

KLUSJES = {  # id -> (name NL, EN, tip NL, EN, needed NL, EN, {reason: (NL, EN)}, dagboek title NL/EN, dagboek text NL/EN, wist-je-dat NL/EN)
    "blub_bellen": ("Bellenbesproeier", "Bubble sprinkler",
                    "Alleen Blubs: blaast waterbelletjes over de gewassen, die groeien dan een tikje sneller",
                    "Blubs only: blows water bubbles over the crops, they grow a little faster",
                    "gewassen die nog groeien rond het huisje (tarwe, wortels, aardappels, kaasknabbelplantjes...)",
                    "crops that are still growing around the huisje (wheat, carrots, potatoes, kaasknabbel plants...)",
                    {"gewassen": ("Gewassen die nog groeien: %s", "Crops still growing: %s"), "geen": ("Geen groeiende gewassen", "No growing crops")},
                    ("Bellen geblazen", "Blew bubbles"), ("Ik heb bellen over de plantjes geblazen. Blub! Ze groeiden er een beetje van.",
                                                          "I blew bubbles over the little plants. Blub! They grew a bit from it."),
                    ("Wist je dat de plantjes bij %s een beetje sneller groeien? Ik blaas er bellen overheen. Blub blub!",
                     "Did you know the plants at %s grow a bit faster? I blow bubbles over them. Blub blub!")),
    "blub_bezorgen": ("Knabbelbezorger", "Knabbel courier",
                      "Alleen Blubs: brengt kaasknabbels uit de kist naar de tamme guhs en diertjes in de buurt",
                      "Blubs only: brings kaasknabbels from the chest to the tame guhs and critters nearby",
                      "kaasknabbels in de kist naast het huisje of in de Bank Guh, en tamme guhs of diertjes in de buurt",
                      "kaasknabbels in the chest next to the huisje or in the Bank Guh, and tame guhs or critters nearby",
                      {"hongerig": ("Wie lust er een knabbel: %s", "Who'd like a knabbel: %s"), "vol": ("Iedereen heeft al gesmuld", "Everyone has had a snack"),
                       "geen_knabbels": ("Geen kaasknabbels in de kist", "No kaasknabbels in the chest"), "geen_opslag": ("Geen kist of Bank Guh", "No chest or Bank Guh")},
                      ("Knabbels bezorgd", "Delivered knabbels"), ("Hup, hup, knabbel! Iedereen kreeg er eentje. Ik ook een beetje. Njeg.",
                                                                    "Hop, hop, knabbel! Everyone got one. Me a little too. Njeg."),
                      ("Wist je dat iedereen bij %s een knabbel van mij kreeg? Huppelend bezorgd!", "Did you know everyone at %s got a knabbel from me? Delivered with a hop!")),
    "blub_zingen": ("Slaapliedjeszanger", "Lullaby singer",
                    "Alleen Blubs: piept 's avonds het slaapliedje, de guhs van het huisje zijn de volgende dag uitgerust",
                    "Blubs only: squeaks the lullaby in the evening, the huisje's guhs are well rested the next day",
                    "de avond (eens per dag) en guhs die in dit huisje wonen", "the evening (once a day) and guhs living in this huisje",
                    {"avond": ("'s Avonds zingt hij", "It sings in the evening")},
                    ("Slaapliedje gezongen", "Sang a lullaby"), ("Piep, piep, pieeep... Ze sliepen allemaal heel lekker. Ik ook.",
                                                                 "Squeak, squeak, squeeeak... They all slept so well. Me too."),
                    ("Wist je dat de guhs van %s zo lekker slapen? Ik zing ze in slaap. Sst...", "Did you know the guhs of %s sleep so well? I sing them to sleep. Shh...")),
    "blub_bad": ("Bubbelbadmeester", "Bubble bath master",
                 "Alleen Blubs: zet een Blub-bubbelbadje klaar met schuim, guhs worden er fris en blij van",
                 "Blubs only: gets a Blub bubble bath ready with foam, guhs feel fresh and happy from it",
                 "een Blub-bubbelbadje rond het huisje", "a Blub bubble bath around the huisje",
                 {"klaarmaken": ("Badjes zonder schuim: %s", "Baths without foam: %s"), "schuim": ("Er ligt al schuim in", "There's foam in it already"),
                  "geen_badje": ("Geen Blub-bubbelbadje in de buurt", "No Blub bubble bath nearby")},
                 ("Bad klaargezet", "Got the bath ready"), ("Schuim, schuim, schuim! De guhs gingen erin. Ze roken naar bubbels.",
                                                           "Foam, foam, foam! The guhs got in. They smelled of bubbles."),
                 ("Wist je dat er bij %s een bubbelbad klaarstaat? Ik heb het zelf geschuimd!", "Did you know there's a bubble bath ready at %s? I foamed it myself!")),
}

TEKSTEN = {
    "block.guhs.blub_badje": ("Blub-bubbelbadje", "Blub Bubble Bath"),
    "item.guhs.blub_badje": ("Blub-bubbelbadje", "Blub Bubble Bath"),
    "item.guhs.blubguh_klus_bellen": ("Bellenbesproeier", "Bubble sprinkler"),
    "item.guhs.blubguh_klus_knabbels": ("Knabbelbezorger", "Knabbel courier"),
    "item.guhs.blubguh_klus_liedje": ("Slaapliedjeszanger", "Lullaby singer"),
    "effect.guhs.fris_en_blij": ("Fris en blij", "Fresh and happy"),
    "gui.guhs.huisje.overzicht.wie.blub": ("Wie: alleen een Blub.", "Who: only a Blub."),
    # (1.8.0: a chore of one kind of resident says so in the list, instead of "kan niet"; Sjokkel too)
    "gui.guhs.huisje.alleen.blub": ("alleen Blubs", "Blubs only"),
    "gui.guhs.huisje.alleen.blub.hint": ("Dit kan alleen een Blub. Laat er een in dit huisje wonen: squish!",
                                         "Only a Blub can do this. Let one live in this huisje: squish!"),
    "gui.guhs.huisje.alleen.sjokkel": ("alleen Sjokkel", "Sjokkel only"),
    "gui.guhs.huisje.alleen.sjokkel.hint": ("Dit kan alleen Sjokkel, heel langzaam. Woont hij hier? Tink tink!",
                                            "Only Sjokkel can do this, very slowly. Does he live here? Tink tink!"),
    # the hug with a guh (ronde 3)
    "gui.guhs.blubguh.knuffel.geen": ("%s kijkt rond... maar er is geen tamme guh om te knuffelen. Dan maar een knuffel voor jou? Squish!",
                                      "%s looks around... but there's no tame guh to hug. A hug for you then? Squish!"),
}
for _id, (_n, _ne, _t, _te, _nod, _node, _redenen, _dt, _dtt, _wj) in KLUSJES.items():
    TEKSTEN[f"gui.guhs.klus.{_id}"] = (_n, _ne)
    TEKSTEN[f"gui.guhs.klus.{_id}.tip"] = (_t, _te)
    TEKSTEN[f"gui.guhs.huisje.overzicht.klus.{_id}.nodig"] = (_nod, _node)
    for _r, (_rn, _re) in _redenen.items():
        TEKSTEN[f"gui.guhs.huisje.overzicht.klus.{_id}.{_r}"] = (_rn, _re)
    TEKSTEN[f"gui.guhs.dagboek.eerste.klusjes_{_id}"] = _dt
    TEKSTEN[f"gui.guhs.dagboek.eerste.klusjes_{_id}.tekst"] = _dtt
    TEKSTEN[f"gui.guhs.wistjedat.klusjes.{_id}"] = _wj

BLOKUITLEG = {
    "nl": "Een klein roze badje met pootjes. Een Blub-guh in de buurt hupt er af en toe zelf in om te badderen: bubbels!\n"
          "\n"
          "- Woont er een Blub met het klusje Bubbelbadmeester in een guhhuisje vlakbij, dan zet hij het badje klaar met schuim.\n"
          "- Met schuim gaan tamme guhs er af en toe in. Ze worden er fris en blij van: een poosje vaker hartjes en blije dansjes.\n"
          "- Het schuim is na een paar minuten weg; dan schuimt de Blub het weer op.\n"
          "- Craften: twee witte terracotta, een slijmbal en nog drie witte terracotta (T T / TST).",
    "en": "A little pink tub on feet. A Blub-guh nearby hops in by itself now and then for a splash: bubbles!\n"
          "\n"
          "- If a Blub with the chore Bubble bath master lives in a guhhuisje nearby, it gets the bath ready with foam.\n"
          "- With foam, tame guhs get in now and then. They feel fresh and happy: hearts and happy little dances for a while.\n"
          "- The foam is gone after a few minutes; then the Blub foams it up again.\n"
          "- Crafting: white terracotta, a slime ball and white terracotta (T T / TST).",
}


def _tex(kleur, donker, licht=None, bellen=False, seed=1):
    import random
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), kleur + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(14):
        x, y = rnd.randrange(16), rnd.randrange(16)
        d.point((x, y), fill=donker + (255,))
    if licht:
        for _ in range(10):
            x, y = rnd.randrange(16), rnd.randrange(16)
            d.point((x, y), fill=licht + (255,))
    if bellen:
        for _ in range(5):
            x, y = rnd.randrange(1, 14), rnd.randrange(1, 14)
            d.ellipse([x, y, x + 2, y + 2], outline=(255, 255, 255, 255))
    return img


def _icoon(soort):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # a little Blub in the bottom left corner (every chore icon has one)
    d.rectangle([1, 9, 8, 14], fill=BLAUW_L)
    d.rectangle([2, 10, 7, 13], fill=BLAUW)
    d.rectangle([2, 7, 2, 9], fill=BLAUW_L)
    d.rectangle([6, 7, 6, 9], fill=BLAUW_L)
    d.point((3, 11), fill=ST)
    d.point((6, 11), fill=ST)
    d.point((2, 15), fill=BLAUW)
    d.point((7, 15), fill=BLAUW)
    if soort == "bellen":                     # bubbles over a sprout
        d.rectangle([12, 10, 12, 14], fill=(90, 170, 80))
        d.rectangle([10, 9, 12, 10], fill=(110, 196, 96))
        d.rectangle([12, 8, 14, 9], fill=(110, 196, 96))
        for x, y, r in ((9, 2, 2), (13, 4, 1), (11, 6, 1)):
            d.ellipse([x - r, y - r, x + r, y + r], outline=WATER_D, fill=(214, 240, 252))
    elif soort == "knabbels":                 # a kaasknabbel and a heart
        d.ellipse([10, 9, 14, 13], fill=(250, 200, 70), outline=(214, 150, 40))
        d.point((11, 10), fill=(214, 150, 40))
        d.point((13, 12), fill=(214, 150, 40))
        d.rectangle([10, 2, 11, 3], fill=(240, 80, 120))
        d.rectangle([13, 2, 14, 3], fill=(240, 80, 120))
        d.rectangle([10, 4, 14, 4], fill=(240, 80, 120))
        d.rectangle([11, 5, 13, 5], fill=(240, 80, 120))
        d.point((12, 6), fill=(240, 80, 120))
    elif soort == "liedje":                   # a moon and a note
        d.ellipse([9, 1, 14, 6], fill=(250, 232, 140))
        d.ellipse([11, 0, 15, 5], fill=(0, 0, 0, 0))
        d.rectangle([12, 8, 12, 12], fill=(120, 90, 170))
        d.rectangle([12, 8, 14, 8], fill=(120, 90, 170))
        d.ellipse([10, 11, 12, 13], fill=(120, 90, 170))
    return img


def _badje_icoon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 7, 14, 12], fill=ROZE, outline=ROZE_D)
    d.rectangle([2, 7, 13, 8], fill=SCHUIM)
    d.rectangle([2, 13, 3, 14], fill=ROZE_D)
    d.rectangle([12, 13, 13, 14], fill=ROZE_D)
    for x, y in ((4, 4), (8, 3), (11, 5)):
        d.ellipse([x - 1, y - 1, x + 1, y + 1], outline=WATER_D)
    return img


def _effect_icoon():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for x, y, r in ((6, 10, 4), (12, 6, 3), (13, 13, 2)):
        d.ellipse([x - r, y - r, x + r, y + r], outline=WATER_D, fill=(220, 242, 252))
    d.rectangle([4, 2, 5, 3], fill=(240, 80, 120))
    d.rectangle([7, 2, 8, 3], fill=(240, 80, 120))
    d.rectangle([4, 4, 8, 4], fill=(240, 80, 120))
    d.rectangle([5, 5, 7, 5], fill=(240, 80, 120))
    d.point((6, 6), fill=(240, 80, 120))
    return img


def _el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down")):
    return {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}


def build(h):
    A, D, w = h.A, h.D, h.w
    h.save(_tex(ROZE, ROZE_D, ROZE_L, seed=11), "block", "blub_badje.png")
    h.save(_tex(WATER, WATER_D, (200, 236, 252), bellen=True, seed=12), "block", "blub_badje_water.png")
    h.save(_tex(SCHUIM, SCHUIM_D, None, bellen=True, seed=13), "block", "blub_badje_schuim.png")
    for naam, oppervlak in (("blub_badje", "#water"), ("blub_badje_schuim", "#schuim")):
        els = [_el([1, 0, 1], [15, 3, 15], "#tub"),
               _el([1, 3, 1], [15, 8, 2], "#tub"), _el([1, 3, 14], [15, 8, 15], "#tub"),
               _el([1, 3, 2], [2, 8, 14], "#tub"), _el([14, 3, 2], [15, 8, 14], "#tub"),
               _el([2, 3, 2], [14, 6.5 if naam == "blub_badje" else 7.5, 14], oppervlak, ("up",))]
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": {
            "tub": "guhs:block/blub_badje", "water": "guhs:block/blub_badje_water", "schuim": "guhs:block/blub_badje_schuim",
            "particle": "guhs:block/blub_badje"}, "elements": els})
    w(f"{A}/blockstates/blub_badje.json", {"variants": {"schuim=false": {"model": "guhs:block/blub_badje"},
                                                       "schuim=true": {"model": "guhs:block/blub_badje_schuim"}}})
    h.save(_badje_icoon(), "item", "blub_badje.png")
    h.item_model("blub_badje")
    h.self_drop("blub_badje")
    h.shaped("blub_badje", ["T T", "TST"], {"T": "minecraft:white_terracotta", "S": "minecraft:slime_ball"}, "guhs:blub_badje")
    for k in ("bellen", "knabbels", "liedje"):
        h.save(_icoon(k), "item", f"blubguh_klus_{k}.png")
        h.item_model(f"blubguh_klus_{k}")
    h.save(_effect_icoon(), "mob_effect", "fris_en_blij.png")


ITEMS = ["blub_badje", "blubguh_klus_bellen", "blubguh_klus_knabbels", "blubguh_klus_liedje"]
