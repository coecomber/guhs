"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.6), slice sp_d2_heen: part 2 steps 0-4: the manager, the Guhfy Guhber ice cream (wink 8), the crown and the frozen Krabguh, the drive, the key in De Ruige Kroeg.
Java: nl.juiced.guhs.feature.einde2.spd2heen (E2SpD2HeenFeature). Chunk c115_e2_sp_d2_heen.

STUB written by the skelet (einde2-sponsguh-skelet). This module is yours: replace what you need, keep build(h) and ftb(fq).
The skelet's einde2_sp_verhaal reads STAPPEN / EXTRA / KORT below and writes them with the line (verhaal_motor.verhaallijn):
keep the names and exactly your steps 0..4 of sponsguh_kroon; rewrite the texts (what to do, where, a direction and a
landmark, CONTRACT_E2_C 5). EXTRA holds your reserved variants "<stap>_b". FTB: one quest per step, keys einde2_sp_d2_heen_k<stap>
(the skelet's sections name them), task fq.adv("sponsguh_kroon_stap_<stap + 1>"), FTB_LINEAIR.
"""

LIJN = "sponsguh_kroon"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True

# the step texts of this slice (skelet drafts)
STAPPEN = {LIJN: {
        0: ("De nieuwe manager",
            "Meneer Krabguh heeft groot nieuws voor iedereen. Ga naar De Krokante Knabbel.",
            "De Krokante Knabbel"),
        1: ("Een ijsje",
            "Sponsguh is een beetje verdrietig. Patguh weet raad: ijs! Ga naar de Guhfy Guhber-ijssalon, west van het plein, en neem plaats aan de bar.",
            "De Guhfy Guhber-ijssalon, west van het plein"),
        2: ("De kroon is weg!",
            "Er klinkt een hoorn op het plein. Koning Neptuguh is in de stad, en hij is woedend. Ga snel naar het plein.",
            "De troon van koning Neptuguh op het plein"),
        3: ("De Knabbelburgermobiel",
            "Stap in de Knabbelburgermobiel voor De Krokante Knabbel, rij de stadspoort uit (oost) en volg de weg naar De Ruige Kroeg.",
            "De Ruige Kroeg, het roestige sleepbootje aan de weg naar Schelpenstad"),
        4: ("De Ruige Kroeg",
            "Een stoere Mika heeft de sleutel van de Knabbelburgermobiel gepakt en is in slaap gevallen aan zijn tafel. Pak hem terug zonder dat iemand je ziet bewegen.",
            "De slapende Mika in De Ruige Kroeg"),
}}
EXTRA = {LIJN: {
        "3_b": ("Rij over de weg naar het oosten. Blijf op de weg: de Knabbelburgermobiel stopt vanzelf bij een rand.",
            "De weg naar De Ruige Kroeg"),
        "4_b": ("Sta stil als de Mika's opkijken! Beweeg alleen als niemand kijkt.",
            "De slapende Mika in De Ruige Kroeg"),
}}
KORT = {LIJN: {
        "0": "Naar De Krokante Knabbel",
        "1": "IJs in de Guhfy Guhber (west)",
        "2": "Naar het plein!",
        "3": "Rij naar De Ruige Kroeg (oost)",
        "3_b": "Blijf op de weg naar het oosten",
        "4": "Pak de sleutel terug",
        "4_b": "Stilstaan als ze kijken!",
}}


def build(h):
    pass


def ftb(fq):
    pass
