"""
Super Guhrio (bbq2; the Java side is feature/guhrio) - the side-view engine's own resources and its test levels.

  blocks      guhrio_grond (painted ground), guhrio_blok (the hard block of stairs), guhrio_startblok (the way into a level),
              guhrio_vraagblok (the ?-block: inhoud munt/superknabbel; the state leeg=true is only its empty look),
              guhrio_steen (brick), guhrio_munt (a coin in the lane), guhrio_vlag (a flag: your spot to come back to),
              guhrio_mast (the flagpole, top=true carries the flag), guhrio_pijp (a pipe's mouth, kanaal 0..15) and
              guhrio_pijp_lijf (its body), guhrio_guhmba_plek (where a Guhmba lives; invisible)
  entity      textures/entity/guhrio_guhmba.png (the boxes of client/GuhmbaRenderer)
  levels      level_json(h, id, banen, ...) writes data/guhs/guhrio_level/<id>.json: the lanes of a level in the level's
              own frame (the start block is 0,0,0, +x is the way it faces, +z its right hand). LevelBouwer below builds a
              level facing east in a Structure and keeps the lane data next to it, so the two cannot drift apart.
  test levels guhrio_testlevel (84 x 28 x 20): about 80 blocks of everything the engine has, with a second lane (a bonus
              room in the sky) behind two pipes; guhrio_testhoek (30 x 16 x 30): a lane with a corner.
              /guhs guhrio testlevel and /guhs guhrio testhoek build them.
  game tests  guhrio_test_baan (24 x 12 x 7): an empty room; the tests build their own lane in it.

A level builder only needs: the blocks above in a lane one block wide, an open view from the camera's side, and level_json.
"""
import math
import os
import random

import numpy as np
from PIL import Image

TESTLEVEL = "guhrio_testlevel"
TESTHOEK = "guhrio_testhoek"
TESTBAAN = "guhrio_test_baan"

GROND, BLOK, START, VRAAG, STEEN, MUNT = ("guhs:guhrio_grond", "guhs:guhrio_blok", "guhs:guhrio_startblok", "guhs:guhrio_vraagblok",
                                          "guhs:guhrio_steen", "guhs:guhrio_munt")
VLAG, MAST, PIJP, PIJP_LIJF, GUHMBA = "guhs:guhrio_vlag", "guhs:guhrio_mast", "guhs:guhrio_pijp", "guhs:guhrio_pijp_lijf", "guhs:guhrio_guhmba_plek"
AIR = "minecraft:air"

# the Guhmba is a mini-Mika: the Mika's dusty pink
MIKA, MIKA_DONKER, MIKA_LICHT = (214, 161, 180), (176, 126, 146), (236, 196, 210)
VOET = (112, 72, 88)


