"""
einde2, slice bouw_lucht: the textures of the temple's own blocks (16 x 16, drawn here pixel by pixel; fixed seeds).

  werkplaatsdeur                 dark planks with two iron bands and rivets (the Werktuigguh's closed door)
  einde2_bouw_lucht_tegel        a cream temple tile with the blue wind curl of the luchtnomaden
  einde2_bouw_lucht_pijltegel    the same tile with the blue arrow (it points north in the texture: the block turns it)
  einde2_bouw_lucht_taart        top, side and bottom of one of Guhatso's taarten
  einde2_bouw_lucht_tandwiel     a brass gear on dark wood
  einde2_bouw_lucht_blauwdruk    the front: a blueprint of a balloon with two ears; the sides: a plank frame
"""
import math
import random

from PIL import Image

import features.einde2_kit as kit

CREME, VOEG, BLAUW, BLAUW_LICHT = (228, 221, 204), (196, 188, 170), (58, 142, 204), (128, 190, 232)


def _ruis(kleur, var, naam, size=16):
    rng = random.Random(kit.seed("bouw_lucht/" + naam))
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in kleur) + (255,))
    return img


def _tegelbasis(naam):
    img = _ruis(CREME, 5, naam)
    for i in range(16):
        for c in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(c, VOEG + (255,))
    for c in ((1, 1), (14, 1), (1, 14), (14, 14)):
        img.putpixel(c, (210, 203, 186, 255))
    return img


def tegel():
    """The wind curl: a spiral that opens to the right, with a little tail."""
    img = _tegelbasis("tegel")
    t = 0.0
    while t < 3.6 * math.pi:
        r = 0.8 + 0.42 * t
        x, y = 7.5 + r * math.cos(t), 7.5 + r * math.sin(t)
        if 2 <= x <= 13 and 2 <= y <= 13:
            img.putpixel((int(round(x)), int(round(y))), (BLAUW if t > 1.2 * math.pi else BLAUW_LICHT) + (255,))
        t += 0.05
    return img


def pijltegel():
    img = _tegelbasis("pijltegel")
    for y in range(6, 13):                               # the shaft
        for x in (7, 8):
            img.putpixel((x, y), BLAUW + (255,))
    for i, y in enumerate(range(3, 7)):                  # the head (points up = north)
        for x in range(7 - i, 9 + i):
            img.putpixel((x, y), BLAUW + (255,))
    for c in ((7, 3), (8, 3), (7, 12), (8, 12)):
        img.putpixel(c, BLAUW_LICHT + (255,))
    return img


def taart():
    """-> top, side, bottom. The top: a ring of crust around a pink-purple filling with a lighter swirl and a dot of cream."""
    korst, korst_donker, vulling, krul, room = (214, 168, 98), (176, 128, 66), (196, 84, 150), (236, 150, 196), (250, 240, 220)
    top = _ruis(korst, 6, "taart_top")
    rng = random.Random(kit.seed("bouw_lucht/taart_vulling"))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 5.6:
                v = rng.randint(-8, 8)
                top.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in vulling) + (255,))
            elif d > 7.9:
                top.putpixel((x, y), korst_donker + (255,))
    t = 0.6
    while t < 2.6 * math.pi:
        r = 0.6 + 0.55 * t
        x, y = 7.5 + r * math.cos(t), 7.5 + r * math.sin(t)
        if math.hypot(x - 7.5, y - 7.5) <= 5.0:
            top.putpixel((int(round(x)), int(round(y))), krul + (255,))
        t += 0.06
    for c in ((7, 7), (8, 7), (7, 8), (8, 8)):
        top.putpixel(c, room + (255,))
    zij = _ruis(korst, 6, "taart_zij")
    for x in range(16):
        zij.putpixel((x, 15), korst_donker + (255,))
        zij.putpixel((x, 0), (232, 190, 120, 255))
        if x % 3 == 1:
            for y in range(3, 14):
                zij.putpixel((x, y), tuple(c - 14 for c in korst) + (255,))
    onder = _ruis(korst_donker, 5, "taart_onder")
    return top, zij, onder


def tandwiel():
    hout, koper, koper_licht, gat = (74, 58, 46), (204, 160, 68), (238, 204, 110), (48, 38, 30)
    img = _ruis(hout, 5, "tandwiel")
    for i in range(16):
        for c in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(c, (52, 42, 34, 255))
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            tand = (math.degrees(math.atan2(dy, dx)) + 360 + 11.25) % 45 < 22.5
            if d <= 4.6 or (d <= 6.4 and tand):
                img.putpixel((x, y), (koper_licht if dx + dy < -3 else koper) + (255,))
            if d <= 1.6:
                img.putpixel((x, y), gat + (255,))
            elif 2.6 < d <= 3.4 and (x + y) % 2 == 0:
                img.putpixel((x, y), (168, 126, 50, 255))
    return img


