"""
einde2, slice bouw_zij2: the Leegtehaven (any of the six outer biomes). Build only: trade, ferry, shipyard and the pub's
questline are a later phase, which reads the markers. The four Ender-Mika traders are placed for real (Handelaar).

A floating harbour: a lump of crumb rock with a stone quay on it and wooden decks around, and from it
  - north: two long steigers, each with a vetschip of the Mika's moored alongside (you can go aboard and into the hold);
  - the market between the steigers: four stalls, an Ender-Mika trader behind each counter, the prikbord in front;
  - east: the veerpier with the ticket booth and the ferry "De Vadsige Golf" (a sweet guh on her sail, a wide open deck);
  - west: the scheepswerf: the helling (the cradle your own vetbootje is built on, 13 x 9 and 7 high free above it), the
    crane, the loods;
  - south: the landing deck, the lighthouse with guh ears, the pub De Vadsige Walvis with the old captain's corner, and the
    empty kitchen of De Spijtige Spatel (the Mika-kok moves in later).

    maak(h) -> (the kit.Bouw, its problems); bouw(h) raises SystemExit on a problem
"""
import math
import random

import features.einde2_kit as kit
from features import einde2_bouw_zij2_gereedschap as g
from features import guheinde_bouw as gb

NAAM = "leegtehaven"
W, H, D = 144, 60, 116
Y = 176                                   # the world y of template y 0
DY = 30                                   # the deck: the layer of the floor blocks (you stand one higher)
CX, CZ = 72, 74
PLANK, BALK, PAAL, HEK = "spruce_planks", "stripped_spruce_log", "dark_oak_log", "spruce_fence"
KADE = [("guhs:kruimelrots_stenen", 55), ("guhs:kaaskorst_stenen", 30), ("guhs:gebarsten_kaaskorst_stenen", 15)]
PLEIN = (47, 55, 97, 95)                  # the quay: x0, z0, x1, z1
STEIGERS = {1: (56, 14, 60, 54), 2: (84, 14, 88, 54)}
VEERPIER = (98, 74, 130, 78)
WERF = (8, 62, 46, 90)
LANDING = (66, 96, 78, 110)
HELLING = (12, 72, 24, 80)                # the cradle: 13 long (x), 9 wide (z)
KROEG = (80, 80, 95, 95)
KRAMEN = [(61, "purple", "Flitskraam"), (67, "yellow", "Vet & Honing"), (73, "orange", "Bijna Vers"), (79, "magenta", "Teruggevonden")]


def vlonder(b, doos, vloer, hek_open=(), palen=6):
    """A wooden deck: planks on the floor layer, a beam under its rim, posts hanging down every `palen` blocks, a fence
    along the rim except where another floor already lies next to it and except the cells of hek_open."""
    x0, z0, x1, z1 = doos
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) in vloer:
                continue
            vloer.add((x, z))
            b.set(x, DY, z, PLANK if (x + z) % 5 else "spruce_planks")
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            if not rand:
                continue
            buiten = [(x + dx, z + dz) for dx, dz in gb.DIRS.values() if (x + dx, z + dz) not in vloer]
            if not buiten:
                continue
            if b.get(x, DY - 1, z) is None:
                b.set(x, DY - 1, z, BALK, {"axis": "x" if z in (z0, z1) else "z"})
            if ((x - x0) % palen == 0 and z in (z0, z1)) or ((z - z0) % palen == 0 and x in (x0, x1)):
                for y in range(DY - 5, DY - 1):
                    if b.get(x, y, z) is None:
                        b.set(x, y, z, PAAL, {"axis": "y"})
            if (x, z) not in hek_open:
                b.set(x, DY + 1, z, HEK)
                if ((x - x0) % palen == 0 and z in (z0, z1)) or ((z - z0) % palen == 0 and x in (x0, x1)):
                    b.set(x, DY + 2, z, "guhs:lampion_geel", gb.lamp(False))


def eiland(b):
    grond = g.eiland(b, CX, CZ, 27, 24, DY, 15, "guhs:kruimelrots_stenen", [("guhs:kruimelrots", 65), ("guhs:kaaskorst", 35)], NAAM,
                     rafel=0.14, onder="guhs:kruimelrots")
    vloer = set()
    x0, z0, x1, z1 = PLEIN
    rng = random.Random(kit.seed(NAAM + "/kade"))
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) in grond:
                b.set(x, DY, z, gb.pick(rng, KADE))
                vloer.add((x, z))
    for (x, z) in grond:                      # (rock outside the quay rectangle: a low kerb, nobody walks there)
        if (x, z) not in vloer:
            b.set(x, DY, z, "guhs:kruimelrots")
    return grond, vloer


def dekken(b, vloer):
    """Every wooden deck. The openings in the fences are where gangways, stairs and other decks join."""
    open_ = set()
    for z in (37, 38):
        open_ |= {(56, z), (60, z)}
    for z in (32, 33):
        open_ |= {(84, z), (88, z)}
    for x in (112, 113, 114):
        open_.add((x, 78))
    open_ |= {(x, 110) for x in range(69, 76)}                                    # (the landing deck is open to the south: you fly in)
    open_ |= {(8, z) for z in range(HELLING[1], HELLING[3] + 1)}                  # (the slip runs off the west rim)
    vlonder(b, (PLEIN[0], PLEIN[1], PLEIN[2], PLEIN[3]), vloer, open_)
    for doos in STEIGERS.values():
        vlonder(b, doos, vloer, open_)
    vlonder(b, VEERPIER, vloer, open_)
    vlonder(b, WERF, vloer, open_)
    vlonder(b, LANDING, vloer, open_)
    # fences that ended up between two decks (a pier meeting the quay): take them out
    for (x, z) in sorted(vloer):
        if b.get(x, DY + 1, z) == gb.mc(HEK) and all((x + dx, z + dz) in vloer for dx, dz in gb.DIRS.values()):
            b.s.blocks.pop((x, DY + 1, z), None)
            if b.get(x, DY + 2, z) == "guhs:lampion_geel":
                b.s.blocks.pop((x, DY + 2, z), None)


