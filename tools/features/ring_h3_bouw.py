"""
bbq2 (ring-h3) - De Mijnen van Knabbelmoria, built block by block: ONE template (structure guhs:knabbelmoria, a "grot" of
wereld.bbq_structuur whose start pool gets ground_level_delta = G + 1, like the Zoutkristalmijn), sunk 30 blocks into the rock
under a cave floor of the Houtskoolvlakte.

What a player walks through (template coordinates: x east, z south, the cave floor's top block at y = G):

  the rock        a dark outcrop on the cave floor (the only part above the ground) with a sheer face on its west and east side.
  west forecourt  cracked paving, two dead worst trees, a rest fire: the Poort van Knabbelmoria, a slab of polished grill iron
                  under an arch of runes ("Zeg njeg en treed binnen").
  the stair       down north from the gate hall to
  lever hall      De Hal van de Hefbomen: four dwarf-guh levers under their emblems, a stele with the rhyme, a portcullis west.
  well room       De Wachtkamer: the well with its windlass (the bucket is still on the rim...), Durguh's tomb, a rest fire, a
                  caved-in corridor, and in the west wall six rune stones around a door nobody sees.
  the secret way  Gimguh's passage: a spiral stair down and a low tunnel to a last rest fire, and out into
  the great hall  De Zuilenhal: two rows of pillars under a stepped vault, heaps of knabbel treasure in the aisles, and in the
                  west wall a black cave mouth, the Diepe Poort, where something sleeps.
  the chasm       De Kloof: a pit of glowing coals. Over it first the Brokkelpad (stone that falls away under your feet,
                  between pillars that hold), then the bridgehead, then the Brug van Knabbel-dum: narrow, no railing.
  the long stair  from the east bank up and round to the east gate, which opens on the east forecourt.

Everything below the cave floor is wrapped in two blocks of plain houtskoolsteen, the stone the dimension is made of, so where
a cave or the sauce sea cuts into the build nothing shows but rock.

Shared with Java (feature/ringh3/Plekken.java, written by ring_h3_java.py and compared by the build): PLEKKEN.
  bouw(h) -> Bouw          the template and everything known about it
  check(b) -> [problems]   the geometry self-check
  preview(out)             pictures (python tools/features/ring_h3_bouw.py <out>)
"""
import math
import os
import random
import sys

NAAM = "knabbelmoria"
MIDDEN = "guhs:knabbelmoria_midden"
MAAT = (92, 46, 76)
G = 30                      # template y of the top block of the cave floor (you walk at G + 1)
BOVEN, MIDDEL, DIEP = G + 1, 21, 12       # the three walking levels: the surface, the lever hall and the well room, the great hall

HOUTSKOOL = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
GEBEITELD = "guhs:gebeitelde_houtskoolsteen_stenen"
MUUR = "guhs:houtskoolsteen_stenen_muur"
PLAAT = "guhs:houtskoolsteen_stenen_plaat"
TRAP = "guhs:houtskoolsteen_stenen_trap"
HEK = "guhs:houtskoolsteen_stenen_hek"
ROOSTER = "guhs:roosterijzer"
PILAAR = "guhs:roosterijzer_pilaar"
GEPOLIJST = "guhs:gepolijst_roosterijzer"
TRALIES = "guhs:roosterijzer_tralies"
GLOEIKOOL = "guhs:gloeikool"
SMEUL = "guhs:smeulkooltjes"
ROOKGAT = "guhs:rookgat"
UIENLICHT = "guhs:uienlicht"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
VERKOOLD = "guhs:verkoold_guhbot"
GRILLKOOL = "guhs:grillkool"
KNABBELS = "guhs:block_of_kaasknabbels"
KAASADER = "guhs:gouden_kaasader"
WORST = "guhs:worst_stam"
SATE = "guhs:sate_stam"
VUUR = "guhs:ring_rustvuur"
BROKKEL = "guhs:ringh3_brokkelsteen"
HENDEL = "guhs:ringh3_hendel"
RUNE = "guhs:ringh3_rune"
KAASSTEEN = "guhs:belegen_kaas_stenen"       # the trim of the dwarf-guhs: aged cheese stone, warm against the black
KAASTEGEL = "guhs:belegen_kaas_tegels"
GATENKAAS = "guhs:gatenkaas_stenen"         # and their gold
LEI = "minecraft:polished_deepslate"
ZWARTSTEEN = "minecraft:polished_blackstone_bricks"
ZWART = "minecraft:blackstone"
BASALT = "minecraft:polished_basalt"
BASALT_RUW = "minecraft:basalt"
TEGEL = "minecraft:deepslate_tiles"
VERGULD = "minecraft:gilded_blackstone"
KETTING = "minecraft:chain"
LANTAARN = "minecraft:lantern"
ZIELLAMP = "minecraft:soul_lantern"
AIR = "minecraft:air"

# the runes (block state teken of guhs:ringh3_rune)
KAAS, WORSTJE, SAUS, KNABBEL, BOT, VLAM, NJEG, TROMMEL = range(8)
# the order of the levers of the lever hall (the stele's rhyme): worst, kaas, saus, knabbel -> lever numbers
HENDEL_TEKENS = (KAAS, WORSTJE, SAUS, KNABBEL)          # lever nr i stands under this emblem
HENDEL_VOLGORDE = (1, 0, 2, 3)                          # "eerst de worst, dan de kaas, de saus erover, en de knabbel toe"
RUNE_GOED = KAAS                                        # the stone to knock on: what Durguh loved most
RUNE_KLOPPEN = 3

# --- the rooms (inclusive boxes of air: x0, y0, z0, x1, y1, z1) ------------------------------------------------------------
ROTS_MIDDEN = (48, 61)
ROTS_STRAAL = (14.5, 12.5)
WESTHAL = (39, BOVEN, 59, 45, BOVEN + 6, 65)
OOSTHAL = (51, BOVEN, 59, 57, BOVEN + 6, 65)
HEFBOOMHAL = (34, MIDDEL, 28, 50, MIDDEL + 5, 41)
PUTKAMER = (14, MIDDEL, 28, 28, MIDDEL + 5, 42)
ZUILENHAL = (18, DIEP, 3, 46, DIEP + 14, 25)
KLOOF = (47, 2, 3, 76, DIEP + 14, 25)
OOSTOEVER = (77, DIEP, 3, 86, DIEP + 14, 25)
DIEPE_POORT = (5, DIEP, 8, 17, DIEP + 13, 20)
RUSTNIS = (18, DIEP, 27, 24, DIEP + 3, 31)
BRUGGENHOOFD = (61, DIEP, 11, 65, DIEP, 17)
BRUG = (66, DIEP - 1, 13, 76, DIEP - 1, 15)             # the deck blocks
BRUG_KAPOT = (66, DIEP - 3, 13, 72, DIEP - 1, 15)       # what breaks in the scene (deck and arch)
# the doors: name -> the box of blocks that slides away (saved as templates ringh3_deur_<name>)
DEUREN = {
    "west": (38, BOVEN, 61, 38, BOVEN + 4, 63),
    "valhek": (31, MIDDEL, 34, 31, MIDDEL + 3, 36),
    "geheim": (13, MIDDEL, 35, 13, MIDDEL + 2, 36),
    "oost": (58, BOVEN, 61, 58, BOVEN + 4, 63),
}
# the crumbling path over the chasm: its corners (x, z of the first cell of each 2 x 2 pillar) from the hall to the bridgehead
PAD_HOEKEN = [(47, 13), (52, 13), (52, 7), (57, 7), (57, 19), (61, 19)]
PAD_EINDE = (61, 17)

SPIRAAL_MIDDEN = (8, 35)
RUNE_Z = (31, 32, 33, 38, 39, 40)                       # the six rune stones in the well room's west wall (x = 13, y = MIDDEL + 1)
RUNE_TEKENS = (BOT, WORSTJE, VLAM, KNABBEL, KAAS, SAUS)
HENDEL_X = (37, 40, 44, 47)                             # the levers on the north wall of the lever hall (z = 28, y = MIDDEL + 1)

