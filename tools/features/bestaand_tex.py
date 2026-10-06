"""
bbq2 (bestaand): the textures and block / item models of the slice (see bestaand.py).

  - bestaand_vuurkorf        the bridge fire of the Spiesburcht: an iron bowl with four guh-ear knobs on a short stem; the lit
                             state (only ever shown per player, see Java Schijn) has glowing coals and campfire flames
  - bestaand_mikakruid       the weed of the pindasaus-tuintje (only ever shown per player): a thorny cross plant with eyes
  - bestaand_zielig_lantaarntje   the reward of the Wachter-guh, the soul lantern parody: a little lantern with guh ears and a
                             pitiful face (hanging or standing; "blij" after a pat)
  - bestaand_knuffelguh / _knuffelmika / _knuffelrookguh   the plush deco blocks of the Knuffelmaker-guh
  - the items bestaand_aansteekspies, bestaand_recept_lantaarn, bestaand_recept_knuffel
"""
from PIL import Image, ImageDraw

T = 16
IJZER = (58, 56, 64)
IJZER_LICHT = (92, 90, 100)
IJZER_DONKER = (36, 34, 40)


# =====================================================================================================================
# small painters
# =====================================================================================================================
def _rgba(c, a=255):
    return tuple(c[:3]) + (a,)


def _ijzer(h, seed):
    """Dark wrought iron with a lighter top edge, a dark bottom edge and a few rivets."""
    img = h.noise_tex(IJZER, 5, seed)
    px = img.load()
    for x in range(T):
        px[x, 0] = _rgba(IJZER_LICHT)
        px[x, 15] = _rgba(IJZER_DONKER)
    for (x, y) in ((2, 3), (7, 3), (12, 3), (4, 9), (10, 9), (2, 13), (13, 13)):
        px[x, y] = _rgba(IJZER_LICHT)
        px[x, y + 1] = _rgba(IJZER_DONKER)
    return img


def _kool(h, seed, gloeit):
    """The coals in the bowl seen from above: dark lumps, or glowing ones with bright cracks."""
    import random
    rng = random.Random(seed)
    basis = (150, 52, 20) if gloeit else (34, 30, 34)
    img = h.noise_tex(basis, 10, seed)
    px = img.load()
    for _ in range(26):
        x, y = rng.randrange(T), rng.randrange(T)
        w, hgt = rng.choice((1, 2, 2)), rng.choice((1, 1, 2))
        kleur = rng.choice(((255, 170, 50), (255, 120, 30), (250, 210, 90))) if gloeit else rng.choice(((54, 50, 56), (22, 20, 24), (70, 64, 70)))
        for dx in range(w):
            for dy in range(hgt):
                px[(x + dx) % T, (y + dy) % T] = _rgba(kleur)
    if not gloeit:
        for (x, y) in ((4, 5), (11, 9), (7, 12)):       # a last grey ash speck: it has been out for a while
            px[x, y] = _rgba((120, 116, 122))
    return img


