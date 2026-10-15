"""
bbq2 (tech-quests): De Grote Knabbelmachine as models (and the statuette, the bowl it drops its knabbel in).

The machine is a giant guh of copper and pink enamel, seven blocks wide and deep and nine high: a boiler belly on an iron
foundation, a window with knabbels in it, shoulders with two smokestacks and a funnel, a head with ears, two eyes, a snoet
and a jaw that really gnaws. Every player builds their OWN one (the questline "knabbelmachine"), so it is not made of blocks:
the client draws it on the kern block guhs:grote_knabbelmachine (its mouth, the bowl) for the stage the local player is at
(feature/techquest/client/KnabbelmachineRenderer).

A block model may only reach from -16 to 32, so the machine is cut into CELLS of 48 px (three blocks): every part (PARTS,
in the order the renderer knows them) gets one model per cell it touches, `techquest_km_<part>_<i>_<j>_<k>`, and the helper
block guhs:techquest_knabbelmachine_deel[nr] maps nr = part * 36 + i * 12 + j * 3 + k to it (i 0..2 = x, j 0..3 = y,
k 0..2 = z; a cell nothing touches is an empty model). The renderer draws cell (i, j, k) three blocks further per step.
Machine space: pixels, x 0..16 / z 0..16 / y 0..16 is the kern block, front = north (-z), the body lies behind the kern.
Textures are 64 x 64 with a 16 px motif (one texel per pixel of the machine, seamless from cell to cell).
"""
import math

import numpy as np
from PIL import Image

PARTS = ["fundering", "ketel", "maag", "snoet", "ogen_dicht", "ogen_open", "kaak", "steiger", "knabbel"]
# the stage (the step of the questline "knabbelmachine", 0..6) from which and up to which a part shows
FASEN = {"fundering": (2, 6), "ketel": (3, 6), "maag": (4, 6), "snoet": (5, 6), "ogen_dicht": (5, 5), "ogen_open": (6, 6),
         "kaak": (5, 6), "steiger": (1, 5), "knabbel": (6, 6)}
CEL = 48
NI, NJ, NK = 3, 4, 3                      # cells in x, y, z; cell (1, 0, 0) holds the kern
X_MIN, Y_MIN, Z_MIN = -16 - CEL, -16, -16  # the corner of cell (0, 0, 0) in machine space
SCHAAL_BEELDJE = 0.082

K, KD, IJ, RZ, RD = "koper", "koper_donker", "ijzer", "roze", "roze_donker"
GL, GLOEI, BEK, TAND, HOUT, KN = "glas", "gloei", "bek", "tand", "hout", "knabbel"
# face textures (16 x 16, on the front of a thin box)
OOG_DICHT, OOG_OPEN, NEUS, BLOS = "oog_dicht", "oog_open", "neus", "blos"


def _spiegel(dozen):
    """The same boxes mirrored in x around the middle of the kern (x = 8)."""
    out = []
    for d in dozen:
        out.append(d)
        out.append((16 - d[3], d[1], d[2], 16 - d[0], d[4], d[5]) + tuple(d[6:]))
    return out


