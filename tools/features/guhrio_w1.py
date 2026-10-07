"""
bbq2 (guhrio-w1): world 1 of Super Guhrio, "de binnentuin": levels 1-1 (de binnentuin) and 1-2 (de heggentuin) of the
Kasteel van de Grote Nether-Mika. Java: feature/guhriow1.

  this module      the blocks of the painted garden (models, blockstates, items, loot), every text, the advancements, the
                   FTB quests of the section "Wereld 1: de binnentuin", the two levels once more as templates of their own
                   for the game tests, and the self-check
  guhrio_w1_bouw   the two levels (LEVELS: what the castle guhs:guhrio_kasteel takes from this module while guhrio.py
                   builds it), their painted walls, and controleer(): can the level be played with the safe jump?
  guhrio_w1_tex    the textures
  guhrio_w1_wiki   what the docs step needs (not in FEATURES)

  blocks      guhriow1_gras, guhriow1_aarde, guhriow1_heg, guhriow1_wolk (the lane: what you walk on), guhriow1_lucht,
              guhriow1_loof, guhriow1_wolk_ver, guhriow1_wolk_snoet, guhriow1_heuvel, guhriow1_heuvel_ogen (the painted wall
              behind the lane: paler, so it never looks like something to stand on), guhriow1_bloem (soort 0..3: a little
              plant you walk through), and two invisible pieces of a level: guhriow1_tip (tip 0..15: a line above the
              panel) and guhriow1_geheim (the mark of a secret room)
  templates   guhriow1_test_1_1, guhriow1_test_1_2 (98 x 23 x 3): each level as the lane builder wrote it, on its own; the
              game tests put them in the world and walk them (their start block names the castle's own level file)
"""
import os
import re

from features import bbq2
from features import guhrio_baan
from features import guhrio_w1_bouw as bouw
from features import guhrio_w1_tex as tex
from features.guhrio_w1_bouw import LEVELS  # noqa: F401  (what guhrio_kasteel takes: {"1-1": bouw_1_1, "1-2": bouw_1_2})

# the section of chapter guhs_guhrio this world's quests land in (CONTRACT_130 5.5 / 8)
FTB_SECTIES = [("guhrio_w1", "Wereld 1: de binnentuin", "npc:padguh", None)]

TEST = {"1-1": "guhriow1_test_1_1", "1-2": "guhriow1_test_1_2"}
VERF = ("aarde", "heg", "wolk", "lucht", "loof", "wolk_ver", "wolk_snoet", "heuvel", "heuvel_ogen")


# =====================================================================================================================
# textures, models, blockstates, items, loot
# =====================================================================================================================
def textures(h):
    h.save(tex.gras_zij(), "block", "guhriow1_gras.png")
    h.save(tex.gras_boven(), "block", "guhriow1_gras_boven.png")
    h.save(tex.aarde(), "block", "guhriow1_aarde.png")
    h.save(tex.heg(), "block", "guhriow1_heg.png")
    h.save(tex.loof(), "block", "guhriow1_loof.png")
    h.save(tex.lucht(), "block", "guhriow1_lucht.png")
    h.save(tex.wolk(), "block", "guhriow1_wolk.png")
    h.save(tex.wolk_ver(), "block", "guhriow1_wolk_ver.png")
    h.save(tex.wolk_ver(True), "block", "guhriow1_wolk_snoet.png")
    h.save(tex.heuvel(), "block", "guhriow1_heuvel.png")
    h.save(tex.heuvel(True), "block", "guhriow1_heuvel_ogen.png")
    for soort in range(4):
        h.save(tex.plant(soort), "block", f"guhriow1_bloem_{soort}.png")
    h.save(tex.tip_icoon(), "item", "guhriow1_tip.png")
    h.save(tex.geheim_icoon(), "item", "guhriow1_geheim.png")


