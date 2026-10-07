"""
Guhbioscoop: everything that is drawn. Two halves:

  1. the textures of the blocks and the item (projector, doek, stoeltje, popcornmachine, bakje popcorn): textures(h)
  2. the sprite library of the films: little pixel pictures (PIL images, no half-transparent pixels) by name. A film takes
     the ones it uses and the film generator packs them into that film's own sprite sheet
     (assets/guhs/textures/guhbioscoop/<film>.png). guh(...) draws any guh: colour, eyes, ears and what it wears.

Everything is drawn by code with fixed numbers (no random without a seed), so a rerun gives the same bytes.
"""
import numpy as np
from PIL import Image, ImageDraw

T = (0, 0, 0, 0)


def nieuw(w, h, kleur=T):
    return Image.new("RGBA", (w, h), kleur)


def rgb(c):
    if isinstance(c, str):
        c = c.lstrip("#")
        return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255)
    return tuple(c) + (255,) if len(c) == 3 else tuple(c)


def tint(c, f):
    """f < 1 darker, f > 1 lighter."""
    c = rgb(c)
    if f <= 1:
        return (int(c[0] * f), int(c[1] * f), int(c[2] * f), 255)
    g = f - 1
    return (int(c[0] + (255 - c[0]) * g), int(c[1] + (255 - c[1]) * g), int(c[2] + (255 - c[2]) * g), 255)


def raster(rows, palet):
    """An ASCII picture: one character per pixel, '.' = nothing."""
    w = max(len(r) for r in rows)
    im = nieuw(w, len(rows))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and ch != " ":
                im.putpixel((x, y), rgb(palet[ch]))
    return im


def strip(frames):
    """Frames of one size side by side (an animated sprite)."""
    w, h = frames[0].size
    im = nieuw(w * len(frames), h)
    for i, f in enumerate(frames):
        im.paste(f, (i * w, 0))
    return im


def hard(im):
    """No half-transparent pixels: the film is drawn with a cutout render type."""
    a = np.array(im)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    a[a[..., 3] == 0] = 0
    return Image.fromarray(a)


