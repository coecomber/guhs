"""
einde2, slice bouw_schelpenstad - the build itself: guhs:schelpenstad, a guhs:leegte_bouw of 256 x 48 x 96 in the deep band
that stands next to Bikinibodem (build contract 6.11). bouw(h) puts the parts together (the land, the road, De Ruige Kroeg,
the loopgraaf, Schelpenstad), records the markers of build contract 6.7, lights what you can walk on, checks everything
and returns the build; build(h) of the main module saves it.

The parts: einde2_bouw_schelpenstad_basis (numbers and tools), _land, _kroeg, _stad.
"""
from collections import deque

from features import guheinde_bouw as gb
from features import einde2_bouw_schelpenstad_kroeg as kroeg_
from features import einde2_bouw_schelpenstad_land as land
from features import einde2_bouw_schelpenstad_stad as stad_
from features.einde2_bouw_schelpenstad_basis import *  # noqa: F401,F403
from features.einde2_bouw_schelpenstad_basis import kit

# every marker of build contract 6.7 for this build (the standard three come from the kit)
VERWACHT = ["weg_naad", "weg_west", "weg_oost", ("rustpunt", 3),
            "ruige_kroeg", "kroeg_parkeer", "kroeg_podium", "bellenblaas_plek", "barman", "uitsmijter", ("stoere_mika", 6), "slapende_mika",
            "sleutel", "kroeg_sluip",
            "loopgraaf", "loopgraaf_rand_west", "loopgraaf_rand_oost", "pad_omlaag", "pad_omhoog", "loopgraaf_bodem", ("monster", 6), "mindy",
            "snor_plek", "dennis", "dennis_route",
            "schelpenstad", "strand", "souvenirwinkel", "cycloop", "droogtafel", "droogtafel_sponsguh", "droogtafel_patguh", "droogtafel_speler",
            "drooglamp", "stopcontact", "sproeier", ("souvenirplank", 6), "kroon", ("bewoner", 4),
            "kust", "hasselguh", "hasselguh_opstap", "mindy_strand",
            "knipoog_appa_vlucht", "knipoog_appa_sponsguh", "knipoog_appa_patguh", "knipoog_appa_camera"]
VLUCHT_Y = RAND_Y + 1 + 14          # Appa's line over the loopgraaf: this high (at least 12 above the rims)
GLOED = "minecraft:ochre_froglight"   # a glowing eye in the cheese of the open ground (the dimension has no sky light)
DIM, HELDER = 2, 4                  # the least light in the loopgraaf, and everywhere else you can walk


def weg_plekken(b, omlaag, omhoog):
    """The markers of the road and of the loopgraaf."""
    R = RAND_Y + 1                                               # you walk at 22 on the rims
    b.prop("weg_naad", 0, WEG_Y + 1, wegz(0), wie="het midden van de weg op de naad met Bikinibodem")
    b.pad("weg_west", land.weg_punten(0, X_WEST_EIND), wie="de weg van de naad langs De Ruige Kroeg naar de westrand van de loopgraaf")
    b.pad("weg_oost", land.weg_punten(X_OOST_BEGIN, X_OOST_EIND), wie="de weg van de oostrand de heuvel af, de stad in")
    b.zone("loopgraaf", KLOOF_W - 2, 1, KLOOF_Z0 - 5, KLOOF_O + 2, RAND_Y + 10, KLOOF_Z1 + 5, wie="de loopgraaf: diep, donker, vol monsters")
    b.zone("loopgraaf_bodem", KLOOF_W + 8, BODEM + 1, KLOOF_Z0 + 5, KLOOF_O - 8, BODEM + 5, KLOOF_Z1 - 5, wie="de bodem van de loopgraaf")
    b.prop("loopgraaf_rand_west", X_WEST_EIND, R, wegz(X_WEST_EIND), wie="hier houdt de weg op: de westrand")
    b.prop("loopgraaf_rand_oost", X_OOST_BEGIN, R, wegz(X_OOST_BEGIN), wie="hier begint de weg weer: de oostrand")
    b.pad("pad_omlaag", omlaag[::3] + [omlaag[-1]], wie="het pad omlaag langs de westwand")
    b.pad("pad_omhoog", omhoog[::3] + [omhoog[-1]], wie="het pad omhoog langs de oostwand")
    for i, (x, y, z, kijkt, _) in enumerate(land.NISSEN, 1):
        yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}[kijkt]
        b.plek(f"monster_{i}", x, y, z, yaw=yaw, wie="een loopgraafmonster op zijn zitplek", wezen="guhs:loopgraafmonster", los=not _)
    # the west rim: Mindy, the moustache, the rest point, the two who look up at Appa
    b.plek("mindy", 86, R, 52, yaw=180.0, wie="prinses Mindy-guh verschijnt hier, aan de westrand", wezen="guhs:mindy_guh")
    b.prop("snor_plek", 87, R, 53, wie="hier krijg je je snor van knabbelkoordraad")
    b.prop("rustpunt_2", 84, wegy(84) + 1, 44, wie="het rustpunt aan de westrand van de loopgraaf")
    b.plek("knipoog_appa_sponsguh", 87, R, 45, yaw=-90.0, wie="Sponsguh kijkt omhoog", wezen="guhs:sponsguh")
    b.plek("knipoog_appa_patguh", 87, R, 43, yaw=-90.0, wie="Patguh kijkt omhoog", wezen="guhs:patguh")
    b.pad("knipoog_appa_vlucht", [(82, VLUCHT_Y, 46), (136, VLUCHT_Y, 49)],
          wie="Appa-guh vliegt hier fluitend over de loopgraaf (9 breed vrij), Aang-guh zwaait")
    b.camera("knipoog_appa", (80, R + 1, 40), (108, VLUCHT_Y, 47), zicht=["knipoog_appa_sponsguh", "knipoog_appa_patguh"],
             wie="een lage camera: de twee aan de rand en de lucht boven de loopgraaf")
    # the east rim: Dennis on his motorbike, and his chase into town
    b.plek("dennis", 131, R, 50, yaw=-90.0, wie="Dennis-Mika duikt hier op, achter je, op de motor", wezen="guhs:dennis_mika")
    b.pad("dennis_route", land.weg_punten(131, X_OOST_EIND, stap=5) + [(PLEIN[0] - 4, STAD_Y + 1, PLEIN[1])],
          wie="de achtervolging over de weg de stad in (3 breed)")
    for id in ("mindy", "snor_plek", "rustpunt_2", "knipoog_appa_sponsguh", "knipoog_appa_patguh", "dennis", "loopgraaf_rand_west",
               "loopgraaf_rand_oost"):
        b.vast.add(tuple(b.plekken[id]["pos"]))


