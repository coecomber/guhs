"""
einde2 "Sponsguh" (CONTRACT_E2_C 4.1), the skelet: the one hook in the two builds of phase A. einde2_bouw_bikinibodem.maak()
and einde2_bouw_schelpenstad_bouw.bouw() call bikinibodem(b) / schelpenstad(b) right before their self-check; each Sponsguh
slice that needs markers, scene cameras or a small prop in a build adds them in its OWN einde2_sp_<slice>_plekken.py. The
order below is fixed; nobody edits this file.
"""
from features import einde2_sp_d1_start_plekken
from features import einde2_sp_d1_stad_plekken
from features import einde2_sp_d1_recept_plekken
from features import einde2_sp_d2_heen_plekken
from features import einde2_sp_d2_weg_plekken
from features import einde2_sp_d2_finale_plekken
from features import einde2_sp_zij_plekken

EIGENAARS = [einde2_sp_d1_start_plekken, einde2_sp_d1_stad_plekken, einde2_sp_d1_recept_plekken, einde2_sp_d2_heen_plekken, einde2_sp_d2_weg_plekken, einde2_sp_d2_finale_plekken, einde2_sp_zij_plekken]


def bikinibodem(b):
    for m in EIGENAARS:
        m.bikinibodem(b)


def schelpenstad(b):
    for m in EIGENAARS:
        m.schelpenstad(b)