def blocks_and_items(h):
    A = h.A
    b = lambda n: f"guhs:block/{n}"
    for naam in VERF:
        h.simple_block(f"guhriow1_{naam}")
    # the lawn: grass on top, the fringe on the sides, earth below
    h.w(f"{A}/models/block/guhriow1_gras.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": b("guhriow1_gras_boven"), "bottom": b("guhriow1_aarde"), "side": b("guhriow1_gras")}})
    h.w(f"{A}/blockstates/guhriow1_gras.json", {"variants": {"": {"model": b("guhriow1_gras")}}})
    h.w(f"{A}/models/item/guhriow1_gras.json", {"parent": b("guhriow1_gras")})
    # the little plants: a cross each
    for soort in range(4):
        h.w(f"{A}/models/block/guhriow1_bloem_{soort}.json", {"parent": "minecraft:block/cross", "render_type": "cutout",
                                                              "textures": {"cross": b(f"guhriow1_bloem_{soort}")}})
    h.w(f"{A}/blockstates/guhriow1_bloem.json", {"variants": {f"soort={s}": {"model": b(f"guhriow1_bloem_{s}")} for s in range(4)}})
    h.item_model("guhriow1_bloem", b("guhriow1_bloem_0"))
    # the two pieces nobody sees: a model with only the breaking particles, a flat picture as item
    for naam in ("guhriow1_tip", "guhriow1_geheim"):
        h.w(f"{A}/models/block/{naam}.json", {"textures": {"particle": f"guhs:item/{naam}"}})
        h.w(f"{A}/blockstates/{naam}.json", {"variants": {"": {"model": b(naam)}}})
        h.item_model(naam)
    for naam in VERF + ("gras", "bloem"):
        h.self_drop(f"guhriow1_{naam}")
    h.add_tag("minecraft/tags/block/mineable/shovel", ["guhs:guhriow1_gras", "guhs:guhriow1_aarde"])
    h.add_tag("minecraft/tags/block/mineable/hoe", ["guhs:guhriow1_heg", "guhs:guhriow1_loof"])