def rand_borden(b):
    """The signs at the two rims and at the seam."""
    R = RAND_Y + 1
    b.bord(88, R, 41, "west", ["LOOPGRAAF", "Diep. Donker.", "Vol monsters.", "Veel plezier!"], wall=False, wood="spruce")
    b.bord(88, R, 54, "west", ["Verboden voor", "kindjes zonder", "snor, njeg"], wall=False, wood="spruce")
    b.set(85, R, 40, "guhs:guh_bank", {"facing": "south"})
    b.paal(86, R, 40, 2, MUUR)
    b.bord(131, R, 44, "west", ["Je leeft nog!", "Schelpenstad:", "heuvel af en", "dan ruik je het"], wall=False, wood="spruce")
    b.paal(132, R, 57, 2, MUUR)
    b.paal(130, R, 43, 2, MUUR)
    b.bord(9, WEG_Y + 1, wegz(9) - 5, "west", ["Schelpenstad", "een eind rijden", "Ruige Kroeg", "vlakbij"], wall=False, wood="spruce")
    for c in ((88, R, 41), (88, R, 54), (131, R, 44)):
        b.vast.add(c)


# =====================================================================================================================
# light
# =====================================================================================================================
class Licht:
    """Block light of the build that can be topped up one lamp at a time (the same rules as the kit's licht())."""

    def __init__(self, b):
        self.b = b
        self.tabel = dict(kit.LICHT, **EIGEN_LICHT)
        self.waarde = {}
        todo = deque()
        for c, bl in b.s.blocks.items():
            if self.tabel.get(bl[0]):
                self.waarde[c] = self.tabel[bl[0]]
                todo.append(c)
        self._vloei(todo)

    def dicht(self, c):
        bl = self.b.s.blocks.get(c)
        if bl is None:
            return False
        n = bl[0]
        return n != gb.AIR and n not in kit.WATER and not self.b.is_dun(n) and not any(t in n for t in gb.TRANSPARENT_SUB) and n not in self.tabel

    def _vloei(self, todo):
        b, waarde = self.b, self.waarde
        while todo:
            c = todo.popleft()
            l = waarde[c] - 1
            if l <= 0:
                continue
            x, y, z = c
            for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if waarde.get(n, 0) < l and b.inside(*n) and not self.dicht(n):
                    waarde[n] = l
                    todo.append(n)

    def erbij(self, c, sterkte):
        if self.waarde.get(c, 0) < sterkte:
            self.waarde[c] = sterkte
            self._vloei(deque([c]))

    def op(self, c):
        return self.waarde.get(c, 0)


