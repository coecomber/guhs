"""
Super Guhrio (bbq2) - the lane builder: what the level slices (guhrio_w1 .. guhrio_w3) and the reward slice
(guhrio_beloning) build with. The manual with every method: guhs_work130/reports/slice_guhrio-engine.md, section 1.

  Baanbouwer   the slot of one level in the castle: a box of L x H cells in the plane of its lane (s along the lane, y up),
               one block deep (d = 0), with the painted wall behind it (d = -1) and the camera's room in front (d > 0, air).
               The slice's bouw(baan) places pieces with its methods; the castle (guhrio_kasteel.py) then checks the level,
               writes the blocks into the castle and the lanes into data/guhs/guhrio_level/<id>.json.
  Plekbouwer   a free room of the castle (the forecourt, the tower room, the level hall) with its own little frame.
  level_json   writes a level's lane file (also used by the test levels of guhrio.py).

Nothing here knows where the castle puts a slot: a builder only gets the mapping (origin, direction) from guhrio_kasteel.
"""
import json
import math
import os
import random

GROND, BLOK, START, VRAAG, STEEN, MUNT = ("guhs:guhrio_grond", "guhs:guhrio_blok", "guhs:guhrio_startblok", "guhs:guhrio_vraagblok",
                                          "guhs:guhrio_steen", "guhs:guhrio_munt")
VLAG, MAST, PIJP, PIJP_LIJF, DEUR = "guhs:guhrio_vlag", "guhs:guhrio_mast", "guhs:guhrio_pijp", "guhs:guhrio_pijp_lijf", "guhs:guhrio_deur"
ONZICHTBAAR, VADSMUNT, SCHAKELAAR, SCHAKELBLOK = ("guhs:guhrio_onzichtbaar", "guhs:guhrio_vadsmunt", "guhs:guhrio_schakelaar",
                                                  "guhs:guhrio_schakelblok")
GUHMBA, SCHILD_MIKA, PLOF_MIKA, HAPBLOEM = ("guhs:guhrio_guhmba_plek", "guhs:guhrio_schild_mika_plek", "guhs:guhrio_plof_mika_plek",
                                            "guhs:guhrio_hapbloem_plek")
GRILLSPIES, PLATFORM, VALBLOK = "guhs:guhrio_grillspies_plek", "guhs:guhrio_platform_plek", "guhs:guhrio_valblok_plek"
GUHSHI_EI, GUHSHI, POORT = "guhs:guhrio_guhshi_ei", "guhs:guhrio_guhshi_plek", "guhs:guhrio_poort"
AIR = "minecraft:air"
SAUS = {1: "guhs:kaas_saus", 2: "guhs:kaas_saus", 3: "guhs:kaasfrituursaus"}

# blocks a player does not stand on (for the check under the start)
LOS = {AIR, MUNT, VADSMUNT, VLAG, MAST, DEUR, GUHMBA, SCHILD_MIKA, PLOF_MIKA, HAPBLOEM, PLATFORM, VALBLOK, GUHSHI_EI, GUHSHI, ONZICHTBAAR, START}
RICHTINGEN = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}


def level_json(h, level_id, banen, wereld=None, uitgang=None, ingang=None, na=None):
    """
    Writes data/guhs/guhrio_level/<level_id>.json. banen: a list of dicts {id, punten [[x,y,z], ...], camera "rechts"/"links",
    afstand, hoogte, onder, boven}, all in the level's own frame (the start block is 0,0,0; +x is the way it faces, +z its
    right hand); the first lane is the start block's. uitgang / ingang: where you are put after the flagpole / when you
    leave any other way (the level's frame); na: the id of the level that must be finished first.
    """
    for baan in banen:
        punten = baan["punten"]
        for a, c in zip(punten, punten[1:]):
            if (a[0] == c[0]) == (a[2] == c[2]):
                raise SystemExit(f"guhrio level {level_id}: lane {baan.get('id')} piece {a} -> {c} must run along x or along z")
    data = {"wereld": wereld or level_id, "banen": banen}
    if uitgang is not None:
        data["uitgang"] = list(uitgang)
    if ingang is not None:
        data["ingang"] = list(ingang)
    if na:
        data["na"] = na
    h.w(os.path.join(h.D, "guhrio_level", f"{level_id}.json"), data)


