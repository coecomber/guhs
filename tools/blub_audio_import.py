"""
1.8.0: the Blub-guh's sounds, one folder per action (inspiration/Blub audio/<map>/) <-> one sound event each
(guhs:blubguh.<id>, feature/blub/BlubGeluid.java).

    python tools/blub_audio_import.py              (from the guhs project root, or any guhs worktree root)

reads every folder, turns each audio file in it (WhatsApp .opus/.ogg, .mp3, .wav, .flac... anything soundfile reads) into a
mono, trimmed, normalised Ogg Vorbis in assets/guhs/sounds/blubguh/<id>/, and rewrites the sound events in sounds.json (a
folder with several files: the game picks one at random each time; an empty folder: no sound). Files whose name starts with
"_" (examples, like the whole lullaby) and LEESMIJ files are skipped. Also writes LEESMIJ.md and the empty folders' LEESMIJ.txt.

    python tools/blub_audio_import.py --placeholders

(once, for the test build) renders the current placeholder sounds into the folders, AS THEY SOUND IN THE GAME (pitched like
Minecraft does: faster = higher), then imports them.

The game adds the pitch variation itself (BlubGeluid: 1 + (r1 - r2) * variatie, babies x1.3), so the files are delivered at
their normal pitch. Where shared maatje code plays a sound at a fixed pitch (pick-up and onto the shoulder 1.5, put down and
off the shoulder 1.3), sounds.json gets the inverse pitch so the file sounds as it is.
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
SOUND_DIR = os.path.join(GUHS_ASSETS, "sounds", "blubguh")
# the folders live next to the projects: MineScapeStory/inspiration/Blub audio
BASE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "inspiration", "Blub audio"))
if not os.path.isdir(os.path.dirname(BASE)):
    BASE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "inspiration", "Blub audio"))
AUDIO = (".ogg", ".opus", ".mp3", ".wav", ".flac", ".m4a", ".oga", ".aiff", ".aif")

# id (= folder = event guhs:blubguh.<id> = BlubGeluid), subtitles, when it plays, how often, the pitch variation (as in
# BlubGeluid.java), the fixed pitch shared code plays it at (sounds.json undoes it), placeholders: (source, pitch as heard,
# how many files). Sources: "piep" = the pieppiepmuisje's 5 peeps (assets/guhs/sounds/piep_ambient<n>.ogg), "v:<path>" =
# a vanilla sound file.
Actie = namedtuple("Actie", "id ondertitel_nl ondertitel_en wanneer hoe_vaak variatie codepitch placeholders")
ACTIES = [
    Actie("idle", "Blub piept", "Blub squeaks", "Zomaar, als hij rondhupt (niet als hij slaapt of verstopt zit).",
          "Gemiddeld 1x per 12 seconden (vanilla 'ambient' van een dier).", "vanilla stem-pitch: 0,8-1,2 (baby 1,3-1,7)", 1.0,
          [("piep", 1.45, 5)]),
    Actie("huppelen", "Blub huppelt", "Blub hops", "Bij elke stap/hup als hij loopt (zachtjes).", "Heel vaak (elke paar stappen).",
          "+-0,15", 1.0, [("v:mob/slime/small1", 1.8, 1), ("v:mob/slime/small2", 1.8, 1), ("v:mob/slime/small3", 1.8, 1)]),
    Actie("landen", "Blub landt", "Blub lands", "Als hij landt na een sprong (meer dan een paar tikken in de lucht).",
          "Na elke grote sprong (truc, plas, hupsen, vallen).", "+-0,10", 1.0, [("v:mob/slime/small4", 1.3, 1), ("v:mob/slime/small5", 1.3, 1)]),
    Actie("squish", "Blub squisht", "Blub squishes", "Squish-knuffel: klikken met een lege hand, of het trucje Squish.",
          "Bij elke klik.", "+-0,12", 1.0, [("v:mob/slime/small1", 1.5, 1), ("v:mob/slime/small3", 1.5, 1), ("v:mob/slime/small5", 1.5, 1)]),
    Actie("blij", "Blub piept blij", "Blub squeaks happily", "Als je hem aait (klik), en bij het trucje Oortjes: blij.",
          "Bij elke klik.", "+-0,12", 1.0, [("piep", 1.6, 3)]),
    Actie("schrik", "Blub schrikt", "Blub gets a fright", "Als iets hem slaat (hij is onkwetsbaar, maar schrikt wel), en bij Oortjes: verlegen.",
          "Bij elke klap.", "+-0,08", 1.0, [("piep", 2.0, 2)]),
    Actie("nieuwsgierig", "Blub is nieuwsgierig", "Blub is curious", "Als er iets nieuws in de buurt is (een item, een vreemde speler): oortjes in een krul. Ook bij Oortjes: nieuwsgierig.",
          "Hooguit 1x per paar seconden.", "+-0,10", 1.0, [("piep", 1.3, 2)]),
    Actie("eten", "Blub knabbelt", "Blub nibbles", "Als hij een kaasknabbel eet (uit je hand, van de grond, bij het temmen).",
          "Bij elke hap.", "+-0,10", 1.0, [("v:random/eat1", 1.6, 1), ("v:random/eat2", 1.6, 1), ("v:random/eat3", 1.6, 1)]),
    Actie("getemd", "Blub is getemd", "Blub is tamed", "Op het moment dat hij van jou wordt.", "1x.", "geen", 1.0, [("piep", 1.75, 1)]),
    Actie("baby", "Babyblubje!", "A baby blub!", "Als er een babyblubje geboren wordt.", "1x per baby.", "+-0,05", 1.0,
          [("v:mob/slime/small2", 2.0, 1), ("piep", 2.1, 1)]),
    Actie("slapen", "Blub snurkt", "Blub snores", "Zolang hij slaapt ('s nachts bij een guh of in een guhnestje).",
          "Elke 2,5 seconde, met een zzz.", "+-0,05", 1.0, [("v:mob/fox/sleep1", 1.6, 1), ("v:mob/fox/sleep2", 1.6, 1), ("v:mob/fox/sleep3", 1.6, 1)]),
    Actie("wakker", "Blub wordt wakker", "Blub wakes up", "Als hij wakker wordt (ochtend, of jij maakt hem wakker).", "1x.",
          "+-0,08", 1.0, [("piep", 1.2, 2)]),
    Actie("bellen", "Blub blaast bellen", "Blub blows bubbles", "Als hij bellen blaast (op het land af en toe; in het water elke 3 seconden).",
          "Op het land 1x per 15-35 seconden.", "+-0,15", 1.0,
          [("v:block/bubble_column/bubble1", 1.5, 1), ("v:block/bubble_column/bubble2", 1.5, 1), ("v:block/bubble_column/bubble3", 1.5, 1)]),
    Actie("plas", "Blub spettert", "Blub splashes", "Als hij in de regen in een plas springt.", "Elke 2-5 seconden zolang het regent.",
          "+-0,15", 1.0, [("v:liquid/splash", 1.6, 1), ("v:liquid/splash2", 1.6, 1)]),
    Actie("uitschudden", "Blub schudt zich uit", "Blub shakes itself dry", "Na een plas-sprong schudt hij zich soms uit.",
          "1 op 3 plas-sprongen.", "+-0,08", 1.0, [("v:mob/wolf/shake", 1.6, 1)]),
    Actie("liedje", "Blub piept een slaapliedje", "Blub squeaks a lullaby",
          "Het trucje Slaapliedje: 8 noten na elkaar. Het spel maakt de melodie zelf door deze noot hoger en lager af te spelen "
          "(toonhoogtes 1,0 - 1,19 - 1,33 - 1,19 - 1,0 - 0,89 - 1,0 - 0,75). Lever dus EEN noot in (of een paar varianten). "
          "_melodie_zoals_in_spel.ogg is alleen een voorbeeld en wordt niet geimporteerd.",
          "8 noten, elke 0,4 seconde.", "geen (de melodie zelf)", 1.0, [("piep", 1.74, 1)]),
    Actie("verstoppen", "Blub verstopt zich", "Blub hides", "Het trucje Verstoppertje: als hij wegrent om zich te verstoppen.", "1x.",
          "+-0,05", 1.0, [("piep", 1.3, 1)]),
    Actie("verstopt_piep", "Blub giechelt", "Blub giggles", "Terwijl hij verstopt zit: een heel zacht piepje als hint.",
          "Elke 6 seconden (een minuut lang).", "+-0,10", 1.0, [("piep", 1.9, 2)]),
    Actie("gevonden", "Blub is gevonden", "Blub was found",
          "Als je hem vindt (en een beetje lager als je hem NIET vond en hij zelf tevoorschijn komt).", "1x.", "+-0,05", 1.0, [("piep", 1.9, 1)]),
    Actie("truc_zit", "Blub gaat zitten", "Blub sits", "Het trucje Zitten.", "1x.", "+-0,05", 1.0, [("v:mob/slime/small2", 1.4, 1)]),
    Actie("truc_rondje", "Blub draait een rondje", "Blub spins", "Het trucje Rondje draaien.", "1x.", "+-0,05", 1.0, [("piep", 1.6, 1)]),
    Actie("truc_dood", "Blub valt om", "Blub flops over", "Het trucje Dood spelen: als hij omvalt.", "1x.", "+-0,05", 1.0,
          [("v:mob/slime/big1", 1.4, 1)]),
    Actie("truc_op", "Blub staat weer op", "Blub gets up", "Het trucje Dood spelen: als hij weer opstaat (2,3 seconde later).", "1x.",
          "+-0,05", 1.0, [("piep", 1.8, 1)]),
    Actie("truc_sprong", "Blub springt", "Blub jumps", "Het trucje Hoge sprong (met salto).", "1x.", "+-0,05", 1.0,
          [("v:mob/rabbit/hop1", 1.4, 1), ("v:mob/rabbit/hop2", 1.4, 1)]),
    Actie("hupsen", "Blub hupst", "Blub hops", "Het trucje Hupsen: bij elke sprong rond jou. Het spel speelt elke volgende sprong 6% hoger.",
          "6-10 sprongen, steeds sneller.", "geen (stijgt per sprong)", 1.0, [("v:mob/rabbit/hop3", 1.5, 1), ("v:mob/rabbit/hop4", 1.5, 1)]),
    Actie("tikkertje", "Blub tikt een guh", "Blub tags a guh", "Tikkertje met een guh: op het moment dat hij de guh tikt.",
          "Af en toe als er guhs in de buurt zijn.", "+-0,10", 1.0, [("piep", 1.7, 2)]),
    Actie("delen", "Blub deelt een knabbel", "Blub shares a knabbel", "Als hij een knabbel deelt met een guh.",
          "Af en toe als er guhs in de buurt zijn.", "+-0,08", 1.0, [("v:random/eat2", 1.8, 1), ("v:random/eat3", 1.8, 1)]),
    Actie("schouder_op", "Blub springt op je schouder", "Blub hops onto your shoulder", "Op je schouder (knop in het menu).", "1x.",
          "geen", 1.5, [("piep", 1.5 * 1.45, 1)]),
    Actie("schouder_af", "Blub springt van je schouder", "Blub hops off your shoulder", "Eraf (sluip + rechtsklik op een blok).", "1x.",
          "geen", 1.3, [("piep", 1.3 * 1.45, 1)]),
    Actie("oppakken", "Blub wordt opgepakt", "Blub is picked up", "Oppakken (sluip + klik, of de knop).", "1x.", "geen", 1.5,
          [("v:mob/slime/small4", 1.5 * 1.6, 1)]),
    Actie("neerzetten", "Blub wordt neergezet", "Blub is put down", "Neerzetten (rechtsklik met de opgepakte Blub op de grond).", "1x.",
          "geen", 1.3, [("v:mob/slime/small2", 1.3 * 1.6, 1)]),
    Actie("strikje", "Blub krijgt een strikje", "Blub gets a bow", "Als je in de Kledingkast een strikje kiest.", "Bij elke keuze.",
          "+-0,05", 1.0, [("v:item/armor/equip_leather1", 1.5, 1), ("v:item/armor/equip_leather2", 1.5, 1)]),
    Actie("glinster", "Blub glinstert", "Blub sparkles", "Een Pure Blub (meter 0) of Volle Guh (meter 100) glinstert af en toe.",
          "1 op 3 glinsteringen: ongeveer elke 4,5 seconde, heel zacht.", "+-0,20", 1.0, [("v:block/amethyst/shimmer", 1.5, 1)]),
    Actie("weg", "Blub is weg", "Blub is gone", "Alleen als hij met /kill of in de leegte verdwijnt (hij kan verder niet doodgaan).",
          "Bijna nooit.", "geen", 1.0, [("piep", 0.8, 1)]),
    Actie("knuffel_guh", "Blub knuffelt een guh", "Blub hugs a guh",
          "Als een tamme Blub en een tamme guh elkaar knuffelen (vanzelf, of het trucje Knuffelen met een guh).",
          "Ongeveer eens per paar minuten per paar dat bij elkaar in de buurt is (huisgenootjes wat vaker).", "+-0,08", 1.0,
          [("v:mob/slime/small3", 1.5, 1), ("piep", 1.7, 1)]),
    Actie("klus_besproeien", "Blub besproeit de plantjes", "Blub sprinkles the plants",
          "Klusje Bellenbesproeier: als hij bellen over een gewas blaast.", "1x per gewas (3 per keer).", "+-0,12", 1.0,
          [("v:block/bubble_column/bubble2", 1.2, 1), ("v:block/bubble_column/bubble3", 1.2, 1)]),
    Actie("klus_bezorgen", "Blub bezorgt een knabbel", "Blub delivers a knabbel",
          "Klusje Knabbelbezorger: als hij een knabbel afgeeft aan een guh of diertje.", "1x per knabbel.", "+-0,08", 1.0,
          [("piep", 1.6, 2)]),
    Actie("klus_zingen", "Blub zingt het slaapliedje", "Blub sings the lullaby",
          "Klusje Slaapliedjeszanger: 's avonds bij het huisje, 8 noten (het spel maakt de melodie zelf, net als bij 'liedje'). Lever een noot in.",
          "Eens per dag, 's avonds.", "geen (de melodie zelf)", 1.0, [("piep", 1.6, 1)]),
    Actie("klus_badderen", "Blub baddert", "Blub splashes in the bath",
          "Klusje Bubbelbadmeester (bad klaarmaken) en als een Blub zelf in het Blub-bubbelbadje baddert.",
          "Elke 1,5 seconde tijdens het klaarmaken; elke 5 seconden in bad.", "+-0,10", 1.0,
          [("v:liquid/splash", 1.4, 1), ("v:block/bubble_column/bubble1", 1.3, 1)]),
]

# (the lullaby's tune, BlubGuhEntity: tonen; for the example file)
LIEDJE = [1.0, 1.19, 1.33, 1.19, 1.0, 0.89, 1.0, 0.75]


def _vanilla_index():
    base = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "assets")
    with open(os.path.join(base, "indexes", "30.json"), encoding="utf-8") as f:
        objects = json.load(f)["objects"]
    return base, objects


def _lees(pad_of_bytes):
    import numpy as np
    import soundfile as sf
    audio, rate = sf.read(pad_of_bytes, dtype="float32", always_2d=True)
    return audio.mean(axis=1).astype(np.float32), rate


def _bron(naam, i):
    """(samples, rate) of a placeholder source: 'piep' = the muisje's peep number i, 'v:<path>' = a vanilla sound."""
    if naam == "piep":
        return _lees(os.path.join(GUHS_ASSETS, "sounds", f"piep_ambient{(i % 5) + 1}.ogg"))
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
    sf.write(pad, x, rate, format="OGG", subtype="VORBIS")


