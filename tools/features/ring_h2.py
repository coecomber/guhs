"""
bbq2 (ring-h2): chapter 2 of the Knabbelring: De Raad van Guhrond (Guhvendel). Java: feature/ringh2.

This is the F0 stub (CONTRACT_130 5.1): the slice replaces the content of this module. Until then its placeholders
(NPC names and textures, Guhdex pages, the fixed ids) come from features/bbq2.py.
"""
FTB_LINEAIR = True
FTB_SECTIES = [("ring_h2", "De Raad van Guhrond", "npc:guhrond", None)]


def build(h):
    # the F0 placeholder of the questline (one step; Java: RingH2Feature.LIJN), so the travel map of ring-kern has its halte
    from features import verhaal_motor
    verhaal_motor.verhaallijn(h, "ring_h2", "De Raad van Guhrond", "Dit deel van het verhaal wordt nog geschreven. Njeg!",
                              stappen=[("Wordt nog geschreven", "Dit deel van het verhaal wordt nog geschreven. Njeg!", "Guhvendel")])
