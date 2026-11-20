"""1.10.10 economie: droomzand comes faster, and a guh slimeball counts as a slimeball.

Java: feature/droomzand (the zandhoopjes), sterrenveld/VallendeSter + Sterrenreeks (golden star, Sterrenreeks), the x3
numbers in their own features, entity/GuhSlimeEntity (knuffelen). The slijmballen tags, the guh -> plain slimeball recipe,
the slime spawns and loot are in make_v2.py (with the rest of the guh slime); the recipes that take #guhs:slijmballen are in
their own generators; the droomwinkel prices in droomwinkel.py.

Here:
  * the golden droomsterretje's two looks (items droomsterretje_goud, droomsterretje_goud_vaag: only for the renderer);
  * the droomzandhoopje (guhs:droomzandhoopje): blockstate, a stepped little heap model with the glittering droomzandblok
    texture, item model + client item;
  * data/guhs/dromen/zandhoopjes.json: three heaps per dream island, picked from the island templates (open ground you can
    walk to from the arrival spot, under open sky, not next to a wall, a sign or a creature, at least 3 blocks from the edge,
    spread out over the island);
  * the texts (Dutch + English; the English also in tools/lang/en/c511_economie.json).

Standalone (from the project root):  PYTHONPATH="tools;tools/features" python tools/features/economie1110.py
"""
import gzip
import json
import math
import os
import struct
import sys
from collections import deque

from PIL import Image

EILANDEN = ["sterren", "molentje", "badje", "herinnering", "liedjes"]
PER_EILAND = 3
VERSIE = 1

TEKSTEN = {
    # the golden star + the Sterrenreeks
    "item.guhs.droomsterretje_goud": ("Gouden sterretje", "Golden little star"),
    "item.guhs.droomsterretje_goud_vaag": ("Vervagend gouden sterretje", "Fading golden little star"),
    "gui.guhs.sterren.goud": ("Een GOUDEN sterretje! %s droomzand, vahoeg!", "A GOLDEN little star! %s dream sand, hooray!"),
    "gui.guhs.sterren.reeks": ("Sterrenreeks! +%s", "Star streak! +%s"),
    # the zandhoopje
    "block.guhs.droomzandhoopje": ("Droomzandhoopje", "Little Heap of Dream Sand"),
    "block.guhs.droomzandhoopje.lore": ("Een hoopje glinsterend droomzand. In een droom mag je er één keer uit scheppen, vads!",
                                        "A little heap of glittering dream sand. In a dream you may scoop from it once, vads!"),
    "gui.guhs.zandhoopje.geschept": ("Je schept %s droomzand uit het hoopje. Glinster glinster!",
                                     "You scoop %s dream sand out of the heap. Twinkle twinkle!"),
    "gui.guhs.zandhoopje.al": ("Dit hoopje heb je al leeggeschept", "You already scooped this heap empty"),
    "gui.guhs.zandhoopje.geen_droom": ("Droomzand schep je alleen in een droom, vadsje.", "You only scoop dream sand in a dream, vadsje."),
    # knuffelen with a guh slime
    "gui.guhs.guh_slime.knuffel": ("Squish! De guh-slime giechelt en geeft je een guhslijmbal.",
                                   "Squish! The guh slime giggles and gives you a guh slimeball."),
    "gui.guhs.guh_slime.uitgeknuffeld": ("Deze guh-slime is even uitgeknuffeld", "This guh slime has had enough cuddles for now"),
}


# =====================================================================================================================
# the golden star (the same star shape as dromen_sterren.ster_textuur, in deep gold with white glints)
# =====================================================================================================================
def gouden_ster(alpha, glans):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 8.0
            a = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            rand = 3.2 + 4.0 * (0.5 + 0.5 * math.cos(5 * (a + math.pi / 2))) ** 3
            if r <= rand:
                c = (214, 128, 10) if r > rand - 1.1 else (255, 190, 30)
                if r < 2.2:
                    c = (255, 226, 96)
                c = tuple(min(255, int(v + glans)) for v in c)
                px[x, y] = c + (alpha,)
    for (x, y) in ((6, 5), (7, 5), (6, 6)):                       # a glint top left
        px[x, y] = (255, 255, 236, alpha)
    for (x, y) in ((1, 2), (14, 3), (2, 13), (13, 14)):           # four little sparkles around it
        px[x, y] = (255, 250, 200, alpha)
    for (x, y, c) in ((6, 8, (70, 36, 20)), (9, 8, (70, 36, 20)), (5, 10, (255, 130, 110)), (10, 10, (255, 130, 110)),
                      (7, 10, (130, 60, 30)), (8, 10, (130, 60, 30))):
        px[x, y] = c + (alpha,)
    return im


