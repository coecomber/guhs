"""
bbq2 (ring-h6) - De Frituurberg (structure guhs:frituurberg, a "burcht" of wereld.bbq_structuur with placement "paleis": its
ash plain lies one block above the sauce sea of the Rookdelta) and the small test mountain of RingH6GameTests.

The mountain (96 x 92 x 96, anchor ANKER = the middle of the plain):
  - a round plain of ash with glowing cracks, four mouths at the edges of the template (you walk or bridge up to the box and
    step on the plain), the base camp on the south side (a Rustvuurtje, a lean-to, the gate of the Kronkelpad);
  - the cone: a concave volcano of houtskoolsteen, basalt and blackstone with glowing cracks that spiral up, and a golden
    brown crust of fried batter that drips down from the crater rim (it is a deep fryer, after all);
  - the climb, in the order the story walks it (feature/ringh6/Klim):
      pad 1   the Kronkelpad: a mountain road on stone retaining walls, three quarters round the cone (south -> east ->
              north -> west), from the camp (feet y 10) to the Eerste Richel (y 30);
      west    three terraces above each other on the west face: Eerste Richel (cage 1, a rest fire), a small ledge and the
              Tweede Richel (y 50; cage 2, a rest fire): no way up but the Elfentouw (hooks 1 and 2, each with a blue
              lantern, each launch spot with a rope post);
      pad 3   a narrow road on from the Tweede Richel round the south side to a small terrace on the east face (y 58), hook 3
              up to the Derde Richel (y 64; cage 3, a rest fire);
      pad 4   the last stretch round the north side to the Frituurspleet (y 69), a cleft through the crater rim on the west
              side: here Sam-guh carries you;
  - the crater: a bowl of kaasfrituursaus, a balcony over it (the Bakrandje: the finale's anchor RAND), and over the pool a
    giant frying basket of grill bars that hangs on chains from a gantry across the rim;
  - a dome of air round all of it (the Barbecuether is one big cave; a copy can land in solid rock, and a protected
    building that is buried can't be dug out), up to the smoke hole above the crater.

  berg(h)          -> Berg (the build, not saved); Berg.plekken / Berg.routes are what Java reads (data/guhs/ringh6/berg.json)
  check(b)         -> problems: the whole climb can be done (walking, the three hook hops with a free line), nothing can be
                      skipped on foot, every fire, cage, lock and sign is there, nothing hangs across a burcht seam
  test_berg(h)     -> (Structure, plekken, routes): the same named spots on a floor of 25 x 17 (RingH6GameTests)
  preview(out)     pictures (python tools/features/ring_h6_bouw.py <out>, from the worktree root)
"""
import math
import os
import sys

from features import spiesburcht_burcht as sb

NAAM = "frituurberg"
W, H, D = 96, 92, 96
CX = CZ = 47.5                 # the axis of the mountain, in cell coordinates (between the cells 47 and 48)
GROND = 9                      # the top block of the plain; you stand at y 10
ANKER = (48, 10, 48)           # the burcht anchor: world y 33 = one above the sauce sea (sea level 32)
R0, RRIM, TOP = 35.0, 12.5, 71
MACHT = 1.35                   # the cone is concave: gentle at the foot, steep under the rim
PLEIN = 47.9                   # the plain reaches the edge of the template at the four mouths
KRATER = 9.5                   # inside this radius the crater is built by hand (no bulges)
POEL_R, POEL_BODEM, POEL_TOP = 6.9, 59, 63

AIR = sb.AIR
HOUTSKOOL, STENEN, GEBARSTEN, GEBEITELD = sb.HOUTSKOOL, sb.STENEN, sb.GEBARSTEN, sb.GEBEITELD
PLAAT, MUUR, HEK, TRAP = sb.PLAAT, sb.MUUR, sb.HEK, sb.TRAP
ROOSTER, PILAAR, GEPOLIJST, TRALIES = sb.ROOSTER, sb.PILAAR, sb.GEPOLIJST, sb.TRALIES
GLOEIKOOL, AS, AS_AARDE, SAUS, ROOKGAT, SMEUL = sb.GLOEIKOOL, sb.AS, sb.AS_AARDE, sb.SAUS, sb.ROOKGAT, sb.SMEUL
SATE = sb.SATE
BASALT, GLAD_BASALT, BLACKSTONE = "minecraft:basalt", "minecraft:smooth_basalt", "minecraft:blackstone"
KORST = ("minecraft:honeycomb_block", "minecraft:orange_terracotta", "minecraft:yellow_terracotta", "minecraft:honeycomb_block")
KORST_RAND = "minecraft:brown_terracotta"
VUUR = "guhs:ring_rustvuur"
HAAK = "guhs:elfentouw_haak"
SLOT = "guhs:ringh6_kooislot"
ROOKGUH = "guhs:ringh6_rookguh"
PASSEERT = {AIR, sb.mc(SMEUL), sb.mc(HAAK), "minecraft:soul_lantern", "minecraft:lantern", "minecraft:chain", "minecraft:spruce_sign",
            "minecraft:spruce_wall_sign"}

