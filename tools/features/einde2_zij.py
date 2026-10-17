"""
einde2 phase B (CONTRACT_E2_B 7.6), slice zij: the side places of the Guheinde and their quests, the four lasting powers,
the Opper-Mika's entrance, the poem at the end of the Guheinde, wink 10.
Java: nl.juiced.guhs.feature.einde2.zij (E2ZijFeature). Texts: einde2_zij_tekst.py (Dutch); English:
tools/lang/en/c96_e2_zij.json. Wiki: einde2_zij_wiki.py. Chunk c96_e2_zij.

  lang      the eight lines (verhaal_motor), the five scenes, the books, dialogue, items, block, titles, the key, the powers
  assets    item textures + models (kristalsnoep-ring, zegenschub, spijtsoep), the rock block of the earth power (vanilla tuff, coarse dirt on top)
  data      recipe einde2_zij_spijtsoep; hidden advancements quest/einde2_zij_* (the FTB tasks of the extra quests)
  clothes   EINDE2_ZIJ_KYOSHI_SCHMINK (white face paint, red eye shadow, a golden crest): BONES + clothes() + icons()
  ftb       two sections in the Guheinde chapter: "Zijpaden van de Guhvatar" and "De Leegtehaven en de Mika-vesting"
"""
import os
import re

from features import einde2_kit as kit
from features import verhaal_motor, wereld
from features import einde2_zij_tekst as tekst

FTB_CHAPTER = "guhs_guheinde"
GUHVATAR_LIJNEN = ["omashguh", "westelijke_luchttempel", "vuurtempel_roku", "kyoshi_dorp", "bibliotheek_wan_shi_guh", "leeuwschildpad"]
HAVEN_LIJNEN = ["mika_kok", "vetbootje"]
ICOON = {"omashguh": "guhs:einde2_zij_kristalsnoep_ring", "westelijke_luchttempel": "guhs:einde2_deco_windgong",
         "vuurtempel_roku": "guhs:einde2_deco_eeuwige_vlam", "kyoshi_dorp": "guhs:einde2_zij_kyoshi_schmink",
         "bibliotheek_wan_shi_guh": "minecraft:written_book", "leeuwschildpad": "guhs:einde2_zij_zegenschub",
         "mika_kok": "guhs:einde2_zij_spijtsoep", "vetbootje": "guhs:vetbootje_fluitje"}


def _ftb_sleutels(lijnen):
    return [f"einde2_zij_{l}_{i}" for l in lijnen for i in range(1, len(tekst.LIJNEN[l]["stappen"]) + 1)]


FTB_SECTIES = [("einde2_zij", "Zijpaden van de Guhvatar", "npc:bumi_guh", _ftb_sleutels(GUHVATAR_LIJNEN) + ["einde2_zij_krachten"]),
               ("einde2_zij_haven", "De Leegtehaven en de Mika-vesting", "npc:veerguh", None)]
# the hidden advancements of the extra quests (granted in Java)
VERBORGEN = ["einde2_zij_kracht", "einde2_zij_handel", "einde2_zij_veer", "einde2_zij_ringenrace"]

# --- clothes: the Kyoshi make-up (a piece for the guh's face: slot EYES) ------------------------------------------------------
CLOTHES = ["einde2_zij_kyoshi_schmink"]
_H = [0, 6, -2]
BONES = {
    # white face paint: two cheek patches under the eyes and a band over the forehead
    "outfit_zij_schmink_wit": ("head", _H, "zij_schmink_wit", [([2.0, 4.4, -12.72], [5.0, 2.5, 0.3], 0), ([-7.0, 4.4, -12.72], [5.0, 2.5, 0.3], 0),
                                                              ([-5.6, 13.1, -12.62], [11.2, 1.3, 0.3], 0)]),
    # red eye shadow over both eyes with a little wing outwards, and the red lips
    "outfit_zij_schmink_rood": ("head", _H, "zij_schmink_rood", [([1.0, 11.9, -12.78], [6.2, 1.2, 0.3], 0), ([-7.2, 11.9, -12.78], [6.2, 1.2, 0.3], 0),
                                                                ([6.6, 10.6, -12.74], [1.0, 1.6, 0.3], 0), ([-7.6, 10.6, -12.74], [1.0, 1.6, 0.3], 0),
                                                                ([-1.2, 3.1, -12.74], [2.4, 0.8, 0.3], 0)]),
    # the golden crest of the headdress on top of the head
    "outfit_zij_schmink_goud": ("head", _H, "zij_schmink_goud", [([-0.7, 14.6, -10.8], [1.4, 3.6, 1.4], 0), ([-3.6, 14.4, -10.5], [2.2, 2.4, 0.8], 0),
                                                                ([1.4, 14.4, -10.5], [2.2, 2.4, 0.8], 0)]),
}
WIT, ROOD, GOUD = (246, 242, 236), (196, 28, 40), (232, 182, 56)
SCHMINK_ICON = ["................", "......gggg......", ".....gGGGGg.....", "....wwwwwwww....", "...wwwwwwwwww...", "..wrrrwwwwrrrw..",
                "..wrkrwwwwrkrw..", "..wwwwwwwwwwww..", "..wwwwwkkwwwww..", "...wwwwwwwwww...", "...wwwwrrwwww...", "....wwwwwwww....",
                ".....wwwwww.....", "................", "................", "................"]


