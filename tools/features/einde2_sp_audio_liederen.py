"""
einde2 "Sponsguh" (CONTRACT_E2_C 6.2), slice sp_audio: the three songs as DATA (pure python, CI-safe): tempo, length, and per
sung line its key, start beat, length in beats and the Dutch words. Own songs: own titles, own melodies (in
einde2_sp_audio_geluid.py, which builds the tune of every line from the start beat given here) and own Dutch words
(nothing of the series or the film: no "Sweet Victory", no "Goofy Goober Rock").

  einde2_sp_audio.py          writes the words (scene.guhs.<scene>.lied.<key>) and checks that SpLiederen.java holds the
                              same ticks as this file
  einde2_sp_audio_geluid.py   builds the recordings on these beats (run by hand)

Ticks: one beat = 1200 / bpm ticks; every tempo below gives a whole number (100 -> 12, 150 -> 8, 120 -> 10).
A line is shown from its start for its length (a caption that runs on: Cutscene.Builder.lied), so the captions follow the
recording when the scene starts the song at the same tick (SpAudio.lied does both).
"""

# key -> (scene id, title, bpm, beats per bar, bars, [(line key, start beat, beats, Dutch words), ...])
LIEDEREN = {
    # Part 1's finale: a power ballad that turns into a stadium anthem (piano and organ, then the whole band and the town),
    # one key change up for the last lines. 100 BPM, 26 bars, 62.4 s.
    "zoete_zege": ("einde2_sp_d1_recept_zoete_zege", "Zoete Knabbelzege", 100, 4, 26, [
        ("intro", 0, 8, "♪ Sponsguh haalt diep adem... ♪"),
        ("1", 8, 8, "Een kruimel alleen is zo klein in de zee,"),
        ("2", 16, 8, "een bel die je blaast, neemt de stroming weer mee."),
        ("3", 24, 8, "Maar kijk eens om je heen: de hele stad is hier,"),
        ("4", 32, 8, "met trommels en met oortjes vol plezier!"),
        ("5", 40, 8, "Dus zet de lampen aan... en hou je oortjes vast..."),
        ("6", 48, 8, "Dit is onze zoete knabbelzege!"),
        ("7", 56, 8, "Hoor de bodem bonken, hoor de bellen gaan!"),
        ("8", 64, 8, "Met z'n allen zijn we nooit verlegen,"),
        ("9", 72, 8, "want samen zijn we niet te verslaan!"),
        ("10", 80, 8, "Dit is onze zoete knabbelzege!"),
        ("11", 88, 16, "Samen zijn we niet... te... ver-slaaaan!"),
    ]),
    # Part 2's finale: the rock song "Ik ben een Guhfy Guhber" (DESIGN's own title). 150 BPM, 34 bars, 54.4 s.
    "guhfy_guhber": ("einde2_sp_d2_finale_guhfy_guhber", "Ik ben een Guhfy Guhber", 150, 4, 34, [
        ("intro", 0, 16, "♪ Een gitaar scheurt door de Bellenkom! ♪"),
        ("1", 16, 8, "Ze zeiden: jochie, je bent nog maar een kind,"),
        ("2", 24, 8, "met een ijshoedje op en je oortjes in de wind."),
        ("3", 32, 8, "Maar wie klein is, blaast de grootste bel,"),
        ("4", 40, 8, "en wie lacht met zijn oortjes, die wint het spel!"),
        ("5", 48, 8, "Ik ben een Guhfy Guhber!"),
        ("6", 56, 8, "Met ijs op mijn hoofd en een knabbel in mijn hand!"),
        ("7", 64, 8, "Ik ben een Guhfy Guhber!"),
        ("8", 72, 8, "De luidste kleine guh van het hele land!"),
        ("9", 80, 8, "Emmers af! Emmers af!"),
        ("10", 88, 8, "Bikinibodem, word wakker... NJEG!"),
        ("11", 96, 8, "Ik ben een Guhfy Guhber!"),
        ("12", 104, 8, "Met ijs op mijn hoofd en een knabbel in mijn hand!"),
        ("13", 112, 8, "Ik ben een Guhfy Guhber!"),
        ("14", 120, 16, "De luidste kleine guh van het hele land!"),
    ]),
    # The trench monsters: (the scene has them roar first) a hummed music-box start, then a sweet waltz in low voices.
    # 3/4, 120 BPM, 20 bars, 30 s.
    "monsters": ("einde2_sp_d2_weg_monsters", "Het lied van de loopgraaf", 120, 3, 20, [
        ("intro", 0, 6, "♪ Een monster neuriet een toontje... en dan zingen ze allemaal! ♪"),
        ("1", 6, 12, "Diep in de loopgraaf, waar het licht nooit komt,"),
        ("2", 18, 12, "zingen wij monsters, en niemand die bromt."),
        ("3", 30, 12, "Wij lijken eng, met tanden zo lang,"),
        ("4", 42, 12, "maar wie met ons meezingt, is nergens meer bang!"),
        ("slot", 54, 6, "♪ Oeh-oeh... grrr... (een tevreden monstergrom) ♪"),
    ]),
}


def tel_ticks(bpm):
    """Ticks per beat (always whole)."""
    t = 1200 / bpm
    assert t == int(t), f"{bpm} BPM gives no whole number of ticks per beat"
    return int(t)


def regels_ticks(lied):
    """[(key, start tick, ticks), ...] of a song: what SpLiederen.java holds."""
    _, _, bpm, _, _, regels = LIEDEREN[lied]
    t = tel_ticks(bpm)
    return [(k, b * t, n * t) for k, b, n, _ in regels]


def duur_ticks(lied):
    """The whole song in ticks (its bars)."""
    _, _, bpm, maat, maten, _ = LIEDEREN[lied]
    return maten * maat * tel_ticks(bpm)


def duur_seconden(lied):
    return duur_ticks(lied) / 20.0
