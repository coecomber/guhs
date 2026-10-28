"""
1.8.0 (Guhdromenland), the beds the user picked from the bed renders (dromen-ontwerp/bedden/b01..b08, scripts in
dromen-ontwerp/_bron/bedden/bedden.py): four real player beds (Droombed, Guh-hoofdbed, Blub-bed, Wolkenhemelbed: two
blocks, sleep / spawn / skip the night) and four critter beds (Guh-en-Blub-mandje, Stapelbedje, Luciferdoosje,
Schelpbedje). Java: feature/bedden (BeddenFeature, GuhsBedBlock, StapelbedBlock, DierBedBlock, DierBedGoal).

  build(h)    textures (16x16 pixel art), block models (elements, after the renders' geometry), block states, item models,
              recipes, loot (beds: the head part; the stapelbedje: the lower half), the tags (#minecraft:beds,
              #guhs:droombedden, #guhs:guh_slaapplekken, #guhs:muisbedjes, #guhs:schillybedjes, mineable/axe), the texts
              (Dutch in nl_nl, English in tools/lang/en/c215_bedden.json), the blokuitleg entries and a self-check

Standalone (from the project root): PYTHONPATH=tools python tools/features/bedden.py
"""
import json
import math
import os
import random
import re
import sys

from PIL import Image, ImageDraw

SPELERBEDDEN = ["droombed", "guh_hoofdbed", "blub_bed", "wolkenhemelbed"]
DIERBEDDEN = ["guh_blub_mandje", "stapelbedje", "luciferdoosje", "schelpbedje"]
ALLE = SPELERBEDDEN + DIERBEDDEN

NAMEN = {  # id -> (Dutch, English, Dutch lore, English lore)
    "droombed": ("Droombed", "Dream Bed",
                 "Een sterrenquilt en een maan als kussen. Hier tel je schaapjes én sterren.",
                 "A starry quilt and a moon for a pillow. Count sheep and stars here."),
    "guh_hoofdbed": ("Guh-hoofdbed", "Guh Head Bed",
                     "Het hoofdbord is een grote guh. Slaap je met je hoofd tegen een guh, dan droom je vads.",
                     "The headboard is a big guh. Sleep with your head against a guh and you dream vads."),
    "blub_bed": ("Blub-bed", "Blub Bed",
                 "Een echt Blub-blok om in te slapen, met stompjes en oortjes. Piept bijna als je instapt.",
                 "A real Blub block to sleep in, with stubby legs and ears. It almost squeaks when you get in."),
    "wolkenhemelbed": ("Wolkenhemelbed", "Cloud Canopy Bed",
                       "Slapen onder een wolk, met een regenboogje erbij. Er valt nooit regen uit, beloofd.",
                       "Sleeping under a cloud, with a little rainbow. No rain ever falls out of it, promise."),
    "guh_blub_mandje": ("Guh-en-Blub-mandje", "Guh and Blub Basket",
                        "'s Nachts kruipen een guh en een Blub er samen in. Samen knuffelen is dubbel zo warm.",
                        "At night a guh and a Blub curl up in it together. Cuddling together is twice as warm."),
    "stapelbedje": ("Stapelbedje", "Bunk Bed",
                    "Voor twee guhs, boven en onder. Wie boven slaapt, mag morgen de eerste knabbel.",
                    "For two guhs, top and bottom. Whoever sleeps on top gets the first nibble tomorrow."),
    "luciferdoosje": ("Luciferdoosje", "Matchbox Bed",
                      "Een bedje voor pieppiepmuisjes, met een geruit dekentje. Piep... piep... (dat is snurken).",
                      "A little bed for squeaky mice, with a checkered blanket. Squeak... squeak... (that's snoring)."),
    "schelpbedje": ("Schelpbedje", "Shell Bed",
                    "Voor Schilly en Poepschilly. Overdag ruzie, 's nachts knuffelen in de schelp.",
                    "For Schilly and Poepschilly. Squabbling by day, cuddling in the shell at night."),
}

import bedden_familie as FAM  # noqa: E402  (tools/features is on the path when run standalone)

NAMEN.update(FAM.NAMEN)
FAMILIE = list(FAM.NAMEN)

TEKSTEN = {}
for _id, (_nl, _en, _lnl, _len) in NAMEN.items():
    TEKSTEN[f"block.guhs.{_id}"] = (_nl, _en)
    TEKSTEN[f"block.guhs.{_id}.lore"] = (_lnl, _len)

UITLEG = {
    "droombed": ("Een echt bed: slapen, je spawnpunt zetten en de nacht overslaan, net als een gewoon bed. Ligt er een "
                 "ingestopte guh of Snoes vlakbij, dan kun je ook samen gaan dromen.",
                 "A real bed: sleep, set your spawn point and skip the night, just like a normal bed. With a tucked-in "
                 "guh or Snoes close by you can go dreaming together too."),
    "guh_blub_mandje": ("Een slaapplekje voor een guh én een Blub. 's Nachts zoekt een guh het mandje vanzelf op; een Blub "
                        "in de buurt kruipt er dan knuffelend bij. Je kunt er ook een guh in instoppen om te gaan dromen.",
                        "A sleeping spot for a guh and a Blub. At night a guh finds the basket by itself; a Blub nearby "
                        "then cuddles up with it. You can also tuck a guh in here to go dreaming."),
    "stapelbedje": ("Twee slaapplekjes boven elkaar: 's nachts slaapt er onder en boven een guh. De bovenste klautert via het "
                    "laddertje naar boven.",
                    "Two sleeping spots on top of each other: at night a guh sleeps below and one on top. The top one "
                    "climbs up the little ladder."),
    "luciferdoosje": ("'s Nachts kruipt een pieppiepmuisje in de buurt erin om te slapen (zzz). Overdag is het weer gewoon "
                      "een doosje.",
                      "At night a squeaky mouse nearby crawls in to sleep (zzz). By day it's just a little box again."),
    "schelpbedje": ("'s Nachts komen Schilly en Poepschilly erin slapen (zzz). Leg het in de buurt van hun strandje.",
                    "At night Schilly and Poepschilly come to sleep in it (zzz). Put it near their beach."),
}
UITLEG["guh_hoofdbank"] = (
    "Een familiebed van meerdere blokken: je zet het in een keer neer (het ligt van je af, het hoofdeinde aan de verre kant) "
    "en wie een stukje breekt, breekt het hele bed. Elk vakje is voor een eigen diertje: guh, Blub, pieppiepmuisje of Schilly "
    "(in een schelpje of een nat hoekje). 's Nachts gaan tamme diertjes in de buurt vanzelf naar hun vakje en slapen ze samen. "
    "Het is geen bed voor jezelf.",
    "A family bed of several blocks: you place it in one go (it lies away from you, the head end on the far side) and breaking "
    "one piece breaks the whole bed. Every nook is for its own critter: guh, Blub, squeaky mouse or Schilly (in a shell or a "
    "wet corner). At night tame critters nearby go to their nook by themselves and sleep together. It is not a bed for you.")
