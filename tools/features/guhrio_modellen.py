"""
Super Guhrio (bbq2) - the box models of the level's creatures and moving things, with their textures.

Every model is a handful of plain boxes grouped in parts ("delen"). This module is the one place they are described:
it lays the faces of every box out on a texture, paints them, and writes
  assets/guhs/guhrio_model/<naam>.json     the boxes (read by client/DoosModel.java; sizes in pixels, the front is +z)
  assets/guhs/textures/entity/<naam>.png   the texture
so what client/WezenRenderers.java draws is exactly what is described here. `python tools/features/guhrio_modellen.py <dir>`
(from the repo root, after the generators ran) renders a preview picture of every model from the files on disk.

  guhmba             a round grumpy mini-Mika on two big feet                      parts voet_l, voet_r, lijf, oren
  schild_mika        a Mika with a green shell on its back                         parts voet_l, voet_r, lijf, oren, schild
  plof_mika          a square block of stone with a Mika face, ears and studs      parts blok (dozing), blok_boos, oren, noppen
  hapbloem           a stem with leaves and a spotted head of two chomping halves  parts steel, kaak_l, kaak_r
  guhrio_grillspies  hub cap, a block of skewer, two kinds of chunk, the tip       parts naaf, stok, brok_a, brok_b, punt
  guhrio_platform    one block of wooden deck and its two end caps                 parts midden, kap_l, kap_r
  guhrio_valblok     the same in cracked stone                                     parts midden, kap_l, kap_r
"""
import json
import math
import os

import numpy as np
from PIL import Image

MIKA, MIKA_DONKER, MIKA_LICHT = (214, 161, 180), (176, 126, 146), (236, 196, 210)
VOET = (112, 72, 88)
ZWART, WIT, NEUS = (40, 22, 34), (255, 255, 255), (236, 110, 150)
STEEN, STEEN_LICHT, STEEN_DONKER = (122, 124, 134), (180, 182, 194), (62, 62, 72)
GROEN, GROEN_LICHT, GROEN_DONKER = (58, 170, 70), (150, 232, 140), (24, 104, 44)
ROOM = (250, 238, 196)
IJZER, IJZER_LICHT, IJZER_DONKER = (86, 86, 96), (150, 150, 162), (36, 36, 44)

NAMEN = ("guhmba", "schild_mika", "plof_mika", "hapbloem", "guhrio_grillspies", "guhrio_platform", "guhrio_valblok")


# =====================================================================================================================
# painters: a painter fills a (h, w, 4) array in place
# =====================================================================================================================
def kleur(rgb, var=4, seed=0, licht=None, donker=None):
    """A flat colour with a little noise; licht / donker: a bevel (top-left light, bottom-right dark)."""
    def schilder(a):
        h, w = a.shape[:2]
        rng = np.random.default_rng(9300 + seed * 131 + w * 7 + h)
        n = rng.normal(0, var, (h, w))
        for c in range(3):
            a[..., c] = np.clip(rgb[c] + n, 0, 255)
        a[..., 3] = 255
        if licht is not None:
            a[0, :, :3] = licht
            a[:, 0, :3] = licht
        if donker is not None:
            a[h - 1, :, :3] = donker
            a[:, w - 1, :3] = donker
    return schilder


def met(basis, *extra):
    """A painter and then more painters on top."""
    def schilder(a):
        basis(a)
        for e in extra:
            e(a)
    return schilder


def patroon(rijen, kleuren, x0=0, y0=0):
    """Rows of characters -> pixels; '.' (or any unknown char) leaves the pixel alone."""
    def schilder(a):
        for r, rij in enumerate(rijen):
            for c, ch in enumerate(rij):
                if ch in kleuren and 0 <= y0 + r < a.shape[0] and 0 <= x0 + c < a.shape[1]:
                    a[y0 + r, x0 + c, :3] = kleuren[ch]
                    a[y0 + r, x0 + c, 3] = 255
    return schilder


