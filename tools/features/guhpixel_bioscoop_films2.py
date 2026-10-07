"""
Guhbioscoop films, part 2: Guhmon, Boer zoekt Guh, Among Guhs (the films of grap2's two joke games and of the Among Guhs
parody round). See guhpixel_bioscoop_film.py for the format. Dutch here; English in tools/lang/en/c37_px_bioscoop.json.
"""
from PIL import ImageDraw

from features import guhpixel_bioscoop_tex as tex
from features.guhpixel_bioscoop_film import B, H, Film, aftiteling, guh, titelkaart

GY = 32      # a guh that stands on a floor at y 50


# =====================================================================================================================
# pictures
# =====================================================================================================================
def platform(kleur=(150, 200, 120)):
    im = tex.nieuw(34, 9)
    d = ImageDraw.Draw(im)
    d.ellipse((0, 0, 33, 8), fill=tex.tint(kleur, 0.8))
    d.ellipse((2, 0, 31, 6), fill=tex.rgb(kleur))
    return im


def badge():
    return tex.raster(["....ggg....", "..ggGGGgg..", ".gGGGwGGGg.", ".gGGwwwGGg.", "gGGwwzwwGGg", ".gGGwwwGGg.", ".gGGGwGGGg.", "..ggGGGgg..",
                       "...rg.gr...", "..rr...rr.."],
                      {"g": (220, 160, 40), "G": (255, 214, 90), "w": (255, 250, 220), "z": (120, 90, 200), "r": (214, 60, 80)})


def brievenbus():
    im = tex.nieuw(14, 24)
    d = ImageDraw.Draw(im)
    d.rectangle((6, 10, 7, 23), fill=(120, 84, 52, 255))
    d.rounded_rectangle((0, 0, 13, 10), radius=3, fill=(214, 60, 60, 255))
    d.rectangle((2, 4, 9, 5), fill=(60, 20, 26, 255))
    d.rectangle((11, 1, 12, 5), fill=(250, 220, 90, 255))
    return im


def brief():
    return tex.raster(["wwwwwwwwwww", "wdwwwwwwwdw", "wwdwwwwwdww", "wwwdwrwdwww", "wwwwdrdwwww", "wwwwwwwwwww", "wwwwwwwwwww"],
                      {"w": (250, 246, 236), "d": (190, 180, 170), "r": (230, 70, 110)})


def papier():
    im = tex.nieuw(70, 44, (250, 244, 226, 255))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 69, 43), outline=(200, 186, 150, 255))
    for y in range(26, 40, 5):
        d.line([(8, y), (60, y)], fill=(220, 210, 186, 255))
    for (x, y) in ((58, 5), (61, 8)):
        d.rectangle((x, y, x + 1, y + 1), fill=(240, 100, 140, 255))
    return im


def schuur():
    im = tex.nieuw(60, 40)
    d = ImageDraw.Draw(im)
    d.polygon([(0, 14), (30, 0), (59, 14)], fill=(120, 50, 44, 255))
    d.rectangle((3, 14, 56, 39), fill=(176, 60, 50, 255))
    for x in range(8, 56, 8):
        d.line([(x, 14), (x, 39)], fill=(150, 48, 42, 255))
    d.rectangle((20, 20, 39, 39), fill=(70, 40, 34, 255))
    d.line([(20, 20), (39, 39)], fill=(250, 240, 230, 255))
    d.line([(39, 20), (20, 39)], fill=(250, 240, 230, 255))
    d.rectangle((20, 20, 39, 39), outline=(250, 240, 230, 255))
    return im


def hooi(breed=20):
    im = tex.nieuw(breed, 9)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, breed - 1, 8), radius=2, fill=(236, 200, 96, 255))
    for x in range(2, breed - 1, 3):
        d.line([(x, 1 + x % 2), (x, 6)], fill=(210, 168, 70, 255))
    return im


def gordijn():
    im = tex.nieuw(16, H, (150, 30, 60, 255))
    d = ImageDraw.Draw(im)
    for x in range(0, 16, 4):
        d.line([(x, 0), (x, H - 1)], fill=(110, 20, 44, 255))
        d.line([(x + 1, 0), (x + 1, H - 1)], fill=(186, 50, 80, 255))
    d.rectangle((0, 0, 15, 2), fill=(250, 210, 110, 255))
    return im


