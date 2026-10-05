"""
De gids (2.9, "De Grote Guhspelen"): the Guhdex as all-in-one and the Superkompas with icon tabs (Java: feature/gids,
client/screen/GuhDexScreen, client/screen/SuperkompasScreen, item/SuperkompasItem.CATEGORIES). No registry content, so
this module only writes:

  lang (Dutch in both files)   the Guhdex tabs (Guhs, Knus, Minigames, Kleding), everything the Minigames and Kleding tabs
                               say (headings, columns, tooltips), a short label per Highscores row inside its building
                               (gui.guhs.gids.rij.<row>), the Superkompas subheadings and tab texts
  advancements                 grote_guhspelen/gids_ontdekker (all six buildings of De Grote Guhspelen visited) and
                               grote_guhspelen/gids_wereldreiziger (every minigame building visited; a challenge), both
                               granted by GidsFeature (trigger minecraft:impossible)
  ftb(fq)                      two explorer quests in "Minigames & bijzondere plekken"

The self-check compares the row labels with the rows of quest/Highscores.java, and the Superkompas tabs with
SuperkompasItem.CATEGORIES, so nothing shows a raw lang key.
"""
import os
import re

FTB_CHAPTER = "guhs_minigames"
FTB_SECTION = "Op ontdekkingstocht"

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs")

# --- the Guhdex tabs ----------------------------------------------------------------------------------------------------------
TABS = {"guhs": "Guhs", "knus": "Knus", "minigames": "Minigames", "kleding": "Kleding"}

# --- the Kleding tab --------------------------------------------------------------------------------------------------------------
SOORTEN = {  # GidsData.Soort + the kapper
    "klassiekers": "Klassieke minigames",
    "knuffeldal_spelletjes": "Knuffeldal-spelletjes",
    "grote_guhspelen": "De Grote Guhspelen",
    "winkels": "Kleermaker, Guhdex & zelf maken",
    "plekken": "Plekken, feestjes & vriendjes",
    "schatkisten": "Schatkisten",
    "beroepen": "Beroepen",
    "overig": "Nog even zoeken",
    "kapper": "Bij de kapper",
}
KLEDING = {
    "gui.guhs.gids.zonder_bron": "Nog geen bron (njeg?)",
    "gui.guhs.gids.kapsels": "Kapsels van Kapper Krulletje",
    "gui.guhs.gids.tip.bron": "Bron: %s",
    "gui.guhs.gids.tip.prijs": "Prijs: %s",
    "gui.guhs.gids.tip.ontgrendeld": "Ontgrendeld: voor al je guhs, vahoeg!",
    "gui.guhs.gids.tip.op_slot": "Nog op slot. Geen geheim: ga hem halen!",
    "gui.guhs.gids.tip.kapper": "Bij de kapper: Kapper Krulletje knipt je guh. Geen ontgrendeling nodig, njeg!",
    "gui.guhs.gids.kleding.telling": "Ontgrendeld: %s / %s",
    "gui.guhs.gids.kleding.uitleg": "Houd rechtsklik ingedrukt met een kledingstuk in je hand: dan is het voor altijd van jou, voor al je "
                                    "tamme guhs! Grijs = nog op slot, groen randje = van jou. Klik een bron open.",
    "gui.guhs.gids.kleding.bij_de_kapper": "bij de kapper",
}

# --- the Minigames tab ----------------------------------------------------------------------------------------------------------
MINIGAMES = {
    "gui.guhs.gids.minigames.bezocht": "Gebouwen gevonden: %s / %s",
    "gui.guhs.gids.minigames.geen_records": "Nog geen serverrecord van jou... nog niet!",
    "gui.guhs.gids.minigames.records": "%s serverrecords van jou, vahoeg!",
    "gui.guhs.gids.minigames.klik": "Klik een gebouw open voor je scores",
    "gui.guhs.gids.minigames.gevonden": "%s/%s gevonden",
    "gui.guhs.gids.minigames.bij": "bij %s",
    "gui.guhs.gids.minigames.zelf": "gewoon zelf spelen, zonder spelleider",
    "gui.guhs.gids.minigames.kleding_telling": "kleding %s/%s",
    "gui.guhs.gids.minigames.kort_bezocht": "✔ bezocht",
    "gui.guhs.gids.minigames.kort_niet": "nog niet bezocht",
    "gui.guhs.gids.minigames.kleding": "Kleding %s/%s:",
    "gui.guhs.gids.minigames.kolom.spel": "Spel",
    "gui.guhs.gids.minigames.kolom.jij": "Jouw beste",
    "gui.guhs.gids.minigames.kolom.record": "Serverrecord",
    "gui.guhs.gids.minigames.geen_record": "nog niemand",
    "gui.guhs.gids.minigames.geen_scores": "Hier houden we geen scores bij: gewoon lekker spelen, vahoeg!",
    "gui.guhs.gids.minigames.tip.jij": "Jouw beste: %s",
}

