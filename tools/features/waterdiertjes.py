"""
3.0 (Guhverhalen), slice waterdiertjes: de water- en insectdiertjes van de Guhmensie (DESIGN_30 §6).

  - guhxolotl           a guh that is an axolotl (5 colours: roze, mint, choco, wit, rare goud), tameable with a guhvisje,
                        scooped into a guhxolotl_emmertje (item icon per colour), a piep-maatje (menu "Blubbeltjes!",
                        guhhuisje resident); spawns in the shallow water of the Guhzee and the Kaasmoeras
  - guh_eendje          a mama duck with a rijtje kuikentjes (ponds, the Guhzee coasts, the Kaasmoeras)
  - knabbelvlindertje   butterflies around flowers by day (4 colours)
  - glimguhtje          glowing fireflies at night over the Kaasmoeras and the Guhweides
  - lieveheersbeestje   ladybirds with guh spots that help guhtuintjes grow a tikje

build(h) writes the models/animations/textures (waterdiertjes_modellen), the item models + icons, sounds.json (the OGGs are
made once by waterdiertjes_geluid.py and committed), the biome modifiers (spawns), the advancements (tab guhs:diertjes/ +
hidden quest ones), the game test template and all texts (Dutch; en_us gets the same Dutch). ftb(fq) = the section "Water &
insectjes" of the chapter guhs_diertjes.
"""
import os

from PIL import Image

from features import waterdiertjes_modellen as modellen

BONES = {}
CLOTHES = []
FTB_PORTRAIT = "geo:guhxolotl:guhxolotl_roze"

IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
PAGINAS = ["guhxolotl", "guh_eendje", "knabbelvlindertje", "glimguhtje", "lieveheersbeestje"]
KLEUREN = ["roze", "mint", "choco", "wit", "goud"]
QUEST = ["waterdiertjes_goud", "waterdiertjes_rijtje", "waterdiertjes_tuinhulp", "waterdiertjes_blubbeltjes", "waterdiertjes_alle_kleurtjes"]
SOUNDS = {
    "waterdiertjes.guhxolotl_blub": ["blub1", "blub2", "blub3"],
    "waterdiertjes.eendje_kwak": ["kwak1", "kwak2", "kwak3"],
    "waterdiertjes.kuiken_piep": ["piep1", "piep2", "piep3"],
    "waterdiertjes.glimguhtje_ting": ["ting1", "ting2"],
    "waterdiertjes.lieveheersbeestje_zoem": ["zoem"],
}


# =====================================================================================================================
# item icons: the guhxolotl-emmertje in five colours (a water bucket with a little guhxolotl peeking out)
# =====================================================================================================================
def emmertje_icon(h, kleur):
    fur, buik, kieuw, tip, oor = modellen.XOLOTL[kleur]
    img = h.vanilla("item/water_bucket").copy()
    px = img.load()
    donker = tuple(int(c * 0.72) for c in fur) + (255,)
    f = tuple(fur) + (255,)
    # the head: rows 2-6, x 4-11 (a dark rim), sitting in the water of the bucket
    for y in range(2, 7):
        for x in range(4, 12):
            rim = x in (4, 11) or y == 2
            px[x, y] = donker if rim and not (y == 2 and 6 <= x <= 9) else f
    for x in range(5, 11):
        px[x, 2] = f
    for x in range(6, 10):
        px[x, 1] = donker
    # two round guh ears
    for (x, y) in ((4, 1), (5, 1), (10, 1), (11, 1)):
        px[x, y] = donker
    px[5, 0] = donker
    px[10, 0] = donker
    px[5, 1] = tuple(oor) + (255,)
    px[10, 1] = tuple(oor) + (255,)
    # fluffy gills: three plumes each side
    for (x, y) in ((3, 2), (2, 1), (3, 4), (2, 4), (1, 4), (3, 6), (2, 7)):
        px[x, y] = tuple(kieuw) + (255,)
    for (x, y) in ((12, 2), (13, 1), (12, 4), (13, 4), (14, 4), (12, 6), (13, 7)):
        px[x, y] = tuple(kieuw) + (255,)
    for (x, y) in ((1, 0), (0, 4), (1, 8), (14, 0), (15, 4), (14, 8)):
        px[x, y] = tuple(tip) + (255,)
    # the glossy guh eyes: dark with a white shine on top, blue below
    for ex in (6, 9):
        px[ex, 3] = (18, 18, 34, 255)
        px[ex, 4] = (70, 150, 220, 255)
    px[6, 3] = (255, 255, 255, 255)
    px[9, 3] = (255, 255, 255, 255)
    px[5, 3] = (18, 18, 34, 255)
    px[10, 3] = (18, 18, 34, 255)
    px[5, 4] = (40, 100, 200, 255)
    px[10, 4] = (40, 100, 200, 255)
    # blush and the snoet
    px[5, 5] = (255, 140, 180, 255)
    px[10, 5] = (255, 140, 180, 255)
    px[7, 5] = (214, 96, 150, 255)
    px[8, 5] = (214, 96, 150, 255)
    if kleur == "goud":
        for (x, y) in ((7, 2), (4, 13), (11, 9)):
            px[x, y] = (255, 250, 214, 255)
    return img


