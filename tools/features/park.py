"""
1.4.2 Het Knabbelpark (Java: feature/park; English: tools/lang/en/c142_park.json; design: guhs_work142/DESIGN_142.md 9, 9a).

A park in the Guhmensie that grows like a vanilla village: the fountain plaza in the middle (one Reisguh "Knabbelpark"),
three REQUIRED pieces fixed to it (the beeldentuin with the three statues and their plaques, the hertenkooi with three
Hertguhtjes and the kooi-verzorger-guh, the parkwachter-guh's hut), park walks that follow the hills, and plekjes on
their sides. One guaranteed copy in terrain that does not exist yet, and a rare random spread; Superkompas tab "knus".

  park_bouw.py       the templates and pools; the PLEKJES API (conventions, connectors, example plekjes)
  park_modellen.py   the Hertguhtje's antlers and fur, the statues' stone and bronze, the blocks' models and textures
  park_wiki.py       the wiki entries (CONTRACT_130 2.4)
  here               the structure and its sets, every Dutch text, the questline texts, sounds, recipes, advancements,
                     the Guhdex page of the Hertguhtje, FTB quests, the game test floor, the self-check against the Java

BONES / variants are for tools/make_guh_variants.py.
"""
import json
import os
import re

import numpy as np

from features import bbq2, verhaal_motor, wereld
from features import park_bouw as bouw
from features import park_modellen as modellen

NAME = "park"
STRUCTUUR = "knabbelpark"
TYPE = "guhs:knabbelpark"
REISGUH_NAAM = "Knabbelpark"
SALT = 21420901
SPACING, SEPARATION = 56, 20          # rare: about one per 900 x 900 blocks where the land fits
GEGARANDEERD = dict(sector=11, min=900, max=2000)
SIZE, MAX_AFSTAND = 7, 72             # jigsaw depth and reach (72 + the beard's 12 stays well inside 128)
CHECK_RADIUS, VLAK = 40, 14
BIOMES = ["guh_fields", "knabbel_crumbs", "pink_puffs", "kaas_flats", "guh_meadows"]

BONES = modellen.BONES
CLOTHES = []
FTB_CHAPTER = "guhs_minigames"
FTB_SECTIES = [("park", "Het Knabbelpark", "npc:parkwachterguh", None)]

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "park")
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def variants(rng, v):
    return modellen.variants(v, np.random.default_rng(1420200))


def plekken():
    """The spots of ParkPlekken.java: {NAAM: (x, y, z)} and {NAAM_YAW: f}, plus HERTEN = [the three]."""
    src = open(os.path.join(JAVA, "ParkPlekken.java"), encoding="utf-8").read()
    out = {}
    for m in re.finditer(r"(\w+) = new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)", src):
        out[m.group(1)] = tuple(int(m.group(i)) for i in (2, 3, 4))
    for m in re.finditer(r"(\w+_YAW) = (-?[\d.]+)f", src):
        out[m.group(1)] = float(m.group(2))
    out["HERTEN"] = [out["HERT_1"], out["HERT_2"], out["HERT_3"]]
    return out


# =====================================================================================================================
# texts (Dutch; the English is in tools/lang/en/c142_park.json)
# =====================================================================================================================
PLAQUETTE_TEKST = {
    1: "Ter ere van Joostvadsje, zorgt altijd goed voor trapguh.",
    2: "Ter ere van Liekguh en Lunde, bewakers van alle guhs",
    3: "Ter ere van Opa Kaasguh, die per ongeluk op een kaasje ging zitten en zo de eerste kaasknabbel uitvond. Njeg.",
}