class Baanbouwer:
    """The slot of one level. See the module text and the manual; every method raises SystemExit with the level's name."""
    AFSTAND = 11

    def __init__(self, h, level_id, wereld, nr, thema, lengte, hoogte, oorsprong, richting):
        self.h, self.id, self.wereld, self.nr, self.thema = h, level_id, wereld, nr, thema
        self.L, self.H = lengte, hoogte
        self.oorsprong, self.richting = tuple(oorsprong), tuple(richting)
        self.camera = (-richting[1], richting[0])               # +d: the right hand of the lane's direction
        self.rng = random.Random(21302350 + nr * 7919)
        self.cellen = {}                                         # (s, y, d) -> (name, props, nbt)
        self.wezens = []                                         # (s, y, d, nbt)
        self.trog = {}                                           # s -> fluid id (the trench under the lane)
        self._start = None
        self._hoofd = dict(onder=0, boven=hoogte - 1, hoogte=6)
        self._bij = []
        self._achtergrond = True

    # --- where things are ---------------------------------------------------------------------------------------------
    def fout(self, tekst):
        raise SystemExit(f"guhrio level {self.wereld} ({self.id}): {tekst}")

    def kant(self, naam):
        """A facing for block states: "verder" (towards s = L), "terug", "camera" (towards the camera), "muur" (the painted wall)."""
        dx, dz = self.richting
        ex, ez = self.camera
        return {"verder": RICHTINGEN[(dx, dz)], "terug": RICHTINGEN[(-dx, -dz)], "camera": RICHTINGEN[(ex, ez)],
                "muur": RICHTINGEN[(-ex, -ez)]}[naam]

    def as_(self):
        """The axis the lane runs along ("x" or "z"), for pillar-like blocks."""
        return "x" if self.richting[0] else "z"

    def bouw(self, s, y, d=0):
        """(s, y, d) -> the coordinates in the whole castle."""
        return (self.oorsprong[0] + s * self.richting[0] + d * self.camera[0], self.oorsprong[1] + y,
                self.oorsprong[2] + s * self.richting[1] + d * self.camera[1])

    def eigen(self, s, y, d=0):
        """(s, y, d) -> the level's own frame (what guhrio_level/<id>.json and the Java side count in)."""
        if self._start is None:
            self.fout("baan.start(s, y) first: the level's frame hangs on the start block")
        return [s - self._start[0], y - self._start[1], d]

    def naar_eigen(self, bouw):
        """A spot of the whole castle -> the level's own frame."""
        x, y, z = (bouw[i] - self.bouw(self._start[0], self._start[1])[i] for i in range(3))
        return [x * self.richting[0] + z * self.richting[1], y, x * self.camera[0] + z * self.camera[1]]

    def _mag(self, s, y, d):
        if not (0 <= s < self.L and 0 <= y < self.H):
            self.fout(f"({s}, {y}) is outside the slot (s 0..{self.L - 1}, y 0..{self.H - 1})")
        if d > 0:
            self.fout(f"({s}, {y}, d={d}): the camera's room (d > 0) must stay air")
        if d < -1:
            self.fout(f"({s}, {y}, d={d}): only the lane (d = 0) and the painted wall (d = -1) are yours")

    # --- low level ----------------------------------------------------------------------------------------------------
    def zet(self, s, y, blok, props=None, nbt=None, d=0):
        self._mag(s, y, d)
        if ":" not in blok:
            self.fout(f"block id {blok!r} needs a namespace")
        self.cellen[(s, y, d)] = (blok, props, nbt)

    def vul(self, s0, y0, s1, y1, blok, props=None, d=0):
        for s in range(min(s0, s1), max(s0, s1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                self.zet(s, y, blok, props, None, d)

    def lucht(self, s0, y0, s1, y1, d=0):
        for s in range(min(s0, s1), max(s0, s1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                self._mag(s, y, d)
                self.cellen.pop((s, y, d), None)

    def haal(self, s, y, d=0):
        cel = self.cellen.get((s, y, d))
        return None if cel is None or cel[0] == AIR else cel[0]

    def decor(self, s, y, blok, props=None):
        self.zet(s, y, blok, props, None, -1)

    def decor_vul(self, s0, y0, s1, y1, blok, props=None):
        self.vul(s0, y0, s1, y1, blok, props, -1)

    def achtergrond(self, aan):
        """False: the engine does not paint the theme's backdrop on the cells of d = -1 you left alone (they stay plain wall)."""
        self._achtergrond = bool(aan)

    def entity(self, s, y, nbt, d=0):
        self._mag(s, y, d)
        self.wezens.append((s, y, d, nbt))

    def saus(self, s0, s1, soort=None):
        """Sauce in the trench under the lane (below row 0) for the cells s0..s1: what a pit ends in (the theme's own sauce)."""
        for s in range(min(s0, s1), max(s0, s1) + 1):
            self._mag(s, 0, 0)
            self.trog[s] = soort or SAUS[self.thema]

    # --- the engine's pieces --------------------------------------------------------------------------------------------
    def grond(self, s0, s1, y, dik=2, blok=GROND):
        for s in range(min(s0, s1), max(s0, s1) + 1):
            for yy in range(max(0, y - dik + 1), y + 1):
                self.zet(s, yy, blok)

    def blok(self, s, y, hoog=1):
        for yy in range(y, y + hoog):
            self.zet(s, yy, BLOK)

    def steen(self, s, y):
        self.zet(s, y, STEEN)

    def vraag(self, s, y, inhoud="munt"):
        if inhoud not in ("munt", "superknabbel", "vuurpeper"):
            self.fout(f"?-block inhoud {inhoud!r}: munt, superknabbel or vuurpeper")
        self.zet(s, y, VRAAG, {"inhoud": inhoud, "leeg": "false"})

    def onzichtbaar(self, s, y, inhoud="munt"):
        self.zet(s, y, ONZICHTBAAR, {"inhoud": inhoud})

    def munt(self, s, y):
        self.zet(s, y, MUNT)

    def munten(self, s0, s1, y):
        for s in range(min(s0, s1), max(s0, s1) + 1):
            self.zet(s, y, MUNT)

    def vadsmunt(self, s, y, nummer):
        if nummer not in (0, 1, 2):
            self.fout("vadsmunt nummer 0, 1 or 2")
        self.zet(s, y, VADSMUNT, {"nummer": str(nummer)})

    def vlag(self, s, y):
        self.zet(s, y, VLAG)

    def mast(self, s, y, hoog=6):
        for k in range(hoog):
            self.zet(s, y + k, MAST, {"top": "true" if k == hoog - 1 else "false"})

    def start(self, s, y):
        if self._start is not None:
            self.fout("baan.start is called twice")
        self._mag(s, y, 0)
        if y < 1:
            self.fout("the start needs a block under it (y >= 1)")
        self._start = (s, y)

    def pijp(self, s, y, kanaal, hoog=2, richting="omhoog", ingang=True, bloem=False):
        if not 0 <= kanaal <= 15:
            self.fout("pipe kanaal 0..15")
        if richting not in ("omhoog", "omlaag", "terug", "verder"):
            self.fout(f"pipe richting {richting!r}: omhoog, omlaag, terug or verder")
        if bloem and richting != "omhoog":
            self.fout("a Hapbloem only lives in a pipe that opens upwards")
        base = {"kanaal": str(kanaal), "ingang": "true" if ingang and richting != "omlaag" else "false", "boven": "false"}
        if richting == "omhoog":
            self.zet(s, y, PIJP, {**base, "facing": "up"})
            for k in range(1, hoog):
                self.zet(s, y - k, PIJP_LIJF, {"axis": "y"})
            if bloem:
                self.zet(s, y + 1, HAPBLOEM)
        elif richting == "omlaag":
            self.zet(s, y, PIJP, {**base, "facing": "down"})
            for k in range(1, hoog):
                self.zet(s, y + k, PIJP_LIJF, {"axis": "y"})
        else:
            # a sideways pipe is two blocks high; its body runs away from the mouth
            stap = 1 if richting == "terug" else -1
            for dy in (0, 1):
                self.zet(s, y + dy, PIJP, {**base, "facing": self.kant(richting), "boven": "true" if dy else "false"})
                for k in range(1, hoog):
                    self.zet(s + stap * k, y + dy, PIJP_LIJF, {"axis": self.as_()})

    def deur(self, s, y, kanaal):
        if not 0 <= kanaal <= 15:
            self.fout("door kanaal 0..15")
        self.zet(s, y, DEUR, {"kanaal": str(kanaal), "half": "lower"})
        self.zet(s, y + 1, DEUR, {"kanaal": str(kanaal), "half": "upper"})

    def guhmba(self, s, y):
        self.zet(s, y, GUHMBA)

    def schild_mika(self, s, y):
        self.zet(s, y, SCHILD_MIKA)

    def plof_mika(self, s, y):
        self.zet(s, y, PLOF_MIKA)

    def grillspies(self, s, y, lengte=3, tegen_klok=False, snel=False, fase=0):
        if not 1 <= lengte <= 6:
            self.fout("grillspies lengte 1..6")
        self.zet(s, y, GRILLSPIES, {"lengte": str(lengte), "tegen": "true" if tegen_klok else "false", "snel": "true" if snel else "false",
                                    "fase": str(fase % 4)})

    def platform(self, s, y, as_="langs", afstand=4, breed=3, snel=False):
        if as_ not in ("langs", "omhoog") or not 1 <= afstand <= 12 or not 1 <= breed <= 4:
            self.fout("platform: as_ langs/omhoog, afstand 1..12, breed 1..4")
        self.zet(s, y, PLATFORM, {"as": as_, "afstand": str(afstand), "breed": str(breed), "snel": "true" if snel else "false"})

    def valblok(self, s, y, breed=2):
        if not 1 <= breed <= 4:
            self.fout("valblok breed 1..4")
        self.zet(s, y, VALBLOK, {"breed": str(breed)})

    def schakelaar(self, s, y, kanaal, soort="wissel"):
        if soort not in ("wissel", "aan", "uit", "tijd") or not 0 <= kanaal <= 7:
            self.fout("schakelaar: kanaal 0..7, soort wissel/aan/uit/tijd")
        self.zet(s, y, SCHAKELAAR, {"kanaal": str(kanaal), "soort": soort, "ingedrukt": "false"})

    def schakelblok(self, s, y, kanaal, aan=True):
        if not 0 <= kanaal <= 7:
            self.fout("schakelblok kanaal 0..7")
        self.zet(s, y, SCHAKELBLOK, {"kanaal": str(kanaal), "aan": "true" if aan else "false", "open": "false"})

    def guhshi_ei(self, s, y):
        self.zet(s, y, GUHSHI_EI)

    def guhshi(self, s, y):
        self.zet(s, y, GUHSHI)

    # --- lanes ----------------------------------------------------------------------------------------------------------
    def hoofdbaan(self, onder=0, boven=None, hoogte=6):
        self._hoofd = dict(onder=onder, boven=self.H - 1 if boven is None else boven, hoogte=hoogte)

    def bijbaan(self, naam, s0, s1, onder, boven, hoogte=6):
        if not (0 <= s0 < s1 < self.L and 0 <= onder <= boven < self.H):
            self.fout(f"bijbaan {naam}: s0 < s1 inside 0..{self.L - 1}, onder <= boven inside 0..{self.H - 1}")
        self._bij.append(dict(id=naam, s0=s0, s1=s1, onder=onder, boven=boven, hoogte=hoogte))

    # --- for the castle ---------------------------------------------------------------------------------------------------
    def controleer(self, duel=False):
        """Everything section 1.3 of the manual promises to check."""
        if self._start is None:
            self.fout("no baan.start(s, y)")
        s, y = self._start
        onder = self.haal(s, y - 1)
        if onder is None or onder in LOS:
            self.fout(f"the start at ({s}, {y}) has nothing to stand on")
        hoofd = self._hoofd
        if not hoofd["onder"] <= y <= hoofd["boven"]:
            self.fout("the start is not inside the rows of the main lane")
        for bij in self._bij:
            if bij["onder"] <= hoofd["boven"] and bij["boven"] >= hoofd["onder"]:
                self.fout(f"bijbaan {bij['id']} overlaps the rows of the main lane ({hoofd['onder']}..{hoofd['boven']}): give the main "
                          f"lane its own rows with baan.hoofdbaan(onder, boven)")
            for ander in self._bij:
                if ander is not bij and bij["s0"] <= ander["s1"] and bij["s1"] >= ander["s0"] and bij["onder"] <= ander["boven"] \
                        and bij["boven"] >= ander["onder"]:
                    self.fout(f"bijbanen {bij['id']} and {ander['id']} overlap")
        masten = [k for k, v in self.cellen.items() if v[0] == MAST and (v[1] or {}).get("top") == "true"]
        vads = sorted(int((v[1] or {}).get("nummer", 0)) for v in self.cellen.values() if v[0] == VADSMUNT)
        if not duel:
            if len(masten) != 1:
                self.fout(f"a level has exactly one flagpole (baan.mast): found {len(masten)}")
            if vads != [0, 1, 2]:
                self.fout(f"a level has exactly one big vadsmunt of each number 0, 1, 2: found {vads}")
        monden, deuren = {}, {}
        for (cs, cy, cd), (naam, props, _n) in self.cellen.items():
            if naam == PIJP and props.get("boven") != "true":
                monden.setdefault(props["kanaal"], []).append(props.get("ingang") == "true")
            elif naam == DEUR and props.get("half") == "lower":
                deuren[props["kanaal"]] = deuren.get(props["kanaal"], 0) + 1
        for kanaal, lijst in monden.items():
            if len(lijst) < 2:
                self.fout(f"pipe kanaal {kanaal} has only one mouth")
            if not any(lijst):
                self.fout(f"pipe kanaal {kanaal} has no mouth you can enter")
        for kanaal, n in deuren.items():
            if n < 2:
                self.fout(f"door kanaal {kanaal} has only one door")

    def banen(self):
        """The lanes for level_json, in the level's own frame."""
        ys = self._start[1]
        hoofd = self._hoofd
        uit = [{"id": "hoofd", "punten": [self.eigen(0, ys), self.eigen(self.L - 1, ys)], "camera": "rechts", "afstand": self.AFSTAND,
                "hoogte": hoofd["hoogte"], "onder": hoofd["onder"] - ys, "boven": hoofd["boven"] - ys}]
        for bij in self._bij:
            uit.append({"id": bij["id"], "punten": [self.eigen(bij["s0"], ys), self.eigen(bij["s1"], ys)], "camera": "rechts",
                        "afstand": self.AFSTAND, "hoogte": bij["hoogte"], "onder": bij["onder"] - ys, "boven": bij["boven"] - ys})
        return uit

    def stempel(self, zet, entity, achtergrond):
        """Writes the level into the castle: zet(x, y, z, name, props, nbt), entity(x, y, z, nbt); achtergrond(s, y) -> the
        theme's block for a cell of the painted wall the slice left alone."""
        for s in range(self.L):
            for y in range(self.H):
                for d in (0, -1):
                    cel = self.cellen.get((s, y, d))
                    x, yy, z = self.bouw(s, y, d)
                    if cel is not None:
                        zet(x, yy, z, cel[0], cel[1], cel[2])
                    elif d == -1 and self._achtergrond:
                        blok = achtergrond(s, y)
                        zet(x, yy, z, blok[0], blok[1], None)
        for s, fluid in self.trog.items():
            x, yy, z = self.bouw(s, -1, 0)
            zet(x, yy, z, fluid, {"level": "0"}, None)
        sx, sy, sz = self.bouw(self._start[0], self._start[1])
        zet(sx, sy, sz, START, {"facing": self.kant("verder")}, {"id": "guhs:guhrio_start", "Level": self.id})
        for s, y, d, nbt in self.wezens:
            x, yy, z = self.bouw(s, y, d)
            entity(x + 0.5, yy, z + 0.5, nbt)


class Plekbouwer:
    """
    A free room of the castle with a frame of its own: x from left to right when you face INTO the room from its entrance,
    z from the entrance inwards, y = 0 is the floor's top block (things stand at y = 1). See the manual, section 1.4.
    """

    def __init__(self, h, naam, maat, oorsprong, rechts, zet, entity, haal, vrij=()):
        self.h, self.naam = h, naam
        self.B, self.H, self.D = maat
        self.oorsprong, self.rechts = tuple(oorsprong), tuple(rechts)
        self.diep = (rechts[1], -rechts[0])                       # +z: into the room, so that +x is your right hand
        self._zet, self._entity, self._haal = zet, entity, haal
        self.vrij = list(vrij)

    def fout(self, tekst):
        raise SystemExit(f"guhrio {self.naam}: {tekst}")

    def bouw(self, x, y, z):
        return (self.oorsprong[0] + x * self.rechts[0] + z * self.diep[0], self.oorsprong[1] + y,
                self.oorsprong[2] + x * self.rechts[1] + z * self.diep[1])

    def kant(self, naam):
        """A facing for block states: "in" (deeper into the room), "uit", "links", "rechts"."""
        r, d = self.rechts, self.diep
        return {"in": RICHTINGEN[d], "uit": RICHTINGEN[(-d[0], -d[1])], "rechts": RICHTINGEN[r], "links": RICHTINGEN[(-r[0], -r[1])]}[naam]

    def _mag(self, x, y, z):
        if not (0 <= x < self.B and 0 <= z < self.D and 0 <= y <= self.H):
            self.fout(f"({x}, {y}, {z}) is outside the room ({self.B} x {self.H} x {self.D})")

    def zet(self, x, y, z, blok, props=None, nbt=None):
        self._mag(x, y, z)
        self._zet(*self.bouw(x, y, z), blok, props, nbt)

    def vul(self, x0, y0, z0, x1, y1, z1, blok, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.zet(x, y, z, blok, props)

    def haal(self, x, y, z):
        self._mag(x, y, z)
        return self._haal(*self.bouw(x, y, z))

    def entity(self, x, y, z, nbt):
        self._mag(x, y, z)
        bx, by, bz = self.bouw(x, y, z)
        self._entity(bx + 0.5, by, bz + 0.5, nbt)

    def yaw(self, kijkt="uit"):
        """The yaw (degrees) of something that looks "uit" (to the entrance), "in", "links" or "rechts"."""
        return {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[self.kant(kijkt)]

    def npc(self, kind, bezetting_id, x, z, kijkt="uit", plek_naam=None):
        """A sitting quest NPC on the floor (features/wereld.npc), looking "uit" (towards the entrance) by default."""
        from features import wereld
        self.entity(x, 1, z, wereld.npc(self.h, kind, bezetting_id, plek_naam, self.yaw(kijkt)))

    def bord(self, x, y, z, kijkt, regels, sleutel):
        """A wall sign that looks "in" / "uit" / "links" / "rechts"; regels: up to four Dutch lines; sleutel: its lang prefix
        (sign.guhs.<sleutel>.<n>, written here)."""
        keys = []
        for i, regel in enumerate(regels[:4]):
            key = f"sign.guhs.{sleutel}.{i + 1}"
            self.h.lang(key, regel, regel)
            keys.append(json.dumps({"translate": key}) if regel else '""')
        keys += ['""'] * (4 - len(keys))
        ms = self.h.ms
        tekst = {"messages": ms.NbtList(8, keys), "color": "yellow", "has_glowing_text": ms.Byte(1)}
        leeg = {"messages": ms.NbtList(8, ['""'] * 4), "color": "black", "has_glowing_text": ms.Byte(0)}
        self.zet(x, y, z, "minecraft:crimson_wall_sign", {"facing": self.kant(kijkt), "waterlogged": "false"},
                 {"id": "minecraft:sign", "is_waxed": ms.Byte(1), "front_text": tekst, "back_text": leeg})
