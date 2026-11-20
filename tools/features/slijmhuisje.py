"""
1.10.10: Het Slijmhuisje (Java: feature/slijmhuisje). A sweet little house of 3 x 3 (about 3 high) for one guh slime; on 4
vadskracht the slime makes a guh slimeball every 45 seconds into a bowl of 16. Plus the item "Opgepakte guh-slime" and the
chore "Slijmpjes verzorgen".

This module writes:
  * MECHANICS (stable): the loot table, the recipe, the tags (guhs:vadskracht for the hover readout, mineable/axe), the two
    advancements (quest/ + techniek/tech_machines_slijmhuisje), the client item definitions and every text (Dutch here,
    English in TEKSTEN too, and in tools/lang/en/c511_slijmhuisje.json, which this module writes as well);
  * THE LOOK ("Zeepbelkoepel", the user's pick): every texture, block model and blockstate, in the section "the look"
    below and nothing else (which block draws what: see there). The renderer (client/SlijmhuisjeRenderer.java) draws the
    slime on the cushion and the slimeballs in the bowl at the positions noted there.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/slijmhuisje.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

STATEN = ("slaapt", "werkt", "vol")
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
EN_FILE = os.path.join("tools", "lang", "en", "c511_slijmhuisje.json")

# =====================================================================================================================
# texts: key -> (Dutch, English)
# =====================================================================================================================
G = "gui.guhs.slijmhuisje."
O = "gui.guhs.huisje.overzicht.klus.slijmpjes."
TEKSTEN = {
    "block.guhs.slijmhuisje": ("Slijmhuisje", "Slime House"),
    "block.guhs.slijmhuisje.lore": ("Een lief huisje voor één guh-slime. Op 4 vadskracht maakt hij elke 45 tellen een guh-slijmbal in zijn schaaltje. Blub!",
                                    "A sweet little house for one guh slime. On 4 chonk power it makes a guh slimeball every 45 seconds, into its little bowl. Blub!"),
    "block.guhs.slijmhuisje_deel": ("Slijmhuisje", "Slime House"),
    "item.guhs.opgepakte_guh_slime": ("Opgepakte guh-slime", "Picked-up Guh Slime"),
    "item.guhs.opgepakte_guh_slime.tooltip": ("Rechtsklik op een blok: hij stuitert eruit. Rechtsklik op een Slijmhuisje: hij trekt erin.",
                                              "Right-click a block: it bounces out. Right-click a Slime House: it moves in."),
    "item.guhs.opgepakte_guh_slime.grootte.1": ("Grootte: klein slijmpje (1)", "Size: little slime (1)"),
    "item.guhs.opgepakte_guh_slime.grootte.2": ("Grootte: middelste slijm (2)", "Size: medium slime (2)"),
    "item.guhs.opgepakte_guh_slime.grootte.4": ("Grootte: dikke vadsslijm (4)", "Size: big chonky slime (4)"),
    G + "opgepakt": ("Je hebt %s opgepakt. Wiebel wiebel!", "You picked up %s. Wobble wobble!"),
    G + "neergezet": ("%s stuitert er weer vrolijk vandoor.", "%s happily bounces off again."),
    G + "erin": ("%s woont nu in het Slijmhuisje. Njeg, wat knus!", "%s lives in the Slime House now. Aww, so cosy!"),
    G + "bezet": ("Hier woont al een slijmpje. Eén per huisje, anders wordt het plakkerig!",
                  "A slime lives here already. One per house, or it gets sticky!"),
    G + "eruit": ("%s komt even naar buiten.", "%s comes out for a bit."),
    G + "niemand": ("Er woont nog niemand. Pak een guh-slime op (sluip + rechtsklik) en breng hem hier.",
                    "Nobody lives here yet. Pick up a guh slime (sneak + right-click) and bring it here."),
    G + "gepakt": ("Je pakt %s guh-slijmballen uit het schaaltje.", "You take %s guh slimeballs from the little bowl."),
    G + "leeg_klik": ("Het huisje is leeg. Breng er een guh-slime naartoe!", "The house is empty. Bring a guh slime here!"),
    G + "slaapt_klik": ("%s ligt te slapen. Zonder vadskracht maakt hij niks, zzz...", "%s is asleep. Without chonk power it makes nothing, zzz..."),
    G + "schaal_leeg": ("Het schaaltje is nog leeg. %s stuitert: nog %s tellen tot de volgende slijmbal.",
                        "The little bowl is still empty. %s is bouncing: %s more seconds until the next slimeball."),
    G + "leeg": ("Er woont nog geen slijmpje in dit huisje", "No slime lives in this house yet"),
    G + "bewoner": ("Hier woont %s (grootte %s)", "%s lives here (size %s)"),
    G + "schaal_vol": ("Het schaaltje is vol (%s): tevreden wacht hij tot iemand het leeghaalt", "The little bowl is full (%s): it waits contentedly until someone empties it"),
    G + "slaapt": ("Slaapt (geen vadskracht). In het schaaltje: %s/%s", "Asleep (no chonk power). In the little bowl: %s/%s"),
    G + "maakt": ("In het schaaltje: %s/%s. Volgende slijmbal over %s tellen", "In the little bowl: %s/%s. Next slimeball in %s seconds"),
    "advancements.guhs.techniek.tech_machines_slijmhuisje.title": ("Thuis is waar de slijm is", "Home Is Where the Slime Is"),
    "advancements.guhs.techniek.tech_machines_slijmhuisje.description": ("Laat een guh-slime in een Slijmhuisje een slijmbal maken",
                                                                          "Let a guh slime in a Slime House make a slimeball"),
    # the chore
    "gui.guhs.klus.slijmpjes": ("Slijmpjes verzorgen", "Caring for Slimes"),
    "gui.guhs.klus.slijmpjes.tip": ("Loopt naar je Slijmhuisje, aait het slijmpje en brengt de guh-slijmballen uit het schaaltje naar de kist. (Guhs)",
                                    "Walks to your Slime House, pets the slime and brings the guh slimeballs from the little bowl to the chest. (Guhs)"),
    O + "ophalen": ("Slijmhuisjes met slijmballen in het schaaltje: %s", "Slime Houses with slimeballs in the little bowl: %s"),
    O + "geen_slijm": ("Slijmhuisjes zonder slijmpje: %s. Breng er een guh-slime naartoe", "Slime Houses without a slime: %s. Bring a guh slime there"),
    O + "geen_kracht": ("Slijmhuisjes waar het slijmpje slaapt (geen vadskracht): %s", "Slime Houses where the slime is asleep (no chonk power): %s"),
    O + "maakt": ("Nu nog niets op te halen. Slijmhuisjes waar een slijmpje stuitert: %s", "Nothing to fetch yet. Slime Houses where a slime is bouncing: %s"),
    O + "geen": ("Geen Slijmhuisje van jou", "No Slime House of yours"),
    O + "nodig": ("een Slijmhuisje dat jij zelf hebt neergezet in de klus-area, met een guh-slime erin en vadskracht. Liggen er een paar "
                  "slijmballen in het schaaltje, dan aaien ze het slijmpje en brengen ze de slijmballen naar de kist naast het huisje of de Bank Guh.",
                  "a Slime House you placed yourself in the chore area, with a guh slime in it and chonk power. When there are a few "
                  "slimeballs in the little bowl, they pet the slime and bring the slimeballs to the chest next to the house or the Bank Guh."),
    "gui.guhs.dagboek.eerste.klusjes_slijmpjes": ("Slijmvriendje", "Slime Buddy"),
    "gui.guhs.dagboek.eerste.klusjes_slijmpjes.tekst": ("Ik heb een slijmpje geaaid. Hij voelde koud en wiebelig. Hij zei blub. Ik zei njeg. Nu zijn wij vrienden.",
                                                        "I petted a slime. It felt cold and wobbly. It said blub. I said nyeg. Now we're friends."),
    "gui.guhs.wistjedat.klusjes.slijmpjes": ("Wist je dat er bij %s een slijmpje in een zeepbel woont? Ik aai hem elke dag. Hij is plakkerig, maar wel lief.",
                                             "Did you know a slime lives in a soap bubble at %s? I pet it every day. It's sticky, but sweet."),
}

# =====================================================================================================================
# the look: "Zeepbelkoepel" (the user's pick; concept render guhs_winter/slijmhuisje_renders/A_zeepbelkoepel_*.png, its
# voxel source _bron/a_zeepbelkoepel.py). Coordinates: F = the house's own pixels, facing north (the door at z 0), x/z 0..48
# over the 3 x 3, y 0 = the ground. Which block draws what (MachineDeelBlock offsets: the kern is F x 16..32, z 0..16):
#   kern (slijmhuisje_<snoet>)        the snoet + the door (a decal on the front), the cream door frame, the mint bowl in front
#                                     of the door (it sticks out of the 3 x 3 on purpose, as in the render)
#   dy 0, middle (slijmhuisje_voet)   the whole base: mint plinth, pink body with cream drips, cream frosting rim with pink
#                                     dots, the pistachio floor with its cream seat ring, four candy knobs, the lavender cushion
#   dy 2, middle (slijmhuisje_koepel) the soap-bubble dome (translucent, with a glint) and the pink flower on top
# The renderer (client/SlijmhuisjeRenderer.java) puts the slime on the cushion (top at F y 28, middle F 24/24) and the pink
# slimeballs in the bowl. The base and the dome are round, so they never turn; the kern turns with its facing.
# =====================================================================================================================
H = 24                      # the base: plinth y 0..3, body to 21, frosting rim 21..24
KOEPEL_RX, KOEPEL_RY = 23.0, 27.0
KOEPEL_RES = 2              # the dome is built from cells of this many pixels (fewer elements)
BEL = (226, 222, 252)       # the soap film, faintly lilac
BLOEM, BLOEM_LICHT, GOUD = (255, 150, 196), (255, 196, 222), (250, 214, 96)
STEEL, BLAD = (108, 186, 112), (140, 210, 130)
HART, DEURRAND = (255, 196, 222), (250, 240, 220)
OOG, GLANS, WANG, MOND = (52, 34, 58), (255, 255, 255), (244, 140, 172), (120, 50, 80)
SNOEP = (255, 160, 200)


def _bron(a, pad):
    """A texture of the mod as RGBA (the plaster of the bouwhulp, the bounce cushion...)."""
    return Image.open(os.path.join(a, "textures", *pad.split("/")) + ".png").convert("RGBA")


def _tegel(tex, n=64):
    """A 16 x 16 texture tiled over n x n."""
    out = Image.new("RGBA", (n, n))
    for x in range(0, n, 16):
        for y in range(0, n, 16):
            out.paste(tex, (x, y))
    return out


def _effen(kleur):
    return Image.new("RGBA", (16, 16), tuple(kleur[:3]) + (255,))


def _rond_vierkant(x, z, lo, hi, r):
    """Is pixel (x, z) (its middle) inside the square lo..hi with corners rounded by r?"""
    px, pz = x + .5, z + .5
    cx, cz = min(max(px, lo + r), hi - r), min(max(pz, lo + r), hi - r)
    return math.hypot(px - cx, pz - cz) <= r


def _afstand(x, z):
    return math.hypot(x + .5 - 24, z + .5 - 24)


def _in_deur(x, y, r=4.6, cy=6.0, cx=24.0):
    return abs(x - cx) <= r and (y <= cy or math.hypot(x - cx, y - cy) <= r)


def texturen(a):
    """name -> image. 'zij', 'boven' and 'kussen' are 64 x 64 canvases mapped onto the house's pixels (F / 4, see _uv_f):
    zij by height (rows: y = 31 - row) and along the face, boven by x/z from above."""
    roze, aqua, vanille = _bron(a, "block/bouwhulp/pleister_roze"), _bron(a, "block/bouwhulp/pleister_aqua"), _bron(a, "block/bouwhulp/pleister_vanille")
    framboos, lavendel = _bron(a, "block/bouwhulp/pleister_framboos"), _bron(a, "block/stuiterkussen_lavendel")
    t = {}
    # the sides: mint plinth (y 0..2), pink body (3..20) with cream drips at y 20, cream rim (21..23) with pink dots
    zij = _tegel(roze)
    mint, room = _tegel(aqua), _tegel(vanille)
    px, mpx, rpx = zij.load(), mint.load(), room.load()
    for row in range(64):
        y = 31 - row
        for c in range(64):
            if y <= 2 or y > 31:
                px[c, row] = mpx[c, row]
            elif y >= 21:
                px[c, row] = rpx[c, row]
                if y == 22 and c % 8 == 3:
                    px[c, row] = SNOEP + (255,)
            elif y == 20 and (c // 3) % 2 == 0:
                px[c, row] = rpx[c, row]
    t["zij"] = zij
    # the top: cream rim, pistachio floor under the dome, a cream seat ring, pink sprinkles on the rim
    boven = _tegel(vanille)
    bpx = boven.load()
    pist = _tegel(aqua).point(lambda v: min(255, int(v * 1.1)))
    ppx = pist.load()
    for x in range(48):
        for z in range(48):
            d = _afstand(x, z)
            if d <= 20.5:
                bpx[x, z] = ppx[x, z]
            elif d <= 23.2:
                r_, g_, b_, _ = bpx[x, z]
                bpx[x, z] = (min(255, r_ + 6), min(255, g_ + 6), min(255, b_ + 8), 255)
            elif (x * 7 + z * 13) % 23 == 0:
                bpx[x, z] = SNOEP + (255,)
    t["boven"] = boven
    t["kussen"] = _tegel(lavendel)
    # the front decal (32 x 32, transparent; column = viewer's left to right over F x 40..8, row = 31 - y): the round door
    # (raspberry, a heart, a gold knob) and the plantagebak snoet above it, per state
    for staat in STATEN:
        img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        d = img.load()
        fpx = framboos.load()
        def zet(c, y, kleur):
            if 0 <= c < 32 and 0 <= 31 - y < 32:
                d[c, 31 - y] = tuple(kleur[:3]) + (255,)
        for c in range(32):
            for y in range(14):
                if _in_deur(c + 8 + .5, y + .5):
                    zet(c, y, fpx[c % 16, (31 - y) % 16])
        # (columns: the door's middle is column 15.5; the voxel source's x minus 8)
        for (x, y) in ((21, 8), (22, 8), (24, 8), (25, 8), (20, 7), (21, 7), (22, 7), (23, 7), (24, 7), (25, 7), (26, 7),
                       (21, 6), (22, 6), (23, 6), (24, 6), (25, 6), (22, 5), (23, 5), (24, 5), (23, 4)):
            zet(x - 8, y, HART)
        zet(18, 3, GOUD)                                       # the little gold door knob
        # the snoet: eyes 3 x 4 at columns 7..9 and 22..24 (bottom y 15), cheeks under them, the mouth in the middle
        for c0, wx in ((7, 5), (22, 24)):
            if staat == "slaapt":                              # closed: a little line, no shine
                for i in range(3):
                    zet(c0 + i, 15, OOG)
            elif staat == "vol":                               # content: happy "^" eyes
                for dc, y in ((0, 15), (0, 16), (1, 17), (2, 16), (2, 15)):
                    zet(c0 + dc, y, OOG)
            else:                                              # happy: big round open eyes with a shine
                for i in range(3):
                    for j in range(4):
                        if (i, j) not in ((0, 0), (2, 0), (0, 3), (2, 3)):
                            zet(c0 + i, 15 + j, OOG)
                zet(c0, 17, GLANS)
            if staat != "slaapt":
                for i in range(3):
                    zet(wx + i, 14, WANG)
        if staat == "slaapt":
            zet(15, 12, MOND), zet(16, 12, MOND)
        else:
            l, r = (12, 19) if staat == "vol" else (13, 18)    # a smile; content = a wider one
            zet(l, 13, MOND), zet(r, 13, MOND)
            for c in range(l + 1, r):
                zet(c, 12, MOND)
        t[f"voor_{staat}"] = img
    t["room"] = _effen(DEURRAND)
    t["snoep"] = _effen(SNOEP)
    t["schaal"] = aqua
    # the soap film and its glint (forced translucent in the models)
    bel = Image.new("RGBA", (16, 16), BEL + (58,))
    bpx = bel.load()
    for x in range(16):
        for y in range(16):
            if (x + y) % 7 == 0:
                bpx[x, y] = (236, 246, 255, 70)
    t["bel"] = bel
    t["glim"] = Image.new("RGBA", (16, 16), (255, 255, 255, 185))
    for naam, kleur in (("bloem", BLOEM), ("bloem_licht", BLOEM_LICHT), ("goud", GOUD), ("steel", STEEL), ("blad", BLAD)):
        t[naam] = _effen(kleur)
    # the slime: the real guh slime texture (the renderer builds the slime model from it like the entity's)
    t["slijm"] = _bron(a, "entity/guh_slime")
    t["balletje"] = _effen((236, 120, 176))
    t["balletje"].putpixel((0, 0), (255, 196, 228, 255))
    # the item: a little pink slime cube in your hand
    item = np.zeros((16, 16, 4), np.uint8)
    for y in range(4, 15):
        for x in range(2, 14):
            item[y, x] = (240, 140, 190, 255)
    item[4, 2:14, :3] = item[14, 2:14, :3] = item[4:15, 2, :3] = item[4:15, 13, :3] = (190, 80, 140)
    item[6:12, 4:12, :3] = (226, 110, 170)
    item[7:9, 5, :3] = item[7:9, 10, :3] = (58, 28, 60)
    item[7, 5, :3] = item[7, 10, :3] = (255, 255, 255)
    item[11, 7:9, :3] = (150, 60, 110)
    item[5, 3:6, :3] = (255, 210, 232)
    t["item/opgepakte_guh_slime"] = Image.fromarray(item)
    return t


VLAKKEN = ("north", "south", "west", "east", "up", "down")


def _uv_f(vlak, f, t):
    """uv on a 64 x 64 canvas mapped onto the house's pixels (F): 4 pixels a uv unit, vertical faces by height."""
    (x1, y1, z1), (x2, y2, z2) = f, t
    uv = {"north": (48 - x2, 32 - y2, 48 - x1, 32 - y1), "south": (x1, 32 - y2, x2, 32 - y1),
          "west": (z1, 32 - y2, z2, 32 - y1), "east": (48 - z2, 32 - y2, 48 - z1, 32 - y1),
          "up": (x1, z1, x2, z2), "down": (x1, 48 - z2, x2, 48 - z1)}[vlak]
    return [round(v / 4, 4) for v in uv]


