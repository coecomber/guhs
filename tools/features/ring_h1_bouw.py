"""
bbq2 (ring-h1) - what chapter 1 of the Knabbelring builds: the Knabbelgouw, Guhdalf's camp and the test room.

  knabbelgouw      (81 x 26 x 81, structure guhs:knabbelgouw, Guhmensie only) the BIG barbecueput (the very build of
                   barbecuether_put.groot, with its Grillguh) and around it, as one whole: five heuvelholletjes (round doors
                   in every colour, a room inside each), the feestwei with the feestboom, long tables, a dance floor and
                   Guhdalf's camp, Sam-guh's moestuin, a pond with a little bridge, paths, hedges, signposts. The ground
                   layer is layer G = 4 (as the pit's own); the centre jigsaw is in layer 0 (the start pool sinks the piece
                   G + 1, see ring_h1.structuur).
  ringh1_kamp      (15 x 9 x 11) Guhdalf's camp alone: his pointed tent, his cart full of fireworks, the party table, the
                   provisions and a Rustvuurtje on a plate of trodden ground. Bezetting puts it on the free strip around a big
                   barbecueput that was generated before this update; the Knabbelgouw has the very same camp on its feestwei
                   (so a scene written against the camp fits both).
  ringh1_test_kamer  RingH1GameTests: a floor with room for the camp.

Everything is built from parts (Stuk) in ONE direction and turned into place (block states and entities turn along).

  bouw_gouw(h) -> (Structure, problems)      bouw_kamp(h) -> (Structure, problems)
  preview(out)   pictures (python tools/features/ring_h1_bouw.py <out>)
"""
import math
import os
import random
import sys

G = 4                       # the ground layer (its top block is what you walk on), as barbecuether_put.G_BIG
SIZE = (81, 26, 81)
C = 40                      # the middle (the centre jigsaw sits here, in layer 0)
MIDDEN = "guhs:knabbelgouw_midden"
PUT = (22, 42)              # where the corner of the big pit (37 x 37) goes: its middle is (40, 60)

# --- the camp (also the prop template ringh1_kamp); positions are camp-local, y 0 = the plate -------------------------------
KAMP = (15, 9, 11)
KAMP_GUHDALF = (10, 1, 6)   # Guhdalf sits here and looks south (yaw 0): Java Kamp.GUHDALF
KAMP_TAFEL = (7, 1, 7)      # the party table: Java Kamp.TAFEL (how the camp's direction is read back)
KAMP_KIST = (10, 2, 2)      # the fireworks crate on the cart
KAMP_VUUR = (3, 1, 8)       # the Rustvuurtje
KAMP_PROVIAND = ((12, 1, 6), (13, 1, 7), (12, 1, 8))
KAMP_SAM = (5, 1, 9)        # where Sam-guh waits at a camp without a Gouw: Java Kamp.SAM
# where the camp stands in the Knabbelgouw (its corner, turned KAMP_KWART quarters clockwise) and Sam-guh's own garden
GOUW_KAMP = (46, 17)
GOUW_KAMP_KWART = 0
GOUW_SAM = (10, G + 1, 47)

AIR = "minecraft:air"
GRAS = "minecraft:grass_block"
AARDE = "minecraft:dirt"
PAD = "minecraft:dirt_path"
RICHTING = ["north", "east", "south", "west"]
BLOEMEN = ["minecraft:poppy", "minecraft:dandelion", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:azure_bluet",
           "minecraft:pink_tulip", "minecraft:allium", "guhs:knabbelroos", "guhs:kaasbloem"]
VUURWERKKIST = "guhs:ringh1_vuurwerkkist"
FEESTTAFEL = "guhs:ringh1_feesttafel"
PROVIAND = "guhs:ringh1_proviand"
SCHOORSTEEN = "guhs:ringh1_schoorsteentje"
VUUR = "guhs:ring_rustvuur"


