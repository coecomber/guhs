"""
Super Guhrio (bbq2; the Java side is feature/guhrio) - the engine's resources, the castle and the test levels.

  this module      the blocks' textures / models / blockstates / loot / tags, every text, Guhshi's look (bones + fur), the
                   questline "guhrio" and its advancements, the FTB quests of the castle, the test levels and test rooms
  guhrio_tex       the textures of the full engine's pieces (this module still draws the first ones itself)
  guhrio_modellen  the box models + textures of the creatures (Guhmba, Schild-Mika, Plof-Mika, Hapbloem, grill spit,
                   platform, falling block); `python tools/features/guhrio_modellen.py <dir>` renders previews
  guhrio_baan      the lane builder (Baanbouwer / Plekbouwer / level_json): what the level slices build with
  guhrio_proef     the engine's own practice lanes (what stands in a level slot until its slice delivers)
  guhrio_kasteel   the castle guhs:guhrio_kasteel: the shell, the slots, the levels stamped in, the structure set

  blocks      guhrio_grond, guhrio_blok, guhrio_siersteen (plain blocks to build with; siersteen = the brick's look for the
              castle's trim), guhrio_startblok, guhrio_poort (a gate to a level elsewhere), guhrio_vraagblok
              (inhoud munt / superknabbel / vuurpeper; leeg=true is only its empty look), guhrio_steen, guhrio_onzichtbaar,
              guhrio_munt, guhrio_vadsmunt (nummer 0..2), guhrio_vlag, guhrio_mast (top), guhrio_pijp (kanaal 0..15, facing,
              ingang, boven) + guhrio_pijp_lijf (axis), guhrio_deur (half, kanaal), guhrio_schakelaar (kanaal 0..7, soort,
              ingedrukt = look) + guhrio_schakelblok (kanaal, aan, open = look), the invisible spots guhrio_guhmba_plek,
              guhrio_schild_mika_plek, guhrio_plof_mika_plek, guhrio_hapbloem_plek, guhrio_platform_plek (as, afstand, breed,
              snel), guhrio_valblok_plek (breed), the grill spit's hub guhrio_grillspies_plek (lengte, tegen, snel, fase),
              guhrio_guhshi_ei and guhrio_guhshi_plek
  items       guhrio_superknabbel, guhrio_vuurpeper, guhrio_vadsmunt_schim (pictures only)
  levels      data/guhs/guhrio_level/<id>.json (guhrio_baan.level_json): kasteel_1_1 .. kasteel_3_2, kasteel_duel, the tests
  test levels guhrio_testlevel (84 x 28 x 20) and guhrio_testhoek (a lane with a corner): /guhs guhrio testlevel|testhoek
              (dev runs only); guhrio_test_baan (24 x 12 x 7, an empty room for the game tests) and guhrio_test_slot (a
              little level made with the Baanbouwer, so the tests play what the lane builder really writes)
"""
import math
import os
import random

import numpy as np
from PIL import Image

from features import bbq2
from features import guhrio_baan
from features import guhrio_kasteel
from features import guhrio_loop
from features import guhrio_modellen
from features import guhrio_tex
from features.guhrio_baan import level_json  # noqa: F401  (also the import path the prototype had)

TESTLEVEL = "guhrio_testlevel"
TESTHOEK = "guhrio_testhoek"
TESTBAAN = "guhrio_test_baan"
TESTSLOT = "guhrio_test_slot"

GROND, BLOK, START, VRAAG, STEEN, MUNT = ("guhs:guhrio_grond", "guhs:guhrio_blok", "guhs:guhrio_startblok", "guhs:guhrio_vraagblok",
                                          "guhs:guhrio_steen", "guhs:guhrio_munt")
VLAG, MAST, PIJP, PIJP_LIJF, GUHMBA = "guhs:guhrio_vlag", "guhs:guhrio_mast", "guhs:guhrio_pijp", "guhs:guhrio_pijp_lijf", "guhs:guhrio_guhmba_plek"
DEUR = "guhs:guhrio_deur"
AIR = "minecraft:air"

# the section of chapter guhs_guhrio the castle's own quests land in (CONTRACT_130 5.5 / 8)
FTB_SECTIES = [("guhrio", "Het Kasteel van de Grote Nether-Mika", "npc:padguh", None)]

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


