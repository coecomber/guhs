"""
T-GELUID: the three musical forms built on componist.Stuk:
  gebied(spec)  a soft area theme  (intro, A, A2, B, A3, outro; ~70-100 s; ends on the tonic)
  baas(spec)    a boss theme       (intro, A, B, A2, bridge; a seamless loop of ~50-65 s)
  stinger(...)  is hand-written in stukken.py (a few seconds each)
"""
from componist import Stuk


def _rij(s):
    return " ".join(t for t in s.split() if t != "|")


def _melodie(st, m, start, vel, motief=None, rustig=False):
    instr, lo, hi, v = m[:4]
    st.spoor("melodie_" + instr, instr, *_mix(m))
    return st.periode("melodie_" + instr, start, v + vel, lo, hi, rustig=rustig, motief=motief)


def _mix(m):
    # optional 5th/6th entries: gain, reverb send
    g = m[4] if len(m) > 4 else 1.0
    w = m[5] if len(m) > 5 else 0.35
    return g, w


def _begeleiding(st, beg, start, eind, vel):
    for b in beg:
        soort, instr = b[0], b[1]
        g = b[-1] if isinstance(b[-1], dict) else {}
        naam = soort + "_" + instr
        st.spoor(naam, instr, g.get("gain", 1.0), g.get("galm", 0.35))
        if soort == "arp":
            st.arpeggio(naam, start, eind, b[2], b[5] + vel, stap=b[3], vorm=b[4], klinkt=g.get("klinkt", 1.6))
        elif soort == "pad":
            st.pad(naam, start, eind, b[2], b[3] + vel, n=g.get("n", 3))
        elif soort == "bas":
            st.bas(naam, start, eind, b[2], b[4] + vel, stijl=b[3])
        elif soort == "wals":
            st.walsbegeleiding(naam, start, eind, b[2], b[3] + vel)
        elif soort == "tegen":
            st.tegenstem(naam, start, eind, b[2], b[3] + vel)
        elif soort == "ostinato":
            st.ostinato(naam, start, eind, b[2], b[3] + vel, figuur=g.get("figuur", (0, 0, 7, 0, 12, 0, 7, 0)), stap=g.get("stap", 0.5))
        elif soort == "slag":
            st.slag(naam, start, eind, [(beat, key, v + vel) for beat, key, v in b[2]])


def echo(st, bron, doel, instr, vertraging, factor, transpose=0, gain=0.8, galm=0.5, van=0.0, tot=1e9):
    st.spoor(doel, instr, gain, galm)
    for s, d, k, v in list(st.sporen[bron][3]):
        if van <= s < tot:
            st.noot(doel, s + vertraging, d, k + transpose, v * factor)


