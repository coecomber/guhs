"""
bbq2 (guhrio-w3): world 3 of Super Guhrio (the burcht): levels 3-1 and 3-2 and the final duel. Java: feature/guhriow3.

  guhrio_w3_banen.py     the two levels and the duel arena, built with the engine's lane builder (LEVELS, DUEL: the castle
                         of features/guhrio_kasteel.py plugs them into its slots while guhrio.build runs)
  guhrio_w3_modellen.py  the box models: the Grote Nether-Mika, his coals, the cake
  guhrio_w3_tex.py       the block textures: the bridge's grate, the lever, Guhshi's hitching post, the Vuurpeper bush
  guhrio_w3_wiki.py      what the docs step needs (not built here)

build(h): those textures and models, the blockstates / items / loot of the five blocks, every text (the fight's lines, the
end scene, the narrator card of the duel), the advancements, data/guhs/guhriow3/duel.json (where the boss finds the bridge,
the lever and the ?-block: the same numbers the arena is built with) and the test room guhriow3_test_duel (the arena by
itself, for the game tests). ftb(fq): the section "Wereld 3: de burcht" of the chapter guhs_guhrio.
"""
import os
import re

from features import guhrio_w3_banen as banen

FTB_SECTIES = [("guhrio_w3", "Wereld 3: de burcht", "npc:perzikguh", None)]

# what the castle reads (features/guhrio_kasteel.py)
LEVELS = banen.LEVELS
DUEL = banen.DUEL

TESTDUEL = "guhriow3_test_duel"
SCENE, KAART = "guhriow3_einde", "guhriow3_duel"
BLOKKEN = ("guhriow3_brug", "guhriow3_baas_plek", "guhriow3_hendel", "guhriow3_parkeerpaal", "guhriow3_peperstruik")
VERBORGEN = ("guhrio_w3_hendel", "guhrio_w3_schild", "guhrio_w3_taart", "guhrio_w3_vadsmunten")

TEXTS = {
    "block.guhs.guhriow3_brug": "Roosterbrug",
    "block.guhs.guhriow3_baas_plek": "Plekje van de Grote Nether-Mika",
    "block.guhs.guhriow3_hendel": "Brughendel",
    "block.guhs.guhriow3_parkeerpaal": "Guhshi-parkeerpaal",
    "block.guhs.guhriow3_peperstruik": "Vuurpeperstruik",
    "entity.guhs.grote_nether_mika": "Grote Nether-Mika",
    "entity.guhs.guhriow3_kooltje": "Gloeiend kooltje",
    "entity.guhs.guhriow3_taart": "Taartkarretje van Prinses Perzikguh",
    "gui.guhs.guhriow3.intro": "\"WIE KOMT DAAR AAN MIJN TAART?! NJEG!\"",
    "gui.guhs.guhriow3.ronde1": "Ronde 1: ren onder hem door als hij springt en trek aan de hendel, njeg!",
    "gui.guhs.guhriow3.hendel": "KLIK! De halve brug ploft in de saus. \"MIJN BRUG!\"",
    "gui.guhs.guhriow3.pijp": "De groene pijp brengt je terug naar de brug, njeg.",
    "gui.guhs.guhriow3.ronde2": "Ronde 2: hij rolt in zijn schild. Spring eroverheen, njeg!",
    "gui.guhs.guhriow3.ronde3": "Ronde 3: het ?-blok zit weer vol. Pak de Vuurpeper en knabbel zijn schild terug!",
    "gui.guhs.guhriow3.treffer": "Raak! Nog %s keer en hij rolt de saus in, njeg!",
    "gui.guhs.guhriow3.plons": "PLONS! (Geen zorgen: hij kan zwemmen. Hij is alleen heel erg beledigd.)",
    "gui.guhs.guhriow3.opnieuw": "\"Nog eentje?! Goed dan. NJEG!\"",
    "gui.guhs.guhriow3.gewonnen": "Je hebt de Grote Nether-Mika verslagen in %s. Hij is niet boos meer: hij heeft taart. Vahoeg!",
    "gui.guhs.guhriow3.parkeer": "Guhshi wacht hier even op je: van vuurpepers moet hij niezen, njeg.",
    "gui.guhs.guhriow3.peper_guhshi": "Op Guhshi gooi je geen knabbels (hij hapt ze zelf op). Zet hem eerst bij een parkeerpaal, njeg.",
    "gui.guhs.guhriow3.padguh": "Pad-guh: Psst! Achter die hele grote poort met die hoorns ruikt het naar taart. Dáár moet je zijn, "
                                "denk ik. Vahoeg veel succes, njeg!",
}
SCENE_TEKST = {
    "mok": "Hmpf. Nat. Plakkerig. En nog steeds geen taart. Njeg.",
    "herrie": "Joehoe! Wie maakt er zo'n herrie op mijn brug?",
    "redden": "Kom je me redden? Wat lief! Maar ik ben helemaal niet ontvoerd, njeg. Ik was hier taart aan het bakken.",
    "stukje": "Ik wou alleen een stukje. Maar niemand nodigt een Grote Nether-Mika ooit uit voor taart...",
    "vragen": "Dan vraag je het toch gewoon, grote vadserik? Hier. Het grootste stuk is voor jou.",
    "voor_mij": "VOOR MIJ?! ...Vahoeg. Maar de volgende keer win ik. Na de taart.",
    "torenkamer": "Kom mee naar de torenkamer: er is genoeg voor iedereen. Vads voor alle guhs!",
}
KAART_REGELS = [
    "Achter de grote poort wacht hij: de Grote Nether-Mika, met hoorns, rode manen en een schild vol stekels.",
    "Hij duwt alleen maar. Wie geraakt wordt of in de saus valt, staat zo weer bij zijn vlaggetje.",
    "Drie rondes: ren onder hem door naar de hendel, spring over zijn rollende schild, en knabbel dat schild terug met een Vuurpeper.",
    "En vergeet niet waar het allemaal om begon: een stukje taart. Njeg!",
]