def clothes(rng, v):
    """The texture of the make-up: three swatches (the hook's rng shifts with the module's place: kit.rng does not)."""
    r = kit.rng("zij/kleding")

    def goud():
        a = v.fabric(GOUD, r, 8)
        a[::6, :] = (176, 128, 30)
        return a
    return {"einde2_zij_kyoshi_schmink": {"zij_schmink_wit": (lambda: v.fabric(WIT, r, 4)), "zij_schmink_rood": (lambda: v.fabric(ROOD, r, 6)),
                                          "zij_schmink_goud": goud}}


def icons(ic):
    return {"einde2_zij_kyoshi_schmink": ic.icon(SCHMINK_ICON, {"w": WIT, "r": ROOD, "k": (40, 30, 30), "g": (176, 128, 30), "G": GOUD})}


# =====================================================================================================================
# item textures
# =====================================================================================================================
RING = ["................", "................", "......cCCc......", ".....cCppCc.....", "......cCCc......", ".......gg.......",
        ".....gg..gg.....", "....g......g....", "....g......g....", "....g......g....", ".....gg..gg.....", ".......gg.......",
        "................", "................", "................", "................"]
SCHUB = ["................", "......llll......", "....llGGGGll....", "...lGGgGGgGGl...", "..lGGggGGggGGl..", "..lGgGGLLGGgGl..",
         "..lGgGLLLLGgGl..", "..lGGGLLLLGGGl..", "...lGGgLLgGGl...", "...lGGgggGGGl...", "....lGGGGGGl....", ".....lGGGGl.....",
         "......lGGl......", ".......ll.......", "................", "................"]
SOEP = ["................", "................", "......s..s......", ".......s..s.....", "......s..s......", "................",
        "..bbbbbbbbbbbb..", ".bSSoSSSSoSSSSb.", ".bSSSSgSSSSgSSb.", "..bSoSSSSSSSSb..", "..bBBBBBBBBBBb..", "...bBBBBBBBBb...",
        "....bBBBBBBb....", ".....bbbbbb.....", "................", "................"]


def textures(h):
    h.save(h.grid(RING, {"c": (150, 220, 240, 255), "C": (210, 245, 255, 255), "p": (255, 150, 210, 255), "g": (226, 178, 60, 255)}),
           "item", "einde2_zij_kristalsnoep_ring.png")
    h.save(h.grid(SCHUB, {"l": (40, 80, 50, 255), "G": (96, 170, 96, 255), "g": (70, 130, 76, 255), "L": (200, 255, 210, 255)}),
           "item", "einde2_zij_zegenschub.png")
    h.save(h.grid(SOEP, {"s": (230, 230, 230, 200), "b": (90, 56, 30, 255), "B": (140, 92, 52, 255), "S": (220, 140, 60, 255),
                         "o": (250, 200, 90, 255), "g": (90, 160, 70, 255)}), "item", "einde2_zij_spijtsoep.png")


def blocks_and_items(h):
    for item in ("einde2_zij_kristalsnoep_ring", "einde2_zij_zegenschub", "einde2_zij_spijtsoep"):
        h.item_model(item)
    # the rock of the earth power (pillar and wall): vanilla tuff with coarse dirt on top, no item, drops nothing
    h.w(f"{h.A}/models/block/einde2_zij_aardzuil.json", {"parent": "minecraft:block/cube_column",
                                                         "textures": {"side": "minecraft:block/tuff", "end": "minecraft:block/coarse_dirt"}})
    h.w(f"{h.A}/blockstates/einde2_zij_aardzuil.json", {"variants": {"": {"model": "guhs:block/einde2_zij_aardzuil"}}})
    h.shapeless("einde2_zij_spijtsoep", ["minecraft:bowl", "guhs:schimmelpaddenstoel", "guhs:kristalkaas_brokje", "guhs:knabbelkoorvrucht"],
                "guhs:einde2_zij_spijtsoep")


