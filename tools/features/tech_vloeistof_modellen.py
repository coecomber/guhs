"""
bbq2 (tech-vloeistof): the looks of the sauce blocks. Helper of features/tech_vloeistof.py (not in FEATURES).

Every block is a little guh: pink, two ears, and the standard machine face of features/vadskracht.py (asleep / happy /
surprised). None of them is a plain cube:
  sauspomp         a pink belly on a steel suction pipe, a steel cap with a yellow knob, hose couplings left, right and behind
  sausslang        a thin sauce-yellow hose: a brass knot and an arm per joined side (multipart)
  sausvat          a wooden barrel with iron hoops, a tap under its face, an open top where the sauce stands, and a glass gauge
                   in both sides that shows which sauce and how much (saus x niveau)
  brouwautomaat    a pink machine with a dark pan of kaasbouillon on top
  frituurautomaat  a pink fryer with a steel rim, the frituursaus and a wire basket with a handle
  grillkoolpers    two blocks high: a dark base with the face, four steel pillars, a pink head with the ears, and a stamp that
                   comes down on the block of grillkool while it presses (perst)
Textures are painted per box (a 16 x 16 picture per material, laid on by position), models are written with explicit uv on
every face (26.1 wants that for anything above y 16, and it keeps the boxes of the press in range).
"""
import numpy as np
from PIL import Image

from features import vadskracht

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
STATEN = vadskracht.STATEN

ROZE = (232, 150, 190)
ROZE_LICHT = (244, 184, 212)
STAAL = (112, 134, 148)
STAAL_DONKER = (70, 86, 98)
GEEL = (246, 196, 80)
MESSING = (198, 150, 62)
SLANG = (234, 128, 56)
HOUT = (184, 128, 80)
HOUT_NAAD = (128, 84, 50)
HOEPEL = (84, 88, 100)
PAN = (64, 58, 74)
PAN_LICHT = (98, 92, 110)
KOOL = (72, 64, 72)
PAARS = (150, 110, 205)
ORANJE = (232, 140, 60)
GLAS = (206, 232, 238)
OOR = vadskracht.OOR
OOR_BINNEN = vadskracht.OOR_BINNEN
# the four sauces: what stands in the vat, in the gauge and in the pans
SAUZEN = {"kaassaus": (244, 178, 38), "frituursaus": (236, 112, 30), "water": (70, 128, 226), "melk": (247, 245, 238)}


def donker(kleur, f=0.72):
    return tuple(int(max(0, min(255, c * f))) for c in kleur[:3])


def _zaad(naam):
    return sum((i + 1) * ord(ch) for i, ch in enumerate(naam)) % 100000


class Doek:
    """A 16 x 16 texture to paint on, in texture pixels (x to the right, y down)."""

    def __init__(self, naam, basis=None, var=5):
        self.naam = naam
        self.a = np.zeros((16, 16, 4), np.uint8)
        self.rng = np.random.default_rng(_zaad(naam))
        if basis is not None:
            self.vlak(0, 0, 16, 16, basis, var)

    def vlak(self, x0, y0, x1, y1, kleur, var=5, rand=None):
        """Fills [x0, x1) x [y0, y1) with this colour and a little noise; rand: a frame of one pixel in that colour."""
        x0, y0, x1, y1 = max(0, x0), max(0, y0), min(16, x1), min(16, y1)
        if x1 <= x0 or y1 <= y0:
            return self
        n = self.rng.normal(0, var, (y1 - y0, x1 - x0)) if var else np.zeros((y1 - y0, x1 - x0))
        for c in range(3):
            self.a[y0:y1, x0:x1, c] = np.clip(kleur[c] + n, 0, 255)
        self.a[y0:y1, x0:x1, 3] = 255
        if rand is not None:
            self.a[y0, x0:x1, :3] = rand
            self.a[y1 - 1, x0:x1, :3] = rand
            self.a[y0:y1, x0, :3] = rand
            self.a[y0:y1, x1 - 1, :3] = rand
        return self

    def px(self, x, y, kleur):
        if 0 <= x < 16 and 0 <= y < 16:
            self.a[y, x, :3] = kleur[:3]
            self.a[y, x, 3] = 255
        return self

    def gat(self, x, y):
        if 0 <= x < 16 and 0 <= y < 16:
            self.a[y, x] = 0
        return self

    def snoet(self, x, y, staat):
        img = vadskracht.snoet(Image.fromarray(self.a), x, y, staat)
        self.a = np.asarray(img).copy()
        return self

    def img(self):
        return Image.fromarray(self.a)

    def save(self, h):
        h.save(self.img(), "block", f"{self.naam}.png")


