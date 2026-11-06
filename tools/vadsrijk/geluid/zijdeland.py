"""
U-Z6MUZIEK: the music of zone 6, Het Zijdeland (ZONE6_PLAN §9.4): old Middle-East flavour, but guh-sweet, warm and cosy.

Timbres from the GM SoundFont: oud = nylon guitar ("oed"), buzuq/oud tremolo = mandolin ("mandoline"), qanun = dulcimer
("kanun") and koto, ney = shakuhachi ("ney"), darbuka + riq = the GM kit's congas/bongos/tambourine, plus music box, harp,
celesta and tinker bells for the sweetness. Maqam colour: hijaz (phrygian dominant), nahawand (harmonic minor), ajam (major),
rast (mixolydian, without the quarter tones).

The LEITMOTIF of the zone is Oma Dadel's KARAVAANLIED (4 bars, see KaravaanStuk): it is quoted in the area themes (B section,
or A in De Moederoase), in the Rozenkloof town tune, in the stingers, in Ringeling's last phase (the whole oasis sings it) and in
her sweet ending.

Pieces (group/name -> sounds/vadsrijk/muziek/<group>/<name>.ogg):
  gebied/  roze_duinen, palmenkloven, zoutvlakte, zoutvlakte_nacht, ruinedal, moederoase
  stad/    rozenkloof_1 (Rozenplein), rozenkloof_2 (echo in de kloof), saffraanpoort_1 (de Grote Bazaar),
           saffraanpoort_2 (het Rozijnenpaleis / de Paradijstuin)
  baas/    wormelien, zoutkolos (field bosses); sterrenwacht (Astrolabiumwachter), glimmertje, verzonken_sterrenguh (dungeon);
           zandwachter, spiegelvizier, sterrenbeelden, ringeling (raid)  + every one's _fase loop
  fanfare/ the same 9 (Ringeling's is hand-written: grumpy hijaz -> the karavaanlied in sweet major)
  stinger/ baken (a star beacon lit), moederoase (De Moederoase found)
"""
import random

from componist import Stuk
from vormen import baas, fanfare, gebied


def _g(**k):
    d = {"gain": 0.8, "galm": 0.45}
    d.update(k)
    return d


ORNAMENT = ("ney", "kanun", "oed", "mandoline")


class ZijdeStuk(Stuk):
    """A Stuk that ornaments its ney / qanun / oud melodies the maqam way while they are written (so the echoes and the _fase
    doublings copy the ornaments too): a quick upper-neighbour grace before some long notes, now and then a turn. The ornaments
    have their own seeded random, so the composition itself is unchanged and the _fase loop stays note-identical."""

    def __init__(self, *a, **k):
        super().__init__(*a, **k)
        self.orn = random.Random(7919 + sum(map(ord, self.naam)))

    def periode(self, spoor, *a, **k):
        i = len(self.sporen[spoor][3])
        m = super().periode(spoor, *a, **k)
        self._versier(spoor, i)
        return m

    def leidmotief(self, spoor, *a, **k):
        i = len(self.sporen[spoor][3])
        super().leidmotief(spoor, *a, **k)
        self._versier(spoor, i)

    def _versier(self, spoor, vanaf):
        instr, gain, galm, noten = self.sporen[spoor]
        if instr not in ORNAMENT:
            return
        r = self.orn
        nieuw = noten[:vanaf]
        for s, d, k, v in noten[vanaf:]:
            if d >= 0.9 and r.random() < 0.35:
                g = 0.14 if instr == "ney" else 0.11
                nieuw.append((s, g * 0.9, self.stap(k, 1), max(1, v - 14)))
                if r.random() < 0.3:
                    nieuw.append((s + g, g * 0.9, k, max(1, v - 10)))
                    nieuw.append((s + 2 * g, g * 0.9, self.stap(k, -1), max(1, v - 12)))
                    nieuw.append((s + 3 * g, d - 3 * g, k, v))
                else:
                    nieuw.append((s + g, d - g, k, v))
            else:
                nieuw.append((s, d, k, v))
        noten[:] = nieuw


class KaravaanStuk(ZijdeStuk):
    """A Stuk whose leitmotif is Oma Dadel's karavaanlied instead of the Vadsrijk motif (same 4-bar 4/4 shape).
    In hijaz: 5 b6 5 4 3 | 4 3 b2 3 | 3 4 5 b6 5 | 4 3 b2 1 ; in a major mode the same degrees sound bright and sweet."""
    LEIDMOTIEF = [
        (0, 0, 1, 4), (0, 1, .5, 5), (0, 1.5, .5, 4), (0, 2, 1, 3), (0, 3, 1, 2),
        (1, 0, .5, 3), (1, .5, .5, 2), (1, 1, 1, 1), (1, 2, 2, 2),
        (2, 0, 1, 2), (2, 1, .5, 3), (2, 1.5, .5, 4), (2, 2, 1, 5), (2, 3, 1, 4),
        (3, 0, .5, 3), (3, .5, .5, 2), (3, 1, 1, 1), (3, 2, 2, 0),
    ]

    def __init__(self, *a, **k):
        super().__init__(*a, **k)
        # hijaz: the b2 major chord is the sweet-exotic neighbour (Eb over D); elsewhere a plain I - V
        self.LEIDMOTIEF_AKKOORDEN = "1 2,1 1 2,1" if self.modus == "hijaz" else "1 5,1 1 5,1"


