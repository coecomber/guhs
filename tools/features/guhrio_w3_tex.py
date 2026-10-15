"""
bbq2 (guhrio-w3) - the 16 x 16 textures of the blocks world 3 adds to Super Guhrio, drawn pixel by pixel in the look of the
engine's own pieces (features/guhrio_tex.py): the grate of the duel's bridge, the lever (up and pulled), Guhshi's hitching
post, the Vuurpeper bush, and the little icon of the boss's spot.
"""
import numpy as np
from PIL import Image


def _leeg(n=16):
    return np.zeros((n, n, 4), np.uint8)


def _px(a, x, y, kleur, alpha=255):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
        a[y, x, :3] = kleur[:3]
        a[y, x, 3] = alpha


def _patroon(a, x0, y0, rijen, kleuren):
    for r, rij in enumerate(rijen):
        for c, ch in enumerate(rij):
            if ch in kleuren:
                _px(a, x0 + c, y0 + r, kleuren[ch])


def brug():
    """The grate of the bridge: iron bars, and the sauce glowing up through the holes."""
    rng = np.random.default_rng(21302650)
    a = _leeg()
    for y in range(16):
        for x in range(16):
            bar = x % 4 in (0, 1) or y % 4 in (0, 1)
            if bar:
                licht = (x % 4 == 0 and y % 4 not in (0, 1)) or (y % 4 == 0)
                k = (126, 128, 142) if licht else (84, 84, 98)
            else:
                k = (228, 110, 30) if (x // 4 + y // 4) % 2 == 0 else (190, 74, 22)
            n = int(rng.normal(0, 4))
            _px(a, x, y, tuple(int(np.clip(v + n, 0, 255)) for v in k))
    a[0, :, :3] = (156, 158, 172)
    a[15, :, :3] = (46, 44, 56)
    a[:, 0, :3] = np.maximum(a[:, 0, :3], 100)
    a[:, 15, :3] = (46, 44, 56)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):               # rivets
        _px(a, x, y, (214, 214, 226))
    return Image.fromarray(a)


def hendel(getrokken=False):
    """The lever: a golden foot, an iron handle with a fat red knob. Up it leans back (to the left), pulled it leans on."""
    a = _leeg()
    k = {"g": (236, 190, 60), "G": (170, 120, 30), "i": (150, 150, 164), "I": (84, 84, 98), "r": (226, 52, 48), "R": (150, 24, 30), "w": (255, 190, 180)}
    _patroon(a, 0, 0, ["................",
                       "..rrr...........",
                       ".rwrrr..........",
                       ".rrrrR..........",
                       "..rRRi..........",
                       "....iiI.........",
                       ".....iiI........",
                       "......iiI.......",
                       "......iiI.......",
                       ".......iiI......",
                       ".......iiI......",
                       "......gggg......",
                       ".....gggggG.....",
                       "...ggggggggGG...",
                       "..ggggggggggGG..",
                       "..GGGGGGGGGGGG.."], k)
    if getrokken:
        a = a[:, ::-1].copy()
    return Image.fromarray(a)


def parkeerpaal():
    """Guhshi's hitching post: a wooden post with an iron ring and a round green sign with his head on it."""
    a = _leeg()
    k = {"h": (150, 100, 56), "H": (104, 66, 34), "l": (196, 146, 90), "i": (170, 170, 184), "g": (108, 194, 74), "G": (58, 130, 46),
         "w": (250, 250, 244), "z": (30, 30, 36), "r": (226, 52, 48), "o": (240, 140, 40)}
    _patroon(a, 0, 0, ["....rr..........",
                       "...gggg.........",
                       "..gggggg........",
                       ".ggzgggwwG......",
                       ".ggggggwwwG.....",
                       ".GgggggwwwG.....",
                       "..GggggGGG......",
                       "...GGGGlh.......",
                       ".......lhH......",
                       ".......lhH.iii..",
                       ".......lhHi...i.",
                       ".......lhhi...i.",
                       ".......lhH.iii..",
                       ".......lhH......",
                       "......llhHH.....",
                       ".....hhhhhHH...."], k)
    return Image.fromarray(a)


def peperstruik():
    """The Vuurpeper bush: a round bush full of red peppers (a cross model: the same from every side)."""
    a = _leeg()
    k = {"g": (70, 150, 60), "G": (36, 100, 44), "l": (130, 200, 96), "r": (232, 50, 40), "R": (160, 24, 26), "y": (255, 214, 90), "s": (96, 66, 40)}
    _patroon(a, 0, 0, ["................",
                       "................",
                       "......lgg.......",
                       "....lgggggg.....",
                       "...lggygggggG...",
                       "..lggrrggggygG..",
                       "..gggrRgglgrrG..",
                       ".lggggRgggrRgG..",
                       ".gggyggggggRgGG.",
                       ".ggrrgglggggggG.",
                       ".ggrRggggyggggG.",
                       "..ggRgggrrgggG..",
                       "..GggggggrRgGG..",
                       "...GGggggGRGG...",
                       ".....GGsGGG.....",
                       ".......ss......."], k)
    return Image.fromarray(a)


def baas_icoon():
    """What you hold when you place the boss's spot: his face (horns, red brows, glowing eyes)."""
    a = _leeg()
    k = {"p": (176, 100, 112), "P": (128, 66, 80), "l": (208, 136, 146), "b": (240, 234, 208), "r": (226, 58, 40), "o": (255, 156, 40), "z": (30, 18, 26),
         "w": (255, 255, 255), "n": (112, 44, 60)}
    _patroon(a, 0, 0, ["................",
                       ".b.....rr.....b.",
                       ".bb...rrrr...bb.",
                       "..bb.pprrpp.bb..",
                       "..bpppppppppb...",
                       "...pppppppppp...",
                       "..prrrpppprrrp..",
                       "..ppworppwropp..",
                       "..ppwwpppwwppp..",
                       "..ppplllllpppP..",
                       "..pplllnnlllpP..",
                       "..ppzzzzzzzzpP..",
                       "...pzwzzzzwzP...",
                       "...PpzzzzzzPP...",
                       "....PPPPPPPP....",
                       "................"], k)
    return Image.fromarray(a)