# =====================================================================================================================
# model elements with explicit uv
# =====================================================================================================================
def _reeks(lo, hi):
    """16 units of texture for the span lo..hi of a box: shifted so that it stays inside 0..16 (the top / left end first)."""
    s = 0
    if hi > 16:
        s = hi - 16
    if lo - s < 0:
        s = lo
    return lo - s, hi - s


def doos(frm, to, tex, **anders):
    """A box whose six faces show `tex` laid on by position; anders: north="#voor", up=None (no face) ... per face."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    xa, xb = _reeks(x0, x1)
    ya, yb = _reeks(y0, y1)
    za, zb = _reeks(z0, z1)
    uv = {"north": [16 - xb, 16 - yb, 16 - xa, 16 - ya], "south": [xa, 16 - yb, xb, 16 - ya],
          "east": [16 - zb, 16 - yb, 16 - za, 16 - ya], "west": [za, 16 - yb, zb, 16 - ya],
          "up": [xa, za, xb, zb], "down": [xa, 16 - zb, xb, 16 - za]}
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        t = anders.get(f, tex)
        if t is not None:
            faces[f] = {"texture": t, "uv": uv[f]}
    return {"from": list(frm), "to": list(to), "faces": faces}


def oren(y, z=1, tex="#oor", xs=(3, 10)):
    """The two ears (3 wide, 3 high, 2 deep) standing on height y near the front."""
    out = []
    for x in xs:
        faces = {f: {"texture": tex, "uv": [0, 0, 3, 3]} for f in ("north", "south")}
        faces.update({f: {"texture": tex, "uv": [4, 0, 6, 3]} for f in ("east", "west")})
        faces["up"] = {"texture": tex, "uv": [0, 4, 3, 6]}
        out.append({"from": [x, y, z], "to": [x + 3, y + 3, z + 2], "faces": faces})
    return out


def model(h, naam, textures, elements, particle, display=None):
    m = {"parent": "minecraft:block/block", "textures": {"particle": particle, **textures}, "elements": elements}
    if display:
        m["display"] = display
    h.w(f"{h.A}/models/block/{naam}.json", m)


def kind(h, naam, ouder, textures):
    h.w(f"{h.A}/models/block/{naam}.json", {"parent": f"guhs:block/{ouder}", "textures": textures})


T = lambda naam: f"guhs:block/{naam}"


# =====================================================================================================================
# shared materials
# =====================================================================================================================
def materialen(h):
    # steel with rivets
    d = Doek("techsaus_staal", STAAL, 4)
    for x, y in ((1, 1), (9, 1), (5, 6), (13, 6), (1, 11), (9, 11), (5, 14), (13, 14)):
        d.px(x, y, donker(STAAL, 1.25))
    for y in (4, 9):
        d.a[y, :, :3] = np.clip(d.a[y, :, :3].astype(int) - 16, 0, 255)
    d.save(h)
    # the yellow knob / brass
    d = Doek("techsaus_messing", MESSING, 5)
    d.vlak(0, 0, 16, 16, MESSING, 5)
    for i in range(0, 16, 4):
        d.a[:, i, :3] = donker(MESSING, 0.8)
        d.a[i, :, :3] = donker(MESSING, 0.8)
    d.save(h)
    # the ears: the outside (0..3 x 0..3) with a darker inside, the side (4..6 x 0..3), the top (0..3 x 4..6)
    d = Doek("techsaus_oor", OOR, 2)
    d.px(1, 1, OOR_BINNEN).px(1, 2, OOR_BINNEN)
    d.save(h)
    # what stands in a vat or a pan: the sauce seen from above
    for id, kleur in SAUZEN.items():
        d = Doek(f"techsaus_saus_{id}", kleur, 4)
        licht = donker(kleur, 1.12)
        for x, y in ((3, 4), (4, 4), (10, 3), (11, 8), (6, 10), (7, 10), (12, 12), (2, 12)):
            d.px(x, y, licht)
        if id == "frituursaus":
            for x, y in ((5, 6), (9, 11), (13, 5)):
                d.px(x, y, (255, 214, 96))            # hot bubbles
        d.save(h)
    # an empty vat / pan seen from above: dark
    Doek("techsaus_bodem", (48, 40, 46), 4).save(h)


# =====================================================================================================================
# Sauspomp
# =====================================================================================================================
def sauspomp(h):
    n = "sauspomp"
    # the belly, 12 wide (x 2..14) and 9 high (y 4..13): texture rows 3..11, columns 2..13
    for staat in STATEN:
        d = Doek(f"{n}_voor_{staat}")
        d.vlak(2, 3, 14, 12, ROZE, 5, rand=donker(ROZE))
        d.vlak(3, 10, 13, 11, GEEL if staat != "slaapt" else donker(GEEL, 0.6), 3)   # the sauce band glows when it has vadskracht
        d.snoet(2, 5, staat)
        d.save(h)
    d = Doek(n)                                               # the sides and the back of the belly
    d.vlak(2, 3, 14, 12, ROZE, 6, rand=donker(ROZE))
    d.vlak(3, 10, 13, 11, GEEL, 3)
    for x, y in ((4, 5), (11, 5)):
        d.px(x, y, ROZE_LICHT)                                # rivets
    d.save(h)
    d = Doek(f"{n}_boven")
    d.vlak(2, 2, 14, 14, donker(ROZE, 1.08), 4, rand=donker(ROZE))
    d.save(h)
    for staat in STATEN:
        model(h, f"{n}_{staat}", {"voor": T(f"{n}_voor_{staat}"), "lijf": T(n), "boven": T(f"{n}_boven"), "staal": T("techsaus_staal"),
                                 "knop": T("techsaus_messing"), "oor": T("techsaus_oor")}, [
            doos((5, 0, 5), (11, 4, 11), "#staal"),                                   # the suction pipe into the source
            doos((2, 4, 2), (14, 13, 14), "#lijf", north="#voor", up="#boven", down="#boven"),
            doos((4, 13, 5), (12, 15, 12), "#staal"),                                 # the cap
            doos((7, 15, 7), (9, 16, 10), "#knop"),                                   # the knob
            doos((0, 5, 5), (2, 11, 11), "#staal"), doos((14, 5, 5), (16, 11, 11), "#staal"),     # couplings: west, east
            doos((5, 5, 14), (11, 11, 16), "#staal"),                                 # and behind
            *oren(13, z=2)], T(n))
    return n


# =====================================================================================================================
# Sausslang
# =====================================================================================================================
def sausslang(h):
    n = "sausslang"
    d = Doek(n, SLANG, 5)                                    # rubber with darker rings, the same in both directions
    for i in (2, 7, 12):
        d.a[:, i, :3] = donker(SLANG, 0.78)
        d.a[i, :, :3] = donker(SLANG, 0.78)
    d.save(h)
    d = Doek(f"{n}_knoop", MESSING, 5)
    d.vlak(5, 5, 11, 11, MESSING, 5, rand=donker(MESSING, 0.7))
    d.px(7, 7, donker(MESSING, 1.3)).px(8, 8, donker(MESSING, 0.7))
    d.save(h)
    tex = {"slang": T(n), "knoop": T(f"{n}_knoop")}
    model(h, f"{n}_knoop", tex, [doos((5, 5, 5), (11, 11, 11), "#knoop")], T(n))
    armen = {"north": ((6, 6, 0), (10, 10, 5)), "south": ((6, 6, 11), (10, 10, 16)), "west": ((0, 6, 6), (5, 10, 10)),
             "east": ((11, 6, 6), (16, 10, 10)), "down": ((6, 0, 6), (10, 5, 10)), "up": ((6, 11, 6), (10, 16, 10))}
    for kant, (frm, to) in armen.items():
        model(h, f"{n}_arm_{kant}", tex, [doos(frm, to, "#slang")], T(n))
    h.w(f"{h.A}/blockstates/{n}.json", {"multipart": [{"apply": {"model": T(f"{n}_knoop")}}] + [
        {"when": {kant: "true"}, "apply": {"model": T(f"{n}_arm_{kant}")}} for kant in armen]})
    # in the hand: a straight piece
    model(h, n, tex, [doos((5, 5, 5), (11, 11, 11), "#knoop"), doos(*armen["west"], "#slang"), doos(*armen["east"], "#slang")], T(n))
    return n


# =====================================================================================================================
# Sausvat
# =====================================================================================================================
def _duigen(d, x0, y0, x1, y1):
    """Wooden staves between x0..x1, y0..y1."""
    d.vlak(x0, y0, x1, y1, HOUT, 6)
    for x in range(x0 + 2, x1, 3):
        d.a[y0:y1, x, :3] = HOUT_NAAD


def sausvat(h):
    n = "sausvat"
    snoeten = {"slaapt": "slaapt", "werkt": "werkt", "vol": "vol"}
    # the barrel, 12 wide (x 2..14) and 13 high (y 2..15): texture rows 1..13, columns 2..13
    for staat in snoeten:
        d = Doek(f"{n}_voor_{staat}")
        _duigen(d, 0, 0, 16, 16)
        d.vlak(2, 4, 14, 10, ROZE, 4, rand=donker(ROZE))                             # a pink face plate on the wood
        d.snoet(2, 5, staat)
        d.save(h)

    def zijkant(naam, kleur, niveau):
        d = Doek(naam)
        _duigen(d, 0, 0, 16, 16)
        d.vlak(6, 3, 10, 12, HOEPEL, 3)                                              # the frame of the gauge
        d.vlak(7, 4, 9, 11, GLAS, 2)                                                 # glass, 7 high
        hoogte = (0, 2, 4, 6, 7)[niveau]
        if hoogte:
            d.vlak(7, 11 - hoogte, 9, 11, kleur, 3)
        d.save(h)

    zijkant(f"{n}_zij_leeg", None, 0)
    for id, kleur in SAUZEN.items():
        for niveau in range(1, 5):
            zijkant(f"{n}_zij_{id}_{niveau}", kleur, niveau)
    d = Doek(n)                                                                      # (the placeholder's name: the back and the inside)
    _duigen(d, 0, 0, 16, 16)
    d.save(h)
    d = Doek(f"{n}_boven", donker(HOUT, 1.1), 5)                                     # the rim (from above and from the side)
    d.vlak(2, 2, 14, 14, donker(HOUT, 1.1), 5, rand=HOUT_NAAD)
    d.save(h)
    Doek(f"{n}_hoepel", HOEPEL, 4).save(h)

    basis = {"voor": T(f"{n}_voor_slaapt"), "zij": T(f"{n}_zij_leeg"), "achter": T(n), "boven": T(f"{n}_boven"), "hoepel": T(f"{n}_hoepel"),
             "saus": T("techsaus_bodem"), "staal": T("techsaus_staal"), "knop": T("techsaus_messing"), "oor": T("techsaus_oor")}

    def elementen(hoogte):
        """hoogte: where the sauce stands inside the open top (y), 4 = the dark bottom of an empty vat."""
        return [
            doos((1, 0, 1), (15, 2, 15), "#hoepel"),                                  # the foot
            doos((2, 2, 2), (14, 15, 14), "#zij", north="#voor", south="#achter", up=None, down=None),
            doos((1, 5, 1), (15, 6, 15), "#hoepel"), doos((1, 11, 1), (15, 12, 15), "#hoepel"),   # two hoops
            # the open top: a rim of four staves and the sauce inside
            doos((2, 15, 2), (14, 16, 4), "#boven"), doos((2, 15, 12), (14, 16, 14), "#boven"),
            doos((2, 15, 4), (4, 16, 12), "#boven"), doos((12, 15, 4), (14, 16, 12), "#boven"),
            doos((4, hoogte - 1, 4), (12, hoogte, 12), "#saus", north=None, south=None, east=None, west=None, down=None),
            doos((4, hoogte, 4), (12, 16, 4.5), "#achter", up=None, down=None), doos((4, hoogte, 11.5), (12, 16, 12), "#achter", up=None, down=None),
            doos((4, hoogte, 4.5), (4.5, 16, 11.5), "#achter", up=None, down=None), doos((11.5, hoogte, 4.5), (12, 16, 11.5), "#achter", up=None, down=None),
            doos((7, 3, 0), (9, 5, 2), "#staal"), doos((7.5, 5, 0.5), (8.5, 7, 1.5), "#knop"),    # the tap and its handle
            *oren(16, z=2)]

    hoogtes = {0: 4, 1: 7, 2: 10, 3: 13, 4: 15}
    for niveau, hoogte in hoogtes.items():
        model(h, f"{n}_vorm_{niveau}", basis, elementen(hoogte), T(n))
    kind(h, f"{n}_leeg", f"{n}_vorm_0", {"voor": T(f"{n}_voor_slaapt")})
    for id in SAUZEN:
        for niveau in range(1, 5):
            kind(h, f"{n}_{id}_{niveau}", f"{n}_vorm_{niveau}", {
                "voor": T(f"{n}_voor_{'vol' if niveau == 4 else 'werkt'}"), "zij": T(f"{n}_zij_{id}_{niveau}"), "saus": T(f"techsaus_saus_{id}")})
    varianten = {}
    for f, r in ROT.items():
        for saus in ("leeg", *SAUZEN):
            for niveau in range(5):
                # (an empty vat with a level, or a sauce without one, never happens: both look empty)
                m = f"{n}_leeg" if saus == "leeg" or niveau == 0 else f"{n}_{saus}_{niveau}"
                varianten[f"facing={f},niveau={niveau},saus={saus}"] = {"model": T(m), **({"y": r} if r else {})}
    h.w(f"{h.A}/blockstates/{n}.json", {"variants": varianten})
    kind(h, n, f"{n}_vorm_2", {"voor": T(f"{n}_voor_werkt"), "zij": T(f"{n}_zij_kaassaus_2"), "saus": T("techsaus_saus_kaassaus")})   # in the hand
    return n


# =====================================================================================================================
# Brouwautomaat
# =====================================================================================================================
def brouwautomaat(h):
    n = "brouwautomaat"
    # the machine: the lower half of the block (y 0..8): texture rows 8..15
    for staat in STATEN:
        d = Doek(f"{n}_voor_{staat}")
        d.vlak(0, 8, 16, 16, ROZE, 5, rand=donker(ROZE))
        d.snoet(2, 10, staat)
        d.save(h)
    d = Doek(n)
    d.vlak(0, 8, 16, 16, ROZE, 6, rand=donker(ROZE))
    d.vlak(1, 13, 15, 14, PAARS, 3)                                                  # a purple band, like the Guhdrankjes
    for x in (3, 7, 11):                                                             # three little bottles in a rack
        d.vlak(x, 10, x + 2, 13, GLAS, 2)
        d.px(x, 9, HOEPEL).px(x + 1, 9, HOEPEL)
    d.save(h)
    d = Doek(f"{n}_boven", donker(ROZE, 1.08), 4)
    d.vlak(0, 0, 16, 16, donker(ROZE, 1.08), 4, rand=donker(ROZE))
    d.save(h)
    d = Doek(f"{n}_pan", PAN, 5)                                                     # the dark pan
    for x, y in ((2, 10), (7, 9), (12, 10), (4, 5), (10, 5)):
        d.px(x, y, PAN_LICHT)
    d.a[3, :, :3] = PAN_LICHT
    d.save(h)
    Doek(f"{n}_rand", PAN_LICHT, 4).save(h)
    d = Doek(f"{n}_bouillon", (242, 194, 60), 4)                                     # kaasbouillon with bubbles
    for x, y in ((4, 5), (9, 4), (11, 9), (6, 11), (8, 7)):
        d.px(x, y, (255, 232, 140))
    d.save(h)
    for staat in STATEN:
        model(h, f"{n}_{staat}", {"voor": T(f"{n}_voor_{staat}"), "lijf": T(n), "boven": T(f"{n}_boven"), "pan": T(f"{n}_pan"),
                                 "rand": T(f"{n}_rand"), "soep": T(f"{n}_bouillon"), "oor": T("techsaus_oor")}, [
            doos((0, 0, 0), (16, 8, 16), "#lijf", north="#voor", up="#boven", down="#boven"),
            doos((2, 8, 2), (14, 13, 14), "#pan", up=None, down=None),
            # the rim of the pan and what bubbles in it
            doos((1, 13, 1), (15, 15, 3), "#rand"), doos((1, 13, 13), (15, 15, 15), "#rand"),
            doos((1, 13, 3), (3, 15, 13), "#rand"), doos((13, 13, 3), (15, 15, 13), "#rand"),
            doos((3, 13, 3), (13, 14, 13), "#soep", north=None, south=None, east=None, west=None, down=None),
            doos((0, 10, 6), (2, 12, 10), "#rand"), doos((14, 10, 6), (16, 12, 10), "#rand"),      # two handles
            *oren(15, z=1)], T(n))
    return n


# =====================================================================================================================
# Frituurautomaat
# =====================================================================================================================
def frituurautomaat(h):
    n = "frituurautomaat"
    # the fryer: x 1..15, y 0..11: texture rows 5..15, columns 1..14
    for staat in STATEN:
        d = Doek(f"{n}_voor_{staat}")
        d.vlak(1, 5, 15, 16, ROZE, 5, rand=donker(ROZE))
        d.snoet(2, 7, staat)
        d.vlak(4, 12, 12, 14, STAAL_DONKER, 3)                                       # the vent: it glows when it fries
        for x in range(5, 11, 2):
            d.px(x, 12, ORANJE if staat != "slaapt" else donker(STAAL_DONKER, 1.3))
            d.px(x, 13, (255, 206, 96) if staat != "slaapt" else donker(STAAL_DONKER, 1.3))
        d.save(h)
    d = Doek(n)
    d.vlak(1, 5, 15, 16, ROZE, 6, rand=donker(ROZE))
    d.vlak(2, 12, 14, 14, ORANJE, 3)                                                 # an orange band
    for x, y in ((3, 7), (12, 7)):
        d.px(x, y, ROZE_LICHT)
    d.save(h)
    d = Doek(f"{n}_boven")
    d.vlak(1, 1, 15, 15, donker(ROZE, 1.08), 4, rand=donker(ROZE))
    d.save(h)
    d = Doek(f"{n}_mand")                                                            # the wire basket: a mesh with holes
    for i in range(4, 12):
        for j in range(4, 12):
            if i % 2 == 0 or j % 2 == 0:
                d.px(i, j, STAAL if (i + j) % 4 else donker(STAAL, 1.25))
    d.save(h)
    for staat in STATEN:
        model(h, f"{n}_{staat}", {"voor": T(f"{n}_voor_{staat}"), "lijf": T(n), "boven": T(f"{n}_boven"), "staal": T("techsaus_staal"),
                                 "saus": T("techsaus_saus_frituursaus"), "mand": T(f"{n}_mand"), "oor": T("techsaus_oor")}, [
            doos((1, 0, 1), (15, 11, 15), "#lijf", north="#voor", up="#boven", down="#boven"),
            # the steel rim around the sauce
            doos((1, 11, 1), (15, 13, 3), "#staal"), doos((1, 11, 13), (15, 13, 15), "#staal"),
            doos((1, 11, 3), (3, 13, 13), "#staal"), doos((13, 11, 3), (15, 13, 13), "#staal"),
            doos((3, 11, 3), (13, 12, 13), "#saus", north=None, south=None, east=None, west=None, down=None),
            # the basket hangs in it, its handle sticks out at the back
            doos((4, 12, 4), (12, 12.5, 12), "#mand", north=None, south=None, east=None, west=None),
            doos((7, 12.5, 11), (9, 14, 12), "#staal"), doos((7, 13, 12), (9, 14, 16), "#staal"),
            *oren(13, z=1)], T(n))
    return n


# =====================================================================================================================
# Grillkoolpers (two blocks high: the kern's model draws both)
# =====================================================================================================================
def grillkoolpers(h):
    n = "grillkoolpers"
    # the base: y 0..9: texture rows 7..15
    for staat in STATEN:
        d = Doek(f"{n}_voor_{staat}")
        d.vlak(0, 7, 16, 16, KOOL, 5, rand=donker(KOOL))
        d.vlak(1, 8, 15, 14, ROZE, 4, rand=donker(ROZE))                             # a pink face plate on the dark base
        d.snoet(2, 9, staat)
        d.save(h)
    d = Doek(n)
    d.vlak(0, 7, 16, 16, KOOL, 6, rand=donker(KOOL))
    d.vlak(2, 10, 7, 13, SAUZEN["frituursaus"], 3, rand=donker(KOOL, 0.6))          # two little windows: sauce and water
    d.vlak(9, 10, 14, 13, SAUZEN["water"], 3, rand=donker(KOOL, 0.6))
    d.save(h)
    d = Doek(f"{n}_boven", KOOL, 4)
    d.vlak(0, 0, 16, 16, donker(KOOL, 1.15), 4, rand=donker(KOOL))
    d.vlak(2, 2, 14, 14, STAAL_DONKER, 4, rand=donker(STAAL_DONKER))                 # the bed the block lies on
    d.save(h)
    d = Doek(f"{n}_kop")                                                             # the head (5 high): rows 0..4; its top and bottom
    d.vlak(0, 0, 16, 16, ROZE, 5, rand=donker(ROZE))
    d.vlak(0, 0, 16, 5, ROZE, 5, rand=donker(ROZE))
    d.vlak(2, 2, 14, 3, PAARS, 3)
    d.save(h)
    d = Doek(f"{n}_stempel", STAAL_DONKER, 4)                                        # the stamp
    d.vlak(0, 0, 16, 16, STAAL_DONKER, 4, rand=donker(STAAL_DONKER, 0.7))
    d.save(h)
    for perst in (False, True):
        plaat = 12 if perst else 19                                                   # the underside of the stamp
        blok = [doos((5, 10, 5), (11, 12, 11), "#kool")] if perst else []
        for staat in STATEN:
            model(h, f"{n}_{staat}{'_perst' if perst else ''}", {
                "voor": T(f"{n}_voor_{staat}"), "lijf": T(n), "boven": T(f"{n}_boven"), "kop": T(f"{n}_kop"), "staal": T("techsaus_staal"),
                "stempel": T(f"{n}_stempel"), "kool": "guhs:block/grillkool", "oor": T("techsaus_oor")}, [
                doos((0, 0, 0), (16, 9, 16), "#lijf", north="#voor", up="#boven", down="#boven"),
                doos((2, 9, 2), (14, 10, 14), "#boven", down=None),
                doos((1, 9, 1), (3, 24, 3), "#staal", up=None, down=None), doos((13, 9, 1), (15, 24, 3), "#staal", up=None, down=None),
                doos((1, 9, 13), (3, 24, 15), "#staal", up=None, down=None), doos((13, 9, 13), (15, 24, 15), "#staal", up=None, down=None),
                doos((0, 24, 0), (16, 29, 16), "#kop"),
                doos((7, plaat + 2, 7), (9, 24, 9), "#staal", up=None, down=None),    # the rod
                doos((4, plaat, 4), (12, plaat + 2, 12), "#stempel"),
                *blok,
                *oren(29, z=1)], T(n))
    varianten = {}
    for f, r in ROT.items():
        for staat in STATEN:
            for perst in (False, True):
                varianten[f"facing={f},perst={'true' if perst else 'false'},snoet={staat}"] = {
                    "model": T(f"{n}_{staat}{'_perst' if perst else ''}"), **({"y": r} if r else {})}
    h.w(f"{h.A}/blockstates/{n}.json", {"variants": varianten})
    return n


def machine_blockstate(h, n):
    h.w(f"{h.A}/blockstates/{n}.json", {"variants": {
        f"facing={f},snoet={staat}": {"model": T(f"{n}_{staat}"), **({"y": r} if r else {})} for f, r in ROT.items() for staat in STATEN}})


# the press is two blocks high: smaller in the hand and in the inventory
PERS_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -3.25, 0], "scale": [0.4, 0.4, 0.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.2, 0.2, 0.2]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -4, 0], "scale": [0.4, 0.4, 0.4]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0], "scale": [0.28, 0.28, 0.28]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
}


def build(h):
    """Every texture, model, blockstate and item model of the six blocks."""
    materialen(h)
    for maak in (sauspomp, brouwautomaat, frituurautomaat):
        n = maak(h)
        machine_blockstate(h, n)
        kind(h, n, f"{n}_werkt", {})                          # (the placeholder's model name: what the item shows)
        h.w(f"{h.A}/models/item/{n}.json", {"parent": T(n)})
    grillkoolpers(h)
    kind(h, "grillkoolpers", "grillkoolpers_werkt", {})
    h.w(f"{h.A}/models/item/grillkoolpers.json", {"parent": T("grillkoolpers"), "display": PERS_DISPLAY})
    sausvat(h)
    h.w(f"{h.A}/models/item/sausvat.json", {"parent": T("sausvat")})
    sausslang(h)
    h.w(f"{h.A}/models/item/sausslang.json", {"parent": T("sausslang")})
