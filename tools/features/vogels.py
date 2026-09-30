"""
3.0 (Guhverhalen), slice vogels: De vogeltjes van de Guhmensie (DESIGN_30 §6): pluisvinkjes, kaasmeesjes, guh-uiltjes en
zeemeeuwtjes, plus het pluisveertje (the Hemelkapelletje quest asks one) and the vogelvoerhuisje (a bird table: seeds on it
and the birds come to eat; the pluisvinkjes lose a feather there now and then).

build(h) writes: the models/textures/animations (vogels_modellen.py), the item + block resources (pluisveertje icon, spawn
eggs, the voerhuisje with 5 fill levels), the feather particle, sounds.json (the OGGs are made by vogels_geluid.py and
committed), loot tables, the recipe, the food tags, the spawns (biome modifiers vogels_*.json), the Guhdex texts, the
advancements (tab guhs:diertjes/), all texts (Dutch, also in en_us) and the game test template vogels_test_wei.
ftb(fq) adds the Vogeltjes section of the Diertjes chapter.
"""
import os

from PIL import Image

from features import verhaal
from features import vogels_modellen

BONES = {}
CLOTHES = []
FTB_PORTRAIT = "geo:pluisvinkje:pluisvinkje"

BIRDS = ("pluisvinkje", "kaasmeesje", "guh_uiltje", "zeemeeuwtje")
SOUNDS = {"vogels.pluisvinkje": ["pluisvinkje1", "pluisvinkje2", "pluisvinkje3"], "vogels.kaasmeesje": ["kaasmeesje1", "kaasmeesje2"],
          "vogels.guh_uiltje": ["guh_uiltje1", "guh_uiltje2"], "vogels.zeemeeuwtje": ["zeemeeuwtje1", "zeemeeuwtje2"],
          "vogels.mijn_mijn": ["mijn_mijn1", "mijn_mijn2"], "vogels.fladder": ["fladder1", "fladder2"]}
SUBTITLES = {"vogels.pluisvinkje": "Pluisvinkje tjilpt: tjiep-tjiep!", "vogels.kaasmeesje": "Kaasmeesje zingt: tsjie-tsjie-bee",
             "vogels.guh_uiltje": "Guh-uiltje roept: oehoe... njeg!", "vogels.zeemeeuwtje": "Zeemeeuwtje krijst: kliew!",
             "vogels.mijn_mijn": "Zeemeeuwtje: Mijn! Mijn!", "vogels.fladder": "Vleugeltjes fladderen"}

# the spawns (CONTRACT_30 §5.2): biome -> weight, group size
SPAWNS = {
    "pluisvinkje": (["guhs:guh_fields", "guhs:pink_puffs", "guhs:guh_meadows"], 14, 3, 6),
    "kaasmeesje": (["guhs:vadswoud", "guhs:kaas_flats"], 12, 1, 3),
    "guh_uiltje": (["guhs:guh_peaks", "guhs:vadswoud"], 5, 1, 1),
    "zeemeeuwtje": (["guhs:guh_sea", "guhs:diepe_guhzee", "guhs:guhwaii"], 12, 2, 4),
}

