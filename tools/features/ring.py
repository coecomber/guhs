"""
bbq2 (ring-kern): the Knabbelring: the ring and its effects, Sam-guh, Guhdalf, Smikagol, the cast, the Nine, the gifts, rest points, the portal lock, the travel map. Java: feature/ring.

This is the F0 stub (CONTRACT_130 5.1): the slice replaces the content of this module. Until then its placeholders
(NPC names and textures, Guhdex pages, the fixed ids) come from features/bbq2.py.
"""
# The chapter guhs_knabbelring is the one chapter whose quests really depend on each other (make_ftbquests.py FTB_LINEAIR):
# every ring module sets it. A quest without deps comes after the previous quest of the same module; the first quest of a
# module names the last quest of the chapter before it in deps=[...].
FTB_LINEAIR = True
FTB_SECTIES = [("ring", "De Knabbelring", "npc:guhdalf", None)]


def variants(rng, v):
    """Sam-guh's fur (placeholder: a warm brown; the slice paints the gardener look and adds its bones "samguh*")."""
    return {"sam_guh": ((185, 138, 90), {})}


def build(h):
    pass
