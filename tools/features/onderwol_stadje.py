"""
1.7.0 "Onder de Wol", rounds 32-43 (design: MineScapeStory/ontwerp-guhs170-mijnen.md, "Vadsmijnstadje"; framework:
features/onderwol_bouw.py; Java: feature/onderwol/stadje): the Vadsmijnstadje, an underground village of REAL villagers
(guh villagers, VillagerType guhs:guh) in a big cave hall in the knuffelsteen layer.

    the hall      ~59 x 59 blocks, a dome up to ~19 high, carved by the template itself (its air is air in the world,
                  its rock is the SCHIL marker: the layer stone of each spot where the world is open); glitterranken
                  hanging from the ceiling, lichtmos, pluisbolletjes and crystals along the wall
    the plaza     in the middle (radius 8): the Guhrad-centrale, a pavilion with a running Guhrad (feature/vadskracht
                  GuhWheelBlock, a guh inside it, no owner: nobody can take it out). The vadskracht system has no lamps,
                  so the village lamps are decorative; the wheel IS a redstone source while its guh runs, so the two
                  redstone lamps right against it (under and behind it) burn on the guh's power. The bell (the meeting
                  point), benches, lantern posts and the welcome board.
    10 houses     on a ring around the plaza (one every 30 degrees, north and south left open), walls of viltsteen
                  bricks or pastel bricks (there is no guhklei brick: guhklei smelts to pink terracotta, so the
                  "guhklei-baksteen" houses are pastelbaksteen with pink terracotta corners), pastel dakpan roofs, a
                  door with a name board towards the plaza and a path to it. In each: a bed per inhabitant, the
                  inhabitant's workstation (so it keeps its job) and a chest or barrel.
    villagers     10 grown-ups, every one a guh villager with a guh profession: the four new ones of feature/guhvillagers
                  (Toverguh, Vadsmonteur, 2 x Mijnwerker, Kristalslijper) and five of the older guh jobs (Knabbelboer,
                  Kleermaker, Vadssmid, Hamsterbouwer, Vadstemmer: guh villagers never keep a vanilla job, see
                  ModVillagers.onVillagerTick), plus 2 children; Xp 1, so they never lose their job; 12 beds; 2
                  IJzerguhlems
    the lift      north: the marker of the entrance hut's shaft (b.ingang): the Mijningang-hutje of
                  onderwol_structuren1.py on the surface, with a wolkenlift down (ingang kans 1.0)
    the mine      south: rails from a mine tunnel (caved in at its end) to the ore depot, with a chest cart

Rarity: random_spread spacing 94 chunks (~1500 blocks), separation 40.

    PYTHONPATH=tools python tools/features/onderwol_stadje.py      (from the project root; standalone, idempotent)
    PYTHONPATH=tools python tools/features/onderwol_stadje.py --render   (+ the preview mijnen-ontwerp/r30_vadsmijnstadje.png)
"""
import json
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
if os.path.join(HERE, "..") not in sys.path:
    sys.path.insert(0, os.path.join(HERE, ".."))

import make_structures as ms  # noqa: E402
import sign_text  # noqa: E402
from features import onderwol_bouw as ob  # noqa: E402

NAAM = "vadsmijnstadje"
PREFIX = "sign.guhs.onderwol_bouw.stadje"
SALT = 17040001
SX, SY, SZ = 64, 24, 64
C = 32               # the middle (x and z)
VL = 1               # the floor layer (you stand at y 2)
STA = VL + 1
R = 29.5             # the hall's radius (it wobbles about a block)
RING = 21.0          # the houses' ring
PLEIN = 8.5          # the plaza's radius
EN_OVERLAY = os.path.join("tools", "lang", "en", "c197_vadsmijnstadje.json")

DIRS = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}

# the guh professions and their workstations (feature/guhvillagers and registry/ModVillagers)
WERKPLEK = {"guhs:toverguh": "toverguh_boekenstandaard", "guhs:vadsmonteur": "guhrad_werkbank", "guhs:mijnwerker": "ertsenkist",
            "guhs:kristalslijper": "kristalslijpsteen", "guhs:knabbelboer": "zaadbak", "guhs:guh_kleermaker": "naaitafel",
            "guhs:vadssmid": "vadsaambeeld", "guhs:hamsterbouwer": "buizenbank", "guhs:vads_temmer": "knabbelbak"}

