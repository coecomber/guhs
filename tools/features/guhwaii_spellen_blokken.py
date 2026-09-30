"""
Surfen & hula op Guhwai'i (3.0, slice guhwaii-spellen) - the things: the Tiki decorations (textures, block models,
blockstates, loot, tags), the schelpjesmunt and the loaned surfplankje (item icons), the surfplankje entity (GeckoLib model +
texture), the wave texture of the surf game (textures/misc/guhwaiispellen_golf.png) and Tikiguh (the sitting guh behind a big
carved tiki mask on a stick, a grass skirt, a flower lei and a hibiscus behind his ear).
"""
import math
import os

import numpy as np
from PIL import Image

from features import sterrenwacht_hulp as hulp
from features.knuffeldal import stair_states

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

HOUT = (164, 102, 58)
HOUT_DONKER = (84, 48, 28)
BAMBOE = (206, 196, 118)
RIET = (222, 186, 102)
ROZE = (246, 128, 176)
TURKOOIS = (60, 214, 200)
GEEL = (255, 214, 80)
CREME = (252, 240, 214)

TIKI = ["tiki_fakkel", "tiki_masker", "tiki_masker_roze", "tiki_beeld", "tiki_rietdak", "tiki_rietdak_trap", "tiki_rietdak_plaat",
        "tiki_bloemenslinger", "tiki_schelpjeslampion", "tiki_surfplankrek", "tiki_bloemenmat", "tiki_kruk", "tiki_radiootje"]


# =====================================================================================================================
# painting helpers
# =====================================================================================================================
def _rng(seed):
    return np.random.default_rng(seed)


def vlak(kleur, seed, size=16, var=10):
    a = np.zeros((size, size, 4), np.uint8)
    n = _rng(seed).normal(0, var / 2, (size, size, 1))
    a[..., :3] = np.clip(np.array(kleur, np.float32) + n, 0, 255).astype(np.uint8)
    a[..., 3] = 255
    return a


def nerf(kleur, seed, size=16, var=12, vertical=True):
    """Wood / straw with its grain along one direction."""
    rng = _rng(seed)
    a = vlak(kleur, seed, size, 4)
    cols = rng.integers(-var, var + 1, size)
    for i in range(size):
        d = cols[i]
        if vertical:
            a[:, i, :3] = np.clip(a[:, i, :3].astype(int) + d, 0, 255)
        else:
            a[i, :, :3] = np.clip(a[i, :, :3].astype(int) + d, 0, 255)
    return a


def img(a):
    return Image.fromarray(a, "RGBA")


def pixelart(rows, palette, size=16):
    a = np.zeros((size, size, 4), np.uint8)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                c = palette[ch]
                a[y, x] = c + (255,) if len(c) == 3 else c
    return a


def overlay(base, top):
    out = base.copy()
    m = top[..., 3] > 0
    out[m] = top[m]
    return out


def hibiscus(a, cx, cy, r=2, kleur=ROZE, hart=GEEL):
    """A tiny hibiscus (five petals round a yellow heart) painted into a (transparent or not) texture."""
    h, w = a.shape[:2]
    for k in range(5):
        ang = -math.pi / 2 + k * math.pi * 2 / 5
        px, py = cx + math.cos(ang) * r * 0.8, cy + math.sin(ang) * r * 0.8
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                x, y = int(round(px + dx * 0.6)), int(round(py + dy * 0.6))
                if 0 <= x < w and 0 <= y < h:
                    a[y, x] = kleur + (255,)
    if 0 <= cx < w and 0 <= cy < h:
        a[int(cy), int(cx)] = hart + (255,)


# =====================================================================================================================
# textures
# =====================================================================================================================
MASKER = ["..aa........aa..",
          ".abba......abba.",
          ".abbaaaaaaaabba.",
          "..aakkkkkkkkaa..",
          "..akwwwkkwwwka..",
          "..awppwkkwppwa..",
          "..akwwwkkwwwka..",
          "..akkkknnkkkka..",
          "..ackkkkkkkkca..",
          "..ackmmmmmmkca..",
          "..akmtttttmmka..",
          "..akmrrrrrrmka..",
          "..akkmmmmmmkka..",
          "...akkkkkkkka...",
          "....akkkkkka....",
          ".....aaaaaa....."]
