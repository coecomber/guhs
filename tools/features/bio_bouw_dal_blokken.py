"""
biomes3 slice "bouw-dal": the small blocks (not in FEATURES; bio_bouw_dal.py calls build()).

  japan_*   the twelve blocks of the verzamelreeks (a block with an item; places anywhere, drops itself)
  weeb_*    the fixed things of het weebhuisje: the otaku collection and Evivads' mini-loket (blocks only: no item, no loot)
  japan_sushi / _ramen / _mochi / _onigiri   the four foods (item pictures)

Every block has one picture of its own: little colour cells (a Mod hands them out) plus the drawn bits (a guh face, a
poster, the text of a sign). Models are made for facing north: the front is the north side, the back stands against the
south side of the block.
"""
import numpy as np
from PIL import Image

from features import bio_lib as lib

REEKS = {  # id: Dutch name
    "japan_geluksguh": "Zwaaiende geluksguh", "japan_lampion": "Papieren lampion", "japan_mini_torii": "Mini-torii",
    "japan_ramenkom": "Ramenkom", "japan_daruma_guh": "Daruma-guh", "japan_waaier": "Waaier", "japan_theeservies": "Theeservies",
    "japan_kokeshi_guh": "Kokeshi-guh", "japan_koinobori": "Koinobori-vlag", "japan_bonsai_schaaltje": "Bonsai-schaaltje",
    "japan_windgong": "Windgong", "japan_maneki_knabbel": "Maneki-knabbel",
}
WEEB = {
    "weeb_figuurtjes": "Plank met guh-figuurtjes", "weeb_poster": "Poster", "weeb_mangastapel": "Stapel manga",
    "weeb_dakimakura": "Guh-dakimakura", "weeb_loket": "Mini-loket", "weeb_nummerautomaat": "Nummertjesautomaat",
    "weeb_loketbord": "Loketbordje", "weeb_briefje": "Briefje op de deur",
}
ETEN = {"japan_sushi": "Guh-sushi", "japan_ramen": "Ramen", "japan_mochi": "Mochi", "japan_onigiri": "Onigiri"}
SOORTEN = 4

FONT = {  # 3 x 5
    "A": "010101111101101", "B": "110101110101110", "C": "011100100100011", "D": "110101101101110", "E": "111100110100111",
    "F": "111100110100100", "G": "011100101101011", "H": "101101111101101", "I": "111010010010111", "J": "001001001101010",
    "K": "101101110101101", "L": "100100100100111", "M": "101111111101101", "N": "110101101101101", "O": "010101101101010",
    "P": "110101110100100", "Q": "010101101110011", "R": "110101110101101", "S": "011100010001110", "T": "111010010010010",
    "U": "101101101101111", "V": "101101101101010", "W": "101101111111101", "X": "101101010101101", "Y": "101101010010010",
    "Z": "111001010100111", "0": "111101101101111", "1": "010110010010111", "2": "110001010100111", "3": "110001010001110",
    "4": "101101111001001", "5": "111100110001110", "6": "011100111101111", "7": "111001010010010", "8": "111101111101111",
    "9": "111101111001110", ".": "000000000000010", "-": "000000111000000", "&": "010101010101011", "!": "010010010000010",
    ",": "000000000010100", ":": "000010000010000",
}


