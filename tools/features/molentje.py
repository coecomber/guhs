"""
1.8.0 (Guhdromenland), island agent "molentje" (dromen-ontwerp/BOUWPLAN_180.md part E): Het Maanmolentje, the island where the
Zandguh grinds dreams into droomzand (Snoes' fifth piece). Java: feature/molentje (MolentjeFeature, Molen, MolenBlokken,
MolentjeCommando, client/WiekenRenderer).

  build(h)    the island template data/guhs/structure/dromen/eiland_molentje.nbt (from the render r06_maanmolentje.py, with the
              concept blocks turned into real ones and the sails as one turning block), the island json for part A
              (data/guhs/dromen/eiland/molentje.json), the spots of the puzzle (data/guhs/dromen/molentje_plekken.json), the
              three blocks (molen_tandwiel, blaaskussentje, molenwieken: textures, models, states, items, loot, recipes) and the
              texts (Dutch in nl_nl, English in tools/lang/en/c213_molentje.json)

Coordinates: the template is X x Y x Z; its centre block (MIDDEN) lands on the island's centre spot; every spot in the jsons
is relative to that centre (feet positions: +0.5 in x and z).

Standalone (from the project root): PYTHONPATH=tools python tools/features/molentje.py
"""
import gzip
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

X, Y, Z = 84, 60, 84
C = 42                    # island centre (x and z)
T = 30                    # the island's top layer (y)
MIDDEN = (C, T, C)
VERSIE = 1
SIGN = "sign.guhs.molentje"

NAMEN = {  # id -> (Dutch, English, Dutch lore, English lore)
    "molen_tandwiel": ("Molentandwieltje", "Mill Cog",
                       "Klik om te draaien. Wijzen alle pijltjes omhoog? Dan kan de molen malen!",
                       "Click to turn it. All arrows pointing up? Then the mill can grind!"),
    "blaaskussentje": ("Blaaskussentje", "Blowing Cushion",
                       "Klik erop bij het Maanmolentje: een Blub-guh komt erop zitten en blaast bellen naar de wieken.",
                       "Click it at the Maanmolentje: a Blub-guh comes to sit on it and blows bubbles at the sails."),
    "molenwieken": ("Molenwieken", "Mill Sails",
                    "De wieken van het Maanmolentje. Ze draaien alleen als er genoeg geblazen wordt, njeg.",
                    "The sails of the Maanmolentje. They only turn when there's enough blowing, njeg."),
}

TEKSTEN = {  # key -> (Dutch, English)
    "gui.guhs.molentje.tandwiel.klik": ("Klik! Tandwieltje %s staat nu %s.", "Click! Cog %s now points %s."),
    "gui.guhs.molentje.richting.0": ("omhoog", "up"),
    "gui.guhs.molentje.richting.1": ("naar rechts", "right"),
    "gui.guhs.molentje.richting.2": ("omlaag", "down"),
    "gui.guhs.molentje.richting.3": ("naar links", "left"),
    "gui.guhs.molentje.tandwielen_goed": ("Alle tandwieltjes wijzen omhoog. Nu nog blazen!", "All cogs point up. Now for some blowing!"),
    "gui.guhs.molentje.kussen.komt": ("Een Blub-guh hupst naar het blaaskussentje!", "A Blub-guh hops over to the blowing cushion!"),
    "gui.guhs.molentje.kussen.bezet": ("Op dit kussentje blaast al een Blub. Vahoeg!", "A Blub is already blowing on this cushion. Vahoeg!"),
    "gui.guhs.molentje.kussen.geen_blub": ("Geen Blub in de buurt... ze huppelen vast ergens rond het molentje.",
                                           "No Blub nearby... they must be hopping around the mill somewhere."),
    "gui.guhs.molentje.kussen.niet_hier": ("Dit kussentje werkt alleen bij het Maanmolentje in Guhdromenland.",
                                           "This cushion only works at the Maanmolentje in Guhdromenland."),
    "gui.guhs.molentje.blazen": ("%s van de 3 Blubs blazen.", "%s of the 3 Blubs are blowing."),
    "gui.guhs.molentje.draait": ("De wieken draaien! De Zandguh maalt droomzand. Ga snel bij hem langs!",
                                 "The sails are turning! The Zandguh is grinding droomzand. Go and see him!"),
    "gui.guhs.molentje.stil": ("De wieken staan weer stil. Wil je nog eens? Draai de tandwieltjes en roep de Blubs!",
                               "The sails have stopped. Again? Turn the cogs and call the Blubs!"),
    "gui.guhs.molentje.zandguh.hint.0": ("De molen staat stil, vadsje... Drie Blubs moeten op de blaaskussentjes de wieken aanblazen.",
                                         "The mill stands still, vadsje... Three Blubs on the blowing cushions have to blow the sails going."),
    "gui.guhs.molentje.zandguh.hint.1": ("En de tandwieltjes op de muur! Alle pijltjes omhoog, anders draait er niks. Klik, klik, njeg.",
                                         "And the cogs on the wall! All arrows up, or nothing turns. Click, click, njeg."),
    "gui.guhs.molentje.zandguh.hint.2": ("Pas op: een tandwieltje draait zijn buurman altijd een stukje mee. Vahoeg slim hoor.",
                                         "Careful: a cog always turns its neighbour along a little. Vahoeg clever, huh."),
    "gui.guhs.molentje.zandguh.droomzand": ("Hij draait! Hier, een zakje vers gemalen droomzand. Voor Snoes zijn mooiste droom. Slaap lekker, vadsje!",
                                            "It's turning! Here, a little bag of freshly ground droomzand. For Snoes' sweetest dream. Sleep tight, vadsje!"),
    "gui.guhs.molentje.zandguh.al": ("Je hebt je zakje al gekregen, njeg. Wacht tot de molen weer gaat draaien!",
                                     "You already got your little bag, njeg. Wait until the mill turns again!"),
    "gui.guhs.molentje.stukje": ("Een stukje van Snoes' droom: het droomzand!", "A piece of Snoes' dream: the droomzand!"),
    "gui.guhs.molentje.cmd.los": ("Maanmolentje: tandwieltjes omhoog, %s Blubs op de kussentjes.", "Maanmolentje: cogs up, %s Blubs on the cushions."),
    "gui.guhs.molentje.cmd.schud": ("Maanmolentje: stil gezet en geschud.", "Maanmolentje: stopped and shuffled."),
    "gui.guhs.molentje.cmd.info": ("Maanmolentje: tandwieltjes %s, %s Blubs blazen, draait: %s.", "Maanmolentje: cogs %s, %s Blubs blowing, turning: %s."),
    "gui.guhs.molentje.cmd.geen": ("Het Maanmolentje is niet gevonden (ben je in Guhdromenland?).", "The Maanmolentje wasn't found (are you in Guhdromenland?)."),
}
for _id, (nl, en, lnl, len_) in NAMEN.items():
    TEKSTEN[f"block.guhs.{_id}"] = (nl, en)
    TEKSTEN[f"block.guhs.{_id}.lore"] = (lnl, len_)