def _deur(boven):
    """Half of a door: dark wood with a pink frame; the top half has a little window, the bottom half the knob."""
    a = _vlak((132, 84, 52), 4, 9110 + boven)
    lijst, donker = (236, 150, 186), (86, 50, 30)
    a[:, 0:2, :3] = lijst
    a[:, 14:16, :3] = lijst
    if boven:
        a[0:2, :, :3] = lijst
        for y in range(4, 9):
            for x in range(5, 11):
                _px(a, x, y, (150, 220, 250) if (x + y) % 5 else (230, 250, 255))
        for x in range(4, 12):
            _px(a, x, 3, donker)
            _px(a, x, 9, donker)
    else:
        for x in range(4, 12):
            _px(a, x, 4, donker)
            _px(a, x, 12, donker)
        for y in range(4, 13):
            _px(a, 4, y, donker)
            _px(a, 11, y, donker)
        _px(a, 12, 1, (255, 214, 70))
        _px(a, 12, 2, (200, 150, 30))
    return Image.fromarray(a)


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
    h.save(_deur(False), "block", "guhrio_deur_onder.png")
    h.save(_deur(True), "block", "guhrio_deur_boven.png")
    h.save(_munt(), "item", "guhrio_munt.png")
    h.save(_guhmba_icoon(), "item", "guhrio_guhmba_plek.png")
    t = guhrio_tex
    h.save(t.schakelaar(False), "block", "guhrio_schakelaar.png")
    h.save(t.schakelaar(True), "block", "guhrio_schakelaar_in.png")
    for rood, naam in ((False, "blauw"), (True, "rood")):
        h.save(t.schakelblok(rood, False), "block", f"guhrio_schakelblok_{naam}.png")
        h.save(t.schakelblok(rood, True), "block", f"guhrio_schakelblok_{naam}_open.png")
    h.save(t.poort(), "block", "guhrio_poort.png")
    h.save(t.naaf(), "block", "guhrio_grillspies_plek.png")
    h.save(t.vadsmunt(False), "item", "guhrio_vadsmunt.png")
    h.save(t.vadsmunt(True), "item", "guhrio_vadsmunt_schim.png")
    h.save(t.onzichtbaar_icoon(), "item", "guhrio_onzichtbaar.png")
    h.save(t.superknabbel(), "item", "guhrio_superknabbel.png")
    h.save(t.vuurpeper(), "item", "guhrio_vuurpeper.png")
    h.save(t.guhshi_ei(), "item", "guhrio_guhshi_ei.png")
    h.save(t.guhshi_kop(), "item", "guhrio_guhshi_plek.png")
    for soort in ("schild_mika", "plof_mika", "hapbloem", "platform", "valblok"):
        h.save(t.plek_icoon(soort), "item", f"guhrio_{soort}_plek.png")
    guhrio_modellen.build(h)
    # (the prototype's Guhmba texture is the model's own now)
    oud = os.path.join(h.TEX, "entity", "guhrio_guhmba.png")
    if os.path.exists(oud):
        os.remove(oud)


# =====================================================================================================================
# models, blockstates, items, loot, tags
# =====================================================================================================================
ONZICHTBAAR = ("guhrio_munt", "guhrio_vadsmunt", "guhrio_onzichtbaar", "guhrio_guhmba_plek", "guhrio_schild_mika_plek", "guhrio_plof_mika_plek",
               "guhrio_hapbloem_plek", "guhrio_platform_plek", "guhrio_valblok_plek", "guhrio_guhshi_ei", "guhrio_guhshi_plek")
DROPT = ("guhrio_grond", "guhrio_blok", "guhrio_siersteen", "guhrio_steen", "guhrio_vraagblok", "guhrio_pijp", "guhrio_pijp_lijf", "guhrio_munt", "guhrio_vadsmunt",
         "guhrio_vlag", "guhrio_mast", "guhrio_startblok", "guhrio_poort", "guhrio_deur", "guhrio_schakelaar", "guhrio_schakelblok",
         "guhrio_grillspies_plek", "guhrio_guhshi_ei", "guhrio_guhshi_plek")
BLOKKEN = ("guhrio_grond", "guhrio_blok", "guhrio_siersteen", "guhrio_steen", "guhrio_vraagblok", "guhrio_onzichtbaar", "guhrio_munt", "guhrio_vadsmunt", "guhrio_vlag",
           "guhrio_mast", "guhrio_pijp", "guhrio_pijp_lijf", "guhrio_startblok", "guhrio_poort", "guhrio_deur", "guhrio_schakelaar",
           "guhrio_schakelblok", "guhrio_guhmba_plek", "guhrio_schild_mika_plek", "guhrio_plof_mika_plek", "guhrio_hapbloem_plek",
           "guhrio_grillspies_plek", "guhrio_platform_plek", "guhrio_valblok_plek", "guhrio_guhshi_ei", "guhrio_guhshi_plek")


