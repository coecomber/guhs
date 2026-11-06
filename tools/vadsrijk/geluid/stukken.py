"""
T-GELUID: every piece of Vadsrijk music, described as data (vormen.gebied / vormen.baas) or written by hand (the stingers).

Entries of 'begeleiding' / 'vol' / 'intro':
  ("arp", instr, around_key, step, "op"|"neer"|"golf", vel, {opts})     broken chords (harp, music box, guitar...)
  ("pad", instr, around_key, vel, {opts n=})                             held chords (strings, choir, pads)
  ("bas", instr, around_key, "heel"|"kwint"|"wals"|"kwart", vel, {})     the bass line on the chord roots
  ("wals", instr, around_key, vel, {})                                   short chord plucks on the weak beats
  ("tegen", instr, around_key, vel, {})                                  a slow counter line (3rds/7ths)
  ("ostinato", instr, around_key, vel, {figuur=, stap=})                 a driving figure (bosses)
  ("slag", "slagwerk"|..., [(beat, key, vel), ...], {})                  percussion per bar
Melodies: (instr, lowest, highest, vel[, gain, reverb]).
"""
from componist import Stuk
from vormen import baas, fanfare, gebied

ZACHT = {"gain": 0.8, "galm": 0.45}


def _g(**k):
    d = dict(ZACHT)
    d.update(k)
    return d