# ------------------------------------------------------------------------------------------------ percussion (GM kit keys)
DUM, TEK, TEK2, KA, RIQ, BLOK, BLOK2, TRIANGEL = 64, 62, 60, 63, 54, 76, 77, 81


def _zacht(patroon, f):
    return [(b, k, int(v * f)) for b, k, v in patroon]


MAQSUM = [(0, DUM, 64), (0.5, TEK, 46), (1.5, TEK, 50), (2, DUM, 60), (3, TEK, 52), (3.5, TEK2, 38)]
BALADI = [(0, DUM, 64), (0.5, DUM, 54), (1.5, TEK, 48), (2, DUM, 60), (3, TEK, 50)]
SAMAI = [(0, DUM, 60), (1, TEK, 44), (1.5, TEK2, 36), (2, TEK, 48)]   # 3/4
ZESACHT = [(0, DUM, 60), (1, TEK, 42), (1.5, DUM, 50), (2.5, TEK, 44)]   # 6/8
RIQJE = [(0, RIQ, 34), (1, RIQ, 26), (2, RIQ, 32), (3, RIQ, 26)]
MAQSUM_ZWAAR = [(0, DUM, 84), (0.5, TEK, 60), (1, TEK2, 46), (1.5, TEK, 64), (2, DUM, 80), (2.5, KA, 52), (3, TEK, 66),
                (3.5, TEK2, 54), (3.75, TEK2, 46)]
SAMAI_ZWAAR = [(0, DUM, 82), (0.5, TEK2, 48), (1, TEK, 62), (1.5, TEK, 54), (2, DUM, 70), (2.5, TEK2, 50)]
DUBBEL = [(0, DUM, 80), (0.25, TEK2, 44), (0.5, TEK, 58), (0.75, TEK2, 44), (1, KA, 60), (1.25, TEK2, 44), (1.5, TEK, 60),
          (1.75, TEK2, 46), (2, DUM, 78), (2.25, TEK2, 44), (2.5, TEK, 56), (2.75, TEK2, 44), (3, DUM, 70), (3.25, TEK, 54),
          (3.5, TEK, 60), (3.75, TEK, 66), (0, RIQ, 44), (1, RIQ, 40), (2, RIQ, 44), (3, RIQ, 40)]
DUBBEL3 = [(0, DUM, 80), (0.25, TEK2, 44), (0.5, TEK, 58), (0.75, TEK2, 44), (1, KA, 60), (1.25, TEK2, 46), (1.5, TEK, 60),
           (2, DUM, 74), (2.25, TEK2, 46), (2.5, TEK, 60), (2.75, TEK, 66), (0, RIQ, 44), (1, RIQ, 40), (2, RIQ, 44)]
TAIKO = [(0, 45, 76), (1, 45, 50), (2, 45, 70), (2.5, 45, 48), (3, 50, 60)]
TAIKO3 = [(0, 45, 76), (1, 45, 48), (1.5, 45, 56), (2, 50, 60)]


def _fase(drie=False, extra=None, koor=62):
    """The zone-6 last phase: double-time darbuka + riq, a timpani pulse, a high tremolo ostinato and a choir pad."""
    l = [("slag", "slagwerk", DUBBEL3 if drie else DUBBEL, _g(gain=0.62, galm=0.2)),
         ("slag", "pauk", [(0, 41, 62), (2, 36, 56)] if drie else [(0, 41, 64), (2, 36, 58), (3.5, 36, 50)], _g(gain=0.6, galm=0.3)),
         ("ostinato", "tremolo", 67, 50, _g(gain=0.48, galm=0.4, figuur=(0, 7, 12) if drie else (0, 7, 12, 7), stap=0.25)),
         ("pad", "koor", koor, 52, _g(gain=0.5, galm=0.5, n=4))]
    return l + (extra or [])


