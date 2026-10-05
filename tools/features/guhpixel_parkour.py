"""
Guhpixel slice "parkour" (Java: feature/guhpixel/parkour; namespaces guhparkour; English: tools/lang/en/c39_px_parkour.json).

STUB from the foundation: the slice fills it in. It builds: the Guh-parkour: Startpaaltje, Finishpaaltje, the obstacles, the scorebord and the route runner for 0-4 guhs (DESIGN_PX section 6). Independent of the dimension.
Helpers: tools/features/guhpixel_lib.py (deco, muurdeco, npc, geluid, quest_adv, test_kamer, teksten, controleer); own helper
modules are tools/features/guhpixel_parkour_*.py (_bouw, _tex, _modellen, _tekst, _geluid). Every text is Dutch here.
"""
from features import guhpixel_lib as lib

TEXTS = {}


def build(h):
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_parkour", keys=TEXTS)


def ftb(fq):
    pass