# ------------------------------------------------------------------------------------------------------------- area themes
GEBIEDEN = [
    # ---- zone 1: Zwamzoet's lands
    dict(naam="klodderweide", toon="G", modus="majeur", bpm=100, maat=4, seed=101,
         A="1 6 4 5 | 1 6 4,5 1", B="4 1 6 5 | 4 1 2m,5 5",
         melodie=("fluit", 74, 88, 74), melodie2=("marimba", 67, 84, 80), melodieB=("fluit", 72, 86, 72),
         begeleiding=[("arp", "harp", 62, 0.5, "golf", 54, _g()), ("bas", "pizz", 43, "kwint", 62, _g(galm=0.3)),
                      ("pad", "strijkers", 60, 40, _g(gain=0.6))],
         vol=[("tegen", "klarinet", 62, 46, _g(gain=0.6))],
         intro=[("arp", "harp", 62, 0.5, "golf", 50, _g())], verdubbel=("glocken", 0, 0, 40, 0.35, 0.5)),
    dict(naam="sporenwoud", toon="F", modus="lydisch", bpm=80, maat=3, seed=102, rustig=True,
         A="1M7 2 1M7 2 | 6m 2 5s4,5 1M7", B="4 5 3m 6m | 4 5 2 2",
         melodie=("ocarina", 69, 86, 70), melodie2=("celesta", 72, 91, 72), melodieB=("vibrafoon", 67, 86, 70),
         begeleiding=[("arp", "speeldoos", 72, 0.5, "golf", 48, _g(gain=0.6)), ("pad", "glaspad", 60, 42, _g(gain=0.55)),
                      ("bas", "harp", 41, "wals", 56, _g())],
         intro=[("pad", "glaspad", 60, 40, _g(gain=0.55)), ("arp", "speeldoos", 72, 0.5, "golf", 44, _g(gain=0.6))]),
    dict(naam="druipgrotten", toon="A", modus="mineur", bpm=72, maat=4, seed=103, rustig=True, kort=True,
         A="1 6 3 7 | 1 6 4,5M 1", B="6 7 3 1 | 6 4 5M 5M",
         melodie=("kalimba", 67, 84, 74), melodie2=("marimba", 64, 81, 74), melodieB=("kalimba", 69, 86, 72),
         begeleiding=[("pad", "strijkers", 57, 38, _g(gain=0.55)), ("bas", "contrabas", 40, "heel", 50, _g(gain=0.7)),
                      ("arp", "druppels", 76, 1.0, "neer", 40, _g(gain=0.35, galm=0.6))],
         intro=[("pad", "strijkers", 57, 34, _g(gain=0.55))], echo=("kalimba", 0.75, 0.45, 12)),
    dict(naam="zwamtempel", toon="E", modus="dorisch", bpm=66, maat=4, seed=104, rustig=True, kort=True,
         A="1 4 1 7 | 1 4 7 1", B="3 7 4 1 | 3 7 4 5m",
         melodie=("panfluit", 67, 83, 72), melodie2=("celesta", 72, 88, 70), melodieB=("panfluit", 69, 86, 72),
         begeleiding=[("pad", "oohs", 59, 44, _g(gain=0.6, n=4)), ("arp", "harp", 59, 1.0, "op", 50, _g()),
                      ("bas", "cello", 40, "heel", 48, _g(gain=0.6))],
         vol=[("slag", "buisklok", [(0, 76, 34)], _g(gain=0.35, galm=0.7))],
         intro=[("pad", "oohs", 59, 40, _g(gain=0.6, n=4))]),
    # ---- zone 2: De Splinterpieken
    dict(naam="knuffelmeer", toon="C", modus="lydisch", bpm=84, maat=3, seed=201, rustig=True,
         A="1 2 1M7 2 | 3m 6m 2 1", B="6m 3m 4 2 | 6m 3m 5s4 5",
         melodie=("klarinet", 64, 81, 72), melodie2=("vibrafoon", 69, 86, 72), melodieB=("fluit", 72, 88, 70),
         begeleiding=[("arp", "harp", 62, 0.5, "golf", 52, _g()), ("pad", "strijkers", 60, 40, _g(gain=0.55)),
                      ("bas", "contrabas", 41, "wals", 52, _g(gain=0.6))],
         intro=[("arp", "harp", 62, 0.5, "golf", 48, _g())]),
    dict(naam="galmhellingen", toon="Bb", modus="majeur", bpm=92, maat=4, seed=202,
         A="1 5 6 4 | 1 5 4 1", B="6 3 4 1 | 2m 5 4 5",
         melodie=("blokfluit", 70, 86, 74), melodie2=("fluit", 72, 88, 74),
         begeleiding=[("arp", "glocken", 79, 1.0, "op", 38, _g(gain=0.5)), ("bas", "pizz", 46, "kwint", 58, _g()),
                      ("pad", "hoorn", 58, 38, _g(gain=0.55))],
         intro=[("pad", "hoorn", 58, 34, _g(gain=0.55))], echo=("fluit", 1.0, 0.5, 0)),
    dict(naam="splinterkloof", toon="D", modus="mineur", bpm=108, maat=3, samengesteld=True, seed=203,
         A="1 7 6 5M | 1 7 4,5M 1", B="6 3 7 1 | 4 1 5M 5M",
         melodie=("hobo", 67, 84, 72), melodie2=("viool", 69, 88, 72),
         begeleiding=[("ostinato", "pizz", 50, 50, _g(figuur=(0, 7, 12, 7, 3, 7), stap=0.5)), ("pad", "cello", 50, 40, _g(gain=0.55)),
                      ("arp", "harp", 62, 1.5, "op", 44, _g())],
         vol=[("slag", "pauk", [(0, 38, 40)], _g(gain=0.5))],
         intro=[("ostinato", "pizz", 50, 46, _g(figuur=(0, 7, 12, 7, 3, 7), stap=0.5))]),
    dict(naam="spiegelburcht", toon="E", modus="harm", bpm=76, maat=3, seed=204, rustig=True,
         A="1 4 5M 1 | 6 4 5M 1", B="6 3 4 1 | 6 2d 5M 5M",
         melodie=("celesta", 72, 88, 72), melodie2=("klavecimbel", 67, 84, 64), melodieB=("viool", 67, 86, 70),
         begeleiding=[("pad", "strijkers", 59, 40, _g(gain=0.55)), ("wals", "harp", 62, 46, _g()),
                      ("bas", "contrabas", 40, "wals", 48, _g(gain=0.6))],
         intro=[("pad", "glaspad", 59, 38, _g(gain=0.5))], echo=("glocken", 1.5, 0.35, 12)),
    # ---- zone 3: De Schemerwortel
    dict(naam="wortelvelden", toon="A", modus="mixo", bpm=88, maat=4, seed=301,
         A="1 7 4 1 | 1 7 4,5m 1", B="4 1 7 4 | 2m 4 5m 5m",
         melodie=("ocarina", 69, 86, 74), melodie2=("marimba", 64, 84, 78), melodieB=("fluit", 72, 88, 72),
         begeleiding=[("arp", "gitaar", 57, 0.5, "golf", 52, _g()), ("bas", "akbas", 40, "kwint", 56, _g(gain=0.7)),
                      ("pad", "warmpad", 60, 38, _g(gain=0.5))],
         vol=[("slag", "slagwerk", [(0, 70, 30), (1, 70, 22), (2, 70, 30), (3, 70, 22)], _g(gain=0.35, galm=0.25))],
         intro=[("arp", "gitaar", 57, 0.5, "golf", 48, _g())]),
    dict(naam="wortelduister", toon="C", modus="mineur", bpm=64, maat=4, seed=302, rustig=True, kort=True,
         A="1 6 1 7 | 1 6 4 1", B="6 7 1 1 | 4 6 5M 5M",
         melodie=("kalimba", 64, 81, 72), melodie2=("vibrafoon", 64, 81, 66), melodieB=("panfluit", 64, 81, 68),
         begeleiding=[("pad", "strijkers", 55, 36, _g(gain=0.5)), ("bas", "contrabas", 36, "heel", 48, _g(gain=0.6)),
                      ("arp", "druppels", 79, 2.0, "neer", 36, _g(gain=0.35, galm=0.7))],
         intro=[("pad", "strijkers", 55, 32, _g(gain=0.5))], echo=("kalimba", 1.0, 0.4, 12)),
    dict(naam="hart", toon="Eb", modus="majeur", bpm=60, maat=3, seed=303, rustig=True, kort=True,
         A="1 4 6 5 | 1 4 5 1", B="6 3 4 1 | 6 2m 5s4 5",
         melodie=("celesta", 70, 87, 70), melodie2=("harp", 67, 84, 70), melodieB=("oohs", 63, 79, 62),
         begeleiding=[("pad", "koor", 58, 40, _g(gain=0.55, n=4)), ("arp", "harp", 58, 0.5, "golf", 46, _g()),
                      ("bas", "cello", 39, "heel", 44, _g(gain=0.6))],
         intro=[("pad", "koor", 58, 36, _g(gain=0.55, n=4))]),
    dict(naam="schorsstad", toon="F", modus="majeur", bpm=104, maat=4, seed=304,
         A="1 4 5 1 | 6 4 5 1", B="4 5 3m 6 | 2m 5 1 5",
         melodie=("hakkebord", 69, 86, 76), melodie2=("klarinet", 65, 82, 74), melodieB=("accordeon", 65, 81, 62),
         begeleiding=[("bas", "fagot", 41, "kwart", 56, _g(gain=0.7)), ("wals", "pizz", 60, 46, _g()),
                      ("pad", "strijkers", 62, 34, _g(gain=0.45))],
         vol=[("slag", "slagwerk", [(0, 76, 30), (2, 77, 26)], _g(gain=0.35, galm=0.2))],
         intro=[("bas", "fagot", 41, "kwart", 52, _g(gain=0.7))]),
    dict(naam="stamspiraal", toon="G", modus="mixo", bpm=92, maat=4, seed=305,
         A="1 7 1 4 | 1 7 4 1", B="4 5m 7 1 | 4 5m 7 7",
         melodie=("panfluit", 67, 86, 74), melodie2=("harp", 67, 86, 72), melodieB=("fluit", 72, 88, 72),
         begeleiding=[("arp", "harp", 60, 0.5, "op", 50, _g()), ("bas", "pizz", 43, "kwint", 56, _g()),
                      ("pad", "warmpad", 60, 36, _g(gain=0.5))],
         intro=[("arp", "harp", 60, 0.5, "op", 46, _g())]),
    dict(naam="takkenbanen", toon="D", modus="majeur", bpm=110, maat=3, samengesteld=True, seed=306,
         A="1 4 1 5 | 1 4 5 1", B="6 4 1 5 | 6 4 2m 5",
         melodie=("fluit", 74, 91, 74), melodie2=("piccolo", 79, 93, 64), melodieB=("fluit", 72, 88, 72),
         begeleiding=[("arp", "harp", 62, 0.5, "golf", 50, _g()), ("bas", "pizz", 50, "wals", 54, _g()),
                      ("arp", "glocken", 84, 1.5, "op", 34, _g(gain=0.45))],
         intro=[("arp", "harp", 62, 0.5, "golf", 46, _g())]),
    dict(naam="kruinzee", toon="E", modus="majeur", bpm=78, maat=3, seed=307, rustig=True,
         A="1 3m 4 1 | 6 4 5 1", B="4 5 3m 6 | 4 1 2m 5",
         melodie=("viool", 71, 88, 70), melodie2=("celesta", 72, 91, 70), melodieB=("fluit", 71, 88, 70),
         begeleiding=[("pad", "strijkers", 61, 44, _g(gain=0.6)), ("arp", "harp", 61, 0.5, "golf", 48, _g()),
                      ("bas", "contrabas", 40, "wals", 50, _g(gain=0.6))],
         intro=[("pad", "strijkers", 61, 40, _g(gain=0.6))]),
    dict(naam="zeppelinwerf", toon="Bb", modus="majeur", bpm=112, maat=4, seed=308,
         A="1 6 2m 5 | 1 6 2m,5 1", B="4 1 4 5 | 3m 6 2 5",
         melodie=("klarinet", 65, 84, 76), melodie2=("xylofoon", 72, 89, 66), melodieB=("fagot", 50, 67, 72),
         begeleiding=[("bas", "tuba", 34, "kwint", 56, _g(gain=0.7)), ("wals", "pizz", 58, 46, _g()),
                      ("arp", "glocken", 82, 1.0, "op", 30, _g(gain=0.4))],
         vol=[("slag", "slagwerk", [(0, 75, 28), (1, 76, 24), (2, 75, 28), (3, 76, 24)], _g(gain=0.35, galm=0.2))],
         intro=[("bas", "tuba", 34, "kwint", 52, _g(gain=0.7))]),
    # ---- zone 4: De Lampionkust (+ the leitmotif)
    dict(naam="lampionkust_dag", toon="D", modus="majeur", bpm=96, maat=4, seed=401, leidmotief="B",
         A="1 4 1 5 | 1 4 5 1", B="",
         melodie=("steeldrum", 69, 86, 70), melodie2=("ukelele", 67, 84, 74), melodieB=("fluit", 72, 88, 74),
         begeleiding=[("arp", "ukelele", 60, 0.5, "golf", 50, _g()), ("bas", "akbas", 38, "kwint", 54, _g(gain=0.7)),
                      ("pad", "strijkers", 62, 34, _g(gain=0.45))],
         vol=[("slag", "slagwerk", [(0, 70, 28), (0.5, 70, 18), (1, 70, 26), (1.5, 70, 18), (2, 70, 28), (2.5, 70, 18), (3, 70, 26),
                                    (3.5, 70, 18)], _g(gain=0.3, galm=0.2))],
         intro=[("arp", "ukelele", 60, 0.5, "golf", 46, _g())]),
    dict(naam="lampionkust_avond", toon="A", modus="majeur", bpm=72, maat=4, seed=402, rustig=True, leidmotief="A",
         A="", B="6 3m 4 1 | 6 4 2m 5",
         melodie=("speeldoos", 72, 88, 70), melodie2=("vibrafoon", 69, 86, 70), melodieB=("celesta", 72, 88, 70),
         begeleiding=[("arp", "harp", 61, 0.5, "golf", 46, _g()), ("pad", "strijkers", 61, 38, _g(gain=0.55)),
                      ("bas", "contrabas", 40, "heel", 46, _g(gain=0.6))],
         intro=[("arp", "harp", 61, 0.5, "golf", 42, _g())]),
    # ---- zone 5: Hoogvads
    dict(naam="hoogvads", toon="C", modus="majeur", bpm=84, maat=4, seed=501, leidmotief="A",
         A="", B="6 3m 4 1 | 2m 5 1 5",
         melodie=("hoorn", 58, 74, 74, 1.0), melodie2=("trompet", 64, 79, 66, 0.8), melodieB=("viool", 69, 86, 72),
         begeleiding=[("pad", "strijkers", 60, 46, _g(gain=0.65)), ("arp", "harp", 62, 0.5, "op", 46, _g()),
                      ("bas", "contrabas", 36, "kwint", 54, _g(gain=0.65))],
         vol=[("pad", "koor", 64, 34, _g(gain=0.4)), ("slag", "pauk", [(0, 36, 36)], _g(gain=0.45))],
         intro=[("pad", "strijkers", 60, 40, _g(gain=0.65)), ("slag", "buisklok", [(0, 72, 34)], _g(gain=0.35, galm=0.7))]),
    dict(naam="hoogvads_markt", toon="F", modus="majeur", bpm=108, maat=3, seed=502,
         A="1 4 5 1 | 1 4 5 1", B="6 2m 5 1 | 6 2m 5 5",
         melodie=("fluit", 72, 89, 76), melodie2=("accordeon", 65, 81, 62), melodieB=("glocken", 79, 93, 54),
         begeleiding=[("bas", "pizz", 41, "wals", 56, _g()), ("wals", "pizz", 60, 46, _g()),
                      ("arp", "harp", 65, 0.5, "golf", 40, _g())],
         vol=[("slag", "slagwerk", [(0, 81, 22)], _g(gain=0.3, galm=0.4))],
         intro=[("bas", "pizz", 41, "wals", 52, _g())]),
]

