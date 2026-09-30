"""
Piep (2.8.1) - the resources of feature/piep: the pieppiepmuisje, Poepschilly, the roze guh koek, the boze kaasknabbels and
their nest. The models are the user's (blockbench/*.bbmodel, exported to assets/guhs/geo|animations|textures/entity; the
code that first made them is piep_modellen.py), the nest template is piep_nest.py.

  build(h)           textures (icons, particles, effect icons), the koek block's blockstate/loot, item models, sounds,
                     lang (Dutch in both files), loot tables, tags, advancements, the Poepschilly spawns, the nest structure,
                     the gametest rooms, and roze guh koek trays on tables in a few lieve buildings
  ftb(fq)            the quests; make_ftbquests puts them in the chapter guhs_piep ("Piep!") of the Guhs group
"""
import gzip
import math
import os
import random
import struct

import numpy as np
from PIL import Image, ImageDraw

from features import piep_modellen as modellen
from features import piep_nest as nest

NAME = nest.NAME
CHAPTER_VERSION = 15
SALT = 20281001
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
AMBIENT = [f"piep_ambient{i}" for i in range(1, 6)]

QUEST = ["piep_muisje_geaaid", "piep_muisje_geaaid_10", "piep_muisje_getamed", "piep_verstoppertje_3", "piep_schilly_gevonden",
         "piep_schilly_getamed", "piep_schilly2_gevonden", "piep_schilly2_getamed", "piep_schilly_bestie", "piep_schilly_beef", "piep_poetsbeurt", "piep_nest_gevonden", "piep_nest_gewonnen", "piep_recept", "piep_koek"]

SOUNDS = {
    "entity.pieppiepmuisje.ambient": [f"guhs:{s}" for s in AMBIENT],
    "entity.pieppiepmuisje.hurt": ["guhs:piep_hurt"],
    "entity.poepschilly.poets": [{"name": "minecraft:block.bubble_column.whirlpool_inside", "type": "event"},
                                 {"name": "guhs:piep_ambient2", "pitch": 0.8}],
    "entity.poepschilly.plop": [{"name": "minecraft:block.bubble_column.bubble_pop", "type": "event"},
                                {"name": "minecraft:entity.chicken.egg", "type": "event"}],
    "entity.boze_kaasknabbel.ambient": [{"name": "guhs:piep_ambient1", "pitch": 0.55}, {"name": "guhs:piep_ambient4", "pitch": 0.6},
                                        {"name": "minecraft:entity.slime.squish_small", "type": "event"}],
    "entity.boze_kaasknabbel.zieli": [{"name": "guhs:piep_ambient3", "pitch": 0.5}, {"name": "guhs:piep_ambient5", "pitch": 0.55}],
}

