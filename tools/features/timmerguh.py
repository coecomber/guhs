"""
3.0 (Guhverhalen), slice timmerguh: De Timmerguh en de huisjes-introductie (DESIGN_30 par. 1; Java: feature/timmerguh).

  - the bouwplaats at the edge of every Knuffeldal town (template knuffeldal_stadje/bouwplaats: timmerguh_bouw.py; its jigsaw
    on the west end of hoek_noordwest's street: knuffeldal_stadje.BOUWPLAATS_*): a half-built guhhuisje whose oortjesdak is
    still see-through ghost tiles (block timmerguh_dakplek, deel = dak / oor / binnenoor), scaffolding, a bouwkeet, a crane;
  - the Timmerguh (NPC timmerguh): his own model (a cream helmpje with a pink ridge and ear bumps, a pencil, a denim
    tuinbroek, a gereedschapsriem), his Guhdex page and all his lines of "Samen een huisje bouwen";
  - the items: timmerguh_bouwboekje (the key of the three guhhuisje recipes, tools/features/huisje.py) and the loaned
    timmerguh_dakpluisje;
  - the outfit (marker <timmerguh> in GuhClothes, source "timmerguh"): TIMMER_HELMPJE (HEAD) and TIMMER_GEREEDSCHAPSRIEM (BODY);
  - the read-only huisje screen's texts (gui.guhs.timmerguh.alleen_kijken*), advancements verhalen/timmerguh_* and the hidden
    quest/timmerguh_*, the FTB section "Samen een huisje bouwen", the game test template timmerguh_test_bouw.
"""
import os
import random

import numpy as np
from PIL import Image, ImageDraw

from features import timmerguh_bouw, verhaal

T = 16
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}

# =====================================================================================================================
# the outfit (make_guh_variants: BONES / clothes; make_clothes_icons: icons; make_resources: CLOTHES item models)
# =====================================================================================================================
_H = [0, 6, -2]
_B = [0, 6, 6]
BONES = {
    # the timmermanshelmpje: a cream dome with a rim and a klep at the front, a pink ridge over the top, a heart sticker
    "outfit_timmerhelm": ("head", _H, "timmerhelm", [([-5.2, 14.6, -11.2], [10.4, 2.4, 9.6], 0), ([-4.2, 17.0, -10.2], [8.4, 1.0, 7.6], 0),
                                                   ([-5.8, 14.6, -12.0], [11.6, 0.5, 11.0], 0), ([-4.4, 14.6, -13.8], [8.8, 0.5, 2.0], 0)]),
    "outfit_timmerhelm_kam": ("head", _H, "timmerhelm_kam", [([-0.7, 17.6, -10.6], [1.4, 0.8, 8.6], 0)]),
    "outfit_timmerhelm_sticker": ("head", _H, "timmerhelm_sticker", [([1.6, 15.2, -11.45], [2.2, 1.6, 0.4], 0)]),
    # the gereedschapsriem: a leather belt round the belly with a golden buckle, a hamertje hanging on the right, a yellow
    # duimstok (folding ruler) on the left and a little spijkerzakje
    "outfit_timmerriem": ("body", _B, "timmerriem", [([-6.9, 10.6, 4], [13.8, 0.8, 2.2], 0), ([6.3, 1.2, 4], [0.8, 9.6, 2.2], 0),
                                                    ([-7.1, 1.2, 4], [0.8, 9.6, 2.2], 0), ([-6.9, 0.6, 4], [13.8, 0.8, 2.2], 0),
                                                    ([-8.4, 6.2, 6.4], [1.4, 3.0, 2.8], 0)]),
    "outfit_timmerriem_gesp": ("body", _B, "timmerriem_gesp", [([-7.4, 4.6, 4.4], [0.6, 2.2, 1.4], 0)]),
    "outfit_timmerriem_hamer": ("body", _B, "timmerriem_hamer", [([7.1, 3.2, 4.8], [0.6, 5.2, 0.6], 0)]),
    "outfit_timmerriem_hamerkop": ("body", _B, "timmerriem_hamerkop", [([6.9, 7.8, 3.8], [1.0, 1.2, 2.6], 0)]),
    "outfit_timmerriem_duimstok": ("body", _B, "timmerriem_duimstok", [([-8.0, 2.0, 2.4], [0.6, 5.6, 1.2], 0)]),
}
CLOTHES = ["timmer_helmpje", "timmer_gereedschapsriem"]
HELM = (246, 238, 222)
KAM = (244, 132, 178)
LEER = (126, 80, 46)
DUIMSTOK = (246, 206, 60)