def _el(f, t, tex, vlakken=VLAKKEN, off=(0, 0, 0), uv="f"):
    """An element given in F pixels, written in the model's own pixels (F - off). tex: one texture or {face: texture};
    uv "f" = the F-mapped canvas, "vol" = the whole 16 x 16 texture, or a list."""
    faces = {}
    for v in vlakken:
        tx = tex.get(v, tex.get("*")) if isinstance(tex, dict) else tex
        if tx is None:
            continue
        u = _uv_f(v, f, t) if uv == "f" else [0, 0, 16, 16] if uv == "vol" else uv
        faces[v] = {"texture": tx, "uv": u}
    return {"from": [round(f[i] - off[i], 4) for i in range(3)], "to": [round(t[i] - off[i], 4) for i in range(3)], "faces": faces}


def _rechthoeken(cellen):
    """A set of (x, z) cells as few rectangles (x1, z1, x2, z2), exclusive ends: row runs, merged down when equal."""
    rijen = {}
    for z in sorted({c[1] for c in cellen}):
        xs = sorted(x for x, zz in cellen if zz == z)
        runs, start = [], None
        for i, x in enumerate(xs):
            if start is None:
                start = x
            if i + 1 == len(xs) or xs[i + 1] != x + 1:
                runs.append((start, x + 1))
                start = None
        rijen[z] = runs
    open_, uit = {}, []
    for z in sorted(rijen):
        nieuw = {}
        for run in rijen[z]:
            if run in open_ and open_[run][1] == z:
                nieuw[run] = (open_[run][0], z + 1)
            else:
                nieuw[run] = (z, z + 1)
        for run, (z1, z2) in open_.items():
            if run not in nieuw or nieuw[run][0] != z1:
                uit.append((run[0], z1, run[1], z2))
        open_ = nieuw
    uit += [(run[0], z1, run[1], z2) for run, (z1, z2) in open_.items()]
    return uit