def blocks_and_items(h):
    A = h.A
    b = lambda n: f"guhs:block/{n}"
    for name in ("guhrio_grond", "guhrio_blok", "guhrio_steen", "guhrio_grillspies_plek"):
        h.simple_block(name)
    # the brick as plain masonry (the castle's trim): the brick's look on an ordinary block, see GuhrioFeature.SIERSTEEN
    h.simple_block("guhrio_siersteen", b("guhrio_steen"))
    # the ?-block: its own look and its empty look (the block entity draws the one that is true for you)
    h.w(f"{A}/models/block/guhrio_vraagblok.json", {"parent": "minecraft:block/cube_all", "textures": {"all": b("guhrio_vraagblok")}})
    h.w(f"{A}/models/block/guhrio_vraagblok_leeg.json", {"parent": "minecraft:block/cube_all", "textures": {"all": b("guhrio_vraagblok_leeg")}})
    h.w(f"{A}/blockstates/guhrio_vraagblok.json", {"variants": {
        f"leeg={l}": {"model": b("guhrio_vraagblok_leeg" if l == "true" else "guhrio_vraagblok")} for l in ("false", "true")}})
    h.w(f"{A}/models/item/guhrio_vraagblok.json", {"parent": b("guhrio_vraagblok")})
    # the switch (pressed = the channel is on for you) and the switched blocks (blue: solid while on, red: solid while off)
    for naam in ("guhrio_schakelaar", "guhrio_schakelaar_in", "guhrio_schakelblok_blauw", "guhrio_schakelblok_rood"):
        h.w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": b(naam)}})
    for naam in ("guhrio_schakelblok_blauw_open", "guhrio_schakelblok_rood_open"):
        h.w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/cube_all", "render_type": "cutout", "textures": {"all": b(naam)}})
    h.w(f"{A}/blockstates/guhrio_schakelaar.json", {"variants": {"ingedrukt=false": {"model": b("guhrio_schakelaar")},
                                                                  "ingedrukt=true": {"model": b("guhrio_schakelaar_in")}}})
    h.w(f"{A}/blockstates/guhrio_schakelblok.json", {"variants": {
        f"aan={aan},open={op}": {"model": b(f"guhrio_schakelblok_{'blauw' if aan == 'true' else 'rood'}{'_open' if op == 'true' else ''}")}
        for aan in ("true", "false") for op in ("true", "false")}})
    h.w(f"{A}/models/item/guhrio_schakelaar.json", {"parent": b("guhrio_schakelaar")})
    h.w(f"{A}/models/item/guhrio_schakelblok.json", {"parent": b("guhrio_schakelblok_blauw")})
    # the pipe: the mouth opens the way it faces, the body lies along its axis
    h.w(f"{A}/models/block/guhrio_pijp.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": b("guhrio_pijp_boven"), "bottom": b("guhrio_pijp_boven"), "side": b("guhrio_pijp")}})
    h.w(f"{A}/models/block/guhrio_pijp_lijf.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": b("guhrio_pijp_boven"), "bottom": b("guhrio_pijp_boven"), "side": b("guhrio_pijp_lijf")}})
    draai = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "west": {"x": 90, "y": 270}, "east": {"x": 90, "y": 90}}
    h.w(f"{A}/blockstates/guhrio_pijp.json", {"variants": {f"facing={f}": {"model": b("guhrio_pijp"), **r} for f, r in draai.items()}})
    h.w(f"{A}/blockstates/guhrio_pijp_lijf.json", {"variants": {"axis=y": {"model": b("guhrio_pijp_lijf")},
                                                                 "axis=z": {"model": b("guhrio_pijp_lijf"), "x": 90},
                                                                 "axis=x": {"model": b("guhrio_pijp_lijf"), "x": 90, "y": 90}}})
    for name in ("guhrio_pijp", "guhrio_pijp_lijf"):
        h.w(f"{A}/models/item/{name}.json", {"parent": b(name)})
    # flags, the start and the gate: crosses (you see them from every side)
    for name in ("guhrio_vlag", "guhrio_mast", "guhrio_mast_top", "guhrio_startblok", "guhrio_poort"):
        h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cross", "render_type": "cutout", "textures": {"cross": b(name)}})
    h.w(f"{A}/blockstates/guhrio_vlag.json", {"variants": {"": {"model": b("guhrio_vlag")}}})
    h.w(f"{A}/blockstates/guhrio_mast.json", {"variants": {"top=false": {"model": b("guhrio_mast")}, "top=true": {"model": b("guhrio_mast_top")}}})
    h.w(f"{A}/blockstates/guhrio_startblok.json", {"variants": {"": {"model": b("guhrio_startblok")}}})
    h.w(f"{A}/blockstates/guhrio_poort.json", {"variants": {"": {"model": b("guhrio_poort")}}})
    h.item_model("guhrio_vlag", b("guhrio_vlag"))
    h.item_model("guhrio_mast", b("guhrio_mast_top"))
    h.item_model("guhrio_startblok", b("guhrio_startblok"))
    h.item_model("guhrio_poort", b("guhrio_poort"))
    # the door: two thin crossed panels (one of them always faces the camera, whichever way the lane runs)
    for half in ("onder", "boven"):
        tex = b(f"guhrio_deur_{half}")
        face = {"uv": [0, 0, 16, 16], "texture": "#deur"}
        h.w(f"{A}/models/block/guhrio_deur_{half}.json", {"render_type": "cutout", "textures": {"deur": tex, "particle": tex}, "elements": [
            {"from": [0, 0, 7.5], "to": [16, 16, 8.5], "faces": {"north": face, "south": face}},
            {"from": [7.5, 0, 0], "to": [8.5, 16, 16], "faces": {"east": face, "west": face}}]})
    h.w(f"{A}/blockstates/guhrio_deur.json", {"variants": {"half=lower": {"model": b("guhrio_deur_onder")},
                                                            "half=upper": {"model": b("guhrio_deur_boven")}}})
    h.item_model("guhrio_deur", b("guhrio_deur_onder"))
    # drawn by their block entity / not drawn at all: a model with only the breaking particles, a flat picture as item
    for name in ONZICHTBAAR:
        h.w(f"{A}/models/block/{name}.json", {"textures": {"particle": f"guhs:item/{name}"}})
        h.w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": b(name)}}})
        h.item_model(name)
    for name in ("guhrio_superknabbel", "guhrio_vuurpeper", "guhrio_vadsmunt_schim"):
        h.item_model(name)
    for name in DROPT:
        h.self_drop(name)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in (
        "guhrio_grond", "guhrio_blok", "guhrio_siersteen", "guhrio_steen", "guhrio_vraagblok", "guhrio_pijp", "guhrio_pijp_lijf",
        "guhrio_schakelaar", "guhrio_schakelblok", "guhrio_grillspies_plek")])


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {
    "block.guhs.guhrio_grond": "Guhrio-grond",
    "block.guhs.guhrio_blok": "Guhrio-blok",
    "block.guhs.guhrio_steen": "Guhrio-steen",
    "block.guhs.guhrio_siersteen": "Guhrio-siersteen",
    "block.guhs.guhrio_vraagblok": "Vraagtekenblok",
    "block.guhs.guhrio_onzichtbaar": "Onzichtbaar vraagtekenblok",
    "block.guhs.guhrio_munt": "Guhrio-munt",
    "block.guhs.guhrio_vadsmunt": "Grote vadsmunt",
    "block.guhs.guhrio_vlag": "Guhrio-vlaggetje",
    "block.guhs.guhrio_mast": "Guhrio-vlaggenmast",
    "block.guhs.guhrio_pijp": "Groene pijp",
    "block.guhs.guhrio_pijp_lijf": "Groene pijp (onderstuk)",
    "block.guhs.guhrio_deur": "Guhrio-deur",
    "block.guhs.guhrio_startblok": "Guhrio-startvlag",
    "block.guhs.guhrio_poort": "Levelpoort",
    "block.guhs.guhrio_schakelaar": "Uitroeptekenschakelaar",
    "block.guhs.guhrio_schakelblok": "Schakelblok",
    "block.guhs.guhrio_guhmba_plek": "Guhmba-plekje",
    "block.guhs.guhrio_schild_mika_plek": "Schild-Mika-plekje",
    "block.guhs.guhrio_plof_mika_plek": "Plof-Mika-plekje",
    "block.guhs.guhrio_hapbloem_plek": "Hapbloem-plekje",
    "block.guhs.guhrio_grillspies_plek": "Naaf van een grillspies",
    "block.guhs.guhrio_platform_plek": "Platform-plekje",
    "block.guhs.guhrio_valblok_plek": "Valblok-plekje",
    "block.guhs.guhrio_guhshi_ei": "Ei van Guhshi",
    "block.guhs.guhrio_guhshi_plek": "Wachtplekje van Guhshi",
    "item.guhs.guhrio_superknabbel": "Superknabbel",
    "item.guhs.guhrio_vuurpeper": "Vuurpeper",
    "item.guhs.guhrio_vadsmunt_schim": "Grote vadsmunt (al gehad)",
    "entity.guhs.guhmba": "Guhmba",
    "entity.guhs.schild_mika": "Schild-Mika",
    "entity.guhs.plof_mika": "Plof-Mika",
    "entity.guhs.hapbloem": "Hapbloem",
    "entity.guhs.guhrio_grillspies": "Draaiende grillspies",
    "entity.guhs.guhrio_knabbel": "Gegooide knabbel",
    "entity.guhs.guhrio_platform": "Platform",
    "entity.guhs.guhrio_valblok": "Valblok",
    "gui.guhs.guhrio.hud.naam": "GUHRIO",
    "gui.guhs.guhrio.hud.klein": "klein",
    "gui.guhs.guhrio.hud.super": "SUPER!",
    "gui.guhs.guhrio.hud.vuur": "VUURPEPER!",
    "gui.guhs.guhrio.hud.guhshi": "OP GUHSHI!",
    "gui.guhs.guhrio.hud.munten": "MUNTEN",
    "gui.guhs.guhrio.hud.wereld": "WERELD",
    "gui.guhs.guhrio.hud.tijd": "TIJD",
    "gui.guhs.guhrio.hud.klaar": "Gehaald, njeg!",
    "gui.guhs.guhrio.vlag": "Vlaggetje gehaald! Hier kom je terug als het misgaat, njeg.",
    "gui.guhs.guhrio.klaar": "Level %s gehaald in %s met %s munten. Vahoeg!",
    "gui.guhs.guhrio.klaar.record": "Level %s gehaald in %s met %s munten. Dat is je snelste tijd, vahoeg!",
    "gui.guhs.guhrio.geen_level": "Deze startvlag weet niet welk level hij is (%s), njeg.",
    "gui.guhs.guhrio.pijp.kanaal": "Kanaal %s (twee pijpen of twee deuren met hetzelfde kanaal horen bij elkaar)",
    "gui.guhs.guhrio.eerst": "Deze poort zit nog dicht. Haal eerst level %s, njeg!",
    "gui.guhs.guhrio.stoppen": "Druk nog een keer op Q om uit het level te stappen. Njeg?",
    "gui.guhs.guhrio.vadsmunt": "Grote vadsmunt! Dat is %s van de 3 in dit level. Vads!",
    "gui.guhs.guhrio.vadsmunten_alle": "Alle achttien grote vadsmunten van het kasteel zijn van jou. Super vahoege vads!",
    "gui.guhs.guhrio.ei": "Er beweegt iets in dat ei... Het is van Guhshi! In de burcht wacht hij op je, njeg.",
    "gui.guhs.guhrio.geen_ei": "Guhshi kijkt je vragend aan. Hij komt alleen mee met wie zijn ei gevonden heeft (in de kelders), njeg.",
    "gui.guhs.guhrio.verdwaald": "Hé, hoe kom jij hier? Hier kun je alleen spelen. Pad-guh zet je even terug bij de poort, njeg!",
    "gui.guhs.guhrio.kasteel": "Het hele kasteel in één keer: %s. Vahoeg!",
    "gui.guhs.guhrio.kasteel.record": "Het hele kasteel in één keer: %s. Dat is je snelste keer ooit, super vahoeg!",
    "gui.guhs.guhrio.vadsmunt.0": "De eerste grote vadsmunt van dit level",
    "gui.guhs.guhrio.vadsmunt.1": "De tweede grote vadsmunt van dit level",
    "gui.guhs.guhrio.vadsmunt.2": "De derde grote vadsmunt van dit level",
    "gui.guhs.guhrio.beloning.kasteel": "Het hele kasteel in één keer uitgespeeld (van 1-1 tot en met 3-2)",
    "gui.guhs.highscores.game.guhrio_kasteel": "Super Guhrio: het hele kasteel",
    "gui.guhs.spelgroep.guhrio_beloning": "Super Guhrio",
    "gui.guhs.spelgroep.guhrio_beloning.waar": "In de frituursauszee van de Guhbarbecuether: het Kasteel van de Grote Nether-Mika, bij Pad-guh op "
                                               "het voorplein. Njeg!",
    "sign.guhs.guhrio.poort.duel.1": "De Grote",
    "sign.guhs.guhrio.poort.duel.2": "Nether-Mika",
    "sign.guhs.guhrio.kasteel.bord.1": "Kasteel van de",
    "sign.guhs.guhrio.kasteel.bord.2": "Grote Nether-Mika",
    "sign.guhs.guhrio.kasteel.bord.3": "Voeten vegen, njeg",
}
WERELDEN = {"1": "de binnentuin", "2": "de kelders", "3": "de burcht"}
for _w in "123":
    for _n in "12":
        TEXTS[f"gui.guhs.guhrio.vadsmunten.{_w}_{_n}"] = f"Alle drie de grote vadsmunten van level {_w}-{_n}"
        TEXTS[f"gui.guhs.highscores.game.guhrio_{_w}_{_n}"] = f"Super Guhrio: level {_w}-{_n}"
        TEXTS[f"sign.guhs.guhrio.poort.{_w}-{_n}.1"] = f"Level {_w}-{_n}"
        TEXTS[f"sign.guhs.guhrio.poort.{_w}-{_n}.2"] = WERELDEN[_w]


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)
    bbq2.pagina(h, "guhshi", "Guhshi", "Eén per speler, na het duel in het Kasteel van de Grote Nether-Mika",
                "Guhshi komt uit een gespikkeld ei in de kelders van het kasteel. Hij heeft een grote ronde neus, een rood zadeltje en "
                "oranje laarsjes. Met jou op zijn rug fladdert hij over de grootste gaten, en met zijn lange tong hapt hij Guhmba's en "
                "munten zo uit de lucht. Njam-njeg!")