def _duimstok(rng, v):
    a = v.fabric(DUIMSTOK, rng, 6)
    for y in range(0, a.shape[0], 4):
        a[y, :] = (60, 40, 30)                     # the marks of the ruler
    for y in range(0, a.shape[0], 8):
        a[y:y + 2, a.shape[1] // 2:] = (60, 40, 30)
    return a


def _sticker(rng, v):
    a = v.fabric(HELM, rng, 4)
    px = a.shape[0]
    for y in range(px):
        for x in range(px):
            u, w = (x - px / 2 + 0.5) / (px / 2), (y - px / 2 + 0.5) / (px / 2)
            if (u * u + w * w - 0.5) ** 3 - u * u * (-w) ** 3 <= 0:          # a little heart
                a[y, x] = (236, 70, 130)
    return a


def clothes(rng, v):
    return {
        "timmer_helmpje": {"timmerhelm": lambda: v.fabric(HELM, rng, 8),
                           "timmerhelm_kam": lambda: v.fabric(KAM, rng, 10),
                           "timmerhelm_sticker": lambda: _sticker(rng, v)},
        "timmer_gereedschapsriem": {"timmerriem": lambda: v.fabric(LEER, rng, 10),
                                    "timmerriem_gesp": lambda: v.metal((246, 200, 70), rng),
                                    "timmerriem_hamer": lambda: v.fabric((170, 112, 64), rng, 8),
                                    "timmerriem_hamerkop": lambda: v.metal((150, 156, 168), rng),
                                    "timmerriem_duimstok": lambda: _duimstok(rng, v)},
    }


HELM_ICON = ["................", "................", "................", "......kkkk......", "....bbkkkkbb....", "...bbbbkkbbbb...",
             "..bbbbbkkbbbbb..", "..bbbhhbkbbbbb..", "..bbbhhbkbbbbb..", ".bbbbbbbkbbbbbb.", "aaaaaaaaaaaaaaaa", "aaaaaaaaaaaaa...",
             "................", "................", "................", "................"]
RIEM_ICON = ["................", "................", "...........ss...", "...........ss...", "..........mmmm..", "...........hh...",
             "cccccccgggccchhc", "aaaaaaagyga.ahha", "cccccccgggccchhc", "..dd.....pp.....", "..dd....pppp....", "..dd....pppp....",
             "..dd.....pp.....", "..dd............", "................", "................"]


def icons(ic):
    return {
        "timmer_helmpje": ic.icon(HELM_ICON, {"a": (206, 190, 170), "b": HELM, "k": KAM, "h": (236, 70, 130)}),
        "timmer_gereedschapsriem": ic.icon(RIEM_ICON, {"a": (90, 56, 30), "c": LEER, "g": (246, 200, 70), "y": (255, 240, 150),
                                                       "h": (170, 112, 64), "m": (150, 156, 168), "s": (190, 196, 206),
                                                       "d": DUIMSTOK, "p": (140, 96, 60)}),
    }


# =====================================================================================================================
# textures
# =====================================================================================================================
DEEL_KLEUR = {"dak": (238, 170, 214), "oor": (250, 160, 200), "binnenoor": (214, 80, 172)}


def _ghost(kleur):
    """A see-through ghost tile: a dashed rim, soft dots, a little white heart in the middle ("hier moet nog iets")."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    for y in range(T):
        for x in range(T):
            edge = x in (0, 15) or y in (0, 15)
            if edge and (x + y) % 4 < 3:
                img.putpixel((x, y), kleur + (215,))
            elif (x * 3 + y) % 7 == 0:
                img.putpixel((x, y), kleur + (105,))
            else:
                img.putpixel((x, y), kleur + (48,))
    hart = [".hh.hh.", "hhhhhhh", "hhhhhhh", ".hhhhh.", "..hhh..", "...h..."]
    for dy, row in enumerate(hart):
        for dx, ch in enumerate(row):
            if ch == "h":
                img.putpixel((4 + dx, 5 + dy), (255, 255, 255, 230))
    return img


def _bouwboekje():
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((3, 1, 13, 14), fill=(236, 120, 170, 255), outline=(150, 50, 100, 255))     # the cover
    d.rectangle((3, 1, 4, 14), fill=(170, 60, 116, 255))                                     # the spine
    d.rectangle((13, 2, 14, 14), fill=(250, 244, 230, 255))                                  # the pages
    for y in range(3, 14, 2):
        img.putpixel((14, y), (214, 200, 180, 255))
    # a little guhhuisje on the cover: a round head with two ears, a door
    d.ellipse((6, 5, 12, 11), fill=(255, 226, 238, 255))
    for (x, y) in ((6, 4), (7, 4), (11, 4), (12, 4), (6, 5), (12, 5)):
        img.putpixel((x, y), (255, 226, 238, 255))
    for (x, y) in ((8, 7), (10, 7)):
        img.putpixel((x, y), (40, 20, 40, 255))
    for (x, y) in ((9, 9), (9, 10)):
        img.putpixel((x, y), (170, 90, 60, 255))
    # a carpenter's pencil across the corner
    for i in range(5):
        img.putpixel((10 + i, 15 - i), (246, 206, 60, 255))
    img.putpixel((15, 10), (60, 40, 30, 255))
    return img


def _dakpluisje():
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rng = random.Random(3006)
    d.rounded_rectangle((1, 4, 14, 13), radius=4, fill=(238, 160, 206, 255), outline=(190, 100, 156, 255))
    for (x, y, r) in ((4, 5, 3), (8, 4, 3), (12, 5, 3)):                                     # fluffy top
        d.ellipse((x - r, y - r, x + r, y + r), fill=(250, 190, 224, 255))
    for _ in range(18):
        x, y = rng.randint(2, 13), rng.randint(3, 12)
        img.putpixel((x, y), (255, 222, 240, 255))
    for (x, y) in ((6, 9), (9, 9)):
        img.putpixel((x, y), (60, 30, 50, 255))
    for (x, y) in ((7, 10), (8, 10)):
        img.putpixel((x, y), (200, 80, 130, 255))
    return img


def textures(h):
    for deel, kleur in DEEL_KLEUR.items():
        h.save(_ghost(kleur), "block", f"timmerguh_dakplek_{deel}.png")
    h.save(_bouwboekje(), "item", "timmerguh_bouwboekje.png")
    h.save(_dakpluisje(), "item", "timmerguh_dakpluisje.png")


def models(h):
    A, w = h.A, h.w
    for deel in DEEL_KLEUR:
        w(f"{A}/models/block/timmerguh_dakplek_{deel}.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                                               "textures": {"all": f"guhs:block/timmerguh_dakplek_{deel}"}})
    w(f"{A}/blockstates/timmerguh_dakplek.json", {"variants": {f"deel={d}": {"model": f"guhs:block/timmerguh_dakplek_{d}"} for d in DEEL_KLEUR}})
    h.item_model("timmerguh_bouwboekje")
    h.item_model("timmerguh_dakpluisje")


# =====================================================================================================================
# the Timmerguh himself (a sitting guh with his own bones; the page, the seen advancement)
# =====================================================================================================================
def npc(h):
    from features import beroepen_tex
    H = [0, 13, 0]
    B = [0, 12, -4]
    beroepen_tex._npc(h, "timmerguh", 0.93, 0.95, 1.0, {
        # a cream helmpje with a pink ridge, two little round ear bumps on top and a klep
        "timmer_helm": (("head", H), [([-7.0, 25.2, -7.6], [14, 0.8, 13.4], "helm", 0), ([-6.0, 26.0, -6.0], [12, 4.2, 11], "helm", 0),
                                      ([-0.7, 29.8, -6.2], [1.4, 0.9, 11.4], "kam", 0), ([-6.2, 25.2, -9.4], [12.4, 0.8, 2.0], "helm", 0),
                                      ([-5.4, 30.0, -1.6], [2.6, 1.4, 2.6], "kam", 0), ([2.8, 30.0, -1.6], [2.6, 1.4, 2.6], "kam", 0),
                                      ([2.0, 27.0, -6.4], [2.4, 1.8, 0.5], "hart", 0)]),
        "timmer_potlood": (("head", H), [([5.0, 22.0, -3.0], [0.8, 0.8, 5.0], "potlood", 0), ([5.0, 22.0, -3.6], [0.8, 0.8, 0.6], "punt", 0)]),
        # a denim tuinbroek: the bib and two straps with yellow buttons
        "timmer_tuinbroek": (("body", B), [([-3.8, 2.2, -4.6], [7.6, 7.2, 0.8], "denim", 0), ([-3.6, 9.2, -4.6], [1.2, 2.6, 0.7], "denim", 0),
                                           ([2.4, 9.2, -4.6], [1.2, 2.6, 0.7], "denim", 0), ([-3.5, 8.6, -4.9], [1.0, 1.0, 0.4], "knoop", 0),
                                           ([2.5, 8.6, -4.9], [1.0, 1.0, 0.4], "knoop", 0), ([-1.4, 4.4, -4.9], [2.8, 2.0, 0.4], "zakje", 0)]),
        # the gereedschapsriem: leather, a hamertje on the right, a yellow duimstok on the left
        "timmer_riem": (("body", B), [([-5.6, 1.8, -4.8], [11.2, 1.2, 9.6], "riem", 0), ([3.4, 0.6, -5.4], [1.0, 2.8, 1.0], "steel", 0),
                                      ([2.6, 3.2, -5.6], [2.6, 1.2, 1.4], "hamerkop", 0), ([-4.6, 0.0, -5.4], [1.2, 3.4, 0.6], "duimstok", 0)]),
    }, {"helm": (HELM, 6, None), "kam": (KAM, 6, None), "hart": ((236, 70, 130), 3, None), "potlood": ((246, 190, 60), 6, None),
        "punt": ((60, 40, 30), 3, None), "denim": ((70, 110, 180), 10, None), "knoop": ((246, 206, 60), 4, None),
        "zakje": ((56, 92, 160), 6, None), "riem": (LEER, 6, None), "steel": ((170, 112, 64), 6, None), "hamerkop": ((140, 146, 158), 5, None),
        "duimstok": (DUIMSTOK, 5, None)}, 3006)
    for key, nl in NPC_TEXTS.items():
        h.lang(key, nl, nl)
    h.w(f"{h.D}/advancement/quest/seen_timmerguh.json", {"criteria": IMPOSSIBLE})


NPC_TEXTS = {
    "entity.guhs.guh_npc.timmerguh": "Timmerguh",
    "gui.guhs.guhdex.rarity.timmerguh": "Zeldzaamheid: één in elk Knuffeldal-stadje (op de bouwplaats, aan het eind van de straat langs het huisje van Cocotje)",
    "gui.guhs.guhdex.info.timmerguh": "De Timmerguh bouwt aan de rand van elk Knuffeldal-stadje een guhhuisje: een huisje in de vorm van "
                                      "een guhhoofd, met oortjes als dak. Zijn helmpje zit altijd een beetje scheef en hij neuriet bij elke "
                                      "tik van zijn hamertje. Help je mee met het oortjesdak? Dan leert hij je zelf huisjes timmeren, in "
                                      "alle drie de maten. Samen bouwen is dubbel zo vadsig, njeg!",
}


# =====================================================================================================================
# advancements (tab guhs:verhalen/) and the hidden quest ones (FTB tasks)
# =====================================================================================================================
QUEST_ADVANCEMENTS = ["timmerguh_gesproken", "timmerguh_materiaal", "timmerguh_dak", "timmerguh_bewoner", "timmerguh_klaar", "timmerguh_knus"]


def advancements(h):
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    z = verhaal.zichtbaar
    z(h, "verhalen", "timmerguh_bouwplaats", "root", "minecraft:oak_planks", "task", "Hallo, Timmerguh!",
      "Vind de bouwplaats aan de rand van een Knuffeldal-stadje en maak kennis met de Timmerguh")
    z(h, "verhalen", "timmerguh_dak", "timmerguh_bouwplaats", "guhs:timmerguh_dakpluisje", "task", "De vlag in top!",
      "Leg samen met de Timmerguh het oortjesdak op zijn halve guhhuisje")
    z(h, "verhalen", "timmerguh_bewoner", "timmerguh_dak", "guhs:guhhuisje_klein", "task", "Ons eerste huisje",
      "Laat een van je guhs wonen in het huisje dat je van de Timmerguh kreeg")
    z(h, "verhalen", "timmerguh_klaar", "timmerguh_bewoner", "guhs:timmerguh_bouwboekje", "goal", "Samen een huisje bouwen",
      "Krijg het bouwboekje van de Timmerguh: nu timmer je zelf alle drie de Guhhuisjes!")
    z(h, "verhalen", "timmerguh_knus", "timmerguh_klaar", "guhs:klusjes_guhlampje", "task", "Knus ingericht",
      "Zet een speeltje en een guhlampje bij een van je Guhhuisjes (een extra klusje van de Timmerguh)")


# =====================================================================================================================
# texts
# =====================================================================================================================
Q = "quest.guhs.timmerguh."
TEXTS = {
    "block.guhs.timmerguh_dakplek": "Dakplekje (hier moet nog dak op!)",
    "item.guhs.timmerguh_bouwboekje": "Bouwboekje van de Timmerguh",
    "item.guhs.timmerguh_bouwboekje.lore": "Alle drie de Guhhuisjes staan erin: klein, medium en groot.",
    "item.guhs.timmerguh_bouwboekje.lore2": "Blijft gewoon in je werkbank liggen als je een huisje timmert. Tik tik, njeg!",
    "item.guhs.timmerguh_dakpluisje": "Dakpluisje",
    "item.guhs.timmerguh_dakpluisje.lore": "Geleend van de Timmerguh. Past alleen op de doorzichtige plekjes van zijn oortjesdak.",
    "item.guhs.timmer_helmpje": "Timmermanshelmpje",
    "item.guhs.timmer_gereedschapsriem": "Gereedschapsriem",
    # the signs on the bouwplaats
    "sign.guhs.timmerguh_wegwijzer1": "Naar de",
    "sign.guhs.timmerguh_wegwijzer2": "bouwplaats!",
    "sign.guhs.timmerguh_wegwijzer3": "(Timmerguh)",
    "sign.guhs.timmerguh_bord1": "Hier bouwt de",
    "sign.guhs.timmerguh_bord2": "Timmerguh een",
    "sign.guhs.timmerguh_bord3": "knus guhhuisje!",
    "sign.guhs.timmerguh_bord4": "Help je mee? Njeg!",
    "sign.guhs.timmerguh_keet1": "Bouwkeet",
    "sign.guhs.timmerguh_keet2": "Eerst... kaas!",
    # messages
    "gui.guhs.timmerguh.past_niet": "Dat dakpluisje past alleen op de doorzichtige plekjes van het oortjesdak van de Timmerguh.",
    "gui.guhs.timmerguh.nog": "Pluf! Nog %s plekjes, dan zit het oortjesdak erop!",
    "gui.guhs.timmerguh.vertel_het": "%s woont nu in je huisje! Ga het snel aan de Timmerguh vertellen, njeg!",
    "gui.guhs.timmerguh.alleen_kijken": "Alleen de eigenaar mag dit huisje aanpassen. Jij mag wel lekker kijken, njeg!",
    "gui.guhs.timmerguh.alleen_kijken_kort": "(alleen kijken)",
    # the talking screen
    Q + "hallo": "Hoi hoi! Ik ben de Timmerguh. Tik tik, njeg! Zie je dat halve huisje? Het is een guhhuisje, in de vorm van een "
                 "guhhoofd. De muurtjes staan al, maar het oortjesdak... dat krijg ik in mijn eentje niet af.",
    Q + "uitleg": "Een guhhuisje is een thuis voor je tamme guhs (en muisjes en schildpadjes). Ze slapen erin, spelen in de buurt en "
                  "doen er klusjes. Als je me helpt met dit dak, leer ik je hoe je ze zelf timmert. In alle drie de maten!",
    Q + "vraag": "Joepie! Voor het dak heb ik %s planken nodig (van elk hout) en %s roze wol: daar maak ik dakpluisjes van. "
                 "Breng je ze? Dan zetten we samen het oortjesdak erop.",
    Q + "nodig": "Heb je de spulletjes al? Planken: %s van de %s. Roze wol: %s van de %s. Het mag allemaal door elkaar, als het "
                 "maar planken zijn. Njeg!",
    Q + "materiaal_heb": "Ooh, ik zie %s planken en %s roze wol! Mag ik ze hebben? Dan maak ik er meteen dakpluisjes van.",
    Q + "dak_uitleg": "Tik tik tik... klaar! Hier zijn %s dakpluisjes. Klim de steiger op (de ladder bij de bouwkeet) en rechtsklik "
                      "elk doorzichtig plekje op het dak met een dakpluisje. Vergeet de oortjes niet!",
    Q + "nieuw_dak": "Het vorige huisje is af... en er is meteen een guhgezinnetje ingetrokken, njeg! Dus zetten we er gewoon nog een "
                     "dak op. Hier zijn %s dakpluisjes: klim de steiger op en rechtsklik elk doorzichtig plekje.",
    Q + "dak_nog": "Nog %s plekjes, dan zit het oortjesdak erop! Klim de steiger op en rechtsklik de doorzichtige plekjes met je "
                   "dakpluisjes. Ik hou de ladder vast, beloofd.",
    Q + "dak_af": "DE VLAG IN TOP! Het oortjesdak zit erop, njeg! Weet je wat? Dit kleine huisje is voor jou: ons eerste huisje. "
                  "Zet het bij je thuis neer en laat een van je guhs erin wonen. Kom het me daarna vertellen!",
    Q + "hint.bewoner": "zet het kleine Guhhuisje neer bij je thuis, laat een van je guhs erin wonen en vertel het de Timmerguh",
    Q + "bewoner_nog": "Woont er al een guh in je huisje? Zet het neer, rechtsklik het en kies Nieuwe bewoner (of rechtsklik het met "
                       "een opgepakte guh). Daarna kom je het me vertellen, njeg!",
    Q + "klaar": "Er woont een guh in ons huisje! VAHOEG! Jij bent nu een echte timmerguh. Hier: mijn bouwboekje (daar staan alle "
                 "drie de Guhhuisjes in: klein, medium en groot), nog een klein huisje cadeau en een timmermanshelmpje voor je guh!",
    Q + "knus_tip": "Wil je het huisje nog knusser maken? Zet een speeltje (een glijbaantje, een wip, een schommel, een tunnel of een "
                    "knabbelbal) en een guhlampje bij je huisje, in het thuisgebied. Dan heb ik nog iets moois voor je!",
    Q + "knus_klaar": "Een speeltje en een guhlampje: wat KNUS! Daar worden je guhs heel vadsig blij van. Hier, mijn eigen "
                      "gereedschapsriem, met een hamertje en een duimstok. Tik tik, njeg!",
    Q + "boekje_kwijt": "Ben je mijn bouwboekje kwijt? Geeft niks hoor, ik heb er nog eentje. Hier, njeg!",
    Q + "bedankt0": "Tik tik, njeg! Hoe gaat het met je huisjes? Zijn ze nog knus?",
    Q + "bedankt1": "Weet je wat het mooiste van een guhhuisje is? De oortjes. Altijd de oortjes. Njeg!",
    Q + "bedankt2": "Samen bouwen is dubbel zo vadsig! Kom je weer eens helpen? Er is altijd wel een dak te doen.",
    Q + "niks": "Tik tik... huh? Mijn bouwtekening is zoek, en ik weet niet meer waar het dak moet. Kom straks nog eens terug, njeg!",
    Q + "optie.help": "Ik help je mee!",
    Q + "optie.wat": "Wat is een guhhuisje?",
    Q + "optie.oke": "Komt goed!",
    Q + "optie.inleveren": "Hier, alsjeblieft!",
    Q + "optie.aan_de_slag": "Aan de slag!",
    Q + "optie.dankjewel": "Dankjewel, Timmerguh!",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# the game test room, the self-check
# =====================================================================================================================
TEST_TILES = [((5, 3, 6), "dak"), ((6, 3, 6), "dak"), ((7, 3, 6), "dak"), ((5, 3, 7), "dak"), ((6, 4, 7), "oor"), ((7, 3, 7), "binnenoor")]


def test_templates(h):
    """timmerguh_test_bouw (14 x 8 x 14): grass, and a little stone roof with six ghost tiles (4 dak, an ear, an inner ear)."""
    t = h.Structure((14, 8, 14))
    for x in range(14):
        for z in range(14):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    for x in range(5, 8):
        for z in range(6, 8):
            for y in (1, 2):
                t.set(x, y, z, "minecraft:stone")
    t.set(6, 3, 7, "minecraft:stone")
    for (pos, deel) in TEST_TILES:
        t.set(*pos, timmerguh_bouw.DAKPLEK, {"deel": deel})
    t.save("timmerguh_test_bouw")


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for p in ([f"{A}/blockstates/timmerguh_dakplek.json", f"{A}/models/item/timmerguh_bouwboekje.json",
               f"{A}/models/item/timmerguh_dakpluisje.json", f"{A}/geo/entity/guh_npc_timmerguh.geo.json",
               os.path.join(h.TEX, "entity", "npc_timmerguh.png"), f"{D}/worldgen/template_pool/knuffeldal_stadje/bouwplaats.json"]
              + [os.path.join(h.TEX, "block", f"timmerguh_dakplek_{d}.png") for d in DEEL_KLEUR]
              + [os.path.join(h.TEX, "item", f"timmerguh_{i}.png") for i in ("bouwboekje", "dakpluisje")]):
        if not os.path.exists(p):
            missing.append(p)
    for key in list(TEXTS) + list(NPC_TEXTS):
        if key not in h.NL:
            missing.append(f"lang {key}")
    for m in ("klein", "medium", "groot"):          # the huisje recipes need the bouwboekje (huisje.py)
        path = f"{D}/recipe/guhhuisje_{m}.json"
        if not os.path.exists(path) or "guhs:timmerguh_bouwboekje" not in open(path, encoding="utf-8").read():
            missing.append(f"recipe guhhuisje_{m} without the bouwboekje")
    if missing:
        raise SystemExit(f"timmerguh assets missing: {missing}")


def build(h):
    textures(h)
    models(h)
    npc(h)
    timmerguh_bouw.make(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_verhalen, section "Samen een huisje bouwen")
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("timmerguh_start", "De Timmerguh", "Aan de rand van elk &dKnuffeldal-stadje&r (loop de straat langs het huisje van Cocotje helemaal uit: volg het bordje) "
      "ligt een bouwplaats. Daar bouwt de &6Timmerguh&r een guhhuisje: een huisje in de vorm van een guhhoofd. Zeg eens hallo!",
      "guhs:timmerguh_dakpluisje", [adv("timmerguh_gesproken")], rewards=(("guhs:kaas_knabbels", 8),), shape="circle", xp=50)
    q("timmerguh_materiaal", "Planken en roze wol", "De Timmerguh heeft &616 planken&r (van elk hout) en &d8 roze wol&r nodig voor "
      "het oortjesdak. Breng ze naar de bouwplaats!", "minecraft:pink_wool", [adv("timmerguh_materiaal")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["timmerguh_start"], xp=50)
    q("timmerguh_dak", "De vlag in top!", "Klim de steiger op en rechtsklik elk doorzichtig plekje van het dak met een &ddakpluisje&r. "
      "Vergeet de oortjes niet! Als het laatste plekje dicht is, gaat de vlag in top.", "guhs:pluisdak", [adv("timmerguh_dak")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 2),), deps=["timmerguh_materiaal"], xp=100)
    q("timmerguh_bewoner", "Ons eerste huisje", "Van de Timmerguh krijg je een klein &dGuhhuisje&r. Zet het neer bij je thuis en laat "
      "een van je tamme guhs erin wonen (rechtsklik het huisje: Nieuwe bewoner).", "guhs:guhhuisje_klein", [adv("timmerguh_bewoner")],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["timmerguh_dak"], xp=100)
    q("timmerguh_klaar", "Samen een huisje bouwen", "Vertel het de Timmerguh: je krijgt zijn &6bouwboekje&r! Daarmee maak je zelf alle "
      "drie de Guhhuisjes (het boekje blijft in je werkbank liggen). Plus nog een klein huisje cadeau en een timmermanshelmpje.",
      "guhs:timmerguh_bouwboekje", [adv("timmerguh_klaar")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),),
      deps=["timmerguh_bewoner"], shape="gear", xp=250)
    q("timmerguh_helmpje", "Timmermanshelmpje", "Een crèmekleurig helmpje met een roze kam en een hartjessticker. Houd het vast "
      "(rechtsklik ingedrukt) om het te ontgrendelen, en trek het je guh aan in de kledingkast!", "guhs:timmer_helmpje",
      [item("guhs:timmer_helmpje")], rewards=(("guhs:kaas_knabbels", 6),), deps=["timmerguh_klaar"])
    q("timmerguh_knus", "Knus ingericht", "Een extra klusje van de Timmerguh: zet een &dspeeltje&r (glijbaantje, wip, schommel, tunnel "
      "of knabbelbal) en een &6guhlampje&r in het thuisgebied van een van je huisjes, en vertel het hem.", "guhs:klusjes_guhlampje",
      [adv("timmerguh_knus")], rewards=(("guhs:marshmallow_knabbel", 3),), deps=["timmerguh_klaar"], xp=100)
    q("timmerguh_riem", "Gereedschapsriem", "De gereedschapsriem van de Timmerguh zelf, met een hamertje, een gele duimstok en een "
      "spijkerzakje. Tik tik, njeg!", "guhs:timmer_gereedschapsriem", [item("guhs:timmer_gereedschapsriem")],
      rewards=(("guhs:kaas_knabbels", 6),), deps=["timmerguh_knus"])