# --- the short labels of the Highscores rows inside their building (gui.guhs.gids.rij.<row>) --------------------------------------
NIVEAUS = {"makkelijk": "Makkelijk", "medium": "Medium", "lastig": "Lastig"}
BANEN = {"regenboog": "Regenboogbaan", "vads": "Vadsbaan", "kaasberg": "Kaasbergbaan"}
SPELEN = {"knabbelhappen": "Knabbelhappen", "zaklopen": "Zaklopen", "blikgooien": "Mika-blikgooien", "eierlopen": "Eierlopen met knabbelei",
          "spijkerpoepen": "Spijkerpoepen", "guhguhtje_prik": "Guhguhtje prik", "zeskamp": "De Grote Zeskamp (alles!)"}


def rij_labels():
    """{Highscores row id: short Dutch label} for every row (checked against Highscores.java in selfcheck)."""
    rows = {}

    def levels(base, suffix=""):
        # base_makkelijk, base (= medium, the old board), base_lastig
        rows[f"{base}_makkelijk"] = "Makkelijk" + suffix
        rows[base] = "Medium" + suffix
        rows[f"{base}_lastig"] = "Lastig" + suffix

    for base in ("beauty", "meppen", "golf", "smul"):
        levels(base)
    levels("race", ": hele race")
    levels("race_lap", ": snelste ronde")
    levels("vissen", ": punten")
    levels("vissen_zwaarste", ": zwaarste vis")
    rows["disco_makkelijk"] = "Vadsige Tango (makkelijk)"
    rows["disco"] = "Ze hangen aan me vet (medium)"
    rows["disco_lastig"] = "Mika-Mambo (lastig)"
    rows["disco_boogie"] = "Njeg-Njeg Boogie (bonus)"
    rows["verstop_makkelijk"] = "Makkelijk"
    rows["verstop_medium"] = "Medium"
    rows["verstop_moeilijk"] = "Moeilijk"
    rows["bakkerij"] = "Knabbels bakken"
    rows["creche"] = "Babyguhtjes terugbrengen"
    rows["kapper"] = "Kappersshow"
    rows["glijbaan_roze_trechter"] = "Glijbaan: Roze Trechter"
    rows["glijbaan_glimtunnel"] = "Glijbaan: Glimtunnel"
    rows["glijbaan_grote_plons"] = "Glijbaan: Grote Plons"
    rows["sjoelen"] = "Een beurt: 20 schijfjes"
    for lvl, name in NIVEAUS.items():
        rows[f"doolhof_{lvl}"] = name
        rows[f"katapult_{lvl}"] = name
    for sid, name in SPELEN.items():
        rows[f"spelen_{sid}"] = name
    rows["elfguhjestocht"] = "Alle 11 stempels"
    for baan, bname in BANEN.items():
        for lvl in NIVEAUS:
            rows[f"circuit_{baan}_{lvl}"] = f"{bname} {lvl}: race"
            rows[f"circuit_{baan}_{lvl}_ronde"] = f"{bname} {lvl}: ronde"
    # 3.0 (Guhverhalen): the Nomguh sledesprint, surfing and hula (the rows are registered by the fundament)
    for lvl in NIVEAUS:
        rows[f"sledesprint_{lvl}"] = f"Sledesprint: {lvl}"
        rows[f"surfen_{lvl}"] = f"Surfen: {lvl}"
        rows[f"hula_{lvl}"] = f"Hula: {lvl}"
    return rows


# --- the Superkompas ------------------------------------------------------------------------------------------------------------
SUPERKOMPAS = {
    "gui.guhs.superkompas.minigames.tooltip": "Alle spelletjes: de klassiekers, Knuffeldal, De Grote Guhspelen en de Guhverhalen",
    "gui.guhs.superkompas.kopje.klassiekers": "Klassiekers",
    "gui.guhs.superkompas.kopje.knuffeldal": "Knuffeldal",
    "gui.guhs.superkompas.kopje.grote_guhspelen": "De Grote Guhspelen",
    "gui.guhs.superkompas.kopje.verhalen": "Guhverhalen",
    "gui.guhs.superkompas.kopje.guhpixel": "Guhpixel",
    "gui.guhs.superkompas.zoekt_al": "Hier wijst je superkompas nu naartoe",
    "gui.guhs.superkompas.pick": "Klik een plek aan: je superkompas wijst de weg!",
}

# --- advancements -------------------------------------------------------------------------------------------------------------
ADVANCEMENTS = [
    # (name, parent, icon, frame, title, description)
    ("gids_ontdekker", "guhs:grote_guhspelen/root", "guhs:guhmensie_superkompas", "goal", "Guhspelen-ontdekker",
     "Bezoek alle zes gebouwen van De Grote Guhspelen. Je superkompas wijst de weg!"),
    ("gids_wereldreiziger", "guhs:grote_guhspelen/gids_ontdekker", "guhs:guhdex", "challenge", "Vahoege wereldreiziger",
     "Bezoek ELK minigame-gebouw van de Guhmensie en Knuffeldal. Kijk in de Guhdex (Minigames) welke je nog mist!"),
]


