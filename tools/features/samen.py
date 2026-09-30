"""
Samen spelen, samen knuffelen (2.10 "Lieve vadsjes van elkaar", slice samen; see guhs_work210/CONTRACT_210.md par. 5.6).

  - the four clothes of the hartjes levels (GuhClothes marker <samen>, source "band"): the hartjesspeldje (a golden hairpin
    with a pink heart on the left guh ear), the knuffeltruitje (a chunky pink knitted sweater with little hearts, a rolled
    collar and a heart patch on the back), the zielskroontje (a little golden crown with pink heart points) and the
    exclusive gouden hartjes-halsbandje (a golden collar with a sparkly heart locket): bones, textures, item icons
  - particles samen_zielshartje (the zielsguh's twinkling heart) and samen_bff_hart (the big heart of the bff-knuffel)
  - sounds samen.juich, samen.ooh, samen.welkom, samen.bff (little synthesised jingles; the guh's own voice plays along
    from the code)
  - the texts (Dutch in both languages): cheering, the games' names, the reactions, friendships, the rewards, the emote
    lock, the dagboek lines (eerste keren + wist-je-datjes)
  - advancements (tab lieve_vadsjes + hidden quest ones), the game test rooms, and the FTB section
    "Samen spelen, samen knuffelen"
"""
import os

import numpy as np
from PIL import Image

from features import spelen

# =====================================================================================================================
# the clothes
# =====================================================================================================================
CLOTHES = ["samen_hartjesspeldje", "samen_knuffeltruitje", "samen_zielskroontje", "gouden_hartjeshalsbandje"]
H = [0, 6, -2]           # the guh's head pivot (make_guh_variants.HEAD_PIVOT)
BODY = [0, 6, 6]         # the body pivot


def _hart_blokjes(x, y, z, s, diep, plat=False):
    """A little blocky heart: two round lobes, the middle and the tip. (x, y) = the bottom-left of the lobes, s = lobe size.
    plat: lying on its back (in x/z, tip towards +z) instead of standing up (in x/y, tip down)."""
    if plat:
        return [([x, y, z], [s, diep, s], 0), ([x + s, y, z], [s, diep, s], 0),
                ([x + s * 0.25, y, z + s], [s * 1.5, diep, s * 0.8], 0), ([x + s * 0.65, y, z + s * 1.8], [s * 0.7, diep, s * 0.5], 0)]
    return [([x, y, z], [s, s, diep], 0), ([x + s, y, z], [s, s, diep], 0),
            ([x + s * 0.25, y - s * 0.8, z], [s * 1.5, s * 0.8, diep], 0), ([x + s * 0.65, y - s * 1.3, z], [s * 0.7, s * 0.5, diep], 0)]


# the ear: x 4.5..11.5, y 10..17, z -6.25..-4.75 (its front at z -6.25). Only on the LEFT ear: a hairpin is cuter on one side.
BONES = {
    "outfit_oren_hartjesspeld_links": ("ear_left", spelen.EAR_LEFT_PIVOT, "samen_goud", [([5.2, 13.1, -6.75], [4.4, 0.55, 0.5], 0),
                                                                                           ([5.0, 12.95, -6.8], [0.5, 0.85, 0.6], 0)]),
    "outfit_oren_hartjesspeld_hart": ("ear_left", spelen.EAR_LEFT_PIVOT, "samen_hart", _hart_blokjes(8.0, 14.1, -7.15, 1.15, 0.6)),
    # the sweater: the onesie bones ("outfit_suit") in knitted pink, plus a rolled collar behind the head and two hearts
    # (one on the back above the tail, one lying on top of the back)
    "outfit_samen_truitje": ("body", BODY, "samen_hart", [([-6.95, 0.8, -3.15], [13.9, 11.0, 1.5], 0)]),
    "outfit_samen_truitje_hart": ("body", BODY, "samen_hart", _hart_blokjes(-2.3, 7.5, 11.95, 2.3, 0.45)
                                  + _hart_blokjes(-2.3, 10.95, 3.0, 2.3, 0.45, plat=True)),
    # the crown: a small golden band on top of the head between the ears, heart-shaped points and a pink gem in front
    "outfit_samen_kroontje": ("head", H, "samen_goud", [([-2.7, 15.0, -8.0], [5.4, 1.2, 4.4], 0)]
                              + [([x, 16.2, z], [0.9, 1.0, 0.9], 0) for x in (-2.7, 1.8) for z in (-8.0, -4.5)]),
    "outfit_samen_kroontje_hart": ("head", H, "samen_hart", _hart_blokjes(-0.95, 16.95, -8.25, 0.95, 0.5)
                                   + [([-0.55, 15.25, -8.3], [1.1, 0.7, 0.35], 0)]),
    # the collar: a golden band under the snoet and round the cheeks, a heart locket hanging from the middle
    "outfit_samen_halsbandje": ("head", H, "samen_goud", [([-4.9, 1.25, -12.0], [9.8, 0.75, 0.6], 0), ([-5.5, 1.25, -11.6], [0.6, 0.75, 4.2], 0),
                                                         ([4.9, 1.25, -11.6], [0.6, 0.75, 4.2], 0), ([-0.3, 0.45, -12.1], [0.6, 0.8, 0.5], 0)]),
    "outfit_samen_halsbandje_hart": ("head", H, "samen_hart", _hart_blokjes(-1.15, -0.45, -12.35, 1.15, 0.6)),
}