def _mikakruid():
    """A thorny weed: dark purple stalks with hooked thorns, two little red-eyed buds and a sickly yellow-green leaf."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    rows = [
        "................",
        "......r..r......",
        ".....rkr.rkr....",
        "......d...d.....",
        "...t..d...d..t..",
        "....t.dd.dd.t...",
        ".t...ttd.dtt....",
        "..t....ddd...t..",
        "...tt..dld..t...",
        ".....t.dld.t....",
        "..l...ttdtt...l.",
        "...l....d....l..",
        "....ll..d..ll...",
        "......lldll.....",
        ".......ddd......",
        "........d.......",
    ]
    pal = {"d": (70, 34, 84, 255), "t": (104, 52, 120, 255), "l": (150, 160, 50, 255), "r": (220, 44, 50, 255), "k": (30, 14, 34, 255)}
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                px[x, y] = pal[ch]
    return img


def _gezicht(blij):
    """The glass of the Zielig lantaarntje (16x16, shown on an 8x8 face): a soft turquoise glow with a face. Sad: drooping
    eyes, a wobbly mouth and a tear; blij (after a pat): closed happy eyes, pink cheeks and a smile."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 255))
    px = img.load()
    for y in range(T):
        for x in range(T):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if blij:
                c = (int(250 - d * 5), int(246 - d * 8), int(170 + d * 3))
            else:
                c = (int(150 - d * 7), int(240 - d * 6), int(236 - d * 3))
            px[x, y] = tuple(max(0, min(255, v)) for v in c) + (255,)
    donker = (24, 44, 60, 255)
    d = ImageDraw.Draw(img)
    for x in (0, 15):
        d.line((x, 0, x, 15), fill=_rgba(IJZER))
    d.line((0, 0, 15, 0), fill=_rgba(IJZER_LICHT))
    d.line((0, 15, 15, 15), fill=_rgba(IJZER_DONKER))
    if blij:
        for (x0, x1) in ((3, 6), (9, 12)):                 # happy closed eyes: little arches
            d.line((x0, 7, x0 + 1, 6), fill=donker)
            d.line((x0 + 1, 6, x1 - 1, 6), fill=donker)
            d.line((x1 - 1, 6, x1, 7), fill=donker)
        for (x, y) in ((2, 9), (3, 9), (12, 9), (13, 9)):
            px[x, y] = (255, 150, 190, 255)
        d.line((6, 10, 7, 11), fill=donker)
        d.line((7, 11, 8, 11), fill=donker)
        d.line((8, 11, 9, 10), fill=donker)
    else:
        for x0 in (3, 10):                                  # big sad eyes with a shine
            d.rectangle((x0, 6, x0 + 2, 8), fill=donker)
            px[x0 + 1, 6] = (236, 252, 255, 255)
        d.line((2, 4, 5, 3), fill=donker)                   # brows that slant up towards the middle: pitiful
        d.line((10, 3, 13, 4), fill=donker)
        d.line((6, 12, 7, 11), fill=donker)                 # a wobbly, turned-down mouth
        d.line((7, 11, 8, 11), fill=donker)
        d.line((8, 11, 9, 12), fill=donker)
        for (x, y) in ((4, 9), (4, 10), (4, 11)):           # one fat tear
            px[x, y] = (90, 190, 255, 255)
        px[4, 12] = (200, 240, 255, 255)
    return img


def _glas(blij):
    img = Image.new("RGBA", (T, T), (0, 0, 0, 255))
    px = img.load()
    for y in range(T):
        for x in range(T):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            c = (int(250 - d * 6), int(240 - d * 8), int(160 + d * 2)) if blij else (int(140 - d * 6), int(232 - d * 6), int(230 - d * 3))
            px[x, y] = tuple(max(0, min(255, v)) for v in c) + (255,)
    return img


# --- the plush blocks -------------------------------------------------------------------------------------------------
# colours per plush: fur, belly, dark (eyes), cheek / accent, ear tip
KNUFFELS = {
    "knuffelguh": dict(vacht=(255, 150, 200), buik=(255, 208, 228), oog=(46, 26, 46), wang=(255, 96, 160), tip=(255, 120, 180), naad=(214, 104, 160)),
    "knuffelmika": dict(vacht=(66, 58, 70), buik=(104, 94, 108), oog=(232, 46, 50), wang=(250, 250, 246), tip=(210, 44, 50), naad=(40, 34, 44)),
    "knuffelrookguh": dict(vacht=(188, 190, 200), buik=(226, 228, 236), oog=(60, 60, 78), wang=(255, 176, 206), tip=(150, 152, 164), naad=(140, 142, 154)),
}


def _vacht(h, kleur, seed):
    return h.noise_tex(kleur, 7, seed)