LANG = {
    # the creatures
    "entity.guhs.pieppiepmuisje": "Pieppiepmuisje",
    "entity.guhs.poepschilly": "Poepschilly",
    "entity.guhs.schilly": "Schilly",
    "item.guhs.schilly_spawn_egg": "Schilly-spawnei",
    "effect.guhs.besties": "Besties",
    "entity.guhs.boze_kaasknabbel": "Boze Kaasknabbel",
    "entity.guhs.boze_oppernabbel": "Boze Oppernabbel",
    "item.guhs.pieppiepmuisje_spawn_egg": "Pieppiepmuisje-spawnei",
    "item.guhs.poepschilly_spawn_egg": "Poepschilly-spawnei",
    "item.guhs.boze_kaasknabbel_spawn_egg": "Boze Kaasknabbel-spawnei",
    "item.guhs.boze_oppernabbel_spawn_egg": "Boze Oppernabbel-spawnei",
    "item.guhs.pieppiepmuisje_item": "Pieppiepmuisje",
    "item.guhs.pieppiepmuisje_item.tooltip": "Rechtsklik op een blok: het muisje hopt eraf. Rechtsklik in de lucht: op je schouder! Piep.",
    "item.guhs.poepschilly_item": "Poepschilly",
    "item.guhs.poepschilly_item.tooltip": "Een opgepakte Poepschilly (met naam en al). Rechtsklik op een blok: hij waggelt er weer vanaf. Blub!",
    "item.guhs.schilly_item": "Schilly",
    "item.guhs.schilly_item.tooltip": "Een opgepakte Schilly (met naam en al). Rechtsklik op een blok: daar is Schilly weer. Minihoofdje!",
    # the koek
    "block.guhs.roze_guh_koek": "Roze Guh Koek",
    "item.guhs.roze_guh_koek": "Roze Guh Koek",
    "block.guhs.roze_guh_koek.tooltip": "Eet hem op: Lief kijken! Zet hem neer: een bakje met plek voor zes. Lege hand: eentje opsmikkelen, sluipen: eentje pakken.",
    "item.guhs.roze_guh_koek_recept": "Recept: Roze Guh Koek",
    "item.guhs.roze_guh_koek_recept.tooltip": "Rechtsklik om het geheime recept te leren. Daarna bak je hem in de Knabbeloven: zoetdeeg, plaatje, glazuur. Njeg!",
    "effect.guhs.fris_van_binnen": "Fris van binnen",
    "effect.guhs.lief_kijken": "Lief kijken",
    # messages
    "gui.guhs.piep.muisje_getamed": "Piep piep! Het muisje is nu van jou. VAHOEG!",
    "gui.guhs.piep.verstoppertje": "%s wil verstoppertje spelen... piep! Zoek het muisje!",
    "gui.guhs.piep.gevonden": "Gevonden! Piep piep piep! (een blij muisjesdansje)",
    "gui.guhs.piep.niet_gevonden": "Het muisje komt zelf maar tevoorschijn... piep. (Een tikje trots.)",
    "gui.guhs.piep.opgepakt": "Je pakt het muisje op. Het past precies in je hand. Piep!",
    "gui.guhs.piep.op_schouder": "Het muisje klimt op je schouder. Piep! (Sluip + rechtsklik op een blok: eraf.)",
    "gui.guhs.piep.schouder_vol": "Er zit al een muisje op je schouder, njeg.",
    "gui.guhs.piep.poepschilly_klaar": "Poepschilly heeft er zin in! Rechtsklik nu (lege hand) op een van je tamme guhs...",
    "gui.guhs.piep.poepschilly_rust": "Poepschilly rust even uit van het poetsen (nog %s:%s).",
    "gui.guhs.piep.poepschilly_getamed": "Poepschilly is nu je maatje! Tijd voor een poetsbeurt?",
    "gui.guhs.piep.schilly_klaar": "Schilly wil een bestie-moment! Rechtsklik nu (lege hand) op een van je tamme guhs...",
    "gui.guhs.piep.schilly_rust": "Schilly heeft even genoeg geknuffeld (nog %s:%s).",
    "gui.guhs.piep.schilly_getamed": "Schilly is nu je maatje! (Een minihoofdje, maar wel een lieve.)",
    "gui.guhs.piep.bestie_moment": "Schilly en %s: echte besties! <3",
    "gui.guhs.piep.beef": "Schilly en %s hebben beef... (minihoofdje!)",
    "gui.guhs.piep.beef_bijgelegd": "...toch besties <3",
    "gui.guhs.piep.schilly_alleen_tam": "Poepschilly kruipt alleen in guhs die van jou zijn.",
    "gui.guhs.piep.al_fris": "%s is al helemaal fris van binnen!",
    "gui.guhs.piep.schilly_waggelt": "Poepschilly waggelt naar het kontje van %s... poets poets!",
    "gui.guhs.piep.schilly_kruipt": "Plop! Poepschilly kruipt in het kontje van %s... het kriebelt! Guh guh guhhh!",
    "gui.guhs.piep.fris": "Kontje van %s weer blinkend schoon, VAHOEG! (fris van binnen)",
    "gui.guhs.piep.schilly_afgebroken": "Plop! Poepschilly is er alweer uit. De poetsbeurt is een andere keer, njeg.",
    # the little menus (piepmenu) and picking up
    "gui.guhs.piep.niet_jouw_maatje": "Dit is niet jouw maatje, njeg. Alleen het baasje mag dat.",
    "gui.guhs.piep.even_bezig": "%s is even bezig. Straks weer!",
    "gui.guhs.piep.opgepakt.pieppiepmuisje": "Je pakt %s op. Het past precies in je hand. Piep!",
    "gui.guhs.piep.opgepakt.poepschilly": "Je pakt %s op. Hij trekt zijn pootjes in: blub!",
    "gui.guhs.piep.opgepakt.schilly": "Je pakt %s op. Schilly kijkt een beetje beledigd (minihoofdje), maar vindt het stiekem fijn.",
    "gui.guhs.piep.neergezet.pieppiepmuisje": "%s hopt eraf. Piep piep!",
    "gui.guhs.piep.neergezet.poepschilly": "%s waggelt er weer vandoor. Blub!",
    "gui.guhs.piep.neergezet.schilly": "Daar is %s weer! Vahoeg!",
    "gui.guhs.piep.menu.sub.pieppiepmuisje": "Pieppiepmuisje: klein, pluizig, heel erg lief",
    "gui.guhs.piep.menu.sub.poepschilly": "Poepschilly: de kontpoetser van de guhzee",
    "gui.guhs.piep.menu.sub.schilly": "Schilly: minihoofdje, maar een echte bestie",
    "gui.guhs.piep.menu.aan": "aan",
    "gui.guhs.piep.menu.uit": "uit",
    "gui.guhs.piep.menu.naam": "Naam veranderen",
    "gui.guhs.piep.menu.naam.tooltip": "Typ een nieuwe naam en druk op Oké (of Enter). Leeg = geen naam. Geen naambordje nodig!",
    "gui.guhs.piep.menu.naam_oke": "Oké",
    "gui.guhs.piep.menu.rondvadsen": "Rondvadsen: %s",
    "gui.guhs.piep.menu.rondvadsen.tooltip": "Aan: lekker rondscharrelen. Uit: blijft precies hier zitten tot jij het weer zegt.",
    "gui.guhs.piep.menu.rondvadsen.aan": "%s gaat weer lekker rondvadsen!",
    "gui.guhs.piep.menu.rondvadsen.uit": "%s blijft hier zitten. Braaf!",
    "gui.guhs.piep.menu.volgen": "Volg mij: %s",
    "gui.guhs.piep.menu.volgen.tooltip": "Aan: loopt (of zwemt) achter je aan. Uit: blijft in de buurt van zijn eigen plekje.",
    "gui.guhs.piep.menu.volgen.aan": "%s volgt je weer overal. Waggel waggel!",
    "gui.guhs.piep.menu.volgen.uit": "%s blijft lekker in de buurt van hier.",
    "gui.guhs.piep.menu.piepjes": "Piepjes: %s",
    "gui.guhs.piep.menu.piepjes.tooltip": "Het muisje piept af en toe. Uit: een stil muisje (bij verstoppertje piept het wel, anders vind je het nooit).",
    "gui.guhs.piep.menu.piepjes.aan": "%s piept weer. Piep piep!",
    "gui.guhs.piep.menu.piepjes.uit": "%s is heel stil. ...piep. (sorry)",
    "gui.guhs.piep.menu.verstoppen": "Verstoppertje: %s",
    "gui.guhs.piep.menu.verstoppen.tooltip": "Aan: het muisje verstopt zich soms in een bloempot of mandje en piept tot je het vindt.",
    "gui.guhs.piep.menu.verstoppen.aan": "%s wil weer verstoppertje spelen!",
    "gui.guhs.piep.menu.verstoppen.uit": "%s verstopt zich even niet meer.",
    "gui.guhs.piep.menu.zwemmen": "Zwemmen: %s",
    "gui.guhs.piep.menu.zwemmen.tooltip": "Aan: zwemt lekker rondjes als er water is. Uit: blijft liever op het droge.",
    "gui.guhs.piep.menu.zwemmen.aan": "%s gaat weer zwemmen. Blub blub!",
    "gui.guhs.piep.menu.zwemmen.uit": "%s blijft lekker op het droge.",
    "gui.guhs.piep.menu.besties": "Besties zoeken: %s",
    "gui.guhs.piep.menu.besties.tooltip": "Aan: Schilly waggelt zelf naar guhs voor een bestie-moment (of een beetje beef...). Uit: Schilly houdt het rustig.",
    "gui.guhs.piep.menu.besties.aan": "%s zoekt weer besties (en af en toe beef).",
    "gui.guhs.piep.menu.besties.uit": "%s houdt het even rustig. Geen beef vandaag.",
    "gui.guhs.piep.menu.speciaal.pieppiepmuisje": "Op mijn schouder!",
    "gui.guhs.piep.menu.speciaal.pieppiepmuisje.tooltip": "Het muisje klimt op je schouder. Sluip + rechtsklik op een blok (lege hand): eraf.",
    "gui.guhs.piep.menu.speciaal.poepschilly": "Kontje poetsen!",
    "gui.guhs.piep.menu.speciaal.poepschilly.tooltip": "Rechtsklik daarna (lege hand) op een van je tamme guhs: Poepschilly kruipt in het kontje en poetst het blinkend schoon!",
    "gui.guhs.piep.menu.speciaal.schilly": "Bestie-moment!",
    "gui.guhs.piep.menu.speciaal.schilly.tooltip": "Rechtsklik daarna (lege hand) op een van je guhs: echte besties!",
    "gui.guhs.piep.menu.rust": "%s (nog %s:%s)",
    "gui.guhs.piep.menu.oppakken": "Oppakken",
    "gui.guhs.piep.menu.oppakken.tooltip": "Mee in je zak, met naam en al. Rechtsklik op een blok om het weer neer te zetten. (Sluip + rechtsklik kan ook.)",
    "gui.guhs.piep.menu.doei": "Doei!",
    "gui.guhs.piep.lief_kijken": "Je kijkt ineens heel lief... de guhs smelten! (Lief kijken)",
    "gui.guhs.piep.zieli": "zieli...",
    "gui.guhs.piep.gewoon_zieli": "Hij was gewoon zieli...",
    "gui.guhs.piep.nest_start": "De boze kaasknabbels worden wakker! Njeg njeg njeg!",
    "gui.guhs.piep.nest_golf": "Golf %s van %s: daar komen ze uit de gaten!",
    "gui.guhs.piep.nest_golf_klaar": "Die golf is verslagen (ze waren een beetje zieli). Er komt nog meer...",
    "gui.guhs.piep.nest_baas": "De Boze Oppernabbel komt eraan! Hij is heel boos. Of... gewoon zieli?",
    "gui.guhs.piep.nest_beschermd": "Het kaasknabbel-nest is van de knabbels: niet slopen, njeg!",
    "gui.guhs.piep.recept_onbekend": "Dat recept ken je nog niet... (het ligt ergens in een kaasknabbel-nest)",
    "gui.guhs.piep.recept_al": "Dit recept ken je al. Vahoeg!",
    "gui.guhs.piep.recept_geleerd": "Je kent nu het recept van de Roze Guh Koek! Bak hem in de Knabbeloven: zoetdeeg + plaatje + glazuur.",
    # sounds
    "subtitles.guhs.entity.pieppiepmuisje.ambient": "Muisje piept",
    "subtitles.guhs.entity.pieppiepmuisje.hurt": "Muisje piept au",
    "subtitles.guhs.entity.poepschilly.poets": "Poepschilly poetst van binnen",
    "subtitles.guhs.entity.poepschilly.plop": "Poepschilly: plop!",
    "subtitles.guhs.entity.boze_kaasknabbel.ambient": "Kaasknabbel moppert",
    "subtitles.guhs.entity.boze_kaasknabbel.zieli": "Kaasknabbel: zieli...",
    # the superkompas
    f"structure.guhs.{NAME}": "Kaasknabbel-nest",
    f"structure.guhs.{NAME}.tooltip": "Een kaaskorstheuvel vol gaten... en boze kaasknabbels. Stap op de gouden kern als je durft!",
    # the Guhdex (Knus tab, section piep)
    "gui.guhs.knus.onderdeel.piep": "Piep!",
    "gui.guhs.knus.mijlpaal.piep_aaien": "Muisjes geaaid",
    "gui.guhs.knus.mijlpaal.piep_temmen": "Een eigen muisje",
    "gui.guhs.knus.mijlpaal.piep_muizenfamilie": "Een hele muizenfamilie",
    "gui.guhs.knus.mijlpaal.piep_verstoppertje": "Verstoppertje: 3 keer gevonden",
    "gui.guhs.knus.mijlpaal.piep_verstopkampioen": "Verstopkampioen",
    "gui.guhs.knus.mijlpaal.piep_poetsen": "Eerste poetsbeurt",
    "gui.guhs.knus.mijlpaal.piep_poetsmeester": "Poetsmeester (poetsbeurten)",
    "gui.guhs.knus.mijlpaal.piep_besties": "Bestie-momenten met Schilly",
    "gui.guhs.knus.mijlpaal.piep_beef": "Beef bijgelegd",
    "gui.guhs.knus.mijlpaal.piep_knabbels": "Zielige knabbels verslagen",
    "gui.guhs.knus.mijlpaal.piep_nest": "Kaasknabbel-nest gewonnen",
    "gui.guhs.knus.mijlpaal.piep_koekjes": "Roze guh koeken gesmikkeld",
    "gui.guhs.knus.verzameling.piepboek": "Piepboek",
    "gui.guhs.knus.piepboek.pieppiepmuisje": "Pieppiepmuisje",
    "gui.guhs.knus.piepboek.pieppiepmuisje.info":
        "Een piepklein pluchen muisje: donkerpaars, met een pluizig crème bandje om zijn buik en een glimmend roze neusje. "
        "Het woont in de lieve guhhuizen van de Guhmensie (ongeveer een per tien guhs) en is dol op kaasknabbels. "
        "Tem het met een knabbel, draag het op je schouder en speel verstoppertje: het piept tot je het vindt!",
    "gui.guhs.knus.piepboek.poepschilly": "Poepschilly",
    "gui.guhs.knus.piepboek.poepschilly.info":
        "Een bruine zeeschildpad van pluche (met een crème buikje), van de kust van de guhzee. Poepschilly heeft een heel bijzonder beroep: "
        "hij waggelt naar het kontje van een guh, kruipt erin en poetst alles van binnenuit blinkend schoon. Het kriebelt "
        "verschrikkelijk (de guh wiebelt, giechelt en blaast zeepbelletjes), en dan: plop, daar is hij weer! Daarna is de guh "
        "fris van binnen: sneller en sprankelend. Keurig, hoor! (Rechtsklik je Poepschilly voor zijn menuutje: Kontje poetsen!) "
        "(Let op: Poepschilly is de kontpoetser. Zijn lookalike Schilly lijkt er veel op, maar poetst niks.)",
    "gui.guhs.knus.piepboek.schilly": "Schilly",
    "gui.guhs.knus.piepboek.schilly.info":
        "Schilly lijkt heel erg op Poepschilly, maar het is echt een andere schildpad (kijk maar goed!). Schilly poetst "
        "geen guhs: Schilly is een minihoofdje. Daarom hebben guh en Schilly soms beef en draaien ze elkaar boos de rug toe... "
        "maar ze zijn ook echte besties, dus het wordt altijd weer goedgemaakt. Rechtsklik je tamme Schilly (menuutje: "
        "Bestie-moment!) en dan een guh.",
    "gui.guhs.knus.piepboek.roze_guh_koek": "Roze Guh Koek",
    "gui.guhs.knus.piepboek.roze_guh_koek.info":
        "Een roze geglazuurd koekje met grote glimmende blauwe ogen en een klein snoetje. Ze komen met zes in een bakje. "
        "Wie er een opeet, kijkt zo lief dat de guhs er hartjes van krijgen (en makkelijker tam worden). Het recept ligt in "
        "de schat van het kaasknabbel-nest.",
    "gui.guhs.knus.piepboek.boze_kaasknabbel": "Boze Kaasknabbel",
    "gui.guhs.knus.piepboek.boze_kaasknabbel.info":
        "Een kaasknabbel met boze wenkbrauwen. Geen guh (een snack!), dus hij mag best boos zijn. Hij hupt op je af en duwt "
        "een beetje. Verslagen blijkt hij vooral... zieli. Hij laat gewone kaasknabbels achter.",
    "gui.guhs.knus.piepboek.boze_oppernabbel": "Boze Oppernabbel",
    "gui.guhs.knus.piepboek.boze_oppernabbel.info":
        "De grootste, boosste kaasknabbel van het nest, met een kroontje. Hij stampt zo hard dat je wegvliegt (spring op tijd!) "
        "en roept hulp als het tegenzit. Maar eigenlijk... was hij gewoon zieli.",
}