BEELD = ["..aa........aa..",
         ".abbaaaaaaaabba.",
         "..akwwwkkwwwka..",
         "..awppwkkwppwa..",
         "..akwwwnnwwwka..",
         "..ackmmmmmmkca..",
         "..akmtttttmmka..",
         "..akkmmmmmmkka..",
         "..aaaaaaaaaaaa..",
         ".akkkkkkkkkkkka.",
         ".aklkkkffkkklka.",
         ".akllkfhffkllka.",
         ".akklkkffkklkka.",
         ".akkkkkkkkkkkka.",
         ".akkakkkkkkakka.",
         ".aaaaaaaaaaaaaa."]


def _masker(basis, donker, extra):
    pal = {"a": donker, "k": basis, "b": extra, "w": CREME, "p": (40, 24, 22), "n": donker, "c": (240, 120, 150),
           "m": donker, "t": (252, 246, 232), "r": (170, 50, 60), "l": donker, "f": extra, "h": GEEL}
    return pal


def textures(h):
    s = lambda a, *p: h.save(img(a), *p)  # noqa: E731
    hout = nerf(HOUT, 3001)
    s(hout, "block", "tiki_hout.png")
    s(nerf(HOUT_DONKER, 3002), "block", "tiki_hout_donker.png")
    # the masks: a carved guh-tiki face (big eyes, a wide grin, pink cheeks, round ears)
    front = overlay(hout, pixelart(MASKER, _masker(HOUT, HOUT_DONKER, (230, 150, 100))))
    s(front, "block", "tiki_masker.png")
    roze_basis = nerf(ROZE, 3003, var=8)
    s(overlay(roze_basis, pixelart(MASKER, _masker(ROZE, (120, 40, 80), TURKOOIS))), "block", "tiki_masker_roze.png")
    s(roze_basis, "block", "tiki_masker_roze_zij.png")
    # the statue: a guh-tiki on a carved body with a flower on its belly
    s(overlay(hout, pixelart(BEELD, _masker(HOUT, HOUT_DONKER, ROZE))), "block", "tiki_beeld.png")
    top = nerf(HOUT, 3004)
    hibiscus(top, 8, 8, 3)
    s(top, "block", "tiki_beeld_top.png")
    # bamboo (vertical stalks with their nodes)
    bam = nerf(BAMBOE, 3005, var=10)
    for y in (3, 11):
        bam[y, :, :3] = np.clip(bam[y, :, :3].astype(int) - 45, 0, 255)
    for x in (4, 8, 12):
        bam[:, x, :3] = np.clip(bam[:, x, :3].astype(int) - 25, 0, 255)
    s(bam, "block", "tiki_bamboe.png")
    # thatch: straw strands in rows, a bit shaggy
    rng = _rng(3006)
    riet = vlak(RIET, 3006, var=6)
    for y in range(16):
        for x in range(16):
            v = ((x + y * 3) % 5 == 0) * -34 + ((y % 4) == 3) * -22 + rng.integers(-10, 11)
            riet[y, x, :3] = np.clip(riet[y, x, :3].astype(int) + v, 0, 255)
    s(riet, "block", "tiki_rietdak.png")
    # the garland (cutout): a green vine with hibiscus and leaves
    sl = np.zeros((16, 16, 4), np.uint8)
    for x in range(16):
        y = int(round(8 + 2 * math.sin(x / 16 * math.pi * 2)))
        sl[y, x] = (70, 150, 70, 255)
        if x % 3 == 0:
            sl[min(15, y + 1), x] = (90, 180, 90, 255)
    for cx, kl in ((3, ROZE), (8, (250, 90, 110)), (13, GEEL)):
        hibiscus(sl, cx, int(round(8 + 2 * math.sin(cx / 16 * math.pi * 2))), 2, kl, GEEL if kl != GEEL else (240, 120, 40))
    s(sl, "block", "tiki_bloemenslinger.png")
    # the shell lantern: pink scallops round a warm glow
    sh = vlak((250, 214, 196), 3007, var=4)
    for y in range(16):
        for x in range(16):
            ring = (math.hypot(x - 7.5, (y - 16) * 0.8) // 3) % 2
            if ring:
                sh[y, x, :3] = ROZE
            if 5 <= x <= 10 and 5 <= y <= 11:
                sh[y, x, :3] = (255, 236, 170)
    s(sh, "block", "tiki_schelpjeslampion.png")
    # the surf boards on the rack (three colours) and the flower mat
    for i, (kleur, streep) in enumerate(((ROZE, TURKOOIS), (TURKOOIS, GEEL), (GEEL, ROZE))):
        b = vlak(kleur, 3010 + i, var=4)
        b[:, 7:9, :3] = streep
        b[0, :, :3] = CREME
        hibiscus(b, 8, 4, 2, CREME, streep)
        s(b, "block", f"tiki_surfplank_{i}.png")
    mat = nerf((214, 176, 96), 3013, var=8, vertical=False)
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0:
                mat[y, x, :3] = np.clip(mat[y, x, :3].astype(int) - 26, 0, 255)
    for (cx, cy, kl) in ((4, 4, ROZE), (11, 5, (250, 90, 110)), (6, 11, GEEL), (12, 12, ROZE), (8, 8, (255, 150, 60))):
        hibiscus(mat, cx, cy, 2, kl, GEEL if kl != GEEL else (240, 120, 40))
    s(mat, "block", "tiki_bloemenmat.png")
    # the stool seat, the radio's shell speaker and dial, the torch flame
    zit = nerf(RIET, 3014, var=8, vertical=False)
    for r in range(1, 8, 2):
        for k in range(24):
            a = k / 24 * math.pi * 2
            x, y = int(round(7.5 + math.cos(a) * r)), int(round(7.5 + math.sin(a) * r))
            zit[y, x, :3] = np.clip(zit[y, x, :3].astype(int) - 30, 0, 255)
    s(zit, "block", "tiki_kruk_top.png")
    radio = nerf(HOUT, 3015)
    for y in range(3, 13):
        for x in range(2, 11):
            d = math.hypot(x - 6, y - 8)
            if d < 4.6:
                ang = math.atan2(y - 8, x - 6)
                radio[y, x, :3] = (250, 206, 196) if int(d * 2 + ang * 1.2) % 2 else ROZE
    radio[4:7, 12:15, :3] = CREME
    radio[5, 13, :3] = (200, 50, 60)
    radio[9:12, 12:15, :3] = (60, 50, 44)
    radio[10, 13, :3] = GEEL
    s(radio, "block", "tiki_radiootje.png")
    vlam = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) / (1 + (15 - y) * 0.35)
            if d < 1 and y > 2:
                c = (255, 240, 150) if d < 0.4 else (255, 170, 40) if d < 0.75 else (240, 90, 30)
                vlam[y, x] = c + (255,)
    s(vlam, "block", "tiki_vlam.png")
    # items: the schelpjesmunt (a pink scallop coin with a cream rim and a tiny guh face) and the loaned surfplankje
    munt = ["................",
            "......rrrr......",
            "....rrssssrr....",
            "...rsspsspssr...",
            "..rsspsssspssr..",
            "..rspsekkespsr..",
            ".rspssekkesspsr.",
            ".rssspsssspsssr.",
            ".rsspsscsscpssr.",
            ".rssspsmmspsssr.",
            "..rsspsssspssr..",
            "..rrsspsspssrr..",
            "....rrssssrr....",
            ".....rrbbrr.....",
            "......rbbr......",
            "................"]
    s(pixelart(munt, {"r": (190, 90, 130), "s": (250, 170, 200), "p": (255, 214, 230), "e": (40, 24, 22), "k": (255, 255, 255),
                      "c": (240, 110, 150), "m": (150, 60, 90), "b": (230, 150, 110)}), "item", "schelpjesmunt.png")
    plank = np.zeros((16, 16, 4), np.uint8)
    for i in range(15):
        for w in range(-2, 3):
            x, y = 1 + i, 14 - i + w
            if 0 <= x < 16 and 0 <= y < 16:
                edge = abs(w) == 2 or i in (0, 14)
                plank[y, x] = ((150, 60, 100) if edge else TURKOOIS if w == 0 else ROZE) + (255,)
    hibiscus(plank, 11, 5, 1, CREME, GEEL)
    s(plank, "item", "surfplankje_leen.png")
    # the wave texture of the surf game (tinted by the vertex colours): soft ripples and flecks of foam, tiling
    size = 64
    wave = np.zeros((size, size, 4), np.uint8)
    rng = _rng(3020)
    for y in range(size):
        for x in range(size):
            v = 205 + 22 * math.sin(x / size * math.pi * 6 + math.sin(y / size * math.pi * 4) * 1.5) \
                + 14 * math.sin(y / size * math.pi * 10 + x / size * math.pi * 2)
            wave[y, x] = (int(min(255, v)), int(min(255, v + 6)), 255, 255)
    for _ in range(90):
        cx, cy = rng.integers(0, size, 2)
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                if rng.random() < 0.7:
                    wave[(cy + dy) % size, (cx + dx) % size] = (255, 255, 255, 255)
    h.save(img(wave), "misc", "guhwaiispellen_golf.png")


