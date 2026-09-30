"""The hula songs of Guhwai'i (3.0, guhwaii-spellen) and the little surf/hula sounds.

Three original songs in the spirit of a fifties crooner on a Hawaiian beach: a guh with a big quiff, a steel guitar that
sighs, ukuleles, a slapback echo on everything, and the guh choir (the mod's own guh noises, tools/remix/guhstem.py)
singing the melodies. The song is the level of the hula game (feature/guhwaiispellen/HulaLiedje.java):

  aloha_njeg        makkelijk   88 BPM  32 bars   a slow moonlight croon: steel guitar, ukulele, ipu and "ooh-wah" guhs
  guhla_hula_rock   medium     132 BPM  48 bars   rock'n'roll on the beach: boogie slap bass, guitar with slapback, brass
  vahoeg_hula_hop   lastig     160 BPM  60 bars   surf-rock/rockabilly: tom rolls, tremolo surf guitar, "VAHOEG!" shouts

Beat 0 is at t = 0 (every beat k at k * 60 / BPM s), mono 48 kHz, as loud as the disco songs (guhband.master_rms);
checked by check_songs.py. The bars of every song (SECTIONS) are the same as the chart sections in HulaLiedje.java.

Also the small sounds of the surf spot (sounds/guhwaiispellen/): ukelele_tokkel (a happy strum: a trick / a hula step),
golf_breekt (a wave breaking), schelpje (a shell coin clinks), aloha (three guh notes: "A-lo-ha!"), plons (a wipeout splash).

    python tools/remix/make_hula.py [out_dir]      (deterministic: fixed seeds; default out_dir = the mod's sounds folder)
"""
import os
import sys

import numpy as np

import guhmix
from guhmix import SR, Tracks, filt, reverb, delay, kick, snare, hat, pluck, write_ogg, expdec, adsr, mf, brush_snare, compress
from guhband import chord, pizz, trumpet, master_rms
from guhstem import Choir, place

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "guhs", "sounds", "guhwaiispellen"))

# id: (bpm, bars) - keep in sync with HulaLiedje.java (and check_songs.py)
SONGS = {"aloha_njeg": (88.0, 32), "guhla_hula_rock": (132.0, 48), "vahoeg_hula_hop": (160.0, 60)}


def reseed(seed):
    guhmix.rng = np.random.default_rng(seed)
    import guhband
    guhband.rng = guhmix.rng


def R():
    return guhmix.rng


# =====================================================================================================================
# instruments
# =====================================================================================================================
UKE_STRINGS = [67, 60, 64, 69]          # G C E A (re-entrant: the G string is high)


def uke_voicing(tones):
    """The chord on the four ukulele strings: per string the lowest chord tone within 7 frets."""
    pcs = {t % 12 for t in tones}
    out = []
    for open_ in UKE_STRINGS:
        for fret in range(0, 8):
            if (open_ + fret) % 12 in pcs:
                out.append(open_ + fret)
                break
        else:
            out.append(open_)
    return out


def ukulele(tones, dur, down=True, vel=1.0, spread=0.011):
    """A strum over the four strings (down: G C E A, up: the other way), a bright wooden box."""
    notes = uke_voicing(tones)
    if not down:
        notes = notes[::-1]
    n = int((dur + spread * 4) * SR)
    s = np.zeros(n)
    for k, m in enumerate(notes):
        p = pluck(m, dur, bright=0.62, damp=0.9935) * (0.85 + 0.15 * R().random())
        i = int(k * spread * SR)
        s[i:i + len(p)] += p[: n - i]
    s = filt(s, "high", 220) + filt(s, "band", [1800, 3200]) * 0.35      # the little body
    return s * vel * 0.55


def uke_pick(m, dur=0.3, vel=1.0):
    p = pluck(m, dur, bright=0.7, damp=0.994)
    return filt(p, "high", 250) * vel * 0.7


def twang(m, dur, vel=1.0, drive=1.8):
    """A fifties hollow-body electric guitar: a bright pluck, a bit of tube drive."""
    p = pluck(m, dur, bright=0.9, damp=0.9975)
    p = np.tanh(p * drive) / np.tanh(drive)
    p = filt(p, "low", 5200) + filt(p, "band", [700, 1500]) * 0.4
    return p * vel * 0.6


def surf_guitar(m, dur, vel=1.0, trem=None, bpm=160.0):
    """A surf guitar: tremolo-picked (16ths) or a single stab, lots of spring reverb added on the bus."""
    if trem is None:
        return twang(m, dur, vel, 2.4)
    n = int(dur * SR)
    s = np.zeros(n)
    step = 60.0 / bpm / 4
    k = 0
    while k * step < dur - 0.01:
        p = twang(m, min(step * 1.6, dur - k * step), vel * (0.95 if k % 2 == 0 else 0.8), 2.4)
        i = int(k * step * SR)
        s[i:i + len(p)] += p[: n - i]
        k += 1
    return s


