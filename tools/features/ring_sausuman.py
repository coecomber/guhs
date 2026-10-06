"""
bbq2 (ring-sausuman): the extra stop of the Knabbelring: de Toren van Sausuman. Java: feature/ringsausuman.

  - the tower (structure guhs:sausuman_toren: ring_sausuman_bouw.py, a "grot" of wereld.bbq_structuur, exactly one per world
    200-400 blocks from the tree city of chapter 4, behind Guhdalfs sluier until chapter 4 is done)
  - Sausuman van de Vele Sauzen (NPC kind sausuman: ring_sausuman_modellen.py) and his questline "ring_sausuman" (4 steps, per
    player): he wants a bite of the ring, doesn't get it, and bakes his own in the Ringenbakker. It becomes an onion ring.
  - the blocks: ringsausuman_ringenbakker (the machine's face and lever), ringsausuman_voorraad (the three stations),
    ringsausuman_mikarad (a real source of 10 vadskracht), ringsausuman_sputterpijp (the puffing exhaust), ringsausuman_pannantir
    (the seeing pan); the items: the three ingredients and the uienring
  - the baking scene's texts, sounds, advancements, FTB quests (chapter guhs_knabbelring, the last section)

Hooks: build(h), ftb(fq), FTB_LINEAIR, FTB_SECTIES. The wiki entries are in ring_sausuman_wiki.py.
"""
import json
import os
import re

from PIL import Image, ImageDraw

from . import bbq2
from . import ring_sausuman_bouw as bouw
from . import ring_sausuman_modellen as modellen
from . import vadskracht
from . import verhaal_motor
from . import wereld

NAME = "ring_sausuman"
SALT = 21302201                                  # slice 22 (CONTRACT_130 3); the guaranteed set is this + 7
STRUCTUUR = bouw.STRUCTUUR
BOOMSTAD = "guhladriel_boomstad"                 # chapter 4's structure: the tower stands 200-400 blocks from it
VOORRANG = 250                                   # (CONTRACT_130 3: the ring structures 300 .. 260, the tower 250)
# free blocks the cave must have above the yard: the whole shaft and the feet of the horns stand in the open; only where the
# ceiling comes lower the template opens it itself (ring_sausuman_bouw.Bouw.grond)
HOOGTE = 32
BEELDEN = 8                                      # the visions of the Pannantir (PannantirBlock.BEELDEN)
VERMOGEN = 10                                    # what the Mika-rad gives (MikaradBlock.VERMOGEN)

FTB_LINEAIR = True
FTB_SECTIES = [("ring_sausuman", "De Toren van Sausuman", "npc:sausuman", None)]

# =====================================================================================================================
# sounds
# =====================================================================================================================
SOUNDS = {
    "ringsausuman.sputter": [{"name": "minecraft:block.fire.extinguish", "type": "event", "pitch": 0.6, "volume": 0.5},
                             {"name": "minecraft:block.lava.pop", "type": "event", "pitch": 0.7, "volume": 0.9},
                             {"name": "minecraft:block.candle.extinguish", "type": "event", "pitch": 0.5, "volume": 1.0}],
    "ringsausuman.ronk": [{"name": "minecraft:block.beacon.activate", "type": "event", "pitch": 0.55, "volume": 1.0}],
    "ringsausuman.pling": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.6, "volume": 1.0}],
    "ringsausuman.mok": [{"name": "minecraft:entity.villager.no", "type": "event", "pitch": 0.55, "volume": 0.9}],
}
SUBTITLES = {"ringsausuman.sputter": "Sputterpijp sputtert", "ringsausuman.ronk": "Ringenbakker ronkt", "ringsausuman.pling": "Pling!",
             "ringsausuman.mok": "Sausuman mokt"}

