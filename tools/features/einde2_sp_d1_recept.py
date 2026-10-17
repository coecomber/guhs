"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.5), slice sp_d1_recept: part 1 steps 7-10: Plan Z, the sneak through De Vetemmer (wink 6), the recipe back, the drum in the Bellenkom and the song; part 1's rewards.
Java: nl.juiced.guhs.feature.einde2.spd1recept (E2SpD1ReceptFeature). Chunk c114_e2_sp_d1_recept.

STUB written by the skelet (einde2-sponsguh-skelet). This module is yours: replace what you need, keep build(h) and ftb(fq).
The skelet's einde2_sp_verhaal reads STAPPEN / EXTRA / KORT below and writes them with the line (verhaal_motor.verhaallijn):
keep the names and exactly your steps 7..10 of sponsguh_recept; rewrite the texts (what to do, where, a direction and a
landmark, CONTRACT_E2_C 5). EXTRA holds your reserved variants "<stap>_b" and "klaar" (you own the line's last step). FTB: one quest per step, keys einde2_sp_d1_recept_r<stap>
(the skelet's sections name them), task fq.adv("sponsguh_recept_stap_<stap + 1>"), FTB_LINEAIR.
"""

LIJN = "sponsguh_recept"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True

# the step texts of this slice (skelet drafts)
STAPPEN = {LIJN: {
        7: ("Plan Z",
            "Terug naar De Krokante Knabbel: Meneer Krabguh roept iedereen bij elkaar. Er is iets vreselijks gebeurd met de kluis in zijn kantoor.",
            "Het kantoor van Meneer Krabguh, achter in De Krokante Knabbel"),
        8: ("Inbreken in De Vetemmer",
            "Plankmika heeft het geheime recept! Sluip De Vetemmer in (de grote emmer aan de overkant van De Krokante Knabbel), naar het lab achterin. Blijf uit het zicht van de camera's en stap niet door de lasers.",
            "De Vetemmer, de emmer aan de overkant van De Krokante Knabbel"),
        9: ("Het recept terug",
            "Breng het geheime recept terug naar Meneer Krabguh in De Krokante Knabbel.",
            "Meneer Krabguh in De Krokante Knabbel"),
        10: ("De Bellenkom",
            "Vanavond is het feest in de Bellenkom, het grote stadion in het noorden. Octo moet optreden en heeft geen band... Klim op het podium en pak de trommel!",
            "Het podium van de Bellenkom, het stadion in het noorden"),
}}
EXTRA = {LIJN: {
        "8_b": ("Je hebt het recept! Sluip terug naar buiten zonder dat Karen het merkt.",
            "De uitgang van De Vetemmer"),
        "10_b": ("Sla op de trommel als de bellen op de maat knappen. Een paar missen geeft niks: de stad zingt toch wel mee.",
            "Het podium van de Bellenkom"),
        "klaar": ("Het geheime recept is terug en Octo is een ster! Meneer Krabguh heeft binnenkort groot nieuws...",
            "De Krokante Knabbel"),
}}
KORT = {LIJN: {
        "7": "Terug naar Krabguh's kantoor",
        "8": "Sluip De Vetemmer in",
        "8_b": "Sluip terug naar buiten",
        "9": "Recept terug naar Krabguh",
        "10": "Naar het podium van de Bellenkom",
        "10_b": "Trommelen op de maat!",
        "klaar": "Op naar deel 2",
}}


def build(h):
    pass


def ftb(fq):
    pass