# the cast: (kind, id, x, y, z, yaw, plek or None, step from, step to)
CAST = [
    ("guhdalf", "ringh3_guhdalf_poort", 35, BOVEN, 60, 250.0, "ringh3_poort", 0, 1),
    ("gimguh", "ringh3_gimguh_poort", 34, BOVEN, 65, 290.0, None, 0, 1),
    ("leguhlas", "ringh3_leguhlas_poort", 31, BOVEN, 66, 300.0, None, 0, 1),
    ("araguh", "ringh3_araguh_poort", 31, BOVEN, 58, 240.0, None, 0, 1),
    ("boromika", "ringh3_boromika_poort", 28, BOVEN, 65, 290.0, None, 0, 1),
    ("merrie", "ringh3_merrie_poort", 26, BOVEN, 60, 100.0, None, 0, 1),
    ("pippguh", "ringh3_pippguh_poort", 25, BOVEN, 62, 80.0, None, 0, 1),
    ("guhdalf", "ringh3_guhdalf_hal", 45, MIDDEL, 38, 30.0, "ringh3_hal", 2, 2),
    ("gimguh", "ringh3_gimguh_hal", 38, MIDDEL, 39, 330.0, None, 2, 2),
    ("gimguh", "ringh3_gimguh_gang", 15, MIDDEL, 37, 300.0, "ringh3_gang", 3, 4),
    ("pippguh", "ringh3_pippguh_put", 24, MIDDEL, 33, 100.0, "ringh3_put", 3, 4),
    ("merrie", "ringh3_merrie_put", 25, MIDDEL, 37, 60.0, None, 3, 4),
    ("guhdalf", "ringh3_guhdalf_put", 17, MIDDEL, 31, 320.0, "ringh3_put_guhdalf", 3, 4),
    ("araguh", "ringh3_araguh_buiten", 64, BOVEN, 62, 90.0, "ringh3_buiten", 6, 6),
    ("leguhlas", "ringh3_leguhlas_buiten", 66, BOVEN, 58, 120.0, None, 6, 6),
    ("gimguh", "ringh3_gimguh_buiten", 68, BOVEN, 65, 60.0, None, 6, 6),
    ("boromika", "ringh3_boromika_buiten", 71, BOVEN, 62, 100.0, None, 6, 6),
    ("merrie", "ringh3_merrie_buiten", 67, BOVEN, 67, 20.0, None, 6, 6),
    ("pippguh", "ringh3_pippguh_buiten", 69, BOVEN, 67, 340.0, None, 6, 6),
]

# what the Java side needs to know (feature/ringh3/Plekken.java): name -> (x, y, z) or a box
PLEKKEN = {
    "MAAT": MAAT,
    "POORT_SCHRIFT": (37, BOVEN + 7, 62),              # the middle of the inscription over the west gate
    "POORT_BUITEN": (35, BOVEN, 62),                   # where you stand in front of the gate
    "PLEIN": (22, G, 53, 37, G + 10, 70),              # the west forecourt (arriving here starts the chapter)
    "HEFBOOMHAL": HEFBOOMHAL,
    "PUTKAMER": PUTKAMER,
    "PUT": (21, MIDDEL, 35),                           # the well (anchor of the scene with the bucket)
    "GIMGUH": (15, MIDDEL, 37),
    "ZUILENHAL": ZUILENHAL,
    "KLOOF": (18, 0, 3, 86, DIEP - 4, 25),             # whoever is in here has fallen
    "OOSTOEVER": (78, DIEP - 1, 4, 86, DIEP + 6, 24),
    "BRUG_ANKER": (71, DIEP, 14),                      # the middle of the bridge (anchor of the bridge scene)
    "BRUG_KAPOT": BRUG_KAPOT,
    "BRUG_RAND_WEST": (65, DIEP, 14),
    "BRUG_RAND_OOST": (73, DIEP, 14),
    "ROG_START": (11, DIEP, 14),                       # where the Barbecuerog comes out of the dark
    "ROG_EINDE": (43, DIEP, 14),                       # how far he follows: the edge of the hall
    "HAL_INGANG": (21, DIEP, 25),                      # where Gimguh's passage comes out
    "RUSTVUUR_HAL": (21, DIEP, 29),
    "RUSTVUUR_PUT": (25, MIDDEL, 38),
    "RUSTVUUR_PLEIN": (27, BOVEN, 59),
    "RUSTVUUR_OOST": (68, BOVEN, 59),
    "ARAGUH": (64, BOVEN, 62),
    "OOSTPLEIN": (60, G, 53, 75, G + 10, 70),
}


def _ruis(seed):
    rng = random.Random(seed)
    tabel = {}

    def f(x, z, schaal=6.0):
        def punt(i, j):
            if (i, j) not in tabel:
                tabel[(i, j)] = rng.random()
            return tabel[(i, j)]
        fx, fz = x / schaal, z / schaal
        i, j = math.floor(fx), math.floor(fz)
        u, v = fx - i, fz - j
        u, v = u * u * (3 - 2 * u), v * v * (3 - 2 * v)
        return (punt(i, j) * (1 - u) + punt(i + 1, j) * u) * (1 - v) + (punt(i, j + 1) * (1 - u) + punt(i + 1, j + 1) * u) * v
    return f


