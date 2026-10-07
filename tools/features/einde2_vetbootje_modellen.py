"""
einde2, slice vetbootje - the looks of the two vehicles: GeckoLib models, animations and textures.

  vetbootje         the player's own little vetschip, in the style of the big one at the Mika-vesting (guheinde_bouw.ship):
                    a greasy orange / yellow / brown hull with a honey rim and vet dripping down its sides, a raised bow with
                    a pink Mika head as figurehead, a dark mast with one square sail, a pennant, a steering wheel, the chest
                    at the stern, a yellow lampion on a pole and two pink cushions for the guhs in front of the mast.
                    Bones: romp, boegbeeld, mast, zeil, wimpel, stuurwiel, kist, deksel, lantaarn.
  heteluchtballon   the small personal balloon: a wicker basket with sandbags, a burner with a snoet (every machine of the
                    Werktuigguh has one) and its flame (bone vlam), four ropes, and a banded envelope in knabbel yellow,
                    pink and cream with a sleepy guh face and two round guh ears on top, plus a little wind vane (bone
                    windvaan) that the renderer turns into the wind.

Textures are painted face by face on an atlas (the idea of boerderij_dieren.Atlas; this one packs by height, knows which
faces carry the boat's colour or its sail picture, and lets a plain face share a small stretched patch). The vetbootje is
drawn in layers, so sixteen colours and six sails cost 22 small files instead of 96 big ones:
  textures/entity/vetbootje.png                    everything, in the default colour (bruin) with the Mika sail
  textures/entity/vetbootje_kleur_<dye>.png        only the coloured parts (sail cloth, pennant, hull stripe), 16 colours
  textures/entity/vetbootje_zeil_<zeil>.png        only the picture on the sail, 6 pictures (Zeil.java, same order)
  textures/entity/vetbootje_gloed.png              the lampion (full bright)
  textures/entity/heteluchtballon.png, heteluchtballon_gloed.png (the flame and the burner's glow)
  textures/gui/einde2_vetbootje_zeil_<zeil>.png    the sail pictures again, small, for the scheepswerf screen

  build(h)       writes all of the above
  check(h)       every bone the Java side moves exists, parents exist, the animations exist, the layers line up
  preview(out)   (python tools/features/einde2_vetbootje_modellen.py <out>) offline renders, a few colours and sails
"""
import math
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import boerderij_dieren as bd, einde2_kit as kit
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import boerderij_dieren as bd, einde2_kit as kit

disc, guh_face, round_ear = bd.disc, bd.guh_face, bd.round_ear

# the sixteen dye colours, in DyeColor order (id 0..15): (name, cloth colour)
KLEUREN = [("white", (236, 238, 240)), ("orange", (240, 128, 32)), ("magenta", (196, 84, 186)), ("light_blue", (92, 176, 226)),
           ("yellow", (248, 206, 62)), ("lime", (126, 196, 44)), ("pink", (244, 148, 180)), ("gray", (84, 90, 96)),
           ("light_gray", (160, 160, 154)), ("cyan", (34, 148, 156)), ("purple", (132, 62, 180)), ("blue", (62, 76, 172)),
           ("brown", (134, 88, 54)), ("green", (96, 126, 34)), ("red", (184, 50, 44)), ("black", (40, 40, 46))]
STANDAARD_KLEUR = 12                                   # bruin, as the sails of the big vetschip (= VetbootjeEntity.STANDAARD_KLEUR)
ZEILEN = ["mika", "knabbel", "streep", "hart", "ster", "kruimel"]     # = Zeil.java, same order

# --- colours ------------------------------------------------------------------------------------------------------------------
ORANJE = (212, 122, 54)             # the hull: orange, yellow and brown terracotta
GEEL = (226, 168, 62)
BRUIN = (124, 78, 50)
BRUIN_DONKER = (84, 52, 36)
HONING = (246, 180, 46)             # the rim and the vet
HONING_LICHT = (255, 228, 130)
MAST = (72, 48, 34)                 # dark oak
MAST_LICHT = (104, 72, 48)
MIKA = (236, 160, 190)              # Mika pink
MIKA_DONKER = (200, 118, 152)
ROOD = (214, 40, 46)
GOUD = (246, 200, 70)
GOUD_DONKER = (190, 136, 34)
IJZER = (96, 96, 108)
KUSSEN = (246, 150, 186)
KUSSEN_LICHT = (255, 208, 226)
LAMPION = (255, 222, 96)
RIET = (206, 160, 96)               # the basket
RIET_DONKER = (150, 104, 58)
TOUW = (180, 150, 110)
ZAK = (196, 176, 140)
KNABBELGEEL = (250, 206, 84)
ROZE = (246, 160, 196)
CREME = (255, 244, 222)
KOPER = (206, 122, 72)

FACES = ("north", "south", "east", "west", "up", "down")
ZIJ = ("north", "south", "east", "west")


# =====================================================================================================================
# the atlas
# =====================================================================================================================
class Atlas:
    """Every cube face gets a patch; the patches are packed by height once the model is complete (bak). A face's tag
    says which layer it also belongs to ("kleur", "zeil", "gloed"; "a+b" = both)."""

    def __init__(self, naam, size, s):
        self.naam, self.size, self.s = naam, size, s
        self.wacht = []
        self.vakken = {}
        self.img = None

    def patch(self, w, h, painter, tag=None, rek=None):
        """A face of w x h model pixels painted by painter(W, H, rng) -> RGBA. rek=(pw, ph): the face shows a smaller
        patch stretched (for plain faces nobody looks at closely)."""
        pw, ph = rek or (w, h)
        pw, ph = max(1, int(math.ceil(pw))), max(1, int(math.ceil(ph)))
        uv = {"uv": [0, 0], "uv_size": [min(w, pw) if rek is None else pw, min(h, ph) if rek is None else ph]}
        self.wacht.append((pw, ph, painter, tag, uv))
        return uv

    def bak(self):
        rng = kit.rng(f"vetbootje/atlas/{self.naam}")
        self.img = np.zeros((self.size * self.s, self.size * self.s, 4), np.uint8)
        x = y = rij = 0
        for pw, ph, painter, tag, uv in sorted(self.wacht, key=lambda p: -p[1]):      # (stable: equal heights keep their order)
            if x + pw > self.size:
                x, y, rij = 0, y + rij, 0
            if y + ph > self.size:
                raise SystemExit(f"einde2_vetbootje_modellen: the atlas of {self.naam} is full ({self.size})")
            W, H = pw * self.s, ph * self.s
            self.img[y * self.s:y * self.s + H, x * self.s:x * self.s + W] = painter(W, H, rng)
            uv["uv"] = [x, y]
            for t in (tag or "").split("+"):
                if t:
                    self.vakken.setdefault(t, []).append((x, y, pw, ph))
            x += pw
            rij = max(rij, ph)
        self.vol = (y + rij) / self.size
        return self.img

    def alleen(self, tag):
        """The baked image with everything but the faces of this tag made see-through."""
        out = np.zeros_like(self.img)
        for (x, y, w, h) in self.vakken.get(tag, []):
            s = self.s
            out[y * s:(y + h) * s, x * s:(x + w) * s] = self.img[y * s:(y + h) * s, x * s:(x + w) * s]
        return out