# --- the fixed spots of the climb (template coordinates; a "feet" cell is the air cell you stand in) -----------------------
KAMP_VUUR = (48, 10, 89)
RICHEL = {1: dict(x0=19, x1=26, z0=41, z1=55, y=30), 2: dict(x0=27, x1=33, z0=42, z1=53, y=50), 3: dict(x0=59, x1=66, z0=38, z1=51, y=64)}
TUSSEN = dict(x0=23, x1=28, z0=50, z1=54, y=40)          # the small ledge between the Eerste and the Tweede Richel
OOST = dict(x0=62, x1=68, z0=45, z1=50, y=58)            # the small terrace under the Derde Richel
KOOI = {1: (22, 46, 1), 2: (30, 45, 1), 3: (62, 48, -1)}  # the middle of each cage (x, z) and the side of its lock (1 south, -1 north)
VUREN = {1: (24, 30, 51), 2: (30, 50, 50), 3: (62, 64, 43)}
# hook -> (the hook block, its facing, the launch cell under it)
HAKEN = {1: ((22, 39, 52), "west", (20, 30, 52)), 2: ((26, 49, 52), "west", (24, 40, 52)), 3: ((67, 63, 49), "east", (68, 58, 49))}
SPLEET = dict(x0=33, x1=36, z0=45, z1=50, y=69)          # the landing in front of the cleft
RAND = (41, 66, 48)                                      # the anchor of the finale: a feet cell on the balcony
BALKON = dict(x0=39, x1=43, z0=44, z1=51, y=66)
MAND = (45, 50)                                          # the frying basket: x and z from .. to
# the roads: (from angle, to angle, from feet y, to feet y); an angle is clockwise from the south seen from above... no:
# 0 = south (+z), 90 = east (+x), 180 = north, 270 = west; the roads run with rising angle
PADEN = {1: (0.0, 256.0, 10, 30), 3: (289.0, 440.0, 50, 58), 4: (118.0, 257.0, 64, 69)}


def _h(x, y, z):
    """A cheap, fixed pseudo-random number 0..1 per cell."""
    n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def pool(x, z):
    """(distance to the axis, angle in degrees) of a cell."""
    dx, dz = x - CX, z - CZ
    return math.hypot(dx, dz), math.degrees(math.atan2(dx, dz)) % 360.0


def bobbel(graden):
    """The ridges and gullies of the cone: the factor its radius is stretched by at this angle."""
    a = math.radians(graden)
    return 1.0 + 0.05 * math.sin(5 * a + 0.9) + 0.03 * math.sin(11 * a + 2.1)


def straal(y):
    """The radius of the smooth cone at height y (the inverse of its profile)."""
    t = min(1.0, max(0.0, (y - GROND) / float(TOP - GROND)))
    return RRIM + (R0 - RRIM) * (1.0 - t ** (1.0 / MACHT))


def profiel(x, z):
    """The top block of the untouched mountain in this column (None: outside the plain)."""
    d, graden = pool(x, z)
    if d > PLEIN:
        return None
    if d < POEL_R:
        return POEL_BODEM
    if d < KRATER:
        return int(round(64 + (d - POEL_R) * (TOP - 64) / (KRATER - POEL_R)))
    de = d / bobbel(graden)
    if de >= R0:
        return GROND
    if de <= RRIM:
        return TOP + (1 if _h(x, 7, z) < 0.35 else 0)
    return int(round(GROND + (TOP - GROND) * ((R0 - de) / (R0 - RRIM)) ** MACHT))


def koepel(d):
    """The top of the dome of air above a column at this distance from the axis."""
    if d < RRIM + 2:
        return 88
    if d < R0:
        return int(86 - (d - RRIM - 2) / (R0 - RRIM - 2) * 40)
    return int(46 - (d - R0) / (PLEIN - R0) * 22)