PAGES = {
    "pluisvinkje": ("Pluisvinkje", "Vaak, in groepjes, in de Guhvelden, de Roze pluisjes en de Guhweides",
                    "Een rond roze-wit bolletje pluis met guhoortjes! Pluisvinkjes vliegen in groepjes achter hun leidertje aan en "
                    "zitten graag in de guhbloesembomen. Soms dwarrelt er een zacht pluisveertje af, vooral als je ze zaadjes geeft. "
                    "Sluip dichterbij, anders fladdert het hele groepje weg. Tjiep-njeg!"),
    "kaasmeesje": ("Kaasmeesje", "In het Vadswoud en de Kaasvlakte",
                   "Een geel meesje met een kaasstreep (met gaatjes!) op zijn buik en een lila guhpetje. Hangt het liefst "
                   "ondersteboven onder de blaadjes en pikt aan rijpe knabbelbessen (hij laat ze voor jou hangen, lief he?). "
                   "Geef hem bessen en hij zingt: tsjie-tsjie-bee!"),
    "guh_uiltje": ("Guh-uiltje", "Zeldzaam, in de Guhpieken en het Vadswoud ('s nachts wakker)",
                   "Overdag slaapt het guh-uiltje dik opgepluisd op een takje. 's Nachts gaan zijn oogjes open en gloeien ze in "
                   "het donker. Zijn oortjes zijn echte guhoortjes, en zijn kopje draait helemaal rond om je in de gaten te houden. "
                   "Oehoe... njeg!"),
    "zeemeeuwtje": ("Zeemeeuwtje", "Langs de Guhzee, de Diepe Guhzee en op Guhwai'i",
                    "Mijn! Mijn! Een brutaal maar lief zeemeeuwtje. Hou een visje (of brood) vast en het hele strand komt boven je "
                    "hoofd rondcirkelen. Een visje op de grond? Weg is het! Maar je andere spulletjes laten ze gewoon liggen. "
                    "Dobbert graag op de golfjes."),
}

LANG = {
    "item.guhs.pluisveertje": "Pluisveertje",
    "item.guhs.pluisveertje.lore": "Zo zacht als een wolkje. Van een Pluisvinkje!",
    "item.guhs.pluisveertje.lore2": "De Wolkenhoeder van het Hemelkapelletje zoekt er een. Rechtsklik: pfff!",
    "block.guhs.vogels_voerhuisje": "Vogelvoerhuisje",
    "block.guhs.vogels_voerhuisje.lore": "Leg er zaadjes op en de vogeltjes komen smullen. Pluisvinkjes laten er soms een veertje achter!",
    "gui.guhs.vogels.voerhuisje.gevuld": "Zaadjes in het voerhuisje: %s van %s. Smullen maar, vogeltjes!",
    "gui.guhs.vogels.voerhuisje.vol": "Het voerhuisje zit al helemaal vol. Njeg!",
    "gui.guhs.vogels.mijn_mijn": "Zeemeeuwtje: Mijn! Mijn!",
    "item.guhs.pluisvinkje_spawn_egg": "Pluisvinkje-spawnei",
    "item.guhs.kaasmeesje_spawn_egg": "Kaasmeesje-spawnei",
    "item.guhs.guh_uiltje_spawn_egg": "Guh-uiltje-spawnei",
    "item.guhs.zeemeeuwtje_spawn_egg": "Zeemeeuwtje-spawnei",
}

