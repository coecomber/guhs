"""
bbq2 (fossiel-mijn): the textures of features/fossiel_mijn.py, all painted here (numpy / PIL, 16x16 unless said).

  blocks   zoutkristal ore / vein / block / little crystals, bottenzand (with a bone tip, and brushed empty), puin (rubble
           over a cart rail), the skeleton stand and its bones (bone, the skull's faces)
  items    zoutkristal, the five fossil bones, the Guhkwastje, the Zoutkristalhouweel (ICONS, with ITEM_PAL)

The rock and the ash under the crystals and the bones are the Barbecuether's own (features/barbecuether_tex.py).
"""
import math
import random

import numpy as np

from features import barbecuether_tex as bt

# salt crystal: white with a rosy and an icy side (pink like Himalaya salt, because guhs are pink)
ZOUT = (246, 246, 250)
ZOUT_ROZE = (250, 206, 220)
ZOUT_BLAUW = (204, 224, 246)
ZOUT_SCHADUW = (168, 172, 204)
ZOUT_RAND = (112, 112, 150)
# fossil bone: old ivory
BOT = (232, 222, 196)
BOT_LICHT = (248, 242, 224)
BOT_SCHADUW = (192, 178, 148)
BOT_DONKER = (138, 122, 96)
HOUT = (104, 74, 50)
HOUT_DONKER = (68, 46, 32)
MESSING = (214, 168, 70)


