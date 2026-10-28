"""
1.8.0 uitbreiding (dromen-ontwerp/BOUWPLAN_180_UITBREIDING.md), agent "vliegdroom": De Vliegdroom, the island high above the
Droomplein you reach on a Rookguh. Java: feature/vliegdroom (VliegdroomFeature, DroomRookguhEntity, VliegParcours, VliegPlekken,
client/VliegdroomClient); island id DroomEilanden.VLIEG.

  build(h)    the island template data/guhs/structure/dromen/eiland_vlieg.nbt (from ontwerp(): a big cloud island with the
              Rookguh-stal, the highest cloud with its spiral stair and the view, guh-ballonnetjes, the landing, the bubble down,
              and the ten wolkjesringen of the parcours around it), the island json (data/guhs/dromen/eiland/vlieg.json), its
              spots (data/guhs/dromen/vlieg_plekken.json), its islanders (data/guhs/dromen/bewoners/vlieg.json), the
              Vlieg-diploma item and the texts (Dutch in nl_nl, English in tools/lang/en/c216_vliegdroom.json)

ontwerp() is plain geometry (a dict of blocks + the spots): the render dromen-ontwerp/_bron/r07_vliegdroom.py draws the very same
island. Coordinates: the template is X x Y x Z; its centre block MIDDEN lands on the island's centre spot (0, 200, 0); every spot
in the jsons is relative to that centre (feet positions: +0.5 in x and z).

Standalone (from the project root): PYTHONPATH=tools python tools/features/vliegdroom.py
"""
import gzip
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

X, Y, Z = 100, 80, 100
C = 50                    # island centre (x and z)
T = 34                    # the island's top layer (y)
MIDDEN = (C, T, C)
VERSIE = 1
R = 22                    # the island's radius
SIGN = "sign.guhs.vliegdroom"

# the tower of the highest cloud
TX, TZ, TOP = C + 13, C + 1, T + 26
# the stal (a big round nest)
SX, SZ, SR = C, C - 12, 6
# the landing (aankomst), the Rookguh back down, the bubble down
AANKOMST = (C, T + 1, C + 13)
TERUG = (C - 6, T + 1, C + 16)
STEIGER = (C + 6, T + 1, C + 17)
# the parcours: ten rings in a loop around the island
RINGEN = 10
RING_R = 34
RING_STRAAL = 3.5

