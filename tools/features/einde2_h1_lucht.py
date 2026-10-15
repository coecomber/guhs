"""
einde2 phase B (CONTRACT_E2_B 7.2), slice h1_lucht: chapter 1 of the Guhvatar story (the kaasbol, the opening film, the
Zuidelijke Luchttempel, Zuka at the pier), the recipe of the Oog van Vadsig, the travel map of the whole story.
Java: nl.juiced.guhs.feature.einde2.h1lucht (E2H1LuchtFeature). Chunk c92_e2_h1_lucht.

  texts      every Dutch text (einde2_h1_lucht_tekst): the questline guhvatar_lucht, the narrator card, the eight scenes,
             the travel map "guhvatar", the talks, the minigames, the rewards
  looks      the kaasbol and the luchtbal ball (GeckoLib), the luchtscooter toy block, the Luchtnomadengewaad
             (einde2_h1_lucht_modellen; the clothes through the hooks clothes() / icons() / BONES)
  recipe     guhs:oog_van_vadsig = guhkristal + kaasknabbel + Mika-traan + grillspiespoeder (DESIGN 1: patched on top of
             guheinde.py's recipe; Eyes already made and filled portal frames stay valid: the item is the same)
  ftb        the section "De Guhvatar: Lucht": one quest per step, in order (FTB_LINEAIR)
The pier, Zuka's ship and the markers of the escape are in the build of the temple (einde2_bouw_lucht_bouw: steiger,
zuka_schip_laag), which this slice owns too.
"""
import json
import os

from PIL import Image

import features.einde2_kit as kit
from features import verhaal_motor as vm
from features import einde2_h1_lucht_modellen as modellen
from features import einde2_h1_lucht_tekst as tekst

FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True
FTB_SECTIES = [("einde2_h1_lucht", tekst.FTB_TITEL, "npc:aang_guh", None)]

BONES = modellen.BONES
clothes = modellen.clothes
icons = modellen.icons

LIJN = tekst.LIJN_ID
STAPPEN = len(tekst.LIJN["stappen"])
SCENES = list(tekst.SCENES)
WEZENS = ["kaasbol", "einde2_h1_lucht_luchtbal"]


# =====================================================================================================================
# looks
# =====================================================================================================================
def looks(h):
    A = h.A
    kaas, gloed = modellen.kaas_textuur()
    h.save(kaas, "entity", "kaasbol.png")
    h.save(gloed, "entity", "kaasbol_gloed.png")
    h.w(f"{A}/geckolib/models/entity/kaasbol.geo.json", modellen.kaasbol_geo())
    h.w(f"{A}/geckolib/animations/entity/kaasbol.animation.json", modellen.kaasbol_anim())
    wind, wgloed = modellen.wind_textuur()
    h.save(wind, "entity", "einde2_h1_lucht_luchtbal.png")
    h.save(wgloed, "entity", "einde2_h1_lucht_luchtbal_gloed.png")
    h.w(f"{A}/geckolib/models/entity/einde2_h1_lucht_luchtbal.geo.json", modellen.luchtbal_geo())
    h.w(f"{A}/geckolib/animations/entity/einde2_h1_lucht_luchtbal.animation.json", modellen.luchtbal_anim())
    for id in WEZENS:
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})
    # the toy block
    voet, bal = modellen.scooter_textures()
    h.save(voet, "block", "luchtscooter_voet.png")
    h.save(bal, "block", "luchtscooter.png")
    h.w(f"{A}/models/block/luchtscooter.json", modellen.scooter_model())
    h.w(f"{A}/blockstates/luchtscooter.json", {"variants": {"": {"model": "guhs:block/luchtscooter"}}})
    h.w(f"{A}/models/item/luchtscooter.json", {"parent": "guhs:block/luchtscooter"})
    h.self_drop("luchtscooter")
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:luchtscooter"])


# =====================================================================================================================
# texts
# =====================================================================================================================
def texts(h):
    for key, text in tekst.TEXTS.items():
        kit.lang(h, key, text)
    l = tekst.LIJN
    vm.verhaallijn(h, LIJN, l["naam"], l["uitleg"], stappen=l["stappen"], klaar=l["klaar"], extra=l["extra"], kort=l["kort"])
    titel, regels = tekst.KAART
    vm.vertelkaart(h, "einde2_h1_lucht", titel, regels, modellen.kaart_hoofdstuk(vm))
    vm.reiskaart(h, "guhvatar", modellen.kaart_reis(vm), tekst.REISKAART)
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        vm.scene(h, f"einde2_h1_lucht_{naam}", titel, regels, namen)
    vm.sluier(h, "zuidelijke_luchttempel")
    kit.zichtbaar(h, "einde2_h1_lucht_geleerd", "guheinde_winst", "guhs:luchtscooter", "goal",
                  tekst.TEXTS["advancements.guhs.guheinde.einde2_h1_lucht_geleerd.title"],
                  tekst.TEXTS["advancements.guhs.guheinde.einde2_h1_lucht_geleerd.description"])


