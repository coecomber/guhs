"""
Op Safari! (1.6.0), slice S6 - all safari sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/safari/:

  dier/<groep>1-3     a cute call per animal group (soorten.json "geluid" says which group a species uses):
                      grote_kat (a soft rolling mini roar + purr), olifant (a little trumpet with a squeaky end), grazer (a
                      snort + soft hum), klein (squeak / chirp), vogel (twitter), groot_vogel (honk / boom), pinguin (a bray),
                      zee (a bubbly wet "mmrrp"), beer (a grumbly "mrrf"), aap (a hoot "oe-oe"), reptiel (a tiny hiss +
                      click), nijlpaard (a deep giggly grunt), geit ("mèh"), ezel ("iiee-aah"), cavia ("wheek")
  sfeer/<zone>        an ambience loop per zone (about 12 s): savanne (32 s: breeze, far doves, a zebra, an elephant), delta (frogs + water), regenwoud
                      (jungle calls + drips), bamboe (wind chimes + leaves), ijskust (icy wind + waves), oertijd (deep wind),
                      europa (forest birds + brook), australie (dry wind + kookaburra-ish laugh), entree (cheerful murmur)
  muziek_lobby        a cheerful safari lobby loop (marimba, shaker, soft bass), about 24 s
  camera              a camera shutter (click + whirr)
  munt                a coin "ting-ting" (Safarimunten)
  truck_motor         an idling safari truck (a putt-putt loop)
  truck_toeter        a cheerful double honk
  kano                a canoe paddle stroke (a splash and a drip)

Not part of make_resources.py (the OGGs are committed, like the other sounds). Needs numpy, scipy and soundfile:
    python tools/features/safari_geluid.py        (from the project root)
    python tools/features/safari_geluid.py --savanne   (round 4: only sfeer/savanne)
The sound events are registered by tools/features/safari_blokken.py (sounds.json) and feature/safari/spullen/SafariGeluid.
"""
import os

import numpy as np

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "safari")
rng = np.random.default_rng(20261020)

DIER_GROEPEN = ["grote_kat", "olifant", "grazer", "klein", "vogel", "groot_vogel", "pinguin", "zee", "beer", "aap", "reptiel",
                "nijlpaard", "geit", "ezel", "cavia"]
ZONES = ["savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie", "entree"]


def _sig():
    import scipy.signal
    return scipy.signal


def band(x, lo, hi, order=2):
    s = _sig()
    b, a = s.butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return s.lfilter(b, a, x)


def low(x, f, order=2):
    s = _sig()
    b, a = s.butter(order, f / (SR / 2), btype="low")
    return s.lfilter(b, a, x)


def high(x, f, order=2):
    s = _sig()
    b, a = s.butter(order, f / (SR / 2), btype="high")
    return s.lfilter(b, a, x)


def t_(dur):
    return np.arange(int(dur * SR)) / SR


def env(n, a=0.01, r=0.1):
    t = np.arange(n) / SR
    d = n / SR
    return np.minimum(1, t / max(a, 1e-4)) * np.minimum(1, np.maximum(0, d - t) / max(r, 1e-4))


