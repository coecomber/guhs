"""
1.7.0 "Rondje Guhmension" (design: MineScapeStory/ontwerp-guhs170-mijnen.md, ronde 25 and 31; the holes and rewards:
mijnen-ontwerp/golf/voorstel_achterste9.md). The guh golf course grows to 18 holes. Java: feature/golf (Golf18, Golf18Blocks,
GolfGame, GolfBanen).

  build(h)    the new blocks (ijsvilt, the sliding guh block and its rail, the turning hub, the snapping jaw, the bank
              corner, the looping, ruitjesvilt in three colours with stairs and slabs, the golf rack, golf balls on tees,
              the hole with a flag, the golf cart, the golf trophy): textures, models, blockstates (also the tees and cups of
              holes 10-18), loot, tags, the 26.1 item definitions, advancements, texts; and the guh_golfbaan18 structure:
              the old 9-hole course (golf.golf_structure, unchanged, moved 32 south and 5 up) with the back 9 east of it
              (golf18_holes), paths of ruitjesvilt and a gate. The structure guh_golfbaan now starts from this template
              (centred on its jigsaw "guhs:guh_golfbaan_midden"); courses that already exist keep their own (old) template,
              because a structure start remembers its pieces. New chunks only.
  selfcheck   every hole 10-18 has its three tees, its cup, a free spot behind each tee, and its signs; every guhs: block
              exists; GolfBanen.java has the same pars and template numbers as this file.

On its own (like the slices since 1.5): python tools/features/golf18.py from the project root. It writes only its own files
(and patches the guh_golfbaan structure files and the tee/cup blockstates), and merges its texts into nl_nl.json / en_us.json.
"""
import json
import math
import os
import random
import re
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
if os.path.join(HERE, "..") not in sys.path:
    sys.path.insert(0, os.path.join(HERE, ".."))

from features import golf  # noqa: E402
from features import golf18_holes as holes  # noqa: E402

# ---------------------------------------------------------------------------------------------------------------------
# the 18-hole template
# ---------------------------------------------------------------------------------------------------------------------
X, Y, Z = 200, 34, 160
BY = 6                       # the ground (grass) of the whole template; the front 9's felt is on it too
FRONT = (0, BY - 1, 32)      # where the old 96x96 course goes (its y=0 foundation one under the ground)
BX, BZ, CW, CD = 102, 2, 32, 52
VAKKEN = [(0, 0), (0, 1), (0, 2), (1, 2), (1, 1), (1, 0), (2, 0), (2, 1), (2, 2)]   # a snake: down, up, down again
MIDDEN = (98, BY - 1, 80)    # the jigsaw the structure is centred on (in the gap between the front and back 9)
ACHTER = (100, 0, 0, X - 1, Y - 1, Z - 1)
PATH = "guhs:ruitjesvilt"
RUITJES = {"ruitjesvilt": ((240, 150, 190), "Checkered Felt", "Ruitjesvilt"),
           "ruitjesvilt_mint": ((150, 214, 186), "Mint Checkered Felt", "Mint ruitjesvilt"),
           "ruitjesvilt_geel": ((246, 210, 110), "Yellow Checkered Felt", "Geel ruitjesvilt")}


def _lijn(key, nl, en, texts, **style):
    texts[key] = (en, nl)
    out = {"translate": key, "fallback": nl}
    out.update(style)
    return out


def sign_nbt(lines):
    from make_structures import NbtList, Byte
    msgs = [json.dumps(l) if l else json.dumps("") for l in (list(lines) + [""] * 4)[:4]]
    empty = [json.dumps("")] * 4
    return {"id": "minecraft:sign",
            "front_text": {"messages": NbtList(8, msgs), "color": "black", "has_glowing_text": Byte(0)},
            "back_text": {"messages": NbtList(8, empty), "color": "black", "has_glowing_text": Byte(0)},
            "is_waxed": Byte(1)}


# the texts of the signs: (nr, tag) -> [(Dutch, English)] (short lines: a sign is narrow); "naam" is made from the hole
SIGNS = {
    (10, "saus"): [("Pas op!", "Careful!"), ("Kaassaus =", "Cheese sauce ="), ("je bal wordt", "your ball turns"), ("fondue", "into fondue")],
    (10, "modder"): [("Kaasmodder!", "Cheese mud!"), ("Hier plakt", "Everything"), ("alles. Ook je", "sticks here."), ("bal. En sokjes.", "Socks too.")],
    (10, "green"): [("Bijna droog!", "Almost dry!"), ("Nog één", "Just one more"), ("bruggetje,", "little bridge,"), ("guhtje", "guhtje")],
    (11, "trechter"): [("Trechter!", "Funnel!"), ("Erin: omlaag.", "In: down you go."), ("Ernaast: nog", "Missed it:"), ("een keer.", "try again.")],
    (11, "lagen"): [("Vilt, knuffel,", "Felt, plush,"), ("kristal:", "crystal:"), ("hoe dieper,", "the deeper,"), ("hoe glimmer!", "the glitterier!")],
    (11, "beneden"): [("Kristallen", "Don't pet the"), ("niet aaien,", "crystals, they"), ("ze zijn al glad", "are smooth"), ("genoeg.", "enough already.")],
    (12, "schuif"): [("Deze vadsjes", "These vadsjes"), ("zijn verlegen:", "are shy: they"), ("ze schuiven", "keep sliding"), ("steeds weg.", "away.")],
    (12, "bomen"): [("Niet tegen de", "Don't hit the"), ("bomen slaan.", "trees. They're"), ("Die zijn al", "vads enough"), ("vads genoeg.", "already.")],
    (13, "ijs"): [("IJsvilt!", "Ice felt!"), ("Zachtjes tikken,", "Tap it gently,"), ("anders glij je", "or you slide"), ("naar de Guhzee.", "to the Guh Sea.")],
    (13, "gat"): [("Hier ging", "Guhmmie went"), ("Guhmmie naar", "down here."), ("beneden. Jouw", "Will your"), ("balletje ook?", "ball, too?")],
    (14, "gaatjes"): [("Drie gaatjes,", "Three holes,"), ("één goeie.", "one good one."), ("Welke? Njeg,", "Which? Njeg,"), ("zeggen we niet!", "we won't tell!")],
    (14, "saus"): [("Oeps! Verkeerd", "Oops! Wrong"), ("gaatje. Dit is", "hole. This one"), ("van de", "is the fondue"), ("fonduepan.", "pot's."),],
    (14, "modder"): [("Bijna!", "Almost!"), ("Nog even uit", "Just dig your"), ("de modder", "way out of"), ("wroeten.", "the mud.")],
    (15, "wachten"): [("Wacht op je", "Wait your"), ("beurt... de", "turn... the"), ("molen wacht", "mill won't"), ("niet op jou.", "wait for you.")],
    (15, "kruimels"): [("Kruimels? Van", "Crumbs? They're"), ("de koekjesguh.", "the cookie"), ("Niet opeten!", "guh's. Don't"), ("", "eat them!")],
    (16, "bank"): [("Kaats via de", "Bank off the"), ("hoek! Tilt", "corner! There's"), ("bestaat hier", "no tilt"), ("niet.", "here.")],
    (16, "putje"): [("Niet in het", "Not down the"), ("putje: de", "drain: the"), ("visjes willen", "fishies want"), ("ook rust.", "some peace too.")],
    (17, "plank"): [("Vahoeg!", "Vahoeg!"), ("Hard slaan =", "Hit hard ="), ("vliegen. Zacht", "fly. Softly ="), ("= naar beneden.", "down you go.")],
    (17, "looping"): [("Looping!", "Loop-the-loop!"), ("Niet misselijk", "Don't get"), ("worden,", "dizzy,"), ("guhtje.", "guhtje.")],
    (18, "tanden"): [("Hap-hap!", "Chomp chomp!"), ("Wacht tot", "Wait till"), ("hij gaapt.", "he yawns."), ("", "")],
    (18, "maag"): [("Welkom in de", "Welcome to the"), ("maag! Niet in", "tummy! Don't"), ("het zuur rollen,", "roll into acid,"), ("dat kriebelt.", "it tickles.")],
    (18, "trofee"): [("Alle 18 op par?", "All 18 at par?"), ("Dan krijg je", "Then you get"), ("de Golftrofee", "the Golf Trophy"), ("van de Golfguh!", "from the Golfguh!")],
    (0, "poort"): [("Achterste 9", "Back 9"), ("Rondje", "Tour of the"), ("Guhmension", "Guhmension"), ("Durf je?", "Dare you?")],
    (0, "slot"): [("Achterste 9:", "Back 9:"), ("eerst de voorste", "first play the"), ("9 op par,", "front 9 at par,"), ("guhtje!", "guhtje!")],
}


