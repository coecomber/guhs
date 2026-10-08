"""
Het Guhpad (DESIGN_VERHALENPAD A; Java: feature/guhpad): the stories open the worlds. The big stories of the Guhmensie open
the Knabbelring and with it the Guhbarbecuether; the Knabbelring, Super Guhrio and the Aangebrande Mika open the Guheinde;
"Het echte Guheinde" is a preview that is locked for everybody. No blocks, items or structures: this module writes

  lang      gui.guhs.guhpad.*           the path map and the per-world list of the Guhdex tab Verhalen, the Superkompas
                                        option "Mijn verhaal" (its explanation), the preview of "Het echte Guheinde"
            gui.guhs.guhpad.verhaal.<id>   the name of every big story (VERHALEN: the same ids as GroteVerhalen.java)
            quest.guhs.guhpad.*         what Guhdalf and the two portals say when a world is still locked (with the list)
            stat.guhs.verhalen_gevolgd  the custom statistic behind the FTB counter
  data      advancement/quest/guhpad_klaar_<id>, guhpad_open_<wereld>, guhpad_mika, guhpad_echt   hidden, for the FTB tasks
            (guhpad_echt is never granted: the real Guheinde does not exist yet)
  textures  gui/guhpad/padkaart.png, gui/guhpad/echt.png   (features/guhpad_tex.py)
  FTB       the tasks of the four lock quests of the chapter group "Het Guhpad" (ftb_sloten; the chapters themselves are in
            tools/make_ftbquests.py) and the quests of the chapter "Het echte Guheinde": question marks that really hang
            behind its lock quest (FTB_OP_SLOT), the riddles and the row of layer 6 of the Guh-technologie.

Het Snuffeleiland is built by another slice: it counts (in the lock quest of the Guhbarbecuether chapter, in the counter)
as soon as the module "snuffel" is in FEATURES, exactly as the Java side counts it as soon as the Verhaallijn
"snuffeleiland" is registered. The self-check compares VERHALEN with GroteVerhalen.java.
Wiki: features/guhpad_wiki.py. Visual check for the merge step: tools/autocheck/bbq2_guhpad.txt.
"""
import os
import re

from features import FEATURES, bbq2, guhpad_tex

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpad", "GroteVerhalen.java")

# --- FTB (tools/make_ftbquests.py) ---------------------------------------------------------------------------------------
FTB_CHAPTER = "guhs_pad_echt"
FTB_OP_SLOT = True           # every quest of this module really hangs behind the lock quest of its chapter
VRAAG = "guhs:textures/ftbquests/icon_vraag.png"
FTB_PLAATJES = {"icon_vraag.png": ("vraag", [64])}
LAAG6 = 5                    # the question marks of layer 6 of the Guh-technologie (GuhpadTab.LAAG6)
FTB_SECTIES = [("guhpad_echt", "???", "silhouet:guh:normal", ["guhpad_echt_1", "guhpad_echt_2", "guhpad_echt_3"]),
               ("guhpad_laag6", "Guh-technologie: laag 6", "silhouet:npc:uitvinderguh",
                [f"guhpad_laag6_{i}" for i in range(1, LAAG6 + 1)])]

# --- the big stories: id -> (world, world where it begins, name, icon item). The same ids, worlds, icons and order as
#     feature/guhpad/GroteVerhalen.java (the self-check compares them) --------------------------------------------------------
SNUFFELEILAND = "snuffeleiland"
VERHALEN = {
    "balto": ("guhmensie", "guhmensie", "Baltoguh en Nomguh", "guhs:baltoguh_beeldje"),
    "mewtwo": ("guhmensie", "guhmensie", "Guhtwo en het kloon-eiland", "guhs:mewtwo_labnotitie"),
    "hemel": ("guhmensie", "guhmensie", "Het Hemelkapelletje", "guhs:pluisveertje"),
    "guhwaii": ("guhmensie", "guhmensie", "Ohana op Guhwai'i", "guhs:kokosnoot"),
    SNUFFELEILAND: ("guhmensie", "guhmensie", "Het Snuffeleiland", "minecraft:bone"),
    "knabbelring": ("barbecuether", "guhmensie", "In de ban van de Knabbelring", "guhs:knabbelring"),
    "guhrio": ("barbecuether", "barbecuether", "Super Guhrio", "guhs:guhrio_vadsmunt"),
}
WERELDEN = {   # id -> (name, short name on the path map, what the tooltip says)
    "guhmensie": ("De Guhmensie", "Guhmensie", "Waar elk guh-avontuur begint. Hier mag iedereen meteen naartoe."),
    "barbecuether": ("De Guhbarbecuether", "Barbecuether",
                     "Achter het grillportaal. Guhdalf laat je pas door als je de grote verhalen van de Guhmensie hebt gevolgd."),
    "guheinde": ("Het Guheinde", "Guheinde",
                 "Achter het portaal in de Knabbelkelder. Eerst de Knabbelring, Super Guhrio en de Aangebrande Mika."),
    "echt": ("Het echte Guheinde", "???", "Niemand weet wat hier ligt. Njeg..."),
}
RAADSELS = ["Voorbij het einde ligt nog een einde.",
            "Wie alle verhalen volgt, hoort er nog eentje fluisteren.",
            "Het laatste knabbeltje is nooit het laatste. Njeg?"]