# =====================================================================================================================
# the recipe of the Oog van Vadsig (DESIGN 1)
# =====================================================================================================================
OOG = ["guhs:guh_kristal", "guhs:kaas_knabbels", "guhs:mika_traan", "guhs:grillspiespoeder"]


def recept(h):
    pad = f"{h.D}/recipe/oog_van_vadsig.json"

    def vier(data):
        data["ingredients"] = list(OOG)
    h.patch_json(pad, vier)


# =====================================================================================================================
# FTB: one quest per step (quest i is done when the player reaches step i + 1)
# =====================================================================================================================
def ftb(fq):
    vorige = None
    for i, (key, titel, text, icon, stap) in enumerate(tekst.FTB):
        laatste = i == len(tekst.FTB) - 1
        fq.q(key, titel, text, icon, [fq.adv(f"{LIJN}_stap_{stap + 1}")], rewards=(("guhs:kaas_knabbels", 12 if laatste else 6),),
             deps=[vorige] if vorige else [], shape="gear" if laatste else None, xp=300 if laatste else 50)
        vorige = key


# =====================================================================================================================
def selfcheck(h):
    A, D = h.A, h.D
    problems = []
    for key in tekst.TEXTS:
        if key not in h.NL:
            problems.append(f"no text {key}")
    for path in (f"{A}/geckolib/models/entity/kaasbol.geo.json", f"{A}/geckolib/animations/entity/kaasbol.animation.json",
                 f"{A}/textures/entity/kaasbol.png", f"{A}/textures/entity/kaasbol_gloed.png",
                 f"{A}/geckolib/models/entity/einde2_h1_lucht_luchtbal.geo.json", f"{A}/textures/entity/einde2_h1_lucht_luchtbal.png",
                 f"{A}/blockstates/luchtscooter.json", f"{A}/models/block/luchtscooter.json", f"{A}/textures/block/luchtscooter.png",
                 f"{A}/textures/gui/verhaal/kaart_einde2_h1_lucht.png", f"{A}/textures/gui/verhaal/reiskaart_guhvatar.png",
                 f"{D}/loot_table/blocks/luchtscooter.json"):
        if not os.path.exists(path):
            problems.append(f"missing {path}")
    for naam in SCENES:
        if f"scene.guhs.einde2_h1_lucht_{naam}.titel" not in h.NL:
            problems.append(f"no title of the scene {naam}")
    recept = json.load(open(f"{D}/recipe/oog_van_vadsig.json", encoding="utf-8"))
    if sorted(recept.get("ingredients", [])) != sorted(OOG):
        problems.append(f"the Oog van Vadsig recipe is {recept.get('ingredients')}")
    # the Java side: the line's step count, the scene ids and the clothes constant
    J = os.path.join("src", "main", "java", "nl", "juiced", "guhs")
    lucht = open(os.path.join(J, "feature", "einde2", "h1lucht", "E2H1Lucht.java"), encoding="utf-8").read()
    if f"STAPPEN = {STAPPEN};" not in lucht:
        problems.append(f"E2H1Lucht.STAPPEN is not {STAPPEN}")
    scenes = open(os.path.join(J, "feature", "einde2", "h1lucht", "H1Scenes.java"), encoding="utf-8").read()
    for naam in SCENES:
        if f'maak("{naam}")' not in scenes:
            problems.append(f"H1Scenes has no scene {naam}")
    kleren = open(os.path.join(J, "entity", "GuhClothes.java"), encoding="utf-8").read()
    blok = kleren[kleren.find("// <einde2_h1_lucht>"):kleren.find("// </einde2_h1_lucht>")]
    if "    GUHVATAR_LUCHTNOMADE(" not in blok:
        problems.append("GuhClothes has no GUHVATAR_LUCHTNOMADE between the einde2_h1_lucht markers")
    if problems:
        raise SystemExit("einde2_h1_lucht self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    looks(h)
    texts(h)
    recept(h)
    selfcheck(h)
