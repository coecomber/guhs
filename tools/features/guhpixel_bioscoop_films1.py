"""
Guhbioscoop films, part 1: Skyblok, Bedwars, Vadsnite (the films of the three joke games of grap1). Each function builds one
Film (see guhpixel_bioscoop_film.py for the format and the tools). Dutch here; English in tools/lang/en/c37_px_bioscoop.json.
"""
from PIL import Image, ImageDraw

from features import guhpixel_bioscoop_tex as tex
from features.guhpixel_bioscoop_film import B, H, Film, aftiteling, guh, titelkaart

LUCHT, NAAD = "#8fd0ff", "#79bfee"
GRAS, GRAS_D, AARDE, AARDE_D = (104, 184, 84), (74, 150, 66), (134, 96, 67), (104, 72, 50)


# =====================================================================================================================
# pictures
# =====================================================================================================================
def eiland(breed=64, hoog=22, rand=None):
    """A floating island: grass on top (or a rim of wool in a team colour), earth that narrows towards the bottom."""
    im = tex.nieuw(breed, hoog)
    d = ImageDraw.Draw(im)
    for y in range(2, hoog):
        f = (y - 2) / max(1, hoog - 3)
        inspring = int(f * f * breed * 0.36)
        d.line([(inspring, y), (breed - 1 - int(inspring * 1.3), y)], fill=AARDE + (255,) if y % 3 else AARDE_D + (255,))
    d.rectangle((0, 0, breed - 1, 1), fill=tex.rgb(rand) if rand else GRAS + (255,))
    d.line([(0, 2), (breed - 1, 2)], fill=tex.tint(rand, 0.75) if rand else GRAS_D + (255,))
    for x in range(5, breed - 6, 9):
        im.putpixel((x, 5 + x % 3), (150, 150, 156, 255))
    return im


def boom():
    im = tex.nieuw(18, 24)
    d = ImageDraw.Draw(im)
    d.rectangle((7, 12, 10, 23), fill=(120, 84, 52, 255))
    d.line([(7, 12), (7, 23)], fill=(96, 64, 40, 255))
    d.rounded_rectangle((0, 0, 17, 13), radius=4, fill=(70, 150, 70, 255))
    d.rounded_rectangle((2, 1, 13, 7), radius=3, fill=(96, 180, 84, 255))
    for (x, y) in ((4, 10), (12, 9), (14, 4)):
        im.putpixel((x, y), (50, 120, 56, 255))
    return im


def kist():
    return tex.raster(["bbbbbbbbbb", "bBBBBBBBBb", "bBBBBBBBBb", "bbbbggbbbb", "bBBBggBBBb", "bBBBBBBBBb", "bBBBBBBBBb", "bbbbbbbbbb"],
                      {"b": (96, 62, 30), "B": (160, 112, 52), "g": (220, 220, 230)})


def ijs():
    return tex.raster(["iiiiiii", "iIwIIIi", "iwIIIIi", "iIIIIIi", "iIIIIwi", "iIIIIIi", "iiiiiii"],
                      {"i": (120, 170, 235), "I": (170, 208, 250), "w": (240, 250, 255)})


def emmer():
    return tex.raster([".ggggg.", "gOoOoOg", "gggggggg"[:7], ".gGGGg.", ".gGGGg.", ".gGGGg.", "..ggg.."],
                      {"g": (120, 124, 136), "G": (170, 174, 186), "O": (255, 150, 40), "o": (255, 214, 80)})


def ladder(hoog=46):
    im = tex.nieuw(7, hoog)
    d = ImageDraw.Draw(im)
    d.line([(0, 0), (0, hoog - 1)], fill=(140, 100, 60, 255))
    d.line([(6, 0), (6, hoog - 1)], fill=(140, 100, 60, 255))
    for y in range(2, hoog, 4):
        d.line([(1, y), (5, y)], fill=(176, 130, 80, 255))
    return im