# =====================================================================================================================
# Dutch texts
# =====================================================================================================================
S = "quest.guhs.ringsausuman.sausuman."
HINT = "quest.guhs.ringsausuman.hint."
GUI = "gui.guhs.ringsausuman."
LANG = {
    "entity.guhs.guh_npc.sausuman": "Sausuman",
    "block.guhs.ringsausuman_ringenbakker": "De Ringenbakker",
    "block.guhs.ringsausuman_voorraad": "Voorraad van Sausuman",
    "block.guhs.ringsausuman_mikarad": "Mika-rad",
    "block.guhs.ringsausuman_sputterpijp": "Sputterpijp",
    "block.guhs.ringsausuman_pannantir": "Pannantír",
    "item.guhs.ringsausuman_ringdeeg": "Ringdeeg",
    "item.guhs.ringsausuman_ringdeeg.lore": "Een slappe ring van deeg. Nog niet lekker. Voor de Ringenbakker",
    "item.guhs.ringsausuman_frituursaus": "Kannetje hete frituursaus",
    "item.guhs.ringsausuman_frituursaus.lore": "Gloeiend heet. Niet likken. Voor de Ringenbakker",
    "item.guhs.ringsausuman_ui": "Ui uit de Kaaskast",
    "item.guhs.ringsausuman_ui.lore": "Het enige wat er nog lag. Rond is rond, njeg. Voor de Ringenbakker",
    "item.guhs.ringsausuman_uienring": "Uienring",
    "item.guhs.ringsausuman_uienring.lore": "Eén ring om ze allemaal op te eten. Geen Knabbelring, wel krokant",
    # the stations
    GUI + "voorraad.eerst_praten": "Dit is van Sausuman. Vraag hem eerst wat hij ermee wil, njeg.",
    GUI + "voorraad.pak.deeg": "De Deegkneder spuugt een slappe ring van deeg uit. Plop. Die is voor de Ringenbakker.",
    GUI + "voorraad.pak.saus": "Je tapt een kannetje gloeiend hete frituursaus. Niet likken. (Wel lekker.)",
    GUI + "voorraad.pak.kaas": "De Kaaskast is leeg. Helemaal leeg. Er ligt alleen nog een ui. Nou ja: rond is rond, njeg.",
    GUI + "voorraad.heb_je.deeg": "Je hebt al ringdeeg. Eén ring tegelijk, zegt de Deegkneder.",
    GUI + "voorraad.heb_je.saus": "Je kannetje zit nog vol.",
    GUI + "voorraad.heb_je.kaas": "Er lag maar één ui. Die heb jij al.",
    GUI + "voorraad.klaar.deeg": "De Deegkneder kneedt lusteloos verder. Jij hebt hier niks meer nodig.",
    GUI + "voorraad.klaar.saus": "Drup. Drup. De Sauskraan lekt nog steeds. Jij hebt hier niks meer nodig.",
    GUI + "voorraad.klaar.kaas": "De Kaaskast is nu echt helemaal leeg. Er hangt een briefje: 'KAAS KOPEN!!'",
    GUI + "voorraad.nog": "Nog %s te gaan voor de Ringenbakker",
    # the machine
    GUI + "bakker.afblijven": "AFBLIJVEN! roept Sausuman. Praat eerst met hem voor je aan zijn Ringenbakker zit.",
    GUI + "bakker.mist": "De Ringenbakker rammelt leeg. Je hebt %s van de %s dingen bij je: deeg, hete saus en kaas haal je boven in de toren.",
    GUI + "bakker.koelt_af": "De Ringenbakker tikt en koelt af. In de hoek zit iemand heel hard te mokken.",
    GUI + "bakker.morgen": "De Ringenbakker moet afkoelen. Morgen bakt hij weer een uienring voor je.",
    GUI + "bakker.dagelijks": "PLING! Eén verse uienring. Sausuman kijkt expres de andere kant op.",
    GUI + "mikarad.por": "De Uruk-Mika kijkt je aan. Hij sjokt geen stap harder.",
    GUI + "mikarad.regel": "Hierin sjokt een Uruk-Mika. Een Mika rent niet, een Mika wordt gebracht.",
    # the Pannantir
    GUI + "pannantir.kijk": "Je kijkt in de Pannantír. %s",
    GUI + "pannantir.0": "Een groot oog. Het knippert. Het heeft trek.",
    GUI + "pannantir.1": "Negen ruiters. Ze zijn de weg kwijt en durven het niet te vragen.",
    GUI + "pannantir.2": "Een guh die keihard rent in een rad. Hij vindt het leuk. Njeg!",
    GUI + "pannantir.3": "Jezelf, van bovenaf. Je hebt een kruimel op je snoet.",
    GUI + "pannantir.4": "Een berg waar frituurlucht uit komt. Je krijgt er honger van.",
    GUI + "pannantir.5": "Guhdalf. Hij zwaait. Hij kan jou dus ook zien.",
    GUI + "pannantir.6": "Alleen saus. Het is ook gewoon een pan.",
    GUI + "pannantir.7": "Sausuman, die stiekem aan een uienring ruikt. Hij ziet je kijken en mokt snel verder.",
    # answers, Guhdex
    GUI + "optie.nee": "Nee, njeg!",
    GUI + "optie.delen": "Hij is om te delen. Later.",
    GUI + "optie.hapje": "Wil je een hapje uienring?",
    GUI + "optie.weg": "Ik laat je even mokken",
    GUI + "optie.machines": "Waarom doen je machines het niet?",
    GUI + "optie.nog_een": "Mag ik nog een uienring?",
    GUI + "nodig.kaas": "Kaas uit de Kaaskast",
    GUI + "beloning.dagelijks": "Elke dag één uienring uit de Ringenbakker",
    # Sausuman
    S + "te_vroeg": "Wie ben jij? Geen ring? Dan heb ik niks aan je. Kom terug als je iets ronds en lekkers bij je hebt. En veeg je poten!",
    S + "hallo": "Zo zo. De ringdrager. Ik ruik hem. Kaas. Krokant. ROND. Ik ben Sausuman van de Vele Sauzen: ik heb overal saus voor, behalve "
                 "iets lekkers om het op te doen. Geef mij een hapje van die Knabbelring. Eén hapje! Ik vraag het netjes. Min of meer.",
    S + "hallo_op": "WAT? Hij is OP? Gefrituurd, gedeeld, en ik heb NIKS gehad? Zelfs dat Oog heeft een stukje gekregen! Ik ben Sausuman van de "
                    "Vele Sauzen, ik zat hier klaar met ACHT soorten saus! Heb je echt geen kruimeltje meer voor me?",
    S + "zelf_nee": "Nee? NEE? Tegen MIJ? Goed. Prima. Dan bak ik er ZELF een.",
    S + "zelf_delen": "Later? LATER? Ik wacht al weken! Weet je wat? Laat maar. Ik bak er ZELF een.",
    S + "opdracht": "Mijn Ringenbakker is af. Bijna. Hij is alleen nog leeg. Er moet deeg in van de Deegkneder, hete saus uit de Sauskraan en "
                    "KAAS uit de Kaaskast. Alles staat boven. Haal jij het even? Ik moet hier blijven om belangrijk te zijn.",
    S + "ingredienten": "Nog %s dingen! Deeg op de eerste verdieping, hete saus op de tweede, kaas op de derde. De trap op, njeg. En niet aan "
                        "mijn Pannantír zitten!",
    S + "hendel": "Alles binnen? Trek dan aan de hendel van de Ringenbakker! Nee wacht, IK wil... nee. Doe jij maar. Als hij ontploft heb "
                  "jij het gedaan.",
    S + "mok": "... Ik zeg niks. Ik mok. Ik wilde een Knabbelring, met KAAS. Dit is een UIENRING. Dat is GROENTE. Ga weg.",
    S + "hapje": "... Geef hier. *knabbel* ... *knabbel knabbel* ... Njeg. Best lekker. MAAR IK MOK NOG STEEDS. Hier, neem die pan mee, ik zie er "
                 "toch alleen maar dat Oog in. En die uienringen ook. Ik hoef ze niet. (Laat er eentje liggen.)",
    S + "opgegeten": "Je hebt hem ZELF opgegeten? Waar ik BIJ zat? Nu mok ik DUBBEL. Hier, neem die pan mee, ik wil niks ronds meer zien. En die "
                     "uienringen ook. Ja, ik had er stiekem nog een paar gebakken. Zeg niks.",
    S + "weg": "Goed. Ga maar. Ik zit hier prima. In mijn hoek. Alleen. Met mijn baard.",
    S + "na": "Wat. Ik mok. Dat zie je toch? ... Is er nog iets?",
    S + "te_zwaar": "Te zwaar, te zwaar... Die Uitvinder-guh van de Oude Guhrad-centrale kwam langs. 'Sausuman,' zei hij, 'vijf machines op één "
                    "rad van %s vadskracht, dat is te zwaar. Dan staat alles stil. En er hoort een GUH in een rad, geen Mika.' Wat weet HIJ "
                    "ervan? ... Hij had gelijk. Zeg dat niet tegen hem.",
    S + "nog_een": "De hendel. Eén per dag. Ik kijk niet. Ik kijk NOOIT.",
    S + "nog_een_morgen": "Je hebt er vandaag al een gehad. De Ringenbakker moet afkoelen. Ik ook.",
    HINT + "ingredienten": "haal deeg, hete saus en kaas op de drie verdiepingen van de toren",
    HINT + "hendel": "trek aan de hendel van de Ringenbakker, beneden in de hal",
    HINT + "mok": "Sausuman mokt. Bied hem een hapje van je uienring aan",
    HINT + "klaar": "de Ringenbakker bakt elke dag één uienring voor je",
    "quest.guhs.ringsausuman.wenk": "Ruik je dat? Iemand bakt iets. Het komt van die zwarte toren, daar woont Sausuman. Het hoeft niet voor de "
                                    "reis, maar je Superkompas weet de weg (tab Barbecue), njeg.",
    "quest.guhs.ringsausuman.wenk_alleen": "Niet ver van de boomstad staat een zwarte toren te roken: de Toren van Sausuman. Het hoeft niet voor de "
                                           "reis, maar je Superkompas weet de weg (tab Barbecue).",
}
SCENE = {
    "start": "Achteruit! Sausuman van de Vele Sauzen gaat een RING bakken!",
    "spreuk": "Eén ring om ze allemaal op te eten! Eén ring om... om... nou ja, ÉÉN RING!",
    "pling": "PLING. Er rolt iets uit. Het is rond. Het is krokant. Het ruikt naar... ui.",
    "ui": "Dat is geen Knabbelring. Dat is een UIENRING. Wie heeft er UI in gedaan?!",
    "sam": "Hij ruikt wel lekker, njeg.",
    "mok": "Ik wilde KAAS. Dit is GROENTE. Ga weg. Ik mok.",
}
STAPPEN = [  # (stapnaam, nu, waar): the questline in the Guhdex and on the travel map
    ("Klop aan bij Sausuman", "Ga naar de zwarte toren en praat met Sausuman. Hij wil iets van je.",
     "De Toren van Sausuman, een eindje lopen vanaf de boomstad van Guhladriel"),
    ("Deeg, saus en kaas", "Haal ringdeeg (eerste verdieping), hete frituursaus (tweede) en kaas (derde) uit de toren.",
     "Boven in de Toren van Sausuman"),
    ("Trek aan de hendel", "Trek aan de hendel van de Ringenbakker.", "De hal van de Toren van Sausuman"),
    ("De mokkende tovenaar", "Sausuman mokt in zijn Mokhoek. Bied hem een hapje van je uienring aan.", "De hal van de Toren van Sausuman"),
]
KORT = {"0": "Praat met Sausuman in zijn toren", "1": "Haal deeg, hete saus en kaas boven in de toren", "2": "Trek aan de hendel van de Ringenbakker",
        "3": "Bied Sausuman een hapje uienring aan"}
