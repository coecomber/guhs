"""
Het Guhpad - the two pictures of the Guhdex tab Verhalen (Java: feature/guhpad/client/GuhpadTab):

  textures/gui/guhpad/padkaart.png   256x76: the path map of the whole Guhpad on parchment (the painter of
                                     features/verhaal_motor.py, so it looks like the travel maps): from left to right the
                                     Guhmensie (pink hills, blossom trees, a guh house), the Guhbarbecuether (a smoking
                                     mountain over the sauce sea), the Guheinde (pale islands in the dark) and a black cloud
                                     nobody knows anything about. The game draws the four stops, the dotted path between
                                     them, the names and the pips on top (HALTES: the same pixels as GuhpadTab.HALTE_X / _Y).
  textures/gui/guhpad/echt.png       256x48: the preview of "Het echte Guheinde": black shapes with a rim of light, question
                                     marks and a few stars. Nothing in it is a real character or place.

Everything is drawn here (no hand-made files) and deterministic.
"""
import math
import random

from PIL import Image, ImageDraw

from features import verhaal_motor

KAART_W, KAART_H = 256, 76
ECHT_W, ECHT_H = 256, 48
# the four stops on the path map: the Guhmensie, the Guhbarbecuether, the Guheinde, the real Guheinde
HALTES = [(34, 30), (98, 24), (162, 30), (226, 24)]
BAND = 42            # the path map is this band of a whole parchment (rows BAND .. BAND + KAART_H)
INKT = (74, 50, 32, 255)


