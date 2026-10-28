"""
1.8.0: the sounds of Guhdromenland that the user records himself: the music of every island and the voices of the dream
NPCs. One folder each (inspiration/Dromen audio/<map>/) <-> one sound event each (guhs:dromen.<id>,
feature/droomsfeer/DroomGeluid.java). Music folders are muziek/<eiland> (id muziek.<eiland>).

    python tools/dromen_audio_import.py              (from the guhs project root, or any guhs worktree root)

reads every folder, turns each audio file in it (WhatsApp .opus/.ogg, .mp3, .wav, .flac... anything soundfile reads) into a
mono, trimmed, normalised Ogg Vorbis in assets/guhs/sounds/dromen/<id>/, and rewrites the sound events in sounds.json (a
folder with several files: the game picks one at random each time; an empty folder: no sound). Music is streamed. Files
whose name starts with "_" and LEESMIJ files are skipped. Also writes LEESMIJ.md and the empty folders' LEESMIJ.txt.

    python tools/dromen_audio_import.py --placeholders

(once, for the test build) renders the current placeholder sounds into the folders, AS THEY SOUND IN THE GAME (pitched like
Minecraft does: faster = higher), then imports them.
"""
import argparse
import json
import os
import re
import sys
from collections import namedtuple
from fractions import Fraction

ROOT = os.getcwd()
GUHS_ASSETS = os.path.join("src", "main", "resources", "assets", "guhs")
SOUNDS_JSON = os.path.join(GUHS_ASSETS, "sounds.json")
SOUND_DIR = os.path.join(GUHS_ASSETS, "sounds", "dromen")
# the folders live next to the projects: MineScapeStory/inspiration/Dromen audio
BASE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "inspiration", "Dromen audio"))
if not os.path.isdir(os.path.dirname(BASE)):
    BASE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "inspiration", "Dromen audio"))
AUDIO = (".ogg", ".opus", ".mp3", ".wav", ".flac", ".m4a", ".oga", ".aiff", ".aif")