# =====================================================================================================================
# the questline, its advancements and the FTB quests of the castle
# =====================================================================================================================
def verhaal(h):
    from features import verhaal_motor
    kasteel = "Het Kasteel van de Grote Nether-Mika (Guhbarbecuether)"
    hal = "De levelhal van het kasteel"
    stappen = [("Loop het kasteel in", "Zoek het Kasteel van de Grote Nether-Mika in de frituursauszee en loop door de poort van level 1-1.", kasteel)]
    for w in "123":
        for n in "12":
            stappen.append((f"Haal level {w}-{n}", f"Loop in de levelhal door de poort van level {w}-{n} ({WERELDEN[w]}) en haal de "
                                                   f"vlaggenmast aan het eind. A en D lopen, spatie springt, S duikt in een pijp. Njeg!", hal))
    stappen.append(("Het duel met de Grote Nether-Mika", "De grote poort aan het eind van de rode loper is open. Daarachter wacht de Grote "
                                                         "Nether-Mika zelf. Hij duwt alleen maar, njeg.", hal))
    verhaal_motor.verhaallijn(h, "guhrio", "Super Guhrio", "De Grote Nether-Mika heeft Prinses Perzikguh meegenomen 'voor een stukje taart'. "
                              "Zes levels van opzij en een duel staan tussen jou en de taart. Njeg!", stappen,
                              klaar=("Het kasteel is uitgespeeld. Bedankt! Maar speel gerust nog eens: er liggen vast nog vadsmunten. Vahoeg!", kasteel),
                              kort={"0": "Loop het kasteel in"})
    for naam in ("guhrio_ei", "guhrio_duel", "guhrio_vadsmunten", "guhrio_kasteel_loop"):
        bbq2.verborgen(h, naam)
    bbq2.zichtbaar(h, "guhrio", "guhrio_levels", "root", "guhs:guhrio_mast", "goal", "Vlaggenmastenverzamelaar",
                   "Haal alle zes de levels van het Kasteel van de Grote Nether-Mika")
    bbq2.zichtbaar(h, "guhrio", "guhrio_vadsmunten", "guhrio_levels", "guhs:guhrio_vadsmunt", "challenge", "Super vahoege vadsverzamelaar",
                   "Vind alle achttien grote vadsmunten van het kasteel")
    bbq2.zichtbaar(h, "guhrio", "guhrio_duel", "guhrio_levels", "guhs:guhrio_vuurpeper", "challenge", "Hij had gewoon trek in taart",
                   "Win het duel met de Grote Nether-Mika")