# ------------------------------------------------------------------------------------------------------------- area themes
GEBIEDEN = [
    # De Roze Duinen (lv 60-62): long pink dunes, the caravan road, bultguh bells. Ney over a rocking oud, a soft maqsum; the
    # karavaanlied comes in the B section on the music box.
    dict(naam="roze_duinen", toon="D", modus="hijaz", bpm=92, maat=4, seed=6101, leidmotief="B", klasse=KaravaanStuk,
         A="1 2 1 7 | 1 4 2 1", B="",
         melodie=("ney", 69, 86, 74), melodie2=("kanun", 67, 86, 74), melodieB=("speeldoos", 74, 88, 72),
         begeleiding=[("arp", "oed", 57, 0.5, "golf", 52, _g()), ("bas", "pizz", 38, "kwint", 56, _g(gain=0.7)),
                      ("pad", "strijkers", 62, 34, _g(gain=0.45))],
         vol=[("slag", "slagwerk", _zacht(MAQSUM, 0.55), _g(gain=0.35, galm=0.25)),
              ("slag", "buisklok", [(0, 86, 22)], _g(gain=0.2, galm=0.7))],
         intro=[("arp", "oed", 57, 0.5, "golf", 48, _g())], verdubbel=("tinkel", 0, 0, 40, 0.3, 0.6)),
    # De Palmenkloven (lv 62-64): the river canyon, waterfalls, palms, silk trees. A lilting 6/8 on qanun + oud, ney in B.
    dict(naam="palmenkloven", toon="F", modus="majeur", bpm=102, maat=3, samengesteld=True, seed=6102,
         A="1 4 1 5 | 1 4 5 1", B="6 3m 4 1 | 2m 5 4 5",
         melodie=("kanun", 69, 88, 74), melodie2=("oed", 62, 79, 78), melodieB=("ney", 69, 86, 72),
         begeleiding=[("arp", "harp", 62, 0.5, "golf", 50, _g()), ("bas", "pizz", 41, "wals", 54, _g(gain=0.7)),
                      ("pad", "strijkers", 60, 34, _g(gain=0.45))],
         vol=[("slag", "slagwerk", _zacht(ZESACHT, 0.55), _g(gain=0.33, galm=0.25)),
              ("arp", "druppels", 84, 1.5, "neer", 34, _g(gain=0.3, galm=0.6))],
         intro=[("arp", "harp", 62, 0.5, "golf", 46, _g())]),
    # De Zoutvlakte by day (lv 64-66): white-pink salt, glittering. A slow rast-like waltz, qanun + celesta, a glassy pad.
    dict(naam="zoutvlakte", toon="E", modus="mixo", bpm=78, maat=3, seed=6103, rustig=True,
         A="1 7 1 4 | 1 7 4 1", B="4 1 7 4 | 2m 4 5m 5m",
         melodie=("kanun", 71, 88, 72), melodie2=("celesta", 72, 91, 70), melodieB=("ney", 68, 85, 70),
         begeleiding=[("pad", "glaspad", 62, 38, _g(gain=0.5)), ("arp", "harp", 59, 0.5, "golf", 46, _g()),
                      ("bas", "contrabas", 40, "wals", 46, _g(gain=0.55))],
         vol=[("slag", "slagwerk", _zacht(SAMAI, 0.5), _g(gain=0.3, galm=0.3)),
              ("arp", "tinkel", 88, 1.5, "neer", 32, _g(gain=0.3, galm=0.7))],
         intro=[("pad", "glaspad", 62, 34, _g(gain=0.5))], echo=("kanun", 0.75, 0.4, 12)),
    # De Zoutvlakte at night: the caravans travel by the stars. Breathy ney, a music-box karavaanlied, a halo pad, crickets live
    # in the ambience.
    dict(naam="zoutvlakte_nacht", toon="E", modus="hijaz", bpm=66, maat=4, seed=6104, rustig=True, kort=True, leidmotief="B",
         klasse=KaravaanStuk, A="1 7 1 2 | 1 4 2 1", B="",
         melodie=("ney", 64, 81, 70), melodie2=("vibrafoon", 64, 81, 64), melodieB=("speeldoos", 76, 88, 66),
         begeleiding=[("pad", "halopad", 59, 36, _g(gain=0.45)), ("arp", "kanun", 64, 1.0, "op", 42, _g(gain=0.6)),
                      ("bas", "contrabas", 40, "heel", 44, _g(gain=0.55))],
         intro=[("pad", "halopad", 59, 32, _g(gain=0.45))], echo=("tinkel", 1.0, 0.35, 12)),
    # Het Ruïnedal (lv 66-68): mesas, half-buried star temples. Hijaz but hushed: qanun + vibraphone, a soft choir, a far bell.
    dict(naam="ruinedal", toon="C", modus="hijaz", bpm=72, maat=4, seed=6105, rustig=True,
         A="1 2 1 7 | 1 4 2 1", B="4 7 1 1 | 4 2 2 2",
         melodie=("kanun", 67, 84, 72), melodie2=("vibrafoon", 67, 84, 66), melodieB=("ney", 67, 84, 70),
         begeleiding=[("pad", "oohs", 60, 40, _g(gain=0.5, n=4)), ("arp", "harp", 60, 1.0, "neer", 46, _g()),
                      ("bas", "cello", 36, "heel", 44, _g(gain=0.55))],
         vol=[("slag", "buisklok", [(0, 72, 30)], _g(gain=0.3, galm=0.7)), ("slag", "slagwerk", _zacht(BALADI, 0.45), _g(gain=0.28))],
         intro=[("pad", "oohs", 60, 36, _g(gain=0.5, n=4))], echo=("celesta", 1.5, 0.3, 12)),
    # De Moederoase (lv 68-70): the hidden lake, the Moederpalm. Warm and grand: the karavaanlied blooms in A (ney, then violin),
    # harp waves, strings, a soft choir and the darbuka.
    dict(naam="moederoase", toon="Eb", modus="majeur", bpm=80, maat=4, seed=6106, leidmotief="A", klasse=KaravaanStuk,
         A="", B="6 3m 4 1 | 2m 5 1 5",
         melodie=("ney", 63, 80, 76), melodie2=("viool", 70, 87, 70), melodieB=("kanun", 67, 86, 74),
         begeleiding=[("arp", "harp", 63, 0.5, "golf", 48, _g()), ("pad", "strijkers", 60, 42, _g(gain=0.6)),
                      ("bas", "contrabas", 39, "kwint", 50, _g(gain=0.6))],
         vol=[("pad", "koor", 63, 32, _g(gain=0.4)), ("slag", "slagwerk", _zacht(BALADI, 0.5), _g(gain=0.3, galm=0.3))],
         intro=[("arp", "harp", 63, 0.5, "golf", 44, _g()), ("slag", "buisklok", [(0, 75, 30)], _g(gain=0.3, galm=0.7))],
         verdubbel=("tinkel", 0, 0, 36, 0.3, 0.6)),
]