# ------------------------------------------------------------------------------------------------------------- boss themes
DRUM_ZWAAR = [(0, 36, 70), (1.5, 36, 56), (2, 38, 50), (3, 36, 60), (3.5, 38, 40)]
TAIKO = [(0, 45, 76), (1, 45, 50), (2, 45, 70), (2.5, 45, 48), (3, 50, 60)]
TAIKO3 = [(0, 45, 76), (1, 45, 48), (1.5, 45, 56), (2, 50, 60)]
BAZEN = [
    # zone 1
    dict(naam="klodderreus", toon="C", modus="mineur", bpm=132, maat=4, seed=1101,
         A="1 1 6 7 | 1 6 4 5M", B="6 7 3 1 | 6 4 5M 5M",
         melodie=("tuba", 48, 64, 88, 1.0, 0.3), melodie2=("koper", 60, 76, 86),
         begeleiding=[("ostinato", "snelstrijk", 48, 60, _g(gain=0.8, figuur=(0, 0, 12, 0, 7, 0, 12, 7))),
                      ("slag", "taiko", TAIKO, _g(gain=0.9, galm=0.3)), ("bas", "contrabas", 36, "kwint", 66, _g(gain=0.8))],
         vol=[("pad", "koor", 60, 48, _g(gain=0.55)), ("arp", "xylofoon", 79, 0.5, "op", 46, _g(gain=0.45))],
         verdubbel=("trombone", 0, 0, 80, 0.8, 0.3)),
    dict(naam="koning_klodder", toon="G", modus="mineur", bpm=116, maat=4, seed=1102,
         A="1 4 5M 1 | 1 4 5M 1", B="6 7 3 6 | 4 1 5M 5M",
         melodie=("trompet", 64, 81, 90), melodie2=("snelstrijk", 67, 86, 84),
         begeleiding=[("bas", "tuba", 43, "kwint", 66, _g(gain=0.8)),
                      ("slag", "slagwerk", [(0, 36, 66), (1, 38, 52), (1.5, 38, 40), (2, 36, 60), (3, 38, 56), (3.75, 38, 40)], _g(gain=0.7, galm=0.25)),
                      ("pad", "koper", 58, 50, _g(gain=0.6))],
         vol=[("slag", "pauk", [(0, 43, 60), (2, 38, 52)], _g(gain=0.6)), ("tegen", "hoorn", 60, 56, _g(gain=0.6))],
         verdubbel=("glocken", 12, 0, 46, 0.45, 0.4)),
    dict(naam="opperzwam", toon="D", modus="dorisch", bpm=114, maat=3, samengesteld=True, seed=1103,
         A="1 7 1 4 | 1 7 4 5m", B="3 4 1 7 | 3 4 5m 5m",
         melodie=("hobo", 67, 84, 84), melodie2=("koor", 62, 79, 74),
         begeleiding=[("ostinato", "pizz", 50, 60, _g(figuur=(0, 7, 12, 7, 0, 7), stap=0.5)), ("pad", "oohs", 62, 50, _g(gain=0.6, n=4)),
                      ("slag", "taiko", TAIKO3, _g(gain=0.8))],
         vol=[("arp", "celesta", 74, 0.5, "golf", 46, _g(gain=0.5)), ("bas", "contrabas", 38, "heel", 60, _g(gain=0.8))],
         verdubbel=("fluit", 12, 0, 60, 0.6, 0.4)),
    dict(naam="mika_spion", toon="E", modus="harm", bpm=138, maat=4, seed=1104,
         A="1 1 6 6 | 1 1 4 5M", B="4 4 1 1 | 6 2d 5M 5M",
         melodie=("klarinet", 64, 81, 84), melodie2=("viool", 67, 86, 82),
         begeleiding=[("ostinato", "pizz", 40, 62, _g(figuur=(0, 12, 7, 12, 0, 12, 6, 7), stap=0.5)),
                      ("slag", "slagwerk", [(0, 36, 60), (0.5, 42, 36), (1, 38, 46), (1.5, 42, 36), (2, 36, 54), (2.5, 42, 36), (3, 38, 46),
                                            (3.5, 42, 36)], _g(gain=0.6, galm=0.2)),
                      ("bas", "contrabas", 40, "kwart", 60, _g(gain=0.8))],
         vol=[("pad", "tremolo", 64, 46, _g(gain=0.6)), ("slag", "pauk", [(0, 40, 50)], _g(gain=0.5))],
         verdubbel=("xylofoon", 12, 0, 48, 0.45, 0.3)),
    # zone 2
    dict(naam="boormeester", toon="A", modus="mineur", bpm=144, maat=4, seed=1201,
         A="1 1 7 7 | 6 6 5M 5M", B="1 6 3 7 | 1 6 5M 5M",
         melodie=("koper", 60, 77, 92), melodie2=("snelstrijk", 69, 88, 86),
         begeleiding=[("ostinato", "contrabas", 40, 70, _g(gain=0.8, figuur=(0, 0, 0, 12, 0, 0, 7, 10))),
                      ("slag", "slagwerk", [(0, 36, 70), (1, 38, 56), (2, 36, 66), (2.5, 36, 50), (3, 38, 60), (0, 56, 36), (2, 56, 36)], _g(gain=0.7, galm=0.2)),
                      ("pad", "hoorn", 57, 50, _g(gain=0.6))],
         vol=[("slag", "taiko", TAIKO, _g(gain=0.7)), ("arp", "xylofoon", 81, 0.25, "op", 40, _g(gain=0.35))],
         verdubbel=("trombone", -12, 0, 70, 0.7, 0.3)),
    dict(naam="galmwolf", toon="F", modus="mineur", bpm=126, maat=4, seed=1202,
         A="1 6 7 1 | 1 6 4 5M", B="6 3 7 1 | 6 3 5M 5M",
         melodie=("hoornsolo", 60, 76, 90), melodie2=("koor", 63, 79, 80),
         begeleiding=[("ostinato", "snelstrijk", 53, 60, _g(gain=0.8, figuur=(0, 7, 12, 7, 0, 7, 12, 15))),
                      ("slag", "taiko", TAIKO, _g(gain=0.85)), ("bas", "contrabas", 41, "kwint", 64, _g(gain=0.8))],
         vol=[("pad", "oohs", 60, 50, _g(gain=0.55)), ("slag", "pauk", [(0, 41, 56), (3.5, 36, 50)], _g(gain=0.6))],
         verdubbel=("hoorn", -12, 0, 60, 0.6, 0.6)),
    dict(naam="spiegelzucht", toon="B", modus="harm", bpm=150, maat=3, seed=1203,
         A="1 4 5M 1 | 6 4 5M 1", B="6 2d 5M 1 | 6 2d 5M 5M",
         melodie=("viool", 66, 86, 86), melodie2=("celesta", 71, 90, 80),
         begeleiding=[("bas", "contrabas", 40, "wals", 62, _g(gain=0.8)), ("wals", "snelstrijk", 62, 52, _g(gain=0.6)),
                      ("pad", "glaspad", 62, 44, _g(gain=0.5))],
         vol=[("arp", "harp", 66, 0.5, "golf", 48, _g()), ("slag", "pauk", [(0, 35, 50)], _g(gain=0.55))],
         verdubbel=("glocken", 12, 0, 44, 0.45, 0.6)),
    # zone 3
    dict(naam="baron_bleu", toon="C#", modus="harm", bpm=128, maat=3, seed=1301,
         A="1 4 5M 1 | 1 6 5M 1", B="6 3 4 1 | 2d 6 5M 5M",
         melodie=("klavecimbel", 68, 85, 82), melodie2=("trompet", 64, 80, 84),
         begeleiding=[("bas", "contrabas", 37, "wals", 62, _g(gain=0.8)), ("wals", "pizz", 61, 52, _g()),
                      ("pad", "koper", 61, 48, _g(gain=0.55))],
         vol=[("slag", "pauk", [(0, 37, 56), (2, 32, 46)], _g(gain=0.6)), ("tegen", "hoorn", 61, 54, _g(gain=0.6))],
         verdubbel=("glocken", 12, 0, 40, 0.4, 0.4)),
    dict(naam="nestwachter", toon="D", modus="mineur", bpm=120, maat=4, seed=1302,
         A="1 7 6 7 | 1 7 4 5M", B="6 7 1 1 | 6 7 5M 5M",
         melodie=("hoorn", 58, 74, 88), melodie2=("snelstrijk", 69, 86, 84),
         begeleiding=[("slag", "taiko", TAIKO, _g(gain=0.9)), ("ostinato", "cello", 50, 60, _g(gain=0.7, figuur=(0, 7, 0, 10, 0, 7, 12, 7))),
                      ("bas", "contrabas", 38, "heel", 62, _g(gain=0.8))],
         vol=[("pad", "koor", 62, 50, _g(gain=0.55)), ("arp", "harp", 62, 0.5, "op", 46, _g())],
         verdubbel=("panfluit", 12, 0, 56, 0.5, 0.45)),
    dict(naam="wortelwurger", toon="Bb", modus="mineur", bpm=100, maat=4, seed=1303,
         A="1 1 6 6 | 4 4 5M 5M", B="6 7 1 3 | 6 4 5M 5M",
         melodie=("cello", 50, 67, 90), melodie2=("koper", 58, 74, 86),
         begeleiding=[("ostinato", "contrabas", 34, 66, _g(gain=0.85, figuur=(0, 0, 1, 0, 0, 0, 1, 3))),
                      ("slag", "taiko", [(0, 41, 80), (1.5, 41, 56), (2, 45, 66), (3, 41, 60)], _g(gain=0.9)),
                      ("pad", "tremolo", 58, 46, _g(gain=0.55))],
         vol=[("pad", "koor", 58, 48, _g(gain=0.5)), ("slag", "pauk", [(0, 34, 60)], _g(gain=0.6))],
         verdubbel=("trombone", 0, 0, 70, 0.7, 0.3)),
    dict(naam="piepvel", toon="D", modus="harm", bpm=132, maat=3, seed=1304,
         A="1 4 5M 1 | 6 4 5M 1", B="6 3 4 5M | 6 2d 5M 5M",
         melodie=("orgel", 62, 79, 74, 0.7, 0.5), melodie2=("klavecimbel", 69, 86, 80),
         begeleiding=[("bas", "contrabas", 38, "wals", 62, _g(gain=0.8)), ("wals", "snelstrijk", 62, 52, _g(gain=0.6)),
                      ("slag", "pauk", [(0, 38, 54)], _g(gain=0.6))],
         vol=[("pad", "koor", 62, 46, _g(gain=0.5)), ("arp", "celesta", 74, 0.5, "golf", 44, _g(gain=0.5))],
         verdubbel=("glocken", 12, 0, 40, 0.4, 0.5)),
    dict(naam="kruinroofvogel", toon="E", modus="mineur", bpm=138, maat=3, samengesteld=True, seed=1305,
         A="1 7 6 7 | 1 7 4 5M", B="6 7 3 1 | 4 6 5M 5M",
         melodie=("snelstrijk", 71, 88, 88), melodie2=("hoorn", 59, 76, 88),
         begeleiding=[("ostinato", "snelstrijk", 52, 58, _g(gain=0.7, figuur=(0, 7, 12, 7, 12, 7), stap=0.5)),
                      ("slag", "taiko", TAIKO3, _g(gain=0.85)), ("bas", "contrabas", 40, "heel", 62, _g(gain=0.8))],
         vol=[("arp", "harp", 64, 0.5, "golf", 48, _g()), ("pad", "koor", 64, 46, _g(gain=0.5))],
         verdubbel=("piccolo", 12, 0, 50, 0.5, 0.4)),
    dict(naam="vahoegnir", toon="D", modus="mineur", bpm=112, maat=4, seed=1306,
         A="1 6 3 7 | 1 6 4 5M", B="6 7 1 3 | 4 6 5M 5M", brug="1 6 7 5M",
         melodie=("koper", 60, 77, 92), melodie2=("koor", 62, 79, 84),
         begeleiding=[("ostinato", "snelstrijk", 50, 62, _g(gain=0.8, figuur=(0, 0, 7, 0, 12, 0, 7, 10))),
                      ("slag", "taiko", TAIKO, _g(gain=0.9)), ("bas", "contrabas", 38, "kwint", 66, _g(gain=0.85)),
                      ("slag", "pauk", [(0, 38, 60), (3.5, 33, 50)], _g(gain=0.6))],
         vol=[("pad", "koor", 62, 54, _g(gain=0.6, n=4)), ("tegen", "hoorn", 62, 60, _g(gain=0.65)),
              ("slag", "slagwerk", [(0, 49, 40)], _g(gain=0.4, galm=0.5))],
         verdubbel=("trompet", 12, 0, 62, 0.55, 0.35)),
    dict(naam="brokkel", toon="G", modus="mineur", bpm=140, maat=4, seed=1307,
         A="1 7 6 5M | 1 7 4 5M", B="3 7 1 6 | 4 1 5M 5M",
         melodie=("xylofoon", 67, 86, 84), melodie2=("trompet", 64, 81, 84),
         begeleiding=[("bas", "tuba", 43, "kwart", 64, _g(gain=0.8)),
                      ("slag", "slagwerk", [(0, 36, 70), (1, 38, 56), (2, 36, 62), (3, 38, 58), (3.5, 38, 44)], _g(gain=0.7, galm=0.2)),
                      ("ostinato", "pizz", 55, 54, _g(figuur=(0, 12, 7, 12)))],
         vol=[("pad", "koper", 58, 50, _g(gain=0.55)), ("slag", "taiko", TAIKO, _g(gain=0.6))],
         verdubbel=("pizz", 12, 0, 50, 0.6, 0.3)),
    dict(naam="roquefort", toon="A", modus="harm", bpm=124, maat=3, samengesteld=True, seed=1308,
         A="1 4 5M 1 | 1 6 5M 1", B="6 7 3 6 | 4 2d 5M 5M",
         melodie=("trompet", 64, 81, 88), melodie2=("snelstrijk", 69, 88, 84),
         begeleiding=[("bas", "tuba", 45, "wals", 62, _g(gain=0.8)), ("ostinato", "pizz", 57, 54, _g(figuur=(0, 7, 12, 7, 0, 7))),
                      ("slag", "slagwerk", [(0, 36, 66), (1.5, 38, 54), (2.5, 38, 40)], _g(gain=0.65, galm=0.25))],
         vol=[("pad", "koper", 57, 50, _g(gain=0.55)), ("slag", "pauk", [(0, 45, 56)], _g(gain=0.6))],
         verdubbel=("glocken", 12, 0, 40, 0.4, 0.4)),
    dict(naam="taleggio", toon="F", modus="mineur", bpm=132, maat=4, seed=1309,
         A="1 6 7 1 | 1 6 4 5M", B="6 3 7 1 | 4 1 5M 5M",
         melodie=("hoorn", 60, 77, 88), melodie2=("klarinet", 65, 82, 82),
         begeleiding=[("ostinato", "snelstrijk", 53, 58, _g(gain=0.75, figuur=(0, 0, 7, 0, 10, 0, 7, 0))),
                      ("slag", "slagwerk", [(0, 36, 66), (1, 38, 50), (2, 36, 60), (3, 38, 54)], _g(gain=0.65, galm=0.25)),
                      ("bas", "contrabas", 41, "kwint", 62, _g(gain=0.8))],
         vol=[("arp", "marimba", 72, 0.5, "op", 46, _g(gain=0.5)), ("slag", "taiko", TAIKO, _g(gain=0.6))],
         verdubbel=("trombone", -12, 0, 60, 0.6, 0.3)),
    # zone 4
    dict(naam="zeeslurf", toon="G", modus="mineur", bpm=96, maat=3, samengesteld=True, seed=1401,
         A="1 6 7 1 | 1 6 4 5M", B="3 7 6 3 | 4 1 5M 5M",
         melodie=("tuba", 46, 62, 90, 1.0, 0.35), melodie2=("koor", 62, 79, 82),
         begeleiding=[("bas", "contrabas", 38, "heel", 64, _g(gain=0.85)), ("arp", "harp", 60, 0.5, "golf", 50, _g()),
                      ("slag", "taiko", [(0, 43, 76), (1.5, 43, 60), (2.5, 45, 48)], _g(gain=0.85)),
                      ("pad", "tremolo", 58, 46, _g(gain=0.55))],
         vol=[("pad", "koor", 60, 50, _g(gain=0.55)), ("tegen", "hoorn", 60, 56, _g(gain=0.6))],
         verdubbel=("cello", 12, 0, 70, 0.7, 0.4)),
    dict(naam="vliegervanger", toon="A", modus="mineur", bpm=152, maat=4, seed=1402,
         A="1 7 6 5M | 1 7 4 5M", B="6 7 1 1 | 6 4 5M 5M",
         melodie=("fluit", 72, 89, 88), melodie2=("snelstrijk", 69, 88, 86),
         begeleiding=[("ostinato", "snelstrijk", 57, 58, _g(gain=0.75, figuur=(0, 7, 12, 7, 0, 7, 12, 14), stap=0.25)),
                      ("slag", "slagwerk", [(0, 36, 66), (1, 38, 52), (2, 36, 60), (2.5, 36, 46), (3, 38, 56), (3.75, 38, 40)], _g(gain=0.65, galm=0.2)),
                      ("bas", "contrabas", 40, "kwart", 62, _g(gain=0.8))],
         vol=[("pad", "koper", 60, 50, _g(gain=0.55)), ("slag", "taiko", TAIKO, _g(gain=0.6))],
         verdubbel=("piccolo", 12, 0, 52, 0.5, 0.35)),
    # ---- U-AUDIO2: the bosses that had no own theme yet
    # zone 3, secret: De Mika-imker (honey, bees, a sneaky beekeeper): a buzzing 16th string ostinato, bassoon + clarinet
    dict(naam="mika_imker", toon="F", modus="harm", bpm=134, maat=4, seed=1310,
         A="1 1 6 6 | 4 4 5M 5M", B="6 7 3 6 | 4 2d 5M 5M",
         melodie=("fagot", 50, 67, 90, 1.0, 0.3), melodie2=("klarinet", 65, 82, 84),
         begeleiding=[("ostinato", "snelstrijk", 65, 52, _g(gain=0.6, figuur=(0, 1, 0, 1, 0, 1, 0, -1), stap=0.25)),
                      ("slag", "slagwerk", [(0, 36, 62), (1, 38, 48), (1.75, 36, 44), (2, 36, 58), (3, 38, 54), (3.5, 76, 40)], _g(gain=0.6, galm=0.2)),
                      ("bas", "contrabas", 41, "kwart", 62, _g(gain=0.8))],
         vol=[("arp", "marimba", 77, 0.5, "neer", 44, _g(gain=0.45)), ("wals", "pizz", 60, 50, _g(gain=0.6))],
         verdubbel=("xylofoon", 12, 0, 48, 0.45, 0.3),
         fase_dubbel=("klarinet", 12, 0, 60, 0.55, 0.35)),
    # zone 5 finale: De Grauwe Droom: a dreamy LULLABY boss (a music box waltz that went grey), its last phase a dark choir
    dict(naam="grauwe_droom", toon="D", modus="harm", bpm=84, maat=3, seed=1501,
         A="1 6 4 5M | 1 6 2d 5M", B="6 3 4 1 | 6 4 5M 5M",
         melodie=("speeldoos", 72, 88, 84, 1.0, 0.55), melodie2=("oohs", 62, 79, 72, 0.8, 0.6),
         begeleiding=[("bas", "contrabas", 38, "wals", 54, _g(gain=0.7)), ("wals", "harp", 62, 48, _g()),
                      ("pad", "glaspad", 62, 42, _g(gain=0.5)), ("slag", "pauk", [(0, 38, 44)], _g(gain=0.5, galm=0.6))],
         vol=[("arp", "celesta", 74, 0.5, "golf", 42, _g(gain=0.5)), ("tegen", "cello", 55, 50, _g(gain=0.6))],
         verdubbel=("glocken", 12, 0, 40, 0.4, 0.6),
         fase=[("pad", "koor", 57, 54, _g(gain=0.6, n=4)), ("slag", "taiko", [(0, 38, 70), (2, 38, 52)], _g(gain=0.7, galm=0.5)),
               ("pad", "tremolo", 50, 50, _g(gain=0.5)), ("ostinato", "contrabas", 38, 56, _g(gain=0.6, figuur=(0, 0, 1), stap=1.0)),
               ("slag", "pauk", [(0, 38, 60), (2, 33, 50), (2.5, 33, 44)], _g(gain=0.55))],
         fase_dubbel=("halopad", -12, 0, 56, 0.5, 0.6),
         fanfare=dict(lead="speeldoos", koper="hoorn", glans="celesta", zacht=True, figuur=1, bpm=84)),
    # zone 5: Donderbuik, de Grauwe Onweerswolk (grumpy, heavy, raindrops): tuba + horn, taiko thunder, harp/kalimba drops
    dict(naam="donderbuik", toon="C", modus="mineur", bpm=108, maat=4, seed=1502,
         A="1 1 6 7 | 1 1 4 5M", B="6 3 7 1 | 6 4 5M 5M",
         melodie=("tuba", 46, 62, 92, 1.0, 0.35), melodie2=("hoorn", 58, 74, 88),
         begeleiding=[("ostinato", "contrabas", 36, 64, _g(gain=0.85, figuur=(0, 0, 0, 3, 0, 0, 7, 6))),
                      ("slag", "taiko", [(0, 43, 80), (0.75, 43, 50), (1.5, 43, 64), (3, 45, 70), (3.5, 43, 56)], _g(gain=0.9, galm=0.45)),
                      ("pad", "tremolo", 55, 46, _g(gain=0.55))],
         vol=[("arp", "kalimba", 79, 0.25, "golf", 40, _g(gain=0.4)), ("pad", "koor", 60, 48, _g(gain=0.5)),
              ("slag", "pauk", [(0, 36, 60), (3.5, 31, 50)], _g(gain=0.6))],
         verdubbel=("trombone", 0, 0, 70, 0.7, 0.3)),
    # zone 5: Opzichter Gorgonzola (Ketelkamer, a big wrench, valves): a grumpy-comic boiler-room polka, bassoon + trombone
    dict(naam="gorgonzola", toon="Bb", modus="mineur", bpm=152, maat=4, seed=1503,
         A="1 5M 1 5M | 1 4 5M 1", B="4 1 5M 1 | 6 4 5M 5M",
         melodie=("fagot", 46, 63, 92, 1.0, 0.3), melodie2=("trombone", 53, 70, 88),
         begeleiding=[("bas", "tuba", 34, "kwart", 66, _g(gain=0.85)),
                      ("slag", "slagwerk", [(0, 36, 66), (1, 76, 50), (2, 36, 60), (3, 77, 50), (3.5, 81, 36)], _g(gain=0.65, galm=0.2)),
                      ("wals", "pizz", 58, 54, _g(gain=0.7))],
         vol=[("arp", "xylofoon", 77, 0.5, "op", 42, _g(gain=0.4)), ("pad", "accordeon", 58, 46, _g(gain=0.45))],
         verdubbel=("piccolo", 24, 0, 46, 0.45, 0.3)),
    # zone 5: Kootje Zilverstaart (the spy finale of "Verraad in de stad"): a sly tango, violin + accordion, staccato strings
    dict(naam="zilverstaart", toon="A", modus="harm", bpm=118, maat=4, seed=1504,
         A="1 1 4 4 | 6 2d 5M 5M", B="4 4 1 1 | 6 2d 5M 1",
         melodie=("viool", 67, 88, 90), melodie2=("accordeon", 64, 81, 86),
         begeleiding=[("ostinato", "pizz", 45, 62, _g(figuur=(0, 0, 7, 0, 12, 0, 7, 0), stap=0.5)),
                      ("slag", "slagwerk", [(0, 36, 66), (1.5, 36, 50), (2, 38, 60), (3, 36, 54), (3.5, 38, 44)], _g(gain=0.6, galm=0.2)),
                      ("bas", "contrabas", 45, "kwart", 62, _g(gain=0.8))],
         vol=[("arp", "klavecimbel", 72, 0.5, "golf", 46, _g(gain=0.45)), ("pad", "koper", 57, 48, _g(gain=0.5))],
         verdubbel=("glocken", 12, 0, 42, 0.4, 0.4)),
    # fallback for any boss without its own theme (new bosses from later slices)
    dict(naam="algemeen", toon="E", modus="mineur", bpm=126, maat=4, seed=1001,
         A="1 6 7 1 | 1 6 4 5M", B="6 7 3 6 | 4 1 5M 5M",
         melodie=("koper", 60, 77, 88), melodie2=("snelstrijk", 67, 86, 84),
         begeleiding=[("ostinato", "snelstrijk", 52, 58, _g(gain=0.75)), ("slag", "taiko", TAIKO, _g(gain=0.85)),
                      ("bas", "contrabas", 40, "kwint", 62, _g(gain=0.8))],
         vol=[("pad", "koor", 64, 48, _g(gain=0.5))]),
]


