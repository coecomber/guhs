"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.7), slice sp_d2_weg: part 2 steps 5-8: the moustaches (wink 9), the loopgraaf and its singing monsters, Dennis and the moustache, the Cycloop and the tear.
Java: nl.juiced.guhs.feature.einde2.spd2weg (E2SpD2WegFeature). Chunk c116_e2_sp_d2_weg.

STUB written by the skelet (einde2-sponsguh-skelet). This module is yours: replace what you need, keep build(h) and ftb(fq).
The skelet's einde2_sp_verhaal reads STAPPEN / EXTRA / KORT below and writes them with the line (verhaal_motor.verhaallijn):
keep the names and exactly your steps 5..8 of sponsguh_kroon; rewrite the texts (what to do, where, a direction and a
landmark, CONTRACT_E2_C 5). EXTRA holds your reserved variants "<stap>_b". FTB: one quest per step, keys einde2_sp_d2_weg_k<stap>
(the skelet's sections name them), task fq.adv("sponsguh_kroon_stap_<stap + 1>"), FTB_LINEAIR.
"""

LIJN = "sponsguh_kroon"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True

# the step texts of this slice (skelet drafts)
STAPPEN = {LIJN: {
        5: ("De loopgraaf",
            "De weg houdt op bij een diepe, donkere kloof: de loopgraaf. Loop naar de rand en kijk wie daar op jullie wacht.",
            "De westrand van de loopgraaf"),
        6: ("Door de loopgraaf",
            "Daal het pad af, steek de bodem over en klim aan de overkant weer omhoog. Met een snor ben je nergens bang voor... toch?",
            "Het pad omhoog aan de oostkant van de loopgraaf"),
        7: ("Dennis",
            "Er komt een motor aan... Loop over de weg naar Schelpenstad.",
            "De weg naar Schelpenstad, oost van de loopgraaf"),
        8: ("Schelpenstad",
            "De kroon ligt in de souvenirwinkel van de Cycloop, aan het plein van Schelpenstad. Ga naar binnen.",
            "De souvenirwinkel van de Cycloop, aan het plein van Schelpenstad"),
}}
EXTRA = {LIJN: {
        "6_b": ("Ze zingen mee! Klim nu aan de overkant omhoog.",
            "Het pad omhoog aan de oostkant van de loopgraaf"),
        "7_b": ("Rennen! Dennis zit achter je aan. Volg de weg naar de stadspoort van Schelpenstad.",
            "De stadspoort van Schelpenstad"),
        "8_b": ("Onder de drooglamp! Help de traan over de tafel naar het stopcontact.",
            "De droogtafel in de souvenirwinkel"),
}}
KORT = {LIJN: {
        "5": "Naar de rand van de loopgraaf",
        "6": "Door de loopgraaf",
        "6_b": "Klim aan de overkant omhoog",
        "7": "Over de weg naar Schelpenstad",
        "7_b": "Ren naar Schelpenstad!",
        "8": "De souvenirwinkel in",
        "8_b": "De traan naar het stopcontact",
}}


def build(h):
    pass


def ftb(fq):
    pass