# ------------------------------------------------------------------------------------------------------------- town tunes
STEDEN = [
    # Rozenkloof 1, het Rozenplein: a happy arrival tune, music box + qanun, baladi darbuka, the karavaanlied in B
    dict(naam="rozenkloof_1", toon="G", modus="majeur", bpm=104, maat=4, seed=6201, leidmotief="B", klasse=KaravaanStuk,
         A="1 4 5 1 | 6 4 5 1", B="",
         melodie=("speeldoos", 74, 91, 76), melodie2=("kanun", 67, 86, 76), melodieB=("ney", 67, 84, 74),
         begeleiding=[("wals", "oed", 59, 46, _g()), ("bas", "pizz", 43, "kwart", 54, _g(gain=0.7)),
                      ("slag", "slagwerk", _zacht(BALADI, 0.6), _g(gain=0.35, galm=0.2))],
         vol=[("arp", "glocken", 83, 1.0, "op", 30, _g(gain=0.35)), ("slag", "slagwerk", RIQJE, _g(gain=0.35))],
         intro=[("wals", "oed", 59, 42, _g())]),
    # Rozenkloof 2, echo in de kloof: a ney in the canyon with its own echo, harp, hijaz, slow
    dict(naam="rozenkloof_2", toon="D", modus="hijaz", bpm=84, maat=3, seed=6202, rustig=True,
         A="1 2 1 7 | 1 4 2 1", B="4 7 1 2 | 4 7 2 2",
         melodie=("ney", 66, 83, 74), melodie2=("kanun", 69, 86, 70), melodieB=("ney", 69, 86, 72),
         begeleiding=[("arp", "harp", 62, 0.5, "golf", 46, _g()), ("pad", "strijkers", 60, 34, _g(gain=0.45)),
                      ("bas", "contrabas", 38, "wals", 46, _g(gain=0.55))],
         vol=[("slag", "slagwerk", _zacht(SAMAI, 0.5), _g(gain=0.3))],
         intro=[("arp", "harp", 62, 0.5, "golf", 42, _g())], echo=("ney", 1.0, 0.45, 0)),
    # Saffraanpoort 1, de Grote Bazaar: bustle! Buzuq tremolo (mandolin) + clarinet, an oud ostinato, maqsum + riq
    dict(naam="saffraanpoort_1", toon="A", modus="hijaz", bpm=122, maat=4, seed=6203,
         A="1 1 2 1 | 7 7 2 1", B="4 4 1 1 | 7 2 1 1",
         melodie=("mandoline", 69, 88, 78), melodie2=("klarinet", 64, 81, 74), melodieB=("kanun", 69, 88, 76),
         begeleiding=[("ostinato", "oed", 45, 50, _g(gain=0.65, figuur=(0, 7, 12, 7, 0, 7, 13, 12))),
                      ("bas", "akbas", 33, "kwart", 52, _g(gain=0.65)),
                      ("slag", "slagwerk", _zacht(MAQSUM, 0.7), _g(gain=0.4, galm=0.2))],
         vol=[("slag", "slagwerk", RIQJE, _g(gain=0.4)), ("arp", "xylofoon", 81, 0.5, "op", 30, _g(gain=0.3))],
         intro=[("ostinato", "oed", 45, 46, _g(gain=0.65, figuur=(0, 7, 12, 7, 0, 7, 13, 12)))]),
    # Saffraanpoort 2, het Rozijnenpaleis + de Paradijstuin: an elegant courtyard waltz, qanun and flute, a fountain of celesta
    dict(naam="saffraanpoort_2", toon="F", modus="majeur", bpm=88, maat=3, seed=6204,
         A="1 3m 4 1 | 6 4 5 1", B="4 5 3m 6 | 2m 5 1 5",
         melodie=("kanun", 69, 88, 74), melodie2=("fluit", 72, 88, 72), melodieB=("harp", 67, 86, 72),
         begeleiding=[("pad", "strijkers", 60, 40, _g(gain=0.55)), ("arp", "celesta", 77, 0.5, "golf", 38, _g(gain=0.45)),
                      ("bas", "contrabas", 41, "wals", 48, _g(gain=0.6))],
         vol=[("slag", "slagwerk", _zacht(SAMAI, 0.5), _g(gain=0.3)), ("tegen", "hoorn", 60, 40, _g(gain=0.45))],
         intro=[("arp", "celesta", 77, 0.5, "golf", 36, _g(gain=0.45))]),
]