def placeholders():
    """Renders the current placeholder sounds into the folders (as they sound in the game)."""
    import numpy as np
    for a in ACTIES:
        map_ = os.path.join(BASE, a.id)
        os.makedirs(map_, exist_ok=True)
        n = 0
        for bron, pitch, aantal in a.placeholders:
            for i in range(aantal):
                x, rate = _bron(bron, i)
                naam = "piepje" if bron == "piep" else re.sub(r"[^a-z0-9]+", "_", bron[2:].split("/")[-1])
                n += 1
                _schrijf(os.path.join(map_, f"placeholder_{n}_{naam}.ogg"), _pitch(x, pitch), rate)
        if a.id == "liedje":                              # the whole tune, as an example (not imported: starts with "_")
            x, rate = _bron("piep", 0)
            noot = _pitch(x, a.placeholders[0][1])
            stap = int(rate * 0.4)
            uit = np.zeros(stap * len(LIEDJE) + len(noot) * 2, np.float32)
            for k, t in enumerate(LIEDJE):
                y = _pitch(noot, t)
                uit[k * stap:k * stap + len(y)] += y
            _schrijf(os.path.join(map_, "_melodie_zoals_in_spel.ogg"), uit, rate)
    print(f"placeholders: {len(ACTIES)} folders in {BASE}")


