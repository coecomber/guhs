"""
bbq2 (guhrio-w3) - the box models of world 3 of Super Guhrio, made with the engine's model builder
(features/guhrio_modellen.Model: boxes in pixels, the front is +z, +x is the creature's own left):

  grote_nether_mika   the boss: a big dusky-pink Mika with bone horns, a red mane and brows, glowing eyes, a cream belly,
                      a spiked collar and bracelets, and a green shell full of bone spikes on his back (drawn 1.6 times
                      its pixels by client/GuhrioW3Client.java). Parts: voet_l, voet_r, staart, lijf, schild, arm_l, arm_r,
                      hoofd (ears, horns, mane, collar), gezicht_boos / gezicht_mok / gezicht_blij (the head box with one
                      of three faces, and the snout), bol (the shell he tucks into: a spiked ball round the origin).
  guhriow3_kooltje    a lump of glowing coal (part kool)
  guhriow3_taart      the cake of Prinses Perzikguh: a plate (bord) and four quarters (punt_0 .. punt_3), a cherry on each

`python tools/features/guhrio_w3_modellen.py <dir>` (from the repo root, after the generators ran) renders preview
pictures of all of them from the files on disk.
"""
import os

import numpy as np

from features import guhrio_modellen as gm

NAMEN = ("grote_nether_mika", "guhriow3_kooltje", "guhriow3_taart")

VACHT, VACHT_DONKER, VACHT_LICHT = (176, 100, 112), (128, 66, 80), (208, 136, 146)
BUIK, BUIK_LIJN = (240, 218, 168), (206, 172, 116)
BOT, BOT_DONKER = (240, 234, 208), (190, 178, 146)
MANEN, MANEN_LICHT, MANEN_DONKER = (226, 58, 40), (255, 128, 66), (150, 28, 26)
SCHILD, SCHILD_LICHT, SCHILD_DONKER = (52, 150, 64), (136, 220, 126), (20, 92, 40)
ZWART, WIT, GLOED, NEUS, TONG = (30, 18, 26), (255, 255, 255), (255, 156, 40), (112, 44, 60), (236, 96, 120)
KLAUW = (250, 246, 232)


def _rijen(a, y0, y1, rgb):
    a[y0:y1, :, :3] = rgb


def _vacht(seed):
    """Fur: a little darker along the bottom."""
    return gm.met(gm.kleur(VACHT, 5, 200 + seed), lambda a: _rijen(a, a.shape[0] - 1, a.shape[0], VACHT_DONKER))


def _platen(a):
    """The plates of the shell: a honeycomb of dark seams, light on top of every plate."""
    h, w = a.shape[:2]
    for y in range(h):
        for x in range(w):
            rij = y // 4
            if y % 4 == 3 or (x + rij * 3) % 6 == 5:
                a[y, x, :3] = SCHILD_DONKER
            elif y % 4 == 0 and (x + rij * 3) % 6 in (1, 2):
                a[y, x, :3] = SCHILD_LICHT


def _stekel(m, deel, x, y, z, richting, groot=3.0):
    """A bone spike: a base and a tip, pointing along `richting` (a unit axis vector)."""
    dx, dy, dz = richting
    bot = gm.kleur(BOT, 3, 240, licht=WIT, donker=BOT_DONKER)
    h = groot / 2
    lang = 2.2

    def doos(a0, a1, half, schilder):
        van = [x - half, y - half, z - half]
        tot = [x + half, y + half, z + half]
        for i, d in enumerate((dx, dy, dz)):
            if d:
                van[i], tot[i] = sorted(((x, y, z)[i] + d * a0, (x, y, z)[i] + d * a1))
        m.doos(deel, tuple(van), tuple(tot), schilder)
    doos(0, lang, h, bot)
    doos(lang, lang + 1.6, h * 0.5, gm.kleur(WIT, 2, 241, donker=BOT))


