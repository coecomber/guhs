"""
1.7.0 "Onder de Wol", part 2: the textures of the extra cave blocks (felt gravel, pink crystal dripstone, knuffelwortels,
belletjeskristal, rozenkwarts-druppel) and the meadow fluff particle.

  textures(h)    writes every texture into h.A/textures/block and h.A/textures/particle (same signature as onderwol_tex)
  PYTHONPATH=tools python tools/features/onderwol_tex2.py     (from the project root): writes them all and a contact
                                            sheet ../onderwol_tex2_vel.png (enlarged, with names) to look at

The vanilla-shaped blocks (dripstone, hanging roots, rooted dirt, amethyst) take their silhouette and shading from the
vanilla textures in the client jar: the brightness of every vanilla pixel is mapped onto one of our pastel ramps, so the
shapes and the top-left light stay vanilla-readable while the colours become Guhs. Deterministic (fixed seeds).
"""
import io
import os
import sys
import zipfile

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from features.onderwol_tex import (A_STANDAARD, MC_JAR, rng, vlak, leeg, ruis, zet, img, arr, mix,   # noqa: E402
                                   VILT, VILT_LICHT, VILT_DONKER, _vel)


# ---------------------------------------------------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------------------------------------------------
def vanilla(naam):
    with zipfile.ZipFile(MC_JAR) as z:
        im = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{naam}.png"))).convert("RGBA")
    return arr(im.crop((0, 0, im.width, im.width)))


def helderheid(t):
    """0..1 brightness of the opaque pixels, stretched over their own min..max."""
    l = t[..., :3] @ np.array([0.3, 0.5, 0.2], np.float32)
    m = t[..., 3] > 0
    lo, hi = l[m].min(), l[m].max()
    return np.clip((l - lo) / max(hi - lo, 1), 0, 1)


def ramp(f, kleuren):
    """f (0..1 array) through the colour stops `kleuren` (dark -> light)."""
    k = np.array(kleuren, np.float32)
    x = f * (len(k) - 1)
    i = np.clip(np.floor(x).astype(int), 0, len(k) - 2)
    w = (x - i)[..., None]
    return k[i] * (1 - w) + k[i + 1] * w


def herkleur(t, kleuren, gamma=1.0):
    u = t.copy()
    u[..., :3] = ramp(helderheid(t) ** gamma, kleuren)
    u[..., 3] = np.where(t[..., 3] > 0, 255, 0)
    return u


# ---------------------------------------------------------------------------------------------------------------------
# viltkiezel: gravel of little felt balls
# ---------------------------------------------------------------------------------------------------------------------
BOLKLEUREN = [(232, 146, 178), (206, 120, 156), (244, 196, 210), (196, 140, 178), (250, 226, 222), (222, 160, 196)]