def _knuffel_voor(h, naam, k, seed):
    """The front, in the block's own (x, y) space (texel (u, v) = (x, 16 - y)): the face on the head (y 7..14), the lighter
    belly (y 0..7), the paws."""
    img = _vacht(h, k["vacht"], seed)
    d = ImageDraw.Draw(img)
    px = img.load()
    d.rectangle((6, 10, 9, 14), fill=_rgba(k["buik"]))                  # belly patch
    d.line((4, 9, 11, 9), fill=_rgba(k["naad"]))                        # the neck seam
    for x in (5, 6, 9, 10):                                             # eyes (2 x 2)
        for y in (4, 5):
            px[x, y] = _rgba(k["oog"])
    if naam == "knuffelmika":
        d.line((4, 3, 6, 3), fill=_rgba(k["naad"]))                     # angry brows (embroidered, so a bit crooked)
        d.line((9, 3, 11, 3), fill=_rgba(k["naad"]))
        px[6, 4] = _rgba(k["vacht"])
        px[9, 4] = _rgba(k["vacht"])
        for x in (7, 8):                                                # two felt fangs
            px[x, 7] = _rgba(k["wang"])
        px[7, 8] = _rgba(k["wang"])
    elif naam == "knuffelrookguh":
        for x in (5, 9):                                                # big sad shiny eyes
            px[x, 4] = (240, 244, 255, 255)
        d.line((7, 7, 8, 7), fill=_rgba(k["oog"]))                      # a little "o" mouth
        d.line((7, 8, 8, 8), fill=_rgba(k["oog"]))
        for x in (4, 11):
            px[x, 6] = _rgba(k["wang"])
    else:
        px[5, 4] = (255, 255, 255, 255)                                 # a sparkle
        px[9, 4] = (255, 255, 255, 255)
        for x in (4, 11):                                               # cheeks
            px[x, 6] = _rgba(k["wang"])
            px[x, 7] = _rgba(k["wang"])
        d.line((7, 6, 8, 6), fill=_rgba(k["wang"]))                     # nose
        px[7, 7] = _rgba(k["oog"])                                      # a tiny smile
        px[8, 7] = _rgba(k["oog"])
    for x in range(3, 13):                                              # ear tips (the two rows above the head)
        px[x, 0] = _rgba(k["tip"])
    return img


def _knuffel_achter(h, k, seed):
    img = _vacht(h, k["vacht"], seed + 1)
    d = ImageDraw.Draw(img)
    d.line((8, 2, 8, 15), fill=_rgba(k["naad"]))                        # the back seam, hand-stitched
    for y in range(3, 15, 3):
        d.point((7, y), fill=_rgba(k["naad"]))
        d.point((9, y + 1), fill=_rgba(k["naad"]))
    d.rectangle((10, 12, 12, 14), fill=(250, 246, 236, 255))            # the label: "met liefde genaaid"
    d.point((11, 13), fill=(236, 70, 130, 255))
    for x in range(3, 13):
        img.putpixel((x, 0), _rgba(k["tip"]))
    return img


def _knuffel_zij(h, k, seed):
    img = _vacht(h, k["vacht"], seed + 2)
    d = ImageDraw.Draw(img)
    d.line((4, 9, 12, 9), fill=_rgba(k["naad"]))
    d.rectangle((6, 10, 8, 13), fill=_rgba(tuple(max(0, c - 18) for c in k["vacht"])))   # the arm
    for x in range(6, 10):
        img.putpixel((x, 0), _rgba(k["tip"]))
    return img


def _knuffel_boven(h, k, seed):
    img = _vacht(h, k["vacht"], seed + 3)
    d = ImageDraw.Draw(img)
    d.line((8, 4, 8, 11), fill=_rgba(k["naad"]))
    d.rectangle((3, 7, 5, 8), fill=_rgba(k["tip"]))                     # the ears seen from above
    d.rectangle((10, 7, 12, 8), fill=_rgba(k["tip"]))
    return img


