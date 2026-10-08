"""
biomes3 slice "bouw-dal": the templates (not in FEATURES; bio_bouw_dal.py calls build()).

Every structure is a guhs:bio_plek structure (features/bio_wereld_plek.py): its start jigsaw lands IN the top ground block
of a spot of the terrain model, and the template's north side (low z) is turned to what the spot looks at. Local
coordinates below: x east, y up, z south; G = the template y of that ground layer.

  dal_torii         terras        a guh torii over a short stone path (2 variants: plain; with toro and bonsai)
  dal_torii_water   over_rivier   a big torii standing in the river, its beam across the water
  dal_lantaarns     terras        stone toro along a short path of stepping stones (3 variants)
  dal_boogbrug      over_rivier   the red-pink arched bridge (2 variants: lampion posts; stone caps)
  dal_theehuisje    rots          the open tea house at the top of a tall fall, a guh having tea (1)
  dal_zenhoek       terras        raked sand with rings around three boulders (2 variants)
  dal_staptreden    waterval      a stair of stone steps up the rock face beside a fall (1)
  weebhuisje        terras        het weebhuisje with its garden and balloon mooring (1)

What the terrain guarantees, and what the templates do about it:
  - terras: flat dry ground 6 blocks around the anchor, so everything that must stand on ground stays within 13 x 13.
  - over_rivier: the river is at most 9 wide under the anchor, the banks one above the water. The bridge is 13 long and
    lies ON the banks, so it fits any width; under each end is a stone abutment three deep (seen only where the bank
    falls away further out: the first copy the dev server found hung in the air at one end). The big torii's posts go
    3 down. Neither takes the place of water (the bridge through guhs:dal_water_blijft).
  - rots: only the anchor column itself is sure (the bank at the very top of the fall; the river is beside it, east or
    west, and the drop right in front, north). The tea house is a deck one above the ground that reaches back (south)
    onto the terrace, on a rock footing that tapers down; the processor list guhs:dal_water_blijft keeps every water
    block, so where the deck hangs over the river the river stays.
  - waterval: the anchor is the lower bank right beside the river; the rock face (7 high) is just north of it and the
    UPPER river lies right beside the stair. So the stair is a slot with a rock cheek on both sides up to the upper
    ground: the cheek on the river side takes the place of the outermost column of the upper river (the one known
    deviation from the terrain model; without it the upper river would run into the stair).
The renders in guhs_workbio/reports/screens/bouw-dal/_bron read these same functions.
"""
import math

from features import bio_lib as lib

GROND = "minecraft:pink_wool"
STEEN = "guhs:gladde_knuffelsteen"
PLANK = "guhs:roze_lakhout_planken"
STRUCTUREN = ["dal_torii", "dal_torii_water", "dal_lantaarns", "dal_boogbrug", "dal_theehuisje", "dal_zenhoek", "dal_staptreden", "weebhuisje"]
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}
# structure sets: (kind of spot, spacing, separation, salt); the salts are 2150NN01, 2150NN11, ... with NN = 06.
# How often: a structure only starts in a chunk that HAS a spot of its kind, and those are few. Measured on seed 20261007
# in the 81 x 81 chunks around the dal at 256 352 (one valley, about 1000 blocks across; /guhs bio bouw-dal tel 40), over
# two rounds with other spacings: of the chunks a set tries, 1.0 % has a "terras" spot (15 starts in 1557 tries), 0.4 % an
# "over_rivier" spot (11 in 2769), 1.0 % a "waterval" spot (4 in 411), and 2 chunks in 6561 a "rots" spot. With the
# spacings below a valley of that size gets on average 7 torii, 3 torii in the water, 4 lantern paths, 2 zen corners,
# 3 bridges, 4 stairs, a tea house at every tall fall (this valley has three) and 1.3 weebhuisjes: none in about one
# valley in four, two in about one in four (the Superkompas finds the nearest). What that valley really got in the third
# round (these spacings, the torii still at 4): 1 torii, 1 in the water, 3 lantern paths, 3 bridges, 3 tea houses, 2 zen
# corners, 4 stairs, 2 weebhuisjes 590 blocks apart. One torii is thin for the valley's emblem: its spacing went to 3
# (7 on average) after that count.
# biomes3 merge: with the finished valley of wereld-dal (broad terraces, a plain right bank) a "terras" spot is in 2.3 % of
# the chunks a set tries instead of 1.0 % (34 starts in 1461 tries, same valley, same command): 16 torii, 9 lantern paths,
# 3 zen corners and SIX weebhuisjes stood in it. The four terras sets got 1.5 x their spacing (the square root of 2.3), which
# brings back the averages above: 6 torii, 4 lantern paths, 2 zen corners, 1.2 weebhuisjes a valley.
# dal_torii_water found no spot in this valley any more (0 in 729 tries; the river is 6-7 wide now): left as it was.
PLAATSING = {
    "dal_torii": ("terras", 5, 2, 21500601),
    "dal_torii_water": ("over_rivier", 3, 1, 21500611),
    "dal_lantaarns": ("terras", 6, 3, 21500621),
    "dal_boogbrug": ("over_rivier", 3, 1, 21500631),
    "dal_theehuisje": ("rots", 1, 0, 21500641),
    "dal_zenhoek": ("terras", 9, 4, 21500651),
    "dal_staptreden": ("waterval", 4, 2, 21500661),
    "weebhuisje": ("terras", 11, 5, 21500671),
}
WATER_BLIJFT = ("dal_theehuisje", "dal_boogbrug")


