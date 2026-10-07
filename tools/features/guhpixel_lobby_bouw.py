"""
The lobby island of Guhpixel: template guhs:guhpixel/lobby (Java: feature/guhpixel/Lobby, LobbyPlek; the things that live
on it: feature/guhpixel/lobby).

A floating cheese island with a pink plaza: the big GUHPIXEL logo on an arch in the north, in front of it the row of game
stalls (one per anchor, the NPCs themselves are placed by LobbyNpcs), the over-the-top shop in the west (the Verkoper-guh
stands in its doorway), the AFK-hoek in the east, the two boards and the door home in the south. The lobby parkour starts
at the east side, climbs the AFK-hoek, hops over floating cushions and the stall roofs and ends on the shop roof. Ten
golden knabbels are hidden all over (KNABBELS).

Fixed geometry (CONTRACT_PX 2 and 2.1; controleer() checks it):
  - the size exactly 97 x (at most 96) x 97; the template is stamped with its min corner at world (-48, 68, -48), so
    local (48, 32, 48) = world (0, 100, 0) = the spawn point (feet) and the plaza floor is local y 31 (world y 99);
  - every anchor of ANKERS: a 5 x 5 pad (anchor +-2 in x and z) with a solid floor at world y 99 and 4 free blocks above,
    and NO NPC of an anchor in the template (LobbyNpcs places them);
  - the exit portal: guhs:guhpixel_portaal[soort=uit] at world x -1..1, y 100..102, z 30 (here 5 wide and 4 high);
  - no free-flowing water or lava (they would flow forever in the void).
Bump VERSIE whenever the template changes: a server that already has a lobby then clears the box and stamps it again.
Everything below works in WORLD coordinates (Bouw.zet); nothing is random without a fixed seed.
"""
import math
import os
import random
import re

VERSIE = 3
NAME = "guhpixel/lobby"
W, H = 97, 56                    # (H may grow to 96)
MIN = (-48, 68, -48)             # world position of the template's min corner
VLOER = 99                       # world y of the plaza floor
STRAAL = 44

# name: (x, y, z, yaw) in world coordinates; a copy of LobbyPlek.java (checked by controleer)
ANKERS = {
    "SPAWN": (0, 100, 0, 180), "WELKOM": (3, 100, -5, 0), "SPEL_SKYBLOK": (-24, 100, -16, 0), "SPEL_BEDWARS": (-16, 100, -21, 0),
    "SPEL_VADSNITE": (-8, 100, -24, 0), "SPEL_AMONG": (0, 100, -26, 0), "SPEL_GUHMON": (8, 100, -24, 0), "SPEL_BZG": (16, 100, -21, 0),
    "SPEL_RESERVE": (24, 100, -16, 0), "BORD_AMONG": (4, 100, -29, 0), "WINKEL": (-28, 100, 6, -90), "BORD_STATS": (-10, 100, 14, 180),
    "BORD_ONLINE": (10, 100, 14, 180), "PARKOUR_START": (28, 100, 6, -90), "UITGANG": (0, 100, 28, 0),
}
PORTAAL = [(x, y, 30) for x in (-1, 0, 1) for y in (100, 101, 102)]
LUCHT = (None, "minecraft:air")

# --- what the Java side mirrors (feature/guhpixel/lobby; the game test PxLobbyGameTests reads the template) ----------------
# the ten golden knabbels: number -> world position of the block guhs:lobby_gouden_knabbel[nummer=n]
KNABBELS = {
    0: (-10, 93, 20),     # the little cellar under the plaza, down the ladder behind the stats board
    1: (0, 100, 34),      # behind the door home
    2: (19, 100, -40),    # behind the right leg of the logo arch
    3: (26, 106, -19),    # on the roof of the "binnenkort" stall (parkour)
    4: (-43, 100, -5),    # in the shop, behind the shelves in the far corner
    5: (-43, 108, 17),    # the far corner of the shop roof (parkour)
    6: (40, 100, 11),     # in the AFK-hoek, behind the pile of cushions
    7: (-25, 100, -20),   # behind the Skyblok stall
    8: (43, 100, 5),      # the narrow ledge behind the AFK-hoek
    9: (9, 100, 10),      # in plain sight: between the flowers of a planter
}
# where the chat guhs sit (LobbySlice keeps one LOBBY_CHATGUH on each): (x, y, z, yaw)
CHATGUHS = [(12, 100, 3, 90), (-13, 100, -7, -60), (6, 100, 21, 180), (-17, 100, 23, -140), (37, 100, 3, 90), (37, 100, 8, 90)]
# the lobby parkour: the start block lies in the floor under PARKOUR_START, the finish block in the shop roof
PARKOUR_START = (28, 99, 6)
PARKOUR_FINISH = (-35, 107, 15)
PARKOUR_TUSSEN = [(24, 105, -18), (0, 105, -31), (-24, 105, -18)]      # checkpoints: the roofs of three stalls

STALLEN = {   # anchor -> (wool colour, concrete colour)
    "SPEL_SKYBLOK": ("light_blue", "light_blue"), "SPEL_BEDWARS": ("red", "red"), "SPEL_VADSNITE": ("purple", "purple"),
    "SPEL_AMONG": ("cyan", "cyan"), "SPEL_GUHMON": ("yellow", "orange"), "SPEL_BZG": ("lime", "green"),
    "SPEL_RESERVE": ("light_gray", "gray"),
}

