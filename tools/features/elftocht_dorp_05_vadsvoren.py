"""Elf-Guhjestocht village 5: VADSVOREN - a little harbour with frozen-in boats and an ijszeiler.

A harbour basin opens onto the canal. Frozen in the ice lie the botter 'Vadse Mien' (brown guh sail with ears, leeboards)
and the kotter 'Knabbeltje' (wheelhouse with a guh face), and at the harbour mouth an ijszeiler waits for wind. Around
the basin: the Knabbelafslag (the knabbel auction hall with a guh clock gable and a cupola), the red-white Havenlicht
on the west pier with a glowing guh crystal lamp and ears, a boathouse with a little rowing boat, the havenmeester's
house, fishermen's huts, bollards, an anchor, crates and barrels. A kraam with warme chocovet & gerookte knabbels and
the Stempelpost on the east pier.
"""
INDEX = 5
NAAM = "Vadsvoren"

# ---------------------------------------------------------------------------------------------------------------------
# Bouwdoos (the same small toolkit in every village module of this builder, so each module stands on its own)
# ---------------------------------------------------------------------------------------------------------------------
import json
from collections import deque

try:  # imported as tools/features/elftocht_dorp_xx.py (package) or standalone with the api next to it
    from . import elftocht_dorp_api as api
except ImportError:
    try:
        from features import elftocht_dorp_api as api
    except ImportError:
        import elftocht_dorp_api as api

PLOT_X, PLOT_Y, PLOT_Z = api.PLOT_X, api.PLOT_Y, api.PLOT_Z
RAND = PLOT_Z - 1                      # the canal edge row (local z)
FACING = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
OPP = {"north": "south", "south": "north", "west": "east", "east": "west"}

# thin / partial blocks: never a full cube (substring match on the id without namespace)
_NIET_VOL = ("stairs", "_trap", "slab", "plaat", "fence", "_hek", "pane", "bars", "wall", "muur", "door", "deur", "luik",
             "sign", "banner", "lantern", "lampion", "torch", "carpet", "flower", "potted", "campfire", "chain", "vlaggetjes",
             "slinger", "button", "rail", "cauldron", "guh_bank", "guh_stoel", "guh_tafel", "xylofoon", "theepotje", "knuffel_",
             "sneeuwpop", "_bed", "end_rod", "candle", "cake", "ladder", "bell", "lever", "guhoortjes", "molentje",
             "ijspegelguh_kristal", "kristal_cluster", "sprietjes", "ijsbloempje", "poort", "gate", "anvil", "hopper",
             "flower_pot", "grindstone", "lectern", "guh_taart", "telescoop", "brewing", "sterrenlantaarn", "skull", "head",
             "tripwire", "string", "cobweb", "scaffolding", "zitzak", "kussen", "feestbuffettafel", "bloempot", "composter",
             "trapdoor", "pressure_plate", "note_block_x")
_LOOPBAAR = ("air", "snow", "carpet", "sign", "banner", "torch", "vlaggetjes", "slinger", "button", "rail", "pressure_plate",
             "door", "deur", "flower", "sprietjes", "ijsbloempje", "guhoortjes", "tripwire", "string", "kussen")
_VERBIND = ("fence", "_hek", "pane", "bars", "wall", "muur")


def _kort(name):
    return (name or "minecraft:air").split(":")[-1]


def is_vol(name):
    k = _kort(name)
    if k in ("air", "snow"):
        return False
    return not any(t in k for t in _NIET_VOL)


def is_vloer(name):
    """Something you can stand on (full blocks, slabs, stairs, ice)."""
    k = _kort(name)
    return is_vol(name) or "stairs" in k or "_trap" in k or "slab" in k or "plaat" in k or k in ("cauldron", "snow_block")


def is_loopbaar(name, props=None):
    """Air-like for feet/head: you can walk through it."""
    k = _kort(name)
    if k == "snow":
        return int((props or {}).get("layers", "1")) <= 2
    if "snow_block" in k or "trapdoor" in k or "luik" in k or "flower_pot" in k or "potted" in k:
        return False
    return any(t in k for t in _LOOPBAAR)


def _p(props):
    return {k: ("true" if v is True else "false" if v is False else str(v)) for k, v in (props or {}).items()}