# (x0, y0, z0, x1, y1, z1, texture[, front texture]) in machine pixels
DOZEN = {
    "fundering": [(-40, 0, 16, 56, 6, 106, IJ), (-34, 6, 22, 50, 10, 100, KD)]
                 + _spiegel([(-46, 0, 16, -34, 8, 28, IJ), (-46, 0, 94, -34, 8, 106, IJ), (-44, 8, 18, -36, 10, 26, K),
                             (-44, 8, 96, -36, 10, 104, K)]),
    "ketel": [(-34, 10, 24, 50, 22, 98, K), (-40, 22, 20, 56, 62, 104, K), (-34, 62, 24, 50, 74, 98, K),
              (-41, 30, 19, 57, 34, 105, IJ), (-41, 52, 19, 57, 56, 105, IJ),          # two iron bands
              (-14, 26, 18, 30, 60, 20, IJ), (-11, 29, 17, 27, 57, 18.5, GL)]          # the window with knabbels behind it
             + _spiegel([(-48, 12, 52, -40, 70, 60, KD), (-48, 62, 52, -34, 70, 60, KD),   # a fat pipe up each side
                         (-52, 24, 70, -40, 56, 84, GL), (-53, 22, 69, -39, 25, 85, IJ), (-53, 55, 69, -39, 58, 85, IJ)]),   # a sauce tank
    "maag": [(-28, 74, 30, 44, 86, 94, KD),                                              # the shoulders
             (-6, 86, 70, 22, 100, 96, IJ), (-12, 100, 64, 28, 106, 102, IJ), (-8, 106, 68, 24, 108, 98, BEK)]   # the funnel
            + _spiegel([(-32, 74, 86, -20, 120, 98, IJ), (-34, 120, 84, -18, 124, 100, KD), (-30, 124, 88, -22, 126, 96, BEK)]),   # smokestacks
    "snoet": [(-26, 86, 16, 42, 128, 64, RZ),                                            # the head
              (-10, 84, 9, 26, 92, 16, RZ),                                              # the upper lip
              (-8, 80, 10, -2, 84, 14, TAND), (5, 80, 10, 11, 84, 14, TAND), (18, 80, 10, 24, 84, 14, TAND),
              (-8, 72, 15, 24, 86, 20, BEK),                                             # the dark of its mouth
              (2, 96, 11, 14, 104, 16, RD, NEUS)]
             + _spiegel([(-30, 124, 34, -6, 146, 42, RZ), (-26, 128, 33, -10, 142, 34, RD),   # the ears
                         (-22, 94, 15, -12, 100, 16, RZ, BLOS)]),
    "ogen_dicht": _spiegel([(-16, 104, 15, -2, 116, 16, RZ, OOG_DICHT)]),
    "ogen_open": _spiegel([(-16, 104, 15, -2, 116, 16, RZ, OOG_OPEN)])
                 + [(-8, 32, 16.5, 24, 54, 17.5, GLOEI)],                                # the gloeister heart behind the window
    "kaak": [(-12, 64, 6, 28, 72, 20, RZ), (-9, 72, 8, -3, 76, 12, TAND), (5, 72, 8, 11, 76, 12, TAND), (19, 72, 8, 25, 76, 12, TAND),
             (-14, 62, 16, -10, 74, 22, IJ), (26, 62, 16, 30, 74, 22, IJ)],              # with its two hinges
    "steiger": _spiegel([(-56, 0, 2, -52, 132, 6, HOUT), (-56, 0, 106, -52, 132, 110, HOUT),   # four poles
                         (-58, 44, 0, -50, 47, 112, HOUT), (-58, 92, 0, -50, 95, 112, HOUT)])   # planks along the sides
               + [(-56, 44, 0, 72, 47, 8, HOUT), (-56, 92, 0, 72, 95, 8, HOUT),          # and across the front
                  (-56, 128, 2, 72, 132, 6, HOUT), (-56, 128, 106, 72, 132, 110, HOUT),
                  (30, 0, 0, 33, 47, 3, HOUT), (40, 0, 0, 43, 47, 3, HOUT)]              # a ladder
               + [(30, y, 0.5, 43, y + 2, 2.5, HOUT) for y in range(6, 44, 8)],
    "knabbel": [(5, 13, 5, 11, 17, 11, KN), (6, 17, 6, 10, 18, 10, KN)],
}
# the bowl (the kern block's own model, always there): a foot, a stem, a dish with a rim
KERN = [(2, 0, 2, 14, 4, 14, IJ), (5, 4, 5, 11, 10, 11, KD), (1, 10, 1, 15, 13, 15, K), (0, 13, 0, 16, 15, 2, K), (0, 13, 14, 16, 15, 16, K),
        (0, 13, 2, 2, 15, 14, K), (14, 13, 2, 16, 15, 14, K)]
STENEN_VOET = [(2, 0, 2, 14, 2, 14, "steen"), (3, 2, 3, 13, 3, 13, "steen")]


