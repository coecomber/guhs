"""
1.8.1 (design: MineScapeStory/ontwerp-guhs181-maagwand.md, kermis-ontwerp/BOUWPLAN_181.md, layout kermis-ontwerp/_bron/uitbouw_a.py;
Java: feature/kermisuitbouw): the template guh_kermis_uitbouw, the extension of the Guhkermis. KermisUitbouw stamps it at every
kermis (also at the ones that already stand in a world). Its frame is the frame of the kermis template guh_kermis (so it turns
along), moved by PLEK: y 1 is the kermis floor.

Standalone and idempotent:   python tools/features/kermisuitbouw.py     (from the project root)
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import make_structures as ms  # noqa: E402
from features import kermisuitbouw_kit as M  # noqa: E402

NAAM = "guh_kermis_uitbouw"
OX, OZ, G = -256, -992, 69              # kermis A in the world: the origin of its template and its floor (the kit's coordinates)
PLEK = (-40, 0, 2)                      # the template's min corner in the kermis frame (Java: KermisUitbouw.UITBOUW)
MAAT = (121, 49, 90)
SX, DZ = -238, -935                     # the main street and the cross street (world coordinates of kermis A)
EN_OVERLAY = os.path.join("tools", "lang", "en", "c281_kermisuitbouw.json")
TEKSTEN = {"gui.guhs.kermis.groeit": ("Njeg! De Guhkermis groeit! Er komen kraampjes en attracties bij, vahoeg!",
                                      "Njeg! The Guh Fair is growing! Stalls and rides are moving in, vahoeg!")}
LATER = re.compile(r"^kermis(rit|kraam)_")   # blocks of the other 1.8.1 slices


class Plan:
    """The kit's T: world coordinates of kermis A go into a template."""

    def __init__(self):
        self.s = ms.Structure(MAAT)

    def _l(self, x, y, z):
        p = (int(x) - OX - PLEK[0], int(y) - G + 1 - PLEK[1], int(z) - OZ - PLEK[2])
        if not self.s.inside(*p):
            raise SystemExit(f"kermisuitbouw: {x},{y},{z} is outside the template: {p}")
        return p

    def set(self, x, y, z, s, roof=False):
        p = self._l(x, y, z)
        naam, props = s[:-1].split("[", 1) if "[" in s else (s, "")
        if ":" not in naam:
            naam = "guhs:" + naam
        self.s.set(*p, naam, dict(kv.split("=") for kv in props.split(",")) if props else None)

    def fill(self, x1, y1, z1, x2, y2, z2, s, roof=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if s == "minecraft:air":
                        self.s.blocks.pop(self._l(x, y, z), None)
                    else:
                        self.set(x, y, z, s)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def walls(self, x1, y1, z1, x2, y2, z2, s, roof=False):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                self.set(x, y, z1, s)
                self.set(x, y, z2, s)
            for z in range(z1, z2 + 1):
                self.set(x1, y, z, s)
                self.set(x2, y, z, s)

    def guh(self, *a, **k):
        pass                                 # (the render's visitors: the stalls and rides bring their own)


def bouw():
    T = Plan()
    M.pad(T, SX - 3, -961, SX + 3, -907, G)
    M.pad(T, -272, DZ - 2, -206, DZ + 3, G)
    M.pad(T, -205, DZ, -189, DZ + 3, G)      # on to the Guhtwister and the Zweefmolen
    M.pad(T, -271, -947, -269, -938, G)      # from the cross street to the Booster
    M.pad(T, -262, -960, -242, -958, G)      # from the main street to the Booster
    M.booster(T, -277, -971, G)
    M.twister(T, -190, -915, G, "north")
    M.churros(T, SX - 4, -953, "east", G)
    M.suikerspin(T, SX - 4, -944, "east", G)
    M.poffertjes(T, -255, DZ - 3, "south", G)
    M.oliebollen(T, -264, DZ - 3, "south", G)
    M.blikkengooien(T, SX + 4, -953, "west", G)
    M.knabbelschieten(T, SX + 4, -944, "west", G)
    M.guhtjes_vissen(T, -220, -946, "south", G)
    M.zweefmolen(T, -195, -948, G, "south")
    M.botsguhs(T, -228, -929, -204, -912, G, "north")
    M.reuzenrad(T, -262, -918, G)
    M.spookhuis(T, -295, -946, -278, -924, G)
    for z in (-958, -948, -938, -926, -914):
        M.lantaarn(T, SX - 4 if z > -938 else SX - 8, z, G)
        M.lantaarn(T, SX + 4 if z > -938 else SX + 8, z, G)
    for x in (-270, -250, -214):
        M.lantaarn(T, x, DZ + 4, G)
    for z in range(-960, -906, 8):
        for x in range(SX - 3, SX + 4):
            T.set(x, G + 6, z, "vlaggetjes[axis=x]")
    return T.s


def check(s):
    """Every guhs block must exist, except the anchors of the other 1.8.1 slices."""
    bs = os.path.join("src", "main", "resources", "assets", "guhs", "blockstates")
    mist = sorted({b[0] for b in s.blocks.values() if b[0].startswith("guhs:") and not LATER.match(b[0][5:])
                   and not os.path.exists(os.path.join(bs, b[0][5:] + ".json"))})
    if mist:
        raise SystemExit(f"kermisuitbouw: unknown blocks {mist}")
    # the stall's Kermis-guh appears one block behind the counter (FACING = the customer's side): two blocks of air there
    achter = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}
    for (x, y, z), b in s.blocks.items():
        if b[0].startswith("guhs:kermiskraam_"):
            dx, dz = achter[b[1]["facing"]]
            for dy in (0, 1):
                q = (x + dx, y + dy, z + dz)
                if q in s.blocks:
                    raise SystemExit(f"kermisuitbouw: {b[0]}: {s.blocks[q][0]} stands where the Kermis-guh appears, {q}")


