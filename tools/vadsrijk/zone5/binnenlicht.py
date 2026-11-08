"""[U-Z5TUIN] More light in every enterable room of Hoogvads (feedback: "meer verlichting binnen").

For every room seed (the ground floor and upper floor of every house, shop, the inn and the bank, plus every published spot that
lies under a roof: station, academy, university wings, guild workshops, soup kitchen, ...) the room is flood-filled at
head-plus-one height (floor + 3, never in anyone's way) and gets:
  - golden wall sconces (gouden_wandluchter, U-Z5BINNEN) along its walls, about every 5 blocks;
  - small round chandeliers (kroonluchter S, on a chain when the ceiling is high) in the open middle, about every 6 blocks;
  - in rooms too low for that (ceiling at floor + 3) a lantern hanging from the ceiling instead.
Rules: sconces 4 apart and not right next to an existing light, chandeliers not within 3 of one, nothing over a stair, bed or door (the two cells below must be air with a plain
floor under them), sconces only on solid walls (not glass, panes, doors, bars or leaves). The halls U-Z5BINNEN already lit
(cathedral, throne room, Raadszaal, library, chancellery) are skipped. All light blocks have no collision.
"""
import binnen2 as B2
import bouwhulp as BH
import gotiek as G

LICHT_WOORDEN = ("lantern", "luchter", "kandelaar", "candle", "lichtlijst", "glowstone", "end_rod", "torch", "shroomlight",
                 "kaarsenrand", "lamp", "lampion", "froglight")
GEEN_WAND = ("glass", "glas", "pane", "ruit", "door", "deur", "iron_bars", "leaves", "ladder", "fence", "_muur", "_wall",
             "trapdoor", "luik", "banner", "vaandel", "sign", "bord", "chain")
NIET_VLOER = ("stairs", "_trap", "_bed", "slab", "_plaat", "water", "ladder", "door")
OVERSLAAN = ("kathedraal_", "troonzaal_", "raadszaal_", "universiteit_bibliotheek", "paleis_kanselarij", "z5w_", "spuitgat",
             "kampvuur_", "markt_", "staartpark", "ooggaarde", "haven_5_4_luchtschip", "haven_5_4_mast", "wolkwaldroom")
EXTRA = [  # rooms without a spot of their own (local x, standing y, z)
    (-60, 235, -35), (-40, 235, -35), (-20, 235, -35),          # university south wing (lecture hall side)
    (-60, 235, -74), (-20, 235, -74),                            # university north wing ends
    (-70, 235, -50), (-35, 235, 72), (-44, 235, 70),             # west wing, academy hall, barracks
    (50, 237, -68), (66, 237, -68), (82, 237, -68),              # station hall
]
MAX_KAMER = 2600


def naam(W, x, y, z):
    return W.wget(int(x), int(y), int(z))


def is_licht(s):
    return s.startswith("minecraft:light[") or any(w in s for w in LICHT_WOORDEN)