def norm(x, peak=0.8):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def save(name, x, peak=0.8, loop=False):
    import soundfile as sf
    path = os.path.join(OUT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if loop:                                       # a seamless loop: crossfade the tail into the head
        k = int(0.4 * SR)
        x = x.copy()
        ramp = np.linspace(0, 1, k)
        x[:k] = x[:k] * ramp + x[-k:] * (1 - ramp)
        x = x[:-k]
    else:
        x = x * env(len(x), 0.005, 0.05)
    data = norm(x, peak).astype(np.float32)
    with sf.SoundFile(path, "w", SR, 1, format="OGG", subtype="VORBIS") as f:   # in blocks: one big write overflows
        for i in range(0, len(data), 8192):                                    # libvorbis' stack on Windows
            f.write(data[i:i + 8192])
    print("wrote", name, f"{len(x) / SR:.2f}s")


def toon(f0, dur, glij=0.0, vib=0.0, vibf=6.0, harm=(1.0, 0.5, 0.25), a=0.01, r=0.1):
    """A sung/played tone with a pitch slide (glij = end factor - 1), vibrato and a few harmonics."""
    t = t_(dur)
    f = f0 * (1 + glij * (t / dur)) * (1 + vib * np.sin(2 * np.pi * vibf * t))
    fase = 2 * np.pi * np.cumsum(f) / SR
    x = sum(w * np.sin(fase * (i + 1)) for i, w in enumerate(harm))
    return x * env(len(t), a, r)


def ruis(dur):
    return rng.standard_normal(int(dur * SR))


def mix(lengte, *delen):
    """delen: (start seconds, signal, gain)."""
    out = np.zeros(int(lengte * SR))
    for s, x, g in delen:
        i = int(s * SR)
        n = min(len(x), len(out) - i)
        if n > 0:
            out[i:i + n] += x[:n] * g
    return out


# --- the animal calls ------------------------------------------------------------------------------------------------------
def dier(groep, v):
    j = 1 + 0.06 * (v - 1)                         # every variation a little higher
    if groep == "grote_kat":
        brul = band(ruis(0.7), 120 * j, 700) * env(int(0.7 * SR), 0.08, 0.35) * (1 + 0.6 * np.sin(2 * np.pi * 22 * t_(0.7)))
        purr = low(ruis(0.9), 180) * (0.6 + 0.4 * np.sin(2 * np.pi * 26 * t_(0.9))) * env(int(0.9 * SR), 0.1, 0.3)
        return mix(1.7, (0, brul, 1.0), (0.7, purr, 0.7), (0.65, toon(520 * j, 0.15, glij=0.4, harm=(1, 0.2)), 0.25))
    if groep == "olifant":
        tr = toon(380 * j, 0.6, glij=0.5, vib=0.02, harm=(1, 0.8, 0.6, 0.4, 0.3)) + 0.3 * band(ruis(0.6), 600, 2400)
        return mix(0.9, (0, tr, 1.0), (0.55, toon(900 * j, 0.22, glij=0.3, harm=(1, 0.3)), 0.5))
    if groep == "grazer":
        snuif = band(ruis(0.25), 300, 1800) * env(int(0.25 * SR), 0.02, 0.15)
        hum = toon(150 * j, 0.5, glij=-0.1, harm=(1, 0.6, 0.3), a=0.05, r=0.2)
        return mix(0.9, (0, snuif, 0.8), (0.3, hum, 0.7))
    if groep == "klein":
        return mix(0.5, (0, toon(1500 * j, 0.09, glij=0.4, harm=(1, 0.2)), 0.8), (0.14, toon(1800 * j, 0.08, glij=0.3, harm=(1, 0.2)), 0.7),
                   (0.27, toon(1650 * j, 0.12, glij=-0.2, harm=(1, 0.2)), 0.6))
    if groep == "vogel":
        delen = [(i * 0.09, toon((2600 + 300 * (i % 3)) * j, 0.07, glij=0.5 if i % 2 else -0.3, harm=(1, 0.1)), 0.6) for i in range(6)]
        return mix(0.7, *delen)
    if groep == "groot_vogel":
        honk = toon(330 * j, 0.25, glij=-0.15, harm=(1, 0.9, 0.7, 0.5)) * (1 + 0.5 * np.sin(2 * np.pi * 40 * t_(0.25)))
        return mix(0.7, (0, honk, 1.0), (0.32, toon(300 * j, 0.25, glij=-0.2, harm=(1, 0.9, 0.7, 0.5)), 0.8))
    if groep == "pinguin":
        delen = [(i * 0.16, toon(420 * j * (1.1 if i % 2 else 0.9), 0.14, harm=(1, 0.8, 0.6, 0.4)) * (1 + 0.4 * np.sin(2 * np.pi * 30 * t_(0.14))), 0.8)
                 for i in range(4)]
        return mix(0.8, *delen)
    if groep == "zee":
        bel = [(0.05 * i + 0.3, toon(600 + 140 * i, 0.05, glij=0.8, harm=(1,)), 0.3) for i in range(5)]
        mm = toon(200 * j, 0.45, glij=0.3, vib=0.03, harm=(1, 0.5), a=0.05, r=0.15)
        return mix(0.8, (0, mm, 1.0), *bel)
    if groep == "beer":
        return mix(0.7, (0, toon(170 * j, 0.4, glij=-0.2, harm=(1, 0.7, 0.5, 0.3)) + 0.2 * low(ruis(0.4), 400), 1.0),
                   (0.42, toon(240 * j, 0.15, glij=0.2, harm=(1, 0.4)), 0.5))
    if groep == "aap":
        return mix(0.9, (0, toon(500 * j, 0.18, glij=0.6, harm=(1, 0.3)), 0.8), (0.25, toon(650 * j, 0.22, glij=0.5, harm=(1, 0.3)), 0.8),
                   (0.55, toon(820 * j, 0.25, glij=-0.3, harm=(1, 0.3)), 0.7))
    if groep == "reptiel":
        sis = high(ruis(0.3), 3500) * env(int(0.3 * SR), 0.02, 0.2)
        klik = np.zeros(int(0.05 * SR))
        klik[:60] = rng.standard_normal(60)
        return mix(0.6, (0, sis, 0.5), (0.35, klik, 0.8), (0.42, klik, 0.6), (0.38, toon(1200 * j, 0.06, harm=(1,)), 0.3))
    if groep == "nijlpaard":
        delen = [(i * 0.17, toon(110 * j * (1 + 0.1 * i), 0.15, harm=(1, 0.8, 0.6, 0.4)) * (1 + 0.6 * np.sin(2 * np.pi * 20 * t_(0.15))), 0.9)
                 for i in range(4)]
        return mix(0.9, *delen)
    if groep == "geit":
        m = toon(560 * j, 0.55, vib=0.06, vibf=11, harm=(1, 0.8, 0.5, 0.3)) + 0.15 * band(ruis(0.55), 1000, 3000)
        return m
    if groep == "ezel":
        return mix(1.2, (0, toon(900 * j, 0.3, glij=0.1, harm=(1, 0.7, 0.4)), 0.8), (0.35, toon(260 * j, 0.5, glij=-0.15, harm=(1, 0.9, 0.7, 0.5)), 1.0),
                   (0.85, toon(880 * j, 0.25, harm=(1, 0.7, 0.4)), 0.6))
    if groep == "cavia":
        return mix(0.7, (0, toon(1900 * j, 0.2, glij=0.5, harm=(1, 0.3)), 0.8), (0.25, toon(2200 * j, 0.25, glij=0.4, harm=(1, 0.3)), 0.8))
    raise ValueError(groep)


# --- ambience loops ------------------------------------------------------------------------------------------------------
def krekels(dur, f=4200, tempo=14.0):
    t = t_(dur)
    puls = (np.sin(2 * np.pi * tempo * t) > 0.6).astype(float)
    return band(ruis(dur), f - 400, f + 400) * puls * 0.4


def vogeltjes(dur, n, lo=2200, hi=4200):
    delen = []
    for _ in range(n):
        s = rng.uniform(0, dur - 0.6)
        f = rng.uniform(lo, hi)
        for k in range(int(rng.integers(2, 5))):
            delen.append((s + 0.1 * k, toon(f * rng.uniform(0.9, 1.15), 0.07, glij=rng.uniform(-0.4, 0.6), harm=(1, 0.1)), rng.uniform(0.1, 0.25)))
    return mix(dur, *delen)


def wind(dur, lo=200, hi=900):
    t = t_(dur)
    golf = 0.6 + 0.4 * np.sin(2 * np.pi * t / dur * 2) * np.sin(2 * np.pi * t / dur * 3 + 1)
    return band(ruis(dur), lo, hi) * golf


def water(dur):
    return low(ruis(dur), 900) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.3 * t_(dur)))