class Dorp:
    def __init__(self, s, ox, oy, oz, naam):
        self.s, self.ox, self.oy, self.oz, self.naam = s, ox, oy, oz, naam
        self.lichten, self.boost, self.publiek, self.stempel = [], [], [], None
        self.mijn = set()
        self.gezichten = 0
        self.oren = 0

    # -- basics (all coordinates are LOCAL: x 0..27 west->east, z 0..23 north->south (23 = canal edge), y 0 = on the ground)
    def a(self, x, y, z):
        return self.ox + x, self.oy + y, self.oz + z

    def set(self, x, y, z, name, props=None, nbt=None):
        if not (0 <= x < PLOT_X and -3 <= y < PLOT_Y and 0 <= z < PLOT_Z):
            raise SystemExit(f"{self.naam}: {name} at local {(x, y, z)} is outside the plot")
        name = name if ":" in name else "minecraft:" + name
        self.s.set(self.ox + x, self.oy + y, self.oz + z, name, _p(props), nbt)
        self.mijn.add((x, y, z))
        if name.endswith("_gezicht"):
            self.gezichten += 1

    def get(self, x, y, z):
        return self.s.get(self.ox + x, self.oy + y, self.oz + z)

    def props(self, x, y, z):
        b = self.s.blocks.get(self.a(x, y, z))
        return b[1] if b else {}

    def leeg(self, x, y, z):
        return _kort(self.get(x, y, z)) == "air"

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def lucht(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, "air")

    # -- shapes
    def trap(self, x, y, z, name, facing, half="bottom", shape="straight"):
        self.set(x, y, z, name, {"facing": facing, "half": half, "shape": shape, "waterlogged": False})

    def plaat(self, x, y, z, name, type_="bottom"):
        self.set(x, y, z, name, {"type": type_, "waterlogged": False})

    def gezicht(self, x, y, z, facing, hout=False, stemming=None):
        stemming = (x * 7 + y * 3 + z * 5) % 4 if stemming is None else stemming
        self.set(x, y, z, "guhs:vadshout_gezicht" if hout else "guhs:knuffelsteen_gezicht",
                 {"facing": facing, "stemming": stemming})

    def oor(self, x, y, z, name="guhs:pluisdak", sneeuw=True):
        """One guh ear (a block sticking up), with a tiny snow cap."""
        self.set(x, y, z, name)
        self.oren += 1
        if sneeuw:
            self.sneeuw(x, y + 1, z, 1)

    def sneeuw(self, x, y, z, layers=1):
        if self.leeg(x, y, z) and is_vol(self.get(x, y - 1, z)):
            self.set(x, y, z, "snow", {"layers": layers})

    def deur(self, x, y, z, facing, name="guhs:vadshout_deur", hinge="left"):
        for half, dy in (("lower", 0), ("upper", 1)):
            self.set(x, y + dy, z, name, {"facing": facing, "half": half, "hinge": hinge, "open": False, "powered": False})

    def raam(self, x, y, z, vlak, hoog=2, glas="glass_pane"):
        """A window of `hoog` panes; vlak 'x' = in a wall running west-east (south/north facade), 'z' = east/west facade."""
        for dy in range(hoog):
            self.set(x, y + dy, z, glas)

    def lampion(self, x, y, z, kleur="geel", hangend=False):
        self.set(x, y, z, f"guhs:lampion_{kleur}", {"hanging": hangend, "waterlogged": False})
        self.lichten.append(self.a(x, y, z))

    def lantaarnpaal(self, x, z, kleur="geel", hoog=2, paal="polished_blackstone_wall", y=0):
        for dy in range(hoog):
            self.set(x, y + dy, z, paal)
        self.lampion(x, y + hoog, z, kleur)

    def vuurkorf(self, x, z, y=0):
        self.set(x, y, z, "cauldron")
        self.set(x, y + 1, z, "campfire", {"facing": "south", "lit": True, "signal_fire": False, "waterlogged": False})
        self.lichten.append(self.a(x, y + 1, z))

    def kampvuur(self, x, y, z):
        self.set(x, y, z, "campfire", {"facing": "south", "lit": True, "signal_fire": False, "waterlogged": False})
        self.lichten.append(self.a(x, y, z))

    def banier(self, x, y, z, facing, soort="nl", muur=True):
        """A flag: 'nl' (rood-wit-blauw), 'oranje', 'guh' (roze met een wit rondje), 'fries' (blauw-wit)."""
        from make_structures import NbtList
        kleur, lagen = {"nl": ("white", [("red", "stripe_top"), ("blue", "stripe_bottom")]),
                        "oranje": ("orange", [("white", "border"), ("yellow", "circle")]),
                        "guh": ("pink", [("white", "circle"), ("magenta", "triangles_top")]),
                        "fries": ("white", [("blue", "diagonal_left"), ("red", "flower")]),
                        "wit": ("white", [("pink", "circle")])}[soort]
        nbt = {"id": "minecraft:banner",
               "patterns": NbtList(10, [{"color": c, "pattern": "minecraft:" + p} for c, p in lagen])}
        if muur:
            self.set(x, y, z, f"{kleur}_wall_banner", {"facing": facing}, nbt)
        else:
            rot = {"south": 0, "west": 4, "north": 8, "east": 12}[facing]
            self.set(x, y, z, f"{kleur}_banner", {"rotation": rot}, nbt)

    def vlaggenmast(self, x, z, hoog=4, soort="nl", facing="south", y=0):
        self.set(x, y, z, "guhs:knuffelsteen_muur")
        for dy in range(1, hoog):
            self.set(x, y + dy, z, "guhs:knuffelsteen_muur")
        self.banier(x, y + hoog, z, facing, soort, muur=False)

    def vlaggetjes(self, x0, x1, y, z, axis="x", slinger=False):
        blok = "guhs:feestslingers" if slinger else "guhs:vlaggetjes"
        if axis == "x":
            for x in range(min(x0, x1), max(x0, x1) + 1):
                if self.leeg(x, y, z):
                    self.set(x, y, z, blok, {"axis": "x"})
        else:
            for zz in range(min(x0, x1), max(x0, x1) + 1):
                if self.leeg(z, y, zz):
                    self.set(z, y, zz, blok, {"axis": "z"})

    def bord(self, x, y, z, facing, regels, muur=True, hout="spruce", kleur="black", glim=True):
        from make_structures import NbtList, Byte
        msgs = [json.dumps({"text": r}) for r in regels] + [json.dumps("")] * (4 - len(regels))
        nbt = {"id": "minecraft:sign", "is_waxed": Byte(1),
               "front_text": {"messages": NbtList(8, msgs), "color": kleur, "has_glowing_text": Byte(1 if glim else 0)},
               "back_text": {"messages": NbtList(8, [json.dumps("")] * 4), "color": "black", "has_glowing_text": Byte(0)}}
        if muur:
            self.set(x, y, z, f"{hout}_wall_sign", {"facing": facing, "waterlogged": False}, nbt)
        else:
            rot = {"south": 0, "west": 4, "north": 8, "east": 12}[facing]
            self.set(x, y, z, f"{hout}_sign", {"rotation": rot, "waterlogged": False}, nbt)

    def bank(self, x, z, facing, y=0):
        self.set(x, y, z, "guhs:guh_bank", {"facing": facing})

    def sneeuwpop(self, x, z, facing="south", y=0):
        self.set(x, y, z, "guhs:sneeuwpopguh", {"facing": facing, "half": "lower"})
        self.set(x, y + 1, z, "guhs:sneeuwpopguh", {"facing": facing, "half": "upper"})

    def knotwilg(self, x, z, hoog=3, y=0):
        """A pollard willow: a stubby trunk with a knob and a broom of frosty twigs with snow caps on the top twigs."""
        blad = "guhs:knotwilg_bladeren"
        for dy in range(hoog + 1):
            self.set(x, y + dy, z, "guhs:knotwilg_stam", {"axis": "y"})
        top = y + hoog
        rondom = [(1, 0), (-1, 0), (0, 1), (0, -1)]
        schuin = [(1, 1), (-1, -1), (1, -1), (-1, 1)]
        twijg = {}
        for dx, dz in rondom + schuin[: 2 + (x + z) % 3]:
            twijg[(x + dx, top, z + dz)] = True
        for dx, dz in rondom:
            twijg[(x + dx, top + 1, z + dz)] = True
        twijg[(x, top + 1, z)] = True
        for dx, dz in schuin[(x + z) % 2::2]:
            twijg[(x + dx, top + 2, z + dz)] = False
        twijg[(x, top + 2, z)] = True
        for (tx, ty, tz) in twijg:
            if 0 <= tx < PLOT_X and 0 <= tz < PLOT_Z and self.leeg(tx, ty, tz):
                boven = (tx, ty + 1, tz) in twijg
                self.set(tx, ty, tz, blad, {"persistent": True, "distance": 1, "waterlogged": False, "sneeuw": not boven})
        # the loose top twigs need a neighbour: tie them to the centre twig
        for dx, dz in schuin:
            if (x + dx, top + 2, z + dz) in twijg and not self.leeg(x + dx, top + 2, z + dz):
                if self.leeg(x + dx, top + 1, z + dz):
                    self.set(x + dx, top + 1, z + dz, blad, {"persistent": True, "distance": 1, "waterlogged": False,
                                                             "sneeuw": False})

    def paaltje(self, x, z, y=0):
        """A guh bollard: a little knuffelsteen post with a glowing ijspegelguh ear on top."""
        self.set(x, y, z, "guhs:knuffelsteen")
        self.ijsoor(x, y + 1, z, "up")

    def ijsoor(self, x, y, z, facing="up"):
        """An ijspegelguh-kristal: a glowing ice crystal shaped like a guh ear (grows on the block behind/below it)."""
        self.set(x, y, z, "guhs:ijspegelguh_kristal", {"facing": facing, "waterlogged": False})
        self.lichten.append(self.a(x, y, z))

    def sneeuwdek(self, kans=0.3, zaad=1):
        """Soft snow patches on the open ground (not on ice or paved klinkers), in little clusters."""
        import math as _m
        for x in range(PLOT_X):
            for z in range(PLOT_Z):
                onder = _kort(self.get(x, -1, z))
                if onder in ("polderijs", "knuffelklinkers", "smooth_stone", "air") or "planken" in onder or "planks" in onder:
                    continue
                v = _m.sin(x * 0.9 + zaad) * _m.cos(z * 0.7 - zaad) + _m.sin((x + z) * 0.43 + zaad * 2) * 0.6
                if v > 1.0 - kans * 2:
                    self.sneeuw(x, 0, z, 2 if v > 1.25 - kans else 1)

    def pot(self, x, y, z, plant="guhoortjes"):
        self.set(x, y, z, f"guhs:potted_{plant}" if plant in ("guhoortjes", "kaasbloem", "knabbelroos", "roze_guhbloem")
                 else f"potted_{plant}")

    def knuffel(self, x, y, z, soort, facing="south"):
        self.set(x, y, z, f"guhs:knuffel_{soort}", {"facing": facing})

    # -- NPCs
    def _tags_nbt(self):
        """The api helpers put Tags in a plain python list, which make_structures' NBT writer can't save: make it a
        TAG_List of strings (harmless if the api already does it)."""
        from make_structures import NbtList
        nbt = self.s.entities[-1][3]
        if "Tags" in nbt and not isinstance(nbt["Tags"], NbtList):
            nbt["Tags"] = NbtList(8, list(nbt["Tags"]))

    def stempelguh(self, x, y, z):
        self.stempel = (x, y, z)
        api.stempelguh(self.s, self.ox + x, self.oy + y, self.oz + z, 0, INDEX)
        self._tags_nbt()

    def toeschouwer(self, x, y, z, yaw=0):
        self.publiek.append((x, y, z, yaw))
        api.publiek(self.s, self.ox + x, self.oy + y, self.oz + z, yaw)
        self._tags_nbt()

    # -- roofs and gables
    def zadeldak(self, x0, x1, z0, z1, y, mat="guhs:pluisdak_trap", nok="guhs:pluisdak", as_="z", sneeuw=True):
        """A gable roof over x0..x1 (ridge along z) or z0..z1 (ridge along x); hollow, stairs + a full ridge."""
        k = 0
        while True:
            if as_ == "z":
                a, b = x0 + k, x1 - k
                if a > b:
                    break
                for z in range(z0, z1 + 1):
                    if a == b:
                        self.set(a, y + k, z, nok)
                        if sneeuw:
                            self.sneeuw(a, y + k + 1, z, 2)
                    else:
                        self.trap(a, y + k, z, mat, "east")
                        self.trap(b, y + k, z, mat, "west")
                        if a + 1 == b:
                            self.plaat(a, y + k + 1, z, nok.replace("pluisdak", "pluisdak_plaat") if "pluisdak" in nok else nok)
            else:
                a, b = z0 + k, z1 - k
                if a > b:
                    break
                for x in range(x0, x1 + 1):
                    if a == b:
                        self.set(x, y + k, a, nok)
                        if sneeuw:
                            self.sneeuw(x, y + k + 1, a, 2)
                    else:
                        self.trap(x, y + k, a, mat, "south")
                        self.trap(x, y + k, b, mat, "north")
            k += 1
        return y + k

    def guhgevel(self, x0, x1, z, y, muur, face_facing, stijl="trap", oor="guhs:pluisdak", hout=False, rand=None, vlak="x"):
        """The gable above the cornice (y): a triangle that follows a zadeldak plus a 'guh head' on top: a 3 wide crown
        with a guh face and two snowy ears. stijl: trap (step slabs), hals (stair shoulders), klok (bell).
        vlak 'x': the gable runs x0..x1 at row z (roof ridge along z); vlak 'z': it runs z x0..x1 at column x=z."""
        def P(a, yy):
            return (a, yy, z) if vlak == "x" else (z, yy, a)
        links, rechts = ("east", "west") if vlak == "x" else ("south", "north")
        w = x1 - x0 + 1
        m = (w - 1) // 2
        xm = x0 + m
        for k in range(m + 1):
            for a in range(x0 + k, x1 - k + 1):
                self.set(*P(a, y + k), muur)
        top = y + m
        for a in (xm - 1, xm + 1):
            self.set(*P(a, top), muur)
        self.gezicht(*P(xm, top), face_facing, hout=hout)
        self.oor(*P(xm - 1, top + 1), oor)
        self.oor(*P(xm + 1, top + 1), oor)
        trim = rand or "guhs:knuffelsteen_plaat"
        trim_trap = trim.replace("_plaat", "_trap").replace("_slab", "_stairs")
        for k in range(m - 1):
            la, ra = x0 + k, x1 - k
            if stijl == "trap":
                self.plaat(*P(la, y + k + 1), trim)
                self.plaat(*P(ra, y + k + 1), trim)
            else:
                self.trap(*P(la, y + k + 1), trim_trap, links)
                self.trap(*P(ra, y + k + 1), trim_trap, rechts)
        return top + 1

    # -- a canal house (front to the south or north), hollow, with lit floors
    def pand(self, x0, z0, w, d, hoog, muur, voor="south", stijl="trap", deur_x=None, deurblok="guhs:vadshout_deur",
             dak="guhs:pluisdak_trap", nok="guhs:pluisdak", kozijn="smooth_quartz", oor="guhs:pluisdak", hout_gezicht=False,
             glas="glass_pane", licht="geel", vlag=None, trim=None, meubels=True):
        x1, z1 = x0 + w - 1, z0 + d - 1
        zf = z1 if voor == "south" else z0            # the facade row
        zb = z0 if voor == "south" else z1
        inw = -1 if voor == "south" else 1            # step from the facade into the house
        # shell
        for y in range(0, hoog):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.set(x, y, z, muur)
                    else:
                        self.set(x, y, z, "air")
        # floors every 4 blocks, a lampion under each ceiling
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                self.set(x, -1, z, "guhs:vadshout_planken")
        for f in range(4, hoog - 2, 4):
            for x in range(x0 + 1, x1):
                for z in range(z0 + 1, z1):
                    self.set(x, f, z, "guhs:vadshout_planken")
        for f in list(range(4, hoog - 2, 4)) + [hoog]:
            self.lampion(x0 + w // 2, f - 1, (z0 + z1) // 2, licht, hangend=True)
        # ceiling under the roof so the lampion of the top floor hangs
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                self.set(x, hoog, z, "guhs:vadshout_planken")
        # cornice
        for x in range(x0, x1 + 1):
            self.set(x, hoog, zf, trim or muur)
        # windows on every floor of the front and back, door on the ground floor
        deur_x = x0 + w // 2 if deur_x is None else deur_x
        cols = [x for x in range(x0 + 1, x1) if (x - x0) % 2 == 1] if w > 3 else [x0 + 1]
        for f in range(0, hoog - 2, 4):
            for x in cols:
                if f == 0 and abs(x - deur_x) < 1:
                    continue
                self.raam(x, f + 1, zf, "x", glas=glas)
                self.raam(x, f + 1, zb, "x", glas=glas)
                if kozijn and f + 3 < hoog:
                    self.set(x, f + 3, zf, kozijn)
                    self.set(x, f + 3, zb, kozijn)
        self.deur(deur_x, 0, zf, "north" if voor == "south" else "south", deurblok)
        # a little canopy above the door + transom light
        self.set(deur_x, 2, zf, "glass")
        # side windows on the upper floors (only if the side is free)
        for f in range(4, hoog - 2, 4):
            for x in (x0, x1):
                for z in range(z0 + 1, z1):
                    if (z - z0) % 2 == 1:
                        side = x - 1 if x == x0 else x + 1
                        if 0 <= side < PLOT_X and self.leeg(side, f + 1, z):
                            self.raam(x, f + 1, z, "z", glas=glas)
        # roof + gables
        self.zadeldak(x0, x1, z0 + 1, z1 - 1, hoog + 1, dak, nok, "z")
        self.guhgevel(x0, x1, zf, hoog + 1, muur, voor, stijl, oor, hout_gezicht, rand=trim)
        self.guhgevel(x0, x1, zb, hoog + 1, muur, OPP[voor], stijl, oor, hout_gezicht, rand=trim)
        if vlag:
            vx = x0 + w // 2 if (w // 2) % 2 == 0 else x0 + w // 2 - 1
            self.banier(vx, hoog - 2, zf - inw, voor, vlag)
        if meubels and w >= 5 and d >= 5:
            self.set(x0 + 1, 0, zb - inw * 1, "guhs:guh_tafel", {"facing": voor})
            if d >= 6:
                self.set(x0 + 1, 0, zb - inw * 2, "guhs:guh_stoel", {"facing": OPP[voor]})
            self.set(x1 - 1, 0, zb - inw, "guhs:guh_kast", {"facing": voor, "open": False})
        return deur_x

    # -- a market stall (w wide, 3 deep) facing south or north: barrels at the back, a counter with goods in front,
    #    fence posts, a striped wool awning with carpet stripes, a glowing sign and a lampion under the awning
    def kraam(self, x0, z0, w, kleur1, kleur2, regels, waar=(), naar="south", lampion="geel", boost=True):
        x1 = x0 + w - 1
        zf = z0 + 2 if naar == "south" else z0
        zb = z0 if naar == "south" else z0 + 2
        zm = z0 + 1
        uit = 1 if naar == "south" else -1
        for x in range(x0, x1 + 1):
            self.set(x, 0, zb, "barrel", {"facing": "up", "open": False})
            self.set(x, 0, zf, "guhs:vadshout_planken")
            self.set(x, 0, zm, "air" if x0 < x < x1 else "guhs:vadshout_hek")
        goods = list(waar)
        for i, x in enumerate(range(x0 + 1, x1)):
            blok = goods[i] if i < len(goods) else None
            if blok:
                self.set(x, 1, zf, blok[0], blok[1] if len(blok) > 1 else None)
            else:
                self.plaat(x, 1, zf, "guhs:vadshout_plaat")
            if i % 2 == 0:
                self.set(x, 1, zb, "guhs:knuffel_normal" if i % 4 == 0 else "guhs:theepotje", {"facing": naar})
        for x in (x0, x1):
            for z in (zb, zm, zf):
                for y in (1, 2):
                    self.set(x, y, z, "guhs:vadshout_hek")
        for x in range(x0, x1 + 1):
            kl = kleur1 if (x - x0) % 2 == 0 else kleur2
            for z in range(z0, z0 + 3):
                self.set(x, 3, z, f"{kl}_wool")
            self.set(x, 4, zm, f"{kleur2 if (x - x0) % 2 == 0 else kleur1}_carpet")
        self.lampion(x0 + w // 2, 2, zm, lampion, hangend=True)
        self.bord(x0 + w // 2, 3, zf + uit, naar, regels)
        if boost:
            self.boost.append(self.a(x0 + w // 2, 0, zf))
        return (x0 + w // 2, 0, zm)

    # -- finishing: connect fences / panes / walls / bars to their neighbours
    def verbind(self):
        for (x, y, z) in sorted(self.mijn):
            name = self.get(x, y, z)
            k = _kort(name)
            if not any(t in k for t in _VERBIND) or "sign" in k or "banner" in k:
                continue
            is_muur = ("wall" in k or "muur" in k)
            pr = {}
            con = {}
            for side, (dx, dz) in FACING.items():
                n = self.get(x + dx, y, z + dz) if 0 <= x + dx < PLOT_X and 0 <= z + dz < PLOT_Z else None
                nk = _kort(n)
                c = bool(n) and (is_vol(n) or any(t in nk for t in _VERBIND) and "sign" not in nk and "banner" not in nk
                                 or "gate" in nk or "poort" in nk)
                if "pane" in k or "bars" in k:
                    c = c and ("pane" in nk or "bars" in nk or is_vol(n))
                elif "fence" in k or "_hek" in k:
                    c = c and ("fence" in nk or "_hek" in nk or is_vol(n) or "gate" in nk or "poort" in nk)
                con[side] = c
            if is_muur:
                boven = self.get(x, y + 1, z)
                for side, c in con.items():
                    pr[side] = ("tall" if boven and is_vol(boven) else "low") if c else "none"
                recht = (con["north"] and con["south"] and not con["east"] and not con["west"]) or \
                        (con["east"] and con["west"] and not con["north"] and not con["south"])
                pr["up"] = not recht or bool(boven and not _kort(boven) == "air")
            else:
                for side, c in con.items():
                    pr[side] = c
            pr["waterlogged"] = False
            self.s.blocks[self.a(x, y, z)] = (name, _p(pr), None)

    def resultaat(self):
        self.verbind()
        if self.stempel is None:
            raise SystemExit(f"{self.naam}: no Stempelguh")
        sx, sy, sz = self.stempel
        return {"stempelguh": (*self.a(sx, sy, sz), 0),
                "publiek": [(*self.a(x, y, z), yaw) for x, y, z, yaw in self.publiek],
                "boost": list(self.boost),
                "lichten": list(self.lichten)}


# ---------------------------------------------------------------------------------------------------------------------
# De zelfcontrole
# ---------------------------------------------------------------------------------------------------------------------
def _blok(s, x, y, z):
    b = s.blocks.get((x, y, z))
    return (b[0], b[1]) if b else ("minecraft:air", {})


def controleer(s, ox, oy, oz, naam, index, min_gezichten=8):
    problemen = []

    def loc(p):
        return p[0] - ox, p[1] - oy, p[2] - oz

    def binnen(x, y, z):
        return ox <= x < ox + PLOT_X and oy - 3 <= y < oy + PLOT_Y and oz <= z < oz + PLOT_Z

    def loopbaar(x, y, z):
        n, pr = _blok(s, x, y, z)
        return is_loopbaar(n, pr)

    def vloer(x, y, z):
        return is_vloer(_blok(s, x, y, z)[0])

    def staanbaar(x, y, z):
        return binnen(x, y, z) and loopbaar(x, y, z) and loopbaar(x, y + 1, z) and vloer(x, y - 1, z)

    # walk from the canal edge
    starts = [(x, oy, oz + RAND) for x in range(ox, ox + PLOT_X) if staanbaar(x, oy, oz + RAND)]
    if not starts:
        problemen.append(f"{naam}: nothing walkable on the canal edge row")
    seen, todo = set(starts), deque(starts)
    while todo:
        x, y, z = todo.popleft()
        for dx, dz in FACING.values():
            for dy in (0, 1, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if dy == 1 and not loopbaar(x, y + 2, z):
                    continue
                if dy < 0 and any(not loopbaar(x + dx, y + k, z + dz) for k in range(dy + 1, 2)):
                    continue
                if staanbaar(*n):
                    if n not in seen:
                        seen.add(n)
                        todo.append(n)
                    break
    # the Stempelguh
    stempels = [e for e in s.entities if f"guhs_elftocht_dorp_{index}" in (e[3].get("Tags") or [])]
    if len(stempels) != 1:
        problemen.append(f"{naam}: {len(stempels)} Stempelguhs")
    for e in stempels:
        p = (int(e[0] - 0.5), int(e[1]), int(e[2] - 0.5))
        if not staanbaar(*p):
            problemen.append(f"{naam}: Stempelguh at {loc(p)} doesn't stand free on solid ground")
        if p not in seen:
            problemen.append(f"{naam}: Stempelguh at {loc(p)} can't be reached from the canal edge")
        if loc(p)[2] < RAND - 3:
            problemen.append(f"{naam}: Stempelguh at {loc(p)} is more than 3 blocks from the canal edge")
        if e[3].get("Kind") != "stempelguh":
            problemen.append(f"{naam}: Stempelguh kind {e[3].get('Kind')}")
    publiek = [e for e in s.entities if "guhs_elftocht_publiek" in (e[3].get("Tags") or [])
               and binnen(int(e[0] - 0.5), int(e[1]), int(e[2] - 0.5))]
    if not 2 <= len(publiek) <= 8:
        problemen.append(f"{naam}: {len(publiek)} audience guhs (2..8)")
    for e in publiek + stempels:
        p = (int(e[0] - 0.5), int(e[1]), int(e[2] - 0.5))
        if not binnen(*p):
            problemen.append(f"{naam}: entity at {loc(p)} outside the plot")
        if not staanbaar(*p):
            problemen.append(f"{naam}: guh at {loc(p)} doesn't stand free on solid ground")
    # every block in the plot box
    echte = set()
    gezichten = oren = 0
    for (x, y, z), (n, pr, nbt) in s.blocks.items():
        if not binnen(x, y, z) or _kort(n) == "air":
            continue
        echte.add((x, y, z))
        k = _kort(n)
        l = loc((x, y, z))
        if k.endswith("_gezicht"):
            gezichten += 1
        if ("door" in k or "deur" in k) and "trap" not in k:
            if pr.get("half") == "lower":
                bo, bpr = _blok(s, x, y + 1, z)
                if bo != n or bpr.get("half") != "upper" or not vloer(x, y - 1, z):
                    problemen.append(f"{naam}: door at {l} is broken")
                if (x, y, z) not in seen and not any((x + dx, y, z + dz) in seen for dx, dz in FACING.values()):
                    problemen.append(f"{naam}: door at {l} can't be reached")
            elif pr.get("half") == "upper":
                if _blok(s, x, y - 1, z)[0] != n:
                    problemen.append(f"{naam}: door top at {l} has no bottom")
        if "lampion" in k or "lantern" in k:
            if pr.get("hanging") == "true":
                bo = _kort(_blok(s, x, y + 1, z)[0])
                if not (is_vloer(_blok(s, x, y + 1, z)[0]) or any(t in bo for t in ("fence", "_hek", "wall", "muur", "chain"))):
                    problemen.append(f"{naam}: hanging lampion at {l} hangs on {bo}")
            else:
                on = _kort(_blok(s, x, y - 1, z)[0])
                if not (vloer(x, y - 1, z) or any(t in on for t in ("fence", "_hek", "wall", "muur", "chain"))):
                    problemen.append(f"{naam}: lampion at {l} stands on {on}")
        if k == "ijspegelguh_kristal":
            f = pr.get("facing", "up")
            d3 = {"up": (0, -1, 0), "down": (0, 1, 0), "north": (0, 0, 1), "south": (0, 0, -1), "west": (1, 0, 0),
                  "east": (-1, 0, 0)}[f]
            if not is_vol(_blok(s, x + d3[0], y + d3[1], z + d3[2])[0]):
                problemen.append(f"{naam}: ijsoor at {l} grows on {_blok(s, x + d3[0], y + d3[1], z + d3[2])[0]}")
        if k == "snow" and not is_vol(_blok(s, x, y - 1, z)[0]):
            problemen.append(f"{naam}: snow at {l} lies on {_blok(s, x, y - 1, z)[0]}")
        if k.endswith("_wall_banner") or k.endswith("_wall_sign"):
            dx, dz = FACING[pr["facing"]]
            if not is_vol(_blok(s, x - dx, y, z - dz)[0]):
                problemen.append(f"{naam}: {k} at {l} hangs on {_blok(s, x - dx, y, z - dz)[0]}")
        elif k.endswith("_banner") or (k.endswith("_sign") and "hanging" not in k):
            on = _kort(_blok(s, x, y - 1, z)[0])
            if not (vloer(x, y - 1, z) or any(t in on for t in ("fence", "_hek", "wall", "muur"))):
                problemen.append(f"{naam}: {k} at {l} stands on {on}")
        if any(t in k for t in ("potted", "guh_bank", "guh_stoel", "guh_tafel", "sneeuwpopguh", "knuffel_", "barrel",
                                "xylofoon", "theepotje", "guh_taart", "campfire", "cauldron", "note_block", "jukebox")):
            if not (vloer(x, y - 1, z) or _kort(_blok(s, x, y - 1, z)[0]) in ("sneeuwpopguh", "cauldron", "guh_tafel")):
                problemen.append(f"{naam}: {k} at {l} stands on {_blok(s, x, y - 1, z)[0]}")
        if k in ("sneeuwpopguh",) and pr.get("half") == "lower" and _kort(_blok(s, x, y + 1, z)[0]) != "sneeuwpopguh":
            problemen.append(f"{naam}: snowman at {l} has no head")
    # nothing floats: every group of blocks reaches the ground layer (y <= oy-1)
    gezien = set()
    for p in echte:
        if p in gezien:
            continue
        comp, stapel, grond = [], [p], False
        gezien.add(p)
        while stapel:
            q = stapel.pop()
            comp.append(q)
            if q[1] <= oy - 1:
                grond = True
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (q[0] + d[0], q[1] + d[1], q[2] + d[2])
                if n in echte and n not in gezien:
                    gezien.add(n)
                    stapel.append(n)
        if not grond:
            f = sorted(comp)[0]
            problemen.append(f"{naam}: {len(comp)} floating blocks from {loc(f)} ({_blok(s, *f)[0]})")
    if gezichten < min_gezichten:
        problemen.append(f"{naam}: only {gezichten} guh faces")
    if problemen:
        raise SystemExit("\n".join(problemen[:40]))
    return {"loopbaar": len(seen), "gezichten": gezichten, "blokken": len(echte)}

# ---------------------------------------------------------------------------------------------------------------------
# Vadsvoren
# ---------------------------------------------------------------------------------------------------------------------
IJS = "guhs:polderijs"
KS = "guhs:knuffelsteen"
KLINK = "guhs:knuffelklinkers"
HAVEN = (8, 19, 7)       # basin x0, x1, from z0 down to the canal


def _haven(d):
    x0, x1, z0 = HAVEN
    for x in range(x0, x1 + 1):
        for z in range(z0, PLOT_Z):
            d.set(x, -1, z, IJS)
    # quays: klinkers with a stone edge; bollards
    for x in range(PLOT_X):
        for z in range(PLOT_Z):
            if x0 <= x <= x1 and z >= z0:
                continue
            rand = (x in (x0 - 1, x1 + 1) and z >= z0) or (z == z0 - 1 and x0 - 1 <= x <= x1 + 1)
            if rand:
                d.set(x, -1, z, "smooth_stone")
            elif z >= 6 or 7 <= x <= 20:
                d.set(x, -1, z, KLINK if (x * 2 + z) % 6 else KS)
    for x, z in ((x0 - 1, 10), (x0 - 1, 16), (x1 + 1, 10), (x1 + 1, 16), (11, z0 - 1), (16, z0 - 1)):
        d.paaltje(x, z)


def _botter(d):
    """De Vadse Mien: a botter with a brown guh sail (face + ears), leeboards, a pennant."""
    x, z0, z1 = 11, 9, 16
    for z in range(z0, z1 + 1):
        d.set(x, 0, z, "dark_oak_planks")
        if z0 < z < z1:
            d.set(x - 1, 0, z, "dark_oak_planks")
            d.set(x + 1, 0, z, "dark_oak_planks")
            d.set(x - 1, 1, z, "spruce_planks")
            d.set(x + 1, 1, z, "spruce_planks")
    d.set(x, 1, z0, "spruce_planks")
    d.set(x, 2, z0, "spruce_fence")
    d.fill(x - 1, 1, z1, x + 1, 1, z1, "spruce_planks")
    d.set(x, 1, z1 - 1, "guhs:guh_kast", {"facing": "north", "open": False})
    d.bord(x, 1, z1 + 1, "south", ["Vadse Mien", "VV 2"], kleur="white")
    # leeboards
    for z in (12, 13):
        d.set(x - 2, 1, z, "spruce_trapdoor", {"facing": "west", "half": "bottom", "open": True, "powered": False,
                                               "waterlogged": False})
        d.set(x + 2, 1, z, "spruce_trapdoor", {"facing": "east", "half": "bottom", "open": True, "powered": False,
                                               "waterlogged": False})
    # mast, gaff and the sail
    for y in range(1, 11):
        d.set(x, y, 12, "spruce_fence")
    d.banier(x, 11, 12, "south", "oranje", muur=False)
    for z in range(13, 17):
        d.set(x, 2, z, "stripped_spruce_log", {"axis": "z"})
    for y in range(3, 10):
        breed = 4 if y < 8 else (3 if y < 9 else 2)
        for z in range(13, 13 + breed):
            d.set(x, y, z, "brown_wool")
    d.gezicht(x, 6, 14, "west", hout=True, stemming=1)
    d.set(x, 7, 14, "brown_wool")
    d.oor(x, 10, 13, "pink_wool")
    d.set(x, 10, 14, "brown_wool")
    d.oor(x, 11, 14, "pink_wool")


def _kotter(d):
    """Het Knabbeltje: a little fishing boat with a white wheelhouse (guh face) and a lamp mast."""
    x, z0, z1 = 16, 11, 18
    for z in range(z0, z1 + 1):
        d.set(x, 0, z, "blue_terracotta")
        if z0 < z < z1:
            d.set(x - 1, 0, z, "blue_terracotta")
            d.set(x + 1, 0, z, "blue_terracotta")
            d.set(x - 1, 1, z, "red_terracotta")
            d.set(x + 1, 1, z, "red_terracotta")
    d.set(x, 1, z0, "red_terracotta")
    d.set(x, 2, z0, "red_nether_brick_wall")
    d.fill(x - 1, 1, z1, x + 1, 1, z1, "red_terracotta")
    # wheelhouse
    for zz in (15, 16, 17):
        for xx in (x - 1, x, x + 1):
            if zz == 15 or xx != x:
                d.fill(xx, 2, zz, xx, 3, zz, "white_concrete")
        d.plaat(x - 1, 4, zz, "guhs:pluisdak_plaat")
        d.plaat(x, 4, zz, "guhs:pluisdak_plaat")
        d.plaat(x + 1, 4, zz, "guhs:pluisdak_plaat")
    d.set(x, 2, 15, "glass_pane")
    d.gezicht(x, 3, 15, "north", stemming=3)
    d.set(x, 2, 17, "white_concrete")
    d.set(x, 3, 17, "white_concrete")
    d.set(x, 2, 16, "air")
    d.set(x, 3, 16, "air")
    d.set(x, 1, 16, "spruce_planks")
    d.lampion(x, 3, 16, "mint", hangend=True)
    d.set(x - 1, 5, 16, "pink_wool")
    d.set(x + 1, 5, 16, "pink_wool")
    d.oren += 2
    d.sneeuw(x - 1, 6, 16, 1)
    d.sneeuw(x + 1, 6, 16, 1)
    d.bord(x, 1, z1 + 1, "south", ["Knabbeltje", "VV 11"], kleur="white")
    # lamp mast at the bow
    for y in range(1, 6):
        d.set(x, y, 13, "spruce_fence")
    d.lampion(x, 6, 13, "roze")
    d.set(x, 1, 12, "barrel", {"facing": "up", "open": False})


def _ijszeiler(d):
    """An ijszeiler at the harbour mouth: runner plank, hull, mast and a white-orange triangle sail."""
    x = 13
    for xx in range(x - 2, x + 3):
        d.plaat(xx, 0, 20, "spruce_slab")
    for z in range(19, 23):
        d.plaat(x, 0, z, "spruce_slab")
    for y in range(1, 9):
        d.set(x, y, 20, "spruce_fence")
    d.banier(x, 9, 20, "south", "oranje", muur=False)
    for y in range(2, 8):
        n = min(3, max(1, (8 - y) // 2 + 1))
        for z in range(21, 21 + n):
            d.set(x, y, z, "orange_wool" if y in (4, 5) else "white_wool")
    d.knuffel(x, 1, 22, "pinguh", "south")


def _vuurtoren(d):
    """Het Havenlicht: a red-white guh lighthouse on the west pier."""
    x0, z0 = 3, 18           # 3x3 tower x 3..5, z 18..20
    for y in range(0, 10):
        for x in range(x0, x0 + 3):
            for z in range(z0, z0 + 3):
                if x in (x0, x0 + 2) or z in (z0, z0 + 2):
                    d.set(x, y, z, "red_concrete" if (y // 2) % 2 == 0 else "white_concrete")
                else:
                    d.set(x, y, z, "air")
    for x in range(x0, x0 + 3):
        for z in range(z0, z0 + 3):
            d.set(x, -1, z, "guhs:vadshout_planken")
    d.set(x0 + 1, 9, z0 + 1, "guhs:vadshout_planken")
    d.lampion(x0 + 1, 8, z0 + 1, "geel", hangend=True)
    d.deur(x0 + 2, 0, z0 + 1, "west", "guhs:vadshout_deur")
    d.raam(x0 + 1, 4, z0 + 2, "x", hoog=1)
    d.raam(x0 + 1, 7, z0 + 2, "x", hoog=1)
    # gallery + lantern room + dome with guh head
    for x in range(x0 - 1, x0 + 4):
        for z in range(z0 - 1, z0 + 4):
            d.plaat(x, 10, z, "smooth_stone_slab", "double" if (x0 <= x < x0 + 3 and z0 <= z < z0 + 3) else "bottom")
    for x in range(x0 - 1, x0 + 4):
        for z in range(z0 - 1, z0 + 4):
            if x in (x0 - 1, x0 + 3) or z in (z0 - 1, z0 + 3):
                d.set(x, 11, z, "iron_bars")
    for x in range(x0, x0 + 3):
        for z in range(z0, z0 + 3):
            if (x, z) == (x0 + 1, z0 + 1):
                d.set(x, 11, z, "guhs:guh_kristal_lamp")
                d.lichten.append(d.a(x, 11, z))
            elif x in (x0, x0 + 2) and z in (z0, z0 + 2):
                d.set(x, 11, z, "red_concrete")
            else:
                d.set(x, 11, z, "yellow_stained_glass")
            d.set(x, 12, z, "red_concrete")
    d.set(x0 + 1, 13, z0 + 1, "red_concrete")
    d.set(x0, 13, z0 + 1, "red_concrete")
    d.set(x0 + 2, 13, z0 + 1, "red_concrete")
    d.gezicht(x0 + 1, 12, z0 + 2, "south", stemming=0)
    d.gezicht(x0 + 2, 12, z0 + 1, "east", stemming=1)
    d.oor(x0, 14, z0 + 1, "pink_wool")
    d.oor(x0 + 2, 14, z0 + 1, "pink_wool")
    d.bord(x0 + 1, 1, z0 + 3, "south", ["Het", "Havenlicht"], kleur="red")


def _afslag(d):
    """De Knabbelafslag: the knabbel auction hall on the north quay (x 8..19, z 0..5) with a clock gable and cupola."""
    x0, x1, z0, z1, h = 8, 19, 0, 5, 5
    for y in range(h):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                rand = x in (x0, x1) or z in (z0, z1)
                d.set(x, y, z, ("bricks" if y else "smooth_quartz") if rand else "air")
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            d.set(x, -1, z, "guhs:vadshout_planken")
    d.fill(x0, h, z0, x1, h, z1, "guhs:vadshout_planken")
    for x in range(x0, x1 + 1):
        d.set(x, h, z1, "smooth_quartz")
    d.zadeldak(x0, x1, z0, z1, h + 1, "guhs:pluisdak_trap", "guhs:pluisdak", as_="x")
    for x in range(x0, x1 + 1):
        d.plaat(x, h + 4, 2, "guhs:pluisdak_plaat")
        d.plaat(x, h + 4, 3, "guhs:pluisdak_plaat")
    # side gables
    for xg, kijk in ((x0, "west"), (x1, "east")):
        for k in range(3):
            for z in range(z0 + k, z1 - k + 1):
                d.set(xg, h + 1 + k, z, "bricks")
    # the clock gable in the middle of the front (a guh face as the clock) + ears
    d.guhgevel(12, 16, z1, h + 1, "bricks", "south", "hals", "pink_wool", rand="smooth_quartz_slab")
    d.set(14, h + 2, z1, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": 1})
    d.gezichten += 1
    # big arched windows / doors on the front
    for xx in (9, 10, 17, 18):
        d.raam(xx, 1, z1, "x", hoog=3)
    d.deur(13, 0, z1, "north", "spruce_door", "left")
    d.deur(14, 0, z1, "north", "spruce_door", "right")
    d.set(15, 0, z1, "bricks")
    d.raam(11, 1, z1, "x", hoog=2)
    d.raam(16, 1, z1, "x", hoog=2)
    d.bord(14, 3, z1 + 1, "south", ["De", "Knabbelafslag", "vers van de kaas!"], hout="dark_oak", kleur="yellow")
    # the cupola on the ridge with a bell
    cx = 11
    for dx in (0, 1):
        for dz in (0, 1):
            d.fill(cx + dx, h + 4, 2 + dz, cx + dx, h + 4, 2 + dz, "white_concrete")
    d.set(cx, h + 5, 2, "white_concrete")
    d.set(cx + 1, h + 5, 3, "white_concrete")
    d.set(cx + 1, h + 5, 2, "white_concrete")
    d.set(cx, h + 5, 3, "white_concrete")
    d.set(cx, h + 6, 2, "guhs:pluisdak")
    d.set(cx + 1, h + 6, 3, "guhs:pluisdak")
    d.set(cx + 1, h + 6, 2, "guhs:pluisdak")
    d.set(cx, h + 6, 3, "guhs:pluisdak")
    d.oor(cx, h + 7, 2, "pink_wool")
    d.oor(cx + 1, h + 7, 3, "pink_wool")
    # inside: auction counter, crates of kaasknabbels, lampions
    for xx in range(10, 18):
        if xx % 3 == 0:
            d.set(xx, 0, 1, "guhs:block_of_kaasknabbels")
        else:
            d.set(xx, 0, 1, "barrel", {"facing": "up", "open": False})
    for xx in (10, 17):
        d.lampion(xx, h - 1, 3, "geel", hangend=True)
    d.lampion(14, h - 1, 2, "roze", hangend=True)


def _huizen(d):
    # the havenmeester's house (NE) and a wooden fisherman's hut (NW)
    d.pand(21, 0, 5, 6, 9, "bricks", "south", "trap", deurblok="warped_door", trim="smooth_quartz_slab", vlag="nl",
           licht="geel")
    d.pand(1, 0, 5, 5, 5, "spruce_planks", "south", "klok", deurblok="guhs:vadshout_deur", kozijn="green_concrete",
           trim="spruce_slab", oor="pink_wool", hout_gezicht=True, licht="roze", meubels=True)
    d.bord(23, 3, 6, "south", ["Havenmeester", "Guh Vadsvoort"], hout="dark_oak", kleur="white")
    # the boathouse on the west quay (open to the basin) with a little rowing boat
    x0, x1, z0, z1 = 1, 6, 8, 14
    for y in range(0, 4):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                rand = x in (x0, x1) or z in (z0, z1)
                d.set(x, y, z, "spruce_planks" if rand else "air")
    d.lucht(x1, 0, z0 + 2, x1, 2, z1 - 2)
    for x in range(x0 + 1, x1 + 1):
        for z in range(z0 + 1, z1):
            d.set(x, -1, z, "guhs:vadshout_planken")
    d.fill(x0, 4, z0, x1, 4, z1, "spruce_planks")
    d.zadeldak(x0, x1, z0, z1, 5, "guhs:pluisdak_trap", "guhs:pluisdak", as_="x")
    d.guhgevel(z0, z1, x1, 5, "spruce_planks", "east", "trap", "pink_wool", hout=True, rand="spruce_slab", vlak="z")
    d.guhgevel(z0, z1, x0, 5, "spruce_planks", "west", "trap", "pink_wool", hout=True, rand="spruce_slab", vlak="z")
    # rowing boat inside
    for z in range(z0 + 2, z1 - 1):
        d.plaat(3, 0, z, "oak_slab")
        d.set(2, 0, z, "oak_planks")
        d.set(4, 0, z, "oak_planks")
    d.lampion(3, 3, 11, "geel", hangend=True)


def _kade(d):
    # the east quay: kraam, anchor, crates, the Stempelpost on the pier head
    d.kraam(22, 14, 5, "orange", "white", ["Warme Chocovet", "& Gerookte", "Knabbels"],
            waar=[("guhs:theepotje", {"facing": "south"}), ("guhs:block_of_kaasknabbels", None),
                  ("guhs:theepotje", {"facing": "south"})], lampion="geel")
    for x in range(20, 24):
        for z in range(20, PLOT_Z):
            d.set(x, -1, z, "guhs:vadshout_planken")
    d.stempelguh(21, 0, 22)
    d.set(23, 0, 21, "anvil", {"facing": "east"})
    d.set(23, 1, 21, "chain", {"axis": "y", "waterlogged": False})
    d.set(22, 0, 20, "spruce_fence")
    d.bord(22, 1, 20, "south", ["Stempelpost 5", "VADSVOREN", "plof!"], muur=False, kleur="blue")
    d.vlaggenmast(20, 20, 4, "nl")
    d.vlaggenmast(24, 23, 4, "oranje")
    for x, z in ((26, 12), (25, 10), (26, 10)):
        d.set(x, 0, z, "barrel", {"facing": "up", "open": False})
    d.set(26, 1, 10, "barrel", {"facing": "up", "open": False})
    d.set(25, 0, 12, "guhs:block_of_kaasknabbels")
    # net drying rack
    for z in (7, 10):
        d.fill(23, 0, z, 23, 2, z, "spruce_fence")
    for z in (8, 9):
        d.set(23, 2, z, "spruce_fence")
    # lamp posts, bunting over the harbour front, snowmen, welcome sign
    for x, z in ((7, 12), (20, 12), (7, 22), (0, 16), (27, 18), (20, 7), (7, 7)):
        d.lantaarnpaal(x, z, "geel" if (x + z) % 2 else "roze")
    d.sneeuwpop(26, 21, "south")
    d.set(0, 0, 23, "spruce_fence")
    d.bord(0, 1, 23, "south", ["Welkom in", "VADSVOREN", "haventje!"], muur=False, kleur="blue")
    d.sneeuwdek(0.35, 5)


def bouw(s, ox, oy, oz):
    d = Dorp(s, ox, oy, oz, NAAM)
    _haven(d)
    _botter(d)
    _kotter(d)
    _ijszeiler(d)
    _vuurtoren(d)
    _afslag(d)
    _huizen(d)
    _kade(d)
    for x, z in ((5, 22), (1, 22), (25, 21), (26, 23), (7, 19), (21, 18)):
        d.toeschouwer(x, 0, z, 0)
    return d.resultaat()


def check(s, ox, oy, oz):
    return controleer(s, ox, oy, oz, NAAM, INDEX, min_gezichten=10)
