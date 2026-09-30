"""
Nomguh (3.0 Guhverhalen, slice balto) - the buildings of the town, placed by balto_bouw on its template:

  huisje(...)       a cosy log cottage with pastel walls, white window frames, a steep roof of snowy roof tiles with two pink
                    guh ears on the ridge, a chimney with a little smoke, a porch lamp, and a warm room inside (bed, table,
                    chairs, cupboard, carpet, a hanging lamp)
  ziekenhuisje(...) the white hospital with the big pink heart on its front gable: a waiting room, and the ward with the sick
                    guh babies (cradles, tiny guhs in sleeping bags who sneeze), Rosy's bed by the window, the nurse, the
                    EMPTY medicine cupboard
  stal(...)         the red sled-dog stable: big doors to the start line, six stalls with hay and water, four guh-sledehondjes,
                    harnesses on the wall, a sled in the corner, a hay loft
  berghut(...)      the mountain hut on the Nomguh peaks: a stone base, log walls, a porch, and the table with the medicine
                    chest
  boot(...)         the old boat stuck in the frozen bay: Baltoguh and Boris' home (a little cabin, a broken mast with a flag)
  iglo(...)         Muk and Luk's igloo by the ice pond (a fishing hole, a bucket of guh-visjes)
  plein(...)        the town square: pink cobbles, the big decorated sneeuwguhspar, benches, sneeuwpopguhs, the hot chocolate
                    kiosk and the EMPTY statue pedestal ("hier komt een held te staan...")
Every sign uses a translate key sign.guhs.balto.<key> (Dutch texts in TEKSTEN, exported by balto.py).
"""
import json
import math

AIR = "minecraft:air"
DAK_TRAP = "guhs:nomguh_sneeuwdak_trap"
DAK_PLAAT = "guhs:nomguh_sneeuwdak_plaat"
DAK = "guhs:nomguh_sneeuwdak"
SPOOR = "guhs:nomguh_sneeuwspoor"
KIST = "guhs:nomguh_medicijnkist"
KLINKERS = "guhs:knuffelklinkers"
VUURKORF = "guhs:elftocht_vuurkorf"

TEKSTEN = {}          # sign.guhs.balto.<key> -> Dutch text (filled while building)

DIRS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}   # an entity looking that way


def mc(n):
    return n if ":" in n else "minecraft:" + n