# =====================================================================================================================
# block models
# =====================================================================================================================
def el(frm, to, faces, rot=None):
    e = {"from": frm, "to": to, "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def alle(tex, uv=None, skip=()):
    out = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        if f in skip:
            continue
        out[f] = {"texture": tex}
        if uv:
            out[f]["uv"] = uv
    return out


def facing_block(h, name, model, item_model=None):
    h.w(f"{h.A}/blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{model}", **({"y": r} if r else {})}
                                                        for f, r in ROT.items()}})
    h.w(f"{h.A}/models/item/{name}.json", {"parent": f"guhs:block/{item_model or model}"})


def models(h):
    A = h.A
    b = lambda n: f"guhs:block/{n}"  # noqa: E731
    # the torch: a bamboo pole, a coconut-shell bowl, a crossed flame on top
    h.w(f"{A}/models/block/tiki_fakkel.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
                                              "textures": {"particle": b("tiki_bamboe"), "bamboe": b("tiki_bamboe"), "kom": b("tiki_hout_donker"),
                                                           "vlam": b("tiki_vlam")},
                                              "elements": [el([6.5, 0, 6.5], [9.5, 13, 9.5], alle("#bamboe")),
                                                           el([5, 12, 5], [11, 15, 11], alle("#kom")),
                                                           el([5.5, 14.5, 8], [10.5, 21, 8], {"north": {"texture": "#vlam", "uv": [0, 0, 16, 16]}, "south": {"texture": "#vlam", "uv": [0, 0, 16, 16]}},
                                                              {"angle": 45, "axis": "y", "origin": [8, 8, 8]}),
                                                           el([8, 14.5, 5.5], [8, 21, 10.5], {"east": {"texture": "#vlam", "uv": [0, 0, 16, 16]}, "west": {"texture": "#vlam", "uv": [0, 0, 16, 16]}},
                                                              {"angle": 45, "axis": "y", "origin": [8, 8, 8]})]})
    h.w(f"{A}/blockstates/tiki_fakkel.json", {"variants": {"": {"model": b("tiki_fakkel")}}})
    h.item_model("tiki_fakkel", b("tiki_vlam"))
    h.w(f"{A}/models/item/tiki_fakkel.json", {"parent": b("tiki_fakkel")})
    # the masks (on a wall: facing north = against the wall at its south side), with two round ears
    for name, front, zij in (("tiki_masker", "tiki_masker", "tiki_hout"), ("tiki_masker_roze", "tiki_masker_roze", "tiki_masker_roze_zij")):
        h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {"particle": b(zij), "front": b(front), "zij": b(zij)},
                                             "elements": [el([2, 1, 13], [14, 15, 16], {"north": {"texture": "#front", "uv": [2, 1, 14, 15]},
                                                                                         "south": {"texture": "#zij"}, "east": {"texture": "#zij"},
                                                                                         "west": {"texture": "#zij"}, "up": {"texture": "#zij"},
                                                                                         "down": {"texture": "#zij"}}),
                                                          el([1, 13, 14], [4, 16, 16], alle("#zij")), el([12, 13, 14], [15, 16, 16], alle("#zij"))]})
        facing_block(h, name, name)
    # the statue
    h.w(f"{A}/models/block/tiki_beeld.json", {"parent": "minecraft:block/block", "textures": {"particle": b("tiki_hout"), "front": b("tiki_beeld"),
                                                                                           "zij": b("tiki_hout"), "top": b("tiki_beeld_top")},
                                             "elements": [el([3, 0, 3], [13, 16, 13], {"north": {"texture": "#front", "uv": [0, 0, 16, 16]},
                                                                                       "south": {"texture": "#zij"}, "east": {"texture": "#zij"},
                                                                                       "west": {"texture": "#zij"}, "up": {"texture": "#top"},
                                                                                       "down": {"texture": "#zij"}}),
                                                          el([1.5, 14, 5], [3, 16, 9], alle("#zij")), el([13, 14, 5], [14.5, 16, 9], alle("#zij"))]})
    facing_block(h, "tiki_beeld", "tiki_beeld")
    # the thatch: block, stairs, slab
    h.simple_block("tiki_rietdak")
    t = b("tiki_rietdak")
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        h.w(f"{A}/models/block/tiki_rietdak_trap{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": t, "top": t, "side": t}})
    h.w(f"{A}/blockstates/tiki_rietdak_trap.json", {"variants": stair_states(b("tiki_rietdak_trap"))})
    h.w(f"{A}/models/item/tiki_rietdak_trap.json", {"parent": b("tiki_rietdak_trap")})
    h.w(f"{A}/models/block/tiki_rietdak_plaat.json", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    h.w(f"{A}/models/block/tiki_rietdak_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    h.w(f"{A}/blockstates/tiki_rietdak_plaat.json", {"variants": {"type=bottom": {"model": b("tiki_rietdak_plaat")},
                                                                 "type=top": {"model": b("tiki_rietdak_plaat_top")},
                                                                 "type=double": {"model": b("tiki_rietdak")}}})
    h.w(f"{A}/models/item/tiki_rietdak_plaat.json", {"parent": b("tiki_rietdak_plaat")})
    # the garland: a flat strip against the wall
    h.w(f"{A}/models/block/tiki_bloemenslinger.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
                                                      "textures": {"particle": b("tiki_bloemenslinger"), "s": b("tiki_bloemenslinger")},
                                                      "elements": [el([0, 4, 15], [16, 16, 15], {"north": {"texture": "#s", "uv": [0, 2, 16, 14]},
                                                                                               "south": {"texture": "#s", "uv": [16, 2, 0, 14]}})]})
    facing_block(h, "tiki_bloemenslinger", "tiki_bloemenslinger")
    h.item_model("tiki_bloemenslinger", b("tiki_bloemenslinger"))
    # the shell lantern on its string
    h.w(f"{A}/models/block/tiki_schelpjeslampion.json", {"parent": "minecraft:block/block", "textures": {"particle": b("tiki_schelpjeslampion"),
                                                                                                      "s": b("tiki_schelpjeslampion"), "b": b("tiki_bamboe")},
                                                        "elements": [el([5, 2, 5], [11, 12, 11], alle("#s")), el([4, 11, 4], [12, 12.5, 12], alle("#b")),
                                                                     el([7.5, 12.5, 7.5], [8.5, 16, 8.5], alle("#b"))]})
    facing_block(h, "tiki_schelpjeslampion", "tiki_schelpjeslampion")
    # the board rack: two bamboo posts, three boards leaning back
    rek = [el([1, 0, 14], [2.5, 12, 15.5], alle("#b")), el([13.5, 0, 14], [15, 12, 15.5], alle("#b")), el([1, 10, 14], [15, 11.5, 15.5], alle("#b"))]
    for i, x in enumerate((2.5, 6.5, 10.5)):
        rek.append(el([x, 0, 11], [x + 3.2, 16, 12.2], alle(f"#p{i}"), {"angle": -22.5, "axis": "x", "origin": [8, 0, 12]}))
    h.w(f"{A}/models/block/tiki_surfplankrek.json", {"parent": "minecraft:block/block", "textures": {
        "particle": b("tiki_surfplank_0"), "b": b("tiki_bamboe"), "p0": b("tiki_surfplank_0"), "p1": b("tiki_surfplank_1"), "p2": b("tiki_surfplank_2")},
        "elements": rek})
    facing_block(h, "tiki_surfplankrek", "tiki_surfplankrek")
    # the flower mat (a carpet)
    h.w(f"{A}/models/block/tiki_bloemenmat.json", {"parent": "minecraft:block/carpet", "textures": {"wool": b("tiki_bloemenmat")}})
    h.w(f"{A}/blockstates/tiki_bloemenmat.json", {"variants": {"": {"model": b("tiki_bloemenmat")}}})
    h.w(f"{A}/models/item/tiki_bloemenmat.json", {"parent": b("tiki_bloemenmat")})
    # the bar stool: four bamboo legs, a woven seat
    kruk = [el([x, 0, z], [x + 1.5, 9, z + 1.5], alle("#b")) for x in (4.5, 10) for z in (4.5, 10)]
    kruk.append(el([4, 9, 4], [12, 10.5, 12], {"up": {"texture": "#t"}, "down": {"texture": "#b"}, "north": {"texture": "#b"},
                                                 "south": {"texture": "#b"}, "east": {"texture": "#b"}, "west": {"texture": "#b"}}))
    kruk.append(el([4.5, 4, 4.5], [11.5, 5, 11.5], alle("#b", skip=("up",))))
    h.w(f"{A}/models/block/tiki_kruk.json", {"parent": "minecraft:block/block", "textures": {"particle": b("tiki_bamboe"), "b": b("tiki_bamboe"),
                                                                                          "t": b("tiki_kruk_top")}, "elements": kruk})
    facing_block(h, "tiki_kruk", "tiki_kruk")
    # the radio: a box with the shell speaker at the front, a little antenna
    h.w(f"{A}/models/block/tiki_radiootje.json", {"parent": "minecraft:block/block", "textures": {"particle": b("tiki_hout"), "f": b("tiki_radiootje"),
                                                                                               "z": b("tiki_bamboe"), "h": b("tiki_hout_donker")},
                                                 "elements": [el([3, 0, 5], [13, 9, 12], {"north": {"texture": "#f", "uv": [1, 3, 15, 13]},
                                                                                          "south": {"texture": "#z"}, "east": {"texture": "#z"},
                                                                                          "west": {"texture": "#z"}, "up": {"texture": "#h"},
                                                                                          "down": {"texture": "#h"}}),
                                                              el([11, 9, 8], [12, 14, 9], alle("#h")), el([4, 9, 7], [10, 10, 10], alle("#z"))]})
    facing_block(h, "tiki_radiootje", "tiki_radiootje")
    # loot + tags
    for n in TIKI:
        if n == "tiki_rietdak_plaat":
            h.w(f"{h.D}/loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
                "type": "minecraft:item", "name": f"guhs:{n}", "functions": [{"function": "minecraft:set_count", "count": 2, "conditions": [{
                    "condition": "minecraft:block_state_property", "block": f"guhs:{n}", "properties": {"type": "double"}}]},
                    {"function": "minecraft:explosion_decay"}]}]}]})
        else:
            h.self_drop(n)
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{n}" for n in TIKI if n not in ("tiki_rietdak", "tiki_rietdak_trap", "tiki_rietdak_plaat",
                                                                                          "tiki_bloemenslinger", "tiki_bloemenmat")])
    h.add_tag("minecraft/tags/block/mineable/hoe", ["guhs:tiki_rietdak", "guhs:tiki_rietdak_trap", "guhs:tiki_rietdak_plaat"])
    h.add_tag("minecraft/tags/block/stairs", ["guhs:tiki_rietdak_trap"])
    h.add_tag("minecraft/tags/block/slabs", ["guhs:tiki_rietdak_plaat"])
    h.add_tag("minecraft/tags/item/stairs", ["guhs:tiki_rietdak_trap"])
    h.add_tag("minecraft/tags/item/slabs", ["guhs:tiki_rietdak_plaat"])
    h.item_model("schelpjesmunt")
    h.item_model("surfplankje_leen")
    h.add_tag("guhs/tags/item/loaned", ["guhs:surfplankje_leen"])


