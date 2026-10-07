"""
einde2 (bouw_water): the block textures of the Noordelijke Waterstam (16 x 16, drawn here pixel by pixel).

  einde2_bouw_water_ijssteen         bricks sawn out of the glacier: pale blue, frosted joints, a glint here and there
  einde2_bouw_water_ijslantaarn      a block of clear ice with a light frozen into it
  einde2_bouw_water_stamteken        the emblem of the Waterstam: a crescent moon over three waves, in a ring with guh ears
  einde2_bouw_water_vuurnatie_teken  the emblem of the Vuurnatie: a black flame with the angry eyes of a Mika
"""
import random

from PIL import Image

import features.einde2_kit as kit

BLOCKS = ["einde2_bouw_water_ijssteen", "einde2_bouw_water_ijslantaarn", "einde2_bouw_water_stamteken", "einde2_bouw_water_vuurnatie_teken"]


def _klem(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c) + (255,)


def ijssteen():
    r = random.Random(kit.seed("bouw_water/ijssteen"))
    img = Image.new("RGBA", (16, 16))
    basis, voeg, diep = (196, 226, 243), (232, 245, 252), (160, 200, 230)
    for y in range(16):
        rij = y // 4
        for x in range(16):
            xs = (x + (4 if rij % 2 else 0)) % 8                  # bricks of 8 x 4, every other row shifted
            c = list(basis)
            tint = ((rij * 3 + (x + (4 if rij % 2 else 0)) // 8) * 37) % 5 - 2      # each brick its own shade
            c = [v + tint * 3 for v in c]
            if y % 4 == 3 or xs == 7:
                c = list(voeg)                                    # the frosted joints
            elif y % 4 == 0 or xs == 0:
                c = [v + 8 for v in c]                            # light on the upper and left edge of a brick
            elif y % 4 == 2 and xs == 6:
                c = list(diep)
            v = r.randint(-4, 4)
            img.putpixel((x, y), _klem([ch + v for ch in c]))
    for x, y in ((2, 1), (11, 5), (5, 9), (13, 13), (9, 2)):      # glints
        img.putpixel((x, y), (250, 253, 255, 255))
    for x, y in ((4, 6), (12, 10), (1, 14)):                      # a hairline of deeper blue in a brick
        img.putpixel((x, y), _klem(diep))
        img.putpixel((x + 1, y), _klem(diep))
    return img


def ijslantaarn():
    r = random.Random(kit.seed("bouw_water/ijslantaarn"))
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            ronde = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 6.6:
                c = (150, 196, 232)                               # the frame of ice
            elif d > 5.6:
                c = (206, 232, 247)
            else:
                t = min(1.0, ronde / 6.5)                         # the light in the middle: warm white, cooling to ice blue
                c = (255 - 70 * t, 252 - 30 * t, 214 + 36 * t)
            v = r.randint(-3, 3)
            img.putpixel((x, y), _klem([ch + v for ch in c]))
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        img.putpixel((x, y), (120, 172, 216, 255))
    for i in range(3):                                            # two glints on the ice, as on glass
        img.putpixel((3 + i, 5 - i), (255, 255, 255, 255))
    img.putpixel((11, 11), (255, 255, 255, 255))
    img.putpixel((12, 10), (255, 255, 255, 255))
    return img


STAMTEKEN = ["bbbbbbbbbbbbbbbb",
             "bbWWbbbbbbbbWWbb",
             "bbWWWbbbbbbWWWbb",
             "bbbWWWWWWWWWWbbb",
             "bbWWddddddddWWbb",
             "bWWdddWWWdddddWb",
             "bWdddWWddddddddW",
             "bWdddWWddddddddW",
             "bWddddWWWWdddddW",
             "bWdddddddddddddW",
             "bWdWWddWWddWWddW",
             "bWddWWddWWddWWdW",
             "bWWddddddddddWWb",
             "bbWWdWWddWWdWWbb",
             "bbbWWWWWWWWWWbbb",
             "bbbbbbbbbbbbbbbb"]

VUURTEKEN = ["rrrrrrrrrrrrrrrr",
             "rrrrrrrkrrrrrrrr",
             "rrrrrrkkrrrrrrrr",
             "rrrkrrkkkrrrkrrr",
             "rrrkkrkkkkrkkrrr",
             "rrrkkkkkkkkkkrrr",
             "rrkkkkkkkkkkkkrr",
             "rrkkokkkkkkokkrr",
             "rrkkkookkookkkrr",
             "rrkkkkkkkkkkkkrr",
             "rrkkkkkkkkkkkkrr",
             "rrrkkkgkkgkkkrrr",
             "rrrkkkkkkkkkkrrr",
             "rrrrkkkkkkkkrrrr",
             "rrrrrrkkkkrrrrrr",
             "rrrrrrrrrrrrrrrr"]


def _teken(rijen, kleuren, naam, ruis=4):
    r = random.Random(kit.seed("bouw_water/" + naam))
    img = Image.new("RGBA", (16, 16))
    for y, rij in enumerate(rijen):
        assert len(rij) == 16, (naam, y, rij)
        for x, ch in enumerate(rij):
            v = r.randint(-ruis, ruis)
            img.putpixel((x, y), _klem([c + v for c in kleuren[ch]]))
    return img


def textures(h):
    h.save(ijssteen(), "block", "einde2_bouw_water_ijssteen.png")
    h.save(ijslantaarn(), "block", "einde2_bouw_water_ijslantaarn.png")
    h.save(_teken(STAMTEKEN, {"b": (36, 66, 150), "d": (52, 96, 186), "W": (238, 246, 252)}, "stamteken"), "block", "einde2_bouw_water_stamteken.png")
    h.save(_teken(VUURTEKEN, {"r": (150, 28, 24), "k": (28, 20, 24), "o": (255, 150, 40), "g": (240, 240, 232)}, "vuurnatie_teken"), "block",
           "einde2_bouw_water_vuurnatie_teken.png")