# =====================================================================================================================
# textures
# =====================================================================================================================
def _tegel(motief, seed, var=4.0):
    """A 64 x 64 texture: the 16 x 16 motif four by four, with a little noise that also repeats every 16 px."""
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.float32)
    a[..., 3] = 255
    motief(a)
    a[..., :3] += rng.normal(0, var, (16, 16, 1))
    return Image.fromarray(np.clip(np.tile(a, (4, 4, 1)), 0, 255).astype(np.uint8), "RGBA")


def _platen(basis, licht, donker, klinknagel=None):
    def f(a):
        a[..., :3] = basis
        a[0, :, :3] = licht
        a[:, 0, :3] = licht
        a[15, :, :3] = donker
        a[:, 15, :3] = donker
        if klinknagel:
            for (y, x) in ((2, 2), (2, 13), (13, 2), (13, 13)):
                a[y, x, :3] = klinknagel
                a[y + 1, x, :3] = donker
    return f


def _vlak(basis):
    def f(a):
        a[..., :3] = basis
    return f


def _glas(a):
    """Knabbels behind glass: cheese-yellow lumps in the dark, a glint across."""
    a[..., :3] = (58, 40, 30)
    for (y, x, c) in ((2, 3, (250, 204, 72)), (3, 10, (242, 180, 50)), (7, 6, (255, 220, 110)), (8, 13, (240, 190, 60)), (11, 1, (246, 196, 66)),
                      (12, 9, (252, 210, 90)), (5, 0, (236, 172, 48)), (14, 5, (248, 200, 70)), (0, 14, (244, 186, 56)), (10, 4, (228, 160, 44))):
        for dy in range(3):
            for dx in range(3):
                a[(y + dy) % 16, (x + dx) % 16, :3] = c if dy < 2 else tuple(int(v * 0.8) for v in c)
    for i in range(16):
        a[i, (20 - i) % 16, :3] = np.minimum(255, a[i, (20 - i) % 16, :3] + 70)


def _gloei(a):
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5) / 10.0
    for c, (hi, lo) in enumerate(((255, 255), (250, 150), (190, 40))):
        a[..., c] = hi + (lo - hi) * np.clip(d, 0, 1)
    a[7:9, :, :3] = (255, 255, 235)
    a[:, 7:9, :3] = (255, 255, 235)


def _hout(a):
    a[..., :3] = (128, 94, 56)
    for y in (3, 8, 12):
        a[y, :, :3] = (98, 70, 40)
    a[:, 5, :3] = (112, 82, 48)


def _knabbel(a):
    a[..., :3] = (255, 214, 92)
    a[::5, ::3, :3] = (236, 176, 52)
    a[2::5, 1::4, :3] = (255, 240, 170)


def _gezicht(rows, pal, basis):
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = basis
    a[..., 3] = 255
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                a[y, x, :3] = pal[ch]
    return Image.fromarray(a, "RGBA")


ROZE = (238, 150, 186)
GEZICHT_PAL = {"o": (58, 28, 60), "w": (255, 255, 255), "g": (255, 226, 120), "n": (226, 98, 150), "d": (190, 70, 120), "b": (255, 120, 168)}
GEZICHTEN = {
    OOG_DICHT: ["................", "................", "................", "................", "................", "................",
                "................", ".oo..........oo.", "..oooooooooooo..", "....oooooooo....", "................", "................",
                "................", "................", "................", "................"],
    OOG_OPEN: ["................", "....oooooooo....", "..oooooooooooo..", ".oooowwooooooo..", ".ooowwwwoooooo..", ".ooowwwwoooooo..",
               ".oooowwoooooooo.", ".oooooooooggooo.", ".ooooooooogggoo.", ".oooooooooggooo.", ".oooooooooooooo.", "..oooooooooooo..",
               "...oooooooooo...", "....oooooooo....", "................", "................"],
    NEUS: ["nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn", "nndddnnnnnndddnn", "nndddnnnnnndddnn", "nndddnnnnnndddnn", "nnnnnnnnnnnnnnnn",
           "nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn",
           "nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn", "nnnnnnnnnnnnnnnn"],
    BLOS: ["b" * 16] * 16,
}