def _voet():
    """The base (block dy 0, middle: off F 16, 0, 16)."""
    off = (16, 0, 16)
    els = []
    lijf = {(x, z) for x in range(48) for z in range(48) if _rond_vierkant(x, z, 1, 47, 6)}
    for x1, z1, x2, z2 in _rechthoeken(lijf):               # plinth + body: the canvas colours them by height
        els.append(_el((x1, 0, z1), (x2, 21, z2), "#zij", ("north", "south", "west", "east"), off))
    rand = {(x, z) for x in range(48) for z in range(48) if _rond_vierkant(x, z, 0, 48, 7)}
    for x1, z1, x2, z2 in _rechthoeken(rand):               # the frosting rim with the floor painted on top
        els.append(_el((x1, 21, z1), (x2, H, z2), {"up": "#boven", "down": "#room", "*": "#zij"}, off=off))
    ring = {(x, z) for x in range(48) for z in range(48) if 20.5 < _afstand(x, z) <= 23.2}
    for x1, z1, x2, z2 in _rechthoeken(ring):               # the cream seat ring the dome stands in
        els.append(_el((x1, H, z1), (x2, H + 1, z2), "#room", ("north", "south", "west", "east", "up"), off, uv=[0, 0, 1, 1]))
    for x, z in ((3, 3), (43, 3), (3, 43), (43, 43)):       # four candy knobs on the corners
        els.append(_el((x, H, z), (x + 2, H + 2, z + 2), "#snoep", ("north", "south", "west", "east", "up"), off, uv=[0, 0, 2, 2]))
    kussen = {(x, z) for x in range(48) for z in range(48) if _afstand(x, z) <= 10.5}
    for x1, z1, x2, z2 in _rechthoeken(kussen):             # the lavender cushion (a puffed middle) and its button
        els.append(_el((x1, H, z1), (x2, H + 3, z2), "#kussen", ("north", "south", "west", "east", "up"), off))
    midden = {(x, z) for x in range(48) for z in range(48) if _afstand(x, z) <= 8}
    for x1, z1, x2, z2 in _rechthoeken(midden):
        els.append(_el((x1, H + 3, z1), (x2, H + 4, z2), "#kussen", ("north", "south", "west", "east", "up"), off))
    els.append(_el((23.5, H + 4, 23.5), (24.5, H + 4.6, 24.5), "#room", ("north", "south", "west", "east", "up"), off, uv=[0, 0, 1, 1]))
    return els


