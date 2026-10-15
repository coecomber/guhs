"""
einde2 phase B/C (CONTRACT_E2_B 6, 7.11), slice audio: the music per area, the epic moments, the generated tunes and
stingers, the licences and credits. Java: nl.juiced.guhs.feature.einde2.audio (E2AudioFeature).

  einde2_audio_licenties.json   the licence ledger: every real piece (maker, source, licence, what was changed)
  einde2_audio_geluid.py        the sound files (run by hand: the OGGs are committed); imported here only for its lists
                                (it imports numpy at the top, scipy and soundfile only when it really makes sound)
  einde2_audio_wiki.py          the wiki dict (credits page)
  here                          sounds.json (every name of 6.1), the subtitles, the "now playing" names of the background
                                pieces, the credits book, Miroh's lullaby words, the credits file in the jar, the self-check

CI-safe: this module imports only the standard library (and numpy through einde2_audio_geluid).
"""
import json
import os
import re

import features.einde2_kit as kit
from features import einde2_audio_geluid as geluid

G = "gui.guhs.einde2_audio."
SUB = "subtitles.guhs.einde2_audio."
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "audio")
LEDGER = os.path.join("tools", "features", "einde2_audio_licenties.json")
# licences the user allowed (12a.2): CC0, CC BY (any version) and the Pixabay licence; never NC or ND
TOEGESTAAN = re.compile(r"^(CC0|CC BY [34]\.0|Pixabay)$")
MUZIEK_MAX_MB = 42.0

# the names of 6.1 (AudioNamen.java holds the same lists; the self-check compares them)
MUZIEK = ["lucht_1", "lucht_2", "water_1", "water_2", "aarde_1", "aarde_2", "vuur_1", "vuur_2", "geest_1", "geest_2"]
MOMENTEN = ["intro", "appa", "appa_achtervolging", "golf", "yue_maan", "appa_verloren", "lu_ten", "val", "zwarte_zon", "tent",
            "luchtvloot", "agni_kai", "duel", "duel_koor", "kroning", "aftiteling"]
DEUNTJES = ["theehuis", "thee_ting", "pai_sho", "luchtscooter", "momo", "slee", "monorail", "glijbaan", "dansfeest", "waaierdans",
            "ritmeduel"]
STINGERS = ["kolen", "toneel_fanfare", "feestje", "mijn_eer", "opgelost", "tegel"]
GEEST = ["koi_zang", "koh_kraak", "koh_stilte", "lantaarn_tinkel", "klok", "geestentij", "maannacht", "reiger"]
SOORTEN = [("muziek", MUZIEK), ("moment", MOMENTEN), ("deuntje", DEUNTJES), ("stinger", STINGERS), ("geest", GEEST)]


def alle():
    return [f"{s}.{n}" for s, namen in SOORTEN for n in namen]


def bestand(naam):
    """muziek.lucht_1 -> muziek_lucht_1 (assets/guhs/sounds/einde2_audio/<this>.ogg)."""
    return naam.replace(".", "_")


# where each piece sounds (the book, the wiki, the subtitles)
GEBIED_NAAM = {"lucht": "bij de Luchtnomaden", "water": "bij de Waterstam", "aarde": "in het Aarderijk", "vuur": "in de Vuurnatie",
               "geest": "in de Geestenwereld"}
MOMENT_NAAM = {
    "intro": "de openingsfilm", "appa": "vliegen op Appa-guh", "appa_achtervolging": "Zuka op je hielen", "golf": "de grote golf",
    "yue_maan": "Yue-guh wordt de maan", "appa_verloren": "Appa-guhs verloren dagen", "lu_ten": "Miroh's wiegenlied",
    "val": "de val van Ba Sing Guh", "zwarte_zon": "de Dag van de Zwarte Zon", "tent": "de tent van de Witte Lotus",
    "luchtvloot": "door de luchtvloot", "agni_kai": "de Agni Kai", "duel": "het grote duel", "duel_koor": "de Guhvatar-staat",
    "kroning": "de kroning", "aftiteling": "de aftiteling",
}


def waar(naam):
    soort, n = naam.split(".", 1)
    if soort == "muziek":
        return "Achtergrond " + GEBIED_NAAM[n.rsplit("_", 1)[0]]
    return MOMENT_NAAM[n][0].upper() + MOMENT_NAAM[n][1:]


