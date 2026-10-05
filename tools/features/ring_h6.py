"""
bbq2 (ring-h6): chapter 6 of the Knabbelring: De Frituurberg. Java: feature/ringh6.

This is the F0 stub (CONTRACT_130 5.1): the slice replaces the content of this module. Until then its placeholders
(NPC names and textures, Guhdex pages, the fixed ids) come from features/bbq2.py.
"""
FTB_LINEAIR = True
FTB_SECTIES = [("ring_h6", "De Frituurberg", "guh:sam_guh", None)]


def build(h):
    # the F0 placeholder of the questline (one step; Java: RingH6Feature.LIJN), so the travel map of ring-kern has its halte
    from features import verhaal_motor
    verhaal_motor.verhaallijn(h, "ring_h6", "De Frituurberg", "Dit deel van het verhaal wordt nog geschreven. Njeg!",
                              stappen=[("Wordt nog geschreven", "Dit deel van het verhaal wordt nog geschreven. Njeg!", "De Frituurberg")])