def _kern_delen():
    """The kern's own parts (F, off 16, 0, 0): the front decal, the cream door frame, the mint bowl in front of the door."""
    off = (16, 0, 0)
    els = [_el((8, 0, 0.97), (40, 32, 0.97), "#voor", ("north",), off, uv=[0, 0, 16, 16])]
    lijst = {(x, y) for x in range(16, 33) for y in range(14) if _in_deur(x + .5, y + .5, r=5.9) and not _in_deur(x + .5, y + .5)}
    for x1, y1, x2, y2 in _rechthoeken(lijst):              # (x, y) cells here: the frame stands 1 px proud of the body
        els.append(_el((x1, y1, 0), (x2, y2, 1), "#room", ("north", "west", "east", "up", "down"), off, uv=[0, 0, 1, 1]))
    bak = lambda x, z: math.hypot(x + .5 - 8, z + .5 + 10)  # the bowl: middle at kern x 8, z -10 (F 24, -10), radius 5
    vloer = {(x, z) for x in range(3, 13) for z in range(-15, -5) if bak(x, z) <= 5}
    wand = {c for c in vloer if bak(*c) > 3.3}
    for x1, z1, x2, z2 in _rechthoeken({(x + 16, z) for x, z in vloer}):
        els.append(_el((x1, 0, z1), (x2, 1, z2), "#schaal", ("north", "south", "west", "east", "up"), off, uv=[0, 0, 4, 4]))
    for x1, z1, x2, z2 in _rechthoeken({(x + 16, z) for x, z in wand}):
        els.append(_el((x1, 1, z1), (x2, 2, z2), "#schaal", ("north", "south", "west", "east"), off, uv=[4, 4, 8, 5]))
        els.append(_el((x1, 2, z1), (x2, 3, z2), "#room", ("north", "south", "west", "east", "up"), off, uv=[0, 0, 1, 1]))
    return els


