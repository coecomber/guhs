"""
Guhpixel slice "bioscoop" (Java: feature/guhpixel/bioscoop; namespaces guhbioscoop; English: tools/lang/en/c37_px_bioscoop.json).

STUB from the foundation: the slice fills it in. It builds: the Guhbioscoop: projector, doek (up to 7 x 4), stoeltje, popcornmachine, the film player and all nine films (DESIGN_PX section 4).
Helpers: tools/features/guhpixel_lib.py (deco, muurdeco, npc, geluid, quest_adv, test_kamer, teksten, controleer); own helper
modules are tools/features/guhpixel_bioscoop_*.py (_bouw, _tex, _modellen, _tekst, _geluid). Every text is Dutch here.
"""
from features import guhpixel_lib as lib

TEXTS = {}


def build(h):
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_bioscoop", keys=TEXTS)


def ftb(fq):
    pass