def face_size(face, size):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


def cube(atlas, origin, size, paint, over=None, faces=FACES, tag=None, tags=None, rek=None, inflate=None):
    """A cube: every face in `faces` gets a patch painted by paint (or over[face]); tags[face] / tag = its layer;
    rek = {face: (pw, ph)} or one (pw, ph) for all = stretched small patches."""
    uv = {}
    for f in faces:
        w, h = face_size(f, size)
        r = rek.get(f) if isinstance(rek, dict) else rek
        uv[f] = atlas.patch(w, h, (over or {}).get(f, paint), (tags or {}).get(f, tag), r)
    c = {"origin": list(origin), "size": list(size), "uv": uv}
    if inflate:
        c["inflate"] = inflate
    return c


def bone(name, parent, pivot, cubes, rotation=None):
    b = {"name": name, "pivot": list(pivot), "cubes": cubes}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = list(rotation)
    return b


def geo(identifier, atlas, bones, width, height):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": atlas.size, "texture_height": atlas.size,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


# =====================================================================================================================
# painters: painter(W, H, rng) -> RGBA uint8 (H, W, 4); S = texels per model pixel of the atlas being painted
# =====================================================================================================================
S = 4


def _vlak(W, H, rng, colour, var=8):
    a = np.zeros((H, W, 4), np.float32)
    a[..., :3] = colour
    a[..., :3] += rng.normal(0, var / 2, (H, W, 1))
    a[..., 3] = 255
    return a


def effen(colour, var=8):
    return lambda W, H, rng: np.clip(_vlak(W, H, rng, colour, var), 0, 255).astype(np.uint8)