TEXTS = {
    # names
    "structure.guhs.knabbelpark": "Het Knabbelpark",
    "structure.guhs.knabbelpark.tooltip": "Een groot roze park met een fontein, beelden, Hertguhtjes en de parkwachter-guh (Guhmensie)",
    "entity.guhs.guh_npc.parkwachterguh": "Parkwachter-guh",
    "entity.guhs.guh_npc.kooiverzorgerguh": "Kooi-verzorger-guh",
    "block.guhs.park_bankje": "Parkbankje",
    "block.guhs.park_bankje.lore": "Met twee guh-oortjes op de leuning. Even zitten, even niks",
    "block.guhs.park_lantaarn": "Guhlantaarn",
    "block.guhs.park_lantaarn.lore": "Zet hem op twee kersenhouten hekpaaltjes: een lantaarnpaal zoals in het Knabbelpark",
    "block.guhs.park_prullenbak": "Guhprullenbak",
    "block.guhs.park_prullenbak.lore": "Gooi er iets in (rechtsklik) en hij eet het op. NJEG! Mooie spullen spuugt hij uit",
    "block.guhs.park_plaquette": "Plaquette",
    "block.guhs.park_beeld": "Beeld",
    "block.guhs.park_eigen_beeld": "Beeldje van je guh",
    "block.guhs.park_eigen_beeld.lore": "Rechtsklik er een van je eigen guhs mee: hij gaat poseren en het beeldje gaat op hem lijken",
    "block.guhs.park_eigen_beeld.van": "Een beeldje van %s",
    "subtitles.guhs.park.njeg": "Prullenbak: njeg!",
    # the litter bin, the plaques, the statues, the progress lines
    "gui.guhs.park.prullenbak.0": "NJEG! De prullenbak smakt tevreden.",
    "gui.guhs.park.prullenbak.1": "Hap! Lekker opgeruimd, njeg.",
    "gui.guhs.park.prullenbak.2": "De prullenbak boert zachtjes. Pardon!",
    "gui.guhs.park.prullenbak.3": "Mmm, nog eentje? Njeg njeg!",
    "gui.guhs.park.prullenbak.te_mooi": "Njeg? Dat is veel te mooi om weg te gooien! De prullenbak spuugt het weer uit.",
    "gui.guhs.park.plaquette.naam": "Plaquette",
    "gui.guhs.park.plaquette.1": PLAQUETTE_TEKST[1],
    "gui.guhs.park.plaquette.2": PLAQUETTE_TEKST[2],
    "gui.guhs.park.plaquette.3": PLAQUETTE_TEKST[3],
    "gui.guhs.park.beeld.joostvadsje": "Joostvadsje, in brons. Hij kijkt heel trots naar trapguh.",
    "gui.guhs.park.beeld.trapguh": "Trapguh, klein en in brons, op het trapje van de sokkel. Zo schattig!",
    "gui.guhs.park.beeld.liekguh": "Liekguh, in wit steen, met haar gouden neusringetje.",
    "gui.guhs.park.beeld.lunde": "Lunde, in wit steen, met een hartje op zijn voorhoofd. Hij past op alle guhs.",
    "gui.guhs.park.beeld.opa_kaasguh": "Opa Kaasguh, in oud brons, op zijn beroemde kaasje.",
    "gui.guhs.park.gelezen": "Plaquettes gelezen: %s van de %s",
    "gui.guhs.park.gevoerd": "Hertguhtjes gevoerd: %s van de %s",
    "gui.guhs.park.al_gevoerd": "Dit Hertguhtje heeft al gesmuld. Voer een ander!",
    "gui.guhs.park.poseert": "%s poseert! Het beeldje lijkt nu precies op hem.",
    "gui.guhs.park.nodig.plaquettes": "Plaquettes gelezen",
    "gui.guhs.park.nodig.herten": "Hertguhtjes gevoerd",
    "gui.guhs.park.beloning.hertguhtje": "Je eigen Hertguhtje",
    "quest.guhs.reis.vaste_naam": "%s hoort bij deze plek, met deze naam. Die blijft zo!",
    # the parkwachter-guh
    "quest.guhs.park.wachter.hallo": "Welkom in het Knabbelpark, njeg! Ik ben de parkwachter. Ik pas op de bloemen, de bankjes en de "
                                     "Hertguhtjes. Heb je zin om me een dagje te helpen?",
    "quest.guhs.park.wachter.park": "Dit park is gemaakt ter ere van een paar hele bijzondere guhs. Hun beelden staan in de beeldentuin. "
                                    "En achterin wonen de Hertguhtjes: guhs met piepkleine geweitjes. Vahoeg!",
    "quest.guhs.park.wachter.start": "Fijn! Begin maar in de beeldentuin: lees de drie plaquettes bij de beelden. Dan weet je wie we "
                                     "hier allemaal eren.",
    "quest.guhs.park.wachter.plaquettes": "Heb je de plaquettes al gelezen? Je hebt er %s van de %s. Ze staan voor de beelden.",
    "quest.guhs.park.wachter.herten": "De Hertguhtjes hebben trek! Geef ze elk een kaasknabbel, over het hek van de hertenkooi.",
    "quest.guhs.park.wachter.verzorger": "De kooi-verzorger kan wel een handje gebruiken. Ze zit bij de hooibalen naast de hertenkooi.",
    "quest.guhs.park.wachter.klaar": "Je hebt de beelden geëerd, de Hertguhtjes gevoerd en de verzorger geholpen. Wat een parkdag! "
                                     "En weet je wat? Een Hertguhtje wil heel graag met je mee...",
    "quest.guhs.park.wachter.beloning": "Hier is je eigen Hertguhtje! Zorg goed voor hem, net zoals Joostvadsje voor trapguh. En dit "
                                        "beeldje is voor jouw guh: laat hem maar poseren. Njeg!",
    "quest.guhs.park.wachter.later0": "Mooi weer in het park vandaag, hè? Njeg.",
    "quest.guhs.park.wachter.later1": "Gooi je je papiertjes in de prullenbak? Die vindt dat heerlijk.",
    "quest.guhs.park.wachter.later2": "Gaat het goed met je Hertguhtje? Hij mag altijd op bezoek komen!",
    "quest.guhs.park.wachter.later3": "Op de bankjes zitten is ook een vak. Ik oefen elke dag. Vahoeg!",
    "quest.guhs.park.optie.helpen": "Ik help je graag!",
    "quest.guhs.park.optie.park": "Wat is dit voor park?",
    "quest.guhs.park.optie.dank": "Echt? Dankjewel!",
    # the kooi-verzorger-guh
    "quest.guhs.park.verzorger.hallo": "Hoi! Ik zorg voor de Hertguhtjes. Ze zijn een beetje verlegen, maar dol op kaasknabbels.",
    "quest.guhs.park.verzorger.voeren": "Je hebt %s van de %s Hertguhtjes gevoerd. Elk wil er één, over het hek heen!",
    "quest.guhs.park.verzorger.klusje": "Wil je me helpen? De ruif is leeg. Breng me %s tarwe, dan krijgen de Hertguhtjes vanavond "
                                        "lekker hooi.",
    "quest.guhs.park.verzorger.tekort": "Je hebt %s tarwe, ik heb er %s nodig voor de ruif.",
    "quest.guhs.park.verzorger.dank": "Wat lief! De ruif is weer vol. Ga maar gauw naar de parkwachter, hij heeft iets voor je.",
    "quest.guhs.park.verzorger.terug": "Ga maar gauw naar de parkwachter in zijn huisje!",
    "quest.guhs.park.verzorger.later0": "De Hertguhtjes slapen het liefst onder het afdakje. Ssst.",
    "quest.guhs.park.verzorger.later1": "Geweitjes poetsen is best moeilijk, hoor. Ze kriebelen!",
    "quest.guhs.park.verzorger.later2": "Dankzij jou zijn ze helemaal vadsig geworden. Njeg!",
    # hints
    "quest.guhs.park.hint.plaquettes": "Lees de drie plaquettes in de beeldentuin (rechtsklik).",
    "quest.guhs.park.hint.herten": "Voer elk Hertguhtje in de hertenkooi een kaasknabbel (rechtsklik met een kaasknabbel).",
    "quest.guhs.park.hint.verzorger": "Praat met de kooi-verzorger bij de hooibalen naast de hertenkooi.",
    "quest.guhs.park.hint.hooi": "Breng de kooi-verzorger tarwe voor de ruif.",
    "quest.guhs.park.hint.terug": "Ga terug naar de parkwachter in zijn huisje.",
    "quest.guhs.park.hint.beeld": "Rechtsklik een van je eigen guhs met het beeldje: hij gaat poseren!",
}

