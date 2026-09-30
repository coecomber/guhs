"""
De klassiekers op niveau (2.9, De Grote Guhspelen): makkelijk / medium / lastig for the five 2.4 games beauty, meppen,
golf, smul and vissen (Java: feature/klassiekers + the level knobs in each game's own package; medium = the game as it
always was, its boards keep their ids).

  build(h)   lang (Dutch in both files): the level buttons' tooltips per game, the screen/record lines, what the guhs say
             when you start on makkelijk or lastig, vissen escapes (2.10: no more golf wind); the advancements of the tab "De Grote
             Guhspelen" (makkelijk, medium, lastig per game, the Klassiekers-kampioen); a self-check that every
             klassiekers lang key the Java code uses exists
  ftb(fq)    the quests of the section "Makkelijk, medium of lastig" (chapter guhs_minigames)

The Highscores row names (beauty_makkelijk, ...) are in spelen.py (phase 1).
"""
import os
import re

SPELLEN = ["beauty", "meppen", "golf", "smul", "vissen"]
MUNTEN = {"beauty": "guhs:showrozet", "meppen": "guhs:mepmunt", "golf": "guhs:golfballetje", "smul": "guhs:smulmunt",
          "vissen": "guhs:visbon"}

# --- the tooltips of the level buttons: what the level means for this game -------------------------------------------------
TOOLTIPS = {
    "beauty": {
        "makkelijk": "Een hele minuut om je model aan te kleden, en een lieve jury: elk jurylid geeft een puntje extra. Lekker rustig vadsen!",
        "medium": "De Vads-wedstrijd zoals altijd: 45 seconden aankleden, een eerlijke jury.",
        "lastig": "Maar 30 seconden aankleden! En een strenge jury: Juf Vadsma trekt twee punten af per kledingstuk dat niet bij het thema past, "
                  "Meneer Glitterguh wil een setje dat bij elkaar hoort en Oma Knabbel is kritisch. Njeg!",
    },
    "meppen": {
        "makkelijk": "De Mika's ploppen rustiger op en blijven langer staan. Minder guhs tussendoor (niet meppen!) en vaker een gouden Mika.",
        "medium": "Mika meppen zoals altijd: steeds sneller, steeds meer koppen.",
        "lastig": "Supersnelle Mika's die zo weer weg zijn, veel meer guhs tussendoor (niet meppen, njeg!) en gouden Mika's zijn zeldzaam.",
    },
    "golf": {
        "makkelijk": "10 slagen per hole en geen strafslagen: valt je bal in de kaassaus of van de baan, dan gaat hij gewoon terug. "
                     "Je slaat af vanaf de groene matjes, vlak bij de hole.",
        "medium": "Guhgolf zoals altijd: 8 slagen per hole, kaassaus of van de baan af is een strafslag.",
        "lastig": "Maar 6 slagen per hole en strafslagen, vanaf de rode afslagmatjes helemaal achteraan. De Golfguh zet extra roze "
                  "slijmbumpers neer en haar guhmolen draait twee keer zo snel. Njeg!",
    },
    "smul": {
        "makkelijk": "Het eten valt rustiger naar beneden en er valt veel minder Mika-vet. Smullen zonder stress!",
        "medium": "Het Vadsig eetfestijn zoals altijd.",
        "lastig": "Het eten valt veel sneller en de Mika's gooien bakken Mika-vet naar beneden. Alleen voor echte smulkampioenen!",
    },
    "vissen": {
        "makkelijk": "Beet? Je hebt ruim de tijd om binnen te halen (2 tot 3,5 seconde). Geen vis ontsnapt!",
        "medium": "De Guhvis-wedstrijd zoals altijd: bij beet 1 tot 2 seconden om binnen te halen.",
        "lastig": "Bij beet heb je maar een halve seconde! En vissen kunnen van je haakje spartelen: hoe zwaarder de vis, hoe groter de kans. "
                  "(De Mika-meerval blijft altijd hangen, njeg!)",
    },
}
MUNTEN_TIP = {"makkelijk": "Beloning: gewoon", "medium": "Beloning: gewoon", "lastig": "Beloning: de helft extra! VAHOEG!"}

