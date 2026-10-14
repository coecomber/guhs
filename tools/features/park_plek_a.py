"""
1.4.2 Het Knabbelpark, plekjes A (Java: feature/park/plek; English: tools/lang/en/c142_park_a.json; design:
guhs_work142/DESIGN_142.md 9 and 9a): five random plekjes of the park, each a rigid piece of the pool knabbelpark/plekjes
(registered in park_bouw.PLEKJES; the park module saves the templates):

  eendenvijver   guh-eendjes with their rijtjes kuikentjes (feed them: kwak-njeg!), a jetty with a rowing boat always ready
  speeltuin      slide, two swings, a seesaw (park_glijbaan, park_schommel, park_wip: also craftable and for sale) and a
                 sandbox where every player digs up one kaasknabbel a day (park_zandbak)
  picknickweide  picnic blankets (park_picknickkleed) and the kraampje of the kraampjesguh: treats and a playground for vads
  muziektuin     flower beds, a small kas and the muziektent: click the jukebox, the three guhs on the floor dance
  japanse_tuin   a red arched bridge over a brook into a koi pond with Knabbelkoi's (a Guhdex bonus page), a zen corner

  park_plek_a_bouw.py       the templates
  park_plek_a_modellen.py   the block models and textures, the Knabbelkoi's texture
  park_plek_a_wiki.py       the wiki paragraphs (not in FEATURES)
  here                      every Dutch text, recipes and loot, hidden advancements, the Guhdex page, the FTB quests, the
                            self-check against the Java
"""
import os
import re

from features import bbq2
from features import park_plek_a_bouw as bouw
from features import park_plek_a_modellen as modellen

NAME = "park_plek_a"
FTB_CHAPTER = "guhs_minigames"
FTB_SECTIES = [("park_plekjes", "Plekjes in het Knabbelpark", "npc:kraamguh", None)]
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "park", "plek")
# the dancers of the muziektent (PlekA.DANSER_VACHT)
DANSER_VACHT = ["mint", "choco", "rainbow"]
VERBORGEN = ["parkplek_eendjes", "parkplek_glijbaan", "parkplek_schommel", "parkplek_wip", "parkplek_zandbak", "parkplek_kraampje",
             "parkplek_muziek", "parkplek_knabbelkoi"]


def plekken():
    """The spots of PlekAPlekken.java: {NAAM: (x, y, z)}, {NAAM_YAW: f}, plus EENDEN, DANSERS, KOIEN."""
    src = open(os.path.join(JAVA, "PlekAPlekken.java"), encoding="utf-8").read()
    out = {}
    for m in re.finditer(r"(\w+) = new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)", src):
        out[m.group(1)] = tuple(int(m.group(i)) for i in (2, 3, 4))
    for m in re.finditer(r"(\w+_YAW) = (-?[\d.]+)f", src):
        out[m.group(1)] = float(m.group(2))
    out["EENDEN"] = [out["EEND_1"], out["EEND_2"]]
    out["DANSERS"] = [out["DANSER_1"], out["DANSER_2"], out["DANSER_3"]]
    out["KOIEN"] = [out["KOI_1"], out["KOI_2"], out["KOI_3"], out["KOI_4"]]
    return out


def templates(h):
    """(park_bouw.templates) the five plekjes."""
    return bouw.templates(h, plekken())


