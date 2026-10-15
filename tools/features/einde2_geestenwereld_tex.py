"""
einde2, slice geestenwereld: the textures of the Geestenwereld (painted with numpy + Pillow, fixed seeds):

  - the blocks with "turned around" colours: geestengras (lila), geestenaarde, geestenhout (white with a blue grain) and
    schaduwhout (black with a white grain) with their planks, the glowing leaves (cyan, pink), maansteen and its tiles,
    nevelwater (an animated strip whose mist runs UP), the spirit mushroom (stem, cap), the lotus leaf, the meditation
    stone, the memorial stone, the lotus puzzle tiles (one big white lotus cut in corner / edge / middle), the twelve masks
    of Koh's cave and the spirit lantern (the vanilla lantern turned pale cyan);
  - the sky: geestenwereld_lucht.png (a slow twilight of lila and teal with a few stars) and geestenwereld_noorderlicht.png
    (see-through pastel curtains drawn over it), used by E2GeestenwereldSky.

Only numpy and Pillow (the wiki's CI has nothing else).
"""
import math

import numpy as np
from PIL import Image

import features.einde2_kit as kit

LILA = (196, 164, 236)
LILA_D = (150, 116, 204)
AARDE = (164, 178, 214)
WIT = (236, 240, 250)
NERF = (104, 146, 232)
ZWART = (34, 28, 46)
MAAN = (214, 220, 242)
CYAAN = (96, 232, 230)
ROZE = (250, 156, 214)

# the twelve faces of Koh's cave (gezicht 0..11), in this order
MASKERS = ["noh", "guh", "mika", "guhdalf", "sponsguh", "vos", "aap", "lach", "verdriet", "blanco", "blauwe_geest", "uil"]


def _rng(naam):
    return kit.rng(f"geestenwereld/tex/{naam}")


def _ruis(basis, var, naam, size=16):
    r = _rng(naam)
    a = np.zeros((size, size, 4), np.int32)
    n = r.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = basis[c] + n
    a[..., 3] = 255
    return a


def _img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _spikkels(a, kleur, kans, naam):
    r = _rng(naam + "/spikkel")
    m = r.random(a.shape[:2]) < kans
    for c in range(3):
        a[..., c][m] = kleur[c]
    return a


# --- ground and wood ---------------------------------------------------------------------------------------------------
def geestengras_top():
    a = _ruis(LILA, 9, "gras_top")
    a = _spikkels(a, (232, 214, 252), 0.08, "gras_top")
    return _spikkels(a, LILA_D, 0.06, "gras_top2")


def geestenaarde():
    a = _ruis(AARDE, 8, "aarde")
    a = _spikkels(a, (226, 232, 248), 0.05, "aarde")
    return _spikkels(a, (124, 136, 176), 0.07, "aarde2")


def geestengras_zij():
    a = geestenaarde()
    r = _rng("gras_zij")
    for x in range(16):
        d = 3 + int(r.integers(0, 3))
        for y in range(d):
            v = r.normal(0, 8)
            a[y, x, :3] = [LILA[0] + v, LILA[1] + v, LILA[2] + v]
    return a


def hout_zij(basis, nerf, naam):
    a = _ruis(basis, 5, naam)
    r = _rng(naam + "/nerf")
    for x in range(16):
        if r.random() < 0.38:
            for y in range(16):
                if r.random() < 0.85:
                    t = 0.55 + 0.35 * r.random()
                    a[y, x, :3] = [int(basis[c] * (1 - t) + nerf[c] * t) for c in range(3)]
    # a knot
    kx, ky = int(r.integers(4, 12)), int(r.integers(3, 13))
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        a[ky + dy, kx + dx, :3] = nerf
    return a


def hout_top(basis, nerf, naam):
    a = _ruis(basis, 4, naam)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if int(d) % 3 == 0 or d > 7.2:
                a[y, x, :3] = [int(basis[c] * 0.45 + nerf[c] * 0.55) for c in range(3)]
    return a


def planken(basis, nerf, naam):
    a = _ruis(basis, 5, naam)
    for y in range(16):
        if y % 4 == 3:
            a[y, :, :3] = [int(basis[c] * 0.4 + nerf[c] * 0.6) for c in range(3)]
    r = _rng(naam + "/naad")
    for rij in range(4):
        x = int(r.integers(2, 14))
        a[rij * 4:rij * 4 + 3, x, :3] = [int(basis[c] * 0.5 + nerf[c] * 0.5) for c in range(3)]
    for x in range(16):
        if r.random() < 0.25:
            a[int(r.integers(0, 16)), x, :3] = nerf
    return a