SUBTITELS = {
    **{f"muziek.{n}": "Muziek " + GEBIED_NAAM[n.rsplit("_", 1)[0]] for n in MUZIEK},
    **{f"moment.{n}": "Muziek: " + MOMENT_NAAM[n] for n in MOMENTEN},
    "deuntje.theehuis": "Gezellig theehuisdeuntje", "deuntje.thee_ting": "Theekopje tingelt", "deuntje.pai_sho": "Rustig Pai Sho-deuntje",
    "deuntje.luchtscooter": "Luchtscooterdeuntje", "deuntje.momo": "Achter Momo-guh aan", "deuntje.slee": "Sleedeuntje",
    "deuntje.monorail": "Monorail dendert", "deuntje.glijbaan": "Glijbaandeuntje", "deuntje.dansfeest": "Geheim dansfeest",
    "deuntje.waaierdans": "Waaierdans", "deuntje.ritmeduel": "Trommels van het ritmeduel",
    "stinger.kolen": "Kolenkar kiepert om", "stinger.toneel_fanfare": "Trotse fanfare (bijna goed)", "stinger.feestje": "Er is een feestje!",
    "stinger.mijn_eer": "Dramatische trom", "stinger.opgelost": "Raadsel opgelost", "stinger.tegel": "Pai Sho-tegel tikt",
    "geest.koi_zang": "Geestkoi zingt", "geest.koh_kraak": "Iets kraakt in het donker", "geest.koh_stilte": "IJzige stilte",
    "geest.lantaarn_tinkel": "Lantaarngeesten tinkelen", "geest.klok": "De Geestenklok luidt", "geest.geestentij": "De geestentij ruist",
    "geest.maannacht": "Maanlicht glinstert", "geest.reiger": "De reigergeest roept",
}

# Miroh's lullaby: own words (CONTRACT_E2_B 6.3: nothing of the series; a young guh soldier coming home)
LIED = {
    "neuriet": "♪ Miroh neuriet zacht ♪",
    "1": "Over de heuvels, door gras en door wind,",
    "2": "loopt er een guhtje, nog bijna een kind.",
    "3": "Zijn helm is te groot en zijn oortjes zijn moe,",
    "4": "de maan wijst de weg naar zijn huisje toe.",
    "5": "De lantaarn brandt en de thee staat al klaar,",
    "6": "het kussen ligt zacht, net als ieder jaar.",
    "7": "Leg neer je speer, want de nacht is nu zacht,",
    "8": "slaap maar, mijn guhtje, er wordt op je gewacht.",
}

BOEK = {
    "titel": "De Muziek van het Guheinde",
    "lore": "Wie maakte de muziek die je hoort?",
    "gekregen": "Je krijgt het Muziekboekje van het Guheinde: alle makers van de muziek die je onderweg hoorde, njeg.",
}


def ledger():
    return json.load(open(LEDGER, encoding="utf-8"))["stukken"]


def boek_paginas():
    """The pages of the credits book (Muziekboekje.PAGINAS = their number)."""
    stukken = ledger()
    paginas = ["De Muziek van het Guheinde\n\nWie door het Guheinde reist, hoort muziek van echte makers. Die mogen we "
               "gebruiken omdat zij hun muziek vrij hebben gegeven. Dit boekje noemt ze allemaal.\n\nDank je wel, njeg!"]
    for i in range(0, len(stukken), 2):
        delen = []
        for s in stukken[i:i + 2]:
            delen.append(f"{s['titel']}\n{s['maker'].replace(' (incompetech.com)', '')}\n{s['licentie']}\n- {waar(s['naam'])}")
        paginas.append("\n\n".join(delen))
    paginas.append("Zelf gemaakt\n\nMiroh's wiegenlied, de deuntjes van het theehuis en de spelletjes, de grapjes en de "
                   "geluiden van de Geestenwereld maakten de guhs zelf.\n\n'Happy Birthday' is een oud liedje van iedereen.")
    makers = sorted({s["maker"] for s in stukken})
    licenties = sorted({(s["licentie"], s["licentie_url"]) for s in stukken})
    paginas.append("Makers\n" + "\n".join(makers) + "\n\nLicenties\n" + "\n".join(f"{l}: {u.replace('https://', '')}" for l, u in licenties)
                   + "\n\nBronnen: incompetech.com, opengameart.org")
    return paginas


def texts(h):
    for naam, tekst in SUBTITELS.items():
        kit.lang(h, SUB + naam, tekst)
    # the "now playing" toast of the background pieces: guhs.einde2_audio.<file> = "Maker - Title"
    for s in ledger():
        if s["naam"].startswith("muziek."):
            kit.lang(h, "guhs.einde2_audio." + bestand(s["naam"]), f"{s['maker'].replace(' (incompetech.com)', '')} - {s['titel']}")
    for k, v in LIED.items():
        kit.lang(h, G + "lied.lu_ten." + k, v)
    for k, v in BOEK.items():
        kit.lang(h, G + "boek." + k, v)
    for i, p in enumerate(boek_paginas()):
        kit.lang(h, G + f"boek.{i}", p)


def sounds(h):
    def patch(d):
        for k in [k for k in d if k.startswith("einde2_audio.")]:
            del d[k]
        for naam in alle():
            lang = naam.startswith(("muziek.", "moment.")) or (naam.startswith("deuntje.") and naam != "deuntje.thee_ting")
            geluid_ = {"name": f"guhs:einde2_audio/{bestand(naam)}"}
            if lang:
                geluid_["stream"] = True
            d[f"einde2_audio.{naam}"] = {"sounds": [geluid_], "subtitle": SUB + naam}
    h.patch_json(f"{h.A}/sounds.json", patch)