TEKSTEN = {  # key -> (Dutch, English)
    "gui.guhs.dromen.eiland.vlieg": ("De Vliegdroom", "The Flying Dream"),
    "subtitles.guhs.dromen.muziek.vlieg": ("Vliegmuziek speelt", "Flying music plays"),
    "entity.guhs.droom_rookguh": ("Droom-Rookguh", "Dream Rookguh"),
    "item.guhs.vlieg_diploma": ("Vlieg-diploma", "Flying Diploma"),
    "item.guhs.vlieg_diploma.lore": ("Voor wie het hele wolkjesringen-parcours van de Vliegdroom heeft gevlogen. Zonder ook maar één "
                                     "keer de weg kwijt te raken (nou ja... bijna).",
                                     "For whoever flew the whole cloud-ring course of the Flying Dream. Without getting lost even once "
                                     "(well... almost)."),
    "gui.guhs.vliegdroom.omhoog": ("De Rookguh blaast zijn wangetjes bol en zweeft met je omhoog... naar de Vliegdroom! Niet naar beneden kijken, vadsje.",
                                   "The Rookguh puffs up its little cheeks and floats you up... to the Flying Dream! Don't look down, vadsje."),
    "gui.guhs.vliegdroom.omlaag": ("Hup, op de rug! De Rookguh brengt je zachtjes terug naar het Droomplein.",
                                   "Hop on its back! The Rookguh takes you gently back down to the Dream Square."),
    "gui.guhs.vliegdroom.rij": ("Je zit op een Rookguh! Vooruit = vliegen waar je kijkt, springen = omhoog, sprinten = sneller, sluipen = eraf. "
                                "Vlieg door de wolkjesringen voor het parcours!",
                                "You're on a Rookguh! Forward = fly where you look, jump = up, sprint = faster, sneak = hop off. "
                                "Fly through the cloud rings for the course!"),
    "gui.guhs.vliegdroom.bezet": ("Op deze Rookguh zit al iemand. Njeg, wachten op je beurt!", "Someone is already on this Rookguh. Njeg, wait your turn!"),
    "gui.guhs.vliegdroom.ring": ("Wolkjesring %s van %s! (%s)", "Cloud ring %s of %s! (%s)"),
    "gui.guhs.vliegdroom.kwijt": ("Te lang geen ring... de klok is gestopt. Begin weer bij de eerste ring!",
                                  "Too long without a ring... the clock stopped. Start again at the first ring!"),
    "gui.guhs.vliegdroom.klaar": ("Parcours rond in %s! (je record: %s)", "Course done in %s! (your record: %s)"),
    "gui.guhs.vliegdroom.record": ("Nieuw record: %s! Vahoeg snel. (De beloning voor vandaag had je al.)",
                                   "New record: %s! Vahoeg fast. (You already had today's reward.)"),
    "gui.guhs.vliegdroom.record_beloning": ("Nieuw record: %s! De Rookguh hikt van trots. Hier, wat droomzand.",
                                            "New record: %s! The Rookguh hiccups with pride. Here, some dream sand."),
    "gui.guhs.vliegdroom.diploma": ("Je eerste hele parcours, in %s! Hier is je Vlieg-diploma, en een zakje droomzand. Piloot-guh-waardig!",
                                    "Your first whole course, in %s! Here's your Flying Diploma, and a little bag of dream sand. Pilot-guh worthy!"),
}

BORDJES = {  # sign key suffix -> ([Dutch lines], [English lines]); at most 16 characters a line
    "welkom": (["~ Vliegdroom ~", "Hoger dromen", "kan niet.", "Nou ja... bijna"], ["~ Flying Dream ~", "You can't dream", "any higher.", "Well... almost"]),
    "stal": (["Rookguh-stal", "klik = opstappen", "kijk = sturen", "sluip = eraf"], ["Rookguh stable", "click = hop on", "look = steer", "sneak = hop off"]),
    "parcours": (["Wolkjesringen:", "door alle tien,", "op volgorde!", "klok loopt mee"], ["Cloud rings:", "through all ten,", "in order!", "a clock runs"]),
    "uitzicht": (["Hoogste wolk", "Kijk eens hoe", "klein het plein", "is, vadsje!"], ["Highest cloud", "Look how tiny", "the square is,", "vadsje!"]),
    "ballon": (["Niet loslaten!", "Anders vliegt", "hij naar de", "maan. Njeg."], ["Don't let go!", "Or it floats", "off to the", "moon. Njeg."]),
    "terug": (["Naar beneden?", "Klik op de", "Rookguh, of", "spring in de bel"], ["Going down?", "Click the", "Rookguh, or", "jump in bubble"]),
}
for _k, (_nl, _en) in BORDJES.items():
    for _i, (_a, _b) in enumerate(zip(_nl, _en)):
        TEKSTEN[f"{SIGN}.{_k}.{_i}"] = (_a, _b)

BALLON_KLEUREN = ["knuffelblok_roze", "knuffelblok_hemelsblauw", "knuffelblok_boter", "knuffelblok_lavendel", "knuffelblok_aqua",
                  "knuffelblok_perzik"]