def knuffel_elementen(naam):
    """The plush, sitting, facing north. Faces without an explicit uv take the block's own coordinates."""
    voor, achter, zij, boven = "#voor", "#achter", "#zij", "#boven"

    def box(frm, to):
        return {"from": frm, "to": to, "faces": {"north": {"texture": voor}, "south": {"texture": achter}, "west": {"texture": zij},
                                                 "east": {"texture": zij}, "up": {"texture": boven}, "down": {"texture": boven}}}
    els = [
        box([4, 0, 5], [12, 7, 12]),                 # body
        box([3, 7, 4], [13, 14, 12]),                # head
        box([2.5, 2, 6], [4, 6, 9]),                 # arms
        box([12, 2, 6], [13.5, 6, 9]),
    ]
    if naam == "knuffelrookguh":
        els += [box([5, 0, 12], [7, 2, 14]), box([9, 0, 12], [11, 3, 15]),     # wisps of smoke instead of feet
                box([7, 14, 7], [9, 16, 9])]                                    # one curl on top
    else:
        els += [box([4, 0, 3], [7, 2, 6]), box([9, 0, 3], [12, 2, 6])]          # feet
        if naam == "knuffelmika":
            els += [box([3, 14, 7], [5, 16, 9]), box([11, 14, 7], [13, 16, 9]),  # horns
                    box([3, 15.5, 7.5], [4, 16, 8.5]), box([12, 15.5, 7.5], [13, 16, 8.5])]
        else:
            els += [box([3, 14, 7], [6, 16, 9]), box([10, 14, 7], [13, 16, 9])]  # round guh ears
    return els


# --- items -------------------------------------------------------------------------------------------------------------
def _aansteekspies():
    """A grill skewer with a glowing coal on its tip: the Wachter-guh's lighter."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    px = img.load()
    for i in range(11):
        px[2 + i, 14 - i] = (150, 152, 164, 255)
        if i < 10:
            px[3 + i, 14 - i] = (96, 96, 108, 255)
    for (x, y) in ((1, 15), (2, 15), (1, 14)):                           # the wooden grip
        px[x, y] = (126, 80, 46, 255)
    for (x, y, c) in ((12, 2, (255, 214, 90)), (13, 2, (255, 150, 40)), (12, 3, (255, 150, 40)), (13, 3, (220, 70, 24)),
                      (11, 3, (220, 70, 24)), (12, 1, (255, 240, 170)), (13, 1, (255, 190, 70)), (14, 2, (200, 60, 20)),
                      (12, 4, (150, 44, 20)), (14, 0, (255, 220, 120)), (11, 1, (255, 190, 70))):
        px[x, y] = c + (255,)
    return img


def _recept(kleur, rand, teken):
    """A recipe card: a little sheet with a ribbon, a picture and scribbled lines."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((2, 1, 13, 14), fill=(250, 244, 226, 255), outline=(170, 150, 120, 255))
    d.rectangle((2, 1, 13, 3), fill=_rgba(kleur), outline=_rgba(rand))
    px = img.load()
    for y, row in enumerate(teken):
        for x, ch in enumerate(row):
            if ch != ".":
                px[4 + x, 5 + y] = _rgba(kleur if ch == "a" else rand if ch == "b" else (40, 30, 40))
    for x in range(4, 12, 2):
        px[x, 12] = (120, 110, 100, 255)
        px[x + 1, 12] = (120, 110, 100, 255)
    px[13, 14] = (0, 0, 0, 0)                                           # a dog-ear
    px[12, 14] = (170, 150, 120, 255)
    px[13, 13] = (170, 150, 120, 255)
    return img


LANTAARN_TEKEN = ["..bbbb..", ".b.bb.b.", ".baaaab.", ".bakkab.", ".baaaab.", "..bbbb.."]
KNUFFEL_TEKEN = [".aa..aa.", ".aaaaaa.", ".akaaka.", ".aabbaa.", "..aaaa..", ".aa..aa."]