def schepen(b):
    """The two vetschepen at the steigers and the ferry at the veerpier."""
    # vetschip 1: west of steiger 1, bow north, the Mika's face on the main sail
    s1 = g.Schip(b, 48, DY, 52, "north", naam="vetschip_1")
    vetschip(b, s1, 1, gb.mika_face_rows(9, 9, "b"))
    for z in (37, 38):
        for x in (54, 55):
            b.set(x, DY, z, PLANK)
    # vetschip 2: east of steiger 2, bow south, striped sails
    s2 = g.Schip(b, 96, DY, 18, "south", naam="vetschip_2")
    vetschip(b, s2, 2, None)
    for z in (32, 33):
        for x in (89, 90):
            b.set(x, DY, z, PLANK)
    b.sign(57, DY + 1, 42, "east", ["De Vette Hap", "Thuishaven:", "Mika-vesting", "Njeg aanraken"], wall=False)
    b.sign(87, DY + 1, 28, "west", ["De Gladde Aal", "Lading: vet.", "Bemanning: weg", "(knabbelpauze)"], wall=False)
    # the ferry: south of the veerpier, bow east, wide, one mast with the guh sail, an open deck
    veer = g.Schip(b, 100, DY, 87, "east", lengte=32, breedte=6, naam="veer")
    veer.romp(diep=5)
    veer.dek(reling_open=[(-1, 12, 14)])
    veer.mast(26, 13, 9, 8, g.guh_zeil(9, 8))
    veer.boegbeeld(guh=True)
    for x in (112, 113, 114):
        for z in (79, 80):
            b.set(x, DY, z, PLANK)
    # benches along the rails, a lantern mast at the stern, the wheel
    for v in (6, 9, 16, 19):
        for u, kant in ((-4, "stuurboord"), (4, "bakboord")):
            if not (u < 0 and 12 <= v <= 14):
                veer.zet(u, 1, v, "guhs:guh_bank", {"facing": veer.richting(kant)})
    for k in range(1, 5):
        veer.zet(0, k, 2, HEK)
    veer.zet(0, 5, 2, "guhs:lampion_roze", gb.lamp(False))
    veer.zet(0, 1, 4, HEK)
    veer.zet(0, 2, 4, "grindstone", {"face": "wall", "facing": veer.richting("boeg")})
    for u in (-3, 3):
        veer.zet(u, 1, 23, HEK)
        veer.zet(u, 2, 23, "guhs:lampion_geel", gb.lamp(False))
    wx0, wz0 = veer.w(-4, 5)
    wx1, wz1 = veer.w(4, 21)
    b.zone("veer_dek", min(wx0, wx1), DY + 1, min(wz0, wz1), max(wx0, wx1), DY + 6, max(wz0, wz1),
           wie="het open dek van veerboot De Vadsige Golf: hier staan de passagiers")
    return s1, s2, veer