# =====================================================================================================================
# textures, models, blockstates, items, loot
# =====================================================================================================================
def textures(h):
    from features import guhrio_w3_modellen, guhrio_w3_tex as t
    h.save(t.brug(), "block", "guhriow3_brug.png")
    h.save(t.hendel(False), "block", "guhriow3_hendel.png")
    h.save(t.hendel(True), "block", "guhriow3_hendel_om.png")
    h.save(t.parkeerpaal(), "block", "guhriow3_parkeerpaal.png")
    h.save(t.peperstruik(), "block", "guhriow3_peperstruik.png")
    h.save(t.baas_icoon(), "item", "guhriow3_baas_plek.png")
    guhrio_w3_modellen.build(h)


def _paneel(h, naam):
    """Two thin crossed panels with the picture on them: one of them always faces the camera of a lane, whichever way the
    lane runs, and the picture is never mirrored on the screen (the engine's door is made the same way)."""
    tex = f"guhs:block/{naam}"
    face = {"uv": [0, 0, 16, 16], "texture": "#beeld"}
    h.w(f"{h.A}/models/block/{naam}.json", {"render_type": "cutout", "textures": {"beeld": tex, "particle": tex}, "elements": [
        {"from": [0, 0, 7.5], "to": [16, 16, 8.5], "faces": {"north": face, "south": face}},
        {"from": [7.5, 0, 0], "to": [8.5, 16, 16], "faces": {"east": face, "west": face}}]})


