"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.7), slice sp_d2_weg: part 2 steps 5-8 of sponsguh_kroon on the road from De Ruige Kroeg to
Schelpenstad: the moustaches (wink 9 first), the loopgraaf and its singing monsters, Dennis and the moustache (the required
joke), the Cycloop and the tear. Java: nl.juiced.guhs.feature.einde2.spd2weg (E2SpD2WegFeature). Chunk c116_e2_sp_d2_weg.

  STAPPEN / EXTRA / KORT   the step texts (the skelet's einde2_sp_verhaal writes them with the line)
  texts                    the seven scenes, the three look scenes of the hints, the chat of the minigames and the clicks,
                           the moustache (einde2_sp_d2_weg_tekst.py; English in tools/lang/en/c116_e2_sp_d2_weg.json)
  BONES / clothes / icons  the guh clothes EINDE2_SP_D2_WEG_SNOR: a curly moustache of knabbelkoordraad (EYES slot, a bone on
                           the head), its swatch and its item icon
  ftb                      one quest per step (einde2_sp_d2_weg_k5..k8), FTB_LINEAIR; the skelet's FTB_SECTIES places them

No build additions (einde2_sp_d2_weg_plekken.py stays empty): every scene and game reads the phase-A markers of Schelpenstad
(and two template spots of the build: the drying room's trail and the pad points of dennis_route / pad_omlaag).
"""
import numpy as np

import features.einde2_kit as kit
from features import einde2_sp_d2_weg_tekst as tekst
from features import verhaal_motor

LIJN = "sponsguh_kroon"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True
SCENE = "einde2_sp_d2_weg_"
SNOR = "einde2_sp_d2_weg_snor"

# the step texts of this slice: what to do, where (a direction and a landmark), and why (playtest 1)
STAPPEN = {LIJN: {
        5: ("Vanaf hier lopen we",
            "De Knabbelburgermobiel kan niet over de loopgraaf. Loop met Sponsguh en Patguh over de weg naar het oosten, de heuvel op, "
            "tot de weg ophoudt bij een diepe, donkere kloof. Aan de rand wacht iemand op jullie.",
            "De westrand van de loopgraaf, oost van De Ruige Kroeg"),
        6: ("Door de loopgraaf",
            "Daal het zigzagpad langs de westwand af naar de donkere bodem. Daar wonen de monsters... Met een snor ben je nergens bang "
            "voor. Toch?",
            "Het zigzagpad omlaag, aan de westwand van de loopgraaf"),
        7: ("Dennis-Mika",
            "Klim boven aan de oostrand van de loopgraaf de weg op. Er komt een motor aan...",
            "De oostrand van de loopgraaf, waar de weg naar Schelpenstad begint"),
        8: ("De souvenirwinkel",
            "Die enorme laars... De kroon ligt vast in de souvenirwinkel van de Cycloop: het grote gebouw met de reuzenschelp op het dak, "
            "aan de noordkant van het plein van Schelpenstad. Ga naar binnen.",
            "De souvenirwinkel (reuzenschelp op het dak), noord aan het plein van Schelpenstad"),
}}
EXTRA = {LIJN: {
        "6_b": ("De monsters zongen jullie uitgeleide. Steek de bodem over en klim het pad langs de oostwand omhoog.",
                "Het pad omhoog aan de oostwand van de loopgraaf"),
        "7_b": ("Rennen! Dennis-Mika zit achter je aan. Volg de weg de heuvel af naar het oosten, door de stadspoort van Schelpenstad.",
                "De stadspoort van Schelpenstad, oost aan de weg"),
        "8_b": ("Onder de drooglamp! Kantel de droogtafel met WASD en rol Patguhs traan over het natte spoor naar het stopcontact aan de muur.",
                "De droogtafel achter in de souvenirwinkel"),
}}
KORT = {LIJN: {
        "5": "Loop naar de rand van de loopgraaf",
        "6": "Daal af in de loopgraaf",
        "6_b": "Klim aan de oostkant omhoog",
        "7": "De oostrand van de loopgraaf op",
        "7_b": "Ren naar de stadspoort!",
        "8": "De souvenirwinkel in",
        "8_b": "Rol de traan naar het stopcontact",
}}

# =====================================================================================================================
# the moustache for guhs (make_guh_variants: BONES / clothes; make_clothes_icons: icons; make_resources: CLOTHES item models)
# =====================================================================================================================
_H = [0, 6, -2]
BONES = {
    # a twisted cord of knabbelkoordraad under the nose: a knot in the middle, two halves, the ends curl up into little loops
    "outfit_einde2_sp_d2_weg_snor": ("head", _H, SNOR, [
        ([-1.0, 3.5, -13.7], [2.0, 1.7, 1.1], 0),
        ([-5.4, 3.3, -13.4], [4.5, 1.4, 0.8], 0), ([0.9, 3.3, -13.4], [4.5, 1.4, 0.8], 0),
        ([-6.7, 3.9, -13.3], [1.3, 2.1, 0.7], 0), ([5.4, 3.9, -13.3], [1.3, 2.1, 0.7], 0),
        ([-6.1, 5.7, -13.3], [1.1, 0.9, 0.7], 0), ([5.0, 5.7, -13.3], [1.1, 0.9, 0.7], 0)]),
}
CLOTHES = [SNOR]

KOORD, KOORD_DONKER, KOORD_LICHT = (214, 170, 74), (150, 108, 40), (246, 214, 132)


def clothes(rng, v):
    r = kit.rng("sp_d2_weg/snor")

    def koord():
        a = v.fabric(KOORD, r, 10).astype(np.float32)
        px = a.shape[0]
        for y in range(px):
            for x in range(px):
                # a twisted cord: diagonal strands, a dark groove between them, a light ridge on each
                f = (x + y) % 8
                if f in (0, 1):
                    a[y, x] = KOORD_DONKER
                elif f == 4:
                    a[y, x] = KOORD_LICHT
        return np.clip(a, 0, 255)
    return {SNOR: {SNOR: koord}}


SNOR_ICON = ["................", "................", "................", "................", "................",
             ".a............a.", "ab............ba", "ab..aaa..aaa..ba", ".abbbbcbbcbbbba.", "..abbcbbcbbcba..",
             "...aaaaaaaaaa...", "................", "................", "................", "................", "................"]


def icons(ic):
    return {SNOR: ic.icon(SNOR_ICON, {"a": KOORD_DONKER, "b": KOORD, "c": KOORD_LICHT})}


# =====================================================================================================================
# texts
# =====================================================================================================================
def texts(h):
    for key, text in tekst.TEKSTEN.items():
        kit.lang(h, key, text)
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        verhaal_motor.scene(h, SCENE + naam, titel, regels, namen)
    for naam, (titel, regel) in tekst.KIJK.items():
        verhaal_motor.scene(h, SCENE + "kijk_" + naam, titel, {"tekst": regel})


def build(h):
    texts(h)
    selfcheck(h)


def ftb(fq):
    for key, titel, text, icon, adv in tekst.FTB:
        fq.q(key, titel, text, icon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 8),))


def selfcheck(h):
    problems = []
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        if f"scene.guhs.{SCENE}{naam}.titel" not in h.NL:
            problems.append(f"scene {naam}: no title")
    for naam in tekst.KIJK:
        if f"scene.guhs.{SCENE}kijk_{naam}.tekst" not in h.NL:
            problems.append(f"look scene {naam}: no caption")
    if "Dennis" not in tekst.SCENES["dennis_snor"][0] or "snor" not in tekst.SCENES["dennis_snor"][0]:
        problems.append("the required joke lost its scene (dennis_snor)")
    if "herkent niemand ons" not in tekst.SCENES["snor"][1]["herkent"]:
        problems.append("the setup of the joke is gone from snor")
    if sorted(STAPPEN[LIJN]) != [5, 6, 7, 8] or sorted(EXTRA[LIJN]) != ["6_b", "7_b", "8_b"]:
        problems.append("the steps and variants differ from CONTRACT_E2_C 2.2 / 2.3")
    for wie, lijnen in tekst.KLIK.items():
        for s in ("5", "6", "6_b", "7", "7_b", "8", "8_b"):
            if len(lijnen.get(s, [])) != 2:
                problems.append(f"click lines {wie} {s}")
    if problems:
        raise SystemExit("einde2_sp_d2_weg self-check: " + "; ".join(problems))