# ------------------------------------------------------------------------------------------------------------- boss themes
BAZEN = [
    # ---- field bosses
    # Koningin Wormelien (sand worm queen with lashes): a heavy, slithering hijaz march, horn + buzuq, darbuka + taiko
    dict(naam="wormelien", toon="C", modus="hijaz", bpm=120, maat=4, seed=6301,
         A="1 2 1 2 | 1 4 2 1", B="4 4 7 7 | 4 2 2 2",
         melodie=("hoorn", 58, 74, 90), melodie2=("mandoline", 67, 86, 84),
         begeleiding=[("ostinato", "contrabas", 36, 64, _g(gain=0.85, figuur=(0, 1, 4, 1, 0, 1, 7, 4))),
                      ("slag", "slagwerk", MAQSUM_ZWAAR, _g(gain=0.7, galm=0.2)), ("slag", "taiko", TAIKO, _g(gain=0.7))],
         vol=[("arp", "oed", 60, 0.5, "golf", 52, _g(gain=0.6)), ("pad", "koor", 60, 44, _g(gain=0.45))],
         verdubbel=("kanun", 12, 0, 56, 0.55, 0.35),
         fase=_fase(extra=[("arp", "kanun", 72, 0.25, "golf", 46, _g(gain=0.45))]),
         fase_dubbel=("koper", 12, 0, 62, 0.55, 0.35),
         fanfare=dict(lead="hoorn", glans="tinkel")),
    # De Zoutkolos (a salt construct, at night; its crystals "sing"): crystal pings, trombone, taiko, a tremolo bed
    dict(naam="zoutkolos", toon="E", modus="hijaz", bpm=108, maat=4, seed=6302,
         A="1 1 2 2 | 1 7 2 1", B="4 7 1 1 | 4 7 2 2",
         melodie=("trombone", 52, 69, 90, 1.0, 0.3), melodie2=("kanun", 71, 88, 84),
         begeleiding=[("ostinato", "kristal", 76, 46, _g(gain=0.5, figuur=(0, 7, 12, 7, 13, 7, 12, 7), stap=0.5)),
                      ("slag", "taiko", [(0, 40, 80), (1.5, 40, 58), (2, 45, 66), (3, 40, 60)], _g(gain=0.85)),
                      ("bas", "contrabas", 40, "kwint", 62, _g(gain=0.8)), ("slag", "slagwerk", _zacht(MAQSUM_ZWAAR, 0.8), _g(gain=0.55))],
         vol=[("pad", "tremolo", 59, 44, _g(gain=0.5)), ("slag", "pauk", [(0, 40, 56)], _g(gain=0.55))],
         verdubbel=("glocken", 12, 0, 44, 0.4, 0.5),
         fase=_fase(extra=[("arp", "glocken", 83, 0.25, "op", 40, _g(gain=0.4))]),
         fanfare=dict(lead="kanun", koper="trombone", glans="kristal")),
    # ---- dungeon: De Verzonken Sterrenwacht
    # De Astrolabiumwachter (rings of bronze that turn): a clockwork hijaz, harpsichord 16ths, trumpet
    dict(naam="sterrenwacht", toon="G", modus="hijaz", bpm=132, maat=4, seed=6311,
         A="1 2 1 7 | 1 2 7 1", B="4 4 2 2 | 4 7 2 2",
         melodie=("trompet", 64, 81, 88), melodie2=("kanun", 67, 88, 84),
         begeleiding=[("ostinato", "klavecimbel", 55, 54, _g(gain=0.55, figuur=(0, 4, 7, 12, 7, 4), stap=0.25)),
                      ("slag", "slagwerk", MAQSUM_ZWAAR + [(1, BLOK, 40), (3, BLOK2, 40)], _g(gain=0.65, galm=0.2)),
                      ("bas", "contrabas", 43, "kwart", 60, _g(gain=0.8))],
         vol=[("pad", "koper", 55, 46, _g(gain=0.5)), ("arp", "celesta", 79, 0.5, "golf", 40, _g(gain=0.4))],
         verdubbel=("glocken", 12, 0, 42, 0.4, 0.4), fase=_fase(),
         fanfare=dict(lead="trompet", glans="celesta")),
    # Kootje Glimmertje (the Lampendovers' foreman with a giant candle snuffer): a sneaky tiptoe in nahawand, clarinet + oud
    dict(naam="glimmertje", toon="A", modus="harm", bpm=126, maat=4, seed=6312,
         A="1 1 6 6 | 1 1 4 5M", B="4 4 1 1 | 6 2d 5M 5M",
         melodie=("klarinet", 64, 81, 86), melodie2=("oed", 64, 81, 84),
         begeleiding=[("ostinato", "pizz", 45, 60, _g(figuur=(0, 12, 7, 12, 0, 12, 8, 7), stap=0.5)),
                      ("slag", "slagwerk", MAQSUM + [(1.5, BLOK, 36), (3.5, BLOK2, 36)], _g(gain=0.6, galm=0.2)),
                      ("bas", "contrabas", 45, "kwart", 58, _g(gain=0.75))],
         vol=[("pad", "tremolo", 64, 42, _g(gain=0.5)), ("slag", "pauk", [(0, 45, 48)], _g(gain=0.5))],
         verdubbel=("xylofoon", 12, 0, 46, 0.45, 0.3), fase=_fase(),
         fanfare=dict(lead="klarinet", glans="glocken")),
    # De Verzonken Sterrenguh (an old giant of star stone gone grey; a victim, cheered up): a sad-grand nahawand waltz, cello +
    # choir, celesta stars; its last phase = the telescope beams meeting (brass, choir, bells)
    dict(naam="verzonken_sterrenguh", toon="D", modus="harm", bpm=96, maat=3, seed=6313,
         A="1 6 4 5M | 1 6 2d 5M", B="6 3 4 1 | 6 4 5M 5M",
         melodie=("cello", 50, 67, 90, 1.0, 0.4), melodie2=("koor", 62, 79, 76),
         begeleiding=[("bas", "contrabas", 38, "wals", 58, _g(gain=0.75)), ("wals", "harp", 62, 46, _g()),
                      ("arp", "celesta", 74, 0.5, "golf", 40, _g(gain=0.45)),
                      ("slag", "slagwerk", _zacht(SAMAI_ZWAAR, 0.7), _g(gain=0.5, galm=0.3))],
         vol=[("pad", "strijkers", 57, 44, _g(gain=0.55)), ("slag", "pauk", [(0, 38, 50)], _g(gain=0.55))],
         verdubbel=("hoorn", 12, 0, 56, 0.55, 0.45),
         fase=_fase(drie=True, extra=[("arp", "glocken", 81, 0.5, "golf", 40, _g(gain=0.4))]),
         fase_dubbel=("koper", 12, 0, 60, 0.55, 0.4),
         fanfare=dict(lead="hoorn", glans="celesta", zacht=True, bpm=96)),
    # ---- raid: De Sterrendovende Toren
    # 1. De Zandwachter (a sand golem with an hourglass): a ticking woodblock hourglass, horn + oud, maqsum + taiko
    dict(naam="zandwachter", toon="F", modus="hijaz", bpm=116, maat=4, seed=6321,
         A="1 2 1 7 | 1 4 2 1", B="7 7 1 1 | 4 4 2 2",
         melodie=("hoorn", 60, 77, 90), melodie2=("oed", 65, 84, 86),
         begeleiding=[("slag", "slagwerk", MAQSUM_ZWAAR + [(b * 0.5, BLOK if b % 2 == 0 else BLOK2, 30) for b in range(8)],
                       _g(gain=0.7, galm=0.2)),
                      ("ostinato", "contrabas", 41, 62, _g(gain=0.8, figuur=(0, 0, 1, 0, 4, 0, 1, 0))),
                      ("slag", "taiko", TAIKO, _g(gain=0.75))],
         vol=[("pad", "koper", 58, 46, _g(gain=0.5)), ("arp", "kanun", 72, 0.5, "golf", 44, _g(gain=0.45))],
         verdubbel=("trombone", -12, 0, 62, 0.6, 0.3),
         # the last phase: the hourglass runs out (16th ticks) + the zone's phase layers
         fase=_fase(extra=[("slag", "slagwerk", [(b * 0.25, BLOK, 34) for b in range(16)], _g(gain=0.6))]),
         fanfare=dict(lead="hoorn", glans="tinkel")),
    # 2. De Spiegelvizier (a hall of mirrors, the vizier hides in reflections): qanun + glassy music box, mysterious 3/4;
    # the last phase: the hall breaks (shattering crystal, crashes, taiko)
    dict(naam="spiegelvizier", toon="Ab", modus="hijaz", bpm=138, maat=3, seed=6322,
         A="1 2 1 7 | 1 4 2 1", B="4 7 1 2 | 4 7 2 2",
         melodie=("kanun", 68, 87, 88), melodie2=("viool", 68, 87, 84),
         begeleiding=[("bas", "contrabas", 44, "wals", 60, _g(gain=0.75)), ("wals", "speeldoos", 68, 44, _g(gain=0.55)),
                      ("pad", "glaspad", 63, 42, _g(gain=0.5)), ("slag", "slagwerk", _zacht(SAMAI_ZWAAR, 0.8), _g(gain=0.55))],
         vol=[("arp", "celesta", 75, 0.5, "golf", 44, _g(gain=0.45)), ("slag", "pauk", [(0, 44, 50)], _g(gain=0.5))],
         verdubbel=("glocken", 12, 0, 44, 0.45, 0.6),
         fase=_fase(drie=True, extra=[("arp", "kristal", 87, 0.25, "neer", 46, _g(gain=0.45, galm=0.6)),
                                      ("slag", "slagwerk", [(0, 49, 52)], _g(gain=0.5)),
                                      ("slag", "taiko", TAIKO3, _g(gain=0.7))]),
         fanfare=dict(lead="kanun", glans="kristal")),
    # 3. De Gedoofde Sterrenbeelden (three at once: De Grote Knuffel, De Vliegende Bultguh, De Kleine Pluis): three voices -
    # a low horn (the tank), fast strings (the stormer) and a celesta line (the healer); the "_fase" = the window (tense strings)
    dict(naam="sterrenbeelden", toon="E", modus="lydisch", bpm=120, maat=4, seed=6323,
         A="1 2 1 7m | 1 2 5 1", B="6m 2 3m 6m | 4 2 5 5",
         melodie=("hoorn", 56, 72, 90), melodie2=("snelstrijk", 68, 88, 82),
         begeleiding=[("ostinato", "pizz", 52, 58, _g(figuur=(0, 7, 12, 14, 12, 7), stap=0.5)),
                      ("slag", "slagwerk", MAQSUM_ZWAAR, _g(gain=0.6, galm=0.2)), ("bas", "contrabas", 40, "kwint", 60, _g(gain=0.8))],
         vol=[("tegen", "celesta", 76, 56, _g(gain=0.55)), ("pad", "glaspad", 64, 40, _g(gain=0.45))],
         verdubbel=("tinkel", 24, 0, 48, 0.4, 0.6),
         fase=_fase(extra=[("ostinato", "snelstrijk", 64, 50, _g(gain=0.5, figuur=(0, 1, 0, 1), stap=0.25))]),
         fanfare=dict(lead="celesta", koper="hoorn", glans="tinkel")),
    # 4. Hertogin Ringeling, de Ringvinger: a pompous waltz with jingling rings (tambourine, tinker bells), trumpet + harpsichord;
    # her last phase "Ringdans": the whole oasis sings the karavaanlied (choir) in the B section, double-time darbuka, brass
    dict(naam="ringeling", toon="D", modus="hijaz", bpm=144, maat=3, seed=6324, klasse=KaravaanStuk,
         A="1 2 1 7 | 1 4 2 1", B="1 2,1 1 2,1 | 1 2,1 1 2,1",
         melodie=("trompet", 62, 81, 90), melodie2=("klavecimbel", 66, 86, 84),
         begeleiding=[("bas", "tuba", 38, "wals", 62, _g(gain=0.8)), ("wals", "snelstrijk", 62, 50, _g(gain=0.6)),
                      ("slag", "slagwerk", [(0, DUM, 74), (1, RIQ, 52), (1.5, RIQ, 40), (2, RIQ, 52), (2, TEK, 50)], _g(gain=0.6, galm=0.25))],
         vol=[("arp", "tinkel", 86, 0.5, "golf", 40, _g(gain=0.4, galm=0.6)), ("pad", "koper", 57, 46, _g(gain=0.5))],
         verdubbel=("glocken", 12, 0, 44, 0.45, 0.5),
         fase=_fase(drie=True, extra=[("arp", "tinkel", 90, 0.25, "golf", 44, _g(gain=0.4, galm=0.6))]),
         fase_dubbel=("koper", 12, 0, 62, 0.55, 0.35)),
]