# the houses: angle on the ring (0 = east, 90 = south), profession, wall colour, roof colour, style, children, name board
HUIZEN = [
    (0, "guhs:toverguh", "lavendel", "lavendel", "vilt", 0, ["Huize Hocus", "Toverguh", "Spreukjes vers", "van de pers!"]),
    (30, "guhs:vadsmonteur", "aqua", "mint", "klei", 0, ["Huize Moer", "Vadsmonteur", "Wij maken alles", "behalve haast."]),
    (60, "guhs:mijnwerker", "mokka", "kersrood", "vilt", 0, ["Huize Houweel", "Mijnwerker", "Hak hak, njeg!", ""]),
    (120, "guhs:mijnwerker", "perzik", "perzik", "klei", 1, ["Huize Pikhaak", "Mijnwerker", "Erts? Ruilen!", "Kaas? Houden."]),
    (150, "guhs:knabbelboer", "boter", "kersrood", "vilt", 1, ["Huize Knabbel", "Knabbelboer", "Knabbels zonder", "zon. Hoe? Vads!"]),
    (180, "guhs:guh_kleermaker", "roze", "roze", "klei", 0, ["Huize Naaldje", "Kleermaker", "Ook sokjes voor", "guhpootjes!"]),
    (210, "guhs:vadssmid", "muisgrijs", "hemelsblauw", "vilt", 0, ["Huize Aambeeld", "Vadssmid", "Tik tik, vads!", ""]),
    (240, "guhs:kristalslijper", "hemelsblauw", "hemelsblauw", "klei", 0, ["Huize Glinster", "Kristalslijper", "Alles glimt hier", "behalve ik."]),
    (300, "guhs:hamsterbouwer", "pistache", "mint", "vilt", 0, ["Huize Buis", "Hamsterbouwer", "Buizen in alle", "kleuren roze."]),
    (330, "guhs:vads_temmer", "orchidee", "lavendel", "klei", 0, ["Huize Knabbelbak", "Vadstemmer", "Aaien mag.", "Voeren ook."]),
]

BORDEN = {
    "welkom": ["Welkom in het", "Vadsmijnstadje!", "Diep onder de", "wol. Njeg!"],
    "inwoners": ["Inwoners: 12", "Guhraderen: 1", "Kaas: genoeg", "Zon: nooit"],
    "rennen": ["Niet rennen.", "Behalve in het", "Guhrad. Daar", "MOET je rennen."],
    "lift": ["Wolkenlift", "Op = naar boven", "Neer = hier ben", "je al. Njeg!"],
    "centrale": ["Guhrad-centrale", "Vadskracht: AAN", "Niet storen,", "hij rent!"],
    "bel": ["Dorpsbel", "Eén keer: eten.", "Twee keer: ook", "eten."],
    "depot": ["Ertsdepot", "Karretjes niet", "voeren. Njeg!", ""],
    "gang": ["Oude mijngang", "Ingestort.", "Andere kant op!", ""],
}

EN = {
    "Huize Hocus": "House Hocus", "Toverguh": "Toverguh", "Spreukjes vers": "Spells fresh", "van de pers!": "off the press!",
    "Huize Moer": "House Nut", "Vadsmonteur": "Vads Mechanic", "Wij maken alles": "We fix it all", "behalve haast.": "except hurry.",
    "Huize Houweel": "House Pickaxe", "Mijnwerker": "Miner", "Hak hak, njeg!": "Dig dig, njeg!",
    "Huize Pikhaak": "House Pickhook", "Erts? Ruilen!": "Ore? Trade!", "Kaas? Houden.": "Cheese? Mine.",
    "Huize Knabbel": "House Nibble", "Knabbelboer": "Nibble Farmer", "Knabbels zonder": "Nibbles without", "zon. Hoe? Vads!": "sun. How? Vads!",
    "Huize Naaldje": "House Needle", "Kleermaker": "Tailor", "Ook sokjes voor": "Socks for", "guhpootjes!": "guh paws too!",
    "Huize Aambeeld": "House Anvil", "Vadssmid": "Vads Smith", "Tik tik, vads!": "Tink tink, vads!",
    "Huize Glinster": "House Sparkle", "Kristalslijper": "Crystal Cutter", "Alles glimt hier": "All shines here", "behalve ik.": "except me.",
    "Huize Buis": "House Tube", "Hamsterbouwer": "Hamster Builder", "Buizen in alle": "Tubes in every", "kleuren roze.": "shade of pink.",
    "Huize Knabbelbak": "House Snack Tray", "Vadstemmer": "Vads Tamer", "Aaien mag.": "Petting: yes.", "Voeren ook.": "Feeding too.",
    "Welkom in het": "Welcome to", "Vadsmijnstadje!": "Vads Mine Town!", "Diep onder de": "Deep under the", "wol. Njeg!": "wool. Njeg!",
    "Inwoners: 12": "Residents: 12", "Guhraderen: 1": "Guh wheels: 1", "Kaas: genoeg": "Cheese: plenty", "Zon: nooit": "Sun: never",
    "Niet rennen.": "No running.", "Behalve in het": "Except in the", "Guhrad. Daar": "guh wheel. There", "MOET je rennen.": "you MUST run.",
    "Wolkenlift": "Cloud lift", "Op = naar boven": "Up = upstairs", "Neer = hier ben": "Down = you are", "je al. Njeg!": "here. Njeg!",
    "Guhrad-centrale": "Guh wheel plant", "Vadskracht: AAN": "Vads power: ON", "Niet storen,": "Do not disturb,", "hij rent!": "he's running!",
    "Dorpsbel": "Village bell", "Eén keer: eten.": "Once: dinner.", "Twee keer: ook": "Twice: also", "eten.": "dinner.",
    "Ertsdepot": "Ore depot", "Karretjes niet": "Don't feed the", "voeren. Njeg!": "carts. Njeg!",
    "Oude mijngang": "Old mine tunnel", "Ingestort.": "Caved in.", "Andere kant op!": "Other way!",
}