BORDJES = {  # sign key suffix -> ([Dutch lines], [English lines]); at most 16 characters a line
    "welkom": (["~ Maanmolentje ~", "Stil! Hier", "worden dromen", "gemalen"], ["~ Moon Mill ~", "Shh! Dreams", "are ground", "here"]),
    "puzzel": (["Tandwieltjes:", "alle pijltjes", "omhoog = de", "molen draait!"], ["The cogs:", "all arrows", "up = the", "mill turns!"]),
    "puzzel2": (["Vadsige tandjes:", "eerst draaien,", "dan dutten"], ["Lazy little cogs:", "first turning,", "then napping"]),
    "kussens": (["Blaaskussentjes", "klik = een Blub", "komt blazen!", "(3 nodig)"], ["Blowing cushions", "click = a Blub", "comes to blow!", "(3 needed)"]),
    "meer": (["Niet voeren:", "ze eten alleen", "sterrenstof"], ["Don't feed them:", "they only eat", "stardust"]),
    "zand": (["Droomzand", "vers gemalen", "door de Zandguh"], ["Droomzand", "freshly ground", "by the Zandguh"]),
}
for _k, (_nl, _en) in BORDJES.items():
    for _i, (_a, _b) in enumerate(zip(_nl, _en)):
        TEKSTEN[f"{SIGN}.{_k}.{_i}"] = (_a, _b)

# the concept blocks of the render -> real blocks
CONCEPT = {"c_droomzand": "guhs:droomzandblok", "c_maanlampje": "guhs:maanlampje_maan[facing=north,lit=true]",
           "c_zeepbelglas": "guhs:zeepbelglas", "c_maanbloemetje": "guhs:maanbloemetje[open=true]"}