class Berg(sb.Bouw):
    def __init__(self, h):
        super().__init__(h, (W, H, D), 21302100)
        self.naam = NAAM
        self.top = {}                  # (x, z) -> the top block of the untouched mountain
        self.pad = {}                  # (x, z) -> feet y of a road / terrace cell
        self.plekken = {}
        self.routes = {}
        self.middens = {}              # road number -> its centre line [(x, feet y, z)]
        self.terrassen = []            # (the terrace, its cells)
        self.midcellen = set()

    # --- the mountain itself -------------------------------------------------------------------------------------------------
    def steen(self, x, y, z, top, d, graden):
        """What the mountain is made of at this cell (top = the top block of its column)."""
        diep = top - y
        de = d / bobbel(graden)
        if de >= R0:                                             # the plain
            if diep == 0:
                r = _h(x, 1, z)
                return AS_AARDE if r < 0.5 else HOUTSKOOL if r < 0.8 else AS
            return HOUTSKOOL
        if d < KRATER or de <= RRIM + 0.5:                       # the crater wall and the crest: crust all over
            return KORST[int(_h(x, y, z) * 4)] if diep <= 1 else BLACKSTONE
        # the crust drips down from the rim, further in some places than in others
        drup = 1.6 + 2.6 * (0.5 + 0.5 * math.sin(math.radians(graden) * 7 + 0.3)) + 2.2 * _h(int(graden / 6), 3, 0)
        if diep <= 1 and de < RRIM + drup:
            return KORST_RAND if de > RRIM + drup - 0.55 else KORST[int(_h(x, y, z) * 4)]
        if diep == 0:
            # glowing cracks that spiral up the cone, and a few loose embers
            if ((graden + y * 2.6) % 45.0) < 2.2 + 16.0 / max(8.0, d) or _h(x, y + 40, z) < 0.01:
                return GLOEIKOOL
            if de > R0 - 3.0 and _h(x, 2, z) < 0.5:
                return AS                                        # ash screes at the foot
        if math.sin(math.radians(graden) * 17 + 0.7 * math.sin(y * 0.21)) > 0.55:
            return BASALT
        r = _h(x, y, z)
        return BLACKSTONE if r < 0.3 else GLAD_BASALT if r < 0.4 else HOUTSKOOL

    def massief(self):
        for x in range(W):
            for z in range(D):
                t = profiel(x, z)
                if t is not None:
                    self.top[(x, z)] = t
        for (x, z), t in self.top.items():
            d, graden = pool(x, z)
            buren = [self.top.get(c) for c in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))]
            laagste = min([t] + [b for b in buren if b is not None])
            if d / bobbel(graden) >= R0:
                # the plain: a plate that gets thicker towards the mountain (a foot in the sauce sea)
                bodem = max(0, min(GROND - 3, int(GROND - 3 - (PLEIN - d) * 0.5)))
            else:
                bodem = max(0, laagste - 3)
            for y in range(bodem, t + 1):
                name = self.steen(x, y, z, t, d, graden)
                self.set(x, y, z, name, {"axis": "y"} if name == BASALT else None)
            # the dome of air
            for y in range(t + 1, min(H - 3, max(t + 10, koepel(d))) + 1):
                self.set(x, y, z, AIR)
            if d < POEL_R:
                for y in range(POEL_BODEM + 1, POEL_TOP + 1):
                    self.set(x, y, z, SAUS, {"level": "0"})

    def scheuren(self):
        """Glowing cracks over the plain, from the foot of the mountain outwards (not through the camp)."""
        for (x, z), t in self.top.items():
            d, graden = pool(x, z)
            if t != GROND or d < R0 or d > 45.5 or (z > 80 and 38 < x < 60):
                continue
            for k in range(7):
                midden = 25.0 + k * 51.4 + 5.0 * math.sin(d * 0.45 + k)
                afstand = abs((graden - midden + 180.0) % 360.0 - 180.0) * math.pi / 180.0 * d
                if afstand < 0.62 and _h(x, 5, z) < 0.85:
                    self.set(x, GROND, z, GLOEIKOOL)

    # --- roads and terraces ------------------------------------------------------------------------------------------------
    def richel(self, cellen, name=None, hoog=4):
        """A ledge: {(x, z): feet y}. Each cell gets its floor and room above it; under the part that sticks out of the mountain
        hangs a corbel of the mountain's own rock that gets one block thinner with every block further out (no blank walls)."""
        cellen = {c: y for c, y in cellen.items() if c in self.top}
        # how far out is each cell: 0 where the mountain itself reaches the floor
        uit = {c: 0 for c, y in cellen.items() if self.top[c] >= y - 2}
        rand = list(uit)
        while rand:
            volgende = []
            for (x, z) in rand:
                for c in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                    if c in cellen and c not in uit:
                        uit[c] = uit[(x, z)] + 1
                        volgende.append(c)
            rand = volgende
        ver = max(uit.values()) if uit else 0
        for (x, z), y in cellen.items():
            t = self.top[(x, z)]
            d, graden = pool(x, z)
            dik = max(0, ver - uit.get((x, z), ver))
            for yy in range(max(t + 1, y - 1 - dik), y - 1):
                if self.get(x, yy, z) in (None, AIR):
                    n = self.steen(x, yy, z, y - 1, d, graden)
                    self.set(x, yy, z, n, {"axis": "y"} if n == BASALT else None)
            self.set(x, y - 1, z, name or (GEBARSTEN if _h(x, y, z) < 0.2 else STENEN))
            for yy in range(y, y + hoog):
                self.set(x, yy, z, AIR)
            self.pad[(x, z)] = y

    def weg(self, nr, breed=3.2):
        """A road round the cone (PADEN[nr]): built out from the surface on a retaining wall, with half steps where it rises."""
        g0, g1, y0, y1 = PADEN[nr]
        cellen, midden = {}, []
        stappen = int((g1 - g0) * 6)
        for i in range(stappen + 1):
            f = i / float(stappen)
            graden = g0 + (g1 - g0) * f
            y = int(round(y0 + (y1 - y0) * f))
            r = straal(y - 1) * bobbel(graden)
            a = math.radians(graden)
            for k in range(int(breed * 3) + 1):
                rr = r - 0.4 + k / 3.0
                c = (int(math.floor(CX + rr * math.sin(a) + 0.5)), int(math.floor(CZ + rr * math.cos(a) + 0.5)))
                cellen[c] = max(y, cellen.get(c, 0)) if c in cellen and abs(cellen[c] - y) > 1 else cellen.get(c, y)
            rm = r - 0.4 + breed / 2.0
            m = (int(math.floor(CX + rm * math.sin(a) + 0.5)), y, int(math.floor(CZ + rm * math.cos(a) + 0.5)))
            if not midden or midden[-1] != m:
                midden.append(m)
        self.richel(cellen)
        self.middens[nr] = midden
        self.midcellen.update((x, z) for x, _y, z in midden)
        return cellen

    def terras(self, t, name=None, hoog=5, hoek=1):
        """A terrace on a corbel: a rectangle with its corners cut off, a chiselled rim and a low wall along its outer sides."""
        cellen = {}
        for x in range(t["x0"], t["x1"] + 1):
            for z in range(t["z0"], t["z1"] + 1):
                if min(x - t["x0"], t["x1"] - x) + min(z - t["z0"], t["z1"] - z) >= hoek:
                    cellen[(x, z)] = t["y"]
        self.richel(cellen, name, hoog)
        for (x, z) in cellen:
            buren = [(x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)]
            if any(c not in cellen for c in buren):
                self.set(x, t["y"] - 1, z, GEBEITELD)
        self.terrassen.append((t, cellen))

    def leuningen(self):
        """Low walls along the open sides of the terraces: not where a road comes in, not round a launch spot."""
        starts = [h[2] for h in HAKEN.values()] + [self.plekken[f"boven_{n}"] for n in HAKEN if f"boven_{n}" in self.plekken]
        for t, cellen in self.terrassen:
            y = t["y"]
            for (x, z) in cellen:
                buren = [(x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)]
                open_ = [c for c in buren if c not in cellen]
                if not open_ or self.get(x, y, z) != AIR:
                    continue
                if any(abs(self.pad.get(c, -99) - y) <= 1 or self.top.get(c, -99) >= y - 1 for c in open_):
                    continue                                      # a road, or the mountain itself
                if any(abs(x - sx) <= 2 and abs(z - sz) <= 2 and sy == y for sx, sy, sz in starts):
                    continue
                self.wall(x, y, z)

    def halve_treden(self):
        """Where a road cell has a neighbour one higher: a slab to walk up (no jumping with a heavy ring)."""
        for (x, z), y in list(self.pad.items()):
            if self.get(x, y, z) != AIR:
                continue
            if any(self.pad.get(c) == y + 1 for c in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))):
                self.slab(x, y, z)

    def borstwering(self):
        """A low wall here and there on the outer edge of the roads (never where a rope has to pass)."""
        for (x, z), y in list(self.pad.items()):
            d, graden = pool(x, z)
            if self.get(x, y, z) != AIR or any(x in range(t["x0"] - 1, t["x1"] + 2) and z in range(t["z0"] - 1, t["z1"] + 2)
                                                for t in list(RICHEL.values()) + [TUSSEN, OOST, SPLEET]):
                continue
            a = math.radians(graden)
            buiten = (int(math.floor(x + math.sin(a) + 0.5)), int(math.floor(z + math.cos(a) + 0.5)))
            if buiten not in self.pad and _h(x, 11, z) < 0.3 and (x, z) not in self.midcellen:
                self.wall(x, y, z)

    # --- what stands on them ---------------------------------------------------------------------------------------------------
    def bord(self, x, y, z, rotatie, regels):
        import sign_text
        B = self.h.Byte
        leeg = sign_text.messages("sign.guhs.ring_h6", ["", "", "", ""])
        self.set(x, y, z, "minecraft:spruce_sign", {"rotation": str(rotatie), "waterlogged": "false"}, {
            "id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages("sign.guhs.ring_h6", regels), "color": "black", "has_glowing_text": B(0)},
            "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}})

    def kooi(self, nr):
        """A cage of grill bars (5 x 5, three high, a roof of grill iron) with a little Rookguh in it; the lock in its south face."""
        from features import ring_h6_tekst as tekst
        cx, cz, kant = KOOI[nr]
        y = RICHEL[nr]["y"]
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                rand = abs(dx) == 2 or abs(dz) == 2
                hoek = abs(dx) == 2 and abs(dz) == 2
                self.set(cx + dx, y - 1, cz + dz, GEPOLIJST)
                for yy in range(y, y + 3):
                    if hoek:
                        self.set(cx + dx, yy, cz + dz, PILAAR, {"axis": "y"})
                    elif rand:
                        self.bars(cx + dx, yy, cz + dz)
                    else:
                        self.set(cx + dx, yy, cz + dz, AIR)
                self.set(cx + dx, y + 3, cz + dz, ROOSTER if rand else GEPOLIJST)
                self.set(cx + dx, y + 4, cz + dz, AIR)
        self.set(cx, y + 4, cz, GLOEIKOOL)                        # (a coal on the roof: the Mika's keep their fan warm)
        self.set(cx, y + 1, cz + 2 * kant, SLOT, {"nr": str(nr)})
        self.bord(cx - 2, y, cz + 3 * kant, 0 if kant > 0 else 8, tekst.BORDEN[f"kooi_{nr}"])
        self.s.entity(cx + 0.5, y + 0.4, cz + 0.5, rookguh_nbt(self.h, nr))
        self.plekken[f"slot_{nr}"] = (cx, y + 1, cz + 2 * kant)
        self.plekken[f"kooi_{nr}"] = (cx, y, cz)

    def haak(self, nr):
        (x, y, z), facing, start = HAKEN[nr]
        self.set(x, y, z, HAAK, {"facing": facing})
        dx = -1 if facing == "west" else 1
        self.lantern(x, y - 1, z, hanging=True, soul=True)        # a blue lantern under every hook: look up, there it is
        # the rope post at the launch spot
        sx, sy, sz = start
        self.fence(sx, sy, sz + 1)
        self.fence(sx, sy + 1, sz + 1)
        self.lantern(sx, sy + 2, sz + 1, soul=True)
        self.plekken[f"haak_{nr}"] = (x, y, z)
        self.plekken[f"start_{nr}"] = start
        self.plekken[f"boven_{nr}"] = (x - dx, y + 1, z)

    def kamp(self):
        """The base camp on the south side of the plain: the fire, a lean-to, benches, the gate of the Kronkelpad."""
        from features import ring_h6_tekst as tekst
        x, y, z = KAMP_VUUR
        for dx in range(-5, 6):
            for dz in range(-4, 5):
                if math.hypot(dx, dz * 1.2) < 5.3:
                    self.set(x + dx, GROND, z + dz, GEBARSTEN if 1.5 < math.hypot(dx, dz) < 2.8 and _h(dx, 0, dz) < 0.5 else
                             STENEN if math.hypot(dx, dz) < 2.8 else AS_AARDE)
        self.set(x, y, z, VUUR)
        for i in range(3):
            self.set(x - 1 + i, y, z + 3, SATE, {"axis": "x"})
            self.set(x - 4, y, z - 1 + i, SATE, {"axis": "z"})
        # the lean-to in the south-east corner, open to the fire
        for px in (x + 3, x + 6):
            for yy in range(y, y + 3):
                self.set(px, yy, z + 1, SATE, {"axis": "y"})
            for yy in range(y, y + 2):
                self.set(px, yy, z + 3, SATE, {"axis": "y"})
        for px in range(x + 2, x + 8):
            self.slab(px, y + 3, z + 1)
            self.slab(px, y + 2, z + 2, "top")
            self.slab(px, y + 2, z + 3)
        self.set(x + 4, y, z + 2, "guhs:green_kussen", {"facing": "west"})
        self.set(x + 5, y, z + 2, "minecraft:green_carpet")
        self.set(x + 4, y, z + 3, "guhs:block_of_kaasknabbels")
        self.fence(x - 3, y, z + 3)
        self.fence(x - 3, y + 1, z + 3)
        self.lantern(x - 3, y + 2, z + 3)
        self.bord(x + 2, y, z - 2, 2, tekst.BORDEN["kamp"])
        # the gate of the Kronkelpad: two pillars with a Mika head each, a beam, a lantern, the warning
        gx, gz = self.middens[1][0][0] - 1, self.middens[1][0][2] + 3
        for px in (gx - 2, gx + 3):
            for yy in range(y, y + 4):
                self.set(px, yy, gz, PILAAR, {"axis": "y"})
            self.mikakop(px, y + 4, gz, "south")
        for px in range(gx - 2, gx + 4):
            self.set(px, y + 3, gz, PILAAR, {"axis": "x"}) if px not in (gx - 2, gx + 3) else None
        self.hang_lantern(gx, y + 3, gz, 0)
        self.hang_lantern(gx + 1, y + 3, gz, 0)
        self.bord(gx + 4, y, gz + 1, 1, tekst.BORDEN["poort"])
        self.plekken["kamp"] = (x - 1, y, z - 2)
        self.plekken["kamp_vuur"] = (x, y, z)

    def inrichting(self):
        from features import ring_h6_tekst as tekst
        for nr in (1, 2, 3):
            self.kooi(nr)
            vx, vy, vz = VUREN[nr]
            self.set(vx, vy, vz, VUUR)
            for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1), (0, -1), (0, 1), (-1, 0), (1, 0)):
                if self.get(vx + dx, vy - 1, vz + dz) is not None and self.pad.get((vx + dx, vz + dz)) == vy:
                    self.set(vx + dx, vy - 1, vz + dz, GEBARSTEN)
            self.plekken[f"vuur_{nr}"] = (vx, vy, vz)
            t = RICHEL[nr]
            self.plekken[f"richel_{nr}"] = (vx - 1, vy, vz)
            self.haak(nr)
        self.bord(HAKEN[1][2][0] + 2, 30, HAKEN[1][2][2] + 2, 8, tekst.BORDEN["touw"])
        self.bord(OOST["x0"] + 1, OOST["y"], OOST["z0"], 14, tekst.BORDEN["zwaar"])
        self.bord(SPLEET["x0"], SPLEET["y"], SPLEET["z1"], 12, tekst.BORDEN["spleet"])

    # --- the crater -----------------------------------------------------------------------------------------------------------
    def spleet(self):
        """The cleft through the west rim, the steps down and the balcony over the pool."""
        y = SPLEET["y"]
        for i, x in enumerate(range(SPLEET["x1"] + 1, BALKON["x0"])):          # 37, 38: two steps down through the rim
            for z in (47, 48):
                voet = y - 1 - i
                for yy in range(self.top.get((x, z), 0) - 2, voet - 1):
                    self.set(x, yy, z, BLACKSTONE)
                self.set(x, voet - 1, z, GEBEITELD)
                for yy in range(voet, 76):
                    self.set(x, yy, z, AIR)
                self.pad[(x, z)] = voet
        b = BALKON
        for x in range(b["x0"], b["x1"] + 1):
            for z in range(b["z0"], b["z1"] + 1):
                d, _ = pool(x, z)
                self.set(x, b["y"] - 1, z, GEBEITELD if x == b["x1"] or z in (b["z0"], b["z1"]) else STENEN if _h(x, 3, z) > 0.2 else GEBARSTEN)
                self.set(x, b["y"] - 2, z, GEPOLIJST if d < POEL_R + 0.6 else BLACKSTONE)
                for yy in range(b["y"], b["y"] + 9):
                    self.set(x, yy, z, AIR)
                self.pad[(x, z)] = b["y"]
        # two brackets of grill iron under the part that hangs over the pool, lanterns on the corners, a little rail at the sides
        for z in (b["z0"], b["z1"]):
            for x in range(b["x0"] + 1, b["x1"] + 1):
                self.set(x, b["y"] - 3, z, PILAAR, {"axis": "x"})
            self.wall(b["x0"], b["y"], z)
            self.lantern(b["x0"], b["y"] + 1, z)
            self.fence(b["x1"], b["y"], z)
            self.lantern(b["x1"], b["y"] + 1, z, soul=False)
        self.plekken["rand"] = RAND
        self.plekken["spleet"] = (SPLEET["x1"], SPLEET["y"], 48)
        self.plekken["frituur"] = (48, POEL_TOP, 48)

    def mand(self):
        """The frying basket: grill bars round a floor of grill iron, half sunk in the pool, on four chains from a gantry
        that spans the crater from the north rim to the south rim."""
        m0, m1 = MAND
        for x in range(m0, m1 + 1):
            for z in range(m0, m1 + 1):
                rand = x in (m0, m1) or z in (m0, m1)
                hoek = x in (m0, m1) and z in (m0, m1)
                self.set(x, POEL_TOP - 2, z, GEPOLIJST)
                for y in range(POEL_TOP - 1, POEL_TOP + 2):
                    if hoek:
                        self.set(x, y, z, PILAAR, {"axis": "y"})
                    elif rand:
                        self.bars(x, y, z)
        ybalk = 82
        for z in range(36, 60):
            for x in (47, 48):
                self.set(x, ybalk, z, PILAAR, {"axis": "z"})
        for z in (m0, m1):
            for x in range(m0, m1 + 1):
                self.set(x, ybalk - 1, z, PILAAR, {"axis": "x"})
            for x in (m0, m1):
                for y in range(POEL_TOP + 2, ybalk - 1):
                    self.chain(x, y, z)
        # the two pylons on the rim, three by four at the foot and slimmer higher up
        for z0 in (36, 58):
            for x in range(46, 50):
                for z in range(z0, z0 + 2):
                    voet = self.top.get((x, z), TOP)
                    hoek = x in (46, 49)
                    for y in range(voet + 1, ybalk + (1 if not hoek else -3)):
                        if hoek and y > voet + 5:
                            continue
                        self.set(x, y, z, ROOSTER if (y - voet) % 4 else GEPOLIJST)
            self.lantern(45, self.top.get((45, z0), TOP) + 1, z0)
        # smoke vents on the crest
        for graden in range(20, 360, 45):
            a = math.radians(graden)
            x, z = int(CX + 10.6 * math.sin(a) + 0.5), int(CZ + 10.6 * math.cos(a) + 0.5)
            if (x, z) in self.top and (x, z) not in self.pad and self.get(x, self.top[(x, z)] + 1, z) == AIR:
                self.set(x, self.top[(x, z)], z, ROOKGAT)

    # --- the plain ------------------------------------------------------------------------------------------------------------
    def vlakte(self):
        """Basalt needles, smoke vents and embers on the plain; the four mouths stay clear."""
        import random
        rng = random.Random(21302150)
        gezet = 0
        while gezet < 16:
            a, r = rng.uniform(0, 2 * math.pi), rng.uniform(37.0, 45.5)
            x, z = int(CX + r * math.sin(a)), int(CZ + r * math.cos(a))
            if abs(x - CX) < 8 or abs(z - CZ) < 8 or self.get(x, GROND + 1, z) != AIR:
                continue
            gezet += 1
            for dx, dz, hoog in ((0, 0, rng.randint(4, 8)), (1, 0, rng.randint(2, 5)), (0, 1, rng.randint(1, 4)), (-1, 0, rng.randint(0, 2))):
                for y in range(GROND + 1, GROND + 1 + hoog):
                    if (x + dx, z + dz) in self.top:
                        self.set(x + dx, y, z + dz, BASALT, {"axis": "y"})
        for (x, z), t in self.top.items():
            d, _ = pool(x, z)
            if t != GROND or d < R0 + 1 or (x, z) in self.pad or self.get(x, GROND + 1, z) != AIR or (z > 80 and 38 < x < 60):
                continue
            r = _h(x, 9, z)
            if r < 0.006 and abs(x - CX) > 9 and abs(z - CZ) > 9:
                self.set(x, GROND, z, ROOKGAT)
            elif r < 0.035 and self.get(x, GROND, z) != sb.mc(GLOEIKOOL):
                self.set(x, GROND + 1, z, SMEUL)

    # --- what Java reads ------------------------------------------------------------------------------------------------------
    def route_sam(self):
        """Where Sam-guh carries his player: from the fire of the Derde Richel along road 4, through the cleft, to the balcony."""
        vx, vy, vz = VUREN[3]
        punten = [(vx - 0.5, vy, vz + 0.5), (61.5, vy, 41.5)]
        vorige = None
        for (x, y, z) in self.middens[4]:
            if vorige is None or math.dist(vorige, (x, y, z)) >= 3.0:
                punten.append((x + 0.5, y, z + 0.5))
                vorige = (x, y, z)
        punten += [(35.0, SPLEET["y"], 48.0), (37.5, SPLEET["y"] - 1, 48.0), (38.5, SPLEET["y"] - 2, 48.0), (40.5, BALKON["y"], 48.0)]
        self.routes["sam"] = punten


