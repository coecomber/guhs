"""
1.10.10 Guhwinter, slice plein (design: MineScapeStory/guhswinter/GUHSWINTER.md R2, R7, R8, R11; terrain: guhswinter/TERREIN.md;
Java: feature/guhwinter/plein): the templates of the event square in the Guhmension and its anchors.

  guhwinter_plein_winter   the snowy Dutch winter square (75 x 75, x -350..-276, z -735..-661, floor y 30)
  guhwinter_plein_raam     the empty event frame for after the event (same footprint: low wall + hedge, gate, plain square)
  guhwinter_aankomst       the arrival around the new spawn portal (outside the square; only cheap, breakable blocks)
  data/guhs/guhwinter/ankers.json   every named spot (world coordinates; GuhwinterAnkers.get)

Everything is in WORLD coordinates; the templates are stamped at their min corner (GuhwinterPlein.WINTER_MIN etc.).
Standalone and idempotent:   python tools/features/guhwinter_plein.py     (from the project root)
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import make_structures as ms  # noqa: E402

# --- the square ---------------------------------------------------------------------------------------------------------
X1, Z1, X2, Z2 = -350, -735, -276, -661       # footprint (inclusive)
VLOER = 30                                     # the top of the paving: you walk at y 31
Y0, HOOGTE = 24, 36                            # template y range: 24..59
PLEIN_MIN = (X1, Y0, Z1)
# --- the arrival (outside the square, south of the gate) ---------------------------------------------------------------
AX1, AZ1, AX2, AZ2 = -310, -660, -282, -640
AVLOER = 29                                    # its ground: the old valley floor
AY0, AHOOGTE = 26, 12
AANKOMST_MIN = (AX1, AY0, AZ1)
# --- the new spawn portal (frame x -301..-298, y 30..34, z -655; the portal blocks x -300..-299, y 31..33) ---------------
PORTAAL = (-300, 30, -655)

ANKERS_PAD = os.path.join("src", "main", "resources", "data", "guhs", "guhwinter", "ankers.json")
EN_OVERLAY = os.path.join("tools", "lang", "en", "c480_guhwinter_plein.json")
TEKSTEN = {}                                   # key -> (nl, en)


def tekst(key, nl, en):
    TEKSTEN[key] = (nl, en)
    return key


# command feedback (Java: GuhwinterPleinCommando)
for _k, _nl, _en in (
        ("commands.guhs.guhwinter.inrichten.start", "Guhwinter: het plein wordt ingericht, njeg! Even geduld (zie de serverlog).",
         "Guhwinter: setting up the square, njeg! Hang on (see the server log)."),
        ("commands.guhs.guhwinter.inrichten.bezig", "Guhwinter: er is al een inrichting bezig: %s",
         "Guhwinter: a setup is already running: %s"),
        ("commands.guhs.guhwinter.inrichten.klaar", "Guhwinter: klaar! %s VAHOEG",
         "Guhwinter: done! %s VAHOEG"),
        ("commands.guhs.guhwinter.inrichten.geen_dim", "Guhwinter: de Guhmension is er niet.",
         "Guhwinter: the Guhmension is missing."),
        ("commands.guhs.guhwinter.plein.start", "Guhwinter: het plein (%s) wordt neergezet.",
         "Guhwinter: placing the square (%s)."),
        ("commands.guhs.guhwinter.anker", "Anker %s: %s %s %s (yaw %s)", "Anchor %s: %s %s %s (yaw %s)"),
        ("commands.guhs.guhwinter.anker.onbekend", "Geen anker met de naam %s.", "No anchor named %s."),
):
    tekst(_k, _nl, _en)


def mc(n):
    return n if ":" in n else "minecraft:" + n


def g(n):
    return "guhs:" + n


DIRS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}
LEFT = {"north": "west", "west": "south", "south": "east", "east": "north"}     # turning left when facing that way
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}               # Minecraft yaw of someone looking that way

LICHT = {"minecraft:lantern": 15, "minecraft:soul_lantern": 10, "minecraft:glowstone": 15, "minecraft:shroomlight": 15,
         "minecraft:sea_lantern": 15, "minecraft:campfire": 15, "minecraft:torch": 14, "minecraft:wall_torch": 14,
         "minecraft:jack_o_lantern": 15, "minecraft:ochre_froglight": 15, "minecraft:pearlescent_froglight": 15,
         "minecraft:end_rod": 14, "guhs:lampion_roze": 15, "guhs:lampion_geel": 15, "guhs:lampion_mint": 15,
         "guhs:sterrenlantaarn": 15, "minecraft:redstone_lamp": 15}


class Plan:
    """A template in world coordinates."""

    def __init__(self, mn, size):
        self.mn = mn
        self.s = ms.Structure(size)
        self.ankers = {}
        self.pad = set()              # (x, z) where no snow goes (paths, doors, ice...)
        self.lichten = []             # (x, y, z, level)

    def _l(self, x, y, z):
        return (x - self.mn[0], y - self.mn[1], z - self.mn[2])

    def binnen(self, x, y, z):
        return self.s.inside(*self._l(x, y, z))

    def set(self, x, y, z, naam, props=None, nbt=None):
        naam = mc(naam)
        if "[" in naam:
            naam, rest = naam[:-1].split("[", 1)
            p = dict(kv.split("=") for kv in rest.split(","))
            p.update(props or {})
            props = p
        if not self.binnen(x, y, z):
            raise SystemExit(f"guhwinter_plein: {naam} at {x},{y},{z} is outside the template {self.mn} {self.s.size}")
        if naam == "minecraft:air":
            self.s.blocks[self._l(x, y, z)] = ("minecraft:air", {}, None)
        else:
            self.s.set(*self._l(x, y, z), naam, props, nbt)
        if naam in LICHT and (props or {}).get("lit", "true") != "false":
            self.lichten.append((x, y, z, LICHT[naam]))
        if naam == "guhs:guh_lantaarnpaal" and (props or {}).get("half") == "upper":
            self.lichten.append((x, y, z, 15))

    def get(self, x, y, z):
        b = self.s.blocks.get(self._l(x, y, z))
        return b[0] if b else None

    def leeg(self, x, y, z):
        b = self.get(x, y, z)
        return b is None or b == "minecraft:air"

    def fill(self, x1, y1, z1, x2, y2, z2, naam, props=None):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, naam, props)

    def walls(self, x1, y1, z1, x2, y2, z2, naam, props=None):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                self.set(x, y, z1, naam, props)
                self.set(x, y, z2, naam, props)
            for z in range(z1, z2 + 1):
                self.set(x1, y, z, naam, props)
                self.set(x2, y, z, naam, props)

    def weg(self, x, y, z):
        self.s.blocks.pop(self._l(x, y, z), None)

    # --- shapes ---------------------------------------------------------------------------------------------------------
    def trap(self, x, y, z, blok, facing, half="bottom", shape="straight"):
        self.set(x, y, z, blok, {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"})

    def plaat(self, x, y, z, blok, typ="bottom"):
        self.set(x, y, z, blok, {"type": typ, "waterlogged": "false"})

    def lantaarn(self, x, y, z, hangend=False):
        self.set(x, y, z, "lantern", {"hanging": str(hangend).lower(), "waterlogged": "false"})

    def lampion(self, x, y, z, kleur="roze", hangend=True):
        self.set(x, y, z, g("lampion_" + kleur), {"hanging": str(hangend).lower(), "waterlogged": "false"})

    def paal(self, x, y, z):
        """A guh lantern post (two blocks) standing on y - 1."""
        self.set(x, y, z, g("guh_lantaarnpaal"), {"half": "lower"})
        self.set(x, y + 1, z, g("guh_lantaarnpaal"), {"half": "upper"})
        self.pad.add((x, z))

    def bord(self, x, y, z, facing, regels, muur=True, hout="spruce", kleur="black", glim=True, waxed=True):
        """A sign: regels = [(key, nl, en), ...] (keys sign.guhs.guhwinter_plein.<key>); empty list = a blank sign."""
        blank = json.dumps("")
        msgs = []
        for key, nl, en in regels:
            k = tekst(f"sign.guhs.guhwinter_plein.{key}", nl, en)
            msgs.append(json.dumps({"translate": k}))
        msgs += [blank] * (4 - len(msgs))
        text = {"messages": ms.NbtList(8, msgs), "color": kleur, "has_glowing_text": ms.Byte(1 if glim else 0)}
        back = {"messages": ms.NbtList(8, [blank] * 4), "color": "black", "has_glowing_text": ms.Byte(0)}
        nbt = {"id": "minecraft:sign", "is_waxed": ms.Byte(1 if waxed else 0), "front_text": text, "back_text": back}
        if muur:
            self.set(x, y, z, f"{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
            self.set(x, y, z, f"{hout}_sign", {"rotation": rot, "waterlogged": "false"}, nbt)

    def banier(self, x, y, z, basis, patronen, facing=None, rotatie=0):
        nbt = {"id": "minecraft:banner", "patterns": ms.NbtList(10, [{"color": c, "pattern": "minecraft:" + p} for c, p in patronen])}
        if facing:
            self.set(x, y, z, f"{basis}_wall_banner", {"facing": facing}, nbt)
        else:
            self.set(x, y, z, f"{basis}_banner", {"rotation": str(rotatie % 16)}, nbt)

    def anker(self, naam, x, y, z, yaw=0.0):
        if naam in self.ankers:
            raise SystemExit(f"guhwinter_plein: anchor {naam} twice")
        self.ankers[naam] = {"x": x, "y": y, "z": z, "yaw": yaw}

    def deur(self, x, y, z, facing, hout="spruce", scharnier="left"):
        for dy, half in ((0, "lower"), (1, "upper")):
            self.set(x, y + dy, z, f"{hout}_door", {"facing": facing, "half": half, "hinge": scharnier, "open": "false", "powered": "false"})
        self.pad.add((x, z))

    def raam(self, x, y, z, as_ns, glas="glass_pane"):
        """A window pane (as_ns: the window lies in a wall along x, so the pane connects east-west)."""
        self.set(x, y, z, glas, {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})

    def spar(self, x, y, z, hoog=7, sneeuw=True, gezicht=None):
        """A snowy guh spruce in tiers: trunk on y, needles (persistent) from y + 1, optional guh face on the trunk."""
        naald = {"distance": "1", "persistent": "true", "sneeuw": str(sneeuw).lower(), "waterlogged": "false"}
        for dy in range(hoog - 1):
            self.set(x, y + dy, z, g("sneeuwguhspar_stam"), {"axis": "y"})
        if gezicht:
            self.set(x, y, z, g("sneeuwguhspar_gezicht"), {"facing": gezicht})
        rmax = 1.2 + hoog * 0.28
        for dy in range(1, hoog + 1):
            yy = y + dy
            u = (dy - 1) / max(1, hoog - 1)
            r = rmax * (1 - u) + (0.0 if dy % 2 else -0.75)
            if dy == hoog:
                r = 0
            ri = int(math.ceil(max(r, 0)))
            for dx in range(-ri, ri + 1):
                for dz in range(-ri, ri + 1):
                    if dx * dx + dz * dz > r * r + 0.35 and (dx, dz) != (0, 0):
                        continue
                    if (dx, dz) == (0, 0) and dy < hoog - 1:
                        continue
                    if self.binnen(x + dx, yy, z + dz) and X1 < x + dx < X2 and Z1 < z + dz < Z2 and self.leeg(x + dx, yy, z + dz):
                        self.set(x + dx, yy, z + dz, g("sneeuwguhspar_naalden"), naald)
        self.pad.add((x, z))

    def bankje(self, x, y, z, facing, soort="hout"):
        self.set(x, y, z, g("guh_bankje_" + soort), {"facing": facing})


def oppervlak(p, x, z, blok):
    p.set(x, VLOER, z, blok)


# =====================================================================================================================
# The frame: low wall with a hedge, posts with lanterns, the gate in the south (both variants)
# =====================================================================================================================
POORT = (-302, -297)            # the opening in the south wall (x), straight north of the portal


def rand(p, winter):
    heg = {"distance": "7", "persistent": "true", "waterlogged": "false"}
    for x in range(X1, X2 + 1):
        for z in range(Z1, Z2 + 1):
            if x not in (X1, X2) and z not in (Z1, Z2):
                continue
            if z == Z2 and POORT[0] <= x <= POORT[1]:
                continue
            p.set(x, VLOER, z, "stone_bricks")
            p.set(x, VLOER + 1, z, "stone_bricks")
            hoek = x in (X1, X2) and z in (Z1, Z2)
            post = hoek or ((x - X1) % 8 == 0 and z in (Z1, Z2)) or ((z - Z1) % 8 == 0 and x in (X1, X2))
            if post:
                p.set(x, VLOER + 2, z, "chiseled_stone_bricks")
                p.set(x, VLOER + 3, z, "stone_brick_wall")
                p.lantaarn(x, VLOER + 4, z)
            else:
                p.set(x, VLOER + 2, z, "spruce_leaves" if winter else "azalea_leaves", heg)
            p.pad.add((x, z))
    # the gate: two pillars and a beam one row in, the signs on its south face (readable from the portal)
    zb = Z2 - 1
    for x in (POORT[0] - 1, POORT[1] + 1):
        p.fill(x, VLOER + 1, Z2, x, VLOER + 5, Z2, "stone_bricks")
        p.fill(x, VLOER + 1, zb, x, VLOER + 5, zb, "stone_bricks")
        p.set(x, VLOER + 6, Z2, "stone_brick_wall")
        p.lantaarn(x, VLOER + 7, Z2)
    for x in range(POORT[0] - 1, POORT[1] + 2):
        p.set(x, VLOER + 6, zb, "stone_bricks")
        p.plaat(x, VLOER + 7, zb, "stone_brick_slab")
    for x in range(POORT[0], POORT[1] + 1):
        p.trap(x, VLOER + 5, zb, "stone_brick_stairs", "south", "top")
    if winter:
        p.bord(-300, VLOER + 6, Z2, "south", [("poort_1", "Welkom op het", "Welcome to the"), ("poort_2", "Winterplein!", "Winter Square!")])
        p.bord(-299, VLOER + 6, Z2, "south", [("poort_3", "Njeg njeg,", "Njeg njeg,"), ("poort_4", "VAHOEG!", "VAHOEG!")])
        for x in range(POORT[0], POORT[1] + 1):
            p.set(x, VLOER + 4, zb, g("seizoensslinger"), {"axis": "x", "seizoen": "winter"})
    else:
        p.bord(-300, VLOER + 6, Z2, "south", [("raam_1", "Hier komt het", "The next party"), ("raam_2", "volgende feest!", "comes here!")])
        p.bord(-299, VLOER + 6, Z2, "south", [("raam_3", "Tot snel,", "See you soon,"), ("raam_4", "VAHOEG!", "VAHOEG!")])
    for x in range(POORT[0], POORT[1] + 1):
        p.set(x, VLOER, Z2, "polished_andesite")
        p.pad.add((x, Z2))


# =====================================================================================================================
# The winter square
# =====================================================================================================================
# canal (gracht): water z -670..-666 (2 deep, surface y 29), the harbour basin east of the bridge down to the south wall
GRACHT_Z = (-670, -666)
HAVEN_X = (-294, -278)
BRUG_X = POORT                    # the bridge right behind the gate
HUIZEN_Z = (-683, -674)           # canal houses: front (south) at -674
PANDEN = [  # x1, x2, wall, trim, storeys, gable, roof, door x offset
    (-349, -342, g("pastelbaksteen_framboos"), "quartz_block", 3, "trap", "dakpan_kersrood_trap", 2),
    (-341, -335, g("pastelbaksteen_boter"), "quartz_block", 4, "klok", "dakpan_roze_trap", 3),
    (-334, -327, "bricks", "quartz_block", 3, "hals", "dakpan_hemelsblauw_trap", 2),
    (-326, -319, g("pastelbaksteen_korenbloem"), "quartz_block", 3, "trap", "dakpan_lavendel_trap", 5),
    (-318, -312, g("pastelbaksteen_perzik"), "quartz_block", 4, "tuit", "dakpan_mint_trap", 3),
    (-311, -303, g("pastelbaksteen_mokka"), "quartz_block", 3, "klok", "dakpan_kersrood_trap", 2),
]
HUIS = (-324, -734, -303, -719)   # Krijn & Corine (x1, z1, x2, z2), front (south) at -719
HUISVLOER = VLOER + 1             # their floor: one step up from the street
VIJVER = (-288, -721, 7.5, 9.5)   # skating pond: centre x, z and radii
KRAAM_X = (-325, -321)
KRAMEN = [-715, -709, -703, -697]  # z1 of every stall (5 wide)
VELD_X = (-348, -334)             # min x of the west and east build plots (7 x 7 inside)
VELD_Z = [-732, -722, -712, -702]  # min z of the plots
STRAAT_X = (-340, -336)           # Surprisestraatje
WEG_SPARREN = [(-346, -732), (-329, -733)]   # (1.10.11) deco spruces removed: in plot 0 / on plot 4's fence (check_velden)


def is_gracht(x, z):
    if GRACHT_Z[0] <= z <= GRACHT_Z[1] and X1 + 1 <= x <= X2 - 1:
        return True
    return HAVEN_X[0] <= x <= HAVEN_X[1] and GRACHT_Z[0] - 1 <= z <= Z2 - 1


def paving(p):
    """Klinkers everywhere, herringbone paths, canal and kades."""
    rng = random.Random(1109)
    for x in range(X1 + 1, X2):
        for z in range(Z1 + 1, Z2):
            p.set(x, VLOER - 1, z, "pink_wool")
            r = rng.random()
            p.set(x, VLOER, z, g("knuffelklinkers") if r < 0.86 else ("stone_bricks" if r < 0.95 else "mossy_stone_bricks"))
    # the canal: kade walls of stone bricks, water 2 deep
    for x in range(X1 + 1, X2):
        for z in range(Z1 + 1, Z2):
            if not is_gracht(x, z):
                continue
            p.set(x, VLOER - 4, z, "stone_bricks")
            p.set(x, VLOER - 3, z, "gravel" if (x + z) % 3 else "clay")
            p.set(x, VLOER - 2, z, "water", {"level": "0"})
            p.set(x, VLOER - 1, z, "water", {"level": "0"})
            p.set(x, VLOER, z, "air")
            p.pad.add((x, z))
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, nz = x + dx, z + dz
                if X1 < nx < X2 and Z1 < nz < Z2 and not is_gracht(nx, nz):
                    for y in range(VLOER - 4, VLOER):
                        p.set(nx, y, nz, "stone_bricks")
                    p.set(nx, VLOER, nz, "polished_andesite")      # the kade edge
                    p.pad.add((nx, nz))
    # bollards and rings along the kade
    for x in range(X1 + 4, HAVEN_X[0] - 6, 6):
        p.set(x, VLOER + 1, GRACHT_Z[0] - 1, "spruce_fence")
    # main paths: gate -> bridge -> between the houses -> the square; the square's ring
    def tegelpad(x1, z1, x2, z2, blok=g("tegel_visgraat")):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                if p.get(x, VLOER, z) in (g("knuffelklinkers"), "minecraft:stone_bricks", "minecraft:mossy_stone_bricks"):
                    p.set(x, VLOER, z, blok)
                p.pad.add((x, z))
    tegelpad(X1 + 1, Z2 - 4, HAVEN_X[0] - 2, Z2 - 3)                       # south kade walk
    tegelpad(X1 + 1, HUIZEN_Z[1] + 1, HAVEN_X[0] - 2, HUIZEN_Z[1] + 2)     # north kade walk in front of the houses
    tegelpad(BRUG_X[0] + 1, HUIZEN_Z[0] - 1, BRUG_X[1] - 1, HUIZEN_Z[1] + 2)  # the street between the houses
    tegelpad(X1 + 1, HUIZEN_Z[0] - 2, X2 - 1, HUIZEN_Z[0] - 1)             # back street along the houses
    tegelpad(STRAAT_X[0], Z1 + 1, STRAAT_X[1], HUIZEN_Z[0] - 3)            # Surprisestraatje
    tegelpad(KRAAM_X[1] + 1, -716, KRAAM_X[1] + 2, HUIZEN_Z[0] - 3)       # along the stalls
    tegelpad(-315, HUIS[3] + 1, -312, HUIZEN_Z[0] - 3)                     # from the house door to the back street
    tegelpad(KRAAM_X[1] + 1, -716, -297, -715)                             # in front of the house
    tegelpad(-302, -716, -298, HUIZEN_Z[0] - 3)                            # from the bridge street up to the pond


def brug(p):
    """A little arched bridge over the canal right behind the gate."""
    z1, z2 = GRACHT_Z
    for x in range(BRUG_X[0], BRUG_X[1] + 1):
        rail = x in (BRUG_X[0], BRUG_X[1])
        for z in range(z1, z2 + 1):
            mid = z1 < z < z2
            if mid:
                p.set(x, VLOER, z, "spruce_planks")
                p.trap(x, VLOER - 1, z, "stone_brick_stairs", "north" if z == z1 + 1 else ("south" if z == z2 - 1 else "north"), "top") if z != (z1 + z2) // 2 else p.plaat(x, VLOER - 1, z, "stone_brick_slab", "top")
                if rail:
                    p.set(x, VLOER + 1, z, "spruce_fence")
            else:
                p.plaat(x, VLOER, z, "spruce_slab", "top")
                if rail:
                    p.set(x, VLOER + 1, z, "spruce_fence")
            p.pad.add((x, z))
        if rail:
            p.lantaarn(x, VLOER + 2, (z1 + z2) // 2)
    for x in (BRUG_X[0], BRUG_X[1]):
        for z in (z1, z2):
            p.set(x, VLOER + 1, z, "stone_brick_wall")
            p.set(x, VLOER + 2, z, "air")


def gevel_rijen(soort, w):
    """The gable above the eaves: per row (from 0) the inset from both sides; None ends it. Plus the cap rows."""
    rows = []
    if soort == "trap":                       # stepped gable: steps of 2 rows
        inset = 0
        while w - 2 * inset >= 2:
            rows += [inset, inset]
            inset += 1
        return rows
    if soort == "tuit":                       # simple point
        return list(range((w + 1) // 2))
    if soort == "klok":                       # bell: full for 2 rows, then a narrow body with a round top
        body = max(1, (w - 4) // 2)
        return [0, 0, body - 1, body, body, body, body + 1]
    if soort == "hals":                       # neck gable: 1 full row, then the neck
        body = max(1, (w - 4) // 2)
        return [0, body, body, body, body, body + 1]
    return []


def pand(p, x1, x2, muur, trim, verdiepingen, gevel, dak, deur_dx, idx):
    """A canal house, front (south) at HUIZEN_Z[1], back at HUIZEN_Z[0]. Storeys of 4: floors at 30, 35, 39, (43).

    (1.10.11) The ground floor is level with the street (planks at VLOER, the door's sill height), so you walk straight in;
    it used to be at VLOER + 1, one block above the doorway, and nobody could get inside. Its room is 4 high (31..34)."""
    zf, zb = HUIZEN_Z[1], HUIZEN_Z[0]
    w = x2 - x1 + 1
    top = VLOER + 1 + 4 * verdiepingen        # first row above the top storey
    # walls and floors
    for y in range(VLOER, top):
        for x in range(x1, x2 + 1):
            for z in range(zb, zf + 1):
                rand_ = x in (x1, x2) or z in (zb, zf)
                if rand_:
                    p.set(x, y, z, muur)
                elif y == VLOER or (y > VLOER + 1 and (y - VLOER - 1) % 4 == 0):
                    p.set(x, y, z, "spruce_planks")
                else:
                    p.set(x, y, z, "air")
    # white trim: plinth and cornice
    for x in range(x1, x2 + 1):
        for z in (zb, zf):
            p.set(x, VLOER + 1, z, "stone_bricks")
            p.set(x, top - 1, z, trim)
    # gable front and back + the roof between them (ridge along z)
    rows = gevel_rijen(gevel, w)
    for z in (zb, zf):
        for i, inset in enumerate(rows):
            for x in range(x1 + inset, x2 - inset + 1):
                p.set(x, top + i, z, muur)
        if rows:
            last = len(rows)
            ins = rows[-1]
            for x in range(x1 + ins, x2 - ins + 1):
                p.plaat(x, top + last, z, "smooth_quartz_slab")
            # caps on every step
            for i, inset in enumerate(rows):
                nxt = rows[i + 1] if i + 1 < len(rows) else None
                if nxt is not None and nxt > inset:
                    for x in list(range(x1 + inset, x1 + nxt)) + list(range(x2 - nxt + 1, x2 - inset + 1)):
                        if p.leeg(x, top + i + 1, z):
                            p.plaat(x, top + i + 1, z, "smooth_quartz_slab")
    half = (w - 1) / 2.0
    for k in range(0, (w + 1) // 2 + 1):
        y = top + k
        xl, xr = x1 + k, x2 - k
        if xl > xr:
            break
        for z in range(zb + 1, zf):
            if xl == xr:
                p.plaat(xl, y, z, dak.replace("_trap", "_plaat"))
            elif xr - xl == 1:
                p.trap(xl, y, z, dak, "east")
                p.trap(xr, y, z, dak, "west")
            else:
                p.trap(xl, y, z, dak, "east")
                p.trap(xr, y, z, dak, "west")
                for x in range(xl + 1, xr):
                    if p.leeg(x, y, z):
                        p.set(x, y, z, "air")
    # chimney at the back, through the roof (with smoke)
    cx = x1 + 1 if idx % 2 else x2 - 1
    cz = zb + 2
    ch = top + (min(cx - x1, x2 - cx)) + 2
    p.fill(cx, top, cz, cx, ch, cz, "bricks")
    p.set(cx, ch + 1, cz, g("guh_schoorsteen"), {"rookt": "true"})
    # windows: 2 high, in every storey, front and back
    for s in range(verdiepingen):
        yb = VLOER + 2 + 4 * s
        for xw in range(x1 + 1, x2):
            if (xw - x1) % 2 == 0:
                continue
            for z in (zb, zf):
                if s == 0 and z == zf and abs(xw - (x1 + deur_dx)) <= 1:
                    continue
                if s == 0 and z == zb and xw == x1 + w // 2:
                    continue
                p.raam(xw, yb, z, True)
                p.raam(xw, yb + 1, z, True)
                p.set(xw, yb + 2, z, trim)
        # a warm lamp in every storey so the windows glow
        p.set(x1 + w // 2, yb + 2, (zb + zf) // 2, "lantern", {"hanging": "true", "waterlogged": "false"})
        p.lichten.append((x1 + w // 2, yb + 2, (zb + zf) // 2, 15))
    # gable window + hoisting beam
    if rows:
        gx = x1 + w // 2
        p.raam(gx, top + 1, zf, True)
        if len(rows) > 3:
            p.set(gx, top + 3, zf + 1, "spruce_fence")
            p.set(gx, top + 2, zf + 1, "iron_chain", {"axis": "y", "waterlogged": "false"})
    # door with a stoop and a lantern, a window box, the ground floor room (table, chairs, plant)
    dx = x1 + deur_dx
    p.deur(dx, VLOER + 1, zf, "south", "spruce" if idx % 2 else "dark_oak")
    p.set(dx, VLOER + 3, zf, trim)
    p.set(dx + 1 if dx + 1 < x2 else dx - 1, VLOER + 3, zf + 1, "lantern", {"hanging": "false", "waterlogged": "false"})
    p.lichten.append((dx + 1, VLOER + 3, zf + 1, 15))
    # (1.10.11) the room's things stand on the ground floor at VLOER (one lower than before, with the floor)
    p.set(x2 - 1, VLOER + 1, zb + 2, g("guh_tafel"), {"facing": "south"})
    p.set(x2 - 1, VLOER + 1, zb + 3, g("guh_stoel"), {"facing": "north"})
    p.set(x1 + 1, VLOER + 1, zb + 1, "potted_spruce_sapling")
    p.set(x1 + 1, VLOER + 1, zf - 1, "red_carpet")
    # a garland of winter lights under the cornice
    for x in range(x1 + 1, x2):
        p.set(x, top - 2, zf + 1, g("seizoensslinger"), {"axis": "x", "seizoen": "winter"})
    p.pad.add((dx, zf + 1))


def panden(p):
    for i, (x1, x2, muur, trim, v, gevel, dak, deur) in enumerate(PANDEN):
        pand(p, x1, x2, muur, trim, v, gevel, g(dak), deur, i)
        p.anker(f"pand_deur_{i}", x1 + deur + 0.5, VLOER + 1, HUIZEN_Z[1] + 1.5, YAW["south"])


# --- Krijn & Corine's house -------------------------------------------------------------------------------------------
def huis(p):
    x1, z1, x2, z2 = HUIS
    F = HUISVLOER
    top = F + 9                                   # the eaves: a tall hall inside (32..39)
    muur, trim = "bricks", "quartz_block"
    for y in range(VLOER, top + 1):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                if x in (x1, x2) or z in (z1, z2):
                    p.set(x, y, z, muur)
                elif y <= F:
                    p.set(x, y, z, "spruce_planks" if y == F else "pink_wool")
                elif y == top:
                    p.set(x, y, z, "spruce_planks")
                else:
                    p.set(x, y, z, "air")
    for x in range(x1, x2 + 1):
        for z in (z1, z2):
            p.set(x, VLOER, z, "stone_bricks")
            p.set(x, top, z, trim)
    for z in range(z1, z2 + 1):
        for x in (x1, x2):
            p.set(x, VLOER, z, "stone_bricks")
            p.set(x, top, z, trim)
    # roof: ridge along x (east-west), gables east and west, snowy pan roof
    dak = g("dakpan_kersrood_trap")
    d = z2 - z1 + 1
    for k in range(0, d // 2 + 1):
        y = top + 1 + k
        zn, zs = z1 - 1 + k, z2 + 1 - k
        if zn > zs:
            break
        for x in range(x1 - 1, x2 + 2):
            if zn == zs:
                p.plaat(x, y, zn, g("dakpan_kersrood_plaat"))
            else:
                p.trap(x, y, zn, dak, "south")
                p.trap(x, y, zs, dak, "north")
        for x in (x1, x2):
            for z in range(zn + 1, zs):
                p.set(x, y, z, muur)
    # a stepped front gable above the door (solid behind, capped), with a round window
    gx1, gx2 = -318, -309
    for j, ins in enumerate([0, 0, 1, 2, 3, 4]):
        y = top + 1 + j
        for x in range(gx1 + ins, gx2 - ins + 1):
            for z in range(z2 + 1 - max(j, 1), z2 + 1):
                p.set(x, y, z, muur)
        nxt = [0, 0, 1, 2, 3, 4, 99][j + 1]
        for x in range(gx1 + ins, gx2 - ins + 1):
            if x < gx1 + nxt or x > gx2 - nxt:
                for z in range(z2 + 1 - max(j, 1), z2 + 1):
                    p.plaat(x, y + 1, z, "smooth_quartz_slab")
    for (x, y) in ((-314, top + 2), (-313, top + 2), (-314, top + 3), (-313, top + 3)):
        p.raam(x, y, z2, True)
    p.set(-314, top + 1, z2, trim)
    p.set(-313, top + 1, z2, trim)
    # dormers in the front roof
    for xd in (x1 + 3, x2 - 3):
        zd = z2 - 1
        p.fill(xd - 1, top + 2, zd, xd + 1, top + 4, zd, "quartz_block")
        p.raam(xd, top + 3, zd + 0, True)
        p.set(xd, top + 2, zd, "quartz_block")
        for x in (xd - 1, xd, xd + 1):
            p.trap(x, top + 5, zd, dak, "south")
        p.fill(xd - 1, top + 2, zd - 1, xd + 1, top + 4, zd - 1, "air")
    # the chimney above the hearth (west wall), and one at the east
    hz = (z1 + z2) // 2
    for cx, cz, h in ((x1, hz, top + 10), (x2, z1 + 3, top + 7)):
        p.fill(cx, VLOER + 1, cz - 1, cx, h, cz + 1, "bricks") if cx == x1 else p.fill(cx, top, cz, cx, h, cz, "bricks")
        p.set(cx, h + 1, cz, g("guh_schoorsteen"), {"rookt": "true"})
    # windows: tall ones on the ground floor and a row above, front and back; side windows
    door = (-314, -313)
    for x in range(x1 + 2, x2 - 1, 3):
        for z in (z1, z2):
            if z == z2 and door[0] - 1 <= x <= door[1] + 1:
                continue
            for y in range(F + 1, F + 4):
                p.raam(x, y, z, True)
            p.set(x, F + 4, z, trim)
            for y in (F + 6, F + 7):
                p.raam(x, y, z, True)
            if z == z2:
                p.set(x, F + 1, z + 1, "air")
    for z in range(z1 + 3, z2 - 1, 4):
        for y in range(F + 1, F + 4):
            p.raam(x2, y, z, False)
    # front door: double, with a stoop, two lanterns and a wreath
    for x in door:
        p.deur(x, F + 1, z2, "south", "dark_oak", "left" if x == door[0] else "right")
        p.set(x, F + 3, z2, trim)
        p.trap(x, VLOER + 1, z2 + 1, "stone_brick_stairs", "north")
        p.pad.add((x, z2 + 1))
    for x in (door[0] - 2, door[1] + 2):
        p.set(x, F + 3, z2 + 1, "lantern", {"hanging": "false", "waterlogged": "false"})
        p.lichten.append((x, F + 3, z2 + 1, 15))
        p.set(x, F + 2, z2 + 1, "stone_brick_wall")
        p.set(x, VLOER + 1, z2 + 1, "stone_bricks")
    p.set(door[0], F + 4, z2 + 1, "spruce_leaves", {"distance": "7", "persistent": "true", "waterlogged": "false"})
    p.set(door[1], F + 4, z2 + 1, "spruce_leaves", {"distance": "7", "persistent": "true", "waterlogged": "false"})
    p.bord(door[0], F + 5, z2 + 1, "south", [("huis_1", "Huize Krijn", "The home of Krijn"), ("huis_2", "& Corine", "& Corine")])
    p.anker("huis_deur", door[0] + 1.0, VLOER + 1, z2 + 2.5, YAW["north"])
    p.anker("npc_krijn", door[1] + 2.5, VLOER + 1, z2 + 2.5, YAW["south"])
    # --- inside -------------------------------------------------------------------------------------------------------
    # the hearth on the west wall: an open fire in a brick surround, a mantel with candles, the shoe spot
    hx = x1 + 1
    for z in range(hz - 2, hz + 3):
        for y in range(F + 1, F + 5):
            p.set(hx, y, z, "bricks")
    for z in range(hz - 1, hz + 2):
        p.set(hx, F + 1, z, "air")
        p.set(hx, F + 2, z, "air")
        p.set(x1, F + 1, z, "bricks")
    p.set(hx, F + 1, hz, "campfire", {"facing": "east", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for z in range(hz - 2, hz + 3):
        p.trap(hx + 1, F + 4, z, "quartz_stairs", "west", "top")
    for z in (hz - 2, hz + 2):
        p.set(hx + 1, F + 5, z, "candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    p.set(hx + 1, F + 5, hz, g("sterrenlantaarn"))
    p.anker("huis_haard", hx + 1.5, F + 1, hz + 0.5, YAW["west"])
    p.anker("schoorsteen_schoen", hx + 1 + 0.5, F + 1, hz - 1 + 0.5, YAW["west"])
    p.set(hx + 1, F + 1, hz - 1, "air")
    p.pad.add((hx + 1, hz - 1))
    # two armchairs facing the fire, a rug, a side table with a lamp
    for z in range(hz - 2, hz + 3):
        for x in range(hx + 2, hx + 6):
            p.set(x, F + 1, z, "red_carpet" if (x + z) % 2 else "white_carpet")
    p.set(hx + 4, F + 1, hz - 2, g("bieb_guhfauteuil"), {"facing": "west"})
    p.set(hx + 4, F + 1, hz + 2, g("bieb_guhfauteuil"), {"facing": "west"})
    p.anker("npc_marrie", hx + 4.5, F + 1, hz - 2 + 0.5, YAW["west"])
    p.anker("npc_wijnand", hx + 4.5, F + 1, hz + 2 + 0.5, YAW["west"])
    p.set(hx + 4, F + 1, hz + 4, "spruce_fence")
    p.set(hx + 4, F + 2, hz + 4, "lantern", {"hanging": "false", "waterlogged": "false"})
    p.lichten.append((hx + 4, F + 2, hz + 4, 15))
    p.set(x1 + 1, F + 1, z1 + 1, "bookshelf")
    p.set(x1 + 1, F + 2, z1 + 1, "bookshelf")
    p.set(x1 + 2, F + 1, z1 + 1, "bookshelf")
    p.set(x1 + 2, F + 2, z1 + 1, "bookshelf")
    # the long dining table for 12 (east-west), with gourmet sets and candles
    tx1, tx2 = x1 + 9, x1 + 14
    tz = hz
    for x in range(tx1, tx2 + 1):
        for z in (tz - 1, tz):
            p.plaat(x, F + 1, z, "dark_oak_slab", "top")
    i = 0
    for x in range(tx1, tx2 + 1):
        p.set(x, F + 1, tz - 2, g("guh_stoel"), {"facing": "south"})
        p.anker(f"huis_tafel_{i}", x + 0.5, F + 1, tz - 2 + 0.5, YAW["south"])
        i += 1
    for x in range(tx1, tx2 + 1):
        p.set(x, F + 1, tz + 1, g("guh_stoel"), {"facing": "north"})
        p.anker(f"huis_tafel_{i}", x + 0.5, F + 1, tz + 1 + 0.5, YAW["north"])
        i += 1
    # on the table: a gourmet grill (iron plate) for every four, little pans (gold plates) and candles in between
    for k, x in enumerate(range(tx1, tx2 + 1)):
        for z in (tz - 1, tz):
            if k % 3 == 1:
                p.set(x, F + 2, z, "heavy_weighted_pressure_plate", {"power": "0"})
            elif (k + (z == tz)) % 2 == 0:
                p.set(x, F + 2, z, "light_weighted_pressure_plate", {"power": "0"})
            else:
                p.set(x, F + 2, z, "white_candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    p.anker("huis_tafel_midden", (tx1 + tx2 + 1) / 2.0, F + 2, tz + 0.0, 0.0)
    # chandeliers above the table
    for x in (tx1 + 1, tx2 - 1):
        p.set(x, top - 1, tz, "iron_chain", {"axis": "y", "waterlogged": "false"})
        p.set(x, top - 2, tz, "lantern", {"hanging": "true", "waterlogged": "false"})
        p.lichten.append((x, top - 2, tz, 15))
    # the big Christmas tree in the east part (7 high), presents underneath, a star on top
    kx, kz = x2 - 4, hz
    boom(p, kx, F + 1, kz)
    p.anker("kerstboom", kx + 0.5, F + 1, kz + 0.5, YAW["west"])               # the base centre of the tree (tekenen)
    p.anker("kerstboom_voor", kx - 4 + 0.5, F + 1, kz + 0.5, YAW["east"])       # where you stand to look at it
    # a sideboard with plants and a cosy lamp at the back wall, Corine near the table, a coat rack by the door
    for x in range(tx1, tx2 + 1):
        p.set(x, F + 1, z1 + 1, "spruce_planks" if x in (tx1, tx2) else "barrel", {"facing": "up", "open": "false"} if x not in (tx1, tx2) else None)
    p.set(tx1 + 1, F + 2, z1 + 1, "potted_spruce_sapling")
    p.set(tx1 + 3, F + 2, z1 + 1, "candle", {"candles": "4", "lit": "true", "waterlogged": "false"})
    p.set(tx2 - 1, F + 2, z1 + 1, "potted_red_tulip")
    p.anker("npc_corine", tx2 + 1.5, F + 1, tz + 3.5, YAW["west"])
    # hanging lamps over the room and lights in the corners
    for (x, z) in ((x1 + 4, z1 + 3), (x1 + 4, z2 - 3), (x2 - 9, z2 - 2), (x2 - 9, z1 + 2)):
        p.set(x, top - 1, z, "iron_chain", {"axis": "y", "waterlogged": "false"})
        p.set(x, top - 2, z, "lantern", {"hanging": "true", "waterlogged": "false"})
        p.lichten.append((x, top - 2, z, 15))
    p.set(x2 - 1, F + 1, z2 - 1, "potted_spruce_sapling")
    p.set(x1 + 1, F + 1, z2 - 1, "potted_spruce_sapling")


def boom(p, x, y, z, hoogte=8):
    """The indoor Christmas tree: a cone of spruce needles with baubles and lights, presents under it, a star on top."""
    rng = random.Random(25)
    naald = {"distance": "1", "persistent": "true", "sneeuw": "false", "waterlogged": "false"}
    ballen = ["red_wool", "yellow_wool", g("lampion_roze"), "light_blue_wool", "red_wool", g("lampion_geel")]
    p.set(x, y, z, "spruce_log", {"axis": "y"})
    for dy in range(1, hoogte):
        yy = y + dy
        r = (hoogte - dy) * 0.42 + 0.3
        ri = int(math.ceil(r))
        for dx in range(-ri, ri + 1):
            for dz in range(-ri, ri + 1):
                d2 = dx * dx + dz * dz
                if d2 > r * r + 0.2:
                    continue
                if (dx, dz) == (0, 0):
                    p.set(x, yy, z, "spruce_log", {"axis": "y"})
                    continue
                rand_ = d2 > (r - 1) * (r - 1)
                if rand_ and rng.random() < 0.13:
                    b = rng.choice(ballen)
                    if b.startswith("guhs:lampion"):
                        p.set(x + dx, yy, z + dz, b, {"hanging": "false", "waterlogged": "false"})
                        p.lichten.append((x + dx, yy, z + dz, 15))
                    else:
                        p.set(x + dx, yy, z + dz, b)
                else:
                    p.set(x + dx, yy, z + dz, g("sneeuwguhspar_naalden"), naald)
    p.set(x, y + hoogte, z, g("sterrenlantaarn"))
    p.lichten.append((x, y + hoogte, z, 15))
    # presents
    for (dx, dz, c, l) in ((-2, 2, "red_wool", "white_carpet"), (-3, -1, "lime_wool", "red_carpet"), (2, -2, "light_blue_wool", "yellow_carpet"),
                           (-2, -2, "yellow_wool", "red_carpet"), (2, 2, "magenta_wool", "white_carpet")):
        if p.get(x + dx, y, z + dz) in (None, "minecraft:air", g("sneeuwguhspar_naalden")):
            p.set(x + dx, y, z + dz, c)
            p.set(x + dx, y + 1, z + dz, l) if p.leeg(x + dx, y + 1, z + dz) else None


# --- the skating pond with its walking route -------------------------------------------------------------------------
def vijver(p):
    cx, cz, rx, rz = VIJVER
    for x in range(int(cx - rx - 4), int(cx + rx + 5)):
        for z in range(int(cz - rz - 4), int(cz + rz + 5)):
            d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if d <= 1.0:
                p.set(x, VLOER - 1, z, "packed_ice")
                p.set(x, VLOER, z, g("polderijs"))
                p.pad.add((x, z))
            elif d <= 1.28:
                p.set(x, VLOER, z, "stone_bricks" if (x + z) % 2 else "polished_andesite")     # the rim
                p.pad.add((x, z))
            elif d <= 1.75:
                p.set(x, VLOER, z, g("tegel_visgraat"))                                         # the walking route
                p.pad.add((x, z))
    # route anchors (walking, around) and the skating route (on the ice): block centres, each looking at the next one
    def ring(prefix, n, sx, sz):
        pts = []
        for i in range(n):
            a = 2 * math.pi * i / n
            pts.append((math.floor(cx + sx * math.cos(a)) + 0.5, math.floor(cz + sz * math.sin(a)) + 0.5))
        for i, (x, z) in enumerate(pts):
            nx, nz = pts[(i + 1) % n]
            p.anker(f"{prefix}{i}", x, VLOER + 1, z, round(-math.degrees(math.atan2(nx - x, nz - z)), 1))
    ring("wandel_", 12, rx * 1.23 + 0.7, rz * 1.23 + 0.7)
    ring("vijver_route_", 8, rx * 0.68, rz * 0.68)
    p.anker("vijver_midden", cx + 0.5, VLOER + 1, cz + 0.5, 0.0)
    # (1.10.10 integration) spellen's start sign of the schaatsrondje: on the rim 2 rows north of the start (vijver_route_0),
    # its face to the ice
    rij = math.floor(cz) - 2
    for x in range(int(cx), int(cx + rx + 5)):
        if 1.0 < ((x - cx) / rx) ** 2 + ((rij - cz) / rz) ** 2 <= 1.28:
            p.anker("schaats_start", x + 0.5, VLOER + 1, rij + 0.5, YAW["west"])
            break
    # benches, lanterns and snowy spruces around it; the family's spots; a little warm-chocolate tent
    for a_deg in (20, 110, 200, 290):
        a = math.radians(a_deg)
        x = int(round(cx + (rx * 1.23 + 3.2) * math.cos(a)))
        z = int(round(cz + (rz * 1.23 + 3.2) * math.sin(a)))
        if X1 + 2 <= x <= X2 - 2 and Z1 + 2 <= z <= Z2 - 2:
            p.paal(x, VLOER + 1, z)
    for (x, z) in ((-298, -731), (-279, -731), (-278, -712), (-298, -710)):
        p.spar(x, VLOER + 1, z, 7, gezicht="south" if z > -720 else None)
    p.bankje(-290, VLOER + 1, -733, "south")
    p.bankje(-287, VLOER + 1, -733, "south")
    p.anker("npc_daan", math.floor(cx - rx - 1) + 0.5, VLOER + 1, cz + 3.5, YAW["east"])
    p.anker("npc_kim", math.floor(cx - rx - 1) + 0.5, VLOER + 1, cz + 5.5, YAW["east"])
    p.anker("npc_finn", cx + 1.5, VLOER + 1, cz - 2.5, YAW["east"])
    p.anker("npc_liv", math.floor(cx - rx + 1) + 0.5, VLOER + 1, cz + 3.5, YAW["east"])
    # Liv's sled, parked on the ice next to her spot (two spruce trapdoors)
    for x in (-295, -294):
        p.set(x, VLOER + 1, -719, "spruce_trapdoor", {"facing": "east", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"})
    p.pad.add((-295, -719))
    p.pad.add((-294, -719))
    p.anker("slee", -294.0, VLOER + 1, -718.5, YAW["east"])
    p.anker("highscorebord_schaats", -299 + 0.5, VLOER + 1, -714 + 0.5, YAW["east"])
    p.set(-300, VLOER + 1, -714, "spruce_planks")
    p.set(-300, VLOER + 2, -714, "spruce_planks")
    p.set(-300, VLOER + 3, -714, "spruce_slab", {"type": "bottom", "waterlogged": "false"})
    p.bord(-299, VLOER + 3, -714, "east", [("schaats_1", "Schaatsrondje", "Skating lap"), ("schaats_2", "snelste guhs", "fastest guhs")])


# --- market stalls ---------------------------------------------------------------------------------------------------
KRAAM_NAMEN = [("winkel", "Pepernoten-", "Pepernoot", "winkeltje", "shop", "red"),
               ("oliebollen", "Oliebollen-", "Oliebollen", "kraam", "stall", "yellow"),
               ("chocomel", "Warme", "Hot", "chocomel", "chocolate", "brown"),
               ("snoep", "Snoep &", "Sweets &", "speculaas", "speculaas", "light_blue")]


def kraam(p, z1, i):
    x1, x2 = KRAAM_X
    z2 = z1 + 4
    key, nl1, en1, nl2, en2, kleur = KRAAM_NAMEN[i]
    # back wall and sides
    for z in range(z1, z2 + 1):
        for y in range(VLOER + 1, VLOER + 4):
            p.set(x1, y, z, "spruce_planks")
    for x in (x1, x2):
        for z in (z1, z2):
            p.fill(x, VLOER + 1, z, x, VLOER + 3, z, "spruce_fence" if x == x2 else "spruce_log")
    # counter along the front
    for z in range(z1 + 1, z2):
        p.set(x2, VLOER + 1, z, "barrel", {"facing": "east", "open": "false"})
        p.plaat(x2, VLOER + 2, z, "spruce_slab")
    # striped awning, sloping out to the front
    for x in range(x1, x2 + 2):
        for z in range(z1, z2 + 1):
            wol = f"{kleur}_wool" if z % 2 else "white_wool"
            y = VLOER + 4 if x <= x2 - 1 else VLOER + 4
            if x == x2 + 1:
                p.plaat(x, VLOER + 4, z, "spruce_slab")
            else:
                p.set(x, y, z, wol)
    for z in range(z1, z2 + 1):
        p.set(x1 + 1, VLOER + 5, z, f"{kleur}_wool" if z % 2 else "white_wool")
        p.set(x1 + 2, VLOER + 5, z, f"{kleur}_wool" if z % 2 else "white_wool")
    # name sign on the awning
    p.bord(x2, VLOER + 5, z1 + 2, "east", [(f"kraam_{key}_1", nl1, en1), (f"kraam_{key}_2", nl2, en2)])
    # lights and goods
    p.set(x1 + 2, VLOER + 3, z1 + 2, "lantern", {"hanging": "true", "waterlogged": "false"})
    p.lichten.append((x1 + 2, VLOER + 3, z1 + 2, 15))
    for z in range(z1 + 1, z2):
        p.set(x1, VLOER + 2, z, "spruce_planks")
    for k, z in enumerate(range(z1 + 1, z2)):
        goed = [["candle", "potted_spruce_sapling", "candle"], ["cake", "cake", "cake"], ["decorated_pot", "flower_pot", "decorated_pot"],
                ["cake", "candle", "cake"]][i][k]
        if goed == "candle":
            p.set(x2, VLOER + 3, z, "red_candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
        elif goed == "cake":
            p.set(x2, VLOER + 3, z, "cake", {"bites": "0"})
        elif goed == "decorated_pot":
            p.set(x2, VLOER + 3, z, "decorated_pot", {"facing": "east", "cracked": "false", "waterlogged": "false"})
        else:
            p.set(x2, VLOER + 3, z, goed)
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 2):
            p.pad.add((x, z))
    keeper = (x1 + 2 + 0.5, VLOER + 1, z1 + 2 + 0.5, YAW["east"])
    klant = (x2 + 2 + 0.5, VLOER + 1, z1 + 2 + 0.5, YAW["west"])
    return key, keeper, klant


def kramen(p):
    for i, z1 in enumerate(KRAMEN):
        key, keeper, klant = kraam(p, z1, i)
        if key == "winkel":
            p.anker("winkel", *keeper)
            # (1.10.10 integration) spellen puts its winkelkraam block ON the keeper spot: the winkelguh stands beside it
            p.anker("npc_winkelguh", keeper[0], keeper[1], keeper[2] - 1, keeper[3])
            p.anker("winkel_klant", *klant)
        elif key == "oliebollen":
            p.anker("oliebollenkraam", *keeper)
            p.anker("oliebollenkraam_klant", *klant)
        else:
            p.anker(f"kraam_{key}", *keeper)
    # lanterns between the stalls
    for z in (-716, -710, -704, -698, -692):
        if Z1 < z < Z2:
            p.paal(KRAAM_X[1] + 3, VLOER + 1, z)


# --- the arcade corner -----------------------------------------------------------------------------------------------
def arcade(p):
    """A small roofed booth by the back street; the cabinet (spellen) stands at 'arcadekast', the race board next to it."""
    x1, x2, z1, z2 = -311, -305, -692, -688
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            p.set(x, VLOER, z, g("tegel_dambord"))
            p.pad.add((x, z))
    for z in range(z1, z2 + 1):
        p.fill(x1, VLOER + 1, z, x1, VLOER + 3, z, "pink_concrete" if z % 2 else "white_concrete")
    for x in range(x1, x2 + 1):
        p.fill(x, VLOER + 1, z1, x, VLOER + 3, z1, "pink_concrete" if x % 2 else "white_concrete")
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            p.set(x, VLOER + 4, z, "pink_wool" if (x + z) % 2 else "magenta_wool")
    p.set(x2, VLOER + 1, z2, "spruce_fence")
    p.set(x2, VLOER + 2, z2, "spruce_fence")
    p.set(x2, VLOER + 3, z2, "spruce_fence")
    p.set(x1, VLOER + 1, z2, "pink_concrete")
    p.bord(x1 + 3, VLOER + 4, z2 + 1, "south", [("arcade_1", "Speelhoekje", "Play corner"), ("arcade_2", "Pepernotenrace", "Pepernoot Race")])
    p.set(x1 + 3, VLOER + 3, z1 + 2, "lantern", {"hanging": "true", "waterlogged": "false"})
    p.lichten.append((x1 + 3, VLOER + 3, z1 + 2, 15))
    p.anker("arcadekast", x1 + 2 + 0.5, VLOER + 1, z1 + 1 + 0.5, YAW["south"])
    p.anker("highscorebord_race", x1 + 4 + 0.5, VLOER + 2, z1 + 1 + 0.5, YAW["south"])
    p.bankje(x2 - 1, VLOER + 1, z1 + 1, "west", "roze")


# --- the Sint's kade: chair, Het Grote Boek, the steamboat -----------------------------------------------------------
def sintkade(p):
    # podium with a red carpet, facing west (to the bridge and the square)
    px1, px2, pz1, pz2 = -281, -277, -682, -675
    for x in range(px1, px2 + 1):
        for z in range(pz1, pz2 + 1):
            p.set(x, VLOER + 1, z, "red_wool" if px1 < x < px2 and pz1 < z < pz2 else "spruce_planks")
            p.pad.add((x, z))
    for z in range(pz1 + 1, pz2):
        p.trap(px1 - 1, VLOER + 1, z, "spruce_stairs", "east")
        p.pad.add((px1 - 1, z))
    # the chair: dark oak with a tall red and gold back
    sx, sz = px2 - 1, (pz1 + pz2) // 2
    p.trap(sx, VLOER + 2, sz, "dark_oak_stairs", "east")
    p.fill(sx + 1, VLOER + 2, sz - 1, sx + 1, VLOER + 4, sz + 1, "red_wool")
    p.set(sx + 1, VLOER + 5, sz, "gold_block")
    p.set(sx + 1, VLOER + 2, sz - 1, "gold_block")
    p.set(sx + 1, VLOER + 2, sz + 1, "gold_block")
    p.set(sx, VLOER + 2, sz - 1, "dark_oak_fence")
    p.set(sx, VLOER + 2, sz + 1, "dark_oak_fence")
    p.set(sx, VLOER + 3, sz - 1, "red_carpet")
    p.set(sx, VLOER + 3, sz + 1, "red_carpet")
    p.anker("sint_stoel", sx + 0.5, VLOER + 2, sz + 0.5, YAW["west"])
    p.anker("npc_sint", sx + 0.5, VLOER + 2, sz + 0.5, YAW["west"])
    # Het Grote Boek on a lectern beside him
    p.set(sx - 1, VLOER + 2, sz - 3, "lectern", {"facing": "west", "has_book": "false", "powered": "false"})
    p.anker("grote_boek", sx - 1 + 0.5, VLOER + 2, sz - 3 + 0.5, YAW["west"])
    # lights and banners around the podium
    for (x, z) in ((px1, pz1), (px1, pz2), (px2, pz1), (px2, pz2)):
        p.set(x, VLOER + 2, z, "spruce_fence")
        p.set(x, VLOER + 3, z, "spruce_fence")
        p.set(x, VLOER + 4, z, "lantern", {"hanging": "false", "waterlogged": "false"})
        p.lichten.append((x, VLOER + 4, z, 15))
    for z in range(pz1 + 1, pz2):
        p.set(px2 + 1, VLOER + 1, z, "stone_bricks") if px2 + 1 < X2 else None
    # Rins and Anthony at the kade, a bench for the visitors
    p.anker("npc_rins", -289 + 0.5, VLOER + 1, GRACHT_Z[0] - 2 + 0.5, YAW["south"])
    p.anker("npc_anthony", -286 + 0.5, VLOER + 1, GRACHT_Z[0] - 2 + 0.5, YAW["south"])
    p.anker("kade_boot", -286 + 0.5, VLOER + 1, GRACHT_Z[0] - 2 + 0.5, YAW["south"])
    p.bankje(-292, VLOER + 1, -680, "south")
    p.bankje(-289, VLOER + 1, -680, "south")
    p.paal(-294, VLOER + 1, -677)
    p.paal(-283, VLOER + 1, -673)
    # the Pakjesguhs' spots
    for i, (x, z) in enumerate(((-284, -674), (-291, -674), (-282, -684), (-293, -683))):
        p.anker(f"npc_pakjesguh_{i}", x + 0.5, VLOER + 1, z + 0.5, YAW["south"])
    # a pile of presents on the kade
    for (x, z, c, l) in ((-279, -673, "red_wool", "yellow_carpet"), (-278, -673, "lime_wool", "red_carpet"),
                         (-279, -674, "light_blue_wool", "white_carpet"), (-278, -674, "magenta_wool", "lime_carpet")):
        p.set(x, VLOER + 1, z, c)
        p.set(x, VLOER + 2, z, l)
    p.set(-278, VLOER + 2, -673, "yellow_wool")
    p.set(-278, VLOER + 3, -673, "red_carpet")


def boot(p):
    """The Pakjesboot: a pink and red steamboat with a Sint flag, moored in the basin (bow to the east)."""
    bx1, bx2 = -292, -279
    zc = -667
    hw = {}
    for x in range(bx1, bx2 + 1):
        d = bx2 - x
        hw[x] = 0 if d == 0 else (1 if d <= 2 else 2)
        if x == bx1:
            hw[x] = 2
    for x in range(bx1, bx2 + 1):
        w = hw[x]
        for z in range(zc - w, zc + w + 1):
            edge = abs(z - zc) == w or x == bx1
            p.set(x, VLOER - 2, z, "red_concrete")
            p.set(x, VLOER - 1, z, "red_concrete" if edge else "spruce_planks")
            if edge:
                p.set(x, VLOER, z, "pink_concrete")
                p.set(x, VLOER + 1, z, "white_concrete" if x != bx2 else "pink_concrete")
            else:
                p.set(x, VLOER, z, "spruce_planks")
                p.set(x, VLOER + 1, z, "air")
            p.pad.add((x, z))
    p.set(bx2 + 1, VLOER + 1, zc, "spruce_fence")              # bowsprit
    p.set(bx2 + 1, VLOER, zc, "pink_concrete") if bx2 + 1 <= HAVEN_X[1] else None
    # the cabin with round windows
    cx1, cx2 = bx1 + 3, bx1 + 7
    for x in range(cx1, cx2 + 1):
        for z in range(zc - 1, zc + 2):
            for y in range(VLOER + 1, VLOER + 4):
                wand = x in (cx1, cx2) or z in (zc - 1, zc + 1)
                p.set(x, y, z, "white_concrete" if wand else "air")
            p.plaat(x, VLOER + 4, z, "red_concrete".replace("concrete", "nether_brick_slab").replace("red_nether_brick_slab", "red_nether_brick_slab"))
    for x in (cx1 + 1, cx1 + 3):
        for z in (zc - 1, zc + 1):
            p.set(x, VLOER + 2, z, g("patrijspoort_koper"), {"facing": "north" if z < zc else "south"})
    p.set(cx2, VLOER + 2, zc, "glass_pane")
    p.set(cx1 + 2, VLOER + 3, zc, "lantern", {"hanging": "true", "waterlogged": "false"})
    p.lichten.append((cx1 + 2, VLOER + 3, zc, 15))
    p.set(cx2, VLOER + 1, zc, "air")
    # the funnel: red with a white band, smoking
    fx = cx1 + 2
    for y in range(VLOER + 5, VLOER + 9):
        p.set(fx, y, zc, "white_concrete" if y == VLOER + 7 else "red_concrete")
    p.set(fx, VLOER + 9, zc, g("guh_schoorsteen"), {"rookt": "true"})
    # paddle box on the south side
    for y in range(VLOER - 1, VLOER + 2):
        for x in range(fx - 1, fx + 2):
            if y == VLOER + 1 and x != fx:
                continue
            p.set(x, y, zc + 3, "red_concrete" if y < VLOER + 1 else "pink_concrete")
    # masts with the Sint flag
    for mx in (bx1 + 1, bx2 - 3):
        p.fill(mx, VLOER + 1, zc, mx, VLOER + 6, zc, "spruce_fence")
        p.banier(mx, VLOER + 6, zc - 1, "red", [("yellow", "rhombus"), ("white", "border"), ("yellow", "circle")], facing="north")
        p.banier(mx, VLOER + 6, zc + 1, "red", [("yellow", "rhombus"), ("white", "border"), ("yellow", "circle")], facing="south")
    # lights strung from the masts, presents on the deck, the name board
    for x in range(bx1 + 2, bx2 - 3):
        if p.leeg(x, VLOER + 6, zc):
            p.set(x, VLOER + 6, zc, g("seizoensslinger"), {"axis": "x", "seizoen": "winter"})
    for (x, z, c) in ((bx2 - 2, zc, "red_wool"), (bx2 - 3, zc - 1, "lime_wool"), (bx2 - 4, zc + 1, "light_blue_wool"), (bx1 + 1, zc + 1, "yellow_wool"),
                      (bx2 - 5, zc, "magenta_wool")):
        p.set(x, VLOER + 1, z, c)
        p.set(x, VLOER + 2, z, "white_carpet" if c != "red_wool" else "yellow_carpet")
    p.bord(cx1 + 2, VLOER + 3, zc - 2, "north", [("boot_1", "Pakjesboot", "Present Boat"), ("boot_2", "Nr. 12", "No. 12")])
    # gangplank from the kade
    for z in (GRACHT_Z[0] - 1, GRACHT_Z[0]):
        if is_gracht(-286, z) and p.get(-286, VLOER, z) in (None, "minecraft:air"):
            p.plaat(-286, VLOER, z, "spruce_slab", "top")
            p.pad.add((-286, z))
    p.anker("boot_dek", cx2 + 2 + 0.5, VLOER + 1, zc + 0.5, YAW["east"])


# --- steps out of the water (playtest 1: players and guhs got stuck in the canal by the Pakjesboot) -------------------
# (water cell x, z) -> (kade edge cell x, z), the way up: a stone brick step in the water (waterlogged) and a polished
# andesite step cut into the kade edge, so you walk out in two half steps
TRAPJES = [((-282, -671), (-282, -672), "north"), ((-281, -671), (-281, -672), "north"),      # by the Pakjesboot (kade_boot)
           ((-294, -664), (-295, -664), "west"), ((-294, -663), (-295, -663), "west"),        # south-west corner of the basin
           ((-337, -670), (-337, -671), "north"), ((-336, -670), (-336, -671), "north"),      # the canal, north side
           ((-313, -670), (-313, -671), "north"), ((-312, -670), (-312, -671), "north"),
           ((-345, -666), (-345, -665), "south"), ((-344, -666), (-344, -665), "south"),      # the canal, south side
           ((-318, -666), (-318, -665), "south"), ((-317, -666), (-317, -665), "south")]


def trapjes(p):
    for (xw, zw), (xk, zk), richting in TRAPJES:
        dx, dz = DIRS[richting]
        if p.get(xw, VLOER - 1, zw) != "minecraft:water" or not p.leeg(xw, VLOER, zw) or not p.leeg(xw, VLOER + 1, zw):
            raise SystemExit(f"guhwinter_plein: step {xw},{zw} is not open water")
        if p.get(xk, VLOER, zk) != "minecraft:polished_andesite" or not p.leeg(xk, VLOER + 1, zk) or (xk - xw, zk - zw) != (dx, dz):
            raise SystemExit(f"guhwinter_plein: step {xk},{zk} is not a free kade edge")
        if not (p.leeg(xk + dx, VLOER + 1, zk + dz) or p.get(xk + dx, VLOER + 1, zk + dz) == "minecraft:snow") or not p.leeg(xk + dx, VLOER + 2, zk + dz):
            raise SystemExit(f"guhwinter_plein: nowhere to step off at {xk + dx},{zk + dz}")
        p.set(xw, VLOER - 1, zw, "stone_brick_stairs", {"facing": richting, "half": "bottom", "shape": "straight", "waterlogged": "true"})
        p.trap(xk, VLOER, zk, "polished_andesite_stairs", richting)
        p.pad.add((xk, zk))


# --- the Surprisestraatje: 8 empty build plots ----------------------------------------------------------------------------
def straatje(p):
    i = 0
    for vx in VELD_X:
        oost = vx > STRAAT_X[1]                  # the east row: its gate faces west, onto the street
        for vz in VELD_Z:
            # the plot: snow floor, 7 x 7 x 7 room, low fence around with a gate to the street
            for x in range(vx, vx + 7):
                for z in range(vz, vz + 7):
                    p.set(x, VLOER, z, "snow_block")
                    for y in range(VLOER + 1, VLOER + 8):
                        p.set(x, y, z, "air")
                    p.pad.add((x, z))
            gx = vx - 1 if oost else vx + 7
            for x in range(vx - 1, vx + 8):
                for z in (vz - 1, vz + 7):
                    p.set(x, VLOER + 1, z, g("guh_hekje_wit"))
                    p.pad.add((x, z))
            for z in range(vz - 1, vz + 8):
                for x in (vx - 1, vx + 7):
                    p.set(x, VLOER + 1, z, g("guh_hekje_wit"))
                    p.pad.add((x, z))
            p.set(gx, VLOER + 1, vz + 3, g("guh_poortje_wit"), {"facing": "east" if oost else "west", "in_wall": "false", "open": "false", "powered": "false"})
            # path tile, lantern post and name sign on the street side
            sx = gx - 1 if oost else gx + 1
            p.set(sx, VLOER, vz + 3, g("tegel_hartjes"))
            p.paal(sx, VLOER + 1, vz + 1)
            p.set(sx, VLOER + 1, vz + 5, "spruce_fence")
            p.bord(sx, VLOER + 2, vz + 5, "west" if oost else "east", [], muur=False, waxed=False)
            p.anker(f"bouwveld_{i}", vx, VLOER + 1, vz, 0.0)
            p.anker(f"bouwveld_bord_{i}", sx, VLOER + 2, vz + 5, YAW["west" if oost else "east"])
            p.anker(f"bouwveld_poort_{i}", gx + 0.5, VLOER + 1, vz + 3 + 0.5, YAW["east" if oost else "west"])
            i += 1
    # the street's entrance arch with its name
    ax1, ax2, az = STRAAT_X[0] - 1, STRAAT_X[1] + 1, HUIZEN_Z[0] - 4
    for x in (ax1, ax2):
        p.fill(x, VLOER + 1, az, x, VLOER + 4, az, "spruce_log")
    for x in range(ax1, ax2 + 1):
        p.set(x, VLOER + 5, az, "spruce_planks")
        p.set(x, VLOER + 4, az, g("seizoensslinger"), {"axis": "x", "seizoen": "winter"}) if ax1 < x < ax2 else None
    p.bord(-338, VLOER + 5, az + 1, "south", [("straat_1", "Surprise-", "Surprise"), ("straat_2", "straatje", "Lane")])
    p.lantaarn(ax1, VLOER + 6, az)
    p.lantaarn(ax2, VLOER + 6, az)


# --- the square itself: prikbord, outdoor tree, lights, trees, benches, snowmen ---------------------------------------
def prikbord(p):
    x1, x2, z = -307, -305, -716
    for x in range(x1 - 1, x2 + 2):
        p.fill(x, VLOER + 1, z, x, VLOER + 1, z, "spruce_planks")
    for x in (x1 - 1, x2 + 1):
        p.fill(x, VLOER + 1, z, x, VLOER + 4, z, "spruce_log")
    for x in range(x1, x2 + 1):
        p.set(x, VLOER + 2, z, "brown_mushroom_block")
        p.set(x, VLOER + 3, z, "brown_mushroom_block")
    for x in range(x1 - 1, x2 + 2):
        p.trap(x, VLOER + 5, z, "spruce_stairs", "north")
    p.bord(-306, VLOER + 4, z + 1, "south", [("prik_1", "Prikbord", "Notice board"), ("prik_2", "goede voornemens", "New Year wishes")])
    p.anker("prikbord", -306 + 0.5, VLOER + 2, z + 1 + 0.5, YAW["north"])


def plein_midden(p):
    # an outdoor tree with lights in a round planter, benches around, lantern posts
    cx, cz = -313, -701
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            d = math.hypot(x - cx, z - cz)
            if d <= 4.4:
                p.set(x, VLOER, z, g("tegel_guhgezicht") if d > 3.2 else ("stone_bricks" if d > 1.5 else "grass_block"))
                if d <= 4.4:
                    p.pad.add((x, z)) if d > 1.5 else None
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            if (x, z) != (cx, cz):
                p.set(x, VLOER + 1, z, "stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    p.spar(cx, VLOER + 1, cz, 10, sneeuw=True, gezicht="south")
    rng = random.Random(7)
    for (x, y, z) in list(p.s.blocks.keys()):
        wx, wy, wz = x + p.mn[0], y + p.mn[1], z + p.mn[2]
        if p.get(wx, wy, wz) == g("sneeuwguhspar_naalden") and abs(wx - cx) <= 4 and abs(wz - cz) <= 4 and rng.random() < 0.12:
            p.lampion(wx, wy, wz, rng.choice(["roze", "geel", "mint"]), hangend=False)
    for (dx, dz, f) in ((0, -5, "north"), (0, 5, "south"), (-5, 0, "west"), (5, 0, "east")):
        p.bankje(cx + dx, VLOER + 1, cz + dz, f)
    for (dx, dz) in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        p.paal(cx + dx, VLOER + 1, cz + dz)
    p.anker("winterplein_reis", -299.5, VLOER + 1, Z2 - 3 + 0.5, YAW["south"])   # kern's Reisguh point: arrive just inside the gate
    p.anker("plein_midden", cx + 0.5, VLOER + 1, cz + 6.5, YAW["north"])


def muziektent(p, cx=-289, cz=-697):
    """A Dutch bandstand on the square: raised octagon, eight posts, a pink pan roof with a lantern on top."""
    def r(x, z):
        return max(abs(x - cx), abs(z - cz)) * 0.62 + (abs(x - cx) + abs(z - cz)) * 0.45
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = r(x, z)
            if d <= 4.6:
                p.set(x, VLOER + 1, z, "stone_bricks" if d > 3.7 else "spruce_planks")
                p.pad.add((x, z))
                for y in range(VLOER + 2, VLOER + 6):
                    p.set(x, y, z, "air")
    for (dx, dz) in ((-4, -2), (-4, 2), (4, -2), (4, 2), (-2, -4), (2, -4), (-2, 4), (2, 4)):
        p.fill(cx + dx, VLOER + 2, cz + dz, cx + dx, VLOER + 5, cz + dz, "spruce_fence")
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = r(x, z)
            if 3.7 < d <= 4.6 and p.leeg(x, VLOER + 2, z) and not (z > cz + 2 and abs(x - cx) <= 1):
                p.set(x, VLOER + 2, z, "spruce_fence")
    for x in range(cx - 1, cx + 2):
        p.trap(x, VLOER + 1, cz + 5, "spruce_stairs", "north")
        p.pad.add((x, cz + 5))
    for (y, lim, blok) in ((VLOER + 6, 5.2, "plaat"), (VLOER + 7, 3.9, "vol"), (VLOER + 8, 2.6, "vol"), (VLOER + 9, 1.3, "vol")):
        for x in range(cx - 6, cx + 7):
            for z in range(cz - 6, cz + 7):
                if r(x, z) <= lim:
                    if blok == "plaat" and r(x, z) > 3.9:
                        p.plaat(x, y, z, g("dakpan_roze_plaat"))
                    else:
                        p.set(x, y, z, g("dakpan_roze"))
    p.set(cx, VLOER + 10, cz, "spruce_fence")
    p.set(cx, VLOER + 11, cz, "lantern", {"hanging": "false", "waterlogged": "false"})
    p.lichten.append((cx, VLOER + 11, cz, 15))
    p.set(cx, VLOER + 5, cz, g("lampion_roze"), {"hanging": "true", "waterlogged": "false"})
    p.lichten.append((cx, VLOER + 5, cz, 15))
    p.anker("muziektent", cx + 0.5, VLOER + 2, cz + 0.5, YAW["south"])


def bloembakken(p):
    x1, z1, x2, z2 = HUIS
    for x in (-322, -320, -307, -305):
        p.set(x, HUISVLOER, z2 + 1, g("seizoensbloembak"), {"seizoen": "winter"})


def sneeuwpoppen(p):
    for (x, z, f) in ((-302, -704, "east"), (-283, -697, "west"), (-344, -690, "south")):
        p.set(x, VLOER + 1, z, g("sneeuwpopguh"), {"facing": f, "half": "lower"})
        p.set(x, VLOER + 2, z, g("sneeuwpopguh"), {"facing": f, "half": "upper"})
        p.pad.add((x, z))


def bomen_en_lichten(p):
    # snowy spruces along the walls and kades (1.10.11: the two by the north wall are gone: (-346, -732) stood inside plot 0
    # and (-329, -733) on plot 4's fence with its needles in the plot; see check_velden)
    for (x, z) in ((-346, -664), (-336, -663), (-326, -664), (-316, -663), (-306, -664),
                   (-347, -692), (-327, -688),
                   (-280, -688), (-293, -689), (-279, -702)):
        p.spar(x, VLOER + 1, z, 6 + (x * 7 + z) % 3, gezicht="north" if (x + z) % 3 == 0 else None)
    # their old spots stay free of snow, as on the live world where they were cleared (and so the snow noise, drawn in
    # visiting order, stays exactly as before everywhere else)
    for (x, z) in WEG_SPARREN:
        los = Plan(p.mn, p.s.size)
        los.spar(x, VLOER + 1, z, 6 + (x * 7 + z) % 3)
        p.pad |= {(lx + p.mn[0], lz + p.mn[2]) for (lx, _ly, lz) in los.s.blocks}
    # lantern posts along the kades and streets
    for x in range(-344, HAVEN_X[0] - 2, 8):
        p.paal(x, VLOER + 1, Z2 - 2)
        p.paal(x + 4, VLOER + 1, HUIZEN_Z[1] + 2)
    for (x, z) in ((-300, -689), (-288, -689), (-300, -706), (-319, -686), (-331, -686), (-297, -717)):
        p.paal(x, VLOER + 1, z)
    # garlands of lights over the house street
    for z in (HUIZEN_Z[1] + 0, HUIZEN_Z[0]):
        for x in range(BRUG_X[0], BRUG_X[1] + 1):
            p.set(x, VLOER + 5, z, g("seizoensslinger"), {"axis": "x", "seizoen": "winter"})
    # benches along the kade
    for x in (-340, -324, -310):
        p.bankje(x, VLOER + 1, Z2 - 4 + 0, "north")
    # the main guhs' spots
    for naam, (x, z, f) in {"joost": (-299, -686, "north"), "suus": (-298, -686, "north"), "liek": (-317, -696, "west"),
                            "lin": (-317, -697, "west"), "evi": (-305, -711, "south"), "niels": (-304, -711, "south")}.items():
        p.anker(f"npc_{naam}", x + 0.5, VLOER + 1, z + 0.5, YAW[f])


# --- snow -----------------------------------------------------------------------------------------------------------
def sneeuw(p):
    """Snow layers on the open paving, never where a lamp would melt it (block light > 11), never on paths."""
    rng = random.Random(1225)
    rooster = {}

    def hoek(i, j):
        if (i, j) not in rooster:
            rooster[(i, j)] = rng.random()
        return rooster[(i, j)]

    def ruis(x, z, cel=5):
        """Smooth value noise in 0..1: patches of thin and thicker snow instead of salt and pepper."""
        i, j = x // cel, z // cel
        u, v = (x % cel) / cel, (z % cel) / cel
        u, v = u * u * (3 - 2 * u), v * v * (3 - 2 * v)
        a = hoek(i, j) * (1 - u) + hoek(i + 1, j) * u
        b = hoek(i, j + 1) * (1 - u) + hoek(i + 1, j + 1) * u
        return a * (1 - v) + b * v
    licht = {}
    for (lx, ly, lz, lv) in p.lichten:
        r = lv - 11
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                d = abs(dx) + abs(dz) + abs(VLOER + 1 - ly)
                if lv - d > 11:
                    licht[(lx + dx, lz + dz)] = True
    for x in range(X1 + 1, X2):
        for z in range(Z1 + 1, Z2):
            if (x, z) in p.pad or (x, z) in licht:
                continue
            onder = p.get(x, VLOER, z)
            if onder not in (g("knuffelklinkers"), "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:grass_block"):
                continue
            if not p.leeg(x, VLOER + 1, z):
                continue
            boven = any(not p.leeg(x, y, z) for y in range(VLOER + 2, Y0 + HOOGTE))
            if boven:
                continue
            r = ruis(x, z)
            if r > 0.18:
                p.set(x, VLOER + 1, z, "snow", {"layers": "2" if r > 0.72 else "1"})
    # snow caps on the wall posts and hedges
    for x in range(X1, X2 + 1):
        for z in (Z1, Z2):
            if p.get(x, VLOER + 2, z) == "minecraft:spruce_leaves" and p.leeg(x, VLOER + 3, z) and (x, z) not in licht:
                p.set(x, VLOER + 3, z, "snow", {"layers": "1"})


def winter():
    p = Plan(PLEIN_MIN, (X2 - X1 + 1, HOOGTE, Z2 - Z1 + 1))
    paving(p)
    rand(p, True)
    brug(p)
    panden(p)
    huis(p)
    vijver(p)
    kramen(p)
    arcade(p)
    sintkade(p)
    boot(p)
    trapjes(p)
    straatje(p)
    prikbord(p)
    plein_midden(p)
    sneeuwpoppen(p)
    muziektent(p)
    bloembakken(p)
    bomen_en_lichten(p)
    # hiding spots for the presents (overboard) and the pepernoten (playtest 1: pakje 0 lay under the spruce at -346,-664,
    # now beside the lamp post; pepernoot 4 lay under the spruce at -327,-688, now at its foot; see check_verstop)
    pakjes = [(-342, VLOER + 1, -663), (-331, VLOER + 1, -672), (-296, VLOER + 1, -684), (-277, VLOER + 1, -686),
              (-349, VLOER + 1, -686), (-301, VLOER + 1, -733), (-325, VLOER + 1, -717)]
    for i, (x, y, z) in enumerate(pakjes):
        p.anker(f"pakje_verstop_{i}", x + 0.5, y, z + 0.5, 0.0)
    pepers = [(-349, -734), (-277, -734), (-277, -662), (-349, -662), (-325, -685), (-302, -692), (-318, -713), (-283, -706),
              (-296, -722), (-280, -726), (-323, -718), (-342, -694), (-332, -694), (-305, -681), (-292, -678), (-310, -689)]
    # (1.10.10 integration) these spots lie on a raised floor: one block higher. (1.10.11: none left; -305,-681 is inside
    # canal house 5, whose ground floor is now level with the street, so it stands at VLOER + 1 like the others)
    hoger = set()
    for i, (x, z) in enumerate(pepers):
        p.anker(f"pepernoot_verstop_{i}", x + 0.5, VLOER + (2 if (x, z) in hoger else 1), z + 0.5, 0.0)
    sneeuw(p)
    check_verstop(p)
    check_deuren(p)
    check_velden(p)
    return p


def check_verstop(p):
    """Every hiding spot on the square stands in the open: nothing but air (or a snow layer) in it and two blocks above it."""
    for k, a in p.ankers.items():
        if not k.startswith(("pakje_verstop_", "pepernoot_verstop_")):
            continue
        x, y, z = math.floor(a["x"]), int(a["y"]), math.floor(a["z"])
        for yy in range(y, y + 3):
            b = p.get(x, yy, z)
            if not (b in (None, "minecraft:air") or (yy == y and b == "minecraft:snow")):
                raise SystemExit(f"guhwinter_plein: hiding spot {k} at {x},{y},{z} is not open ({b} at y {yy})")


def check_velden(p):
    """(1.10.11) The 8 lootjes plots are clean: the snow floor whole, nothing at all in the 7x7 room from its floor up to the
    template's top (a Surprise captures the 7x7x7 and a builder may only change that), and the fence ring whole (hekjes and
    one gate). Playtest: two deco spruces stood in plots 0 and 4. (Needles resting on top of a fence are allowed: the spruce
    at -347,-692 touches plot 3's south fence, outside its room.)"""
    top = Y0 + HOOGTE - 1
    for i in range(len(VELD_X) * len(VELD_Z)):
        a = p.ankers[f"bouwveld_{i}"]
        vx, vy, vz = int(a["x"]), int(a["y"]), int(a["z"])
        fout = []
        for x in range(vx - 1, vx + 8):
            for z in range(vz - 1, vz + 8):
                ring = x in (vx - 1, vx + 7) or z in (vz - 1, vz + 7)
                if not ring and p.get(x, vy - 1, z) != "minecraft:snow_block":
                    fout.append(f"floor {x},{vy - 1},{z} is {p.get(x, vy - 1, z)}")
                if ring and p.get(x, vy, z) not in (g("guh_hekje_wit"), g("guh_poortje_wit")):
                    fout.append(f"fence {x},{vy},{z} is {p.get(x, vy, z)}")
                for y in ([] if ring else range(vy, top + 1)):
                    if not p.leeg(x, y, z):
                        fout.append(f"{p.get(x, y, z)} at {x},{y},{z}")
        poorten = sum(1 for x in range(vx - 1, vx + 8) for z in range(vz - 1, vz + 8) if p.get(x, vy, z) == g("guh_poortje_wit"))
        if poorten != 1:
            fout.append(f"{poorten} gates")
        if fout:
            raise SystemExit(f"guhwinter_plein: plot bouwveld_{i} is not clean: " + "; ".join(fout[:8]) + (" ..." if len(fout) > 8 else ""))


# what you can walk through (in a door's cell or the two cells above a floor); None = nothing in the template = cleared air
DOORLOOPBAAR = ("minecraft:air", "minecraft:snow", "minecraft:light_weighted_pressure_plate", "minecraft:heavy_weighted_pressure_plate")


def _doorloopbaar(b):
    return b is None or b in DOORLOOPBAAR or b.endswith("_carpet")


def _vloer(p, x, y, z):
    """The block at (x, y, z) is something you stand on, its top at about y + 1 (a full block, a stair, a top slab)."""
    b = p.s.blocks.get(p._l(x, y, z))
    if b is None or _doorloopbaar(b[0]) or b[0] in ("minecraft:water", "minecraft:lava"):
        return False
    if b[0].endswith("_slab"):
        return b[1].get("type") in ("top", "double")
    return not b[0].endswith(("_door", "_fence", "_wall", "_pane", "_sign", "_banner", "_trapdoor")) and "lantern" not in b[0]


def check_deuren(p):
    """(1.10.11) Every door you can walk through: the block under it, and on both sides (in front and behind) a floor at
    the door's sill level with two free blocks above it. Playtest: the canal houses' floors were one block above the doors."""
    n = 0
    for (lx, ly, lz), (naam, props, _nbt) in list(p.s.blocks.items()):
        if not naam.endswith("_door") or props.get("half") != "lower":
            continue
        x, y, z = lx + p.mn[0], ly + p.mn[1], lz + p.mn[2]
        dx, dz = DIRS[props["facing"]]
        fouten = []
        if not _vloer(p, x, y - 1, z):
            fouten.append(f"nothing solid under the door ({p.get(x, y - 1, z)})")
        for kant, (sx, sz) in (("front", (x + dx, z + dz)), ("back", (x - dx, z - dz))):
            if not _vloer(p, sx, y - 1, sz):
                fouten.append(f"{kant}: no floor at the sill level ({p.get(sx, y - 1, sz)} at y {y - 1})")
            for yy in (y, y + 1):
                if not _doorloopbaar(p.get(sx, yy, sz)):
                    fouten.append(f"{kant}: {p.get(sx, yy, sz)} at y {yy}")
        if fouten:
            raise SystemExit(f"guhwinter_plein: the door at {x},{y},{z} is not walk-through: " + "; ".join(fouten))
        n += 1
    return n


def raam():
    """The empty frame: same wall and gate, a flat plain square (no snow), four lamp posts."""
    p = Plan(PLEIN_MIN, (X2 - X1 + 1, HOOGTE, Z2 - Z1 + 1))
    for x in range(X1 + 1, X2):
        for z in range(Z1 + 1, Z2):
            p.set(x, VLOER - 1, z, "pink_wool")
            d = max(abs(x - (X1 + X2) / 2), abs(z - (Z1 + Z2) / 2))
            p.set(x, VLOER, z, "smooth_stone" if int(d) % 6 == 0 else g("knuffelklinkers"))
    rand(p, False)
    for (x, z) in ((-330, -715), (-296, -715), (-330, -681), (-296, -681)):
        p.paal(x, VLOER + 1, z)
    return p


# =====================================================================================================================
# The arrival around the new portal (outside the square): paths, benches, lanterns, flowers. Cheap and breakable only.
# =====================================================================================================================
AANKOMST_BLOKKEN = ("stone_bricks", "stone_brick_slab", "stone_brick_stairs", "stone_brick_wall", "cobblestone", "mossy_cobblestone",
                    "andesite", "polished_andesite", "gravel", "spruce_planks", "spruce_slab", "spruce_stairs", "spruce_fence", "spruce_log",
                    "oak_planks", "oak_slab", "oak_fence", "lantern", "water", "lily_pad", "pink_wool", "dirt", "grass_block",
                    "poppy", "dandelion", "azure_bluet", "oxeye_daisy", "pink_tulip", "allium", "cornflower", "short_grass",
                    "guhs:guh_lantaarnpaal", "guhs:guh_bankje_hout", "guhs:roze_guhbloem", "guhs:knuffelklinkers", "air")


def aankomst():
    p = Plan(AANKOMST_MIN, (AX2 - AX1 + 1, AHOOGTE, AZ2 - AZ1 + 1))
    G = AVLOER
    fx1, fx2, fz = PORTAAL[0] - 1, PORTAAL[0] + 2, PORTAAL[2]
    # a stone square around the portal, a path north to the gate (with a step up to the square's floor), and west/east
    for x in range(fx1 - 2, fx2 + 3):
        for z in range(fz - 3, fz + 4):
            p.set(x, G, z, "stone_bricks" if (x + z) % 4 else "polished_andesite")
    for x in range(POORT[0], POORT[1] + 1):
        for z in range(AZ1, fz - 3):
            p.set(x, G, z, "stone_bricks" if (x + z) % 3 else "cobblestone")
        p.trap(x, G + 1, AZ1, "stone_brick_stairs", "north")
    # a path east to the little pond and one south-west into the valley
    for x in range(fx2 + 3, -287):
        for z in (fz, fz + 1):
            p.set(x, G, z, "gravel" if (x * 3 + z) % 5 else "cobblestone")
    for i, z in enumerate(range(fz + 4, fz + 10)):          # (it fades into the valley where the ground starts to rise)
        for x in (-300 + (i // 3), -299 + (i // 3)):
            if AX1 <= x <= AX2:
                p.set(x, G, z, "gravel" if (x + z) % 4 else "mossy_cobblestone")
    # the pond (where the waterfall used to land): water 1 deep, lily pads, a reed-ish edge of grass
    pcx, pcz = -289, -651
    for x in range(pcx - 5, pcx + 6):
        for z in range(pcz - 4, pcz + 5):
            d = ((x - pcx) / 4.6) ** 2 + ((z - pcz) / 3.4) ** 2
            if d <= 1.0:
                p.set(x, G - 1, z, "dirt")
                p.set(x, G, z, "water", {"level": "0"})
            elif d <= 1.5 and p.get(x, G, z) is None:
                p.set(x, G, z, "grass_block", {"snowy": "false"})
                if (x + z) % 2 == 0:
                    p.set(x, G + 1, z, "short_grass")
    for (x, z) in ((-290, -652), (-287, -650), (-291, -649)):
        p.set(x, G + 1, z, "lily_pad")
    # benches facing the portal, lantern posts, flowers in wooden planters
    p.set(fx1 - 3, G + 1, fz - 1, "guhs:guh_bankje_hout", {"facing": "east"})
    p.set(fx1 - 3, G + 1, fz + 1, "guhs:guh_bankje_hout", {"facing": "east"})
    p.set(pcx, G + 1, pcz - 5, "guhs:guh_bankje_hout", {"facing": "south"}) if p.get(pcx, G, pcz - 5) is not None else None
    for (x, z) in ((fx1 - 2, fz - 3), (fx2 + 2, fz - 3), (fx1 - 2, fz + 3), (fx2 + 2, fz + 3), (-292, fz - 1), (POORT[0] - 1, AZ1 + 2)):
        p.set(x, G + 1, z, "guhs:guh_lantaarnpaal", {"half": "lower"})
        p.set(x, G + 2, z, "guhs:guh_lantaarnpaal", {"half": "upper"})
    bloemen = ["poppy", "dandelion", "azure_bluet", "oxeye_daisy", "pink_tulip", "allium", "cornflower", "guhs:roze_guhbloem"]
    rng = random.Random(1209)
    for (x1, z1, x2, z2) in ((fx1 - 4, fz - 2, fx1 - 4, fz + 2), (POORT[1] + 1, AZ1 + 1, POORT[1] + 2, fz - 4),
                             (POORT[0] - 2, AZ1 + 1, POORT[0] - 1, fz - 4)):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                if p.get(x, G, z) is None and p.get(x, G + 1, z) is None:
                    p.set(x, G, z, "grass_block", {"snowy": "false"})
                    p.set(x, G + 1, z, rng.choice(bloemen))
    # planter boxes of spruce around the flower beds by the portal
    p.set(fx1 - 4, G + 1, fz - 3, "spruce_log", {"axis": "y"})
    p.set(fx1 - 4, G + 1, fz + 3, "spruce_log", {"axis": "y"})
    # anchors: the portal, the horse's trail (to its hiding spot down the valley, outside the square)
    p.anker("portaal", PORTAAL[0] + 1.0, G + 1, fz - 1.5, YAW["north"])
    p.anker("aankomst", PORTAAL[0] + 1.0, G + 1, fz - 3.5, YAW["north"])
    return p


SPOOR = [(-299, 30, -652), (-298, 30, -651), (-298, 30, -649), (-297, 30, -648), (-296, 30, -647), (-293, 31, -646)]
# Ozovads' trail from the portal down into the valley (y: standing level; outside the square, use Anker.opGrond)
VERSTOP = (-294, 30, -644)                     # his hiding spot behind the rocks (Java: GuhwinterPlein.VERSTOP)


def check(*plannen):
    bs = os.path.join("src", "main", "resources", "assets", "guhs", "blockstates")
    for p in plannen:
        mist = sorted({b[0] for b in p.s.blocks.values() if b[0].startswith("guhs:")
                       and not os.path.exists(os.path.join(bs, b[0][5:] + ".json"))})
        if mist:
            raise SystemExit(f"guhwinter_plein: unknown blocks {mist}")


def check_aankomst(p):
    """Only common, breakable blocks around the portal (GUHSWINTER.md §0)."""
    toegestaan = {mc(b) for b in AANKOMST_BLOKKEN}
    vreemd = sorted({b[0] for b in p.s.blocks.values() if b[0] not in toegestaan})
    if vreemd:
        raise SystemExit(f"guhwinter_plein: the arrival may only use common blocks, not {vreemd}")


def ankers(*plannen):
    alle = {}
    for p in plannen:
        for k, v in p.ankers.items():
            if k in alle:
                raise SystemExit(f"guhwinter_plein: anchor {k} twice")
            alle[k] = v
    for i, (x, y, z) in enumerate(SPOOR):
        alle[f"paard_spoor_{i}"] = {"x": x + 0.5, "y": y, "z": z + 0.5, "yaw": 0.0}
    alle["paard_verstop"] = {"x": VERSTOP[0] + 0.5, "y": VERSTOP[1], "z": VERSTOP[2] + 0.5, "yaw": 0.0}
    alle["npc_ozovads_verstop"] = dict(alle["paard_verstop"])
    alle["npc_ozovads"] = {"x": -283.5, "y": VLOER + 1, "z": -686.5, "yaw": YAW["west"]}
    # pakjes also around the plaza (outside, by the arrival)
    alle["pakje_verstop_7"] = {"x": -306.5, "y": AVLOER + 1, "z": -657.5, "yaw": 0.0}
    alle["pakje_verstop_8"] = {"x": -284.5, "y": AVLOER + 1, "z": -655.5, "yaw": 0.0}
    out = {"_doc": "Guhwinter (1.10.10) anchors in guhs:guhmension, world coordinates; x/z are block centres where it matters, y is the "
                   "standing level (the block a guh stands in). Made by tools/features/guhwinter_plein.py; read with GuhwinterAnkers.get(name).",
           "ankers": dict(sorted(alle.items(), key=lambda kv: _sorteer(kv[0])))}
    return out


def _sorteer(k):
    import re
    m = re.match(r"(.*?)(\d+)$", k)
    return (m.group(1), int(m.group(2))) if m else (k, -1)


def build(h):
    w, r, a = winter(), raam(), aankomst()
    check(w, r, a)
    for plan in (r, a):                            # (the winter square checks its doors in winter())
        check_deuren(plan)
    check_aankomst(a)
    w.s.save("guhwinter_plein_winter")
    r.s.save("guhwinter_plein_raam")
    a.s.save("guhwinter_aankomst")
    data = ankers(w, a)
    os.makedirs(os.path.dirname(ANKERS_PAD), exist_ok=True)
    with open(ANKERS_PAD, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
    if h is not None:
        for k, (nl, en) in TEKSTEN.items():
            h.lang(k, en, nl)
    _en_overlay()
    return w, r, a


def _en_overlay():
    overlay = {k: v[1] for k, v in TEKSTEN.items()}
    os.makedirs(os.path.dirname(EN_OVERLAY), exist_ok=True)
    with open(EN_OVERLAY, "w", encoding="utf-8", newline="\n") as f:
        json.dump(dict(sorted(overlay.items())), f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    build(None)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        pad = os.path.join("src", "main", "resources", "assets", "guhs", "lang", taal + ".json")
        data = json.load(open(pad, encoding="utf-8"))
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(pad, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    _direct()