class Licht:
    def __init__(self, W):
        self.W = W
        self.lichten = []          # (x, y, z) of lights placed by this pass (for spacing)
        self.oud = []              # (x, y, z) of lights that were already there
        self.wand = []             # the sconces placed by this pass
        self.gedaan = set()
        self.n_wand = self.n_kroon = self.n_lamp = 0

    def dichtbij(self, x, y, z, r=3, r_oud=2, lijst=None):
        """A light within r (placed by this pass) or r_oud (already there: the house lamp, sconces of U-Z5BINNEN)?"""
        for (a, b, c) in (self.lichten if lijst is None else lijst):
            if abs(a - x) <= r and abs(c - z) <= r and abs(b - y) <= r + 1:
                return True
        for (a, b, c) in self.oud:
            if abs(a - x) <= r_oud and abs(c - z) <= r_oud and abs(b - y) <= r_oud + 2:
                return True
        return False

    def plafond(self, x, y, z, hoog=24):
        top = G.plafond(self.W, x, y, z, y + hoog)
        return top if top < y + hoog else None

    def vloer_ok(self, x, ys, z):
        """The two cells below (x, ys, z) are air, and under them a plain floor (no stair, bed, slab, water)."""
        W = self.W
        if not (G.lucht(W, x, ys - 1, z) and G.lucht(W, x, ys - 2, z)):
            return False
        f = naam(W, x, ys - 3, z)
        return not G.lucht(W, x, ys - 3, z) and not any(t in f for t in NIET_VLOER)

    def kamer(self, x, y, z):
        """Flood fill the room of the standing spot (x, y, z) at ys = y + 2; returns (ys, cells) or None (outdoors / blocked)."""
        W = self.W
        ys = int(y) + 2
        x, z = int(x), int(z)
        if not G.lucht(W, x, ys, z):     # (a lamp hangs right above the spot: start next to it)
            for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
                if G.lucht(W, x + dx, ys, z + dz) and G.lucht(W, x + dx, ys - 1, z + dz):
                    x, z = x + dx, z + dz
                    break
        if (x, ys, z) in self.gedaan or not G.lucht(W, x, ys, z) or self.plafond(x, ys, z) is None:
            return None
        cellen, rand = {(x, z)}, [(x, z)]
        while rand and len(cellen) < MAX_KAMER:
            cx, cz = rand.pop()
            for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                p = (cx + dx, cz + dz)
                if p in cellen or abs(p[0] - x) > 60 or abs(p[1] - z) > 60:
                    continue
                if not G.lucht(W, p[0], ys, p[1]) or self.plafond(p[0], ys, p[1]) is None:
                    continue
                cellen.add(p)
                rand.append(p)
        for (cx, cz) in cellen:
            self.gedaan.add((cx, ys, cz))
        return ys, cellen

    def bestaande(self, ys, cellen):
        W = self.W
        for (cx, cz) in cellen:
            for y in range(ys - 3, ys + 8):
                s = naam(W, cx, y, cz)
                if s != "minecraft:air" and is_licht(s):
                    self.oud.append((cx, y, cz))
            for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (cx + dx, cz + dz) not in cellen:
                    for y in range(ys - 2, ys + 3):
                        s = naam(W, cx + dx, y, cz + dz)
                        if is_licht(s):
                            self.oud.append((cx + dx, y, cz + dz))

    def verlicht(self, ys, cellen):
        W = self.W
        self.bestaande(ys, cellen)
        # 1. ceiling chandeliers in the open middle (cells with all 8 neighbours in the room)
        midden = [(cx, cz) for (cx, cz) in cellen
                  if all((cx + a, cz + b) in cellen for a in (-1, 0, 1) for b in (-1, 0, 1))]
        for (cx, cz) in sorted(midden, key=lambda p: (p[0] * 31 + p[1] * 17) % 97):
            if self.dichtbij(cx, ys, cz, 4, 3) or not self.vloer_ok(cx, ys, cz):
                continue
            top = self.plafond(cx, ys, cz)
            if top is None:
                continue
            if top - ys <= 1:
                # low room: a hanging lantern from the ceiling (at floor + 3, above every head)
                if top - ys == 1:
                    BH.zet(W, cx, ys, cz, G.LANTAARN_H)
                    self.n_lamp += 1
                    self.lichten.append((cx, ys, cz))
                continue
            yh = min(top - 1, ys + 3)
            B2.kroonluchter(W, cx, yh, cz, "S", ymax=top + 1)
            self.lichten.append((cx, yh, cz))
            self.n_kroon += 1

        # 2. wall sconces (spaced against each other and the lights that were there, not against the chandeliers)
        for (cx, cz) in sorted(cellen):
            if self.dichtbij(cx, ys, cz, 3, 1, self.wand) or not self.vloer_ok(cx, ys, cz):
                continue
            for (dx, dz, facing) in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
                wx, wz = cx + dx, cz + dz
                if (wx, wz) in cellen:
                    continue
                w = naam(W, wx, ys, wz)
                onder = naam(W, wx, ys - 1, wz)
                if G.lucht(W, wx, ys, wz) or any(t in w for t in GEEN_WAND) or any(t in onder for t in ("door", "deur", "trapdoor")):
                    continue
                if B2.wandluchter(W, cx, ys, cz, facing):
                    self.wand.append((cx, ys, cz))
                    self.n_wand += 1
                    break


def bouw(W, S, info, log=print):
    import plan5 as PL
    L = Licht(W)
    seeds = []
    for h in info.get("huizen_lokaal", []):
        seeds.append(tuple(h["binnen"]))
    for k, v in sorted(info.get("plekken", {}).items()):
        if v and len(v) == 3 and v[1] is not None and not k.startswith(OVERSLAAN):
            seeds.append(tuple(v))
    for k, v in sorted(PL.plekken().items()):
        if v and v[1] is not None and not k.startswith(OVERSLAAN):
            seeds.append(tuple(v))
    seeds += EXTRA
    n_kamers = 0
    for (x, y, z) in seeds:
        r = L.kamer(x, y, z)
        if r is None:
            continue
        ys, cellen = r
        if len(cellen) < 4:
            continue
        L.verlicht(ys, cellen)
        n_kamers += 1
    log(f"binnenlicht (U-Z5TUIN): {n_kamers} kamers, {L.n_wand} wandluchters, {L.n_kroon} kroonluchters, "
        f"{L.n_lamp} hanglampen")
    return n_kamers