# =====================================================================================================================
# the surfplankje (entity, GeckoLib)
# =====================================================================================================================
def surfplank(h):
    # texture 64x32: the top (8 x 30 px at (0, 0)), the bottom (8 x 30 at (8, 0)), the rails (1 x 30 at (16, 0) and (17, 0)),
    # the nose/tail bits and the fin use little swatches on the right
    t = np.zeros((32, 64, 4), np.uint8)
    rng = _rng(3030)
    for y in range(30):
        for x in range(8):
            c = ROZE if 1 <= x <= 6 else CREME
            if x in (3, 4):
                c = TURKOOIS
            t[y, x] = tuple(int(np.clip(v + rng.integers(-5, 6), 0, 255)) for v in c) + (255,)
            t[y, 8 + x] = TURKOOIS + (255,)
        t[y, 16] = CREME + (255,)
        t[y, 17] = CREME + (255,)
    # a hibiscus on the tail half, a little guh face on the nose
    for (x, y, c) in ((2, 20, GEEL), (5, 20, ROZE), (3, 19, (250, 90, 110)), (4, 19, (250, 90, 110)), (3, 21, (250, 90, 110)), (4, 21, (250, 90, 110))):
        t[y, x] = c + (255,)
    for (x, y) in ((2, 4), (5, 4)):
        t[y, x] = (40, 24, 22, 255)
    t[6, 3] = (150, 60, 90, 255)
    t[6, 4] = (150, 60, 90, 255)
    t[5, 1] = (240, 120, 150, 255)
    t[5, 6] = (240, 120, 150, 255)
    for (sx, sy, c) in ((20, 0, ROZE), (28, 0, TURKOOIS), (36, 0, CREME)):
        t[sy:sy + 8, sx:sx + 8] = c + (255,)
    h.save(img(t), "entity", "guhwaiispellen_surfplank.png")

    def c(origin, size, top, bottom, side, north=None, south=None):
        f = {"up": {"uv": top[0], "uv_size": top[1]}, "down": {"uv": bottom[0], "uv_size": bottom[1]},
             "east": {"uv": side[0], "uv_size": side[1]}, "west": {"uv": side[0], "uv_size": side[1]},
             "north": {"uv": (north or side)[0], "uv_size": (north or side)[1]}, "south": {"uv": (south or side)[0], "uv_size": (south or side)[1]}}
        return {"origin": origin, "size": size, "uv": f}

    roze = ((20, 0), (8, 8))
    turk = ((28, 0), (8, 8))
    creme = ((36, 0), (8, 8))
    bones = [{"name": "root", "pivot": [0, 0, 0]},
             {"name": "plank", "parent": "root", "pivot": [0, 0, 0], "cubes": [
                 c([-4, 0, -15], [8, 1, 30], ((0, 0), (8, 30)), ((8, 0), (8, 30)), ((16, 0), (1, 30)), creme, creme),
                 c([-3, 0, -17], [6, 1, 2], roze, turk, creme),
                 c([-2, 0, -18.5], [4, 1, 1.5], roze, turk, creme),
                 c([-3, 0, 15], [6, 1, 1.5], roze, turk, creme),
                 c([-0.5, -2.2, 10], [1, 2.2, 3.5], turk, turk, turk)]}]
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.guhwaiispellen_surfplank", "texture_width": 64, "texture_height": 32, "visible_bounds_width": 3,
                        "visible_bounds_height": 1.5, "visible_bounds_offset": [0, 0.3, 0]}, "bones": bones}]}
    h.w(os.path.join(h.A, "geo", "entity", "guhwaiispellen_surfplank.geo.json"), geo)
    h.w(os.path.join(h.A, "animations", "entity", "guhwaiispellen_surfplank.animation.json"), {"format_version": "1.8.0", "animations": {}})