# =====================================================================================================================
# texts
# =====================================================================================================================
def texts(h):
    for lijn, d in tekst.LIJNEN.items():
        verhaal_motor.verhaallijn(h, lijn, d["naam"], d["uitleg"], d["stappen"], klaar=d.get("klaar"), extra=d.get("extra"))
    for sid, d in tekst.SCENES.items():
        verhaal_motor.scene(h, sid, d["titel"], d["regels"], d["namen"])
    for bid, (titel, paginas) in tekst.BOEKEN.items():
        kit.lang(h, f"book.guhs.einde2_zij.{bid}.titel", titel)
        for i, p in enumerate(paginas):
            kit.lang(h, f"book.guhs.einde2_zij.{bid}.{i}", p)
    for key, text in tekst.TEKST.items():
        kit.lang(h, key, text)
    # fix round 1 (fix2-geestzij): the step hints and their look scenes (ZijHints.java)
    for k, v in tekst.HINTS.items():
        kit.lang(h, f"gui.guhs.einde2_zij.hint.{k}", v)
    for naam, (titel, onder) in tekst.KIJK.items():
        verhaal_motor.scene(h, f"einde2_zij_kijk_{naam}", titel, {"tekst": onder})
    for name in VERBORGEN:
        kit.verborgen(h, name)


# =====================================================================================================================
# FTB
# =====================================================================================================================
def ftb(fq):
    for lijn in GUHVATAR_LIJNEN + HAVEN_LIJNEN:
        d = tekst.LIJNEN[lijn]
        stappen = [(naam, nu, ICOON[lijn]) for naam, nu, _waar in d["stappen"]]
        wereld.ftb_questlijn(fq, "einde2_zij", lijn, stappen, eind=(("guhs:kaas_knabbels", 16),))
    for key, (titel, tekst_) in tekst.FTB_EXTRA.items():
        taak = {"krachten": fq.adv("einde2_zij_kracht"), "handel": fq.adv("einde2_zij_handel"), "veer": fq.adv("einde2_zij_veer"),
                "ringenrace": fq.adv("einde2_zij_ringenrace"), "vestingbevrijder": fq.adv("einde2_vesting_bevrijder")}[key]
        icoon = {"krachten": "minecraft:feather", "handel": "guhs:flitskruimel", "veer": "minecraft:oak_boat", "ringenrace": "guhs:guhvleugels",
                 "vestingbevrijder": "minecraft:iron_bars"}[key]
        fq.q(f"einde2_zij_{key}", titel, tekst_, icoon, [taak], rewards=(("guhs:kaas_knabbels", 12),), shape="diamond")


# =====================================================================================================================
# self-check: every text key the Java code names is written here
# =====================================================================================================================
JAVA = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "zij")


def selfcheck(h):
    problems = []
    for f in sorted(os.listdir(JAVA)):
        if not f.endswith(".java"):
            continue
        src = open(os.path.join(JAVA, f), encoding="utf-8").read()
        m = re.search(r'static final String Q = Zij\.Q \+ "([a-z_]+\.)"', src)
        prefixen = {"Zij.Q": "quest.guhs.einde2_zij.", "Zij.GUI": "gui.guhs.einde2_zij.", "Q": "quest.guhs.einde2_zij." + (m.group(1) if m else "")}
        if f == "Zij.java":
            prefixen["Q"] = "quest.guhs.einde2_zij."
        src = re.sub(r'static final String Q = Zij\.Q \+ "[a-z_]+\.";', "", src)
        for naam, prefix in prefixen.items():
            for lit, plus in re.findall(r'(?<![\w.])' + re.escape(naam) + r' \+ "([^"]+)"( \+)?', src):
                key = prefix + lit
                if plus:
                    if not any(k.startswith(key) for k in h.NL):
                        problems.append(f"{f}: no key starting with {key}")
                elif key not in h.NL:
                    problems.append(f"{f}: no key {key}")
    hints = open(os.path.join(JAVA, "ZijHints.java"), encoding="utf-8").read()
    for k in re.findall(r'T \+ "([a-z_.0-9]+)"', hints):
        if f"gui.guhs.einde2_zij.hint.{k}" not in h.NL:
            problems.append(f"ZijHints.java: no hint text {k}")
    for k in re.findall(r'kijk\("([a-z_0-9]+)"\)', hints):
        if f"scene.guhs.einde2_zij_kijk_{k}.titel" not in h.NL:
            problems.append(f"ZijHints.java: no look scene {k}")
    for p in ("textures/item/einde2_zij_kristalsnoep_ring.png", "textures/item/einde2_zij_zegenschub.png", "textures/item/einde2_zij_spijtsoep.png"):
        if not os.path.exists(os.path.join(h.A, p)):
            problems.append("missing " + p)
    if problems:
        raise SystemExit("einde2_zij self-check:\n  " + "\n  ".join(problems))


def build(h):
    textures(h)
    blocks_and_items(h)
    texts(h)
    selfcheck(h)
