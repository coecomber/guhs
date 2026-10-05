"""
Guhpixel slice "among" (Java: feature/guhpixel/among; namespaces among; English: tools/lang/en/c34_px_among.json).

STUB from the foundation: the slice fills it in. It builds: Among Guhs: the ship, the session engine, roles, NPC AI, meetings, the eight task screens, sabotage, the queue, the parody round, rewards, titles and the shop offers (DESIGN_PX sections 2, 3, 4).
Helpers: tools/features/guhpixel_lib.py (deco, muurdeco, npc, geluid, quest_adv, test_kamer, teksten, controleer); own helper
modules are tools/features/guhpixel_among_*.py (_bouw, _tex, _modellen, _tekst, _geluid). Every text is Dutch here.
"""
from features import guhpixel_lib as lib

TEXTS = {}


def build(h):
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_among", keys=TEXTS)


def ftb(fq):
    pass
