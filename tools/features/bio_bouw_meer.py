"""
biomes3 slice "bouw-meer" (Java: feature/bio/bouwmeer; English: tools/lang/en/c87_bio_bouw_meer.json; contract: CONTRACT_BIO.md).

What stands at the Bloesemmeertje:
  - het botenhuisje (structure botenhuisje, guhs:bio_plek kind meer_oever, Superkompas tab knus): boathouse, jetty,
    de meerpaal (anchor block), the roeibootje (entity; vanilla's boat renderer with our own picture), the visser-guh
    (NPC kind botenhuisje_visserguh: his own model with a straw hat and a rod), the steigerlantaarn; a processor list
    (botenhuisje_palen) turns the posts into waterlogged posts where they stand in the lake's water;
  - het picknickeilandje (structure picknickeilandje, kind meer_boom, tab knus): rug, de picknickmand (anchor block,
    a treat per player from loot table chests/picknickeilandje), the hanami guhs (NPC kinds hanami_*);
  - three decorations: botenhuisje_steigerlantaarn, botenhuisje_hengelstandaard, botenhuisje_koiwindzak (reward + recipes);
  - eight hidden proof advancements guhs:quest/... for slice systemen (BEWIJZEN).
The templates and the numbers Java shares with them: bio_bouw_meer_bouw.py.
"""
import json
import os

import numpy as np
from PIL import Image

from features import bio_bouw_meer_bouw as bouw
from features import bio_lib as lib
from features import bio_wereld_plek as bio_plek
from features import sterrenwacht_hulp as hulp