def rookguh_nbt(h, nr):
    """The little Rookguh of cage `nr` (Java: feature/ringh6/GekooideRookguhEntity, Bezetting id ringh6_rookguh_<nr>): only there
    for players who did not open that cage yet (the Zicht marks of ring-kern: steps 0..nr of ring_h6)."""
    return {"id": ROOKGUH, "PersistenceRequired": h.Byte(1), "Invulnerable": h.Byte(1), "Nr": nr, "Rotation": h.floats(0.0, 0.0),
            "Tags": h.ms.NbtList(8, ["guhs_ring_zicht"]), "NeoForgeData": {"guhs_bezetting": f"ringh6_rookguh_{nr}", "guhs_ring_bij": f"ring_h6:0-{nr}"}}


def berg(h):
    b = Berg(h)
    b.massief()
    b.scheuren()
    b.weg(4, breed=2.8)
    b.terras(SPLEET)
    b.terras(RICHEL[3])
    b.terras(OOST)
    b.weg(3, breed=2.8)
    b.terras(RICHEL[2])
    b.terras(TUSSEN)
    b.terras(RICHEL[1])
    b.weg(1)
    b.spleet()
    b.halve_treden()
    b.borstwering()
    b.kamp()
    b.inrichting()
    b.leuningen()
    b.mand()
    b.vlakte()
    b.connect()
    b.route_sam()
    return b