def ftb(fq):
    q, adv, structure = fq.q, fq.adv, fq.structure
    q("guhrio_kasteel", "Een kasteel in de saus", "Midden in de frituursauszee van de Guhbarbecuether staat het &6Kasteel van de Grote "
      "Nether-Mika&r. Het superkompas (Barbecue > Kasteel van de Grote Nether-Mika) wijst de weg; er ligt een steigertje voor de poort.",
      "guhs:guhrio_steen", [structure("guhrio_kasteel")], rewards=(("guhs:kaas_knabbels", 12),), deps=["bbq_aan"], shape="hexagon", xp=100)
    q("guhrio_binnen", "Van opzij bekeken", "Loop in de levelhal door de poort van &6level 1-1&r. In een level kijk je van opzij: &dA&r en &dD&r "
      "lopen, &dspatie&r springt (ingedrukt houden = hoger), &dS&r duikt in een pijp, &dW&r gaat door een deur, sprinten is rennen. "
      "Er gaat nooit iets stuk en niemand doet je pijn: wie valt of geduwd wordt, staat weer bij zijn laatste vlaggetje. Twee keer &dQ&r = eruit.",
      "guhs:guhrio_startblok", [adv("guhrio_stap_1")], deps=["guhrio_kasteel"])
    q("guhrio_levels", "Zes vlaggenmasten", "Haal de vlaggenmast van alle zes de levels: de binnentuin (1-1, 1-2), de kelders (2-1, 2-2) en de "
      "burcht (3-1, 3-2). Elke poort gaat open als het level ervoor gehaald is.", "guhs:guhrio_mast", [adv("guhrio_stap_7")],
      rewards=(("guhs:kaas_knabbels", 24),), deps=["guhrio_binnen"], shape="gear", xp=200)
    q("guhrio_vadsmunten", "Achttien keer vads", "In elk level liggen drie &6grote vadsmunten&r. Ze blijven van jou, ook als je het level "
      "opnieuw speelt; de Guhdex (Verhalen > Super Guhrio) houdt per level bij welke je hebt.", "guhs:guhrio_vadsmunt", [adv("guhrio_vadsmunten")],
      rewards=(("guhs:kaas_knabbels", 32),), deps=["guhrio_binnen"], shape="diamond", xp=200)
    q("guhrio_loop", "In één ruk door", "Begin bij level 1-1 en haal alle zes de levels achter elkaar, in volgorde. Je tijd komt op het "
      "highscorebord (Guhdex > Highscores), naast het record van de server. Tijd die je in een level verliest door eruit te stappen telt mee, njeg.",
      "minecraft:clock", [adv("guhrio_kasteel_loop")], rewards=(("guhs:kaas_knabbels", 16),), deps=["guhrio_levels"], shape="circle")