def tekst(a, x, y, regel, kleur):
    """Writes a line in the 3 x 5 font into the picture a (rows, columns, 4); returns the x after it."""
    for ch in regel.upper():
        if ch == " ":
            x += 2
            continue
        bits = FONT[ch]
        for i, bit in enumerate(bits):
            if bit == "1" and 0 <= y + i // 3 < a.shape[0] and 0 <= x + i % 3 < a.shape[1]:
                a[y + i // 3, x + i % 3] = tuple(kleur) + (255,)
        x += 4
    return x


def breedte(regel):
    return sum(2 if ch == " " else 4 for ch in regel) - 1


def midden(a, y, regel, kleur):
    tekst(a, (a.shape[1] - breedte(regel)) // 2, y, regel, kleur)


class Mod:
    """A block model with its own picture: colour cells of 4 x 4 pixels from the top, drawn pictures from the bottom."""

    def __init__(self, naam, px=32):
        self.naam, self.px = naam, px
        self.a = np.zeros((px, px, 4), np.uint8)
        self.cel = {}
        self.onder = px
        self.links = 0
        self.rij = 0
        self.el = []
        self.rng = lib.rng(naam)

    def _uv(self, x0, y0, x1, y1, rand=0.0):
        k = 16.0 / self.px
        return [round((x0 + rand) * k, 3), round((y0 + rand) * k, 3), round((x1 - rand) * k, 3), round((y1 - rand) * k, 3)]

    def kleur(self, rgb, ruis=5):
        rgb = tuple(rgb)
        if rgb not in self.cel:
            i = len(self.cel)
            cx, cy = (i % (self.px // 4)) * 4, (i // (self.px // 4)) * 4
            assert cy + 4 <= self.onder, f"{self.naam}: the picture is full"
            alpha = rgb[3] if len(rgb) > 3 else 255
            n = self.rng.integers(-ruis, ruis + 1, (4, 4, 1)) if ruis else 0
            self.a[cy:cy + 4, cx:cx + 4, :3] = np.clip(np.array(rgb[:3]) + n, 0, 255)
            self.a[cy:cy + 4, cx:cx + 4, 3] = alpha
            self.cel[rgb] = self._uv(cx, cy, cx + 4, cy + 4, 0.5)
        return self.cel[rgb]

    def plaatje(self, beeld):
        """Puts a drawn picture (rows, columns, 3 or 4) in the sheet; returns its uv."""
        beeld = np.asarray(beeld, np.uint8)
        hgt, w = beeld.shape[:2]
        if self.links + w > self.px or self.rij < hgt:
            self.onder -= hgt
            self.links = 0
            self.rij = hgt
        y0 = self.onder
        assert y0 >= ((len(self.cel) + self.px // 4 - 1) // (self.px // 4)) * 4, f"{self.naam}: the picture is full"
        x0 = self.links
        self.a[y0:y0 + hgt, x0:x0 + w, :beeld.shape[2]] = beeld
        if beeld.shape[2] == 3:
            self.a[y0:y0 + hgt, x0:x0 + w, 3] = 255
        self.links += w
        return self._uv(x0, y0, x0 + w, y0 + hgt)

    def doos(self, x0, y0, z0, x1, y1, z1, rgb, gloei=False, plat=False, rot=None, **vlakken):
        """A box in one colour; a keyword per face (north=uv, up=uv ...) gives that face a drawn picture."""
        uv = self.kleur(rgb)
        faces = {f: {"texture": "#t", "uv": vlakken.get(f, uv)} for f in ("down", "up", "north", "south", "west", "east")}
        e = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
        if gloei:
            e["light_emission"] = 15
        if plat or gloei:
            e["shade"] = False
        if rot:
            e["rotation"] = rot
        self.el.append(e)
        return e

    def vlak(self, x0, y0, x1, y1, z, uv, beide=True):
        """A flat picture standing in the x/y plane at depth z (seen from the north; from the south it is mirrored)."""
        faces = {"north": {"texture": "#t", "uv": [uv[2], uv[1], uv[0], uv[3]]}}
        if beide:
            faces["south"] = {"texture": "#t", "uv": uv}
        self.el.append({"from": [x0, y0, z], "to": [x1, y1, z], "shade": False, "faces": faces})

    def bewaar(self, h, model=None, extra=None):
        h.save(Image.fromarray(self.a, "RGBA"), "block", f"{self.naam}.png")
        t = {"t": f"guhs:block/{self.naam}", "particle": f"guhs:block/{self.naam}", **(extra or {})}
        h.w(f"{h.A}/models/block/{model or self.naam}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                              "textures": t, "elements": self.el})


def snoet(vacht, w=12, hgt=10, blos=(255, 150, 180), oog=(40, 28, 44)):
    """A guh face: two eyes with a glint, a little mouth, blush."""
    a = np.zeros((hgt, w, 3), np.uint8)
    a[:] = vacht
    ey = hgt // 2 - 1
    for ex in (w // 4 - 1, w - w // 4 - 1):
        a[ey:ey + 2, ex:ex + 2] = oog
        a[ey, ex] = (255, 255, 255)
    a[ey + 3, w // 2 - 1:w // 2 + 1] = oog
    a[ey + 2, 1:3] = blos
    a[ey + 2, w - 3:w - 1] = blos
    return a


def guhje(m, cx, y0, cz, vacht, s=1.0, oor=None, buik=None, zit=True):
    """A little guh: a body, a head with a face on the north side, two ears. cx, cz = its middle; s = its size."""
    oor = oor or vacht
    b, hd = 3.0 * s, 3.0 * s
    lijf = 4.0 * s if zit else 5.0 * s
    m.doos(cx - b, y0, cz - 2.5 * s, cx + b, y0 + lijf, cz + 2.5 * s, buik or vacht)
    m.doos(cx - hd, y0 + lijf, cz - 2.5 * s, cx + hd, y0 + lijf + 5 * s, cz + 2.5 * s, vacht, north=m.plaatje(snoet(vacht)))
    for sx in (-1, 1):
        ox = cx + sx * 2.0 * s
        m.doos(ox - 1.0 * s, y0 + lijf + 5 * s, cz - 0.5 * s, ox + 1.0 * s, y0 + lijf + 7 * s, cz + 0.5 * s, oor)
    return y0 + lijf + 7 * s


# =====================================================================================================================
# the verzamelreeks
# =====================================================================================================================
ROOD, GOUD, WIT, ZWART, HOUT, ROZE = (214, 52, 62), (244, 198, 70), (250, 246, 240), (44, 36, 46), (160, 110, 70), (246, 150, 186)
LAK, GROEN, CREME = (196, 44, 78), (96, 160, 84), (250, 236, 208)


def geluksguh(h):
    """A white lucky guh with a red collar and a golden bell; its paw waves (an animated flat picture)."""
    m = Mod("japan_geluksguh")
    m.doos(3, 0, 4, 13, 1, 12, ROOD)                                    # its little cushion
    guhje(m, 8, 1, 8, WIT, oor=(255, 190, 205))
    m.doos(4.8, 4.6, 5.3, 11.2, 5.4, 10.7, ROOD)                        # collar
    m.doos(7.3, 3.4, 4.9, 8.7, 4.8, 5.5, GOUD)                          # bell
    m.doos(4.4, 1, 4.6, 6.4, 3, 6, WIT)                                 # the paw that holds still
    m.el.append({"from": [10.2, 3, 5.2], "to": [15.2, 11, 5.2], "shade": False, "faces": {
        "north": {"texture": "#poot", "uv": [16, 0, 0, 16]}, "south": {"texture": "#poot", "uv": [0, 0, 16, 16]}}})
    m.bewaar(h, extra={"poot": "guhs:block/japan_geluksguh_poot"})
    # the paw: four frames, up - forward - up - back (5 x 8 model pixels drawn 16 x 16)
    strip = np.zeros((64, 16, 4), np.uint8)
    for f, dx in enumerate((0, 3, 0, -3)):
        for y in range(16):
            t = (15 - y) / 15.0
            mx = 6 + dx * t
            for x in range(16):
                if abs(x - mx - 1.5) <= (3.2 if y < 5 else 2.2):
                    strip[f * 16 + y, x] = (250, 246, 240, 255) if y > 1 else (255, 190, 205, 255)
        strip[f * 16 + 2, int(6 + dx) + 1, :3] = (255, 170, 190)
    h.save(Image.fromarray(strip, "RGBA"), "block", "japan_geluksguh_poot.png")
    h.w(f"{h.TEX}/block/japan_geluksguh_poot.png.mcmeta", {"animation": {"frametime": 6, "interpolate": False}})


def lampion(h):
    m = Mod("japan_lampion")
    m.doos(7.6, 13, 7.6, 8.4, 16, 8.4, ZWART)                           # the cord
    m.doos(5.5, 12, 5.5, 10.5, 13, 10.5, ZWART)
    for (r, y0, y1, c) in ((2.5, 11, 12, ROOD), (3.5, 10, 11, WIT), (4, 6, 10, ROOD), (3.5, 5, 6, WIT), (2.5, 4, 5, ROOD)):
        m.doos(8 - r, y0, 8 - r, 8 + r, y1, 8 + r, c, gloei=True)
    m.doos(5.5, 3, 5.5, 10.5, 4, 10.5, ZWART)
    m.doos(7.5, 0.5, 7.5, 8.5, 3, 8.5, GOUD)                            # the tassel
    m.bewaar(h)


def mini_torii(h):
    m = Mod("japan_mini_torii")
    m.doos(1, 0, 6, 15, 1, 10, (226, 222, 214))
    for x in (3, 11):
        m.doos(x, 1, 7, x + 2, 12, 9, LAK)
        m.doos(x - 0.5, 1, 6.5, x + 2.5, 2.5, 9.5, ZWART)
    m.doos(2, 8.5, 7.3, 14, 10, 8.7, LAK)
    m.doos(7, 10, 7.2, 9, 12, 8.8, CREME, north=m.plaatje(snoet(CREME, 8, 8)))
    m.doos(0.5, 12, 6.8, 15.5, 13.5, 9.2, LAK)
    m.doos(0, 13.5, 6.5, 16, 14.5, 9.5, (120, 124, 134))
    for x in (5, 9.5):
        m.doos(x, 14.5, 7.5, x + 1.5, 16, 8.5, ROZE)                    # the ears
    m.bewaar(h)


def ramenkom(h):
    m = Mod("japan_ramenkom")
    m.doos(5, 0, 5, 11, 1, 11, ROOD)
    m.doos(3.5, 1, 3.5, 12.5, 3, 12.5, WIT)
    m.doos(2.5, 3, 2.5, 13.5, 6, 13.5, WIT)
    for (x0, z0, x1, z1) in ((2.5, 2.5, 13.5, 3.5), (2.5, 12.5, 13.5, 13.5), (2.5, 3.5, 3.5, 12.5), (12.5, 3.5, 13.5, 12.5)):
        m.doos(x0, 5, z0, x1, 6.2, z1, ROOD)                             # the red rim
    soep = np.zeros((20, 20, 3), np.uint8)
    soep[:] = (222, 164, 84)
    rng = m.rng
    for i in range(9):                                                   # noodles
        y = 2 + i * 2
        for x in range(1, 19):
            if (x + i) % 5:
                soep[min(19, y + (x // 3) % 2), x] = (250, 228, 150)
    soep[3:9, 11:17] = (255, 255, 250)                                   # half an egg
    soep[5:7, 13:15] = (250, 190, 60)
    for (cx, cy) in ((5, 13), (13, 14)):                                 # two slices of narutomaki
        for y in range(-2, 3):
            for x in range(-2, 3):
                if abs(x) + abs(y) < 4:
                    soep[cy + y, cx + x] = (255, 250, 250)
        soep[cy, cx] = (246, 120, 160)
        soep[cy - 1, cx + 1] = (246, 120, 160)
    soep[10:13, 2:5] = (70, 140, 70)                                     # spring onion
    m.doos(3.5, 5.6, 3.5, 12.5, 6.05, 12.5, (222, 164, 84), up=m.plaatje(soep))
    m.doos(1, 6.2, 9, 15, 6.8, 9.8, HOUT, rot={"origin": [8, 6, 9.4], "axis": "y", "angle": 22.5})   # chopsticks
    m.doos(1, 6.2, 10.4, 15, 6.8, 11.2, HOUT, rot={"origin": [8, 6, 10.8], "axis": "y", "angle": 22.5})
    m.bewaar(h)


def daruma_guh(h):
    m = Mod("japan_daruma_guh")
    for (r, y0, y1) in ((3.5, 0, 1), (4.5, 1, 8), (4, 8, 10), (3, 10, 11)):
        m.doos(8 - r, y0, 8 - r, 8 + r, y1, 8 + r, ROOD)
    gezicht = snoet(CREME, 12, 10)
    gezicht[0, :] = GOUD
    gezicht[:, 0] = GOUD
    gezicht[:, -1] = GOUD
    gezicht[1, 2:5] = ZWART                                              # the stern eyebrows
    gezicht[1, 7:10] = ZWART
    m.doos(5, 4, 3.2, 11, 9, 3.6, CREME, north=m.plaatje(gezicht))
    m.doos(6.5, 1.5, 3.3, 9.5, 3.5, 3.6, GOUD)                           # the golden sign on its belly
    for x in (5, 9):
        m.doos(x, 11, 7.5, x + 2, 13, 8.5, ROOD)
    m.bewaar(h)


def waaier(h):
    m = Mod("japan_waaier")
    m.doos(5, 0, 6.5, 11, 1, 9.5, ZWART)
    m.doos(7.5, 1, 7.5, 8.5, 3, 8.5, ZWART)
    a = np.zeros((28, 28, 4), np.uint8)
    for y in range(28):
        for x in range(28):
            dx, dy = x - 13.5, 27 - y
            r = (dx * dx + dy * dy) ** 0.5
            hoek = np.degrees(np.arctan2(dy, dx))
            if 5 <= r <= 26 and 12 <= hoek <= 168:
                spaak = int(hoek // 13) % 2
                a[y, x] = (ROOD if r > 22 else WIT if spaak else ROZE) + (255,)
                if 14 < r < 17:
                    a[y, x] = GOUD + (255,)
            elif r < 5 and dy >= 0:
                a[y, x] = HOUT + (255,)
    m.vlak(1, 2, 15, 16, 8, m.plaatje(a))
    m.bewaar(h)


def theeservies(h):
    m = Mod("japan_theeservies")
    m.doos(1.5, 0, 3, 14.5, 1, 13, HOUT)                                 # the tray
    pot = (88, 150, 132)
    m.doos(4, 1, 5.5, 9, 5, 10.5, pot)
    m.doos(5, 5, 6.5, 8, 5.6, 9.5, pot)
    m.doos(6, 5.6, 7.5, 7, 6.4, 8.5, GOUD)
    m.doos(9, 2.5, 7.5, 11, 3.5, 8.5, pot)                               # the spout
    m.doos(4.5, 6, 7.6, 8.5, 7, 8.4, HOUT)                               # the handle over the top
    for (x, z) in ((11, 4.2), (11.2, 9.8)):
        m.doos(x, 1, z, x + 2.4, 3, z + 2.4, WIT)
        m.doos(x + 0.4, 2.9, z + 0.4, x + 2, 3.05, z + 2, (150, 190, 96))
    m.bewaar(h)


def kokeshi_guh(h):
    m = Mod("japan_kokeshi_guh")
    kimono = np.zeros((16, 10, 3), np.uint8)
    kimono[:] = ROOD
    for (x, y) in ((2, 3), (6, 6), (3, 10), (7, 12), (5, 1)):
        kimono[y, x] = WIT
        kimono[y, x + 1] = ROZE
        kimono[y + 1, x] = ROZE
    kimono[7:9, :] = GOUD                                                # the obi
    uv = m.plaatje(kimono)
    m.doos(5.5, 0, 5.5, 10.5, 8, 10.5, ROOD, north=uv, south=uv, west=uv, east=uv)
    m.doos(5, 8, 5.2, 11, 13, 10.8, CREME, north=m.plaatje(snoet(CREME)))
    m.doos(4.8, 12, 5, 11.2, 13.4, 11, ZWART)                            # the hair
    m.doos(4.8, 9, 9.5, 11.2, 12, 11, ZWART)
    for x in (5.2, 8.8):
        m.doos(x, 13.4, 7.5, x + 2, 15.2, 8.5, CREME)
    m.bewaar(h)


def koinobori(h):
    m = Mod("japan_koinobori")
    m.doos(6, 0, 6, 10, 1, 10, (226, 222, 214))
    m.doos(7.5, 1, 7.5, 8.5, 15, 8.5, HOUT)
    m.doos(7.2, 15, 7.2, 8.8, 16, 8.8, GOUD)

    def vis(kleur, buik):
        a = np.zeros((6, 16, 4), np.uint8)
        for y in range(6):
            for x in range(16):
                d = abs(y - 2.5)
                if x < 12 and d <= 2.6 - max(0, 2 - x) * 0.8:
                    a[y, x] = ((buik if (x + y) % 3 == 0 and x > 3 else kleur) + (255,))
                elif x >= 12 and d >= (15 - x) * 0.5 and d <= 2.6:
                    a[y, x] = kleur + (255,)
        a[1:3, 1:3] = (255, 255, 255, 255)
        a[2, 2] = ZWART + (255,)
        return a
    for i, (kleur, buik) in enumerate(((ZWART, (120, 120, 130)), (ROOD, (255, 190, 190)), ((70, 120, 200), (190, 215, 250)))):
        m.vlak(8.5, 11.5 - i * 3.6, 16, 14.5 - i * 3.6, 8, m.plaatje(vis(kleur, buik)))
    m.bewaar(h)


def bonsai_schaaltje(h):
    m = Mod("japan_bonsai_schaaltje")
    schaal = (90, 110, 150)
    m.doos(2, 0, 4, 14, 1, 12, schaal)
    m.doos(1.5, 1, 3.5, 14.5, 3, 12.5, schaal)
    m.doos(2.3, 2.8, 4.3, 13.7, 3.1, 11.7, (120, 150, 80))               # moss
    m.doos(9.5, 3, 6, 12.5, 5, 9, (200, 200, 204))                       # a stone
    m.doos(4.5, 3, 7.2, 6, 7, 8.7, HOUT)                                 # the trunk leans
    m.doos(5.5, 6, 7.2, 8, 7.5, 8.7, HOUT)
    m.doos(7, 7, 7.2, 8.5, 9.5, 8.7, HOUT)
    naald = (58, 132, 78)
    m.doos(2.5, 7, 5.5, 6.5, 8.5, 10.5, naald)
    m.doos(6, 9.5, 5.5, 11.5, 11, 10.5, naald)
    m.doos(7, 11, 6.5, 10.5, 12, 9.5, naald)
    m.bewaar(h)


def windgong(h):
    m = Mod("japan_windgong")
    glas = (170, 222, 240)
    m.doos(7.7, 13, 7.7, 8.3, 16, 8.3, ZWART)
    m.doos(6, 12, 6, 10, 13, 10, glas)
    m.doos(5, 8, 5, 11, 12, 11, glas)
    m.doos(5, 8, 4.95, 11, 9, 5, ROZE)                                   # a painted band
    m.doos(7.7, 5, 7.7, 8.3, 8, 8.3, ZWART)                              # the clapper's string
    m.doos(7.2, 6.5, 7.2, 8.8, 7.5, 8.8, GOUD)
    strook = np.zeros((16, 6, 4), np.uint8)
    strook[:] = CREME + (255,)
    strook[2:14:3, 2:4] = ROOD + (255,)
    m.vlak(6.5, 0.5, 9.5, 5, 8, m.plaatje(strook))                       # the paper strip the wind catches
    m.bewaar(h)


def maneki_knabbel(h):
    """A golden guh that holds a big kaasknabbel in both paws."""
    m = Mod("japan_maneki_knabbel")
    m.doos(3.5, 0, 4.5, 12.5, 1, 11.5, ROOD)
    guhje(m, 8, 1, 8.5, GOUD, oor=(250, 220, 120))
    kaas = np.zeros((8, 10, 3), np.uint8)
    kaas[:] = (250, 206, 70)
    for (x, y) in ((2, 2), (6, 4), (4, 6), (8, 1)):
        kaas[y, x] = (214, 150, 40)
    m.doos(5.5, 2, 4.4, 10.5, 5.5, 6, (250, 206, 70), north=m.plaatje(kaas))
    for x in (4.6, 9.9):
        m.doos(x, 2.6, 4.8, x + 1.5, 4.4, 6.2, GOUD)
    m.bewaar(h)


# =====================================================================================================================
# het weebhuisje: the collection and the mini-loket
# =====================================================================================================================
FIGUREN = [  # per look of the shelf: three figurines (fur, ears, something special)
    [((246, 150, 186), None), ((150, 200, 240), None), ((250, 246, 240), "doos")],
    [((190, 160, 240), None), ((250, 206, 70), "doos"), ((246, 150, 186), None)],
    [((120, 210, 170), "doos"), ((246, 150, 186), None), ((240, 130, 90), None)],
    [((60, 56, 70), None), ((250, 246, 240), None), ((246, 120, 160), "doos")],
]


def figuurtjes(h):
    for soort, rij in enumerate(FIGUREN):
        m = Mod(f"weeb_figuurtjes_{soort}", px=64)
        m.doos(0, 0, 8, 16, 1, 16, HOUT)                                 # the shelf and its back
        m.doos(0, 1, 15, 16, 16, 16, (214, 60, 100))
        for i, (vacht, extra) in enumerate(rij):
            cx = 2.8 + i * 5.2
            guhje(m, cx, 1 + (1 if extra else 0), 11.5, vacht, s=0.62)
            if extra:                                                    # the limited edition stands on a golden base
                m.doos(cx - 2.3, 1, 9.2, cx + 2.3, 2, 13.8, GOUD)
        m.bewaar(h)


def posters(h):
    def idool(a):
        a[:] = (255, 196, 220)
        for (x, y) in ((3, 4), (26, 6), (5, 24), (27, 22), (15, 2)):
            a[y, x] = a[y - 1, x] = a[y + 1, x] = a[y, x - 1] = a[y, x + 1] = (255, 250, 200)
        a[8:22, 9:23] = (246, 150, 186)
        a[5:9, 10:14] = a[5:9, 18:22] = (246, 150, 186)
        a[12:16, 11:15] = a[12:16, 17:21] = (40, 28, 44)
        a[12:14, 11:13] = a[12:14, 17:19] = (255, 255, 255)
        a[18, 15:17] = (40, 28, 44)
        a[17:19, 9:11] = a[17:19, 21:23] = (255, 110, 150)
        midden_(a, 25, "GUH-CHAN", (214, 52, 100))

    def trein(a):
        a[:] = (150, 208, 246)
        a[20:, :] = (120, 190, 120)
        a[23:25, :] = (90, 90, 100)
        for x in range(2, 30):
            neus = max(0, 9 - x) // 2
            a[13 + neus:22, x] = (250, 250, 252)
        a[18:20, 4:30] = (40, 90, 200)
        for x in range(11, 29, 4):
            a[14:17, x:x + 3] = (60, 70, 90)
        a[22, 6:28:5] = (40, 36, 46)
        midden_(a, 3, "OP TIJD", (40, 60, 150))

    def fuji(a):
        a[:] = (255, 220, 200)
        a[4:9, 22:27] = (236, 80, 70)                                    # the sun
        for y in range(8, 26):
            w = (y - 8) * 0.9 + 1
            for x in range(32):
                if abs(x - 14) <= w:
                    a[y, x] = (250, 250, 252) if y < 13 else (110, 130, 190)
        a[26:, :] = (120, 180, 130)
        for (x, y) in ((3, 20), (6, 23), (26, 21), (28, 25), (23, 24), (2, 26)):
            a[y - 1:y + 1, x - 1:x + 1] = (255, 170, 200)

    def kawaii(a):
        a[:] = (190, 170, 240)
        for i in range(0, 32, 4):
            a[:, i] = (204, 186, 246)
        a[6:24, 5:27] = (250, 246, 240)
        a[3:7, 6:11] = a[3:7, 21:26] = (250, 246, 240)
        a[11:17, 8:13] = a[11:17, 19:24] = (40, 28, 44)
        a[11:13, 8:10] = a[11:13, 19:21] = (255, 255, 255)
        a[15, 11] = a[15, 22] = (255, 255, 255)
        a[19, 15:17] = (40, 28, 44)
        a[18:20, 6:8] = a[18:20, 24:26] = (255, 150, 180)
        midden_(a, 26, "KAWAII", (255, 250, 250))

    def midden_(a, y, regel, kleur):
        b = np.zeros(a.shape[:2] + (4,), np.uint8)
        midden(b, y, regel, kleur)
        a[b[..., 3] > 0] = kleur
    for soort, teken in enumerate((idool, trein, fuji, kawaii)):
        m = Mod(f"weeb_poster_{soort}", px=64)
        a = np.zeros((32, 32, 3), np.uint8)
        teken(a)
        a[0, :] = a[-1, :] = a[:, 0] = a[:, -1] = (250, 250, 250)
        m.doos(1, 1, 15.6, 15, 15, 16, (250, 250, 250), north=m.plaatje(a))
        m.bewaar(h)


def mangastapel(h):
    m = Mod("weeb_mangastapel")
    y = 0
    for i, (kleur, dx, dz, hoogte) in enumerate((((214, 60, 100), 0, 0, 2), ((80, 140, 220), 1, -1, 1.5), ((250, 206, 70), -0.5, 0.5, 2),
                                                 ((120, 200, 150), 0.5, 0, 1.5), ((190, 150, 240), -1, -0.5, 2), ((246, 150, 186), 0.5, 1, 1.5))):
        m.doos(3.5 + dx, y, 4 + dz, 10.5 + dx, y + hoogte, 13 + dz, kleur)
        m.doos(3.7 + dx, y + 0.25, 3.9 + dz, 10.3 + dx, y + hoogte - 0.25, 4 + dz, WIT)
        y += hoogte
    kaft = np.zeros((18, 14, 3), np.uint8)
    kaft[:] = (246, 150, 186)
    kaft[3:11, 3:11] = snoet((250, 246, 240), 8, 8)
    b = np.zeros((18, 14, 4), np.uint8)
    midden(b, 12, "GUH", (255, 255, 255))
    kaft[b[..., 3] > 0] = (255, 255, 255)
    m.el[-2]["faces"]["up"]["uv"] = m.plaatje(kaft)
    m.doos(11.5, 0, 6, 14.5, 1.2, 12, (250, 250, 250))                   # one lies open beside the pile
    m.bewaar(h)


def dakimakura(h):
    """The long guh pillow, lying along the wall: the guh is printed on top."""
    m = Mod("weeb_dakimakura", px=64)
    a = np.zeros((28, 10, 3), np.uint8)
    a[:] = (255, 214, 230)
    a[3:12, 1:9] = (246, 150, 186)                                       # the head
    a[1:4, 1:4] = a[1:4, 6:9] = (246, 150, 186)
    a[6:8, 2:4] = a[6:8, 6:8] = (40, 28, 44)
    a[6, 2] = a[6, 6] = (255, 255, 255)
    a[9, 4:6] = (40, 28, 44)
    a[8:10, 1] = a[8:10, 8] = (255, 110, 150)
    a[12:24, 2:8] = (246, 150, 186)                                      # the body, with a little kimono
    a[13:21, 2:8] = (214, 52, 62)
    a[16:18, 2:8] = (244, 198, 70)
    a[24:26, 2:4] = a[24:26, 6:8] = (246, 150, 186)
    m.doos(1.5, 0, 9, 14.5, 3, 15, (255, 214, 230), up={"dummy": 0})
    uv = m.plaatje(a)
    m.el[-1]["faces"]["up"] = {"texture": "#t", "uv": uv, "rotation": 90}
    m.bewaar(h)


def loket(h):
    m = Mod("weeb_loket", px=64)
    voor = np.zeros((20, 32, 3), np.uint8)
    voor[:] = (226, 196, 150)
    voor[0:2, :] = voor[-2:, :] = (160, 110, 70)
    voor[4:13, 4:28] = (250, 246, 240)
    b = np.zeros((20, 32, 4), np.uint8)
    midden(b, 6, "LOKET", (196, 44, 78))
    voor[b[..., 3] > 0] = (196, 44, 78)
    voor[15:17, 6:26] = (196, 44, 78)
    m.doos(0, 0, 4, 16, 10, 12, (226, 196, 150), north=m.plaatje(voor))
    m.doos(0, 10, 3, 16, 11, 13, HOUT)                                   # the counter top
    m.doos(2, 11, 5, 6, 11.6, 9, (60, 60, 150))                          # the ink pad
    for (x, z, kop) in ((7.5, 5.5, ROOD), (9.8, 7.5, (70, 120, 200))):   # two stamps
        m.doos(x, 11, z, x + 1.8, 12.2, z + 1.8, HOUT)
        m.doos(x + 0.4, 12.2, z + 0.4, x + 1.4, 14, z + 1.4, kop)
    for i in range(3):                                                   # a pile of passports
        m.doos(11.5 + i * 0.3, 11 + i * 0.5, 8 - i * 0.3, 15 + i * 0.3, 11.5 + i * 0.5, 12 - i * 0.3, (160, 40, 80) if i % 2 == 0 else (190, 60, 100))
    m.doos(12.6, 12.5, 9, 14.4, 12.55, 10.4, GOUD)
    m.bewaar(h)


def nummerautomaat(h):
    m = Mod("weeb_nummerautomaat", px=64)
    m.doos(5, 0, 5, 11, 1, 11, (90, 90, 100))
    m.doos(7.2, 1, 7.2, 8.8, 9, 8.8, (150, 150, 160))
    kast = np.zeros((14, 16, 3), np.uint8)
    kast[:] = ROOD
    kast[2:8, 2:14] = (30, 40, 34)
    b = np.zeros((14, 16, 4), np.uint8)
    midden(b, 2, "047", (120, 255, 140))
    kast[b[..., 3] > 0] = (120, 255, 140)
    kast[10:12, 4:12] = (40, 30, 36)
    m.doos(4, 9, 5.5, 12, 16, 10.5, ROOD, north=m.plaatje(kast))
    m.doos(6.5, 8.2, 5.2, 9.5, 11, 5.5, WIT)                             # the next ticket sticks out
    m.bewaar(h)


def loketbord(h):
    for stand, regels, kleur in (("open", ("LOKET OPEN", "TREK EEN", "NUMMERTJE"), (60, 150, 90)),
                                 ("dicht", ("LOKET", "GESLOTEN", "WEGENS JAPAN"), (214, 52, 62))):
        m = Mod(f"weeb_loketbord_{stand}", px=64)
        a = np.zeros((26, 52, 3), np.uint8)
        a[:] = (250, 246, 236)
        a[0:2, :] = a[-2:, :] = kleur
        a[:, 0:2] = a[:, -2:] = kleur
        b = np.zeros((26, 52, 4), np.uint8)
        for i, regel in enumerate(regels):
            midden(b, 4 + i * 6, regel, kleur if i == 0 else (60, 50, 60))
        a[b[..., 3] > 0] = b[b[..., 3] > 0][:, :3]
        uv = m.plaatje(a)
        m.doos(1.5, 1, 7.5, 14.5, 7.5, 8.5, (250, 246, 236), north=uv, south=uv)
        m.doos(3, 0, 6.5, 5, 1, 9.5, HOUT)
        m.doos(11, 0, 6.5, 13, 1, 9.5, HOUT)
        m.bewaar(h)


BRIEFJE = ("ZIJN EVEN NAAR", "JAPAN. MORGEN", "TERUG. NIET AAN", "DE FIGUURTJES", "KOMEN.", "- E & N")


def briefje(h):
    m = Mod("weeb_briefje_aan", px=64)
    a = np.zeros((48, 62, 3), np.uint8)
    a[:] = (255, 250, 226)
    a[:, 0] = a[:, -1] = a[0, :] = a[-1, :] = (226, 214, 180)
    b = np.zeros((48, 62, 4), np.uint8)
    for i, regel in enumerate(BRIEFJE):
        if i == len(BRIEFJE) - 1:
            tekst(b, 62 - breedte(regel) - 4, 6 + i * 7, regel, (196, 44, 78))
        else:
            tekst(b, 3, 5 + i * 7, regel, (60, 50, 60))
    a[b[..., 3] > 0] = b[b[..., 3] > 0][:, :3]
    a[0:3, 26:36] = (246, 150, 186)                                      # a bit of pink tape
    m.doos(2, 3, 15.7, 14.5, 12.7, 15.9, (255, 250, 226), north=m.plaatje(a))
    m.bewaar(h)
    leeg = Mod("weeb_briefje_uit")
    leeg.kleur((255, 250, 226))
    leeg.bewaar(h)


# =====================================================================================================================
# blockstates, items, loot, the foods
# =====================================================================================================================
def _draai(model, extra=""):
    return {f"facing={f}{extra}": {"model": f"guhs:block/{model}", **({"y": y} if y else {})}
            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}


ETEN_PLAATJES = {
    "japan_sushi": (["................", "................", "................", "................", "...oooo..oooo...", "..orrrro.owwwo..",
                     "..orrrroowkkwo..", "..wwwwwwwwkgwo..", "..wwwwwwowkkwo..", "..wkkkkw.owwwo..", "..wwwwww..oooo..", "...dddd.........",
                     "................", "................", "................", "................"],
                    {"o": (60, 44, 50), "r": (250, 130, 110), "w": (252, 250, 244), "k": (40, 60, 44), "g": (246, 150, 186), "d": (200, 196, 188)}),
    "japan_ramen": (["................", "..........h.h...", ".........h.h....", "........h.h.....", "..rrrrrrhrhrrr..", ".rnnnnnhnhnnnnr.",
                     ".rnyynnnnnppnnr.", ".rnyynggnnppnnr.", ".rrnnnnnnnnnnrr.", "..wwwwwwwwwwww..", "..wwwwwwwwwwww..", "...wwwwwwwwww...",
                     "....wwwwwwww....", ".....rrrrrr.....", "................", "................"],
                    {"r": (214, 52, 62), "n": (250, 226, 150), "y": (250, 190, 60), "g": (80, 150, 80), "p": (246, 150, 186), "w": (250, 246, 240),
                     "h": (160, 110, 70)}),
    "japan_mochi": (["................", "................", "................", "................", "................", ".....pppp.......",
                     "....pppppp..gg..", "...pwpppppgggg..", "...pppppppgggg..", "..wwwwpppgggggg.", ".wwwwwwwpggggg..", ".wwwwwwww.......",
                     ".wwwwwwww.......", "..wwwwww........", "................", "................"],
                    {"p": (250, 170, 200), "w": (252, 250, 246), "g": (150, 200, 120)}),
    "japan_onigiri": (["................", "................", ".......ww.......", "......wwww......", ".....wwwwww.....", ".....wwwwww.....",
                       "....wwwppwww....", "....wwwppwww....", "...wwwwwwwwww...", "...wwkkkkkkww...", "..wwwkkkkkkwww..", "..wwwkkkkkkwww..",
                       "..wwwkkkkkkwww..", "...wwwwwwwwww...", "................", "................"],
                      {"w": (252, 250, 244), "k": (40, 60, 44), "p": (246, 120, 150)}),
}


def build(h):
    for maak in (geluksguh, lampion, mini_torii, ramenkom, daruma_guh, waaier, theeservies, kokeshi_guh, koinobori, bonsai_schaaltje, windgong,
                 maneki_knabbel, figuurtjes, posters, mangastapel, dakimakura, loket, nummerautomaat, loketbord, briefje):
        maak(h)
    A, w = h.A, h.w
    for b in REEKS:
        w(f"{A}/blockstates/{b}.json", {"variants": _draai(b)})
        w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
        h.self_drop(b)
    for b in ("weeb_mangastapel", "weeb_dakimakura", "weeb_loket", "weeb_nummerautomaat"):
        w(f"{A}/blockstates/{b}.json", {"variants": _draai(b)})
    for b in ("weeb_figuurtjes", "weeb_poster"):
        variants = {}
        for i in range(SOORTEN):
            variants.update(_draai(f"{b}_{i}", f",soort={i}"))
        w(f"{A}/blockstates/{b}.json", {"variants": variants})
    w(f"{A}/blockstates/weeb_loketbord.json", {"variants": {**_draai("weeb_loketbord_open", ",aan=true"), **_draai("weeb_loketbord_dicht", ",aan=false")}})
    w(f"{A}/blockstates/weeb_briefje.json", {"variants": {**_draai("weeb_briefje_aan", ",aan=true"), **_draai("weeb_briefje_uit", ",aan=false")}})
    for item, (rows, pal) in ETEN_PLAATJES.items():
        h.save(h.grid(rows, {k: v + (255,) for k, v in pal.items()}), "item", f"{item}.png")
        h.item_model(item)
    h.add_tag("guhs/tags/item/band/snacks", [f"guhs:{e}" for e in ETEN])
    lib.teksten(h, {f"block.guhs.{b}": naam for b, naam in {**REEKS, **WEEB}.items()})
    lib.teksten(h, {f"item.guhs.{e}": naam for e, naam in ETEN.items()})