# --- what the guh says when you start on makkelijk / lastig -----------------------------------------------------------------
START = {
    "meppen": {"makkelijk": "Rustig aan vandaag: de Mika's zijn nog slaperig van hun middagdutje. Mep ze!",
               "lastig": "Pas op, dit zijn de snelste Mika's van de hele Guhmensie! En er zitten stiekem veel guhs tussen: die NIET meppen, hè!"},
    "smul": {"makkelijk": "De Mika's zijn vandaag lekker lui: er valt bijna geen Mika-vet. Smakelijk!",
             "lastig": "Oei, de Mika's zitten boven met emmers vol Mika-vet, en het eten valt als een raket! Wordt jouw guh toch vahoeg?"},
    "golf": {"makkelijk": "Geen strafslagen vandaag, en je mag %s keer slaan per hole. Rustig mikken!",
             "lastig": "Maar %s slagen per hole, vanaf de rode matjes achteraan, met extra slijmbumpers en een turbo-guhmolen! Vahoeg mikken, njeg!"},
    "beauty": {"makkelijk": "Neem je tijd: %s seconden per ronde, en de jury is vandaag extra lief.",
               "lastig": "Maar %s seconden per ronde, en de jury heeft haar strengste bril op. Laat zien wat je kan!"},
    "vissen": {"makkelijk": "De vissen bijten vandaag heel rustig: je hebt ruim de tijd om binnen te halen.",
               "lastig": "De vissen zijn vandaag zo snel als een Mika met een kaasknabbel! Snel binnenhalen, en hou ze goed vast."},
}

LANG = {
    "gui.guhs.klassiekers.records": "Jouw records:",
    "gui.guhs.klassiekers.wereldrecord": "Wereldrecord (medium): %s",
    "gui.guhs.klassiekers.gespeeld": "%s keer gespeeld",
    "gui.guhs.klassiekers.beauty.shows": "%s shows gelopen",
    "gui.guhs.klassiekers.beauty.bord": "(%s s aankleden)",
    "gui.guhs.klassiekers.golf.bord": "(max %s slagen)",
    "gui.guhs.klassiekers.golf.rondjes": "Rondjes: %s   Hole-in-ones: %s",
    "gui.guhs.klassiekers.golf.geen_strafslag": "Geen strafslag op makkelijk: je bal gaat terug",
    "gui.guhs.klassiekers.vissen.niveau": "(niveau: %s)",
    "gui.guhs.klassiekers.vissen.ontsnapt": "Njeg! De %s van %s spartelt van je haakje en zwemt giechelend weg...",
    "quest.guhs.klassiekers.vissen.ontsnapt_totaal": "Ontsnapt: %s vis(sen). Die zwemmen nu vadsig rond in de vijver.",
}

# --- advancements (tab De Grote Guhspelen; all granted by the games: minecraft:impossible) ------------------------------------
ADV = [
    # name, parent, icon, frame, title, description
    ("klassiekers_makkelijk", "root", "guhs:gouden_smulknabbel", "task", "Rustig aan, guhtje",
     "Speel een klassieker op makkelijk: de beauty, meppen, golf, smullen of vissen"),
    ("klassiekers_medium", "root", "guhs:mika_mep_hamer", "task", "Zoals vanouds",
     "Speel een klassieker op medium, zoals je gewend bent"),
    ("klassiekers_beauty_lastig", "klassiekers_medium", "guhs:showrozet", "goal", "Streng maar vadsig",
     "Loop een hele show op lastig: 30 seconden aankleden en een strenge jury"),
    ("klassiekers_meppen_lastig", "klassiekers_medium", "guhs:mepmunt", "goal", "Bliksemmepper",
     "Speel Mika meppen op lastig en mep minstens één Mika"),
    ("klassiekers_golf_lastig", "klassiekers_medium", "guhs:golfballetje", "goal", "Van het rode matje",
     "Speel alle 9 holes guhgolf op lastig: 6 slagen per hole, de verste afslagjes en extra slijmbumpers!"),
    ("klassiekers_smul_lastig", "klassiekers_medium", "guhs:smulmunt", "goal", "Vet vadsig",
     "Haal punten op het Vadsig eetfestijn op lastig, tussen al dat Mika-vet door"),
    ("klassiekers_vissen_lastig", "klassiekers_medium", "guhs:visbon", "goal", "Snelle hengel",
     "Vis een hele Guhvis-wedstrijd op lastig, waar de vissen van je haakje spartelen"),
    ("klassiekers_kampioen", "klassiekers_medium", "guhs:guh_taart", "challenge", "Klassiekers-kampioen",
     "Speel alle vijf de klassiekers op lastig. Jij bent de vahoegste guh van de Guhmensie!"),
]


def all_lang():
    lang = dict(LANG)
    for spel, levels in TOOLTIPS.items():
        for lvl, text in levels.items():
            lang[f"gui.guhs.klassiekers.{spel}.{lvl}"] = text
    for lvl, text in MUNTEN_TIP.items():
        lang[f"gui.guhs.klassiekers.munten.{lvl}"] = text
    for spel, levels in START.items():
        for lvl, text in levels.items():
            lang[f"quest.guhs.klassiekers.{spel}.{lvl}"] = text
    for name, _parent, _icon, _frame, title, desc in ADV:
        lang[f"advancements.guhs.grote_guhspelen.{name}.title"] = title
        lang[f"advancements.guhs.grote_guhspelen.{name}.description"] = desc
    return lang