GUHDEX = ("hertguhtje", "Hertguhtje", "zeldzaam (alleen in het Knabbelpark)",
          "Een guh met piepkleine geweitjes en witte vlekjes. De Hertguhtjes wonen in de hertenkooi van het Knabbelpark en "
          "zijn dol op kaasknabbels. Help de parkwachter, dan mag er eentje met je mee.")

STAPPEN = [
    ("Praat met de parkwachter-guh", "Praat met de parkwachter-guh in zijn huisje in het Knabbelpark.", "Het Knabbelpark (Guhmensie)"),
    ("Lees de drie plaquettes", "Lees de drie plaquettes bij de beelden in de beeldentuin.", "De beeldentuin van het Knabbelpark"),
    ("Voer de Hertguhtjes", "Geef elk Hertguhtje in de hertenkooi een kaasknabbel.", "De hertenkooi van het Knabbelpark"),
    ("Help de kooi-verzorger", "Praat met de kooi-verzorger bij de hertenkooi en breng haar tarwe voor de ruif.", "Naast de hertenkooi"),
    ("Terug naar de parkwachter", "Ga terug naar de parkwachter: hij heeft iets voor je.", "Het huisje van de parkwachter"),
]

VERBORGEN = ["park_plaquette_1", "park_plaquette_2", "park_plaquette_3", "park_hert_gevoerd", "park_prullenbak", "park_klaar"]