def credits_bestand(h):
    """The credits as a plain text file in the jar (CC BY asks for the credit where the work is): assets/guhs/einde2_audio/credits.txt."""
    regels = ["Guhs - einde2 - music credits", "", "Real pieces of music, used under their licences (mono, loudness evened out;",
              "pieces marked 'trimmed' were shortened with a fade-out):", ""]
    for s in ledger():
        regels.append(f"{s['naam']}: \"{s['titel']}\" by {s['maker']}")
        regels.append(f"    {s['bron_url']}")
        regels.append(f"    Licensed under {s['licentie']} ({s['licentie_url']})" + ("; trimmed" if s.get("knip") else ""))
    regels += ["", "Made by the Guhs mod itself: Miroh's lullaby (own melody and words), the tunes, the stingers ('Happy Birthday'",
               "is a public-domain melody) and the sounds of the Geestenwereld.", ""]
    pad = os.path.join(h.A, "einde2_audio", "credits.txt")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    tekst = "\n".join(regels)
    if not os.path.exists(pad) or open(pad, encoding="utf-8").read() != tekst:
        with open(pad, "w", encoding="utf-8", newline="\n") as f:
            f.write(tekst)


def _java_lijst(bron, naam):
    m = re.search(naam + r"\s*=\s*List\.of\(([^;]*)\);", bron)
    return re.findall(r'"([a-z0-9_]+)"', m.group(1)) if m else None


def selfcheck(h):
    problems = []
    namen = open(os.path.join(JAVA, "AudioNamen.java"), encoding="utf-8").read()
    for soort, lijst in SOORTEN:
        java = _java_lijst(namen, {"muziek": "MUZIEK", "moment": "MOMENTEN", "deuntje": "DEUNTJES", "stinger": "STINGERS", "geest": "GEEST"}[soort])
        if java != lijst:
            problems.append(f"AudioNamen.java and einde2_audio.py list different {soort} names")
    for naam in alle():
        if not os.path.exists(os.path.join(h.A, "sounds", "einde2_audio", bestand(naam) + ".ogg")):
            problems.append(f"sound file {bestand(naam)}.ogg is missing: run python tools/features/einde2_audio_geluid.py "
                            "(--muziek <bron> for the real pieces)")
        if naam not in SUBTITELS:
            problems.append(f"no subtitle for {naam}")
    eigen = set(geluid.EIGEN)
    stukken = ledger()
    echte = {bestand(s["naam"]) for s in stukken}
    for naam in alle():
        b = bestand(naam)
        if (b in eigen) == (b in echte):
            problems.append(f"{naam}: must be made by the mod OR be a real piece in the ledger (exactly one)")
    for s in stukken:
        for veld in ("naam", "titel", "maker", "bron_url", "licentie", "licentie_url", "bewerkt", "bestand", "datum"):
            if not s.get(veld):
                problems.append(f"ledger: {s.get('naam')} has no {veld}")
        if not TOEGESTAAN.match(s.get("licentie", "")) or re.search(r"\b(NC|ND)\b", s.get("licentie", "")):
            problems.append(f"ledger: {s.get('naam')} has a licence the user did not allow: {s.get('licentie')}")
        if not s.get("naam", "").startswith(("muziek.", "moment.")):
            problems.append(f"ledger: {s.get('naam')} is not a background piece or a moment")
    groot = sum(os.path.getsize(os.path.join(h.A, "sounds", "einde2_audio", b + ".ogg")) for b in echte
                if os.path.exists(os.path.join(h.A, "sounds", "einde2_audio", b + ".ogg"))) / 1024 / 1024
    if groot > MUZIEK_MAX_MB:
        problems.append(f"the real music is {groot:.1f} MB: more than the cap of about 40 MB (12a.2)")
    boek = open(os.path.join(JAVA, "Muziekboekje.java"), encoding="utf-8").read()
    m = re.search(r"PAGINAS = (\d+);", boek)
    if not m or int(m.group(1)) != len(boek_paginas()):
        problems.append(f"Muziekboekje.PAGINAS is not {len(boek_paginas())} (the pages written here)")
    lied = open(os.path.join(JAVA, "LuTenLied.java"), encoding="utf-8").read()
    maten = [(int(a), int(b)) for a, b in re.findall(r'new Regel\(K \+ "\d", (\d+) \* TICKS_PER_MAAT, (\d+) \* TICKS_PER_MAAT\)', lied)]
    if maten != geluid.REGELS:
        problems.append("LuTenLied.REGELS and einde2_audio_geluid.REGELS put the sung lines on different bars")
    for k in set(re.findall(r'K \+ "([a-z0-9]+)"', lied)):
        if k not in LIED:
            problems.append(f"LuTenLied asks for the words {k}, which are not in LIED")
    tel = sum(n[1] for n in geluid.LU_TEN)
    if tel != 93 or len(geluid.REGELS) != 8:
        problems.append(f"the lullaby has {tel} beats (93 expected: 31 bars of 3)")
    if problems:
        raise SystemExit("einde2_audio:\n  " + "\n  ".join(problems))


def build(h):
    texts(h)
    sounds(h)
    credits_bestand(h)
    selfcheck(h)