def strook(y0, y1, rgb):
    def schilder(a):
        a[y0:y1, :, :3] = rgb
    return schilder


def stippen(rgb, plekken):
    def schilder(a):
        for x, y in plekken:
            for dx in (0, 1):
                for dy in (0, 1):
                    if 0 <= y + dy < a.shape[0] and 0 <= x + dx < a.shape[1]:
                        a[y + dy, x + dx, :3] = rgb
    return schilder


# =====================================================================================================================
# the model builder: lays every face of every box out on the texture
# =====================================================================================================================
class Model:
    def __init__(self, naam, tw=64, th=64):
        self.naam, self.tw, self.th = naam, tw, th
        self.tex = np.zeros((th, tw, 4), np.uint8)
        self.delen = {}
        self.x = self.y = self.rij = 0

    def _vlak(self, w, h, schilder):
        w, h = max(1, int(math.ceil(w - 1e-6))), max(1, int(math.ceil(h - 1e-6)))
        if self.x + w > self.tw:
            self.x, self.y, self.rij = 0, self.y + self.rij + 1, 0
        if self.y + h > self.th:
            raise SystemExit(f"guhrio model {self.naam}: the texture ({self.tw} x {self.th}) is full")
        schilder(self.tex[self.y:self.y + h, self.x:self.x + w])
        uv = [self.x, self.y, w, h]
        self.x += w + 1
        self.rij = max(self.rij, h)
        return uv

    def doos(self, deel, van, tot, zij, voor=None, boven=None, achter=None, onder=None, links=None, rechts=None):
        """A box from `van` to `tot` (pixels). zij paints every face that has no painter of its own; links = its own left
        (+x), rechts = its own right (-x), voor = +z."""
        dx, dy, dz = (tot[i] - van[i] for i in range(3))
        d = {"van": list(van), "tot": list(tot), "zij": self._vlak(dz, dy, zij),
             "voor": self._vlak(dx, dy, voor or zij), "achter": self._vlak(dx, dy, achter or zij),
             "boven": self._vlak(dx, dz, boven or zij), "onder": self._vlak(dx, dz, onder or zij)}
        if links is not None:
            d["links"] = self._vlak(dz, dy, links)
        if rechts is not None:
            d["rechts"] = self._vlak(dz, dy, rechts)
        self.delen.setdefault(deel, []).append(d)

    def save(self, h):
        h.save(Image.fromarray(self.tex), "entity", f"{self.naam}.png")
        h.w(os.path.join(h.A, "guhrio_model", f"{self.naam}.json"), {"textuur": [self.tw, self.th], "delen": self.delen})


# =====================================================================================================================
# the models
# =====================================================================================================================
BOOS = ["bb......bb..",
        "..bb..bb....",
        "..ww..ww....",
        "..wo..ow....",
        "............",
        ".....nn.....",
        "..w......w..",
        "...mmmmmm...",
        "..m......m.."]


def _gezicht(boos=True):
    """A Mika face, 12 wide: slanted brows, white eyes, the pink nose, a frown with two teeth."""
    rijen = ["bb........bb", ".bbb....bbb.", "..ww....ww..", "..wo....ow..", "............", ".....nn.....", "...w....w...", "...mmmmmm...",
             "..m......m.."] if boos else ["............", "..bb....bb..", "..ww....ww..", "..ow....wo..", "............", ".....nn.....",
                                          "............", "...m....m...", "....mmmm...."]
    return patroon(rijen, {"b": ZWART, "w": WIT, "o": ZWART, "n": NEUS, "m": ZWART})


def _voeten(m):
    voet = met(kleur(VOET, 4, 1), lambda a: a.__setitem__((slice(a.shape[0] - 1, None), slice(None), slice(0, 3)), (70, 44, 56)))
    m.doos("voet_l", (-6, 0, -3), (-1, 3, 4), voet)
    m.doos("voet_r", (1, 0, -3), (6, 3, 4), voet)