def _goud(v, rng, glans=(255, 246, 190)):
    """Polished gold: warm yellow with a diagonal shine and a few twinkles."""
    a = v.fabric((238, 188, 58), rng, 6)
    n = a.shape[0]
    for y in range(n):
        for x in range(n):
            t = abs(((x + y) % 16) - 5) / 5.0
            a[y, x] = a[y, x] * (0.7 + 0.3 * min(1, t)) + np.array(glans, np.float32) * 0.3 * max(0, 1 - t)
    for y, x in ((4, 7), (12, 22), (20, 5), (27, 17)):
        a[y, x] = (255, 255, 240)
    return np.clip(a, 0, 255)


def _roze_glans(v, rng, basis=(244, 96, 164), licht=(255, 200, 228)):
    """A glossy pink enamel (the hearts): a light spot top-left, a deeper pink at the bottom."""
    a = v.fabric(basis, rng, 5)
    n = a.shape[0]
    for y in range(n):
        for x in range(n):
            d = ((x - 9) ** 2 + (y - 9) ** 2) ** 0.5 / 14
            a[y, x] = a[y, x] * (0.85 + 0.15 * y / n) + np.array(licht, np.float32) * max(0, 0.55 - d)
    return np.clip(a, 0, 255)


def _gebreid(v, rng, basis, rib):
    """Chunky knit: vertical 'v' stitches (rows of little chevrons) with a darker rib."""
    a = v.fabric(basis, rng, 6)
    n = a.shape[0]
    for y in range(n):
        for x in range(n):
            if (x % 4 == 0) or ((y + (x % 4)) % 4 == 0 and x % 4 in (1, 3)):
                a[y, x] = a[y, x] * 0.82 + np.array(rib, np.float32) * 0.18
    return a


