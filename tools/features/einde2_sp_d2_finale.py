"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.8), slice sp_d2_finale: part 2 steps 9-11: home on David Hasselguh, the bucket helmets, the song 'Ik ben een Guhfy Guhber'; part 2's rewards and the Sponsguh buddy.
Java: nl.juiced.guhs.feature.einde2.spd2finale (E2SpD2FinaleFeature). Chunk c117_e2_sp_d2_finale.

STUB written by the skelet (einde2-sponsguh-skelet). This module is yours: replace what you need, keep build(h) and ftb(fq).
The skelet's einde2_sp_verhaal reads STAPPEN / EXTRA / KORT below and writes them with the line (verhaal_motor.verhaallijn):
keep the names and exactly your steps 9..11 of sponsguh_kroon; rewrite the texts (what to do, where, a direction and a
landmark, CONTRACT_E2_C 5). EXTRA holds your reserved variants "<stap>_b" and "klaar" (you own the line's last step). FTB: one quest per step, keys einde2_sp_d2_finale_k<stap>
(the skelet's sections name them), task fq.adv("sponsguh_kroon_stap_<stap + 1>"), FTB_LINEAIR.
"""

LIJN = "sponsguh_kroon"
FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True

# the step texts of this slice (skelet drafts)
STAPPEN = {LIJN: {
        9: ("Naar huis",
            "Loop met de kroon naar de pier van Schelpenstad. Daar dobbert iemand heel groots in de leegte.",
            "De pier van Schelpenstad"),
        10: ("Emmerhelmen",
            "Bikinibodem loopt rond met Plankmika's emmerhelmen op! Blaas de emmers van de hoofden met je bellenblaas en loop naar de Bellenkom.",
            "De Bellenkom, het stadion in het noorden van Bikinibodem"),
        11: ("Ik ben een Guhfy Guhber",
            "Klim op het podium van de Bellenkom. Tijd voor het grootste nummer dat Bikinibodem ooit hoorde.",
            "Het podium van de Bellenkom"),
}}
EXTRA = {LIJN: {
        "10_b": ("Nog een paar emmers! Daarna snel naar het podium van de Bellenkom.",
            "De Bellenkom"),
        "klaar": ("De kroon is terug, Meneer Krabguh is ontdooid en Sponsguh is manager. Vahoeg!",
            "Bikinibodem"),
}}
KORT = {LIJN: {
        "9": "Naar de pier",
        "10": "Emmers eraf, naar de Bellenkom",
        "10_b": "Nog een paar emmers",
        "11": "Op het podium!",
        "klaar": "Vahoeg!",
}}


def build(h):
    pass


def ftb(fq):
    pass