# =====================================================================================================================
# texts
# =====================================================================================================================
TIPS = [
    "A en D lopen, spatie springt. Houd spatie vast voor een hoge sprong, njeg!",
    "Spring van onderen tegen het vraagtekenblok. Wat zou erin zitten?",
    "Een Guhmba! Spring er bovenop: plat. Van opzij duwt hij je terug naar je vlaggetje.",
    "Niet in de kaasvijver stappen! Wie valt, staat gewoon weer bij zijn vlaggetje.",
    "Ga bovenop de groene pijp staan en duik erin met S.",
    "In sommige blokken zit een Superknabbel. Daar word je groot van, en groot breek je stenen!",
    "Ga voor de deur staan en druk op W.",
    "Een Schild-Mika! Spring erop, en schop dan zijn schild weg: dat ruimt lekker op.",
    "Wat een brede vijver! Houd sprinten ingedrukt: met een aanloop spring je verder.",
]
TEXTS = {
    "block.guhs.guhriow1_gras": "Geschilderd gras",
    "block.guhs.guhriow1_aarde": "Geschilderde aarde",
    "block.guhs.guhriow1_heg": "Geschilderde heg",
    "block.guhs.guhriow1_wolk": "Geschilderde wolk",
    "block.guhs.guhriow1_lucht": "Geschilderde lucht",
    "block.guhs.guhriow1_loof": "Geschilderd loof in de verte",
    "block.guhs.guhriow1_wolk_ver": "Geschilderd wolkje in de verte",
    "block.guhs.guhriow1_wolk_snoet": "Geschilderd wolkje met een snoet",
    "block.guhs.guhriow1_heuvel": "Geschilderde heuvel",
    "block.guhs.guhriow1_heuvel_ogen": "Geschilderde heuvel met oogjes",
    "block.guhs.guhriow1_bloem": "Geschilderd tuinplantje",
    "block.guhs.guhriow1_tip": "Guhrio-tip",
    "block.guhs.guhriow1_geheim": "Guhrio-geheimpje",
    "gui.guhs.guhriow1.geheim": "Geheim gevonden, njeg!",
    "gui.guhs.guhriow1.geheim.1_1": "Geheim gevonden: het mollenhol onder de binnentuin. De mol spaart munten, maar wie het vindt mag ze hebben. Njeg!",
    "gui.guhs.guhriow1.geheim.1_2": "Geheim gevonden: de muntenkas van de tuinman. Dus dáár groeien ze. Vahoeg!",
    "gui.guhs.guhriow1.vads": "Alle drie de grote vadsmunten van level %s zijn van jou. Vads, vads, vads!",
    "quest.guhs.guhriow1.padguh.bedankt": "Bedankt! Maar de prinses is in een ander kasteeldeel, njeg.",
    "quest.guhs.guhriow1.padguh.verder": "De Grote Nether-Mika nam haar mee 'voor een stukje taart'. Probeer de kelders eens: de poort van level 2-1 "
                                         "is nu open. Njeg!",
    "quest.guhs.guhriow1.padguh.alweer.0": "Alweer jij? Gezellig! Ze is er nog steeds niet, hoor. Ik heb hier alleen een tuin. En geen taart.",
    "quest.guhs.guhriow1.padguh.alweer.1": "Vahoeg, wat ben jij snel. De prinses? Nee joh. Ander kasteeldeel. Echt waar, njeg.",
    "quest.guhs.guhriow1.padguh.alweer.2": "Ik sta hier elke keer weer te zwaaien en elke keer moet ik het zeggen. Sorry, njeg. Wil je een bloemetje?",
    "quest.guhs.guhriow1.padguh.praat": "Njeg? Hoe kom jij hier, zo naast de baan? De prinses is in een ander kasteeldeel. Dat zeg ik tegen iedereen.",
}
for _i, _tip in enumerate(TIPS):
    TEXTS[f"gui.guhs.guhriow1.tip.{_i}"] = _tip
VERBORGEN = ("guhrio_w1_geheim_1_1", "guhrio_w1_geheim_1_2", "guhrio_w1_vads_1_1", "guhrio_w1_vads_1_2", "guhrio_w1_prinses")


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def advancements(h):
    for naam in VERBORGEN:
        bbq2.verborgen(h, naam)
    bbq2.zichtbaar(h, "guhrio", "guhrio_w1_binnentuin", "root", "guhs:guhriow1_gras", "task", "In een ander kasteeldeel",
                   "Haal level 1-1 en 1-2 van de binnentuin, en hoor van Pad-guh waar de prinses níét is")
    bbq2.zichtbaar(h, "guhrio", "guhrio_w1_tuingeheimen", "guhrio_w1_binnentuin", "guhs:guhriow1_bloem", "goal", "Geen tuingeheimen meer",
                   "Vind het mollenhol, de muntenkas en alle zes de grote vadsmunten van de binnentuin")