# =====================================================================================================================
# the zandhoopje spots, from the island templates
# =====================================================================================================================
class _P:
    """A tiny NBT reader (big endian, gzip already undone)."""

    def __init__(self, d):
        self.d, self.p = d, 0

    def rd(self, f):
        v = struct.unpack_from(f, self.d, self.p)
        self.p += struct.calcsize(f)
        return v[0]

    def s(self):
        n = self.rd(">H")
        t = self.d[self.p:self.p + n].decode("utf-8")
        self.p += n
        return t

    def v(self, t):
        if t in (1, 2, 3, 4, 5, 6):
            return self.rd({1: ">b", 2: ">h", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}[t])
        if t == 8:
            return self.s()
        if t == 9:
            et, n = self.rd(">B"), self.rd(">i")
            return [self.v(et) for _ in range(n)]
        if t == 10:
            o = {}
            while True:
                tt = self.rd(">B")
                if tt == 0:
                    return o
                k = self.s()
                o[k] = self.v(tt)
        if t in (7, 11, 12):
            n = self.rd(">i")
            size = {7: 1, 11: 4, 12: 8}[t]
            out = list(struct.unpack_from(f">{n}{'biq'[[7, 11, 12].index(t)]}", self.d, self.p))
            self.p += n * size
            return out
        raise ValueError(t)


def lees_template(pad):
    d = gzip.decompress(open(pad, "rb").read())
    p = _P(d)
    assert p.rd(">B") == 10
    p.s()
    return p.v(10)


# no collision: you walk through these (and a heap may not stand IN one: a heap needs real air)
DOORLOOP = ("minecraft:air", "guhs:pluisgras", "guhs:maanbloemetje", "guhs:roze_guhbloem", "minecraft:allium",
            "guhs:wolkenweide_rank", "minecraft:birch_sign", "minecraft:birch_wall_sign", "minecraft:cherry_sign")
# you can't stand on these (water, bouncy pillows, fences you can't climb)
NIET_STAAN = ("minecraft:water", "guhs:droomzee", "guhs:stuiterkussen_", "guhs:guh_hekje_", "minecraft:iron_bars",
              "guhs:zeepbel", "minecraft:big_dripleaf")
# good ground for a heap
GROND = ("guhs:pluiswolblok", "guhs:knuffelgras", "guhs:slaapliedjesgras", "guhs:maansteen", "guhs:maansteen_tegels",
         "guhs:droomzandblok", "guhs:wolkenblok_wit", "guhs:wolkenblok_roze", "guhs:knuffelsteen", "minecraft:moss_block",
         "guhs:tegel_", "guhs:knuffelbad_badtegel", "minecraft:smooth_quartz")


def _is(naam, lijst):
    return any(naam == x or (x.endswith("_") and naam.startswith(x)) for x in lijst)


def kies_plekken(eiland, opzet, root):
    """Three heap spots on this island (offsets from its centre), or a SystemExit when it can't find them."""
    pal = root["palette"]
    sx, sy, sz = root["size"]
    blok = {}
    for b in root["blocks"]:
        blok[tuple(b["pos"])] = pal[b["state"]]["Name"]
    naam = lambda c: blok.get(c, "minecraft:air")            # (no block in a template = cleared to air by the stamp)
    door = lambda c: _is(naam(c), DOORLOOP)
    staan = lambda c: not door(c) and not _is(naam(c), NIET_STAAN)
    loopbaar = lambda c: staan((c[0], c[1] - 1, c[2])) and door(c) and door((c[0], c[1] + 1, c[2]))

    m = opzet["midden"]
    plek = lambda rel: (m[0] + math.floor(rel[0]), m[1] + math.floor(rel[1]), m[2] + math.floor(rel[2]))
    start = plek(opzet["aankomst"]["plek"])
    if not loopbaar(start):
        raise SystemExit(f"economie1110: the arrival of {eiland} {start} is no walkable spot")
    # walk from the arrival: a step up of one block, a drop of at most three (you float softly in a dream anyway)
    afstand = {start: 0}
    rij = deque([start])
    while rij:
        c = rij.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1, -2, -3):
                n = (c[0] + dx, c[1] + dy, c[2] + dz)
                if not (0 <= n[0] < sx and 1 <= n[1] < sy - 1 and 0 <= n[2] < sz) or n in afstand:
                    continue
                if dy == 1 and not door((c[0], c[1] + 2, c[2])):
                    continue                                   # no head room for the step up
                if dy < 0 and not all(door((n[0], c[1] + k, n[2])) for k in range(dy + 1, 2)):
                    continue                                   # the way down must be open
                if loopbaar(n):
                    afstand[n] = afstand[c] + 1
                    rij.append(n)
                    break

    weg = [plek(opzet["aankomst"]["plek"])]
    for k in ("steiger",):
        if k in opzet:
            weg.append(plek(opzet[k]))
    for v in opzet.get("koor", {}).values():
        weg.append(plek(v))
    wezens = [tuple(e["blockPos"]) for e in root.get("entities", [])]
    max_y = max((p[1] for p in blok), default=0)

    def goed(c):
        x, y, z = c
        if naam(c) != "minecraft:air" or not _is(naam((x, y - 1, z)), GROND):
            return False
        if not all(naam((x, k, z)) == "minecraft:air" for k in range(y + 1, max_y + 1)):
            return False                                       # open sky above: you see it glitter from afar
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for dy in (0, 1):
                    if (dx or dz) and not door((x + dx, y + dy, z + dz)):
                        return False                           # not against a wall, a fence or a sign
                if (dx or dz) and not staan((x + dx, y - 1, z + dz)):
                    return False                               # ground all around (not on an edge or a pillow)
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if not any(staan((x + dx, y - k, z + dz)) for k in (1, 2)):
                    return False                               # not near the island's edge
        if any(math.dist((x, z), (w[0], w[2])) < 6 for w in weg):
            return False                                       # not on the arrival, the steiger or a koor spot
        if any(math.dist(c, w) < 3 for w in wezens):
            return False                                       # not where a guh stands
        return True

    kandidaten = [c for c, d in afstand.items() if 8 <= d <= 60 and goed(c)]
    if len(kandidaten) < PER_EILAND:
        raise SystemExit(f"economie1110: only {len(kandidaten)} heap spots on {eiland}")
    # spread out: the first one far from the arrival (but not the farthest corner), then each next one far from the others
    kandidaten.sort()
    gekozen = []
    eerste = sorted(kandidaten, key=lambda c: (abs(afstand[c] - 22), c))[0]
    gekozen.append(eerste)
    while len(gekozen) < PER_EILAND:
        gekozen.append(max(kandidaten, key=lambda c: (min(math.dist(c, g) for g in gekozen + [start]) - 0.15 * max(0, afstand[c] - 35), c)))
    return [[c[0] - m[0], c[1] - m[1], c[2] - m[2]] for c in gekozen], afstand


