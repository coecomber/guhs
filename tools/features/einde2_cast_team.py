"""
einde2 (the Guheinde expansion), slice cast_team (CONTRACT_E2 11.4, 11.9): Team Guhvatar and the friends you make on the way,
as GuhNpcEntity.Kinds (Aang-guh, Katara-guh, Sokka-guh, Toph-guh, Zuka in six looks, Oom Miroh in three, Suki-guh in two,
prinses Yue-guh and the moon spirit, and the five of the Leegtehaven and the ringenrace).
Java: nl.juiced.guhs.feature.einde2.castteam (CastTeam = the table, CastTeamRol = the default role, E2CastTeamFeature,
E2CastTeamCommando, client/E2CastTeamClient).

  build(h)   the models, textures and animation files (einde2_cast_team_modellen), the names and the small talk
             (einde2_cast_team_tekst, Dutch in both files), and a self-check

No Guhdex page, no FTB quest, no advancement, no spawn egg (a Kind has none: /summon guhs:guh_npc ~ ~ ~ {Kind:"<kind>"} or
the dev command /guhs einde2 cast_team toon). English: tools/lang/en/c68_e2_cast_team.json.
"""
import features.einde2_kit as kit
from features import einde2_cast_team_modellen as modellen
from features import einde2_cast_team_tekst as tekst


def texts(h):
    for kind, naam in tekst.NAMEN.items():
        kit.lang(h, f"entity.guhs.guh_npc.{kind}", naam)
    for kind, regels in tekst.PRAATJES.items():
        for i, regel in enumerate(regels):
            kit.lang(h, f"{tekst.P}{kind}.{i}", regel)


def build(h):
    modellen.build(h)
    texts(h)
    selfcheck(h)


def selfcheck(h):
    modellen.check(h)
    problems = []
    for kind in modellen.CAST:
        if kind not in tekst.NAMEN:
            problems.append(f"{kind} has no name")
        if len(tekst.PRAATJES.get(kind, ())) != 2:
            problems.append(f"{kind} does not have exactly two lines of small talk")
    for kind in set(tekst.NAMEN) | set(tekst.PRAATJES):
        if kind not in modellen.CAST:
            problems.append(f"{kind} has a text but is not in CAST")
    if problems:
        raise SystemExit("einde2_cast_team self-check failed:\n  " + "\n  ".join(problems))
