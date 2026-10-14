"""
einde2 (the Guheinde expansion), slice cast_volk (CONTRACT_E2 11.5): the masters, rulers and townsfolk of the Guhvatar story as
GuhNpcEntity.Kinds: thirty-nine kinds, each with its own model and sheet, one animation file per character, a name and two
lines of small talk. No story logic (phase B attaches roles; the merge step puts them at their markers, 11.7).
Java: nl.juiced.guhs.feature.einde2.castvolk (E2CastVolkFeature; the table: CastVolk).

  einde2_cast_volk_kern       the workbench: a Pop per kind (base body, paint, extra bones, swatches), animation language
  einde2_cast_volk_delen      the wardrobe: robes, hair, hats, beards, glasses ... shared by the characters
  einde2_cast_volk_burgers    the crowds (mix sets) from table rows
  einde2_cast_volk_modellen   the named characters, the table CAST, the self-check, the previews
  einde2_cast_volk_tekst      every Dutch text
"""
from features import einde2_cast_volk_modellen as modellen
from features import einde2_cast_volk_tekst as tekst
from features import einde2_kit as kit

TEXTS = tekst.TEXTS


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def build(h):
    modellen.build(h)
    texts(h)
    selfcheck(h)


def selfcheck(h):
    problems = modellen.check(h)
    for rij in modellen.CAST:
        kind = rij["kind"]
        for key in (f"entity.guhs.guh_npc.{kind}", f"{tekst.P}{kind}.0", f"{tekst.P}{kind}.1"):
            if key not in h.NL:
                problems.append(f"no text {key}")
    if set(tekst.NAMEN) != set(modellen.RIJ) or set(tekst.PRAATJES) != set(modellen.RIJ):
        problems.append("the texts and the table name other kinds")
    if problems:
        raise SystemExit("einde2_cast_volk:\n  " + "\n  ".join(problems))
