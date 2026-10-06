"""
Guhbioscoop films, part 3: the three stories as films: Baltoguh, Guhtwo (the kloon-eiland) and 626 (Ohana op Guhwai'i). A
player has them when the story is finished (the progress behind the titles Held van Nomguh / Vriend van Guhtwo /
Ohana-guh). See guhpixel_bioscoop_film.py for the format. Dutch here; English in tools/lang/en/c37_px_bioscoop.json.
"""
import numpy as np
from PIL import ImageDraw

from features import guhpixel_bioscoop_tex as tex
from features.guhpixel_bioscoop_film import B, H, Film, aftiteling, guh, titelkaart

GY = 32      # a guh that stands on a floor at y 50


# =====================================================================================================================
# pictures
# =====================================================================================================================
def bergen(seed=31, kleur=(70, 86, 130), top=(236, 240, 250)):
    """A row of mountains, twice the picture wide (it scrolls)."""
    rng = np.random.default_rng(seed)
    w = B * 2
    im = tex.nieuw(w, 30)
    d = ImageDraw.Draw(im)
    x = -10
    while x < w:
        b, hgt = int(rng.integers(26, 44)), int(rng.integers(16, 29))
        d.polygon([(x, 29), (x + b // 2, 29 - hgt), (x + b, 29)], fill=tex.rgb(kleur))
        d.polygon([(x + b // 2 - 4, 29 - hgt + 6), (x + b // 2, 29 - hgt), (x + b // 2 + 4, 29 - hgt + 6)], fill=tex.rgb(top))
        x += int(b * 0.7)
    return im


def sneeuw(seed):
    """Snowflakes, one picture high and wide (two copies scroll past each other)."""
    rng = np.random.default_rng(seed)
    im = tex.nieuw(B, H)
    for _ in range(40):
        im.putpixel((int(rng.integers(0, B)), int(rng.integers(0, H))), (255, 255, 255, 255))
    return im


def hut():
    im = tex.nieuw(34, 26)
    d = ImageDraw.Draw(im)
    d.rectangle((3, 10, 30, 25), fill=(130, 88, 54, 255))
    for y in range(13, 26, 4):
        d.line([(3, y), (30, y)], fill=(104, 68, 40, 255))
    d.polygon([(0, 11), (17, 0), (33, 11)], fill=(240, 244, 252, 255))
    d.rectangle((13, 15, 20, 25), fill=(80, 50, 30, 255))
    d.rectangle((23, 14, 28, 19), fill=(255, 220, 120, 255))
    return im


def slee():
    return tex.raster(["...........bbbbbb.....", "..rrrrrrrrrrrrrrr.b...", "..rRRRRRRRRRRRRRr.b...", "..rrrrrrrrrrrrrrr.b...", "...b.....b.....b..b...",
                       "bbbbbbbbbbbbbbbbbbb..."],
                      {"r": (176, 60, 50), "R": (214, 90, 76), "b": (110, 74, 46)})


def medkist():
    return tex.raster(["bbbbbbbbbbb", "bwwwwwwwwwb", "bwwwwrwwwwb", "bwwwrrrwwwb", "bwwwwrwwwwb", "bwwwwwwwwwb", "bbbbbbbbbbb"],
                      {"b": (150, 104, 60), "w": (250, 250, 250), "r": (226, 50, 60)})


def ziekenhuisje():
    im = tex.nieuw(36, 28)
    d = ImageDraw.Draw(im)
    d.rectangle((2, 10, 33, 27), fill=(244, 244, 250, 255))
    d.polygon([(0, 11), (18, 0), (35, 11)], fill=(214, 220, 236, 255))
    d.rectangle((14, 17, 21, 27), fill=(150, 104, 70, 255))
    d.rectangle((5, 14, 10, 19), fill=(255, 220, 120, 255))
    im.alpha_composite(tex.hart((247, 120, 180)), (25, 13))
    return im


def maan():
    im = tex.nieuw(12, 12)
    d = ImageDraw.Draw(im)
    d.ellipse((0, 0, 11, 11), fill=(250, 244, 210, 255))
    d.ellipse((3, 2, 5, 4), fill=(226, 218, 180, 255))
    d.ellipse((7, 6, 9, 8), fill=(226, 218, 180, 255))
    return im


def tank(heel, wie=None):
    """The kloontank: a glass tube full of pink knabbelsap on a metal foot (cracked and nearly empty while it is broken)."""
    im = tex.nieuw(30, 46)
    d = ImageDraw.Draw(im)
    d.rectangle((2, 4, 27, 41), fill=(196, 228, 246, 255))
    sap = 8 if heel else 34
    d.rectangle((3, sap, 26, 41), fill=(250, 150, 200, 255))
    d.line([(3, sap), (26, sap)], fill=(255, 200, 228, 255))
    if wie is not None:
        im.alpha_composite(wie, (4, 18))
        for (x, y) in ((6, 12), (22, 15), (10, 38)):       # bubbles
            d.ellipse((x, y, x + 2, y + 2), outline=(255, 230, 242, 255))
    if not heel:
        d.line([(8, 6), (12, 14), (9, 20), (14, 28)], fill=(90, 110, 130, 255))
        d.line([(22, 8), (19, 16), (23, 24)], fill=(90, 110, 130, 255))
    d.line([(4, 6), (4, 30)], fill=(255, 255, 255, 255))
    d.rectangle((0, 0, 29, 4), fill=(96, 100, 120, 255))
    d.rectangle((0, 41, 29, 45), fill=(96, 100, 120, 255))
    for x in (4, 12, 20):
        d.rectangle((x, 43, x + 2, 44), fill=(110, 220, 130, 255) if heel else (230, 70, 70, 255))
    return im


def computer():
    return tex.raster(["bbbbbbbbbbbbbb", "bssssssssssssb", "bsgsssssgsssb."[:13] + "b", "bssgsgsgssssb."[:13] + "b", "bssssgsssssssb", "bbbbbbbbbbbbbb",
                       ".....bbbb.....", "..bbbbbbbbbb.."],
                      {"b": (200, 190, 160), "s": (24, 40, 36), "g": (110, 230, 140)})


def notitie():
    return tex.raster(["wwwwwww.", "wdddddww", "wwwwwwww", "wddddwww", "wwwwwwww", "wdddddww", "wwwwwwww"], {"w": (250, 246, 226), "d": (150, 140, 170)})


def tandwiel():
    return tex.raster(["..g.g.g..", ".ggggggg.", "ggg...ggg", ".gg...gg.", "ggg...ggg", ".ggggggg.", "..g.g.g.."], {"g": (170, 176, 196)})


def schaal(vol=True):
    im = tex.nieuw(34, 14)
    d = ImageDraw.Draw(im)
    if vol:
        for i, x in enumerate(range(4, 28, 5)):
            d.ellipse((x, 1 + i % 2, x + 6, 7 + i % 2), fill=(255, 196, 70, 255), outline=(236, 150, 40, 255))
    d.pieslice((0, -6, 33, 13), 0, 180, fill=(196, 150, 210, 255))
    d.line([(1, 4), (32, 4)], fill=(226, 190, 236, 255))
    return im


def palm():
    im = tex.nieuw(26, 36)
    d = ImageDraw.Draw(im)
    for y in range(10, 36):
        x = 12 + int((35 - y) * 0.12)
        d.rectangle((x, y, x + 2, y), fill=(150, 104, 60, 255) if y % 4 else (120, 80, 44, 255))
    for (x0, y0, x1, y1) in ((14, 10, 0, 6), (14, 10, 2, 16), (14, 10, 25, 5), (14, 10, 24, 16), (14, 10, 13, 0)):
        d.line([(x0, y0), (x1, y1)], fill=(60, 150, 70, 255), width=3)
        d.line([(x0, y0 - 1), (x1, y1 - 1)], fill=(96, 190, 96, 255))
    d.ellipse((12, 10, 15, 13), fill=(110, 74, 46, 255))
    return im


def capsule():
    im = tex.nieuw(26, 16)
    d = ImageDraw.Draw(im)
    d.ellipse((0, 2, 25, 15), fill=(200, 60, 60, 255))
    d.ellipse((6, 0, 19, 10), fill=(170, 220, 250, 255))
    d.line([(3, 12), (22, 12)], fill=(150, 40, 44, 255))
    for x in (4, 12, 20):
        d.rectangle((x, 13, x + 1, 14), fill=(250, 220, 100, 255))
    return im


def vallende_ster():
    return tex.raster(["........yy", "......yyww", "....yyywwy", "..yyyy.yy.", "yyy......."], {"y": (255, 226, 120), "w": (255, 255, 255)})


def paalhuisje():
    im = tex.nieuw(38, 34)
    d = ImageDraw.Draw(im)
    for x in (5, 31):
        d.rectangle((x, 20, x + 1, 33), fill=(120, 84, 52, 255))
    d.rectangle((2, 10, 35, 21), fill=(226, 190, 130, 255))
    d.polygon([(0, 11), (19, 0), (37, 11)], fill=(120, 150, 80, 255))
    d.rectangle((16, 13, 22, 21), fill=(110, 74, 46, 255))
    d.rectangle((26, 13, 31, 17), fill=(170, 220, 250, 255))
    d.line([(8, 22), (4, 33)], fill=(150, 104, 60, 255))
    return im


def rommel(soort):
    if soort == 0:      # a cushion
        return tex.raster([".pppppp.", "pPPPPPPp", "pPPwPPPp", ".pppppp."], {"p": (240, 120, 170), "P": (250, 170, 204), "w": (255, 255, 255)})
    if soort == 1:      # a sock from the washing line
        return tex.raster(["bbb..", "bbb..", "bbb..", "bbbbb", ".bbbb"], {"b": (120, 190, 240)})
    return kokosnoot()


def kokosnoot():
    return tex.raster([".bbbbb.", "bBBBBBb", "bBdBdBb", "bBBdBBb", "bBBBBBb", ".bbbbb."], {"b": (110, 74, 46), "B": (150, 104, 66), "d": (60, 40, 26)})


def hibiscus():
    return tex.raster(["..p.p..", ".pPpPp.", "pPPyPPp", ".pPyPp.", "..pPp..", "...g..."], {"p": (240, 90, 150), "P": (255, 150, 190), "y": (255, 226, 100), "g": (70, 150, 80)})


def zon():
    im = tex.nieuw(16, 16)
    ImageDraw.Draw(im).ellipse((0, 0, 15, 15), fill=(255, 196, 90, 255))
    ImageDraw.Draw(im).ellipse((3, 3, 12, 12), fill=(255, 226, 130, 255))
    return im


# =====================================================================================================================
def balto():
    f = Film("balto", "Baltoguh: De Film", "De guhbaby's van Nomguh zijn ziek. Alleen Baltoguh durft door de sneeuwstorm.",
             "Word de Held van Nomguh: speel het verhaal van Baltoguh uit.")
    for naam, im in (("bergen", bergen()), ("sneeuw_a", sneeuw(41)), ("sneeuw_b", sneeuw(42)), ("hut", hut()), ("slee", slee()), ("medkist", medkist()),
                     ("ziekenhuisje", ziekenhuisje()), ("maan", maan())):
        f.sprite(naam, im)
    wit = (236, 236, 230)
    b_open, b_blij, b_boos, b_slaap = (guh("balto", o, oren="punt", binnen=wit) for o in ("open", "blij", "boos", "dicht"))
    steele, steele_schrik = guh("steele", "mika", "tand", oren="punt"), guh("steele", "schrik", oren="punt")
    rosy = guh("sneeuw", "open", "zuster")
    baby_ziek, baby_blij = guh("lila", "half", "ziek"), guh("lila", "blij", "wangen")

    def nacht(s, schuif=None, storm=0):
        s.vlak("#1e2a4a")
        s.spr("sterren", 0, 0)
        s.spr("maan", 92, 5)
        if schuif:
            s.spr("bergen", pad=[(0, schuif[0], 22), (s.duur, schuif[1], 22)])
        else:
            s.spr("bergen", -30, 22)
        s.vlak("#eef2fa", 0, 50, B, 14)
        s.vlak("#d0d8ea", 0, 50, B, 1)
        return s

    titelkaart(f, "BALTOGUH", "en de medicijnkist", achter="#1e2a4a", kleur="#bfe4ff",
               versier=lambda s: (s.spr("sterren", 0, 0), s.spr("bergen", -20, 34)))

    s = f.scene(150, "nomguh")
    nacht(s)
    s.spr("ziekenhuisje", 6, 23)
    s.spr(rosy, 44, GY)
    for i, x in enumerate((70, 84, 98)):
        s.spr(baby_ziek, pad=[(0, x, GY + 6), (40 + i * 22, x, GY + 6), (44 + i * 22, x, GY + 2, "uit"), (50 + i * 22, x, GY + 6, "in")], s=0.7)
    s.zeg(8, 74, "In Nomguh zijn de guhbaby's ziek. Hatsjoe-njeg!")
    s.zeg(80, 146, "Het medicijn ligt in de berghut. Achter de sneeuwstorm.")
    s.geluid(44, "njeg", 1.6).geluid(66, "njeg", 1.7).geluid(88, "njeg", 1.5)
    s.cue(46, "snik")

    s = f.scene(140, "steele")
    nacht(s)
    s.spr(steele, 70, GY, tot=70, golf=(0, 1, 6))
    s.spr(steele, 70, GY, van=70)
    s.spr(b_open, 16, GY, tot=70)
    s.spr(b_boos, 16, GY, van=70, tot=104)
    s.spr(b_boos, pad=[(104, 16, GY), (136, -30, GY, "in")], van=104, spiegel=True, golf=(0, 0.8, 8))
    s.spr("ballon", 56, 4, spiegel=True, tot=66)
    s.tekst("HA HA HA!", 76, 8, schaal=0.6, kleur="#5a5a6a", schaduw=False, tot=66)
    s.zeg(5, 66, "Steele-Mika lacht: 'Zo'n wolf-guhtje lukt dat nooit!'")
    s.zeg(72, 136, "Baltoguh zegt niks. Baltoguh pakt de slee.")
    s.geluid(6, "mika").geluid(30, "mika", 1.2).geluid(104, "guh", 0.8)
    s.cue(108, "juich")

    s = f.scene(170, "storm")
    nacht(s, schuif=(0, -110))
    s.spr("slee", 22, 42, golf=(0, 1, 10, 3))
    s.spr(b_boos, 44, GY - 2, golf=(1, 1.5, 10), tot=96)
    s.spr(b_open, pad=[(96, 44, GY - 2), (104, 44, GY - 14, "uit"), (114, 44, GY - 2, "in")], van=96, tot=114)
    s.spr(b_boos, 44, GY - 2, golf=(1, 1.5, 10), van=114)
    s.spr("sneeuw_a", pad=[(0, 0, -H), (170, -40, H * 2)])
    s.spr("sneeuw_a", pad=[(0, 30, -H * 2), (170, -10, H)])
    s.spr("sneeuw_b", pad=[(0, 10, 0), (170, -60, H * 3)])
    s.spr("sneeuw_b", pad=[(0, 20, -H * 3), (170, -50, 0)])
    s.tekst("HOBBEL!", 44, 8, schaal=0.75, kleur="#ffe27a", uitlijn="links", van=98, tot=124)
    s.zeg(5, 76, "Door de storm! Links! Rechts! Rechtdoor, njeg!")
    s.zeg(84, 166, "Het is koud. Het is ver. Maar thuis wachten knabbels.")
    s.geluid(2, "wind").geluid(60, "wind", 1.2).geluid(98, "plof").geluid(120, "wind")
    s.cue(100, "schrik")

    s = f.scene(120, "hut")
    nacht(s)
    s.spr("hut", 66, 25)
    s.spr("medkist", pad=[(0, 76, 42), (60, 76, 42), (72, 50, 30, "uit")])
    s.spr(b_open, pad=[(0, -24, GY), (46, 34, GY, "uit")], golf=(0, 0.8, 8), tot=46)
    s.spr(b_blij, pad=[(46, 34, GY), (56, 34, GY - 8, "uit"), (66, 34, GY, "in")], van=46)
    s.spr("ster", 60, 24, van=72, golf=(0, 1, 12))
    s.zeg(8, 114, "De berghut! En daar: de medicijnkist!")
    s.geluid(50, "vahoeg").geluid(72, "pling")
    s.cue(60, "juich")

    s = f.scene(160, "terug")
    s.vlak("#141c38")
    s.spr("sterren", 0, 0)
    for i, (kleur, y) in enumerate((("#3ad08a", 6), ("#58e0b0", 11), ("#7a8cf0", 16))):
        s.vlak(kleur, 0, y, B, 3, golf=(0, 2, 50, i * 14))
    s.spr("bergen", pad=[(0, -110, 22), (160, 0, 22)])
    s.vlak("#eef2fa", 0, 50, B, 14)
    s.spr("slee", 46, 42, golf=(0, 1, 10, 3), spiegel=True)
    s.spr("medkist", 54, 37, golf=(0, 1, 10, 3))
    s.spr(b_open, 26, GY - 2, golf=(1, 1.5, 10), spiegel=True, tot=70)
    s.spr(b_blij, 26, GY - 4, van=70, tot=120, spiegel=True, r=-14)
    s.spr(b_open, 26, GY - 2, golf=(1, 1.5, 10), spiegel=True, van=120)
    s.tekst("AWOEEE!", 14, 20, schaal=0.75, kleur="#bfe4ff", uitlijn="links", van=70, tot=120)
    s.zeg(5, 78, "Terug door de nacht. Baltoguh huilt naar het noorderlicht.")
    s.zeg(86, 156, "Het noorderlicht huilt niet terug. Maar wel mooi.")
    s.geluid(70, "vahoeg", 0.7).geluid(84, "vahoeg", 0.6)
    s.cue(74, "snik").cue(124, "lach")

    s = f.scene(170, "held")
    nacht(s)
    s.spr("ziekenhuisje", 6, 23)
    s.spr(rosy, 40, GY)
    for i, x in enumerate((62, 76, 90)):
        s.spr(baby_blij, x, GY + 6, s=0.7, golf=(0, 2.5, 12, i * 4))
    s.spr(b_blij, pad=[(0, 120, GY), (30, 96, GY, "uit")], tot=96, spiegel=True)
    s.spr(b_slaap, 96, GY, van=96)
    s.spr("zzz", 108, 20, van=102)
    s.vlak("#bfe4ff", pad=[(0, 0, -14, B, 12), (14, 0, 3, B, 12, "uit")], tot=96)
    s.tekst("HELD VAN NOMGUH", B // 2, 5, schaal=1.0, kleur="#1e2a4a", schaduw=False, van=14, tot=96)
    s.zeg(5, 80, "De guhbaby's zijn beter! Baltoguh is de Held van Nomguh!")
    s.zeg(98, 166, "En de held zelf? Die slaapt al. Het was een lange rit.")
    s.geluid(8, "fanfare").geluid(40, "vahoeg", 1.4).geluid(104, "snurk")
    s.cue(12, "juich").cue(104, "lach").cue(140, "slaap")

    aftiteling(f, ["Held: Baltoguh", "Zuster: Rosy", "Slechterik: Steele-Mika", "Storm: de storm"], slaper=b_slaap, duur=100)
    return f


# =====================================================================================================================
def mewtwo():
    f = Film("mewtwo", "Guhtwo: De Film", "De kloontank is kapot en de professor is zijn notities kwijt. Wie zwemt daar in de tank?",
             "Word Vriend van Guhtwo: speel het verhaal van het kloon-eiland uit.")
    paars = (150, 106, 188)
    g_open, g_boos, g_blij, g_slaap = (guh("guhtwo", o, "staart:paars", iris=(150, 80, 200), binnen=paars) for o in ("open", "boos", "blij", "dicht"))
    mieuw, mieuw_blij, mieuw_slaap = (guh("mieuw", o, "staart:mieuw", iris=(90, 170, 240)) for o in ("open", "blij", "dicht"))
    prof, prof_schrik, prof_blij = (guh("sneeuw", o, "labjas", "bril") for o in ("open", "schrik", "blij"))
    from features import guhpixel_bioscoop_film as film
    for naam, im in (("tank_kapot", tank(False)), ("tank", tank(True)), ("tank_vol", tank(True, film._GUHS[g_slaap])),
                     ("tank_wakker", tank(True, film._GUHS[g_boos])), ("computer", computer()), ("notitie", notitie()), ("tandwiel", tandwiel()),
                     ("schaal", schaal()), ("schaal_leeg", schaal(False))):
        f.sprite(naam, im)

    def lab(s):
        s.vlak("#28303e")
        s.vlak("#343e50", 0, 0, B, 10)
        for x in range(8, B, 26):
            s.vlak("#3e4a60", x, 12, 14, 8)
        s.vlak("#1c222e", 0, 50, B, 14)
        s.spr("computer", 6, 42)
        s.spr("computer", 92, 42)

    titelkaart(f, "GUHTWO", "Het kloon-eiland", achter="#2a1a4a", kleur="#e0c8ff",
               versier=lambda s: (s.spr("sterren", 0, 0), s.spr("tank", 41, 40)))

    s = f.scene(150, "lab")
    lab(s)
    s.spr("tank_kapot", 41, 6)
    for i, (x, y) in enumerate(((20, 30), (34, 20), (70, 24), (84, 32), (60, 14), (26, 12))):
        s.spr("notitie", pad=[(0, x, y), (20 + i * 6, x, y), (70 + i * 8, x + (40 if i % 2 else -50), -12, {"r": 200, "e": "in"})])
    s.spr(prof, 74, GY, tot=24)
    s.spr(prof_schrik, 74, GY, van=24, golf=(1.5, 0, 6))
    s.zeg(8, 72, "Professor Knabbelkloon is zijn labnotities kwijt.")
    s.zeg(80, 146, "En de kloontank is kapot. Slechte dag, njeg.")
    s.geluid(24, "oeh").geluid(80, "plof", 0.6)
    s.cue(28, "schrik")

    s = f.scene(150, "zoeken")
    lab(s)
    s.spr("tank_kapot", 41, 6, tot=110)
    s.spr("tank", 41, 6, van=110)
    for i in range(6):
        s.spr("notitie", pad=[(8 + i * 10, -10, 20 + i * 3), (22 + i * 10, 4 + i * 6, 30, "uit")], van=8 + i * 10)
    for i in range(4):
        s.spr("tandwiel", pad=[(70 + i * 9, 116, 22), (86 + i * 9, 56, 30, "in")], van=70 + i * 9, tot=86 + i * 9, r=0)
    s.spr(prof_blij, 74, GY, golf=(0, 1.5, 10))
    s.tekst("Notities: %s/6", 3, 2, schaal=0.6, uitlijn="links", teller=(0, 6, 8, 68))
    s.zeg(5, 72, "Zes notities en vier tankonderdelen later...")
    s.zeg(80, 146, "...doet de tank het weer. Blub!")
    for i in range(6):
        s.geluid(22 + i * 10, "pling", 1.0 + i * 0.1, 0.6)
    s.geluid(86, "klik").geluid(104, "klik").geluid(112, "fanfare", 1.2, 0.7)
    s.cue(114, "juich")

    s = f.scene(160, "tank")
    lab(s)
    s.spr("tank_vol", 41, 6, tot=70, golf=(0, 0.6, 30))
    s.vlak("#ffffff", 0, 0, B, H, van=64, tot=70)
    s.spr("tank_wakker", 41, 6, van=70)
    s.spr(prof, 76, GY, tot=70)
    s.spr(prof_schrik, pad=[(70, 76, GY), (80, 88, GY, "uit")], van=70)
    s.tekst("GUHTWO!", B // 2, 54, schaal=0.9, kleur="#e0c8ff", van=74, tot=90)
    s.zeg(5, 62, "Wie zwemt daar in de tank?")
    s.zeg(72, 156, "Het is... GUHTWO! De sterkste guh ooit gekloond. Hij kijkt boos.")
    s.geluid(6, "spanning").geluid(40, "spanning", 1.2).geluid(64, "boem")
    s.cue(66, "schrik")

    s = f.scene(160, "honger")
    lab(s)
    s.spr("tank", 41, 6)
    s.spr(g_boos, 16, GY - 8, golf=(0, 2, 30), tot=90)
    s.spr(g_open, 16, GY - 8, golf=(0, 2, 30), van=90)
    s.spr(prof_schrik, 86, GY, golf=(1, 0, 5), tot=90)
    s.spr(prof, 86, GY, van=90)
    s.spr("denkwolk", 30, 2, van=96)
    s.spr("knabbel", 41, 7, van=100, golf=(0, 1, 14))
    s.zeg(5, 72, "'Ik ben gemaakt om te vechten,' zegt Guhtwo.")
    s.zeg(82, 156, "Zijn buik zegt iets anders. Zijn buik zegt: knabbels.")
    s.geluid(6, "njeg", 0.6).geluid(90, "mika", 0.5, 0.7).geluid(100, "pling")
    s.cue(100, "lach")

    s = f.scene(160, "maaltijd")
    s.vlak("#3a2a5a")
    s.spr("sterren", 0, 0)
    s.vlak("#5a4a7a", 0, 50, B, 14)
    s.spr("schaal", 39, 38, tot=120)
    s.spr("schaal_leeg", 39, 38, van=120)
    s.spr(g_open, 12, GY, tot=40)
    s.spr(g_blij, 12, GY, van=40, golf=(0, 1, 8))
    s.spr(mieuw, pad=[(40, 120, 4), (74, 78, GY - 4, "uit")], van=40, tot=80, golf=(0, 2, 20), spiegel=True)
    s.spr(mieuw_blij, 78, GY - 4, van=80, golf=(0, 1, 8, 4), spiegel=True)
    s.zeg(5, 76, "Een dubbele portie! En kijk: daar is Mieuwguh ook.")
    s.zeg(84, 156, "Samen eten. Samen uitbuiken. Dat is pas kracht.")
    s.geluid(40, "guh", 1.5).geluid(60, "smak").geluid(84, "smak", 1.1).geluid(108, "smak", 0.9)
    s.cue(44, "snik").cue(124, "lach")

    s = f.scene(130, "vrienden")
    s.vlak("#3a2a5a")
    s.spr("sterren", 0, 0)
    s.vlak("#5a4a7a", 0, 50, B, 14)
    s.spr("schaal_leeg", 39, 38)
    s.spr(g_slaap, 14, GY, golf=(0, 0.5, 30))
    s.spr(mieuw_slaap, 76, GY, golf=(0, 0.5, 30, 12), spiegel=True)
    s.spr("zzz", 32, 22)
    s.spr("zzz", 70, 22)
    s.spr("hart", 52, 16, s=2, golf=(0, 1.5, 24))
    s.zeg(8, 124, "Guhtwo en Mieuwguh: beste vriendjes. Het eiland is gered!")
    s.geluid(6, "fanfare").geluid(60, "snurk")
    s.cue(10, "juich").cue(70, "slaap")

    aftiteling(f, ["Kloon: Guhtwo", "Vriendje: Mieuwguh", "Notities: weer terecht", "Knabbels: allemaal op"], slaper=g_slaap, duur=100)
    return f


# =====================================================================================================================
def stitch626():
    f = Film("stitch626", "626: Ohana op Guhwai'i", "Er viel een ster op Guhwai'i. Er zat een heel stout blauw guhtje in.",
             "Word Ohana-guh: speel het verhaal van 626-guh op Guhwai'i uit.")
    for naam, im in (("palm", palm()), ("capsule", capsule()), ("vallende_ster", vallende_ster()), ("paalhuisje", paalhuisje()),
                     ("kussen", rommel(0)), ("sok", rommel(1)), ("kokosnoot", kokosnoot()), ("hibiscus", hibiscus()), ("zon", zon())):
        f.sprite(naam, im)
    licht = (150, 190, 240)
    s_boos, s_open, s_blij, s_slaap = (guh("626", o, oren="flap", binnen=licht) for o in ("boos", "open", "blij", "dicht"))
    lilo, lilo_blij, lilo_slaap = (guh("lilo", o, "haar", "jurk") for o in ("open", "blij", "dicht"))
    nani, nani_schrik, nani_slaap = (guh("lilo", o, "haar") for o in ("open", "schrik", "dicht"))

    def strand(s, nacht=False, avond=False):
        s.vlak("#1a2046" if nacht else "#ffb07a" if avond else "#8fd8ff")
        if nacht:
            s.spr("sterren", 0, 0)
        else:
            s.spr("zon", 84, 24 if avond else 4)
            s.spr("wolk", 10, 8)
        s.vlak("#2a4a8a" if nacht else "#e07a70" if avond else "#3aa0e0", 0, 38, B, 12)
        s.vlak("#3a5a9a" if nacht else "#f09a80" if avond else "#6ac0f0", 0, 38, B, 1, golf=(0, 0.6, 30))
        s.vlak("#b0a070" if nacht else "#f0d890", 0, 50, B, 14)
        s.spr("palm", 0, 15)

    titelkaart(f, "626", "Ohana op Guhwai'i", achter="#1a4a8a", kleur="#8fd8ff", schaal=3.0,
               versier=lambda s: (s.spr("sterren", 0, 0), s.spr("vallende_ster", pad=[(0, 120, 0), (60, -20, 50)])))

    s = f.scene(140, "ster")
    strand(s, nacht=True)
    s.spr("vallende_ster", pad=[(10, 120, -8), (60, 62, 40, "in")], van=10, tot=60)
    s.vlak("#ffffff", 0, 0, B, H, van=60, tot=64)
    s.spr("capsule", 54, 38, van=60)
    s.spr("ster", pad=[(64, 62, 36), (90, 52, 20, "uit")], van=64, tot=90)
    s.spr("ster", pad=[(64, 66, 36), (90, 82, 22, "uit")], van=64, tot=90)
    s.zeg(5, 62, "Er viel een ster op de heuvel van Guhwai'i.")
    s.zeg(70, 136, "In de ster zat iets blauws. Iets heel stouts.")
    s.geluid(12, "wind", 1.4).geluid(60, "boem")
    s.cue(62, "schrik")

    s = f.scene(140, "capsule")
    strand(s, nacht=True)
    s.spr("capsule", 54, 38)
    s.spr(s_boos, pad=[(20, 56, 30), (30, 56, 6, "uit"), (44, 30, GY, "in")], van=20)
    s.tekst("RAAAH!", 56, 4, schaal=1.0, kleur="#ff5a5a", uitlijn="links", van=30, tot=70)
    s.zeg(8, 78, "Experiment 626-guh! Gemaakt om rommel te maken.")
    s.zeg(84, 136, "En dat kan hij goed, njeg.")
    s.geluid(20, "klik").geluid(30, "mika", 1.5)
    s.cue(32, "schrik").cue(88, "lach")

    s = f.scene(160, "rommel")
    strand(s)
    s.spr("paalhuisje", 70, 17)
    s.spr(nani, 46, GY, tot=40)
    s.spr(nani_schrik, 46, GY, van=40, golf=(1, 0, 6))
    for i, (naam, t, x, y) in enumerate((("kussen", 20, 14, 44), ("sok", 36, 30, 22), ("kokosnoot", 52, 62, 10), ("kussen", 68, 88, 42), ("sok", 84, 20, 8),
                                        ("kokosnoot", 100, 40, 52))):
        s.spr(naam, pad=[(t, 80, 30), (t + 14, x, y, {"r": 360, "e": "uit"})], van=t)
    s.spr(s_boos, pad=[(0, 90, GY), (20, 4, GY), (40, 88, GY - 12), (60, 10, GY), (80, 86, GY), (100, 20, GY - 10), (120, 70, GY)], tot=120,
          golf=(0, 2, 6))
    s.spr(s_boos, 70, GY, van=120)
    s.spr(lilo_blij, pad=[(96, 120, GY), (120, 92, GY, "uit")], van=96, spiegel=True)
    s.spr("hart", 100, 22, van=124, golf=(0, 1, 16))
    s.zeg(5, 82, "Kussens! Kokosnoten! De was van Nani-guh! Alles vliegt.")
    s.zeg(90, 156, "Lilo-guh vindt hem toch lief. Lilo-guh heeft gelijk.")
    for t in (20, 52, 84):
        s.geluid(t, "plof", 1.2)
    s.geluid(40, "oeh").geluid(124, "guh", 1.3)
    s.cue(42, "lach").cue(126, "snik")

    s = f.scene(170, "lief")
    strand(s)
    s.spr("paalhuisje", 70, 17)
    s.spr(lilo, 20, GY)
    s.spr(s_boos, 62, GY, tot=50, spiegel=True)
    s.spr(s_open, 62, GY, van=50, tot=110, spiegel=True)
    s.spr(s_blij, pad=[(110, 62, GY), (118, 62, GY - 8, "uit"), (126, 62, GY, "in")], van=110, spiegel=True)
    for naam, t in (("kokosnoot", 14), ("hibiscus", 44), ("knabbel", 74)):
        s.spr(naam, pad=[(t, 40, 38), (t + 22, 58, 40, "zacht")], van=t, tot=t + 30)
    s.spr("hart", 68, 20, van=114, golf=(0, 1, 16))
    s.zeg(5, 78, "Een kokosnoot. Een hibiscus. Acht kaasknabbels.")
    s.zeg(86, 166, "Daar kan zelfs een stout experiment niet tegen.")
    s.geluid(36, "smak").geluid(66, "pling").geluid(96, "smak", 1.1).geluid(112, "vahoeg")
    s.cue(114, "snik")

    s = f.scene(150, "ohana")
    strand(s)
    s.spr("paalhuisje", 70, 17)
    s.spr(nani, 14, GY)
    s.spr(lilo_blij, 40, GY, golf=(0, 1, 10))
    s.spr(s_blij, 66, GY, golf=(0, 1, 10, 5))
    s.spr("hart", 51, 8, s=2, golf=(0, 1.5, 24), van=20)
    s.tekst("OHANA", B // 2, 2, schaal=1.5, kleur="#ffffff", van=10, tot=76)
    s.zeg(8, 72, "Ohana betekent familie.")
    s.zeg(78, 146, "En familie laat je nooit in de steek, njeg!")
    s.geluid(10, "fanfare").geluid(80, "vahoeg")
    s.cue(14, "juich").cue(84, "snik")

    s = f.scene(110, "dutje")
    strand(s, avond=True)
    s.spr(nani_slaap, 20, GY + 2, golf=(0, 0.5, 30))
    s.spr(lilo_slaap, 44, GY + 2, golf=(0, 0.5, 30, 8))
    s.spr(s_slaap, 68, GY + 2, golf=(0, 0.5, 30, 16))
    s.spr("zzz", 36, 22)
    s.spr("zzz", 86, 22)
    s.zeg(8, 104, "En samen een dutje doen hoort daar ook bij.")
    s.geluid(12, "snurk")
    s.cue(40, "slaap")

    aftiteling(f, ["Experiment: 626-guh", "Ohana: Lilo-guh", "De was: Nani-guh", "Rommel: opgeruimd"], slaper=s_slaap, duur=90)
    return f


FILMS = (balto, mewtwo, stitch626)