# =====================================================================================================================
# Tikiguh: a sun-browned guh behind a big carved tiki mask on a stick, palm leaves on top, a grass skirt, a flower lei
# =====================================================================================================================
def tikiguh(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_tikiguh")
    sw = hulp.swatches(geo, ["masker", "hout", "blad", "rok", "slinger", "bloem", "stok"])
    cube = hulp.cube

    def mcube(origin, size):
        f = {"uv": list(sw["hout"]), "uv_size": [hulp.SW, hulp.SW]}
        cu = {"origin": origin, "size": size, "uv": {k: dict(f) for k in ("south", "east", "west", "up", "down")}}
        cu["uv"]["north"] = {"uv": list(sw["masker"]), "uv_size": [hulp.SW, hulp.SW]}
        return cu

    geo["bones"].append({"name": "tiki_masker", "parent": "body", "pivot": [0, 12, -8], "cubes": [
        mcube([-6.5, 12.5, -10.2], [13, 14, 1.4]),
        cube([-7.5, 23.5, -9.8], [2.5, 3, 1], sw["hout"]), cube([5, 23.5, -9.8], [2.5, 3, 1], sw["hout"]),
        cube([-0.6, 6.5, -9.6], [1.2, 6.5, 1.2], sw["stok"])]})
    geo["bones"].append({"name": "tiki_bladeren", "parent": "tiki_masker", "pivot": [0, 26, -9], "cubes": [
        cube([-1.2, 26, -9.6], [2.4, 6, 0.6], sw["blad"], rotation=[0, 0, 0], pivot=[0, 26, -9]),
        cube([-1.2, 26, -9.6], [2.4, 5.5, 0.6], sw["blad"], rotation=[0, 0, 32], pivot=[0, 26, -9]),
        cube([-1.2, 26, -9.6], [2.4, 5.5, 0.6], sw["blad"], rotation=[0, 0, -32], pivot=[0, 26, -9]),
        cube([-1.0, 26, -9.6], [2.0, 4.5, 0.6], sw["blad"], rotation=[0, 0, 62], pivot=[0, 26, -9]),
        cube([-1.0, 26, -9.6], [2.0, 4.5, 0.6], sw["blad"], rotation=[0, 0, -62], pivot=[0, 26, -9])]})
    geo["bones"].append({"name": "tiki_rokje", "parent": "body", "pivot": [0, 4, 0], "cubes": [
        cube([-5.4, 0.6, -4.4], [10.8, 6.5, 8.8], sw["rok"], inflate=0.15)]})
    geo["bones"].append({"name": "tiki_slinger", "parent": "body", "pivot": [0, 12, 0], "cubes": [
        cube([-5.8, 11.6, -5.2], [11.6, 1.8, 9.4], sw["slinger"], inflate=0.1)]})
    geo["bones"].append({"name": "tiki_bloem", "parent": "ear_left", "pivot": [8, 25, -1], "cubes": [
        cube([8.5, 24.5, -2.6], [3, 3, 1.2], sw["bloem"])]})
    hulp.save_geo(h, "guh_npc_tikiguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.06, sat=0.48, val=0.95)
    rng = np.random.default_rng(30401)

    def masker(block):
        big = pixelart(MASKER, _masker(HOUT, HOUT_DONKER, (230, 150, 100)))
        base = nerf(HOUT, 3041)
        face = overlay(base, big)
        block[...] = np.asarray(Image.fromarray(face).resize((32, 32), Image.NEAREST))
    hulp.paint_swatch(a, sw["masker"], HOUT, rng, 4, masker)
    hulp.paint_swatch(a, sw["hout"], HOUT, rng, 10)
    hulp.paint_swatch(a, sw["stok"], BAMBOE, rng, 8)
    hulp.paint_swatch(a, sw["blad"], (80, 170, 80), rng, 14)

    def rok(block):
        for x in range(32):
            if x % 3 == 0:
                block[:, x, :3] = np.clip(block[:, x, :3].astype(int) - 40, 0, 255)
    hulp.paint_swatch(a, sw["rok"], (170, 200, 90), rng, 12, rok)

    def slinger(block):
        for y in range(32):
            for x in range(32):
                k = ((x // 6) + (y // 8)) % 3
                block[y, x, :3] = (ROZE, GEEL, (250, 90, 110))[k]
    hulp.paint_swatch(a, sw["slinger"], ROZE, rng, 6, slinger)

    def bloem(block):
        block[..., :3] = (250, 90, 120)
        block[12:20, 12:20, :3] = GEEL
    hulp.paint_swatch(a, sw["bloem"], (250, 90, 120), rng, 4, bloem)
    h.save(Image.fromarray(a), "entity", "npc_tikiguh.png")


def build(h):
    textures(h)
    models(h)
    surfplank(h)
    tikiguh(h)
