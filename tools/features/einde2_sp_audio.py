"""
einde2 "Sponsguh" (CONTRACT_E2_C 6, 7.9), slice sp_audio: the music of Bikinibodem, Schelpenstad and the loopgraaf, the eight
moments (three of them the own songs), the minigame tunes, the stingers, the licences.
Java: nl.juiced.guhs.feature.einde2.spaudio (E2SpAudioFeature, SpAudio, SpLiederen). Chunk c118_e2_sp_audio.

  einde2_sp_audio_liederen.py     the three songs as data: their sung lines, beats and Dutch words
  einde2_sp_audio_geluid.py       the sound files (run by hand: the OGGs are committed; NOT imported here)
  einde2_sp_audio_licenties.json  the licence ledger of every real piece
  einde2_sp_audio_wiki.py         the wiki dict (credits page)
  here                            sounds.json (every name of 6.1), the subtitles, the "now playing" names, the song words,
                                  the credits file in the jar, the self-check (Java lists and song ticks, files, ledger)

CI-safe: standard library only.
"""
import json
import os
import re

import features.einde2_kit as kit
from features import einde2_sp_audio_liederen as liederen

SUB = "subtitles.guhs.einde2_sp_audio."
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "spaudio")
LEDGER = os.path.join("tools", "features", "einde2_sp_audio_licenties.json")
GELUID = os.path.join("tools", "features", "einde2_sp_audio_geluid.py")
# licences the user allowed (C-B 12a.2): CC0, CC BY (any version) and the Pixabay licence; never NC or ND
TOEGESTAAN = re.compile(r"^(CC0|CC BY [34]\.0|Pixabay)$")
# the real music of the Sponsguh story (the Guhvatar music has its own cap of about 40 MB)
MUZIEK_MAX_MB = 11.0

# the names of 6.1 (SpAudio.java holds the same lists; the self-check compares them)
MUZIEK = ["muziek.bikinibodem_1", "muziek.bikinibodem_2", "muziek.schelpenstad", "muziek.loopgraaf"]
MOMENTEN = ["moment.zoete_zege", "moment.guhfy_guhber", "moment.monsters", "moment.plan_z", "moment.bevroren", "moment.cycloop",
            "moment.hasselguh", "moment.dennis"]
DEUNTJES = ["deuntje.bakken", "deuntje.kwallen", "deuntje.examen", "deuntje.sluipen", "deuntje.trommel", "deuntje.kroeg",
            "deuntje.ijssalon", "deuntje.emmers"]
STINGERS = ["stinger.verteller", "stinger.klarinet", "stinger.karen", "stinger.bevries", "stinger.snor", "stinger.opgelost",
            "stinger.mislukt", "stinger.lach"]
ALLE = MUZIEK + MOMENTEN + DEUNTJES + STINGERS
BPM = {"deuntje.trommel": 112, "deuntje.kroeg": 96}

SUBTITELS = {
    "muziek.bikinibodem_1": "Muziek in Bikinibodem", "muziek.bikinibodem_2": "Muziek in Bikinibodem",
    "muziek.schelpenstad": "Muziek in Schelpenstad", "muziek.loopgraaf": "Griezelig-grappige muziek in de loopgraaf",
    "moment.zoete_zege": "Lied: Zoete Knabbelzege", "moment.guhfy_guhber": "Lied: Ik ben een Guhfy Guhber",
    "moment.monsters": "De monsters van de loopgraaf zingen", "moment.plan_z": "Muziek: een gemeen plannetje",
    "moment.bevroren": "Muziek: de kroon is weg", "moment.cycloop": "Muziek: spanning in de souvenirwinkel",
    "moment.hasselguh": "Muziek: zwemmen naar huis", "moment.dennis": "Muziek: de achtervolging",
    "deuntje.bakken": "Vrolijk grilldeuntje", "deuntje.kwallen": "Huppelend kwallenvangdeuntje",
    "deuntje.examen": "Zenuwachtig examendeuntje", "deuntje.sluipen": "Op-je-tenen-sluipdeuntje",
    "deuntje.trommel": "De band speelt een beat", "deuntje.kroeg": "Loom kroegdeuntje", "deuntje.ijssalon": "Zoet ijssalondeuntje",
    "deuntje.emmers": "Emmertjesmars",
    "stinger.verteller": "De verteller: een accordeontje", "stinger.klarinet": "Octo's klarinet piept vals",
    "stinger.karen": "Karen piept", "stinger.bevries": "Iets bevriest", "stinger.snor": "Boing! Een snor",
    "stinger.opgelost": "Gelukt!", "stinger.mislukt": "Boing (net niet)", "stinger.lach": "Sponsguh giechelt",
}