# ------------------------------------------------------------------------------------------------ plain geometry
class Ontwerp:
    """A dict of blocks (template coordinates -> 'id[props]'), plus signs, plus the spots."""

    def __init__(self):
        self.b = {}
        self.borden = []      # (x, y, z, rotation, key)

    def inb(self, x, y, z):
        return 0 <= x < X and 0 <= y < Y and 0 <= z < Z

    def set(self, x, y, z, b, keep=False):
        x, y, z = int(x), int(y), int(z)
        if not self.inb(x, y, z) or (keep and (x, y, z) in self.b):
            return
        if b == "minecraft:air":
            self.b.pop((x, y, z), None)
        else:
            self.b[(x, y, z)] = b

    def is_air(self, x, y, z):
        return (int(x), int(y), int(z)) not in self.b

    def fill(self, x1, y1, z1, x2, y2, z2, b, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, b, keep=keep)

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, b, keep=False):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1:
                        self.set(x, y, z, b, keep=keep)

    def bord(self, x, y, z, rotation, sleutel):
        self.borden.append((int(x), int(y), int(z), rotation, sleutel))
        self.set(x, y, z, f"minecraft:birch_sign[rotation={rotation},waterlogged=false]")


def stairs(b, f, half="bottom"):
    return f"{b}[facing={f},half={half},shape=straight,waterlogged=false]"


def eiland(O, rng):
    """The cloud island: a flat fluffy top (walkable), a pink rim, a dripping cloud underneath."""
    fase = rng.random(3) * 2 * math.pi

    def rand(a):
        return R * (1 + 0.12 * (0.6 * math.sin(3 * a + fase[0]) + 0.4 * math.sin(5 * a + fase[1]) + 0.2 * math.sin(9 * a + fase[2])))

    def binnen(x, z):
        return math.hypot(x - C, z - C) <= rand(math.atan2(z - C, x - C))

    for x in range(C - 30, C + 31):
        for z in range(C - 30, C + 31):
            dx, dz = x - C, z - C
            d = math.hypot(dx, dz) / rand(math.atan2(dz, dx))
            if d > 1:
                continue
            h = int(16 * (1 - d) ** 0.6 + rng.random() * 1.5) + 1
            for k in range(h + 1):
                if k == 0:
                    b = "guhs:wolkenblok_roze" if d > 0.93 else ("guhs:pluiswolblok" if rng.random() < 0.82 else "guhs:wolkenblok_wit")
                elif k >= h - 1 and d < 0.35:
                    b = "guhs:wolkenblok_roze"
                else:
                    b = "guhs:wolkenblok_wit"
                O.set(x, T - k, z, b)
            if d > 0.85 and rng.random() < 0.1:
                for k in range(1, int(rng.integers(2, 6))):
                    O.set(x, T - h - k, z, "guhs:wolkenweide_rank", keep=True)
    return binnen


def pad(O, a, b, blok, breed=1):
    """A path on the top layer from a to b (x, z), breed blocks either side."""
    n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) * 2 + 1
    for i in range(n + 1):
        f = i / n
        x, z = a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f
        for w in range(-breed, breed + 1):
            for v in range(-breed, breed + 1):
                if (int(round(x)) + w, T, int(round(z)) + v) in O.b:
                    O.set(int(round(x)) + w, T, int(round(z)) + v, blok)


def lantaarnpaal(O, x, z, kleur="lampion_roze"):
    O.set(x, T + 1, z, "guhs:guh_hekje_wit")
    O.set(x, T + 2, z, "guhs:guh_hekje_wit")
    O.set(x, T + 3, z, f"guhs:{kleur}")