def build(h):
    for tid, name in TABS.items():
        h.lang(f"gui.guhs.guhdex.tab.{tid}", name, name)
    for sid, name in SOORTEN.items():
        h.lang(f"gui.guhs.gids.soort.{sid}", name, name)
    for table in (KLEDING, MINIGAMES, SUPERKOMPAS):
        for key, text in table.items():
            h.lang(key, text, text)
    for rid, label in rij_labels().items():
        h.lang(f"gui.guhs.gids.rij.{rid}", label, label)
    for name, parent, icon, frame, title, desc in ADVANCEMENTS:
        h.w(f"{h.D}/advancement/grote_guhspelen/{name}.json", {
            "parent": parent,
            "display": {"icon": {"id": icon},
                        "title": {"translate": f"advancements.guhs.grote_guhspelen.{name}.title"},
                        "description": {"translate": f"advancements.guhs.grote_guhspelen.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.title", title, title)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.description", desc, desc)
    selfcheck()


def _java(*path):
    full = os.path.join(JAVA, *path)
    return open(full, encoding="utf-8").read() if os.path.exists(full) else None


def highscore_ids():
    """The row ids of Highscores.GAMES, read from the Java source (new Game("id", ...), points("id", ...), time("id", ...))."""
    src = _java("quest", "Highscores.java")
    if src is None:
        return None
    body = src[src.index("GAMES = List.of("):src.index("/** A 2.9 row with points")]
    return re.findall(r'(?:new Game|points|time)\("([a-z0-9_]+)"', body)


def superkompas_tabs():
    """The category ids of SuperkompasItem.CATEGORIES (cat("id", ...) / new Category("id", ...)) and every Kopje id."""
    src = _java("item", "SuperkompasItem.java")
    if src is None:
        return None, None
    body = src[src.index("CATEGORIES = List.of("):src.index("public static int categoryOf")]
    return re.findall(r'(?:cat|new Category)\("([a-z_]+)"', body), re.findall(r'new Kopje\("([a-z_]+)"', body)


def selfcheck():
    problems = []
    labels = rij_labels()
    ids = highscore_ids()
    if ids is not None:
        missing = [i for i in ids if i not in labels]
        extra = [i for i in labels if i not in ids]
        if missing:
            problems.append("Highscores rows without a gids label: " + ", ".join(missing))
        if extra:
            problems.append("gids labels for rows that don't exist: " + ", ".join(extra))
        if len(ids) != len(set(ids)):
            problems.append("duplicate Highscores rows")
    cats, kopjes = superkompas_tabs()
    if cats is not None:
        if len(cats) > 14:
            problems.append(f"{len(cats)} Superkompas tabs: at most 14 fit on one row")
        if "knus" not in cats or "barbecue" not in cats or "minigames" not in cats:
            problems.append("the Superkompas tabs knus, barbecue and minigames must stay")
        for k in kopjes:
            if f"gui.guhs.superkompas.kopje.{k}" not in SUPERKOMPAS:
                problems.append(f"Superkompas subheading without text: {k}")
    for text in list(KLEDING.values()) + list(MINIGAMES.values()) + list(SUPERKOMPAS.values()):
        if "te vads" in text.lower():
            problems.append("a guh is never 'te vads': " + text)
    if problems:
        raise SystemExit("gids self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    fq.q("gids_ontdekker", "Op ontdekkingstocht!",
         "Zes gloednieuwe gebouwen vol spelletjes: het &6Sjoelhuisje&r, het &2Guhdoolhof&r, de &cKnabbelkatapult&r, de &9Knabbelspelen&r, "
         "de &bElf-Guhjestocht&r en het &dGuh-Circuit&r. Kies ze in je superkompas (tab Minigames) en loop er eens binnen. In je Guhdex "
         "(tab Minigames) zie je welke je al gevonden hebt!", "guhs:guhmensie_superkompas",
         [fq.adv("guhs:grote_guhspelen/gids_ontdekker")], rewards=(("guhs:kaas_knabbels", 16),), shape="gear", xp=150)
    fq.q("gids_wereldreiziger", "Vahoege wereldreiziger",
         "Een echte guh-toerist bezoekt ALLE minigame-gebouwen: de klassiekers, Knuffeldal en De Grote Guhspelen. De Guhdex (tab "
         "Minigames) laat met een vinkje zien waar je al was. Nog eentje missen? Njeg! Het superkompas helpt je.", "guhs:guhdex",
         [fq.adv("guhs:grote_guhspelen/gids_wereldreiziger")], rewards=(("guhs:kaas_knabbels", 32), ("guhs:guh_taart", 1)), shape="rsquare",
         xp=400)


BONES = {}
CLOTHES = []