def _sign_lines(nr, tag, texts):
    import sign_text
    if tag == "naam":
        H = holes.HOLES[nr - 10]
        return [sign_text.ref("gui.guhs.golf.hole_title", "Hole %s", args=[nr], color="dark_purple", bold=True),
                {"translate": f"gui.guhs.golf.hole.{H.ID}"},
                {"translate": "gui.guhs.golf.sign_pars", "with": [str(p) for p in H.PARS]}]
    lines = SIGNS[(nr, tag)]
    return [_lijn(f"sign.guhs.golf18.{nr}_{tag}_{i}", nl, en, texts) if nl or en else "" for i, (nl, en) in enumerate(lines)]


def golf18_structure(h, texts):
    """The 18-hole course. Returns (S, info) with the spots the self-check and GolfBanen.java need."""
    from make_structures import Structure
    S = Structure((X, Y, Z))
    mc = h.mc
    rng = random.Random(1707)
    # the ground everywhere (the front 9 and the holes put their own on top)
    for x in range(X):
        for z in range(Z):
            S.set(x, BY - 1, z, mc("dirt"))
            S.set(x, BY, z, mc("grass_block"), {"snowy": "false"})
    # the front 9: the old course, block for block
    F = golf.golf_structure(h)
    fx, fy, fz = FRONT
    for (x, y, z), (name, props, nbt) in F.blocks.items():
        S.set(x + fx, y + fy, z + fz, name, props, nbt)
    for (x, y, z, nbt) in F.entities:
        S.entity(x + fx, y + fy, z + fz, nbt)
    # the gate in its east hedge, a path of ruitjesvilt to the back 9, and the signs by the gate
    gz = (fz + 60, fz + 64)
    for z in range(gz[0], gz[1] + 1):
        for y in (BY + 1, BY + 2):
            S.set(95, y, z, mc("air"))
    for x in range(94, BX):
        for z in range(gz[0] + 1, gz[1]):
            S.set(x, BY, z, PATH)
    for z in (gz[0], gz[1]):
        for y in range(BY + 1, BY + 5):
            S.set(98, y, z, mc("gold_block"))
    for z in range(gz[0], gz[1] + 1):
        S.set(98, BY + 5, z, mc("pink_concrete") if z % 2 else mc("white_concrete"))
    S.set(98, BY + 6, (gz[0] + gz[1]) // 2, "guhs:guh_kristal_lamp")
    for (z, tag) in ((gz[0] - 1, "poort"), (gz[1] + 1, "slot")):
        S.set(97, BY + 1, z, mc("cherry_sign"), {"rotation": "4", "waterlogged": "false"}, sign_nbt(_sign_lines(0, tag, texts)))
    # the back 9
    info = {"tees": {}, "cups": {}, "signs": []}
    for H, (c, r) in zip(holes.HOLES, VAKKEN):
        ox, oz = BX + c * CW, BZ + r * CD
        oy = BY - H.BASIS
        P = holes.Plot(S, ox, oy, oz, H.NR)
        H.bouw(P)
        for (x, y, z, facing, tag) in P.signs:
            name, props, _nbt = S.blocks[(x, y, z)]
            S.set(x, y, z, name, props, sign_nbt(_sign_lines(H.NR, tag, texts)))
            info["signs"].append((H.NR, tag))
        info["origin", H.NR] = (ox, oy, oz)
    # paths of ruitjesvilt along the columns (only over the lawn)
    for c in range(4):
        for x in (BX + c * CW - 3, BX + c * CW - 2):
            for z in range(1, Z - 1):
                if 0 <= x < X and S.get(x, BY, z) == "minecraft:grass_block" and S.get(x, BY + 1, z) in (None, "minecraft:air"):
                    S.set(x, BY, z, PATH)
    # a few flowers and blossom trees on the open lawn
    FLOWERS = ["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]
    for x in range(1, X - 1):
        for z in range(1, Z - 1):
            if S.get(x, BY, z) == "minecraft:grass_block" and S.get(x, BY + 1, z) is None and rng.random() < 0.04:
                if all(S.get(x + dx, BY + 1, z + dz) is None for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
                    S.set(x, BY + 1, z, rng.choice(FLOWERS))
    # the jigsaw the structure is centred on
    S.set(*MIDDEN, mc("jigsaw"), {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": "guhs:guh_golfbaan_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "joint": "rollable", "final_state": "minecraft:dirt", "selection_priority": 0, "placement_priority": 0})
    S.clear_above([(x, z) for x in range(X) for z in range(Z)], BY + 1)
    for (x, y, z), (name, props, _nbt) in S.blocks.items():
        if name == "guhs:golfbaan_afslag":
            info["tees"][(int(props["hole"]), props["niveau"])] = (x, y, z, props["facing"])
        elif name == "guhs:golfbaan_hole":
            info["cups"][int(props["hole"])] = (x, y, z)
    return S, info


# ---------------------------------------------------------------------------------------------------------------------
# the self-check
# ---------------------------------------------------------------------------------------------------------------------
PASS = ("minecraft:air", "minecraft:cherry_sign")


def check(S, info, A):
    problems = []
    for H in holes.HOLES:
        n = H.NR
        if n not in info["cups"]:
            problems.append(f"hole {n}: no cup")
        for lvl in ("makkelijk", "medium", "lastig"):
            t = info["tees"].get((n, lvl))
            if not t:
                problems.append(f"hole {n}: no {lvl} tee")
                continue
            x, y, z, facing = t
            sx, sz = golf.STEP[facing]
            free = lambda k: S.get(x - k * sx, y + 1, z - k * sz) in PASS + (None,) and S.get(x - k * sx, y + 2, z - k * sz) in PASS + (None,)                 and S.get(x - k * sx, y, z - k * sz) not in (None, "minecraft:air")
            if not (free(2) or free(1)):                       # (GolfGame.toTee: two behind the tee, or one when that's a wall)
                problems.append(f"hole {n}: nowhere to stand behind the {lvl} tee {t}")
        for tag in [t for (nr, t) in SIGNS if nr == n]:
            if (n, tag) not in info["signs"]:
                problems.append(f"hole {n}: sign {tag} is not on the course")
    for n in range(1, 10):
        if n not in info["cups"] or (n, "medium") not in info["tees"]:
            problems.append(f"front hole {n} missing")
    # every guhs: block exists (has a blockstate) and every property it gets is one of its blockstate's
    have = {f[:-5] for f in os.listdir(os.path.join(A, "blockstates"))}
    missing = sorted({name for (name, _p, _n) in S.blocks.values() if name.startswith("guhs:") and name[5:] not in have})
    if missing:
        problems.append(f"blocks without a blockstate: {missing}")
    # GolfBanen.java has the numbers of this file
    jpath = os.path.join(HERE, "..", "..", "src", "main", "java", "nl", "juiced", "guhs", "feature", "golf", "GolfBanen.java")
    text = open(jpath, encoding="utf-8").read()

    def ints(name):
        start = text.index(name)
        return [int(v) for v in re.findall(r"-?\d+", text[text.index("{", start):text.index(";", start)])]
    pars = ints("int[][] PARS_ACHTER")
    want = [H.PARS[k] for k in range(3) for H in holes.HOLES]
    if pars != want:
        problems.append(f"GolfBanen.PARS_ACHTER {pars} != {want}")
    cup1 = list(info["cups"][1])
    if ints("int[] CUP1_18") != cup1:
        problems.append(f"GolfBanen.CUP1_18 should be {cup1}")
    tee10 = list(info["tees"][(10, "medium")][:3])
    if ints("int[] TEE10_18") != tee10:
        problems.append(f"GolfBanen.TEE10_18 should be {tee10}")
    if ints("int[] ACHTER_18") != list(ACHTER):
        problems.append(f"GolfBanen.ACHTER_18 should be {list(ACHTER)}")
    if ints("int[] BAAN_18") != [0, 0, 0, X - 1, Y - 1, Z - 1]:
        problems.append(f"GolfBanen.BAAN_18 should be {[0, 0, 0, X - 1, Y - 1, Z - 1]}")
    if info["tees"][(1, "medium")][3] != "west":
        problems.append("the medium tee of hole 1 must face west (GolfBanen.TEE_FACING[0])")
    for H in holes.HOLES:                          # the whole back 9 inside the box the Golfguh scans
        for k, v in list(info["tees"].items()) + [((n, ""), c + ("",)) for n, c in info["cups"].items()]:
            if k[0] >= 10 and not (ACHTER[0] <= v[0] <= ACHTER[3] and ACHTER[1] <= v[1] <= ACHTER[4] and ACHTER[2] <= v[2] <= ACHTER[5]):
                problems.append(f"hole {k[0]}: {v} outside ACHTER_18")
        break
    return problems


# ---------------------------------------------------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------------------------------------------------
def _img(h, *path):
    return Image.open(os.path.join(h.TEX, *path)).convert("RGBA")


def _noise(base, seed, amp=8, size=16):
    r = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = r.randint(-amp, amp)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base) + (255,))
    return img


def textures(h):
    felt = _img(h, "block", "golfbaan_vilt.png")
    r = random.Random(1707)
    # ijsvilt: golf felt in icy blue, with sparkles and a few ice lines
    ijs = felt.copy()
    px = ijs.load()
    avg = [sum(px[x, y][i] for x in range(16) for y in range(16)) / 256 for i in range(3)]
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            px[x, y] = tuple(int(max(0, min(255, (c[i] - avg[i]) * 0.5 + (168, 214, 248)[i]))) for i in range(3)) + (255,)
    for _ in range(9):
        px[r.randrange(16), r.randrange(16)] = (250, 255, 255, 255)
    for k in range(1, 16, 5):
        px[(k * 3) % 16, k] = (120, 175, 225, 255)
    h.save(ijs, "block", "golfbaan_ijsvilt.png")
    # ruitjesvilt: a checker of 4 px in the colour and soft white, with a thin golden line (the argyle vads sweater)
    for name, (col, _en, _nl) in RUITJES.items():
        img = Image.new("RGBA", (16, 16))
        rr = random.Random(sum(map(ord, name)))
        for y in range(16):
            for x in range(16):
                base = col if (x // 4 + y // 4) % 2 else (252, 246, 250)
                if (x + y) % 8 == 0 and (x // 4 + y // 4) % 2 == 0:
                    base = (250, 208, 76)
                v = rr.randint(-6, 6)
                img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base) + (255,))
        h.save(img, "block", f"{name}.png")
    # the sliding guh block: pink, a guh snout, little arrows to the sides
    sg = _noise((240, 150, 190), 1708, 5)
    p = sg.load()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            p[x, y] = (200, 90, 140, 255)
    for (x, y) in ((5, 6), (10, 6), (5, 7), (10, 7)):
        p[x, y] = (40, 30, 40, 255)
    for x in range(6, 10):
        p[x, 10] = (170, 60, 110, 255)
    p[6, 9] = p[9, 9] = (170, 60, 110, 255)
    for (x, y) in ((1, 7), (2, 6), (2, 8), (14, 7), (13, 6), (13, 8)):
        p[x, y] = (255, 255, 255, 255)
    h.save(sg, "block", "golfbaan_schuifguh.png")
    rail = felt.copy()
    p = rail.load()
    for x in range(16):
        for y in (7, 8):
            p[x, y] = (150, 70, 110, 255)
        c = p[x, 6]
        p[x, 6] = (int(c[0] * 0.8), int(c[1] * 0.8), int(c[2] * 0.8), 255)
    h.save(rail, "block", "golfbaan_schuifrail.png")
    # the jaw of a snapping tooth: pink gum
    gum = _noise((226, 112, 146), 1709, 7)
    p = gum.load()
    for x in range(16):
        p[x, 15] = (250, 246, 240, 255)
    h.save(gum, "block", "golfbaan_haptand.png")
    # the bank corner: white with pink stripes along its slant
    bank = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            bank.putpixel((x, y), (236, 96, 160, 255) if ((x + y) // 3) % 2 else (250, 246, 248, 255))
    h.save(bank, "block", "golfbaan_bankhoek.png")
    # the looping: felt with a golden arrow (pointing north) and a little circle
    loop = felt.copy()
    p = loop.load()
    for i, row in enumerate(["...gg...", "..gggg..", ".gggggg.", "...gg...", "...gg...", "...gg..."]):
        for j, ch in enumerate(row):
            if ch == "g":
                p[4 + j, 2 + i] = (250, 208, 76, 255)
    for a in range(24):
        x, y = 8 + 3.2 * math.cos(a / 24 * 2 * math.pi), 11.5 + 2.6 * math.sin(a / 24 * 2 * math.pi)
        p[int(x), int(y)] = (255, 255, 255, 255)
    h.save(loop, "block", "golfbaan_looping.png")
    # the flags of holes 10-18: pink with a white two-digit number
    vlag = _img(h, "block", "golfbaan_vlag_1.png")
    roze = vlag.getpixel((0, 1))
    for n in range(10, 19):
        v = vlag.copy()
        p = v.load()
        for y in range(1, 11):
            for x in range(15):
                p[x, y] = roze
        for i, ch in enumerate(str(n)):
            for rr_, rij in enumerate(golf.DIGITS.get(ch, ["111", "101", "101", "101", "111"])):
                for cc, bit in enumerate(rij):
                    if bit == "1":
                        for dy in (0, 1):
                            for dx in (0, 1):
                                p[1 + i * 7 + cc * 2 + dx, 1 + rr_ * 2 + dy] = (255, 255, 255, 255)
        h.save(v, "block", f"golfbaan_vlag_{n}.png")
    # the garden flag: pink with a white heart
    gv = vlag.copy()
    p = gv.load()
    for y in range(1, 11):
        for x in range(15):
            p[x, y] = roze
    for (x, y) in [(5, 3), (6, 3), (9, 3), (10, 3)] + [(x, 4) for x in range(4, 12)] + [(x, 5) for x in range(4, 12)] + \
            [(x, 6) for x in range(5, 11)] + [(x, 7) for x in range(6, 10)] + [(7, 8), (8, 8)]:
        p[x, y] = (255, 255, 255, 255)
    h.save(gv, "block", "golf_vlaggetje_vlag.png")
    # the golf bag of the rack, the cart's body and its striped roof, the trophy's plinth
    tas = _noise((236, 110, 168), 1710, 6)
    p = tas.load()
    for y in range(16):
        for x in (4, 11):
            p[x, y] = (252, 246, 250, 255)
    for x in range(16):
        p[x, 2] = (250, 208, 76, 255)
    h.save(tas, "block", "golfrek_tas.png")
    kar = _noise((244, 160, 200), 1711, 5)
    p = kar.load()
    for x in range(16):
        p[x, 0] = p[x, 15] = (252, 246, 250, 255)
    for (x, y) in ((5, 6), (10, 6), (5, 7), (10, 7)):
        p[x, y] = (40, 30, 40, 255)
    for x in range(6, 10):
        p[x, 10] = (200, 70, 120, 255)
    h.save(kar, "block", "golfkarretje.png")
    dak = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dak.putpixel((x, y), (236, 96, 160, 255) if (x // 4) % 2 else (252, 248, 250, 255))
    h.save(dak, "block", "golfkarretje_dak.png")
    voet = _noise((236, 120, 170), 1712, 5)
    p = voet.load()
    for x in range(16):
        p[x, 3] = p[x, 12] = (250, 208, 76, 255)
    h.save(voet, "block", "golftrofee_voet.png")
    # item icons: a golf ball on a tee
    pal = {"w": (252, 252, 252), "s": (214, 214, 220), "p": (236, 110, 168), "d": (190, 70, 120), "e": (250, 186, 212)}
    rows = ["................", "......ee..ee....", "......ewwwwe....", ".....wwwswwww...", "....wwswwwwsww..",
            "....wwwwwwwwww..", "....wswwwwwsww..", ".....wwwsswww...", "......wwwwww....", "........pp......",
            "........pp......", ".......pppp.....", "........pd......", "........pd......", "........dd......", "................"]
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                icon.putpixel((x, y), pal[ch] + (255,))
    h.save(icon, "item", "golfbal_tee.png")


# ---------------------------------------------------------------------------------------------------------------------
# models, blockstates, loot, tags, items
# ---------------------------------------------------------------------------------------------------------------------
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def _rotated(model, extra=""):
    return {f"facing={f}{extra}": ({"model": model, "y": r} if r else {"model": model}) for f, r in ROT.items()}


def _box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
    out = {"from": frm, "to": to, "faces": {}}
    for f in faces:
        face = {"texture": tex}
        if uv:
            face["uv"] = uv
        out["faces"][f] = face
    return out


def _hole_model(h, name, flag):
    """The cup of golf.py (a hole in the felt), with a flag on a pole (flag = the flag texture) or without (None)."""
    inner = {"texture": "#inner"}
    felt = lambda cull=None: {"texture": "#felt", **({"cullface": cull} if cull else {})}
    els = [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {
        "down": felt("down"), "north": felt("north"), "south": felt("south"), "east": felt("east"), "west": felt("west"),
        "up": {"texture": "#inner"}}}]
    for frm, to in (([0, 8, 0], [3, 16, 16]), ([13, 8, 0], [16, 16, 16]), ([3, 8, 0], [13, 16, 3]), ([3, 8, 13], [13, 16, 16])):
        faces = {}
        for side in ("north", "south", "east", "west"):
            outer = (side == "west" and frm[0] == 0) or (side == "east" and to[0] == 16) or \
                    (side == "north" and frm[2] == 0) or (side == "south" and to[2] == 16)
            faces[side] = felt(side) if outer else dict(inner)
        faces["up"] = {"texture": "#rim", "uv": [frm[0], frm[2], to[0], to[2]], "cullface": "up"}
        els.append({"from": frm, "to": to, "faces": faces})
    textures = {"particle": "guhs:block/golfbaan_vilt", "felt": "guhs:block/golfbaan_vilt", "inner": "guhs:block/golfbaan_hole_binnen",
                "rim": "guhs:block/golfbaan_vilt"}
    if flag:
        els.append({"from": [7.5, 8, 7.5], "to": [8.5, 31, 8.5], "faces": {s: {"texture": "#pole"} for s in ("north", "south", "east", "west", "up")}})
        els.append({"from": [8.5, 24, 8], "to": [16.5, 30, 8], "faces": {"north": {"texture": "#flag", "uv": [16, 0, 0, 12]},
                                                                         "south": {"texture": "#flag", "uv": [0, 0, 16, 12]}}})
        textures.update({"pole": "guhs:block/golfbaan_vlag_stok", "flag": flag})
    h.w(f"{h.A}/models/block/{name}.json", {"textures": textures, "render_type": "minecraft:cutout", "elements": els})


def models(h):
    A, D = h.A, h.D
    w = h.w

    def cube(name, tex=None):
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex or f"guhs:block/{name}"}})

    def item_block(name, model=None):
        w(f"{A}/models/item/{name}.json", {"parent": model or f"guhs:block/{name}"})
        w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})

    def drop(name, functions=None, conditions=True):
        entry = {"type": "minecraft:item", "name": f"guhs:{name}"}
        if functions:
            entry["functions"] = functions
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry]}
        if conditions:
            pool["conditions"] = [{"condition": "minecraft:survives_explosion"}]
        w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [pool]})

    # simple blocks
    for name in ("golfbaan_ijsvilt", "golfbaan_schuifguh", "golfbaan_haptand"):
        cube(name)
        w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})
    item_block("golfbaan_ijsvilt")
    drop("golfbaan_ijsvilt")
    cube("golfbaan_schuifrail")
    w(f"{A}/blockstates/golfbaan_schuifrail.json", {"variants": {f"stand={s}": {"model": "guhs:block/golfbaan_schuifrail"} for s in ("geen", "a", "b")}})
    w(f"{A}/models/block/golfbaan_draaias.json", {"parent": "minecraft:block/cube_column", "textures": {
        "side": "guhs:block/golfbaan_molenas", "end": "guhs:block/golfbaan_molenas_zijkant"}})
    w(f"{A}/blockstates/golfbaan_draaias.json", {"variants": {"": {"model": "guhs:block/golfbaan_draaias"}}})
    # the bank corner: four steps (facing north: the corner is north-east)
    t = "#bank"
    els = [_box([4 * i, 0, 0], [4 * i + 4, 16, 4 * (i + 1)], t) for i in range(4)]
    w(f"{A}/models/block/golfbaan_bankhoek.json", {"textures": {"particle": "guhs:block/golfbaan_bankhoek", "bank": "guhs:block/golfbaan_bankhoek"},
                                                  "elements": els})
    w(f"{A}/blockstates/golfbaan_bankhoek.json", {"variants": _rotated("guhs:block/golfbaan_bankhoek")})
    w(f"{A}/models/block/golfbaan_looping.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/golfbaan_looping", "side": "guhs:block/golfbaan_vilt", "bottom": "guhs:block/golfbaan_vilt"}})
    w(f"{A}/blockstates/golfbaan_looping.json", {"variants": _rotated("guhs:block/golfbaan_looping")})
    # the tees and cups of all 18 holes (golf.py makes 1-9; same models)
    model = {"makkelijk": "guhs:block/golfbaan_afslag_makkelijk", "medium": "guhs:block/golfbaan_afslag", "lastig": "guhs:block/golfbaan_afslag_lastig"}
    w(f"{A}/blockstates/golfbaan_afslag.json", {"variants": {
        f"facing={f},hole={n},niveau={lvl}": ({"model": model[lvl], "y": r} if r else {"model": model[lvl]})
        for f, r in ROT.items() for n in range(1, 19) for lvl in ("makkelijk", "medium", "lastig")}})
    for n in range(10, 19):
        _hole_model(h, f"golfbaan_hole_{n}", f"guhs:block/golfbaan_vlag_{n}")
    w(f"{A}/blockstates/golfbaan_hole.json", {"variants": {f"hole={n}": {"model": f"guhs:block/golfbaan_hole_{n}"} for n in range(1, 19)}})
    # ruitjesvilt: block, stairs, slab
    from features.barbecuether import stair_states
    for name in RUITJES:
        tx = f"guhs:block/{name}"
        cube(name)
        w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            w(f"{A}/models/block/{name}_trap{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": tx, "top": tx, "side": tx}})
        w(f"{A}/blockstates/{name}_trap.json", {"variants": stair_states(f"guhs:block/{name}_trap")})
        w(f"{A}/models/block/{name}_plaat.json", {"parent": "minecraft:block/slab", "textures": {"bottom": tx, "top": tx, "side": tx}})
        w(f"{A}/models/block/{name}_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": tx, "top": tx, "side": tx}})
        w(f"{A}/blockstates/{name}_plaat.json", {"variants": {"type=bottom": {"model": f"guhs:block/{name}_plaat"},
                                                               "type=top": {"model": f"guhs:block/{name}_plaat_top"},
                                                               "type=double": {"model": f"guhs:block/{name}"}}})
        for b in (name, f"{name}_trap", f"{name}_plaat"):
            item_block(b)
        drop(name)
        drop(f"{name}_trap")
        drop(f"{name}_plaat", [{"function": "minecraft:set_count", "add": False, "count": 2, "conditions": [
            {"condition": "minecraft:block_state_property", "block": f"guhs:{name}_plaat", "properties": {"type": "double"}}]}])
    # the golf rack: a pink golf bag against the wall with three clubs and two balls on top (facing north: the wall is south)
    els = [_box([4, 0, 10], [12, 12, 15], "#tas"), _box([3.5, 11, 9.5], [12.5, 12.5, 15.5], "#goud"),
           _box([5, 12, 11], [6, 18, 12], "#ijzer"), _box([4, 17, 10], [6.5, 18.5, 12.5], "#goud"),
           _box([8, 12, 12], [9, 19, 13], "#ijzer"), _box([7, 18, 11], [9.5, 19.5, 13.5], "#goud"),
           _box([10, 12, 11], [11, 17, 12], "#ijzer"), _box([10, 16, 10], [12, 17.5, 12.5], "#goud"),
           _box([6, 2, 8.5], [8, 4, 10], "#bal"), _box([9, 6, 8.5], [11, 8, 10], "#bal")]
    w(f"{A}/models/block/golfrek.json", {"textures": {"particle": "guhs:block/golfrek_tas", "tas": "guhs:block/golfrek_tas",
                                                      "goud": "minecraft:block/gold_block", "ijzer": "minecraft:block/iron_block",
                                                      "bal": "minecraft:block/white_wool"}, "elements": els})
    w(f"{A}/blockstates/golfrek.json", {"variants": _rotated("guhs:block/golfrek")})
    item_block("golfrek")
    drop("golfrek")
    # golf balls on tees: 1-4
    spots = {1: [(8, 8)], 2: [(5, 6), (11, 10)], 3: [(5, 5), (11, 6), (7, 11)], 4: [(4, 4), (11, 5), (5, 11), (12, 12)]}
    for n, pts in spots.items():
        els = []
        for (cx, cz) in pts:
            els.append(_box([cx - 0.5, 0, cz - 0.5], [cx + 0.5, 2, cz + 0.5], "#tee"))
            els.append(_box([cx - 1.5, 2, cz - 1.5], [cx + 1.5, 5, cz + 1.5], "#bal"))
        w(f"{A}/models/block/golfbal_tee_{n}.json", {"textures": {"particle": "minecraft:block/white_wool", "bal": "minecraft:block/white_wool",
                                                                  "tee": "minecraft:block/pink_concrete"}, "elements": els})
    w(f"{A}/blockstates/golfbal_tee.json", {"variants": {f"balletjes={n}": {"model": f"guhs:block/golfbal_tee_{n}"} for n in spots}})
    w(f"{A}/models/item/golfbal_tee.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/golfbal_tee"}})
    w(f"{A}/items/golfbal_tee.json", {"model": {"type": "minecraft:model", "model": "guhs:item/golfbal_tee"}})
    w(f"{D}/loot_table/blocks/golfbal_tee.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
        {"type": "minecraft:item", "name": "guhs:golfbal_tee", "functions": [
            {"function": "minecraft:set_count", "add": False, "count": n, "conditions": [
                {"condition": "minecraft:block_state_property", "block": "guhs:golfbal_tee", "properties": {"balletjes": str(n)}}]}
            for n in (2, 3, 4)]}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # the hole with a flag (garden deco)
    _hole_model(h, "golf_vlaggetje", "guhs:block/golf_vlaggetje_vlag")
    _hole_model(h, "golf_vlaggetje_leeg", None)
    w(f"{A}/blockstates/golf_vlaggetje.json", {"variants": {"vlag=true": {"model": "guhs:block/golf_vlaggetje"},
                                                            "vlag=false": {"model": "guhs:block/golf_vlaggetje_leeg"}}})
    item_block("golf_vlaggetje")
    drop("golf_vlaggetje")
    # the golf cart (facing north: its front, the steering wheel, is north; you sit facing north)
    els = [_box([1, 2, 0], [15, 8, 16], "#kar"),
           _box([0, 0, 1], [2, 3, 4], "#wiel"), _box([14, 0, 1], [16, 3, 4], "#wiel"),
           _box([0, 0, 12], [2, 3, 15], "#wiel"), _box([14, 0, 12], [16, 3, 15], "#wiel"),
           _box([2, 8, 8], [14, 9, 14], "#zit"), _box([2, 8, 13], [14, 15, 15], "#zit"),
           _box([7, 8, 2], [9, 12, 3], "#ijzer"), _box([5, 12, 1.5], [11, 13, 3.5], "#zwart"),
           _box([1, 8, 0.5], [2, 22, 1.5], "#ijzer"), _box([14, 8, 0.5], [15, 22, 1.5], "#ijzer"),
           _box([1, 15, 14.5], [2, 22, 15.5], "#ijzer"), _box([14, 15, 14.5], [15, 22, 15.5], "#ijzer"),
           _box([0, 22, -1], [16, 23.5, 17], "#dak")]
    w(f"{A}/models/block/golfkarretje.json", {"textures": {"particle": "guhs:block/golfkarretje", "kar": "guhs:block/golfkarretje",
                                                           "wiel": "minecraft:block/black_wool", "zit": "minecraft:block/white_wool",
                                                           "ijzer": "minecraft:block/iron_block", "zwart": "minecraft:block/black_concrete",
                                                           "dak": "guhs:block/golfkarretje_dak"}, "elements": els})
    w(f"{A}/blockstates/golfkarretje.json", {"variants": _rotated("guhs:block/golfkarretje")})
    item_block("golfkarretje")
    drop("golfkarretje")
    # the golf trophy: a golden cup with handles on a pink plinth
    els = [_box([3, 0, 3], [13, 4, 13], "#voet"), _box([6, 4, 6], [10, 9, 10], "#goud"), _box([4, 9, 4], [12, 15, 12], "#goud"),
           _box([2, 10, 7], [4, 14, 9], "#goud"), _box([12, 10, 7], [14, 14, 9], "#goud"), _box([5, 15, 5], [11, 16, 11], "#roze")]
    w(f"{A}/models/block/golftrofee.json", {"textures": {"particle": "minecraft:block/gold_block", "goud": "minecraft:block/gold_block",
                                                         "voet": "guhs:block/golftrofee_voet", "roze": "minecraft:block/pink_wool"},
                                            "elements": els})
    w(f"{A}/blockstates/golftrofee.json", {"variants": _rotated("guhs:block/golftrofee")})
    item_block("golftrofee")
    w(f"{D}/loot_table/blocks/golftrofee.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "conditions": [{"condition": "minecraft:survives_explosion"}], "entries": [{"type": "minecraft:item", "name": "guhs:golftrofee",
        "functions": [{"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_name", "minecraft:lore"]}]}]}]})
    # tags
    trappen = [f"guhs:{n}_trap" for n in RUITJES]
    platen = [f"guhs:{n}_plaat" for n in RUITJES]
    for kind in ("block", "item"):
        h.add_tag(f"minecraft/tags/{kind}/stairs", trappen)
        h.add_tag(f"minecraft/tags/{kind}/slabs", platen)
    # advancements of the back 9 (given by GolfGame)
    for name in ("golf_achter_open", "golf_18", "golf_trofee"):
        w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def structure_files(h):
    """guh_golfbaan starts from the 18-hole template now (centred on its jigsaw); the old template stays for the old courses."""
    D = h.D
    path = f"{D}/worldgen/structure/guh_golfbaan.json"
    data = json.load(open(path, encoding="utf-8"))
    data["check_radius"] = 100
    data["max_height_difference"] = 30
    data["keep_clear"] = 112
    data["jigsaw"]["start_jigsaw_name"] = "guhs:guh_golfbaan_midden"
    data["jigsaw"]["max_distance_from_center"] = 116
    h.w(path, data)
    h.w(f"{D}/worldgen/template_pool/guh_golfbaan/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "guhs:guh_golfbaan18",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})


# ---------------------------------------------------------------------------------------------------------------------
# texts
# ---------------------------------------------------------------------------------------------------------------------
TEXTS = {  # key: (English, Dutch)
    "block.guhs.golfbaan_ijsvilt": ("Ice Golf Felt", "IJsvilt"),
    "block.guhs.golfbaan_schuifguh": ("Sliding Guh Block", "Schuif-guhblok"),
    "block.guhs.golfbaan_schuifrail": ("Sliding Rail", "Schuifrail"),
    "block.guhs.golfbaan_draaias": ("Turning Arm Hub", "Draaimolen-as"),
    "block.guhs.golfbaan_haptand": ("Snapping Jaw", "Hapkaak"),
    "block.guhs.golfbaan_bankhoek": ("Bank Corner", "Schuine bankhoek"),
    "block.guhs.golfbaan_looping": ("Vahoeg Looping", "Vahoeg-looping"),
    "block.guhs.golfrek": ("Golf Rack", "Golfrek"),
    "block.guhs.golfbal_tee": ("Golf Ball on a Tee", "Golfballetje op een tee"),
    "block.guhs.golf_vlaggetje": ("Hole with a Flag", "Hole met vlaggetje"),
    "block.guhs.golfkarretje": ("Golf Cart", "Golfkarretje"),
    "block.guhs.golftrofee": ("Golf Trophy", "Golftrofee"),
    "item.guhs.golftrofee.van": ("Golf Trophy of %s", "Golftrofee van %s"),
    "item.guhs.golftrofee.record": ("18 holes in %s strokes (par %s, %s)", "18 holes in %s slagen (par %s, %s)"),
    "item.guhs.golftrofee.niveau": ("Played on: %s", "Gespeeld op: %s"),
    "block.guhs.golfrek.lore": ("Deco from the Golfguh's shop. Guh golf clubs, ready for the next round!",
                                "Deco uit het winkeltje van de Golfguh. Guhgolfclubs, klaar voor het volgende rondje!"),
    "block.guhs.golfbal_tee.lore": ("Put up to 4 on one block, like candles.", "Tot 4 op één blok, net als kaarsjes."),
    "block.guhs.golf_vlaggetje.lore": ("Right-click: flag out, flag in. Your own little green!", "Rechtsklik: vlag eruit, vlag erin. Je eigen kleine green!"),
    "block.guhs.golfkarretje.lore": ("Right-click to sit in it. Vroem... well, it doesn't drive. Njeg.",
                                     "Rechtsklik om erin te zitten. Vroem... nou ja, rijden doet hij niet. Njeg."),
    "block.guhs.golftrofee.lore": ("For 18 holes at or under par. Right-click: who won it, and the record.",
                                   "Voor 18 holes op of onder par. Rechtsklik: wie hem won, en het record."),
    # the holes of the back 9
    "gui.guhs.golf.hole.blubberslalom": ("The Blubber Slalom", "De Blubberslalom"),
    "gui.guhs.golf.hole.kristalschacht": ("The Crystal Shaft", "De Kristalschacht"),
    "gui.guhs.golf.hole.schuifvadsjes": ("The Sliding Vadsjes", "De Schuifvadsjes"),
    "gui.guhs.golf.hole.guhpiek": ("The Guh Peak", "De Guhpiek"),
    "gui.guhs.golf.hole.gatenkaasgokje": ("The Holey Cheese Gamble", "Het Gatenkaasgokje"),
    "gui.guhs.golf.hole.kruimeldraaimolen": ("The Crumb Carousel", "De Kruimeldraaimolen"),
    "gui.guhs.golf.hole.kokosflipper": ("The Coconut Pinball", "De Kokosflipper"),
    "gui.guhs.golf.hole.wolkenhop": ("The Cloud Hop", "De Wolkenhop"),
    "gui.guhs.golf.hole.grote_guhmond": ("The Big Guh Mouth", "De Grote Guhmond"),
    "gui.guhs.golf.sign_pars": ("Par %s/%s/%s", "Par %s/%s/%s"),
    # the Golfguh
    "quest.guhs.golf.start_achter": ("Yay! The back 9: a Tour of the Guhmension! Your ball is on hole 10. 3... 2... 1...",
                                     "Joepie! De achterste 9: een Rondje Guhmension! Je bal ligt klaar op hole 10. 3... 2... 1..."),
    "quest.guhs.golf.start_alle": ("All 18 holes! That's a vadsy lot of golf. Your ball is on hole 1. 3... 2... 1...",
                                   "Alle 18 holes! Dat is vadsig veel golf. Je bal ligt klaar op hole 1. 3... 2... 1..."),
    "quest.guhs.golf.geen_achter": ("Njeg, this course only has 9 holes. The back 9 are on the newer golf courses!",
                                    "Njeg, deze baan heeft maar 9 holes. De achterste 9 liggen op de nieuwere golfbanen!"),
    "quest.guhs.golf.achter_dicht": ("The back 9 are still locked: first play the front 9 at or under par once, guhtje!",
                                     "De achterste 9 zijn nog dicht: speel eerst één keer de voorste 9 op of onder par, guhtje!"),
    "quest.guhs.golf.achter_open": ("Njeg! At par! The gate to the back 9 is open: a Tour of the Guhmension, from the Kaasmoeras right into the Big Guh Mouth. Dare you?",
                                    "Njeg! Op par! Het poortje naar de achterste 9 is open: een Rondje Guhmension, van het Kaasmoeras tot in de Grote Guhmond. Durf je?"),
    "quest.guhs.golf.achter_open_elders": ("Njeg! At par! The back 9 are open for you now, on every 18-hole guh golf course. This one only has 9...",
                                           "Njeg! Op par! De achterste 9 zijn nu open voor jou, op elke guhgolfbaan met 18 holes. Deze heeft er maar 9..."),
    "quest.guhs.golf.trofee": ("ALL 18 AT OR UNDER PAR! The Golfguh gives you the Golf Trophy, with your name on it. VAHOEG!",
                               "ALLE 18 OP OF ONDER PAR! De Golfguh geeft je de Golftrofee, met jouw naam erop. VAHOEG!"),
    "gui.guhs.golf.bar_n": ("Hole %s/%s: %s (par %s) | stroke %s/%s | %s blocks to go | total %s",
                            "Hole %s/%s: %s (par %s) | slag %s/%s | nog %s blokken | totaal %s"),
    "gui.guhs.golf.play_voor": ("Front 9", "Voorste 9"),
    "gui.guhs.golf.play_voor.tooltip": ("Holes 1-9, the classic guh golf course", "Hole 1-9, de klassieke guhgolfbaan"),
    "gui.guhs.golf.play_achter": ("Back 9", "Achterste 9"),
    "gui.guhs.golf.play_achter.tooltip": ("Holes 10-18: a Tour of the Guhmension, more golfballetjes per hole",
                                          "Hole 10-18: een Rondje Guhmension, meer golfballetjes per hole"),
    "gui.guhs.golf.play_alle": ("All 18", "Alle 18"),
    "gui.guhs.golf.play_alle.tooltip": ("The whole course: a big bonus, your own 18-hole record and at par the Golf Trophy",
                                        "De hele baan: een grote bonus, je eigen 18-holes record en op par de Golftrofee"),
    "gui.guhs.golf.achter_dicht": ("Locked: first play the front 9 at or under par once", "Nog dicht: speel eerst één keer de voorste 9 op of onder par"),
    "gui.guhs.golf.par_line_achter": ("Back 9: %s (total %s)  ·  your best 18: %s", "Achterste 9: %s (totaal %s)  ·  jouw beste 18: %s"),
    "gui.guhs.golf.par_line_dicht": ("Back 9 (locked): %s (total %s)  ·  best 18: %s", "Achterste 9 (dicht): %s (totaal %s)  ·  beste 18: %s"),
    "gui.guhs.scorebord.golf18": ("Guh golf top 3: 18 holes", "Guhgolf top 3: 18 holes"),
    "structure.guhs.guh_golfbaan.tooltip": ("Minigame: mini golf with the Golfguh (new courses: 18 holes), golfballetjes and a golf shop",
                                            "Minigame: minigolf met de Golfguh (nieuwe banen: 18 holes), golfballetjes en een golfwinkeltje"),
    "gui.guhs.golf.shop.tooltip": ("The golf outfit, golf shoes, building blocks and deco, for golfballetjes",
                                   "Het golfpakje, golfschoentjes, bouwblokken en deco, voor golfballetjes"),
}


def texts(h, extra):
    for key, (en, nl) in TEXTS.items():
        h.lang(key, en, nl)
    for name, (_c, en, nl) in RUITJES.items():
        h.lang(f"block.guhs.{name}", en, nl)
        h.lang(f"block.guhs.{name}_trap", f"{en} Stairs", f"{nl}trap" if nl == "Ruitjesvilt" else f"{nl} trap")
        h.lang(f"block.guhs.{name}_plaat", f"{en} Slab", f"{nl}plaat" if nl == "Ruitjesvilt" else f"{nl} plaat")
    for key, (en, nl) in extra.items():
        h.lang(key, en, nl)


def build(h):
    textures(h)
    models(h)
    structure_files(h)
    extra = {}
    S, info = golf18_structure(h, extra)
    texts(h, extra)
    problems = check(S, info, h.A)
    if problems:
        raise AssertionError("guh_golfbaan18 self-check:\n  " + "\n  ".join(problems))
    S.save("guh_golfbaan18")
    print(f"guh_golfbaan18: {len(S.blocks)} blocks, {len(info['signs'])} signs on the back 9, self-check OK")


# =====================================================================================================================
# on its own: python tools/features/golf18.py (from the project root)
# =====================================================================================================================
def _direct():
    import types
    import make_structures as ms
    import make_v2
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
    NL, EN = {}, {}

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def save(img, *path):
        full = os.path.join(A, "textures", *path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        img.save(full)

    def add_tag(path, values):
        full = f"{R}/data/{path}.json"
        data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
        for v in values:
            if v not in data["values"]:
                data["values"].append(v)
        w(full, data)

    def lang(key, en, nl):
        EN[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=R, A=A, D=D, TEX=os.path.join(A, "textures"), w=w, save=save, add_tag=add_tag, lang=lang,
                              mc=ms.mc, Structure=ms.Structure, Byte=ms.Byte, floats=ms.floats, blossom_tree=make_v2.blossom_tree)
    build(h)
    w(os.path.join("tools", "lang", "en", "c193_golf18.json"), dict(sorted(EN.items())))   # (the English overlay of a full run)
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    print(f"golf18: {len(NL)} texts")


# --- FTB Quests (tools/make_ftbquests.py): in the section of the golf course (SECTIONS: module ("golf", "golf18")) ---------------
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    q("golf18_achter_open", "De achterste negen", "Nieuwe golfbanen hebben 18 holes: een rondje langs de hele Guhmensie, van het "
      "Kaasmoeras tot de Grote Guhmond. Speel eerst de voorste 9 een keer op of onder par, dan gaan de nieuwe holes open. Ze zijn een "
      "stuk lastiger: molens, schansen, bumpers en gladde vloeren!", "guhs:golf_vlaggetje", [adv("golf_achter_open")], xp=200)
    q("golf18_18", "Rondje Guhmensie", "Speel alle 18 holes in een ronde.", "guhs:golfbal_tee", [adv("golf_18")],
      rewards=(("guhs:golfballetje", 8),), xp=250)
    q("golf18_trofee", "De Golftrofee", "Speel alle 18 holes op of onder par. De Golfguh geeft je een keer de &6Golftrofee&r, met je naam en "
      "je record erop. Niet te koop!", "guhs:golftrofee", [adv("golf_trofee")], rewards=(("guhs:golfballetje", 16),), shape="hexagon",
      xp=500)
    q("golf18_ruitjesvilt", "Ruitjesvilt", "In het winkeltje van de Golfguh: ruitjesvilt (roze, mint of geel, met trapjes en plaatjes), "
      "4 blokken voor een golfballetje.", "guhs:ruitjesvilt", [item("guhs:ruitjesvilt", 4)], xp=50)
    q("golf18_karretje", "Een eigen golfkarretje", "Het duurste uit het golfwinkeltje: een golfkarretje om in te zitten, voor 64 "
      "golfballetjes. Rijden doet hij niet, maar vads zit hij wel.", "guhs:golfkarretje", [item("guhs:golfkarretje")], shape="gear", xp=200)


if __name__ == "__main__":
    _direct()