# where each real piece sounds (credits, wiki): no spoilers beyond "a big moment"
WAAR = {
    "muziek.bikinibodem_1": "achtergrond in Bikinibodem", "muziek.bikinibodem_2": "achtergrond in Bikinibodem",
    "muziek.schelpenstad": "achtergrond in Schelpenstad", "muziek.loopgraaf": "achtergrond in de loopgraaf",
}


def bestand(naam):
    """muziek.bikinibodem_1 -> muziek_bikinibodem_1 (assets/guhs/sounds/einde2_sp_audio/<this>.ogg)."""
    return naam.replace(".", "_")


def ledger():
    return json.load(open(LEDGER, encoding="utf-8"))["stukken"]


def maker(s):
    return s["maker"].replace(" (incompetech.com)", "")


def texts(h):
    for naam, tekst in SUBTITELS.items():
        kit.lang(h, SUB + naam, tekst)
    # the "now playing" toast of the background pieces: guhs.einde2_sp_audio.<file> = "Maker - Title"
    for s in ledger():
        if s["naam"].startswith("muziek."):
            kit.lang(h, "guhs.einde2_sp_audio." + bestand(s["naam"]), f"{maker(s)} - {s['titel']}")
    # the words of the three songs, under their scenes (CONTRACT_E2_C 1.3: they live in this chunk)
    for scene, _, _, _, _, regels in liederen.LIEDEREN.values():
        for k, _, _, tekst in regels:
            kit.lang(h, f"scene.guhs.{scene}.lied.{k}", tekst)


def sounds(h):
    def patch(d):
        for k in [k for k in d if k.startswith("einde2_sp_audio.")]:
            del d[k]
        for naam in ALLE:
            geluid = {"name": f"guhs:einde2_sp_audio/{bestand(naam)}"}
            if not naam.startswith("stinger."):
                geluid["stream"] = True
            d[f"einde2_sp_audio.{naam}"] = {"sounds": [geluid], "subtitle": SUB + naam}
    h.patch_json(f"{h.A}/sounds.json", patch)


def credits_bestand(h):
    """The credits as a plain text file in the jar (CC BY asks for the credit where the work is)."""
    regels = ["Guhs - einde2 - the Sponsguh story - music credits", "",
              "Real pieces of music, used under their licences (mono, loudness evened out; pieces marked 'trimmed' were",
              "shortened with a fade-out):", ""]
    for s in ledger():
        regels.append(f"{s['naam']}: \"{s['titel']}\" by {s['maker']}")
        regels.append(f"    {s['bron_url']}")
        regels.append(f"    Licensed under {s['licentie']} ({s['licentie_url']})" + ("; trimmed" if s.get("knip") else ""))
    regels += ["", "Made by the Guhs mod itself (own melodies, own Dutch words): the songs \"Zoete Knabbelzege\", \"Ik ben een",
               "Guhfy Guhber\" and the song of the loopgraaf monsters, the minigame tunes and the stingers.", ""]
    pad = os.path.join(h.A, "einde2_sp_audio", "credits.txt")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    tekst = "\n".join(regels)
    if not os.path.exists(pad) or open(pad, encoding="utf-8").read() != tekst:
        with open(pad, "w", encoding="utf-8", newline="\n") as f:
            f.write(tekst)


def _java_lijst(bron, naam):
    m = re.search(naam + r"\s*=\s*List\.of\(([^;]*)\);", bron)
    return re.findall(r'"([a-z0-9_.]+)"', m.group(1)) if m else None


def eigen_namen():
    """The files einde2_sp_audio_geluid.py makes (its EIGEN dict, read as text: the generator needs numpy and scipy)."""
    bron = open(GELUID, encoding="utf-8").read()
    m = re.search(r"^EIGEN = \{(.*?)^\}", bron, re.S | re.M)
    return set(re.findall(r'"((?:moment|deuntje|stinger|muziek)_[a-z0-9_]+)":', m.group(1))) if m else set()