def plekken():
    data = os.path.join("src", "main", "resources", "data", "guhs")
    uit = {}
    for e in EILANDEN:
        opzet = json.load(open(os.path.join(data, "dromen", "eiland", e + ".json"), encoding="utf-8"))
        root = lees_template(os.path.join(data, "structure", "dromen", f"eiland_{e}.nbt"))
        uit[e], _ = kies_plekken(e, opzet, root)
    return {"versie": VERSIE, "eilanden": uit}


# =====================================================================================================================
# writing
# =====================================================================================================================
def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def bestanden(r, w=_schrijf_json):
    a = os.path.join(r, "assets", "guhs")
    tex = os.path.join(a, "textures")
    os.makedirs(os.path.join(tex, "item"), exist_ok=True)
    for naam, alpha, glans in (("droomsterretje_goud", 255, 0), ("droomsterretje_goud_vaag", 120, 40)):
        gouden_ster(alpha, glans).save(os.path.join(tex, "item", naam + ".png"))
        w(os.path.join(a, "models", "item", naam + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/" + naam}})
        w(os.path.join(a, "items", naam + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + naam}})
    # the heap: three steps of droomzand (the glittering, animated droomzandblok texture)
    t = "guhs:block/droomzandblok"

    def stap(van, tot, uv):
        return {"from": van, "to": tot, "faces": {k: {"texture": "#zand", "uv": uv if k in ("up", "down") else
                                                      [van[0], 16 - tot[1], tot[0], 16 - van[1]]} for k in
                                                  ("north", "south", "east", "west", "up", "down")}}
    w(os.path.join(a, "models", "block", "droomzandhoopje.json"), {
        "parent": "minecraft:block/block", "textures": {"zand": t, "particle": t},
        "elements": [stap([1, 0, 1], [15, 3, 15], [1, 1, 15, 15]), stap([3, 3, 3], [13, 5, 13], [3, 3, 13, 13]),
                     stap([5, 5, 5], [11, 7, 11], [5, 5, 11, 11])]})
    w(os.path.join(a, "blockstates", "droomzandhoopje.json"), {"variants": {"": {"model": "guhs:block/droomzandhoopje"}}})
    w(os.path.join(a, "models", "item", "droomzandhoopje.json"), {"parent": "guhs:block/droomzandhoopje"})
    w(os.path.join(a, "items", "droomzandhoopje.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/droomzandhoopje"}})
    w(os.path.join(r, "data", "guhs", "dromen", "zandhoopjes.json"), plekken())


def build(h):
    bestanden(h.R, h.w)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    r = os.path.join("src", "main", "resources")
    bestanden(r)
    _schrijf_json(os.path.join("tools", "lang", "en", "c511_economie.json"), {k: en for k, (nl, en) in TEKSTEN.items()})
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(r, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf_json(p, data)
    z = json.load(open(os.path.join(r, "data", "guhs", "dromen", "zandhoopjes.json"), encoding="utf-8"))
    print(f"economie1110: 2 star looks, the zandhoopje, {sum(len(v) for v in z['eilanden'].values())} heaps, {len(TEKSTEN)} texts")
    for e, v in z["eilanden"].items():
        print(f"  {e}: {v}")


if __name__ == "__main__":
    _direct()