def hout(colours, naad=BRUIN_DONKER, langs="u", plank=3.0, vet=False):
    """Planks: seams every `plank` model pixels, running along u ("u": the planks lie flat on a wall) or along v ("v").
    colours: one colour or a list (the planks take turns: the striped deck of the vetschip). vet: honey drips from the top."""
    kleuren = colours if isinstance(colours, list) else [colours]

    def paint(W, H, rng):
        a = _vlak(W, H, rng, kleuren[0], 6)
        n = H if langs == "u" else W
        stap = max(2, int(round(plank * S)))
        for i, p0 in enumerate(range(0, n, stap)):
            c = np.array(kleuren[i % len(kleuren)], np.float32) + rng.normal(0, 5)
            blok = a[p0:p0 + stap, :, :3] if langs == "u" else a[:, p0:p0 + stap, :3]
            blok[...] = c + rng.normal(0, 2.5, blok.shape[:2] + (1,))
            m = blok.shape[1] if langs == "u" else blok.shape[0]
            for _ in range(max(1, m // 14)):                                        # the grain: a few long, soft streaks
                q, lengte = int(rng.integers(0, stap)), int(rng.integers(6, 22))
                r0 = int(rng.integers(0, max(1, m - lengte)))
                if langs == "u":
                    blok[min(q, blok.shape[0] - 1), r0:r0 + lengte] -= 9
                else:
                    blok[r0:r0 + lengte, min(q, blok.shape[1] - 1)] -= 9
            if langs == "u":
                a[p0, :, :3] = np.array(naad) * 0.9 + c * 0.1
                if p0 + 1 < H:
                    a[p0 + 1, :, :3] += 10                                            # a light edge under the seam
            else:
                a[:, p0, :3] = np.array(naad) * 0.9 + c * 0.1
                if p0 + 1 < W:
                    a[:, p0 + 1, :3] += 10
        if vet:
            x = int(rng.integers(2, 9))
            while x < W - 2:
                lengte = int(rng.integers(H // 6, max(H // 6 + 1, int(H * 0.75))))
                breed = int(rng.integers(2, 4))
                for y in range(lengte):
                    b = breed if y < lengte - 2 else max(1, breed - 1)
                    a[y, x:x + b, :3] = HONING if (y + x) % 5 else HONING_LICHT
                a[max(0, lengte - 3):lengte, x, :3] = HONING_LICHT                    # the fat drop at the end, shining
                a[0:max(1, lengte // 3), x, :3] = HONING_LICHT
                x += int(rng.integers(5, 16))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def honing(W, H, rng):
    a = _vlak(W, H, rng, HONING, 6)
    for _ in range(max(1, W * H // 60)):                                             # little shines
        x, y = int(rng.integers(0, W)), int(rng.integers(0, H))
        a[y, x:x + int(rng.integers(1, 4)), :3] = HONING_LICHT
    a[H - 1, :, :3] *= 0.82
    return np.clip(a, 0, 255).astype(np.uint8)


def doek(colour, zoom=True):
    """Sail cloth: soft folds across, a fine weave, a darker stitched hem."""
    def paint(W, H, rng):
        a = _vlak(W, H, rng, colour, 5)
        x = np.arange(W)
        plooi = np.sin(x / max(1, W) * math.pi * 3.0 + 0.6) * 9 + np.sin(x / max(1, W) * math.pi * 7.0) * 3
        a[..., :3] += plooi[None, :, None]
        a[::2, :, :3] -= 3                                                             # the weave
        y = np.arange(H)
        a[..., :3] -= (y / max(1, H) * 10)[:, None, None]                              # a little darker towards the foot
        if zoom and W > 4 * S and H > 4 * S:
            rand = max(1, S // 2)
            for sl in (np.s_[:rand, :], np.s_[-rand:, :], np.s_[:, :rand], np.s_[:, -rand:]):
                a[sl][..., :3] *= 0.72
            for i in range(S, W - S, S):                                               # the stitches
                a[rand + 1, i, :3] = a[H - rand - 2, i, :3] = np.array(CREME) * 0.9
            for j in range(S, H - S, S):
                a[j, rand + 1, :3] = a[j, W - rand - 2, :3] = np.array(CREME) * 0.9
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def _lijn(a, x0, y0, x1, y1, dik, colour):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        disc(a, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, dik, colour)


def _mika_trekken(a, cx, cy, r):
    """A Mika's face around (cx, cy), r = half the width of the head: angry slanted brows, red eyes with a slit,
    a smirk with one white fang (MIKA_FACE of the big vetschip's sail, drawn round)."""
    donker = (30, 18, 34)
    for sx in (-1, 1):
        ex, ey = cx + sx * 0.42 * r, cy - 0.08 * r
        disc(a, ex, ey, 0.25 * r, donker, 1.0, ry=0.21 * r)
        disc(a, ex, ey + 0.02 * r, 0.19 * r, ROOD, 1.0, ry=0.15 * r)
        disc(a, ex, ey + 0.02 * r, 0.05 * r, donker, 1.0, ry=0.13 * r)
        disc(a, ex + 0.08 * r, ey - 0.06 * r, 0.045 * r, (255, 236, 236))
        _lijn(a, ex + sx * 0.3 * r, ey - 0.42 * r, ex - sx * 0.3 * r, ey - 0.16 * r, 0.065 * r, donker)       # the brow
    _lijn(a, cx - 0.4 * r, cy + 0.42 * r, cx + 0.1 * r, cy + 0.5 * r, 0.04 * r, donker)                        # the smirk
    _lijn(a, cx + 0.1 * r, cy + 0.5 * r, cx + 0.42 * r, cy + 0.36 * r, 0.04 * r, donker)
    for i in range(int(0.2 * r) + 1):                                                                           # the fang
        half = 0.085 * r * (1 - i / (0.2 * r + 1))
        y = int(cy + 0.5 * r + i)
        if 0 <= y < a.shape[0]:
            a[y, max(0, int(cx - 0.2 * r - half)):int(cx - 0.2 * r + half) + 1, :3] = (255, 255, 255)
            a[y, max(0, int(cx - 0.2 * r - half)):int(cx - 0.2 * r + half) + 1, 3] = 255
    disc(a, cx, cy + 0.2 * r, 0.06 * r, MIKA_DONKER, 1.0, ry=0.04 * r)                                           # the nose


def mika_snoet(W, H, rng):
    """The figurehead's face: Mika pink fur with the face on it."""
    a = _vlak(W, H, rng, MIKA, 8)
    _mika_trekken(a, W / 2, H * 0.5, W * 0.42)
    for sx in (-1, 1):
        disc(a, W / 2 + sx * 0.34 * W, H * 0.74, 0.07 * W, (255, 130, 170), 0.5, ry=0.04 * W)
    return np.clip(a, 0, 255).astype(np.uint8)


def mika_oor(W, H, rng):
    a = _vlak(W, H, rng, MIKA, 8)
    disc(a, W / 2, H * 0.6, W * 0.3, MIKA_DONKER, 1.0, ry=H * 0.34)
    return np.clip(a, 0, 255).astype(np.uint8)


def kistwand(deksel=False, slot=False):
    def paint(W, H, rng):
        a = hout(BRUIN, BRUIN_DONKER, "u", 2.5)(W, H, rng).astype(np.float32)
        for x0 in (S, W - 2 * S):                                                      # two iron bands
            a[:, x0:x0 + S, :3] = np.array(IJZER) + rng.normal(0, 4, (H, 1, 1))
            a[::S, x0 + S // 2, :3] = (190, 190, 200)                                   # their rivets
        if deksel:                                                                     # a kaasknabbel-yellow heart: this is a guh's chest
            for y in range(H):
                for x in range(W):
                    u, v = (x + 0.5 - W / 2) / (H * 0.36), -(y + 0.5 - H * 0.5) / (H * 0.36)
                    if (u * u + v * v - 1) ** 3 - u * u * v ** 3 <= 0:
                        a[y, x, :3] = GOUD
        if slot:
            disc(a, W / 2, H * 0.3, 1.3 * S, GOUD, 1.0)
            disc(a, W / 2, H * 0.3, 0.45 * S, BRUIN_DONKER, 1.0)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def lampion(W, H, rng):
    a = _vlak(W, H, rng, LAMPION, 5)
    for x in range(0, W, max(2, S)):                                                   # the paper's ribs
        a[:, x, :3] -= 26
    y = np.arange(H)
    a[..., :3] += (np.sin(y / max(1, H) * math.pi) * 22)[:, None, None]
    return np.clip(a, 0, 255).astype(np.uint8)


def stuurwiel(W, H, rng):
    """A ship's wheel on a see-through patch: rim, hub, eight spokes with handles."""
    a = np.zeros((H, W, 4), np.float32)
    cx, cy, r = W / 2, H / 2, min(W, H) / 2
    for k in range(8):
        h_ = math.radians(k * 45)
        _lijn(a, cx, cy, cx + math.cos(h_) * r * 0.97, cy + math.sin(h_) * r * 0.97, 0.07 * r, MAST_LICHT)
        disc(a, cx + math.cos(h_) * r * 0.9, cy + math.sin(h_) * r * 0.9, 0.11 * r, GOUD)
    for y in range(H):
        for x in range(W):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r
            if 0.56 <= d <= 0.74:
                a[y, x, :3] = MAST if d > 0.7 or d < 0.6 else MAST_LICHT
                a[y, x, 3] = 255
    disc(a, cx, cy, 0.2 * r, GOUD_DONKER)
    disc(a, cx, cy, 0.11 * r, GOUD)
    return np.clip(a, 0, 255).astype(np.uint8)


def kussen(W, H, rng):
    a = _vlak(W, H, rng, KUSSEN, 6)
    rand = max(1, S // 2)
    for sl in (np.s_[:rand, :], np.s_[-rand:, :], np.s_[:, :rand], np.s_[:, -rand:]):
        a[sl][..., :3] = KUSSEN_LICHT
    disc(a, W / 2, H / 2, 0.9 * S, KUSSEN_LICHT, 1.0)                                  # the button in the middle
    return np.clip(a, 0, 255).astype(np.uint8)


def knabbel_embleem(W, H, rng):
    """The transom: planks with a golden kaasknabbel ring (the ship's badge)."""
    a = hout([ORANJE, GEEL], BRUIN_DONKER, "u", 2.5)(W, H, rng).astype(np.float32)
    cx, cy, r = W / 2, H / 2, H * 0.36
    for y in range(H):
        for x in range(W):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r
            if d <= 1:
                a[y, x, :3] = GOUD_DONKER if d > 0.82 else GOUD
    for (dx, dy, rr) in ((-0.3, -0.25, 0.2), (0.32, 0.1, 0.24), (-0.12, 0.4, 0.15)):   # cheese holes
        disc(a, cx + dx * r, cy + dy * r, rr * r, GOUD_DONKER)
    return np.clip(a, 0, 255).astype(np.uint8)


def riet(W, H, rng):
    """Wicker: little woven blocks, every other row shifted."""
    a = _vlak(W, H, rng, RIET, 6)
    stap = max(2, S)
    for j, y in enumerate(range(0, H, stap)):
        for i, x in enumerate(range(-stap, W, 2 * stap)):
            x0 = x + (stap if j % 2 else 0)
            a[y:y + stap - 1, max(0, x0):max(0, x0 + stap), :3] = np.array(RIET_DONKER) + rng.normal(0, 5)
        a[min(H - 1, y + stap - 1), :, :3] *= 0.8
    return np.clip(a, 0, 255).astype(np.uint8)


def band(colour, rand=None, stippen=None):
    """A band of the balloon: its colour, a little shading, a trim line at its lower edge, optional dots."""
    def paint(W, H, rng):
        a = _vlak(W, H, rng, colour, 5)
        y = np.arange(H)
        a[..., :3] += (10 - y / max(1, H) * 20)[:, None, None]
        for x in range(0, W, 6 * S):                                                  # the seams between the panels
            a[:, x, :3] -= 16
        if stippen:
            stap = 6 * S
            for i, x in enumerate(range(stap // 2, W, stap)):
                disc(a, x, H / 2 + (S if i % 2 else -S), 1.1 * S, stippen)
        if rand:
            a[H - max(1, S // 2):, :, :3] = rand
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def slaapsnoet(fur):
    """The balloon's face: a guh that dozes while it floats (closed eyes as two little arcs, blush, a snoet)."""
    def paint(W, H, rng):
        a = band(fur)(W, H, rng).astype(np.float32)
        cx, cy, r = W / 2, H * 0.4, H * 0.25
        donker = (40, 28, 48)
        for sx in (-1, 1):
            ex = cx + sx * 1.9 * r
            for t in np.linspace(0, math.pi, 40):                                     # the closed eye: an arc, lashes at its ends
                disc(a, ex + math.cos(t) * r * 0.8, cy + math.sin(t) * r * 0.5, 0.12 * r, donker)
            _lijn(a, ex - sx * -0.8 * r, cy, ex + sx * 1.15 * r, cy - 0.3 * r, 0.08 * r, donker)
            disc(a, ex + sx * 0.3 * r, cy + 1.25 * r, 0.75 * r, (255, 150, 186), 0.55, ry=0.4 * r)
        disc(a, cx, cy + 0.75 * r, 0.3 * r, (214, 96, 150), 1.0, ry=0.22 * r)           # the snoet
        _lijn(a, cx - 0.5 * r, cy + 1.3 * r, cx, cy + 1.5 * r, 0.07 * r, donker)        # a small smile
        _lijn(a, cx, cy + 1.5 * r, cx + 0.5 * r, cy + 1.3 * r, 0.07 * r, donker)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def brandersnoet(W, H, rng):
    """The burner: copper with a tiny guh face (every machine of the Werktuigguh has a snoet)."""
    a = _vlak(W, H, rng, KOPER, 6)
    a[:max(1, S // 2), :, :3] *= 1.2
    for sx in (-1, 1):
        disc(a, W / 2 + sx * W * 0.22, H * 0.42, W * 0.1, (24, 18, 30))
        disc(a, W / 2 + sx * W * 0.22 + W * 0.03, H * 0.42 - W * 0.03, W * 0.035, (255, 255, 255))
    disc(a, W / 2, H * 0.68, W * 0.06, (214, 96, 150))
    return np.clip(a, 0, 255).astype(np.uint8)


def vlam(W, H, rng):
    """A flame on a see-through patch: white-yellow core, orange edge, pointed at the top."""
    a = np.zeros((H, W, 4), np.float32)
    for y in range(H):
        t = 1 - y / H                                                                  # 0 at the foot, 1 at the tip
        half = (W / 2) * (1 - t ** 1.4) * (0.75 + 0.25 * math.sin(t * 9))
        for x in range(W):
            d = abs(x + 0.5 - W / 2)
            if d <= half:
                k = d / max(0.5, half)
                a[y, x, :3] = (255, 250, 210) if k < 0.35 and t < 0.7 else (255, 206, 70) if k < 0.7 else (250, 136, 40)
                a[y, x, 3] = 255
    return a.astype(np.uint8)


def doorzichtig(W, H, rng):
    return np.zeros((H, W, 4), np.uint8)


# --- the sail pictures (on a see-through patch: the cloth's colour shows around them) ------------------------------------------
def _omlijn(a, colour=(40, 26, 40)):
    """A dark outline of one texel around everything that is painted."""
    vol = a[..., 3] > 0
    rand = np.zeros_like(vol)
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        rand |= np.roll(vol, (dy, dx), (0, 1))
    rand &= ~vol
    rand[0, :] = rand[-1, :] = False
    rand[:, 0] = rand[:, -1] = False
    a[rand, :3] = colour
    a[rand, 3] = 255
    return a


def _implicit(W, H, binnen, colour, licht=None):
    a = np.zeros((H, W, 4), np.float32)
    for y in range(H):
        for x in range(W):
            u, v = (x + 0.5 - W / 2) / (W / 2), -(y + 0.5 - H / 2) / (W / 2)
            if binnen(u, v):
                a[y, x, :3] = licht if licht is not None and binnen(u * 1.5 + 0.28, v * 1.5 - 0.42) and u < -0.1 and v > 0.2 else colour
                a[y, x, 3] = 255
    return a


def zeil_mika(W, H, rng):
    a = np.zeros((H, W, 4), np.float32)
    cx, cy, r = W / 2, H * 0.52, W * 0.36
    for sx in (-1, 1):                                                                 # the ears stick out at the top
        disc(a, cx + sx * 0.78 * r, cy - 0.74 * r, 0.34 * r, MIKA, 1.0, ry=0.3 * r)
        disc(a, cx + sx * 0.78 * r, cy - 0.72 * r, 0.18 * r, MIKA_DONKER, 1.0, ry=0.16 * r)
    disc(a, cx, cy, r, MIKA, 1.0, ry=r * 0.92)
    _mika_trekken(a, cx, cy, r * 0.95)
    return _omlijn(a).astype(np.uint8)


def zeil_knabbel(W, H, rng):
    """The bag of kaasknabbels itself (the item's own icon, so it is recognised at a glance), as big as the sail allows."""
    icon = Image.open(os.path.join("src", "main", "resources", "assets", "guhs", "textures", "item", "kaas_knabbels.png")).convert("RGBA")
    k = max(1, int(min(W, H) * 0.86) // 16)
    groot = np.asarray(icon.resize((16 * k, 16 * k), Image.NEAREST)).astype(np.float32)
    a = np.zeros((H, W, 4), np.float32)
    x0, y0 = (W - 16 * k) // 2, (H - 16 * k) // 2
    a[y0:y0 + 16 * k, x0:x0 + 16 * k] = groot
    return _omlijn(a).astype(np.uint8)


def zeil_streep(W, H, rng):
    """Vetstrepen: three broad honey stripes with a shine."""
    a = np.zeros((H, W, 4), np.float32)
    rand = max(2, S)
    for i in range(3):
        y0 = int(H * (0.16 + i * 0.27))
        y1 = y0 + int(H * 0.13)
        a[y0:y1, rand:W - rand, :3] = HONING
        a[y0:y0 + max(1, S // 2), rand:W - rand, :3] = HONING_LICHT
        a[y1 - max(1, S // 2):y1, rand:W - rand, :3] = np.array(HONING) * 0.8
        a[y0:y1, rand:W - rand, 3] = 255
    return a.astype(np.uint8)


def zeil_hart(W, H, rng):
    a = _implicit(W, H, lambda u, v: ((u / 0.7) ** 2 + (v / 0.7 + 0.1) ** 2 - 1) ** 3 - (u / 0.7) ** 2 * (v / 0.7 + 0.1) ** 3 <= 0, (246, 110, 160))
    disc(a, W * 0.36, H * 0.38, W * 0.07, (255, 214, 230), 1.0, ry=W * 0.045)          # its shine
    return _omlijn(a).astype(np.uint8)


def zeil_ster(W, H, rng):
    def ster(u, v):
        hoek = math.atan2(u, v)
        d = math.hypot(u, v)
        k = (hoek / (2 * math.pi / 5)) % 1.0
        straal = 0.34 + (0.78 - 0.34) * abs(2 * k - 1) ** 1.3
        return d <= straal
    a = _implicit(W, H, ster, GOUD)
    disc(a, W / 2, H / 2, W * 0.12, (255, 236, 150))
    return _omlijn(a).astype(np.uint8)


def zeil_kruimel(W, H, rng):
    a = np.zeros((H, W, 4), np.float32)
    r = kit.rng("vetbootje/zeil/kruimel")                                              # (its own rng: the same crumbs on both sides)
    for i in range(15):
        cx, cy = r.uniform(W * 0.14, W * 0.86), r.uniform(H * 0.1, H * 0.9)
        kleur = (KNABBELGEEL, (240, 150, 60), (255, 228, 150))[i % 3]
        for _ in range(3):
            disc(a, cx + r.uniform(-1, 1) * S, cy + r.uniform(-1, 1) * S, r.uniform(0.7, 1.5) * S, kleur)
    return _omlijn(a).astype(np.uint8)


ZEIL_PLAATJE = {"mika": zeil_mika, "knabbel": zeil_knabbel, "streep": zeil_streep, "hart": zeil_hart, "ster": zeil_ster, "kruimel": zeil_kruimel}


def zeildoek(colour, plaatje):
    """One side of the sail: the cloth with the picture on it."""
    def paint(W, H, rng):
        a = doek(colour)(W, H, rng).astype(np.float32)
        p = plaatje(W, H, rng).astype(np.float32)
        al = p[..., 3:4] / 255.0
        a[..., :3] = a[..., :3] * (1 - al) + p[..., :3] * al
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


# =====================================================================================================================
# the vetbootje (front = -z; 16 model pixels = 1 block; the deck lies at y 6)
# =====================================================================================================================
BOOT_ATLAS = (128, 4)
BOOT_BONES = ["root", "romp", "boegbeeld", "mast", "zeil", "wimpel", "stuurwiel", "kist", "deksel", "lantaarn"]


def vetbootje(atlas, kleur=KLEUREN[STANDAARD_KLEUR][1], zeil="mika", alleen_plaatje=False):
    """The model on this atlas, its coloured parts in `kleur`. alleen_plaatje: the sail's two big faces show only the
    picture (the zeil layer)."""
    c = lambda *a, **k: cube(atlas, *a, **k)  # noqa: E731
    wand = hout([ORANJE, GEEL, BRUIN], BRUIN_DONKER, "u", 1.7, vet=True)
    wand_binnen = hout([GEEL, ORANJE], BRUIN_DONKER, "u", 1.7)
    onder = hout(BRUIN, BRUIN_DONKER, "v", 2.5)
    dek = hout([ORANJE, GEEL], BRUIN_DONKER, "v", 2.0)
    donker = effen(BRUIN_DONKER, 6)
    mast = hout(MAST, (44, 28, 20), "v", 1.3)
    klein = (8, 8)
    romp = [
        # the keel and the rudder
        c([-1, -1.5, -22], [2, 2.5, 40], donker, faces=("north", "south", "east", "west", "down"), rek=(12, 3)),
        c([-0.5, -1.5, 18], [1, 10, 4.5], hout(BRUIN, BRUIN_DONKER, "v", 1.5), rek=(5, 10)),
        # the bottom and the belly, narrowing to the bow in three steps
        c([-7, 0, -14], [14, 2, 32], onder, faces=("north", "south", "east", "west", "down")),
        c([-10, 2, -14], [20, 3, 32], wand, over={"down": onder, "north": onder, "south": onder}, faces=("north", "south", "east", "west", "down")),
        c([-8, 2, -19], [16, 3, 5], wand, over={"down": onder}, faces=("north", "east", "west", "down")),
        c([-5.5, 2, -23], [11, 3, 4], wand, over={"down": onder}, faces=("north", "east", "west", "down")),
        c([-3, 2, -26], [6, 3, 3], wand, over={"down": onder}, faces=("north", "east", "west", "down")),
        # the bulwarks
        c([9, 5, -14], [2, 5, 32], wand, over={"west": wand_binnen, "down": donker}, faces=("north", "south", "east", "west", "down"), rek={"down": (2, 8)}),
        c([-11, 5, -14], [2, 5, 32], wand, over={"east": wand_binnen, "down": donker}, faces=("north", "south", "east", "west", "down"), rek={"down": (2, 8)}),
        c([-9, 5, 16], [18, 6, 2], wand_binnen, over={"south": knabbel_embleem}, faces=("north", "south", "east", "west")),
        # the raised bow (a solid little foredeck) in three steps, and the prow
        c([-9, 5, -19], [18, 5, 5], wand, over={"up": dek, "south": wand_binnen}),
        c([-6.5, 5, -23], [13, 5, 4], wand, over={"up": dek}, faces=("north", "east", "west", "up", "down")),
        c([-4, 5, -26], [8, 5, 3], wand, over={"up": dek}, faces=("north", "east", "west", "up", "down")),
        c([-1.5, 4, -29], [3, 5.5, 3], donker, rek=klein),
        # the deck
        c([-9, 5, -14], [18, 1, 30], dek, faces=("up",)),
        # the honey rim on the bulwarks and the stern
        c([8.75, 10, -14], [2.5, 1, 32.25], honing, rek={"north": (3, 1), "south": (3, 1)}),
        c([-11.25, 10, -14], [2.5, 1, 32.25], honing, rek={"north": (3, 1), "south": (3, 1)}),
        c([-9.25, 11, 15.75], [18.5, 1, 2.5], honing, rek={"east": (3, 1), "west": (3, 1)}),
        # the stripe in the boat's colour along both sides
        c([10.95, 6.6, -14], [0.3, 1.8, 32], doek(kleur, False), faces=("east", "north", "south"), tag="kleur", rek={"north": (1, 2), "south": (1, 2)}),
        c([-11.25, 6.6, -14], [0.3, 1.8, 32], doek(kleur, False), faces=("west", "north", "south"), tag="kleur", rek={"north": (1, 2), "south": (1, 2)}),
        # the guhs' cushions in front of the mast
        c([1.6, 6, -13], [6.6, 1.3, 6.6], kussen, faces=("north", "south", "east", "west", "up"), rek={f: (7, 2) for f in ZIJ}),
        c([-8.2, 6, -13], [6.6, 1.3, 6.6], kussen, faces=("north", "south", "east", "west", "up"), rek={f: (7, 2) for f in ZIJ}),
        # the wheel's post and the lampion's pole at the stern
        c([-0.7, 6, 2.6], [1.4, 6, 1.4], mast, faces=ZIJ + ("up",), rek=(2, 6)),
        c([7.6, 11, 16.4], [1, 9.6, 1], mast, faces=ZIJ + ("up",), rek=(1, 10)),
        c([4.6, 19.6, 16.4], [4, 1, 1], mast, rek=(4, 1)),
    ]
    bones = [bone("root", None, [0, 0, 0], []), bone("romp", "root", [0, 0, 0], romp)]
    # the figurehead: a Mika head with its two ears, looking where the boat goes
    vacht = effen(MIKA, 8)
    bones.append(bone("boegbeeld", "romp", [0, 9, -29], [
        c([-3.75, 7.5, -33.5], [7.5, 6.5, 5], vacht, over={"north": mika_snoet}),
        c([2.2, 13.2, -31.6], [3.6, 3, 1], vacht, over={"north": mika_oor}, rek={"east": (1, 3), "west": (1, 3), "up": (4, 1), "down": (4, 1)}),
        c([-5.8, 13.2, -31.6], [3.6, 3, 1], vacht, over={"north": mika_oor}, rek={"east": (1, 3), "west": (1, 3), "up": (4, 1), "down": (4, 1)})]))
    # the mast with its two yards
    bones.append(bone("mast", "romp", [0, 6, -2], [
        c([-1.25, 6, -3.25], [2.5, 36.5, 2.5], mast, faces=ZIJ + ("up",), rek={"up": (3, 3)}),
        c([-2, 6, -4], [4, 1.4, 4], donker, faces=ZIJ + ("up",), rek=(4, 2)),
        c([-10, 37.4, -4.7], [20, 1.6, 1.6], mast, rek={"east": (2, 2), "west": (2, 2)}),
        c([-0.9, 42.5, -2.9], [1.8, 1.8, 1.8], effen(GOUD, 6), rek=(2, 2))]))
    # the sail: the cloth in the boat's colour, the picture on both sides
    plaatje = ZEIL_PLAATJE[zeil]
    groot = plaatje if alleen_plaatje else zeildoek(kleur, plaatje)
    zijkant = doek(kleur, False)
    bones.append(bone("zeil", "mast", [0, 37.4, -3.9], [
        c([-9.5, 14.8, -4.5], [19, 1.3, 1.3], mast, rek={"east": (2, 2), "west": (2, 2)}),        # the boom swings with the sail
        c([-9, 16, -4.2], [18, 21.4, 0.6], zijkant, over={"north": groot, "south": groot},
          tags={"north": "kleur+zeil", "south": "kleur+zeil"}, tag="kleur",
          rek={"east": (1, 8), "west": (1, 8), "up": (8, 1), "down": (8, 1)})]))
    # the pennant on the mast top, flying aft
    bones.append(bone("wimpel", "mast", [0, 40.8, -0.75], [
        c([-0.15, 39.6, -0.75], [0.3, 2.6, 5], doek(kleur, False), faces=("east", "west"), tag="kleur"),
        c([-0.15, 40.2, 4.25], [0.3, 1.4, 3], doek(kleur, False), faces=("east", "west"), tag="kleur")]))
    # the steering wheel (the renderer turns it with the helm)
    bones.append(bone("stuurwiel", "romp", [0, 12.4, 2.3], [
        c([-3.4, 9, 2.0], [6.8, 6.8, 0.5], stuurwiel, faces=("north", "south"))]))
    # the chest at the stern, its lid a bone of its own
    kist = kistwand()
    bones.append(bone("kist", "romp", [0, 6, 12], [
        c([-5, 6, 9.4], [10, 4.4, 6.2], kist, over={"north": kistwand(slot=True)}, faces=ZIJ,
          rek={"east": (6, 5), "west": (6, 5)})]))
    bones.append(bone("deksel", "kist", [0, 10.4, 15.6], [
        c([-5.2, 10.4, 9.2], [10.4, 2, 6.6], kist, over={"up": kistwand(deksel=True)}, faces=ZIJ + ("up",),
          rek={"east": (7, 2), "west": (7, 2)})]))
    # the lampion under the arm of its pole
    kap = effen(MAST, 5)
    bones.append(bone("lantaarn", "romp", [5.6, 19.6, 16.9], [
        c([4.2, 15.4, 15.5], [2.8, 3.6, 2.8], lampion, over={"up": kap, "down": kap}, tags={f: "gloed" for f in ZIJ}, rek={"up": (3, 3), "down": (3, 3)}),
        c([5.2, 19, 16.5], [0.8, 0.6, 0.8], kap, faces=ZIJ, rek=(1, 1))]))
    return geo("vetbootje", atlas, bones, 5, 4)


def vetbootje_anims():
    """zweven: the boat bobs on the air, the sail breathes, the pennant flutters, the lampion swings."""
    kf = lambda pairs: {str(round(t, 3)): v for t, v in pairs}  # noqa: E731
    L = 4.0
    return {"format_version": "1.8.0", "animations": {"animation.vetbootje.zweven": {"loop": True, "animation_length": L, "bones": {
        "root": {"position": kf([(0, [0, 0, 0]), (1, [0, 0.7, 0]), (2, [0, 0, 0]), (3, [0, -0.7, 0]), (4, [0, 0, 0])]),
                 "rotation": kf([(0, [0, 0, -1.2]), (2, [0.8, 0, 1.2]), (4, [0, 0, -1.2])])},
        "zeil": {"rotation": kf([(0, [-4, 0, 0]), (1.3, [-9, 0, 0]), (2.6, [-3, 0, 0]), (4, [-4, 0, 0])])},
        "wimpel": {"rotation": kf([(0, [0, 14, 0]), (0.5, [0, -10, 0]), (1.0, [0, 16, 0]), (1.5, [0, -14, 0]), (2.0, [0, 10, 0]),
                                   (2.5, [0, -16, 0]), (3.0, [0, 12, 0]), (3.5, [0, -8, 0]), (4, [0, 14, 0])])},
        "lantaarn": {"rotation": kf([(0, [0, 0, 5]), (1, [3, 0, -4]), (2, [0, 0, 5]), (3, [-3, 0, -4]), (4, [0, 0, 5])])}}}}}


# =====================================================================================================================
# the heteluchtballon (the basket's floor at y 0)
# =====================================================================================================================
BALLON_ATLAS = (144, 4)
BALLON_BONES = ["root", "mand", "brander", "vlam", "ballon", "ear_left", "ear_right", "windvaan"]
# the envelope from the neck up: (width, y0, y1, colour, trim, dots)
BANDEN = [(14, 30, 34, KOPER, None, None), (26, 34, 39, ROZE, CREME, None), (38, 39, 45, CREME, KNABBELGEEL, None),
          (46, 45, 52, KNABBELGEEL, ROZE, (240, 150, 60)), (50, 52, 70, CREME, KNABBELGEEL, None),
          (46, 70, 77, KNABBELGEEL, ROZE, (240, 150, 60)), (38, 77, 83, CREME, KNABBELGEEL, None), (26, 83, 87, ROZE, None, None),
          (12, 87, 89, CREME, None, None)]


def heteluchtballon(atlas):
    c = lambda *a, **k: cube(atlas, *a, **k)  # noqa: E731
    rand = effen(RIET_DONKER, 6)
    mand = [c([-9, 0, -9], [18, 1.5, 18], riet, over={"up": hout(RIET, RIET_DONKER, "v", 2.5)})]
    for (o, sz) in (([-9, 1.5, -9], [18, 9, 1.5]), ([-9, 1.5, 7.5], [18, 9, 1.5]), ([7.5, 1.5, -7.5], [1.5, 9, 15]), ([-9, 1.5, -7.5], [1.5, 9, 15])):
        mand.append(c(o, sz, riet, over={"up": rand}, faces=ZIJ + ("up",)))
    mand.append(c([-9.4, 9.6, -9.4], [18.8, 1.2, 1.2], rand, rek=(10, 2)))                 # the rolled rim
    mand.append(c([-9.4, 9.6, 8.2], [18.8, 1.2, 1.2], rand, rek=(10, 2)))
    mand.append(c([8.2, 9.6, -8.2], [1.2, 1.2, 16.4], rand, rek=(10, 2)))
    mand.append(c([-9.4, 9.6, -8.2], [1.2, 1.2, 16.4], rand, rek=(10, 2)))
    zak = effen(ZAK, 8)
    for (x, z) in ((9.2, -5), (9.2, 2), (-11.9, -5), (-11.9, 2)):                           # four sandbags on their little ropes
        mand.append(c([x, 3.2, z], [2.7, 3.6, 3], zak, rek=(3, 4)))
        mand.append(c([x + 1.1, 6.8, z + 1.2], [0.5, 3.2, 0.5], effen(TOUW, 6), faces=ZIJ, rek=(1, 3)))
    touw = effen(TOUW, 6)
    for (x, z) in ((7.8, 7.8), (7.8, -8.6), (-8.6, 7.8), (-8.6, -8.6)):                     # the four ropes up to the envelope
        mand.append(c([x, 10.6, z], [0.8, 23.6, 0.8], touw, faces=ZIJ, rek=(1, 12)))
    mand.append(c([8.6, 10.8, 8.6], [0.6, 6, 0.6], effen(MAST, 5), faces=ZIJ + ("up",), rek=(1, 6)))     # the wind vane's little pole
    bones = [bone("root", None, [0, 0, 0], []), bone("mand", "root", [0, 0, 0], mand)]
    koper = effen(KOPER, 6)
    bones.append(bone("brander", "mand", [0, 21.5, 0], [
        c([-8.6, 21.5, -0.5], [17.2, 1, 1], effen(IJZER, 5), rek=(9, 1)),
        c([-0.5, 21.5, -8.6], [1, 1, 17.2], effen(IJZER, 5), rek=(9, 1)),
        c([-2.5, 20, -2.5], [5, 4, 5], koper, over={"north": brandersnoet})]))
    bones.append(bone("vlam", "brander", [0, 24, 0], [
        c([-1.75, 24, 0], [3.5, 6, 0.05], vlam, faces=("north", "south"), tag="gloed"),
        c([0, 24, -1.75], [0.05, 6, 3.5], vlam, faces=("east", "west"), tag="gloed")]))
    ballon = []
    for i, (w, y0, y1, kleur, trim, stip) in enumerate(BANDEN):
        b = band(kleur, trim, stip)
        over = {"up": effen(kleur, 5), "down": effen(tuple(v * 0.86 for v in kleur), 5)}
        if w == 50:
            over["north"] = slaapsnoet(kleur)
        ballon.append(c([-w / 2, y0, -w / 2], [w, y1 - y0, w], b, over=over, rek={"up": (10, 10), "down": (10, 10)}))
    bones.append(bone("ballon", "root", [0, 30, 0], ballon))
    oor, zij = round_ear(ROZE, (236, 128, 168)), effen(ROZE, 6)
    for kant, sx in (("left", 1), ("right", -1)):                                           # two round guh ears on top
        ox = 6 if sx > 0 else -13
        bones.append(bone(f"ear_{kant}", "ballon", [sx * 9.5, 87, 0], [
            c([ox, 86.4, -0.6], [7, 7, 1.2], doorzichtig, over={"north": oor, "south": oor}, faces=("north", "south"))]))
    bones.append(bone("windvaan", "mand", [8.9, 16.6, 8.9], [
        c([8.75, 14.8, 8.9], [0.3, 2.4, 5.5], doek(ROZE, False), faces=("east", "west")),
        c([8.4, 16.8, 8.4], [1, 1, 1], effen(GOUD, 6), rek=(1, 1))]))
    return geo("heteluchtballon", atlas, bones, 5, 7)


def heteluchtballon_anims():
    """zweven: the envelope sways a little above its basket, the ears wiggle now and then."""
    kf = lambda pairs: {str(round(t, 3)): v for t, v in pairs}  # noqa: E731
    L = 6.0
    return {"format_version": "1.8.0", "animations": {"animation.heteluchtballon.zweven": {"loop": True, "animation_length": L, "bones": {
        "ballon": {"rotation": kf([(0, [0, 0, -1.4]), (1.5, [1.0, 0, 0]), (3, [0, 0, 1.4]), (4.5, [-1.0, 0, 0]), (6, [0, 0, -1.4])])},
        "mand": {"rotation": kf([(0, [0, 0, 0.8]), (3, [0, 0, -0.8]), (6, [0, 0, 0.8])])},
        "ear_left": {"rotation": kf([(0, [0, 0, 0]), (4.6, [0, 0, 0]), (4.8, [0, 0, -16]), (5.0, [0, 0, 0])])},
        "ear_right": {"rotation": kf([(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.2, [0, 0, 16]), (2.4, [0, 0, 0])])},
        "vlam": {"scale": kf([(0, [1, 1, 1]), (0.15, [0.9, 1.2, 0.9]), (0.3, [1.1, 0.9, 1.1]), (0.45, [1, 1, 1])])}}}}}


# =====================================================================================================================
def _atlas(naam, maat):
    global S
    S = maat[1]
    return Atlas(naam, maat[0], maat[1])


def boot_lagen():
    """-> (geo, base image, {dye: kleur layer}, {zeil: picture layer}, gloed layer), all RGBA arrays."""
    a = _atlas("vetbootje", BOOT_ATLAS)
    model = vetbootje(a)
    basis = a.bak()
    gloed = a.alleen("gloed")
    kleuren, zeilen = {}, {}
    for naam, rgb in KLEUREN:
        k = _atlas("vetbootje", BOOT_ATLAS)
        vetbootje(k, rgb, alleen_plaatje=True)
        k.bak()
        laag = k.alleen("kleur")
        for (x, y, w, h) in k.vakken["zeil"]:                                               # under the picture: the bare cloth
            s = k.s
            laag[y * s:(y + h) * s, x * s:(x + w) * s] = doek(rgb)(w * s, h * s, kit.rng(f"vetbootje/doek/{naam}/{x}"))
        kleuren[naam] = laag
    for zeil in ZEILEN:
        z = _atlas("vetbootje", BOOT_ATLAS)
        vetbootje(z, zeil=zeil, alleen_plaatje=True)
        z.bak()
        zeilen[zeil] = z.alleen("zeil")
    return model, basis, kleuren, zeilen, gloed, a


def build(h):
    A = h.A
    model, basis, kleuren, zeilen, gloed, _ = boot_lagen()
    h.w(f"{A}/geckolib/models/entity/vetbootje.geo.json", model)
    h.w(f"{A}/geckolib/animations/entity/vetbootje.animation.json", vetbootje_anims())
    h.save(Image.fromarray(basis), "entity", "vetbootje.png")
    h.save(Image.fromarray(gloed), "entity", "vetbootje_gloed.png")
    for naam, laag in kleuren.items():
        h.save(Image.fromarray(laag), "entity", f"vetbootje_kleur_{naam}.png")
    for zeil, laag in zeilen.items():
        h.save(Image.fromarray(laag), "entity", f"vetbootje_zeil_{zeil}.png")
    global S
    S = 2                                                                                    # the screen's small sail pictures (36 x 42)
    for zeil in ZEILEN:
        h.save(Image.fromarray(ZEIL_PLAATJE[zeil](36, 42, kit.rng(f"vetbootje/gui/{zeil}"))), "gui", f"einde2_vetbootje_zeil_{zeil}.png")
    b = _atlas("heteluchtballon", BALLON_ATLAS)
    ballon = heteluchtballon(b)
    plaat = b.bak()                                                                          # (bak fills in the uv of every face)
    h.w(f"{A}/geckolib/models/entity/heteluchtballon.geo.json", ballon)
    h.w(f"{A}/geckolib/animations/entity/heteluchtballon.animation.json", heteluchtballon_anims())
    h.save(Image.fromarray(plaat), "entity", "heteluchtballon.png")
    h.save(Image.fromarray(b.alleen("gloed")), "entity", "heteluchtballon_gloed.png")


def check(h):
    import json
    problems = []
    for name, wanted, anim in (("vetbootje", BOOT_BONES, "animation.vetbootje.zweven"), ("heteluchtballon", BALLON_BONES, "animation.heteluchtballon.zweven")):
        g = json.load(open(f"{h.A}/geckolib/models/entity/{name}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
        for bn in wanted:
            if bn not in names:
                problems.append(f"{name}: no bone {bn}")
        anims = json.load(open(f"{h.A}/geckolib/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
        if anim not in anims:
            problems.append(f"{name}: no animation {anim}")
        for an, a in anims.items():
            for bn in a.get("bones", {}):
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
        tex = Image.open(os.path.join(h.TEX, "entity", f"{name}.png"))
        if tex.size[0] != g["description"]["texture_width"] * (BOOT_ATLAS[1] if name == "vetbootje" else BALLON_ATLAS[1]):
            problems.append(f"{name}: texture size {tex.size} does not fit the model's uv space")
    basis = Image.open(os.path.join(h.TEX, "entity", "vetbootje.png"))
    for naam, _ in KLEUREN:
        laag = Image.open(os.path.join(h.TEX, "entity", f"vetbootje_kleur_{naam}.png"))
        if laag.size != basis.size or np.asarray(laag)[..., 3].max() == 0:
            problems.append(f"vetbootje: the colour layer {naam} is empty or has another size")
    for zeil in ZEILEN:
        laag = Image.open(os.path.join(h.TEX, "entity", f"vetbootje_zeil_{zeil}.png"))
        if laag.size != basis.size or np.asarray(laag)[..., 3].max() == 0:
            problems.append(f"vetbootje: the sail layer {zeil} is empty or has another size")
    return problems


def preview(out):
    """Offline renders into out/: the boat in a few colours and sails (the layers put together as the renderer does),
    and the balloon, from the front-left and from behind."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(os.path.join(out, "textures", "entity"), exist_ok=True)
    model_dir = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    tex_dir = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "entity")
    tiles = []
    echt = wr.ASSETS
    for kleur, zeil in (("brown", "mika"), ("pink", "hart"), ("light_blue", "ster"), ("yellow", "knabbel"), ("black", "streep"), ("lime", "kruimel")):
        img = Image.open(os.path.join(tex_dir, "vetbootje.png")).convert("RGBA")
        img.alpha_composite(Image.open(os.path.join(tex_dir, f"vetbootje_kleur_{kleur}.png")).convert("RGBA"))
        img.alpha_composite(Image.open(os.path.join(tex_dir, f"vetbootje_zeil_{zeil}.png")).convert("RGBA"))
        img.save(os.path.join(out, "textures", "entity", f"boot_{kleur}_{zeil}.png"))
        wr.ASSETS = out
        q = wr.geo_quads(os.path.join(model_dir, "vetbootje.geo.json"), f"guhs:entity/boot_{kleur}_{zeil}")
        for yaw, pitch in ((215, -20), (35, -20)):
            tiles.append(wr.render(q, yaw, pitch, 420, margin=0.05))
    wr.ASSETS = echt
    q = wr.geo_quads(os.path.join(model_dir, "heteluchtballon.geo.json"), "guhs:entity/heteluchtballon")
    for yaw, pitch in ((205, -12), (25, -12), (180, 0), (200, 35)):
        tiles.append(wr.render(q, yaw, pitch, 420, margin=0.05))
    sheet = Image.new("RGBA", (420 * 4, 420 * ((len(tiles) + 3) // 4)), (150, 170, 200, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 4) * 420, (i // 4) * 420))
    sheet.save(os.path.join(out, "vetbootje_sheet.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
