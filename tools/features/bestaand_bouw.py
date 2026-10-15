"""
bbq2 (bestaand): what is ADDED to the two existing buildings of the Guhbarbecuether (features/spiesburcht_burcht.py builds
them; this slice never changes their templates, so nothing here is worldgen). Everything comes through Bezetting (Java:
feature/wereld), also at the copies that were generated long ago:

  Spiesburcht        bestaand_wachthokje      the sentry box of the Wachter-guh (3 x 6 x 3), next to the statue in the hall
                     bestaand_brugvuur_<k>    a fire bowl on a post on a carved pedestal on each of the four bridges (k: 0 east, 1 south,
                                              2 west, 3 north; the bowl's block state carries k)
  Mika-grillpaleis   bestaand_naaihoek        the sewing cage of the captive Knuffelmaker-guh on the first floor (the fourth
                                              corner, next to the three cages with stolen plush guhs)

PLEKKEN below is the single source of the spots (coordinates of the whole build, as Bezetting wants them for a guhs:burcht).
Java has the same numbers in feature/bestaand/Plekken.java; check(h) rebuilds both buildings and fails the build when a spot is
not free in the template (air with a floor under it) or when Plekken.java says something else.
"""
import os
import re

from features import spiesburcht_burcht as burcht

STENEN = burcht.STENEN
GEBEITELD = burcht.GEBEITELD
MUUR = burcht.MUUR
TRAP = burcht.TRAP
PLAAT = burcht.PLAAT
GEPOLIJST = burcht.GEPOLIJST
TRALIES = burcht.TRALIES
AIR = burcht.AIR
VUURKORF = "guhs:bestaand_vuurkorf"
LANTAARNTJE = "guhs:bestaand_zielig_lantaarntje"

C, Y = burcht.SB_C, burcht.SB_DECK              # the Spiesburcht: its middle and its deck
TUIN = Y + 10                                   # where you stand in the pindasaus-tuintje
GC, GF = burcht.GP_C, burcht.GP_G + 10          # the grillpaleis: its middle and where you stand on the first floor

BRUGGEN = ("oost", "zuid", "west", "noord")     # k = 0..3 (spiesburcht_burcht.rot)


def brugvuur_keuzes(k):
    """The spots a bridge fire may take, in order of preference: on the right of the walkway, a good third of the way out;
    then nearer and further, then the left side."""
    return [(x, Y + 1, z) for (x, z) in (burcht.rot(k, a, bb, C) for bb in (1, -1) for a in (35, 29, 41))]


PLEKKEN = {
    # --- Spiesburcht ---
    "WACHTHOKJE": [(C + 4, Y + 1, C - 4)],                      # corner of the 3 x 6 x 3 sentry box (open to the north)
    "WACHTER": [(C + 5, Y + 1, C - 3)],                         # the Wachter-guh stands in it
    "BRUGVUUR_0": brugvuur_keuzes(0), "BRUGVUUR_1": brugvuur_keuzes(1), "BRUGVUUR_2": brugvuur_keuzes(2), "BRUGVUUR_3": brugvuur_keuzes(3),
    "MIKAKRUID": [(C - 4, TUIN, C - 2), (C + 5, TUIN, C + 1), (C, TUIN, C - 5), (C, TUIN, C + 4), (C - 7, TUIN, C + 2), (C + 7, TUIN, C - 4)],
    # --- Mika-grillpaleis ---
    "NAAIHOEK": [(GC + 5, GF, GC + 4)],                         # corner of the 5 x 4 x 6 sewing cage (its sign row first)
    "KNUFFELMAKER": [(GC + 7, GF, GC + 7)],
    "KOOIEN": [(GC - 7, GF, GC - 7), (GC + 7, GF, GC - 7), (GC - 7, GF, GC + 7)],        # the middles of the three plush cages
    "KNUFFELPLEKKEN": [(GC + 6, GF, GC + 6), (GC + 8, GF, GC + 6), (GC + 8, GF, GC + 7)],  # where a freed plush sits (in the naaihoek)
}
WACHTER_YAW = 180.0             # (looks north, like the statue)
KNUFFELMAKER_YAW = 180.0        # (looks at the door of his cage)
# the plush guh in a cage, relative to the cage's middle: what a player who freed it no longer sees
KNUFFEL_IN_KOOI = [(-1, 0, 0), (0, 0, 0), (1, 0, 0), (0, 1, 0), (-1, 1, 0), (1, 1, 0), (0, 0, -1)]