BOUWSTOF = [("viltsteen", 6, 4, 12), ("viltsteen_bakstenen", 4, 4, 10), ("guhklei", 5, 2, 8), ("knuffelsteen", 4, 4, 10),
            ("gladde_knuffelsteen", 3, 2, 8), ("viltsteen_tegels", 3, 2, 8)]
SNACK = [("kaas_knabbels", 8, 2, 8), ("vahoege_vads", 4, 1, 3), ("kaaskorrel", 3, 1, 4), ("glitterbes", 2, 1, 4)]
LOOT_DEF = {
    "vadsmijnstadje": [((2, 4), BOUWSTOF), ((1, 3), SNACK), (1, [("lampion_roze", 3, 1, 2), ("lampion_mint", 3, 1, 2),
                                                                 ("guh_lantaarnpaal", 2, 1, 1), ("leeg", 5, 1, 1)])],
    "vadsmijnstadje_depot": [((3, 5), [("kaaskorrel", 4, 2, 6), ("guhklei", 4, 2, 6), ("minecraft:raw_iron", 3, 2, 5), ("minecraft:coal", 4, 3, 8),
                                       ("minecraft:rail", 2, 4, 10), ("vahoegiet", 1, 1, 1)]), ((1, 2), SNACK)],
}
LOOT = {n: f"guhs:chests/onderwol/{n}" for n in LOOT_DEF}


def _dir(v):
    return DIRS[(int(v[0]), int(v[1]))]


def _villager(beroep, kind=False, yaw=0.0):
    nbt = {"id": "minecraft:villager", "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(float(yaw), 0.0),
           "VillagerData": {"type": "guhs:guh", "profession": beroep or "minecraft:none", "level": 1}}
    if beroep:
        nbt["Xp"] = 1   # (an employed villager with 0 xp loses its job the first tick it has no job site memory)
    if kind:
        nbt["Age"] = -24000
    return nbt


# =====================================================================================================================
# the hall
# =====================================================================================================================
class Zaal:
    def __init__(self, seed=1704):
        rng = random.Random(seed)
        self.fase = [rng.uniform(0, 6.3) for _ in range(6)]

    def straal(self, hoek):
        f = self.fase
        return R + 0.7 * math.sin(3 * hoek + f[0]) + 0.5 * math.sin(5 * hoek + f[1]) + 0.35 * math.sin(8 * hoek + f[2])

    def d(self, x, z):
        """0 in the middle, 1 at the wall (> 1: outside)."""
        dx, dz = x - C + 0.5, z - C + 0.5
        return math.hypot(dx, dz) / self.straal(math.atan2(dz, dx))

    def top(self, x, z):
        """The highest air y of this column (a dome: ~19 above the floor in the middle, ~6 at the wall)."""
        d = self.d(x, z)
        f = self.fase
        h = 5.5 + 13.5 * math.sqrt(max(0.0, 1 - d * d)) + 0.8 * math.sin(x * 0.55 + f[3]) * math.cos(z * 0.45 + f[4])
        return min(SY - 3, VL + int(round(h)))


def bouw_zaal(b, zaal, rng):
    binnen = {}
    for x in range(SX):
        for z in range(SZ):
            if zaal.d(x, z) <= 1.0:
                binnen[(x, z)] = zaal.top(x, z)
    for (x, z), top in binnen.items():
        b.schil(x, 0, z)
        r = rng.random()
        b.zet(x, VL, z, "gladde_knuffelsteen" if r < 0.22 else "viltkiezel" if r < 0.30 else "knuffelsteen")
        for y in range(STA, top + 1):
            b.set(x, y, z, "minecraft:air")
        b.schil(x, top + 1, z)
        if top + 2 < SY:
            b.schil(x, top + 2, z)
    # the wall: the rock around the hall (columns next to it)
    for x in range(SX):
        for z in range(SZ):
            if (x, z) in binnen:
                continue
            buren = [binnen[(x + dx, z + dz)] for dx in (-1, 0, 1) for dz in (-1, 0, 1) if (x + dx, z + dz) in binnen]
            if buren:
                for y in range(0, max(buren) + 2):
                    b.schil(x, y, z)
    return binnen


# =====================================================================================================================
# a house
# =====================================================================================================================
W, D = 7, 6          # wide (along the front), deep