BEKEND = ("Het enige dat bekend is: je komt er alleen door alle verhalen van de Guhmensie, de Guhbarbecuether en het Guheinde "
          "te volgen.")

TEXTS = {
    # --- the Guhdex tab Verhalen -------------------------------------------------------------------------------------------
    "gui.guhs.guhpad.titel": "Het Guhpad",
    "gui.guhs.guhpad.uitleg": "De grote verhalen openen de werelden: eerst de Guhmensie, dan de Guhbarbecuether, dan het Guheinde. "
                              "Wijs een halte aan en je ziet wat je daarvoor nog mist.",
    "gui.guhs.guhpad.teller": "Verhalen gevolgd: %s van %s",
    "gui.guhs.guhpad.teller.uitleg": "Alleen de grote verhalen tellen mee:",
    "gui.guhs.guhpad.slot.open": "Open: je mag hier naar binnen. Vahoeg!",
    "gui.guhs.guhpad.slot.dicht": "Op slot. Dit ontbreekt nog:",
    "gui.guhs.guhpad.slot.echt": "Op slot voor iedereen. Nog wel, njeg...",
    "gui.guhs.guhpad.op_slot": "op slot",
    "gui.guhs.guhpad.open": "open",
    "gui.guhs.guhpad.nog_nodig": "Op slot, njeg. Nog nodig: %s.",
    "gui.guhs.guhpad.leeg": "Hier zijn nog geen verhalen.",
    "gui.guhs.guhpad.hier": "Hier ben je nu op het Guhpad",
    "gui.guhs.guhpad.klik_halte": "Klik om naar deze verhalen te springen",
    "gui.guhs.guhpad.vouw_open": "Klik om de verhalen te laten zien",
    "gui.guhs.guhpad.vouw_dicht": "Klik om de verhalen in te klappen",
    "gui.guhs.guhpad.groot": "★ Groot verhaal: telt mee voor het Guhpad",
    "gui.guhs.guhpad.lijst.komma": ", ",
    "gui.guhs.guhpad.lijst.en": " en ",
    # --- what a world asks besides stories -----------------------------------------------------------------------------------
    "gui.guhs.guhpad.eis.knabbelfeest": "Het knabbelfeest van Guhdalf (hoofdstuk 1 van de Knabbelring)",
    "gui.guhs.guhpad.eis.aangebrande_mika": "De Aangebrande Mika verslaan",
    "gui.guhs.guhpad.eis.echt": "???",
    # --- Het echte Guheinde: a preview ---------------------------------------------------------------------------------------
    "gui.guhs.guhpad.echt.bekend": BEKEND,
    "gui.guhs.guhpad.echt.laag6": "Guh-technologie, laag 6: ???",
    "gui.guhs.guhpad.echt.laag6.tekst": "Gaat pas open in het echte Guheinde.",
    # --- the Superkompas option "Mijn verhaal" ------------------------------------------------------------------------------
    "gui.guhs.guhpad.kompas.uitleg": "Wijst naar het dichtstbijzijnde grote verhaal dat je nog niet hebt gedaan. Volg je een verhaal "
                                     "(Guhdex, tab Verhalen), dan wijst het naar de volgende stap daarvan. Is dat in een andere "
                                     "wereld, dan wijst het naar het portaal.",
    "gui.guhs.guhpad.kompas.dichtstbij": "Nu: het dichtstbijzijnde verhaal dat je nog niet hebt gedaan",
    "gui.guhs.guhpad.kompas.volgt": "Nu volg je: %s",
    # --- (1.4.1) the Superkompas tab Verhalen: the places per world, a place you have not reached yet ---------------------------
    "gui.guhs.guhpad.kompas.verhaal": "Verhaal: %s",
    "gui.guhs.guhpad.kompas.geheim": "???",
    "gui.guhs.guhpad.kompas.geheim.uitleg": "Een plek uit %s waar je verhaal nog niet is geweest. Volg het verhaal, dan staat "
                                            "hij hier vanzelf. Njeg!",
    "gui.guhs.guhpad.kompas.vouw_open": "Klik om de plekken te laten zien",
    "gui.guhs.guhpad.kompas.vouw_dicht": "Klik om de plekken in te klappen",
    # --- a world is still locked: Guhdalf, the grill portal, the portal of the Knabbelkelder (%s = the list) --------------------
    "quest.guhs.guhpad.guhdalf.nee": "Hohoho, njeg! Een ring draag je niet zomaar. Laat eerst zien dat je een dappere guh bent en volg "
                                     "deze verhalen in de Guhmensie: %s. Kom daarna terug, dan steek ik het vuurwerk voor je aan!",
    "quest.guhs.guhpad.grillportaal": "Het grillportaal sputtert alleen een beetje, njeg. Guhdalf laat je pas door als je deze verhalen "
                                      "in de Guhmensie hebt gevolgd: %s. Je Guhdex (tab Verhalen) wijst de weg!",
    "quest.guhs.guhpad.guheindeportaal": "Het portaal naar het Guheinde blijft donker, njeg. Dit moet je eerst nog doen: %s. Je Guhdex "
                                         "(tab Verhalen) wijst de weg!",
    # --- the statistic behind the FTB counter (Statistieken > Algemeen) --------------------------------------------------------
    "stat.guhs.verhalen_gevolgd": "Verhalen gevolgd",
}