def build(h):
    for key, text in all_lang().items():
        h.lang(key, text, text)
    for name, parent, icon, frame, _title, _desc in ADV:
        h.w(f"{h.D}/advancement/grote_guhspelen/{name}.json", {
            "parent": f"guhs:grote_guhspelen/{parent}",
            "display": {"icon": {"id": icon},
                        "title": {"translate": f"advancements.guhs.grote_guhspelen.{name}.title"},
                        "description": {"translate": f"advancements.guhs.grote_guhspelen.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})
    selfcheck()


def selfcheck():
    """Every klassiekers lang key the Java code names exists (dynamic ones: per game / level / wind strength)."""
    lang = all_lang()
    problems = []
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "java", "nl", "juiced", "guhs", "feature")
    for pkg in SPELLEN + ["klassiekers"]:
        for dirpath, _dirs, files in os.walk(os.path.join(root, pkg)):
            for f in files:
                if not f.endswith(".java"):
                    continue
                text = open(os.path.join(dirpath, f), encoding="utf-8").read()
                for key in re.findall(r'"((?:gui|quest)\.guhs\.klassiekers\.[a-z0-9_.]+)"', text):
                    if key.endswith("."):
                        if not any(k.startswith(key) for k in lang):
                            problems.append(f"{f}: no keys under {key}")
                    elif key not in lang:
                        problems.append(f"{f}: missing lang {key}")
    for spel in SPELLEN:
        for lvl in ("makkelijk", "medium", "lastig"):
            if f"gui.guhs.klassiekers.{spel}.{lvl}" not in lang:
                problems.append(f"no tooltip for {spel} {lvl}")
        for lvl in ("makkelijk", "lastig"):
            if f"quest.guhs.klassiekers.{spel}.{lvl}" not in lang:
                problems.append(f"no start line for {spel} {lvl}")
        if not any(a[0] == f"klassiekers_{spel}_lastig" for a in ADV):
            problems.append(f"no lastig advancement for {spel}")
    for text in lang.values():
        if "te vads" in text.lower():
            problems.append("a guh is never 'te vads': " + text)
    if problems:
        raise SystemExit("klassiekers self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    q, adv = fq.q, fq.adv
    q("klassiekers_niveaus", "Makkelijk, medium of lastig?",
      "Bij de &dVads-wedstrijd&r, &dMika meppen&r, &dguhgolf&r, het &dVadsig eetfestijn&r en de &dGuhvis-wedstrijd&r kies je nu zelf "
      "hoe moeilijk het is: drie knopjes boven de speelknop. &aMakkelijk&r is lekker rustig, &emedium&r is zoals altijd en &clastig&r... "
      "dat is echt lastig, maar je krijgt wel de helft meer beloning! Elk niveau heeft zijn eigen scorebord.",
      "guhs:gouden_smulknabbel", [adv("guhs:grote_guhspelen/klassiekers_makkelijk")], rewards=(("guhs:kaas_knabbels", 16),), x=0, y=0)
    teksten = {
        "beauty": ("Streng maar vadsig", "Loop een hele show op &clastig&r: maar 30 seconden om je model aan te kleden, en een strenge jury. "
                                         "Tip: kies stukken die echt bij het thema passen, en liefst een setje!"),
        "meppen": ("Bliksemmepper", "Speel Mika meppen op &clastig&r: de Mika's zijn bliksemsnel en er zitten veel guhs tussen. "
                                    "Niet de guhs meppen, hè!"),
        "golf": ("Van het rode matje", "Speel alle 9 holes op &clastig&r: 6 slagen per hole en strafslagen, vanaf de rode afslagmatjes "
                                       "helemaal achteraan, langs extra slijmbumpers en onder een turbo-guhmolen door."),
        "smul": ("Vet vadsig", "Haal punten op het eetfestijn op &clastig&r: het eten valt als een raket en het regent Mika-vet. "
                               "Wordt jouw guh toch vahoeg?"),
        "vissen": ("Snelle hengel", "Vis een hele wedstrijd op &clastig&r: bij beet heb je maar een halve seconde, en zware vissen "
                                    "spartelen soms van je haakje."),
    }
    for i, spel in enumerate(SPELLEN):
        title, desc = teksten[spel]
        q(f"klassiekers_{spel}_lastig", title, desc, MUNTEN[spel], [adv(f"guhs:grote_guhspelen/klassiekers_{spel}_lastig")],
          rewards=((MUNTEN[spel], 6),), deps=["klassiekers_niveaus"], x=-4 + 2 * i, y=2)
    q("klassiekers_kampioen", "Klassiekers-kampioen",
      "Speel alle vijf de klassiekers op &clastig&r. Dan ben jij de vahoegste guh van de Guhmensie! VAHOEG!",
      "guhs:guh_taart", [adv("guhs:grote_guhspelen/klassiekers_kampioen")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 8), ("guhs:guh_taart", 1)),
      deps=[f"klassiekers_{s}_lastig" for s in SPELLEN], x=0, y=4, shape="gear", xp=200)


BONES = {}
CLOTHES = []
