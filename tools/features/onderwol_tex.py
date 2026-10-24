"""
1.7.0 "Onder de Wol": the textures of the new underground (blocks and items of feature/onderwol).

  textures(h)    writes every 16x16 texture into h.A/textures/block and h.A/textures/item (called by onderwol.py)
  python tools/features/onderwol_tex.py     (from the project root): writes them all and a contact sheet
                                            ../onderwol_tex_vel.png (enlarged, with names) to look at

The layer stones: viltsteen is new (a warm, darker pink felt with fibres, clearly not wool and not knuffelsteen); the ores
of the knuffelsteen and guhkristalsteen layers use the existing knuffelsteen.png / guh_kristalsteen.png. The vanilla ores
take their pattern from the vanilla textures: every pixel of {x}_ore.png that differs from stone.png is ore, and those
pixels are laid on our stone. Deterministic (fixed seeds).
"""
import io
import os
import sys
import zipfile

import numpy as np
from PIL import Image, ImageDraw

A_STANDAARD = os.path.join("src", "main", "resources", "assets", "guhs")
MC_JAR = os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "libraries", "com", "mojang", "minecraft", "26.1.2",
                      "minecraft-26.1.2-client.jar")
VANILLA = {"kolenerts": "coal", "kopererts": "copper", "ijzererts": "iron", "gouderts": "gold", "lapiserts": "lapis",
           "redstoneerts": "redstone", "diamanterts": "diamond", "smaragderts": "emerald"}
SOORTEN = list(VANILLA) + ["vadserts", "kaasknabbelerts", "kaaskorrelerts", "pigmenterts"]
LAGEN = ["vilt", "knuffel", "kristal"]


# ---------------------------------------------------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------------------------------------------------
def rng(seed):
    return np.random.default_rng(seed)


def vlak(kleur):
    t = np.zeros((16, 16, 4), np.float32)
    t[..., :3] = kleur
    t[..., 3] = 255
    return t


def leeg():
    return np.zeros((16, 16, 4), np.float32)