def schip():
    """De Vadsvaarder: a wedge of cheese with windows and a flame."""
    im = tex.nieuw(54, 24)
    d = ImageDraw.Draw(im)
    d.polygon([(10, 22), (10, 8), (52, 14), (52, 22)], fill=(250, 208, 96, 255))
    d.polygon([(10, 8), (52, 14), (52, 16), (10, 11)], fill=(255, 232, 150, 255))
    for (x, y) in ((16, 15), (26, 16), (36, 17)):
        d.ellipse((x, y, x + 4, y + 4), fill=(150, 210, 250, 255))
    for (x, y) in ((22, 12), (44, 19), (32, 20)):
        d.ellipse((x, y, x + 2, y + 2), fill=(226, 170, 60, 255))
    d.polygon([(10, 12), (0, 15), (10, 19)], fill=(255, 140, 60, 255))
    d.polygon([(10, 14), (4, 15), (10, 17)], fill=(255, 230, 120, 255))
    return im


def paneel(kleur=(110, 220, 130)):
    im = tex.nieuw(14, 16)
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 13, 15), fill=(70, 80, 104, 255))
    d.rectangle((2, 2, 11, 8), fill=(24, 30, 44, 255))
    d.line([(3, 6), (6, 4)], fill=tex.rgb(kleur))
    d.line([(6, 4), (10, 7)], fill=tex.rgb(kleur))
    for x, c in ((2, (230, 70, 70)), (6, (250, 210, 80)), (10, (110, 220, 130))):
        d.rectangle((x, 11, x + 1, 12), fill=c + (255,))
    return im


def tafel():
    im = tex.nieuw(64, 14)
    d = ImageDraw.Draw(im)
    d.ellipse((0, 0, 63, 11), fill=(120, 130, 156, 255))
    d.ellipse((3, 1, 60, 8), fill=(160, 170, 196, 255))
    d.rectangle((28, 10, 35, 13), fill=(90, 98, 120, 255))
    return im


def noodknop():
    def een(aan):
        im = tex.nieuw(12, 8)
        d = ImageDraw.Draw(im)
        d.rectangle((0, 5, 11, 7), fill=(250, 210, 80, 255))
        d.rounded_rectangle((2, 0 if not aan else 2, 9, 5), radius=2, fill=(255, 90, 90, 255) if aan else (214, 40, 50, 255))
        return im
    return tex.strip([een(False), een(True)])


def luik():
    """The vent: closed, and open with a dark hole."""
    def een(open_):
        im = tex.nieuw(30, 22)
        d = ImageDraw.Draw(im)
        d.rectangle((0, 0, 29, 21), fill=(110, 118, 140, 255))
        if open_:
            d.rectangle((2, 2, 27, 19), fill=(20, 22, 32, 255))
        else:
            for y in range(3, 20, 4):
                d.rectangle((3, y, 26, y + 1), fill=(60, 66, 84, 255))
        return im
    return tex.strip([een(False), een(True)])


def kruimels():
    return tex.raster(["o..o.", "..o..", ".o..o"], {"o": (236, 160, 50)})


