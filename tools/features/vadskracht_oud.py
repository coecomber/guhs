"""
1.7.0 "Onder de Wol", ronde 45-47 ("Vadskracht in de structuren", ontwerp-guhs170-mijnen.md): WORKING vadskracht in NEW
copies of four old structures. The templates are patched in place (only chunks generated from now on use them; what stands
on the server stays exactly as it is). Every template keeps its size and its jigsaws, so the pieces old copies saved still
load.

The rule: where players may break blocks only early-tier vadskracht stands (Guhrad + Guhdraad, Guhoven, Sauspomp + Sausslang
+ Sausvat, Vadsmolen); a protected building may show everything.

  structure         protected?                                     what it gets
  guh_village       no  (nothing guards a guh village)             the Guhbakkerij on the square: a Guhrad, a Vadsmolen grinding
                                                                   knabbelgraan, a Guhoven baking (early tier)
  kaasmijn          yes (KaasmijnProtection: every piece)          the sauspomp-hal: Sauspompen on kaas saus, Sausvaten, and a
                                                                   Knabbelaar mining kaasbrokken into Knabbelbuizen to a chest
  mika_grillpaleis  no  (bestaand only repairs its NPC spots)      the grill kitchen: Guhraden, a Sauspomp on the frituursaus
                                                                   moat into Sausvaten, a Guhoven grilling (early tier)
  spiesburcht       no  (idem)                                     the skewer kitchen: a Guhrad, Guhovens grilling saté, a
                                                                   Vadsmolen (early tier)
  guhboerderij      yes (BoerderijProtection: every piece)         the oogst-schuur: an Oogster and a Plantagebak (with its
                                                                   klusjes-guh) on Guhraden

Every setup has a sign ("Dit is een ... op N vadskracht") and a lever: a building's own Guhrad ("Vast") stops while a lever
next to it is on (GuhWheelBlockEntity: the guh takes a break), so you can switch the whole setup off and on.

The template helpers are those of features/onderwol_vadskracht.py (guhrad, machine, oven, draad, slang, verbind), on a
Sjabloon: an existing template read with every NBT tag type kept (features/bbq_zonder_lava's reader), the old 32 x 32 tiles of
a guhs:burcht build put together into one coordinate space and cut back into the same tiles on save.

Idempotent: a template that already has a Guhrad is left alone (it was patched already). Rebuild from scratch: check the
templates out of git first. On its own: python tools/features/vadskracht_oud.py from the project root (writes the templates,
the sign texts into nl_nl/en_us and the English overlay tools/lang/en/c199_vadskracht_oud.json).
"""
import json
import os
import re
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
if os.path.join(HIER, "..") not in sys.path:
    sys.path.insert(0, os.path.join(HIER, ".."))

import make_structures as ms  # noqa: E402
import sign_text  # noqa: E402
from features import bbq_zonder_lava as nbtio  # noqa: E402
from features import onderwol_bouw as ob  # noqa: E402
from features import onderwol_vadskracht as ov  # noqa: E402

Tag = nbtio.Tag
STRUCTUUR = os.path.join("src", "main", "resources", "data", "guhs", "structure")
PREFIX = "sign.guhs.vadskracht_oud"
TEGEL = 32
LUCHT = "minecraft:air"
HENDEL = "minecraft:lever"


# =====================================================================================================================
# NBT <-> python
# =====================================================================================================================
def _naar_tag(v):
    """An ms-style value (dict, ms.Byte, ms.NbtList...) as a Tag."""
    if isinstance(v, Tag):
        return v
    t = ms._tag_type(v)
    if t == 9:
        items = [_naar_tag(i) for i in v]
        return Tag(9, (v.tag_type if v else 0, [i.v for i in items]))
    if t == 10:
        return Tag(10, {k: _naar_tag(x) for k, x in v.items()})
    if t == 1:
        return Tag(1, int(v))
    return Tag(t, v)