def textures(h):
    plat = {K: _platen((196, 118, 72), (226, 152, 100), (150, 84, 52), (240, 190, 130)),
            KD: _platen((150, 88, 58), (176, 110, 74), (112, 62, 42), (200, 140, 96)),
            IJ: _platen((74, 70, 78), (100, 96, 106), (48, 46, 54), (140, 136, 146)),
            RZ: _platen(ROZE, (250, 182, 210), (206, 112, 152)),
            RD: _vlak((214, 110, 156)), GL: _glas, GLOEI: _gloei, BEK: _vlak((54, 20, 34)), TAND: _vlak((250, 246, 236)),
            HOUT: _hout, KN: _knabbel}
    for i, (naam, motief) in enumerate(plat.items()):
        h.save(_tegel(motief, 30820 + i, 0.0 if naam in (GL, GLOEI) else 3.5), "block", f"techquest_km_{naam}.png")
    for naam, rows in GEZICHTEN.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), naam
        h.save(_gezicht(rows, GEZICHT_PAL, ROZE), "block", f"techquest_km_{naam}.png")


def _tex():
    t = {n: f"guhs:block/techquest_km_{n}" for n in (K, KD, IJ, RZ, RD, GL, GLOEI, BEK, TAND, HOUT, KN, OOG_DICHT, OOG_OPEN, NEUS, BLOS)}
    t["particle"] = f"guhs:block/techquest_km_{K}"
    return t


# =====================================================================================================================
# models
# =====================================================================================================================
def _element(d, o=(0.0, 0.0, 0.0), klein=None):
    """A model element of box d, moved by -o (the corner of its cell). uv: one texel per pixel of a 64 x 64 texture, by
    position (seamless over the cells). klein = (factor, (mx, mz), lift): shrunk for the statuette."""
    x0, y0, z0, x1, y1, z1, tex = d[:7]
    voor = d[7] if len(d) > 7 else None
    if klein:
        f, (mx, mz), lift = klein
        x0, x1 = 8 + (x0 - mx) * f, 8 + (x1 - mx) * f
        z0, z1 = 8 + (z0 - mz) * f, 8 + (z1 - mz) * f
        y0, y1 = lift + y0 * f, lift + y1 * f
    else:
        x0, y0, z0, x1, y1, z1 = x0 - o[0], y0 - o[1], z0 - o[2], x1 - o[0], y1 - o[1], z1 - o[2]

    def uv(a0, a1, b0, b1):
        if klein:
            w, hh = max(0.5, min(16.0, (a1 - a0) / klein[0] / 4)), max(0.5, min(16.0, (b1 - b0) / klein[0] / 4))
            return [0, 0, round(w, 3), round(hh, 3)]
        return [round((a0 + 16) / 4, 3), round((b0 + 16) / 4, 3), round((a1 + 16) / 4, 3), round((b1 + 16) / 4, 3)]
    ref = f"#{tex}"
    faces = {"north": {"uv": uv(x0, x1, y0, y1), "texture": ref}, "south": {"uv": uv(x0, x1, y0, y1), "texture": ref},
             "west": {"uv": uv(z0, z1, y0, y1), "texture": ref}, "east": {"uv": uv(z0, z1, y0, y1), "texture": ref},
             "up": {"uv": uv(x0, x1, z0, z1), "texture": ref}, "down": {"uv": uv(x0, x1, z0, z1), "texture": ref}}
    if voor:
        faces["north"] = {"uv": [0, 0, 16, 16], "texture": f"#{voor}"}
    r = [round(v, 3) for v in (x0, y0, z0, x1, y1, z1)]
    return {"from": r[:3], "to": r[3:], "faces": faces}


def cellen(deel):
    """{(i, j, k): [boxes clipped to that cell]} for a part."""
    out = {}
    for d in DOZEN[deel]:
        for i in range(NI):
            for j in range(NJ):
                for k in range(NK):
                    cx, cy, cz = X_MIN + i * CEL, Y_MIN + j * CEL, Z_MIN + k * CEL
                    x0, y0, z0 = max(d[0], cx), max(d[1], cy), max(d[2], cz)
                    x1, y1, z1 = min(d[3], cx + CEL), min(d[4], cy + CEL), min(d[5], cz + CEL)
                    if x0 < x1 and y0 < y1 and z0 < z1:
                        voor = d[7:] if len(d) > 7 and z0 == d[2] else ()
                        out.setdefault((i, j, k), []).append((x0, y0, z0, x1, y1, z1, d[6]) + tuple(voor))
    return out