# =====================================================================================================================
# Guhshi: the same guh, with the nose, the saddle, the crest and the boots
# =====================================================================================================================
# The guh texture has no room left for new variant swatches (make_guh_variants: "no free texture space"). A variant's bones are
# only ever drawn on that variant's own texture, so Guhshi's bones borrow the swatch PLACES of other story guhs and paint
# their own colours there in guh_guhshi.png (nothing changes for the Baltoguh, Guhtwo or the 626-guh).
GROEN_VLAK, WIT_VLAK, ROOD_VLAK, ORANJE_VLAK, DONKER_VLAK = "balto_vacht", "balto_licht", "stitch_vacht", "mewtwo_buis", "stitch_oor_binnen"
_KOP, _LIJF = [0, 5, -2], [0, 5, 5]
BONES = {
    # (GuhVariant GUHSHI shows the bones whose name starts with "guhshi")
    "guhshi_snuit": ("head", _KOP, GROEN_VLAK, [([-5.0, 1.0, -15.6], [10.0, 6.6, 4.2], 0), ([-4.0, 1.8, -17.0], [8.0, 5.0, 1.6], 0)]),
    "guhshi_neusgaten": ("head", _KOP, DONKER_VLAK, [([-2.6, 5.2, -17.3], [1.2, 1.2, 0.5], 0), ([1.4, 5.2, -17.3], [1.2, 1.2, 0.5], 0)]),
    "guhshi_kin": ("head", _KOP, WIT_VLAK, [([-4.6, 0.2, -15.2], [9.2, 1.4, 3.8], 0)]),
    "guhshi_kam": ("head", _KOP, ROOD_VLAK, [([-1.2, 13.6, -7.0], [2.4, 2.6, 2.6], 0), ([-1.2, 13.0, -3.6], [2.4, 2.6, 2.6], 0),
                                                 ([-1.2, 11.4, -0.4], [2.4, 2.6, 2.4], 0)]),
    "guhshi_zadel": ("body", _LIJF, ROOD_VLAK, [([-4.6, 9.8, 2.0], [9.2, 2.6, 7.6], 0)]),
    "guhshi_zadelrand": ("body", _LIJF, WIT_VLAK, [([-5.4, 9.5, 1.2], [10.8, 1.2, 9.2], 0)]),
    "guhshi_buik": ("body", _LIJF, WIT_VLAK, [([-5.0, -0.25, -1.0], [10.0, 0.5, 10.0], 0)]),
    "guhshi_staart": ("tail", [0, 0.5, 11], GROEN_VLAK, [([-1.8, 0.6, 10.4], [3.6, 3.4, 3.8], 0)]),
    "guhshi_laars_vl": ("leg_front_left", [4, 1, -11], ORANJE_VLAK, [([1.9, -0.1, -14.1], [4.2, 2.7, 4.6], 0)]),
    "guhshi_laars_vr": ("leg_front_right", [-4, 1, -11], ORANJE_VLAK, [([-6.1, -0.1, -14.1], [4.2, 2.7, 4.6], 0)]),
    "guhshi_laars_al": ("leg_back_left", [6, 1, 8], ORANJE_VLAK, [([4.2, -0.1, 5.6], [4.1, 3.2, 5.3], 0)]),
    "guhshi_laars_ar": ("leg_back_right", [-6, 1, 8], ORANJE_VLAK, [([-8.3, -0.1, 5.6], [4.1, 3.2, 5.3], 0)]),
}


