"""
Guhpixel slice "kantoor" (Java: feature/guhpixel/kantoor; namespaces guhkantoor; English: tools/lang/en/c36_px_kantoor.json).

STUB from the foundation: the slice fills it in. It builds: the Guhkantoor: Prikklok, Bureautje, 8 real hours, loonstrookjes, the kwartaalrapport, werknemer van de maand (DESIGN_PX section 4).
Helpers: tools/features/guhpixel_lib.py (deco, muurdeco, npc, geluid, quest_adv, test_kamer, teksten, controleer); own helper
modules are tools/features/guhpixel_kantoor_*.py (_bouw, _tex, _modellen, _tekst, _geluid). Every text is Dutch here.
"""
from features import guhpixel_lib as lib

TEXTS = {}


def build(h):
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_kantoor", keys=TEXTS)


def ftb(fq):
    pass