def bladeren(kleur, naam):
    """Leaves with holes (cutout) and bright veins: they glow by themselves (the block gives light)."""
    r = _rng(naam)
    a = _ruis(kleur, 14, naam)
    licht = tuple(min(255, c + 50) for c in kleur)
    for y in range(16):
        for x in range(16):
            if r.random() < 0.16:
                a[y, x, 3] = 0
            elif r.random() < 0.12:
                a[y, x, :3] = licht
    return a


# --- stone and water ---------------------------------------------------------------------------------------------------
def maansteen():
    a = _ruis(MAAN, 6, "maansteen")
    r = _rng("maansteen/glans")
    for _ in range(5):          # soft sheen: blue and pink patches
        x, y = int(r.integers(0, 16)), int(r.integers(0, 16))
        kleur = (184, 206, 250) if r.random() < 0.5 else (240, 206, 236)
        for dx in range(-1, 2):
            for dy in range(-1, 1):
                if r.random() < 0.7:
                    a[(y + dy) % 16, (x + dx) % 16, :3] = kleur
    return a


def maansteen_tegels():
    a = _ruis(MAAN, 5, "maantegels")
    voeg = (168, 176, 210)
    for y in range(16):
        for x in range(16):
            if y % 8 == 7 or (x + (4 if (y // 8) % 2 else 0)) % 8 == 7:
                a[y, x, :3] = voeg
    for x, y in ((3, 3), (11, 11)):   # two tiny lotus marks
        a[y, x, :3] = (250, 214, 236)
        a[y - 1, x, :3] = (250, 230, 244)
    return a


def nevelwater(frames=16):
    """A strip of frames: soft white-cyan streaks that rise one pixel per frame (the mist falls UP), see-through."""
    r = _rng("nevelwater")
    basis = np.zeros((16, 16, 4), np.float64)
    for x in range(16):
        golf = 0.5 + 0.5 * math.sin(x * 0.9)
        for y in range(16):
            v = r.random()
            basis[y, x, :3] = [150 + 90 * v, 220 + 30 * v, 236 + 19 * v]
            basis[y, x, 3] = 70 + 70 * golf * v + (60 if v > 0.86 else 0)
    out = np.zeros((16 * frames, 16, 4), np.float64)
    for f in range(frames):
        out[f * 16:(f + 1) * 16] = np.roll(basis, -f, axis=0)
    return _img(out)


def paddenstoel_steel():
    a = _ruis((232, 226, 246), 6, "steel")
    for x in (2, 6, 9, 13):
        a[:, x, :3] = (200, 182, 236)
    return a


def paddenstoel_hoed():
    a = _ruis((86, 206, 196), 10, "hoed")
    r = _rng("hoed/vlek")
    for _ in range(4):
        x, y = int(r.integers(1, 14)), int(r.integers(1, 14))
        for dx in range(2):
            for dy in range(2):
                a[y + dy, x + dx, :3] = (252, 196, 232)
    return a


def lotusblad_top():
    a = np.zeros((16, 16, 4), np.int32)
    r = _rng("lotus")
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            hoek = math.atan2(dy, dx)
            if d > 8.2 or (abs(hoek - 0.4) < 0.18 and d > 2):      # round, with the little notch
                a[y, x] = (0, 0, 0, 0)
                continue
            v = r.normal(0, 7)
            kleur = [244 + v, 196 + v, 222 + v]
            if abs(math.sin(hoek * 5)) < 0.14 or d < 1.2:          # white veins from the middle
                kleur = [252, 240, 248]
            if d > 7.2:
                kleur = [222, 158, 196]
            a[y, x, :3] = kleur
            a[y, x, 3] = 255
    return a


def lotusblad_zij():
    a = _ruis((222, 158, 196), 8, "lotus_zij")
    return a


# --- stones with a meaning ---------------------------------------------------------------------------------------------
def meditatiesteen(top):
    a = maansteen()
    kleur = (110, 190, 255)
    # the swirl of the air nomads (a spiral), bright blue; on top a full spiral, on the sides a little one
    for t in np.linspace(0, 3.2 * math.pi, 120):
        rr = 0.75 * t if top else 0.5 * t
        x, y = int(round(7.5 + rr * math.cos(t))), int(round(7.5 + rr * math.sin(t)))
        if 0 <= x < 16 and 0 <= y < 16:
            a[y, x, :3] = kleur
    if not top:
        a[13:15, 2:14, :3] = (170, 200, 250)
    return a


def gedenksteen(voor):
    a = _ruis((78, 84, 116), 6, "gedenk" + ("v" if voor else "z"))
    if voor:
        r = _rng("gedenk/tekst")
        for regel in range(4):
            y = 3 + regel * 3
            x = 3
            while x < 13:
                n = int(r.integers(1, 4))
                for i in range(n):
                    if x + i < 13:
                        a[y, x + i, :3] = (226, 232, 252)
                x += n + 1
        a[1, 2:14, :3] = (150, 160, 200)
        a[14, 2:14, :3] = (150, 160, 200)
    return a


def lotus_groot(size=48):
    """The white lotus of the Pai Sho tile (8 petals, 4-fold symmetric) on a dark board, size x size."""
    a = np.zeros((size, size, 4), np.int32)
    c = (size - 1) / 2
    r = _rng("paisho")
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            d = math.hypot(dx, dy) / (size / 2)
            hoek = math.atan2(dy, dx)
            bord = (92 + r.normal(0, 5), 54 + r.normal(0, 4), 34 + r.normal(0, 3))
            kleur = bord
            blad = abs(math.cos(4 * hoek))                       # 8 petals
            if d < 0.18:
                kleur = (250, 212, 104)                           # the heart
            elif d < 0.42 + 0.48 * blad ** 0.6:
                kleur = (246, 244, 236) if d < 0.36 + 0.44 * blad ** 0.6 else (190, 176, 168)
            elif abs(d - 0.97) < 0.025:
                kleur = (200, 164, 96)                            # a gold ring
            a[y, x, :3] = kleur
            a[y, x, 3] = 255
    return a


def tegel_stukken():
    """The three pieces of the 3 x 3 lotus: hoek (the north-west corner), rand (the north edge), midden."""
    g = lotus_groot()
    return {"hoek": g[0:16, 0:16].copy(), "rand": g[0:16, 16:32].copy(), "midden": g[16:32, 16:32].copy()}


def tegel_zij():
    a = _ruis((70, 42, 28), 5, "tegel_zij")
    a[0, :, :3] = (200, 164, 96)
    return a


# --- the masks of Koh's cave -------------------------------------------------------------------------------------------
def _gezicht(basis, naam):
    a = np.zeros((16, 16, 4), np.int32)
    r = _rng("masker/" + naam)
    for y in range(16):
        for x in range(16):
            if math.hypot((x - 7.5) / 7.4, (y - 7.8) / 7.9) <= 1.0:
                v = r.normal(0, 5)
                a[y, x, :3] = [basis[0] + v, basis[1] + v, basis[2] + v]
                a[y, x, 3] = 255
    return a


def _zet(a, punten, kleur):
    for x, y in punten:
        if 0 <= x < 16 and 0 <= y < 16:
            a[y, x, :3] = kleur
            a[y, x, 3] = 255


def masker(naam):
    oog = (24, 20, 30)
    if naam == "noh":
        a = _gezicht((240, 236, 226), naam)
        _zet(a, [(4, 6), (5, 6), (10, 6), (11, 6)], oog)
        _zet(a, [(4, 4), (5, 4), (10, 4), (11, 4)], (60, 50, 50))
        _zet(a, [(7, 11), (8, 11)], (200, 40, 50))
        _zet(a, [(3, 8), (12, 8)], (240, 190, 190))
    elif naam == "guh":
        a = _gezicht((248, 176, 204), naam)
        _zet(a, [(2, 1), (3, 1), (2, 2), (13, 1), (12, 1), (13, 2)], (232, 120, 168))      # ears
        _zet(a, [(5, 6), (5, 7), (10, 6), (10, 7)], oog)
        _zet(a, [(6, 10), (7, 11), (8, 11), (9, 10)], (120, 50, 80))
    elif naam == "mika":
        a = _gezicht((150, 92, 196), naam)
        _zet(a, [(4, 6), (5, 6), (4, 7), (5, 7), (10, 6), (11, 6), (10, 7), (11, 7)], oog)
        _zet(a, [(3, 5), (6, 5), (9, 5), (12, 5)], (90, 40, 130))                           # grumpy brows
        _zet(a, [(5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11)], oog)
    elif naam == "guhdalf":
        a = _gezicht((232, 196, 186), naam)
        _zet(a, [(5, 6), (10, 6)], oog)
        _zet(a, [(x, y) for y in range(9, 16) for x in range(3 + (y - 9) // 2, 13 - (y - 9) // 2)], (214, 214, 220))   # beard
        _zet(a, [(x, 3) for x in range(0, 16)] + [(x, 2) for x in range(3, 13)], (110, 110, 126))                     # hat brim
    elif naam == "sponsguh":
        a = np.zeros((16, 16, 4), np.int32)
        for y in range(1, 15):
            for x in range(1, 15):
                a[y, x] = (246, 226, 70, 255)
        _zet(a, [(3, 3), (11, 4), (5, 12), (12, 11)], (200, 176, 40))                       # holes
        _zet(a, [(4, 5), (5, 5), (4, 6), (5, 6), (10, 5), (11, 5), (10, 6), (11, 6)], (250, 250, 250))
        _zet(a, [(5, 6), (10, 6)], (60, 120, 220))
        _zet(a, [(x, 10) for x in range(3, 13)] + [(3, 9), (12, 9)], (120, 40, 40))         # the big grin
        _zet(a, [(6, 11), (7, 11), (8, 11), (9, 11)], (250, 250, 250))                      # buck teeth
    elif naam == "vos":
        a = _gezicht((244, 244, 240), naam)
        _zet(a, [(1, 1), (2, 2), (14, 1), (13, 2), (4, 6), (5, 7), (11, 6), (10, 7)], (210, 60, 40))
        _zet(a, [(5, 6), (10, 6)], oog)
        _zet(a, [(7, 12), (8, 12)], oog)
    elif naam == "aap":
        a = _gezicht((150, 100, 70), naam)
        _zet(a, [(x, y) for y in range(8, 14) for x in range(5, 11)], (230, 196, 160))
        _zet(a, [(5, 6), (10, 6)], oog)
        _zet(a, [(7, 10), (8, 10)], oog)
    elif naam == "lach":
        a = _gezicht((250, 230, 170), naam)
        _zet(a, [(4, 7), (5, 6), (6, 7), (9, 7), (10, 6), (11, 7)], oog)                    # happy eyes ^ ^
        _zet(a, [(4, 10), (5, 11), (6, 12), (7, 12), (8, 12), (9, 12), (10, 11), (11, 10)], (150, 40, 40))
    elif naam == "verdriet":
        a = _gezicht((200, 214, 240), naam)
        _zet(a, [(5, 6), (10, 6)], oog)
        _zet(a, [(5, 8), (5, 9)], (90, 150, 240))                                            # a tear
        _zet(a, [(5, 12), (6, 11), (7, 11), (8, 11), (9, 11), (10, 12)], oog)
    elif naam == "blanco":
        a = _gezicht((236, 236, 236), naam)
    elif naam == "blauwe_geest":
        a = _gezicht((40, 70, 170), naam)
        _zet(a, [(4, 6), (5, 6), (10, 6), (11, 6)], (250, 250, 250))
        _zet(a, [(3, 4), (12, 4)], (20, 30, 90))
        _zet(a, [(x, 10) for x in range(3, 13)] + [(x, 11) for x in range(4, 12, 2)], (250, 250, 250))   # teeth
        _zet(a, [(1, 2), (14, 2), (1, 3), (14, 3)], (40, 70, 170))
    elif naam == "uil":
        a = _gezicht((60, 48, 40), naam)
        for cx in (5, 10):
            _zet(a, [(cx - 1, 6), (cx, 6), (cx + 1, 6), (cx - 1, 7), (cx + 1, 7), (cx - 1, 8), (cx, 8), (cx + 1, 8)], (240, 236, 220))
            _zet(a, [(cx, 7)], oog)
        _zet(a, [(7, 9), (8, 9), (7, 10)], (220, 170, 60))
    else:
        raise ValueError(naam)
    return a


def lantaarn(h):
    """The vanilla lantern turned pale cyan (a spirit light)."""
    src = h.vanilla("block/lantern").convert("RGBA")
    return h.recolour(src, hue=0.50, sat=0.7, val=1.08, only=lambda hh, s, v: s > 0.15)


# --- the sky ----------------------------------------------------------------------------------------------------------
def lucht(size=128):
    """Tileable: a slow twilight between lila and teal, with a few stars."""
    r = _rng("lucht")
    a = np.zeros((size, size, 4), np.float64)
    for y in range(size):
        for x in range(size):
            u, v = 2 * math.pi * x / size, 2 * math.pi * y / size
            t = 0.5 + 0.25 * math.sin(u + 0.6 * math.sin(v)) + 0.25 * math.sin(2 * v + 0.3)
            a[y, x, :3] = [96 * (1 - t) + 54 * t, 70 * (1 - t) + 104 * t, 150 * (1 - t) + 150 * t]
            a[y, x, 3] = 255
    for _ in range(70):
        x, y = int(r.integers(0, size)), int(r.integers(0, size))
        a[y, x, :3] = (250, 244, 255) if r.random() < 0.7 else (255, 200, 236)
    return _img(a)


def noorderlicht(size=128):
    """Tileable see-through curtains in pastel (pink, mint, lila, cyan): drawn over the sky and drifting the other way."""
    a = np.zeros((size, size, 4), np.float64)
    kleuren = [(255, 170, 220), (160, 250, 210), (200, 170, 255), (140, 230, 255)]
    for y in range(size):
        for x in range(size):
            u, v = 2 * math.pi * x / size, 2 * math.pi * y / size
            golf = math.sin(3 * u + 1.7 * math.sin(v * 2))
            band = max(0.0, golf) ** 3
            k = kleuren[int((x * 4) // size) % 4]
            k2 = kleuren[(int((x * 4) // size) + 1) % 4]
            f = (x * 4 / size) % 1.0
            kleur = [k[c] * (1 - f) + k2[c] * f for c in range(3)]
            a[y, x, :3] = kleur
            a[y, x, 3] = 150 * band * (0.6 + 0.4 * math.sin(v * 3 + u))
    return _img(np.clip(a, 0, 255))


def build(h):
    s = h.save
    s(_img(geestengras_top()), "block", "geestengras_top.png")
    s(_img(geestengras_zij()), "block", "geestengras_zij.png")
    s(_img(geestenaarde()), "block", "geestenaarde.png")
    s(_img(hout_zij(WIT, NERF, "geestenhout")), "block", "geestenhout.png")
    s(_img(hout_top(WIT, NERF, "geestenhout_top")), "block", "geestenhout_top.png")
    s(_img(planken(WIT, NERF, "geestenplanken")), "block", "geestenplanken.png")
    s(_img(hout_zij(ZWART, (226, 226, 240), "schaduwhout")), "block", "schaduwhout.png")
    s(_img(hout_top(ZWART, (226, 226, 240), "schaduwhout_top")), "block", "schaduwhout_top.png")
    s(_img(planken(ZWART, (200, 200, 222), "schaduwplanken")), "block", "schaduwplanken.png")
    s(_img(bladeren(CYAAN, "blad_cyaan")), "block", "gloeibladeren_cyaan.png")
    s(_img(bladeren(ROZE, "blad_roze")), "block", "gloeibladeren_roze.png")
    s(_img(bladeren((58, 44, 86), "blad_schaduw")), "block", "schaduwbladeren.png")
    s(_img(maansteen()), "block", "maansteen.png")
    s(_img(maansteen_tegels()), "block", "maansteen_tegels.png")
    s(nevelwater(), "block", "nevelwater.png")
    s(_img(paddenstoel_steel()), "block", "geestpaddenstoel_steel.png")
    s(_img(paddenstoel_hoed()), "block", "geestpaddenstoel_hoed.png")
    s(_img(lotusblad_top()), "block", "lotusblad.png")
    s(_img(lotusblad_zij()), "block", "lotusblad_zij.png")
    s(_img(meditatiesteen(True)), "block", "meditatiesteen_top.png")
    s(_img(meditatiesteen(False)), "block", "meditatiesteen.png")
    s(_img(gedenksteen(True)), "block", "gedenksteen_voor.png")
    s(_img(gedenksteen(False)), "block", "gedenksteen.png")
    for stuk, a in tegel_stukken().items():
        s(_img(a), "block", f"lotuspuzzeltegel_{stuk}.png")
    s(_img(tegel_zij()), "block", "lotuspuzzeltegel_zij.png")
    for naam in MASKERS:
        s(_img(masker(naam)), "block", f"geestenmasker_{naam}.png")
    s(lantaarn(h), "block", "geestlantaarn.png")
    s(lucht(), "environment", "geestenwereld_lucht.png")
    s(noorderlicht(), "environment", "geestenwereld_noorderlicht.png")


TEXTURES = (["geestengras_top", "geestengras_zij", "geestenaarde", "geestenhout", "geestenhout_top", "geestenplanken", "schaduwhout",
             "schaduwhout_top", "schaduwplanken", "gloeibladeren_cyaan", "gloeibladeren_roze", "schaduwbladeren", "maansteen", "maansteen_tegels", "nevelwater",
             "geestpaddenstoel_steel", "geestpaddenstoel_hoed", "lotusblad", "lotusblad_zij", "meditatiesteen_top", "meditatiesteen",
             "gedenksteen_voor", "gedenksteen", "lotuspuzzeltegel_hoek", "lotuspuzzeltegel_rand", "lotuspuzzeltegel_midden",
             "lotuspuzzeltegel_zij", "geestlantaarn"] + [f"geestenmasker_{m}" for m in MASKERS])