def in_loopgraaf(c):
    return KLOOF_W - 1 <= c[0] <= KLOOF_O + 1 and c[1] <= RAND_Y and KLOOF_Z0 - 4 <= c[2] <= KLOOF_Z1 + 4


def lichtwachter(b, lopen):
    """Puts a light wherever a walkable cell is too dark, in a fixed order (so the lights fall in a loose grid): a lantern on
    a low post on the land, a tiki torch on the beach, and in the loopgraaf only a faint glow (a soul lantern, or mould)."""
    licht = Licht(b)
    rng = b.rng
    geplaatst = {"gloed": 0, "lantaarn": 0, "fakkel": 0, "ziel": 0}

    def vrij(c):
        x, y, z = c
        return c not in b.vast and b.naam(x, y, z) is None and b.naam(x, y + 1, z) is None and b.naam(x, y + 2, z) is None \
            and b.solid_at((x, y - 1, z)) and not gb.is_fency(b.naam(x, y - 1, z)) and "_plaat" not in b.naam(x, y - 1, z) \
            and "slab" not in b.naam(x, y - 1, z) and "wool" not in b.naam(x, y - 1, z)

    for c in sorted(lopen):
        diep = in_loopgraaf(c)
        if licht.op(c) >= (DIM if diep else HELDER) + 1:
            continue
        x, y, z = c
        plek = None
        for r in range(0, 5):                                    # the nearest free cell at the same height
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    k = (x + dx, y, z + dz)
                    if max(abs(dx), abs(dz)) == r and plek is None and k in lopen and vrij(k):
                        plek = k
            if plek:
                break
        if plek is None:
            continue
        px, py, pz = plek
        onder = b.naam(px, py - 1, pz)
        if diep:
            b.set(px, py, pz, "minecraft:soul_lantern", gb.lamp(False))
            licht.erbij(plek, 10)
            geplaatst["ziel"] += 1
        elif onder == ZAND:
            b.set(px, py, pz, "guhs:tiki_fakkel")
            licht.erbij(plek, 15)
            geplaatst["fakkel"] += 1
        elif onder in (KORST, BARST, ROTS) and b.naam(px, py - 2, pz) is not None:
            b.set(px, py - 1, pz, GLOED)                         # a glowing eye in the cheese: no post in the way
            licht.erbij((px, py - 1, pz), 15)
            geplaatst["gloed"] += 1
        else:
            b.set(px, py, pz, MUUR)
            b.set(px, py + 1, pz, "minecraft:lantern", gb.lamp(False))
            licht.erbij((px, py + 1, pz), 15)
            geplaatst["lantaarn"] += 1
        b.vast.add(plek)
    return geplaatst


def begroeiing(b, tops):
    """Dry bushes and korstgras on the open ground, a few boulders."""
    rng = b.rng
    for (x, z), top in sorted(tops.items()):
        if KLOOF_W - 2 <= x <= KLOOF_O + 2:
            continue
        if x >= STAD_X and rng.random() < 0.55:                  # (town is tidier than the plain)
            continue
        c = (x, top + 1, z)
        onder = b.naam(x, top, z)
        if c in b.vast or b.naam(*c) is not None or onder not in (KORST, BARST, ROTS):
            continue
        r = rng.random()
        if r < 0.035:
            b.set(x, top + 1, z, "guhs:dorre_korststruik")
        elif r < 0.075 and onder != ROTS:
            b.set(x, top + 1, z, "guhs:korstgras")
        elif r < 0.081 and abs(z - wegz(x)) > 9 and b.naam(x, top + 2, z) is None:
            b.set(x, top + 1, z, ROTS)
            if rng.random() < 0.5 and (x + 1, top + 1, z) not in b.vast and b.naam(x + 1, top + 1, z) is None and b.solid_at((x + 1, top, z)):
                b.set(x + 1, top + 1, z, ROTS)