def nummer(deel, i, j, k):
    return PARTS.index(deel) * NI * NJ * NK + i * NJ * NK + j * NK + k


def modellen(h):
    A, w = h.A, h.w
    tex = _tex()
    leeg = {"textures": {"particle": tex["particle"]}, "elements": []}
    w(f"{A}/models/block/techquest_km_leeg.json", leeg)
    varianten = {f"nr={n}": {"model": "guhs:block/techquest_km_leeg"} for n in range(len(PARTS) * NI * NJ * NK)}
    for deel in PARTS:
        for (i, j, k), dozen in cellen(deel).items():
            o = (X_MIN + i * CEL + 16, Y_MIN + j * CEL + 16, Z_MIN + k * CEL + 16)      # (the block the cell is drawn at)
            naam = f"techquest_km_{deel}_{i}_{j}_{k}"
            el = [_element(d, o) for d in dozen]
            for e in el:
                assert min(e["from"]) >= -16 and max(e["to"]) <= 32, (naam, e["from"], e["to"])
            w(f"{A}/models/block/{naam}.json", {"textures": tex, "elements": el})
            varianten[f"nr={nummer(deel, i, j, k)}"] = {"model": f"guhs:block/{naam}"}
    w(f"{A}/blockstates/techquest_knabbelmachine_deel.json", {"variants": varianten})
    # the kern block itself: the bowl (the same in every direction; the machine is turned by the renderer)
    w(f"{A}/models/block/grote_knabbelmachine.json", {"parent": "minecraft:block/block", "textures": tex, "elements": [_element(d) for d in KERN]})
    w(f"{A}/blockstates/grote_knabbelmachine.json", {"variants": h.facing_states("grote_knabbelmachine")})
    # in the hand: the whole machine, small (the same model as the statuette without its foot)
    klein = (SCHAAL_BEELDJE, (8, 61), 3.0)
    alles = [d for deel in ("fundering", "ketel", "maag", "snoet", "ogen_open", "kaak") for d in DOZEN[deel]]
    machine = [_element(d, klein=klein) for d in alles]
    w(f"{A}/models/item/grote_knabbelmachine.json", {"parent": "minecraft:block/block", "textures": tex, "elements": machine, "display": {
        "gui": {"rotation": [20, 215, 0], "translation": [0, -1.5, 0], "scale": [1.0, 1.0, 1.0]}}})
    # the statuette: the machine on a stone foot
    voet = [_element(d) for d in STENEN_VOET]
    w(f"{A}/models/block/knabbelmachine_beeldje.json", {"parent": "minecraft:block/block",
                                                        "textures": {**tex, "steen": "guhs:block/gepolijst_roosterijzer"},
                                                        "elements": voet + machine})
    w(f"{A}/blockstates/knabbelmachine_beeldje.json", {"variants": h.facing_states("knabbelmachine_beeldje")})
    w(f"{A}/models/item/knabbelmachine_beeldje.json", {"parent": "guhs:block/knabbelmachine_beeldje", "display": {
        "gui": {"rotation": [20, 215, 0], "translation": [0, -1.5, 0], "scale": [1.0, 1.0, 1.0]}}})


def build(h):
    textures(h)
    modellen(h)


def quads(wr, fase, kaak=0.0):
    """(preview) the quads of the whole machine at a stage, for tools/wiki_renders.render: wr = the wiki_renders module."""
    out = wr.model_quads("guhs:block/grote_knabbelmachine")
    for deel in PARTS:
        van, tot = FASEN[deel]
        if not van <= fase <= tot:
            continue
        for (i, j, k) in cellen(deel):
            off = ((i - 1) * 3, j * 3 + (kaak / 16 if deel == "kaak" else 0), k * 3)
            out += wr.model_quads(f"guhs:block/techquest_km_{deel}_{i}_{j}_{k}", offset=off)
    return out