def druppels(dur, n):
    return mix(dur, *[(rng.uniform(0, dur - 0.2), toon(rng.uniform(900, 1600), 0.06, glij=1.0, harm=(1,)), 0.2) for _ in range(n)])


def gonggong(dur, n):
    noten = [523, 659, 784, 880, 1047]
    return mix(dur, *[(rng.uniform(0, dur - 1.6), toon(noten[int(rng.integers(len(noten)))], 1.5, harm=(1, 0.3, 0.2), a=0.002, r=1.4), 0.2)
                      for _ in range(n)])


def savanne(dur=32.0):
    """Round 4 (test round 4: the savanne ambience was "helemaal niks"): a calm, warm savanne. A soft breeze that swells and
    settles, far-away doves and a hornbill-ish two-note call, once a distant zebra bark and once a far elephant rumble,
    and only a faint cricket bed under it all. No music, nothing sharp; a longer loop so it does not feel repetitive."""
    r = np.random.default_rng(20261021)
    t = t_(dur)
    # the breeze: low warm noise, slowly swelling (whole periods over the loop, so it loops cleanly)
    golf = 0.55 + 0.25 * np.sin(2 * np.pi * t / dur * 2) + 0.2 * np.sin(2 * np.pi * t / dur * 5 + 1.3)
    bries = low(band(r.standard_normal(len(t)), 90, 700), 500) * golf
    ruisel = band(r.standard_normal(len(t)), 1500, 4000) * np.maximum(0, golf - 0.6) * 0.25     # grass rustling on the gusts
    # far-away doves ("roe-koe-koe"): soft low hoots, muffled by the distance
    duif = []
    for s0 in (3.0, 15.5, 25.0):
        for k, (f, d) in enumerate(((520, 0.32), (470, 0.22), (470, 0.38))):
            duif.append((s0 + [0, 0.42, 0.72][k], toon(f, d, glij=-0.06, harm=(1, 0.15), a=0.06, r=0.12), 0.11))
    # a hornbill-ish call: two notes, a little higher, twice
    neushoornvogel = [(s0 + 0.28 * k, toon(760 if k % 2 == 0 else 640, 0.18, glij=-0.1, harm=(1, 0.3, 0.1), a=0.02, r=0.08), 0.06)
                      for s0 in (9.0, 21.0) for k in range(4)]
    # little songbirds, few and soft
    vogels = []
    for _ in range(5):
        s0 = r.uniform(1.0, dur - 2.0)
        f = r.uniform(2000, 3000)
        for k in range(int(r.integers(2, 4))):
            vogels.append((s0 + 0.12 * k, toon(f * r.uniform(0.92, 1.1), 0.08, glij=r.uniform(-0.3, 0.5), harm=(1, 0.1), a=0.01, r=0.04), 0.035))
    # a distant zebra bark ("kwa-ha"), heavily muffled
    zebra = mix(1.2, (0, toon(620, 0.16, glij=0.3, harm=(1, 0.8, 0.5)), 1.0), (0.22, toon(540, 0.2, glij=-0.2, harm=(1, 0.8, 0.5)), 0.9),
                (0.48, toon(500, 0.18, glij=-0.2, harm=(1, 0.8, 0.5)), 0.7))
    zebra = low(zebra, 1400)
    # a far elephant rumble: a deep swell, more felt than heard
    olifant = toon(70, 2.6, glij=0.12, vib=0.02, vibf=3, harm=(1, 0.6, 0.3), a=0.8, r=1.2) + 0.2 * low(r.standard_normal(int(2.6 * SR)), 250)
    # the faint cricket bed (slow, soft pulses, never the old harsh hiss)
    puls = (0.5 + 0.5 * np.sin(2 * np.pi * 3.0 * t)) ** 6
    krekel = band(r.standard_normal(len(t)), 3800, 4400) * puls * 0.05
    return (bries * 0.9 + ruisel + krekel
            + mix(dur, *duif) + mix(dur, *neushoornvogel) + mix(dur, *vogels)
            + mix(dur, (12.0, zebra, 0.07), (27.0, olifant, 0.22)))