def _bestanden(map_):
    if not os.path.isdir(map_):
        return []
    return sorted(f for f in os.listdir(map_) if f.lower().endswith(AUDIO) and not f.startswith("_") and not f.upper().startswith("LEESMIJ"))


def importeer():
    """The folders -> assets/guhs/sounds/blubguh/<id>/<n>.ogg (mono, trimmed, normalised, normal pitch) + sounds.json."""
    import shutil
    sys.path.insert(0, os.path.join(ROOT, "tools"))
    from import_sound import trim_silence
    import numpy as np
    totaal = 0
    for a in ACTIES:
        doel = os.path.join(SOUND_DIR, a.id)
        if os.path.isdir(doel):
            shutil.rmtree(doel)
        for n, f in enumerate(_bestanden(os.path.join(BASE, a.id)), 1):
            x, rate = _lees(os.path.join(BASE, a.id, f))
            x = trim_silence(x, rate)
            fade = min(len(x) // 4, int(0.01 * rate))
            if fade > 0:
                x[:fade] *= np.linspace(0, 1, fade)
                x[-fade:] *= np.linspace(1, 0, fade)
            _schrijf(os.path.join(doel, f"{n}.ogg"), x, rate)
            totaal += 1
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        d = json.load(f)
    sound_events(d)
    with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"blub audio: {totaal} files in {len(ACTIES)} sound events")


