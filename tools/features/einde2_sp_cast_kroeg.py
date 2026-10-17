"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.2), slice sp_cast - the Mikas of De Ruige Kroeg as GuhNpcEntity Kinds: the barman (an
apron, a tattoo "MAM", a mug he never stops polishing), the uitsmijter (huge, sunglasses, a gold chain), two stoere Mikas
(a leather vest with studs and tattoos; a denim vest, a cap turned back and an eye patch) and the slapende Mika (snoring,
the key on his belt). Every one stays a Mika at a glance (the kit of einde2_cast_schurk_blad, read-only).

Animations (one file per Kind, animation.guh_sitting.<name>): idle (each with its own signature), praat (24 ticks),
kijk_op (a pose: the stand-still game's look: up on its paws, ears up, two big staring eyes) and slaap (a pose: head down,
z's floating up). The slapende Mika's idle IS his sleep.

  KINDS       kind -> (Dutch name, [(animation, ticks; 0 = a pose that lasts)]): the same table as the Java enum
              nl.juiced.guhs.feature.einde2.spcast.KroegMikas (check() compares the two)
  build(h)    geckolib/models/entity/guh_npc_<kind>.geo.json, textures/entity/npc_<kind>.png,
              geckolib/animations/entity/guh_npc_<kind>.animation.json
  check(h)    the files, the animation names, every animated bone, the Java table
"""
import json
import os
import re

import numpy as np

from features import boerderij_dieren as bd
from features.einde2_cast_schurk_blad import (GOUD, GRIJS, GRIJS_LICHT, ROOD, WIT, WEG, Blad, anim, disc, effen, golf, kf, metaal, met, snuit,
                                              stof)
from features.einde2_cast_schurk_mikas import harnas, mika_idle, namen, poot_opzij, praat

P = "animation.guh_sitting."
ANIMS = [("praat", 24), ("kijk_op", 0), ("slaap", 0)]
KINDS = {
    "kroeg_barman_mika": ("Barman-Mika", ANIMS),
    "kroeg_uitsmijter_mika": ("Uitsmijter-Mika", ANIMS),
    "stoere_mika": ("Stoere Mika", ANIMS),
    "stoere_mika_2": ("Stoere Mika", ANIMS),
    "slapende_mika": ("Slapende Mika", ANIMS),
}
LEER = (46, 36, 34)
LEER_LICHT = (92, 74, 66)
DENIM = (64, 92, 150)
DENIM_LICHT = (110, 140, 196)
TATOEAGE = (60, 70, 150)
ZWART = (24, 22, 28)

# 3 x 5 pixel letters for the tattoo
LETTERS = {"M": ["X.X", "XXX", "XXX", "X.X", "X.X"], "A": [".X.", "X.X", "XXX", "X.X", "X.X"]}


def tattoo(W, H, rng, a, tekst="MAM", hart=True):
    """A heart with a banner and the word on it, in old blue ink and red."""
    if hart:
        disc(a, W * 0.36, H * 0.32, W * 0.16, ROOD)
        disc(a, W * 0.64, H * 0.32, W * 0.16, ROOD)
        for y in range(int(H * 0.32), int(H * 0.8)):
            half = W * 0.32 * (1 - (y - H * 0.32) / (H * 0.48))
            a[y, int(W / 2 - half):int(W / 2 + half) + 1, :3] = ROOD
    by = int(H * 0.42)
    a[by - 1:by + 7, int(W * 0.08):int(W * 0.92), :3] = (236, 226, 200)
    x = int(W / 2 - (len(tekst) * 4 - 1) / 2)
    for ch in tekst:
        for r, rij in enumerate(LETTERS[ch]):
            for c, v in enumerate(rij):
                if v == "X":
                    a[by + r, x + c, :3] = TATOEAGE
        x += 4


def huid_met(tekening, kleur):
    """A patch of the Mika's skin with something drawn on it."""
    def paint(W, H, rng):
        a = bd.plain(kleur, 6)(W, H, rng).astype(np.float32)
        tekening(W, H, rng, a)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def open_ogen(een=False):
    """The stand-still game's look: two (or one) huge staring Mika eyes on a see-through plate in front of the face."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        for sx in ((1,) if een else (-1, 1)):
            cx, cy, r = W / 2 + sx * W * 0.24, H * 0.42, W * 0.17
            disc(a, cx, cy, r + 1.2, (48, 14, 34))
            disc(a, cx, cy, r, WIT)
            disc(a, cx, cy, r * 0.62, (232, 44, 40))
            a[int(cy - r * 0.55):int(cy + r * 0.55), int(cx - 0.8):int(cx + 0.8), :3] = (20, 8, 10)
            disc(a, cx + r * 0.3, cy - r * 0.35, r * 0.2, WIT)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def letter_z(W, H, rng):
    a = np.zeros((H, W, 4), np.uint8)
    t = max(1, H // 5)
    a[:t, :, :] = (250, 250, 255, 255)
    a[H - t:, :, :] = (250, 250, 255, 255)
    for y in range(H):
        x = int((1 - y / max(1, H - 1)) * (W - t))
        a[y, x:x + t + 1, :] = (250, 250, 255, 255)
    return a


def ogen_bot(b, een=False):
    """The bone of the staring eyes (only shown in kijk_op)."""
    b.bot("kroeg_ogen", "head", [0, 7, -12], [b.kubus([-7.0, 1.5, -12.25], [14.0, 11.0, 0.1], open_ogen(een), alleen=("north",))])


def zzz_bot(b):
    """Three z's that float up from the head (only in slaap)."""
    cubes = [b.kubus([3.0 + 1.6 * i, 15.0 + 2.6 * i, -8.0], [1.4 + 0.5 * i, 1.4 + 0.5 * i, 0.1], letter_z, alleen=("north", "south")) for i in range(3)]
    b.bot("kroeg_zzz", "head", [4.0, 15.0, -8.0], cubes)


def standaard_anims(idle, extra_praat=None, slaapt=False, een_oog=False):
    """idle (given), praat, kijk_op, slaap: what every Kroeg Mika has."""
    verstop = {"kroeg_ogen": {"scale": WEG}, "kroeg_zzz": {"scale": WEG}}
    for k, v in verstop.items():
        idle["bones"].setdefault(k, v)
    L = 2.0
    kijk = {"body": {"position": [0, 1.2, 0]}, "head": {"position": kf([(0, [0, 1.6, -0.6]), (1.0, [0, 1.8, -0.6]), (L, [0, 1.6, -0.6])])},
            "ear_left": {"rotation": [0, 0, -22]}, "ear_right": {"rotation": [0, 0, 22]}, "tail": {"rotation": [20, 0, 0]},
            "leg_front_left": {"position": [0, 0.6, 0]}, "leg_front_right": {"position": [0, 0.6, 0]},
            "kroeg_ogen": {"scale": [1, 1, 1]}, "kroeg_zzz": {"scale": WEG}}
    if een_oog:                                                   # the sleeper only half wakes: head stays low
        kijk["head"] = {"position": [0, -0.6, -0.4]}
        kijk["body"] = {"position": [0, 0, 0]}
    L2 = 4.0
    zzz = {"position": kf([(0, [0, 0, 0]), (L2, [1.5, 4.0, 0])]),
           "scale": kf([(0, [0.4, 0.4, 0.4]), (L2 * 0.25, [1, 1, 1]), (L2 * 0.85, [1, 1, 1]), (L2, [0.2, 0.2, 0.2])])}
    slaap = {"head": {"position": [0, -2.4, -0.6], "rotation": [0, 0, 8]},
             "body": {"scale": kf([(0, [1, 1, 1]), (L2 / 2, [1.04, 1.06, 1.03]), (L2, [1, 1, 1])])},
             "ear_left": {"rotation": [0, 0, 40]}, "ear_right": {"rotation": [0, 0, -40]}, "tail": {"rotation": [-6, 0, 0]},
             "kroeg_zzz": zzz, "kroeg_ogen": {"scale": WEG}}
    A = {"idle": idle, "praat": praat(dict(verstop, **(extra_praat or {}))), "kijk_op": anim(L, kijk), "slaap": anim(L2, slaap)}
    if slaapt:
        A["idle"] = anim(L2, dict(slaap))
    return A


# =====================================================================================================================
def kroeg_barman_mika(h):
    b = Blad(h, "kroeg_barman_mika")
    snuit(b, brauw="dik", mond="zuur", lid=0.18)

    def vlekken(W, H, rng, a):
        for _ in range(max(2, W * H // 260)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.2, 2.6), (196, 170, 120), 0.7)
    schort = met(stof(WIT, var=4), vlekken)
    harnas(b, "barman_hemd", stof((150, 60, 50), strepen=((120, 44, 40), 6)), boven=stof((150, 60, 50)))
    b.bot("barman_schort", "body", [0, 5, -2], [b.kubus([-5.2, 0.4, -2.4], [10.4, 8.8, 0.6], schort)])
    b.bot("barman_slab", "head", [0, 1, -12], [b.kubus([-4.6, 0.0, -12.9], [9.2, 2.4, 0.8], schort)])
    # the tattoo "MAM" in a heart on his left side
    b.bot("barman_tattoo", "body", [7.1, 5, 4], [b.kubus([7.05, 2.2, 1.0], [0.2, 6.0, 7.0], effen(b.vacht, 4),
                                                         overrides={"east": huid_met(tattoo, b.vacht)}, alleen=("east",))])
    # the mug he polishes, in the lifted paw, and a towel over it
    px, py, pz = poot_opzij(b)
    glas, bier, schuim = effen((210, 230, 240), 4), effen((176, 110, 40), 6), effen((250, 246, 236), 4)
    b.bot("barman_mok", "leg_front_right", [px, py, pz], [
        b.kubus([px - 1.6, py, pz - 1.6], [3.2, 4.0, 3.2], glas, overrides={"north": bier, "east": bier}),
        b.kubus([px - 1.7, py + 4.0, pz - 1.7], [3.4, 1.0, 3.4], schuim),
        b.kubus([px + 1.6, py + 1.0, pz - 0.4], [1.2, 2.2, 0.8], glas),
        b.kubus([px - 2.2, py + 2.2, pz - 2.2], [1.0, 2.6, 4.4], effen((240, 240, 236), 5))])
    ogen_bot(b)
    zzz_bot(b)
    L = 8.0
    idle = mika_idle(L, staart=5, kop=1.0)
    # the signature: he polishes the mug, round and round
    idle["bones"]["leg_front_right"] = {"position": kf([(i * 0.5, [0.8 * np.cos(i * np.pi / 2), 0.4 + 0.4 * np.sin(i * np.pi / 2), 0]) for i in range(17)])}
    return b, standaard_anims(idle)


def kroeg_uitsmijter_mika(h):
    b = Blad(h, "kroeg_uitsmijter_mika", tint=(0, 0.95, 0.88))
    snuit(b, brauw="boos", mond="recht", lid=0.3)
    harnas(b, "uitsmijter_shirt", stof(ZWART, var=4), boven=stof((40, 38, 44), var=4))
    # the sunglasses, a band round the head
    bril = metaal(ZWART, (90, 90, 110))
    b.bot("uitsmijter_bril", "head", [0, 8, -12], [
        b.kubus([-6.6, 6.4, -12.65], [5.6, 3.0, 0.6], bril), b.kubus([1.0, 6.4, -12.65], [5.6, 3.0, 0.6], bril),
        b.kubus([-1.0, 8.0, -12.6], [2.0, 0.8, 0.5], bril), b.kubus([-8.1, 7.6, -12.4], [16.2, 0.8, 6.0], effen(ZWART, 3))])
    goud = metaal(GOUD, (255, 240, 170))
    b.bot("uitsmijter_ketting", "head", [0, 1, -12], [b.kubus([-4.6, -0.4, -12.85], [9.2, 0.8, 0.7], goud),
                                                      b.kubus([-1.0, -1.8, -12.9], [2.0, 1.6, 0.7], goud)])
    ogen_bot(b)
    zzz_bot(b)
    L = 10.0
    idle = mika_idle(L, staart=3, kop=0.5)
    # the signature: arms (front paws) planted wide, he rolls his shoulders once
    idle["bones"]["body"] = {"scale": kf([(0, [1, 1, 1]), (6.0, [1, 1, 1]), (6.5, [1.08, 1.04, 1.02]), (7.2, [1, 1, 1]), (L, [1, 1, 1])])}
    return b, standaard_anims(idle)


def _vest(paint, studs=True):
    def knopjes(W, H, rng, a):
        if studs:
            for x in range(3, W - 2, 6):
                disc(a, x, 3, 1.1, GRIJS_LICHT)
                disc(a, x, H - 3, 1.1, GRIJS_LICHT)
    return met(paint, knopjes)


def stoere_mika(h):
    b = Blad(h, "stoere_mika")
    snuit(b, brauw="boos", mond="grijns", lid=0.12)
    harnas(b, "stoer_vest", _vest(stof(LEER, var=5)), boven=_vest(stof(LEER_LICHT, var=5)))

    def anker(W, H, rng, a):
        cx = W / 2
        a[int(H * 0.15):int(H * 0.8), int(cx - 1):int(cx + 1), :3] = TATOEAGE
        a[int(H * 0.25):int(H * 0.25) + 2, int(W * 0.3):int(W * 0.7), :3] = TATOEAGE
        a[int(H * 0.72):int(H * 0.8), int(W * 0.25):int(W * 0.75), :3] = TATOEAGE
    b.bot("stoer_tattoo", "head", [-8, 6, -6], [b.kubus([-8.05, 3.0, -9.0], [0.1, 6.0, 5.0], effen(b.vacht, 4),
                                                       overrides={"west": huid_met(anker, b.vacht)}, alleen=("west",))])
    b.bot("stoer_bandana", "head", [0, 12, -6], [b.kubus([-8.3, 10.8, -12.3], [16.6, 2.2, 12.6], stof((120, 40, 140), zoom=WIT, zoom_dik=0.18), k=0.5),
                                                 b.kubus([-1.2, 9.4, 0.1], [2.4, 2.6, 1.0], stof((120, 40, 140)))])
    b.bot("stoer_polsband", "leg_front_left", [4, 1, -11], [b.kubus([2.0, 1.2, -13.7], [4.0, 0.8, 3.9], _vest(stof(LEER, var=4)))])
    ogen_bot(b)
    zzz_bot(b)
    L = 12.0
    idle = mika_idle(L, staart=10, kop=1.5)
    # the signature: he cracks his knuckles (a front paw taps twice)
    idle["bones"]["leg_front_left"] = {"position": kf([(0, [0, 0, 0]), (7.0, [0, 0, 0]), (7.2, [0, 1.2, 0]), (7.4, [0, 0, 0]), (7.6, [0, 1.2, 0]), (7.8, [0, 0, 0])])}
    return b, standaard_anims(idle)


def stoere_mika_2(h):
    b = Blad(h, "stoere_mika_2", tint=(-0.04, 1.0, 0.96))
    snuit(b, brauw="dik", mond="grijns", lid=0.2)
    harnas(b, "stoer2_vest", _vest(stof(DENIM, var=6), studs=False), boven=stof(DENIM_LICHT, var=5))
    pet = stof((200, 60, 50), var=5)
    b.bot("stoer2_pet", "head", [0, 13, -6], [b.kubus([-6.0, 13.4, -10.5], [12.0, 2.4, 9.0], pet, k=0.5),
                                              b.kubus([-3.2, 13.4, -1.6], [6.4, 0.6, 3.6], pet)])
    b.bot("stoer2_ooglap", "head", [0, 8, -12], [b.kubus([1.4, 5.6, -12.6], [4.6, 4.0, 0.5], effen(ZWART, 3)),
                                                 b.kubus([-8.1, 8.6, -12.4], [16.2, 0.6, 6.0], effen(ZWART, 3))])

    def hart(W, H, rng, a):
        disc(a, W * 0.38, H * 0.4, W * 0.18, ROOD)
        disc(a, W * 0.62, H * 0.4, W * 0.18, ROOD)
        for y in range(int(H * 0.4), int(H * 0.85)):
            half = W * 0.34 * (1 - (y - H * 0.4) / (H * 0.45))
            a[y, int(W / 2 - half):int(W / 2 + half) + 1, :3] = ROOD
    b.bot("stoer2_tattoo", "head", [8, 6, -6], [b.kubus([7.95, 3.0, -9.0], [0.1, 5.0, 5.0], effen(b.vacht, 4),
                                                      overrides={"east": huid_met(hart, b.vacht)}, alleen=("east",))])
    ogen_bot(b, een=False)
    zzz_bot(b)
    L = 12.0
    idle = mika_idle(L, staart=8, kop=2.0)
    # the signature: he nods along to music only he hears
    idle["bones"]["head"] = {"rotation": golf(2, 2.0, L, n=8), "position": kf([(i * 0.5, [0, 0.5 if i % 2 else 0, 0]) for i in range(25)])}
    return b, standaard_anims(idle)


def slapende_mika(h):
    b = Blad(h, "slapende_mika", tint=(0.03, 0.9, 0.95))
    snuit(b, ogen="dicht", brauw="blij", mond="o")
    harnas(b, "slaper_hemd", stof((120, 130, 90), strepen=((96, 104, 70), 7)), boven=stof((120, 130, 90)))
    muts = stof((70, 110, 170), strepen=((240, 240, 240), 4))
    b.bot("slaper_muts", "head", [0, 12, -6], [b.kubus([-7.0, 11.6, -11.0], [14.0, 3.2, 10.0], muts, k=0.5),
                                               b.kubus([-1.4, 14.8, -7.4], [2.8, 2.8, 2.8], effen(WIT, 6))])
    # the belt with THE key (what the game is about): a big brass key hanging at his side
    riem = effen((90, 60, 40), 6)
    sleutel = metaal((214, 170, 60), (255, 230, 150))
    b.bot("slaper_riem", "body", [0, 3, 5], [b.kubus([-6.8, 2.6, -2.3], [13.6, 1.2, 13.6], riem, k=0.5)])
    b.bot("slaper_sleutel", "slaper_riem", [6.9, 3.2, 3.0], [
        b.kubus([6.9, 0.6, 2.4], [0.5, 2.6, 1.2], sleutel), b.kubus([6.9, -0.4, 1.4], [0.5, 1.6, 3.2], sleutel),
        b.kubus([6.9, -1.4, 2.6], [0.5, 1.0, 0.8], sleutel), b.kubus([6.9, -2.4, 2.6], [0.5, 1.0, 2.0], sleutel)])
    ogen_bot(b, een=True)
    zzz_bot(b)
    A = standaard_anims(None or {"bones": {}, "loop": True, "animation_length": 4.0}, slaapt=True, een_oog=True)
    # the key swings with every snore
    for n in ("idle", "slaap"):
        A[n]["bones"]["slaper_sleutel"] = {"rotation": golf(0, 8, 4.0)}
    return b, A


MAKERS = {"kroeg_barman_mika": kroeg_barman_mika, "kroeg_uitsmijter_mika": kroeg_uitsmijter_mika, "stoere_mika": stoere_mika,
          "stoere_mika_2": stoere_mika_2, "slapende_mika": slapende_mika}


def maak(h, kind):
    blad, anims = MAKERS[kind](h)
    return blad, {"format_version": "1.8.0", "animations": namen(anims)}


def build(h):
    for kind in KINDS:
        blad, anims = maak(h, kind)
        blad.schrijf()
        h.w(f"{h.A}/geckolib/animations/entity/guh_npc_{kind}.animation.json", anims)


def _java():
    pad = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "spcast", "KroegMikas.java")
    return open(pad, encoding="utf-8").read() if os.path.exists(pad) else None


def check(h):
    problems = []
    for kind, (_naam, anims) in KINDS.items():
        try:
            g = json.load(open(f"{h.A}/geckolib/models/entity/guh_npc_{kind}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
            a = json.load(open(f"{h.A}/geckolib/animations/entity/guh_npc_{kind}.animation.json", encoding="utf-8"))["animations"]
        except OSError as e:
            problems.append(str(e))
            continue
        botten = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in botten:
                problems.append(f"{kind}: bone {b['name']} has a missing parent")
        for n in ["idle"] + [n for n, _ in anims]:
            if P + n not in a:
                problems.append(f"{kind}: no animation {n}")
        for an, inhoud in a.items():
            for bn in inhoud.get("bones", {}):
                if bn not in botten:
                    problems.append(f"{kind}: animation {an} moves a missing bone {bn}")
        if not os.path.exists(f"{h.A}/textures/entity/npc_{kind}.png"):
            problems.append(f"{kind}: no texture")
    java = _java()
    if java is None:
        problems.append("KroegMikas.java is missing")
    else:
        for kind, (_naam, anims) in KINDS.items():
            m = re.search(r"^\s*" + kind.upper() + r"\((.*?)\)[,;]\s*$", java, re.M)
            wil = ", ".join(f'"{n}:{t}"' for n, t in anims)
            if not m:
                problems.append(f"KroegMikas.java has no row {kind.upper()}")
            elif " ".join(m.group(1).split()) != wil:
                problems.append(f"KroegMikas.{kind.upper()} says ({m.group(1)}), the python table ({wil})")
    return problems