def steel_guitar(m_from, m_to, dur, vel=1.0):
    """Hawaiian lap steel: a swell that slides into the note, a slow warm vibrato, a sine-rich tone."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    glide = np.clip(t / max(0.001, min(0.18, dur * 0.3)), 0, 1)
    mm = m_from + (m_to - m_from) * (1 - (1 - glide) ** 2)
    mm = mm + 0.14 * np.sin(2 * np.pi * 5.2 * t) * np.clip((t - 0.15) / 0.3, 0, 1)
    f = guhmix.A4 * 2 ** ((mm - 69) / 12)
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(ph) + 0.45 * np.sin(2 * ph) + 0.22 * np.sin(3 * ph) + 0.1 * np.sin(4 * ph)
    env = adsr(n, min(0.09, dur * 0.25), 0.2, 0.75, min(0.25, dur * 0.3))
    return filt(s, "low", 4200) * env * vel * 0.32


def ipu(accent=1.0):
    """The ipu (a gourd drum): a hollow low thump."""
    n = int(0.28 * SR)
    t = np.arange(n) / SR
    f = 95 + 60 * np.exp(-t / 0.02)
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * expdec(n, 0.11)
    s += np.sin(2 * np.pi * 210 * t) * expdec(n, 0.03) * 0.3
    s += filt(R().normal(0, 1, n), "band", [300, 1200]) * expdec(n, 0.006) * 0.35
    return s * 0.75 * accent


def ipu_pa(accent=1.0):
    """The slap on the ipu's side: "pa"."""
    n = int(0.12 * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * 330 * t) * expdec(n, 0.03) + filt(R().normal(0, 1, n), "band", [900, 3000]) * expdec(n, 0.01) * 0.6
    return s * 0.45 * accent


def uliuli(length=0.09, accent=1.0):
    """The feathered gourd rattle ('uli'uli): a soft shake."""
    n = int(length * SR)
    s = filt(R().normal(0, 1, n), "band", [3500, 9000]) * adsr(n, 0.012, 0.03, 0.5, 0.03)
    return s * 0.22 * accent


def tom(pitch=1.0, accent=1.0):
    n = int(0.35 * SR)
    t = np.arange(n) / SR
    f = (120 * pitch) * (1 + 0.5 * np.exp(-t / 0.03))
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * expdec(n, 0.16)
    s += filt(R().normal(0, 1, n), "band", [400, 2500]) * expdec(n, 0.012) * 0.4
    return s * 0.6 * accent


def slap_bass(m, dur, vel=1.0, slap=True):
    """An upright bass slapped on the off-beat: the note (pizzicato) plus a click of the strings on the fingerboard."""
    s = pizz(m, dur, vel)
    if slap:
        n = min(len(s), int(0.03 * SR))
        s[:n] += filt(R().normal(0, 1, n), "band", [1500, 5000]) * expdec(n, 0.004) * 0.5 * vel
    return s


def sax(notes, dur, vel=1.0):
    """A breathy tenor sax (the brass section's soft sister)."""
    s = trumpet(notes, dur, vel)
    s = filt(s, "low", 3200) + filt(guhmix.rng.normal(0, 1, len(s)), "band", [1500, 4500]) * adsr(len(s), 0.02, 0.1, 0.4, 0.05) * 0.04
    return s


# =====================================================================================================================
# helpers
# =====================================================================================================================
def mix_down(tr, beat, vox_gain=1.5, slapback=0.11, room=0.22, names=None):
    g = tr.get
    vox = filt(g("vox"), "high", 140)
    vox = compress(vox * 1.8, thr=0.25, ratio=3.0)
    vox = vox + delay(vox, slapback, taps=2, fb=0.3, gain=0.35, lp=3500) + reverb(vox, 1.6, 0.5) * 0.25
    gtr = g("gtr")
    gtr = gtr + delay(gtr, slapback, taps=2, fb=0.3, gain=0.4, lp=4000) + reverb(gtr, 2.2, 0.8, 5000) * 0.3
    steel = g("steel")
    steel = steel + reverb(steel, 2.8, 1.1, 5000) * 0.45
    band = g("bass") * 0.9 + g("uke") * 2.0 + gtr + steel + g("brass") + g("keys")
    music = g("drums") * 0.8 + band * tr.side
    mix = music + reverb(g("uke") * 0.5 + g("brass") + g("keys"), 1.6, 0.5, 6000) * room + vox * vox_gain
    rms = lambda x: float(np.sqrt(np.mean(x ** 2)))
    print(f"  final: drums {rms(g('drums') * 0.8):.3f} bass {rms(g('bass') * 0.9):.3f} uke {rms(g('uke') * 2.0):.3f} gtr {rms(gtr):.3f} steel {rms(steel):.3f} brass {rms(g('brass')):.3f} vox {rms(vox * vox_gain):.3f}")
    for name in names or ("drums", "bass", "uke", "gtr", "steel", "brass", "keys", "vox"):
        print(f"  {name:6s} rms {np.sqrt(np.mean(g(name) ** 2)):.4f}")
    return mix