def blauwdruk():
    """-> front, side. The front: white lines on blue: a balloon with two ears, its basket, a few measuring lines."""
    blauw, lijn, vaag, hout = (38, 84, 168), (236, 242, 250), (70, 116, 196), (112, 84, 52)
    voor = _ruis(blauw, 4, "blauwdruk")
    for i in range(16):
        for c in ((i, 0), (i, 15), (0, i), (15, i)):
            voor.putpixel(c, hout + (255,))
    for i in range(2, 14, 3):                            # the grid of the paper
        for j in range(1, 15):
            voor.putpixel((i, j), vaag + (255,))
            voor.putpixel((j, i), vaag + (255,))
    for y in range(1, 15):
        for x in range(1, 15):
            d = math.hypot(x - 7.5, y - 5.5)
            if 3.3 < d <= 4.3:                           # the envelope
                voor.putpixel((x, y), lijn + (255,))
    for c in ((4, 1), (5, 1), (10, 1), (11, 1), (4, 2), (11, 2),            # the two ears
              (6, 10), (9, 10), (6, 11), (9, 11),                          # the ropes
              (6, 12), (7, 12), (8, 12), (9, 12), (6, 13), (9, 13), (6, 14), (7, 14), (8, 14), (9, 14),   # the basket
              (6, 5), (9, 5), (7, 7), (8, 7),                              # eyes and nose
              (12, 9), (13, 9), (13, 10), (13, 11), (12, 11)):             # a measuring mark
        voor.putpixel(c, lijn + (255,))
    zij = _ruis(hout, 6, "blauwdruk_zij")
    for y in (0, 5, 10, 15):
        for x in range(16):
            zij.putpixel((x, y), (84, 62, 38, 255))
    for x, y in ((3, 2), (11, 7), (6, 12)):
        zij.putpixel((x, y), (92, 68, 42, 255))
        zij.putpixel((x + 1, y), (92, 68, 42, 255))
    return voor, zij


def werkplaatsdeur():
    plank, naad, ijzer, ijzer_licht, klinknagel = (98, 68, 42), (70, 48, 30), (112, 114, 120), (150, 152, 158), (64, 66, 72)
    img = _ruis(plank, 6, "werkplaatsdeur")
    for x in (0, 5, 10, 15):
        for y in range(16):
            img.putpixel((x, y), naad + (255,))
    for y0 in (2, 11):
        for x in range(16):
            img.putpixel((x, y0), ijzer_licht + (255,))
            img.putpixel((x, y0 + 1), ijzer + (255,))
            img.putpixel((x, y0 + 2), (88, 90, 96, 255))
        for x in (2, 7, 12):
            img.putpixel((x, y0 + 1), klinknagel + (255,))
    return img


def build(h):
    h.save(werkplaatsdeur(), "block", "werkplaatsdeur.png")
    h.save(tegel(), "block", "einde2_bouw_lucht_tegel.png")
    h.save(pijltegel(), "block", "einde2_bouw_lucht_pijltegel.png")
    top, zij, onder = taart()
    h.save(top, "block", "einde2_bouw_lucht_taart_top.png")
    h.save(zij, "block", "einde2_bouw_lucht_taart_zij.png")
    h.save(onder, "block", "einde2_bouw_lucht_taart_onder.png")
    h.save(tandwiel(), "block", "einde2_bouw_lucht_tandwiel.png")
    voor, zij = blauwdruk()
    h.save(voor, "block", "einde2_bouw_lucht_blauwdruk.png")
    h.save(zij, "block", "einde2_bouw_lucht_blauwdruk_zij.png")


def preview(out):
    """A contact sheet of the textures, 8 times enlarged (python -c "... preview('x.png')" from the project root)."""
    alle = [werkplaatsdeur(), tegel(), pijltegel(), *taart(), tandwiel(), *blauwdruk()]
    sheet = Image.new("RGBA", (len(alle) * 136 + 8, 144), (40, 40, 48, 255))
    for i, img in enumerate(alle):
        sheet.paste(img.resize((128, 128), Image.NEAREST), (8 + i * 136, 8))
    sheet.save(out)