# =====================================================================================================================
# texts (Dutch; the English is in tools/lang/en/c142_park_a.json)
# =====================================================================================================================
TEXTS = {
    # blocks and the seat
    "block.guhs.park_glijbaan": "Glijbaan",
    "block.guhs.park_glijbaan.lore": "Leg ze als een trapje naar beneden: één blok omlaag per blok vooruit. Stap erin en... wieee!",
    "block.guhs.park_schommel": "Schommel",
    "block.guhs.park_schommel.lore": "Hang hem onder een balk, twee blokken hoger. Rechtsklik en schommelen maar, vanzelf!",
    "block.guhs.park_wip": "Wip",
    "block.guhs.park_wip.lore": "Een wip voor twee. Rechtsklik aan de kant waar je wilt zitten: op en neer, op en neer!",
    "block.guhs.park_zandbak": "Speelzand",
    "block.guhs.park_picknickkleed": "Picknickkleed",
    "block.guhs.park_picknickkleed.lore": "Rood-wit geruit, met piepkleine guh-hartjes. Lekker op het gras met een theetje.",
    "entity.guhs.park_zitje": "Zitje",
    # the slide, the sandbox
    "gui.guhs.parkplek.glijbaan": "Wieeeee!",
    "gui.guhs.parkplek.zandbak.knabbel": "Je graaft en graaft... en vindt een KAASKNABBEL! Morgen ligt er vast weer eentje. Njeg!",
    "gui.guhs.parkplek.zandbak.vondst.0": "Je vindt... zand. Heel veel zand.",
    "gui.guhs.parkplek.zandbak.vondst.1": "Een schelpje! O nee, het is een kiezeltje.",
    "gui.guhs.parkplek.zandbak.vondst.2": "Je vindt een verloren sokje van een guh. Njeg?",
    "gui.guhs.parkplek.zandbak.vondst.3": "Een briefje: 'Morgen weer graven. Groetjes, de zandbak.'",
    "gui.guhs.parkplek.zandbak.vondst.4": "Je bouwt een prachtig zandkasteeltje. Vahoeg!",
    "gui.guhs.parkplek.zandbak.vondst.5": "Een kruimeltje kaasknabbel! Al opgegeten door een guh. Sorry.",
    # the eendjes
    "gui.guhs.parkplek.eendjes.0": "Kwak-njeg! Het eendje smult.",
    "gui.guhs.parkplek.eendjes.1": "Kwak kwak! De kuikentjes huppelen van blijdschap.",
    "gui.guhs.parkplek.eendjes.2": "Smikkel smikkel! Wat een vadsig eendje wordt dat.",
    # the muziektent
    "gui.guhs.parkplek.muziek.aan": "De platenkast van het park kiest een plaatje. Dansen maar!",
    "gui.guhs.parkplek.muziek.uit": "Muziek uit. De guhs puffen even uit. Njeg.",
    # the Knabbelkoi
    "gui.guhs.parkplek.koi.plons": "Plons! De Knabbelkoi springt van blijdschap uit het water.",
    "gui.guhs.parkplek.koi.emmer": "Njeg! De Knabbelkoi's wonen hier. Niet in een emmer, hoor.",
    # the kraampjesguh
    "quest.guhs.parkplek.kraam.hallo.0": "Welkom bij mijn kraampje! Lekkers voor je picknick, en je betaalt met vads. Njeg!",
    "quest.guhs.parkplek.kraam.hallo.1": "Een taartje bij de thee? Of een wip voor je eigen tuin? Kijk maar rond!",
    "quest.guhs.parkplek.kraam.hallo.2": "Kaasknabbels, koekjes, appeltjes... alles vers en lekker vadsig!",
    "quest.guhs.parkplek.kraam.hallo.3": "Psst... de schommels hier zijn de beste van de hele Guhmensie. Ik verkoop ze ook!",
}
KRAAMGUH = "Kraampjesguh"
GUHDEX = ("knabbelkoi", "Knabbelkoi", "zeldzaam (alleen in de japanse tuin van het Knabbelpark)",
          "Een koi in kaasknabbelkleuren, met gaatjes en al. De Knabbelkoi's zwemmen in de koivijver van de japanse tuin in het "
          "Knabbelpark, een knipoog naar de kois van het Klaterdal. Geef er een een kaasknabbel of koivoer: plons!")


def texts(h):
    for k, v in TEXTS.items():
        h.lang(k, v, v)
    bbq2.stub_npc(h, "kraamguh", KRAAMGUH, hue=0.98, sat=0.85, val=0.95)
    id_, naam, zeldzaam, info = GUHDEX
    bbq2.pagina(h, id_, naam, zeldzaam, info)
    for name in VERBORGEN:
        bbq2.verborgen(h, name)


def recepten_en_loot(h):
    h.shaped("park_glijbaan", ["  P", " P ", "P  "], {"P": "minecraft:pink_concrete"}, "guhs:park_glijbaan", 3)
    h.shaped("park_schommel", ["C C", "C C", "SSS"], {"C": "minecraft:chain", "S": "minecraft:cherry_slab"}, "guhs:park_schommel", 1)
    h.shaped("park_wip", ["SSS", " F "], {"S": "minecraft:cherry_slab", "F": "minecraft:cherry_fence"}, "guhs:park_wip", 1)
    h.shaped("park_picknickkleed", ["RW", "WR"], {"R": "minecraft:red_carpet", "W": "minecraft:white_carpet"}, "guhs:park_picknickkleed", 4)
    for b in ("park_glijbaan", "park_schommel", "park_wip", "park_picknickkleed"):
        h.self_drop(b)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:park_glijbaan"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:park_schommel", "guhs:park_wip"])


