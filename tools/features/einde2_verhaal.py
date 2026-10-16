"""
einde2 phase B (CONTRACT_E2_B 2, 4.2, 9): the story state of the Guhvatar story, shared by every slice of the wave.
Java: nl.juiced.guhs.feature.einde2.verhaal (E2Verhaal, E2VerhaalDev, E2VerhaalCommando).

  lang      gui.guhs.verhalen.kop.guheinde        the heading of the group "guheinde" in the Guhdex tab Verhalen
            gui.guhs.guhpad.verhaal.guhvatar      the name of the ONE big story of the Guhpad in the Guheinde
            gui.guhs.einde2_verhaal.hervat.*      fix round 1: "Verder met het verhaal" (E2Hervat: respawn / login lines, the kaasbol)
            gui.guhs.einde2_verhaal.hint.*        fix round 1: the step hints (StapHint)
            gui.guhs.einde2_verhaal.richting.*    fix round 1: the direction phrases (Richting)
            scene.guhs.einde2_verhaal_kijk*       fix round 1: the look scenes (StapHint.kijkScene)
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


# =====================================================================================================================
# fix round 1 (fix-kern, playtest 1): continuity and hints
# =====================================================================================================================
HERVAT = {
    "waar": "Je verhaal: %1$s - %2$s",
    "portaal": "Ga door je portaal terug naar het Guheinde. Bij de Knabbelberg, waar je uitkomt, gloeit de kaasbol: "
               "klik erop en je bent meteen terug bij je verhaal.",
    "kaasbol": "Ver weg van je verhaal? Klik op de gloeiende kaasbol bij de Knabbelberg (%s): die brengt je meteen terug.",
    "kaasbol_hier": "De kaasbol gloeit hier vlakbij. Klik erop en je bent meteen terug bij je verhaal.",
    "kaasbol_balk": "Klik op de kaasbol: verder met je verhaal!",
    "verder": "De kaasbol gloeit fel... en brengt je terug naar je verhaal!",
    # merge_fix1: the same from the row "Verder met het verhaal" in Appa's menu
    "verder_appa": "Appa bromt, zwiept met zijn staart... en een windvlaag brengt je terug naar je verhaal!",
    "geest": "Deze stap speelt in de Geestenwereld: mediteer opnieuw op de plek waar je de vorige keer mediteerde.",
    "wacht": "De wind zoekt de weg nog. Probeer het zo nog eens, njeg.",
    "geen_plek": "Voor deze stap weet de kaasbol geen plek. Kijk in je Guhdex (Verhalen) waar je heen moet.",
    "niets": "Er is nu geen verhaal om verder te gaan.",
}
HINT = {
    "standaard": "Volgende stap: %1$s Dat is %2$s.",
    "balk": "➜ %s",
    "elders": "in een andere wereld",
}
RICHTING = {
    "n": "in het noorden", "no": "in het noordoosten", "o": "in het oosten", "zo": "in het zuidoosten",
    "z": "in het zuiden", "zw": "in het zuidwesten", "w": "in het westen", "nw": "in het noordwesten",
    "dichtbij": "dichtbij", "eindje": "een eindje verder", "ver": "ver weg", "heel_ver": "heel ver weg",
    "boven": "boven je", "onder": "onder je",
    "recht_boven": "recht boven je", "recht_onder": "recht onder je", "hier": "hier vlakbij",
    "zin2": "%1$s, %2$s", "zin3": "%1$s, %2$s en %3$s",
}
KIJK = {
    "einde2_verhaal_kijk": ("Kijk daar!", {}),
}


def kern(h):
    """fix round 1 (fix-kern): the texts of E2Hervat, StapHint and Richting, and the caption-less look scene (merge_fix1: chapter 1's
    hints, look scenes and airbending lesson moved to einde2_h1_lucht_tekst.py, under h1's prefixes)."""
    for k, v in HERVAT.items():
        kit.lang(h, f"gui.guhs.einde2_verhaal.hervat.{k}", v)
    for k, v in HINT.items():
        kit.lang(h, f"gui.guhs.einde2_verhaal.hint.{k}", v)
    for k, v in RICHTING.items():
        kit.lang(h, f"gui.guhs.einde2_verhaal.richting.{k}", v)
    for sid, (titel, regels) in KIJK.items():
        verhaal_motor.scene(h, sid, titel, regels)


def build(h):
    verhaal_motor.groep(h, GROEP, KOP)
    kern(h)
    kit.lang(h, f"gui.guhs.guhpad.verhaal.{GROOT}", NAAM)
    kit.verborgen(h, f"guhpad_klaar_{GROOT}")
    for s in VERHAAL_STRUCTUREN:
        verhaal_motor.sluier(h, s)
    selfcheck(h)


def selfcheck(h):
    import os
    problems = [k for k in (f"gui.guhs.verhalen.kop.{GROEP}", f"gui.guhs.guhpad.verhaal.{GROOT}") if k not in h.NL]
    # fix round 1: every direction key Richting.java builds, and a title for every look scene
    for k in ["n", "no", "o", "zo", "z", "zw", "w", "nw", "dichtbij", "eindje", "ver", "heel_ver", "boven", "onder",
              "recht_boven", "recht_onder", "hier", "zin2", "zin3"]:
        if f"gui.guhs.einde2_verhaal.richting.{k}" not in h.NL:
            problems.append(f"no direction text {k}")
    problems += [f"no title of {sid}" for sid in KIJK if f"scene.guhs.{sid}.titel" not in h.NL]
    if not os.path.exists(f"{h.D}/advancement/quest/guhpad_klaar_{GROOT}.json"):
        problems.append(f"no advancement guhpad_klaar_{GROOT}")
    if problems:
        raise SystemExit("einde2_verhaal self-check: " + "; ".join(problems))