def sfeer(zone, dur=12.0):
    if zone == "savanne":
        return savanne()
    if zone == "delta":
        kwak = mix(dur, *[(rng.uniform(0, dur - 0.3), toon(rng.uniform(220, 320), 0.18, harm=(1, 0.8, 0.5)) * (1 + 0.8 * np.sin(2 * np.pi * 30 * t_(0.18))), 0.25)
                          for _ in range(10)])
        return water(dur) * 0.4 + kwak + vogeltjes(dur, 3) * 0.4
    if zone == "regenwoud":
        roep = mix(dur, *[(rng.uniform(0, dur - 0.8), toon(rng.uniform(600, 1100), 0.6, glij=rng.uniform(-0.5, 0.7), vib=0.03, harm=(1, 0.3)), 0.12)
                          for _ in range(6)])
        return krekels(dur, 5200, 20) * 0.5 + roep + druppels(dur, 25) + vogeltjes(dur, 6, 2500, 4500) * 0.5
    if zone == "bamboe":
        return wind(dur, 300, 1400) * 0.4 + gonggong(dur, 7) + vogeltjes(dur, 2) * 0.3
    if zone == "ijskust":
        golven = low(ruis(dur), 500) * (0.5 + 0.5 * np.sin(2 * np.pi * t_(dur) / 4.0) ** 2)
        return wind(dur, 400, 2000) * 0.6 + golven * 0.6
    if zone == "oertijd":
        return wind(dur, 80, 400) * 0.9 + wind(dur, 1200, 2600) * 0.15
    if zone == "europa":
        beek = band(ruis(dur), 600, 3000) * 0.25
        return beek + vogeltjes(dur, 9, 2400, 4800) * 0.8 + wind(dur, 200, 700) * 0.1
    if zone == "australie":
        lach = mix(dur, *[(s + 0.11 * k, toon(900 + 120 * (k % 3), 0.09, glij=0.4, harm=(1, 0.5)), 0.2) for s in (2.0, 8.0) for k in range(8)])
        return wind(dur, 200, 900) * 0.5 + krekels(dur, 3600, 10) * 0.4 + lach
    if zone == "entree":
        gemompel = band(ruis(dur), 300, 1200) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.7 * t_(dur)))
        return gemompel * 0.3 + vogeltjes(dur, 4) * 0.5 + krekels(dur, 4000, 8) * 0.2
    raise ValueError(zone)