# =====================================================================================================================
def guhmon():
    f = Film("guhmon", "Guhmon: Het Grote Gevecht", "Een echt Guhmon-gevecht! Wie het eerst in slaap valt, wint.",
             "Win het Guhmon-gevecht in de Guhpixel-lobby.")
    f.sprite("platform", platform())
    f.sprite("badge", badge())
    leider, leider_blij = guh("goud", "open", "pet:rood"), guh("goud", "blij", "pet:rood")
    eigen, eigen_blij, eigen_half, eigen_slaap = guh("lila"), guh("lila", "blij"), guh("lila", "half"), guh("lila", "dicht")
    tegen, tegen_boos, tegen_half, tegen_slaap = (guh("mint", o, "muts:blauw") for o in ("open", "boos", "half", "dicht"))
    BAR = 40

    def veld(s, e0, e1, g0=0, g1=0, ogen_eigen=eigen, ogen_tegen=tegen, van=20, tot=60):
        """The battle screen; the sleep bars go from e0 to e1 (yours) and g0 to g1 (the other guh's) between van and tot."""
        s.vlak("#d8f0c8")
        s.vlak("#c0e4b0", 0, 44, B, 20)
        s.spr("platform", 70, 26)
        s.spr("platform", 8, 44)
        s.spr(ogen_tegen, 76, 10, golf=(0, 0.6, 26))
        s.spr(ogen_eigen, 14, 28, golf=(0, 0.6, 26, 10))
        for (x, y, a, b, naam) in ((4, 3, g0, g1, "DUTJES' GUH"), (60, 33, e0, e1, "GUH")):
            s.vlak("#3a2a48", x, y, 48, 13)
            s.vlak("#fff6fb", x + 1, y + 1, 46, 11)
            s.tekst(naam, x + 3, y + 2, schaal=0.5, uitlijn="links", kleur="#3a2a48", schaduw=False)
            s.tekst("SLAAP", x + 3, y + 7, schaal=0.4, uitlijn="links", kleur="#b06090", schaduw=False)
            s.vlak("#5a4a64", x + 5 + 14, y + 7, BAR - 14, 3)
            s.vlak("#7ad0ff", pad=[(0, x + 19, y + 7, (BAR - 14) * a / 100, 3), (van, x + 19, y + 7, (BAR - 14) * a / 100, 3),
                                   (tot, x + 19, y + 7, (BAR - 14) * b / 100, 3, "zacht")])

    titelkaart(f, "GUHMON", "Slaap ze allemaal!", achter="#2a4a7a", kleur="#ffe27a",
               versier=lambda s: (s.spr("ster", 14, 40), s.spr("ster", 92, 44), s.spr("badge", 50, 46, golf=(0, 1, 30))))

    s = f.scene(130, "uitdaging")
    s.vlak("#f0d8a0")
    s.vlak("#d8b878", 0, 50, B, 14)
    s.spr(leider, pad=[(0, 120, GY), (30, 70, GY, "uit")], golf=(0, 0.8, 8), tot=30)
    s.spr(leider, 70, GY, van=30, tot=90)
    s.spr(leider_blij, 70, GY, van=90)
    s.spr(eigen, 18, GY)
    s.zeg(8, 68, "Gymleider Dutjes daagt je uit voor een Guhmon-gevecht!")
    s.zeg(74, 126, "Wie het eerst slaapt, wint. Echt waar, njeg.")
    s.geluid(30, "guh").geluid(74, "pling")

    s = f.scene(130, "vadsen")
    veld(s, 0, 30, 0, 0, ogen_eigen=eigen_half)
    s.tekst("+30", 80, 26, schaal=0.6, kleur="#3a8ad0", van=24, tot=70, pad=[(24, 96, 30), (70, 96, 22)])
    s.zeg(5, 62, "Guh gebruikt Vadsen!")
    s.zeg(68, 126, "Het is super vadsig!")
    s.geluid(8, "gaap").geluid(68, "pling")
    s.cue(72, "lach")

    s = f.scene(130, "njeg")
    veld(s, 30, 15, 0, 0, ogen_tegen=tegen_boos)
    s.spr("ballon", 44, 0, van=10, tot=64, spiegel=True)
    s.tekst("NJEG!", 64, 4, schaal=0.9, kleur="#d63c3c", schaduw=False, van=10, tot=64)
    s.zeg(5, 62, "Dutjes' guh gebruikt Njeg!")
    s.zeg(68, 126, "Het is niet erg effectief...")
    s.geluid(10, "njeg")
    s.cue(74, "lach")

    s = f.scene(140, "knabbel")
    veld(s, 15, 55, 0, 0, ogen_eigen=eigen_blij, van=30, tot=120)
    s.spr("knabbel", pad=[(0, 90, 50), (26, 30, 30, "uit")], tot=30, r=0)
    s.zeg(5, 66, "Guh gebruikt Knabbel eten! Smak, smak.")
    s.zeg(72, 136, "Volle maag: elke beurt een beetje slaperiger.")
    s.geluid(28, "smak").geluid(44, "smak", 1.1)

    s = f.scene(170, "dutje")
    veld(s, 55, 55, 0, 85, ogen_eigen=eigen_half, ogen_tegen=tegen_half, van=8, tot=40)
    # (the last stretch: your bar runs full a little after the other one shot up)
    s.vlak("#7ad0ff", pad=[(96, 79, 40, (BAR - 14) * 0.55, 3), (130, 79, 40, BAR - 14, 3, "zacht")], van=96)
    s.vlak("#d8f0c8", 12, 26, 26, 20, van=134)
    s.spr("platform", 8, 44, van=134)
    s.spr(eigen_slaap, 14, 28, van=134)
    s.spr("zzz", 34, 20, van=138)
    s.tekst("!", 100, 6, schaal=1.5, kleur="#d63c3c", van=10, tot=60)
    s.zeg(5, 76, "Dutjes' guh gebruikt Dutje! Dat gaat hard!")
    s.zeg(84, 166, "Guh gebruikt Vadsen... en valt als eerste in slaap!")
    s.geluid(6, "spanning").geluid(96, "gaap").geluid(136, "snurk")
    s.cue(12, "schrik")

    s = f.scene(150, "winst")
    veld(s, 100, 100, 85, 100, ogen_eigen=eigen_slaap, ogen_tegen=tegen_slaap, van=60, tot=110)
    s.spr("zzz", 34, 20)
    s.spr("zzz", 96, 2, van=112)
    s.vlak("#2a4a7a", 22, 14, 68, 16, tot=70)
    s.tekst("JE WINT!", B // 2, 17, schaal=1.25, kleur="#ffe27a", tot=70)
    s.spr("badge", pad=[(70, 50, -12), (88, 50, 16, "uit")], van=70)
    s.zeg(5, 66, "Guh slaapt! Je wint het gevecht!")
    s.zeg(72, 146, "Jij sliep sneller. Hier is je gymbadge, njeg.")
    s.geluid(4, "fanfare").geluid(88, "pling")
    s.cue(8, "juich").cue(120, "slaap")

    aftiteling(f, ["Vadsen: super vadsig", "Njeg: niet effectief", "Knabbels: op", "Wakker: niemand"], slaper=eigen_slaap, duur=100)
    return f


# =====================================================================================================================
def bzg():
    f = Film("bzg", "Boer zoekt Guh", "Boer Guhrrit zoekt een guh die met hem wil vadsen. Er zijn drie brieven binnen!",
             "Speel Boer zoekt Guh uit in de Guhpixel-lobby.")
    for naam, im in (("brievenbus", brievenbus()), ("brief", brief()), ("papier", papier()), ("schuur", schuur()), ("hooi", hooi()),
                     ("hooi_breed", hooi(96)), ("gordijn", gordijn()), ("deken", tex.deken(92))):
        f.sprite(naam, im)
    guhvon, guhvon_blij = guh("roze", "open", "strik", "microfoon"), guh("roze", "blij", "strik", "microfoon")
    boer, boer_blij, boer_schrik, boer_slaap = (guh("goud", o, "strohoed", "overall") for o in ("open", "blij", "schrik", "dicht"))
    kand = [guh(k, "open", "wangen") for k in ("mint", "choco", "sneeuw")]
    kand_slaap = [guh(k, "dicht", "wangen") for k in ("mint", "choco", "sneeuw")]

    def studio(s):
        s.vlak("#5a2050")
        s.vlak("#7a3470", 28, 0, 56, H)
        s.vlak("#3a1434", 0, 50, B, 14)
        s.spr("gordijn", 0, 0)
        s.spr("gordijn", B - 16, 0, spiegel=True)

    def erf(s):
        s.vlak("#a8dcff")
        s.spr("wolk", 8, 6)
        s.spr("wolk_groot", 70, 4)
        s.spr("schuur", 46, 10)
        s.vlak("#78c46a", 0, 50, B, 14)
        s.vlak("#62ae58", 0, 50, B, 1)

    titelkaart(f, "BOER ZOEKT GUH", "Iedereen valt in slaap", achter="#7a2a5a", schaal=1.0,
               versier=lambda s: (s.spr("hart", 20, 44, golf=(0, 1, 20)), s.spr("hart", 86, 44, golf=(0, 1, 20, 10))))

    s = f.scene(150, "studio")
    studio(s)
    s.spr(guhvon, 45, GY, tot=80)
    s.spr(guhvon_blij, 45, GY, van=80)
    s.zeg(8, 76, "Welkom bij Boer zoekt Guh! Ik ben Presentatrice Guhvon.")
    s.zeg(82, 146, "Boer Guhrrit zoekt een guh die samen met hem wil vadsen.")
    s.geluid(8, "guh").geluid(82, "pling")

    s = f.scene(140, "brievenbus")
    erf(s)
    s.spr("brievenbus", 90, 26)
    s.spr(boer, 12, GY, tot=40)
    s.spr(boer_blij, pad=[(40, 12, GY), (48, 12, GY - 6, "uit"), (56, 12, GY, "in")], van=40)
    for i, t in enumerate((20, 34, 48)):
        s.spr("brief", pad=[(t, 92, 28), (t + 16, 60, 8 + i * 4, "uit"), (t + 34, 30 + i * 3, 30 + i * 3, "in")], van=t, r=0)
    s.zeg(5, 68, "Boer Guhrrit, er zijn drie brieven voor je binnen!")
    s.zeg(76, 136, "Drie! Dat is bijna vier, njeg.")
    s.geluid(20, "plof").geluid(34, "plof", 1.1).geluid(48, "plof", 1.2).geluid(76, "vahoeg")
    s.cue(80, "lach")

    s = f.scene(260, "brieven")
    studio(s)
    s.spr("papier", pad=[(0, 21, 70), (14, 21, 2, "uit")])
    for i, (kop, regel, t0, t1) in enumerate((("Brief 1", "Ik hou van slapen.", 14, 90), ("Brief 2", "Ik ook. En knabbels.", 90, 170),
                                             ("Brief 3", "Zzzzzzzz...", 170, 260))):
        s.tekst(kop, 27, 7, schaal=0.75, uitlijn="links", kleur="#b03060", schaduw=False, van=t0, tot=t1)
        s.tekst(regel, 27, 17, schaal=0.5, uitlijn="links", kleur="#3a2a48", schaduw=False, van=t0 + 6, tot=t1)
    s.spr(kand_slaap[2], 62, 22, van=186, s=0.8)
    s.spr("zzz", 80, 16, van=190)
    s.zeg(14, 86, "Brief één: 'Lieve boer, ik hou heel veel van slapen.'")
    s.zeg(92, 166, "Brief twee: 'Ik hou óók van slapen. En van knabbels.'")
    s.zeg(172, 256, "Brief drie is tijdens het schrijven in slaap gevallen.")
    s.geluid(14, "klik").geluid(90, "klik").geluid(170, "klik").geluid(190, "snurk")
    s.cue(100, "lach").cue(192, "lach")

    s = f.scene(160, "logeerweek")
    s.vlak("#3a2a30")
    s.vlak("#6a4030", 0, 0, B, 40)
    for x in range(0, B, 14):
        s.vlak("#5a3428", x, 0, 1, 40)
    s.vlak("#2a2036", 78, 6, 20, 14)
    s.spr("ster", 82, 9)
    s.spr("ster", 91, 13)
    s.spr("hooi_breed", 8, 46)
    for i, wie in enumerate((kand_slaap[0], kand_slaap[1], boer_slaap, kand_slaap[2])):
        s.spr(wie, 10 + i * 23, GY - 2, golf=(0, 0.5, 30, i * 7))
    s.spr("deken", pad=[(70, 10, 70), (110, 10, 43, "uit")], van=70)
    s.spr("zzz", 30, 18)
    s.spr("zzz", 76, 16)
    s.spr(guhvon, pad=[(40, 116, GY + 6), (60, 98, GY + 6, "uit")], van=40, spiegel=True, s=0.8)
    s.zeg(5, 78, "De logeerweek! Dag één tot en met zeven: iedereen slaapt.")
    s.zeg(86, 156, "Sssst... we stoppen ze lekker in.")
    s.geluid(10, "snurk").geluid(60, "snurk", 1.2).geluid(110, "plof", 0.8)
    s.cue(112, "snik")

    s = f.scene(210, "keuze")
    studio(s)
    s.spr(kand[0], 14, GY)
    s.spr(kand[1], 76, GY)
    s.spr(kand[2], 92, GY - 3, s=0.8)
    for t in range(0, 140, 28):
        s.spr(boer, 46, GY, van=t, tot=t + 14)
        s.spr(boer, 46, GY, van=t + 14, tot=t + 28, spiegel=True)
    s.spr(boer_schrik, 46, GY, van=140, tot=160)
    s.spr(boer_blij, pad=[(160, 46, GY), (168, 46, GY - 7, "uit"), (176, 46, GY, "in")], van=160)
    for i, x in enumerate((30, 66, 50)):
        s.spr("hart", pad=[(164 + i * 6, x, 30), (206, x, 8)], van=164 + i * 6)
    s.zeg(5, 62, "Boer Guhrrit, het is zover. Wie kies je?")
    s.zeg(68, 156, "...Ik kan niet kiezen, njeg. Ze snurken alle drie zo mooi.")
    s.zeg(162, 206, "Dan worden we allemaal vrienden!")
    s.geluid(6, "spanning").geluid(40, "spanning", 1.1).geluid(140, "njeg").geluid(162, "fanfare")
    s.cue(144, "lach").cue(166, "juich")

    s = f.scene(110, "dutje")
    erf(s)
    s.spr("hooi_breed", 8, 46)
    for i, wie in enumerate((kand_slaap[0], boer_slaap, kand_slaap[1], kand_slaap[2])):
        s.spr(wie, 10 + i * 23, GY - 2, golf=(0, 0.5, 30, i * 7))
    s.spr("hart", 52, 12, golf=(0, 1.5, 24), s=2)
    s.spr("zzz", 34, 18)
    s.spr("zzz", 80, 18)
    s.zeg(8, 104, "En ze vadsten nog lang en gelukkig.")
    s.geluid(10, "snurk")
    s.cue(40, "slaap")

    aftiteling(f, ["Presentatie: Guhvon", "Boer: Guhrrit", "Brieven: 3", "Gekozen: iedereen"], slaper=boer_slaap, duur=90)
    return f


# =====================================================================================================================
def among():
    f = Film("among", "Among Guhs: De Film", "Aan boord van De Vadsvaarder is iemand de Mika. Maar wie is er eigenlijk wakker?",
             "Speel de Among Guhs-parodieronde uit bij de Kapitein-guh in de Guhpixel-lobby.")
    for naam, im in (("schip", schip()), ("paneel", paneel()), ("paneel_rood", paneel((230, 70, 70))), ("tafel", tafel()), ("kruimels", kruimels())):
        f.sprite(naam, im)
    f.sprite("noodknop", noodknop(), frames=2, per=5)
    f.sprite("luik", luik(), frames=2, per=4)
    kleuren = ("rood", "blauw", "groen", "geel")
    wakker = {k: guh(k, "open", "pak") for k in kleuren}
    slaap = {k: guh(k, "dicht", "pak") for k in kleuren}
    schrik = {k: guh(k, "schrik", "pak") for k in kleuren}
    kapitein, kapitein_blij = guh("wit", "open", "pak", "kapitein"), guh("wit", "blij", "pak", "kapitein")
    mika_slaap = guh("mika", "dicht", "tand", binnen=(214, 128, 150))

    def gang(s):
        s.vlak("#3a4660")
        s.vlak("#465474", 0, 0, B, 8)
        s.vlak("#2a3248", 0, 50, B, 14)
        for x in range(0, B, 28):
            s.vlak("#303a52", x, 8, 1, 42)

    titelkaart(f, "AMONG GUHS", "Wie is er nog wakker?", achter="#0e1024", schaal=1.5,
               versier=lambda s: (s.spr("sterren", 0, 0), s.spr("schip", pad=[(0, -56, 40), (70, 30, 42, "uit")])))

    s = f.scene(110, "schip")
    s.vlak("#0e1024")
    s.spr("sterren", pad=[(0, 0, 0), (110, -10, 0)])
    s.spr("schip", pad=[(0, 20, 22), (110, 36, 20)], golf=(0, 1.5, 40))
    s.zeg(8, 104, "Aan boord van De Vadsvaarder werkt de crew keihard.")
    s.geluid(6, "wind", 0.7)

    s = f.scene(150, "taken")
    gang(s)
    for i, k in enumerate(kleuren):
        s.spr("paneel", 6 + i * 27, 22)
        s.spr(slaap[k], 2 + i * 27, GY, golf=(0, 0.5, 30, i * 8))
        s.spr("zzz", 18 + i * 27, 10 + (i % 2) * 4)
    s.vlak("#18101e", 3, 2, 60, 5)
    s.tekst("TAKEN: 0%", 5, 3, schaal=0.5, uitlijn="links", kleur="#68d88a", schaduw=False)
    s.zeg(5, 72, "Nou ja. Hard. Alle taken staan nog op nul.")
    s.zeg(80, 146, "Maar ze liggen er wel heel mooi bij, njeg.")
    s.geluid(10, "snurk").geluid(70, "snurk", 1.2)
    s.cue(30, "lach")

    s = f.scene(210, "noodknop")
    gang(s)
    s.vlak("#d63c3c", 0, 0, B, 8, tot=60)
    s.tekst("NOODVERGADERING!", B // 2, 1, schaal=0.75, kleur="#ffffff", schaduw=False, tot=60)
    for i, k in enumerate(kleuren):
        x = 2 + i * 29
        s.spr(schrik[k], x, GY - 14, tot=60)
        s.spr(wakker[k], x, GY - 14, van=60, tot=140) if k != "rood" else s.spr(wakker[k], x, GY - 14, van=60, tot=80)
        if k == "rood":
            s.spr(schrik[k], x, GY - 14, van=80)
        else:
            s.spr(wakker[k], x, GY - 14, van=140, spiegel=True)
    s.spr("tafel", 24, 40)
    s.spr("noodknop", 50, 36, tot=60)
    s.spr("noodknop", 50, 36, van=60, frame=0)
    s.spr("ballon", 46, 0, van=72, tot=140)
    s.tekst("Rood is sus!", 66, 4, schaal=0.5, kleur="#d63c3c", schaduw=False, van=72, tot=140)
    s.zeg(5, 66, "Noodvergadering! Iemand heeft op de knop geleund.")
    s.zeg(72, 136, "'Rood is sus, njeg.' 'Waarom?'")
    s.zeg(142, 206, "'Rood is altijd sus.'")
    s.geluid(2, "piep").geluid(14, "piep").geluid(26, "piep").geluid(72, "njeg").geluid(142, "njeg", 0.8)
    s.cue(6, "schrik").cue(150, "lach")

    s = f.scene(150, "stemming")
    gang(s)
    for i, k in enumerate(kleuren):
        x = 2 + i * 29
        s.spr(wakker[k], x, GY - 14)
        s.spr("kruimels", x + 8, GY + 5, van=70)
    s.spr("tafel", 24, 40)
    s.spr("knabbel", 52, 34, tot=40)
    s.spr("kruimels", 53, 38, van=40)
    s.tekst("?", B // 2, 10, schaal=1.5, kleur="#ffe27a", van=40, tot=70)
    s.zeg(5, 66, "De stemming: wie at de laatste knabbel?")
    s.zeg(74, 146, "Iedereen heeft kruimels. Zeer verdacht allemaal.")
    s.geluid(40, "smak").geluid(74, "oeh")
    s.cue(78, "lach")

    s = f.scene(160, "luik")
    gang(s)
    s.spr("luik", 41, 18, frame=0, tot=50)
    s.spr("luik", 41, 18, frame=1, van=50)
    s.spr(mika_slaap, 45, 20, van=50, golf=(0, 0.5, 30))
    s.spr("zzz", 66, 12, van=58)
    s.spr(wakker["blauw"], pad=[(0, -24, GY), (30, 6, GY, "uit")], golf=(0, 0.8, 8), tot=30)
    s.spr(wakker["blauw"], 6, GY, van=30, tot=50)
    s.spr(schrik["blauw"], 6, GY, van=50, tot=90)
    s.spr(wakker["blauw"], 6, GY, van=90)
    s.zeg(5, 76, "En de Mika? Die ligt te slapen in het ventilatieluik.")
    s.zeg(84, 156, "Sabotage: nul. Geduwd: niemand. Gesnurkt: flink.")
    s.geluid(50, "klik").geluid(52, "mika").geluid(70, "snurk", 0.8)
    s.cue(52, "schrik").cue(90, "lach")

    s = f.scene(150, "winst")
    s.vlak("#0e1024")
    s.spr("sterren", 0, 0)
    s.vlak("#68d88a", 0, 4, B, 15, tot=76)
    s.tekst("CREW WINT", B // 2, 7, schaal=1.25, kleur="#12301c", schaduw=False, tot=76)
    for i, k in enumerate(kleuren):
        s.spr(slaap[k], 2 + i * 21, GY + 2, golf=(0, 0.5, 30, i * 8))
    s.spr(kapitein, 88, GY, tot=76)
    s.spr(kapitein_blij, pad=[(76, 88, GY), (84, 88, GY - 6, "uit"), (92, 88, GY, "in")], van=76)
    s.zeg(5, 72, "Crew wint! Omdat niemand ook maar iets gedaan heeft.")
    s.zeg(78, 146, "Kapitein-guh: 'Wil je het ook eens écht proberen, njeg?'")
    s.geluid(4, "fanfare").geluid(78, "guh")
    s.cue(8, "juich").cue(120, "slaap")

    aftiteling(f, ["Taken gedaan: 0", "Rood: nog steeds sus", "De Mika: sliep", "De crew: ook"], slaper=slaap["rood"], duur=90)
    return f


FILMS = (guhmon, bzg, among)
