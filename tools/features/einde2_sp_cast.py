"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.2), slice sp_cast: the cast of the Sponsguh story as the story needs it.
Java: nl.juiced.guhs.feature.einde2.spcast (E2SpCastFeature, SpCast, KroegMikas, KroegBewoners). Chunk c111_e2_sp_cast.

  einde2_sp_cast_looks   the eight story looks (bones look_<look> on the cast's models) and the story animations; called by
                         einde2_sponsguh_wezens_modellen (phase A, owned by sp_cast) where the cast's models are written
  einde2_sp_cast_kroeg   the Mikas of De Ruige Kroeg as GuhNpcEntity Kinds: models, textures, animations
  this module            their names and small talk, the self-check
"""
from features import einde2_kit as kit
from features import einde2_sp_cast_kroeg as kroeg
from features import einde2_sp_cast_looks as looks

P = "gui.guhs.einde2_sp_cast."
# kind -> (line 0, line 1): what a Kroeg Mika says on a click (own words; two each)
PRAATJES = {
    "kroeg_barman_mika": ("Wat mag het zijn? Kwallensap, kwallensap of... kwallensap.",
                          "Deze mok poets ik al sinds dinsdag. Hij wordt maar niet schoon, njeg."),
    "kroeg_uitsmijter_mika": ("Alleen echte stoere types mogen naar binnen. Jij bent... klein. Prima, kom maar.",
                              "Ik ben de uitsmijter. Ik smijt mensen eruit. Heel voorzichtig, hoor."),
    "stoere_mika": ("Ik ben zo stoer dat ik mijn kwallensap zonder rietje drink.",
                    "Zie je deze tatoeage? Een anker. Ik ben nog nooit op een boot geweest."),
    "stoere_mika_2": ("Die ooglap? Gewoon voor de stijl. Ik zie prima, njeg.",
                      "Ik heb ooit een zee-egel geaaid. Met mijn blote poot. Ja. Stoer, he."),
    "slapende_mika": ("Zzzz... nog vijf minuutjes, mam... zzzz...",
                      "Mmmh... niet aan mijn sleuteltje komen... zzzz..."),
}


def build(h):
    for kind, (naam, _anims) in kroeg.KINDS.items():
        kit.lang(h, f"entity.guhs.guh_npc.{kind}", naam)
        for i, regel in enumerate(PRAATJES[kind]):
            kit.lang(h, f"{P}praatje.{kind}.{i}", regel)
    kroeg.build(h)
    selfcheck(h)


def selfcheck(h):
    problems = kroeg.check(h)
    if set(PRAATJES) != set(kroeg.KINDS):
        problems.append("PRAATJES and kroeg.KINDS list other kinds")
    if looks.ALLE != ["snor", "emmer", "droog", "ijsje", "netje", "petje", "manager", "kroon_vast"]:
        problems.append("einde2_sp_cast_looks.ALLE is not the order of SpCast.LOOKS")
    import os
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "sponsguh", "Cast.java")
    if os.path.exists(java):
        src = open(java, encoding="utf-8").read()
        if 'List.of("snor", "emmer", "droog", "ijsje", "netje", "petje", "manager", "kroon_vast")' not in src:
            problems.append("Cast.LOOKS in Java is not the order of einde2_sp_cast_looks.ALLE")
    if problems:
        raise SystemExit("einde2_sp_cast self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    pass