GEZICHT = {
    # 16 wide, 11 high: the head's front. r brow, w eye white, o glowing iris, z pupil, d dark fur shade
    "boos": ["................",
             ".rr..........rr.",
             ".rrrr......rrrr.",
             "..rrrrr..rrrrr..",
             "..wwwrr..rrwww..",
             "..wozww..wwzow..",
             "..wwwww..wwwww..",
             "................",
             "................",
             "................",
             "................"],
    "mok": ["................",
            "................",
            "....rrr..rrr....",
            "..rrrr....rrrr..",
            ".rr..........rr.",
            "..zzzzz..zzzzz..",
            "..wwozw..wwozw..",
            "................",
            "................",
            "................",
            "................"],
    "blij": ["................",
             "..rrrr....rrrr..",
             ".rr..........rr.",
             "................",
             "...zzz....zzz...",
             "..z...z..z...z..",
             "................",
             ".pp..........pp.",
             ".pp..........pp.",
             "................",
             "................"],
}
SNUIT = {
    # 10 wide, 6 high: the snout's front. n nose, m mouth, t tooth, g tongue
    "boos": ["...nnnn...",
             "..........",
             "mmmmmmmmmm",
             "mtmmtmtmtm",
             "mtmmmmmmtm",
             ".mmmmmmmm."],
    "mok": ["...nnnn...",
            "..........",
            "..........",
            "...mmmm...",
            "..m....m..",
            ".........."],
    "blij": ["...nnnn...",
             "..........",
             "mmmmmmmmmm",
             "mtmtmmtmtm",
             ".mggggggm.",
             "..mmmmmm.."],
}
KLEUREN = {"r": MANEN, "w": WIT, "o": GLOED, "z": ZWART, "n": NEUS, "m": ZWART, "t": KLAUW, "g": TONG, "p": (236, 130, 150)}