def blocks_and_items(h):
    A = h.A
    b = lambda n: f"guhs:block/{n}"
    h.simple_block("guhriow3_brug")
    for naam in ("guhriow3_hendel", "guhriow3_hendel_om", "guhriow3_parkeerpaal"):
        _paneel(h, naam)
    h.w(f"{A}/blockstates/guhriow3_hendel.json", {"variants": {"getrokken=false": {"model": b("guhriow3_hendel")},
                                                                "getrokken=true": {"model": b("guhriow3_hendel_om")}}})
    h.w(f"{A}/blockstates/guhriow3_parkeerpaal.json", {"variants": {"": {"model": b("guhriow3_parkeerpaal")}}})
    h.w(f"{A}/models/block/guhriow3_peperstruik.json", {"parent": "minecraft:block/cross", "render_type": "cutout",
                                                        "textures": {"cross": b("guhriow3_peperstruik")}})
    h.w(f"{A}/blockstates/guhriow3_peperstruik.json", {"variants": {"": {"model": b("guhriow3_peperstruik")}}})
    for naam in ("guhriow3_hendel", "guhriow3_parkeerpaal", "guhriow3_peperstruik"):
        h.item_model(naam, b(naam))
    # the boss's spot is not drawn at all: a model with only the breaking particles, a flat picture as item
    h.w(f"{A}/models/block/guhriow3_baas_plek.json", {"textures": {"particle": "guhs:item/guhriow3_baas_plek"}})
    h.w(f"{A}/blockstates/guhriow3_baas_plek.json", {"variants": {"": {"model": b("guhriow3_baas_plek")}}})
    h.item_model("guhriow3_baas_plek")
    for naam in ("guhriow3_brug", "guhriow3_hendel", "guhriow3_parkeerpaal", "guhriow3_peperstruik"):
        h.self_drop(naam)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:guhriow3_brug", "guhs:guhriow3_hendel"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:guhriow3_parkeerpaal"])


