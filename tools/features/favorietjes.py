"""
De geheime favorietjes van elke guh (2.10 "Lieve vadsjes van elkaar", slice favorietjes; see guhs_work210/CONTRACT_210.md).

  - particles favorietjes_explosie (the big heart explosion: a client emitter, no texture), favorietjes_glinster (sparkles),
    favorietjes_vraagje (a pink question mark: a warm hint), favorietjes_snuffel (sniff puffs: a cold hint)
  - sounds favorietjes.ontdekt, favorietjes.snuffel, favorietjes.nieuwsgierig
  - data/guhs/favorietjes/favorietjes.json: the clothes -> colour table (from the clothes textures themselves, the name
    wins where it says a colour) and how many texts every wist-je-datje has
  - advancements lieve_vadsjes/favorietjes_* (the first one, one per kind, all eight) + the hidden quest/favorietjes_juichen
  - texts: warm/cold hints, discoveries, the eerste keren (favorietjes_*) and lots of wist-je-datjes for moments all over
    the mod (gui.guhs.wistjedat.favorietjes.*)
  - the FTB quests of the section "Favorietjes" and the game test room
"""
import colorsys
import os
import random
import re
from collections import Counter

from PIL import Image

BONES = {}
CLOTHES = []

KINDS = ["eten", "plek", "knuffel", "liedje", "speeltje", "emote", "kleur", "vriend"]
KLEUREN = ["roze", "rood", "oranje", "geel", "groen", "mint", "blauw", "paars", "wit", "zwart", "bruin", "goud"]
HINT_VARIANTEN = 3   # = Favorietjes.HINT_VARIANTEN

# =====================================================================================================================
# particles
# =====================================================================================================================
STER = ["...X...",
        "...X...",
        "..XHX..",
        "XXHHHXX",
        "..XHX..",
        "...X...",
        "...X..."]
VRAAG = ["..XXXX..",
         ".XHHHHX.",
         "XHHXXHHX",
         ".XX.XHHX",
         "...XHHX.",
         "..XHHX..",
         "..XHHX..",
         "...XX...",
         "..XHHX..",
         "..XHHX..",
         "...XX..."]