def grote_nether_mika(h):
    m = gm.Model("grote_nether_mika", 256, 256)
    # --- feet: three claws each ---
    klauwen = gm.patroon(["w.ww.ww"], {"w": KLAUW}, 0, 3)
    voet = gm.kleur(VACHT_DONKER, 4, 201)
    m.doos("voet_l", (2, 0, -4), (9, 4, 6), voet, voor=gm.met(voet, klauwen))
    m.doos("voet_r", (-9, 0, -4), (-2, 4, 6), voet, voor=gm.met(voet, klauwen))
    # --- the body: fur, a cream belly of plates on the front ---
    def buik(a):
        hh, ww = a.shape[:2]
        a[1:hh - 1, 3:ww - 3, :3] = BUIK
        for y in range(4, hh - 1, 3):
            a[y, 3:ww - 3, :3] = BUIK_LIJN
        a[1:hh - 1, 3, :3] = BUIK_LIJN
        a[1:hh - 1, ww - 4, :3] = BUIK_LIJN
    m.doos("lijf", (-9, 4, -6), (9, 17, 5), _vacht(2), voor=gm.met(_vacht(3), buik), boven=gm.kleur(VACHT_DONKER, 4, 204))
    m.doos("staart", (-2.5, 4, -17), (2.5, 8, -11), _vacht(5))
    _stekel(m, "staart", 0, 6, -17, (0, 0, -1), 2.4)
    # --- the shell on his back: a cream rim, a green dome of plates, bone spikes ---
    rand = gm.kleur(BUIK, 3, 206, licht=(255, 246, 214), donker=BUIK_LIJN)
    koepel = gm.met(gm.kleur(SCHILD, 5, 207), _platen)
    m.doos("schild", (-11, 5, -12.5), (11, 8, -5), rand)
    m.doos("schild", (-10, 8, -13.5), (10, 19.5, -6), koepel)
    m.doos("schild", (-7, 19.5, -12.5), (7, 21.5, -7), gm.met(gm.kleur(SCHILD, 5, 208), _platen))
    for sx in (-6, 0, 6):
        for sy in (11, 16.5):
            _stekel(m, "schild", sx, sy, -13.5, (0, 0, -1))
    for sx in (-4, 4):
        _stekel(m, "schild", sx, 21.5, -9.5, (0, 1, 0))
    for sx in (-1, 1):
        _stekel(m, "schild", sx * 10, 14, -9.5, (sx, 0, 0))
    # --- arms: a black band with bone studs at the wrist, claws ---
    def band(a):
        hh, ww = a.shape[:2]
        a[hh - 4:hh - 2, :, :3] = ZWART
        for x in range(1, ww, 3):
            a[hh - 4:hh - 2, x, :3] = BOT
        a[hh - 1, ::2, :3] = KLAUW
    arm = gm.met(_vacht(9), band)
    m.doos("arm_l", (9, 8, -3), (13, 16, 3), arm, boven=gm.kleur(VACHT_DONKER, 4, 210))
    m.doos("arm_r", (-13, 8, -3), (-9, 16, 3), arm, boven=gm.kleur(VACHT_DONKER, 4, 211))
    # --- the head's trimmings: the spiked collar, Mika ears, horns, the mane ---
    def noppen(a):
        hh, ww = a.shape[:2]
        for x in range(1, ww, 4):
            a[0:hh, x:x + 2, :3] = BOT
    m.doos("hoofd", (-8.6, 15.4, -6.6), (8.6, 17.6, 6.6), gm.met(gm.kleur(ZWART, 2, 212), noppen))
    oor = gm.met(gm.kleur(VACHT, 4, 213), lambda a: a.__setitem__((slice(1, 3), slice(1, 3), slice(0, 3)), (236, 150, 170)))
    m.doos("hoofd", (-7.5, 28, -2), (-3.5, 32, 1), gm.kleur(VACHT, 4, 213), voor=oor)
    m.doos("hoofd", (3.5, 28, -2), (7.5, 32, 1), gm.kleur(VACHT, 4, 214), voor=oor)
    hoorn = gm.kleur(BOT, 3, 215, licht=WIT, donker=BOT_DONKER)
    for sx in (-1, 1):
        x0, x1 = sorted((sx * 8, sx * 11.5))
        m.doos("hoofd", (x0, 23.5, -2), (x1, 26.5, 1), hoorn)
        x0, x1 = sorted((sx * 9.8, sx * 12.4))
        m.doos("hoofd", (x0, 26.5, -1.6), (x1, 31, 0.6), hoorn)
        x0, x1 = sorted((sx * 10.6, sx * 12.2))
        m.doos("hoofd", (x0, 31, -1.2), (x1, 33, 0.2), gm.kleur(WIT, 2, 216, donker=BOT))
    manen = gm.met(gm.kleur(MANEN, 6, 217, licht=MANEN_LICHT, donker=MANEN_DONKER))
    m.doos("hoofd", (-2, 28, -6.5), (2, 31.5, 3.5), manen)
    m.doos("hoofd", (-1.5, 31.5, -5.5), (1.5, 34, 0.5), manen)
    m.doos("hoofd", (-2, 19, -8), (2, 28, -6), manen)
    # --- the head itself with one of three faces, and the snout ---
    for naam in ("boos", "mok", "blij"):
        deel = f"gezicht_{naam}"
        kop = gm.kleur(VACHT, 5, 220)
        m.doos(deel, (-8, 17, -6), (8, 28, 6), kop, voor=gm.met(kop, gm.patroon(GEZICHT[naam], KLEUREN)), boven=gm.kleur(VACHT_DONKER, 4, 221))
        snuit = gm.kleur(VACHT_LICHT, 4, 222)
        m.doos(deel, (-5, 17, 6), (5, 22.5, 9.5), snuit, voor=gm.met(snuit, gm.patroon(SNUIT[naam], KLEUREN)),
               boven=gm.met(snuit, gm.patroon(["...nnnn..."], KLEUREN, 0, 3)))
    # --- the shell he tucks into: a ball round the origin, the belly plate underneath, spikes all over ---
    def gaten(a):
        hh, ww = a.shape[:2]
        a[hh // 2 - 2:hh // 2 + 2, ww // 2 - 3:ww // 2 + 3, :3] = (16, 40, 22)          # where his head went
    bol = gm.met(gm.kleur(SCHILD, 5, 230), _platen)
    m.doos("bol", (-8.5, -6.5, -8.5), (8.5, 7, 8.5), bol, voor=gm.met(bol, gaten), achter=gm.met(bol, gaten))
    m.doos("bol", (-9.5, -2, -9.5), (9.5, 1.5, 9.5), rand)
    m.doos("bol", (-7, -7.5, -7), (7, -6.5, 7), gm.met(gm.kleur(BUIK, 3, 231), lambda a: a.__setitem__((slice(None, None, 3), slice(None), slice(0, 3)), BUIK_LIJN)))
    for sx in (-4.5, 4.5):
        for sz in (-4.5, 4.5):
            _stekel(m, "bol", sx, 7, sz, (0, 1, 0))
    for sx in (-4.5, 4.5):
        _stekel(m, "bol", sx, 4.2, 8.5, (0, 0, 1), 2.6)
        _stekel(m, "bol", sx, 4.2, -8.5, (0, 0, -1), 2.6)
    for sx in (-1, 1):
        _stekel(m, "bol", sx * 8.5, 4.2, 0, (sx, 0, 0), 2.6)
    m.save(h)


def kooltje(h):
    m = gm.Model("guhriow3_kooltje", 64, 64)

    def gloed(a):
        hh, ww = a.shape[:2]
        if hh < 4 or ww < 4:
            return
        # two glowing cracks, zigzagging down the face
        for x0, y0, lang in ((1, 1, hh - 3), (ww - 3, 0, hh - 2)):
            x = x0
            for k in range(lang):
                a[min(hh - 1, y0 + k), x, :3] = (255, 226, 110) if k % 3 == 1 else (255, 132, 30)
                x = min(ww - 1, max(0, x + (1 if (k // 2) % 2 == 0 else -1)))
    kool = gm.met(gm.kleur((54, 40, 44), 7, 250, licht=(96, 70, 66), donker=(26, 18, 22)), gloed)
    m.doos("kool", (-4, -4, -4), (4, 4, 4), kool)
    m.doos("kool", (-5, -2, -2.5), (-4, 2.5, 2), kool)
    m.doos("kool", (4, -2.5, -2), (5, 2, 2.5), kool)
    m.doos("kool", (-2, 4, -2.5), (2.5, 5, 2), gm.kleur((255, 150, 40), 10, 251, licht=(255, 236, 150), donker=(226, 80, 24)))
    m.save(h)


def taart(h):
    m = gm.Model("guhriow3_taart", 64, 64)
    m.doos("bord", (-7.5, 0, -7.5), (7.5, 1, 7.5), gm.kleur((244, 244, 250), 2, 260, licht=WIT, donker=(190, 190, 204)))

    def lagen(a):
        hh, ww = a.shape[:2]
        a[:, :, :3] = (246, 226, 170)                                   # sponge
        a[:, :, 3] = 255
        a[0:2, :, :3] = (255, 170, 196)                                 # pink icing on top
        a[1, ::3, :3] = (255, 240, 246)                                 # ... dripping
        a[hh // 2, :, :3] = (214, 60, 70)                               # a line of jam
        a[hh - 1, :, :3] = (214, 180, 120)
    glazuur = gm.met(gm.kleur((255, 170, 196), 3, 261, licht=(255, 226, 236)), gm.stippen(WIT, ((1, 1), (4, 3))))
    kers = gm.kleur((214, 30, 50), 4, 262, licht=(255, 130, 140), donker=(130, 10, 30))
    for i, (sx, sz) in enumerate(((1, 1), (-1, 1), (-1, -1), (1, -1))):
        x0, x1 = sorted((0, sx * 6))
        z0, z1 = sorted((0, sz * 6))
        m.doos(f"punt_{i}", (x0, 1, z0), (x1, 7, z1), lagen, boven=glazuur)
        m.doos(f"punt_{i}", (sx * 3 - 0.9, 7, sz * 3 - 0.9), (sx * 3 + 0.9, 8.6, sz * 3 + 0.9), kers)
    m.save(h)


def build(h):
    grote_nether_mika(h)
    kooltje(h)
    taart(h)


# =====================================================================================================================
# preview (from the files on disk): the poses client/GuhrioW3Client.java draws, without the head's nod
# =====================================================================================================================
STAAND = ["voet_l", "voet_r", "staart", "lijf", "schild", "arm_l", "arm_r", "hoofd"]
POSES = {
    "boos": (STAAND + ["gezicht_boos"], None),
    "sprong": (STAAND + ["gezicht_boos"], {"arm_l": (0, 5, 0), "arm_r": (0, 5, 0), "voet_l": (0, 2, 0), "voet_r": (0, 2, 0)}),
    "mok": (STAAND + ["gezicht_mok"], {"voet_l": (0, 0, 7), "voet_r": (0, 0, 7), "arm_l": (-4, -6, 6), "arm_r": (4, -6, 6), "lijf": (0, -4, 0),
                                       "schild": (0, -4, 0), "staart": (0, -4, 0), "hoofd": (0, -5, 0), "gezicht_mok": (0, -5, 0)}),
    "blij": (STAAND + ["gezicht_blij"], {"voet_l": (0, 0, 7), "voet_r": (0, 0, 7), "arm_l": (2, 2, 0), "arm_r": (-2, 2, 0), "lijf": (0, -4, 0),
                                         "schild": (0, -4, 0), "staart": (0, -4, 0), "hoofd": (0, -5, 0), "gezicht_blij": (0, -5, 0)}),
    "bol": (["bol"], None),
    "bol_kijkt": (["bol", "hoofd", "gezicht_boos"], {"hoofd": (0, -14, 3), "gezicht_boos": (0, -14, 3)}),
}


def voorbeeld(out):
    """Renders the models into `out` (run from the repo root)."""
    import sys
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    for pose, (delen, schuif) in POSES.items():
        q = gm.quads("grote_nether_mika", delen, schuif)
        for hoek, kant in ((215, "voor"), (150, "zij"), (180, "recht"), (35, "achter")):
            wr.render(q, hoek, -14, 384, margin=0.06).save(os.path.join(out, f"mika_{pose}_{kant}.png"))
    for naam in ("guhriow3_kooltje", "guhriow3_taart"):
        wr.render(gm.quads(naam), 210, -24, 256, margin=0.1).save(os.path.join(out, f"{naam}.png"))
    wr.render(gm.quads("guhriow3_taart", ["bord", "punt_1", "punt_2", "punt_3"]), 210, -24, 256, margin=0.1).save(os.path.join(out, "guhriow3_taart_stukje.png"))
    print("guhrio-w3 models rendered to", out)


if __name__ == "__main__":
    import sys
    sys.path.insert(0, "tools")
    voorbeeld(sys.argv[1] if len(sys.argv) > 1 else "guhrio_w3_modellen")
