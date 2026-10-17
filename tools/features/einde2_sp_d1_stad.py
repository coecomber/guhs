"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.4), slice sp_d1_stad: part 1 steps 5-6: jellyfishing with Patguh (wink 7), the boating exam with juf Pufguh (wink 5).
Java: nl.juiced.guhs.feature.einde2.spd1stad (E2SpD1StadFeature). Chunk c113_e2_sp_d1_stad.

STUB written by the skelet (einde2-sponsguh-skelet). This module is yours: replace what you need, keep build(h) and ftb(fq).
The skelet's einde2_sp_verhaal reads STAPPEN / EXTRA / KORT below and writes them with the line (verhaal_motor.verhaallijn):
keep the names and exactly your steps 5..6 of sponsguh_recept; rewrite the texts (what to do, where, a direction and a
landmark, CONTRACT_E2_C 5). EXTRA holds your reserved variants "<stap>_b". FTB: one quest per step, keys einde2_sp_d1_stad_r<stap>
(the skelet's sections name them), task fq.adv("sponsguh_recept_stap_<stap + 1>"), FTB_LINEAIR.
"""

LIJN = "sponsguh_recept"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True

# the step texts of this slice (skelet drafts)
STAPPEN = {LIJN: {
        5: ("Kwallen vangen",
            "Patguh wil kwallen vangen! Loop naar Patguh, die met de netjes bij de kwallenvelden aan de rand van de stad staat.",
            "Patguh met de netjes, bij de kwallenvelden aan de rand van de stad"),
        6: ("Het bootrijexamen",
            "Juf Pufguh's bootschool ligt aan de westkant van de stad, bij het pylonenparcours. Sponsguh doet weer examen, en jij gaat hem helpen.",
            "De bootschool van juf Pufguh, west in de stad"),
}}
EXTRA = {LIJN: {
        "5_b": ("Vang kruimelkwallen met je netje: tik zachtjes tegen een gloeiende kwal. Acht stuks voor Patguh!",
            "De kwallenvelden"),
        "6_b": ("Stuur het vetbootje langs alle pylonen, over het bruggetje en terug naar de finish. Rustig aan: Sponsguh gilt genoeg voor twee.",
            "Het pylonenparcours van de bootschool"),
}}
KORT = {LIJN: {
        "5": "Naar Patguh bij de kwallenvelden",
        "5_b": "Vang acht kruimelkwallen",
        "6": "Naar de bootschool (west)",
        "6_b": "Vaar het pylonenparcours",
}}


def build(h):
    pass


def ftb(fq):
    pass