def vetschip(b, s, nr, zeil):
    """A vetschip of the Mika's: hull of terracotta and honey, two masts, a stern cabin, a hatch with stairs into the hold
    (barrels of vet, a modest chest), the wheel, lampions."""
    s.romp()
    s.dek(reling_open=[(1, 14, 15)])
    s.mast(22, 14, 9, 9, zeil)
    s.mast(12, 11, 7, 6)
    s.boegbeeld()
    s.zet(0, 15, 22, "purple_wool")
    # the hatch (port side, between the masts) and its stairs down into the hold
    for i in range(1, 6):
        for u in (-2, -3):
            s.zet(u, 0, 15 + i, gb.AIR)
            g.trap(b, *_w(s, u, -i, 15 + i), "spruce_stairs", s.richting("hek"))
    for u in (-2, -3):
        for i in range(1, 6):
            for y in range(-5, -i):
                s.zet(u, y, 15 + i, "brown_terracotta")
    for v in range(16, 22):
        s.zet(-1, 1, v, HEK)
    for u in (-1, -2, -3):
        s.zet(u, 1, 21, HEK)
    # the hold
    for u, v in ((2, 8), (2, 9), (-2, 10), (1, 24), (-1, 25), (2, 26)):
        s.zet(u, -5, v, "barrel", {"facing": "up", "open": "false"})
    s.zet(2, -4, 8, "barrel", {"facing": "up", "open": "false"})
    s.zet(0, -5, 27, "honey_block")
    cx, cz = s.w(0, 6)
    b.chest(cx, DY - 5, cz, s.richting("boeg"), "guhs:chests/einde2_bouw_zij2_ruim")
    for v in (9, 14, 24):
        hx, hz = s.w(0, v)
        b.set(hx, DY - 1, hz, "guhs:lampion_geel", gb.lamp(True))
        b.set(hx, DY, hz, "brown_terracotta")
    sx, sz = s.w(1, 13)
    b.sign(sx, DY - 5, sz, s.richting("hek"), ["VETVOORRAAD", "Niet likken!", "(de kapitein", "telt de likjes)"] if nr == 1 else
           ["RUIM", "Vet, vet en", "nog een ton", "vet. Njeg."], wall=False)
    # the stern: a low cabin you can walk through, the wheel on its roof? No: the wheel in front of it, on the deck
    for u in range(-3, 4):
        for v in range(1, 6):
            wand = abs(u) == 3 or v in (1, 5)
            for y in (1, 2, 3):
                if wand:
                    s.zet(u, y, v, "yellow_terracotta" if y != 1 else "brown_terracotta")
            s.zet(u, 4, v, "orange_terracotta" if wand else "brown_terracotta")
    for y in (1, 2):
        s.zet(0, y, 5, gb.AIR)
    for u in (-3, 3):
        s.zet(u, 2, 3, "yellow_stained_glass_pane")
    s.zet(0, 3, 3, "guhs:lampion_roze", gb.lamp(True))
    s.zet(-2, 1, 2, "guhs:mikatrofee", {"facing": s.richting("boeg")})
    g.bed(b, *_w(s, 2, 1, 2), s.richting("boeg"), "purple")
    s.zet(0, 1, 8, HEK)
    s.zet(0, 2, 8, "grindstone", {"face": "wall", "facing": s.richting("hek")})
    for u, v in ((-5, 24), (5, 24), (-5, 9), (5, 9)):
        s.zet(u, 2, v, "guhs:lampion_geel", gb.lamp(False))
    x0, y0, z0, x1, y1, z1 = s.doos()
    b.zone(f"vetschip_{nr}", x0, y0, z0, x1, y1, z1, wie=f"het afgemeerde vetschip aan steiger {nr}")
    sx0, sz0, sx1, sz1 = STEIGERS[nr]
    b.zone(f"steiger_{nr}", sx0, DY + 1, sz0, sx1, DY + 6, sz1, wie=f"steiger {nr}")


def _w(s, u, y, v):
    x, z = s.w(u, v)
    return x, s.y + y, z


def markt(b):
    """Four stalls in a row between the steigers, their backs to the north; a trader behind each counter."""
    for i, (x0, kleur, naam) in enumerate(KRAMEN, 1):
        for x in range(x0, x0 + 5):
            for y in range(DY + 1, DY + 4):
                b.set(x, y, 55, "stripped_spruce_log" if x in (x0, x0 + 4) else PLANK, {"axis": "y"} if x in (x0, x0 + 4) else None)
            for z in range(55, 61):
                b.set(x, DY + 5, z, f"{kleur}_wool" if (x - x0) % 2 == 0 else "white_wool")
            b.set(x, DY + 4, 55, PLANK)
        for x in (x0, x0 + 4):
            for y in range(DY + 1, DY + 5):
                b.set(x, y, 59, HEK)
        for x in range(x0 + 1, x0 + 4):
            b.set(x, DY + 1, 59, "barrel", {"facing": "south", "open": "false"} if x != x0 + 2 else None)
            if x == x0 + 2:
                b.set(x, DY + 1, 59, "stripped_dark_oak_log", {"axis": "x"})
        g.hanglamp(b, x0 + 2, DY + 4, 58, ketting=0, lamp="guhs:lampion_roze")
        b.wezen(f"handelaar_{i}", x0 + 2, DY + 1, 57, "guhs:ender_mika", yaw=0.0, honk=0, Handelaar=b.h.ms.Byte(1))
    # what lies on the counters and hangs on the back walls
    waar = {0: [("amethyst_cluster", {"facing": "up", "waterlogged": "false"}), ("crying_obsidian", None)],
            1: [("honey_block", None), ("honeycomb_block", None)],
            2: [("guhs:block_of_kaasknabbels", None), ("cake", {"bites": "2"})],
            3: [("guhs:block_of_kaasknabbels", None), ("guhs:mikatrofee", {"facing": "south"})]}
    for i, (x0, _kleur, _naam) in enumerate(KRAMEN):
        (a, ap), (c, cp) = waar[i]
        b.set(x0 + 1, DY + 2, 59, a, ap)
        b.set(x0 + 3, DY + 2, 59, c, cp)
        b.set(x0 + 1, DY + 1, 56, "barrel", {"facing": "up", "open": "false"})
        b.set(x0 + 3, DY + 1, 56, "barrel", {"facing": "up", "open": "false"})
    teksten = [["Flitskraam", "Flitskruimels", "vers gevangen", "(flitsen na)"], ["Vet & Honing", "Proeven mag.", "Likken kost", "een knabbel."],
               ["Bijna Vers", "Knabbels van", "gisteren. Of", "eergisteren."], ["Teruggevonden", "Lijkt dit op", "jouw knabbel?", "Toeval. Njeg."]]
    for (x0, _k, _n), tekst in zip(KRAMEN, teksten):
        b.sign(x0 + 2, DY + 1, 60, "south", tekst)
    b.zone("markt", 61, DY + 1, 55, 83, DY + 6, 66, wie="de markt van de Ender-Mika's")
    # the prikbord: a board on two posts in the middle of the quay, looking south at whoever lands
    for x in (70, 74):
        for y in range(DY + 1, DY + 5):
            b.set(x, y, 68, PAAL, {"axis": "y"})
    for x in range(71, 74):
        for y in (DY + 2, DY + 3):
            b.set(x, y, 68, "dark_oak_planks")
        g.plaat(b, x, DY + 4, 68, "spruce_slab")
    b.prop("prikbord", 72, DY + 2, 68, wie="het prikbord: 3 breed, 2 hoog, kijkt naar het zuiden")
    b.sign(71, DY + 3, 69, "south", ["GEZOCHT", "Wie jat hier", "steeds mijn", "knabbels?!"])
    b.sign(73, DY + 3, 69, "south", ["Te koop:", "vetschip,", "lekt een", "beetje vet"])
    b.sign(72, DY + 2, 69, "south", ["Leegtehaven", "Veer: oost", "Werf: west", "Kroeg: zuid"])
    b.sign(71, DY + 2, 69, "south", ["Walvis gezien?", "Meld het in", "de kroeg.", "(lacht njeg)"])
    for x in (70, 74):
        b.set(x, DY + 5, 68, "guhs:lampion_geel", gb.lamp(False))