def melody(tr, choir, t0, beat, notes, gain=1.0, glide=0.0, short=False):
    """notes: [(beat offset, midi, beats)]; sung by the guh choir."""
    for b, m, ln in notes:
        place(tr, choir.note(m, ln * beat * 0.96, glide=glide if ln >= 1 else 0, short=short or ln < 0.75), t0 + b * beat, gain)


# =====================================================================================================================
# 1. Aloha, Njeg (makkelijk, 88 BPM, F): a slow moonlight croon
# =====================================================================================================================
def aloha_njeg():
    reseed(3001)
    bpm, bars = SONGS["aloha_njeg"]
    beat = 60 / bpm
    bar = 4 * beat
    sections = [("intro", ["F", "Dm", "Gm7", "C7"]),
                ("verse", ["F", "Am", "Bb", "F", "Gm7", "C7", "F", "C7"]),
                ("chorus", ["Bb", "F", "Bb", "F", "Gm7", "C7", "F", "F7"]),
                ("solo", ["F", "Dm", "Gm7", "C7"]),
                ("chorus2", ["Bb", "F", "Gm7", "C7", "F", "Dm"]),
                ("outro", ["Gm7", "C7"])]
    assert sum(len(c) for _, c in sections) == bars
    verse = [[(0, 72, 1.5), (1.5, 69, 0.5), (2, 72, 1), (3, 74, 1)], [(0, 72, 2), (2, 69, 1), (3, 67, 1)],
             [(0, 70, 1), (1, 72, 1), (2, 74, 1.5), (3.5, 72, 0.5)], [(0, 69, 3)],
             [(0, 70, 1.5), (1.5, 69, 0.5), (2, 67, 1), (3, 70, 1)], [(0, 72, 2), (2, 70, 1), (3, 67, 1)],
             [(0, 69, 1), (1, 67, 1), (2, 65, 2)], []]
    chorus = [[(0, 74, 1), (1, 72, 0.5), (1.5, 70, 1.5), (3, 72, 1)], [(0, 69, 2.5), (3, 72, 1)],
              [(0, 74, 1), (1, 72, 0.5), (1.5, 77, 1.5), (3, 76, 1)], [(0, 72, 3)],
              [(0, 70, 1), (1, 72, 1), (2, 74, 1), (3, 72, 1)], [(0, 70, 1), (1, 69, 1), (2, 67, 2)],
              [(0, 65, 3.5)], [(2, 72, 0.5), (2.5, 72, 0.5), (3, 75, 1)]]
    chorus2 = [chorus[0], chorus[1], chorus[4], chorus[5], [(0, 65, 2), (2, 69, 0.5), (2.5, 72, 1.5)], []]
    tr = Tracks(bars * bar + 5.0)
    lead = Choir("guh_ambient", flat=0.9)
    low = Choir("guh_ambient", max_shift=16, flat=0.9)
    b0 = 0
    for sec, chords in sections:
        for b, name in enumerate(chords):
            t0 = (b0 + b) * bar
            root, tones = chord(name)
            # --- the beat: ipu on 1 and 3 (the game's beat), a soft pa and brushes on 2 and 4, uli'uli on the and ---
            for q in range(4):
                tb = t0 + q * beat
                if q % 2 == 0:
                    tr.add("drums", ipu(1.0 if q == 0 else 0.8), tb, 0.85)
                    tr.add("drums", kick(0.55, 0.25, 46), tb, 0.45)
                else:
                    tr.add("drums", ipu_pa(0.8), tb, 0.6)
                    if sec != "intro":
                        tr.add("drums", brush_snare(), tb, 0.5)
                tr.add("drums", uliuli(), tb + beat / 2, 0.8 if sec != "intro" else 0.5)
            # --- the bass: root on 1, fifth on 3 (a warm upright) ---
            tr.add("bass", slap_bass(root, beat * 1.8, 0.9, slap=False), t0, 0.8)
            tr.add("bass", slap_bass(root + 7, beat * 1.8, 0.8, slap=False), t0 + 2 * beat, 0.7)
            # --- ukulele: a gentle "down, down-up, up-down-up" island strum ---
            for pos, down, v in ((0, True, 1.0), (1, True, 0.7), (1.5, False, 0.55), (2.5, False, 0.6), (3, True, 0.75), (3.5, False, 0.5)):
                tr.add("uke", ukulele(tones, beat * 0.9, down, v), t0 + pos * beat, 0.5)
            # --- the steel guitar: swells on every chord in the intro, fills in the gaps, the solo ---
            if sec == "intro":
                top = max(tones) + 12
                tr.add("steel", steel_guitar(top - 2, top, bar * 0.95, 1.0), t0, 0.9)
                if b == 3:
                    for k, m in enumerate((72, 74, 76, 77)):
                        tr.add("steel", steel_guitar(m - 1, m, beat * 0.9, 0.8), t0 + (2 + k * 0.5) * beat, 0.6)
            elif sec == "solo":
                lick = [[(0, 77, 1.5, 76), (1.5, 76, 0.5, 74), (2, 72, 2, 71)], [(0, 74, 1, 72), (1, 77, 1, 76), (2, 81, 2, 79)],
                        [(0, 79, 1.5, 77), (1.5, 77, 0.5, 79), (2, 74, 2, 72)], [(0, 76, 1, 74), (1, 72, 1, 70), (2, 70, 1, 69), (3, 72, 1, 71)]][b]
                for pos, m, ln, frm in lick:
                    tr.add("steel", steel_guitar(frm, m, ln * beat * 0.98, 1.0), t0 + pos * beat, 1.0)
            elif (sec == "verse" and b in (3, 7)) or (sec.startswith("chorus") and b in (3, 7)):
                for k, m in enumerate(sorted(tones)[-3:]):
                    tr.add("steel", steel_guitar(m + 11, m + 12, beat * 1.2, 0.7), t0 + (1 + k) * beat, 0.6)
            # --- the guhs: the croon, and "ooh-wah" backing ---
            notes = None
            if sec == "verse":
                notes = verse[b]
            elif sec == "chorus":
                notes = chorus[b]
            elif sec == "chorus2":
                notes = chorus2[b]
            if notes:
                melody(tr, lead, t0, beat, notes, 1.0, glide=0.6)
            if sec in ("verse", "chorus", "chorus2") and b % 2 == 1:
                for k, m in enumerate(sorted(tones)[:2]):     # "oooh" on the 2nd bar of each pair, low guhs
                    place(tr, low.note(m - 12 if m > 66 else m, beat * 1.8), t0 + (2 + k * 0.02) * beat, 0.45)
            if sec == "outro" and b == 1:
                place(tr, lead.note(72, beat * 0.5, short=True), t0 + 2 * beat, 0.9)
                place(tr, lead.note(74, beat * 0.5, short=True), t0 + 2.5 * beat, 0.9)
                place(tr, lead.note(77, beat * 1.2, glide=0.8), t0 + 3 * beat, 1.0)
        b0 += len(chords)
    # the ending: one big F6 on the beat after the last bar, a steel slide up and a last "njeg"
    tend = bars * bar
    root, tones = chord("F6")
    tr.add("uke", ukulele(tones, 2.5, True, 1.0, spread=0.03), tend, 0.6)
    tr.add("bass", slap_bass(root, 2.0, 1.0, slap=False), tend, 0.9)
    tr.add("drums", ipu(1.0), tend, 0.9)
    tr.add("steel", steel_guitar(76, 81, 3.5, 1.0), tend, 1.0)
    place(tr, lead.note(77, 1.8, glide=1.0), tend + 0.02, 1.0)
    mix = mix_down(tr, beat, vox_gain=1.15, slapback=0.13)
    return master_rms(mix, fade_out=1.5)