VERBORGEN = ["ring_sausuman_mok", "ring_sausuman_uienring", "ring_sausuman_dagelijks", "ring_sausuman_pannantir", "ring_sausuman_mikarad"]
ADV = [  # name, parent, icon, frame, title, description (tab knabbelring)
    ("ring_sausuman_mok", "ring_gekregen", "guhs:ringsausuman_uienring", "goal", "Eén uienring om ze allemaal op te eten",
     "Help Sausuman zijn eigen ring bakken. Het wordt een uienring. Hij mokt"),
]

# =====================================================================================================================
# textures
# =====================================================================================================================
KOPER, KOPER_L, KOPER_D = (196, 110, 78), (232, 150, 108), (140, 72, 52)
IJZER, IJZER_L, IJZER_D = (64, 62, 72), (104, 102, 114), (36, 34, 42)
HOUT, HOUT_L, HOUT_D = (98, 70, 48), (134, 100, 68), (62, 42, 30)
GLOED, GLOED_L = (240, 130, 30), (255, 214, 96)
ROEST = (150, 84, 50)


def _doek(kleur, donker, licht, seed, klinknagels=True):
    """A 16 x 16 plate: a base colour with a little noise, a darker rim and a rivet in each corner."""
    import random
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rng.randint(-7, 7)
            c = donker if min(x, y, 15 - x, 15 - y) == 0 else kleur
            px[x, y] = tuple(max(0, min(255, v + r)) for v in c) + (255,)
    if klinknagels:
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            px[x, y] = licht + (255,)
    return img


def bakker_voor():
    """The Ringenbakker's face: two gauges for eyes, a wide glowing mouth (where the ring comes out) and the lever on the right."""
    img = _doek(KOPER, KOPER_D, KOPER_L, 1)
    d = ImageDraw.Draw(img)
    for cx in (4, 9):                                           # the eyes: round gauges with a needle
        d.ellipse((cx - 2, 2, cx + 2, 6), fill=IJZER_D + (255,))
        d.ellipse((cx - 1, 3, cx + 1, 5), fill=(236, 232, 214, 255))
        img.putpixel((cx, 4), (190, 30, 30, 255))
    d.rectangle((2, 9, 11, 13), fill=IJZER_D + (255,))           # the mouth
    d.rectangle((3, 10, 10, 12), fill=GLOED + (255,))
    d.rectangle((4, 11, 9, 11), fill=GLOED_L + (255,))
    for x in (3, 5, 7, 9):                                      # teeth of the grate
        img.putpixel((x, 10), IJZER_D + (255,))
    d.rectangle((13, 5, 14, 13), fill=IJZER_D + (255,))          # the lever's slot, the handle and its red knob
    d.rectangle((13, 5, 13, 9), fill=IJZER_L + (255,))
    d.rectangle((12, 3, 14, 5), fill=(206, 44, 36, 255))
    img.putpixel((13, 3), (255, 130, 110, 255))
    return img


