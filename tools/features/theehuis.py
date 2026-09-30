"""
Het Knabbelthee-huisje (2.8.0 "Knuffeldal", phase 2, slice creche): the plein slot "theehuis" of the Knuffeldal town - a
giant teapot with a guh-face lid (theehuis_bouw.py) - where Mevrouw Theelepel pours tea (see
src/main/java/nl/juiced/guhs/feature/theehuis and guhs_work28/KNUFFEL_CONTRACT.md).

  - blocks: theetafel (laid during a theekransje), theepotje (makes tea from kaasknabbels / #knus/kaasmelk / #knus/theekruid /
    #knus/guhbloem)
  - items: the four teas knabbelthee, kaasmelkthee, theekruidthee, guhbloementhee (tag guhs:knus/thee), feest_theeservies
    (the tea house's feesttaakje, tag guhs:knus/theeservies)
  - the effect gezellig, particles theestoom / gezellig_hartje, sounds theehuis.inschenken / theehuis.kopjes_klink
  - Mevrouw Theelepel's own model (a flowery hat, pearls, a teaspoon), the theemutsje (a tea cosy hat)
  - advancements (tab guhs:knuffeldal), lang (Dutch in both files), Knus tab texts, the game test room, FTB quests (row y = 86)
"""
import os

import numpy as np
from PIL import Image

from features import knuffeldal_npcs as kn
from features import theehuis_bouw as bouw
from features import theehuis_tex as tex

CLOTHES = ["theemutsje"]
FTB_Y = 86
TEAS = list(tex.THEE)
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

_H = [0, 6, -2]
BONES = {
    # the theemutsje: a knitted tea cosy as a hat (a dome with a rim), with a fluffy pompom on top
    "outfit_theemutsje": ("head", _H, "theemuts", [([-6.2, 14.8, -10.4], [12.4, 2.8, 9.6], 0), ([-5.0, 17.6, -9.3], [10, 2.0, 7.4], 0),
                                                  ([-3.4, 19.6, -8.2], [6.8, 1.2, 5.2], 0)]),
    "outfit_theemutsje_rand": ("head", _H, "theemuts_rand", [([-6.6, 14.4, -10.8], [13.2, 1.0, 10.4], 0)]),
    "outfit_theemutsje_pom": ("head", _H, "theemuts_pom", [([-1.3, 20.7, -6.9], [2.6, 2.2, 2.6], 0)]),
}


def clothes(rng, v):
    def muts():
        a = v.stripes((246, 150, 196), (255, 240, 226), rng, 4)
        a[::3, ::5] = (200, 230, 250)
        return a

    return {"theemutsje": {"theemuts": muts, "theemuts_rand": lambda: v.fabric((246, 200, 70), rng, 8),
                           "theemuts_pom": lambda: v.fabric((255, 252, 250), rng, 6)}}


def icons(ic):
    muts = ic.icon(ic.pad(["......ww........", ".....wwww.......", "......ww........", "....abbcbba.....", "...abcbbcbba....",
                           "..abbbcbbcbba...", "..abcbbcbbbca...", ".abbbcbbcbbbba..", ".abcbbbcbbbcba..", ".dddddddddddd...",
                           ".dddddddddddd..."]),
                   {"a": (200, 110, 150), "b": (246, 150, 196), "c": (255, 240, 226), "w": (255, 252, 250), "d": (246, 200, 70)})
    return {"theemutsje": muts}