# =====================================================================================================================
# 2. Guhla-Hula Rock (medium, 132 BPM, G): rock'n'roll on the beach
# =====================================================================================================================
def guhla_hula_rock():
    reseed(3002)
    bpm, bars = SONGS["guhla_hula_rock"]
    beat = 60 / bpm
    bar = 4 * beat
    blues = ["G", "G", "G", "G", "C", "C", "G", "G", "D", "C", "G", "D"]
    sections = [("intro", ["G", "G", "D", "D"]), ("vers1", blues), ("solo", blues), ("vers2", blues), ("outro", ["C", "C", "G", "G", "D", "C", "G", "G"])]
    assert sum(len(c) for _, c in sections) == bars
    lead = Choir("guh_ambient", flat=0.88)
    back = Choir("guh_ambient", flat=0.9)
    tr = Tracks(bars * bar + 4.0)
    walk = [0, 4, 7, 9, 10, 9, 7, 4]
    # the hook "Guh-la hu-la ba-by, njeg!" (beat, interval above the chord root in octave 5, beats)
    hook = [(0, 7, 0.5), (0.5, 7, 0.5), (1, 4, 0.5), (1.5, 0, 1.0), (3, 3, 0.25), (3.25, 4, 0.75)]
    answer = [(0, 12, 0.5), (0.5, 10, 0.5), (1, 7, 1.5)]
    b0 = 0
    for sec, chords in sections:
        for b, name in enumerate(chords):
            t0 = (b0 + b) * bar
            root, tones = chord(name)
            r5 = 60 + (root % 12)
            if r5 > 64:
                r5 -= 12
            intro_break = sec == "intro" and b < 2
            # --- drums: kick on 1 and 3, snare on 2 and 4 (slapback on the bus), hats on the eighths, a tom fill every 4 bars ---
            for q in range(4):
                tb = t0 + q * beat
                if not intro_break or q in (0, 2):
                    tr.add("drums", kick(1.0 if q == 0 else 0.85), tb, 0.8)
                    tr.duck(tb, 0.25, beat * 0.8)
                if q % 2 == 1 and not intro_break:
                    tr.add("drums", snare(0.45), tb, 0.75)
                tr.add("drums", hat(False), tb, 0.35)
                tr.add("drums", hat(q % 2 == 1), tb + beat / 2, 0.3)
                tr.add("drums", ipu_pa(0.6), tb + beat / 2, 0.25)
            if intro_break:
                for q in (1, 3):
                    tr.add("drums", tom(1.4 if q == 1 else 1.1), t0 + q * beat, 0.8)
            if (b0 + b) % 4 == 3 and sec != "intro":
                for k in range(4):
                    tr.add("drums", tom(1.6 - k * 0.2, 0.9), t0 + (2 + k * 0.5) * beat, 0.6)
            # --- boogie slap bass (eighths) ---
            if not intro_break:
                for e, iv in enumerate(walk):
                    tr.add("bass", slap_bass(root + iv, beat / 2 * 0.9, 1.0 if e % 2 == 0 else 0.8, slap=e % 2 == 1), t0 + e * beat / 2, 0.75)
            else:
                tr.add("bass", slap_bass(root, beat * 0.9, 1.0), t0, 0.8)
            # --- rhythm: guitar chuck on 2 and 4, ukulele eighths ---
            for q in (1, 3):
                tr.add("gtr", sum(twang(m, beat * 0.35, 0.8) for m in tones) / 2, t0 + q * beat, 0.4 if not intro_break else 0.2)
            if not intro_break:
                for e in range(8):
                    tr.add("uke", ukulele(tones, beat * 0.45, e % 2 == 0, 0.9 if e % 2 == 0 else 0.6), t0 + e * beat / 2, 0.35)
            else:
                tr.add("uke", ukulele(tones, bar * 0.9, True, 1.0, spread=0.02), t0, 0.45)
            # --- the intro guitar riff: a twangy boogie line ---
            if sec == "intro":
                for e, iv in enumerate([0, 4, 7, 9, 10, 9, 7, 4] if b % 2 == 0 else [12, 10, 7, 4, 3, 4, 7, 10]):
                    tr.add("gtr", twang(root + 24 + iv, beat / 2 * 0.95, 0.9), t0 + e * beat / 2, 0.55)
            # --- the guhs: hook and answer in the verses; brass + steel in the solo ---
            if sec in ("vers1", "vers2"):
                if b in (0, 2, 4, 6, 8):
                    melody(tr, lead, t0, beat, [(p, r5 + 12 + iv, ln) for p, iv, ln in hook], 1.0)
                elif b in (1, 3, 5, 7, 9):
                    melody(tr, back, t0, beat, [(p + 2, r5 + 12 + iv, ln) for p, iv, ln in answer], 0.7, short=True)
                if b == 10:
                    melody(tr, lead, t0, beat, [(0, 74, 0.5), (0.5, 74, 0.5), (1, 76, 0.5), (1.5, 74, 0.5), (2, 71, 1), (3, 67, 1)], 1.0)
                if b == 11:
                    for k in range(3):
                        place(tr, back.note(62 + 12, beat * 0.4, short=True), t0 + (1 + k * 0.5) * beat, 0.6)
                if sec == "vers2" and b % 2 == 0:
                    tr.add("brass", sax([r5 + 12 + 7, r5 + 12 + 10], beat * 1.5, 0.8), t0 + 2 * beat, 0.35)
            if sec == "solo":
                if b < 6:
                    for pos, iv, ln, frm in [(0, 12, 1.5, 10), (1.5, 10, 0.5, 12), (2, 7, 1, 6), (3, 10, 1, 9)] if b % 2 == 0 else \
                            [(0, 7, 2, 5), (2, 4, 1, 3), (3, 0, 1, -1)]:
                        tr.add("steel", steel_guitar(r5 + 12 + frm, r5 + 12 + iv, ln * beat * 0.98, 1.0), t0 + pos * beat, 1.0)
                else:
                    for pos, ln in ((0, 0.75), (1.5, 0.5), (2.5, 1.0)):
                        tr.add("brass", trumpet([r5 + 12 + t % 12 for t in tones[:3]], ln * beat, 1.0), t0 + pos * beat, 0.5)
                    if b % 2 == 1:
                        place(tr, lead.note(79, beat * 0.4, short=True), t0 + 3 * beat, 0.8)      # "njeg!"
            if sec == "outro":
                if b % 2 == 0 and b < 6:
                    melody(tr, lead, t0, beat, [(p, r5 + 12 + iv, ln) for p, iv, ln in hook], 1.0)
                if b in (1, 3, 5):
                    melody(tr, back, t0, beat, [(p + 2, r5 + 12 + iv, ln) for p, iv, ln in answer], 0.7, short=True)
                if b == 6:
                    for pos, m in ((0, 67), (1, 71), (2, 74), (3, 79)):
                        tr.add("brass", trumpet([m, m - 4], beat * 0.6, 1.0), t0 + pos * beat, 0.5)
        b0 += len(chords)
    tend = bars * bar
    root, tones = chord("G7")
    tr.add("gtr", sum(twang(m, 2.5, 1.0) for m in tones + [tones[0] + 12]) / 2.5, tend, 0.6)
    tr.add("brass", trumpet([m + 12 for m in tones[:3]], 1.6, 1.1, fall=True), tend, 0.55)
    tr.add("bass", slap_bass(root, 1.5, 1.0), tend, 0.9)
    tr.add("drums", kick(1.0), tend, 1.0)
    tr.add("drums", snare(0.6), tend, 0.7)
    for k in range(6):
        tr.add("drums", tom(1.5 - k * 0.12), tend + 0.3 + k * 0.08, 0.5)
    place(tr, lead.note(79, 1.2, glide=1.5), tend, 1.0)
    mix = mix_down(tr, beat, vox_gain=1.1, slapback=0.105)
    return master_rms(mix, fade_out=1.2)