# ------------------------------------------------------------------------------------------------------------- post-passes
def karavaan_koor(st, spec):
    """Ringeling's last phase: the choir sings the karavaanlied over the B section (bars 11-18 of the loop), in 3/4."""
    st.spoor("koor_karavaan", "koor", 0.55, 0.5)
    B = st.maat
    start = (2 + 8) * B   # intro (2 bars) + A (8 bars)
    for herhaal in range(2):
        t0 = start + herhaal * 4 * B
        for bar, beat, dur, g in st.LEIDMOTIEF:
            o, d = divmod(g, 7)
            k = 62 + st.schaal[d] + 12 * o
            st.noot("koor_karavaan", t0 + bar * B + beat * 0.75, dur * 0.75 * 0.95, k, 66 + (6 if beat == 0 else 0))
            st.noot("koor_karavaan", t0 + bar * B + beat * 0.75, dur * 0.75 * 0.95, k - 12, 52)
    return st


def _gebied(s):
    s.setdefault("klasse", ZijdeStuk)
    return gebied(s)


def _baas(s, fase=False):
    s.setdefault("klasse", ZijdeStuk)
    st = baas(s, fase=fase)
    if fase and s["naam"] == "ringeling":
        karavaan_koor(st, s)
    return st


def _fanfare(s):
    if s["naam"] == "ringeling":
        return fanfare_ringeling()
    s.setdefault("klasse", ZijdeStuk)
    return fanfare(s)