def bestaat(verhaal):
    """Is this big story in this tree (the Snuffeleiland: as soon as its own slice's module "snuffel" is listed)?"""
    return verhaal != SNUFFELEILAND or "snuffel" in FEATURES


def verhalen(wereld=None):
    """The ids of the big stories that are in this tree (of one world), in order."""
    return [v for v, (w, _begin, _naam, _icoon) in VERHALEN.items() if bestaat(v) and (wereld is None or w == wereld)]


def texts(h):
    for key, tekst in TEXTS.items():
        h.lang(key, tekst, tekst)
    for v, (_wereld, _begin, naam, _icoon) in VERHALEN.items():
        h.lang(f"gui.guhs.guhpad.verhaal.{v}", naam, naam)
    for w, (naam, kort, uitleg) in WERELDEN.items():
        h.lang(f"gui.guhs.guhpad.wereld.{w}", naam, naam)
        h.lang(f"gui.guhs.guhpad.wereld.{w}.kort", kort, kort)
        h.lang(f"gui.guhs.guhpad.wereld.{w}.uitleg", uitleg, uitleg)
    for i, raadsel in enumerate(RAADSELS, 1):
        h.lang(f"gui.guhs.guhpad.echt.raadsel.{i}", raadsel, raadsel)


def advancements(h):
    for v in VERHALEN:
        bbq2.verborgen(h, f"guhpad_klaar_{v}")
    for w in WERELDEN:
        bbq2.verborgen(h, f"guhpad_open_{w}")
    bbq2.verborgen(h, "guhpad_mika")
    bbq2.verborgen(h, "guhpad_echt")      # (never granted: the real Guheinde does not exist yet)