# =====================================================================================================================
# 3. Vahoeg Hula Hop (lastig, 160 BPM, E): surf-rock and rockabilly
# =====================================================================================================================
def vahoeg_hula_hop():
    reseed(3003)
    bpm, bars = SONGS["vahoeg_hula_hop"]
    beat = 60 / bpm
    bar = 4 * beat
    a_part = ["Em", "Em", "C", "B7", "Em", "Em", "C", "B7", "Am", "Am", "Em", "Em", "C", "B7", "Em", "Em"]
    b_part = ["E", "E", "A", "A", "E", "E", "B7", "B7", "E", "E", "A", "A", "E", "B7", "E", "E"]
    sections = [("intro", ["Em", "Em", "Em", "Em"]), ("a", a_part), ("b", b_part), ("break", ["Em", "Em", "Em", "Em"]),
                ("a2", a_part[:12]), ("outro", ["C", "B7", "Em", "Em", "C", "B7", "Em", "Em"])]
    assert sum(len(c) for _, c in sections) == bars
    lead = Choir("guh_ambient", flat=0.88)
    shout = Choir("guh_ambient", flat=0.8)
    tr = Tracks(bars * bar + 4.0)
    # the surf melody over the A part (tremolo-picked, 2 bars per phrase: (beat, midi, beats))
    surf = [[(0, 76, 1.5), (1.5, 79, 0.5), (2, 78, 1), (3, 76, 1)], [(0, 74, 2), (2, 71, 2)],
            [(0, 72, 1.5), (1.5, 76, 0.5), (2, 79, 2)], [(0, 78, 2), (2, 75, 2)]]
    b0 = 0
    for sec, chords in sections:
        for b, name in enumerate(chords):
            t0 = (b0 + b) * bar
            root, tones = chord(name)
            drums_only = sec == "intro" and b < 2
            brk = sec == "break"
            # --- drums: the surf beat (kick on every beat, snare 2 and 4, toms rolling in the intro and the break) ---
            for q in range(4):
                tb = t0 + q * beat
                tr.add("drums", kick(1.0 if q % 2 == 0 else 0.8), tb, 0.8)
                tr.duck(tb, 0.25, beat * 0.7)
                if q % 2 == 1:
                    tr.add("drums", snare(0.4), tb, 0.8)
                if not drums_only and not brk:
                    tr.add("drums", hat(False), tb + beat / 2, 0.35)
                    tr.add("drums", uliuli(0.06), tb + beat / 4, 0.4)
                    tr.add("drums", uliuli(0.06), tb + 3 * beat / 4, 0.4)
            if drums_only or brk:
                pattern = [(0.5, 1.3), (1, 1.1), (1.5, 1.0), (2.5, 1.3), (3, 1.1), (3.25, 1.0), (3.5, 0.85), (3.75, 0.75)]
                for pos, pitch in pattern:
                    tr.add("drums", tom(pitch, 0.9), t0 + pos * beat, 0.7)
                tr.add("drums", ipu(1.0), t0, 0.6)
            if sec == "intro" and b == 1:
                place(tr, shout.note(76, beat * 1.2, glide=2.0), t0 + 2 * beat, 1.2)            # "VAHOEG!"
            if brk and b in (1, 3):
                place(tr, shout.note(79 if b == 1 else 81, beat * 1.0, glide=2.0), t0 + 2 * beat, 1.1)
            if drums_only:
                continue
            # --- bass: slapped eighths walking the chord ---
            walk = [0, 0, 7, 7, 12, 12, 7, 5] if name[-1:] == "m" else [0, 4, 7, 9, 12, 9, 7, 4]
            for e, iv in enumerate(walk):
                tr.add("bass", slap_bass(root + iv, beat / 2 * 0.9, 1.0 if e % 2 == 0 else 0.8, slap=e % 2 == 1), t0 + e * beat / 2, 0.75)
            # --- ukulele stabs on the off-beats, a rhythm guitar ---
            for q in range(4):
                tr.add("uke", ukulele(tones, beat * 0.3, q % 2 == 0, 0.9), t0 + q * beat + beat / 2, 0.4)
            if sec in ("a", "a2", "outro", "break"):
                for q in (0, 2):
                    tr.add("gtr", sum(twang(m - 12, beat * 0.8, 0.6, 2.2) for m in tones[:3]) / 2, t0 + q * beat, 0.25)
            # --- the lead: surf guitar in the A parts, the guhs in the B part ---
            if sec in ("a", "a2"):
                for pos, m, ln in surf[b % 4]:
                    tr.add("gtr", surf_guitar(m - 12, ln * beat, 0.9, trem=True, bpm=bpm), t0 + pos * beat, 0.55)
                if b % 4 == 3:
                    place(tr, shout.note(81, beat * 0.5, short=True), t0 + 3.5 * beat, 0.9)    # "njeg!"
            if sec == "b":
                phrase = [[(0, 76, 0.5), (0.5, 76, 0.5), (1, 80, 1), (2, 78, 0.5), (2.5, 76, 1.5)], [(0, 71, 1), (1, 73, 1), (2, 76, 2)],
                          [(0, 81, 0.5), (0.5, 81, 0.5), (1, 80, 0.5), (1.5, 78, 0.5), (2, 76, 2)], [(0, 73, 1), (1, 76, 1), (2, 69, 2)]]
                melody(tr, lead, t0, beat, [(p, m - (0 if b % 8 < 4 else 0), ln) for p, m, ln in phrase[b % 4]], 1.0)
                if b % 4 == 3:
                    tr.add("brass", trumpet([m + 12 for m in tones[1:]], beat * 1.5, 1.0), t0 + 2.5 * beat, 0.45)
                if b >= 8:
                    tr.add("steel", steel_guitar(tones[-1] + 11, tones[-1] + 12, bar * 0.9, 0.8), t0, 0.5)
            if sec == "outro":
                if b in (0, 1, 4, 5):
                    for pos, m, ln in surf[b % 4]:
                        tr.add("gtr", surf_guitar(m - 12, ln * beat, 0.9, trem=True, bpm=bpm), t0 + pos * beat, 0.55)
                if b in (3, 7):
                    place(tr, shout.note(79, beat * 1.2, glide=2.0), t0 + 2 * beat, 1.2)         # "VAHOEG!"
        b0 += len(chords)
    tend = bars * bar
    root, tones = chord("Em")
    tr.add("gtr", surf_guitar(64, 2.5, 1.0), tend, 0.7)
    tr.add("gtr", sum(twang(m, 2.5, 1.0, 2.4) for m in tones) / 2, tend, 0.5)
    tr.add("bass", slap_bass(root, 1.5, 1.0), tend, 0.9)
    tr.add("drums", kick(1.0), tend, 1.0)
    for k in range(10):
        tr.add("drums", tom(1.6 - k * 0.09), tend + 0.1 + k * 0.06, 0.5)
    place(tr, shout.note(81, 1.4, glide=3.0), tend + 0.05, 1.2)
    gtr = tr.get("gtr")
    tr.bus["gtr"] = gtr + reverb(gtr, 2.5, 0.9, 4500) * 0.45            # (the spring reverb of a surf amp)
    mix = mix_down(tr, beat, vox_gain=1.1, slapback=0.09)
    return master_rms(mix, fade_out=1.2)