# =====================================================================================================================
# textures
# =====================================================================================================================
def icon_koek():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 4, 14, 14], fill=(206, 150, 92, 255))                 # the cake edge
    d.ellipse([1, 2, 14, 12], fill=(226, 84, 170, 255))                 # the glaze
    d.ellipse([3, 3, 7, 5], fill=(242, 128, 196, 255))                  # a shine
    for x in (4, 9):                                                    # the eyes
        d.rectangle([x, 5, x + 2, 8], fill=(40, 20, 44, 255))
        d.point((x + 1, 8), fill=(40, 160, 210, 255))
        d.point((x, 5), fill=(255, 255, 255, 255))
    d.point((7, 9), fill=(120, 40, 56, 255))
    d.line([6, 10, 8, 10], fill=(130, 44, 66, 255))
    return img


def icon_recept():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 1, 13, 14], fill=(250, 240, 222, 255), outline=(190, 160, 120, 255))
    d.rectangle([2, 1, 13, 2], fill=(236, 120, 180, 255))
    koek = icon_koek().resize((8, 8), Image.NEAREST)
    img.alpha_composite(koek, (4, 3))
    for y in (11, 13):
        d.line([4, y, 11, y], fill=(150, 120, 100, 255))
    return img


def icon_muisje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 4, 14, 13], fill=(42, 34, 51, 255))                   # the body (seen from the side, facing left)
    d.rectangle([8, 4, 10, 13], fill=(236, 228, 208, 255))              # the cream band
    d.ellipse([7, 4, 11, 5], fill=(236, 228, 208, 255))
    d.ellipse([1, 9, 5, 13], fill=(236, 228, 208, 255))                 # the chin
    d.ellipse([3, 3, 6, 6], fill=(92, 90, 98, 255))                     # an ear
    d.point((4, 8), fill=(250, 250, 255, 255))                          # the bead eye
    d.point((3, 8), fill=(10, 10, 14, 255))
    d.rectangle([1, 9, 2, 10], fill=(240, 142, 162, 255))               # the pink nose
    d.rectangle([11, 13, 12, 13], fill=(26, 22, 30, 255))
    return img