def bakker_zij():
    img = _doek(KOPER, KOPER_D, KOPER_L, 2)
    d = ImageDraw.Draw(img)
    d.rectangle((1, 7, 14, 8), fill=KOPER_D + (255,))            # a seam, and a pipe down the side
    d.rectangle((6, 1, 9, 14), fill=IJZER + (255,))
    d.rectangle((7, 1, 7, 14), fill=IJZER_L + (255,))
    for y in (4, 11):
        d.rectangle((5, y, 10, y), fill=IJZER_D + (255,))
    return img


def bakker_boven():
    img = _doek(KOPER, KOPER_D, KOPER_L, 3)
    d = ImageDraw.Draw(img)
    d.ellipse((3, 3, 12, 12), fill=IJZER_D + (255,))             # the funnel
    d.ellipse((5, 5, 10, 10), fill=(16, 12, 14, 255))
    return img


def voorraad_voor(soort):
    """The three stations: a trough of dough with a kneading arm, a copper tank with a dripping tap, an open cupboard with
    one onion on its shelf."""
    if soort == "deeg":
        img = _doek(HOUT, HOUT_D, HOUT_L, 4, klinknagels=False)
        d = ImageDraw.Draw(img)
        d.rectangle((1, 9, 14, 14), fill=HOUT_D + (255,))        # the trough
        d.rectangle((2, 8, 13, 12), fill=(236, 220, 176, 255))   # dough, bulging over the rim
        d.ellipse((3, 5, 9, 10), fill=(244, 230, 190, 255))
        d.ellipse((8, 6, 13, 10), fill=(226, 208, 160, 255))
        d.rectangle((7, 1, 8, 6), fill=IJZER + (255,))           # the kneading arm
        d.rectangle((5, 1, 10, 2), fill=IJZER_L + (255,))
        d.rectangle((1, 13, 14, 13), fill=IJZER + (255,))        # an iron band
    elif soort == "saus":
        img = _doek(KOPER, KOPER_D, KOPER_L, 5)
        d = ImageDraw.Draw(img)
        d.rectangle((4, 2, 11, 6), fill=(60, 40, 30, 255))       # a window in the tank: hot sauce
        d.rectangle((5, 3, 10, 5), fill=GLOED + (255,))
        d.rectangle((5, 3, 10, 3), fill=GLOED_L + (255,))
        d.rectangle((6, 8, 9, 9), fill=IJZER + (255,))           # the tap
        d.rectangle((7, 9, 8, 11), fill=IJZER_L + (255,))
        d.rectangle((5, 7, 10, 7), fill=(206, 44, 36, 255))      # its red handle
        img.putpixel((7, 12), GLOED + (255,))                    # a drop, and a puddle
        img.putpixel((8, 13), GLOED_L + (255,))
        d.rectangle((5, 14, 10, 14), fill=GLOED + (255,))
    else:
        img = _doek(HOUT, HOUT_D, HOUT_L, 6, klinknagels=False)
        d = ImageDraw.Draw(img)
        d.rectangle((2, 2, 13, 13), fill=(30, 22, 20, 255))      # the open cupboard: dark and empty
        d.rectangle((2, 7, 13, 7), fill=HOUT_L + (255,))         # two bare shelves
        d.rectangle((2, 12, 13, 12), fill=HOUT_L + (255,))
        d.rectangle((1, 1, 3, 14), fill=HOUT_L + (255,))         # the door, swung open to the left
        img.putpixel((3, 8), (250, 208, 84, 255))
        d.ellipse((8, 8, 11, 11), fill=(198, 150, 84, 255))      # the onion, all alone
        img.putpixel((9, 9), (236, 200, 140, 255))
        d.rectangle((9, 6, 9, 7), fill=(96, 160, 70, 255))       # its green sprout
        img.putpixel((10, 6), (96, 160, 70, 255))
        d.rectangle((6, 3, 12, 5), fill=(250, 226, 120, 255))    # the label: KAAS
        for x in (7, 9, 11):
            img.putpixel((x, 4), (120, 84, 30, 255))
    return img


def mikarad_voor():
    """The Mika-rad: a wheel behind bars, and in its hub the long face of an Uruk-Mika who would rather not."""
    img = _doek(IJZER, IJZER_D, IJZER_L, 7)
    d = ImageDraw.Draw(img)
    d.ellipse((1, 1, 14, 14), fill=IJZER_D + (255,))
    d.ellipse((2, 2, 13, 13), fill=(88, 66, 50, 255))            # the wheel's wooden tread
    d.ellipse((4, 4, 11, 11), fill=(150, 128, 112, 255))         # the Mika: grey-brown fur
    d.rectangle((4, 3, 5, 4), fill=(150, 128, 112, 255))         # ears
    d.rectangle((10, 3, 11, 4), fill=(150, 128, 112, 255))
    for x in (6, 9):                                            # half-closed eyes under a frown
        img.putpixel((x, 7), (30, 22, 24, 255))
        img.putpixel((x, 6), (88, 66, 50, 255))
    d.rectangle((7, 9, 8, 9), fill=(60, 40, 40, 255))            # the mouth: a flat line
    for x in (3, 8, 12):                                        # the bars
        d.rectangle((x, 1, x, 14), fill=IJZER_L + (255,))
    return img


def pijp_textuur():
    """The sputterpijp: sooty iron with a band of copper, darker towards the top."""
    img = _doek(IJZER, IJZER_D, IJZER_L, 8, klinknagels=False)
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            k = 0.6 + 0.4 * (y / 15)                             # soot at the top
            px[x, y] = (int(r * k), int(g * k), int(b * k), 255)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 9, 15, 10), fill=KOPER + (255,))
    d.rectangle((0, 9, 15, 9), fill=KOPER_L + (255,))
    for x in (2, 7, 12):
        px[x, 12] = IJZER_L + (255,)
    return img


def pan_textuur():
    """The Pannantir's top: an iron rim, simmering sauce, and in it an eye that looks back."""
    img = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=IJZER_D + (255,))
    d.rectangle((1, 1, 14, 14), fill=IJZER + (255,))
    d.rectangle((2, 2, 13, 13), fill=GLOED + (255,))
    d.rectangle((3, 3, 12, 12), fill=(250, 170, 50, 255))
    d.ellipse((4, 5, 11, 10), fill=GLOED_L + (255,))             # the eye in the sauce
    d.ellipse((6, 5, 9, 10), fill=(214, 60, 30, 255))
    d.rectangle((7, 6, 8, 9), fill=(30, 14, 14, 255))
    img.putpixel((3, 11), GLOED_L + (255,))                      # bubbles
    img.putpixel((12, 4), GLOED_L + (255,))
    return img