def _kristallen(a, seed, n, lang=(3, 6)):
    """Paints n little crystal shards (slanted, with a light and a shaded side and a dark outline) on a."""
    rng = random.Random(seed)
    size = a.shape[0]
    for _ in range(n):
        x, y = rng.randrange(1, size - 3), rng.randrange(lang[1], size - 1)
        hoog = rng.randint(*lang)
        kleur = rng.choice((ZOUT_ROZE, ZOUT_BLAUW, ZOUT, ZOUT))
        scheef = rng.choice((-1, 0, 0, 1))
        for i in range(hoog):
            yy = y - i
            xx = x + (scheef * i) // 3
            breed = 2 if i < hoog - 1 else 1
            if not (0 <= yy < size):
                continue
            for d in range(breed):
                if 0 <= xx + d < size:
                    a[yy, xx + d] = kleur if d == 0 else ZOUT_SCHADUW
            if 0 <= xx - 1 < size:
                a[yy, xx - 1] = ZOUT_RAND
            if 0 <= xx + breed < size:
                a[yy, xx + breed] = ZOUT_RAND
        top = y - hoog
        if 0 <= top < size and 0 <= x + (scheef * hoog) // 3 < size:
            a[top, x + (scheef * hoog) // 3] = ZOUT
    return a


def zoutkristalerts(seed=31101):
    """Houtskoolsteen with a few salt crystals grown into it (the nether quartz ore of the Barbecuether)."""
    a = np.asarray(bt.houtskoolsteen(seed, embers=False).convert("RGB")).astype(np.float32)
    return bt.img(_kristallen(a, seed, 5, (2, 4)))


def zoutader(seed=31102):
    """The vein of the mine: the rock is nearly all crystal, with a glowing seam through it."""
    a = np.asarray(bt.houtskoolsteen(seed, embers=False).convert("RGB")).astype(np.float32)
    n = bt.vnoise(16, 3, seed, 2)
    naad = np.abs(n - 0.5) < 0.09
    a[naad] = ZOUT_ROZE
    a[np.abs(n - 0.5) < 0.035] = ZOUT
    return bt.img(_kristallen(a, seed + 1, 11, (3, 6)))


def zoutkristalblok(seed=31103):
    """A block of pressed salt crystal: facets in white, rose and ice blue, with a pale frame."""
    rng = np.random.default_rng(seed)
    punten = rng.integers(0, 16, (9, 2))
    kleuren = [ZOUT, ZOUT_ROZE, ZOUT_BLAUW, (236, 232, 244), (252, 226, 234), (224, 236, 250), ZOUT, ZOUT_ROZE, ZOUT_BLAUW]
    a = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        for x in range(16):
            d = [min(abs(x - px), 16 - abs(x - px)) + min(abs(y - py), 16 - abs(y - py)) * 1.3 for px, py in punten]
            order = np.argsort(d)
            a[y, x] = kleuren[order[0]]
            if d[order[1]] - d[order[0]] < 1.0:
                a[y, x] = ZOUT_SCHADUW
    a = bt.grain(a, seed, 4)
    a[0, :] = a[15, :] = a[:, 0] = a[:, 15] = (214, 210, 232)
    return bt.img(a)


def zoutkristalletjes(seed=31104):
    """A tuft of crystals for a cross model (transparent around them)."""
    a = np.zeros((16, 16, 4), np.float32)
    rgb = np.zeros((16, 16, 3), np.float32) - 1
    _kristallen(rgb, seed, 0)
    rng = random.Random(seed)
    for x, hoog, kleur, scheef in ((3, 7, ZOUT_BLAUW, -1), (7, 12, ZOUT, 0), (11, 8, ZOUT_ROZE, 1), (5, 5, ZOUT_ROZE, 0), (9, 6, ZOUT_BLAUW, 0)):
        for i in range(hoog):
            y = 15 - i
            xx = x + (scheef * i) // 4
            breed = 3 if i < hoog - 2 else 2 if i < hoog - 1 else 1
            for d in range(breed):
                rgb[y, xx + d] = kleur if d == 0 else (ZOUT if d == 1 and breed == 3 else ZOUT_SCHADUW)
            rgb[y, xx - 1] = ZOUT_RAND
            if xx + breed < 16:
                rgb[y, xx + breed] = ZOUT_RAND
        rgb[15 - hoog, x + (scheef * hoog) // 4] = ZOUT_RAND
    _ = rng
    mask = rgb[..., 0] >= 0
    a[mask, :3] = rgb[mask]
    a[mask, 3] = 255
    return bt.img(a)


def bottenzand(seed=31110, leeg=False):
    """Ash with something in it: the tip of a bone and a rib sticking out. leeg: brushed empty (a shallow dent, brush strokes)."""
    a = np.asarray(bt.as_aarde(seed).convert("RGB")).astype(np.float32)
    n = bt.vnoise(16, 5, seed, 2)
    a = a * 0.55 + bt.ramp(n, [(0, bt.ASH_DARK), (0.5, bt.ASH), (1, (188, 184, 178))]) * 0.45
    a = bt.grain(a, seed, 4)
    if leeg:
        for y in range(4, 12):
            for x in range(3, 13):
                d = math.hypot((x - 7.5) / 5.0, (y - 7.5) / 4.0)
                if d < 1:
                    a[y, x] = a[y, x] * (0.62 + 0.3 * d)
        for y in (5, 7, 9):                                   # the strokes of the brush
            a[y, 4:12:2] = (150, 146, 140)
    else:
        for (x, y) in ((4, 5), (5, 5), (6, 6), (7, 6), (8, 7)):          # a rib, slanting
            a[y, x] = BOT
            a[y + 1, x] = BOT_SCHADUW
        a[4, 3] = a[4, 4] = BOT_LICHT
        for (x, y) in ((10, 10), (11, 10), (10, 11), (11, 11), (12, 11)):   # the knob of a bone
            a[y, x] = BOT
        a[10, 10] = BOT_LICHT
        a[12, 10:13] = BOT_DONKER
        a[9, 12] = BOT_SCHADUW
    return bt.img(a)


def puin(seed=31120):
    """Fallen rock: lumps of houtskoolsteen with ash-grey edges and dark gaps between them."""
    rng = np.random.default_rng(seed)
    punten = rng.integers(0, 16, (10, 2))
    basis = np.asarray(bt.houtskoolsteen(seed, embers=False).convert("RGB")).astype(np.float32)
    a = basis.copy()
    for y in range(16):
        for x in range(16):
            d = sorted(math.hypot(min(abs(x - px), 16 - abs(x - px)), min(abs(y - py), 16 - abs(y - py))) for px, py in punten)
            if d[1] - d[0] < 0.9:
                a[y, x] = (16, 12, 13)                         # the gap between two lumps
            elif d[0] < 1.6:
                a[y, x] = basis[y, x] * 1.5 + 24               # the lit top of a lump
    a[rng.integers(0, 16, 5), rng.integers(0, 16, 5)] = ZOUT_SCHADUW   # a grain of salt here and there
    return bt.img(a)


def bot(seed=31130):
    """Old ivory bone with fine cracks (the skeleton on the stand, the statuette)."""
    n = bt.vnoise(16, 4, seed, 3)
    a = bt.ramp(n, [(0, BOT_SCHADUW), (0.4, BOT), (1, BOT_LICHT)])
    a = bt.grain(a, seed, 3)
    rng = random.Random(seed)
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(10)
        for _ in range(rng.randint(3, 5)):
            a[y % 16, x % 16] = BOT_DONKER
            x += rng.choice((-1, 0, 1))
            y += 1
    return bt.img(a)


def schedel(seed=31131):
    """The skull's front (16x16, the whole face): two big round eye sockets, a nose hole and a row of little teeth."""
    a = np.asarray(bot(seed).convert("RGB")).astype(np.float32)
    donker = (38, 30, 26)
    for cx in (4.5, 11.5):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, (y - 6.0) * 1.0)
                if d < 2.7:
                    a[y, x] = donker
                elif d < 3.5:
                    a[y, x] = BOT_SCHADUW
    a[9:11, 7:9] = donker                                      # the nose
    a[11, 7:9] = BOT_SCHADUW
    a[13, 2:14] = BOT_DONKER                                   # the jaw line
    for x in range(2, 14, 2):                                  # teeth
        a[14, x] = BOT_LICHT
        a[14, x + 1] = BOT_DONKER
        a[15, x] = BOT_LICHT
    return bt.img(a)


def rek(seed=31132):
    """The stand's wood: dark planks with a brass strip (the name plate)."""
    a = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        for x in range(16):
            a[y, x] = HOUT_DONKER if y % 8 == 7 or (x + (y // 8) * 5) % 11 == 0 else HOUT
    a = bt.grain(a, seed, 5)
    a[5:8, 4:12] = MESSING
    a[5, 4:12] = (246, 214, 120)
    a[6, 5:11:2] = (120, 88, 30)                               # "letters"
    return bt.img(a)


# =====================================================================================================================
# items
# =====================================================================================================================
ITEM_PAL = {
    ".": (0, 0, 0, 0), "k": ZOUT_RAND + (255,), "w": ZOUT + (255,), "r": ZOUT_ROZE + (255,), "b": ZOUT_BLAUW + (255,),
    "s": ZOUT_SCHADUW + (255,), "B": BOT + (255,), "L": BOT_LICHT + (255,), "S": BOT_SCHADUW + (255,), "D": BOT_DONKER + (255,),
    "x": (38, 30, 26, 255), "h": HOUT + (255,), "H": HOUT_DONKER + (255,), "m": MESSING + (255,), "M": (150, 110, 36, 255),
    "p": (244, 150, 190, 255), "P": (206, 100, 150, 255), "q": (255, 204, 224, 255),
}
ICONS = {
    # one fat crystal with two small ones against it
    "zoutkristal": ["................", ".......kk.......", "......kwwk......", "......kwrsk.....", ".....kwwrsk.....",
                    ".....kwrrsk.....", "..kk.kwrrsk.....", ".kbbkkwrrsk.kk..", ".kbbskwrrskkrrk.", ".kbbskwrrskrrsk.",
                    ".kbbskwrrskrrsk.", ".kbsskwrsskrssk.", "..kkskwsssksssk.", "...kkkkkkkkkkk..", "................",
                    "................"],
    # the skull: round, with ears (it is a guh after all), big eye sockets and teeth
    "fossielmijn_bot_schedel": ["................", "..DD......DD....", ".DLBD....DLBD...", ".DBSDDDDDDBSD...", ".DBLLLLLLLLBD...",
                                ".DLBBBBBBBBBSD..", ".DBxxBBBBxxBSD..", ".DBxxxBBxxxBSD..", ".DBxxBBBBxxBSD..", ".DBBBBxxBBBBSD..",
                                ".DBBBBBBBBBSSD..", "..DSLSLSLSLSD...", "..DSDSDSDSDSD...", "...DDDDDDDDD....", "................",
                                "................"],
    # the spine: a row of vertebrae with little spikes
    "fossielmijn_bot_ruggengraat": ["................", "................", ".............D..", "...........DDLD.", "..........DLBBD.",
                                    "........DDDBSD..", ".......DLBBDD...", ".....DDDBSD.....", "....DLBBDD......", "..DDDBSD........",
                                    ".DLBBDD.........", ".DBSD...........", "..DD............", "................", "................",
                                    "................"],
    # the ribcage: three hoops on a piece of spine
    "fossielmijn_bot_ribben": ["................", "................", "..DDDDDDDDDDDD..", ".DLBBBBBBBBBBSD.", ".DBDDBDDDBDDBSD.",
                               ".DBD.DBD.DBD.DD.", ".DBD.DBD.DBD....", ".DBD.DBD.DBD....", ".DBD.DBD.DBD....", ".DSD.DBD.DSD....",
                               "..D..DSD..D.....", "......D.........", "................", "................", "................",
                               "................"],
    # two short legs with big feet
    "fossielmijn_bot_pootjes": ["................", "................", "...DD.....DD....", "..DLBD...DLBD...", "..DBSD...DBSD...",
                                "..DBSD...DBSD...", "..DBSD...DBSD...", "..DBSD...DBSD...", "..DBSD...DBSD...", ".DLBSDD.DLBSDD..",
                                "DLBBBSSDLBBBSSD.", "DBDBDBDDBDBDBDD.", ".D.D.D..D.D.D...", "................", "................",
                                "................"],
    # the tail: thick to thin, a little curl at the end
    "fossielmijn_bot_staart": ["................", "................", "................", "............DD..", "...........DLBD.",
                               "..........DLBSD.", ".........DLBSD..", "........DLBSD...", "......DDLBSD....", "....DDLBBSD.....",
                               "..DDLBBSSD......", ".DLBBSSDD.......", ".DBSDDD.........", ".DSD............", "..D.............",
                               "................"],
    # the Guhkwastje: a brush with a wooden handle, a brass ring and pink bristles
    "fossielmijn_kwastje": ["................", "............HH..", "...........HhhH.", "..........HhhH..", ".........HhhH...",
                            "........HhhH....", ".......HhhH.....", "......MmmH......", ".....MmmM.......", "....PpmM........",
                            "...PpqpP........", "..PpqppP........", "..PqppP.........", "..PppP..........", "...PP...........",
                            "................"],
    # the Zoutkristalhouweel: a pickaxe with a head of crystal
    "fossielmijn_zoutkristalhouweel": ["................", "....kkkkkkk.....", "...kwwrrrrsk....", "..kwrkkkkkrsk...", "..kbk....Hkrsk..",
                                       "..kbk...HhHkrk..", "..ksk..HhH.kbk..", "...k..HhH..kbk..", ".....HhH...ksk..", "....HhH.....k...",
                                       "...HhH..........", "..HhH...........", ".HhH............", ".HH.............", "................",
                                       "................"],
}
