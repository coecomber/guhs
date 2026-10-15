"""
einde2 phase B/C (CONTRACT_E2_B 7), slice trap5_machines: tech tier 5: the Werktuigguh's workshop and projects, the machines and sources.
Java: nl.juiced.guhs.feature.einde2.trap5 (E2Trap5Feature).

Skelet stub: the trap5_machines slice replaces this module's content (it stays in features.FEATURES at this place). Chunk:
CONTRACT_E2_B 1.3. For now: the names of the two vadskracht sources the skelet put in BronSoort.
"""
import features.einde2_kit as kit  # noqa: F401

# the two sources of tier 5 (Java: BronSoort.ENDERGUHRAD, KOMEETKERN; features/vadskracht.py leaves their names to this module)
K = "gui.guhs.vadskracht.soort."
BRONNEN = {"enderguhrad": ("Enderguhraden", "Enderguhrad"), "komeetkern": ("Komeetkernen", "Komeetkern")}


def build(h):
    for id, (meer, een) in BRONNEN.items():
        kit.lang(h, K + id, meer)
        kit.lang(h, K + id + ".enkel", een)