# =====================================================================================================================
# FTB
# =====================================================================================================================
def ftb_sloten(fq):
    """The tasks of the four lock quests of the chapter group "Het Guhpad": what to finish before each chapter opens."""
    def verhaal(v):
        return fq.met(fq.adv(f"guhpad_klaar_{v}"), VERHALEN[v][2], VERHALEN[v][3])
    return {
        "guhpad_slot_guhmensie": [fq.met(fq.dim("guhs:guhmension"), "Stap door je guhportaal de Guhmensie in", "guhs:block_of_kaasknabbels")],
        "guhpad_slot_barbecuether": [verhaal(v) for v in verhalen("guhmensie")],
        "guhpad_slot_guheinde": [verhaal(v) for v in verhalen("barbecuether")]
                                + [fq.met(fq.adv("guhpad_mika"), TEXTS["gui.guhs.guhpad.eis.aangebrande_mika"], "guhs:gloeister")],
        # the ONE counter ("Verhalen gevolgd: n van m": a stat task shows n / m), and the lock nobody can open yet
        "guhpad_slot_echt": [fq.met(fq.stat("verhalen_gevolgd", len(verhalen())), TEXTS["stat.guhs.verhalen_gevolgd"], "guhs:guhdex"),
                             fq.met(fq.adv("guhpad_echt"), "???")],
    }


def ftb(fq):
    """The chapter "Het echte Guheinde": question marks only. Every quest really hangs behind the lock quest (FTB_OP_SLOT), and
    its task is the advancement nobody gets."""
    geheim = [fq.met(fq.adv("guhpad_echt"), "???")]
    for i, raadsel in enumerate(RAADSELS, 1):
        fq.q(f"guhpad_echt_{i}", "???", f"&7&o{raadsel}&r", VRAAG, geheim, rewards=())
    for i in range(1, LAAG6 + 1):
        fq.q(f"guhpad_laag6_{i}", "???", "Laag 6 van de &bGuh-technologie&r. Gaat pas open in &7het echte Guheinde&r. Njeg, nog even "
             "geduld!", VRAAG, geheim, rewards=(), shape="rsquare")


# =====================================================================================================================
# the self-check and the build
# =====================================================================================================================
def java_verhalen():
    """{id: (world, begin, icon)} from GroteVerhalen.java, in source order (None: no Java in this tree)."""
    if not os.path.exists(JAVA):
        return None
    src = open(JAVA, encoding="utf-8").read().replace("SNUFFELEILAND, Wereld", f'"{SNUFFELEILAND}", Wereld')
    return {m[0]: (m[1].lower(), m[2].lower(), m[3])
            for m in re.findall(r'(?:groot\(|new GrootVerhaal\()"(\w+)", Wereld\.(\w+), Wereld\.(\w+),\s*"([\w:]+)"', src)
            if m[0] != "guhvatar"}   # (the example in the class text)


def selfcheck(h):
    problems = []
    java = java_verhalen()
    if java is not None:
        if list(java) != list(VERHALEN):
            problems.append(f"the big stories differ: GroteVerhalen.java {list(java)}, guhpad.py {list(VERHALEN)}")
        for v, (wereld, begin, icoon) in java.items():
            if v in VERHALEN and VERHALEN[v][0:2] + (VERHALEN[v][3],) != (wereld, begin, icoon):
                problems.append(f"{v}: GroteVerhalen.java says {(wereld, begin, icoon)}, guhpad.py {VERHALEN[v]}")
    for v in VERHALEN:
        if f"gui.guhs.guhpad.verhaal.{v}" not in h.NL:
            problems.append(f"no name for the big story {v}")
        if not os.path.exists(f"{h.D}/advancement/quest/guhpad_klaar_{v}.json"):
            problems.append(f"no advancement for the big story {v}")
    for key in ("gui.guhs.guhpad.titel", "gui.guhs.guhpad.wereld.echt.kort", "gui.guhs.guhpad.echt.raadsel.3", "quest.guhs.guhpad.guhdalf.nee",
                "stat.guhs.verhalen_gevolgd"):
        if key not in h.NL:
            problems.append(f"no text {key}")
    for naam in ("padkaart.png", "echt.png"):
        if not os.path.exists(os.path.join(h.TEX, "gui", "guhpad", naam)):
            problems.append(f"no picture gui/guhpad/{naam}")
    if (guhpad_tex.KAART_W, guhpad_tex.KAART_H, len(guhpad_tex.HALTES)) != (256, 76, len(WERELDEN)):
        problems.append("the path map is 256x76 with one stop per world (GuhpadTab.KAART_W / KAART_H / HALTE_X)")
    if problems:
        raise SystemExit("guhpad self-check:\n  " + "\n  ".join(problems))


def build(h):
    texts(h)
    advancements(h)
    guhpad_tex.build(h)
    selfcheck(h)