class Huis:
    """A house in its own frame: u 0..W-1 along the front (door at u 3), v 0..D-1 from the front to the back."""

    def __init__(self, b, hoek):
        a = math.radians(hoek)
        mx, mz = C - 0.5 + RING * math.cos(a), C - 0.5 + RING * math.sin(a)
        # the front faces the plaza: the cardinal direction closest to "towards the middle"
        tx, tz = -math.cos(a), -math.sin(a)
        self.F = (int(round(tx)), 0) if abs(tx) >= abs(tz) else (0, int(round(tz)))
        self.Rt = (-self.F[1], self.F[0])
        # the door M lies on the front, 2.5 from the middle of the house
        self.M = (int(round(mx + 2.5 * self.F[0])), int(round(mz + 2.5 * self.F[1])))
        self.b = b

    def w(self, u, v):
        """(u, v) -> world x, z (v < 0: in front of the house)."""
        return (self.M[0] + (u - 3) * self.Rt[0] - v * self.F[0], self.M[1] + (u - 3) * self.Rt[1] - v * self.F[1])

    def zet(self, u, v, y, s, nbt=None):
        x, z = self.w(u, v)
        self.b.zet(x, y, z, s, nbt)

    def voetafdruk(self, rand=0):
        return {self.w(u, v) for u in range(-rand, W + rand) for v in range(-rand, D + rand)}


def bouw_huis(b, h, beroep, kleur, dak, stijl, kinderen, bord, rng):
    F, Rt = h.F, h.Rt
    naar_voor, naar_achter = _dir(F), _dir((-F[0], -F[1]))
    links, rechts = _dir((-Rt[0], -Rt[1])), _dir(Rt)
    if stijl == "vilt":
        muur, hoek = "viltsteen_bakstenen", f"pastelbaksteen_{kleur}"
    else:
        muur, hoek = f"pastelbaksteen_{kleur}", "minecraft:pink_terracotta"
    trap = f"dakpan_{dak}_trap"
    # floor, walls, inside air
    for u in range(W):
        for v in range(D):
            rand = u in (0, W - 1) or v in (0, D - 1)
            h.zet(u, v, VL, "gepolijste_viltsteen" if rand else "vadshout_planken")
            for y in range(STA, STA + 4):
                if rand:
                    h.zet(u, v, y, hoek if (u in (0, W - 1) and v in (0, D - 1)) else muur)
                else:
                    h.zet(u, v, y, "minecraft:air")
            for y in range(STA + 4, STA + 7):
                if not rand:
                    h.zet(u, v, y, "minecraft:air")
    # the gables (u 0 and W-1) and the roof: stairs stepping up from front and back to a ridge along u
    for u in (0, W - 1):
        for v in range(1, D - 1):
            h.zet(u, v, STA + 4, muur)
        for v in range(2, D - 2):
            h.zet(u, v, STA + 5, muur)
    for k, (vf, va) in enumerate([(-1, D), (0, D - 1), (1, D - 2), (2, D - 3)]):
        y = STA + 3 + k
        for u in range(-1, W + 1):
            h.zet(u, vf, y, f"{trap}[facing={naar_achter},half=bottom,shape=straight]")
            h.zet(u, va, y, f"{trap}[facing={naar_voor},half=bottom,shape=straight]")
    # windows, door, light
    for v in (2, 3):
        h.zet(0, v, STA + 1, "huisje_raam")
        h.zet(W - 1, v, STA + 1, "huisje_raam")
    h.zet(1, 0, STA + 1, "huisje_raam")
    h.zet(5, 0, STA + 1, "huisje_raam")
    h.zet(3, D - 1, STA + 1, "huisje_raam")
    deur = "vadshout_deur" if stijl == "vilt" else "roze_lakhout_deur"
    h.zet(3, 0, STA, f"{deur}[facing={naar_achter},half=lower,hinge=left,open=false]")
    h.zet(3, 0, STA + 1, f"{deur}[facing={naar_achter},half=upper,hinge=left,open=false]")
    h.zet(3, -1, VL, "viltsteen_tegels")
    h.zet(1, -1, STA + 2, "lampion_roze[hanging=true]")
    h.zet(5, -1, STA + 2, "lampion_geel[hanging=true]")
    h.zet(3, 2, STA + 5, "lampion_mint[hanging=true]")
    # the name board right of the door
    x, z = h.w(4, -1)
    b.sign(x, STA + 1, z, naar_voor, bord, wall=True)
    # inside: beds (head at the back), the workstation, a chest or barrel, a plant
    bedden = []
    for i in range(1 + kinderen):
        u = 1 + i
        kleurbed = ["pink", "magenta", "light_blue", "lime"][(i + len(bord[0])) % 4]
        h.zet(u, 3, STA, f"minecraft:{kleurbed}_bed[facing={naar_achter},part=foot,occupied=false]")
        h.zet(u, 4, STA, f"minecraft:{kleurbed}_bed[facing={naar_achter},part=head,occupied=false]")
        bedden.append(h.w(u, 4))
    h.zet(5, 4, STA, f"{WERKPLEK[beroep]}[facing={naar_voor}]")
    x, z = h.w(5, 1)
    if rng.random() < 0.5:
        b.kist(x, STA, z, links, LOOT["vadsmijnstadje"])
    else:
        b.ton(x, STA, z, LOOT["vadsmijnstadje"])
    h.zet(1, 1, STA, rng.choice(["potted_roze_guhbloem", "potted_knabbelroos", "potted_kaasbloem"]))
    if kinderen == 0:
        h.zet(2, 4, STA, "pluiskussen")
    # the inhabitants
    x, z = h.w(3, 2)
    b.entity(x + 0.5, STA, z + 0.5, _villager(beroep, yaw=rng.uniform(0, 360)))
    return bedden