# =====================================================================================================================
# the FTB quests of "Wereld 1: de binnentuin"
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("guhrio_w1_1_1", "Level 1-1: de binnentuin", "De eerste poort links in de levelhal. Een geschilderd gazon binnen de kasteelmuren: hier leer "
      "je lopen, springen, tegen &6vraagtekenblokken&r bonken en bovenop &dGuhmba's&r landen. Aan het eind van het gazon duik je in de grote "
      "groene pijp (&dS&r) en loop je over de wolken en de boomtoppen naar de vlaggenmast. Njeg!",
      "guhs:guhriow1_gras", [adv("guhrio_stap_2")], deps=["guhrio_binnen"])
    q("guhrio_w1_1_2", "Level 1-2: de heggentuin", "De eerste poort rechts. Heggen om op te klimmen, kaasvijvers om over te springen en een "
      "&dSchild-Mika&r met een rijtje Guhmba's ervoor: spring op hem en schop zijn schild weg. Door de torendeur (&dW&r) kom je bovenop de "
      "tuinmuur; daar staat de vlaggenmast.", "guhs:guhriow1_heg", [adv("guhrio_stap_3")], deps=["guhrio_w1_1_1"])
    q("guhrio_w1_prinses", "In een ander kasteeldeel", "Achter de vlaggenmast van level 1-2 staat &dPad-guh&r al te zwaaien. Hij heeft goed "
      "nieuws en slecht nieuws. &oBedankt! Maar de prinses is in een ander kasteeldeel, njeg.&r De poort naar de kelders (wereld 2) is nu open.",
      "guhs:guhrio_mast", [adv("guhrio_w1_prinses")], rewards=(("guhs:kaas_knabbels", 12),), deps=["guhrio_w1_1_2"], shape="gear", xp=50)
    q("guhrio_w1_vads_1_1", "Drie keer vads in de binnentuin", "De drie &6grote vadsmunten&r van level 1-1. Eén ligt bovenop de stenen (klim op "
      "de heg erachter en spring terug), één ligt op een wolkje (waarom zweeft daar een los muntje?), en één ligt ergens waar alleen een mol "
      "komt. Haal daarna de vlaggenmast.", "guhs:guhrio_vadsmunt", [adv("guhrio_w1_vads_1_1")], deps=["guhrio_w1_1_1"], shape="diamond")
    q("guhrio_w1_vads_1_2", "Drie keer vads in de heggentuin", "De drie &6grote vadsmunten&r van level 1-2. Eén zit in een kistje van stenen: "
      "daar kom je alleen in als je groot bent (de &dSuperknabbel&r zit in het blok ervoor). Eén hangt hoog boven het gat achter het "
      "muurtorentje: spring ver. En één staat bij de tuinman op de plank. Haal daarna de vlaggenmast.",
      "guhs:guhrio_vadsmunt", [adv("guhrio_w1_vads_1_2")], deps=["guhrio_w1_1_2"], shape="diamond")
    q("guhrio_w1_geheim_1_1", "Het mollenhol", "Niet elke pijp in de binnentuin is zomaar een pijp. Boven één ervan zweeft een muntje. "
      "Probeer er eens in te duiken (&dS&r), njeg.", "guhs:guhrio_pijp", [adv("guhrio_w1_geheim_1_1")], deps=["guhrio_w1_1_1"], shape="circle")
    q("guhrio_w1_geheim_1_2", "De muntenkas", "Bovenop de heggenboog van level 1-2 staat een deur. Maar hoe kom je daar? Naast het lage "
      "heggetje ervoor zweven twee muntjes boven elkaar... spring er eens onder. Sommige blokken zie je pas als je er met je hoofd "
      "tegenaan bonkt.", "guhs:guhrio_deur", [adv("guhrio_w1_geheim_1_2")], deps=["guhrio_w1_1_2"], shape="circle")


# =====================================================================================================================
# the levels on their own (for the game tests) and the check that they can be played
# =====================================================================================================================
def testlevels(h):
    """
    Each level once more in a little structure of its own (the lane runs east along z = 1, the painted wall is z = 0): the
    game tests place it and walk it, so they test the very blocks the castle gets. Its start block names the castle's level
    (kasteel_1_1 / kasteel_1_2): the lanes of a level hang on its start block, so the castle's own level file fits.
    """
    uit = {}
    for nr, (wereld, fn) in enumerate(LEVELS.items()):
        L, HH = 96, 20
        s = h.Structure((L + 2, HH + 3, 3))
        s.fill(0, 0, 0, L + 1, 1, 2, "minecraft:smooth_stone")
        for x in range(L):
            s.set(1 + x, 1, 1, guhrio_baan.AIR)                    # the trench under the lane
        baan = guhrio_baan.Baanbouwer(h, "kasteel_" + wereld.replace("-", "_"), wereld, nr, 1, L, HH, (1, 2, 1), (1, 0))
        fn(baan)
        baan.controleer()
        baan.stempel(s.set, s.entity, lambda _s, _y: (bouw.LUCHT, None))
        s.save(TEST[wereld])
        uit[wereld] = baan
    return uit