# ------------------------------------------------------------------------------------------------------------- stingers
def _stinger(naam, toon="C", bpm=96, seed=1):
    return Stuk(naam, toon, "majeur", bpm, 4, seed=seed)


def stinger_ontdekking():
    """A new place: a harp glissando up, then the first bar of the leitmotif on celesta + glockenspiel, over a soft I - IV - I."""
    st = _stinger("ontdekking", "D", 88)
    st.zet_akkoorden(0, "1 4,1")
    st.spoor("harp", "harp", 1.0, 0.5)
    k = 62
    for i in range(12):
        st.noot("harp", i * 0.0833, 1.5, k, 52 + i * 2)
        k = st.stap(k, 1)
    st.spoor("cel", "celesta", 1.0, 0.5)
    st.spoor("glo", "glocken", 0.5, 0.6)
    for bar, beat, dur, g in st.LEIDMOTIEF[:5]:
        kk = 74 + st.schaal[g % 7] + 12 * (g // 7)
        st.noot("cel", 1 + beat, dur * 1.1, kk, 76)
        st.noot("glo", 1 + beat, dur, kk + 12, 50)
    st.noot("cel", 5, 3, 86, 70)
    st.spoor("pad", "strijkers", 0.7, 0.5)
    for kk in (62, 66, 69):
        st.noot("pad", 0, 3, kk, 46)
    for kk in (62, 67, 71):
        st.noot("pad", 3, 1, kk, 44)
    for kk in (62, 66, 69, 74):
        st.noot("pad", 4, 4, kk, 46)
    return st


def stinger_quest_klaar():
    """A quest done: 'da-da-da DAAA' on music box + pizzicato, a little glockenspiel run, I - V - I."""
    st = _stinger("quest_klaar", "F", 120)
    st.spoor("doos", "speeldoos", 1.0, 0.45)
    st.spoor("pizz", "pizz", 0.8, 0.35)
    st.spoor("glo", "glocken", 0.5, 0.5)
    st.spoor("str", "strijkers", 0.7, 0.45)
    for t, kk in ((0, 77), (0.5, 77), (1, 77), (1.5, 81)):
        st.noot("doos", t, 0.45, kk, 82)
    st.noot("doos", 2, 0.5, 79, 80)
    st.noot("doos", 2.5, 0.5, 76, 78)
    st.noot("doos", 3, 3, 77, 86)
    st.noot("doos", 3, 3, 84, 70)
    for t, kk in ((0, 41), (1.5, 48), (3, 41)):
        st.noot("pizz", t, 0.5, kk, 70)
    for i, kk in enumerate((84, 88, 91, 96)):
        st.noot("glo", 3 + i * 0.125, 1.2, kk, 46)
    for kk in (60, 65, 69):
        st.noot("str", 3, 3.5, kk, 50)
    return st


def stinger_level_op():
    """A level up: a rising marimba/harp arpeggio, then a warm brass + strings chord with a timpani roll and bells."""
    st = _stinger("level_op", "Bb", 132)
    st.spoor("mar", "marimba", 1.0, 0.4)
    st.spoor("harp", "harp", 0.8, 0.5)
    for i, kk in enumerate((58, 62, 65, 70, 74, 77, 82)):
        st.noot("mar", i * 0.25, 0.5, kk, 64 + i * 3)
        st.noot("harp", i * 0.25, 1.2, kk - 12, 50 + i * 2)
    st.spoor("koper", "koper", 0.8, 0.45)
    st.spoor("str", "strijkers", 0.8, 0.45)
    st.spoor("pauk", "pauk", 0.7, 0.4)
    st.spoor("glo", "glocken", 0.55, 0.6)
    for kk in (58, 62, 65, 70):
        st.noot("koper", 2, 3.5, kk, 74)
    for kk in (65, 70, 74, 77):
        st.noot("str", 2, 4, kk, 60)
    for i in range(8):
        st.noot("pauk", i * 0.125, 0.15, 46, 30 + i * 4)
    st.noot("pauk", 2, 1.5, 46, 80)
    for i, kk in enumerate((82, 86, 89, 94)):
        st.noot("glo", 2 + i * 0.1667, 1.5, kk, 56)
    return st


def stinger_baas_verslagen():
    """A boss beaten (cheered up!): the second half of the leitmotif as a brass fanfare, timpani, a soft cymbal, a big tonic."""
    st = _stinger("baas_verslagen", "C", 104)
    st.zet_akkoorden(0, "1 5,1 1")
    st.spoor("trompet", "trompet", 0.9, 0.4)
    st.spoor("hoorn", "hoorn", 0.8, 0.45)
    st.spoor("str", "tremolo", 0.6, 0.45)
    st.spoor("pauk", "pauk", 0.75, 0.4)
    st.spoor("bek", "slagwerk", 0.45, 0.6)
    st.spoor("glo", "glocken", 0.45, 0.6)
    st.spoor("bas", "contrabas", 0.7, 0.35)
    for bar, beat, dur, g in st.LEIDMOTIEF[8:]:
        kk = 72 + st.schaal[g % 7] + 12 * (g // 7)
        st.noot("trompet", (bar - 2) * 4 + beat, dur * 0.95, kk, 90 if beat == 0 else 82)
        st.noot("hoorn", (bar - 2) * 4 + beat, dur * 0.95, kk - 12, 72)
    for kk in (60, 64, 67, 72):
        st.noot("hoorn", 8, 4, kk, 78)
        st.noot("str", 0, 8, kk, 44)
        st.noot("str", 8, 4, kk + 12, 60)
    st.noot("trompet", 8, 4, 79, 88)
    st.noot("trompet", 8, 4, 76, 80)
    for t, kk in ((0, 36), (4, 31), (6, 31), (7.5, 31)):
        st.noot("pauk", t, 0.5, kk + 12, 70)
    for i in range(8):
        st.noot("pauk", 7 + i * 0.125, 0.15, 43, 40 + i * 5)
    st.noot("pauk", 8, 2, 36 + 12, 92)
    st.noot("bek", 8, 1, 49, 70)
    st.noot("bas", 0, 4, 36, 66)
    st.noot("bas", 4, 2, 41, 62)
    st.noot("bas", 6, 2, 43, 64)
    st.noot("bas", 8, 4, 36, 74)
    for i, kk in enumerate((84, 88, 91, 96, 100)):
        st.noot("glo", 8 + i * 0.125, 1.5, kk, 52)
    return st


def stinger_geheim():
    """A secret found: a twinkly descending celesta line over a floating lydian chord, a glass shimmer."""
    st = Stuk("geheim", "F", "lydisch", 92, 4, seed=5)
    st.spoor("cel", "celesta", 1.0, 0.6)
    st.spoor("pad", "glaspad", 0.6, 0.6)
    st.spoor("harp", "harp", 0.8, 0.6)
    k = 89
    for i in range(9):
        st.noot("cel", i * 0.25, 0.6, k, 70 - i * 2)
        k = st.stap(k, -1 if i % 3 != 2 else 2)
    for kk in (65, 69, 71, 76):
        st.noot("pad", 0, 5, kk, 48)
    for i, kk in enumerate((53, 60, 65, 69, 71, 76)):
        st.noot("harp", 2.5 + i * 0.1, 3, kk, 54)
    return st


STINGERS = [stinger_ontdekking, stinger_quest_klaar, stinger_level_op, stinger_baas_verslagen, stinger_geheim]


def alle():
    """[(group, name, make(), render options)]"""
    l = []
    for s in GEBIEDEN:
        galm = 3.2 if s.get("rustig") else 2.4
        l.append(("gebied", s["naam"], (lambda s=s: gebied(s)), {"galm": galm, "rms": -25.0, "uitval": 2.0}))
    for s in BAZEN:
        l.append(("baas", s["naam"], (lambda s=s: baas(s)), {"lus": True, "galm": 1.8, "rms": -21.0, "helder": 6500}))
        # U-AUDIO2: the last-phase loop (same notes + the fase layers, same length) and the boss's own victory fanfare
        l.append(("baas", s["naam"] + "_fase", (lambda s=s: baas(s, fase=True)),
                  {"lus": True, "galm": 1.8, "rms": -20.0, "helder": 7000, "kwaliteit": 0.8}))
        l.append(("fanfare", s["naam"], (lambda s=s: fanfare(s)), {"galm": 2.0, "rms": -21.0, "uitval": 0.6, "kwaliteit": 0.6}))
    for f in STINGERS:
        n = f.__name__.replace("stinger_", "")
        l.append(("stinger", n, f, {"galm": 2.4, "rms": -21.0, "uitval": 0.4, "kwaliteit": 0.5}))
    import zijdeland   # U-Z6MUZIEK: zone 6, het Zijdeland (area + town themes, bosses with phases + fanfares, 2 stingers)
    l += zijdeland.alle()
    return l