# =====================================================================================================================
# checks
# =====================================================================================================================
def _naam(b, c):
    v = b.s.blocks.get(c)
    return v[0] if v else None


def _vast(name):
    return name is not None and name not in PASSEERT and name != sb.mc(SAUS)


def staplekken(b):
    """Every cell a player can stand in: a block that carries under it, two free cells."""
    blocks = b.s.blocks
    cells = set()
    for (x, y, z), v in blocks.items():
        n = v[0]
        if not _vast(n) or n in (sb.mc(HEK), sb.mc(MUUR), sb.mc(TRALIES)):
            continue
        if _vast(_naam(b, (x, y + 1, z))) or _vast(_naam(b, (x, y + 2, z))):
            if not (n != sb.mc(PLAAT) and _naam(b, (x, y + 1, z)) == sb.mc(PLAAT) and not _vast(_naam(b, (x, y + 2, z))) and not _vast(_naam(b, (x, y + 3, z)))):
                continue
            cells.add((x, y + 2, z))                              # on a half step: counted as the cell above the slab
            continue
        if n == sb.mc(PLAAT) and v[1].get("type") == "bottom":
            cells.add((x, y + 1, z))
            continue
        cells.add((x, y + 1, z))
    return cells


def lopen(cells, start, omlaag=3):
    """Every cell you get to on foot from `start`: one up (a jump or a half step), up to `omlaag` down."""
    if start not in cells:
        return set()
    seen, todo = {start}, [start]
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in [1, 0] + [-i for i in range(1, omlaag + 1)]:
                n = (x + dx, y + dy, z + dz)
                if n in cells:
                    if n not in seen:
                        seen.add(n)
                        todo.append(n)
                    break
    return seen