def icon_schildpad(schild, patroon, buik, huid):
    """A picked-up turtle plush (Poepschilly / Schilly), seen from the side facing left: dome shell, cream belly, head, flippers."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rand = tuple(max(0, c - 40) for c in schild) + (255,)
    d.ellipse([9, 10, 13, 14], fill=huid + (255,))                   # back flipper
    d.ellipse([4, 10, 8, 14], fill=huid + (255,))                    # front flipper
    d.ellipse([0, 6, 6, 11], fill=huid + (255,))                     # the head
    d.chord([3, 3, 15, 15], 180, 360, fill=schild + (255,), outline=rand)   # the dome shell
    d.rectangle([4, 9, 14, 11], fill=buik + (255,))                  # the cream belly rim
    for (x, y) in ((6, 6), (9, 5), (12, 7), (8, 8), (11, 8)):        # the shell pattern
        d.point((x, y), fill=patroon + (255,))
    d.point((1, 8), fill=(20, 20, 24, 255))                          # the bead eye
    d.point((2, 7), fill=(255, 255, 255, 255))
    d.point((0, 9), fill=(240, 142, 162, 255))                       # a tiny blush
    return img


def particle_piepje():
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 4, 4, 7], fill=(255, 190, 214, 255))
    d.line([4, 5, 4, 0], fill=(255, 190, 214, 255))
    d.line([4, 0, 6, 1], fill=(255, 190, 214, 255))
    d.point((2, 5), fill=(255, 255, 255, 255))
    return img


def particle_sparkel(frame):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = [3, 2, 1, 1][frame]
    c = [(210, 255, 250, 255), (160, 240, 230, 255), (130, 220, 220, 220), (120, 200, 210, 160)][frame]
    d.line([3.5 - r, 3.5, 3.5 + r, 3.5], fill=c)
    d.line([3.5, 3.5 - r, 3.5, 3.5 + r], fill=c)
    d.point((3, 3), fill=(255, 255, 255, 255))
    d.point((4, 4), fill=(255, 255, 255, 255))
    return img


def effect_fris():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for (x, y, r) in ((5, 11, 4), (12, 6, 3), (12, 13, 2)):
        d.ellipse([x - r, y - r, x + r, y + r], outline=(120, 220, 230, 255), fill=(200, 250, 250, 120))
        d.point((x - r // 2, y - r // 2), fill=(255, 255, 255, 255))
    d.line([3, 3, 3, 7], fill=(255, 255, 255, 255))
    d.line([1, 5, 5, 5], fill=(255, 255, 255, 255))
    return img


def effect_lief():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 17, 17], fill=(242, 128, 196, 255))
    for x in (4, 10):
        d.polygon([(x + 2, 11), (x, 8), (x + 1, 6), (x + 2, 7), (x + 3, 6), (x + 4, 8)], fill=(214, 40, 110, 255))
    d.arc([6, 10, 12, 14], 20, 160, fill=(120, 30, 70, 255))
    return img


def textures(h):
    h.save(icon_koek(), "item", "roze_guh_koek.png")
    h.save(icon_recept(), "item", "roze_guh_koek_recept.png")
    h.save(icon_muisje(), "item", "pieppiepmuisje_item.png")
    h.save(icon_schildpad((112, 128, 62), (74, 88, 40), (238, 228, 208), (150, 176, 96)), "item", "poepschilly_item.png")
    h.save(icon_schildpad((134, 160, 74), (106, 94, 80), (226, 214, 190), (170, 196, 110)), "item", "schilly_item.png")
    h.save(particle_piepje(), "particle", "piepje.png")
    for f in range(4):
        h.save(particle_sparkel(f), "particle", f"fris_sparkel_{f}.png")
    h.save(effect_fris(), "mob_effect", "fris_van_binnen.png")
    h.save(effect_lief(), "mob_effect", "lief_kijken.png")


# =====================================================================================================================
# blocks, items, loot, tags
# =====================================================================================================================
def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # the koek tray: its models (1-6 koeken) and textures are the user's (blockbench/roze_guh_koek.bbmodel); write them only
    # if they are missing (a fresh checkout without them)
    if not os.path.exists(f"{A}/models/block/roze_guh_koek_6.json"):
        modellen.koek_block(h)
    w(f"{A}/blockstates/roze_guh_koek.json", {"variants": {
        f"facing={f},koeken={n}": {"model": f"guhs:block/roze_guh_koek_{n}", **({"y": r} if r else {})}
        for f, r in ROT.items() for n in range(1, 7)}})
    h.item_model("roze_guh_koek")
    h.item_model("roze_guh_koek_recept")
    h.item_model("pieppiepmuisje_item")
    h.item_model("poepschilly_item")
    h.item_model("schilly_item")
    for e in ("pieppiepmuisje", "poepschilly", "schilly", "boze_kaasknabbel", "boze_oppernabbel"):
        w(f"{A}/models/item/{e}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    # particles
    w(f"{A}/particles/piepje.json", {"textures": ["guhs:piepje"]})
    w(f"{A}/particles/fris_sparkel.json", {"textures": [f"guhs:fris_sparkel_{f}" for f in range(4)]})
    # loot: the koek tray drops every koek on it
    w(f"{D}/loot_table/blocks/roze_guh_koek.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "guhs:roze_guh_koek", "functions": [
            {"function": "minecraft:set_count", "count": n, "add": False, "conditions": [{
                "condition": "minecraft:block_state_property", "block": "guhs:roze_guh_koek", "properties": {"koeken": str(n)}}]}
            for n in range(2, 7)]}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    w(f"{D}/loot_table/entities/boze_kaasknabbel.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": h.count_fn(1, 3)}]}]})
    w(f"{D}/loot_table/entities/boze_oppernabbel.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": h.count_fn(16, 24)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "functions": h.count_fn(4, 8)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:gouden_kaasknabbel"}]}]})
    knabbels = [{"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 6, "functions": h.count_fn(8, 16)},
                {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 3, "functions": h.count_fn(2, 6)},
                {"type": "minecraft:item", "name": "guhs:roze_guh_koek", "weight": 2, "functions": h.count_fn(1, 3)},
                {"type": "minecraft:item", "name": "guhs:block_of_kaasknabbels", "weight": 2, "functions": h.count_fn(1, 3)}]
    w(f"{D}/loot_table/chests/{NAME}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": knabbels}]})
    w(f"{D}/loot_table/chests/{NAME}_schat.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:roze_guh_koek_recept"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:roze_guh_koek", "functions": h.count_fn(6, 6)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:gouden_kaasknabbel"}]},
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 6}, "entries": knabbels}]})
    # a roze guh koek now and then in the chests of the lieve buildings (no Mika places, no Barbecuether)
    for table in ("guh_village", "hamster_house", "guh_picnic", "guh_kasteel", "boomhutdorp", "kampeerplekje", "ballonfestival",
                  "guhboerderij", "guh_sterrenwacht", "zwevende_eilanden"):
        if os.path.exists(f"{D}/loot_table/chests/{table}.json"):
            h.add_loot(table, [{"type": "minecraft:item", "name": "guhs:roze_guh_koek", "functions": h.count_fn(1, 3)}], chance=0.3)
    # tags
    h.add_tag("guhs/tags/item/knus/gebak", ["guhs:roze_guh_koek"])
    h.add_tag("guhs/tags/block/piep/verstopplekken", ["#minecraft:flower_pots", "minecraft:decorated_pot", "minecraft:barrel",
                                                      "minecraft:composter", "minecraft:cauldron", "minecraft:hay_block",
                                                      "guhs:guh_bloempot", "guhs:potted_roze_guhbloem", "guhs:potted_guhoortjes",
                                                      "guhs:potted_kaasbloem", "guhs:guh_wastobbe", "guhs:guhnestje", "guhs:kippennestje",
                                                      "guhs:bladerhoopje", "guhs:guh_voerbak", "guhs:seizoensbloembak"])
    h.add_tag("minecraft/tags/entity_type/can_breathe_under_water", ["guhs:poepschilly", "guhs:schilly"])
    # Poepschilly on the guhzee coasts
    w(f"{D}/neoforge/biome_modifier/piep_poepschilly.json", {
        "type": "neoforge:add_spawns", "biomes": "#guhs:guhzeeen",
        "spawners": [{"type": "guhs:poepschilly", "weight": 6, "minCount": 1, "maxCount": 2},
                     {"type": "guhs:schilly", "weight": 3, "minCount": 1, "maxCount": 1}]})


def check_tags(h):
    """Every block in the verstop tag exists (as a blockstate of ours, or a vanilla one)."""
    problems = []
    import json
    tag = json.load(open(f"{h.R}/data/guhs/tags/block/piep/verstopplekken.json", encoding="utf-8"))
    for v in tag["values"]:
        if v.startswith("guhs:") and not os.path.exists(f"{h.A}/blockstates/{v[5:]}.json"):
            problems.append(f"piep: verstop tag names a missing block {v}")
    return problems


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    for name in QUEST:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)


# =====================================================================================================================
# the nest and the test rooms
# =====================================================================================================================
def structure(h):
    h.TEMPLATE_SIZES[NAME] = 18
    h.FLATNESS[NAME] = 9
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, h.GUHMENSION_LAND, spacing=44, separation=16, salt=SALT, start_y=-nest.G, reach=40, centre=nest.ANCHOR,
                spawn_overrides={"monster": none, "creature": none, "ambient": none})
    s, info = nest.build(h)
    problems = nest.check(s, info)
    if problems:
        raise SystemExit("kaasknabbel_nest geometry check failed:\n  " + "\n  ".join(problems[:40]))
    s.save(NAME)
    # the gametest rooms: a little meadow, and an arena for the nest fight (floor at y 0, the kern in the middle)
    t = h.Structure((9, 5, 9))
    for x in range(9):
        for z in range(9):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("piep_test_wei")
    a = h.Structure((23, 6, 23))
    for x in range(23):
        for z in range(23):
            a.set(x, 0, z, "guhs:kaaskorst_stenen")
            if x in (0, 22) or z in (0, 22):
                for y in range(1, 5):
                    a.set(x, y, z, "minecraft:glass")
    a.save("piep_test_arena")


# =====================================================================================================================
# roze guh koek trays on a few tables of lieve buildings (the templates are made earlier in the same run)
# =====================================================================================================================
# Each: template, max number of trays. Only on a guhs:guh_tafel with nothing above it.
TAFELS = [("ballonfestival", 2), ("boomhutdorp", 1), ("kampeerplekje", 1), ("knuffeldal_stadje/hoek_noordoost", 1),
          ("knuffeldal_stadje/hoek_noordwest", 1), ("knuffeldal_stadje/hoek_zuidoost", 1), ("knuffeldal_stadje/hoek_zuidwest", 1)]


def _nbt_read(data):
    """A typed NBT reader: every value is (type id, value), so the file can be written back exactly."""
    pos = 0

    def take(n):
        nonlocal pos
        pos += n
        return data[pos - n:pos]

    def string():
        (n,) = struct.unpack(">H", take(2))
        return take(n).decode("utf-8")

    def payload(t):
        if t == 1:
            return struct.unpack(">b", take(1))[0]
        if t == 2:
            return struct.unpack(">h", take(2))[0]
        if t == 3:
            return struct.unpack(">i", take(4))[0]
        if t == 4:
            return struct.unpack(">q", take(8))[0]
        if t == 5:
            return struct.unpack(">f", take(4))[0]
        if t == 6:
            return struct.unpack(">d", take(8))[0]
        if t == 7:
            (n,) = struct.unpack(">i", take(4))
            return take(n)
        if t == 8:
            return string()
        if t == 9:
            et = take(1)[0]
            (n,) = struct.unpack(">i", take(4))
            return (et, [payload(et) for _ in range(n)])
        if t == 10:
            out = {}
            while True:
                tt = take(1)[0]
                if tt == 0:
                    return out
                k = string()
                out[k] = (tt, payload(tt))
        if t == 11:
            (n,) = struct.unpack(">i", take(4))
            return list(struct.unpack(f">{n}i", take(4 * n)))
        if t == 12:
            (n,) = struct.unpack(">i", take(4))
            return list(struct.unpack(f">{n}q", take(8 * n)))
        raise ValueError(f"nbt type {t}")

    t = take(1)[0]
    string()
    return payload(t)


def _nbt_write(root):
    def payload(t, v):
        if t == 1:
            return struct.pack(">b", v)
        if t == 2:
            return struct.pack(">h", v)
        if t == 3:
            return struct.pack(">i", v)
        if t == 4:
            return struct.pack(">q", v)
        if t == 5:
            return struct.pack(">f", v)
        if t == 6:
            return struct.pack(">d", v)
        if t == 7:
            return struct.pack(">i", len(v)) + v
        if t == 8:
            b = v.encode("utf-8")
            return struct.pack(">H", len(b)) + b
        if t == 9:
            et, items = v
            return bytes([et if items else 0]) + struct.pack(">i", len(items)) + b"".join(payload(et, i) for i in items)
        if t == 10:
            out = b""
            for k, (tt, vv) in v.items():
                kb = k.encode("utf-8")
                out += bytes([tt]) + struct.pack(">H", len(kb)) + kb + payload(tt, vv)
            return out + b"\x00"
        if t == 11:
            return struct.pack(">i", len(v)) + struct.pack(f">{len(v)}i", *v)
        if t == 12:
            return struct.pack(">i", len(v)) + struct.pack(f">{len(v)}q", *v)
        raise ValueError(t)
    return b"\x0a\x00\x00" + payload(10, root)


def koek_op_tafels(h):
    base = os.path.join(h.R, "data", "guhs", "structure")
    placed = {}
    rng = random.Random(28101)
    for name, max_n in TAFELS:
        path = os.path.join(base, name + ".nbt")
        if not os.path.exists(path):
            continue
        root = _nbt_read(gzip.open(path).read())
        palette = root["palette"][1][1]
        blocks = root["blocks"][1][1]
        names = [p["Name"][1] for p in palette]
        if "guhs:roze_guh_koek" in names:
            continue                                            # (done already)
        at = {tuple(b["pos"][1][1]): names[b["state"][1]] for b in blocks}
        ent_spots = {tuple(int(math.floor(c)) for c in e["pos"][1][1]) for e in root.get("entities", (9, (10, [])))[1][1]}
        tafels = [p for p, n in at.items() if n == "guhs:guh_tafel"
                  and at.get((p[0], p[1] + 1, p[2]), "minecraft:air") == "minecraft:air" and (p[0], p[1] + 1, p[2]) not in ent_spots]
        rng.shuffle(tafels)
        tafels.sort(key=lambda p: (p[1], p[0], p[2]))            # (stable, then pick the first few spread out)
        chosen = []
        for p in tafels:
            if len(chosen) >= max_n:
                break
            if all(abs(p[0] - q[0]) + abs(p[2] - q[2]) > 4 for q in chosen):
                chosen.append(p)
        if not chosen:
            continue
        for i, p in enumerate(chosen):
            koeken = 6 if i % 2 == 0 else 4
            state = {"Name": (8, "guhs:roze_guh_koek"), "Properties": (10, {"facing": (8, rng.choice(list(ROT))), "koeken": (8, str(koeken))})}
            palette.append(state)
            idx = len(palette) - 1
            top = (p[0], p[1] + 1, p[2])
            blocks[:] = [b for b in blocks if tuple(b["pos"][1][1]) != top]
            blocks.append({"pos": (9, (3, list(top))), "state": (3, idx)})
        with open(path, "wb") as f:
            f.write(gzip.compress(_nbt_write(root)))
        placed[name] = len(chosen)
    print(f"piep: roze guh koek trays on tables: {placed}")
    return placed


# =====================================================================================================================
def build(h):
    modellen.build(h)                   # (writes nothing unless PIEP_MODELLEN=1: the models are the user's)
    from features import piep_bbexport
    piep_bbexport.main(piep_bbexport.newer())   # the user's Blockbench edits (newer than their exports) into the game
    textures(h)
    blocks_and_items(h)
    sounds(h)
    advancements(h)
    texts(h)
    structure(h)
    koek_op_tafels(h)
    problems = check_tags(h) + modellen.check(h)
    if problems:
        raise SystemExit("piep self-check failed:\n  " + "\n  ".join(problems))


# =====================================================================================================================
# FTB Quests: the chapter guhs_piep (no locking)
# =====================================================================================================================
FTB_CHAPTER = "guhs_piep"   # (2.8 chapter group: make_ftbquests puts these quests in their own chapter "Piep!")
FTB_SECTION = "Piep!"


def ftb(fq):
    for key, title, desc, icon, tasks, rewards, x, y, shape, xp in _quests(fq):
        fq.q(key, title, desc, icon, tasks, rewards=rewards, shape=shape, xp=xp)


def _quests(fq):
    quests = []

    def q(key, title, desc, icon, tasks, rewards=(("guhs:kaas_knabbels", 8),), x=0.0, y=0.0, shape=None, xp=0):
        quests.append((key, title, desc, icon, tasks, rewards, x, y, shape, xp))

    adv, item = fq.adv, fq.item
    q("piep_start", "Piep piep!", "Er wonen piepkleine &5pieppiepmuisjes&r in de lieve guhhuizen van de Guhmensie. Ga naar een guhhuis, "
      "zoek een muisje (luister naar het gepiep!) en aai het met een lege hand.", "guhs:pieppiepmuisje_spawn_egg",
      [adv("piep_muisje_geaaid")], x=0, y=0, shape="gear", xp=100)
    q("piep_temmen", "Een eigen muisje", "Geef een muisje een &6kaasknabbel&r. Soms eet het meteen uit je hand en is het van jou. "
      "Een muisje rent trouwens ook naar knabbels die op de grond liggen!", "guhs:kaas_knabbels", [adv("piep_muisje_getamed")],
      x=2, y=0, xp=100)
    q("piep_schouder", "Muisje mee", "Sluip en rechtsklik je eigen muisje (of kies &dOppakken&r in zijn menuutje): je pakt het op, met naam "
      "en al. Rechtsklik ermee in de lucht en het zit op je schouder! Sluip + rechtsklik op een blok (lege hand) en het hopt eraf.",
      "guhs:pieppiepmuisje_item",
      [item("guhs:pieppiepmuisje_item")], x=4, y=0)
    q("piep_verstoppertje", "Verstoppertje", "Je muisje verstopt zich soms in een bloempot, een mandje of een ton in de buurt, en piept tot je "
      "het vindt. Rechtsklik op de plek waar het piept! Vind het &d3 keer&r.", "minecraft:flower_pot", [adv("piep_verstoppertje_3")],
      rewards=(("guhs:roze_guh_koek", 1),), x=6, y=0, shape="rsquare", xp=150)
    q("piep_schilly", "Poepschilly", "Aan de kust van de guhzee zwemt &2Poepschilly&r, een pluchen zeeschildpad. Zoek hem en tem hem met "
      "zeewier (of een kaasknabbel).", "guhs:poepschilly_spawn_egg", [adv("piep_schilly_gevonden"), adv("piep_schilly_getamed")],
      rewards=(("minecraft:kelp", 16),), x=0, y=2, xp=100)
    q("piep_poetsbeurt", "Fris van binnen", "Rechtsklik je Poepschilly (lege hand) en kies &dKontje poetsen!&r. Rechtsklik dan een van je tamme "
      "guhs: Poepschilly waggelt erheen, kruipt in het kontje en poetst alles van binnenuit! Het kriebelt... plop! Daarna is je guh "
      "&bfris van binnen&r.", "minecraft:bubble_coral",
      [adv("piep_poetsbeurt")], rewards=(("guhs:kaas_knabbels", 12),), x=2, y=2, shape="rsquare", xp=150)
    q("piep_schilly2", "Schilly (niet Poepschilly!)", "Aan dezelfde kust woont ook &2Schilly&r: bijna hetzelfde, maar het is een "
      "minihoofdje dat nooit poetst. Zoek en tem Schilly.", "guhs:schilly_spawn_egg",
      [adv("piep_schilly2_gevonden"), adv("piep_schilly2_getamed")], rewards=(("minecraft:kelp", 16),), x=4, y=2, xp=100)
    q("piep_bestie", "Besties", "Rechtsklik je tamme Schilly, kies &dBestie-moment!&r en rechtsklik dan een van je guhs: echte besties!",
      "minecraft:sea_pickle",
      [adv("piep_schilly_bestie")], rewards=(("guhs:kaas_knabbels", 12),), x=6, y=2, shape="rsquare", xp=100)
    q("piep_beef", "Beef... toch besties", "Soms hebben Schilly en een guh beef (minihoofdje!). Wacht even: ze maken het altijd weer goed. "
      "Zie een beef bijleggen.", "minecraft:heart_of_the_sea", [adv("piep_schilly_beef")], rewards=(("guhs:roze_guh_koek", 1),),
      x=8, y=2, xp=100)
    q("piep_nest", "Het kaasknabbel-nest", "Ergens in de Guhmensie staat een kaaskorstheuvel vol gaten: het &6kaasknabbel-nest&r. "
      "Het superkompas (categorie Avontuur) wijst de weg.", "guhs:block_of_kaasknabbels", [fq.structure(NAME)],
      x=0, y=4, shape="circle", xp=100)
    q("piep_knabbels", "Boze knabbels", "Versla 10 boze kaasknabbels. Ze zijn eigenlijk... zieli.", "guhs:boze_kaasknabbel_spawn_egg",
      [fq.kill("guhs:boze_kaasknabbel", 10)], x=2, y=4, xp=100)
    q("piep_oppernabbel", "Hij was gewoon zieli", "Stap op de gouden kern in het nest, versla drie golven boze kaasknabbels en daarna de "
      "&cBoze Oppernabbel&r. Pas op voor zijn stamp: spring!", "guhs:boze_oppernabbel_spawn_egg", [adv("piep_nest_gewonnen")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=4, y=4, shape="gear", xp=300)
    q("piep_recept", "Het geheime recept", "In de schatkist van het nest ligt het recept van de Roze Guh Koek. Rechtsklik het om het te "
      "leren; daarna bak je hem in de Knabbeloven (zoetdeeg, plaatje, glazuur).", "guhs:roze_guh_koek_recept", [adv("piep_recept")],
      x=6, y=4, xp=150)
    q("piep_koek", "Lief kijken", "Eet een &dRoze Guh Koek&r (of zet een bakje neer: er passen er zes in!). Wie er een opeet, kijkt zo lief "
      "dat de guhs er hartjes van krijgen.", "guhs:roze_guh_koek", [adv("piep_koek")], rewards=(("guhs:roze_guh_koek", 2),),
      x=8, y=4, shape="rsquare", xp=100)

    return quests
