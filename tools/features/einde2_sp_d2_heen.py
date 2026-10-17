"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.6), slice sp_d2_heen: part 2 steps 0-4: the manager, the Guhfy Guhber ice cream (wink 8), the
crown and the frozen Krabguh, the drive, the key in De Ruige Kroeg; the narrator card of part 2.
Java: nl.juiced.guhs.feature.einde2.spd2heen (E2SpD2HeenFeature). Chunk c115_e2_sp_d2_heen.

  texts       einde2_sp_d2_heen_tekst.py: the step texts (STAPPEN / EXTRA / KORT below, read by einde2_sp_verhaal), the narrator
              card, the nine scenes (THE ice-cream line of CONTRACT_E2_C 5.3 in "ijssalon"), the five look scenes of the
              hints, the chat of the drive and of the stand-still game, the clicks, the FTB quests
  here        the narrator card and its drawn map (verhaal_motor.vertelkaart), the scene texts (verhaal_motor.scene), the
              lang lines, the five FTB quests (keys einde2_sp_d2_heen_k0..k4, section einde2_sp_deel2 of the skelet) and the
              English title of that section (the skelet names it, this slice writes its English: CONTRACT_E2_C 1.3)
  build       einde2_sp_d2_heen_plekken.py adds this slice's markers and scene cameras to the two builds
"""
import os

from features import einde2_kit as kit
from features import einde2_sp_d2_heen_tekst as tekst
from features import verhaal_motor

LIJN = "sponsguh_kroon"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True
KAART = "einde2_sp_d2_heen"

# the step texts of this slice (einde2_sp_verhaal reads these three)
STAPPEN = {LIJN: tekst.STAPPEN}
EXTRA = {LIJN: tekst.EXTRA}
KORT = {LIJN: tekst.KORT}

# the scenes this slice registers in Java (spd2heen.D2Scenes): checked against SCENES so no scene goes without its text
JAVA_SCENES = ["manager", "ijssalon", "knipoog_ijs", "bevroren", "rit", "kroeg", "baby", "sleutel_terug"]


# =====================================================================================================================
# the narrator card's map: Bikinibodem under its bubble, the gate, the road east past De Ruige Kroeg, the loopgraaf,
# Schelpenstad on the beach
# =====================================================================================================================
def kaart(k):
    paars = (186, 150, 206, 255)
    k.water([(0, 0), (256, 0), (256, 160), (0, 160)], kleur=(206, 184, 222, 255))          # the leegtemist as the sea
    k.land([(6, 96), (20, 52), (62, 36), (104, 50), (116, 92), (92, 132), (40, 140)], kleur=(232, 196, 110, 255))   # the cheese dish
    k.d.arc((14, 30, 112, 150), 180, 360, fill=(120, 170, 220, 255), width=2)              # the bubble dome
    for x, y in ((40, 96), (58, 88), (76, 100), (66, 112)):
        k.huisje(x, y, kleur=(240, 170, 90, 255))
    k.huisje(88, 82, kleur=(214, 70, 60, 255))                                              # De Krokante Knabbel
    k.land([(112, 76), (250, 70), (252, 104), (112, 104)], kleur=(214, 186, 120, 255))      # the slab with the road
    k.land([(196, 70), (212, 70), (212, 104), (196, 104)], kleur=(96, 70, 60, 255))         # the loopgraaf
    k.land([(224, 64), (254, 60), (254, 112), (224, 116)], kleur=(240, 220, 170, 255))      # Schelpenstad's beach
    k.d.polygon([(140, 96), (172, 96), (166, 104), (146, 104)], fill=(150, 90, 60, 255), outline=k.INKT)   # De Ruige Kroeg (the tug)
    k.d.rectangle((156, 88, 160, 96), fill=(120, 80, 60, 255), outline=k.INKT)
    k.huisje(236, 92, kleur=(250, 210, 230, 255))
    k.huisje(246, 84, kleur=(250, 230, 190, 255))
    k.pad([(92, 88), (114, 88), (150, 88), (196, 86)])
    k.pad([(212, 86), (232, 90)], kleur=paars)
    k.kruis(240, 88)
    k.kompasroos(26, 24)


# =====================================================================================================================
# texts
# =====================================================================================================================
def texts(h):
    for key, text in tekst.TEKSTEN.items():
        kit.lang(h, key, text)
    verhaal_motor.vertelkaart(h, KAART, tekst.KAART_TITEL, tekst.KAART_REGELS, kaart)
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        verhaal_motor.scene(h, tekst.SCENE + naam, titel, regels, namen)
    for naam, (titel, regel) in tekst.KIJK.items():
        verhaal_motor.scene(h, tekst.SCENE + "kijk_" + naam, titel, {"tekst": regel})


# =====================================================================================================================
# FTB: one quest per step (keys of the skelet's section einde2_sp_deel2), in order (FTB_LINEAIR)
# =====================================================================================================================
def ftb(fq):
    for key, titel, text, icon, adv in tekst.FTB:
        fq.q(key, titel, text, icon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 6),))


def build(h):
    texts(h)
    selfcheck(h)


def selfcheck(h):
    problems = []
    if sorted(tekst.STAPPEN) != [0, 1, 2, 3, 4]:
        problems.append(f"steps {sorted(tekst.STAPPEN)} are not 0..4")
    if sorted(tekst.EXTRA) != ["3_b", "4_b"]:
        problems.append(f"variants {sorted(tekst.EXTRA)} are not the reserved 3_b, 4_b")
    for naam in JAVA_SCENES:
        if naam not in tekst.SCENES:
            problems.append(f"scene {naam} has no text")
        elif f"scene.guhs.{tekst.SCENE}{naam}.titel" not in h.NL:
            problems.append(f"scene {naam}: no title")
    # CONTRACT_E2_C 5.3: the user's line, verbatim
    if tekst.SCENES["ijssalon"][1].get("poppetje") != "zeg.. hoort hier geen poppetje bij?":
        problems.append("the ice-cream line is not the user's exact line")
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        for key, regel in regels.items():
            if not regel.strip():
                problems.append(f"scene {naam}: empty line {key}")
    if not os.path.exists(os.path.join(h.TEX, "gui", "verhaal", f"kaart_{KAART}.png")):
        problems.append("the narrator card's map is missing")
    if len(tekst.KAART_REGELS) not in (3, 4):
        problems.append("the narrator card wants 3-4 lines")
    if problems:
        raise SystemExit("einde2_sp_d2_heen self-check: " + "; ".join(problems))