def veer(b):
    """The veerpier: the ticket booth where the pier leaves the quay, the ferry master's spot, the timetable."""
    b.zone("veerpier", VEERPIER[0], DY + 1, VEERPIER[1], VEERPIER[2], DY + 6, VEERPIER[3], wie="de veerpier")
    # the booth: 3 x 3 on the quay just north of the pier's root
    for x in range(94, 98):
        for z in range(69, 73):
            wand = x in (94, 97) or z in (69, 72)
            for y in range(DY + 1, DY + 4):
                if wand:
                    b.set(x, y, z, "stripped_spruce_log" if (x in (94, 97) and z in (69, 72)) else "pink_terracotta",
                          {"axis": "y"} if (x in (94, 97) and z in (69, 72)) else None)
            b.set(x, DY + 4, z, "spruce_slab", gb.slab())
    b.set(95, DY + 2, 72, gb.AIR)
    b.set(96, DY + 2, 72, gb.AIR)
    b.set(95, DY + 3, 72, "pink_terracotta")
    g.deur(b, 94, DY + 1, 70, "west", scharnier="left")
    b.set(95, DY + 3, 70, "guhs:lampion_roze", gb.lamp(True))
    b.sign(95, DY + 3, 73, "south", ["VEERDIENST", "De Vadsige Golf", "vaart als ze", "zin heeft"])
    b.plek("veerman", 100, DY + 1, 76, yaw=90.0, wie="de veerman-guh, aan het begin van de veerpier bij de loopplank", kind="VEERGUH")
    b.set(104, DY + 2, 74, HEK)
    b.sign(104, DY + 2, 75, "south", ["Dienstregeling", "Heen: straks.", "Terug: later.", "Vol = vol."])
    b.set(104, DY + 3, 74, "guhs:lampion_geel", gb.lamp(False))