def viltkiezel():
    """Packed little felt balls: every pixel belongs to its nearest ball centre (wrapping, so it tiles); each ball is lit
    from the top-left, and the pixels where two balls meet are the dark plum creases between them."""
    r = rng(2701)
    centra = []
    for rij, y0 in enumerate(range(0, 16, 3)):
        for x0 in range(0, 16, 3):
            centra.append((x0 + 1.5 * (rij % 2) + r.uniform(-0.6, 0.6), y0 + r.uniform(-0.5, 0.5)))
    centra = np.array(centra)
    kleuren = np.array([BOLKLEUREN[i] for i in r.integers(0, len(BOLKLEUREN), len(centra))], np.float32)
    t = vlak((0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx = (x - centra[:, 0] + 8) % 16 - 8
            dy = (y - centra[:, 1] + 8) % 16 - 8
            d = np.hypot(dx, dy)
            o = np.argsort(d)
            i, j = o[0], o[1]
            k = kleuren[i] + (-(dx[i] + dy[i]) * 9) - d[i] * 4
            if d[j] - d[i] < 0.55:                       # a crease between two balls
                k = mix(tuple(k), (120, 62, 96), 0.6)
            t[y, x, :3] = k
    return ruis(t, 2, 2703)


# ---------------------------------------------------------------------------------------------------------------------
# roze knuffelsteen: knuffelsteen, but rosy
# ---------------------------------------------------------------------------------------------------------------------
def roze_knuffelsteen(a):
    """Same blocks, grooves and shading as knuffelsteen.png, recoloured from blush-cream to a clear rose pink."""
    k = arr(Image.open(os.path.join(a, "textures", "block", "knuffelsteen.png")))
    return herkleur(k, [(196, 92, 132), (226, 126, 164), (240, 154, 186), (250, 182, 206)], 0.9)


# ---------------------------------------------------------------------------------------------------------------------
# kristalpegel: pink guhkristal dripstone
# ---------------------------------------------------------------------------------------------------------------------
PEGEL = [(176, 78, 140), (214, 112, 174), (236, 150, 202), (248, 186, 222), (253, 216, 238)]
PEGELDELEN = ["tip", "tip_merge", "frustum", "middle", "base"]


def pegel(richting, deel):
    v = vanilla(f"pointed_dripstone_{richting}_{deel}")
    t = herkleur(v, PEGEL, 1.0)
    m = t[..., 3] > 0
    r = rng(2711 + PEGELDELEN.index(deel) * 7 + (richting == "up"))
    for _ in range(2):
        y = r.integers(0, 16)
        xs = np.nonzero(m[y])[0]
        if len(xs) >= 4:
            zet(t, xs[1] + 1, y, (255, 250, 255))
    return t


def kristalpegelblok():
    t = herkleur(vanilla("dripstone_block"), [(176, 96, 140), (204, 126, 166), (226, 154, 190), (242, 186, 214)], 1.0)
    r = rng(2721)
    for (x, y) in [(3, 2), (11, 5), (6, 10), (13, 13), (1, 13)]:     # little crystal glints in the rock
        zet(t, x, y, (255, 230, 246))
        zet(t, x + 1, y + 1, (236, 120, 190))
    for _ in range(3):
        x, y = r.integers(0, 16, 2)
        zet(t, x, y, (252, 206, 232))
    return t


# ---------------------------------------------------------------------------------------------------------------------
# knuffelwortels
# ---------------------------------------------------------------------------------------------------------------------
WORTEL = [(190, 100, 140), (224, 144, 178), (246, 194, 212), (255, 234, 238)]


def knuffelwortels():
    """Hanging roots: vanilla silhouette, soft cream-pink roots, with a tiny pink fluff tuft on three root tips."""
    t = herkleur(vanilla("hanging_roots"), WORTEL, 1.0)
    m = t[..., 3] > 0
    tips = []
    for x in range(16):                                  # the lowest pixel of each root column = a root tip
        ys = np.nonzero(m[:, x])[0]
        if len(ys):
            tips.append((ys[-1], x))
    tips.sort(reverse=True)
    gekozen = []
    for (y, x) in tips:
        if all(abs(x - gx) > 3 for (_, gx) in gekozen):
            gekozen.append((y, x))
        if len(gekozen) == 3:
            break
    for (y, x) in gekozen:
        zet(t, x, y, (246, 156, 196))
        zet(t, x, min(y + 1, 15), (232, 128, 176))
    return t


def knuffelwortelaarde():
    """Rooted dirt: soft mauve-pink soil (dark part of vanilla) with cream roots (the light part)."""
    v = vanilla("rooted_dirt")
    f = helderheid(v)
    t = v.copy()
    aarde = ramp(np.clip(f / 0.62, 0, 1), [(166, 104, 130), (186, 124, 150), (204, 144, 168)])
    wortel = ramp(np.clip((f - 0.62) / 0.38, 0, 1), [(238, 190, 202), (250, 220, 224), (255, 238, 236)])
    t[..., :3] = np.where((f > 0.62)[..., None], wortel, aarde)
    t[..., 3] = 255
    return t


# ---------------------------------------------------------------------------------------------------------------------
# belletjeskristal
# ---------------------------------------------------------------------------------------------------------------------
def pastel_glans(t, seed):
    """Lay a diagonal lilac -> pink -> mint shimmer over a brightness-mapped crystal."""
    f = helderheid(t)
    yy, xx = np.mgrid[0:16, 0:16]
    golf = (np.sin((xx + yy) / 3.2 + seed) + 1) / 2          # 0..1 diagonal bands
    lila = ramp(f, [(150, 116, 196), (186, 152, 228), (220, 196, 246), (248, 236, 255)])
    roze = ramp(f, [(186, 104, 160), (226, 146, 196), (246, 194, 226), (255, 238, 248)])
    mint = ramp(f, [(110, 190, 172), (150, 222, 196), (190, 244, 220), (236, 255, 246)])
    golf2 = (np.sin((xx - yy) / 2.6 + seed * 2) + 1) / 2
    g = (0.2 + 0.6 * golf)[..., None]
    k = lila * (1 - g) + roze * g                              # mostly a soft lilac <-> pink shimmer
    w = (np.clip((f - 0.3) / 0.4, 0, 1) * golf2 ** 1.5)[..., None]
    k = k * (1 - w) + mint * w                                 # mint only in the bright facets
    u = t.copy()
    u[..., :3] = k
    u[..., 3] = np.where(t[..., 3] > 0, 255, 0)
    return u


def belletjeskristal():
    t = pastel_glans(vanilla("amethyst_block"), 0.3)
    for (x, y) in [(3, 3), (12, 6), (6, 12)]:              # three little sparkles
        zet(t, x, y, (255, 255, 255))
    return t


def bel(t, cx, top):
    """A tiny bell sitting on a crystal tip: a round dome, a 3-wide flared body lit top-left, and a pink clapper below."""
    goud, licht, donker = (250, 220, 140), (255, 248, 220), (212, 160, 84)
    zet(t, cx, top, licht)                                 # the dome
    for dx in (-1, 0, 1):
        zet(t, cx + dx, top + 1, goud)
        zet(t, cx + dx, top + 2, donker if dx == 1 else goud)
    zet(t, cx - 1, top + 1, licht)
    zet(t, cx, top + 3, (240, 110, 168))                    # the clapper


def belletjestrosje():
    """Amethyst cluster silhouette in pastel shimmer, with a tiny bell at the tip of every spike."""
    v = vanilla("amethyst_cluster")
    t = pastel_glans(v, 1.4)
    m = t[..., 3] > 0
    top = [np.nonzero(m[:, x])[0][0] if m[:, x].any() else 99 for x in range(16)]
    pieken = []
    for x in range(16):                                    # local highest points of the silhouette
        l = top[x - 1] if x > 0 else 99
        rr = top[x + 1] if x < 15 else 99
        if top[x] < 99 and top[x] <= l and top[x] <= rr and all(abs(x - p) > 2 for p, _ in pieken):
            pieken.append((x, top[x]))
    pieken = sorted(pieken, key=lambda p: p[1])[:3]
    for (x, y) in pieken:
        bel(t, x, max(y - 3, 0))
    return t


# ---------------------------------------------------------------------------------------------------------------------
# rozenkwarts-druppel
# ---------------------------------------------------------------------------------------------------------------------
def rozenkwarts_druppel():
    t = herkleur(vanilla("small_amethyst_bud"), [(196, 104, 140), (234, 150, 176), (250, 196, 210), (255, 236, 240)], 0.8)
    m = t[..., 3] > 0
    ys = np.nonzero(m.any(1))[0]
    if len(ys):
        xs = np.nonzero(m[ys[0]])[0]
        zet(t, xs[0], ys[0], (255, 255, 255))               # a white glint on the tip
    return t


# ---------------------------------------------------------------------------------------------------------------------
# particle
# ---------------------------------------------------------------------------------------------------------------------
def pluisje():
    """A tiny soft fluff (about 5 px across): white heart, blush edge, two wisps, on a transparent 8x8."""
    t = np.zeros((8, 8, 4), np.float32)

    def p(x, y, k, a):
        t[y, x, :3] = k
        t[y, x, 3] = a
    for (x, y) in [(3, 2), (4, 2), (2, 3), (3, 3), (4, 3), (5, 3), (2, 4), (3, 4), (4, 4), (5, 4), (3, 5), (4, 5)]:
        p(x, y, (255, 248, 252), 255)
    for (x, y) in [(3, 1), (1, 3), (6, 4), (4, 6), (1, 4), (5, 2), (2, 5), (5, 5)]:
        p(x, y, (252, 214, 230), 170)
    for (x, y) in [(6, 2), (1, 6)]:                        # wisps
        p(x, y, (248, 196, 220), 110)
    p(3, 3, (255, 255, 255), 255)
    p(4, 4, (250, 226, 238), 255)
    return t


# ---------------------------------------------------------------------------------------------------------------------
def alle(a):
    """{(kind, name): array} for every texture."""
    out = {("block", "viltkiezel"): viltkiezel(),
           ("block", "roze_knuffelsteen"): roze_knuffelsteen(a)}
    for richting in ("down", "up"):
        for deel in PEGELDELEN:
            out[("block", f"kristalpegel_{richting}_{deel}")] = pegel(richting, deel)
    out[("block", "kristalpegelblok")] = kristalpegelblok()
    out[("block", "knuffelwortels")] = knuffelwortels()
    out[("block", "knuffelwortelaarde")] = knuffelwortelaarde()
    out[("block", "belletjeskristal")] = belletjeskristal()
    out[("block", "belletjestrosje")] = belletjestrosje()
    out[("block", "rozenkwarts_druppel")] = rozenkwarts_druppel()
    out[("particle", "onderwol_pluisje")] = pluisje()
    return out


def textures(h):
    for (soort, naam), t in alle(h.A).items():
        p = os.path.join(h.A, "textures", soort, f"{naam}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img(t).save(p)


if __name__ == "__main__":
    class _H:
        A = A_STANDAARD
    textures(_H)
    texs = alle(A_STANDAARD)
    blok = os.path.join(A_STANDAARD, "textures", "block")
    for ref in ("knuffelsteen", "viltsteen", "guh_kristal_blok", "guh_kristal_cluster"):     # neighbours, for comparison
        texs[("ref", "ref: " + ref)] = arr(Image.open(os.path.join(blok, ref + ".png")))
    p8 = texs[("particle", "onderwol_pluisje")]
    groot = np.zeros((16, 16, 4), np.float32)
    groot[4:12, 4:12] = p8                                  # show the 8x8 particle centred on a 16 cell
    texs[("particle", "onderwol_pluisje")] = groot
    _vel(texs, os.path.join("..", "onderwol_tex2_vel.png"))
    print("onderwol tex2 written;", os.path.abspath(os.path.join("..", "onderwol_tex2_vel.png")), file=sys.stderr)