def _kaal(baan):
    """A level's cells without what depends on the way its lane runs (a sideways pipe's facing, a pipe body's axis)."""
    return {k: (v[0], {p: w for p, w in (v[1] or {}).items() if p not in ("facing", "axis") or w in ("up", "down", "y")})
            for k, v in baan.cellen.items()}


def selfcheck(h, tests):
    A, D = h.A, h.D
    missing = [p for p in [f"{A}/blockstates/guhriow1_{n}.json" for n in VERF + ("gras", "bloem", "tip", "geheim")]
               + [f"{h.TEX}/block/guhriow1_{n}.png" for n in VERF + ("gras", "gras_boven", "bloem_0", "bloem_3")]
               + [f"{D}/structure/{n}.nbt" for n in TEST.values()] + [f"{D}/guhrio_level/kasteel_1_{n}.json" for n in "12"]
               + [f"{D}/advancement/quest/{n}.json" for n in VERBORGEN]
               if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    # the castle really took this module's levels, and they can be played (small and big, with the safe jump)
    for wereld in LEVELS:
        baan = bouw.KASTEEL.get(wereld)
        if baan is None or baan.id != "kasteel_" + wereld.replace("-", "_") or len(baan._bij) != 2:
            missing.append(f"level {wereld} was not built into the castle by guhrio_kasteel (guhrio.py must run before this module)")
            continue
        for naam, b in (("castle", baan), ("test copy", tests[wereld])):
            cijfers = bouw.controleer(b)
            if naam == "castle":
                print(f"guhrio_w1 level {wereld}: {cijfers}")
        if _kaal(baan) != _kaal(tests[wereld]):
            missing.append(f"the test copy of level {wereld} is not the level of the castle")
    # every tip a level uses has a text, every text a tip
    gebruikt = {int(v[1]["tip"]) for b in bouw.KASTEEL.values() for k, v in b.cellen.items() if v[0] == bouw.TIP}
    if gebruikt != set(range(len(TIPS))):
        missing.append(f"tips used by the levels {sorted(gebruikt)} / tips with a text 0..{len(TIPS) - 1}")
    # Java's numbers are these numbers
    java = open("src/main/java/nl/juiced/guhs/feature/guhriow1/Binnentuin.java", encoding="utf-8").read()
    baan = bouw.KASTEEL.get("1-2")
    if baan is not None:
        padguh = [(s, y, d, nbt) for s, y, d, nbt in baan.wezens if nbt.get("Kind") == "padguh"]
        if len(padguh) != 1:
            missing.append(f"one Pad-guh at the end of 1-2, found {len(padguh)}")
        else:
            x, y, z = baan.bouw(padguh[0][0], padguh[0][1], padguh[0][2])
            if f"PADGUH = new BlockPos({x}, {y}, {z})" not in java:
                missing.append(f"Binnentuin.PADGUH must be new BlockPos({x}, {y}, {z})")
            yaw = bouw._camera_yaw(baan)
            if f"PADGUH_YAW = {int(yaw)}f" not in java:
                missing.append(f"Binnentuin.PADGUH_YAW must be {int(yaw)}f")
    if not re.search(r"TIPS = %d;" % len(TIPS), java):
        missing.append(f"Binnentuin.TIPS must be {len(TIPS)}")
    alweer = len([k for k in TEXTS if k.startswith("quest.guhs.guhriow1.padguh.alweer.")])
    if not re.search(r"ALWEER = %d;" % alweer, java):
        missing.append(f"Binnentuin.ALWEER must be {alweer}")
    if missing:
        raise SystemExit(f"guhrio_w1: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    texts(h)
    advancements(h)
    selfcheck(h, testlevels(h))
