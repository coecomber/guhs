"""
bbq2 (tech-quests): the Oude Guhrad-centrale, the uitvinder-guh's questline and practice hall, the whole FTB chapter Guh-technologie, De Grote Knabbelmachine. Java: feature/techquest.

This is the F0 stub (CONTRACT_130 5.1): the slice replaces the content of this module. Until then its placeholders
(NPC names and textures, Guhdex pages, the fixed ids) come from features/bbq2.py.
"""
# The FTB chapter guhs_techniek is written by this module only, as PROJECTS (CONTRACT_130 8). Until the slice fills it in, the
# chapter is the scaffold: its "Hoe kom je hier?" quest and the preview picture below.
# FTB_SECTIES = [(sid, title, portrait, keys or None = the rest), ...]: the sections of this module, in order.
FTB_SECTIES = []
# One picture at the very END of the chapter with no quests under it (make_ftbquests.py FTB_SLOT): the preview that the
# last part of the Guh-technologie is not there yet and belongs to the Guheinde (the Bestelguh, draadloze vadskracht and
# the Guhterminal come with the next update).
FTB_SLOT = ("Wordt vervolgd in het Guheinde...", "guh:ender",
            ["Hier ontbreekt nog een stukje. Njeg!",
             "De laatste uitvindingen liggen diep in het &5Guheinde&r:",
             "de Bestelguh, draadloze vadskracht en de Guhterminal.",
             "Die komen in een volgende update. Bouw vast een mooie fabriek!"])


def build(h):
    pass