class Bouw:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure(MAAT)
        self.rng = random.Random(21301800)
        self.ruis = _ruis(21301801)
        self.binnen = set()          # air that lies inside the rock (it gets a shell)
        self.buiten = set()          # open air above the ground (no shell)
        self.vast = set()            # blocks the shell and later passes never replace

    # --- small tools ----------------------------------------------------------------------------------------------------------
    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def lucht(self, x0, y0, z0, x1, y1, z1, buiten=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.gat(x, y, z, buiten)

    def gat(self, x, y, z, buiten=False):
        if self.s.inside(x, y, z):
            self.s.blocks[(x, y, z)] = (AIR, {}, None)
            (self.buiten if buiten else self.binnen).add((x, y, z))

    def vul(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def steen(self, verweerd=0.25, goud=0.0):
        r = self.rng.random()
        if r < goud:
            return KAASADER
        return GEBARSTEN if r < goud + verweerd else STENEN

    def kamer(self, doos, vloer=None, wand=True, verweerd=0.2, goud=0.0):
        """A room: air, a floor under it, brick walls and a ceiling around it."""
        x0, y0, z0, x1, y1, z1 = doos
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1, z1 + 2):
                rand = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1)
                for y in range(y0 - 1, y1 + 2):
                    if rand or y in (y0 - 1, y1 + 1):
                        if wand and (x, y, z) not in self.binnen:
                            self.set(x, y, z, vloer(x, z) if (vloer and y == y0 - 1 and not rand) else self.steen(verweerd, goud))
                    else:
                        self.gat(x, y, z)

    def trap(self, x, y, z, facing, half="bottom", name=TRAP):
        self.set(x, y, z, name, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"})

    def plaat(self, x, y, z, boven=False, name=PLAAT):
        self.set(x, y, z, name, {"type": "top" if boven else "bottom", "waterlogged": "false"})

    def lamp(self, x, y, z, hangend=True, ziel=False):
        self.set(x, y, z, ZIELLAMP if ziel else LANTAARN, {"hanging": "true" if hangend else "false", "waterlogged": "false"})

    def ketting(self, x, y0, y1, z):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, KETTING, {"axis": "y", "waterlogged": "false"})

    def kroonluchter(self, x, y_plafond, z, lang=2, ziel=False):
        self.ketting(x, y_plafond - lang + 1, y_plafond, z)
        self.lamp(x, y_plafond - lang, z, True, ziel)

    def rune(self, x, y, z, teken):
        self.set(x, y, z, RUNE, {"teken": str(teken)})

    def bord(self, x, y, z, facing, regels):
        import sign_text
        B = self.h.Byte
        leeg = sign_text.messages("sign.guhs.ringh3", ["", "", "", ""])
        self.set(x, y, z, "minecraft:spruce_wall_sign", {"facing": facing, "waterlogged": "false"}, {
            "id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages("sign.guhs.ringh3", regels), "color": "black", "has_glowing_text": B(0)},
            "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}})

    def staand_bord(self, x, y, z, rotatie, regels):
        import sign_text
        B = self.h.Byte
        leeg = sign_text.messages("sign.guhs.ringh3", ["", "", "", ""])
        self.set(x, y, z, "minecraft:spruce_sign", {"rotation": str(rotatie), "waterlogged": "false"}, {
            "id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages("sign.guhs.ringh3", regels), "color": "black", "has_glowing_text": B(0)},
            "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}})

    def vat(self, x, y, z):
        self.set(x, y, z, "minecraft:barrel", {"facing": "up", "open": "false"})

    def zuil(self, cx, cz, y0, y1, breed=1):
        """A great pillar: a shaft of 3 x 3 (breed 1) with a wider foot and a flaring capital."""
        for y in range(y0, y1 + 1):
            for dx in range(-breed, breed + 1):
                for dz in range(-breed, breed + 1):
                    hoek = abs(dx) == breed and abs(dz) == breed
                    rand = abs(dx) == breed or abs(dz) == breed
                    band = (y - y0) % 5 == 4 or y >= y1 - 1
                    if hoek and breed:
                        name = BASALT if not band and y > y0 else KAASSTEEN if band else GEBEITELD
                        self.set(cx + dx, y, cz + dz, name, {"axis": "y"} if name == BASALT else None)
                    else:
                        self.set(cx + dx, y, cz + dz, KAASSTEEN if band and rand else self.steen(0.2))
        b = breed + 1
        for dx in range(-b, b + 1):
            for dz in range(-b, b + 1):
                if max(abs(dx), abs(dz)) == b:
                    # the foot: a ring of stairs; the capital: two flaring rings
                    if abs(dx) == b and abs(dz) == b:
                        self.plaat(cx + dx, y0, cz + dz)
                        self.plaat(cx + dx, y1 - 1, cz + dz, True)
                    else:
                        naar = "east" if dx == -b else "west" if dx == b else "south" if dz == -b else "north"
                        self.trap(cx + dx, y0, cz + dz, naar)
                        self.trap(cx + dx, y1 - 1, cz + dz, naar, "top")
                    self.set(cx + dx, y1, cz + dz, GEBEITELD if (dx + dz) % 2 == 0 else STENEN)

    # --- the rock on the cave floor and the two forecourts -------------------------------------------------------------------
    def rots(self):
        (cx, cz), (rx, rz) = ROTS_MIDDEN, ROTS_STRAAL
        for x in range(MAAT[0]):
            for z in range(MAAT[2]):
                # (no drum: the outline wanders in and out, the flanks slope down to the floor, a few crags stand on top)
                d = math.hypot((x - cx) / rx, (z - cz) / rz) * (1.0 + (0.5 - self.ruis(x + 131, z + 57, 7.0)) * 0.26)
                if d >= 1.0:
                    continue
                hoog = 4.5 + 11.0 * (1 - d ** 2.4) ** 0.7 + (self.ruis(x, z, 5.0) - 0.5) * 4.0 + (self.ruis(x, z, 2.2) - 0.5) * 1.6
                hoog += max(0.0, self.ruis(x + 40, z + 90, 3.4) - 0.62) * 9.0 * (1 - d)
                if d > 0.5:
                    t = (d - 0.5) / 0.5
                    hoog = max(1.0, hoog * (1 - 0.86 * t * t * (3 - 2 * t)))
                # the two sheer faces with the gates
                if x < 37 or x > 59:
                    if abs(z - 62) <= 7:
                        continue
                    hoog *= max(0.25, 1 - (min(abs(x - 37), abs(x - 59)) * 0.12))
                top = BOVEN + max(1, int(hoog))
                for y in range(G - 2, min(top, MAAT[1] - 1) + 1):
                    r = self.rng.random()
                    self.set(x, y, z, ZWART if r < 0.07 else BASALT_RUW if r < 0.10 and y > BOVEN else HOUTSKOOL,
                             {"axis": "y"} if 0.07 <= r < 0.10 and y > BOVEN else None)
        # the faces themselves: flat dressed rock, a little higher than the rock behind them
        for x, kant in ((37, -1), (59, 1)):
            for z in range(54, 71):
                hoog = 13.5 - abs(z - 62) * 0.5 - max(0, abs(z - 62) - 6) * 2.0 + (self.ruis(z, x, 3.0) - 0.5) * 1.5
                for y in range(G - 2, BOVEN + max(3, int(hoog)) + 1):
                    for diep in range(0, 3):
                        self.set(x - kant * diep, y, z, HOUTSKOOL if abs(z - 62) > 6 or y > BOVEN + 10 else self.steen(0.3))

    def gevel(self, x, kant, deur):
        """A gate in a sheer face at plane x; kant = -1: it looks west, +1: east. The door slab sits one block in."""
        binnen = x - kant
        # the doorway: a recess in the face, the slab (three wide, five high) behind it
        for z in range(61, 64):
            for y in range(BOVEN, BOVEN + 5):
                self.gat(x, y, z, buiten=True)
                self.set(binnen, y, z, GEPOLIJST)
        # ornaments on the slab: a star of cheese gold, two hammers, a threshold stone (the door is one template, DEUREN)
        self.set(binnen, BOVEN + 3, 62, GATENKAAS)
        self.set(binnen, BOVEN + 2, 61, GEBEITELD)
        self.set(binnen, BOVEN + 2, 63, GEBEITELD)
        self.set(binnen, BOVEN, 62, GEBEITELD)
        self.set(binnen, BOVEN + 4, 61, KAASSTEEN)
        self.set(binnen, BOVEN + 4, 63, KAASSTEEN)
        # the frame: two pale pillars with cheese-stone capitals
        for z in (60, 64):
            self.set(x, BOVEN, z, GEBEITELD)
            for y in range(BOVEN + 1, BOVEN + 5):
                self.set(x, y, z, BASALT, {"axis": "y"})
            self.set(x, BOVEN + 5, z, KAASSTEEN)
        # the arch: a band of runes over the door, a band of chiselled stone round it, a keystone of gold
        for dz in range(-5, 6):
            for y in range(BOVEN + 5, BOVEN + 11):
                r = math.hypot(dz, (y - (BOVEN + 4)) * 1.0)
                z = 62 + dz
                if 2.6 <= r < 3.7:
                    if deur == "west":
                        self.rune(x, y, z, NJEG)
                    else:
                        self.set(x, y, z, KAASSTEEN if (dz + y) % 2 else GEBEITELD)
                elif 3.7 <= r < 4.8:
                    self.set(x, y, z, GEBEITELD)
                elif r < 2.6:
                    self.set(x, y, z, ZWARTSTEEN)
        self.set(x, BOVEN + 5, 62, GATENKAAS)
        self.set(x, BOVEN + 9, 62, GATENKAAS)
        # two great buttresses of basalt with a fire on top, and two braziers on plinths beside the path
        for z in (56, 68):
            for y in range(BOVEN, BOVEN + 8):
                self.set(x, y, z, BASALT, {"axis": "y"})
                if y < BOVEN + 3:
                    self.set(x + kant, y, z, STENEN if y < BOVEN + 2 else GEBEITELD)
            self.set(x, BOVEN + 8, z, KAASSTEEN)
            self.set(x, BOVEN + 9, z, GLOEIKOOL)
        for z in (58, 66):
            self.set(x + kant, BOVEN, z, GEBEITELD)
            self.set(x + kant, BOVEN + 1, z, ROOKGAT)
            self.vast.add((x + kant, BOVEN + 1, z))

    def plein(self, cx, kant, vuur, bord):
        """A forecourt: weathered paving round a worn path to the gate, open air above it."""
        for x in range(cx - 9, cx + 10):
            for z in range(51, 73):
                d = math.hypot((x - cx) / 8.5, (z - 62) / 9.5)
                if d > 1.0 + (self.ruis(x, z, 3.0) - 0.5) * 0.25:
                    continue
                if (x, G + 1, z) in self.s.blocks and self.get(x, G + 1, z) != AIR and abs(z - 62) > 7:
                    continue   # (the foot of the rock)
                pad = abs(z - 62) <= 1
                r = self.rng.random()
                top = (STENEN if r < 0.6 else GEBARSTEN) if pad else (GEBARSTEN if r < 0.35 else STENEN if r < 0.55 else AS_AARDE if r < 0.8 else HOUTSKOOL)
                if d < 0.42 and not pad and (x + z) % 4 == 0:
                    top = GEBEITELD
                self.set(x, G, z, top)
                self.set(x, G - 1, z, HOUTSKOOL)
                koepel = BOVEN + int(3 + 9 * (1 - d * d))
                for y in range(BOVEN, min(koepel, MAAT[1] - 1) + 1):
                    if (x, y, z) not in self.s.blocks:
                        self.gat(x, y, z, buiten=True)
        # two dead worst trees: bare, black, leaning towards the path
        for z, s in ((57, 1), (67, -1)):
            x = cx + kant * 5
            for y in range(BOVEN, BOVEN + 5):
                self.set(x, y, z, WORST, {"axis": "y"})
            self.set(x, BOVEN + 4, z + s, WORST, {"axis": "z"})
            self.set(x, BOVEN + 5, z + s, WORST, {"axis": "y"})
            self.set(x + kant, BOVEN + 3, z, WORST, {"axis": "x"})
            self.set(x - kant, BOVEN + 4, z, WORST, {"axis": "x"})
            self.set(x - kant * 2, BOVEN + 4, z, WORST, {"axis": "x"})
            self.set(x - kant * 2, BOVEN + 3, z, KETTING, {"axis": "y", "waterlogged": "false"})
            self.lamp(x - kant * 2, BOVEN + 2, z, True, ziel=True)
        # the rest fire with two log benches and a pack
        vx, vz = vuur
        self.set(vx, BOVEN, vz, VUUR)
        for dx in (-1, 0, 1):
            self.set(vx + dx, BOVEN, vz - 2, SATE, {"axis": "x"})
        for dz in (-1, 0, 1):
            self.set(vx - kant * -2, BOVEN, vz + dz, SATE, {"axis": "z"})
        self.vat(vx - kant * 2, BOVEN, vz - 2)
        for z in (59, 65):
            px = cx - kant * 4
            if self.get(px, BOVEN, z) in (AIR, None):
                self.set(px, BOVEN, z, HEK)
                self.set(px, BOVEN + 1, z, HEK)
                self.lamp(px, BOVEN + 2, z, False)
        for z in (60, 64):
            px = cx + kant * 2
            if self.get(px, BOVEN, z) in (AIR, None):
                self.set(px, BOVEN, z, HEK)
                self.set(px, BOVEN + 1, z, HEK)
                self.lamp(px, BOVEN + 2, z, False)
        self.staand_bord(cx - kant * 6, BOVEN, 64, 4 if kant < 0 else 12, bord)

    # --- inside -------------------------------------------------------------------------------------------------------------------
    def poorthal(self, doos, west):
        x0, y0, z0, x1, y1, z1 = doos
        self.kamer(doos, vloer=lambda x, z: KAASTEGEL if z == 62 or x == (x0 + x1) // 2 else LEI if (x + z) % 2 == 0 else TEGEL)
        cx = (x0 + x1) // 2
        # a shallow vault: the middle of the ceiling one higher, with a chandelier
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                self.gat(x, y1 + 1, z)
                self.set(x, y1 + 2, z, STENEN)
        self.kroonluchter(cx, y1 + 1, 62, 2)
        # two lamps on plinths inside the gate, one on each side of the door
        lx = x0 + 1 if west else x1 - 1
        for z in (z0 + 1, z1 - 1):
            self.set(lx, y0, z, GEBEITELD)
            self.lamp(lx, y0 + 1, z, False)
        for x in (x0, x1):
            for z in (z0, z1):
                for y in range(y0, y1 + 1):
                    self.set(x, y, z, KAASSTEEN if y in (y0, y1) else BASALT, {"axis": "y"} if y not in (y0, y1) else None)
        for x in range(x0, x1 + 1):
            for z in (z0 - 1, z1 + 1):
                self.set(x, y1, z, KAASSTEEN)
        for z in range(z0, z1 + 1):
            for x in (x0 - 1, x1 + 1):
                self.set(x, y1, z, KAASSTEEN)
        # the doorway north to the stair (3 wide, 4 high)
        dx = 42 if west else 54
        for x in range(dx - 1, dx + 2):
            for y in range(y0, y0 + 4):
                self.gat(x, y, z0 - 1)
        for x in range(dx - 2, dx + 3):
            self.set(x, y0 + 4, z0 - 1, KAASSTEEN if x in (dx - 2, dx + 2) else GEBEITELD)
        # benches of the gate guard, a rack of old picks (fences), barrels
        kant = x1 if west else x0
        self.vat(kant, y0, z1)
        self.vat(kant, y0 + 1, z1)
        self.vat(kant, y0, z1 - 1)
        self.set(kant, y0, z0, KNABBELS)
        for z in (z0 + 1, z1 - 1):
            self.trap(x0 + (5 if west else 1), y0, z, "west" if west else "east", name=TRAP)

    def westtrap(self):
        """From the west gate hall north and down to the lever hall: ten steps under a stepped ceiling."""
        for z in range(57, 41, -1):
            stap = 30 - (57 - z) if z >= 48 else MIDDEL - 1          # the block you walk on
            for x in range(41, 44):
                if z >= 48:
                    self.trap(x, stap, z, "south")
                    self.set(x, stap - 1, z, STENEN)
                else:
                    self.set(x, stap, z, GEPOLIJST if x == 42 else self.steen(0.3))
                for y in range(stap + 1, stap + 5):
                    self.gat(x, y, z)
                self.set(x, stap + 5, z, self.steen(0.2))
            for x in (40, 44):
                for y in range(stap, stap + 5):
                    self.set(x, y, z, KAASSTEEN if y == stap + 2 else self.steen(0.25, 0.03))
            if z % 4 == 1:
                self.lamp(42, stap + 4, z, True)

    def hefboomhal(self):
        x0, y0, z0, x1, y1, z1 = HEFBOOMHAL

        def vloer(x, z):
            if x in (x0, x1) or z in (z0, z1):
                return LEI
            if (x - x0) % 4 == 0 or (z - z0) % 4 == 2:
                return KAASTEGEL
            return TEGEL if self.rng.random() < 0.8 else GEBARSTEN
        self.kamer(HEFBOOMHAL, vloer=vloer, goud=0.03)
        # a beamed ceiling: beams of grill iron across the hall
        for x in range(x0 + 2, x1, 4):
            for z in range(z0, z1 + 1):
                self.set(x, y1, z, ROOSTER)
        # pilasters along the long walls
        for x in range(x0 + 2, x1, 4):
            for z in (z0, z1):
                if z == z0 and x in HENDEL_X:
                    continue
                for y in range(y0, y1):
                    self.set(x, y, z, BASALT if y0 < y < y1 - 1 else KAASSTEEN, {"axis": "y"} if y0 < y < y1 - 1 else None)
        # a frieze of cheese stone round the hall under the beams
        for x in range(x0 - 1, x1 + 2):
            for z in (z0 - 1, z1 + 1):
                if (x, y1, z) not in self.binnen:
                    self.set(x, y1, z, KAASSTEEN if x % 2 else GEBEITELD)
        for z in range(z0, z1 + 1):
            for x in (x0 - 1, x1 + 1):
                if (x, y1, z) not in self.binnen:
                    self.set(x, y1, z, KAASSTEEN if z % 2 else GEBEITELD)
        # the four levers on the north wall, each in a niche under its emblem
        for nr, x in enumerate(HENDEL_X):
            for y in range(y0, y0 + 4):
                self.gat(x, y, z0 - 1)                                    # the niche (one deep)
                self.set(x, y, z0 - 2, GEBEITELD if y == y0 + 3 else ZWARTSTEEN)
            self.set(x, y0 + 3, z0 - 1, AIR)
            self.rune(x, y0 + 3, z0 - 2, HENDEL_TEKENS[nr])
            self.set(x, y0 + 1, z0 - 1, HENDEL, {"facing": "south", "nr": str(nr), "om": "false"})
            self.set(x, y0, z0 - 1, GEBEITELD)
            self.vast.add((x, y0 + 1, z0 - 1))
            for dx in (-1, 1):
                for y in range(y0, y0 + 4):
                    self.set(x + dx, y, z0 - 1, GATENKAAS if y == y0 + 3 else ZWARTSTEEN)
            self.set(x, y0 + 4, z0 - 1, GATENKAAS)
        # the stele with the rhyme in the middle, on a step
        sx, sz = 42, 35
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if dx or dz:
                    naar = "east" if dx == -1 else "west" if dx == 1 else "south" if dz == -1 else "north"
                    if dx and dz:
                        self.plaat(sx + dx, y0, sz + dz)
                    else:
                        self.trap(sx + dx, y0, sz + dz, naar)
        self.set(sx, y0, sz, GEBEITELD)
        self.set(sx, y0 + 1, sz, ZWARTSTEEN)
        self.set(sx, y0 + 2, sz, VERGULD)
        from features import ring_h3_tekst as tekst
        self.bord(sx, y0 + 1, sz + 1, "south", tekst.BORDEN["stele_1"])
        self.bord(sx, y0 + 1, sz - 1, "north", tekst.BORDEN["stele_2"])
        self.bord(sx - 1, y0 + 1, sz, "west", tekst.BORDEN["stele_3"])
        self.bord(sx + 1, y0 + 1, sz, "east", tekst.BORDEN["stele_3"])
        # chandeliers, a knabbel hoard, barrels and a cart of old tools along the south wall
        for x in (35, 39, 43, 47):
            for z in (31, 38):
                self.kroonluchter(x, y1, z, 2 if x in (39, 43) else 1)
        for (x, z) in ((36, z1), (37, z1), (36, z1 - 1), (48, z1), (49, z1)):
            self.vat(x, y0, z)
        self.vat(36, y0 + 1, z1)
        for (x, y, z) in ((x1, y0, z1), (x1 - 1, y0, z1), (x1, y0 + 1, z1), (x1, y0, z1 - 1)):
            self.set(x, y, z, KNABBELS)
        # the south doorway (from the stair) and the west corridor with the portcullis
        for x in range(41, 44):
            for y in range(y0, y0 + 4):
                self.gat(x, y, z1 + 1)
        cx0, cx1 = 29, 33
        for x in range(cx0, cx1 + 1):
            for z in range(34, 37):
                self.set(x, y0 - 1, z, TEGEL)
                for y in range(y0, y0 + 4):
                    self.gat(x, y, z)
                self.set(x, y0 + 4, z, GEBEITELD if x in (30, 32) else STENEN)
            for z in (33, 37):
                for y in range(y0 - 1, y0 + 5):
                    self.set(x, y, z, self.steen(0.2))
        dx0, dy0, dz0, dx1, dy1, dz1 = DEUREN["valhek"]
        self.vul(dx0, dy0, dz0, dx1, dy1, dz1, TRALIES)
        for z in (33, 37):
            for y in range(y0, y0 + 4):
                self.set(31, y, z, PILAAR, {"axis": "y"})

    def putkamer(self):
        x0, y0, z0, x1, y1, z1 = PUTKAMER
        px, _, pz = PLEKKEN["PUT"]

        def vloer(x, z):
            d = math.hypot(x - px, z - pz)
            if 3.4 < d < 4.6:
                return KAASTEGEL
            return LEI if d < 3.4 else TEGEL if self.rng.random() < 0.75 else GEBARSTEN
        self.kamer(PUTKAMER, vloer=vloer, verweerd=0.3, goud=0.02)
        # a domed ceiling over the well
        for x in range(x0 + 3, x1 - 2):
            for z in range(z0 + 3, z1 - 2):
                d = math.hypot(x - px, z - pz)
                if d < 5.2:
                    self.gat(x, y1 + 1, z)
                    self.set(x, y1 + 2, z, STENEN)
                if d < 3.2:
                    self.gat(x, y1 + 2, z)
                    self.set(x, y1 + 3, z, GEBEITELD if d < 1 else STENEN)
        # the well: a ring wall, a shaft down to the glow, a grate so nobody follows the bucket, and the windlass
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                d = max(abs(dx), abs(dz))
                if d == 2 and abs(dx) + abs(dz) < 4:
                    self.set(px + dx, y0, pz + dz, GEBEITELD if (dx + dz) % 2 == 0 else KAASSTEEN)
                    self.vast.add((px + dx, y0, pz + dz))
                elif d <= 1:
                    for y in range(4, y0 + 1):
                        self.gat(px + dx, y, pz + dz)
                    self.set(px + dx, y0 - 1, pz + dz, TRALIES)
                    self.set(px + dx, 3, pz + dz, GLOEIKOOL)
                    self.vast.add((px + dx, y0 - 1, pz + dz))
        for dz in (-2, 2):
            for y in range(y0 + 1, y0 + 4):
                self.set(px, y, pz + dz, HEK)
        for dz in range(-2, 3):
            self.set(px, y0 + 4, pz + dz, SATE, {"axis": "z"})
        self.ketting(px, y0 + 2, y0 + 3, pz)
        self.lamp(px, y0 + 3, pz - 1, True)
        self.lamp(px, y0 + 3, pz + 1, True)
        # Durguh's tomb against the north wall: a sarcophagus, his name, his skull, an offering of his favourite
        from features import ring_h3_tekst as tekst
        for x in range(19, 24):
            self.set(x, y0, z0, ZWARTSTEEN)
            self.plaat(x, y0 + 1, z0, name="minecraft:polished_blackstone_slab")
        self.set(21, y0 + 1, z0, VERGULD)
        self.set(21, y0 + 2, z0, VERKOOLD, {"axis": "y"})
        self.bord(21, y0, z0 + 1, "south", tekst.BORDEN["tombe_1"])
        self.bord(20, y0, z0 + 1, "south", tekst.BORDEN["tombe_2"])
        self.set(18, y0, z0, KNABBELS)
        self.set(24, y0, z0, KAASADER)
        self.rune(21, y0 + 3, z0 - 1, KAAS)
        # the caved-in corridor in the south wall
        for x in range(19, 24):
            for y in range(y0, y0 + 4):
                if abs(x - 21) + (y - y0) <= 3:
                    self.set(x, y, z1 + 1, self.rng.choice(["minecraft:cobbled_deepslate", GEBARSTEN, "minecraft:gravel", HOUTSKOOL]))
        for (x, z) in ((20, z1), (21, z1), (22, z1), (21, z1 - 1)):
            self.set(x, y0, z, self.rng.choice(["minecraft:cobbled_deepslate", "minecraft:gravel"]))
        self.set(21, y0 + 1, z1, "minecraft:cobbled_deepslate")
        self.bord(23, y0 + 1, z1, "north", tekst.BORDEN["ingestort"])
        self.set(23, y0, z1, STENEN)
        # the rest fire in the south-east corner, a bench, a lantern
        vx, _, vz = PLEKKEN["RUSTVUUR_PUT"]
        self.set(vx, y0, vz, VUUR)
        for dx in (-1, 0, 1):
            self.set(vx + dx, y0, vz + 2, SATE, {"axis": "x"})
        self.vat(x1 - 1, y0, z1)
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            for y in range(y0, y1 + 1):
                self.set(x, y, z, KAASSTEEN if y in (y0, y1) else BASALT, {"axis": "y"} if y not in (y0, y1) else None)
        for (x, z) in ((x0 + 2, z0 + 2), (x1 - 2, z0 + 2), (x1 - 2, z1 - 2), (x0 + 2, z1 - 2), (x0 + 2, pz), (x1 - 2, pz), (px, z0 + 2), (px, z1 - 2)):
            self.kroonluchter(x, y1, z, 1)
        # the west wall: six rune stones and, between them, the door nobody sees (the same bricks as the wall)
        for z, teken in zip(RUNE_Z, RUNE_TEKENS):
            self.rune(x0 - 1, y0 + 1, z, teken)
            self.set(x0 - 1, y0 + 2, z, GEBEITELD)
            self.vast.add((x0 - 1, y0 + 1, z))
        dx0, dy0, dz0, dx1, dy1, dz1 = DEUREN["geheim"]
        self.vul(dx0, dy0, dz0, dx1, dy1, dz1, STENEN)
        self.set(dx0, dy0 + 1, dz0, GEBARSTEN)
        # the doorway east (from the lever hall)
        for z in range(34, 37):
            for y in range(y0, y0 + 4):
                self.gat(x1 + 1, y, z)

    def geheime_gang(self):
        """Behind the door: two steps of passage, a spiral stair down round a pillar, a low tunnel to the last rest fire."""
        y0 = MIDDEL
        for x in (11, 12):
            for z in (35, 36):
                self.set(x, y0 - 1, z, STENEN)
                for y in range(y0, y0 + 3):
                    self.gat(x, y, z)
        cx, cz = SPIRAAL_MIDDEN
        ring = [(10, 35), (10, 36), (10, 37), (9, 37), (8, 37), (7, 37), (6, 37), (6, 36), (6, 35), (6, 34), (6, 33), (7, 33), (8, 33)]
        for x in range(cx - 1, cx + 2):
            for z in range(cz - 1, cz + 2):
                for y in range(DIEP - 1, y0 + 4):
                    self.set(x, y, z, GEBEITELD if (y % 4 == 0) else STENEN)
        loop = y0
        for i, (x, z) in enumerate(ring):
            if i < 2:
                blok = y0 - 1
                self.set(x, blok, z, STENEN)
            elif i < 11:
                blok = y0 - (i - 1)
                px, pz = ring[i - 1]
                naar = "east" if px > x else "west" if px < x else "south" if pz > z else "north"
                self.trap(x, blok, z, naar)
                self.set(x, blok - 1, z, STENEN)
            else:
                blok = DIEP - 1
                self.set(x, blok, z, STENEN)
            for y in range(blok + 1, blok + 5):
                self.gat(x, y, z)
            loop = blok + 1
            if i in (3, 7, 11):
                # a lantern in a little niche in the outer wall
                ox, oz = (x + (1 if x > cx else -1 if x < cx else 0), z + (1 if z > cz else -1 if z < cz else 0))
                if abs(ox - cx) == 3 or abs(oz - cz) == 3:
                    self.gat(ox if abs(ox - cx) == 3 else x, blok + 2, oz if abs(oz - cz) == 3 else z)
                    self.lamp(ox if abs(ox - cx) == 3 else x, blok + 2, oz if abs(oz - cz) == 3 else z, False)
        assert loop == DIEP, loop
        # the tunnel: north out of the stairwell, then east under the well room to the rest niche
        for z in range(29, 33):
            for x in (8,):
                self.set(x, DIEP - 1, z, STENEN)
                for y in range(DIEP, DIEP + 3):
                    self.gat(x, y, z)
        for x in range(8, 18):
            for z in (29, 30):
                self.set(x, DIEP - 1, z, STENEN if self.rng.random() < 0.6 else GEBARSTEN)
                for y in range(DIEP, DIEP + 3):
                    self.gat(x, y, z)
            if x % 4 == 0:
                self.gat(x, DIEP + 1, 28)
                self.lamp(x, DIEP + 1, 28, False, ziel=True)
        nx0, ny0, nz0, nx1, ny1, nz1 = RUSTNIS
        self.kamer(RUSTNIS, vloer=lambda x, z: TEGEL if (x + z) % 2 else STENEN, verweerd=0.3)
        for z in (29, 30):
            for y in range(DIEP, DIEP + 3):
                self.gat(17, y, z)
        vx, _, vz = PLEKKEN["RUSTVUUR_HAL"]
        self.set(vx + 2, DIEP, vz, VUUR)
        self.set(vx + 3, DIEP, vz - 1, SATE, {"axis": "z"})
        self.set(vx + 3, DIEP, vz + 1, SATE, {"axis": "z"})
        self.lamp(nx0, ny1, nz1, True)
        # the mouth into the great hall
        for x in range(20, 23):
            for y in range(DIEP, DIEP + 4):
                self.gat(x, y, 26)
        for x in (19, 23):
            self.set(x, DIEP + 3, 26, GEBEITELD)

    def zuilenhal(self):
        x0, y0, z0, x1, y1, z1 = ZUILENHAL
        zm = 14
        # the nave is high (y1), the two aisles are lower; the ceiling steps up towards the middle
        for x in range(x0, KLOOF[3] + 1 + (OOSTOEVER[3] - KLOOF[3])):
            for z in range(z0, z1 + 1):
                dz = abs(z - zm)
                plafond = y1 if dz <= 4 else y1 - 1 if dz <= 5 else y1 - 2 if dz <= 6 else y1 - 4 if dz <= 8 else y1 - 6
                if x > x1:
                    # over the chasm the roof is living rock: ragged, with teeth hanging down
                    plafond = y1 - int(self.ruis(x, z, 4.0) * 3.2) - (2 if dz > 9 else 0)
                bodem = y0 if (x <= x1 or x >= OOSTOEVER[0]) else KLOOF[1]
                for y in range(bodem, plafond + 1):
                    self.gat(x, y, z)
                if x <= x1 or x >= OOSTOEVER[0]:
                    naaf = dz <= 1
                    self.set(x, y0 - 1, z, LEI if naaf and (x % 4) else KAASTEGEL if naaf or dz == 4 else
                             (TEGEL if self.rng.random() < 0.75 else GEBARSTEN))
                    self.set(x, y0 - 2, z, HOUTSKOOL)
                    self.set(x, plafond + 1, z, self.steen(0.2) if x <= x1 else HOUTSKOOL)
                else:
                    self.set(x, plafond + 1, z, HOUTSKOOL)
                    if self.rng.random() < 0.06 and plafond > y1 - 3:
                        for y in range(plafond - self.rng.randint(0, 2), plafond + 1):
                            self.set(x, y, z, BASALT_RUW, {"axis": "y"})
        # the walls of the hall: dressed stone with pilasters; of the chasm: rough rock
        for x in range(x0 - 1, OOSTOEVER[3] + 2):
            for z in (z0 - 1, z1 + 1):
                for y in range(KLOOF[1] - 1 if KLOOF[0] <= x <= KLOOF[3] else y0 - 1, y1 + 2):
                    if (x, y, z) not in self.binnen:
                        ruw = KLOOF[0] <= x <= KLOOF[3] or y < y0
                        self.set(x, y, z, HOUTSKOOL if ruw else self.steen(0.2, 0.03))
        for z in range(z0 - 1, z1 + 2):
            for x in (x0 - 1, OOSTOEVER[3] + 1):
                for y in range(y0 - 1, y1 + 2):
                    if (x, y, z) not in self.binnen:
                        self.set(x, y, z, self.steen(0.2))
        for x in range(x0 + 2, x1, 4):
            for z in (z0, z1):
                for y in range(y0, y1 - 6):
                    self.set(x, y, z, BASALT if y > y0 else GEBEITELD, {"axis": "y"} if y > y0 else None)
                self.set(x, y1 - 6, z, KAASSTEEN)
        # the two rows of great pillars
        for cx in (23, 31, 39):
            for cz in (zm - 6, zm + 6):
                self.zuil(cx, cz, y0, y1 - 2)
                # an ember bowl on the nave side of the foot: the only light the hall has
                bz = cz + (2 if cz < zm else -2)
                if cx != 23:
                    self.set(cx + 2, y0, bz, GEBEITELD)
                    self.set(cx + 2, y0 + 1, bz, GLOEIKOOL)
                    self.vast.add((cx + 2, y0, bz))
                    self.vast.add((cx + 2, y0 + 1, bz))
                else:
                    self.set(cx + 2, y0, bz, ROOKGAT)
                    self.vast.add((cx + 2, y0, bz))
        # arches between the pillars across the nave (ribs of the vault)
        for cx in (23, 31, 39):
            for dz in range(-4, 5):
                hoog = y1 - (0 if abs(dz) <= 2 else 1 if abs(dz) == 3 else 2)
                self.set(cx, hoog, zm + dz, KAASSTEEN if dz == 0 else GEBEITELD)
                if abs(dz) == 4:
                    self.set(cx, hoog - 1, zm + dz, GEBEITELD)
        # the treasure of the dwarf-guhs in the aisles: heaps of knabbels, veins of cheese gold, barrels, a broken cart
        for (x, z, n) in ((20, 4, 3), (27, 4, 2), (35, 3, 4), (42, 4, 2), (26, 24, 3), (34, 25, 2), (43, 24, 3)):
            for i in range(n):
                for j in range(n - i):
                    xx, zz = x + j, z + (i if z < zm else -i)
                    if self.get(xx, y0, zz) in (AIR, None):
                        self.set(xx, y0, zz, KNABBELS if (i + j) % 3 else KAASADER)
                        if i == 0 and j == 0 and n > 2:
                            self.set(xx, y0 + 1, zz, KNABBELS)
        for (x, z) in ((29, 3), (30, 3), (38, 25), (39, 25), (45, 3)):
            self.vat(x, y0, z)
        # cracks in the floor towards the chasm, and a scorch trail down the nave from the Diepe Poort
        for x in range(x0, x1 + 1):
            if self.rng.random() < 0.5:
                self.set(x, y0 - 1, zm + self.rng.choice((-2, 2, -3, 3)), GEBARSTEN)
        for x in range(x0, x0 + 9):
            for z in range(zm - 2, zm + 3):
                if self.rng.random() < 0.55 - (x - x0) * 0.05:
                    self.set(x, y0 - 1, z, self.rng.choice([ZWART, ZWART, BASALT_RUW, GRILLKOOL]), None)
        # the edge of the hall floor at the chasm: broken, with two stumps of a fallen gate
        for z in range(z0, z1 + 1):
            if abs(z - zm) > 2 and self.rng.random() < 0.5:
                self.gat(x1, y0 - 1, z)
                self.gat(x1, y0 - 2, z)
        for z in (zm - 3, zm + 3):
            for y in range(y0, y0 + 3 + (z > zm)):
                self.set(x1, y, z, self.steen(0.5))
            self.set(x1, y0 - 1, z, STENEN)
            self.set(x1, y0 - 2, z, HOUTSKOOL)

    def diepe_poort(self):
        """The cave in the west wall where the Barbecuerog sleeps: a black, scorched mouth, 13 high, with a split arch."""
        x0, y0, z0, x1, y1, z1 = DIEPE_POORT
        cx, cz = (x0 + x1) / 2 + 2, (z0 + z1) / 2
        for x in range(x0, x1 + 2):
            for z in range(z0, z1 + 1):
                for y in range(y0, y1 + 1):
                    d = math.sqrt(((x - cx) / 8.5) ** 2 + ((y - y0) / 14.0) ** 2 + ((z - cz) / 6.8) ** 2)
                    if d < 1.0 + (self.ruis(x + y, z, 3.0) - 0.5) * 0.18 or (x >= x1 and abs(z - cz) <= 5 and y - y0 <= 11 - abs(z - cz) * 0.9):
                        self.gat(x, y, z)
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1, z1 + 2):
                if (x, y0, z) in self.binnen:
                    self.set(x, y0 - 1, z, self.rng.choice([ZWART, ZWART, BASALT_RUW, GRILLKOOL, HOUTSKOOL]))
                    self.set(x, y0 - 2, z, HOUTSKOOL)
        # the arch in the hall's wall: black stone, cracked
        for z in range(z0 - 1, z1 + 2):
            for y in range(y0, y1 + 3):
                if (x1 + 1, y, z) not in self.binnen and any((x1 + 1, y + dy, z + dz) in self.binnen for dy in (-1, 0, 1) for dz in (-1, 0, 1)):
                    self.set(x1 + 1, y, z, ZWARTSTEEN if self.rng.random() < 0.7 else "minecraft:cracked_polished_blackstone_bricks")
        # embers deep inside: the first thing anybody sees of him is this glow
        for (x, z) in ((x0 + 2, int(cz) - 3), (x0 + 3, int(cz) + 4), (x0 + 1, int(cz))):
            if (x, y0, z) in self.binnen:
                self.set(x, y0 - 1, z, GLOEIKOOL)

    def kloof(self):
        x0, y0, z0, x1, y1, z1 = KLOOF
        # the bed of coals at the bottom, heaped against the walls: rivers of glowing coal between banks and islands of black
        # slag (broad shapes from a noise of their own: a coin toss per block made a checkerboard of it, seen from the bridge)
        gloed = _ruis(21301809)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                r = self.rng.random()
                heet = gloed(x, z, 5.5) * 0.7 + gloed(x + 300, z + 170, 2.4) * 0.3
                self.set(x, y0 - 1, z, GLOEIKOOL if heet > 0.5 else GRILLKOOL if heet > 0.44 else BASALT_RUW if r > 0.9 else ZWART,
                         {"axis": "y"} if heet <= 0.44 and r > 0.9 else None)
                self.set(x, y0 - 2, z, HOUTSKOOL)
                rand = min(z - z0, z1 - z, x - x0, x1 - x)
                hoop = int(max(0, 3 - rand) * self.ruis(x, z, 2.5) * 1.6) + (1 if r > 0.93 else 0)
                for y in range(y0, y0 + hoop):
                    kop = self.rng.random()
                    self.set(x, y, z, GLOEIKOOL if heet > 0.47 and (y == y0 or kop < 0.6) else GRILLKOOL)
                if hoop == 0 and heet < 0.3:
                    self.set(x, y0, z, ZWART if heet > 0.22 or r < 0.5 else HOUTSKOOL)      # the slag stands a block proud of the fire
                elif hoop == 0 and r < 0.1 and heet > 0.5:
                    self.set(x, y0, z, SMEUL)
        # the walls bulge and break: ledges of rock under the hall floor and the east bank
        for z in range(z0, z1 + 1):
            for kant, rand in ((1, x0), (-1, x1)):
                for y in range(y0, DIEP - 1):
                    uit = int((self.ruis(z, y + kant * 40, 3.5)) * 3.4 * (0.4 + 0.6 * (1 - (y - y0) / (DIEP - y0))))
                    for i in range(uit):
                        self.set(rand + kant * i, y, z, HOUTSKOOL if self.rng.random() < 0.9 else ZWART)
        # the Brokkelpad: pillars that stand (2 x 2, down to the coals) and runs of crumbling stone between them
        dek = DIEP - 1
        hoeken = PAD_HOEKEN
        for (hx, hz) in hoeken:
            for x in (hx, hx + 1):
                for z in (hz, hz + 1):
                    for y in range(y0 - 1, dek + 1):
                        breed = 1 if y < y0 + 3 else 0
                        for ddx in range(-breed, breed + 1):
                            for ddz in range(-breed, breed + 1):
                                if (ddx == 0 and ddz == 0) or self.rng.random() < 0.6:
                                    self.set(x + ddx, y, z + ddz, STENEN if (ddx == 0 and ddz == 0) and y > dek - 4 else HOUTSKOOL)
                    self.set(x, dek, z, GEBEITELD)
                    self.vast.add((x, dek, z))
            # a hanging lamp post on every pillar so the path can be read
            self.set(hx, dek + 1, hz, MUUR, {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
            self.lamp(hx, dek + 2, hz, False)
            self.vast.add((hx, dek + 1, hz))
        cellen = []
        punten = [(46, 13)] + hoeken + [PAD_EINDE]
        for (ax, az), (bx, bz) in zip(punten, punten[1:]):
            if az == bz:
                for x in range(min(ax, bx), max(ax, bx) + 2):
                    for z in (az, az + 1):
                        cellen.append((x, z))
            else:
                for z in range(min(az, bz), max(az, bz) + 2):
                    for x in (ax, ax + 1):
                        cellen.append((x, z))
        self.brokkel = []
        for (x, z) in dict.fromkeys(cellen):
            if x < x0 or (x, dek, z) in self.vast or BRUGGENHOOFD[0] <= x <= BRUGGENHOOFD[3] and BRUGGENHOOFD[2] <= z <= BRUGGENHOOFD[5]:
                continue
            self.set(x, dek, z, BROKKEL, {"staat": "0"})
            self.vast.add((x, dek, z))
            self.brokkel.append((x, dek, z))
        # the bridgehead: a tower of rock out of the coals, a flat top, two broken posts where the bridge begins
        bx0, _, bz0, bx1, _, bz1 = BRUGGENHOOFD
        for x in range(bx0, bx1 + 1):
            for z in range(bz0, bz1 + 1):
                for y in range(y0 - 1, dek + 1):
                    inkrimp = 0 if y > dek - 3 else 1 if y > y0 + 3 else 0
                    if bx0 + inkrimp <= x <= bx1 - inkrimp and bz0 + inkrimp <= z <= bz1 - inkrimp:
                        self.set(x, y, z, (KAASTEGEL if z == 14 else LEI if abs(z - 14) <= 1 else TEGEL) if y == dek else self.steen(0.3) if y > dek - 3 else HOUTSKOOL)
        for z in (12, 16):
            self.set(bx1, dek + 1, z, GEBEITELD)
            self.set(bx1, dek + 2, z, GLOEIKOOL)
            self.vast.add((bx1, dek + 2, z))
        # the Brug van Knabbel-dum: one narrow span, three wide, no railing, an arch underneath
        gx0, gy, gz0, gx1, _, gz1 = BRUG
        lang = gx1 - gx0
        for x in range(gx0, gx1 + 1):
            t = (x - gx0) / lang
            dik = 1 + int(round(2.2 * abs(2 * t - 1) ** 1.6))          # thick at both ends, one block in the middle
            for z in range(gz0, gz1 + 1):
                self.set(x, gy, z, KAASTEGEL if z == 14 else STENEN if (x % 4) else GEBEITELD)
                for i in range(1, dik):
                    self.set(x, gy - i, z, STENEN if z != 14 else GEBARSTEN if i == dik - 1 else STENEN)
        for z in range(gz0, gz1 + 1):
            self.trap(gx0 + 1, gy - 2, z, "west", "top")
            self.trap(gx1 - 1, gy - 2, z, "east", "top")
        # the east bank: its ragged lip, two great lamps, the door to the long stair
        ox0 = OOSTOEVER[0]
        for z in range(z0, z1 + 1):
            if abs(z - 14) > 2 and self.rng.random() < 0.45:
                self.gat(ox0, DIEP - 1, z)
                self.gat(ox0, DIEP - 2, z)
        for z in (10, 18):
            self.zuil(ox0 + 3, z, DIEP, DIEP + 6, breed=0)
            self.set(ox0 + 3, DIEP + 7, z, GLOEIKOOL)
        for z in (5, 23):
            self.set(ox0 + 6, DIEP, z, GEBEITELD)
            self.set(ox0 + 6, DIEP + 1, z, GLOEIKOOL)
        from features import ring_h3_tekst as tekst
        self.staand_bord(ox0 + 1, DIEP, 17, 4, tekst.BORDEN["brug"])

    def langetrap(self):
        """From the east bank south and up (nine steps), a landing, west and up (three), a long corridor, south, and seven steps
        up into the east gate hall."""
        for z in range(26, 42):
            stap = DIEP + (z - 27) if 27 <= z <= 35 else DIEP - 1 if z < 27 else MIDDEL - 1
            for x in range(82, 85):
                if 27 <= z <= 35:
                    self.trap(x, stap, z, "south")
                    self.set(x, stap - 1, z, STENEN)
                else:
                    self.set(x, stap, z, GEPOLIJST if x == 83 else self.steen(0.3))
                for y in range(stap + 1, stap + 5):
                    self.gat(x, y, z)
            for x in (81, 85):
                for y in range(stap, stap + 5):
                    if (x, y, z) not in self.binnen:
                        self.set(x, y, z, KAASSTEEN if y == stap + 2 else self.steen(0.25, 0.03))
            if z % 4 == 2:
                self.lamp(83, stap + 4, z, True)
        for x in range(53, 82):
            stap = MIDDEL + (79 - x) if 77 <= x <= 79 else MIDDEL - 1 if x > 79 else MIDDEL + 2
            for z in range(39, 42):
                if 77 <= x <= 79:
                    self.trap(x, stap, z, "west")
                    self.set(x, stap - 1, z, STENEN)
                else:
                    self.set(x, stap, z, GEPOLIJST if z == 40 and x % 3 else self.steen(0.3))
                for y in range(stap + 1, stap + (5 if x > 76 else 4)):
                    self.gat(x, y, z)
            for z in (38, 42):
                for y in range(stap, stap + 4):
                    if (x, y, z) not in self.binnen and not (53 <= x <= 55 and z == 42) and not (82 <= x <= 84 and z == 38):
                        self.set(x, y, z, KAASSTEEN if y == stap + 2 else self.steen(0.25, 0.03))
            if x % 6 == 0 and x < 77:
                self.lamp(x, stap + 3, 40, True)
        for z in range(42, 59):
            stap = MIDDEL + 3 + (z - 52) if z >= 52 else MIDDEL + 2
            for x in range(53, 56):
                if z >= 52:
                    self.trap(x, stap, z, "south")
                    self.set(x, stap - 1, z, STENEN)
                else:
                    self.set(x, stap, z, GEPOLIJST if x == 54 else self.steen(0.3))
                for y in range(stap + 1, stap + 5 if z >= 52 else stap + 4):
                    self.gat(x, y, z)
            for x in (52, 56):
                for y in range(stap, stap + 4):
                    if (x, y, z) not in self.binnen:
                        self.set(x, y, z, KAASSTEEN if y == stap + 2 else self.steen(0.25, 0.03))
            if z % 5 == 0:
                self.lamp(54, stap + (4 if z >= 52 else 3), z, True)

    # --- finishing ------------------------------------------------------------------------------------------------------------------
    def schil(self):
        """Two blocks of rock round every bit of air inside: the inner one dressed stone (where a room didn't say otherwise), the
        outer one plain houtskoolsteen."""
        b = self.s.blocks
        for laag, name in ((1, None), (2, HOUTSKOOL)):
            nieuw = {}
            bron = self.binnen if laag == 1 else self._laag1
            for (x, y, z) in bron:
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        for dz in (-1, 0, 1):
                            q = (x + dx, y + dy, z + dz)
                            if q not in b and q not in nieuw and self.s.inside(*q):
                                nieuw[q] = name or (self.steen(0.25, 0.02) if q[1] > G - 26 else HOUTSKOOL)
            for q, n in nieuw.items():
                b[q] = (n, {}, None)
            if laag == 1:
                self._laag1 = set(nieuw) | {q for q in b if b[q][0] != AIR and q[1] <= G}

    def verbind(self):
        """Fences, walls and bars get their arms (a template is placed without neighbour updates)."""
        b = self.s.blocks
        vol = lambda q: q in b and b[q][0] not in (AIR, KETTING, LANTAARN, ZIELLAMP, SMEUL, VUUR, HENDEL, BROKKEL) and "sign" not in b[q][0] \
            and not b[q][0].endswith(("_plaat", "_slab", "_trap", "_stairs", "_carpet"))
        for (x, y, z), (name, props, nbt) in list(b.items()):
            soort = "hek" if name in (HEK, TRALIES) else "muur" if name == MUUR else None
            if not soort:
                continue
            p = dict(props)
            for richting, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
                q = (x + dx, y, z + dz)
                zelfde = q in b and b[q][0] == name
                aan = zelfde or (vol(q) and b[q][0] not in (HEK, TRALIES, MUUR))
                p[richting] = ("true" if aan else "false") if soort == "hek" else ("low" if aan else "none")
            if soort == "muur":
                p["up"] = "true"
            p["waterlogged"] = "false"
            b[(x, y, z)] = (name, p, nbt)

    def midden(self):
        cx, cz = ROTS_MIDDEN
        self.s.set(cx, 0, cz, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": MIDDEN, "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": HOUTSKOOL, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})

    def cast(self):
        from features import ring
        for kind, id, x, y, z, yaw, plek, van, tot in CAST:
            self.s.entity(x + 0.5, float(y), z + 0.5, ring.cast(self.h, kind, id, "ring_h3", van, tot, plek=plek, yaw=yaw))


def bouw(h):
    b = Bouw(h)
    from features import ring_h3_tekst as tekst
    b.rots()
    b.plein(29, -1, PLEKKEN["RUSTVUUR_PLEIN"][::2], tekst.BORDEN["plein_west"])
    b.plein(67, 1, PLEKKEN["RUSTVUUR_OOST"][::2], tekst.BORDEN["plein_oost"])
    b.poorthal(WESTHAL, True)
    b.poorthal(OOSTHAL, False)
    b.gevel(37, -1, "west")
    b.gevel(59, 1, "oost")
    b.westtrap()
    b.hefboomhal()
    b.putkamer()
    b.geheime_gang()
    b.zuilenhal()
    b.diepe_poort()
    b.kloof()
    b.langetrap()
    b.schil()
    b.verbind()
    b.midden()
    b.cast()
    return b


def deur_template(h, b, naam):
    """The blocks of a door as a little template of its own (Herstel puts it back after it slid open)."""
    x0, y0, z0, x1, y1, z1 = DEUREN[naam]
    s = h.Structure((x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1))
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                blok = b.s.blocks.get((x, y, z))
                if blok:
                    s.blocks[(x - x0, y - y0, z - z0)] = blok
    return s


def test_template(h):
    """RingH3GameTests: a room with a floor of houtskoolsteen."""
    s = h.Structure((25, 9, 25))
    for x in range(25):
        for z in range(25):
            s.set(x, 0, z, HOUTSKOOL)
    return s


# =====================================================================================================================
# the self-check
# =====================================================================================================================
def _loopbaar(b):
    """Every cell a player can stand in: two blocks of air (or passable things) over something to stand on."""
    blocks = b.s.blocks
    door = (AIR, KETTING, LANTAARN, ZIELLAMP, SMEUL, HENDEL, "minecraft:spruce_wall_sign", "minecraft:spruce_sign")

    def vrij(q):
        return q in blocks and blocks[q][0] in door

    def grond(q):
        return q in blocks and blocks[q][0] not in door and blocks[q][0] != TRALIES
    uit = set()
    for (x, y, z), blok in blocks.items():
        if blok[0] in door and vrij((x, y + 1, z)) and (grond((x, y - 1, z))):
            uit.add((x, y, z))
    return uit


def bereik(b, start, open_deuren=()):
    """The standing cells reachable on foot from start (steps of one block up or down, a jump of one), with these doors open."""
    lopen = _loopbaar(b)
    extra = set()
    for naam in open_deuren:
        x0, y0, z0, x1, y1, z1 = DEUREN[naam]
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                extra.add((x, y0, z))
    lopen |= extra
    gezien, stapel = set(), [start]
    while stapel:
        p = stapel.pop()
        if p in gezien or p not in lopen:
            continue
        gezien.add(p)
        x, y, z = p
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                stapel.append((x + dx, y + dy, z + dz))
    return gezien


def check(b):
    problems = []
    blocks = b.s.blocks
    tel = lambda name: sum(1 for v in blocks.values() if v[0] == name)
    if tel(VUUR) != 4:
        problems.append(f"{tel(VUUR)} rest fires, 4 wanted")
    for naam in ("RUSTVUUR_PLEIN", "RUSTVUUR_PUT", "RUSTVUUR_OOST"):
        if b.get(*PLEKKEN[naam]) != VUUR:
            problems.append(f"no rest fire at {naam}")
    hendels = sorted((v[1]["nr"], k) for k, v in blocks.items() if v[0] == HENDEL)
    if [n for n, _ in hendels] != ["0", "1", "2", "3"]:
        problems.append(f"the levers: {hendels}")
    runen = [(k, v[1]["teken"]) for k, v in blocks.items() if v[0] == RUNE and k[0] == PUTKAMER[0] - 1 and k[1] == MIDDEL + 1]
    if sorted(t for _, t in runen) != sorted(str(t) for t in RUNE_TEKENS):
        problems.append(f"the rune stones of the well room: {runen}")
    if len(b.brokkel) < 40:
        problems.append(f"only {len(b.brokkel)} crumbling stones")
    jigsaws = [k for k, v in blocks.items() if v[0] == "minecraft:jigsaw"]
    if jigsaws != [(ROTS_MIDDEN[0], 0, ROTS_MIDDEN[1])]:
        problems.append(f"the centre jigsaw: {jigsaws}")
    # nothing of the inside is open to the rock: every inner air cell has a block (not nothing) on all six sides
    for (x, y, z) in b.binnen:
        for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
            if q not in blocks and b.s.inside(*q):
                problems.append(f"the inside is open at {q}")
                break
        if len(problems) > 30:
            return problems
    # the way through, door by door
    start = PLEKKEN["POORT_BUITEN"]
    in_hal = (42, BOVEN, 62)
    stadia = [((), [start], [in_hal]),
              (("west",), [in_hal, (40, MIDDEL, 33)], [(20, MIDDEL, 33)]),
              (("west", "valhek"), [(20, MIDDEL, 31), PLEKKEN["GIMGUH"]], [(11, MIDDEL, 35)]),
              (("west", "valhek", "geheim"), [(21, DIEP, 25), (30, DIEP, 14), (63, DIEP, 14), (80, DIEP, 14), (54, BOVEN, 62)], [(62, BOVEN, 62)]),
              (("west", "valhek", "geheim", "oost"), [(62, BOVEN, 62), PLEKKEN["ARAGUH"]], [])]
    for deuren, wel, niet in stadia:
        r = bereik(b, start, deuren)
        for p in wel:
            if tuple(p) not in r:
                problems.append(f"with the doors {deuren} open, {p} can't be reached from the west forecourt")
        for p in niet:
            if tuple(p) in r:
                problems.append(f"with only the doors {deuren} open, {p} can already be reached")
    # the cast stands on something, in the air
    for kind, id, x, y, z, yaw, plek, van, tot in CAST:
        if b.get(x, y, z) not in (AIR,) or b.get(x, y - 1, z) in (AIR, None):
            problems.append(f"{id} does not stand at {(x, y, z)}: {b.get(x, y, z)} over {b.get(x, y - 1, z)}")
    # nothing hangs loose
    for (x, y, z), v in blocks.items():
        if v[0] in (LANTAARN, ZIELLAMP):
            onder_boven = (x, y + 1, z) if v[1].get("hanging") == "true" else (x, y - 1, z)
            if blocks.get(onder_boven, (AIR,))[0] == AIR:
                problems.append(f"a lantern hangs on nothing at {(x, y, z)}")
        if v[0] == "minecraft:spruce_wall_sign":
            f = v[1]["facing"]
            achter = {"south": (x, y, z - 1), "north": (x, y, z + 1), "east": (x - 1, y, z), "west": (x + 1, y, z)}[f]
            if blocks.get(achter, (AIR,))[0] == AIR:
                problems.append(f"a wall sign on nothing at {(x, y, z)}")
    return problems


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    def __init__(self):
        import make_structures as ms
        self.ms, self.Structure, self.Byte, self.floats = ms, ms.Structure, ms.Byte, ms.floats


def preview(out, alleen=()):
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    from features import ring_h3_beeld as beeld
    os.makedirs(out, exist_ok=True)
    b = bouw(_H())
    for p in check(b):
        print("PROBLEM:", p)
    print("blocks:", len(b.s.blocks), "inner air:", len(b.binnen), "crumbling:", len(b.brokkel))
    beeld.alles(b, out, alleen)


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".", alleen=sys.argv[2:])