HOKJE = (3, 6, 3)
BRUGVUUR = (1, 3, 1)
KORF_HOOGTE = 2                 # the bowl stands this far above the corner of its template (Java: Vuren.KORF_HOOGTE)
NAAIHOEK = (5, 4, 6)


# =====================================================================================================================
# the props
# =====================================================================================================================
def _muur(s, x, y, z, **zijden):
    props = {"up": "true", "north": "none", "east": "none", "south": "none", "west": "none", "waterlogged": "false"}
    props.update(zijden)
    s.set(x, y, z, MUUR, props)


def _trap(s, x, y, z, facing, half="bottom", shape="straight"):
    s.set(x, y, z, TRAP, {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"})


def wachthokje(h):
    """The sentry box (3 x 6 x 3, open to the north = template z 0): charcoal brick with a pink band like the cheeks of the
    guh faces on the keep, two wall posts in front, a little hipped roof with a carved guh face block as its knob, and the
    Wachter-guh's own Zielig lantaarntje hanging inside, above his plume (a preview of his reward)."""
    s = h.Structure(HOKJE)
    for y in range(4):
        band = "minecraft:pink_terracotta" if y == 2 else STENEN
        for x in range(3):
            s.set(x, y, 2, band)                                # the back wall
        s.set(0, y, 1, band)                                    # the side walls
        s.set(2, y, 1, band)
        _muur(s, 0, y, 0, south="tall" if y < 3 else "low")     # the two posts in front
        _muur(s, 2, y, 0, south="tall" if y < 3 else "low")
        s.set(1, y, 0, AIR)
        s.set(1, y, 1, AIR)
    # the roof: stairs all round, rising to the knob in the middle
    _trap(s, 1, 4, 0, "south")
    _trap(s, 1, 4, 2, "north")
    _trap(s, 0, 4, 1, "east")
    _trap(s, 2, 4, 1, "west")
    _trap(s, 0, 4, 0, "south", shape="outer_left")
    _trap(s, 2, 4, 0, "south", shape="outer_right")
    _trap(s, 0, 4, 2, "north", shape="outer_right")
    _trap(s, 2, 4, 2, "north", shape="outer_left")
    s.set(1, 4, 1, GEBEITELD)
    for x in range(3):
        for z in range(3):
            s.set(x, 5, z, AIR)
    s.set(1, 5, 1, PLAAT, {"type": "bottom", "waterlogged": "false"})
    s.set(1, 3, 1, LANTAARNTJE, {"hanging": "true", "blij": "false"})
    return s


def brugvuur(h, k):
    """A bridge fire (1 x 3 x 1): the fire bowl (out; its nr says which bridge) on a post on a carved pedestal, about as
    tall as the saté lamp posts of the bridge, so its flames show from the keep."""
    s = h.Structure(BRUGVUUR)
    s.set(0, 0, 0, GEBEITELD)
    _muur(s, 0, 1, 0)
    s.set(0, KORF_HOOGTE, 0, VUURKORF, {"lit": "false", "nr": str(k)})
    return s


def _tralies(s, cellen):
    """Grill bars that join each other (a template keeps the shapes it is given)."""
    for (x, y, z) in cellen:
        s.set(x, y, z, TRALIES, {"north": str((x, y, z - 1) in cellen).lower(), "south": str((x, y, z + 1) in cellen).lower(),
                                 "west": str((x - 1, y, z) in cellen).lower(), "east": str((x + 1, y, z) in cellen).lower(),
                                 "waterlogged": "false"})


def naaihoek(h, bord):
    """The sewing cage (5 x 4 x 6; template z 0 is the row of the sign in front of it, the door is in the north side): a cage
    like the three with the stolen plush guhs, but with its door ajar, a loom, a pile of wool and a soul lantern. `bord` =
    the sign's NBT maker (Bouw.sign of spiesburcht_burcht, so the text is a translate key)."""
    s = h.Structure(NAAIHOEK)
    for x in range(5):
        for z in range(6):
            for y in range(4):
                s.set(x, y, z, AIR)
    cellen = set()
    for x in range(5):
        for z in range(1, 6):
            if x in (0, 4) or z in (1, 5):
                for y in range(3):
                    if not (x == 2 and z == 1 and y < 2):       # the doorway
                        cellen.add((x, y, z))
            s.set(x, 3, z, GEPOLIJST)
    _tralies(s, cellen)
    s.set(1, 0, 4, "minecraft:loom", {"facing": "north"})
    s.set(3, 0, 4, "minecraft:pink_wool")
    s.set(3, 1, 4, "minecraft:white_wool")
    s.set(1, 0, 3, "minecraft:magenta_wool")
    s.set(2, 0, 2, "minecraft:pink_carpet")
    s.set(2, 2, 3, "minecraft:soul_lantern", {"hanging": "true", "waterlogged": "false"})
    bord(s, 3, 0, 0)
    return s


def props(h):
    """Builds and saves the props; returns {name: Structure}."""
    b = burcht.Bouw(h, NAAIHOEK, 13000)

    def bord(s, x, y, z):
        b.s = s
        b.sign(x, y, z, "north", ["bestaand.naai1", "bestaand.naai2", "bestaand.naai3", "bestaand.naai4"], wall=False)
    uit = {"bestaand_wachthokje": wachthokje(h), "bestaand_naaihoek": naaihoek(h, bord)}
    for k in range(4):
        uit[f"bestaand_brugvuur_{k}"] = brugvuur(h, k)
    for name, s in uit.items():
        s.save(name)
    return uit


# =====================================================================================================================
# the game test rooms
# =====================================================================================================================
TEST_BRUG = (16, 6, 16)
TEST_BURCHT = (24, 12, 24)
TEST_PALEIS = (24, 8, 24)
TEST_VUREN = [(3, 1, 3), (12, 1, 3), (3, 1, 12), (12, 1, 12)]      # the four fire bowls of bestaand_test_brug (nr = index)


def test_templates(h):
    """bestaand_test_brug: a floor and four fire bowls (nr 0..3) on pedestals. bestaand_test_burcht: a floor that stands for
    the hall of a Spiesburcht (the test maps the anchor of a fake copy onto its middle). bestaand_test_paleis: the first floor
    of a grillpaleis with the three plush cages where the real ones are (middle of the room = middle of the palace)."""
    brug = h.Structure(TEST_BRUG)
    for x in range(TEST_BRUG[0]):
        for z in range(TEST_BRUG[2]):
            brug.set(x, 0, z, STENEN)
    for k, (x, y, z) in enumerate(TEST_VUREN):
        brug.set(x, y, z, GEBEITELD)
        brug.set(x, y + 1, z, VUURKORF, {"lit": "false", "nr": str(k)})
    brug.save("bestaand_test_brug")
    hal = h.Structure(TEST_BURCHT)
    for x in range(TEST_BURCHT[0]):
        for z in range(TEST_BURCHT[2]):
            hal.set(x, 0, z, STENEN)
    hal.save("bestaand_test_burcht")
    paleis = burcht.Bouw(h, TEST_PALEIS, 13001)
    for x in range(TEST_PALEIS[0]):
        for z in range(TEST_PALEIS[2]):
            paleis.set(x, 0, z, GEPOLIJST)
    for (kx, _ky, kz) in PLEKKEN["KOOIEN"]:
        burcht.cage(paleis, kx - GC + 12, 1, kz - GC + 12)
    paleis.connect()
    paleis.s.save("bestaand_test_paleis")


# =====================================================================================================================
# checks
# =====================================================================================================================
def _java_plekken(h):
    """NAME -> [(x, y, z), ...] as feature/bestaand/Plekken.java has them."""
    pad = os.path.join(os.path.dirname(h.R), "java", "nl", "juiced", "guhs", "feature", "bestaand", "Plekken.java")
    if not os.path.exists(pad):
        raise SystemExit(f"bestaand: {pad} is missing")
    tekst = open(pad, encoding="utf-8").read()
    uit = {}
    for m in re.finditer(r"\b([A-Z][A-Z0-9_]*)\s*=\s*((?:List\.of\()?\s*new BlockPos\([^;]*);", tekst):
        uit[m.group(1)] = [tuple(int(v) for v in p) for p in re.findall(r"new BlockPos\((-?\d+),\s*(-?\d+),\s*(-?\d+)\)", m.group(2))]
    return uit


def check(h, gebouwd):
    """Every spot is free in the templates of the two buildings, and Plekken.java has the same numbers."""
    problemen = []
    sb = burcht.spiesburcht(h)
    gp = burcht.grillpaleis(h)

    def vrij(b, naam, hoek, maat, wat):
        vloer = 0
        for dx in range(maat[0]):
            for dz in range(maat[2]):
                for dy in range(maat[1]):
                    c = (hoek[0] + dx, hoek[1] + dy, hoek[2] + dz)
                    if b.get(*c) != AIR:
                        problemen.append(f"{naam}: {wat} at {hoek} needs air at {c}, the template has {b.get(*c)}")
                vloer += 1 if burcht.solid(b.get(hoek[0] + dx, hoek[1] - 1, hoek[2] + dz)) else 0
        if vloer * 2 < maat[0] * maat[2]:
            problemen.append(f"{naam}: {wat} at {hoek} has no floor under it")

    vrij(sb, "spiesburcht", PLEKKEN["WACHTHOKJE"][0], HOKJE, "the wachthokje")
    for k in range(4):
        for keuze in PLEKKEN[f"BRUGVUUR_{k}"]:
            vrij(sb, "spiesburcht", keuze, BRUGVUUR, f"the fire of bridge {BRUGGEN[k]}")
    for plek in PLEKKEN["MIKAKRUID"]:
        vrij(sb, "spiesburcht", plek, (1, 2, 1), "a mikakruid")
    wx, wy, wz = PLEKKEN["WACHTER"][0]
    hx, hy, hz = PLEKKEN["WACHTHOKJE"][0]
    if (wx - hx, wy - hy, wz - hz) != (1, 0, 1):
        problemen.append("the Wachter-guh does not stand in the middle of his wachthokje")
    vrij(gp, "mika_grillpaleis", PLEKKEN["NAAIHOEK"][0], NAAIHOEK, "the naaihoek")
    nx, ny, nz = PLEKKEN["NAAIHOEK"][0]
    kx, ky, kz = PLEKKEN["KNUFFELMAKER"][0]
    if (kx - nx, ky - ny, kz - nz) != (2, 0, 3):
        problemen.append("the Knuffelmaker-guh does not sit in the middle of his naaihoek")
    naai = gebouwd["bestaand_naaihoek"]
    for (px, py, pz) in PLEKKEN["KNUFFELPLEKKEN"]:
        if naai.get(px - nx, py - ny, pz - nz) != AIR:
            problemen.append(f"the naaihoek has something at the plush spot {(px, py, pz)}")
    for (cx, cy, cz) in PLEKKEN["KOOIEN"]:
        for (dx, dy, dz) in KNUFFEL_IN_KOOI:
            blok = gp.get(cx + dx, cy + dy, cz + dz)
            if blok not in ("minecraft:pink_wool", "minecraft:pink_carpet", "minecraft:black_wool"):
                problemen.append(f"mika_grillpaleis: no plush at {(cx + dx, cy + dy, cz + dz)} of the cage at {(cx, cy, cz)}: {blok}")
        if gp.get(cx + 2, cy, cz) != burcht.mc(TRALIES):
            problemen.append(f"mika_grillpaleis: no cage around {(cx, cy, cz)}")
    java = _java_plekken(h)
    for naam, plekken in PLEKKEN.items():
        if java.get(naam) != [tuple(p) for p in plekken]:
            problemen.append(f"Plekken.java {naam} = {java.get(naam)}, bestaand_bouw.py says {plekken}")
    if problemen:
        raise SystemExit("bestaand: the spots do not fit:\n  " + "\n  ".join(problemen[:40]))
    return sb, gp


def build(h):
    gebouwd = props(h)
    test_templates(h)
    check(h, gebouwd)
    return gebouwd