def eigen_fouten(b, lopen, omlaag, omhoog):
    """What the kit's check does not look at: the seam, the two roads, the light (dim in the loopgraaf, bright elsewhere),
    the free room of the big spots, the paths, Appa's line."""
    p = kit.check_naad(b, "west")
    p += land.weg_fouten(b, 0, X_WEST_EIND, "weg_west")
    p += land.weg_fouten(b, X_OOST_BEGIN, X_OOST_EIND, "weg_oost")
    licht = b.licht()
    donker = [c for c in lopen if licht.get(c, 0) < (DIM if in_loopgraaf(c) else HELDER)]
    if donker:
        p.append(f"{len(donker)} beloopbare plekken te donker, bv. {sorted(donker)[:6]}")
    fel = [c for c in lopen if in_loopgraaf(c) and c[1] <= 8 and licht.get(c, 0) > 12]
    if len(fel) > 60:
        p.append(f"de bodem van de loopgraaf is te licht: {len(fel)} plekken boven 12")

    def vrij(naam, x0, y0, z0, x1, y1, z1):
        vol = [(x, y, z) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1) for z in range(z0, z1 + 1) if b.s.blocks.get((x, y, z)) is not None]
        if vol:
            p.append(f"{naam}: niet vrij, bv. {b.naam(*vol[0])} op {vol[0]} ({len(vol)} blokken)")

    x, y, z = b.plekken["kroeg_parkeer"]["pos"]
    vrij("kroeg_parkeer (5 x 4 x 7)", x - 2, y, z - 3, x + 2, y + 3, z + 3)
    x, y, z = b.plekken["hasselguh"]["pos"]
    vrij("hasselguh (13 x 6 x 13)", x - 6, y, z - 6, x + 6, y + 5, z + 6)
    x, y, z = b.plekken["kroon"]["pos"]
    vrij("kroon (1 x 2 x 1)", x, y, z, x, y + 1, z)
    (xa, ya, za), (xb, yb, zb) = b.plekken["knipoog_appa_vlucht"]["punten"]
    vrij("knipoog_appa_vlucht (9 breed)", xa, ya - 2, min(za, zb) - 4, xb, ya + 4, max(za, zb) + 4)
    if ya < RAND_Y + 1 + 12:
        p.append("knipoog_appa_vlucht ligt minder dan 12 boven de randen")
    for naam, stappen in (("pad_omlaag", omlaag), ("pad_omhoog", omhoog)):
        weg_ = [c for c in stappen if c not in lopen]
        if weg_:
            p.append(f"{naam}: {len(weg_)} stappen niet beloopbaar, bv. {weg_[:4]}")
        for a, c in zip(stappen, stappen[1:]):
            if abs(a[1] - c[1]) > 1:
                p.append(f"{naam}: een stap van {abs(a[1] - c[1])} bij {c}")
    for id in ("weg_west", "weg_oost", "kroeg_sluip", "dennis_route"):
        mis = [q for q in b.plekken[id]["punten"] if tuple(q) not in lopen]
        if mis:
            p.append(f"pad {id}: punten waar je niet kunt lopen: {mis[:4]}")
    for id, e in b.plekken.items():                              # nothing stands on the spot of a character
        if e["soort"] == "npc" and id not in b.los:
            x, y, z = e["pos"]
            if b.s.blocks.get((x, y, z)) is not None or b.s.blocks.get((x, y + 1, z)) is not None:
                p.append(f"plek {id}: er staat iets op")
    for i, (x, y, z, kijkt, _) in enumerate(land.NISSEN, 1):     # a monster is 2.1 high and 1.2 wide: three free on something solid
        if any(b.s.blocks.get((x, y + d, z)) is not None for d in range(3)) or not b.solid_at((x, y - 1, z)):
            p.append(f"monster_{i} {(x, y, z)}: geen drie vrije blokken op een vaste richel")
    gebruikt = {bl[0] for bl in b.s.blocks.values()}
    for blok in EIGEN_BLOKKEN:
        if blok not in gebruikt:
            p.append(f"{blok} staat nergens in de bouw")
    tegels = (W + kit.TILE - 1) // kit.TILE, (D + kit.TILE - 1) // kit.TILE
    if tegels != (8, 3):
        p.append(f"tegels {tegels}")
    return p


def bouw(h):
    """The whole build, checked. Returns (b, start): start = where a player stands after arriving."""
    b = Stad(h)
    tops = land.slab(b)
    land.weg(b)
    omlaag, omhoog = land.loopgraaf(b)
    kroeg_.kroeg(b)
    stad_.stad(b)
    weg_plekken(b, omlaag, omhoog)
    rand_borden(b)
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    w = kit.Loper(b)
    lopen = w.bfs(start)
    kloofvloer = {c for c in lopen if in_loopgraaf(c)}
    land.schimmel(b, kloofvloer, b.rng)
    begroeiing(b, tops)
    # light for everything you can walk on, and for the open ground you could jump to
    grond = {(x, t + 1, z) for (x, z), t in tops.items() if w.standable((x, t + 1, z))}
    b.verlichting = lichtwachter(b, kit.Loper(b).bfs(start) | grond)
    b.connect()
    lopen = kit.Loper(b).bfs(start)
    fouten = b.check(start, VERWACHT, min_licht=1) + eigen_fouten(b, lopen, omlaag, omhoog)
    b.lopen = lopen
    return b, start, fouten