def variants(rng, v):
    """Guhshi's fur and the swatches of his bones: green, a white belly and cheeks, a red saddle and crest, orange boots."""
    return {"guhshi": ((108, 194, 74), {GROEN_VLAK: lambda: v.fabric((108, 194, 74), rng, 8),
                                       DONKER_VLAK: lambda: v.fabric((44, 96, 40), rng, 4),
                                       WIT_VLAK: lambda: v.fabric((250, 250, 244), rng, 5),
                                       ROOD_VLAK: lambda: v.fabric((226, 52, 48), rng, 8),
                                       ORANJE_VLAK: lambda: v.fabric((240, 140, 40), rng, 8)})}


# =====================================================================================================================
# levels
# =====================================================================================================================
class LevelBouwer:
    """
    A level built facing east in a Structure of its own (the test levels): remembers the start block and gives every lane to
    level_json in the level's own frame (template coordinates minus the start block's). x is along the first lane, y up.
    (A level of the castle is built with guhrio_baan.Baanbouwer instead.)
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
    for k, half in enumerate(("lower", "upper")):                              # a door to the bonus room too
        s.set(44, y + k, Z, DEUR, {"kanaal": "5", "half": half})
    s.set(46, y, Z, GUHMBA)
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
    for x in range(31, 43):
        s.set(x, B + 1, Z, MUNT)
        if x % 2 == 1:
            s.set(x, B + 3, Z, MUNT)
    for k, half in enumerate(("lower", "upper")):
        s.set(44, B + 1 + k, Z, DEUR, {"kanaal": "5", "half": half})
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


def testslot(h):
    """
    A little level made with the lane builder in a structure of its own (32 x 15 x 13): the lane runs east along z = 1, the
    painted wall is z = 0, the camera's room z = 2..12. The game tests play it, so they test what guhrio_baan really writes:
    the start block, a coin, a ?-block, the three big vadsmunten, a Guhmba in a pen, a switch with its block, a pair of pipes
    (the second one sideways) and the flagpole.
    """
    L, HH = 30, 12
    s = h.Structure((L + 2, HH + 3, 13))
    s.fill(0, 0, 0, L + 1, 1, 12, "minecraft:smooth_stone")
    s.fill(0, 2, 1, L + 1, HH + 2, 12, AIR)
    baan = guhrio_baan.Baanbouwer(h, TESTSLOT, "SLOT", 0, 1, L, HH, (1, 2, 1), (1, 0))
    for x in range(L):
        s.set(1 + x, 1, 1, AIR)                                  # the trench under the lane
    Y = 3
    baan.grond(0, L - 1, 2, dik=3)
    baan.start(2, Y)
    baan.munt(5, Y + 1)
    baan.vraag(7, Y + 3)
    for n, x in enumerate((9, 10, 11)):
        baan.vadsmunt(x, Y, n)
    baan.blok(13, Y)
    baan.guhmba(15, Y)
    baan.blok(17, Y)
    baan.schakelaar(18, Y + 3, 0, "aan")
    baan.schakelblok(19, Y, 0, aan=False)
    baan.pijp(20, Y + 1, 5, hoog=2)
    baan.pijp(24, Y, 5, hoog=2, richting="terug")
    baan.vlag(22, Y)
    baan.mast(27, Y, hoog=5)
    baan.controleer()
    baan.stempel(s.set, s.entity, lambda _s, _y: ("minecraft:light_blue_concrete", None))
    s.save(TESTSLOT)
    level_json(h, TESTSLOT, baan.banen(), "SLOT", uitgang=baan.eigen(28, Y), ingang=baan.eigen(1, Y))


def structures(h):
    testlevel(h)
    testhoek(h)
    h.Structure((24, 12, 7)).save(TESTBAAN)
    testslot(h)
    return guhrio_kasteel.build(h)


def selfcheck(h, banen):
    A, D = h.A, h.D
    missing = [p for p in [f"{A}/blockstates/{n}.json" for n in BLOKKEN]
               + [f"{A}/guhrio_model/{n}.json" for n in guhrio_modellen.NAMEN] + [f"{h.TEX}/entity/{n}.png" for n in guhrio_modellen.NAMEN]
               + [f"{D}/guhrio_level/{n}.json" for n in (TESTLEVEL, TESTHOEK, TESTSLOT, "kasteel_duel")]
               + [f"{D}/guhrio_level/{b.id}.json" for b in banen.values()]
               + [f"{D}/structure/{n}.nbt" for n in (TESTLEVEL, TESTHOEK, TESTBAAN, TESTSLOT, "guhrio_kasteel/stuk_0_0", "guhrio_kasteel/stuk_3_3")]
               + [f"{D}/worldgen/structure/guhrio_kasteel.json", f"{D}/worldgen/structure_set/guhrio_kasteel_gegarandeerd.json"]
               if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    if len(banen) != 7:
        missing.append(f"7 lanes in the castle, found {len(banen)}")
    if missing:
        raise SystemExit(f"guhrio: missing {missing}")


# the inside of an ear in the guh's base texture (guh.png). The ears are bones of the base model, so make_guh_variants.py
# leaves them as they are: purple, also on a green Guhshi
OOR_BINNEN, OOR_GUHSHI = (160, 109, 174), (72, 148, 56)


def guhshi_oren(h):
    """
    Guhshi's ears are green inside (a darker green than his fur): the purple of the base texture is repainted in his own two
    textures (awake and asleep), which make_guh_variants.py and make_sleep_eyes.py have just written. No swatch moves and no
    other texture is touched.
    """
    for pad in (os.path.join(h.TEX, "entity", "guh_guhshi.png"), os.path.join(h.TEX, "entity", "guh_slaap", "guh_guhshi.png")):
        if not os.path.exists(pad):
            continue
        a = np.asarray(Image.open(pad).convert("RGBA")).astype(np.int32)
        verschil = a[..., :3] - np.array(OOR_BINNEN)
        mask = (np.abs(verschil).sum(-1) <= 14) & (a[..., 3] > 0)
        if not mask.any():
            continue
        a[..., :3] = np.where(mask[..., None], np.clip(np.array(OOR_GUHSHI) + verschil, 0, 255), a[..., :3])
        # (written the way the tool that made the file writes it: make_sleep_eyes.py optimises its pictures)
        Image.fromarray(a.astype(np.uint8)).save(pad, optimize="guh_slaap" in pad)


def build(h):
    textures(h)
    guhshi_oren(h)
    blocks_and_items(h)
    texts(h)
    verhaal(h)
    _b, banen = structures(h)
    # every level of the castle is walked with a jump 10 % weaker than the game's (and its route written for the game tests)
    guhrio_loop.controleer(h, banen)
    selfcheck(h, banen)