# 6 x 8 letters (the logo and the SALE sign on the shop roof)
LETTERS = {
    "G": [".####.", "##..##", "##....", "##....", "##.###", "##..##", "##..##", ".####."],
    "U": ["##..##", "##..##", "##..##", "##..##", "##..##", "##..##", "##..##", ".####."],
    "H": ["##..##", "##..##", "##..##", "######", "######", "##..##", "##..##", "##..##"],
    "P": ["#####.", "##..##", "##..##", "##..##", "#####.", "##....", "##....", "##...."],
    "I": ["######", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..", "######"],
    "X": ["##..##", "##..##", ".####.", "..##..", "..##..", ".####.", "##..##", "##..##"],
    "E": ["######", "##....", "##....", "#####.", "##....", "##....", "##....", "######"],
    "L": ["##....", "##....", "##....", "##....", "##....", "##....", "##....", "######"],
    "S": [".#####", "##....", "##....", ".####.", "....##", "....##", "....##", "#####."],
    "A": [".####.", "##..##", "##..##", "######", "##..##", "##..##", "##..##", "##..##"],
}


def lokaal(x, y, z):
    """World coordinates -> template coordinates."""
    return x - MIN[0], y - MIN[1], z - MIN[2]


def in_pad(x, y, z):
    """Inside the free space of an anchor pad (5 x 5, 4 high)?"""
    for (ax, ay, az, _) in ANKERS.values():
        if abs(x - ax) <= 2 and abs(z - az) <= 2 and ay <= y < ay + 4:
            return True
    return False


class Bouw:
    """A Structure addressed in world coordinates. Nothing is ever put in the free space of an anchor pad (except the portal)."""

    def __init__(self, s):
        self.s = s

    def zet(self, x, y, z, blok, props=None, nbt=None):
        if in_pad(x, y, z) and blok != "guhs:guhpixel_portaal":
            return
        if not blok.count(":"):
            blok = "minecraft:" + blok
        self.s.set(*lokaal(x, y, z), blok, props, nbt)

    def weg(self, x, y, z):
        self.s.blocks.pop(lokaal(x, y, z), None)

    def get(self, x, y, z):
        return self.s.get(*lokaal(x, y, z))

    def vul(self, x0, y0, z0, x1, y1, z1, blok, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.zet(x, y, z, blok, props)

    def leeg(self, x0, y0, z0, x1, y1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.weg(x, y, z)


def bord_nbt(h, regels, kleur="black", gloei=False):
    """A sign with translatable lines (tools/sign_text.py, prefix sign.guhs.lobby)."""
    import sign_text
    B = h.ms.Byte
    leeg = sign_text.messages("sign.guhs.lobby", ["", "", "", ""])
    return {"id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages("sign.guhs.lobby", regels), "color": kleur, "has_glowing_text": B(1 if gloei else 0)},
            "back_text": {"messages": leeg, "color": kleur, "has_glowing_text": B(0)}}


def staand_bord(h, b, x, y, z, rotatie, regels, hout="birch", **kw):
    """rotatie: 0 = the text faces south, 4 = west, 8 = north, 12 = east."""
    b.zet(x, y, z, f"{hout}_sign", {"rotation": str(rotatie), "waterlogged": "false"}, bord_nbt(h, regels, **kw))


def muur_bord(h, b, x, y, z, facing, regels, hout="birch", **kw):
    b.zet(x, y, z, f"{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, bord_nbt(h, regels, **kw))


# =====================================================================================================================
# the island
# =====================================================================================================================
def _diepte(x, z, rng_tabel):
    d = math.hypot(x, z)
    if d > STRAAL + 0.5:
        return None
    t = max(0.0, 1.0 - d / (STRAAL + 0.5))
    return int(3 + 25 * t ** 0.8 + rng_tabel[(x, z)])


def eiland(b):
    """The plaza floor (pink with white rings and cheese-yellow roads) on a hollow shell of cheese that tapers to a point."""
    rng = random.Random(260501)
    ruis = {(x, z): rng.choice((0, 0, 0, 1, 1, 2)) for x in range(-48, 49) for z in range(-48, 49)}
    bodem = {}
    for x in range(-48, 49):
        for z in range(-48, 49):
            diep = _diepte(x, z, ruis)
            if diep is not None:
                bodem[(x, z)] = VLOER - diep
    kaas = random.Random(260502)
    for (x, z), onder in bodem.items():
        d = math.hypot(x, z)
        # the floor
        ring = int(d) % 8 == 7
        weg = (abs(x) <= 1 and 3 <= z <= 30) or (abs(z - 6) <= 1 and 3 <= abs(x) <= 26) or (abs(x) <= 1 and -24 <= z <= -3)
        if weg:
            vloer = "yellow_concrete" if (x + z) % 2 else "yellow_terracotta"
        elif ring:
            vloer = "white_concrete"
        elif d < 6.5:
            vloer = "magenta_concrete"
        else:
            vloer = "pink_concrete" if (int(d) // 8) % 2 == 0 else "pink_wool"
        b.zet(x, VLOER, z, vloer)
        # the shell underneath: only what can be seen (the two lowest blocks of a column and every exposed side)
        buren = [bodem.get((x + dx, z + dz), VLOER + 1) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        for y in range(onder, VLOER):
            if y - onder > 1 and y < VLOER - 1 and all(n <= y for n in buren):
                continue
            diep = VLOER - y
            if diep <= 1:
                blok = "yellow_terracotta"
            elif y - onder <= 0 and diep > 8:
                blok = "pink_concrete"                      # the drips at the very bottom
            else:
                r = kaas.random()
                blok = "guhs:gatenkaas" if r < 0.55 else "guhs:belegen_kaas_stenen" if r < 0.8 else "yellow_concrete"
            b.zet(x, y, z, blok)
        if d > STRAAL - 0.5:
            b.zet(x, VLOER + 1, z, "pink_stained_glass")    # a low rim, so nobody rolls off
    return bodem


def pads(b):
    """The anchor pads: a cheese-coloured 5 x 5 with a lit middle; magenta under the spawn point."""
    for naam, (ax, ay, az, _) in ANKERS.items():
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                rand = max(abs(dx), abs(dz)) == 2
                if naam == "SPAWN":
                    blok = "magenta_glazed_terracotta" if rand else "magenta_concrete"
                elif naam.startswith("BORD"):
                    blok = "white_concrete" if rand else "light_gray_concrete"
                else:
                    blok = "yellow_concrete" if rand else "yellow_terracotta"
                b.s.set(*lokaal(ax + dx, VLOER, az + dz), "minecraft:" + blok)
        b.s.set(*lokaal(ax, VLOER, az), "minecraft:sea_lantern")
    b.s.set(*lokaal(*PARKOUR_START), "guhs:lobby_parkour_start")


# =====================================================================================================================
# letters
# =====================================================================================================================
def tekst(b, woord, x, y_top, z, stap, blok, tussen=1):
    """Block letters, read from the side where `stap` points to the reader's right: stap = (dx, dz) per column.
    (x, y_top, z) is the top left corner of the first letter. blok: one name or a function(letter index) -> name."""
    kolom = 0
    for i, letter in enumerate(woord):
        rijen = LETTERS[letter]
        for r, rij in enumerate(rijen):
            for c, ch in enumerate(rij):
                if ch == "#":
                    naam = blok(i) if callable(blok) else blok
                    b.zet(x + stap[0] * (kolom + c), y_top - r, z + stap[1] * (kolom + c), naam)
        kolom += len(rijen[0]) + tussen
    return kolom - tussen


def logo(b):
    """The GUHPIXEL logo: 55 blocks wide on an arch across the north of the plaza, GUH in pink and PIXEL in gold."""
    z = -38
    for x in (-19, -18, 18, 19):
        for zz in (z, z + 1):
            for y in range(VLOER + 1, 107):
                b.zet(x, y, zz, "white_concrete" if (y // 2) % 2 else "pink_concrete")
    for x in range(-29, 30):                                   # the beam
        for zz in (z, z + 1):
            b.zet(x, 106, zz, "magenta_concrete")
            b.zet(x, 107, zz, "white_concrete")
    for x in range(-29, 30):                                   # the back plate with a pink border and lights in it
        for y in range(108, 118):
            rand = x in (-29, 29) or y == 117
            b.zet(x, y, z, "magenta_concrete" if rand else "white_concrete")
    for x in range(-26, 27, 4):
        b.zet(x, 107, z + 1, "sea_lantern")
    kleuren = ["pink_concrete"] * 3 + ["gold_block"] * 5
    breed = tekst(b, "GUHPIXEL", -27, 116, z + 1, (1, 0), lambda i: kleuren[i])
    assert breed == 55, breed
    # two golden kaasknabbels on top, and lanterns hanging from the beam
    for x in (-24, 24):
        b.vul(x - 1, 118, z, x + 1, 119, z + 1, "guhs:block_of_kaasknabbels")
        b.zet(x, 120, z, "guhs:block_of_kaasknabbels")
    for x in range(-14, 15, 7):
        b.zet(x, 105, z + 1, "chain", {"axis": "y", "waterlogged": "false"})
        b.zet(x, 104, z + 1, "guhs:lampion_roze", {"hanging": "true"})


# =====================================================================================================================
# the game stalls
# =====================================================================================================================
def stal_z(naam):
    """The z of a stall's back wall: 3 behind the anchor; the middle stall (Among Guhs, with its board beside it) stands 6
    behind it, right under the logo. (The Guhmon stall misses one corner post: the pad of the Among board is there.)"""
    return ANKERS[naam][2] - (6 if naam == "SPEL_AMONG" else 3)


def stallen(h, b):
    """Behind every game NPC a little market stall: a back wall in the game's colour and a striped awning (the parkour
    runs over these roofs). The seventh stall has no game: it is the nap corner (sign "DUTJESHOEK")."""
    for naam, (wol, beton) in STALLEN.items():
        ax, ay, az, _ = ANKERS[naam]
        zb = stal_z(naam)
        for x in range(ax - 2, ax + 3):
            for y in range(100, 105):
                rand = abs(x - ax) == 2 or y == 104
                b.zet(x, y, zb, f"{beton}_concrete" if rand else f"{wol}_wool" if (x + y) % 2 else "white_wool")
            for z in range(zb, zb + 3):                         # the awning (y 105: above the pad's free space)
                b.zet(x, 105, z, f"{wol}_wool" if (x - ax) % 2 == 0 else "white_wool")
        b.zet(ax, 103, zb, "sea_lantern")
        if naam == "SPEL_RESERVE":
            for x in range(ax - 2, ax + 3):
                b.zet(x, 100, zb, "yellow_concrete" if x % 2 else "black_concrete")
            # (no game belongs here and none is promised: it is the nap corner, not a "coming soon" stall)
            staand_bord(h, b, ax, 100, az + 3, 0, ["~ Kraampje 7 ~", "DUTJESHOEK", "Geen spel.", "Wel een kussen."], gloei=True)
            staand_bord(h, b, ax - 3, 100, az, 0, ["Dit kraampje", "is voor dutjes.", "Meer niet.", "Echt niet."])
            b.zet(ax + 3, 100, az, "pink_wool")
            b.zet(ax + 3, 101, az, "pink_carpet")


# =====================================================================================================================
# the shop
# =====================================================================================================================
WINKEL = (-44, -25, -6, 18)      # x0, x1, z0, z1 (CONTRACT_PX 2.1)
WINKEL_DAK = 107


def winkel(h, b, bodem):
    x0, x1, z0, z1 = WINKEL
    # ground under the whole building (its far corners hang over the round plaza)
    for x in range(x0 - 1, x1 + 1):
        for z in range(z0 - 1, z1 + 2):
            if (x, z) not in bodem:
                b.zet(x, VLOER, z, "white_concrete")
                b.zet(x, VLOER - 1, z, "yellow_terracotta")
                b.zet(x, VLOER - 2, z, "guhs:gatenkaas")
            if x0 < x < x1 and z0 < z < z1:
                b.s.set(*lokaal(x, VLOER, z), "minecraft:pink_concrete" if (x + z) % 2 else "minecraft:white_concrete")
    pads(b)                                                    # (the shop floor went over the Verkoper's pad)
    b.leeg(x0 + 1, 100, z0 + 1, x1 - 1, WINKEL_DAK - 1, z1 - 1)    # (no plaza rim inside)
    # walls with a magenta band; the front (east) is one big shop window with the doorway in the middle
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                for y in range(100, WINKEL_DAK):
                    b.zet(x, y, z, "magenta_concrete" if y in (100, 106) else "white_concrete")
            b.zet(x, WINKEL_DAK, z, "yellow_concrete" if (z // 2) % 2 else "white_concrete")
    for z in range(1, 12):                                     # the doorway (the Verkoper's pad is z 4..8)
        for y in range(100, 105):
            b.weg(x1, y, z)
    for z in list(range(-4, 0)) + list(range(13, 17)):         # the shop windows left and right of it
        for y in range(101, 105):
            b.zet(x1, y, z, "pink_stained_glass")
    for z in range(z0 + 2, z1 - 1, 4):                         # side and back windows
        for y in (102, 103):
            b.zet(x0, y, z, "pink_stained_glass")
    for x in range(x0 + 3, x1 - 2, 4):
        for y in (102, 103):
            b.zet(x, y, z0, "pink_stained_glass")
            b.zet(x, y, z1, "pink_stained_glass")
    # a red and white awning over the whole front, and SALE in huge red letters on the roof edge
    for z in range(z0, z1 + 1):
        b.zet(x1 + 1, 105, z, "red_wool" if z % 2 else "white_wool")
        b.zet(x1 + 2, 105, z, "red_wool" if z % 2 else "white_wool")
    breed = tekst(b, "SALE", x1, WINKEL_DAK + 9, z1 + 1, (0, -1), "red_concrete")
    assert breed == 27, breed
    b.vul(x1, WINKEL_DAK + 1, z0 - 1, x1, WINKEL_DAK + 1, z1 + 1, "white_concrete")
    # inside: lights, shelves full of cushions and cheese along the walls, two counters beside the Verkoper
    for x in range(x0 + 3, x1 - 1, 5):
        for z in range(z0 + 3, z1 - 1, 5):
            b.zet(x, WINKEL_DAK - 1, z, "sea_lantern")
    kleur = ["pink", "magenta", "yellow", "light_blue", "lime", "orange", "purple", "white"]
    n = 0
    for z in range(z0 + 1, z1):
        if z in (z0 + 1, z0 + 2):
            continue                                           # (the corner with the golden knabbel stays open behind the shelves)
        for y in (100, 101, 102):
            n += 1
            b.zet(x0 + 1, y, z, "guhs:block_of_kaasknabbels" if n % 5 == 0 else f"{kleur[n % 8]}_wool")
    for x in range(x0 + 2, x1 - 3):
        for zz in (z0 + 1, z1 - 1):
            for y in (100, 101):
                n += 1
                if zz == z0 + 1 and x < x0 + 5:
                    continue
                b.zet(x, y, zz, f"{kleur[n % 8]}_wool" if n % 3 else "bookshelf")
    b.zet(x0 + 3, 100, z0 + 2, "barrel", {"facing": "up", "open": "false"})          # the gap to the knabbel is 1 wide
    b.zet(x0 + 3, 101, z0 + 2, "barrel", {"facing": "up", "open": "false"})
    for (x, z) in ((-34, 0), (-34, 12), (-39, 6)):             # display tables in the middle
        b.vul(x - 1, 100, z - 1, x + 1, 100, z + 1, "smooth_quartz")
        b.zet(x, 101, z, "guhs:block_of_kaasknabbels")
        b.zet(x - 1, 101, z, "cake", {"bites": "0"})
        b.zet(x + 1, 101, z + 1, "guhs:pink_kussen", {"facing": "east"})
        b.zet(x, 101, z - 1, "guhs:magenta_kussen", {"facing": "east"})
    for z in (2, 3, 9, 10):                                    # the counters
        b.zet(-27, 100, z, "smooth_quartz")
        b.zet(-28, 100, z, "smooth_quartz")
    b.zet(-27, 101, 2, "guhs:lampion_roze", {"hanging": "false"})
    b.zet(-27, 101, 10, "guhs:lampion_roze", {"hanging": "false"})
    # the jokes
    muur_bord(h, b, x1 + 1, 102, 0, "east", ["UITVERKOOP!", "Nu 0% korting!", "Alleen vandaag", "(en morgen)"], gloei=True)
    muur_bord(h, b, x1 + 1, 102, 12, "east", ["Alleen", "cosmetisch,", "echt waar,", "njeg"], gloei=True)
    muur_bord(h, b, x1 + 1, 101, 0, "east", ["Niet goed?", "Muntjes weg.", "Zo werkt", "een winkel."])
    muur_bord(h, b, x1 + 1, 101, 12, "east", ["* EULA-proof", "volgens de", "Verkoper-guh", "zelf"])
    staand_bord(h, b, -22, 100, 1, 12, ["2 halen =", "2 betalen!", "Op = op", "(nooit op)"])
    staand_bord(h, b, -22, 100, 11, 12, ["Pay-to-vads?", "Nee hoor.", "Vadsen is", "altijd gratis"])
    muur_bord(h, b, x0 + 1, 103, 6, "east", ["Algemene", "voorwaarden:", "niet gelezen,", "wel akkoord"])
    muur_bord(h, b, -34, 101, -5, "south", ["Kussen", "was: 5 muntjes", "nu: 5 muntjes!", "WAUW"])
    muur_bord(h, b, -34, 101, 17, "north", ["Loot box?", "Nee: een doos.", "Er zit niks", "in. Eerlijk!"])
    b.zet(*KNABBELS[4], "guhs:lobby_gouden_knabbel", {"nummer": "4"})
    b.zet(*KNABBELS[5], "guhs:lobby_gouden_knabbel", {"nummer": "5"})
    b.zet(*PARKOUR_FINISH, "guhs:lobby_parkour_finish")
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, 1), (-1, 1), (1, -1)):
        b.zet(PARKOUR_FINISH[0] + dx, WINKEL_DAK, PARKOUR_FINISH[2] + dz, "black_concrete" if (dx + dz) % 2 else "white_concrete")


# =====================================================================================================================
# the AFK-hoek (east) and the start of the parkour
# =====================================================================================================================
HAL = (33, 41, -2, 12)
HAL_DAK = 104


def afk_hoek(h, b):
    """A lounge full of beds and cushions where the lobby guhs are "afk (slaap)". Its flat roof is part of the parkour."""
    x0, x1, z0, z1 = HAL
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.zet(x, VLOER, z, "purple_concrete" if (x + z) % 2 else "magenta_concrete")
            if x in (x0, x1) or z in (z0, z1):
                for y in range(100, HAL_DAK):
                    b.zet(x, y, z, "purple_concrete" if y == 100 else "light_blue_concrete")
            b.zet(x, HAL_DAK, z, "light_blue_wool" if (x + z) % 2 else "white_wool")
    for z in range(1, 10):                                     # the open front (west), with a window strip above
        for y in range(100, 103):
            b.weg(x0, y, z)
    for z in range(z0 + 2, z1 - 1, 3):
        b.zet(x1, 101, z, "light_blue_stained_glass")
        b.zet(x1, 102, z, "light_blue_stained_glass")
    for (x, z) in ((36, 1), (36, 6), (36, 10)):
        b.zet(x, HAL_DAK - 1, z, "guhs:lampion_roze", {"hanging": "true"})
    # beds along the back wall (nobody can use them: the lobby guhs got there first), cushions everywhere
    for i, z in enumerate((0, 2, 4, 6)):
        kl = ("pink", "light_blue", "yellow", "lime")[i]
        b.zet(40, 100, z, f"{kl}_bed", {"facing": "east", "part": "head", "occupied": "false"})
        b.zet(39, 100, z, f"{kl}_bed", {"facing": "east", "part": "foot", "occupied": "false"})
    for (x, z, kl, f) in ((35, 0, "pink", "south"), (35, 11, "magenta", "north"), (38, 11, "light_blue", "north"), (34, 5, "lime", "east")):
        b.zet(x, 100, z, f"guhs:{kl}_kussen", {"facing": f})
    # the pile of cushions in the far corner, with a golden knabbel behind it
    for (x, y, z) in ((39, 100, 10), (40, 100, 9), (39, 100, 11), (40, 101, 9)):
        b.zet(x, y, z, "pink_wool" if (x + y + z) % 2 else "magenta_wool")
    b.zet(*KNABBELS[6], "guhs:lobby_gouden_knabbel", {"nummer": "6"})
    muur_bord(h, b, x0 - 1, 102, 0, "west", ["~ AFK-hoek ~", "Stil zijn!", "Er wordt hier", "hard ge-afk't"], gloei=True)
    muur_bord(h, b, x0 - 1, 102, 10, "west", ["brb", "(over een", "uurtje of 12,", "njeg)"])
    # the narrow ledge behind the building
    b.zet(*KNABBELS[8], "guhs:lobby_gouden_knabbel", {"nummer": "8"})


# the parkour: every stone the player lands on, in order: (x, y, z) of the block (they stand on y + 1), or a named roof
def parkour_route():
    """The whole course as a list of landing areas: each a list of (x, y, z) top blocks."""
    def dak(x0, x1, z0, z1, y):
        return [(x, y, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]

    def stal(naam):
        ax, ay, az, _ = ANKERS[naam]
        zb = stal_z(naam)
        return dak(ax - 2, ax + 2, zb, zb + 2, 105)

    route = [[PARKOUR_START, (30, 99, 7)],
             [(31, 100, 7)], [(32, 101, 9)], [(32, 102, 11)], [(32, 103, 13)],
             dak(HAL[0], HAL[1], HAL[2], HAL[3], HAL_DAK),
             [(32, 104, -5)], [(31, 105, -7)], [(30, 105, -10)], [(28, 105, -13)], [(27, 105, -15)],
             stal("SPEL_RESERVE"), [(20, 105, -21)],
             stal("SPEL_BZG"), [(12, 105, -25)],
             stal("SPEL_GUHMON"), [(4, 105, -29)],
             stal("SPEL_AMONG"), [(-4, 105, -29)],
             stal("SPEL_VADSNITE"), [(-12, 105, -25)],
             stal("SPEL_BEDWARS"), [(-20, 105, -21)],
             stal("SPEL_SKYBLOK"),
             [(-28, 105, -14)], [(-29, 106, -12)], [(-30, 106, -9)], [(-31, 107, -7)],
             dak(WINKEL[0], WINKEL[1] - 1, WINKEL[2], WINKEL[3], WINKEL_DAK)]
    return route


def parkour(h, b):
    """The loose stones of the course: floating cushions (wool) in rainbow order, and the checkpoints in three stall roofs."""
    kleuren = ["red", "orange", "yellow", "lime", "light_blue", "blue", "purple", "magenta"]
    n = 0
    for deel in parkour_route():
        if len(deel) == 1:
            b.zet(*deel[0], f"{kleuren[n % 8]}_wool")
            n += 1
    for pos in PARKOUR_TUSSEN:
        b.zet(*pos, "guhs:lobby_parkour_tussenpunt")
    staand_bord(h, b, 31, 100, 4, 4, ["LOBBY-PARKOUR", "Over de daken", "naar de winkel.", "Niet vallen!"], gloei=True)
    b.zet(*KNABBELS[3], "guhs:lobby_gouden_knabbel", {"nummer": "3"})


# =====================================================================================================================
# the south: the door home and the two boards
# =====================================================================================================================
def uitgang(h, b):
    """ "Terug naar huis": the front of a cosy little house; its front door is the exit portal."""
    for x in range(-2, 3):
        for y in range(100, 104):
            b.zet(x, y, 30, "guhs:guhpixel_portaal", {"soort": "uit"})
    for x in range(-6, 7):
        for y in range(100, 105):
            b.zet(x, y, 31, "bricks")
    for y in range(100, 105):
        for x in (-3, 3):
            b.zet(x, y, 30, "stripped_spruce_log", {"axis": "y"})
    for x in range(-3, 4):
        b.zet(x, 104, 30, "stripped_spruce_log", {"axis": "x"})
    for x in (-5, 5):                                          # windows with flower boxes
        b.zet(x, 102, 31, "glass")
        b.zet(x, 101, 30, "spruce_trapdoor", {"facing": "north", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
        b.zet(x, 102, 30, "potted_pink_tulip")
    for i in range(0, 8):                                      # a gable roof
        y = 105 + i
        for x in range(-7 + i, 8 - i):
            for z in (30, 31, 32):
                rand = x in (-7 + i, 7 - i)
                if z == 31 and not rand:
                    b.zet(x, y, z, "bricks")
                elif rand:
                    b.zet(x, y, z, "red_wool")
    b.zet(0, 107, 30, "sea_lantern")
    b.zet(0, 106, 30, "red_wool")
    b.vul(-6, 100, 32, 6, 100, 32, "bricks")                   # the back: a plain wall, and the knabbel behind it
    b.zet(*KNABBELS[1], "guhs:lobby_gouden_knabbel", {"nummer": "1"})
    staand_bord(h, b, -4, 100, 28, 8, ["Terug naar", "huis", "Tot gauw,", "vadsje!"], gloei=True)
    staand_bord(h, b, 4, 100, 28, 8, ["Of typ /lobby", "(of /hub, /l)", "Dat is minder", "lopen, njeg"])


def borden(h, b):
    """The frames of the two boards (the texts float in front of them: LobbyBorden), and the cellar behind the stats board."""
    for naam in ("BORD_STATS", "BORD_ONLINE"):
        ax, ay, az, _ = ANKERS[naam]
        for x in range(ax - 3, ax + 4):
            for y in range(100, 106):
                rand = abs(x - ax) == 3 or y in (100, 105)
                b.zet(x, y, az + 3, "yellow_concrete" if rand else "black_concrete")
        b.zet(ax - 3, 106, az + 3, "guhs:block_of_kaasknabbels")
        b.zet(ax + 3, 106, az + 3, "guhs:block_of_kaasknabbels")
    # the cellar: a shaft with a ladder behind the stats board, a tiny room with a lantern and a golden knabbel
    kx, ky, kz = KNABBELS[0]
    for x in range(kx - 2, kx + 3):
        for y in range(ky - 1, ky + 4):
            for z in range(kz - 2, kz + 3):
                binnen = abs(x - kx) <= 1 and abs(z - kz) <= 1 and ky <= y <= ky + 2
                if binnen:
                    b.weg(x, y, z)
                else:
                    b.zet(x, y, z, "guhs:belegen_kaas_stenen")
    for y in range(ky + 3, VLOER + 1):                         # the shaft (ladder on its north side)
        for (x, z) in ((kx - 1, kz - 1), (kx, kz - 2), (kx + 1, kz - 1), (kx, kz)):
            if b.get(x, y, z) in LUCHT:
                b.zet(x, y, z, "guhs:belegen_kaas_stenen")
        b.weg(kx, y, kz - 1)
    for y in range(ky, VLOER + 1):
        b.zet(kx, y, kz - 1, "ladder", {"facing": "south", "waterlogged": "false"})
        if b.get(kx, y, kz - 2) in LUCHT:
            b.zet(kx, y, kz - 2, "guhs:belegen_kaas_stenen")
    b.zet(kx + 1, ky, kz + 1, "lantern", {"hanging": "false", "waterlogged": "false"})
    b.zet(kx, ky, kz, "guhs:lobby_gouden_knabbel", {"nummer": "0"})
    muur_bord(h, b, kx - 1, ky + 1, kz + 1, "east", ["Geheime", "kaaskelder", "(niet verder", "vertellen)"])


# =====================================================================================================================
# decoration
# =====================================================================================================================
def versiering(h, b):
    # four lamp posts with planters around the spawn point
    for (x, z) in ((9, 9), (-9, 9), (9, -9), (-9, -9)):
        for y in (100, 101, 102):
            b.zet(x, y, z, "birch_fence", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
        b.zet(x, 103, z, "sea_lantern")
        b.zet(x, 104, z, "pink_wool")
        for dx, dz, bloem in ((1, 0, "pink_tulip"), (-1, 0, "allium"), (0, 1, "pink_tulip"), (0, -1, "allium")):
            b.zet(x + dx, VLOER, z + dz, "moss_block")
            b.zet(x + dx, 100, z + dz, bloem)
    b.zet(*KNABBELS[9], "guhs:lobby_gouden_knabbel", {"nummer": "9"})       # (instead of the tulip south of the lamp post)
    # cushions for the lobby guhs (they sit ON the floor block: a wool block in the floor marks their spot)
    for (x, y, z, _) in CHATGUHS[:4]:
        b.zet(x, VLOER, z, "magenta_wool")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            b.zet(x + dx, VLOER, z + dz, "pink_wool")
    # benches at the rim, facing the middle
    for (x, z, f) in ((-20, 30, "north"), (20, 30, "north"), (-36, -14, "east"), (36, -10, "west")):
        b.zet(x, 100, z, "guhs:guh_bank", {"facing": f})
    # the two knabbels behind things
    b.zet(*KNABBELS[2], "guhs:lobby_gouden_knabbel", {"nummer": "2"})
    b.zet(*KNABBELS[7], "guhs:lobby_gouden_knabbel", {"nummer": "7"})
    # signs at the spawn point
    staand_bord(h, b, -3, 100, -3, 0, ["Welkom op", "GUHPIXEL", "De vadsigste", "server ooit"], gloei=True)
    staand_bord(h, b, 6, 100, -8, 0, ["Regels:", "1. Niet slopen", "2. Lief zijn", "3. Vadsen mag"])
    staand_bord(h, b, -6, 100, 10, 8, ["Lag? Dat is", "geen lag. De", "server doet", "een dutje."])


# =====================================================================================================================
def maak(h):
    """Builds the whole lobby and returns the Structure (not saved)."""
    s = h.Structure((W, H, W))
    b = Bouw(s)
    bodem = eiland(b)
    pads(b)
    logo(b)
    stallen(h, b)
    winkel(h, b, bodem)
    afk_hoek(h, b)
    parkour(h, b)
    uitgang(h, b)
    borden(h, b)
    versiering(h, b)
    # the free space of every pad, once more (walls and roads may not creep in)
    for naam, (ax, ay, az, _) in ANKERS.items():
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in range(4):
                    blok = b.get(ax + dx, ay + dy, az + dz)
                    if blok not in LUCHT and blok != "guhs:guhpixel_portaal":
                        b.weg(ax + dx, ay + dy, az + dz)
    return s


def build(h):
    s = maak(h)
    controleer(s)
    s.save(NAME)
    h.w(f"{h.D}/guhpixel/lobby_versie.json", {"versie": VERSIE})
    return s


def _afstand(a, b):
    """The air gap between two landing areas: the smallest horizontal distance between their blocks, minus one."""
    return min(math.hypot(p[0] - q[0], p[2] - q[2]) for p in a for q in b) - 1


def controleer(s):
    """The fixed geometry every version of the lobby must keep (see the module comment), and the lobby's own promises:
    the knabbels, the parkour (every jump can be made), the chat guhs' spots."""
    problems = []
    if s.size[0] != 97 or s.size[2] != 97 or s.size[1] > 96:
        problems.append(f"the lobby must be 97 x (max 96) x 97, not {s.size}")
    for naam, (ax, ay, az, _) in ANKERS.items():
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if s.get(*lokaal(ax + dx, ay - 1, az + dz)) in LUCHT:
                    problems.append(f"anchor {naam}: no floor at {(ax + dx, ay - 1, az + dz)}")
                for dy in range(4):
                    b = s.get(*lokaal(ax + dx, ay + dy, az + dz))
                    if b not in LUCHT and b != "guhs:guhpixel_portaal":
                        problems.append(f"anchor {naam}: {b} in the way at {(ax + dx, ay + dy, az + dz)}")
    for (x, y, z) in PORTAAL:
        b = s.blocks.get(lokaal(x, y, z))
        if not b or b[0] != "guhs:guhpixel_portaal" or b[1].get("soort") != "uit":
            problems.append(f"no exit portal block at {(x, y, z)}")
    for (x, y, z), (b, _, _) in s.blocks.items():
        if b in ("minecraft:water", "minecraft:lava"):
            problems.append(f"free {b} at template {(x, y, z)}")
        if not (0 <= x < s.size[0] and 0 <= y < s.size[1] and 0 <= z < s.size[2]):
            problems.append(f"{b} outside the template at {(x, y, z)}")
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "LobbyPlek.java")
    if os.path.exists(java):
        src = open(java, encoding="utf-8").read()
        found = {m.group(1): tuple(int(float(v)) for v in m.group(2, 3, 4, 5))
                 for m in re.finditer(r"^\s+([A-Z_]+)\((-?\d+), (-?\d+), (-?\d+), (-?\d+)\)[,;]", src, re.M)}
        if found != ANKERS:
            problems.append("ANKERS is not the same table as LobbyPlek.java")

    def get(x, y, z):
        return s.get(*lokaal(x, y, z))

    # the shop stands where the contract says and the Verkoper can be reached from the plaza
    if get(WINKEL[0], 103, 0) in LUCHT or get(WINKEL[1], 106, 6) in LUCHT or get(WINKEL[1], 102, 6) not in LUCHT:
        problems.append("the shop building (x -44..-25, z -6..18) or its doorway is not right")
    # the knabbels: ten different numbers, each on something solid, each with a free neighbour to click it from
    gevonden = {}
    for (x, y, z), (b, props, _) in s.blocks.items():
        if b == "guhs:lobby_gouden_knabbel":
            gevonden[int(props["nummer"])] = (x + MIN[0], y + MIN[1], z + MIN[2])
    if gevonden != KNABBELS:
        problems.append(f"the golden knabbels in the template {gevonden} are not KNABBELS")
    for n, (x, y, z) in KNABBELS.items():
        if get(x, y - 1, z) in LUCHT:
            problems.append(f"knabbel {n} floats at {(x, y, z)}")
        if all(get(x + dx, y, z + dz) not in LUCHT or get(x + dx, y + 1, z + dz) not in LUCHT or get(x + dx, y - 1, z + dz) in LUCHT
               for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            problems.append(f"knabbel {n} at {(x, y, z)} cannot be reached: nowhere to stand beside it")
    # the chat guhs: a floor, and room
    for (x, y, z, _) in CHATGUHS:
        if get(x, y - 1, z) in LUCHT or get(x, y, z) not in LUCHT or get(x, y + 1, z) not in LUCHT or in_pad(x, y, z):
            problems.append(f"chat guh spot {(x, y, z)} is not free")
    # the parkour: start, finish and checkpoints are there; every landing has headroom; every jump can be made
    if get(*PARKOUR_START) != "guhs:lobby_parkour_start" or get(*PARKOUR_FINISH) != "guhs:lobby_parkour_finish":
        problems.append("the parkour start or finish block is missing")
    for pos in PARKOUR_TUSSEN:
        if get(*pos) != "guhs:lobby_parkour_tussenpunt":
            problems.append(f"no parkour checkpoint at {pos}")
    route = parkour_route()
    for i, deel in enumerate(route):
        for (x, y, z) in deel:
            if get(x, y, z) in LUCHT:
                problems.append(f"parkour part {i}: no block at {(x, y, z)}")
        if len(deel) == 1:
            x, y, z = deel[0]
            for dy in (1, 2, 3):
                if get(x, y + dy, z) not in LUCHT:
                    problems.append(f"parkour part {i}: no headroom above {(x, y, z)}")
    for i, (a, b) in enumerate(zip(route, route[1:])):
        hoger = b[0][1] - a[0][1]
        gat = _afstand(a, b)
        if hoger > 1 or (hoger == 1 and gat > 2.0) or (hoger <= 0 and gat > 3.0):
            problems.append(f"parkour jump {i} -> {i + 1} is too hard: {hoger} up over a gap of {gat:.2f}")
    if any(p in route[-1] for p in [PARKOUR_FINISH]) is False:
        problems.append("the parkour finish is not on the last roof")
    if problems:
        raise SystemExit("guhpixel lobby check failed:\n  " + "\n  ".join(problems[:40]))