def deur():
    """The staff door in the sky."""
    im = tex.nieuw(12, 20)
    d = ImageDraw.Draw(im)
    d.rectangle((1, 4, 10, 19), fill=(120, 126, 140, 255))
    d.rectangle((2, 5, 9, 19), fill=(160, 166, 180, 255))
    d.rectangle((8, 12, 8, 13), fill=(250, 220, 110, 255))
    d.rectangle((0, 0, 11, 2), fill=(250, 250, 250, 255))
    d.line([(2, 1), (9, 1)], fill=(220, 60, 60, 255))
    return im


def generator():
    return tex.raster(["bbb.sss.ooo", "bbb.sSs.oOo", "bbb.sss.ooo"],
                      {"b": (90, 150, 240), "s": (130, 130, 136), "S": (170, 170, 176), "o": (250, 140, 40), "O": (255, 214, 80)})


def bedje_icoon():
    return tex.raster(["w..........", "wwwrrrrrrrr", "bbbbbbbbbbb", "b.........b"],
                      {"w": (250, 250, 250), "r": (200, 50, 60), "b": (140, 96, 60)})


def plakband():
    return tex.raster(["ttt", "ttt"], {"t": (236, 226, 190)})


def vadsbus():
    """The flying Vadsbus: a blue bus under a big pink balloon."""
    im = tex.nieuw(46, 32)
    d = ImageDraw.Draw(im)
    d.ellipse((8, 0, 37, 15), fill=(245, 140, 200, 255))
    d.ellipse((12, 2, 24, 8), fill=(255, 186, 224, 255))
    d.line([(12, 14), (8, 18)], fill=(90, 70, 80, 255))
    d.line([(33, 14), (37, 18)], fill=(90, 70, 80, 255))
    d.rounded_rectangle((2, 18, 43, 29), radius=2, fill=(70, 110, 220, 255))
    d.line([(4, 18), (41, 18)], fill=(120, 160, 250, 255))
    d.rectangle((2, 26, 43, 27), fill=(250, 210, 80, 255))
    for x in (6, 15, 24, 33):
        d.rectangle((x, 20, x + 6, 24), fill=(200, 232, 250, 255))
        im.putpixel((x + 2, 22), (28, 20, 36, 255))
        im.putpixel((x + 4, 22), (28, 20, 36, 255))
    for x in (9, 33):
        d.ellipse((x, 27, x + 4, 31), fill=(40, 40, 50, 255))
    return im


def parachute(kleur=(240, 90, 110)):
    im = tex.nieuw(26, 20)
    d = ImageDraw.Draw(im)
    d.pieslice((0, 0, 25, 22), 180, 360, fill=tex.rgb(kleur))
    for x in (6, 13, 19):
        d.line([(x, 2), (x, 10)], fill=(255, 255, 255, 255))
    d.line([(1, 11), (10, 19)], fill=(90, 70, 80, 255))
    d.line([(24, 11), (15, 19)], fill=(90, 70, 80, 255))
    return im


def wolkrand(spiegel=False):
    """The puffy edge of the pink slaapwolk (a column)."""
    im = tex.nieuw(10, H)
    d = ImageDraw.Draw(im)
    for y in range(-4, H, 9):
        d.ellipse((0, y, 13, y + 12), fill=(250, 176, 214, 255))
        d.ellipse((3, y + 2, 9, y + 7), fill=(255, 208, 232, 255))
    d.rectangle((6, 0, 9, H - 1), fill=(250, 176, 214, 255))
    return im.transpose(Image.FLIP_LEFT_RIGHT) if spiegel else im


def beker():
    return tex.raster(["ggggggggg", "gGGGGGGGg", ".gGGwGGg.", ".gGGGGGg.", "..gGGGg..", "...ggg...", "....g....", "...ggg...", "..ggggg.."],
                      {"g": (230, 170, 40), "G": (255, 220, 100), "w": (255, 255, 255)})