# =====================================================================================================================
# write everything
# =====================================================================================================================
def textures(h):
    h.save(_ijzer(h, 13010), "block", "bestaand_vuurkorf_ijzer.png")
    h.save(_kool(h, 13011, False), "block", "bestaand_vuurkorf_kool.png")
    h.save(_kool(h, 13012, True), "block", "bestaand_vuurkorf_gloed.png")
    h.save(_mikakruid(), "block", "bestaand_mikakruid.png")
    h.save(_ijzer(h, 13013), "block", "bestaand_zielig_lantaarntje_ijzer.png")
    for blij in (False, True):
        s = "_blij" if blij else ""
        h.save(_gezicht(blij), "block", f"bestaand_zielig_lantaarntje_gezicht{s}.png")
        h.save(_glas(blij), "block", f"bestaand_zielig_lantaarntje_glas{s}.png")
    for i, (naam, k) in enumerate(KNUFFELS.items()):
        seed = 13020 + i * 10
        h.save(_knuffel_voor(h, naam, k, seed), "block", f"bestaand_{naam}_voor.png")
        h.save(_knuffel_achter(h, k, seed), "block", f"bestaand_{naam}_achter.png")
        h.save(_knuffel_zij(h, k, seed), "block", f"bestaand_{naam}_zij.png")
        h.save(_knuffel_boven(h, k, seed), "block", f"bestaand_{naam}_boven.png")
    h.save(_aansteekspies(), "item", "bestaand_aansteekspies.png")
    h.save(_recept((90, 214, 220), (36, 70, 96), LANTAARN_TEKEN), "item", "bestaand_recept_lantaarn.png")
    h.save(_recept((255, 150, 200), (214, 80, 150), KNUFFEL_TEKEN), "item", "bestaand_recept_knuffel.png")


def _vol(tex):
    """A face that shows the whole 16x16 texture."""
    return {"texture": tex, "uv": [0, 0, 16, 16]}


def vuurkorf_model(h):
    A, w = h.A, h.w
    ijzer, kool = "#ijzer", "#kool"

    def box(frm, to, faces=None):
        return h.el(frm, to, ijzer, faces=faces)
    basis = [
        box([6, 0, 6], [10, 1, 10]),                                    # the foot
        box([7, 1, 7], [9, 4, 9]),                                      # the stem
        box([3, 4, 3], [13, 5, 13]),                                    # the bottom of the bowl
        box([2, 5, 2], [14, 10, 3]), box([2, 5, 13], [14, 10, 14]),     # its walls
        box([2, 5, 3], [3, 10, 13]), box([13, 5, 3], [14, 10, 13]),
        box([1, 9, 1], [4, 12, 4]), box([12, 9, 1], [15, 12, 4]),       # four guh-ear knobs on the corners
        box([1, 9, 12], [4, 12, 15]), box([12, 9, 12], [15, 12, 15]),
        {"from": [3, 5, 3], "to": [13, 8.5, 13], "faces": {"up": _vol(kool)}},     # the coals
    ]
    vlam = {"texture": "#vuur", "uv": [0, 0, 16, 16]}
    vlammen = [
        {"from": [1.5, 8, 8], "to": [14.5, 23, 8], "shade": False,
         "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}, "faces": {"north": vlam, "south": vlam}},
        {"from": [8, 8, 1.5], "to": [8, 23, 14.5], "shade": False,
         "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}, "faces": {"west": vlam, "east": vlam}},
    ]
    w(f"{A}/models/block/bestaand_vuurkorf.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"particle": "guhs:block/bestaand_vuurkorf_ijzer", "ijzer": "guhs:block/bestaand_vuurkorf_ijzer",
                     "kool": "guhs:block/bestaand_vuurkorf_kool"}, "elements": basis})
    w(f"{A}/models/block/bestaand_vuurkorf_aan.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"particle": "guhs:block/bestaand_vuurkorf_ijzer", "ijzer": "guhs:block/bestaand_vuurkorf_ijzer",
                     "kool": "guhs:block/bestaand_vuurkorf_gloed", "vuur": "minecraft:block/campfire_fire"}, "elements": basis + vlammen})
    w(f"{A}/blockstates/bestaand_vuurkorf.json", {"variants": {
        f"lit={str(lit).lower()},nr={nr}": {"model": "guhs:block/bestaand_vuurkorf" + ("_aan" if lit else "")}
        for lit in (False, True) for nr in range(4)}})