def _ring(buiten, binnen, basis, licht, donker, rand):
    """A ring icon (16 x 16): two ellipses and a hole, lit from the top left."""
    img = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(img)
    d.ellipse(buiten, fill=rand + (255,))
    d.ellipse((buiten[0] + 1, buiten[1] + 1, buiten[2] - 1, buiten[3] - 1), fill=basis + (255,))
    d.arc((buiten[0] + 1, buiten[1] + 1, buiten[2] - 1, buiten[3] - 1), 190, 290, fill=licht + (255,))
    d.arc((buiten[0] + 1, buiten[1] + 1, buiten[2] - 1, buiten[3] - 1), 10, 110, fill=donker + (255,))
    d.ellipse(binnen, fill=rand + (255,))
    d.ellipse((binnen[0] + 1, binnen[1] + 1, binnen[2] - 1, binnen[3] - 1), fill=(0, 0, 0, 0))
    return img


def item_textures(h):
    # the uienring: golden brown and crunchy, with crumbs
    ring = _ring((1, 2, 14, 13), (5, 6, 10, 9), (224, 160, 60), (255, 220, 130), (176, 110, 36), (120, 70, 24))
    for x, y in ((3, 5), (11, 4), (12, 9), (4, 10), (8, 11), (7, 3)):
        if ring.getpixel((x, y))[3]:
            ring.putpixel((x, y), (255, 236, 170, 255))
    h.save(ring, "item", "ringsausuman_uienring.png")
    # the ringdeeg: the same ring, pale and limp
    deeg = _ring((1, 3, 14, 13), (5, 7, 10, 9), (236, 220, 176), (250, 242, 214), (206, 186, 138), (160, 140, 100))
    h.save(deeg, "item", "ringsausuman_ringdeeg.png")
    # the kannetje hete frituursaus: a little iron jug, glowing sauce to the brim, a wisp of steam
    kan = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(kan)
    d.rectangle((4, 6, 10, 13), fill=IJZER + (255,))
    d.rectangle((4, 6, 4, 13), fill=IJZER_D + (255,))
    d.rectangle((9, 7, 9, 12), fill=IJZER_L + (255,))
    d.rectangle((5, 14, 9, 14), fill=IJZER_D + (255,))
    d.rectangle((5, 6, 9, 7), fill=GLOED + (255,))
    d.rectangle((6, 6, 8, 6), fill=GLOED_L + (255,))
    d.rectangle((11, 8, 12, 8), fill=IJZER + (255,))             # the handle
    d.rectangle((12, 8, 12, 11), fill=IJZER + (255,))
    d.rectangle((11, 11, 12, 11), fill=IJZER + (255,))
    kan.putpixel((3, 6), IJZER_L + (255,))                       # the spout
    for x, y in ((6, 4), (7, 3), (6, 2), (8, 1)):
        kan.putpixel((x, y), (230, 230, 230, 150))
    h.save(kan, "item", "ringsausuman_frituursaus.png")
    # the ui
    ui = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(ui)
    d.ellipse((3, 5, 12, 14), fill=(132, 92, 44, 255))
    d.ellipse((4, 6, 11, 13), fill=(198, 150, 84, 255))
    d.arc((5, 6, 10, 13), 90, 270, fill=(236, 200, 140, 255))
    d.line((8, 6, 8, 13), fill=(168, 122, 64, 255))
    d.rectangle((7, 3, 8, 5), fill=(160, 120, 70, 255))
    d.line((8, 3, 10, 1), fill=(96, 160, 70, 255))
    d.line((7, 3, 6, 1), fill=(70, 130, 56, 255))
    d.rectangle((6, 14, 9, 14), fill=(110, 76, 40, 255))
    h.save(ui, "item", "ringsausuman_ui.png")
    for naam in ("uienring", "ringdeeg", "frituursaus", "ui"):
        h.item_model(f"ringsausuman_{naam}")


def textures(h):
    h.save(bakker_voor(), "block", "ringsausuman_ringenbakker_voor.png")
    h.save(bakker_zij(), "block", "ringsausuman_ringenbakker_zij.png")
    h.save(bakker_boven(), "block", "ringsausuman_ringenbakker_boven.png")
    for soort in bouw.SOORTEN:
        h.save(voorraad_voor(soort), "block", f"ringsausuman_voorraad_{soort}.png")
    h.save(_doek(HOUT, HOUT_D, HOUT_L, 10, klinknagels=False), "block", "ringsausuman_voorraad_zij.png")
    h.save(mikarad_voor(), "block", "ringsausuman_mikarad_voor.png")
    h.save(_doek(IJZER, IJZER_D, IJZER_L, 11), "block", "ringsausuman_mikarad_zij.png")
    h.save(pijp_textuur(), "block", "ringsausuman_sputterpijp.png")
    h.save(pan_textuur(), "block", "ringsausuman_pannantir.png")
    h.save(_doek(IJZER, IJZER_D, IJZER_L, 9, klinknagels=False), "block", "ringsausuman_pannantir_ijzer.png")
    item_textures(h)
    modellen.build(h)