# =====================================================================================================================
# textures
# =====================================================================================================================
def _vlak(kleur, var, seed, w=16, h=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((h, w, 4), np.uint8)
    n = rng.normal(0, var, (h, w))
    for c in range(3):
        a[..., c] = np.clip(kleur[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _px(a, x, y, kleur):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
        a[y, x, :3] = kleur[:3]
        a[y, x, 3] = 255


def _rand(a, licht, donker):
    """A bevel: light along the top and left, dark along the bottom and right."""
    h, w = a.shape[:2]
    a[0, :, :3] = licht
    a[:, 0, :3] = licht
    a[h - 1, :, :3] = donker
    a[:, w - 1, :3] = donker


def _grond():
    """The ground of the old games: orange-brown slabs with dark seams and a light edge."""
    a = _vlak((204, 112, 58), 4, 9101)
    donker, licht = (110, 52, 26), (242, 176, 120)
    for (x0, y0, x1, y1) in ((0, 0, 9, 7), (10, 0, 15, 4), (10, 5, 15, 11), (0, 8, 5, 15), (6, 8, 9, 11), (6, 12, 15, 15)):
        for x in range(x0, x1 + 1):
            _px(a, x, y0, licht)
            _px(a, x, y1, donker)
        for y in range(y0, y1 + 1):
            _px(a, x0, y, licht)
            _px(a, x1, y, donker)
    return Image.fromarray(a)


def _blok():
    """The hard block: a bevelled brown tile."""
    a = _vlak((178, 106, 62), 4, 9102)
    licht, donker = (236, 180, 130), (96, 50, 28)
    for i in range(3):
        a[i, i:16 - i, :3] = licht
        a[i:16 - i, i, :3] = licht
        a[15 - i, i:16 - i, :3] = donker
        a[i:16 - i, 15 - i, :3] = donker
    return Image.fromarray(a)


def _steen():
    """Bricks: four rows, every other row shifted."""
    a = _vlak((206, 104, 52), 5, 9103)
    voeg = (92, 44, 24)
    for y in (3, 7, 11, 15):
        a[y, :, :3] = voeg
    for rij, y0 in enumerate((0, 4, 8, 12)):
        for x in ((7, 15) if rij % 2 == 0 else (3, 11)):
            for y in range(y0, y0 + 3):
                _px(a, x, y, voeg)
        a[y0, :, :3] = np.clip(a[y0, :, :3].astype(int) + 26, 0, 255)       # a light top to every brick
        for x in ((7, 15) if rij % 2 == 0 else (3, 11)):
            _px(a, x, y0, voeg)
    return Image.fromarray(a)


VRAAGTEKEN = ["..####..",
              ".##..##.",
              ".##..##.",
              "....##..",
              "...##...",
              "...##...",
              "........",
              "...##..."]


def _vraag(leeg):
    """The ?-block: gold with rivets and a ?; empty it is a dull brown box."""
    a = _vlak((150, 96, 58) if leeg else (252, 188, 44), 4, 9104 + leeg)
    if leeg:
        _rand(a, (186, 130, 88), (86, 50, 30))
    else:
        _rand(a, (255, 232, 150), (150, 86, 14))
    niet = (96, 56, 32) if leeg else (150, 86, 14)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        _px(a, x, y, niet)
    if not leeg:
        for r, rij in enumerate(VRAAGTEKEN):
            for c, ch in enumerate(rij):
                if ch == "#":
                    _px(a, 5 + c, 5 + r, (120, 60, 10))        # its shadow
        for r, rij in enumerate(VRAAGTEKEN):
            for c, ch in enumerate(rij):
                if ch == "#":
                    _px(a, 4 + c, 4 + r, (255, 250, 230))
    return Image.fromarray(a)


def _munt():
    """A coin (the item's picture; the coin in a level is this picture turning)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - 7.5) / 5.2, (y - 7.5) / 7.2)
            if d <= 1.0:
                px[x, y] = (150, 96, 10, 255) if d > 0.84 else (255, 206, 56, 255)
            if d <= 0.62:
                px[x, y] = (255, 232, 130, 255)
    for y in range(4, 12):                                    # a V of vads
        dx = (y - 4) * 3 // 8
        for x in (5 + dx, 10 - dx):
            px[x, y] = (176, 110, 14, 255)
    px[5, 3] = px[6, 2] = (255, 252, 224, 255)                # a shine
    return img


def _pijp(mond):
    """The side of a green pipe: a round look (light stripe on the left, dark on the right); the mouth has a thick rim."""
    a = _vlak((58, 176, 66), 3, 9106 + mond)
    licht, donker, rand = (150, 236, 140), (24, 104, 40), (14, 70, 30)
    for y in range(16):
        for x, k in ((2, licht), (3, licht), (5, (104, 210, 104)), (12, donker), (13, donker)):
            _px(a, x, y, k)
    if mond:
        a[0, :, :3] = rand
        a[15, :, :3] = rand
        a[14, :, :3] = donker
        a[:, 0, :3] = rand
        a[:, 15, :3] = rand
    else:
        a[:, 0, :3] = (30, 30, 34)                            # the body is a little narrower: a shadow down both edges
        a[:, 15, :3] = (30, 30, 34)
        a[:, 1, :3] = rand
        a[:, 14, :3] = rand
    return Image.fromarray(a)


def _pijp_boven():
    a = _vlak((58, 176, 66), 3, 9108)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.6:
                a[y, x, :3] = (14, 70, 30)
            elif d < 4.6:
                a[y, x, :3] = (10, 30, 16)
            elif d < 5.6:
                a[y, x, :3] = (24, 104, 40)
    return Image.fromarray(a)


def _paal(kleur, top=None, vlag=None):
    """A thin pole for a cross model; optionally a ball on top and a flag hanging from it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        px[7, y] = kleur + (255,)
        px[8, y] = tuple(max(0, c - 50) for c in kleur) + (255,)
    if top:
        for x, y in ((7, 0), (8, 0), (6, 1), (7, 1), (8, 1), (9, 1), (7, 2), (8, 2)):
            px[x, y] = top + (255,)
    if vlag:
        for y in range(3, 10):
            for x in range(9, 9 + max(1, 7 - abs(y - 6) * 2)):
                px[min(15, x), y] = vlag + (255,)
        px[11, 5] = px[11, 7] = (255, 255, 255, 255)           # a little guh face on the flag
        px[12, 6] = (255, 255, 255, 255)
    return img


def _start():
    """The start: a pole with a chequered banner."""
    img = _paal((230, 230, 236))
    px = img.load()
    for y in range(1, 9):
        for x in range(9, 16):
            px[x, y] = (232, 60, 70, 255) if (x // 2 + y // 2) % 2 == 0 else (250, 250, 250, 255)
    for x in range(1, 7):                                      # and one to the other side, so it reads from both sides
        for y in range(1, 9):
            px[x, y] = (250, 250, 250, 255) if (x // 2 + y // 2) % 2 == 0 else (232, 60, 70, 255)
    return img


def _guhmba_gezicht(a, x0, y0):
    """The grumpy face, 12 x 9 at (x0, y0): slanted brows, white eyes, a Mika nose, a frown with two little teeth."""
    wit, zwart, neus = (255, 255, 255), (40, 22, 34), (236, 110, 150)
    for (x, y) in ((1, 1), (2, 1), (3, 2), (4, 2), (10, 1), (9, 1), (8, 2), (7, 2)):
        _px(a, x0 + x, y0 + y, zwart)                         # brows
    for (x, y) in ((2, 3), (3, 3), (2, 4), (3, 4), (8, 3), (9, 3), (8, 4), (9, 4)):
        _px(a, x0 + x, y0 + y, wit)
    _px(a, x0 + 3, y0 + 4, zwart)
    _px(a, x0 + 8, y0 + 4, zwart)
    _px(a, x0 + 5, y0 + 5, neus)
    _px(a, x0 + 6, y0 + 5, neus)
    for x in range(3, 9):
        _px(a, x0 + x, y0 + 7, zwart)
    _px(a, x0 + 2, y0 + 8, zwart)
    _px(a, x0 + 9, y0 + 8, zwart)
    _px(a, x0 + 4, y0 + 6, wit)
    _px(a, x0 + 7, y0 + 6, wit)


def _guhmba():
    """The Guhmba's 64 x 64 texture; the rectangles are GuhmbaRenderer's GEZICHT, VACHT, BOVEN, VOET and OOR."""
    a = np.zeros((64, 64, 4), np.uint8)
    a[0:9, 0:12] = _vlak(MIKA, 4, 9120, 12, 9)                 # GEZICHT
    a[6:9, 2:10, :3] = MIKA_LICHT                              # a lighter snout
    _guhmba_gezicht(a, 0, 0)
    a[0:9, 16:28] = _vlak(MIKA, 5, 9121, 12, 9)                # VACHT
    a[7:9, 16:28, :3] = MIKA_DONKER
    a[0:10, 32:44] = _vlak(MIKA_DONKER, 5, 9122, 12, 10)       # BOVEN
    a[16:22, 0:6] = _vlak(VOET, 4, 9123, 6, 6)                 # VOET
    a[21, 0:6, :3] = (70, 44, 56)
    a[16:20, 16:20] = _vlak(MIKA, 4, 9124, 4, 4)               # OOR
    a[17:19, 17:19, :3] = (240, 150, 180)
    return Image.fromarray(a)


def _guhmba_icoon():
    a = np.zeros((16, 16, 4), np.uint8)
    a[4:13, 2:14] = _vlak(MIKA, 4, 9125, 12, 9)
    _guhmba_gezicht(a, 2, 4)
    a[13:15, 2:7] = _vlak(VOET, 3, 9126, 5, 2)
    a[13:15, 9:14] = _vlak(VOET, 3, 9127, 5, 2)
    for x0 in (3, 10):
        a[2:4, x0:x0 + 3] = _vlak(MIKA, 3, 9128, 3, 2)
    return Image.fromarray(a)


def textures(h):
    h.save(_grond(), "block", "guhrio_grond.png")
    h.save(_blok(), "block", "guhrio_blok.png")
    h.save(_steen(), "block", "guhrio_steen.png")
    h.save(_vraag(False), "block", "guhrio_vraagblok.png")
    h.save(_vraag(True), "block", "guhrio_vraagblok_leeg.png")
    h.save(_pijp(True), "block", "guhrio_pijp.png")
    h.save(_pijp(False), "block", "guhrio_pijp_lijf.png")
    h.save(_pijp_boven(), "block", "guhrio_pijp_boven.png")
    h.save(_paal((200, 200, 208), vlag=(255, 120, 180)), "block", "guhrio_vlag.png")
    h.save(_paal((120, 220, 110)), "block", "guhrio_mast.png")
    h.save(_paal((120, 220, 110), top=(255, 210, 60), vlag=(255, 120, 180)), "block", "guhrio_mast_top.png")
    h.save(_start(), "block", "guhrio_startblok.png")
    h.save(_munt(), "item", "guhrio_munt.png")
    h.save(_guhmba_icoon(), "item", "guhrio_guhmba_plek.png")
    h.save(_guhmba(), "entity", "guhrio_guhmba.png")


# =====================================================================================================================
# models, blockstates, items, loot, tags
# =====================================================================================================================
def blocks_and_items(h):
    A = h.A
    b = lambda n: f"guhs:block/{n}"
    for name in ("guhrio_grond", "guhrio_blok", "guhrio_steen"):
        h.simple_block(name)
    # the ?-block: its own look and its empty look (the block entity draws the one that is true for you)
    h.w(f"{A}/models/block/guhrio_vraagblok.json", {"parent": "minecraft:block/cube_all", "textures": {"all": b("guhrio_vraagblok")}})
    h.w(f"{A}/models/block/guhrio_vraagblok_leeg.json", {"parent": "minecraft:block/cube_all", "textures": {"all": b("guhrio_vraagblok_leeg")}})
    h.w(f"{A}/blockstates/guhrio_vraagblok.json", {"variants": {
        f"inhoud={i},leeg={l}": {"model": b("guhrio_vraagblok_leeg" if l == "true" else "guhrio_vraagblok")}
        for i in ("munt", "superknabbel") for l in ("false", "true")}})
    h.w(f"{A}/models/item/guhrio_vraagblok.json", {"parent": b("guhrio_vraagblok")})
    # the pipe
    h.w(f"{A}/models/block/guhrio_pijp.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": b("guhrio_pijp_boven"), "bottom": b("guhrio_pijp_boven"), "side": b("guhrio_pijp")}})
    h.w(f"{A}/models/block/guhrio_pijp_lijf.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": b("guhrio_pijp_boven"), "bottom": b("guhrio_pijp_boven"), "side": b("guhrio_pijp_lijf")}})
    for name in ("guhrio_pijp", "guhrio_pijp_lijf"):
        h.w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": b(name)}}})
        h.w(f"{A}/models/item/{name}.json", {"parent": b(name)})
    # flags and the start: crosses (you see them from every side)
    for name in ("guhrio_vlag", "guhrio_mast", "guhrio_mast_top", "guhrio_startblok"):
        h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cross", "render_type": "cutout", "textures": {"cross": b(name)}})
    h.w(f"{A}/blockstates/guhrio_vlag.json", {"variants": {"": {"model": b("guhrio_vlag")}}})
    h.w(f"{A}/blockstates/guhrio_mast.json", {"variants": {"top=false": {"model": b("guhrio_mast")}, "top=true": {"model": b("guhrio_mast_top")}}})
    h.w(f"{A}/blockstates/guhrio_startblok.json", {"variants": {"": {"model": b("guhrio_startblok")}}})
    h.item_model("guhrio_vlag", b("guhrio_vlag"))
    h.item_model("guhrio_mast", b("guhrio_mast_top"))
    h.item_model("guhrio_startblok", b("guhrio_startblok"))
    # drawn by their block entity / not drawn at all: a model with only the breaking particles
    for name, particle in (("guhrio_munt", "guhs:item/guhrio_munt"), ("guhrio_guhmba_plek", "guhs:item/guhrio_guhmba_plek")):
        h.w(f"{A}/models/block/{name}.json", {"textures": {"particle": particle}})
        h.w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": b(name)}}})
        h.item_model(name)
    for name in ("guhrio_grond", "guhrio_blok", "guhrio_steen", "guhrio_vraagblok", "guhrio_pijp", "guhrio_pijp_lijf", "guhrio_munt",
                 "guhrio_vlag", "guhrio_mast", "guhrio_startblok"):
        h.self_drop(name)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in ("guhrio_grond", "guhrio_blok", "guhrio_steen", "guhrio_vraagblok",
                                                                             "guhrio_pijp", "guhrio_pijp_lijf")])


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {
    "block.guhs.guhrio_grond": "Guhrio-grond",
    "block.guhs.guhrio_blok": "Guhrio-blok",
    "block.guhs.guhrio_steen": "Guhrio-steen",
    "block.guhs.guhrio_vraagblok": "Vraagtekenblok",
    "block.guhs.guhrio_munt": "Guhrio-munt",
    "block.guhs.guhrio_vlag": "Guhrio-vlaggetje",
    "block.guhs.guhrio_mast": "Guhrio-vlaggenmast",
    "block.guhs.guhrio_pijp": "Groene pijp",
    "block.guhs.guhrio_pijp_lijf": "Groene pijp (onderstuk)",
    "block.guhs.guhrio_startblok": "Guhrio-startvlag",
    "block.guhs.guhrio_guhmba_plek": "Guhmba-plekje",
    "entity.guhs.guhrio_guhmba": "Guhmba",
    "gui.guhs.guhrio.hud.naam": "GUHRIO",
    "gui.guhs.guhrio.hud.klein": "klein maar vads",
    "gui.guhs.guhrio.hud.super": "SUPER!",
    "gui.guhs.guhrio.hud.munten": "MUNTEN",
    "gui.guhs.guhrio.hud.wereld": "WERELD",
    "gui.guhs.guhrio.hud.tijd": "TIJD",
    "gui.guhs.guhrio.hud.klaar": "Gehaald, njeg!",
    "gui.guhs.guhrio.vlag": "Vlaggetje gehaald! Hier kom je terug als het misgaat, njeg.",
    "gui.guhs.guhrio.klaar": "Level %s gehaald in %s met %s munten. Vahoeg!",
    "gui.guhs.guhrio.klaar.record": "Level %s gehaald in %s met %s munten. Dat is je snelste tijd, vahoeg!",
    "gui.guhs.guhrio.geen_level": "Deze startvlag weet niet welk level hij is (%s), njeg.",
    "gui.guhs.guhrio.pijp.kanaal": "Pijp-kanaal %s (twee pijpen met hetzelfde kanaal horen bij elkaar)",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# levels
# =====================================================================================================================
def level_json(h, level_id, banen, wereld=None, uitgang=None):
    """
    Writes data/guhs/guhrio_level/<level_id>.json. banen: a list of dicts {id, punten [[x,y,z], ...], camera "rechts"/"links",
    afstand, hoogte, onder, boven}, all in the level's own frame (see the module text); the first lane is the start block's.
    """
    for baan in banen:
        punten = baan["punten"]
        for a, c in zip(punten, punten[1:]):
            if (a[0] == c[0]) == (a[2] == c[2]):
                raise SystemExit(f"guhrio level {level_id}: lane {baan.get('id')} piece {a} -> {c} must run along x or along z")
    data = {"wereld": wereld or level_id, "banen": banen}
    if uitgang is not None:
        data["uitgang"] = list(uitgang)
    h.w(os.path.join(h.D, "guhrio_level", f"{level_id}.json"), data)


class LevelBouwer:
    """
    A level built facing east in a Structure: remembers the start block and gives every lane to level_json in the level's
    own frame (template coordinates minus the start block's). x is along the first lane, z = baan_z, y up.
    """

    def __init__(self, h, level_id, size, start, wereld=None):
        self.h, self.id, self.wereld = h, level_id, wereld
        self.s = h.Structure(size)
        self.start = start
        self.banen = []
        self.uitgang = None

    def eigen(self, p):
        return [p[0] - self.start[0], p[1] - self.start[1], p[2] - self.start[2]]

    def baan(self, baan_id, punten, onder, boven, camera="rechts", afstand=13, hoogte=6):
        """A lane through these template points; onder / boven are template heights (the floor you fall out under, the ceiling)."""
        self.banen.append({"id": baan_id, "punten": [self.eigen(p) for p in punten], "camera": camera, "afstand": afstand,
                           "hoogte": hoogte, "onder": onder - self.start[1], "boven": boven - self.start[1]})

    def save(self):
        self.s.set(*self.start, START, {"facing": "east"}, {"id": "guhs:guhrio_start", "Level": self.id})
        self.s.save(self.id)
        level_json(self.h, self.id, self.banen, self.wereld, None if self.uitgang is None else self.eigen(self.uitgang))


def _decor(s, rng, w, hgt, z, y0):
    """The painted wall behind a lane: sky, a few clouds, green hills (the binnentuin of world 1)."""
    for x in range(w):
        heuvel = y0 + 2 + int(2.5 * math.sin(x / 7.0) + 1.8 * math.sin(x / 3.1 + 1))
        for y in range(1, hgt):
            s.set(x, y, z, "minecraft:lime_concrete" if y <= heuvel else "minecraft:light_blue_concrete")
    for _ in range(max(3, w // 9)):
        cx, cy = rng.randrange(3, w - 3), rng.randrange(y0 + 7, hgt - 2)
        for dx in range(-2, 3):
            for dy in (0, 1):
                if abs(dx) < 2 or dy == 0:
                    s.set(cx + dx, cy + dy, z, "minecraft:white_concrete")


def testlevel(h):
    """
    The test level: 84 long, running east along z = 2 (a painted wall at z = 0, an open yard to z = 19 for the camera, a
    sauce-coloured plate far below). The ground's top is y = 7, you walk at y = 8. From the start (x = 4):
    coins, ?-blocks and bricks (one with a Superknabbel), a Guhmba, a pipe up to the bonus room in the sky (a second lane)
    and back down, a gap, a flag, stairs, two Guhmba's, bricks with coins on them, a wider gap, a flag, the big stairs and
    the flagpole.
    """
    W, H, D, Z, G = 84, 28, 20, 2, 7
    lvl = LevelBouwer(h, TESTLEVEL, (W, H, D), (4, G + 1, Z), wereld="TEST")
    s = lvl.s
    rng = random.Random(130001)
    s.fill(0, 0, 0, W - 1, 0, D - 1, "minecraft:orange_concrete")           # the "sauce" far below
    s.fill(0, 1, 1, W - 1, H - 1, D - 1, AIR)                                # the lane's space and the camera's yard
    _decor(s, rng, W, H, 0, G)
    gaten = set(range(33, 36)) | set(range(61, 64))
    for x in range(W):
        if x not in gaten:
            s.fill(x, G - 2, Z, x, G, Z, GROND)
    y = G + 1                                                                  # where you walk
    for x in (8, 9, 10):
        s.set(x, y + 1, Z, MUNT)
    s.set(12, y + 3, Z, VRAAG, {"inhoud": "munt", "leeg": "false"})
    for x, wat in ((16, STEEN), (17, VRAAG), (18, STEEN), (19, "super"), (20, STEEN)):
        if wat == "super":
            s.set(x, y + 3, Z, VRAAG, {"inhoud": "superknabbel", "leeg": "false"})
        elif wat == VRAAG:
            s.set(x, y + 3, Z, VRAAG, {"inhoud": "munt", "leeg": "false"})
        else:
            s.set(x, y + 3, Z, wat)
    s.set(22, y, Z, BLOK)                                                      # a step: the Guhmba turns here
    s.set(25, y, Z, GUHMBA)
    # pipe 1 up to the bonus room
    s.set(28, y, Z, PIJP_LIJF)
    s.set(28, y + 1, Z, PIJP, {"kanaal": "1"})
    s.set(31, y + 1, Z, MUNT)
    for x in (33, 34, 35):                                                     # an arc of coins over the gap
        s.set(x, y + 2 + (x == 34), Z, MUNT)
    s.set(38, y, Z, VLAG)
    for i, x in enumerate((40, 41, 42)):                                       # stairs
        s.fill(x, y, Z, x, y + i, Z, BLOK)
    s.set(45, y, Z, GUHMBA)
    s.set(48, y, Z, GUHMBA)
    # pipe 2: where you come back from the bonus room
    s.set(50, y, Z, PIJP_LIJF)
    s.set(50, y + 1, Z, PIJP, {"kanaal": "2"})
    for x in range(53, 59):
        s.set(x, y + 3, Z, STEEN if x != 55 else VRAAG, None if x != 55 else {"inhoud": "munt", "leeg": "false"})
        s.set(x, y + 4, Z, MUNT)
    s.set(57, y, Z, GUHMBA)
    s.set(66, y, Z, VLAG)
    for i, x in enumerate((68, 69, 70, 71)):                                   # the big stairs
        s.fill(x, y, Z, x, y + i, Z, BLOK)
    for k in range(6):                                                         # the flagpole
        s.set(76, y + k, Z, MAST, {"top": "true" if k == 5 else "false"})
    lvl.baan("hoofd", [(0, y, Z), (W - 1, y, Z)], onder=G - 4, boven=G + 10)
    lvl.uitgang = (80, y, Z)
    # the bonus room in the sky: its own lane (x 26..48), floor at y = 19, you walk at y = 20
    B = 19
    for x in range(26, 49):
        s.set(x, B, Z, STEEN if x in (26, 48) else GROND)
    for x in (26, 48):                                                         # walls at both ends
        s.fill(x, B + 1, Z, x, B + 4, Z, BLOK)
    s.set(28, B + 1, Z, PIJP_LIJF)
    s.set(28, B + 2, Z, PIJP, {"kanaal": "1"})
    s.set(46, B + 1, Z, PIJP_LIJF)
    s.set(46, B + 2, Z, PIJP, {"kanaal": "2"})
    for x in range(31, 44):
        s.set(x, B + 1, Z, MUNT)
        if x % 2 == 1:
            s.set(x, B + 3, Z, MUNT)
    lvl.baan("bonus", [(26, B + 1, Z), (48, B + 1, Z)], onder=B, boven=B + 8)
    lvl.save()


def testhoek(h):
    """A lane with a corner (20 east, then 20 south), flat, with coins, a Guhmba after the corner, a flag and the pole."""
    W, H, D, G = 34, 16, 34, 3
    lvl = LevelBouwer(h, TESTHOEK, (W, H, D), (2, G + 1, 2), wereld="HOEK")
    s = lvl.s
    s.fill(0, 0, 0, W - 1, 0, D - 1, "minecraft:orange_concrete")
    s.fill(0, 1, 0, W - 1, H - 1, D - 1, AIR)
    y = G + 1
    for x in range(0, 23):
        s.fill(x, G - 1, 2, x, G, 2, GROND)
    for z in range(2, 23):
        s.fill(22, G - 1, z, 22, G, z, GROND)
    for x in range(6, 20, 2):
        s.set(x, y + 1, 2, MUNT)
    for z in range(6, 16, 2):
        s.set(22, y + 1, z, MUNT)
    s.set(22, y, 9, GUHMBA)
    s.set(22, y, 5, BLOK)
    s.set(22, y, 14, BLOK)
    s.set(12, y + 3, 2, VRAAG, {"inhoud": "munt", "leeg": "false"})
    for k in range(5):
        s.set(22, y + k, 20, MAST, {"top": "true" if k == 4 else "false"})
    # the camera stands on the lane's right hand: south of the first piece, west of the second (inside the corner)
    lvl.baan("hoek", [(0, y, 2), (22, y, 2), (22, y, 22)], onder=G - 2, boven=G + 9, afstand=11, hoogte=5.5)
    lvl.save()


def structures(h):
    testlevel(h)
    testhoek(h)
    h.Structure((24, 12, 7)).save(TESTBAAN)


def selfcheck(h):
    A, D = h.A, h.D
    missing = [p for p in [f"{A}/blockstates/{n}.json" for n in ("guhrio_grond", "guhrio_blok", "guhrio_steen", "guhrio_vraagblok", "guhrio_munt",
                                                                 "guhrio_vlag", "guhrio_mast", "guhrio_pijp", "guhrio_pijp_lijf",
                                                                 "guhrio_startblok", "guhrio_guhmba_plek")]
               + [f"{D}/guhrio_level/{TESTLEVEL}.json", f"{D}/guhrio_level/{TESTHOEK}.json", f"{D}/structure/{TESTLEVEL}.nbt",
                  f"{D}/structure/{TESTHOEK}.nbt", f"{D}/structure/{TESTBAAN}.nbt"] if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    if missing:
        raise SystemExit(f"guhrio: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    texts(h)
    structures(h)
    selfcheck(h)