def vrije_lijn(b, van, naar):
    """No block between the hands of a player standing in cell `van` and the hook block `naar`."""
    a = (van[0] + 0.5, van[1] + 0.9, van[2] + 0.5)
    c = (naar[0] + 0.5, naar[1] + 0.5, naar[2] + 0.5)
    n = int(math.dist(a, c) * 4) + 1
    for i in range(1, n):
        p = [a[k] + (c[k] - a[k]) * i / n for k in range(3)]
        cel = (int(math.floor(p[0])), int(math.floor(p[1])), int(math.floor(p[2])))
        if cel != tuple(naar) and _vast(_naam(b, cel)):
            return cel
    return None


def check(b):
    problems = []
    blocks = b.s.blocks
    cells = staplekken(b)
    p = b.plekken
    for naam in ("kamp", "kamp_vuur", "rand", "spleet", "frituur") + tuple(f"{s}_{n}" for s in ("slot", "kooi", "vuur", "richel", "haak", "start", "boven")
                                                                             for n in (1, 2, 3)):
        if naam not in p:
            problems.append(f"no spot '{naam}'")
    if problems:
        return problems
    for n in (1, 2, 3):
        if _naam(b, p[f"vuur_{n}"]) != VUUR:
            problems.append(f"no Rustvuurtje on richel {n}")
        if _naam(b, p[f"slot_{n}"]) != SLOT:
            problems.append(f"no lock on cage {n}")
        if _naam(b, p[f"haak_{n}"]) != HAAK:
            problems.append(f"no hook {n}")
        if tuple(p[f"boven_{n}"]) not in cells:
            problems.append(f"hook {n}: nowhere to stand above it at {p[f'boven_{n}']}")
        if tuple(p[f"richel_{n}"]) not in cells:
            problems.append(f"richel {n}: its spot {p[f'richel_{n}']} is not a place to stand")
    if _naam(b, p["kamp_vuur"]) != VUUR:
        problems.append("no Rustvuurtje in the camp")
    for naam in ("kamp", "rand", "spleet", "start_1", "start_2", "start_3"):
        if tuple(p[naam]) not in cells:
            problems.append(f"'{naam}' at {p[naam]} is not a place to stand")
    if problems:
        return problems
    # the climb, stage by stage: on foot, then the rope
    voet = lopen(cells, tuple(p["kamp"]))
    if tuple(p["richel_1"]) not in voet or tuple(p["start_1"]) not in voet:
        problems.append("the Kronkelpad does not lead from the camp to the Eerste Richel")
    for verboden in ("boven_1", "richel_2", "richel_3", "spleet", "rand"):
        if tuple(p[verboden]) in voet:
            problems.append(f"'{verboden}' can be walked to from the camp: the rope would not be needed")
    etappes = [("start_1", "haak_1", "boven_1", ["start_2"]), ("start_2", "haak_2", "boven_2", ["richel_2", "start_3"]),
               ("start_3", "haak_3", "boven_3", ["richel_3", "spleet", "rand"])]
    for start, haak, boven, doelen in etappes:
        hk = p[haak]
        afstand = math.dist((p[start][0] + 0.5, p[start][1] + 1.6, p[start][2] + 0.5), (hk[0] + 0.5, hk[1] + 0.5, hk[2] + 0.5))
        if afstand > 22:
            problems.append(f"{haak} is {afstand:.1f} blocks from its launch spot (the rope reaches 24)")
        botst = vrije_lijn(b, p[start], hk)
        if botst:
            problems.append(f"the rope from {start} to {haak} hits {_naam(b, botst)} at {botst}")
        verder = lopen(cells, tuple(p[boven]))
        for doel in doelen:
            if tuple(p[doel]) not in verder:
                problems.append(f"from above {haak} you can't walk to '{doel}'")
    for (x, y, z) in b.routes["sam"]:
        cel = (int(math.floor(x)), int(y), int(math.floor(z)))
        if cel not in cells and (cel[0], cel[1] + 1, cel[2]) not in cells and (cel[0], cel[1] - 1, cel[2]) not in cells:
            problems.append(f"Sam-guh's route passes {cel}, where nobody can stand")
    # the pool is a closed bowl
    saus = [c for c, v in blocks.items() if v[0] == sb.mc(SAUS)]
    for (x, y, z) in saus:
        for c in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y - 1, z)):
            n = _naam(b, c)
            if n is None or n == AIR:
                problems.append(f"the frituur would run out of the pool at {c}")
                break
    if len(saus) < 300:
        problems.append(f"the pool holds only {len(saus)} blocks of sauce")
    if sum(1 for v in blocks.values() if v[0].endswith("_sign")) < 8:
        problems.append("signs are missing")
    if len(b.s.entities) != 3:
        problems.append(f"{len(b.s.entities)} caged Rookguhs instead of 3")
    # nothing that hangs on its neighbour across a seam of the burcht
    from features import paleizen_bouw
    problems += paleizen_bouw.check_steun(b, ANKER)
    return problems