def _pixels(rows, kleur, rand, size=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    h, w = len(rows), len(rows[0])
    x0, y0 = (size - w) // 2, (size - h) // 2
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == "H":
                img.putpixel((x0 + x, y0 + y), kleur + (255,))
            elif c == "X":
                img.putpixel((x0 + x, y0 + y), rand + (255,))
    return img


def _puf(r, seed):
    """A soft little cloud of three round puffs."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(seed)
    for (cx, cy, rr) in [(8, 8, r), (8 + rng.choice((-2, 2)), 7, r - 1), (8 + rng.choice((-1, 1)), 9, r - 1)]:
        for y in range(16):
            for x in range(16):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d <= rr:
                    a = 230 if d < rr - 1 else 150
                    old = img.getpixel((x, y))
                    img.putpixel((x, y), (250, 244, 248, max(a, old[3])))
    return img


def particles(h):
    for i, (kleur, rand) in enumerate([((255, 238, 150), (230, 170, 40)), ((255, 210, 236), (236, 110, 170)),
                                       ((255, 255, 255), (200, 200, 230))]):
        h.save(_pixels(STER, kleur, rand), "particle", f"favorietjes_glinster_{i}.png")
    h.w(f"{h.A}/particles/favorietjes_glinster.json", {"textures": [f"guhs:favorietjes_glinster_{i}" for i in range(3)]})
    h.save(_pixels(VRAAG, (255, 150, 200), (160, 50, 110)), "particle", "favorietjes_vraagje.png")
    h.w(f"{h.A}/particles/favorietjes_vraagje.json", {"textures": ["guhs:favorietjes_vraagje"]})
    for i, r in enumerate((3, 4)):
        h.save(_puf(r, i), "particle", f"favorietjes_snuffel_{i}.png")
    h.w(f"{h.A}/particles/favorietjes_snuffel.json", {"textures": [f"guhs:favorietjes_snuffel_{i}" for i in range(2)]})
    # the explosion is an emitter (it throws band hearts and sparkles): no sprites of its own
    h.w(f"{h.A}/particles/favorietjes_explosie.json", {"textures": []})


# =====================================================================================================================
# sounds
# =====================================================================================================================
SOUNDS = {
    "favorietjes.ontdekt": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 1.8, "volume": 0.9},
                            {"name": "minecraft:block.amethyst_block.resonate", "type": "event", "pitch": 1.6, "volume": 1.0}],
    "favorietjes.snuffel": [{"name": "minecraft:entity.fox.sniff", "type": "event", "pitch": 1.5, "volume": 0.9},
                            {"name": "minecraft:entity.fox.sniff", "type": "event", "pitch": 1.7, "volume": 0.9}],
    "favorietjes.nieuwsgierig": [{"name": "minecraft:entity.allay.ambient_without_item", "type": "event", "pitch": 1.5, "volume": 0.6},
                                 {"name": "minecraft:entity.fox.sniff", "type": "event", "pitch": 1.9, "volume": 0.8}],
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# the clothes -> colour table
# =====================================================================================================================
# a colour word in the id wins (as a whole word part: "gouden_hartjeshalsbandje", "red_bowtie", "oorstrikje_mint")
NAAM_KLEUR = {
    "pink": "roze", "roze": "roze", "red": "rood", "rood": "rood", "rode": "rood", "orange": "oranje", "oranje": "oranje",
    "yellow": "geel", "geel": "geel", "gele": "geel", "green": "groen", "groen": "groen", "groene": "groen", "mint": "mint",
    "blue": "blauw", "blauw": "blauw", "blauwe": "blauw", "purple": "paars", "paars": "paars", "paarse": "paars",
    "white": "wit", "wit": "wit", "witte": "wit", "black": "zwart", "zwart": "zwart", "zwarte": "zwart",
    "brown": "bruin", "bruin": "bruin", "bruine": "bruin", "gold": "goud", "golden": "goud", "goud": "goud", "gouden": "goud",
}
# pieces whose colour you'd name differently from their biggest colour area (crowns, medals and chains are gold...)
HAND_KLEUR = {
    "royal_crown": "goud", "koning_kroon": "goud", "koning_ketting": "goud", "mikamepper_medaille": "goud",
    "showster_tiara": "goud", "burgemeesterssjerp": "goud", "monocle": "goud", "stethoscope": "zwart",
    "knight_helmet": "wit", "knight_armour": "wit", "orange_crown": "oranje",
}


def _bucket(r, g, b):
    hh, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    hh *= 360
    if v < 0.2:
        return "zwart"
    if s < 0.16:
        if v > 0.78:
            return "wit"
        return "zwart" if v < 0.4 else None
    if 15 <= hh < 45 and v < 0.62:
        return "bruin"
    if hh < 12 or hh >= 345:
        return "roze" if (s < 0.5 and v > 0.75) else "rood"
    if hh >= 290:
        return "roze"
    if hh < 38:
        return "oranje"
    if hh < 66:
        return "geel"
    if hh < 150:
        return "mint" if (hh >= 100 and v >= 0.75 and s < 0.45) else "groen"
    if hh < 185:
        return "mint"
    if hh < 250:
        return "mint" if (hh < 200 and s < 0.5 and v > 0.9) else "blauw"
    return "paars"


def clothes_ids(h):
    """(id, slot) of every GuhClothes piece, read from the enum itself."""
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    return [(n.lower(), slot) for n, slot in re.findall(r"^    ([A-Z0-9_]+)\(Slot\.([A-Z]+)", src, re.M)]


def kleur_van(h, cid):
    if cid in HAND_KLEUR:
        return HAND_KLEUR[cid]
    for deel in cid.split("_"):
        if deel in NAAM_KLEUR:
            return NAAM_KLEUR[deel]
    path = os.path.join(h.TEX, "entity", "guh_clothes", f"{cid}.png")
    if not os.path.exists(path):
        return None
    c = Counter()
    for px in Image.open(path).convert("RGBA").getdata():
        if px[3] > 0:
            b = _bucket(*px[:3])
            if b:
                c[b] += 1
    return c.most_common(1)[0][0] if c else None


def kleuren(h):
    out = {}
    for cid, slot in clothes_ids(h):
        if slot == "HAAR":
            continue   # (hairstyles are tinted by the kapper: no colour of their own)
        k = kleur_van(h, cid)
        if k:
            out[cid] = k
    return out


# =====================================================================================================================
# advancements
# =====================================================================================================================
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
QUEST_ADVANCEMENTS = ["favorietjes_juichen"]
ADV_ICON = {"eten": "guhs:guh_cupcake", "plek": "minecraft:cherry_sapling", "knuffel": "minecraft:pink_wool",
            "liedje": "minecraft:note_block", "speeltje": "minecraft:slime_ball", "emote": "minecraft:cake",
            "kleur": "minecraft:pink_dye", "vriend": "minecraft:poppy"}
ADV_TEKST = {
    "eten": ("Lievelingshapje!", "Ontdek het lievelingshapje van een guh (voer hem allerlei snackjes)"),
    "plek": ("Lievelingsplekje!", "Ontdek het lievelingsplekje van een guh (neem hem mee op reis)"),
    "knuffel": ("Lievelingsknuffel!", "Ontdek de lievelingsknuffel van een guh (zet knuffels bij hem neer)"),
    "liedje": ("Lievelingsliedje!", "Ontdek het lievelingsliedje van een guh (xylofoon of disco)"),
    "speeltje": ("Lievelingsspeeltje!", "Ontdek het lievelingsspeeltje van een guh"),
    "emote": ("Lievelingskunstje!", "Ontdek de lievelingsemote van een guh"),
    "kleur": ("Lievelingskleur!", "Ontdek de lievelingskleur van een guh (trek hem kleertjes aan)"),
    "vriend": ("Allerbeste guh-vriendje!", "Ontdek wie het allerliefste guh-vriendje van een guh is"),
}


def visible(h, name, parent, icon, frame, title, desc, hidden=False):
    adv = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.lieve_vadsjes.{name}.title"},
                       "description": {"translate": f"advancements.guhs.lieve_vadsjes.{name}.description"},
                       "frame": frame, "show_toast": True, "announce_to_chat": frame != "task", "hidden": hidden},
           "criteria": IMPOSSIBLE, "parent": f"guhs:lieve_vadsjes/{parent}"}
    h.w(f"{h.D}/advancement/lieve_vadsjes/{name}.json", adv)
    h.lang(f"advancements.guhs.lieve_vadsjes.{name}.title", title, title)
    h.lang(f"advancements.guhs.lieve_vadsjes.{name}.description", desc, desc)


def advancements(h):
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    visible(h, "favorietjes_eerste", "root", "minecraft:gold_nugget", "task", "Een geheimpje!",
            "Ontdek een favorietje van een guh. Hij laat het merken met een hartjesexplosie!")
    for k in KINDS:
        titel, desc = ADV_TEKST[k]
        visible(h, f"favorietjes_{k}", "favorietjes_eerste", ADV_ICON[k], "task", titel, desc)
    visible(h, "favorietjes_alle", "favorietjes_eerste", "minecraft:nether_star", "challenge", "Ik ken al je geheimpjes",
            "Ontdek alle acht favorietjes van één guh. VAHOEG!")


# =====================================================================================================================
# texts
# =====================================================================================================================
HINTS = {  # kind: (warm x3, koud x3); %s = the guh
    "eten": (["%s kijkt nieuwsgierig naar het hapje... bijna! Iets wat hierop lijkt is zijn allerliefste, njeg!",
              "%s spitst zijn oortjes: mmm, dit smaakt BIJNA als zijn lievelingshapje. Warm, warm!",
              "%s likt zijn snoet af en kijkt je hoopvol aan. Zoiets, maar dan nét anders... njeg?"],
             ["%s snuffelt... smakt... njeg? Lekker hoor, maar niet zijn allerliefste hapje.",
              "%s kauwt beleefd. Het is lief bedoeld, maar zijn lievelingshapje is iets heel anders. Koud!",
              "%s snuffelt eraan en kijkt een beetje scheel. Njeg? Probeer eens iets heel anders!"]),
    "plek": (["%s kijkt nieuwsgierig om zich heen... het ruikt hier bijna naar zijn lievelingsplekje!",
              "%s trippelt opgewonden rondjes. Dit lijkt op zijn lievelingsplekje, maar het is 'm nét niet. Warm!",
              "%s snuift de lucht op: zijn lievelingsplekje lijkt op dit plekje, njeg!"],
             ["%s snuffelt aan de grond... njeg? Leuk hier, maar zijn lievelingsplekje is heel ergens anders.",
              "%s gaapt eens. Mooi uitzicht, maar niet om van te wiebelen. Koud, koud!",
              "%s kijkt rond en haalt zijn schoudertjes op. Verder zoeken, njeg!"]),
    "knuffel": (["%s knuffelt de knuffel en kijkt nieuwsgierig op... zo'n soort knuffel is bijna zijn lievelingsknuffel!",
                 "%s drukt zijn snoet in de knuffel: warm! Zijn allerliefste knuffel lijkt hier een beetje op.",
                 "%s giechelt zachtjes. Bijna, bijna! Zijn lievelingsknuffel is een neefje van deze, njeg."],
                ["%s snuffelt aan de knuffel... njeg? Zacht, maar niet zijn allerliefste.",
                 "%s geeft de knuffel een beleefd klopje. Koud! Zijn lievelingsknuffel is heel anders.",
                 "%s kijkt de knuffel aan, de knuffel kijkt terug. Geen vonk. Njeg."]),
    "liedje": (["%s wiebelt nieuwsgierig mee... zo'n soort liedje is bijna zijn lievelingsliedje!",
                "%s neuriet een paar noten mee en stopt dan. Warm! Zijn liedje klinkt een beetje zo.",
                "%s zwaait met zijn oortjes op de maat: bijna! Probeer een ander liedje van dezelfde soort."],
               ["%s luistert beleefd... njeg? Leuk deuntje, maar niet zijn lievelingsliedje.",
                "%s tikt met één pootje mee en valt dan bijna in slaap. Koud!",
                "%s snuffelt aan de muziek (dat kan dus). Njeg? Een heel ander soort liedje misschien!"]),
    "speeltje": (["%s speelt nieuwsgierig... dit soort spelen is bijna zijn lievelingsspeeltje, njeg!",
                  "%s rent enthousiast rondjes. Warm! Met zijn allerliefste speeltje speel je op net zo'n manier.",
                  "%s piept vrolijk: bijna! Probeer een speeltje waar je net zo mee speelt."],
                 ["%s snuffelt aan het speeltje... njeg? Leuk, maar niet zijn lievelingsspeeltje.",
                  "%s speelt even mee en gaat dan lekker zitten. Koud!",
                  "%s kijkt je aan alsof hij wil zeggen: ander speeltje graag, njeg."]),
    "emote": (["%s kijkt nieuwsgierig op na zijn kunstje... zo'n soort kunstje is bijna zijn lievelingskunstje!",
               "%s straalt een beetje. Warm! Zijn allerliefste kunstje heeft precies dezelfde stemming.",
               "%s wiebelt nog even na: bijna, njeg! Probeer een kunstje dat net zo voelt."],
              ["%s snuffelt eens... njeg? Leuk kunstje, maar niet zijn allerliefste.",
               "%s doet het braaf, maar zijn oortjes blijven plat. Koud!",
               "%s haalt diep adem en zucht. Njeg. Een heel ander kunstje misschien?"]),
    "kleur": (["%s bekijkt zichzelf nieuwsgierig... deze kleur ligt vlak naast zijn lievelingskleur!",
               "%s draait een rondje voor een plasje water. Warm! Net een tintje anders.",
               "%s aait over zijn nieuwe kleertje: bijna, njeg! Zijn lievelingskleur is een buurkleurtje."],
              ["%s snuffelt aan zijn kleertje... njeg? Mooi, maar niet zijn lievelingskleur.",
               "%s kijkt naar zijn kleertje en dan naar jou. Koud! Een heel andere kleur graag.",
               "%s trekt een pruilsnoetje. Het staat hem best, maar het is zijn kleur niet, njeg."]),
    "vriend": (["%s snuffelt blij aan deze guh... warm! Zijn allerliefste guh-vriendje is een vriendje van hem.",
                "%s geeft een pootje. Warm! Deze guh kent zijn allerbeste vriendje heel goed, njeg.",
                "%s speelt even mee en kijkt dan rond: waar is zijn allerbeste vriendje? Bijna!"],
               ["%s snuffelt aan de andere guh... njeg? Aardig, maar niet zijn allerliefste vriendje.",
                "%s knikt beleefd naar de andere guh. Koud! Zijn beste vriendje is een andere guh.",
                "%s kijkt de andere guh aan en zegt: njeg. Dat betekent: leuk, maar niet mijn bff."]),
}
ONTDEKT = {  # %1$s the guh, %2$s the favourite
    "eten": "♥ Njeg!! %1$s smult en smakt en straalt: %2$s is zijn LIEVELINGSHAPJE! VAHOEG!",
    "plek": "♥ VAHOEG! %1$s rolt door het gras van geluk: %2$s is zijn lievelingsplekje!",
    "knuffel": "♥ Oooh! %1$s knuffelt en knuffelt en laat niet meer los: %2$s is zijn lievelingsknuffel!",
    "liedje": "♥ Njeg njeg njeg! %1$s zingt uit volle borst mee: %2$s is zijn lievelingsliedje!",
    "speeltje": "♥ Wieee! %1$s kan niet meer stoppen: %2$s is zijn lievelingsspeeltje!",
    "emote": "♥ %1$s doet het nog een keer, en nog een keer: %2$s is zijn lievelingskunstje! VAHOEG!",
    "kleur": "♥ %1$s showt zijn kleertje aan iedereen: %2$s is zijn lievelingskleur! Zo vads, zo mooi!",
    "vriend": "♥ %1$s en %2$s zijn de allerbeste vriendjes! Samen zijn ze dubbel zo vahoeg!",
}
WEER = {  # action bar, %1$s the guh, %2$s the favourite
    "eten": "♥ %1$s smult van %2$s, zijn lievelingshapje!",
    "plek": "♥ %1$s huppelt rond: weer op zijn lievelingsplekje, %2$s!",
    "knuffel": "♥ %1$s knuffelt %2$s weer, zijn allerliefste knuffel!",
    "liedje": "♥ %1$s zingt mee met %2$s, zijn lievelingsliedje!",
    "speeltje": "♥ %1$s speelt weer met %2$s. VAHOEG!",
    "emote": "♥ %1$s doet zijn lievelingskunstje: %2$s!",
    "kleur": "♥ %1$s draagt weer %2$s, zijn lievelingskleur!",
    "vriend": "♥ %1$s is weer samen met %2$s, zijn allerbeste vriendje!",
}
TEXTS = {
    "gui.guhs.favorietjes.hint.warm": "Guh kijkt nieuwsgierig... warm, warm!",
    "gui.guhs.favorietjes.hint.koud": "Guh snuffelt... njeg? Koud!",
    "gui.guhs.favorietjes.ontdekt_titel": "♥ Favorietje ontdekt! ♥",
    "gui.guhs.favorietjes.alle": "★ Je kent nu ALLE favorietjes van %s! Jullie zijn echt lieve vadsjes van elkaar. VAHOEG! ★",
    "gui.guhs.favorietjes.neuriet": "♪ %1$s neuriet zijn lievelingsliedje (%2$s): hij heeft er zin in! Een blije guh juicht harder! ♪",
    "subtitles.guhs.favorietjes.ontdekt": "Favorietje ontdekt!",
    "subtitles.guhs.favorietjes.snuffel": "Guh snuffelt",
    "subtitles.guhs.favorietjes.nieuwsgierig": "Guh is nieuwsgierig",
    # fundament's Guhdex page text, a bit more helpful now that the favourites can be found
    "gui.guhs.mijnguhs.favorietjes_hint": "Nog geen favorietjes ontdekt. Probeer van alles: snackjes, plekjes, knuffels, liedjes, "
                                          "speeltjes, kunstjes, kleertjes en andere guhs. Kijkt hij nieuwsgierig? Dan ben je warm!",
}

# the eerste keren: id -> (title, the funny line the guh writes)
EERSTE = {
    # a favourite found (per kind) and all of them
    "favorietjes_fav_eten": ("Lievelingshapje gevonden", "Mijn baasje weet nu wat ik het állerlekkerst vind. Nu krijg ik het vast elke dag. Toch? Njeg?"),
    "favorietjes_fav_plek": ("Lievelingsplekje gevonden", "We waren op mijn lievelingsplekje! Ik heb er even in het gras gerold. Drie keer. Vier keer."),
    "favorietjes_fav_knuffel": ("Lievelingsknuffel gevonden", "Ik heb mijn allerliefste knuffel gevonden. Ik laat hem nooit meer los. Behalve als er een kaasknabbel komt."),
    "favorietjes_fav_liedje": ("Lievelingsliedje gevonden", "Mijn baasje speelde MIJN liedje! Ik zong zo hard mee dat mijn oortjes ervan trilden."),
    "favorietjes_fav_speeltje": ("Lievelingsspeeltje gevonden", "Dit speeltje is van mij. Nou ja, van ons. Maar vooral van mij, njeg."),
    "favorietjes_fav_emote": ("Lievelingskunstje gevonden", "Mijn baasje vindt mijn lievelingskunstje ook leuk! Ik ga het nu heel vaak doen. Heel, heel vaak."),
    "favorietjes_fav_kleur": ("Lievelingskleur gevonden", "Ik draag mijn lievelingskleur! Ik ben nu officieel de mooiste guh van de wereld. Zeg ik."),
    "favorietjes_fav_vriend": ("Allerbeste guh-vriendje", "Mijn baasje snapt nu wie mijn allerbeste vriendje is. We doen alles samen. Zelfs snurken."),
    "favorietjes_alle": ("Alle favorietjes!", "Mijn baasje kent al mijn geheimpjes. Alle acht! Behalve waar ik die ene kaasknabbel verstopt heb. Shh."),
    # snacks
    "favorietjes_ijsje": ("Eerste ijsje", "Brrr! Koud op mijn snoet! Nog een hapje. Brrr! Nog een hapje. Njeg."),
    "favorietjes_taartje": ("Eerste gebakje", "Ik heb gebak gegeten. Er zitten nu kruimels in mijn vachtje en die gaan er nooit meer uit."),
    "favorietjes_suikerspin": ("Eerste snoepje", "Snoep! Het plakt aan mijn snorharen. Dit is de beste dag van mijn leven."),
    "favorietjes_fruit": ("Eerste stukje fruit", "Fruit is gezond, zegt mijn baasje. Het is ook lekker, zeg ik. Maar kaasknabbels zijn lekkerder."),
    "favorietjes_frituur": ("Eerste gefrituurde hapje", "Knapperig! Goudbruin! Vads! Ik denk dat ik nu nog een beetje vadsiger ben. Mooi zo, VAHOEG!"),
    "favorietjes_visje": ("Eerste visje", "Ik heb een visje gegeten. Het keek me een beetje verbaasd aan. Ik het ook."),
    "favorietjes_hapjes_100": ("Honderd hapjes", "Ik heb al honderd hapjes uit de hand van mijn baasje gegeten. Mijn vads groeit en groeit. VAHOEG!"),
    # places
    "favorietjes_strand": ("Naar het strand", "Zand tussen mijn tenen! Ik heb een zandkasteel gebouwd. Nou ja, een zandbultje."),
    "favorietjes_sneeuw": ("In de sneeuw", "Alles is wit en koud en knisperig. Mijn pootjes zijn ijsklontjes. Maar het is zo mooi, njeg!"),
    "favorietjes_zee": ("Aan zee", "Zoveel water! Waar komt het allemaal vandaan? Ik heb er heel lang naar gekeken."),
    "favorietjes_woestijn": ("In de woestijn", "Warm! Zanderig! Geen kaasknabbel te bekennen. Gauw weer verder, njeg."),
    "favorietjes_bloemen": ("Tussen de bloemetjes", "Ik heb aan alle bloemetjes gesnuffeld. Eentje kietelde. Ik moest niezen. Hatsjoeguh!"),
    "favorietjes_paddenstoel": ("Bij de paddenstoelkoeien", "Er liepen koeien met paddenstoelen op hun rug. Ik heb niks gezegd. Beleefd, hè."),
    "favorietjes_jungle": ("In de jungle", "Hoge bomen, gekke geluiden en een papegaai die 'guh' riep. Of was ik dat zelf?"),
    "favorietjes_moeras": ("In het moeras", "Het is hier drassig en een beetje stinkerig. Mijn pootjes zeiden 'sop sop'. Leuk geluid!"),
    "favorietjes_grot": ("In een grot", "Het is donker en er druppelt iets. Ik ben heel dapper geweest. Ik heb maar één keer gepiept."),
    "favorietjes_bergen": ("Hoog in de bergen", "We zijn helemaal naar boven geklommen. Ik kon mijn huisje bijna zien! Denk ik. Njeg."),
    "favorietjes_knuffeldal": ("In het Knuffeldal", "Een heel dal vol knuffels en lieve guhs. Ik wil hier wonen. Of in ieder geval logeren."),
    "favorietjes_guhpolder": ("In de Guhpolder", "Molentjes, slootjes en ijs! Ik ben niet uitgegleden. Oké, één keertje. Oké, drie keer."),
    "favorietjes_vadswoud": ("In het Vadswoud", "Het Vadswoud ruikt naar knabbelbessen en avontuur. Ik voelde me heel stoer. En een beetje klein."),
    # dimensions
    "favorietjes_nether": ("In de Nether", "Het is hier heel warm en heel rood. Ik voelde me bijna een gefrituurde kaasknabbel. Snel weg!"),
    "favorietjes_end": ("In het End", "Het End is heel stil en er lopen lange zwarte dingen rond. Ik heb ze niet aangekeken. Veiliger."),
    "favorietjes_guhmaag": ("In de Guhmaag", "Ik ben in de maag van mijn baasje geweest. Van binnen! Het is er gezelliger dan je denkt."),
    "favorietjes_barbecuether": ("In de Guhbarbecuether", "Het ruikt hier naar barbecue en ik heb honger. Ik heb altijd honger, maar nu extra."),
    # plush, songs, clothes, emotes
    "favorietjes_knuffel": ("Eerste knuffel geknuffeld", "Ik heb een knuffel geknuffeld. Hij knuffelde niet terug, maar ik weet dat hij het wel wilde."),
    "favorietjes_koortje": ("Eerste koortje", "Iemand speelde een liedje op de xylofoon en ik zong mee. Ik ben nu een zangguh. Bijna beroemd."),
    "favorietjes_disco": ("Eerste disco", "Lichtjes! Muziek! Ik heb gedanst tot mijn vads ervan schudde. Disco is mijn ding, njeg."),
    "favorietjes_kleding": ("Eerste kleertje", "Ik draag kleren! Ik ben nu een heel deftige guh. Niet meer in de modder rollen. Oké, een beetje."),
    "favorietjes_dansje": ("Eerste dansje", "Ik heb gedanst waar mijn baasje bij was! Mijn baasje klapte. Ik denk dat ik een ster ben."),
    "favorietjes_vahoeg": ("Eerste VAHOEG-sprong", "VAHOEG! Ik sprong zo hoog dat ik even de wolken kon ruiken. Ze ruiken naar suikerspin."),
    "favorietjes_zingen": ("Eerste liedje gezongen", "Ik zong een liedje voor mijn baasje. Het had geen woorden, alleen 'njeg'. Maar met veel gevoel."),
    "favorietjes_rollen": ("Eerste koprol", "Ik heb een koprol gedaan! Daarna wist ik even niet meer waar boven was. Nog een keer!"),
    # travelling, cuddles, work, play
    "favorietjes_reis_1000": ("1000 blokjes samen", "Wij hebben samen al 1000 blokjes gelopen! Mijn pootjes zijn moe, maar mijn hartje is blij."),
    "favorietjes_reis_10000": ("10.000 blokjes samen", "Tienduizend blokjes! Ik heb ze niet allemaal geteld. Wel de meeste. Tot twaalf. Njeg."),
    "favorietjes_marathon": ("Een hele marathon!", "42.195 blokjes samen: een echte marathon! Ik verdien een medaille. Van kaas, graag."),
    "favorietjes_knuffels_10": ("Tien dikke knuffels", "Al tien dikke knuffels! Ik hou een lijstje bij. Het lijstje is ook geknuffeld."),
    "favorietjes_knuffels_100": ("Honderd dikke knuffels", "Honderd knuffels! Ik ben de meest geknuffelde guh van de hele wereld. Zeker weten."),
    "favorietjes_record": ("Samen een record", "Mijn baasje zette een record en ik stond te juichen! Eigenlijk is het dus ook mijn record, njeg."),
    "favorietjes_klusjes_100": ("Honderd klusjes", "Honderd klusjes gedaan! Ik ben de hardst werkende guh van het hele huisje. Tijd voor een dutje."),
    "favorietjes_speeltjes_50": ("Vijftig keer gespeeld", "Vijftig keer gespeeld! Spelen is ook heel hard werken, hoor. Vooral voor mijn oortjes."),
    "favorietjes_nachtje": ("Eerste nachtje in het huisje", "Mijn eerste nacht in mijn eigen huisje. Ik heb de hele nacht gesnurkt. Zeggen ze. Ik niet."),
    # time together, weather, the sky
    "favorietjes_week": ("Een week samen", "We zijn al een hele week samen! Zeven dagen. Dat zijn wel zeven keer zo veel knuffels."),
    "favorietjes_maand": ("Een maand samen", "Een hele maand samen! Ik ken nu al je geheimpjes. En jij de mijne. Nou ja, de meeste."),
    "favorietjes_honderd_dagen": ("Honderd dagen samen", "Honderd dagen! Ik heb een taartje gebakken. In mijn hoofd. Het was heel lekker."),
    "favorietjes_regen": ("Eerste regenbui", "Druppels op mijn oortjes! Ik ben een natte guh nu. Natte guhs ruiken naar... guh."),
    "favorietjes_onweer": ("Eerste onweer", "BOEM! Ik ben heel even tegen mijn baasje aan gekropen. Maar ik was niet bang. Echt niet. Njeg."),
    "favorietjes_sneeuwvlokjes": ("Eerste sneeuwvlokjes", "Er vielen sneeuwvlokjes op mijn snoet. Ik heb er eentje opgegeten. Smaakt naar niks. Toch lekker."),
    "favorietjes_sterrennacht": ("Eerste sterrennacht", "We keken samen naar de sterren. Ik heb er eentje uitgezocht. Hij heet Kaasknabbel."),
    "favorietjes_volle_maan": ("Eerste volle maan", "De maan was helemaal rond. Net een kaasknabbel. Ik heb er heel lang naar gekeken, met open snoet."),
    "favorietjes_besties": ("Beste vriendjes!", "Ik heb een bestie! Een echte! We zijn nu zielsvadsjes. Mijn baasje blijft wel op één, hoor."),
}

# the wist-je-datjes: sleutel -> texts (%1$s = the thing, only for the sleutels in MET_DING; %2$s = the owner's name)
WIST = {
    # snacks (%1$s = the snack)
    "eten_ijskoud": ["Wist je dat %1$s zo koud is dat mijn oortjes er rechtop van gaan staan? Nog een, graag.",
                     "Vandaag at ik %1$s. Hersenbevriezing! Maar dan in mijn vads. Vadsbevriezing. Njeg.",
                     "%2$s gaf me %1$s. Ik heb het heel langzaam opgelikt, zodat het langer duurde."],
    "eten_gebak": ["Wist je dat %1$s precies in mijn snoet past? Ik heb het getest. Twee keer.",
                   "Vandaag kreeg ik %1$s van %2$s. De kruimels bewaar ik voor later, in mijn vachtje.",
                   "Ik droom soms van %1$s. Vandaag was de droom echt. VAHOEG!"],
    "eten_snoep": ["Wist je dat %1$s aan je snorharen blijft plakken? Nu heb ik roze snorharen. Mooi!",
                   "Vandaag at ik %1$s. Ik ben nu zo suikerig dat ik drie rondjes heb gerend."],
    "eten_fruit": ["Wist je dat %1$s gezond is? %2$s zegt dat. Ik at het toch op, want het was lekker.",
                   "Vandaag at ik %1$s. Ik voel me nu een heel gezonde guh. Tijd voor een kaasknabbel."],
    "eten_frituur": ["Wist je dat %1$s knapperig kraakt? Knap knap knap. Mijn lievelingsgeluid, bijna.",
                     "Vandaag at ik %1$s. Mijn vads is er blij van. Ik ben er blij van. Iedereen blij."],
    "eten_knabbel": ["Wist je dat ik %1$s kan ruiken van drie blokjes ver? Van vier als ik honger heb.",
                     "Vandaag gaf %2$s me %1$s. Kaas is liefde. Dat weet elke guh.",
                     "Ik heb %1$s gegeten. Er kan altijd nog een kaasknabbel bij, njeg."],
    "eten_vis": ["Wist je dat %1$s naar zee smaakt? Nu wil ik naar zee. Met %2$s.",
                 "Vandaag at ik %1$s. Ik ben nu een beetje een zeeguh."],
    "eten_overig": ["Vandaag at ik %1$s uit de hand van %2$s. Alles is lekkerder uit de hand van %2$s.",
                    "Wist je dat %1$s best lekker is? Ik had het niet verwacht, maar mijn buikje wel."],
    # places (%1$s = the biome)
    "plek_guhmensie": ["Wist je dat het in %1$s naar suikerspin ruikt? Ik ben er zeker van.",
                       "Vandaag liepen %2$s en ik door %1$s. Alles roze, net als ik!"],
    "plek_knuffeldal": ["In het Knuffeldal knuffelt iedereen iedereen. Ik heb er wel twintig geknuffeld.",
                        "Wist je dat de guhs in het Knuffeldal extra pluizig zijn? Ik ben stiekem een beetje jaloers."],
    "plek_guhpolder": ["Wist je dat een molentje draait als je er heel hard tegen blaast? Ik heb het geprobeerd. Niet waar.",
                       "Vandaag waren we in de Guhpolder. Er lag ijs op de sloot en ik op mijn billen."],
    # 3.0 (Guhverhalen): the two new biomes
    "plek_sneeuwguhtoendra": ["Wist je dat de sneeuw in de Sneeuwguhtoendra tot aan mijn buikje komt? Mijn vads is nu een sneeuwvads.",
                              "Vandaag liepen %2$s en ik door de witte toendra. Ik hoorde ver weg sledebelletjes. Tingeling, njeg!"],
    "plek_guhwaii": ["Wist je dat op Guhwai'i de palmbomen een gezichtje hebben? Eentje knipoogde naar me. Echt waar!",
                     "Vandaag waren we op Guhwai'i. Warm zand, een kokosnoot en een zeemeeuwtje dat \"Mijn!\" riep. Aloha, njeg!"],
    "plek_vadswoud": ["Wist je dat er in het Vadswoud knabbelbessen groeien? Ik heb er drie gevonden. En opgegeten.",
                      "Vandaag liepen we door het Vadswoud. Ik hoorde een uil. Of een Mika. Gauw door!"],
    "plek_strand": ["Wist je dat zand kriebelt tussen je tenen? Ik heb nu acht kriebeltenen.",
                    "Vandaag waren we op %1$s. Ik heb een schelpje gevonden en het Schelpie genoemd."],
    "plek_sneeuw": ["Wist je dat sneeuw knispert onder je pootjes? Knisper knisper. Ik kan er niet mee stoppen.",
                    "Vandaag was het koud in %1$s. %2$s had het ook koud. Toen hebben we geknuffeld. Beter!"],
    "plek_zee": ["Wist je dat de zee nooit ophoudt? Ik heb heel lang gekeken. Hij hield niet op.",
                 "Vandaag zag ik %1$s. Heel veel water en geen enkele kaasknabbel. Rare plek."],
    "plek_woestijn": ["Wist je dat een cactus prikt? Ja. Nu weet ik het ook. Au, njeg.",
                      "Vandaag was het zo warm in %1$s dat mijn vachtje bijna smolt. Gauw een ijsje!"],
    "plek_bloemen": ["Wist je dat bloemen lekker ruiken? Behalve als er een bij op zit. Dan zoemen ze.",
                     "Vandaag liepen we door %1$s. Ik heb een bloemetje achter mijn oortje gedaan."],
    "plek_paddenstoel": ["Wist je dat er koeien zijn met paddenstoelen op hun rug? Ik heb het zelf gezien. Echt!",
                         "Vandaag waren we op %1$s. Alles was paars en sponzig. Heel gek, heel leuk."],
    "plek_jungle": ["Wist je dat er in de jungle papegaaien zijn die alles nadoen? Eentje zei 'njeg'. Mijn nieuwe vriend.",
                    "Vandaag was ik in %1$s. De bomen waren zo hoog dat mijn nekje er moe van werd."],
    "plek_moeras": ["Wist je dat een moeras 'blub' zegt? Ik zei 'blub' terug. Nu zijn we vriendjes.",
                    "Vandaag liepen we door %1$s. Mijn pootjes zijn nu groen. Staat me goed."],
    "plek_grot": ["Wist je dat het in een grot echoot? Njeg! Njeg! njeg... Nu zijn we met z'n drieën.",
                  "Vandaag waren we onder de grond. Ik bleef heel dicht bij %2$s, voor de zekerheid."],
    "plek_bergen": ["Wist je dat je vanaf een berg heel ver kan kijken? Ik zag een kaasknabbel. Denk ik. Of een schaap.",
                    "Vandaag klommen we op %1$s. Ik ben nu een bergguh. Een moe bergguh."],
    "plek_bos": ["Wist je dat bomen heel goed zijn om je achter te verstoppen? Behalve als je vads net zo breed is.",
                 "Vandaag wandelden we door %1$s. Ik heb een eekhoorn gezien. Of een blaadje."],
    "plek_wei": ["Wist je dat gras heerlijk is om in te rollen? Ik heb het uitgebreid getest.",
                 "Vandaag liepen we door %1$s. Wind in mijn oortjes. Het beste gevoel ooit."],
    "plek_overig": ["Vandaag waren we in %1$s. Ik wist niet dat de wereld zo groot was. Nu wel.",
                    "Wist je dat ik overal heen ga waar %2$s heen gaat? Zelfs naar %1$s."],
    # plush, songs, clothes
    "knuffel": ["Vandaag knuffelde ik %1$s. Zacht, warm, en hij klaagde niet één keer.",
                "Wist je dat %1$s precies zo groot is als ik? Daarom knuffelt hij zo lekker."],
    "liedje_koortje": ["Vandaag zong ik %1$s mee. Ik kende de woorden niet, dus ik zong 'njeg'.",
                       "Wist je dat %1$s mijn oortjes laat wiebelen? Ze wiebelen nu nog."],
    "liedje_disco": ["Vandaag danste ik op %1$s. Mijn vads deed ook mee. Hij heeft ritme!",
                     "Wist je dat je op %1$s niet stil kan blijven staan? Ik heb het geprobeerd. Kon niet."],
    "kleding": ["Vandaag droeg ik %1$s. Ik zag er heel vahoeg uit, al zeg ik het zelf.",
                "Wist je dat %1$s mijn vads nog mooier laat uitkomen? %2$s vond het ook."],
    # emotes
    "emote_zwaaien": ["Ik heb vandaag naar %2$s gezwaaid. %2$s zwaaide terug! Vriendschap."],
    "emote_dansen": ["Wist je dat ik heel goed kan dansen? Ik heb het vandaag aan %2$s laten zien. Applaus!"],
    "emote_slapen": ["Vandaag deed ik een dutje. Een lang dutje. Nou ja, drie dutjes achter elkaar."],
    "emote_vahoeg": ["VAHOEG! Vandaag sprong ik zo hoog dat %2$s ervan schrok. Ik ook een beetje."],
    "emote_rollen": ["Wist je dat ik een koprol kan? Ik heb er vandaag vier gedaan. Daarna zag ik sterretjes."],
    "emote_smakken": ["Smak smak smak. Wist je dat smakken de beste manier is om te zeggen dat iets lekker is?"],
    "emote_verlegen": ["Vandaag was ik een beetje verlegen. %2$s zei iets liefs. Mijn wangetjes werden nog rozer."],
    "emote_gapen": ["Wist je dat gapen besmettelijk is? Ik gaapte en toen gaapte %2$s ook. Haha. Gaap."],
    "emote_zingen": ["Vandaag zong ik een liedje voor %2$s. Het ging over kaas. Alle mooie liedjes gaan over kaas."],
    "emote_knuffelen": ["Wist je dat knuffelen gezond is? Ik ben dus heel gezond. Supergezond. Knuffel?"],
    "emote_hartjes": ["Vandaag blies ik hartjes naar %2$s. Eentje kwam aan. De rest zweeft nog ergens rond."],
    "emote_knuffeldansje": ["Wist je dat een knuffeldansje een knuffel én een dansje is? Twee keer zo leuk dus."],
    "emote_bff_knuffel": ["Vandaag gaf ik %2$s een bff-knuffel. Zielsguh bff 5evr <3. Dat meen ik."],
    "emote_verdrietje": ["Vandaag was ik even een beetje verdrietig. %2$s aaide me en toen was het weer over. Njeg."],
    "emote_ukelele": ["Vandaag speelde ik ukelele voor %2$s. Met vier armpjes tegelijk! Aloha, njeg!"],   # (3.0: the 626-guh)
    # riding, travelling (%1$s = blocks together), dimensions
    "rit": ["Vandaag zat %2$s op mijn rug. Ik ben sterk! Een beetje plat nu, maar sterk.",
            "Wist je dat ik heel hard kan rennen met %2$s op mijn rug? Hup hup, guh!"],
    "reis": ["Wist je dat %2$s en ik al %1$s blokjes samen hebben gelopen? Ik heb ze allemaal geteld. Bijna.",
             "We zijn vandaag weer op pad geweest. Mijn pootjes zijn moe, mijn hartje is vol."],
    "dim_bovenwereld": ["Terug in de gewone wereld! Alles is groen in plaats van roze. Even wennen, hoor."],
    "dim_nether": ["Vandaag was ik in de Nether. Warm! Veel te warm! Ik wil een ijsje."],
    "dim_end": ["Wist je dat het End geen bovenkant en geen onderkant heeft? Ik ben er een beetje duizelig van."],
    "dim_guhmensie": ["Wist je dat in de Guhmensie alles roze is? Ik val er helemaal weg, zo roze ben ik."],
    "dim_guhmaag": ["Vandaag was ik in de Guhmaag van %2$s. Ik heb er naar een kaasknabbel gezwaaid."],
    "dim_guheinde": ["Het Guheinde is donker, maar met %2$s erbij ben ik nergens bang voor. Bijna nergens."],
    "dim_barbecuether": ["Wist je dat de Guhbarbecuether naar barbecue ruikt? Mijn buikje rommelde de hele tijd."],
    # minigames (after a game together)
    "spel": ["Vandaag speelden %2$s en ik een spelletje. Ik juichte het hardst van iedereen, njeg!"],
    "spel_beauty": ["Vandaag was er een Beautyshow. Ik vond iedereen mooi. Maar mezelf het mooist."],
    "spel_race": ["Wist je dat racen heel hard gaat? Mijn oortjes waaiden bijna weg. VAHOEG!"],
    "spel_meppen": ["Vandaag mepte %2$s Mika's weg. Heel zachtjes, want eigenlijk zijn ze best lief."],
    "spel_disco": ["Vandaag stond ik op de dansvloer met %2$s. Wij waren de sterren van de disco."],
    "spel_golf": ["Wist je dat een golfballetje heel ver kan rollen? Ik wilde erachteraan. Mocht niet."],
    "spel_smul": ["Vandaag was er een smulwedstrijd. Ik keek toe. Met open snoet. Er kwam niks in."],
    "spel_vissen": ["Vandaag gingen %2$s en ik vissen. Ik heb heel geduldig gewacht. Drie seconden."],
    "spel_verstop": ["Verstoppertje! Ik verstopte me achter een bloemetje. Mijn vads stak eruit. Gevonden."],
    "spel_bakkerij": ["Vandaag bakten we in de bakkerij. Ik heb de kruimels opgeruimd. Met mijn snoet."],
    "spel_creche": ["Vandaag waren we in de crèche. Al die babyguhtjes! Ik voelde me heel groot en wijs."],
    "spel_theehuis": ["Wist je dat thee het lekkerst is met een koekje? Of twee. Of de hele schaal."],
    "spel_kapper": ["Vandaag was %2$s bij de kapper. Ik heb gezegd dat het mooi was. Het was ook mooi. Echt."],
    "spel_sterrenwacht": ["Wist je dat er een sterrenbeeld is dat op een guh lijkt? Ik heb het zelf bedacht."],
    "spel_ballon": ["Vandaag vlogen we in een luchtballon! Ik heb naar alle wolken gezwaaid."],
    "spel_knuffelbad": ["Vandaag gingen we zwemmen in het Knuffelbad. Ik ben een natte, blije guh."],
    "spel_grijpmachine": ["Wist je dat de grijpmachine altijd nét loslaat? Ik denk dat hij het expres doet."],
    "spel_sjoelen": ["Vandaag sjoelden %2$s en ik. Ik juichte bij elke schijf. Ook bij de verkeerde."],
    "spel_doolhof": ["Wist je dat ik de weg in het doolhof precies weet? Ik zeg het alleen niet. Spannender!"],
    "spel_katapult": ["Vandaag schoten we knabbels met de katapult. Ik wilde er zelf in. Mocht niet, njeg."],
    "spel_knabbelspelen": ["Vandaag waren de Knabbelspelen! Ik heb voor %2$s gejuicht tot mijn stemmetje op was."],
    "spel_elftocht": ["Wist je dat schaatsen op een sloot heel glad is? Ik weet het nu. Mijn billen ook."],
    "spel_circuit": ["Vandaag reden we op het Guh-Circuit. Vroem! Ik ben nog steeds een beetje duizelig."],
    "spel_beroepen": ["Vandaag deed %2$s een beroep in de beroepenstraat. Later word ik ook iets. Knuffelaar."],
    "record": ["Wist je dat %2$s vandaag een record heeft gezet? Ik juichte zo hard dat ik er de hik van kreeg.",
               "RECORD! %2$s is de allerbeste. Dat wist ik natuurlijk allang."],
    # chores
    "klus": ["Vandaag heb ik hard gewerkt. Een dutje is dus wel verdiend."],
    "klus_opgraven": ["Wist je dat er kaasknabbels in de grond zitten? Ik graaf ze op. Sommige eet ik stiekem op."],
    "klus_farmen": ["Vandaag heb ik geoogst en weer geplant. Ik ben nu een boerenguh. Met een strohoedje in mijn hoofd."],
    "klus_opruimen": ["Vandaag ruimde ik alles op. Alles ligt nu in de kist. Behalve één kaasknabbel. In mijn buikje."],
    "klus_dieren": ["Wist je dat een guhschaapje heel zacht is? Ik heb het gevoerd en toen even tegen hem aan gelegen."],
    "klus_bakken": ["Vandaag bakte ik iets in de knabbeloven. Het rook zo lekker dat ik er bijna in kroop."],
    "klus_vissen": ["Vandaag heb ik gevist bij het huisje. Ik ving een schelpje. Het is nu mijn beste vriendje. Na %2$s."],
    "klus_waken": ["Vandaag hield ik de wacht bij het huisje. Er kwam een Mika. Ik heb heel lief 'ga weg' gezegd."],
    "klus_plukken": ["Wist je dat bloemetjes plukken heel ontspannend is? Ik heb er ook een paar opgegeten. Per ongeluk."],
    "klus_lampjes": ["Vandaag deed ik de lampjes aan. Nu is het huisje knus en gezellig. Ik ben een lampjesguh."],
    "klus_oppas": ["Vandaag paste ik op de muisjes. Ze piepten de hele tijd. Ik piepte terug. Gezellig."],
    # toys (%1$s = the toy)
    "speeltje": ["Vandaag speelde ik met %1$s. Spelen is het allerbelangrijkste werk dat er is."],
    "speeltje_knabbelbal": ["Vandaag duwde ik de knabbelbal tot de knabbel eruit viel. Beloning! Ik ben heel slim.",
                            "Wist je dat een knabbelbal nooit rolt waar je wilt? Toch krijg ik de knabbel er altijd uit."],
    "speeltje_glijbaantje": ["Wieee! Vandaag gleed ik van het glijbaantje. En nog een keer. En nog een keer. Wieee!",
                             "Wist je dat klimmen het saaie stukje is en glijden het leuke? Ik doe het toch allebei."],
    "speeltje_tunnel": ["Wist je dat je je in de pluizige tunnel heel goed kan verstoppen? Niemand vond me. Ik mezelf ook niet.",
                        "Vandaag rende ik door de pluizige tunnel. Het kriebelde aan alle kanten. Heerlijk!"],
    "speeltje_wip_schommel": ["Vandaag zat ik op de schommel. Hoger! Hoger! Oké, niet zó hoog. Njeg.",
                              "Wist je dat een wip alleen werkt met z'n tweeën? Daarom zijn vriendjes zo handig."],
    # friends (%1$s = the other guh)
    "vriendje": ["Vandaag speelde ik met %1$s. We hebben samen een kaasknabbel gedeeld. Nou ja, ik at hem op.",
                 "Wist je dat %1$s bijna net zo vads is als ik? Bijna."],
    "vriendjes_nieuw": ["%1$s en ik zijn nu vriendjes! We hebben het beklonken met een neusje-neusje.",
                        "Vandaag werden %1$s en ik vriendjes. We gaan samen heel veel knabbels delen. Misschien."],
    "besties": ["%1$s is mijn bestie! We gaan samen door dik en dun. Vooral door dik, want we zijn vads.",
                "Wist je dat %1$s en ik zelfs tegelijk snurken? Zo goed zijn we op elkaar ingespeeld."],
    # the huisje, sleeping, the guhkamer
    "huisje_in": ["Ik woon nu in Guhhuisje %1$s. Ik heb de beste kamer. Er is maar één kamer."],
    "droom": ["Vannacht droomde ik van een kaasknabbel zo groot als een huis. Ik woonde erin.",
              "Ik droomde dat ik kon vliegen. Met mijn oortjes. Flap flap. VAHOEG!",
              "Vannacht droomde ik dat %2$s een guh was. Een hele mooie, vadsige guh.",
              "Ik droomde van een regen van ijsjes. Ik had mijn snoet wijd open.",
              "Vannacht droomde ik dat ik de allergrootste guh van de wereld was. Iedereen wilde me knuffelen.",
              "Ik droomde dat een Mika mijn kaasknabbel pikte. Toen werd ik wakker. Knabbel was er nog. Pfieuw."],
    "wakker": ["Goedemorgen! Ik heb elf uur geslapen en ik ben nog steeds moe, njeg.",
               "Wist je dat de ochtend naar dauw en kaasknabbels ruikt? Nou ja, naar dauw.",
               "Ik ben wakker! Tijd voor ontbijt. Ontbijt is kaasknabbels. Lunch ook."],
    "guhkamer": ["Ik logeer nu in de Guhkamer. Er is een bedje, speelgoed, en af en toe komt %2$s kijken. Perfect."],
    # petting and cuddles
    "aai": ["%2$s aaide me vandaag over mijn kopje. Ik heb heel hard gesnord. Guhs snorren, wist je dat?",
            "Wist je dat een aai precies één hartje waard is? Ik hou ze allemaal bij.",
            "Vandaag kreeg ik een aaitje. Mijn oortjes gingen vanzelf plat. Van geluk.",
            "Aai aai aai. Meer hoeft een guh niet te weten."],
    "knuffel_groot": ["Vandaag kreeg ik een dikke knuffel van %2$s. Ik ben helemaal warm van binnen.",
                      "Wist je dat een knuffel het beste medicijn is? Tegen alles. Behalve honger.",
                      "%2$s knuffelde me zo lang dat we bijna samen in slaap vielen."],
    # weather and the sky
    "regen": ["Wist je dat regendruppels op je oortjes tikken als een trommeltje? Ik heb er een liedje van gemaakt.",
              "Vandaag regende het. %2$s en ik werden nat. Samen nat is gezellig nat."],
    "onweer": ["Vandaag onweerde het. Ik kroop tegen %2$s aan. Het was niet eng. Het was alleen heel hard."],
    "sneeuwvlokjes": ["Vandaag sneeuwde het! Ik ving sneeuwvlokjes op mijn tong. Ze smaken naar wolkjes."],
    "nacht": ["Wist je dat sterren knipogen? Ik knipoogde terug. We hebben nu een geheimpje.",
              "Vanavond keken %2$s en ik naar de lucht. Ik heb één ster Njeg genoemd.",
              "De nacht is donker, maar met %2$s erbij is het knus."],
    "volle_maan": ["Wist je dat de volle maan op een kaasknabbel lijkt? Ik heb er heel lang naar zitten smachten."],
}
# the sleutels whose texts may use %1$s (the others get "" as the thing)
MET_DING = ({k for k in WIST if k.startswith(("eten_", "plek_", "speeltje", "liedje_"))}
            | {"knuffel", "kleding", "reis", "vriendje", "vriendjes_nieuw", "besties", "huisje_in"})
# written directly by Favorietjes on a discovery (%1$s = the favourite; "alle": %1$s = the owner)
ONTDEKT_WIST = {
    "ontdekt_eten": "Wist je dat %1$s mijn allerliefste hapje is? Nu weet je het. Hint hint.",
    "ontdekt_plek": "Wist je dat %1$s mijn lievelingsplekje is? Daar ben ik het vadsigst gelukkig.",
    "ontdekt_knuffel": "Wist je dat %1$s mijn allerliefste knuffel is? Ik knuffel hem ook in mijn dromen.",
    "ontdekt_liedje": "Wist je dat %1$s mijn lievelingsliedje is? Als het speelt, zing ik keihard mee. Ook bij spelletjes!",
    "ontdekt_speeltje": "Wist je dat %1$s mijn lievelingsspeeltje is? Ik kan er uren mee spelen. Minuten. Heel veel minuten.",
    "ontdekt_emote": "Wist je dat %1$s mijn lievelingskunstje is? Kijk maar, ik doe het nog een keer!",
    "ontdekt_kleur": "Wist je dat %1$s mijn lievelingskleur is? Alles in %1$s maakt me blij.",
    "ontdekt_vriend": "Wist je dat %1$s mijn allerbeste guh-vriendje is? Samen zijn we onverslaanbaar vads.",
    "alle": "Vandaag kent %1$s AL mijn favorietjes. Alle acht. Ik voel me helemaal gezien. VAHOEG!",
}
# the sleutels Verhaaltjes.java writes: each must have texts
MINIGAMES = ["beauty", "race", "meppen", "disco", "golf", "smul", "vissen", "verstop", "bakkerij", "creche", "theehuis", "kapper",
             "sterrenwacht", "ballon", "knuffelbad", "grijpmachine", "sjoelen", "doolhof", "katapult", "knabbelspelen", "elftocht",
             "circuit", "beroepen"]
KLUSSEN = ["opgraven", "farmen", "opruimen", "dieren", "bakken", "vissen", "waken", "plukken", "lampjes", "oppas"]
EMOTES = ["zwaaien", "dansen", "slapen", "vahoeg", "rollen", "smakken", "verlegen", "gapen", "zingen", "knuffelen", "hartjes",
          "knuffeldansje", "bff_knuffel", "verdrietje"]
PLEK_GROEPEN = ["guhmensie", "knuffeldal", "guhpolder", "vadswoud", "strand", "sneeuw", "zee", "woestijn", "bloemen", "paddenstoel",
                "jungle", "moeras", "grot", "bergen", "bos", "wei", "overig",
                "sneeuwguhtoendra", "guhwaii"]   # (3.0)
VERWACHT = (["eten_" + g for g in ("ijskoud", "gebak", "snoep", "fruit", "frituur", "knabbel", "vis", "overig")]
            + ["plek_" + g for g in PLEK_GROEPEN] + ["spel_" + g for g in MINIGAMES] + ["klus_" + k for k in KLUSSEN]
            + ["emote_" + e for e in EMOTES] + ["speeltje_" + s for s in ("knabbelbal", "glijbaantje", "tunnel", "wip_schommel")]
            + ["dim_" + d for d in ("bovenwereld", "nether", "end", "guhmensie", "guhmaag", "guheinde", "barbecuether")]
            + ["knuffel", "liedje_koortje", "liedje_disco", "kleding", "rit", "reis", "spel", "record", "klus", "speeltje", "vriendje",
               "vriendjes_nieuw", "besties", "huisje_in", "droom", "wakker", "guhkamer", "aai", "knuffel_groot", "regen", "onweer",
               "sneeuwvlokjes", "nacht", "volle_maan"])


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)
    for kind, (warm, koud) in HINTS.items():
        for i, t in enumerate(warm):
            h.lang(f"gui.guhs.favorietjes.hint.warm.{kind}.{i}", t, t)
        for i, t in enumerate(koud):
            h.lang(f"gui.guhs.favorietjes.hint.koud.{kind}.{i}", t, t)
    for kind, t in ONTDEKT.items():
        h.lang(f"gui.guhs.favorietjes.ontdekt.{kind}", t, t)
    for kind, t in WEER.items():
        h.lang(f"gui.guhs.favorietjes.weer.{kind}", t, t)
    for k, (titel, tekst) in EERSTE.items():
        h.lang(f"gui.guhs.dagboek.eerste.{k}", titel, titel)
        h.lang(f"gui.guhs.dagboek.eerste.{k}.tekst", tekst, tekst)
    for sleutel, lijst in WIST.items():
        for i, t in enumerate(lijst):
            h.lang(f"gui.guhs.wistjedat.favorietjes.{sleutel}.{i}", t, t)
    for sleutel, t in ONTDEKT_WIST.items():
        h.lang(f"gui.guhs.wistjedat.favorietjes.{sleutel}", t, t)


def data(h):
    h.w(f"{h.D}/favorietjes/favorietjes.json", {
        "_": "Made by tools/features/favorietjes.py: the clothes -> colour table and the number of texts per wist-je-datje.",
        "kleuren": kleuren(h),
        "wistjedat": {k: len(v) for k, v in WIST.items()},
    })


# =====================================================================================================================
# the game test room
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((12, 5, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("favorietjes_test_wei")


def selfcheck(h):
    problems = []
    # every kind has its hints and texts; every colour can be found in the wardrobe
    for kind in KINDS:
        warm, koud = HINTS[kind]
        if len(warm) != HINT_VARIANTEN or len(koud) != HINT_VARIANTEN:
            problems.append(f"hints {kind}")
        if kind not in ONTDEKT or kind not in WEER or f"ontdekt_{kind}" not in ONTDEKT_WIST:
            problems.append(f"texts of {kind}")
        if f"favorietjes_fav_{kind}" not in EERSTE:
            problems.append(f"eerste keer favorietjes_fav_{kind}")
    per = Counter(kleuren(h).values())
    for k in KLEUREN:
        if per[k] < 2:
            problems.append(f"colour {k} has only {per[k]} pieces")
    # the eerste keren the Java writes all have texts
    src = ""
    for f in ("Verhaaltjes.java", "Favorietjes.java"):
        src += open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "favorietjes", f), encoding="utf-8").read()
    for eid in set(re.findall(r'"(favorietjes_[a-z0-9_]+)"', src)):
        if eid not in EERSTE and not eid.startswith("favorietjes_fav_"):
            problems.append(f"eerste keer {eid} has no text")
    # the wist-je-datjes: all expected sleutels, the args they may use
    for s in VERWACHT:
        if s not in WIST:
            problems.append(f"wist-je-datje {s} missing")
    alles = list(TEXTS.values()) + list(ONTDEKT.values()) + list(WEER.values()) + list(ONTDEKT_WIST.values())
    alles += [t for w, k in HINTS.values() for t in w + k] + [t for e in EERSTE.values() for t in e]
    for s, lijst in WIST.items():
        for t in lijst:
            alles.append(t)
            if re.search(r"%(?!\d\$s)", t):
                problems.append(f"wist {s}: only %1$s / %2$s: {t}")
            if "%1$s" in t and s not in MET_DING:
                problems.append(f"wist {s} uses %1$s but gets no thing")
    # lore: a guh is never "te vads", and guhs are not hamsters
    for t in alles:
        low = t.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {t}")
    if problems:
        raise SystemExit("favorietjes: " + "; ".join(problems))


def build(h):
    particles(h)
    sounds(h)
    advancements(h)
    texts(h)
    data(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (section "Favorietjes" of the chapter guhs_band)
# =====================================================================================================================
FTB = {  # kind: (title, text, icon, reward)
    "eten": ("Lievelingshapje", "Elke guh heeft een geheim &dlievelingshapje&r. Voer je guh allerlei snackjes: cupcakes, macarons, ijsjes, "
             "wafels, zoete bessen... Kijkt hij &dnieuwsgierig&r? Dan ben je warm (iets uit dezelfde familie). &7Snuffelt hij... njeg?&r "
             "Dan ben je koud. Het goede hapje geeft een &dhartjesexplosie&r!", "guhs:guh_cupcake", ("guhs:macaron_roze", 2)),
    "plek": ("Lievelingsplekje", "Neem je guh mee op reis: elk biome dat jullie samen bezoeken kan zijn &dlievelingsplekje&r zijn. Vaak iets "
             "in de Guhmensie, het Knuffeldal of de Guhpolder, soms een bloemenbos of een strand. Op zijn lievelingsplekje is hij de "
             "hele tijd &dblij&r!", "minecraft:cherry_sapling", ("guhs:kaas_knabbels", 12)),
    "knuffel": ("Lievelingsknuffel", "Zet knuffels neer bij je guh (die van het Wereldleven): als hij er eentje gaat knuffelen, zie je of "
                "het zijn &dlievelingsknuffel&r is. Warm = een knuffel uit dezelfde familie (kleurtjes, sprookjes of beestjes).",
                "minecraft:pink_wool", ("guhs:kaas_knabbels", 12)),
    "liedje": ("Lievelingsliedje", "Speel liedjes op de &dguh-xylofoon&r of dans in de &dGuhdisco&r met je guh erbij. Het goede liedje "
               "laat hem keihard meezingen. Daarna neuriet hij het bij elk spelletje en &djuicht hij harder&r!", "minecraft:note_block",
               ("guhs:kaas_knabbels", 12)),
    "speeltje": ("Lievelingsspeeltje", "Knabbelbal, glijbaantje, pluizige tunnel of wip & schommel: één daarvan is het &dlievelingsspeeltje&r "
                 "van je guh. Laat hem spelen en kijk wat er gebeurt!", "minecraft:slime_ball", ("guhs:kaas_knabbels", 12)),
    "emote": ("Lievelingskunstje", "Laat je guh zijn kunstjes doen (houd rechtsklik op hem, &dEmotes&r). Eén ervan is zijn "
              "&dlievelingskunstje&r. Warm = een kunstje met dezelfde stemming (vrolijk of knus).", "minecraft:cake", ("guhs:kaas_knabbels", 12)),
    "kleur": ("Lievelingskleur", "Trek je guh kleertjes aan in de kledingkast. Elk kleertje heeft een kleur; één kleur is zijn "
              "&dlievelingskleur&r. Warm = een buurkleurtje op de kleurencirkel. Draagt hij zijn lievelingskleur, dan is hij de hele "
              "dag blij!", "minecraft:pink_dye", ("guhs:kaas_knabbels", 12)),
    "vriend": ("Allerbeste guh-vriendje", "Heb je twee of meer guhs? Dan heeft elke guh een &dallerliefste guh-vriendje&r. Laat ze samen "
               "spelen en bij elkaar staan terwijl jij erbij bent: bij het goede vriendje straalt hij helemaal.", "minecraft:poppy",
               ("guhs:kaas_knabbels", 12)),
}


def ftb(fq):
    q, adv = fq.q, fq.adv
    q("favorietjes_eerste", "Een geheimpje!", "Elke tamme guh heeft &dacht geheime favorietjes&r: een hapje, een plekje, een knuffel, een "
      "liedje, een speeltje, een kunstje, een kleur en een guh-vriendje. Probeer van alles met je guh erbij. &dNieuwsgierig&r = warm, "
      "&7snuffelen... njeg?&r = koud. Raak? Dan volgt er een grote &dhartjesexplosie&r, heel veel hartjes en een blije guh.",
      "minecraft:gold_nugget", [adv("guhs:lieve_vadsjes/favorietjes_eerste")], rewards=(("guhs:kaas_knabbels", 16),), shape="circle")
    for kind in KINDS:
        titel, tekst, icoon, beloning = FTB[kind]
        q(f"favorietjes_{kind}", titel, tekst, icoon, [adv(f"guhs:lieve_vadsjes/favorietjes_{kind}")], rewards=(beloning,))
    q("favorietjes_juichen", "Juichen met een liedje", "Een &dblije&r guh juicht harder! Ken je zijn lievelingsliedje, dan neemt hij het "
      "mee naar elk spelletje: bij een goede score of een record zingt hij het keihard, met muzieknootjes en glinstertjes.",
      "minecraft:jukebox", [adv("favorietjes_juichen")], rewards=(("guhs:kaasknabbel_milkshake", 2),))
    q("favorietjes_alle", "Ik ken al je geheimpjes", "Ontdek &6alle acht&r favorietjes van één guh. In zijn dagboekje (Guhdex, Mijn guhs) "
      "staan ze dan allemaal, in plaats van ???. Zielsguh-materiaal!", "minecraft:nether_star",
      [adv("guhs:lieve_vadsjes/favorietjes_alle")], rewards=(("guhs:vahoege_vads_ingot", 1),), shape="gear", xp=300)