def balk(as_="y"):
    return f"guhs:roze_lakhout_balk[axis={as_}]"


def plaat(blok, soort="bottom"):
    return f"{blok}_plaat[type={soort},waterlogged=false]"


def trap(blok, facing, half="bottom", vorm="straight"):
    return f"{blok}_trap[facing={facing},half={half},shape={vorm},waterlogged=false]"


def dakpan(kleur):
    return f"guhs:guh_dakpan_{kleur}"


def lampion(hangt):
    return f"guhs:lampion_roze[hanging={'true' if hangt else 'false'}]"


def ontleed(staat):
    if "[" not in staat:
        return staat, {}
    naam, rest = staat[:-1].split("[")
    return naam, dict(p.split("=") for p in rest.split(","))


class Bouw:
    """A template being built: blocks as "name[prop=value,...]" strings; `dak` marks what a roof-off picture leaves out."""

    def __init__(self, h, naam, size, anker):
        self.h, self.naam, self.size, self.anker = h, naam, size, anker
        self.s = h.Structure(tuple(size))
        self.dak = set()
        self.wezens = []      # (x, y, z, yaw, skin, zit): for the pictures

    def zet(self, x, y, z, staat, nbt=None, dak=False):
        naam, props = ontleed(staat)
        self.s.set(x, y, z, naam, props, nbt)
        (self.dak.add if dak else self.dak.discard)((x, y, z))

    def vul(self, x0, y0, z0, x1, y1, z1, staat, dak=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.zet(x, y, z, staat, dak=dak)

    def lucht(self, x0, y0, z0, x1, y1, z1):
        self.vul(x0, y0, z0, x1, y1, z1, "minecraft:air")

    def blok(self, x, y, z):
        b = self.s.blocks.get((x, y, z))
        return b[0] if b else None

    def vast(self, x, y, z):
        """Something with a full body stands here (for the krul's shape and the fences)."""
        b = self.blok(x, y, z)
        return b is not None and b not in ("minecraft:air", "minecraft:structure_void", "minecraft:jigsaw")

    def krul(self, x, y, z, kleur, facing, dak=True):
        """A dakkrul; its shape is worked out in afwerken() the way DakpanHoekBlock does."""
        self.zet(x, y, z, f"guhs:guh_dakpan_{kleur}_hoek[facing={facing},vorm=recht]", dak=dak)

    def npc(self, x, y, z, soort, kijk, skin=None):
        self.s.entity(x, y, z, {"id": "guhs:guh_npc", "Kind": soort, "PersistenceRequired": self.h.ms.Byte(1),
                                "Rotation": self.h.ms.floats(YAW[kijk], 0.0)})
        self.wezens.append((x, y, z, {"south": 0, "west": 90, "north": 180, "east": 270}[kijk], skin or f"npc_{soort}", True))

    def afwerken(self):
        """The krul shapes and the fence arms, from what stands beside them."""
        links = {"north": (-1, 0), "east": (0, -1), "south": (1, 0), "west": (0, 1)}     # counter-clockwise of the facing
        for (x, y, z), (naam, props, nbt) in list(self.s.blocks.items()):
            if naam.endswith("_hoek") and "guh_dakpan" in naam:
                lx, lz = links[props["facing"]]
                l, r = self.vast(x + lx, y, z + lz), self.vast(x - lx, y, z - lz)
                vorm = "recht"
                if l != r:
                    bx, bz = (x + lx, z + lz) if l else (x - lx, z - lz)
                    buur = self.s.blocks[(bx, y, bz)]
                    krul = buur[0] == naam and buur[1].get("facing") == props["facing"]
                    vorm = ("eind_rechts" if krul else "hoek_rechts") if l else ("eind_links" if krul else "hoek_links")
                props["vorm"] = vorm
            elif naam.endswith("_hek"):
                for kant, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("west", (-1, 0)), ("east", (1, 0))):
                    b = self.blok(x + dx, y, z + dz)
                    vol = b is not None and (b.endswith(("_hek", "_planken", "_balk")) or b in (STEEN, GROND))
                    props[kant] = "true" if vol else "false"
                props["waterlogged"] = "false"

    def bewaar(self, variant=None):
        ax, ay, az = self.anker
        naam = self.naam
        assert self.blok(ax, ay, az) is not None, f"{naam}: the anchor has no final block"
        final_naam, final_props = self.s.blocks[(ax, ay, az)][0], self.s.blocks[(ax, ay, az)][1]
        final = final_naam + ("[" + ",".join(f"{k}={v}" for k, v in sorted(final_props.items())) + "]" if final_props else "")
        self.afwerken()
        self.eind = final          # (for the pictures: what the anchor becomes)
        self.s.set(ax, ay, az, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": f"guhs:{naam}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
        self.bestand = naam if variant is None else f"{naam}_{variant}"
        self.s.save(self.bestand)
        return self


# =====================================================================================================================
# torii
# =====================================================================================================================
def torii(b, cx, z, y0, half, hoog, diep=0, kleur="grijs"):
    """A guh torii: posts at cx-half and cx+half, the beam along x. y0 = the first block above the ground. Ears on the top
    beam and a guh face plaque (facing north, the side the spot looks at). diep: how far the stone feet go down."""
    for px in (cx - half, cx + half):
        for y in range(y0 - diep, y0 + hoog):
            b.zet(px, y, z, STEEN if y < y0 + 1 else balk("y"))
    for x in range(cx - half - 1, cx + half + 2):                 # nuki: the lower beam, through the posts
        if abs(x - cx) != half:
            b.zet(x, y0 + hoog - 2, z, balk("x"))
    b.zet(cx, y0 + hoog - 1, z, "guhs:knuffelsteen_gezicht[facing=north,stemming=0]")
    for x in range(cx - half - 2, cx + half + 3):                 # kasagi: the top beam, and its little roof
        b.zet(x, y0 + hoog, z, balk("x"))
        b.zet(x, y0 + hoog + 1, z, plaat(dakpan(kleur)))
    b.krul(cx - half - 2, y0 + hoog + 1, z, kleur, "west", dak=False)
    b.krul(cx + half + 2, y0 + hoog + 1, z, kleur, "east", dak=False)
    oor = max(1, half - 1)
    for x in (cx - oor, cx + oor):                                # the guh ears: two little bumps on the roof
        b.zet(x, y0 + hoog + 1, z, dakpan(kleur))
        b.zet(x, y0 + hoog + 2, z, plaat("guhs:roze_lakhout"))


def dal_torii(h, variant):
    G = 1
    b = Bouw(h, "dal_torii", (9, 10, 5), (4, G, 2))
    for z in range(5):                                            # the path: a line of stone, loose stones beside it
        b.zet(4, G, z, STEEN)
        if z % 2 == 0:
            b.zet(3 if z != 2 else 5, G, z, STEEN)
    b.zet(5, G, 0, plaat(STEEN, "top"))
    torii(b, 4, 2, G + 1, 2, 5, diep=1, kleur="grijs" if variant == "a" else "roze")
    if variant == "b":
        b.zet(1, G + 1, 0, "guhs:toro[lit=false]")
        b.zet(7, G + 1, 0, "guhs:toro[lit=false]")
        b.zet(7, G + 1, 4, "guhs:bonsai_pot[facing=north,vorm=1]")
        b.zet(1, G, 0, STEEN)
        b.zet(7, G, 0, STEEN)
        b.zet(7, G, 4, STEEN)
    return b.bewaar(variant)


def dal_torii_water(h):
    G = 3
    b = Bouw(h, "dal_torii_water", (11, 13, 3), (5, G, 1))
    b.zet(5, G, 1, "minecraft:air")
    torii(b, 5, 1, G + 1, 3, 6, diep=4, kleur="grijs")
    return b.bewaar()


# =====================================================================================================================
# lantaarns
# =====================================================================================================================
def dal_lantaarns(h, variant):
    G = 0
    b = Bouw(h, "dal_lantaarns", (11, 4, 7), (5, G, 3))
    toro = "guhs:toro[lit=false]"
    if variant == "a":           # a straight path of stepping stones, a lantern at each end on other sides
        for x in range(1, 10):
            b.zet(x, G, 3, STEEN if x % 2 else plaat(STEEN, "top"))
        b.zet(5, G, 3, STEEN)
        for (x, z) in ((2, 2), (8, 4)):
            b.zet(x, G, z, STEEN)
            b.zet(x, G + 1, z, toro)
    elif variant == "b":         # a bend, three lanterns
        pad = [(1, 5), (2, 5), (3, 4), (4, 4), (5, 3), (6, 3), (7, 2), (8, 1), (9, 1)]
        for i, (x, z) in enumerate(pad):
            b.zet(x, G, z, STEEN if i % 3 else plaat(STEEN, "top"))
        b.zet(5, G, 3, STEEN)
        for (x, z) in ((2, 4), (6, 4), (8, 2)):
            b.zet(x, G, z, STEEN)
            b.zet(x, G + 1, z, toro)
    else:                        # one lantern by a few stones, a bonsai and bamboo
        for (x, z) in ((4, 3), (5, 3), (6, 3), (5, 2), (6, 4)):
            b.zet(x, G, z, STEEN)
        b.zet(4, G, 2, STEEN)
        b.zet(4, G + 1, 2, toro)
        b.zet(6, G + 1, 4, "guhs:bonsai_pot[facing=north,vorm=3]")
        for (x, z, hoogte) in ((7, 2, 3), (8, 3, 2)):
            b.zet(x, G, z, STEEN)
            for i in range(hoogte):
                top = hoogte - 1 - i
                b.zet(x, G + 1 + i, z, f"guhs:guh_bamboe[leaves={'large' if top == 0 and hoogte > 2 else 'small' if top <= 1 else 'none'},stage=0]")
    return b.bewaar(variant)


# =====================================================================================================================
# boogbrug
# =====================================================================================================================
def dal_boogbrug(h, variant):
    G = 2
    half = 6
    b = Bouw(h, "dal_boogbrug", (2 * half + 1, G + 6, 4), (half, G, 1))
    b.zet(half, G, 1, "minecraft:air")
    lak = "guhs:roze_lakhout"
    for dx in range(-half, half + 1):
        q = int(round(2 * 2.6 * math.cos(dx / (half + 0.8) * math.pi / 2)))
        for z, extra in ((0, 1), (1, 0), (2, 0), (3, 1)):          # the two rails lie half a block higher than the deck
            qq = q + extra
            if qq <= 0:
                continue
            if qq % 2 == 0:
                b.zet(half + dx, G + qq // 2, z, plaat(lak, "top"))
            else:
                b.zet(half + dx, G + 1 + (qq - 1) // 2, z, plaat(lak))
    for x in (0, 1, 2 * half - 1, 2 * half):                       # a stone abutment under each end: where the bank is lower
        for z in range(4):                                         # than the spot promised, the bridge still lands on something
            for y in range(G - 2, G + 1):                          # (it never takes the place of water: guhs:dal_water_blijft)
                if y >= 0:
                    b.zet(x, y, z, STEEN)
    for x in (0, 2 * half):                                        # the four end posts
        for z in (0, 3):
            b.zet(x, G + 1, z, balk("y"))
            b.zet(x, G + 2, z, balk("y"))
            b.zet(x, G + 3, z, lampion(False) if variant == "a" else plaat(dakpan("grijs")))
    return b.bewaar(variant)


# =====================================================================================================================
# theehuisje
# =====================================================================================================================
def dal_theehuisje(h):
    G = 5
    b = Bouw(h, "dal_theehuisje", (7, G + 10, 8), (3, G, 1))
    # the rock footing under the deck: full for two layers, then stepping in (a corbel where it hangs free)
    for y in range(G + 1):
        inzet = max(0, (G - 1 - y + 1) // 2)
        for x in range(1 + inzet, 6 - inzet):
            for z in range(1 + inzet, 6 - inzet):
                b.zet(x, y, z, STEEN)
    for x in range(1, 6):
        for z in range(1, 6):
            rand = x in (1, 5) or z in (1, 5)
            b.zet(x, G + 1, z, PLANK if not rand else balk("x" if z in (1, 5) else "z"))
            b.lucht(x, G + 2, z, x, G + 4, z)
    for x in (2, 3, 4):                                            # the step up, at the back
        b.zet(x, G + 1, 6, plaat("guhs:roze_lakhout"))
    for (x, z) in ((1, 1), (5, 1), (1, 5), (5, 5)):
        for y in range(G + 2, G + 5):
            b.zet(x, y, z, balk("y"))
    for d in (2, 3, 4):                                            # a low fence on the three open sides
        b.zet(d, G + 2, 1, "guhs:roze_lakhout_hek")
        b.zet(1, G + 2, d, "guhs:roze_lakhout_hek")
        b.zet(5, G + 2, d, "guhs:roze_lakhout_hek")
    dak(b, 0, 0, 6, 6, G + 5, "roze", oren=True)
    for (x, z) in ((0, 0), (6, 0), (0, 6), (6, 6)):
        b.zet(x, G + 4, z, lampion(True))
    b.zet(3, G + 2, 3, "guhs:theetafel[gedekt=true]")
    b.zet(2, G + 2, 3, "guhs:pink_kussen[facing=east]")
    b.npc(3.5, float(G + 2), 4.5, "dal_theeguh", "north")
    return b.bewaar()


def dak(b, x0, z0, x1, z1, y, kleur, oren=False, nok=None):
    """A low hip roof over x0..x1, z0..z1 (the eave ring included): slab eaves with a krul on each corner, then a ring of
    full tiles, a ring of slabs one higher... Two rings rise one block."""
    nok = nok or kleur
    ring = 0
    while x0 + ring <= x1 - ring and z0 + ring <= z1 - ring:
        ax, bx, az, bz = x0 + ring, x1 - ring, z0 + ring, z1 - ring
        yy = y + ring // 2
        laatste = bx - ax <= 1 or bz - az <= 1
        for x in range(ax, bx + 1):
            for z in range(az, bz + 1):
                if x not in (ax, bx) and z not in (az, bz) and not laatste:
                    continue
                if ring % 2 == 0:
                    b.zet(x, yy, z, plaat(dakpan(kleur)), dak=True)
                else:
                    b.zet(x, yy, z, dakpan(kleur), dak=True)
        if ring == 0:
            b.krul(ax, yy, az, kleur, "north")
            b.krul(bx, yy, az, kleur, "north")
            b.krul(ax, yy, bz, kleur, "south")
            b.krul(bx, yy, bz, kleur, "south")
        if laatste:
            top = yy + (1 if ring % 2 else 0)
            for x in range(ax, bx + 1):
                for z in range(az, bz + 1):
                    if ring % 2 == 0:
                        b.zet(x, yy, z, dakpan(nok), dak=True)       # the ridge: one line of white tiles
                    else:
                        b.zet(x, yy + 1, z, plaat(dakpan(nok)), dak=True)
            if oren:                                                # guh ears on the ridge
                ends = [(ax, az), (bx, bz)] if (ax, az) != (bx, bz) else [(ax, az)]
                for (x, z) in ends:
                    b.zet(x, top + 1, z, plaat(dakpan(kleur)), dak=True)
            return top
        ring += 1
    return y


# =====================================================================================================================
# zenhoek
# =====================================================================================================================
RING = {(0, -1): "noord", (1, -1): "noordoost", (1, 0): "oost", (1, 1): "zuidoost", (0, 1): "zuid", (-1, 1): "zuidwest", (-1, 0): "west",
        (-1, -1): "noordwest"}


def zandbed(b, G, cellen, keien, as_="x"):
    """Raked sand in `cellen`, with rings around each kei (x, z, height): the kei stands IN the bed (its foot is the
    centre the ring pieces circle)."""
    for (x, z) in cellen:
        b.zet(x, G, z, f"guhs:geharkt_zand[axis={as_}]")
    for (kx, kz, hoogte) in keien:
        b.zet(kx, G, kz, STEEN)
        for i in range(hoogte):
            b.zet(kx, G + 1 + i, kz, STEEN if i < hoogte - 1 or hoogte == 1 else plaat(STEEN))
        for (dx, dz), vorm in RING.items():
            if (kx + dx, kz + dz) in cellen and b.blok(kx + dx, G, kz + dz) != STEEN:
                b.zet(kx + dx, G, kz + dz, f"guhs:geharkt_zand_ring[vorm={vorm}]")


def dal_zenhoek(h, variant):
    G = 0
    b = Bouw(h, "dal_zenhoek", (13, 4, 11), (6, G, 5))
    if variant == "a":
        vorm = lambda x, z: (abs(x - 6) / 5.4) ** 3 + (abs(z - 5) / 4.2) ** 3 <= 1          # a rounded rectangle
        keien = [(3, 5, 2), (8, 6, 1), (7, 3, 1)]
    else:
        vorm = lambda x, z: 1 <= x <= 11 and 2 <= z <= 8 and not (x >= 9 and z >= 7)         # an L with a corner cut off
        keien = [(3, 4, 1), (6, 6, 2), (9, 3, 1)]
    cellen = {(x, z) for x in range(13) for z in range(11) if vorm(x, z)}
    rand = {(x + dx, z + dz) for (x, z) in cellen for dx in (-1, 0, 1) for dz in (-1, 0, 1)} - cellen
    for (x, z) in rand:                                            # a kerb of stone, flush with the ground
        if 0 <= x < 13 and 0 <= z < 11:
            b.zet(x, G, z, STEEN)
    zandbed(b, G, cellen, keien)
    hoek = sorted(rand, key=lambda p: (p[0] - 12) ** 2 + (p[1] - 0) ** 2)[0]
    b.zet(hoek[0], G + 1, hoek[1], "guhs:toro[lit=false]")
    hoek2 = sorted(rand, key=lambda p: (p[0] - 0) ** 2 + (p[1] - 10) ** 2)[0]
    b.zet(hoek2[0], G + 1, hoek2[1], "guhs:bonsai_pot[facing=north,vorm=0]")
    return b.bewaar(variant)


# =====================================================================================================================
# staptreden
# =====================================================================================================================
def dal_staptreden(h):
    G = 0
    hoog = 7
    b = Bouw(h, "dal_staptreden", (3, hoog + 5, hoog + 2), (1, G, hoog + 1))
    b.zet(1, G, hoog + 1, STEEN)
    for k in range(1, hoog + 1):
        z = hoog + 1 - k
        for y in range(G + 1, G + k):
            b.zet(1, y, z, STEEN)
        b.zet(1, G + k, z, trap(STEEN, "north"))
        b.lucht(1, G + k + 1, z, 1, G + k + 3, z)
        for x in (0, 2):                                           # the rock cheeks (they hold the upper river back)
            for y in range(G + 1, G + hoog + 1):
                b.zet(x, y, z, STEEN)
    for x in range(3):                                             # the landing on top
        b.zet(x, G + hoog, 0, STEEN)
        b.lucht(x, G + hoog + 1, 0, x, G + hoog + 3, 0)
    return b.bewaar()


# =====================================================================================================================
# het weebhuisje
# =====================================================================================================================
# (local spots other code names: the two stand at E and N; the balloon waits one above the middle of the mooring)
WEEB_G = 1
WEEB_E = (8.5, 10.5)
WEEB_N = (4.5, 10.0)
WEEB_STEIGER = (12, 2)


def weebhuisje(h):
    G = WEEB_G
    b = Bouw(h, "weebhuisje", (15, G + 12, 15), (7, G, 7))
    x0, x1, z0, z1 = 2, 10, 6, 12                                   # the walls
    shoji = lambda f, kant: f"guhs:shoji[facing={f},hinge={kant},open=false]"
    # the footing and the floor
    b.vul(x0, 0, z0, x1, G, z1, STEEN)
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            paar = (x - (x0 + 1)) % 2
            if x == x1 - 1:
                b.zet(x, G, z, "guhs:tatami[facing=south,gekoppeld=false]")
            else:
                b.zet(x, G, z, f"guhs:tatami[facing={'east' if paar == 0 else 'west'},gekoppeld=true]")
            b.lucht(x, G + 1, z, x, G + 4, z)
    # the walls: corner posts, a top beam, shoji in front, planks behind the shelves
    for y in range(G + 1, G + 4):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                b.zet(x, y, z, PLANK)
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                b.zet(x, y, z, PLANK)
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        for y in range(G + 1, G + 4):
            b.zet(x, y, z, balk("y"))
    for x in range(x0 + 1, x1):
        b.zet(x, G + 3, z0, balk("x"))
        b.zet(x, G + 3, z1, balk("x"))
    for z in range(z0 + 1, z1):
        b.zet(x0, G + 3, z, balk("z"))
        b.zet(x1, G + 3, z, balk("z"))
    for y in (G + 1, G + 2):
        for (x, kant) in ((3, "left"), (4, "right"), (8, "left"), (9, "right")):      # front: two pairs beside the door
            b.zet(x, y, z0, shoji("south", kant))
        for (z, kant) in ((8, "left"), (9, "right")):                                   # the east wall, by the loket
            b.zet(x1, y, z, shoji("west", kant))
    # the door, in the middle of the front, with the note on its outside (it only shows while they are away)
    b.zet(6, G + 1, z0, "guhs:roze_lakhout_deur[facing=south,half=lower,hinge=left,open=false,powered=false]")
    b.zet(6, G + 2, z0, "guhs:roze_lakhout_deur[facing=south,half=upper,hinge=left,open=false,powered=false]")
    b.zet(6, G + 2, z0 - 1, "guhs:weeb_briefje[facing=north,aan=false]")
    b.zet(5, G + 1, z0, balk("y"))
    b.zet(5, G + 2, z0, balk("y"))
    b.zet(7, G + 1, z0, balk("y"))
    b.zet(7, G + 2, z0, balk("y"))
    # the roof and two beams across with the lampions
    dak(b, x0 - 1, z0 - 1, x1 + 1, z1 + 1, G + 4, "roze", oren=True)
    for x in (4, 8):
        for z in range(z0 + 1, z1):
            b.zet(x, G + 4, z, balk("z"), dak=True)
        b.zet(x, G + 3, 9, lampion(True), dak=True)
    # --- inside: Nielsvads' collection along the back and the west wall ---
    for i, x in enumerate((3, 4, 5, 6)):
        b.zet(x, G + 1, z1 - 1, f"guhs:weeb_figuurtjes[facing=north,soort={i % 4}]")
        b.zet(x, G + 2, z1 - 1, f"guhs:weeb_figuurtjes[facing=north,soort={(i + 2) % 4}]")
    b.zet(3, G + 1, 9, "guhs:weeb_figuurtjes[facing=east,soort=1]")
    b.zet(3, G + 2, 9, "guhs:weeb_poster[facing=east,soort=0]")
    b.zet(3, G + 2, 8, "guhs:weeb_poster[facing=east,soort=1]")
    b.zet(3, G + 2, 10, "guhs:weeb_poster[facing=east,soort=2]")
    b.zet(7, G + 3, z1 - 1, "guhs:weeb_poster[facing=north,soort=3]")
    b.zet(3, G + 1, 7, "guhs:weeb_mangastapel[facing=east]")
    b.zet(3, G + 1, 8, "guhs:weeb_dakimakura[facing=east]")
    b.zet(3, G + 1, 10, "guhs:bonsai_pot[facing=east,vorm=2]")
    # the low table in the middle
    b.zet(6, G + 1, 9, "guhs:theetafel[gedekt=true]")
    b.zet(5, G + 1, 9, "guhs:pink_kussen[facing=east]")
    b.zet(7, G + 1, 9, "guhs:pink_kussen[facing=west]")
    # --- Evivads' mini-loket in the east corner ---
    b.zet(8, G + 1, 9, "guhs:weeb_loket[facing=north]")
    b.zet(9, G + 1, 9, "guhs:weeb_loket[facing=north]")
    b.zet(9, G + 2, 9, "guhs:weeb_loketbord[facing=north,aan=true]")
    b.zet(9, G + 1, 7, "guhs:weeb_nummerautomaat[facing=west]")
    b.zet(9, G + 1, z1 - 1, "guhs:japan_geluksguh[facing=north]")
    b.zet(7, G + 1, z1 - 1, "guhs:japan_lampion[facing=north]")
    b.npc(WEEB_E[0], float(G + 1), WEEB_E[1], "weeb_evivads", "north")
    b.npc(WEEB_N[0], float(G + 1), WEEB_N[1], "weeb_nielsvads", "east")
    # --- the garden in front: a path from the door, toro, a little bed of raked sand, bonsai ---
    for z in range(1, z0 - 1):
        b.zet(6, G, z, STEEN if z % 2 else plaat(STEEN, "top"))
    b.zet(6, G, z0 - 1, STEEN)
    b.zet(5, G, 3, STEEN)
    b.zet(7, G, 2, STEEN)
    cellen = {(x, z) for x in range(2, 5) for z in range(2, 5)}
    for (x, z) in {(x + dx, z + dz) for (x, z) in cellen for dx in (-1, 0, 1) for dz in (-1, 0, 1)} - cellen:
        b.zet(x, G, z, STEEN)
    zandbed(b, G, cellen, [(3, 3, 1)])
    for (x, z) in ((5, 5), (8, 3)):
        b.zet(x, G, z, STEEN)
        b.zet(x, G + 1, z, "guhs:toro[lit=false]")
    b.zet(1, G + 1, 5, "guhs:bonsai_pot[facing=north,vorm=3]")
    b.zet(1, G, 5, STEEN)
    for (x, z, hoogte) in ((1, 8, 4), (1, 10, 3), (1, 12, 4)):      # bamboo along the west wall
        for i in range(hoogte):
            top = hoogte - 1 - i
            b.zet(x, G + 1 + i, z, f"guhs:guh_bamboe[leaves={'large' if top == 0 else 'small' if top == 1 else 'none'},stage=0]")
    # --- the balloon mooring in the north-east corner: a 3 x 3 steiger one step up, a post with a mini balloon ---
    sx, sz = WEEB_STEIGER
    for x in range(sx - 1, sx + 2):
        for z in range(sz - 1, sz + 2):
            b.zet(x, G, z, STEEN)
            b.zet(x, G + 1, z, "guhs:ballonsteiger")
    b.zet(sx - 2, G + 1, sz, plaat(STEEN))
    b.zet(sx - 2, G, sz, STEEN)
    b.zet(sx - 1, G + 2, sz + 2, "guhs:roze_lakhout_hek")
    b.zet(sx - 1, G + 1, sz + 2, "guhs:roze_lakhout_hek")
    b.zet(sx - 1, G, sz + 2, STEEN)
    b.zet(sx - 1, G + 3, sz + 2, "guhs:mini_luchtballon")
    for x in range(8, sx - 2):
        b.zet(x, G, 2, STEEN if x % 2 else plaat(STEEN, "top"))
    b.s.entity(sx + 0.5, float(G + 2), sz + 0.5, {"id": "guhs:weeb_ballon", "Kleur": 0, "Rotation": h.ms.floats(180.0, 0.0)})
    b.ballon = (sx + 0.5, G + 2, sz + 0.5)
    return b.bewaar()


# =====================================================================================================================
def alles(h):
    """Every template, by file name."""
    lijst = [dal_torii(h, "a"), dal_torii(h, "b"), dal_torii_water(h), dal_lantaarns(h, "a"), dal_lantaarns(h, "b"), dal_lantaarns(h, "c"),
             dal_boogbrug(h, "a"), dal_boogbrug(h, "b"), dal_theehuisje(h), dal_zenhoek(h, "a"), dal_zenhoek(h, "b"), dal_staptreden(h),
             weebhuisje(h)]
    return {b.bestand: b for b in lijst}


def build(h):
    from features import bio_wereld_plek as bio_plek
    gebouwd = alles(h)
    D = h.D
    h.add_tag("guhs/tags/block/dal_water_blijft", ["minecraft:water"])
    h.w(f"{D}/worldgen/processor_list/dal_water_blijft.json", {"processors": [
        {"processor_type": "minecraft:protected_blocks", "value": "#guhs:dal_water_blijft"}]})
    for naam in STRUCTUREN:
        soort, spacing, separation, salt = PLAATSING[naam]
        varianten = sorted(k for k in gebouwd if k == naam or (k.startswith(naam + "_") and len(k) == len(naam) + 2))
        assert varianten, naam
        # (h.structure writes a pool that names the template `naam`: point it at a real file first, then at all of them)
        h.structure(naam, ["klaterdal"], spacing=spacing, separation=separation, salt=salt, reach=24, centre=f"guhs:{naam}_midden")
        h.w(f"{D}/worldgen/template_pool/{naam}/start.json", {"fallback": "minecraft:empty", "elements": [
            {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{v}", "projection": "rigid",
                                      "processors": "guhs:dal_water_blijft" if naam in WATER_BLIJFT else "minecraft:empty"}}
            for v in varianten]})
        bio_plek.plek(h, naam, soort)
    return gebouwd