def ruis(t, amp, seed, blok=1):
    r = rng(seed)
    n = r.normal(0, amp, (16 // blok, 16 // blok, 1))
    n = np.repeat(np.repeat(n, blok, 0), blok, 1)
    t = t.copy()
    t[..., :3] += n
    return t


def zet(t, x, y, kleur, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        t[y, x, :3] = kleur
        t[y, x, 3] = a


def img(t):
    return Image.fromarray(np.clip(t, 0, 255).astype(np.uint8), "RGBA")


def arr(im):
    return np.array(im.convert("RGBA"), np.float32)


def mix(a, b, f):
    return tuple(a[i] * (1 - f) + b[i] * f for i in range(3))


# ---------------------------------------------------------------------------------------------------------------------
# the viltsteen family
# ---------------------------------------------------------------------------------------------------------------------
VILT = (214, 122, 156)          # warmer and darker than pink wool (237, 141, 172 looks lighter and cooler) and knuffelsteen
VILT_LICHT = (232, 150, 180)
VILT_DONKER = (178, 92, 126)


def viltsteen():
    """Felt: soft mottling plus short fibres in all directions."""
    r = rng(1701)
    t = ruis(vlak(VILT), 5, 1702, 2)
    t = ruis(t, 4, 1703)
    for _ in range(34):
        x, y = r.integers(0, 16, 2)
        dx, dy = [(1, 0), (0, 1), (1, 1), (1, -1)][r.integers(0, 4)]
        k = VILT_LICHT if r.random() < 0.55 else VILT_DONKER
        for i in range(r.integers(2, 4)):
            zet(t, (x + dx * i) % 16, (y + dy * i) % 16, mix(t[(y + dy * i) % 16, (x + dx * i) % 16, :3], k, 0.65))
    return t


def gepolijst():
    t = ruis(vlak((222, 134, 166)), 2.5, 1711, 2)
    t[0, :, :3] = VILT_LICHT
    t[:, 0, :3] = VILT_LICHT
    t[15, :, :3] = VILT_DONKER
    t[:, 15, :3] = VILT_DONKER
    t[1:15, 1, :3] += 6
    t[1, 1:15, :3] += 6
    return t


def bakstenen():
    t = ruis(vlak((220, 130, 162)), 3, 1721, 1)
    voeg = (170, 86, 120)
    for y in (3, 7, 11, 15):
        t[y, :, :3] = voeg
    for rij, y0 in enumerate((0, 4, 8, 12)):
        for x in ((7, 15) if rij % 2 == 0 else (3, 11)):
            t[y0:y0 + 3, x, :3] = voeg
        t[y0, :, :3] += 12          # light top edge of every row of bricks
    return t


def tegels():
    t = ruis(vlak((224, 138, 170)), 2, 1731, 2)
    voeg = (176, 90, 126)
    for i in (0, 8):
        t[i, :, :3] = VILT_LICHT
        t[:, i, :3] = VILT_LICHT
        t[i + 7, :, :3] = voeg
        t[:, i + 7, :3] = voeg
    return t


def gebeiteld():
    """Polished viltsteen with a guh face carved in: round ears, eyes, a little snout."""
    t = gepolijst()
    diep, licht = (168, 84, 118), (238, 164, 192)
    for (x, y) in [(2, 2), (3, 2), (2, 3), (3, 3), (12, 2), (13, 2), (12, 3), (13, 3)]:   # the round ears (top corners)
        zet(t, x, y, diep)
    zet(t, 2, 2, licht)
    zet(t, 13, 2, licht)
    for x in range(4, 12):                                                 # the head outline
        zet(t, x, 4, diep)
        zet(t, x, 12, diep)
    for y in range(5, 12):
        zet(t, 3, y, diep)
        zet(t, 12, y, diep)
    for (x, y) in [(5, 7), (6, 7), (5, 8), (6, 8), (9, 7), (10, 7), (9, 8), (10, 8)]:   # eyes
        zet(t, x, y, diep)
    zet(t, 5, 7, licht)
    zet(t, 9, 7, licht)
    for (x, y) in [(7, 10), (8, 10)]:                                      # the snout
        zet(t, x, y, (196, 96, 140))
    return t


def knuffelnaad():
    """The bottom of the world: deep magenta felt with a seam of white stitches across."""
    t = ruis(vlak((92, 34, 74)), 6, 1741, 2)
    t = ruis(t, 4, 1742)
    for y in (7, 8):
        t[y, :, :3] = mix((92, 34, 74), (40, 10, 34), 0.6)
    for x in range(0, 16, 4):                            # stitches across the seam, like a sewn plush
        for y in (6, 7, 8, 9):
            zet(t, x + 1, y, (236, 200, 222) if y in (6, 9) else (206, 166, 192))
    return t


def guhklei():
    """Pink clay: smooth, a little lilac, with soft lighter smudges."""
    r = rng(1751)
    t = ruis(vlak((214, 170, 196)), 3, 1752, 2)
    for _ in range(7):
        x, y = r.integers(0, 15, 2)
        t[y:y + 2, x:x + 2, :3] += 9
    for _ in range(5):
        x, y = r.integers(0, 16, 2)
        zet(t, x, y, (194, 150, 180))
    return t


# ---------------------------------------------------------------------------------------------------------------------
# the ores
# ---------------------------------------------------------------------------------------------------------------------
def vanilla_ertspatroon(naam):
    """(ore pixels mask, the ore texture) from the vanilla jar: what differs from stone.png is ore."""
    with zipfile.ZipFile(MC_JAR) as z:
        steen = arr(Image.open(io.BytesIO(z.read("assets/minecraft/textures/block/stone.png"))))
        erts = arr(Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{naam}_ore.png"))))
    masker = np.abs(erts[..., :3] - steen[..., :3]).sum(-1) > 24
    return masker, erts


def op_steen(steen, masker, erts):
    """Lay the vanilla ore on our stone. The coloured ore pixels stay as they are; the grey ones around them (the
    vanilla shading, which would look like a grey stain on pink) become our own stone, darkened as much as vanilla's grey
    is darker than stone. So coal stays dark, but plum-dark instead of grey."""
    t = steen.copy()
    e = erts[..., :3]
    verzadiging = e.max(-1) - e.min(-1)
    kleurig = masker & (verzadiging > 28)
    grijs = masker & ~kleurig
    helder = e.mean(-1, keepdims=True) / 125.0          # vanilla stone is ~125 bright
    t[grijs, :3] = np.clip(steen[..., :3] * helder * 0.92, 0, 255)[grijs]
    t[kleurig, :3] = e[kleurig]
    return t


def vlekjes(steen, kleuren, plekken, seed):
    """Little 2x2 / 3x2 lumps with a light corner and a dark corner, the idiom of the mod's ores."""
    r = rng(seed)
    t = steen.copy()
    for i, (x, y) in enumerate(plekken):
        k = kleuren[i % len(kleuren)]
        b, h = (3, 2) if r.random() < 0.4 else (2, 2)
        for dx in range(b):
            for dy in range(h):
                zet(t, x + dx, y + dy, k)
        zet(t, x, y, mix(k, (255, 255, 255), 0.45))
        zet(t, x + b - 1, y + h - 1, mix(k, (0, 0, 0), 0.3))
        zet(t, x + b, y + h - 1, mix(t[min(y + h - 1, 15), min(x + b, 15), :3], (0, 0, 0), 0.18))
    return t


PLEKKEN = [(2, 2), (9, 1), (5, 6), (12, 7), (1, 10), (7, 11), (12, 12)]
PLEKKEN_KLEIN = [(3, 3), (10, 2), (6, 8), (12, 10), (2, 12)]


def vads(steen):
    # the vads ore of the mod has pink-lilac gems: here bright vahoeg-pink with a golden glint
    return vlekjes(steen, [(236, 92, 170), (214, 70, 156), (250, 128, 196)], PLEKKEN_KLEIN, 1801)


def kaasknabbel(steen, a):
    """The kaasknabbel overlay of the mod (orange curls) laid on the stone, a little smaller."""
    p = os.path.join(a, "textures", "block", "kaasknabbel_overlay.png")
    if os.path.exists(p):
        ov = arr(Image.open(p))
        if ov.shape[0] != 16:
            ov = arr(Image.open(p).resize((16, 16), Image.NEAREST))
        t = steen.copy()
        m = ov[..., 3] > 100
        t[m, :3] = ov[m, :3]
        return t
    return vlekjes(steen, [(245, 160, 40), (226, 128, 30)], PLEKKEN, 1811)


def kaaskorrel(steen):
    return vlekjes(steen, [(248, 210, 96), (236, 186, 70), (252, 226, 130)], PLEKKEN, 1821)


def pigment(steen):
    pastel = [(240, 120, 150), (130, 206, 168), (130, 176, 236), (246, 220, 110), (190, 150, 232), (244, 166, 112), (120, 214, 220)]
    return vlekjes(steen, pastel, PLEKKEN, 1831)


def vahoegiet_erts(steen):
    """Big glossy pink-white gems in guhkristalsteen."""
    t = steen.copy()
    for (x, y) in [(2, 2), (9, 5), (4, 10)]:
        vorm = [(1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1), (0, 2), (1, 2), (2, 2), (3, 2), (1, 3), (2, 3)]
        for dx, dy in vorm:
            zet(t, x + dx, y + dy, (246, 186, 222))
        for dx, dy in [(1, 0), (0, 1), (1, 1)]:
            zet(t, x + dx, y + dy, (255, 246, 252))
        for dx, dy in [(3, 2), (2, 3), (3, 1)]:
            zet(t, x + dx, y + dy, (206, 120, 176))
    return t


# ---------------------------------------------------------------------------------------------------------------------
# the cave biomes
# ---------------------------------------------------------------------------------------------------------------------
def lichtmos(seed):
    r = rng(seed)
    t = ruis(vlak((214, 96, 160)), 7, seed + 1, 2)
    t = ruis(t, 5, seed + 2)
    for _ in range(9):                                   # little glowing tips
        x, y = r.integers(0, 16, 2)
        zet(t, x, y, (255, 214, 238))
    for _ in range(6):
        x, y = r.integers(0, 16, 2)
        zet(t, x, y, (176, 64, 128))
    return t


def rank(bessen):
    t = leeg()
    steel, licht, donker = (232, 112, 182), (252, 170, 214), (190, 76, 146)
    for (x0, fase) in ((5, 0.0), (10, 2.1)):
        for y in range(16):
            x = int(round(x0 + 1.4 * np.sin(y / 2.4 + fase)))
            zet(t, x, y, steel)
            if y % 4 == 1:
                zet(t, x + 1, y, licht)           # a leaf
            if y % 4 == 3:
                zet(t, x - 1, y, donker)
            if y % 5 == 2:
                zet(t, x + 1, y - 1, (255, 250, 255))   # a glint
    if bessen:
        for (x, y) in [(6, 12), (11, 9), (4, 6), (12, 14)]:
            zet(t, x, y, (255, 206, 110))
            zet(t, x + 1, y, (246, 168, 120))
            zet(t, x, y + 1, (240, 150, 130))
            zet(t, x + 1, y + 1, (226, 120, 120))
            zet(t, x, y, (255, 240, 190))
    return t


def glinsterpaddenstoel():
    t = leeg()
    hoed, licht, steel = (196, 156, 240), (232, 210, 255), (246, 236, 242)
    for (cx, h, b) in ((5, 7, 2), (11, 5, 2), (8, 3, 1)):
        for y in range(16 - h, 16):
            zet(t, cx, y, steel)
        top = 16 - h - 2
        for dx in range(-b, b + 1):
            zet(t, cx + dx, top + 1, hoed)
        for dx in range(-b + 1, b):
            zet(t, cx + dx, top, hoed)
        zet(t, cx - b + 1, top, licht)
        zet(t, cx, top + 1, (250, 244, 255))          # a glowing dot
    return t


def pluisbolletje():
    t = leeg()
    r = rng(1861)
    for y in range(9, 16):
        zet(t, 8, y, (150, 196, 128))
    zet(t, 7, 13, (130, 178, 110))
    for y in range(1, 11):
        for x in range(2, 15):
            d = np.hypot(x - 8, y - 5.5)
            if d < 5.2 and r.random() < 1.05 - d * 0.12:
                v = 255 - r.integers(0, 22)
                zet(t, x, y, (v, v - r.integers(0, 10), v - r.integers(0, 6)))
    zet(t, 8, 5, (236, 226, 232))
    return t


def pluiskussen():
    t = ruis(vlak((250, 226, 238)), 3, 1871, 2)
    rand = (234, 186, 210)
    t[0, :, :3] = rand
    t[15, :, :3] = rand
    t[:, 0, :3] = rand
    t[:, 15, :3] = rand
    for i in range(1, 15):                                # puffy: lighter towards the middle
        for j in range(1, 15):
            d = max(abs(i - 7.5), abs(j - 7.5))
            t[i, j, :3] += (7.5 - d) * 1.4
    for (x, y) in [(7, 7), (8, 8), (7, 8), (8, 7)]:      # the button in the middle
        zet(t, x, y, (226, 164, 196))
    zet(t, 7, 7, (244, 206, 224))
    return t


# ---------------------------------------------------------------------------------------------------------------------
# items
# ---------------------------------------------------------------------------------------------------------------------
def vahoegiet_item():
    t = leeg()
    vorm = {
        2: range(6, 10), 3: range(4, 12), 4: range(3, 13), 5: range(2, 14), 6: range(2, 14), 7: range(3, 13),
        8: range(4, 12), 9: range(5, 11), 10: range(6, 10), 11: range(7, 9)}
    for y, xs in vorm.items():
        for x in xs:
            f = (x - 2) / 12
            zet(t, x, y, mix((252, 206, 232), (214, 116, 180), f * 0.8 + (y - 2) / 30))
    for (x, y) in [(5, 4), (6, 4), (4, 5), (5, 5)]:
        zet(t, x, y, (255, 252, 255))
    for y in range(3, 11):
        zet(t, 8, y, mix(tuple(t[y, 8, :3]), (255, 255, 255), 0.25))
    # outline
    lijn = (150, 70, 126)
    for y, xs in vorm.items():
        xs = list(xs)
        zet(t, xs[0] - 1, y, lijn)
        zet(t, xs[-1] + 1, y, lijn)
    for x in range(6, 10):
        zet(t, x, 1, lijn)
    for x in (7, 8):
        zet(t, x, 12, lijn)
    return t


def glitterbes_item():
    t = leeg()
    for y in range(4, 14):
        for x in range(3, 13):
            d = np.hypot(x - 7.5, y - 8.5)
            if d < 4.8:
                zet(t, x, y, mix((255, 196, 120), (232, 110, 150), d / 5))
            elif d < 5.6:
                zet(t, x, y, (170, 70, 110))
    for (x, y) in [(6, 6), (7, 6), (6, 7)]:
        zet(t, x, y, (255, 246, 220))
    for (x, y) in [(10, 10), (5, 11), (11, 7)]:
        zet(t, x, y, (255, 255, 255))
    for (x, y) in [(8, 2), (8, 3), (9, 1), (7, 3)]:      # the little stalk
        zet(t, x, y, (190, 90, 150))
    return t


# ---------------------------------------------------------------------------------------------------------------------
def alle(a):
    """{(kind, name): array} for every texture."""
    blok = os.path.join(a, "textures", "block")
    out = {}
    v = viltsteen()
    out[("block", "viltsteen")] = v
    out[("block", "gepolijste_viltsteen")] = gepolijst()
    out[("block", "viltsteen_bakstenen")] = bakstenen()
    out[("block", "viltsteen_tegels")] = tegels()
    out[("block", "gebeitelde_viltsteen")] = gebeiteld()
    out[("block", "knuffelnaad")] = knuffelnaad()
    out[("block", "guhklei")] = guhklei()
    stenen = {"vilt": v, "knuffel": arr(Image.open(os.path.join(blok, "knuffelsteen.png"))),
              "kristal": arr(Image.open(os.path.join(blok, "guh_kristalsteen.png")))}
    patronen = {s: vanilla_ertspatroon(n) for s, n in VANILLA.items()}
    for soort in SOORTEN:
        for laag in LAGEN:
            steen = stenen[laag]
            if soort in patronen:
                t = op_steen(steen, *patronen[soort])
            elif soort == "vadserts":
                t = vads(steen)
            elif soort == "kaasknabbelerts":
                t = kaasknabbel(steen, a)
            elif soort == "kaaskorrelerts":
                t = kaaskorrel(steen)
            else:
                t = pigment(steen)
            out[("block", f"{soort}_{laag}")] = t
    out[("block", "vahoegiet_erts")] = vahoegiet_erts(stenen["kristal"])
    out[("block", "lichtmosblok")] = lichtmos(1841)
    out[("block", "lichtmos")] = lichtmos(1845)
    out[("block", "glitterrank")] = rank(False)
    out[("block", "glitterrank_bes")] = rank(True)
    out[("block", "glinsterpaddenstoel")] = glinsterpaddenstoel()
    out[("block", "pluisbolletje")] = pluisbolletje()
    out[("block", "pluiskussen")] = pluiskussen()
    out[("item", "vahoegiet")] = vahoegiet_item()
    out[("item", "glitterbes")] = glitterbes_item()
    return out


def textures(h):
    for (soort, naam), t in alle(h.A).items():
        p = os.path.join(h.A, "textures", soort, f"{naam}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img(t).save(p)


def _vel(texs, pad):
    items = list(texs.items())
    kol, cel = 10, 118
    rijen = (len(items) + kol - 1) // kol
    vel = Image.new("RGBA", (kol * cel, rijen * (cel + 4)), (250, 244, 248, 255))
    d = ImageDraw.Draw(vel)
    for i, ((_, naam), t) in enumerate(items):
        x, y = (i % kol) * cel, (i // kol) * (cel + 4)
        # a checker under it, so transparent plants read
        for cy in range(0, 96, 12):
            for cx in range(0, 96, 12):
                d.rectangle([x + 10 + cx, y + 4 + cy, x + 21 + cx, y + 15 + cy],
                            fill=(214, 214, 214) if (cx + cy) // 12 % 2 else (236, 236, 236))
        im = img(t).resize((96, 96), Image.NEAREST)
        vel.paste(im, (x + 10, y + 4), im)
        d.text((x + 6, y + 102), naam[:19], fill=(90, 40, 70))
    vel.save(pad)


if __name__ == "__main__":
    class _H:
        A = A_STANDAARD
    textures(_H)
    _vel(alle(A_STANDAARD), os.path.join("..", "onderwol_tex_vel.png"))
    print("onderwol textures written;", os.path.abspath(os.path.join("..", "onderwol_tex_vel.png")), file=sys.stderr)