def gebied(spec):
    st = spec.get("klasse", Stuk)(spec["naam"], spec["toon"], spec["modus"], spec["bpm"], spec["maat"], spec.get("samengesteld", False), spec["seed"])   # (U-Z6MUZIEK: klasse)
    B = st.maat
    rustig = spec.get("rustig", False)
    A, Bp = _rij(spec["A"]), _rij(spec["B"])
    if not A:
        A = st.LEIDMOTIEF_AKKOORDEN + " " + st.LEIDMOTIEF_AKKOORDEN
    if not Bp:
        Bp = A
    t = 0.0
    secties = []
    # intro: 2 bars on the first two chords of A
    intro = " ".join(A.split()[:2])
    st.zet_akkoorden(t, intro)
    secties.append(("intro", t, t + 2 * B))
    t += 2 * B
    lm = spec.get("leidmotief")
    lmrij = st.LEIDMOTIEF_AKKOORDEN + " " + st.LEIDMOTIEF_AKKOORDEN
    for naam, rij in (("A", A), ("A2", A), ("B", Bp), ("A3", A)):
        if naam == "A3" and spec.get("kort"):
            continue
        if (lm == "A" and naam in ("A", "A2")) or (lm == "B" and naam == "B"):
            rij = lmrij
        st.zet_akkoorden(t, rij)
        secties.append((naam, t, t + 8 * B))
        t += 8 * B
    slot = spec.get("slot", "1 1")
    st.zet_akkoorden(t, slot)
    secties.append(("outro", t, t + 2 * B))
    dyn = {"intro": -12, "A": -2, "A2": 2, "B": 0, "A3": 4, "outro": -10}
    motief = None
    for naam, a, e in secties:
        vel = dyn[naam]
        beg = spec["begeleiding"] if naam not in ("intro", "outro") else spec.get("intro", spec["begeleiding"][:1])
        _begeleiding(st, beg, a, e, vel)
        if naam in ("A2", "A3", "B") and spec.get("vol"):
            _begeleiding(st, spec["vol"], a, e, vel)
        if naam == "A":
            if lm == "A":
                _leid(st, spec["melodie"], a, vel)
            else:
                motief = _melodie(st, spec["melodie"], a, vel, rustig=rustig)
        elif naam == "A2":
            m2 = spec.get("melodie2", spec["melodie"])
            if lm == "A":
                _leid(st, m2, a, vel)
            else:
                _melodie(st, m2, a, vel, motief=motief, rustig=rustig)
        elif naam == "B":
            mb = spec.get("melodieB", spec.get("melodie2", spec["melodie"]))
            if lm == "B":
                _leid(st, mb, a, vel)
            else:
                _melodie(st, mb, a, vel, rustig=rustig)
        elif naam == "A3":
            if lm == "A":
                _leid(st, spec["melodie"], a, vel)
            else:
                _melodie(st, spec["melodie"], a, vel, motief=motief, rustig=rustig)
            if spec.get("verdubbel"):
                instr, lo, hi, v = spec["verdubbel"][:4]
                bron = "melodie_" + spec["melodie"][0]
                echo(st, bron, "dubbel_" + instr, instr, 0, v / 100.0, 12, *_mix(spec["verdubbel"]), van=a, tot=e)
        elif naam == "outro":
            # a last sparkle: the tonic, an octave up, on the first melody instrument
            instr, lo, hi, v = spec["melodie"][:4]
            k = st.dichtstbij((lo + hi) // 2, [st.grond])
            st.noot("melodie_" + instr, a, B * 1.8, k, v - 12)
    if spec.get("echo"):
        e = spec["echo"]
        echo(st, "melodie_" + spec["melodie"][0], "echo_" + e[0], e[0], e[1], e[2], e[3] if len(e) > 3 else 0)
    return st


def _leid(st, m, start, vel):
    """The leitmotif (4 bars) twice: the second time an octave up or on the same pitch, with the chords to match."""
    instr, lo, hi, v = m[:4]
    st.spoor("melodie_" + instr, instr, *_mix(m))
    # (the section already has the leitmotif's chords, see gebied())
    rond = st.dichtstbij((lo + hi) // 2 - 3, [st.grond])
    st.leidmotief("melodie_" + instr, start, v + vel, rond)
    st.leidmotief("melodie_" + instr, start + 4 * st.maat, v + vel + 4, rond)


def baas(spec, fase=False):
    """fase=True (U-AUDIO2): the same piece note for note (same seed, same loop length) plus the last-phase layers on top
    (fase_lagen), so the client can crossfade from the normal loop into this one at the same spot."""
    st = spec.get("klasse", Stuk)(spec["naam"], spec["toon"], spec["modus"], spec["bpm"], spec["maat"], spec.get("samengesteld", False), spec["seed"],
                                   lus=True)   # (U-Z6MUZIEK: klasse)
    B = st.maat
    A, Bp = _rij(spec["A"]), _rij(spec["B"])
    brug = _rij(spec.get("brug", " ".join(A.split()[:4])))
    t = 0.0
    secties = []
    st.zet_akkoorden(t, " ".join(A.split()[:2]))
    secties.append(("intro", t, t + 2 * B))
    t += 2 * B
    for naam, rij in (("A", A), ("B", Bp), ("A2", A)):
        st.zet_akkoorden(t, rij)
        secties.append((naam, t, t + 8 * B))
        t += 8 * B
    st.zet_akkoorden(t, brug)
    secties.append(("brug", t, t + len(brug.split()) * B))
    dyn = {"intro": -8, "A": 0, "B": 4, "A2": 8, "brug": 2}
    motief = None
    for naam, a, e in secties:
        vel = dyn[naam]
        _begeleiding(st, spec["begeleiding"], a, e, vel)
        if naam in ("B", "A2") and spec.get("vol"):
            _begeleiding(st, spec["vol"], a, e, vel)
        if naam == "A":
            motief = _melodie(st, spec["melodie"], a, vel)
        elif naam == "B":
            _melodie(st, spec.get("melodie2", spec["melodie"]), a, vel)
        elif naam == "A2":
            _melodie(st, spec["melodie"], a, vel, motief=motief)
            if spec.get("verdubbel"):
                d = spec["verdubbel"]
                echo(st, "melodie_" + spec["melodie"][0], "dubbel_" + d[0], d[0], 0, d[3] / 100.0, d[1], *_mix(d), van=a, tot=e)
    if fase:
        _fase(st, spec, secties)
    return st


# ---------------------------------------------------------------------------------------------------- U-AUDIO2: the last phase
DUBBEL_TIJD = [(0, 36, 66), (0.5, 42, 40), (1, 38, 58), (1.5, 42, 40), (2, 36, 62), (2.5, 36, 48), (3, 38, 60), (3.25, 38, 40),
               (3.5, 38, 50), (3.75, 38, 56)]
DUBBEL_TIJD3 = [(0, 36, 66), (0.5, 42, 40), (1, 38, 56), (1.5, 42, 40), (2, 38, 54), (2.5, 38, 46)]


def fase_lagen(spec):
    """The extra layers of the last phase: spec["fase"] or, by default, driving drums, a timpani pulse, a high string
    tremolo-ostinato, a choir pad and the melody doubled an octave up on brass (the "everything at once" feel)."""
    if spec.get("fase"):
        return spec["fase"]
    drie = spec["maat"] == 3
    lagen = [
        ("slag", "slagwerk", DUBBEL_TIJD3 if drie else DUBBEL_TIJD, {"gain": 0.6, "galm": 0.2}),
        ("slag", "pauk", [(0, 41, 62), (2, 36, 56)] if drie else [(0, 41, 64), (2, 36, 58), (3.5, 36, 50)], {"gain": 0.6, "galm": 0.3}),
        ("ostinato", "tremolo", 67, 50, {"gain": 0.5, "galm": 0.4, "figuur": (0, 7, 12, 7) if not drie else (0, 7, 12), "stap": 0.25}),
        ("pad", "koor", 62, 52, {"gain": 0.5, "galm": 0.5, "n": 4}),
    ]
    return lagen


def _fase(st, spec, secties):
    dyn = {"intro": 0, "A": 4, "B": 6, "A2": 8, "brug": 4}
    for naam, a, e in secties:
        _begeleiding(st, fase_lagen(spec), a, e, dyn[naam])
    # the melody an octave up on brass (or the boss's own choice) through the whole loop
    d = spec.get("fase_dubbel", ("koper", 12, 0, 62, 0.55, 0.35))
    for m in (spec["melodie"], spec.get("melodie2")):
        if m and ("melodie_" + m[0]) in st.sporen:
            echo(st, "melodie_" + m[0], "fase_" + d[0] + "_" + m[0], d[0], 0, d[3] / 100.0, d[1], *_mix(d))


def fanfare(spec):
    """U-AUDIO2: the boss's own short victory fanfare (~4-6 s): its key turned major (a happy ending: the boss is cheered up),
    its own lead instrument, a rising figure picked by its seed, then a big held tonic with timpani, cymbal and bells."""
    f = dict(spec.get("fanfare", {}))
    bpm = min(spec["bpm"], f.get("bpm", 120))
    st = spec.get("klasse", Stuk)(spec["naam"] + "_fanfare", spec["toon"], "majeur", bpm, 4, seed=spec["seed"] + 7)   # (U-Z6MUZIEK: klasse)
    st.zet_akkoorden(0, "5 5 1 1")
    lead = f.get("lead", spec["melodie"][0])
    lo, hi = spec["melodie"][1], spec["melodie"][2]
    if hi - lo < 12:
        hi = lo + 12
    zacht = f.get("zacht", False)
    st.spoor("lead", lead, f.get("gain", 0.95), 0.45)
    st.spoor("koper", f.get("koper", "koper"), 0.75, 0.45)
    st.spoor("str", "tremolo" if not zacht else "strijkers", 0.55, 0.5)
    st.spoor("bas", "contrabas", 0.65, 0.35)
    st.spoor("glo", f.get("glans", "glocken"), 0.45, 0.6)
    if not zacht:
        st.spoor("pauk", "pauk", 0.7, 0.4)
        st.spoor("bek", "slagwerk", 0.4, 0.6)
    g = st.grond
    ton = st.dichtstbij((lo + hi) // 2, [g])
    vijf = st.dichtstbij(ton - 5, [(g + 7) % 12])
    figuren = [
        [(0, 0.33, vijf), (0.33, 0.33, vijf), (0.67, 0.33, vijf), (1, 3, ton)],                                   # ta-ta-ta TAAA
        [(0, 0.5, ton - 12), (0.5, 0.5, ton - 8), (1, 0.5, ton - 5), (1.5, 2.5, ton)],                             # arpeggio up
        [(0, 0.5, vijf), (0.5, 0.5, vijf + 2), (1, 0.5, vijf + 4), (1.5, 0.5, vijf + 5), (2, 2, ton)],             # a run up
        [(0, 0.75, ton - 5), (0.75, 0.25, ton - 3), (1, 1, ton - 1), (2, 2, ton)],                                 # a march
        [(0, 0.5, ton + 4), (0.5, 0.5, ton + 2), (1, 0.5, vijf + 4 if vijf + 4 < ton else vijf), (1.5, 2.5, ton)],  # down then home
    ]
    fig = figuren[f.get("figuur", spec["seed"] % len(figuren))]
    # the major third (raised for minor keys: a Picardy-style happy end)
    terts = st.dichtstbij(ton + 4, [(g + 4) % 12])
    for t, d, k in fig:
        k = max(lo - 5, min(hi + 5, k))
        st.noot("lead", 4 + t, d * 0.95, k, 92 if t == 0 else 84)
        st.noot("koper", 4 + t, d * 0.95, k - 12, 70)
    eind = 4 + fig[-1][0]
    st.noot("lead", eind, 4.5, ton, 96)
    st.noot("lead", eind, 4.5, terts, 80)
    for k in (g + 48, g + 52, g + 55, g + 60):
        st.noot("koper", eind, 4.5, k, 76 if not zacht else 52)
        st.noot("str", eind, 5, k + 12, 60)
    for k in (g + 55, g + 59, g + 62):
        st.noot("str", 4, eind - 4, k, 46)   # the V chord under the figure
    st.noot("bas", 4, eind - 4, (g + 7) % 12 + 36, 66)
    st.noot("bas", eind, 4.5, g + 36, 76)
    if not zacht:
        for i in range(8):
            st.noot("pauk", eind - 1 + i * 0.125, 0.15, (g + 7) % 12 + 36, 40 + i * 5)
        st.noot("pauk", eind, 2, g % 12 + 36, 92)
        st.noot("bek", eind, 1, 49, 70)
    for i, k in enumerate((84, 88, 91, 96)):
        st.noot("glo", eind + i * 0.125, 1.5, st.dichtstbij(k, [g, (g + 4) % 12, (g + 7) % 12]), 54)
    # shift everything back by 4 beats (the chord list started at 0 for the V-V-I-I harmony lookups)
    for v in st.sporen.values():
        v[3][:] = [(s - 4, d, k, vv) for s, d, k, vv in v[3]]
    return st