UITLEG["knuffelnest"] = (
    "Het grote ronde nest voor acht guhs (4 bij 4 blokken). 's Nachts komen tot acht tamme guhs uit de buurt erin slapen, in "
    "een kringetje met hun kopjes naar het knabbelschaaltje. Je zet het in een keer neer; wie een stukje breekt, breekt het hele nest.",
    "The big round nest for eight guhs (4 by 4 blocks). At night up to eight tame guhs from around sleep in it, in a circle with "
    "their heads towards the nibble bowl. You place it in one go; breaking one piece breaks the whole nest.")
UITLEG_ZELFDE = {"guh_hoofdbed": "droombed", "blub_bed": "droombed", "wolkenhemelbed": "droombed"}
UITLEG_ZELFDE.update({k: "guh_hoofdbank" for k in FAMILIE if k not in ("guh_hoofdbank", "knuffelnest")})

ROZE, ROZE_D, ROZE_L = (240, 170, 206), (214, 120, 162), (255, 214, 232)
PAARS, PAARS_L, NACHT = (112, 82, 160), (140, 108, 190), (52, 48, 112)
HOUT, HOUT_D = (232, 206, 170), (204, 172, 130)
WIT = (250, 248, 255)
GEEL, GEEL_L = (255, 226, 120), (255, 244, 190)
BLAUW, BLAUW_RAND, BLAUW_D = (138, 201, 247), (170, 220, 250), (110, 170, 230)
LAV = (206, 186, 240)
RIET, RIET_D = (196, 150, 100), (170, 124, 78)


def vlak(kleur, stip=None, seed=0, kans=0.12, ruis=6):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = stip if stip and rng.random() < kans else kleur
            v = rng.randint(-ruis, ruis)
            img.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    return img


def sterren(img, kleur=GEEL, seed=1, n=6):
    rng = random.Random(seed)
    for _ in range(n):
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            img.putpixel((x + dx, y + dy), kleur + (255,))
    return img


def hartjes(img, kleur=(255, 120, 170), seed=2, n=3):
    rng = random.Random(seed)
    for _ in range(n):
        x, y = rng.randint(1, 12), rng.randint(1, 12)
        for dx, dy in ((0, 0), (2, 0), (0, 1), (1, 1), (2, 1), (1, 2), (-0, 1)):
            img.putpixel((x + dx, y + dy), kleur + (255,))
    return img


def guh_gezicht():
    """The Guh-hoofdbed's headboard: a big pink guh face (glossy eyes, blush, nose, :3 mouth)."""
    img = vlak(ROZE, ROZE_L, 7, 0.06, 3)
    d = ImageDraw.Draw(img)
    for ox in (3, 10):
        d.rectangle([ox, 6, ox + 2, 9], fill=(30, 24, 40, 255))
        img.putpixel((ox, 6), (255, 255, 255, 255))
        d.line([ox, 9, ox + 2, 9], fill=(110, 222, 234, 255))
        d.rectangle([ox - 1, 10, ox + 2, 10], fill=(255, 150, 190, 255)) if False else None
    d.rectangle([1, 11, 3, 11], fill=(255, 150, 190, 255))
    d.rectangle([12, 11, 14, 11], fill=(255, 150, 190, 255))
    img.putpixel((7, 10), (255, 120, 160, 255))
    img.putpixel((8, 10), (255, 120, 160, 255))
    for (x, y) in ((6, 12), (7, 13), (8, 12), (9, 13), (10, 12)):
        img.putpixel((x, y), (136, 86, 150, 255))
    return img