# =====================================================================================================================
# block models
# =====================================================================================================================
def blokken(h):
    A = h.A

    def orientable(naam, voor, zij, boven):
        h.w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/orientable",
                                              "textures": {"front": f"guhs:block/{voor}", "side": f"guhs:block/{zij}", "top": f"guhs:block/{boven}"}})
    orientable("ringsausuman_ringenbakker", "ringsausuman_ringenbakker_voor", "ringsausuman_ringenbakker_zij", "ringsausuman_ringenbakker_boven")
    h.w(f"{A}/blockstates/ringsausuman_ringenbakker.json", {"variants": h.facing_states("ringsausuman_ringenbakker")})
    orientable("ringsausuman_mikarad", "ringsausuman_mikarad_voor", "ringsausuman_mikarad_zij", "ringsausuman_mikarad_zij")
    h.w(f"{A}/blockstates/ringsausuman_mikarad.json", {"variants": h.facing_states("ringsausuman_mikarad")})
    varianten = {}
    for soort in bouw.SOORTEN:
        orientable(f"ringsausuman_voorraad_{soort}", f"ringsausuman_voorraad_{soort}", "ringsausuman_voorraad_zij", "ringsausuman_voorraad_zij")
        varianten.update(h.facing_states(None, model=f"guhs:block/ringsausuman_voorraad_{soort}", extra=f",soort={soort}"))
    h.w(f"{A}/blockstates/ringsausuman_voorraad.json", {"variants": varianten})
    # the sputterpijp: a pipe with a wider collar at its mouth
    t = "#pijp"

    def doos(frm, to, uv_zij, top=True):
        faces = {f: {"uv": uv_zij, "texture": t} for f in ("north", "south", "west", "east")}
        faces["down"] = {"uv": [frm[0], frm[2], to[0], to[2]], "texture": t}
        if top:
            faces["up"] = {"uv": [frm[0], frm[2], to[0], to[2]], "texture": "#gat"}
        return {"from": frm, "to": to, "faces": faces}
    h.w(f"{A}/models/block/ringsausuman_sputterpijp.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"pijp": "guhs:block/ringsausuman_sputterpijp", "gat": "guhs:block/ringsausuman_ringenbakker_boven",
                     "particle": "guhs:block/ringsausuman_sputterpijp"},
        "elements": [doos([5, 0, 5], [11, 12, 11], [5, 4, 11, 16], top=False), doos([4, 12, 4], [12, 16, 12], [4, 0, 12, 4])]})
    h.w(f"{A}/blockstates/ringsausuman_sputterpijp.json", {"variants": {f"groot={g}": {"model": "guhs:block/ringsausuman_sputterpijp"}
                                                                         for g in ("false", "true")}})
    h.w(f"{A}/models/item/ringsausuman_sputterpijp.json", {"parent": "guhs:block/ringsausuman_sputterpijp"})
    # the Pannantir: a pan full of sauce on a foot, with a handle
    def stuk(frm, to, saus=False):
        faces = {f: {"texture": "#ijzer"} for f in ("north", "south", "west", "east", "down", "up")}
        if saus:
            faces["up"] = {"uv": [0, 0, 16, 16], "texture": "#saus"}
        return {"from": frm, "to": to, "faces": faces}
    h.w(f"{A}/models/block/ringsausuman_pannantir.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"saus": "guhs:block/ringsausuman_pannantir", "ijzer": "guhs:block/ringsausuman_pannantir_ijzer",
                     "particle": "guhs:block/ringsausuman_pannantir_ijzer"},
        "elements": [stuk([2, 6, 2], [14, 9, 14], saus=True), stuk([6, 1, 6], [10, 6, 10]), stuk([4, 0, 4], [12, 1, 12]),
                     stuk([14, 7, 7], [16, 8, 9])]})
    h.w(f"{A}/blockstates/ringsausuman_pannantir.json", {"variants": {"": {"model": "guhs:block/ringsausuman_pannantir"}}})
    h.w(f"{A}/models/item/ringsausuman_pannantir.json", {"parent": "guhs:block/ringsausuman_pannantir"})


# =====================================================================================================================
# data: loot, recipes, tags, sounds
# =====================================================================================================================
def data(h):
    h.self_drop("ringsausuman_sputterpijp")
    h.self_drop("ringsausuman_pannantir")
    # sputterpijpen for your own factory: roosterijzer round a glowing coal
    h.shaped("ringsausuman_sputterpijp", ["I I", "IKI", "I I"], {"I": "guhs:gepolijst_roosterijzer", "K": "guhs:gloeikool"},
             "guhs:ringsausuman_sputterpijp", 4)
    h.add_tag("guhs/tags/item/loaned", [f"guhs:ringsausuman_{n}" for n in ("ringdeeg", "frituursaus", "ui")])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:ringsausuman_{n}" for n in ("sputterpijp", "pannantir", "ringenbakker", "mikarad")])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:ringsausuman_voorraad"])
    vadskracht.toon(h, "ringsausuman_mikarad")           # the hover readout of Guh-technologie works on the Mika-rad

    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# the tower
# =====================================================================================================================
def structuur(h):
    s, problems = bouw.toren(h)
    if problems:
        raise SystemExit("ring_sausuman: the tower is not right:\n  " + "\n  ".join(problems[:40]))
    s.save(STRUCTUUR)
    bouw.test_kamer(h).save("ringsausuman_test_kamer")
    # exactly one tower per world, a walk from the tree city of chapter 4. As long as ring-h4 has not declared its structure
    # (this slice was built before it) the copy stands in a sector of its own, so the tower exists in a world of this branch
    if BOOMSTAD in wereld.STRUCTUREN:
        gegarandeerd = dict(rond=BOOMSTAD, min=200, max=400)
    else:
        gegarandeerd = dict(sector=13, min=250, max=900)
    wereld.bbq_structuur(h, STRUCTUUR, soort="grot", titel="Toren van Sausuman",
                         tooltip="De zwarte toren van Sausuman, de tovenaar-Mika die ook een hapje wil (Guhbarbecuether)",
                         biomes=wereld.BBQ, salt=SALT, templates=[(STRUCTUUR, 1)], gegarandeerd=gegarandeerd, voorrang=VOORRANG,
                         kompas="barbecue", grootte=24, vlak=10, hoogte=HOOGTE)
    # the centre jigsaw is in layer 0 and the pool says where the ground really is (as fossiel-mijn, sausdieren and toren-peper do)

    def grond(pool, delta=bouw.G + 1):
        for e in pool["elements"]:
            el = {"element_type": "guhs:grond_single_pool_element"}
            el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
            el["ground_level_delta"] = delta
            e["element"] = el
    h.patch_json(f"{h.D}/worldgen/template_pool/{STRUCTUUR}/start.json", grond)
    verhaal_motor.sluier(h, STRUCTUUR)