# id (= event guhs:dromen.<id> = DroomGeluid), folder, subtitles, when it plays, how often, the pitch variation (as in
# DroomGeluid.java), placeholders: (source, pitch as heard, how many files). Sources: "g:<path>" = a guhs sound
# (assets/guhs/sounds/<path>.ogg, "{n}" = 1, 2, ...), "v:<path>" = a vanilla sound file.
Actie = namedtuple("Actie", "id map wanneer hoe_vaak variatie placeholders")
MUZIEK_WANNEER = "De muziek van {naam}: speelt als je in Guhdromenland het dichtst bij {waar} bent. Loop je naar een ander eiland, dan stopt hij en begint de muziek van dat eiland (niet door elkaar heen)."
ACTIES = [
    Actie("muziek.plein", "muziek/plein", MUZIEK_WANNEER.format(naam="het Droomplein", waar="het Droomplein"),
          "Steeds opnieuw, met 1 tot 20 seconden stilte ertussen.", "geen", [("g:hemel/muziek", 1.0, 1)]),
    Actie("muziek.liedjes", "muziek/liedjes", MUZIEK_WANNEER.format(naam="de Slaapliedjesheuvels", waar="de Slaapliedjesheuvels"),
          "Steeds opnieuw, met 1 tot 20 seconden stilte ertussen.", "geen", [("g:hemel/muziek", 0.84, 1)]),
    Actie("muziek.sterren", "muziek/sterren", MUZIEK_WANNEER.format(naam="het Sterrenkussenveld", waar="het Sterrenkussenveld"),
          "Steeds opnieuw, met 1 tot 20 seconden stilte ertussen.", "geen", [("g:hemel/muziek", 1.12, 1)]),
    Actie("muziek.herinnering", "muziek/herinnering", MUZIEK_WANNEER.format(naam="het Herinneringseiland", waar="het Herinneringseiland"),
          "Steeds opnieuw, met 1 tot 20 seconden stilte ertussen.", "geen", [("g:hemel/muziek", 0.9, 1)]),
    Actie("muziek.badje", "muziek/badje", MUZIEK_WANNEER.format(naam="het Wolkenbadje", waar="het Wolkenbadje"),
          "Steeds opnieuw, met 1 tot 20 seconden stilte ertussen.", "geen", [("g:hemel/muziek", 1.06, 1)]),
    Actie("muziek.molentje", "muziek/molentje", MUZIEK_WANNEER.format(naam="het Maanmolentje", waar="het Maanmolentje"),
          "Steeds opnieuw, met 1 tot 20 seconden stilte ertussen.", "geen", [("g:hemel/muziek", 0.8, 1)]),
    Actie("snoes_praat", "snoes_praat", "Als je met Snoes praat (verfomfaaid, voor zijn verhaal af is).", "Bij elke klik (2 van de 3 keer; de andere keer gaapt hij).",
          "+-0,08", [("g:guh_ambient{n}", 0.8, 2)]),
    Actie("snoes_geeuw", "snoes_geeuw", "Snoes gaapt als je met hem praat.", "1 van de 3 klikken.", "+-0,05", [("g:guh_ambient{n}", 0.6, 1)]),
    Actie("snoes_blij", "snoes_blij", "Als je met de blije Snoes praat (na zijn verhaal, op het Droomplein).", "Bij elke klik.", "+-0,08",
          [("g:guh_ambient{n}", 1.25, 2)]),
    Actie("zandguh", "zandguh", "Als je met de Zandguh praat (Maanmolentje).", "Bij elke klik.", "+-0,08", [("g:guh_ambient{n}", 0.9, 2)]),
    Actie("wekkerguh", "wekkerguh", "Als je met de Wekkerguh praat (Droomplein). Zijn wekker-rinkel is een eigen geluid (droomwezens.rinkel) en blijft.",
          "Bij elke klik.", "+-0,08", [("g:guh_ambient{n}", 1.1, 2)]),
    Actie("rommelguh", "rommelguh", "Als je met Rommelguh praat (Herinneringseiland).", "Bij elke klik.", "+-0,08", [("g:guh_ambient{n}", 1.0, 2)]),
    Actie("oma", "oma", "Als je met Oma-guh praat (Herinneringseiland).", "Bij elke klik.", "+-0,06", [("g:guh_ambient{n}", 0.85, 2)]),
    Actie("kussengevecht", "kussengevecht", "Als je met een kussengevecht-guh praat (Sterrenkussenveld). Het kussen zelf (plof) is een eigen geluid en blijft.",
          "Bij elke klik.", "+-0,10", [("g:guh_ambient{n}", 1.3, 2)]),
    Actie("badmeester", "badmeester", "Als je met Badmeester Bubbels praat (badhuisje bij de Bubbelsap-poeltjes, Zingende Weide).", "Bij elke klik.",
          "+-0,08", [("g:guh_ambient{n}", 1.05, 2)]),
    Actie("sterrenvisje", "sterrenvisje", "Een sterrenvisje twinkelt zachtjes in het water (zoals een vis af en toe een geluidje maakt).",
          "Af en toe, heel zacht.", "+-0,15", [("v:block/amethyst/shimmer", 1.6, 1)]),
]
MUZIEK = {a.id for a in ACTIES if a.id.startswith("muziek.")}


def _vanilla_index():
    base = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "assets")
    with open(os.path.join(base, "indexes", "30.json"), encoding="utf-8") as f:
        objects = json.load(f)["objects"]
    return base, objects


def _lees(pad):
    import numpy as np
    import soundfile as sf
    audio, rate = sf.read(pad, dtype="float32", always_2d=True)
    return audio.mean(axis=1).astype(np.float32), rate


def _nummer(i):
    """Which guh_ambient<n> the i-th placeholder of a folder uses (a spread over the 16, so they differ)."""
    return [1, 5, 9, 13][i % 4]