# =====================================================================================================================
# texts, the scene, the narrator card, advancements
# =====================================================================================================================
def _kaart(k):
    """The duel's card: the keep in the sauce sea, the bridge, and what it is all about."""
    saus, donker, inkt = (232, 150, 60, 255), (170, 96, 40, 255), k.INKT
    k.water([(6, 110), (40, 96), (110, 104), (180, 94), (250, 108), (250, 154), (6, 154)], saus)
    # the two ledges and the bridge between them
    k.land([(14, 112), (70, 100), (78, 126), (20, 134)], (124, 108, 116, 255))
    k.land([(176, 100), (240, 108), (236, 132), (170, 124)], (124, 108, 116, 255))
    for x in range(78, 172, 6):
        k.d.line((x, 106 - (x % 12) // 6, x + 5, 106 - ((x + 6) % 12) // 6), fill=inkt, width=2)
        k.d.line((x + 2, 107, x + 2, 112), fill=donker)
    k.toren(26, 106, 34)
    k.toren(226, 106, 40)
    k.toren(206, 108, 26)
    # the cake, big, in the sky over the bridge
    cx, cy = 126, 62
    k.d.rectangle((cx - 30, cy + 14, cx + 30, cy + 18), fill=(250, 250, 250, 255), outline=inkt)
    k.d.rectangle((cx - 24, cy - 8, cx + 24, cy + 14), fill=(246, 226, 170, 255), outline=inkt)
    k.d.rectangle((cx - 24, cy + 2, cx + 24, cy + 4), fill=(214, 60, 70, 255))
    k.d.rectangle((cx - 24, cy - 8, cx + 24, cy - 2), fill=(255, 170, 196, 255), outline=inkt)
    for x in range(cx - 20, cx + 22, 8):
        k.d.ellipse((x - 2, cy - 3, x + 2, cy + 3), fill=(255, 170, 196, 255))
    for x in (cx - 14, cx, cx + 14):
        k.d.ellipse((x - 3, cy - 15, x + 3, cy - 9), fill=(214, 30, 50, 255), outline=inkt)
    # two horns over it all
    for sx in (-1, 1):
        k.d.polygon([(cx + sx * 44, cy + 4), (cx + sx * 58, cy - 22), (cx + sx * 50, cy + 8)], fill=(240, 234, 208, 255), outline=inkt)
    k.kompasroos(232, 30) if hasattr(k, "kompasroos") else None


def texts(h):
    from features import verhaal_motor
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)
    verhaal_motor.scene(h, SCENE, "Een stukje taart", SCENE_TEKST, namen={"mika": "Grote Nether-Mika", "perzik": "Prinses Perzikguh"})
    verhaal_motor.vertelkaart(h, KAART, "Het duel met de Grote Nether-Mika", KAART_REGELS, _kaart)


def advancements(h):
    from features import bbq2
    for naam in VERBORGEN:
        bbq2.verborgen(h, naam)
    bbq2.zichtbaar(h, "guhrio", "guhrio_w3_taart", "guhrio_duel", "minecraft:cake", "goal", "Taart voor iedereen",
                   "Kijk hoe het afloopt met de Grote Nether-Mika, Prinses Perzikguh en de taart")


# =====================================================================================================================
# the duel's plan and the test room
# =====================================================================================================================
def duel(h):
    h.w(os.path.join(h.D, "guhriow3", "duel.json"), banen.duel_plan())


def testduel(h):
    """The duel arena by itself in a room of 42 x 21 x 5 (the lane runs east along z = 1, the painted wall is z = 0): the
    game tests fight the boss in exactly what bouw_duel builds. No sauce (nothing may flow in a test room; the boss's splash
    is a sum, not a fluid)."""
    from features import guhrio_baan as gb
    L, HH = 40, 18
    s = h.Structure((L + 2, HH + 3, 5))
    s.fill(0, 0, 0, L + 1, 1, 4, "minecraft:smooth_stone")
    s.fill(0, 2, 1, L + 1, HH + 2, 4, "minecraft:air")
    for x in range(L):
        s.set(1 + x, 1, 1, "minecraft:air")
    baan = gb.Baanbouwer(h, TESTDUEL, "DUEL", 6, 3, L, HH, (1, 2, 1), (1, 0))
    banen.bouw_duel(baan)
    baan.trog.clear()
    baan.controleer(duel=True)
    baan.stempel(s.set, s.entity, lambda _s, _y: (banen.NETHER, None))
    s.save(TESTDUEL)
    gb.level_json(h, TESTDUEL, baan.banen(), "DUEL", uitgang=baan.eigen(1, banen.Y), ingang=baan.eigen(1, banen.Y))


def selfcheck(h):
    from features import guhrio_w3_modellen
    A, D = h.A, h.D
    missing = [p for p in [f"{A}/blockstates/{n}.json" for n in BLOKKEN] + [f"{A}/models/item/{n}.json" for n in BLOKKEN]
               + [f"{A}/guhrio_model/{n}.json" for n in guhrio_w3_modellen.NAMEN] + [f"{h.TEX}/entity/{n}.png" for n in guhrio_w3_modellen.NAMEN]
               + [f"{D}/guhriow3/duel.json", f"{D}/structure/{TESTDUEL}.nbt", f"{D}/guhrio_level/{TESTDUEL}.json",
                  f"{h.TEX}/gui/verhaal/kaart_{KAART}.png"]
               + [f"{D}/advancement/quest/{n}.json" for n in VERBORGEN]
               if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    missing += [f"scene.guhs.{SCENE}.{k}" for k in SCENE_TEKST if f"scene.guhs.{SCENE}.{k}" not in h.NL]
    # the arena the generator builds and the one the boss falls back on (DuelPlan.STANDAARD) must be the same
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhriow3", "DuelPlan.java")
    if os.path.exists(java):
        m = re.search(r"STANDAARD = new DuelPlan\((-?\d+), (-?\d+), (-?\d+), (-?\d+), (-?\d+), new BlockPos\((-?\d+), (-?\d+), (-?\d+)\), "
                      r"new BlockPos\((-?\d+), (-?\d+), (-?\d+)\), \"([a-z0-9_:]+)\"\)", open(java, encoding="utf-8").read())
        p = banen.duel_plan()
        zelf = (p["brug_van"], p["brug_tot"], p["breuk"], p["muur_links"], p["muur_rechts"], *p["hendel"], *p["vraag"])
        if not m or tuple(int(v) for v in m.groups()[:11]) != zelf or m.group(12) != p["blok"]:
            missing.append(f"DuelPlan.STANDAARD in {java} is not the arena of guhrio_w3_banen.duel_plan() {zelf + (p['blok'],)}")
    if missing:
        raise SystemExit(f"guhrio_w3: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    texts(h)
    advancements(h)
    duel(h)
    testduel(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: the section "Wereld 3: de burcht"
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("guhrio_w3_3_1", "Level 3-1: De Grillgang", "De poort van &6level 3-1&r gaat open als de kelders gehaald zijn. Hier wacht &dGuhshi&r op "
      "wie zijn ei gevonden heeft: loop tegen hem aan en je zit erop. Spatie ingedrukt houden = fladderen. Spring over de saus van naaf "
      "naar naaf van de draaiende &6grillspiesen&r en lok de &6Plof-Mika's&r naar beneden voor je eronderdoor rent. Bij de "
      "&dparkeerpaal&r blijft Guhshi even wachten: het stuk met de &6Vuurpeper&r doe je te voet (een knabbel door de spleet zet de "
      "schakelaar om).", "guhs:guhrio_grillspies_plek", [adv("guhrio_stap_6")], rewards=(("guhs:kaas_knabbels", 12),), deps=["guhrio_binnen"])
    q("guhrio_w3_3_2", "Level 3-2: De Sauskelder", "Het level van Guhshi. Twee gaten zijn zo breed dat alleen zijn fladdersprong eroverheen "
      "komt (te voet legt de &dklokschakelaar&r er acht tellen lang een brug). Daartussen: valblokken onder snelle spiesen, een platform "
      "onder twee Plof-Mika's, en achter een deur de &6Peperkamer&r.", "guhs:guhrio_guhshi_plek", [adv("guhrio_stap_7")],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["guhrio_w3_3_1"])
    q("guhrio_w3_vadsmunten", "Zes keer vads in de burcht", "De zes &6grote vadsmunten&r van wereld 3. Tip: één hangt boven de middelste "
      "grillspies, één ligt onder een Plof-Mika, één in een schatkamer waar je alleen door een pijp met een Hapbloem komt, één hoog "
      "boven het eerste grote gat, één laag tussen twee valblokken (een klein hupje!) en één in de kooi van de Peperkamer.",
      "guhs:guhrio_vadsmunt", [adv("guhrio_w3_vadsmunten")], rewards=(("guhs:kaas_knabbels", 16),), deps=["guhrio_w3_3_1"], shape="diamond")
    q("guhrio_w3_hendel", "Ronde 1: de hendel", "Achter de grote poort staat de &6Grote Nether-Mika&r op zijn roosterbrug. Hij gooit "
      "trage gloeiende kooltjes (spring eroverheen) en als hij landt, duwt de dreun je weg (spring op het goede moment). Ren &donder hem "
      "door&r terwijl hij in de lucht hangt en trek aan de &6hendel&r: de halve brug ploft in de saus. De groene pijp brengt je terug.",
      "guhs:guhriow3_hendel", [adv("guhrio_w3_hendel")], deps=["guhrio_w3_3_2"])
    q("guhrio_w3_schild", "Ronde 2 en 3: schild retour", "Op de korte brug kruipt hij in zijn schild en rolt heen en weer: &dspring "
      "eroverheen&r. Na drie bonken tegen de muur zit het &6?-blok&r weer vol: pak de &6Vuurpeper&r en gooi een knabbel tegen het schild "
      "terwijl het op je af rolt. Dan stuitert het terug. Drie keer raak en hij kan niet meer remmen.", "guhs:guhrio_vuurpeper",
      [adv("guhrio_w3_schild")], deps=["guhrio_w3_hendel"])
    q("guhrio_w3_duel", "De Grote Nether-Mika", "Win het duel. Hij plonst in de saus, klimt er aan de overkant weer uit (nat, plakkerig, "
      "beledigd, verder niks aan de hand) en gaat zitten mokken. Niemand doet hier iemand pijn, njeg.", "guhs:guhriow3_brug",
      [adv("guhrio_duel")], rewards=(("guhs:kaas_knabbels", 32),), deps=["guhrio_w3_schild"], shape="gear", xp=300)
    q("guhrio_w3_taart", "Een stukje taart", "Kijk hoe het afloopt: &dPrinses Perzikguh&r, de taart, en waarom de Grote Nether-Mika haar "
      "eigenlijk had meegenomen. De Guhdex (Verhalen > Super Guhrio) speelt het filmpje nog eens af. Daarna sta je in de torenkamer.",
      "minecraft:cake", [adv("guhrio_w3_taart")], rewards=(("minecraft:cake", 1),), deps=["guhrio_w3_duel"], shape="circle")