def ruis(basis, var, seed, w=16, h=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((h, w, 4), np.uint8)
    n = rng.normal(0, var, (h, w))
    b = rgb(basis)
    for c in range(3):
        a[..., c] = np.clip(b[c] + n, 0, 255)
    a[..., 3] = 255
    return a


# =====================================================================================================================
# 1. block and item textures
# =====================================================================================================================
ROOD, ROOD_D, ROOD_L = (176, 36, 52), (124, 22, 38), (214, 70, 84)
METAAL, METAAL_D, METAAL_L = (72, 68, 84), (44, 40, 54), (112, 108, 128)
ROZE = (247, 150, 200)
GOUD = (255, 210, 110)


def _doek(seed, basis):
    a = ruis(basis, 2.5, seed)
    for y in range(16):          # a fine weave
        for x in range(16):
            if (x + y) % 4 == 0:
                a[y, x, :3] = np.clip(a[y, x, :3].astype(int) - 5, 0, 255)
    return Image.fromarray(a)


def _projector_kast():
    a = ruis(METAAL, 4, 7701)
    a[0, :, :3] = METAAL_L
    a[15, :, :3] = METAAL_D
    a[:, 0, :3] = METAAL_L
    a[:, 15, :3] = METAAL_D
    a[7:9, 1:15, :3] = ROZE            # a pink trim line
    for x in range(3, 13, 2):          # cooling slits
        a[11:14, x, :3] = METAAL_D
    a[3, 12, :3] = (110, 220, 130)     # the power light
    a[3, 3:6, :3] = GOUD               # a little name plate
    return Image.fromarray(a)


def _projector_lens(aan):
    a = ruis(METAAL_D, 3, 7702)
    glas = (255, 244, 190) if aan else (38, 44, 70)
    rand = (255, 214, 120) if aan else (96, 100, 130)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5.2:
                a[y, x, :3] = glas
            elif d < 7.2:
                a[y, x, :3] = rand
    if not aan:
        a[5, 5, :3] = (150, 170, 220)
        a[6, 5, :3] = (110, 130, 180)
    else:
        a[5:7, 5:7, :3] = (255, 255, 255)
    return Image.fromarray(a)


def _projector_spoel():
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 7.8:
                a[y, x] = (*METAAL_D, 255)
            if d < 6.6:
                a[y, x] = (30, 24, 34, 255)        # the film on the reel
            if d < 3.0:
                a[y, x] = (*METAAL_L, 255)
            if d < 1.2:
                a[y, x] = (*ROZE, 255)
    for i in range(16):                             # three spokes
        if 2 < i < 13:
            a[7, i] = (*METAAL_L, 255)
            a[i, 7] = (*METAAL_L, 255)
    return Image.fromarray(a)


def _stof():
    a = ruis(ROOD, 5, 7711)
    for y in range(0, 16, 4):          # buttoned plush
        for x in range(2 if y % 8 else 0, 16, 4):
            a[y, x, :3] = ROOD_D
            a[min(15, y + 1), x, :3] = ROOD_L
    a[0, :, :3] = ROOD_L
    a[15, :, :3] = ROOD_D
    return Image.fromarray(a)


def _frame():
    a = ruis((58, 40, 52), 3, 7712)
    a[0, :, :3] = (96, 70, 88)
    a[15, :, :3] = (36, 24, 34)
    a[7:9, :, :3] = GOUD               # a golden rim
    return Image.fromarray(a)


def _popcorn_glas():
    """The glass box: popcorn piled up inside, a little kettle hanging from the top."""
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = (196, 228, 244)
    a[..., 3] = 255
    a[:, 0, :3] = (150, 190, 214)
    a[:, 15, :3] = (150, 190, 214)
    rng = np.random.default_rng(7721)
    for x in range(1, 15):             # the pile
        top = 9 + int(2 * abs(np.sin(x * 0.9))) - (1 if 4 < x < 11 else 0)
        for y in range(top, 16):
            a[y, x, :3] = (255, 236, 150) if rng.random() < 0.7 else (255, 250, 220)
            if rng.random() < 0.12:
                a[y, x, :3] = (232, 190, 90)
    a[1:3, 7:9, :3] = (90, 90, 104)    # the kettle on its arm
    a[3:6, 5:11, :3] = (150, 150, 168)
    a[6, 6:10, :3] = (96, 96, 110)
    a[2, 2, :3] = (255, 255, 255)      # a glint
    a[3, 2, :3] = (255, 255, 255)
    a[2, 3, :3] = (255, 255, 255)
    for (x, y) in ((3, 7), (12, 6), (11, 8)):   # kernels in the air
        a[y, x, :3] = (255, 250, 220)
    return Image.fromarray(a)


def _popcorn_kar(voor):
    a = ruis(ROOD, 4, 7722)
    for x in range(0, 16, 4):          # red and white stripes
        a[:, x:x + 2, :3] = (250, 240, 236)
    a[0, :, :3] = GOUD
    a[15, :, :3] = ROOD_D
    if voor:                           # a little sign: POP
        a[5:12, 2:14, :3] = (60, 30, 40)
        letters = ["ppp.ooo.ppp", "p.p.o.o.p.p", "ppp.o.o.ppp", "p...o.o.p..", "p...ooo.p.."]
        for y, rij in enumerate(letters):
            for x, ch in enumerate(rij):
                if ch != ".":
                    a[6 + y, 2 + x + 0, :3] = GOUD
    return Image.fromarray(a)


def _popcorn_dak():
    a = ruis(ROOD, 4, 7723)
    a[0, :, :3] = GOUD
    a[15, :, :3] = GOUD
    a[:, 0, :3] = GOUD
    a[:, 15, :3] = GOUD
    return Image.fromarray(a)


POPCORN_ICOON = [
    "................",
    ".....ww.yw......",
    "...wwywwwwyw....",
    "..wywwwywwwww...",
    "..wwwywwwywyw...",
    "...wwwwwywww....",
    "..rRwwRrrRwwRr..",
    "..rRWWRrrRWWRr..",
    "..rRWWRrrRWWRr..",
    "...RWWRrrRWWR...",
    "...RWWRrrRWWR...",
    "...RWWRrrRWWR...",
    "....WWRrrRWW....",
    "....WWRrrRWW....",
    "....dddddddd....",
    "................",
]
POPCORN_PALET = {"w": (255, 250, 222), "y": (255, 226, 130), "r": ROOD_L, "R": ROOD, "W": (250, 240, 236), "d": ROOD_D}


def textures(h):
    h.save(_doek(7731, (240, 238, 232)), "block", "guhbioscoop_doek.png")
    h.save(_doek(7732, (150, 146, 150)), "block", "guhbioscoop_doek_achter.png")
    h.save(_projector_kast(), "block", "guhbioscoop_projector.png")
    h.save(_projector_lens(False), "block", "guhbioscoop_projector_lens.png")
    h.save(_projector_lens(True), "block", "guhbioscoop_projector_lens_aan.png")
    h.save(_projector_spoel(), "block", "guhbioscoop_projector_spoel.png")
    h.save(_stof(), "block", "guhbioscoop_stoeltje.png")
    h.save(_frame(), "block", "guhbioscoop_stoeltje_frame.png")
    h.save(_popcorn_glas(), "block", "guhbioscoop_popcornmachine_glas.png")
    h.save(_popcorn_kar(False), "block", "guhbioscoop_popcornmachine_kar.png")
    h.save(_popcorn_kar(True), "block", "guhbioscoop_popcornmachine_voor.png")
    h.save(_popcorn_dak(), "block", "guhbioscoop_popcornmachine_dak.png")
    h.save(raster(POPCORN_ICOON, POPCORN_PALET), "item", "guhbioscoop_popcorn.png")


# =====================================================================================================================
# 2. the sprite library of the films
# =====================================================================================================================
# every guh sprite has the same size: the body stands at x 3..18, y 7..17; above it there is room for ears and hats
GUH_B, GUH_H = 22, 18
LILA = (196, 162, 206)
KLEUREN = {
    "lila": LILA, "mint": (150, 220, 190), "choco": (150, 104, 78), "sneeuw": (240, 240, 248), "goud": (250, 208, 96),
    "mika": (222, 164, 186), "balto": (150, 154, 166), "guhtwo": (226, 216, 236), "626": (76, 122, 216), "mieuw": (255, 176, 206),
    "rood": (214, 60, 60), "blauw": (70, 110, 220), "groen": (80, 180, 90), "geel": (240, 210, 70), "roze": (245, 140, 200),
    "oranje": (240, 150, 60), "paars": (150, 90, 200), "wit": (236, 236, 240), "steele": (124, 124, 134), "lilo": (232, 200, 196),
}
OOG_DONKER = (28, 20, 36)


def guh(kleur="lila", ogen="open", oren="rond", draagt=(), iris=(84, 176, 232), binnen=None):
    """A guh, seen from the front. ogen: open, dicht (asleep), blij, schrik, half (sleepy), boos, mika. oren: rond, punt,
    flap, geen. draagt: headset, kussen, muts:<kleur>, rugzak, pet:<kleur>, strik, microfoon, strohoed, overall,
    pak (a space suit in the body colour), kapitein, zuster, labjas, bril, haar, jurk, staart:<kleur>, tand, wangen, snor."""
    k = KLEUREN.get(kleur, kleur)
    im = nieuw(GUH_B, GUH_H)
    d = ImageDraw.Draw(im)
    lijf, licht, schaduw = rgb(k), tint(k, 1.25), tint(k, 0.8)
    inn = rgb(binnen) if binnen else tint(k, 0.72)
    pak = "pak" in draagt
    if pak:
        oren = "geen"
    # --- ears ---
    if oren == "rond":
        for x0 in (2, 14):
            d.ellipse((x0, 4, x0 + 5, 9), fill=lijf)
            d.ellipse((x0 + 1, 5, x0 + 4, 8), fill=inn)
    elif oren == "punt":
        d.polygon([(3, 9), (5, 3), (8, 9)], fill=lijf)
        d.polygon([(13, 9), (16, 3), (18, 9)], fill=lijf)
        d.line([(5, 6), (5, 8)], fill=inn)
        d.line([(16, 6), (16, 8)], fill=inn)
    elif oren == "flap":
        d.polygon([(4, 8), (0, 6), (0, 10), (3, 12)], fill=lijf)
        d.polygon([(17, 8), (21, 6), (21, 10), (18, 12)], fill=lijf)
        d.line([(1, 8), (3, 9)], fill=inn)
        d.line([(20, 8), (18, 9)], fill=inn)
    # --- body ---
    d.rounded_rectangle((3, 7, 18, 17), radius=2, fill=lijf)
    d.line([(5, 7), (16, 7)], fill=licht)
    d.line([(5, 17), (16, 17)], fill=schaduw)
    for x in (5, 6, 15, 16):                     # feet
        im.putpixel((x, 17), tint(k, 0.62))
    if pak:
        d.rounded_rectangle((2, 5, 19, 17), radius=3, fill=lijf)
        d.line([(5, 5), (16, 5)], fill=licht)
        d.line([(5, 17), (16, 17)], fill=schaduw)
        d.rectangle((20, 10, 21, 15), fill=schaduw)                      # the air tank
        d.rounded_rectangle((5, 9, 16, 14), radius=1, fill=(196, 228, 246, 255))    # the visor
        d.line([(6, 14), (15, 14)], fill=(140, 180, 210, 255))
        im.putpixel((6, 10), (255, 255, 255, 255))
    # --- eyes ---
    oy = 11
    for ox in (6, 13):
        if ogen in ("open", "boos", "half"):
            d.rectangle((ox, oy, ox + 2, oy + 2), fill=OOG_DONKER)
            im.putpixel((ox, oy), (255, 255, 255, 255))
            im.putpixel((ox + 1, oy + 2), rgb(iris))
            im.putpixel((ox + 2, oy + 2), rgb(iris))
            if ogen == "half":
                d.line([(ox, oy), (ox + 2, oy)], fill=schaduw if not pak else (140, 180, 210, 255))
        elif ogen == "mika":
            d.rectangle((ox, oy, ox + 2, oy + 2), fill=(214, 56, 40, 255))
            d.line([(ox + 1, oy), (ox + 1, oy + 2)], fill=OOG_DONKER)
        elif ogen == "dicht":
            im.putpixel((ox, oy + 1), OOG_DONKER + (255,))
            im.putpixel((ox + 1, oy + 2), OOG_DONKER + (255,))
            im.putpixel((ox + 2, oy + 1), OOG_DONKER + (255,))
        elif ogen == "blij":
            im.putpixel((ox, oy + 2), OOG_DONKER + (255,))
            im.putpixel((ox + 1, oy + 1), OOG_DONKER + (255,))
            im.putpixel((ox + 2, oy + 2), OOG_DONKER + (255,))
        elif ogen == "schrik":
            d.rectangle((ox, oy - 1, ox + 2, oy + 2), fill=(255, 255, 255, 255))
            im.putpixel((ox + 1, oy + 1), OOG_DONKER + (255,))
    if ogen in ("boos", "mika"):                 # angry brows
        d.line([(5, oy - 3), (8, oy - 1)], fill=OOG_DONKER)
        d.line([(16, oy - 3), (13, oy - 1)], fill=OOG_DONKER)
    # --- snout ---
    if not pak:
        neus = (232, 140, 176, 255) if kleur not in ("626",) else (30, 44, 110, 255)
        im.putpixel((10, 14), neus)
        im.putpixel((11, 14), neus)
        if ogen == "schrik":
            d.rectangle((10, 15, 11, 16), fill=OOG_DONKER)
        elif ogen in ("blij",):
            im.putpixel((9, 15), tint(k, 0.55))
            im.putpixel((10, 16), tint(k, 0.55))
            im.putpixel((11, 16), tint(k, 0.55))
            im.putpixel((12, 15), tint(k, 0.55))
        elif ogen == "mika":
            d.line([(8, 15), (13, 15)], fill=OOG_DONKER)
            im.putpixel((7, 14), OOG_DONKER + (255,))
            im.putpixel((14, 14), OOG_DONKER + (255,))
            im.putpixel((10, 16), (255, 255, 255, 255))
    # --- what it wears ---
    for stuk in draagt:
        naam, _, arg = stuk.partition(":")
        c = KLEUREN.get(arg, LILA) if arg else LILA
        if naam == "headset":
            d.arc((3, 2, 18, 12), 180, 360, fill=(40, 40, 52, 255))
            d.rectangle((2, 8, 3, 12), fill=(40, 40, 52, 255))
            d.rectangle((18, 8, 19, 12), fill=(40, 40, 52, 255))
            d.line([(3, 13), (6, 15)], fill=(40, 40, 52, 255))            # the microphone
            im.putpixel((7, 15), (110, 220, 130, 255))
        elif naam == "kussen":                                             # armour made of pillows
            d.rounded_rectangle((2, 15, 19, 17), radius=1, fill=(250, 244, 236, 255))
            d.line([(4, 15), (17, 15)], fill=(255, 255, 255, 255))
            for x in (6, 10, 11, 15):
                im.putpixel((x, 16), (247, 150, 200, 255))
            d.rounded_rectangle((6, 3, 15, 7), radius=2, fill=(250, 244, 236, 255))   # and one on its head
            d.line([(8, 7), (13, 7)], fill=(214, 200, 196, 255))
        elif naam == "muts":                                               # a nightcap
            d.polygon([(5, 7), (16, 7), (14, 3), (8, 4)], fill=rgb(c))
            d.polygon([(14, 3), (19, 2), (19, 5), (15, 5)], fill=rgb(c))
            d.rectangle((19, 5, 20, 6), fill=(255, 255, 255, 255))
            d.line([(5, 7), (16, 7)], fill=(255, 255, 255, 255))
        elif naam == "rugzak":                                             # the parachute pack: straps and a pack behind
            d.rectangle((0, 9, 2, 15), fill=(60, 110, 80, 255))
            d.rectangle((19, 9, 21, 15), fill=(60, 110, 80, 255))
            d.line([(3, 15), (18, 15)], fill=(40, 80, 58, 255))
        elif naam == "pet":
            d.rounded_rectangle((6, 4, 15, 7), radius=1, fill=rgb(c))
            d.rectangle((3, 7, 12, 7), fill=tint(c, 0.7))
            im.putpixel((10, 5), (255, 255, 255, 255))
        elif naam == "kapitein":
            d.rounded_rectangle((6, 3, 15, 7), radius=1, fill=(250, 250, 255, 255))
            d.rectangle((5, 7, 16, 7), fill=(30, 40, 90, 255))
            d.rectangle((10, 4, 11, 5), fill=GOUD + (255,))
        elif naam == "zuster":
            d.rounded_rectangle((7, 4, 14, 7), radius=1, fill=(255, 255, 255, 255))
            d.rectangle((10, 4, 11, 6), fill=(230, 60, 80, 255))
            d.rectangle((9, 5, 12, 5), fill=(230, 60, 80, 255))
        elif naam == "strik":
            d.polygon([(8, 5), (5, 3), (5, 8)], fill=(240, 80, 130, 255))
            d.polygon([(8, 5), (11, 3), (11, 8)], fill=(240, 80, 130, 255))
            im.putpixel((8, 5), (255, 180, 210, 255))
        elif naam == "microfoon":
            d.line([(17, 17), (15, 14)], fill=(60, 60, 70, 255))
            d.rectangle((14, 12, 15, 13), fill=(40, 40, 50, 255))
        elif naam == "strohoed":
            d.rectangle((2, 7, 19, 7), fill=(226, 190, 96, 255))
            d.rounded_rectangle((6, 3, 15, 7), radius=2, fill=(240, 208, 116, 255))
            d.line([(6, 6), (15, 6)], fill=(176, 60, 50, 255))
        elif naam == "overall":
            d.rectangle((4, 14, 17, 17), fill=(70, 110, 190, 255))
            d.line([(6, 9), (6, 14)], fill=(70, 110, 190, 255))
            d.line([(15, 9), (15, 14)], fill=(70, 110, 190, 255))
            im.putpixel((6, 14), GOUD + (255,))
            im.putpixel((15, 14), GOUD + (255,))
            im.putpixel((10, 14), (232, 140, 176, 255))
            im.putpixel((11, 14), (232, 140, 176, 255))
        elif naam == "labjas":
            d.rectangle((3, 14, 8, 17), fill=(250, 250, 255, 255))
            d.rectangle((13, 14, 18, 17), fill=(250, 250, 255, 255))
            d.line([(9, 15), (9, 17)], fill=(200, 204, 220, 255))
            d.line([(12, 15), (12, 17)], fill=(200, 204, 220, 255))
        elif naam == "bril":
            d.rectangle((5, 10, 9, 14), outline=(50, 40, 60, 255))
            d.rectangle((12, 10, 16, 14), outline=(50, 40, 60, 255))
            d.line([(9, 12), (12, 12)], fill=(50, 40, 60, 255))
        elif naam == "haar":
            d.rounded_rectangle((3, 4, 18, 9), radius=2, fill=(30, 26, 34, 255))
            d.rectangle((3, 9, 4, 13), fill=(30, 26, 34, 255))
            d.rectangle((17, 9, 18, 13), fill=(30, 26, 34, 255))
            d.rectangle((1, 9, 2, 10), fill=(240, 100, 150, 255))         # a flower
        elif naam == "jurk":
            d.rectangle((4, 15, 17, 17), fill=(206, 50, 50, 255))
            for x in (6, 10, 14):
                im.putpixel((x, 16), (255, 255, 255, 255))
        elif naam == "staart":
            d.line([(18, 15), (20, 13)], fill=rgb(c))
            d.rectangle((19, 10, 21, 12), fill=rgb(c))
        elif naam == "tand":
            im.putpixel((10, 16), (255, 255, 255, 255))
        elif naam == "wangen":
            for x in (5, 16):
                im.putpixel((x, 14), (255, 150, 170, 255))
        elif naam == "ziek":
            for x in (5, 16):
                im.putpixel((x, 14), (150, 210, 130, 255))
            d.line([(12, 15), (16, 16)], fill=(250, 250, 250, 255))       # a thermometer
            im.putpixel((16, 16), (230, 60, 60, 255))
    return im


def zzz():
    """Two frames: a little z and a bigger one drifting up."""
    a = raster(["....zzz", ".....z.", "....zzz", ".......", "zz.....", ".z.....", "zz....."], {"z": (255, 255, 255)})
    b = raster([".....zz", "......z", ".....zz", "zzz....", ".z.....", "zzz....", "......."], {"z": (255, 255, 255)})
    return strip([a, b])


def hart(kleur=(240, 80, 120)):
    return raster([".rr.rr.", "rrrrrrr", "rrrrrrr", ".rrrrr.", "..rrr..", "...r..."], {"r": kleur})


def ster(kleur=(255, 240, 150)):
    return raster(["..s..", ".sss.", "sssss", ".sss.", "..s.."], {"s": kleur})


def sterren(seed, w=112, h=64, n=46):
    """A night sky full of little stars."""
    rng = np.random.default_rng(seed)
    im = nieuw(w, h)
    for _ in range(n):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        im.putpixel((x, y), (255, 255, 255, 255) if rng.random() < 0.6 else (255, 236, 170, 255))
    for _ in range(5):
        x, y = int(rng.integers(2, w - 2)), int(rng.integers(2, h - 2))
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            im.putpixel((x + dx, y + dy), (255, 255, 255, 255))
    return im


def wolk(w=22, h=8, kleur=(255, 255, 255)):
    im = nieuw(w, h)
    d = ImageDraw.Draw(im)
    d.ellipse((0, h // 2 - 1, w // 2, h - 1), fill=rgb(kleur))
    d.ellipse((w // 4, 0, w * 3 // 4, h - 1), fill=rgb(kleur))
    d.ellipse((w // 2, h // 2 - 2, w - 1, h - 1), fill=rgb(kleur))
    d.rectangle((3, h - 2, w - 4, h - 1), fill=rgb(kleur))
    return im


def knabbel():
    """A kaasknabbel: a cheesy orange curl."""
    return raster(["..oooo..", ".oOOOOo.", "oOO..OOo", "oO....Oo", "oOO....."[:8], ".oOOo...", "..oo...."],
                  {"o": (236, 150, 40), "O": (255, 196, 70)})


def bed(kleur=ROOD, breed=30):
    """A bed from the side: a wooden frame, a red blanket, a white pillow on the left."""
    im = nieuw(breed, 10)
    d = ImageDraw.Draw(im)
    d.rectangle((0, 5, breed - 1, 7), fill=(150, 104, 66, 255))
    d.rectangle((0, 8, 1, 9), fill=(110, 74, 46, 255))
    d.rectangle((breed - 2, 8, breed - 1, 9), fill=(110, 74, 46, 255))
    d.rectangle((0, 1, 1, 5), fill=(110, 74, 46, 255))
    d.rectangle((2, 3, breed - 1, 5), fill=rgb(kleur))
    d.line([(2, 3), (breed - 1, 3)], fill=tint(kleur, 1.3))
    d.rounded_rectangle((2, 2, 9, 4), radius=1, fill=(250, 250, 250, 255))
    return im


def deken(breed, kleur=(240, 120, 170)):
    im = nieuw(breed, 8)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, breed - 1, 7), radius=2, fill=rgb(kleur))
    for x in range(3, breed - 2, 6):
        d.rectangle((x, 2, x + 1, 3), fill=tint(kleur, 1.35))
        d.rectangle((x + 3, 5, x + 4, 5), fill=tint(kleur, 1.35))
    d.line([(1, 7), (breed - 2, 7)], fill=tint(kleur, 0.75))
    return im


def vuurwerk(kleur):
    """Three frames of a firework bursting."""
    frames = []
    for r in (2, 5, 7):
        im = nieuw(17, 17)
        for i in range(8):
            a = i * np.pi / 4
            for rr in range(max(1, r - 2), r + 1):
                x, y = int(round(8 + np.cos(a) * rr)), int(round(8 + np.sin(a) * rr))
                im.putpixel((x, y), rgb(kleur) if rr < r else (255, 255, 255, 255))
        frames.append(im)
    return strip(frames)


def tekstballon(w=40, h=16, staart="links"):
    im = nieuw(w, h + 4)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=3, fill=(255, 255, 255, 255), outline=(60, 40, 70, 255))
    x = 8 if staart == "links" else w - 9
    d.polygon([(x - 2, h - 2), (x + 2, h - 2), (x, h + 3)], fill=(255, 255, 255, 255))
    d.line([(x - 2, h - 1), (x, h + 3)], fill=(60, 40, 70, 255))
    d.line([(x + 2, h - 1), (x, h + 3)], fill=(60, 40, 70, 255))
    return im


def denkwolk(w=30, h=18):
    im = nieuw(w, h + 6)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=6, fill=(255, 255, 255, 255), outline=(150, 150, 170, 255))
    d.ellipse((5, h, 9, h + 3), fill=(255, 255, 255, 255), outline=(150, 150, 170, 255))
    d.rectangle((2, h + 4, 3, h + 5), fill=(255, 255, 255, 255))
    return im


def kruis():
    return raster(["rr.....rr", ".rr...rr.", "..rr.rr..", "...rrr...", "..rr.rr..", ".rr...rr.", "rr.....rr"], {"r": (226, 40, 50)})


def grond(w, h, boven, onder, rand=2):
    """A strip of ground: a lighter top edge."""
    im = nieuw(w, h, rgb(onder))
    ImageDraw.Draw(im).rectangle((0, 0, w - 1, rand - 1), fill=rgb(boven))
    return im
