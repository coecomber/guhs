"""
1.8.0 Guhdromenland, island "liedjes" (dromen-ontwerp/BOUWPLAN_180.md part E; design ontwerp-guhs180-dromen.md): de
Slaapliedjesheuvels, home of the Blub-guhs, where Snoes' slaapliedje is found with the Blub-koortje. Java:
feature/dromen/liedjes (BlubKoortje).

The island follows the approved render dromen-ontwerp/_bron/r02_slaapliedjesheuvels.py (its Scene code, with the concept
blocks swapped for the real ones): soft lavender moon meadows (slaapliedjesgras), five klokjesheuvels (do re mi fa sol) with
a podium in the note colour and a giant bell flower, Blub-holletjes in the hills, a maanmeertje (droomzee), a reuzen-speeldoos,
a trail of floating notes, cherry trees with lampions, nests, and the steiger back to the Droomplein (108 degrees: the plein
lies there seen from this island).

  build()     data/guhs/structure/dromen/eiland_liedjes.nbt (100 x 56 x 100, centre spot = template (50, 24, 50)) and
              data/guhs/dromen/eiland/liedjes.json (DroomEilanden: midden, aankomst, steiger; plus "koor": the standing spot
              on each podium, relative to the centre, read by BlubKoortje), the sign texts (nl + en)

Standalone (from the project root): PYTHONPATH=tools python tools/features/eiland_liedjes.py
"""
import json
import math
import os
import random
import re
import sys

sys.path.insert(0, "tools")
import make_structures as ms  # noqa: E402
import sign_text  # noqa: E402

X, Y, Z = 100, 56, 100
C = 50                          # centre (x and z)
T = 24                          # base top of the island
VERSIE = 2   # 2 = playtest fixes: walkable podium hills, no fence on the way to the steiger
PREFIX = "sign.guhs.dromen.liedjes"
NOTEN = ["do", "re", "mi", "fa", "sol"]

# concept blocks of the render -> the real ones
ECHT = {
    "c_slaapliedjesgras": "guhs:slaapliedjesgras",
    "c_maanbloemetje": "guhs:maanbloemetje",
    "c_maanlampje": "guhs:maanlampje_maan",
    "c_droomzee": "guhs:droomzee",
    "c_droomzand": "guhs:droomzandblok",
    "c_zeepbelglas": "guhs:zeepbelglas",
    "c_brievenbus_liedjes": "guhs:bzg_brievenbus",
}
# English for the sign lines (the Dutch is the key's text)
EN = {
    "Slaapliedjes-": "Lullaby", "heuvels": "Hills", "Sst! Hier slapen": "Shh! 12 Blub-guhs", "12 Blub-guhs": "sleep here",
    "(en 1 vadsguh)": "(and 1 vadsguh)", "Hier woont Blub": "Blub lives here", "Niet aanbellen,": "Don't ring the bell,",
    "njeg!": "njeg!", "Niet opwinden": "Don't wind up", "na 8 uur.": "after 8 o'clock.", "Vahoeg.": "Vahoeg.",
    "Het Koortje": "The Little Choir", "Breng elke Blub": "Bring every Blub", "naar zijn eigen": "to its very own",
    "klokjesheuvel!": "bell hill!", "(sluip, ze zijn": "(sneak, they are", "verlegen)": "shy)",
    "do": "do", "re": "re", "mi": "mi", "fa": "fa", "sol": "sol", "~ heuvel ~": "~ hill ~",
    "Naar het": "To the", "Droomplein": "Dream Square", "Stap op de": "Step on the", "steiger en zweef": "pier and float",
}