def _bron(naam, i):
    """(samples, rate) of a placeholder source (i = the how-manieth file of it, 0-based)."""
    if naam.startswith("g:"):
        pad = naam[2:].replace("{n}", str(_nummer(i)))
        return _lees(os.path.join(GUHS_ASSETS, "sounds", pad + ".ogg"))
    base, objects = _vanilla_index()
    h = objects[f"minecraft/sounds/{naam[2:]}.ogg"]["hash"]
    return _lees(os.path.join(base, "objects", h[:2], h))


def _pitch(x, pitch):
    """Like Minecraft: play the samples faster (higher, shorter) or slower."""
    import numpy as np
    from scipy.signal import resample_poly
    if abs(pitch - 1.0) < 1e-6:
        return x
    frac = Fraction(pitch).limit_denominator(200)
    return resample_poly(x, frac.denominator, frac.numerator).astype(np.float32)


def _schrijf(pad, x, rate):
    import numpy as np
    import soundfile as sf
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.9
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    # (in blocks: libsndfile's Vorbis writer overflows its stack on a long file written in one go)
    with sf.SoundFile(pad, "w", samplerate=rate, channels=1, format="OGG", subtype="VORBIS") as f:
        for i in range(0, len(x), 4096):
            f.write(x[i:i + 4096])


def placeholders():
    """Renders the current placeholder sounds into the folders (as they sound in the game)."""
    for a in ACTIES:
        map_ = os.path.join(BASE, *a.map.split("/"))
        os.makedirs(map_, exist_ok=True)
        n = 0
        for bron, pitch, aantal in a.placeholders:
            for i in range(aantal):
                x, rate = _bron(bron, i)
                naam = re.sub(r"[^a-z0-9]+", "_", bron[2:].replace("{n}", str(_nummer(i))).split("/")[-1])
                n += 1
                _schrijf(os.path.join(map_, f"placeholder_{n}_{naam}.ogg"), _pitch(x, pitch), rate)
    print(f"placeholders: {len(ACTIES)} folders in {BASE}")


def _bestanden(map_):
    if not os.path.isdir(map_):
        return []
    return sorted(f for f in os.listdir(map_) if f.lower().endswith(AUDIO) and not f.startswith("_") and not f.upper().startswith("LEESMIJ"))


def _naam(bestand, n, gebruikt):
    """A resource-safe name from the file name (lower case, a-z 0-9 _), unique in its folder."""
    stam = re.sub(r"[^a-z0-9_]+", "_", os.path.splitext(bestand)[0].lower()).strip("_") or str(n)
    naam, k = stam, 2
    while naam in gebruikt:
        naam, k = f"{stam}_{k}", k + 1
    gebruikt.add(naam)
    return naam


def _doel(a):
    return os.path.join(SOUND_DIR, a.id.replace(".", "_"))


def importeer():
    """The folders -> assets/guhs/sounds/dromen/<id>/<naam>.ogg (mono, trimmed, normalised, normal pitch) + sounds.json."""
    import shutil
    sys.path.insert(0, os.path.join(ROOT, "tools"))
    from import_sound import trim_silence
    import numpy as np
    totaal = 0
    for a in ACTIES:
        doel = _doel(a)
        if os.path.isdir(doel):
            shutil.rmtree(doel)
        gebruikt = set()
        bron = os.path.join(BASE, *a.map.split("/"))
        for n, f in enumerate(_bestanden(bron), 1):
            x, rate = _lees(os.path.join(bron, f))
            x = trim_silence(x, rate)
            fade = min(len(x) // 4, int((0.5 if a.id in MUZIEK else 0.01) * rate))
            if fade > 0:
                x[:fade] *= np.linspace(0, 1, fade)
                x[-fade:] *= np.linspace(1, 0, fade)
            _schrijf(os.path.join(doel, f"{_naam(f, n, gebruikt)}.ogg"), x, rate)
            totaal += 1
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        d = json.load(f)
    sound_events(d)
    with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"dromen audio: {totaal} files in {len(ACTIES)} sound events")