def trap(facing, half="bottom", shape="straight"):
    return {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"}


def plaat(soort="bottom"):
    return {"type": soort, "waterlogged": "false"}


def hek(**kw):
    p = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
    p.update(kw)
    return p


def luik(facing, open_="true", half="bottom"):
    return {"facing": facing, "half": half, "open": open_, "powered": "false", "waterlogged": "false"}


def lantaarn(hangt=False):
    return {"hanging": "true" if hangt else "false", "waterlogged": "false"}


def draai_props(props, kwart):
    """The block state of a block that is turned `kwart` quarters clockwise (seen from above)."""
    k = kwart % 4
    if not props or not k:
        return props
    p = dict(props)
    if p.get("facing") in RICHTING:
        p["facing"] = RICHTING[(RICHTING.index(p["facing"]) + k) % 4]
    if p.get("axis") in ("x", "z") and k % 2:
        p["axis"] = "z" if p["axis"] == "x" else "x"
    if "rotation" in p:
        p["rotation"] = str((int(p["rotation"]) + 4 * k) % 16)
    if all(d in p for d in RICHTING):
        was = [props[d] for d in RICHTING]
        for i, d in enumerate(RICHTING):
            p[RICHTING[(i + k) % 4]] = was[i]
    return p


class Stuk:
    """A part of a build in its own coordinates (y 0 = the ground layer), pasted into a Structure turned and moved."""

    def __init__(self, h, size):
        self.h = h
        self.size = size
        self.blocks = {}
        self.entities = []

    def set(self, x, y, z, name, props=None, nbt=None):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = (name if ":" in name else "minecraft:" + name, props or {}, nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def vrij(self, x, y, z):
        return self.get(x, y, z) in (None, AIR)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def entity(self, x, y, z, nbt):
        self.entities.append((x, y, z, nbt))

    def plek(self, x, z, kwart):
        """Block (x, z) of this part after `kwart` quarter turns clockwise."""
        w, d = self.size[0], self.size[2]
        for _ in range(kwart % 4):
            x, z = d - 1 - z, x
            w, d = d, w
        return x, z

    def plak(self, doel, ox, oy, oz, kwart=0):
        for (x, y, z), (name, props, nbt) in self.blocks.items():
            xx, zz = self.plek(x, z, kwart)
            doel.set(ox + xx, oy + y, oz + zz, name, draai_props(props, kwart), nbt)
        w, d = self.size[0], self.size[2]
        for (x, y, z, nbt) in self.entities:
            ww, dd = w, d
            for _ in range(kwart % 4):
                x, z = dd - z, x
                ww, dd = dd, ww
            nbt = dict(nbt)
            if kwart % 4 and "Rotation" in nbt:
                nbt["Rotation"] = self.h.floats((float(nbt["Rotation"][0]) + 90.0 * kwart) % 360.0, float(nbt["Rotation"][1]))
            doel.entity(ox + x, oy + y, oz + z, nbt)


def bord(h, s, x, y, z, regels, rotatie=None, facing=None, hout="spruce"):
    """A sign (standing with `rotatie` 0..15, or on a wall facing `facing`); the lines are translate keys (sign_text)."""
    import sign_text
    B = h.Byte
    leeg = sign_text.messages("sign.guhs.ring_h1", ["", "", "", ""])
    nbt = {"id": "minecraft:sign", "is_waxed": B(1),
           "front_text": {"messages": sign_text.messages("sign.guhs.ring_h1", regels), "color": "black", "has_glowing_text": B(0)},
           "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}}
    if facing:
        s.set(x, y, z, f"minecraft:{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        s.set(x, y, z, f"minecraft:{hout}_sign", {"rotation": str(rotatie or 0), "waterlogged": "false"}, nbt)


# =====================================================================================================================
# Guhdalf's camp
# =====================================================================================================================
def kamp(h, met_sam=False, guhdalf=None):
    """Guhdalf's camp as a part (15 x 9 x 11, the open side and the party table to the south): a plate of trodden ground,
    his pointed blue tent with a bedroll, his cart with the fireworks crate, the party table under a line of flags, the
    provisions, a Rustvuurtje with two log seats. `guhdalf`: the entity NBT of Guhdalf (the Knabbelgouw has him in its
    template; at an old pit Bezetting makes him)."""
    W, H, D = KAMP
    k = Stuk(h, KAMP)
    rng = random.Random(21301610)
    # the plate: trodden earth in the middle, grass and coarse dirt at the frayed edge
    for x in range(W):
        for z in range(D):
            rand = min(x, W - 1 - x, z, D - 1 - z)
            hoek = (x in (0, W - 1)) and (z in (0, D - 1))
            if hoek:
                continue
            if rand == 0:
                top = rng.choice([GRAS, GRAS, "minecraft:coarse_dirt"])
            elif rand == 1:
                top = rng.choice([PAD, PAD, "minecraft:coarse_dirt", GRAS])
            else:
                top = PAD if rng.random() < 0.9 else "minecraft:coarse_dirt"
            k.set(x, 0, z, top)
    # --- the tent (x 1..5, z 0..4), its flap to the south: walls of canvas, a roof that climbs to a point, a little flag ---
    DOEK, TRAP, PLAAT = "guhs:campingmarkt_tentdoek_blauw", "guhs:campingmarkt_tentdoek_blauw_trap", "guhs:campingmarkt_tentdoek_creme_plaat"
    for x in range(1, 6):
        for z in range(0, 5):
            k.set(x, 0, z, "minecraft:spruce_planks")
            rand = x in (1, 5) or z in (0, 4)
            if rand and not (x == 3 and z == 4):
                k.set(x, 1, z, DOEK if (x + z) % 2 else "guhs:campingmarkt_tentdoek_creme")
    def ring(y, x0, x1, z0, z1, gat=None):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if not (x in (x0, x1) or z in (z0, z1)) or (x, z) == gat:
                    continue
                if x == x0 and z == z0:
                    p = trap("south", shape="outer_left")
                elif x == x1 and z == z0:
                    p = trap("south", shape="outer_right")
                elif x == x0 and z == z1:
                    p = trap("north", shape="outer_right")
                elif x == x1 and z == z1:
                    p = trap("north", shape="outer_left")
                elif z == z0:
                    p = trap("south")
                elif z == z1:
                    p = trap("north")
                elif x == x0:
                    p = trap("east")
                else:
                    p = trap("west")
                k.set(x, y, z, TRAP, p)
    ring(2, 1, 5, 0, 4, gat=(3, 4))
    ring(3, 2, 4, 1, 3)
    k.set(3, 4, 2, DOEK)
    k.set(3, 5, 2, "minecraft:spruce_fence", hek())
    k.set(3, 6, 2, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    k.set(3, 1, 1, "guhs:guh_slaapzak", {"facing": "south", "occupied": "false"})
    k.set(2, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    k.set(4, 1, 2, "minecraft:lantern", lantaarn())
    k.set(2, 1, 3, "guhs:gray_kussen", {"facing": "south"})
    # --- the cart (x 8..12, z 1..3): a bed of planks on four wheels, shafts to the south, the load on top ---
    for x in range(9, 12):
        for z in range(1, 4):
            k.set(x, 1, z, "minecraft:spruce_slab", plaat("top"))
    for z in (1, 3):
        k.set(8, 1, z, "minecraft:dark_oak_trapdoor", luik("west"))
        k.set(12, 1, z, "minecraft:dark_oak_trapdoor", luik("east"))
    for x in (9, 11):
        k.set(x, 1, 4, "minecraft:spruce_fence", hek(north="true"))
    for x in range(9, 12):
        k.set(x, 2, 0, "minecraft:spruce_trapdoor", luik("north"))
    k.set(8, 2, 1, "minecraft:spruce_trapdoor", luik("west"))
    k.set(8, 2, 2, "minecraft:spruce_trapdoor", luik("west"))
    k.set(12, 2, 1, "minecraft:spruce_trapdoor", luik("east"))
    k.set(*KAMP_KIST, VUURWERKKIST)
    k.set(9, 2, 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    k.set(10, 2, 1, "minecraft:hay_block", {"axis": "x"})
    k.set(11, 2, 1, "minecraft:red_wool")
    k.set(11, 3, 1, "minecraft:lantern", lantaarn())
    k.set(9, 2, 3, "minecraft:white_wool")
    k.set(11, 2, 2, "guhs:block_of_kaasknabbels")
    # --- the party table in the middle of the open side, a bench on either side, a line of little flags over it ---
    k.set(*KAMP_TAFEL, FEESTTAFEL)
    k.set(6, 1, 7, "guhs:guh_bank", {"facing": "east"})
    k.set(8, 1, 7, "guhs:guh_bank", {"facing": "west"})
    for x in (4, 10):
        for y in (1, 2, 3):
            k.set(x, y, 5, "minecraft:spruce_fence", hek())
        k.set(x, 4, 5, "guhs:lampion_geel" if x == 4 else "guhs:lampion_roze", {"hanging": "false"})
    for x in range(5, 10):
        k.set(x, 3, 5, "guhs:vlaggetjes", {"axis": "x"})
    # --- the provisions next to the cart: three open crates (worst, kaas, knabbels) and what would not fit ---
    for i, (x, y, z) in enumerate(KAMP_PROVIAND):
        k.set(x, y, z, PROVIAND, {"soort": str(i)})
    k.set(13, 1, 5, "minecraft:barrel", {"facing": "up", "open": "false"})
    k.set(13, 2, 5, "minecraft:brown_carpet")
    k.set(13, 1, 8, "minecraft:hay_block", {"axis": "y"})
    # --- the Rustvuurtje with two log seats ---
    k.set(*KAMP_VUUR, VUUR)
    k.set(1, 1, 8, "minecraft:spruce_log", {"axis": "z"})
    k.set(3, 1, 10, "minecraft:spruce_log", {"axis": "x"})
    k.set(1, 1, 6, "minecraft:lantern", lantaarn())
    bord(h, k, 13, 1, 9, ["Guhdalf de Grijze", "Vuurwerk & feesten", "Niet aankomen!", "(njeg)"], rotatie=2)
    if guhdalf is not None:
        k.entity(KAMP_GUHDALF[0] + 0.5, KAMP_GUHDALF[1], KAMP_GUHDALF[2] + 0.5, guhdalf)
    return k


# =====================================================================================================================
# a heuvelholletje
# =====================================================================================================================
class Hol:
    """One hill with a hole in it, the round door to the south (z+). rx, rz: half the width and depth of the mound; hoog: its
    height; iw, diep: the room (2 * iw + 1 wide); hout: the wood of the door ("warped" = green...)."""

    def __init__(self, h, seed, rx, rz, hoog, iw, diep, hout, snede=3, boom=False):
        self.h, self.rx, self.rz, self.hoog, self.iw, self.diep, self.hout = h, rx, rz, hoog, iw, diep, hout
        self.rng = random.Random(seed)
        self.fz = rz + snede                      # the face of the hill
        self.stuk = Stuk(h, (2 * rx + 1, hoog + 12, self.fz + 5))
        self.dx = rx                              # the door
        self.problems = []
        self._heuvel()
        self._kamer()
        self._gevel()
        self._tuintje()
        if boom:
            self._boom(rx - 5, rz - 1)

    def in_heuvel(self, x, y, z):
        if y < 1 or z > self.fz:
            return False
        return ((x - self.rx) / (self.rx + 0.45)) ** 2 + ((z - self.rz) / (self.rz + 0.45)) ** 2 + ((y - 0.3) / (self.hoog + 0.2)) ** 2 <= 1.0

    def top(self, x, z):
        """The highest layer of the hill in this column (0: no hill here)."""
        for y in range(self.hoog + 1, 0, -1):
            if self.in_heuvel(x, y, z):
                return y
        return 0

    def _heuvel(self):
        s = self.stuk
        for x in range(s.size[0]):
            for z in range(self.fz + 1):
                t = self.top(x, z)
                if not t:
                    continue
                s.set(x, 0, z, AARDE)
                for y in range(1, t + 1):
                    s.set(x, y, z, GRAS if y == t else AARDE)
                # what grows on the hill
                r = self.rng.random()
                if z < self.fz and r < 0.16:
                    s.set(x, t + 1, z, "minecraft:short_grass")
                elif z < self.fz and r < 0.22:
                    s.set(x, t + 1, z, self.rng.choice(BLOEMEN))
                elif z < self.fz and r < 0.24:
                    s.set(x, t + 1, z, "minecraft:fern")

    def _kamer(self):
        s, rx, fz, iw, diep = self.stuk, self.rx, self.fz, self.iw, self.diep
        x0, x1, z0, z1 = rx - iw, rx + iw, fz - diep, fz - 1
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if not self.in_heuvel(x, 5, z):
                    self.problems.append(f"the room has no roof of earth at {(x, z)}")
                s.set(x, 0, z, "minecraft:spruce_planks")
                for y in (1, 2, 3):
                    s.set(x, y, z, AIR)
                s.set(x, 4, z, "minecraft:dark_oak_planks" if (x - x0) % 3 == 0 else "minecraft:birch_planks")
        for y in (1, 2, 3):
            for x in range(x0 - 1, x1 + 2):
                for z in (z0 - 1,):
                    s.set(x, y, z, "minecraft:stripped_birch_log" if (x - x0) % 3 == 0 else "minecraft:birch_planks",
                          {"axis": "y"} if (x - x0) % 3 == 0 else None)
            for z in range(z0, z1 + 1):
                for x in (x0 - 1, x1 + 1):
                    s.set(x, y, z, "minecraft:stripped_birch_log" if (z - z0) % 3 == 1 else "minecraft:birch_planks",
                          {"axis": "y"} if (z - z0) % 3 == 1 else None)
        self.kamer = (x0, x1, z0, z1)
        s.set(rx, 3, (z0 + z1) // 2, "minecraft:lantern", lantaarn(True))

    def _gevel(self):
        """The face of the hill: cream plaster in a frame of timber, the round door (a real door in a disc of its own wood,
        the corners of the disc rounded with stairs), a round window with shutters on either side."""
        s, rx, fz, hout = self.stuk, self.rx, self.fz, self.hout
        vlak = {(x, y) for x in range(s.size[0]) for y in range(1, self.hoog + 2) if self.in_heuvel(x, y, fz)}
        for (x, y) in vlak:
            rand = any((x + dx, y + dy) not in vlak for dx, dy in ((1, 0), (-1, 0), (0, 1))) and y >= 1
            if (x, y + 1) not in vlak:
                s.set(x, y, fz, GRAS)           # (the sod hangs over the face)
            elif rand:
                s.set(x, y, fz, "minecraft:stripped_spruce_log", {"axis": "z"})
            else:
                s.set(x, y, fz, "minecraft:smooth_sandstone" if self.rng.random() < 0.8 else "minecraft:sandstone")
        dx = self.dx
        planken = f"minecraft:{hout}_planks"
        for x in (dx - 1, dx, dx + 1):
            for y in (1, 2, 3):
                s.set(x, y, fz, planken)
        tr = f"minecraft:{hout}_stairs"
        s.set(dx - 1, 3, fz, tr, trap("east"))
        s.set(dx + 1, 3, fz, tr, trap("west"))
        s.set(dx - 1, 1, fz, tr, trap("east", half="top"))
        s.set(dx + 1, 1, fz, tr, trap("west", half="top"))
        for y, half in ((1, "lower"), (2, "upper")):
            s.set(dx, y, fz, f"minecraft:{hout}_door", {"facing": "north", "half": half, "hinge": "left", "open": "false", "powered": "false"})
        # a frame of stone around the disc
        for x in (dx - 2, dx + 2):
            for y in (1, 2, 3):
                if (x, y) in vlak:
                    s.set(x, y, fz, "minecraft:stone_bricks" if self.rng.random() < 0.7 else "minecraft:mossy_stone_bricks")
        for x in (dx - 1, dx, dx + 1):
            if (x, 4) in vlak and (x, 5) in vlak:
                s.set(x, 4, fz, "minecraft:stone_bricks")
        # the round windows
        self.ramen = []
        for wx in (dx - 4, dx + 4):
            if (wx, 2) in vlak and (wx, 3) in vlak and (wx - 1, 2) in vlak and (wx + 1, 2) in vlak and abs(wx - dx) <= self.iw + 1:
                s.set(wx, 2, fz, "minecraft:yellow_stained_glass")
                s.set(wx - 1, 2, fz + 1, "minecraft:spruce_trapdoor", luik("south"))
                s.set(wx + 1, 2, fz + 1, "minecraft:spruce_trapdoor", luik("south"))
                self.ramen.append(wx)
        s.set(dx + 2, 3, fz + 1, "minecraft:lantern", lantaarn(True)) if (dx + 2, 4) in vlak else None
        if (dx + 2, 4) in vlak:
            s.set(dx + 2, 4, fz + 1, "minecraft:spruce_fence", hek(north="true"))

    def _tuintje(self):
        """In front of the door: a doorstep, a strip of flowers under the windows, a path out."""
        s, dx, fz = self.stuk, self.dx, self.fz
        for z in range(fz + 1, fz + 5):
            for x in (dx - 1, dx, dx + 1):
                if z == fz + 1 or x == dx or self.rng.random() < 0.5:
                    s.set(x, 0, z, PAD)
        for wx in self.ramen:
            for x in (wx - 1, wx, wx + 1):
                if s.vrij(x, 1, fz + 1):
                    s.set(x, 1, fz + 1, self.rng.choice(BLOEMEN))

    def _boom(self, x, z):
        """The oak on top of the hill."""
        s = self.stuk
        t = self.top(x, z)
        for y in range(t, t + 6):
            s.set(x, y + 1, z, "minecraft:oak_log", {"axis": "y"})
        cy = t + 7
        for ax in range(x - 4, x + 5):
            for az in range(z - 4, z + 5):
                for ay in range(cy - 2, cy + 4):
                    d = math.dist((ax, ay * 1.25, az), (x, cy * 1.25, z))
                    if d <= 3.9 + self.rng.uniform(-0.6, 0.3) and s.vrij(ax, ay, az):
                        s.set(ax, ay, az, "minecraft:oak_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})

    def schoorsteen(self, x, z):
        s = self.stuk
        t = self.top(x, z)
        if t:
            s.set(x, t + 1, z, "minecraft:bricks")
            s.set(x, t + 2, z, SCHOORSTEEN)


def _inrichting(hol, soort, borden):
    """The furniture of a hole. The room is (x0..x1, z0..z1), the door in the middle of the south wall."""
    s, h = hol.stuk, hol.h
    x0, x1, z0, z1 = hol.kamer
    dx = hol.dx
    def zet(x, y, z, name, props=None):
        if (x, z) != (dx, z1) and (x, z) != (dx, z1 - 1):       # (never in the doorway)
            s.set(x, y, z, name, props)
    if soort == "eind":
        # Knabbel-eind: a hall with a hearth, a table set for second breakfast, a bed, a pantry corner, maps on the wall
        zet(x0, 1, z0, "minecraft:pink_bed", {"facing": "west", "part": "head", "occupied": "false"})
        zet(x0 + 1, 1, z0, "minecraft:pink_bed", {"facing": "west", "part": "foot", "occupied": "false"})
        zet(x0, 1, z0 + 1, "guhs:guh_kast", {"facing": "east", "open": "false"})
        zet(dx, 1, z0, "minecraft:bricks")
        zet(dx - 1, 1, z0, "minecraft:bricks")
        zet(dx + 1, 1, z0, "minecraft:bricks")
        zet(dx, 2, z0, "guhs:gloeikool")
        zet(dx - 1, 2, z0, "minecraft:brick_stairs", trap("east"))
        zet(dx + 1, 2, z0, "minecraft:brick_stairs", trap("west"))
        zet(dx, 3, z0, "minecraft:bricks")
        zet(dx, 1, z0 + 2, "guhs:theetafel", {"gedekt": "true"})
        zet(dx - 1, 1, z0 + 2, "guhs:guh_stoel", {"facing": "east"})
        zet(dx + 1, 1, z0 + 2, "guhs:guh_stoel", {"facing": "west"})
        zet(x1, 1, z0, "minecraft:barrel", {"facing": "up", "open": "false"})
        zet(x1, 2, z0, "guhs:block_of_kaasknabbels")
        zet(x1, 1, z0 + 1, "minecraft:barrel", {"facing": "west", "open": "false"})
        zet(x1 - 1, 1, z0, "minecraft:bookshelf")
        zet(x1 - 1, 2, z0, "minecraft:bookshelf")
        zet(x1, 1, z1, "guhs:green_zitzak", {"facing": "west"})
        zet(x0, 1, z1, "guhs:guh_bloempot")
        zet(x0 + 1, 1, z1 - 2, "minecraft:green_carpet")
        zet(dx, 1, z1 - 2, "minecraft:green_carpet")
        zet(dx - 1, 1, z1 - 2, "minecraft:lime_carpet")
        bord(h, s, x1, 2, z0 + 3, borden["eind_binnen"], facing="west")
    elif soort == "sam":
        zet(x0, 1, z0, "minecraft:lime_bed", {"facing": "west", "part": "head", "occupied": "false"})
        zet(x0 + 1, 1, z0, "minecraft:lime_bed", {"facing": "west", "part": "foot", "occupied": "false"})
        zet(x1, 1, z0, "minecraft:composter", {"level": "3"})
        zet(x1, 1, z0 + 1, "minecraft:barrel", {"facing": "up", "open": "false"})
        zet(x1, 2, z0 + 1, "minecraft:flower_pot")
        zet(x1 - 1, 1, z0, "guhs:frying_pan") if False else None
        zet(dx, 1, z0, "guhs:guh_tafel", {"facing": "south"})
        zet(dx, 1, z0 + 1, "guhs:guh_stoel", {"facing": "north"})
        zet(x0, 1, z1, "guhs:guh_moestuinbak")
        zet(x1, 1, z1, "guhs:brown_kussen", {"facing": "north"})
        bord(h, s, x1, 2, z0 + 2, borden["sam_binnen"], facing="west")
    elif soort == "bakker":
        zet(x0, 1, z0, "minecraft:smoker", {"facing": "south", "lit": "false"})
        zet(x0 + 1, 1, z0, "guhs:guh_tafel", {"facing": "south"})
        zet(x0 + 2, 1, z0, "guhs:theepotje", {"facing": "south"}) if x0 + 2 < x1 else None
        zet(x1, 1, z0, "minecraft:yellow_bed", {"facing": "north", "part": "head", "occupied": "false"})
        zet(x1, 1, z0 + 1, "minecraft:yellow_bed", {"facing": "north", "part": "foot", "occupied": "false"})
        zet(x0, 1, z1, "minecraft:hay_block", {"axis": "y"})
        zet(x1, 1, z1, "guhs:yellow_zitzak", {"facing": "west"})
        zet(x0, 1, z0 + 1, "guhs:block_of_kaasknabbels")
    elif soort == "lezer":
        for y in (1, 2):
            zet(x0, y, z0, "minecraft:bookshelf")
            zet(x0 + 1, y, z0, "minecraft:bookshelf")
        zet(x1, 1, z0, "minecraft:red_bed", {"facing": "north", "part": "head", "occupied": "false"})
        zet(x1, 1, z0 + 1, "minecraft:red_bed", {"facing": "north", "part": "foot", "occupied": "false"})
        zet(x0, 1, z1, "guhs:red_zitzak", {"facing": "east"})
        zet(x0, 1, z1 - 1, "minecraft:lectern", {"facing": "east", "has_book": "false", "powered": "false"})
        zet(x1, 1, z1, "guhs:guh_bloempot")
        zet(dx, 1, z0, "guhs:guh_kast", {"facing": "south", "open": "false"})
    else:
        # the gardener of the pond: fishing rods are not blocks, so: a tub, a table, a bed
        zet(x0, 1, z0, "minecraft:cyan_bed", {"facing": "north", "part": "head", "occupied": "false"})
        zet(x0, 1, z0 + 1, "minecraft:cyan_bed", {"facing": "north", "part": "foot", "occupied": "false"})
        zet(x1, 1, z0, "guhs:guh_wastobbe", {"facing": "south", "vulling": "schuim"})
        zet(dx, 1, z0, "guhs:guh_tafel", {"facing": "south"})
        zet(x1, 1, z1, "minecraft:barrel", {"facing": "up", "open": "false"})
        zet(x0, 1, z1, "guhs:cyan_kussen", {"facing": "east"})


# =====================================================================================================================
# the Knabbelgouw
# =====================================================================================================================
class Gouw:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure(SIZE)
        self.rng = random.Random(21301600)
        self.problems = []
        self.deuren = []        # (x, z) of the doorstep of every hole (for the walk check)
        self.vast = set()       # columns that are part of something (no loose flowers there)

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name if ":" in name else "minecraft:" + name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def vrij(self, x, y, z):
        return self.get(x, y, z) in (None, AIR)

    def binnen(self, x, z, marge=0.0):
        nx, nz = (x - C) / 40.5, (z - C) / 40.5
        return (abs(nx) ** 3.4 + abs(nz) ** 3.4) ** (1 / 3.4) <= 0.985 - marge

    # --- the land ----------------------------------------------------------------------------------------------------------
    def grond(self):
        W, H, D = SIZE
        for x in range(W):
            for z in range(D):
                nx, nz = (x - C) / 40.5, (z - C) / 40.5
                d = (abs(nx) ** 3.4 + abs(nz) ** 3.4) ** (1 / 3.4)
                if d > 0.985 + self.rng.uniform(-0.04, 0.012):
                    continue
                for y in range(G):
                    self.set(x, y, z, AARDE)
                self.set(x, G, z, GRAS)
                for y in range(G + 1, H):
                    self.set(x, y, z, AIR)

    def put(self):
        """The big barbecueput, block for block the template barbecueput_groot (its own ground, grill, frame, statues, the
        Grillguh's corner), its centre jigsaw turned back into the floor block."""
        from features import barbecuether_put as put
        p = put.groot(self.h)
        ox, oz = PUT
        for (x, y, z), (name, props, nbt) in p.s.blocks.items():
            if name == "minecraft:jigsaw":
                name, props, nbt = put.STENEN, {}, None
            self.set(ox + x, y, oz + z, name, props, nbt)
            if name != AIR:
                self.vast.add((ox + x, oz + z))
        self.s.entity(ox + put.NPC_BIG[0], put.NPC_BIG[1], oz + put.NPC_BIG[2],
                      {"id": "guhs:guh_npc", "Kind": "grillguh", "PersistenceRequired": self.h.Byte(1), "Rotation": self.h.floats(90.0, 0.0)})
        self.put_frame = p.frame
        self.put_stuk = p

    def pad(self, punten, breed=1):
        """A trodden path along these points (a brush of `breed` around the line), never over something that stands there."""
        for a, b in zip(punten, punten[1:]):
            n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1])) * 2) + 1
            for i in range(n + 1):
                t = i / n
                px, pz = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
                for dx in range(-breed, breed + 1):
                    for dz in range(-breed, breed + 1):
                        x, z = int(round(px + dx)), int(round(pz + dz))
                        if math.hypot(dx, dz) > breed + 0.3 or self.get(x, G, z) != GRAS or not self.vrij(x, G + 1, z):
                            continue
                        rand = math.hypot(dx, dz) > breed - 0.5 and breed > 0
                        self.set(x, G, z, PAD if not rand or self.rng.random() < 0.7 else "minecraft:coarse_dirt")
                        self.vast.add((x, z))

    def hol(self, hol, cx, cz, kwart, soort, borden):
        """Puts a hole down with the middle of its mound on (cx, cz), turned `kwart` quarters clockwise (0: door south)."""
        _inrichting(hol, soort, borden)
        st = hol.stuk
        mx, mz = st.plek(hol.rx, hol.rz, kwart)
        ox, oz = cx - mx, cz - mz
        st.plak(self.s, ox, G, oz, kwart)
        for (x, y, z) in st.blocks:
            xx, zz = st.plek(x, z, kwart)
            self.vast.add((ox + xx, oz + zz))
        dx, dz = st.plek(hol.dx, hol.fz + 3, kwart)
        self.deuren.append((ox + dx, oz + dz))
        ix, iz = st.plek(hol.dx, hol.fz - 2, kwart)
        self.problems += [f"{soort}: {p}" for p in hol.problems]
        return (ox + dx, oz + dz), (ox + ix, oz + iz)

    def boom(self, x, z, hoog=5, straal=2.6, stam="minecraft:oak_log", blad="minecraft:oak_leaves"):
        for y in range(G + 1, G + 1 + hoog):
            self.set(x, y, z, stam, {"axis": "y"})
        cy = G + hoog + 1
        for ax in range(x - 4, x + 5):
            for az in range(z - 4, z + 5):
                for ay in range(cy - 2, cy + 3):
                    if math.dist((ax, ay * 1.2, az), (x, cy * 1.2, z)) <= straal + self.rng.uniform(-0.5, 0.3) and self.vrij(ax, ay, az):
                        self.set(ax, ay, az, blad, {"persistent": "true", "distance": "1", "waterlogged": "false"})
        self.vast.add((x, z))

    def heg(self, punten):
        for a, b in zip(punten, punten[1:]):
            n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
            for i in range(n + 1):
                x, z = int(round(a[0] + (b[0] - a[0]) * i / n)), int(round(a[1] + (b[1] - a[1]) * i / n))
                if self.get(x, G, z) == GRAS and self.vrij(x, G + 1, z):
                    self.set(x, G + 1, z, "minecraft:oak_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
                    self.vast.add((x, z))

    def lantaarnpaal(self, x, z, hoog=3):
        for y in range(G + 1, G + 1 + hoog):
            self.set(x, y, z, "minecraft:spruce_fence", hek())
        self.set(x, G + 1 + hoog, z, "minecraft:lantern", lantaarn())
        self.vast.add((x, z))

    # --- the feestwei ------------------------------------------------------------------------------------------------------
    def feestboom(self, x, z):
        """The party tree: a thick oak (2 x 2), a wide crown, lanterns and lampions hanging from it."""
        for dx in (0, 1):
            for dz in (0, 1):
                for y in range(G + 1, G + 9):
                    self.set(x + dx, y, z + dz, "minecraft:oak_log", {"axis": "y"})
                self.vast.add((x + dx, z + dz))
        # roots and big branches
        for (dx, dz) in ((-1, 0), (2, 1), (0, -1), (1, 2)):
            self.set(x + dx, G + 1, z + dz, "minecraft:oak_log", {"axis": "y"})
            self.vast.add((x + dx, z + dz))
        for (dx, dz, ax) in ((-1, 0, "x"), (-2, 0, "x"), (-3, 0, "x"), (2, 1, "x"), (3, 1, "x"), (4, 1, "x"), (0, -1, "z"), (0, -2, "z"), (0, -3, "z"),
                             (1, 2, "z"), (1, 3, "z"), (1, 4, "z")):
            self.set(x + dx, G + 8, z + dz, "minecraft:oak_log", {"axis": ax})
        cy = G + 10
        blad = set()
        for ax in range(x - 8, x + 10):
            for az in range(z - 8, z + 10):
                for ay in range(cy - 3, cy + 5):
                    d = math.dist(((ax - 0.5), ay * 1.55, (az - 0.5)), (x, cy * 1.55, z))
                    if d <= 7.2 + self.rng.uniform(-0.9, 0.4) and self.vrij(ax, ay, az):
                        self.set(ax, ay, az, "minecraft:oak_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
                        blad.add((ax, ay, az))
        # lights: under the lowest leaves, a chain and a lantern or a lampion
        hangers = [(-4, -3), (5, -2), (-3, 5), (6, 4), (-6, 1), (2, -6), (3, 7), (-1, -5)]
        for i, (dx, dz) in enumerate(hangers):
            ax, az = x + dx, z + dz
            onder = min((ay for (bx, ay, bz) in blad if (bx, bz) == (ax, az)), default=None)
            if onder is None or onder - 2 <= G + 3:
                continue
            self.set(ax, onder - 1, az, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
            if i % 2:
                self.set(ax, onder - 2, az, "minecraft:lantern", lantaarn(True))
            else:
                self.set(ax, onder - 2, az, ("guhs:lampion_geel", "guhs:lampion_roze", "guhs:lampion_mint")[i // 2 % 3], {"hanging": "true"})

    def feesttafels(self, x0, z0, lang, langs_x=True):
        """A long table with benches on both sides."""
        for i in range(lang):
            x, z = (x0 + i, z0) if langs_x else (x0, z0 + i)
            self.set(x, G + 1, z, "guhs:feestbuffettafel", {"facing": "south" if langs_x else "east", "gedekt": "true"})
            self.vast.add((x, z))
            for kant, facing in ((-1, "south" if langs_x else "east"), (1, "north" if langs_x else "west")):
                bx, bz = (x, z + kant) if langs_x else (x + kant, z)
                if i % 3 != 1:
                    self.set(bx, G + 1, bz, "guhs:guh_bank", {"facing": facing})
                    self.vast.add((bx, bz))

    def dansvloer(self, x0, z0, n=5):
        for x in range(x0, x0 + n):
            for z in range(z0, z0 + n):
                self.set(x, G, z, "minecraft:spruce_planks" if (x + z) % 2 else "minecraft:birch_planks")
                self.vast.add((x, z))
        for (x, z) in ((x0 - 1, z0 - 1), (x0 + n, z0 - 1), (x0 - 1, z0 + n), (x0 + n, z0 + n)):
            for y in range(G + 1, G + 4):
                self.set(x, y, z, "minecraft:spruce_fence", hek())
            self.set(x, G + 4, z, "guhs:lampion_mint", {"hanging": "false"})
            self.vast.add((x, z))
        for x in range(x0, x0 + n):
            self.set(x, G + 3, z0 - 1, "guhs:vlaggetjes", {"axis": "x"})
            self.set(x, G + 3, z0 + n, "guhs:feestslingers", {"axis": "x"})

    # --- Sam-guh's garden, the pond ----------------------------------------------------------------------------------------
    def moestuin(self, x0, z0, x1, z1):
        """Rows of potatoes, carrots and beetroot between a fence, a water trough in the middle, a scarecrow."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                rand = x in (x0, x1) or z in (z0, z1)
                self.vast.add((x, z))
                if rand:
                    if (x, z) in ((x1, (z0 + z1) // 2),):
                        self.set(x, G + 1, z, "minecraft:spruce_fence_gate", {"facing": "east", "open": "false", "powered": "false", "in_wall": "false"})
                        continue
                    self.set(x, G + 1, z, "minecraft:spruce_fence", hek(north="true" if x in (x0, x1) and z > z0 else "false",
                                                                         south="true" if x in (x0, x1) and z < z1 else "false",
                                                                         west="true" if z in (z0, z1) and x > x0 else "false",
                                                                         east="true" if z in (z0, z1) and x < x1 else "false"))
                    continue
                if x == (x0 + x1) // 2:
                    if z % 4 == 1:
                        self.set(x, G, z, "minecraft:water")
                    else:
                        self.set(x, G, z, PAD)
                    continue
                self.set(x, G, z, "minecraft:farmland", {"moisture": "7"})
                gewas = ("minecraft:potatoes", "minecraft:carrots", "minecraft:beetroots")[(x - x0) // 2 % 3]
                self.set(x, G + 1, z, gewas, {"age": "3" if gewas.endswith("beetroots") else str(self.rng.choice([5, 7, 7, 7]))})
        # the scarecrow
        sx, sz = x0 + 2, z0 - 2
        self.set(sx, G + 1, sz, "minecraft:spruce_fence", hek())
        self.set(sx, G + 2, sz, "minecraft:hay_block", {"axis": "y"})
        self.set(sx, G + 3, sz, "minecraft:carved_pumpkin", {"facing": "east"})
        self.vast.add((sx, sz))

    def vijver(self, cx, cz, rx, rz):
        for x in range(cx - rx - 1, cx + rx + 2):
            for z in range(cz - rz - 1, cz + rz + 2):
                d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
                if d <= 1.0 + self.rng.uniform(-0.12, 0.08) and self.get(x, G, z) == GRAS:
                    self.set(x, G, z, "minecraft:water")
                    self.set(x, G - 1, z, "minecraft:clay" if self.rng.random() < 0.5 else "minecraft:sand")
                    self.vast.add((x, z))
                    if self.rng.random() < 0.1:
                        self.set(x, G + 1, z, "minecraft:lily_pad")
        # a little bridge over the middle (along z)
        for z in range(cz - rz - 1, cz + rz + 2):
            self.set(cx, G + 1, z, "minecraft:spruce_slab", plaat("bottom"))
            for x in (cx - 1, cx + 1):
                if abs(z - cz) <= rz - 1:
                    self.set(x, G + 1, z, "minecraft:spruce_fence", hek(north="true", south="true"))
            self.vast.add((cx, z))
        # reeds on the bank
        for _ in range(26):
            x, z = cx + self.rng.randint(-rx - 2, rx + 2), cz + self.rng.randint(-rz - 2, rz + 2)
            if self.get(x, G, z) == GRAS and self.vrij(x, G + 1, z) and any(self.get(x + a, G, z + b) == "minecraft:water" for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                self.set(x, G + 1, z, "minecraft:sugar_cane", {"age": "0"})
                if self.rng.random() < 0.5:
                    self.set(x, G + 2, z, "minecraft:sugar_cane", {"age": "0"})
                self.vast.add((x, z))

    def begroeiing(self):
        """Loose flowers and tufts of grass on what is still bare lawn."""
        W, H, D = SIZE
        for x in range(W):
            for z in range(D):
                if (x, z) in self.vast or self.get(x, G, z) != GRAS or not self.vrij(x, G + 1, z):
                    continue
                r = self.rng.random()
                if r < 0.10:
                    self.set(x, G + 1, z, "minecraft:short_grass")
                elif r < 0.135:
                    self.set(x, G + 1, z, self.rng.choice(BLOEMEN))

    def midden(self):
        self.s.set(C, 0, C, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": MIDDEN, "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": AARDE, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def _borden():
    from features import ring_h1_tekst as tekst
    return tekst.BORDEN


def bouw_gouw(h, entiteiten=None):
    """The template of the Knabbelgouw (not saved) and the problems of its self-check. `entiteiten`: {"guhdalf": nbt, "sam":
    nbt, "bewoners": [nbt, ...]} (ring_h1.py makes them; None for a picture)."""
    borden = _borden()
    e = entiteiten or {}
    g = Gouw(h)
    g.grond()
    g.put()
    # the holes: Knabbel-eind in the north (the big one, with the oak on top), two beside it, Sam-guh's in the west, one east
    eind = Hol(h, 1, 12, 9, 9, 4, 6, "warped", snede=3, boom=True)
    eind.schoorsteen(eind.rx + 4, eind.rz - 2)
    d_eind, i_eind = g.hol(eind, 40, 10, 0, "eind", borden)
    west = Hol(h, 2, 9, 7, 7, 3, 5, "bamboo", snede=3)
    west.schoorsteen(west.rx - 3, west.rz - 2)
    d_west, i_west = g.hol(west, 15, 12, 0, "bakker", borden)
    oost = Hol(h, 3, 9, 7, 7, 3, 5, "mangrove", snede=3)
    oost.schoorsteen(oost.rx + 3, oost.rz - 2)
    d_oost, i_oost = g.hol(oost, 65, 12, 0, "lezer", borden)
    sam = Hol(h, 4, 9, 7, 7, 3, 5, "birch", snede=3)
    sam.schoorsteen(sam.rx - 3, sam.rz - 1)
    d_sam, i_sam = g.hol(sam, 9, 32, 3, "sam", borden)       # door to the east
    visser = Hol(h, 5, 9, 7, 7, 3, 5, "cherry", snede=3)
    visser.schoorsteen(visser.rx + 3, visser.rz - 1)
    d_vis, i_vis = g.hol(visser, 71, 32, 1, "visser", borden)  # door to the west
    g.binnen_plekken = {"eind": i_eind, "bakker": i_west, "lezer": i_oost, "sam": i_sam, "visser": i_vis}
    # the feestwei: the tree, Guhdalf's camp east of it, the long tables and the dance floor west of it
    g.feestboom(39, 24)
    kx, kz = GOUW_KAMP
    stuk = kamp(h, guhdalf=e.get("guhdalf"))
    stuk.plak(g.s, kx, G, kz, GOUW_KAMP_KWART)
    for (x, y, z) in stuk.blocks:
        xx, zz = stuk.plek(x, z, GOUW_KAMP_KWART)
        g.vast.add((kx + xx, kz + zz))
    g.kamp_stuk = stuk
    g.feesttafels(24, 20, 7)
    g.feesttafels(24, 30, 7)
    g.dansvloer(26, 23)
    for (x, z) in ((22, 18), (32, 18), (22, 32), (32, 32)):
        g.lantaarnpaal(x, z)
    for (x, z) in ((21, 25), (21, 26)):
        g.set(x, G + 1, z, "minecraft:barrel", {"facing": "up", "open": "false"})
        g.vast.add((x, z))
    g.set(21, G + 2, 25, "minecraft:barrel", {"facing": "up", "open": "false"})
    # Sam-guh's moestuin south of his hole, the pond in the east
    g.moestuin(4, 44, 16, 56)
    g.vijver(68, 52, 7, 5)
    # paths: from the north way into the pit up over the feestwei to Knabbel-eind, and to every door
    g.pad([(40, 46), (40, 37), (41, 30), (43, 22), (40, 15), d_eind], 1)
    g.pad([(40, 37), (30, 36), (22, 34), d_sam], 1)
    g.pad([(41, 36), (52, 34), (60, 33), d_vis], 1)
    g.pad([(22, 34), (17, 26), d_west], 1)
    g.pad([(60, 33), (64, 26), d_oost], 1)
    g.pad([d_sam, (18, 40), (18, 50)], 0)
    g.pad([d_vis, (66, 40), (68, 45)], 0)
    g.pad([(43, 22), (48, 26), (kx + 7, kz + 9)], 0)
    # the way in from outside: past the pit on both sides, to its south entrance
    g.pad([(22, 34), (19, 44), (20, 62), (27, 76), (40, 79)], 0)
    g.pad([(60, 33), (61, 44), (60, 62), (53, 76), (40, 79)], 0)
    # hedges, trees, lamp posts, the signs
    g.heg([(28, 14), (24, 16)])
    g.heg([(52, 14), (56, 16)])
    g.heg([(18, 58), (18, 66)])
    for (x, z, hoog) in ((6, 22, 5), (74, 22, 5), (5, 62, 6), (75, 64, 5), (12, 72, 5), (68, 72, 6), (76, 44, 4)):
        if g.binnen(x, z, 0.04):
            g.boom(x, z, hoog, stam="minecraft:birch_log" if (x + z) % 3 == 0 else "minecraft:oak_log",
                   blad="minecraft:birch_leaves" if (x + z) % 3 == 0 else "minecraft:oak_leaves")
    for (x, z) in ((38, 39), (43, 39), (34, 15), (46, 15), (20, 37), (61, 37)):
        if g.get(x, G, z) in (GRAS, PAD, "minecraft:coarse_dirt") and g.vrij(x, G + 1, z):
            if g.get(x, G, z) != GRAS:
                continue
            g.lantaarnpaal(x, z)
    bord(h, g.s, 38, G + 1, 41, borden["welkom"], rotatie=0)
    g.vast.add((38, 41))
    bord(h, g.s, d_eind[0] + 2, G + 1, d_eind[1] - 1, borden["eind"], rotatie=0)
    g.vast.add((d_eind[0] + 2, d_eind[1] - 1))
    bord(h, g.s, d_sam[0] - 1, G + 1, d_sam[1] + 2, borden["sam"], rotatie=12)
    g.vast.add((d_sam[0] - 1, d_sam[1] + 2))
    bord(h, g.s, 17, G + 1, 50, borden["moestuin"], rotatie=12)
    g.vast.add((17, 50))
    g.begroeiing()
    g.midden()
    # who lives here
    if "sam" in e:
        g.s.entity(GOUW_SAM[0] + 0.5, GOUW_SAM[1], GOUW_SAM[2] + 0.5, e["sam"])
    for nbt, (x, y, z) in zip(e.get("bewoners", []), bewoner_plekken(g)):
        g.s.entity(x + 0.5, y, z + 0.5, nbt)
    g.problems += check_gouw(g)
    return g, g.problems


def bewoner_plekken(g):
    """Where the residents stand: in the three holes that are not Sam's or Knabbel-eind, and one at the dance floor."""
    b = g.binnen_plekken
    return [(b["bakker"][0], G + 1, b["bakker"][1]), (b["lezer"][0], G + 1, b["lezer"][1]), (b["visser"][0], G + 1, b["visser"][1]), (28, G + 1, 25)]


def check_gouw(g):
    """The geometry self-check: the pit is whole (its frame as the big pit's), every door can be walked to from the south
    entrance of the pit and from the edge, the camp is complete, Guhdalf and Sam-guh have room, nothing of the camp floats."""
    s = g.s
    blocks = s.blocks
    problems = []

    def nm(c):
        b = blocks.get(c)
        return b[0] if b else None

    def leeg(c):
        n = nm(c)
        return n in (None, AIR) or n.endswith(("_carpet", "short_grass", "fern", "_sign", "lily_pad", "potatoes", "carrots", "beetroots")) or n in BLOEMEN

    # the pit: the same frame spots as barbecueput_groot
    ox, oz = PUT
    from features import barbecuether_put as put
    have = [c for c in g.put_frame if nm((ox + c[0], c[1], oz + c[2])) == put.GRILLKOOL]
    if not 4 <= len(have) <= len(g.put_frame) - 2:
        problems.append(f"the pit's frame has {len(have)} of {len(g.put_frame)} grillkool")
    # walking: a flood fill over ground cells with two free blocks above them (doors are passable), one step up or down
    def loopbaar(x, z, y):
        onder = nm((x, y - 1, z))
        if onder in (None, AIR, "minecraft:water") or (leeg((x, y - 1, z)) and not onder.endswith("_carpet")):
            return False
        for yy in (y, y + 1):
            n = nm((x, yy, z))
            if not (leeg((x, yy, z)) or (n and n.endswith("_door")) or (n and n.endswith("fence_gate"))):
                return False
        return True

    start = (40, G + 1, 79)
    while not loopbaar(start[0], start[2], start[1]) and start[2] > 70:
        start = (start[0], start[1], start[2] - 1)
    gezien = {start}
    stapel = [start]
    while stapel:
        x, y, z = stapel.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                n = (x + dx, y + dy, z + dz)
                if n not in gezien and 0 <= n[0] < SIZE[0] and 0 <= n[2] < SIZE[2] and loopbaar(n[0], n[2], n[1]):
                    gezien.add(n)
                    stapel.append(n)
    for naam, (x, z) in g.binnen_plekken.items():
        if (x, G + 1, z) not in gezien:
            problems.append(f"the room of '{naam}' at {(x, z)} can't be walked to from the south edge")
    kx, kz = GOUW_KAMP
    for naam, (x, y, z) in (("guhdalf", KAMP_GUHDALF), ("the party table's front", (KAMP_TAFEL[0], 1, KAMP_TAFEL[2] + 1)),
                            ("the cart", (KAMP_KIST[0], 1, KAMP_KIST[2] + 2)), ("the provisions", (11, 1, 7))):
        xx, zz = g.kamp_stuk.plek(x, z, GOUW_KAMP_KWART)
        if (kx + xx, G + y, kz + zz) not in gezien:
            problems.append(f"{naam} at the camp can't be walked to")
    if (GOUW_SAM[0], GOUW_SAM[1], GOUW_SAM[2]) not in gezien:
        problems.append("Sam-guh's spot in the garden can't be walked to")
    if (40, G + 1, 60 - 8) not in gezien:
        problems.append("the pit can't be walked into")
    # the quest blocks are all there, once
    for blok, n in ((VUURWERKKIST, 1), (FEESTTAFEL, 1), (VUUR, 1)):
        if sum(1 for b in blocks.values() if b[0] == blok) != n:
            problems.append(f"{blok} should be there {n}x")
    if sorted(b[1].get("soort") for b in blocks.values() if b[0] == PROVIAND) != ["0", "1", "2"]:
        problems.append("the three provisions are not all there")
    if not any(b[0] == "minecraft:jigsaw" and b[2] and b[2].get("name") == MIDDEN for b in blocks.values()) or nm((C, 0, C)) != "minecraft:jigsaw":
        problems.append("no centre jigsaw in layer 0")
    return problems


def bouw_kamp(h):
    """The prop template of the camp (for an old pit) and the problems of its self-check."""
    k = kamp(h)
    s = h.Structure(KAMP)
    k.plak(s, 0, 0, 0, 0)
    problems = []
    blocks = s.blocks
    for (x, y, z), b in blocks.items():
        if y == 0 or b[0] == AIR:
            continue
        buren = [(x, y - 1, z), (x, y + 1, z), (x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1)]
        if not any(blocks.get(c, (AIR,))[0] != AIR for c in buren):
            problems.append(f"{b[0]} floats at {(x, y, z)}")
    for naam, c in (("crate", KAMP_KIST), ("table", KAMP_TAFEL), ("fire", KAMP_VUUR)):
        if c not in blocks:
            problems.append(f"no {naam}")
    for (x, y, z) in (KAMP_GUHDALF, KAMP_SAM):
        if blocks.get((x, y, z)) or blocks.get((x, y + 1, z)) or (x, 0, z) not in blocks:
            problems.append(f"no room to stand at {(x, y, z)}")
    # the open side for the scenes: x 4..11, z 8..10 is bare ground
    for x in range(4, 12):
        for z in range(8, 11):
            if blocks.get((x, 1, z)):
                problems.append(f"the open side of the camp is not free at {(x, z)}")
    return s, problems


def test_kamer(h):
    """RingH1GameTests: dirt with a lawn on it (25 x 12 x 25): room for the camp, a walk and a grill portal."""
    s = h.Structure((25, 12, 25))
    for x in range(25):
        for z in range(25):
            s.set(x, 0, z, AARDE)
            s.set(x, 1, z, GRAS)
    return s


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    """What the builders need of the make_v2 namespace, for the preview."""
    def __init__(self):
        import make_structures as ms
        self.ms, self.Structure, self.Byte, self.floats = ms, ms.Structure, ms.Byte, ms.floats


def gedraaid(s, kwart):
    import make_structures as ms
    W, H, D = s.size
    for _ in range(kwart % 4):
        t = ms.Structure((D, H, W))
        for (x, y, z), b in s.blocks.items():
            t.blocks[(D - 1 - z, y, x)] = b
        s, (W, H, D) = t, t.size
    return s


def uitsnede(s, x0, z0, x1, z1, y0=0, y1=None):
    import make_structures as ms
    y1 = s.size[1] - 1 if y1 is None else y1
    t = ms.Structure((x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1))
    for (x, y, z), b in s.blocks.items():
        if x0 <= x <= x1 and z0 <= z <= z1 and y0 <= y <= y1:
            t.blocks[(x - x0, y - y0, z - z0)] = b
    return t


def preview(out):
    import wiki_renders as wr
    kleuren = {VUUR: (255, 150, 50), "minecraft:lantern": (255, 214, 120), "minecraft:chain": (70, 74, 90), VUURWERKKIST: (190, 60, 60),
               FEESTTAFEL: (240, 200, 210), PROVIAND: (200, 160, 90), SCHOORSTEEN: (150, 80, 60), "minecraft:short_grass": (96, 150, 60),
               "minecraft:fern": (80, 130, 60), "minecraft:water": (70, 120, 220), "minecraft:dirt_path": (150, 122, 70),
               "minecraft:grass_block": (106, 160, 64), "minecraft:birch_leaves": (110, 150, 80), "minecraft:sugar_cane": (130, 180, 90),
               "minecraft:farmland": (96, 62, 36), "minecraft:potatoes": (70, 140, 50), "minecraft:carrots": (60, 150, 60), "minecraft:beetroots": (90, 130, 60),
               "guhs:vlaggetjes": (230, 90, 110), "guhs:feestslingers": (250, 210, 80), "minecraft:lily_pad": (40, 120, 50),
               "minecraft:spruce_sign": (126, 94, 56), "minecraft:spruce_wall_sign": (126, 94, 56), "minecraft:smooth_sandstone": (226, 216, 172),
               "minecraft:brick_stairs": (150, 90, 74), "guhs:guh_bank": (170, 120, 80), "guhs:guh_stoel": (170, 120, 80), "guhs:guh_tafel": (180, 130, 86),
               "guhs:theetafel": (190, 140, 96), "guhs:guh_bloempot": (190, 110, 80), "guhs:guh_moestuinbak": (120, 90, 50), "guhs:guh_wastobbe": (150, 170, 190),
               "guhs:houtskoolsteen_stenen_hek": (57, 47, 47), "guhs:feestbuffettafel": (236, 226, 200)}
    for hout, rgb in (("warped", (58, 142, 140)), ("bamboo", (200, 180, 80)), ("mangrove", (130, 50, 50)), ("birch", (200, 190, 140)), ("cherry", (230, 170, 180))):
        kleuren[f"minecraft:{hout}_door"] = rgb
    for b in BLOEMEN:
        kleuren[b] = {"poppy": (220, 40, 40), "dandelion": (250, 220, 50), "oxeye_daisy": (240, 240, 240), "cornflower": (80, 110, 230),
                      "azure_bluet": (220, 230, 240), "pink_tulip": (240, 160, 200), "allium": (180, 110, 230), "knabbelroos": (250, 190, 60),
                      "kaasbloem": (250, 220, 90)}[b.split(":")[1]]
    wr.SPECIAL_COLOURS.update(kleuren)
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    h = _H()
    s, problems = bouw_kamp(h)
    for p in problems:
        print("PROBLEM kamp:", p)
    for kwart in (0, 1, 2):
        wr.render_structure(gedraaid(s, kwart), {}, px=30, max_size=1400).save(os.path.join(out, f"kamp_{kwart}.png"))
    g, problems = bouw_gouw(h)
    for p in problems:
        print("PROBLEM gouw:", p)
    for kwart in (0, 1, 2, 3):
        wr.render_structure(gedraaid(g.s, kwart), {}, px=12, max_size=2400).save(os.path.join(out, f"gouw_{kwart}.png"))
    # close-ups: the north half (the holes and the feestwei) from the south, and a look into Knabbel-eind
    noord = uitsnede(g.s, 0, 0, 80, 41, y0=G)
    wr.render_structure(gedraaid(noord, 2), {}, px=20, max_size=2600).save(os.path.join(out, "gouw_noord.png"))
    eind = uitsnede(g.s, 27, 0, 53, 16, y0=G)
    wr.render_structure(gedraaid(eind, 2), {}, px=30, max_size=2000).save(os.path.join(out, "knabbel_eind.png"))
    wr.render_structure(gedraaid(uitsnede(g.s, 27, 0, 53, 16, y0=G, y1=G + 3), 2), {}, px=30, max_size=2000).save(os.path.join(out, "knabbel_eind_binnen.png"))
    print("gouw:", SIZE, len(g.s.blocks), "blocks")


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