def el(frm, to, texture, faces=None):
    return {"from": frm, "to": to, "faces": {f: {"texture": texture} for f in (faces or ("down", "up", "north", "south", "west", "east"))}}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    tafel_tex = {"particle": "guhs:block/theetafel_kleed", "kleed": "guhs:block/theetafel_kleed", "rand": "guhs:block/theetafel_rand",
                 "poot": "minecraft:block/cherry_planks", "kopje": "minecraft:block/white_terracotta", "pot": "guhs:block/theepotje",
                 "taart": "minecraft:block/cake_side", "taart_top": "minecraft:block/cake_top", "goud": "minecraft:block/gold_block"}
    tafel = [el([0, 13, 0], [16, 16, 16], "#kleed", ("up", "down")),
             {"from": [0, 8, -0.01], "to": [16, 16, -0.01], "faces": {"north": {"texture": "#rand"}, "south": {"texture": "#rand"}}},
             {"from": [0, 8, 16.01], "to": [16, 16, 16.01], "faces": {"north": {"texture": "#rand"}, "south": {"texture": "#rand"}}},
             {"from": [-0.01, 8, 0], "to": [-0.01, 16, 16], "faces": {"west": {"texture": "#rand"}, "east": {"texture": "#rand"}}},
             {"from": [16.01, 8, 0], "to": [16.01, 16, 16], "faces": {"west": {"texture": "#rand"}, "east": {"texture": "#rand"}}},
             el([6.5, 0, 6.5], [9.5, 13, 9.5], "#poot"), el([3.5, 0, 3.5], [12.5, 1.5, 12.5], "#poot")]
    gedekt = tafel + [
        el([2, 16, 2], [5, 16.5, 5], "#kopje"), el([2.5, 16.5, 2.5], [4.5, 18.5, 4.5], "#kopje"),
        el([11, 16, 11], [14, 16.5, 14], "#kopje"), el([11.5, 16.5, 11.5], [13.5, 18.5, 13.5], "#kopje"),
        el([9, 16, 3], [13, 20, 7], "#pot"), el([10, 20, 4], [12, 21, 6], "#goud"), el([13, 17, 4.5], [14.5, 19, 5.5], "#pot"),
        el([3, 16, 10], [7, 18.5, 14], "#taart", ("north", "south", "west", "east")), el([3, 18.5, 10], [7, 18.5, 14], "#taart_top", ("up",))]
    w(f"{A}/models/block/theetafel.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": tafel_tex,
                                           "elements": tafel})
    w(f"{A}/models/block/theetafel_gedekt.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                  "textures": tafel_tex, "elements": gedekt})
    w(f"{A}/blockstates/theetafel.json", {"variants": {"gedekt=false": {"model": "guhs:block/theetafel"},
                                                       "gedekt=true": {"model": "guhs:block/theetafel_gedekt"}}})
    w(f"{A}/models/item/theetafel.json", {"parent": "guhs:block/theetafel_gedekt"})
    # the teapot: the spout is its front (north), the lid has a guh face on that side
    pot_tex = {"particle": "guhs:block/theepotje", "pot": "guhs:block/theepotje", "deksel": "guhs:block/theepotje_deksel",
               "gezicht": "guhs:block/theepotje_gezicht", "goud": "minecraft:block/gold_block"}
    potje = [el([4, 0, 4], [12, 6, 12], "#pot"),
             {"from": [5, 6, 5], "to": [11, 9, 11], "faces": {"north": {"texture": "#gezicht"}, "south": {"texture": "#deksel"},
                                                             "west": {"texture": "#deksel"}, "east": {"texture": "#deksel"},
                                                             "up": {"texture": "#deksel"}, "down": {"texture": "#deksel"}}},
             el([5, 9, 6.5], [6.5, 11, 8], "#deksel"), el([9.5, 9, 6.5], [11, 11, 8], "#deksel"),
             el([7.5, 9, 7.5], [8.5, 10, 8.5], "#goud"),
             el([7, 2, 2], [9, 4.5, 4], "#pot"), el([7, 3.5, 0.5], [9, 5.5, 2], "#pot"),
             el([7, 1.5, 12], [9, 2.5, 14.5], "#pot"), el([7, 4.5, 12], [9, 5.5, 14.5], "#pot"), el([7, 1.5, 13.5], [9, 5.5, 14.5], "#pot")]
    w(f"{A}/models/block/theepotje.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": pot_tex,
                                           "elements": potje})
    w(f"{A}/blockstates/theepotje.json", {"variants": {f"facing={f}": {"model": "guhs:block/theepotje", **({"y": r} if r else {})}
                                                       for f, r in ROT.items()}})
    w(f"{A}/models/item/theepotje.json", {"parent": "guhs:block/theepotje"})
    for i in TEAS + ["feest_theeservies"]:
        h.item_model(i)
    for blk in ("theetafel", "theepotje"):
        h.self_drop(blk)
    h.shaped("theetafel", ["CCC", " P ", " P "], {"C": "minecraft:white_carpet", "P": "#minecraft:planks"}, "guhs:theetafel", 1)
    h.shaped("theepotje", ["TKT", "TTT"], {"T": "minecraft:white_terracotta", "K": "guhs:kaas_knabbels"}, "guhs:theepotje", 1)
    add = h.add_tag
    add("minecraft/tags/block/mineable/axe", ["guhs:theetafel"])
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:theepotje"])
    add("guhs/tags/item/knus/thee", [f"guhs:{t}" for t in TEAS])
    add("guhs/tags/item/knus/theeservies", ["guhs:feest_theeservies"])