class Bouwer:
    """A thin helper around the template (make_structures.Structure) with the block states Nomguh needs."""

    def __init__(self, s, h, rng):
        self.s, self.h, self.rng = s, h, rng
        self.ms = h.ms
        self.npcs = []          # (kind, (x, y, z)) for the checks

    # --- plain blocks ---------------------------------------------------------------------------------------------------
    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def lucht(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, AIR)

    def log(self, x, y, z, hout="spruce", axis="y"):
        self.set(x, y, z, f"{hout}_log", {"axis": axis})

    def trap(self, x, y, z, name, facing, half="bottom", shape="straight"):
        self.set(x, y, z, name, {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"})

    def plaat(self, x, y, z, name, typ="bottom"):
        self.set(x, y, z, name, {"type": typ, "waterlogged": "false"})

    def deur(self, x, y, z, facing, hout="spruce", hinge="left", open_=False):
        for dy, half in ((0, "lower"), (1, "upper")):
            self.set(x, y + dy, z, f"{hout}_door", {"facing": facing, "half": half, "hinge": hinge, "open": str(open_).lower(), "powered": "false"})

    def raam(self, x, y, z, langs_x, glas="light_blue_stained_glass_pane"):
        """A glass pane in a wall running along x (langs_x) or along z."""
        self.set(x, y, z, glas, {"east": str(langs_x).lower(), "west": str(langs_x).lower(), "north": str(not langs_x).lower(),
                                 "south": str(not langs_x).lower(), "waterlogged": "false"})

    def hek(self, x, y, z, hout="spruce", **sides):
        props = {d: str(sides.get(d, False)).lower() for d in ("north", "south", "east", "west")}
        props["waterlogged"] = "false"
        self.set(x, y, z, f"{hout}_fence", props)

    def poort(self, x, y, z, facing, hout="spruce", open_=False):
        self.set(x, y, z, f"{hout}_fence_gate", {"facing": facing, "open": str(open_).lower(), "in_wall": "false", "powered": "false"})

    def lantaarn(self, x, y, z, hangend=False):
        self.set(x, y, z, "lantern", {"hanging": str(hangend).lower(), "waterlogged": "false"})

    def luik(self, x, y, z, facing, half="top", open_=True, hout="spruce"):
        self.set(x, y, z, f"{hout}_trapdoor", {"facing": facing, "half": half, "open": str(open_).lower(), "powered": "false", "waterlogged": "false"})

    def bed(self, x, y, z, facing, kleur="red"):
        """A bed whose head is at (x, y, z), its foot one step against facing (facing = where the head points)."""
        dx, dz = DIRS[facing]
        self.set(x, y, z, f"{kleur}_bed", {"facing": facing, "part": "head", "occupied": "false"})
        self.set(x - dx, y, z - dz, f"{kleur}_bed", {"facing": facing, "part": "foot", "occupied": "false"})

    def kampvuur(self, x, y, z, facing="north"):
        self.set(x, y, z, "campfire", {"facing": facing, "lit": "true", "signal_fire": "false", "waterlogged": "false"})

    def bord(self, x, y, z, facing, regels, muur=True, hout="spruce", kleur="black", glim=True):
        """A sign with Dutch lines: regels = [(key, text), ...] (translate keys sign.guhs.balto.<key>)."""
        blank = json.dumps("")
        msgs = []
        for key, text in regels:
            TEKSTEN[f"sign.guhs.balto.{key}"] = text
            msgs.append(json.dumps({"translate": f"sign.guhs.balto.{key}"}))
        msgs += [blank] * (4 - len(msgs))
        ms = self.ms
        text = {"messages": ms.NbtList(8, msgs), "color": kleur, "has_glowing_text": ms.Byte(1 if glim else 0)}
        nbt = {"id": "minecraft:sign", "is_waxed": ms.Byte(1), "front_text": text, "back_text": text}
        if muur:
            self.set(x, y, z, f"{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
            self.set(x, y, z, f"{hout}_sign", {"rotation": rot, "waterlogged": "false"}, nbt)

    def banier(self, x, y, z, basis, patronen, facing=None, rotatie=0):
        ms = self.ms
        nbt = {"id": "minecraft:banner", "patterns": ms.NbtList(10, [{"color": c, "pattern": "minecraft:" + p} for c, p in patronen])}
        if facing:
            self.set(x, y, z, f"{basis}_wall_banner", {"facing": facing}, nbt)
        else:
            self.set(x, y, z, f"{basis}_banner", {"rotation": str(rotatie % 16)}, nbt)

    # --- entities -------------------------------------------------------------------------------------------------------
    def npc(self, x, y, z, kind, kijk="south", roledata=None):
        ms = self.ms
        nbt = {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(YAW[kijk], 0.0)}
        if roledata:
            nbt["RoleData"] = roledata
        self.s.entity(x + 0.5, float(y), z + 0.5, nbt)
        self.npcs.append((kind, (x, y, z)))

    def guh(self, x, y, z, kijk="south", schaal=1.0, dx=0.5, dz=0.5, **extra):
        ms = self.ms
        nbt = ms.guh_nbt(schaal, Rotation=ms.floats(YAW[kijk] if isinstance(kijk, str) else float(kijk), 0.0), **extra)
        self.s.entity(x + dx, float(y), z + dz, nbt)
        return nbt


# =====================================================================================================================
# roofs
# =====================================================================================================================
def zadeldak(b, x0, z0, x1, z1, y0, langs_x, oren=True, gevel="spruce_planks", rond_raam=True):
    """A gable roof of snowy roof tiles over the box x0..x1 / z0..z1 (walls included), starting at y0 (the first roof row),
    the ridge along x (langs_x) or z, one block of overhang all round, gable ends filled with `gevel`, and two pink guh ears
    on the ridge ends. Returns the ridge y."""
    if langs_x:
        breed = z1 - z0 + 1
        half = breed // 2
        for i in range(half + (breed % 2)):
            y = y0 + i
            for x in range(x0 - 1, x1 + 2):
                if i < half or breed % 2 == 0:
                    b.trap(x, y, z0 - 1 + i, DAK_TRAP, "south")
                    b.trap(x, y, z1 + 1 - i, DAK_TRAP, "north")
                if breed % 2 == 1 and i == half:
                    b.plaat(x, y, z0 + half, DAK_PLAAT, "bottom")
                    b.set(x, y - 1, z0 + half, DAK)
            # the gable ends
            for z in range(z0 + i, z1 - i + 1):
                if y > y0 - 1 and z0 + i <= z <= z1 - i:
                    for x in (x0, x1):
                        if i < half or z != z0 + half or breed % 2 == 1:
                            b.set(x, y, z, gevel)
        top = y0 + half
        if rond_raam and breed >= 7:
            for x in (x0, x1):
                b.set(x, y0 + 1, z0 + half, "light_blue_stained_glass")
        if oren:
            for x in (x0 - 1, x1 + 1):
                ear(b, x, top + (0 if breed % 2 else -1), z0 + half, True)
        return top
    breed = x1 - x0 + 1
    half = breed // 2
    for i in range(half + (breed % 2)):
        y = y0 + i
        for z in range(z0 - 1, z1 + 2):
            if i < half or breed % 2 == 0:
                b.trap(x0 - 1 + i, y, z, DAK_TRAP, "east")
                b.trap(x1 + 1 - i, y, z, DAK_TRAP, "west")
            if breed % 2 == 1 and i == half:
                b.plaat(x0 + half, y, z, DAK_PLAAT, "bottom")
                b.set(x0 + half, y - 1, z, DAK)
        for x in range(x0 + i, x1 - i + 1):
            for z in (z0, z1):
                if i < half or x != x0 + half or breed % 2 == 1:
                    b.set(x, y, z, gevel)
    top = y0 + half
    if rond_raam and breed >= 7:
        for z in (z0, z1):
            b.set(x0 + half, y0 + 1, z, "light_blue_stained_glass")
    if oren:
        for z in (z0 - 1, z1 + 1):
            ear(b, x0 + half, top + (0 if breed % 2 else -1), z, False)
    return top


def ear(b, x, y, z, langs_x):
    """Two pink guh ears standing on the ridge end: rounded, two wide and three high, leaning a little outwards, with a
    lighter pink inside."""
    for kant in (-1, 1):
        for (d, dy, blok) in ((1, 1, "pink_wool"), (1, 2, "pink_concrete_powder"), (1, 3, "pink_wool"),
                              (2, 1, "pink_wool"), (2, 2, "pink_wool")):
            if langs_x:
                b.set(x, y + dy, z + kant * d, blok)
            else:
                b.set(x + kant * d, y + dy, z, blok)


def schoorsteen(b, x, z, y0, y1):
    """A stone chimney from y0 to y1 with a lit campfire on top (a thin line of smoke) under a little cap."""
    for y in range(y0, y1 + 1):
        b.set(x, y, z, "stone_bricks")
    b.kampvuur(x, y1 + 1, z)
    b.set(x, y1 + 2, z, "air")


# =====================================================================================================================
# the cottage
# =====================================================================================================================
MUREN = ["pink_terracotta", "light_blue_terracotta", "yellow_terracotta", "white_terracotta", "lime_terracotta", "orange_terracotta"]
TAPIJT = ["pink_carpet", "light_blue_carpet", "yellow_carpet", "lime_carpet", "magenta_carpet", "cyan_carpet"]


def huisje(b, x0, z0, W, D, deur, G, kleur, naam=None, bewoner=None):
    """A Nomguh cottage on the box x0..x0+W-1 / z0..z0+D-1 (the floor at G), its door on side `deur`."""
    rng = b.rng
    x1, z1 = x0 + W - 1, z0 + D - 1
    muur = MUREN[kleur % len(MUREN)]
    # the floor, the stone foundation, the walls
    b.fill(x0, G - 1, z0, x1, G - 1, z1, "cobblestone")
    b.fill(x0, G, z0, x1, G, z1, "stone_bricks")
    b.fill(x0 + 1, G, z0 + 1, x1 - 1, G, z1 - 1, "spruce_planks")
    b.lucht(x0 + 1, G + 1, z0 + 1, x1 - 1, G + 4, z1 - 1)
    for y in range(G + 1, G + 5):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                b.set(x, y, z, muur)
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                b.set(x, y, z, muur)
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            b.log(x, y, z)
    # a white band under the roof and white window frames
    for x in range(x0 + 1, x1):
        for z in (z0, z1):
            b.set(x, G + 4, z, "birch_planks")
    for z in range(z0 + 1, z1):
        for x in (x0, x1):
            b.set(x, G + 4, z, "birch_planks")
    # the door in the middle of its side, windows in the other walls
    dx, dz = DIRS[deur]
    if deur in ("north", "south"):
        deur_x, deur_z = x0 + W // 2, (z0 if deur == "north" else z1)
    else:
        deur_x, deur_z = (x0 if deur == "west" else x1), z0 + D // 2
    b.deur(deur_x, G + 1, deur_z, deur, hinge="left")
    for (side, langs_x) in (("north", True), ("south", True), ("west", False), ("east", False)):
        n = W if langs_x else D
        for i in range(2, n - 2, 3):
            if langs_x:
                x, z = x0 + i, (z0 if side == "north" else z1)
            else:
                x, z = (x0 if side == "west" else x1), z0 + i
            if (x, z) == (deur_x, deur_z) or abs((x - deur_x) + (z - deur_z)) <= 1 and side == deur:
                continue
            b.raam(x, G + 2, z, langs_x)
            b.raam(x, G + 3, z, langs_x)
            ox, oz = DIRS[side]
            # a little flower box under the window (a closed trapdoor), with a dusting of snow
            b.luik(x + ox, G + 1, z + oz, side, half="top", open_=False)
    # the roof (the ridge along the long side), guh ears, a chimney
    langs_x = W >= D
    top = zadeldak(b, x0, z0, x1, z1, G + 5, langs_x)
    schoorsteen(b, x1 - 2 if langs_x else x0 + 2, z0 + 2 if langs_x else z1 - 2, G + 1, top + 1)
    # the porch: a little awning over the door with two posts and a lamp
    px, pz = deur_x + dx, deur_z + dz
    for s in (-1, 1):
        qx, qz = (px + s, pz) if deur in ("north", "south") else (px, pz + s)
        for y in range(G + 1, G + 4):
            b.hek(qx, y, qz)
        b.plaat(qx, G + 4, qz, DAK_PLAAT)
    b.plaat(px, G + 4, pz, DAK_PLAAT)
    b.lantaarn(px, G + 3, pz, hangend=True)
    b.lucht(px, G + 1, pz, px, G + 2, pz)
    # inside: a bed, a table with chairs, a cupboard, a carpet and a hanging lamp
    ix0, iz0, ix1, iz1 = x0 + 1, z0 + 1, x1 - 1, z1 - 1
    b.fill(ix0 + 1, G + 1, iz0 + 1, ix1 - 1, G + 1, iz1 - 1, TAPIJT[kleur % len(TAPIJT)])
    far = OPP[deur]
    if deur in ("north", "south"):
        bz = iz1 if deur == "north" else iz0
        b.bed(ix0, G + 1, bz, "south" if deur == "north" else "north", kleur=["red", "pink", "light_blue", "yellow"][kleur % 4])
        b.set(ix1, G + 1, bz, "guhs:guh_kast", {"facing": deur, "open": "false"})
        b.set(ix1 - 1, G + 1, iz0 + (iz1 - iz0) // 2, "guhs:guh_tafel", {"facing": deur})
        b.set(ix1 - 1, G + 1, iz0 + (iz1 - iz0) // 2 - 1, "guhs:guh_stoel", {"facing": "south"})
    else:
        bx = ix1 if deur == "west" else ix0
        b.bed(bx, G + 1, iz0, "east" if deur == "west" else "west", kleur=["red", "pink", "light_blue", "yellow"][kleur % 4])
        b.set(bx, G + 1, iz1, "guhs:guh_kast", {"facing": deur, "open": "false"})
        b.set(ix0 + (ix1 - ix0) // 2, G + 1, iz1 - 1, "guhs:guh_tafel", {"facing": deur})
        b.set(ix0 + (ix1 - ix0) // 2 - 1, G + 1, iz1 - 1, "guhs:guh_stoel", {"facing": "east"})
    b.lantaarn(x0 + W // 2, G + 4, z0 + D // 2, hangend=True)
    # a name board beside the door
    if naam:
        sx, sz = (deur_x + 2, deur_z + dz) if deur in ("north", "south") else (deur_x + dx, deur_z + 2)
        b.bord(sx, G + 2, sz, deur, naam)
    if bewoner is not None:
        b.guh(ix0 + 1, G + 1, iz0 + 1, kijk=deur, schaal=bewoner)
    return (deur_x + dx, G + 1, deur_z + dz)


# =====================================================================================================================
# the ziekenhuisje
# =====================================================================================================================
def ziekenhuisje(b, x0, z0, G):
    """The white hospital, 23 x 19, its door to the west (the plaza). Returns (door spot, Rosy's spot)."""
    W, D = 23, 19
    x1, z1 = x0 + W - 1, z0 + D - 1
    b.fill(x0, G - 1, z0, x1, G - 1, z1, "cobblestone")
    b.fill(x0, G, z0, x1, G, z1, "smooth_quartz")
    b.fill(x0 + 1, G, z0 + 1, x1 - 1, G, z1 - 1, "white_terracotta")
    b.lucht(x0 + 1, G + 1, z0 + 1, x1 - 1, G + 5, z1 - 1)
    for y in range(G + 1, G + 6):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                b.set(x, y, z, "white_concrete")
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                b.set(x, y, z, "white_concrete")
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1), (x0 + 8, z0), (x0 + 8, z1)):
            b.set(x, y, z, "pink_concrete")
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            b.set(x, G + 5, z, "pink_concrete")
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            b.set(x, G + 5, z, "pink_concrete")
    # the inner wall between the waiting room (west) and the ward (east), with an open arch
    for y in range(G + 1, G + 5):
        for z in range(z0 + 1, z1):
            b.set(x0 + 8, y, z, "white_concrete" if not (z0 + 7 <= z <= z0 + 11 and y <= G + 3) else "air")
    b.set(x0 + 8, G + 4, z0 + 9, "pink_concrete")
    # windows all round (2 high), the door on the west side
    deur_z = z0 + D // 2
    for z in range(z0 + 2, z1 - 1, 3):
        if abs(z - deur_z) > 1:
            b.raam(x0, G + 2, z, False, "white_stained_glass_pane")
            b.raam(x0, G + 3, z, False, "white_stained_glass_pane")
        b.raam(x1, G + 2, z, False, "pink_stained_glass_pane")
        b.raam(x1, G + 3, z, False, "pink_stained_glass_pane")
    for x in range(x0 + 2, x1 - 1, 3):
        if x != x0 + 8:
            for z in (z0, z1):
                b.raam(x, G + 2, z, True, "pink_stained_glass_pane" if x > x0 + 8 else "white_stained_glass_pane")
                b.raam(x, G + 3, z, True, "pink_stained_glass_pane" if x > x0 + 8 else "white_stained_glass_pane")
    b.deur(x0, G + 1, deur_z, "west", hout="birch", hinge="left")
    b.deur(x0, G + 1, deur_z + 1, "west", hout="birch", hinge="right")
    # the roof: ridge along x, pink guh ears; the big pink heart on the west gable
    top = zadeldak(b, x0, z0, x1, z1, G + 6, True, gevel="white_concrete", rond_raam=False)
    hart(b, x0 - 1, G + 7, z0 + D // 2)
    schoorsteen(b, x1 - 3, z0 + 3, G + 1, top + 1)
    # the porch with a sign
    for z in (deur_z - 2, deur_z + 3):
        for y in range(G + 1, G + 4):
            b.set(x0 - 2, y, z, "birch_fence", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    for z in range(deur_z - 2, deur_z + 4):
        b.plaat(x0 - 2, G + 4, z, DAK_PLAAT)
        b.plaat(x0 - 1, G + 4, z, DAK_PLAAT)
    b.lantaarn(x0 - 1, G + 3, deur_z - 1, hangend=True)
    b.lantaarn(x0 - 1, G + 3, deur_z + 2, hangend=True)
    b.bord(x0 - 1, G + 5, deur_z, "west", [("zh1", "Ziekenhuisje"), ("zh2", "van Nomguh"), ("zh3", "♥ Stil, de guhbaby's")])
    b.bord(x0 - 1, G + 5, deur_z + 1, "west", [("zh4", "Bezoek welkom!"), ("zh5", "Knuffels ook.")])
    # the waiting room: chairs, a table with a teapot, plants, a desk
    for z in range(z0 + 2, z0 + 7):
        b.set(x0 + 2, G + 1, z, "guhs:guh_stoel", {"facing": "east"})
    b.set(x0 + 5, G + 1, z0 + 3, "guhs:guh_tafel", {"facing": "west"})
    b.set(x0 + 5, G + 2, z0 + 3, "guhs:theepotje", {"facing": "west"})
    for (x, z) in ((x0 + 1, z0 + 1), (x0 + 1, z1 - 1), (x0 + 7, z0 + 1), (x0 + 7, z1 - 1)):
        b.set(x, G + 1, z, "guhs:guh_bloempot", {"facing": "east", "gewaterd": "true"})
    b.set(x0 + 4, G + 1, z1 - 2, "guhs:guh_tafel", {"facing": "south"})
    b.bord(x0 + 4, G + 2, z1 - 1, "north", [("zh6", "Receptie"), ("zh7", "Even wachten,"), ("zh8", "njeg!")])
    # the ward: cradles with tucked-in babies along the south wall, sleeping bags with tiny sick guhs (they sneeze), Rosy's
    # bed by the east window, the empty medicine cupboard, a warm stove
    for i, x in enumerate(range(x0 + 10, x1 - 1, 3)):
        b.set(x, G + 1, z1 - 1, "guhs:guh_wiegje", {"facing": "north", "baby": "ingestopt", "wens": "geen"})
    for i, x in enumerate(range(x0 + 10, x0 + 17, 3)):
        b.set(x, G + 1, z0 + 1, "guhs:guh_slaapzak", {"facing": "south", "occupied": "false"})
        b.guh(x, G + 1, z0 + 1, kijk="south", schaal=0.42, NoAI=b.ms.Byte(1), Invulnerable=b.ms.Byte(1), Silent=b.ms.Byte(0),
              ClothesNeck="winter_scarf", NeoForgeData={"guhs_balto_ziek": b.ms.Byte(1)})
    rosy = (x1 - 3, G + 1, z0 + D // 2)
    b.npc(*rosy, "rosy", kijk="west")
    b.set(x1 - 1, G + 1, z0 + D // 2 - 2, "guhs:guh_bloempot", {"facing": "west", "gewaterd": "true"})
    b.set(x1 - 1, G + 1, z0 + D // 2 + 2, "guhs:guh_tafel", {"facing": "west"})
    b.set(x1 - 1, G + 2, z0 + D // 2 + 2, "guhs:theepotje", {"facing": "west"})
    # the medicine cupboard: empty shelves and a sad little sign
    for z in (z0 + 4, z0 + 5):
        b.set(x1 - 1, G + 1, z, "barrel", {"facing": "west", "open": "true"})
        b.set(x1 - 1, G + 2, z, "barrel", {"facing": "west", "open": "true"})
    b.bord(x1 - 2, G + 3, z0 + 4, "west", [("zh9", "Medicijnkast"), ("zh10", "LEEG..."), ("zh11", "njeg :(")])
    b.set(x1 - 1, G + 1, z0 + 2, "furnace", {"facing": "west", "lit": "true"})
    # the nurse guh (a doctor's coat and a stethoscope)
    nurse = b.guh(x0 + 12, G + 1, z0 + D // 2, kijk="east", schaal=1.0, NoAI=b.ms.Byte(1), Invulnerable=b.ms.Byte(1),
                  ClothesBody="doctor_coat", ClothesNeck="stethoscope", Variant="snow",
                  CustomName=json.dumps({"translate": "gui.guhs.balto.zuster"}), CustomNameVisible=b.ms.Byte(1))
    b.lantaarn(x0 + 14, G + 5, z0 + D // 2, hangend=True)
    b.lantaarn(x0 + 4, G + 5, z0 + D // 2, hangend=True)
    b.fill(x0 + 10, G + 1, z0 + 6, x1 - 5, G + 1, z1 - 6, "white_carpet")
    return (x0 - 1, G + 1, deur_z), rosy


def hart(b, x, y, z):
    """A big pink heart (two round guh-ear bumps) on the west gable (in the plane x = const), with a white plus."""
    rijen = ["..##.##..", ".#######.", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...."]
    for i, row in enumerate(rijen):
        for j, c in enumerate(row):
            if c == "#":
                b.set(x, y + len(rijen) - 1 - i, z - 4 + j, "pink_concrete")
    for (dy, dz) in ((4, 0), (5, 0), (3, 0), (4, -1), (4, 1)):
        b.set(x, y + dy, z + dz, "white_concrete")


# =====================================================================================================================
# the sled-dog stable
# =====================================================================================================================
def stal(b, x0, z0, G):
    """The red stable, 25 x 13, its big doors to the north (the start line). Returns the door spot (outside, middle)."""
    W, D = 25, 13
    x1, z1 = x0 + W - 1, z0 + D - 1
    b.fill(x0, G - 1, z0, x1, G - 1, z1, "cobblestone")
    b.fill(x0, G, z0, x1, G, z1, "spruce_planks")
    b.lucht(x0 + 1, G + 1, z0 + 1, x1 - 1, G + 6, z1 - 1)
    for y in range(G + 1, G + 7):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                b.set(x, y, z, "red_terracotta" if y > G + 1 else "stripped_spruce_log", {"axis": "x"} if y == G + 1 else None)
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                b.set(x, y, z, "red_terracotta" if y > G + 1 else "stripped_spruce_log", {"axis": "z"} if y == G + 1 else None)
        for x in range(x0, x1 + 1, 6):
            for z in (z0, z1):
                b.log(x, y, z)
        for z in (z0, z1):
            b.log(x0, y, z)
            b.log(x1, y, z)
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            b.set(x, G + 6, z, "white_terracotta")
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            b.set(x, G + 6, z, "white_terracotta")
    # the big doors (open, 5 wide) in the north wall, a white X on the closed barn doors beside them
    mid = x0 + W // 2
    b.lucht(mid - 2, G + 1, z0, mid + 2, G + 4, z0)
    for x in (mid - 3, mid + 3):
        for y in range(G + 1, G + 5):
            b.log(x, y, z0)
    for x in range(mid - 3, mid + 4):
        b.log(x, G + 5, z0, axis="x")
    b.bord(mid, G + 6, z0 - 1, "north", [("st1", "Sledehondenstal"), ("st2", "Nomguh"), ("st3", "sinds 1925 guhjaar")])
    # windows
    for x in range(x0 + 3, x1 - 1, 6):
        for z in (z0, z1):
            if abs(x - mid) > 3:
                b.raam(x, G + 3, z, True, "glass_pane")
                b.raam(x + 1, G + 3, z, True, "glass_pane")
    # the stalls: six boxes along the south wall with gates, hay and water; guh-sledehondjes in four of them
    honden = ["snow", "choco", "normal", "mint"]
    for i in range(6):
        sx = x0 + 2 + i * 4 if i < 3 else x0 + 3 + i * 4
        if abs(sx + 1 - mid) <= 2:
            sx += 3
        sx0, sx1 = sx, min(sx + 2, x1 - 1)
        for z in range(z1 - 4, z1):
            b.hek(sx0 - 1, G + 1, z, north=True, south=True)
        b.poort(sx0 + 1, G + 1, z1 - 5, "north")
        for x in range(sx0 - 1, sx1 + 2):
            if x != sx0 + 1 and x0 < x < x1:
                b.hek(x, G + 1, z1 - 5, east=True, west=True)
        b.set(sx0, G + 1, z1 - 1, "hay_block", {"axis": "x"})
        b.set(sx1, G + 1, z1 - 1, "cauldron")
        if i % 2 == 0:
            b.lantaarn(sx0 + 1, G + 4, z1 - 1, hangend=True)
        if i < len(honden):
            b.guh(sx0 + 1, G + 1, z1 - 3, kijk="north", schaal=0.95, Variant=honden[i], ClothesNeck="winter_scarf" if i % 2 else "red_bowtie")
    # a sled in the corner (spruce slabs on runners), harness chains on the wall, a hay loft over the west end
    for x in range(x0 + 2, x0 + 6):
        b.plaat(x, G + 1, z0 + 2, "spruce_slab")
        b.set(x, G + 1, z0 + 3, "spruce_trapdoor", {"facing": "north", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"})
    b.set(x0 + 1, G + 2, z0 + 2, "spruce_fence", {"north": "false", "south": "false", "east": "true", "west": "false", "waterlogged": "false"})
    for x in range(x1 - 6, x1):
        b.set(x, G + 3, z0 + 1, "chain", {"axis": "y", "waterlogged": "false"})
        b.set(x, G + 2, z0 + 1, "chain", {"axis": "y", "waterlogged": "false"})
    b.fill(x0 + 1, G + 4, z0 + 1, x0 + 6, G + 4, z1 - 1, "spruce_planks")
    b.fill(x0 + 1, G + 5, z0 + 1, x0 + 4, G + 5, z1 - 2, "hay_block", {"axis": "z"})
    for y in range(G + 1, G + 4):
        b.set(x0 + 7, y, z0 + 4, "ladder", {"facing": "east", "waterlogged": "false"})
    b.lantaarn(mid, G + 6, z0 + D // 2, hangend=True)
    b.lantaarn(mid - 8, G + 6, z0 + D // 2, hangend=True)
    b.lantaarn(mid + 8, G + 6, z0 + D // 2, hangend=True)
    top = zadeldak(b, x0, z0, x1, z1, G + 7, True, gevel="red_terracotta")
    # the weather vane: a guh-sled
    return (mid, G + 1, z0 - 1)


# =====================================================================================================================
# the berghut
# =====================================================================================================================
def berghut(b, x0, z0, Y):
    """The mountain hut (13 x 11), floor at Y, its door to the east. Returns (door spot outside, the chest's spot)."""
    W, D = 13, 11
    x1, z1 = x0 + W - 1, z0 + D - 1
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            for y in range(Y - 4, Y):
                b.set(x, y, z, "cobblestone" if (x + z + y) % 5 else "mossy_cobblestone")
    b.fill(x0, Y, z0, x1, Y, z1, "stone_bricks")
    b.fill(x0 + 1, Y, z0 + 1, x1 - 1, Y, z1 - 1, "spruce_planks")
    b.lucht(x0 + 1, Y + 1, z0 + 1, x1 - 1, Y + 4, z1 - 1)
    for y in range(Y + 1, Y + 5):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                b.log(x, y, z, axis="x")
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                b.log(x, y, z, axis="z")
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            b.log(x, y, z, "dark_oak")
    deur_z = z0 + D // 2
    b.deur(x1, Y + 1, deur_z, "east")
    for z in (z0 + 2, z1 - 2):
        b.raam(x1, Y + 2, z, False, "glass_pane")
        b.raam(x0, Y + 2, z, False, "glass_pane")
    for x in (x0 + 3, x1 - 3):
        b.raam(x, Y + 2, z0, True, "glass_pane")
        b.raam(x, Y + 2, z1, True, "glass_pane")
    top = zadeldak(b, x0, z0, x1, z1, Y + 5, True, gevel="spruce_planks")
    schoorsteen(b, x0 + 2, z0 + 2, Y + 1, top + 1)
    # the porch deck with a railing and a bench, a sign
    for z in range(z0 + 1, z1):
        b.plaat(x1 + 1, Y, z, "spruce_slab", "top")
        b.plaat(x1 + 2, Y, z, "spruce_slab", "top")
        b.set(x1 + 1, Y - 1, z, "spruce_planks")
        b.set(x1 + 2, Y - 1, z, "spruce_planks")
        b.lucht(x1 + 1, Y + 1, z, x1 + 2, Y + 3, z)
    for z in (z0 + 1, z1 - 1):
        for y in range(Y + 1, Y + 4):
            b.hek(x1 + 2, y, z)
        b.plaat(x1 + 1, Y + 4, z, DAK_PLAAT)
        b.plaat(x1 + 2, Y + 4, z, DAK_PLAAT)
    for z in range(z0 + 1, z1):
        b.plaat(x1 + 1, Y + 4, z, DAK_PLAAT)
        b.plaat(x1 + 2, Y + 4, z, DAK_PLAAT)
    b.lantaarn(x1 + 1, Y + 3, deur_z - 2, hangend=True)
    b.lantaarn(x1 + 1, Y + 3, deur_z + 2, hangend=True)
    b.set(x1 + 2, Y + 1, deur_z + 2, "guhs:guh_bank", {"facing": "west"})
    b.bord(x1 + 1, Y + 3, deur_z, "east", [("bh1", "Berghut"), ("bh2", "De Guhpiek"), ("bh3", "2112 guhmeter")])
    # inside: the table with the medicine chest, a stove, bunks, shelves, a warm carpet
    kist = (x0 + W // 2, Y + 2, deur_z)
    b.set(kist[0], Y + 1, kist[2], "guhs:guh_tafel", {"facing": "east"})
    b.fill(x0 + 3, Y + 1, z0 + 3, x1 - 3, Y + 1, z1 - 3, "red_carpet")
    b.set(kist[0], Y + 1, kist[2], "guhs:guh_tafel", {"facing": "east"})
    b.set(*kist, KIST, {"facing": "east"})
    b.set(kist[0] - 1, Y + 1, kist[2], "guhs:guh_stoel", {"facing": "east"})
    b.set(x0 + 1, Y + 1, z1 - 1, "furnace", {"facing": "east", "lit": "true"})
    b.bed(x0 + 1, Y + 1, z0 + 2, "north", kleur="light_blue")
    b.bed(x0 + 3, Y + 1, z0 + 2, "north", kleur="pink")
    for x in range(x0 + 5, x1):
        b.set(x, Y + 1, z0 + 1, "barrel", {"facing": "south", "open": "false"})
    b.set(x0 + 6, Y + 2, z0 + 1, "bookshelf")
    b.set(x0 + 7, Y + 2, z0 + 1, "bookshelf")
    b.lantaarn(x0 + W // 2, Y + 4, deur_z, hangend=True)
    b.bord(x0 + 4, Y + 3, z1 - 1, "north", [("bh4", "Medicijnkist"), ("bh5", "voor Nomguh."), ("bh6", "Voorzichtig, njeg!")])
    return (x1 + 3, Y + 1, deur_z), kist


# =====================================================================================================================
# the old boat on the frozen bay
# =====================================================================================================================
def boot(b, x0, z0, Y):
    """Baltoguh and Boris' old boat (bow to the north), 7 wide and 15 long, stuck in the ice at Y (the ice level)."""
    L, Bb = 15, 7
    mx = x0 + Bb // 2
    for i in range(L):
        z = z0 + i
        # the hull narrows to the bow (i small) and a little at the stern
        half = 1 if i == 0 else 2 if i < 3 else 3
        if i == L - 1:
            half = 2
        for y in range(Y - 1, Y + 3):
            h = half if y > Y - 1 else max(0, half - 1)
            for x in range(mx - h, mx + h + 1):
                edge = abs(x - mx) == h or i in (0, L - 1)
                if y == Y - 1 or edge:
                    b.set(x, y, z, "dark_oak_planks" if y == Y + 2 and edge else "spruce_planks")
                else:
                    b.set(x, y, z, "air")
        # the deck at Y+2 inside the hull (a few broken planks)
        for x in range(mx - half + 1, mx + half):
            if not (i in (5, 6) and x == mx + 1):
                b.set(x, Y + 1, z, "spruce_slab", {"type": "top", "waterlogged": "false"})
    # the cabin at the stern (5 x 4), a door to the bow, a window, a stove pipe
    cz0, cz1 = z0 + L - 6, z0 + L - 2
    for z in range(cz0, cz1 + 1):
        for x in range(mx - 2, mx + 3):
            for y in range(Y + 2, Y + 5):
                edge = x in (mx - 2, mx + 2) or z in (cz0, cz1)
                b.set(x, y, z, "spruce_planks" if edge else "air")
            b.set(x, Y + 5, z, "spruce_slab", {"type": "bottom", "waterlogged": "false"})
    b.deur(mx, Y + 2, cz0, "north")
    b.raam(mx - 2, Y + 3, cz0 + 2, False, "glass_pane")
    b.raam(mx + 2, Y + 3, cz0 + 2, False, "glass_pane")
    b.set(mx - 1, Y + 2, cz1 - 1, "hay_block", {"axis": "x"})          # Baltoguh's straw bed
    b.set(mx + 1, Y + 2, cz1 - 1, "guhs:guhnestje")
    b.lantaarn(mx, Y + 4, cz0 + 2, hangend=True)
    b.set(mx + 1, Y + 6, cz1 - 1, "cobblestone_wall", {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
    # the broken mast with a tattered pink flag
    for y in range(Y + 2, Y + 8):
        b.log(mx, y, z0 + 4)
    b.log(mx + 1, Y + 8, z0 + 4, axis="x")
    b.banier(mx - 1, Y + 7, z0 + 4, "pink", [("white", "stripe_middle"), ("magenta", "triangles_bottom")], facing="west")
    b.set(mx, Y + 2, z0 + 1, "chain", {"axis": "y", "waterlogged": "false"})
    b.bord(mx - 3, Y + 1, z0 + 8, "west", [("bo1", "De Oude Boot"), ("bo2", "Baltoguh & Boris"), ("bo3", "Gak! Welkom!")], muur=False)
    return (mx, Y + 2, z0 + 3), (mx - 5, Y + 1, z0 + 9)


# =====================================================================================================================
# Muk and Luk's igloo and the ice pond
# =====================================================================================================================
def iglo(b, cx, cz, Y):
    """A snow igloo (radius 4) with a tunnel to the east; inside a blue rug and a lantern."""
    R = 4.3
    for x in range(int(cx - R) - 1, int(cx + R) + 2):
        for z in range(int(cz - R) - 1, int(cz + R) + 2):
            for y in range(Y, Y + 6):
                d = math.dist((x, (y - Y) * 1.15, z), (cx, 0, cz))
                if d <= R and d > R - 1.1:
                    b.set(x, y, z, "snow_block")
                elif d <= R - 1.1:
                    b.set(x, y, z, "air")
    b.fill(cx - 2, Y - 1, cz - 2, cx + 2, Y - 1, cz + 2, "packed_ice")
    b.fill(cx - 2, Y, cz - 2, cx + 2, Y, cz + 2, "blue_carpet")
    for x in range(cx + 3, cx + 7):
        for z in (cz - 1, cz + 1):
            b.set(x, Y, z, "snow_block")
            b.set(x, Y + 1, z, "snow_block")
        b.set(x, Y + 2, cz, "snow_block")
        b.lucht(x, Y, cz, x, Y + 1, cz)
    b.lantaarn(cx, Y, cz - 2)
    b.set(cx - 2, Y, cz + 1, "guhs:blue_kussen", {"facing": "east"})
    b.set(cx - 2, Y, cz - 1, "guhs:light_blue_kussen", {"facing": "east"})


def vijver(b, cx, cz, r, Y):
    """The ice pond (ice at Y) with a round fishing hole, a bucket and a little sign."""
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.dist((x, z), (cx, cz))
            if d <= r:
                b.set(x, Y, z, "packed_ice" if (x * 7 + z * 3) % 5 else "blue_ice")
    # the fishing spot: a ring of blue ice round a dark hole, a barrel of guh-visjes and a little stool
    for (dx, dz) in ((1, 0), (3, 0), (2, -1), (2, 1)):
        b.set(cx + dx, Y, cz + dz, "blue_ice")
    b.set(cx + 2, Y, cz, "black_concrete_powder")
    b.set(cx + 2, Y + 1, cz + 2, "barrel", {"facing": "up", "open": "true"})
    b.set(cx + 4, Y + 1, cz - 1, "guhs:guh_stoel", {"facing": "west"})


# =====================================================================================================================
# the plaza
# =====================================================================================================================
def spar(b, x, y, z, hoog=11, versierd=False):
    """A sneeuwguhspar built into the template (like the worldgen tree: trunk with a face, a cone of snowy needles);
    versierd: lanterns and lampions in the branches and a golden star on top."""
    rng = b.rng
    naald = "guhs:sneeuwguhspar_naalden"
    for dy in range(hoog):
        if dy == 1:
            b.set(x, y + dy, z, "guhs:sneeuwguhspar_gezicht", {"facing": rng.choice(["north", "south", "east", "west"])})
        else:
            b.set(x, y + dy, z, "guhs:sneeuwguhspar_stam", {"axis": "y"})
    lagen = []
    breed = 4 if hoog >= 10 else 3
    for dy in range(2, hoog + 2):
        van_top = hoog + 1 - dy
        if van_top == 0:
            r = 0
        else:
            f = van_top / (hoog - 1)
            r = max(1, round(f * breed + (-0.6 if van_top % 2 == 0 else 0.4)))
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if dx == 0 and dz == 0 and dy < hoog:
                    continue
                d = math.hypot(dx, dz)
                if d > r + 0.35:
                    continue
                lagen.append((x + dx, y + dy, z + dz))
    lagen.append((x, y + hoog + 1, z))
    pos = set(lagen)
    for (px, py, pz) in lagen:
        sneeuw = (px, py + 1, pz) not in pos
        b.set(px, py, pz, naald, {"distance": "1", "persistent": "true", "waterlogged": "false", "sneeuw": str(sneeuw).lower()})
    if versierd:
        for (px, py, pz) in lagen:
            if (px, py + 1, pz) not in pos and (px - x) ** 2 + (pz - z) ** 2 >= 4 and rng.random() < 0.35:
                kind = rng.choice(["guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint", "lantern"])
                if kind == "lantern":
                    b.lantaarn(px, py + 1, pz)
                else:
                    b.set(px, py + 1, pz, kind, {"hanging": "false"})
        b.set(x, y + hoog + 2, z, "gold_block")
        b.lantaarn(x, y + hoog + 3, z)


def plein(b, cx, cz, G, r=13):
    """The round town square: pink cobbles with a snowy rim, the big decorated spar, benches, lamp posts, two
    sneeuwpopguhs, the hot chocolate kiosk and the empty statue pedestal. Returns the spots of interest."""
    rng = b.rng
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.dist((x, z), (cx, cz))
            if d <= r:
                b.set(x, G, z, KLINKERS if d < r - 1.2 or rng.random() < 0.5 else "snow_block")
                b.lucht(x, G + 1, z, x, G + 3, z)
    # the big tree north of the middle (the anchor is the middle of the square)
    spar(b, cx, G + 1, cz - 6, hoog=13, versierd=True)
    for (dx, dz) in ((-2, -6), (2, -6), (0, -8), (0, -4)):
        b.set(cx + dx, G + 1, cz + dz, "snow", {"layers": "2"})
    # benches round the tree, lamp posts on the rim
    for (dx, dz, f) in ((-4, -6, "east"), (4, -6, "west"), (0, -2, "north")):
        b.set(cx + dx, G + 1, cz + dz, "guhs:guh_bank", {"facing": f})
    for a in range(0, 360, 45):
        lx, lz = cx + round((r - 1) * math.cos(math.radians(a))), cz + round((r - 1) * math.sin(math.radians(a)))
        if abs(lz - cz) <= 1 and lx < cx or (a % 90 == 0):
            continue
        lamppaal(b, lx, G + 1, lz)
    # two sneeuwpopguhs
    for (dx, dz, f) in ((-7, 3, "south"), (7, 3, "south")):
        b.set(cx + dx, G + 1, cz + dz, "guhs:sneeuwpopguh", {"facing": f, "half": "lower"})
        b.set(cx + dx, G + 2, cz + dz, "guhs:sneeuwpopguh", {"facing": f, "half": "upper"})
    # the empty statue pedestal (the beeldje will come... a hero is still to be found)
    px, pz = cx, cz + 7
    b.fill(px - 1, G + 1, pz - 1, px + 1, G + 1, pz + 1, "polished_andesite")
    b.set(px, G + 2, pz, "chiseled_stone_bricks")
    b.set(px, G + 3, pz, "snow", {"layers": "1"})
    b.bord(px, G + 2, pz - 1, "north", [("pl1", "Hier komt"), ("pl2", "een held"), ("pl3", "te staan...")])
    # the hot chocolate kiosk (on the square's west rim)
    kx, kz = cx - 10, cz - 5
    for (x, z) in ((kx - 1, kz - 1), (kx + 1, kz - 1), (kx - 1, kz + 1), (kx + 1, kz + 1)):
        for y in range(G + 1, G + 4):
            b.hek(x, y, z, hout="dark_oak")
    for x in range(kx - 2, kx + 3):
        for z in range(kz - 2, kz + 3):
            b.set(x, G + 4, z, "red_wool" if (x + z) % 2 else "white_wool")
    b.set(kx, G + 1, kz, "barrel", {"facing": "up", "open": "false"})
    b.set(kx, G + 2, kz, "guhs:theepotje", {"facing": "east"})
    b.bord(kx + 2, G + 3, kz, "east", [("pl4", "Warme"), ("pl5", "chocomelk"), ("pl6", "gratis, njeg!")], muur=False)
    return {"sokkel": (px, G + 1, pz - 2), "kiosk": (kx + 2, G + 1, kz)}


def lamppaal(b, x, y, z, hoog=3):
    for dy in range(hoog):
        b.hek(x, y + dy, z, hout="dark_oak")
    b.lantaarn(x, y + hoog, z)