def _koepel():
    """The soap-bubble dome and the flower on top (block dy 2, middle: off F 16, 32, 16). The outer skin of a voxel
    ellipsoid (cells of KOEPEL_RES px), greedy-merged into flat faces; the glint where the light catches it."""
    off, n = (16, 32, 16), KOEPEL_RES
    nx_, ny_ = 48 // n, int(KOEPEL_RY) // n + 1

    def binnen(i, j, k):
        if not (0 <= i < nx_ and 0 <= k < nx_ and j >= 0):
            return False
        x, y, z = i * n + n / 2, H + j * n + n / 2, k * n + n / 2
        return ((x - 24) / KOEPEL_RX) ** 2 + ((y - H) / KOEPEL_RY) ** 2 + ((z - 24) / KOEPEL_RX) ** 2 <= 1

    def glimt(i, j, k):
        x, y, z = i * n + n / 2 - 24, j * n + n / 2, k * n + n / 2 - 24
        v = (x / KOEPEL_RX, y / KOEPEL_RY, z / KOEPEL_RX)
        ln = math.sqrt(sum(c * c for c in v)) or 1
        g = (0.5 * v[0] + 0.78 * v[1] - 0.38 * v[2]) / ln     # upper left as you look at the door
        return 0.955 < g < 0.982 or g > 0.994

    richtingen = {"north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1), "up": (1, 1)}
    els = []
    for vlak, (as_, s) in richtingen.items():
        masker = {}
        for i in range(nx_):
            for j in range(ny_):
                for k in range(nx_):
                    if not binnen(i, j, k):
                        continue
                    nb = [i, j, k]
                    nb[as_] += s
                    if binnen(*nb):
                        continue
                    vlakpos = (i, j, k)[as_]
                    a, b = [c for idx, c in enumerate((i, j, k)) if idx != as_]
                    masker.setdefault((vlakpos, "#glim" if glimt(i, j, k) else "#bel"), set()).add((a, b))
        for (pos, tex), cellen in masker.items():
            for a1, b1, a2, b2 in _rechthoeken(cellen):
                lo, hi = [0, 0, 0], [0, 0, 0]
                vrij = [idx for idx in range(3) if idx != as_]
                lo[vrij[0]], hi[vrij[0]] = a1 * n, a2 * n
                lo[vrij[1]], hi[vrij[1]] = b1 * n, b2 * n
                vlak_c = (pos + (1 if s > 0 else 0)) * n
                lo[as_] = hi[as_] = vlak_c
                lo[1] += H
                hi[1] += H
                els.append(_el(tuple(lo), tuple(hi), tex, (vlak,), off, uv="vol"))
    top = H + int(KOEPEL_RY)
    bloem = [((23, top - 1, 23), (25, top + 2, 25), "#steel"), ((21, top + 1, 23), (23, top + 2, 25), "#blad"),
             ((23, top + 2, 23), (25, top + 4, 25), "#goud"), ((23, top + 4, 23), (25, top + 5, 25), "#bloem_licht")]
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        bloem.append(((23 + dx, top + 2, 23 + dz), (25 + dx, top + 4, 25 + dz), "#bloem"))
    for f, t, tex in bloem:
        els.append(_el(f, t, tex, off=off, uv=[0, 0, 2, 2]))
    return els


def _tex(*namen, doorzichtig=()):
    out = {"particle": "guhs:block/slijmhuisje_zij"}
    for naam in namen:
        sprite = f"guhs:block/slijmhuisje_{naam}"
        out[naam] = {"sprite": sprite, "force_translucent": True} if naam in doorzichtig else sprite
    return out


def _model(textures, elements):
    """A block model in the 26.1 shape (no render_type: 26.1 picks the layer from the texture; the soap film is forced
    translucent, what tools/mc26.py makes of render_type translucent). No ambient occlusion: the parts reach far out of
    their block."""
    return {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": textures, "elements": elements}


def _slijm_box(van, maat, uvpos, tex="#slijm"):
    """A box of the guh slime's entity model (texture 64 x 32: 4 px a uv unit across, 2 down), front = north."""
    (u, v), (w, h, d) = uvpos, maat
    reg = {"up": (u + d, v, u + d + w, v + d), "down": (u + d + w, v, u + d + 2 * w, v + d),
           "west": (u, v + d, u + d, v + d + h), "north": (u + d, v + d, u + d + w, v + d + h),
           "east": (u + d + w, v + d, u + 2 * d + w, v + d + h), "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}
    faces = {k: {"texture": tex, "uv": [r[0] / 4, r[1] / 2, r[2] / 4, r[3] / 2]} for k, r in reg.items()}
    return {"from": list(van), "to": [van[0] + w, van[1] + h, van[2] + d], "faces": faces}


def modellen():
    """path under models/ -> model json."""
    m = {}
    for staat in STATEN:
        tx = _tex("room", "schaal")
        tx["voor"] = f"guhs:block/slijmhuisje_voor_{staat}"
        m[f"block/slijmhuisje_{staat}"] = _model(tx, _kern_delen())
    m["block/slijmhuisje_voet"] = _model(_tex("zij", "boven", "room", "snoep", "kussen"), _voet())
    m["block/slijmhuisje_koepel"] = _model(_tex("bel", "glim", "steel", "blad", "goud", "bloem", "bloem_licht", doorzichtig=("bel", "glim")) |
                                           {"particle": "guhs:block/slijmhuisje_bloem"}, _koepel())
    m["block/slijmhuisje_leeg"] = {"textures": {"particle": "guhs:block/slijmhuisje_zij"}}
    # the slime (the renderer: centred on x/z 0, standing on y 0, 8 px = size 1), as the entity: an inner cube with eyes
    # and a mouth (cutout) and the jelly around it (translucent)
    m["block/slijmhuisje_slijm_binnen"] = {"textures": {"particle": "guhs:block/slijmhuisje_slijm", "slijm": "guhs:block/slijmhuisje_slijm"},
                                           "elements": [_slijm_box((-3, 1, -3), (6, 6, 6), (0, 16)),
                                                        _slijm_box((-3.25, 4, -3.5), (2, 2, 2), (32, 0)),
                                                        _slijm_box((1.25, 4, -3.5), (2, 2, 2), (32, 4)),
                                                        _slijm_box((-0.5, 2, -3.5), (1, 1, 1), (32, 8))]}
    m["block/slijmhuisje_slijm_buiten"] = {"textures": {"particle": "guhs:block/slijmhuisje_slijm",
                                                        "slijm": {"sprite": "guhs:block/slijmhuisje_slijm", "force_translucent": True}},
                                           "elements": [_slijm_box((-4, 0, -4), (8, 8, 8), (0, 0))]}
    m["block/slijmhuisje_balletje"] = {"textures": {"particle": "guhs:block/slijmhuisje_balletje", "bal": "guhs:block/slijmhuisje_balletje"},
                                       "elements": [_el((0, 0, 0), (2, 2, 2), "#bal", uv=[0, 0, 2, 2])]}
    # the item: the whole house at half size (base, kern parts, dome), as you see it from the front
    def half(els, d):
        out = []
        for e in els:
            e = json.loads(json.dumps(e))
            for k in ("from", "to"):
                p = [e[k][i] + d[i] for i in range(3)]
                e[k] = [round(8 + (p[0] - 8) * 0.5, 4), round(p[1] * 0.5, 4), round(8 + (p[2] - 8) * 0.5, 4)]
            out.append(e)
        return out
    tx = _tex("zij", "boven", "room", "snoep", "kussen", "schaal", "bel", "glim", "steel", "blad", "goud", "bloem", "bloem_licht",
              doorzichtig=("bel", "glim"))
    tx["voor"] = "guhs:block/slijmhuisje_voor_werkt"
    m["item/slijmhuisje"] = _model(tx, half(_voet(), (0, 0, 0)) + half(_kern_delen(), (0, 0, -16)) + half(_koepel(), (0, 32, 0)))
    m["item/opgepakte_guh_slime"] = {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/opgepakte_guh_slime"}}
    return m


def blockstates():
    kern = {"variants": {f"facing={f},snoet={s}": {"model": f"guhs:block/slijmhuisje_{s}", **({"y": r} if r else {})}
                         for f, r in ROT.items() for s in STATEN}}
    deel = [{"apply": {"model": "guhs:block/slijmhuisje_leeg"}},
            {"when": {"dy": "0", "stuk": "midden"}, "apply": {"model": "guhs:block/slijmhuisje_voet"}},
            {"when": {"dy": "2", "stuk": "midden"}, "apply": {"model": "guhs:block/slijmhuisje_koepel"}}]
    return {"slijmhuisje": kern, "slijmhuisje_deel": {"multipart": deel}}


# =====================================================================================================================
# the mechanics' data
# =====================================================================================================================
RECEPT = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GGG", "PWP", "PDP"],
          "key": {"G": "minecraft:glass", "P": "minecraft:pink_wool", "W": "minecraft:white_wool", "D": "guhs:guh_wire"},
          "result": {"id": "guhs:slijmhuisje"}}
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _tag(r, pad, waarden, w):
    vol = os.path.join(r, "data", *pad.split("/")) + ".json"
    data = {"replace": False, "values": []}
    if os.path.exists(vol):
        with open(vol, encoding="utf-8") as f:
            data = json.load(f)
    for v in waarden:
        if v not in data["values"]:
            data["values"].append(v)
    w(vol, data)


def schrijf(a, r, w=_schrijf_json):
    """Everything but the lang files. a = assets/guhs, r = src/main/resources."""
    d = os.path.join(r, "data", "guhs")
    # the look
    for naam, img in texturen(a).items():
        pad = os.path.join(a, "textures", *(naam.split("/") if "/" in naam else ("block", f"slijmhuisje_{naam}"))) + ".png"
        os.makedirs(os.path.dirname(pad), exist_ok=True)
        img.save(pad)
    for pad, model in modellen().items():
        w(os.path.join(a, "models", *pad.split("/")) + ".json", model)
    for naam, staat in blockstates().items():
        w(os.path.join(a, "blockstates", naam + ".json"), staat)
    # the mechanics
    for item in ("slijmhuisje", "opgepakte_guh_slime"):
        w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{item}"}})
    w(os.path.join(d, "loot_table", "blocks", "slijmhuisje.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:slijmhuisje"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    w(os.path.join(d, "recipe", "slijmhuisje.json"), RECEPT)
    w(os.path.join(d, "advancement", "quest", "tech_machines_slijmhuisje.json"), {"criteria": IMPOSSIBLE})
    w(os.path.join(d, "advancement", "techniek", "tech_machines_slijmhuisje.json"), {
        "display": {"icon": {"id": "guhs:slijmhuisje"},
                    "title": {"translate": "advancements.guhs.techniek.tech_machines_slijmhuisje.title"},
                    "description": {"translate": "advancements.guhs.techniek.tech_machines_slijmhuisje.description"},
                    "frame": "task", "show_toast": True, "announce_to_chat": False, "hidden": False},
        "criteria": IMPOSSIBLE, "parent": "guhs:techniek/tech_machines_boom"})
    _tag(r, "guhs/tags/block/vadskracht", ["guhs:slijmhuisje", "guhs:slijmhuisje_deel"], w)
    _tag(r, "minecraft/tags/block/mineable/axe", ["guhs:slijmhuisje", "guhs:slijmhuisje_deel"], w)


def build(h):
    schrijf(h.A, h.R, h.w)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    r = os.path.join("src", "main", "resources")
    a = os.path.join(r, "assets", "guhs")
    schrijf(a, r)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf_json(p, data)
    _schrijf_json(EN_FILE, {k: v[1] for k, v in sorted(TEKSTEN.items())})
    print(f"slijmhuisje: {len(texturen(a))} textures, {len(modellen())} models, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