class Scene:
    """The render kit's Scene, but writing into a make_structures.Structure (block strings 'name[a=b]' -> name + props)."""

    def __init__(self):
        self.s = ms.Structure((X, Y, Z))

    @staticmethod
    def _staat(s):
        m = re.match(r"^([^\[]+)(?:\[(.*)\])?$", s)
        naam, props = m.group(1), {}
        if m.group(2):
            for kv in m.group(2).split(","):
                k, v = kv.split("=")
                props[k] = v
        naam = ECHT.get(naam, naam)
        if ":" not in naam:
            naam = "guhs:" + naam
        return naam, props

    def set(self, x, y, z, s, keep=False):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if keep and not self.is_air(x, y, z):
            return
        naam, props = self._staat(s)
        self.s.set(x, y, z, naam, props)

    def is_air(self, x, y, z):
        b = self.s.get(int(round(x)), int(round(y)), int(round(z)))
        return b is None or b == "minecraft:air"

    def fill(self, x1, y1, z1, x2, y2, z2, s, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, s, keep)

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, s, keep=False, hollow=0):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
                for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d <= 1:
                        if hollow and d < (1 - hollow / max(rx, ry, rz)) ** 2:
                            continue
                        self.set(x, y, z, s, keep)

    def sign(self, x, y, z, facing, lines, wall=False):
        nbt = {"id": "minecraft:sign", "is_waxed": ms.Byte(1),
               "front_text": {"messages": sign_text.messages(PREFIX, lines), "color": "purple", "has_glowing_text": ms.Byte(1)},
               "back_text": {"messages": sign_text.messages(PREFIX, ["", "", "", ""]), "color": "black", "has_glowing_text": ms.Byte(0)}}
        rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
        if wall:
            self.s.set(x, y, z, "minecraft:cherry_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            self.s.set(x, y, z, "minecraft:cherry_sign", {"rotation": rot, "waterlogged": "false"}, nbt)


def eiland(S, cx, y, cz, r, diepte, top, seed, rafel=0.18, laag="pluiswolblok", kern="knuffelsteen", punt="c_droomzand"):
    """The render kit's floating island (droomkit.eiland): a frayed rim, the underside a drop narrowing to a point."""
    rng = random.Random(seed)
    fase = [rng.random() * 2 * math.pi for _ in range(4)]

    def rand(a):
        return r * (1 + rafel * (0.6 * math.sin(3 * a + fase[0]) + 0.4 * math.sin(5 * a + fase[1]) + 0.25 * math.sin(8 * a + fase[2])))

    def binnen(x, z):
        dx, dz = x - cx, z - cz
        return math.hypot(dx, dz) <= rand(math.atan2(dz, dx))

    for x in range(int(cx - r * 1.5) - 1, int(cx + r * 1.5) + 2):
        for z in range(int(cz - r * 1.5) - 1, int(cz + r * 1.5) + 2):
            dx, dz = x - cx, z - cz
            d = math.hypot(dx, dz) / rand(math.atan2(dz, dx))
            if d > 1:
                continue
            h = int(diepte * (1 - d) ** 0.7 + rng.random() * 1.5) + 1
            for k in range(h + 1):
                yy = y - k
                if yy < 0:
                    break
                S.set(x, yy, z, top if k == 0 else laag if k <= 2 else punt if k >= h - 1 and d < 0.3 else kern)
            if d > 0.82 and rng.random() < 0.12:
                onder = y - h
                for k in range(1, rng.randint(2, 4)):
                    if onder - k >= 0:
                        S.set(x, onder - k, z, "wolkenweide_rank[tip=false]", keep=True)
    return binnen


def bouw():
    S = Scene()
    rng = random.Random(288)
    binnen = eiland(S, C, T, C, 38, diepte=24, top="c_slaapliedjesgras", seed=288, rafel=0.15)
    PA = math.radians(108)                              # the plein, seen from this island
    PDX, PDZ = math.sin(PA), -math.cos(PA)

    # --- rolling hills (height field above T); the five podium hills get a bell flower and a colour
    HEUVELS = [(30, 30, 12, 12, "do"), (55, 22, 11, 10, "re"), (74, 40, 10, 8, "mi"), (66, 66, 12, 11, "fa"),
               (36, 68, 11, 9, "sol"), (47, 46, 13, 4, None)]
    H = {}
    for x in range(X):
        for z in range(Z):
            if not binnen(x, z):
                continue
            h = 0.0
            for (hx, hz, hr, hh, _) in HEUVELS:
                d = math.hypot(x - hx, z - hz) / hr
                if d < 1:
                    h = max(h, hh * (0.5 + 0.5 * math.cos(math.pi * d)))
            if math.hypot(x - C, z - C) > 30:
                h *= min(1.0, (38 - math.hypot(x - C, z - C)) / 8)
            h = max(int(round(h + 0.4 * math.sin(x * 0.7) * math.cos(z * 0.6))), 0)
            for y in range(T, T + h):
                S.set(x, y, z, "pluiswolblok")
            S.set(x, T + h, z, "c_slaapliedjesgras")
            H[(x, z)] = T + h

    def vloer(x, z):
        return H.get((int(round(x)), int(round(z))), T)

    KLEUR = {"do": "knuffelblok_roze", "re": "knuffelblok_boter", "mi": "knuffelblok_aqua", "fa": "knuffelblok_lavendel",
             "sol": "knuffelblok_hemelsblauw"}
    koor = {}
    for (hx, hz, hr, hh, noot) in HEUVELS:
        if not noot:
            continue
        top = vloer(hx, hz) - 1                              # (playtest fix: one lower than the hilltop)
        # (playtest fix) a walkable cone round the podium: going outwards the ground drops at most one block per block,
        # so a survival player and a Blub can walk up (the bare hill was a jump of 2-3 blocks onto the podium)
        for x in range(hx - hr - 2, hx + hr + 3):
            for z in range(hz - hr - 2, hz + hr + 3):
                r = math.hypot(x - hx, z - hz)
                if r <= 3.2 or not binnen(x, z):
                    continue
                nodig = top - math.ceil(r - 3.2)
                oud = vloer(x, z)
                if (x, z) in H and nodig > oud:
                    for y in range(oud, nodig):
                        S.set(x, y, z, "pluiswolblok")
                    S.set(x, nodig, z, "c_slaapliedjesgras")
                    H[(x, z)] = nodig
        for x in range(hx - 3, hx + 4):
            for z in range(hz - 3, hz + 4):
                r = math.hypot(x - hx, z - hz)
                if r <= 3.2:
                    for y in range(vloer(x, z) + 1, top + 1):
                        S.set(x, y, z, "pluiswolblok")
                    for y in range(top + 1, top + 4):                 # nothing growing on the podium
                        S.set(x, y, z, "minecraft:air")
                    S.set(x, top, z, "maansteen_tegels" if r > 1.9 else (KLEUR[noot] if r > 0.5 else "tegel_guhgezicht"))
                    H[(x, z)] = top
        koor[noot] = [hx - C, top + 1 - T, hz - C]          # where its Blub stands (feet), relative to the centre
        kx, kz = hx + 1, hz - 2                              # the giant bell flower behind the podium
        S.fill(kx, top + 1, kz, kx, top + 4, kz, "minecraft:moss_block")
        S.set(kx + 1, top + 2, kz, "minecraft:big_dripleaf[facing=east,tilt=none,waterlogged=false]", keep=True)
        for x in range(kx - 1, kx + 2):
            for z in range(kz - 1, kz + 2):
                S.set(x, top + 6, z, KLEUR[noot])
                if (x, z) != (kx, kz):
                    S.set(x, top + 5, z, KLEUR[noot])
        S.set(kx, top + 5, kz, "minecraft:air")
        S.set(kx, top + 7, kz, "minecraft:moss_block")
        for (x, z) in ((kx - 2, kz), (kx + 2, kz), (kx, kz - 2), (kx, kz + 2)):
            S.set(x, top + 5, z, KLEUR[noot])
        S.set(kx, top + 4, kz, "minecraft:amethyst_cluster[facing=down,waterlogged=false]")
        S.set(hx - 2, top + 1, hz, "minecraft:note_block")
        S.set(hx - 2, top + 2, hz, "minecraft:bell[attachment=floor,facing=east,powered=false]")
        S.set(hx + 2, top + 1, hz + 1, "c_maanlampje[facing=south,lit=true]")
        S.sign(hx + 2, top + 1, hz - 2, "south", ["", noot, "~ heuvel ~", ""])
        for a in (45, 135, 225, 315):
            x, z = int(round(hx + 3.6 * math.cos(math.radians(a)))), int(round(hz + 3.6 * math.sin(math.radians(a))))
            if binnen(x, z):
                S.set(x, vloer(x, z) + 1, z, "minecraft:amethyst_cluster[facing=up,waterlogged=false]", keep=True)

    # --- the trail of floating notes between the hills
    spoor = [(30, 30), (55, 22), (74, 40), (66, 66), (36, 68)]
    for (a, b) in zip(spoor, spoor[1:]):
        for t in (0.15, 0.325, 0.5, 0.675, 0.85):
            x, z = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
            y = max(vloer(x, z), T) + 3 + int(2 * math.sin(t * math.pi))
            S.set(x, y, z, "zeepbel" if rng.random() < 0.6 else "minecraft:amethyst_cluster[facing=up,waterlogged=false]", keep=True)

    # --- the reuzen-speeldoos on the low middle hill
    mx, mz = 47, 46
    top = vloer(mx, mz)
    for x in range(mx - 5, mx + 6):
        for z in range(mz - 3, mz + 4):
            rand = x in (mx - 5, mx + 5) or z in (mz - 3, mz + 3)
            for y in range(top + 1, top + 4):
                S.set(x, y, z, "pleister_lavendel" if rand else "minecraft:air")
            S.set(x, top + 1, z, "pleister_lavendel" if rand else "maansteen")
    for x in range(mx - 4, mx + 5):
        S.set(x, top + 2, mz, "maansteen")
        if (x * 7) % 3 == 0:
            S.set(x, top + 3, mz, "minecraft:amethyst_cluster[facing=up,waterlogged=false]")
    for x in range(mx - 5, mx + 6):
        S.set(x, top + 4, mz - 4, "pleister_lavendel")
        S.set(x, top + 5, mz - 5, "pleister_lavendel")
        S.set(x, top + 6, mz - 5, "pleister_lavendel")
    S.set(mx, top + 5, mz - 4, "knuffelsteen_gezicht[facing=south,stemming=1]")
    S.set(mx + 6, top + 2, mz, "maansteen_plaat[type=bottom,waterlogged=false]")
    S.set(mx + 7, top + 3, mz, "lampion_roze[hanging=false]")
    S.sign(mx + 4, top + 1, mz + 4, "south", ["Niet opwinden", "na 8 uur.", "Vahoeg.", ""])

    # --- Blub-holletjes in the flanks
    HOL = [(22, 36, "east"), (60, 15, "south"), (80, 46, "west"), (72, 74, "north"), (30, 76, "east"), (42, 60, "north")]
    for (x, z, f) in HOL:
        y = T + 1
        dx, dz = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}[f]
        bx_, bz_ = x - dx * 2, z - dz * 2
        for xx in range(bx_ - 5, bx_ + 6):
            for zz in range(bz_ - 5, bz_ + 6):
                d = math.hypot(xx - bx_, zz - bz_) / 4.6
                if d < 1 and binnen(xx, zz):
                    hh = int(round(4.5 * (0.5 + 0.5 * math.cos(math.pi * d))))
                    oud = vloer(xx, zz)
                    if T + hh > oud:
                        for yy in range(oud, T + hh):
                            S.set(xx, yy, zz, "pluiswolblok")
                        S.set(xx, T + hh, zz, "c_slaapliedjesgras")
                        H[(xx, zz)] = T + hh
        for k in range(0, 3):
            cx, cz = x - dx * k, z - dz * k
            for s in (-1, 0, 1):
                for yy in (y, y + 1):
                    S.set(cx + dz * s, yy, cz + dx * s, "minecraft:air")
                S.set(cx + dz * s, y + 2, cz + dx * s, "pluiswolblok")
            S.set(cx, y - 1, cz, "pluiswolblok")
        S.set(x - dx * 2, y, z - dz * 2, "guhnestje")
        S.set(x + dx, y + 2, z + dz, "c_maanlampje[facing=south,lit=true]", keep=True)
        S.set(x - dx, y + 3, z - dz, f"knuffelsteen_gezicht[facing={f},stemming={(x + z) % 4}]")
    S.sign(25, T + 1, 40, "east", ["Hier woont Blub", "Niet aanbellen,", "njeg!", ""])

    # --- the maanmeertje
    qx, qz = 54, 80
    for x in range(qx - 7, qx + 8):
        for z in range(qz - 4, qz + 5):
            if ((x - qx) / 7) ** 2 + ((z - qz) / 4.2) ** 2 <= 1 and binnen(x, z):
                y = vloer(x, z)
                S.set(x, y, z, "c_droomzee")
                S.set(x, y - 1, z, "c_droomzand")
                for yy in range(y + 1, y + 4):
                    S.set(x, yy, z, "minecraft:air")
    S.set(qx + 1, vloer(qx, qz) + 2, qz - 1, "zeepbel")
    S.set(qx - 4, vloer(qx, qz) + 3, qz + 1, "zeepbel")

    # --- sleepy trees with lampions
    for (x, z) in ((18, 50), (84, 56), (48, 12), (60, 50), (26, 58), (80, 26)):
        if binnen(x, z):
            y = vloer(x, z) + 1
            S.fill(x, y, z, x, y + 3, z, "minecraft:cherry_log[axis=y]")
            S.ellipsoid(x, y + 5, z, 3, 2, 3, "minecraft:cherry_leaves[distance=1,persistent=true,waterlogged=false]", keep=True)
            S.set(x + 2, y + 3, z, "lampion_mint[hanging=false]", keep=True)
            S.set(x - 2, y + 3, z, "lampion_roze[hanging=false]", keep=True)

    # --- nests (where guhs dream; the guhs themselves are in the dream of the player)
    for (x, z) in ((40, 34), (62, 32), (58, 58), (28, 46)):
        S.set(x, vloer(x, z) + 1, z, "guhnestje")

    # --- the plein side: steiger, bubble, letterbox, welcome spot, signs
    for k10 in range(300, 440, 5):
        k = k10 / 10
        for w in (-1, 0, 1):
            x, z = int(round(C + PDX * k - PDZ * w)), int(round(C + PDZ * k + PDX * w))
            if not binnen(x, z):
                S.set(x, T, z, "wolkenblok_wit_plaat[type=top,waterlogged=false]" if w == 0 else
                      "wolkenblok_roze_plaat[type=top,waterlogged=false]", keep=True)
    bx, bz = C + PDX * 46, C + PDZ * 46
    S.ellipsoid(bx, T + 3, bz, 2.6, 2.6, 2.6, "c_zeepbelglas", hollow=1)
    wx, wz = int(round(C + PDX * 31)), int(round(C + PDZ * 31))
    for x in range(wx - 3, wx + 4):
        for z in range(wz - 3, wz + 4):
            if binnen(x, z):
                S.set(x, vloer(x, z), z, "maansteen_tegels")
                for y in range(vloer(x, z) + 1, vloer(x, z) + 4):
                    S.set(x, y, z, "minecraft:air")
    vw = vloer(wx, wz)
    S.set(wx - 1, vw + 1, wz - 2, "c_brievenbus_liedjes[facing=east]")
    S.set(wx - 2, vw + 1, wz + 2, "wolkenbank[facing=east]")
    S.set(wx, vw + 1, wz - 3, "sterrenlantaarn")
    S.sign(wx + 1, vw + 1, wz + 2, "east", ["Sst! Hier slapen", "12 Blub-guhs", "(en 1 vadsguh)", ""])
    S.sign(wx + 1, vw + 1, wz - 2, "east", ["Het Koortje", "Breng elke Blub", "naar zijn eigen", "klokjesheuvel!"])
    S.sign(wx - 1, vw + 1, wz + 3, "east", ["", "(sluip, ze zijn", "verlegen)", ""])
    stx, stz = int(round(C + PDX * 40)), int(round(C + PDZ * 40))
    S.sign(int(round(C + PDX * 36 - PDZ * 2)), T + 1, int(round(C + PDZ * 36 + PDX * 2)), "east",
           ["Naar het", "Droomplein", "Stap op de", "steiger en zweef"])

    # --- flowers, fences on the rim, clouds around
    for (x, z), y in sorted(H.items()):
        if S.is_air(x, y + 1, z) and rng.random() < 0.10:
            S.set(x, y + 1, z, rng.choice(["c_maanbloemetje[open=true]", "c_maanbloemetje[open=true]", "pluisgras", "minecraft:allium"]))
    pad = set()                                          # (playtest fix) the walk from the welcome spot to the steiger
    for k10 in range(240, 450, 5):
        for w in (-2, -1, 0, 1, 2):
            pad.add((int(round(C + PDX * k10 / 10 - PDZ * w)), int(round(C + PDZ * k10 / 10 + PDX * w))))
    for x in range(X):
        for z in range(Z):
            if binnen(x, z) and not all(binnen(x + a, z + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                if (x, z) not in pad and rng.random() < 0.5 and S.is_air(x, vloer(x, z) + 1, z):
                    S.set(x, vloer(x, z) + 1, z, "guh_hekje_roze")
    for (x, y, z) in ((12, 38, 22), (86, 40, 78), (16, 34, 82), (82, 42, 18)):
        S.ellipsoid(x, y, z, 3, 1.2, 2, "wolkenblok_wit")
    eiland(S, 88, T + 10, 88, 3, diepte=4, top="c_slaapliedjesgras", seed=7)
    S.set(88, T + 11, 88, "sterrenlantaarn")

    # the steiger stays free (signs and flowers must not stand on the walkway out)
    yaw = math.degrees(math.atan2(-(C - wx), C - wz))      # from the welcome spot, looking into the island
    opzet = {
        "versie": VERSIE,
        "template": "guhs:dromen/eiland_liedjes",
        "midden": [C, T, C],
        "aankomst": {"plek": [wx - C + 0.5, vw + 1 - T, wz - C + 0.5], "yaw": round(yaw, 1)},
        "steiger": [stx - C + 0.5, 1, stz - C + 0.5],
        "koor": koor,
    }
    return S, opzet


def build():
    S, opzet = bouw()
    S.s.save(os.path.join("dromen", "eiland_liedjes"))
    pad = os.path.join("src", "main", "resources", "data", "guhs", "dromen", "eiland", "liedjes.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(opzet, f, indent=2)
        f.write("\n")
    # sign texts -> nl_nl / en_us (+ the English overlay)
    nl = {k: v for k, v in sign_text.TEXTS.items() if k.startswith(PREFIX)}
    en = {}
    for k, v in nl.items():
        if v not in EN:
            raise SystemExit(f"eiland_liedjes: no English for sign line {v!r}")
        en[k] = EN[v]
    for k, (n, e) in TEKSTEN.items():
        nl[k], en[k] = n, e
    with open(os.path.join("tools", "lang", "en", "c213_eiland_liedjes.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, d_ in (("nl_nl", nl), ("en_us", en)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(d_)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"eiland_liedjes: template + json (koor {opzet['koor']}), {len(nl)} texts")


# the Blub-koortje's own messages (BlubKoortje)
TEKSTEN = {
    "gui.guhs.dromen.koortje.volgt": ("%s piept zachtjes de %s en hupt achter je aan.", "%s softly peeps a %s and hops after you."),
    "gui.guhs.dromen.koortje.schuw": ("Te wild! %s schrikt en hupt weg. Sluip maar, vadsje.", "Too wild! %s startles and hops away. Sneak, vadsje."),
    "gui.guhs.dromen.koortje.verkeerd": ("Njeg... dit is niet mijn heuvel. Ik ben de %s!", "Njeg... this isn't my hill. I'm the %s!"),
    "gui.guhs.dromen.koortje.goed": ("%s staat op de %s-heuvel. Oortjes rechtop! (%s/5)", "%s is on the %s hill. Ears up! (%s/5)"),
    "gui.guhs.dromen.koortje.zingt": ("Het Blub-koortje zingt het slaapliedje... sst.", "The Blub choir sings the lullaby... shh."),
    "gui.guhs.dromen.koortje.stukje": ("Je hebt Snoes' liedje gevonden! Hij zal het nooit meer vergeten.", "You found Snoes' song! He will never forget it."),
    "gui.guhs.dromen.koortje.klaar": ("Wat mooi. De Blubs zijn moe en huppelen weer weg... morgen weer?", "How lovely. The Blubs are tired and hop away again... again tomorrow?"),
    "gui.guhs.dromen.koortje.niet_temmen": ("%s zingt in het koortje, die is niet te temmen, njeg!", "%s sings in the choir, it can't be tamed, njeg!"),
    "gui.guhs.dromen.koortje.do": ("do", "do"),
    "gui.guhs.dromen.koortje.re": ("re", "re"),
    "gui.guhs.dromen.koortje.mi": ("mi", "mi"),
    "gui.guhs.dromen.koortje.fa": ("fa", "fa"),
    "gui.guhs.dromen.koortje.sol": ("sol", "sol"),
}

if __name__ == "__main__":
    build()