def mikakruid_model(h):
    A, w = h.A, h.w
    w(f"{A}/models/block/bestaand_mikakruid.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                     "textures": {"cross": "guhs:block/bestaand_mikakruid"}})
    w(f"{A}/blockstates/bestaand_mikakruid.json", {"variants": {"": {"model": "guhs:block/bestaand_mikakruid"}}})


def lantaarntje_model(h):
    A, w = h.A, h.w

    def elementen(dy, ketting):
        ijzer = "#ijzer"

        def box(frm, to):
            return h.el([frm[0], frm[1] + dy, frm[2]], [to[0], to[1] + dy, to[2]], ijzer)
        gezicht = _vol("#gezicht")
        els = [
            box([5, 0, 5], [11, 1, 11]),                                # the base
            {"from": [4, 1 + dy, 4], "to": [12, 9 + dy, 12], "faces": {"north": gezicht, "south": gezicht, "west": gezicht, "east": gezicht,
                                                                        "up": _vol("#glas"), "down": _vol("#glas")}},
            box([3.5, 9, 3.5], [12.5, 10.5, 12.5]),                     # the cap
            box([6.5, 10.5, 6.5], [9.5, 11.5, 9.5]),                    # the knob
            box([3.5, 10.5, 7], [6, 13, 9]), box([10, 10.5, 7], [12.5, 13, 9]),   # two guh ears
        ]
        if ketting:
            els.append(h.el([7.25, 11.5 + dy, 7.25], [8.75, 16, 8.75], ijzer))
        return els
    for blij in (False, True):
        s = "_blij" if blij else ""
        tex = {"particle": "guhs:block/bestaand_zielig_lantaarntje_ijzer", "ijzer": "guhs:block/bestaand_zielig_lantaarntje_ijzer",
               "gezicht": f"guhs:block/bestaand_zielig_lantaarntje_gezicht{s}", "glas": f"guhs:block/bestaand_zielig_lantaarntje_glas{s}"}
        w(f"{A}/models/block/bestaand_zielig_lantaarntje{s}.json", {"parent": "minecraft:block/block", "textures": tex,
                                                                   "elements": elementen(0, False)})
        w(f"{A}/models/block/bestaand_zielig_lantaarntje_hangend{s}.json", {"parent": "minecraft:block/block", "textures": tex,
                                                                           "elements": elementen(2, True)})
    w(f"{A}/blockstates/bestaand_zielig_lantaarntje.json", {"variants": {
        f"blij={str(blij).lower()},hanging={str(hangt).lower()}": {
            "model": "guhs:block/bestaand_zielig_lantaarntje" + ("_hangend" if hangt else "") + ("_blij" if blij else "")}
        for blij in (False, True) for hangt in (False, True)}})
    w(f"{A}/models/item/bestaand_zielig_lantaarntje.json", {"parent": "guhs:block/bestaand_zielig_lantaarntje"})


def knuffel_modellen(h):
    A, w = h.A, h.w
    for naam in KNUFFELS:
        id = f"bestaand_{naam}"
        w(f"{A}/models/block/{id}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": f"guhs:block/{id}_zij", "voor": f"guhs:block/{id}_voor", "achter": f"guhs:block/{id}_achter",
                         "zij": f"guhs:block/{id}_zij", "boven": f"guhs:block/{id}_boven"},
            "elements": knuffel_elementen(naam)})
        w(f"{A}/blockstates/{id}.json", {"variants": h.facing_states(id)})
        w(f"{A}/models/item/{id}.json", {"parent": f"guhs:block/{id}"})


def models(h):
    vuurkorf_model(h)
    mikakruid_model(h)
    lantaarntje_model(h)
    knuffel_modellen(h)
    for item in ("bestaand_aansteekspies", "bestaand_recept_lantaarn", "bestaand_recept_knuffel"):
        h.item_model(item)
    # the skewer is held like a tool
    h.item_model("bestaand_aansteekspies", parent="minecraft:item/handheld")


def build(h):
    textures(h)
    models(h)
