"""
Guhpixel slice "guhkade" (Java: feature/guhpixel/guhkade; namespaces guhkade; English: tools/lang/en/c35_px_guhkade.json).

STUB from the foundation: the slice fills it in. It builds: the Guhkade cabinets Flappy Guh and Mika-Pong, their screens, the top 5 per cabinet and the guhs that come and play (DESIGN_PX section 4).
Helpers: tools/features/guhpixel_lib.py (deco, muurdeco, npc, geluid, quest_adv, test_kamer, teksten, controleer); own helper
modules are tools/features/guhpixel_guhkade_*.py (_bouw, _tex, _modellen, _tekst, _geluid). Every text is Dutch here.
"""
from features import guhpixel_lib as lib

TEXTS = {}


def build(h):
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_guhkade", keys=TEXTS)


def ftb(fq):
    pass
