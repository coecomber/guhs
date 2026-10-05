"""
Guhpixel slice "grap1" (Java: feature/guhpixel/grap1; namespaces skyblok, bedwars, vadsnite; English: tools/lang/en/c32_px_grap1.json).

STUB from the foundation: the slice fills it in. It builds: Skyblok, Bedwars and Vadsnite: an arena, a session, a lobby NPC, 3-5 steps and a keepsake each (DESIGN_PX section 2).
Helpers: tools/features/guhpixel_lib.py (deco, muurdeco, npc, geluid, quest_adv, test_kamer, teksten, controleer); own helper
modules are tools/features/guhpixel_grap1_*.py (_bouw, _tex, _modellen, _tekst, _geluid). Every text is Dutch here.
"""
from features import guhpixel_lib as lib

TEXTS = {}


def build(h):
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_grap1", keys=TEXTS)


def ftb(fq):
    pass