def stal(O, plek):
    """The Rookguh-stal: a big round nest (a soft floor, a rim of fluff one block high with two ways in), cushions, a sign."""
    kleuren = ["guhs:knuffelblok_roze", "guhs:knuffelblok_wolkje", "guhs:knuffelblok_lavendel", "guhs:knuffelblok_vanille"]
    for x in range(SX - SR - 1, SX + SR + 2):
        for z in range(SZ - SR - 1, SZ + SR + 2):
            d = math.hypot(x - SX, z - SZ)
            if d <= SR - 0.5:
                O.set(x, T, z, kleuren[(x // 2 + z // 2) % len(kleuren)])
            elif d <= SR + 0.6:
                a = math.degrees(math.atan2(z - SZ, x - SX))
                ingang = abs(((a - 90) + 180) % 360 - 180) < 22 or abs(((a + 90) + 180) % 360 - 180) < 22
                O.set(x, T, z, "guhs:pluiswolblok")
                if not ingang:
                    O.set(x, T + 1, z, "guhs:pluiswolblok")
    plek["stal"] = []
    for (dx, dz) in ((-3, 1), (0, -2), (3, 1)):
        plek["stal"].append([SX + dx - C + 0.5, 1.5, SZ + dz - C + 0.5])
    # a little roof of cloud over the back half, on four cloud posts (headroom: 5)
    for (dx, dz) in ((-5, -2), (5, -2), (-4, -5), (4, -5)):
        for y in range(T + 1, T + 6):
            O.set(SX + dx, y, SZ + dz, "guhs:wolkenblok_wit")
    for x in range(SX - 6, SX + 7):
        for z in range(SZ - 7, SZ):
            if math.hypot(x - SX, (z - SZ) * 1.3) <= 6.8:
                O.set(x, T + 6, z, "guhs:wolkenblok_roze" if (x + z) % 3 else "guhs:wolkenblok_wit")
    for x in (SX - 3, SX, SX + 3):
        O.set(x, T + 5, SZ - 4, "guhs:sterrenlantaarn")
    O.set(SX + 2, T + 1, SZ + SR + 2, "minecraft:air")
    O.bord(SX + 3, T + 1, SZ + SR + 2, 8, "stal")


def toren(O, plek):
    """The highest cloud: a tower of cloud puffs, a spiral stair around it (steps of one block), the view platform on top."""
    for y in range(T + 1, TOP):
        r = 4.0 + 0.6 * math.sin(y * 0.7)
        for x in range(TX - 6, TX + 7):
            for z in range(TZ - 6, TZ + 7):
                if math.hypot(x - TX, z - TZ) <= min(r, 4.2):
                    O.set(x, y, z, "guhs:wolkenblok_wit" if (y // 3) % 2 else "guhs:pluiswolblok")
    # the spiral: a ramp of cloud blocks (radius 4.7 to 6.9) that rises RISE blocks per turn; every cell takes the height of its
    # own angle, so neighbours never differ more than one block (a stair you can walk, also for a Blub)
    RISE = 10
    start_a = math.radians(205)               # it starts on the side towards the landing (south-west of the tower)
    lussen = int(math.ceil((TOP - 1 - (T + 1)) / RISE)) + 1
    eind_a = start_a
    for k in range(lussen):
        for x in range(TX - 8, TX + 9):
            for z in range(TZ - 8, TZ + 9):
                d = math.hypot(x - TX, z - TZ)
                if not 4.7 <= d <= 6.9:
                    continue
                a = (math.atan2(z - TZ, x - TX) - start_a) % (2 * math.pi)
                A = a + 2 * math.pi * k
                y = T + 1 + int(A / (2 * math.pi) * RISE)
                if y > TOP - 1:
                    continue
                O.set(x, y, z, "guhs:wolkenblok_roze" if int(A * 3) % 2 else "guhs:wolkenblok_wit")
                if y == TOP - 1:
                    eind_a = max(eind_a, start_a + A)
    # the platform on top (radius 4.5) with a fence, open where the stair arrives
    for x in range(TX - 6, TX + 7):
        for z in range(TZ - 6, TZ + 7):
            d = math.hypot(x - TX, z - TZ)
            if d <= 4.6:
                O.set(x, TOP, z, "guhs:regenboogblok" if d < 1.2 else "guhs:pluiswolblok")
                if d > 3.7:
                    a = math.atan2(z - TZ, x - TX)
                    if abs(((math.degrees(a - eind_a)) + 180) % 360 - 180) > 30:
                        O.set(x, TOP + 1, z, "guhs:guh_hekje_wit")
    O.set(TX - 1, TOP + 1, TZ - 2, stairs("guhs:wolkenblok_roze_trap", "north"))
    O.set(TX, TOP + 1, TZ - 2, stairs("guhs:wolkenblok_roze_trap", "north"))
    O.set(TX + 1, TOP + 1, TZ - 2, stairs("guhs:wolkenblok_roze_trap", "north"))
    O.set(TX + 2, TOP + 1, TZ + 1, "guhs:sterrenlantaarn")
    O.set(TX - 2, TOP + 1, TZ + 1, "guhs:sterrenlantaarn")
    O.bord(TX, TOP + 1, TZ + 1, 0, "uitzicht")
    sx, sz = int(round(TX + math.cos(start_a + 0.15) * 5.8)), int(round(TZ + math.sin(start_a + 0.15) * 5.8))
    plek["trap_begin"] = [sx - C + 0.5, 1, sz - C + 0.5]
    plek["uitzicht"] = [TX - C + 0.5, TOP + 1 - T, TZ - 2 - C + 0.5]


def ballonnen(O, rng, plek):
    """Guh-ballonnetjes on strings (end rods), round knuffelblok balloons with a little guh face."""
    plek["ballonnen"] = []
    for k, ang in enumerate((30, 140, 230, 265, 300, 330)):
        a = math.radians(ang)
        x, z = int(round(C + math.sin(a) * (R - 4))), int(round(C - math.cos(a) * (R - 4)))
        if (x, T, z) not in O.b:
            continue
        hoog = 6 + k % 3
        for y in range(T + 1, T + 1 + hoog):
            O.set(x, y, z, "minecraft:end_rod[facing=up]")
        kleur = "guhs:" + BALLON_KLEUREN[k % len(BALLON_KLEUREN)]
        O.ellipsoid(x, T + hoog + 2, z, 1.6, 2.1, 1.6, kleur)
        O.set(x, T + hoog + 2, z + 2, "guhs:tegel_guhgezicht")
        O.set(x, T + hoog, z, kleur)
        plek["ballonnen"].append([x - C + 0.5, 1, z - C + 1.5])
    O.bord(C - 8, T + 1, C + 8, 6, "ballon")


def ringen(O, plek):
    """Ten wolkjesringen in a loop around the island, at changing heights, each standing across the way you fly."""
    plek["ringen"] = []
    for i in range(RINGEN):
        th = math.radians(10 + i * 360 / RINGEN)
        cx, cz = C + math.sin(th) * RING_R, C - math.cos(th) * RING_R
        cy = T + 10 + 7 * math.sin(i * 0.9) + i * 1.3
        ux, uz = math.sin(th), -math.cos(th)            # radial (in the ring's plane)
        for k in range(160):
            f = 2 * math.pi * k / 160
            for rr in (RING_STRAAL, RING_STRAAL + 0.5):
                x, y, z = cx + ux * math.cos(f) * rr, cy + math.sin(f) * rr, cz + uz * math.cos(f) * rr
                kwart = int((f % (2 * math.pi)) / (math.pi / 4)) % 2
                O.set(int(round(x)), int(round(y)), int(round(z)), "guhs:regenboogblok" if kwart else "guhs:wolkenblok_wit")
        O.set(int(round(cx + ux * (RING_STRAAL + 0.6))), int(round(cy + RING_STRAAL + 0.6)), int(round(cz + uz * (RING_STRAAL + 0.6))),
              "guhs:sterrenlantaarn")
        plek["ringen"].append([round(cx - C + 0.5, 2), round(cy - T + 0.5, 2), round(cz - C + 0.5, 2), RING_STRAAL])


def ontwerp():
    O = Ontwerp()
    rng = np.random.default_rng(1808)
    plek = {}
    binnen = eiland(O, rng)
    # paths: landing -> stal, landing -> tower stair, landing -> the Rookguh down and the bubble
    stal(O, plek)
    toren(O, plek)
    tb = plek["trap_begin"]
    pad(O, (AANKOMST[0], AANKOMST[2]), (SX, SZ + SR + 1), "guhs:pleister_lavendel")
    pad(O, (AANKOMST[0], AANKOMST[2]), (int(tb[0] + C), int(tb[2] + C)), "guhs:pleister_wolkje")
    pad(O, (AANKOMST[0], AANKOMST[2]), (TERUG[0], TERUG[2]), "guhs:pleister_lavendel")
    pad(O, (AANKOMST[0], AANKOMST[2]), (STEIGER[0], STEIGER[2]), "guhs:pleister_lavendel")
    # the Rookguh down: a rainbow pad; the bubble down: a pad of zeepbelglas with a bubble above it
    O.fill(TERUG[0] - 1, T, TERUG[2] - 1, TERUG[0] + 1, T, TERUG[2] + 1, "guhs:regenboogblok")
    O.fill(STEIGER[0] - 1, T, STEIGER[2] - 1, STEIGER[0] + 1, T, STEIGER[2] + 1, "guhs:zeepbelglas")
    O.set(STEIGER[0], T + 4, STEIGER[2], "guhs:zeepbel")
    O.bord(C - 2, T + 1, C + 16, 8, "terug")
    O.bord(C + 2, T + 1, C + 11, 8, "welkom")
    O.bord(C + 3, T + 1, C - 3, 8, "parcours")
    for (x, z, kl) in ((C - 3, C + 11, "lampion_roze"), (C + 9, C + 12, "lampion_mint"), (C - 3, C + 1, "lampion_geel"),
                       (C + 3, C - 4, "lampion_roze"), (C + 7, C + 6, "lampion_mint")):
        if (x, T, z) in O.b and (x, T + 1, z) not in O.b:
            lantaarnpaal(O, x, z, kl)
    ballonnen(O, rng, plek)
    # moon flowers and a few bubbles over the island
    for _ in range(70):
        x, z = int(rng.integers(C - R, C + R)), int(rng.integers(C - R, C + R))
        if binnen(x, z) and (x, T, z) in O.b and O.b[(x, T, z)] in ("guhs:pluiswolblok", "guhs:wolkenblok_wit") and (x, T + 1, z) not in O.b:
            O.set(x, T + 1, z, "guhs:maanbloemetje[open=true]")
    for _ in range(10):
        x, z = int(rng.integers(C - R, C + R)), int(rng.integers(C - R, C + R))
        y = T + int(rng.integers(5, 12))
        if binnen(x, z) and math.hypot(x - TX, z - TZ) > 8 and math.hypot(x - SX, z - SZ) > 9:
            O.set(x, y, z, "guhs:zeepbel", keep=True)
    ringen(O, plek)
    plek["aankomst"] = {"plek": [AANKOMST[0] - C + 0.5, 1.0, AANKOMST[2] - C + 0.5], "yaw": 180.0}
    plek["steiger"] = [STEIGER[0] - C + 0.5, 1.0, STEIGER[2] - C + 0.5]
    plek["terug"] = [TERUG[0] - C + 0.5, 1.6, TERUG[2] - C + 0.5]
    return O, plek


# ------------------------------------------------------------------------------------------------ the item texture
def diploma_png():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 3, 13, 12], fill=(255, 248, 232, 255), outline=(176, 140, 196, 255))
    d.line([4, 6, 11, 6], fill=(150, 120, 170, 255))
    d.line([4, 8, 9, 8], fill=(150, 120, 170, 255))
    d.rectangle([1, 2, 2, 13], fill=(220, 180, 230, 255))
    d.rectangle([13, 2, 14, 13], fill=(220, 180, 230, 255))
    d.ellipse([9, 9, 13, 13], fill=(255, 140, 180, 255))          # the seal: a little Rookguh-pink
    d.point([(10, 11), (12, 11)], fill=(90, 40, 80, 255))
    d.line([10, 13, 9, 15], fill=(240, 90, 140, 255))
    d.line([12, 13, 13, 15], fill=(240, 90, 140, 255))
    return img


# ------------------------------------------------------------------------------------------------ build
def build(h, ms):
    img = diploma_png()
    p = os.path.join(h.A, "textures", "item", "vlieg_diploma.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    h.w(f"{h.A}/models/item/vlieg_diploma.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/vlieg_diploma"}})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)

    O, plek = ontwerp()
    S = ms.Structure((X, Y, Z))
    for (x, y, z), b in O.b.items():
        naam, props = b, {}
        if "[" in b:
            naam, rest = b[:-1].split("[", 1)
            props = dict(kv.split("=") for kv in rest.split(","))
        nbt = None
        if naam == "minecraft:birch_sign":
            sleutel = next(s for (bx, by, bz, _, s) in O.borden if (bx, by, bz) == (x, y, z))
            nl = BORDJES[sleutel][0]
            msgs = [json.dumps({"translate": f"{SIGN}.{sleutel}.{i}", "fallback": nl[i]}) if i < len(nl) else json.dumps("") for i in range(4)]
            leeg = [json.dumps("")] * 4
            nbt = {"id": "minecraft:sign", "is_waxed": ms.Byte(1),
                   "front_text": {"messages": ms.NbtList(8, msgs), "color": "black", "has_glowing_text": ms.Byte(0)},
                   "back_text": {"messages": ms.NbtList(8, leeg), "color": "black", "has_glowing_text": ms.Byte(0)}}
        S.set(x, y, z, naam, props or None, nbt)
    S.save("dromen/eiland_vlieg")
    pad_ = os.path.join(ms.OUT, "dromen", "eiland_vlieg.nbt")
    with open(pad_, "rb") as f:
        data = gzip.decompress(f.read())
    with open(pad_, "wb") as f:
        f.write(gzip.compress(data, mtime=0))
    D = os.path.join("src", "main", "resources", "data", "guhs", "dromen")
    os.makedirs(os.path.join(D, "eiland"), exist_ok=True)
    os.makedirs(os.path.join(D, "bewoners"), exist_ok=True)
    schrijf(os.path.join(D, "eiland", "vlieg.json"), {"versie": VERSIE, "template": "guhs:dromen/eiland_vlieg", "midden": list(MIDDEN),
                                                       "aankomst": plek["aankomst"], "steiger": plek["steiger"]})
    schrijf(os.path.join(D, "vlieg_plekken.json"), {"ringen": plek["ringen"], "stal": plek["stal"], "terug": plek["terug"],
                                                    "plein_steiger": [12.5, 1, -17.5]})
    bewoners = [{"soort": "blub", "plek": b, "yaw": 180} for b in plek["ballonnen"][:3]]
    bewoners.append({"soort": "guh_slaapt", "plek": [SX - C + 0.5 - 4, 1.0, SZ - C + 0.5 + 3], "yaw": 90})
    bewoners.append({"soort": "guh_slaapt", "plek": plek["uitzicht"], "yaw": 0})
    schrijf(os.path.join(D, "bewoners", "vlieg.json"), {"versie": 1, "bewoners": bewoners})
    selfcheck(plek)
    return plek


def schrijf(pad_, data):
    with open(pad_, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def selfcheck(plek):
    problems = []
    if len(plek["ringen"]) != RINGEN:
        problems.append("rings")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    for k, (nl, en) in BORDJES.items():
        for line in nl + en:
            if len(line) > 16:
                problems.append(f"sign {k}: '{line}' is longer than 16")
    if problems:
        raise SystemExit("vliegdroom self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    import make_structures as ms
    build(h, ms)
    en_pad = os.path.join("tools", "lang", "en", "c216_vliegdroom.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    with open(os.path.join(a, "vlieg_diploma.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "guhs:item/vlieg_diploma"}}, f, indent=2)
        f.write("\n")
    print(f"vliegdroom: island template, {RINGEN} rings, diploma, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