# --- music and effects ---------------------------------------------------------------------------------------------------
def marimba(f, dur=0.45):
    t = t_(dur)
    return (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * 4 * f * t) * np.exp(-t * 30)) * np.exp(-t * 7)


def muziek():
    bpm = 120
    tel = 60 / bpm
    maten = 12                                      # 12 bars of 4 = 24 s
    akkoorden = [(392, 494, 587), (440, 523, 659), (349, 440, 523), (392, 494, 587)]   # G Am F G
    melodie = [784, 880, 988, 880, 784, 659, 587, 659, 784, 659, 587, 523, 587, 659, 784, 880]
    dur = maten * 4 * tel
    delen = []
    for m in range(maten):
        ak = akkoorden[m % 4]
        for b in range(4):
            s = (m * 4 + b) * tel
            delen.append((s, toon(ak[0] / 4, tel * 0.9, harm=(1, 0.4), a=0.01, r=0.2), 0.35))          # bass
            delen.append((s + tel / 2, sum(marimba(f, 0.3) for f in ak) / 3, 0.25))                 # off-beat chord
            sh = band(ruis(0.06), 5000, 9000) * np.exp(-t_(0.06) * 60)
            delen.append((s, sh, 0.15))
            delen.append((s + tel / 2, sh, 0.1))
        for k in range(4):                         # the tune: 2 notes per beat in the even bars
            if m % 2 == 0:
                n = melodie[(m * 2 + k) % len(melodie)]
                delen.append(((m * 4 + k) * tel, marimba(n), 0.5))
                delen.append(((m * 4 + k + 0.5) * tel, marimba(n * 1.122), 0.3))
    return mix(dur + 0.4, *delen)


def camera():
    klik = high(ruis(0.03), 2000) * np.exp(-t_(0.03) * 120)
    whir = band(ruis(0.35), 900, 2500) * (0.5 + 0.5 * np.sin(2 * np.pi * 60 * t_(0.35))) * env(int(0.35 * SR), 0.02, 0.1)
    return mix(0.5, (0, klik, 1.0), (0.04, klik, 0.7), (0.1, whir, 0.25))


def munt():
    return mix(0.6, (0, toon(1568, 0.25, harm=(1, 0.3, 0.2), a=0.001, r=0.22), 0.8), (0.1, toon(2093, 0.4, harm=(1, 0.3, 0.2), a=0.001, r=0.38), 0.8))


def truck_motor(dur=3.0):
    t = t_(dur)
    puf = (np.sin(2 * np.pi * 14 * t) > 0.2).astype(float)
    return low(ruis(dur), 260) * (0.4 + 0.6 * puf) + toon(55, dur, harm=(1, 0.6, 0.4, 0.2), a=0.001, r=0.001) * 0.4


def truck_toeter():
    tt = toon(440, 0.18, harm=(1, 0.8, 0.6, 0.4)) + toon(554, 0.18, harm=(1, 0.8, 0.6, 0.4))
    return mix(0.55, (0, tt, 1.0), (0.26, tt, 1.0))


def kano():
    plons = band(ruis(0.4), 300, 3000) * env(int(0.4 * SR), 0.02, 0.3)
    return mix(1.0, (0, plons, 1.0), *[(0.45 + 0.12 * k, toon(rng.uniform(1000, 1700), 0.05, glij=0.8, harm=(1,)), 0.2) for k in range(3)])


def main():
    import sys
    if "--savanne" in sys.argv:            # round 4: only the new savanne ambience (the other OGGs stay as they are)
        save("sfeer/savanne", savanne(), 0.45, loop=True)
        return
    for g in DIER_GROEPEN:
        for v in (1, 2, 3):
            save(f"dier/{g}{v}", dier(g, v), 0.7)
    for z in ZONES:
        save(f"sfeer/{z}", sfeer(z), 0.5, loop=True)
    save("muziek_lobby", muziek(), 0.6, loop=True)
    save("camera", camera(), 0.8)
    save("munt", munt(), 0.6)
    save("truck_motor", truck_motor(), 0.5, loop=True)
    save("truck_toeter", truck_toeter(), 0.7)
    save("kano", kano(), 0.6)


if __name__ == "__main__":
    main()
