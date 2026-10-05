"""
bbq2 (ring-h4): chapter 4 of the Knabbelring: De Spiegel van Guhladriel. Java: feature/ringh4.

This is the F0 stub (CONTRACT_130 5.1): the slice replaces the content of this module. Until then its placeholders
(NPC names and textures, Guhdex pages, the fixed ids) come from features/bbq2.py.
"""
FTB_LINEAIR = True
FTB_SECTIES = [("ring_h4", "De Spiegel van Guhladriel", "npc:guhladriel", None)]


def build(h):
    # the F0 placeholder of the questline (one step; Java: RingH4Feature.LIJN), so the travel map of ring-kern has its halte
    from features import verhaal_motor
    verhaal_motor.verhaallijn(h, "ring_h4", "De Spiegel van Guhladriel", "Dit deel van het verhaal wordt nog geschreven. Njeg!",
                              stappen=[("Wordt nog geschreven", "Dit deel van het verhaal wordt nog geschreven. Njeg!", "De boomstad van Guhladriel")])