def sound_events(d):
    """sounds.json: one event per action with the imported files (in place), old blubguh.* events dropped."""
    for k in [k for k in d if k.startswith("blubguh.")]:
        if k[len("blubguh."):] not in {a.id for a in ACTIES}:
            del d[k]
    for a in ACTIES:
        doel = os.path.join(SOUND_DIR, a.id)
        namen = sorted((f[:-4] for f in os.listdir(doel) if f.endswith(".ogg")), key=lambda s: (len(s), s)) if os.path.isdir(doel) else []
        sounds = []
        for n in namen:
            e = {"name": f"guhs:blubguh/{a.id}/{n}"}
            if a.codepitch != 1.0:
                e["pitch"] = round(1.0 / a.codepitch, 4)        # (shared code plays it at a fixed pitch: undo it)
            sounds.append(e)
        d[f"blubguh.{a.id}"] = {"sounds": sounds, "subtitle": f"subtitles.guhs.blubguh.{a.id}"}


LEESMIJ_KOP = """# Blub audio

Hier staan alle geluiden van de Blub-guh: **een map per actie**. Wat in een map staat, is precies wat je in het spel hoort.
Nu zijn het nog *placeholders* (piepjes van het pieppiepmuisje en Minecraft-geluiden, al op de juiste toonhoogte gezet).

## Spelregels
- **Weghalen en toevoegen mag.** Zet in een map zoveel geluidjes als je wilt: het spel kiest elke keer **willekeurig een** uit de map.
  Een lege map = geen geluid bij die actie.
- **Elk formaat** dat `tools/import_sound.py` aankan: WhatsApp-spraakberichten (.opus/.ogg), .mp3, .wav, .flac. Stereo wordt mono,
  stilte aan het begin en eind wordt weggeknipt, het volume wordt gelijkgetrokken.
- **Lever op gewone toonhoogte in.** Het spel maakt zelf een klein beetje variatie (zie de tabel), en babyblubjes klinken 1,3x hoger.
- Bestanden die met `_` beginnen worden **niet** gebruikt (voorbeelden, zoals `liedje/_melodie_zoals_in_spel.ogg`).
- Daarna in de guhs-map (of de worktree) draaien:

      python tools/blub_audio_import.py

  Dat zet de geluiden in de mod (`assets/guhs/sounds/blubguh/<map>/`) en vult `sounds.json`. Daarna de client opnieuw starten.

## Per map
| Map | Wanneer | Hoe vaak | Toonhoogte-variatie in het spel | Nu |
|---|---|---|---|---|
"""


def leesmij():
    regels = []
    for a in ACTIES:
        n = len(_bestanden(os.path.join(BASE, a.id)))
        extra = f" (het spel speelt dit op {str(a.codepitch).replace('.', ',')}x; sounds.json draait dat terug, dus lever gewoon in)" if a.codepitch != 1.0 else ""
        regels.append(f"| `{a.id}` | {a.wanneer}{extra} | {a.hoe_vaak} | {a.variatie} | {n} bestand(en), willekeurig gekozen |")
        map_ = os.path.join(BASE, a.id)
        os.makedirs(map_, exist_ok=True)
        txt = os.path.join(map_, "LEESMIJ.txt")
        if n == 0:
            with open(txt, "w", encoding="utf-8", newline="\n") as f:
                f.write(f"Nog geen geluid voor '{a.id}'.\nWanneer: {a.wanneer}\nZet hier een of meer geluidjes neer en draai python tools/blub_audio_import.py\n")
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