def theeguh(h):
    """Mevrouw Theelepel: a mint-green sitting guh with a flowery hat, a string of pearls and a big teaspoon."""
    geo_file = kn._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_theeguh"
    sw = kn._swatches(geo, ["hoed", "hoedband", "bloem", "parel", "lepel"])
    geo["bones"].append({"name": "theeguh_hoed", "parent": "head", "pivot": [0, 13, 0], "cubes": [
        kn._cube([-5.8, 25.6, -6.2], [11.6, 0.8, 11], sw["hoed"]),
        kn._cube([-3.8, 26.4, -4.6], [7.6, 3.2, 7.6], sw["hoed"]),
        kn._cube([-3.8, 26.4, -4.6], [7.6, 1.0, 7.6], sw["hoedband"], inflate=0.12),
        kn._cube([2.6, 26.6, -5.2], [2.2, 2.2, 0.6], sw["bloem"]),
        kn._cube([1.4, 27.0, -5.1], [1.4, 1.4, 0.5], sw["bloem"])]})
    geo["bones"].append({"name": "theeguh_parels", "parent": "body", "pivot": [0, 11, -4], "cubes": [
        kn._cube([-3.6 + 1.2 * i, 10.6 - (0.5 if 1 <= i <= 5 else 0) - (0.4 if 2 <= i <= 4 else 0), -4.9], [0.8, 0.8, 0.6], sw["parel"])
        for i in range(7)]})
    geo["bones"].append({"name": "theeguh_lepel", "parent": "body", "pivot": [5.4, 5, -4.4], "cubes": [
        kn._cube([5.1, 5.0, -4.9], [0.6, 5.6, 0.6], sw["lepel"]), kn._cube([4.7, 10.4, -5.2], [1.4, 1.9, 1.2], sw["lepel"])]})
    kn._save_geo(h, "guh_npc_theeguh.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.42, sat=0.38, val=1.02, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2841)
    kn._paint(a, sw["hoed"], (206, 170, 226), rng, 6)
    kn._paint(a, sw["hoedband"], (255, 250, 246), rng, 4)
    kn._paint(a, sw["bloem"], (246, 110, 170), rng, 8)
    kn._paint(a, sw["parel"], (250, 248, 240), rng, 4)
    kn._paint(a, sw["lepel"], (212, 216, 226), rng, 8)
    h.save(Image.fromarray(a), "entity", "npc_theeguh.png")


SOUNDS = {
    "theehuis.inschenken": [{"name": "minecraft:item.bottle.empty", "type": "event", "pitch": 0.9},
                            {"name": "minecraft:block.bubble_column.upwards_ambient", "type": "event", "volume": 0.6}],
    "theehuis.kopjes_klink": [{"name": "minecraft:block.amethyst_block.hit", "type": "event", "pitch": 1.6},
                              {"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.8, "volume": 0.5}],
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


QUEST_ADVANCEMENTS = ["theehuis_theelepel", "theehuis_thee_gezet", "theehuis_gezellig", "theehuis_zelfgebakken", "theehuis_theesoorten",
                      "seen_theeguh"]
TAB = [
    ("theehuis_gezellig", "stadje", "guhs:theetafel", "task", "Gezellig!",
     "Houd een gezellig theekransje met je tamme guhs in het Knabbelthee-huisje"),
    ("theehuis_zelfgebakken", "theehuis_gezellig", "guhs:theepotje", "goal", "Zelfgebakken!",
     "Serveer op een gezellig theekransje iets uit de Knabbelbakkerij. Mmm, VAHOEG!"),
    ("theehuis_theesoorten", "theehuis_gezellig", "guhs:guhbloementhee", "goal", "Theeproever",
     "Zet of drink alle vier de theesoorten"),
]


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, title, desc in TAB:
        h.w(f"{D}/advancement/knuffeldal/{name}.json", {
            "parent": f"guhs:knuffeldal/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                        "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})
        h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
        h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


THEE_TEKST = {
    "knabbelthee": ("Knabbelthee", "Thee van kaasknabbels: goudbruin, knapperig van smaak en een beetje gezellig.",
                    "De huisthee van Mevrouw Theelepel: gezet van kaasknabbels in een theepotje. Maakt je een halve minuut gezellig."),
    "kaasmelkthee": ("Kaasmelkthee", "Romig en zacht. Spoelt alle nare effecten weg!",
                     "Thee met kaasmelk van de Guhboerderij. Zo romig dat alle nare effecten ervan wegsmelten."),
    "theekruidthee": ("Theekruidthee", "Groen en fris, van theekruid uit je tuintje. Je knapt er even van op.",
                      "Theekruid uit je eigen tuintje in een theepotje: frisse groene thee waar je even van opknapt."),
    "guhbloementhee": ("Guhbloementhee", "Roze thee van guhbloemetjes. Zo gezellig, anderhalve minuut lang!",
                       "Guhbloemetjes geven roze thee die ruikt naar de lente. De gezelligste thee van allemaal."),
}

LANG = {
    "block.guhs.theetafel": "Theetafel",
    "block.guhs.theetafel.lore": "Een ronde tafel met een bloemetjeskleed. Zet er guh_stoelen omheen voor een theekransje.",
    "block.guhs.theepotje": "Theepotje",
    "block.guhs.theepotje.lore": "Doe er iets lekkers in (kaasknabbels, kaasmelk, theekruid of guhbloemetjes): twee kopjes thee!",
    "item.guhs.feest_theeservies": "Feest-theeservies",
    "item.guhs.feest_theeservies.lore": "Het mooiste theeservies van Mevrouw Theelepel, geleend voor het Grote Knusfeest. Voorzichtig!",
    "item.guhs.theemutsje": "Theemutsje",
    "effect.guhs.gezellig": "Gezellig",
    "subtitles.guhs.theehuis.inschenken": "Thee wordt ingeschonken",
    "subtitles.guhs.theehuis.kopjes_klink": "Kopjes klinken",
    "entity.guhs.guh_npc.theeguh": "Mevrouw Theelepel",
    "gui.guhs.guhdex.rarity.theeguh": "Zeldzaamheid: Uniek (Knabbelthee-huisje, Knuffeldal)",
    "gui.guhs.guhdex.info.theeguh": "Schenkt thee voor iedereen die even vads wil zitten. Ze roert altijd met haar grote theelepel en "
                                    "kent elke theesoort. Neem je tamme guhs mee voor een theekransje: gezellig!",
    # her lines
    "quest.guhs.theeguh.hoi.0": "Welkom in het Knabbelthee-huisje, lieverd! Een kopje thee? Of een heel theekransje?",
    "quest.guhs.theeguh.hoi.1": "Ach, wat gezellig dat je er bent. Heb je je guhs meegenomen? Dan dekken we de tafel!",
    "quest.guhs.theeguh.hoi.2": "Een guh is nooit te vads voor nog een kopje thee. Alleen misschien niet vahoeg genoeg! Hihi.",
    "quest.guhs.theeguh.bezig": "Er is nu een theekransje bezig, lieverd. Kom straks gezellig terug!",
    "quest.guhs.theeguh.bezig_jij": "Je theekransje is bezig! Kijk boven de koppies van je guhs wat ze willen.",
    "quest.guhs.theeguh.geen_tafel": "Njeg, waar is mijn theetafel? Zonder tafel geen theekransje.",
    "quest.guhs.theeguh.geen_guhs": "Ik zie geen tamme guhs van jou! Neem ze mee naar binnen, dan houden we een theekransje.",
    "quest.guhs.theeguh.start": "Ga maar lekker zitten, allemaal! (%s gasten) Schenk thee als iemand thee wil, en serveer iets lekkers "
                                "als iemand trek heeft. Gebak uit de Knabbelbakkerij is het állerlekkerst!",
    "quest.guhs.theeguh.huisthee": "Hier, %s kopjes knabbelthee van het huis. Meer thee zet je in een theepotje.",
    "quest.guhs.theeguh.gezellig": "Wat een gezellige tafel! Iedereen is helemaal knus. Kom gauw weer eens langs, lieverd.",
    "quest.guhs.theeguh.theemutsje": "Omdat het je eerste gezellige theekransje was: een theemutsje voor je guh! Staat vast schattig.",
    "quest.guhs.theeguh.theeservies": "Voor het Grote Knusfeest? Dan leen ik je mijn allermooiste feest-theeservies. Breng het naar "
                                      "Burgemeester Vadsema, en pas op voor Kruimel-Mika's!",
    "quest.guhs.theeguh.bijna": "Het was best knus (%s van de %s), maar nog niet echt gezellig. Volgende keer nog meer thee!",
    "quest.guhs.theeguh.gestopt": "Oké lieverd, dan ruim ik de kopjes op. Tot de volgende keer!",
    # her screen
    "gui.guhs.theehuis.knop.start": "Theekransje houden",
    "gui.guhs.theehuis.knop.start.tooltip": "Je tamme guhs (tot 6) gaan aan de theetafel zitten: schenk thee en serveer iets lekkers",
    "gui.guhs.theehuis.knop.stop": "Theekransje stoppen",
    "gui.guhs.theehuis.knop.doei": "Doei!",
    "gui.guhs.theehuis.scherm.uitleg": "Zin in een theekransje? %s van je tamme guhs kunnen aanschuiven. Schenk thee, serveer iets lekkers "
                                       "en kletsen maar. Een gezellige tafel maakt iedereen knus!",
    "gui.guhs.theehuis.scherm.feest": "Het Grote Knusfeest! Na een gezellig theekransje leen ik je mijn feest-theeservies. %s van je "
                                      "tamme guhs kunnen aanschuiven.",
    "gui.guhs.theehuis.scherm.geen_guhs": "Ik zie geen tamme guhs van jou in de buurt, lieverd. Neem ze mee, dan dekken we de tafel!",
    "gui.guhs.theehuis.scherm.bezig": "%s houdt nu een theekransje. Kom straks gezellig terug!",
    "gui.guhs.theehuis.scherm.bezig_jij": "Je theekransje is bezig: gezelligheid %s van de %s.",
    "gui.guhs.theehuis.scherm.kransjes": "Gezellige theekransjes: %s   Theesoorten: %s/4",
    "gui.guhs.theehuis.scherm.tip": "Tip: thee zet je in een theepotje.",
    # during a kransje
    "gui.guhs.theehuis.bar": "Gezelligheid: %s / %s  ·  nog %s s",
    "gui.guhs.theehuis.wil_thee": "%s wil graag een kopje thee!",
    "gui.guhs.theehuis.wil_gebak": "%s heeft trek in iets lekkers!",
    "gui.guhs.theehuis.kletst": "%s zit gezellig te kletsen.",
    "gui.guhs.theehuis.heeft_al": "%s heeft nog genoeg, dankjewel! Njeg.",
    "gui.guhs.theehuis.gaat_zitten": "%s gaat eerst even zitten...",
    "gui.guhs.theehuis.niet_jouw": "Dit is het theekransje van %s.",
    "gui.guhs.theehuis.vergeten": "%s wilde nog iets... nou ja, het kletsen is ook fijn.",
    "gui.guhs.theehuis.lekker": "%s: mmm! +%s gezelligheid",
    "gui.guhs.theehuis.lekker_bijzonder": "%s: wat een bijzondere thee! +%s gezelligheid",
    "gui.guhs.theehuis.lekker_zelfgebakken": "%s: ZELFGEBAKKEN! VAHOEG! +%s gezelligheid",
    "gui.guhs.theehuis.gezet": "%s kopjes %s gezet!",
    "gui.guhs.theehuis.theepotje.leeg": "Doe er iets lekkers in: kaasknabbels, kaasmelk, theekruid of guhbloemetjes.",
    # the Knus tab
    "gui.guhs.knus.mijlpaal.theehuis_eerste_kransje": "Je eerste gezellige theekransje",
    "gui.guhs.knus.mijlpaal.theehuis_kransjes": "5 gezellige theekransjes",
    "gui.guhs.knus.mijlpaal.theehuis_zelfgebakken": "5 keer zelfgebakken geserveerd",
    "gui.guhs.knus.mijlpaal.theehuis_ingeschonken": "30 kopjes ingeschonken",
    "gui.guhs.knus.mijlpaal.theehuis_gezet": "20 kopjes thee gezet",
    "gui.guhs.knus.mijlpaal.theehuis_theesoorten": "Alle 4 theesoorten",
    "gui.guhs.knus.verzameling.theesoorten": "Theesoorten",
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for tid, (naam, lore, info) in THEE_TEKST.items():
        h.lang(f"item.guhs.{tid}", naam, naam)
        h.lang(f"item.guhs.{tid}.lore", lore, lore)
        h.lang(f"gui.guhs.knus.theesoorten.{tid}", naam, naam)
        h.lang(f"gui.guhs.knus.theesoorten.{tid}.info", info, info)


TEST_TAFELS = [(6, 6), (7, 6), (6, 7), (7, 7)]
TEST_STOELEN = [((5, 6), "east"), ((5, 7), "east"), ((8, 6), "west"), ((8, 7), "west"), ((6, 5), "south"), ((7, 5), "south"),
                ((6, 8), "north"), ((7, 8), "north")]


def test_templates(h):
    """theehuis_test_kamer (15 x 6 x 15): four tea tables in the middle, eight chairs round them, a teapot in a corner."""
    s = h.Structure((15, 6, 15))
    for x in range(15):
        for z in range(15):
            s.set(x, 0, z, "guhs:knuffelklinkers")
    for (x, z) in TEST_TAFELS:
        s.set(x, 1, z, "guhs:theetafel", {"gedekt": "false"})
    for (x, z), f in TEST_STOELEN:
        s.set(x, 1, z, "guhs:guh_stoel", {"facing": f})
    s.set(2, 1, 2, "guhs:theepotje", {"facing": "south"})
    s.save("theehuis_test_kamer")


def selfcheck_assets(h):
    A = h.A
    missing = []
    for b in ("theetafel", "theepotje"):
        if not os.path.exists(f"{A}/blockstates/{b}.json") or not os.path.exists(f"{A}/models/item/{b}.json") or f"block.guhs.{b}" not in h.NL:
            missing.append(f"block {b}")
    for i in TEAS + ["feest_theeservies"]:
        if not os.path.exists(f"{A}/textures/item/{i}.png") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for f in ("geo/entity/guh_npc_theeguh.geo.json", "textures/entity/npc_theeguh.png", "textures/mob_effect/gezellig.png"):
        if not os.path.exists(f"{A}/{f}"):
            missing.append(f)
    if missing:
        raise SystemExit(f"theehuis assets missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    theeguh(h)
    sounds(h)
    advancements(h)
    texts(h)
    b = bouw.build_and_check(h)
    test_templates(h)
    selfcheck_assets(h)
    print(f"theehuis: building ok ({len(b.s.blocks)} blocks, {b.gezichten} small + {b.grote_gezichten} big guh faces)")


def ftb(fq):
    q, y = fq.q, FTB_Y
    q("theehuis_theelepel", "Mevrouw Theelepel", "Aan de oostkant van het plein van het Knuffeldal staat een reuzentheepot met een "
      "guhkop als deksel: het &dKnabbelthee-huisje&r. Praat met &dMevrouw Theelepel&r.", "guhs:theepotje", [fq.adv("theehuis_theelepel")],
      rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y, shape="circle", xp=100)
    q("theehuis_thee_zetten", "Thee zetten", "Doe iets lekkers in een theepotje (rechtsklik): kaasknabbels geven knabbelthee. Met kaasmelk, "
      "theekruid of guhbloemetjes krijg je andere theesoorten.", "guhs:knabbelthee", [fq.adv("theehuis_thee_gezet")],
      rewards=(("guhs:kaas_knabbels", 8),), x=-6.5, y=y, xp=100)
    q("theehuis_gezellig", "Gezellig!", "Neem je tamme guhs mee en houd een theekransje. Ze gaan aan de theetafel zitten en kletsen; "
      "schenk thee en serveer iets lekkers als ze dat willen. Een gezellige tafel maakt iedereen &dgezellig&r!",
      "guhs:theetafel", [fq.adv("theehuis_gezellig")], rewards=(("guhs:guhbloementhee", 2),), x=-5, y=y, shape="gear", xp=250)
    q("theehuis_zelfgebakken", "Zelfgebakken!", "Serveer op een theekransje gebak uit de Knabbelbakkerij. Daar worden guhs extra blij van!",
      "guhs:theepotje", [fq.adv("theehuis_zelfgebakken")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-3.5, y=y, xp=200)
    q("theehuis_theesoorten", "Theeproever", "Zet of drink alle vier de theesoorten: knabbelthee, kaasmelkthee, theekruidthee en "
      "guhbloementhee. Ze staan in je Guhdex (tab Knus).", "guhs:guhbloementhee", [fq.adv("theehuis_theesoorten")],
      rewards=(("guhs:theepotje", 1),), x=-2, y=y, shape="gear", xp=250)
    q("theehuis_theemutsje", "Een theemutsje", "Na je eerste gezellige theekransje geeft Mevrouw Theelepel je een theemutsje. Zet het "
      "je guh op!", "guhs:theemutsje", [fq.item("guhs:theemutsje")], rewards=(("guhs:knabbelthee", 4),), x=-0.5, y=y, xp=100)
    q("theehuis_guhdex", "Mevrouw Theelepel in je Guhdex", "Ga vlak bij Mevrouw Theelepel staan, dan staat ze in je Guhdex.", "guhs:guhdex",
      [fq.adv("seen_theeguh")], rewards=(("guhs:kaas_knabbels", 4),), x=1, y=y, shape="rsquare", xp=100)