# the rides of feature/kermisritten: the ticket post and the way out (KermisritSoort, drawn for facing south; origin = the middle
# of the bottom of the controller block), and the parts that turn: (axis y above the controller, z range, radius, y from)
RITTEN = {"booster": ((3, 1, 9), (0, 1, 8)), "zweefmolen": ((3, 1, 11), (0, 1, 11)), "reuzenrad": ((3, 1, 6), (0, 1, 5)),
          "twister": ((3, 1, 12.5), (0, 1, 12))}
VEEG = {"booster": (40.5, 2, 6, 23), "reuzenrad": (19.5, -2, 2, 16.5)}
DRAAI = {"south": (1, 0, 0, 1), "north": (-1, 0, 0, -1), "east": (0, 1, -1, 0), "west": (0, -1, 1, 0)}


def check_ritten(s):
    """Floor and room at the post and the way out, nothing in the way of what turns."""
    leeg = lambda p: p not in s.blocks
    for (x, y, z), b in list(s.blocks.items()):
        m = re.match(r"guhs:kermisrit_(.+)", b[0])
        if not m or m.group(1) not in RITTEN:
            continue
        rit = m.group(1)
        a, c, d, e = DRAAI[b[1]["facing"]]
        for naam, (ox, oy, oz) in zip(("post", "way out"), RITTEN[rit]):
            wx, wz = x + 0.5 + a * ox + c * oz, z + 0.5 + d * ox + e * oz
            q = (int(wx // 1), y + oy, int(wz // 1))
            if not leeg(q) or not leeg((q[0], q[1] + 1, q[2])) or leeg((q[0], y, q[2])):
                raise SystemExit(f"kermisuitbouw: {rit}: no room at its {naam} {q}: {s.blocks.get(q)}")
        if rit in VEEG:
            ay, z1, z2, r = VEEG[rit]
            for (px, py, pz), n in s.blocks.items():
                if z1 <= pz - z <= z2 and r * 0.6 < ((px - x) ** 2 + (py - y - ay) ** 2) ** 0.5 <= r:
                    raise SystemExit(f"kermisuitbouw: {n[0]} at {(px, py, pz)} stands in the way of the {rit}")


def build(h):
    s = bouw()
    check(s)
    check_ritten(s)
    s.save(NAAM)
    if h is not None:
        for k, (nl, en) in TEKSTEN.items():
            h.lang(k, en, nl)
    return s


def _direct():
    build(None)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    overlay = json.load(open(EN_OVERLAY, encoding="utf-8")) if os.path.exists(EN_OVERLAY) else {}
    overlay.update({k: v[1] for k, v in TEKSTEN.items()})
    os.makedirs(os.path.dirname(EN_OVERLAY), exist_ok=True)
    with open(EN_OVERLAY, "w", encoding="utf-8", newline="\n") as f:
        json.dump(dict(sorted(overlay.items())), f, indent=2, ensure_ascii=False)
        f.write("\n")


if __name__ == "__main__":
    _direct()