def sound_events(d):
    """sounds.json: one event per folder with the imported files (in place), old dromen.* events dropped."""
    ids = {a.id for a in ACTIES}
    for k in [k for k in d if k.startswith("dromen.")]:
        if k[len("dromen."):] not in ids:
            del d[k]
    for a in ACTIES:
        doel = _doel(a)
        namen = sorted((f[:-4] for f in os.listdir(doel) if f.endswith(".ogg")), key=lambda s: (len(s), s)) if os.path.isdir(doel) else []
        sounds = []
        for n in namen:
            e = {"name": f"guhs:dromen/{os.path.basename(doel)}/{n}"}
            if a.id in MUZIEK:
                e["stream"] = True                                  # (a whole song)
                e["volume"] = 0.6
            sounds.append(e)
        d[f"dromen.{a.id}"] = {"sounds": sounds, "subtitle": f"subtitles.guhs.dromen.{a.id}"}


LEESMIJ_KOP = """# Dromen audio

Hier staan de geluiden van Guhdromenland die jij zelf kunt maken: **de muziek van elk eiland** (map `muziek/`) en **de stemmen
van de droomwezens**. Wat in een map staat, is precies wat je in het spel hoort. Nu zijn het nog *placeholders* (de bestaande
hemelmuziek op een andere toonhoogte, en guh-geluidjes, al op de juiste toonhoogte gezet).

## Spelregels
- **Weghalen en toevoegen mag.** Zet in een map zoveel geluidjes als je wilt: het spel kiest elke keer **willekeurig een** uit de
  map. Een lege map = geen geluid (bij een muziekmap: stilte op dat eiland).
- **Elk formaat** dat `tools/import_sound.py` aankan: WhatsApp-spraakberichten (.opus/.ogg), .mp3, .wav, .flac. Stereo wordt mono,
  stilte aan het begin en eind wordt weggeknipt, het volume wordt gelijkgetrokken. Muziek krijgt een zachte fade in en uit.
- **Lever op gewone toonhoogte in.** Het spel maakt bij de stemmen zelf een klein beetje variatie (zie de tabel).
- Bestanden die met `_` beginnen worden **niet** gebruikt.
- Daarna in de guhs-map (of de worktree) draaien:

      python tools/dromen_audio_import.py

  Dat zet de geluiden in de mod (`assets/guhs/sounds/dromen/<map>/`) en vult `sounds.json`. Daarna de client opnieuw starten.

## Per map
| Map | Wanneer | Hoe vaak | Toonhoogte-variatie in het spel | Nu |
|---|---|---|---|---|
"""


def leesmij():
    regels = []
    for a in ACTIES:
        map_ = os.path.join(BASE, *a.map.split("/"))
        n = len(_bestanden(map_))
        regels.append(f"| `{a.map}` | {a.wanneer} | {a.hoe_vaak} | {a.variatie} | {n} bestand(en), willekeurig gekozen |")
        os.makedirs(map_, exist_ok=True)
        txt = os.path.join(map_, "LEESMIJ.txt")
        if n == 0:
            with open(txt, "w", encoding="utf-8", newline="\n") as f:
                f.write(f"Nog geen geluid voor '{a.map}'.\nWanneer: {a.wanneer}\nZet hier een of meer geluidjes neer en draai python tools/dromen_audio_import.py\n")
        elif os.path.exists(txt):
            os.remove(txt)
    with open(os.path.join(BASE, "LEESMIJ.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write(LEESMIJ_KOP + "\n".join(regels) + "\n")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--placeholders", action="store_true", help="render the placeholder sounds into the folders first (once)")
    args = ap.parse_args()
    if args.placeholders:
        placeholders()
    importeer()
    leesmij()