# =====================================================================================================================
# FTB quests (section "Plekjes in het Knabbelpark" of Minigames & bijzondere plekken; nothing locked, nothing needed)
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    kn = "guhs:kaas_knabbels"
    q("parkplek_eendjes", "Kwak-njeg!", "In de &deendenvijver&r van het Knabbelpark zwemmen guh-eendjes met hun kuikentjes in een rijtje. "
      "Geef ze brood, zaadjes of een kaasknabbel. Bij het steigertje ligt altijd een &6roeibootje&r klaar: roeien mag!",
      "minecraft:bread", [adv("parkplek_eendjes")], rewards=((kn, 6),), deps=["park_gevonden"], xp=20)
    q("parkplek_speeltuin", "Speeltuin De Vrolijke Vads", "In de &dspeeltuin&r van het Knabbelpark: glij van de glijbaan, ga op een "
      "schommel zitten en wip op de wip (rechtsklik). Wil je er zelf een? Het kraampje op de picknickweide verkoopt ze!",
      "guhs:park_schommel", [adv("parkplek_glijbaan"), adv("parkplek_schommel"), adv("parkplek_wip")], rewards=((kn, 8),),
      deps=["park_gevonden"], xp=30)
    q("parkplek_zandbak", "Zandbakschat", "Graaf in de &6zandbak&r van de speeltuin (rechtsklik op het zand). Eén keer per dag vind "
      "je een kaasknabbel, de rest van de dag... nou ja, zand.", "minecraft:sand", [adv("parkplek_zandbak")], rewards=((kn, 4),),
      deps=["park_gevonden"], xp=20)
    q("parkplek_kraampje", "Picknick!", "Op de &dpicknickweide&r staat het kraampje van de kraampjesguh. Hij verkoopt lekkers, "
      "picknickkleden en zelfs een speeltuin voor thuis. Betalen doe je met vads.", "guhs:park_picknickkleed",
      [adv("parkplek_kraampje")], rewards=((kn, 4),), deps=["park_gevonden"], xp=20)
    q("parkplek_muziek", "Dansfeest in de muziektent", "Klik in de &dmuziektuin&r op de jukebox in de muziektent: de platenkast "
      "van het park kiest een plaatje en de guhs gaan dansen. Een eigen plaatje mag ook!", "minecraft:jukebox",
      [adv("parkplek_muziek")], rewards=((kn, 6),), deps=["park_gevonden"], xp=20)
    q("parkplek_knabbelkoi", "Knabbelkoi's", "In de koivijver van de &djapanse tuin&r zwemmen Knabbelkoi's: kois in "
      "kaasknabbelkleuren. Loop over het rode bruggetje en geef er eentje een kaasknabbel of koivoer. Plons!", "guhs:koivoer",
      [adv("parkplek_knabbelkoi")], rewards=((kn, 6),), deps=["park_gevonden"], xp=30)


# =====================================================================================================================
# build + self-check
# =====================================================================================================================
def selfcheck(h):
    problems = []
    java = ""
    for root, _, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java"):
                java += open(os.path.join(root, f), encoding="utf-8").read()
    for m in re.finditer(r'"((?:gui|quest)\.guhs\.parkplek[\w.]*)"', java):
        k = m.group(1)
        if k.endswith("."):
            continue
        if k not in TEXTS:
            problems.append(f"Java names {k}, no Dutch text")
    for k in [f"gui.guhs.parkplek.zandbak.vondst.{i}" for i in range(6)] + [f"gui.guhs.parkplek.eendjes.{i}" for i in range(3)] \
            + [f"quest.guhs.parkplek.kraam.hallo.{i}" for i in range(4)]:
        if k not in TEXTS:
            problems.append(f"no text {k}")
    if "VONDSTEN = 6" not in java or "GROETEN = 4" not in java:
        problems.append("SpeelBlokken.VONDSTEN / Kraamguh.GROETEN changed: check the texts")
    vacht = re.search(r"DANSER_VACHT = List\.of\(([^)]*)\)", java)
    if not vacht or [v.strip().split(".")[-1].lower() for v in vacht.group(1).split(",")] != DANSER_VACHT:
        problems.append("PlekA.DANSER_VACHT differs from park_plek_a.DANSER_VACHT")
    for adv in VERBORGEN:
        if f'"{adv}"' not in java:
            problems.append(f"hidden advancement {adv} is never granted")
    for naam in bouw.BOUWERS:
        if f'"knabbelpark/{naam}"' not in java:
            problems.append(f"PlekAPlekken has no template name knabbelpark/{naam}")
    problems += bouw.check(templates(h))
    if problems:
        raise SystemExit("park_plek_a: not right:\n  " + "\n  ".join(problems))


def build(h):
    selfcheck(h)
    texts(h)
    recepten_en_loot(h)
    modellen.build(h)
