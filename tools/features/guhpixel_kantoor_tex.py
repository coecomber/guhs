"""
Textures of the Guhkantoor (guhpixel slice "kantoor"): drawn pixel by pixel with fixed seeds, so every run gives the same
files. Block textures are 16 x 16 (the two papers on the wall 32 x 32 so the little lines stay readable); item icons 16 x 16.
"""
import random

from PIL import Image, ImageDraw

BEIGE = (222, 208, 176)
BEIGE_D = (190, 174, 140)
BEIGE_L = (238, 228, 202)
HOUT = (232, 190, 170)
HOUT_D = (204, 156, 138)
ROZE = (247, 150, 190)
ROZE_D = (214, 110, 156)
PAPIER = (250, 244, 226)
PAPIER_D = (226, 216, 190)
INKT = (74, 56, 62)
GOUD = (240, 196, 80)
GOUD_D = (196, 148, 44)
SCHERM = (22, 30, 46)


def _ruis(img, seed, sterkte=7):
    rng = random.Random(seed)
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            d = rng.randint(-sterkte, sterkte)
            px[x, y] = (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), a)
    return img


def hout():
    img = Image.new("RGBA", (16, 16), HOUT + (255,))
    d = ImageDraw.Draw(img)
    for y in (3, 8, 12):
        d.line([(0, y), (15, y)], fill=HOUT_D + (255,))
    d.point([(4, 5), (11, 10), (7, 14), (13, 1)], fill=HOUT_D + (255,))
    d.rectangle([0, 0, 15, 15], outline=(186, 136, 122, 255))
    return _ruis(img, 3601, 4)


def computer():
    img = Image.new("RGBA", (16, 16), BEIGE + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=BEIGE_D + (255,))
    d.line([(1, 1), (14, 1)], fill=BEIGE_L + (255,))
    for y in (5, 7, 9, 11):   # the vents
        d.line([(4, y), (11, y)], fill=BEIGE_D + (255,))
    d.point([(13, 13)], fill=(110, 220, 130, 255))   # the little power light
    return _ruis(img, 3602, 4)


def scherm(bezet):
    """The monitor's front: a beige rim and the tube. Empty desk: the screensaver (a bouncing cheese nibble). A guh asleep on
    the keyboard: endless lines of the same letter and a blinking cursor."""
    img = Image.new("RGBA", (16, 16), BEIGE + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=BEIGE_D + (255,))
    d.rectangle([2, 2, 13, 12], fill=SCHERM + (255,))
    d.point([(13, 14)], fill=(110, 220, 130, 255))
    d.line([(3, 14), (6, 14)], fill=BEIGE_D + (255,))
    if bezet:
        groen = (120, 240, 150, 255)
        rng = random.Random(3603)
        for y in (3, 5, 7, 9):
            lengte = 9 if y < 9 else rng.randint(3, 6)
            for x in range(3, 3 + lengte):
                if (x + y) % 5:
                    img.putpixel((x, y), groen)
        img.putpixel((3 + 6, 11), (255, 255, 255, 255))
    else:
        d.rectangle([8, 6, 10, 8], fill=(250, 200, 70, 255))
        d.point([(9, 7)], fill=(220, 150, 40, 255))
        d.point([(4, 4), (6, 10), (12, 4)], fill=(60, 80, 120, 255))
    return img


def toetsenbord():
    img = Image.new("RGBA", (16, 16), BEIGE_D + (255,))
    d = ImageDraw.Draw(img)
    for y in range(1, 15, 3):
        for x in range(1, 15, 3):
            d.rectangle([x, y, x + 1, y + 1], fill=BEIGE_L + (255,))
    d.rectangle([4, 13, 11, 14], fill=BEIGE_L + (255,))   # the space bar
    return img


def kruk():
    img = Image.new("RGBA", (16, 16), ROZE + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=ROZE_D + (255,))
    d.point([(5, 5), (10, 5), (5, 10), (10, 10)], fill=ROZE_D + (255,))   # buttons of the cushion
    return _ruis(img, 3604, 5)