# =====================================================================================================================
# the test mountain (RingH6GameTests): the same named spots on one floor
# =====================================================================================================================
def test_berg(h):
    s = h.Structure((25, 12, 17))
    for x in range(25):
        for z in range(17):
            s.set(x, 0, z, HOUTSKOOL)
            s.set(x, 1, z, STENEN)
    p = {"kamp": (2, 2, 3), "kamp_vuur": (2, 2, 2), "rand": (21, 2, 13), "spleet": (15, 2, 15), "frituur": (22, 1, 14)}
    s.set(2, 2, 2, VUUR)
    # three "ledges" in a row: a fire, a lock, and a pillar with the hook that leads to the next one
    for n in (1, 2, 3):
        x = 2 + n * 5
        s.set(x, 2, 2, VUUR)
        s.set(x + 2, 3, 2, SLOT, {"nr": str(n)})
        s.set(x + 2, 2, 2, STENEN)
        hoog = 3 + n
        for y in range(2, 2 + hoog):
            s.set(x, y, 8, STENEN)
        s.set(x, 1 + hoog, 9, HAAK, {"facing": "south"})
        p[f"vuur_{n}"] = (x, 2, 2)
        p[f"slot_{n}"] = (x + 2, 3, 2)
        p[f"kooi_{n}"] = (x + 2, 2, 4)
        p[f"richel_{n}"] = (x + 1, 2, 4)
        p[f"haak_{n}"] = (x, 1 + hoog, 9)
        p[f"start_{n}"] = (x, 2, 13)
        p[f"boven_{n}"] = (x, 2 + hoog, 8)
    # the pool: a tub of sauce in the floor
    for x in range(22, 24):
        for z in range(14, 16):
            s.set(x, 1, z, SAUS, {"level": "0"})
    routes = {"sam": [(8.5, 2, 5.5), (12.5, 2, 6.5), (16.5, 2, 11.5), (18.5, 2, 13.5), (21.5, 2, 13.5)]}
    return s, p, routes


