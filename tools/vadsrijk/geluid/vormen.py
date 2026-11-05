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
    st = Stuk(spec["naam"], spec["toon"], spec["modus"], spec["bpm"], spec["maat"], spec.get("samengesteld", False), spec["seed"])
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


def baas(spec):
    st = Stuk(spec["naam"], spec["toon"], spec["modus"], spec["bpm"], spec["maat"], spec.get("samengesteld", False), spec["seed"], lus=True)
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
    return st