def padkaart():
    k = verhaal_motor.Kaart(seed=41)
    d, y0 = k.d, BAND

    def p(x, y):
        return (x, y + y0)

    # (the scenery keeps to the lower third: the game draws the stops, their names and their pips in the sky above it)
    # --- the Guhmensie: pink hills, blossom trees, a guh house ------------------------------------------------------------
    k.land([p(3, 73), p(3, 60), p(12, 55), p(26, 61), p(44, 62), p(58, 56), p(70, 63), p(70, 73)], kleur=(232, 202, 196, 255))
    k.berg(10, y0 + 60, 8, kleur=(226, 170, 190, 255))
    k.berg(60, y0 + 61, 8, kleur=(226, 170, 190, 255))
    for x, y in ((6, 70), (36, 71), (48, 69), (66, 71)):
        k.boom(x, y0 + y, kleur=(244, 150, 184, 255))
    k.huisje(22, y0 + 71)
    # --- the Guhbarbecuether: a smoking mountain over the sauce sea ----------------------------------------------------------
    k.water([p(74, 73), p(74, 63), p(86, 58), p(104, 61), p(128, 56), p(134, 64), p(134, 73)], kleur=(236, 150, 60, 255))
    k.land([p(108, 66), p(114, 60), p(131, 60), p(134, 66), p(126, 70), p(112, 70)], kleur=(150, 110, 96, 255))
    k.vulkaan(123, y0 + 66, 13)
    k.land([p(78, 69), p(83, 64), p(92, 66), p(90, 71), p(81, 72)], kleur=(150, 110, 96, 255))
    # --- the Guheinde: pale islands in the dark ----------------------------------------------------------------------------
    d.polygon([p(138, 73), p(138, 64), p(146, 58), p(176, 61), p(190, 56), p(194, 64), p(194, 73)], fill=(86, 64, 112, 255), outline=INKT)
    k.land([p(141, 70), p(147, 65), p(156, 67), p(154, 71), p(145, 72)], kleur=(236, 224, 170, 255))
    k.land([p(176, 69), p(183, 64), p(192, 67), p(188, 71), p(178, 72)], kleur=(236, 224, 170, 255))
    k.toren(184, y0 + 66, 7, kleur=(200, 170, 226, 255))
    for x, y in ((160, 69), (167, 71), (171, 65), (152, 62)):
        d.point(p(x, y), fill=(250, 240, 200, 255))
    # --- the real Guheinde: a black cloud, nothing known ---------------------------------------------------------------------
    rng = random.Random(6)
    wolk = [(212, 66), (222, 61), (234, 62), (243, 66), (240, 71), (226, 72), (212, 71), (206, 68)]
    for x, y in wolk:
        r = rng.randint(6, 8)
        d.ellipse((x - r, y0 + y - r, x + r, y0 + y + r), fill=(22, 14, 34, 255))
    for x, y in ((210, 66), (232, 68), (244, 70), (222, 71)):
        d.point(p(x, y), fill=(176, 144, 224, 255))
    for x, y in ((237, 62), (216, 66)):                    # two tiny question marks in the dark
        d.line((x, y0 + y, x + 2, y0 + y, x + 2, y0 + y + 2, x + 1, y0 + y + 3), fill=(150, 120, 200, 255))
        d.point((x + 1, y0 + y + 5), fill=(150, 120, 200, 255))
    img = k.img.crop((0, BAND, KAART_W, BAND + KAART_H))
    # the band's own edge (the crop cut the parchment's top and bottom edge off)
    ImageDraw.Draw(img).rectangle((2, 2, KAART_W - 3, KAART_H - 3), outline=(122, 90, 52, 255))
    px = img.load()
    for x in range(KAART_W):
        for y in list(range(0, 6)) + list(range(KAART_H - 6, KAART_H)):
            rand = min(y, KAART_H - 1 - y)
            schaduw = max(0, 6 - rand) * 4
            r, g, b, a = px[x, y]
            px[x, y] = (max(0, r - schaduw), max(0, g - schaduw), max(0, b - schaduw * 5 // 4), a)
    return img


def _guh(d, cx, basis, r, kleur, groei=0):
    """The black shape of a guh as the game draws them, seen from the front: a blocky body that is wider than high, two small
    square ears on its top corners and two little feet, standing on y = basis. r = half its width; groei grows it on all sides."""
    hoog, oor, g = int(r * 1.25), max(2, r // 3), groei
    d.rectangle((cx - r - g, basis - hoog - g, cx + r + g, basis), fill=kleur)
    d.rectangle((cx - r + 2 - g, basis - hoog - 2 - g, cx + r - 2 + g, basis - hoog), fill=kleur)       # the rounded top
    for side in (-1, 1):
        ex = cx + side * (r - 1)
        d.rectangle((ex - oor - g, basis - hoog - oor - g, ex + oor + g, basis - hoog + oor), fill=kleur)   # an ear
        fx = cx + side * (r - 3)
        d.rectangle((fx - 2 - g, basis - 1, fx + 2 + g, basis + 2 + g), fill=kleur)                         # a foot


def echt():
    img = Image.new("RGBA", (ECHT_W, ECHT_H), (0, 0, 0, 255))
    px = img.load()
    rng = random.Random(23)
    for y in range(ECHT_H):
        for x in range(ECHT_W):
            # a dark sky that glows a little in the middle, as if something is there
            gloed = max(0.0, 1.0 - math.hypot((x - ECHT_W / 2) / 120.0, (y - ECHT_H * 0.9) / 44.0))
            ruis = rng.randint(-2, 2)
            px[x, y] = (int(18 + 46 * gloed) + ruis, int(12 + 26 * gloed) + ruis, int(34 + 66 * gloed) + ruis, 255)
    d = ImageDraw.Draw(img)
    for _ in range(34):                                      # a few stars
        x, y = rng.randint(3, ECHT_W - 4), rng.randint(3, 26)
        d.point((x, y), fill=rng.choice(((200, 180, 240, 255), (150, 130, 200, 255), (250, 240, 200, 255))))
    # the shapes: first their rim of light, then black on top
    vormen = [(38, 14), (80, 9), (128, 18), (178, 10), (218, 13)]
    for kleur, groei in (((120, 96, 176, 255), 1), ((8, 5, 14, 255), 0)):
        for cx, r in vormen:
            _guh(d, cx, ECHT_H - 6, r, kleur, groei)
    d.rectangle((0, ECHT_H - 4, ECHT_W, ECHT_H), fill=(8, 5, 14, 255))
    font = verhaal_motor.ImageFont.load_default()
    for cx, r in vormen:                                     # a question mark on every shape
        d.text((cx - 2, ECHT_H - 6 - int(r * 0.62) - 5), "?", fill=(176, 144, 224, 255), font=font)
    for x, y in ((14, 8), (104, 6), (152, 20), (240, 26)):   # and a few in the sky
        d.text((x, y), "?", fill=(96, 76, 140, 255), font=font)
    return img


def build(h):
    h.save(padkaart(), "gui", "guhpad", "padkaart.png")
    h.save(echt(), "gui", "guhpad", "echt.png")