def items(h):
    A = h.A
    for k in KLEUREN:
        h.save(emmertje_icon(h, k), "item", f"guhxolotl_emmertje_{k}.png")
        h.item_model(f"guhxolotl_emmertje_{k}")
    h.w(f"{A}/models/item/guhxolotl_emmertje.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/guhxolotl_emmertje_roze"},
        "overrides": [{"predicate": {"guhs:kleur": i / 4}, "model": f"guhs:item/guhxolotl_emmertje_{k}"} for i, k in enumerate(KLEUREN) if i]})
    for p in PAGINAS:
        h.w(f"{A}/models/item/{p}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def sounds(h):
    def patch(d):
        for event, files in SOUNDS.items():
            d[event] = {"sounds": [f"guhs:waterdiertjes/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# spawns (NeoForge biome modifiers)
# =====================================================================================================================
SPAWNS = {
    "waterdiertjes_guhxolotl": (["guhs:guh_sea", "guhs:kaasmoeras"], "guhs:guhxolotl", 10, 1, 3),
    "waterdiertjes_eendjes": (["guhs:guh_sea", "guhs:diepe_guhzee", "guhs:kaasmoeras", "guhs:guh_meadows", "guhs:guh_fields", "guhs:knuffeldal"],
                              "guhs:guh_eendje", 10, 1, 1),
    "waterdiertjes_vlindertjes": (["guhs:guh_fields", "guhs:pink_puffs", "guhs:guh_meadows", "guhs:knuffeldal", "guhs:kaas_flats", "guhs:guhwaii"],
                                  "guhs:knabbelvlindertje", 12, 1, 3),
    "waterdiertjes_glimguhtjes": (["guhs:kaasmoeras", "guhs:guh_meadows"], "guhs:glimguhtje", 16, 2, 5),
    "waterdiertjes_lieveheersbeestjes": (["guhs:guh_fields", "guhs:guh_meadows", "guhs:pink_puffs", "guhs:knuffeldal", "guhs:kaas_flats"],
                                         "guhs:lieveheersbeestje", 8, 1, 2),
}


def spawns(h):
    h.add_tag("minecraft/tags/entity_type/can_breathe_under_water", ["guhs:guhxolotl"])   # (gills!)
    for name, (biomes, entity, weight, lo, hi) in SPAWNS.items():
        h.w(f"{h.D}/neoforge/biome_modifier/{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
                                                           "spawners": [{"type": entity, "weight": weight, "minCount": lo, "maxCount": hi}]})


# =====================================================================================================================
# advancements: the Diertjes tab (guhs:diertjes/waterdiertjes_*) and the hidden quest ones
# =====================================================================================================================
def advancements(h):
    from features import verhaal
    D = h.D
    for name in QUEST:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    for p in PAGINAS:
        h.w(f"{D}/advancement/quest/seen_{p}.json", {"criteria": IMPOSSIBLE})
    tame = {"done": {"trigger": "minecraft:tame_animal", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "guhs:guhxolotl"}}]}}}
    emmer = {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:guhxolotl_emmertje"}]}}}
    h.w(f"{D}/advancement/quest/waterdiertjes_getemd.json", {"criteria": tame})
    h.w(f"{D}/advancement/quest/waterdiertjes_emmertje.json", {"criteria": emmer})
    z = verhaal.zichtbaar
    t = "diertjes"
    z(h, t, "waterdiertjes_guhxolotl", "root", "guhs:guhxolotl_spawn_egg", "task", "Blub-njeg!",
      "Zet een guhxolotl in je Guhdex: een guh die een axolotl is, met pluizige kieuwtjes")
    z(h, t, "waterdiertjes_goud", "waterdiertjes_guhxolotl", "minecraft:gold_nugget", "challenge", "Goud waard",
      "Zie een zeldzame gouden guhxolotl van dichtbij")
    z(h, t, "waterdiertjes_getemd", "waterdiertjes_guhxolotl", "guhs:guh_vis", "goal", "Mijn eigen blubje",
      "Tem een guhxolotl met een guhvisje", criteria=tame)
    z(h, t, "waterdiertjes_emmertje", "waterdiertjes_guhxolotl", "guhs:guhxolotl_emmertje", "task", "Een emmertje vol liefde",
      "Schep een guhxolotl in een emmertje", criteria=emmer)
    z(h, t, "waterdiertjes_alle_kleurtjes", "waterdiertjes_getemd", "minecraft:axolotl_bucket", "challenge", "Alle blubkleurtjes",
      "Tem een roze, een mint, een choco, een witte én een gouden guhxolotl")
    z(h, t, "waterdiertjes_guh_eendje", "root", "guhs:guh_eendje_spawn_egg", "task", "Kwak-njeg!",
      "Zet een guh-eendje in je Guhdex")
    z(h, t, "waterdiertjes_rijtje", "waterdiertjes_guh_eendje", "minecraft:feather", "task", "Allemaal in een rijtje",
      "Zie een mama-eendje met minstens drie kuikentjes achter zich aan")
    z(h, t, "waterdiertjes_knabbelvlindertje", "root", "guhs:knabbelvlindertje_spawn_egg", "task", "Fladder-de-fladder",
      "Zet een knabbelvlindertje in je Guhdex")
    z(h, t, "waterdiertjes_glimguhtje", "root", "guhs:glimguhtje_spawn_egg", "task", "Lichtjes in de nacht",
      "Zet een glimguhtje in je Guhdex (ze komen pas 's nachts)")
    z(h, t, "waterdiertjes_lieveheersbeestje", "root", "guhs:lieveheersbeestje_spawn_egg", "task", "Stipjes met oortjes",
      "Zet een lieveheersbeestje in je Guhdex")
    z(h, t, "waterdiertjes_tuinhulp", "waterdiertjes_lieveheersbeestje", "guhs:guh_bloempot", "task", "Lieve helpertjes",
      "Zie een lieveheersbeestje je guhtuintje een tikje sneller laten groeien")
    z(h, t, "waterdiertjes_alle", "root", "minecraft:lily_pad", "goal", "Blub, kwak, fladder, glim!",
      "Zet alle vijf water- en insectdiertjes in je Guhdex")


# =====================================================================================================================
# texts (Dutch, also in en_us)
# =====================================================================================================================
TEXTS = {
    "entity.guhs.guhxolotl": "Guhxolotl",
    "entity.guhs.guh_eendje": "Guh-eendje",
    "entity.guhs.knabbelvlindertje": "Knabbelvlindertje",
    "entity.guhs.glimguhtje": "Glimguhtje",
    "entity.guhs.lieveheersbeestje": "Lieveheersbeestje",
    "item.guhs.guhxolotl_spawn_egg": "Guhxolotl-spawnei",
    "item.guhs.guh_eendje_spawn_egg": "Guh-eendje-spawnei",
    "item.guhs.knabbelvlindertje_spawn_egg": "Knabbelvlindertje-spawnei",
    "item.guhs.glimguhtje_spawn_egg": "Glimguhtje-spawnei",
    "item.guhs.lieveheersbeestje_spawn_egg": "Lieveheersbeestje-spawnei",
    "item.guhs.guhxolotl_emmertje": "Guhxolotl-emmertje",
    "item.guhs.guhxolotl_emmertje.met": "Emmertje met een %s guhxolotl",
    "item.guhs.guhxolotl_emmertje.tooltip": "Rechtsklik op een blok: plons, daar zwemt hij weer! (Sluipen: zonder water)",
    "item.guhs.guhxolotl_emmertje.getemd": "Jouw eigen blubje, njeg",
    "item.guhs.guhxolotl_emmertje.kleintje": "Nog een kleintje",
    "gui.guhs.waterdiertjes.kleur.roze": "roze",
    "gui.guhs.waterdiertjes.kleur.mint": "mint",
    "gui.guhs.waterdiertjes.kleur.choco": "choco",
    "gui.guhs.waterdiertjes.kleur.wit": "witte",
    "gui.guhs.waterdiertjes.kleur.goud": "GOUDEN",
    # the piep-maatje texts (menu, picking up)
    "gui.guhs.piep.opgepakt.guhxolotl": "%s zit veilig in een emmertje. Blub!",
    "gui.guhs.piep.neergezet.guhxolotl": "Plons! %s zwemt weer vrolijk rond.",
    "gui.guhs.piep.menu.speciaal.guhxolotl": "Blubbeltjes!",
    "gui.guhs.piep.menu.speciaal.guhxolotl.tooltip": "Je guhxolotl blaast een wolk blubbeltjes om je heen: twee minuten lang kun je onder water ademen, njeg!",
    "gui.guhs.piep.menu.sub.guhxolotl": "Een guh die een axolotl is. Blub-njeg!",
    "gui.guhs.waterdiertjes.getemd": "%s blubt blij: jullie zijn nu vriendjes! Blub-njeg!",
    "gui.guhs.waterdiertjes.droog": "%s is een beetje droog en wil graag terug het water in...",
    "gui.guhs.waterdiertjes.blubbeltjes": "%s blaast een wolk blubbeltjes om je heen: je kunt even onder water ademen!",
    "gui.guhs.waterdiertjes.blub_rust": "Nog even bijblubben... (%s:%s)",
    "gui.guhs.waterdiertjes.alle_kleurtjes": "Alle blubkleurtjes! Roze, mint, choco, wit én GOUD. Wat een VAHOEGE verzameling, njeg!",
    # sounds
    "subtitles.guhs.waterdiertjes.guhxolotl_blub": "Guhxolotl blubt",
    "subtitles.guhs.waterdiertjes.eendje_kwak": "Guh-eendje kwaakt",
    "subtitles.guhs.waterdiertjes.kuiken_piep": "Kuikentje piept",
    "subtitles.guhs.waterdiertjes.glimguhtje_ting": "Glimguhtje tingelt",
    "subtitles.guhs.waterdiertjes.lieveheersbeestje_zoem": "Lieveheersbeestje zoemt",
    # the Guhdex pages
    "gui.guhs.guhdex.rarity.guhxolotl": "Zeldzaamheid: Vaak (ondiep water van de Guhzee en het Kaasmoeras). Goud: heel zeldzaam!",
    "gui.guhs.guhdex.info.guhxolotl": "Een guh die een axolotl is: een ronde guhkop, guhoogjes, een snoetje, blosjes en roze pluizige kieuwtjes. Er zijn er in roze, mint, choco, wit en (heel zeldzaam) GOUD. Tem hem met een guhvisje en schep hem in een emmertje. Speelt dood als je hem pijn doet (foei!).",
    "gui.guhs.guhdex.rarity.guh_eendje": "Zeldzaamheid: Vaak (vijvers, de Guhzee en het Kaasmoeras)",
    "gui.guhs.guhdex.info.guh_eendje": "Een mama-eendje met guhoortjes en een rijtje pluizige gele kuikentjes achter zich aan. Elk kuikentje volgt het kuikentje voor zich. Kwak-njeg! Lust zaadjes, brood en kaasknabbels.",
    "gui.guhs.guhdex.rarity.knabbelvlindertje": "Zeldzaamheid: Vaak (overdag, rond bloemen)",
    "gui.guhs.guhdex.info.knabbelvlindertje": "Een vlindertje met een piepklein guhkopje en vleugels vol knabbelstipjes. In kaasgeel, roze, mint en lila. Fladdert van bloem naar bloem en slaapt 's nachts op een bloemetje.",
    "gui.guhs.guhdex.rarity.glimguhtje": "Zeldzaamheid: Alleen 's nachts (Kaasmoeras en Guhweides)",
    "gui.guhs.guhdex.info.glimguhtje": "Een piepklein rond guhtje met een lampje in zijn buik. 's Nachts zweven ze in dromerige rondjes boven het gras en het water. Bij zonsopgang glimmen ze zachtjes weg.",
    "gui.guhs.guhdex.rarity.lieveheersbeestje": "Zeldzaamheid: Vaak (bij bloemen en guhtuintjes)",
    "gui.guhs.guhdex.info.lieveheersbeestje": "Een lieveheersbeestje met stipjes in de vorm van guhkopjes. Het komt graag op je guhtuintjes zitten en laat ze dan een tikje sneller groeien. Lief helpertje!",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# game test template (WaterdiertjesGameTests): 14 x 14 grass with a pool in one corner
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((14, 6, 14))
    for x in range(14):
        for z in range(14):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("waterdiertjes_test_wei")


def selfcheck(h):
    problems = modellen.check(h)
    for key, text in TEXTS.items():
        low = text.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {key}")
    for p in PAGINAS:
        for k in (f"entity.guhs.{p}", f"gui.guhs.guhdex.rarity.{p}", f"gui.guhs.guhdex.info.{p}", f"item.guhs.{p}_spawn_egg"):
            if k not in TEXTS:
                problems.append(f"missing text {k}")
    for files in SOUNDS.values():
        for f in files:
            if not os.path.exists(os.path.join(h.A, "sounds", "waterdiertjes", f + ".ogg")):
                problems.append(f"missing sound waterdiertjes/{f}.ogg (run tools/features/waterdiertjes_geluid.py)")
    if problems:
        raise SystemExit("waterdiertjes self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    items(h)
    sounds(h)
    spawns(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: "Water & insectjes" (chapter guhs_diertjes; laid out automatically, nothing locked)
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("waterdiertjes_guhxolotl", "Blub-njeg!",
      "In het ondiepe water van de &bGuhzee&r en het &eKaasmoeras&r zwemmen &dguhxolotls&r: guhs die axolotls zijn, met pluizige kieuwtjes. "
      "Zwem er dichtbij voor je Guhdex!", "guhs:guhxolotl_spawn_egg", [adv("seen_guhxolotl")], rewards=(("guhs:guh_vis", 4),))
    q("waterdiertjes_temmen", "Mijn eigen blubje",
      "Geef een guhxolotl een &6guhvisje&r (1 op 3 kans). Getemd volgt hij je door water en over land, en klik je hem aan voor zijn menuutje.",
      "guhs:guh_vis", [adv("waterdiertjes_getemd")], rewards=(("guhs:kaas_knabbels", 16),))
    q("waterdiertjes_emmertje", "Een emmertje vol liefde",
      "Schep een guhxolotl op met een &bemmer water&r (of sluip-klik je eigen guhxolotl met een lege hand). Zet hem neer waar je wilt: plons! "
      "Hij kan ook in een guhhuisje wonen, daar droogt hij nooit uit.", "guhs:guhxolotl_emmertje", [adv("waterdiertjes_emmertje")])
    q("waterdiertjes_blubbeltjes", "Blubbeltjes!",
      "Druk in het menuutje van je guhxolotl op &bBlubbeltjes!&r: hij blaast een wolk bubbels om je heen en je kunt twee minuten onder water ademen.",
      "minecraft:heart_of_the_sea", [adv("waterdiertjes_blubbeltjes")], rewards=(("guhs:guh_vis", 8),))
    q("waterdiertjes_goud", "Goud waard",
      "Heel af en toe is een guhxolotl &6GOUD&r, met glinsterende stipjes. Zie er een van dichtbij (of fok er een: kleintjes zijn soms goud!).",
      "minecraft:gold_nugget", [adv("waterdiertjes_goud")], rewards=(("guhs:gouden_kaasknabbel", 1),), shape="gear", xp=200)
    q("waterdiertjes_alle_kleurtjes", "Alle blubkleurtjes",
      "Tem een &droze&r, een &amint&r, een &6choco&r, een &fwitte&r én een &egouden&r guhxolotl. Wat een VAHOEGE verzameling!",
      "minecraft:axolotl_bucket", [adv("waterdiertjes_alle_kleurtjes")], rewards=(("guhs:guh_kristal", 8),), shape="gear", xp=300)
    q("waterdiertjes_eendje", "Kwak-njeg!",
      "Op vijvers en langs de Guhzee peddelen &fguh-eendjes&r. Kom dichtbij voor je Guhdex!", "guhs:guh_eendje_spawn_egg",
      [adv("seen_guh_eendje")])
    q("waterdiertjes_rijtje", "Allemaal in een rijtje",
      "Een mama-eendje heeft een rijtje &epluizige kuikentjes&r achter zich aan: elk kuikentje volgt het kuikentje voor zich. "
      "Zie er een met minstens drie kuikentjes!", "minecraft:feather", [adv("waterdiertjes_rijtje")], rewards=(("minecraft:bread", 4),))
    q("waterdiertjes_vlindertje", "Fladder-de-fladder",
      "Overdag fladderen &eknabbelvlindertjes&r van bloem naar bloem. Loop er zachtjes naartoe (niet rennen!) voor je Guhdex.",
      "guhs:knabbelvlindertje_spawn_egg", [adv("seen_knabbelvlindertje")])
    q("waterdiertjes_glimguhtje", "Lichtjes in de nacht",
      "Als het donker is, zweven er &aglimguhtjes&r boven het Kaasmoeras en de Guhweides: piepkleine guhtjes met een lampje in hun buik.",
      "guhs:glimguhtje_spawn_egg", [adv("seen_glimguhtje")], rewards=(("minecraft:glow_berries", 4),))
    q("waterdiertjes_lieveheersbeestje", "Stipjes met oortjes",
      "Een &clieveheersbeestje&r met guhkopjes als stipjes! Je vindt ze bij bloemen en bij je guhtuintjes.",
      "guhs:lieveheersbeestje_spawn_egg", [adv("seen_lieveheersbeestje")])
    q("waterdiertjes_tuinhulp", "Lieve helpertjes",
      "Heb je een guhtuintje met iets erin dat groeit? Dan komt er soms een lieveheersbeestje op zitten, en dan groeit het een &atikje sneller&r. "
      "Zie het gebeuren!", "guhs:guh_bloempot", [adv("waterdiertjes_tuinhulp")], rewards=(("guhs:knabbelzaadjes", 8),))