def gegevens(b_plekken, routes, grootte, anker):
    """The json Java reads (feature/ringh6/Berg): the named spots and routes in template coordinates."""
    return {"grootte": list(grootte), "anker": list(anker), "plekken": {k: list(v) for k, v in sorted(b_plekken.items())},
            "routes": {k: [[round(c, 2) for c in punt] for punt in v] for k, v in sorted(routes.items())}}


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    def __init__(self):
        import make_structures as ms
        self.ms, self.Structure, self.Byte, self.floats = ms, ms.Structure, ms.Byte, ms.floats


def gedraaid(s, kwart):
    import make_structures as ms
    Wx, Hy, Dz = s.size
    for _ in range(kwart % 4):
        t = ms.Structure((Dz, Hy, Wx))
        for (x, y, z), bl in s.blocks.items():
            t.blocks[(Dz - 1 - z, y, x)] = bl
        s, (Wx, Hy, Dz) = t, t.size
    return s


def preview(out, alleen=None):
    import make_structures as ms
    import wiki_renders as wr
    wr.SPECIAL_COLOURS.update({VUUR: (255, 150, 50), "minecraft:lantern": (255, 214, 120), "minecraft:soul_lantern": (110, 220, 240),
                               "minecraft:chain": (70, 74, 90), sb.mc(HEK): (57, 47, 47), "minecraft:spruce_sign": (150, 112, 66),
                               HAAK: (230, 236, 246), SLOT: (240, 200, 60), sb.mc(SAUS): (238, 150, 36), sb.mc(SMEUL): (236, 110, 40),
                               "guhs:green_kussen": (90, 130, 60)})
    wr.SEE_THROUGH = tuple(wr.SEE_THROUGH) + ("tralies",)
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    b = berg(_H())
    for p in check(b):
        print("PROBLEM:", p)
    print("blocks:", len(b.s.blocks), "of which air:", sum(1 for v in b.s.blocks.values() if v[0] == AIR))
    s = b.s
    if alleen in (None, "heel"):
        for kwart in (0, 1, 2, 3):
            wr.render_structure(gedraaid(s, kwart), {}, px=7, max_size=1500).save(os.path.join(out, f"frituurberg_{kwart}.png"))
    if alleen in (None, "krater"):
        # the crater from above: everything over the gantry left out, the west half of the rim cut away
        k = ms.Structure((40, 40, 40))
        for (x, y, z), bl in s.blocks.items():
            if 28 <= x < 68 and 28 <= z < 68 and 50 <= y < 90:
                k.blocks[(x - 28, y - 50, z - 28)] = bl
        wr.render_structure(k, {}, px=16, max_size=1500).save(os.path.join(out, "krater_0.png"))
        wr.render_structure(gedraaid(k, 2), {}, px=16, max_size=1500).save(os.path.join(out, "krater_2.png"))
    if alleen in (None, "west"):
        # the west face with its three terraces
        k = ms.Structure((34, 62, 40))
        for (x, y, z), bl in s.blocks.items():
            if 14 <= x < 48 and 28 <= z < 68 and 9 <= y < 71:
                k.blocks[(x - 14, y - 9, z - 28)] = bl
        wr.render_structure(k, {}, px=14, max_size=1500).save(os.path.join(out, "west.png"))
    if alleen in (None, "kamp"):
        k = ms.Structure((30, 16, 22))
        for (x, y, z), bl in s.blocks.items():
            if 34 <= x < 64 and 74 <= z < 96 and 8 <= y < 24:
                k.blocks[(x - 34, y - 8, z - 74)] = bl
        wr.render_structure(gedraaid(k, 2), {}, px=22, max_size=1500).save(os.path.join(out, "kamp.png"))
    return b


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".", sys.argv[2] if len(sys.argv) > 2 else None)
