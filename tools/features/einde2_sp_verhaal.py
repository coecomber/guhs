"""
einde2 "Sponsguh" (CONTRACT_E2_C 2, 3, 4, 9), the skelet sp_verhaal: the story state of the second story of the Guheinde,
Sponsguh Kaasbroek (DESIGN 5b). Java: nl.juiced.guhs.feature.einde2.spverhaal (SpVerhaal, SpStappen, SpStandIn, SpPoort,
SpCommando, SpVerhaalDev). Chunk c110_e2_sp_verhaal.

  lang      gui.guhs.verhalen.kop.sponsguh              the heading of the group "sponsguh" in the Guhdex tab Verhalen
            gui.guhs.guhpad.verhaal.sponsguh            the name of the big story of the Guhpad
            gui.guhs.verhalen.sponsguh_recept|kroon.naam/.uitleg   the two lines (their STEP texts: each step owner's module)
            gui.guhs.einde2_sp_verhaal.poort.*          the gate (the line over the hotbar, the three chat lines)
            gui.guhs.einde2_sp_verhaal.standin.*        what the stand-in of a step says
  data      advancement/quest/guhpad_klaar_sponsguh     hidden, granted by GuhpadEvents when part 2 is done
            advancement/quest/sponsguh_*_stap_<i>       the step advancements (verhaal_motor.verhaallijn)
            kaart/verborgen.json                        Guhdalfs sluier: both places hidden on the live map

The step texts: every slice module that owns steps has STAPPEN = {lijn: {stap: (naam, nu, waar)}}, EXTRA = {lijn: {sleutel:
(nu, waar)}} (the reserved variants "<stap>_b" of its steps, and "klaar" for the owner of a line's last step) and KORT =
{lijn: {sleutel: kort}}; this module puts them together into ONE verhaal_motor.verhaallijn call per line and fails when a step
has no owner text. FTB: the two sections of the story (CONTRACT_E2_C 6) with the quest keys of the step owners.
"""
import os

from features import einde2_kit as kit
from features import verhaal_motor
from features import einde2_sp_d1_start, einde2_sp_d1_stad, einde2_sp_d1_recept, einde2_sp_d2_heen, einde2_sp_d2_weg, einde2_sp_d2_finale

P = "gui.guhs.einde2_sp_verhaal"
GROEP = "sponsguh"
GROOT = "sponsguh"
KOP = "Sponsguh Kaasbroek"
NAAM = "Sponsguh Kaasbroek"
RECEPT, KROON = "sponsguh_recept", "sponsguh_kroon"
STAPPEN = {RECEPT: 11, KROON: 12}
VARIANTEN = {RECEPT: ["4_b", "5_b", "6_b", "8_b", "10_b"], KROON: ["3_b", "4_b", "6_b", "7_b", "8_b", "10_b"]}   # = SpStappen.VARIANTEN
STRUCTUREN = ["bikinibodem", "schelpenstad"]
# the step owners, in story order (CONTRACT_E2_C 0): which steps of which line each module writes
EIGENAARS = [
    (einde2_sp_d1_start, RECEPT, range(0, 5)),
    (einde2_sp_d1_stad, RECEPT, range(5, 7)),
    (einde2_sp_d1_recept, RECEPT, range(7, 11)),
    (einde2_sp_d2_heen, KROON, range(0, 5)),
    (einde2_sp_d2_weg, KROON, range(5, 9)),
    (einde2_sp_d2_finale, KROON, range(9, 12)),
]

LIJNEN = {
    RECEPT: ("Sponsguh deel 1: Het geheime recept",
             "Diep onder de eilanden van het Guheinde ligt Bikinibodem, een stadje onder een reusachtige zeepbel. De Krokante "
             "Knabbel zoekt een nieuwe collega, en Sponsguh Kaasbroek kan niet wachten om je alles te leren. Wat kan er nou "
             "misgaan, njeg?"),
    KROON: ("Sponsguh deel 2: De kroon van koning Neptuguh",
            "Meneer Krabguh opent een tweede restaurant en zoekt een manager. Sponsguh weet zeker dat hij het wordt. Maar dan "
            "verdwijnt de kroon van koning Neptuguh, en Krabguh krijgt de schuld. Jij, Sponsguh en Patguh gaan hem terughalen: "
            "helemaal in Schelpenstad. Met de Knabbelburgermobiel. Wat kan er nou misgaan, njeg?"),
}

TEXTS = {
    # the gate (SpPoort, Sluiers.BERICHTEN): pink bubbles round both places until the Guhvatar story is done
    f"{P}.poort.balk": "Roze bubbels houden je tegen. Speel eerst het Guhvatar-verhaal uit, njeg!",
    f"{P}.poort.1": "<Sponsguh> (dof, door de bubbels heen) Hallooo daarbuiten! Sorry, we zijn nog dicht! Kom terug als je de "
                    "wereld gered hebt, dan bakken we samen burgers!",
    f"{P}.poort.2": "Bikinibodem en Schelpenstad gaan voor je open zodra je het Guhvatar-verhaal (De Legende van Guh) hebt "
                    "uitgespeeld.",
    f"{P}.poort.3": "Nog niet begonnen of even kwijt? De gloeiende kaasbol bij de Knabbelberg brengt je bij je verhaal. Daarna "
                    "mag je hier zo vaak komen als je wilt, njeg.",
    # the stand-in of a step that is not built yet (SpStandIn)
    f"{P}.standin.stap": "[stand-in] Stap %1$s: %2$s. Ga naar: %3$s.",
    f"{P}.standin.klaar": "[stand-in] %s: gedaan (deze stap wordt nog gebouwd).",
    f"{P}.standin.lijn_klaar": "[stand-in] %s is klaar!",
}