BLOKKEN = ["botenhuisje_meerpaal", "botenhuisje_steigerlantaarn", "botenhuisje_hengelstandaard", "botenhuisje_koiwindzak", "picknickeilandje_mand"]
MET_ITEM = ["botenhuisje_steigerlantaarn", "botenhuisje_hengelstandaard", "botenhuisje_koiwindzak"]
NPCS = ["botenhuisje_visserguh", "hanami_guh", "hanami_guh_slaapt", "hanami_bloesemguh", "hanami_bloesemguh_slaapt"]
STRUCTUREN = ["botenhuisje", "picknickeilandje"]
PALEN = "botenhuisje_palen"            # the processor list of the botenhuisje's start pool (see data())
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
BEWIJZEN = {  # guhs:quest/<name> (Java: BouwMeerSlice.BEWIJZEN)
    "botenhuisje_gevonden": {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:botenhuisje"}}}}},
    "botenhuisje_visser": IMPOSSIBLE, "botenhuisje_koivoer": IMPOSSIBLE, "botenhuisje_visser_klaar": IMPOSSIBLE, "roeibootje_gevaren": IMPOSSIBLE,
    "picknickeilandje_gevonden": {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:picknickeilandje"}}}}},
    "picknickeilandje_mand": IMPOSSIBLE, "hanami_gesproken": IMPOSSIBLE,
}
# structure sets: (biomes, spacing, separation, salt, kind of spot); frequencies: see the slice report
SETS = {
    # biomes3 fix-plaatsing: both are one per lake now (bio_wereld_plek.PER_REGIO: a botenhuisje at every lake, two at the
    # largest; a picknickeilandje at about one lake in three); the set is only the grid whose cell the chosen spot lies in
    "botenhuisje": (["bloesemmeertje", "klaterdal"], 4, 0, 21500701, "meer_oever"),
    "picknickeilandje": (["bloesemmeertje"], 4, 0, 21500711, "meer_boom"),
}

# =====================================================================================================================
# texts (Dutch; English later in tools/lang/en/c87_bio_bouw_meer.json)
# =====================================================================================================================
GROET = {
    "ochtend": ["Goeiemorgen, njeg. De mist hangt nog boven het water en de koi slapen uit. Ik ook, eigenlijk.",
                "Vroeg op, vads? 's Ochtends is het meer op zijn stilst. Luister maar. ... Precies. Niks. Njeg."],
    "middag": ["Ha, njeg. Ga zitten. De koi doen hun rondje en ik doe het mijne: zitten.",
               "Weet je wat het mooie is van vissen, vads? Dat je niks hoeft te vangen. Njeg."],
    "avond": ["Kijk, de lantaarn gaat aan. Dan weet ik dat het avond is, njeg. Een horloge heb ik niet. Het meer is nooit te laat.",
              "De zon zakt in het water en de koi worden van goud. Elke avond weer. Vahoeg, hè?"],
    "nacht": ["Sst, njeg. De koi slapen. Ik vis nu op sterren: die bijten ook niet, maar ze spiegelen mooi.",
              "Nog wakker, vads? Ik ook. 's Nachts hoor je de bloesem vallen. Plop. Plop. Njeg."],
}
GEDACHTEN = [
    "Ze bijten nooit, njeg, daarom vis ik ze ook niet. Er zit geen haakje aan mijn lijn, alleen een bloesemblaadje. De koi komen kijken, ik kijk terug. "
    "Zo vangen we elkaar een beetje.",
    "Een koi wordt wel honderd jaar, zeggen ze. Die heeft dus alle tijd, njeg. Daar kan een guh nog wat van leren. Ik oefen elke dag.",
    "Vroeger wilde ik de grootste koi van het meer vangen. Nu weet ik: de grootste koi is degene die je laat zwemmen. Vads. Diep, hè? Net als het meer.",
]
LIEF = [
    "Kijk dan omhoog, njeg. Elk blaadje valt maar één keer. Daarom kijken we.",
    "Kom erbij zitten, vads. Er is taart. En bloesem. Meer hoeft niet.",
    "Sst... die daar slaapt. Die is al sinds het eerste blaadje moe van het kijken. Njeg.",
    "Hanami! Dat betekent: bloemen kijken. Wij doen het elk jaar. En elke dag. Vahoeg, jij bent er ook!",
]
GEWOON = ["Njeg! Bloesem! Daar! En daar! En daar ook!", "Er zit lekkers in de mand. Pak maar, er is voor iedereen wat. Vads!"]
SLAAP = ["Zzz... njeg... nog één blaadje... zzz", "Zzz... bloesem... vahoeg... zzz", "*snurkje* ...taart... njegnjeg... zzz"]

TEXTS = {
    # names
    "entity.guhs.roeibootje": "Roeibootje",
    "entity.guhs.guh_npc.botenhuisje_visserguh": "Visser-guh",
    "entity.guhs.guh_npc.hanami_guh": "Hanami-guh",
    "entity.guhs.guh_npc.hanami_guh_slaapt": "Hanami-guh",
    "entity.guhs.guh_npc.hanami_bloesemguh": "Hanami-bloesemguh",
    "entity.guhs.guh_npc.hanami_bloesemguh_slaapt": "Hanami-bloesemguh",
    "block.guhs.botenhuisje_meerpaal": "Meerpaal",
    "block.guhs.botenhuisje_steigerlantaarn": "Steigerlantaarn",
    "block.guhs.botenhuisje_steigerlantaarn.lore": "Gaat vanzelf aan als het donker wordt. Voor aan het eind van je steiger",
    "block.guhs.botenhuisje_hengelstandaard": "Hengel op een standaard",
    "block.guhs.botenhuisje_hengelstandaard.lore": "Zonder haakje, met een bloesemblaadje. Zo vist de visser-guh",
    "block.guhs.botenhuisje_koiwindzak": "Koi-windzak",
    "block.guhs.botenhuisje_koiwindzak.lore": "Een vrolijke koi aan een stok, voor bij het water",
    "block.guhs.picknickeilandje_mand": "Picknickmand",
    "structure.guhs.botenhuisje": "Botenhuisje",
    "structure.guhs.botenhuisje.tooltip": "Een licht botenhuisje met een steiger aan een Bloesemmeertje: de visser-guh leent je zijn roeibootje",
    "structure.guhs.picknickeilandje": "Picknickeilandje",
    "structure.guhs.picknickeilandje.tooltip": "Zeldzaam: guhs vieren hanami onder de grootste bloesemboom van een eiland in een Bloesemmeertje",
    # the boat
    "gui.guhs.roeibootje.te_ver": "Het roeibootje hoort bij zijn meer, njeg. Het dobbert terug naar de steiger.",
    "gui.guhs.roeibootje.geroepen": "Er ligt een roeibootje voor je klaar aan de meerpaal.",
    "gui.guhs.roeibootje.ligt_klaar": "Het roeibootje ligt al klaar. Stap maar in!",
    # the visser-guh: his answers
    "gui.guhs.botenhuisje.optie.bijten": "Bijten ze een beetje?",
    "gui.guhs.botenhuisje.optie.koivoer": "Heb je koivoer voor me?",
    "gui.guhs.botenhuisje.optie.bootje": "Mag ik het roeibootje lenen?",
    "gui.guhs.botenhuisje.optie.meer": "Leer me het meer kennen",
    "gui.guhs.botenhuisje.optie.meer_bezig": "Wat moest ik ook alweer doen?",
    "gui.guhs.botenhuisje.optie.meer_klaar": "Vertel nog eens over het meer",
    "gui.guhs.botenhuisje.visser.koivoer": "Hier, een handje koivoer. Strooi het op het water en ze komen vanzelf, njeg. Morgen heb ik weer wat voor je.",
    "gui.guhs.botenhuisje.visser.koivoer_op": "Voor vandaag ben ik door mijn voer heen, vads. De koi mogen ook niet te dik worden. Kom morgen maar terug.",
    "gui.guhs.botenhuisje.visser.bootje": "Tuurlijk, njeg. Het ligt aan de meerpaal, stap maar in. Roei waar je wilt, zolang het maar op het meer is. Laat je het "
                                           "ergens liggen, dan dobbert het vanzelf weg en ligt er hier weer eentje klaar. Dat bootje kent het meer beter dan ik.",
    # the lessons
    "gui.guhs.botenhuisje.les.begin": "Wil je het meer leren kennen, njeg? Dat gaat niet met haast. Vier kleine dingen. Eerst: voer een koi. Hier is wat "
                                      "koivoer. Strooi het op het water bij de steiger en kijk wat er gebeurt.",
    "gui.guhs.botenhuisje.les.voer_nog": "Eerst een koi voeren, vads. Strooi het koivoer op het water, daar waar ze zwemmen. Geen haast. Ze komen wel.",
    "gui.guhs.botenhuisje.les.voer_klaar": "Zag je dat, njeg? Ze komen niet voor jou, ze komen voor het voer. Maar ze blijven voor het gezelschap. Twee: op het "
                                           "water drijven bloesemblaadjes. Breng me er eentje. Voorzichtig oppakken, ze zijn dun.",
    "gui.guhs.botenhuisje.les.blaadje_nog": "Een drijvend bloesemblaadje, vads. Ze liggen op het water, vooral onder de bomen. Eentje is genoeg.",
    "gui.guhs.botenhuisje.les.blaadje_klaar": "Dank je, vads. Dit blaadje viel van de boom, dreef een rondje en ligt nu in mijn poot. Zo gaat alles hier in "
                                              "rondjes. Drie: neem het roeibootje en roei naar een groot eiland. Stap er even uit en kijk om je heen.",
    "gui.guhs.botenhuisje.les.eiland_nog": "Het roeibootje ligt aan de meerpaal, njeg. Roei naar een groot eiland, zo eentje met een flinke boom, en stap er "
                                           "even uit. Een stapsteen telt niet. Daar pas je niet eens op.",
    "gui.guhs.botenhuisje.les.eiland_klaar": "Je bent op het eiland geweest, ik zie het aan je oren. Njeg. Vanaf daar is deze steiger maar een streepje, hè? Zo "
                                             "klein zijn wij. Het laatste: vang een koi. Hier is mijn emmer, schep er water mee en dan een koi. En laat hem "
                                             "daarna weer vrij. Dat laatste is het belangrijkste.",
    "gui.guhs.botenhuisje.les.koi_nog": "Schep eerst water in de emmer, vads, en dan een koi. Rustig aan. En daarna laat je hem weer zwemmen.",
    "gui.guhs.botenhuisje.les.koi_loslaten": "Je hebt er eentje in je emmer, njeg. Mooi zo. En nu het moeilijke deel: loslaten. Giet hem terug in het water.",
    "gui.guhs.botenhuisje.les.klaar": "Gevangen en weer losgelaten. Vahoeg. Nu ken je het meer, njeg: je mag alles even vasthouden, zolang je het teruggeeft. "
                                      "Hier, voor jouw eigen plekje aan het water.",
    "gui.guhs.botenhuisje.les.klaar.chat": "Je kent het meer! De visser-guh gaf je twee steigerlantaarns, een hengel op een standaard en een koi-windzak.",
    "gui.guhs.botenhuisje.les.na": "Je kent het meer al, vads. Nu hoef je alleen nog maar te komen zitten. Dat is het moeilijkste van alles, njeg.",
    "quest.guhs.botenhuisje.voer": "Voer een koi: strooi koivoer op het water waar koi zwemmen",
    "quest.guhs.botenhuisje.blaadje": "Breng de visser-guh een drijvend bloesemblaadje",
    "quest.guhs.botenhuisje.eiland": "Roei met het roeibootje naar een groot eiland en stap aan land",
    "quest.guhs.botenhuisje.koi": "Vang een koi met een emmer water, en laat hem weer vrij",
    "quest.guhs.botenhuisje.loslaten": "Laat de koi uit je emmer weer vrij in het water",
    "quest.guhs.botenhuisje.terug": "Ga terug naar de visser-guh op de steiger",
    # the picnic
    "gui.guhs.picknickeilandje.mand.pak": "Wat lekkers uit de picknickmand, van de hanami-guhs. Voor jou!",
    "gui.guhs.picknickeilandje.mand.gehad": "Jij hebt je lekkers al gehad, njeg. De rest is voor wie nog komt.",
}
for _deel, _regels in GROET.items():
    for _i, _regel in enumerate(_regels):
        TEXTS[f"gui.guhs.botenhuisje.visser.{_deel}.{_i}"] = _regel
for _i, _regel in enumerate(GEDACHTEN):
    TEXTS[f"gui.guhs.botenhuisje.visser.koi.{_i}"] = _regel
for _naam, _regels in (("lief", LIEF), ("guh", GEWOON), ("slaap", SLAAP)):
    for _i, _regel in enumerate(_regels):
        TEXTS[f"gui.guhs.hanami.{_naam}.{_i}"] = _regel


# =====================================================================================================================
# block textures
# =====================================================================================================================
def _ruis(naam, basis, var=5, n=16):
    r = lib.rng(naam)
    a = np.clip(np.array(basis, np.float32)[None, None, :] + r.normal(0, var, (n, n, 1)), 0, 255)
    return np.concatenate([a, np.full((n, n, 1), 255, np.float32)], axis=2).astype(np.uint8)


def _bloesem(a, cx, cy, blad=(246, 160, 196), hart=(255, 224, 130)):
    for (dx, dy) in ((0, -2), (2, -1), (1, 2), (-1, 2), (-2, -1)):
        for (ex, ey) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            a[cy + dy + ey, cx + dx + ex, :3] = blad
    a[cy:cy + 2, cx:cx + 2, :3] = hart


def textures(h):
    def bewaar(a, naam):
        h.save(Image.fromarray(a, "RGBA"), "block", f"{naam}.png")

    # rope: twisted tan strands
    a = _ruis("touw", (206, 176, 128), 4)
    for y in range(16):
        for x in range(16):
            if (x + y * 2) % 4 == 0:
                a[y, x, :3] = (168, 136, 92)
    bewaar(a, "botenhuisje_touw")
    # the lantern: paper in a pale frame with a blossom on it; lit: a warm glow from the middle
    hout = np.asarray(Image.open(os.path.join(h.TEX, "block", "bleekhout_gestript.png")).convert("RGBA"))
    for naam, papier, gloed in (("botenhuisje_steigerlantaarn", (240, 234, 224), 0.0), ("botenhuisje_steigerlantaarn_aan", (255, 240, 196), 1.0)):
        a = _ruis(naam, papier, 3)
        if gloed:
            for y in range(16):
                for x in range(16):
                    d = ((x - 7.5) ** 2 + (y - 8.5) ** 2) ** 0.5 / 9.0
                    a[y, x, :3] = np.clip(np.array((255, 250, 226)) * (1 - d) + np.array((255, 214, 150)) * d, 0, 255)
        _bloesem(a, 7, 8, blad=(240, 140, 182) if gloed else (246, 164, 198))
        for i in range(16):
            for rand in (0, 15):
                a[i, rand, :3] = hout[i, 3, :3]
                a[rand, i, :3] = hout[3, i, :3]
            a[i, 5, :3] = np.clip(a[i, 5, :3].astype(int) - 14, 0, 255)
            a[i, 10, :3] = np.clip(a[i, 10, :3].astype(int) - 14, 0, 255)
        bewaar(a, naam)
    # the rod: bamboo with its knots
    a = _ruis("hengel", (214, 196, 132), 4)
    for y in (3, 9, 14):
        a[y, :, :3] = (170, 150, 92)
    bewaar(a, "botenhuisje_hengel")
    # the windsock: a white koi with red-orange patches, a round eye near the mouth (the mouth is at the left)
    a = _ruis("koiwindzak", (250, 246, 238), 3)
    r = lib.rng("koiwindzak_vlekken")
    for _ in range(5):
        cx, cy, rr = int(r.integers(4, 15)), int(r.integers(1, 15)), float(r.uniform(2.0, 3.6))
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= rr * rr:
                    a[y, x, :3] = (236, 96, 62) if (x + y) % 5 else (246, 132, 84)
    for y in range(16):                                         # scales: little arcs
        for x in range(4, 16):
            if (x % 4 == (2 if (y // 3) % 2 else 0)) and y % 3 == 1:
                a[y, x, :3] = np.clip(a[y, x, :3].astype(int) - 26, 0, 255)
    a[:, 0:2, :3] = (246, 150, 190)                             # the pink rim of the mouth
    a[5:8, 2:5, :3] = (255, 255, 255)
    a[6:8, 3:5, :3] = (40, 30, 50)
    bewaar(a, "botenhuisje_koiwindzak")
    # the basket: wicker, and its gingham cloth
    a = _ruis("mand", (204, 164, 104), 5)
    for y in range(16):
        for x in range(16):
            blok = ((x // 2) + (y // 2)) % 2
            if blok and y % 2 == 0:
                a[y, x, :3] = (168, 128, 76)
            elif not blok and x % 2 == 0:
                a[y, x, :3] = (178, 138, 84)
    a[0, :, :3] = (150, 112, 66)
    a[15, :, :3] = (150, 112, 66)
    bewaar(a, "picknickeilandje_mand")
    a = _ruis("mand_doek", (255, 250, 250), 2)
    for y in range(16):
        for x in range(16):
            rx, ry = (x // 2) % 2, (y // 2) % 2
            if rx and ry:
                a[y, x, :3] = (236, 120, 166)
            elif rx or ry:
                a[y, x, :3] = (250, 186, 212)
    bewaar(a, "picknickeilandje_mand_doek")


# =====================================================================================================================
# block models
# =====================================================================================================================
def _el(frm, to, tex, rot=None, faces=None, **extra):
    e = {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in (faces or ("down", "up", "north", "south", "west", "east"))}}
    if rot:
        e["rotation"] = rot
    e.update(extra)
    return e


def modellen(h):
    A, w = h.A, h.w
    plank, paal, paal_top = "guhs:block/bleekhout_planken", "guhs:block/bleekhout_gestript", "guhs:block/bleekhout_gestript_top"
    roze = "guhs:block/guh_dakpan_roze" if lib.bestaat(h, "guh_dakpan_roze") else "minecraft:block/pink_wool"

    # de meerpaal: a piece of deck with the post through it, a rope round it and a pink cap
    post = _el([5, 0, 5], [11, 26, 11], "#paal")
    post["faces"]["up"]["texture"] = post["faces"]["down"]["texture"] = "#paal_top"
    meerpaal = [_el([0, 8, 0], [16, 16, 16], "#plank"), post,
                _el([4, 18, 4], [12, 21, 12], "#touw"), _el([4.5, 26, 4.5], [11.5, 28, 11.5], "#roze"),
                _el([11, 16.5, 7], [16, 17.5, 9], "#touw")]
    w(f"{A}/models/block/botenhuisje_meerpaal.json", {"parent": "minecraft:block/block", "textures": {
        "plank": plank, "paal": paal, "paal_top": paal_top, "touw": "guhs:block/botenhuisje_touw", "roze": roze, "particle": plank}, "elements": meerpaal})
    w(f"{A}/blockstates/botenhuisje_meerpaal.json", {"variants": h.facing_states("botenhuisje_meerpaal")})

    # de steigerlantaarn: a foot, a paper body, a pink cap with a knob
    def lantaarn(aan):
        body = _el([4, 5, 4], [12, 13, 12], "#papier")
        body["faces"]["up"]["texture"] = body["faces"]["down"]["texture"] = "#paal_top"
        if aan:
            body["shade"] = False
            body["light_emission"] = 15
        return [_el([6, 0, 6], [10, 5, 10], "#paal"), body, _el([3, 13, 3], [13, 15, 13], "#roze"), _el([6, 15, 6], [10, 16, 10], "#roze")]
    for staat, tex in (("", "botenhuisje_steigerlantaarn"), ("_aan", "botenhuisje_steigerlantaarn_aan")):
        w(f"{A}/models/block/botenhuisje_steigerlantaarn{staat}.json", {"parent": "minecraft:block/block", "textures": {
            "papier": f"guhs:block/{tex}", "paal": paal, "paal_top": paal_top, "roze": roze, "particle": f"guhs:block/{tex}"},
            "elements": lantaarn(bool(staat))})
    w(f"{A}/blockstates/botenhuisje_steigerlantaarn.json", {"variants": {
        "lit=false": {"model": "guhs:block/botenhuisje_steigerlantaarn"}, "lit=true": {"model": "guhs:block/botenhuisje_steigerlantaarn_aan"}}})
    w(f"{A}/models/item/botenhuisje_steigerlantaarn.json", {"parent": "guhs:block/botenhuisje_steigerlantaarn_aan"})

    # de hengel op een standaard: a foot, a holder, the rod leaning north, its line and a petal
    schuin = {"origin": [8, 3, 8], "axis": "x", "angle": -22.5}
    hengel = [_el([4, 0, 5], [12, 2, 11], "#plank"), _el([6, 2, 6], [10, 4, 10], "#roze"),
              _el([7.5, 3, 7.5], [8.5, 30, 8.5], "#hengel", rot=schuin),
              _el([7.8, 13, -2.6], [8.2, 27.5, -2.2], "#lijn"), _el([7, 12, -3.4], [9, 13, -1.4], "#blaadje")]
    w(f"{A}/models/block/botenhuisje_hengelstandaard.json", {"parent": "minecraft:block/block", "textures": {
        "plank": plank, "roze": roze, "hengel": "guhs:block/botenhuisje_hengel", "lijn": "minecraft:block/white_wool", "blaadje": "minecraft:block/pink_wool",
        "particle": plank}, "elements": hengel, "display": {
            "gui": {"rotation": [20, 60, 0], "translation": [1, -3, 0], "scale": [0.5, 0.5, 0.5]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
            "fixed": {"rotation": [0, 90, 0], "translation": [0, -3, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [60, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.35, 0.35, 0.35]}}})
    w(f"{A}/blockstates/botenhuisje_hengelstandaard.json", {"variants": h.facing_states("botenhuisje_hengelstandaard")})
    w(f"{A}/models/item/botenhuisje_hengelstandaard.json", {"parent": "guhs:block/botenhuisje_hengelstandaard"})

    # de koi-windzak: a pole, the koi blowing out to the south (its mouth at the pole), a tail
    def zak(frm, to, u0, u1):
        e = _el(frm, to, "#koi")
        for f in ("west", "east", "up", "down"):
            e["faces"][f]["uv"] = [u0, 0, u1, 16] if f in ("west", "east") else [u0, 0, u1, 16]
        e["faces"]["east"]["uv"] = [u1, 0, u0, 16]
        e["faces"]["north"]["uv"] = [0, 0, 2, 16]
        e["faces"]["south"]["uv"] = [12, 0, 16, 16]
        return e
    koi = [_el([7, 0, 7], [9, 16, 9], "#paal"),
           zak([5, 9, 9], [11, 15, 16], 0, 8), zak([5.5, 9.5, 16], [10.5, 14.5, 21], 8, 13), zak([6.5, 10.5, 21], [9.5, 13.5, 24], 13, 16),
           _el([7.5, 8.5, 24], [8.5, 15.5, 26], "#koi")]
    w(f"{A}/models/block/botenhuisje_koiwindzak.json", {"parent": "minecraft:block/block", "textures": {
        "paal": paal, "koi": "guhs:block/botenhuisje_koiwindzak", "particle": "guhs:block/botenhuisje_koiwindzak"}, "elements": koi, "display": {
            "gui": {"rotation": [20, 120, 0], "translation": [2, -1, 0], "scale": [0.55, 0.55, 0.55]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
            "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [60, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
            "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.35, 0.35, 0.35]}}})
    w(f"{A}/blockstates/botenhuisje_koiwindzak.json", {"variants": h.facing_states("botenhuisje_koiwindzak")})
    w(f"{A}/models/item/botenhuisje_koiwindzak.json", {"parent": "guhs:block/botenhuisje_koiwindzak"})

    # de picknickmand: wicker, a gingham cloth, a handle, a koek and a bottle peeking out
    mand = [_el([2, 0, 3], [14, 7, 13], "#mand"), _el([1.5, 7, 2.5], [14.5, 8, 13.5], "#doek"),
            _el([7, 7, 3], [9, 13, 4], "#mand"), _el([7, 7, 12], [9, 13, 13], "#mand"), _el([7, 12, 4], [9, 13, 12], "#mand"),
            _el([3.5, 8, 5], [6.5, 9.5, 8], "#koek"), _el([10.5, 8, 9], [12.5, 12, 11], "#fles"), _el([11, 12, 9.5], [12, 13.5, 10.5], "#mand")]
    w(f"{A}/models/block/picknickeilandje_mand.json", {"parent": "minecraft:block/block", "textures": {
        "mand": "guhs:block/picknickeilandje_mand", "doek": "guhs:block/picknickeilandje_mand_doek", "koek": "minecraft:block/pink_wool",
        "fles": "minecraft:block/white_stained_glass", "particle": "guhs:block/picknickeilandje_mand"}, "elements": mand})
    w(f"{A}/blockstates/picknickeilandje_mand.json", {"variants": {
        k.replace("facing=", "facing=") + f",versierd={v}": m for v in ("false", "true") for k, m in h.facing_states("picknickeilandje_mand").items()}})

    for b in MET_ITEM:
        h.self_drop(b)


# =====================================================================================================================
# the roeibootje: vanilla's boat picture in pale wood with a blossom-pink rim and pink paddle blades
# =====================================================================================================================
def roeibootje(h):
    import mc26
    bron = np.asarray(mc26.vanilla_121("entity/boat/cherry").convert("RGBA")).astype(np.float32)
    grijs = bron[..., :3].mean(axis=2, keepdims=True)
    lo, hi = grijs[bron[..., 3] > 0].min(), grijs[bron[..., 3] > 0].max()
    t = (grijs - lo) / max(1.0, hi - lo)
    hout = np.array((176, 160, 132), np.float32) * (1 - t) + np.array((246, 238, 220), np.float32) * t
    roze = np.array((214, 110, 156), np.float32) * (1 - t) + np.array((255, 190, 216), np.float32) * t
    uit = np.concatenate([hout, bron[..., 3:]], axis=2)
    # the rim of the four side boards: their top face and the top two rows of their sides (uv rows 19, 27, 35, 43 are
    # where the boards start; the paddles share one piece of the picture for shaft and blade, so they stay plain wood)
    for y0 in (19, 27, 35, 43):
        uit[y0:y0 + 4, 0:62, :3] = roze[y0:y0 + 4, 0:62]
    h.save(Image.fromarray(np.clip(uit, 0, 255).astype(np.uint8), "RGBA"), "entity", "boat", "roeibootje.png")


# =====================================================================================================================
# the characters
# =====================================================================================================================
GEZICHT = (440, 128, 496, 172)       # the face on the 512 x 512 sitting guh picture (x0, y0, x1, y1)


def _ogen_dicht(a):
    """Closes the eyes of a sitting guh picture: the eyes become fur, with a little sleeping curve each."""
    x0, y0, x1, y1 = GEZICHT
    vlak = a[y0:y1, x0:x1]
    rgb = vlak[..., :3].astype(int)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    oog = ((b > r + 12) | (r + g + b < 330) | ((r > 244) & (g > 244) & (b > 244))) & (np.arange(y1 - y0)[:, None] < (y1 - y0) * 0.62)
    vacht = np.median(rgb[~oog & (vlak[..., 3] > 0)], axis=0)
    kolommen = np.where(oog.any(axis=0))[0]
    if len(kolommen) == 0:
        raise SystemExit("bio_bouw_meer: no eyes found on guh_sitting.png")
    groepen, begin = [], kolommen[0]
    for i in range(1, len(kolommen) + 1):
        if i == len(kolommen) or kolommen[i] > kolommen[i - 1] + 3:
            groepen.append((begin, kolommen[i - 1]))
            begin = kolommen[i] if i < len(kolommen) else None
    for (c0, c1) in groepen:
        rijen = np.where(oog[:, c0:c1 + 1].any(axis=1))[0]
        r0, r1 = rijen[0], rijen[-1]
        vlak[max(0, r0 - 1):r1 + 2, max(0, c0 - 1):c1 + 2, :3] = vacht
        cx, breed, yc = (c0 + c1) / 2.0, (c1 - c0) / 2.0, int(r0 + (r1 - r0) * 0.62)
        for x in range(c0, c1 + 1):
            t = (x - cx) / max(1.0, breed)
            y = yc + 3 - int(round(5 * t * t))
            vlak[y:y + 3, x, :3] = (92, 54, 84)
    return a


def karakters(h):
    # the hanami guhs: the plain sitting guh; the bloesemguh: blossom-white with petals in its fur; both also asleep
    gewoon = np.asarray(Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")).copy()
    bloesem = hulp.sitting_texture(h, hue=0.93, sat=0.42, val=1.05)
    r = lib.rng("hanami_bloesem")
    vacht = np.argwhere(bloesem[..., 3] > 0)
    x0, y0, x1, y1 = GEZICHT
    for _ in range(120):
        y, x = vacht[int(r.integers(0, len(vacht)))]
        if x0 - 4 <= x <= x1 + 4 and y0 - 4 <= y <= y1 + 4:
            continue                                            # (no petals over the face)
        kleur = ((250, 168, 204), (252, 188, 216), (244, 150, 192))[int(r.integers(0, 3))]
        bloesem[y:y + 3, x:x + 3, :3] = kleur
        bloesem[y + 1:y + 4, x + 2:x + 4, :3] = kleur
    for naam, vel in (("hanami_guh", gewoon), ("hanami_bloesemguh", bloesem)):
        h.save(Image.fromarray(vel, "RGBA"), "entity", f"npc_{naam}.png")
        h.save(Image.fromarray(_ogen_dicht(vel.copy()), "RGBA"), "entity", f"npc_{naam}_slaapt.png")

    # the visser-guh: warm apricot fur (the colour of a koi), a wide straw hat with a pink band, a rod with a petal on its line
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_botenhuisje_visserguh")
    sw = hulp.swatches(geo, ["stro", "band", "hengel", "lijn", "blaadje"])
    c = hulp.cube
    geo["bones"].append({"name": "visser_hoed", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-11.5, 25.6, -11.5], [23, 0.8, 21], sw["stro"]),
        c([-6.5, 26.4, -7.0], [13, 1.6, 12], sw["stro"]),
        c([-6.6, 26.4, -7.1], [13.2, 0.7, 12.2], sw["band"]),
        c([-4.5, 28.0, -5.0], [9, 1.4, 8], sw["stro"]),
        c([-2.5, 29.4, -3.0], [5, 1.0, 4], sw["stro"])]})
    geo["bones"].append({"name": "visser_hengel", "parent": "body", "pivot": [-3.2, 9, -6], "cubes": [
        c([-3.6, 8.6, -24], [0.8, 0.8, 19], sw["hengel"], rotation=[-25, 0, 0], pivot=[-3.2, 9, -6]),
        c([-3.4, -17.0, -22.6], [0.4, 33.6, 0.4], sw["lijn"]),
        c([-4.2, -17.6, -23.4], [2.0, 0.6, 2.0], sw["blaadje"])]})
    hulp.save_geo(h, "guh_npc_botenhuisje_visserguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.075, sat=1.7, val=1.0)
    r = lib.rng("visserguh")

    def vlechtwerk(block):
        for y in range(0, block.shape[0], 4):
            block[y, :, :3] = np.clip(block[y, :, :3].astype(int) - 26, 0, 255)
    hulp.paint_swatch(a, sw["stro"], (232, 208, 140), r, 8, vlechtwerk)
    hulp.paint_swatch(a, sw["band"], (240, 140, 184), r, 6)
    hulp.paint_swatch(a, sw["hengel"], (206, 184, 120), r, 8)
    hulp.paint_swatch(a, sw["lijn"], (250, 250, 250), r, 2)
    hulp.paint_swatch(a, sw["blaadje"], (248, 168, 204), r, 6)
    h.save(Image.fromarray(a, "RGBA"), "entity", "npc_botenhuisje_visserguh.png")


# =====================================================================================================================
# data: structures, loot, recipes, advancements
# =====================================================================================================================
def data(h):
    D, w = h.D, h.w
    for naam, (biomes, spacing, separation, salt, soort) in SETS.items():
        h.structure(naam, biomes, spacing=spacing, separation=separation, salt=salt, reach=24, centre=f"guhs:{naam}_midden")
        bio_plek.plek(h, naam, soort)
    # The jetty's posts and the lake's water: the model is the terrain, so a post never takes water away. Where a post of
    # the template (a bleekhout stam) comes to stand in water it becomes a waterlogged bleekhout hek post: the water of
    # that column stays source water. In the lake floor, in the shore and in the air it stays the log it was.
    w(f"{D}/worldgen/processor_list/{PALEN}.json", {"processors": [{"processor_type": "minecraft:rule", "rules": [{
        "input_predicate": {"predicate_type": "minecraft:block_match", "block": "guhs:bleekhout_stam"},
        "location_predicate": {"predicate_type": "minecraft:block_match", "block": "minecraft:water"},
        "output_state": {"Name": "guhs:bleekhout_hek", "Properties": {
            "east": "false", "north": "false", "south": "false", "west": "false", "waterlogged": "true"}}}]}]})

    def met_palen(pool):
        for e in pool["elements"]:
            e["element"]["processors"] = f"guhs:{PALEN}"
    h.patch_json(f"{D}/worldgen/template_pool/botenhuisje/start.json", met_palen)
    for naam, crit in BEWIJZEN.items():
        w(f"{D}/advancement/quest/{naam}.json", {"criteria": crit})
    tel = h.count_fn
    w(f"{D}/loot_table/chests/picknickeilandje.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:roze_guh_koek", "functions": tel(2, 3)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": tel(3, 5)}]},
        {"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": "guhs:lampion_roze", "weight": 3},
            {"type": "minecraft:item", "name": "guhs:drijvende_bloesemblaadjes", "weight": 3, "functions": tel(3, 5)},
            {"type": "minecraft:item", "name": "guhs:koivoer", "weight": 3, "functions": tel(3, 5)},
            {"type": "minecraft:item", "name": "guhs:guh_taart", "weight": 1}]}]})
    h.shaped("botenhuisje_steigerlantaarn", ["R", "T", "H"], {"R": "minecraft:pink_carpet", "T": "minecraft:torch", "H": "guhs:bleekhout_hek"},
             "guhs:botenhuisje_steigerlantaarn", 2)
    h.shapeless("botenhuisje_hengelstandaard", ["minecraft:fishing_rod", "guhs:bleekhout_plaat"], "guhs:botenhuisje_hengelstandaard", 1)
    h.shapeless("botenhuisje_koiwindzak", ["minecraft:stick", "minecraft:white_wool", "minecraft:orange_dye"], "guhs:botenhuisje_koiwindzak", 1)
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{b}" for b in MET_ITEM])


# =====================================================================================================================
def build(h):
    textures(h)
    modellen(h)
    roeibootje(h)
    karakters(h)
    data(h)
    bouw.build(h)
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    A, D = h.A, h.D
    mis = list(bouw.check(h))
    for b in BLOKKEN:
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/block/{b}.json"):
            if not os.path.exists(p):
                mis.append(p)
        if f"block.guhs.{b}" not in TEXTS:
            mis.append(f"lang block.guhs.{b}")
        if os.path.exists(f"{A}/models/block/{b}.json"):
            for t in json.load(open(f"{A}/models/block/{b}.json", encoding="utf-8"))["textures"].values():
                t = t["sprite"] if isinstance(t, dict) else t
                if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                    mis.append(f"texture {t} ({b})")
    for b in MET_ITEM:
        for p in (f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json", f"{D}/recipe/{b}.json"):
            if not os.path.exists(p):
                mis.append(p)
        if f"block.guhs.{b}.lore" not in TEXTS:
            mis.append(f"lang block.guhs.{b}.lore")
    for n in NPCS:
        if not os.path.exists(f"{A}/textures/entity/npc_{n}.png"):
            mis.append(f"skin npc_{n}.png")
        if f"entity.guhs.guh_npc.{n}" not in TEXTS:
            mis.append(f"lang entity.guhs.guh_npc.{n}")
    for s in STRUCTUREN:
        for p in (f"{D}/structure/{s}.nbt", f"{D}/worldgen/structure/{s}.json", f"{D}/worldgen/structure_set/{s}.json"):
            if not os.path.exists(p):
                mis.append(p)
        pool = f"{D}/worldgen/template_pool/{s}/start.json"
        wil = f"guhs:{PALEN}" if s == "botenhuisje" else "minecraft:empty"
        if not os.path.exists(pool) or any(e["element"]["processors"] != wil for e in json.load(open(pool, encoding="utf-8"))["elements"]):
            mis.append(f"start pool of {s}: processors are not {wil}")
        if os.path.exists(f"{D}/worldgen/structure/{s}.json"):
            st = json.load(open(f"{D}/worldgen/structure/{s}.json", encoding="utf-8"))
            if st.get("type") != "guhs:bio_plek" or st.get("plek") != SETS[s][4]:
                mis.append(f"structure {s} is not a guhs:bio_plek of kind {SETS[s][4]}")
        for k in (f"structure.guhs.{s}", f"structure.guhs.{s}.tooltip"):
            if k not in TEXTS:
                mis.append(f"lang {k}")
    for naam in BEWIJZEN:
        if not os.path.exists(f"{D}/advancement/quest/{naam}.json"):
            mis.append(f"advancement quest/{naam}")
    # every text a Java class asks for by number exists (the counts in Visserguh and Hanami)
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bio", "bouwmeer", "Visserguh.java"), encoding="utf-8").read()
    if f"GROETEN = {len(GROET['ochtend'])}, GEDACHTEN = {len(GEDACHTEN)}" not in java or any(len(v) != len(GROET["ochtend"]) for v in GROET.values()):
        mis.append("Visserguh.GROETEN / GEDACHTEN do not match the texts")
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bio", "bouwmeer", "Hanami.java"), encoding="utf-8").read()
    if f"LIEF = {len(LIEF)}, GEWOON = {len(GEWOON)}, SLAAP = {len(SLAAP)}" not in java:
        mis.append("Hanami.LIEF / GEWOON / SLAAP do not match the texts")
    if mis:
        raise SystemExit("bio_bouw_meer self-check failed:\n  " + "\n  ".join(mis))
