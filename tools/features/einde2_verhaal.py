"""
einde2 phase B (CONTRACT_E2_B 2, 4.2, 9): the story state of the Guhvatar story, shared by every slice of the wave.
Java: nl.juiced.guhs.feature.einde2.verhaal (E2Verhaal, E2VerhaalDev, E2VerhaalCommando).

  lang      gui.guhs.verhalen.kop.guheinde        the heading of the group "guheinde" in the Guhdex tab Verhalen
            gui.guhs.guhpad.verhaal.guhvatar      the name of the ONE big story of the Guhpad in the Guheinde
            gui.guhs.einde2_verhaal.*             (texts of this module, none yet)
  data      advancement/quest/guhpad_klaar_guhvatar   hidden, granted by GuhpadEvents when the finale is done
            kaart/verborgen.json                  Guhdalfs sluier: the four story structures are hidden on the live map

The five lines, their texts and their FTB sections are the chapter slices' (einde2_h1_lucht .. einde2_h4_vuur). Chunk
c91_e2_verhaal.
"""
from features import einde2_kit as kit
from features import verhaal_motor

GROEP = "guheinde"
GROOT = "guhvatar"
KOP = "Verhalen van het Guheinde"
NAAM = "De Legende van Guh"
VERHAAL_STRUCTUREN = ["zuidelijke_luchttempel", "noordelijke_waterstam", "ba_sing_guh", "vuurnatie_hoofdstad"]


def build(h):
    verhaal_motor.groep(h, GROEP, KOP)
    kit.lang(h, f"gui.guhs.guhpad.verhaal.{GROOT}", NAAM)
    kit.verborgen(h, f"guhpad_klaar_{GROOT}")
    for s in VERHAAL_STRUCTUREN:
        verhaal_motor.sluier(h, s)
    selfcheck(h)


def selfcheck(h):
    import os
    problems = [k for k in (f"gui.guhs.verhalen.kop.{GROEP}", f"gui.guhs.guhpad.verhaal.{GROOT}") if k not in h.NL]
    if not os.path.exists(f"{h.D}/advancement/quest/guhpad_klaar_{GROOT}.json"):
        problems.append(f"no advancement guhpad_klaar_{GROOT}")
    if problems:
        raise SystemExit("einde2_verhaal self-check: " + "; ".join(problems))