def _lucht(s, wolken=True):
    """The painted sky box: light blue, seams, tape on the corners of the panels, flat clouds."""
    s.vlak(LUCHT)
    s.vlak(NAAD, 37, 0, 1, H)
    s.vlak(NAAD, 75, 0, 1, H)
    s.vlak(NAAD, 0, 31, B, 1)
    for (x, y) in ((36, 30), (74, 30)):
        s.spr("plakband", x, y)
    if wolken:
        s.spr("wolk", 6, 6)
        s.spr("wolk_groot", 46, 12)
        s.spr("wolk", 82, 40)


# =====================================================================================================================
def skyblok():
    f = Film("skyblok", "Skyblok: De Film", "Eén eilandje, 4.812 stappen en een bed. Raad eens wat er wint.",
             "Speel Skyblok uit in de Guhpixel-lobby.")
    for naam, im in (("eiland", eiland()), ("eiland_klein", eiland(30, 12)), ("boom", boom()), ("kist", kist()), ("ijs", ijs()), ("emmer", emmer()),
                     ("ladder", ladder()), ("deur", deur()), ("generator", generator()), ("bedje", bedje_icoon()), ("plakband", plakband())):
        f.sprite(naam, im)
    pro = guh("lila", "open", "headset")
    pro_blij = guh("lila", "blij", "headset")
    pro_schrik = guh("lila", "schrik", "headset")
    pro_slaap = guh("lila", "dicht", "headset")

    def toneel(s, bed=True, wolken=True):
        _lucht(s, wolken)
        s.spr("ladder", 102, 14)
        s.spr("deur", 86, 8)
        s.spr("eiland", 20, 40)
        s.spr("boom", 60, 17)
        s.spr("kist", 24, 32)
        if bed:
            s.spr("bed", 36, 31)

    titelkaart(f, "SKYBLOK", "De Film", achter="#1a2a5a",
               versier=lambda s: (s.spr("sterren", 0, 0), s.spr("eiland_klein", 41, 46, golf=(0, 1.5, 40))))

    s = f.scene(170, "eiland")
    toneel(s)
    s.spr(pro, pad=[(0, -24, 22), (45, 2, 22), (60, 22, 22, "zacht")], golf=(0, 0.8, 8), tot=60)
    s.spr(pro, 22, 22, van=60)
    s.tekst("Stap 1 van 4.812", 3, 3, schaal=0.75, uitlijn="links", van=84)
    s.zeg(8, 78, "Welkom op Skyblok. Hier begint een episch avontuur, njeg.")
    s.zeg(86, 166, "Stap 1 van 4.812: maak een cobblestone generator.")
    s.geluid(20, "guh").geluid(86, "pling")

    s = f.scene(150, "kist")
    toneel(s)
    s.tekst("Stap 1 van 4.812", 3, 3, schaal=0.75, uitlijn="links")
    s.spr("ijs", pad=[(0, 26, 32), (16, 20, 20, "uit")])
    s.spr("emmer", pad=[(6, 26, 32), (22, 31, 20, "uit"), (78, 31, 20), (92, 44, 20, "in"), (104, 88, 34), (124, 96, 70, {"r": 300, "e": "in"})])
    s.spr(pro, 34, 22, tot=70)
    s.spr(pro, pad=[(70, 34, 22), (80, 30, 22, "uit")], spiegel=True, van=70, tot=106)
    s.spr(pro_schrik, 30, 22, van=106)
    s.zeg(5, 72, "In de kist: ijs. En een emmer lava die je niet mag gebruiken.")
    s.zeg(80, 146, "Oeps. Nou ja. Die mocht toch al niet, njeg.")
    s.geluid(4, "klik").geluid(80, "plof").geluid(126, "plof", 0.6)
    s.cue(108, "schrik").cue(128, "lach")

    s = f.scene(150, "bonk")
    toneel(s, wolken=False)
    s.spr("wolk", 6, 6)
    s.spr("wolk", 82, 40)
    s.spr("wolk_groot", pad=[(0, 20, 6), (52, 20, 6), (60, 22, 3, "uit"), (64, 20, 6), (104, 16, 70, {"r": 40, "e": "in"})])
    s.spr(pro, 30, 22, tot=40)
    s.spr(pro_blij, pad=[(40, 30, 22), (52, 30, 8, "uit")], van=40, tot=52)
    s.spr(pro_schrik, pad=[(52, 30, 8), (64, 30, 22, "in")], van=52, tot=110)
    s.spr(pro, 30, 22, van=110)
    s.tekst("BONK!", 44, 6, schaal=1.0, kleur="#ff5a5a", uitlijn="links", van=52, tot=74)
    s.zeg(2, 60, "Wat een prachtige lucht. Zo blauw. Zo... dichtbij?")
    s.zeg(68, 146, "De lucht is een geverfde doos. Niet tegenaan springen, njeg.")
    s.geluid(40, "vahoeg").geluid(52, "plof", 0.7).geluid(106, "plof")
    s.cue(54, "schrik").cue(108, "lach")

    s = f.scene(150, "denken")
    toneel(s)
    s.spr(pro, 22, 22, tot=92)
    s.spr("denkwolk", 34, 0, van=10, tot=92)
    s.spr("generator", 44, 6, van=14, tot=92)
    s.spr("kruis", 45, 4, van=46, tot=92)
    s.vlak("#ffffff", 42, 3, 15, 12, van=66, tot=92)
    s.spr("bedje", 44, 7, van=66, tot=92, golf=(0, 1, 16))
    s.spr(pro_blij, pad=[(92, 22, 22), (120, 40, 20, "zacht")], van=92, tot=120, golf=(0, 0.8, 8))
    s.spr(pro_slaap, 40, 20, van=120)
    s.spr("bed", 36, 31, van=120)
    s.spr("zzz", 60, 12, van=126)
    s.zeg(4, 62, "Nog 4.811 stappen te gaan...")
    s.zeg(68, 146, "...of een dutje. Even kijken wat het bed doet.")
    s.geluid(46, "njeg").geluid(66, "pling").geluid(122, "gaap")
    s.cue(70, "lach")

    s = f.scene(170, "uitgespeeld")
    toneel(s, bed=False)
    s.spr(pro_slaap, 40, 20)
    s.spr("bed", 36, 31)
    s.spr("zzz", 60, 12)
    s.spr("vuurwerk_roze", 8, 4, van=24, tot=39)
    s.spr("vuurwerk_geel", 84, 2, van=44, tot=59)
    s.spr("vuurwerk_blauw", 30, 34, van=64, tot=79)
    s.spr("vuurwerk_geel", 6, 30, van=84, tot=99)
    s.spr("vuurwerk_roze", 80, 28, van=104, tot=119)
    s.tekst("SKYBLOK", B // 2, 3, schaal=1.5, kleur="#ffe27a", van=16)
    s.tekst("UITGESPEELD!", B // 2, 17, schaal=1.25, kleur="#ffffff", van=22)
    s.zeg(22, 92, "SKYBLOK UITGESPEELD! Het enige wat hier werkt is het bed.")
    s.zeg(98, 166, "Het doel van het leven is vadsen. Geen cobblestone generator.")
    s.geluid(16, "fanfare").geluid(24, "boem").geluid(44, "boem", 1.2).geluid(64, "boem", 0.9).geluid(84, "boem", 1.1).geluid(104, "boem")
    s.cue(20, "juich").cue(104, "lach")

    aftiteling(f, ["Regie: een guh", "Cobblestone: niemand", "Stappen gezet: 1", "Dutjes: ja, njeg"], slaper=pro_slaap)
    return f


# =====================================================================================================================
def bedwars():
    f = Film("bedwars", "Bedwars: De Film", "Vier teams, één bed. Verdedig het... door erin te gaan liggen.",
             "Speel Bedwars uit in de Guhpixel-lobby.")
    f.sprite("eiland_rood", eiland(28, 12, (214, 60, 60)))
    f.sprite("eiland_blauw", eiland(28, 12, (70, 110, 220)))
    f.sprite("eiland_groen", eiland(28, 12, (80, 180, 90)))
    f.sprite("eiland_geel", eiland(80, 16, (240, 210, 70)))
    f.sprite("bed_breed", tex.bed((240, 210, 70), 72))
    held, held_slaap = guh("lila", "open", "kussen"), guh("lila", "dicht", "kussen")
    rood, blauw, groen = guh("mint", "open", "muts:rood"), guh("choco", "open", "muts:blauw"), guh("sneeuw", "open", "muts:groen")
    rood_s, blauw_s, groen_s = guh("mint", "dicht", "muts:rood"), guh("choco", "dicht", "muts:blauw"), guh("sneeuw", "dicht", "muts:groen")
    GY = 28      # where a guh stands on the big island (its top is at y 46)

    def toneel(s, vijand=True):
        s.vlak("#9ad0f5")
        s.spr("wolk", 60, 30)
        s.spr("wolk_groot", 2, 36)
        s.spr("eiland_rood", 2, 18)
        s.spr("eiland_groen", 42, 12)
        s.spr("eiland_blauw", 82, 18)
        s.spr("eiland_geel", 16, 46)
        if vijand:
            s.spr(rood, 5, 0)
            s.spr(groen, 45, -6)
            s.spr(blauw, 85, 0)

    def in_bed(s, wie, x, van=None, tot=None):
        s.spr(wie, x, GY - 4, van=van, tot=tot)

    titelkaart(f, "BEDWARS", "Verdedig je bed!", achter="#5a1e3a",
               versier=lambda s: s.spr("bed", 41, 48))

    s = f.scene(150, "eilanden")
    toneel(s)
    s.spr("bed_breed", 20, 35)
    s.spr(held, pad=[(0, 20, GY), (30, 20, GY - 6, "uit"), (40, 20, GY, "in")])
    s.zeg(8, 78, "Vier teams, vier eilandjes. En één heel belangrijk bed.")
    s.zeg(84, 146, "Verdedig je bed met alles wat je hebt, njeg!")
    s.geluid(10, "guh").geluid(30, "vahoeg")

    s = f.scene(130, "verdedigen")
    toneel(s)
    s.spr(held, pad=[(0, 20, GY), (40, 46, GY, "zacht"), (52, 46, GY - 9, "uit"), (62, 46, GY - 4, "in")], tot=62, golf=(0, 0.8, 8))
    in_bed(s, held_slaap, 46, van=62)
    s.spr("bed_breed", 20, 35)
    s.spr("zzz", 66, 14, van=74)
    s.zeg(5, 62, "De beste verdediging: er zelf in gaan liggen.")
    s.zeg(70, 126, "Zo. Daar komt niemand meer langs.")
    s.geluid(62, "plof").geluid(84, "snurk")
    s.cue(66, "lach")

    s = f.scene(170, "brug")
    toneel(s, vijand=False)
    # the three bridges: columns of wool that grow down towards your island
    s.vlak("#d63c3c", pad=[(0, 14, 30, 4, 0), (150, 14, 30, 4, 16)])
    s.vlak("#50b45a", pad=[(0, 54, 24, 4, 0), (150, 54, 24, 4, 22)])
    s.vlak("#466edc", pad=[(0, 94, 30, 4, 0), (150, 94, 30, 4, 16)])
    s.spr(rood, pad=[(0, 5, 0), (20, 5, 12), (150, 5, 28)])
    s.spr(groen, pad=[(0, 45, -6), (20, 45, 6), (150, 45, 28)])
    s.spr(blauw, pad=[(0, 85, 0), (20, 85, 12), (150, 85, 28)])
    in_bed(s, held_slaap, 46)
    s.spr("bed_breed", 20, 35)
    s.spr("zzz", 66, 14)
    s.zeg(5, 76, "De vijand bouwt een brug. Heel. Langzaam.")
    s.zeg(86, 166, "Blokje voor blokje komen ze dichterbij...")
    s.geluid(6, "spanning")
    for t in (30, 60, 90, 120, 150):
        s.geluid(t, "plof", 1.2)
    s.geluid(100, "spanning", 1.2)
    s.cue(152, "schrik")

    s = f.scene(160, "aankomst")
    toneel(s, vijand=False)
    s.vlak("#d63c3c", 14, 30, 4, 16)
    s.vlak("#50b45a", 54, 24, 4, 22)
    s.vlak("#466edc", 94, 30, 4, 16)
    s.tekst("ZE ZIJN ER!", B // 2, 2, schaal=1.25, kleur="#ff5a5a", tot=56)
    for wie, slaap, x0, x1, t in ((rood, rood_s, 5, 22, 64), (groen, groen_s, 45, 64, 84), (blauw, blauw_s, 85, 76, 104)):
        s.spr(wie, pad=[(0, x0, GY), (t - 12, x1, GY, "zacht"), (t - 5, x1, GY - 9, "uit"), (t, x1, GY - 4, "in")], tot=t, golf=(0, 0.8, 8),
              spiegel=x1 < x0)
        in_bed(s, slaap, x1, van=t)
    in_bed(s, held_slaap, 44)
    s.spr("bed_breed", 20, 35)
    s.spr("zzz", 34, 12, van=70)
    s.spr("zzz", 60, 10, van=40)
    s.spr("zzz", 88, 12, van=110)
    s.zeg(5, 60, "Ze zijn er! Ze staan naast je bed! Ze...")
    s.zeg(68, 156, "...gaan er gezellig bij liggen. Welterusten.")
    s.geluid(4, "oeh").geluid(64, "plof").geluid(84, "plof").geluid(104, "plof").geluid(90, "gaap").geluid(124, "snurk")
    s.cue(8, "schrik").cue(86, "lach").cue(126, "snik")

    s = f.scene(150, "eindscherm")
    toneel(s, vijand=False)
    for slaap, x in ((rood_s, 22), (held_slaap, 44), (groen_s, 64), (blauw_s, 76)):
        in_bed(s, slaap, x)
    s.spr("bed_breed", 20, 35)
    s.vlak("#2a1238", 0, 0, B, 24)
    s.vlak("#ffd27a", 0, 23, B, 1)
    s.tekst("BED VERDEDIGD", B // 2, 2, schaal=1.25, kleur="#ffe27a")
    s.tekst("Bedden vernield: 0", 6, 14, schaal=0.6, uitlijn="links", van=20)
    s.tekst("Dutjes: 5", B - 6, 14, schaal=0.6, uitlijn="rechts", van=40)
    s.zeg(8, 80, "BED VERDEDIGD! Bedden vernield: 0. Dutjes: 5.")
    s.zeg(88, 146, "Vijf? Ja. De cameraguh sliep ook, njeg.")
    s.geluid(4, "fanfare").geluid(92, "njeg")
    s.cue(10, "juich").cue(96, "lach")

    aftiteling(f, ["Team Geel: sliep", "Team Rood: sliep", "Team Groen: sliep", "Team Blauw: sliep ook"], slaper=held_slaap)
    return f


# =====================================================================================================================
def vadsnite():
    f = Film("vadsnite", "Vadsnite: De Film", "100 guhs springen uit de Vadsbus. Wie blijft als laatste wakker?",
             "Speel Vadsnite uit in de Guhpixel-lobby.")
    f.sprite("vadsbus", vadsbus())
    f.sprite("parachute", parachute())
    f.sprite("parachute_geel", parachute((250, 200, 70)))
    f.sprite("parachute_blauw", parachute((90, 150, 240)))
    f.sprite("wolkrand", wolkrand())
    f.sprite("wolkrand_r", wolkrand(True))
    f.sprite("beker", beker())
    held, held_blij, held_schrik, held_half, held_slaap = (guh("lila", o, "rugzak") for o in ("open", "blij", "schrik", "half", "dicht"))
    slapers = [guh(k, "dicht") for k in ("mint", "choco", "sneeuw", "goud", "lila")]
    wakkeren = [guh(k, "open") for k in ("mint", "choco", "sneeuw", "goud", "lila")]
    GY = 32      # a guh on the grass (the grass begins at y 50)

    def wei(s):
        s.vlak("#9ad0f5")
        s.spr("wolk", 10, 6)
        s.spr("wolk_groot", 70, 10)
        s.vlak("#6cc070", 0, 50, B, 14)
        s.vlak("#58aa5e", 0, 50, B, 1)

    titelkaart(f, "VADSNITE", "Blijf wakker (als je kunt)", achter="#3a2a6a",
               versier=lambda s: (s.spr("sterren", 0, 0), s.spr("vadsbus", pad=[(0, -50, 34), (70, 116, 30)], golf=(0, 1.5, 30))))

    s = f.scene(150, "bus")
    s.vlak("#9ad0f5")
    s.spr("wolk", pad=[(0, 80, 8), (150, 60, 8)])
    s.spr("wolk_groot", pad=[(0, 20, 34), (150, -4, 34)])
    s.spr("wolk", pad=[(0, 96, 46), (150, 70, 46)])
    for i, (t, x) in enumerate(((64, 30), (78, 44), (92, 58), (106, 72), (120, 86))):
        s.spr(wakkeren[i], pad=[(t, x, 22), (t + 30, x - 6, 70, "in")], van=t, s=0.6)
    s.spr("vadsbus", pad=[(0, -46, 4), (150, 100, 6)], golf=(0, 1.5, 30))
    s.tekst("Nog wakker: %s", B - 3, 3, schaal=0.75, uitlijn="rechts", teller=(100, 100, 0, 1))
    s.zeg(8, 80, "100 guhs springen uit de Vadsbus. Wie blijft er wakker?")
    s.zeg(88, 146, "Kies je landingsplek goed, njeg!")
    s.geluid(4, "wind").geluid(64, "vahoeg").geluid(92, "vahoeg", 1.2)

    s = f.scene(130, "val")
    s.vlak("#9ad0f5")
    s.spr("wolk", pad=[(0, 8, 50), (130, 8, -12)])
    s.spr("wolk_groot", pad=[(0, 70, 70), (130, 70, 0)])
    s.vlak("#6cc070", pad=[(0, 0, 70, B, 14), (80, 0, 70, B, 14), (118, 0, 50, B, 14, "uit")])
    for wie, para, x, fase in ((wakkeren[0], "parachute_geel", 12, 0), (held, "parachute", 45, 9), (wakkeren[1], "parachute_blauw", 80, 17)):
        s.spr(para, pad=[(0, x - 2, -40), (118, x - 2, 14, "uit")], golf=(3, 1, 34, fase), tot=120)
        s.spr(wie, pad=[(0, x, -22), (118, x, GY, "uit")], golf=(3, 1, 34, fase))
    s.tekst("Nog wakker: %s", B - 3, 3, schaal=0.75, uitlijn="rechts", teller=(100, 100, 0, 1))
    s.zeg(5, 76, "Waar landen we? Maakt niet uit: overal ligt lekker zacht gras.")
    s.geluid(4, "wind").geluid(118, "plof")

    s = f.scene(160, "landing")
    wei(s)
    plekken = ((2, 0), (24, 14), (68, 6), (90, 20), (46, 30))
    for i, (x, t) in enumerate(plekken):
        if i == 4:
            continue
        s.spr(wakkeren[i], pad=[(t, x, -20), (t + 14, x, GY, "in")], van=t, tot=t + 22)
        s.spr(slapers[i], x, GY, van=t + 22)
        s.spr("zzz", x + 16, GY - 8, van=t + 30)
    s.spr(held, 46, GY, tot=110)
    s.spr(held, 46, GY, van=110, spiegel=True, tot=130)
    s.spr(held_schrik, 46, GY, van=130)
    s.tekst("Nog wakker: %s", B - 3, 3, schaal=0.75, uitlijn="rechts", teller=(100, 1, 26, 124))
    s.zeg(8, 76, "Geland! En... iedereen valt meteen in slaap.")
    s.zeg(84, 156, "Nog wakker: 37. Nee, 12. Nee... eentje?!")
    s.geluid(14, "plof").geluid(28, "plof").geluid(42, "plof").geluid(50, "snurk").geluid(96, "snurk", 1.2).geluid(130, "oeh")
    s.cue(44, "lach").cue(132, "schrik")

    s = f.scene(150, "slaapwolk")
    wei(s)
    for i, (x, t) in enumerate(plekken[:4]):
        s.spr(slapers[i], x, GY)
    s.spr(held, 46, GY, tot=30)
    s.spr(held_schrik, 46, GY, van=30, tot=60)
    s.spr(held, 46, GY, van=60, tot=84, spiegel=True)
    s.spr(held, 46, GY, van=84, tot=108)
    s.spr(held_half, 46, GY, van=108)
    s.vlak("#fab0d6", pad=[(0, -40, 0, 40, H), (136, 0, 0, 40, H)])
    s.spr("wolkrand_r", pad=[(0, -4, 0), (136, 36, 0)], golf=(0, 2, 40))
    s.vlak("#fab0d6", pad=[(0, B, 0, 40, H), (136, B - 40, 0, 40, H)])
    s.spr("wolkrand", pad=[(0, B - 6, 0), (136, B - 46, 0)], golf=(0, 2, 40, 20))
    s.tekst("Nog wakker: 1", B // 2, 3, schaal=0.75)
    s.zeg(5, 70, "De slaapwolk komt dichterbij. Hij is roze. En heel zacht.")
    s.zeg(78, 146, "Niet in slaap vallen. Niet in slaap vallen. Niet...")
    s.geluid(6, "spanning").geluid(60, "spanning", 1.2).geluid(110, "gaap")
    s.cue(32, "schrik")

    s = f.scene(170, "winst")
    wei(s)
    for i, (x, t) in enumerate(plekken[:4]):
        s.spr(slapers[i], x, GY)
    s.vlak("#fab0d6", 0, 0, 40, H)
    s.spr("wolkrand_r", 36, 0, golf=(0, 2, 40))
    s.vlak("#fab0d6", B - 40, 0, 40, H)
    s.spr("wolkrand", B - 46, 0, golf=(0, 2, 40, 20))
    s.spr(held_blij, pad=[(0, 46, GY), (10, 46, GY - 8, "uit"), (20, 46, GY, "in"), (30, 46, GY - 8, "uit"), (40, 46, GY, "in")], tot=92)
    s.spr(held_half, 46, GY, van=92, tot=118)
    s.spr(held_slaap, 46, GY, van=118)
    s.spr("zzz", 62, GY - 8, van=124)
    s.vlak("#ffd27a", pad=[(0, 0, -14, B, 13), (12, 0, 4, B, 13, "uit")], tot=118)
    s.tekst("#1 VADSOVERWINNING", B // 2, 7, schaal=0.9, kleur="#5a1e3a", schaduw=False, van=12, tot=118)
    s.spr("beker", 51, 18, van=16, tot=92, golf=(0, 1, 20))
    s.vlak("#2a1238", 0, 4, B, 13, van=118)
    s.tekst("Gelijkspel, njeg.", B // 2, 7, schaal=0.9, kleur="#ffe6ee", van=118)
    s.zeg(6, 82, "#1 VADSOVERWINNING! Jij bent als enige nog wakker!")
    s.zeg(92, 166, "...en nu slaapt echt iedereen. Gelijkspel, njeg.")
    s.geluid(8, "fanfare").geluid(94, "gaap").geluid(124, "snurk")
    s.cue(10, "juich").cue(120, "lach").cue(150, "slaap")

    aftiteling(f, ["Gesprongen: 100 guhs", "Geland: 100 guhs", "Wakker gebleven: 0", "De Vadsbus: te laat"], slaper=held_slaap, duur=100)
    return f


FILMS = (skyblok, bedwars, vadsnite)