def metaal():
    img = Image.new("RGBA", (16, 16), (176, 180, 190, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(132, 136, 150, 255))
    d.line([(1, 1), (14, 1)], fill=(214, 218, 226, 255))
    d.point([(2, 13), (13, 13), (2, 3), (13, 3)], fill=(110, 114, 128, 255))   # screws
    return _ruis(img, 3605, 4)


def prikklok_voor():
    """The front of the Prikklok: a round dial (ten past nap o'clock), the card slot and a pink card sticking out."""
    img = metaal()
    d = ImageDraw.Draw(img)
    d.ellipse([3, 1, 12, 10], fill=(250, 248, 240, 255), outline=INKT + (255,))
    for (x, y) in ((7, 2), (11, 5), (8, 9), (4, 6)):
        img.putpixel((x, y), ROZE_D + (255,))
    d.line([(8, 5), (8, 3)], fill=INKT + (255,))
    d.line([(8, 5), (10, 6)], fill=INKT + (255,))
    d.rectangle([4, 12, 11, 12], fill=(40, 40, 50, 255))   # the slot
    d.rectangle([6, 13, 9, 15], fill=ROZE + (255,))        # the card
    d.line([(7, 14), (8, 14)], fill=ROZE_D + (255,))
    return img


def _regels(d, x0, x1, ys, rng, kleur=INKT):
    for y in ys:
        d.line([(x0, y), (x1 - rng.randint(0, 4), y)], fill=kleur + (255,))


def loonstrookje(groot=True):
    """A narrow slip: a pink heading, lines, a dotted tear line and a red stamp."""
    n = 32 if groot else 16
    img = Image.new("RGBA", (n, n), PAPIER + (255,))
    d = ImageDraw.Draw(img)
    rng = random.Random(3606)
    d.rectangle([0, 0, n - 1, n - 1], outline=PAPIER_D + (255,))
    if groot:
        d.rectangle([3, 3, 28, 6], fill=ROZE + (255,))
        _regels(d, 3, 28, (9, 11, 13), rng)
        for x in range(3, 29, 2):
            img.putpixel((x, 16), PAPIER_D + (255,))
        _regels(d, 3, 22, (19, 21, 23), rng)
        d.line([(24, 19), (28, 19)], fill=INKT + (255,))
        d.line([(24, 23), (28, 23)], fill=INKT + (255,))
        d.rectangle([17, 25, 28, 29], outline=(214, 70, 90, 255))
        d.line([(19, 27), (26, 27)], fill=(214, 70, 90, 255))
    return img


def kwartaalrapport():
    """A report page: a heading, a flat line graph, equal bars and a one-slice pie."""
    img = Image.new("RGBA", (32, 32), PAPIER + (255,))
    d = ImageDraw.Draw(img)
    rng = random.Random(3607)
    d.rectangle([0, 0, 31, 31], outline=PAPIER_D + (255,))
    d.rectangle([3, 3, 28, 5], fill=(120, 150, 220, 255))
    _regels(d, 3, 28, (8,), rng)
    d.line([(4, 11), (4, 18)], fill=INKT + (255,))          # the flat line graph
    d.line([(4, 18), (14, 18)], fill=INKT + (255,))
    d.line([(5, 17), (14, 17)], fill=(214, 70, 90, 255))
    for i, x in enumerate((18, 21, 24, 27)):                # the equal bars
        d.rectangle([x, 12, x + 1, 18], fill=(ROZE if i % 2 == 0 else ROZE_D) + (255,))
    d.line([(17, 18), (29, 18)], fill=INKT + (255,))
    d.ellipse([4, 21, 11, 28], fill=ROZE + (255,), outline=ROZE_D + (255,))   # the pie: 100 %
    _regels(d, 14, 28, (22, 24, 26, 28), rng)
    return img


def oorkonde():
    """A certificate in a golden frame: a ribbon, a star and a paw print for a signature."""
    img = Image.new("RGBA", (32, 32), PAPIER + (255,))
    d = ImageDraw.Draw(img)
    rng = random.Random(3608)
    d.rectangle([0, 0, 31, 31], outline=GOUD_D + (255,))
    d.rectangle([1, 1, 30, 30], outline=GOUD + (255,))
    d.rectangle([2, 2, 29, 29], outline=GOUD_D + (255,))
    d.rectangle([7, 5, 24, 7], fill=GOUD + (255,))
    for (x, y) in ((15, 10), (16, 10), (14, 11), (15, 11), (16, 11), (17, 11), (15, 12), (16, 12), (14, 13), (17, 13)):   # a star
        img.putpixel((x, y), GOUD_D + (255,))
    _regels(d, 6, 26, (16, 18, 20), rng)
    d.ellipse([21, 22, 25, 26], fill=(214, 70, 90, 255))    # the seal
    d.line([(22, 27), (22, 29)], fill=(214, 70, 90, 255))
    d.line([(24, 27), (24, 29)], fill=(214, 70, 90, 255))
    d.rectangle([7, 24, 9, 26], fill=ROZE_D + (255,))       # the paw print
    d.point([(6, 23), (8, 22), (10, 23)], fill=ROZE_D + (255,))
    return img


def rand():
    """The thin edge of a sheet of paper."""
    img = Image.new("RGBA", (16, 16), PAPIER_D + (255,))
    return _ruis(img, 3609, 3)


def icoon(soort):
    """Item icons (16 x 16, transparent around the sheet)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if soort == "loonstrookje":
        d.rectangle([4, 1, 11, 14], fill=PAPIER + (255,), outline=PAPIER_D + (255,))
        d.rectangle([5, 2, 10, 3], fill=ROZE + (255,))
        for y in (5, 7, 9):
            d.line([(5, y), (9, y)], fill=INKT + (255,))
        d.rectangle([7, 11, 10, 13], outline=(214, 70, 90, 255))
    elif soort == "kwartaalrapport":
        d.rectangle([2, 1, 13, 14], fill=PAPIER + (255,), outline=PAPIER_D + (255,))
        d.rectangle([3, 2, 12, 3], fill=(120, 150, 220, 255))
        d.line([(3, 8), (7, 8)], fill=(214, 70, 90, 255))
        d.line([(3, 9), (7, 9)], fill=INKT + (255,))
        for x in (9, 11):
            d.rectangle([x, 6, x, 9], fill=ROZE_D + (255,))
        d.ellipse([3, 10, 6, 13], fill=ROZE + (255,))
        d.line([(8, 12), (12, 12)], fill=INKT + (255,))
    else:
        d.rectangle([1, 2, 14, 13], fill=PAPIER + (255,), outline=GOUD_D + (255,))
        d.rectangle([2, 3, 13, 12], outline=GOUD + (255,))
        d.rectangle([5, 5, 10, 5], fill=GOUD_D + (255,))
        d.line([(4, 8), (11, 8)], fill=INKT + (255,))
        d.ellipse([10, 9, 12, 11], fill=(214, 70, 90, 255))
        d.point([(4, 10), (5, 10), (4, 11)], fill=ROZE_D + (255,))
    return img


def alles(h):
    h.save(hout(), "block", "guhkantoor_hout.png")
    h.save(computer(), "block", "guhkantoor_computer.png")
    h.save(scherm(False), "block", "guhkantoor_scherm.png")
    h.save(scherm(True), "block", "guhkantoor_scherm_bezet.png")
    h.save(toetsenbord(), "block", "guhkantoor_toetsenbord.png")
    h.save(kruk(), "block", "guhkantoor_kruk.png")
    h.save(metaal(), "block", "guhkantoor_metaal.png")
    h.save(prikklok_voor(), "block", "guhkantoor_prikklok_voor.png")
    h.save(loonstrookje(), "block", "guhkantoor_loonstrookje.png")
    h.save(kwartaalrapport(), "block", "guhkantoor_kwartaalrapport.png")
    h.save(oorkonde(), "block", "guhkantoor_oorkonde.png")
    h.save(rand(), "block", "guhkantoor_papierrand.png")
    for soort in ("loonstrookje", "kwartaalrapport", "oorkonde"):
        h.save(icoon(soort), "item", f"guhkantoor_{soort}.png")