def selfcheck(h):
    problems = []
    api = open(os.path.join(JAVA, "SpAudio.java"), encoding="utf-8").read()
    for naam, lijst in (("MUZIEK", MUZIEK), ("MOMENTEN", MOMENTEN), ("DEUNTJES", DEUNTJES), ("STINGERS", STINGERS)):
        if _java_lijst(api, naam) != lijst:
            problems.append(f"SpAudio.{naam} and einde2_sp_audio.py differ")
    for k, v in BPM.items():
        if not re.search(rf'"{re.escape(k)}", {v}\b', api):
            problems.append(f"SpAudio.BPM does not say {k} = {v}")
    for lied, (scene, _, _, _, _, _) in liederen.LIEDEREN.items():
        if f'"{lied}", "{scene}"' not in api:
            problems.append(f"SpAudio.LIEDEREN does not map {lied} to {scene}")
    # the song ticks: SpLiederen.java = einde2_sp_audio_liederen.py
    java = open(os.path.join(JAVA, "SpLiederen.java"), encoding="utf-8").read()
    for lied in liederen.LIEDEREN:
        m = re.search(rf'new Lied\("{lied}", "([a-z0-9_]+)", (\d+), List\.of\((.*?)\)\);', java, re.S)
        if not m:
            problems.append(f"SpLiederen.java has no song {lied}")
            continue
        regels = [(k, int(a), int(b)) for k, a, b in re.findall(r'new Regel\("([a-z0-9]+)", (\d+), (\d+)\)', m.group(3))]
        if m.group(1) != liederen.LIEDEREN[lied][0] or int(m.group(2)) != liederen.duur_ticks(lied) or regels != liederen.regels_ticks(lied):
            problems.append(f"SpLiederen.java and einde2_sp_audio_liederen.py differ for {lied}")
        for k, b, n in liederen.regels_ticks(lied):
            if b + n > liederen.duur_ticks(lied):
                problems.append(f"{lied}: line {k} runs past the end of the song")
    # files, subtitles, own XOR real
    eigen = eigen_namen()
    stukken = ledger()
    echte = {bestand(s["naam"]) for s in stukken}
    for naam in ALLE:
        b = bestand(naam)
        if not os.path.exists(os.path.join(h.A, "sounds", "einde2_sp_audio", b + ".ogg")):
            problems.append(f"sound file {b}.ogg is missing: run python tools/features/einde2_sp_audio_geluid.py "
                            "(--muziek <bron> for the real pieces)")
        if naam not in SUBTITELS:
            problems.append(f"no subtitle for {naam}")
        if (b in eigen) == (b in echte):
            problems.append(f"{naam}: must be made by the mod OR be a real piece in the ledger (exactly one)")
    for s in stukken:
        for veld in ("naam", "titel", "maker", "bron_url", "licentie", "licentie_url", "bewerkt", "bestand", "datum"):
            if not s.get(veld):
                problems.append(f"ledger: {s.get('naam')} has no {veld}")
        if not TOEGESTAAN.match(s.get("licentie", "")) or re.search(r"\b(NC|ND)\b", s.get("licentie", "")):
            problems.append(f"ledger: {s.get('naam')} has a licence the user did not allow: {s.get('licentie')}")
        if s.get("naam") not in MUZIEK + MOMENTEN:
            problems.append(f"ledger: {s.get('naam')} is not a background piece or a moment of 6.1")
        if s.get("naam", "").split(".", 1)[-1] in liederen.LIEDEREN:
            problems.append(f"ledger: {s.get('naam')} is one of the own songs (own melody and words, never a real piece)")
    groot = sum(os.path.getsize(os.path.join(h.A, "sounds", "einde2_sp_audio", b + ".ogg")) for b in echte
                if os.path.exists(os.path.join(h.A, "sounds", "einde2_sp_audio", b + ".ogg"))) / 1024 / 1024
    if groot > MUZIEK_MAX_MB:
        problems.append(f"the real Sponsguh music is {groot:.1f} MB: more than {MUZIEK_MAX_MB} MB")
    if problems:
        raise SystemExit("einde2_sp_audio:\n  " + "\n  ".join(problems))


def build(h):
    texts(h)
    sounds(h)
    credits_bestand(h)
    selfcheck(h)


def ftb(fq):
    pass