def texts(h):
    for k, v in TEXTS.items():
        h.lang(k, v, v)
    bbq2.stub_npc(h, "parkwachterguh", TEXTS["entity.guhs.guh_npc.parkwachterguh"], hue=0.30, sat=0.9, val=0.85)
    bbq2.stub_npc(h, "kooiverzorgerguh", TEXTS["entity.guhs.guh_npc.kooiverzorgerguh"], hue=0.08, sat=0.75, val=0.95)
    id_, naam, zeldzaam, info = GUHDEX
    bbq2.pagina(h, id_, naam, zeldzaam, info)
    verhaal_motor.groep(h, STRUCTUUR, "Het Knabbelpark")
    verhaal_motor.verhaallijn(h, STRUCTUUR, "Een dagje in het Knabbelpark",
                              "De parkwachter-guh kan wel wat hulp gebruiken: lees de plaquettes, voer de Hertguhtjes en help de "
                              "kooi-verzorger. Dan mag er een Hertguhtje met je mee!",
                              stappen=STAPPEN, klaar=("Klaar! Je eigen Hertguhtje is mee. Vahoeg!", "Het Knabbelpark"))
    for name in VERBORGEN:
        bbq2.verborgen(h, name)


def sounds(h):
    def patch(d):
        d["park.njeg"] = {"sounds": [{"name": "guhs:entity.guh.happy", "type": "event", "pitch": 0.7, "volume": 0.7},
                                     {"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 0.75, "volume": 0.6}],
                          "subtitle": "subtitles.guhs.park.njeg"}
        return d
    h.patch_json(f"{h.A}/sounds.json", patch)


def recepten_en_loot(h):
    h.shaped("park_bankje", ["SSS", "P P"], {"S": "minecraft:cherry_slab", "P": "minecraft:stick"}, "guhs:park_bankje", 2)
    h.shaped("park_lantaarn", [" W ", "WGW"], {"W": "minecraft:pink_wool", "G": "minecraft:glowstone"}, "guhs:park_lantaarn", 2)
    h.shapeless("park_prullenbak", ["minecraft:cauldron", "minecraft:pink_dye"], "guhs:park_prullenbak", 1)
    for b in ("park_bankje", "park_lantaarn", "park_prullenbak", "park_eigen_beeld"):
        h.self_drop(b)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:park_bankje"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:park_lantaarn", "guhs:park_prullenbak", "guhs:park_eigen_beeld"])
    h.add_tag("guhs/tags/block/knabbelpark/vrij", [])          # (the plekjes API: blocks everybody may change in a park)
    for b in ("park_bankje", "park_lantaarn", "park_prullenbak"):
        h.w(f"{h.A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})


# =====================================================================================================================
# the structure
# =====================================================================================================================
def structuur(h, t):
    D = h.D
    for naam, s in t.items():
        s.save(f"knabbelpark/{naam}")
    bouw.pools(h)
    verplicht = [f"guhs:knabbelpark/{n}" for n in bouw.VERPLICHT]
    h.w(f"{D}/worldgen/structure/{STRUCTUUR}.json", {
        "type": TYPE, "biomes": f"#guhs:has_structure/{STRUCTUUR}", "step": "surface_structures", "spawn_overrides": {},
        "terrain_adaptation": "beard_thin", "start_pool": f"guhs:{STRUCTUUR}/start", "size": SIZE, "max_distance_from_center": MAX_AFSTAND,
        "check_radius": CHECK_RADIUS, "max_height_difference": VLAK, "verplicht": verplicht, "pogingen": 8})
    entry = [{"structure": f"guhs:{STRUCTUUR}", "weight": 1}]
    h.w(f"{D}/worldgen/structure_set/{STRUCTUUR}.json", {"structures": entry, "placement": {
        "type": "minecraft:random_spread", "spacing": SPACING, "separation": SEPARATION, "salt": SALT}})
    g = GEGARANDEERD
    h.w(f"{D}/worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json", {"structures": entry, "placement": {
        "type": "guhs:gegarandeerd", "salt": SALT + wereld.GEGARANDEERD_SALT, "min_afstand": g["min"], "max_afstand": g["max"],
        "alleen_nieuw": True, "sector": g["sector"], "sectoren": wereld.SECTOREN}})
    h.w(f"{D}/tags/worldgen/biome/has_structure/{STRUCTUUR}.json", {"values": [f"guhs:{b}" for b in BIOMES]})
    # how far its pieces reach from the middle of the start chunk (make_v2.bouwruimte() writes keep_clear)
    h.RUIMTE_HOOKS[TYPE] = lambda s_json, jigsaw_reach: s_json["max_distance_from_center"] + 8 + 4
    h.VOORRANG.pop(STRUCTUUR, None)
    wereld.STRUCTUREN[STRUCTUUR] = dict(soort="land", kompas="knus", gegarandeerd=g, voorrang=None, salt=SALT, spacing=SPACING, biomes=BIOMES)
    wereld._schrijf_overzicht(h)


def test_templates(h):
    s = h.Structure((15, 6, 15))
    s.fill(0, 0, 0, 14, 0, 14, "minecraft:grass_block", {"snowy": "false"})
    s.save("park_test_kamer")


# =====================================================================================================================
# FTB quests
# =====================================================================================================================
def ftb(fq):
    q, adv, item, st = fq.q, fq.adv, fq.item, fq.structure
    kn = "guhs:kaas_knabbels"
    q("park_gevonden", "Het Knabbelpark", "Ergens in de &dGuhmensie&r ligt een groot roze park, met wandelpaadjes over de heuvels, "
      "bankjes, lantaarns en heel veel bloemen. Het &6Superkompas&r wijst de weg (tabblad Knus).", "guhs:park_lantaarn",
      [st(f"guhs:{STRUCTUUR}")], rewards=((kn, 12),), shape="circle", xp=100)
    wereld.ftb_questlijn(fq, NAME, STRUCTUUR, [
        ("De parkwachter", "In zijn huisje zit de &dparkwachter-guh&r. Praat met hem: hij kan wel een handje hulp gebruiken.",
         "guhs:park_bankje"),
        ("De beeldentuin", "Lees de drie &6plaquettes&r bij de beelden van Joostvadsje en trapguh, Liekguh en Lunde, en Opa Kaasguh. "
         "Rechtsklik op een plaquette.", "minecraft:oak_sign"),
        ("Hertguhtjes voeren", "In de hertenkooi wonen drie &dHertguhtjes&r. Geef ze elk een kaasknabbel, over het hek heen.",
         "guhs:kaas_knabbels"),
        ("De kooi-verzorger", "Help de &dkooi-verzorger-guh&r bij de hooibalen: ze heeft tarwe nodig voor de ruif.", "minecraft:wheat"),
        ("Een Hertguhtje voor jou", "Terug bij de parkwachter krijg je je eigen &dHertguhtje&r en een &6beeldje van je guh&r. "
         "Laat je guh poseren: rechtsklik hem met het beeldje!", "guhs:park_eigen_beeld"),
    ], na=["park_gevonden"], eind=((kn, 16), ("guhs:park_lantaarn", 2)))
    q("park_hertguhtje", "Een Hertguhtje gezien", "Kijk een &dHertguhtje&r van dichtbij aan: zijn bladzijde in de Guhdex vult zich "
      "(een extra bladzijde: hij telt niet mee voor 'alles verzameld').", "guhs:park_eigen_beeld", [adv("seen_hertguhtje")],
      rewards=((kn, 8),), deps=["park_gevonden"], xp=50)
    q("park_prullenbak", "Njeg!", "Gooi iets in een &6guhprullenbak&r (rechtsklik met iets in je hand). Hij eet het smakelijk op. "
      "Mooie spullen spuugt hij gewoon weer uit.", "guhs:park_prullenbak", [adv("park_prullenbak")], rewards=((kn, 4),),
      deps=["park_gevonden"], xp=20)


# =====================================================================================================================
# build + self-check
# =====================================================================================================================
def selfcheck(h, t, p):
    problems = bouw.check(h, t, p)
    java = ""
    for f in os.listdir(JAVA):
        if f.endswith(".java"):
            java += open(os.path.join(JAVA, f), encoding="utf-8").read()
    for m in re.finditer(r'"((?:gui|quest)\.guhs\.park[\w.]*)"', java):
        k = m.group(1)
        if k.endswith("."):
            continue
        if k not in TEXTS:
            problems.append(f"Java names {k}, no Dutch text")
    if f'REISGUH_NAAM = "{REISGUH_NAAM}"' not in java:
        problems.append("ParkFeature.REISGUH_NAAM differs from park.REISGUH_NAAM")
    if f'PLAQUETTES = {len(PLAQUETTE_TEKST)}' not in java or "HOOI = 6" not in java:
        problems.append("Park.java: PLAQUETTES / HOOI changed: check the texts")
    for soort in modellen.BEELDEN:
        if soort.upper() not in java:
            problems.append(f"statue {soort} not in ParkBlokken.Soort")
    for k in [f"quest.guhs.park.wachter.later{i}" for i in range(4)] + [f"quest.guhs.park.verzorger.later{i}" for i in range(3)] \
            + [f"gui.guhs.park.prullenbak.{i}" for i in range(4)]:
        if k not in TEXTS:
            problems.append(f"no text {k}")
    if problems:
        raise SystemExit("park: the park is not right:\n  " + "\n  ".join(problems[:60]))


def build(h):
    p = plekken()
    t = bouw.templates(h, p)
    selfcheck(h, t, p)
    structuur(h, t)
    test_templates(h)
    texts(h)
    sounds(h)
    recepten_en_loot(h)
    modellen.build(h)