# =====================================================================================================================
# the village
# =====================================================================================================================
def pad_naar_plein(b, start, binnen, bezet, rng):
    """A path two wide from the front of a door straight to the plaza."""
    x, z = start
    stappen = 0
    while math.hypot(x - C + 0.5, z - C + 0.5) > PLEIN - 0.5 and stappen < 40:
        for (px, pz) in ((x, z), (x + (1 if abs(z - C) > abs(x - C) else 0), z + (0 if abs(z - C) > abs(x - C) else 1))):
            if (px, pz) in binnen and (px, pz) not in bezet:
                b.zet(px, VL, pz, "viltsteen_tegels" if rng.random() < 0.75 else "gepolijste_viltsteen")
                bezet.add((px, pz))
        dx, dz = C - 0.5 - x, C - 0.5 - z
        if abs(dx) > abs(dz):
            x += 1 if dx > 0 else -1
        else:
            z += 1 if dz > 0 else -1
        stappen += 1


def lantaarnpaal(b, x, z):
    b.zet(x, STA, z, "guh_lantaarnpaal[half=lower]")
    b.zet(x, STA + 1, z, "guh_lantaarnpaal[half=upper]")


def bouw_stadje(h=None):
    rng = random.Random(170432)
    b = ob.Bouw((SX, SY, SZ), NAAM, prefix=PREFIX)
    zaal = Zaal()
    binnen = bouw_zaal(b, zaal, rng)
    bezet = set()        # floor columns already used (paths, plaza, houses)
    hoog = {}            # (x, z) -> the lowest y a glitterrank may reach down to here

    # --- the plaza
    for (x, z) in binnen:
        r = math.hypot(x - C + 0.5, z - C + 0.5)
        if r <= PLEIN:
            if r < 3.6:
                s = "gepolijste_viltsteen"
            elif r < 4.6:
                s = "pastelbaksteen_roze"
            elif r < 7.6:
                s = "viltsteen_tegels" if (x + z) % 2 else "pastelbaksteen_vanille"
            else:
                s = "viltsteen_bakstenen"
            b.zet(x, VL, z, s)
            bezet.add((x, z))
            hoog[(x, z)] = 13 if r < 5 else 6

    # --- the Guhrad-centrale: a pavilion with the running wheel
    for (px, pz) in ((C - 3, C - 3), (C + 3, C - 3), (C - 3, C + 3), (C + 3, C + 3)):
        for y in range(STA, STA + 5):
            b.zet(px, y, pz, "viltsteen_bakstenen_muur")
    for k, r in enumerate((4, 3, 2, 1)):
        y = STA + 5 + k
        for x in range(C - r, C + r + 1):
            for z in range(C - r, C + r + 1):
                if max(abs(x - C), abs(z - C)) != r:
                    continue
                if abs(x - C) == r and abs(z - C) == r:
                    b.zet(x, y, z, "dakpan_mint")
                elif abs(z - C) == r:
                    b.zet(x, y, z, f"dakpan_mint_trap[facing={'south' if z < C else 'north'},half=bottom,shape=straight]")
                else:
                    b.zet(x, y, z, f"dakpan_mint_trap[facing={'east' if x < C else 'west'},half=bottom,shape=straight]")
    b.zet(C, STA + 9, C, "dakpan_mint")
    b.zet(C, STA + 10, C, "guh_kristal_lamp")
    for (px, pz) in ((C - 2, C - 2), (C + 2, C - 2), (C - 2, C + 2), (C + 2, C + 2)):
        b.zet(px, STA + 4, pz, "lampion_roze[hanging=true]")
    # the wheel: the block at (C, STA, C), facing south (along = west), its 8 parts; a guh running inside, no owner
    guh = ms.guh_nbt(1.0)
    b.zet(C, STA, C, "guh_wheel[facing=south,running=true]", {"id": "guhs:guh_wheel", "HasGuh": ms.Byte(1), "Blij": ms.Byte(1), "Guh": guh})
    for s in range(3):
        for hgt in range(3):
            if s == 1 and hgt == 0:
                continue
            b.zet(C - (s - 1), STA + hgt, C, f"guh_wheel_part[facing=south,side={s},height={hgt}]")
    # the guh's power: two redstone lamps right against the wheel (under it and behind it), lit while it runs
    b.zet(C, VL, C, "minecraft:redstone_lamp[lit=true]")
    b.zet(C, STA, C - 1, "minecraft:redstone_lamp[lit=true]")
    b.zet(C - 1, STA, C - 1, "viltsteen_tegels_plaat[type=bottom]")
    b.zet(C + 1, STA, C - 1, "viltsteen_tegels_plaat[type=bottom]")
    b.sign(C + 2, STA, C + 3, "south", BORDEN["centrale"], wall=False)
    # the bell (the meeting point) and benches, lantern posts
    b.zet(C, STA, C - 6, "minecraft:bell[attachment=floor,facing=north,powered=false]")
    b.sign(C + 1, STA, C - 6, "north", BORDEN["bel"], wall=False)
    for (bx, bz, f) in ((C - 6, C, "east"), (C + 5, C, "west"), (C - 6, C + 1, "east"), (C + 5, C - 1, "west"),
                        (C - 1, C + 6, "north"), (C, C + 6, "north")):
        b.zet(bx, STA, bz, f"guh_bankje_roze[facing={f}]")
    for i in range(8):
        a = math.radians(22.5 + 45 * i)
        lantaarnpaal(b, int(round(C - 0.5 + 7.2 * math.cos(a))), int(round(C - 0.5 + 7.2 * math.sin(a))))

    # --- the houses
    bedden = []
    huizen = []
    for (hoek, beroep, kleur, dak, stijl, kinderen, bord) in HUIZEN:
        h = Huis(b, hoek)
        for hv in huizen:
            assert not (h.voetafdruk(1) & hv.voetafdruk()), f"house {bord[0]} touches another"
        for p in h.voetafdruk(1):
            assert p in binnen and zaal.top(*p) >= STA + 8, f"house {bord[0]} does not fit under the ceiling at {p}"
        huizen.append(h)
        bedden += bouw_huis(b, h, beroep, kleur, dak, stijl, kinderen, bord, rng)
        for p in h.voetafdruk(1):
            bezet.add(p)
            hoog[p] = STA + 9
    for h in huizen:
        x, z = h.w(3, -2)
        pad_naar_plein(b, (x, z), binnen, bezet, rng)
        lx, lz = h.w(0, -2)
        if (lx, lz) in binnen:
            lantaarnpaal(b, lx, lz)
            bezet.add((lx, lz))
    # the children play on the plaza
    b.entity(C - 3.5, STA, C + 5.5, _villager(None, kind=True, yaw=40))
    b.entity(C + 4.5, STA, C - 4.5, _villager(None, kind=True, yaw=220))
    # two IJzerguhlems keep watch
    b.entity(C - 9.5, STA, C + 0.5, {"id": "guhs:ijzerguhlem", "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(90.0, 0.0)})
    b.entity(C + 10.5, STA, C + 9.5, {"id": "guhs:ijzerguhlem", "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(225.0, 0.0)})

    # --- the lift (north): the shaft's marker, a platform, the welcome board
    lz = C - 23
    for x in range(C - 3, C + 4):
        for z in range(lz - 2, C - 8):
            if (x, z) in binnen:
                b.zet(x, VL, z, "viltsteen_tegels" if abs(x - C) <= 1 else "gepolijste_viltsteen")
                bezet.add((x, z))
                hoog[(x, z)] = 5
    for z in range(lz - 2, lz + 3):
        for x in (C - 3, C + 3):
            if z != lz + 2:
                b.zet(x, STA, z, "guh_hekje_roze")
    b.ingang(C, VL, lz, final="guhs:viltsteen_tegels")
    for x in range(C - 2, C + 3):
        for z in range(lz - 2, lz + 3):
            hoog[(x, z)] = SY   # (no glitterranken in the shaft)
    b.sign(C + 2, STA, lz + 3, "south", BORDEN["lift"], wall=False)
    # the welcome board: a little wall east of the lift street with three signs facing west
    for z in range(lz + 3, lz + 8):
        for y in range(STA, STA + 3):
            b.zet(C + 5, y, z, "viltsteen_bakstenen" if z in (lz + 3, lz + 7) else "pastelbaksteen_roze")
        b.zet(C + 5, STA + 3, z, "dakpan_roze_trap[facing=east,half=bottom,shape=straight]")
        bezet.add((C + 5, z))
    for i, k in enumerate(("welkom", "inwoners", "rennen")):
        b.sign(C + 4, STA + 1, lz + 4 + i, "west", BORDEN[k], wall=True)
    b.zet(C + 4, STA + 2, lz + 3, "lampion_geel[hanging=false]")
    b.zet(C + 4, STA + 2, lz + 7, "lampion_roze[hanging=false]")
    lantaarnpaal(b, C - 2, lz + 6)

    # --- the mine (south): a caved-in tunnel, rails to the ore depot, a chest cart
    tz0 = max(z for (x, z) in binnen if x == C) - 1
    for z in range(tz0, SZ):
        for x in range(C - 2, C + 3):
            for y in range(0, STA + 5):
                binnen_gang = abs(x - C) <= 1 and STA <= y <= STA + 3
                if binnen_gang:
                    b.set(x, y, z, "minecraft:air")
                elif (x, z) not in binnen or y == 0:
                    b.schil(x, y, z)
            if (x, z) not in binnen:
                b.zet(x, VL, z, "gladde_knuffelsteen" if abs(x - C) <= 1 else ob.SCHIL)
        if (z - tz0) % 3 == 1:
            for x in (C - 1, C + 1):
                for y in range(STA, STA + 3):
                    b.zet(x, y, z, "minecraft:oak_fence")
            for x in range(C - 1, C + 2):
                b.zet(x, STA + 3, z, "vadshout_planken")
            b.zet(C, STA + 2, z, "lampion_geel[hanging=true]")
    for z in range(SZ - 2, SZ):   # the caved-in end
        for x in range(C - 1, C + 2):
            for y in range(STA, STA + 4):
                if y < STA + 3 or rng.random() < 0.6:
                    b.zet(x, y, z, rng.choice(["viltkiezel", "knuffelsteen", "knuffelsteen", "kaasknabbelerts_knuffel"]))
    b.sign(C + 1, STA + 1, SZ - 3, "north", BORDEN["gang"], wall=True)
    rz = C + 13
    for z in range(rz + 1, SZ - 2):
        b.zet(C, STA, z, "minecraft:rail[shape=north_south,waterlogged=false]")
        bezet.add((C, z))
    b.zet(C, STA, rz, "minecraft:rail[shape=south_east,waterlogged=false]")
    for x in range(C + 1, C + 6):
        b.zet(x, STA, rz, "minecraft:rail[shape=east_west,waterlogged=false]")
    b.zet(C + 6, STA, rz, "viltsteen_bakstenen")
    b.zet(C + 6, STA + 1, rz, "lampion_geel[hanging=false]")
    for x in range(C - 1, C + 7):
        for z in range(rz - 1, rz + 2):
            if (x, z) in binnen and (x, z) not in bezet:
                b.zet(x, VL, z, "gladde_knuffelsteen")
                bezet.add((x, z))
    b.ton(C + 5, STA, rz - 1, LOOT["vadsmijnstadje_depot"])
    b.zet(C + 4, STA, rz - 1, "block_of_kaasknabbels")
    b.zet(C + 3, STA, rz - 1, "guhklei")
    b.sign(C + 2, STA, rz - 1, "south", BORDEN["depot"], wall=False)
    b.entity(C + 3.5, STA + 0.0625, rz + 0.5, {"id": "minecraft:chest_minecart", "LootTable": LOOT["vadsmijnstadje_depot"],
                                               "Rotation": ms.floats(90.0, 0.0)})
    b.entity(C + 0.5, STA + 0.0625, rz + 9.5, {"id": "minecraft:minecart", "Rotation": ms.floats(0.0, 0.0)})

    # --- the cave itself: glitterranken from the ceiling, plants and crystals along the wall
    kolommen = sorted(binnen)
    rng.shuffle(kolommen)
    n = 0
    for (x, z) in kolommen:
        if n >= 110:
            break
        d = zaal.d(x, z)
        if d < 0.08:
            continue
        top = binnen[(x, z)]
        lengte = rng.choice([1, 2, 2, 3, 3, 4, 5])
        onder = top - lengte + 1
        if onder < hoog.get((x, z), STA + 3) or onder <= STA + 2:
            continue
        for y in range(onder, top + 1):
            b.zet(x, y, z, "glitterrank_bes" if (y == onder and rng.random() < 0.35) else "glitterrank")
        n += 1
    for (x, z) in kolommen:
        if (x, z) in bezet or zaal.d(x, z) < 0.80:
            continue
        r = rng.random()
        if r < 0.10:
            b.zet(x, STA, z, "lichtmos")
        elif r < 0.15:
            b.zet(x, STA, z, "pluisbolletje")
        elif r < 0.19:
            b.zet(x, STA, z, "glinsterpaddenstoel")
        elif r < 0.215:
            b.zet(x, STA, z, "guh_kristal_cluster[facing=up]")
    # the anchor in the plaza's floor (south of the wheel)
    b.anker(C, VL, C + 5, final="guhs:pastelbaksteen_vanille")
    b.bedden = bedden
    return b


# =====================================================================================================================
# the resources
# =====================================================================================================================
def build(h):
    for naam, pools in LOOT_DEF.items():
        ob.loot(h, naam, pools)
    b = bouw_stadje(h)
    b.save(f"onderwol/{NAAM}/start")
    ob.grot_bouw(h, NAAM, templates=[(b, 1)], titel="Vadsmijnstadje",
                 tooltip="Een ondergronds stadje vol guh-villagers in een grote grotzaal, met een Guhrad-centrale op het plein.",
                 titel_en="Vads Mine Town", tooltip_en="An underground town full of guh villagers in a big cave hall, with a guh wheel plant on the square.",
                 salt=SALT, band=(22, 28), spacing=94, separation=40, ruimte="zaal", ingang=dict(kans=1.0), dekking=6, voorrang=70,
                 biomes=ob.guhmension_biomes(h))
    return b


def check(b):
    """Every guhs: block exists; villagers, workstations and beds add up."""
    fouten = []
    A = os.path.join("src", "main", "resources", "assets", "guhs", "blockstates")
    for (naam, _p, _n) in b.blocks.values():
        if naam.startswith("guhs:") and not os.path.exists(os.path.join(A, naam[5:] + ".json")):
            fouten.append(f"block {naam} does not exist")
    villagers = [e for e in b.entities if e[3]["id"] == "minecraft:villager"]
    koppen = [k for k, v in b.blocks.items() if v[0].endswith("_bed") and v[1].get("part") == "head"]
    if len(koppen) < len(villagers):
        fouten.append(f"{len(koppen)} beds for {len(villagers)} villagers")
    for (x, y, z, nbt) in villagers:
        beroep = nbt["VillagerData"]["profession"]
        if beroep == "minecraft:none":
            continue
        blok = "guhs:" + WERKPLEK[beroep]
        if not any(v[0] == blok and math.dist((k[0] + .5, k[1], k[2] + .5), (x, y, z)) <= 6 for k, v in b.blocks.items()):
            fouten.append(f"{beroep} at {(x, y, z)}: no {blok} close by")
    return sorted(set(fouten))


def _direct(render=False):
    import types
    Rr = os.path.join("src", "main", "resources")
    A, Dd = os.path.join(Rr, "assets", "guhs"), os.path.join(Rr, "data", "guhs")
    NL, ENG = {}, {}

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def lang(key, en, nl):
        ENG[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=Rr, A=A, D=Dd, w=w, lang=lang)
    b = build(h)
    for f in check(b):
        print("onderwol_stadje:", f)
    for k, nl in sign_text.TEXTS.items():
        if k.startswith(PREFIX + "."):
            NL[k] = nl
            ENG[k] = EN.get(nl, nl)
    for taal, teksten in (("nl_nl", NL), ("en_us", ENG)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    w(EN_OVERLAY, dict(sorted(ENG.items())))
    villagers = sum(1 for e in b.entities if e[3]["id"] == "minecraft:villager")
    print(f"onderwol_stadje: {len(b.blocks)} blocks, {villagers} villagers, {len(b.bedden)} beds, {len(NL)} texts")
    if render:
        voorbeeld(b)


# =====================================================================================================================
# the preview (mijnen-ontwerp/r30_vadsmijnstadje.png): the template in the render kit, ceiling cut off
# =====================================================================================================================
def voorbeeld(b):
    import zipfile
    import tempfile
    sys.path.insert(0, os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", "mijnen-ontwerp", "_bron")))
    import mijnkit as K
    V = K.V
    # the blocks of this checkout (newer than the kit's 1.6.4 assets)
    zp = os.path.join(tempfile.gettempdir(), "guhs_stadje_assets.zip")
    with zipfile.ZipFile(zp, "w") as zf:
        base = os.path.join("src", "main", "resources")
        for sub in ("blockstates", "models", "textures/block", "textures/entity"):
            top = os.path.join(base, "assets", "guhs", sub)
            for root, _, files in os.walk(top):
                for fn in files:
                    full = os.path.join(root, fn)
                    zf.write(full, os.path.relpath(full, base).replace(os.sep, "/"))
    V.JARS = [zp] + V.JARS
    V._res = V._bk = None
    V.EMISSIVE |= {"guhs:glitterrank", "guhs:glitterrank_bes", "guhs:glinsterpaddenstoel", "guhs:lichtmos", "guhs:guh_lantaarnpaal",
                   "minecraft:redstone_lamp", "guhs:pluisbolletje"}
    S = V.Scene(SX, SY, SZ)
    for (x, y, z), (naam, props, _nbt) in b.blocks.items():
        if naam in ("minecraft:air", "minecraft:jigsaw"):
            if naam == "minecraft:jigsaw":
                S.set(x, y, z, "guhs:viltsteen_tegels")
            continue
        if naam == ob.SCHIL:
            naam, props = ("guhs:knuffelsteen" if y < 16 else "guhs:viltsteen"), {}
        if naam.startswith("guhs:guh_wheel"):
            continue
        s = naam + ("[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]" if props else "")
        S.set(x, y, z, s)
    # the wheel as a ring of pink, the guh in it; villagers shown as guhs
    for s_ in range(3):
        for hgt in range(3):
            if not (s_ == 1 and hgt == 0):
                S.set(C - (s_ - 1), STA + hgt, C, "minecraft:pink_concrete")
    S.guh(C + 0.5, STA, C + 0.5, 180)
    for (x, y, z, nbt) in b.entities:
        if nbt["id"] == "minecraft:villager":
            S.guh(x, y, z, 180, sitting=False)
    # a section through the middle (the dome, the glitterranken, the centrale), then the dome cut off to look inside
    V.render(S, "r30_vadsmijnstadje_doorsnede", yaw=20, pitch=20, scale=9, cut=("z", C))
    for x in range(SX):
        for z in range(SZ):
            for y in range(STA + 10, SY):
                S.v[x, y, z] = 0
    V.render(S, "r30_vadsmijnstadje", yaw=35, pitch=38, scale=9)
    V.render(S, "r30_vadsmijnstadje_boven", yaw=0, pitch=85, scale=9)


if __name__ == "__main__":
    _direct(render="--render" in sys.argv)