# =====================================================================================================================
# texts, advancements, the questline
# =====================================================================================================================
def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for event, text in SUBTITLES.items():
        h.lang(f"subtitles.guhs.{event}", text, text)
    for name in VERBORGEN:
        bbq2.verborgen(h, name)
    for name, parent, icon, frame, titel, tekst in ADV:
        bbq2.zichtbaar(h, "knabbelring", name, parent, icon, frame, titel, tekst)
    verhaal_motor.verhaallijn(
        h, "ring_sausuman", "De Toren van Sausuman",
        "Sausuman van de Vele Sauzen wil ook een hapje van de Knabbelring. Hij krijgt het niet, dus bakt hij er zelf een. Wat kan er misgaan? "
        "(Dit hoeft niet voor de reis.)", STAPPEN,
        klaar=("Sausuman mokt nog steeds, maar de Ringenbakker bakt elke dag een uienring voor je. Vahoeg!", "De Toren van Sausuman"), kort=KORT)
    verhaal_motor.scene(h, "ringsausuman_bakken", "De Ringenbakker bakt", SCENE, namen={"sausuman": "Sausuman", "sam": "Sam-guh"})


# =====================================================================================================================
# self-check
# =====================================================================================================================
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ringsausuman")


def java_plekken():
    """The template coordinates in Toren.java: ({name: [(x, y, z), ...]}, G, NPC_YAW)."""
    src = open(os.path.join(JAVA, "Toren.java"), encoding="utf-8").read()
    uit = {}
    for m in re.finditer(r"public static final [\w<>]+ (\w+) = (.*?);", src, re.S):
        punten = [tuple(int(v) for v in p) for p in re.findall(r"new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)", m.group(2))]
        if punten:
            uit[m.group(1)] = punten
    g = re.search(r"public static final int G = (\d+);", src)
    yaw = re.search(r"public static final float NPC_YAW = (-?[\d.]+)f;", src)
    return uit, int(g.group(1)) if g else None, float(yaw.group(1)) if yaw else None


def java_getal(bestand, naam):
    src = open(os.path.join(JAVA, bestand), encoding="utf-8").read()
    m = re.search(rf"\b{naam} = (\d+)", src)
    return int(m.group(1)) if m else None