def _truitje(v, rng):
    """The sweater: pink knit with rows of little white hearts and a ribbed hem."""
    a = _gebreid(v, rng, (246, 150, 190), (190, 70, 120))
    hart = ["x.x", "xxx", ".x."]
    for oy in range(3, 28, 9):
        for ox in range((oy // 9 % 2) * 4 + 2, 30, 8):
            for dy, row in enumerate(hart):
                for dx, c in enumerate(row):
                    if c == "x":
                        a[oy + dy, ox + dx] = (255, 244, 248)
    a[-3:, :] = a[-3:, :] * 0.7 + np.array((190, 70, 120), np.float32) * 0.3   # the ribbed hem
    return np.clip(a, 0, 255)


def clothes(rng, v):
    return {
        "samen_hartjesspeldje": {"samen_goud": lambda: _goud(v, rng), "samen_hart": lambda: _roze_glans(v, rng)},
        "samen_knuffeltruitje": {"suit": lambda: _truitje(v, rng),
                                 "samen_hart": lambda: np.clip(_gebreid(v, rng, (226, 64, 120), (150, 30, 80)), 0, 255)},
        "samen_zielskroontje": {"samen_goud": lambda: _goud(v, rng), "samen_hart": lambda: _roze_glans(v, rng, (255, 110, 180), (255, 236, 246))},
        "gouden_hartjeshalsbandje": {"samen_goud": lambda: _goud(v, rng, (255, 252, 214)),
                                     "samen_hart": lambda: _roze_glans(v, rng, (236, 72, 150), (255, 214, 236))},
    }


SPELD_ICON = ["................", "................", ".........aa.aa..", "........abbabba.", "........abcbbba.", "........abbbbba.",
              ".........abbba..", "..gggggggggabag.", ".ghhhhhhhhhhagh.", "..gggggggggggg..", "................", "................",
              "................", "................", "................", "................"]
TRUI_ICON = ["...aaa....aaa...", "..abbbaaaabbba..", ".abbbccccccbbba.", ".abbbbbbbbbbbba.", ".aabbwbbbbwbbaa.", "..aabbbbbbbbaa..",
             "...abbbrrbrbba..", "...abbrrrrrbba..", "...abbbrrrbbba..", "...abbbbrbbbba..", "...abwbbbbbwba..", "...acccccccccca..",
             "................", "................", "................", "................"]
KROON_ICON = ["................", "................", "..h..........h..", ".hhh...h.h..hhh.", "..g...hhhhh..g..", "..gg...hhh..gg..",
              "..ggg...h..ggg..", "..gggggggggggg..", "..gyggggrggggyg..", "..gggggrrrggggg.", "..gggggggggggg..", "..aaaaaaaaaaaa..",
              "................", "................", "................", "................"]
HALSBAND_ICON = ["................", "................", "..gg........gg..", "..ag........ga..", "...gg......gg...", "....gg....gg....",
                 ".....gggggg.....", ".......gg.......", "......h..h......", ".....hrhhrh.....", ".....hrrrrh.....", "......hrrh......",
                 ".......hh.......", "................", "................", "................"]


def icons(ic):
    goud = {"g": (238, 188, 58), "a": (170, 120, 30), "y": (255, 246, 190), "h": (255, 170, 205), "r": (230, 60, 140)}
    return {
        "samen_hartjesspeldje": ic.icon(SPELD_ICON, {"a": (150, 40, 90), "b": (246, 110, 176), "c": (255, 220, 236),
                                                     "g": (238, 188, 58), "h": (255, 240, 190)}),
        "samen_knuffeltruitje": ic.icon(TRUI_ICON, {"a": (160, 60, 105), "b": (246, 150, 190), "c": (210, 100, 150),
                                                    "w": (255, 244, 248), "r": (226, 50, 110)}),
        "samen_zielskroontje": ic.icon(KROON_ICON, goud),
        "gouden_hartjeshalsbandje": ic.icon(HALSBAND_ICON, goud),
    }


# =====================================================================================================================
# particles
# =====================================================================================================================
HART = ["..XX.XX..", ".XHHXXXX.", "XHHXXXXXX", "XHXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "...XXX...", "....X...."]


def _zielshartje(frame):
    """A small pink heart with a white rim-light and a four-point sparkle that moves round it (4 frames: it twinkles)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    x0, y0 = 3, 4
    kleur = [(255, 120, 186), (255, 140, 200), (255, 160, 212), (255, 140, 200)][frame]
    for y, row in enumerate(HART):
        for x, c in enumerate(row):
            if c == ".":
                continue
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < 9 and 0 <= ny < 9) or HART[ny][nx] == ".":
                    px[x0 + nx, y0 + ny] = (176, 40, 110, 255)
    for y, row in enumerate(HART):
        for x, c in enumerate(row):
            if c != ".":
                px[x0 + x, y0 + y] = (255, 236, 246, 255) if c == "H" else kleur + (255,)
    sx, sy = [(13, 2), (14, 11), (2, 13), (1, 3)][frame]
    for d, a in ((0, 255), (1, 200)):
        for ddx, ddy in ((d, 0), (-d, 0), (0, d), (0, -d)):
            if 0 <= sx + ddx < 16 and 0 <= sy + ddy < 16:
                px[sx + ddx, sy + ddy] = (255, 250, 210, a)
    return img


def _bff_hart(frame):
    """The big bff heart (32x32): a soft pink-to-rose gradient, a white shine, a darker rim and sparkles (2 frames)."""
    size = 32
    a = np.zeros((size, size, 4), np.uint8)
    ys, xs = np.mgrid[0:size, 0:size]
    x = (xs - 15.5) / 12.5
    y = (15.5 - ys) / 12.0
    f = (x ** 2 + y ** 2 - 1) ** 3 - x ** 2 * y ** 3
    binnen = f <= 0
    rand = np.zeros_like(binnen)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            rand |= np.roll(np.roll(binnen, dy, 0), dx, 1)
    rand &= ~binnen
    grad = np.clip((ys - 3) / 25.0, 0, 1)[..., None]
    top, onder = np.array((255, 176, 214)), np.array((232, 60, 130))
    kleur = (top * (1 - grad) + onder * grad).astype(np.uint8)
    a[binnen, :3] = kleur[binnen]
    a[binnen, 3] = 255
    a[rand] = (150, 24, 80, 255)
    for (cx, cy) in ((8, 8), (9, 8), (8, 9), (9, 9), (10, 7), (7, 10)):
        a[cy, cx] = (255, 250, 253, 255)
    spots = [((27, 3), (3, 26), (28, 22)), ((3, 4), (28, 27), (16, 30))][frame]
    for sx, sy in spots:
        for d in (-2, -1, 0, 1, 2):
            for px, py in ((sx + d, sy), (sx, sy + d)):
                if 0 <= px < size and 0 <= py < size:
                    a[py, px] = (255, 244, 180, 255 if abs(d) < 2 else 170)
    return Image.fromarray(a)


def particles(h):
    for i in range(4):
        h.save(_zielshartje(i), "particle", f"samen_zielshartje_{i}.png")
    h.w(f"{h.A}/particles/samen_zielshartje.json", {"textures": [f"guhs:samen_zielshartje_{i}" for i in range(4)]})
    for i in range(2):
        h.save(_bff_hart(i), "particle", f"samen_bff_hart_{i}.png")
    h.w(f"{h.A}/particles/samen_bff_hart.json", {"textures": [f"guhs:samen_bff_hart_{i}" for i in range(2)]})


# =====================================================================================================================
# sounds: four little jingles (made once: vorbis encoding isn't byte-for-byte stable)
# =====================================================================================================================
SR = 44100


def _toon(freq, lengte, vorm="sinus", vibrato=0.0, glij=0.0):
    t = np.arange(int(SR * lengte)) / SR
    f = freq * (1 + glij * t / max(lengte, 1e-6)) * (1 + vibrato * np.sin(2 * np.pi * 6 * t))
    fase = 2 * np.pi * np.cumsum(f) / SR
    if vorm == "driehoek":
        golf = 2 / np.pi * np.arcsin(np.sin(fase))
    else:
        golf = np.sin(fase) + 0.25 * np.sin(2 * fase) * np.exp(-t * 6)
    aanslag = np.minimum(1, t / 0.008)
    return golf * aanslag


def _plak(out, geluid, start, volume):
    s = int(start * SR)
    e = min(len(out), s + len(geluid))
    out[s:e] += geluid[:e - s] * volume


def _glitter(out, van, tot, n, seed):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        s = rng.uniform(van, tot)
        ln = int(0.03 * SR)
        t = np.arange(ln) / SR
        _plak(out, np.sin(2 * np.pi * rng.uniform(3600, 6400) * t) * np.exp(-t * 110), s, 0.07)


def _klaar(out):
    n = len(out)
    out *= np.minimum(1, (n - np.arange(n)) / (0.1 * SR))
    return out / max(1e-6, np.abs(out).max()) * 0.8


def _juich():
    """'Tu-di-DIE!': three quick rising squeaky notes, the last one with a happy wobble, and glitter."""
    out = np.zeros(int(SR * 0.75))
    for i, (f, lengte) in enumerate(((784, 0.09), (988, 0.09), (1319, 0.34))):
        t = np.arange(int(SR * lengte)) / SR
        _plak(out, _toon(f, lengte, "driehoek", vibrato=0.02 if i == 2 else 0.0) * np.exp(-t * (9 if i < 2 else 4)), 0.02 + i * 0.1, 0.6)
    _glitter(out, 0.25, 0.65, 14, 61)
    return _klaar(out)


def _ooh():
    """'Ooh... njeg': a soft round note sliding down, then a smaller lower one (lovingly sad, never grumpy)."""
    out = np.zeros(int(SR * 1.0))
    t1 = np.arange(int(SR * 0.45)) / SR
    _plak(out, _toon(660, 0.45, vibrato=0.012, glij=-0.22) * np.exp(-t1 * 2.5), 0.0, 0.6)
    t2 = np.arange(int(SR * 0.45)) / SR
    _plak(out, _toon(470, 0.45, vibrato=0.015, glij=-0.12) * np.exp(-t2 * 4), 0.42, 0.45)
    return _klaar(out)


def _welkom():
    """Welcome back: a bouncy five-note arpeggio going up (C E G C E) with bells, then a shimmer."""
    out = np.zeros(int(SR * 1.3))
    for i, f in enumerate((523.3, 659.3, 784.0, 1046.5, 1318.5)):
        lengte = 0.5 if i == 4 else 0.22
        t = np.arange(int(SR * lengte)) / SR
        bel = _toon(f, lengte) + 0.3 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 10)
        _plak(out, bel * np.exp(-t * (5 if i < 4 else 3)), 0.02 + i * 0.11, 0.55)
    _glitter(out, 0.5, 1.2, 20, 62)
    return _klaar(out)


def _bff():
    """The bff-knuffel: two soft heartbeats and a warm chord that swells up (C E G B, like a big hug), then glitter."""
    out = np.zeros(int(SR * 1.6))
    for s in (0.0, 0.2):                                        # ba-dum
        t = np.arange(int(SR * 0.12)) / SR
        _plak(out, np.sin(2 * np.pi * (70 + 40 * np.exp(-t * 30)) * t) * np.exp(-t * 28), s, 0.9)
    for f in (261.6, 329.6, 392.0, 493.9, 523.3):
        t = np.arange(int(SR * 1.2)) / SR
        zwel = np.minimum(1, t / 0.35) * np.exp(-np.maximum(0, t - 0.5) * 2.5)
        _plak(out, _toon(f, 1.2, vibrato=0.004) * zwel, 0.32, 0.22)
    _glitter(out, 0.6, 1.4, 16, 63)
    return _klaar(out)


GELUIDEN = {"juich": _juich, "ooh": _ooh, "welkom": _welkom, "bff": _bff}
ONDERTITELS = {"juich": "Guh juicht", "ooh": "Guh: ooh njeg...", "welkom": "Guh zegt welkom thuis", "bff": "Bff-knuffel"}


def sounds(h):
    for naam, maak in GELUIDEN.items():
        path = os.path.join(h.A, "sounds", "samen", f"{naam}.ogg")
        if not os.path.exists(path):
            import soundfile as sf
            os.makedirs(os.path.dirname(path), exist_ok=True)
            sf.write(path, maak().astype(np.float32), SR, format="OGG", subtype="VORBIS")

    def patch(d):
        for naam in GELUIDEN:
            d[f"samen.{naam}"] = {"sounds": [{"name": f"guhs:samen/{naam}", "volume": 0.8}], "subtitle": f"subtitles.guhs.samen.{naam}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    for naam, tekst in ONDERTITELS.items():
        h.lang(f"subtitles.guhs.samen.{naam}", tekst, tekst)


# =====================================================================================================================
# advancements (tab guhs:lieve_vadsjes) and the hidden quest ones
# =====================================================================================================================
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
QUEST_ADVANCEMENTS = ["samen_gespeeld", "samen_kart", "samen_schaatsen", "samen_zwemmen", "samen_troost", "samen_welkom", "samen_onweer",
                      "samen_welterusten", "samen_vriendjes_knuffel"]
ADVANCEMENTS = [  # name, parent, icon, frame, title, description
    ("samen_juichen", "root", "minecraft:firework_rocket", "task", "Hup, hup, VAHOEG!",
     "Je guh juicht voor je bij een minigame: een goede worp, een vangst, een snelle tijd"),
    ("samen_gespeeld", "samen_juichen", "minecraft:target", "task", "Samen gespeeld",
     "Speel een minigame met je guh erbij. Samen spelen = hartjes!"),
    ("samen_record", "samen_gespeeld", "minecraft:jukebox", "goal", "Een dansje voor je record",
     "Zet een nieuw record neer terwijl je guh toekijkt: hij doet het knuffeldansje"),
    ("samen_kart", "samen_gespeeld", "minecraft:saddle", "goal", "Achterop!",
     "Je guh springt achterop in de racekart en rijdt de hele race mee"),
    ("samen_schaatsen", "samen_gespeeld", "minecraft:ice", "goal", "Schaatsmaatjes",
     "Je guh schaatst met je mee op de Elf-Guhjestocht"),
    ("samen_zwemmen", "samen_gespeeld", "minecraft:heart_of_the_sea", "task", "Zwemmaatjes",
     "Zwem in het Knuffelbad: je guh springt erin en zwemt met je mee"),
    ("samen_welkom", "root", "minecraft:cake", "task", "Welkom thuis!",
     "Kom na een lange tijd terug bij je guh: hij doet een welkom-terug-dansje"),
    ("samen_vriendjes", "root", "minecraft:poppy", "task", "Guh-vriendjes",
     "Twee van je guhs worden vriendjes (laat ze veel samen zijn)"),
    ("samen_besties", "samen_vriendjes", "minecraft:pink_tulip", "challenge", "Beste vriendjes",
     "Twee van je guhs worden beste vriendjes: een echt bff-duo!"),
    ("samen_beloning_lief", "band_lief", "guhs:samen_hartjesspeldje", "task", "Een speldje met een hartje",
     "Lieve vadsjes van elkaar: het hartjesspeldje en de emote Hartjes zijn van jou"),
    ("samen_beloning_mega", "band_mega", "guhs:samen_knuffeltruitje", "goal", "Het knuffeltruitje",
     "Mega lieve vadsjes van elkaar: het knuffeltruitje en het knuffeldansje zijn van jou"),
    ("samen_beloning_zielsguh", "band_zielsguh", "guhs:samen_zielskroontje", "challenge", "Een kroontje voor je zielsguh",
     "Zielsguh bff 5evr <3: het zielskroontje en de bff-knuffel zijn van jou"),
    ("samen_halsbandje", "samen_beloning_zielsguh", "guhs:gouden_hartjeshalsbandje", "challenge", "Het gouden hartjes-halsbandje",
     "Het allermooiste halsbandje: alleen voor een zielsguh. VAHOEG!"),
    ("samen_bff_knuffel", "samen_beloning_zielsguh", "minecraft:red_dye", "challenge", "Bff-knuffel",
     "Doe de bff-knuffel met je zielsguh: een dikke knuffel met een groot hart boven jullie"),
]


def advancements(h):
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    for name, parent, icon, frame, title, desc in ADVANCEMENTS:
        h.w(f"{h.D}/advancement/lieve_vadsjes/{name}.json", {
            "parent": f"guhs:lieve_vadsjes/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.lieve_vadsjes.{name}.title"},
                        "description": {"translate": f"advancements.guhs.lieve_vadsjes.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": IMPOSSIBLE})
        h.lang(f"advancements.guhs.lieve_vadsjes.{name}.title", title, title)
        h.lang(f"advancements.guhs.lieve_vadsjes.{name}.description", desc, desc)


# =====================================================================================================================
# texts (Dutch in both languages)
# =====================================================================================================================
# the minigames (Minigames ids): the name, and the little line the guh writes in its dagboek after playing it together
SPELLEN = {
    "beauty": ("de Guh Beauty Show", "Mijn baasje liep over de catwalk en ik klapte zo hard met mijn pootjes dat ze rood werden. Tien punten van mij!"),
    "race": ("de Guhracebaan", "Wij raceten vandaag! Ik heb wel mijn oren moeten vasthouden, zo hard ging het. VAHOEG!"),
    "meppen": ("Mika-meppen", "Mika's meppen met een zacht hamertje. Ze vonden het niet eens erg, ze giechelden alleen maar. Njeg."),
    "disco": ("de Guhdisco", "We hebben gedanst op de lichtjesvloer! Ik ken nu alle kleurtjes uit mijn hoofd. Bijna."),
    "golf": ("guhgolf", "Mijn baasje sloeg een balletje in een putje. Ik heb het balletje bijna opgegeten. Bijna!"),
    "smul": ("het Smulfestijn", "Er vielen hapjes uit de lucht! Het was de mooiste dag van mijn leven, en ik heb er maar drie gepikt."),
    "vissen": ("de viswedstrijd", "Wij visten guhvissen. Ik ruik nu naar vis, maar ik ben er heel trots op."),
    "verstop": ("verstoppertje", "Wij speelden verstoppertje. Ik verstopte me achter mijn eigen vads. Niemand vond me, njeg."),
    "bakkerij": ("de bakkerij", "Wij bakten koekjes! Er zat meel op mijn snoet en een koekje in mijn wangzak. Geheimpje."),
    "creche": ("de Knuffelcreche", "Wij pasten op babyguhs. Ze zijn zo klein! Ik voelde me een hele grote vadsige guh."),
    "theehuis": ("het theehuis", "Een theekransje! Ik heb mijn pinkje opgestoken. Ik heb geen pinkje, maar het ging best."),
    "kapper": ("de kapper", "Er kwam een kapsel voorbij met krullen. Ik wil ook krullen. Of een kuifje. Of allebei!"),
    "sterrenwacht": ("de Guh-Sterrenwacht", "Wij keken naar sterren. Er was er een in de vorm van een kaasknabbel, dat weet ik zeker."),
    "ballon": ("een ballonvlucht", "We vlogen in een luchtballon! Ik keek naar beneden en zag heel de Guhmensie. Mijn buik kriebelde."),
    "knuffelbad": ("het Knuffelbad", "Wij gingen van de glijbaan: wieee! Nu ben ik een nat vadsje, maar wel een blij nat vadsje."),
    "grijpmachine": ("de grijpmachine", "Een grijpmachine! Ik wilde erin kruipen om de knuffels te redden. Mocht niet, njeg."),
    "sjoelen": ("sjoelen", "Wij sjoelden bij Opoe Njegschuif. Ik juichte bij elke schijf, ook die in het verkeerde vakje."),
    "doolhof": ("het Guhdoolhof", "Wij liepen door het doolhof. Ik wist de weg! Ik zei het alleen niet, anders was het niet spannend."),
    "katapult": ("de Knabbelkatapult", "Pluisballen schieten op een Mika-fort! BOEM! De Mika's lachten, dus dat telt als lief."),
    "knabbelspelen": ("de Knabbelspelen", "Sport! Mijn baasje rende en ik rende mee in mijn hoofd. Ik ben nu heel moe."),
    "elftocht": ("de Elf-Guhjestocht", "Wij schaatsten langs alle dorpjes. Mijn pootjes zijn koud, maar mijn hartje is warm."),
    "circuit": ("het Guh-Circuit", "Rondjes racen op het circuit! Ik zat achterop en riep elke bocht VAHOEG. Elke bocht."),
    "beroepen": ("de beroepenstraat", "Wij deden echt werk, net als grote guhs. Ik was de allerbeste helper, zei ik tegen mezelf."),
}

# the eerste keren (gui.guhs.dagboek.eerste.<id> + .tekst): the fixed ones of samen, plus samen's own
EERSTE = {
    "eerste_minigame": ("Samen gespeeld", "We deden samen een spelletje. Ik juichte zo hard dat mijn oortjes flapperden."),
    "eerste_vriendje": ("Een vriendje!", "Ik heb een vriendje gemaakt. We zijn nu vadsjes van elkaar. Niet zo vads als met mijn baasje, hoor."),
    "samen_bestie": ("Een beste vriendje", "Ik heb nu een BESTE vriendje. Dat is een vriendje, maar dan met extra kaasknabbels."),
    "samen_op_reis": ("Samen op reis", "We liepen en liepen en liepen. Ik weet niet waar we heen gingen, maar het was samen, dus het was goed."),
    "samen_kart": ("Achterop in de kart", "Ik zat achterop in een racekart! Mijn wangen wapperden. Nog een keer, NU."),
    "samen_schaatsen": ("Samen geschaatst", "Ik kan schaatsen! Nou ja, glijden op mijn buikje. Maar heel sierlijk."),
    "samen_zwemmen": ("Samen gezwommen", "Ik zwom naast mijn baasje in het Knuffelbad. Guhs drijven heel goed, dankzij de vads."),
    "samen_troost": ("Een troostknuffel", "Mijn baasje was even weg en kwam heel moe terug. Ik gaf een knuffel. Knuffels maken alles beter."),
    "samen_welkom": ("Welkom thuis!", "Mijn baasje was zo lang weg. Toen ze terugkwam deed ik een dansje. Een heel groot dansje."),
    "samen_onweer": ("Knuffelen bij onweer", "Het rommelde in de lucht. Ik was niet bang hoor, ik wilde gewoon heel dicht bij mijn baasje zitten."),
    "samen_welterusten": ("Welterusten!", "Mijn baasje ging slapen en ik zwaaide. Morgen weer een vadsige dag, njeg."),
    "samen_bff_knuffel": ("De bff-knuffel", "De allergrootste knuffel ooit, met een hart erboven. Het hart was echt, ik heb het gezien."),
}

WISTJEDAT = {  # gui.guhs.wistjedat.samen.<id> (%s = the player's name, or the other guh's name)
    "eerste_minigame": "Vandaag deden %s en ik samen een spelletje. Ik juichte het hardst van iedereen, njeg!",
    "record": "%s zette een record neer en ik deed mijn allermooiste dansje. Mijn vads wiebelt nog steeds.",
    "op_reis": "Wist je dat %s en ik al een heel eind samen gelopen hebben? Mijn pootjes weten het nog.",
    "kart": "Ik zat achterop in de racekart van %s! Ik riep VAHOEG in elke bocht. Ik ben nu een beetje schor.",
    "schaatsen": "Vandaag schaatste ik naast %s over het ijs. Ik viel maar twee keer, en toen gleed ik gewoon door op mijn buik.",
    "zwemmen": "Ik ben met %s wezen zwemmen in het Knuffelbad. Wist je dat een guh niet kan zinken? Te vadsig!",
    "troost": "%s kwam heel moe terug. Ik stond al te wachten en gaf de dikste troostknuffel ooit.",
    "welkom": "%s was zo lang weg! Ik heb een welkom-terug-dansje gedaan. Met draaien en huppelen en alles.",
    "onweer": "Het onweerde, dus ik kroop tegen %s aan. Wist je dat een baasje beter werkt dan een deken?",
    "welterusten": "Ik zwaaide %s welterusten. Slaap lekker vads, heb ik gezegd. Of gedacht. Allebei.",
    "bff_knuffel": "%s en ik deden de bff-knuffel. Er zweefde een groot roze hart boven ons. Zielsguh bff 5evr <3!",
    "vriendje": "Ik heb een vriendje: %s! We lopen samen, we knuffelen samen, we snurken samen. Vahoeg!",
    "bestie": "%s is nu mijn BESTE vriendje. Ik heb het in mijn dagboekje geschreven, met een hartje erbij.",
    "samen_slapen": "Vannacht sliep ik in ons huisje naast %s. Wie het hardst snurkte? Ik niet, njeg. (Wel.)",
}

TEKSTEN = {
    # the clothes
    "item.guhs.samen_hartjesspeldje": "Hartjesspeldje",
    "item.guhs.samen_knuffeltruitje": "Knuffeltruitje",
    "item.guhs.samen_zielskroontje": "Zielskroontje",
    "item.guhs.gouden_hartjeshalsbandje": "Gouden hartjes-halsbandje",
    # minigames together
    "gui.guhs.samen.samen_gespeeld": "♥ %s speelde %s met je mee! Samen spelen = hartjes.",
    "gui.guhs.samen.kart": "♥ %s springt achterop! Samen racen, VAHOEG!",
    "gui.guhs.samen.zwemmen": "♥ %s zwemt met je mee. Plons plons, njeg!",
    # reactions
    "gui.guhs.samen.troost": "♥ %s stond al op je te wachten en geeft je een troostknuffel. Njeg, gelukkig ben je er weer!",
    "gui.guhs.samen.welkom": "♥ %s is zó blij dat je er weer bent! Welkom thuis, VAHOEG!",
    "gui.guhs.samen.onweer": "Brr, onweer! %s kruipt dicht tegen je aan. Knuffel!",
    "gui.guhs.samen.welterusten": "%s zwaait je welterusten. Slaap lekker vads, njeg!",
    "gui.guhs.samen.welterusten_meer": "%s en nog %s guhs zwaaien je welterusten. Slaap lekker vads, njeg!",
    "gui.guhs.samen.bff": "♥ Jij en %s geven elkaar een dikke bff-knuffel! Zielsguh bff 5evr <3",
    # friendships
    "gui.guhs.samen.vriendjes": "♥ %s en %s zijn nu vriendjes! Njeg, wat lief.",
    "gui.guhs.samen.besties": "♥ %s en %s zijn nu BESTE vriendjes! Een echt bff-duo. VAHOEG!",
    # the rewards of the hartjes levels, the emote lock
    "gui.guhs.samen.emote_ontgrendeld": "♥ Ontgrendeld door jullie hartjes: de emote %s! Kies hem in het Emotes-menu van je guh.",
    "gui.guhs.samen.emote_op_slot": "%s zit nog op slot: word eerst %s met een guh.",
    "gui.guhs.samen.halsbandje": "♥ Het gouden hartjes-halsbandje is van jou! Het mooiste wat een guh om kan hebben, en alleen te krijgen met een zielsguh. VAHOEG!",
    "gui.guhs.samen.bff_uitleg": "Tip: kies de Bff-knuffel in het Emotes-menu terwijl je vlak bij je guh staat: dan knuffelen jullie elkaar echt.",
    "gui.guhs.emotes.loop_kort": "Blijf",
    "gui.guhs.emotes.op_slot": "Op slot, njeg! Word eerst %s met een guh.",
}


def texts(h):
    for key, nl in TEKSTEN.items():
        h.lang(key, nl, nl)
    for sid, (naam, zin) in SPELLEN.items():
        h.lang(f"gui.guhs.samen.spel.{sid}", naam, naam)
        h.lang(f"gui.guhs.wistjedat.samen.spel.{sid}", zin, zin)
    for k, (titel, tekst) in EERSTE.items():
        h.lang(f"gui.guhs.dagboek.eerste.{k}", titel, titel)
        h.lang(f"gui.guhs.dagboek.eerste.{k}.tekst", tekst, tekst)
    for k, v in WISTJEDAT.items():
        h.lang(f"gui.guhs.wistjedat.samen.{k}", v, v)


# =====================================================================================================================
# the game test rooms
# =====================================================================================================================
def test_templates(h):
    """The test room: 16 x 6 x 16 of air. The tests lay their own floor (and pool) in code, on the helper's y 0: that way
    it lines up with where the tests put their players and guhs, whatever the test framework does with templates."""
    h.Structure((16, 6, 16)).save("samen_test_wei")


def selfcheck(h):
    import re
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "samen", "SamenSpel.java"), encoding="utf-8").read()
    ids = set(re.findall(r'"([a-z]+)"', java[java.index("SPELLEN = "):java.index(");", java.index("SPELLEN = "))]))
    problems = []
    if ids != set(SPELLEN):
        problems.append(f"SamenSpel.SPELLEN and samen.py SPELLEN differ: {sorted(ids ^ set(SPELLEN))}")
    kleding = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    blok = kleding[kleding.index("// <samen>"):kleding.index("// </samen>")]
    enum = [m.lower() for m in re.findall(r"^\s+([A-Z_]+)\(Slot", blok, re.M)]
    if enum != CLOTHES:
        problems.append(f"GuhClothes <samen> {enum} != CLOTHES {CLOTHES}")
    for bone in re.findall(r'"(outfit_[a-z_]+)"', blok):
        if bone != "outfit_suit" and not any(b.startswith(bone) for b in BONES):
            problems.append(f"no bone for {bone}")
    for p in (f"{h.A}/particles/samen_zielshartje.json", f"{h.A}/particles/samen_bff_hart.json"):
        if not os.path.exists(p):
            problems.append(f"missing {p}")
    for naam in GELUIDEN:
        if not os.path.exists(os.path.join(h.A, "sounds", "samen", f"{naam}.ogg")):
            problems.append(f"missing sound {naam}")
    if problems:
        raise SystemExit("samen self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    particles(h)
    sounds(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_band, section "Samen spelen, samen knuffelen")
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("samen_juichen", "Hup, hup, VAHOEG!", "Neem je guh mee naar een minigame: sjoelen, de katapult, golf, vissen, de disco... Bij een goede "
      "worp of een snelle tijd &djuicht&r hij voor je, bij een misser is hij heel lief een beetje verdrietig (ooh njeg...).",
      "minecraft:firework_rocket", [adv("guhs:lieve_vadsjes/samen_juichen")], shape="circle")
    q("samen_gespeeld", "Samen gespeeld", "Speel een minigame helemaal uit met je guh in de buurt. &dSamen spelen geeft hartjes&r, en het "
      "komt in zijn dagboekje!", "minecraft:target", [adv("samen_gespeeld")], rewards=(("guhs:kaas_knabbels", 12),))
    q("samen_record", "Een dansje voor je record", "Zet een nieuw persoonlijk record neer terwijl je guh toekijkt. Hij doet dan het "
      "&dknuffeldansje&r en jullie krijgen extra hartjes.", "minecraft:jukebox", [adv("guhs:lieve_vadsjes/samen_record")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 3),))
    q("samen_kart", "Achterop!", "Start een race op de &dGuhracebaan&r of het &dGuh-Circuit&r met je eigen guh in de buurt: hij springt "
      "&dachterop in de kart&r en rijdt de hele race mee. Elke ronde een VAHOEG!", "minecraft:saddle", [adv("samen_kart")],
      rewards=(("guhs:kaas_knabbels", 16),))
    q("samen_schaatsen", "Schaatsmaatjes", "Schaats de &dElf-Guhjestocht&r (of lekker vrij) met je guh in de buurt: hij &dschaatst naast je&r "
      "mee, net als een Pinguh. Brr, maar gezellig!", "minecraft:ice", [adv("samen_schaatsen")], rewards=(("guhs:kaas_knabbels", 16),))
    q("samen_zwemmen", "Zwemmaatjes", "Ga zwemmen in het &dKnuffelbad&r met je guh erbij: hij springt in het water en &dzwemt met je mee&r. "
      "Glijd je van een glijbaan? Dan juicht hij vanaf de kant.", "minecraft:heart_of_the_sea", [adv("samen_zwemmen")])
    q("samen_welkom", "Welkom thuis!", "Ben je lang weg geweest? Als je terugkomt doet je guh een &dwelkom-terug-dansje&r. Hij heeft je gemist!",
      "minecraft:cake", [adv("samen_welkom")])
    q("samen_troost", "Een troostknuffel", "Oeps, doodgegaan? Je guh staat al bij je &drespawnplek&r te wachten en geeft je een "
      "&dtroostknuffel&r. Njeg, gelukkig ben je er weer!", "minecraft:red_bed", [adv("samen_troost")])
    q("samen_onweer", "Knuffelen bij onweer", "Bij &donweer&r komt je guh dicht tegen je aan kruipen voor een knuffel. Niet bang hoor, "
      "gewoon gezellig.", "minecraft:lightning_rod", [adv("samen_onweer")])
    q("samen_welterusten", "Welterusten!", "Ga in bed liggen met je guhs in de buurt: ze &dzwaaien je welterusten&r. Slaap lekker vads!",
      "minecraft:pink_bed", [adv("samen_welterusten")])
    q("samen_vriendjes", "Guh-vriendjes", "Guhs die veel samen zijn worden &dvriendjes&r: ze lopen samen, knuffelen elkaar, slapen naast "
      "elkaar in hun huisje en spelen samen op de wip en de schommel. Laat twee guhs vriendjes worden!", "minecraft:poppy",
      [adv("guhs:lieve_vadsjes/samen_vriendjes")], rewards=(("guhs:guh_cupcake", 2),))
    q("samen_vriendjes_knuffel", "Knuffelende vriendjes", "Zie twee van je guh-vriendjes elkaar een &dknuffel&r geven. Hartjes ertussen, "
      "njeg!", "minecraft:pink_dye", [adv("samen_vriendjes_knuffel")])
    q("samen_besties", "Beste vriendjes", "Blijven twee vriendjes lang samen, dan worden het &dbeste vriendjes&r: een echt bff-duo. Je ziet "
      "het in de &dGuhdex&r bij Mijn guhs.", "minecraft:pink_tulip", [adv("guhs:lieve_vadsjes/samen_besties")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), shape="gear", xp=200)
    q("samen_speldje", "Een speldje met een hartje", "Word met een guh &dlieve vadsjes van elkaar&r: al je guhs kunnen nu het "
      "&dhartjesspeldje&r op hun oortje, en de emote &dHartjes&r (hartjes blazen) is van jou.", "guhs:samen_hartjesspeldje",
      [adv("guhs:lieve_vadsjes/samen_beloning_lief")])
    q("samen_truitje", "Het knuffeltruitje", "Word met een guh &dmega lieve vadsjes van elkaar&r: het &dknuffeltruitje&r en het "
      "&dknuffeldansje&r zijn van jou.", "guhs:samen_knuffeltruitje", [adv("guhs:lieve_vadsjes/samen_beloning_mega")],
      rewards=(("guhs:kaas_knabbels", 16),))
    q("samen_halsbandje", "Het gouden hartjes-halsbandje", "&6Zielsguh bff 5evr <3&r: het &6zielskroontje&r en het exclusieve &6gouden "
      "hartjes-halsbandje&r zijn van jou. En je zielsguh heeft nu een glinsterend hartje naast zijn naam!",
      "guhs:gouden_hartjeshalsbandje", [adv("guhs:lieve_vadsjes/samen_halsbandje")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),),
      shape="gear", xp=500)
    q("samen_bff_knuffel", "Bff-knuffel", "Kies met je zielsguh de emote &dBff-knuffel&r terwijl je vlak bij hem staat: jullie geven elkaar "
      "een dikke knuffel met een &dgroot hart&r erboven. Zielsguh bff 5evr <3", "minecraft:red_dye",
      [adv("guhs:lieve_vadsjes/samen_bff_knuffel")], shape="gear", xp=300)