class Sjabloon:
    """An existing template (one .nbt, or a folder of guhs:burcht tiles stuk_i_j.nbt) to patch with the helpers of
    onderwol_vadskracht: blocks {(x, y, z): (name, {prop: value}, nbt)} in one coordinate space, nbt a Tag (as read) or an
    ms-style dict (newly set)."""

    def __init__(self, naam, sign_prefix):
        self.naam = naam
        self.prefix = sign_prefix
        self.blocks = {}
        self.bestanden = {}   # path -> (tile origin x, z, root name, root Tag)
        map_ = os.path.join(STRUCTUUR, naam)
        if os.path.isdir(map_) and not os.path.exists(map_ + ".nbt"):
            for f in sorted(os.listdir(map_)):
                m = re.fullmatch(r"stuk_(\d+)_(\d+)\.nbt", f)
                if m:
                    self._lees(os.path.join(map_, f), int(m.group(1)) * TEGEL, int(m.group(2)) * TEGEL)
        else:
            self._lees(map_ + ".nbt", 0, 0)
        assert self.bestanden, f"no template {naam}"

    def _lees(self, pad, ox, oz):
        rootnaam, root = nbtio.nbt_load(pad)
        self.bestanden[pad] = (ox, oz, rootnaam, root)
        palette = [self._staat(s) for s in root.v["palette"].v[1]]
        for b in root.v["blocks"].v[1]:
            x, y, z = b["pos"].v[1]
            naam, props = palette[b["state"].v]
            self.blocks[(x + ox, y, z + oz)] = (naam, dict(props), b["nbt"] if "nbt" in b else None)

    @staticmethod
    def _staat(s):
        props = {k: t.v for k, t in s["Properties"].v.items()} if "Properties" in s else {}
        return s["Name"].v, props

    def maat(self, pad):
        return self.bestanden[pad][3].v["size"].v[1]

    def grootte(self):
        """The size of the whole template (all tiles)."""
        sx = max(ox + self.maat(p)[0] for p, (ox, oz, _, _) in self.bestanden.items())
        sz = max(oz + self.maat(p)[2] for p, (ox, oz, _, _) in self.bestanden.items())
        return sx, self.maat(next(iter(self.bestanden)))[1], sz

    # --- what the helpers of onderwol_vadskracht use --------------------------------------------------------------------
    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def set(self, x, y, z, name, props=None, nbt=None):
        p = (int(x), int(y), int(z))
        assert self._binnen(p), f"{self.naam}: {p} lies outside the template"
        self.blocks[p] = (name, props or {}, nbt)

    def zet(self, x, y, z, s, nbt=None):
        naam, props = ob._blok(s)
        self.set(x, y, z, naam, props, nbt)

    def _binnen(self, p):
        x, y, z = p
        for pad, (ox, oz, _, _) in self.bestanden.items():
            sx, sy, sz = self.maat(pad)
            if ox <= x < ox + sx and 0 <= y < sy and oz <= z < oz + sz:
                return True
        return False

    def sign(self, x, y, z, facing, lines, wall=True, wood="birch"):
        """A waxed sign, its lines translatable (sign_text keys under this template's prefix)."""
        for line in lines:
            assert len(line) <= 16, f"{self.naam}: sign line {line!r} is longer than 16 characters"
        nbt = {"id": "minecraft:sign", "is_waxed": ms.Byte(1),
               "front_text": {"messages": sign_text.messages(self.prefix, lines), "color": "black", "has_glowing_text": ms.Byte(0)},
               "back_text": {"messages": sign_text.messages(self.prefix, ["", "", "", ""]), "color": "black", "has_glowing_text": ms.Byte(0)}}
        if wall:
            self.set(x, y, z, f"minecraft:{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            self.set(x, y, z, f"minecraft:{wood}_sign", {"rotation": ob.ROT[facing], "waterlogged": "false"}, nbt)

    def hendel(self, x, y, z, face, facing):
        """A lever (off): face floor / wall / ceiling, facing = where it points (on a wall: away from the wall)."""
        self.set(x, y, z, HENDEL, {"face": face, "facing": facing, "powered": "false"})

    def heeft(self, naam):
        return any(b[0] == naam for b in self.blocks.values())

    def entity(self, x, y, z, nbt):
        """A new entity (saved into the tile it stands in)."""
        for pad, (ox, oz, _, root) in self.bestanden.items():
            sx, _, sz = self.maat(pad)
            if ox <= x < ox + sx and oz <= z < oz + sz:
                e = {"pos": ms.doubles(x - ox, y, z - oz), "blockPos": ms.ints(int(x) - ox, int(y), int(z) - oz), "nbt": nbt}
                root.v["entities"].v[1].append(_naar_tag(e).v)
                if root.v["entities"].v[0] == 0:
                    root.v["entities"] = Tag(9, (10, root.v["entities"].v[1]))
                return
        raise AssertionError(f"{self.naam}: an entity at {(x, y, z)} lies outside the template")

    def entities(self):
        """[(x, y, z, nbt Tag-dict)] of every entity (template coordinates)."""
        uit = []
        for pad, (ox, oz, _, root) in self.bestanden.items():
            for e in root.v["entities"].v[1]:
                x, y, z = e["pos"].v[1]
                uit.append((x + ox, y, z + oz, e["nbt"].v))
        return uit

    # --- saving ----------------------------------------------------------------------------------------------------------
    def save(self):
        for pad, (ox, oz, rootnaam, root) in self.bestanden.items():
            sx, sy, sz = self.maat(pad)
            palette, index, blocks = [], {}, []
            for (x, y, z), (naam, props, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
                if not (ox <= x < ox + sx and oz <= z < oz + sz):
                    continue
                sleutel = (naam, tuple(sorted(props.items())))
                if sleutel not in index:
                    index[sleutel] = len(palette)
                    s = {"Name": Tag(8, naam)}
                    if props:
                        s["Properties"] = Tag(10, {k: Tag(8, str(v)) for k, v in sorted(props.items())})
                    palette.append(s)
                b = {"pos": Tag(9, (3, [x - ox, y, z - oz])), "state": Tag(3, index[sleutel])}
                if nbt is not None:
                    b["nbt"] = _naar_tag(nbt)
                blocks.append(b)
            root.v["palette"] = Tag(9, (10, palette))
            root.v["blocks"] = Tag(9, (10, blocks))
            nbtio.nbt_save(pad, rootnaam, root)


# =====================================================================================================================
# helpers on top of onderwol_vadskracht
# =====================================================================================================================
RICHTING = ov.RICHTING
CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
CCW = {v: k for k, v in CW.items()}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}
BUIS, RICHTSTUK = "guhs:knabbelbuis", "guhs:knabbelbuis_richting"
# what a Knabbelbuis joins besides tubes (anything that holds items; only for the look of its arms: the game works out the
# routes itself)
HOUDT = {"minecraft:chest", "minecraft:barrel", "guhs:knabbelaar", "guhs:oogster", "guhs:grillkoolpers", "guhs:frituurautomaat",
         "guhs:vadsmolen", "guhs:guh_oven"}


def vrij(b, x, y, z):
    return b.get(x, y, z) in (None, LUCHT)


def plaats(b, x, y, z, wat):
    assert vrij(b, x, y, z), f"{b.naam}: {wat} at {(x, y, z)} needs room, not {b.get(x, y, z)}"


def rad(b, x, y, z, facing):
    """A building's Guhrad (a happy guh: 15 VK, "Vast") whose wheel block is at (x, y, z)."""
    ov.guhrad(b, x, y, z, facing)


def hendel_op(b, x, y, z, facing):
    """A lever (off) on the floor at (x, y, z), pointing `facing`; it stands right next to a wheel block: on = the guh's break."""
    plaats(b, x, y, z, "the lever")
    assert not vrij(b, x, y - 1, z), f"{b.naam}: the lever at {(x, y, z)} needs a floor"
    b.hendel(x, y, z, "floor", facing)


def buis(b, x, y, z):
    plaats(b, x, y, z, "a Knabbelbuis")
    b.set(x, y, z, BUIS, {})


def richtstuk(b, x, y, z, facing):
    """A Richtingstuk: takes items out of what is behind it (opposite `facing`) and sends them on out of its front."""
    plaats(b, x, y, z, "a Richtingstuk")
    b.set(x, y, z, RICHTSTUK, {"facing": facing}, {"id": "guhs:knabbelbuis_richting"})


def kist(b, x, y, z, facing):
    plaats(b, x, y, z, "a chest")
    b.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"}, {"id": "minecraft:chest"})


def bord(b, x, y, z, facing, regels):
    """A standing sign whose text faces `facing`."""
    plaats(b, x, y, z, "a sign")
    b.sign(x, y, z, facing, regels, wall=False)


def vat(b, x, y, z):
    plaats(b, x, y, z, "a Sausvat")
    b.zet(x, y, z, "sausvat[facing=north,niveau=0,saus=leeg]", {"id": "guhs:sausvat"})


def bron(b, x, y, z, vloeistof):
    """A source block of a fluid in the floor (the blocks around it must hold it in)."""
    for kant in ("north", "east", "south", "west", "down"):
        dx, dy, dz = RICHTING[kant]
        assert not vrij(b, x + dx, y + dy, z + dz), f"{b.naam}: the {vloeistof} at {(x, y, z)} would run out at {kant}"
    b.set(x, y, z, vloeistof, {"level": "0"})


def plantagebak(b, x, y, z, facing, stapels):
    """A Plantagebak whose kern (the middle of the front edge) is at (x, y, z), its bed of 3 x 3 behind it, with its eight
    part blocks (their offset to the kern, dx/dy/dz + 2, and their piece of the bed: PlantagebakDeelBlock.Stuk)."""
    zij = RICHTING[CW[facing]]
    terug = RICHTING[OPP[facing]]
    mx0, mz0 = x + terug[0], z + terug[2]
    for d in range(3):
        for s in (-1, 0, 1):
            px, pz = x + zij[0] * s + terug[0] * d, z + zij[2] * s + terug[2] * d
            plaats(b, px, y, pz, "the Plantagebak")
            if d == 0 and s == 0:
                continue
            mx, mz = px - mx0, pz - mz0
            stuk = ["nw", "n", "no", "w", "midden", "o", "zw", "z", "zo"][(mz + 1) * 3 + (mx + 1)]
            b.set(px, y, pz, "guhs:techmachine_plantagebak_deel", {"dx": str(px - x + 2), "dy": "0", "dz": str(pz - z + 2), "stuk": stuk})
    ov.machine(b, x, y, z, "plantagebak", facing, slots=1, stapels=stapels)


def verbind_buizen(b):
    """The arms of every Knabbelbuis (BuisBlock: a tube, the front or back of a piece, or something that holds items)."""
    for p, (naam, props, nbt) in list(b.blocks.items()):
        if naam != BUIS:
            continue
        arms = {}
        for kant, (dx, dy, dz) in RICHTING.items():
            buur = b.blocks.get((p[0] + dx, p[1] + dy, p[2] + dz))
            n = buur[0] if buur else None
            if n == BUIS or n in HOUDT:
                arms[kant] = "true"
            elif n == RICHTSTUK:
                arms[kant] = "true" if buur[1].get("facing") in (kant, OPP[kant]) else "false"
            else:
                arms[kant] = "false"
        b.blocks[p] = (naam, arms, nbt)


def klusguh(b, x, y, z, yaw=0.0):
    """A klusjes-guh of the building (GebouwKlus: it chops the trees of the building's own Plantagebakken)."""
    nbt = ms.guh_nbt(1.0)
    nbt["Rotation"] = ms.floats(float(yaw), 0.0)
    nbt["Tags"] = ms.NbtList(8, ["guhs_gebouwklus"])
    b.entity(x, y, z, nbt)


class Lokaal:
    """A little build in its own frame: u along the row (the wheel's width), v towards the front (facing)."""

    def __init__(self, b, x0, y, z0, facing):
        self.b, self.x0, self.y, self.z0, self.facing = b, x0, y, z0, facing
        self.F = RICHTING[facing]
        self.U = RICHTING[CCW[facing]]

    def p(self, u, v, dy=0):
        return (self.x0 + self.U[0] * u + self.F[0] * v, self.y + dy, self.z0 + self.U[2] * u + self.F[2] * v)


EN_REGELS = {
    "Guhbakkerij!": "Guh bakery!", "Guhrad: 15 VK": "Guh wheel: 15 VP", "Hendel = aan/uit": "Lever = on/off",
    "Hendel bij het": "The lever by the", "rad = aan/uit": "wheel = on/off",
    "Dit is een": "This is a", "Dit zijn": "These are", "Guhoven op": "Guh oven on", "Guhovens op": "Guh ovens on",
    "5 vadskracht": "5 vads power", "6 vadskracht": "6 vads power", "8 vadskracht": "8 vads power", "10 vadskracht": "10 vads power",
    "bakt aardappels": "bakes potatoes", "Vadsmolen op": "Vads mill on", "maalt graan": "grinds grain", "grillen saté": "grills satay",
    "Spieskeuken!": "Skewer kitchen!", "Mika-grillkeuken": "Mika grill shack", "Sauspomp op": "Sauce pump on", "pompt saus": "pumps sauce",
    "pompt kaassaus": "pumps cheese sauce", "Sausvaten:": "Sauce vats:", "zie ze vollopen!": "watch them fill!",
    "2 pompen: 12 VK": "2 pumps: 12 VP", "Knabbelaar op": "Nibbler on", "knabbelt kaas": "nibbles cheese",
    "Kaasbrokken": "Cheese chunks", "door de buis": "through the tube", "naar de kist!": "to the chest!",
    "Oogster op": "Harvester on", "oogst het graan": "harvests the wheat", "Knabbelbuis:": "Nibble tube:",
    "de oogst rolt": "the harvest rolls", "Plantagebak op": "Planter on", "Klusguh hakt!": "A chore guh chops!",
    "Plantage:": "Plantation:", "Grillkoolfabriek": "Grill coal works", "2 Guhraden: 30": "2 guh wheels: 30",
    "vadskracht.": "vads power.", "(saus + water)": "(sauce + water)", "Frituurautomaat": "Frying machine",
    "op 8 vadskracht": "on 8 vads power", "bakt knabbels": "fries nibbles", "Grillkoolpers op": "Grill coal press",
    "8 vadskracht:": "on 8 vads power:", "saus + water =": "sauce + water =", "grillkool!": "grill coal!",
    "Kist: grillkool": "Chest: grill coal", "en knabbels via": "and nibbles via", "de knabbelbuis!": "the nibble tube!",
}
LANG = {
    "gui.guhs.vadskracht.guhrad.rust": ("De guh heeft pauze: de hendel staat aan", "The guh is taking a break: the lever is on"),
}


# =====================================================================================================================
# 1. the guh village (not protected: early tier only): the Guhbakkerij on the square
# =====================================================================================================================
KRAAM_U, KRAAM_V = range(-1, 8), range(-1, 4)   # the stall (u 0..6, v 0..2) and a rim of one block to walk round it


def kraam_plek(b, midden=(32, 32)):
    """The spot of the stall: free ground (and room above) for its 7 x 3 and a rim, off the two main paths and out of the
    fountain, as close to the square as it fits, its front towards the middle. Returns (x0, z0, facing)."""
    beste = None
    for facing in ("north", "east", "south", "west"):
        for x0 in range(64):
            for z0 in range(64):
                l = Lokaal(b, x0, 1, z0, facing)
                ok = True
                for u in KRAAM_U:
                    for v in KRAAM_V:
                        x, _, z = l.p(u, v)
                        onder = b.get(x, 0, z)
                        if not (0 <= x < 64 and 0 <= z < 64) or onder in (None, LUCHT) or "saus" in onder or "fence" in onder \
                                or abs(x - midden[0]) <= 2 or abs(z - midden[1]) <= 2 \
                                or any(not vrij(b, x, y, z) for y in range(1, 6)):
                            ok = False
                            break
                    if not ok:
                        break
                if not ok:
                    continue
                cx, _, cz = l.p(3, 1)
                d = ((cx - midden[0]) ** 2 + (cz - midden[1]) ** 2) ** 0.5
                naar = ((midden[0] - cx) / d, (midden[1] - cz) / d)
                if l.F[0] * naar[0] + l.F[2] * naar[1] < 0.7:
                    continue
                if beste is None or d < beste[0]:
                    beste = (d, x0, z0, facing)
    assert beste, f"{b.naam}: no room for the Guhbakkerij"
    return beste[1:]


def guhbakkerij(b):
    """The Guhbakkerij: a stall with a striped roof on four posts. A Guhrad (one happy guh, 15 VK) powers, over pink Guhdraad
    on redstone lamps, a Guhoven baking potatoes (5) and the square's Vadsmolen grinding knabbelgraan (5). The lever in
    front of the wheel gives the guh a break (everything stops). Early tier: a guh village is not protected."""
    x0, z0, facing = kraam_plek(b)
    l = Lokaal(b, x0, 1, z0, facing)
    f = facing
    rad(b, *l.p(2, 0), f)
    ov.oven(b, *l.p(4, 0), f, "minecraft:potato", 32)
    ov.machine(b, *l.p(5, 0), "vadsmolen", f, slots=2, stapels=[(0, "guhs:knabbelgraan", 64)])
    hendel_op(b, *l.p(2, 1), f)
    for u in (3, 4, 5):
        ov.draad(b, *l.p(u, 1))
    for u in (0, 6):
        for v in (0, 2):
            for dy in range(3):
                b.set(*l.p(u, v, dy), "minecraft:birch_fence", {})
    for u in range(-1, 8):                         # a striped wool canopy
        for v in range(-1, 4):
            b.set(*l.p(u, v, 3), "minecraft:pink_wool" if u % 2 else "minecraft:white_wool", {})
    bord(b, *l.p(1, 2), f, ["Guhbakkerij!", "Guhrad: 15 VK", "Hendel bij het", "rad = aan/uit"])
    bord(b, *l.p(4, 2), f, ["Dit is een", "Guhoven op", "5 vadskracht", "bakt aardappels"])
    bord(b, *l.p(5, 2), f, ["Dit is een", "Vadsmolen op", "5 vadskracht", "maalt graan"])
    return x0, z0, facing


# =====================================================================================================================
# 2. the kaasmijn (protected: KaasmijnProtection, every piece): the Knabbelgrot (L1, floor y 20)
# =====================================================================================================================
def kaasmijn(b):
    """Two setups in the Knabbelgrot, each on its own Guhrad with its lever behind the wheel:
      - the pomphuis: two Sauspompen in the kaassaus pool (12 of 15 VK), their hoses to five Sausvaten along the west wall;
      - the kaasbrokken: a Knabbelaar (10 of 15 VK) gnawing a kaasader in the west wall (it grows back: KaasaderBlock),
        a Richtingstuk on top takes the kaasbrokken out and sends them through a Knabbelbuis into a chest.
    Tier: everything (protected); the Knabbelaar works in its own building (TechBlockEntity.gebouwMachine)."""
    Y = 21
    # --- the kaasbrokken ---
    assert b.get(35, Y, 65) == "guhs:kaasader", b.get(35, Y, 65)
    rad(b, 38, Y, 64, "south")
    hendel_op(b, 38, Y, 63, "north")
    ov.draad(b, 37, Y, 65)
    ov.machine(b, 36, Y, 65, "knabbelaar", "west")
    richtstuk(b, 36, Y + 1, 65, "up")
    buis(b, 36, Y + 2, 65)
    buis(b, 36, Y + 2, 66)
    buis(b, 36, Y + 1, 66)
    kist(b, 36, Y, 66, "east")
    bord(b, 38, Y, 66, "south", ["Dit is een", "Knabbelaar op", "10 vadskracht", "knabbelt kaas"])
    bord(b, 39, Y, 62, "south", ["Kaasbrokken", "door de buis", "naar de kist!", "Hendel = aan/uit"])
    # --- the pomphuis ---
    rad(b, 44, Y, 64, "south")
    hendel_op(b, 44, Y, 63, "north")
    for z in (65, 66, 67, 68):
        ov.draad(b, 44, Y, z)
    ov.draad(b, 45, Y, 68)
    for x in (44, 45):
        assert b.get(x, Y - 1, 69) == "guhs:kaas_saus", (x, b.get(x, Y - 1, 69))
        ov.machine(b, x, Y, 69, "sauspomp", "south")
        ov.slang(b, x, Y + 1, 69)
    for x in range(38, 44):
        ov.slang(b, x, Y + 1, 69)
    for z in range(67, 72):
        vat(b, 37, Y, z)
        ov.slang(b, 37, Y + 1, z)
    bord(b, 46, Y, 66, "south", ["Dit is een", "Sauspomp op", "6 vadskracht", "pompt kaassaus"])
    bord(b, 42, Y, 63, "south", ["Guhrad: 15 VK", "2 pompen: 12 VK", "Hendel bij het", "rad = aan/uit"])
    bord(b, 38, Y, 72, "east", ["Sausvaten:", "zie ze vollopen!", "", ""])


# =====================================================================================================================
# 3. the Barbecuether: the Spiesburcht and the Mika-grillpaleis (not protected: early tier), the Mika-woonblokken
#    (protected: PaleizenFeature, Bescherming): the grillkool works
# =====================================================================================================================
def spiesburcht(b):
    """The Spieskeuken in the great hall (deck y 30, along the west wall, tile stuk_1_1): a Guhrad (15 VK) feeding two
    Guhovens grilling saté and a Vadsmolen (3 x 5 = 15 VK), the lever behind the wheel."""
    Y = 31
    rad(b, 46, Y, 54, "east")
    hendel_op(b, 45, Y, 54, "west")
    for z in range(52, 58):
        ov.draad(b, 47, Y, z)
    ov.machine(b, 46, Y, 52, "vadsmolen", "east", slots=2, stapels=[(0, "guhs:knabbelgraan", 64)])
    ov.oven(b, 46, Y, 56, "east", "guhs:kaasknabbelsate", 16)
    ov.oven(b, 46, Y, 57, "east", "guhs:kaasknabbelsate", 16)
    bord(b, 48, Y, 54, "east", ["Spieskeuken!", "Guhrad: 15 VK", "Hendel bij het", "rad = aan/uit"])
    bord(b, 48, Y, 56, "east", ["Dit zijn", "Guhovens op", "5 vadskracht", "grillen saté"])
    bord(b, 48, Y, 52, "east", ["Dit is een", "Vadsmolen op", "5 vadskracht", "maalt graan"])


def grillpaleis(b):
    """The Mika-grillkeuken on the second floor of the keep (floor y 29, the north-west corner, tile stuk_0_0): a Guhrad
    (15 VK) feeds a Guhoven grilling saté (5) and a Sauspomp (6) on a frituursaus source in the floor, its hose to five
    Sausvaten along the north wall; the lever behind the wheel."""
    Y = 30
    rad(b, 25, Y, 27, "east")
    hendel_op(b, 24, Y, 27, "west")
    for x in (26, 27, 28, 29):
        ov.draad(b, x, Y, 27)
    ov.oven(b, 28, Y, 28, "south", "guhs:kaasknabbelsate", 16)
    b.set(30, Y - 2, 27, "guhs:gepolijst_roosterijzer", {})   # (a stub under the source: the room below has a ceiling there)
    bron(b, 30, Y - 1, 27, "guhs:kaasfrituursaus")
    ov.machine(b, 30, Y, 27, "sauspomp", "south")
    for (x, z) in ((30, 27), (30, 26), (30, 25)):
        ov.slang(b, x, Y + 1, z)
    for x in range(27, 32):
        vat(b, x, Y, 24)
        ov.slang(b, x, Y + 1, 24)
    bord(b, 27, Y, 29, "south", ["Mika-grillkeuken", "Guhrad: 15 VK", "Hendel bij het", "rad = aan/uit"])
    bord(b, 28, Y, 29, "south", ["Dit is een", "Guhoven op", "5 vadskracht", "grillen saté"])
    bord(b, 30, Y, 29, "south", ["Dit is een", "Sauspomp op", "6 vadskracht", "pompt saus"])
    bord(b, 31, Y, 26, "east", ["Sausvaten:", "zie ze vollopen!", "", ""])


def woonblokken(b):
    """The Grillkoolfabriek in the courtyard (floor y 12, the south-west, tile stuk_0_1). Two Guhraden face each other
    with ONE lever between them (both guhs take a break together): 30 VK for a Sauspomp on frituursaus (6), a Sauspomp on
    water (6), the Grillkoolpers (8: saus + water = grillkool) and the Frituurautomaat (8: kaasknabbels in the frituursaus).
    One hose net brings both (each tank takes only its own). Richtingstukken take the grillkool and the fried knabbels out
    and send them through Knabbelbuizen into a chest. Tier: everything (the palaces are protected)."""
    Y = 13
    rad(b, 21, Y, 37, "east")
    rad(b, 23, Y, 37, "west")
    hendel_op(b, 22, Y, 37, "north")
    for x in range(20, 27):
        ov.draad(b, x, Y, 35)
    bron(b, 21, Y - 1, 34, "guhs:kaasfrituursaus")
    ov.machine(b, 21, Y, 34, "sauspomp", "south")
    bron(b, 26, Y - 1, 36, "minecraft:water")
    ov.machine(b, 26, Y, 36, "sauspomp", "south")
    ov.machine(b, 24, Y, 34, "frituurautomaat", "south", slots=3, stapels=[(0, "guhs:kaas_knabbels", 64)])
    ov.machine(b, 26, Y, 34, "grillkoolpers", "south", slots=1)
    for x in range(21, 27):
        ov.slang(b, x, Y + 1, 34)
    for z in (36, 35):
        ov.slang(b, 26, Y + 1, z)
    richtstuk(b, 24, Y, 33, "north")
    richtstuk(b, 26, Y, 33, "north")
    buis(b, 24, Y, 32)
    kist(b, 25, Y, 32, "south")
    buis(b, 26, Y, 32)
    bord(b, 22, Y, 39, "south", ["Grillkoolfabriek", "2 Guhraden: 30", "vadskracht.", "Hendel = aan/uit"])
    bord(b, 20, Y, 36, "south", ["Dit is een", "Sauspomp op", "6 vadskracht", "(saus + water)"])
    bord(b, 24, Y, 36, "south", ["Frituurautomaat", "op 8 vadskracht", "bakt knabbels", ""])
    bord(b, 25, Y, 36, "south", ["Grillkoolpers op", "8 vadskracht:", "saus + water =", "grillkool!"])
    bord(b, 27, Y, 32, "south", ["Kist: grillkool", "en knabbels via", "de knabbelbuis!", ""])


# =====================================================================================================================
# 4. the Guhboerderij (protected: BoerderijProtection, every piece): the oogst-hoek south-west of the yard (ground y 4)
# =====================================================================================================================
def guhboerderij(b):
    """Two setups, each on its own Guhrad with its lever behind the wheel:
      - the Oogster (8 of 15 VK) over a fenced wheat field of 5 x 5 (ripe: it cuts and replants), a Richtingstuk behind it
        sends the harvest through a Knabbelbuis into a chest;
      - the Plantage: a Plantagebak (6 of 15 VK) with oak saplings grows a tree within a minute; the farm's klusjes-guhs
        (GebouwKlus) chop it and put the wood in the chest beside it.
    Tier: everything (protected); the machines work in their own building (TechBlockEntity.gebouwMachine)."""
    Y = 5
    # --- the Oogster and its field ---
    rad(b, 17, Y, 50, "east")
    hendel_op(b, 16, Y, 50, "west")
    ov.draad(b, 18, Y, 50)
    ov.draad(b, 19, Y, 50)
    ov.machine(b, 20, Y, 50, "oogster", "south")
    for x in range(18, 23):
        for z in range(51, 56):
            plaats(b, x, Y, z, "the field")
            if (x, z) == (20, 53):
                b.set(x, Y - 1, z, "minecraft:water", {"level": "0"})
            else:
                b.set(x, Y - 1, z, "minecraft:farmland", {"moisture": "7"})
                b.set(x, Y, z, "minecraft:wheat", {"age": "7"})
    for (x, z) in [(17, z) for z in range(52, 57)] + [(23, z) for z in range(50, 57)] + [(x, 56) for x in range(18, 23)] + [(21, 50), (22, 50)]:
        plaats(b, x, Y, z, "the field fence")
        b.set(x, Y, z, "minecraft:cherry_fence", {})
    richtstuk(b, 20, Y, 49, "north")
    buis(b, 20, Y, 48)
    kist(b, 20, Y, 47, "south")
    bord(b, 21, Y, 49, "north", ["Dit is een", "Oogster op", "8 vadskracht", "oogst het graan"])
    bord(b, 18, Y, 48, "north", ["Guhrad: 15 VK", "Hendel bij het", "rad = aan/uit", ""])
    bord(b, 21, Y, 47, "north", ["Knabbelbuis:", "de oogst rolt", "naar de kist!", ""])
    # --- the Plantage ---
    plantagebak(b, 26, Y, 52, "west", [(0, "minecraft:oak_sapling", 4)])
    rad(b, 27, Y, 57, "north")
    hendel_op(b, 27, Y, 58, "south")
    for z in (56, 55, 54):
        ov.draad(b, 27, Y, z)
    kist(b, 24, Y, 53, "east")
    bord(b, 25, Y, 50, "west", ["Dit is een", "Plantagebak op", "6 vadskracht", "Klusguh hakt!"])
    bord(b, 29, Y, 56, "south", ["Plantage:", "Guhrad: 15 VK", "Hendel bij het", "rad = aan/uit"])
    klusguh(b, 25.5, Y, 55.5, 180.0)
    klusguh(b, 29.5, Y, 53.5, 90.0)


# =====================================================================================================================
# all of it
# =====================================================================================================================
SJABLONEN = {
    "guh_village/layout_a": (guhbakkerij, "dorp"), "guh_village/layout_b": (guhbakkerij, "dorp"),
    "guh_village/layout_c": (guhbakkerij, "dorp"), "kaasmijn": (kaasmijn, "kaasmijn"), "spiesburcht": (spiesburcht, "spiesburcht"),
    "mika_grillpaleis": (grillpaleis, "grillpaleis"), "mika_woonblokken": (woonblokken, "woonblokken"),
    "guhboerderij": (guhboerderij, "boerderij"),
}


def patch_alles():
    """Patches every template that has no Guhrad yet; returns {template: what happened}."""
    uit = {}
    for naam, (fn, kort) in SJABLONEN.items():
        b = Sjabloon(naam, f"{PREFIX}.{kort}")
        if b.heeft("guhs:guh_wheel"):
            # (patched before: register its sign texts all the same, so a full regeneration keeps them)
            uit[naam] = "already"
            _teksten_opnieuw(b)
            continue
        r = fn(b)
        ov.verbind(b)
        verbind_buizen(b)
        b.save()
        uit[naam] = f"patched {r}" if r else "patched"
    return uit


def _teksten_opnieuw(b):
    """The sign texts of an already patched template (sign_text.TEXTS, for the lang files)."""
    for (_, _, nbt) in b.blocks.values():
        if isinstance(nbt, Tag) and "front_text" in nbt.v:
            for m in nbt.v["front_text"].v["messages"].v[1]:
                try:
                    d = json.loads(m)
                except ValueError:
                    continue
                if isinstance(d, dict) and d.get("translate", "").startswith(PREFIX + "."):
                    sign_text.register(d["translate"], d["fallback"])


def build(h):
    patch_alles()
    for k, (nl, en) in LANG.items():
        h.lang(k, en, nl)


EN_OVERLAY = os.path.join("tools", "lang", "en", "c199_vadskracht_oud.json")


def _direct():
    A = os.path.join("src", "main", "resources", "assets", "guhs")
    for naam, wat in patch_alles().items():
        print(f"vadskracht_oud: {naam}: {wat}")
    NL, EN = {}, {}
    for k, nl in sign_text.TEXTS.items():
        if k.startswith(PREFIX + "."):
            NL[k] = nl
            if nl not in EN_REGELS:
                raise SystemExit(f"vadskracht_oud: no English for the sign line {nl!r}")
            EN[k] = EN_REGELS[nl]
    for k, (nl, en) in LANG.items():
        NL[k], EN[k] = nl, en
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    with open(EN_OVERLAY, "w", encoding="utf-8", newline="\n") as f:
        json.dump(dict(sorted(EN.items())), f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"vadskracht_oud: {len(NL)} texts")


# =====================================================================================================================
# a look at a template (for planning; python tools/features/vadskracht_oud.py --kaart <template> <y0> <y1> [x0 z0 x1 z1])
# =====================================================================================================================
def kaart(naam, y0, y1, vak=None):
    s = Sjabloon(naam, PREFIX)
    sx, sy, sz = s.grootte()
    x0, z0, x1, z1 = vak or (0, 0, sx - 1, sz - 1)
    print(naam, "size", (sx, sy, sz))
    tekens = {}
    for y in range(y1, y0 - 1, -1):
        print(f"--- y {y}")
        print("     " + "".join(str(x % 10) for x in range(x0, x1 + 1)))
        for z in range(z0, z1 + 1):
            rij = ""
            for x in range(x0, x1 + 1):
                n = s.get(x, y, z)
                if n is None:
                    rij += " "
                elif n == LUCHT:
                    rij += "."
                else:
                    if n not in tekens:
                        tekens[n] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#$%&*+=?@^~!"[len(tekens) % 72]
                    rij += tekens[n]
            print(f"{z:4d} {rij}")
    for n, t in tekens.items():
        print(t, n)


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--kaart":
        a = sys.argv[2:]
        kaart(a[0], int(a[1]), int(a[2]), tuple(int(v) for v in a[3:7]) if len(a) >= 7 else None)
    else:
        _direct()
