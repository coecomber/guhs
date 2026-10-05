"""
Guhpixel slice "grap2" (Java: feature/guhpixel/grap2; namespaces guhmon, bzg; English: tools/lang/en/c33_px_grap2.json).

STUB from the foundation: the slice fills it in. It builds: the Guhmon-gevecht (battle screen, guh picker, leenguh, badge case + pet) and Boer zoekt Guh (studio + farm set, letters, keepsakes) (DESIGN_PX section 2).
Helpers: tools/features/guhpixel_lib.py (deco, muurdeco, npc, geluid, quest_adv, test_kamer, teksten, controleer); own helper
modules are tools/features/guhpixel_grap2_*.py (_bouw, _tex, _modellen, _tekst, _geluid). Every text is Dutch here.
"""
from features import guhpixel_lib as lib

TEXTS = {}


def build(h):
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_grap2", keys=TEXTS)


def ftb(fq):
    pass