# advancements (tab guhs:diertjes/): name -> (parent, icon, frame, title, description, criteria or None = granted by code)
ADV = {
    "vogels_pluisvinkje": ("root", "guhs:pluisveertje", "task", "Tjiep-njeg!", "Zet een Pluisvinkje in je Guhdex", None),
    "vogels_kaasmeesje": ("root", "guhs:kaasmeesje_spawn_egg", "task", "Kaas met een streepje", "Zet een Kaasmeesje in je Guhdex", None),
    "vogels_guh_uiltje": ("root", "guhs:guh_uiltje_spawn_egg", "task", "Wie is daar zo laat nog op?", "Zet een Guh-uiltje in je Guhdex", None),
    "vogels_zeemeeuwtje": ("root", "guhs:zeemeeuwtje_spawn_egg", "task", "Een brutaal snaveltje", "Zet een Zeemeeuwtje in je Guhdex", None),
    "vogels_pluisveertje": ("vogels_pluisvinkje", "guhs:pluisveertje", "task", "Zo zacht als een wolkje",
                            "Vind een pluisveertje van een Pluisvinkje",
                            {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:pluisveertje"}]}}}),
    "vogels_voeren": ("root", "minecraft:wheat_seeds", "task", "Vogelvoer", "Geef een vogeltje zijn lievelingshapje", None),
    "vogels_voerhuisje": ("vogels_voeren", "guhs:vogels_voerhuisje", "task", "Smullen maar!", "Leg zaadjes in een vogelvoerhuisje", None),
    "vogels_sluipen": ("root", "minecraft:leather_boots", "task", "Stil maar...", "Sluip tot vlak bij een vogeltje zonder dat hij wegvliegt", None),
    "vogels_ondersteboven": ("vogels_kaasmeesje", "minecraft:oak_leaves", "task", "Ondersteboven!",
                             "Zie een Kaasmeesje ondersteboven onder de blaadjes hangen", None),
    "vogels_oehoe": ("vogels_guh_uiltje", "minecraft:clock", "task", "Oehoe... njeg!", "Hoor een Guh-uiltje roepen in de nacht", None),
    "vogels_mijn": ("vogels_zeemeeuwtje", "minecraft:cod", "task", "Mijn! Mijn!", "Laat een Zeemeeuwtje \"Mijn!\" roepen om jouw visje", None),
    "vogels_alle": ("root", "minecraft:feather", "challenge", "Vogelkijker", "Zet alle vier de vogeltjes van de Guhmensie in je Guhdex", None),
}


def build(h):
    vogels_modellen.build(h)
    items(h)
    voerhuisje(h)
    particle(h)
    sounds(h)
    loot(h)
    tags(h)
    spawns(h)
    advancements(h)
    texts(h)
    test_templates(h)
    problems = selfcheck(h)
    if problems:
        raise SystemExit("vogels:\n  " + "\n  ".join(problems))


# =====================================================================================================================
# items, the bird feeder, the particle
# =====================================================================================================================
def feather_icon():
    """The pluisveertje: a curved, fluffy pink-white feather with a darker quill."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rows = [
        "..........##....",
        ".........#ww#...",
        "........#wwpp#..",
        ".......#wwppp#..",
        "......#wwpppp#..",
        ".....#wwppppq#..",
        "....#wwpppp q#..",
        "...#wwppppq#....",
        "..#wwpppp q#....",
        "..#wpppp q#.....",
        ".#wpppq q#......",
        ".#wppq  #.......",
        ".#wpqq#.........",
        "..#qq#..........",
        "..q.............",
        ".q..............",
    ]
    pal = {"#": (214, 110, 160, 255), "w": (255, 250, 252, 255), "p": (255, 196, 222, 255), "q": (190, 96, 140, 255),
           " ": (255, 220, 236, 255)}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                px[x, y] = pal[ch]
    return img


def items(h):
    h.save(feather_icon(), "item", "pluisveertje.png")
    h.item_model("pluisveertje")
    for b in BIRDS:
        h.w(f"{h.A}/models/item/{b}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def wood_tex(seed):
    import random
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = (244, 170, 196) if (y // 4) % 2 == 0 else (236, 156, 186)
            v = rng.randint(-8, 8)
            if x in (0, 15) or (y % 4 == 3):
                v -= 22
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base) + (255,))
    return img


def seeds_tex(seed):
    import random
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (196, 150, 90, 255))
    for _ in range(70):
        x, y = rng.randrange(16), rng.randrange(16)
        c = rng.choice([(236, 206, 120), (170, 120, 60), (250, 230, 160), (130, 150, 70), (255, 190, 210)])
        img.putpixel((x, y), c + (255,))
    return img


def el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down")):
    def uv(face):
        x0, y0, z0 = frm
        x1, y1, z1 = to
        if face in ("north", "south"):
            return [x0, 16 - y1, x1, 16 - y0]
        if face in ("east", "west"):
            return [z0, 16 - y1, z1, 16 - y0]
        return [x0, z0, x1, z1]
    return {"from": frm, "to": to, "faces": {f: {"texture": tex, "uv": uv(f)} for f in faces}}


def voerhuisje(h):
    """A little pink bird table: a post, a tray with a rim, a heart-shaped little sign in front, and 0-4 scoops of seeds."""
    h.save(wood_tex(301), "block", "vogels_voerhuisje_hout.png")
    h.save(seeds_tex(302), "block", "vogels_voerhuisje_zaad.png")
    tex = {"particle": "guhs:block/vogels_voerhuisje_hout", "hout": "guhs:block/vogels_voerhuisje_hout",
           "zaad": "guhs:block/vogels_voerhuisje_zaad", "poot": "minecraft:block/stripped_birch_log", "hart": "minecraft:block/red_wool"}
    base = [el([7, 0, 7], [9, 8, 9], "#poot"), el([5, 0, 5], [11, 1, 11], "#poot"),
            el([2, 8, 2], [14, 10, 14], "#hout"),
            el([2, 10, 2], [14, 11, 3], "#hout"), el([2, 10, 13], [14, 11, 14], "#hout"),
            el([2, 10, 3], [3, 11, 13], "#hout"), el([13, 10, 3], [14, 11, 13], "#hout"),
            # a little heart on the front of the tray (north)
            el([6.5, 8.5, 1.5], [7.5, 9.5, 2], "#hart", ("north", "up", "east", "west")),
            el([8.5, 8.5, 1.5], [9.5, 9.5, 2], "#hart", ("north", "up", "east", "west")),
            el([7, 7.5, 1.5], [9, 9, 2], "#hart", ("north", "down", "east", "west"))]
    for n in range(5):
        els = list(base)
        if n:
            els.append(el([3, 10, 3], [13, 10 + 0.25 * n, 13], "#zaad", ("up", "north", "south", "east", "west")))
            if n >= 3:
                els.append(el([5, 10 + 0.25 * n, 5], [11, 10.5 + 0.25 * n, 11], "#zaad", ("up", "north", "south", "east", "west")))
        h.w(f"{h.A}/models/block/vogels_voerhuisje_{n}.json", {"parent": "minecraft:block/block", "textures": tex, "elements": els})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    h.w(f"{h.A}/blockstates/vogels_voerhuisje.json", {"variants": {
        f"facing={f},voer={n}": ({"model": f"guhs:block/vogels_voerhuisje_{n}", "y": r} if r else {"model": f"guhs:block/vogels_voerhuisje_{n}"})
        for f, r in rot.items() for n in range(5)}})
    h.w(f"{h.A}/models/item/vogels_voerhuisje.json", {"parent": "guhs:block/vogels_voerhuisje_3"})
    h.self_drop("vogels_voerhuisje")
    h.shaped("vogels_voerhuisje", [" D ", "PSP", " T "],
             {"D": "minecraft:pink_dye", "P": "#minecraft:planks", "S": "minecraft:wheat_seeds", "T": "minecraft:stick"},
             "guhs:vogels_voerhuisje")


def particle(h):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    rows = ["......#.", ".....#p#", "....#pp#", "...#pp#.", "..#pw#..", ".#pw#...", ".#w#....", "q#......"]
    pal = {"#": (240, 150, 190, 255), "p": (255, 206, 226, 255), "w": (255, 250, 252, 255), "q": (200, 110, 150, 255)}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    h.save(img, "particle", "vogels_veertje.png")
    h.w(f"{h.A}/particles/vogels_veertje.json", {"textures": ["guhs:vogels_veertje"]})


def sounds(h):
    def patch(d):
        for event, files in SOUNDS.items():
            d[event] = {"sounds": [f"guhs:vogels/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def loot(h):
    for b in BIRDS:
        h.w(f"{h.D}/loot_table/entities/{b}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": "minecraft:feather", "functions": h.count_fn(0, 1)}]}]})


def tags(h):
    h.add_tag("guhs/tags/item/vogels/zaadjes", ["#c:seeds", "minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:pumpkin_seeds",
                                                "minecraft:beetroot_seeds", "minecraft:torchflower_seeds", "guhs:knabbelzaadjes",
                                                "guhs:kaasknabbelzaadjes", "guhs:guhbloemzaadjes", "guhs:theekruidzaadjes"])
    h.add_tag("guhs/tags/item/vogels/bessen", ["guhs:knabbelbessen", "minecraft:sweet_berries", "minecraft:glow_berries"])
    h.add_tag("guhs/tags/item/vogels/visjes", ["#minecraft:fishes", "guhs:guh_vis", "guhs:gebakken_guh_vis", "minecraft:bread"])
    h.add_tag("guhs/tags/item/vogels/uiltjeshapjes", ["guhs:kaas_knabbels"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:vogels_voerhuisje"])


def spawns(h):
    for b, (biomes, weight, lo, hi) in SPAWNS.items():
        h.w(f"{h.D}/neoforge/biome_modifier/vogels_{b}.json", {
            "type": "neoforge:add_spawns", "biomes": biomes,
            "spawners": [{"type": f"guhs:{b}", "weight": weight, "minCount": lo, "maxCount": hi}]})


def advancements(h):
    for name, (parent, icon, frame, title, desc, crit) in ADV.items():
        verhaal.zichtbaar(h, "diertjes", name, parent, icon, frame, title, desc, criteria=crit)


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for b, (name, rarity, info) in PAGES.items():
        h.lang(f"entity.guhs.{b}", name, name)
        h.lang(f"gui.guhs.guhdex.rarity.{b}", "Zeldzaamheid: " + rarity, "Zeldzaamheid: " + rarity)
        h.lang(f"gui.guhs.guhdex.info.{b}", info, info)
        h.w(f"{h.D}/advancement/quest/seen_{b}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for event, text in SUBTITLES.items():
        h.lang(f"subtitles.guhs.{event}", text, text)


def test_templates(h):
    """vogels_test_wei: 12 x 12 grass (floor at template y 0) with a little leaf roof in one corner (3 x 3 leaves at y 4 on
    a log, air under it: a kaasmeesje can hang there) and one sand + water strip for the gull."""
    t = h.Structure((12, 8, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    for y in range(1, 4):
        t.set(1, y, 1, "minecraft:oak_log", {"axis": "y"})
    for x in range(0, 3):
        for z in range(0, 3):
            t.set(x, 4, z, "minecraft:oak_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
    for z in range(12):
        t.set(11, 0, z, "minecraft:sand")
    t.save("vogels_test_wei")


# =====================================================================================================================
# FTB quests (chapter guhs_diertjes, section "Vogeltjes")
# =====================================================================================================================
def ftb(fq):
    adv = lambda name: fq.adv(f"guhs:diertjes/{name}")   # noqa: E731
    fq.q("vogels_pluisvinkje", "Pluisvinkjes",
         "In de &dGuhvelden&r, de &dRoze pluisjes&r en de Guhweides fladderen groepjes &dPluisvinkjes&r rond de guhbloesembomen. "
         "Sluip dichterbij (&7sneaken&r!), anders vliegt het hele groepje op. Zet er een in je Guhdex.",
         "guhs:pluisvinkje_spawn_egg", [fq.adv("seen_pluisvinkje")], rewards=(("guhs:kaas_knabbels", 8),))
    fq.q("vogels_pluisveertje", "Zo zacht als een wolkje",
         "Pluisvinkjes laten soms een &dpluisveertje&r vallen, vooral als je ze &ezaadjes&r geeft. De Wolkenhoeder van het "
         "Hemelkapelletje zoekt er een!",
         "guhs:pluisveertje", [fq.item("guhs:pluisveertje")], rewards=(("minecraft:wheat_seeds", 16),))
    fq.q("vogels_kaasmeesje", "Kaasmeesjes",
         "In het &2Vadswoud&r en de &eKaasvlakte&r wonen &eKaasmeesjes&r: geel, met een kaasstreep op hun buik. Zet er een in je Guhdex.",
         "guhs:kaasmeesje_spawn_egg", [fq.adv("seen_kaasmeesje")], rewards=(("guhs:kaas_knabbels", 8),))
    fq.q("vogels_ondersteboven", "Ondersteboven!",
         "Kaasmeesjes hangen graag &eondersteboven&r onder de blaadjes. Kijk maar eens omhoog in het bos!",
         "minecraft:oak_leaves", [adv("vogels_ondersteboven")], rewards=(("guhs:knabbelbessen", 6),))
    fq.q("vogels_guh_uiltje", "Guh-uiltjes",
         "Overdag slaapt het &6Guh-uiltje&r op een takje in de &7Guhpieken&r of het &2Vadswoud&r. 's Nachts gaan zijn oogjes "
         "gloeien. Zet er een in je Guhdex.",
         "guhs:guh_uiltje_spawn_egg", [fq.adv("seen_guh_uiltje")], rewards=(("guhs:kaas_knabbels", 8),))
    fq.q("vogels_oehoe", "Oehoe... njeg!",
         "Ga 's nachts op pad en luister: hoor je een &6Guh-uiltje&r roepen? Zijn kopje draait helemaal rond om jou te zien!",
         "minecraft:clock", [adv("vogels_oehoe")], rewards=(("guhs:kaas_knabbels", 12),))
    fq.q("vogels_zeemeeuwtje", "Zeemeeuwtjes",
         "Langs de &bGuhzee&r, de &9Diepe Guhzee&r en op &dGuhwai'i&r zitten &fZeemeeuwtjes&r op het strand of dobberen ze op "
         "de golfjes. Zet er een in je Guhdex.",
         "guhs:zeemeeuwtje_spawn_egg", [fq.adv("seen_zeemeeuwtje")], rewards=(("guhs:kaas_knabbels", 8),))
    fq.q("vogels_mijn", "Mijn! Mijn!",
         "Hou een &bvisje&r (of brood) vast bij de zeemeeuwtjes. Wat roepen ze dan? Geef er eentje een visje, dan is hij de "
         "blijste meeuw van de Guhzee.",
         "minecraft:cod", [adv("vogels_mijn")], rewards=(("minecraft:cod", 4),))
    fq.q("vogels_voerhuisje", "Smullen maar!",
         "Maak een &dvogelvoerhuisje&r en leg er zaadjes op (rechtsklik). Alle vogeltjes in de buurt komen smullen, en "
         "pluisvinkjes laten er soms een pluisveertje achter!",
         "guhs:vogels_voerhuisje", [adv("vogels_voerhuisje")], rewards=(("minecraft:wheat_seeds", 16),))
    fq.q("vogels_sluipen", "Stil maar...",
         "Vogeltjes schrikken als je aan komt stampen. &7Sneak&r tot vlak bij een vogeltje dat zit, zonder dat het wegvliegt.",
         "minecraft:leather_boots", [adv("vogels_sluipen")], rewards=(("guhs:kaas_knabbels", 8),))
    fq.q("vogels_alle", "Vogelkijker",
         "Zet &dalle vier&r de vogeltjes van de Guhmensie in je Guhdex: het pluisvinkje, het kaasmeesje, het guh-uiltje en het "
         "zeemeeuwtje. Tjiep-njeg!",
         "minecraft:spyglass", [adv("vogels_alle")], rewards=(("guhs:kaas_knabbels", 32),), shape="gear")


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    p = []
    for b in BIRDS:
        for f in (f"{h.A}/geckolib/models/entity/{b}.geo.json", f"{h.A}/geckolib/animations/entity/{b}.animation.json",
                  os.path.join(h.TEX, "entity", f"{b}.png"), os.path.join(h.TEX, "entity", f"{b}_dicht.png"),
                  f"{h.D}/neoforge/biome_modifier/vogels_{b}.json", f"{h.D}/loot_table/entities/{b}.json"):
            if not os.path.exists(f):
                p.append(f"missing {f}")
        for biome in SPAWNS[b][0]:
            if not os.path.exists(f"{h.D}/worldgen/biome/{biome.split(':')[1]}.json"):
                p.append(f"{b} spawns in a missing biome {biome}")
    for files in SOUNDS.values():
        for f in files:
            if not os.path.exists(f"{h.A}/sounds/vogels/{f}.ogg"):
                p.append(f"missing sound {f}.ogg (python tools/features/vogels_geluid.py)")
    for n in range(5):
        if not os.path.exists(f"{h.A}/models/block/vogels_voerhuisje_{n}.json"):
            p.append(f"missing voerhuisje model {n}")
    return p
