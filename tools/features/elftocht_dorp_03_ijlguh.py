"""Elf-Guhjestocht village 3: IJLGUH - the warme-chocovet village.

The heart is 'De Dampende Beker': a giant guh mug (white with a pink band, a guh face, guh ears on the rim, a handle,
a mound of whipped cream with marshmallow knabbels on top) that is a real kiosk: the chocovet guh serves through the
counter window facing the ice. Around it two warmtekringen (vuurkorven with benches) where guhs roast marshmallow
knabbels, a pink-white marshmallow kraam, an arrenslee with a Pinguh knuffel, and at the back the Frisian farm
'De Warme Poot' (kop-hals-romp) with a guh 'uilebord' on the barn gable. Stempelpost on a steiger between two vuurkorven.
"""
INDEX = 3
NAAM = "IJlguh"

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
        """1.2.0: every line is a translate key sign.guhs.elftocht.dorp<INDEX>.<slug> with the Dutch as fallback."""
        from make_structures import NbtList, Byte
        import sign_text
        msgs = sign_text.messages(f"sign.guhs.elftocht.dorp{INDEX}", regels)
        nbt = {"id": "minecraft:sign", "is_waxed": Byte(1),
               "front_text": {"messages": msgs, "color": kleur, "has_glowing_text": Byte(1 if glim else 0)},
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
# IJlguh
# ---------------------------------------------------------------------------------------------------------------------
KLINK = "guhs:knuffelklinkers"
BEKER = (13, 15)       # the mug's centre (x, z)
BEKER_R = 4.0


def _paden(d):
    # a klinker path in front of the farm, one down to the steiger and a square around the mug
    for x in range(PLOT_X):
        for z in (9, 10):
            d.set(x, -1, z, KLINK)
    for x in range(8, 19):
        for z in range(11, 21):
            if (x - BEKER[0]) ** 2 + (z - BEKER[1]) ** 2 <= 5.6 ** 2:
                d.set(x, -1, z, KLINK if (x * 3 + z) % 7 else "guhs:knuffelsteen")
    for x in range(10, 17):
        for z in range(19, 21):
            d.set(x, -1, z, KLINK)


def _boerderij(d):
    """De Warme Poot: kop (a house with a guh gable), hals and a long romp (barn) with a huge roof and an uilebord."""
    d.pand(2, 1, 7, 7, 7, "bricks", "south", "klok", deurblok="guhs:vadshout_deur", dak="guhs:vadshout_trap",
           nok="guhs:vadshout_planken", oor="pink_wool", trim="smooth_quartz_slab", licht="geel", vlag="fries")
    # chimney with a cosy fire on top
    for y in range(7, 12):
        d.set(4, y, 3, "bricks")
    d.kampvuur(4, 12, 3)
    # hals: x 9..11, z 2..6
    for y in range(0, 4):
        for x in range(9, 12):
            for z in range(2, 7):
                d.set(x, y, z, "bricks" if z in (2, 6) else "air")
    for x in range(9, 12):
        for z in range(3, 6):
            d.set(x, -1, z, "guhs:vadshout_planken")
    d.fill(9, 4, 2, 11, 4, 6, "guhs:vadshout_planken")
    d.zadeldak(9, 11, 2, 6, 5, "guhs:vadshout_trap", "guhs:vadshout_planken", as_="x")
    d.raam(10, 1, 6, "x")
    # romp (barn): x 12..24, z 0..8, walls 4 high (bricks with a white plinth), big roof along x
    x0, x1, z0, z1, h = 12, 24, 0, 8, 4
    for y in range(h):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                rand = x in (x0, x1) or z in (z0, z1)
                d.set(x, y, z, ("smooth_quartz" if y == 0 else "bricks") if rand else "air")
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            d.set(x, -1, z, "guhs:vadshout_planken")
    # the opening to the hals
    d.lucht(12, 0, 3, 12, 2, 5)
    d.fill(x0, h, z0, x1, h, z1, "guhs:vadshout_planken")
    d.zadeldak(x0, x1, z0, z1, h + 1, "guhs:vadshout_trap", "guhs:vadshout_planken", as_="x")
    d.guhgevel(z0, z1, x1, h + 1, "bricks", "east", "trap", "pink_wool", hout=True, rand="smooth_quartz_slab", vlak="z")
    d.guhgevel(z0, z1, x0, h + 1, "bricks", "west", "trap", "pink_wool", hout=True, rand="smooth_quartz_slab", vlak="z")
    # the big barn doors (a double door) with a lampion above, small windows
    d.deur(18, 0, z1, "north", "guhs:vadshout_deur", "left")
    d.deur(19, 0, z1, "north", "guhs:vadshout_deur", "right")
    d.fill(17, 0, z1, 17, 2, z1, "guhs:vadshout_stam")
    d.fill(20, 0, z1, 20, 2, z1, "guhs:vadshout_stam")
    d.set(18, 2, z1, "guhs:vadshout_planken")
    d.set(19, 2, z1, "guhs:vadshout_planken")
    d.gezicht(18, 3, z1, "south", hout=True, stemming=2)
    d.gezicht(19, 3, z1, "south", hout=True, stemming=3)
    for x in (14, 22):
        d.raam(x, 1, z1, "x")
        d.raam(x, 1, z0, "x")
    for z in (3, 5):
        d.raam(x1, 1, z, "z")
    d.set(21, 3, z1 + 1, "spruce_fence")
    d.lampion(21, 2, z1 + 1, "geel", hangend=True)
    d.set(16, 3, z1 + 1, "spruce_fence")
    d.lampion(16, 2, z1 + 1, "geel", hangend=True)
    # inside: hay, a cosy guh nest, lampions
    for x, z in ((13, 1), (13, 2), (14, 1), (23, 1), (23, 2), (22, 1)):
        d.set(x, 0, z, "hay_block", {"axis": "y"})
    d.set(13, 1, 1, "hay_block", {"axis": "y"})
    d.set(23, 1, 1, "hay_block", {"axis": "y"})
    for x in (15, 21):
        d.lampion(x, h - 1, 4, "geel", hangend=True)


def _beker(d):
    """De Dampende Beker: a mug kiosk with guh ears, face, handle and whipped cream + marshmallow knabbels."""
    cx, cz = BEKER
    r = BEKER_R
    H = 7                                            # the mug wall is y 0..6, the chocovet surface at y 6
    cel = {(x, z) for x in range(cx - 5, cx + 6) for z in range(cz - 5, cz + 6) if (x - cx) ** 2 + (z - cz) ** 2 <= r * r}
    rand = {c for c in cel if any((c[0] + dx, c[1] + dz) not in cel for dx, dz in FACING.values())}
    front = max(z for (x, z) in cel)                 # the counter row
    for (x, z) in cel:
        d.set(x, -1, z, "guhs:vadshout_planken")
        for y in range(0, H):
            if (x, z) in rand:
                blok = "white_concrete" if y in (0, 3, H - 1) else "pink_concrete"
                if y in (1, 4, 5) and (x * 2 + z + y) % 4 == 0:
                    blok = "white_concrete"         # white polka dots
                d.set(x, y, z, blok)
            elif y < H - 1:
                d.set(x, y, z, "air")
        if (x, z) not in rand:
            d.set(x, H - 1, z, "brown_concrete")      # the chocovet surface (the kiosk's ceiling)
    # counter window facing the ice, a door at the back
    for x in (cx - 1, cx, cx + 1):
        d.lucht(x, 1, front, x, 2, front)
    d.set(cx - 1, 1, front, "white_candle", {"candles": 3, "lit": True, "waterlogged": False})
    d.set(cx + 1, 1, front, "pink_candle", {"candles": 2, "lit": True, "waterlogged": False})
    d.lichten.append(d.a(cx - 1, 1, front))
    d.deur(cx, 0, cz - int(r), "south")
    d.gezicht(cx, 4, front, "south", stemming=1)
    d.gezicht(cx, 4, cz - int(r), "north", stemming=3)
    d.bord(cx, 3, front + 1, "south", ["De Dampende", "Beker", "warme chocovet!"], kleur="brown")
    # handle on the east side
    for x, y in ((cx + 5, 1), (cx + 6, 1), (cx + 6, 2), (cx + 6, 3), (cx + 6, 4), (cx + 6, 5), (cx + 5, 5)):
        d.set(x, y, cz, "white_concrete")
    # big guh ears on the rim (west and east)
    for x in (cx - int(r), cx + int(r)):
        for y, zs in ((H, (cz - 1, cz, cz + 1)), (H + 1, (cz - 1, cz)), (H + 2, (cz - 1,))):
            for z in zs:
                d.set(x, y, z, "pink_wool" if (y, z) != (H, cz) else "magenta_wool")
        d.sneeuw(x, H + 3, cz - 1, 1)
        d.oren += 1
    # whipped cream mound + marshmallow knabbels + one golden kaasknabbel on top
    for (x, z) in cel:
        dist2 = (x - cx) ** 2 + (z - cz) ** 2
        if (x, z) in rand:
            continue
        if dist2 <= 1.9 ** 2:
            d.set(x, H, z, "white_wool")
        if dist2 <= 1.0:
            d.set(x, H + 1, z, "white_wool")
    d.set(cx, H + 2, cz, "white_wool")
    for x, y, z, blok in ((cx - 1, H + 1, cz - 1, "pink_wool"), (cx + 1, H + 1, cz + 1, "pink_wool"), (cx + 2, H, cz, "white_concrete"),
                          (cx - 2, H, cz + 1, "pink_wool"), (cx, H, cz - 2, "white_concrete"), (cx + 1, H, cz - 2, "pink_wool"),
                          (cx, H + 3, cz, "guhs:block_of_kaasknabbels")):
        d.set(x, y, z, blok)
    # inside: the chocovet ketel, a vendor, lampions
    d.set(cx - 2, 0, cz, "cauldron")
    d.set(cx - 2, 0, cz - 1, "campfire", {"facing": "east", "lit": False, "signal_fire": False, "waterlogged": False})
    d.set(cx + 2, 0, cz, "brewing_stand", {"has_bottle_0": True, "has_bottle_1": True, "has_bottle_2": False})
    d.set(cx + 2, 0, cz - 1, "guhs:guh_kast", {"facing": "west", "open": False})
    d.set(cx - 2, 0, cz + 2, "guhs:guh_kast", {"facing": "east", "open": False})
    d.lampion(cx, H - 2, cz, "geel", hangend=True)
    d.toeschouwer(cx, 0, cz + 2, 0)
    d.boost.append(d.a(cx, 0, front))


def _warmte(d):
    # two warmtekringen: a vuurkorf with benches and guhs roasting marshmallow knabbels
    for fx, fz in ((5, 13), (22, 17)):
        d.vuurkorf(fx, fz)
        d.bank(fx - 1, fz, "east")
        d.bank(fx + 1, fz, "west")
        d.bank(fx, fz - 1, "south")
    # the marshmallow kraam
    d.kraam(1, 17, 5, "pink", "white", ["Marshmallow-", "knabbels", "roosteren!"],
            waar=[("pink_candle", {"candles": 4, "lit": True, "waterlogged": False}),
                  ("white_candle", {"candles": 3, "lit": True, "waterlogged": False}),
                  ("pink_candle", {"candles": 2, "lit": True, "waterlogged": False})], lampion="roze", boost=False)
    # the arrenslee with a Pinguh knuffel on the seat
    for x in range(22, 26):
        d.plaat(x, 0, 12, "spruce_slab")
        d.plaat(x, 0, 14, "spruce_slab")
    d.fill(22, 0, 13, 25, 0, 13, "spruce_planks")
    d.trap(26, 0, 12, "spruce_stairs", "west")
    d.trap(26, 0, 14, "spruce_stairs", "west")
    d.fill(22, 1, 12, 22, 1, 14, "red_wool")
    d.set(22, 2, 13, "red_wool")
    d.set(23, 1, 13, "red_carpet")
    d.knuffel(24, 1, 13, "pinguh", "east")
    d.set(25, 1, 13, "spruce_fence")
    d.lampion(25, 2, 13, "roze")
    # a little terrace for chocovet drinkers
    for tx in (18, 21):
        d.set(tx, 0, 20, "guhs:guh_tafel", {"facing": "south"})
        d.set(tx - 1, 0, 20, "guhs:guh_stoel", {"facing": "east"})
        d.set(tx + 1, 0, 20, "guhs:guh_stoel", {"facing": "west"})
        d.set(tx, 1, 20, "guhs:theepotje", {"facing": "south"})
    # snowmen, knotwilgen, lamp posts
    d.sneeuwpop(27, 19, "south")
    d.sneeuwpop(0, 11, "south")
    d.knotwilg(26, 10)
    d.knotwilg(1, 21)
    for x, z in ((8, 11), (18, 11), (0, 9), (9, 21), (17, 21), (25, 22)):
        d.lantaarnpaal(x, z, "geel" if x % 2 else "roze")
    # welcome sign
    d.set(0, 0, 23, "spruce_fence")
    d.bord(0, 1, 23, "south", ["Welkom in", "IJLGUH", "warm je poot!"], muur=False, kleur="brown")


def _stempel(d):
    for x in range(11, 16):
        for z in range(21, 24):
            d.set(x, -1, z, "guhs:vadshout_planken")
    d.vuurkorf(11, 22)
    d.vuurkorf(15, 22)
    d.stempelguh(13, 0, 22)
    d.set(13, 0, 21, "guhs:vadshout_stam")
    d.set(13, 1, 21, "guhs:vadshout_stam")
    d.gezicht(13, 2, 21, "south", hout=True, stemming=0)
    d.set(12, 2, 21, "guhs:vadshout_planken")
    d.set(14, 2, 21, "guhs:vadshout_planken")
    d.oor(12, 3, 21, "pink_wool")
    d.oor(14, 3, 21, "pink_wool")
    d.set(12, 1, 21, "guhs:vadshout_planken")
    d.set(12, 0, 21, "guhs:vadshout_stam")
    d.set(14, 1, 21, "guhs:vadshout_planken")
    d.set(14, 0, 21, "guhs:vadshout_stam")
    d.bord(12, 1, 22, "south", ["Stempelpost 3", "IJLGUH"], kleur="brown")
    d.bord(14, 1, 22, "south", ["plof!", "vahoeg!"], kleur="brown")
    d.vlaggenmast(10, 21, 4, "oranje")
    d.vlaggenmast(16, 21, 4, "oranje")


def bouw(s, ox, oy, oz):
    d = Dorp(s, ox, oy, oz, NAAM)
    _paden(d)
    _boerderij(d)
    _beker(d)
    _warmte(d)
    _stempel(d)
    for x, z in ((6, 22), (8, 23), (19, 22), (21, 23), (4, 14), (23, 16)):
        d.toeschouwer(x, 0, z, 0)
    d.sneeuwdek(0.35, 3)
    return d.resultaat()


def check(s, ox, oy, oz):
    return controleer(s, ox, oy, oz, NAAM, INDEX, min_gezichten=8)
