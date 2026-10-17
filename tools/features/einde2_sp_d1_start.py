"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.3), slice sp_d1_start: part 1 steps 0-4: the bubble letter, the trip to Bikinibodem, the bubble stream and the welcome, the job interview, baking Knabbelburgers.
Java: nl.juiced.guhs.feature.einde2.spd1start (E2SpD1StartFeature). Chunk c112_e2_sp_d1_start.

STUB written by the skelet (einde2-sponsguh-skelet). This module is yours: replace what you need, keep build(h) and ftb(fq).
The skelet's einde2_sp_verhaal reads STAPPEN / EXTRA / KORT below and writes them with the line (verhaal_motor.verhaallijn):
keep the names and exactly your steps 0..4 of sponsguh_recept; rewrite the texts (what to do, where, a direction and a
landmark, CONTRACT_E2_C 5). EXTRA holds your reserved variants "<stap>_b". FTB: one quest per step, keys einde2_sp_d1_start_r<stap>
(the skelet's sections name them), task fq.adv("sponsguh_recept_stap_<stap + 1>"), FTB_LINEAIR.
"""

LIJN = "sponsguh_recept"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True

# the step texts of this slice (skelet drafts)
STAPPEN = {LIJN: {
        0: ("De bubbelbrief",
            "Er drijft een zeepbel met een briefje naar je toe. Klik erop om hem te laten knappen.",
            "Overal in het Guheinde (de bubbel vindt jou)"),
        1: ("Naar Bikinibodem",
            "Vlieg met Appa naar Bikinibodem (in zijn menu bij de overige plekken) of volg het Superkompas (tabblad einde). Land op het eilandje boven de grote zeepbel.",
            "Het eilandje boven de zeepbel van Bikinibodem, diep tussen de buiteneilanden"),
        2: ("De bubbelstroom",
            "Stap op het eilandje in de bubbelstroom die omlaag borrelt. Hij zet je zachtjes af op het plein van Bikinibodem.",
            "De bubbelstroom op het eilandje, omlaag naar het plein"),
        3: ("Een baantje",
            "Loop van het plein naar het oosten, naar De Krokante Knabbel (de deur zit aan de Hoofdstraat). Praat met Meneer Krabguh.",
            "De Krokante Knabbel, oost van het plein aan de Hoofdstraat"),
        4: ("Knabbelburgers bakken",
            "Ga in de keuken achter de grill staan. Klanten bestellen aan de besteltafels: kijk op het bonnetje en stapel de burger in de goede volgorde. Vijf goede bestellingen en je bent een echte knabbelbakker.",
            "De grill in de keuken van De Krokante Knabbel"),
}}
EXTRA = {LIJN: {
        "4_b": ("Bakken maar! Kijk op het bonnetje wat de klant wil en stapel het in die volgorde.",
            "De grill in de keuken van De Krokante Knabbel"),
}}
KORT = {LIJN: {
        "0": "Klik op de bubbelbrief",
        "1": "Naar Bikinibodem (Appa: overige plekken)",
        "2": "De bubbelstroom omlaag",
        "3": "Praat met Meneer Krabguh",
        "4": "Bak vijf Knabbelburgers",
        "4_b": "Stapel wat op het bonnetje staat",
}}


def build(h):
    pass


def ftb(fq):
    pass