# FTB (CONTRACT_E2_C 6): the story's two sections; the quests are the step owners' (one per step, key fixed here)
FTB_CHAPTER = "guhs_guheinde"
QUESTS = {
    RECEPT: [f"einde2_sp_d1_start_r{i}" for i in range(0, 5)] + [f"einde2_sp_d1_stad_r{i}" for i in range(5, 7)]
            + [f"einde2_sp_d1_recept_r{i}" for i in range(7, 11)],
    KROON: [f"einde2_sp_d2_heen_k{i}" for i in range(0, 5)] + [f"einde2_sp_d2_weg_k{i}" for i in range(5, 9)]
           + [f"einde2_sp_d2_finale_k{i}" for i in range(9, 12)],
}
FTB_SECTIES = [("einde2_sp_deel1", "Sponsguh Kaasbroek: Het geheime recept", "geo:sponsguh:sponsguh", QUESTS[RECEPT]),
               ("einde2_sp_deel2", "Sponsguh Kaasbroek: De kroon van koning Neptuguh", "geo:neptuguh:neptuguh", QUESTS[KROON])]


def stappen(lijn):
    """(stappen, extra, kort) of a line, put together from the step owners' modules (and checked: every step has one owner)."""
    rijen, extra, kort = {}, {}, {}
    for mod, l, nummers in EIGENAARS:
        if l != lijn:
            continue
        eigen = getattr(mod, "STAPPEN", {}).get(lijn, {})
        if sorted(eigen) != list(nummers):
            raise SystemExit(f"einde2_sp_verhaal: {mod.__name__}.STAPPEN[{lijn}] has steps {sorted(eigen)}, not {list(nummers)}")
        rijen.update(eigen)
        extra.update(getattr(mod, "EXTRA", {}).get(lijn, {}))
        kort.update(getattr(mod, "KORT", {}).get(lijn, {}))
    n = STAPPEN[lijn]
    if sorted(rijen) != list(range(n)):
        raise SystemExit(f"einde2_sp_verhaal: {lijn} has texts for steps {sorted(rijen)}, not 0..{n - 1}")
    for v in VARIANTEN[lijn]:
        if v not in extra:
            raise SystemExit(f"einde2_sp_verhaal: no text of the reserved variant {lijn} {v} (its step owner writes it)")
    klaar = extra.pop("klaar", None)
    return [rijen[i] for i in range(n)], klaar, extra, kort


def build(h):
    verhaal_motor.groep(h, GROEP, KOP)
    for lijn, (naam, uitleg) in LIJNEN.items():
        rijen, klaar, extra, kort = stappen(lijn)
        verhaal_motor.verhaallijn(h, lijn, naam, uitleg, stappen=rijen, klaar=klaar, extra=extra, kort=kort)
    for key, text in TEXTS.items():
        kit.lang(h, key, text)
    kit.lang(h, f"gui.guhs.guhpad.verhaal.{GROOT}", NAAM)
    kit.verborgen(h, f"guhpad_klaar_{GROOT}")
    for s in STRUCTUREN:
        verhaal_motor.sluier(h, s)
    selfcheck(h)


def ftb(fq):
    pass   # (the quests are the step owners'; this module only names its two sections, FTB_SECTIES)


def selfcheck(h):
    problems = [k for k in (f"gui.guhs.verhalen.kop.{GROEP}", f"gui.guhs.guhpad.verhaal.{GROOT}") + tuple(TEXTS) if k not in h.NL]
    if not os.path.exists(f"{h.D}/advancement/quest/guhpad_klaar_{GROOT}.json"):
        problems.append(f"no advancement guhpad_klaar_{GROOT}")
    # the Java side: the step counts and the reserved variants are the same numbers
    J = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "spverhaal")
    src = open(os.path.join(J, "SpVerhaal.java"), encoding="utf-8").read()
    for naam, n in (("R_STAPPEN", STAPPEN[RECEPT]), ("K_STAPPEN", STAPPEN[KROON])):
        if f"{naam} = {n};" not in src:
            problems.append(f"SpVerhaal.{naam} is not {n}")
    stap = open(os.path.join(J, "SpStappen.java"), encoding="utf-8").read()
    for lijn, cnst in ((RECEPT, "RECEPT"), (KROON, "KROON")):
        regel = f"SpVerhaal.{cnst}, List.of(" + ", ".join(f'"{v}"' for v in VARIANTEN[lijn]) + ")"
        if regel not in stap:
            problems.append(f"SpStappen.VARIANTEN of {lijn} is not {VARIANTEN[lijn]}")
    if problems:
        raise SystemExit("einde2_sp_verhaal self-check: " + "; ".join(problems))