def textures():
    T = {}
    # droombed
    T["droom_frame"] = vlak(PAARS, PAARS_L, 10)
    T["droom_quilt"] = sterren(vlak(NACHT, (64, 60, 130), 11), GEEL, 3, 5)
    T["droom_matras"] = vlak(WIT, (236, 232, 246), 12, ruis=3)
    maan = vlak(GEEL_L, GEEL, 13, 0.3, 3)
    T["droom_maan"] = maan
    lucht = sterren(vlak((40, 36, 92), (52, 48, 112), 14), (255, 230, 140), 5, 5)
    ImageDraw.Draw(lucht).ellipse([9, 2, 13, 6], fill=(255, 240, 180, 255))
    ImageDraw.Draw(lucht).ellipse([10, 1, 14, 5], fill=(40, 36, 92, 255))
    T["droom_lucht"] = lucht
    ster = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ImageDraw.Draw(ster).polygon([(8, 1), (10, 6), (15, 6), (11, 9), (13, 15), (8, 11), (3, 15), (5, 9), (1, 6), (6, 6)],
                                 fill=GEEL + (255,))
    T["droom_ster"] = ster
    # guh-hoofdbed
    T["guhbed_hout"] = vlak(HOUT, HOUT_D, 20)
    T["guhbed_quilt"] = hartjes(vlak((255, 196, 222), ROZE_L, 21), seed=4, n=4)
    T["guhbed_kussen"] = vlak((255, 255, 255), (244, 240, 250), 22, ruis=3)
    T["guhbed_gezicht"] = guh_gezicht()
    T["guhbed_roze"] = vlak(ROZE, ROZE_L, 23, 0.06, 3)
    T["guhbed_oor"] = vlak((255, 150, 190), ROZE, 24, 0.2, 3)
    # blub-bed
    T["blub_lijf"] = vlak(BLAUW, BLAUW_RAND, 30, 0.08, 3)
    gez = vlak(BLAUW, BLAUW_RAND, 31, 0.08, 3)
    d = ImageDraw.Draw(gez)
    for (x0, y0, x1, y1) in ((2, 6, 4, 7), (11, 6, 13, 7), (7, 9, 8, 9)):
        d.rectangle([x0, y0, x1, y1], fill=(112, 67, 80, 255))
    T["blub_gezicht"] = gez
    T["blub_poot"] = vlak((122, 186, 236), BLAUW, 32, 0.1, 3)
    oor = vlak(BLAUW_RAND, None, 33, ruis=2)
    ImageDraw.Draw(oor).rectangle([5, 2, 10, 15], fill=(118, 178, 232, 255))
    ImageDraw.Draw(oor).rectangle([7, 2, 8, 12], fill=BLAUW_RAND + (255,))
    T["blub_oor"] = oor
    deken = vlak(BLAUW_D, None, 34, ruis=3)
    for (x, y) in ((3, 3), (11, 5), (6, 11), (13, 12)):
        ImageDraw.Draw(deken).rectangle([x, y, x + 1, y + 1], fill=(255, 255, 255, 255))
    T["blub_deken"] = deken
    T["blub_matras"] = vlak(WIT, (236, 242, 250), 35, ruis=3)
    # wolkenhemelbed
    T["wolk_frame"] = vlak(WIT, (238, 236, 246), 40, ruis=3)
    T["wolk_quilt"] = vlak(LAV, (220, 204, 246), 41)
    T["wolk_kussen"] = vlak((255, 230, 240), None, 42, ruis=3)
    T["wolk_wolk"] = vlak((255, 255, 255), (238, 240, 252), 43, 0.3, 2)
    boog = Image.new("RGBA", (16, 16))
    for i, k in enumerate([(255, 140, 160), (255, 196, 130), (255, 240, 140), (160, 230, 160), (150, 200, 255), (200, 160, 240)]):
        for y in range(i * 16 // 6, (i + 1) * 16 // 6 + 1):
            for x in range(16):
                if y < 16:
                    boog.putpixel((x, y), k + (255,))
    T["wolk_regenboog"] = boog
    T["wolk_gordijn_roze"] = vlak((255, 200, 226), None, 44, ruis=3)
    T["wolk_gordijn_blauw"] = vlak((190, 220, 255), None, 45, ruis=3)
    # mandje
    riet = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            riet.putpixel((x, y), (RIET if ((x // 2) + (y // 2)) % 2 == 0 else RIET_D) + (255,))
    T["mandje_riet"] = riet
    T["mandje_kussen"] = vlak((255, 196, 222), ROZE_L, 50)
    T["mandje_deken"] = hartjes(vlak((186, 220, 255), (206, 230, 255), 51), (255, 140, 180), 5, 3)
    # stapelbedje
    T["stapel_hout"] = vlak(HOUT, HOUT_D, 60)
    T["stapel_matras"] = vlak((255, 250, 245), None, 61, ruis=3)
    T["stapel_deken_groen"] = vlak((190, 240, 210), (214, 248, 226), 62)
    T["stapel_deken_roze"] = hartjes(vlak((255, 200, 222), ROZE_L, 63), seed=6, n=2)
    vlag = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, k in enumerate([(255, 150, 180), (255, 220, 130), (150, 210, 255), (180, 230, 170)]):
        ImageDraw.Draw(vlag).polygon([(i * 4, 0), (i * 4 + 3, 0), (i * 4 + 1.5, 6)], fill=k + (255,))
    T["stapel_vlaggetjes"] = vlag
    ladder = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(ladder)
    d.rectangle([1, 0, 2, 15], fill=HOUT_D + (255,))
    d.rectangle([13, 0, 14, 15], fill=HOUT_D + (255,))
    for y in (2, 7, 12):
        d.rectangle([1, y, 14, y + 1], fill=HOUT_D + (255,))
    T["stapel_ladder"] = ladder
    # luciferdoosje
    T["lucifer_hoesje"] = vlak((214, 60, 60), (230, 90, 90), 70, ruis=4)
    et = vlak((214, 60, 60), None, 71, ruis=3)
    ImageDraw.Draw(et).rectangle([2, 4, 13, 11], fill=(255, 220, 90, 255))
    ImageDraw.Draw(et).ellipse([6, 5, 9, 10], fill=(214, 60, 60, 255))
    T["lucifer_etiket"] = et
    T["lucifer_laatje"] = vlak((236, 214, 176), (224, 200, 160), 72)
    T["lucifer_bedje"] = vlak((250, 240, 220), None, 73, ruis=3)
    ruit = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            ruit.putpixel((x, y), ((240, 120, 150) if ((x // 4) + (y // 4)) % 2 == 0 else (255, 230, 236)) + (255,))
    T["lucifer_ruit"] = ruit
    T["lucifer_stokje"] = vlak((232, 200, 150), None, 74, ruis=3)
    T["lucifer_kopje"] = vlak((200, 40, 50), None, 75, ruis=4)
    # schelpbedje
    schelp = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            schelp.putpixel((x, y), ((255, 186, 196) if (x // 2) % 2 == 0 else (255, 214, 220)) + (255,))
    T["schelp_ribbel"] = schelp
    T["schelp_parel"] = vlak((250, 248, 255), (236, 236, 250), 80, 0.3, 2)
    T["schelp_zeewier"] = vlak((130, 210, 200), (110, 190, 180), 81)
    return T


def E(frm, to, faces):
    if isinstance(faces, str):
        faces = {s: faces for s in ("down", "up", "north", "south", "west", "east")}
    return {"from": frm, "to": to, "faces": {s: {"texture": f"#{t}"} for s, t in faces.items()}}


def model(h, naam, elements, textures):
    tx = {k: f"guhs:block/bedden/{v}" for k, v in textures.items()}
    tx.setdefault("particle", next(iter(tx.values())))
    h.w(f"{h.A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": tx, "elements": elements})


Y_ROT = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def bedstaat(naam):
    """A two-block bed: facing x part (occupied ignored). Models are drawn facing north (head = the north block)."""
    v = {}
    for f, y in Y_ROT:
        for part in ("head", "foot"):
            for occ in ("true", "false"):
                v[f"facing={f},occupied={occ},part={part}"] = {"model": f"guhs:block/{naam}_{part}", **({"y": y} if y else {})}
    return {"variants": v}


def bed_elementen(head, frame, matras, quilt, kussen):
    """The shared frame of the player beds (legs, frame, mattress, quilt; the pillow on the head)."""
    els = []
    if head:
        els += [E([0, 0, 0], [3, 3, 3], frame), E([13, 0, 0], [16, 3, 3], frame)]
        els.append(E([0, 3, 0], [16, 6, 16], frame))
        els.append(E([1, 6, 1], [15, 9, 16], matras))
        els.append(E([0, 8, 9], [16, 10, 16], quilt))
        els.append(E([3, 9, 2], [13, 12, 7], kussen))
    else:
        els += [E([0, 0, 13], [3, 3, 16], frame), E([13, 0, 13], [16, 3, 16], frame)]
        els.append(E([0, 3, 0], [16, 6, 16], frame))
        els.append(E([1, 6, 0], [15, 9, 15], matras))
        els.append(E([0, 8, 0], [16, 10, 16], quilt))
        els.append(E([0, 3, 16], [16, 10, 16.5], quilt))      # (the quilt hangs over the foot end)
    return els


def build(h):
    for naam, img in textures().items():
        h.save(img, "block", "bedden", f"{naam}.png")
    A = h.A
    # 1. Droombed: purple, a starry quilt, a moon pillow, a night-sky headboard with a moon, a little canopy with stars
    tx = {"frame": "droom_frame", "matras": "droom_matras", "quilt": "droom_quilt", "kussen": "droom_maan",
          "lucht": "droom_lucht", "ster": "droom_ster"}
    head = bed_elementen(True, "frame", "matras", "quilt", "kussen") + [
        E([-1, 0, -0.5], [17, 24, 1.5], {"north": "frame", "south": "lucht", "up": "frame", "down": "frame", "west": "frame", "east": "frame"}),
        E([-1.5, 0, -0.5], [-0.5, 30, 1], "frame"), E([16.5, 0, -0.5], [17.5, 30, 1], "frame"),
        E([-1.5, 29, -0.5], [17.5, 30.5, 8], "frame"),
        E([3, 24, 7], [5, 26, 7.01], {"south": "ster", "north": "ster"}), E([7, 25, 7], [9, 27, 7.01], {"south": "ster", "north": "ster"}),
        E([11, 24, 7], [13, 26, 7.01], {"south": "ster", "north": "ster"}),
    ]
    model(h, "droombed_head", head, tx)
    model(h, "droombed_foot", bed_elementen(False, "frame", "matras", "quilt", "kussen"), tx)
    # 2. Guh-hoofdbed: wood, a pink heart quilt, a white pillow; the headboard is a big guh face with round ears
    tx = {"frame": "guhbed_hout", "matras": "guhbed_kussen", "quilt": "guhbed_quilt", "kussen": "guhbed_kussen",
          "gezicht": "guhbed_gezicht", "roze": "guhbed_roze", "oor": "guhbed_oor"}
    head = bed_elementen(True, "frame", "matras", "quilt", "kussen") + [
        E([-2, 4, -0.5], [18, 28, 1.5], {"north": "roze", "south": "gezicht", "up": "roze", "down": "roze", "west": "roze", "east": "roze"}),
        E([-1, 26, -0.25], [5, 32, 1.25], {"north": "roze", "south": "oor", "up": "roze", "down": "roze", "west": "roze", "east": "roze"}),
        E([11, 26, -0.25], [17, 32, 1.25], {"north": "roze", "south": "oor", "up": "roze", "down": "roze", "west": "roze", "east": "roze"}),
    ]
    model(h, "guh_hoofdbed_head", head, tx)
    model(h, "guh_hoofdbed_foot", bed_elementen(False, "frame", "matras", "quilt", "kussen"), tx)
    # 3. Blub-bed: the whole bed is a Blub block (stumps for legs, flat Blub ears as the headboard, the face at the foot end)
    tx = {"lijf": "blub_lijf", "gezicht": "blub_gezicht", "poot": "blub_poot", "oor": "blub_oor", "deken": "blub_deken",
          "matras": "blub_matras"}
    head = [E([1, 0, 1], [5, 3, 5], "poot"), E([11, 0, 1], [15, 3, 5], "poot"),
            E([-1, 3, 0], [17, 10, 16], "lijf"), E([1, 10, 1], [15, 11, 16], "matras"),
            E([3, 11, 2], [13, 14, 7], "matras"), E([0, 10, 9], [16, 12, 16], "deken"),
            E([2, 10, -0.5], [6, 28, 1], {"north": "oor", "south": "oor", "up": "lijf", "west": "lijf", "east": "lijf"}),
            E([10, 10, -0.5], [14, 28, 1], {"north": "oor", "south": "oor", "up": "lijf", "west": "lijf", "east": "lijf"})]
    foot = [E([1, 0, 11], [5, 3, 15], "poot"), E([11, 0, 11], [15, 3, 15], "poot"),
            E([-1, 3, 0], [17, 10, 17], {"down": "lijf", "up": "lijf", "north": "lijf", "south": "gezicht", "west": "lijf", "east": "lijf"}),
            E([1, 10, 0], [15, 11, 15], "matras"), E([0, 10, 0], [16, 12, 16], "deken")]
    model(h, "blub_bed_head", head, tx)
    model(h, "blub_bed_foot", foot, tx)
    # 4. Wolkenhemelbed: white, a lilac quilt, four posts with a cloud canopy, a rainbow ribbon and little curtains
    tx = {"frame": "wolk_frame", "matras": "wolk_frame", "quilt": "wolk_quilt", "kussen": "wolk_kussen", "wolk": "wolk_wolk",
          "boog": "wolk_regenboog", "gr": "wolk_gordijn_roze", "gb": "wolk_gordijn_blauw"}
    head = bed_elementen(True, "frame", "matras", "quilt", "kussen") + [
        E([-1, 0, -1], [1, 28, 1], "frame"), E([15, 0, -1], [17, 28, 1], "frame"),
        E([-2, 28, -2], [18, 32, 16], "wolk"),
        E([-1.5, 12, 1], [-1, 28, 7], {"west": "gr", "east": "gr"}), E([17, 12, 1], [17.5, 28, 7], {"west": "gb", "east": "gb"}),
    ]
    foot = bed_elementen(False, "frame", "matras", "quilt", "kussen") + [
        E([-1, 0, 15], [1, 28, 17], "frame"), E([15, 0, 15], [17, 28, 17], "frame"),
        E([-2, 28, 0], [18, 32, 18], "wolk"),
        E([-2, 25, 17.5], [18, 28, 18], {"south": "boog", "north": "boog"}),
        E([-1.5, 16, 10], [-1, 28, 15], {"west": "gr", "east": "gr"}), E([17, 16, 10], [17.5, 28, 15], {"west": "gb", "east": "gb"}),
    ]
    model(h, "wolkenhemelbed_head", head, tx)
    model(h, "wolkenhemelbed_foot", foot, tx)
    for n in SPELERBEDDEN:
        h.w(f"{A}/blockstates/{n}.json", bedstaat(n))
        h.w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/bedden/{n}"}})   # (icon: render of the whole bed, see icoon())
        h.w(f"{h.D}/loot_table/blocks/{n}.json", deel_loot(n, "part", "head"))

    # 5. Guh-en-Blub-mandje: a wicker basket with a pink cushion and a heart blanket at the foot
    model(h, "guh_blub_mandje", [
        E([1, 0, 1], [15, 1, 15], "riet"), E([1, 1, 1], [15, 2.5, 15], "kussen"),
        E([0, 0, 0], [16, 6, 1.5], "riet"), E([0, 0, 14.5], [16, 6, 16], "riet"),
        E([0, 0, 1.5], [1.5, 6, 14.5], "riet"), E([14.5, 0, 1.5], [16, 6, 14.5], "riet"),
        E([1.5, 4, 9], [14.5, 5, 14.5], "deken"),
    ], {"riet": "mandje_riet", "kussen": "mandje_kussen", "deken": "mandje_deken"})
    h.w(f"{A}/blockstates/guh_blub_mandje.json", facing_staat("guh_blub_mandje"))
    # 6. Stapelbedje: two halves (lower / upper), posts, two mattresses, a ladder at the side, little flags on top
    pal = {"hout": "stapel_hout", "matras": "stapel_matras", "groen": "stapel_deken_groen", "roze": "stapel_deken_roze",
           "vlag": "stapel_vlaggetjes", "ladder": "stapel_ladder"}
    palen = [E([0, 0, 0], [2, 16, 2], "hout"), E([14, 0, 0], [16, 16, 2], "hout"), E([0, 0, 14], [2, 16, 16], "hout"),
             E([14, 0, 14], [16, 16, 16], "hout")]
    model(h, "stapelbedje_lower", palen + [
        E([0, 0.5, 0], [16, 1.5, 16], "hout"), E([1, 1.5, 1], [15, 3, 15], "matras"), E([1, 3, 6], [15, 4, 15], "groen"),
        E([16, 0, 3], [16.5, 16, 13], {"east": "ladder", "west": "ladder"}),
    ], pal)
    model(h, "stapelbedje_upper", [E([0, 0, 0], [2, 9, 2], "hout"), E([14, 0, 0], [16, 9, 2], "hout"),
                                   E([0, 0, 14], [2, 9, 16], "hout"), E([14, 0, 14], [16, 9, 16], "hout")] + [
        E([0, 0, 0], [16, 1.5, 16], "hout"), E([1, 1.5, 1], [15, 3, 15], "matras"), E([1, 3, 6], [15, 4, 15], "roze"),
        E([2, 5, 0], [14, 7, 1], "hout"), E([2, 5, 15], [14, 7, 16], "hout"),
        E([16, 0, 3], [16.5, 5, 13], {"east": "ladder", "west": "ladder"}),
        E([1, 8, 15.6], [15, 11, 15.7], {"north": "vlag", "south": "vlag"}),
    ], pal)
    v = {}
    for f, y in Y_ROT:
        for half in ("lower", "upper"):
            v[f"facing={f},half={half}"] = {"model": f"guhs:block/stapelbedje_{half}", **({"y": y} if y else {})}
    h.w(f"{A}/blockstates/stapelbedje.json", {"variants": v})
    # 7. Luciferdoosje: the drawer with a checkered blanket, the red sleeve behind it, a match as the headboard
    model(h, "luciferdoosje", [
        E([3, 0, 1], [13, 4, 4], {"north": "etiket", "south": "hoesje", "up": "hoesje", "down": "hoesje", "west": "hoesje", "east": "hoesje"}),
        E([3, 0, 4], [13, 3, 15], "laatje"), E([3.5, 1, 4.5], [12.5, 3.01, 14.5], {"up": "bedje"}),
        E([3.5, 3, 9], [12.5, 3.5, 14.5], "ruit"),
        E([3, 3, 4.5], [12, 3.8, 5.3], "stokje"), E([12, 2.8, 4.2], [13.5, 4.2, 5.6], "kopje"),
    ], {"hoesje": "lucifer_hoesje", "etiket": "lucifer_etiket", "laatje": "lucifer_laatje", "bedje": "lucifer_bedje",
        "ruit": "lucifer_ruit", "stokje": "lucifer_stokje", "kopje": "lucifer_kopje"})
    h.w(f"{A}/blockstates/luciferdoosje.json", facing_staat("luciferdoosje"))
    # 8. Schelpbedje: the bottom shell lying down, the top shell upright as the headboard, a pearl pillow, a seaweed blanket
    model(h, "schelpbedje", [
        E([2, 0, 3], [14, 2, 15], "ribbel"), E([1, 0, 5], [2, 1.5, 13], "ribbel"), E([14, 0, 5], [15, 1.5, 13], "ribbel"),
        E([2, 0, 1.5], [14, 13, 3], "ribbel"), E([4, 13, 1.5], [12, 15, 3], "ribbel"),
        E([6, 2, 4], [10, 4.5, 7], "parel"), E([3, 2, 9], [13, 3, 14], "zeewier"),
    ], {"ribbel": "schelp_ribbel", "parel": "schelp_parel", "zeewier": "schelp_zeewier"})
    h.w(f"{A}/blockstates/schelpbedje.json", facing_staat("schelpbedje"))
    for n in ("guh_blub_mandje", "luciferdoosje", "schelpbedje"):
        h.w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/bedden/{n}"}})
        h.self_drop(n)
    h.w(f"{A}/models/item/stapelbedje.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/bedden/stapelbedje"}})
    h.w(f"{h.D}/loot_table/blocks/stapelbedje.json", deel_loot("stapelbedje", "half", "lower"))

    # recipes: sweet ingredients that fit each bed
    planken = "#minecraft:planks"
    h.shaped("droombed", ["WDW", "PPP"], {"W": "minecraft:purple_wool", "D": "guhs:droomzand", "P": planken}, "guhs:droombed")
    h.shaped("guh_hoofdbed", ["KKK", "WWW", "PPP"], {"K": "guhs:knuffelblok_roze", "W": "minecraft:pink_wool", "P": planken},
             "guhs:guh_hoofdbed")
    h.shaped("blub_bed", ["WSW", "BBB"], {"W": "minecraft:white_wool", "S": "minecraft:slime_ball", "B": "minecraft:light_blue_wool"},
             "guhs:blub_bed")
    h.shaped("wolkenhemelbed", ["CCC", "T T", "WWW"], {"C": "guhs:wolkenblok_wit", "T": "minecraft:stick", "W": "minecraft:white_wool"},
             "guhs:wolkenhemelbed")
    h.shaped("guh_blub_mandje", ["S S", "SKS", "SSS"], {"S": "minecraft:sugar_cane", "K": "guhs:pink_kussen"}, "guhs:guh_blub_mandje")
    h.shaped("stapelbedje", ["T T", "WTW", "WTW"], {"T": "minecraft:stick", "W": "minecraft:pink_wool"}, "guhs:stapelbedje")
    h.shaped("luciferdoosje", ["P", "K", "R"], {"P": "minecraft:paper", "K": "guhs:kaas_knabbels", "R": "minecraft:red_dye"},
             "guhs:luciferdoosje")
    h.shaped("schelpbedje", ["N", "S"], {"N": "minecraft:nautilus_shell", "S": "minecraft:seagrass"}, "guhs:schelpbedje")
    # tags
    h.add_tag("minecraft/tags/block/beds", [f"guhs:{n}" for n in SPELERBEDDEN])
    h.add_tag("minecraft/tags/item/beds", [f"guhs:{n}" for n in SPELERBEDDEN])
    h.add_tag("guhs/tags/block/droombedden", ["#minecraft:beds"])
    h.add_tag("guhs/tags/block/guh_slaapplekken", ["guhs:guh_blub_mandje", "guhs:stapelbedje"])
    h.add_tag("guhs/tags/block/muisbedjes", ["guhs:luciferdoosje"])
    h.add_tag("guhs/tags/block/schillybedjes", ["guhs:schelpbedje"])
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{n}" for n in ALLE])
    fam_build(h)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


# --- the family beds: a model per cell, cut from the whole bed's boxes ------------------------------------------------------
def fam_textures(beden):
    """One texture per colour (a soft noise) plus the patterned ones (ruit, gezicht, water, schelp, ster, hartje, schuim)."""
    T = {}
    for b in beden:
        for box in b.boxes:
            t = box[6]
            if t in T:
                continue
            if t.startswith("k"):
                k = tuple(int(t[1 + i:3 + i], 16) for i in (0, 2, 4))
                T[t] = vlak(k, None, int(t[1:], 16) & 0xffff, ruis=4)
            elif t.startswith("ruit_"):
                _, a, c = t.split("_")
                k1, k2 = (tuple(int(hx[i:i + 2], 16) for i in (0, 2, 4)) for hx in (a, c))
                img = Image.new("RGBA", (16, 16))
                for y in range(16):
                    for x in range(16):
                        img.putpixel((x, y), (k1 if ((x // 4) + (y // 4)) % 2 == 0 else k2) + (255,))
                T[t] = img
    T["kffb6cc"] = vlak((255, 182, 204), None, 89, ruis=3)
    T["gezicht"] = guh_gezicht()
    T["water"] = vlak((120, 200, 230), (160, 224, 245), 90, 0.15, 4)
    T["schelp"] = textures()["schelp_ribbel"]
    T["ster"] = vlak(GEEL, GEEL_L, 91, 0.3, 3)
    T["hartje"] = vlak((255, 140, 180), (255, 180, 206), 92, 0.2, 3)
    T["schuim"] = vlak((255, 255, 255), (236, 244, 255), 93, 0.3, 2)
    return T


def fam_vorm(b):
    """The bed's frame: origin, size in cells, and every box and vakje in pixels / blocks of that frame."""
    xs = [v for box in b.boxes for v in (box[0], box[3])] + [v[0] for v in b.vakjes]
    ys = [v for box in b.boxes for v in (box[1], box[4])]
    zs = [v for box in b.boxes for v in (box[2], box[5])] + [v[2] for v in b.vakjes]
    ox, oy, oz = min(xs), min(ys), min(zs)
    # the render's bed, scaled (evenly, keeping its shape) onto the footprint the user picked (5x2, 8x2, ...)
    dx, dy, dz = FAM.MAAT[b.id]
    s = min(dx / (max(xs) - ox), dz / (max(zs) - oz))
    sv = min(s, dy / (max(ys) - oy)) if dy else s
    sx, sy, sz = (math.ceil((max(xs) - ox) * s - 0.01), math.ceil((max(ys) - oy) * sv - 0.01), math.ceil((max(zs) - oz) * s - 0.01))
    px = [((x0 - ox) * s * 16, (y0 - oy) * sv * 16, (z0 - oz) * s * 16, (x1 - ox) * s * 16, (y1 - oy) * sv * 16, (z1 - oz) * s * 16,
           t, so, f) for (x0, y0, z0, x1, y1, z1, t, so, f) in b.boxes]
    vak = [((x - ox) * s, (y - oy) * sv, (z - oz) * s, soort) for (x, y, z, soort) in b.vakjes]
    return sx, sy, sz, px, vak


ZIJDEN = ("down", "up", "north", "south", "west", "east")


def fam_cellen(sx, sy, sz, px):
    """Per cell (deel = x + sx * (z + sz * y)): its elements (clipped to the cell, in its own pixels) and its solid boxes."""
    cellen = {}
    for ly in range(sy):
        for lz in range(sz):
            for lx in range(sx):
                c0 = (lx * 16, ly * 16, lz * 16)
                els, solide = [], []
                for (x0, y0, z0, x1, y1, z1, t, so, f) in px:
                    a = [max(x0, c0[0]), max(y0, c0[1]), max(z0, c0[2])]
                    e = [min(x1, c0[0] + 16), min(y1, c0[1] + 16), min(z1, c0[2] + 16)]
                    if any(e[i] - a[i] < 0.05 for i in range(3)):
                        continue
                    frm = [round(a[i] - c0[i], 3) for i in range(3)]
                    to = [round(e[i] - c0[i], 3) for i in range(3)]
                    if f == "south":
                        bw, bh = x1 - x0, y1 - y0
                        uv = [round((a[0] - x0) / bw * 16, 3), round((y1 - e[1]) / bh * 16, 3),
                              round((e[0] - x0) / bw * 16, 3), round((y1 - a[1]) / bh * 16, 3)]
                        faces = {z_: {"texture": "#kffb6cc"} for z_ in ZIJDEN if z_ != "south"}
                        faces["south"] = {"texture": f"#{t}", "uv": uv}
                        els.append({"from": frm, "to": to, "faces": faces, "_tex": [t, "kffb6cc"]})
                    else:
                        els.append({"from": frm, "to": to, "faces": {z_: {"texture": f"#{t}"} for z_ in ZIJDEN}, "_tex": [t]})
                    if so:
                        solide.append(frm + to)
                if els:
                    solide.sort(key=lambda bx: -(bx[3] - bx[0]) * (bx[4] - bx[1]) * (bx[5] - bx[2]))
                    cellen[lx + sx * (lz + sz * ly)] = (els, solide[:8])
    return cellen


def fam_build(h):
    beden = [f() for f in FAM.ALLE]
    for naam, img in fam_textures(beden).items():
        h.save(img, "block", "bedden", "fam", f"{naam}.png")
    h.w(f"{h.A}/models/block/familie/leeg.json", {"parent": "minecraft:block/block",
                                                  "textures": {"particle": "guhs:block/bedden/fam/gezicht"}, "elements": []})
    java = []
    for b in beden:
        sx, sy, sz, px, vak = fam_vorm(b)
        if sx * sy * sz > 128:
            raise SystemExit(f"{b.id}: {sx}x{sy}x{sz} cells, more than DEEL allows")
        cellen = fam_cellen(sx, sy, sz, px)
        anker = sx * (sz - 1)
        if anker not in cellen:
            cellen[anker] = ([], [])
        for deel, (els, _) in cellen.items():
            tex = {}
            for e in els:
                for t in e.pop("_tex"):
                    tex[t] = f"guhs:block/bedden/fam/{t}"
            if not els:
                continue
            tex["particle"] = next(iter(tex.values()))
            h.w(f"{h.A}/models/block/familie/{b.id}_{deel}.json", {"parent": "minecraft:block/block", "textures": tex, "elements": els})
        v = {}
        for f, y in Y_ROT:
            for deel in range(128):
                heeft = deel in cellen and cellen[deel][0]
                m = f"guhs:block/familie/{b.id}_{deel}" if heeft else "guhs:block/familie/leeg"
                v[f"deel={deel},facing={f}"] = {"model": m, **({"y": y} if y and heeft else {})}
        h.w(f"{h.A}/blockstates/{b.id}.json", {"variants": v})
        h.item_model(b.id, tex=f"guhs:item/bedden/{b.id}")
        h.self_drop(b.id)
        java.append((b.id, sx, sy, sz, cellen, vak))
        icoon(b.id)
        print(f"  {b.id}: {sx}x{sy}x{sz} cells, {len(cellen)} blocks, {len(vak)} vakjes")
    fam_java(java)
    # recipes: wool and cushions of the bed's colours, plus what makes it this bed
    W, P = "minecraft:pink_wool", "#minecraft:planks"
    h.shaped("guh_hoofdbank", ["KKK", "WWW", "PPP"], {"K": "guhs:knuffelblok_roze", "W": W, "P": P}, "guhs:guh_hoofdbank")
    h.shaped("slaaptreintje", ["WWW", "RMR", "III"], {"W": W, "R": "minecraft:red_dye", "M": "minecraft:minecart",
                                                      "I": "minecraft:iron_nugget"}, "guhs:slaaptreintje")
    h.shaped("kussenfort", ["F F", "KKK", "KKK"], {"F": "minecraft:stick", "K": "guhs:pink_kussen"}, "guhs:kussenfort")
    h.shaped("droomschuitje", ["S S", "WDW", " B "], {"S": "minecraft:string", "W": "minecraft:purple_wool", "D": "guhs:droomzand",
                                                      "B": "minecraft:oak_boat"}, "guhs:droomschuitje")
    h.shaped("boomhut_stapelbed", ["LLL", "PSP", "PSP"], {"L": "#minecraft:leaves", "P": P, "S": "minecraft:ladder"},
             "guhs:boomhut_stapelbed")
    h.shaped("quiltbed", ["ABC", "WWW", "PPP"], {"A": "minecraft:pink_wool", "B": "minecraft:light_blue_wool",
                                                 "C": "minecraft:yellow_wool", "W": "minecraft:white_wool", "P": P}, "guhs:quiltbed")
    h.shaped("badkuipbed", ["Q Q", "QWQ", "G G"], {"Q": "minecraft:quartz_block", "W": "minecraft:water_bucket",
                                                   "G": "minecraft:gold_nugget"}, "guhs:badkuipbed")
    h.shaped("knuffelnest", ["KWK", "W W", "KWK"], {"K": "guhs:pink_kussen", "W": W}, "guhs:knuffelnest")
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{n}" for n in FAMILIE])


def icoon(naam):
    """(1.8.0 fix) The item icon of a bed: textures/item/bedden/<naam>.png, a 64x64 render of the WHOLE bed from its own block
    models (isometric, transparent; made with the renderkit, see the session tool bed_iconen.py). Committed; never cropped from the
    concept renders any more (those gave odd icons with room walls in them)."""
    pad = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "item", "bedden", f"{naam}.png")
    if not os.path.exists(pad):
        print(f"  bedden: no icon {pad} (render it with the renderkit)")


JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bedden", "FamilieVormen.java")


def fam_java(beden):
    r = ["package nl.juiced.guhs.feature.bedden;", "", "import java.util.HashMap;", "import java.util.LinkedHashMap;",
         "import java.util.List;", "import java.util.Map;", "",
         "/**", " * The family beds' cells, solid boxes (pixels, per cell, drawn facing north) and vakjes (blocks, in the bed's frame).",
         " * GENERATED by tools/features/bedden.py (from the family bed renders): do not edit by hand.", " */",
         "public final class FamilieVormen {",
         "    /** A family bed: its size in cells, the cells that hold a block, their solid boxes and the vakjes. */",
         "    public record Vorm(int sx, int sy, int sz, int[] cellen, Map<Integer, double[][]> blok, List<Vakjes.Vakje> vakjes) {",
         "        public double[][] blokjes(int deel) {", "            return blok.getOrDefault(deel, new double[0][]);", "        }", "    }",
         "", "    public static final Map<String, Vorm> VORMEN = new LinkedHashMap<>();", "", "    private FamilieVormen() {", "    }", "",
         "    static {"]
    for (bid, sx, sy, sz, cellen, vak) in beden:
        r.append(f"        {{   // {bid}")
        r.append("            Map<Integer, double[][]> b = new HashMap<>();")
        for deel, (_, solide) in sorted(cellen.items()):
            if solide:
                r.append(f"            b.put({deel}, new double[][] {{" + ", ".join(
                    "{" + ", ".join(f"{v:g}" for v in bx) + "}" for bx in solide) + "});")
        cel = ", ".join(str(d) for d in sorted(cellen))
        vk = ", ".join(f"new Vakjes.Vakje({x:.3f}, {y:.3f}, {z:.3f}, Vakjes.Soort.{so})" for (x, y, z, so) in vak)
        r.append(f"            VORMEN.put(\"{bid}\", new Vorm({sx}, {sy}, {sz}, new int[] {{{cel}}}, b, List.of({vk})));")
        r.append("        }")
    r += ["    }", "}", ""]
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(r))


def facing_staat(naam):
    return {"variants": {f"facing={f}": {"model": f"guhs:block/{naam}", **({"y": y} if y else {})} for f, y in Y_ROT}}


def deel_loot(naam, prop, waarde):
    """Only one half drops the item (like a vanilla bed's head, or a door's lower half)."""
    return {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{naam}", "conditions": [
            {"condition": "minecraft:block_state_property", "block": f"guhs:{naam}", "properties": {prop: waarde}}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


def selfcheck(h):
    problems = []
    for n in ALLE + FAMILIE:
        for p in (f"{h.A}/blockstates/{n}.json", f"{h.A}/models/item/{n}.json", f"{h.D}/recipe/{n}.json", f"{h.D}/loot_table/blocks/{n}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    if problems:
        raise SystemExit("bedden self-check failed:\n  " + "\n  ".join(problems))


BLOKUITLEG = os.path.join("tools", "wiki_site", "blokuitleg.py")
_MARK = "# <bedden> (1.8.0, tools/features/bedden.py: rewritten on every run)"


def blokuitleg():
    """The blokuitleg entries, as one marked block at the end of tools/wiki_site/blokuitleg.py (replaced on a re-run)."""
    with open(BLOKUITLEG, encoding="utf-8") as f:
        src = f.read()
    src = re.sub(r"\n" + re.escape(_MARK) + r".*?# </bedden>\n", "\n", src, flags=re.S)
    regels = [_MARK]
    for k, (nl, en) in UITLEG.items():
        regels.append(f"UITLEG[{k!r}] = dict(nl={nl!r}, en={en!r})")
    for k, bron in UITLEG_ZELFDE.items():
        regels.append(f"UITLEG[{k!r}] = UITLEG[{bron!r}]")
    regels.append("# </bedden>")
    if not src.endswith("\n"):
        src += "\n"
    with open(BLOKUITLEG, "w", encoding="utf-8", newline="\n") as f:
        f.write(src + "\n".join(regels) + "\n")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    blokuitleg()
    en_pad = os.path.join("tools", "lang", "en", "c215_bedden.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    _mc26()
    print(f"bedden: {len(SPELERBEDDEN)} player beds, {len(DIERBEDDEN)} critter beds, {len(FAMILIE)} family beds, {len(TEKSTEN)} texts")


def _mc26():
    """Only the beds' items in their 26.1 shape (client item definitions)."""
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in ALLE + FAMILIE:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    _direct()