def guhmba(h):
    m = Model("guhmba", 64, 64)
    _voeten(m)
    vacht = met(kleur(MIKA, 4, 2), lambda a: a.__setitem__((slice(a.shape[0] - 2, None), slice(None), slice(0, 3)), MIKA_DONKER))
    snoet = met(kleur(MIKA, 4, 3), lambda a: a.__setitem__((slice(6, 9), slice(2, 10), slice(0, 3)), MIKA_LICHT), _gezicht(True))
    m.doos("lijf", (-6, 3, -5), (6, 12, 5), vacht, voor=snoet, boven=kleur(MIKA_DONKER, 5, 4))
    oor = met(kleur(MIKA, 4, 5), lambda a: a.__setitem__((slice(1, 3), slice(1, 2), slice(0, 3)), (240, 150, 180)))
    m.doos("oren", (-5.5, 12, -1), (-2.5, 15, 1), kleur(MIKA, 4, 5), voor=oor)
    m.doos("oren", (2.5, 12, -1), (5.5, 15, 1), kleur(MIKA, 4, 6), voor=oor)
    m.save(h)


def schild_mika(h):
    m = Model("schild_mika", 64, 64)
    _voeten(m)
    vacht = kleur(MIKA, 4, 7)
    snoet = met(kleur(MIKA, 4, 8), lambda a: a.__setitem__((slice(6, 9), slice(2, 8), slice(0, 3)), MIKA_LICHT),
                patroon(["..........", ".bb....bb.", ".ww....ww.", ".wo....ow.", "..........", "....nn....", "..........", "...mmmm...",
                         "..m....m.."], {"b": ZWART, "w": WIT, "o": ZWART, "n": NEUS, "m": ZWART}))
    m.doos("lijf", (-5, 3, -1), (5, 12, 6), vacht, voor=snoet, boven=kleur(MIKA_DONKER, 5, 9))
    oor = met(kleur(MIKA, 4, 10), lambda a: a.__setitem__((slice(1, 3), slice(1, 2), slice(0, 3)), (240, 150, 180)))
    m.doos("oren", (-4.5, 12, 2), (-1.5, 15, 4), kleur(MIKA, 4, 10), voor=oor)
    m.doos("oren", (1.5, 12, 2), (4.5, 15, 4), kleur(MIKA, 4, 11), voor=oor)
    # the shell: a cream rim, a green dome with lighter plates, a little top
    def platen(a):
        hh, ww = a.shape[:2]
        for y in range(hh):
            for x in range(ww):
                if (x + (y // 3) * 2) % 5 == 0 or y % 3 == 2:
                    a[y, x, :3] = GROEN_DONKER
        a[0, :, :3] = GROEN_LICHT
    m.doos("schild", (-6.5, 3, -8), (6.5, 5.5, 0), kleur(ROOM, 3, 12, licht=(255, 250, 226), donker=(196, 176, 120)))
    m.doos("schild", (-6, 5.5, -7.5), (6, 11, -0.5), met(kleur(GROEN, 4, 13), platen))
    m.doos("schild", (-4.5, 11, -6), (4.5, 13.5, -2), kleur(GROEN_LICHT, 4, 14, licht=(200, 250, 190), donker=GROEN))
    m.save(h)


def plof_mika(h):
    m = Model("plof_mika", 128, 128)

    def scheuren(a):
        hh, ww = a.shape[:2]
        rng = np.random.default_rng(9340 + ww)
        for _ in range(3):
            x, y = int(rng.integers(3, ww - 3)), int(rng.integers(3, hh - 6))
            for _k in range(5):
                a[y, x, :3] = STEEN_DONKER
                y, x = min(hh - 1, y + 1), min(ww - 1, max(0, x + int(rng.integers(-1, 2))))

    steen = met(kleur(STEEN, 7, 20, licht=STEEN_LICHT, donker=STEEN_DONKER), scheuren)
    # the dozing face: heavy lids, a straight mouth; the awake one: furious brows, wide eyes, teeth
    slaap = ["..........................",
             "..........................",
             "....bbbbbb......bbbbbb....",
             "...bbbbbbbb....bbbbbbbb...",
             "...wwwwwwww....wwwwwwww...",
             "....wwoowww....wwwoow.....",
             "..........................",
             "...........nnnn...........",
             "...........nnnn...........",
             "..........................",
             ".....mmmmmmmmmmmmmmmm.....",
             "....m................m...."]
    boos = ["...bbb..............bbb...",
            "....bbbb..........bbbb....",
            "......bbbb......bbbb......",
            "...wwwwwbbb....bbbwwwww...",
            "...wwwwwwww....wwwwwwww...",
            "...wwwooww......wwooww....",
            "...wwwooww......wwooww....",
            "....wwwww........wwwww....",
            "...........nnnn...........",
            "...........nnnn...........",
            "....mmmmmmmmmmmmmmmmmm....",
            "....mtt.tt.tt.tt.tt.tm....",
            "....mtt.tt.tt.tt.tt.tm....",
            "....mmmmmmmmmmmmmmmmmm...."]
    k = {"b": (30, 30, 36), "w": WIT, "o": (200, 30, 40), "n": NEUS, "m": (30, 30, 36), "t": (240, 240, 240)}
    m.doos("blok", (-13, 0, -13), (13, 28, 13), steen, voor=met(steen, patroon(slaap, {**k, "o": (30, 30, 36)}, 0, 7)))
    m.doos("blok_boos", (-13, 0, -13), (13, 28, 13), steen, voor=met(steen, patroon(boos, k, 0, 6)))
    oor = kleur(STEEN, 6, 21, licht=STEEN_LICHT, donker=STEEN_DONKER)
    m.doos("oren", (-11, 28, -3), (-6, 32, 3), oor)
    m.doos("oren", (6, 28, -3), (11, 32, 3), oor)
    nop = kleur(STEEN_LICHT, 4, 22, licht=(220, 222, 232), donker=STEEN)
    for sx in (-1, 1):
        m.doos("noppen", (sx * 15 if sx < 0 else 13, 10, -3), (sx * 13 if sx < 0 else 15, 16, 3), nop)
        for sz in (-1, 1):
            m.doos("noppen", (min(sx * 13, sx * 10), 28, min(sz * 13, sz * 10)), (max(sx * 13, sx * 10), 30, max(sz * 13, sz * 10)), nop)
    m.save(h)


def hapbloem(h):
    m = Model("hapbloem", 64, 64)
    steel = kleur(GROEN, 4, 30, licht=GROEN_LICHT, donker=GROEN_DONKER)
    m.doos("steel", (-1, 0, -1), (1, 13, 1), steel)
    m.doos("steel", (-6, 4, -0.5), (-1, 6, 0.5), kleur(GROEN, 4, 31, licht=GROEN_LICHT))
    m.doos("steel", (1, 7, -0.5), (6, 9, 0.5), kleur(GROEN, 4, 32, licht=GROEN_LICHT))
    rood = (232, 60, 80)
    bol = met(kleur(rood, 4, 33, licht=(255, 130, 140), donker=(150, 30, 50)), stippen(WIT, ((1, 2), (6, 5), (2, 8), (8, 1), (7, 9))))
    # the inside of the mouth: dark pink with a row of white teeth along the top
    binnen = met(kleur((150, 40, 70), 4, 34), lambda a: a.__setitem__((slice(0, 2), slice(None, None, 2), slice(0, 3)), WIT))
    lip = kleur((255, 250, 250), 2, 35, donker=(220, 200, 200))
    m.doos("kaak_l", (-6, 12, -5), (0, 24, 5), bol, links=binnen)
    m.doos("kaak_l", (-1.5, 22.5, -5.5), (0.2, 25, 5.5), lip)
    m.doos("kaak_r", (0, 12, -5), (6, 24, 5), bol, rechts=binnen)
    m.doos("kaak_r", (-0.2, 22.5, -5.5), (1.5, 25, 5.5), lip)
    m.save(h)


def grillspies(h):
    m = Model("guhrio_grillspies", 64, 64)
    ijzer = kleur(IJZER, 5, 40, licht=IJZER_LICHT, donker=IJZER_DONKER)
    m.doos("naaf", (-3.5, -3.5, -3.5), (3.5, 3.5, 3.5), ijzer, voor=met(ijzer, stippen((255, 190, 70), ((2, 2), (3, 3)))))
    m.doos("stok", (-8, -1, -1), (8, 1, 1), kleur(IJZER_LICHT, 4, 41, donker=IJZER))

    def strepen(a):
        for x in range(1, a.shape[1], 3):
            a[:, x, :3] = (70, 34, 20)
    vlees = met(kleur((168, 92, 52), 6, 42, licht=(214, 140, 90), donker=(96, 48, 26)), strepen)
    peper = met(kleur((226, 52, 40), 5, 43, licht=(255, 130, 90), donker=(140, 24, 22)), stippen((255, 226, 110), ((1, 1), (4, 5))))
    m.doos("brok_a", (-4, -3.5, -3.5), (4, 3.5, 3.5), vlees)
    m.doos("brok_b", (-3, -4, -4), (3, 4, 4), peper)
    m.doos("punt", (-8, -1.5, -1.5), (-3, 1.5, 1.5), kleur((255, 170, 60), 6, 44, licht=(255, 236, 150), donker=(220, 90, 30)))
    m.save(h)


def _dek(h, naam, boven, zij, kap):
    m = Model(naam, 64, 64)
    m.doos("midden", (-8, 0, -8), (8, 8, 8), zij, boven=boven)
    m.doos("kap_l", (-9, 0.5, -8.5), (-8, 7.5, 8.5), kap)
    m.doos("kap_r", (8, 0.5, -8.5), (9, 7.5, 8.5), kap)
    m.save(h)


def platform(h):
    def planken(a):
        for y in (0, 5, 10, 15):
            if y < a.shape[0]:
                a[y, :, :3] = (110, 70, 36)
        for x, y in ((2, 2), (13, 2), (2, 12), (13, 12)):
            if y < a.shape[0] and x < a.shape[1]:
                a[y, x, :3] = (236, 206, 120)
    hout = kleur((184, 130, 76), 5, 50)
    rand = met(kleur((160, 108, 60), 5, 51, licht=(226, 176, 110), donker=(96, 60, 30)), strook(3, 5, (240, 200, 80)))
    _dek(h, "guhrio_platform", met(hout, planken), rand, kleur((214, 214, 222), 4, 52, licht=WIT, donker=(130, 130, 140)))


def valblok(h):
    def barsten(a):
        hh, ww = a.shape[:2]
        for x, y in ((3, 2), (4, 3), (5, 3), (6, 4), (10, 1), (10, 2), (11, 3), (12, 5), (2, 6)):
            if y < hh and x < ww:
                a[y, x, :3] = STEEN_DONKER
    steen = met(kleur((150, 150, 160), 6, 60, licht=(210, 210, 222), donker=(70, 70, 82)), barsten)
    frons = patroon(["bb....bb", ".b....b.", "........", "..mmmm..", ".m....m."], {"b": (40, 40, 50), "m": (40, 40, 50)}, 4, 1)
    m = Model("guhrio_valblok", 64, 64)
    m.doos("midden", (-8, 0, -8), (8, 8, 8), steen, voor=met(steen, frons), boven=steen)
    kap = kleur((110, 110, 122), 5, 61, licht=(170, 170, 184), donker=(56, 56, 66))
    m.doos("kap_l", (-9, 0.5, -8.5), (-8, 7.5, 8.5), kap)
    m.doos("kap_r", (8, 0.5, -8.5), (9, 7.5, 8.5), kap)
    m.save(h)


def build(h):
    guhmba(h)
    schild_mika(h)
    plof_mika(h)
    hapbloem(h)
    grillspies(h)
    platform(h)
    valblok(h)


# =====================================================================================================================
# preview: the boxes as client/DoosModel.java draws them (from the files on disk)
# =====================================================================================================================
def quads(naam, delen=None, verschuif=None, assets=os.path.join("src", "main", "resources", "assets", "guhs")):
    """wiki_renders quads of a model (blocks; the front is +z). delen: which parts (default all); verschuif: {deel: (dx, dy, dz)} pixels."""
    import wiki_renders as wr
    data = json.load(open(os.path.join(assets, "guhrio_model", f"{naam}.json"), encoding="utf-8"))
    tex = np.asarray(Image.open(os.path.join(assets, "textures", "entity", f"{naam}.png")).convert("RGBA")).astype(np.float32)
    tw, th = data["textuur"]
    uit = []
    for deel, dozen in data["delen"].items():
        if delen is not None and deel not in delen:
            continue
        sx, sy, sz = (verschuif or {}).get(deel, (0, 0, 0))
        for d in dozen:
            (x0, y0, z0), (x1, y1, z1) = [[(v + s) / 16 for v, s in zip(d[k], (sx, sy, sz))] for k in ("van", "tot")]
            w, hh, dd = x1 - x0, y1 - y0, z1 - z0

            def uv(k):
                r = d.get(k) or d["zij"]
                return (r[0] / tw, r[1] / th, (r[0] + r[2]) / tw, (r[1] + r[3]) / th)
            for o, u, v, n, k in (((x0, y1, z1), (w, 0, 0), (0, -hh, 0), (0, 0, 1), "voor"), ((x1, y1, z0), (-w, 0, 0), (0, -hh, 0), (0, 0, -1), "achter"),
                                  ((x1, y1, z1), (0, 0, -dd), (0, -hh, 0), (1, 0, 0), "links"), ((x0, y1, z0), (0, 0, dd), (0, -hh, 0), (-1, 0, 0), "rechts"),
                                  ((x0, y1, z0), (w, 0, 0), (0, 0, dd), (0, 1, 0), "boven"), ((x0, y0, z1), (w, 0, 0), (0, 0, -dd), (0, -1, 0), "onder")):
                uit.append(wr.Quad(o, u, v, tex, uv(k), n))
    return uit


def voorbeeld(out):
    """Renders every model from two sides into `out` (run from the repo root)."""
    import sys
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    keuzes = {"plof_mika": [("", ["blok", "oren", "noppen"]), ("_boos", ["blok_boos", "oren", "noppen"])],
              "guhrio_grillspies": [("", None)], "schild_mika": [("", None), ("_schild", ["schild"])]}
    for naam in NAMEN:
        for achter, delen in keuzes.get(naam, [("", None)]):
            schuif = {"brok_a": (16, 0, 0), "brok_b": (32, 0, 0), "stok": (24, 0, 0), "punt": (56, 0, 0)} if naam == "guhrio_grillspies" else None
            q = quads(naam, delen, schuif)
            # (yaw 180 + 30: the camera of wiki_renders looks down +z; the models' front is +z)
            for hoek, kant in ((210, "voor"), (150, "zij"), (180, "recht")):
                wr.render(q, hoek, -18, 256, margin=0.06).save(os.path.join(out, f"{naam}{achter}_{kant}.png"))
    print("guhrio models rendered to", out)


if __name__ == "__main__":
    import sys
    voorbeeld(sys.argv[1] if len(sys.argv) > 1 else "guhrio_modellen")