def selfcheck(h):
    problems = modellen.check(h)
    # the geometry Java uses is the geometry of the template
    java, g, yaw = java_plekken()
    for naam, wat in bouw.PLEKKEN.items():
        py = [tuple(wat)] if isinstance(wat, tuple) else [tuple(p) for p in wat]
        if java.get(naam) != py:
            problems.append(f"Toren.java {naam} = {java.get(naam)}, the template has {py}")
    if g != bouw.G or yaw != bouw.NPC_YAW:
        problems.append(f"Toren.java G / NPC_YAW = {g} / {yaw}, the template has {bouw.G} / {bouw.NPC_YAW}")
    # the numbers the texts and the Java side share
    if java_getal("PannantirBlock.java", "BEELDEN") != BEELDEN or any(f"{GUI}pannantir.{i}" not in LANG for i in range(BEELDEN)):
        problems.append("PannantirBlock.BEELDEN and the visions in LANG differ")
    if java_getal("MikaradBlock.java", "VERMOGEN") != VERMOGEN:
        problems.append("MikaradBlock.VERMOGEN differs from the python side")
    vraag = sum(vadskracht.getal(n) for n in ("KNABBELAAR", "KNUTSELMACHINE", "OOGSTER", "GUH_OVEN", "MOLEN"))
    if len(bouw.HAL_MACHINES) != 5 or vraag <= VERMOGEN:
        problems.append(f"the hall must hold five machines that ask more than the Mika-rad gives (now {len(bouw.HAL_MACHINES)}, {vraag} against {VERMOGEN})")
    m = re.search(r'maak\("ring_sausuman", "knabbelring"\)\.stappen\((\d+)\)\.na\("ring_h4"\)', open(os.path.join(JAVA, "RingSausumanFeature.java"), encoding="utf-8").read())
    if not m or int(m.group(1)) != len(STAPPEN):
        problems.append("RingSausumanFeature.LIJN: the id, the number of steps or the `na` chain differs from the texts")
    for key in list(LANG) + [f"subtitles.guhs.{e}" for e in SOUNDS] + [f"structure.guhs.{STRUCTUUR}", "gui.guhs.verhalen.ring_sausuman.naam",
                                                                     "scene.guhs.ringsausuman_bakken.titel"]:
        if key not in h.NL:
            problems.append(f"missing lang {key}")
    for key, text in list(LANG.items()) + list(SCENE.items()):
        if "hamster" in text.lower():
            problems.append(f"lore: {key}")
    # every text key the Java sources name exists (also the keys built from a prefix and an id)
    for f in os.listdir(JAVA):
        if not f.endswith(".java") or f.endswith("GameTests.java"):
            continue
        src = open(os.path.join(JAVA, f), encoding="utf-8").read()
        prefixen = dict(re.findall(r'String (\w+) = "((?:quest|gui)\.guhs\.ringsausuman\.[\w.]*)";', src))
        for m in re.finditer(r'(?:(\b[A-Z]\w*) \+ )?"((?:(?:quest|gui)\.guhs\.ringsausuman\.)?[a-z_][\w.]*)"', src):
            pre, rest = m.groups()
            key = prefixen[pre] + rest if pre in prefixen else rest
            if not key.startswith(("quest.guhs.ringsausuman.", "gui.guhs.ringsausuman.")):
                continue
            if key.endswith("."):
                if not any(k.startswith(key) for k in h.NL):
                    problems.append(f"{f} names texts {key}*, none exists")
            elif key not in h.NL:
                problems.append(f"{f} names the text {key}, which does not exist")
        for m in re.finditer(r'\.zeg\(\d+, "\w*", "(\w+)"', src):
            if f"scene.guhs.ringsausuman_bakken.{m.group(1)}" not in h.NL:
                problems.append(f"{f}: the scene says {m.group(1)}, which has no text")
    for name in re.findall(r'grant\(\w+, "(ring_sausuman_\w+)"\)|behaald\(\w+, "(ring_sausuman_\w+)"\)', "".join(
            open(os.path.join(JAVA, f), encoding="utf-8").read() for f in os.listdir(JAVA) if f.endswith(".java") and not f.endswith("GameTests.java"))):
        if (name[0] or name[1]) not in VERBORGEN:
            problems.append(f"the Java side grants quest/{name[0] or name[1]}, which this module does not write")
    for soort in bouw.SOORTEN:
        for key in (f"{GUI}voorraad.pak.{soort}", f"{GUI}voorraad.heb_je.{soort}", f"{GUI}voorraad.klaar.{soort}"):
            if key not in h.NL:
                problems.append(f"missing lang {key}")
    for p in ([f"{h.D}/structure/{n}.nbt" for n in (STRUCTUUR, "ringsausuman_test_kamer")]
              + [f"{h.D}/worldgen/structure/{STRUCTUUR}.json", f"{h.D}/worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json",
                 f"{h.D}/advancement/quest/ring_sausuman_stap_{len(STAPPEN)}.json", f"{h.D}/advancement/knabbelring/ring_sausuman_mok.json",
                 f"{h.D}/loot_table/blocks/ringsausuman_pannantir.json", f"{h.D}/recipe/ringsausuman_sputterpijp.json"]
              + [os.path.join(h.TEX, "item", f"ringsausuman_{t}.png") for t in ("uienring", "ringdeeg", "frituursaus", "ui")]
              + [f"{h.A}/blockstates/ringsausuman_{b}.json" for b in ("ringenbakker", "voorraad", "mikarad", "sputterpijp", "pannantir")]):
        if not os.path.exists(p):
            problems.append(f"missing file {p}")
    pool = json.load(open(f"{h.D}/worldgen/template_pool/{STRUCTUUR}/start.json", encoding="utf-8"))
    if any(e["element"].get("ground_level_delta") != bouw.G + 1 for e in pool["elements"]):
        problems.append("the start pool of the tower does not say where the ground is")
    verborgen = json.load(open(f"{h.D}/kaart/verborgen.json", encoding="utf-8"))["structures"]
    if f"guhs:{STRUCTUUR}" not in verborgen:
        problems.append("the tower is not on the map-hide list")
    sounds = json.load(open(f"{h.A}/sounds.json", encoding="utf-8"))
    for e in SOUNDS:
        if e not in sounds:
            problems.append(f"sounds.json misses {e}")
    if problems:
        raise SystemExit("ring_sausuman self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    textures(h)
    blokken(h)
    data(h)
    structuur(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_knabbelring, the last section; the chapter is linear)
# =====================================================================================================================
def ftb(fq):
    """The tower hangs under chapter 4, not under the end of the story: its first quest names a quest of ring_h4 (the last
    one that module added; while ring_h4 has none yet: ring-kern's "De gaven van Guhladriel", which is given in chapter 4).
    The quests after it are a chain of their own; the three extras each name the first quest (a branch that locks nothing)."""
    q, adv = fq.q, fq.adv
    h4 = [x[0] for x in fq.QUESTS if x[0].startswith("ring_h4_")]
    na = [h4[-1] if h4 else "ring_gaven"]
    q("ring_sausuman_toren", "Een zwarte toren vol rook",
      "Niet ver van de boomstad van Guhladriel staat een zwarte toren te sputteren. Daar woont &6Sausuman van de Vele Sauzen&r, een "
      "tovenaar-Mika die ook een hapje van de ring wil. Je &dSuperkompas&r weet de weg (tab Barbecue). Het hoeft niet voor de reis, "
      "maar het ruikt er wel lekker, njeg.",
      "minecraft:polished_blackstone_bricks", [adv("ring_sausuman_stap_1")], deps=na, shape="hexagon")
    q("ring_sausuman_ingredienten", "Deeg, saus en... kaas?",
      "Sausuman krijgt geen hapje, dus bakt hij zelf een ring. Haal voor zijn &6Ringenbakker&r: &6ringdeeg&r van de Deegkneder (eerste "
      "verdieping), &6hete frituursaus&r uit de Sauskraan (tweede) en &6kaas&r uit de Kaaskast (derde). Die kast is alleen een beetje leeg.",
      "guhs:ringsausuman_ringdeeg", [adv("ring_sausuman_stap_2")])
    q("ring_sausuman_bakken", "Eén ring om ze allemaal op te eten",
      "Alles bij je? Trek dan aan de hendel van de &6Ringenbakker&r, beneden in de hal. Achteruit: hij ronkt, hij sputtert, hij schudt... "
      "PLING!", "guhs:ringsausuman_frituursaus", [adv("ring_sausuman_stap_3")])
    q("ring_sausuman_mok", "Ik mok",
      "Het is een &6uienring&r geworden. Sausuman wilde kaas, dit is groente, dus nu zit hij in zijn Mokhoek. Bied hem een hapje aan. "
      "Je krijgt de &6Pannantír&r, een handvol uienringen, en de Ringenbakker bakt voortaan elke dag een uienring voor je.",
      "guhs:ringsausuman_uienring", [adv("ring_sausuman_stap_4")], rewards=(("guhs:kaas_knabbels", 12),), shape="gear", xp=100)
    eerste = ["ring_sausuman_toren"]
    q("ring_sausuman_te_zwaar", "Te zwaar, njeg",
      "Kijk eens naar de machines in de hal: &cTe zwaar: er is vadskracht te weinig, dus alles staat stil&r. Sausuman heeft vijf echte "
      "machines aan één &6Mika-rad&r gehangen, en een Mika rent niet. Por de Mika maar eens (rechtsklik op het rad). Zo moet het dus niet. "
      "Hoe het wel moet staat bij &bGuh-technologie&r: een Guhrad, met een guh erin.", "guhs:guh_wire", [adv("ring_sausuman_mikarad")],
      rewards=(("guhs:kaas_knabbels", 4),), deps=eerste)
    q("ring_sausuman_pannantir", "De Pannantír",
      "Boven in de toren staat een pan waar je dingen in ziet. Kijk er eens in (rechtsklik). Meestal zie je een oog dat trek heeft. Na "
      "afloop mag je er zelf een mee naar huis nemen.", "guhs:ringsausuman_pannantir", [adv("ring_sausuman_pannantir")],
      rewards=(("guhs:kaas_knabbels", 4),), deps=eerste)
    q("ring_sausuman_dagelijks", "Elke dag een ring",
      "Als Sausuman mokt, doet de &6Ringenbakker&r het nog steeds: trek aan de hendel en je krijgt één &6uienring&r per dag. Hij kijkt "
      "niet. Hij kijkt nooit.", "guhs:ringsausuman_uienring", [adv("ring_sausuman_dagelijks")], rewards=(("guhs:kaas_knabbels", 4),),
      deps=["ring_sausuman_mok"])