def fanfare_ringeling():
    """Ringeling cheered up: a last grumpy hijaz flourish (her trumpet), a breath, a harp glissando up, and then the karavaanlied's
    last two bars in sweet D major on the music box + choir + strings, ending on a warm chord with tinker bells (~10 s)."""
    st = KaravaanStuk("ringeling_fanfare", "D", "majeur", 92, 4, seed=6399)
    st.zet_akkoorden(0, "1 1 1 5 1 1")
    st.spoor("tr", "trompet", 0.8, 0.4)
    st.spoor("pauk", "pauk", 0.6, 0.4)
    # 1) grumpy: D hijaz run down (D C Bb A G F# Eb D) on trumpet, staccato, then a sulky low Eb-D
    for i, k in enumerate((86, 84, 82, 81, 79, 78, 75, 74)):
        st.noot("tr", i * 0.25, 0.2, k, 84 - i * 3)
    st.noot("tr", 2.25, 0.4, 63, 70)
    st.noot("tr", 2.75, 0.9, 62, 62)
    st.noot("pauk", 2.75, 0.5, 38, 60)
    # 2) the turn: a harp glissando up over D major
    st.spoor("harp", "harp", 0.9, 0.55)
    k = 50
    for i in range(14):
        st.noot("harp", 4 + i * 0.07, 1.6, k, 50 + i * 2)
        k = st.stap(k, 1)
    # 3) sweet: bars 3-4 of the karavaanlied (major) on music box + choir, a held tonic
    st.spoor("doos", "speeldoos", 1.0, 0.5)
    st.spoor("koor", "oohs", 0.6, 0.55)
    st.spoor("str", "strijkers", 0.6, 0.5)
    st.spoor("bas", "contrabas", 0.6, 0.35)
    st.spoor("glo", "tinkel", 0.5, 0.65)
    t0 = 5
    for bar, beat, dur, g in st.LEIDMOTIEF[9:]:
        o, d = divmod(g, 7)
        kk = 74 + st.schaal[d] + 12 * o
        st.noot("doos", t0 + (bar - 2) * 4 + beat, dur * 0.95, kk, 84)
        st.noot("koor", t0 + (bar - 2) * 4 + beat, dur * 0.95, kk - 12, 58)
    eind = t0 + 8
    for kk in (62, 66, 69, 74):
        st.noot("str", t0, 8, kk - 12 if kk == 62 else kk, 48)
        st.noot("str", eind, 4, kk, 58)
        st.noot("koor", eind, 4, kk, 56)
    st.noot("doos", eind, 4, 86, 86)
    st.noot("doos", eind, 4, 90, 70)
    st.noot("bas", t0, 4, 38, 60)
    st.noot("bas", t0 + 4, 2, 45, 58)
    st.noot("bas", t0 + 6, 2, 45, 58)
    st.noot("bas", eind, 4, 38, 66)
    for i, kk in enumerate((86, 90, 93, 98, 102)):
        st.noot("glo", eind + i * 0.15, 1.6, kk, 54)
    return st