# ------------------------------------------------------------------------------------------------ a small Scene on a template
class Bouw:
    """The render's Scene calls (set/fill/air/cylinder/ellipsoid/is_air), onto a make_structures.Structure."""

    def __init__(self, ms):
        self.ms = ms
        self.s = ms.Structure((X, Y, Z))

    @staticmethod
    def blok(naam):
        if naam.split("[")[0] in CONCEPT:
            naam = CONCEPT[naam.split("[")[0]]
        props = {}
        if "[" in naam:
            naam, rest = naam[:-1].split("[", 1)
            for kv in rest.split(","):
                k, v = kv.split("=")
                props[k] = v
        if ":" not in naam:
            naam = "guhs:" + naam
        return naam, props

    def inb(self, x, y, z):
        return 0 <= x < X and 0 <= y < Y and 0 <= z < Z

    def set(self, x, y, z, b, roof=False, keep=False, nbt=None):
        x, y, z = int(x), int(y), int(z)
        if not self.inb(x, y, z) or (keep and not self.is_air(x, y, z)):
            return
        naam, props = self.blok(b)
        self.s.set(x, y, z, naam, props or None, nbt)

    def get(self, x, y, z):
        return self.s.get(int(x), int(y), int(z)) or "minecraft:air"

    def is_air(self, x, y, z):
        return self.get(x, y, z) == "minecraft:air"

    def fill(self, x1, y1, z1, x2, y2, z2, b, roof=False, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, b, keep=keep)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def cylinder(self, cx, cz, r, y1, y2, b, hollow=0.0, roof=False, keep=False):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                d = math.hypot(x - cx, z - cz)
                if d <= r and not (hollow and d <= r - hollow):
                    for y in range(y1, y2 + 1):
                        self.set(x, y, z, b, keep=keep)

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, b, hollow=0.0, roof=False, keep=False):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d > 1:
                        continue
                    if hollow and ((x - cx) / (rx - hollow)) ** 2 + ((y - cy) / (ry - hollow)) ** 2 + ((z - cz) / (rz - hollow)) ** 2 <= 1:
                        continue
                    self.set(x, y, z, b, keep=keep)

    def bordje(self, x, y, z, facing, sleutel):
        """A waxed birch wall sign with the four lines of BORDJES[sleutel] (translate keys with the Dutch as fallback)."""
        nl = BORDJES[sleutel][0]
        msgs = [json.dumps({"translate": f"{SIGN}.{sleutel}.{i}", "fallback": nl[i]}) if i < len(nl) else json.dumps("") for i in range(4)]
        leeg = [json.dumps("")] * 4
        nbt = {"id": "minecraft:sign", "is_waxed": self.ms.Byte(1),
               "front_text": {"messages": self.ms.NbtList(8, msgs), "color": "black", "has_glowing_text": self.ms.Byte(0)},
               "back_text": {"messages": self.ms.NbtList(8, leeg), "color": "black", "has_glowing_text": self.ms.Byte(0)}}
        self.s.set(int(x), int(y), int(z), "minecraft:birch_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)


def stairs(b, f, half="bottom"):
    return f"{b}[facing={f},half={half},shape=straight,waterlogged=false]"


# ------------------------------------------------------------------------------------------------ the island (render r06)
def eiland(S, cx, y, cz, r, diepte, top, laag, kern, punt="c_droomzand", rafel=0.18, seed=61):
    """droomkit.eiland: a floating island, a ragged rim and a droplet underneath; returns binnen(x, z)."""
    rng = np.random.default_rng(seed)
    fase = rng.random(4) * 2 * math.pi

    def rand(a):
        return r * (1 + rafel * (0.6 * math.sin(3 * a + fase[0]) + 0.4 * math.sin(5 * a + fase[1]) + 0.25 * math.sin(8 * a + fase[2])))

    def binnen(x, z):
        return math.hypot(x - cx, z - cz) <= rand(math.atan2(z - cz, x - cx))

    for x in range(int(cx - r * 1.5) - 1, int(cx + r * 1.5) + 2):
        for z in range(int(cz - r * 1.5) - 1, int(cz + r * 1.5) + 2):
            dx, dz = x - cx, z - cz
            d = math.hypot(dx, dz) / rand(math.atan2(dz, dx))
            if d > 1:
                continue
            h = int(diepte * (1 - d) ** 0.7 + rng.random() * 1.5) + 1
            for k in range(h + 1):
                b = top if k == 0 else laag if k <= 2 else punt if k >= h - 1 and d < 0.3 else kern
                S.set(x, y - k, z, b)
            if d > 0.82 and rng.random() < 0.12:
                onder = y - h
                for k in range(1, int(rng.integers(2, 5))):
                    S.set(x, onder - k, z, "wolkenweide_rank", keep=True)
    return binnen


def bouw_eiland(ms):
    S = Bouw(ms)
    rng = np.random.default_rng(6)
    plek = {}

    binnen = eiland(S, C, T, C, 28, diepte=22, top="maansteen", laag="pluiswolblok", kern="maansteen")
    for (kx, kz, kr) in ((26, 52, 3.2), (55, 22, 2.6), (30, 30, 2.2), (60, 50, 2.0), (22, 40, 1.8)):
        for x in range(int(kx - kr - 2), int(kx + kr + 3)):
            for z in range(int(kz - kr - 2), int(kz + kr + 3)):
                d = math.hypot(x - kx, z - kz)
                if not binnen(x, z):
                    continue
                if d <= kr:
                    S.set(x, T, z, "c_droomzand" if d < kr * 0.55 else "maansteen_tegels")
                    S.set(x, T - 1, z, "maansteen")
                elif d <= kr + 1.2:
                    S.set(x, T + 1, z, "maansteen_plaat[type=bottom,waterlogged=false]", keep=True)

    # the path from the steiger (towards the plein, compass 252) to the mill
    PLEIN = 252
    pdx, pdz = math.sin(math.radians(PLEIN)), -math.cos(math.radians(PLEIN))
    qx, qz = math.cos(math.radians(PLEIN)), math.sin(math.radians(PLEIN))
    MX, MZ = 46, 38
    wolk = []
    for k in np.arange(0, 34, 0.5):
        for w in (-1.5, -0.5, 0.5, 1.5):
            x, z = int(round(MX + pdx * k + qx * w)), int(round(MZ + pdz * k + qz * w))
            if binnen(x, z):
                S.set(x, T, z, "pleister_muisgrijs" if abs(w) < 1 else "maansteen_tegels")
                S.air(x, T + 1, z, x, T + 4, z)
            elif k > 20:
                S.set(x, T, z, "wolkenblok_wit_plaat[type=top,waterlogged=false]" if abs(w) < 1 else
                      "wolkenblok_roze_plaat[type=top,waterlogged=false]", keep=True)
                if abs(w) < 1:
                    wolk.append((k, x, z))
    ex, ez = MX + pdx * 37, MZ + pdz * 37
    S.ellipsoid(ex, T + 3, ez, 2.6, 2.6, 2.6, "c_zeepbelglas", hollow=1)
    # the steiger: the far end of the cloud path (step on it to float to the plein)
    k, sx, sz = max(wolk)
    plek["steiger"] = [sx + 0.5 - C, 1.0, sz + 0.5 - C]
    # the welcome gate
    gx, gz = MX + pdx * 24, MZ + pdz * 24
    for s in (-3, 3):
        x, z = int(round(gx + qx * s)), int(round(gz + qz * s))
        S.fill(x, T + 1, z, x, T + 4, z, "pleister_muisgrijs")
        S.set(x, T + 5, z, "c_maanlampje")
    for s in np.arange(-3, 3.5, 0.5):
        x, z = int(round(gx + qx * s)), int(round(gz + qz * s))
        S.set(x, T + 5, z, "maansteen_tegels", keep=True)
    S.set(int(round(gx)), T + 6, int(round(gz)), "knuffelsteen_gezicht[facing=west,stemming=1]")
    bx, bz = int(round(gx - qx * 4 - pdx)), int(round(gz - qz * 4 - pdz))
    S.set(bx, T + 1, bz, "pleister_muisgrijs")
    S.bordje(bx - 1, T + 1, bz, "west", "welkom")
    # arrival: on the path just inside the gate, looking at the mill
    ax, az = MX + pdx * 20, MZ + pdz * 20
    yaw = math.degrees(math.atan2(-(MX - ax), MZ - az)) % 360
    plek["aankomst"] = {"plek": [round(ax - C, 2), 1.0, round(az - C, 2)], "yaw": round(yaw, 1)}

    # ---------------------------------------------------------------- the mill: a round tower narrowing upwards
    RING = [(0, 6.4), (4, 5.9), (8, 5.3), (12, 4.8), (15, 4.4), (18, 4.1)]

    def straal(h):
        for (h0, r0), (h1, r1) in zip(RING, RING[1:]):
            if h0 <= h <= h1:
                return r0 + (r1 - r0) * (h - h0) / (h1 - h0)
        return RING[-1][1]

    S.cylinder(MX, MZ, 7.4, T, T, "maansteen_tegels")
    S.cylinder(MX, MZ, 7.4, T, T, "tegel_guhgezicht", hollow=1)
    HT = 18
    for h in range(HT):
        r = straal(h)
        b = "maansteen" if h < 3 else ("pleister_muisgrijs" if (h // 3) % 2 else "pleister_wolkje")
        S.cylinder(MX, MZ, r, T + 1 + h, T + 1 + h, b, hollow=1)
        S.cylinder(MX, MZ, r - 1.2, T + 1 + h, T + 1 + h, "minecraft:air")
    for k in range(7):
        S.cylinder(MX, MZ, 5.0 - k * 0.75, T + 1 + HT + k, T + 1 + HT + k,
                   "pluisdak_plaat[type=double,waterlogged=false]" if k < 6 else "pleister_lavendel")
    S.set(MX, T + HT + 8, MZ, "pleister_boter")
    S.set(MX, T + HT + 9, MZ, "c_maanlampje")
    for ox in (-3, 3):
        S.ellipsoid(MX + ox, T + HT + 4, MZ, 1.2, 2.2, 1.6, "pleister_roze")
        S.set(MX + ox, T + HT + 4, MZ + 1, "pleister_framboos")
    S.air(MX, T + 1, MZ + 6, MX, T + 3, MZ + 6)
    S.air(MX, T + 1, MZ + 5, MX, T + 3, MZ + 5)
    S.set(MX, T + 5, MZ + 6, "knuffelsteen_gezicht[facing=south,stemming=2]")
    for (wx, wz, h) in ((MX - 5, MZ, 6), (MX + 5, MZ, 6), (MX, MZ - 5, 6), (MX - 4, MZ, 11), (MX + 4, MZ, 11),
                        (MX, MZ - 4, 11), (MX, MZ + 4, 11)):
        S.fill(wx, T + h, wz, wx, T + h + 1, wz, "c_zeepbelglas")
    S.set(MX, T + 9, MZ - 5, "knuffelsteen_gezicht[facing=north,stemming=0]")
    S.cylinder(MX, MZ, 2, T + 1, T + 1, "maansteen")
    S.set(MX, T + 2, MZ, "c_droomzand")
    # the axle and the sails: ONE block (molenwieken) whose renderer draws the four big sails, turning or still
    AY, AZ = T + 15, MZ + 5
    S.set(MX, AY, AZ, "golfbaan_molenas[facing=south]")
    S.set(MX, AY, AZ + 1, "guhs:molenwieken[facing=south,draait=false]", nbt={"id": "guhs:molenwieken"})
    plek["wieken"] = [MX - C, AY - T, AZ + 1 - C]

    # ---------------------------------------------------------------- the three blowing cushions in front of the mill
    kussens = []
    for (kx, kz) in ((39, 52), (45, 54), (52, 52)):
        S.fill(kx - 1, T, kz - 1, kx + 1, T, kz + 1, "maansteen_tegels")
        S.set(kx, T + 1, kz, "guhs:blaaskussentje")
        kussens.append([kx - C, 1, kz - C])
    plek["kussentjes"] = kussens
    S.set(36, T + 1, 55, "pleister_muisgrijs")
    S.bordje(36, T + 1, 56, "south", "kussens")

    # ---------------------------------------------------------------- the cog puzzle: a wall west of the mill
    PX, PZ = 31, 40
    S.fill(PX - 6, T + 1, PZ, PX + 6, T + 9, PZ, "maansteen")
    S.fill(PX - 6, T + 1, PZ, PX + 6, T + 1, PZ, "maansteen_tegels")
    for x in range(PX - 7, PX + 8):
        S.set(x, T + 10, PZ, stairs("maansteen_trap", "north"))
        S.set(x, T + 10, PZ + 1, stairs("maansteen_trap", "south"))
    GEARS = [(PX - 3, T + 6, 2.4, "pleister_boter", 0), (PX + 1, T + 3, 1.8, "pleister_lavendel", 22),
             (PX + 4, T + 7, 1.5, "pleister_boter", 10), (PX - 1, T + 9, 1.2, "pleister_framboos", 45)]
    tandwielen = []
    for (gx_, gy_, gr, kleur, rot) in GEARS:
        for x in range(int(gx_ - gr - 2), int(gx_ + gr + 3)):
            for y in range(int(gy_ - gr - 2), int(gy_ + gr + 3)):
                if y <= T or y > T + 9:
                    continue
                d = math.hypot(x - gx_, y - gy_)
                hoek = (math.degrees(math.atan2(y - gy_, x - gx_)) + rot) % 45
                if d <= gr or (d <= gr + 1.1 and hoek < 18):
                    S.set(x, y, PZ + 1, kleur)
        S.set(gx_, gy_, PZ + 1, "guhs:molen_tandwiel[facing=south,stand=0]")
        tandwielen.append([gx_ - C, gy_ - T, PZ + 1 - C])
    plek["tandwielen"] = sorted(tandwielen)   # the chain runs left to right (west to east, as you face the wall): each one turns the next along
    S.set(PX - 4, T + 1, PZ + 2, "einde2_bouw_lucht_tandwiel")
    S.set(PX + 5, T + 1, PZ + 2, "einde2_bouw_lucht_tandwiel")
    S.set(PX, T + 1, PZ + 2, "guh_tafel[facing=south]")
    S.set(PX - 6, T + 2, PZ + 1, "pleister_muisgrijs")
    S.bordje(PX - 6, T + 2, PZ + 2, "south", "puzzel")
    S.bordje(PX + 6, T + 2, PZ + 2, "south", "puzzel2")
    for x in range(PX - 6, PX + 7):
        for y in range(T + 2, T + 10):
            if (x + y) % 4 == 0:
                S.set(x, y, PZ, "pleister_lavendel")
    for x in (PX - 3, PX + 3):
        S.set(x, T + 5, PZ - 1, "knuffelsteen_gezicht[facing=north,stemming=3]")
    S.set(PX, T + 1, PZ - 1, "c_maanlampje")
    S.set(PX, T + 8, PZ - 1, "sterrenlantaarn")
    for x in range(PX + 6, MX - 5):
        S.set(x, T + 7, PZ, "pleister_drop", keep=True)

    # ---------------------------------------------------------------- the Zandguh, bags of droomzand and the chute
    ZX, ZZ = 55, 43
    plek["zandguh"] = {"plek": [ZX + 0.5 - C, 1.0, ZZ + 0.5 - C], "yaw": 225.0}
    S.set(ZX + 1, T + 1, ZZ - 1, "light_gray_zitzak[facing=west]")
    for (x, z, b) in ((58, 37, "white_zitzak"), (59, 38, "light_gray_zitzak"), (58, 39, "white_zitzak"),
                      (60, 36, "light_gray_zitzak"), (59, 36, "white_zitzak")):
        S.set(x, T + 1, z, f"{b}[facing=west]")
    S.set(58, T + 2, 38, "white_zitzak[facing=west]")
    S.set(59, T + 2, 37, "light_gray_zitzak[facing=west]")
    for k, x in enumerate(range(MX + 5, MX + 10)):
        S.set(x, T + 6 - k, MZ + 1, stairs("maansteen_trap", "west"))
    S.fill(MX + 9, T + 1, MZ - 1, MX + 12, T + 1, MZ + 3, "maansteen_tegels")
    S.fill(MX + 10, T + 1, MZ, MX + 11, T + 1, MZ + 2, "c_droomzand")
    for (x, y, z) in ((MX + 10, T + 2, MZ + 1), (MX + 11, T + 2, MZ + 1), (MX + 10, T + 2, MZ)):
        S.set(x, y, z, "c_droomzand")
    plek["goot"] = [MX + 10.5 - C, 3.0, MZ + 1.5 - C]
    S.set(MX + 13, T + 1, MZ + 1, "pleister_muisgrijs")
    S.bordje(MX + 13, T + 1, MZ + 2, "south", "zand")

    # ---------------------------------------------------------------- the moon pond with sterrenvisjes (real water)
    LX, LZ = 58, 56
    MEER = set()
    for x in range(LX - 9, LX + 10):
        for z in range(LZ - 7, LZ + 8):
            d = ((x - LX) / 7.5) ** 2 + ((z - LZ) / 5.8) ** 2
            if d <= 1 and binnen(x, z):
                S.set(x, T, z, "minecraft:water[level=0]")
                MEER.add((x, z))
                S.set(x, T - 1, z, "minecraft:water[level=0]" if d < 0.5 else "maansteen")
                S.set(x, T - 2, z, "maansteen")
            elif d <= 1.35 and binnen(x, z):
                S.set(x, T, z, "maansteen_tegels")
    visjes = sorted(MEER, key=lambda p: (p[0] * 7 + p[1] * 13) % 17)[:6]
    plek["visjes"] = [[x + 0.5 - C, 0.3, z + 0.5 - C] for (x, z) in visjes]
    for x in range(LX - 3, LX + 1):
        S.set(x, T + 1, LZ - 6, "maansteen_plaat[type=bottom,waterlogged=false]")
    S.set(LX + 7, T + 1, LZ - 7, "pleister_muisgrijs")
    S.bordje(LX + 7, T + 1, LZ - 6, "south", "meer")

    # ---------------------------------------------------------------- lamps, little fences, nests, flowers
    for a in range(0, 360, 30):
        x, z = int(round(MX + 9.5 * math.cos(math.radians(a)))), int(round(MZ + 9.5 * math.sin(math.radians(a))))
        if binnen(x, z) and S.is_air(x, T + 1, z) and not S.is_air(x, T, z):
            S.set(x, T + 1, z, "c_maanlampje")
    for x in range(X):
        for z in range(Z):
            if binnen(x, z) and not all(binnen(x + a, z + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                if S.is_air(x, T + 1, z) and not S.is_air(x, T, z) and (x, z) not in MEER:
                    S.set(x, T + 1, z, "guh_hekje_wit")
    for (x, z) in ((24, 34), (33, 58)):
        S.set(x, T + 1, z, "guhnestje")
        S.set(x + 1, T + 1, z + 1, "c_maanlampje")
    brng = np.random.default_rng(2)
    for x in range(X):
        for z in range(Z):
            if binnen(x, z) and S.is_air(x, T + 1, z) and not S.is_air(x, T, z) and (x, z) not in MEER and brng.random() < 0.06:
                S.set(x, T + 1, z, ("c_maanbloemetje", "c_maanbloemetje", "pluisgras")[brng.integers(0, 3)])
    # where the island's Blubs hop around (open grass in front of the mill and around the pond)
    plek["blubs"] = [[x - C + 0.5, 1.0, z - C + 0.5] for (x, z) in ((36, 47), (41, 58), (49, 59), (27, 46), (64, 30), (30, 52), (60, 44))]

    # ---------------------------------------------------------------- the sleeping crescent moon in the sky (north-east)
    SX, SY, SZ = 70, 50, 18
    for x in range(SX - 9, SX + 10):
        for y in range(SY - 9, SY + 10):
            d1 = math.hypot(x - SX, y - SY)
            d2 = math.hypot(x - (SX + 4), y - (SY + 2))
            if d1 <= 8.5 and d2 > 7:
                S.set(x, y, SZ, "pleister_boter")
                S.set(x, y, SZ + 1, "pleister_vanille")
    for (x, y) in ((SX - 5, SY + 1), (SX - 4, SY + 1)):
        S.set(x, y, SZ + 2, "pleister_drop")
        S.set(x, y, SZ - 1, "pleister_drop")
    S.set(SX - 6, SY - 2, SZ + 2, "pleister_framboos")
    S.set(SX - 6, SY - 2, SZ - 1, "pleister_framboos")
    for (x, y, z) in ((12, 22, 20), (72, 24, 70), (14, 40, 66), (36, 46, 10)):
        S.ellipsoid(x, y, z, 3.4, 1.3, 2.2, "wolkenblok_wit")
    return S, plek


# ------------------------------------------------------------------------------------------------ textures
def textures():
    T_ = {}
    # the cog: a round cog on maansteen, a big arrow pointing up (the model turns the face)
    img = Image.new("RGBA", (16, 16), (196, 196, 214, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(150, 150, 172, 255))
    for a in range(0, 360, 45):
        x, y = 7.5 + 6.2 * math.cos(math.radians(a)), 7.5 + 6.2 * math.sin(math.radians(a))
        d.rectangle([x - 1.2, y - 1.2, x + 1.2, y + 1.2], fill=(250, 214, 120, 255))
    d.ellipse([2, 2, 13, 13], fill=(250, 214, 120, 255), outline=(206, 160, 70, 255))
    d.ellipse([5, 5, 10, 10], fill=(255, 238, 180, 255))
    d.polygon([(7.5, 1.5), (11, 6), (9, 6), (9, 10), (6, 10), (6, 6), (4, 6)], fill=(232, 96, 150, 255))
    d.point([(7, 12), (8, 12)], fill=(90, 50, 80, 255))
    T_["molen_tandwiel"] = img
    zij = Image.new("RGBA", (16, 16), (206, 160, 70, 255))
    T_["molen_tandwiel_zij"] = zij
    # the blowing cushion: a round lavender cushion with bubbles
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, 15, 15], fill=(206, 186, 246, 255), outline=(160, 140, 214, 255))
    for (x, y, r) in ((5, 5, 2), (10, 8, 1.5), (6, 11, 1.2), (11, 4, 1)):
        d.ellipse([x - r, y - r, x + r, y + r], outline=(255, 255, 255, 255))
        d.point([(x - r / 2, y - r / 2)], fill=(255, 255, 255, 255))
    T_["blaaskussentje_top"] = img
    T_["blaaskussentje_zij"] = Image.new("RGBA", (16, 16), (176, 156, 226, 255))
    # the sails: pink and vanilla stripes on a dark frame; the hub: a guh face
    img = Image.new("RGBA", (16, 16), (255, 238, 206, 255))
    d = ImageDraw.Draw(img)
    for i in range(0, 16, 4):
        d.rectangle([0, i, 15, i + 1], fill=(255, 182, 212, 255))
    d.rectangle([0, 0, 15, 15], outline=(110, 80, 110, 255))
    T_["wiek_zeil"] = img
    T_["wiek_hout"] = Image.new("RGBA", (16, 16), (110, 80, 110, 255))
    img = Image.new("RGBA", (16, 16), (236, 232, 246, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(150, 150, 172, 255))
    d.rectangle([4, 6, 5, 7], fill=(40, 30, 50, 255))
    d.rectangle([10, 6, 11, 7], fill=(40, 30, 50, 255))
    d.point([(4, 6), (10, 6)], fill=(255, 255, 255, 255))
    d.rectangle([3, 9, 4, 9], fill=(255, 160, 190, 255))
    d.rectangle([11, 9, 12, 9], fill=(255, 160, 190, 255))
    d.point([(7, 9), (8, 9)], fill=(232, 96, 150, 255))
    d.line([(6, 11), (7, 12), (8, 11), (9, 12)], fill=(90, 50, 80, 255))
    T_["wiek_naaf"] = img
    return T_


def E(frm, to, faces, rot=0):
    if isinstance(faces, str):
        faces = {s: faces for s in ("down", "up", "north", "south", "west", "east")}
    out = {}
    for s, t in faces.items():
        f = {"texture": f"#{t}"}
        if rot and s == "south":
            f["rotation"] = rot
        out[s] = f
    return {"from": frm, "to": to, "faces": out}


def model(h, naam, elements, textures, parent="minecraft:block/block"):
    tx = {k: f"guhs:block/molentje/{v}" for k, v in textures.items()}
    tx.setdefault("particle", next(iter(tx.values())))
    h.w(f"{h.A}/models/block/{naam}.json", {"parent": parent, "textures": tx, "elements": elements})


FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def build(h, ms):
    for naam, img in textures().items():
        h.save(img, "block", "molentje", f"{naam}.png")
    A = h.A
    # the cog: a flat disc on the wall (its arrow side faces out = the block's facing), four models (face rotation per stand)
    for stand in range(4):
        model(h, f"molen_tandwiel_{stand}", [
            E([1, 1, 14], [15, 15, 16], {"south": "voor", "north": "zij", "up": "zij", "down": "zij", "west": "zij", "east": "zij"},
              rot=90 * stand)], {"voor": "molen_tandwiel", "zij": "molen_tandwiel_zij"})
    variants = {}
    for f, y in FACINGS:
        for stand in range(4):
            variants[f"facing={f},stand={stand}"] = {"model": f"guhs:block/molen_tandwiel_{stand}", **({"y": (y + 180) % 360} if (y + 180) % 360 else {})}
    h.w(f"{A}/blockstates/molen_tandwiel.json", {"variants": variants})
    h.w(f"{A}/models/item/molen_tandwiel.json", {"parent": "guhs:block/molen_tandwiel_0"})
    # the cushion
    model(h, "blaaskussentje", [E([2, 0, 2], [14, 3, 14], {"up": "top", "down": "zij", "north": "zij", "south": "zij", "west": "zij", "east": "zij"}),
                                E([4, 3, 4], [12, 4, 12], {"up": "top", "north": "zij", "south": "zij", "west": "zij", "east": "zij"})],
          {"top": "blaaskussentje_top", "zij": "blaaskussentje_zij"})
    h.w(f"{A}/blockstates/blaaskussentje.json", {"variants": {"": {"model": "guhs:block/blaaskussentje"}}})
    h.w(f"{A}/models/item/blaaskussentje.json", {"parent": "guhs:block/blaaskussentje"})
    # the sails: the block itself is the hub (a guh face); the four sails are a standalone model the renderer scales up
    model(h, "molenwieken", [E([4, 4, 0], [12, 12, 8], {"south": "naaf", "north": "hout", "up": "hout", "down": "hout", "west": "hout", "east": "hout"})],
          {"naaf": "wiek_naaf", "hout": "wiek_hout"})
    zeil = {"north": "zeil", "south": "zeil", "up": "hout", "down": "hout", "west": "hout", "east": "hout"}
    hout = "hout"
    model(h, "molenwieken_draai", [
        E([7, 9, 7.5], [9, 32, 8.5], hout), E([9, 14, 7.6], [15, 31, 8.4], zeil),        # up
        E([9, 7, 7.5], [32, 9, 8.5], hout), E([14, 1, 7.6], [31, 7, 8.4], zeil),         # right
        E([7, -16, 7.5], [9, 7, 8.5], hout), E([1, -15, 7.6], [7, 2, 8.4], zeil),        # down
        E([-16, 7, 7.5], [7, 9, 8.5], hout), E([-15, 9, 7.6], [2, 15, 8.4], zeil),       # left
        E([6, 6, 7], [10, 10, 9], {"south": "naaf", "north": "hout", "up": "hout", "down": "hout", "west": "hout", "east": "hout"}),
    ], {"zeil": "wiek_zeil", "hout": "wiek_hout", "naaf": "wiek_naaf"})
    variants = {}
    for f, y in FACINGS:
        for draait in ("true", "false"):
            variants[f"draait={draait},facing={f}"] = {"model": "guhs:block/molenwieken", **({"y": (y + 180) % 360} if (y + 180) % 360 else {})}
    h.w(f"{A}/blockstates/molenwieken.json", {"variants": variants})
    h.w(f"{A}/models/item/molenwieken.json", {"parent": "guhs:block/molenwieken_draai", "display": {
        "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.32, 0.32, 0.32]},
        "ground": {"scale": [0.2, 0.2, 0.2]}, "fixed": {"scale": [0.35, 0.35, 0.35]},
        "thirdperson_righthand": {"scale": [0.15, 0.15, 0.15]}, "firstperson_righthand": {"scale": [0.15, 0.15, 0.15]}}})
    for n in NAMEN:
        h.self_drop(n)
    h.shaped("molen_tandwiel", [" G ", "GCG", " G "], {"G": "minecraft:gold_nugget", "C": "minecraft:copper_ingot"}, "guhs:molen_tandwiel")
    h.shaped("blaaskussentje", ["WSW"], {"W": "minecraft:purple_wool", "S": "minecraft:slime_ball"}, "guhs:blaaskussentje")
    h.shaped("molenwieken", ["W W", " S ", "W W"], {"W": "minecraft:pink_wool", "S": "minecraft:stick"}, "guhs:molenwieken")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:molen_tandwiel"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:molenwieken"])
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)

    # the island template + its jsons
    S, plek = bouw_eiland(ms)
    S.s.save("dromen/eiland_molentje")
    pad = os.path.join(ms.OUT, "dromen", "eiland_molentje.nbt")
    with open(pad, "rb") as f:
        data = gzip.decompress(f.read())
    with open(pad, "wb") as f:
        f.write(gzip.compress(data, mtime=0))
    D = os.path.join("src", "main", "resources", "data", "guhs", "dromen")
    os.makedirs(os.path.join(D, "eiland"), exist_ok=True)
    with open(os.path.join(D, "eiland", "molentje.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"versie": VERSIE, "template": "guhs:dromen/eiland_molentje", "midden": list(MIDDEN),
                   "aankomst": plek["aankomst"], "steiger": plek["steiger"]}, f, indent=2)
        f.write("\n")
    with open(os.path.join(D, "molentje_plekken.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: plek[k] for k in ("tandwielen", "kussentjes", "wieken", "zandguh", "goot", "blubs", "visjes")}, f, indent=2)
        f.write("\n")
    selfcheck(h, plek)


def selfcheck(h, plek):
    problems = []
    for n in NAMEN:
        for p in (f"{h.A}/blockstates/{n}.json", f"{h.A}/models/item/{n}.json", f"{h.D}/recipe/{n}.json", f"{h.D}/loot_table/blocks/{n}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    if len(plek["tandwielen"]) != 4 or len(plek["kussentjes"]) != 3:
        problems.append("puzzle spots")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    for k, (nl, en) in BORDJES.items():
        for line in nl:
            if len(line) > 16:
                problems.append(f"sign {k}: '{line}' is longer than 16")
    if problems:
        raise SystemExit("molentje self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    import make_structures as ms
    build(h, ms)
    en_pad = os.path.join("tools", "lang", "en", "c213_molentje.json")
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
    for n in NAMEN:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"molentje: island template, {len(NAMEN)} blocks, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
