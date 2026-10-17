"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.2), slice sp_cast: the cast of the Sponsguh story: the story looks (SpCast.zetLook), story animations, the Mikas of De Ruige Kroeg, the residents' visibility, new guh clothes.
Java: nl.juiced.guhs.feature.einde2.spcast (E2SpCastFeature). Chunk c111_e2_sp_cast.

STUB written by the skelet (einde2-sponsguh-skelet). This module is yours: replace what you need, keep build(h) and ftb(fq).
"""

import features.einde2_kit as kit

# the four Kinds the skelet put into the <einde2_sp_cast> block of GuhNpcEntity (names only; sp_cast replaces this with the real
# kinds: models, small talk, animations)
STUB_NAMEN = {
    "kroeg_barman_mika": "Barman-Mika", "kroeg_uitsmijter_mika": "Uitsmijter-Mika", "stoere_mika": "Stoere Mika",
    "slapende_mika": "Slapende Mika",
}


def build(h):
    for kind, naam in STUB_NAMEN.items():
        kit.lang(h, f"entity.guhs.guh_npc.{kind}", naam)


def ftb(fq):
    pass