# ------------------------------------------------------------------------------------------------------------- stingers
def stinger_baken():
    """A star beacon lit: a soft whoosh of harp + kanun up, a ney call (the first bar of the karavaanlied), a bright chord with
    tinker bells and celesta sparkling down (~5 s)."""
    st = KaravaanStuk("baken", "D", "majeur", 96, 4, seed=6401)
    st.zet_akkoorden(0, "1 1 1")
    st.spoor("harp", "harp", 0.9, 0.5)
    st.spoor("kanun", "kanun", 0.7, 0.5)
    k = 50
    for i in range(12):
        st.noot("harp", i * 0.06, 1.5, k, 46 + i * 3)
        st.noot("kanun", i * 0.06 + 0.03, 0.8, k + 12, 40 + i * 2)
        k = st.stap(k, 1)
    st.spoor("ney", "ney", 0.95, 0.55)
    for bar, beat, dur, g in st.LEIDMOTIEF[:5]:
        o, d = divmod(g, 7)
        st.noot("ney", 1 + beat, dur * 0.95, 74 + st.schaal[d] + 12 * o, 80)
    st.noot("ney", 5, 2.5, 74 + 7, 82)
    st.spoor("pad", "strijkers", 0.6, 0.5)
    for kk in (62, 66, 69, 74):
        st.noot("pad", 1, 6.5, kk, 46)
    st.spoor("glo", "tinkel", 0.5, 0.65)
    st.spoor("cel", "celesta", 0.6, 0.6)
    for i, kk in enumerate((98, 93, 90, 86, 81, 78)):
        st.noot("cel", 5 + i * 0.17, 1.2, kk, 60 - i * 3)
    for i, kk in enumerate((86, 90, 93, 98)):
        st.noot("glo", 5 + i * 0.1, 1.6, kk, 50)
    return st


def stinger_moederoase():
    """De Moederoase found: the karavaanlied's first two bars on the ney over harp waves, then a choir + strings swell into a
    warm Eb major chord, timpani roll, bells (~9 s)."""
    st = KaravaanStuk("moederoase", "Eb", "majeur", 84, 4, seed=6402)
    st.zet_akkoorden(0, "1 5,1 1 1")
    st.spoor("harp", "harp", 0.85, 0.55)
    for bar in range(3):
        k = 51
        for i in range(8):
            st.noot("harp", bar * 4 + i * 0.5, 1.6, k + [0, 7, 12, 16, 19, 16, 12, 7][i] + (7 if bar == 1 and i % 2 else 0), 46)
    st.spoor("ney", "ney", 0.95, 0.55)
    for bar, beat, dur, g in st.LEIDMOTIEF[:9]:
        o, d = divmod(g, 7)
        st.noot("ney", bar * 4 + beat, dur * 0.95, 63 + st.schaal[d] + 12 * o, 80)
    st.spoor("koor", "koor", 0.6, 0.55)
    st.spoor("str", "strijkers", 0.65, 0.5)
    st.spoor("pauk", "pauk", 0.6, 0.4)
    st.spoor("bas", "contrabas", 0.6, 0.35)
    st.spoor("glo", "tinkel", 0.5, 0.65)
    for i, kk in enumerate((55, 58, 63, 67)):
        st.noot("koor", 6 + i * 0.25, 6, kk + 12, 40 + i * 4)
    for kk in (51, 58, 63, 67, 70, 75):
        st.noot("str", 8, 5, kk + 12, 58)
    st.noot("bas", 0, 4, 39, 54)
    st.noot("bas", 4, 2, 46, 52)
    st.noot("bas", 6, 2, 39, 52)
    st.noot("bas", 8, 5, 39, 64)
    for i in range(12):
        st.noot("pauk", 6.5 + i * 0.125, 0.15, 39, 30 + i * 4)
    st.noot("pauk", 8, 2, 39, 80)
    st.noot("ney", 8, 4.5, 75, 82)
    for i, kk in enumerate((87, 91, 94, 99, 103)):
        st.noot("glo", 8 + i * 0.15, 1.8, kk, 52)
    return st


STINGERS = [stinger_baken, stinger_moederoase]

GEBIED_NAMEN = [s["naam"] for s in GEBIEDEN]
STAD_NAMEN = [s["naam"] for s in STEDEN]
BAAS_NAMEN = [s["naam"] for s in BAZEN]
STINGER_NAMEN = [f.__name__.replace("stinger_", "") for f in STINGERS]


def alle():
    """[(group, name, make(), render options)] for maak_muziek (via stukken.alle())."""
    l = []
    for s in GEBIEDEN:
        galm = 3.2 if s.get("rustig") else 2.6
        l.append(("gebied", s["naam"], (lambda s=s: _gebied(s)), {"galm": galm, "rms": -25.0, "uitval": 2.0}))
    for s in STEDEN:
        l.append(("stad", s["naam"], (lambda s=s: _gebied(s)), {"galm": 2.6, "rms": -25.0, "uitval": 2.0}))
    for s in BAZEN:
        l.append(("baas", s["naam"], (lambda s=s: _baas(s)), {"lus": True, "galm": 1.8, "rms": -21.0, "helder": 6500}))
        l.append(("baas", s["naam"] + "_fase", (lambda s=s: _baas(s, fase=True)),
                  {"lus": True, "galm": 1.8, "rms": -20.0, "helder": 7000, "kwaliteit": 0.8}))
        l.append(("fanfare", s["naam"], (lambda s=s: _fanfare(s)), {"galm": 2.2, "rms": -21.0, "uitval": 0.6, "kwaliteit": 0.6}))
    for f in STINGERS:
        l.append(("stinger", f.__name__.replace("stinger_", ""), f, {"galm": 2.6, "rms": -21.0, "uitval": 0.5, "kwaliteit": 0.5}))
    return l