# =====================================================================================================================
# the little sounds
# =====================================================================================================================
def sfx():
    out = {}
    reseed(3010)
    _, tones = chord("C")
    s = ukulele(tones, 0.9, True, 1.0, spread=0.018) + np.concatenate([np.zeros(int(0.13 * SR)), ukulele(chord("F")[1], 0.9, True, 0.9)])[: len(ukulele(tones, 0.9, True, 1.0, spread=0.018))]
    out["ukelele_tokkel"] = s
    # a wave breaking: a rising roar, the crash, and the fizz running out
    n = int(2.2 * SR)
    t = np.arange(n) / SR
    noise = guhmix.rng.normal(0, 1, n)
    env = np.clip(t / 0.5, 0, 1) ** 2 * np.exp(-np.clip(t - 0.5, 0, None) / 0.55)
    roar = filt(noise, "low", 900) * env
    crash = filt(noise, "band", [800, 6000]) * np.exp(-np.clip(t - 0.45, 0, None) / 0.35) * (t > 0.42)
    fizz = filt(guhmix.rng.normal(0, 1, n), "high", 4000) * np.exp(-np.clip(t - 0.6, 0, None) / 0.9) * (t > 0.5) * 0.4
    out["golf_breekt"] = roar * 0.9 + crash * 0.7 + fizz
    # a shell coin: two little glassy chimes
    n = int(0.9 * SR)
    t = np.arange(n) / SR
    s = np.zeros(n)
    for dt, f in ((0.0, 1760), (0.08, 2637)):
        k = int(dt * SR)
        tt = t[: n - k]
        s[k:] += (np.sin(2 * np.pi * f * tt) + 0.4 * np.sin(2 * np.pi * f * 2.76 * tt)) * np.exp(-tt / 0.25)
    out["schelpje"] = s * 0.5
    # "A-lo-ha!" sung by a guh (three notes, the last one high and happy)
    tr = Tracks(1.8)
    ch = Choir("guh_ambient", flat=0.85)
    for k, (m, ln) in enumerate(((74, 0.22), (77, 0.22), (81, 0.7))):
        place(tr, ch.note(m, ln, glide=1.2 if k == 2 else 0, short=k < 2), 0.05 + k * 0.24, 1.0)
    v = tr.get("vox")
    out["aloha"] = v + reverb(v, 1.2, 0.4) * 0.25
    # a plons: somebody falls off the board
    n = int(1.2 * SR)
    t = np.arange(n) / SR
    body = np.sin(2 * np.pi * np.cumsum(180 * np.exp(-t / 0.15) + 60) / SR) * np.exp(-t / 0.18)
    splash = filt(guhmix.rng.normal(0, 1, n), "band", [600, 7000]) * np.exp(-t / 0.3) * np.clip(t / 0.01, 0, 1)
    out["plons"] = body * 0.6 + splash * 0.8
    for k in out:
        x = out[k]
        out[k] = x / (np.max(np.abs(x)) + 1e-9) * 0.85
    return out


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else OUT
    os.makedirs(out, exist_ok=True)
    only = sys.argv[2:] if len(sys.argv) > 2 else None
    makers = {"aloha_njeg": aloha_njeg, "guhla_hula_rock": guhla_hula_rock, "vahoeg_hula_hop": vahoeg_hula_hop}
    for name, fn in makers.items():
        if only and name not in only:
            continue
        print(name)
        write_ogg(os.path.join(out, name + ".ogg"), fn())
        bpm, bars = SONGS[name]
        print(f"  {bars} bars, {bars * 4} beats at {bpm} BPM")
    if not only or "sfx" in only:
        for name, sig in sfx().items():
            write_ogg(os.path.join(out, name + ".ogg"), sig)


if __name__ == "__main__":
    main()