def werf(b):
    """The scheepswerf on the west deck."""
    x0, z0, x1, z1 = HELLING
    b.zone("werf", WERF[0], DY + 1, WERF[1], WERF[2], DY + 12, WERF[3], wie="de scheepswerf")
    # the cradle: keel blocks and ribs in the floor, free above
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            midden = z == (z0 + z1) // 2
            b.set(x, DY, z, "stripped_dark_oak_log" if midden else ("dark_oak_planks" if x % 3 == 0 else PLANK), {"axis": "x"} if midden else None)
    b.prop("helling", (x0 + x1) // 2, DY, (z0 + z1) // 2,
           wie=f"de helling: de wieg is x {x0}-{x1}, z {z0}-{z1}; daarboven 7 hoog vrij (hier wordt het vetbootje gebouwd)")
    # the slip runs on over the west rim: two rails sloping down
    for k in range(1, 5):
        for z in (z0 + 2, z1 - 2):
            b.set(WERF[0] - k, DY - (k + 1) // 2, z, "stripped_dark_oak_log", {"axis": "x"})
            if k == 3:
                b.set(WERF[0] - k, DY - 1, z, "stripped_dark_oak_log", {"axis": "x"})
    for x, z in ((x0 + 2, (z0 + z1) // 2), (x1 - 2, (z0 + z1) // 2), ((x0 + x1) // 2, z0 + 1), ((x0 + x1) // 2, z1 - 1), (x0 + 1, z0 + 1),
                 (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
        b.set(x, DY, z, "guhs:guh_kristal_lamp")              # (lamps IN the cradle floor: nothing may stand on it)
    # four masts around the cradle with a beam frame on top, and the crane's hook
    for x in (x0 - 1, x1 + 1):
        for z in (z0 - 1, z1 + 1):
            for y in range(DY + 1, DY + 10):
                b.set(x, y, z, PAAL, {"axis": "y"})
    for x in range(x0 - 1, x1 + 2):
        for z in (z0 - 1, z1 + 1):
            b.set(x, DY + 9, z, BALK, {"axis": "x"})
    for z in range(z0 - 1, z1 + 2):
        for x in (x0 - 1, x1 + 1, (x0 + x1) // 2):
            b.set(x, DY + 9, z, BALK, {"axis": "z"})
    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
    b.set(mx, DY + 8, mz, "chain", {"axis": "y", "waterlogged": "false"})
    for x, z in ((x0 - 1, z0 - 1), (x1 + 1, z0 - 1), (x0 - 1, z1 + 1), (x1 + 1, z1 + 1)):
        b.set(x, DY + 10, z, "guhs:lampion_geel", gb.lamp(False))
    for x in (x0 + 3, x1 - 3):
        for z in (z0 - 1, z1 + 1):
            b.set(x, DY + 8, z, "guhs:lampion_geel", gb.lamp(True))
    b.plek("scheepsbouwer", x1 + 4, DY + 1, mz, yaw=90.0, wie="de scheepsbouwer-guh, kijkt uit over de helling", kind="SCHEEPSBOUWERGUH")
    b.sign(x1 + 3, DY + 1, z0 - 2, "east", ["SCHEEPSWERF", "Njeg & Zonen", "Wij lijmen", "alles met vet"], wall=False)
    # timber and sailcloth
    for k, (x, z) in enumerate(((28, 84), (29, 84), (30, 84), (28, 85), (29, 85), (30, 85))):
        b.set(x, DY + 1, z, "stripped_spruce_log" if k % 2 else "stripped_dark_oak_log", {"axis": "x"})
    for x, z in ((28, 84), (29, 84), (29, 85)):
        b.set(x, DY + 2, z, "stripped_spruce_log", {"axis": "x"})
    for k, kleur in enumerate(("pink", "white", "orange", "purple")):
        b.set(34 + k, DY + 1, 86, f"{kleur}_wool")
    b.set(35, DY + 2, 86, "brown_wool")
    b.set(39, DY + 1, 86, "loom", {"facing": "north"})
    for x, z in ((14, 66), (15, 66), (14, 67), (20, 86), (21, 86)):
        g.ton(b, x, DY + 1, z)
    b.set(14, DY + 2, 66, "honey_block")
    b.sign(16, DY + 1, 66, "east", ["Scheepslijm", "(het is vet)", "", ""], wall=False)
    # the loods: a shed with the bench where the boat is drawn up
    lx0, lz0, lx1, lz1 = 30, 63, 44, 70
    for x in range(lx0, lx1 + 1):
        for z in range(lz0, lz1 + 1):
            wand = x in (lx0, lx1) or z in (lz0, lz1)
            hoek = x in (lx0, lx1) and z in (lz0, lz1)
            b.set(x, DY, z, "dark_oak_planks")
            for y in range(DY + 1, DY + 5):
                if hoek:
                    b.set(x, y, z, PAAL, {"axis": "y"})
                elif wand:
                    b.set(x, y, z, "glass_pane" if y in (DY + 2, DY + 3) and (x + z) % 4 == 0 else PLANK)
            b.set(x, DY + 5, z, "dark_oak_slab" if not wand else "dark_oak_planks", gb.slab() if not wand else None)
    g.deur(b, 37, DY + 1, lz1, "south")
    b.set(37, DY + 3, lz1, PLANK)
    for x, blok, props in ((31, "crafting_table", None), (32, "smithing_table", None), (33, "cartography_table", None),
                           (35, "barrel", {"facing": "up", "open": "false"}), (36, "barrel", {"facing": "up", "open": "false"})):
        b.set(x, DY + 1, lz0 + 1, blok, props)
    b.set(43, DY + 1, lz0 + 1, "grindstone", {"face": "floor", "facing": "west"})
    b.set(43, DY + 1, lz0 + 3, "loom", {"facing": "west"})
    b.set(40, DY + 1, lz0 + 1, "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    for x in (33, 41):
        b.set(x, DY + 4, lz0 + 3, "guhs:lampion_geel", gb.lamp(True))
    b.sign(38, DY + 2, lz1 + 1, "south", ["De Loods", "Hier tekenen", "wij bootjes.", "Vaak scheef."])
    b.sign(39, DY + 2, lz0 + 1, "south", ["Bouwtekening", "vetbootje:", "voor = punt", "achter = njeg"])


def vuurtoren(b):
    """The lighthouse on the south-west of the quay: a round tower, a lamp room, and two guh ears on top."""
    cx, cz = 53, 89
    r = 3.3
    for y in range(DY + 1, DY + 17):
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                d = math.hypot(x - cx, z - cz)
                if r - 1.1 < d <= r:
                    if DY + 13 <= y <= DY + 15:
                        b.set(x, y, z, "glass")
                    else:
                        b.set(x, y, z, "white_terracotta" if (y - DY) // 3 % 2 else "pink_terracotta")
                elif d <= r - 1.1 and y in (DY + 12, DY + 16):
                    b.set(x, y, z, "pink_terracotta")
                elif r < d <= r + 1.1 and y == DY + 12:
                    b.set(x, y, z, "spruce_slab", gb.slab("top"))     # (the gallery around the lamp room)
    for y in (DY + 13, DY + 14):
        b.set(cx, y, cz, "guhs:guh_kristal_lamp")
    for dx in (-2, 2):                                      # the ears
        for y, hw in ((DY + 17, 1), (DY + 18, 1), (DY + 19, 0)):
            for x in range(cx + dx - hw, cx + dx + hw + 1):
                b.set(x, y, cz, "pink_wool" if hw else "magenta_wool")
    g.deur(b, cx, DY + 1, cz - 3, "north")
    b.set(cx, DY + 3, cz - 3, "pink_terracotta")
    # a ladder up the inside to the lamp room
    for y in range(DY + 1, DY + 13):
        b.set(cx, y, cz + 2, "ladder", {"facing": "north", "waterlogged": "false"})
    b.set(cx - 1, DY + 1, cz, "guhs:lampion_geel", gb.lamp(False))
    b.sign(cx + 1, DY + 2, cz - 4, "north", ["Vuurtoren", "Guhoortje", "Schijnt voor", "elke guh"])


def kroeg(b):
    """De Vadsige Walvis: a timber pub under a steep roof, the bar along the south wall, the old captain's corner."""
    x0, z0, x1, z1 = KROEG
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wand = x in (x0, x1) or z in (z0, z1)
            hoek = x in (x0, x1) and z in (z0, z1)
            b.set(x, DY, z, "dark_oak_planks" if (x + z) % 2 else PLANK)
            for y in range(DY + 1, DY + 6):
                if hoek or (wand and (x - x0) % 5 == 0 and z in (z0, z1)) or (wand and (z - z0) % 5 == 0 and x in (x0, x1)):
                    b.set(x, y, z, PAAL, {"axis": "y"})
                elif wand:
                    raam = y in (DY + 2, DY + 3) and ((x - x0) % 5 in (2, 3) and z in (z0, z1) or (z - z0) % 5 in (2, 3) and x in (x0, x1))
                    b.set(x, y, z, "yellow_stained_glass_pane" if raam else ("white_terracotta" if y > DY + 1 else "dark_oak_planks"))
    # the roof: a gable along x, stairs up both slopes, planks in the gable ends
    for k in range(9):
        y = DY + 5 + k
        for x in range(x0 - 1, x1 + 2):
            g.trap(b, x, y, z0 - 1 + k, "dark_oak_stairs", "south")
            g.trap(b, x, y, z1 + 1 - k, "dark_oak_stairs", "north")
        for x in (x0, x1):
            for z in range(z0 + k, z1 + 1 - k):
                if y > DY + 5:
                    b.set(x, y, z, PLANK if (y, z) != (DY + 9, z0 + 7) and (y, z) != (DY + 9, z0 + 8) else "yellow_stained_glass_pane")
    for x in (x0 + 4, x0 + 8, x0 + 12):
        for z in range(z0 + 1, z1):
            b.set(x, DY + 6, z, "stripped_dark_oak_log", {"axis": "z"})
        for z in (z0 + 4, z0 + 11):
            g.hanglamp(b, x, DY + 5, z, ketting=1)
    # the door in the west wall, the sign above it
    g.deur(b, x0, DY + 1, 87, "west")
    b.set(x0, DY + 3, 87, "dark_oak_planks")
    b.sign(x0 - 1, DY + 3, 87, "west", ["De Vadsige", "Walvis", "Kroeg. Altijd", "een plekje vrij"])
    for z in (85, 89):
        b.set(x0 - 1, DY + 1, z, HEK)
        b.set(x0 - 1, DY + 2, z, "guhs:lampion_roze", gb.lamp(False))
    b.zone("kroeg", x0, DY + 1, z0, x1, DY + 12, z1, wie="havenkroeg De Vadsige Walvis")
    # the bar: counter along z 91, shelves on the south wall, stools
    for x in range(83, 91):
        b.set(x, DY + 1, 91, "barrel" if x % 3 else "stripped_dark_oak_log", {"facing": "north", "open": "false"} if x % 3 else {"axis": "x"})
    for x in range(83, 92, 2):
        b.set(x, DY + 1, 89, "guhs:guh_stoel", {"facing": "south"})
    for x in range(82, 93):
        b.set(x, DY + 1, 94, "barrel", {"facing": "north", "open": "false"})
        if x % 2:
            b.set(x, DY + 2, 94, "barrel", {"facing": "north", "open": "false"})
    b.set(84, DY + 2, 91, "cake", {"bites": "1"})
    b.set(88, DY + 2, 91, "guhs:block_of_kaasknabbels")
    b.set(86, DY + 3, 94, "guhs:mikatrofee", {"facing": "north"})
    b.set(87, DY + 3, 94, "barrel", {"facing": "north", "open": "false"})
    b.set(86, DY + 2, 94, "barrel", {"facing": "north", "open": "false"})
    b.set(88, DY + 3, 94, "barrel", {"facing": "north", "open": "false"})
    b.set(88, DY + 2, 94, "barrel", {"facing": "north", "open": "false"})
    b.sign(88, DY + 3, 93, "north", ["Gevangen bij", "de derde pier.", "Hij beet", "zelf. Njeg."])
    b.plek("kroegbaas", 86, DY + 1, 92, yaw=180.0, wie="de kroegbaas-guh, achter de tap", kind="KROEGBAASGUH")
    # tables
    for tx, tz in ((84, 84), (88, 84)):
        b.set(tx, DY + 1, tz, "guhs:guh_tafel", {"facing": "north"})
        b.set(tx - 1, DY + 1, tz, "guhs:guh_stoel", {"facing": "east"})
        b.set(tx + 1, DY + 1, tz, "guhs:guh_stoel", {"facing": "west"})
        b.set(tx, DY + 1, tz + 1, "guhs:guh_stoel", {"facing": "north"})
    b.set(82, DY + 1, 82, "jukebox")
    b.sign(82, DY + 2, 81, "south", ["Verzoekjes:", "alleen het", "walvislied.", "(dat kan hij)"])
    # the old captain's corner (north-east): his bench, his table, the chart, and the stand for the proof
    b.zone("kapitein_hoek", 90, DY + 1, 81, 94, DY + 5, 85, wie="de hoek van de oude kapitein-guh")
    b.set(94, DY + 1, 82, "guhs:guh_bank", {"facing": "west"})
    b.set(94, DY + 1, 83, "guhs:guh_bank", {"facing": "west"})
    b.set(92, DY + 1, 81, "guhs:guh_tafel", {"facing": "south"})
    b.set(94, DY + 1, 81, "cartography_table")
    b.set(94, DY + 1, 84, "barrel", {"facing": "up", "open": "false"})
    b.set(94, DY + 2, 84, "guhs:lampion_geel", gb.lamp(False))
    b.set(90, DY + 1, 81, "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    b.prop("walvis_bewijs", 90, DY + 1, 81, wie="de lessenaar in de kapiteinshoek: hier leg je het bewijs van de walvis neer")
    b.plek("kapitein", 93, DY + 1, 83, yaw=90.0, wie="de oude kapitein-guh, voor zijn bankje", kind="KAPITEINGUH")
    b.sign(93, DY + 3, 81, "south", ["Ik zag hem.", "Zo groot als", "een eiland.", "Hij ZONG."])
    b.sign(91, DY + 3, 81, "south", ["Bewijs?", "Leg het hier.", "Dan gelooft", "de kroeg mij."])
    # a rug in front of the corner
    for x in range(90, 93):
        for z in range(83, 85):
            b.set(x, DY + 1, z, "blue_carpet")
    b.s.blocks.pop((93, DY + 1, 83), None)


def spatel(b):
    """De Spijtige Spatel: an open kitchen under a lean-to on the quay, waiting for its cook."""
    x0, z0, x1, z1 = 89, 62, 95, 68
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, DY, z, "guhs:belegen_kaas_tegels")
            b.set(x, DY + 5, z, "red_wool" if (x - x0) % 2 else "white_wool")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        for y in range(DY + 1, DY + 5):
            b.set(x, y, z, PAAL, {"axis": "y"})
    for x in range(x0 + 1, x1):                              # the back (north): the stove and the shelves
        b.set(x, DY + 1, z0, {0: "smoker", 1: "furnace", 2: "cauldron", 3: "barrel", 4: "crafting_table"}[x - x0 - 1],
              {0: {"facing": "south", "lit": "false"}, 1: {"facing": "south", "lit": "false"}, 3: {"facing": "up", "open": "false"}}.get(x - x0 - 1))
    for z in range(z0 + 1, z1):                              # the counter (west) with two stools in front
        b.set(x0, DY + 1, z, "stripped_spruce_log", {"axis": "z"})
    for z in (z0 + 2, z0 + 4):
        b.set(x0 - 1, DY + 1, z, "guhs:guh_stoel", {"facing": "east"})
    b.set(x0, DY + 2, z0 + 3, "guhs:guh_taart", {"bites": "0"})
    g.hanglamp(b, x0 + 3, DY + 4, z0 + 3, ketting=0, lamp="guhs:lampion_roze")
    b.plek("mika_kok", x0 + 3, DY + 1, z0 + 3, yaw=90.0, wie="de spijtige Mika-kok (later: hij verhuist hierheen uit de vesting)", kind="MIKA_KOK")
    b.sign(x0 - 1, DY + 3, z0, "west", ["De Spijtige", "Spatel", "Kok gezocht.", "Spijt mag."])
    b.sign(x0, DY + 2, z1 + 1, "south", ["Menu:", "nog niks.", "Wel al een", "taart, njeg."])


def aankomst(b):
    x0, z0, x1, z1 = LANDING
    for x in range(x0 + 1, x1):
        for z in range(z0 + 2, z1 - 1):
            rand = x in (x0 + 1, x1 - 1) or z in (z0 + 2, z1 - 2)
            b.set(x, DY, z, "pink_terracotta" if rand else PLANK)
    for x, z in ((72, 104), (71, 104), (73, 104), (72, 103), (72, 105)):
        b.set(x, DY, z, "pink_concrete")
    b.zone("aankomst", x0 + 1, DY + 1, z0 + 2, x1 - 1, DY + 9, z1 - 2, wie="de landingsvlonder van de Leegtehaven")
    b.prop("aankomst_speler", 72, DY + 1, 104)
    b.prop("uitzicht", 72, DY + 22, 150, wie="van het zuiden, hoog: de hele haven met de masten erachter")
    b.sign(67, DY + 1, 99, "east", ["Leegtehaven", "Welkom, guh!", "Niet in de", "leegte spugen"], wall=False)
    # a wind sock
    for k in range(1, 6):
        b.set(x1, DY + 1 + k, z1, HEK)
    for k, kleur in enumerate(("orange_wool", "white_wool", "orange_wool")):
        b.set(x1 - 1 - k, DY + 6, z1, kleur)


def kade_spul(b, vloer):
    """The compass rose in the quay, crates, bollards, benches and lamp posts; mooring posts on the steigers."""
    for dx in range(-6, 7):
        for dz in range(-6, 7):
            x, z, d = 72 + dx, 77 + dz, math.hypot(dx, dz)
            if b.get(x, DY + 1, z) is not None or (x, z) not in vloer:
                continue
            if dx == 0 or dz == 0:
                b.set(x, DY, z, "pink_terracotta" if d > 0.5 else "pink_concrete")
            elif abs(dx) == abs(dz) and d < 5:
                b.set(x, DY, z, "white_terracotta")
            elif 5.5 < d <= 6.5:
                b.set(x, DY, z, "guhs:kaaskorst_stenen")
    for x, z in ((49, 60), (49, 61), (50, 60), (64, 92), (65, 92), (64, 93), (78, 66), (62, 78), (62, 79)):
        if b.get(x, DY + 1, z) is None:
            g.ton(b, x, DY + 1, z)
    b.set(49, DY + 2, 60, "barrel", {"facing": "up", "open": "false"})
    b.set(64, DY + 2, 92, "honey_block")
    for x, z, f in ((58, 72, "east"), (58, 76, "east"), (70, 88, "north"), (74, 88, "north")):
        b.set(x, DY + 1, z, "guhs:guh_bank", {"facing": f})
    for x, z in ((52, 68), (66, 74), (78, 74), (66, 84), (78, 84), (72, 92), (92, 58), (52, 58), (60, 94)):
        if b.get(x, DY + 1, z) is None:
            g.lantaarnpaal(b, x, DY + 1, z, hoogte=3, hek="dark_oak_fence")
    for nr, (sx0, sz0, sx1, sz1) in STEIGERS.items():
        for z in range(sz0 + 4, sz1, 8):
            b.set(sx0 + 2, DY + 1, z, "barrel", {"facing": "up", "open": "false"} if (z // 8) % 2 else None)
            if not (z // 8) % 2:
                b.set(sx0 + 2, DY + 1, z, "dark_oak_log", {"axis": "y"})
                b.set(sx0 + 2, DY + 2, z, "guhs:lampion_roze", gb.lamp(False))
        b.sign(sx0 + 2, DY + 1, sz1 - 1, "south", [f"Steiger {nr}", "Alleen voor", "vetschepen", "en nette guhs"], wall=False)


def _bouw(h):
    b = kit.Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=g.PREFIX)
    _grond, vloer = eiland(b)
    dekken(b, vloer)
    schepen(b)
    markt(b)
    veer(b)
    werf(b)
    vuurtoren(b)
    kroeg(b)
    spatel(b)
    aankomst(b)
    kade_spul(b, vloer)
    b.connect()
    start = (72, DY + 1, 104)
    n, over = g.verlicht(b, start, [g.in_vloer("guhs:guh_kristal_lamp"), g.op_vloer("lantern", gb.lamp(False)), g.in_wand("guhs:guh_kristal_lamp")])
    b.laatste_licht = n
    return b, start, over


VERWACHT = ["steiger_1", "steiger_2", "vetschip_1", "vetschip_2", "markt", ("handelaar", 4), "prikbord", "veerpier", "veerman", "veer_dek",
            "werf", "helling", "scheepsbouwer", "kroeg", "kroegbaas", "kapitein", "kapitein_hoek", "walvis_bewijs", "mika_kok"]


def eigen_check(b):
    """What the contract asks of this place beyond the kit's check: the helling free (9 wide, 7 high, 13 long), the ferry
    deck at least 7 x 9 and flat, every trader an Ender-Mika with Handelaar behind a counter."""
    p = []
    x0, z0, x1, z1 = HELLING
    vol = [(x, y, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) for y in range(DY + 1, DY + 8) if b.get(x, y, z) not in (None, gb.AIR)]
    if vol or (x1 - x0 + 1, z1 - z0 + 1) != (13, 9):
        p.append(f"de helling is niet 13 x 9 x 7 vrij: {vol[:4]}")
    d = b.plekken["veer_dek"]
    if d["tot"][0] - d["pos"][0] + 1 < 9 or d["tot"][2] - d["pos"][2] + 1 < 7:
        p.append(f"veer_dek is kleiner dan 9 x 7: {d}")
    vrij = sum(1 for x in range(d["pos"][0], d["tot"][0] + 1) for z in range(d["pos"][2], d["tot"][2] + 1)
               if b.get(x, DY, z) is not None and b.get(x, DY + 1, z) is None and b.get(x, DY + 2, z) is None)
    if vrij < 63:
        p.append(f"veer_dek: maar {vrij} vrije plekken om te staan")
    handelaars = [e for e in b.s.entities if e[3].get("id") == "guhs:ender_mika"]
    if len(handelaars) != 4 or any(int(e[3].get("Handelaar", 0)) != 1 for e in handelaars):
        p.append(f"{len(handelaars)} Ender-Mika's, niet vier handelaars")
    for (x, y, z, _nbt) in handelaars:
        if b.get(int(x), DY + 1, int(z) + 2) is None or any(b.get(int(x), DY + k, int(z)) is not None for k in (1, 2, 3)):
            p.append(f"handelaar op {(x, y, z)}: geen toonbank voor zich, of geen drie blokken vrij")
    return p


def maak(h):
    b, start, over = _bouw(h)
    schip = lambda c: False                                   # noqa: E731  (nothing may float here: even the ships hang on their gangways)
    problemen = b.check(start, VERWACHT, zwevend_ok=schip)
    problemen += [f"{len(over)} donkere plekken kregen geen licht, bv. {over[:4]}"] if over else []
    problemen += eigen_check(b)
    return b, problemen


def bouw(h):
    b, problemen = maak(h)
    if problemen:
        raise SystemExit(f"{NAAM}: self-check found {len(problemen)} problem(s):\n  " + "\n  ".join(problemen[:40]))
    print(f"{NAAM}: self-check ok {b.stats}, {b.laatste_licht} lights added")
    return b
